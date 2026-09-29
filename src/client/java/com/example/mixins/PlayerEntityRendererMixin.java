package com.example.mixins;

import com.example.Configs.Config;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// codex start
// codex (old code) import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
// codex end
import net.minecraft.network.chat.Component;

import static com.example.Constants.MINECRAFT_CLIENT_INSTANCE;
import static com.example.UntitledClient.config;

// codex start
// codex (old code) @Mixin(AvatarRenderer.class)
@Mixin(PlayerRenderer.class)
// codex end
public class PlayerEntityRendererMixin {
//    @Inject(at = @At(value = "RETURN"), method = "getArmPose(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/util/Arm;)Lnet/minecraft/client/render/entity/model/BipedEntityModel$ArmPose;", cancellable = true)
//    private static void onGetArmPose(AbstractClientPlayerEntity player, Arm arm, CallbackInfoReturnable<BipedEntityModel.ArmPose> cir) {
//        // TODO
//    }
//    @Inject(at = @At(value = "RETURN"), method = "getArmPose(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/Hand;)Lnet/minecraft/client/render/entity/model/BipedEntityModel$ArmPose;", cancellable = true)
//    private static void onGetArmPose(PlayerEntity player, ItemStack stack, Hand hand, CallbackInfoReturnable<BipedEntityModel.ArmPose> cir) {
//        // TODO -> disable other player's being left-handed
//    }

    // the livingEntity one DOES NOT get called!
    @Inject(at = @At(value = "RETURN"), method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V")
    private void onExtractRenderState(
            Entity livingEntity,
            EntityRenderState renderState,
            float par3,
            CallbackInfo ci) {
        if (config.isPlayerNameplateSimplified) {
            renderState.nameTag = livingEntity.getName();
        }
        if (renderState.nameTag instanceof Component text &&
                config.nameplateUuids.get(livingEntity.getUUID()) instanceof Config.NameplateTeam team) {
            renderState.nameTag = text.copy().setStyle(text.getStyle().withColor(team.color.getValue()));
        }
    }

//    @Inject(method = "<init>", at = @At("TAIL"))
//    private void replaceArmorFeature(
//            EntityRendererFactory.Context ctx,
//            boolean slim,
//            CallbackInfo ci
//    ) {
//        if (config.nameplateUuids.get(abstractClient)) {
//            return;
//        }
//
//        List<FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel>> features = ((LivingEntityRendererAccessor<PlayerEntityRenderState, PlayerEntityModel>) (Object) this).getFeatures();
//        features.removeIf(feature ->
//                feature instanceof ArmorFeatureRenderer<?, ?, ?>
//        );
//        // TODO ?
//        features.add(
//                new TintedArmorFeatureRenderer(
//                        (FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel>) this,
//                        new ArmorEntityModel(ctx.getPart(slim ? EntityModelLayers.PLAYER_SLIM_INNER_ARMOR : EntityModelLayers.PLAYER_INNER_ARMOR)),
//                        new ArmorEntityModel(ctx.getPart(slim ? EntityModelLayers.PLAYER_SLIM_OUTER_ARMOR : EntityModelLayers.PLAYER_OUTER_ARMOR)),
//                        ctx.getEquipmentRenderer()
//                )
//        );
//    }
}
