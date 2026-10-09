# Aim-only KillAura port

Open the Minecraft settings screen by choosing **KillAura (aim only)** in CamwenMod's configuration window. Enable it in that screen; it starts disabled and saves through CamwenMod's config. There is no module toggle keybind.

This adapts the player-only aim controller from `pvputils/LiquidBounce-silentaura`, branch `codex/killaura-native-screen`, commit `06147c60e`, to CamwenMod 26.2. It uses native client tick/render hooks, visible player yaw/pitch, and CamwenMod persistence. It adds no LiquidBounce runtime dependency.

Activation requires the current hit result to be `MISS`, a fresh zero-margin pick to remain a miss, and a pick with the configured **Margin** to hit an eligible player. Margin is measured in blocks (default 0.3, allowed 0–1); it is a temporary player hitbox expansion for this probe only. A normal entity hit, a block hit, or a miss outside the margin stops correction. It resumes when the margin-only condition becomes true again. Existing team exclusion remains honored.

There is no FOV, ranked target list, target-priority/filter settings, extra reach, through-wall acquisition, or scan range. The normal Minecraft pick chooses the nearby player; no entity list is scanned. The controller takes camera priority over the existing targeting assist only while correcting a margin miss. Probes never replace Minecraft's hit result, change persisted attack margins, or alter interaction attributes. Manual attacks continue through CamwenMod's existing Minecraft input path, including pre-existing CamwenMod hooks. The port never invokes an attack, swing, simulated input, or packet send.

Aim-point exemptions, delay/lazy/Gaussian offsets, and Linear, Sigmoid, Interpolation, or Acceleration smoothing remain configurable. Acceleration uses a native rate-limited adaptation rather than every upstream processor's optional error/dynamic/deceleration feature. LiquidBounce's silent server rotation, movement correction, AI models, fail-rotation and short-stop processors are not ported. Requires gates and automatic clicking/attack code remain absent.

Adapted source retains LiquidBounce attribution and GPL-3.0-or-later licensing. The license is bundled at `src/main/resources/assets/untitled/KillAuraLicenseTodoAi.txt`.

Validation (Java 25):

```powershell
./gradlew.bat -I tests/killAuraTestsTodoAi.gradle -I tests/aimAssistTestsTodoAi.gradle build killAuraTestsTodoAi aimAssistTestsTodoAi --offline
./gradlew.bat -I tests/killAuraGameTestsTodoAi.gradle runClientGameTest --offline
```

The focused tests cover geometry, smoothing, config migration/serialization and numeric validation. The client test covers margin-only acquisition, rejection outside the margin and on normal entity/block hits, immediate render deactivation, pause/resume, scoped probes with global cheats on/off, unchanged interaction reach/manual hit result, disable, and native screen opening.
