package me.silverwolfg11.maptowny.objects;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

// 地図のアイコンの置き場所を決める。URL（https:// など）はそのまま使い、
// それ以外は「プラグインのフォルダの中のファイル」（例: icons/king.png）として扱う。フォルダの外は指せない。
public final class IconSource {
    private IconSource() { }

    public static URL resolve(String value, File dataFolder) throws MalformedURLException {
        if (value == null || value.trim().isEmpty()) throw new MalformedURLException("Icon is empty");
        String trimmed = value.trim();
        if (trimmed.contains("://") || trimmed.startsWith("file:")) return new URL(trimmed);
        File file = new File(dataFolder, trimmed);
        try {
            String root = dataFolder.getCanonicalPath() + File.separator;
            if (new File(trimmed).isAbsolute() || !file.getCanonicalPath().startsWith(root)) throw new MalformedURLException("Icon must be inside the plugin folder");
        } catch (IOException error) {
            throw new MalformedURLException("Icon path cannot be resolved");
        }
        return file.toURI().toURL();
    }
}
