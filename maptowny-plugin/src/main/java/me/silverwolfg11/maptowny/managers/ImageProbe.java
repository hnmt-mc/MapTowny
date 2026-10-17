package me.silverwolfg11.maptowny.managers;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

// 旗の画像の「ほんとうの大きさ」を調べる。指定した大きさと違う画像は、設定できないようにするため。
// 画像のURLは、許可したサイトの https のものだけが、ここに来る（ImageHtml.check のあと）。
public final class ImageProbe {
    private ImageProbe() { }

    public enum SizeProblem { WRONG_SIZE }

    public static final int MAX_BYTES = 3 * 1024 * 1024;   // 大きすぎる画像は、読まずに断る
    private static final int TIMEOUT_MILLIS = 5000;

    // 画像の横・縦（ピクセル）。画像として読めなければ null。画像全体をデコードせず、ヘッダーだけを読む。
    public static int[] dimensions(byte[] data) {
        if (data == null || data.length == 0) return null;
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            if (input == null) return null;
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return null;
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                return new int[]{reader.getWidth(0), reader.getHeight(0)};
            } finally { reader.dispose(); }
        } catch (IOException | RuntimeException error) { return null; }
    }

    // 必要な大きさ（requiredWidth × requiredHeight）と同じか。どちらかが0なら、制限しない。
    public static SizeProblem checkSize(int width, int height, int requiredWidth, int requiredHeight) {
        if (requiredWidth <= 0 || requiredHeight <= 0) return null;
        return width == requiredWidth && height == requiredHeight ? null : SizeProblem.WRONG_SIZE;
    }

    public static String explain(int width, int height, int requiredWidth, int requiredHeight) {
        return "画像の大きさが違います（必要: " + requiredWidth + "×" + requiredHeight + "、この画像: " + width + "×" + height + "）";
    }

    // URLの画像を、上限つきで読む。転送先への移動（リダイレクト）はしない（許可していないサイトへ行かせないため）。
    // 読めない・画像でない・大きすぎる・時間切れのときは null。ネットワークを使うので、メインスレッドでは呼ばない。
    public static byte[] fetch(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setRequestProperty("User-Agent", "MapTowny-FlagCheck");
            if (connection.getResponseCode() != 200) return null;
            String type = connection.getContentType();
            if (type == null || !type.toLowerCase(java.util.Locale.ROOT).startsWith("image/")) return null;
            if (connection.getContentLengthLong() > MAX_BYTES) return null;
            try (InputStream in = connection.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    if (out.size() > MAX_BYTES) return null;
                }
                return out.toByteArray();
            }
        } catch (IOException | RuntimeException error) { return null; }
        finally { if (connection != null) connection.disconnect(); }
    }
}
