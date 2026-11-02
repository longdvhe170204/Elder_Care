# T001 và phần độc lập T006 — 2026-10-29

Nhánh impl/010-incurred-costs, đã fast-forward từ da9636a lên origin/develop e7b2b71 theo yêu cầu người dùng; worktree .worktrees/010-incurred-costs do người dùng tạo. Chi tiết bảo toàn công việc và Q-394 → Q-397 ở [develop-sync-2026-10-29.md](develop-sync-2026-10-29.md). Bản dịch HTTP/glossary đã được A/chủ dự án duyệt qua E trước viết mã. Chưa commit/push/PR/merge; không sửa worktree 008.

> **Cập nhật sau T003/T004:** hiện có thêm API/DTO/sự kiện/port và fake chung, 71 test M11 xanh. Kết quả mới và phần còn dở ở [api-declaration-verification.md](api-declaration-verification.md); các số 46/49 bên dưới giữ bằng chứng các lượt trước.

> **R-15 ngày 2026-10-30:** nền hiện tại 8573190; đã có T005a ba bảng riêng và fixture nguồn, 74 unit/arch + 15 IT M11 xanh. Xem [r15-storage-verification.md](r15-storage-verification.md). Các mốc 46/49/71 dưới đây là bằng chứng lượt trước.

## 1. Kết quả đã có

| Task | Trạng thái | Mã và test |
| --- | --- | --- |
| T001 | [x] kiểm khung và luật kiến trúc | Giữ package-info M11; M11ArchTest 4 ca xanh, pricing.internal.domain chỉ có lớp cần dùng. Không tạo cây gói rỗng |
| T006 | [ ] xong 3/4 hàm, chờ phần đầu vào của B | DongRounding (12 ca), ResidencyDayFee (20 ca), DayCareSessionFee (13 ca theo Q-395), còn BillingPeriod |

## 2. TDD và phạm vi kiểm

**Hiện tại trên e7b2b71:** test Q-395 viết trước; q395-unit-red.log lỗi testCompile vì chữ ký cũ yêu cầu hệ số. Sau sửa helper nội trú không nhận hệ số: 49/49 test xanh, 0 thất bại/lỗi/bỏ qua (45 tính phí + 4 kiến trúc). Nội trú tính đủ giá; bán trú giữ tỷ lệ từ nguồn, thêm ca 0/50/100%. Spotless M11 qua. Kiểm ngày Giải phóng giường vẫn chờ nguồn B/004; không coi test số tiền là nghiệm thu lệnh lập khoản.

**Clean verify sau pull trên e7b2b71:** clean verify nền đã nạp .env/backend/.env và giữ mật khẩu demo; hoàn tất 08:03: 689 unit/arch xanh (M11 49), 1426 IT còn 7 thất bại/9 lỗi, 0 bỏ qua, đúng 9 lớp M01/M03 đã có trong team-plan và lượt trước pull; không có lớp lỗi mới. Spotless check 2009 file sạch. Maven trả BUILD FAILURE do các lỗi nền này, không coi toàn kho xanh. Log q395-clean-verify.log và q395-full-summary.json tại backend/target/010-foundation-verification của checkout gốc; full-summary.json cũ giữ bằng chứng trên da9636a.

**Bằng chứng lượt trước pull trên da9636a (giữ để tra cứu):**

- Test được viết trước: unit-red.log thất bại testCompile do ba lớp hàm thuần chưa có. Sau cài, unit-green.log: 46 test, 0 thất bại/lỗi/bỏ qua. Không ghi lượt đầu là 46 test đã chạy đỏ.
- Kiểm cả làm tròn số âm/dương tại 0,5, số tiền lớn; tổng đúng giá tháng qua 12 tháng và tháng nhuận; chỉ ngày/buổi cuối nhận phần dư; làm tròn cơ sở trước hệ số; không dồn phần dư vào ngày kết thúc lưu trú giữa tháng; dùng giá hiệu lực do caller cung cấp; hệ số phần trăm và đơn giá trực tiếp.
- Helper buổi nhận số buổi có lịch đã lọc/ngày khu nghỉ và cờ buổi cuối từ caller. Test không chứng minh việc truy vấn/lọc nguồn 004 hay trạng thái có mặt 005 đã có bản thật. Không có fake production, ghi dữ liệu hoặc thời gian hệ thống trong các hàm.
- M11ArchTest đặt luật hai chiều port theo P-12; port/job thật chưa có nên chưa nghiệm thu gọi chéo hoặc job. Rule domain thuần áp lên ba lớp hiện có. Không gọi api của module khác, không sửa API đã duyệt.
- Spotless M11 đã qua. Clean verify toàn kho hoàn tất sau 09:48: 683 unit/arch xanh, gồm cả 46 test M11; 1426 IT còn 7 thất bại/9 lỗi, 0 bỏ qua, đúng 9 lớp M01/M03 đã có trong team-plan, không lớp lỗi mới. Toàn bộ Spotless check qua (2013 file sạch). Log và full-summary.json tại backend/target/010-foundation-verification của checkout gốc, ignored.

Các lớp đỏ có sẵn: `BR_M01_01_TemporaryAbsenceOverdueIT`, `BR_M01_02_ReassessmentNoticeIT`, `CompleteAdmissionConsentIT`, `CompleteAdmissionWishWarningsIT`, `FR_023h_KhongDauNangLucIT`, `FR_034a_CompleteAdmissionIT`, `ResidentContractIT`, `StatusHistoryIT`, `FR_035_LocationHistoryHttpIT`. Không sửa test/source của B/C cho khớp mã M11; không coi toàn kho xanh.

## 3. Phần phải chờ và phần E còn lại

- B/004: T002 chưa có interface/fake người đại diện; phần BillingPeriod của T006 cần kiểu hợp đồng/hồ sơ kết thúc đúng contract 004; kiểm ngày lưu trú cần ngày Giải phóng giường theo Q-395 để ngừng tính từ chính ngày đó. Đã ghi vào dòng B ở team-plan. Không dựng DTO hoặc fake thứ hai của B để vượt chỗ chờ.
- B/004, C/005, D/006: nguồn tài chính/đơn giá/ngày lưu trú, sự kiện tính phí/có mặt và producer thuốc còn như preparation-review.md mục 4; chỉ phần phụ thuộc mới chờ các nguồn này.
- E: T003/T004 đã khai báo và có fake/test xanh tại nhánh E, xem api-declaration-verification.md; còn T005 migration, T007 job, T008 provider; không đổ các task E này thành việc B/C/D phải làm. Phase 1/2 và T006 chưa đóng.
- A (nền web): `frontend/scripts/gen-api.mjs:9` chưa có 010 trong TRANSLATED. Đã chạy `npm run gen:api`, script chỉ sinh lại 8 feature cũ, không có api010.js và không có diff frontend sau giữ CRLF. Cần A thêm 010 vào danh sách đã dịch; phần cần thêm chính xác ở mục 5. Không sửa script của A hoặc viết tay client. Chưa có HTTP/client 010/web/mobile/SQL hoặc nghiệm thu đầu–cuối trong lượt này. 17 response 2xx thiếu schema của bản gốc vẫn cần cụ thể hóa trước task HTTP tương ứng.

## 4. File để đọc

- backend/src/main/java/com/eldercare/backend/billing/internal/pricing/domain/DongRounding.java
- backend/src/main/java/com/eldercare/backend/billing/internal/pricing/domain/ResidencyDayFee.java
- backend/src/main/java/com/eldercare/backend/billing/internal/pricing/domain/DayCareSessionFee.java
- backend/src/test/java/com/eldercare/backend/billing/: DongRoundingTest, ResidencyDayFeeTest, DayCareSessionFeeTest, M11ArchTest.
- tasks/plan/research P-12, glossary mục 16, biên bản duyệt preparation-review.md, team-plan và dòng mới cuối progress.

## 5. Việc A cần làm để sinh client 010

Bản dịch 010 đã được duyệt. Tại frontend/scripts/gen-api.mjs dòng 9, thêm duy nhất mã 010 vào TRANSLATED:

```javascript
const TRANSLATED = ['000', '001', '002', '003', '006', '008', '009', '010', '016']
```

Sau thay đổi của A, chạy npm run gen:api; cần có api010.js với 29 hàm và export trong index.js, các client cũ giữ nguyên. Khi có thay đổi web, chạy npm test, npm run lint, npm run build theo luật kho. Lượt hiện tại script gốc chưa có 010 nên chưa tạo client, không có thay đổi web và chưa chạy ba lệnh kiểm web. Đây là việc đang chờ chủ phần nền, không phải xin duyệt lại hợp đồng.
