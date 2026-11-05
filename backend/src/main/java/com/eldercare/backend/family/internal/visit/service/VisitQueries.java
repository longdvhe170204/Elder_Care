package com.eldercare.backend.family.internal.visit.service;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.care.api.spi.TimetableSourcePort;
import com.eldercare.backend.family.internal.relationship.domain.FamilyMember;
import com.eldercare.backend.family.internal.relationship.service.RelationshipQueries;
import com.eldercare.backend.family.internal.visit.domain.Visit;
import com.eldercare.backend.family.internal.visit.domain.Visit.Status;
import com.eldercare.backend.incident.api.dto.VisitTracePersonView;
import com.eldercare.backend.incident.api.dto.VisitTraceView;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc lượt thăm cho module khác: thời khóa biểu của người cao tuổi ({@code ThoiKhoaBieuNguonPort} của 005, lượt Đã duyệt,
 * Đã vào, Đã ra trong ngày) và truy vết tiếp xúc của 007 ({@code NguoiThanKhanCapPort.luotThamTrongKhoang}; FR-038):
 * người đăng ký (có hồ sơ) và người đi cùng thực tế vào (chỉ họ tên, quan hệ, số điện thoại nếu có), giờ vào, giờ ra.
 */
@Component
public class VisitQueries implements TimetableSourcePort {

    private static final Set<String> ON_TIMETABLE = Set.of("APPROVED", "CHECKED_IN", "CHECKED_OUT");
    private static final Set<String> ENTERED = Set.of(Status.CHECKED_IN.name(), Status.CHECKED_OUT.name());

    private final SimpleRecordApi records;
    private final RelationshipQueries relationships;

    VisitQueries(SimpleRecordApi records, RelationshipQueries relationships) {
        this.records = records;
        this.relationships = relationships;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimetableItemView> onDate(long residentId, LocalDate date) {
        return records.listByResident(Visit.KIND, residentId, ON_TIMETABLE).stream()
                .filter(v -> v.content().date().equals(date))
                .sorted(Comparator.comparing(v -> v.content().slotStart()))
                .map(v -> new TimetableItemView(
                        v.content().slotStart(),
                        Visit.KIND.code(),
                        "Thăm: " + name(v.content().familyMemberId()) + ", "
                                + v.content().startTime() + "–" + v.content().endTime(),
                        v.status()))
                .toList();
    }

    /** Lượt Đã vào hoặc Đã ra có thời gian chồng {@code [from, to)}. */
    @Transactional(readOnly = true)
    public List<VisitTraceView> inRange(long residentId, Instant from, Instant to) {
        List<VisitTraceView> result = new ArrayList<>();
        for (SimpleRecord<Visit> v : records.listByResident(Visit.KIND, residentId, ENTERED)) {
            Visit visit = v.content();
            boolean overlaps = visit.checkedInAt().isBefore(to)
                    && (visit.checkedOutAt() == null || visit.checkedOutAt().isAfter(from));
            if (!overlaps) {
                continue;
            }
            List<VisitTracePersonView> people = new ArrayList<>();
            Optional<FamilyMember> registrant = relationships.familyMember(visit.familyMemberId());
            people.add(new VisitTracePersonView(
                    Optional.of(visit.familyMemberId()),
                    registrant.map(FamilyMember::fullName).orElse(""),
                    relationships
                            .active(visit.familyMemberId(), residentId)
                            .map(r -> r.relationship().name()),
                    registrant.map(FamilyMember::phoneNumber),
                    true));
            visit.companions().stream()
                    .filter(Visit.Companion::entered)
                    .forEach(c -> people.add(new VisitTracePersonView(
                            Optional.empty(),
                            c.fullName(),
                            Optional.of(c.relationship()),
                            Optional.ofNullable(c.phoneNumber()),
                            false)));
            result.add(
                    new VisitTraceView(v.id(), visit.checkedInAt(), Optional.ofNullable(visit.checkedOutAt()), people));
        }
        return result;
    }

    private String name(long familyMemberId) {
        return relationships
                .familyMember(familyMemberId)
                .map(FamilyMember::fullName)
                .orElse("người thân");
    }
}
