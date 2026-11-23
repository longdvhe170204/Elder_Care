package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.belongings.internal.returns.service.RecipientConfirmationService;
import com.eldercare.backend.belongings.internal.returns.service.RecipientConfirmationSourceAccess;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import org.junit.jupiter.api.Test;

/**
 * BF-14 test web 2026-11-23: người đại diện mở được yêu cầu xác nhận người nhận khác từ thông báo (FR-017, FR-030; 009
 * {@code SourceRecordAccessPort}). Mở được đúng khi API cổng xem yêu cầu ({@code viewOnPortal}) cho xem.
 */
class RecipientConfirmationSourceAccessTest {

    private final RecipientConfirmationService confirmations = mock(RecipientConfirmationService.class);
    private final RecipientConfirmationSourceAccess access = new RecipientConfirmationSourceAccess(confirmations);

    @Test
    void answersForRecipientConfirmationSource() {
        assertThat(access.sourceRecordType()).isEqualTo("M12.RECIPIENT_CONFIRMATION");
    }

    @Test
    void representativeWhoCanViewOnPortal_canOpen() {
        when(confirmations.viewOnPortal(8L)).thenReturn(null);

        assertThat(access.canOpen("8")).isTrue();
    }

    @Test
    void notRepresentative_missingRecord_orBadId_cannotOpen() {
        when(confirmations.viewOnPortal(9L)).thenThrow(new PermissionDeniedException("Không phải người đại diện"));
        when(confirmations.viewOnPortal(10L)).thenThrow(new NotFoundException());

        assertThat(access.canOpen("9")).isFalse();
        assertThat(access.canOpen("10")).isFalse();
        assertThat(access.canOpen("abc")).isFalse();
    }
}
