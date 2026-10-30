package com.workbuddy.jxc;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public final class DateRanges {
    public static final int DAY = 0;
    public static final int MONTH = 1;
    public static final int YEAR = 2;

    public static class Range {
        public final String label;
        public final long start;
        public final long end;

        public Range(String label, long start, long end) {
            this.label = label;
            this.start = start;
            this.end = end;
        }
    }

    private DateRanges() {
    }

    public static Range containing(int mode, long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        if (mode == DAY) {
            setStartOfDay(calendar);
        } else if (mode == MONTH) {
            setStartOfDay(calendar);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
        } else {
            setStartOfDay(calendar);
            calendar.set(Calendar.DAY_OF_YEAR, 1);
        }
        long start = calendar.getTimeInMillis();
        String label;
        if (mode == DAY) {
            label = String.format(Locale.CHINA, "%d年%d月%d日", calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        } else if (mode == MONTH) {
            label = String.format(Locale.CHINA, "%d年%d月", calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1);
            calendar.add(Calendar.MONTH, 1);
        } else {
            label = String.format(Locale.CHINA, "%d年", calendar.get(Calendar.YEAR));
            calendar.add(Calendar.YEAR, 1);
        }
        return new Range(label, start, calendar.getTimeInMillis());
    }

    public static long shift(int mode, long timestamp, int amount) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        if (mode == MONTH) {
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.add(Calendar.MONTH, amount);
        } else if (mode == YEAR) {
            calendar.set(Calendar.DAY_OF_YEAR, 1);
            calendar.add(Calendar.YEAR, amount);
        } else {
            calendar.add(Calendar.DAY_OF_MONTH, amount);
        }
        return calendar.getTimeInMillis();
    }

    public static List<Range> trend(int mode, long timestamp) {
        List<Range> ranges = new ArrayList<>();
        int count = mode == DAY ? 7 : mode == MONTH ? 6 : 5;
        long cursor = shift(mode, timestamp, -(count - 1));
        for (int i = 0; i < count; i++) {
            Range full = containing(mode, cursor);
            String label;
            if (mode == DAY) {
                Calendar day = Calendar.getInstance();
                day.setTimeInMillis(full.start);
                label = (day.get(Calendar.MONTH) + 1) + "/" + day.get(Calendar.DAY_OF_MONTH);
            } else if (mode == MONTH) {
                Calendar month = Calendar.getInstance();
                month.setTimeInMillis(full.start);
                label = (month.get(Calendar.MONTH) + 1) + "月";
            } else {
                label = full.label;
            }
            ranges.add(new Range(label, full.start, full.end));
            cursor = shift(mode, cursor, 1);
        }
        return ranges;
    }

    public static long startOfDay(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        setStartOfDay(calendar);
        return calendar.getTimeInMillis();
    }

    private static void setStartOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }
}
