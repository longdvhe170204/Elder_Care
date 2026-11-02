package com.eldercare.backend.admission.internal.waitinglist.service;

import com.eldercare.backend.admission.api.events.BedAvailableForWaitingList;
import com.eldercare.backend.admission.internal.domain.CareLevelCodes;
import com.eldercare.backend.admission.internal.waitinglist.domain.BedContact;
import com.eldercare.backend.admission.internal.waitinglist.domain.WaitingListEntry;
import com.eldercare.backend.admission.internal.waitinglist.persistence.BedContactStore;
import com.eldercare.backend.admission.internal.waitinglist.persistence.WaitingListStore;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.Gender;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.roombedallocation.api.BedStatusApi;
import com.eldercare.backend.roombedallocation.api.RoomPolicyApi;
import com.eldercare.backend.roombedallocation.api.dto.GenderPolicy;
import com.eldercare.backend.roombedallocation.api.dto.RoomAllocationProperties;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Danh sách hồ sơ chờ phù hợp với một giường Trống (FR-012, FR-013, BR-M02-01, BR-M02-10, BR-M02-20; research T-07; Q-394): hồ sơ
 * Đang chờ thỏa loại lưu trú, mức được phép, giới tính của phòng, phòng không chịu chặn cách ly, chưa có lần liên hệ, từ chối hay
 * hết hạn với chính giường này trong lượt trống hiện tại; xếp Ưu tiên trước, rồi ngày đăng ký, thời điểm tạo. Không chọn sẵn,
 * không giới hạn số hồ sơ. Giường có phân bổ tương lai hoặc dấu "chờ chuyển người" không nhận hồ sơ.
 */
@Service
public class MatchingEntryService {

    /** {@code vacancyStart} rỗng: giường không Trống hoặc không nhận hồ sơ. */
    public record Result(Instant vacancyStart, List<WaitingListEntry> entries) {}

    private final WaitingListStore store;
    private final BedContactStore contacts;
    private final ResidentApi residents;
    private final RoomPolicyApi rooms;
    private final BedStatusApi bedStatus;
    private final BusinessTime time;

    MatchingEntryService(
            WaitingListStore store,
            BedContactStore contacts,
            ResidentApi residents,
            RoomPolicyApi rooms,
            BedStatusApi bedStatus,
            BusinessTime time) {
        this.store = store;
        this.contacts = contacts;
        this.residents = residents;
        this.rooms = rooms;
        this.bedStatus = bedStatus;
        this.time = time;
    }

    @Transactional(readOnly = true)
    public Result matching(long bedId) {
        Optional<Instant> vacancy = bedStatus.emptyPeriodStarted(bedId);
        RoomAllocationProperties room = rooms.allocationProperties(bedId);
        if (vacancy.isEmpty() || room.futureAllocation() || room.awaitingResidentTransfer()) {
            return new Result(null, List.of());
        }
        if (room.isolated()) {
            return new Result(vacancy.get(), List.of());
        }
        Set<ResidenceType> types = EnumSet.noneOf(ResidenceType.class);
        if (room.longTerm()) {
            types.add(ResidenceType.LONG_TERM_RESIDENTIAL);
        }
        if (room.shortTerm()) {
            types.add(ResidenceType.SHORT_TERM_RESIDENTIAL);
        }
        Set<CareLevel> levels =
                room.careLevels().stream().map(CareLevelCodes::of).collect(Collectors.toSet());
        Instant start = vacancy.get();
        List<WaitingListEntry> entries = store.waitingOf(types, levels).stream()
                .filter(entry -> genderAllowed(entry, room.genderPolicy()))
                .filter(entry -> !contactedInVacancy(entry, bedId, start))
                .toList();
        return new Result(start, entries);
    }

    /** Báo Hành chính giường cần chọn hồ sơ, kèm số hồ sơ phù hợp (FR-013, FR-015); 0 là "không có người phù hợp". */
    public void notifyBedAvailable(long bedId, ApplicationEventPublisher events) {
        Result result = matching(bedId);
        if (result.vacancyStart() != null) {
            events.publishEvent(
                    new BedAvailableForWaitingList(bedId, result.entries().size(), time.now()));
        }
    }

    private boolean genderAllowed(WaitingListEntry entry, GenderPolicy policy) {
        if (policy == GenderPolicy.UNRESTRICTED) {
            return true;
        }
        Gender required = policy == GenderPolicy.MALE ? Gender.MALE : Gender.FEMALE;
        return residents.gender(entry.residentId()).filter(required::equals).isPresent();
    }

    private boolean contactedInVacancy(WaitingListEntry entry, long bedId, Instant vacancyStart) {
        return contacts.of(entry.id()).stream()
                .filter(event -> !event.occurredAt().isBefore(vacancyStart))
                .map(event -> event.content())
                .map(BedContact::bedId)
                .anyMatch(id -> id == bedId);
    }
}
