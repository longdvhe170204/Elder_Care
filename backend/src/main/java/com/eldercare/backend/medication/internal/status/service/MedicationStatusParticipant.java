package com.eldercare.backend.medication.internal.status.service;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.internal.dose.domain.DoseAuthorityBasis;
import com.eldercare.backend.medication.internal.dose.domain.DoseCancellationReason;
import com.eldercare.backend.medication.internal.dose.domain.DoseResult;
import com.eldercare.backend.medication.internal.dose.domain.DoseResultType;
import com.eldercare.backend.medication.internal.dose.domain.DoseStatus;
import com.eldercare.backend.medication.internal.dose.domain.MedicationDose;
import com.eldercare.backend.medication.internal.dose.persistence.DoseResultRepository;
import com.eldercare.backend.medication.internal.dose.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.family.domain.FamilyMedicationBatchStatus;
import com.eldercare.backend.medication.internal.family.persistence.FamilyMedicationBatchRepository;
import com.eldercare.backend.medication.internal.handover.domain.AbsenceIntervalType;
import com.eldercare.backend.medication.internal.handover.domain.HandoverStatus;
import com.eldercare.backend.medication.internal.handover.domain.MedicationAbsenceInterval;
import com.eldercare.backend.medication.internal.handover.domain.MedicationHandover;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationAbsenceIntervalRepository;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationHandoverRepository;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.prescription.domain.TerminationReason;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.reconciliation.domain.MedicationReconciliation;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationReason;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationStatus;
import com.eldercare.backend.medication.internal.reconciliation.persistence.MedicationReconciliationRepository;
import com.eldercare.backend.medication.internal.reconciliation.service.ReconciliationDeadlines;
import com.eldercare.backend.resident.api.dto.CommandContext;
import com.eldercare.backend.resident.api.dto.Condition;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import com.eldercare.backend.resident.api.spi.StatusTransitionParticipant;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/**
 * Tác động của 006 trong lệnh trạng thái của 001 (T042, research D-15, D-16; FR-021, FR-047, FR-048, FR-048b): khoảng
 * vắng của thuốc mở và đóng theo lệnh (lịch thuốc tính Tạm dừng, Mang theo từ khoảng này), trở về khôi phục liều đã vật
 * hóa và chuyển lần giao sang chờ nhận lại, trạng thái cuối ngừng đơn và đóng liều. Chạy sau 005 ({@code @Order(30)}).
 * Hoàn tất tiếp nhận và trở về từ bệnh viện chuyển cho {@link ReconciliationDeadlines} (T037, research D-13).
 */
@Component
@Order(40)
public class MedicationStatusParticipant implements StatusTransitionParticipant {

    static final String FAMILY_MEDICATION_CONDITION = "KT_C_THUOC_GUI";

    private static final Set<StatusCommand> FINAL =
            EnumSet.of(StatusCommand.END_RESIDENCY, StatusCommand.RECORD_DEATH, StatusCommand.CANCEL_ADMISSION);
    private static final Set<StatusCommand> COMMANDS = EnumSet.of(
            StatusCommand.GRANT_TEMPORARY_ABSENCE,
            StatusCommand.TRANSFER_OUT,
            StatusCommand.RECORD_RETURN,
            StatusCommand.ROLL_CALL_DEPARTURE,
            StatusCommand.ROLL_CALL_RETURN,
            StatusCommand.CANCEL_DEPARTURE_RECORD,
            StatusCommand.CANCEL_RETURN_RECORD,
            StatusCommand.COMPLETE_ADMISSION,
            StatusCommand.END_RESIDENCY,
            StatusCommand.RECORD_DEATH,
            StatusCommand.CANCEL_ADMISSION);
    private static final List<FamilyMedicationBatchStatus> HELD_BATCHES = List.of(
            FamilyMedicationBatchStatus.PENDING_RECONCILIATION,
            FamilyMedicationBatchStatus.IN_USE,
            FamilyMedicationBatchStatus.HOLD_ONLY);
    private static final Set<DoseStatus> AWAY = EnumSet.of(DoseStatus.SUSPENDED, DoseStatus.LEAVE_MEDICATION);
    private static final Set<DoseStatus> OPEN_DOSES =
            EnumSet.of(DoseStatus.SUSPENDED, DoseStatus.LEAVE_MEDICATION, DoseStatus.DUE, DoseStatus.LATE);
    private static final Set<PrescriptionStatus> UNENDED =
            EnumSet.of(PrescriptionStatus.ACTIVE, PrescriptionStatus.SUSPENDED, PrescriptionStatus.PENDING_EFFECTIVE);
    private static final Set<ReconciliationStatus> OPEN_TICKETS =
            EnumSet.of(ReconciliationStatus.OPEN, ReconciliationStatus.PENDING_CONFIRMATION);

    private final MedicationAbsenceIntervalRepository absences;
    private final MedicationDoseRepository doses;
    private final DoseResultRepository results;
    private final PrescriptionRepository prescriptions;
    private final MedicationHandoverRepository handovers;
    private final MedicationReconciliationRepository tickets;
    private final FamilyMedicationBatchRepository batches;
    private final BusinessTime time;

    private final ReconciliationDeadlines deadlines;

    public MedicationStatusParticipant(
            MedicationAbsenceIntervalRepository absences,
            MedicationDoseRepository doses,
            DoseResultRepository results,
            PrescriptionRepository prescriptions,
            MedicationHandoverRepository handovers,
            MedicationReconciliationRepository tickets,
            FamilyMedicationBatchRepository batches,
            BusinessTime time,
            ReconciliationDeadlines deadlines) {
        this.deadlines = deadlines;
        this.absences = absences;
        this.doses = doses;
        this.results = results;
        this.prescriptions = prescriptions;
        this.handovers = handovers;
        this.tickets = tickets;
        this.batches = batches;
        this.time = time;
    }

    @Override
    public String module() {
        return "006";
    }

    @Override
    public Set<StatusCommand> interestedCommands() {
        return COMMANDS;
    }

    /** Kết thúc lưu trú chờ thuốc gia đình gửi được hoàn trả, hủy và thuốc mang theo được nhận lại (D-14, Q-64). */
    @Override
    public List<Condition> checkConditions(CommandContext context) {
        if (context.command() != StatusCommand.END_RESIDENCY) {
            return List.of();
        }
        boolean held = !batches.findByResidentIdAndStatusIn(context.residentId(), HELD_BATCHES)
                        .isEmpty()
                || !handovers
                        .findByResidentIdAndStatusIn(
                                context.residentId(),
                                List.of(HandoverStatus.HANDED_OVER, HandoverStatus.WAITING_RETURN))
                        .isEmpty();
        return held
                ? List.of(new Condition(
                        FAMILY_MEDICATION_CONDITION,
                        "Còn thuốc gia đình gửi chưa hoàn trả, hủy hoặc thuốc mang theo chưa nhận lại",
                        "FR-047",
                        true))
                : List.of();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void applyEffects(TransitionResult result) {
        long resident = result.residentId();
        JsonNode details = result.details();
        Instant at = instant(details, "at").orElse(result.at());
        switch (result.command()) {
            case GRANT_TEMPORARY_ABSENCE ->
                open(new MedicationAbsenceInterval(
                        resident,
                        bool(details, "takesMedication")
                                ? AbsenceIntervalType.TEMPORARY_LEAVE_WITH_MED
                                : AbsenceIntervalType.TEMPORARY_LEAVE_NO_MED,
                        at,
                        instant(details, "expectedReturnAt").orElse(null),
                        null,
                        null,
                        time.now()));
            case COMPLETE_ADMISSION -> deadlines.completeAdmission(resident, at);
            case TRANSFER_OUT -> {
                absences.findOpenByResidentId(resident).ifPresent(interval -> close(interval, at));
                open(new MedicationAbsenceInterval(
                        resident,
                        AbsenceIntervalType.HOSPITAL,
                        at,
                        instant(details, "expectedReturnAt").orElse(null),
                        null,
                        null,
                        time.now()));
                if (result.from() != ResidentStatus.ADMISSION_IN_PROGRESS) {
                    cancelOpenTickets(resident, "Chuyển viện lại");
                }
            }
            case RECORD_RETURN -> {
                Instant returnedAt = instant(details, "returnedAt").orElse(at);
                absences.findOpenByResidentId(resident).ifPresent(interval -> close(interval, returnedAt));
                if (result.from() == ResidentStatus.HOSPITALIZED) {
                    deadlines.begin(resident, ReconciliationReason.HOSPITAL_RETURN, returnedAt);
                } else {
                    resumeDoses(resident);
                    awaitReturn(handovers.findByResidentIdAndStatusIn(resident, List.of(HandoverStatus.HANDED_OVER)));
                }
            }
            case ROLL_CALL_DEPARTURE -> {
                long trip = details.get("tripId").asLong();
                if (absences.findByTripId(trip).isEmpty()) {
                    open(new MedicationAbsenceInterval(
                            resident,
                            AbsenceIntervalType.TRIP_WITH_MED,
                            at,
                            instant(details, "expectedReturnAt").orElse(null),
                            null,
                            trip,
                            time.now()));
                }
            }
            case ROLL_CALL_RETURN -> {
                long trip = details.get("tripId").asLong();
                absences.findByTripId(trip)
                        .filter(MedicationAbsenceInterval::isOpen)
                        .ifPresent(i -> close(i, at));
                resumeDoses(resident);
                awaitReturn(handovers.findByTripId(trip).stream()
                        .filter(h -> h.getStatus() == HandoverStatus.HANDED_OVER)
                        .toList());
            }
            case CANCEL_RETURN_RECORD -> {
                // 014 FR-031a (c): ghi về nhầm, người cao tuổi vẫn ở ngoài; khoảng mang thuốc của chuyến mở lại
                long trip = details.get("tripId").asLong();
                absences.findByTripId(trip)
                        .filter(i -> !i.isOpen() && i.getAbsenceType() == AbsenceIntervalType.TRIP_WITH_MED)
                        .ifPresent(i -> {
                            i.reopen();
                            absences.save(i);
                        });
            }
            case CANCEL_DEPARTURE_RECORD -> {
                long trip = details.get("tripId").asLong();
                absences.findByTripId(trip)
                        .filter(i -> i.isOpen() && i.getAbsenceType() == AbsenceIntervalType.TRIP_WITH_MED)
                        .ifPresent(i -> close(i, i.getFromTime()));
                resumeDoses(resident);
            }
            default -> {
                if (FINAL.contains(result.command())) {
                    finalStatus(resident);
                }
            }
        }
    }

    private void open(MedicationAbsenceInterval interval) {
        absences.save(interval);
    }

    private void close(MedicationAbsenceInterval interval, Instant at) {
        interval.close(at);
        absences.save(interval);
    }

    /**
     * Trở về (FR-048, D-15): liều Tạm dừng của đơn Hiệu lực và liều Mang theo chưa ghi nhận, cuối cửa sổ chưa qua, quay
     * về Đến giờ hoặc Chưa đến giờ; liều chưa vật hóa tự tính lại từ khoảng vắng đã đóng.
     */
    private void resumeDoses(long resident) {
        Instant now = time.now();
        for (MedicationDose dose : doses.findByResidentIdAndStatusIn(resident, AWAY)) {
            if (dose.getEndedAt() != null || !dose.getWindowTo().isAfter(now)) {
                continue;
            }
            if (dose.getStatus() == DoseStatus.SUSPENDED
                    && prescriptions
                                    .findById(dose.getPrescriptionId())
                                    .map(Prescription::getStatus)
                                    .orElse(null)
                            != PrescriptionStatus.ACTIVE) {
                continue;
            }
            dose.resumeFromSuspension(now);
            doses.save(dose);
        }
    }

    /** Lần giao đang mang theo chuyển chờ nhận lại khi người cao tuổi trở về (FR-048a, D-15). */
    private void awaitReturn(List<MedicationHandover> open) {
        for (MedicationHandover handover : open) {
            handover.markWaitingReturn();
            handovers.save(handover);
        }
    }

    /**
     * Trạng thái cuối (FR-047, D-16): đơn chưa kết thúc → Đã ngừng; liều Tạm dừng, Mang theo → Đã hủy; liều Đến giờ, Trễ
     * → Không thực hiện, người đóng Hệ thống; phiếu đối chiếu mở → Đã hủy. Lô và lần giao giữ nguyên.
     */
    private void finalStatus(long resident) {
        Instant now = time.now();
        for (Prescription prescription : prescriptions.findByResidentIdAndStatusIn(resident, UNENDED)) {
            prescription.discontinue(TerminationReason.FINAL_STATE);
            prescriptions.save(prescription);
        }
        for (MedicationDose dose : doses.findByResidentIdAndStatusIn(resident, OPEN_DOSES)) {
            if (AWAY.contains(dose.getStatus())) {
                dose.cancel(DoseCancellationReason.FINAL_STATE, now);
            } else {
                dose.closeByFinalStatus(now);
                results.save(new DoseResult(
                        dose.getId(),
                        resident,
                        dose.getFloorAreaId(),
                        DoseResultType.NOT_ADMINISTERED,
                        null,
                        null,
                        null,
                        DoseAuthorityBasis.SYSTEM,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "Người cao tuổi sang trạng thái cuối (FR-047)",
                        null,
                        false,
                        false,
                        false,
                        null,
                        now));
            }
            doses.save(dose);
        }
        cancelOpenTickets(resident, "Người cao tuổi sang trạng thái cuối");
    }

    private void cancelOpenTickets(long resident, String reason) {
        for (MedicationReconciliation ticket : tickets.findByResidentIdAndStatusIn(resident, OPEN_TICKETS)) {
            ticket.cancel(reason);
            tickets.save(ticket);
        }
    }

    private static Optional<Instant> instant(JsonNode details, String field) {
        return details != null && details.has(field) && !details.get(field).isNull()
                ? Optional.of(Instant.parse(details.get(field).asString()))
                : Optional.empty();
    }

    private static boolean bool(JsonNode details, String field) {
        return details != null && details.has(field) && details.get(field).asBoolean();
    }
}
