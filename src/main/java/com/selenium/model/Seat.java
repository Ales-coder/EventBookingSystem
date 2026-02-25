package com.selenium.model;

public class Seat {

    private long seatId;
    private long venueId;

    private String section;
    private String rowLabel;
    private int seatNo;

    public Seat(long seatId, long venueId,
                String section, String rowLabel, int seatNo) {

        this.seatId = seatId;
        this.venueId = venueId;
        this.section = section;
        this.rowLabel = rowLabel;
        this.seatNo = seatNo;
    }

    public long getSeatId() {
        return seatId;
    }

    public long getVenueId() {
        return venueId;
    }

    public String getSection() {
        return section;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public int getSeatNo() {
        return seatNo;
    }

    @Override
    public String toString() {
        return section + "-" + rowLabel + seatNo;
    }
}
