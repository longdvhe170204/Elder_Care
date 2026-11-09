package com.eldercare.backend.medication.internal.handover.service;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.dto.TimeInterval;
import com.eldercare.backend.medication.api.spi.TripMedicationPort;
import com.eldercare.backend.medication.internal.catalog.domain.Medication;
import com.eldercare.backend.medication.internal.catalog.persistence.MedicationRepository;
import com.eldercare.backend.medication.internal.dose.domain.DoseStatus;
import com.eldercare.backend.medication.internal.dose.domain.MedicationDose;
import com.eldercare.backend.medication.internal.dose.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.handover.domain.AbsenceIntervalType;
import com.eldercare.backend.medication.internal.handover.domain.CreateHandoverBody;
import com.eldercare.backend.medication.internal.handover.domain.HandoverItemBody;
import com.eldercare.backend.medication.internal.handover.domain.HandoverResponse;
import com.eldercare.backend.medication.internal.handover.domain.HandoverStatus;
import com.eldercare.backend.medication.internal.handover.domain.MedicationAbsenceInterval;
import com.eldercare.backend.medication.internal.handover.domain.MedicationHandover;
import com.eldercare.backend.medication.internal.handover.domain.MedicationHandoverDose;
import com.eldercare.backend.medication.internal.handover.domain.MedicationHandoverItem;
import com.eldercare.backend.medication.internal.handover.domain.MedicationReturn;
import com.eldercare.backend.medication.internal.handover.domain.MedicationReturnItem;
import com.eldercare.backend.medication.internal.handover.domain.MedicationToBringResponse;
import com.eldercare.backend.medication.internal.handover.domain.NoMedicationOrder;
import com.eldercare.backend.medication.internal.handover.domain.NoMedicationOrderBody;
import com.eldercare.backend.medication.internal.handover.domain.RecipientType;
import com.eldercare.backend.medication.internal.handover.domain.ReturnHandoverBody;
import com.eldercare.backend.medication.internal.handover.domain.ReturnItemBody;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationAbsenceIntervalRepository;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationHandoverDoseRepository;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationHandoverItemRepository;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationHandoverRepository;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationReturnRepository;
import com.eldercare.backend.medication.internal.handover.persistence.NoMedicationOrderRepository;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionType;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.reconciliation.persistence.ResidentMedicationLock;
import com.eldercare.backend.medication.internal.schedule.service.CalculatedDoseSlot;
import com.eldercare.backend.medication.internal.schedule.service.MedicationScheduleCalculator;
import com.eldercare.backend.medication.internal.schedule.service.ScheduleContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business service for leave medications and absence intervals (FR-021, FR-027, FR-047, FR-048 -> FR-048b, BR-M07-03).
 */
@Service
public class MedicationHandoverService {

    private final MedicationAbsenceIntervalRepository absenceRepo;
    private final MedicationHandoverRepository handoverRepo;
    private final MedicationHandoverItemRepository handoverItemRepo;
    private final MedicationHandoverDoseRepository handoverDoseRepo;
    private final MedicationReturnRepository returnRepo;
    private final NoMedicationOrderRepository noMedOrderRepo;
    private final PrescriptionRepository prescriptionRepo;
    private final MedicationRepository medicationRepo;
    private final MedicationDoseRepository doseRepo;
    private final ResidentMedicationLock residentMedicationLock;
    private final BusinessTime businessTime;
    private final MedicationScheduleCalculator scheduleCalculator;
    // 014 cài khi có chuyến đi; chưa có thì giờ về chỉ lấy từ khoảng vắng
    private final ObjectProvider<TripMedicationPort> trips;

    @Autowired
    public MedicationHandoverService(
            MedicationAbsenceIntervalRepository absenceRepo,
            MedicationHandoverRepository handoverRepo,
            MedicationHandoverItemRepository handoverItemRepo,
            MedicationHandoverDoseRepository handoverDoseRepo,
            MedicationReturnRepository returnRepo,
            NoMedicationOrderRepository noMedOrderRepo,
            PrescriptionRepository prescriptionRepo,
            MedicationRepository medicationRepo,
            @Autowired(required = false) MedicationDoseRepository doseRepo,
            ResidentMedicationLock residentMedicationLock,
            BusinessTime businessTime,
            MedicationScheduleCalculator scheduleCalculator,
            ObjectProvider<TripMedicationPort> trips) {
        this.absenceRepo = absenceRepo;
        this.handoverRepo = handoverRepo;
        this.handoverItemRepo = handoverItemRepo;
        this.handoverDoseRepo = handoverDoseRepo;
        this.returnRepo = returnRepo;
        this.noMedOrderRepo = noMedOrderRepo;
        this.prescriptionRepo = prescriptionRepo;
        this.medicationRepo = medicationRepo;
        this.doseRepo = doseRepo;
        this.residentMedicationLock = residentMedicationLock;
        this.businessTime = businessTime;
        this.scheduleCalculator = scheduleCalculator;
        this.trips = trips;
    }

    @Transactional(readOnly = true)
    public List<MedicationToBringResponse> getMedicationsToBring(Long residentId, Long tripId) {
        // Find associated absence interval
        Optional<MedicationAbsenceInterval> intervalOpt =
                tripId != null ? absenceRepo.findByTripId(tripId) : absenceRepo.findOpenByResidentId(residentId);

        Instant fromTime =
                intervalOpt.map(MedicationAbsenceInterval::getFromTime).orElseGet(businessTime::now);
        // Giờ về: của khoảng vắng, không có thì của chuyến (014); không biết thì không tính số lượng
        Instant toTime = intervalOpt
                .map(MedicationAbsenceInterval::getExpectedToTime)
                .or(() -> tripEnd(tripId))
                .orElse(null);

        List<Prescription> prescriptions =
                prescriptionRepo.findByResidentIdAndStatusIn(residentId, List.of(PrescriptionStatus.ACTIVE));

        List<MedicationToBringResponse> result = new ArrayList<>();
        for (Prescription p : prescriptions) {
            MedicationToBringResponse res = new MedicationToBringResponse();
            res.setPrescriptionId(p.getId());
            res.setDosage(p.getDosage());
            res.setDosageUnit(p.getDosageUnit());
            res.setPrescriptionType(p.getPrescriptionType().name());
            res.setMedicationSource(p.getMedicationSource());

            Medication med = medicationRepo.findById(p.getMedicationId()).orElse(null);
            res.setMedicationName(med != null ? med.getName() : "Medication ID=" + p.getMedicationId());

            // Đơn định kỳ: liều dự kiến trong khoảng vắng; đơn PRN không có lịch nên người giao tự định số lượng
            if (p.getPrescriptionType() == PrescriptionType.PERIODIC && toTime != null) {
                List<Instant> times = scheduleCalculator
                        .calculate(p, med, fromTime, toTime, businessTime.now(), ScheduleContext.empty(residentId))
                        .stream()
                        .map(CalculatedDoseSlot::scheduledTime)
                        .toList();
                res.setScheduledTimes(new ArrayList<>(times));
                res.setTotalQuantityNeeded(p.getDosage().multiply(BigDecimal.valueOf(times.size())));
            }

            result.add(res);
        }

        return result;
    }

    private Optional<Instant> tripEnd(Long tripId) {
        TripMedicationPort port = trips.getIfAvailable();
        if (tripId == null || port == null) {
            return Optional.empty();
        }
        return port.tripInterval(tripId).map(TimeInterval::to);
    }

    @Transactional
    public HandoverResponse createHandover(Long residentId, CreateHandoverBody body, Long currentUserId) {
        residentMedicationLock.lock(residentId);

        RecipientType recipientType = parseRecipientType(body.getRecipientType());
        if (recipientType == RecipientType.RELATIVE) {
            if ((body.getRecipientName() == null || body.getRecipientName().isBlank())
                    && (body.getRelationship() == null || body.getRelationship().isBlank())) {
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "INVALID_VALUE",
                        "Handover to family relative requires recipient name or relationship");
            }
        }

        // Find or open absence interval
        Optional<MedicationAbsenceInterval> intervalOpt = body.getTripId() != null
                ? absenceRepo.findByTripId(body.getTripId())
                : absenceRepo.findOpenByResidentId(residentId);

        Long absenceIntervalId =
                intervalOpt.map(MedicationAbsenceInterval::getId).orElse(null);

        MedicationHandover handover = new MedicationHandover(
                residentId,
                absenceIntervalId,
                body.getTripId(),
                recipientType,
                body.getRecipientId(),
                body.getRecipientName(),
                body.getRelationship(),
                currentUserId,
                businessTime.now(),
                businessTime.now());

        MedicationHandover saved = handoverRepo.save(handover);
        List<MedicationHandoverItem> savedItems = new ArrayList<>();

        for (HandoverItemBody ib : body.getItems()) {
            Prescription p = prescriptionRepo.findById(ib.getPrescriptionId()).orElseThrow(NotFoundException::new);
            if (!p.getResidentId().equals(residentId)) {
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_VALUE", "Prescription does not belong to resident");
            }

            MedicationHandoverItem item = new MedicationHandoverItem(
                    saved.getId(),
                    p.getId(),
                    ib.getQuantity(),
                    ib.getFamilyMedicationBatchId(),
                    p.getMedicationSource() != null ? p.getMedicationSource().name() : "FACILITY");

            savedItems.add(handoverItemRepo.save(item));

            // Transition open doses during absence to LEAVE_MEDICATION (DoseStatus.LEAVE_MEDICATION)
            if (doseRepo != null) {
                List<MedicationDose> openDoses = doseRepo.findByPrescriptionIdAndStatusIn(
                        p.getId(), List.of(DoseStatus.UPCOMING, DoseStatus.DUE));
                for (MedicationDose d : openDoses) {
                    d.markLeaveMedication(businessTime.now());
                    doseRepo.save(d);

                    MedicationHandoverDose hd = new MedicationHandoverDose(saved.getId(), d.getId());
                    handoverDoseRepo.save(hd);
                }
            }
        }

        return toResponse(saved, savedItems);
    }

    @Transactional(readOnly = true)
    public List<HandoverResponse> listHandovers(Long residentId) {
        List<MedicationHandover> handovers = handoverRepo.findByResidentId(residentId);
        return handovers.stream()
                .map(h -> {
                    List<MedicationHandoverItem> items = handoverItemRepo.findByHandoverId(h.getId());
                    return toResponse(h, items);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public HandoverResponse getHandover(Long id) {
        MedicationHandover h = handoverRepo.findById(id).orElseThrow(NotFoundException::new);
        List<MedicationHandoverItem> items = handoverItemRepo.findByHandoverId(id);
        return toResponse(h, items);
    }

    @Transactional
    public HandoverResponse returnHandover(Long handoverId, ReturnHandoverBody body, Long currentUserId) {
        MedicationHandover handover = handoverRepo.findById(handoverId).orElseThrow(NotFoundException::new);
        residentMedicationLock.lock(handover.getResidentId());

        if (handover.getStatus() == HandoverStatus.RETURNED) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "FINAL_STATE", "Medication handover has already been returned");
        }

        List<MedicationHandoverItem> handedItems = handoverItemRepo.findByHandoverId(handoverId);
        Map<Long, BigDecimal> handedQtyMap = new HashMap<>();
        for (MedicationHandoverItem hi : handedItems) {
            handedQtyMap.put(hi.getPrescriptionId(), hi.getQuantity());
        }

        BigDecimal totalReturned = BigDecimal.ZERO;
        for (ReturnItemBody ri : body.getItems()) {
            BigDecimal handedQty = handedQtyMap.getOrDefault(ri.getPrescriptionId(), BigDecimal.ZERO);
            if (ri.getQuantity().compareTo(handedQty) > 0) {
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "INVALID_QUANTITY",
                        "Return quantity (" + ri.getQuantity() + ") cannot exceed handed over quantity (" + handedQty
                                + ") (FR-048a)");
            }
            totalReturned = totalReturned.add(ri.getQuantity());
        }

        // If total returned quantity is 0, reason is required (FR-048a)
        if (totalReturned.compareTo(BigDecimal.ZERO) == 0) {
            if (body.getReason() == null || body.getReason().isBlank()) {
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "INVALID_VALUE",
                        "Reason is required when returned medication quantity is 0 (FR-048a)");
            }
        }

        Instant now = businessTime.now();
        // Dòng nhận lại khóa ngay khi thêm (006 T069): dựng đủ các dòng trước khi lưu
        List<MedicationReturnItem> returnItems = new ArrayList<>();
        for (ReturnItemBody ri : body.getItems()) {
            MedicationReturnItem returnItem = new MedicationReturnItem(null, ri.getPrescriptionId(), ri.getQuantity());
            returnItem.assignId((long) returnItems.size() + 1);
            returnItems.add(returnItem);
        }
        returnRepo.save(new MedicationReturn(handover, currentUserId, now, body.getReason(), returnItems, now));

        handover.markReturned(now);
        MedicationHandover savedHandover = handoverRepo.save(handover);

        return toResponse(savedHandover, handedItems);
    }

    @Transactional
    public NoMedicationOrder orderNoMedication(Long residentId, NoMedicationOrderBody body, Long currentUserId) {
        residentMedicationLock.lock(residentId);

        if (noMedOrderRepo
                .findByResidentIdAndTripId(residentId, body.getTripId())
                .isPresent()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "DA_TON_TAI",
                    "Resident already has a no-medication order for this trip (FR-048b)");
        }

        // Chỉ người được đánh giá Đạt cho chuyến (014); liều trong khoảng đi dự kiến thành Tạm dừng (FR-048b, Q-171)
        TripMedicationPort port = trips.getIfAvailable();
        TimeInterval tripInterval = (port == null
                        ? Optional.<TimeInterval>empty()
                        : port.approvedTripInterval(residentId, body.getTripId()))
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.CONFLICT,
                        "CONDITION_NOT_MET",
                        "Resident is not assessed eligible for this trip (FR-048b)"));

        NoMedicationOrder order = new NoMedicationOrder(
                residentId, body.getTripId(), body.getReason(), currentUserId, businessTime.now());
        NoMedicationOrder savedOrder = noMedOrderRepo.save(order);

        // Update or create absence interval of type TRIP_NO_MED
        Optional<MedicationAbsenceInterval> intervalOpt = absenceRepo.findByTripId(body.getTripId());
        if (intervalOpt.isPresent()) {
            MedicationAbsenceInterval interval = intervalOpt.get();
            interval.attachNoMedicationOrder(savedOrder.getId());
            absenceRepo.save(interval);
        } else {
            MedicationAbsenceInterval interval = new MedicationAbsenceInterval(
                    residentId,
                    AbsenceIntervalType.TRIP_NO_MED,
                    tripInterval.from(),
                    tripInterval.to(),
                    null,
                    body.getTripId(),
                    businessTime.now());
            interval.attachNoMedicationOrder(savedOrder.getId());
            absenceRepo.save(interval);
        }

        return savedOrder;
    }

    // --- Helpers ---

    private RecipientType parseRecipientType(String typeStr) {
        if (typeStr == null) {
            return RecipientType.STAFF;
        }
        return switch (typeStr.toUpperCase()) {
            case "STAFF", "NHAN_VIEN" -> RecipientType.STAFF;
            case "TRIP_LEADER", "TRUONG_DOAN" -> RecipientType.TRIP_LEADER;
            case "RELATIVE", "NGUOI_THAN" -> RecipientType.RELATIVE;
            default ->
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_VALUE", "Invalid recipient type: " + typeStr);
        };
    }

    private HandoverResponse toResponse(MedicationHandover h, List<MedicationHandoverItem> items) {
        HandoverResponse res = new HandoverResponse();
        res.setId(h.getId());
        res.setResidentId(h.getResidentId());
        res.setTripId(h.getTripId());
        res.setAbsenceIntervalId(h.getAbsenceIntervalId());
        res.setRecipientType(h.getRecipientType().name());
        res.setRecipientId(h.getRecipientId());
        res.setRecipientName(h.getRecipientName());
        res.setRelationship(h.getRelationship());
        res.setDispensedById(h.getDispensedById());
        res.setHandoverTime(h.getHandoverTime());
        res.setStatus(h.getStatus().name());
        if (doseRepo != null && h.getId() != null) {
            res.setDoses(handoverDoseRepo.findByHandoverId(h.getId()).stream()
                    .map(link -> doseRepo.findById(link.getDoseId())
                            .map(d -> new HandoverResponse.HandoverDoseResponse(
                                    d.getPrescriptionId() + "-"
                                            + MedicationScheduleCalculator.DOSE_KEY_FORMATTER.format(
                                                    d.getScheduledTime().atZone(MedicationScheduleCalculator.ZONE_VN)),
                                    link.getRevertedAt() != null))
                            .orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .toList());
        }

        List<HandoverResponse.HandoverItemResponse> itemResponses = new ArrayList<>();
        if (items != null) {
            for (MedicationHandoverItem item : items) {
                if (item == null) {
                    continue;
                }
                HandoverResponse.HandoverItemResponse ir = new HandoverResponse.HandoverItemResponse();
                ir.setId(item.getId());
                ir.setPrescriptionId(item.getPrescriptionId());
                ir.setQuantity(item.getQuantity());
                ir.setFamilyMedicationBatchId(item.getFamilyMedicationBatchId());
                itemResponses.add(ir);
            }
        }
        res.setItems(itemResponses);

        return res;
    }
}
