package com.eldercare.backend.administration.retention.api;

import com.eldercare.backend.administration.parameter.api.ParameterCode;

/**
 * Thời hạn lưu giữ của một loại dữ liệu (contracts/module-interfaces.md mục 1.7; FR-050): theo hồ sơ người cao tuổi
 * (CFG-M15-03), không gắn hồ sơ (CFG-M15-04), hoặc dài hơn theo một tham số do spec sở hữu quy định.
 */
public sealed interface RetentionKind {

    ParameterCode parameter();

    record ResidentRecord() implements RetentionKind {
        @Override
        public ParameterCode parameter() {
            return ParameterCode.CFG_M15_03;
        }
    }

    record NotResidentLinked() implements RetentionKind {
        @Override
        public ParameterCode parameter() {
            return ParameterCode.CFG_M15_04;
        }
    }

    record Longer(ParameterCode parameter) implements RetentionKind {}
}
