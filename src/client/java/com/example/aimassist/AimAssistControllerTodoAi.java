package com.example.aimassist;

import com.example.UntitledClient;
import com.example.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import static com.example.aimassist.AimGeometryTodoAi.*;

/** Client-side look assistance only. Never attacks, sends rotations, or changes reach. */
public final class AimAssistControllerTodoAi {
    private static final State AURA = new State(), TARGETTING = new State();
    private static final Map<UUID, Long> locks = new HashMap<>();
    private static ClientLevel level;
    private static Rotation lastApplied;

    private static final class State {
        LivingEntity target;
        Rotation start, end, goal;
        long lastAttack;
        void clear() { target = null; start = end = goal = null; lastAttack = 0; }
    }
    private AimAssistControllerTodoAi() {}
    public static LivingEntity auraEligibleTarget(Minecraft mc, AimAssistConfigTodoAi.Assist cfg) {
        if (mc.player == null || mc.level == null || mc.hitResult == null ||
            mc.hitResult.getType() == HitResult.Type.ENTITY || cfg.targetingMargin <= 0) return null;
        if (TargetingMarginPickTodoAi.pick(mc, null).getType() == HitResult.Type.ENTITY) return null;
        float increased = Utils.computeCheatConfig().staticTargetingMarginBypass + (float) cfg.targetingMargin;
        HitResult expanded = TargetingMarginPickTodoAi.pick(mc, increased);
        return expanded instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living ? living : null;
    }
    public static AimAssistConfigTodoAi config() {
        if (UntitledClient.config.aimAssist == null) UntitledClient.config.aimAssist = new AimAssistConfigTodoAi();
        return UntitledClient.config.aimAssist;
    }
    public static boolean clickActive(boolean held, long lastAttack, long now, int window) {
        long elapsed = now - lastAttack;
        return held || (lastAttack != 0 && window > 0 && elapsed >= 0 && elapsed < Math.clamp(window, 0, 200) * 1_000_000L);
    }
    public static void attackAttempt() {
        Minecraft mc = Minecraft.getInstance();
        long now = System.nanoTime();
        if (config().aura.enabled) AURA.lastAttack = now;
        if (config().targetting.enabled) TARGETTING.lastAttack = now;
        if (config().targetLock.enabled && mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof Player p) {
            locks.put(p.getUUID(), now + Math.clamp(config().targetLock.maximumTime, 0, 120) * 1_000_000_000L);
        }
    }
    private static boolean active(Minecraft mc, State state, AimAssistConfigTodoAi.Assist cfg) {
        return cfg.enabled && mc.player != null && mc.level != null && mc.screen == null && //codex (old code snippet) return cfg.enabled && mc.player != null && mc.level != null && mc.gui.screen() == null &&
            clickActive(Utils.getIsKeyBindingPressed(mc.options.keyAttack), state.lastAttack, System.nanoTime(), cfg.requires.attackWindow) &&
            (!cfg.requires.notBreaking || mc.gameMode != null && !mc.gameMode.isDestroying());
    }
    public static void tick(Minecraft mc) {
        if (level != mc.level) {
            level = mc.level; AURA.clear(); TARGETTING.clear(); locks.clear(); lastApplied = null;
        }
        if (mc.player == null || mc.level == null) return;
        locks.entrySet().removeIf(e -> e.getValue() <= System.nanoTime() ||
            mc.level.players().stream().noneMatch(p -> p.getUUID().equals(e.getKey()) &&
                p.distanceTo(mc.player) <= config().targetLock.maximumRange));
        plan(mc, AURA, config().aura, false);
        plan(mc, TARGETTING, config().targetting, true);
        lastApplied = current(mc);
    }
    private static void plan(Minecraft mc, State state, AimAssistConfigTodoAi.Assist cfg, boolean targetting) {
        state.target = null; state.start = state.end = null;
        if (!cfg.enabled) { state.clear(); return; }
        if (!active(mc, state, cfg)) return;
        Rotation current = current(mc), goal = null;
        if (targetting) {
            if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity entity && allowed(mc, entity)) {
                goal = targetingGoal(mc, entity, current);
                if (goal != null) state.target = entity;
            }
        } else {
            LivingEntity eligible = auraEligibleTarget(mc, cfg);
            if (eligible == null) return;
            List<LivingEntity> candidates = new ArrayList<>();
            for (var entity : mc.level.entitiesForRendering()) if (entity instanceof LivingEntity living &&
                allowed(mc, living) && living.hurtTime <= cfg.hurtTime &&
                living.getBoundingBox().distanceToSqr(mc.player.getEyePosition()) <= cfg.range * cfg.range) candidates.add(living);
            candidates.sort((a, b) -> compare(mc, a, b, cfg.priorities));
            for (LivingEntity entity : candidates) {
                if (entity != eligible) continue;
                Vec3 point = nearestPoint(mc.player.getEyePosition(), entity.getBoundingBox(), current, cfg.range, mouseStep(mc));
                Rotation desired = lookAt(mc.player.getEyePosition(), point);
                if (valid(mc, entity, desired, cfg.range)) { state.target = entity; goal = desired; break; }
            }
        }
        if (goal != null) {
            state.start = current;
            state.end = smooth(current, goal, cfg.interpolation, state.goal);
            state.goal = goal;
        }
    }
    private static int compare(Minecraft mc, LivingEntity a, LivingEntity b, List<String> priorities) {
        for (String priority : priorities) {
            int result = Double.compare(score(mc, a, priority), score(mc, b, priority));
            if (result != 0) return result;
        }
        return Integer.compare(a.getId(), b.getId());
    }
    private static double score(Minecraft mc, LivingEntity e, String priority) {
        return switch (priority) {
            case "Type" -> e instanceof Player ? 0 : e instanceof Enemy ? 1 : e instanceof NeutralMob ? 2 : 3;
            case "Health" -> e.getHealth() + e.getAbsorptionAmount();
            case "Distance" -> e.getBoundingBox().distanceToSqr(mc.player.getEyePosition());
            case "HurtTime" -> e.hurtTime;
            case "Age" -> -e.tickCount;
            default -> error(current(mc), lookAt(mc.player.getEyePosition(), e.getBoundingBox().getCenter()));
        };
    }
    private static boolean allowed(Minecraft mc, LivingEntity e) {
        var t = config().targets;
        if (UntitledClient.config.isAimAssistDisabledOnTeammates && e instanceof Player) {
            var team = UntitledClient.config.nameplateUuids.get(e.getUUID());
            if (team == com.example.Configs.Config.NameplateTeam.ALLY ||
                team == com.example.Configs.Config.NameplateTeam.FRIENDLY) return false;
        }
        if (e == mc.player || e.isRemoved() || !e.isAlive() && !t.dead || e.isInvisible() && !t.invisible || e.isSpectator()) return false;
        boolean type = e instanceof Player ? t.players : e instanceof ArmorStand ? t.armorStand :
            e instanceof Enemy ? t.hostile : e instanceof NeutralMob ? t.angerable : e instanceof WaterAnimal ? t.waterCreature : t.passive;
        if (!type) return false;
        var lock = config().targetLock;
        if (lock.enabled && e instanceof Player p) {
            if (lock.mode.equals("Filter")) {
                boolean listed = Arrays.stream(lock.usernames.split(",")).anyMatch(n -> n.trim().equalsIgnoreCase(p.getGameProfile().getName())); //codex (old code snippet) boolean listed = Arrays.stream(lock.usernames.split(",")).anyMatch(n -> n.trim().equalsIgnoreCase(p.getGameProfile().name()));
                return listed == lock.whitelist;
            }
            return locks.isEmpty() ? lock.allowWhenUnlocked : locks.containsKey(p.getUUID());
        }
        return true;
    }
    private static Rotation targetingGoal(Minecraft mc, LivingEntity e, Rotation current) {
        double range = mc.player.entityInteractionRange();
        if (!valid(mc, e, current, range)) return null;
        Rotation goal = centered(mc.player.getEyePosition(), e.getBoundingBox(), current, range, mouseStep(mc));
        return goal != null && valid(mc, e, normalize(mc, goal), range) ? goal : null;
    }
    private static boolean valid(Minecraft mc, LivingEntity e, Rotation rotation, double range) {
        Vec3 eyes = mc.player.getEyePosition();
        Vec3 point = e.getBoundingBox().clip(eyes, eyes.add(rotation.direction().scale(range))).orElse(null);
        if (point == null) return false;
        HitResult block = mc.level.clip(new ClipContext(eyes, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        return block.getType() == HitResult.Type.MISS || block.getLocation().distanceToSqr(eyes) + 1e-7 >= point.distanceToSqr(eyes);
    }
    public static void render(Minecraft mc, float partialTicks) {
        if (mc.player == null || mc.level != level) return;
        Rotation actual = current(mc);
        if (lastApplied != null) {
            double yaw = wrap(actual.yaw() - lastApplied.yaw()), pitch = actual.pitch() - lastApplied.pitch();
            for (State state : List.of(AURA, TARGETTING)) {
                if (state.start != null) state.start = new Rotation(state.start.yaw() + yaw, state.start.pitch() + pitch);
                if (state.end != null) state.end = new Rotation(state.end.yaw() + yaw, state.end.pitch() + pitch);
            }
        }
        apply(mc, AURA, config().aura, false, partialTicks);
        apply(mc, TARGETTING, config().targetting, true, partialTicks);
        lastApplied = current(mc);
    }
    private static void apply(Minecraft mc, State state, AimAssistConfigTodoAi.Assist cfg, boolean targeting, float partial) {
        if (!active(mc, state, cfg) || state.target == null || state.end == null || !allowed(mc, state.target)) return;
        Rotation proposed = state.start.toward(state.end, Math.clamp(partial, 0, 1));
        if (targeting) {
            if (!(mc.hitResult instanceof EntityHitResult hit) || hit.getEntity() != state.target) return;
            Rotation goal = targetingGoal(mc, state.target, current(mc));
            if (goal == null) return;
            proposed = constrain(proposed, goal, r -> valid(mc, state.target, normalize(mc, r), mc.player.entityInteractionRange()));
            if (proposed == null) return;
        } else {
            if (auraEligibleTarget(mc, cfg) != state.target) return;
            proposed = new Rotation(cfg.horizontal ? proposed.yaw() : mc.player.getYRot(), cfg.vertical ? proposed.pitch() : mc.player.getXRot());
        }
        Rotation normalized = normalize(mc, proposed);
        mc.player.setYRot((float) normalized.yaw());
        mc.player.setXRot((float) normalized.pitch());
    }
    private static Rotation current(Minecraft mc) { return new Rotation(mc.player.getYRot(), mc.player.getXRot()); }
    private static double mouseStep(Minecraft mc) {
        double f = mc.options.sensitivity().get() * 0.6 + 0.2;
        return f * f * f * 8 * 0.15;
    }
    private static Rotation normalize(Minecraft mc, Rotation rotation) {
        Rotation current = current(mc);
        double step = mouseStep(mc);
        return new Rotation(current.yaw() + Math.round(wrap(rotation.yaw() - current.yaw()) / step) * step,
            Math.clamp(current.pitch() + Math.round((rotation.pitch() - current.pitch()) / step) * step, -90, 90));
    }
}
