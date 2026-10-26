package com.cambae.backend.m02.web.response;

import com.cambae.backend.m02.api.dto.PostDeathItemCode;
import com.cambae.backend.m02.api.dto.PostDeathItemStatus;
import com.cambae.backend.m02.api.dto.PostDeathTaskView;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** Danh sách việc bắt buộc sau qua đời (contracts/api-004.yaml, schema {@code PostDeathTaskList}; FR-071, FR-072). */
@Schema(name = "PostDeathTaskList")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PostDeathTaskListBody(List<ItemBody> items, Instant closedAt) {

    /** Một mục kèm căn cứ do module sở hữu cung cấp. */
    @Schema(name = "PostDeathTaskItem")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ItemBody(PostDeathItemCode code, PostDeathItemStatus status, String basis) {}

    public static PostDeathTaskListBody of(PostDeathTaskView view) {
        return new PostDeathTaskListBody(
                view.items().stream()
                        .map(i -> new ItemBody(i.code(), i.status(), i.basis()))
                        .toList(),
                view.closedAt());
    }
}
