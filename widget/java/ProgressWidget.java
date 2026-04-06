package com.newyou.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

public class ProgressWidget extends AppWidgetProvider {

    public static final String PREFS = "NewYouProgressWidget";

    public static void updateAppWidget(Context ctx, AppWidgetManager mgr, int id) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        String xpLogJson = p.getString("xpLog",  "[]");
        int    level     = p.getInt("level",      1);
        String rank      = p.getString("rank",    "E");
        int    totalXP   = p.getInt("totalXP",    0);

        int[]    values = new int[0];
        String[] labels = new String[0];
        try {
            JSONArray arr = new JSONArray(xpLogJson);
            int n = arr.length();
            values = new int[n];
            labels = new String[n];
            for (int i = 0; i < n; i++) {
                JSONObject e = arr.getJSONObject(i);
                values[i] = e.optInt("xp",   0);
                labels[i] = e.optString("date", "");
            }
        } catch (Exception ignored) {}

        Bitmap graph = drawGraph(values, labels);

        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget_progress);
        v.setImageViewBitmap(R.id.wp_graph, graph);
        v.setTextViewText(R.id.wp_title,    rank + "-RANK  \u00b7  LV." + level);
        v.setTextViewText(R.id.wp_total,    totalXP > 0
                ? formatXP(totalXP) + " TOTAL XP"
                : "Earn XP to build your graph");
        v.setTextViewText(R.id.wp_subtitle, values.length > 0
                ? "LAST " + values.length + " DAYS" : "");

        Intent launch = new Intent(ctx, MainActivity.class);
        launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, 30, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.wp_root, pi);

        mgr.updateAppWidget(id, v);
    }

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) updateAppWidget(ctx, mgr, id);
    }

    // ── Graph rendering ───────────────────────────────────────────────────────

    static Bitmap drawGraph(int[] values, String[] labels) {
        final int W = 800, H = 300;
        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas c   = new Canvas(bmp);
        c.drawColor(0xFF07071A);

        if (values == null || values.length == 0) {
            Paint tp = txt(0xFF4D607E, 28);
            tp.setTextAlign(Paint.Align.CENTER);
            c.drawText("Earn XP to build your progress graph", W / 2f, H / 2f, tp);
            return bmp;
        }

        final float PL = 16, PR = 16, PT = 24, PB = 44;
        final float CW = W - PL - PR, CH = H - PT - PB;
        int n = values.length;
        int maxVal = 1;
        for (int x : values) if (x > maxVal) maxVal = x;
        float stepX = n > 1 ? CW / (n - 1) : CW;

        float[] px = new float[n], py = new float[n];
        for (int i = 0; i < n; i++) {
            px[i] = PL + i * stepX;
            py[i] = PT + CH * (1f - (float) values[i] / maxVal);
        }

        // Grid
        Paint gp = new Paint(); gp.setColor(0x1200D4FF); gp.setStrokeWidth(1);
        for (int g = 0; g <= 3; g++) c.drawLine(PL, PT + CH * g / 3f, PL + CW, PT + CH * g / 3f, gp);
        int vSkip = n <= 14 ? 1 : 3;
        for (int i = 0; i < n; i += vSkip) c.drawLine(px[i], PT, px[i], PT + CH, gp);

        // Fill under line
        Path fill = new Path();
        fill.moveTo(px[0], PT + CH);
        fill.lineTo(px[0], py[0]);
        for (int i = 1; i < n; i++) fill.lineTo(px[i], py[i]);
        fill.lineTo(px[n - 1], PT + CH);
        fill.close();
        Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG);
        fp.setShader(new LinearGradient(0, PT, 0, PT + CH,
                0x4400D4FF, 0x0000D4FF, Shader.TileMode.CLAMP));
        c.drawPath(fill, fp);

        // Line
        Path line = new Path();
        line.moveTo(px[0], py[0]);
        for (int i = 1; i < n; i++) line.lineTo(px[i], py[i]);
        Paint lp = new Paint(Paint.ANTI_ALIAS_FLAG);
        lp.setColor(0xFF00D4FF); lp.setStrokeWidth(3);
        lp.setStyle(Paint.Style.STROKE);
        lp.setStrokeJoin(Paint.Join.ROUND); lp.setStrokeCap(Paint.Cap.ROUND);
        c.drawPath(line, lp);

        // Dots
        Paint do_ = new Paint(Paint.ANTI_ALIAS_FLAG); do_.setColor(0xFF00D4FF);
        Paint di  = new Paint(Paint.ANTI_ALIAS_FLAG); di.setColor(0xFFFFFFFF);
        for (int i = 0; i < n; i++) { c.drawCircle(px[i], py[i], 5, do_); c.drawCircle(px[i], py[i], 2.5f, di); }

        // X-axis date labels
        if (labels != null) {
            Paint lbl = txt(0xFF4D607E, 19); lbl.setTextAlign(Paint.Align.CENTER);
            int sk = n <= 7 ? 1 : n <= 14 ? 2 : 3;
            for (int i = 0; i < n; i += sk) {
                String s = (i < labels.length && labels[i].length() >= 10)
                        ? labels[i].substring(5) : (i < labels.length ? labels[i] : "");
                c.drawText(s, px[i], H - 10, lbl);
            }
        }

        // Max label
        Paint yl = txt(0xFF4D607E, 19); yl.setTextAlign(Paint.Align.LEFT);
        c.drawText(formatXP(maxVal), PL + 2, PT + 16, yl);

        return bmp;
    }

    private static Paint txt(int color, float size) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color); p.setTextSize(size); return p;
    }

    private static String formatXP(int xp) {
        if (xp >= 1_000_000) return (xp / 1_000_000) + "M";
        if (xp >= 1_000)     return (xp / 1_000) + "K";
        return String.valueOf(xp);
    }
}
