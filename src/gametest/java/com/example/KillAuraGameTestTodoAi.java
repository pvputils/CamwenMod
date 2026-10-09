package com.example;
import com.example.killaura.*;
import com.example.aimassist.AimGeometryTodoAi;
import com.example.aimassist.AimAssistMarginScopeTodoAi;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.player.RemotePlayer;
import java.util.UUID;
public final class KillAuraGameTestTodoAi implements FabricClientGameTest {
    private static void check(boolean value,String message) {if(!value)throw new AssertionError(message);}
    @Override public void runTest(ClientGameTestContext context) {
        try(var world=context.worldBuilder().create()) {
            context.waitFor(mc->mc.player!=null&&mc.level!=null&&mc.gui.screen()==null);
            context.waitTicks(3);
            context.runOnClient(mc->{
                var cfg=KillAuraControllerTodoAi.config();
                cfg.enabled=true;
                cfg.rotations.horizontalMin=cfg.rotations.horizontalMax=15;
                cfg.rotations.verticalMin=cfg.rotations.verticalMax=15;
                cfg.range.scanRangeMin=cfg.range.scanRangeMax=0;
                var victim=new RemotePlayer(mc.level,new GameProfile(UUID.randomUUID(),"AimOnlyRegression"));
                victim.setId(1_100_003);
                victim.setPos(mc.player.getX()+1.5,mc.player.getY(),mc.player.getZ()+2);
                mc.level.addEntity(victim);
                mc.player.setYRot(0);mc.player.setXRot(0);
                double reach=mc.player.entityInteractionRange();
                KillAuraControllerTodoAi.tick(mc);
                check(KillAuraControllerTodoAi.target()==victim,"player acquired without pressing attack");
                KillAuraControllerTodoAi.render(mc,1);
                check(mc.player.getYRot()<0,"native camera turns toward player");
                check(mc.player.entityInteractionRange()==reach,"aim range never changes interaction reach");
                var look=AimGeometryTodoAi.lookAt(mc.player.getEyePosition(),victim.getBoundingBox().getCenter());
                mc.player.setYRot((float)look.yaw());mc.player.setXRot((float)look.pitch());
                KillAuraControllerTodoAi.tick(mc);
                check(KillAuraControllerTodoAi.paused(),"normal crosshair on selected player pauses aiming");
                float yaw=mc.player.getYRot(),pitch=mc.player.getXRot();
                KillAuraControllerTodoAi.render(mc,1);
                check(mc.player.getYRot()==yaw&&mc.player.getXRot()==pitch,"paused render leaves look untouched");
                check(AimAssistMarginScopeTodoAi.current()==null,"vanilla probe restores margin scope");
                mc.player.setYRot(0);mc.player.setXRot(0);
                KillAuraControllerTodoAi.tick(mc);KillAuraControllerTodoAi.render(mc,1);
                check(!KillAuraControllerTodoAi.paused()&&mc.player.getYRot()<0,"aim resumes after looking away");
                cfg.enabled=false;
                KillAuraControllerTodoAi.tick(mc);
                check(KillAuraControllerTodoAi.target()==null&&!KillAuraControllerTodoAi.ownsLook(mc),"disable releases target and camera");
                KillAuraScreenTodoAi.open();
                check(mc.gui.screen() instanceof KillAuraScreenTodoAi,"native settings screen opens");
                mc.gui.screen().onClose();
                victim.discard();
            });
            System.out.println("PASS: player acquisition, visible rotation, normal-crosshair pause/resume, unchanged reach, disable and native screen");
        }
    }
}
