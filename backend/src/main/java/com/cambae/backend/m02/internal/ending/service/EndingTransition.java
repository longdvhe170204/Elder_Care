package com.cambae.backend.m02.internal.ending.service;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.dto.CommandContext;
import com.cambae.backend.m01.api.dto.Condition;
import com.cambae.backend.m01.api.dto.ConditionWaiver;
import com.cambae.backend.m01.api.dto.TransitionResult;
import com.cambae.backend.m02.api.events.ResidencyEndingCompleted;
import com.cambae.backend.m02.internal.absence.service.AbsenceTransition;
import com.cambae.backend.m02.internal.change.service.ResidencyChangeClosing;
import com.cambae.backend.m02.internal.contract.service.ContractTransition;
import com.cambae.backend.m02.internal.ending.domain.EndingConditionCode;
import com.cambae.backend.m02.internal.ending.domain.EndingExceptionRecord;
import com.cambae.backend.m02.internal.ending.domain.ResidencyEnding;
import com.cambae.backend.m02.internal.ending.persistence.ResidencyEndingStore;
import com.cambae.backend.m02.internal.transition.service.ResidencyLock;
import com.cambae.backend.m02.internal.waitinglist.service.WaitingListTransition;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/**
 * Phần của hồ sơ kết thúc trong lệnh Kết thúc lưu trú của 001 (FR-063 → FR-066a; research T-09, T-03), do {@code
 * M02StatusParticipant} gọi. Điều kiện của 004: có hồ sơ Đang chuẩn bị đúng mã trong lệnh, chưa mang dấu "quá ngày dự kiến",
 * thời điểm hiệu lực nằm trong ngày kết thúc dự kiến, đã bàn giao (e, không có ngoại lệ). Miễn điều kiện: ngoại lệ đã duyệt
 * của hồ sơ (loại {@code M02.ENDING_EXCEPTION}). Tác động chạy trong transaction của lệnh, dưới khóa {@code
 * resident_residency}: hồ sơ Hoàn tất, hợp đồng Kết thúc hoặc Chấm dứt, lượt vắng đóng, hồ sơ chờ Hủy chờ, yêu cầu thay đổi
 * lưu trú đóng, hồ sơ lưu trú đã đóng (FR-072).
 */
@Component
public class EndingTransition {

    static final String NO_PREPARING_ENDING = "END_NO_PREPARING_ENDING";
    static final String PAST_EXPECTED_DATE = "END_PAST_EXPECTED_DATE";
    static final String EFFECTIVE_DATE_OUT_OF_RANGE = "END_EFFECTIVE_DATE_OUT_OF_RANGE";
    static final AuditAction COMPLETED = new AuditAction("M02_ENDING_COMPLETED", false);

    private final ResidencyEndingStore store;
    private final EndingHandovers handovers;
    private final ContractTransition contractTransition;
    private final AbsenceTransition absenceTransition;
    private final WaitingListTransition waitingListTransition;
    private final ResidencyChangeClosing changeClosing;
    private final ResidencyLock residencyLock;
    private final JdbcTemplate jdbc;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;

    EndingTransition(
            ResidencyEndingStore store,
            EndingHandovers handovers,
            ContractTransition contractTransition,
            AbsenceTransition absenceTransition,
            WaitingListTransition waitingListTransition,
            ResidencyChangeClosing changeClosing,
            ResidencyLock residencyLock,
            JdbcTemplate jdbc,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser) {
        this.store = store;
        this.handovers = handovers;
        this.contractTransition = contractTransition;
        this.absenceTransition = absenceTransition;
        this.waitingListTransition = waitingListTransition;
        this.changeClosing = changeClosing;
        this.residencyLock = residencyLock;
        this.jdbc = jdbc;
        this.auditLog = auditLog;
        this.events = events;
        this.time = time;
        this.currentUser = currentUser;
    }

    /**
     * Điều kiện của 004 cho Kết thúc lưu trú. Xem trước (không có {@code details}) và lệnh thật cùng một bộ kiểm; lệnh thật
     * thêm kiểm mã hồ sơ và thời điểm hiệu lực.
     */
    public List<Condition> unmet(CommandContext context) {
        Optional<ResidencyEnding> open = store.preparingOf(context.residentId());
        JsonNode details = context.details();
        boolean named = details != null && details.hasNonNull("residencyEndingId");
        if (open.isEmpty()
                || (named && open.get().id() != details.get("residencyEndingId").asLong())) {
            return List.of(new Condition(
                    NO_PREPARING_ENDING, "Chưa có hồ sơ kết thúc lưu trú Đang chuẩn bị (FR-062)", "FR-062", false));
        }
        ResidencyEnding ending = open.get();
        List<Condition> unmet = new ArrayList<>();
        if (ending.overdueSince() != null) {
            unmet.add(new Condition(
                    PAST_EXPECTED_DATE,
                    "Hồ sơ mang dấu quá ngày dự kiến từ " + ending.overdueSince()
                            + ": đặt ngày kết thúc dự kiến mới không sớm hơn hôm nay (FR-066a)",
                    "FR-066a",
                    false));
        } else if (details != null && details.hasNonNull("effectiveAt")) {
            LocalDate effectiveDate =
                    localDate(Instant.parse(details.get("effectiveAt").asString()));
            if (!effectiveDate.equals(ending.expectedEndDate())) {
                unmet.add(new Condition(
                        EFFECTIVE_DATE_OUT_OF_RANGE,
                        "Thời điểm hiệu lực phải trong ngày kết thúc dự kiến " + ending.expectedEndDate()
                                + ": muốn kết thúc ngày khác thì đổi ngày dự kiến trước (FR-066)",
                        "FR-066",
                        false));
            }
        }
        if (handovers.effective(ending.id()).isEmpty()) {
            unmet.add(new Condition(
                    EndingConditionCode.END_E_HANDOVER.name(),
                    "Chưa ghi nhận bàn giao người cao tuổi (người nhận, thời điểm, nhân viên bàn giao)",
                    "FR-063 (e)",
                    false));
        }
        return unmet;
    }

    /**
     * Miễn điều kiện bằng ngoại lệ đã duyệt của hồ sơ (FR-064). {@code ConditionWaiver} chỉ mang một mã yêu cầu nên ghi yêu
     * cầu được duyệt đầu tiên; mọi mã được miễn đều nằm trong tập mã.
     */
    public Optional<ConditionWaiver> waiver(CommandContext context) {
        Optional<ResidencyEnding> open = store.preparingOf(context.residentId());
        if (open.isEmpty()) {
            return Optional.empty();
        }
        List<EndingExceptionRecord> exceptions = store.exceptionsOf(open.get().id());
        if (exceptions.isEmpty()) {
            return Optional.empty();
        }
        Set<String> codes = new LinkedHashSet<>();
        exceptions.forEach(e -> codes.add(e.conditionCode().name()));
        long first = exceptions.stream()
                .map(EndingExceptionRecord::approvalRequestId)
                .min(Comparator.naturalOrder())
                .orElseThrow();
        return Optional.of(new ConditionWaiver(first, codes));
    }

    /** Tác động khi lệnh Kết thúc lưu trú thành công (FR-065, FR-072). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void completed(TransitionResult result) {
        long residentId = result.residentId();
        residencyLock.lock(residentId);
        JsonNode details = result.details();
        long endingId = details.get("residencyEndingId").asLong();
        Instant effectiveAt = Instant.parse(details.get("effectiveAt").asString());
        ResidencyEnding ending = store.lock(endingId).orElseThrow();
        if (!ending.preparing() || ending.residentId() != residentId) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "RESIDENCY_ENDING_NOT_PREPARING",
                    "Hồ sơ kết thúc lưu trú không còn Đang chuẩn bị (FR-067)");
        }
        LocalDate endDate = localDate(effectiveAt);
        store.complete(endingId, effectiveAt, currentUser.getObject().accountId(), time.now());
        contractTransition.residencyEnded(result, endDate);
        absenceTransition.closedByFinalState(residentId, effectiveAt, "END_RESIDENCY");
        waitingListTransition.cancelled(result);
        changeClosing.closeAll(residentId, "FR-065");
        jdbc.update(
                "UPDATE resident_residency SET closed_at = ?, version = version + 1 WHERE resident_id = ? AND closed_at"
                        + " IS NULL",
                LocalDateTime.ofInstant(time.now(), ZoneOffset.UTC),
                residentId);
        auditLog.write(AuditLogEntry.builder()
                .action(COMPLETED)
                .entity(ResidencyEndingService.ENTITY_TYPE, endingId)
                .resident(residentId)
                .before(Map.of("status", ending.status().name()))
                .after(Map.of("status", "COMPLETED", "actualEndedAt", effectiveAt.toString()))
                .details(Map.of("historyId", result.historyId()))
                .build());
        events.publishEvent(new ResidencyEndingCompleted(endingId, residentId, endDate));
    }

    static LocalDate localDate(Instant instant) {
        return instant.atZone(BusinessTime.ZONE).toLocalDate();
    }
}
