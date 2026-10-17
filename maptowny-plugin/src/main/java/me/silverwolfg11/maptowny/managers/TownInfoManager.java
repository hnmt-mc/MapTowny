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

package me.silverwolfg11.maptowny.managers;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Government;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyObject;
import me.silverwolfg11.maptowny.objects.TextReplacement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// This class handles loading the tooltip HTML files as well as filling in the placeholders
public class TownInfoManager {

    private final TextReplacement<Town> clickReplacements;
    private final TextReplacement<Town> hoverReplacements;

    private final ImageSettings images;

    public TownInfoManager(File dataFolder, Logger errorLogger, ImageSettings images) {
        this.images = images;
        final String CLICK_FILE_NAME = "click_tooltip.html";
        final String HOVER_FILE_NAME = "hover_tooltip.html";

        // Create the parent directory if it doesn't exist
        if (!dataFolder.exists())
            dataFolder.mkdir();

        // Read hover and click files
        File clickWindowFile = new File(dataFolder, CLICK_FILE_NAME);
        if (!clickWindowFile.exists())
            copyFile(errorLogger, CLICK_FILE_NAME, clickWindowFile);

        clickReplacements = readHTMLReplacementFile(clickWindowFile, errorLogger);

        File hoverWindowFile = new File(dataFolder, HOVER_FILE_NAME);
        if (!hoverWindowFile.exists())
            copyFile(errorLogger, HOVER_FILE_NAME, hoverWindowFile);

        hoverReplacements = readHTMLReplacementFile(hoverWindowFile, errorLogger);

        registerReplacements();
    }

    // Read the replacement HTML file and return the associated text replacement object.
    @NotNull
    private TextReplacement<Town> readHTMLReplacementFile(File file, Logger errorLogger) {
        String replacementFileRaw = null;
        try {
            replacementFileRaw = readFile(file);
        } catch (IOException e) {
            errorLogger.log(Level.SEVERE, "Unable to read file " + file.getName(), e);
        }

        if (replacementFileRaw != null) {
            return TextReplacement.fromHTML(replacementFileRaw);
        }
        else {
            return TextReplacement.empty();
        }
    }

    // Replacement Registration Methods
    private void registerReplacements() {
        register("town", TownyObject::getName);
        register("mayor", t -> t.getMayor().getName());
        register("firespread", t -> String.valueOf(t.getPermissions().fire));
        register("pvp", t -> String.valueOf(t.getPermissions().pvp));
        register("explosion", t -> String.valueOf(t.getPermissions().explosion));
        register("mobs", t -> String.valueOf(t.getPermissions().mobs));
        // ⭕/❌ で表す項目（%pvp% などの true/false は、そのまま残してある）
        // 町長の顔・町の旗・国の旗（画像が無いときは空）
        register("mayor_face", t -> {
            try { return ImageHtml.img(ImageHtml.faceUrl(images.faceUrl(), t.getMayor().getUUID()), images.faceSize()); }
            catch (RuntimeException error) { return ""; }
        });
        // 旗を枠で囲んだ箱。町は、いつも出す（旗がなければ未設定の案内）。国は、町が国に入っているときだけ出す。
        register("town_flag_box", t -> ImageHtml.flagBox("🏘️ 町の旗", flagImg(FlagStore.get(t), images.townFlagSize()), "/town set flag <URL>", images.townFlagSize()));
        register("nation_flag_box", t -> !t.hasNation() ? "" : ImageHtml.flagBox("🏰 国の旗",
                flagImg(FlagStore.get(TownyAPI.getInstance().getTownNationOrNull(t)), images.nationFlagSize()), "/nation set flag <URL>", images.nationFlagSize()));
        register("town_flag", t -> flagImg(FlagStore.get(t), images.townFlagSize()));
        register("nation_flag", t -> flagImg(t.hasNation() ? FlagStore.get(TownyAPI.getInstance().getTownNationOrNull(t)) : null, images.nationFlagSize()));
        // 町のすべての役職と、その役職の人（クリックして開く欄に使う）
        register("ranklist", t -> {
            java.util.Map<String, java.util.List<String>> ranks = new java.util.LinkedHashMap<>();
            for (String rank : com.palmergames.bukkit.towny.permissions.TownyPerms.getTownRanks())
                ranks.put(rank, t.getRank(rank).stream().map(TownyObject::getName).collect(Collectors.toList()));
            return TownTextFormat.rankList(ranks);
        });
        register("pvp_mark", t -> TownTextFormat.mark(t.getPermissions().pvp));
        register("firespread_mark", t -> TownTextFormat.mark(t.getPermissions().fire));
        register("explosion_mark", t -> TownTextFormat.mark(t.getPermissions().explosion));
        register("mobs_mark", t -> TownTextFormat.mark(t.getPermissions().mobs));
        register("public_mark", t -> TownTextFormat.mark(t.isPublic()));
        register("peaceful_mark", t -> TownTextFormat.mark(t.isNeutral()));
        register("residents", t -> TownTextFormat.list(t.getResidents().stream().map(TownyObject::getName).collect(Collectors.toList())));
        register("residentdisplaynames", t -> TownTextFormat.list(t.getResidents()
                .stream()
                .map(r -> {
                    Player resPlayer = r.getPlayer();
                    return resPlayer != null ? resPlayer.getDisplayName() : r.getFormattedName();
                })
                .collect(Collectors.toList())));
        register("residentcount", t -> String.valueOf(t.getNumResidents()));
        register("board", Government::getBoard);
        register("nationstatus", t -> t.hasNation()
                ? TownTextFormat.nationStatus(true, t.isCapital(), TownyAPI.getInstance().getTownNationOrNull(t).getName())
                : "");
        register("public", t -> String.valueOf(t.isPublic()));
        register("peaceful", t -> String.valueOf(t.isNeutral()));

        register("founded", t -> TownTextFormat.founded(t.getRegistered()));

        registerParenthesesReplacement("nation",
                t-> t.hasNation() ? TownyAPI.getInstance().getTownNationOrNull(t).getName() : ""
        );

        // Register Economy Replacements
        if (TownyEconomyHandler.isActive() && TownySettings.isUsingEconomy()) {
            register("tax", t -> {
                if (t.isTaxPercentage()) {
                    return t.getTaxes() + "%";
                }

                return TownyEconomyHandler.getFormattedBalance(t.getTaxes());
            });
            register("upkeep", t -> {
                if (!t.hasUpkeep())
                    return "";

                return TownyEconomyHandler.getFormattedBalance(TownySettings.getTownUpkeepCost(t));
            });
            register("bank",
                    t -> TownyEconomyHandler.getFormattedBalance(t.getAccount().getCachedBalance())
            );
        }

        registerRanks();
    }

    // 旗のURLを、許可したドメインのものだけ <img> にする（設定が変わったあとに、古いURLが出続けないように、表示のときにも検証する）。
    private String flagImg(String url, int width) {
        return url != null && ImageHtml.check(url, images.allowedDomains()) == null ? ImageHtml.imgByWidth(url, width) : "";
    }

    // Register a replacement that will replace parenthesis if empty.
    // E.g. For the key "nation", the replacement "(%nation%)" or "%nation%" would be valid.
    private void registerParenthesesReplacement(String key, Function<Town, String> func) {
        String wrappedKey = "%" + key + "%";
        String pKey = "(" + wrappedKey + ")";
        Function<Town, String> escaped = t -> TownTextFormat.html(func.apply(t));   // 括弧なしで使うときも、プレイヤーが書ける文字を無害にする
        Function<Town, String> pFunc = t -> {
            String result = func.apply(t);
            return (result != null && !result.isEmpty()) ? "(" + TownTextFormat.html(result) + ")" : "";
        };

        if (!clickReplacements.registerReplacement(pKey, pFunc)) {
            clickReplacements.registerReplacement(wrappedKey, escaped);
        }

        if (!hoverReplacements.registerReplacement(pKey, pFunc)) {
            hoverReplacements.registerReplacement(wrappedKey, escaped);
        }
    }

    // すでにHTMLになっている項目（画像・旗の箱・役職の一覧）。ほかの項目は、プレイヤーが書ける文字が入るので、HTMLに入れる前に無害にする。
    private static final java.util.Set<String> HTML_KEYS = new java.util.HashSet<>(java.util.Arrays.asList(
            "mayor_face", "town_flag", "nation_flag", "town_flag_box", "nation_flag_box", "ranklist",
            "residents", "residentdisplaynames"));   // 住民の一覧は、TownTextFormat.list の中で、無害にしてある

    private void register(String key, Function<Town, String> func) {
        String wrappedKey = "%" + key + "%";
        registerReplacement(wrappedKey, HTML_KEYS.contains(key) ? func : town -> TownTextFormat.html(func.apply(town)));
    }


    // API Methods

    public void registerReplacement(@NotNull String key, @NotNull Function<Town, String> func) {
        clickReplacements.registerReplacement(key, func);
        hoverReplacements.registerReplacement(key, func);
    }

    public void unregisterReplacement(@NotNull String key) {
        clickReplacements.unregisterReplacement(key);
        hoverReplacements.unregisterReplacement(key);
    }

    // Does not validate ranks.
    private void registerRanks() {
        // Pattern gets anything matching "%rank_<rank name>%"
        Pattern pattern = Pattern.compile("%rank_.+%");
        registerRanks(clickReplacements, pattern);
        registerRanks(hoverReplacements, pattern);
    }

    private void registerRanks(TextReplacement<Town> textReplacement, Pattern rankPattern) {
        String textToReplace = textReplacement.getTextToReplace();

        if (textToReplace == null)
            return;

        Matcher matcher = rankPattern.matcher(textToReplace);

        while (matcher.find()) {
            String rankReplacement = textToReplace.substring(matcher.start(), matcher.end());
            // %rank_test% - Just need "test" part so substring at 6 and go up to the %.
            final String rank = rankReplacement.substring(6, rankReplacement.length() - 1);
            textReplacement.registerReplacement(rankReplacement, t -> {
                return TownTextFormat.list(t.getRank(rank).stream().map(TownyObject::getName).collect(Collectors.toList()));
            });
        }
    }

    public String getClickTooltip(Town town, Logger errorLogger) {
        final String townName = town.getName();
        return clickReplacements.getReplacedText(town, (replacementKey, ex) -> {
            errorLogger.log(
                    Level.SEVERE,
                    String.format("Error applying the replacement '%s' for click information on town '%s'!", replacementKey, townName),
                    ex
            );
        });
    }

    public String getHoverTooltip(Town town, Logger errorLogger) {
        final String townName = town.getName();
        return hoverReplacements.getReplacedText(town, (replacementKey, ex) -> {
            errorLogger.log(
                    Level.SEVERE,
                    String.format("Error applying the replacement '%s' for hover information on town '%s'!", replacementKey, townName),
                    ex
            );
        });
    }


    // Utility Methods

    private void copyFile(Logger errorLogger, String resource, File dest) {
        InputStream input = getClass().getClassLoader().getResourceAsStream(resource);

        if (input == null) {
            errorLogger.severe("The resource file '"  + resource + "' could not be found!");
            return;
        }

        try {
            Files.copy(input, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            errorLogger.log(Level.SEVERE, "Unable to copy \"" + resource + "\" to plugin directory!");
        }
    }

    // Read file as string
    // https://stackoverflow.com/questions/326390/how-do-i-create-a-java-string-from-the-contents-of-a-file
    private static String readFile(File file)
            throws IOException {
        byte[] encoded = Files.readAllBytes(file.toPath());
        return new String(encoded, StandardCharsets.UTF_8);
    }

}