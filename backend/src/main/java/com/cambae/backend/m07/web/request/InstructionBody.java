package com.cambae.backend.m07.web.request;

import jakarta.validation.constraints.Size;

/**
 * Request body for updating prescription instruction notes (FR-014).
 */
public class InstructionBody {

    @Size(max = 2000)
    private String instructionNote;

    public String getInstructionNote() {
        return instructionNote;
    }

    public void setInstructionNote(String instructionNote) {
        this.instructionNote = instructionNote;
    }
}
