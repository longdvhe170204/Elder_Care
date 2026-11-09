package com.eldercare.backend.belongings.internal.inventorycheck.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.administration.correction.api.dto.CorrectionView;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.belongings.internal.inventorycheck.domain.InventoryCheck;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Đính chính phiếu kiểm kê đã Hoàn thành ({@code DinhChinhDuocProvider} {@code M12.INVENTORY_CHECK}; 013 T025, FR-031a,
 * research K-05): phiếu đã Hoàn thành là nhóm 3 như bản ghi bàn giao, không gắn tầng (cách A người hoàn thành, cách C
 * yêu cầu phê duyệt), chỉ Hành chính ghi. Chỉ sửa ghi chú từng dòng ({@code lineNotes}, khóa là mã đồ); kết quả và bản
 * ghi bàn giao phiếu đã sinh đổi bằng đính chính của chính bản ghi bàn giao đó, nên Hủy ghi nhận không hỗ trợ.
 */
@Component
class InventoryCheckCorrection implements CorrectableRecordProvider {

    private static final String LINE_NOTES = "lineNotes";

    private final SimpleRecordApi records;

    InventoryCheckCorrection(SimpleRecordApi records) {
        this.records = records;
    }

    @Override
    public String recordType() {
        return InventoryCheckService.RECORD_TYPE;
    }

    @Override
    public boolean isConfirmed(long id) {
        return completed(id).isPresent();
    }

    @Override
    public long originalRecorder(long id) {
        return completed(id).orElseThrow().content().completedBy();
    }

    @Override
    public Optional<Long> floorArea(long id) {
        return Optional.empty();
    }

    @Override
    public boolean canRecord(CurrentUser user) {
        return user.roles().contains(Role.OFFICE_STAFF);
    }

    @Override
    public boolean supports(CorrectionType type) {
        return type == CorrectionType.AMEND_CONTENT;
    }

    @Override
    public void validateContent(Object correctedContent) {
        if (!(correctedContent instanceof Map<?, ?> map)
                || map.size() != 1
                || !(map.get(LINE_NOTES) instanceof Map<?, ?> notes)
                || notes.isEmpty()) {
            throw new InvalidValueException("Đính chính phiếu kiểm kê chỉ sửa " + LINE_NOTES + " (ghi chú từng dòng)");
        }
        for (Map.Entry<?, ?> note : notes.entrySet()) {
            try {
                Long.parseLong(String.valueOf(note.getKey()));
            } catch (NumberFormatException e) {
                throw new InvalidValueException(LINE_NOTES + " có khóa là mã đồ gửi");
            }
            if (!(note.getValue() instanceof String)) {
                throw new InvalidValueException("Ghi chú của đồ " + note.getKey() + " phải là chuỗi");
            }
        }
    }

    @Override
    public Object originalContent(long id) {
        Map<String, String> notes = new LinkedHashMap<>();
        for (InventoryCheck.Line line : completed(id).orElseThrow().content().lines()) {
            notes.put(Long.toString(line.belongingId()), line.note());
        }
        return Map.of(LINE_NOTES, notes);
    }

    /** Mã đồ trong nội dung đúng phải thuộc phiếu; sai thì cả đính chính rollback. */
    @Override
    public void afterCorrection(CorrectionView correction) {
        if (!(correction.correctedContent() instanceof Map<?, ?> content)
                || !(content.get(LINE_NOTES) instanceof Map<?, ?> notes)) {
            return;
        }
        InventoryCheck check =
                completed(correction.originalRecordId()).orElseThrow().content();
        for (Object key : notes.keySet()) {
            long belongingId = Long.parseLong(String.valueOf(key));
            if (check.lines().stream().noneMatch(line -> line.belongingId() == belongingId)) {
                throw new InvalidValueException("Đồ " + belongingId + " không có trong phiếu kiểm kê");
            }
        }
    }

    private Optional<SimpleRecord<InventoryCheck>> completed(long id) {
        try {
            SimpleRecord<InventoryCheck> record = records.require(InventoryCheck.KIND, id);
            return InventoryCheck.COMPLETED.equals(record.status()) ? Optional.of(record) : Optional.empty();
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }
}
