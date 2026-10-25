package com.cambae.backend.m02.internal.waitinglist.service;

import com.cambae.backend.m01.api.ResidentApi;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.Gender;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.internal.waitinglist.domain.WaitingListEntry;
import com.cambae.backend.m02.internal.waitinglist.persistence.WaitingListStore;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Danh sách hồ sơ chờ phù hợp với một giường Trống (FR-012, FR-013, BR-M02-01, BR-M02-10; research T-07). Phần của 004: hồ sơ
 * Đang chờ, chưa có lần liên hệ, từ chối hay hết hạn với chính giường này từ đầu lượt trống, sắp theo điểm giảm dần, ngày
 * đăng ký, thời điểm tạo. Điều kiện về phòng (loại hình phục vụ, mức được phép, giới tính, cách ly) và đầu lượt trống là dữ
 * liệu của 003: người gọi truyền vào dạng {@link MatchCriteria} và {@code vacancyStart}. Không chọn sẵn, không giới hạn số
 * hồ sơ; danh sách rỗng nghĩa là "không có người phù hợp". Nối với 003 (thuộc tính phòng, mốc lượt trống, sự kiện giường
 * Trống) làm khi 003 công bố giao diện.
 */
@Service
public class MatchingEntryService {

    /** Điều kiện phòng của giường: loại lưu trú phục vụ, mức chăm sóc được phép, giới tính phù hợp chính sách phòng. */
    public record MatchCriteria(Set<ResidenceType> residenceTypes, Set<CareLevel> careLevels, Set<Gender> genders) {}

    private final WaitingListStore store;
    private final ResidentApi residents;

    MatchingEntryService(WaitingListStore store, ResidentApi residents) {
        this.store = store;
        this.residents = residents;
    }

    @Transactional(readOnly = true)
    public List<WaitingListEntry> matching(long bedId, Instant vacancyStart, MatchCriteria criteria) {
        if (criteria.genders().isEmpty()) {
            return List.of();
        }
        return store.matching(bedId, vacancyStart, criteria.residenceTypes(), criteria.careLevels()).stream()
                .filter(entry -> residents
                        .gender(entry.residentId())
                        .filter(criteria.genders()::contains)
                        .isPresent())
                .toList();
    }
}
