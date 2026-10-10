/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
@file:Suppress("TooManyFunctions")

package net.ccbluex.liquidbounce.utils.combat

import it.unimi.dsi.fastutil.objects.ObjectDoubleImmutablePair
import it.unimi.dsi.fastutil.objects.ObjectDoublePair
import net.ccbluex.fastutil.component1
import net.ccbluex.fastutil.component2
import net.ccbluex.liquidbounce.config.types.list.TaggedTodoLiquidbounce
import net.ccbluex.liquidbounce.features.addon.AddonApiTodoLiquidbounce
import net.ccbluex.liquidbounce.features.global.GlobalSettingsTargetTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.isWithinWorldBorder
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.world.getEntitiesInCube
import net.minecraft.client.CameraType
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.Attackable
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player

/**
 * Global target configurable
 *
 * Modules can have their own enemy configurable if required. If not, they should use this as default.
 * Global enemy configurable can be used to configure which entities should be considered as a target.
 *
 * This can be adjusted by the .target command and the panel inside the ClickGUI.
 */
@AddonApiTodoLiquidbounce
data class EntityTargetingInfoTodoLiquidbounce(val classification: EntityTargetClassificationTodoLiquidbounce, val isFriend: Boolean) {
    companion object {
        @JvmField
        val DEFAULT = EntityTargetingInfoTodoLiquidbounce(EntityTargetClassificationTodoLiquidbounce.TARGET, false)
    }
}

@AddonApiTodoLiquidbounce
enum class EntityTargetClassificationTodoLiquidbounce {
    TARGET,
    INTERESTING,
    IGNORED
}

/**
 * Configurable to configure which entities and their state (like being dead) should be considered as a target
 */
enum class TargetsTodoLiquidbounce(override val tag: String) : TaggedTodoLiquidbounce {
    SELF("Self"),
    PLAYERS("Players"),
    // codex start
    // HOSTILE("Hostile"),
    // codex end
    // codex start
    // ANGERABLE("Angerable"),
    // codex end
    // codex start
    // WATER_CREATURE("WaterCreature"),
    // codex end
    // codex start
    // PASSIVE("Passive"),
    // codex end
    // codex start
    // ARMOR_STAND("ArmorStand"),
    // codex end
    INVISIBLE("Invisible"),
    DEAD("Dead"),
    SLEEPING("Sleeping"),
    FRIENDS("Friends");
}

private fun Set<TargetsTodoLiquidbounce>.shouldAttack(entity: Entity): Boolean {
    if (entity === player || entity.hasPassenger(player)) {
        return false
    }

    val info = EntityTaggingManagerTodoLiquidbounce.getTag(entity).targetingInfo

    return when {
        info.isFriend && TargetsTodoLiquidbounce.FRIENDS !in this -> false
        info.classification === EntityTargetClassificationTodoLiquidbounce.TARGET -> isInteresting(entity, info)
        else -> false
    }
}

private fun Set<TargetsTodoLiquidbounce>.shouldShow(entity: Entity): Boolean {
    if (entity === player || entity.hasPassenger(player)) {
        return TargetsTodoLiquidbounce.SELF in this &&
            (mc.options.cameraType !== CameraType.FIRST_PERSON) //codex (|| ModuleFreeCam.enabled)
    }

    val info = EntityTaggingManagerTodoLiquidbounce.getTag(entity).targetingInfo

    return when {
        info.isFriend && TargetsTodoLiquidbounce.FRIENDS !in this -> false
        info.classification !== EntityTargetClassificationTodoLiquidbounce.IGNORED -> isInteresting(entity, info)
        else -> false
    }
}

/**
 * Check if an entity is considered a target
 */
@Suppress("CyclomaticComplexMethod", "ReturnCount")
private fun Set<TargetsTodoLiquidbounce>.isInteresting(suspect: Entity, info: EntityTargetingInfoTodoLiquidbounce): Boolean {
    // Check if the enemy is living and not dead (or ignore being dead)
    if (suspect !is LivingEntity || !(TargetsTodoLiquidbounce.DEAD in this || suspect.isAlive)) {
        return false
    }

    // Check if enemy is invisible (or ignore being invisible)
    if (TargetsTodoLiquidbounce.INVISIBLE !in this && suspect.isInvisible) {
        return false
    }

    // Check if enemy is a player and should be considered as a target
    return when (suspect) {
        is Player -> when {
            suspect === mc.player -> false
            // Check if enemy is sleeping (or ignore being sleeping)
            suspect.isSleeping && TargetsTodoLiquidbounce.SLEEPING !in this -> false
            // Allow targeting friends even when Players is disabled, as long as Friends is enabled
            else -> TargetsTodoLiquidbounce.PLAYERS in this || (info.isFriend && TargetsTodoLiquidbounce.FRIENDS in this)
        }
        // codex start
        // is WaterAnimal -> Targets.WATER_CREATURE in this
        // codex end
        // codex start
        // is AgeableMob, is Bat, is Allay -> Targets.PASSIVE in this
        // codex end
        // codex start
        // is ArmorStand -> Targets.ARMOR_STAND in this
        // codex end
        // codex start
        // is Monster, is Enemy -> Targets.HOSTILE in this
        // codex end
        // codex start
        // is NeutralMob -> Targets.ANGERABLE in this
        // codex end

        else -> false
    }
}

// Extensions
@AddonApiTodoLiquidbounce
// codex start
// @JvmOverloads
// codex end
fun Entity?.shouldBeShown(enemyConf: Set<TargetsTodoLiquidbounce> = GlobalSettingsTargetTodoLiquidbounce.visual) =
    this?.let { enemyConf.shouldShow(it) } ?: false

@AddonApiTodoLiquidbounce
// codex start
// @JvmOverloads
// codex end
fun Entity?.shouldBeAttacked(enemyConf: Set<TargetsTodoLiquidbounce> = GlobalSettingsTargetTodoLiquidbounce.combat) =
    this is Attackable && enemyConf.shouldAttack(this) && this.isWithinWorldBorder

/**
 * Mirrors the vanilla server-side invalid attack disconnect checks
 *
 * @see net.minecraft.server.network.ServerGamePacketListenerImpl.handleAttack
 */
// codex start
// private fun Entity.canBeAttackedWithVanillaPacket() =
//     this !is ItemEntity &&
//         this !is ExperienceOrb &&
//         this !== player &&
//         (this !is AbstractArrow || this.isAttackable)
//
// /**
//  * Find the best enemy in the current world in a specific range.
//  */
// codex end

@AddonApiTodoLiquidbounce
// codex start
// @JvmOverloads
// codex end
fun ClientLevel.findEnemy(
    range: ClosedFloatingPointRange<Float>,
    enemyConf: Set<TargetsTodoLiquidbounce> = GlobalSettingsTargetTodoLiquidbounce.combat
) = findEnemy(range.start, range.endInclusive, enemyConf)

/**
 * Find the best enemy in the current world in a specific range.
 */
@AddonApiTodoLiquidbounce
// codex start
// @JvmOverloads
// codex end
fun ClientLevel.findEnemy(
    minRange: Float,
    maxRange: Float,
    enemyConf: Set<TargetsTodoLiquidbounce> = GlobalSettingsTargetTodoLiquidbounce.combat
) = findEnemies(minRange, maxRange, enemyConf)
    .minByOrNull { (_, distSqr) -> distSqr }?.key()

@AddonApiTodoLiquidbounce
// codex start
// @JvmOverloads
// codex end
fun ClientLevel.findEnemies(
    minRange: Float,
    maxRange: Float,
    enemyConf: Set<TargetsTodoLiquidbounce> = GlobalSettingsTargetTodoLiquidbounce.combat
): List<ObjectDoublePair<Entity>> {
    val minRangeSqr = minRange * minRange
    val maxRangeSqr = maxRange * maxRange
    val result = ArrayList<ObjectDoubleImmutablePair<Entity>>()

    getEntitiesInCube(player.eyePosition, maxRange.toDouble()) {
        it.shouldBeAttacked(enemyConf)
    }.forEach { entity ->
        val distSqr = entity.squaredBoxedDistanceTo(player)
        if (distSqr in minRangeSqr..maxRangeSqr) {
            result += ObjectDoubleImmutablePair(entity, distSqr)
        }
    }

    return result
}
// codex start
//
// inline fun ClientLevel.getEntitiesBoxInRange(
//     midPos: Vec3,
//     range: Double,
//     crossinline predicate: (Entity) -> Boolean = { true }
// ): MutableList<Entity> {
//     val rangeSquared = range * range
//
//     return getEntitiesInCube(midPos, range) {
//         predicate(it) && it.squaredBoxedDistanceTo(midPos) <= rangeSquared
//     }
// }
//
// /**
//  * @see net.minecraft.client.Minecraft.startAttack
//  * @return attacked or pierced
//  */
// codex end
// codex start
// @AddonApi
// @Suppress("CognitiveComplexMethod")
// codex end
// codex start
// @JvmOverloads
// codex end
// codex start
// fun attackEntity(entity: Entity, swing: SwingMode): Boolean { //codex (keepSprint: Boolean = false)
//     val itemStack = player.getItemInHand(InteractionHand.MAIN_HAND)
//     val piercingWeapon = itemStack.get(DataComponents.PIERCING_WEAPON)
//
//     // Minecraft introduced piercing weapons that have their own attack method.
//     // You HAVE to look at the entity before attacking it.
//     if (piercingWeapon != null && !interaction.isSpectator) {
//         interaction.piercingAttack(itemStack.attackAnimation, piercingWeapon)
//         swing.swing(InteractionHand.MAIN_HAND)
//         return true
//     }
//
//     if (!entity.canBeAttackedWithVanillaPacket()
//         || EventManager.callEvent(AttackEntityEvent(entity)).isCancelled) {
//         return false
//     }
//
//     with(player) {
//         // Swing before attacking (on 1.8)
//         if (isOlderThanOrEqual1_8) {
//             swing.swing(InteractionHand.MAIN_HAND)
//         }
//
//         interaction.ensureHasSentCarriedItem()
//         network.send(ServerboundAttackPacket(entity.id))
//
//         // codex start
//         // if (keepSprint) {
//         //     var genericAttackDamage =
//         //         if (this.isAutoSpinAttack) {
//         //             this.autoSpinAttackDmg
//         //         } else {
//         //             getAttributeValue(Attributes.ATTACK_DAMAGE).toFloat()
//         //         }
//         //     val damageSource = this.damageSources().playerAttack(this)
//         //     var enchantAttackDamage = this.getEnchantedDamage(entity, genericAttackDamage,
//         //         damageSource) - genericAttackDamage
//         //
//         //     val attackCooldown = this.getAttackStrengthScale(0.5f)
//         //     genericAttackDamage *= 0.2f + attackCooldown * attackCooldown * 0.8f
//         //     enchantAttackDamage *= attackCooldown
//         //
//         //     if (genericAttackDamage > 0.0f || enchantAttackDamage > 0.0f) {
//         //         if (enchantAttackDamage > 0.0f) {
//         //             this.magicCrit(entity)
//         //         }
//         //
//         //         // codex start
//         //         // if (ModuleCriticals.wouldDoCriticalHit(true)) {
//         //         //     world.playSound(
//         //         //         null, x, y, z, SoundEvents.PLAYER_ATTACK_CRIT,
//         //         //         soundSource, 1.0f, 1.0f
//         //         //     )
//         //         //     crit(entity)
//         //         // }
//         //         // codex end
//         //     }
//         // } else {
//         //     if (interaction.playerMode != GameType.SPECTATOR) {
//         //         attack(entity)
//         //     }
//         // }
//         // codex end
//
//             if (interaction.playerMode != GameType.SPECTATOR) {
//                 attack(entity)
//             }
//
//
//         // Reset cooldown
//         this.attackStrengthTicker = 0
//
//         // Swing after attacking (on 1.9+)
//         if (!isOlderThanOrEqual1_8) {
//             swing.swing(InteractionHand.MAIN_HAND)
//         }
//     }
//
//     return true
// }
// codex end
