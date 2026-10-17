package me.silverwolfg11.maptowny.managers;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// image-settings.yml（旗の画像のドメイン・大きさ、町長の顔のURL）。ファイルがなければ、初期のものを書き出す。読めない値は、初期値にする。
public final class ImageSettings {
    public static final List<String> DEFAULT_DOMAINS = Collections.unmodifiableList(Arrays.asList("i.imgur.com", "i.gyazo.com", "i.ibb.co", "i.postimg.cc"));
    public static final String DEFAULT_FACE_URL = "https://mc-heads.net/avatar/%uuid%/64";

    private final List<String> allowedDomains;
    private final int townFlagSize, nationFlagSize, faceSize, requiredWidth, requiredHeight;
    private final String faceUrl;

    public ImageSettings(List<String> allowedDomains, int townFlagSize, int nationFlagSize, String faceUrl, int faceSize, int requiredWidth, int requiredHeight) {
        this.requiredWidth = requiredWidth; this.requiredHeight = requiredHeight;
        this.allowedDomains = Collections.unmodifiableList(new java.util.ArrayList<>(allowedDomains)); this.townFlagSize = townFlagSize; this.nationFlagSize = nationFlagSize; this.faceUrl = faceUrl; this.faceSize = faceSize;
    }

    public List<String> allowedDomains() { return allowedDomains; }
    public int townFlagSize() { return townFlagSize; }
    public int nationFlagSize() { return nationFlagSize; }
    public String faceUrl() { return faceUrl; }
    public int faceSize() { return faceSize; }
    // 旗の画像に必要な大きさ（ピクセル）。0なら、制限しない。
    public int requiredWidth() { return requiredWidth; }
    public int requiredHeight() { return requiredHeight; }

    public static ImageSettings load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "image-settings.yml");
        if (!file.exists()) plugin.saveResource("image-settings.yml", false);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<String> domains = yaml.contains("allowed-flag-domains") ? yaml.getStringList("allowed-flag-domains") : DEFAULT_DOMAINS;
        return new ImageSettings(domains, size(yaml.getInt("town-flag-size-pixels", 240)), size(yaml.getInt("nation-flag-size-pixels", 240)), yaml.getString("mayor-face-url", DEFAULT_FACE_URL),
                size(yaml.getInt("mayor-face-size-pixels", 64)),
                Math.max(0, Math.min(4096, yaml.getInt("flag-required-width", 330))), Math.max(0, Math.min(4096, yaml.getInt("flag-required-height", 220))));
    }

    // 画像の大きさは 8〜512 ピクセル。
    static int size(int value) { return Math.max(8, Math.min(512, value)); }
}
