package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.spi.ResidentProfileApi;
import com.eldercare.backend.administration.account.api.spi.ResidentProfileView;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeResidentProfiles;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Bản cài {@link ResidentProfileApi} của M01 cho tài khoản người cao tuổi (002 T091; FR-067, FR-073): họ tên, Đang lưu
 * trú, trạng thái cuối; không có hồ sơ thì rỗng. Lấy bản thật bên cạnh bản giả {@code @Primary} của {@code FakeM15Config}.
 */
class ResidentProfileForAccountIT extends HealthTestBase {

    @Autowired
    List<ResidentProfileApi> implementations;

    private ResidentProfileApi real() {
        return implementations.stream()
                .filter(api -> !(api instanceof FakeResidentProfiles))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void FR_067_profileStatusAndName() {
        long id = newResident();
        String fullName = jdbc.queryForObject("SELECT full_name FROM resident WHERE id = ?", String.class, id);

        assertThat(real().profile(id)).contains(new ResidentProfileView(fullName, true, false));

        setStatus(id, "HOSPITALIZED");
        assertThat(real().profile(id)).contains(new ResidentProfileView(fullName, false, false));

        setStatus(id, "DECEASED");
        assertThat(real().profile(id)).contains(new ResidentProfileView(fullName, false, true));
    }

    @Test
    void unknownResident_empty() {
        assertThat(real().profile(TestUsers.nextId())).isEmpty();
    }
}
