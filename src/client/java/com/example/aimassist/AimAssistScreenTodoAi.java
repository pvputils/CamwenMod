package com.example.aimassist;

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

/** Native config for the two independent assists, opened from CamwenMod's config button. */
public final class AimAssistScreenTodoAi extends Screen {
    private final Screen parent;
    private final Object settings;
    private int page;
    public AimAssistScreenTodoAi(Screen parent) { this(parent, "Aim assist", null); }
    private AimAssistScreenTodoAi(Screen parent, String title, Object settings) {
        super(Component.literal(title)); this.parent = parent; this.settings = settings;
    }
    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        ExternalConfigWindow.close();
        mc.setScreenAndShow(new AimAssistScreenTodoAi(mc.gui.screen()));
    }
    private void button(String text, int y, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(text), b -> action.run())
            .bounds((width - 300) / 2, y, 300, 20).build());
    }
    @Override protected void init() {
        var cfg = AimAssistControllerTodoAi.config();
        if (settings == null) {
            button("aura", height / 2 - 28, () -> openGroup("aura", cfg.aura));
            button("targetting", height / 2, () -> openGroup("targetting", cfg.targetting));
        } else {
            List<Field> fields = new ArrayList<>(Arrays.asList(settings.getClass().getFields()));
            boolean aura = settings == cfg.aura, targeting = settings == cfg.targetting;
            if (targeting) fields.removeIf(f -> Set.of("range", "targetingMargin", "hurtTime", "horizontal", "vertical", "priorities").contains(f.getName()));
            int rows = Math.max(1, (height - 88) / 24);
            int total = fields.size() + (aura || targeting ? 2 : 0);
            int pages = Math.max(1, (total + rows - 1) / rows);
            page = Math.clamp(page, 0, pages - 1);
            for (int index = page * rows; index < Math.min(total, (page + 1) * rows); index++) {
                int y = 32 + (index - page * rows) * 24;
                if (index >= fields.size()) {
                    if (index == fields.size()) button("Targets...", y, () -> openGroup("Targets", cfg.targets));
                    else button("TargetLock...", y, () -> openGroup("TargetLock", cfg.targetLock));
                    continue;
                }
                Field field = fields.get(index);
                try {
                    Object value = field.get(settings);
                    String name = label(field.getName());
                    boolean group = value instanceof AimAssistConfigTodoAi.Requirements || value instanceof AimAssistConfigTodoAi.Interpolation;
                    button(name + (group ? "..." : ": " + value), y, () -> edit(field, group));
                } catch (IllegalAccessException error) { throw new IllegalStateException(error); }
            }
            if (page > 0) button("Previous", height - 52, () -> { page--; rebuildWidgets(); });
            if (page + 1 < pages) {
                addRenderableWidget(Button.builder(Component.literal("Next"), b -> { page++; rebuildWidgets(); })
                    .bounds(width / 2 + 60, height - 52, 90, 20).build());
            }
        }
        button("Done", height - 28, this::onClose);
    }
    private void openGroup(String name, Object group) {
        Minecraft.getInstance().setScreenAndShow(new AimAssistScreenTodoAi(this, name, group));
    }
    private void edit(Field field, boolean group) {
        try {
            if (group) { openGroup(label(field.getName()), field.get(settings)); return; }
            if (field.getType() == boolean.class) {
                field.setBoolean(settings, !field.getBoolean(settings)); save(); rebuildWidgets(); return;
            }
            Minecraft.getInstance().setScreenAndShow(new ValueScreenTodoAi(this, settings, field, settings == AimAssistControllerTodoAi.config().aura.interpolation)); //codex (old code snippet) Minecraft.getInstance().setScreenAndShow(new ValueScreenTodoAi(this, settings, field));
        } catch (IllegalAccessException error) { throw new IllegalStateException(error); }
    }
    private static String label(String field) {
        // codex start
        if (field.equals("reach")) return "Reach addition";
        //codex end
        if (field.equals("targetingMargin")) return "Targeting margin bypass addition";
        return Character.toUpperCase(field.charAt(0)) + field.substring(1);
    }
    static void save() { UntitledClient.config.saveConfig(); }
    @Override public void onClose() {
        save(); Minecraft.getInstance().setScreenAndShow(parent);
        if (parent == null) ExternalConfigWindow.show();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        super.extractRenderState(graphics, mouseX, mouseY, partial);
        graphics.centeredText(font, title, width / 2, 10, -1);
    }
    private static final class ValueScreenTodoAi extends Screen {
        private final Screen parent;
        private final Object owner;
        private final Field field;
        // codex start
        private final boolean auraScaling;
        //codex end
        private EditBox input;
        private String error = "";
        ValueScreenTodoAi(Screen parent, Object owner, Field field, boolean auraScaling) { //codex (old code snippet) ValueScreenTodoAi(Screen parent, Object owner, Field field) {
            super(Component.literal(label(field.getName()))); this.parent = parent; this.owner = owner; this.field = field; this.auraScaling = auraScaling; //codex (old code snippet) super(Component.literal(label(field.getName()))); this.parent = parent; this.owner = owner; this.field = field;
        }
        @Override protected void init() {
            input = new EditBox(font, (width - 300) / 2, height / 2 - 24, 300, 20, title);
            input.setMaxLength(256);
            try {
                Object value = field.get(owner);
                input.setValue(value instanceof List<?> list ? String.join(",", (List<String>) list) : String.valueOf(value));
            } catch (IllegalAccessException e) { throw new IllegalStateException(e); }
            addRenderableWidget(input);
            setInitialFocus(input);
            addRenderableWidget(Button.builder(Component.literal("Save"), b -> {
                try { apply(owner, field, input.getValue(), auraScaling); save(); onClose(); } //codex (old code snippet) try { apply(owner, field, input.getValue()); save(); onClose(); }
                catch (IllegalArgumentException | IllegalAccessException e) { error = e.getMessage(); }
            }).bounds((width - 300) / 2, height / 2 + 4, 300, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds((width - 300) / 2, height - 28, 300, 20).build());
        }
        @Override public void onClose() { Minecraft.getInstance().setScreenAndShow(parent); }
        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float partial) {
            super.extractRenderState(graphics, x, y, partial);
            graphics.centeredText(font, title, width / 2, 10, -1);
            graphics.centeredText(font, Component.literal(error), width / 2, height / 2 + 32, 0xFFFF5555);
        }
    }
    public static void apply(Object owner, Field field, String text) throws IllegalAccessException {
        // codex start
        apply(owner, field, text, false);
    }
    public static void apply(Object owner, Field field, String text, boolean auraScaling) throws IllegalAccessException {
        //codex end
        String name = field.getName();
        if (field.getType() == int.class || field.getType() == double.class) {
            double value = Double.parseDouble(text);
            double min = 0, max = 100;
            switch (name) {
                case "range" -> { min = 1; max = 8; }
                case "targetingMargin" -> max = 8;
                case "hurtTime" -> max = 10;
                case "attackWindow" -> max = 200;
                case "maximumTime" -> max = 120;
                case "maximumRange" -> { min = 8; max = 40; }
                case "midpoint" -> max = 1;
                case "horizontalMin", "horizontalMax", "verticalMin", "verticalMax" -> min = 1;
            }
        // codex start
            if (auraScaling && owner instanceof AimAssistConfigTodoAi.Interpolation &&
                    Set.of("horizontalMin", "horizontalMax", "verticalMin", "verticalMax", "directionMin", "directionMax").contains(name)) max = Integer.MAX_VALUE;
        //codex end
            if (!Double.isFinite(value) || value < min || value > max || field.getType() == int.class && value != Math.rint(value))
                throw new IllegalArgumentException("Enter " + min + " through " + max);
            if (field.getType() == int.class) field.setInt(owner, (int) value); else field.setDouble(owner, value);
        } else if (field.getType() == List.class) {
            List<String> list = Arrays.stream(text.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
            if (list.isEmpty() || list.stream().anyMatch(s -> !Set.of("Type", "Direction", "Health", "Distance", "HurtTime", "Age").contains(s)))
                throw new IllegalArgumentException("Ordered list: Type,Direction,Health,Distance,HurtTime,Age");
            field.set(owner, new ArrayList<>(list));
        } else {
            if (name.equals("mode") && !Set.of("Temporary", "Filter").contains(text))
                throw new IllegalArgumentException("Enter Temporary or Filter");
            field.set(owner, text);
        }
    }
}
