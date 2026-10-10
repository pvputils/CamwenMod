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

package net.ccbluex.liquidbounce.event.events

import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.vertex.PoseStack
import net.ccbluex.liquidbounce.annotations.TagTodoLiquidbounce
import net.ccbluex.liquidbounce.event.EventTodoLiquidbounce
import net.ccbluex.liquidbounce.features.addon.AddonApiTodoLiquidbounce
import net.ccbluex.liquidbounce.render.WorldRenderEnvironmentTodoLiquidbounce
import net.ccbluex.liquidbounce.render.getDynamicTransformsUniform
import net.ccbluex.liquidbounce.render.mesh.BatchCollectorTodoLiquidbounce
import net.minecraft.client.Camera
import net.minecraft.client.gui.GuiGraphicsExtractor

@TagTodoLiquidbounce("gameRender")
object GameRenderEventTodoLiquidbounce : EventTodoLiquidbounce()
// codex start
//
// @Tag("screenRender")
// class ScreenRenderEvent(val context: GuiGraphicsExtractor, val partialTicks: Float) : Event()
// codex end

@AddonApiTodoLiquidbounce
@TagTodoLiquidbounce("worldRender")
class WorldRenderEventTodoLiquidbounce(
    val poseStack: PoseStack,
    val camera: Camera,
    val partialTicks: Float,
    val renderTarget: RenderTarget,
) : EventTodoLiquidbounce(), AutoCloseable {

    @Deprecated("For scripts only", ReplaceWith("poseStack"))
    val matrixStack get() = poseStack

    private val batchCollector = BatchCollectorTodoLiquidbounce()

    val environment = WorldRenderEnvironmentTodoLiquidbounce(
        renderTarget = renderTarget,
        poseStack = poseStack,
        camera = camera,
        batchCollector = batchCollector,
    )

    override fun close() {
        batchCollector.flush(renderTarget, getDynamicTransformsUniform())
    }

}

/**
 * Fired before vanilla collects level features into its [SubmitNodeStorage].
 */
// codex start
// @Tag("worldFeatureSubmit")
// class WorldFeatureSubmitEvent(
//     val poseStack: PoseStack,
//     val camera: Camera,
//     val submitNodeStorage: SubmitNodeStorage,
// ) : Event()
//
// /**
//  * Sometimes, modules might want to contribute something to the glow framebuffer. They can hook this event
//  * in order to do so.
//  *
//  * Note: After writing to the outline framebuffer [markDirty] must be called.
//  */
// codex end
// codex start
// @Tag("drawOutlines")
// class DrawOutlinesEvent(
//     val renderTarget: RenderTarget,
//     val pose: PoseStack,
//     val partialTicks: Float,
// ) : Event() {
//     var dirtyFlag: Boolean = false
//         private set
//
//     /**
//      * Called when the framebuffer was edited.
//      */
//     fun markDirty() {
//         this.dirtyFlag = true
//     }
// }
// codex end

@AddonApiTodoLiquidbounce
@TagTodoLiquidbounce("overlayRender")
class OverlayRenderEventTodoLiquidbounce(
    val context: GuiGraphicsExtractor,
    val tickDelta: Float,
) : EventTodoLiquidbounce()
