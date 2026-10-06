# Minecraft 1.21.4 downport

This branch targets Fabric Minecraft 1.21.4, Java 21, Fabric API 0.119.4 and Sodium 0.6.13. Install the Fabric Kotlin dependency declared in fabric.mod.json as well.

The older, obfuscated Minecraft release uses official Mojang mappings, remapped mod dependencies and a generated mixin refmap. The port adapts HUD rendering, camera capture, inventory input, skins, player rendering and crosshair picking to the 1.21.4 APIs. Sodium 0.6.13 uses isFaceCulled instead of the additional shouldDrawSide hook.

Build and regression checks:

```powershell
.\gradlew.bat build -I tests/aimAssistTestsTodoAi.gradle -I tests/trajectoryTestTodoAi.gradle aimAssistTestsTodoAi trajectoryTestTodoAi
```

Changed source lines retain their previous code in Codex comments. Added source blocks use the requested start/end markers; new source filenames end in TodoAi before the extension. Properties use a preceding native comment because trailing comments become part of the property value. JSON stores markers as string metadata because JSON does not permit comments.

Automated client-world compatibility check (creates an isolated flat test world):

```powershell
.\gradlew.bat -I tests/downportGameTestsTodoAi.gradle runClientGameTest
```

Aim assist follows crosshair proximity to the hitbox, including interpolated player and target movement. Steady or decreasing proximity allows assistance; increasing proximity, leaving the hitbox, or crossing past it blocks assistance. A still mouse does not override walking away. History records the result after assistance so the assist does not feed itself.

Crosshair movement client-world regression check: `./gradlew.bat -I tests/approachMouseGameTestsTodoAi.gradle runClientGameTest`. Geometry regressions are included in `aimAssistTestsTodoAi` above.

Held-attack exits remain unassisted across targeting-to-aura transitions and frames between mouse updates. Assistance resumes immediately on a turn toward the hitbox, or after 150 ms of steady crosshair position. Movement within the hitbox is tracked before the crosshair crosses its edge. The crosshair client test above covers the actual targeting, aura, and full render paths.

Both assists use the existing Range setting as their total probe reach; targeting now exposes that setting as well. Scoped transient modifiers temporarily raise block and entity interaction attributes as needed, and restore them after every probe. Range alone selects neutral-pitch targeting; range plus margin selects aura. The client-world suite includes distant-player reach routing and success/failure attribute cleanup.
