package com.selenium.model;

import java.time.LocalDateTime;

public class Event {

    private long eventId;
    private String title;
    private String category;
    private LocalDateTime startTime;
    private int seatsLeft;


    private String status = "ACTIVE";

    public Event(long eventId, String title, String category, LocalDateTime startTime, int seatsLeft) {
        this.eventId = eventId;
        this.title = title;
        this.category = category;
        this.startTime = startTime;
        this.seatsLeft = seatsLeft;
    }

    public Event(long eventId, String title, String category, LocalDateTime startTime) {
        this(eventId, title, category, startTime, 0);
    }

    public long getEventId() { return eventId; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public LocalDateTime getStartTime() { return startTime; }
    public int getSeatsLeft() { return seatsLeft; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = (status == null ? "ACTIVE" : status); }

    @Override
    public String toString() {
        String base = title + " (" + category + ")  |  " + startTime + "  |  Seats left: " + seatsLeft;
        if ("DELETED".equalsIgnoreCase(status)) return "🗑️ [DELETED] " + base;
        return "✅ " + base;
    }
}
