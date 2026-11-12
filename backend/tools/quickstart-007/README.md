# Quickstart 007 bằng API thật (profile demo)

Chạy các bước của `specs/007-health-alert-incident/quickstart.md` mà báo cáo đối chiếu `D:\eldercare-logs\quickstart\007.md` ghi MỘT PHẦN, KHÔNG THẤY, KHÔNG CHẮC (bước ĐỦ không chạy lại). Mỗi bước in `ĐẠT`, `KHÔNG ĐẠT` hoặc `CẦN XEM TAY` (kèm lý do). Backend là `QuickstartDemoApplication` (như `tools/quickstart-000`) ở cổng 8083, schema riêng `elder_care_qs3`; không đụng `elder_care`.

## Tệp

| Tệp | Việc |
| --- | --- |
| `start.ps1` | Khởi động backend: `DB_URL` trỏ `elder_care_qs3`, `SERVER_PORT=8083`, `TEMP`/`TMP` = `D:\eldercare-tmp`, `-Djdk.net.unixdomain.tmpdir=C:\eldercare-tmp`; log `D:\eldercare-logs\qs-007-server.log` |
| `reset.cjs` | Xóa, tạo lại schema `elder_care_qs3` (chỉ schema này; chạy khi backend đã dừng) |
| `seed.sql`, `seed.cjs` | Dữ liệu điều kiện cần mục 1 ghi thẳng vào bảng (như fixture test): nhân viên, tầng 2–3, phòng, giường, người cao tuổi A, C, E, F, G (nội trú), B (bán trú thứ 2, 4, 6), hợp đồng, phân bổ giường, ca ngày/đêm 25/11 → 31/12, phân công, người thân M, R, N, K, tài khoản người cao tuổi A, B có thiết bị gắn (token `qs007-device-A`, `qs007-device-B`), dị ứng Penicillin, nguyện vọng cuối đời, nguyện vọng khi điều trị. `seed.cjs` bỏ qua nếu đã nạp |
| `setup.cjs` | Bằng API: đồng hồ giả lập 01/12/2026 08:00, CFG-M06-03 (khoảng vật lý), ngưỡng cơ sở SpO2 (cảnh báo < 94, nguy hiểm < 90), huyết áp, nhịp tim, nhiệt độ. Chạy lại được |
| `lib.cjs` | Gọi API, đăng nhập, đồng hồ, `sql()` (SELECT bằng `TEST_DB_ADMIN_*`), ghi kết quả |
| `run.cjs` | Các bước; kết quả JSON ở `D:\eldercare-logs\qs-007-result.json` |

## Chạy

Từ `backend/tools/quickstart-007`, backend demo đã dừng:

```powershell
node reset.cjs
powershell -File start.ps1        # cửa sổ riêng, chờ "Started QuickstartDemoApplication" (~1–2 phút)
node seed.cjs; node setup.cjs
node run.cjs > D:\eldercare-logs\qs-007-run.log
```

- Chạy hết khoảng 25 phút: job M05, M06, M09 chạy theo lịch mỗi phút thật (TestJob của `/testing/clock` không có job M05, M06), nên sau mỗi lần tua đồng hồ script chờ job (tối đa ~85 giây).
- Đồng hồ chỉ tua tới: 01/12 08:00 → 10:xx (chỉ số, cảnh báo, sự cố, SOS, nút hỗ trợ, lây nhiễm, nguy kịch) → 01/12 19:30 (ca đêm, Q54) → 03/12 08:05 (Q7 ngưỡng hiệu lực, Q37 ngã lần hai, chuyển viện C1 → C4, S8) → 15/12, 28/12 (Q40 cân nặng). Chạy lại phải `reset.cjs` từ đầu.
- Thông báo kiểm bằng bảng `notification_request`, `notification` (người nhận theo `staff_id`, `family_member_id`); sự kiện gửi module khác kiểm bằng `event_publication`.

## Kết quả

Lần chạy 2026-11-12 (schema sạch, nhánh `claude/intelligent-ellis-7555a7`; log `D:\eldercare-logs\qs-007-run.log`): 125 kiểm tra trên 68 bước quickstart và một bước lỗi riêng B1 — **97 ĐẠT, 3 KHÔNG ĐẠT, 25 CẦN XEM TAY**. Cả ba KHÔNG ĐẠT là lỗi backend (mục dưới); không còn KHÔNG ĐẠT do script.

- Đạt hết: Q1, Q2, Q3, Q5, Q6, Q7, Q8, Q13, Q14, Q16, Q17, Q22, Q24, Q26, Q27, Q33, Q35, Q36, Q39, Q40, Q47, Q54, Q56, Q59, K1, K2, S3, S6, S8, S9, S10, H1 → H6, H8, H9, C1, C3, C4.
- Đạt phần chạy được, phần còn lại CẦN XEM TAY: Q10, Q11, Q12, Q23, Q37, Q38, Q43, Q44, Q46, Q48, Q58, S4, S7, S12, H7, C2.
- Chỉ CẦN XEM TAY: Q18, Q19, Q20, Q31, Q57, K3, S5, S11.
- KHÔNG ĐẠT: B1, Q52 (B3), Q55 (B2).

## Lỗi backend (báo về, không sửa mã chính)

**B1. Không ai ghi, xem được chỉ số qua API trực tuyến** (US1, FR-010, FR-011; quickstart Q1 → Q5, Q40, Q52).
`POST /api/v1/residents/2001/vital-signs` bằng `demo.chamsoc` (S, phân công chăm sóc A trong ca) và `demo.dieuduong` (D, phụ trách A) → `403 PERMISSION_DENIED`, `failedLayer: SCOPE`; `GET` cùng đường dẫn cũng 403. Bác sĩ → 403 `ROLE` khi ghi (không có quyền ghi). Nguyên nhân: `VitalSignController` (`getVitalSigns`, `recordVitalSign`, `remeasureVitalSign`) có `@RequiresPermission` nhưng không có tham số `Target`, aspect dùng `Target.UNSPECIFIED`, và `PermissionService` (dòng 240) trả `false` cho phạm vi "được giao" khi đối tượng không xác định. Cùng loại lỗi 003 vừa sửa ngày 12/11 (progress.md). Không có IT gọi HTTP các đường dẫn này. Script tránh bằng khung đồng bộ ngoại tuyến (`POST /sync`, loại `M06.BAN_GHI_CHI_SO`, có `Target` đúng) nên phần ngưỡng, cảnh báo, đo lại vẫn kiểm được; bản ghi mang nhãn `OFFLINE_RECORDED`.

**B2. Lựa chọn "chuyển bệnh viện" khi nguy kịch không thực hiện được** (FR-050d, FR-049; quickstart Q55).
`POST /alerts/{id}/execute-choice` `{"choice":"TRANSFER_TO_HOSPITAL","transferOut":{…}}` trên cảnh báo nguy kịch chưa liên kết sự cố → 409 `FR-049` "Chuyển cảnh báo thành sự cố hoặc kích hoạt khẩn cấp trước khi chuyển viện"; sau `activate-emergency` (201) cảnh báo thành `CONVERTED_TO_INCIDENT` và lệnh trả 409 `FR-050e` "Cảnh báo nguy kịch đã kết thúc". `CriticalCareService`: dòng 344 đòi `alert.incidentId() != null`, dòng 415 chỉ cho `OPEN_ALERT` (Mới, Leo thang, Đã tiếp nhận, Đang xử lý, dòng 88) mà liên kết sự cố luôn đưa cảnh báo ra khỏi tập này. Không có IT cho nhánh này.

**B3. "Nguồn đã được đính chính" không tới người phụ trách** (BR-M06-06, FR-085; quickstart Q52).
Đính chính bản ghi huyết áp nguồn của cảnh báo (`POST /corrections/M06.VITAL_SIGN_RECORD/{id}`, 201) tạo `notification_request` `ALERT_SOURCE_CORRECTED` cờ `NO_RECIPIENT`. `AlertNotices.sourceCorrected` (dòng 148) truyền `alert.assigneeId()` (id tài khoản, ví dụ 3, 4, 16) vào `RecipientGroup.Staff(staffId, …)` vốn tra theo id nhân viên (`RecipientResolver` dòng 275 `byStaff`). Các thông báo khác gửi `Staff` đúng id nhân viên.

## Lệch giữa quickstart và spec, hoặc cần chủ dự án xác nhận

- **S7**: quickstart ghi "quá hạn mức Khẩn cấp → leo thang theo BR-M05-01", nhưng BR-M05-01, FR-031 ghi Khẩn cấp không leo thang theo cấp (báo đồng thời, 009 nhắc theo BR-M13-02). Backend làm theo BR (cảnh báo Khẩn cấp không có `acknowledgement_deadline`); script kiểm theo BR.
- **Q12**: quickstart đếm nhắc "đã leo thang tối đa" #1 lúc 09:45 (cùng lúc đạt tối đa), #2 lúc 10:00; backend nhắc #1 lúc 10:00 (một khoảng CFG-M05-01 sau khi đạt tối đa, đúng chữ Q-248 "mỗi khoảng bằng thời hạn tiếp nhận"). Ghi CẦN XEM TAY.
- **H7**: quickstart muốn Nhân viên chăm sóc không thấy dị ứng; FR-047k chỉ ẩn với người không có quyền xem sức khỏe, mà S trong phạm vi có quyền (FieldVisibility FULL). Cần đối chiếu Phụ lục 27.
- **K2**: R chỉ nhận yêu cầu `WISH_RECONFIRMATION` khi có quyền nhận khẩn (lọc FR-014, Q-96 với mức Khẩn); seed đặt R `receive_urgent = TRUE`.
- **Q16**: thông báo "lặp lại" (FR-038a) chỉ đếm phát sinh có dòng nguồn (hệ thống); cảnh báo tạo tay không có dòng nguồn nên không đếm. Script dùng ba lần đo huyết áp mức Cảnh báo.
- **Q36**: backend đặt mốc = lúc ghi − chu kỳ đo 4 giờ (như ví dụ quickstart: ngã 02:00 ghi 09:00 → mốc 05:00), không phải thời điểm ngã.
- Danh mục demo chưa có loại diễn biến nhóm Đánh giá và loại Xử lý không hồi sức (FR-045 cần cả hai để chuyển theo dõi); script thêm `QS_ASSESSMENT`, `QS_FIRST_AID` qua `POST /progression-types`.
- `GET /trend-rules` không trả ETag mà `PUT /trend-rules/{code}` bắt `If-Match`; script đọc `version` từ bảng.
- FR-041: khi người cao tuổi có sự cố Khẩn cấp mở, mọi lần ghi sự cố mới (kể cả không khẩn cấp) trả 409 `OPEN_EMERGENCY_INCIDENT_EXISTS` tới khi người ghi chọn tạo mới (`createDespiteOpen`); script gửi cờ này trừ ở bước Q33.

## Bước CẦN XEM TAY

Cần dữ liệu hoặc thao tác của module khác chưa có trong demo, hoặc phải xem bằng mắt:
- 005: Q10 (chấp nhận 07:00, chặn 09:00), Q11, Q38, Q48 (`nguonDaDoi`, `nguonBiHuy`; script in sự kiện đã phát), Q20 (đóng `DA_THUC_HIEN_DUNG_KHUNG`), Q37 (chặn việc ngoài khoảng), Q43 (ba đêm mất ngủ).
- 008: Q18, Q19 (bàn giao ca; seed không có ca cần bàn giao).
- 001, 004: Q57 (kết thúc lưu trú, qua đời), S4 (A Tạm vắng), Q55 (nguyện vọng mới cần tệp bản ký).
- 012, 014: K3 (cổng người thân), S5, S11 (chuyến đi), Q44 (buổi hoạt động, lượt thăm, lượt vắng), Q46 (ba module chặn khi khoanh vùng).
- Khác: Q23 (dấu ngoài phạm vi trong nhật ký), Q31 (phong bì ngoại tuyến tạo sự cố), Q58 (nhắc bù cần dừng backend thật), S7 (nhắc của 009), S12 (màn báo cáo thời gian tới nơi), C2 (hai tệp đính kèm cần S3; dòng chữ trong PDF), Q12, H7 (mục trên).

## Kết quả 2026-11-12 sau sửa lỗi (nhánh `claude/acceptance-fixes`, schema `elder_care_qs3` dựng lại sạch)

**100 ĐẠT, 0 KHÔNG ĐẠT, 25 CẦN XEM TAY** (log `D:eldercare-logsqs-007-run-fix2.log`). B1 (ghi, xem chỉ số trực tuyến), Q52 (B3, "nguồn đã được đính chính" tới người phụ trách theo mã nhân viên) và Q55 (B2) đạt; CẦN XEM TAY giữ như cũ.

- B2: chủ dự án chốt 2026-11-12, lựa chọn "chuyển bệnh viện" trên cảnh báo nguy kịch chưa có sự cố thì lệnh lập sự cố loại do người xử lý chọn (`incidentTypeId` mới trong thân `execute-choice` của api-007), gắn với cảnh báo mà không đổi trạng thái, rồi Chuyển viện. Script Q55 bỏ bước kích hoạt khẩn cấp, chuyển cảnh báo sang Đang xử lý rồi ghi lựa chọn kèm `incidentTypeId`, `handoverStaffId`.
- `transferOut.handoverStaffId` không bắt buộc trong api-007 nhưng lệnh Chuyển viện của 001/004 bắt buộc (422 `details.handoverStaffId là bắt buộc`); màn Chuyển viện của web (`TransferOutPanel`) và màn nguy kịch web, app chưa có ô người bàn giao (báo chủ dự án 2026-11-12).
