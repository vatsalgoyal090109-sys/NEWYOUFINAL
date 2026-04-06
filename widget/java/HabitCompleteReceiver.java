package com.newyou.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Receives taps on the "DONE" button in the Quests widget habit rows.
 *
 * Flow:
 *  1. Marks the habit completedToday=true in widget SharedPreferences
 *     so the widget list refreshes instantly — no app open needed.
 *  2. Queues the habit ID in pendingCompletions.
 *  3. Next time the app opens, WidgetPlugin.getPendingHabitCompletions()
 *     returns the queue, the JS dispatches COMPLETE_HABIT + addXP,
 *     then calls clearPendingHabitCompletions().
 */
public class HabitCompleteReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String habitId = intent.getStringExtra("habitId");
        if (habitId == null || habitId.isEmpty()) return;

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        // ── 1. Update widget habits JSON ──────────────────────────────────────
        SharedPreferences qPrefs = ctx.getSharedPreferences(
                QuestsWidget.PREFS, Context.MODE_PRIVATE);
        try {
            JSONArray habits = new JSONArray(qPrefs.getString("habits", "[]"));
            for (int i = 0; i < habits.length(); i++) {
                JSONObject h = habits.getJSONObject(i);
                if (habitId.equals(h.optString("id"))) {
                    h.put("completedToday", true);
                    // Optimistically increment streak by 1 for display
                    h.put("streak", h.optInt("streak", 0) + 1);
                    h.put("sub", h.optInt("streak", 0) + " day streak \uD83D\uDD25");
                    habits.put(i, h);
                    break;
                }
            }
            qPrefs.edit().putString("habits", habits.toString()).apply();
        } catch (Exception ignored) {}

        // ── 2. Queue pending completion for the app to process ────────────────
        SharedPreferences statusPrefs = ctx.getSharedPreferences(
                NewYouWidget.PREFS, Context.MODE_PRIVATE);
        try {
            JSONArray pending = new JSONArray(
                    statusPrefs.getString("pendingCompletions", "[]"));
            // Guard against duplicates
            for (int i = 0; i < pending.length(); i++) {
                JSONObject e = pending.getJSONObject(i);
                if (habitId.equals(e.optString("id")) && today.equals(e.optString("date"))) {
                    // Already queued today
                    QuestsWidget.notifyListChanged(ctx);
                    return;
                }
            }
            JSONObject entry = new JSONObject();
            entry.put("id",   habitId);
            entry.put("date", today);
            pending.put(entry);
            statusPrefs.edit().putString("pendingCompletions", pending.toString()).apply();
        } catch (Exception ignored) {}

        // ── 3. Refresh the widget list ────────────────────────────────────────
        QuestsWidget.notifyListChanged(ctx);
    }
}
