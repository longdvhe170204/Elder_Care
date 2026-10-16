# Quickstart: Kiểm chứng feature 014

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md)

Hướng dẫn dùng ở giai đoạn triển khai; giai đoạn thiết kế chưa có mã nên các lệnh chưa chạy được. Hợp đồng xem [contracts/api-014.yaml](contracts/api-014.yaml) và [contracts/module-interfaces.md](contracts/module-interfaces.md); bảng và ràng buộc xem [data-model.md](data-model.md).

## 1. Điều kiện cần

- Như quickstart của 000 → 013: JDK 25, MySQL 8.0 cài trên máy (000 R-14), Node.js LTS, Flutter stable; profile `demo` có đồng hồ giả lập.
- Dữ liệu mẫu của profile `demo`:
  - tài khoản: Trưởng tầng TT2 (tầng 2), TT3; Nhân viên chăm sóc CS1, CS2; Điều dưỡng DD; Hành chính HC; Quản lý viện QL; Nhân viên bếp B (người đi cùng); người đại diện R của L;
  - người cao tuổi: A, B, C nội trú tầng 2; D nội trú tầng 3; L bán trú 07:30–16:30 thứ 2 → 6;
  - hoạt động "Thể dục sáng" tầng 2, mẫu thứ 2, 4, 6 07:00–07:45, tối đa 10, người thường xuyên A, B; "Hát karaoke" có thu phí (dịch vụ "karaoke"); chuyến "Tham quan chùa" toàn viện, xe của viện;
  - CFG theo mặc định Phụ lục 25: CFG-M04-06 2 giờ, CFG-M04-07 30 phút, CFG-M04-10 7 ngày, CFG-M04-11 5%, CFG-M04-13 1 giờ, CFG-M09-04 30 phút;
  - bản giả có công tắc `/api/v1/kiem-thu/gia-lap/{module}`: `LichXeApi`, `LichXeLenhApi` (019); `MangThuocChuyenDiApi` (006); `SuCoLenhApi` (007); sự kiện `KhoanhVungDaTao`, `DauNghiNhiemDaDoi` (007), `CaDaKetThuc` (008).

## 2. Chạy

```text
backend/  ./mvnw verify
backend/  ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
frontend/ npm ci && npm test && npm run dev
mobile/   flutter test
mobile/  flutter test
```

Kết quả mong đợi: `verify` xanh, gồm Modulith `verify()` (M04 không phụ thuộc 006, 007, 008, 010, 011, 012, 015, 016, 019), ArchUnit cho gói nội bộ M04, test cơ sở dữ liệu (data-model mục 9) và các nhóm test ở mục 4.

## 3. Kịch bản kiểm chứng đầu–cuối (profile demo)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| Q1 | TT2 tạo buổi từ mẫu cho 4 tuần; chạy lại cho 2 tuần đầu | 12 buổi, A, B được đăng ký sẵn; lần hai không tạo trùng | US1, FR-006, FR-007, SC-001 |
| Q2 | Không ai tạo buổi; qua 4 tuần | Không có buổi tự sinh | Q-306 |
| Q3 | B mang dấu nghi nhiễm lúc tạo buổi | B không được đăng ký sẵn; TT2 nhận danh sách kèm lý do | US1, FR-008 |
| Q4 | Đổi mẫu sang thứ 3, 5 từ tuần sau | Buổi thứ 2, 4, 6 chưa diễn ra bị hủy "đổi mẫu"; TT2 nhận nhắc tạo lại | US1, FR-009 |
| Q5 | Hủy buổi thứ 4 thủ công rồi tạo lại từ mẫu | Không tạo lại buổi đã hủy | FR-007 |
| Q6 | Đăng ký người thứ 11 | 409 `DU_SO_LUONG` | US2, SC-002 |
| Q7 | Đăng ký A vào hai buổi chồng giờ | 409 `CHONG_THOI_GIAN` | FR-016 |
| Q8 | Đăng ký L vào chuyến 13:00–18:00 thứ 6 | 409 `CAN_DONG_Y_VE_MUON`; R ghi đồng ý qua cổng → đăng ký được, giờ về L thành 18:00 (005 `doiGioVe`) | US2, FR-016a, SC-013 |
| Q9 | Gia hạn chuyến tới 18:30; R rút đồng ý trước giờ bắt đầu | Giờ về L 18:30; rút → hủy đăng ký, giờ về trở lại 16:30 | FR-016a |
| Q10 | Đăng ký C (có lưu ý chăm sóc) không xác nhận cảnh báo | 409 `CAN_XAC_NHAN_CANH_BAO`; xác nhận → lưu dấu | US2, FR-018 |
| Q11 | Khoanh vùng tầng 2 | Buổi tại tầng 2 chưa điểm danh và đăng ký của A, B, C bị hủy "khoanh vùng"; gỡ vùng không khôi phục | US2, FR-021 |
| Q12 | B được gắn nghi nhiễm khi Được đi chuyến chưa rời viện | B Không đi; đăng ký hoạt động nhóm bị hủy; trưởng đoàn, TT2 được báo | FR-021a |
| Q13 | CS1 điểm danh buổi karaoke: A Có mặt "bỏ giữa chừng", B Vắng từ chối; hoàn tất | 010 nhận một `SuKienTinhPhiNguon(DIEM_DANH_HOAT_DONG)` cho A; không có khoản tự tạo | US3, FR-032, SC-003 |
| Q14 | Hoàn tất khi còn đăng ký chưa có kết quả | 409 `CON_DANG_KY_CHUA_CO_KET_QUA` | FR-029 |
| Q15 | CS1 điểm danh ngoại tuyến, đồng bộ 3 giờ sau buổi | Nhãn "ghi nhận muộn"; thời điểm thiết bị lưu | FR-030, Q-01 |
| Q16 | Buổi không ai hoàn tất tới 24:00 (đã có 1 lượt) | Đã điểm danh dấu "không đủ"; còn lại Không ghi nhận; báo Trung bình | FR-014a, SC-015 |
| Q17 | Đính chính lượt Có mặt của A thành Vắng | 010 nhận `SuKienTinhPhiNguonDaDoi(daHuy)` | FR-031 |
| Q18 | Chuyến: điểm danh rời viện khi chưa có trưởng đoàn | 409 `CHUA_CO_TRUONG_DOAN` | US4, SC-004 |
| Q19 | Phân công CS2 làm trưởng đoàn khi CS2 không có ca chồng chuyến; B làm trưởng đoàn | 409 `KHONG_CO_TEN_TRONG_CA`; 409 `KHONG_DU_VAI_TRO` | FR-035 |
| Q20 | CS1 (trưởng đoàn) đánh giá khả năng | 403; TT2 đánh giá A, TT3 đánh giá D | US4, FR-036 |
| Q21 | Rời viện A khi còn liều trong khoảng đi chưa giao | 409 `CHUA_XU_LY_THUOC` | US4, FR-038 |
| Q22 | Rời viện khi xe của viện chưa có lịch Đã đặt | 409 `CHUA_CO_LICH_XE` | FR-040, Q-220 |
| Q23 | Rời viện A, D | Chuyến Đang đi; 001 Hoạt động bên ngoài; 005 hủy công việc trong khoảng; 006 Mang theo; 011 nhận `ChuyenDiDaDoi`; 019 nhận `LichXeLenhApi.theoChuyen(ROI_VIEN_DAU)` trong cùng lệnh | US4, FR-041, SC-005 |
| Q24 | Điểm danh rời viện ngoại tuyến rồi đồng bộ | Áp dụng theo thời điểm thiết bị | FR-046a |
| Q25 | Quá giờ về + 30 phút; + 60 phút chưa gia hạn | Trưởng đoàn, TT nhận Trung bình; QL nhận Trung bình; mỗi mốc một lần | US5, FR-049, FR-049a |
| Q26 | Gia hạn giờ về vượt ca của người đi cùng | TT nhận Trung bình; phân công không bị hủy | FR-035 |
| Q27 | B (người đi cùng, Nhân viên bếp) báo thiếu D | Sự cố Khẩn cấp 007 trong cùng lệnh; D Thiếu khi về; lệnh thứ hai 409 `DA_CO_SU_CO_THIEU_NGUOI` | US5, FR-046, FR-048, SC-006 |
| Q28 | Báo thiếu người khi thiết bị mất mạng | Không lưu tạm; app hướng dẫn gọi trưởng tầng | FR-046a |
| Q29 | Điểm danh về A; kết thúc điểm danh về khi C (đã rời) chưa về | Chuyến Đã về; C Thiếu khi về; 007 nhận cảnh báo Khẩn cấp ≤ 1 phút; không có sự cố tự tạo | US5, FR-047, SC-006 |
| Q30 | TT2 ghi sự cố thiếu người cho C; C về muộn | Sự cố có; điểm danh về muộn; 007 nhận diễn biến "đã trở về"; sự cố không tự đóng | FR-047 |
| Q31 | D chuyển viện khi đang Thiếu khi về | D Rời đoàn, không tính thiếu | FR-045 |
| Q32 | Hủy ghi nhận bản ghi rời viện cuối cùng | Chuyến về Đã lên lịch; 001 Đang lưu trú; 010 hủy lượt; 019 nhận `theoChuyen(HUY_ROI_VIEN_CUOI)` | FR-031a |
| Q33 | 015 gỡ nhiệm vụ trưởng đoàn khi đổi ca | Trưởng đoàn bị gỡ, TT được báo; rời viện bị chặn tới khi phân công lại | FR-035 |
| Q34 | Ghi sở thích "không thích âm nhạc" cho A khi đã có "thích" | 409 `CAN_XAC_NHAN_THAY_SO_THICH`; xác nhận → mục cũ Đã thay | US6, FR-051 |
| Q35 | A 7 ngày tính liên tiếp không Có mặt hoạt động nhóm (có 1 ngày vắng xen giữa) | Ngày vắng bỏ qua; ngày thứ 7 → 007 cảnh báo Nhẹ "nguy cơ cô lập", không kèm gợi ý | US6, FR-054, FR-055, SC-007 |
| Q36 | A Có mặt hoạt động nhóm | Chuỗi đếm lại; 007 nhận diễn biến; cảnh báo không tự đóng | FR-056 |
| Q37 | Mốc CFG-M09-04 trước kết ca, ca có 40 công việc đủ điều kiện, chưa có danh sách | TT2 nhận nhắc | US7, FR-059a |
| Q38 | TT2 lập danh sách 1 mục khi cần 2; lập 2 mục; lập danh sách thứ hai cho ca | 409 `CHUA_DU_TY_LE`; được; 409 `DA_CO_DANH_SACH` | US7, FR-059, SC-008 |
| Q39 | Chọn công việc do chính TT2 thực hiện; liều thuốc | 409 `CONG_VIEC_KHONG_DU_DIEU_KIEN` | FR-060, SC-014 |
| Q40 | Ghi Không đạt không có công việc làm lại; có | 409 `THIEU_CONG_VIEC_LAM_LAI`; được, 005 có công việc làm lại trỏ kết quả; người thực hiện gốc nhận Trung bình | US7, FR-064 |
| Q41 | Ca đêm không có TT2: Người phụ trách ca ghi kết quả cho mục mình thực hiện | 409 `TU_KIEM_TRA_CONG_VIEC_CUA_MINH` | FR-066 |
| Q42 | Ca kết thúc còn mục chưa kiểm tra; ca có công việc đủ điều kiện mà không có danh sách | Mục Quá hạn; QL nhận Nhẹ mỗi danh sách; ca ghi "không có danh sách" | FR-063 |
| Q43 | CS1 xem danh sách kiểm tra | 403 | FR-060 |
| Q44 | R mở cổng phần hoạt động của L | Buổi, chuyến, số buổi; không có mức tham gia, cảnh báo, kết quả kiểm tra | FR-068, SC-010 |
| Q45 | Khởi động lại sau khi tắt qua nửa đêm và mốc quá giờ | Job chạy bù đúng một lần | NFR-04 |

## 4. Nhóm test tự động

| Nhóm | Test | Kịch bản |
| --- | --- | --- |
| Hàm thuần | `BuoiTheoMauTest`, `DieuKienDangKyTest`, `NgayTinhTest`, `CapNhatChuoiTest`, `CongViecDuDieuKienTest`, `SoMucToiThieuTest`, `NguoiKiemTraTest`, `TyLeThamGiaTest` | Q1, Q6 → Q10, Q35, Q38 → Q41 |
| Buổi, đăng ký | `BR_M04_21_TaoBuoiTuMauIT`, `BR_M04_15_DangKyIT`, `FR_016a_DongYVeMuonIT`, `FR_021_KhoanhVungIT` | Q1 → Q12 |
| Điểm danh | `BR_M04_18_SuKienTinhPhiIT`, `FR_014a_HetNgayIT`, `FR_031_DinhChinhIT` | Q13 → Q17 |
| Chuyến đi | `BR_M04_16_RoiVienIT`, `BR_M04_17_ThieuNguoiIT`, `FR_031a_DinhChinhChuyenIT`, `FR_035_NhiemVuIT` | Q18 → Q33 |
| Tinh thần | `BR_M04_22_CoLapIT` (30 kịch bản, SC-007), `FR_051_SoThichIT` | Q34 → Q36 |
| Kiểm tra chất lượng | `BR_M04_23_KiemTraChatLuongIT`, `FR_066_NguoiKiemTraIT` | Q37 → Q43 |
| Cổng | `FR_068_CongHoatDongIT` | Q44 |
| Đồng thời | Hai đăng ký chỗ cuối; rời viện từ hai thiết bị; hai lệnh Báo thiếu người | Q6, Q27 |
| Bộ lập lịch | `ChayBuM04HoatDongIT` | Q45 |
| Kiến trúc | `ApplicationModules.verify()`; ArchUnit: gói `..lich..` không tạo buổi, danh sách, mục, công việc làm lại, sự cố (Q-306, Q-307) | |
| Web (Vitest) | Màn kiểm tra chất lượng không xếp hạng công việc; màn đăng ký hiển thị cảnh báo cần xác nhận | Q10, Q38 |
| App (`flutter_test`) | Điểm danh 20 người ≤ thời gian SC-009; Báo thiếu người chặn khi mất mạng | Q15, Q28 |

## 5. Tiêu chí hoàn tất (đối chiếu SC)

| SC | Cách đo |
| --- | --- |
| SC-001 | Bộ 4 tuần × 10 hoạt động (Q1 → Q5) |
| SC-002 | Q6, Q11, Q12 |
| SC-003 | Q13, Q17: mỗi lượt Có mặt có thu phí có đúng một bản ghi nguồn ở 010 (plan, điểm báo lại 1) |
| SC-004 | Q18 → Q22 |
| SC-005 | Q23, Q29 |
| SC-006 | Q27 → Q30 |
| SC-007 | `BR_M04_22_CoLapIT` 30 kịch bản |
| SC-008 | Q38 → Q40 |
| SC-009 | Đo thao tác điểm danh trên app |
| SC-010 | Q44 |
| SC-013 | Q8, Q9 |
| SC-014 | Q39 và báo cáo chất lượng không hiển thị tỷ lệ của trưởng tầng |
| SC-015 | Q16 |
