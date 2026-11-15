package com.eldercare.backend.billing.internal.purchase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.admission.api.UnitPriceApi;
import com.eldercare.backend.admission.api.dto.BillingPeriod;
import com.eldercare.backend.admission.api.dto.ServiceType;
import com.eldercare.backend.admission.api.dto.ServiceView;
import com.eldercare.backend.billing.internal.purchase.domain.PurchaseConsent;
import com.eldercare.backend.billing.internal.purchase.domain.PurchaseDetails;
import com.eldercare.backend.medication.api.PrescriptionApi;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.api.dto.MedicationSummary;
import com.eldercare.backend.medication.api.dto.PrescriptionView;
import com.eldercare.backend.medication.api.spi.PurchaseAssistanceView;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Q-139 (A duyệt Q5, 2026-11-10): thuốc mua hộ là đề nghị Đã mua có vật phẩm danh mục loại thuốc nguồn viện. */
class PurchasedMedicationsTest {
    private final SimpleRecordApi records = mock(SimpleRecordApi.class);
    private final UnitPriceApi catalog = mock(UnitPriceApi.class);
    private final PrescriptionApi prescriptions = mock(PrescriptionApi.class);
    private final PurchasedMedications port = new PurchasedMedications(records, catalog, prescriptions);

    private void request(String status, Long item) {
        var content = new PurchaseDetails(
                item,
                "Thuốc theo đơn",
                new BigDecimal("2"),
                "Bác sĩ kê",
                "STAFF",
                5,
                PurchaseConsent.draft(100_000),
                1,
                null,
                false,
                100_000L,
                LocalDate.of(2026, 10, 5),
                9L,
                77L);
        when(records.require(PurchaseDetails.KIND, 1))
                .thenReturn(
                        new SimpleRecord<>(1, 42L, null, status, content, null, null, null, null, Instant.EPOCH, 5, 0));
    }

    private void medicineItem() {
        when(catalog.service(8))
                .thenReturn(Optional.of(new ServiceView(
                        8,
                        ServiceType.FACILITY_SUPPLIED_MEDICATION,
                        "Thuốc viện",
                        "viên",
                        BillingPeriod.UNIT,
                        true,
                        31L)));
        when(prescriptions.history(42))
                .thenReturn(List.of(new PrescriptionView(
                        3,
                        42,
                        "DINH_KY",
                        "HIEU_LUC",
                        MedicationSource.FACILITY,
                        LocalDate.of(2026, 10, 1),
                        null,
                        new MedicationSummary(31, "Paracetamol", "Paracetamol", "500mg", "viên"),
                        "1 viên",
                        null)));
    }

    @Test
    void Q_139_purchasedMedication_returnsNameQuantityAndPurchaser() {
        request("PURCHASED", 8L);
        medicineItem();

        assertThat(port.purchasedDetails(1, 42))
                .contains(new PurchaseAssistanceView(1, 42, "Paracetamol", new BigDecimal("2"), 77));
    }

    @Test
    void Q_139_notPurchased_otherResident_orNotMedication_isEmpty() {
        medicineItem();
        request("PURCHASE_ALLOWED", 8L);
        assertThat(port.purchasedDetails(1, 42)).isEmpty();
        request("PURCHASED", 8L);
        assertThat(port.purchasedDetails(1, 43)).isEmpty();
        request("PURCHASED", null);
        assertThat(port.purchasedDetails(1, 42)).isEmpty();
    }
}
