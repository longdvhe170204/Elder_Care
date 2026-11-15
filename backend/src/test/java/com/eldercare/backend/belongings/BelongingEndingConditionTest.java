package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.api.dto.PostDeathItemStatus;
import com.eldercare.backend.admission.api.dto.PostDeathItemStatusView;
import com.eldercare.backend.belongings.api.dto.BelongingEndingConditionView;
import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.internal.ending.domain.EndingConditionCalculator;
import com.eldercare.backend.belongings.internal.ending.domain.EndingConditionCalculator.Item;
import com.eldercare.backend.belongings.internal.ending.domain.PostDeathBelongings;
import com.eldercare.backend.belongings.internal.ending.domain.PostDeathBelongings.Entry;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 013 T022 ({@code DieuKienDoGuiTest}, {@code MucDoGuiSauQuaDoiTest}; FR-025, FR-026, FR-026a, BR-M12-03, Q-148;
 * quickstart Q29 → Q36): hàm thuần điều kiện (b) kết thúc lưu trú và trạng thái mục "xử lý đồ gửi" sau qua đời.
 */
class BelongingEndingConditionTest {

    @Test
    void returningTheItemsOneByOneEventuallyMeetsTheCondition() {
        assertThat(EndingConditionCalculator.evaluate(List.of(
                        new Item(1, BelongingStatus.RETURNED, false), new Item(2, BelongingStatus.IN_USE, false))))
                .extracting(BelongingEndingConditionView::met)
                .isEqualTo(false);
        assertThat(EndingConditionCalculator.evaluate(List.of(
                        new Item(1, BelongingStatus.RETURNED, false),
                        new Item(2, BelongingStatus.RETURNED, false),
                        new Item(3, BelongingStatus.DISPOSED, false))))
                .extracting(BelongingEndingConditionView::met)
                .isEqualTo(true);
    }

    @Test
    void lostItemDoesNotBlockButItsOpenIncidentIsListedSeparately() {
        BelongingEndingConditionView view = EndingConditionCalculator.evaluate(List.of(
                new Item(1, BelongingStatus.LOST, true),
                new Item(2, BelongingStatus.LOST, false),
                new Item(3, BelongingStatus.RETURNED, false)));

        assertThat(view.met()).isTrue();
        assertThat(view.blockingBelongingIds()).isEmpty();
        assertThat(view.lostWithOpenIncidentBelongingIds()).containsExactly(1L);
    }

    @Test
    void postDeathItemIsNotApplicableWhenNothingWasUnreturnedAtDeath() {
        PostDeathItemStatusView none = PostDeathBelongings.evaluate(List.of());
        PostDeathItemStatusView returnedBefore = PostDeathBelongings.evaluate(List.of(
                new Entry(BelongingStatus.RETURNED, BelongingStatus.RETURNED, false),
                new Entry(BelongingStatus.LOST, BelongingStatus.LOST, false)));

        assertThat(none.status()).isEqualTo(PostDeathItemStatus.NOT_APPLICABLE);
        assertThat(returnedBefore.status()).isEqualTo(PostDeathItemStatus.NOT_APPLICABLE);
    }
}
