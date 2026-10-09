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
import java.util.Arrays;
import java.util.List;

/** Minecraft controls for the independent aim-only port, with persistent CamwenMod settings. */
public final class KillAuraScreenTodoAi extends Screen {
    private final Screen parent;
    private final Object settings;
    private int page;
    public KillAuraScreenTodoAi(Screen parent) {
        this(parent, "KillAura (aim only)", KillAuraControllerTodoAi.config());
    }
    private KillAuraScreenTodoAi(Screen parent, String title, Object settings) {
        super(Component.literal(title)); this.parent = parent; this.settings = settings;
    }
    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        ExternalConfigWindow.close();
        mc.setScreenAndShow(new KillAuraScreenTodoAi(mc.gui.screen()));
    }
    private void button(String text, int y, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(text), b -> action.run())
                .bounds((width - 320) / 2, y, 320, 20).build());
    }
    @Override protected void init() {
        List<Field> fields = Arrays.asList(settings.getClass().getFields());
        int rows = Math.max(1, (height - 104) / 24), pages = Math.max(1, (fields.size() + rows - 1) / rows);
        page = Math.clamp(page, 0, pages - 1);
        for (int i = page * rows; i < Math.min(fields.size(), (page + 1) * rows); i++) {
            Field field = fields.get(i);
            try {
                Object value = field.get(settings);
                String text = label(field.getName()) + (group(field) ? "..." : ": " + value);
                button(text, 42 + (i - page * rows) * 24, () -> edit(field));
            } catch (IllegalAccessException e) { throw new IllegalStateException(e); }
        }
        if (page > 0) addRenderableWidget(Button.builder(Component.literal("Previous"), b -> {
            page--; rebuildWidgets();
        }).bounds(width / 2 - 160, height - 52, 100, 20).build());
        if (page + 1 < pages) addRenderableWidget(Button.builder(Component.literal("Next"), b -> {
            page++; rebuildWidgets();
        }).bounds(width / 2 + 60, height - 52, 100, 20).build());
        button("Done", height - 28, this::onClose);
    }
    private static boolean group(Field field) {
        return field.getType().getEnclosingClass() == KillAuraConfigTodoAi.class && !field.getType().isEnum();
    }
    private void edit(Field field) {
        try {
            if (group(field)) {
                Minecraft.getInstance().setScreenAndShow(new KillAuraScreenTodoAi(this, label(field.getName()), field.get(settings)));
            } else if (field.getType() == boolean.class) {
                field.setBoolean(settings, !field.getBoolean(settings)); save(); rebuildWidgets();
            } else if (field.getType().isEnum()) {
                Object[] choices = field.getType().getEnumConstants();
                int index = Arrays.asList(choices).indexOf(field.get(settings));
                field.set(settings, choices[(index + 1) % choices.length]); save(); rebuildWidgets();
            } else Minecraft.getInstance().setScreenAndShow(new NumberScreenTodoAi(this, settings, field));
        } catch (IllegalAccessException e) { throw new IllegalStateException(e); }
    }
    private static String label(String name) {
        String spaced = name.replaceAll("([a-z])([A-Z])", "$1 $2");
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
    private static void save() { UntitledClient.config.saveConfig(); }
    @Override public void onClose() { save(); Minecraft.getInstance().setScreenAndShow(parent); }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float partial) {
        super.extractRenderState(graphics, x, y, partial);
        graphics.centeredText(font, title, width / 2, 10, -1);
        if (settings instanceof KillAuraConfigTodoAi)
            graphics.centeredText(font, Component.literal("Players only. Aims without clicking or attacking."), width / 2, 25, 0xFFAAAAAA);
    }
    public static void applyNumber(Object owner, Field field, String text) throws IllegalAccessException {
        double value;
        try { value = Double.parseDouble(text); } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Enter a finite number");
        }
        double max = 180, min = 0;
        String name = field.getName();
        if (name.equals("margin")) max = 1;
        else if (name.startsWith("delay")) max = 5;
        else if (name.startsWith("lazy")) { min = 0.01; max = 0.4; }
        else if (name.contains("Percent") || name.equals("gaussianChance")) max = 100;
        else if (name.equals("steepness")) max = 20;
        else if (name.equals("gaussianTolerance")) { min = 0.01; max = 0.1; }
        else if (name.contains("Midpoint") || name.startsWith("exempt") || name.startsWith("gaussian")) max = 1;
        if (!Double.isFinite(value) || value < min || value > max ||
                field.getType() == int.class && value != Math.rint(value))
            throw new IllegalArgumentException("Enter " + min + " through " + max);
        if (field.getType() == int.class) field.setInt(owner, (int) value); else field.setDouble(owner, value);
    }
    private static final class NumberScreenTodoAi extends Screen {
        private final Screen parent;
        private final Object owner;
        private final Field field;
        private EditBox input;
        private String error = "";
        NumberScreenTodoAi(Screen parent, Object owner, Field field) {
            super(Component.literal(label(field.getName()))); this.parent = parent; this.owner = owner; this.field = field;
        }
        @Override protected void init() {
            input = new EditBox(font, (width - 320) / 2, height / 2 - 24, 320, 20, title);
            input.setMaxLength(64);
            try { input.setValue(String.valueOf(field.get(owner))); }
            catch (IllegalAccessException e) { throw new IllegalStateException(e); }
            addRenderableWidget(input); setInitialFocus(input);
            addRenderableWidget(Button.builder(Component.literal("Save"), b -> {
                try { applyNumber(owner, field, input.getValue()); save(); onClose(); }
                catch (IllegalArgumentException | IllegalAccessException e) { error = e.getMessage(); }
            }).bounds((width - 320) / 2, height / 2 + 4, 320, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                    .bounds((width - 320) / 2, height - 28, 320, 20).build());
        }
        @Override public void onClose() { Minecraft.getInstance().setScreenAndShow(parent); }
        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float partial) {
            super.extractRenderState(graphics, x, y, partial);
            graphics.centeredText(font, title, width / 2, 10, -1);
            graphics.centeredText(font, Component.literal(error), width / 2, height / 2 + 34, 0xFFFF5555);
        }
    }
}
