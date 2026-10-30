package com.eldercare.backend.administration.sample;

import com.eldercare.backend.administration.shared.api.spi.LegacyRecordTypeSource;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Loại bản ghi cũ mẫu: dòng đính chính, nhật ký ghi trước khi gộp bảng đọc như {@code M15.SAMPLE_RECORD} (R-15 mục 5). */
@Component
public class SampleLegacyRecordTypes implements LegacyRecordTypeSource {

    public static final String LEGACY_TYPE = "M15.SAMPLE_RECORD_V0";

    @Override
    public Map<String, String> legacyRecordTypes() {
        return Map.of(LEGACY_TYPE, SampleFloorRecordProvider.TYPE);
    }
}
