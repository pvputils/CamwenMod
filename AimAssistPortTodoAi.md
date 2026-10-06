# Aim assist port

Open CamwenMod's existing config window and select **Aim assist**. The new native menu has separate **aura** and **targetting** pages. Both start disabled and save in CamwenMod's existing `config` JSON under `aimAssist`. No new key binding is installed.

- **aura** moves an outside crosshair toward the nearest screen-space hitbox edge, with a small inward margin for mouse-angle rounding. It keeps range, hitbox-based FOV, hurt-time filtering, ordered target priorities, and independent horizontal/vertical axes.
- **targetting** acts only on an existing valid entity hit within vanilla interaction reach. It centers the horizontal coordinates, including depth, and chooses the height closest to level pitch on that central vertical column. Both axes interpolate together; centering has priority over level pitch. Each applied step is checked against hitbox intersection and block visibility.
- Each assist has its own **Requires** (held attack or a 0–200 ms window after every attack attempt, plus NotBreaking) and **Interpolation** settings. Interpolation retains HorizontalSpeed, VerticalSpeed, DirectionChangeFactor and Midpoint. Min/Max fields specify the same speed ranges.
- Shared Targets and TargetLock controls retain entity selection and temporary/name-filter locking. They are scoped to the two assists. No LiquidBounce module registry, browser, branding, menu cosmetics, AutoWeapon, AntiBot, Teams, AimPoint or LazyRotation code is included.

The runtime integrates through a client tick callback, the existing render mixin, and the beginning of the existing attack-attempt mixin. The config entry button dispatches from the external Swing window to the Minecraft client thread.

## Validation

Use JDK 25:

```powershell
.\gradlew.bat -I tests/aimAssistTestsTodoAi.gradle aimAssistTestsTodoAi
.\gradlew.bat runClientGameTest
.\gradlew.bat build
```

The standalone checks cover angular edge choice, inward clearance, close-range hits, center depth above/below, conflicting yaw/pitch goals, simultaneous interpolation, post-attempt expiry, settings independence and invalid input. The client test boots Minecraft 26.2, opens both menu levels, creates a single-player world, exercises air/block/entity attack attempts, and checks live aura and targetting movement. Test-only classes/resources are excluded from the production jar.

Interpolation/nearest-edge geometry is adapted from the LiquidBounce work in this session. Its GPL-3.0-or-later attribution is retained in the geometry source; `src/client/resources/AimAssistLicenseTodoAi.txt` contains the license and is bundled in the jar. CamwenMod's existing files retain their existing license.
