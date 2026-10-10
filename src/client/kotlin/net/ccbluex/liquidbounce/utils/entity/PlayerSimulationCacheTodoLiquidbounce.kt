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

package net.ccbluex.liquidbounce.utils.entity

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectImmutableList
import net.ccbluex.liquidbounce.event.EventListenerTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.GameTickEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.MovementInputEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConventionTodoLiquidbounce.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConventionTodoLiquidbounce.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConventionTodoLiquidbounce.MODEL_STATE
import net.ccbluex.liquidbounce.utils.movement.DirectionalInputTodoLiquidbounce
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

object PlayerSimulationCacheTodoLiquidbounce: EventListenerTodoLiquidbounce {
    private val otherPlayerCache = ConcurrentHashMap<Player, SimulatedPlayerCacheTodoLiquidbounce>()
    private var localPlayerCache: SimulatedPlayerCacheTodoLiquidbounce? = null

    @Suppress("unused")
    private val gameTickHandler = handler<GameTickEventTodoLiquidbounce>(priority = FIRST_PRIORITY) {
        this.otherPlayerCache.clear()
    }

    @Suppress("unused")
    private val criticalMovementHandler = handler<MovementInputEventTodoLiquidbounce>(
        priority = CRITICAL_MODIFICATION
    ) { event ->
        this.localPlayerCache = null
        updatePlayerCache(event.directionalInput)
    }

    @Suppress("unused")
    private val movementHandler = handler<MovementInputEventTodoLiquidbounce> { event ->
        updatePlayerCache(event.directionalInput, verify = true)
    }

    @Suppress("unused")
    private val modalMovementHandler = handler<MovementInputEventTodoLiquidbounce>(
        priority = MODEL_STATE
    ) { event ->
        updatePlayerCache(event.directionalInput, verify = true)
    }

    /**
     * Updates the cache for the local player,
     * this will be called on every movement input event
     * to ensure the cache is up to date.
     *
     * @param directionalInput the input to update the cache with
     */
    private fun updatePlayerCache(directionalInput: DirectionalInputTodoLiquidbounce, verify: Boolean = false) {
        // Check if we even need to update the cache
        if (verify && localPlayerCache?.simulatedPlayer?.input?.directionalInput == directionalInput) {
            return
        }

        val simulatedPlayer = SimulatedPlayerTodoLiquidbounce.fromClientPlayer(
            SimulatedPlayerTodoLiquidbounce.SimulatedPlayerInput.fromClientPlayer(directionalInput)
        )

        localPlayerCache = SimulatedPlayerCacheTodoLiquidbounce(simulatedPlayer)
    }

    fun getSimulationForOtherPlayers(player: Player): SimulatedPlayerCacheTodoLiquidbounce {
        return otherPlayerCache.computeIfAbsent(player) {
            val simulatedPlayer = SimulatedPlayerTodoLiquidbounce.fromOtherPlayer(
                it,
                SimulatedPlayerTodoLiquidbounce.SimulatedPlayerInput.guessInput(it)
            )

            SimulatedPlayerCacheTodoLiquidbounce(simulatedPlayer)
        }
    }

    fun getSimulationForLocalPlayer(): SimulatedPlayerCacheTodoLiquidbounce {
        val cached = localPlayerCache

        if (cached != null) {
            return cached
        }

        val simulatedPlayer = SimulatedPlayerTodoLiquidbounce.fromClientPlayer(
            SimulatedPlayerTodoLiquidbounce.SimulatedPlayerInput.fromClientPlayer(DirectionalInputTodoLiquidbounce(player.input))
        )

        val simulatedPlayerCache = SimulatedPlayerCacheTodoLiquidbounce(simulatedPlayer)

        localPlayerCache = simulatedPlayerCache

        return simulatedPlayerCache
    }
}

class SimulatedPlayerCacheTodoLiquidbounce(internal val simulatedPlayer: SimulatedPlayerTodoLiquidbounce) {
    private var currentSimulationStep = 0
    private val simulationSteps = ObjectArrayList<SimulatedPlayerSnapshotTodoLiquidbounce>().apply {
        add(SimulatedPlayerSnapshotTodoLiquidbounce(simulatedPlayer))
    }
    private val lock = ReentrantReadWriteLock()

    fun simulateUntil(ticks: Int) {
        check(ticks >= 0) { "ticks may not be negative" }

        if (currentSimulationStep >= ticks) {
            return
        }

        lock.write {
            while (currentSimulationStep < ticks) {
                simulatedPlayer.tick()
                simulationSteps.add(SimulatedPlayerSnapshotTodoLiquidbounce(simulatedPlayer))

                this.currentSimulationStep++
            }
        }
    }

    fun getSnapshotAt(ticks: Int): SimulatedPlayerSnapshotTodoLiquidbounce {
        simulateUntil(ticks)

        lock.read {
            return simulationSteps[ticks]
        }
    }

    fun simulate(): Sequence<SimulatedPlayerSnapshotTodoLiquidbounce> = sequence {
        var idx = 0

        while (true) {
            yield(getSnapshotAt(idx))

            idx++
        }
    }

    fun getSnapshotsBetween(tickRange: IntRange): List<SimulatedPlayerSnapshotTodoLiquidbounce> {
        check(tickRange.last < 60 * 20) { "tried to simulate a player for more than a minute!" }

        simulateUntil(tickRange.last + 1)

        return lock.read {
            ObjectImmutableList(simulationSteps.subList(tickRange.first, tickRange.last + 1))
        }
    }

    fun simulateBetween(tickRange: IntRange): Sequence<SimulatedPlayerSnapshotTodoLiquidbounce> {
        check(tickRange.last < 60 * 20) { "tried to simulate a player for more than a minute!" }

        simulateUntil(tickRange.last + 1)

        return sequence {
            for (i in tickRange) {
                yield(getSnapshotAt(i))
            }
        }
    }

}

data class SimulatedPlayerSnapshotTodoLiquidbounce(
    val pos: Vec3,
    val fallDistance: Double,
    val velocity: Vec3,
    val onGround: Boolean,
    val clipLedged: Boolean
) {
    constructor(s: SimulatedPlayerTodoLiquidbounce): this(
        s.pos,
        s.fallDistance,
        s.deltaMovement,
        s.onGround,
        s.clipLedged
    )
}

/**
 * Yes, this name sucks as [SimulatedPlayerCache] already exists, but I don't know a better name :/
 */
// codex start
// class CachedPlayerSimulation(val simulatedPlayer: SimulatedPlayerCache): PlayerSimulation {
//     override val pos: Vec3
//         get() = this.simulatedPlayer.getSnapshotAt(this.ticks).pos
//
//     private var ticks = 0
//
//     override fun tick() {
//         this.ticks++
//     }
// }
// codex end
