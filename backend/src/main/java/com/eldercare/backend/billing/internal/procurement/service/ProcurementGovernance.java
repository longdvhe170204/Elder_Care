package com.eldercare.backend.billing.internal.procurement.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.DutyApi;
import com.eldercare.backend.administration.account.api.spi.InvalidPermissionDataException;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.billing.internal.procurement.domain.ProcurementRequestRow;
import com.eldercare.backend.billing.internal.procurement.persistence.ProcurementRequestStore;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.file.FilePermissionProvider;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 019 T004 phần M11 (module-interfaces mục 3; research W-08, W-09): phạm vi {@code M11.PROCUREMENT_REQUEST} (yêu cầu thuốc
 * gắn người cao tuổi của dòng đầu; bộ phận không có trong {@link Target}, kiểm trong lệnh), lưu giữ không gắn hồ sơ theo
 * CFG-M15-04 tính từ lúc vào trạng thái cuối, quyền tải chứng từ theo quyền xem yêu cầu.
 */
@Configuration
public class ProcurementGovernance {

    @Bean
    ScopeAssignmentProvider procurementRequestScope(ProcurementRequestService requests, ProcurementRequestStore store) {
        return new ScopeAssignmentProvider() {
            @Override
            public String entityType() {
                return ProcurementRequestService.ENTITY;
            }

            @Override
            public Target assignScope(long id) {
                if (!store.exists(id))
                    throw new InvalidPermissionDataException(ProcurementRequestService.ENTITY, "MISSING_RECORD", id);
                return requests.target(id);
            }
        };
    }

    @Bean
    RetentionProvider procurementRequestRetention(ProcurementRequestStore store, RetentionDatabase database) {
        return new RetentionProvider() {
            @Override
            public String dataType() {
                return ProcurementRequestService.ENTITY;
            }

            @Override
            public RetentionKind retentionKind() {
                return new RetentionKind.NotResidentLinked();
            }

            @Override
            public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
                return store.lockedBefore(cutoff, batch).stream();
            }

            /** Tài sản (M03) trỏ tới yêu cầu qua id; M11 không đọc M03 (W-02), tài sản có hạn lưu giữ riêng. */
            @Override
            public Set<DataRef> referencesTo(long id) {
                return Set.of();
            }

            @Override
            public void delete(Collection<Long> ids) {
                store.delete(database.jdbc(), ids);
            }
        };
    }

    @Bean
    FilePermissionProvider procurementReceiptPermission(
            ProcurementRequestStore store, CurrentUser user, ObjectProvider<DutyApi> duties) {
        return new FilePermissionProvider() {
            @Override
            public String ownerType() {
                return ProcurementRequestService.RECEIPT;
            }

            /** NFR-08: người lập, Phụ trách bộ phận của yêu cầu, Kế toán, Quản lý viện. */
            @Override
            public boolean canDownload(long ownerId) {
                return store.find(ownerId)
                        .map(row -> visible(row, user, duties.getObject()))
                        .orElse(false);
            }
        };
    }

    static boolean visible(ProcurementRequestRow row, CurrentUser user, DutyApi duties) {
        if (user.roles().contains(Role.NURSING_HOME_MANAGER) || user.roles().contains(Role.ACCOUNTANT)) return true;
        var staff = user.staffId();
        if (staff.isEmpty()) return false;
        return row.createdByStaffId() == staff.get()
                || duties.departmentLead(row.departmentId())
                        .filter(lead -> Objects.equals(lead, staff.get()))
                        .isPresent();
    }
}
