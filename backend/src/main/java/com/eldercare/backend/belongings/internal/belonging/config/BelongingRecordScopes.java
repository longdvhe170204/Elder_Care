package com.eldercare.backend.belongings.internal.belonging.config;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.belongings.internal.consent.domain.SelfCustodyConsent;
import com.eldercare.backend.belongings.internal.inventorycheck.domain.InventoryCheck;
import com.eldercare.backend.belongings.internal.returns.domain.RecipientConfirmation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Phạm vi của các bản ghi {@code simple_record} của M12 ({@code GanPhamViProvider}; 013 T007, contracts mục 1.1, FR-029):
 * xác nhận người nhận khác và đồng ý cho tự giữ thuộc người cao tuổi của bản ghi; phiếu kiểm kê thuộc toàn viện (không
 * gắn người cao tuổi hay tầng).
 */
@Configuration
class BelongingRecordScopes {

    @Bean
    ScopeAssignmentProvider recipientConfirmationScope(SimpleRecordApi records) {
        return residentScope(records, RecipientConfirmation.KIND);
    }

    @Bean
    ScopeAssignmentProvider selfCustodyConsentScope(SimpleRecordApi records) {
        return residentScope(records, SelfCustodyConsent.KIND);
    }

    @Bean
    ScopeAssignmentProvider inventoryCheckScope(SimpleRecordApi records) {
        return new ScopeAssignmentProvider() {
            @Override
            public String entityType() {
                return InventoryCheck.KIND.code();
            }

            @Override
            public Target assignScope(long id) {
                records.require(InventoryCheck.KIND, id);
                return new Target(entityType(), Long.toString(id), null, null);
            }
        };
    }

    private static <T> ScopeAssignmentProvider residentScope(SimpleRecordApi records, SharedKind<T> kind) {
        return new ScopeAssignmentProvider() {
            @Override
            public String entityType() {
                return kind.code();
            }

            @Override
            public Target assignScope(long id) {
                return new Target(
                        entityType(),
                        Long.toString(id),
                        records.require(kind, id).residentId(),
                        null);
            }
        };
    }
}
