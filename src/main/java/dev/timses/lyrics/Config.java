package dev.timses.lyrics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Yalnızca bu bilgisayara ait ayarlar (config/timlyrics.json). Ortak ayarlar botta tutulur. */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("timlyrics.json");

    public String baseUrl = "";
    public String token = "";
    public boolean enabled = true;
    /** Açıksa sözler yalnızca asıldığı sunucu adresinde görünür. Essential/LAN'da kapalı kalmalı. */
    public boolean checkWorld = false;
    public int localOffsetMs = 0;
    public int maxDistance = 96;

    public static Config load() {
        try {
            if (Files.exists(FILE)) {
                Config c = GSON.fromJson(Files.readString(FILE), Config.class);
                if (c != null) return c;
            }
        } catch (Exception ignored) { }
        return new Config();
    }

    public void save() {
        try {
            Files.writeString(FILE, GSON.toJson(this));
        } catch (Exception ignored) { }
    }
}
