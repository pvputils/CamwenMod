# Aim-only KillAura port

Open the Minecraft settings screen by choosing **KillAura (aim only)** in CamwenMod's configuration window. Enable it in that screen; it starts disabled and saves through CamwenMod's config. There is no module toggle keybind.

This adapts the player-only aim controller from `pvputils/LiquidBounce-silentaura`, branch `codex/killaura-native-screen`, commit `06147c60e`, to CamwenMod 26.2. It uses native client tick/render hooks, visible player yaw/pitch, and CamwenMod persistence. It adds no LiquidBounce runtime dependency.

Targets are players. The controller pauses when the normal crosshair already hits its selected player and resumes after looking away. It takes camera priority over CamwenMod's existing targeting assist while enabled. Range, wall range, and scan range restrict aim selection only; they do not change interaction attributes or attack reach. The port never invokes an attack, swing, simulated input, or packet send. Manual attacks continue through CamwenMod's existing Minecraft input path, including any pre-existing CamwenMod hooks.

The native screen exposes target priority/filtering, range/scan settings, aim-point exemptions, delay/lazy/Gaussian offsets, and Linear, Sigmoid, Interpolation, or Acceleration smoothing. Acceleration uses a native rate-limited adaptation rather than every upstream processor's optional error/dynamic/deceleration feature. LiquidBounce's silent server rotation, movement correction, AI models, fail-rotation and short-stop processors are not ported. Requires gates and automatic clicking/attack code remain absent.

Adapted source retains LiquidBounce attribution and GPL-3.0-or-later licensing. The license is bundled at `src/main/resources/assets/untitled/KillAuraLicenseTodoAi.txt`.

Validation (Java 25):

```powershell
./gradlew.bat -I tests/killAuraTestsTodoAi.gradle -I tests/aimAssistTestsTodoAi.gradle build killAuraTestsTodoAi aimAssistTestsTodoAi --offline
./gradlew.bat -I tests/killAuraGameTestsTodoAi.gradle runClientGameTest --offline
```

The focused tests cover geometry, smoothing, config migration/serialization and numeric validation. The client test covers player acquisition without an attack press, visible aiming, crosshair pause/resume, unchanged interaction range, disable, and native screen opening.
