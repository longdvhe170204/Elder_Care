# Thư viện chung cho script nghiệm thu quickstart

Script `tools/quickstart-<NNN>/*.cjs` chạy các bước quickstart bằng API thật của backend demo, mỗi phiên trên cổng và schema riêng. Không chạy trên schema `elder_care` (dữ liệu demo chung của máy; `qs.cjs` chặn).

- `qs.cjs`: `login`, `call`/`get`/`post`/`put`, đồng hồ giả lập (`clock`, `now`, `runJobs`; tua thì tự đăng nhập lại), ghi kết quả (`check`, `manual`, `skip`, `step`, `summary`), SQL trên schema demo (`sql`, `one`, `insertId`, `s`) bằng tài khoản `TEST_DB_ADMIN_*` của `backend/.env`.
- `world.cjs`: điều kiện cần mà dữ liệu mẫu `demo/V*_90` chưa có. `base()` = hồ sơ nhân viên 9001 → 9009 của tài khoản mẫu (kèm bộ phận) và cơ cấu Khu QA – tầng QT1, QT2 – phòng QP101, QP102, QP201, QP202 – giường G1, G2 tạo qua API 003 (phần nào có rồi thì dùng lại). Thêm: `floorManager`, `shift`, `member`, `lead`, `assignResident`, `resident` (đang lưu trú, hợp đồng Hiệu lực, có thể kèm giường), `freeBed`, `assignLead` (API 008). Phần không có API tạo trực tiếp ghi thẳng như `support/RealCareFixture.java`.
- `smoke.cjs`: khói của hai file trên; chạy lại được nhiều lần.

Lưu ý khi dùng:

- Mức chăm sóc: API 003 (phòng, loại phòng, `bed_allocation`) dùng `BASIC/FREQUENT/SPECIAL`; 001, 004 (`resident`, `contract`) dùng `BASIC/REGULAR/SPECIAL`. `world.resident({ careLevel })` nhận mã 001 và tự đổi khi ghi phân bổ giường.
- Bộ phận: API 008 dùng mã logic (`GET /departments`, là id cũ trong `attributes.legacy`); `world.department(name)` trả id vật lý của `catalog_item` cho bảng `staff_department`.
- Lịch ca duy nhất theo tháng – tầng: `world.shift` dùng chung lịch của tầng trong tháng.
- `summary(feature)` ghi `D:/eldercare-logs/qs-results/<QS_DB>/qs-<feature>.json`.

## Cổng và schema của từng phiên

| Phiên | `QS_BASE` | `QS_DB` |
|---|---|---|
| chung, 000 | `http://localhost:8081/api/v1` (mặc định) | `elder_care_qs1` (mặc định) |
| 004 | `http://localhost:8082/api/v1` | `elder_care_qs2` |
| 007 | `http://localhost:8083/api/v1` | `elder_care_qs3` |

## Chạy

Cần `mysql` trong `PATH` (`C:\Program Files\MySQL\MySQL Server 8.0\bin`). Từ `backend/`, Git Bash, đã có `.env`:

```bash
set -a; . ./.env; set +a
MYSQL_PWD="$TEST_DB_ADMIN_PASSWORD" mysql -u "$TEST_DB_ADMIN_USER" -e "CREATE DATABASE IF NOT EXISTS elder_care_qs1 CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci"
export DB_URL=jdbc:mysql://localhost:3306/elder_care_qs1 TEMP='D:\eldercare-tmp' TMP='D:\eldercare-tmp'
./mvnw spring-boot:test-run "-Dspring-boot.run.main-class=com.eldercare.backend.support.demo.QuickstartDemoApplication" "-Dspring-boot.run.profiles=demo" "-Dspring-boot.run.arguments=--server.port=8081 --spring.jpa.show-sql=false" "-Dspring-boot.run.jvmArguments=-Djdk.net.unixdomain.tmpdir=C:\eldercare-tmp" > D:/eldercare-logs/qs-common-backend.log 2>&1
```

Flyway (profile demo) tạo bảng và nạp dữ liệu mẫu khi khởi động. Khi log có `Started QuickstartDemoApplication`, ở cửa sổ khác:

```bash
node tools/quickstart-common/smoke.cjs
```

Phiên khác đổi cổng, schema ở cả hai lệnh và đặt `QS_BASE`, `QS_DB` trước khi chạy script.

## Kết quả 2026-11-12

`smoke.cjs` trên 8081/`elder_care_qs1`: 10/10 đạt, chạy lại ba lần liên tiếp đều 10/10.
