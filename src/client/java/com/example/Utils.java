package com.example;

import com.example.Configs.CheatConfig;
import com.example.Configs.Config;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import org.lwjgl.glfw.GLFW;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

import static com.example.Constants.GSON;
import static com.example.Constants.MINECRAFT_CLIENT_INSTANCE;
import static com.example.UntitledClient.*;


public class Utils {
    public static boolean getIsKeyPressed(int glfwKeybind) {
        return glfwKeybind != -1 && GLFW.glfwGetKey(MINECRAFT_CLIENT_INSTANCE.getWindow().getWindow(), glfwKeybind) == GLFW.GLFW_PRESS; //codex (old code snippet) return glfwKeybind != -1 && GLFW.glfwGetKey(MINECRAFT_CLIENT_INSTANCE.getWindow().handle(), glfwKeybind) == GLFW.GLFW_PRESS;
    }

    public static boolean getIsKeyBindingPressed(KeyMapping keyBinding) {
        InputConstants.Key key = InputConstants.getKey(keyBinding.saveString());
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return getIsKeyPressed(InputConstants.getKey(keyBinding.saveString()).getValue());
        } else if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(MINECRAFT_CLIENT_INSTANCE.getWindow().getWindow(), key.getValue()) == GLFW.GLFW_PRESS; //codex (old code snippet) return GLFW.glfwGetMouseButton(MINECRAFT_CLIENT_INSTANCE.getWindow().handle(), key.getValue()) == GLFW.GLFW_PRESS;
        } else {
            Objects.requireNonNull(null);
            return false;
        }
    }

    // TODO -> enum
    public static void onXrayChange() {
        isPlayerXrayEnabled = !isPlayerXrayEnabled;
        MINECRAFT_CLIENT_INSTANCE.levelRenderer.allChanged(); //codex (old code snippet) MINECRAFT_CLIENT_INSTANCE.levelRenderer.invalidateCompiledGeometry(Objects.requireNonNull(MINECRAFT_CLIENT_INSTANCE.level), MINECRAFT_CLIENT_INSTANCE.options, MINECRAFT_CLIENT_INSTANCE.gameRenderer.mainCamera(), MINECRAFT_CLIENT_INSTANCE.getBlockColors());
    }

    public static void onPvpDamage() {
//        if (config.isMovementTogglePvpDisabling) {
//            doMovementToggleDisable();
//        }
        config.playerWaypointCategory = Config.PlayerWaypointCategory.NONE;

//        if (config.isGuiCheatsPvpDisabling && !Objects.equals(currentXrayType, "")) {
//            onXrayChange("");
//        }
    }

    public static <T> void serializeJsonBlocking(String fileNamePrefix, T jsonCompliantObject) {
        try (FileWriter writer = new FileWriter("pvputils-" + fileNamePrefix + ".json")) {
            GSON.toJson(jsonCompliantObject, writer);
        } catch (IOException e) {
            Minecraft minecraftClient = Minecraft.getInstance();
            if (minecraftClient.player instanceof LocalPlayer player)
                minecraftClient.execute(() -> player.displayClientMessage(Component.literal("serialization failed"), false)); //codex (old code snippet) minecraftClient.execute(() -> player.sendSystemMessage(Component.literal("serialization failed")));
        }
    }

    public static <T> T getDeserializedJsonBlocking(String fileNamePrefix, Type clazz) {
        try (FileReader reader = new FileReader("pvputils-" + fileNamePrefix + ".json")) {
            return GSON.fromJson(reader, clazz);
        } catch (IOException e) {
            if (!(e instanceof FileNotFoundException)) {
                Minecraft minecraftClient = Minecraft.getInstance();
                if (minecraftClient.player instanceof LocalPlayer player)
                    minecraftClient.execute(() -> player.displayClientMessage(Component.literal("deserialization failed: " + e.getMessage()), false)); //codex (old code snippet) minecraftClient.execute(() -> player.sendSystemMessage(Component.literal("deserialization failed: " + e.getMessage())));
                // TODO -> console this
            }
            return null;
        }
    }

//    private static class saveTaskEntry {
//        public AtomicBoolean atomicBoolean;
//        public Boolean bool;
//        public saveTaskEntry() {
//            this.atomicBoolean = new AtomicBoolean(false);
//            this.bool = false;
//        }
//    }
//    private static final HashMap<String, saveTaskEntry> saveTaskData = new HashMap<>();
//    public static <T> void handleUnsafeJsonSave(String fileNamePrefix, T object) {
//        try {
//            saveTaskData.putIfAbsent(fileNamePrefix, new saveTaskEntry());
//            if (saveTaskData.get(fileNamePrefix).atomicBoolean.compareAndSet(false, true)) {
//                var entry = saveTaskData.get(fileNamePrefix);
//                new Thread(() -> {
//                    entry.bool = true;
//                    while (entry.bool) {
//                        entry.bool = false;
//                        serializeJsonBlocking(fileNamePrefix, object);
//                    }
//                    entry.atomicBoolean.set(false);
//                }).start();
//            }
//            else
//                saveTaskData.get(fileNamePrefix).bool = true; // TODO -> this can race condition (?)
//        }
//        catch (Exception e) {
//            if (MINECRAFT_CLIENT_INSTANCE.player instanceof ClientPlayerEntity player)
//                player.sendMessage(Text.literal("unsafejsonsave err: " + e.getMessage()), false);
//        }

    public static Screen buildConfigScreen(
            String name, List<AbstractWidget> clickableWidgets) {
        return new Screen(Component.literal(name)) {
            @Override
            protected void init() {
                int y = 20;
                int x = 20;
                for (int i = 0; i < clickableWidgets.size(); ++i) {
                    int column = i % 4;
                    int row = i / 4;
                    AbstractWidget widget = clickableWidgets.get(i);
                    widget.setPosition(x + 150 * column, y + 20 * row);
                    addRenderableWidget(widget);
                }
            }
        };
    }

    private static String computeServerName() {
        if (MINECRAFT_CLIENT_INSTANCE.getCurrentServer() instanceof ServerData serverInfo) {
            return serverInfo.ip;
        }
        return "singlePlayer";
    }

    public static CheatConfig computeCheatConfig() {
        return cheatConfigs.computeIfAbsent(computeServerName(), unusedTodoAi1 -> new CheatConfig()); //codex (old code snippet) return cheatConfigs.computeIfAbsent(computeServerName(), _ -> new CheatConfig());
    }

    public static ItemStack buildReplacementTeamLeatherItemStack(
            ItemStack original, Item replacement, int color) {
        ItemStack replacementStack = replacement.getDefaultInstance();
//        replacementStack.applyComponentsFrom(original.getComponents());
        replacementStack.set(
                DataComponents.DYED_COLOR,
                new DyedItemColor(color, true) //codex (old code snippet) new DyedItemColor(color)
        );
        replacementStack.set(
                DataComponents.ENCHANTMENTS,
                original.getEnchantments()
        );
        return replacementStack;
    }

    private static final String PVP_UTILS = "key.categories.pvputils"; //codex (old code snippet) private static final KeyMapping.Category PVP_UTILS = Objects.requireNonNull(KeyMapping.Category.register(Identifier.fromNamespaceAndPath("pvputils", "pvp_utils")));

    public static KeyMapping getAbstractPvpUtilsKeybind(String name) {
        return KeyBindingHelper.registerKeyBinding(new KeyMapping( //codex (old code snippet) return KeyMappingHelper.registerKeyMapping(new KeyMapping(
                name,
//                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                Objects.requireNonNull(PVP_UTILS)
        ));
    }
}
