package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.api.ResidencyEndingReadApi;
import com.cambae.backend.m02.api.dto.EndCase;
import com.cambae.backend.m02.api.dto.ResidencyEndingView;
import com.cambae.backend.m02.api.dto.SettlementInfoView;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 004 T106, T107 (US7; Q-379; 017 FR-022, FR-034; quickstart Q42): {@code ResidencyEndingReadApi} (tên logic {@code
 * HoSoKetThucDocApi}) cho 017: hồ sơ Đang chuẩn bị, thông tin bảng quyết toán và ngoại lệ ý tiền (h) đã duyệt. Trường hợp
 * qua đời thuộc user story 8.
 */
@AutoConfigureMockMvc
@WithMockUser
class ResidencyEndingReadApiIT extends EndingTestBase {

    @Autowired
    ResidencyEndingReadApi api;

    @Test
    void preparing_isEmptyWithoutAnEnding_thenShowsThePreparingOne_andEmptyAfterCancel() throws Exception {
        long residentId = inResidence();
        assertThat(api.preparing(residentId)).isEmpty();
        LocalDate expected = d0.plusDays(4);
        long id = createEnding(residentId, expected);

        Optional<ResidencyEndingView> view = api.preparing(residentId);

        assertThat(view).isPresent();
        assertThat(view.get().id()).isEqualTo(id);
        assertThat(view.get().endCase()).isEqualTo(EndCase.DISCHARGE);
        assertThat(view.get().expectedEndDate()).isEqualTo(expected);
        assertThat(view.get().overdueSince()).isNull();
        cancelRaw(office, id, "Đổi ý").andExpect(status().isOk());
        assertThat(api.preparing(residentId)).isEmpty();
    }

    @Test
    void settlementInfo_carriesContractAndDates_andIsEmptyWithoutAPreparingEnding() throws Exception {
        long residentId = inResidence();
        assertThat(api.settlementInfo(residentId)).isEmpty();
        LocalDate expected = d0.plusDays(4);
        long id = createEnding(residentId, expected);
        long contractId = jdbc.queryForObject(
                "SELECT id FROM contract WHERE resident_id = ? AND status = 'ACTIVE'", Long.class, residentId);

        Optional<SettlementInfoView> info = api.settlementInfo(residentId);

        assertThat(info).isPresent();
        assertThat(info.get().residenceType()).isEqualTo(ResidenceType.LONG_TERM_RESIDENTIAL);
        assertThat(info.get().contractId()).isEqualTo(contractId);
        assertThat(info.get().startDate()).isEqualTo(d0);
        assertThat(info.get().expectedEndDate()).isEqualTo(expected);
        assertThat(info.get().endCase()).isEqualTo(EndCase.DISCHARGE);
        assertThat(info.get().deathDate()).isNull();
        cancelRaw(office, id, "Đổi ý").andExpect(status().isOk());
        assertThat(api.settlementInfo(residentId)).isEmpty();
    }

    @Test
    void hasApprovedSettlementException_isTrueOnlyForTheMoneyExceptionOfAPreparingEnding() throws Exception {
        long residentId = inResidence();
        long id = createEnding(residentId, d0.plusDays(4));
        assertThat(api.hasApprovedSettlementException(residentId)).isFalse();

        approvedException(id, D_ALERTS);
        assertThat(api.hasApprovedSettlementException(residentId))
                .as("ngoại lệ của điều kiện khác")
                .isFalse();

        approvedException(id, H_SETTLEMENT);
        assertThat(api.hasApprovedSettlementException(residentId)).isTrue();

        cancelRaw(office, id, "Đổi ý").andExpect(status().isOk());
        assertThat(api.hasApprovedSettlementException(residentId))
                .as("hồ sơ đã hủy thì ngoại lệ không còn căn cứ")
                .isFalse();
    }
}
