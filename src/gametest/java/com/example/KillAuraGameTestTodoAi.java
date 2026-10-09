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
                cfg.rotations.movementCorrection=KillAuraConfigTodoAi.MovementCorrection.CHANGE_LOOK;
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
                for(var mode:new KillAuraConfigTodoAi.MovementCorrection[]{KillAuraConfigTodoAi.MovementCorrection.OFF,
                        KillAuraConfigTodoAi.MovementCorrection.STRICT,KillAuraConfigTodoAi.MovementCorrection.SILENT}) {
                    cfg.rotations.movementCorrection=mode;
                    mc.player.setYRot(0);mc.player.setXRot(0);mc.hitResult=miss;
                    KillAuraControllerTodoAi.tick(mc);KillAuraControllerTodoAi.render(mc,1);
                    var managed=KillAuraControllerTodoAi.managedRotation(mc);
                    check(managed!=null&&managed.yaw()<0,"hidden aim planned: "+mode);
                    check(mc.player.getYRot()==0&&mc.player.getXRot()==0,"hidden mode preserves camera: "+mode);
                    var savedInput=mc.player.input;
                    boolean up=mc.options.keyUp.isDown();
                    try {
                        mc.options.keyUp.setDown(true);
                        mc.player.input=new net.minecraft.client.player.KeyboardInput(mc.options);
                        mc.player.input.tick();
                        var actualInput=mc.player.input.keyPresses;
                        cfg.enabled=false;
                        mc.player.input.tick();
                        var rawInput=mc.player.input.keyPresses;
                        cfg.enabled=true;
                        var expectedInput=KillAuraRotationModesTodoAi.transform(rawInput,mc.player.getYRot(),managed,mode);
                        check(actualInput.equals(expectedInput),"real keyboard hook applies only Silent correction: "+mode);
                    } finally {mc.options.keyUp.setDown(up);mc.player.input=savedInput;}
                    mc.player.setDeltaMovement(Vec3.ZERO);
                    mc.player.moveRelative(1,new Vec3(0,0,1));
                    double movementYaw=Math.toRadians(mode==KillAuraConfigTodoAi.MovementCorrection.OFF?0:managed.yaw());
                    check(Math.abs(mc.player.getDeltaMovement().x+Math.sin(movementYaw))<1e-5,"native strafe hook uses correct yaw: "+mode);
                    try {
                        var send=net.minecraft.client.player.LocalPlayer.class.getDeclaredMethod("sendPosition");
                        var yawLast=net.minecraft.client.player.LocalPlayer.class.getDeclaredField("yRotLast");
                        var pitchLast=net.minecraft.client.player.LocalPlayer.class.getDeclaredField("xRotLast");
                        send.setAccessible(true);yawLast.setAccessible(true);pitchLast.setAccessible(true);
                        send.invoke(mc.player);
                        check(Math.abs(yawLast.getFloat(mc.player)-managed.yaw())<1e-5&&Math.abs(pitchLast.getFloat(mc.player)-managed.pitch())<1e-5,"vanilla movement packet uses managed angles: "+mode);
                        check(mc.player.getYRot()==0&&mc.hitResult==miss,"packet hook preserves camera/manual pick: "+mode);
                    } catch(ReflectiveOperationException error) {throw new AssertionError(error);}
                    mc.player.setSprinting(true);mc.player.setDeltaMovement(Vec3.ZERO);mc.player.jumpFromGround();
                    check(Math.abs(mc.player.getDeltaMovement().x+Math.sin(movementYaw)*0.2)<1e-5,"sprint jump follows movement correction: "+mode);
                    mc.player.setSprinting(false);mc.player.setDeltaMovement(Vec3.ZERO);
                }
                cfg.margin=0;
                KillAuraControllerTodoAi.render(mc,1);
                check(KillAuraControllerTodoAi.target()==null&&mc.player.getYRot()==0,"render drops stale plan immediately when margin shrinks");
                check(KillAuraControllerTodoAi.managedRotation(mc)==null,"failed margin gate clears hidden rotation");
                cfg.enabled=false;
                KillAuraControllerTodoAi.tick(mc);
                check(KillAuraControllerTodoAi.target()==null&&!KillAuraControllerTodoAi.ownsLook(mc),"disable releases camera");
                cfg.rotations.movementCorrection=KillAuraConfigTodoAi.MovementCorrection.CHANGE_LOOK;
                KillAuraScreenTodoAi.open();
                check(mc.gui.screen() instanceof KillAuraScreenTodoAi,"native settings screen opens");
                mc.gui.screen().onClose();
                victim.discard();
            });
            System.out.println("PASS: margin-only miss correction, normal/entity/block hit rejection, pause/resume, scoped probes, unchanged reach/hit result, disable, screen, hidden packet rotations and movement modes");
        }
    }
}
