# Quickstart: Kiểm chứng feature 005

**Ngày**: 2026-10-09 | **Plan**: [plan.md](plan.md)

Hướng dẫn dùng ở giai đoạn triển khai; giai đoạn thiết kế chưa có mã nên các lệnh chưa chạy được. Hợp đồng xem [contracts/api-005.yaml](contracts/api-005.yaml) và [contracts/module-interfaces.md](contracts/module-interfaces.md); bảng và ràng buộc xem [data-model.md](data-model.md).

## 1. Điều kiện cần

- Như quickstart của 000 → 004: JDK 25, MySQL 8.0 cài trên máy (000 R-14), Node.js LTS, Flutter stable; profile `demo` có đồng hồ giả lập.
- Dữ liệu mẫu của profile `demo`:
  - tài khoản và cơ cấu của quickstart 002, 003: Quản lý viện QL, Trưởng tầng H (tầng 2), Bác sĩ BS, Điều dưỡng D và M (tầng 2), Nhân viên chăm sóc S, S2, T, Hành chính HC; P giữ nhiệm vụ Người phụ trách ca tầng 2 (không phải Trưởng tầng);
  - ca ngày 07:00–18:00, ca đêm 18:00–07:00 của tầng 2 và khu bán trú (bản giả `NhanSuCaPort`); phân công: S phụ trách A, B, C; D là điều dưỡng tầng 2;
  - người cao tuổi: A, B, C, F nội trú Đang lưu trú tầng 2; E bán trú lịch 07:30–16:30 (004);
  - danh mục loại công việc mẫu: xoay trở (Bắt buộc), hỗ trợ uống nước (ml, Quan trọng, giới hạn 500), hỗ trợ ăn, thay tã (có tính phí), đo huyết áp, vận động, tâm trạng, kiểm tra đầu ca, giám sát;
  - CFG theo mặc định Phụ lục 25: CFG-M04-01 1 giờ, CFG-M04-02 ±15 phút, CFG-M04-03 16:00 / 60%, CFG-M04-04 3, CFG-M04-05 3, CFG-M04-06 2 giờ, CFG-M04-12 30 phút, CFG-M02-07 2 giờ, CFG-M09-04 30 phút, CFG-M15-07 2 giờ, CFG-M15-08 24 giờ;
  - bản giả T002: test dùng `@Import(FakeM04Config.class)` với profile `test` hoặc `demo`, rồi tiêm từng fake để đặt fixture và gọi `reset()` sau kịch bản. Công tắc đã duyệt: `PUT /api/v1/testing/fakes/{module}`, body `{"enabled": false}` để tắt, `true` để bật, trả 204; module `006`, `007`, `008`, `011`, `012`, `014`. Tắt nguồn gây lỗi rõ ràng, bật lại giữ fixture. Xem ánh xạ tám SPI ở glossary mục 11. Cấu hình/fake/controller đều ở test classpath, không được tự nạp vào context khác; `spring-boot:run` thông thường và JAR production không có chúng. Khi chạy demo có fake phải chủ động dùng test classpath và import cấu hình. Bộ ghi sự kiện tới 007, 009, 010, 011 chưa thuộc T002, sẽ nối khi cài các story phát sự kiện.

## 2. Chạy

```text
backend/  ./mvnw verify
backend/  ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
frontend/ npm ci && npm test && npm run dev
mobile/   flutter test
```

Kết quả mong đợi: `verify` xanh, gồm Modulith `verify()` (không có vòng 005 ↔ 008, 005 ↔ 007), test cơ sở dữ liệu (data-model mục 9), test đồng thời và các nhóm test ở mục 4.

## 3. Kịch bản kiểm chứng đầu–cuối (profile demo)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| Q1 | BS xác nhận đánh giá đầu vào của A (001) | Không có phiên bản kế hoạch nào được tạo | US1 #1, Q-280 |
| Q2 | D lập Nháp cho A: "xoay trở mỗi 2 giờ từ 06:00, Bắt buộc", "uống nước 3 giờ/lần, 1.500 ml/ngày, Quan trọng", một mục không khung; gửi duyệt ngày hiệu lực ngày mai | Mục không khung nhận ±15 phút; `CHO_DUYET`; BS nhận thông báo | US1 #1, #2 |
| Q3 | BS duyệt với ngày hiệu lực hôm nay; D lập Nháp thứ hai khi đã có Chờ duyệt | 409 `NGAY_HIEU_LUC_KHONG_HOP_LE`; 409 `DA_CO_PHIEN_BAN_MO` | US1 #7, FR-004 |
| Q4 | BS duyệt (ngày mai); tua tới 00:00 | `CHO_HIEU_LUC` rồi `HIEU_LUC`; phiên bản cũ `HET_HIEU_LUC` đến hôm qua; H, P nhận `KeHoachDaDoi`; không công việc nào bị đổi | US1 #3, US2 #5, SC-003 |
| Q5 | Sửa mục của phiên bản Hiệu lực (API và `UPDATE` trực tiếp) | 409 `PHIEN_BAN_DA_KHOA`; trigger chặn | US1 #5, SC-004 |
| Q6 | M (không có quyền duyệt) duyệt; QL gán quyền cho D, D tự duyệt không lý do, có lý do | 403; 422; 200, nhật ký "tự duyệt" | US1 #6 |
| Q7 | Trả lại không lý do; thu hồi trước và đúng ngày hiệu lực | 422; 200 / 409 | US1 #4, #8 |
| Q8 | 06:00, P lập lô cho ca ngày: xoay trở A 08:00 → 16:00, giao S | 201; trạng thái `CHUA_DEN_HAN`, khung ±15, Bắt buộc | US2 #1, BR-M04-01 |
| Q9 | P lập lô có: xoay trở 09:00 (không thuộc chuỗi), mức Thường, lập trùng 08:00, giao T (không phân công A), "uống thuốc", lập lúc 05:30 | 409 `loiTheoDong` đủ các mã `KHONG_KHOP_MUC_KE_HOACH`, `TRUNG_CAN_CU`, `NGUOI_THUC_HIEN_KHONG_HOP_LE`, `LIEU_THUOC_KHONG_LA_CONG_VIEC`; lô trước 06:00 bị chặn với P nhưng được với H | US2 #1, #2, #6, FR-017 |
| Q10 | Tua 07:00 khi khu bán trú chưa có công việc | P và H nhận `CaChuaCoCongViec` đúng một lần | US2 #1, FR-017 |
| Q11 | B Cho tạm vắng (001); P lập công việc cho B; Ghi nhận trở về 14:20 | 409 `NCT_VANG_MAT`; P nhận `CanLapCongViec`; không công việc nào tự tạo | US2 #3, FR-020 |
| Q12 | C có công việc 15:00, 17:00; chuyển viện 14:00 | Hai công việc `HUY`, lý do vắng mặt, người đóng Hệ thống | US2 #4, BR-M04-04 |
| Q13 | Bản giả 014 hủy đăng ký buổi thể dục của A; 011 hủy bữa trưa | Công việc Chưa đến hạn gắn nguồn `HUY` | US2 #7, FR-022 |
| Q14 | A qua đời 09:10 khi có công việc Đến hạn 09:00 và Bắt buộc Quá hạn 08:00 kèm cảnh báo | Cùng lệnh: Chưa đến hạn `HUY`; hai công việc `KHONG_THUC_HIEN` do Hệ thống; `YeuCauDongCanhBao(TRANG_THAI_CUOI)`; không có trong `CongViecBanGiaoApi.chuaDong` | US2 #8, FR-010, SC-012 |
| Q15 | Lập công việc cho E chưa điểm danh; S điểm danh đến 08:10; lập 08:00 và 08:30 | 409 `BAN_TRU_CHUA_CO_MAT`; `CO_MAT`, P khu bán trú nhận `DaDiemDanhDen` trong 1 phút; 08:00 bị chặn, 08:30 được | US3 #1, #2, SC-009 |
| Q16 | E điểm danh về 15:00 với bản ghi đón hợp lệ; với bản ghi không hợp lệ | `DA_VE`, công việc 15:30 `HUY` (đã về), công việc Đến hạn giữ; 409 | US3 #3, FR-019 |
| Q17 | Ngày khác: E chưa đến, tua quá 09:30; E đến 10:00 | `VANG_KHONG_BAO`, H được báo; điểm danh đến được, P được báo | US3 #4 |
| Q18 | 004 phát báo vắng đủ hạn cho E; lập công việc ngày đó | `VANG_CO_BAO`; 409 | US3 #5 |
| Q19 | S mở checklist ca ngày; D mở checklist | S: công việc A, B, C, công việc chung phù hợp, lưu ý chăm sóc; D: thêm liều thuốc, bàn giao, cảnh báo, kiểm tra đầu ca | US4 #1, #2, SC-008 |
| Q20 | S ghi 200 ml trong khung; bữa trưa B giá trị "ít"; thay tã C số lượng 2 | `HOAN_THANH`; 422; `HOAN_THANH`, 010 nhận `KetQuaCoTinhPhi` | US4 #3 → #5 |
| Q21 | Ghi huyết áp: bản giả 007 trả `DA_LUU`, `BI_CHAN`, `CHO_DO_LAI` | Đóng, trỏ bản ghi chỉ số; 422, giữ trạng thái; 202, đóng khi 007 gọi `daXacNhan` | US4 #6, FR-041 |
| Q22 | T ghi công việc của A; P ghi công việc Đến hạn của S; P ghi sau khi Quá hạn | 403; 403; 200, `canCuQuyen = NHIEM_VU_PHU_TRACH_CA` | US4 #7, SC-011 |
| Q23 | S ghi lúc 11:30 với thời điểm thực hiện 09:00; thời điểm sau lúc ghi | Nhãn ghi muộn; 422 | US4 #8, #11, BR-M04-12 |
| Q24 | Không thực hiện không có `loaiLyDoId`; với loại "Người cao tuổi từ chối" và ghi chú "bà muốn nghỉ sau bữa trưa"; Ghi nhận phát sinh 150 ml lúc 13:10 | 422; 200 `KHONG_THUC_HIEN` với loại lý do và ghi chú; công việc phát sinh `HOAN_THANH`, tính vào tổng nước | US4 #10, #11, FR-028a (sửa 2026-10-18) |
| Q25 | Ba công việc Thường, Quan trọng, Bắt buộc 10:00; tua 10:15, 10:45 | 10:15: cả ba Quá hạn, nhắc S một lần; H, P nhận việc Quan trọng; 007 nhận `YeuCauCanhBao` Trung bình cho việc Bắt buộc; 10:45: P nhận việc Thường; đóng việc Thường lúc 10:30 thì không báo | US5 #1 → #3, SC-005 |
| Q26 | P phân lại việc Quá hạn tầng 2 cho S2; việc tầng 3 | 200, căn cứ nhiệm vụ; 403 | US5 #4 |
| Q27 | 17:30 `CongViecBanGiaoApi.chuaDong`; bản giả 008 gọi `khiXacNhanBanGiao` lúc 18:10 | Danh sách đủ trường, không lập sẵn mục; việc Thường Quá hạn → `KHONG_THUC_HIEN` (hệ thống đóng sau bàn giao); việc khác sang ca đêm với dấu tồn | US5 #5, #6, #10, SC-006 |
| Q28 | Thiết bị S ngoại tuyến: ghi 10:05 cho việc Bắt buộc 10:00; đồng bộ 10:40 | `CHAP_NHAN`, `HOAN_THANH` (không trễ), 007 nhận yêu cầu đóng cảnh báo "đã thực hiện đúng khung" | US5 #8, FR-035 |
| Q29 | Hai lệnh ghi cùng một công việc gần đồng thời | Một 200, một 409 `CONG_VIEC_DA_DONG` kèm người đóng và nội dung | Edge Case, FR-036 |
| Q30 | A mục tiêu 1.500 ml, tổng 800 ml; tua 16:00; A 1.000 ml | 007 nhận `YeuCauCanhBao(UONG_NUOC_THIEU)`, không công việc tự tạo; không cảnh báo | US6 #1, #2 |
| Q31 | E bán trú mục tiêu 1.200, đến 08:00, uống 300 rồi 400 ml | Cảnh báo với 300 (ngưỡng 360), không với 400 | US6 #3 |
| Q32 | B ba bữa "Một phần", "Bỏ bữa", "Không ăn" qua hai ngày; một bữa vắng xen giữa; bữa "Phần lớn" | 007 nhận cảnh báo, 011 nhận `AnKemKeoDai`; bữa vắng không làm đứt chuỗi; chuỗi về 0 | US6 #5 → #8 |
| Q33 | C tâm trạng tiêu cực 3 ngày; ngày 2 không ghi | Cảnh báo nhẹ không kèm hoạt động; ngày trống làm đứt chuỗi | US6 #9, FR-042 |
| Q34 | Đính chính 2.000 → 200 ml (người ghi gốc); T đính chính; Hủy ghi nhận bữa thứ ba | Bản gốc còn xem được; 403; công việc giữ Hoàn thành, chuỗi còn 2, cảnh báo không rút | US7 #1 → #6 |
| Q35 | Bản giả 001 phát `DanhGiaDaDoiMucHoacNhuCau` cho A; đánh giá chỉ đổi lưu ý | D và người có quyền duyệt nhận `CanXemXetKeHoach`, không có bản ghi yêu cầu; không thông báo | US8 #1 → #4, SC-010 |
| Q36 | Bản giả 008 gọi `khiVangCa(S)`; H phân công việc chung cho điều dưỡng có giấy phép hết hạn; S2 nhận việc | Công việc chưa đóng của S thành chung, H được báo; 409 `THIEU_CHUNG_CHI`; S2 nhận được | US9 #1 → #3 |
| Q37 | 003 phát `DaChuyenGiuong` A sang tầng 3; D xem thời khóa biểu A | Công việc chưa đóng gán theo tầng 3; thời khóa biểu đủ nguồn, không có thao tác sửa | US9 #4, #5 |
| Q38 | Dừng backend 3 giờ (đồng hồ giả lập) rồi khởi động | Chuyển Đến hạn, Quá hạn bù với thời điểm thực tế; nhắc gộp một lần mỗi người; mốc nước chỉ chạy nếu còn trong ngày | FR-052, BR-M04-26 |

## 4. Test tự động: truy vết kịch bản → test

| Nhóm | Test | Kịch bản |
| --- | --- | --- |
| Hàm thuần | `BR_M04_25_ChuoiMoiNGioTest`, `KiemTraLapCongViecTest`, `BangChuyenCongViecTest`, `XacDinhTrangThaiDongTest`, `BR_M04_08_NuocUongTest`, `BR_M04_09_ChuoiBuaKemTest`, `BR_M04_11_TamTrangTest`, `SoSanhPhienBanTest` | Q8, Q9, Q23, Q30 → Q33 |
| Kế hoạch | `FR_003_VongDoiKeHoachIT`, `BR_M04_19_PhienBanKhoaIT`, `DBR_11_MotPhienBanHieuLucIT`, `FR_005_TuDuyetIT`, `BR_M04_20_BaoXemXetKeHoachIT` | Q1 → Q7, Q35 |
| Lập công việc | `BR_M04_01_LapCongViecCaIT`, `DBR_12_KhongTrungCanCuIT`, `FR_021_LapLaiSauHuyVangIT`, `BR_M04_24_KiemTraDauCaIT`, `BR_M04_26_LapMuonVaChayBuIT`, `FR_006_KhongTrungKhungPhienBanCuIT` | Q8 → Q10, Q38 |
| Hủy tự động | `BR_M04_04_HuyKhiVangIT`, `FR_010_TrangThaiCuoiIT`, `FR_022_NguonBiHuyIT`, `FR_020a_DinhChinhChuyenDiIT` | Q11 → Q14 |
| Bán trú | `BR_M04_02_BanTruIT`, `FR_013_BangCoMatIT`, `FR_018a_GioVeIT`, `FR_019_HetNgayConCoMatIT` | Q15 → Q18 |
| Ghi nhận | `BR_M04_12_QuyenGhiNhanIT`, `BR_M04_10_DoChiSoIT`, `FR_034_KetQuaTheoLoaiIT`, `FR_040a_XacNhanPhucVuIT`, `FR_025_GhiNhanPhatSinhIT`, `FR_036_GhiDongThoiIT`, `FR_051_SuKienTinhPhiIT` | Q19 → Q24, Q29 |
| Quá hạn | `BR_M04_05_QuaHanNhacIT`, `BR_M04_06_LeoThangTheoMucIT`, `BR_M04_29_NhacMotLanIT`, `BR_M04_07_DanhSachChoBanGiaoIT`, `FR_047_TuDongDongSauBanGiaoIT`, `FR_047a_TonChoXacNhanIT` | Q25 → Q27 |
| Ngoại tuyến | `BR_M04_12_GhiMuonNgoaiTuyenIT`, `BR_M04_27_DongBoCongViecDaDongIT`, `FR_035_TrangThaiCuoiNgoaiTuyenIT` | Q28 |
| Quy tắc F | `NuocUongIT`, `ChuoiBuaKemIT`, `US7_6_HuyGhiNhanChuoiIT` | Q30 → Q34 |
| Đính chính | `BR_M04_14_DinhChinhIT`, `US7_4_DinhChinhCoTinhPhiIT` | Q34 |
| Công việc chung | `BR_M04_13_CongViecChungIT`, `BR_M04_28_GanLaiIT`, `BR_M09_01_ChungChiCongViecIT` | Q36, Q37 |
| Cơ sở dữ liệu | `MucKeHoachTriggerIT`, `CongViecTrangThaiCuoiTriggerIT`, `NhomBaTriggerIT` (000) mở rộng | Q5 |
| Kiến trúc | `ApplicationModules.verify()`, ArchUnit cấm gọi API của 008, 007 trực tiếp từ `m04` | |
| Web (Vitest) | Màn lập công việc hiện lỗi theo dòng và không có nút chép sẵn; checklist sắp Quá hạn lên đầu | Q9, Q19 |
| App (`flutter_test`) | `ChecklistNgoaiTuyenTest` (dấu "chưa cập nhật"), `GhiNhanTheoLoaiTest`, `KetQuaDongBoTest` (BR-M15-20), `BanGhiBiTuChoiTest` (chuyển thành đính chính hoặc Ghi nhận phát sinh) | Q28, Q29 |

## 5. Tiêu chí hoàn tất (đối chiếu SC)

| SC | Cách đo |
| --- | --- |
| SC-001 | `SC_001_BayNgayIT`: 20 người × 7 ngày giả lập, đếm lệnh vi phạm bị chặn (100%), công việc do Hệ thống tạo (0), ca không có công việc có nhắc (100%) |
| SC-002 | Cùng test: truy vấn công việc có thời điểm trong khoảng vắng hoặc ngoài khoảng có mặt bán trú = 0 |
| SC-003 | Q4: mỗi lần đổi phiên bản có `KeHoachDaDoi`; công việc Hủy do đổi kế hoạch = 0 |
| SC-004 | Q5 và test trigger; 100% phiên bản Hiệu lực có `nguoi_duyet_id` có quyền tại `duyet_luc` |
| SC-005 | Q25 với dữ liệu quy mô SC-008: chênh giữa `cuoi_khung` và lần nhắc ≤ 1 phút |
| SC-006 | Q27: số công việc chưa đóng trước và sau xác nhận khớp; không công việc nào mất mà không có bản ghi đóng |
| SC-007 | 30 kịch bản bảng của ba hàm thuần và `NuocUongIT`, `ChuoiBuaKemIT` |
| SC-008 | Đo `GET /checklist` (15 người, dữ liệu 100 người) ≤ 3 giây p95; ghi nhận ≤ 2 giây phản hồi (NFR-02); thao tác 15 giây đo bằng kịch bản tay trên app |
| SC-009 | Q15: chênh giữa lệnh điểm danh và sự kiện ≤ 1 phút |
| SC-010 | Q3, Q35 |
| SC-011 | Q22, Q23 |
| SC-012 | Q14 |

## 6. Kịch bản bổ sung 2026-10-18 (Q-347, Q-349, Q-350)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| Q39 | D thêm vào Nháp của A thỏa thuận: A chọn uống trà thường mỗi chiều, rủi ro sặc đã trao đổi, người tham gia A, con gái A, BS; Bác sĩ nhận định A tự quyết được (có cách hỗ trợ, ngày xem lại); hai ý kiến nguồn `NGUOI_CAO_TUOI` và `NGUOI_THAN` con gái; người quyết định A; ngày xem lại sau 30 ngày | 200; gửi duyệt, duyệt, tới ngày hiệu lực: không công việc nào có căn cứ là thỏa thuận; `GET /checklist` của S có `thoaThuan` cho A | US1 #10, FR-002a, FR-030 |
| Q40 | Thỏa thuận có `tuQuyetDuoc = true` mà người quyết định là `BAC_SI`; `nctThamGia = false`; ý kiến nguồn người thân không có quan hệ Hiệu lực | 422 cho từng trường hợp (`NGUON_Y_KIEN_KHONG_HOP_LE` ở trường hợp cuối) | FR-002a, 001 FR-023g |
| Q41 | Sửa thỏa thuận của phiên bản Hiệu lực (API và `UPDATE` trực tiếp) | 409 `PHIEN_BAN_DA_KHOA`; trigger chặn | FR-002a, BR-M04-19 |
| Q42 | QL ngừng hiệu lực loại "Người cao tuổi từ chối"; đổi tên; tạo loại "Đang ngủ" rồi ngừng; S ghi Không thực hiện với loại đã ngừng | 409 `LOAI_LY_DO_CO_DINH` hai lần; 200; 422 | FR-028a |

| Q43 | Tua tới ngày xem lại thỏa thuận ở Q39; chạy việc hằng ngày hai lần | D và BS nhận một nhắc Nhẹ; không có công việc nào được tạo | FR-002a, Q-360 |

Test bổ sung: `FR_002a_NhacXemLaiIT` (Q43), `FR_002a_ThoaThuanRuiRoIT` (Q39 → Q41), `FR_028a_LoaiLyDoKhongThucHienIT` (Q24, Q42), test Flutter ghi Không thực hiện ngoại tuyến với loại lý do đã lưu.
