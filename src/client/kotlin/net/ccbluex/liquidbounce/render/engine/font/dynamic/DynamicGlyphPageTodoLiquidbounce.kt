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

package net.ccbluex.liquidbounce.render.engine.font.dynamic

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.ccbluex.liquidbounce.render.engine.font.AtlasSliceHandleTodoLiquidbounce
import net.ccbluex.liquidbounce.render.engine.font.DynamicAtlasAllocatorTodoLiquidbounce
import net.ccbluex.liquidbounce.render.engine.font.FontGlyphTodoLiquidbounce
import net.ccbluex.liquidbounce.render.engine.font.GlyphIdentifierTodoLiquidbounce
import net.ccbluex.liquidbounce.render.engine.font.GlyphAtlasTextureTodoLiquidbounce
import net.ccbluex.liquidbounce.render.engine.font.GlyphPageTodoLiquidbounce
import net.ccbluex.liquidbounce.render.engine.font.GlyphPageTodoLiquidbounce.Companion
import net.ccbluex.liquidbounce.render.engine.font.GlyphRenderInfoTodoLiquidbounce
import net.ccbluex.liquidbounce.render.engine.font.copyCoverageToNativeImage
import net.ccbluex.liquidbounce.render.engine.font.toLuminanceNativeImage
import java.awt.Dimension
import kotlin.math.min

class DynamicGlyphPageTodoLiquidbounce(val atlasSize: Dimension = DEFAULT_ATLAS_SIZE, fontHeight: Int) : GlyphPageTodoLiquidbounce() {
    private val image = createBufferedImageWithDimensions(atlasSize)
    override val texture = GlyphAtlasTextureTodoLiquidbounce(
        label = { "DynamicGlyphPage ${atlasSize.width}x${atlasSize.height}" },
        pixels = image.toLuminanceNativeImage(),
        retainPixels = true,
    )
    private val glyphMap = Object2ObjectOpenHashMap<GlyphIdentifierTodoLiquidbounce, Pair<GlyphRenderInfoTodoLiquidbounce, AtlasSliceHandleTodoLiquidbounce>>()
    private var copyScratchBuffer = IntArray(0)

    private val allocator = DynamicAtlasAllocatorTodoLiquidbounce(
        atlasSize,
        fontHeight + 4,
        Dimension(fontHeight / 3, fontHeight / 3)
    )

    fun getGlyph(fontGlyph: FontGlyphTodoLiquidbounce): GlyphRenderInfoTodoLiquidbounce? {
        return glyphMap[GlyphIdentifierTodoLiquidbounce(fontGlyph)]?.first
    }

    /**
     * Tries to add the given characters to the page.
     *
     * @return A list of characters that could not be added
     */
    fun tryAdd(c: Iterable<FontGlyphTodoLiquidbounce>): List<FontGlyphTodoLiquidbounce> {
        val failed = ObjectArrayList<FontGlyphTodoLiquidbounce>()

        val changesToDo = c
            .filter { glyphId -> !glyphMap.containsKey(GlyphIdentifierTodoLiquidbounce(glyphId)) }
            .mapNotNull { fontGlyph ->
                createCharacterCreationInfo(fontGlyph) ?: run {
                    failed.add(fontGlyph)
                    null
                }
            }
            .sortedByDescending { characterInfo ->
                characterInfo.glyphMetrics.bounds2D.run { width * height }
            }
            .mapNotNull { characterInfo ->
                val atlasAllocation = allocator.allocate(characterInfo.atlasDimension)
                if (atlasAllocation == null) {
                    failed.add(characterInfo.fontGlyph)
                    return@mapNotNull null
                }

                characterInfo.atlasLocation = atlasAllocation.pos
                characterInfo to atlasAllocation
            }

        // Render the characters to the image
        renderGlyphs(this.image, changesToDo.map { it.first })

        changesToDo.forEach { (generationInfo, slice) ->
            val glyph = createGlyphFromGenerationInfo(generationInfo, atlasSize)

            glyphMap.put(GlyphIdentifierTodoLiquidbounce(generationInfo.fontGlyph), glyph to slice)

            updateNativeTexture(generationInfo)
        }

        return failed
    }

    fun free(glyphIdentifier: GlyphIdentifierTodoLiquidbounce): GlyphRenderInfoTodoLiquidbounce? {
        val (renderInfo, sliceHandle) = this.glyphMap.remove(glyphIdentifier) ?: return null

        this.allocator.free(sliceHandle)

        return renderInfo
    }

    /**
     * Clears the allocator and uses optimized characters with optimized allocation order to reduce the amount of
     * fragmentation.
     *
     * @return Removed chars
     */
    fun optimizeAtlas(): List<Pair<GlyphIdentifierTodoLiquidbounce, GlyphRenderInfoTodoLiquidbounce>> {
        // Free everything, create a new allocator and use max(largestFontGlyph.height, medianFontGlyphHeight) as
        // minimal vertical slice height and the dimensions of the smallest character is minDimension.

        TODO()
    }

    private fun updateNativeTexture(generationInfo: Companion.CharacterGenerationInfo) {
        val location = generationInfo.atlasLocation
        val dimension = generationInfo.atlasDimension
        copyScratchBuffer = image.copyCoverageToNativeImage(
            target = texture.pixels!!,
            sourceX = location.x,
            sourceY = location.y,
            targetX = location.x,
            targetY = location.y,
            width = dimension.width,
            height = dimension.height,
            scratchBuffer = copyScratchBuffer,
        )
    }

    companion object {
        private val DEFAULT_ATLAS_SIZE by lazy(LazyThreadSafetyMode.NONE) {
            val atlasSize = min(2048, maxTextureSize.value)
            Dimension(atlasSize, atlasSize)
        }
    }
}
