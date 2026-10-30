package com.workbuddy.jxc;

import java.util.ArrayList;
import java.util.List;

public class AnalysisReport {
    public final String summary;
    public final String marketNotes;
    public final List<AnalysisItem> fastMoving = new ArrayList<>();
    public final List<AnalysisItem> slowMoving = new ArrayList<>();
    public final List<AnalysisItem> purchases = new ArrayList<>();
    public final List<String> categoryInsights = new ArrayList<>();
    public final List<String> risks = new ArrayList<>();

    public AnalysisReport(String summary, String marketNotes) {
        this.summary = summary;
        this.marketNotes = marketNotes;
    }
}
