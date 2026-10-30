package com.eldercare.backend.administration.shared.api;

import com.eldercare.backend.administration.shared.api.dto.HistoryEntry;
import com.eldercare.backend.administration.shared.api.dto.HistoryRow;
import com.eldercare.backend.administration.shared.api.dto.HistoryTrack;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Lịch sử dùng chung {@code status_history} (nhóm 3, chỉ thêm; {@code ends_at} của khoảng đặt một lần; module-interfaces
 * 000 mục 1.8). Lệnh ghi chạy trong transaction của lệnh gọi.
 */
public interface StatusHistoryApi {

    /** Thêm dòng; 409 khi thêm khoảng trong lúc còn khoảng mở, hoặc trùng {@code uniqueKey}. */
    <D> long add(HistoryTrack<D> track, long entityId, HistoryEntry<D> entry);

    /** Đóng khoảng đang mở tại {@code endsAt}; trả dòng đã đóng, rỗng khi không có khoảng mở. */
    <D> Optional<HistoryRow<D>> closeOpen(HistoryTrack<D> track, long entityId, Instant endsAt);

    <D> Optional<HistoryRow<D>> open(HistoryTrack<D> track, long entityId);

    /** Dòng có hiệu lực tại {@code at}: dòng bắt đầu muộn nhất không sau {@code at}, khoảng thì chưa kết thúc tại đó. */
    <D> Optional<HistoryRow<D>> at(HistoryTrack<D> track, long entityId, Instant at);

    /** Theo {@code starts_at}, {@code id}. */
    <D> List<HistoryRow<D>> list(HistoryTrack<D> track, long entityId);
}
