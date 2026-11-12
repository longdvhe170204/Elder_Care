# Quickstart 004 mục 3, 6 ở profile demo

Chạy bằng API thật các bước của `specs/004-admission-residency/quickstart.md` mục 3, 6 mà báo cáo đối chiếu `D:\eldercare-logs\quickstart\004.md` ghi MỘT PHẦN hoặc KHÔNG THẤY test. Bỏ bước lỗi thời theo Q-394, Q-395 (Q17, Q26, Q27, Q28, phần điểm của Q21, phần hệ số của Q25, Q29); Q18, Q25, Q29 chạy theo hành vi mới. Bỏ các bước báo cáo ghi ĐỦ (Q10, Q11, Q13, Q14, Q19, Q32, Q34). Mỗi kiểm tra in `ĐẠT`, `KHÔNG ĐẠT`, `CẦN XEM TAY` (không có HTTP để kiểm, ghi chỗ xem) hoặc `GHI CHÚ`.

## Chạy

Backend là `QuickstartDemoApplication` (`src/test`, xem `tools/quickstart-000/README.md`) trên một schema demo riêng, không dùng `elder_care`. Từ `backend/`, Git Bash, đã có `.env`:

```bash
set -a; . ./.env; set +a
export DB_URL=jdbc:mysql://localhost:3306/elder_care_qs2 TEMP='D:\eldercare-tmp' TMP='D:\eldercare-tmp'
./mvnw -q spring-boot:test-run "-Dspring-boot.run.main-class=com.eldercare.backend.support.demo.QuickstartDemoApplication" "-Dspring-boot.run.profiles=demo" "-Dspring-boot.run.arguments=--server.port=8082" "-Dspring-boot.run.jvmArguments=-Djdk.net.unixdomain.tmpdir=C:\eldercare-tmp -Djava.io.tmpdir=D:\eldercare-tmp" > /d/eldercare-logs/qs-004-backend.log 2>&1
```

Lần đầu trên schema mới: sau khi Flyway chạy xong, nạp hồ sơ nhân viên 9001 → 9009 mà tài khoản `demo/V002_90` trỏ tới (bàn giao, người xác nhận cần mã nhân viên):

```bash
mysql -uroot -p elder_care_qs2 < tools/quickstart-004/seed-staff.sql
```

Khi thấy `Started QuickstartDemoApplication`, ở cửa sổ khác:

```bash
QS_PORT=8082 node tools/quickstart-004/run.cjs > /d/eldercare-logs/qs-004-run-all.log
```

- `run.cjs 1`, `2`, `3` chạy riêng từng phần; phần 2, 3 tự dựng cơ cấu phòng và người cao tuổi riêng. Mỗi lần chạy dùng hậu tố mới nên chạy lại được; mã thoát 1 khi có bước KHÔNG ĐẠT.
- Script tua đồng hồ giả lập về phía trước (khoảng 60 ngày mỗi lần chạy đủ) và đăng nhập lại sau mỗi lần tua. Job M02_DAILY chỉ xử lý "hôm nay" từ 00:15, nên script tua tới 00:20.
- `db.cjs` đọc `backend/.env`, truy vấn schema `QS_SCHEMA` (mặc định `elder_care_qs2`, từ chối `elder_care`) bằng `mysql` CLI: tài khoản quản trị để kiểm (`status_history`, `audit_log`, `event_publication`), `eldercare_app` để thử sửa trực tiếp (Q7, Q38).
- Endpoint `/api/v1/testing/jobs` (`support/demo/QuickstartDemoApplicationJobs`, chỉ profile demo, không thuộc hợp đồng) chạy mọi `ScheduledJob` theo tên, kể cả job không có trong `/testing/clock runJobs`.
- Dữ liệu dựng bằng API thật của 001, 002, 003, 010, 012, 017: tài khoản người thân (Q16) qua `/family-accounts`, quyền đón (Q23) qua yêu cầu đổi quyền kèm bản ký, khu nghỉ bán trú và CFG-M03-01 (Q30). Lần đầu đặt CFG-M03-01 = 50 nếu chưa có giá trị.

## Kết quả 2026-11-12 (schema `elder_care_qs2`)

60 ĐẠT, 8 KHÔNG ĐẠT, 6 CẦN XEM TAY, 3 GHI CHÚ (log `D:\eldercare-logs\qs-004-run-all.log`). Cả 8 KHÔNG ĐẠT là lỗi backend hoặc lệch spec ↔ hợp đồng, không phải lỗi script:

| Bước | Lỗi | Request, phản hồi | Căn cứ |
| --- | --- | --- | --- |
| Q6 | "Ghi nhận đã ký" hợp đồng nội trú không chặn khi chưa có phân bổ giường từ ngày bắt đầu | `POST /contracts/{id}/record-signed` (A chưa có `bed-allocation`) → 200 `ACTIVE`, mong đợi 409. `ContractService.recordSigned` không có kiểm giường | FR-021 |
| Q15 | Hủy yêu cầu thay đổi đã duyệt không giải phóng thuộc tính: phụ lục giữ `PENDING_EFFECTIVE`, yêu cầu cùng thuộc tính bị chặn mãi | `POST /approval-requests/{id}/cancel` → 200; listener `ResidencyChangeListeners.onCancelled` (bất đồng bộ) ném `IllegalStateException: Không có người dùng hiện tại` khi `handler.withdrawn` ghi nhật ký; `event_publication` không hoàn tất; duyệt yêu cầu hai → 409 `DUPLICATE_PENDING_ATTRIBUTE` | FR-046 (US5 #7) |
| Q21 (2 kiểm tra) | Job hết hạn giữ chỗ của 003 lỗi khi giường giữ cho hồ sơ chờ: cả lượt rollback, E vẫn `CONTACTED`, giường vẫn `HELD`, HC không được báo | job `EXPIRE_BED_HOLDS` → 500 `Thay đổi do Bộ lập lịch phải có căn cứ (FR-045)` ở `WaitingListTransition.holdExpired` (nhật ký không có `basis`) | FR-015, BR-M02-02, SC-005; 000 FR-045 |
| Q35, Q39 (2 kiểm tra) | Việc `REMINDERS` của M02_DAILY lỗi mọi ngày khi có người qua đời còn việc mở: mất mọi nhắc Hành chính của 004 (hợp đồng sắp hết hạn, ngắn ngày, quá hạn, việc sau qua đời, chưa tiếp nhận…) | log `Việc REMINDERS của M02_DAILY ngày … lỗi`: `PostDeathTaskService.recalculate` (dòng 126) ghi nhật ký không có `basis` khi Bộ lập lịch chạy. Báo QL quá hạn (`CONTRACT_OVERDUE_ESCALATED`) và dấu `overdueSince` vẫn đúng | FR-026, FR-029, FR-072, SC-011 |
| Q3 | Không có chỉ báo "chưa đánh giá" khi lập hợp đồng lúc chưa có đánh giá đầu vào | `POST /contracts` → 201, `warnings: []`; api-004 cũng không có trường này cho hợp đồng (chỉ `notAssessed` của danh sách chờ). Lệch spec ↔ hợp đồng, cần chủ dự án quyết | FR-025 |

Cùng gốc với Q21, Q35 (thấy trong log backend, không thuộc bước của báo cáo): việc `ENDINGS` của M02_DAILY lỗi ở `EndingOverdueTask.run` (dòng 64), nên dấu "quá ngày dự kiến" (FR-066a, Q34) không được đặt khi chạy thật dù `FR_066a_PastExpectedDateIT` xanh.

CẦN XEM TAY: Q24, Q30 (tỷ lệ phí ngày vắng chỉ có ở `ResidencyDayApi` nội bộ, xem bảng kỳ 010); Q31 (hai mục Chưa đạt cần đồ gửi 013 và cảnh báo 007 thật); Q36 (năm mục việc sau qua đời do 006, 007, 010, 013, 017 sở hữu); Q40 (demo không dừng được Bộ lập lịch); Q42' (`thongTinQuyetToan` là API Java, 017 chỉ dùng trong tệp bảng quyết toán).

GHI CHÚ: Q3 lập hợp đồng khi chưa có người đại diện → 409 `REPRESENTATIVE_REQUIRED` (FR-019), script thêm người đại diện rồi lập lại; Q1c phản hồi lệnh ẩn `reason` với chính Hành chính vừa nhập (che trường của 001); Q43' không dựng khoản nợ 3.000.000 (cần bảng kỳ cuối 010 đã chốt). Q30 khai báo ngày nghỉ thiếu lý do trả 400 (api-004 ghi `reason` bắt buộc), quickstart ghi 422.
