# Quickstart 000 K1–K21 ở profile demo

Chạy tay quickstart 000 mục 3 bằng API thật (000 T103). Loại yêu cầu mẫu `M15.SAMPLE`, bản ghi mẫu `M15.SAMPLE_RECORD`, `M15.SAMPLE_OFFICE_RECORD` chỉ có trong mã test, nên backend chạy bằng `QuickstartDemoApplication` (`src/test`, gói `support.demo`) với `spring-boot:test-run`. Người dùng và quyền là bản thật của 002 (tài khoản mẫu `demo/V002_90`).

## Chạy

Từ `backend/`, PowerShell, đã nạp biến `DB_*` từ `.env`:

```powershell
.\mvnw.cmd spring-boot:test-run "-Dspring-boot.run.main-class=com.eldercare.backend.support.demo.QuickstartDemoApplication" "-Dspring-boot.run.profiles=demo" "-Dspring-boot.run.jvmArguments=-Djdk.net.unixdomain.tmpdir=C:\eldercare-tmp"
```

Khi thấy `Started QuickstartDemoApplication`, ở cửa sổ khác:

```powershell
node tools/quickstart-000/run-k1-k21.cjs
```

- `-Djdk.net.unixdomain.tmpdir` (thư mục phải có sẵn): không có thì Tomcat báo "Unable to establish loopback connection" trên máy Windows của nhóm.
- Script tua đồng hồ giả lập của demo về phía trước và đăng nhập lại sau mỗi lần tua; đổi CFG-M15-05 rồi để 24 giờ (lần chạy sau tự đưa về mặc định trước K1). K16 gán thêm vai trò Hành chính cho `demo.ketoan` rồi gỡ ở cuối.
- Endpoint `/api/v1/testing/sample-request-control`, `/api/v1/testing/sample-data/…` chỉ có khi chạy lớp này, không thuộc hợp đồng.
- Schema `elder_care` cần quyền của `eldercare_app` trên bảng mẫu (`demo/V000_90` dòng `GRANT`); thiếu thì K9, K10, K12, K14 → K17, K20 lỗi `INSERT command denied`.

## Kết quả 2026-11-01

21/22 kiểm tra đạt (K6 có hai kiểm tra). K18 trả 404: Hành chính xem nhật ký theo phạm vi phân công (Phụ lục 27, cột P), hồ sơ mẫu không gắn người cao tuổi, tầng và Hành chính ở demo không có phân công; kịch bản K18 được kiểm bằng `AuditLogFieldMaskingIT` với bản giả quyền.
