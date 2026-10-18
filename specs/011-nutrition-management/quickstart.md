# Quickstart: Kiểm chứng feature 011

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md)

Hướng dẫn dùng ở giai đoạn triển khai; giai đoạn thiết kế chưa có mã nên các lệnh chưa chạy được. Hợp đồng xem [contracts/api-011.yaml](contracts/api-011.yaml) và [contracts/module-interfaces.md](contracts/module-interfaces.md); bảng và ràng buộc xem [data-model.md](data-model.md).

## 1. Điều kiện cần

- Như quickstart của 000 → 010: JDK 25, MySQL 8.0 cài trên máy (000 R-14), Node.js LTS, Flutter stable; profile `demo` có đồng hồ giả lập.
- Dữ liệu mẫu của profile `demo`:
  - tài khoản: Bác sĩ BS; Nhân viên bếp B1, B2; Trưởng tầng TT (tầng 2); Điều dưỡng DD, Nhân viên chăm sóc CS (tầng 2, ca sáng); Quản lý viện QL; người thân N của A (có tài khoản, có quyền xem sức khỏe);
  - chế độ ăn: Thường (mặc định, món an toàn "Cháo trắng"), Tiểu đường (liên quan điều trị, hạn chế "đường"), Mềm; dị nguyên thực phẩm "tôm", "đậu phộng", "đường";
  - người cao tuổi: A nội trú tầng 2, dị ứng "tôm"; C nội trú tầng 3, đang vắng nằm viện tới 11:00; D nội trú chưa có bản gán; E bán trú lịch thứ 2, 4, 6; F nội trú có dị ứng loại "khác";
  - bữa: sáng 06:30, trưa 11:00, tối 17:00; CFG theo mặc định Phụ lục 25 (CFG-M08-01 2 giờ, CFG-M08-03 3 ngày, CFG-M08-04 30 phút, CFG-M08-05 30 phút, CFG-M08-06 24 giờ, CFG-M08-07 2 ngày; CFG-M13-06 giờ hành chính bác sĩ);
  - bản giả có công tắc `/api/v1/kiem-thu/gia-lap/{module}`: `LuotOLaiAnPort` và `LuotOLaiDaDoi` (012), `ChuyenDiBuaAnPort` và `ChuyenDiDaDoi` (014), `TonKhoNguyenLieuPort` (019); bộ phát sự kiện thử cho 001, 003, 004, 005, 007.

## 2. Chạy

```text
backend/  ./mvnw verify
backend/  ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
frontend/ npm ci && npm test && npm run dev
mobile/  flutter test
```

Kết quả mong đợi: `verify` xanh, gồm Modulith `verify()` (không có vòng 011 ↔ 005, 011 ↔ 007, 011 ↔ 012, 011 ↔ 014), test cơ sở dữ liệu (data-model mục 12) và các nhóm test ở mục 4.

## 3. Kịch bản kiểm chứng đầu–cuối (profile demo)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| Q1 | BS lập bản gán A: Thường → Tiểu đường, không lý do; áp dụng | 409 `CAN_LY_DO_CHUYEN_MON`; thêm lý do → Hiệu lực, bản cũ Ngừng cùng lệnh | US1, FR-013 |
| Q2 | BS lập bản gán A với hiệu lực ngày mai 00:00 | Chờ hiệu lực; tới mốc job chuyển Hiệu lực, bản cũ Ngừng | US1, FR-017 |
| Q3 | Lập bản gán thứ hai khi A đã có bản Đề xuất | 409 (FR-012) | US1 |
| Q4 | D Hoàn tất tiếp nhận không có bản gán | Suất D dùng chế độ mặc định, dấu `CHUA_GAN_CHE_DO_AN`; BS được báo | US1, FR-014 |
| Q5 | A Kết thúc lưu trú khi có bản Hiệu lực và bản Chờ hiệu lực | Ngừng và Đã hủy; phát sinh "−1" cho bữa đã chốt chưa tới giờ; không thông báo | FR-015 |
| Q6 | B1 tạo món "Bún tôm" Nháp, gửi xác nhận; BS trả lại có lý do; gửi lại; BS xác nhận | Nháp → Chờ xác nhận → Nháp → Hiệu lực; B1 nhận thông báo | US2, FR-004a |
| Q7 | B1 gửi bản sửa của món Hiệu lực; trước khi BS xác nhận, chốt suất | Chốt dùng nội dung đã xác nhận; xác nhận bản sửa chạy lại kiểm tra thực đơn chưa hết tuần | US2 |
| Q8 | Xếp món có "đường" vào chế độ Tiểu đường | 409 `MON_KHONG_PHU_HOP` (FR-005) | US2 |
| Q9 | Ngừng món là món an toàn của chế độ Thường | 409 `LA_MON_AN_TOAN` | Bảng trạng thái món |
| Q10 | Ghi dị ứng "đậu phộng" mới cho A (001) khi thực đơn tuần có món đậu phộng không có món thay thế | 007 nhận `YeuCauTaoCanhBao` mức Trung bình; BS được báo; bữa đã chốt chưa phục vụ có phát sinh; `daKiemTra(AN)` được gọi | US2, FR-021 |
| Q11 | B1 tạo thực đơn tuần sau, thiếu món bữa tối Tiểu đường thứ 4; gửi kiểm tra | 409 `THIEU_DO_PHU` liệt kê (Tiểu đường, thứ 4, tối) cùng mọi cảnh báo trong một lần | US3, SC-009 |
| Q12 | Gửi kiểm tra không tích xác nhận kiểm tra chế biến | 409 (FR-025a); màn trước khi gửi hiện tồn kho (hoặc "chưa có dữ liệu kho") và số người theo chế độ ăn, không tên | US3 |
| Q13 | Món giống nhau 3 ngày liên tiếp, trong đó 1 ngày thuộc tuần trước đã công bố | Cảnh báo lặp món qua ranh giới tuần; món thay thế không tính | US3, FR-026 |
| Q14 | Món có tôm trong chế độ Thường (A ăn Thường, không có món thay thế phù hợp) | Bếp thấy "1 người"; BS thấy tên A | US3, SC-014 |
| Q15 | B1 gọi công bố | 403 | US3, Q-308 |
| Q16 | BS công bố sau khi dữ liệu đổi làm thiếu độ phủ | Chặn; dữ liệu đúng → Công bố; tuần đã bắt đầu → Đã áp dụng | US3, FR-029 |
| Q17 | Tới 00:00 thứ 2 | Công bố → Đã áp dụng | FR-030 |
| Q18 | 2 ngày trước tuần mới chưa có thực đơn Công bố | Nhắc B1, B2 (và BS) | BR-M08-16 |
| Q19 | B1 đổi món bữa trưa hôm nay sau khi chốt | `doi_mon` ghi trước/sau; BS nhận thông báo Trung bình; phát sinh cho các tầng | US3, FR-031 |
| Q20 | B1 mở tính sẵn bữa trưa lúc 08:00 (trước mốc 09:00) và chốt | Xem được; chốt 409 `CHUA_TOI_MOC_CHOT` | US4, FR-033 |
| Q21 | Chốt bữa trưa lúc 09:00 | Lần chốt; C (về 11:00) được tính dấu `DU_KIEN_TRO_VE`; E tính khi thứ 2/4/6 và chưa báo vắng; phiếu mỗi tầng lập cùng lệnh | US4, FR-034, FR-044 |
| Q22 | B1 và B2 chốt cùng lúc | Một thành công, một 409 `DA_CHOT` | US4 |
| Q23 | Bản giả 012 có N ở lại đăng ký ăn | Suất người thân tầng 2; 010 nhận `SuKienTinhPhiNguon(SUAT_AN_NGUOI_THAN)`; N kết thúc sớm trước giờ bữa → phát sinh "−1", `SuKienTinhPhiNguonDaDoi(daHuy)` | US4, FR-043, SC-011 |
| Q24 | Bản giả 014 có chuyến trùng giờ trưa cho A | A không được tính | US4, FR-034 |
| Q25 | Sau chốt, A chuyển giường từ tầng 2 sang 3 | Phát sinh "−1" tầng 2, "+1" tầng 3 ≤ 5 phút; tầng 3 chưa có phiếu → phiếu lập khi B1 xác nhận phát sinh | US4, FR-039, SC-002 |
| Q26 | Phát sinh chưa xác nhận tới 10:30 | Nhắc bếp và BS mức Trung bình | BR-M08-13, FR-041 |
| Q27 | Sau 11:00, A về muộn; TT yêu cầu suất bổ sung lúc 11:20; lúc 11:40 | Phần bổ sung gửi bếp; 11:40 bị chặn `QUA_NGUONG_GIAO_TRE` | FR-040 |
| Q28 | C chưa về lúc 11:00 | Phát sinh "−1" không cần giao, suất giữ; C về 11:15 → phục vụ suất giữ tạo "+1" không cần giao | FR-040a |
| Q29 | Chốt bữa sáng 04:30 (ngoài giờ hành chính) có suất `THIEU_MON_THAY_THE` | Món an toàn áp ngay trong lệnh, BS nhận thông báo Nhẹ | FR-037a, SC-012 |
| Q30 | Bữa trưa có suất thiếu món thay thế, BS không xử lý tới 10:30 | Job áp món an toàn, phát sinh đổi suất; nếu món an toàn xung đột → điều dưỡng phụ trách nhận Trung bình | FR-037a, SC-012 |
| Q31 | Chưa có thực đơn tuần; chốt | Phiếu dấu `CHUA_CO_THUC_DON`; thông báo theo FR-065 | FR-038 |
| Q32 | B1 giao phiếu khi còn suất đặc biệt chưa chuẩn bị; chưa có lưu mẫu | 409 `CHUA_CHUAN_BI`, `CHUA_LUU_MAU` | US5, FR-047, BR-M08-15 |
| Q33 | Giao; CS tầng 2 nhận; người không có phân công tầng 2 nhận | Đã nhận; người ngoài 403 `NGOAI_PHAM_VI_NHAN` | US5, FR-048 |
| Q34 | TT báo sai lệch; B1 xử lý bổ sung suất | Có sai lệch → Đã giao; người báo nhận thông báo | US5 |
| Q35 | Phát sinh "+" sau khi phiếu Đã nhận | Phần bổ sung giao, nhận riêng; phiếu giữ Đã nhận | FR-048b |
| Q36 | 11:30 phiếu tầng 3 chưa giao | TT tầng 3 và bếp nhận cảnh báo nhẹ một lần | BR-M08-12 |
| Q37 | B1 xem phiếu, nhãn, thông báo | Không có dị ứng, bệnh lý, thành phần xung đột, dấu "cần đối chiếu" | SC-008 |
| Q38 | CS xác nhận suất của A cho người khác không xung đột | 409 `SUAT_KHONG_DUNG_NGUOI`, không sự cố | US6, FR-049 |
| Q39 | CS phục vụ suất có tôm cho A | 409 `SUAT_CO_THANH_PHAN_DI_UNG`; cảnh báo 007 mức Trung bình đã có dù lệnh bị chặn; gửi lại `daAn = true` kèm sự cố → sự cố 007 trỏ xác nhận; `daAn = false` → không sự cố | US6, SC-005 |
| Q40 | 005 ghi kết quả ăn của A khi chưa xác nhận phục vụ; F (dị ứng loại khác) | Bị chặn (`xacNhanPhucVu = false`); F cũng phải xác nhận | FR-050, FR-036a |
| Q41 | CS xác nhận ngoại tuyến, đồng bộ sau | Kiểm tra lại khi đồng bộ; thời điểm thiết bị lưu riêng | FR-051, Q-01 |
| Q42 | DD ghi đồ ăn có tôm cho A; đồ ăn thành phần "không rõ"; đồ uống có cồn | Không sử dụng (N nhận thông báo Nhẹ); Cần xác nhận; Không sử dụng | US7, BR-M08-04, SC-007 |
| Q43 | Đồ ăn Được sử dụng, A có dị ứng mới trùng; tới hạn dùng | Không sử dụng, điều dưỡng phụ trách được báo | FR-055, FR-056 |
| Q44 | Gửi lệnh cho dùng đồ ăn Không sử dụng | 409 `KHONG_CHO_DUNG_LAI` | Bảng trạng thái |
| Q45 | 005 phát `AnKemKeoDai` cho A; 007 phát `SutCanPhatHien` | BS nhận mỗi sự kiện một thông báo Nhẹ; không có yêu cầu, nhắc | US8, SC-010 |
| Q46 | BS lập bản gán chọn căn cứ thông báo xem lại | Bản gán lưu `thong_bao_xem_lai_id` | FR-059 |
| Q47 | B1 ghi lưu mẫu bữa trưa; phát sinh thêm món thay thế; quá 24 giờ chưa hủy mẫu | Danh sách món gồm món thay thế, món an toàn; ghi thêm món mới; nhắc B1 | US9, SC-013 |
| Q48 | Hành chính gọi API 011; TT xem bản gán | 403 | FR-063 |
| Q49 | Khởi động lại sau khi tắt qua mốc nhắc chốt, giờ bữa | Job chạy bù đúng một lần (cột "đã làm lúc") | NFR-04 |

## 4. Nhóm test tự động

| Nhóm | Test | Kịch bản |
| --- | --- | --- |
| Hàm thuần | `CanLyDoChuyenMonTest`, `XungDotTest`, `ChonMonThayTheTest`, `KiemTraDoPhuTest`, `LapMonTest`, `NguoiDuocTinhTest`, `TinhPhatSinhTest`, `DoiChieuDoAnTest`, `MonCanLuuMauTest` | Q1, Q8, Q11 → Q14, Q21, Q25, Q42 |
| Bản gán | `BR_M08_03_BanGanIT`, `FR_014_CheDoMacDinhIT`, `FR_015_TrangThaiCuoiIT` | Q1 → Q5 |
| Món, thực đơn | `FR_004a_XacNhanMonIT`, `BR_M08_06_DoPhuIT`, `BR_M08_07_LapMonIT`, `BR_M08_08_DoiMonIT`, `BR_M08_16_NhacThucDonIT` | Q6 → Q19 |
| Chốt, phát sinh | `BR_M08_01_ChotSuatIT`, `BR_M08_13_PhatSinhIT`, `FR_040a_SuatGiuIT`, `FR_037a_MonAnToanIT`, `FR_043_SuatNguoiThanIT`, `DBR_27_TongPhieuIT` | Q20 → Q31 |
| Phiếu | `BR_M08_10_GiaoPhieuIT`, `BR_M08_11_NhanPhieuIT`, `BR_M08_12_GiaoTreIT`, `FR_048b_PhanBoSungIT`, `SC_008_DtoBepTest` | Q32 → Q37 |
| Phục vụ | `BR_M08_14_XacNhanPhucVuIT`, `FR_050_ChanKetQuaAnIT` | Q38 → Q41 |
| Đồ ăn gia đình, xem lại, lưu mẫu | `BR_M08_04_DoAnGiaDinhIT`, `BR_M08_05_XemLaiIT`, `BR_M08_15_LuuMauIT` | Q42 → Q47 |
| Đồng thời | Hai lệnh chốt; hai BS áp dụng bản gán; xác nhận phát sinh và giao cùng lúc | Q22 |
| Bộ lập lịch | `ChayBuM08IT` | Q49 |
| Kiến trúc | `ApplicationModules.verify()`; ArchUnit: gói `..lich..` không gọi lệnh chốt, không lập phiếu (Q-306); DTO bếp không tham chiếu kiểu sức khỏe | |
| Web (Vitest) | Màn tính sẵn chỉ xem, nút chốt tách riêng; màn kiểm tra thực đơn hiện mọi lỗi một lần; bếp không thấy tên trong cảnh báo dị ứng | Q11, Q14, Q20 |
| App (`flutter_test`) | Xác nhận phục vụ chọn suất theo nhãn; luồng khai đã ăn/chưa ăn; ghi ngoại tuyến | Q38 → Q41 |

## 5. Tiêu chí hoàn tất (đối chiếu SC)

| SC | Cách đo |
| --- | --- |
| SC-001 | Bộ 7 ngày × 60 người dựng sẵn: số suất từng bữa khớp tính tay |
| SC-002 | Q25 và bộ SC-001: mọi thay đổi sau chốt thành phát sinh ≤ 5 phút; `DBR_27_TongPhieuIT` xanh |
| SC-003 | Q11, Q15, Q16 |
| SC-004 | Q1 → Q5 và kiểm bất biến "mỗi người Đang lưu trú có chế độ ăn" sau mọi kịch bản |
| SC-005 | Q38 → Q40 |
| SC-006 | Đo `POST /bua-ngay/{id}/chot-suat` với 300 người ≤ 1 phút |
| SC-007 | Q42 → Q44 |
| SC-008 | Q37, `SC_008_DtoBepTest` quét mọi DTO, nhãn, thông báo tới bếp |
| SC-009 | Q11, đợt thử thực đơn 6 chế độ ăn |
| SC-010 | Q45 |
| SC-011 | Q23 |
| SC-012 | Q29, Q30 |
| SC-013 | Q47 |
| SC-014 | Q12, Q14 |

## 6. Kịch bản bổ sung 2026-10-18, lượt 3 (Q-366)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| A1 | A có chế độ ăn mềm, tuần có thực đơn Công bố; 012 gọi `SuatAnDuKienApi.cuaNguoiCaoTuoi(A, thứ 2, chủ nhật)` | Món của chế độ ăn mềm, có món thay thế; không có tên chế độ ăn, thành phần dị ứng | FR-063 |
| A2 | Tuần sau chưa có thực đơn công bố | Danh sách rỗng | FR-063 |

Test: `FR_063_SuatAnDuKienNguoiCaoTuoiIT`.

