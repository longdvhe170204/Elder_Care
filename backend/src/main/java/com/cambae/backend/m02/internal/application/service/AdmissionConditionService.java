package com.cambae.backend.m02.internal.application.service;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.AssessmentApi;
import com.cambae.backend.m01.api.CareLevelApi;
import com.cambae.backend.m01.api.HealthRecordApi;
import com.cambae.backend.m01.api.ResidentStatusApi;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.Condition;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m01.api.dto.StatusCommand;
import com.cambae.backend.m01.api.dto.Warning;
import com.cambae.backend.m02.api.AdmissionConditionApi;
import com.cambae.backend.m02.api.dto.ConditionListView;
import com.cambae.backend.m02.api.dto.ConditionStatus;
import com.cambae.backend.m02.api.dto.ConditionView;
import com.cambae.backend.m02.api.dto.ContractView;
import com.cambae.backend.m02.api.dto.ProgressStepView;
import com.cambae.backend.m02.internal.application.domain.AdmissionApplication;
import com.cambae.backend.m02.internal.application.domain.AdmissionOpinion;
import com.cambae.backend.m02.internal.application.domain.ApplicationStatus;
import com.cambae.backend.m02.internal.application.persistence.AdmissionApplicationStore;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.Target;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Danh sách điều kiện tiếp nhận (a) → (f) và tiến trình của lượt đăng ký (004 US1; FR-003, FR-004, FR-004a, FR-005,
 * FR-076; research T-09). Danh sách dựng từ chính kết quả {@link ResidentStatusApi#previewConditions} của lệnh Hoàn tất
 * tiếp nhận nên luôn khớp lệnh: mỗi điều kiện lệnh từ chối là một dòng Chưa đạt mang mã của module sở hữu ((a) của 001,
 * (d) của 003, (f) của 012, (b) (c) (e) của 004); điều kiện đạt hiện mã cố định của dòng. Ý kiến vào ở và các cảnh báo
 * không phải điều kiện. Cài {@link AdmissionConditionApi} cho 012, 016.
 */
@Service
public class AdmissionConditionService implements AdmissionConditionApi {

    static final String ASSESSMENT = "ADM_A_ASSESSMENT";
    static final String BED = "ADM_D_BED";
    static final String CONTACTS = "ADM_F_CONTACTS";

    public static final String CARE_LEVEL_MISMATCH = "CONTRACT_CARE_LEVEL_MISMATCH";
    public static final String RESIDENCE_TYPE_MISMATCH = "RESIDENCE_TYPE_MISMATCH";
    public static final String NOT_YET_ADMITTED = "NOT_YET_ADMITTED_FEE_CHARGED";

    /** Sáu dòng điều kiện của FR-004 (a) → (f): mã cố định khi đạt, tên, căn cứ và module sở hữu. */
    private enum Row {
        ASSESSMENT_ROW(ASSESSMENT, "Đánh giá đầu vào Đã xác nhận, chưa quá hạn", "FR-004 (a)", "M01"),
        CONTRACT_ROW(
                AdmissionContractConditions.CONTRACT,
                "Hợp đồng Hiệu lực, không sớm hơn ngày bắt đầu",
                "FR-004 (b), FR-075",
                "M02"),
        DEPOSIT_ROW(AdmissionContractConditions.DEPOSIT, "Đặt cọc Đã đáp ứng hoặc Không yêu cầu", "FR-004 (c)", "M11"),
        BED_ROW(BED, "Nội trú đã có phân bổ giường", "FR-004 (d)", "M03"),
        LEVEL_ROW(
                AdmissionContractConditions.CONTRACT_LEVEL,
                "Mức chăm sóc của hợp đồng trùng mức hiện hành",
                "BR-M02-22",
                "M02"),
        CONTACTS_ROW(CONTACTS, "Một liên hệ chính và ít nhất một người đại diện", "DBR-02", "M10");

        final String code;
        final String name;
        final String basis;
        final String module;

        Row(String code, String name, String basis, String module) {
            this.code = code;
            this.name = name;
            this.basis = basis;
            this.module = module;
        }

        static Row of(String unmetCode) {
            return switch (unmetCode) {
                case "INITIAL_ASSESSMENT_NOT_CONFIRMED", "INITIAL_ASSESSMENT_EXPIRED" -> ASSESSMENT_ROW;
                case AdmissionContractConditions.CONTRACT -> CONTRACT_ROW;
                case AdmissionContractConditions.DEPOSIT -> DEPOSIT_ROW;
                case AdmissionContractConditions.CONTRACT_LEVEL -> LEVEL_ROW;
                case "DBR_02" -> CONTACTS_ROW;
                // Điều kiện còn lại của lệnh Hoàn tất tiếp nhận là của 003: phân bổ giường (FR-004 (d), FR-018)
                default -> BED_ROW;
            };
        }
    }

    /** Danh sách điều kiện, cảnh báo và ý kiến hiện hành của một lượt. */
    public record Evaluation(
            List<ConditionView> conditions, List<Warning> warnings, Optional<AdmissionOpinion> opinion) {}

    private final AdmissionApplicationStore store;
    private final ResidentStatusApi residentStatus;
    private final AdmissionContractConditions contractConditions;
    private final AdmissionOpinionWarnings opinionWarnings;
    private final AssessmentApi assessments;
    private final HealthRecordApi healthRecords;
    private final CareLevelApi careLevels;
    private final BusinessTime time;

    AdmissionConditionService(
            AdmissionApplicationStore store,
            ResidentStatusApi residentStatus,
            AdmissionContractConditions contractConditions,
            AdmissionOpinionWarnings opinionWarnings,
            AssessmentApi assessments,
            HealthRecordApi healthRecords,
            CareLevelApi careLevels,
            BusinessTime time) {
        this.store = store;
        this.residentStatus = residentStatus;
        this.contractConditions = contractConditions;
        this.opinionWarnings = opinionWarnings;
        this.assessments = assessments;
        this.healthRecords = healthRecords;
        this.careLevels = careLevels;
        this.time = time;
    }

    @RequiresPermission(function = Function.ADMISSION_CONTRACT_DEPOSIT, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public Evaluation conditions(Target target, long applicationId) {
        AdmissionApplication application = store.find(applicationId).orElseThrow(NotFoundException::new);
        if (!application.status().isOpen()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "APPLICATION_CLOSED",
                    "Lượt đăng ký đã đóng, không còn danh sách điều kiện tiếp nhận (FR-004)");
        }
        return evaluate(application.residentId(), Optional.of(application));
    }

    @RequiresPermission(function = Function.ADMISSION_CONTRACT_DEPOSIT, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<ProgressStepView> progressOf(Target target, long applicationId) {
        return progress(applicationId);
    }

    @Override
    @Transactional(readOnly = true)
    public ConditionListView admissionConditions(long residentId) {
        Evaluation evaluation = evaluate(residentId, store.findOpen(residentId));
        return new ConditionListView(
                evaluation.conditions(),
                evaluation.warnings().stream().map(Warning::code).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgressStepView> progress(long applicationId) {
        AdmissionApplication application = store.find(applicationId).orElseThrow(NotFoundException::new);
        long residentId = application.residentId();
        boolean admitted = application.status() == ApplicationStatus.ADMITTED;
        Optional<ContractView> contract = contractConditions.latestContract(residentId);
        String contractBasis =
                contract.map(c -> "Hợp đồng " + c.contractNumber()).orElse(null);
        boolean conditionsMet = admitted
                || (application.status().isOpen()
                        && evaluate(residentId, Optional.of(application)).conditions().stream()
                                .noneMatch(line -> line.status() == ConditionStatus.NOT_MET));

        List<ProgressStepView> steps = new ArrayList<>();
        steps.add(new ProgressStepView("APPLICATION", true, "Lượt đăng ký " + application.id()));
        steps.add(new ProgressStepView(
                "INFORMATION_COLLECTION", healthRecords.initial(residentId).isPresent(), "Hồ sơ sức khỏe ban đầu"));
        steps.add(new ProgressStepView(
                "ASSESSMENT", assessments.latestConfirmed(residentId).isPresent(), "Đánh giá đầu vào Đã xác nhận"));
        steps.add(new ProgressStepView("RESIDENCE_TYPE_DETERMINATION", contract.isPresent(), contractBasis));
        steps.add(new ProgressStepView("CARE_DETERMINATION", contract.isPresent(), contractBasis));
        steps.add(new ProgressStepView("SERVICE_DETERMINATION", contract.isPresent(), contractBasis));
        steps.add(new ProgressStepView("CONDITION_CHECK", conditionsMet, "Danh sách điều kiện tiếp nhận"));
        steps.add(new ProgressStepView("ADMISSION", admitted, "Hoàn tất tiếp nhận"));
        return List.copyOf(steps);
    }

    @Override
    public ConditionListView endingConditions(long residencyEndingId) {
        throw new UnsupportedOperationException("Điều kiện kết thúc lưu trú thuộc user story kết thúc lưu trú (UC-17)");
    }

    private Evaluation evaluate(long residentId, Optional<AdmissionApplication> application) {
        List<Condition> unmet = residentStatus.previewConditions(residentId, StatusCommand.COMPLETE_ADMISSION);
        Optional<ContractView> contract = contractConditions.activeContract(residentId);
        ResidenceType residenceType = contract.map(ContractView::residenceType)
                .or(() -> application.map(AdmissionApplication::desiredResidenceType))
                .orElse(ResidenceType.LONG_TERM_RESIDENTIAL);

        List<ConditionView> lines = new ArrayList<>();
        for (Row row : Row.values()) {
            List<Condition> rowUnmet =
                    unmet.stream().filter(c -> Row.of(c.code()) == row).toList();
            if (!rowUnmet.isEmpty()) {
                rowUnmet.forEach(c -> lines.add(new ConditionView(
                        c.code(), c.description(), ConditionStatus.NOT_MET, c.basis(), row.module, null)));
            } else {
                ConditionStatus status = row == Row.BED_ROW && residenceType == ResidenceType.DAY_CARE
                        ? ConditionStatus.NOT_APPLICABLE
                        : ConditionStatus.MET;
                lines.add(new ConditionView(row.code, row.name, status, row.basis, row.module, null));
            }
        }
        return new Evaluation(
                List.copyOf(lines),
                warnings(residentId, application, contract),
                application.flatMap(opinionWarnings::opinionOf));
    }

    private List<Warning> warnings(
            long residentId, Optional<AdmissionApplication> application, Optional<ContractView> contract) {
        List<Warning> warnings = new ArrayList<>(opinionWarnings.of(application));
        if (contract.isEmpty()) {
            return List.copyOf(warnings);
        }
        ContractView active = contract.get();
        Optional<CareLevel> current = careLevels.current(residentId);
        if (current.isPresent() && current.get() != active.careLevel()) {
            warnings.add(new Warning(
                    CARE_LEVEL_MISMATCH,
                    "Mức chăm sóc của hợp đồng (" + active.careLevel() + ") khác mức hiện hành (" + current.get()
                            + ")"));
        }
        if (application.isPresent() && application.get().desiredResidenceType() != active.residenceType()) {
            warnings.add(new Warning(
                    RESIDENCE_TYPE_MISMATCH,
                    "Loại lưu trú của hợp đồng (" + active.residenceType() + ") khác loại mong muốn của lượt đăng ký ("
                            + application.get().desiredResidenceType() + ")"));
        }
        if (isFeeChargedBeforeAdmission(active, time.today())) {
            warnings.add(new Warning(
                    NOT_YET_ADMITTED,
                    "Phí lưu trú đang tính từ ngày " + active.startDate()
                            + " nhưng người cao tuổi chưa Hoàn tất tiếp nhận (FR-074, FR-076)"));
        }
        return List.copyOf(warnings);
    }

    /** FR-076: nội trú, hợp đồng Hiệu lực đã bắt đầu trước {@code day} mà chưa Hoàn tất tiếp nhận. */
    public static boolean isFeeChargedBeforeAdmission(ContractView contract, LocalDate day) {
        return contract.residenceType() != ResidenceType.DAY_CARE
                && contract.startDate().isBefore(day);
    }
}
