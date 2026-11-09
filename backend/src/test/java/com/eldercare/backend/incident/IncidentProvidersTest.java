package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.incident.internal.governance.service.IncidentFieldClassifications;
import com.eldercare.backend.incident.internal.governance.service.IncidentFilePermissionProviders;
import com.eldercare.backend.incident.internal.governance.service.IncidentScopeAssignments;
import org.junit.jupiter.api.Test;

class IncidentProvidersTest {

    @Test
    void FR_080_incidentFieldsClassifiedAsHealthOrContact() {
        var incident = IncidentFieldClassifications.incident();
        assertThat(incident.entityType()).isEqualTo("M05.INCIDENT");
        assertThat(incident.fieldGroups().get("description")).isEqualTo(InfoGroup.HEALTH);
        assertThat(incident.fieldGroups().get("emergencyCard")).isEqualTo(InfoGroup.HEALTH);
        assertThat(incident.fieldGroups().get("wishes")).isEqualTo(InfoGroup.HEALTH);
        assertThat(incident.fieldGroups().get("clinicalAssessment")).isEqualTo(InfoGroup.HEALTH);

        var progression = IncidentFieldClassifications.incidentProgression();
        assertThat(progression.entityType()).isEqualTo("M05.INCIDENT_PROGRESSION");
        assertThat(progression.fieldGroups().get("progression")).isEqualTo(InfoGroup.HEALTH);

        var alert = IncidentFieldClassifications.alert();
        assertThat(alert.entityType()).isEqualTo("M05.ALERT");
        assertThat(alert.fieldGroups().get("level")).isEqualTo(InfoGroup.HEALTH);

        var contactList = IncidentFieldClassifications.contactList();
        assertThat(contactList.entityType()).isEqualTo("M05.CONTACT_LIST");
        assertThat(contactList.fieldGroups().get("phoneNumber")).isEqualTo(InfoGroup.CONTACT);
    }

    @Test
    void FR_034_incidentScopeAssignment_assignsScopeByEntityType() {
        var incidentScope = IncidentScopeAssignments.incident();
        assertThat(incidentScope.entityType()).isEqualTo("M05.INCIDENT");
        var incidentTarget = incidentScope.assignScope(301L);
        assertThat(incidentTarget.type()).isEqualTo("M05.INCIDENT");
        assertThat(incidentTarget.id()).isEqualTo("301");

        var containmentScope = IncidentScopeAssignments.containment();
        assertThat(containmentScope.entityType()).isEqualTo("M05.CONTAINMENT");
        var containmentTarget = containmentScope.assignScope(401L);
        assertThat(containmentTarget.type()).isEqualTo("M05.CONTAINMENT");
        assertThat(containmentTarget.residentId()).isNull();
    }

    @Test
    void NFR_08_incidentFilePermissions_registeredForIncidentFile() {
        var incidentFile = IncidentFilePermissionProviders.incidentFile();
        assertThat(incidentFile.ownerType()).isEqualTo("M05.TEP_SU_CO");
        assertThat(incidentFile.canDownload(1L)).isTrue();
    }
}
