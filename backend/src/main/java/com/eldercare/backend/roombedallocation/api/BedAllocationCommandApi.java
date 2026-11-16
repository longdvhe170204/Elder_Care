package com.eldercare.backend.roombedallocation.api;

import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.roombedallocation.api.dto.HoldCancellationReason;
import com.eldercare.backend.roombedallocation.api.dto.ScheduledTransferResult;
import com.eldercare.backend.roombedallocation.api.spi.ResidencyType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Hợp đồng 003 BedAllocationCommandApi (T003); bản cài thuộc user story tương ứng. */
public interface BedAllocationCommandApi {
    @Transactional(propagation = Propagation.MANDATORY)
    void holdForWaitingRecord(long waitingRecordId, long residentId, long bedId, Instant at);

    @Transactional(propagation = Propagation.MANDATORY)
    void cancelHold(long waitingRecordId, HoldCancellationReason reason);

    @Transactional(propagation = Propagation.MANDATORY)
    void releaseWhenAbsent(long residentId, Instant at, Basis basis);

    @Transactional(propagation = Propagation.MANDATORY)
    void extendExpectedEnd(long residentId, Instant expectedEnd);

    @Transactional(propagation = Propagation.MANDATORY)
    List<UnmetCondition> checkScheduledReservation(long residentId, long nextBedId, LocalDate effectiveDate);

    @Transactional(propagation = Propagation.MANDATORY)
    long reserveScheduledTransfer(long requestId, long residentId, long nextBedId, LocalDate effectiveDate);

    /**
     * Mục 1.4a (chủ dự án duyệt 2026-11-17; 004 FR-041): điều kiện đặt trước phân bổ đầu tiên cho người chưa có giường
     * (bán trú đổi sang nội trú kèm giường). Người đã có phân bổ Hiệu lực ({@code ALLOCATION_ALREADY_ACTIVE}) hoặc Tương
     * lai ({@code FUTURE_ALLOCATION_EXISTS}) thì không đạt; FR-018 xét từ 00:00 ngày hiệu lực tới {@code expectedEnd}
     * ({@code null} với dài ngày), điều kiện (b) theo {@code targetType} vì lúc duyệt người này còn là bán trú.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    List<UnmetCondition> checkFirstAllocationReservation(
            long residentId, long bedId, LocalDate effectiveDate, Instant expectedEnd, ResidencyType targetType);

    /**
     * Mục 1.4a: lúc duyệt yêu cầu của 004, tạo phân bổ Tương lai từ 00:00 ngày hiệu lực, căn cứ là yêu cầu, không có
     * phân bổ trước. Ngày hiệu lực bắt đầu bằng {@link #applyScheduledTransfer}; kiểm lại bằng {@link
     * #checkScheduledTransfer}; hủy bằng {@link #cancelScheduledAllocation}.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    long reserveFirstAllocation(
            long requestId,
            long residentId,
            long bedId,
            LocalDate effectiveDate,
            Instant expectedEnd,
            ResidencyType targetType);

    @Transactional(propagation = Propagation.MANDATORY)
    List<UnmetCondition> checkScheduledTransfer(long requestId);

    @Transactional(propagation = Propagation.MANDATORY)
    ScheduledTransferResult applyScheduledTransfer(long requestId, Instant plannedAt);

    @Transactional(propagation = Propagation.MANDATORY)
    void cancelScheduledAllocation(long requestId, Reason reason);

    @Transactional(propagation = Propagation.MANDATORY)
    void requestCreatedAfterUrgentTransfer(long allocationId, long requestId);

    @Transactional(propagation = Propagation.MANDATORY)
    void urgentTransferRequestResult(long allocationId, boolean approved);

    /**
     * FR-030 (chủ dự án chốt 2026-11-12): yêu cầu sau chuyển gấp bị hủy hoặc Áp dụng không thành, không còn yêu cầu khác cho
     * lần chuyển đó: đặt lại dấu "chờ lập yêu cầu" và mở lại mốc nhắc CFG-M03-02. Phân bổ đã đóng hoặc đang chờ chuyển về
     * phòng cùng giá thì không làm gì; gọi lặp không tác động thêm.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    void urgentTransferRequestWithdrawn(long allocationId);

    /**
     * T035 (BR-M03-23, FR-034): 004 gọi khi phụ lục đổi loại lưu trú được áp dụng, thay cho nghe {@code ResidenceTypeChanged}
     * (nghe tạo vòng admission ↔ roombedallocation). Kiểm lại phân bổ Hiệu lực, Tương lai theo dữ liệu trong transaction.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    void residenceTypeChanged(long residentId);
}
