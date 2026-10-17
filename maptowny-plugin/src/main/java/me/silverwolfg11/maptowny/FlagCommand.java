package me.silverwolfg11.maptowny;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import me.silverwolfg11.maptowny.managers.FlagStore;
import me.silverwolfg11.maptowny.managers.ImageHtml;
import me.silverwolfg11.maptowny.managers.ImageProbe;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

// /town set flag <URL>・/nation set flag <URL>: 町・国の旗の画像を設定する。clear で解除。
// 町は町長、国は国王だけが設定できる。権限は maptowny.flag.town・maptowny.flag.nation。
public final class FlagCommand implements CommandExecutor {
    private final MapTowny plugin;
    private final boolean nation;

    public FlagCommand(MapTowny plugin, boolean nation) { this.plugin = plugin; this.nation = nation; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(ChatColor.RED + "プレイヤーだけが使えます"); return true; }
        Player player = (Player) sender;
        if (!player.hasPermission(nation ? "maptowny.flag.nation" : "maptowny.flag.town")) { player.sendMessage(ChatColor.RED + "旗を設定する権限がありません"); return true; }
        String[] rest = args.length > 0 && args[0].equalsIgnoreCase("flag") ? java.util.Arrays.copyOfRange(args, 1, args.length) : args;
        String kind = nation ? "nation" : "town";
        if (rest.length != 1) { player.sendMessage(ChatColor.RED + "使い方: /" + kind + " set flag <画像のURL>（解除は /" + kind + " set flag clear）"); return true; }

        Resident resident = TownyAPI.getInstance().getResident(player);
        Town town = resident == null ? null : resident.getTownOrNull();
        Nation nationObj = town == null ? null : town.getNationOrNull();
        if (nation ? (nationObj == null || !nationObj.isKing(resident)) : (town == null || !town.isMayor(resident))) {
            player.sendMessage(ChatColor.RED + (nation ? "国王だけが、国の旗を設定できます" : "町長だけが、町の旗を設定できます"));
            return true;
        }

        if (rest[0].equalsIgnoreCase("clear")) {
            if (nation) FlagStore.clear(nationObj); else FlagStore.clear(town);
            refresh(town, nationObj);
            player.sendMessage(ChatColor.GREEN + (nation ? "国の旗を解除しました" : "町の旗を解除しました"));
            return true;
        }

        // imgur の画像のページのURLは、画像そのもののURLに直してから調べる。
        final String url = ImageHtml.normalize(rest[0]);
        ImageHtml.Problem problem = ImageHtml.check(url, plugin.imageSettings().allowedDomains());
        if (problem != null) { player.sendMessage(ChatColor.RED + explain(problem)); return true; }
        final me.silverwolfg11.maptowny.managers.ImageSettings settings = plugin.imageSettings();
        final java.util.UUID id = player.getUniqueId();
        player.sendMessage(ChatColor.GRAY + "画像の大きさを確かめています…");
        // 画像の大きさは、ネットワークで調べるので、メインスレッドの外で行い、結果をメインスレッドで反映する。
        plugin.async(() -> {
            String error = null;
            if (settings.requiredWidth() > 0 && settings.requiredHeight() > 0) {
                int[] size = ImageProbe.dimensions(ImageProbe.fetch(url));
                if (size == null) error = "画像を読み込めませんでした（png・jpg・gif の画像で、" + (ImageProbe.MAX_BYTES / 1024 / 1024) + "MB までです）";
                else if (ImageProbe.checkSize(size[0], size[1], settings.requiredWidth(), settings.requiredHeight()) != null)
                    error = ImageProbe.explain(size[0], size[1], settings.requiredWidth(), settings.requiredHeight());
            }
            final String failure = error;
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                Player current = org.bukkit.Bukkit.getPlayer(id);
                if (failure != null) { if (current != null) current.sendMessage(ChatColor.RED + failure); return; }
                // 確かめている間に、町長・国王でなくなった場合は、設定しない。
                Resident now = current == null ? null : TownyAPI.getInstance().getResident(current);
                Town nowTown = now == null ? null : now.getTownOrNull();
                Nation nowNation = nowTown == null ? null : nowTown.getNationOrNull();
                if (current == null || (nation ? (nowNation == null || !nowNation.isKing(now)) : (nowTown == null || !nowTown.isMayor(now)))) return;
                if (nation) FlagStore.set(nowNation, url); else FlagStore.set(nowTown, url);
                refresh(nowTown, nowNation);
                current.sendMessage(ChatColor.GREEN + (nation ? "国の旗を設定しました。地図には、数分以内に反映されます" : "町の旗を設定しました。地図には、数分以内に反映されます"));
            });
        });
        return true;
    }

    // 設定・解除したあと、地図の表示を作り直す（国の旗なら、国の町すべて）。
    private void refresh(Town town, Nation nationObj) {
        if (nation && nationObj != null) nationObj.getTowns().forEach(t -> plugin.getLayerManager().renderTown(t));
        else if (town != null) plugin.getLayerManager().renderTown(town);
    }

    static String explain(ImageHtml.Problem problem) {
        switch (problem) {
            case EMPTY: return "画像のURLを指定してください";
            case TOO_LONG: return "URLが長すぎます（" + ImageHtml.MAX_LENGTH + "文字まで）";
            case NOT_HTTPS: return "https:// で始まるURLだけが使えます";
            case BAD_DOMAIN: return "そのサイトの画像は使えません（運営が許可したサイトの画像だけが使えます）";
            case BAD_EXTENSION: return "画像そのもののURL（.png・.jpg・.jpeg・.gif で終わるもの）を指定してください。画像のページのURLは使えません（imgur は、画像を右クリックして「画像アドレスをコピー」で取れます）";
            default: return "URLの形が正しくありません（空白や引用符などは使えません）";
        }
    }
}
