package com.workbuddy.jxc;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class BarChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<TrendPoint> data = new ArrayList<>();

    public BarChartView(Context context) {
        super(context);
        paint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(List<TrendPoint> data) {
        this.data = data == null ? new ArrayList<>() : data;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) {
            return;
        }
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setTextSize(Ui.dp(getContext(), 10));
        if (data.isEmpty()) {
            paint.setColor(Ui.MUTED);
            canvas.drawText("暂无趋势数据", width / 2f, height / 2f, paint);
            return;
        }
        long max = 1;
        for (TrendPoint point : data) {
            max = Math.max(max, Math.max(point.salesCents, point.purchaseCents));
        }
        float top = Ui.dp(getContext(), 14);
        float bottom = height - Ui.dp(getContext(), 30);
        float chartHeight = bottom - top;
        float slot = width / (float) data.size();
        float barWidth = Math.min(Ui.dp(getContext(), 14), slot / 4);
        paint.setStrokeWidth(Ui.dp(getContext(), 1));
        paint.setColor(Ui.BORDER);
        canvas.drawLine(0, bottom, width, bottom, paint);
        for (int i = 0; i < data.size(); i++) {
            TrendPoint point = data.get(i);
            float center = slot * i + slot / 2;
            float salesHeight = chartHeight * point.salesCents / max;
            float purchaseHeight = chartHeight * point.purchaseCents / max;
            paint.setColor(Ui.PRIMARY);
            canvas.drawRoundRect(center - barWidth - 2, bottom - salesHeight, center - 2,
                    bottom, 4, 4, paint);
            paint.setColor(0xFFE59A45);
            canvas.drawRoundRect(center + 2, bottom - purchaseHeight, center + barWidth + 2,
                    bottom, 4, 4, paint);
            paint.setColor(Ui.MUTED);
            canvas.drawText(point.label, center, height - Ui.dp(getContext(), 10), paint);
        }
    }
}
