package com.workbuddy.jxc;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

public final class Ui {
    public static final int PRIMARY = Color.rgb(244, 63, 111);
    public static final int PRIMARY_DARK = Color.rgb(211, 42, 86);
    public static final int PRIMARY_LIGHT = Color.rgb(255, 235, 241);
    public static final int BACKGROUND = Color.rgb(251, 247, 246);
    public static final int SURFACE = Color.WHITE;
    public static final int TEXT = Color.rgb(45, 37, 40);
    public static final int MUTED = Color.rgb(139, 122, 128);
    public static final int BORDER = Color.rgb(241, 227, 230);
    public static final int GREEN = Color.rgb(37, 157, 107);
    public static final int ORANGE = Color.rgb(237, 151, 55);
    public static final int BLUE = Color.rgb(92, 145, 220);
    public static final int PURPLE = Color.rgb(157, 113, 218);

    private Ui() {
    }

    public static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static LinearLayout vertical(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    public static LinearLayout horizontal(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    public static LinearLayout card(Context context) {
        LinearLayout card = vertical(context);
        card.setPadding(dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 16));
        card.setBackground(roundRect(SURFACE, 16, BORDER, context));
        card.setElevation(dp(context, 1));
        return card;
    }

    public static TextView text(Context context, String value, float size, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setLineSpacing(0, 1.12f);
        return view;
    }

    public static TextView pill(Context context, String value, int textColor, int backgroundColor) {
        TextView view = text(context, value, 11, textColor, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(context, 9), dp(context, 3), dp(context, 9), dp(context, 3));
        view.setBackground(roundRect(backgroundColor, 20, backgroundColor, context));
        return view;
    }

    public static Button button(Context context, String label, boolean primary) {
        Button button = new Button(context);
        button.setText(label);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(primary ? Color.WHITE : PRIMARY_DARK);
        button.setMinHeight(dp(context, 46));
        button.setMinimumHeight(dp(context, 46));
        button.setPadding(dp(context, 13), 0, dp(context, 13), 0);
        button.setBackground(roundRect(primary ? PRIMARY : SURFACE, 11, primary ? PRIMARY : BORDER, context));
        return button;
    }

    public static EditText input(Context context, String hint, boolean multiline) {
        EditText input = new EditText(context);
        input.setHint(hint);
        input.setTextSize(15);
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setSingleLine(!multiline);
        if (multiline) {
            input.setMinLines(3);
            input.setGravity(Gravity.TOP | Gravity.START);
        }
        input.setPadding(dp(context, 13), dp(context, 9), dp(context, 13), dp(context, 9));
        input.setBackground(roundRect(SURFACE, 11, BORDER, context));
        return input;
    }

    public static EditText decimalInput(Context context, String hint) {
        EditText input = input(context, hint, false);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        return input;
    }

    public static EditText integerInput(Context context, String hint) {
        EditText input = input(context, hint, false);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        return input;
    }

    public static Space space(Context context, int height) {
        Space space = new Space(context);
        space.setLayoutParams(new LinearLayout.LayoutParams(1, dp(context, height)));
        return space;
    }

    public static void add(LinearLayout parent, View child, int width, int height, int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.topMargin = dp(parent.getContext(), topMargin);
        parent.addView(child, params);
    }

    public static GradientDrawable roundRect(int color, int radiusDp, int strokeColor, Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(context, radiusDp));
        drawable.setStroke(dp(context, 1), strokeColor);
        return drawable;
    }

    public static GradientDrawable gradient(int startColor, int endColor, int radiusDp, Context context) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{startColor, endColor});
        drawable.setCornerRadius(dp(context, radiusDp));
        return drawable;
    }

    public static LinearLayout.LayoutParams weightParams(int height) {
        return new LinearLayout.LayoutParams(0, height, 1);
    }

    public static ViewGroup.LayoutParams matchWrap() {
        return new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
