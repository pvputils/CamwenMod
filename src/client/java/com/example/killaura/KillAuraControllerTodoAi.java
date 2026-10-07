/* Target acquisition and rotations adapted from LiquidBounce, copyright CCBlueX 2015-2026.
 * GPL-3.0-or-later. See KillAuraLicenseTodoAi.txt. */
package com.example.killaura;

import com.example.Configs.Config;
import com.example.UntitledClient;
import com.example.aimassist.AimGeometryTodoAi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import org.slf4j.LoggerFactory;
import static com.example.killaura.KillAuraRotationTodoAi.*;

/** Only plans rotations. Attacks remain inside Minecraft.startAttack, driven by real presses. */
public final class KillAuraControllerTodoAi {
    private static final KillAuraRotationTodoAi smoother = new KillAuraRotationTodoAi();
    private static ClientLevel level;
    private static LivingEntity target;
    private static Rotation rotation, previousRotation;
    private static int idleTicks;
    private static String loadedModel;
    private static KillAuraModelTodoAi model;
    private static final Set<String> failedModels = new HashSet<>();
    private KillAuraControllerTodoAi() {}
    public static KillAuraConfigTodoAi config() {
        if (UntitledClient.config.killAura == null) UntitledClient.config.killAura = new KillAuraConfigTodoAi();
        var config = UntitledClient.config.killAura;
        config.repair();
        return config;
    }
    public static LivingEntity target() { return target; }
    public static Rotation rotation() { return rotation; }
    public static void reset() { target = null; rotation = previousRotation = null; idleTicks = 0; smoother.reset(); }
    private static boolean available(Minecraft mc) {
        return config().enabled && mc.player != null && mc.level != null && mc.gui.screen() == null
            && !mc.player.isSpectator() && !mc.player.isDeadOrDying();
    }
    public static AttackRange attackRange(Minecraft mc) {
        AttackRange range = mc.player.getMainHandItem().get(DataComponents.ATTACK_RANGE);
        return range == null ? AttackRange.defaultFor(mc.player) : range;
    }
    public static Rotation clientRotation(Minecraft mc) { return new Rotation(mc.player.getYRot(), mc.player.getXRot()); }
    private static AimGeometryTodoAi.Rotation geometry(Rotation rotation) { return new AimGeometryTodoAi.Rotation(rotation.yaw(), rotation.pitch()); }
    public static Vec3 direction(Rotation rotation) { return geometry(rotation).direction(); }
    private static Rotation lookAt(Vec3 eyes, Vec3 point) {
        var rotation = AimGeometryTodoAi.lookAt(eyes, point);
        return new Rotation(rotation.yaw(), rotation.pitch());
    }
    private static double boxedDistanceSquared(Minecraft mc, Entity entity) {
        var eyes = mc.player.getEyePosition(); var box = entity.getBoundingBox();
        return eyes.distanceToSqr(new Vec3(Math.clamp(eyes.x, box.minX, box.maxX), Math.clamp(eyes.y, box.minY, box.maxY), Math.clamp(eyes.z, box.minZ, box.maxZ)));
    }
    private static int typeWeight(Minecraft mc, LivingEntity entity) {
        return entity instanceof Player ? 0 : entity instanceof Enemy ? 1 : entity instanceof NeutralMob mob && mc.player.getUUID().equals(mob.getPersistentAngerTarget()) ? 2 : Integer.MAX_VALUE;
    }
    public static boolean eligible(Minecraft mc, LivingEntity entity) {
        var cfg = config(); var targets = cfg.targets;
        if (entity == mc.player || entity.isRemoved() || entity instanceof ArmorStand || entity.isSpectator() || entity.hurtTime > cfg.target.hurtTime) return false;
        if (!entity.isAlive() && !targets.dead || entity.isInvisible() && !targets.invisible) return false;
        boolean allowed = entity instanceof Player ? targets.players : entity instanceof Enemy ? targets.hostile
            : entity instanceof NeutralMob mob && mc.player.getUUID().equals(mob.getPersistentAngerTarget()) ? targets.angerable
            : entity instanceof WaterAnimal ? targets.waterCreatures : targets.passive;
        if (!allowed) return false;
        if (entity instanceof Player && cfg.target.excludeTeammates && UntitledClient.config.nameplateUuids.containsKey(entity.getUUID())) return false;
        if (entity instanceof Player && !cfg.target.ignoreShield && entity.isBlocking() && !mc.player.getMainHandItem().is(ItemTags.AXES)) {
            Vec3 towardPlayer = mc.player.position().subtract(entity.position()).normalize();
            if (entity.getLookAngle().dot(towardPlayer) > 0) return false;
        }
        return distance(clientRotation(mc), lookAt(mc.player.getEyePosition(), entity.getBoundingBox().getCenter())) <= cfg.target.fov;
    }
    private static Comparator<LivingEntity> comparator(Minecraft mc) {
        Comparator<LivingEntity> order = (a, b) -> 0;
        for (String priority : config().target.priorities) {
            Comparator<LivingEntity> next = switch (priority) {
                case "Type" -> Comparator.comparingInt(e -> typeWeight(mc, e));
                case "Health" -> Comparator.comparingDouble(e -> e.getHealth() + e.getAbsorptionAmount());
                case "Distance" -> Comparator.comparingDouble(e -> boxedDistanceSquared(mc, e));
                case "Direction" -> Comparator.comparingDouble(e -> distance(clientRotation(mc), lookAt(mc.player.getEyePosition(), e.getBoundingBox().getCenter())));
                case "HurtTime" -> Comparator.comparingInt(e -> e.hurtTime);
                case "Age" -> Comparator.<LivingEntity>comparingInt(e -> e.tickCount).reversed();
                default -> (a, b) -> 0;
            };
            order = order.thenComparing(next);
        }
        return order;
    }
    private static Vec3 hit(Minecraft mc, Entity entity, Rotation rotation) {
        Vec3 eyes = mc.player.getEyePosition(), end = eyes.add(direction(rotation).scale(attackRange(mc).effectiveMaxRange(mc.player)));
        Vec3 point = entity.getBoundingBox().contains(eyes) ? eyes : entity.getBoundingBox().clip(eyes, end).orElse(null);
        if (point == null || !attackRange(mc).isInRange(mc.player, point)) return null;
        BlockHitResult block = mc.level.clip(new ClipContext(eyes, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        return block.getType() == HitResult.Type.MISS || eyes.distanceToSqr(block.getLocation()) + 1E-7 >= eyes.distanceToSqr(point) ? point : null;
    }
    private static Rotation goal(Minecraft mc, LivingEntity entity, Rotation current) {
        if (config().rotations.lazyRotation && hit(mc, entity, current) != null) return current;
        var box = entity.getBoundingBox(); var eyes = mc.player.getEyePosition();
        double range = attackRange(mc).effectiveMaxRange(mc.player);
        Vec3 best = AimGeometryTodoAi.nearestPoint(eyes, box, geometry(current), range, 0.1);
        Rotation result = best == null ? null : lookAt(eyes, best);
        if (result != null && hit(mc, entity, result) != null) return result;
        result = null;
        // Search the box faces as well as the nearest angular point so partial cover does not
        // reject an otherwise visible enemy. Every candidate uses unexpanded bounds and LOS.
        double difference = Double.POSITIVE_INFINITY;
        for (double x : new double[]{0.05, 0.5, 0.95}) for (double y : new double[]{0.05, 0.5, 0.95}) for (double z : new double[]{0.05, 0.5, 0.95}) {
            Vec3 point = new Vec3(box.minX + box.getXsize() * x, box.minY + box.getYsize() * y, box.minZ + box.getZsize() * z);
            Rotation candidate = lookAt(eyes, point);
            double angle = distance(current, candidate);
            if (angle < difference && hit(mc, entity, candidate) != null) { result = candidate; difference = angle; }
        }
        return result;
    }
    public static void tick(Minecraft mc) {
        if (level != mc.level) { level = mc.level; reset(); }
        if (!available(mc)) { reset(); return; }
        Rotation current = rotation == null ? clientRotation(mc) : rotation;
        List<LivingEntity> candidates = new ArrayList<>();
        double range = attackRange(mc).effectiveMaxRange(mc.player);
        for (Entity entity : mc.level.entitiesForRendering()) if (entity instanceof LivingEntity living && eligible(mc, living) && boxedDistanceSquared(mc, living) <= range * range) candidates.add(living);
        candidates.sort(comparator(mc));
        target = null; Rotation goal = null;
        for (LivingEntity candidate : candidates) {
            Rotation candidateGoal = goal(mc, candidate, current);
            if (candidateGoal != null) { target = candidate; goal = candidateGoal; break; }
        }
        if (target == null) {
            if (rotation == null) return;
            if (++idleTicks > config().rotations.ticksUntilReset && distance(current, clientRotation(mc)) <= config().rotations.resetThreshold) { reset(); return; }
            previousRotation = current;
            rotation = quantize(current, smoother.step(current, clientRotation(mc), config().rotations, false, 0, null), mc.options.sensitivity().get());
            return;
        }
        idleTicks = 0;
        if (config().rotations.timing.equals("Snap")) { rotation = null; return; }
        if (config().rotations.timing.equals("OnTick") && estimateTicks(current, goal, config().rotations) <= 1) { rotation = null; return; }
        rotation = quantize(current, smoother.step(current, goal, config().rotations, hit(mc, target, current) != null,
            Math.sqrt(boxedDistanceSquared(mc, target)), modelOutput(mc, current, goal)), mc.options.sensitivity().get());
        previousRotation = current;
        if (config().rotations.movementCorrection.equals("ChangeLook")) {
            mc.player.setYRot((float) rotation.yaw()); mc.player.setXRot((float) rotation.pitch());
        }
    }
    private static float[] modelOutput(Minecraft mc, Rotation current, Rotation goal) {
        if (!config().rotations.smoothing.equals("AI")) return null;
        String name = config().rotations.ai.model;
        if (name == null || failedModels.contains(name)) return null;
        try {
            if (!name.equals(loadedModel)) {
                Path folder = mc.gameDirectory.toPath().resolve("LiquidBounceKillAura/deeplearning/models");
                model = KillAuraModelTodoAi.load(name, folder); loadedModel = name;
            }
            Rotation previous = previousRotation == null ? current : previousRotation;
            return model.predict(new float[]{(float) wrap(goal.yaw() - current.yaw()), (float) (goal.pitch() - current.pitch()),
                (float) wrap(current.yaw() - previous.yaw()), (float) (current.pitch() - previous.pitch()),
                (float) (mc.player.getDeltaMovement().horizontalDistance() + target.getDeltaMovement().horizontalDistance()), (float) boxedDistanceSquared(mc, target)});
        } catch (Exception error) {
            failedModels.add(name);
            LoggerFactory.getLogger("CamwenMod/KillAura").warn("Cannot load KillAura AI model {}; using interpolation", name, error);
            return null;
        }
    }
    /** Wraps vanilla's complete startAttack call, including CamwenMod's existing mixins.
     * Temporary crosshair/packet state is restored even if another mixin cancels or throws. */
    public static boolean attack(Minecraft mc, Supplier<Boolean> vanilla) {
        if (!available(mc) || mc.player.isUsingItem() || mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK || target == null) return vanilla.get();
        var cfg = config();
        if (!eligible(mc, target)) return false;
        Rotation aim = cfg.rotations.timing.equals("Normal") ? (rotation == null ? clientRotation(mc) : rotation) : goal(mc, target, clientRotation(mc));
        if (aim == null) return false;
        Entity attacked = target;
        Vec3 point = hit(mc, target, aim);
        if (!cfg.raycast.equals("None")) {
            double nearest = Double.POSITIVE_INFINITY;
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity == mc.player || entity.isSpectator() || !entity.isPickable()) continue;
                if (cfg.raycast.equals("OnlyEnemy") && (!(entity instanceof LivingEntity living) || !eligible(mc, living))) continue;
                Vec3 intercept = hit(mc, entity, aim);
                if (intercept != null && mc.player.getEyePosition().distanceToSqr(intercept) < nearest) {
                    attacked = entity; point = intercept; nearest = mc.player.getEyePosition().distanceToSqr(intercept);
                }
            }
        }
        if (point == null || !mc.player.getMainHandItem().isItemEnabled(mc.level.enabledFeatures()) || mc.player.cannotAttackWithItem(mc.player.getMainHandItem(), 0)) return false;
        HitResult original = mc.hitResult;
        Rotation camera = clientRotation(mc);
        try {
            mc.hitResult = new EntityHitResult(attacked, point);
            sendRotation(mc, aim);
            mc.player.setYRot((float) aim.yaw()); mc.player.setXRot((float) aim.pitch());
            return vanilla.get();
        } finally {
            mc.player.setYRot((float) camera.yaw()); mc.player.setXRot((float) camera.pitch());
            mc.hitResult = original;
            if (!cfg.rotations.timing.equals("Normal")) sendRotation(mc, camera);
        }
    }
    private static void sendRotation(Minecraft mc, Rotation rotation) {
        mc.player.connection.send(new ServerboundMovePlayerPacket.PosRot(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            (float) rotation.yaw(), (float) rotation.pitch(), mc.player.onGround(), mc.player.horizontalCollision));
    }
    public static float packetYaw(float original) { return rotation == null ? original : (float) rotation.yaw(); }
    public static float packetPitch(float original) { return rotation == null ? original : (float) rotation.pitch(); }
    public static float movementYaw(float original) {
        return rotation == null || config().rotations.movementCorrection.equals("Off") ? original : (float) rotation.yaw();
    }
    /** Silent correction chooses the closest keyboard direction in the spoofed coordinate frame. */
    public static Vec3 correctInput(Vec3 input, float cameraYaw) {
        if (rotation == null || !config().rotations.movementCorrection.equals("Silent") || input.horizontalDistanceSqr() < 1E-9) return input;
        double delta = Math.toRadians(cameraYaw - rotation.yaw());
        double x = input.x * Math.cos(delta) - input.z * Math.sin(delta), z = input.x * Math.sin(delta) + input.z * Math.cos(delta);
        double angle = Math.atan2(x, z), snapped = Math.rint(angle / (Math.PI / 4)) * (Math.PI / 4);
        return new Vec3(Math.round(Math.sin(snapped)), input.y, Math.round(Math.cos(snapped)));
    }
}
