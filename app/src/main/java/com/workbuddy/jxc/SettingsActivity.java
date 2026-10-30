package com.workbuddy.jxc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class SettingsActivity extends Activity {
    private SettingsPanel panel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = Ui.vertical(this);
        root.setFitsSystemWindows(true);
        root.setBackgroundColor(Ui.BACKGROUND);
        root.setPadding(Ui.dp(this, 18), Ui.dp(this, 12), Ui.dp(this, 18), Ui.dp(this, 18));
        LinearLayout header = Ui.horizontal(this);
        Button back = Ui.button(this, "返回", false);
        back.setOnClickListener(v -> finish());
        header.addView(back);
        header.addView(Ui.text(this, "  系统设置", 24, Ui.TEXT, true));
        root.addView(header);
        ScrollView scroll = new ScrollView(this);
        panel = new SettingsPanel(this);
        scroll.addView(panel.getView(), Ui.matchWrap());
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        panel.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        panel.destroy();
        super.onDestroy();
    }
}
