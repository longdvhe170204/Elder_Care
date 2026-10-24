package com.cambae.backend.m07.internal.service;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.m07.internal.domain.AbsenceIntervalType;
import com.cambae.backend.m07.internal.domain.DoseStatus;
import com.cambae.backend.m07.internal.domain.HandoverStatus;
import com.cambae.backend.m07.internal.domain.Medication;
import com.cambae.backend.m07.internal.domain.MedicationAbsenceInterval;
import com.cambae.backend.m07.internal.domain.MedicationDose;
import com.cambae.backend.m07.internal.domain.MedicationHandover;
import com.cambae.backend.m07.internal.domain.MedicationHandoverDose;
import com.cambae.backend.m07.internal.domain.MedicationHandoverItem;
import com.cambae.backend.m07.internal.domain.MedicationReturn;
import com.cambae.backend.m07.internal.domain.MedicationReturnItem;
import com.cambae.backend.m07.internal.domain.NoMedicationOrder;
import com.cambae.backend.m07.internal.domain.Prescription;
import com.cambae.backend.m07.internal.domain.PrescriptionStatus;
import com.cambae.backend.m07.internal.domain.PrescriptionType;
import com.cambae.backend.m07.internal.domain.RecipientType;
import com.cambae.backend.m07.internal.persistence.MedicationAbsenceIntervalRepository;
import com.cambae.backend.m07.internal.persistence.MedicationDoseRepository;
import com.cambae.backend.m07.internal.persistence.MedicationHandoverDoseRepository;
import com.cambae.backend.m07.internal.persistence.MedicationHandoverItemRepository;
import com.cambae.backend.m07.internal.persistence.MedicationHandoverRepository;
import com.cambae.backend.m07.internal.persistence.MedicationRepository;
import com.cambae.backend.m07.internal.persistence.MedicationReturnItemRepository;
import com.cambae.backend.m07.internal.persistence.MedicationReturnRepository;
import com.cambae.backend.m07.internal.persistence.NoMedicationOrderRepository;
import com.cambae.backend.m07.internal.persistence.PrescriptionRepository;
import com.cambae.backend.m07.internal.persistence.ResidentMedicationLock;
import com.cambae.backend.m07.web.request.CreateHandoverBody;
import com.cambae.backend.m07.web.request.HandoverItemBody;
import com.cambae.backend.m07.web.request.NoMedicationOrderBody;
import com.cambae.backend.m07.web.request.ReportedDoseBody;
import com.cambae.backend.m07.web.request.ReturnHandoverBody;
import com.cambae.backend.m07.web.request.ReturnItemBody;
import com.cambae.backend.m07.web.response.HandoverResponse;
import com.cambae.backend.m07.web.response.MedicationToBringResponse;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final MedicationReturnItemRepository returnItemRepo;
    private final NoMedicationOrderRepository noMedOrderRepo;
    private final PrescriptionRepository prescriptionRepo;
    private final MedicationRepository medicationRepo;
    private final MedicationDoseRepository doseRepo;
    private final ResidentMedicationLock residentMedicationLock;

    @Autowired
    public MedicationHandoverService(
            MedicationAbsenceIntervalRepository absenceRepo,
            MedicationHandoverRepository handoverRepo,
            MedicationHandoverItemRepository handoverItemRepo,
            MedicationHandoverDoseRepository handoverDoseRepo,
            MedicationReturnRepository returnRepo,
            MedicationReturnItemRepository returnItemRepo,
            NoMedicationOrderRepository noMedOrderRepo,
            PrescriptionRepository prescriptionRepo,
            MedicationRepository medicationRepo,
            @Autowired(required = false) MedicationDoseRepository doseRepo,
            ResidentMedicationLock residentMedicationLock) {
        this.absenceRepo = absenceRepo;
        this.handoverRepo = handoverRepo;
        this.handoverItemRepo = handoverItemRepo;
        this.handoverDoseRepo = handoverDoseRepo;
        this.returnRepo = returnRepo;
        this.returnItemRepo = returnItemRepo;
        this.noMedOrderRepo = noMedOrderRepo;
        this.prescriptionRepo = prescriptionRepo;
        this.medicationRepo = medicationRepo;
        this.doseRepo = doseRepo;
        this.residentMedicationLock = residentMedicationLock;
    }

    @Transactional(readOnly = true)
    public List<MedicationToBringResponse> getMedicationsToBring(Long residentId, Long tripId) {
        // Find associated absence interval
        Optional<MedicationAbsenceInterval> intervalOpt =
                tripId != null ? absenceRepo.findByTripId(tripId) : absenceRepo.findOpenByResidentId(residentId);

        Instant fromTime =
                intervalOpt.map(MedicationAbsenceInterval::getFromTime).orElseGet(Instant::now);
        Instant toTime = intervalOpt
                .flatMap(i -> Optional.ofNullable(i.getExpectedToTime()))
                .orElseGet(() -> fromTime.plus(Duration.ofHours(24)));

        List<Prescription> prescriptions =
                prescriptionRepo.findByResidentIdAndStatusIn(residentId, List.of(PrescriptionStatus.ACTIVE));

        List<MedicationToBringResponse> result = new ArrayList<>();
        for (Prescription p : prescriptions) {
            MedicationToBringResponse res = new MedicationToBringResponse();
            res.setPrescriptionId(p.getId());
            res.setDosage(p.getDosage());
            res.setDosageUnit(p.getDosageUnit());
            res.setPrescriptionType(p.getPrescriptionType().name());

            Medication med = medicationRepo.findById(p.getMedicationId()).orElse(null);
            res.setMedicationName(med != null ? med.getName() : "Medication ID=" + p.getMedicationId());

            if (p.getPrescriptionType() == PrescriptionType.PERIODIC) {
                // Calculate projected doses during absence (each dose = p.getDosage())
                BigDecimal total =
                        p.getDosage() != null ? p.getDosage().multiply(BigDecimal.valueOf(2)) : BigDecimal.ONE;
                res.setTotalQuantityNeeded(total);
            } else {
                // PRN: Quantity based on maxDosesPer24h
                int maxDoses = p.getMaxDosesPer24h() != null ? p.getMaxDosesPer24h() : 2;
                res.setTotalQuantityNeeded(p.getDosage().multiply(BigDecimal.valueOf(maxDoses)));
            }

            result.add(res);
        }

        return result;
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
                currentUserId != null ? currentUserId : 1L,
                Instant.now());

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
                    d.markLeaveMedication(Instant.now());
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
                    HttpStatus.CONFLICT, "TRANG_THAI_CUOI", "Medication handover has already been returned");
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
                        "SO_LUONG_KHONG_HOP_LE",
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

        MedicationReturn medReturn = new MedicationReturn(
                handoverId, currentUserId != null ? currentUserId : 1L, Instant.now(), body.getReason());
        MedicationReturn savedReturn = returnRepo.save(medReturn);

        for (ReturnItemBody ri : body.getItems()) {
            MedicationReturnItem returnItem =
                    new MedicationReturnItem(savedReturn.getId(), ri.getPrescriptionId(), ri.getQuantity());
            returnItemRepo.save(returnItem);
        }

        handover.markReturned();
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

        NoMedicationOrder order = new NoMedicationOrder(
                residentId, body.getTripId(), body.getReason(), currentUserId != null ? currentUserId : 1L);
        NoMedicationOrder savedOrder = noMedOrderRepo.save(order);

        // Update or create absence interval of type TRIP_NO_MED
        Optional<MedicationAbsenceInterval> intervalOpt = absenceRepo.findByTripId(body.getTripId());
        if (intervalOpt.isPresent()) {
            MedicationAbsenceInterval interval = intervalOpt.get();
            interval.setAbsenceType(AbsenceIntervalType.TRIP_NO_MED);
            interval.setNoMedicationOrderId(savedOrder.getId());
            absenceRepo.save(interval);
        } else {
            MedicationAbsenceInterval interval = new MedicationAbsenceInterval(
                    residentId,
                    AbsenceIntervalType.TRIP_NO_MED,
                    Instant.now(),
                    Instant.now().plus(Duration.ofHours(24)),
                    null,
                    body.getTripId());
            interval.setNoMedicationOrderId(savedOrder.getId());
            absenceRepo.save(interval);
        }

        return savedOrder;
    }

    @Transactional
    public MedicationDose recordReportedDose(Long doseId, ReportedDoseBody body, Long currentUserId, boolean isNurse) {

        if (!isNurse) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "FORBIDDEN", "Only a Nurse can record leave medication by report (FR-027)");
        }

        if (body.getReportedBy() == null || body.getReportedBy().isBlank()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INVALID_VALUE",
                    "Reporter information is required when recording reported leave dose (FR-027)");
        }

        if (doseRepo == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Dose not found");
        }

        MedicationDose dose = doseRepo.findById(doseId).orElseThrow(NotFoundException::new);
        if (dose.getStatus() != DoseStatus.LEAVE_MEDICATION) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "TRANG_THAI_KHONG_HOP_LE",
                    "Can only record report for doses in LEAVE_MEDICATION status (FR-027)");
        }

        dose.closeAdministered(currentUserId != null ? currentUserId : 1L, "NURSE", null, Instant.now());
        return doseRepo.save(dose);
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
