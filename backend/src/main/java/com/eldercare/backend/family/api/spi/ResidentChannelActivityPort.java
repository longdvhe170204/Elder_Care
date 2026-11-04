package com.eldercare.backend.family.api.spi;

import com.eldercare.backend.family.api.dto.ResidentChannelSessionView;
import java.time.LocalDate;
import java.util.List;

/** SPI 014 cài ({@code HoatDongKenhNctPort}; 012 contracts mục 1.4, 014 FR-068a); chỉ đọc. Bean thiếu thì kênh hiện "chưa có dữ liệu". */
public interface ResidentChannelActivityPort {

    List<ResidentChannelSessionView> upcoming(long residentId, LocalDate from, LocalDate to);
}
