package com.eldercare.backend.incident.api.dto;

import java.util.Optional;

/** Tách phần chung và sức khỏe theo phạm vi đồng ý (S-19a). */
public record FamilyIncidentView(
        long incidentId, FamilyIncidentGeneralPart general, Optional<FamilyIncidentHealthPart> health) {}
