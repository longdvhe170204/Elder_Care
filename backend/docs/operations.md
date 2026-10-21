# Quy trình vận hành (000 T102)

Áp cho máy chủ chạy `backend/` với MySQL 8.0 (schema `elder_care`). Căn cứ: NFR-05, NFR-06 (Phụ lục NFR của `docs/business/nghiep-vu.md`), FR-050 → FR-051a, research R-05, R-10, R-11 của 000.

## 1. Mục tiêu

| Mục | Yêu cầu |
|-----|---------|
| Khả dụng (NFR-05) | 99,5%/tháng; bảo trì có kế hoạch ngoài khung giao ca 06:00–08:00 và 17:00–19:00 (giờ Việt Nam) |
| Sao lưu (NFR-06) | Hằng ngày, giữ 30 ngày; mất dữ liệu tối đa 24 giờ (RPO), khôi phục trong 4 giờ (RTO) |

## 2. Tài khoản cơ sở dữ liệu

| Tài khoản | Dùng cho | Biến môi trường |
|-----------|----------|-----------------|
| `eldercare_app` | ứng dụng; không có `DELETE` trên bảng nhóm 3, nhật ký | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` |
| `eldercare_retention` | chỉ job loại bỏ dữ liệu hết hạn | `DB_RETENTION_USERNAME`, `DB_RETENTION_PASSWORD` |
| tài khoản migration | Flyway (DDL, tạo tài khoản) | `DB_MIGRATION_USERNAME`, `DB_MIGRATION_PASSWORD` (thiếu thì dùng `DB_*`) |
| tài khoản sao lưu | `mysqldump`, khôi phục | do quản trị đặt, không ghi vào kho mã |

Mật khẩu chỉ đặt bằng biến môi trường hoặc kho bí mật của máy chủ, không ghi vào kho mã.

## 2a. Khởi tạo Quản lý viện đầu tiên

Làm một lần khi cài đặt, sau khi migration đã chạy (BR-M15-22, Q-310). Mở ứng dụng kèm ba tham số:

```bash
java -jar backend.jar --eldercare.bootstrap-manager.run-once=true --eldercare.bootstrap-manager.username=<tên đăng nhập> "--eldercare.bootstrap-manager.full-name=<họ tên>"
```

- Ứng dụng tạo hồ sơ nhân viên tối thiểu, tài khoản Hoạt động có vai trò Quản lý viện, rồi in mật khẩu tạm ra màn hình một lần. Chép mật khẩu giao trực tiếp cho người nhận; người đó buộc đổi mật khẩu ở lần đăng nhập đầu. Nhật ký ghi người thực hiện là khởi tạo hệ thống (`actor_type = BOOTSTRAP`).
- Hệ thống đã có tài khoản đang giữ vai trò Quản lý viện thì lệnh bị từ chối (`CONDITION_NOT_MET`, BR-M15-22), không tạo gì thêm.
- Các lần mở sau không truyền `run-once`. Không có endpoint REST cho việc này.

## 3. Job theo lịch (giờ Việt Nam)

| Job | Lịch | Ghi chú |
|-----|------|---------|
| `APPLY_DUE_REQUESTS` | phút 05, 20, 35, 50 mỗi giờ (gồm 00:05) và lúc ứng dụng khởi động | áp dụng bù yêu cầu Đã duyệt sau gián đoạn (FR-037a) |
| `REMIND_PENDING_REQUESTS` | 15 phút một lần | nhắc, báo Quản lý viện theo CFG-M15-05, CFG-M15-06 |
| `PURGE_EXPIRED_DATA` | 02:30 ngày 1 hằng tháng | loại bỏ dữ liệu hết hạn lưu giữ (FR-051) |

Lịch là thông số kỹ thuật, không phải CFG. Lịch hai job đầu đổi được bằng `eldercare.jobs.apply-due-requests.cron`, `eldercare.jobs.remind-pending-requests.cron` (giá trị `-` là tắt; chỉ profile `test` tắt). ShedLock (bảng `shedlock`) giữ cho mỗi job không chạy chồng khi có nhiều bản ứng dụng. Mỗi lần chạy có một dòng trong `scheduler_run` (`result` là `SUCCEEDED` hoặc `FAILED`); kiểm bảng này khi theo dõi.

## 4. Sao lưu hằng ngày

1. Chạy lúc 01:30 (ngoài khung giao ca, trước job loại bỏ dữ liệu 02:30 ngày 1):

   ```sh
   mysqldump --single-transaction --routines --triggers --events \
     --set-gtid-purged=OFF --default-character-set=utf8mb4 \
     -u <tài khoản sao lưu> -p elder_care | gzip > elder_care_$(date +%F).sql.gz
   ```

   `--single-transaction` cho bản chụp nhất quán của bảng InnoDB mà không khóa ứng dụng. Phải có `--triggers`: trigger chỉ ghi thêm của nhóm 3 và nhật ký là một phần của quy tắc (R-05).
2. Chép bản sao lưu ra nơi lưu trữ khác máy chủ cơ sở dữ liệu (mã hóa khi lưu, vì có dữ liệu sức khỏe).
3. Xóa bản quá 30 ngày. Không giữ bản sao lưu lâu hơn 30 ngày: bản cũ chứa dữ liệu đã hết hạn lưu giữ và đã bị loại bỏ.
4. Tệp đính kèm trên AWS S3 (001) sao lưu theo cơ chế của bucket (versioning, lifecycle 30 ngày); cấu hình cụ thể ghi ở plan 001.
5. Hằng tháng khôi phục thử một bản vào máy riêng, đo thời gian (phải dưới 4 giờ), ghi kết quả.

## 5. Khôi phục

Bản sao lưu có thể chứa dữ liệu đã bị loại bỏ sau thời điểm sao lưu (job loại bỏ chạy ngày 1 hằng tháng). Khôi phục không được làm sống lại dữ liệu đó (FR-051).

1. Dừng ứng dụng (mọi bản), báo người dùng theo kênh của viện.
2. Khôi phục bản sao lưu gần nhất vào schema `elder_care`:

   ```sh
   gunzip -c elder_care_<ngày>.sql.gz | mysql -u <tài khoản sao lưu> -p elder_care
   ```

3. Kiểm trigger và tài khoản: `SHOW TRIGGERS FROM elder_care;` có đủ trigger chỉ ghi thêm; `eldercare_app` vẫn không có `DELETE` trên bảng nhóm 3.
4. Trước khi mở cho người dùng, chạy một đợt loại bỏ dữ liệu hết hạn `PURGE_EXPIRED_DATA` (job tính hạn theo CFG-M15-03, CFG-M15-04 hiện hành và ngày hôm nay, nên xóa lại đúng phần đã bị loại bỏ), rồi kiểm bản nhật ký tóm tắt `EXPIRED_DATA_PURGED` mới và `scheduler_run`. Cách chạy: mở ứng dụng một lần với `--eldercare.jobs.purge-expired-data.run-once=true`, chưa mở cho người dùng; job chạy một đợt khi ứng dụng sẵn sàng. Kiểm xong thì dừng ứng dụng và bỏ tham số này (để lại thì mỗi lần khởi động chạy thêm một đợt, chỉ xóa dữ liệu đã hết hạn nên không hại nhưng tốn thời gian khởi động).
5. Đối chiếu bản tóm tắt các đợt loại bỏ sau thời điểm sao lưu (lưu ngoài cơ sở dữ liệu, mục 6) với bản tóm tắt vừa ghi.
5a. Kết thúc mọi phiên đăng nhập có trong bản sao lưu (002 T079, NFR-06, NFR-08): phiên đã đăng xuất, bị kết thúc vì khóa tài khoản hay gỡ thiết bị sau thời điểm sao lưu sẽ sống lại nếu để nguyên. Chạy bằng tài khoản `eldercare_app` (có `UPDATE` trên `login_session`) trước khi mở cho người dùng:

   ```sql
   UPDATE login_session SET ended_at = UTC_TIMESTAMP(6), end_reason = 'TIMED_OUT' WHERE ended_at IS NULL;
   ```

   Mọi người đăng nhập lại; người cao tuổi đăng nhập lại bằng mã QR do điều dưỡng tạo (thiết bị gắn vẫn giữ). Khóa, gỡ thiết bị, đổi quyền làm sau thời điểm sao lưu đã mất cùng dữ liệu khác, Quản lý viện làm lại theo biên bản ở bước 7.
6. Mở ứng dụng. Lúc khởi động, `APPLY_DUE_REQUESTS` tự áp dụng bù yêu cầu đã đến hạn.
7. Báo người dùng: dữ liệu nhập sau thời điểm sao lưu bị mất (tối đa 24 giờ) và cần nhập lại; ghi biên bản sự cố.

## 6. Lưu bản tóm tắt loại bỏ ngoài cơ sở dữ liệu

Sau mỗi đợt `PURGE_EXPIRED_DATA`, xuất bản tóm tắt (`audit_log` có `action = 'EXPIRED_DATA_PURGED'`) ra nơi lưu trữ của mục 4. Khi phải khôi phục từ bản sao lưu có trước đợt đó, bản tóm tắt này là căn cứ đối chiếu ở bước 5.5.

## 7. Bảo trì có kế hoạch

- Chỉ trong khung 22:00–05:00 hoặc 09:00–16:00, không chạm 06:00–08:00 và 17:00–19:00 (NFR-05). Tránh 01:30 (sao lưu) và 02:30 ngày 1 (loại bỏ dữ liệu).
- Báo trước người dùng; ứng dụng di động phần nhân viên ghi ngoại tuyến được trong lúc bảo trì (Q-01) và đồng bộ sau.
- Nâng cấp phiên bản ứng dụng: Flyway chạy migration lúc khởi động bằng tài khoản migration. Sao lưu ngay trước khi nâng cấp.
- Nâng MySQL 8.0 lên 8.4 LTS trước khi vận hành (research R-01); migration chỉ dùng cú pháp có ở cả hai bản.

## 8. Theo dõi

- `scheduler_run` có `result = 'FAILED'` hoặc job không có dòng mới quá hai chu kỳ: kiểm log ứng dụng.
- Nhật ký `REQUEST_APPLY_ERROR`: yêu cầu áp dụng lỗi, vẫn Đã duyệt và được thử lại ở lần chạy sau; lặp lại thì báo nhóm phát triển.
- Bảng `EVENT_PUBLICATION` có dòng chưa hoàn tất lâu: sự kiện chưa giao xong, được giao lại khi khởi động (R-09).

## 9. HTTPS (002 T079, NFR-08)

- Mọi kết nối của web, app di động tới `backend/` bắt buộc qua HTTPS; không mở cổng HTTP ra ngoài (chỉ chuyển hướng sang HTTPS nếu cần). Cookie phiên web có `Secure`, `HttpOnly`, `SameSite=Strict` (`SessionTokens.cookie`) nên qua HTTP trình duyệt không gửi cookie và web không đăng nhập được.
- TLS đặt ở reverse proxy trước ứng dụng (hoặc `server.ssl.*` của Spring Boot); chứng chỉ do quản trị cấp và gia hạn, khóa riêng không ghi vào kho mã.
- Ứng dụng chỉ nghe ở mạng nội bộ của máy chủ khi có reverse proxy.
