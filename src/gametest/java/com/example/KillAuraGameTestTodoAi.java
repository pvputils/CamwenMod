package com.example;
import com.example.killaura.*;
import com.example.aimassist.AimGeometryTodoAi;
import com.example.aimassist.AimAssistMarginScopeTodoAi;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.*;
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
                com.example.aimassist.AimAssistControllerTodoAi.config().targetting.enabled=false;
                var eyes=mc.player.getEyePosition();
                var victim=new RemotePlayer(mc.level,new GameProfile(UUID.randomUUID(),"MarginRegression"));
                victim.setId(1_100_003);
                victim.setPos(eyes.x+0.75,mc.player.getY(),eyes.z+2.3);
                victim.setBoundingBox(new AABB(eyes.x+0.45,eyes.y-0.5,eyes.z+2,eyes.x+1.05,eyes.y+0.5,eyes.z+2.6));
                mc.level.addEntity(victim);
                mc.player.setYRot(0);mc.player.setXRot(0);
                var miss=BlockHitResult.miss(eyes.add(0,0,5),Direction.NORTH,BlockPos.containing(eyes.add(0,0,5)));
                mc.hitResult=miss;
                double reach=mc.player.entityInteractionRange();
                cfg.margin=0.1;
                KillAuraControllerTodoAi.tick(mc);KillAuraControllerTodoAi.render(mc,1);
                check(KillAuraControllerTodoAi.target()==null&&mc.player.getYRot()==0,"outside margin never acquires or rotates");
                check(!KillAuraControllerTodoAi.ownsLook(mc),"inactive correction releases existing aim assist");
                cfg.margin=0.6;
                UntitledClient.config.isCheatsEnabled=false;
                KillAuraControllerTodoAi.tick(mc);
                check(KillAuraControllerTodoAi.target()==victim,"margin hit acquires player even with global cheats disabled");
                KillAuraControllerTodoAi.render(mc,1);
                check(mc.player.getYRot()<0,"margin miss correction turns native camera");
                check(mc.player.entityInteractionRange()==reach,"margin probe never changes interaction reach");
                check(mc.hitResult==miss,"probe never replaces manual attack hit result");
                check(KillAuraMarginPickTodoAi.current()==null&&AimAssistMarginScopeTodoAi.current()==null,"probe restores both scopes");
                var look=AimGeometryTodoAi.lookAt(mc.player.getEyePosition(),victim.getBoundingBox().getCenter());
                mc.player.setYRot((float)look.yaw());mc.player.setXRot((float)look.pitch());
                KillAuraControllerTodoAi.tick(mc);
                check(KillAuraControllerTodoAi.target()==null&&KillAuraControllerTodoAi.paused(),"fresh normal player hit stops correction even when stored result is stale");
                float yaw=mc.player.getYRot(),pitch=mc.player.getXRot();
                KillAuraControllerTodoAi.render(mc,1);
                check(mc.player.getYRot()==yaw&&mc.player.getXRot()==pitch,"normal hit leaves look untouched");
                mc.player.setYRot(0);mc.player.setXRot(0);
                mc.hitResult=new EntityHitResult(victim);
                KillAuraControllerTodoAi.tick(mc);
                check(KillAuraControllerTodoAi.target()==null,"stored entity hit cannot activate correction");
                mc.hitResult=new BlockHitResult(eyes,Direction.NORTH,BlockPos.containing(eyes),false);
                KillAuraControllerTodoAi.tick(mc);
                check(KillAuraControllerTodoAi.target()==null,"stored block hit cannot activate correction");
                mc.hitResult=miss;
                KillAuraControllerTodoAi.tick(mc);KillAuraControllerTodoAi.render(mc,1);
                check(KillAuraControllerTodoAi.target()==victim&&mc.player.getYRot()<0,"margin correction resumes after looking away");
                mc.player.setYRot(0);mc.player.setXRot(0);
                UntitledClient.config.isCheatsEnabled=true;
                Utils.computeCheatConfig().movingTargetingMarginBypass=0.9f;
                KillAuraControllerTodoAi.tick(mc);
                check(KillAuraControllerTodoAi.target()==victim,"probe ignores separately configured global margin");
                check(Utils.computeCheatConfig().movingTargetingMarginBypass==0.9f,"probe never changes persisted attack margin");
                cfg.margin=0;
                KillAuraControllerTodoAi.render(mc,1);
                check(KillAuraControllerTodoAi.target()==null&&mc.player.getYRot()==0,"render drops stale plan immediately when margin shrinks");
                cfg.enabled=false;
                KillAuraControllerTodoAi.tick(mc);
                check(KillAuraControllerTodoAi.target()==null&&!KillAuraControllerTodoAi.ownsLook(mc),"disable releases camera");
                KillAuraScreenTodoAi.open();
                check(mc.gui.screen() instanceof KillAuraScreenTodoAi,"native settings screen opens");
                mc.gui.screen().onClose();
                victim.discard();
            });
            System.out.println("PASS: margin-only miss correction, normal/entity/block hit rejection, pause/resume, scoped probes, unchanged reach/hit result, disable and screen");
        }
    }
}
