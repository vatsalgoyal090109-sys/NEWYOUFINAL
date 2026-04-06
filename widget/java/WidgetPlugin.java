package com.newyou.app;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "WidgetPlugin")
public class WidgetPlugin extends Plugin {

    // ── Status Widget ────────────────────────────────────────────────────────

    @PluginMethod
    public void updateStatusWidget(PluginCall call) {
        Context ctx = getContext();
        SharedPreferences.Editor e =
                ctx.getSharedPreferences(NewYouWidget.PREFS, Context.MODE_PRIVATE).edit();

        e.putString("name",    call.getString("name",    "HUNTER"));
        e.putString("rank",    call.getString("rank",    "E"));
        e.putInt("level",      safeInt(call, "level",       1));
        e.putInt("xpPct",      safeInt(call, "xpPct",       0));
        e.putInt("totalXP",    safeInt(call, "totalXP",     0));
        e.putInt("hp",         safeInt(call, "hp",         100));
        e.putInt("maxHp",      safeInt(call, "maxHp",      100));
        e.putInt("coins",      safeInt(call, "coins",        0));
        e.putString("oath",    call.getString("oath",    ""));
        e.putString("stats",   call.getString("stats",   "[]"));
        e.apply();

        push(ctx, NewYouWidget.class);
        call.resolve();
    }

    // ── Quests + Habits Widget ───────────────────────────────────────────────

    @PluginMethod
    public void updateQuestsWidget(PluginCall call) {
        Context ctx = getContext();
        SharedPreferences.Editor e =
                ctx.getSharedPreferences(QuestsWidget.PREFS, Context.MODE_PRIVATE).edit();

        e.putString("quests", call.getString("quests", "[]"));
        e.putString("habits", call.getString("habits", "[]"));
        e.apply();

        QuestsWidget.notifyListChanged(ctx);
        call.resolve();
    }

    // ── Progress Widget ──────────────────────────────────────────────────────

    @PluginMethod
    public void updateProgressWidget(PluginCall call) {
        Context ctx = getContext();
        SharedPreferences.Editor e =
                ctx.getSharedPreferences(ProgressWidget.PREFS, Context.MODE_PRIVATE).edit();

        e.putString("xpLog",  call.getString("xpLog",  "[]"));
        e.putInt("level",     safeInt(call, "level",      1));
        e.putString("rank",   call.getString("rank",   "E"));
        e.putInt("totalXP",   safeInt(call, "totalXP",    0));
        e.apply();

        push(ctx, ProgressWidget.class);
        call.resolve();
    }

    // ── Pending habit completions (queued by HabitCompleteReceiver) ──────────

    @PluginMethod
    public void getPendingHabitCompletions(PluginCall call) {
        String json = getContext()
                .getSharedPreferences(NewYouWidget.PREFS, Context.MODE_PRIVATE)
                .getString("pendingCompletions", "[]");
        JSObject result = new JSObject();
        result.put("pendingJson", json);
        call.resolve(result);
    }

    @PluginMethod
    public void clearPendingHabitCompletions(PluginCall call) {
        getContext()
            .getSharedPreferences(NewYouWidget.PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("pendingCompletions", "[]")
            .apply();
        call.resolve();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void push(Context ctx, Class<?> cls) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, cls));
        if (cls == NewYouWidget.class) {
            for (int id : ids) NewYouWidget.updateAppWidget(ctx, mgr, id);
        } else if (cls == ProgressWidget.class) {
            for (int id : ids) ProgressWidget.updateAppWidget(ctx, mgr, id);
        }
    }

    private int safeInt(PluginCall call, String key, int fallback) {
        try { Integer v = call.getInt(key); return v != null ? v : fallback; }
        catch (Exception e) { return fallback; }
    }
}
