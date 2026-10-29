package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.support.TestUsers;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** T020/T025 phần khóa: gọi khóa thật trước INSERT SQL để tách kiểm đồng thời khỏi nguồn M01 chưa có. */
class FR_020_AllocationLocksIT extends StructureTestSupport {
    @Autowired
    ApplicationContext context;

    @Autowired
    PlatformTransactionManager transactions;

    private void lock(long resident, List<Long> beds) {
        assertThat(context.containsBean("allocationLocks"))
                .as("Cần bộ khóa tuần tự G-03")
                .isTrue();
        var bean = context.getBean("allocationLocks");
        try {
            bean.getClass().getMethod("lock", long.class, List.class).invoke(bean, resident, beds);
        } catch (InvocationTargetException error) {
            if (error.getCause() instanceof RuntimeException cause) throw cause;
            throw new AssertionError(error.getCause());
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    @Test
    void FR_020_tenTransactionsForOneBedOnlyOneInsertCommits() throws Exception {
        assertThat(context.containsBean("allocationLocks"))
                .as("Cần bộ khóa tuần tự G-03")
                .isTrue();
        var start = new CountDownLatch(1);
        var tasks = new ArrayList<Future<Boolean>>();
        try (var pool = Executors.newFixedThreadPool(10)) {
            for (int i = 0; i < 10; i++)
                tasks.add(pool.submit(() -> {
                    if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Không mở được cổng bắt đầu test");
                    try {
                        new TransactionTemplate(transactions).executeWithoutResult(status -> {
                            long resident = TestUsers.nextId();
                            lock(resident, List.of(bed));
                            jdbc.update(
                                    "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,'FUTURE','2026-11-01','BASIC','CONTRACT')",
                                    resident,
                                    bed);
                        });
                        return true;
                    } catch (DataAccessException error) {
                        assertThat(error.getMostSpecificCause())
                                .isInstanceOfSatisfying(
                                        java.sql.SQLException.class,
                                        e -> assertThat(e.getSQLState()).isEqualTo("45000"));
                        return false;
                    }
                }));
            start.countDown();
            int succeeded = 0;
            for (var task : tasks) if (task.get(60, TimeUnit.SECONDS)) succeeded++;
            assertThat(succeeded).isEqualTo(1);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bed_allocation WHERE bed_id=?", Integer.class, bed))
                .isEqualTo(1);
    }

    @Test
    void FR_020_lockRequiresCallerTransactionAndMissingBedRollsBackTechnicalRow() {
        assertThat(context.containsBean("allocationLocks"))
                .as("Cần bộ khóa tuần tự G-03")
                .isTrue();
        long resident = TestUsers.nextId();
        assertThatThrownBy(() -> lock(resident, List.of(bed)))
                .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
        assertThatThrownBy(() -> new TransactionTemplate(transactions)
                        .executeWithoutResult(status -> lock(resident, List.of(Long.MAX_VALUE))))
                .isInstanceOf(com.eldercare.backend.common.error.NotFoundException.class);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM resident_position WHERE resident_id=?", Integer.class, resident))
                .isZero();
    }

    @Test
    void FR_020_existingResidentCommandsSerializeWithoutLockUpgradeDeadlock() throws Exception {
        long resident = TestUsers.nextId();
        jdbc.update("INSERT INTO resident_position(resident_id) VALUES(?)", resident);
        var start = new CountDownLatch(1);
        var tasks = new ArrayList<Future<?>>();
        try (var pool = Executors.newFixedThreadPool(10)) {
            for (int i = 0; i < 10; i++)
                tasks.add(pool.submit(() -> {
                    if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Không mở được cổng bắt đầu test");
                    new TransactionTemplate(transactions).executeWithoutResult(status -> {
                        lock(resident, List.of(bed));
                        jdbc.update("UPDATE resident_position SET version=version+1 WHERE resident_id=?", resident);
                    });
                    return null;
                }));
            start.countDown();
            for (var task : tasks) task.get(60, TimeUnit.SECONDS);
        }
        assertThat(jdbc.queryForObject(
                        "SELECT version FROM resident_position WHERE resident_id=?", Integer.class, resident))
                .isEqualTo(10);
    }
}
