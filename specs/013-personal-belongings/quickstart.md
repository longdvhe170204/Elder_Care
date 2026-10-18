# Quickstart: Kiểm chứng feature 013

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md)

Hướng dẫn dùng ở giai đoạn triển khai; giai đoạn thiết kế chưa có mã nên các lệnh chưa chạy được. Hợp đồng xem [contracts/api-013.yaml](contracts/api-013.yaml) và [contracts/module-interfaces.md](contracts/module-interfaces.md); bảng và ràng buộc xem [data-model.md](data-model.md).

## 1. Điều kiện cần

- Như quickstart của 000 → 012: JDK 25, MySQL 8.0 cài trên máy (000 R-14), Node.js LTS, Flutter stable; profile `demo` có đồng hồ giả lập.
- Dữ liệu mẫu của profile `demo`:
  - tài khoản: Hành chính HC; Điều dưỡng DD (tầng 2); Quản lý viện QL; Trưởng tầng TT; Nhân viên chăm sóc CS; người thân R (người đại diện của A, có giấy tờ), P (được phép đón), N (không có quyền nhận);
  - A nội trú tầng 2; vị trí "Tủ khóa HC" (không gắn tầng), "Tủ tầng 2" (gắn tầng 2);
  - loại đồ: điện thoại (có giá trị), tiền mặt (có giá trị, có số tiền, cần đồng ý), trang sức (có giá trị, cần đồng ý), quần áo, kính;
  - CFG theo mặc định Phụ lục 25: CFG-M12-01 điện thoại, trang sức, tiền mặt; CFG-M12-02 7 ngày; CFG-M12-03 90 ngày; CFG-M12-04 hằng tháng, hạn 3 ngày; CFG-M12-05 4 giờ; CFG-M12-06 2 lần qua 2 kênh; CFG-M12-07 2 lần; CFG-M10-08 4 giờ;
  - bản giả có công tắc `/api/v1/kiem-thu/gia-lap/{module}`: `SuCoLenhApi` (007) có chế độ lỗi; sự kiện `CaDaKetThuc` (008), `DaChuyenGiuong` (003).

## 2. Chạy

```text
backend/  ./mvnw verify
backend/  ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
frontend/ npm ci && npm test && npm run dev
mobile/   flutter test
mobile/  flutter test
```

Kết quả mong đợi: `verify` xanh, gồm Modulith `verify()` (không có vòng 013 ↔ 004, 007, 008, 012), test cơ sở dữ liệu (data-model mục 8) và các nhóm test ở mục 4.

## 3. Kịch bản kiểm chứng đầu–cuối (profile demo)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| Q1 | HC tiếp nhận điện thoại của A không ảnh | 409 `THIEU_ANH` | US1, BR-M12-05 |
| Q2 | Tiếp nhận điện thoại có ảnh, vị trí Tủ khóa HC | Đồ Đang giữ, một bản ghi Tiếp nhận; R nhận Nhẹ | US1, FR-004 |
| Q3 | Tiếp nhận 2.000.000 đồng tiền mặt, giao ngay, người giao là P | Chặn: người giao không phải người đại diện; R giao kèm bản ký → hai bản ghi, đồng ý Hiệu lực | US1, FR-005, BR-M12-06 |
| Q4 | DD tiếp nhận cho người ngoài phạm vi | 403 | FR-007 |
| Q5 | Tiếp nhận 10 bộ quần áo; chuyển giữ 4 bộ sang Tủ tầng 2 | Tách: đồ mới 4 bộ Đang giữ tại tầng 2; đồ cũ 6 bộ; tổng bằng 10 | US2, FR-012, SC-001 |
| Q6 | DD thu lại 6 bộ nhưng đếm được 5 | Tách 1 bộ và Báo thất lạc trong cùng lệnh | US2, FR-013 |
| Q7 | DD chuyển giữ điện thoại khi người giữ là HC đang trong ca | 409 `NGUOI_GIAO_KHONG_PHAI_NGUOI_GIU` | US2, FR-014 |
| Q8 | HC chuyển giữ thay khi người giữ không có ca đang diễn ra | Được, lý do "người giữ vắng mặt"; người giữ cũ nhận Nhẹ | FR-014 |
| Q9 | Hai nhân viên cùng giao sử dụng một đồ | Một thành công, một 409 `TRANG_THAI_DA_DOI` kèm trạng thái mới | US2, FR-011b |
| Q10 | Thu lại kính, ghi mức thấp hơn mà không chọn xử lý | 409 `CAN_CHON_XU_LY_TINH_TRANG`; chọn "ghi nhận" → không thông báo | FR-015 |
| Q11 | Giao sử dụng trang sức không có đồng ý | 409 `THIEU_DONG_Y_TU_GIU` | US2, SC-011 |
| Q12 | R rút đồng ý tiền mặt khi đồ Đang sử dụng | HC, điều dưỡng phụ trách nhận Trung bình; trạng thái không đổi | FR-015a |
| Q13 | Ca của DD kết thúc khi DD còn giữ điện thoại | DD và Người phụ trách ca nhận Trung bình; màn bàn giao 008 thấy đồ qua `DoGuiBanGiaoPort`; không có mục tự thêm | FR-014a |
| Q14 | HC trả điện thoại cho P, giấy tờ khớp, có ảnh, chữ ký | Đã trả, R nhận Nhẹ | US3, FR-016 |
| Q15 | Trả cho N | 409 `CHAN_TRA`, lần chặn ghi lại | US3, SC-004 |
| Q16 | Trả quần áo cho chính A (không thuộc loại cần đồng ý) | Được không cần xác nhận (Q-253) | US3, FR-016 |
| Q17 | HC lập xác nhận người nhận khác cho N; R xác nhận qua cổng; trả; trả lần hai | Hiệu lực → Đã dùng; lần hai chặn | US3, FR-017 |
| Q18 | R từ chối; QL duyệt thay | Từ chối; duyệt thay 409 `DA_TU_CHOI` | FR-017 |
| Q19 | Yêu cầu chờ 4 giờ, HC ghi 2 lần liên hệ qua 2 kênh với R; QL duyệt thay | QL nhận Trung bình khi đủ; duyệt thay được; R nhận Trung bình | US3, FR-017a |
| Q20 | QL tự lập rồi duyệt thay | 409 `TU_DUYET` | FR-017a |
| Q21 | DD trả 1 điện thoại + 2 bộ quần áo cho P | Chặn toàn bộ `DIEU_DUONG_DO_CO_GIA_TRI` | FR-016a |
| Q22 | Xác nhận Hiệu lực quá 4 giờ chưa dùng | Hết hạn, người lập nhận Nhẹ | FR-017 |
| Q23 | DD báo thất lạc kính, chọn ghi sự cố mới mức Nhẹ | Sự cố 007 nguồn "đồ gửi" trong cùng lệnh; người liên hệ chính và HC nhận Trung bình ≤ 5 phút | US4, FR-021, SC-005 |
| Q24 | Như Q23 nhưng 007 lỗi | Lệnh không lưu | FR-021 |
| Q25 | Ghi hư hỏng không ghi sự cố; sau đó liên kết sự cố CS đã ghi | Không có sự cố tự tạo; liên kết một lần; lần hai 409 `SU_CO_DA_LIEN_KET` | US4, FR-024 |
| Q26 | Tìm thấy kính (có sự cố liên kết) | Đang giữ; 007 nhận diễn biến "đã tìm thấy"; sự cố không tự đóng | US4 |
| Q27 | QL duyệt đính chính "Hủy ghi nhận" bản ghi Báo thất lạc có sự cố | Đồ về trạng thái trước; người xử lý sự cố được báo; sự cố không tự hủy; người đã nhận thông báo gốc nhận đính chính | FR-011a |
| Q28 | Đính chính đổi loại bàn giao | Bị chặn | FR-011a, Q-160 |
| Q29 | 004 gọi điều kiện kết thúc khi A còn đồ Hư hỏng và đồ Thất lạc có sự cố mở | `KT_B_DO_GUI` Chưa đạt; căn cứ liệt kê đồ Thất lạc "chặn qua điều kiện (d)" | US5, FR-025 |
| Q30 | Trả đồ cuối cùng | Điều kiện Đạt ≤ 1 phút | US5, SC-006 |
| Q31 | A qua đời còn 2 đồ Đang giữ; trả 1, báo thất lạc 1 | Mục `DO_GUI` Hoàn thành; `capNhat` được gọi | US5, FR-026 |
| Q32 | Tìm thấy sau khi mục Hoàn thành | Không mở lại; đồ vào nhắc CFG-M12-02, CFG-M12-03 tính từ lúc tìm thấy | FR-026a |
| Q33 | A Kết thúc lưu trú có ngoại lệ, còn đồ | Nhắc HC mỗi 7 ngày; Tiếp nhận mới bị chặn `NCT_TRANG_THAI_CUOI` | FR-027 |
| Q34 | Lập đề nghị xử lý ở ngày 60 | Chặn trước CFG-M12-03 | US5, SC-010 |
| Q35 | Ngày 91: đề nghị có tiền mặt hình thức Thanh lý; thiếu bằng chứng | Chặn; sửa Chuyển cơ quan, đủ bằng chứng → Chờ duyệt; HC tự duyệt bị chặn | US5, Q-152, Q-159 |
| Q36 | QL duyệt; trước áp dụng 1 đồ đã được trả | Áp dụng các đồ còn lại → Đã xử lý; đồ đã trả bị loại; R nhận Nhẹ | FR-027a |
| Q37 | A chuyển giường sang tầng 3 khi còn đồ ở Tủ tầng 2 | HC, điều dưỡng tầng 3 nhận Nhẹ; đồ không đổi | FR-028 |
| Q38 | Đầu kỳ: 2 vị trí có đồ có giá trị chưa có phiếu | HC nhận nhắc kèm số vị trí; không có phiếu tự tạo | US4, FR-028a, SC-009 |
| Q39 | HC lập phiếu Tủ khóa HC; lập lần hai cùng kỳ; lập cho vị trí không có đồ có giá trị | Phiếu gồm mọi đồ có giá trị Đang giữ; lần hai và vị trí trống bị chặn | FR-028a |
| Q40 | Hoàn thành phiếu: 1 Đúng, 1 Không tìm thấy, 1 đồ không rõ chủ | Không có bản ghi cho Đúng; Báo thất lạc cùng lần; QL nhận Nhẹ | FR-028b |
| Q41 | Quá hạn kỳ chưa hoàn thành | HC nhận mỗi ngày (Trung bình), QL một lần (Nhẹ); phiếu dấu "trễ hạn" | FR-028c |
| Q42 | P mở cổng xem đồ gửi; N mở | P thấy danh sách, lịch sử, ảnh; N 403 | US6, SC-012 |
| Q43 | Thông báo thất lạc tới người liên hệ chính | Chỉ loại đồ và sự kiện; không mô tả, số tiền, ảnh | SC-012 |
| Q44 | CS, Bác sĩ gọi API đồ gửi; TT xem ngoài tầng | 403 | FR-029 |
| Q45 | App nhân viên mất mạng khi trả đồ | Không cho lưu tạm | FR-031 |
| Q46 | Khởi động lại sau khi tắt qua mốc kỳ kiểm kê | Nhắc chạy bù đúng một lần | NFR-04 |

## 4. Nhóm test tự động

| Nhóm | Test | Kịch bản |
| --- | --- | --- |
| Hàm thuần | `KiemTraLenhBanGiaoTest`, `TachDoTest`, `NguoiNhanHopLeTest`, `DieuKienDoGuiTest`, `DuBangChungLienHeTest`, `MucDoGuiSauQuaDoiTest` | Q5 → Q8, Q14 → Q21, Q29 |
| Bàn giao | `BR_M12_01_BanGiaoIT`, `DBR_22_TiepNhanGocIT`, `FR_011b_PhienBanIT`, `FR_011a_DinhChinhIT` | Q1 → Q10, Q27, Q28 |
| Đồng ý, trả | `BR_M12_06_DongYTuGiuIT`, `BR_M12_04_TraDoIT`, `FR_017a_DuyetThayIT` | Q11 → Q22 |
| Sự cố | `BR_M12_02_ThatLacHuHongIT` | Q23 → Q26 |
| Kết thúc | `BR_M12_03_DieuKienKetThucIT`, `FR_026_ViecSauQuaDoiIT`, `BR_M12_08_XuLyDoIT` | Q29 → Q36 |
| Kiểm kê | `BR_M12_07_KiemKeIT` | Q38 → Q41 |
| Cổng | `BR_M12_09_CongDoGuiIT`, `SC_012_ThongBaoTest` | Q42, Q43 |
| Đồng thời | Hai bàn giao cùng đồ; Trả trong lúc Kết thúc lưu trú | Q9 |
| Bộ lập lịch | `ChayBuM12IT` | Q46 |
| Kiến trúc | `ApplicationModules.verify()`; ArchUnit: gói `..lich..` không tạo phiếu kiểm kê, không gọi `SuCoLenhApi` (Q-306, Q-307) | |
| Web (Vitest) | Màn bàn giao hiển thị chênh lệch tình trạng; màn kiểm kê không có danh sách vị trí "gợi ý" | Q10, Q38 |
| App (`flutter_test`) | App nhân viên chặn thao tác khi mất mạng; app người thân xác nhận người nhận khác, ghi/rút đồng ý | Q17, Q45 |

## 5. Tiêu chí hoàn tất (đối chiếu SC)

| SC | Cách đo |
| --- | --- |
| SC-001 | Bất biến tách và trạng thái sau mọi kịch bản, kể cả đính chính |
| SC-002 | Quét `ban_giao_do_gui`: đủ trường bắt buộc; trigger chặn sửa, xóa |
| SC-003 | Q1, Q14 |
| SC-004 | Q15 → Q20 |
| SC-005 | Q23 → Q25 |
| SC-006 | Q29, Q30 |
| SC-007 | Đo thao tác tiếp nhận một đồ có giá trị, trả 5 đồ |
| SC-008 | `GET /nguoi-cao-tuoi/{id}/do-gui` một truy vấn |
| SC-009 | Bộ 3 kỳ kiểm kê (Q38 → Q41) |
| SC-010 | Q34 → Q36 |
| SC-011 | Q11, Q12 |
| SC-012 | Q42, Q43 |

## 6. Kịch bản bổ sung 2026-10-18, lượt 3 (Q-366)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| G1 | A có 3 đồ, 1 đã trả; 012 gọi `DoGuiKenhNctPort.dangGiu(A)`; A mở ảnh một đồ của mình, rồi ảnh đồ của B | 2 đồ, tên, trạng thái, ảnh; ảnh của A mở được, của B 403 | US6 #5, FR-030a |

Test: `FR_030a_DoGuiKenhNguoiCaoTuoiIT`.

