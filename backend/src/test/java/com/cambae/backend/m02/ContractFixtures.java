package com.cambae.backend.m02;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.api.dto.ContractStatus;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

/**
 * Hợp đồng dựng thẳng vào bảng {@code contract} cho test của user story khác (thay bản giả {@code FakeContractReads} từ
 * Phase 5): bỏ qua vòng đời để đặt người cao tuổi vào đúng trạng thái hợp đồng cần thử. Hợp đồng không có phiên bản nội
 * dung. {@link #reset()} chuyển các hợp đồng đã dựng sang Đã hủy (tài khoản ứng dụng không có DELETE).
 */
public class ContractFixtures {

    private final JdbcTemplate jdbc;
    private final List<Long> created = new ArrayList<>();

    ContractFixtures(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Hợp đồng mới của người cao tuổi ở {@code status}; trả mã hợp đồng. */
    public long contract(
            long residentId,
            ContractStatus status,
            ResidenceType residenceType,
            CareLevel careLevel,
            LocalDate startDate) {
        boolean signed = status == ContractStatus.ACTIVE
                || status == ContractStatus.ENDED
                || status == ContractStatus.TERMINATED;
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var statement = connection.prepareStatement(
                            "INSERT INTO contract (resident_id, residence_type, start_date, end_date, care_level,"
                                    + " residency_fee_billing_period, status, signed_date, signing_representative_id,"
                                    + " scan_file_id, termination_reason, created_by, created_at)"
                                    + " VALUES (?, ?, ?, ?, ?, 'MONTH', ?, ?, ?, ?, ?, 1, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    statement.setLong(1, residentId);
                    statement.setString(2, residenceType.name());
                    statement.setObject(3, startDate);
                    // Nội trú ngắn ngày bắt buộc có ngày kết thúc (CHECK); hợp đồng khác để mở
                    statement.setObject(
                            4, residenceType == ResidenceType.SHORT_TERM_RESIDENTIAL ? startDate.plusDays(400) : null);
                    statement.setString(5, careLevel.name());
                    statement.setString(6, status.name());
                    statement.setObject(7, signed ? startDate : null);
                    statement.setObject(8, signed ? 1L : null);
                    statement.setObject(9, signed ? 1L : null);
                    statement.setObject(10, status == ContractStatus.TERMINATED ? "Chấm dứt thử" : null);
                    statement.setObject(11, LocalDateTime.now());
                    return statement;
                },
                keys);
        long id = keys.getKey().longValue();
        jdbc.update(
                "UPDATE contract SET contract_number = ? WHERE id = ?",
                "HD-%d-%06d".formatted(startDate.getYear(), id),
                id);
        created.add(id);
        return id;
    }

    /** Hợp đồng Hiệu lực dài hạn bắt đầu ở {@code startDate}. */
    public long active(long residentId, CareLevel careLevel, LocalDate startDate) {
        return contract(residentId, ContractStatus.ACTIVE, ResidenceType.LONG_TERM_RESIDENTIAL, careLevel, startDate);
    }

    /** Chuyển các hợp đồng đã dựng sang Đã hủy để người cao tuổi lại dùng được. */
    public ContractFixtures reset() {
        created.forEach(id -> jdbc.update("UPDATE contract SET status = 'CANCELLED' WHERE id = ?", id));
        created.clear();
        return this;
    }
}
