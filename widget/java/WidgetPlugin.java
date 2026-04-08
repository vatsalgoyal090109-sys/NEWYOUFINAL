package com.system.app;

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

    @PluginMethod
    public void updateProgressWidget(PluginCall call) {
        try {
            Context ctx = getContext();
            SharedPreferences.Editor e =
                    ctx.getSharedPreferences(ProgressWidget.PREFS, Context.MODE_PRIVATE).edit();
            e.putString("xpLog",  call.getString("xpLog",  "[]"));
            e.putInt("level",     safeInt(call, "level",      1));
            e.putString("rank",   call.getString("rank",   "E"));
            e.putInt("totalXP",   safeInt(call, "totalXP",    0));
            e.apply();

            AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
            int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, ProgressWidget.class));
            for (int id : ids) ProgressWidget.updateAppWidget(ctx, mgr, id);
        } catch (Exception ignored) {}
        call.resolve();
    }

    // Kept so App.jsx calls don't throw errors — these are no-ops now
    @PluginMethod
    public void updateStatusWidget(PluginCall call) { call.resolve(); }

    @PluginMethod
    public void updateQuestsWidget(PluginCall call) { call.resolve(); }

    @PluginMethod
    public void getPendingHabitCompletions(PluginCall call) {
        JSObject result = new JSObject();
        result.put("pendingJson", "[]");
        call.resolve(result);
    }

    @PluginMethod
    public void clearPendingHabitCompletions(PluginCall call) { call.resolve(); }

    private int safeInt(PluginCall call, String key, int fallback) {
        try {
            Integer v = call.getInt(key);
            return v != null ? v : fallback;
        } catch (Exception e) { return fallback; }
    }
}
