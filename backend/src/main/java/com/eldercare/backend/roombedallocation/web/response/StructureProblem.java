package com.eldercare.backend.roombedallocation.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Chỉ mô tả Problem của hợp đồng 003; thân lỗi thật do handler chung dựng. */
@Schema(name = "M03StructureProblem")
public record StructureProblem(
        String type,
        String title,
        Integer status,
        String detail,
        String errorCode,
        List<Condition> unmetConditions,
        List<Blocker> blockingObjects,
        Object currentValue) {
    public record Condition(String code, String description, String basis) {}

    public record Blocker(String type, Long id, String label) {}
}
