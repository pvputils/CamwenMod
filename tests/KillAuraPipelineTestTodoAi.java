package com.example.combat;

import com.example.combat.clicking.AttackClockTodoAi;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.concurrent.atomic.AtomicInteger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import static com.example.combat.HitQueueTodoAi.Result.*;

/** Regression checks use real Minecraft AABB/ray math and the actual ported click planner. */
public final class KillAuraPipelineTestTodoAi {
    private static int checks;
    private static void check(boolean success, String message) {
        checks++;
        if (!success) throw new AssertionError(message);
    }

    private static void member(String className, String name, String descriptor, boolean field) throws Exception {
        AtomicInteger found = new AtomicInteger();
        try (var input = KillAuraPipelineTestTodoAi.class.getResourceAsStream("/" + className + ".class")) {
            if (input == null) throw new AssertionError("Missing Minecraft class " + className);
            new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public FieldVisitor visitField(int access, String actual, String desc, String signature, Object value) {
                    if (field && name.equals(actual) && descriptor.equals(desc)) found.incrementAndGet();
                    return null;
                }
                @Override
                public MethodVisitor visitMethod(int access, String actual, String desc, String signature, String[] errors) {
                    if (!field && name.equals(actual) && descriptor.equals(desc)) found.incrementAndGet();
                    return null;
                }
            }, ClassReader.SKIP_CODE);
        }
        check(found.get() == 1, "mixin target " + className + "." + name + descriptor);
    }

    public static void main(String[] args) throws Exception {
        var queue = new HitQueueTodoAi<Object, String>(3);
        Object alice = new Object();
        Object bob = new Object();
        var first = queue.schedule(alice, "a", 0, 20);
        var second = queue.schedule(bob, "b", 0, 20);
        check(queue.current(1) == first && first.target() == alice, "FIFO retains exact selected entity");
        queue.finish(first, SENT);
        check(first.completion().toCompletableFuture().join() == SENT, "dispatch result");
        check(queue.current(1) == second && second.target() == bob, "second target only after first completed");
        second.cancel();
        check(queue.current(2) == null, "cancelled request cannot run");
        check(second.completion().toCompletableFuture().join() == CANCELLED, "cancellation result");
        var slow = queue.schedule(alice, "a", 2, 20);
        var expiresBehindSlow = queue.schedule(bob, "b", 2, 1);
        check(queue.current(3) == slow, "head waits without switching targets");
        check(expiresBehindSlow.completion().toCompletableFuture().join() == EXPIRED, "queued timeout");
        queue.clear(DISCONNECTED);
        check(slow.completion().toCompletableFuture().join() == DISCONNECTED, "disconnect clears requests");
        check(queue.current(5) == null, "no stale target after world change");
        var reentrant = queue.schedule(alice, "a", 5, 1);
        reentrant.completion().thenRun(() -> queue.schedule(bob, "b", 6, 10));
        check(queue.current(6).target() == bob, "completion callback can enqueue without iterator corruption");
        queue.clear(CANCELLED);
        queue.schedule(alice, "a", 6, 10);
        queue.schedule(alice, "a", 6, 10);
        queue.schedule(bob, "b", 6, 10);
        try {
            queue.schedule(bob, "b", 6, 10);
            throw new AssertionError("queue must be bounded");
        } catch (IllegalStateException expected) { checks++; }

        Vec3 eyes = new Vec3(0, 1, 0);
        AABB box = new AABB(2, 0, -0.3, 2.6, 1.8, 0.3);
        var away = new AuraAimTodoAi.Rotation(90, 0);
        var aim = AuraAimTodoAi.find(eyes, box, away, 3, 0, 0.5, _ -> true, _ -> true);
        check(aim != null && aim.rotation().yaw() != away.yaw(), "turn toward selected player behind camera");
        check(AuraAimTodoAi.trace(eyes, box, aim.rotation(), 3, 0, _ -> true, _ -> true) != null,
                "quantized rotation actually intersects target");
        check(AuraAimTodoAi.find(eyes, box, away, 1, 0, 0.5, _ -> true, _ -> true) == null,
                "outside range stays pending");
        check(AuraAimTodoAi.find(eyes, box, away, 3, 0, 0.5, _ -> false, _ -> true) == null,
                "blocked ray cannot hit with zero wall range");
        check(AuraAimTodoAi.find(eyes, box, away, 3, 3, 0.5, _ -> false, _ -> true) != null,
                "explicit wall range supported");
        check(AuraAimTodoAi.find(eyes, box, away, 3, 0, 0.5, _ -> true, _ -> false) == null,
                "intervening entity cannot be substituted");
        var partial = AuraAimTodoAi.find(eyes, box, away, 3, 0, 0.5,
                point -> point.y > 1.2, _ -> true);
        check(partial != null && partial.hit().y > 1.2, "search visible box points when center is blocked");
        AABB containsEyes = new AABB(-1, 0, -1, 1, 2, 1);
        check(AuraAimTodoAi.trace(eyes, containsEyes, away, 3, 0, _ -> true, _ -> true) != null,
                "overlapping hitbox");

        var clock = new AttackClockTodoAi();
        AtomicInteger dispatched = new AtomicInteger();
        clock.tick(1000, false, 10, 10, false);
        check(clock.ready() && clock.consume(() -> { dispatched.incrementAndGet(); return true; }),
                "first planned click");
        check(!clock.consume(() -> { dispatched.incrementAndGet(); return true; }),
                "cannot dispatch same click twice");
        clock.tick(1050, false, 10, 10, false);
        check(!clock.ready(), "constant timing waits between clicks");
        clock.tick(1100, false, 10, 10, false);
        check(clock.ready(), "next click scheduled at configured interval");
        clock.consume(() -> false);
        check(!clock.consume(() -> true), "failed attempt consumes planned click");
        clock.tick(1150, false, 10, 10, true);
        check(clock.ready() && clock.consume(() -> { dispatched.incrementAndGet(); return true; }),
                "cooldown crossing enforces a click");
        check(dispatched.get() == 2, "only successful dispatched actions counted");
        member("net/minecraft/client/multiplayer/MultiPlayerGameMode", "ensureHasSentCarriedItem", "()V", false);
        member("net/minecraft/world/entity/player/Player", "getEnchantedDamage",
                "(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)F", false);
        member("net/minecraft/world/entity/LivingEntity", "attackStrengthTicker", "I", true);
        member("net/minecraft/world/entity/LivingEntity", "autoSpinAttackDmg", "F", true);
        System.out.println("PASS: " + checks + " queue, aiming, occlusion, upstream click-planner and accessor checks");
    }
}
