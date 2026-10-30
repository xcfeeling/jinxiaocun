package com.workbuddy.jxc;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class StockTrendChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<StockPoint> data = new ArrayList<>();

    public StockTrendChartView(Context context) {
        super(context);
    }

    public void setData(List<StockPoint> data) {
        this.data = data == null ? new ArrayList<>() : data;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data.isEmpty()) return;
        int max = 1;
        for (StockPoint point : data) max = Math.max(max, point.stock);
        float left = 24;
        float right = getWidth() - 16;
        float bottom = getHeight() - 30;
        float top = 16;
        float slot = (right - left) / data.size();
        paint.setTextSize(Ui.dp(getContext(), 10));
        paint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < data.size(); i++) {
            StockPoint point = data.get(i);
            float barHeight = (bottom - top) * point.stock / max;
            float center = left + slot * i + slot / 2;
            paint.setColor(0xFFF8A9BC);
            canvas.drawRoundRect(center - slot * 0.25f, bottom - barHeight, center + slot * 0.25f,
                    bottom, 6, 6, paint);
            paint.setColor(Ui.MUTED);
            canvas.drawText(point.label, center, bottom + 20, paint);
            canvas.drawText(String.valueOf(point.stock), center, bottom - barHeight - 6, paint);
        }
    }
}
