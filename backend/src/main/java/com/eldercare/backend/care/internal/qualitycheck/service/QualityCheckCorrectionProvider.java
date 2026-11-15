package com.eldercare.backend.care.internal.qualitycheck.service;

import static com.eldercare.backend.care.internal.qualitycheck.domain.QualityCheckKinds.*;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.administration.correction.api.dto.CorrectionView;
import com.eldercare.backend.administration.correction.api.dto.CreationMethod;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Q-404: đính chính trong ca, lưu thêm vào chuỗi đính chính của 000 và giữ nguyên bản gốc. */
@Component
public class QualityCheckCorrectionProvider implements CorrectableRecordProvider {
    private final RecordEventApi records;
    private final ObjectProvider<QualityCheckService> quality;
    private final BusinessTime time;
    private final JsonMapper json;

    public QualityCheckCorrectionProvider(
            RecordEventApi records, ObjectProvider<QualityCheckService> quality, BusinessTime time, JsonMapper json) {
        this.records = records;
        this.quality = quality;
        this.time = time;
        this.json = json;
    }

    @Override
    public String recordType() {
        return ITEMS.code();
    }

    @Override
    public boolean isConfirmed(long id) {
        try {
            var row = row(id);
            return row.confirmedAt() != null && !OVERDUE.equals(row.content().status());
        } catch (NotFoundException e) {
            return false;
        }
    }

    @Override
    public long originalRecorder(long id) {
        var row = row(id);
        var inspector = row.content().inspectorId();
        if (inspector != null) return inspector;
        return row.actorId() == null ? 0 : row.actorId();
    }

    @Override
    public Optional<Long> floorArea(long id) {
        return Optional.of(row(id).content().floorAreaId());
    }

    @Override
    public boolean canRecord(CurrentUser user) {
        return user.staffId().isPresent();
    }

    @Override
    public boolean supports(CorrectionType type) {
        return type == CorrectionType.AMEND_CONTENT;
    }

    @Override
    public boolean allows(CurrentUser user, CreationMethod method, long id) {
        if (method == CreationMethod.C) return false;
        var item = row(id).content();
        var service = quality.getObject();
        service.requireAmendment(service.itemTarget(id), item, time.now());
        return true;
    }

    @Override
    public void validateContent(Object content) {
        var amendment = amendment(content);
        QualityCheckService.validateResult(amendment.result(), amendment.note());
        if (FAIL.equals(amendment.result())) QualityCheckService.requireRework(amendment.reworkTask());
    }

    @Override
    public Object originalContent(long id) {
        var item = row(id).content();
        var service = quality.getObject();
        service.requireHistoryView(service.itemTarget(id), item);
        return new Amendment(item.status(), item.note(), item.photoFileIds(), null);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void afterCorrection(CorrectionView correction) {
        var amendment = amendment(correction.correctedContent());
        if (FAIL.equals(amendment.result())) {
            long id = correction.originalRecordId();
            quality.getObject()
                    .reworkAfterAmendment(id, row(id).content(), amendment.reworkTask(), time.now(), correction.id());
        }
    }

    private RecordEvent<Item> row(long id) {
        return records.require(ITEMS, id);
    }

    private Amendment amendment(Object value) {
        if (!(value instanceof Map<?, ?>)) throw new InvalidValueException("Nội dung đính chính phải là đối tượng");
        try {
            return json.convertValue(value, Amendment.class);
        } catch (IllegalArgumentException | JacksonException e) {
            throw new InvalidValueException("Nội dung đính chính kết quả kiểm tra không hợp lệ");
        }
    }

    private record Amendment(
            String result, String note, List<Long> photoFileIds, QualityCheckService.Rework reworkTask) {}
}
