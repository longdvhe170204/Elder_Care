package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.admission.api.PostDeathTaskApi;
import com.eldercare.backend.admission.api.dto.PostDeathItemCode;
import com.eldercare.backend.admission.api.dto.PostDeathItemStatus;
import com.eldercare.backend.medication.internal.family.domain.FamilyMedicationBatch;
import com.eldercare.backend.medication.internal.family.persistence.FamilyMedicationBatchRepository;
import com.eldercare.backend.medication.internal.handover.domain.MedicationHandover;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationHandoverRepository;
import com.eldercare.backend.medication.internal.status.domain.StoredMedicationClosed;
import com.eldercare.backend.medication.internal.status.service.StoredMedicationPostDeathItem;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * 004 T072 (004 FR-071; 006 FR-047, Q-64; 006 contracts/module-interfaces mục 1 dòng {@code MucViecSauQuaDoiPort}): mục
 * {@code STORED_MEDICATION} Chưa hoàn thành khi còn lô Chờ đối chiếu, Được sử dụng, Chỉ giữ hộ hoặc lần giao thuốc mang theo
 * chưa Đã nhận lại; Không áp dụng khi chưa từng có lô, lần giao nào; còn lại Hoàn thành. Hoàn trả, hủy, nhận lại thì 004 tính
 * lại danh sách.
 */
class T072_StoredMedicationPostDeathItemTest {

    private static final long RESIDENT = 9;

    private final FamilyMedicationBatchRepository batches = mock(FamilyMedicationBatchRepository.class);
    private final MedicationHandoverRepository handovers = mock(MedicationHandoverRepository.class);
    private final PostDeathTaskApi tasks = mock(PostDeathTaskApi.class);
    private final StoredMedicationPostDeathItem item;

    T072_StoredMedicationPostDeathItemTest() {
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("tasks", tasks);
        item = new StoredMedicationPostDeathItem(batches, handovers, beans.getBeanProvider(PostDeathTaskApi.class));
    }

    private void given(
            List<FamilyMedicationBatch> all, List<FamilyMedicationBatch> held, List<MedicationHandover> open) {
        when(batches.findByResidentId(RESIDENT)).thenReturn(all);
        when(batches.findByResidentIdAndStatusIn(anyLong(), any())).thenReturn(held);
        when(handovers.findByResidentId(RESIDENT)).thenReturn(List.of());
        when(handovers.findByResidentIdAndStatusIn(anyLong(), any())).thenReturn(open);
    }

    @Test
    void FR_071_statusFromHeldBatchesAndOpenHandovers() {
        FamilyMedicationBatch batch = mock(FamilyMedicationBatch.class);
        assertThat(item.code()).isEqualTo(PostDeathItemCode.STORED_MEDICATION);

        given(List.of(), List.of(), List.of());
        assertThat(item.status(RESIDENT).status()).isEqualTo(PostDeathItemStatus.NOT_APPLICABLE);

        given(List.of(batch), List.of(batch), List.of());
        assertThat(item.status(RESIDENT).status()).isEqualTo(PostDeathItemStatus.NOT_COMPLETED);

        given(List.of(batch), List.of(), List.of(mock(MedicationHandover.class)));
        assertThat(item.status(RESIDENT).status()).isEqualTo(PostDeathItemStatus.NOT_COMPLETED);

        given(List.of(batch), List.of(), List.of());
        assertThat(item.status(RESIDENT).status()).isEqualTo(PostDeathItemStatus.COMPLETED);
    }

    @Test
    void FR_071_closingABatchOrHandover_refreshesThePostDeathList() {
        item.on(new StoredMedicationClosed(RESIDENT));

        verify(tasks).refresh(RESIDENT);
    }
}
