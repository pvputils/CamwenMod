# LiquidBounce runtime import

Source: https://github.com/pvputils/LiquidBounce-silentaura/tree/0a28cd8b4
Original upstream: https://github.com/CCBlueX/LiquidBounce
Copyright (c) 2015–2026 CCBlueX. Imported source is GPL-3.0-or-later; see LICENSE-TodoLiquidbounce. Existing Camwen source retains its original copyright and licensing notices. The combined mod metadata identifies the GPL license.

This imports the cleaned aim-only KillAura fork and its retained runtime rather than a replacement aiming controller. Player-only selection, manual-crosshair pause, movement correction, smoothing modes including AI, config values and the native settings screen are retained.

Copied source filenames, top-level types, Kotlin facades, imports, mixin references and the coroutine service provider use TodoLiquidbounce. The source mapping and source/destination hashes are in SourceImportTodoLiquidbounce.json. KillAura selection, its rotation manager and smoothing implementations were compared with the source snapshot after renaming and match exactly.

Minecraft 26.2 compatibility primarily reverses the API changes from upstream upgrade commit 65c735c22: RenderPearl to Blaze3D, SDL to GLFW, shader loading, first-person rendering, living-entity swing state and fluid simulation/accessors. Mixin argument captures use ordinals where 26.2 local variable names differ. No aiming algorithm was rewritten.

LiquidBounceResourcesTodoLiquidbounce.zip preserves runtime-required resource paths and service-loader names internally; Gradle unpacks it for the jar. Shader resources use the upstream 26.2 versions. LiquidBounceTestResourcesTodoLiquidbounce.zip similarly contains test fixtures. These archives keep convention-compliant tracked names without breaking Minecraft resource lookup.

Open Camwen's config window and select KillAura to open the copied native settings screen. The copied fork's original Right Shift screen entrypoint is also retained. Camwen aim assist remains independent.

Validation: ./gradlew build passes with 262 unit tests passed and one Windows symlink test skipped. ./gradlew runClientGameTest passes: native screen, persistence, players-only selection, visible aiming, crosshair pause/resume, and no automatic attack packets. Unit coverage includes movement correction modes. Live multiplayer/manual Silent-mode hits were not tested.

The original AI rotation mode initializes its DJL/PyTorch engine and downloads native libraries on first launch, as in the source fork. ViaFabricPlus stays optional; its API is compile-only and no incompatible 26.3 ViaFabricPlus mod is bundled.
