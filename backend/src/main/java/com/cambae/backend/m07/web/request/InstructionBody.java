package com.cambae.backend.m07.web.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Size;

/**
 * Request body for updating prescription instruction notes (FR-014).
 * Standardized English field names with backward-compatible Vietnamese aliases.
 */
public class InstructionBody {

    @Size(max = 2000)
    @JsonAlias({"ghiChuHuongDan", "ghiChu"})
    private String instructionNote;

    public String getInstructionNote() {
        return instructionNote;
    }

    public void setInstructionNote(String instructionNote) {
        this.instructionNote = instructionNote;
    }

    // --- Backward-Compatible Vietnamese Getters & Setters ---

    public String getGhiChuHuongDan() {
        return instructionNote;
    }

    public void setGhiChuHuongDan(String ghiChuHuongDan) {
        this.instructionNote = ghiChuHuongDan;
    }
}
