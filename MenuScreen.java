package dev.timses.lyrics;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/** Kontrol menüsü: bağlantı, asma noktası ve boyut/satır/zaman ayarları. */
public final class MenuScreen extends Screen {
    private EditBox urlBox;
    private Button toggleButton;
    private String message = "";

    public MenuScreen() {
        super(Component.literal("TimLyrics"));
    }

    @Override
    protected void init() {
        Config cfg = TimLyricsClient.config;
        int cx = width / 2, left = cx - 130, top = Math.max(8, height / 2 - 112);

        urlBox = new EditBox(font, left, top, 196, 20, Component.literal("Bot adresi"));
        urlBox.setMaxLength(200);
        urlBox.setValue(cfg.baseUrl);
        urlBox.setHint(Component.literal("https://…trycloudflare.com"));
        addRenderableWidget(urlBox);
        addRenderableWidget(Button.builder(Component.literal("Kaydet"), b -> {
            String v = urlBox.getValue().trim();
            if (!v.isEmpty() && !Api.validUrl(v)) { message = "Adres https:// ile başlamalı."; return; }
            if (!v.equals(cfg.baseUrl)) { cfg.token = ""; Sync.snap = Snap.EMPTY; Sync.pairCode = ""; }
            cfg.baseUrl = v; cfg.save(); message = "Adres kaydedildi.";
        }).bounds(left + 200, top, 60, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Discord ile eşleştir"), b -> pair())
                .bounds(left, top + 26, 260, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Sözleri buraya as"), b -> hangHere())
                .bounds(left, top + 78, 160, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Kaldır"), b -> mutate(o -> o.addProperty("action", "clear"), true))
                .bounds(left + 164, top + 78, 96, 20).build());

        stepper(left, top + 104, "Boyut", -1, o -> adjust(o, "scale", -0.2, 0.2, 8));
        stepper(left, top + 104, "Boyut", 1, o -> adjust(o, "scale", 0.2, 0.2, 8));
        stepper(left, top + 128, "Satır", -1, o -> adjust(o, "lines", -1, 1, 7));
        stepper(left, top + 128, "Satır", 1, o -> adjust(o, "lines", 1, 1, 7));
        stepper(left, top + 152, "Zaman", -1, o -> adjust(o, "offsetMs", -100, -5000, 5000));
        stepper(left, top + 152, "Zaman", 1, o -> adjust(o, "offsetMs", 100, -5000, 5000));

        toggleButton = Button.builder(label(cfg), b -> {
            cfg.enabled = !cfg.enabled; cfg.save(); b.setMessage(label(cfg));
        }).bounds(left, top + 180, 126, 20).build();
        addRenderableWidget(toggleButton);
        addRenderableWidget(Button.builder(Component.literal("Kapat"), b -> onClose())
                .bounds(left + 134, top + 180, 126, 20).build());
    }

    private static Component label(Config c) {
        return Component.literal("Sözler: " + (c.enabled ? "Açık" : "Kapalı"));
    }

    private void stepper(int left, int y, String name, int dir, Consumer<JsonObject> change) {
        int x = dir < 0 ? left : left + 200;
        addRenderableWidget(Button.builder(Component.literal(dir < 0 ? "−" : "+"), b -> mutate(change, false))
                .bounds(x, y, 60, 20).build());
    }

    private static void adjust(JsonObject o, String key, double step, double min, double max) {
        Snap.Anchor a = Sync.snap.anchor;
        double cur = a == null ? 0 : key.equals("scale") ? a.scale : key.equals("lines") ? a.lines : a.offsetMs;
        double v = Math.max(min, Math.min(max, cur + step));
        if (key.equals("scale")) v = Math.round(v * 10) / 10.0;
        o.addProperty(key, v);
    }

    private void pair() {
        Config cfg = TimLyricsClient.config;
        if (!Api.validUrl(cfg.baseUrl)) { message = "Önce bot adresini yazıp Kaydet'e bas."; return; }
        message = "Kod isteniyor…";
        Api.pair(cfg).whenComplete((o, err) -> Minecraft.getInstance().execute(() -> {
            if (err != null) { message = "Bağlanılamadı. Adresi kontrol et."; return; }
            cfg.token = o.get("token").getAsString();
            cfg.save();
            String prefix = o.has("prefix") ? o.get("prefix").getAsString() : "!";
            Sync.pairCode = prefix + "panel " + o.get("code").getAsString();
            Minecraft.getInstance().keyboardHandler.setClipboard(Sync.pairCode);
            message = "Komut panoya kopyalandı. Ses kanalındayken Discord'a yapıştır.";
        }));
    }

    private void hangHere() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) { message = "Önce bir dünyaya gir."; return; }
        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle();
        Vec3 spot = eye.add(look.scale(4));
        mutate(o -> {
            o.addProperty("x", spot.x);
            o.addProperty("y", spot.y);
            o.addProperty("z", spot.z);
            o.addProperty("dimension", LyricsHud.dimensionKey(mc));
            o.addProperty("world", LyricsHud.worldKey(mc));
        }, true);
    }

    /** Mevcut asma noktasını koruyup yalnızca değişeni botta günceller; herkes yeni değeri görür. */
    private void mutate(Consumer<JsonObject> change, boolean standalone) {
        Config cfg = TimLyricsClient.config;
        if (cfg.token.isBlank() || !Sync.snap.linked) { message = "Önce Discord ile eşleştir."; return; }
        JsonObject body = new JsonObject();
        Snap.Anchor a = Sync.snap.anchor;
        if (!standalone) {
            if (a == null) { message = "Önce sözleri bir yere as."; return; }
            body.addProperty("x", a.x); body.addProperty("y", a.y); body.addProperty("z", a.z);
            body.addProperty("dimension", a.dimension); body.addProperty("world", a.world);
        }
        change.accept(body);
        Api.anchor(cfg, body).whenComplete((o, err) -> Minecraft.getInstance().execute(() -> {
            if (err != null) {
                Throwable t = err.getCause() != null ? err.getCause() : err;
                String m = String.valueOf(t.getMessage());
                message = m.contains("|") ? m.substring(m.indexOf('|') + 1) : "İşlem başarısız.";
                return;
            }
            Snap prev = Sync.snap;
            Snap.Anchor next = o.has("anchor") ? Sync.parseAnchor(o.get("anchor")) : null;
            Sync.snap = new Snap(prev.linked, prev.status, prev.playing, prev.positionMs, prev.baseMillis, prev.trackId,
                    prev.title, prev.artist, prev.lyricsStatus, prev.times, prev.texts, next, prev.lyricsTrackId);
            message = next == null ? "Sözler kaldırıldı." : "Güncellendi — herkeste aynı yerde.";
        }));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        Snap s = Sync.snap;
        int cx = width / 2, left = cx - 130, top = Math.max(8, height / 2 - 112);
        g.drawCenteredString(font, Component.literal("TimLyrics"), cx, top - 10, 0xFFFFFFFF);

        String conn = TimLyricsClient.config.token.isBlank() ? "Eşleşmedi"
                : s.linked ? (Sync.connected() ? "Bağlı" : "Bağlantı yok") : "Kod bekleniyor";
        g.drawString(font, Component.literal("Durum: " + conn), left, top + 52, 0xFFB8FF9A, false);
        if (!Sync.pairCode.isEmpty() && !s.linked) g.drawString(font, Component.literal(Sync.pairCode), left, top + 64, 0xFFFFE08A, false);
        else if (s.linked) {
            String track = s.title.isEmpty() ? "Şarkı çalmıyor" : s.title;
            g.drawString(font, Component.literal(font.plainSubstrByWidth(track, 190)), left + 62, top + 52, 0xFFFFFFFF, false);
            String lyr = "ready".equals(s.lyricsStatus) ? "senkron söz hazır" : "loading".equals(s.lyricsStatus) ? "sözler aranıyor" : "söz yok";
            String who = s.anchor == null ? "asılı değil" : "asan: " + s.anchor.setBy;
            g.drawString(font, Component.literal(lyr + " · " + who), left, top + 64, 0xFFB3B3B3, false);
        }
        Snap.Anchor a = s.anchor;
        g.drawCenteredString(font, Component.literal(a == null ? "—" : String.format("%.1f", a.scale) + "×"), cx, top + 110, 0xFFFFFFFF);
        g.drawCenteredString(font, Component.literal(a == null ? "—" : a.lines + " satır"), cx, top + 134, 0xFFFFFFFF);
        g.drawCenteredString(font, Component.literal(a == null ? "—" : (a.offsetMs >= 0 ? "+" : "") + a.offsetMs + " ms"), cx, top + 158, 0xFFFFFFFF);
        String hint = !message.isEmpty() ? message : Sync.error;
        if (!hint.isEmpty()) g.drawCenteredString(font, Component.literal(font.plainSubstrByWidth(hint, 300)), cx, top + 206, 0xFFFFC9C9);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
