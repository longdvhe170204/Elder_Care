package com.eldercare.backend.billing.balance.internal.status.domain;

import com.eldercare.backend.administration.shared.api.dto.HistoryTrack;
import java.util.Set;

/**
 * Một lần tính tình trạng (tên logic {@code lich_su_tinh_trang}; data-model 017 mục 4; R-15: dòng {@code status_history}
 * {@code M11.BALANCE}/{@code STATUS}, khóa duy nhất (toàn bảng) {@code M11.BALANCE:<người cao tuổi>:<ngày>:<nguồn>} với nguồn {@code DAILY} hoặc
 * {@code TRANSACTION:<id>}, SC-012). Giá trị trước, sau của dòng là tình trạng.
 */
public record StatusSnapshot(
        long balance, long unfinalizedTotal, Long dailyFee, Integer daysCovered, Set<Notice> notices) {

    public static final HistoryTrack<StatusSnapshot> TRACK =
            new HistoryTrack<>("M11.BALANCE", "STATUS", StatusSnapshot.class);
}
