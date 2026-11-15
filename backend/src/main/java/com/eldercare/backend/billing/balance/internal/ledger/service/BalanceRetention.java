package com.eldercare.backend.billing.balance.internal.ledger.service;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.shared.api.SharedRowRetention;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTypes;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.BalanceGovernanceStore;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * Lưu giữ sổ và giao dịch theo hồ sơ người cao tuổi (tên logic {@code LuuTruProvider}, {@code THEO_HO_SO}, CFG-M15-03;
 * data-model 017 mục 8, FR-029): tính từ lúc người đó sang trạng thái cuối. Sổ bị giữ tới khi xóa hết giao dịch trỏ tới.
 */
@Component
public class BalanceRetention {

    public enum Kind {
        LEDGER,
        TRANSACTION
    }

    private static final Set<ResidentStatus> FINAL_STATUSES =
            Set.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

    private final BalanceGovernanceStore store;
    private final RetentionDatabase database;
    private final ResidentApi residents;

    public BalanceRetention(BalanceGovernanceStore store, RetentionDatabase database, ResidentApi residents) {
        this.store = store;
        this.database = database;
        this.residents = residents;
    }

    public RetentionProvider provider(Kind kind) {
        return new Provider(kind);
    }

    private final class Provider implements RetentionProvider {

        private final Kind kind;

        private Provider(Kind kind) {
            this.kind = kind;
        }

        @Override
        public String dataType() {
            return kind == Kind.LEDGER ? BalanceTypes.LEDGER : BalanceTypes.TRANSACTION;
        }

        @Override
        public RetentionKind retentionKind() {
            return new RetentionKind.ResidentRecord();
        }

        @Override
        public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
            return expiredCandidates(cutoff, null, batch);
        }

        @Override
        public Stream<Candidate> expiredCandidates(Instant cutoff, Candidate after, int batch) {
            List<Candidate> candidates = new ArrayList<>();
            for (long residentId : residents.byStatus(FINAL_STATUSES)) {
                residents
                        .finalStatusAt(residentId)
                        .filter(at -> at.isBefore(cutoff))
                        .ifPresent(at -> ids(residentId).forEach(id -> candidates.add(new Candidate(id, at))));
            }
            return RetentionProvider.page(candidates.stream(), after, batch);
        }

        private List<Long> ids(long residentId) {
            if (kind == Kind.TRANSACTION) return store.transactionsOf(residentId);
            return store.hasLedger(residentId) ? List.of(residentId) : List.of();
        }

        /**
         * Sổ của hồ sơ {@code target} (000 T112, FR-051a). Giao dịch không gắn thẳng vào hồ sơ mà theo sổ, rồi theo giao
         * dịch được trỏ ({@link #referencesTo}): chuỗi xóa ngược thứ tự tìm thấy, nên điều chỉnh xóa trước giao dịch gốc,
         * giao dịch trước sổ (khóa ngoại {@code fk_balance_transaction_original}, {@code fk_balance_transaction_ledger}).
         */
        @Override
        public Set<DataRef> referencing(DataRef target) {
            if (kind == Kind.TRANSACTION || !SharedRowRetention.RESIDENT.equals(target.dataType())) return Set.of();
            return ids(target.id()).stream()
                    .map(id -> new DataRef(dataType(), id))
                    .collect(Collectors.toSet());
        }

        @Override
        public Set<DataRef> referencesTo(long id) {
            List<Long> children =
                    kind == Kind.LEDGER ? store.rootTransactionsOf(id) : store.transactionsReferencing(id);
            return children.stream()
                    .map(child -> new DataRef(BalanceTypes.TRANSACTION, child))
                    .collect(Collectors.toSet());
        }

        @Override
        public void delete(Collection<Long> ids) {
            String sql = kind == Kind.LEDGER
                    ? "DELETE FROM resident_balance WHERE resident_id = ?"
                    : "DELETE FROM balance_transaction WHERE id = ?";
            // Điều chỉnh, giao dịch thứ hai của cặp cấn trừ có mã lớn hơn bản được trỏ: xóa từ mã lớn xuống.
            ids.stream()
                    .distinct()
                    .sorted(Comparator.reverseOrder())
                    .forEach(id -> database.jdbc().update(sql, id));
        }
    }
}
