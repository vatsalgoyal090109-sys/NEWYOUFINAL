package com.newyou.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

public class QuestsWidget extends AppWidgetProvider {

    public static final String PREFS  = "NewYouQuestsWidget";
    public static final String ACTION_COMPLETE_HABIT = "com.newyou.app.COMPLETE_HABIT";

    public static void updateAppWidget(Context ctx, AppWidgetManager mgr, int id) {
        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget_quests);

        try {
            SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

            int pendingQuests = 0, habitTotal = 0, habitDone = 0;

            String questsJson = p.getString("quests", "[]");
            if (questsJson != null && !questsJson.trim().isEmpty()) {
                try {
                    org.json.JSONArray q = new org.json.JSONArray(questsJson);
                    for (int i = 0; i < q.length(); i++) {
                        if (!q.getJSONObject(i).optBoolean("completed", false)) pendingQuests++;
                    }
                } catch (Exception ignored) {}
            }

            String habitsJson = p.getString("habits", "[]");
            if (habitsJson != null && !habitsJson.trim().isEmpty()) {
                try {
                    org.json.JSONArray h = new org.json.JSONArray(habitsJson);
                    habitTotal = h.length();
                    for (int i = 0; i < h.length(); i++) {
                        if (h.getJSONObject(i).optBoolean("completedToday", false)) habitDone++;
                    }
                } catch (Exception ignored) {}
            }

            v.setTextViewText(R.id.wq_quest_count,
                    pendingQuests + " PENDING QUEST" + (pendingQuests != 1 ? "S" : ""));
            v.setTextViewText(R.id.wq_habit_count,
                    habitDone + " / " + habitTotal + " HABITS DONE");

        } catch (Exception e) {
            v.setTextViewText(R.id.wq_quest_count, "Open app to sync");
            v.setTextViewText(R.id.wq_habit_count, "");
        }

        // Wire ListView to RemoteViewsService
        try {
            Intent svc = new Intent(ctx, QuestsWidgetService.class);
            v.setRemoteAdapter(R.id.wq_list, svc);
            v.setEmptyView(R.id.wq_list, R.id.wq_empty);

            // PendingIntent template for habit DONE taps — FLAG_MUTABLE is required
            Intent tpl = new Intent(ctx, HabitCompleteReceiver.class);
            tpl.setAction(ACTION_COMPLETE_HABIT);
            PendingIntent template = PendingIntent.getBroadcast(ctx, 0, tpl,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            v.setPendingIntentTemplate(R.id.wq_list, template);
        } catch (Exception ignored) {}

        // Header tap → open app
        try {
            Intent launch = new Intent(ctx, MainActivity.class);
            launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(ctx, 20, launch,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            v.setOnClickPendingIntent(R.id.wq_header, pi);
        } catch (Exception ignored) {}

        mgr.updateAppWidget(id, v);
    }

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) updateAppWidget(ctx, mgr, id);
    }

    public static void notifyListChanged(Context ctx) {
        try {
            AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
            ComponentName cn = new ComponentName(ctx, QuestsWidget.class);
            int[] ids = mgr.getAppWidgetIds(cn);
            mgr.notifyAppWidgetViewDataChanged(ids, R.id.wq_list);
            for (int id : ids) updateAppWidget(ctx, mgr, id);
        } catch (Exception ignored) {}
    }
}
