package com.roletadefilmes.billing.domain;

import java.time.Instant;
import java.time.ZoneOffset;

public enum BillingPlanCode {
    PREMIUM_MONTHLY("Premium mensal", "MONTHLY", 1, 0),
    PREMIUM_ANNUAL("Premium anual", "ANNUAL", 0, 1);

    private final String label;
    private final String interval;
    private final int months;
    private final int years;

    BillingPlanCode(String label, String interval, int months, int years) {
        this.label = label;
        this.interval = interval;
        this.months = months;
        this.years = years;
    }

    public String label() {
        return label;
    }

    public String interval() {
        return interval;
    }

    public Instant nextPeriodEnd(Instant base) {
        return base.atZone(ZoneOffset.UTC).plusMonths(months).plusYears(years).toInstant();
    }
}
