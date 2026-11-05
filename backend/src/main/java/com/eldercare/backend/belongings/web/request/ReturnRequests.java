package com.eldercare.backend.belongings.web.request;

import com.eldercare.backend.belongings.internal.belonging.domain.GiverType;
import com.eldercare.backend.belongings.internal.returns.domain.ContactChannel;
import com.eldercare.backend.belongings.internal.returns.domain.ReturnCommand;
import com.eldercare.backend.belongings.internal.returns.service.RecipientConfirmationService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Thân các lệnh trả đồ và xác nhận người nhận khác (api-013 {@code ReturnCommand}, {@code CreateRecipientConfirmation}). */
public final class ReturnRequests {

    private ReturnRequests() {}

    /** {@code ReturnCommand}; thiếu trường bắt buộc của hợp đồng thì {@code null} (controller trả 422). */
    public record Return(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            List<ItemRequest> items,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            RecipientRequest recipient,

            Long recipientConfirmationId,
            Long signatureFileId,
            String noSignatureReason,
            String conditionOpinion) {

        /** {@code quantity}: số lượng trả, số tiền với tiền mặt; trống là toàn bộ (013-2 a, FR-012). */
        public record ItemRequest(
                Long belongingId,
                Integer version,
                BigDecimal quantity,
                BelongingIntakeRequest.ItemConditionRequest condition,
                List<Long> photoFileIds) {}

        public record RecipientRequest(
                @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        allowableValues = {"FAMILY_MEMBER", "RESIDENT", "OTHER_PERSON"})
                GiverType type,

                Long familyMemberId,
                String fullName,
                DocumentRequest presentedIdDocument) {}

        public record DocumentRequest(String type, String number) {}

        public ReturnCommand toCommand() {
            if (items == null || items.isEmpty() || recipient == null || recipient.type() == null) {
                return null;
            }
            List<ReturnCommand.Item> mapped = new java.util.ArrayList<>();
            for (ItemRequest item : items) {
                if (item == null
                        || item.belongingId() == null
                        || item.version() == null
                        || item.condition() == null
                        || item.condition().levelId() == null) {
                    return null;
                }
                mapped.add(new ReturnCommand.Item(
                        item.belongingId(),
                        item.version(),
                        item.quantity(),
                        item.condition().levelId(),
                        item.condition().description(),
                        item.photoFileIds()));
            }
            DocumentRequest document = recipient.presentedIdDocument();
            return new ReturnCommand(
                    mapped,
                    new ReturnCommand.Recipient(
                            recipient.type(),
                            recipient.familyMemberId(),
                            recipient.fullName(),
                            document == null ? null : document.type(),
                            document == null ? null : document.number()),
                    recipientConfirmationId,
                    signatureFileId,
                    noSignatureReason,
                    conditionOpinion);
        }

        public List<Long> belongingIds() {
            return items.stream().map(ItemRequest::belongingId).toList();
        }
    }

    /** {@code CreateRecipientConfirmation}. */
    public record CreateConfirmation(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Long residentId,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            String recipientName,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            String recipientIdDocument,

            Boolean recipientIsResident,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            List<Long> belongingIds,

            SignedForm signedForm) {

        public record SignedForm(Long representativeId, Long fileId) {}

        public RecipientConfirmationService.CreateCommand toCommand() {
            return new RecipientConfirmationService.CreateCommand(
                    recipientName,
                    recipientIdDocument,
                    Boolean.TRUE.equals(recipientIsResident),
                    belongingIds,
                    signedForm == null ? null : signedForm.representativeId(),
                    signedForm == null ? null : signedForm.fileId());
        }
    }

    /** {@code ContactAttempt}. */
    public record ContactAttempt(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Long familyMemberId,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            ContactChannel channel,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            OffsetDateTime at,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            String result) {

        public RecipientConfirmationService.ContactCommand toCommand() {
            return new RecipientConfirmationService.ContactCommand(
                    familyMemberId, channel, at == null ? null : at.toInstant(), result);
        }
    }

    /** Thân lệnh {@code approve-on-behalf}, {@code cancel}: lý do và căn cứ duyệt thay. */
    public record Command(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            String reason,

            RecipientConfirmationService.Basis basis) {}

    /** Thân lệnh cổng {@code confirm}, {@code reject}. */
    public record PortalCommand(String reason) {}
}
