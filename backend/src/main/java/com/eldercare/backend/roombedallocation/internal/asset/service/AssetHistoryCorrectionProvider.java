package com.eldercare.backend.roombedallocation.internal.asset.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.AccountView;
import com.eldercare.backend.administration.account.api.spi.DutyApi;
import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.administration.correction.api.dto.CorrectionView;
import com.eldercare.backend.administration.correction.api.dto.CreationMethod;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetKinds;
import com.eldercare.backend.roombedallocation.internal.asset.persistence.AssetStore;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 019 T018 (loại {@code M03.ASSET_HISTORY}; chủ dự án chốt 2026-11-07; 000 FR-026, FR-029): đính chính lần bảo trì và hư
 * hỏng đã xác nhận ở {@code record_event}. Chỉ ghi chú: nội dung, đơn vị, chi phí của lần bảo trì; mô tả của hư hỏng; hoặc
 * Hủy ghi nhận. Không đổi trạng thái, vị trí, bộ phận, hạn hiện hành của tài sản và không báo module khác. Chỉ Phụ trách
 * bộ phận quản lý tài sản. Lịch sử vị trí, bộ phận ở {@code status_history} có id riêng nên chưa nằm trong loại này.
 */
@Component
class AssetHistoryCorrectionProvider implements CorrectableRecordProvider {
    static final String RECORD_TYPE = "M03.ASSET_HISTORY";
    private static final Set<String> MAINTENANCE_FIELDS = Set.of("content", "performedBy", "cost");
    private static final Set<String> DAMAGE_FIELDS = Set.of("description");
    private static final Set<String> ALL_FIELDS = Set.of("content", "performedBy", "cost", "description");

    private final RecordEventApi records;
    private final AssetStore assets;
    private final ObjectProvider<DutyApi> duties;
    private final ObjectProvider<AccountApi> accounts;

    AssetHistoryCorrectionProvider(
            RecordEventApi records,
            AssetStore assets,
            ObjectProvider<DutyApi> duties,
            ObjectProvider<AccountApi> accounts) {
        this.records = records;
        this.assets = assets;
        this.duties = duties;
        this.accounts = accounts;
    }

    @Override
    public String recordType() {
        return RECORD_TYPE;
    }

    @Override
    public boolean isConfirmed(long id) {
        return find(id).map(e -> e.confirmedAt() != null).orElse(false);
    }

    @Override
    public long originalRecorder(long id) {
        // record_event lưu mã nhân viên của người ghi; 000 so với mã tài khoản (FR-026 a)
        var staff = event(id).actorId();
        return staff == null
                ? 0
                : accounts.getObject()
                        .byStaff(staff)
                        .map(AccountView::accountId)
                        .orElse(0L);
    }

    @Override
    public Optional<Long> floorArea(long id) {
        return assets.find(event(id).parent().entityId()).map(a -> a.floorAreaId());
    }

    @Override
    public boolean canRecord(CurrentUser user) {
        return user.staffId().filter(duties.getObject()::isDepartmentLead).isPresent();
    }

    @Override
    public boolean allows(CurrentUser user, CreationMethod method, long id) {
        var asset = assets.find(event(id).parent().entityId()).orElseThrow(NotFoundException::new);
        return user.staffId()
                .flatMap(staff ->
                        duties.getObject().departmentLead(asset.departmentId()).filter(staff::equals))
                .isPresent();
    }

    @Override
    public void validateContent(Object correctedContent) {
        if (!(correctedContent instanceof Map<?, ?> map) || map.isEmpty() || !ALL_FIELDS.containsAll(map.keySet()))
            throw new InvalidValueException(
                    "Chỉ đính chính nội dung, đơn vị, chi phí của lần bảo trì hoặc mô tả hư hỏng");
        map.forEach((key, value) -> {
            if ("cost".equals(key)) {
                if (!(value instanceof Number n) || n.longValue() < 0 || n.doubleValue() != n.longValue())
                    throw new InvalidValueException("Chi phí là số đồng không âm");
            } else if (!(value instanceof String text) || text.isBlank() || text.length() > 2000)
                throw new InvalidValueException(key + " là chữ không trống, tối đa 2000 ký tự");
        });
    }

    @Override
    public Object originalContent(long id) {
        Map<String, Object> values = new LinkedHashMap<>();
        var e = event(id);
        if (e.content() instanceof AssetKinds.Maintenance m) {
            values.put("content", m.content());
            values.put("performedBy", m.performedBy());
            values.put("cost", m.cost());
        } else if (e.content() instanceof AssetKinds.Damage d) values.put("description", d.description());
        return values;
    }

    /** Trường phải hợp loại của bản gốc; kiểm ở đây vì {@link #validateContent} chưa biết bản gốc. */
    @Override
    public void afterCorrection(CorrectionView correction) {
        if (correction.type() == CorrectionType.VOID) return;
        var allowed = event(correction.originalRecordId()).content() instanceof AssetKinds.Maintenance
                ? MAINTENANCE_FIELDS
                : DAMAGE_FIELDS;
        if (!(correction.correctedContent() instanceof Map<?, ?> map) || !allowed.containsAll(map.keySet()))
            throw new InvalidValueException("Trường đính chính không thuộc loại bản ghi gốc");
    }

    private RecordEvent<?> event(long id) {
        return find(id).orElseThrow(NotFoundException::new);
    }

    private Optional<RecordEvent<?>> find(long id) {
        try {
            return Optional.of(records.require(AssetKinds.MAINTENANCE, id));
        } catch (NotFoundException maintenance) {
            try {
                return Optional.of(records.require(AssetKinds.DAMAGE, id));
            } catch (NotFoundException damage) {
                return Optional.empty();
            }
        }
    }
}
