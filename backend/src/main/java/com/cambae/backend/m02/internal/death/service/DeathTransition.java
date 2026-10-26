package com.cambae.backend.m02.internal.death.service;

import com.cambae.backend.common.file.FileApi;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.dto.TransitionResult;
import com.cambae.backend.m02.api.events.DeathRecorded;
import com.cambae.backend.m02.api.events.ResidencyEndingCancelled;
import com.cambae.backend.m02.internal.absence.service.AbsenceTransition;
import com.cambae.backend.m02.internal.change.service.ResidencyChangeClosing;
import com.cambae.backend.m02.internal.contract.service.ContractTransition;
import com.cambae.backend.m02.internal.death.domain.DeathRecord;
import com.cambae.backend.m02.internal.death.persistence.DeathStore;
import com.cambae.backend.m02.internal.ending.domain.ResidencyEnding;
import com.cambae.backend.m02.internal.ending.persistence.ResidencyEndingStore;
import com.cambae.backend.m02.internal.transition.service.ResidencyLock;
import com.cambae.backend.m02.internal.waitinglist.service.WaitingListTransition;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/**
 * Phần của 004 trong lệnh Ghi nhận qua đời của 001 (FR-069 → FR-072; research T-13), do {@code M02StatusParticipant} gọi trong
 * transaction của lệnh, dưới khóa {@code resident_residency}. Lệnh không bị chặn bởi đồ gửi, thuốc gửi, chi phí hay sự cố nên
 * 004 không đưa điều kiện nào. Tác động trong cùng một lần: bản ghi qua đời (nhóm 3), hợp đồng Chấm dứt "Qua đời", lượt vắng
 * đóng tại thời điểm qua đời, hồ sơ chờ Hủy chờ, yêu cầu thay đổi lưu trú đóng, hồ sơ kết thúc Đang chuẩn bị chuyển Đã hủy
 * (mốc dừng tính phí là ngày qua đời nên không gọi {@code FinalPeriodPort.cancelStop}), rồi tạo năm mục việc sau qua đời từ
 * trạng thái hiện tại của module sở hữu; điều kiện kết thúc chưa đạt của hồ sơ chính là các mục đó nên không cần chép riêng.
 * Bằng chứng, giường, lịch, thông báo người liên hệ chính do 001, 003, 009 làm theo sự kiện.
 */
@Component
public class DeathTransition {

    public static final String EVIDENCE_OWNER_TYPE = "M02.DEATH_EVIDENCE";
    static final String CANCEL_REASON = "Qua đời";
    static final AuditAction RECORDED = new AuditAction("M02_DEATH_RECORDED", false);
    static final AuditAction ENDING_CANCELLED = new AuditAction("M02_ENDING_CANCELLED_BY_DEATH", false);

    private final DeathStore store;
    private final ResidencyEndingStore endings;
    private final PostDeathTaskService tasks;
    private final ContractTransition contractTransition;
    private final AbsenceTransition absenceTransition;
    private final WaitingListTransition waitingListTransition;
    private final ResidencyChangeClosing changeClosing;
    private final ResidencyLock residencyLock;
    private final FileApi files;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;

    DeathTransition(
            DeathStore store,
            ResidencyEndingStore endings,
            PostDeathTaskService tasks,
            ContractTransition contractTransition,
            AbsenceTransition absenceTransition,
            WaitingListTransition waitingListTransition,
            ResidencyChangeClosing changeClosing,
            ResidencyLock residencyLock,
            FileApi files,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser) {
        this.store = store;
        this.endings = endings;
        this.tasks = tasks;
        this.contractTransition = contractTransition;
        this.absenceTransition = absenceTransition;
        this.waitingListTransition = waitingListTransition;
        this.changeClosing = changeClosing;
        this.residencyLock = residencyLock;
        this.files = files;
        this.auditLog = auditLog;
        this.events = events;
        this.time = time;
        this.currentUser = currentUser;
    }

    /** Tác động khi lệnh Ghi nhận qua đời thành công. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recorded(TransitionResult result) {
        long residentId = result.residentId();
        residencyLock.lock(residentId);
        JsonNode details = result.details();
        Instant deathAt = Instant.parse(details.get("deathAt").asString());
        Long evidence = details.hasNonNull("evidenceFileId")
                ? details.get("evidenceFileId").asLong()
                : null;
        long accountId = currentUser.getObject().accountId();
        DeathRecord record = new DeathRecord(
                0,
                residentId,
                deathAt,
                details.get("place").asString().strip(),
                details.get("discoveredBy").asString().strip(),
                details.get("confirmedById").asLong(),
                details.hasNonNull("cause") && !details.get("cause").asString().isBlank()
                        ? details.get("cause").asString().strip()
                        : null,
                evidence,
                result.historyId(),
                accountId,
                time.now());
        long recordId = store.insert(record);
        if (evidence != null) {
            files.attach(evidence, EVIDENCE_OWNER_TYPE, recordId);
        }
        contractTransition.residentDied(result);
        absenceTransition.closedByFinalState(residentId, deathAt, "RECORD_DEATH");
        waitingListTransition.cancelled(result);
        changeClosing.closeAll(residentId, "FR-070");
        cancelPreparingEnding(residentId, accountId, result.historyId());
        auditLog.write(AuditLogEntry.builder()
                .action(RECORDED)
                .entity(PostDeathTaskService.ENTITY_TYPE, recordId)
                .resident(residentId)
                .after(afterOf(record))
                .details(Map.of("historyId", result.historyId()))
                .build());
        tasks.recalculate(new DeathRecord(
                recordId,
                residentId,
                deathAt,
                record.place(),
                record.discoveredBy(),
                record.confirmedById(),
                record.causeOfDeath(),
                evidence,
                result.historyId(),
                accountId,
                record.recordedAt()));
        events.publishEvent(new DeathRecorded(residentId, deathAt, recordId));
    }

    private void cancelPreparingEnding(long residentId, long accountId, long historyId) {
        Optional<ResidencyEnding> preparing = endings.lockPreparingOf(residentId);
        if (preparing.isEmpty()) {
            return;
        }
        ResidencyEnding ending = preparing.get();
        endings.cancel(ending.id(), CANCEL_REASON, accountId, time.now());
        auditLog.write(AuditLogEntry.builder()
                .action(ENDING_CANCELLED)
                .entity("M02.RESIDENCY_ENDING", ending.id())
                .resident(residentId)
                .before(Map.of("status", ending.status().name()))
                .after(Map.of("status", "CANCELLED"))
                .details(Map.of("historyId", historyId))
                .build());
        events.publishEvent(new ResidencyEndingCancelled(ending.id(), residentId, ending.expectedEndDate()));
    }

    /** Nội dung ghi nhật ký: nguyên nhân thuộc nhóm sức khỏe nên tên trường {@code cause} được phân loại (BR-M02-21). */
    private static Map<String, Object> afterOf(DeathRecord record) {
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("deathAt", record.deathAt().toString());
        after.put("place", record.place());
        after.put("discoveredBy", record.discoveredBy());
        after.put("confirmedById", record.confirmedById());
        if (record.causeOfDeath() != null) {
            after.put("cause", record.causeOfDeath());
        }
        if (record.evidenceFileId() != null) {
            after.put("evidenceFileId", record.evidenceFileId());
        }
        return after;
    }
}
