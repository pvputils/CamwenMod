package com.example.gametestmixins;

import net.fabricmc.fabric.impl.client.gametest.threading.ThreadingImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Test-only guard for Fabric's asynchronous initial test-thread registration race. */
@Mixin(value = ThreadingImpl.class, remap = false)
public abstract class TestThreadRegistrationTodoAi {
    @Inject(method = "runTestThread", at = @At("TAIL"))
    private static void awaitRegistration(Runnable runner, CallbackInfo ci) {
        long deadline = System.nanoTime() + 2_000_000_000L;
        while (ThreadingImpl.PHASER.getRegisteredParties() < 2) {
            if (System.nanoTime() > deadline) throw new AssertionError("Fabric test thread did not register");
            Thread.onSpinWait();
        }
    }
}
