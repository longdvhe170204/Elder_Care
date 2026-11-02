package com.eldercare.backend.incident.api;

import com.eldercare.backend.health.api.dto.VitalSignReadingView;
import com.eldercare.backend.incident.api.dto.FamilyIncidentView;
import java.time.Instant;
import java.util.List;

/** Nguồn sức khỏe cho đúng cặp người cao tuổi/người thân, mục 1.9. */
public interface FamilyPortalHealthApi {
    List<FamilyIncidentView> incidents(long residentId, long familyMemberId, Instant from, Instant to);

    List<VitalSignReadingView> vitalSigns(long residentId, long familyMemberId, Instant from, Instant to);
}
