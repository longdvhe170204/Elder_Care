# Màn hình theo actor

- **Ngày:** 2026-10-18. **Trạng thái:** bản đề xuất, chờ chủ dự án duyệt.
- **Căn cứ:** `docs/business/nghiep-vu.md` Phụ lục 26.1 (actor, kênh), 26.2 (use case), Phụ lục 27 (ma trận quyền; khác thì 19.3 là căn cứ). Tài liệu này chỉ gom chức năng thành màn, không đổi quyền hay nghiệp vụ.
- **Cách đọc:** mỗi màn gom các dòng Phụ lục 27 cùng đối tượng. Ký hiệu như Phụ lục 27: **C** cấu hình, **T** thực hiện, **D** duyệt, **X** xem, **P** xem trong phạm vi phân công, **—** không vào màn. Số mũ là chú thích của Phụ lục 27. Nút lệnh trong màn vẫn theo danh sách lệnh được phép do backend trả (DESIGN.md mục 1 điều 8).
- **Kênh (26.1):** Quản lý viện, Hành chính, Kế toán dùng web. Trưởng tầng, Bác sĩ, Điều dưỡng, Nhân viên chăm sóc dùng app nhân viên và web. Nhân viên bếp dùng app nhân viên và web (màn hình bếp). Người thân dùng cổng người thân trên web và app.

Viết tắt: QL Quản lý viện, TT Trưởng tầng, BS Bác sĩ, ĐD Điều dưỡng, CS Nhân viên chăm sóc, Bếp Nhân viên bếp, HC Hành chính, KT Kế toán, NT Người thân.

## 1. Danh sách màn và quyền

| Mã | Màn | Gồm (use case, mục) | Feature | QL | TT | BS | ĐD | CS | Bếp | HC | KT | NT | Bản mẫu |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| **Chung** | | | | | | | | | | | | | |
| M01 | Đăng nhập | UC-70 | 002 | T | T | T | T | T | T | T | T | T | Có, người thân (`/login`) |
| M02 | Thông báo | UC-66 | 009 | T | T | T | T | T | T | T | T | T | — |
| M03 | Tổng quan | UC-67 | 016 | X | P | P¹¹ | P | — | — | P¹¹ | P²⁹ | — | Có |
| M04 | Phê duyệt | UC-14 và các yêu cầu chờ duyệt (1.5) | 000 | D | D (đổi ca, lịch ca) | — | — | — | — | D⁴² | — | — | — |
| M05 | Nhật ký thay đổi, đính chính | 19.4, 1.5 | 000 | X, D | P, T¹³ | P, T¹³ | P, T¹³ | P, T¹³ | P, T¹³ | P, T¹³ | P, T¹³ | — | — |
| **Người cao tuổi** | | | | | | | | | | | | | |
| M06 | Người cao tuổi: danh sách, hồ sơ (gồm bản đồng ý, dị ứng, bệnh nền, nguyện vọng cuối đời, sở thích, lệnh trạng thái: tạm vắng, chuyển viện, kết thúc lưu trú, qua đời) | UC-01 → 04, 15 → 18, 82 | 001, 004 | X, T⁸ (tạm vắng), D (kết thúc, qua đời) | P, T (tạm vắng) | X, T (dị ứng, nguyện vọng, chuyển viện, qua đời) | P, T (dị ứng, nguyện vọng, chuyển viện) | P | — | T⁵ | X⁵, T³⁹ | X (cổng) | Có |
| M07 | Đánh giá, mức chăm sóc | UC-05, 06 | 001 | X | P | T, D | T | — | — | — | — | — | — |
| M08 | Kế hoạch chăm sóc | UC-22, 23 | 005 | X | X | D | T | P | — | — | — | — | — |
| M09 | Chỉ số sức khỏe, ngưỡng cảnh báo | UC-32, 33 | 007 | X | P | T (ngưỡng), X | T | T | — | — | — | X¹ (cổng) | — |
| M10 | Thuốc: đơn thuốc, đối chiếu, thuốc gia đình gửi, thuốc cung ứng | UC-39, 40, 43, 44, 11.7 | 006 | X | — | T | T | — | — | — | X (cung ứng) | X (cổng) | — |
| **Trong ca** | | | | | | | | | | | | | |
| M11 | Lập công việc của ca | UC-24 | 005 | X | T | — | T⁴¹ | — | — | — | — | — | — |
| M12 | Checklist ca, việc quá hạn | UC-25 → 27 | 005 | X | T | X²² | T | T | — | — | — | — | Có |
| M13 | Điểm danh bán trú | UC-28 | 005 | X | X | — | T²⁷ | T | — | T | — | — | — |
| M14 | Phát thuốc, thuốc khi cần | UC-41, 42 | 006 | X | X | X | T | — | — | — | — | — | Có |
| M15 | Cảnh báo | UC-34 | 007 | X | T | T | T | X | — | — | — | — | Có |
| M16 | Sự cố, khẩn cấp (gồm khoanh vùng, danh sách tiếp xúc, dấu nguy kịch) | UC-35 → 37, 83, 84 | 007 | X, T²³ | T¹⁰ | T | T¹⁰ | T¹⁰ | T¹⁰ | T¹⁰ | T¹⁰ | X¹ (cổng) | Có (chi tiết) |
| M17 | Bàn giao ca | UC-52, 53 | 008 | X | T | X | T | P²⁵ | — | — | — | — | Có |
| M18 | Hoạt động, ngoài viện, kiểm tra chất lượng | UC-29 → 31 | 014 | X | T | — | T²⁷ | T | — | — | — | X (cổng) | — |
| **Tầng, nhân sự** | | | | | | | | | | | | | |
| M19 | Phòng, giường (gồm cách ly phòng, xác nhận giường sẵn sàng, hư hỏng) | UC-19 → 21, 37, 74 | 003 | C³⁵, T | T | T (cách ly) | T⁴⁰, X | T⁴⁰ | — | T | — | — | Sơ đồ ở Tổng quan |
| M20 | Lịch ca, phân công, đổi ca | UC-50, 51, 54 | 008, 015 | T, D | T, D | P, T | P, T | P, T | P, T | P, T | — | — | — |
| M21 | Hồ sơ nhân viên, chứng chỉ, bộ phận và Phụ trách bộ phận, giao Trưởng tầng cho tầng | UC-49, 13.1a, 13.3 | 008 | T | X | — | — | — | — | — | — | — | — |
| **Bếp, dinh dưỡng** | | | | | | | | | | | | | |
| M22 | Chế độ ăn, món ăn, thực đơn | UC-45, 46 | 011 | X | — | T, D | X | — | T, X | — | — | X (cổng) | — |
| M23 | Suất ăn, phiếu bữa ăn, lưu mẫu, nhận phiếu, suất đặc biệt (bếp dùng thêm bản màn hình bếp, 26.1) | UC-47, 75 → 78 | 011 | X | X, T¹⁵ | X | T¹⁵, T²⁷ | T¹⁵, T | T, X¹⁴ | — | — | — | — |
| M24 | Đối chiếu đồ ăn gia đình | UC-48 | 011 | X | — | T | T | — | — | — | — | — | — |
| M25 | Kho nguyên liệu | UC-91, 92 | 019 | X, D³⁴ | — | X | — | — | T³⁴ | — | — | — | — |
| **Hành chính, gia đình** | | | | | | | | | | | | | |
| M26 | Tiếp nhận, hợp đồng, danh sách chờ, thay đổi lưu trú | UC-08 → 13 | 004 | X, D | — | T (UC-13) | — | — | — | T, D⁴² | T²⁶ (cọc) | X⁷, T² (cổng) | Lệnh Hoàn tất ở M06 |
| M27 | Khai báo tạm trú, lưu trú | UC-80, 81 | 018 | X | — | — | — | — | — | T | — | — | — |
| M28 | Người thân: quyền, mong muốn về khách thăm và người đón, người thân ở lại | UC-55, 14.1, 14.4 | 012 | D¹⁸, X | T (mong muốn), T²⁰ | — | T (mong muốn) | — | — | T | — | T² (cổng) | Có |
| M29 | Thăm, đón | UC-57, 58 | 012 | X, D¹⁹ | X, T¹⁹ | — | T²⁷ | T¹⁹ | — | T | — | T, T² (cổng) | — |
| M30 | Phản hồi, khiếu nại | UC-59 | 012 | D | T | — | T | — | — | T | — | T (cổng) | — |
| M31 | Bản tin | UC-60 | 012 | X | X²¹ | — | T | — | — | — | — | X (cổng) | — |
| M32 | Đồ gửi | UC-65 | 013 | X | X | — | T | — | — | T | — | X (cổng) | — |
| **Tài chính** | | | | | | | | | | | | | |
| M33 | Chi phí, khoản điều chỉnh, chốt kỳ, xuất kế toán, mua hộ | UC-61 → 64, 79 | 010 | D | — | — | — | — | — | T | X¹⁶, T²⁸ | X¹⁶, T² (cổng) | — |
| M34 | Số dư, thu chi, sổ cọc, đối soát, sao kê, chốt quỹ ngày | UC-85 → 88, 93 | 017 | D³² | — | — | — | — | — | X | T | X¹⁶ (cổng) | — |
| M35 | Tài sản, lịch xe, yêu cầu mua sắm | UC-89, 90, 94, 95 | 019 | C, D | T³³ ³⁷ | T³⁷ | T³⁷ ⁴⁴ | T³⁷ | T³⁷ | T³³ ³⁷ | T³⁸ | — | — |
| **Quản trị** | | | | | | | | | | | | | |
| M36 | Tài khoản, phân quyền, tham số, danh mục | UC-68, 69, BR-M01-11, 6.4, 11.1 | 000, 002 | C | — | X (danh mục) | X (danh mục) | — | — | T¹² | — | — | — |
| **Cổng người thân** | | | | | | | | | | | | | |
| M37 | Cổng người thân: thẻ người cao tuổi, thông tin được phép, lịch sinh hoạt; dẫn tới M02, M09, M16, M26, M28 → M34 phần người thân | UC-56 | 012 | — | — | — | — | — | — | — | — | T | Có (`/family`, P1 → P11) |
| **Bổ sung sau khi rà nghiệp vụ** | | | | | | | | | | | | | |
| M38 | Khám và điều trị: khám, chẩn đoán, chỉ định, điều trị, kết quả, cơ sở thực hiện | 10.2, 10.4 | 007 (FR-072, 073) | ? | ? | T | ? | — | — | — | — | X¹ (cổng)? | — |
| M39 | Thời khóa biểu cá nhân theo ngày (công việc, liều thuốc, bữa ăn, hoạt động, lịch thăm; chỉ xem) | 8.2 | 005 (FR-048) | ? | P | ? | P | ? | — | — | — | — | — |
| M40 | Hồ sơ tinh thần theo khoảng thời gian (tâm trạng, giao tiếp, mức tham gia, sở thích; chỉ xem) | 8.10 | 014 (FR-053) | ? | P | ? | P | P | — | — | — | — | — |
| M41 | Báo cáo: người cao tuổi, chăm sóc, sức khỏe, chi phí, nhân sự và ca trực | 18.1 → 18.4, 18.6, UC-67 | 016 | X | P | P¹¹ | P | — | — | P¹¹ | P²⁹ | — | — |
| M42 | Bản ghi ngoại tuyến chờ xem lại (quá CFG-M15-08 hoặc thời điểm thiết bị bất hợp lý) | Q-01, 8.6 | 002, 005, 006, 007, 012 | ? | T | — | — | — | — | — | — | — | — |
| M43 | Tài khoản của tôi: đổi mật khẩu, buộc đổi mật khẩu lần đầu | 19.1, BR-M15-21, 22 | 002 | T | T | T | T | T | T | T | T | T | — |
| M44 | Giấy phép hoạt động của cơ sở (số, cơ quan cấp, ngày cấp, hết hạn, phạm vi) | 10.4 | 002 (FR-039), 007 | ? (nhận cảnh báo hết hạn) | — | ? | — | — | — | ? | — | — | — |
| M45 | Trang chủ cổng người thân: giới thiệu viện, mở ngay sau khi người thân đăng nhập (không còn trang công khai) | DESIGN.md 2.1 | — | — | — | — | — | — | — | — | — | T | Có (`/family`, Trang chủ) |

Ghi chú:
- M04: Phụ lục 27 không có dòng "Phê duyệt" riêng; ô ghi theo các dòng có D. Trưởng tầng duyệt đổi ca (dòng "Yêu cầu đổi ca"), lịch ca; Hành chính duyệt thay đổi nhỏ (⁴²). Các yêu cầu D khác của QL (hợp đồng, thanh lý, mua sắm, ngoại lệ đón…) cũng vào đây.
- M16: QL ghi nhận sự cố, kích hoạt khẩn cấp, tiếp nhận ở leo thang cấp 2 (²³); BS, ĐD thêm khoanh vùng, danh sách tiếp xúc, dấu nguy kịch theo dòng tương ứng.
- M35: T của nhân viên là khi giữ nhiệm vụ Phụ trách bộ phận (³⁷), không theo vai trò; mọi nhân viên đều có thể có màn này.
- **?** ở M38 → M44: Phụ lục 27 không có dòng cho chức năng này; ô "?" là chưa có căn cứ, không đoán. Ô có giá trị lấy từ spec (ghi trong cột Feature) hoặc Q-xx.
- M39, M40 có thể là tab trong hồ sơ M06; M41 có thể là tab của M03.
- Người phụ trách ca là nhiệm vụ trong ca (2.4), không phải vai trò: người giữ nhiệm vụ có thêm lệnh ở M11, M17, M19, M18 (kiểm tra chất lượng, ³⁶).

## 2. Màn theo từng actor

Thứ tự là thứ tự đề xuất trên menu. In nghiêng là màn chỉ xem. Mọi actor có thêm M01 Đăng nhập và M43 Tài khoản của tôi (ở khối người dùng, không lên menu). Ô "?" ở mục 1 chưa đưa vào bảng này.

| Actor | Kênh | Màn |
| --- | --- | --- |
| Quản lý viện | Web | M03 Tổng quan, M41 Báo cáo, M04 Phê duyệt, M02 Thông báo; *M06*, *M07*, *M08*, *M09*, *M10*; *M11*, *M12*, *M13*, *M14*, *M15*, M16, *M17*, *M18*; M19, M20, M21; *M22*, *M23*, *M24*, M25; *M26*, *M27*, M28, *M29*, M30, *M31*, *M32*; M33, M34, M35; M36, M05 |
| Trưởng tầng | App nhân viên, web | M03, M41, M02; M12, M11, M42, M15, M16, M17, M18; *M39*, *M40*; *M06*, *M07*, *M08*, M09; M19, M20, *M21*; M23; M28, M29, M30, *M31*, *M32*; M04 (đổi ca, lịch ca); M35; M05 |
| Bác sĩ | App nhân viên, web | M03, M41, M02; M07, M38, M08, M09, M10; M16, M15, *M12*, *M14*, *M17*; M22, *M23*, M24, *M25*; M19 (cách ly); M20; M35; M05 |
| Điều dưỡng | App nhân viên, web | M03, M41, M02; M14, M12, *M39*, *M40*, M11 (việc phát sinh), M15, M16, M17, M13, M18; M06, M07, M08, M09, M10; M23, M24, *M22*; M19 (giường sẵn sàng); M28 (mong muốn), M29, M30, M31, M32; M20; M35; M05 |
| Nhân viên chăm sóc | App nhân viên, web | M02; M12, M13, M18, *M40*, M16, *M15*, *M17*; *M06*, M09, *M08*; M23 (nhận phiếu, suất đặc biệt); M19 (giường sẵn sàng); M29 (đón theo lệnh nguồn); M20; M35; M05 |
| Nhân viên bếp | App nhân viên, web (màn hình bếp) | M02; M23, M22, M25; M16; M20; M35; M05 |
| Hành chính | Web | M03, M41, M02; M06, M26, M27; M28, M29, M30, M32; M33, *M34*; M19; M13; M16; M20; M35; M36; M04 (thay đổi nhỏ); M05 |
| Kế toán | Web | M03, M41 (báo cáo chi phí, thu chi), M02; M34, M33, M26 (cọc); *M06*; *M10* (cung ứng); M35 (đối chiếu chứng từ); M16; M05 |
| Người thân | Cổng người thân (web, app) | M01, M37, M02; M06, M09, M10, M16 trong cổng theo quyền và bản đồng ý; M26 (xác nhận, yêu cầu), M28 (người đại diện), M29, M30, M31, M32, M33, M34 |

## 3. Lệch giữa menu bản mẫu (`frontend/src/common/layout/navigation.js`) và bảng trên

Menu hiện có 16 mục. Các chỗ lệch, chưa sửa, chờ duyệt:

1. **Thiếu màn trên menu:** M04 Phê duyệt, M02 Thông báo, M07 Đánh giá, M08 Kế hoạch chăm sóc, M09 Chỉ số, M10 Thuốc, M11 Lập công việc của ca, M13 Điểm danh bán trú, M18 Hoạt động, M21 Hồ sơ nhân viên, M22 Thực đơn, M24, M25 Kho, M27 Khai báo tạm trú, M30 Phản hồi, M31 Bản tin, M32 Đồ gửi, M34 Số dư thu chi, M35 Tài sản, mua sắm. Một số có thể là tab trong màn khác (M07, M08, M09 trong hồ sơ M06), cần chốt.
2. **Thiếu vai trò ở mục có sẵn:**
   - "Phòng, giường" thiếu BS (đặt cách ly phòng), ĐD, CS (xác nhận giường sẵn sàng khi là Người phụ trách ca, ⁴⁰).
   - "Suất ăn" thiếu ĐD, CS (nhận phiếu, báo sai lệch, xác nhận suất đặc biệt: UC-76, 77).
   - "Đăng ký thăm" thiếu ĐD, CS (quy trình đón theo lệnh nguồn, ¹⁹ ²⁷); tên mục nên là "Thăm, đón".
   - "Tiếp nhận, hợp đồng" thiếu BS (yêu cầu thay đổi lưu trú, UC-13).
3. **Người thân dùng chung khung nhân viên:** menu hiện cho NT vào các mục "Người cao tuổi", "Sự cố", "Tiếp nhận, hợp đồng", "Chi phí", "Đăng ký thăm" trong khung có menu trái. Theo 26.1 và DESIGN.md mục 2, người thân dùng cổng riêng (M37), thiết kế cho điện thoại trước; nên tách khỏi `AppShell` của nhân viên.
4. **Tổng quan:** đúng dòng "Dashboard, báo cáo" (QL, TT, BS, ĐD, HC, KT); CS và Bếp không có. Màn đầu tiên của CS là Checklist ca, của Bếp là Suất ăn.

## 4. Cần chủ dự án chốt

- **Thiếu dòng ở Phụ lục 27 (báo lại, không tự thêm):**
  - Khám và điều trị (10.2): spec 007 FR-072 ghi Bác sĩ ghi nhận; chưa rõ Điều dưỡng, Trưởng tầng, Quản lý viện xem được không, người thân thấy qua bản đồng ý không.
  - Thời khóa biểu cá nhân (8.2), hồ sơ tinh thần (8.10): spec 005 FR-048 (Trưởng tầng, Điều dưỡng), 014 FR-053 (Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc); chưa rõ Bác sĩ, Quản lý viện.
  - Bản ghi ngoại tuyến chờ xem lại (Q-01): Trưởng tầng xem lại; chưa rõ lệnh xem lại gồm gì và Quản lý viện có làm thay khi tầng không có Trưởng tầng không.
  - Giấy phép hoạt động của cơ sở (10.4): spec 002 FR-039 chỉ nêu cảnh báo Quản lý viện; chưa rõ ai nhập, sửa.

- Gom màn: M07, M08, M09, M10 là màn riêng hay tab trong hồ sơ M06; M11 là màn riêng hay chế độ của M12.
- Phê duyệt (M04) và Thông báo (M02) đặt ở menu hay ở khối người dùng, đầu trang.
- Màn nào có trên app nhân viên (mobile), màn nào chỉ ở web. Theo 26.1, các màn trong ca (M12 → M18, M23) nên có trên app.
- Có sửa `navigation.js` theo mục 3 ngay trong giai đoạn thiết kế không.
