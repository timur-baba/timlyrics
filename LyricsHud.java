package dev.timses.lyrics;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Sözleri dünyadaki sabit bir noktaya "asılı" çizer. Nokta herkeste aynı dünya koordinatıdır;
 * her oyuncu kendi kamerasından ekrana izdüşürür, yazı Minecraft yazı tipiyle çizilir.
 */
public final class LyricsHud {
    private static final int WRAP_PX = 200;
    private static final float WORLD_PER_PX = 0.045f;

    public static String dimensionKey(Minecraft mc) {
        return mc.level == null ? "" : mc.level.dimension().toString();
    }

    public static String worldKey(Minecraft mc) {
        return mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "singleplayer";
    }

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Config cfg = TimLyricsClient.config;
        Snap snap = Sync.snap;
        Snap.Anchor a = snap.anchor;
        if (!cfg.enabled || a == null || !snap.linked || mc.player == null || mc.level == null || mc.options.hideGui) return;
        if (snap.trackId.isEmpty()) return;
        if (!a.dimension.equals(dimensionKey(mc))) return;
        if (cfg.checkWorld && !a.world.equals(worldKey(mc))) return;

        float partial = delta.getGameTimeDeltaPartialTick(false);
        Player p = mc.player;
        Vec3 eye = p.getEyePosition(partial);
        double yaw = Math.toRadians(p.getViewYRot(partial));
        double pitch = Math.toRadians(p.getViewXRot(partial));
        Vec3 fwd = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 up = right.cross(fwd);

        int w = g.guiWidth(), h = g.guiHeight();
        double fov = Math.toRadians(mc.options.fov().get());
        double focal = (h / 2.0) / Math.tan(fov / 2.0);
        Font font = mc.font;

        long now = System.currentTimeMillis();
        long pos = snap.positionAt(now) + a.offsetMs + cfg.localOffsetMs;
        int n = Math.max(1, Math.min(7, a.lines));
        double lineH = 11 * WORLD_PER_PX * a.scale * 1.35;

        if (snap.times.length == 0) {
            String label = "ready".equals(snap.lyricsStatus) ? "" : ("loading".equals(snap.lyricsStatus) ? "♪ Sözler aranıyor…" : "♪ " + snap.title);
            if (!label.isEmpty()) drawLine(g, font, label, a.x, a.y, a.z, eye, fwd, right, up, w, h, focal, a.scale, 1f, 0xFFB3B3B3, cfg);
            return;
        }

        int idx = snap.lineIndexAt(pos);
        double slide = 1.0;
        if (idx >= 0) slide = Math.min(1.0, (pos - snap.times[idx]) / 350.0);
        slide = 1 - Math.pow(1 - slide, 3);
        int before = (n - 1) / 2;
        for (int k = 0; k < n; k++) {
            int i = idx - before + k;
            if (i < 0 || i >= snap.texts.length) continue;
            double yOff = ((n - 1) / 2.0 - k) * lineH - (1 - slide) * lineH;
            boolean current = i == idx;
            int color = current ? 0xFFFFFFFF : 0xFFA8A8A8;
            drawLine(g, font, snap.texts[i], a.x, a.y + yOff, a.z, eye, fwd, right, up, w, h, focal, a.scale, current ? 1.25f : 1f, color, cfg);
        }
    }

    private static void drawLine(GuiGraphics g, Font font, String text, double x, double y, double z, Vec3 eye, Vec3 fwd,
                                 Vec3 right, Vec3 up, int w, int h, double focal, double scale, float emphasis, int color, Config cfg) {
        Vec3 d = new Vec3(x - eye.x, y - eye.y, z - eye.z);
        double zc = d.dot(fwd);
        if (zc < 0.5 || zc > cfg.maxDistance) return;
        double sx = w / 2.0 + d.dot(right) / zc * focal;
        double sy = h / 2.0 - d.dot(up) / zc * focal;
        float s = (float) (WORLD_PER_PX * scale * focal / zc) * emphasis;
        if (s < 0.25f || s > 8f) return;
        if (sx < -400 || sx > w + 400 || sy < -200 || sy > h + 200) return;

        double fade = Math.min(1.0, (cfg.maxDistance - zc) / 16.0);
        int alpha = (int) (((color >>> 24) & 0xFF) * fade);
        int argb = (alpha << 24) | (color & 0xFFFFFF);

        List<FormattedCharSequence> parts = font.split(Component.literal(text), WRAP_PX);
        int total = parts.size() * (font.lineHeight + 1);
        g.pose().pushMatrix();
        g.pose().translate((float) sx, (float) sy);
        g.pose().scale(s, s);
        for (int j = 0; j < parts.size(); j++) {
            FormattedCharSequence part = parts.get(j);
            g.drawString(font, part, -font.width(part) / 2, -total / 2 + j * (font.lineHeight + 1), argb, true);
        }
        g.pose().popMatrix();
    }
}
