package com.cambae.backend.m02.internal.waitinglist.service;

import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m02.internal.waitinglist.domain.WaitingListEntry;
import com.cambae.backend.m02.internal.waitinglist.persistence.WaitingListStore;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.approval.api.ApprovalRequestView;
import com.cambae.backend.m15.approval.api.ResultRef;
import com.cambae.backend.m15.approval.api.spi.RequestTypeHandler;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Loại yêu cầu {@code M02.SCORE_ADJUSTMENT} (FR-011, SC-004; research T-07): Hành chính đề nghị cộng hoặc trừ điểm cho một hồ
 * sơ chờ, Quản lý viện duyệt; áp dụng thì cộng vào tổng điều chỉnh và ghi dòng lịch sử điểm. Hồ sơ Đã liên hệ vẫn giữ giường
 * (US2 #11). Nội dung {@code {entryId, points}}; lý do là lý do của yêu cầu.
 */
@Component
class ScoreAdjustmentRequestHandler implements RequestTypeHandler {

    static final String TYPE = "M02.SCORE_ADJUSTMENT";

    private final WaitingListStore store;
    private final ScoreRecalculation scores;
    private final BusinessTime time;

    ScoreAdjustmentRequestHandler(WaitingListStore store, ScoreRecalculation scores, BusinessTime time) {
        this.store = store;
        this.scores = scores;
        this.time = time;
    }

    @Override
    public String typeCode() {
        return TYPE;
    }

    @Override
    public void validateContent(JsonNode content) {
        entryId(content);
        if (points(content) == 0) {
            throw new InvalidValueException("points phải khác 0");
        }
    }

    /** Hành chính lập (Phụ lục 27 dòng "Danh sách chờ, điểm ưu tiên", ô T). */
    @Override
    public PermissionKey createPermission() {
        return new PermissionKey(Function.WAITING_LIST, Operation.UPDATE);
    }

    /** Quản lý viện duyệt (ô D). */
    @Override
    public PermissionKey approvePermission() {
        return new PermissionKey(Function.WAITING_LIST, Operation.APPROVE);
    }

    @Override
    public Target scope(JsonNode content) {
        long entryId = entryId(content);
        return store.find(entryId)
                .map(entry -> WaitingListService.target(entry.id(), entry.residentId()))
                .orElse(new Target(WaitingListService.ENTITY_TYPE, String.valueOf(entryId), null, null));
    }

    @Override
    public List<UnmetCondition> checkApplyConditions(ApprovalRequestView request) {
        boolean open = store.find(entryId(request.content()))
                .map(entry -> entry.status().isOpen())
                .orElse(false);
        return open
                ? List.of()
                : List.of(new UnmetCondition("FR-011", "Hồ sơ chờ đã ở trạng thái cuối, không điều chỉnh điểm được"));
    }

    @Override
    public ResultRef apply(ApprovalRequestView request, LocalDate actualEffectiveDate) {
        long entryId = entryId(request.content());
        WaitingListEntry entry = store.lock(entryId).orElseThrow();
        scores.adjust(entry, points(request.content()), request.id(), time.today());
        return new ResultRef(WaitingListService.ENTITY_TYPE, String.valueOf(entryId));
    }

    private static long entryId(JsonNode content) {
        JsonNode node = content == null ? null : content.get("entryId");
        if (node == null || !node.isIntegralNumber() || node.asLong() <= 0) {
            throw new InvalidValueException("Nội dung cần entryId là mã hồ sơ chờ");
        }
        return node.asLong();
    }

    private static int points(JsonNode content) {
        JsonNode node = content == null ? null : content.get("points");
        if (node == null || !node.isIntegralNumber()) {
            throw new InvalidValueException("Nội dung cần points là số nguyên");
        }
        return node.asInt();
    }
}
