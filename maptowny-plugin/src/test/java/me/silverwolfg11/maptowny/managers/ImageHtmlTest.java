package me.silverwolfg11.maptowny.managers;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ImageHtmlTest {
    private final List<String> allowed = List.of("cdn.discordapp.com", "media.discordapp.net", "i.imgur.com");

    @Test void acceptsHttpsImagesOnAllowedDomains() {
        assertNull(ImageHtml.check("https://i.imgur.com/abc123.png", allowed));
        assertNull(ImageHtml.check("https://cdn.discordapp.com/attachments/1/2/flag.JPG?ex=abc&is=def&hm=ghi", allowed));   // 画像の置き場の署名つきURL（拡張子は大文字小文字を問わない）
        assertNull(ImageHtml.check("https://media.discordapp.net/attachments/1/2/flag.gif", allowed));
        assertNull(ImageHtml.check("https://sub.i.imgur.com/a.gif", allowed));   // 許可したドメインの下のサブドメイン
    }

    @Test void rejectsEverythingElseWithTheReason() {
        assertEquals(ImageHtml.Problem.EMPTY, ImageHtml.check("", allowed));
        assertEquals(ImageHtml.Problem.EMPTY, ImageHtml.check("   ", allowed));
        assertEquals(ImageHtml.Problem.NOT_HTTPS, ImageHtml.check("http://i.imgur.com/a.png", allowed));
        assertEquals(ImageHtml.Problem.NOT_HTTPS, ImageHtml.check("javascript:alert(1)", allowed));
        assertEquals(ImageHtml.Problem.NOT_HTTPS, ImageHtml.check("data:image/png;base64,AAAA", allowed));
        assertEquals(ImageHtml.Problem.BAD_DOMAIN, ImageHtml.check("https://evil.example.com/a.png", allowed));
        assertEquals(ImageHtml.Problem.BAD_DOMAIN, ImageHtml.check("https://i.imgur.com.evil.com/a.png", allowed));   // 末尾だけ似せたドメイン
        assertEquals(ImageHtml.Problem.BAD_DOMAIN, ImageHtml.check("https://notimgur.com/a.png", allowed));
        assertEquals(ImageHtml.Problem.BAD_DOMAIN, ImageHtml.check("https://user:pass@i.imgur.com/a.png", allowed));   // ユーザー情報つきは使わない
        assertEquals(ImageHtml.Problem.BAD_DOMAIN, ImageHtml.check("https://i.imgur.com:8443/a.png", allowed));        // 標準以外のポート
        assertEquals(ImageHtml.Problem.BAD_EXTENSION, ImageHtml.check("https://i.imgur.com/a.html", allowed));
        assertEquals(ImageHtml.Problem.BAD_EXTENSION, ImageHtml.check("https://i.imgur.com/a.webp", allowed));   // 大きさを調べられない形式は使わない
        assertEquals(ImageHtml.Problem.BAD_EXTENSION, ImageHtml.check("https://i.imgur.com/a", allowed));
        assertEquals(ImageHtml.Problem.BAD_EXTENSION, ImageHtml.check("https://i.imgur.com/a.png.exe", allowed));
        assertEquals(ImageHtml.Problem.MALFORMED, ImageHtml.check("https://i.imgur.com/a b.png", allowed));
        assertEquals(ImageHtml.Problem.MALFORMED, ImageHtml.check("https://i.imgur.com/a\".png", allowed));
        assertEquals(ImageHtml.Problem.MALFORMED, ImageHtml.check("https://i.imgur.com/a'.png", allowed));
        assertEquals(ImageHtml.Problem.MALFORMED, ImageHtml.check("https://i.imgur.com/<a>.png", allowed));
        assertEquals(ImageHtml.Problem.TOO_LONG, ImageHtml.check("https://i.imgur.com/" + "a".repeat(400) + ".png", allowed));
        assertEquals(ImageHtml.Problem.BAD_DOMAIN, ImageHtml.check("https://i.imgur.com/a.png", List.of()));   // 許可するドメインが無ければ、すべて拒否
    }

    @Test void buildsAnImageTagWithFixedSizeAndNoReferrerAndHidesBrokenImages() {
        String tag = ImageHtml.img("https://i.imgur.com/a.png?x=1&y=2", 48);
        assertEquals("<img src=\"https://i.imgur.com/a.png?x=1&amp;y=2\" width=\"48\" height=\"48\" referrerpolicy=\"no-referrer\" onerror=\"this.style.display='none'\" style=\"vertical-align:middle;image-rendering:pixelated;\"/>", tag);
        assertEquals("", ImageHtml.img("", 48));
        assertEquals("", ImageHtml.img(null, 48));
    }

    @Test void flagsAreSizedByWidthOnlySoARectangularFlagKeepsItsShape() {
        String tag = ImageHtml.imgByWidth("https://i.imgur.com/a.png", 96);
        assertTrue(tag.contains("width=\"96\""));
        assertFalse(tag.contains("height="));   // 縦を固定しない
        assertEquals("", ImageHtml.imgByWidth("", 96));
        assertEquals("", ImageHtml.imgByWidth(null, 96));
    }

    @Test void aFlagBoxFramesTheImageWithALabel() {
        String box = ImageHtml.flagBox("🏘️ 町の旗", "<img src=\"x\"/>", "/town set flag <URL>", 160);
        assertTrue(box.contains("🏘️ 町の旗"));
        assertFalse(box.contains("inline-block"));   // 横に並べず、1つずつ縦に積む
        assertTrue(box.contains("<img src=\"x\"/>"));
        assertFalse(box.contains("未設定"));   // 画像があれば、案内は出さない
    }

    @Test void aFlagBoxWithoutAnImageShowsTheHintEscaped() {
        String box = ImageHtml.flagBox("🏘️ 町の旗", "", "/town set flag <URL>", 160);
        assertTrue(box.contains("border:1px dashed"));
        assertFalse(box.contains("inline-block"));
        assertTrue(box.contains("旗が未設定"));
        assertTrue(box.contains("/town set flag &lt;URL&gt;"));
        assertFalse(box.contains("<URL>"));
        assertTrue(box.contains("width:160px"));
        assertTrue(box.contains("height:107px"));   // 旗は、横3:縦2の長方形の枠（正方形にしない）
    }

    @Test void flagImagesGetTheirOwnThinBorderSoWhiteFlagsKeepAVisibleEdge() {
        assertTrue(ImageHtml.imgByWidth("https://i.imgur.com/a.png", 160).contains("border:1px solid"));
    }

    @Test void imgurPageUrlsAreConvertedToTheDirectImageUrl() {
        assertEquals("https://i.imgur.com/rDz3wAW.png", ImageHtml.normalize("https://imgur.com/rDz3wAW"));
        assertEquals("https://i.imgur.com/vE9pKPJ.png", ImageHtml.normalize("https://imgur.com/vE9pKPJ/"));
        assertEquals("https://i.imgur.com/rDz3wAW.png", ImageHtml.normalize("https://www.imgur.com/rDz3wAW"));
        assertEquals("https://i.imgur.com/rDz3wAW.png", ImageHtml.normalize("  https://imgur.com/rDz3wAW  "));   // 前後の空白は取る
        assertNull(ImageHtml.check(ImageHtml.normalize("https://imgur.com/rDz3wAW"), allowed));   // 変換したものは、検証を通る
    }

    @Test void otherUrlsAreLeftAsTheyAre() {
        assertEquals("https://i.imgur.com/abc123.png", ImageHtml.normalize("https://i.imgur.com/abc123.png"));
        assertEquals("https://imgur.com/a/albumId", ImageHtml.normalize("https://imgur.com/a/albumId"));         // アルバムは、画像1つに決められない
        assertEquals("https://imgur.com/gallery/abc123", ImageHtml.normalize("https://imgur.com/gallery/abc123"));
        assertEquals("https://example.com/rDz3wAW", ImageHtml.normalize("https://example.com/rDz3wAW"));
        assertEquals("http://imgur.com/rDz3wAW", ImageHtml.normalize("http://imgur.com/rDz3wAW"));               // https だけ
        assertEquals("", ImageHtml.normalize(""));
        assertNull(ImageHtml.normalize(null));
    }

    @Test void escapesAttributeValues() {
        String tag = ImageHtml.img("https://i.imgur.com/a.png?q=\"><script>", 32);
        assertFalse(tag.contains("<script>"));
        assertTrue(tag.contains("&quot;&gt;&lt;script&gt;"));
    }

    @Test void buildsTheFaceUrlFromTheTemplate() {
        UUID id = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
        assertEquals("https://mc-heads.net/avatar/069a79f444e94726a5befca90e38aaf5/32", ImageHtml.faceUrl("https://mc-heads.net/avatar/%uuid%/32", id));
        assertEquals("https://x.example/069a79f4-44e9-4726-a5be-fca90e38aaf5.png", ImageHtml.faceUrl("https://x.example/%uuid_dashed%.png", id));
        assertEquals("", ImageHtml.faceUrl("https://mc-heads.net/avatar/%uuid%/32", null));   // UUID が分からなければ空
        assertEquals("", ImageHtml.faceUrl("", id));                                           // テンプレートが空なら、顔を出さない
    }
}
