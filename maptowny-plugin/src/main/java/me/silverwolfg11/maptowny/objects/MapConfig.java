/*
 * Copyright (c) 2021 Silverwolfg11
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.silverwolfg11.maptowny.objects;

import me.Silverwolfg11.CommentConfig.annotations.Comment;
import me.Silverwolfg11.CommentConfig.annotations.ConfigVersion;
import me.Silverwolfg11.CommentConfig.annotations.Node;
import me.Silverwolfg11.CommentConfig.annotations.SerializableConfig;
import me.Silverwolfg11.CommentConfig.node.ParentConfigNode;
import me.Silverwolfg11.CommentConfig.serialization.ClassDeserializer;
import me.Silverwolfg11.CommentConfig.serialization.ClassSerializer;
import me.Silverwolfg11.CommentConfig.serialization.NodeSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@SerializableConfig
@ConfigVersion(1.0)
public class MapConfig {

    @Comment("Worlds that should display town claims.")
    @Node("enabled-worlds")
    private List<String> enabledWorlds = Collections.singletonList("world");

    @Comment({"", "How often should the plugin render all towns? (In minutes)"})
    @Node("update-period")
    private int updatePeriod = 5;

    @Comment({"", "Layer Properties"})
    @Node("layer")
    private LayerInfo layerInfo = new LayerInfo();

    @Comment({"", "Fill Style.", "Properties about how claims should look on the map"})
    @Node("fill-style")
    private FillStyle fillStyle = new FillStyle();

    @Comment({"", "Icon Properties:", "Icons are placed at the center of a town's homeblock."})
    @Node("icon-info")
    private IconInfo iconInfo = new IconInfo();

    // アイコンのファイルを探す、プラグインのフォルダ（設定には書き出さない）。
    private transient File dataFolder;

    @SerializableConfig
    private class LayerInfo {
        @Comment("Name of the layer")
        private String name = "Towny";

        @Comment("Should the layer be toggleable by users?")
        @Node("show-controls")
        private boolean showControls = true;

        @Comment("Should the layer be invisible by default?")
        @Node("default-hidden")
        private boolean defaultHidden = false;

        @Comment({"Layer Priority.", "Don't need to touch this unless other Pl3xMap add-ons are interfering with the layer."})
        @Node("layer-priority")
        private int layerPriority = 5;

        @Comment({"The z-index on which the layer will display.", "Decrease if you want the layer to be more blended in with the map."})
        @Node("z-index")
        private int zIndex = 250;
    }

    @SerializableConfig
    private class FillStyle {
        @Comment("Whether to fill the claim with color")
        private boolean fill = true;

        @Node("fill-color")
        private String fillColor = "#FF0000";

        @Node("fill-opacity")
        private double fillOpacity = 0.35;

        @Comment("Use specified nation color as the fill color instead?")
        @Node("use-nation-color-fill")
        private boolean useNationColorFill = true;

        @Comment({"", "Whether to draw a stroke along the claim path."})
        private boolean stroke = true;

        @Comment("Stroke width in pixels")
        @Node("stroke-weight")
        private int strokeWeight = 3;

        @Node("stroke-color")
        private String strokeColor = "#FF0000";

        @Node("stroke-opacity")
        private double strokeOpacity = 0.8;

        @Comment("Use specified nation color as the stroke color instead?")
        @Node("use-nation-color-stroke")
        private boolean useNationColorStroke = false;

        @Node("use-town-color-fill")
        @Comment({"Use specified town color as the fill color instead?",
                  "This option will take priority over the 'use-nation-color-fill' option if it is enabled.",
                  "Towns set their color in-game with /t set mapcolor <color>. A town without a color uses the nation color, then the default."})
        private boolean useTownColorFill = true;

        @Node("use-town-color-stroke")
        @Comment({"Use specified town color as the stroke color instead?",
                "This option will take priority over the 'use-nation-color-stroke' option if it is enabled."})
        private boolean useTownColorStroke = true;

        private transient Color awtFillColor, awtStrokeColor;
    }

    @SerializableConfig
    private class IconInfo {
        @Comment({"Icon for the town's homeblock. An image URL, or a file path inside the plugin folder (e.g. icons/blueflag.png).",
                "The default is the same flag icon that Dynmap-Towny uses (Map Icons Collection, CC BY-SA 3.0)."})
        @Node("town-icon")
        private String townIconImage = "icons/blueflag.png";

        @Comment({"Icon for a town if they are the capital of the nation. An image URL, or a file path inside the plugin folder.",
                "Put 'default' to use the town icon image. The default is the crown icon that Dynmap-Towny uses."})
        @Node("capital-icon")
        private String capitalIconImage = "icons/king.png";

        @Comment({"Icon for an outpost claim that will appear at the location of outpost spawns. Icon must be a valid image URL.",
                "Put 'default' to use the town icon image.",
                "Put 'empty' to not place icons at outposts."})
        @Node("outpost-icon")
        private String outpostIconImage = "default";

        @Comment({"Height of the icon (pixels). The default 16 is the same size that Dynmap-Towny shows its icons at."})
        @Node("icon-height")
        private int iconSizeX = 16;

        @Comment("Width of the icon")
        @Node("icon-width")
        private int iconSizeY = 16;
    }

    public List<String> getEnabledWorlds() {
        return Collections.unmodifiableList(enabledWorlds);
    }

    public int getUpdatePeriod() {
        return updatePeriod;
    }

    @NotNull
    public LayerOptions getLayerOptions() {
        return new LayerOptions(
                layerInfo.name,
                layerInfo.showControls,
                layerInfo.defaultHidden,
                layerInfo.layerPriority,
                layerInfo.zIndex
        );
    }

    @NotNull
    public MarkerOptions.Builder buildMarkerOptions() throws NumberFormatException {
        if (fillStyle.awtFillColor == null)
            fillStyle.awtFillColor = Color.decode(fillStyle.fillColor);

        if (fillStyle.awtStrokeColor == null)
            fillStyle.awtStrokeColor = Color.decode(fillStyle.strokeColor);

        return MarkerOptions.builder()
                .fill(fillStyle.fill)
                .fillColor(fillStyle.awtFillColor)
                .fillOpacity(fillStyle.fillOpacity)
                .stroke(fillStyle.stroke)
                .strokeWeight(fillStyle.strokeWeight)
                .strokeColor(fillStyle.awtStrokeColor)
                .strokeOpacity(fillStyle.strokeOpacity);
    }

    public boolean useNationFillColor() {
        return fillStyle.useNationColorFill;
    }

    public boolean useNationStrokeColor() {
        return fillStyle.useNationColorStroke;
    }

    public boolean useTownFillColor() {
        return fillStyle.useTownColorFill;
    }

    public boolean useTownStrokeColor() {
        return fillStyle.useTownColorStroke;
    }

    @Nullable
    public BufferedImage loadTownIcon(Logger errorLogger) {
        return loadIcon("town", iconInfo.townIconImage, errorLogger);
    }

    @Nullable
    public BufferedImage loadCapitalIcon(Logger errorLogger) {
        String url = iconInfo.capitalIconImage;

        if (url.equalsIgnoreCase("default"))
            url = iconInfo.townIconImage;

        return loadIcon("capital", url, errorLogger);
    }

    @Nullable
    public BufferedImage loadOutpostIcon(Logger errorLogger) {
        String url = iconInfo.outpostIconImage;

        if (url.isEmpty() || url.equalsIgnoreCase("empty"))
            return null;
        else if (url.equalsIgnoreCase("default"))
            url = iconInfo.townIconImage;

        return loadIcon("outpost", url, errorLogger);
    }

    private BufferedImage loadIcon(String type, String urlStr, Logger errorLogger) {
        URL url;
        try {
            url = IconSource.resolve(urlStr, dataFolder);
        } catch (MalformedURLException ex) {
            errorLogger.log(Level.SEVERE, "Cannot load " + type + " icon due to an invalid URL!", ex);
            return null;
        }

        try {
            return ImageIO.read(url);
        } catch (IOException e) {
            errorLogger.log(Level.SEVERE, "Error while loading " + type + " image icon!", e);
            return null;
        }
    }

    public int getIconSizeX() {
        return iconInfo.iconSizeX;
    }

    public int getIconSizeY() {
        return iconInfo.iconSizeY;
    }


    public static MapConfig loadConfig(File directory, Logger errorLogger) throws IOException {

        if (!directory.exists())
            directory.mkdir();

        File configFile = new File(directory, "config.yml");

        NodeSerializer serializer = new NodeSerializer();
        if (!configFile.exists()) {
            configFile.createNewFile();
            // Use save config mapping
            MapConfig config = new MapConfig();
            ParentConfigNode node = ClassSerializer.serializeClass(config);
            serializer.serializeToFile(configFile, node);
            config.dataFolder = directory;
            return config;
        }
        else {
            ClassDeserializer deserializer = new ClassDeserializer();
            deserializer.setErrorLogger(errorLogger);
            MapConfig loaded = deserializer.deserializeClassAndUpdate(configFile, MapConfig.class, serializer);
            loaded.dataFolder = directory;
            return loaded;
        }

    }

}
