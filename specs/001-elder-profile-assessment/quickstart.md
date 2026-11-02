# Quickstart: Kiểm chứng feature 001

**Ngày**: 2026-10-07 | **Plan**: [plan.md](plan.md)

Hướng dẫn dùng ở giai đoạn triển khai; giai đoạn thiết kế chưa có mã nên các lệnh chưa chạy được. Hợp đồng xem [contracts/api-001.yaml](contracts/api-001.yaml) và [contracts/module-interfaces.md](contracts/module-interfaces.md); bảng và ràng buộc xem [data-model.md](data-model.md).

## 1. Điều kiện cần

- Như quickstart của 000 và 002: JDK 25, MySQL 8.0 cài trên máy (000 R-14), Node.js LTS, Flutter stable; profile `demo` có đồng hồ giả lập.
- Dữ liệu mẫu của profile `demo`:
  - các tài khoản của quickstart 002 (một tài khoản cho mỗi vai trò, tầng T1, T2, ca ngày T1 có Người phụ trách ca N, Trưởng tầng H, bác sĩ BS);
  - danh mục dị nguyên có "Penicillin" (thuốc/hoạt chất) và "Tôm" (thực phẩm); mức độ, nguồn thông tin, nhu cầu chăm sóc theo giá trị khởi tạo;
  - người thân R (người đại diện của A ở 012), M (người thân khác của A), P (người thân của B, là người đại diện theo pháp luật theo quyết định của Tòa án, có bản scan mẫu);
  - bean giả `ThamGiaChuyenTrangThai` cho 003, 004, 005, 006, 007, 010, 011, 012, 013. Mỗi bean có công tắc Đạt / Chưa đạt qua `/api/v1/kiem-thu/gia-lap/{module}` (chỉ profile `test`, `demo`);
  - bản giả `ChiSoApi` (007) trả hai chỉ số gần nhất;
  - bản giả 006 có đơn amoxicillin Hiệu lực cho A.

## 2. Chạy

```text
backend/  ./mvnw verify
backend/  ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
frontend/ npm ci && npm test && npm run dev
mobile/   flutter test
```

Kết quả mong đợi: `verify` xanh, gồm test kiến trúc (`HoSoMoGuardArchTest`, Modulith `verify()`), test cơ sở dữ liệu cho trigger và cột sinh (data-model mục 9), và các nhóm test ở mục 4.

## 3. Kịch bản kiểm chứng đầu–cuối (profile demo)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| Q1 | Hành chính tạo hồ sơ A đủ thông tin, CCCD X | 201, `maHoSo` dạng `NCT-2026-…`, trạng thái `DANG_TIEP_NHAN`, lịch sử có dòng `TAO_HO_SO` | US1 #1, FR-001 → FR-003 |
| Q2 | Tạo hồ sơ khác với CCCD X | 409 `CCCD_DANG_DUNG` kèm mã hồ sơ A | US1 #2, DBR-01 |
| Q3 | Tạo hồ sơ không CCCD, trùng họ tên (khác hoa thường, thừa khoảng trắng) và ngày sinh; gửi lại với `xacNhanKhongTrung` | 409 `CO_THE_TRUNG_HO_SO`; 201 | US1 #4, FR-005 |
| Q4 | Nhân viên bếp tra cứu hồ sơ; Nhân viên chăm sóc T1 xem A (trong phạm vi) rồi B (ngoài phạm vi) | 403; A 200 có dị ứng, không có nhận xét; B 404 | US1 #6, #7, SC-012 |
| Q5 | Điều dưỡng ghi dị ứng "Penicillin" mức Nặng cho A | 201, `choKiemTra = true`; bản giả 006 tạo cảnh báo xung đột amoxicillin; cờ được gỡ | US2 #1, #2, #8, FR-020 |
| Q6 | Ghi dị ứng loại "khác" "phấn hoa cây X" | 201 kèm cảnh báo `DI_UNG_KHONG_KIEM_TRA_TU_DONG`; mục mang `khongKiemTraTuDong` | US2 #8, FR-018a |
| Q7 | Tắt listener 011, ghi dị ứng "Tôm"; tua đồng hồ 15 phút; bật lại listener | Mục lưu ngay với `choKiemTra`; job phát lại; cờ gỡ sau khi 011 chạy | BR-M01-12, Edge Case |
| Q8 | Loại trừ mục Penicillin không lý do, rồi có lý do; gọi `PUT`/`DELETE` lên mục | 422; 200 `DA_LOAI_TRU`; không có endpoint (405); `UPDATE` trực tiếp ở cơ sở dữ liệu bị trigger chặn | US2 #3, #4, DBR-04 |
| Q9 | Trưởng tầng ghi mục sức khỏe | 403 | US2 #6 |
| Q10 | Điều dưỡng tạo đánh giá đầu vào A, điền đủ, thử xác nhận; Bác sĩ xác nhận khi thiếu mức; rồi đủ | 403; 409 chỉ ra `mucChamSoc`; 200, mức hiện hành `THUONG_XUYEN`, không có kế hoạch chăm sóc nào được tạo | US3 #2, #3, #6, FR-028, FR-031 |
| Q11 | Tạo nháp thứ hai khi đã có nháp; Hủy nháp không lý do, rồi có lý do | 409 `DA_CO_BAN_NHAP`; 422; 200 | BR-M01-13 |
| Q12 | Xem thẻ người cao tuổi của A ở app nhân viên | Hiện lưu ý chăm sóc của lần vừa xác nhận | US3 #4, FR-038 |
| Q13 | Hoàn tất tiếp nhận A khi bean 004 "hợp đồng" và 003 "giường" Chưa đạt | 409 liệt kê hai điều kiện trong một lần, trạng thái giữ nguyên | US4 #2, FR-045, SC-002 |
| Q14 | Bật mọi bean Đạt, A chưa có bản đồng ý và nguyện vọng; Hoàn tất tiếp nhận | 200 `DANG_LUU_TRU`, cảnh báo `THIEU_BAN_DONG_Y`, `THIEU_NGUYEN_VONG`; tua 1 ngày: Hành chính và Bác sĩ, Điều dưỡng được nhắc | US4 #1, US5 #7, US7 #4 |
| Q15 | Hồ sơ C có đánh giá đầu vào xác nhận cách đây 120 ngày (tua đồng hồ); Hoàn tất tiếp nhận; Bác sĩ xác nhận đánh giá lại; thử lại | 409 "đánh giá đã quá hạn"; 200 với mức của lần đánh giá lại | US4 #2, FR-034a |
| Q16 | Bean giả 006 ném lỗi trong `thucHienTacDong` khi Cho tạm vắng | Lệnh lỗi (500 với lỗi kỹ thuật, 409 với `LoiNghiepVu`); trạng thái, lịch sử, nhật ký, bản ghi của các bean khác đều không đổi | FR-047 |
| Q17 | Hai luồng gửi đồng thời Cho tạm vắng và Chuyển viện cho A | Một lệnh 200; lệnh kia được kiểm lại trên trạng thái mới (thành công nếu hợp lệ, hoặc 409) | US4 #13, FR-048 |
| Q18 | Kết thúc lưu trú A khi Hoạt động bên ngoài | 409 `CHUYEN_KHONG_HOP_LE` | US4 #5 |
| Q19 | Hành chính Ghi nhận qua đời khi A Đang lưu trú; khi A Tạm vắng không tệp; có tệp | 403/409; 422; 200, sự kiện Khẩn cấp cho người liên hệ chính | US4 #11, FR-047b |
| Q20 | Trên A ở `QUA_DOI`: cập nhật thông tin, ghi mục sức khỏe; rút lại đồng ý; Bác sĩ đính chính đánh giá cuối | 409 `HO_SO_DA_DONG`; 200; yêu cầu phê duyệt Chờ duyệt, bản gốc chưa đổi, không báo đánh giá lại | US4 #12, FR-049, FR-049a |
| Q21 | Hành chính ghi bản đồng ý cho A gồm R, không gồm M, thiếu tệp; rồi đủ; R và M xem sức khỏe A | 422; 201; R thấy, M không thấy | US5 #1, #2, #4 |
| Q22 | Ghi bản đồng ý mới chỉ gồm M | Bản cũ `DA_RUT_LAI` "được thay thế"; lần xem kế tiếp R không thấy sức khỏe | US5 #6, FR-013, SC-008 |
| Q23 | Tua đồng hồ tới 90 ngày sau đánh giá gần nhất của A; chạy job | Bác sĩ nhận "cần đánh giá lại" (Nhẹ), căn cứ định kỳ; không có bản ghi yêu cầu nào | US6 #1, SC-007 |
| Q24 | Bản giả 007 phát sự cố ngã cho A khi đang cần đánh giá lại; tua quá 48 giờ từ lần báo đầu | Báo thêm căn cứ, hạn không đổi; cảnh báo quá hạn (Trung bình) một lần; 016 thấy dấu quá hạn | US6 #2 → #4, FR-041, FR-042 |
| Q25 | Bác sĩ xác nhận đánh giá lại A từ `CO_BAN` sang `DAC_BIET` khi bản giả 004 có yêu cầu đổi mức Chờ duyệt | Yêu cầu cũ `DUOC_THAY_THE`; Hành chính được báo (Trung bình), rồi được nhắc mỗi ngày tới khi có yêu cầu; mức hiện hành vẫn `CO_BAN` | US6 #5, #6, FR-036, FR-036a |
| Q26 | Bản giả 004 áp dụng yêu cầu đổi mức ngày 15/06; tra mức ngày 01/06 và 20/06 | `CO_BAN` (căn cứ đánh giá) và `DAC_BIET` (căn cứ yêu cầu) | US6 #11, FR-053 |
| Q27 | A Điều trị tại bệnh viện khi đến hạn định kỳ; Ghi nhận trở về sau 5 ngày | Báo định kỳ khi đang nằm viện, chưa tính hạn; khi trở về báo thêm "trở về từ bệnh viện", hạn tính từ lúc trở về | Edge Case, FR-041 |
| Q28 | Cho tạm vắng A với dự kiến trở lại 17:00; tua tới 19:01; chạy job hai lần; gia hạn tới 21:00; tua tới 23:01 | Một cảnh báo cho Trưởng tầng và Hành chính; không cảnh báo lặp; cảnh báo mới cho mốc 21:00 | US4 #9, FR-050, SC-013 |
| Q29 | Điều dưỡng ghi nguyện vọng cho B (Bác sĩ nhận định B không tự quyết được, có cách hỗ trợ và ngày xem lại) với người ký là B; rồi người ký là con của B, `DAI_DIEN_NGUOI_BENH`, `cachChonDaiDien = NCT_DA_CHON`; Hành chính xem | 409 `NGUOI_KY_KHONG_HOP_LE`; 201; Hành chính chỉ thấy `coNguyenVong = true` | US7 #2, #6, FR-023c, FR-023e (sửa 2026-10-18) |
| Q30 | Ghi phiên bản nguyện vọng mới cho A | Phiên bản cũ `DUOC_THAY_THE`; tối đa một phiên bản Hiệu lực | US7 #5, DBR-34 |
| Q31 | Hồ sơ A1 (CCCD Y) Kết thúc lưu trú 2025, A2 Kết thúc lưu trú 2026; tạo hồ sơ mới CCCD Y; tạo hồ sơ CCCD Z trùng hồ sơ Qua đời | Hồ sơ mới liên kết A2, chuỗi xem được A2, A1; hồ sơ Z không liên kết, cảnh báo `CCCD_TRUNG_HO_SO_QUA_DOI` | US1 #3, #10, FR-004 |
| Q32 | Bác sĩ chọn mức `DAC_BIET` cho người có hợp đồng Bán trú | Chấp nhận; loại lưu trú không đổi | US3 #8, FR-051, SC-010 |

## 4. Test tự động: truy vết kịch bản → test

| Spec | Lớp test (backend, `com.eldercare.backend.resident…`) | Loại |
| --- | --- | --- |
| US1 #1–10, FR-001 → FR-009 | `TaoHoSoIT`, `HoSoCccdIT`, `HoSoTrungTenIT`, `HoSoLienKetIT`, `CapNhatThongTinIT`, `TraCuuPhamViIT`, `GioiHanTruongHoSoIT` | Tích hợp |
| US2 #1–8, FR-017 → FR-023 | `MucSucKhoeBatBienIT`, `BR_M01_07_DiUngIT`, `BR_M01_12_ChoKiemTraIT`, `MucSucKhoeTrungIT`, `HoSoSucKhoeBanDauIT`, `BR_M01_11_DanhMucIT` | Tích hợp, cơ sở dữ liệu |
| US3 #1–8, FR-024 → FR-038 | `BR_M01_09_DanhGiaIT`, `XacNhanDanhGiaIT`, `BR_M01_13_MotBanNhapIT`, `BR_M01_10_LuuYChamSocIT`, `DinhChinhDanhGiaIT` | Tích hợp |
| US4 #1–13, FR-044 → FR-050 | `BR_M01_04_BangChuyenTest`, `FR_045_LietKeDieuKienIT`, `FR_047_RollbackTacDongIT`, `FR_048_LenhDongThoiIT`, `BR_M01_05_HoSoDongIT`, `BR_M01_06_LenhRiengIT`, `GhiNhanQuaDoiIT`, `BR_M01_01_TamVangQuaHanIT`, `HoSoMoGuardArchTest` | Đơn vị, tích hợp, kiến trúc |
| US5 #1–9, FR-010 → FR-016 | `BanDongYIT`, `RutLaiDongYIT`, `BR_M01_08_DongYIT`, `NhacThieuDongYIT` | Tích hợp, đồng hồ giả lập |
| US6 #1–12, FR-039 → FR-043 | `BR_M01_02_BaoDanhGiaLaiIT`, `BR_M01_03_DoiMucIT`, `NhacLapYeuCauDoiMucIT`, `FR_053_MucTaiNgayIT` | Tích hợp, đồng hồ giả lập |
| US7 #1–6, FR-023a → FR-023f | `NguyenVongPhienBanIT`, `NguyenVongNguoiKyIT`, `NguyenVongGioiHanXemIT`, `NhacThieuNguyenVongIT` | Tích hợp |
| Mục G, FR-051 → FR-053 | `DocLapLuuTruMucChamSocIT`, `FR_053_MucTaiNgayIT` | Tích hợp |
| Edge Cases | Mỗi gạch đầu dòng một phương thức trong các lớp trên | |
| Client web | `HoSo.test.jsx` (`truongBiAn`), `DieuKienChuaThoa.test.jsx`, `XacNhanTrung.test.jsx`, `DanhGia.test.jsx` (không có ô điểm, mức đề xuất) | Vitest |
| App nhân viên | `the_nguoi_cao_tuoi_test.dart` (lưu ý chăm sóc, dấu "không kiểm tra tự động", "chờ kiểm tra") | flutter_test |

## 5. Tiêu chí hoàn tất (đối chiếu SC)

- SC-001, SC-002, SC-003, SC-004, SC-007, SC-008, SC-009, SC-010, SC-011, SC-012, SC-013, SC-014 (bổ sung 2026-10-22; phần "lệnh Chuyển viện hiện đủ" đo ở quickstart 007): test tự động ở mục 4, chạy trên MySQL thật và đồng hồ giả lập.
- SC-005 (đánh giá đầu vào trong 10 phút) và SC-006 (tạo hồ sơ trong 5 phút): đo với người dùng đã được hướng dẫn trên profile `demo`, lấy trung vị của ít nhất 10 lần, theo cách đo ghi trong spec.

## 6. Kịch bản bổ sung 2026-10-18 (Q-347, Q-348)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| Q33 | Hành chính ghi bản đồng ý cho A với `DAI_DIEN_PHAP_LUAT` là R (người đại diện ở 012), không có `quyetDinhToaAn` | 422 `THIEU_QUYET_DINH_TOA_AN` | US5 #3, #10, FR-011 |
| Q34 | Ghi bản đồng ý cho B với `DAI_DIEN_PHAP_LUAT` là P, đủ số, ngày, Tòa án, tệp | 201; bản đồng ý mang `quyetDinhToaAn` | US5 #3, FR-011 |
| Q35 | Ghi nguyện vọng cho A: Bác sĩ nhận định A tự quyết được, thiếu `cachHoTroHieu`; rồi đủ, kèm ý kiến khác của R (nguồn `NGUOI_THAN` R) và của một người thân không có quan hệ Hiệu lực | 422; 422 `NGUON_Y_KIEN_KHONG_HOP_LE`; bỏ ý kiến sai thì 201, phiếu có nhận định, ngày xem lại và một ý kiến khác có nguồn R | US7 #3, #7, FR-023a, FR-023c, FR-023g |
| Q36 | Đọc hồ sơ A qua API và schema cơ sở dữ liệu | Không có trường hay cột năng lực chung nào; `FR_023h_KhongDauNangLucArchTest` xanh | US7 #7, FR-023h |

| Q37 | Phiếu của A có ngày xem lại nhận định là ngày mai; tua đồng hồ qua ngày đó; chạy job hai lần | Bác sĩ đã nhận định và Điều dưỡng phụ trách nhận một nhắc Nhẹ; lần chạy thứ hai không nhắc | FR-023i, Q-360 |

Test bổ sung: `NhacXemLaiNhanDinhIT` (Q37), `FR_011_DaiDienPhapLuatIT` (Q33, Q34), `NguyenVongNguoiKyIT` mở rộng (Q29, Q35), `BR_M01_14_NguonYKienIT` (Q35, `NguonYKienApi`), `FR_023h_KhongDauNangLucArchTest` (Q36).

## 7. Kịch bản bổ sung 2026-10-18, lượt 3 (Q-366)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| Y1 | 014 ghi mục sở thích của A nguồn `NGUOI_CAO_TUOI` kèm `phanHoiId` của phản hồi Mong muốn P; ghi mục nguồn `NGUOI_THAN` kèm `phanHoiId` | Sự kiện `YKienDaGhiTuPhanHoi` được phát, P có liên kết; mục thứ hai không gắn vào P | FR-023g |

Test: `FR_023g_YKienTuPhanHoiIT` (dùng bản giả 012 nghe sự kiện).

## 8. Kịch bản bổ sung 2026-10-22 (Q-383, Q-384)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| T1 | Điều dưỡng ghi phiếu nguyện vọng khi điều trị cho A (Đang tiếp nhận): bệnh viện ưu tiên "Bệnh viện X", `hoiGiaDinhTruoc = true`, người cần báo là R (con của A), `nguoiDiKem = GIA_DINH`, người trả lời `NGUOI_CAO_TUOI` | 201; phiên bản `HIEU_LUC` đầu tiên; `NguyenVongApi.dieuTriHieuLuc(A)` trả đủ bệnh viện, R kèm số điện thoại, người đi kèm | US7 #9, FR-023j, FR-023 |
| T2 | Ghi phiên bản mới đổi bệnh viện ưu tiên; Hành chính xem hồ sơ và `GET …/nguyen-vong-dieu-tri` | Phiên bản cũ `HET_HIEU_LUC`, vẫn trong `lichSu`; Hành chính thấy `coNguyenVongDieuTri = true`, endpoint phiếu trả 403; Kế toán xem hồ sơ không có trường cờ (nằm trong `truongBiAn`) | US7 #10, FR-023k, FR-023m, DBR-39 |
| T3 | Ghi phiếu có người cần báo là người thân không có quan hệ Hiệu lực; rồi `hoiGiaDinhTruoc = true` không có người cần báo | 409 `NGUOI_THAN_KHONG_HOP_LE`; 201 kèm cảnh báo `CHUA_CO_NGUOI_CAN_BAO` | FR-023j |
| T4 | C chưa có phiếu; Hoàn tất tiếp nhận (mọi bean Đạt); tua đồng hồ 30 ngày | 200 kèm cảnh báo `THIEU_NGUYEN_VONG_DIEU_TRI`; không có nhắc nào về phiếu này | US7 #11, FR-023l |
| T5 | B ở Điều trị tại bệnh viện; Ghi nhận trở về kèm hai tệp (giấy ra viện, đơn thuốc); Cho tạm vắng kèm `tepDinhKemIds` | 200, `giayToDinhKem(lichSuId)` trả hai tệp, Điều dưỡng tải được, Hành chính bị chặn; 422 `KHONG_NHAN_TEP_DINH_KEM` | FR-047c |

Test: `NguyenVongDieuTriPhienBanIT` (T1, T2, T3; DBR-39), `NguyenVongDieuTriGioiHanXemIT` (T2), `HoanTatTiepNhanCanhBaoDieuTriIT` (T4, đồng hồ giả lập), `GiayToBenhVienIT` (T5).
