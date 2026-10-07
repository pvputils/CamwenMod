package com.example.killaura;

import com.example.UntitledClient;
import com.example.overlayTodoAi.ExternalConfigWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.lang.reflect.Field;
import java.util.*;

/** The same native, paginated settings approach used by CamwenMod's AimAssist. */
public final class KillAuraScreenTodoAi extends Screen {
    private final Screen parent;
    private final Object owner;
    private int page;
    public KillAuraScreenTodoAi(Screen parent) { this(parent, "KillAura", KillAuraControllerTodoAi.config()); }
    private KillAuraScreenTodoAi(Screen parent, String title, Object owner) {
        super(Component.literal(title)); this.parent = parent; this.owner = owner;
    }
    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        ExternalConfigWindow.close();
        mc.setScreenAndShow(new KillAuraScreenTodoAi(mc.gui.screen()));
    }
    @Override protected void init() {
        List<Field> fields = Arrays.asList(owner.getClass().getFields());
        int rows = Math.max(1, (height - 100) / 24), pages = Math.max(1, (fields.size() + rows - 1) / rows);
        page = Math.clamp(page, 0, pages - 1);
        for (int i = page * rows; i < Math.min(fields.size(), (page + 1) * rows); i++) {
            Field field = fields.get(i);
            try {
                Object value = field.get(owner);
                boolean group = group(field);
                button(label(field.getName()) + (group ? "..." : ": " + value), 32 + (i - page * rows) * 24, () -> edit(field));
            } catch (IllegalAccessException error) { throw new IllegalStateException(error); }
        }
        if (page > 0) addRenderableWidget(Button.builder(Component.literal("Previous"), b -> { page--; rebuildWidgets(); }).bounds((width - 300) / 2, height - 54, 145, 20).build());
        if (page + 1 < pages) addRenderableWidget(Button.builder(Component.literal("Next"), b -> { page++; rebuildWidgets(); }).bounds(width / 2 + 5, height - 54, 145, 20).build());
        button("Done", height - 28, this::onClose);
    }
    private void button(String text, int y, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(text), b -> action.run()).bounds((width - 300) / 2, y, 300, 20).build());
    }
    private static boolean group(Field field) { return !field.getType().isPrimitive() && field.getType() != String.class && field.getType() != List.class; }
    private static String label(String field) { return Character.toUpperCase(field.charAt(0)) + field.substring(1).replaceAll("([a-z])([A-Z])", "$1 $2"); }
    private void edit(Field field) {
        try {
            if (group(field)) Minecraft.getInstance().setScreenAndShow(new KillAuraScreenTodoAi(this, label(field.getName()), field.get(owner)));
            else if (field.getType() == boolean.class) { field.setBoolean(owner, !field.getBoolean(owner)); save(); rebuildWidgets(); }
            else Minecraft.getInstance().setScreenAndShow(new ValueScreenTodoAi(this, owner, field));
        } catch (IllegalAccessException error) { throw new IllegalStateException(error); }
    }
    static void save() { UntitledClient.config.saveConfig(); }
    @Override public void onClose() { save(); Minecraft.getInstance().setScreenAndShow(parent); }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float partial) {
        super.extractRenderState(graphics, x, y, partial);
        graphics.centeredText(font, title, width / 2, 10, -1);
    }
    private static final class ValueScreenTodoAi extends Screen {
        private final Screen parent; private final Object owner; private final Field field;
        private EditBox input; private String error = "";
        ValueScreenTodoAi(Screen parent, Object owner, Field field) {
            super(Component.literal(label(field.getName()))); this.parent = parent; this.owner = owner; this.field = field;
        }
        @Override protected void init() {
            input = new EditBox(font, (width - 300) / 2, height / 2 - 24, 300, 20, title);
            input.setMaxLength(256);
            try { Object value = field.get(owner); input.setValue(value instanceof List<?> list ? String.join(",", list.stream().map(String::valueOf).toList()) : String.valueOf(value)); }
            catch (IllegalAccessException error) { throw new IllegalStateException(error); }
            addRenderableWidget(input); setInitialFocus(input);
            addRenderableWidget(Button.builder(Component.literal("Save"), b -> {
                try { apply(owner, field, input.getValue()); save(); onClose(); }
                catch (IllegalArgumentException | IllegalAccessException error) { this.error = error.getMessage(); }
            }).bounds((width - 300) / 2, height / 2 + 4, 300, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose()).bounds((width - 300) / 2, height - 28, 300, 20).build());
        }
        @Override public void onClose() { Minecraft.getInstance().setScreenAndShow(parent); }
        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float partial) {
            super.extractRenderState(graphics, x, y, partial);
            graphics.centeredText(font, title, width / 2, 10, -1);
            graphics.centeredText(font, Component.literal(error), width / 2, height / 2 + 34, 0xFFFF5555);
        }
    }
    public static void apply(Object owner, Field field, String text) throws IllegalAccessException {
        String name = field.getName();
        if (field.getType() == boolean.class) {
            if (!Set.of("true", "false").contains(text)) throw new IllegalArgumentException("Enter true or false");
            field.setBoolean(owner, Boolean.parseBoolean(text)); return;
        }
        if (field.getType() == int.class || field.getType() == double.class) {
            if (name.equals("color")) {
                long color = Long.parseUnsignedLong(text.replace("#", "").replace("0x", ""), 16);
                if (color > 0xFFFFFFFFL) throw new IllegalArgumentException("Enter ARGB hex, e.g. FF50FF50");
                field.setInt(owner, (int) color); return;
            }
            double min = 0, max = 180;
            if (owner instanceof KillAuraConfigTodoAi.Interpolation) max = name.equals("midpoint") ? 1 : 100;
            if (name.equals("midpoint") || name.endsWith("Error") || name.equals("factor")) max = 1;
            if (name.equals("steepness")) max = 20;
            if (name.equals("hurtTime")) max = 10;
            if (name.equals("ticksUntilReset")) { min = 1; max = 30; }
            if (name.equals("resetThreshold")) min = 1;
            if (name.equals("distanceCoefficient")) { min = -2; max = 2; }
            if (name.equals("rate")) { min = 1; max = owner instanceof KillAuraConfigTodoAi.ShortStop ? 25 : 100; }
            if (name.startsWith("duration")) { min = owner instanceof KillAuraConfigTodoAi.ShortStop ? 1 : 0; max = owner instanceof KillAuraConfigTodoAi.ShortStop ? 5 : 20; }
            double value = Double.parseDouble(text);
            if (!Double.isFinite(value) || value < min || value > max || field.getType() == int.class && value != Math.rint(value)) throw new IllegalArgumentException("Enter " + min + " through " + max);
            if (field.getType() == int.class) field.setInt(owner, (int) value); else field.setDouble(owner, value);
            return;
        }
        List<String> choices = switch (name) {
            case "timing" -> List.of("Normal", "Snap", "OnTick");
            case "smoothing" -> List.of("Linear", "Sigmoid", "Interpolation", "Acceleration");
            case "movementCorrection" -> List.of("Off", "Strict", "Silent", "ChangeLook");
            case "raycast" -> List.of("All", "OnlyEnemy", "None");
            case "priorities" -> List.of("Type", "Health", "Distance", "Direction", "HurtTime", "Age");
            default -> List.of();
        };
        if (field.getType() == List.class) {
            var priorities = Arrays.stream(text.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
            if (priorities.isEmpty() || !choices.containsAll(priorities)) throw new IllegalArgumentException("Ordered list: " + String.join(",", choices));
            field.set(owner, new ArrayList<>(new LinkedHashSet<>(priorities)));
        } else {
            if (!choices.isEmpty() && !choices.contains(text)) throw new IllegalArgumentException("Enter " + String.join(", ", choices));
            field.set(owner, text);
        }
    }
}
