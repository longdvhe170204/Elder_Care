package com.eldercare.backend.belongings.internal.returns.domain;

import com.eldercare.backend.belongings.internal.belonging.domain.GiverType;
import com.eldercare.backend.belongings.internal.belonging.domain.ReturnBasis;
import com.eldercare.backend.belongings.internal.belonging.domain.ReturnBlockReason;
import com.eldercare.backend.belongings.internal.belonging.domain.VerificationResult;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Người nhận hợp lệ ({@code NguoiNhanHopLe}; 013 T018, FR-016, FR-016a, FR-017, BR-M12-04, Q-253, research K-06): hàm
 * thuần, không đọc cơ sở dữ liệu.
 *
 * <ol>
 *   <li>Điều dưỡng gặp đồ có giá trị: chặn cả lệnh (FR-016a).
 *   <li>Người đại diện hoặc người có quyền đón tại thời điểm: xác minh giấy tờ; hồ sơ chưa có giấy tờ thì ghi giấy tờ
 *       xuất trình và đối chiếu họ tên.
 *   <li>Chính người cao tuổi với mọi đồ không thuộc loại cần đồng ý: được (Q-253).
 *   <li>Khác: xác nhận người nhận khác Hiệu lực, khớp người nhận, bao gồm mọi đồ đang trả; Điều dưỡng không dùng được
 *       (FR-016a).
 * </ol>
 */
public final class ReturnRecipient {

    private ReturnRecipient() {}

    /** Một đồ trong lệnh Trả. */
    public record Item(long belongingId, boolean valuable, boolean requiresConsent) {}

    /** Người nhận do nhân viên khai và giấy tờ xuất trình. */
    public record Declared(
            GiverType type, Long familyMemberId, String fullName, String documentType, String documentNumber) {}

    /** Giấy tờ đã đăng ký ở hồ sơ người thân. */
    public record RegisteredDocument(String type, String number) {}

    /** Quyền của người nhận khai là người thân, tại thời điểm trả (012 {@code FamilyRightsApi}). */
    public record FamilyFacts(
            boolean representative,
            boolean allowedPicker,
            String profileName,
            Optional<RegisteredDocument> registeredDocument) {

        public static FamilyFacts none() {
            return new FamilyFacts(false, false, null, Optional.empty());
        }
    }

    /** Xác nhận người nhận khác đã tra theo mã trong lệnh; {@code effective} gồm chưa hết hạn. */
    public record Confirmation(
            boolean effective,
            boolean recipientIsResident,
            String recipientName,
            String recipientIdDocument,
            List<Long> belongingIds) {}

    /** Kết quả: được ({@code basis}, {@code verification}) hoặc bị chặn ({@code blockReason}). */
    public record Result(
            ReturnBasis basis,
            VerificationResult verification,
            ReturnBlockReason blockReason,
            boolean missingRegisteredDocument) {

        public boolean allowed() {
            return blockReason == null;
        }

        static Result allowed(ReturnBasis basis, VerificationResult verification) {
            return new Result(
                    basis, verification, null, verification == VerificationResult.NO_DOCUMENT_ON_FILE_CROSS_CHECKED);
        }

        static Result blocked(ReturnBlockReason reason) {
            return new Result(null, null, reason, false);
        }
    }

    public static Result check(
            Declared recipient,
            List<Item> items,
            FamilyFacts family,
            Optional<Confirmation> confirmation,
            boolean officeStaff) {
        if (!officeStaff && items.stream().anyMatch(Item::valuable)) {
            return Result.blocked(ReturnBlockReason.NURSING_STAFF_VALUABLE_ITEM);
        }
        if (recipient.type() == GiverType.FAMILY_MEMBER && (family.representative() || family.allowedPicker())) {
            return verifyDocument(recipient, family);
        }
        if (recipient.type() == GiverType.RESIDENT && items.stream().noneMatch(Item::requiresConsent)) {
            return Result.allowed(ReturnBasis.RESIDENT_Q253_EXCEPTION, null);
        }
        if (confirmation.isEmpty() || !officeStaff) {
            return Result.blocked(ReturnBlockReason.NOT_ENTITLED_TO_RECEIVE);
        }
        return matches(recipient, items, confirmation.get())
                ? Result.allowed(ReturnBasis.RECIPIENT_CONFIRMATION, null)
                : Result.blocked(ReturnBlockReason.INVALID_RECIPIENT_CONFIRMATION);
    }

    private static Result verifyDocument(Declared recipient, FamilyFacts family) {
        if (blank(recipient.documentType()) || blank(recipient.documentNumber())) {
            return Result.blocked(ReturnBlockReason.ID_DOCUMENT_MISMATCH);
        }
        if (family.registeredDocument().isPresent()) {
            RegisteredDocument registered = family.registeredDocument().get();
            return same(registered.type(), recipient.documentType())
                            && same(registered.number(), recipient.documentNumber())
                    ? Result.allowed(ReturnBasis.ENTITLED_RECIPIENT, VerificationResult.MATCHED)
                    : Result.blocked(ReturnBlockReason.ID_DOCUMENT_MISMATCH);
        }
        // FR-016: hồ sơ chưa có giấy tờ thì đối chiếu họ tên với hồ sơ và ghi giấy tờ xuất trình
        return family.profileName() != null && same(family.profileName(), recipient.fullName())
                ? Result.allowed(ReturnBasis.ENTITLED_RECIPIENT, VerificationResult.NO_DOCUMENT_ON_FILE_CROSS_CHECKED)
                : Result.blocked(ReturnBlockReason.ID_DOCUMENT_MISMATCH);
    }

    private static boolean matches(Declared recipient, List<Item> items, Confirmation confirmation) {
        if (!confirmation.effective()) {
            return false;
        }
        boolean person = recipient.type() == GiverType.RESIDENT
                ? confirmation.recipientIsResident()
                : !confirmation.recipientIsResident()
                        && same(confirmation.recipientName(), recipient.fullName())
                        && documentMatches(confirmation.recipientIdDocument(), recipient);
        return person
                && items.stream().allMatch(i -> confirmation.belongingIds().contains(i.belongingId()));
    }

    /**
     * Xác nhận lưu giấy tờ thành một chuỗi (api-013 recipientIdDocument), lệnh trả khai tách loại và số (FR-017): khớp khi
     * chuỗi bằng số giấy tờ, hoặc bằng "loại số".
     */
    private static boolean documentMatches(String stored, Declared recipient) {
        if (same(stored, recipient.documentNumber())) {
            return true;
        }
        return !blank(recipient.documentType())
                && same(
                        stored,
                        recipient.documentType().strip() + " "
                                + recipient.documentNumber().strip());
    }

    private static boolean same(String a, String b) {
        return !blank(a) && !blank(b) && normalize(a).equals(normalize(b));
    }

    private static String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
