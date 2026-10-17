package me.silverwolfg11.maptowny.objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class IconSourceTest {
    @TempDir Path directory;

    @Test void urlsAreUsedAsTheyAre() throws Exception {
        URL url = IconSource.resolve("https://example.com/icon.png", directory.toFile());
        assertEquals("https://example.com/icon.png", url.toString());
        assertEquals("file:/tmp/icon.png", IconSource.resolve("file:/tmp/icon.png", directory.toFile()).toString());
    }

    @Test void aRelativePathPointsInsideThePluginFolder() throws Exception {
        URL url = IconSource.resolve("icons/king.png", directory.toFile());
        assertEquals(new File(directory.toFile(), "icons/king.png").toURI().toURL(), url);
    }

    @Test void pathsThatEscapeThePluginFolderAreRejected() {
        assertThrows(java.net.MalformedURLException.class, () -> IconSource.resolve("../secret.png", directory.toFile()));
        assertThrows(java.net.MalformedURLException.class, () -> IconSource.resolve("icons/../../secret.png", directory.toFile()));
        assertThrows(java.net.MalformedURLException.class, () -> IconSource.resolve("/etc/passwd", directory.toFile()));
    }

    @Test void blankValuesAreRejected() {
        assertThrows(java.net.MalformedURLException.class, () -> IconSource.resolve("", directory.toFile()));
    }
}
