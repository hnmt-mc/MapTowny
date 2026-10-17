package me.silverwolfg11.maptowny.managers;

import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.object.metadata.StringDataField;

// 町・国の旗の画像のURLを、Towny の町・国のメタデータに保存する（データベースの追加は要らない）。
public final class FlagStore {
    private FlagStore() { }

    public static final String KEY = "maptowny_flag_url";

    // 設定した旗のURL。なければ null。
    public static String get(TownyObject object) {
        if (object == null || !object.hasMeta(KEY)) return null;
        StringDataField field = object.getMetadata(KEY, StringDataField.class);
        return field == null || field.getValue() == null || field.getValue().isBlank() ? null : field.getValue();
    }

    public static void set(TownyObject object, String url) {
        clear(object);
        object.addMetaData(new StringDataField(KEY, url));
    }

    // 古い Towny（0.97 系）にも合わせて、フィールドを取ってから外す。
    public static void clear(TownyObject object) {
        CustomDataField<?> field = object.getMetadata(KEY);
        if (field != null) object.removeMetaData(field);
    }
}
