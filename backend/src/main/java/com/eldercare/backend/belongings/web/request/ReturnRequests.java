package com.eldercare.backend.belongings.web.request;

import com.eldercare.backend.belongings.internal.belonging.domain.GiverType;
import com.eldercare.backend.belongings.internal.returns.domain.ContactChannel;
import com.eldercare.backend.belongings.internal.returns.domain.ReturnCommand;
import com.eldercare.backend.belongings.internal.returns.service.RecipientConfirmationService;
import java.time.OffsetDateTime;
import java.util.List;

/** Thân các lệnh trả đồ và xác nhận người nhận khác (api-013 {@code ReturnCommand}, {@code CreateRecipientConfirmation}). */
public final class ReturnRequests {

    private ReturnRequests() {}

    /** {@code ReturnCommand}; thiếu trường bắt buộc của hợp đồng thì {@code null} (controller trả 422). */
    public record Return(
            List<ItemRequest> items,
            RecipientRequest recipient,
            Long recipientConfirmationId,
            Long signatureFileId,
            String noSignatureReason,
            String conditionOpinion) {

        public record ItemRequest(
                Long belongingId,
                Integer version,
                BelongingIntakeRequest.ItemConditionRequest condition,
                List<Long> photoFileIds) {}

        public record RecipientRequest(
                GiverType type, Long familyMemberId, String fullName, DocumentRequest presentedIdDocument) {}

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
            Long residentId,
            String recipientName,
            String recipientIdDocument,
            Boolean recipientIsResident,
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
    public record ContactAttempt(Long familyMemberId, ContactChannel channel, OffsetDateTime at, String result) {

        public RecipientConfirmationService.ContactCommand toCommand() {
            return new RecipientConfirmationService.ContactCommand(
                    familyMemberId, channel, at == null ? null : at.toInstant(), result);
        }
    }

    /** Thân lệnh {@code approve-on-behalf}, {@code cancel}: lý do và căn cứ duyệt thay. */
    public record Command(String reason, RecipientConfirmationService.Basis basis) {}

    /** Thân lệnh cổng {@code confirm}, {@code reject}. */
    public record PortalCommand(String reason) {}
}
