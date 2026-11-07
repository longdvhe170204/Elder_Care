package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.incident.internal.infection.domain.ContactGroup;
import com.eldercare.backend.incident.internal.infection.domain.ContactPersonContent;
import com.eldercare.backend.incident.internal.infection.domain.ContactType;
import com.eldercare.backend.incident.internal.infection.domain.ContainmentScope;
import com.eldercare.backend.incident.internal.infection.persistence.ContactListStore;
import com.eldercare.backend.incident.internal.infection.persistence.ContactPersonStore;
import com.eldercare.backend.incident.internal.infection.persistence.ContainmentStore;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

class ContainmentStoreIT extends MySqlIntegrationTestBase {

    @Autowired
    ContainmentStore store;

    @Autowired
    ContactPersonStore followUps;

    @Autowired
    ContactListStore contactLists;

    @Autowired
    TransactionTemplate transaction;

    @Test
    void BR_M05_12_releasedContainmentIsKeptForHistoricalLookup() {
        long floor = TestUsers.nextId();
        long actor = TestUsers.nextId();
        long incident = TestUsers.nextId();
        var start = Instant.parse("2026-11-03T00:00:00Z");
        var release = start.plusSeconds(60);

        transaction.executeWithoutResult(tx -> {
            tx.setRollbackOnly();
            var scope = new ContainmentScope(Set.of(floor), Set.of());
            var created = store.create(scope, start, actor, "Khoanh vùng", Set.of(incident));
            assertThat(store.scopesAt(start)).contains(scope);

            store.release(created.id(), created.version(), release, actor, "Gỡ khoanh vùng");

            assertThat(store.scopesAt(start)).contains(scope);
            assertThat(store.scopesAt(release)).doesNotContain(scope);
        });
    }

    @Test
    void BR_M05_10_contactFollowUpEndsAtExpiryOrRemoval() {
        long resident = TestUsers.nextId();
        long list = TestUsers.nextId();
        long actor = TestUsers.nextId();
        var start = Instant.parse("2026-11-03T00:00:00Z");
        var removed = start.plusSeconds(60);
        var end = start.plusSeconds(120);
        transaction.executeWithoutResult(tx -> {
            tx.setRollbackOnly();
            var row = followUps.add(
                    list,
                    new ContactPersonContent(
                                    ContactType.RESIDENT,
                                    resident,
                                    null,
                                    null,
                                    null,
                                    ContactGroup.SAME_ROOM,
                                    "Cùng phòng",
                                    start,
                                    end,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    false)
                            .monitored(start, end),
                    ContactPersonStore.ACTIVE,
                    actor);
            assertThat(followUps.listedAt(resident, start.minusSeconds(1))).isFalse();
            assertThat(followUps.listedAt(resident, start)).isTrue();
            assertThat(followUps.listedAt(resident, end)).isFalse();

            followUps.change(row, ContactPersonStore.EXCLUDED, row.content().excluded(removed, "Loại khỏi danh sách"));
            assertThat(followUps.listedAt(resident, start)).isTrue();
            assertThat(followUps.listedAt(resident, removed)).isFalse();
        });
    }

    @Test
    void BR_M05_10_contactListKeepsOneDraftAndItsTraceWindowPerIncident() {
        long incident = TestUsers.nextId();
        long actor = TestUsers.nextId();
        var from = Instant.parse("2026-10-29T00:00:00Z");
        var to = Instant.parse("2026-11-03T00:00:00Z");
        transaction.executeWithoutResult(tx -> {
            tx.setRollbackOnly();
            var draft = contactLists.create(incident, from, to, actor);
            assertThat(contactLists.findByIncident(incident)).contains(draft);
            assertThat(draft.status()).isEqualTo("DRAFT");
            assertThat(draft.content().traceFrom()).isEqualTo(from);
            assertThat(draft.content().traceTo()).isEqualTo(to);
            assertThatThrownBy(() -> contactLists.create(incident, from, to, actor))
                    .isInstanceOf(ConditionNotMetException.class);
        });
    }

    @Test
    void BR_M05_10_contactListCanBeConfirmedAndEndedWithoutLosingConfirmation() {
        long incident = TestUsers.nextId();
        long actor = TestUsers.nextId();
        var from = Instant.parse("2026-10-29T00:00:00Z");
        var at = Instant.parse("2026-11-03T00:00:00Z");
        transaction.executeWithoutResult(tx -> {
            tx.setRollbackOnly();
            var draft = contactLists.create(incident, from, at, actor);
            var confirmed = contactLists.confirm(draft.id(), draft.version(), at, actor, "Không có tiếp xúc");
            assertThat(confirmed.status()).isEqualTo("CONFIRMED");
            assertThat(confirmed.content().confirmedAt()).isEqualTo(at);
            assertThat(confirmed.content().confirmedBy()).isEqualTo(actor);
            assertThat(confirmed.content().noContactsReason()).isEqualTo("Không có tiếp xúc");

            assertThatThrownBy(() -> contactLists.confirm(
                            confirmed.id(), confirmed.version(), at.plusSeconds(1), actor, "Xác nhận lại"))
                    .isInstanceOf(ConditionNotMetException.class);

            var ended = contactLists.end(confirmed.id(), confirmed.version());
            assertThat(ended.status()).isEqualTo("ENDED");
            assertThat(ended.content()).isEqualTo(confirmed.content());
            assertThat(ended.lockedAt()).isNotNull();
            assertThat(contactLists.findByIncident(incident)).contains(ended);
        });
    }
}
