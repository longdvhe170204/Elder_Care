package com.cambae.backend.m02.internal.absence.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.api.ContractReadApi;
import com.cambae.backend.m02.api.dto.ContractContentView;
import com.cambae.backend.m02.internal.absence.domain.Absence;
import com.cambae.backend.m02.internal.absence.domain.AbsenceDay;
import com.cambae.backend.m02.internal.absence.domain.AbsenceType;
import com.cambae.backend.m02.internal.absence.domain.BedHoldChoice;
import com.cambae.backend.m02.internal.absence.domain.BedHoldDecision;
import com.cambae.backend.m02.internal.absence.persistence.AbsenceStore;
import com.cambae.backend.m02.internal.absence.service.AbsencePolicyResolver.Outcome;
import com.cambae.backend.m02.internal.contract.domain.ContractContent.AbsencePolicyLine;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/**
 * Tính và ghi {@code absence_day} cho một lượt vắng (FR-052, FR-056, FR-058, FR-061; research T-08). Mỗi ngày tra chính sách
 * theo nội dung hợp đồng hiệu lực tại ngày đó và bảng CFG-M02-05 hiện hành lúc tính; quyết định giữ giường thay hệ số của
 * dòng chính sách cho các ngày nó áp dụng. Ngày đã ghi không đổi khi bảng đổi: chỉ {@link #fillMissing} (ngày còn thiếu) và
 * {@link #recalculate} (quyết định, đính chính) mới ghi.
 */
@Component
class AbsenceDayCalculator {

    /** Kết quả một lần ghi: ngày đã ghi và các ngày mới mang dấu "không có dòng chính sách" hoặc "chờ quyết định". */
    record Written(List<AbsenceDay> days, List<LocalDate> newNoLine, List<LocalDate> newPending) {

        static final Written NONE = new Written(List.of(), List.of(), List.of());

        boolean isEmpty() {
            return days.isEmpty();
        }
    }

    private final AbsenceStore store;
    private final ContractReadApi contracts;
    private final ParameterApi parameters;
    private final BusinessTime time;

    AbsenceDayCalculator(AbsenceStore store, ContractReadApi contracts, ParameterApi parameters, BusinessTime time) {
        this.store = store;
        this.contracts = contracts;
        this.parameters = parameters;
        this.time = time;
    }

    static LocalDate dateOf(Instant instant) {
        return instant.atZone(BusinessTime.ZONE).toLocalDate();
    }

    /** Ngày vắng cuối cùng của lượt: ngày về, trừ một ngày khi lượt sau mở cùng thời điểm (FR-057); lượt mở thì {@code upTo}. */
    LocalDate lastDay(Absence absence, LocalDate upTo) {
        if (absence.returnedAt() == null) {
            return upTo;
        }
        LocalDate last = dateOf(absence.returnedAt());
        return store.successorDepartedAt(absence.id()).isPresent() ? last.minusDays(1) : last;
    }

    /** Ghi các ngày còn thiếu của lượt tới {@code upTo} (hoặc tới ngày về); không đụng ngày đã ghi. */
    @Transactional(propagation = Propagation.MANDATORY)
    Written fillMissing(Absence absence, LocalDate upTo, List<BedHoldDecision> decisions) {
        LocalDate first = dateOf(absence.departedAt());
        LocalDate last = lastDay(absence, upTo);
        if (last.isBefore(first)) {
            return Written.NONE;
        }
        List<AbsenceDay> existing = store.days(absence.id());
        LocalDate from = existing.isEmpty() ? first : existing.getLast().date().plusDays(1);
        if (from.isAfter(last)) {
            return Written.NONE;
        }
        return write(absence, from, last, existing, decisions);
    }

    /**
     * Tính lại các ngày từ {@code from} tới ngày cuối của lượt (hoặc {@code upTo} khi còn mở) theo quyết định và thời điểm
     * hiện hành; xóa các ngày nằm ngoài khoảng mới (đính chính rút ngắn hoặc dời).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    Written recalculate(Absence absence, LocalDate from, LocalDate upTo, List<BedHoldDecision> decisions) {
        LocalDate first = dateOf(absence.departedAt());
        LocalDate last = lastDay(absence, upTo);
        store.deleteDaysAfter(absence.id(), last.isBefore(first) ? first.minusDays(1) : last);
        store.deleteDaysBefore(absence.id(), first);
        if (last.isBefore(first)) {
            return Written.NONE;
        }
        LocalDate start = from.isBefore(first) ? first : from;
        if (start.isAfter(last)) {
            return Written.NONE;
        }
        List<AbsenceDay> before = store.days(absence.id()).stream()
                .filter(day -> day.date().isBefore(start))
                .toList();
        return write(absence, start, last, before, decisions);
    }

    private Written write(
            Absence absence, LocalDate from, LocalDate to, List<AbsenceDay> before, List<BedHoldDecision> decisions) {
        LocalDate first = dateOf(absence.departedAt());
        JsonNode table = parameters.read(ParameterCode.CFG_M02_05).raw();
        Instant now = time.now();
        BigDecimal previous = null;
        if (from.isAfter(first) && !before.isEmpty()) {
            previous = before.getLast().coefficient();
        }
        List<AbsenceDay> written = new ArrayList<>();
        List<LocalDate> noLine = new ArrayList<>();
        List<LocalDate> pending = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            int sequenceNo = (int) ChronoUnit.DAYS.between(first, day) + 1;
            AbsenceDay result = dayOf(absence, day, sequenceNo, table, previous, decisions);
            store.saveDay(absence.id(), result, now, now);
            written.add(result);
            if (result.noPolicyLine()) {
                noLine.add(day);
            }
            if (result.pendingDecision()) {
                pending.add(day);
            }
            previous = result.coefficient();
        }
        return new Written(written, noLine, pending);
    }

    private AbsenceDay dayOf(
            Absence absence,
            LocalDate day,
            int sequenceNo,
            JsonNode table,
            BigDecimal previous,
            List<BedHoldDecision> decisions) {
        Optional<ContractContentView> content = contracts.contentAtDate(absence.residentId(), day);
        Outcome base = content.map(view -> AbsencePolicyResolver.resolve(
                        residenceType(view.content()),
                        absence.type(),
                        sequenceNo,
                        policyLines(view.content()),
                        table,
                        previous))
                .orElseGet(AbsencePolicyResolver::noLine);
        BedHoldDecision decision = decisions.stream()
                .filter(d -> d.covers(day))
                .max(Comparator.comparing(BedHoldDecision::appliesFrom).thenComparing(BedHoldDecision::id))
                .orElse(null);
        BigDecimal coefficient = decision == null ? base.coefficient() : decision.coefficient();
        boolean pending = decision == null && base.pendingDecision();
        return new AbsenceDay(
                day,
                sequenceNo,
                coefficient,
                held(absence, decisions, day),
                base.hold().name(),
                base.source().name(),
                base.line(),
                pending,
                base.noPolicyLine(),
                decision == null ? null : decision.id());
    }

    /** Giường đang được giữ vào ngày {@code day}: lúc mở đang giữ và chưa có quyết định Giải phóng từ ngày đó trở về trước. */
    static boolean held(Absence absence, List<BedHoldDecision> decisions, LocalDate day) {
        return absence.initialBedHold()
                && decisions.stream()
                        .noneMatch(d -> d.choice() == BedHoldChoice.RELEASE
                                && !d.appliesFrom().isAfter(day));
    }

    /** Dòng chính sách của ngày vắng thứ nhất: cho biết giường giữ ngay từ lúc rời viện hay không (FR-054, BR-M02-17). */
    Outcome dayOne(long residentId, AbsenceType type, LocalDate day) {
        JsonNode table = parameters.read(ParameterCode.CFG_M02_05).raw();
        return contracts
                .contentAtDate(residentId, day)
                .map(view -> AbsencePolicyResolver.resolve(
                        residenceType(view.content()), type, 1, policyLines(view.content()), table, null))
                .orElseGet(AbsencePolicyResolver::noLine);
    }

    private static ResidenceType residenceType(JsonNode content) {
        return ResidenceType.valueOf(content.get("residenceType").asString());
    }

    static List<AbsencePolicyLine> policyLines(JsonNode content) {
        List<AbsencePolicyLine> lines = new ArrayList<>();
        JsonNode overrides = content.get("absencePolicyOverrides");
        if (overrides != null) {
            for (JsonNode line : overrides) {
                lines.add(new AbsencePolicyLine(
                        ResidenceType.valueOf(line.get("residenceType").asString()),
                        line.get("absenceType").asString(),
                        integer(line.get("fromDay")),
                        integer(line.get("toDay")),
                        line.get("coefficient").asString(),
                        text(line.get("holdBed"))));
            }
        }
        return lines;
    }

    private static Integer integer(JsonNode node) {
        return node == null || node.isNull() ? null : node.asInt();
    }

    private static String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asString();
    }
}
