package com.eldercare.backend.medication.internal.governance.persistence;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.resident.api.ResidentApi;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/** Các provider lưu giữ dữ liệu thuốc (data-model 006 mục 13; F-06), dựng bằng {@link MedicationRetentionProvider}. */
@Configuration
class MedicationRetentionBeans {

    /**
     * Lưu giữ dữ liệu thuốc theo hồ sơ (data-model 006 mục 13; F-06). Loại tách riêng theo loại đối tượng của dòng bảng
     * dùng chung đi kèm (xem lại liều, xem lại PRN, lần giao thuốc gia đình gửi nhận).
     */
    @Bean
    public RetentionProvider medicationPrescriptionRetention(
            JdbcTemplate jdbc, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return retention(
                new RetentionSpec(
                        MedicationRetentionProvider.TYPE,
                        "prescription",
                        "",
                        List.of(
                                new RetentionChild(
                                        "M07.MEDICATION_DOSE",
                                        "SELECT id FROM medication_dose WHERE prescription_id = ?"),
                                new RetentionChild(
                                        MedicationRetentionProvider.TYPE,
                                        "SELECT id FROM prescription WHERE replaces_id = ?"),
                                new RetentionChild(
                                        "M07.PRN_ADMINISTRATION",
                                        "SELECT id FROM dose_result WHERE kind = 'PRN' AND prescription_id = ?"))),
                jdbc,
                database,
                residents);
    }

    @Bean
    public RetentionProvider medicationDoseRetention(
            JdbcTemplate jdbc, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return retention(
                new RetentionSpec(
                        "M07.MEDICATION_DOSE",
                        "medication_dose",
                        "",
                        List.of(new RetentionChild("M07.DOSE_RESULT", "SELECT id FROM dose_result WHERE dose_id = ?"))),
                jdbc,
                database,
                residents);
    }

    @Bean
    public RetentionProvider medicationDoseResultRetention(
            JdbcTemplate jdbc, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return retention(
                new RetentionSpec("M07.DOSE_RESULT", "dose_result", "kind = 'SCHEDULED'", List.of()),
                jdbc,
                database,
                residents);
    }

    @Bean
    public RetentionProvider medicationPrnRetention(
            JdbcTemplate jdbc, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return retention(
                new RetentionSpec("M07.PRN_ADMINISTRATION", "dose_result", "kind = 'PRN'", List.of()),
                jdbc,
                database,
                residents);
    }

    @Bean
    public RetentionProvider medicationReconciliationRetention(
            JdbcTemplate jdbc, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return retention(
                new RetentionSpec("M07.RECONCILIATION", "medication_reconciliation", "", List.of()),
                jdbc,
                database,
                residents);
    }

    @Bean
    public RetentionProvider medicationFamilyMedicationBatchRetention(
            JdbcTemplate jdbc, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return retention(
                new RetentionSpec("M07.FAMILY_MEDICATION_BATCH", "family_medication_batch", "", List.of()),
                jdbc,
                database,
                residents);
    }

    /** Lần giao, nhận lại, lệnh không mang thuốc; lần nhận lại trỏ lần giao nên xóa trước. */
    @Bean
    public RetentionProvider medicationHandoverRetention(
            JdbcTemplate jdbc, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return retention(
                new RetentionSpec(
                        "M07.MEDICATION_HANDOVER",
                        "medication_handover",
                        "kind <> 'SUPPLIED_INTAKE'",
                        List.of(new RetentionChild(
                                "M07.MEDICATION_HANDOVER",
                                "SELECT id FROM medication_handover WHERE handover_id = ?"))),
                jdbc,
                database,
                residents);
    }

    @Bean
    public RetentionProvider medicationSuppliedIntakeRetention(
            JdbcTemplate jdbc, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return retention(
                new RetentionSpec(
                        "M07.SUPPLIED_MEDICATION_INTAKE", "medication_handover", "kind = 'SUPPLIED_INTAKE'", List.of()),
                jdbc,
                database,
                residents);
    }

    private static RetentionProvider retention(
            RetentionSpec spec, JdbcTemplate jdbc, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return new MedicationRetentionProvider(spec, jdbc, database, residents);
    }
}
