package com.newyou.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

public class NewYouWidget extends AppWidgetProvider {

    public static final String PREFS = "NewYouStatusWidget";

    public static void updateAppWidget(Context ctx, AppWidgetManager mgr, int id) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        String name      = p.getString("name",    "HUNTER");
        String rank      = p.getString("rank",    "E");
        int    level     = p.getInt("level",      1);
        int    xpPct     = p.getInt("xpPct",      0);
        int    hp        = p.getInt("hp",         100);
        int    maxHp     = p.getInt("maxHp",      100);
        int    coins     = p.getInt("coins",      0);
        int    totalXP   = p.getInt("totalXP",    0);
        String oath      = p.getString("oath",    "");
        String statsJson = p.getString("stats",   "[]");

        int hpPct = maxHp > 0 ? (int)((hp * 100L) / maxHp) : 100;

        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget_status);

        v.setTextViewText(R.id.ws_rank,  rank.equals("MONARCH") ? "M" : rank);
        v.setTextViewText(R.id.ws_name,  name.toUpperCase());
        v.setTextViewText(R.id.ws_level, "LV." + level);
        v.setTextViewText(R.id.ws_xp_label, "XP  \u2014  " + totalXP + " total");
        v.setProgressBar(R.id.ws_xp_bar, 100, clamp(xpPct), false);
        v.setProgressBar(R.id.ws_hp_bar, 100, clamp(hpPct), false);
        v.setTextViewText(R.id.ws_hp_text, hp + " / " + maxHp + " HP");
        v.setTextViewText(R.id.ws_coins, "\uD83E\uDE99  " + coins + " COINS");

        // Build stats line from JSON array [{abbr, value}]
        StringBuilder sb = new StringBuilder();
        try {
            JSONArray arr = new JSONArray(statsJson);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject s = arr.getJSONObject(i);
                if (i > 0) sb.append("   ");
                sb.append(s.optString("abbr", "???"))
                  .append(" ")
                  .append(s.optInt("value", 1));
            }
        } catch (Exception ignored) { sb.append("—"); }
        v.setTextViewText(R.id.ws_stats, sb.length() > 0 ? sb.toString() : "—");

        // Oath row — hidden when empty
        if (oath != null && !oath.trim().isEmpty()) {
            String display = oath.length() > 100 ? oath.substring(0, 97) + "..." : oath;
            v.setTextViewText(R.id.ws_oath, "\u201c" + display + "\u201d");
            v.setViewVisibility(R.id.ws_oath_row, View.VISIBLE);
        } else {
            v.setViewVisibility(R.id.ws_oath_row, View.GONE);
        }

        // Tap widget → open app
        Intent launch = new Intent(ctx, MainActivity.class);
        launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, 10, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.ws_root, pi);

        mgr.updateAppWidget(id, v);
    }

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) updateAppWidget(ctx, mgr, id);
    }

    private static int clamp(int val) { return Math.max(0, Math.min(100, val)); }
}
