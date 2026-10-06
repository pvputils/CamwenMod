package com.example.overlayTodoAi;

import com.example.Configs.Config;
import com.mojang.blaze3d.platform.NativeImage;
import com.sun.jna.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Function;

import static com.example.UntitledClient.*;

// TODO -> AI made this
public final class PlayerWaypointOverlay {
    private WindowsWaypointWindow window;
    private boolean failed;
    private long lastFrame;
    private final Map<ResourceLocation, BufferedImage> faces = new HashMap<>(); //codex (old code snippet) private final Map<Identifier, BufferedImage> faces = new HashMap<>();
    // codex start
    private static final long DEBUG_MESSAGE_DURATION_NANOS = 5_000_000_000L;
    private static final ConcurrentLinkedDeque<DebugMessage> debugMessages = new ConcurrentLinkedDeque<>();
    // codex end

    private boolean visible(Minecraft client) {
        return client.level != null && client.player != null && cameraRenderState != null
                && client.screen == null && client.getOverlay() == null && !client.options.hideGui && client.isWindowActive(); //codex (old code snippet) && client.gui.screen() == null && client.gui.overlay() == null && !client.gui.hud.isHidden() && client.isWindowActive();
//                && config.playerWaypointCategory != Config.PlayerWaypointCategory.NONE;
    }

    public void tick(Minecraft client) {
        if (!visible(client)) hide();
        if (client.level == null) faces.clear();
    }

    public void render(Minecraft client, Function<Vec3, Vector2i> project) {
        if (!visible(client) || failed) {
            hide();
            return;
        }
        long now = System.nanoTime();
        // Desktop bitmap composition need not run at an uncapped Minecraft frame rate.
        if (now - lastFrame < 16_666_667L) return;
        lastFrame = now;
        try {
            if (!Platform.isWindows())
                throw new UnsupportedOperationException("Player waypoint overlay requires Windows");
            if (window == null)
                window = new WindowsWaypointWindow(GLFWNativeWin32.glfwGetWin32Window(client.getWindow().getWindow())); //codex (old code snippet) window = new WindowsWaypointWindow(GLFWNativeWin32.glfwGetWin32Window(client.getWindow().handle()));
            BufferedImage frame = window.beginFrame();
            if (frame == null) return;
            Graphics2D graphics = frame.createGraphics();
            HashSet<ResourceLocation> usedSkins = new HashSet<>(); //codex (old code snippet) HashSet<Identifier> usedSkins = new HashSet<>();
            // codex start
            boolean hasOverlayContent = false;
            // codex end
            try {
                graphics.setComposite(AlphaComposite.Clear);
                graphics.fillRect(0, 0, frame.getWidth(), frame.getHeight());
                graphics.setComposite(AlphaComposite.SrcOver);
                graphics.scale((double) frame.getWidth() / client.getWindow().getGuiScaledWidth(),
                        (double) frame.getHeight() / client.getWindow().getGuiScaledHeight());
                graphics.setFont(new Font(Font.MONOSPACED, Font.BOLD, 9));
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                Vector3f forward = cameraRenderState.orientation.transform(new Vector3f(0, 0, -1));
                Vec3 look = new Vec3(forward.x, forward.y, forward.z).normalize();
                // codex start
                if (config.isProjectileTrajectoryPreviewEnabled) {
                    hasOverlayContent |= ProjectileTrajectoryPreview.draw(graphics, client, project);
                }
                hasOverlayContent |= drawDebugMessages(graphics);
                if (config.isCameraAngleCrosshairIndicatorEnabled && client.player.getXRot() != 0.0f) {
                    hasOverlayContent = true;
                    drawCameraAngleIndicator(
                            graphics,
                            client.getWindow().getGuiScaledWidth() / 2,
                            client.getWindow().getGuiScaledHeight() / 2,
                            client.player.getXRot());
                }
                // codex end
                if (config.playerWaypointCategory != Config.PlayerWaypointCategory.NONE) {
                    for (var player : Objects.requireNonNull(client.level).players()) {
                        if (player == client.player) continue;
                        var team = config.nameplateUuids.get(player.getUUID());
                        if (config.playerWaypointCategory == Config.PlayerWaypointCategory.ENEMIES
                                && (team == Config.NameplateTeam.ALLY || team == Config.NameplateTeam.FRIENDLY))
                            continue;
                        Vec3 world = player.position().add(0, player.getBbHeight() / 2, 0);
                        Vector2i point = project.apply(world);
                        int x = point.x, y = point.y;
                        // codex start
                        boolean isClamped = x <= 0 || x >= client.getWindow().getGuiScaledWidth()
                                || y <= 0 || y >= client.getWindow().getGuiScaledHeight();
                        if (config.isUnclampedPlayerWaypointsDisabled && !isClamped) continue;
                        hasOverlayContent = true;
                        // codex end
                        graphics.setColor(new Color(team == null ? 0xAFFF0000 : 0xFF000000 | team.color.getValue(), true));
                        graphics.fillRect(x - 8, y - 8, 16, 16);
                        ResourceLocation skin = player.getSkin().texture(); //codex (old code snippet) Identifier skin = player.getSkin().body().texturePath();
                        usedSkins.add(skin);
                        BufferedImage face = faces.computeIfAbsent(skin, id -> loadFace(client, id));
                        if (face != null) graphics.drawImage(face, x - 6, y - 6, 12, 12, null);
                        text(graphics, String.format("%.1fm", client.player.position().distanceTo(world)), x, y + 13);
                        if (look.dot(world.subtract(cameraRenderState.pos).normalize()) > 0.995) {
                            text(graphics, player.getScoreboardName(), x, y - 13);
                            text(graphics, String.format("%.0f, %.0f, %.0f", world.x, world.y, world.z), x, y - 24);
                        }
                    }
                }
                if (config.isTeammateTargetCrosshairMarkerEnabled
                        && client.hitResult instanceof EntityHitResult hit
                        && hit.getEntity() instanceof Player target
                        && (config.nameplateUuids.get(target.getUUID()) == Config.NameplateTeam.ALLY || config.nameplateUuids.get(target.getUUID()) == Config.NameplateTeam.FRIENDLY)) {
                    // codex start
                    hasOverlayContent = true;
                    // codex end
                    drawTargetedTeammateMarker(
                            graphics,
                            client.getWindow().getGuiScaledWidth() / 2,
                            client.getWindow().getGuiScaledHeight() / 2);
                }
            } finally {
                graphics.dispose();
            }
            faces.keySet().retainAll(usedSkins);
            // codex start
//            if (usedSkins.isEmpty()) hide(); else window.present();
            if (!hasOverlayContent) hide();
            else window.present();
            // codex end
        } catch (Exception | LinkageError error) {
            failed = true;
            close();
            LoggerFactory.getLogger("PlayerWaypointOverlay").error("Player waypoints disabled: protected overlay unavailable", error);
        }
    }

    private static BufferedImage loadFace(Minecraft client, ResourceLocation id) { //codex (old code snippet) private static BufferedImage loadFace(Minecraft client, Identifier id) {
        var texture = client.getTextureManager().getTexture(id);
        if (texture instanceof DynamicTexture dynamic && dynamic.getPixels() != null) { //codex (old code snippet) if (texture instanceof DynamicTexture dynamic && dynamic.getPixels() != null && !dynamic.getPixels().isClosed()) {
            return face(dynamic.getPixels());
        }
        // Built-in/default skins come from resources rather than downloaded DynamicTextures.
        try (var stream = client.getResourceManager().open(id); var image = NativeImage.read(stream)) {
            return face(image);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void drawTargetedTeammateMarker(Graphics2D graphics, int x, int y) {
        graphics.setColor(Color.RED);
        graphics.setStroke(new BasicStroke(2.0f));
        int size = 3;
        graphics.drawLine(x - size, y - size, x + size, y + size);
        graphics.drawLine(x - size, y + size, x + size, y - size);
    }

    // codex start
    public static void appendDebugMessage(String message) {
        debugMessages.addLast(new DebugMessage(message, System.nanoTime()));
    }

    private static boolean drawDebugMessages(Graphics2D graphics) {
        long oldestAllowed = System.nanoTime() - DEBUG_MESSAGE_DURATION_NANOS;
        while (debugMessages.peekFirst() instanceof DebugMessage oldest && oldest.createdNanos < oldestAllowed) {
            debugMessages.pollFirst();
        }
        int y = 14;
        for (DebugMessage message : debugMessages) {
            graphics.setColor(Color.BLACK);
            graphics.drawString(message.text, 9, y + 1);
            graphics.setColor(Color.WHITE);
            graphics.drawString(message.text, 8, y);
            y += graphics.getFontMetrics().getHeight();
        }
        return !debugMessages.isEmpty();
    }

    private record DebugMessage(String text, long createdNanos) {
    }
    // codex end

    // codex start
    private static void drawCameraAngleIndicator(Graphics2D graphics, int x, int y, float pitch) {
        graphics.setColor(new Color(0x8000FF00, true));
        graphics.setStroke(new BasicStroke(2.0f));
        int size = 2;
        int offset = 9;
        int direction = pitch < 0.0f ? 1 : -1;
        int baseY = y + direction * offset;
        int tipY = y + direction * (offset + size);
        graphics.drawLine(x - size, baseY, x, tipY);
        graphics.drawLine(x, tipY, x + size, baseY);
    }
    //codex end

    private static BufferedImage face(NativeImage skin) {
        BufferedImage result = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        BufferedImage hat = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < 8; x++) {
                result.setRGB(x, y, skin.getPixel(x + 8, y + 8));
                hat.setRGB(x, y, skin.getPixel(x + 40, y + 8));
            }
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.drawImage(hat, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private static void text(Graphics2D graphics, String text, int x, int y) {
        FontMetrics metrics = graphics.getFontMetrics();
        int left = x - metrics.stringWidth(text) / 2;
        int baseline = y + (metrics.getAscent() - metrics.getDescent()) / 2;
        graphics.setColor(Color.BLACK);
        graphics.drawString(text, left + 1, baseline + 1);
        graphics.setColor(Color.WHITE);
        graphics.drawString(text, left, baseline);
    }

    private void hide() {
        if (window != null) window.hide();
    }

    public void close() {
        if (window != null) {
            window.close();
            window = null;
        }
        faces.clear();
    }
}
