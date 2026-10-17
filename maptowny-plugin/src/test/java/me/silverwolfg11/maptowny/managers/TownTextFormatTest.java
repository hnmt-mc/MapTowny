package me.silverwolfg11.maptowny.managers;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TownTextFormatTest {
    @Test void marksShowEnabledAndDisabled() {
        assertEquals("⭕", TownTextFormat.mark(true));
        assertEquals("❌", TownTextFormat.mark(false));
    }

    @Test void listsUseTheJapaneseSeparatorAndNoneWhenEmpty() {
        assertEquals("なし", TownTextFormat.list(List.of()));
        assertEquals("太郎", TownTextFormat.list(List.of("太郎")));
        assertEquals("太郎、花子、Steve", TownTextFormat.list(List.of("太郎", "花子", "Steve")));
    }

    @Test void foundedDatesAreInJapaneseTimeWithoutLeadingZeros() {
        assertEquals("未設定", TownTextFormat.founded(0));
        assertEquals("2026年10月17日", TownTextFormat.founded(java.time.ZonedDateTime.of(2026, 10, 17, 9, 30, 0, 0, java.time.ZoneId.of("Asia/Tokyo")).toInstant().toEpochMilli()));
        assertEquals("2026年1月5日", TownTextFormat.founded(java.time.ZonedDateTime.of(2026, 1, 5, 0, 0, 0, 0, java.time.ZoneId.of("Asia/Tokyo")).toInstant().toEpochMilli()));
        // UTC で前日の15時は、日本時間では翌日0時
        assertEquals("2026年10月18日", TownTextFormat.founded(java.time.ZonedDateTime.of(2026, 10, 17, 15, 0, 0, 0, java.time.ZoneId.of("UTC")).toInstant().toEpochMilli()));
    }

    @Test void nationStatusNamesTheCapitalOrMemberAndIsEmptyWithoutANation() {
        assertEquals("", TownTextFormat.nationStatus(false, false, ""));
        assertEquals("🏰 日ノ本の首都", TownTextFormat.nationStatus(true, true, "日ノ本"));
        assertEquals("🛡️ 日ノ本に所属", TownTextFormat.nationStatus(true, false, "日ノ本"));
    }

    @Test void ranksHaveJapaneseNamesAndUnknownOnesKeepTheirOwnName() {
        assertEquals("補佐", TownTextFormat.rankName("assistant"));
        assertEquals("議員", TownTextFormat.rankName("councillor"));
        assertEquals("custom_rank", TownTextFormat.rankName("custom_rank"));
    }

    @Test void theRankListShowsOnlyRanksWithMembersAndIsEmptyOtherwise() {
        java.util.Map<String, java.util.List<String>> ranks = new java.util.LinkedHashMap<>();
        ranks.put("assistant", List.of("太郎", "花子"));
        ranks.put("councillor", List.of());
        ranks.put("custom_rank", List.of("Steve"));
        assertEquals("<div>🛡️ 補佐: <b>太郎、花子</b></div><div>🛡️ custom_rank: <b>Steve</b></div>", TownTextFormat.rankList(ranks));
        assertEquals("<div>役職についている人はいません</div>", TownTextFormat.rankList(new java.util.LinkedHashMap<>()));
    }
}
