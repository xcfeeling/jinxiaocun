package com.workbuddy.jxc;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class CategoryAnalysisView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<CategoryStats> data = new ArrayList<>();

    public CategoryAnalysisView(Context context) {
        super(context);
    }

    public void setData(List<CategoryStats> data) {
        this.data = data == null ? new ArrayList<>() : data;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data.isEmpty()) {
            paint.setColor(Ui.MUTED);
            paint.setTextSize(Ui.dp(getContext(), 13));
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("暂无分类数据", getWidth() / 2f, getHeight() / 2f, paint);
            return;
        }
        long max = 1;
        for (CategoryStats item : data) {
            max = Math.max(max, Math.max(item.salesQuantity, item.stockQuantity));
        }
        float rowHeight = getHeight() / (float) data.size();
        float labelWidth = Ui.dp(getContext(), 64);
        float chartWidth = getWidth() - labelWidth - Ui.dp(getContext(), 78);
        paint.setTextSize(Ui.dp(getContext(), 11));
        for (int i = 0; i < data.size(); i++) {
            CategoryStats item = data.get(i);
            float center = rowHeight * i + rowHeight / 2;
            paint.setColor(Ui.TEXT);
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(item.name, 0, center + 4, paint);
            float salesWidth = chartWidth * item.salesQuantity / max;
            float stockWidth = chartWidth * item.stockQuantity / max;
            paint.setColor(Ui.PRIMARY);
            canvas.drawRoundRect(labelWidth, center - 13, labelWidth + salesWidth, center - 3,
                    4, 4, paint);
            paint.setColor(0xFFE59A45);
            canvas.drawRoundRect(labelWidth, center + 3, labelWidth + stockWidth, center + 13,
                    4, 4, paint);
            paint.setColor(Ui.MUTED);
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(item.salesQuantity + " / " + item.stockQuantity + "件",
                    getWidth(), center + 4, paint);
        }
    }
}
