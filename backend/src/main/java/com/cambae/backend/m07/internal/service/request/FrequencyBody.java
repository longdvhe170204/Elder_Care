package com.cambae.backend.m07.internal.service.request;

import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.List;

/**
 * Frequency configuration for periodic prescriptions (schema TanSuat in api-006.yaml).
 */
public class FrequencyBody {

    @NotEmpty
    private String type; // TIMED_SLOTS | EVERY_N_HOURS | DAYS_OF_WEEK

    private List<String> timedSlots;

    private Integer everyNHours;

    private Instant firstSlot;

    private List<Integer> daysOfWeek;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getTimedSlots() {
        return timedSlots;
    }

    public void setTimedSlots(List<String> timedSlots) {
        this.timedSlots = timedSlots;
    }

    public Integer getEveryNHours() {
        return everyNHours;
    }

    public void setEveryNHours(Integer everyNHours) {
        this.everyNHours = everyNHours;
    }

    public Instant getFirstSlot() {
        return firstSlot;
    }

    public void setFirstSlot(Instant firstSlot) {
        this.firstSlot = firstSlot;
    }

    public List<Integer> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(List<Integer> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }
}
