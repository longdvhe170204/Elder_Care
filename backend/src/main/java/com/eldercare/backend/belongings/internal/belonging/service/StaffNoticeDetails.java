package com.eldercare.backend.belongings.internal.belonging.service;

import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.belongings.api.dto.BelongingView;
import com.eldercare.backend.belongings.api.events.BelongingNotice;
import com.eldercare.backend.belongings.internal.belonging.domain.BelongingNotices;
import com.eldercare.backend.belongings.internal.catalog.domain.BelongingCatalogs;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.IdentityView;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Chi tiết cho thông báo chỉ gửi nhân viên (test luồng BF-14 2026-11-23): người cao tuổi, đồ, vị trí lưu giữ, để người
 * nhận biết việc nào mà không phải mở bản ghi. Chỉ thêm vào nội dung đầy đủ qua {@link BelongingNotices#withStaffDetail};
 * thông báo có người thân giữ nguyên (SC-012).
 */
@Component
public class StaffNoticeDetails {

    private final ResidentApi residents;
    private final CatalogApi catalogs;

    StaffNoticeDetails(ResidentApi residents, CatalogApi catalogs) {
        this.residents = residents;
        this.catalogs = catalogs;
    }

    /** Thêm người cao tuổi của yêu cầu (nếu có) và các dòng chi tiết khác. */
    public BelongingNotice forStaff(BelongingNotice notice, String... more) {
        List<String> parts = new ArrayList<>();
        notice.notificationRequests().stream()
                .flatMap(r -> r.residentId().stream())
                .findFirst()
                .ifPresent(id -> parts.add(resident(id)));
        for (String line : more) {
            if (line != null && !line.isBlank()) {
                parts.add(line);
            }
        }
        return BelongingNotices.withStaffDetail(notice, String.join(". ", parts));
    }

    public String resident(long residentId) {
        IdentityView identity = residents.identity(residentId);
        return "Người cao tuổi: " + identity.fullName() + " (" + identity.profileCode() + ")";
    }

    public String belongings(Collection<BelongingView> items) {
        return items.isEmpty()
                ? null
                : "Đồ: " + items.stream().map(BelongingView::description).collect(Collectors.joining("; "));
    }

    public String location(long storageLocationId) {
        return "Vị trí: "
                + catalogs.require(BelongingCatalogs.STORAGE_LOCATION, storageLocationId)
                        .name();
    }
}
