package me.silverwolfg11.maptowny.managers;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

// 町・国の旗や町長の顔の「画像のURLの検証」と「<img> の組み立て」。Towny に依存しないので、サーバーなしで試せる。
// 外部の画像をそのまま表示すると、不適切な画像や、見た人の情報を取る画像を貼られるので、許可したドメインの https の画像だけを通す。
public final class ImageHtml {
    private ImageHtml() { }

    public enum Problem { EMPTY, TOO_LONG, NOT_HTTPS, BAD_DOMAIN, BAD_EXTENSION, MALFORMED }

    public static final int MAX_LENGTH = 300;
    private static final List<String> EXTENSIONS = Arrays.asList("png", "jpg", "jpeg", "gif");

    private static final java.util.regex.Pattern IMGUR_PAGE = java.util.regex.Pattern.compile("^https://(?:www\\.)?imgur\\.com/([A-Za-z0-9]{5,8})/?$");

    // 貼られたURLを、使える形に直す。imgur の画像のページ（https://imgur.com/ID）は、画像そのもののURL（https://i.imgur.com/ID.png）にする。
    // imgur は、拡張子が実際の形式と違っても、画像そのものを返す。アルバムやギャラリーは、画像を1つに決められないので、そのまま（検証で断られる）。
    public static String normalize(String url) {
        if (url == null) return null;
        String trimmed = url.trim();
        java.util.regex.Matcher matcher = IMGUR_PAGE.matcher(trimmed);
        return matcher.matches() ? "https://i.imgur.com/" + matcher.group(1) + ".png" : trimmed;
    }

    // 問題がなければ null、あればその理由。
    public static Problem check(String url, Collection<String> allowedDomains) {
        if (url == null || url.isBlank()) return Problem.EMPTY;
        if (url.length() > MAX_LENGTH) return Problem.TOO_LONG;
        if (!url.regionMatches(true, 0, "https://", 0, 8)) return Problem.NOT_HTTPS;
        for (char c : url.toCharArray())
            if (Character.isWhitespace(c) || Character.isISOControl(c) || c == '"' || c == '\'' || c == '<' || c == '>' || c == '\\') return Problem.MALFORMED;
        URI uri;
        try { uri = new URI(url); } catch (URISyntaxException error) { return Problem.MALFORMED; }
        String host = uri.getHost();
        if (host == null) return Problem.MALFORMED;
        host = host.toLowerCase(Locale.ROOT);
        if (uri.getUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)) return Problem.BAD_DOMAIN;
        boolean allowed = false;
        for (String domain : allowedDomains) {
            String d = domain.toLowerCase(Locale.ROOT).trim();
            if (!d.isEmpty() && (host.equals(d) || host.endsWith("." + d))) { allowed = true; break; }
        }
        if (!allowed) return Problem.BAD_DOMAIN;
        String path = uri.getPath() == null ? "" : uri.getPath();
        String name = path.substring(path.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        if (dot < 0 || !EXTENSIONS.contains(name.substring(dot + 1))) return Problem.BAD_EXTENSION;
        return null;
    }

    // 固定の大きさの <img>。読めなければ非表示、参照元の情報は送らない。URLが空なら空文字。
    public static String img(String url, int size) {
        if (url == null || url.isEmpty()) return "";
        return "<img src=\"" + escape(url) + "\" width=\"" + size + "\" height=\"" + size + "\" referrerpolicy=\"no-referrer\""
                + " onerror=\"this.style.display='none'\" style=\"vertical-align:middle;image-rendering:pixelated;\"/>";
    }

    // 旗など、縦横の比が決まっていない画像。幅だけを指定して、縦は画像の比のまま（長方形の旗が、ゆがまない）。
    public static String imgByWidth(String url, int width) {
        if (url == null || url.isEmpty()) return "";
        return "<img src=\"" + escape(url) + "\" width=\"" + width + "\" referrerpolicy=\"no-referrer\""
                + " onerror=\"this.style.display='none'\" style=\"vertical-align:middle;border:1px solid #999;max-width:100%;height:auto;\"/>";
    }

    // 旗を枠で囲んだ箱。画像がなければ、枠の中に「旗が未設定」と設定のしかたを出す。
    // 未設定の枠は、旗の標準の比（横3:縦2）の長方形にする。width は旗の幅（ピクセル）。
    public static String flagBox(String label, String imgHtml, String hint, int width) {
        String body = imgHtml == null || imgHtml.isEmpty()
                ? "<div style=\"display:flex;flex-direction:column;align-items:center;justify-content:center;box-sizing:border-box;border:1px dashed #aaa;color:#999;"
                        + "width:" + width + "px;height:" + Math.round(width * 2 / 3.0) + "px;\">🏳️ 旗が未設定<br/><small>" + escape(hint) + "</small></div>"
                : imgHtml;
        // 1つずつ縦に積む（町の旗の下に、国の旗）。画像は中央、細い枠つき。
        return "<div style=\"margin:8px 0;text-align:center;\">"
                + "<div style=\"font-size:85%;color:#666;margin-bottom:3px;text-align:left;\">" + label + "</div>" + body + "</div>";
    }

    // 町長の顔のURL。%uuid%（ハイフンなし）と %uuid_dashed%（ハイフンあり）を置き換える。UUID が無い・テンプレートが空なら空文字。
    public static String faceUrl(String template, UUID uuid) {
        if (template == null || template.isBlank() || uuid == null) return "";
        return template.replace("%uuid_dashed%", uuid.toString()).replace("%uuid%", uuid.toString().replace("-", ""));
    }

    public static String escape(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "&#39;");
    }
}
