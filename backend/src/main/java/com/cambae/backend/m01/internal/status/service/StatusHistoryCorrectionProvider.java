package com.cambae.backend.m01.internal.status.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.m01.api.ResidentApi;
import com.cambae.backend.m01.internal.status.domain.ResidentStatusHistory;
import com.cambae.backend.m01.internal.status.persistence.ResidentStatusHistoryRepository;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.correction.api.CreationMethod;
import com.cambae.backend.m15.correction.api.spi.CorrectableRecordProvider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Đính chính dòng lịch sử trạng thái (001 T050; FR-049a; module-interfaces mục 1.9). Lịch sử trạng thái là dữ liệu nhóm 3,
 * chỉ thêm; chỉ lý do của dòng đính chính được (trạng thái, lệnh, thời điểm, người thực hiện, căn cứ là sự kiện đã xảy
 * ra). Không gắn tầng nên đi cách A (người ghi gốc) hoặc C (yêu cầu phê duyệt); trên hồ sơ trạng thái cuối chỉ cách C.
 * Đính chính không đổi trạng thái, không thêm dòng lịch sử, không báo đánh giá lại.
 */
@Component
class StatusHistoryCorrectionProvider implements CorrectableRecordProvider {

    static final String REASON = "reason";
    private static final int MAX_REASON = 2000;
    // Vai trò có lệnh trạng thái: Hành chính, Bác sĩ, Điều dưỡng; Quản lý viện duyệt cách C
    private static final Set<Role> RECORDERS =
            Set.of(Role.NURSING_HOME_MANAGER, Role.DOCTOR, Role.NURSING_STAFF, Role.OFFICE_STAFF);

    private final ResidentStatusHistoryRepository histories;
    private final ResidentApi residents;

    StatusHistoryCorrectionProvider(ResidentStatusHistoryRepository histories, ResidentApi residents) {
        this.histories = histories;
        this.residents = residents;
    }

    @Override
    public String recordType() {
        return ResidentStatusService.HISTORY_RECORD_TYPE;
    }

    @Override
    public boolean isConfirmed(long id) {
        return histories.existsById(id);
    }

    @Override
    public long originalRecorder(long id) {
        return find(id).getPerformedBy();
    }

    @Override
    public Optional<Long> floorArea(long id) {
        return Optional.empty();
    }

    @Override
    public Optional<Long> residentId(long id) {
        return histories.findById(id).map(ResidentStatusHistory::getResidentId);
    }

    @Override
    public boolean canRecord(CurrentUser user) {
        return user.roles().stream().anyMatch(RECORDERS::contains);
    }

    @Override
    public void validateContent(Object correctedContent) {
        if (!(correctedContent instanceof Map<?, ?> map) || !map.keySet().equals(Set.of(REASON))) {
            throw new InvalidValueException("Chỉ đính chính được lý do của dòng lịch sử (trường reason)");
        }
        Object reason = map.get(REASON);
        if (!(reason instanceof String text) || text.isBlank() || text.strip().length() > MAX_REASON) {
            throw new InvalidValueException("Lý do là bắt buộc, tối đa " + MAX_REASON + " ký tự");
        }
    }

    @Override
    public Object originalContent(long id) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(REASON, find(id).getReason());
        return values;
    }

    @Override
    public boolean allows(CurrentUser user, CreationMethod method, long id) {
        return method == CreationMethod.C || !residents.isFinalStatus(find(id).getResidentId());
    }

    private ResidentStatusHistory find(long id) {
        return histories.findById(id).orElseThrow(NotFoundException::new);
    }
}
