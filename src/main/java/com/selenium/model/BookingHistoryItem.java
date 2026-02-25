package com.selenium.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BookingHistoryItem {

    private long bookingId;
    private LocalDateTime createdAt;
    private String bookingStatus;

    private long eventId;
    private String eventTitle;
    private LocalDateTime eventStartTime;


    private String eventStatus;
    private boolean eventAvailable;

    private long seatId;
    private String seatLabel;
    private BigDecimal price;


    private LocalDateTime paidAt;

    public BookingHistoryItem(long bookingId,
                              LocalDateTime createdAt,
                              String bookingStatus,
                              long eventId,
                              String eventTitle,
                              LocalDateTime eventStartTime,
                              String eventStatus,
                              boolean eventAvailable,
                              long seatId,
                              String seatLabel,
                              BigDecimal price,
                              LocalDateTime paidAt) {

        this.bookingId = bookingId;
        this.createdAt = createdAt;
        this.bookingStatus = bookingStatus;

        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.eventStartTime = eventStartTime;

        this.eventStatus = eventStatus;
        this.eventAvailable = eventAvailable;

        this.seatId = seatId;
        this.seatLabel = seatLabel;
        this.price = price;

        this.paidAt = paidAt;
    }

    public long getBookingId() { return bookingId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getBookingStatus() { return bookingStatus; }

    public long getEventId() { return eventId; }
    public String getEventTitle() { return eventTitle; }
    public LocalDateTime getEventStartTime() { return eventStartTime; }

    public String getEventStatus() { return eventStatus; }
    public boolean isEventAvailable() { return eventAvailable; }

    public long getSeatId() { return seatId; }
    public String getSeatLabel() { return seatLabel; }

    public BigDecimal getPrice() { return price; }
    public LocalDateTime getPaidAt() { return paidAt; }
}
