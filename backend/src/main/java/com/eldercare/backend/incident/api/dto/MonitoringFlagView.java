package com.eldercare.backend.incident.api.dto;
/** Dấu hiện hành theo người cao tuổi (mục 1.7). */
public record MonitoringFlagView(boolean suspectedInfection, boolean contact, boolean postFall, boolean critical) {}
