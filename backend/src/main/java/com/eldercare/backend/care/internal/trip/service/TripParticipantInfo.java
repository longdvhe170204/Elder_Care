package com.eldercare.backend.care.internal.trip.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.FieldVisibilityApi;
import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.dto.Visibility;
import com.eldercare.backend.care.api.dto.TripParticipantSources.CarriedMedication;
import com.eldercare.backend.care.api.dto.TripParticipantSources.TripDose;
import com.eldercare.backend.care.api.dto.TripParticipantSources.TripEmergencyCard;
import com.eldercare.backend.care.api.dto.TripParticipantSources.TripHealth;
import com.eldercare.backend.care.api.spi.TripHealthPort;
import com.eldercare.backend.care.api.spi.TripMedicationPreparationPort;
import com.eldercare.backend.care.internal.trip.service.TripPreparationService.Detail;
import com.eldercare.backend.common.Role;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sáu phần thông tin của mỗi người tham gia trên TripDetail (014 FR-036, FR-038, FR-039; đề xuất
 * {@code trip-detail-proposal-2026-11-08.md}, chủ dự án duyệt 2026-11-10): liều trong khoảng đi, thuốc mang theo, sức khỏe
 * đang mở, lưu ý chăm sóc, liên hệ chính, thẻ khẩn cấp. Nguồn ở 006, 007 qua SPI care khai; mỗi phần nói rõ có dữ liệu,
 * không có dữ liệu, nguồn chưa kết nối hay lỗi, hoặc người xem không có quyền. Phần sức khỏe chỉ khi xem đủ nhóm sức khỏe
 * (002 FR-019); số điện thoại liên hệ chỉ Trưởng tầng và trưởng đoàn (FR-039).
 */
@Service
@Transactional(readOnly = true)
public class TripParticipantInfo {

    public enum Availability {
        AVAILABLE,
        NO_DATA,
        SOURCE_UNAVAILABLE,
        NOT_PERMITTED
    }

    public record Section<T>(Availability availability, T data) {
        static <T> Section<T> of(Availability availability) {
            return new Section<>(availability, null);
        }
    }

    public record Contact(String fullName, String phoneNumber) {}

    public record Info(
            Section<java.util.List<TripDose>> medicationDoses,
            Section<CarriedMedication> carriedMedication,
            Section<TripHealth> health,
            Section<String> careNotes,
            Section<Contact> primaryContact,
            Section<TripEmergencyCard> emergencyCard) {}

    private final ObjectProvider<TripMedicationPreparationPort> medication;
    private final ObjectProvider<TripHealthPort> health;
    private final FieldVisibilityApi visibility;
    private final CurrentUser user;

    public TripParticipantInfo(
            ObjectProvider<TripMedicationPreparationPort> medication,
            ObjectProvider<TripHealthPort> health,
            FieldVisibilityApi visibility,
            CurrentUser user) {
        this.medication = medication;
        this.health = health;
        this.visibility = visibility;
        this.user = user;
    }

    /** Thông tin theo người cao tuổi của mọi người tham gia chuyến. */
    public Map<Long, Info> of(Detail detail) {
        var result = new LinkedHashMap<Long, Info>();
        for (var p : detail.participants()) result.put(p.residentId(), info(detail, p.residentId()));
        return result;
    }

    private Info info(Detail detail, long residentId) {
        var trip = detail.trip();
        var plan = trip.plan();
        boolean fullHealth = visibility.canView(
                        user,
                        InfoGroup.HEALTH,
                        new Target("M04.TRIP", Long.toString(trip.id()), residentId, trip.floorAreaId()))
                == Visibility.FULL;
        boolean phone = user.roles().contains(Role.FLOOR_MANAGER)
                || (detail.duties() != null
                        && detail.duties().leaderId() != null
                        && user.staffId()
                                .map(s -> s.equals(detail.duties().leaderId()))
                                .orElse(false));
        var medicationPort = medication.getIfAvailable();
        var healthPort = health.getIfAvailable();

        Section<java.util.List<TripDose>> doses = !fullHealth
                ? Section.of(Availability.NOT_PERMITTED)
                : read(
                        medicationPort,
                        () -> medicationPort.dosesDuring(
                                residentId, plan.plannedDepartureAt(), plan.plannedReturnAt()));
        Section<CarriedMedication> carried = !fullHealth
                ? Section.of(Availability.NOT_PERMITTED)
                : readOptional(medicationPort, () -> medicationPort.carried(residentId, trip.id()));
        Section<TripHealth> healthSection = !fullHealth
                ? Section.of(Availability.NOT_PERMITTED)
                : read(healthPort, () -> healthPort.health(residentId, plan.plannedDepartureAt()));
        // Thẻ dựng một lần với đủ nội dung; care che theo quyền người xem trước khi trả
        Section<TripEmergencyCard> full =
                read(healthPort, () -> healthPort.emergencyCard(residentId, plan.plannedDepartureAt(), true));
        TripEmergencyCard card = full.data();

        Section<String> careNotes;
        Section<Contact> contact;
        Section<TripEmergencyCard> cardSection;
        if (card == null) {
            careNotes = fullHealth ? Section.of(full.availability()) : Section.of(Availability.NOT_PERMITTED);
            contact = Section.of(full.availability());
            cardSection = Section.of(full.availability());
        } else {
            careNotes = !fullHealth
                    ? Section.of(Availability.NOT_PERMITTED)
                    : unavailable(card, "careNotes")
                            ? Section.of(Availability.SOURCE_UNAVAILABLE)
                            : present(card.careNotes());
            contact = unavailable(card, "primaryContact")
                    ? Section.of(Availability.SOURCE_UNAVAILABLE)
                    : card.primaryContactName() == null
                            ? Section.of(Availability.NO_DATA)
                            : new Section<>(
                                    Availability.AVAILABLE,
                                    new Contact(card.primaryContactName(), phone ? card.primaryContactPhone() : null));
            cardSection = new Section<>(Availability.AVAILABLE, fullHealth ? card : identificationOnly(card));
        }
        return new Info(doses, carried, healthSection, careNotes, contact, cardSection);
    }

    private static boolean unavailable(TripEmergencyCard card, String section) {
        return card.unavailable() != null && card.unavailable().contains(section);
    }

    private static TripEmergencyCard identificationOnly(TripEmergencyCard c) {
        return new TripEmergencyCard(
                true,
                c.fullName(),
                c.dateOfBirth(),
                c.roomBed(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                c.snapshotAt());
    }

    private static <T> Section<T> present(T value) {
        return value == null ? Section.of(Availability.NO_DATA) : new Section<>(Availability.AVAILABLE, value);
    }

    private static <T> Section<T> read(Object port, Supplier<T> source) {
        if (port == null) return Section.of(Availability.SOURCE_UNAVAILABLE);
        try {
            T value = source.get();
            if (value == null || (value instanceof Collection<?> c && c.isEmpty()))
                return Section.of(Availability.NO_DATA);
            return new Section<>(Availability.AVAILABLE, value);
        } catch (RuntimeException e) {
            return Section.of(Availability.SOURCE_UNAVAILABLE);
        }
    }

    private static <T> Section<T> readOptional(Object port, Supplier<Optional<T>> source) {
        Section<Optional<T>> section = read(port, source);
        if (section.data() == null) return Section.of(section.availability());
        return section.data()
                .map(v -> new Section<>(Availability.AVAILABLE, v))
                .orElseGet(() -> Section.of(Availability.NO_DATA));
    }
}
