package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.belongings.internal.belonging.domain.GiverType;
import com.eldercare.backend.belongings.internal.belonging.domain.ReturnBasis;
import com.eldercare.backend.belongings.internal.belonging.domain.ReturnBlockReason;
import com.eldercare.backend.belongings.internal.belonging.domain.VerificationResult;
import com.eldercare.backend.belongings.internal.returns.domain.ReturnRecipient;
import com.eldercare.backend.belongings.internal.returns.domain.ReturnRecipient.Confirmation;
import com.eldercare.backend.belongings.internal.returns.domain.ReturnRecipient.Declared;
import com.eldercare.backend.belongings.internal.returns.domain.ReturnRecipient.FamilyFacts;
import com.eldercare.backend.belongings.internal.returns.domain.ReturnRecipient.Item;
import com.eldercare.backend.belongings.internal.returns.domain.ReturnRecipient.RegisteredDocument;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 013 T016 ({@code NguoiNhanHopLeTest}; FR-016, FR-016a, FR-017, BR-M12-04, Q-253; quickstart Q14 → Q17, Q21): người
 * nhận hợp lệ là hàm thuần.
 */
class ReturnRecipientTest {

    private static final Item PHONE = new Item(1, true, false);
    private static final Item CLOTHES = new Item(2, false, false);
    private static final Item CASH = new Item(3, true, true);

    private static final FamilyFacts REPRESENTATIVE =
            new FamilyFacts(true, false, "Nguyễn Văn P", Optional.of(new RegisteredDocument("CCCD", "012345")));

    private static Declared family(String type, String number) {
        return new Declared(GiverType.FAMILY_MEMBER, 10L, "Nguyễn Văn P", type, number);
    }

    private static Confirmation confirmation(boolean effective, Long... ids) {
        return new Confirmation(effective, false, "Trần Thị N", "999", List.of(ids));
    }

    private static Declared other() {
        return new Declared(GiverType.OTHER_PERSON, null, "Trần Thị N", "CCCD", "999");
    }

    @Test
    void Q14_representativeWithMatchingDocument_isAllowed() {
        var result = ReturnRecipient.check(
                family("cccd", " 012345 "), List.of(PHONE), REPRESENTATIVE, Optional.empty(), true);

        assertThat(result.allowed()).isTrue();
        assertThat(result.basis()).isEqualTo(ReturnBasis.ENTITLED_RECIPIENT);
        assertThat(result.verification()).isEqualTo(VerificationResult.MATCHED);
        assertThat(result.missingRegisteredDocument()).isFalse();
    }

    @Test
    void FR_016_documentMismatch_orMissingPresentedDocument_isBlocked() {
        var mismatch =
                ReturnRecipient.check(family("CCCD", "000"), List.of(PHONE), REPRESENTATIVE, Optional.empty(), true);
        var missing = ReturnRecipient.check(family(null, null), List.of(PHONE), REPRESENTATIVE, Optional.empty(), true);

        assertThat(mismatch.blockReason()).isEqualTo(ReturnBlockReason.ID_DOCUMENT_MISMATCH);
        assertThat(missing.blockReason()).isEqualTo(ReturnBlockReason.ID_DOCUMENT_MISMATCH);
    }

    @Test
    void FR_016_profileWithoutDocument_recordsPresentedDocument_andAsksOfficeToComplete() {
        var noDocument = new FamilyFacts(false, true, "Nguyễn Văn P", Optional.empty());

        var allowed =
                ReturnRecipient.check(family("CCCD", "777"), List.of(CLOTHES), noDocument, Optional.empty(), true);
        var wrongName = ReturnRecipient.check(
                new Declared(GiverType.FAMILY_MEMBER, 10L, "Người lạ", "CCCD", "777"),
                List.of(CLOTHES),
                noDocument,
                Optional.empty(),
                true);

        assertThat(allowed.allowed()).isTrue();
        assertThat(allowed.verification()).isEqualTo(VerificationResult.NO_DOCUMENT_ON_FILE_CROSS_CHECKED);
        assertThat(allowed.missingRegisteredDocument()).isTrue();
        assertThat(wrongName.blockReason()).isEqualTo(ReturnBlockReason.ID_DOCUMENT_MISMATCH);
    }

    @Test
    void Q15_familyMemberWithoutRight_isBlocked() {
        var result = ReturnRecipient.check(
                family("CCCD", "012345"), List.of(CLOTHES), FamilyFacts.none(), Optional.empty(), true);

        assertThat(result.blockReason()).isEqualTo(ReturnBlockReason.NOT_ENTITLED_TO_RECEIVE);
    }

    @Test
    void Q16_residentTakesItemsWithoutConsentFlag_withoutConfirmation() {
        var resident = new Declared(GiverType.RESIDENT, null, null, null, null);

        var plain = ReturnRecipient.check(resident, List.of(CLOTHES), FamilyFacts.none(), Optional.empty(), false);
        var needsConsent =
                ReturnRecipient.check(resident, List.of(CLOTHES, CASH), FamilyFacts.none(), Optional.empty(), true);

        assertThat(plain.allowed()).isTrue();
        assertThat(plain.basis()).isEqualTo(ReturnBasis.RESIDENT_Q253_EXCEPTION);
        assertThat(needsConsent.blockReason()).isEqualTo(ReturnBlockReason.NOT_ENTITLED_TO_RECEIVE);
    }

    @Test
    void Q17_otherPerson_needsEffectiveConfirmationMatchingPersonAndCoveringEveryItem() {
        var ok = ReturnRecipient.check(
                other(), List.of(PHONE, CLOTHES), FamilyFacts.none(), Optional.of(confirmation(true, 1L, 2L)), true);
        var notEffective = ReturnRecipient.check(
                other(), List.of(PHONE), FamilyFacts.none(), Optional.of(confirmation(false, 1L)), true);
        var missingItem = ReturnRecipient.check(
                other(), List.of(PHONE, CLOTHES), FamilyFacts.none(), Optional.of(confirmation(true, 1L)), true);
        var wrongPerson = ReturnRecipient.check(
                new Declared(GiverType.OTHER_PERSON, null, "Người khác", "CCCD", "111"),
                List.of(PHONE),
                FamilyFacts.none(),
                Optional.of(confirmation(true, 1L)),
                true);
        var none = ReturnRecipient.check(other(), List.of(PHONE), FamilyFacts.none(), Optional.empty(), true);

        assertThat(ok.basis()).isEqualTo(ReturnBasis.RECIPIENT_CONFIRMATION);
        assertThat(notEffective.blockReason()).isEqualTo(ReturnBlockReason.INVALID_RECIPIENT_CONFIRMATION);
        assertThat(missingItem.blockReason()).isEqualTo(ReturnBlockReason.INVALID_RECIPIENT_CONFIRMATION);
        assertThat(wrongPerson.blockReason()).isEqualTo(ReturnBlockReason.INVALID_RECIPIENT_CONFIRMATION);
        assertThat(none.blockReason()).isEqualTo(ReturnBlockReason.NOT_ENTITLED_TO_RECEIVE);
    }

    @Test
    void FR_017_residentRecipientWithConfirmation_matchesByResidentFlag() {
        var resident = new Declared(GiverType.RESIDENT, null, null, null, null);
        var forResident = new Confirmation(true, true, "A", "1", List.of(3L));

        var result = ReturnRecipient.check(resident, List.of(CASH), FamilyFacts.none(), Optional.of(forResident), true);

        assertThat(result.basis()).isEqualTo(ReturnBasis.RECIPIENT_CONFIRMATION);
    }

    @Test
    void Q21_nursingStaff_cannotReturnValuableItem_andCannotUseConfirmation() {
        var valuable = ReturnRecipient.check(
                family("CCCD", "012345"), List.of(PHONE, CLOTHES), REPRESENTATIVE, Optional.empty(), false);
        var plain = ReturnRecipient.check(
                family("CCCD", "012345"), List.of(CLOTHES), REPRESENTATIVE, Optional.empty(), false);
        var viaConfirmation = ReturnRecipient.check(
                other(), List.of(CLOTHES), FamilyFacts.none(), Optional.of(confirmation(true, 2L)), false);

        assertThat(valuable.blockReason()).isEqualTo(ReturnBlockReason.NURSING_STAFF_VALUABLE_ITEM);
        assertThat(plain.allowed()).isTrue();
        assertThat(viaConfirmation.blockReason()).isEqualTo(ReturnBlockReason.NOT_ENTITLED_TO_RECEIVE);
    }
}
