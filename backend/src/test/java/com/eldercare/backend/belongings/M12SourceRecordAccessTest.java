package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.belongings.api.dto.BelongingView;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingStore;
import com.eldercare.backend.belongings.internal.belonging.service.BelongingQueries;
import com.eldercare.backend.belongings.internal.belonging.service.BelongingSourceAccess;
import com.eldercare.backend.belongings.internal.belonging.service.HandoverSourceAccess;
import com.eldercare.backend.belongings.internal.inventorycheck.service.InventoryCheckService;
import com.eldercare.backend.belongings.internal.inventorycheck.service.InventorySourceAccess;
import com.eldercare.backend.belongings.internal.returns.service.RecipientConfirmationService;
import com.eldercare.backend.belongings.internal.returns.service.RecipientConfirmationSourceAccess;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

/**
 * Nhân viên mở bản ghi từ thông báo đồ gửi (test luồng BF-14 2026-11-23; 009 {@code SourceRecordAccessPort}): mở được
 * đúng khi API xem tương ứng của màn nhân viên cho xem.
 */
class M12SourceRecordAccessTest {

    @Test
    void handover_opensWhenHandoverRefIsReadable() {
        BelongingQueries queries = mock(BelongingQueries.class);
        HandoverSourceAccess access = new HandoverSourceAccess(queries);
        when(queries.handoverRef(5L)).thenReturn(null);
        when(queries.handoverRef(6L)).thenThrow(new NotFoundException());

        assertThat(access.sourceRecordType()).isEqualTo("M12.BELONGING_HANDOVER");
        assertThat(access.canOpen("5")).isTrue();
        assertThat(access.canOpen("6")).isFalse();
        assertThat(access.canOpen("x")).isFalse();
    }

    @Test
    void belonging_opensWhenOwnerIsInScope() {
        BelongingStore store = mock(BelongingStore.class);
        BelongingQueries queries = mock(BelongingQueries.class);
        BelongingView view = mock(BelongingView.class, Answers.RETURNS_DEEP_STUBS);
        when(view.residentId()).thenReturn(7L);
        when(store.find(3L)).thenReturn(Optional.of(view));
        when(queries.canView(3L, 7L)).thenReturn(true);
        BelongingSourceAccess access = new BelongingSourceAccess(store, queries);

        assertThat(access.sourceRecordType()).isEqualTo("M12.BELONGING");
        assertThat(access.canOpen("3")).isTrue();
        assertThat(access.canOpen("4")).isFalse();
    }

    @Test
    void inventorySources_openForInventoryReaders() {
        InventoryCheckService checks = mock(InventoryCheckService.class);
        when(checks.canRead()).thenReturn(true, false);

        assertThat(InventorySourceAccess.all(checks).stream()
                        .map(InventorySourceAccess::sourceRecordType)
                        .toList())
                .containsExactlyInAnyOrder("M12.INVENTORY_CHECK", "M12.INVENTORY_PERIOD", "M12.STORAGE_LOCATION");
        var period = InventorySourceAccess.all(checks).stream()
                .filter(a -> a.sourceRecordType().equals("M12.INVENTORY_PERIOD"))
                .findFirst()
                .orElseThrow();
        assertThat(period.canOpen("20758")).isTrue();
        assertThat(period.canOpen("20758")).isFalse();
    }

    /** FR-028: thông báo đồ còn ở tủ tầng cũ có nguồn là đồ gửi của người cao tuổi (mã người cao tuổi). */
    @Test
    void residentBelongings_opensWhenResidentIsInScope() {
        BelongingQueries queries = mock(BelongingQueries.class);
        when(queries.canViewResident(9004L)).thenReturn(true);
        var access =
                new com.eldercare.backend.belongings.internal.belonging.service.ResidentBelongingsSourceAccess(queries);

        assertThat(access.sourceRecordType()).isEqualTo("M12.BELONGING_RESIDENT");
        assertThat(access.canOpen("9004")).isTrue();
        assertThat(access.canOpen("9001")).isFalse();
        assertThat(access.canOpen("x")).isFalse();
    }

    @Test
    void recipientConfirmation_staffWhoMayListAll_canOpen() {
        RecipientConfirmationService confirmations = mock(RecipientConfirmationService.class);
        when(confirmations.viewOnPortal(9L)).thenThrow(new PermissionDeniedException("Không phải người thân"));
        when(confirmations.staffCanOpen(9L)).thenReturn(true);

        assertThat(new RecipientConfirmationSourceAccess(confirmations).canOpen("9"))
                .isTrue();
    }
}
