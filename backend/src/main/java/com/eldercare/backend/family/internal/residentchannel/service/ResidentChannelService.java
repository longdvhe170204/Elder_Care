package com.eldercare.backend.family.internal.residentchannel.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.care.api.ResidentActivityReadApi;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.api.spi.ResidentChannelBelongingPort;
import com.eldercare.backend.family.internal.relationship.domain.FamilyMember;
import com.eldercare.backend.family.internal.relationship.service.RelationshipQueries;
import com.eldercare.backend.family.internal.visit.domain.Visit;
import com.eldercare.backend.family.internal.visit.service.VisitQueries;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kênh người cao tuổi (012 T068; US9; FR-083, FR-084, BR-M10-11; research F-18): chỉ tài khoản AC-16, người cao tuổi lấy
 * từ tài khoản đăng nhập. Năm phần dựng ở một chỗ với DTO riêng, không có trường sức khỏe, chi phí, số dư, hợp đồng:
 * hôm nay (lượt thăm Đã duyệt, buổi hoạt động trong ngày, lịch đến/về bán trú), thực đơn (011 chưa có API), buổi hoạt
 * động sắp tới (014 qua {@link ResidentActivityReadApi}, đổi hướng duyệt 07/11), lượt thăm Đã duyệt, đồ gửi đang giữ (013 qua
 * {@link ResidentChannelBelongingPort}). Nguồn chưa có bản cài thì phần đó báo chưa có dữ liệu.
 */
@Service
public class ResidentChannelService {

    private static final PermissionKey VIEW = new PermissionKey(Function.RESIDENT_CHANNEL, Operation.VIEW);
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");
    /** Buổi hoạt động sắp tới hiện trong 14 ngày. */
    static final int UPCOMING_DAYS = 14;

    private static final String NO_DATA = "Chưa có dữ liệu";

    /** Lượt thăm Đã duyệt: ngày, khung giờ, người thăm (người đăng ký và người đi cùng). */
    public record ChannelVisit(long visitId, LocalDate date, String startTime, String endTime, List<String> visitors) {}

    /** Buổi hoạt động của tầng mình hoặc toàn viện, mình đã có tên chưa. */
    public record ChannelActivity(
            long sessionId, String name, Instant startAt, Instant endAt, boolean facilityWide, boolean registered) {}

    /** Đồ gửi Đang giữ hoặc Đang được sử dụng. */
    public record ChannelBelonging(long belongingId, String name, String status, Long photoFileId) {}

    /** Lịch đến/về của người bán trú trong ngày. */
    public record ChannelDayCare(LocalDate date, String arrivalTime, String departureTime) {}

    /** Hôm nay; {@code unavailable} là phần chưa có nguồn. */
    public record Today(
            LocalDate date,
            List<ChannelVisit> visits,
            List<ChannelActivity> activities,
            List<ChannelDayCare> dayCareSchedule,
            List<String> unavailable) {}

    /** Một phần của kênh: chưa có nguồn thì {@code available = false} kèm câu cho người xem. */
    public record Section<T>(boolean available, String note, List<T> items) {

        static <T> Section<T> of(List<T> items) {
            return new Section<>(true, null, items);
        }

        static <T> Section<T> missing() {
            return new Section<>(false, NO_DATA, List.of());
        }
    }

    private final VisitQueries visits;
    private final RelationshipQueries relationships;
    private final ObjectProvider<ResidentActivityReadApi> activities;
    private final ObjectProvider<ResidentChannelBelongingPort> belongings;
    private final ObjectProvider<ContractReadApi> contracts;
    private final PermissionApi permissions;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;

    ResidentChannelService(
            VisitQueries visits,
            RelationshipQueries relationships,
            ObjectProvider<ResidentActivityReadApi> activities,
            ObjectProvider<ResidentChannelBelongingPort> belongings,
            ObjectProvider<ContractReadApi> contracts,
            PermissionApi permissions,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser) {
        this.visits = visits;
        this.relationships = relationships;
        this.activities = activities;
        this.belongings = belongings;
        this.contracts = contracts;
        this.permissions = permissions;
        this.time = time;
        this.currentUser = currentUser;
    }

    /** Người cao tuổi của tài khoản đăng nhập; tài khoản khác hoặc không có quyền xem kênh thì 403 (FR-084). */
    public long self() {
        CurrentUser user = currentUser.getObject();
        if (user.type() != AccountType.RESIDENT
                || user.residentId().isEmpty()
                || !permissions.permissions(user).contains(VIEW)) {
            throw new PermissionDeniedException(
                    "PERMISSION_DENIED", "Chỉ tài khoản người cao tuổi xem kênh này", Map.of("failedLayer", "ROLE"));
        }
        return user.residentId().get();
    }

    @Transactional(readOnly = true)
    public Today today() {
        long residentId = self();
        LocalDate today = time.today();
        List<String> unavailable = new ArrayList<>();
        List<ChannelVisit> todayVisits = channelVisits(residentId, today).stream()
                .filter(v -> v.date().equals(today))
                .toList();
        List<ChannelActivity> todayActivities = new ArrayList<>();
        ResidentActivityReadApi port = activities.getIfAvailable();
        if (port == null) {
            unavailable.add("activities");
        } else {
            todayActivities.addAll(activityViews(port, residentId, today, today));
        }
        List<ChannelDayCare> dayCare = new ArrayList<>();
        ContractReadApi reads = contracts.getIfAvailable();
        if (reads != null) {
            reads.attendanceOn(residentId, today)
                    .forEach(s -> dayCare.add(new ChannelDayCare(
                            s.date(),
                            s.arrivalTime().format(HOUR),
                            s.departureTime().format(HOUR))));
        }
        return new Today(today, todayVisits, todayActivities, dayCare, unavailable);
    }

    /** Thực đơn tuần đã công bố và suất ăn dự kiến (FR-083 (b)): 011 chưa có {@code SuatAnDuKienApi}. */
    public Section<Object> menu() {
        self();
        return Section.missing();
    }

    @Transactional(readOnly = true)
    public Section<ChannelActivity> upcomingActivities() {
        long residentId = self();
        ResidentActivityReadApi port = activities.getIfAvailable();
        if (port == null) {
            return Section.missing();
        }
        LocalDate today = time.today();
        return Section.of(activityViews(port, residentId, today, today.plusDays(UPCOMING_DAYS)));
    }

    @Transactional(readOnly = true)
    public Section<ChannelVisit> upcomingVisits() {
        long residentId = self();
        return Section.of(channelVisits(residentId, time.today()));
    }

    @Transactional(readOnly = true)
    public Section<ChannelBelonging> heldBelongings() {
        long residentId = self();
        ResidentChannelBelongingPort port = belongings.getIfAvailable();
        if (port == null) {
            return Section.missing();
        }
        return Section.of(port.held(residentId).stream()
                .map(b -> new ChannelBelonging(
                        b.belongingId(), b.name(), b.status(), b.photoFileId().orElse(null)))
                .toList());
    }

    private List<ChannelVisit> channelVisits(long residentId, LocalDate from) {
        return visits.approvedFrom(residentId, from).stream()
                .map(v -> {
                    Visit visit = v.content();
                    List<String> people = new ArrayList<>();
                    relationships
                            .familyMember(visit.familyMemberId())
                            .map(FamilyMember::fullName)
                            .ifPresent(people::add);
                    visit.companions().forEach(c -> people.add(c.fullName()));
                    return new ChannelVisit(
                            v.id(),
                            visit.date(),
                            visit.startTime().format(HOUR),
                            visit.endTime().format(HOUR),
                            people);
                })
                .toList();
    }

    private static List<ChannelActivity> activityViews(
            ResidentActivityReadApi port, long residentId, LocalDate from, LocalDate to) {
        return port.upcoming(residentId, from, to).stream()
                .map(s -> new ChannelActivity(
                        s.sessionId(), s.name(), s.startAt(), s.endAt(), s.facilityWide(), s.registered()))
                .toList();
    }
}
