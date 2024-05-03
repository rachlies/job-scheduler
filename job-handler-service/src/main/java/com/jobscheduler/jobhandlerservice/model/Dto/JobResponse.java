package com.jobscheduler.jobhandlerservice.model.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JobResponse(long id,
                          @JsonProperty("time_left")long timeLeft) {
}
