# Quickstart: Kiểm chứng feature 009

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md)

Hướng dẫn dùng ở giai đoạn triển khai; giai đoạn thiết kế chưa có mã nên các lệnh chưa chạy được. Hợp đồng xem [contracts/api-009.yaml](contracts/api-009.yaml) và [contracts/module-interfaces.md](contracts/module-interfaces.md); bảng và ràng buộc xem [data-model.md](data-model.md).

## 1. Điều kiện cần

- Như quickstart của 000 → 008: JDK 25, MySQL 8.0 cài trên máy (000 R-14), Node.js LTS, Flutter stable; profile `demo` có đồng hồ giả lập.
- Dữ liệu mẫu của profile `demo` (nhân vật của spec): người cao tuổi A ở Tầng 2; D Điều dưỡng phụ trách A; P Người phụ trách ca Tầng 2; T Trưởng tầng được giao Tầng 2; B Bác sĩ trực; Q Quản lý viện; M người liên hệ chính của A (có tài khoản cổng); R người đại diện (quyền xem chi phí); N người thân khác (không quyền sức khỏe); K người thân của E (không quan hệ với A); S Nhân viên chăm sóc; H Hành chính.
- CFG theo mặc định Phụ lục 25: CFG-M13-01 5 phút, CFG-M13-02 21:00–07:00, CFG-M13-03 5 phút, CFG-M13-04 5 phút, CFG-M13-05 1 lần, CFG-M13-06 07:30–17:00 thứ 2 → thứ 7.
- Kênh: trong ứng dụng, cổng người thân bật; tin nhắn dùng bản giả `NhaCungCapTinNhan` bật/tắt và gây lỗi được; FCM giả ghi lại lần đẩy và thời điểm.
- Bản giả `NguoiThanNhanThongBaoPort` (012); bộ phát sự kiện thử: endpoint chỉ có ở `test`, `demo` `/api/v1/kiem-thu/phat-su-kien` nhận một `YeuCauThongBao` để giả lập module nguồn.

## 2. Chạy

```text
backend/  ./mvnw verify
backend/  ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
frontend/ npm ci && npm test && npm run dev
mobile/   flutter test
mobile/  flutter test
```

Kết quả mong đợi: `verify` xanh, gồm Modulith `verify()` (M13 không bị module nào phụ thuộc ngoài qua `m13.api`; không có vòng M13 ↔ M10), test cơ sở dữ liệu (data-model mục 8) và các nhóm test ở mục 4.

## 3. Kịch bản kiểm chứng đầu–cuối (profile demo)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| Q1 | 007 kích hoạt khẩn cấp cho A lúc 10:00:00, người nhận D, P, T, B, Q, M | Sáu thông báo, mỗi người mọi kênh, FCM giả nhận ≤ 10:00:10; không ai chờ ai | US1 #1, SC-001 |
| Q2 | D, P, B, Q xác nhận trước 10:05; tua 10:05 | Bốn `DA_XAC_NHAN`; sự cố không đổi; lượt nhắc gọi T và M giao cho P (gọi T) và P (gọi M); không có yêu cầu nào | US1 #2, #7 |
| Q3 | P ghi "Đã liên lạc được" cho M; T tự xác nhận trước khi được gọi | M "đã xác nhận qua cuộc gọi"; lượt gọi T `DA_DUNG` "người nhận đã tự xác nhận", P được báo | US1 #3, #4 |
| Q4 | Tin nhắn M thất bại, M không có tài khoản | Lượt nhắc gọi M ngay, không chờ 5 phút | US1 #6, FR-028 |
| Q5 | P ghi "Không liên lạc được" cho M; R cũng không | Lượt gọi R ngay; hết người: Q nhận Khẩn cấp kèm danh sách cuộc gọi; M `KHONG_LIEN_LAC_DUOC`; 11:30 M xác nhận → `DA_XAC_NHAN` "xác nhận muộn" | US1 #8, FR-029a |
| Q6 | Lượt gọi M ở P quá 10:10 chưa có kết quả; lượt ở P lúc 18:58, ca kết thúc 19:00 | T nhận nhắc Khẩn cấp, P được báo; Người phụ trách ca đêm nhận nhắc, hạn mới | US1 #9, FR-030a |
| Q7 | T ghi kết quả khi lượt đang ở P; S ghi kết quả | 201, P được báo; 403 | US1 #10 |
| Q8 | 005 gửi Trung bình tới "Trưởng tầng" và "Người phụ trách ca" lúc 14:10; T đồng thời là P | T và P nhận; khi trùng người: một thông báo, hai căn cứ | US2 #1, #2, FR-012 |
| Q9 | Người phụ trách ca đổi từ P sang P2 lúc 13:00; yêu cầu lúc 13:05 | P2 nhận; thông báo cũ của P giữ | US2 #3 |
| Q10 | A không có Điều dưỡng phụ trách; yêu cầu "Điều dưỡng phụ trách" mặc định; leo thang 007 tới Trưởng tầng "không thay" khi tầng chưa có Trưởng tầng | P nhận căn cứ "thay Điều dưỡng phụ trách"; chỉ B nhận, nhóm Trưởng tầng ghi "bỏ qua" | US2 #4, FR-010 |
| Q11 | 007 gửi cho người tạm nhận P (nhóm `NhanVien` căn cứ `TAM_NHAN`) | P nhận căn cứ "tạm nhận – bàn giao chưa xác nhận" | US2 #5, FR-009 |
| Q12 | 000 yêu cầu tới người có quyền duyệt rồi người yêu cầu | Người nhận theo quyền duyệt hiện hành và người lập | US2 #6 |
| Q13 | S vắng ca; yêu cầu tới nhân viên phân công cho A | S không nhận | US2 #7 |
| Q14 | Gửi lại cùng yêu cầu 5 lần | 0 thông báo mới; 4 dòng nhận trùng | US2 #8, SC-007 |
| Q15 | Khẩn cấp Tầng 3 không có ai theo FR-010, mọi Quản lý viện bị khóa | Lỗi gửi "không có người nhận"; Trưởng tầng các tầng khác nhận, chỉ nhận dạng | US2 #9, FR-011 |
| Q16 | 008 gửi Nhẹ cho D; Trung bình cho T khi kênh tin nhắn bật; tắt kênh | Nhẹ: chỉ trong ứng dụng; Trung bình: ứng dụng + tin nhắn rút gọn; tắt: tin nhắn `KENH_CHUA_CAU_HINH`, vẫn Đã gửi | US3 #1 → #3 |
| Q17 | Nội dung rút gọn chứa khóa chỉ số | Tin nhắn `NOI_DUNG_KHONG_HOP_LE`, lỗi gửi; trong ứng dụng vẫn gửi | US3 #4, FR-020 |
| Q18 | 02:00 Trung bình cho Q; Khẩn cấp cho Q; 10:00 thứ 3 Trung bình cho Q | Tin nhắn `BO_QUA_NGOAI_CA`; Khẩn cấp có tin nhắn; trong giờ hành chính có tin nhắn | US3 #5, FR-016a |
| Q19 | Trung bình cho T lúc 17:00, 17:01, 17:03, 17:07 | Tin 17:00 ngay; 17:05 tin tổng hợp (2); 17:10 tin tổng hợp (1); khung kết thúc 17:15 | US3 #6, SC-009 |
| Q20 | 006 gửi Trung bình lúc 22:30 cho M, R; tua 07:00 | Giữ, dự kiến 07:00; 07:00 gửi, thời điểm yêu cầu 22:30 | US4 #1, #2, SC-003 |
| Q21 | Khẩn cấp lúc 23:10 cho M | Gửi ngay | US4 #3 |
| Q22 | 010 gửi "có quyền xem chi phí"; thông báo sức khỏe khi N bị rút bản đồng ý; bản tin ba phần | Chỉ R; N không nhận; R thấy chung + chi phí, M chung + sức khỏe, N chung | US4 #4 → #5a, SC-005 |
| Q23 | 007 gửi tiếp xúc cho K (nhóm `NguoiThan`, không quan hệ) | K chỉ thấy phần chung; không kênh → danh sách liên hệ trực tiếp | US4 #5b, Q-112 |
| Q24 | Thông báo Nhẹ cho N đang giữ; A qua đời trước 07:00; hoặc quyền của N bị bỏ | `KHONG_GUI` kèm lý do tương ứng | US4 #6, FR-015, FR-025 |
| Q25 | Khẩn cấp tới người liên hệ chính khi quyền "nhận khẩn" của M chưa bật; N không có quyền | M nhận; N không | US4 #7, Q-96 |
| Q26 | Trung bình cho M thất bại cổng, không số | M vào danh sách liên hệ trực tiếp; H ghi Đã liên hệ; mục chờ qua đầu ngày → H nhắc, Q được báo | US4 #8, FR-021 |
| Q27 | Thông báo giữ dự kiến 07:00; 23:00 đổi CFG-M13-02 thành 21:00–06:00; 05:30 đổi thành 21:00–05:00 | Dự kiến 06:00; gửi ngay lúc 05:30 | US4 #9, FR-024 |
| Q28 | D mở 1 trong 4 thông báo chưa xem, mở lại | Thời điểm xem lần đầu không đổi; còn 3 chưa xem; liên kết nguồn mở được nếu còn quyền | US5 #1 |
| Q29 | D hết phạm vi ca mở thông báo cũ; N mở thông báo sức khỏe sau khi bị rút bản đồng ý | Chỉ tiêu đề, loại, thời điểm; Q tra cứu thấy nội dung gốc | US5 #2, Q-99 |
| Q30 | Q tra cứu lịch sử sự cố X; ai đó sửa/xóa thông báo | Đủ cột BR-M13-05 ≤ 30 giây; không có thao tác | US5 #3, #4, SC-006 |
| Q31 | M mở thông báo bản tin | Thời điểm xem về 012 (`ThongBaoDaXem`) | US5 #5, BR-M10-06 |
| Q32 | D, P, T cùng "Xác nhận và tiếp nhận" | Ba xác nhận; một tiếp nhận thành công, hai `TU_CHOI` `DA_CO_NGUOI_TIEP_NHAN` | Edge Case, FR-026a |
| Q33 | Cảnh báo Khẩn cấp 10:00 và sự cố 10:02 cùng A, M chưa xác nhận | 10:05 một lượt gọi M; 10:07 thông báo sự cố gộp vào; một kết quả xác nhận cả hai | Edge Case, FR-027a |
| Q34 | B Khóa tạm lúc 09:58; Khẩn cấp 10:00 | B vẫn nhận (tin nhắn, hộp thư); 10:05 lượt gọi B | Edge Case, Q-109 |
| Q35 | 007 hạ sự cố khỏi Khẩn cấp khi lượt gọi đang Chờ | Lượt `DA_DUNG` "sự cố đã hạ mức"; không lượt mới | FR-029a, BR-M05-22 |
| Q36 | Yêu cầu thiếu mức; thiếu loại thông tin; thiếu bản ghi nguồn | Nhẹ + dấu; phần coi là sức khỏe + dấu; `BI_TU_CHOI`; cả ba vào lỗi gửi | FR-003, FR-013 |
| Q37 | Dừng backend 10 phút có thông báo Khẩn cấp quá hạn, khung gộp hết hạn, thông báo giữ hết giờ | Khởi động: lượt nhắc tạo một lần, tin tổng hợp gửi một lần, thông báo giải phóng | NFR-04, FR-042 |
| Q38 | 012 gửi Nhẹ "A đã tham gia buổi sinh hoạt" (`VE_NGUOI_CAO_TUOI`, `CHUNG`) tới mọi người thân của A; bản đồng ý A có `dongYThongBao = true`; rồi bản mới `false` | Lần đầu M, R nhận; lần sau không ai nhận, không vào lỗi gửi | US4 #10, FR-014, Q-353 |
| Q39 | 010 gửi "bảng chi phí đã chốt" (`VE_VIEC_NGUOI_THAN`, `CHI_PHI`) khi `dongYThongBao = false` | R (có quyền xem chi phí) nhận | US4 #10, FR-001a |
| Q40 | 012 gửi sự cố ngã gồm phần `CHUNG` và `SUC_KHOE`; M có quyền xem sức khỏe và trong bản đồng ý, N chỉ trong phạm vi nhận thông báo | M thấy hai phần, N chỉ phần chung | US4 #11, FR-013, Q-352 |
| Q41 | 007 gửi Khẩn cấp tới người liên hệ chính M (ngoài phạm vi) có mã `KHAN_CAP_NGUOI_LIEN_HE_CHINH`; một yêu cầu Khẩn cấp khác thiếu mã | M nhận, dòng `thong_bao` của M có mã, lịch sử gửi hiện mã; yêu cầu thiếu mã: M không nhận, lỗi gửi `THIEU_MA_TRUONG_HOP`, nhân viên vẫn nhận | FR-001a, FR-014a, Q-354 |
| Q42 | Yêu cầu tới người thân thiếu `doiTuongThongTin` | Coi là về người cao tuổi; dấu `THIEU_DOI_TUONG_THONG_TIN` trong lỗi gửi | FR-001a |

## 4. Test tự động: truy vết kịch bản → test

| Nhóm | Test | Kịch bản |
| --- | --- | --- |
| Hàm thuần | `ChuoiThayTest`, `LocPhanNoiDungTest`, `ChonKenhTest`, `KhungGopTest`, `GioYenTinhTest`, `NguoiNhacGoiTest`, `TrangThaiThongBaoTest`, `SC_004_NguoiNhanTest` (200 yêu cầu) | Q8 → Q13, Q16, Q19, Q20, Q27 |
| Tiếp nhận | `FR_003_KiemTraYeuCauIT`, `FR_005_ChongTrungIT`, `FR_006_LoiKhongHongNguonIT` | Q14, Q36 |
| Người nhận | `BR_M13_04_NguoiNhanTheoPhanCongIT`, `FR_010_CachThayIT`, `FR_011_KhongNguoiNhanIT`, `FR_012_GopNguoiNhanIT`, `FR_013_LocNguoiThanIT`, `FR_014_KhanCapNguoiThanIT`, `FR_015_QuaDoiIT` | Q8 → Q15, Q22 → Q25 |
| Kênh | `BR_M13_01_ChonKenhIT`, `FR_016a_NgoaiCaIT`, `FR_016b_GopTinNhanIT`, `FR_018_GuiLaiIT`, `FR_020_NoiDungKenhNgoaiIT`, `FR_021_LienHeTrucTiepIT` | Q16 → Q19, Q26 |
| Giờ yên tĩnh | `BR_M13_03_GioYenTinhIT`, `FR_024_DoiThamSoIT` | Q20, Q21, Q24, Q27 |
| Khẩn cấp | `SC_001_KhanCapDongThoiIT`, `BR_M13_02_LuotNhacGoiIT`, `FR_027a_GopLuotIT`, `FR_028_ThatBaiMoiKenhIT`, `FR_029a_ChuoiGoiIT`, `FR_030a_ChuyenNhacIT`, `FR_026a_XacNhanVaTiepNhanIT`, `FR_032_MotLuotIT` | Q1 → Q7, Q32 → Q35 |
| Hộp thư, tra cứu | `FR_034_HienThiTheoQuyenIT`, `BR_M13_05_LichSuGuiIT`, `FR_035_DuLieuChoNguonIT` | Q28 → Q31 |
| Bộ lập lịch | `ChayBuM13IT` | Q37 |
| Kiến trúc | `ApplicationModules.verify()`; ArchUnit: chỉ `m13` cài listener `CoYeuCauThongBao`; module khác không gọi kênh gửi trực tiếp | |
| Web (Vitest) | Hộp thông báo (Khẩn cấp lên đầu, xác nhận), luồng SSE, danh sách liên hệ trực tiếp | Q28, Q26 |
| App (`flutter_test`) | `NhanDayTest`, `XacNhanHaiThaoTacTest` (SC-008), `XacNhanBatBuocTrucTuyenTest`, `GhiKetQuaGoiTest` | Q1 → Q3 |

## 5. Tiêu chí hoàn tất (đối chiếu SC)

| SC | Cách đo |
| --- | --- |
| SC-001 | `SC_001_KhanCapDongThoiIT`: 20 lần, 6–10 người nhận; thời điểm FCM giả nhận − thời điểm tiếp nhận ≤ 10 giây cho 100% |
| SC-002 | Đồng hồ giả lập: lượt nhắc tạo ≤ 1 phút sau hạn (job 20 giây); 0 lượt cho người đã xác nhận |
| SC-003 | Bộ người thân 24 giờ: 0 thông báo Nhẹ/Trung bình trong giờ yên tĩnh; giải phóng ≤ 5 phút |
| SC-004 | `SC_004_NguoiNhanTest` |
| SC-005 | Q17, Q22 và quét nội dung mọi tin nhắn trong bộ kiểm thử |
| SC-006 | Q30 |
| SC-007 | Q14, Q24 |
| SC-008 | Đo trên app: mở và xác nhận ≤ 2 thao tác; vận hành thử: tỷ lệ xác nhận trong CFG-M13-01 từ `thong_bao_trang_thai` *(R-15: `thong_bao_trang_thai` → `thong_bao` (cột dẫn xuất))* |
| SC-009 | Q19 với 30 thông báo trong 60 phút |

## Kịch bản bổ sung 2026-10-18, lượt 3 (Q-366)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| R1 | 012 gửi lúc 22:00 yêu cầu Nhẹ "nhắc lượt thăm" nhóm `NguoiCaoTuoi(A)`, A có tài khoản Hoạt động | `CHO_HET_GIO_YEN_TINH`, đẩy tới app của A lúc 07:00 | US4 #12, FR-022 |
| R2 | Gửi yêu cầu Trung bình, rồi yêu cầu Nhẹ có phần `SUC_KHOE`, nhóm `NguoiCaoTuoi(A)` | Cả hai `BI_TU_CHOI` mã `NHOM_NGUOI_CAO_TUOI_KHONG_HOP_LE`, vào danh sách lỗi gửi | US4 #13, FR-022a |
| R3 | Gửi yêu cầu Nhẹ nhóm `NguoiCaoTuoi(B)`, B không có tài khoản | Không có `thong_bao`; không có dòng `KHONG_CO_NGUOI_NHAN` | US4 #13, FR-007 |

Test: `FR_022a_NguoiCaoTuoiNhanIT` (R1 → R3).

