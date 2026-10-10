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
package net.ccbluex.liquidbounce.event

import net.ccbluex.liquidbounce.annotations.TagTodoLiquidbounce
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EventClassRegistryTestTodoLiquidbounce {

    @Test
    fun `every built-in event class carries a tag`() {
        val untagged = ALL_EVENT_CLASSES.filter { it.getAnnotation(TagTodoLiquidbounce::class.java) == null }

        assertTrue(untagged.isEmpty(), "Event classes without @Tag: ${untagged.map { it.name }}")
    }

    @Test
    fun `built-in event tags are unique, case-insensitively`() {
        val byName = ALL_EVENT_CLASSES.groupBy { it.getAnnotation(TagTodoLiquidbounce::class.java).name.lowercase() }
        val duplicates = byName.filterValues { it.size > 1 }

        val rendered = duplicates.mapValues { (_, classes) -> classes.map { it.name } }

        assertTrue(duplicates.isEmpty(), "Duplicate event tags: $rendered")
    }

    @Test
    fun `every built-in event class is known to the manager`() {
        assertTrue(EventManagerTodoLiquidbounce.knownEventClasses.containsAll(ALL_EVENT_CLASSES.asList()))
    }

    @Test
    fun `events resolve by tag name, ignoring case`() {
        val eventClass = ALL_EVENT_CLASSES.first()
        val name = eventClass.getAnnotation(TagTodoLiquidbounce::class.java).name

        assertSame(eventClass, EventManagerTodoLiquidbounce.eventClassByName(name))
        assertSame(eventClass, EventManagerTodoLiquidbounce.eventClassByName(name.uppercase()))
    }

    @TagTodoLiquidbounce("EventClassRegistryTestAddonEvent")
    private class AddonEvent : EventTodoLiquidbounce()

    private class UntaggedAddonEvent : EventTodoLiquidbounce()

    @Test
    fun `an add-on event registers on first hook and keeps its hooks across later registrations`() {
        val listener = object : EventListenerTodoLiquidbounce {}
        val hook = EventHookTodoLiquidbounce<AddonEvent>(listener) { }

        EventManagerTodoLiquidbounce.registerEventHook(AddonEvent::class.java, hook)

        assertTrue(AddonEvent::class.java in EventManagerTodoLiquidbounce.knownEventClasses)
        assertSame(AddonEvent::class.java, EventManagerTodoLiquidbounce.eventClassByName("eventclassregistrytestaddonevent"))
        assertNotNull(EventManagerTodoLiquidbounce.eventFlow(AddonEvent::class.java))

        EventManagerTodoLiquidbounce.registerEventHook(UntaggedAddonEvent::class.java, EventHookTodoLiquidbounce(listener) { })

        var received = 0
        val counting = EventHookTodoLiquidbounce<AddonEvent>(listener) { received++ }
        EventManagerTodoLiquidbounce.registerEventHook(AddonEvent::class.java, counting)
        EventManagerTodoLiquidbounce.callEvent(AddonEvent())

        assertEquals(1, received)

        EventManagerTodoLiquidbounce.unregisterEventHandler(listener)
    }

    @Test
    fun `an add-on event without a tag falls back to its simple name`() {
        EventManagerTodoLiquidbounce.registerEventClass(UntaggedAddonEvent::class.java)

        assertEquals("UntaggedAddonEvent", UntaggedAddonEvent::class.java.eventName)
    }
}
