package com.newyou.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class QuestsWidgetFactory implements RemoteViewsService.RemoteViewsFactory {

    private static final int TYPE_SECTION = 0;
    private static final int TYPE_QUEST   = 1;
    private static final int TYPE_HABIT   = 2;

    static class Row {
        int     type;
        String  id, name, badge, sub;
        boolean completed;
        int     streak;
    }

    private final Context    ctx;
    private final List<Row>  rows = new ArrayList<>();

    QuestsWidgetFactory(Context context) { this.ctx = context; }

    @Override public void onCreate()        { reload(); }
    @Override public void onDataSetChanged(){ reload(); }
    @Override public void onDestroy()       { rows.clear(); }

    private void reload() {
        rows.clear();
        SharedPreferences p = ctx.getSharedPreferences(QuestsWidget.PREFS, Context.MODE_PRIVATE);

        // ── Quests section ────────────────────────────────────────────────────
        try {
            JSONArray arr = new JSONArray(p.getString("quests", "[]"));
            if (arr.length() > 0) {
                Row sec = new Row(); sec.type = TYPE_SECTION; sec.name = "ACTIVE QUESTS";
                rows.add(sec);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    Row r = new Row();
                    r.type      = TYPE_QUEST;
                    r.id        = o.optString("id");
                    r.name      = o.optString("name", "Quest");
                    r.badge     = o.optString("type", "DAILY");
                    r.sub       = "+" + o.optInt("xp", 0) + " XP";
                    r.completed = o.optBoolean("completed", false);
                    rows.add(r);
                }
            }
        } catch (Exception ignored) {}

        // ── Habits section ────────────────────────────────────────────────────
        try {
            JSONArray arr = new JSONArray(p.getString("habits", "[]"));
            if (arr.length() > 0) {
                Row sec = new Row(); sec.type = TYPE_SECTION; sec.name = "HABITS";
                rows.add(sec);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    Row r = new Row();
                    r.type      = TYPE_HABIT;
                    r.id        = o.optString("id");
                    r.name      = o.optString("name", "Habit");
                    r.streak    = o.optInt("streak", 0);
                    r.completed = o.optBoolean("completedToday", false);
                    r.sub       = r.streak > 0
                            ? r.streak + " day streak \uD83D\uDD25"
                            : "+" + o.optInt("xpPerCompletion", 25) + " XP";
                    rows.add(r);
                }
            }
        } catch (Exception ignored) {}

        if (rows.isEmpty()) {
            Row empty = new Row(); empty.type = TYPE_SECTION;
            empty.name = "No quests or habits yet";
            rows.add(empty);
        }
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position >= rows.size()) return null;
        Row row = rows.get(position);
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_quest_item);

        if (row.type == TYPE_SECTION) {
            rv.setViewVisibility(R.id.wqi_section_header, View.VISIBLE);
            rv.setViewVisibility(R.id.wqi_content,        View.GONE);
            rv.setTextViewText(R.id.wqi_section_text, row.name);
            return rv;
        }

        rv.setViewVisibility(R.id.wqi_section_header, View.GONE);
        rv.setViewVisibility(R.id.wqi_content,        View.VISIBLE);
        rv.setTextViewText(R.id.wqi_name, row.name);
        rv.setTextViewText(R.id.wqi_sub,  row.sub);

        if (row.type == TYPE_HABIT) {
            rv.setTextViewText(R.id.wqi_badge, row.completed ? "DONE" : "HABIT");
            rv.setTextColor(R.id.wqi_badge, row.completed ? 0xFF2ECC71 : 0xFFFF69B4);

            if (row.completed) {
                rv.setViewVisibility(R.id.wqi_done_icon,    View.VISIBLE);
                rv.setViewVisibility(R.id.wqi_complete_btn, View.GONE);
            } else {
                rv.setViewVisibility(R.id.wqi_done_icon,    View.GONE);
                rv.setViewVisibility(R.id.wqi_complete_btn, View.VISIBLE);
                // Fill-in carries the habit ID to HabitCompleteReceiver
                Intent fill = new Intent();
                fill.putExtra("habitId", row.id);
                rv.setOnClickFillInIntent(R.id.wqi_complete_btn, fill);
            }
        } else {
            rv.setTextViewText(R.id.wqi_badge, row.badge);
            rv.setTextColor(R.id.wqi_badge, badgeColor(row.badge));
            rv.setViewVisibility(R.id.wqi_done_icon,    row.completed ? View.VISIBLE : View.GONE);
            rv.setViewVisibility(R.id.wqi_complete_btn, View.GONE);
        }

        return rv;
    }

    @Override public int     getCount()        { return rows.size(); }
    @Override public RemoteViews getLoadingView() { return null; }
    @Override public int     getViewTypeCount() { return 1; }
    @Override public long    getItemId(int p)   { return p; }
    @Override public boolean hasStableIds()     { return false; }

    private int badgeColor(String badge) {
        if (badge == null) return 0xFF8A9BBF;
        switch (badge) {
            case "DAILY":  return 0xFF00D4FF;
            case "WEEKLY": return 0xFF9B59B6;
            case "SIDE":   return 0xFFF39C12;
            case "STUDY":  return 0xFF2ECC71;
            default:       return 0xFF8A9BBF;
        }
    }
}
