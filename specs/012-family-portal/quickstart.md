# Quickstart: Kiểm chứng feature 012

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md)

Hướng dẫn dùng ở giai đoạn triển khai; giai đoạn thiết kế chưa có mã nên các lệnh chưa chạy được. Hợp đồng xem [contracts/api-012.yaml](contracts/api-012.yaml) và [contracts/module-interfaces.md](contracts/module-interfaces.md); bảng và ràng buộc xem [data-model.md](data-model.md).

## 1. Điều kiện cần

- Như quickstart của 000 → 011: JDK 25, MySQL 8.0 cài trên máy (000 R-14), Node.js LTS, Flutter stable; profile `demo` có đồng hồ giả lập.
- Dữ liệu mẫu của profile `demo`:
  - tài khoản: Hành chính HC; Quản lý viện QL; Trưởng tầng TT (tầng 2); Điều dưỡng DD (phạm vi tầng 2); Nhân viên chăm sóc CS; người thân R (người đại diện, liên hệ chính của A), S (người đại diện thứ hai của A), N (con, chỉ quyền đăng ký thăm), M (có hồ sơ, không có tài khoản);
  - A nội trú tầng 2, phòng 2 giường đang dùng; E bán trú lịch thứ 2, 4, 6; B Đang tiếp nhận chưa có người đại diện;
  - bản đồng ý của A gồm R, không gồm N;
  - CFG theo mặc định Phụ lục 25: CFG-M10-01 Cao 24 giờ / Thường 72 giờ, CFG-M10-02 7 ngày, CFG-M10-03 thứ 2 hằng tuần và 48 giờ, CFG-M10-04 09:00–11:00, 15:00–17:00, 20 người, CFG-M10-05 2 giờ / 14 ngày, CFG-M10-06 3, CFG-M10-07 30 phút, CFG-M10-08 4 giờ, CFG-M10-09 2 giờ, CFG-M10-10 1, CFG-M10-11 48 / 96 giờ, CFG-M10-12 96 giờ;
  - bản giả có công tắc `/api/v1/kiem-thu/gia-lap/{module}`: `HoatDongBanTinPort` (014); bộ phát sự kiện thử của 001, 004, 007, 009.

## 2. Chạy

```text
backend/  ./mvnw verify
backend/  ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
frontend/ npm ci && npm test && npm run dev
mobile/  flutter test
mobile/   flutter test
```

Kết quả mong đợi: `verify` xanh, gồm Modulith `verify()` (không có vòng 012 ↔ 001, 002, 004, 005, 007, 009, 011), test cơ sở dữ liệu (data-model mục 11) và các nhóm test ở mục 4.

## 3. Kịch bản kiểm chứng đầu–cuối (profile demo)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| Q1 | HC lập hồ sơ M không có giấy tờ, rồi lập yêu cầu bật "được phép đón" cho M | 409 `THIEU_GIAY_TO` | US1, FR-007 |
| Q2 | HC kết thúc quan hệ của R khi R là liên hệ chính duy nhất | 409 `VI_PHAM_DBR_02` | US1, DBR-02 |
| Q3 | B (Đang tiếp nhận) chưa có người đại diện; Hoàn tất tiếp nhận | Lưu quan hệ được; Hoàn tất tiếp nhận bị chặn điều kiện `DBR_02` | FR-003 |
| Q4 | HC đặt người đại diện đầu tiên cho B (bằng chứng); đặt thêm người thứ hai | Đầu tiên áp ngay; thứ hai tạo yêu cầu Chờ xác nhận; người được đề nghị xác nhận → 403 | US1, FR-005, Q-127 |
| Q5 | Bật "xem sức khỏe" cho N | Chặn `KHONG_CO_BAN_DONG_Y`; sau khi 001 có bản đồng ý gồm N → được | US1, FR-011 |
| Q6 | Rút bản đồng ý gồm R | Quyền R giữ "bật", hiển thị "không có tác dụng"; cổng phần sức khỏe 403 | FR-011, SC-002 |
| Q7 | HC ghi phiếu đăng ký người thân (bản scan) với quyền của R, N | Mỗi quyền một yêu cầu Hiệu lực cách "bản ký" | US1, FR-008, Q-126 |
| Q8 | N qua cổng lập yêu cầu bỏ chính mình khỏi danh sách đón; N lập yêu cầu bật quyền cho M | Bỏ áp ngay (≤ 1 phút); bật cho người khác 403 `KHONG_DUOC_LAP` | US3, FR-014, SC-003 |
| Q9 | HC lập yêu cầu bật "xem chi phí" cho N; R xác nhận qua cổng | Hiệu lực; S nhận thông báo Nhẹ, R và HC không nhận bản Q-249 | US3, FR-016, Q-267 |
| Q10 | Lập yêu cầu thứ hai trùng (N, xem chi phí) khi còn chờ | 409 `TRUNG_YEU_CAU_CHO` | FR-018 |
| Q11 | Yêu cầu chờ 48 giờ; 96 giờ | Nhắc R, S, QL; báo QL; không tự Từ chối | FR-019 |
| Q12 | Yêu cầu bỏ người đón M trong lúc có yêu cầu bật M đang chờ | Yêu cầu bật tự Hủy "được thay bằng yêu cầu bỏ"; bản ghi đón chưa dùng của M → Không dùng | FR-017, FR-018 |
| Q13 | TT ghi đón A (Tạm vắng) bởi R, giấy tờ khớp; 004 Cho tạm vắng với `banGhiDonId` | Bản ghi đón; 004 gọi `daDung` trong lệnh; gọi lại lần hai → `BAN_GHI_DON_DA_DUNG` | US2, FR-026 |
| Q14 | Ghi đón bởi M (ngoài danh sách) | 409 `CHAN_DON`, lần chặn đón ghi lại, TT nhận Nhẹ | US2, FR-023 |
| Q15 | Lập ngoại lệ đón cho M; R xác nhận; ghi đón; ghi đón lần hai | Ngoại lệ Hiệu lực → Đã dùng; lần hai chặn | US2, FR-024 |
| Q16 | Ngoại lệ Hiệu lực quá 4 giờ | Hết hạn | FR-024 |
| Q17 | Bản ghi đón chưa dùng quá 2 giờ | Không dùng; lệnh nguồn dùng nó bị chặn | FR-027 |
| Q18 | E có dấu "được tự về" bật; CS ghi đón loại Tự về; 005 điểm danh về | Bản ghi không người đón, nhân viên tiễn; 005 gọi `daDung` | US2, Q-125 |
| Q19 | CS ghi đón loại Viện đưa về cho E tại nơi bàn giao | Bản ghi đón và điểm danh về của 005 trong cùng lệnh | US2, Q-295 |
| Q20 | Ghi đón khi mất mạng (app) | App không cho lưu tạm | FR-028 |
| Q21 | N đăng ký thăm A 09:00–11:00 ngày mai với 2 người đi cùng | Đã duyệt ngay, giữ 3 chỗ | US4, FR-031 |
| Q22 | Đăng ký 12:00–13:00; trước 1 giờ; 20 đăng ký đồng thời cho chỗ cuối | 422 lý do (a), (e); đúng một thành công | US4, SC-004 |
| Q23 | Đăng ký khi phòng A đang khoanh vùng; khi A Tạm vắng trùng ngày | 422 lý do (c), (d) | US4, FR-030 |
| Q24 | 007 khoanh vùng tầng 2 khi có lượt Đã duyệt và một lượt Đã vào | Lượt Đã duyệt Hủy (người đăng ký nhận Trung bình); danh sách cho HC, TT; HC được báo khách đang vào | US4, FR-037 |
| Q25 | A chuyển Tạm vắng trùng ngày có lượt thăm | Lượt Hủy, người đăng ký nhận Nhẹ | FR-036 |
| Q26 | HC ghi vào với 1 người không có trong danh sách | Phải thêm tên trước khi vào; tổng ≤ số đã đăng ký | FR-033 |
| Q27 | Hết khung chưa vào; đã vào quá 11:30 chưa ra | Không đến; HC nhận nhắc một lần | FR-034 |
| Q28 | 007 lấy lượt thăm của A trong khoảng | Người đăng ký và người đi cùng thực tế, giờ vào/ra | FR-038, SC-009 |
| Q29 | HC đăng ký R ở lại 3 đêm có ăn; TT xác nhận không ghi ý kiến người cùng phòng | 409 `THIEU_Y_KIEN_CUNG_PHONG`; ghi ý kiến → Đã xác nhận | US8, FR-040 |
| Q30 | Đăng ký lượt thứ hai chồng thời gian cho A | 409 `CHONG_LUOT_O_LAI` | US8, Q-128 |
| Q31 | Bắt đầu 18:00 01/11; qua 00:00 hai lần; kết thúc 07:00 03/11 | 010 nhận hai `SuKienTinhPhiNguon(DEM_O_LAI)`; 011 nhận `LuotOLaiDaDoi`; không có khoản tự tạo | US8, FR-043, SC-008 |
| Q32 | Kết thúc ghi giờ thực tế 23:00 02/11 sau khi đêm 02/11 đã phát | `SuKienTinhPhiNguonDaDoi(daHuy)` cho đêm 02/11 | FR-043 |
| Q33 | A Tạm vắng khi R đang ở lại | HC, TT nhận nhắc Trung bình; lượt không tự kết thúc | FR-044 |
| Q34 | N mở cổng: phần chung, phần sức khỏe, hợp đồng | Chung có thực đơn chung; sức khỏe 403; hợp đồng 403 | US5, FR-046 |
| Q35 | R mở phần thuốc, chỉ số | Hiển thị; mỗi lần một dòng nhật ký xem; QL tra cứu được, R không | US5, FR-048 |
| Q36 | R mở bản tin có phần sức khỏe (009 phát `ThongBaoDaXem`) | Một dòng nhật ký nguồn `BAN_TIN` | FR-048 |
| Q37 | Thứ 2 (mốc kỳ) | DD nhận nhắc lập cho A; không có bản tin tự tạo; người vắng cả kỳ không được nhắc | US7, FR-051, SC-006 |
| Q38 | DD lập bản tin A; lập lần hai cùng kỳ | Chờ duyệt, số liệu tính sẵn theo loại; lần hai 409 `DA_CO_BAN_TIN` | US7 |
| Q39 | Trong kỳ có sự cố mức Trung bình; DD duyệt không giải thích | 409 `THIEU_GIAI_THICH_SU_CO`; có giải thích → Đã gửi; 009 gửi theo quyền từng người | US7, BR-M10-09, SC-007 |
| Q40 | 48 giờ chưa duyệt; 96 giờ; TT duyệt thay | TT nhận nhắc, rồi báo có thể duyệt thay; bản tin dấu "duyệt thay" | US7, Q-124 |
| Q41 | Tới mốc kỳ sau khi bản tin còn Chờ duyệt | Không gửi; QL nhận Nhẹ | FR-057, FR-073 |
| Q42 | 007 hủy ghi nhận sự cố đã nêu trong bản tin | HC và người duyệt được báo; không tự gửi đính chính; DD lập đính chính | FR-056 |
| Q43 | N gửi khiếu nại nhóm chăm sóc | Mới, mức Cao, hạn 24 giờ, phụ trách TT | US6, FR-059 |
| Q44 | Quá hạn chưa trả lời | Dấu quá hạn, QL nhận Trung bình một lần (≤ 5 phút) | US6, BR-M10-05, SC-005 |
| Q45 | TT trả lời phản hồi nhóm sức khỏe gửi bởi N (không có quyền sức khỏe) | Cảnh báo lúc soạn; N chỉ thấy phần chung | FR-058 |
| Q46 | N mở lại; sau 7 ngày không xác nhận | Hạn mới; tự đóng sau CFG-M10-02 | FR-061, FR-064 |
| Q47 | A Qua đời | Lượt thăm tương lai Hủy; bản tin Chờ duyệt Không gửi; yêu cầu, ngoại lệ chờ Hủy; phản hồi tiếp tục | FR-075 |
| Q48 | CS gọi lệnh lượt ở lại; HC gọi nhật ký xem | 403 | FR-067 |
| Q49 | Khởi động lại sau khi tắt qua 00:00 và mốc kỳ | Đêm ở lại và nhắc bản tin chạy bù đúng một lần | NFR-04 |
| Q50 | D ghi mong muốn của A "không muốn X đón", nguồn `NGUOI_CAO_TUOI`; X đang có `duoc_phep_don` | 201; quyền X không đổi; HC, TT thấy `quyenTraiMongMuon` | US3 #9, FR-020a, FR-020b (bổ sung 2026-10-18) |
| Q51 | R (người đại diện) gửi qua cổng yêu cầu thêm X vào danh sách đón; R xác nhận; QL duyệt thiếu `traoDoiVoiNguoiCaoTuoi`; đủ | `CHO_XAC_NHAN`; 409 `CAN_QUAN_LY_DUYET_TRAI_MONG_MUON`; 422; `HIEU_LUC` | US3 #10, FR-020b |
| Q52 | Lập yêu cầu bỏ X khỏi danh sách đón; A đổi ý, D ghi mong muốn mới | Áp ngay; mong muốn cũ `DUOC_THAY_THE`, vẫn xem được | US3 #11, FR-017, FR-020a |
| Q53 | Phiếu đăng ký có hai dòng: Y đăng ký thăm (không trái), X được phép đón (trái) | Dòng Y Hiệu lực; dòng X thành yêu cầu Chờ xác nhận | Edge Case, FR-008, FR-020b |
| Q54 | Bản đồng ý A có `dongYThongBao = false`; duyệt bản tin | Yêu cầu gửi khai `VE_NGUOI_CAO_TUOI`; 009 không gửi ai; bản tin `DA_GUI`, người thân vẫn xem trên cổng | FR-054, Q-353 |
| Q55 | N (không quyền xem sức khỏe) mở cổng khi A có sự cố ngã Trung bình | Thấy phần chung (loại, thời điểm, mức, đã xử lý); không thấy phần sức khỏe; không có dòng nhật ký xem | FR-046, Q-352 |

## 4. Nhóm test tự động

| Nhóm | Test | Kịch bản |
| --- | --- | --- |
| Hàm thuần | `QuyenCoTacDungTest`, `KiemTraDbr02Test`, `KiemTraYeuCauTest`, `KiemTraDangKyThamTest`, `SoDemTest`, `HanPhanHoiTest`, `NguoiDuyetBanTinTest` | Q1 → Q6, Q22, Q31, Q43 |
| Quan hệ, quyền | `DBR_02_LienHeChinhIT`, `BR_M10_07_YeuCauQuyenIT`, `FR_008_PhieuDangKyIT`, `Q249_ThongBaoDaiDienKhacIT` | Q1 → Q12 |
| Đón | `BR_M10_03_QuyTrinhDonIT`, `FR_024_NgoaiLeDonIT`, `FR_027_BanGhiDonHetHieuLucIT`, `Q295_VienDuaVeIT` | Q13 → Q20 |
| Thăm | `BR_M10_02_DangKyThamIT`, `FR_037_KhoanhVungHuyThamIT`, `SC_004_SucChuaDongThoiIT` | Q21 → Q28 |
| Ở lại | `BR_M10_04_DemOLaiIT`, `FR_040_GioiHanOLaiIT` | Q29 → Q33 |
| Cổng | `BR_M10_01_CongTheoQuyenIT`, `FR_048_NhatKyXemIT`, `SC_002_SucKhoeTest` | Q34 → Q36 |
| Bản tin | `BR_M10_08_BanTinIT`, `BR_M10_09_GiaiThichSuCoIT`, `FR_056_DinhChinhIT` | Q37 → Q42 |
| Phản hồi | `BR_M10_05_PhanHoiIT` | Q43 → Q46 |
| Trạng thái cuối | `FR_075_TrangThaiCuoiIT` | Q47 |
| Đồng thời | 20 đăng ký chỗ cuối; hai người đại diện xác nhận cùng lúc; bỏ người đón cùng lúc với ghi đón | Q8, Q22 |
| Bộ lập lịch | `ChayBuM10IT` | Q49 |
| Kiến trúc | `ApplicationModules.verify()`; ArchUnit: gói `..lich..` không lập bản tin, không lập khoản (Q-306); DTO người thân không có trường nhật ký | |
| Web (Vitest) | Cổng ẩn phần không có quyền; đăng ký thăm hiển thị mọi lý do từ chối; soạn bản tin không có nội dung soạn sẵn | Q22, Q34, Q38 |
| App (`flutter_test`) | App người thân: đăng ký thăm ≤ 2 phút (SC-004), xác nhận yêu cầu; app nhân viên: ghi đón chỉ trực tuyến | Q8, Q20, Q21 |
| Kênh người cao tuổi *(bổ sung 2026-10-18)* | `FR_083_KenhNguoiCaoTuoiIT`, `FR_084_KenhKhongLoIT`, `FR_058a_PhanHoiNguoiCaoTuoiIT`, `BR_M10_12_MongMuonIT`, `FR_086_NguoiThanKhongThayIT`; app `hom_nay_test.dart`, `nut_sos_test.dart` | K1 → K9 (mục bổ sung bên dưới) |

## 5. Tiêu chí hoàn tất (đối chiếu SC)

| SC | Cách đo |
| --- | --- |
| SC-001 | Bộ 100 lượt đón dựng sẵn (Q13 → Q20) |
| SC-002 | Q6, Q34 → Q36; quét mọi đường dẫn `/cong/**/suc-khoe` có dòng nhật ký |
| SC-003 | Q8, Q9 |
| SC-004 | Q22, `SC_004_SucChuaDongThoiIT`; đo thao tác đăng ký trên 10 người thân dùng lần đầu |
| SC-005 | Q44; số liệu đáp ứng hạn sau ba tháng từ `NguoiThanThongKeApi.phanHoi` |
| SC-006 | Q37 → Q39 |
| SC-007 | Q39 và quét bản tin, thông báo gửi người thân theo quyền |
| SC-008 | Bộ 30 ngày ở lại so tính tay (Q31, Q32) |
| SC-009 | Q28 ≤ 1 phút |
| SC-010 | Kiểm bất biến DBR-02 sau mọi kịch bản |
| SC-011 *(bổ sung 2026-10-18)* | K1, K2, K5; quét mọi đường dẫn `/kenh-nguoi-cao-tuoi/**` không có trường thuộc danh mục `TRUONG_SUC_KHOE`, chi phí, số dư |

## Kịch bản bổ sung 2026-10-18, lượt 3 (Q-365 → Q-371)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| K1 | A (AC-16) gọi lần lượt `/kenh-nguoi-cao-tuoi/hom-nay`, `thuc-don`, `hoat-dong`, `luot-tham`, `do-gui` | Đủ năm phần của A; không có trường sức khỏe, chi phí, số dư, hợp đồng | US9 #1, FR-083, FR-084 |
| K2 | A gọi `/cong/nguoi-cao-tuoi/{B}/chung` và `/phan-hoi?nct=B` | 403 | US9 #2, FR-084 |
| K3 | A gửi Mong muốn "không muốn gặp ông X" chủ đề `KHACH_THAM_NGUOI_DON`; H trả lời ngay; H ghi `mong_muon_nct`, `gan-y-kien`, trả lời | Mới, Thường, phụ trách Hành chính; 409 `CHUA_GAN_Y_KIEN`; Đã phản hồi, A nhận Nhẹ | US9 #3, FR-058b |
| K4 | A gửi Mong muốn "mấy giờ ăn tối"; T `doi-loai` không lý do; có lý do | 422; loại Hỏi đáp, mức và hạn giữ | US9 #4, FR-066 |
| K5 | R (đại diện của A) gọi `GET /phan-hoi` | Không có phản hồi nào của A | US9 #5, FR-086 |
| K6 | A `dong-y-dong` phản hồi Đã phản hồi | Đóng, `cach_dong = NGUOI_GUI` | US9 #6 |
| K7 | D ghi thay Góp ý cho E (không có tài khoản); T trả lời; D `dong-y-dong` thay | Người gửi E, nguồn `GHI_TRUC_TIEP`; Đóng, `cach_dong = GHI_THAY` | US9 #7, FR-058a |
| K8 | A gửi Hỏi đáp nhóm `SUC_KHOE_THUOC`; T soạn trả lời | T thấy nhắc trả lời trực tiếp; A thấy trả lời chỉ phần chung | US9 #8, FR-058a |
| K9 | App: tài khoản A Đã khóa; mở app trên máy đã từng gắn, bấm SOS | Không gửi được; màn hiện số liên hệ đã lưu và 115 | US9 #9, FR-085 |

## Kịch bản bổ sung 2026-10-22 (Q-373, Q-374, Q-377 → Q-379)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| K10 | A bấm SOS (đã cho quyền vị trí); D tiếp nhận; D ghi đã tới nơi | App gửi vị trí theo chu kỳ; màn hiện "D đang tới" rồi "D đã tới" và dừng gửi | FR-085 |
| K11 | A từ chối quyền vị trí rồi bấm SOS | Cảnh báo vẫn tạo; không gửi vị trí; không hỏi lại trong phiên | FR-085, 007 FR-047h |
| K12 | Kế toán phát hành phiếu báo phí tháng 10 của A; R (có quyền xem chi phí), N (không) mở cổng | R thấy phiếu, tải PDF, Excel, bảng quyết toán hiện hành, sao kê; N không thấy mục chứng từ, gọi trực tiếp 403 | FR-046 (3) |

Test: `nut_sos_test.dart` mở rộng (K10, K11, bản giả API); K12 thuộc test của 017 (`CongChungTuIT`) và `CongChungTu.test.jsx` (web).
