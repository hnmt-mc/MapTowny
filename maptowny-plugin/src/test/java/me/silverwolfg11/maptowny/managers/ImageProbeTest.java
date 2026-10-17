package me.silverwolfg11.maptowny.managers;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class ImageProbeTest {
    private static byte[] image(int width, int height, String format) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), format, out);
        return out.toByteArray();
    }

    @Test void readsTheDimensionsOfPngAndJpegWithoutNeedingToDecodeTheWholeImage() throws Exception {
        assertArrayEquals(new int[]{300, 200}, ImageProbe.dimensions(image(300, 200, "png")));
        assertArrayEquals(new int[]{640, 480}, ImageProbe.dimensions(image(640, 480, "jpg")));
        assertArrayEquals(new int[]{30, 20}, ImageProbe.dimensions(image(30, 20, "gif")));
    }

    @Test void returnsNullForDataThatIsNotAnImage() {
        assertNull(ImageProbe.dimensions("<html>not an image</html>".getBytes()));
        assertNull(ImageProbe.dimensions(new byte[0]));
        assertNull(ImageProbe.dimensions(null));
    }

    @Test void theRequiredSizeMustMatchExactlyAndZeroDisablesTheCheck() {
        assertNull(ImageProbe.checkSize(300, 200, 300, 200));
        assertEquals(ImageProbe.SizeProblem.WRONG_SIZE, ImageProbe.checkSize(301, 200, 300, 200));
        assertEquals(ImageProbe.SizeProblem.WRONG_SIZE, ImageProbe.checkSize(200, 300, 300, 200));   // 縦横が逆
        assertEquals(ImageProbe.SizeProblem.WRONG_SIZE, ImageProbe.checkSize(600, 400, 300, 200));   // 比が同じでも、大きさが違えば断る
        assertNull(ImageProbe.checkSize(123, 45, 0, 0));    // 指定なし
        assertNull(ImageProbe.checkSize(123, 45, 0, 200));  // 片方が0なら、制限しない
    }

    @Test void theSizeMessageNamesBothTheRequiredAndTheActualSize() {
        assertEquals("画像の大きさが違います（必要: 300×200、この画像: 640×480）", ImageProbe.explain(640, 480, 300, 200));
    }
}
