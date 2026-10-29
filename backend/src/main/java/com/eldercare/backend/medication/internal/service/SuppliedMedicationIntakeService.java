package com.eldercare.backend.medication.internal.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.ProcurementIntakeApi;
import com.eldercare.backend.medication.api.dto.IntakeResultView;
import com.eldercare.backend.medication.api.events.SuppliedMedicationIntakeFailed;
import com.eldercare.backend.medication.api.spi.ProcurementMedicationLineView;
import com.eldercare.backend.medication.api.spi.ProcurementMedicationPort;
import com.eldercare.backend.medication.internal.domain.Prescription;
import com.eldercare.backend.medication.internal.domain.SuppliedMedicationIntake;
import com.eldercare.backend.medication.internal.domain.SuppliedMedicationIntake.IntakeResult;
import com.eldercare.backend.medication.internal.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.persistence.SuppliedMedicationIntakeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lần tiếp nhận thuốc cung ứng (FR-054, FR-055, BR-M07-24, BR-M11-17): gắn một dòng của yêu cầu mua sắm loại "thuốc" đã
 * ghi đã mua; không tạo tồn kho, không tính phí, không đổi đơn hay liều. Một dòng có nhiều lần tiếp nhận (giao bù).
 */
@Service
public class SuppliedMedicationIntakeService implements ProcurementIntakeApi {

    static final String ENTITY_TYPE = "M07.SUPPLIED_MEDICATION_INTAKE";

    /** Thân lệnh ghi (api-006 {@code NewSuppliedMedicationIntake}). */
    public record RecordIntake(
            long requestLineId,
            String concentration,
            String dosageForm,
            BigDecimal receivedQuantity,
            String unit,
            LocalDate expiryDate,
            String condition,
            IntakeResult result,
            String discrepancy,
            String reason,
            String delivererName) {}

    private final SuppliedMedicationIntakeRepository intakes;
    private final PrescriptionRepository prescriptions;
    // 019 cài; thiếu bean thì lệnh bị chặn (module-interfaces mục 2.1, mặc định an toàn)
    private final ObjectProvider<ProcurementMedicationPort> procurement;
    private final ApplicationEventPublisher events;
    private final CurrentUser currentUser;
    private final BusinessTime businessTime;

    public SuppliedMedicationIntakeService(
            SuppliedMedicationIntakeRepository intakes,
            PrescriptionRepository prescriptions,
            ObjectProvider<ProcurementMedicationPort> procurement,
            ApplicationEventPublisher events,
            CurrentUser currentUser,
            BusinessTime businessTime) {
        this.intakes = intakes;
        this.prescriptions = prescriptions;
        this.procurement = procurement;
        this.events = events;
        this.currentUser = currentUser;
        this.businessTime = businessTime;
    }

    /** Đối tượng kiểm quyền: người cao tuổi của dòng yêu cầu; dòng chưa ghi đã mua thì 409 (BR-M11-17). */
    @Transactional(readOnly = true)
    public Target targetOfLine(long requestLineId) {
        ProcurementMedicationLineView line = purchasedLine(requestLineId);
        return new Target(ENTITY_TYPE, null, line.residentId(), null);
    }

    @RequiresPermission(function = Function.MEDICATION_RECEIPT, operation = Operation.CREATE)
    @Transactional
    public SuppliedMedicationIntake record(Target target, RecordIntake command) {
        if (command.result() == IntakeResult.NOT_PASSED && (blank(command.discrepancy()) || blank(command.reason()))) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INVALID_VALUE",
                    "Không đạt phải có mô tả sai lệch và lý do (FR-054)");
        }
        ProcurementMedicationLineView line = purchasedLine(command.requestLineId());
        Prescription prescription = prescriptions
                .findById(line.prescriptionId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.CONFLICT, "INVALID_PRESCRIPTION", "Đơn thuốc của dòng yêu cầu không tồn tại"));

        SuppliedMedicationIntake saved = intakes.save(new SuppliedMedicationIntake(
                line.procurementLineId(),
                line.procurementRequestId(),
                line.residentId(),
                line.prescriptionId(),
                prescription.getMedicationId(),
                line.medicationName(),
                command.concentration(),
                command.dosageForm(),
                command.receivedQuantity(),
                command.unit(),
                command.expiryDate(),
                command.condition(),
                command.result(),
                command.discrepancy(),
                command.reason(),
                command.delivererName(),
                currentUser.accountId(),
                businessTime.now()));

        if (saved.getIntakeResult() == IntakeResult.NOT_PASSED) {
            events.publishEvent(new SuppliedMedicationIntakeFailed(
                    saved.getId(),
                    line.procurementRequestId(),
                    line.residentId(),
                    line.creatorStaffId(),
                    line.departmentId(),
                    line.medicationName(),
                    saved.getDiscrepancy(),
                    saved.getReceivedAt()));
        }
        return saved;
    }

    /** Đối tượng kiểm quyền khi xem: người cao tuổi của các lần tiếp nhận; chưa có lần nào thì không có dữ liệu để lộ. */
    @Transactional(readOnly = true)
    public List<SuppliedMedicationIntake> intakesOf(long purchaseRequestId) {
        return intakes.findByProcurementRequestId(purchaseRequestId);
    }

    @RequiresPermission(function = Function.MEDICATION_RECEIPT, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<SuppliedMedicationIntake> list(Target target, long purchaseRequestId) {
        return intakes.findByProcurementRequestId(purchaseRequestId);
    }

    public static Target residentTarget(long residentId) {
        return new Target(ENTITY_TYPE, null, residentId, null);
    }

    /** 019 tính "Đã nhận" (mọi dòng có ít nhất một lần Đạt) từ kết quả này (module-interfaces mục 1.8a). */
    @Override
    @Transactional(readOnly = true)
    public List<IntakeResultView> byProcurementRequest(long procurementRequestId) {
        return intakes.findByProcurementRequestId(procurementRequestId).stream()
                .map(i -> new IntakeResultView(
                        i.getId(),
                        i.getProcurementLineId(),
                        i.getMedicationName(),
                        i.getQuantityReceived(),
                        i.getIntakeResult() == IntakeResult.PASSED,
                        i.getDiscrepancy(),
                        i.getReceivedAt()))
                .toList();
    }

    private ProcurementMedicationLineView purchasedLine(long requestLineId) {
        ProcurementMedicationPort port = procurement.getIfAvailable();
        return (port == null
                        ? Optional.<ProcurementMedicationLineView>empty()
                        : port.lineRecordedPurchased(requestLineId))
                .orElseThrow(() -> new ConditionNotMetException(List.of(new UnmetCondition(
                        "REQUEST_NOT_PURCHASED", "Dòng yêu cầu mua sắm chưa ghi đã mua (BR-M11-17)"))));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
