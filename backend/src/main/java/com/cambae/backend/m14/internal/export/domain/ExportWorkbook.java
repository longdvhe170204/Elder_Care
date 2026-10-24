package com.cambae.backend.m14.internal.export.domain;

import com.cambae.backend.common.document.ExcelDocument;
import com.cambae.backend.common.document.ExcelDocument.Column;
import com.cambae.backend.common.document.ExcelDocument.Format;
import com.cambae.backend.common.document.FacilityInfo;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m14.internal.export.domain.ExportContent.DetailLine;
import com.cambae.backend.m14.internal.export.domain.ExportContent.SummaryRow;
import com.cambae.backend.m14.internal.metric.domain.DetailRow;
import com.cambae.backend.m14.internal.metric.domain.ValueLabel;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * File XLSX của lần xuất (research O-08): trang "Tổng hợp" có đầu trang thông tin viện (CFG-M11-11), người xuất, "tính
 * đến", khoảng, bộ lọc, chế độ, mục đích rồi bảng tổng hợp; trang "Chi tiết" chỉ có khi xuất có định danh (FR-061).
 */
public final class ExportWorkbook {

    /** Dòng đầu file (FR-061: người xuất, "tính đến", bộ lọc). {@code costMode}, {@code purpose} rỗng thì bỏ dòng. */
    public record Header(
            String title,
            String exportedBy,
            Instant asOf,
            LocalDate from,
            LocalDate to,
            Map<String, Object> filters,
            String costMode,
            String purpose) {}

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Map<ValueLabel, String> LABELS = Map.of(
            ValueLabel.NO_DATA, "không có dữ liệu",
            ValueLabel.NOT_CONFIGURED, "chưa cấu hình",
            ValueLabel.DAY_NOT_ENDED, "ngày chưa kết thúc",
            ValueLabel.PENDING_REVIEW, "có bản ghi chờ xem lại",
            ValueLabel.PURGED_BY_RETENTION, "số liệu không đầy đủ do đã loại bỏ theo lưu trữ",
            ValueLabel.PROVISIONAL, "tạm tính",
            ValueLabel.WITHIN_YOUR_SCOPE, "trong phạm vi của bạn");

    private static final List<Column> SUMMARY = List.of(
            new Column("Mã chỉ tiêu", Format.TEXT),
            new Column("Chỉ tiêu", Format.TEXT),
            new Column("Nhóm", Format.TEXT),
            new Column("Giá trị", Format.NUMBER),
            new Column("Tử số", Format.NUMBER),
            new Column("Mẫu số", Format.NUMBER),
            new Column("Ghi chú", Format.TEXT));

    private static final List<Column> DETAIL = List.of(
            new Column("Mã chỉ tiêu", Format.TEXT),
            new Column("Bản ghi", Format.TEXT),
            new Column("Loại", Format.TEXT),
            new Column("Trạng thái", Format.TEXT),
            new Column("Mốc", Format.DATE_TIME),
            new Column("Tầng/khu", Format.TEXT),
            new Column("Người cao tuổi", Format.TEXT),
            new Column("Người thực hiện", Format.TEXT),
            new Column("Kết quả", Format.TEXT),
            new Column("Chờ xem lại", Format.TEXT));

    private ExportWorkbook() {}

    public static byte[] build(FacilityInfo facility, Header header, ExportContent content) {
        ExcelDocument doc = new ExcelDocument();
        ExcelDocument.Sheet summary = doc.sheet("Tổng hợp")
                .header(facility, header.title())
                .line("Người xuất: " + header.exportedBy())
                .line("Tính đến: " + DATE_TIME.format(local(header.asOf())))
                .line("Khoảng: " + DATE.format(header.from()) + " – " + DATE.format(header.to()))
                .line("Bộ lọc: " + (header.filters().isEmpty() ? "không" : describe(header.filters())));
        if (header.costMode() != null) {
            summary.line("Chế độ chi phí: " + header.costMode());
        }
        summary.line("Có định danh người cao tuổi: " + (content.includeIdentity() ? "có" : "không"));
        if (header.purpose() != null) {
            summary.line("Mục đích: " + header.purpose());
        }
        summary.table(
                SUMMARY, content.summary().stream().map(ExportWorkbook::row).toList());
        if (content.includeIdentity()) {
            doc.sheet("Chi tiết")
                    .table(
                            DETAIL,
                            content.details().stream().map(ExportWorkbook::row).toList());
        }
        return doc.toBytes();
    }

    private static List<Object> row(SummaryRow r) {
        String notes = r.labels().stream()
                .map(l -> l == ValueLabel.PENDING_REVIEW
                        ? LABELS.get(l) + " (" + r.pendingReviewCount() + ")"
                        : LABELS.get(l))
                .sorted()
                .collect(Collectors.joining("; "));
        return Arrays.asList(
                r.metricCode(),
                r.metricName(),
                r.breakdownKey() == null ? "Tổng" : r.breakdownKey(),
                r.value(),
                r.numerator(),
                r.denominator(),
                notes);
    }

    private static List<Object> row(DetailLine line) {
        DetailRow r = line.row();
        return Arrays.asList(
                line.metricCode(),
                r.sourceKey(),
                r.type(),
                r.status(),
                r.referenceTime() == null ? null : local(r.referenceTime()),
                r.floorArea(),
                r.resident(),
                r.performedBy(),
                r.result() == null || r.result().isEmpty() ? null : describe(r.result()),
                r.pendingReview() ? "có" : "không");
    }

    private static LocalDateTime local(Instant instant) {
        return LocalDateTime.ofInstant(instant, BusinessTime.ZONE);
    }

    private static String describe(Map<String, Object> map) {
        return map.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", "));
    }
}
