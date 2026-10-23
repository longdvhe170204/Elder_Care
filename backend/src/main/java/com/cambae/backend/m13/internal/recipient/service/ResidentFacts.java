package com.cambae.backend.m13.internal.recipient.service;

import com.cambae.backend.m01.api.ConsentApi;
import com.cambae.backend.m01.api.ResidentApi;
import com.cambae.backend.m01.api.dto.ConsentView;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Thông tin của 001 mà việc xác định người nhận cần, đọc tại thời điểm gửi (research B-04): bản đồng ý Hiệu lực
 * ({@code BanDongYApi.hienHanh}, {@code baoGom}) và thời điểm qua đời ({@code NguoiCaoTuoiApi.thoiDiemTrangThaiCuoi}).
 */
@Service
public class ResidentFacts {

    private final ObjectProvider<ConsentApi> consents;
    private final ObjectProvider<ResidentApi> residents;

    ResidentFacts(ObjectProvider<ConsentApi> consents, ObjectProvider<ResidentApi> residents) {
        this.consents = consents;
        this.residents = residents;
    }

    /**
     * Người thân có quan hệ Hiệu lực thuộc phạm vi "nhận thông báo" (FR-014, Q-353, Q-357): mục chung của bản đồng ý Hiệu
     * lực đang bật; chưa có bản đồng ý thì không ai thuộc phạm vi.
     */
    public boolean notificationScope(long residentId) {
        ConsentApi api = consents.getIfAvailable();
        return api != null
                && api.current(residentId).map(ConsentView::notificationConsent).orElse(false);
    }

    /** Bản đồng ý Hiệu lực cho người thân này xem sức khỏe (FR-013, DBR-03). */
    public boolean healthConsent(long residentId, long familyMemberId) {
        ConsentApi api = consents.getIfAvailable();
        return api != null && api.includes(residentId, familyMemberId);
    }

    /** Thời điểm sang Qua đời; rỗng khi chưa qua đời hoặc ở trạng thái cuối khác (FR-015). */
    public Optional<Instant> deathAt(long residentId) {
        ResidentApi api = residents.getIfAvailable();
        if (api == null) {
            return Optional.empty();
        }
        return api.finalStatusAt(residentId).filter(at -> api.status(residentId) == ResidentStatus.DECEASED);
    }
}
