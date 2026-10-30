# Quickstart: Kiểm chứng feature 015

> **Sửa 2026-10-29 (Q-397):** bỏ mẫu xoay ca, nhóm xoay ca, lập lịch từ mẫu, phủ tối thiểu, thiếu ca, cảnh báo thiếu phủ, lời mời nhận ca; 015 chỉ còn đổi ca và nghỉ đột xuất. Nghỉ "theo ngày" mở cho mọi nhân viên, người lập chọn phạm vi. Phần dưới còn nhắc các phần đã bỏ thì bỏ qua; spec là căn cứ.

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md)

Hướng dẫn dùng ở giai đoạn triển khai; giai đoạn thiết kế chưa có mã nên các lệnh chưa chạy được. Hợp đồng xem [contracts/api-015.yaml](contracts/api-015.yaml) và [contracts/module-interfaces.md](contracts/module-interfaces.md); bảng và ràng buộc xem [data-model.md](data-model.md).

## 1. Điều kiện cần

- Như quickstart của 000 → 014: JDK 25, MySQL 8.0 cài trên máy (000 R-14), Node.js LTS, Flutter stable; profile `demo` có đồng hồ giả lập.
- Dữ liệu mẫu của profile `demo`:
  - tài khoản: Quản lý viện QL; Trưởng tầng TT2 (được giao tầng 2); Điều dưỡng D1, D2, D3; Nhân viên chăm sóc C1 → C6; Bác sĩ BS1, BS2 (ca toàn viện);
  - mẫu ca "Ngày" 07:00–19:00, "Đêm" 19:00–07:00; mẫu xoay ca "2N-2Đ-2Nghỉ" (L = 6);
  - nhóm "Điều dưỡng tầng 2" (D1 vị trí 0, D2 vị trí 2, D3 vị trí 4), nhóm "Chăm sóc tầng 2" (C1 → C6);
  - CFG theo mặc định Phụ lục 25: CFG-M09-02 16 giờ, CFG-M09-06 15 ngày, CFG-M09-07 "≥ 1 Điều dưỡng / tầng / ca", CFG-M09-10 3 ngày, CFG-M09-11 24 giờ;
  - bản giả có công tắc `/api/v1/kiem-thu/gia-lap/{module}`: `CongViecCaApi` (005), `ChuyenDiNhiemVuLenhApi` (014).

## 2. Chạy

```text
backend/  ./mvnw verify
backend/  ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
frontend/ npm ci && npm test && npm run dev
mobile/  flutter test
```

Kết quả mong đợi: `verify` xanh, gồm Modulith `verify()`, ArchUnit (chỉ `staffing.xoayca`, `staffing.doica` dùng port `staffing.*.port`), test cơ sở dữ liệu (data-model mục 7) và các nhóm test ở mục 4.

## 3. Kịch bản kiểm chứng đầu–cuối (profile demo)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| Q1 | 00:00 ngày 16/10 (mốc CFG-M09-06 cho tháng 11) | TT2 nhận nhắc lập lịch tháng 11; không có lịch tự lập | US1, FR-008, SC-002 |
| Q2 | TT2 lập lịch tháng 11 từ mẫu | Lịch Nháp khớp bảng tính tay; vị trí chu kỳ nối tiếp từ tháng 10; ca đêm 30/11 thuộc tháng 11; mọi ca chưa có người phụ trách | US1, FR-004, FR-009, FR-011, SC-001 |
| Q3 | Lập lần hai cho tháng 11; lập cho tầng 3 không có nhóm | 409 `DA_CO_LICH_THANG`; 409 `KHONG_CO_NHOM_XOAY_CA` | FR-014 |
| Q4 | C3 nghỉ việc từ 10/11 trước khi lập | C3 bị bỏ qua từ ngày đó, báo cáo ghi lý do | FR-010 |
| Q5 | Thêm D3 vào nhóm thứ hai chồng khoảng tham gia | 409 `CHONG_KHOANG_THAM_GIA` | FR-003 |
| Q6 | Sửa mẫu của nhóm đã có thành viên | 409 `KHONG_SUA_MAU_NHOM_DA_CO_THANH_VIEN` | FR-002, Q-190 |
| Q7 | TT2 sửa tay lịch Nháp, chọn người phụ trách; xem trước Lập lại; Lập lại không xác nhận | Danh sách sửa sẽ mất; 409 khi thiếu xác nhận, lý do | FR-013 |
| Q8 | Ngày 18/11 không có ca Đêm do mọi Điều dưỡng nghỉ chu kỳ | Mục "thiếu ca" (18/11, Đêm); không tạo ca rỗng | FR-006a |
| Q9 | Ca có D1 (chứng chỉ hết hạn giữa ca) và C1 | Ca Thiếu phủ (Điều dưỡng 1); C1 không tính cho dòng Điều dưỡng | FR-006, Q-188, Q-221 |
| Q10 | Cấu hình dòng Nhân viên chăm sóc ≥ 2 và người có hai vai trò | Phân người cho số dòng đạt lớn nhất; tie-break theo thứ tự khai báo | FR-006 |
| Q11 | 00:00 ngày 29/10 lịch tháng 11 còn Nháp | TT2 và QL được nhắc mỗi ngày tới khi công bố | FR-015 |
| Q12 | TT2 công bố khi còn 2 ca thiếu phủ và 1 mục "thiếu ca", không xác nhận | Bị yêu cầu xác nhận đã xem kèm lý do; công bố → cảnh báo thiếu phủ mở cho 2 ca | US2, FR-017, Q-178 |
| Q13 | C2 lập đổi hai chiều với C4; chọn Y | Chỉ thấy ngày, giờ, mẫu ca của các ca hợp lệ của C4 | US3, FR-020 |
| Q14 | D1 lập nhận thay cho C2 ca X mà D1 được tính dòng Điều dưỡng | 409 `VAI_TRO_KHONG_PHU_HOP` | FR-019, Q-191 |
| Q15 | C4 đồng ý; TT2 duyệt khi C4 sẽ vượt 16 giờ liên tục | Cần xác nhận kèm lý do; áp dụng: ca, phân công chuyển, phủ tính lại; thông báo | US3, FR-021, FR-024 |
| Q16 | A là người phụ trách X, B không đạt 008 FR-015, người duyệt không chọn ai | 409 `CAN_NGUOI_PHU_TRACH`; yêu cầu giữ Chờ duyệt | FR-024 (c), Q-187 |
| Q17 | A là trưởng đoàn chuyến chồng ca X | Cảnh báo lúc lập; khi duyệt 014 nhận `goNhiemVu` | FR-021 (h), FR-024 (g), Q-185 |
| Q18 | Yêu cầu chờ tới 24 giờ trước ca; tới giờ ca | Nhắc người đang phải hành động; tới giờ → Hết hiệu lực | FR-025, FR-022 |
| Q19 | Lập yêu cầu đổi ca thứ hai cho ca đang trong yêu cầu mở | 409 `CA_DANG_TRONG_YEU_CAU_KHAC` | FR-019 |
| Q20 | Duyệt đổi ca cùng lúc với ghi vắng ca của B | Một lệnh thành công, lệnh kia thấy dữ liệu mới và bị chặn | SC-004 |
| Q21 | D2 lập nghỉ 3 ca; TT2 duyệt một phần (2 ca) có lý do | Đã áp dụng một phần; 2 ca nghỉ có duyệt, phân công hủy, 005 `khiNghiCoDuyet`; ca còn lại giữ | US4, FR-028, FR-029, Q-182 |
| Q22 | D2 là người phụ trách ca nghỉ, không còn ai đạt | Lệnh vẫn thực hiện; ca dấu "thiếu người phụ trách" | FR-029 (d), Q-187 |
| Q23 | TT2 lập thay nghỉ cho C5 (gọi điện báo ốm) | Dấu "lập thay"; C5 được thông báo | FR-026 |
| Q24 | Ca của yêu cầu nghỉ tới giờ bắt đầu khi chưa quyết định | Ca tách khỏi yêu cầu; TT2 được nhắc dùng "Ghi nhận vắng ca" | FR-030 |
| Q25 | C6 nghỉ theo ngày 20/12 (tháng 12 chưa có lịch); duyệt; TT2 lập lịch tháng 12 | C6 không được xếp ngày 20/12; báo cáo "nghỉ có duyệt" không nêu lý do | FR-026a, FR-010, Q-184 |
| Q26 | TT2 sửa tay xếp C6 vào 20/12 | Cảnh báo; xác nhận kèm lý do lưu vào yêu cầu nghỉ; C6 được báo | FR-026a |
| Q27 | Nghỉ theo ngày còn chờ khi lịch công bố | Lệnh công bố hiển thị; sau công bố yêu cầu thành dạng theo ca | FR-026a, FR-031 |
| Q28 | Nghỉ đột xuất làm ca chuyển Thiếu phủ | Cảnh báo thiếu phủ mở ≤ 1 phút, không kèm danh sách gợi ý | US5, FR-032, SC-005 |
| Q29 | TT2 tra cứu người thay vai trò Điều dưỡng | Nhân viên không có ca chồng giờ, sắp theo họ tên, kèm giờ đã làm/đã xếp; không xếp hạng | US5, FR-033, FR-034 |
| Q30 | Mời người có ca chồng giờ; mời D3 | Chặn kèm lý do; D3 nhận lời mời Trung bình chỉ trong ứng dụng | FR-033, Q-186 |
| Q31 | D3 nhận lời mời | Bổ sung ngay vào ca; nhật ký hai người; cảnh báo đóng đủ phủ; lời mời khác cùng vai trò hết hiệu lực | FR-038, FR-039 |
| Q32 | TT2 hết giao trưởng tầng trước khi D3 nhận | Nhận bị từ chối; lời mời Hết hiệu lực | FR-038 |
| Q33 | Ca bắt đầu khi cảnh báo còn mở; ca kết thúc | Nhắc TT2, người phụ trách ca, QL; cảnh báo đóng "hết ca" | FR-035, FR-036 |
| Q34 | BS1 (ca toàn viện) lập đổi ca với BS2; TT2 thử duyệt | Chỉ QL duyệt; dấu Bác sĩ trực chuyển sang BS2 | FR-023, FR-024 (f), Q-177 |
| Q35 | Thông báo qua tin nhắn của yêu cầu nghỉ | Không có lý do nghỉ | FR-045, SC-008 |
| Q36 | C1 xem kết quả tra cứu người thay; xem lý do đổi ca của người khác | 403; trường bị ẩn | FR-045 |
| Q37 | Lập yêu cầu khi mất mạng | Không cho lưu tạm | FR-043 |
| Q38 | Đổi CFG-M09-06 sau khi mốc đã qua | Không phát bù nhắc | FR-008 |

## 4. Nhóm test tự động

| Nhóm | Test | Kịch bản |
| --- | --- | --- |
| Hàm thuần | `MauCaTaiNgayTest`, `LapLichThangTest` (SC-001: 3 mẫu × 4 tháng), `DongApDungTest`, `TinhPhuTest`, `KiemTraDoiCaTest`, `DieuKienNguoiThayTest`, `GioTrongThangTest` | Q2, Q8 → Q10, Q15, Q29 |
| Lập lịch | `BR_M09_09_LapLichTuMauIT`, `FR_013_LapLaiIT`, `FR_015_NhacCongBoIT` | Q1 → Q12 |
| Đổi ca | `BR_M09_10_DoiCaIT` | Q13 → Q20, Q34 |
| Nghỉ | `FR_029_NghiTheoCaIT`, `FR_026a_NghiTheoNgayIT` | Q21 → Q27 |
| Thiếu phủ | `BR_M09_11_ThieuPhuIT`, `FR_038_LoiMoiIT` | Q28 → Q33 |
| Riêng tư | `FR_045_RiengTuIT` | Q35, Q36 |
| Bộ 200 yêu cầu | `SC_004_BoYeuCauIT` | SC-004 |
| Đồng thời | Hai lệnh lập lịch; duyệt đổi ca và vắng ca; hai người nhận lời mời chỗ cuối | Q20, Q31 |
| Kiến trúc | `ApplicationModules.verify()`; ArchUnit port nội bộ M09; gói `..lich..` không lập lịch, không chỉ định người phụ trách, không gửi lời mời | |
| Web (Vitest) | Màn tra cứu người thay sắp theo họ tên, không cột xếp hạng; màn công bố hiển thị hai danh sách | Q12, Q29 |
| App (`flutter_test`) | Lập đổi ca, nghỉ ≤ 2 phút (SC-006); trả lời lời mời; không lưu lý do người khác | Q13, Q21, Q37 |

## 5. Tiêu chí hoàn tất (đối chiếu SC)

| SC | Cách đo |
| --- | --- |
| SC-001 | `LapLichThangTest` so bảng tính tay |
| SC-002 | Q1, Q3; đo lệnh lập lịch một phạm vi ≤ 30 giây với 40 nhân viên |
| SC-003 | Đo thời gian thực tế của 3 trưởng tầng |
| SC-004 | `SC_004_BoYeuCauIT` |
| SC-005 | Q28 → Q30 và đối chiếu số giờ tính tay |
| SC-006 | Đo thao tác trên app |
| SC-007 | Số liệu vận hành từ `XoayCaThongKeApi` |
| SC-008 | Q35, quét nhật ký có căn cứ quyền; đồng hồ giả lập cho mọi quy tắc thời gian |
