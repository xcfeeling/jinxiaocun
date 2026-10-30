package com.workbuddy.jxc;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class SalesTrendChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<TrendPoint> data = new ArrayList<>();

    public SalesTrendChartView(Context context) {
        super(context);
    }

    public void setData(List<TrendPoint> data) {
        this.data = data == null ? new ArrayList<>() : data;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data.isEmpty()) return;
        float left = Ui.dp(getContext(), 46);
        float right = getWidth() - Ui.dp(getContext(), 14);
        float top = Ui.dp(getContext(), 16);
        float bottom = getHeight() - Ui.dp(getContext(), 30);
        long max = 1;
        for (TrendPoint point : data) max = Math.max(max, Math.max(point.salesCents, point.purchaseCents));
        max = Math.max(10000, ((max + 9999) / 10000) * 10000);
        paint.setStrokeWidth(1);
        paint.setTextSize(Ui.dp(getContext(), 10));
        for (int i = 0; i <= 2; i++) {
            float y = top + (bottom - top) * i / 2;
            paint.setColor(Ui.BORDER);
            canvas.drawLine(left, y, right, y, paint);
            paint.setColor(Ui.MUTED);
            paint.setTextAlign(Paint.Align.RIGHT);
            long value = max - max * i / 2;
            canvas.drawText(value >= 10000 ? (value / 10000) + "万" : String.valueOf(value / 100),
                    left - 8, y + 4, paint);
        }
        float slot = (right - left) / Math.max(1, data.size() - 1);
        Path salesLine = new Path();
        Path area = new Path();
        Path purchaseLine = new Path();
        for (int i = 0; i < data.size(); i++) {
            TrendPoint point = data.get(i);
            float x = left + slot * i;
            float salesY = bottom - (bottom - top) * point.salesCents / max;
            float purchaseY = bottom - (bottom - top) * point.purchaseCents / max;
            if (i == 0) {
                salesLine.moveTo(x, salesY);
                purchaseLine.moveTo(x, purchaseY);
                area.moveTo(x, bottom);
                area.lineTo(x, salesY);
            } else {
                salesLine.lineTo(x, salesY);
                purchaseLine.lineTo(x, purchaseY);
                area.lineTo(x, salesY);
            }
            if (i == data.size() - 1) area.lineTo(x, bottom);
            paint.setColor(Ui.MUTED);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(point.label, x, bottom + Ui.dp(getContext(), 20), paint);
        }
        area.close();
        paint.setShader(new LinearGradient(0, top, 0, bottom, 0x55F43F6F, 0x00F43F6F, Shader.TileMode.CLAMP));
        canvas.drawPath(area, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Ui.dp(getContext(), 2));
        paint.setColor(Ui.PRIMARY);
        canvas.drawPath(salesLine, paint);
        paint.setColor(Ui.ORANGE);
        paint.setStrokeWidth(Ui.dp(getContext(), 1));
        canvas.drawPath(purchaseLine, paint);
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < data.size(); i++) {
            float x = left + slot * i;
            float y = bottom - (bottom - top) * data.get(i).salesCents / max;
            paint.setColor(Ui.SURFACE);
            canvas.drawCircle(x, y, Ui.dp(getContext(), 5), paint);
            paint.setColor(Ui.PRIMARY);
            canvas.drawCircle(x, y, Ui.dp(getContext(), 3), paint);
        }
    }
}
