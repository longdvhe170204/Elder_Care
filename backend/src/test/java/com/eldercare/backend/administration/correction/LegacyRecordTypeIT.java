package com.eldercare.backend.administration.correction;

import static com.eldercare.backend.administration.sample.CorrectionFixtures.NURSE_A;
import static com.eldercare.backend.administration.sample.SampleLegacyRecordTypes.LEGACY_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.dto.CorrectionChainView;
import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.administration.correction.api.dto.CorrectionView;
import com.eldercare.backend.administration.sample.CorrectionFixtures;
import com.eldercare.backend.administration.sample.SampleFloorRecordProvider;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Bản ghi đã chuyển sang bảng gộp đọc được đính chính ghi với loại cũ (research R-15 mục 5, 000 T108 phần 2):
 * {@code LegacyRecordTypeSource} ánh xạ {@code M15.SAMPLE_RECORD_V0} sang {@code M15.SAMPLE_RECORD}.
 */
@Import(CorrectionFixtures.class)
class LegacyRecordTypeIT extends MySqlIntegrationTestBase {

    private static final String TYPE = SampleFloorRecordProvider.TYPE;

    @Autowired
    CorrectionApi corrections;

    @Autowired
    SwitchableUser user;

    @Autowired
    JdbcTemplate jdbc;

    private long record() {
        jdbc.update(
                "INSERT INTO sample_floor_record (occurred_at, actor_id, floor_area_id, note) VALUES (UTC_TIMESTAMP(6), ?, 2, 'gốc')",
                NURSE_A.accountId());
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /** Dòng đính chính ghi trước khi gộp bảng, với loại cũ (đính chính chỉ thêm nên không đổi loại được). */
    private static void legacyCorrection(long id, String type, String note, boolean voided) throws SQLException {
        try (Connection c = adminConnection();
                PreparedStatement insert = c.prepareStatement("INSERT INTO correction (record_type, original_record_id,"
                        + " type, corrected_content, reason, actor_id, creation_method, recorded_at, sequence_no)"
                        + " VALUES (?, ?, ?, ?, 'cũ', 10, 'A', UTC_TIMESTAMP(6) - INTERVAL 1 DAY, 1)");
                PreparedStatement chain = c.prepareStatement("INSERT INTO correction_chain (record_type,"
                        + " original_record_id, correction_count, voided) VALUES (?, ?, 1, ?)")) {
            insert.setString(1, LEGACY_TYPE);
            insert.setLong(2, id);
            insert.setString(3, type);
            insert.setString(4, note == null ? null : "{\"note\":\"" + note + "\"}");
            insert.executeUpdate();
            chain.setString(1, LEGACY_TYPE);
            chain.setLong(2, id);
            chain.setBoolean(3, voided);
            chain.executeUpdate();
        }
    }

    @Test
    void legacyCorrections_areReadAsCurrentType_andNewOnesContinueTheChain() throws SQLException {
        user.set(NURSE_A);
        long id = record();
        legacyCorrection(id, "AMEND_CONTENT", "cũ", false);

        assertThat(corrections.current(TYPE, List.of(id)).get(id).content()).isEqualTo(Map.of("note", "cũ"));

        corrections.create(TYPE, id, CorrectionType.AMEND_CONTENT, Map.of("note", "mới"), new Reason("sửa"));

        CorrectionChainView chain = corrections.chain(TYPE, id);
        assertThat(chain.corrections()).extracting(CorrectionView::sequenceNo).containsExactly(1, 2);
        assertThat(chain.corrections()).extracting(CorrectionView::recordType).containsOnly(TYPE);
        assertThat(chain.currentContent()).isEqualTo(Map.of("note", "mới"));
        assertThat(corrections.current(TYPE, List.of(id)).get(id).content()).isEqualTo(Map.of("note", "mới"));
    }

    @Test
    void legacyVoid_blocksNewCorrections() throws SQLException {
        user.set(NURSE_A);
        long id = record();
        legacyCorrection(id, "VOID", null, true);

        assertThat(corrections.current(TYPE, List.of(id)).get(id).voided()).isTrue();
        assertThatThrownBy(() -> corrections.create(
                        TYPE, id, CorrectionType.AMEND_CONTENT, Map.of("note", "mới"), new Reason("sửa")))
                .isInstanceOf(BusinessException.class);
    }
}
