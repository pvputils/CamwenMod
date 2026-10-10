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

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import net.ccbluex.liquidbounce.utils.client.error.ErrorHandlerTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.client.logger
import net.minecraft.ReportedException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor

/**
 * Simple cache.
 */
private val eventListenerScopeHolder = ConcurrentHashMap<EventListenerTodoLiquidbounce, CoroutineScope>()

/**
 * Get the related [CoroutineScope] of receiver [EventListener].
 *
 * All tasks will check [EventListener.running] on suspend.
 */
val EventListenerTodoLiquidbounce.eventListenerScope: CoroutineScope
    get() = eventListenerScopeHolder.computeIfAbsent(this) {
        CoroutineScope(
            SupervisorJob() // Prevent exception canceling
                + CoroutineExceptionHandler { _, throwable -> // logging
                when (throwable) {
                    is EventListenerNotListeningExceptionTodoLiquidbounce ->
                        logger.debug("{} is not listening, job cancelled", throwable.eventListener)
                    is ReportedException ->
                        ErrorHandlerTodoLiquidbounce.fatal(throwable, additionalMessage = "CoroutineScope of $it")
                    else -> logger.error("Exception occurred in CoroutineScope of $it", throwable)
                }
            }
                + CoroutineName(it.toString()) // Name
                // Render thread + Auto cancel on not listening
                + it.wrapContinuationInterceptor(Dispatchers.Main)
        )
    }

/**
 * Remove cached scope and cancel it.
 *
 * Remember to do this!
 */
fun EventListenerTodoLiquidbounce.removeEventListenerScope() {
    eventListenerScopeHolder.remove(this)?.cancel(EventListenerNotListeningExceptionTodoLiquidbounce(this))
}

/**
 * Wrap the [original] interceptor and make it auto-detect
 * the listener's running state at suspension
 * to determine whether to resume the continuation.
 */
fun EventListenerTodoLiquidbounce.wrapContinuationInterceptor(
    original: ContinuationInterceptor? = null,
): ContinuationInterceptor = original as? EventListenerRunningContinuationInterceptorTodoLiquidbounce
    ?: EventListenerRunningContinuationInterceptorTodoLiquidbounce(original, this)

/**
 * Occurs when the running [Job] is canceled because [EventListener.running] is false
 */
class EventListenerNotListeningExceptionTodoLiquidbounce(val eventListener: EventListenerTodoLiquidbounce) :
    CancellationException("EventListener $eventListener is not running")

/**
 * Check [EventListener.running] on suspend.
 * If true, continue.
 * If false, cancel the job.
 *
 * This means the cancellation will not be **immediate** like [Thread.interrupt].
 *
 * @param original The original [ContinuationInterceptor] such as a [CoroutineDispatcher],
 * because one [CoroutineContext] can only contain one value for a same key.
 *
 * @author MukjepScarlet
 */
private class EventListenerRunningContinuationInterceptorTodoLiquidbounce(
    private val original: ContinuationInterceptor?,
    private val eventListener: EventListenerTodoLiquidbounce,
) : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {

    override fun <T> interceptContinuation(
        continuation: Continuation<T>
    ): Continuation<T> {
        // Process with original interceptor
        val delegate = original?.interceptContinuation(continuation) ?: continuation

        return object : Continuation<T> {
            override val context get() = continuation.context

            override fun resumeWith(result: Result<T>) {
                // if the event listener is no longer active, abort the result
                val result = if (eventListener.running) {
                    result
                } else {
                    Result.failure(EventListenerNotListeningExceptionTodoLiquidbounce(eventListener))
                }
                delegate.resumeWith(result)
            }
        }
    }
}
