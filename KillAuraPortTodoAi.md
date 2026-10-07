# KillAura port

CamwenMod retains its existing features and settings. KillAura is an additive client module, disabled by default. Open its Minecraft settings screen with Right Shift or the new **KillAura** button in the existing external configuration window. Its settings are persisted separately under the game directory's `LiquidBounceKillAura` folder.

The port uses the reduced module from `pvputils/LiquidBounce-silentaura` commit `64265fc3d`; platform support was adapted using CCBlueX LiquidBounce's last Minecraft 26.2 sources (`65c735c22^`). Minecraft, Fabric, Sodium, and CamwenMod's existing Kotlin versions remain unchanged.

Attacks require real attack-button presses. Holding the button does not schedule extra entity attacks. Vanilla item reach, line of sight, attack cooldown, crits, and sprint slowdown are retained. Linear, Sigmoid, Interpolation, Acceleration, and AI smoothing, movement correction, ShortStop, Fail, and ResetThreshold remain available. The previously removed combat features, Requires, and AimPoint are not restored. CamwenMod's existing aimassist remains available with its original settings and behavior; enabling both aiming controllers can make their rotations compete.

New source filenames end in `TodoAi`; Java public types were renamed accordingly. Kotlin type names retain their original names. New blocks in existing Java and Gradle files use the requested Codex markers. Fabric metadata and test registration remain valid JSON, which does not accept comments. `KillAuraResourcesTodoAi.zip` contains the runtime resources and service registration with their required internal paths; the build expands it into the client resources.

## Attribution and licensing

The imported `net.ccbluex.liquidbounce` sources and resources originate from [CCBlueX/LiquidBounce](https://github.com/CCBlueX/LiquidBounce), copyright CCBlueX 2015–2026, under GPL version 3 or later. Original copyright headers are retained, and `KillAuraLicenseTodoAi.txt` is included in the JAR. The existing CamwenMod source and its license file remain unchanged. Fabric's metadata lists both existing CC0 and imported GPL licensing.

## Validation

Run with Java 25:

```powershell
$env:ALSOFT_DRIVERS = 'null'
./gradlew build -I tests/aimAssistTestsTodoAi.gradle -I tests/trajectoryTestTodoAi.gradle -I tests/approachMouseGameTestsTodoAi.gradle aimAssistTestsTodoAi trajectoryTestTodoAi runClientGameTest
```

The client tests include CamwenMod's existing approach, reach, and centerline regressions, plus KillAura settings/persistence, real-click counts, reach and solid-wall rejection, and grounded/falling damage parity with vanilla combat. The imported build script also configures the client game test run to use OpenAL's null output driver.

The test mod includes a registration guard for Fabric's initial test-thread race. It is excluded from the production JAR. For crit comparisons, the test invokes Minecraft's native jump method because CamwenMod's existing physical-key polling overwrites Fabric's synthetic movement-key state; subsequent motion and server crit evaluation remain vanilla.
