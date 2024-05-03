package com.jobscheduler.jobhandlerservice.model.Dto;

public record JobRequest(int hours, int minutes, int seconds, String url) {

    public JobRequest {
        if (hours < 0 || minutes < 0 || seconds < 0) {
            throw new IllegalArgumentException("Invalid JobRequest: hours, minutes, seconds must be non-negative.");
        }
        if (hours + minutes + seconds <= 0) {
            throw new IllegalArgumentException("Invalid JobRequest: total time cannot be zero or negative.");
        }
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Invalid JobRequest: URL cannot be empty.");
        }
    }
}