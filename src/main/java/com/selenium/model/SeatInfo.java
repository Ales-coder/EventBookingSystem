package com.selenium.model;

import java.math.BigDecimal;

public class SeatInfo {

    private long seatId;
    private String label;
    private BigDecimal price;
    private String state;

    public SeatInfo(long seatId, String label, BigDecimal price, String state) {
        this.seatId = seatId;
        this.label = label;
        this.price = price;
        this.state = state;
    }

    public long getSeatId() { return seatId; }
    public String getLabel() { return label; }
    public BigDecimal getPrice() { return price; }
    public String getState() { return state; }

    @Override
    public String toString() {
        return label + " | " + state + " | " + price + " ALL";
    }
}
