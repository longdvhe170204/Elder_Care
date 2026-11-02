package com.eldercare.backend.administration.shared;

import static com.eldercare.backend.administration.shared.SharedTestKinds.OTHER_RECORD;
import static com.eldercare.backend.administration.shared.SharedTestKinds.RECORD;
import static com.eldercare.backend.administration.shared.SharedTestKinds.unique;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.shared.SharedTestKinds.Attributes;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.Change;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.VersionMismatchException;
import com.eldercare.backend.support.TestUsers;
import java.time.LocalDate;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** Hợp đồng của {@link SimpleRecordApi} (khóa theo dòng, constitution XII; module-interfaces 000 mục 1.8). */
public interface SimpleRecordApiContract {

    SimpleRecordApi records();

    <R> R tx(Supplier<R> work);

    LocalDate D0 = LocalDate.parse("2026-03-01");

    private static NewSimpleRecord<Attributes> record(Long residentId, EntityRef parent, String status, String key) {
        return new NewSimpleRecord<>(residentId, parent, status, new Attributes("đỏ", 1), D0, null, key, 11L, false);
    }

    @Test
    default void create_thenRequire_andList() {
        long resident = TestUsers.nextId();
        EntityRef parent = new EntityRef("M15.TEST_ENTITY", TestUsers.nextId());
        SimpleRecord<Attributes> open = tx(() -> records().create(RECORD, record(resident, parent, "OPEN", null)));
        SimpleRecord<Attributes> done = tx(() -> records().create(RECORD, record(resident, null, "DONE", null)));

        assertThat(tx(() -> records().require(RECORD, open.id()))).isEqualTo(open);
        assertThat(open.lockedAt()).isNull();
        assertThat(open.createdBy()).isEqualTo(11L);
        assertThat(tx(() -> records().listByResident(RECORD, resident, Set.of("OPEN"))))
                .extracting(SimpleRecord::id)
                .containsExactly(open.id());
        assertThat(tx(() -> records().listByResident(RECORD, resident, Set.of())))
                .extracting(SimpleRecord::id)
                .containsExactly(open.id(), done.id());
        assertThat(tx(() -> records().listByParent(RECORD, parent)))
                .extracting(SimpleRecord::id)
                .containsExactly(open.id());
        assertThatThrownBy(() -> tx(() -> records().require(OTHER_RECORD, open.id())))
                .isInstanceOf(NotFoundException.class);
    }

    /** Đổi api 2026-10-30 (002 T098): theo loại và trạng thái, không cần người cao tuổi hay đối tượng cha. */
    @Test
    default void listByStatus_filtersKindAndStatus_byId() {
        String waiting = "W" + TestUsers.nextId();
        String done = "D" + TestUsers.nextId();
        SimpleRecord<Attributes> first = tx(() -> records().create(RECORD, record(null, null, waiting, null)));
        SimpleRecord<Attributes> closed = tx(() -> records().create(RECORD, record(null, null, done, null)));
        SimpleRecord<Attributes> second =
                tx(() -> records().create(RECORD, record(TestUsers.nextId(), null, waiting, null)));
        tx(() -> records().create(OTHER_RECORD, record(null, null, waiting, null)));

        assertThat(tx(() -> records().listByStatus(RECORD, Set.of(waiting))))
                .extracting(SimpleRecord::id)
                .containsExactly(first.id(), second.id());
        assertThat(tx(() -> records().listByStatus(RECORD, Set.of(waiting, done))))
                .extracting(SimpleRecord::id)
                .containsExactly(first.id(), closed.id(), second.id());
    }

    @Test
    default void change_checksVersion_thenLockBlocksFurtherChanges() {
        SimpleRecord<Attributes> r = tx(() -> records().create(RECORD, record(TestUsers.nextId(), null, "OPEN", null)));

        Change<SimpleRecord<Attributes>> confirmed = tx(() -> {
            records().lockForUpdate(RECORD, r.id());
            return records()
                    .change(
                            RECORD,
                            r.id(),
                            r.version(),
                            new RecordChange<>("CONFIRMED", new Attributes("xanh", 2), D0.plusDays(3), null, true));
        });

        assertThat(confirmed.before()).isEqualTo(r);
        assertThat(confirmed.after().status()).isEqualTo("CONFIRMED");
        assertThat(confirmed.after().effectiveTo()).isEqualTo(D0.plusDays(3));
        assertThat(confirmed.after().lockedAt()).isNotNull();
        assertThatThrownBy(() -> tx(() -> records()
                        .change(RECORD, r.id(), r.version(), new RecordChange<>("X", r.content(), null, null, false))))
                .isInstanceOf(VersionMismatchException.class);
        assertThatThrownBy(() -> tx(() -> records()
                        .change(
                                RECORD,
                                r.id(),
                                confirmed.after().version(),
                                new RecordChange<>("X", r.content(), null, null, false))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo("RECORD_LOCKED");
    }

    @Test
    default void uniqueKey_isEnforced_andReleasedWhenSetToNull() {
        String key = unique("R");
        SimpleRecord<Attributes> first = tx(() -> records().create(RECORD, record(null, null, "OPEN", key)));

        assertThatThrownBy(() -> tx(() -> records().create(RECORD, record(null, null, "OPEN", key))))
                .isInstanceOf(ConditionNotMetException.class);
        assertThat(tx(() -> records().findByUniqueKey(RECORD, key))).contains(first);

        tx(() -> records()
                .change(
                        RECORD,
                        first.id(),
                        first.version(),
                        new RecordChange<>("ENDED", first.content(), D0, null, false)));
        tx(() -> records().create(RECORD, record(null, null, "OPEN", key)));
    }
}
