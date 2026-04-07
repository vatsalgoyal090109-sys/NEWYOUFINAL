package com.newyou.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.RemoteViews;

public class NewYouWidget extends AppWidgetProvider {

    public static final String PREFS = "NewYouStatusWidget";

    public static void updateAppWidget(Context ctx, AppWidgetManager mgr, int id) {
        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget_status);

        try {
            SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

            String name    = safe(p.getString("name",  "HUNTER"));
            String rank    = safe(p.getString("rank",  "E"));
            int    level   = p.getInt("level",   1);
            int    xpPct   = p.getInt("xpPct",   0);
            int    hp      = p.getInt("hp",       100);
            int    maxHp   = p.getInt("maxHp",    100);
            int    coins   = p.getInt("coins",    0);
            int    totalXP = p.getInt("totalXP",  0);
            String oath    = p.getString("oath",  "");
            String stats   = p.getString("stats", "");

            int hpPct = maxHp > 0 ? (int)((hp * 100L) / maxHp) : 100;

            v.setTextViewText(R.id.ws_rank,  rank.equals("MONARCH") ? "M" : rank);
            v.setTextViewText(R.id.ws_name,  name.toUpperCase());
            v.setTextViewText(R.id.ws_level, "LV." + level);
            v.setTextViewText(R.id.ws_xp_label, "XP  —  " + totalXP + " total");
            v.setProgressBar(R.id.ws_xp_bar, 100, clamp(xpPct), false);
            v.setProgressBar(R.id.ws_hp_bar, 100, clamp(hpPct), false);
            v.setTextViewText(R.id.ws_hp_text, hp + " / " + maxHp + " HP");
            v.setTextViewText(R.id.ws_coins, coins + " COINS");

            // Build stats line safely
            String statsLine = buildStatsLine(stats);
            v.setTextViewText(R.id.ws_stats, statsLine);

            // Oath — only show if non-empty
            if (oath != null && !oath.trim().isEmpty()) {
                String display = oath.length() > 100
                        ? oath.substring(0, 97) + "..." : oath;
                v.setTextViewText(R.id.ws_oath, "\u201c" + display + "\u201d");
                v.setViewVisibility(R.id.ws_oath_row, View.VISIBLE);
            } else {
                v.setViewVisibility(R.id.ws_oath_row, View.GONE);
            }

        } catch (Exception e) {
            // Safety fallback — show defaults rather than crash
            v.setTextViewText(R.id.ws_name,  "HUNTER");
            v.setTextViewText(R.id.ws_rank,  "E");
            v.setTextViewText(R.id.ws_level, "LV.1");
            v.setTextViewText(R.id.ws_xp_label, "Open app to sync");
            v.setTextViewText(R.id.ws_hp_text, "");
            v.setTextViewText(R.id.ws_coins, "");
            v.setTextViewText(R.id.ws_stats, "");
            v.setViewVisibility(R.id.ws_oath_row, View.GONE);
        }

        // Tap widget → open app (always set this, even if data load failed)
        try {
            Intent launch = new Intent(ctx, MainActivity.class);
            launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(ctx, 10, launch,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            v.setOnClickPendingIntent(R.id.ws_root, pi);
        } catch (Exception ignored) {}

        mgr.updateAppWidget(id, v);
    }

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) updateAppWidget(ctx, mgr, id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String buildStatsLine(String statsJson) {
        if (statsJson == null || statsJson.trim().isEmpty()) {
            return "STR 1   INT 1   DIS 1   VIT 1   FOC 1   CHA 1";
        }
        try {
            org.json.JSONArray arr = new org.json.JSONArray(statsJson);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject s = arr.getJSONObject(i);
                if (i > 0) sb.append("   ");
                sb.append(s.optString("abbr", "???"))
                  .append(" ")
                  .append(s.optInt("value", 1));
            }
            return sb.length() > 0 ? sb.toString()
                    : "STR 1   INT 1   DIS 1   VIT 1   FOC 1   CHA 1";
        } catch (Exception e) {
            return "STR 1   INT 1   DIS 1   VIT 1   FOC 1   CHA 1";
        }
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }
}
