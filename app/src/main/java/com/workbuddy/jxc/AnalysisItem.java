package com.workbuddy.jxc;

public class AnalysisItem {
    public final String code;
    public final String name;
    public final int quantity;
    public final String reason;

    public AnalysisItem(String code, String name, int quantity, String reason) {
        this.code = code;
        this.name = name;
        this.quantity = quantity;
        this.reason = reason;
    }
}
