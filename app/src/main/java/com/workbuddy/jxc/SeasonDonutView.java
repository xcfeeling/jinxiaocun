package com.workbuddy.jxc;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class SeasonDonutView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<CategoryStats> data = new ArrayList<>();

    public SeasonDonutView(Context context) {
        super(context);
    }

    public void setData(List<CategoryStats> data) {
        this.data = data == null ? new ArrayList<>() : data;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long total = 0;
        for (CategoryStats item : data) total += item.salesCents;
        paint.setTextAlign(Paint.Align.CENTER);
        if (total <= 0) {
            paint.setColor(Ui.MUTED);
            paint.setTextSize(Ui.dp(getContext(), 14));
            canvas.drawText("本期暂无季节销售数据", getWidth() / 2f, getHeight() / 2f, paint);
            return;
        }
        float size = Math.min(getHeight() - 24, getWidth() * 0.46f);
        float cx = size / 2 + 8;
        float cy = getHeight() / 2f;
        RectF oval = new RectF(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size * 0.22f);
        float start = -90;
        for (CategoryStats item : data) {
            if (item.salesCents <= 0) continue;
            paint.setColor(color(item.name));
            float sweep = 360f * item.salesCents / total;
            canvas.drawArc(oval, start, sweep, false, paint);
            start += sweep;
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Ui.TEXT);
        paint.setTextSize(Ui.dp(getContext(), 20));
        paint.setFakeBoldText(true);
        canvas.drawText(Formatters.money(total), cx, cy, paint);
        paint.setFakeBoldText(false);
        paint.setColor(Ui.MUTED);
        paint.setTextSize(Ui.dp(getContext(), 11));
        canvas.drawText("总销售额", cx, cy + Ui.dp(getContext(), 22), paint);

        float legendX = size + Ui.dp(getContext(), 26);
        float rowHeight = Math.min(42, (getHeight() - 24) / (float) data.size());
        float y = (getHeight() - rowHeight * data.size()) / 2 + rowHeight / 2;
        for (CategoryStats item : data) {
            if (item.salesCents <= 0) continue;
            paint.setColor(color(item.name));
            canvas.drawCircle(legendX, y, Ui.dp(getContext(), 5), paint);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setColor(Ui.TEXT);
            paint.setTextSize(Ui.dp(getContext(), 12));
            canvas.drawText(item.name + "  " + String.format(java.util.Locale.US, "%.1f%%",
                    item.salesCents * 100.0 / total), legendX + 12, y + 4, paint);
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(Formatters.money(item.salesCents), getWidth() - 4, y + 4, paint);
            y += rowHeight;
        }
    }

    private int color(String season) {
        switch (season) {
            case "秋": return 0xFFF26D91;
            case "夏": return 0xFFFFC884;
            case "春": return 0xFFB5A0EA;
            case "冬": return 0xFF78B7E8;
            default: return 0xFF9FC8B4;
        }
    }
}
