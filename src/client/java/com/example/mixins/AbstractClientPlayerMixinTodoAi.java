package com.example.mixins;

import com.example.Configs.CheatConfig;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static com.example.Constants.MINECRAFT_CLIENT_INSTANCE;
import static com.example.UntitledClient.config;
import static com.example.UntitledClient.isEthyleneSprintFovCancelled;
import static com.example.Utils.computeCheatConfig;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixinTodoAi {
    // TODO gl
//    @Unique
//    private static final Identifier SPRINTING_MODIFIER_ID = Identifier.withDefaultNamespace("sprinting");
//
//    @Redirect(
//            method = "getFieldOfViewModifier",
//            at = @At(
//                    value = "INVOKE",
//                    target = "Lnet/minecraft/client/player/AbstractClientPlayer;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
//    private double getEthyleneFovMovementSpeed(
//            AbstractClientPlayer player, Holder<Attribute> attribute) {
//        double movementSpeed = player.getAttributeValue(attribute);
//        CheatConfig cheatConfig = computeCheatConfig();
//        if (!config.isCheatsEnabled ||
//                !cheatConfig.isEthylene ||
//                player != MINECRAFT_CLIENT_INSTANCE.player ||
//                !player.isSprinting() ||
//                attribute != Attributes.MOVEMENT_SPEED) {
//            isEthyleneSprintFovCancelled = false;
//            return movementSpeed;
//        }
//
//        if (!isEthyleneSprintFovCancelled) {
//            return movementSpeed;
//        }
//
//        AttributeInstance movementSpeedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
//        AttributeModifier sprintingModifier = movementSpeedAttribute.getModifier(SPRINTING_MODIFIER_ID);
//        return sprintingModifier == null
//                ? movementSpeed
//                : movementSpeed / (1.0D + sprintingModifier.amount());
//    }
}
