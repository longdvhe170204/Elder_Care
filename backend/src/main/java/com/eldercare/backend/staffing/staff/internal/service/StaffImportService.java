package com.eldercare.backend.staffing.staff.internal.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.administration.parameter.api.*;
import com.eldercare.backend.administration.parameter.api.dto.*;
import com.eldercare.backend.common.document.*;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.staff.internal.domain.*;
import com.eldercare.backend.staffing.staff.internal.persistence.StaffImportStore;
import java.io.*;
import java.util.*;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

/** T047: file chỉ ở bộ nhớ; xác nhận kiểm lại và chèn toàn bộ cùng nhật ký trong một transaction. */
@Service
public class StaffImportService {
    public static final String MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final List<String> HEADERS =
            List.of("Họ tên", "Chức danh", "Số điện thoại", "Ngày bắt đầu làm việc", "Chuyên môn", "Email");
    private final StaffImportStore store;
    private final ParameterApi parameters;
    private final ObjectProvider<CurrentUser> users;
    private final BusinessTime time;
    private final AuditLogApi audit;
    private final TransactionTemplate tx;

    public StaffImportService(
            StaffImportStore store,
            ParameterApi parameters,
            ObjectProvider<CurrentUser> users,
            BusinessTime time,
            AuditLogApi audit,
            PlatformTransactionManager transactions) {
        this.store = store;
        this.parameters = parameters;
        this.users = users;
        this.time = time;
        this.audit = audit;
        tx = new TransactionTemplate(transactions);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    }

    @RequiresPermission(function = Function.STAFF_PROFILE, operation = Operation.CREATE)
    public byte[] template() {
        var x = new ExcelDocument();
        x.sheet("NhanVien")
                .table(
                        HEADERS.stream()
                                .map(h -> new ExcelDocument.Column(h, ExcelDocument.Format.TEXT))
                                .toList(),
                        List.of());
        x.sheet("HuongDan")
                .line(
                        "Bốn cột đầu bắt buộc. Ngày: ô ngày Excel hoặc dd/MM/yyyy. Số điện thoại: ô chữ, 10 chữ số bắt đầu bằng 0.")
                .line("Chuyên môn và Email không bắt buộc. Không đổi thứ tự hoặc tên cột ở dòng 1.");
        return x.toBytes();
    }

    @RequiresPermission(function = Function.STAFF_PROFILE, operation = Operation.CREATE)
    public StaffImportData.Preview preview(String name, String mime, byte[] file) {
        return check(name, read(name, mime, file), false);
    }

    @RequiresPermission(function = Function.STAFF_PROFILE, operation = Operation.CREATE)
    public StaffImportData.Result confirm(String name, String mime, byte[] file) {
        var rows = read(name, mime, file);
        for (int attempt = 0; ; attempt++)
            try {
                return tx.execute(status -> {
                    var p = check(name, rows, true);
                    if (!p.canConfirm()) throw new StaffImportRejected(p);
                    long actor = users.getObject().accountId();
                    var at = time.now();
                    long batch = store.batch(name, p.rowCount(), actor, at);
                    var ids = store.insert(batch, p.rows(), actor, at);
                    audit.write(AuditLogEntry.builder()
                            .action(new AuditAction("STAFF_IMPORTED", false))
                            .entity("M09.STAFF_IMPORT_BATCH", batch)
                            .after(Map.of("fileName", name, "rowCount", p.rowCount(), "staffIds", ids))
                            .build());
                    return new StaffImportData.Result(batch, ids.size(), ids);
                });
            } catch (PessimisticLockingFailureException ex) {
                if (attempt >= 2) throw ex;
            }
    }

    private StaffImportData.Preview check(String name, List<ExcelRow> raw, boolean lock) {
        var errors = new ArrayList<StaffImportData.Error>();
        var rows = raw.stream()
                .map(r -> StaffImportRowValidator.validate(r, errors))
                .toList();
        var phones = rows.stream()
                .map(StaffImportData.Row::phoneNumber)
                .filter(Objects::nonNull)
                .filter(p -> p.matches("0[0-9]{9}"))
                .distinct()
                .sorted()
                .toList();
        var duplicates = store.duplicates(phones, lock);
        var seen = new HashMap<String, Integer>();
        for (var r : rows) {
            String phone = r.phoneNumber();
            if (phone == null || !phone.matches("0[0-9]{9}")) continue;
            Integer first = seen.putIfAbsent(phone, r.rowNumber());
            if (first != null)
                errors.add(new StaffImportData.Error(
                        r.rowNumber(),
                        "phoneNumber",
                        StaffImportData.ErrorCode.DUPLICATE_FILE_ROW,
                        "Trùng số điện thoại với dòng " + first,
                        null,
                        first));
            Long existing = duplicates.get(phone);
            if (existing != null)
                errors.add(new StaffImportData.Error(
                        r.rowNumber(),
                        "phoneNumber",
                        StaffImportData.ErrorCode.DUPLICATE_STAFF_PROFILE,
                        "Trùng số điện thoại với hồ sơ đã có",
                        existing,
                        null));
        }
        return new StaffImportData.Preview(
                name, rows.size(), rows, List.copyOf(errors), !rows.isEmpty() && errors.isEmpty());
    }

    private List<ExcelRow> read(String name, String mime, byte[] bytes) {
        if (name == null
                || name.length() > 255
                || !name.toLowerCase(Locale.ROOT).endsWith(".xlsx")
                || !MIME.equals(mime)
                || bytes == null
                || bytes.length == 0
                || bytes.length > 5 * 1024 * 1024) throw invalid();
        int limit = parameters.read(ParameterCode.CFG_M09_13).asInt();
        // Giới hạn kỹ thuật giải nén/ô, không phải giá trị nghiệp vụ; chặn file nén phình trước khi đọc workbook.
        try (var zip = ZipFile.builder()
                .setSeekableByteChannel(new SeekableInMemoryByteChannel(bytes))
                .get()) {
            long total = 0;
            int entries = 0;
            boolean content = false;
            byte[] buffer = new byte[8192];
            var iterator = zip.getEntries();
            while (iterator.hasMoreElements()) {
                var entry = iterator.nextElement();
                if (++entries > 1024 || entry.getName().contains("vbaProject") || !zip.canReadEntryData(entry))
                    throw invalid();
                if (entry.getName().equals("[Content_Types].xml")) content = true;
                try (var stream = zip.getInputStream(entry)) {
                    int n;
                    while ((n = stream.read(buffer)) != -1) {
                        total += n;
                        if (total > 64L * 1024 * 1024) throw invalid();
                    }
                }
            }
            if (!content) throw invalid();
        } catch (IOException ex) {
            var failure = invalid();
            failure.initCause(ex);
            throw failure;
        }
        try (var workbook = new ReadableWorkbook(new ByteArrayInputStream(bytes));
                var stream = workbook.getFirstSheet().openStream()) {
            if (!"NhanVien".equals(workbook.getFirstSheet().getName())) throw invalid();
            var it = stream.iterator();
            if (!it.hasNext()) throw invalid();
            var header = it.next();
            if (header.getRowNum() != 1 || header.getCellCount() != 6) throw invalid();
            for (int n = 0; n < 6; n++) if (!HEADERS.get(n).equals(header.getCellText(n))) throw invalid();
            int count = 0, cells = 0;
            while (it.hasNext()) {
                var r = it.next();
                cells += r.getCellCount();
                if (cells > Math.max(10000, (long) (limit + 1) * 12) || r.getCellCount() > 6) throw invalid();
                if (r.stream().anyMatch(c -> c != null && !c.getText().isBlank()) && ++count > limit) throw rowLimit();
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw invalid();
        }
        return ExcelDocument.read(bytes, HEADERS, limit);
    }

    private static BusinessException invalid() {
        return new BusinessException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "INVALID_IMPORT_TEMPLATE",
                "File cần đúng mẫu .xlsx, tối đa 5 MB và giữ nguyên các cột");
    }

    private static BusinessException rowLimit() {
        return new BusinessException(
                HttpStatus.UNPROCESSABLE_ENTITY, "IMPORT_ROW_LIMIT_EXCEEDED", "File vượt số dòng được viện cấu hình");
    }
}
