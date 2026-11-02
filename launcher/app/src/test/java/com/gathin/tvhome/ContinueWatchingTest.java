package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class ContinueWatchingTest {
    @Before
    public void forgetHeld() {
        ContinueWatching.forgetHeld();
    }

    private static ContinueWatching.Item item(String pkg, String title, long engaged) {
        return new ContinueWatching.Item(pkg, title, "", null, -1f, null, engaged);
    }

    @Test
    public void eachShowKeepsItsNewestRow() {
        List<ContinueWatching.Item> shown = ContinueWatching.newestPerShow(Arrays.asList(
                item("tv.daily", "News", 100), item("tv.daily", "News", 300), item("tv.other", "Film", 200)));

        assertEquals(2, shown.size());
        assertEquals(300, shown.get(0).engaged);
        assertEquals("Film", shown.get(1).title);
    }

    @Test
    public void atMostTenCards() {
        List<ContinueWatching.Item> rows = new ArrayList<>();
        for (int i = 0; i < 15; i++) rows.add(item("tv.many", "Show " + i, i));

        assertEquals(ContinueWatching.MAX_ITEMS, ContinueWatching.newestPerShow(rows).size());
    }

    @Test
    public void vanishedAppKeepsItsCardsUntilTheHoldRunsOut() {
        String pkg = "tv.rewrites";
        ContinueWatching.holdVanished(new ArrayList<>(Collections.singletonList(item(pkg, "Film", 1))),
                Collections.singleton(pkg), 0);

        List<ContinueWatching.Item> gone = new ArrayList<>();
        ContinueWatching.holdVanished(gone, new HashSet<>(), 1_000);
        assertEquals(1, gone.size());
        assertTrue(ContinueWatching.isHolding());

        List<ContinueWatching.Item> stillGone = new ArrayList<>();
        ContinueWatching.holdVanished(stillGone, new HashSet<>(), 1_000 + ContinueWatching.HOLD_MS);
        assertTrue(stillGone.isEmpty());
        assertFalse(ContinueWatching.isHolding());
    }

    @Test
    public void appThatWritesAgainShowsItsNewRows() {
        String pkg = "tv.back";
        ContinueWatching.holdVanished(new ArrayList<>(Collections.singletonList(item(pkg, "Old", 1))),
                Collections.singleton(pkg), 0);
        ContinueWatching.holdVanished(new ArrayList<>(), new HashSet<>(), 500);

        List<ContinueWatching.Item> back = new ArrayList<>(Collections.singletonList(item(pkg, "New", 2)));
        ContinueWatching.holdVanished(back, Collections.singleton(pkg), 1_000);

        assertEquals(1, back.size());
        assertEquals("New", back.get(0).title);
    }
}
