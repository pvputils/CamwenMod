# Minecraft 1.21.4 downport

This branch targets Fabric Minecraft 1.21.4, Java 21, Fabric API 0.119.4 and Sodium 0.6.13. Install the Fabric Kotlin dependency declared in fabric.mod.json as well.

The port uses official Mojang mappings, remapped mod dependencies and a generated mixin refmap. It adapts HUD rendering, camera capture, inventory input, skins, player rendering and crosshair picking to the 1.21.4 APIs.

Synced with 26.2 commit 6bda90c: targeting-only aim assist, personal and team hit delta counters, friendly fire counters, pink FOCUS nameplates, and FOCUS/ALLY server response queries and resets. FOCUS nameplates do not grant ally behavior. Chat queries use the 1.21.4 profile accessors and Java 21 compatible listeners.

Build and regression checks:

```powershell
.\gradlew.bat build -I tests/aimAssistTestsTodoAi.gradle -I tests/trajectoryTestTodoAi.gradle -I tests/focusChatTestsTodoAi.gradle aimAssistTestsTodoAi trajectoryTestTodoAi focusChatTestsTodoAi
.\gradlew.bat -I tests/downportGameTestsTodoAi.gradle runClientGameTest
.\gradlew.bat -I tests/approachMouseGameTestsTodoAi.gradle runClientGameTest
```

The trajectory check compiles the standalone hit-counter tests into build/trajectory-tests; run their main classes to verify counter state and rendering.