package com.eldercare.backend.care.internal.result.service;

import com.eldercare.backend.care.internal.catalog.domain.*;
import com.eldercare.backend.care.internal.catalog.persistence.CareCatalogStore;
import com.eldercare.backend.care.internal.result.domain.CareResultInput;
import com.eldercare.backend.care.internal.task.domain.CareTaskData;
import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.error.BusinessException;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** FR-034: đúng hình dạng và danh mục của từng loại; ngưỡng chỉ số do 007 kiểm. */
@Service
public class CareResultValidation {
    private final CareCatalogStore catalogs;

    public CareResultValidation(CareCatalogStore catalogs) {
        this.catalogs = catalogs;
    }

    public String catalogGroup(String type, String code) {
        return catalogs.list(CareCatalogKinds.RESULT_VALUES).stream()
                .filter(r -> r.status() == CatalogStatus.ACTIVE
                        && r.attributes().type().equals(type)
                        && r.attributes().code().equals(code))
                .findFirst()
                .map(r -> Objects.requireNonNullElse(r.attributes().ruleGroup(), ""))
                .orElseThrow(() -> invalid("Giá trị không thuộc danh mục đang hiệu lực"));
    }

    public void validate(CareTaskData type, CareResultInput input) {
        if (input.note() != null && input.note().length() > 2000) throw invalid("Ghi chú tối đa 2000 ký tự");
        if (input.numericValue() != null
                && (input.numericValue().stripTrailingZeros().scale() > 2
                        || input.numericValue().abs().compareTo(new BigDecimal("99999999.99")) > 0))
            throw invalid("Giá trị số vượt khả năng lưu trữ");
        switch (type.resultType()) {
            case "ML" -> {
                whole(input.numericValue(), false);
                shape(input, true, false, false);
                if (type.upperLimit() != null
                        && input.numericValue().compareTo(BigDecimal.valueOf(type.upperLimit())) > 0
                        && !input.upperLimitConfirmed())
                    throw new BusinessException(
                            HttpStatus.UNPROCESSABLE_ENTITY,
                            "CONFIRMATION_REQUIRED",
                            "Cần xác nhận giá trị vượt giới hạn hợp lý");
            }
            case "ITEM_QUANTITY" -> {
                whole(input.numericValue(), true);
                shape(input, true, false, false);
            }
            case "EATING" -> {
                code(input, Set.of("ATE_ALL", "MOST", "SOME", "NOT_EATEN", "SKIPPED"));
                shape(input, false, true, false);
            }
            case "MOVEMENT" -> {
                code(input, Set.of("INDEPENDENT", "ASSISTED", "UNABLE"));
                shape(input, false, true, false);
            }
            case "SLEEP" -> {
                whole(input.numericValue(), true);
                catalogGroup("SLEEP_QUALITY", input.codeValue());
                shape(input, true, true, false);
            }
            case "MOOD" -> {
                catalogGroup("MOOD", input.codeValue());
                shape(input, false, true, false);
            }
            case "BEHAVIOR" -> {
                shape(input, false, false, true);
                var occurred = input.jsonValue().get("occurred");
                if (!(occurred instanceof Boolean)) throw invalid("Cần ghi Có/Không có hành vi bất thường");
                if (Boolean.TRUE.equals(occurred)) {
                    var code = input.jsonValue().get("behaviorCode");
                    var description = input.jsonValue().get("description");
                    if (!(code instanceof String s)
                            || s.isBlank()
                            || !(description instanceof String d)
                            || d.isBlank()
                            || d.length() > 2000) throw invalid("Cần loại hành vi và mô tả");
                    catalogGroup("BEHAVIOR", (String) code);
                }
            }
            case "MEASURED_VALUE" -> {
                if (input.numericValue() == null) throw invalid("Cần giá trị đo");
                shape(input, true, false, false);
            }
            case "PERFORMED" -> shape(input, false, false, false);
            default -> throw invalid("Loại công việc không nhận kết quả tại checklist");
        }
    }

    private void whole(BigDecimal value, boolean allowZero) {
        if (value == null
                || value.stripTrailingZeros().scale() > 0
                || value.signum() < 0
                || (!allowZero && value.signum() == 0))
            throw invalid("Cần số nguyên " + (allowZero ? "không âm" : "dương"));
    }

    private void code(CareResultInput input, Set<String> allowed) {
        if (input.codeValue() == null || !allowed.contains(input.codeValue())) throw invalid("Mã kết quả không hợp lệ");
    }

    private void shape(CareResultInput input, boolean numeric, boolean code, boolean json) {
        if ((!numeric && input.numericValue() != null)
                || (!code && input.codeValue() != null)
                || (!json && !input.jsonValue().isEmpty())) throw invalid("Giá trị không đúng loại kết quả");
    }

    private BusinessException invalid(String message) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_VALUE", message);
    }
}
