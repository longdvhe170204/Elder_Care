package com.cambae.backend.m02.web;

import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.internal.change.service.ResidencyChangeService;
import com.cambae.backend.m02.internal.contract.domain.Contract;
import com.cambae.backend.m02.internal.contract.service.ContractReadService;
import com.cambae.backend.m02.internal.contract.service.ContractService;
import com.cambae.backend.m02.internal.deposit.service.DepositViewService;
import com.cambae.backend.m02.web.request.ContractInputBody;
import com.cambae.backend.m02.web.request.ReasonBody;
import com.cambae.backend.m02.web.request.RecordSignedBody;
import com.cambae.backend.m02.web.response.AddendumBody;
import com.cambae.backend.m02.web.response.ApprovalRequestRefBody;
import com.cambae.backend.m02.web.response.ContractBody;
import com.cambae.backend.m02.web.response.ContractContentBody;
import com.cambae.backend.m02.web.response.ContractResultBody;
import com.cambae.backend.m02.web.response.DepositBody;
import com.cambae.backend.m02.web.response.NonStandardTermBody;
import com.cambae.backend.m15.account.api.dto.Target;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hợp đồng lưu trú (004 US3; contracts/api-004.yaml, tag {@code contracts}). Mọi logic và kiểm quyền ở service. Trạng thái
 * đặt cọc ({@code GET /contracts/{id}/deposit}) do 017 tính, controller chỉ đọc; phụ lục chỉ do yêu cầu thay đổi lưu trú đã
 * duyệt tạo (user story 5) nên chỉ có endpoint đọc.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "contracts")
class ContractController {

    private static final String PROBLEM = "application/problem+json";

    private final ContractService contracts;
    private final ContractReadService reads;
    private final DepositViewService deposits;
    private final ResidencyChangeService changes;

    ContractController(
            ContractService contracts,
            ContractReadService reads,
            DepositViewService deposits,
            ResidencyChangeService changes) {
        this.contracts = contracts;
        this.reads = reads;
        this.deposits = deposits;
        this.changes = changes;
    }

    @GetMapping("/contracts")
    List<ContractBody> list(
            @RequestParam(name = "residentId", required = false) Long residentId,
            @RequestParam(name = "status", required = false) Set<ContractStatus> status,
            @RequestParam(name = "overdue", required = false) Boolean overdue) {
        return contracts.list(Target.UNSPECIFIED, residentId, status, overdue).stream()
                .map(ContractBody::of)
                .toList();
    }

    @PostMapping("/contracts")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã lập"),
        @ApiResponse(
                responseCode = "409",
                description = "REPRESENTATIVE_REQUIRED, RESIDENT_NOT_ELIGIBLE_FOR_CONTRACT",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<ContractResultBody> create(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ContractInputBody body) {
        ContractService.Result result =
                contracts.create(ContractService.targetForResident(body.residentId()), body.content());
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(VersionEtag.of(result.contract().version()))
                .body(ContractResultBody.of(result));
    }

    @GetMapping("/contracts/{id}")
    ResponseEntity<ContractBody> get(@PathVariable long id) {
        Contract contract = contracts.get(contracts.targetOf(id), id);
        return ResponseEntity.ok().eTag(VersionEtag.of(contract.version())).body(ContractBody.of(contract));
    }

    @PutMapping("/contracts/{id}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                description = "CONTRACT_LOCKED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "412",
                description = "If-Match không khớp",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<ContractResultBody> update(
            @PathVariable long id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ContractInputBody body) {
        ContractService.Result result =
                contracts.update(contracts.targetOf(id), id, ifMatch, body.residentId(), body.content());
        return ResponseEntity.ok()
                .eTag(VersionEtag.of(result.contract().version()))
                .body(ContractResultBody.of(result));
    }

    @GetMapping("/contracts/{id}/non-standard-terms")
    List<NonStandardTermBody> nonStandardTerms(@PathVariable long id) {
        return contracts.nonStandardTerms(contracts.targetOf(id), id).stream()
                .map(NonStandardTermBody::of)
                .toList();
    }

    @PostMapping("/contracts/{id}/submit-terms-for-approval")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã lập yêu cầu"),
        @ApiResponse(
                responseCode = "409",
                description = "NO_NON_STANDARD_TERMS, CONTRACT_LOCKED, INVALID_TRANSITION",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<ApprovalRequestRefBody> submitTermsForApproval(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ReasonBody body) {
        var request = contracts.submitTermsForApproval(contracts.targetOf(id), id, body.reason());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApprovalRequestRefBody.of(request));
    }

    @PostMapping("/contracts/{id}/send-for-signature")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                description = "CONDITION_NOT_MET, INVALID_TRANSITION",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ContractResultBody sendForSignature(
            @PathVariable long id, @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey) {
        return ContractResultBody.of(contracts.sendForSignature(contracts.targetOf(id), id));
    }

    @PostMapping("/contracts/{id}/return-to-draft")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "INVALID_TRANSITION",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<Void> returnToDraft(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ReasonBody body) {
        contracts.returnToDraft(contracts.targetOf(id), id, body.reason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/contracts/{id}/record-signed")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                description = "INVALID_TRANSITION, CONTRACT_OVERLAP, INVALID_SIGNING_REPRESENTATIVE",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                description = "Thiếu ngày ký, người đại diện hoặc bản scan",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ContractResultBody recordSigned(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody RecordSignedBody body) {
        return ContractResultBody.of(contracts.recordSigned(
                contracts.targetOf(id), id, body.signedDate(), body.signingRepresentativeId(), body.scanFileId()));
    }

    @PostMapping("/contracts/{id}/cancel")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "INVALID_TRANSITION",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<Void> cancel(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ReasonBody body) {
        contracts.cancel(contracts.targetOf(id), id, body.reason());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/contracts/{id}/content")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "404",
                description = "Hợp đồng chưa ký hoặc ngày trước ngày bắt đầu",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ContractContentBody content(
            @PathVariable long id,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Target target = contracts.targetOf(id);
        return ContractContentBody.of(reads.contentOf(target, id, date), target.residentId());
    }

    @GetMapping("/contracts/{id}/addenda")
    List<AddendumBody> addenda(@PathVariable long id) {
        Contract contract = contracts.get(contracts.targetOf(id), id);
        return changes.addendaOf(contracts.targetOf(id), id).stream()
                .map(addendum -> AddendumBody.of(addendum, contract.residentId()))
                .toList();
    }

    @GetMapping("/contracts/{id}/deposit")
    DepositBody deposit(@PathVariable long id) {
        contracts.get(contracts.targetOf(id), id);
        return DepositBody.of(deposits.view(id));
    }
}
