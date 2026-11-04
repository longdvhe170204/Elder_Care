package com.eldercare.backend.family.api.spi;

import com.eldercare.backend.family.api.dto.ResidentChannelBelongingView;
import java.util.List;

/** SPI 013 cài ({@code DoGuiKenhNctPort}; 012 contracts mục 1.4, 013 FR-030a); chỉ đọc. Bean thiếu thì kênh hiện "chưa có dữ liệu". */
public interface ResidentChannelBelongingPort {

    List<ResidentChannelBelongingView> held(long residentId);
}
