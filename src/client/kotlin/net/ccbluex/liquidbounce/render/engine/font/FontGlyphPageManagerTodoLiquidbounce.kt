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

package net.ccbluex.liquidbounce.render.engine.font

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.api.core.ioScope
import net.ccbluex.liquidbounce.event.EventListenerTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.GameRenderEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.FontFaceTodoLiquidbounce
import net.ccbluex.liquidbounce.render.engine.font.dynamic.DynamicFontCacheManagerTodoLiquidbounce
import net.ccbluex.liquidbounce.render.engine.font.dynamic.DynamicGlyphPageTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.client.logger
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil

class FontGlyphPageManagerTodoLiquidbounce(
    registeredFaces: Collection<FontFaceTodoLiquidbounce>,
    private val primaryFace: FontFaceTodoLiquidbounce,
    private val fallbackFonts: List<FontFaceTodoLiquidbounce> = emptyList()
) : EventListenerTodoLiquidbounce, AutoCloseable {

    private val registeredFonts = registeredFaces.toSet()
    private val staticPage = ArrayList<StaticGlyphPageTodoLiquidbounce>()
    private val staticGlyphs = Object2ObjectOpenHashMap<FontIdTodoLiquidbounce, Int2ObjectOpenHashMap<GlyphDescriptorTodoLiquidbounce>>()
    private val dynamicPage: DynamicGlyphPageTodoLiquidbounce = DynamicGlyphPageTodoLiquidbounce(
        fontHeight = ceil(primaryFace.plainStyle.height * 2.0F).toInt()
    )
    private val dynamicFontManager: DynamicFontCacheManagerTodoLiquidbounce = DynamicFontCacheManagerTodoLiquidbounce(
        this.dynamicPage
    )

    private val fallbackGlyphs: Map<FontFaceTodoLiquidbounce, GlyphDescriptorTodoLiquidbounce>
    private val dynamicallyLoadedGlyphs = Object2ObjectOpenHashMap<GlyphIdentifierTodoLiquidbounce, GlyphDescriptorTodoLiquidbounce>()
    private val closed = AtomicBoolean(false)
    private val staticPagesLock = Any()
    private var commonHanWarmupJob: Job? = null

    init {
        require(primaryFace in registeredFonts) { "Primary font $primaryFace is not registered" }

        val eagerGlyphs = FontCharacterSetTodoLiquidbounce.createEagerGlyphs(registeredFonts, primaryFace, fallbackFonts)
        registerStaticPages(StaticGlyphPageTodoLiquidbounce.createGlyphPages(eagerGlyphs))

        val primaryFallback = requireNotNull(staticGlyphs[primaryFace.plainStyle]?.get('?'.code)) {
            "Primary font $primaryFace has no fallback glyph"
        }
        fallbackGlyphs = registeredFonts.associateWith { font ->
            staticGlyphs[font.plainStyle]?.get('?'.code) ?: primaryFallback
        }

        this.dynamicFontManager.startThread()
        startCommonHanWarmup()
    }

    @Suppress("unused")
    private val renderHandler = handler<GameRenderEventTodoLiquidbounce> {
        this.dynamicFontManager.update().forEach { update ->
            val key = GlyphIdentifierTodoLiquidbounce(update.descriptor.renderInfo.codepoint, update.font)

            if (!update.removed) {
                dynamicallyLoadedGlyphs.put(key, update.descriptor)
            } else {
                dynamicallyLoadedGlyphs.remove(key)
            }
        }
    }

    private fun registerStaticPages(glyphPages: List<StaticGlyphPageTodoLiquidbounce>): Boolean = synchronized(staticPagesLock) {
        if (closed.get()) {
            return@synchronized false
        }

        glyphPages.forEach { glyphPage ->
            for ((font, glyphRenderInfo) in glyphPage.glyphs) {
                staticGlyphs.computeIfAbsent(font) { Int2ObjectOpenHashMap(512) }
                    .put(glyphRenderInfo.codepoint, GlyphDescriptorTodoLiquidbounce(glyphPage, glyphRenderInfo))
            }
        }
        staticPage.addAll(glyphPages)
        true
    }

    private fun startCommonHanWarmup() {
        commonHanWarmupJob = ioScope.launch(Dispatchers.Default) {
            try {
                warmCommonHanGlyphs()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                logger.error("Failed to warm common Han glyphs", exception)
            }
        }
    }

    private suspend fun warmCommonHanGlyphs() {
        val glyphs = FontCharacterSetTodoLiquidbounce.createCommonHanGlyphs(primaryFace, fallbackFonts)
        if (glyphs.isEmpty()) {
            logger.info("Skipping common Han warmup because no configured font can display it")
            return
        }

        val preparedPages = StaticGlyphPageTodoLiquidbounce.prepareGlyphPages(glyphs)
        currentCoroutineContext().ensureActive()
        if (closed.get()) {
            return
        }

        val registered = withContext(Dispatchers.Main) {
            materializeAndRegister(preparedPages)
        }
        if (registered) {
            logger.info("Finished warming ${glyphs.size} common Han glyphs")
        }
    }

    private fun materializeAndRegister(preparedPages: List<PreparedStaticGlyphPageTodoLiquidbounce>): Boolean {
        if (closed.get()) {
            return false
        }

        val pages = preparedPages.map(PreparedStaticGlyphPageTodoLiquidbounce::materialize)
        if (registerStaticPages(pages)) {
            return true
        }

        pages.forEach { it.texture.close() }
        return false
    }

    fun requestGlyph(font: FontFaceTodoLiquidbounce, style: @FontStyleTodoLiquidbounce Int, codepoint: Int): GlyphDescriptorTodoLiquidbounce? {
        check(font in registeredFonts) { "Font $font is not registered" }

        val requestedFont = font.style(style) ?: font.plainStyle
        staticGlyphs[requestedFont]?.get(codepoint)?.let { return it }

        val resolvedFont = resolveFont(font, fallbackFonts, style, codepoint) ?: return null
        findResolvedStaticGlyph(font, resolvedFont, codepoint)?.let { return it }

        val fontGlyph = FontGlyphTodoLiquidbounce(codepoint, resolvedFont)
        val glyphIdentifier = GlyphIdentifierTodoLiquidbounce(fontGlyph)
        this.dynamicFontManager.requestGlyph(fontGlyph)
        return this.dynamicallyLoadedGlyphs[glyphIdentifier]
    }

    private fun findResolvedStaticGlyph(
        requestedFace: FontFaceTodoLiquidbounce,
        resolvedFont: FontIdTodoLiquidbounce,
        codepoint: Int,
    ): GlyphDescriptorTodoLiquidbounce? = staticGlyphs[resolvedFont]?.get(codepoint) ?: fallbackFonts.asSequence()
        .filter { it !== requestedFace }
        .map(FontFaceTodoLiquidbounce::plainStyle)
        .firstNotNullOfOrNull { staticGlyphs[it]?.get(codepoint) }

    fun getFallbackGlyph(font: FontFaceTodoLiquidbounce): GlyphDescriptorTodoLiquidbounce {
        return fallbackGlyphs[font] ?: error("Font $font is not registered")
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) {
            return
        }
        commonHanWarmupJob?.cancel()
        commonHanWarmupJob = null
        unregister()
        this.dynamicFontManager.close()
        this.dynamicPage.texture.close()
        synchronized(staticPagesLock) {
            this.staticPage.forEach { it.texture.close() }
            this.staticPage.clear()
        }
        this.dynamicallyLoadedGlyphs.clear()
    }

}

class GlyphDescriptorTodoLiquidbounce(val page: GlyphPageTodoLiquidbounce, val renderInfo: GlyphRenderInfoTodoLiquidbounce)

internal fun resolveFont(
    requestedFace: FontFaceTodoLiquidbounce,
    fallbackFaces: List<FontFaceTodoLiquidbounce>,
    style: @FontStyleTodoLiquidbounce Int,
    codepoint: Int,
): FontIdTodoLiquidbounce? {
    return synchronized(GlyphPageTodoLiquidbounce.fontRasterizationLock) {
        val requestedFont = requestedFace.style(style) ?: requestedFace.plainStyle
        if (requestedFont.awtFont.canDisplay(codepoint)) {
            return@synchronized requestedFont
        }

        fallbackFaces.asSequence()
            .filter { it !== requestedFace }
            .map { it.style(style) ?: it.plainStyle }
            .firstOrNull { it.awtFont.canDisplay(codepoint) }
    }
}
