# Scheduled player hits

CamwenMod exposes a standalone, client-thread API:

```java
Player selectedPlayer = /* the exact client-world player entity chosen by your code */;
var hit = KillAuraHitTodoAi.scheduleHit(selectedPlayer);
hit.completion().thenAccept(result -> {
    // SENT, CANCELLED, EXPIRED, INVALID_TARGET, DISCONNECTED, DISABLED, or FAILED
});
// Optional, on the client thread:
hit.cancel();
```

Call from another thread using `Minecraft.getInstance().execute(...)`.
The API does not require holding a key, configuring a name, looking at the player, or
removing them from your ally/friendly list. It accepts the caller's exact entity object.
Self, dead, removed, spectator and foreign-world entities are rejected. A player leaving
or respawning invalidates the old object; a reused network entity ID is not a replacement.

Each call queues one attack, in FIFO order, with at most one successful dispatch per tick.
Requests wait for a valid aim and click opportunity, then complete. The default deadline
is 100 client ticks, including time spent waiting behind other requests. Queue capacity
is 64. `SENT` means the client dispatched the action; it does not claim that the server
accepted damage. `stage()` exposes why the active request is waiting.
`KillAuraHitTodoAi.cancelAll()` cancels outstanding work.

Combat cheats must be enabled. Player death/spectating, disconnects and world/player
replacement terminate pending work. Requests do not run while an inventory or other
Minecraft screen is open. Cancelled shield preparation is restored on the next tick.

## Pipeline

The port runs the complete scheduled-hit path using LiquidBounce's **ON_TICK** rotation
route, with native Minecraft 26.2 packet ordering:

1. Validate the queued entity and current world/player identity.
2. Advance the upstream millisecond click plan, with human or constant CPS timing.
3. Check inventory, enabled item and weapon attack restrictions.
4. Find a point on the target's real bounding box, prefer visible/minimal-angle rays,
   quantize the rotation to mouse sensitivity and test both block and entity obstruction.
5. Wait for planned click, miss cooldown, item cooldown and the selected critical mode.
6. Release blocking if configured, wait the unblock delay, and revalidate.
7. Stop sprinting if needed for a required critical. Send the target-facing position/rotation packet.
8. Synchronize the selected hotbar slot, send the entity attack, apply local effects,
   reset the attack ticker and swing. Piercing weapons use Minecraft 26.2's piercing action.
9. Restore view/crosshair state, send the original rotation, restore critical sprint state,
   resume the original blocking item if still equipped, and complete the request.

An intervening entity blocks the request; it never becomes the attack target.
Range/visibility are checked against the actual hitbox intersection, not entity feet.
The ordinary packet names the selected entity. Native piercing attacks may hit multiple
entities along the weapon's ray; this is Minecraft's piercing behavior.

## Options and chosen upstream route

`scheduleHit(Player, Options)` accepts:

| Option | Default |
| --- | --- |
| range / wallsRange | 3 / 0 blocks, capped to current vanilla interaction range |
| timeoutTicks | 100 |
| humanTiming | true |
| minimumCps / maximumCps | 11 / 14 |
| criticals | IGNORE; ALWAYS waits for a natural critical opportunity |
| keepSprint | true |
| unblock / unblockTicks / reblock | true / 1 / true |

This is a standalone port of the **hit execution pipeline**, not a bundled LiquidBounce
client or every configurable module. ON_TICK rotations, native 26.2 protocol, pause-on-GUI,
and explicit-target raycasting are the selected routes. NORMAL/SNAP rotation smoothing,
SMART critical prediction, automatic weapon selection, FightBot movement, fake swings,
legacy ViaVersion inventory/protocol behavior, and unrelated module integrations are not
included. No LiquidBounce mod installation is needed.

The existing name field and hold key remain an optional caller of this same API; they
enqueue a hit only when the queue is idle and cancel their own request when released.
Programmatic requests are independent of that UI.

## Upstream provenance and license

Reference clone: CCBlueX/LiquidBounce commit
`debd001505f39fc0c41698496b895e5dd9d2dfd5` (Minecraft 26.3 source, adapted to 26.2).

- [ModuleKillAura](https://github.com/CCBlueX/LiquidBounce/blob/debd001505f39fc0c41698496b895e5dd9d2dfd5/src/main/kotlin/net/ccbluex/liquidbounce/features/module/modules/combat/killaura/ModuleKillAura.kt):
  attack validation, aim/raycast/attack stages.
- [KillAuraClicker](https://github.com/CCBlueX/LiquidBounce/blob/debd001505f39fc0c41698496b895e5dd9d2dfd5/src/main/kotlin/net/ccbluex/liquidbounce/features/module/modules/combat/killaura/KillAuraClicker.kt):
  unblock, ON_TICK rotate/attack/restore ordering.
- `ClickPlan`, `ClickTiming`, `HumanClickTiming`, `ConstantClickTiming`:
  copied algorithms with package/type names changed to comply with TodoAi naming.
- `RotationFinding.raytraceBox` / `EntityRaytracing.isLookingAtEntity`:
  preferred ray, candidate box points, visible/wall ranges and ray intersection.
  The standalone implementation uses a fixed 125-point search instead of upstream
  projected-face sampling and module-dependent point processors.
- `CombatExtensions.attackEntity`: held-slot synchronization, direct attack packet,
  keep-sprint effects, ticker reset and post-1.8 swing ordering.
- `ModuleCriticals`: natural critical eligibility checks (IGNORE/ALWAYS modes).

Derived files retain CCBlueX attribution and GPL-3.0-or-later notices. The bundled
`LICENSE-LiquidBounceTodoAi.txt` contains the full GPL text. Original CamwenMod code
remains under its existing CC0 notice; the combined artifact is identified as GPL-3.0-or-later
in Fabric metadata.

## Validation

```powershell
.\gradlew.bat -I tests\killAuraTestTodoAi.gradle killAuraTestTodoAi build
```

Regression checks exercise the actual click planner, queue identity/order/cancellation/
timeout behavior, real Minecraft hitbox math, occlusion/partial visibility and accessor
member descriptors in the Minecraft 26.2 bytecode. They do not establish in-game
packet acceptance or damage.

Manual integration checks still needed in a test world: face away and schedule one hit;
put another player in front of the target; move out of range until expiry; release the
hold key while blocking; switch world while queued; exercise ordinary and piercing
weapons; verify rotation/slot/attack/swing/restore packets and shield restoration.
