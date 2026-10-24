package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.m01.api.ResidentApi;
import com.cambae.backend.m01.api.dto.Gender;
import com.cambae.backend.m01.api.dto.IdentityView;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 001 T021 (contracts/module-interfaces.md mục 1.1; FR-044a, FR-046, FR-049): bản thật của {@code ResidentApi} cho 002 và
 * các module khác: trạng thái, nhận dạng, thời điểm sang trạng thái cuối, giới tính, danh sách theo trạng thái.
 */
class ResidentApiIT extends ResidentProfileTestBase {

    @Autowired
    ResidentApi residents;

    @Test
    void status_andIdentity_ofANewProfile() {
        var profile = createAsOffice(noDocument(uniqueName())).profile();

        assertThat(residents.status(profile.id())).isEqualTo(ResidentStatus.ADMISSION_IN_PROGRESS);
        IdentityView identity = residents.identity(profile.id());
        assertThat(identity.residentId()).isEqualTo(profile.id());
        assertThat(identity.profileCode()).isEqualTo(profile.profileCode());
        assertThat(identity.fullName()).isEqualTo(profile.personalInfo().fullName());
        assertThat(identity.photoFileId()).isEmpty();
    }

    @Test
    void unknownProfile_isNotFound() {
        assertThatThrownBy(() -> residents.status(-1)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> residents.identity(-1)).isInstanceOf(NotFoundException.class);
        assertThat(residents.gender(-1)).isEmpty();
        assertThat(residents.finalStatusAt(-1)).isEmpty();
    }

    @Test
    void finalStatus_isReportedWithTheTimeItWasReached() {
        long open = createAsOffice(noDocument(uniqueName())).profile().id();
        long ended = createAsOffice(noDocument(uniqueName())).profile().id();
        setStatus(ended, "RESIDENCY_ENDED");

        assertThat(residents.isFinalStatus(open)).isFalse();
        assertThat(residents.finalStatusAt(open)).isEmpty();
        assertThat(residents.isFinalStatus(ended)).isTrue();
        assertThat(residents.finalStatusAt(ended)).isPresent();
    }

    @Test
    void gender_ofAProfile() {
        long id = createAsOffice(personal(uniqueName(), BORN, Gender.FEMALE, null, null))
                .profile()
                .id();

        assertThat(residents.gender(id)).isEqualTo(Optional.of(Gender.FEMALE));
    }

    @Test
    void byStatus_listsProfilesInThoseStatuses() {
        long admission = createAsOffice(noDocument(uniqueName())).profile().id();
        long inResidence = createAsOffice(noDocument(uniqueName())).profile().id();
        setStatus(inResidence, "IN_RESIDENCE");

        assertThat(residents.byStatus(Set.of(ResidentStatus.IN_RESIDENCE)))
                .contains(inResidence)
                .doesNotContain(admission);
        assertThat(residents.byStatus(Set.of())).isEmpty();
    }
}
