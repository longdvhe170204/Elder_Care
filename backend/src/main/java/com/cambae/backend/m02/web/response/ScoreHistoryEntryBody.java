package com.cambae.backend.m02.web.response;

import com.cambae.backend.m02.internal.waitinglist.service.WaitingListService.HistoryView;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.function.LongFunction;

/**
 * Một dòng lịch sử điểm và điều chỉnh (contracts/api-004.yaml, schema {@code ScoreHistoryEntry}; FR-010, FR-011): điều
 * chỉnh kèm người đề nghị, người duyệt, lý do của yêu cầu phê duyệt.
 */
@Schema(name = "ScoreHistoryEntry")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScoreHistoryEntryBody(
        Instant at, int oldScore, int newScore, String cause, String proposedBy, String approver, String reason) {

    public static ScoreHistoryEntryBody of(HistoryView view, LongFunction<String> names) {
        return new ScoreHistoryEntryBody(
                view.row().at(),
                view.row().oldScore(),
                view.row().newScore(),
                view.row().cause().name(),
                view.proposedById() == null ? null : names.apply(view.proposedById()),
                view.approverId() == null ? null : names.apply(view.approverId()),
                view.reason());
    }
}
