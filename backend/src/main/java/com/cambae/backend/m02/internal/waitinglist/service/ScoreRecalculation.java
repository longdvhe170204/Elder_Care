package com.cambae.backend.m02.internal.waitinglist.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m02.internal.waitinglist.domain.Circumstance;
import com.cambae.backend.m02.internal.waitinglist.domain.CircumstanceStatus;
import com.cambae.backend.m02.internal.waitinglist.domain.ScoreBreakdown;
import com.cambae.backend.m02.internal.waitinglist.domain.ScoreCause;
import com.cambae.backend.m02.internal.waitinglist.domain.WaitingListEntry;
import com.cambae.backend.m02.internal.waitinglist.persistence.WaitingListStore;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tính và ghi điểm ưu tiên của hồ sơ chờ (FR-009, FR-010, research T-07): bảng CFG-M02-08 đọc ở mỗi lần tính (FR-018 của
 * 000: một giá trị cho trọn lần tính). Điểm đổi thì thêm một dòng lịch sử; không đổi thì không thêm. Chạy trong
 * transaction của lệnh gọi, nên các lệnh khóa hồ sơ chờ trước.
 */
@Component
public class ScoreRecalculation {

    private final WaitingListStore store;
    private final ParameterApi parameters;
    private final BusinessTime time;

    ScoreRecalculation(WaitingListStore store, ParameterApi parameters, BusinessTime time) {
        this.store = store;
        this.parameters = parameters;
        this.time = time;
    }

    /** Bảng điểm hiện hành. */
    public PriorityScoreTable table() {
        return parameters
                .read(ParameterCode.CFG_M02_08)
                .asTable(PriorityScoreTable.Config.class)
                .toTable();
    }

    /**
     * Bảng điểm có hiệu lực ngay lúc này, đọc từ lịch sử tham số (không qua bộ đệm): dùng khi chạy sau một sự kiện đổi tham
     * số, vì bộ đệm chỉ xóa khi transaction đổi tham số kết thúc và sự kiện có thể tới muộn hơn lần đổi kế tiếp.
     */
    public PriorityScoreTable currentTable() {
        return parameters
                .readAt(ParameterCode.CFG_M02_08, time.now())
                .asTable(PriorityScoreTable.Config.class)
                .toTable();
    }

    /** Điểm của hồ sơ tại ngày {@code day} theo bảng {@code table}, giữ nguyên tổng điều chỉnh đã duyệt. */
    public ScoreBreakdown compute(
            PriorityScoreTable table, WaitingListEntry entry, List<Circumstance> circumstances, LocalDate day) {
        Set<String> confirmed = circumstances.stream()
                .filter(c -> c.status() == CircumstanceStatus.CONFIRMED)
                .map(Circumstance::code)
                .collect(Collectors.toSet());
        return PriorityScoreCalculator.compute(
                table,
                entry.careLevel(),
                entry.applicationDate(),
                day,
                confirmed,
                entry.breakdown().adjustment());
    }

    /** Lần tính đầu khi đưa vào danh sách chờ: ghi dòng {@code CREATED} (điểm cũ 0). */
    @Transactional(propagation = Propagation.MANDATORY)
    public WaitingListEntry first(WaitingListEntry entry, LocalDate day) {
        ScoreBreakdown breakdown = compute(table(), entry, store.circumstances(entry.id()), day);
        store.updateScore(entry.id(), breakdown);
        store.addHistory(entry.id(), 0, breakdown, ScoreCause.CREATED, null, time.now());
        return store.find(entry.id()).orElseThrow();
    }

    /**
     * Tính lại tại ngày {@code day}; trả hồ sơ sau khi tính. Tổng điểm đổi thì thêm dòng lịch sử nguyên nhân {@code
     * cause}; chỉ thành phần đổi mà tổng giữ nguyên thì cập nhật thành phần, không thêm lịch sử.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public WaitingListEntry rescore(WaitingListEntry entry, LocalDate day, ScoreCause cause) {
        return rescore(entry, table(), day, cause);
    }

    /** Như {@link #rescore(WaitingListEntry, LocalDate, ScoreCause)} với bảng đã đọc, để tính nhiều hồ sơ một giá trị. */
    @Transactional(propagation = Propagation.MANDATORY)
    public WaitingListEntry rescore(WaitingListEntry entry, PriorityScoreTable table, LocalDate day, ScoreCause cause) {
        ScoreBreakdown breakdown = compute(table, entry, store.circumstances(entry.id()), day);
        if (breakdown.equals(entry.breakdown())) {
            return entry;
        }
        store.updateScore(entry.id(), breakdown);
        if (breakdown.total() != entry.score()) {
            store.addHistory(entry.id(), entry.score(), breakdown, cause, null, time.now());
        }
        return store.find(entry.id()).orElseThrow();
    }

    /** Áp dụng đề nghị điều chỉnh đã duyệt (FR-011): cộng {@code points} vào tổng điều chỉnh, ghi dòng {@code ADJUSTMENT}. */
    @Transactional(propagation = Propagation.MANDATORY)
    public WaitingListEntry adjust(WaitingListEntry entry, int points, long approvalRequestId, LocalDate day) {
        ScoreBreakdown base = compute(table(), entry, store.circumstances(entry.id()), day);
        ScoreBreakdown breakdown = new ScoreBreakdown(
                base.careLevel(),
                base.waitingTime(),
                base.circumstances(),
                entry.breakdown().adjustment() + points);
        store.updateScore(entry.id(), breakdown);
        store.addHistory(entry.id(), entry.score(), breakdown, ScoreCause.ADJUSTMENT, approvalRequestId, time.now());
        return store.find(entry.id()).orElseThrow();
    }
}
