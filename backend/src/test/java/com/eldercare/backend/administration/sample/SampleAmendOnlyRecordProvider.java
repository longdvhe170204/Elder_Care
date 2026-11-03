package com.eldercare.backend.administration.sample;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Provider mẫu chỉ hỗ trợ Sửa nội dung (như điểm danh bán trú của 005 khi VOID chờ chốt nghiệp vụ), dùng chung bảng
 * {@code sample_office_record} với {@link SampleOfficeRecordProvider}.
 */
@Component
public class SampleAmendOnlyRecordProvider implements CorrectableRecordProvider {

    public static final String TYPE = "M15.SAMPLE_AMEND_ONLY_RECORD";

    private final SampleOfficeRecordProvider office;

    public SampleAmendOnlyRecordProvider(SampleOfficeRecordProvider office) {
        this.office = office;
    }

    @Override
    public String recordType() {
        return TYPE;
    }

    @Override
    public boolean supports(CorrectionType type) {
        return type == CorrectionType.AMEND_CONTENT;
    }

    @Override
    public boolean isConfirmed(long id) {
        return office.isConfirmed(id);
    }

    @Override
    public long originalRecorder(long id) {
        return office.originalRecorder(id);
    }

    @Override
    public Optional<Long> floorArea(long id) {
        return office.floorArea(id);
    }

    @Override
    public boolean canRecord(CurrentUser user) {
        return office.canRecord(user);
    }

    @Override
    public void validateContent(Object content) {
        office.validateContent(content);
    }

    @Override
    public Object originalContent(long id) {
        return office.originalContent(id);
    }
}
