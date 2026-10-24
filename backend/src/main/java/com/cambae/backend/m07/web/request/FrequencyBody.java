package com.cambae.backend.m07.web.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.List;

/**
 * Frequency configuration for periodic prescriptions (schema TanSuat in api-006.yaml).
 * Standardized English field names with backward-compatible Vietnamese aliases.
 */
public class FrequencyBody {

    @NotEmpty
    @JsonAlias({"kieu"})
    private String type; // TIMED_SLOTS | EVERY_N_HOURS | DAYS_OF_WEEK | CAC_MOC | MOI_N_GIO | NGAY_TRONG_TUAN

    @JsonAlias({"cacMoc"})
    private List<String> timedSlots;

    @JsonAlias({"nGio"})
    private Integer everyNHours;

    @JsonAlias({"mocDau"})
    private Instant firstSlot;

    @JsonAlias({"ngayTrongTuan"})
    private List<Integer> daysOfWeek;

    // --- English Getters & Setters ---

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

    // --- Backward-Compatible Vietnamese Getters & Setters ---

    public String getKieu() {
        return type;
    }

    public void setKieu(String kieu) {
        this.type = kieu;
    }

    public List<String> getCacMoc() {
        return timedSlots;
    }

    public void setCacMoc(List<String> cacMoc) {
        this.timedSlots = cacMoc;
    }

    public Integer getNGio() {
        return everyNHours;
    }

    public void setNGio(Integer nGio) {
        this.everyNHours = nGio;
    }

    public Instant getMocDau() {
        return firstSlot;
    }

    public void setMocDau(Instant mocDau) {
        this.firstSlot = mocDau;
    }

    public List<Integer> getNgayTrongTuan() {
        return daysOfWeek;
    }

    public void setNgayTrongTuan(List<Integer> ngayTrongTuan) {
        this.daysOfWeek = ngayTrongTuan;
    }
}
