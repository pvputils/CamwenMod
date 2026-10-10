/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, either version 3 of the License, or
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

import net.ccbluex.liquidbounce.event.EventListenerTodoLiquidbounce
import net.ccbluex.liquidbounce.event.EventManagerTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.WorldChangeEventTodoLiquidbounce
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RenderedEntitiesTestTodoLiquidbounce {

    // Equal listeners still own separate subscriptions and callbacks.
    private data class TestListener(val name: String) : EventListenerTodoLiquidbounce {
        override val running = true
    }

    private val first = TestListener("subscriber")
    private val second = TestListener("subscriber")

    @AfterTest
    fun unsubscribe() {
        RenderedEntitiesTodoLiquidbounce.unsubscribe(first)
        RenderedEntitiesTodoLiquidbounce.unsubscribe(second)
    }

    @Test
    fun `resubscribing before an update does not resurrect the previous callback`() {
        var oldCalls = 0
        var newCalls = 0
        var otherCalls = 0
        RenderedEntitiesTodoLiquidbounce.subscribe(second)
        with(second) { RenderedEntitiesTodoLiquidbounce.onUpdated { otherCalls++ } }
        RenderedEntitiesTodoLiquidbounce.subscribe(first)
        with(first) { RenderedEntitiesTodoLiquidbounce.onUpdated { oldCalls++ } }

        RenderedEntitiesTodoLiquidbounce.unsubscribe(first)
        RenderedEntitiesTodoLiquidbounce.subscribe(first)
        with(first) { RenderedEntitiesTodoLiquidbounce.onUpdated { newCalls++ } }
        EventManagerTodoLiquidbounce.callEvent(WorldChangeEventTodoLiquidbounce(null))

        assertEquals(0, oldCalls)
        assertEquals(1, newCalls)
        assertEquals(1, otherCalls)
    }

    @Test
    fun `repeated toggles between updates keep only the current callbacks`() {
        RenderedEntitiesTodoLiquidbounce.subscribe(second)
        var calls = 0

        repeat(4) {
            RenderedEntitiesTodoLiquidbounce.subscribe(first)
            with(first) { RenderedEntitiesTodoLiquidbounce.onUpdated { calls++ } }
            RenderedEntitiesTodoLiquidbounce.unsubscribe(first)
        }
        RenderedEntitiesTodoLiquidbounce.subscribe(first)
        with(first) { RenderedEntitiesTodoLiquidbounce.onUpdated { calls++ } }
        EventManagerTodoLiquidbounce.callEvent(WorldChangeEventTodoLiquidbounce(null))

        assertEquals(1, calls)
    }

    @Test
    fun `unsubscription removes every callback owned by that listener`() {
        RenderedEntitiesTodoLiquidbounce.subscribe(second)
        RenderedEntitiesTodoLiquidbounce.subscribe(first)
        var staleCalls = 0
        with(first) {
            RenderedEntitiesTodoLiquidbounce.onUpdated { staleCalls++ }
            RenderedEntitiesTodoLiquidbounce.onUpdated { staleCalls++ }
        }

        RenderedEntitiesTodoLiquidbounce.unsubscribe(first)
        RenderedEntitiesTodoLiquidbounce.subscribe(first)
        EventManagerTodoLiquidbounce.callEvent(WorldChangeEventTodoLiquidbounce(null))

        assertEquals(0, staleCalls)
    }

    @Test
    fun `unsubscription preserves equal listeners and does not call their callbacks early`() {
        RenderedEntitiesTodoLiquidbounce.subscribe(first)
        RenderedEntitiesTodoLiquidbounce.subscribe(second)
        var firstCalls = 0
        var secondCalls = 0
        with(first) { RenderedEntitiesTodoLiquidbounce.onUpdated { firstCalls++ } }
        with(second) { RenderedEntitiesTodoLiquidbounce.onUpdated { secondCalls++ } }

        RenderedEntitiesTodoLiquidbounce.unsubscribe(first)

        assertTrue(RenderedEntitiesTodoLiquidbounce.running)
        assertEquals(0, secondCalls)
        EventManagerTodoLiquidbounce.callEvent(WorldChangeEventTodoLiquidbounce(null))
        assertEquals(0, firstCalls)
        assertEquals(1, secondCalls)
    }

    @Test
    fun `the last unsubscription stops tracking and discards its callbacks`() {
        var calls = 0
        RenderedEntitiesTodoLiquidbounce.subscribe(first)
        with(first) { RenderedEntitiesTodoLiquidbounce.onUpdated { calls++ } }

        RenderedEntitiesTodoLiquidbounce.unsubscribe(first)

        assertFalse(RenderedEntitiesTodoLiquidbounce.running)
        assertTrue(RenderedEntitiesTodoLiquidbounce.isEmpty())
        RenderedEntitiesTodoLiquidbounce.subscribe(first)
        EventManagerTodoLiquidbounce.callEvent(WorldChangeEventTodoLiquidbounce(null))
        assertEquals(0, calls)
    }

    @Test
    fun `callbacks can unsubscribe themselves without skipping another listener`() {
        var firstCalls = 0
        var secondCalls = 0
        RenderedEntitiesTodoLiquidbounce.subscribe(first)
        RenderedEntitiesTodoLiquidbounce.subscribe(second)
        with(first) {
            RenderedEntitiesTodoLiquidbounce.onUpdated {
                firstCalls++
                RenderedEntitiesTodoLiquidbounce.unsubscribe(first)
            }
            RenderedEntitiesTodoLiquidbounce.onUpdated { firstCalls++ }
        }
        with(second) { RenderedEntitiesTodoLiquidbounce.onUpdated { secondCalls++ } }

        EventManagerTodoLiquidbounce.callEvent(WorldChangeEventTodoLiquidbounce(null))

        assertEquals(1, firstCalls)
        assertEquals(1, secondCalls)
        assertTrue(RenderedEntitiesTodoLiquidbounce.running)
        EventManagerTodoLiquidbounce.callEvent(WorldChangeEventTodoLiquidbounce(null))
        assertEquals(1, firstCalls)
        assertEquals(2, secondCalls)
    }

    @Test
    fun `callbacks can unsubscribe another listener before its turn`() {
        var firstCalls = 0
        var secondCalls = 0
        RenderedEntitiesTodoLiquidbounce.subscribe(first)
        RenderedEntitiesTodoLiquidbounce.subscribe(second)
        with(first) {
            RenderedEntitiesTodoLiquidbounce.onUpdated {
                firstCalls++
                RenderedEntitiesTodoLiquidbounce.unsubscribe(second)
            }
        }
        with(second) {
            RenderedEntitiesTodoLiquidbounce.onUpdated { secondCalls++ }
            RenderedEntitiesTodoLiquidbounce.onUpdated { secondCalls++ }
        }

        repeat(2) { EventManagerTodoLiquidbounce.callEvent(WorldChangeEventTodoLiquidbounce(null)) }

        assertEquals(2, firstCalls)
        assertEquals(0, secondCalls)
    }

    @Test
    fun `resubscribing during an update defers the replacement callback until the next update`() {
        var replaced = false
        var secondCalls = 0
        // Reusing the Runnable produces equal pairs, but each registration is distinct.
        val callback = Runnable { secondCalls++ }
        RenderedEntitiesTodoLiquidbounce.subscribe(first)
        RenderedEntitiesTodoLiquidbounce.subscribe(second)
        with(first) {
            RenderedEntitiesTodoLiquidbounce.onUpdated {
                if (!replaced) {
                    replaced = true
                    RenderedEntitiesTodoLiquidbounce.unsubscribe(second)
                    RenderedEntitiesTodoLiquidbounce.subscribe(second)
                    with(second) { RenderedEntitiesTodoLiquidbounce.onUpdated(callback) }
                }
            }
        }
        with(second) { RenderedEntitiesTodoLiquidbounce.onUpdated(callback) }

        EventManagerTodoLiquidbounce.callEvent(WorldChangeEventTodoLiquidbounce(null))

        assertEquals(0, secondCalls)
        EventManagerTodoLiquidbounce.callEvent(WorldChangeEventTodoLiquidbounce(null))
        assertEquals(1, secondCalls)
    }

}
