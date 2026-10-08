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
import net.minecraft.world.phys.AABB;
import java.util.*;
import static com.example.aimassist.AimGeometryTodoAi.*;

/** Client-side look assistance with scoped reach probes. Never attacks or sends rotations. */ //codex (old code snippet) /** Client-side look assistance only. Never attacks, sends rotations, or changes reach. */
public final class AimAssistControllerTodoAi {
    private static final State AURA = new State(), TARGETTING = new State();
    private static final Map<UUID, Long> locks = new HashMap<>();
    private static ClientLevel level;
    private static Rotation lastApplied;
    // codex start
    private static final Map<UUID, AimCrosshairMotionTodoAi.Observation> CROSSHAIR_HISTORY = new HashMap<>();
    private static final Map<UUID, AimCrosshairMotionTodoAi.Gate> CROSSHAIR_GATES = new HashMap<>();
    //codex end

    private static final class State {
        LivingEntity target;
        Rotation start, end, goal;
        long lastAttack;
        void clear() { target = null; start = end = goal = null; lastAttack = 0; }
    }
    private AimAssistControllerTodoAi() {}
    public static LivingEntity auraEligibleTarget(Minecraft mc, AimAssistConfigTodoAi.Assist cfg) {
        // codex start
        if (mc.player == null || mc.level == null || cfg.targetingMargin <= 0) return null;
        if (TargetingMarginPickTodoAi.pick(mc, 0f, cfg.range).getType() == HitResult.Type.ENTITY) return null;
        float increased = Utils.computeCheatConfig().movingTargetingMarginBypass + (float) cfg.targetingMargin;
        HitResult expanded = TargetingMarginPickTodoAi.pick(mc, increased, cfg.range);
        //codex end
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
            level = mc.level; AURA.clear(); TARGETTING.clear(); locks.clear(); lastApplied = null; CROSSHAIR_HISTORY.clear(); CROSSHAIR_GATES.clear(); //codex (old code snippet) level = mc.level; AURA.clear(); TARGETTING.clear(); locks.clear(); lastApplied = null;
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
            // codex start
            HitResult pick = TargetingMarginPickTodoAi.pick(mc, 0f, cfg.range);
            LivingEntity entity = pick instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living
                    ? living : auraFallback(mc);
            if (entity != null && allowed(mc, entity)) {
                goal = targetingGoal(mc, entity, current, cfg);
                if (goal != null) state.target = entity;
            }
            //codex end
        } else {
            LivingEntity eligible = auraEligibleTarget(mc, cfg);
            if (eligible == null) return;
            List<LivingEntity> candidates = new ArrayList<>();
            for (var entity : mc.level.entitiesForRendering()) if (entity instanceof LivingEntity living &&
                allowed(mc, living) && living.hurtTime <= cfg.hurtTime &&
                living.getBoundingBox().distanceToSqr(mc.player.getEyePosition()) <= assistRange(mc, cfg) * assistRange(mc, cfg)) candidates.add(living); //codex (old code snippet) living.getBoundingBox().distanceToSqr(mc.player.getEyePosition()) <= cfg.range * cfg.range) candidates.add(living);
            candidates.sort((a, b) -> compare(mc, a, b, cfg.priorities));
            for (LivingEntity entity : candidates) {
                if (entity != eligible) continue;
                Vec3 point = nearestPoint(mc.player.getEyePosition(), entity.getBoundingBox(), current, assistRange(mc, cfg), mouseStep(mc)); //codex (old code snippet) Vec3 point = nearestPoint(mc.player.getEyePosition(), entity.getBoundingBox(), current, cfg.range, mouseStep(mc));
                Rotation desired = lookAt(mc.player.getEyePosition(), point);
                if (valid(mc, entity, desired, assistRange(mc, cfg))) { state.target = entity; goal = desired; break; } //codex (old code snippet) if (valid(mc, entity, desired, cfg.range)) { state.target = entity; goal = desired; break; }
            }
        }
        // codex start
        if (goal != null && !AimCorrectionFovTodoAi.allows(current, goal, cfg.maxCorrectionFov)) {
            state.target = null;
            state.goal = null;
            return;
        }
        //codex end
        if (goal != null) {
            state.start = current;
            state.end = smooth(current, goal, cfg.interpolation, state.goal, !targetting); //codex (old code snippet) state.end = smooth(current, goal, cfg.interpolation, state.goal);
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
    private static Rotation targetingGoal(Minecraft mc, LivingEntity e, Rotation current, AimAssistConfigTodoAi.Assist cfg) { //codex (old code snippet) private static Rotation targetingGoal(Minecraft mc, LivingEntity e, Rotation current) {
        double range = entityRange(mc, cfg); //codex (old code snippet) double range = mc.player.entityInteractionRange();
        // codex start
        Rotation seed = current;
        if (!valid(mc, e, current, range)) {
            if (auraFallback(mc) != e) return null;
            seed = lookAt(mc.player.getEyePosition(), nearestPoint(mc.player.getEyePosition(), e.getBoundingBox(), current, range, mouseStep(mc)));
            seed = new Rotation(seed.yaw(), Math.min(seed.pitch(), current.pitch()));
            if (!valid(mc, e, seed, range)) return null;
        }
        //codex end
        Rotation goal = AimCenterlineTodoAi.goal(mc.player.getEyePosition(), e.getBoundingBox(), seed, range, mouseStep(mc), cfg.centerlineWidth); //codex (old code snippet) Rotation goal = neutralPitch(mc.player.getEyePosition(), e.getBoundingBox(), current, range, mouseStep(mc));
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
        // codex start
        if (mc.player == null || mc.level != level || mc.screen != null) { //codex (old code snippet) if (mc.player == null || mc.level != level) return;
            CROSSHAIR_HISTORY.clear();
            CROSSHAIR_GATES.clear();
            return;
        }
        //codex end
        Rotation actual = current(mc);
        if (lastApplied != null) {
            double yaw = wrap(actual.yaw() - lastApplied.yaw()), pitch = actual.pitch() - lastApplied.pitch();
            for (State state : List.of(AURA, TARGETTING)) {
                if (state.start != null) state.start = new Rotation(state.start.yaw() + yaw, state.start.pitch() + pitch);
                if (state.end != null) state.end = new Rotation(state.end.yaw() + yaw, state.end.pitch() + pitch);
            }
        }
        // codex start
        long now = System.nanoTime();
        double trackingRange = Math.max(assistRange(mc, config().aura), entityRange(mc, config().targetting)) + 1;
        Set<UUID> tracked = new HashSet<>();
        for (var entity : mc.level.entitiesForRendering()) if (entity instanceof LivingEntity living && living != mc.player) {
            AABB box = crosshairBox(living, partialTicks);
            Vec3 eyes = mc.player.getEyePosition(partialTicks);
            if (box.distanceToSqr(eyes) > trackingRange * trackingRange) continue;
            UUID id = living.getUUID();
            tracked.add(id);
            var previous = CROSSHAIR_HISTORY.get(id);
            CROSSHAIR_GATES.computeIfAbsent(id, ignored -> new AimCrosshairMotionTodoAi.Gate()).update(
                    previous == null ? null : previous.proximity(), AimCrosshairMotionTodoAi.sample(eyes, box, actual), now);
        }
        CROSSHAIR_GATES.keySet().retainAll(tracked);
        //codex end
        apply(mc, AURA, config().aura, false, partialTicks);
        apply(mc, TARGETTING, config().targetting, true, partialTicks);
        lastApplied = current(mc);
        // codex start
        CROSSHAIR_HISTORY.clear();
        for (var entity : mc.level.entitiesForRendering()) if (entity instanceof LivingEntity living) {
            CROSSHAIR_HISTORY.put(living.getUUID(), new AimCrosshairMotionTodoAi.Observation(
                    mc.player.getEyePosition(partialTicks), crosshairBox(living, partialTicks), lastApplied));
        }
        //codex end
    }
    private static void apply(Minecraft mc, State state, AimAssistConfigTodoAi.Assist cfg, boolean targeting, float partial) {
        if (!active(mc, state, cfg) || state.target == null || state.end == null || !allowed(mc, state.target)) return;
        // codex start
        var gate = CROSSHAIR_GATES.get(state.target.getUUID());
        if (gate == null || !gate.allowed()) return;
        //codex end
        Rotation proposed = state.start.toward(state.end, Math.clamp(partial, 0, 1));
        if (targeting) {
            // codex start
            HitResult pick = TargetingMarginPickTodoAi.pick(mc, 0f, cfg.range);
            boolean direct = pick instanceof EntityHitResult hit && hit.getEntity() == state.target;
            if (!direct && (pick instanceof EntityHitResult || auraFallback(mc) != state.target)) return;
            //codex end
            Rotation goal = targetingGoal(mc, state.target, current(mc), cfg); //codex (old code snippet) Rotation goal = targetingGoal(mc, state.target, current(mc));
            if (goal == null || !AimCorrectionFovTodoAi.allows(current(mc), goal, cfg.maxCorrectionFov)) return; //codex (old code snippet) if (goal == null) return;
            // codex start
            proposed = AimCenterlineTodoAi.clamp(current(mc), proposed, goal);
            //codex end
            // codex start
            if (direct) proposed = constrain(proposed, goal, r -> valid(mc, state.target, normalize(mc, r), entityRange(mc, cfg)));
            // On aura-supported misses, intermediate rotations can remain outside until they approach the goal.
            //codex end
            if (proposed == null) return;
        } else {
            if (auraEligibleTarget(mc, cfg) != state.target) return;
            proposed = new Rotation(cfg.horizontal ? proposed.yaw() : mc.player.getYRot(), cfg.vertical ? proposed.pitch() : mc.player.getXRot());
        }
        // codex start
        if (state.goal != null && !AimCorrectionFovTodoAi.allows(current(mc), state.goal, cfg.maxCorrectionFov) ||
                !AimCorrectionFovTodoAi.allows(current(mc), proposed, cfg.maxCorrectionFov)) return;
        //codex end
        Rotation normalized = normalize(mc, proposed);
        // codex start
        if (!AimCorrectionFovTodoAi.allows(current(mc), normalized, cfg.maxCorrectionFov)) return;
        //codex end
        mc.player.setYRot((float) normalized.yaw());
        mc.player.setXRot((float) normalized.pitch());
    }
    // codex start
    private static LivingEntity auraFallback(Minecraft mc) {
        return active(mc, AURA, config().aura) && AURA.target != null && AURA.end != null &&
                auraEligibleTarget(mc, config().aura) == AURA.target ? AURA.target : null;
    }
    //codex end
    // codex start
    private static double entityRange(Minecraft mc, AimAssistConfigTodoAi.Assist cfg) {
        return AimAssistReachTodoAi.range(cfg.range);
    }
    private static double assistRange(Minecraft mc, AimAssistConfigTodoAi.Assist cfg) {
        return AimAssistReachTodoAi.range(cfg.range);
    }
    //codex end
    // codex start
    private static AABB crosshairBox(LivingEntity entity, float partial) {
        return entity.getBoundingBox().move(entity.getPosition(partial).subtract(entity.position()));
    }
    //codex end
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
