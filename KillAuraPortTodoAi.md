# Native KillAura port

KillAura follows CamwenMod's AimAssist integration: focused Java classes, native Minecraft settings, CamwenMod's existing `config` JSON, and a few additions to existing mixins. It starts disabled. Open it with Right Shift or the **KillAura** button in the existing external configuration window. Settings are stored under `killAura` independently of `aimAssist`.

The behavior is adapted from the reduced `pvputils/LiquidBounce-silentaura` module at `64265fc3d`, with the Minecraft 26.2 platform APIs used by the previous port. The LiquidBounce module registry, event bus, coroutine startup, generic config system, debug module, chunk scanner, Blink framework, custom fonts/shaders/GPU renderer, and compatibility infrastructure are removed. KillAura adds no Kotlin source files, access widener, production mixin configuration, or external dependencies. CamwenMod's pre-existing Kotlin Gradle plugin and Fabric language dependency are unchanged.

## Combat and rotations

- Only real attack presses invoke Minecraft's complete `startAttack` implementation. Holding attack does not schedule repeated entity attacks. Existing CamwenMod attack hooks still run against the redirected entity hit.
- Acquisition and the actual press both check the held item's vanilla attack range, unexpanded entity bounds, and solid-block line of sight. No separate reach setting or wall bypass is added. Vanilla item attacks, cooldown scaling, crits, sprint slowdown, and shield behavior stay in Minecraft.
- Targets retain FOV, hurt time, ordered Type/Health/Distance/Direction/HurtTime/Age priorities, shield filtering, entity categories, and a native teammate-exclusion option.
- Rotations retain Normal/Snap/OnTick timing, lazy rotation, Linear/Sigmoid/Interpolation/Acceleration/AI smoothing, Off/Strict/Silent/ChangeLook movement correction, ShortStop, Fail, reset threshold, and reset timing. Silent rotation packets preserve the camera; ChangeLook changes it. OnTick estimates arrival using the selected smoother.
- Crosshair and camera changes during a press are restored in `finally`, including cancellation or exceptions from existing hooks. Opening a screen, disabling KillAura, changing worlds, death, and spectator mode clear active aiming state.

The previous port's cosmetic rendering is replaced by a small native HUD target marker/name and optional held-item reach readout. LiquidBounce's multiple animated target styles, world-space range rings, custom colors/animation trees, and debug interface are not imported. This keeps the rendering integration as small as the AimAssist port; these visuals are not pixel-identical to LiquidBounce.

## AI without an engine dependency

The original `21KC11KP` and `19KC8KP` weights are retained unchanged, in two `TodoAi.params` assets. `KillAuraModelTodoAi` evaluates the fixed six-input, four-linear-layer model using Java float arrays, batch normalization, and ReLU. No DJL/PyTorch engine, native-library download, Okio, or Kotlin infrastructure is needed. Both models were checked against independent NumPy matrix inference, and the numerical reference is tested.

The Model setting accepts either bundled name or a matching custom model. Put compatible FLOAT32 DJL weights in `<game directory>/LiquidBounceKillAura/deeplearning/models/<name>TodoAi.params`. The previous port's `<name>/tf-XXXX.params` user-model directory format is also supported. Files are limited to 1 MiB and must match the supported 6→128→64→32→2 architecture. Missing or invalid models fall back to interpolation and log one warning. The upstream training framework is not included.

The old port's generic LiquidBounce settings JSON is not migrated. New settings use the same storage as CamwenMod's AimAssist; user model weights can still be reused.

## Validation

Use Java 25:

```powershell
./gradlew.bat build -I tests/aimAssistTestsTodoAi.gradle -I tests/trajectoryTestTodoAi.gradle -I tests/killAuraTestsTodoAi.gradle -I tests/approachMouseGameTestsTodoAi.gradle aimAssistTestsTodoAi trajectoryTestTodoAi killAuraTestsTodoAi runClientGameTest
```

The suite covers existing AimAssist/trajectory regressions, native KillAura settings/persistence, shortest-path rotation and OnTick estimates, both real bundled models, corrupt inputs, actual held/rapid click counts, vanilla reach and walls, grounded/falling damage parity, all five live smoothing modes, silent camera preservation, and cancellation/exception restoration. Game tests use OpenAL's null output driver and a test-only Fabric initial-thread registration guard. Test classes and mixins are excluded from the production JAR.

## Source conventions and attribution

New files end in `TodoAi`. Added blocks in existing Java/Gradle files use `// codex start` and `//codex end`; imports need no markers. JSON metadata stays valid JSON, which cannot contain line comments. GPL attribution accompanies the adapted math and original model resources; `KillAuraLicenseTodoAi.txt` is bundled in the JAR. Fabric metadata retains both the original license label and the imported GPL label.
