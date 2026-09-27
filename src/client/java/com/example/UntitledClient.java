package com.example;

import com.example.Configs.CheatConfig;
import com.example.Configs.Config;
import com.google.common.reflect.TypeToken;
import com.mojang.blaze3d.platform.Window;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.example.overlayTodoAi.PlayerWaypointOverlay;
import com.example.overlayTodoAi.ExternalConfigWindow;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.*;

import java.lang.Math;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.example.Constants.*;
import static com.example.DelayedConstantsTodo.TEXT_RENDERER;
import static com.example.Utils.*;

public class UntitledClient implements ClientModInitializer {
    private final PlayerWaypointOverlay playerOverlay = new PlayerWaypointOverlay();
    static public Config config = getDeserializedJsonBlocking("config", Config.class) instanceof Config foo
            ? foo
            : new Config();
    public static HashMap<String, CheatConfig> cheatConfigs = getDeserializedJsonBlocking(
            "cheat-configs",
            new TypeToken<HashMap<String, CheatConfig>>() {
            }.getType()) instanceof HashMap<?, ?> map
            ? (HashMap<String, CheatConfig>) map // TODO -> ?
            : new HashMap<>();
    //    TODO; // gl. also, I have to just do this without supabase-kt because fabric(?) is retarded
    // java has a websocket I can use for this apparently
    //        TODO; // task for sending the http payloads of all the shared info
    //        TODO; // register task for drawing waypoints of far away players
//    public static final KeyMapping // TODO -> idk why it crashes when I move these
//            SNEAK_TOGGLE = getAbstractPvpUtilsKeybind("Sneak (Toggle)"),
//            SNEAK_ENABLE = getAbstractPvpUtilsKeybind("Sneak (Enable)"),
//            SNEAK_DISABLE = getAbstractPvpUtilsKeybind("Sneak (Disable)");
//    public static final KeyMapping
//            SPRINT_TOGGLE = getAbstractPvpUtilsKeybind("Sprint (Toggle)"),
//            SPRINT_ENABLE = getAbstractPvpUtilsKeybind("Sprint (Enable)"),
//            SPRINT_DISABLE = getAbstractPvpUtilsKeybind("Sprint (Disable)");
    public static final KeyMapping
            MOVEMENT_TOGGLE = getAbstractPvpUtilsKeybind("Movement (Toggle)"),
            MOVEMENT_ENABLE = getAbstractPvpUtilsKeybind("Movement (Enable)"),
            MOVEMENT_DISABLE = getAbstractPvpUtilsKeybind("Movement (Disable)");
    //    public static final KeyMapping
//            FULLBRIGHT_TOGGLE = getAbstractPvpUtilsKeybind("Fullbright (Toggle)"),
//            FULLBRIGHT_ENABLE = getAbstractPvpUtilsKeybind("Fullbright (Enable)"),
//            FULLBRIGHT_DISABLE = getAbstractPvpUtilsKeybind("Fullbright (Disable)");
//    public static final KeyMapping
//            FULLBRIGHT_HOLD = getAbstractPvpUtilsKeybind("Fullbright (Hold)");
    public static final KeyMapping
            ALLY_TOGGLE = getAbstractPvpUtilsKeybind("Ally (Toggle)"),
            FRIENDLY_TOGGLE = getAbstractPvpUtilsKeybind("Friendly (Toggle)");
    //            ENEMY_TOGGLE = getAbstractPvpUtilsKeybind("Enemy (Toggle)"),
//            FOCUS_TOGGLE = getAbstractPvpUtilsKeybind("Focus (Toggle)"),
    public static final KeyMapping HEAD_RUN_CAMERA_OFFSET_ENABLE = getAbstractPvpUtilsKeybind("Head-run camera offset enable");
    public static final KeyMapping
//            PLAYER_WAYPOINTS_TOGGLE = getAbstractPvpUtilsKeybind("Player waypoints (Toggle)"),
            PLAYER_WAYPOINTS_CYCLE = getAbstractPvpUtilsKeybind("Player waypoints (Cycle)"),
            PLAYER_WAYPOINTS_DISABLE = getAbstractPvpUtilsKeybind("Player waypoints (Disable)");
    public static final KeyMapping PLAYER_XRAY_TOGGLE = getAbstractPvpUtilsKeybind("Player xray (Toggle)");
    //            BLOCK_XRAY_TOGGLE = getAbstractPvpUtilsKeybind("Block xray (Toggle)");
//    public static final KeyMapping
//            DECREMENT_CHEATS = getAbstractPvpUtilsKeybind("Decrement cheats"),
//            INCREMENT_CHEATS = getAbstractPvpUtilsKeybind("Increment cheats");
    //    public static final KeyMapping JUMP_CHEAT_HOLD = getAbstractPvpUtilsKeybind("Jump cheat (Hold)");
    public static final KeyMapping KEYBIND_CONFIG = getAbstractPvpUtilsKeybind("Config");
//    public static boolean
//            isSprintEnabled,
//            isSneakEnabled,
//            isJumpEnabled,
//            isForwardEnabled,
//            isLeftEnabled,
//            isRightEnabled,
//            isBackwardEnabled;
    public static Input toggleMovementState = new Input(false, false, false, false, false, false, false); // TODO -> constant
    public enum SprintResetState {
        INVALID,
        HELD,
        VALID
    }
    public static SprintResetState sprintResetBackwardsKeyState = SprintResetState.INVALID;

    public static boolean isPlayerXrayEnabled = false;
//    public static String currentXrayType = "";
//    public static Set<Block> immutableXrayBlocks = Set.of(Blocks.STONE, Blocks.DEEPSLATE, Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.SAND, Blocks.RED_SAND, Blocks.SANDSTONE, Blocks.RED_SANDSTONE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.GRANITE, Blocks.GRAVEL); // TODO -> move all destruct-able state to map // Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.FURNACE, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.ANCIENT_DEBRIS, Blocks.NETHER_GOLD_ORE, Blocks.GOLD_BLOCK, Blocks.RAW_GOLD_BLOCK, Blocks.RAW_IRON_BLOCK, Blocks.RAW_COPPER_BLOCK, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE, Blocks.BOOKSHELF, Blocks.COBWEB)); // TODO -> move all destruct-able state to map
//    @Nullable
//    public static Map<String, Object> nullableImmutableState = Map.of(
//            "SHOULD_DRAW_SIDE_MIXIN", (BiFunction<BlockState, Boolean, Boolean>) (state, original) -> {
//                switch (currentXrayType) {
//                    case "block" -> {
//                        if (immutableXrayBlocks.contains(state.getBlock()))
//                            return false;
//                    }
//                    case "player" -> {
//                        return false;
//                    }
//                }
//                return original;
//            });

//    public enum RAGE_CHEAT_LEVEL {
//        ZERO(0.f, 0.f),
//        ONE(0.0005f, .005f),
//        TWO(0.001f, .005f),
//        THREE(.0015f, .005f);
//
//        public final float staticTargetingMarginBypass;
//        public final float movingTargetingMarginBypass;
//
//        RAGE_CHEAT_LEVEL(
//                float staticTargetingMarginBypass, float movingTargetingMarginBypass) {
//            this.staticTargetingMarginBypass = staticTargetingMarginBypass;
//            this.movingTargetingMarginBypass = movingTargetingMarginBypass;
//        }
//    }
//    public static RAGE_CHEAT_LEVEL rageCheatLevel = RAGE_CHEAT_LEVEL.ZERO;

    public static CameraRenderState cameraRenderState;

    record TempWaypoint(String title, Vec3 coordinate) {
    }

    private static final ArrayList<TempWaypoint> tempWaypoints = new ArrayList<>();

    public enum HEAD_RUN_OFFSET_TYPE {
        NONE(0.f),
        LEFT(-45.f),
        RIGHT(45.f);

        public final float delta;

        HEAD_RUN_OFFSET_TYPE(float delta) {
            this.delta = delta;
        }
    }

    public static HEAD_RUN_OFFSET_TYPE headRunCameraOffset = HEAD_RUN_OFFSET_TYPE.NONE;

    // codex start
    public static boolean isEthyleneSprintFovCancelled;
    // codex end

    @Override
    public void onInitializeClient() {
        // codex start
        ExternalConfigWindow.prepareDesktopWindowing();
        // codex end
        // exampleLayer
        {
            // TODO -> fix?
//            final Identifier EXAMPLE_LAYER = Identifier.fromNamespaceAndPath("pvputils1", "hud-example-layer");
//            HudElementRegistry.attachElementBefore(
//                    VanillaHudElements.CHAT,
//                    EXAMPLE_LAYER,
//                    (context, _) -> {
//                        StringBuilder stringBuilder = new StringBuilder("[");
//                        boolean flag = false;
//                        if (config.isToggleSneakGuiEnabled) {
//                            if (isForwardEnabled) {
//                                stringBuilder.append("Forward");
//                                flag = true;
//                            }
//                            flag =
//                                    handleGetIsEnabled(isJumpEnabled,
//                                            handleGetIsEnabled(isBackwardEnabled,
//                                                    handleGetIsEnabled(isRightEnabled,
//                                                            handleGetIsEnabled(isLeftEnabled, flag, stringBuilder, "Left"), stringBuilder, "Right"), stringBuilder, "Backwards"), stringBuilder, "Jump");
//                            if (MINECRAFT_CLIENT_INSTANCE.player instanceof LocalPlayer player) {
//                                boolean isFlying = player.getAbilities().flying;
//                                boolean isSneaking = player.isShiftKeyDown();
//                                boolean isSprintingElseDone = player.isSprinting() && !isFlying; // TODO probably can't do both of these anyway
//                                boolean isSneakingElseDone = isSneaking && !isFlying;
//                                if (isSprintingElseDone &&
//                                        (config.isSprintEnabled || OPTIONS.toggleSprint().get())) {
//                                    if (flag)
//                                        stringBuilder.append(", ");
//                                    stringBuilder.append("Sprinting");
//                                    flag = true;
//                                    isSprintingElseDone = false;
//                                }
//                                if (isSneakingElseDone &&
//                                        (config.isSneakEnabled || OPTIONS.toggleCrouch().get())) {
//                                    if (flag)
//                                        stringBuilder.append(", ");
//                                    stringBuilder.append("Sneaking");
//                                    flag = true;
//                                    isSneakingElseDone = false;
//                                }
//                                if (flag)
//                                    stringBuilder.append(" (Toggled)");
//                                boolean keyHeldFlag = false;
//                                if (isSprintingElseDone && SPRINT_VANILLA.isDown()) {
//                                    if (flag)
//                                        stringBuilder.append(", ");
//                                    stringBuilder.append("Sprinting");
//                                    flag = true;
//                                    keyHeldFlag = true;
//
//                                    isSprintingElseDone = false;
//                                }
//                                if (isSneakingElseDone && SNEAK_VANILLA.isDown()) {
//                                    if (flag)
//                                        stringBuilder.append(", ");
//                                    stringBuilder.append("Sneaking");
//                                    flag = true;
//                                    keyHeldFlag = true;
//
//                                    // TODO -> Sneaking (Vanilla) or (Crouching) from height
//                                }
//                                if (keyHeldFlag)
//                                    stringBuilder.append(" (Key Held)");
//
//                                flag = handleGetIsEnabled(isSprintingElseDone, flag, stringBuilder, "Sprinting (Vanilla)");
//
//                                if (isFlying) {
//                                    StringBuilder flyingBuilder = new StringBuilder();
//                                    boolean flyingFlag = false;
//                                    if (JUMP_VANILLA.isDown()) {
//                                        flyingBuilder.append("Ascending");
//                                        flyingFlag = true;
//                                    }
//                                    if (isSneaking) {
//                                        if (flyingFlag)
//                                            flyingBuilder.append(", ");
//                                        flyingBuilder.append("Descending");
//                                        flyingFlag = true;
//                                    }
//                                    if (!flyingFlag) {
//                                        flyingBuilder.append("Flying");
//                                    }
////                                    if (player.getAbilities().getFlyingSpeed() != BASE_FLY_SPEED)
////                                        flyingBuilder.append(" (")
////                                                .append(player.getAbilities().getFlyingSpeed() / BASE_FLY_SPEED)
////                                                .append("x boost)");
//                                    stringBuilder.append(flyingBuilder);
//
//                                    flag = true;
//                                }
//                                stringBuilder.append("]  "); // double space from original mod
//                            }
//                        }
//                        Window window = MINECRAFT_CLIENT_INSTANCE.getWindow();
//                        int width = window.getGuiScaledWidth();
//                        if (flag) {
//                            String finalText = stringBuilder.toString();
//                            context.text(TEXT_RENDERER,
//                                    finalText,
//                                    width - TEXT_RENDERER.width(finalText) - 1,
//                                    1,
//                                    0xffffff);
//                        }
//                    });
        }

        // exampleLayer
        {
            final Identifier EXAMPLE_LAYER = Identifier.fromNamespaceAndPath("pvputils2", "hud-example-layer");
            HudElementRegistry.attachElementBefore(
                    VanillaHudElements.CHAT,
                    EXAMPLE_LAYER,
                    (context, _) -> {
                        for (var each : tempWaypoints) {
                            Vector2i screenCoords = calculateScreenCoords(each.coordinate);
                            int x = screenCoords.x;
                            int y = screenCoords.y;
                            int size = 16;

                            // TODO -> diamond
                            context.fill(
                                    x - size / 2,
                                    y - size / 2,
                                    x + (size + 1) / 2,
                                    y + (size + 1) / 2,
                                    0xFFFF69B4
                            );
                            // TODO -> make it centered on the waypoint
                            drawText(x, each.title, y, context);
                        }
                        playerOverlay.render(MINECRAFT_CLIENT_INSTANCE, this::calculateScreenCoords);
                    });
        }

        // ai start
        ClientTickEvents.END_CLIENT_TICK.register(playerOverlay::tick);
        // codex start
//        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> playerOverlay.close());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            playerOverlay.close();
            ExternalConfigWindow.close();
        });
        // codex end
        // ai end

        // messageCoordsListener
        ClientReceiveMessageEvents.CHAT.register((
                message,
                _,
                _,
                _,
                _) -> onIncomingMessage(message.getString()));
        ClientReceiveMessageEvents.GAME.register((message, _) -> onIncomingMessage(message.getString()));

        ClientTickEvents.START_CLIENT_TICK.register((client) -> {
            if (client.player instanceof LocalPlayer player) {
                checkAndJump(player, client);
            }
        });
        // TODO -> there's probably no use for this that won't flag speed
//        ClientTickEvents.START_CLIENT_TICK.register((client) -> {
//            if (client.player instanceof LocalPlayer player) {
//                if (getIsKeyBindingPressed(JUMP_VANILLA) && player.onGround()) {
//                    player.jumpFromGround();
//                }
//            }
//        });
    }

    private Vector2i calculateScreenCoords(Vec3 worldPos) {
        // TODO -> apparently JOML provides helpers that can simplify all this
        Vec3 cameraPos = cameraRenderState.pos;
        // world space -> camera-relative world space
        Vec3 cameraRelativeWorldPos = worldPos.subtract(cameraPos);
        Vector4f result = new Vector4f(
                (float) cameraRelativeWorldPos.x,
                (float) cameraRelativeWorldPos.y,
                (float) cameraRelativeWorldPos.z,
                1.0f // ?
        );
        // camera-relative -> camera space
        new Quaternionf(cameraRenderState.orientation).conjugate().transform(result); // TODO -> val
        // camera space -> clip space
        cameraRenderState.projectionMatrix.transform(result);

        if (Math.abs(result.w()) < 0.00001f) result.w = Math.copySign(0.00001f, result.w()); // TODO ?F
        float ndcX = result.x() / result.w();
        float ndcY = result.y() / result.w();

        if (result.w() < 0) {
            ndcX = -ndcX;
            ndcY = -ndcY;
            float max = Math.max(Math.abs(ndcX), Math.abs(ndcY));
            if (max > 0.0f) {
                ndcX /= max;
                ndcY /= max;
            }
        }
        ndcX = Math.clamp(ndcX, -1.0f, 1.0f);
        ndcY = Math.clamp(ndcY, -1.0f, 1.0f);

        Window window = MINECRAFT_CLIENT_INSTANCE.getWindow();
        int screenX = (int) ((ndcX + 1) / 2 * window.getGuiScaledWidth());
        int screenY = (int) ((1 - ndcY) / 2 * window.getGuiScaledHeight());
        return new Vector2i(screenX, screenY);
    }

//    private void drawPlayerWaypoint(
//            Vec3 worldPos, GuiGraphicsExtractor drawContext, AbstractClientPlayer player) {
//        var screenCoords = calculateScreenCoords(worldPos);
//        int screenX = screenCoords.x;
//        int screenY = screenCoords.y;
//        int size = 12;
//        int backgroundSize = size + 4;
//        // TODO -> diamond? w/ face cropped
//        drawContext.fill(
//                screenX - backgroundSize / 2,
//                screenY - backgroundSize / 2,
//                screenX + (backgroundSize + 1) / 2,
//                screenY + (backgroundSize + 1) / 2,
//                config.nameplateUuids.get(player.getUUID()) instanceof Config.NameplateTeam team
//                        ? 0xFF000000 | team.color.getValue()
//                        : 0xAFFF0000
//        );
////            TODO; // config enum option for only doing teammates etc.
//        PlayerFaceExtractor.extractRenderState(
//                drawContext,
//                player.getSkin(),
//                screenX - size / 2,
//                screenY - size / 2,
//                size);
//
//        // distance
//        if (MINECRAFT_CLIENT_INSTANCE.player instanceof LocalPlayer clientPlayerEntity) {
//            double distance = clientPlayerEntity.position().distanceTo(worldPos);
//            String distanceText = String.format("%.1fm", distance);
//            drawText(screenX, distanceText, screenY + size / 2 + 2 + TEXT_RENDERER.lineHeight / 2, drawContext);
//        }
//        // hovered
//        {
//            Vector3f forward = new Vector3f(0, 0, -1);
//            cameraRenderState.orientation.transform(forward);
//            Vec3 look = new Vec3(forward.x, forward.y, forward.z).normalize();
//            Vec3 toMarker = worldPos.subtract(cameraRenderState.pos).normalize();
//            if (look.dot(toMarker) > 0.995) {
//                // TODO -> this could use the supabase username for mod users? + accounts could have nicknames set
//                // TODO -> extra info should also appear when MOUSED over
//                // name
//                {
//                    String name = player.getScoreboardName();
//                    drawText(
//                            screenX,
//                            name,
//                            screenY - size / 2 - TEXT_RENDERER.lineHeight / 2 - 2,
//                            drawContext);
//                }
//                // coords
//                {
//                    String coordinates = String.format(
//                            "%.0f, %.0f, %.0f",
//                            worldPos.x,
//                            worldPos.y,
//                            worldPos.z
//                    );
//                    drawText(
//                            screenX,
//                            coordinates,
//                            screenY - size / 2 - TEXT_RENDERER.lineHeight * 3 / 2 - 4,
//                            drawContext
//                    );
//                }
//            }
//        }
//    }

    private void onIncomingMessage(String message) {
        // TODO -> async?
        // TODO -> handle two coordinates. which would require a beacon or something
        // TODO -> handle dimensions?
        // TODO -> support decimals
        // TODO -> handle x, y, z differently
        // TODO -> once I find one, iterate until I reach the beginning of the message
        // which could contain other ones, which turns this into a leetCode problem
        ArrayList<Integer> numbers = new ArrayList<>();
        StringBuilder numberBuilder = new StringBuilder();
        for (int i = message.length() - 1; i >= 0; --i) {
            char c = message.charAt(i);
            if (Character.isDigit(c)) {
                numberBuilder.insert(0, c);
                continue;
            }
            if (!Set.of(' ', '.', ',', 'x', 'y', 'z', ':', '-').contains(Character.toLowerCase(c))) {
                numbers.clear();
                numberBuilder.setLength(0);
                continue;
            }
            if (!numberBuilder.isEmpty()) {
                if (c == '-') {
                    numberBuilder.insert(0, c);
                }
                int foo;
                try {
                    foo = Integer.parseInt(numberBuilder.toString());
                } catch (NumberFormatException e) {
                    return;
                }
                numbers.addFirst(foo);
                numberBuilder.setLength(0);
                if (numbers.size() == 3) {
//                    while (Set.of(' ', '.', ',', 'x', 'y', 'z', ':', '-').contains(Character.toLowerCase(message.charAt(i)))) {
//                        --i; // TODO -> make constant for this set
//                    } TODO
                    tempWaypoints.add(new TempWaypoint(
                            message.subSequence(0, i).toString(),
                            new Vec3(numbers.get(0), numbers.get(1), numbers.get(2))));
                    SCHEDULED_EXECUTOR_SERVICE.schedule(
                            () -> Minecraft.getInstance().execute(tempWaypoints::removeFirst), // TODO -> this bad
                            15,
                            TimeUnit.SECONDS);
                    return; // TODO -> multiple rallies in one message
                }
            }
        }
    }

    private static boolean handleGetIsEnabled(
            boolean isEnabled, boolean flag, StringBuilder stringBuilder, String string) {
        if (isEnabled) {
            if (flag)
                stringBuilder.append(", ");
            stringBuilder.append(string);
            return true;
        }
        return flag;
    }

    private static void drawText(
            int screenX,
            String text,
            int screenY,
            GuiGraphicsExtractor drawContext) {
        int textX = screenX - TEXT_RENDERER.width(text) / 2;
//        int textY = screenY + size / 2 + 2;
        int textY = screenY - TEXT_RENDERER.lineHeight / 2;

        drawContext.text(
                TEXT_RENDERER,
                text,
                textX,
                textY,
                0xFFFFFFFF
        );
    }

    // TODO -> this should simulate where the player is gonna be in the next tick so it can be at the last possible moment
    public double minDepth = 0.5;
    public double edgeDistance = 0.001;

    private void checkAndJump(LocalPlayer player, Minecraft client) {
        if (!config.isParkourCheatEnabled) {
            return;
        }
        if (!player.onGround()) {
            return;
        }
        Vec3 velocity = player.getDeltaMovement();
        Vec3 horizontalVec = new Vec3(velocity.x, 0, velocity.z);
        if (horizontalVec.lengthSqr() < 0.00001) {
            return;
        }

        Vec3 direction = horizontalVec.normalize();
        Vec3 offset = direction.multiply(edgeDistance, edgeDistance, edgeDistance);
        Vec3 totalOffset = new Vec3(velocity.x, 0, velocity.z).add(offset);

        AABB currentBox = player.getBoundingBox();
        AABB aheadBox = currentBox.move(totalOffset.x, 0, totalOffset.z);
        AABB dropBox = aheadBox.move(0, -0.05, 0)
                .expandTowards(0, -minDepth, 0);

        if (client.level instanceof ClientLevel level && !level.getBlockCollisions(player, dropBox).iterator().hasNext()) {
            player.jumpFromGround();
        }
    }
}
