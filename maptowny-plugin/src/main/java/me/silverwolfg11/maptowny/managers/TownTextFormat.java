package me.silverwolfg11.maptowny.managers;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.stream.Collectors;

// 町の情報を、日本語と絵文字で見やすく表示するための整形。Towny に依存しないので、サーバーなしで試せる。
public final class TownTextFormat {
    private TownTextFormat() { }

    private static final DateTimeFormatter FOUNDED = DateTimeFormatter.ofPattern("yyyy年M月d日").withZone(ZoneId.of("Asia/Tokyo"));

    // プレイヤーが書ける文字（掲示板・名前など）を、HTMLに入れる前に無害にする（< > & " ' を置き換える）。nullは空。
    public static String html(String text) { return text == null ? "" : ImageHtml.escape(text); }

    // 有効・無効を、⭕と❌で表す。
    public static String mark(boolean enabled) { return enabled ? "⭕" : "❌"; }

    // 名前の一覧を「、」で並べる。空なら「なし」。
    public static String list(Collection<String> names) {
        return names.isEmpty() ? "なし" : names.stream().map(TownTextFormat::html).collect(Collectors.joining("、"));
    }

    // 設立日。0 は未設定。日本時間で「2026年10月17日」の形。
    public static String founded(long millis) {
        return millis == 0 ? "未設定" : FOUNDED.format(Instant.ofEpochMilli(millis));
    }

    // 町の役職の表示名。知らない役職は、そのままの名前。
    public static String rankName(String rank) {
        switch (rank) {
            case "assistant": return "補佐";
            case "councillor": return "議員";
            case "sheriff": return "保安官";
            case "donator": return "支援者";
            case "trusted": return "信頼された人";
            case "builder": return "建築家";
            default: return rank;
        }
    }

    // 役職ごとの人を、HTMLの行にする（人がいない役職は出さない）。誰もいなければ、その旨。
    public static String rankList(java.util.Map<String, java.util.List<String>> ranks) {
        StringBuilder html = new StringBuilder();
        for (java.util.Map.Entry<String, java.util.List<String>> entry : ranks.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            html.append("<div>🛡️ ").append(html(rankName(entry.getKey()))).append(": <b>").append(list(entry.getValue())).append("</b></div>");
        }
        return html.length() == 0 ? "<div>役職についている人はいません</div>" : html.toString();
    }

    // 国の中での立場。国に入っていなければ空。
    public static String nationStatus(boolean hasNation, boolean capital, String nationName) {
        if (!hasNation) return "";
        return capital ? "🏰 " + nationName + "の首都" : "🛡️ " + nationName + "に所属";
    }
}
