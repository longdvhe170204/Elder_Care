package com.eldercare.backend.administration.retention.api.spi;

import com.eldercare.backend.administration.retention.api.RetentionKind;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Một loại dữ liệu được lưu giữ và loại bỏ theo thời hạn (contracts/module-interfaces.md mục 1.7, research R-11; FR-050,
 * FR-051, FR-051a). Module sở hữu loại dữ liệu cài; chỉ job loại bỏ dữ liệu hết hạn gọi {@link #delete}, qua
 * {@link com.eldercare.backend.administration.retention.api.RetentionDatabase} (tài khoản {@code eldercare_retention}).
 */
public interface RetentionProvider {

    /** Loại dữ liệu, trùng loại đối tượng trong nhật ký và loại bản ghi của đính chính (ví dụ {@code M07.MEDICATION_DOSE}). */
    String dataType();

    RetentionKind retentionKind();

    /**
     * Loại đối tượng của bản ghi ở bảng dùng chung R-15 (lịch sử, bản ghi con, đối tượng nhỏ, dấu đã nhắc) để job tìm và
     * xóa các dòng đó cùng bản ghi. Mặc định là {@link #dataType()}; loại tách riêng để tính hạn nhưng ghi bảng chung bằng
     * loại khác thì trả loại đó (005 T064: task vị trí ghi {@code M04.TASK}). Bổ sung 2026-11-07.
     */
    default String sharedRowEntityType() {
        return dataType();
    }

    /** Bản ghi có mốc tính hạn trước {@code cutoff}, tối đa {@code batch} bản ghi. */
    Stream<Candidate> expiredCandidates(Instant cutoff, int batch);

    /**
     * Trang tiếp theo của {@link #expiredCandidates(Instant, int)}: ứng viên xếp theo {@code (at, id)} tăng dần, đứng sau
     * {@code after} (rỗng là trang đầu). Job lặp trang để ứng viên đầu danh sách bị giữ (FR-051a) không chặn ứng viên
     * sau (000 T112). Mặc định chỉ có trang đầu, như trước khi có hàm này.
     */
    default Stream<Candidate> expiredCandidates(Instant cutoff, Candidate after, int batch) {
        return after == null ? expiredCandidates(cutoff, batch) : Stream.empty();
    }

    /** Bản ghi đang trỏ tới bản ghi {@code id} của loại này (FR-051a). */
    Set<DataRef> referencesTo(long id);

    /**
     * Bản ghi của loại này đang trỏ tới {@code target}, một bản ghi của loại khác (FR-051a; 000 T112): module có bản ghi
     * gắn hồ sơ người cao tuổi của module khác trả các bản ghi đó, vì provider của hồ sơ không biết module khác. Mặc
     * định rỗng.
     */
    default Set<DataRef> referencing(DataRef target) {
        return Set.of();
    }

    /** Xóa bằng tài khoản {@code eldercare_retention}, trong transaction của job. */
    void delete(Collection<Long> ids);

    /** Một bản ghi đã qua hạn, kèm mốc tính hạn. */
    record Candidate(long id, Instant at) {}

    /**
     * Trang của {@code all} theo {@link #expiredCandidates(Instant, Candidate, int)}: xếp theo {@code (at, id)}, đứng sau
     * {@code after}, tối đa {@code batch}; cho provider dựng ứng viên trong bộ nhớ (000 T112).
     */
    static Stream<Candidate> page(Stream<Candidate> all, Candidate after, int batch) {
        java.util.Comparator<Candidate> order =
                java.util.Comparator.comparing(Candidate::at).thenComparingLong(Candidate::id);
        return all.filter(c -> after == null || order.compare(c, after) > 0)
                .sorted(order)
                .limit(batch);
    }

    /** Tham chiếu tới một bản ghi của loại dữ liệu khác. */
    record DataRef(String dataType, long id) {}
}
