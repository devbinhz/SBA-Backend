package com.bookverse.enums;

public enum DeliveryType {
    SELF(0L),
    GIFT(10_000L);

    private final long giftWrapFeeVnd;

    DeliveryType(long giftWrapFeeVnd) {
        this.giftWrapFeeVnd = giftWrapFeeVnd;
    }

    public long giftWrapFeeVnd() {
        return giftWrapFeeVnd;
    }
}
