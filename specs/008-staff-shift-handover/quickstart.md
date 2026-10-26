# Quickstart: Kiểm chứng feature 008

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md)

Hướng dẫn kịch bản nghiệm thu; tên tiếng Việt bên dưới là tên logic, đường dẫn/mã dùng khi gọi thật theo [contracts/api-008.yaml](contracts/api-008.yaml), không sao chép nguyên tên logic vào terminal. Trạng thái kiểm tự động, nguồn thật còn thiếu và số đo người dùng ở [validation.md](validation.md). Giao tiếp module xem [contracts/module-interfaces.md](contracts/module-interfaces.md); bảng và ràng buộc xem [data-model.md](data-model.md).

## 1. Điều kiện cần

- Như quickstart của 000 → 007: JDK 25, MySQL 8.0 cài trên máy (000 R-14), Node.js LTS, Flutter stable; profile `demo` có đồng hồ giả lập.
- Dữ liệu mẫu của profile `demo`:
  - tài khoản của quickstart 002: Quản lý viện QL; Trưởng tầng T (tầng 2), U; Điều dưỡng D, E, F (tầng 2); Nhân viên chăm sóc S, M, N; Bác sĩ B; Hành chính H; Nhân viên bếp C (bộ phận Bếp); Kế toán K;
  - mẫu ca: Ca ngày 07:00–18:00, Ca đêm 18:00–07:00, Ca toàn viện Bác sĩ 07:00–07:00 (24 giờ, không bàn giao);
  - tầng 2 có 25 người cao tuổi ở 8 phòng (dữ liệu 003): 20 người mức cơ bản, 6 người mức đặc biệt (trọng số 1 và 2,5 theo CFG-M09-12);
  - giấy phép điều dưỡng của E hết hạn 04/10; đào tạo sơ cứu của S bắt buộc, hết hạn 30/11;
  - CFG theo mặc định Phụ lục 25: CFG-M09-01 8 trọng số/nhân viên, CFG-M09-02 16 giờ, CFG-M09-03 60 ngày, CFG-M09-04 30 phút, CFG-M09-05 30 phút, CFG-M09-08 24 giờ, CFG-M09-09 24 giờ, CFG-M15-07 2 giờ;
  - test tự động nhập bản giả dùng chung của `HeldBelongingsHandoverPort` (013), `TripDutyPort` (014), `ExtendedPublicationCheck` (015) trong `backend/src/test`; ứng dụng thật không có endpoint công tắc giả lập và không đăng ký bean fake. Kiểm demo đầu–cuối cần nguồn thật tương ứng; xem `validation.md`.

## 2. Chạy

```text
backend/  ./mvnw verify
backend/  ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
frontend/ npm ci && npm test && npm run dev
mobile/   flutter test
```

Kết quả mong đợi: `verify` xanh, gồm Modulith `verify()` (không có vòng 008 ↔ 002, 008 ↔ 005, 008 ↔ 007, 008 ↔ 013, 008 ↔ 014), test cơ sở dữ liệu (data-model mục 8), test đồng thời và các nhóm test ở mục 4.

## 3. Kịch bản kiểm chứng đầu–cuối (profile demo)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| Q1 | QL tạo mẫu "Ca đêm" 18:00–07:00; mẫu 08:00–08:00; mẫu dài 0 | 201 qua ngày; 201 ca 24 giờ; 422 | US2 #1, FR-011 |
| Q2 | T xếp S vào ca 12:00–20:00 tầng 3 khi S có ca ngày tầng 2 | 409 `CHONG_CA` nêu ca bị chồng | US2 #2, DBR-21 |
| Q3 | T xếp S vào ca đêm cùng ngày ca ngày | 409 `CAN_XAC_NHAN_CANH_BAO` 24 giờ > 16; gửi lại có lý do: 200 | US2 #3, FR-014 |
| Q4 | T công bố khi còn ca thiếu người phụ trách; chỉ định S làm người phụ trách | 409 liệt kê ca; 409 vai trò | US2 #4, #5 |
| Q5 | T công bố lịch hợp lệ; S mở `/toi/ca` | Lịch `DA_CONG_BO`; mỗi nhân viên nhận `LichCaDaCongBo` chỉ ca của mình; phân công Nháp → Tương lai/Hiệu lực; `ban_cong_bo_lich` có bản ghi; S chỉ thấy ca của mình | US2 #6, FR-054 |
| Q6 | T xóa S khỏi ca đã công bố | 405/409 (không có thao tác) | US2 #7 |
| Q7 | T chuyển người phụ trách ca đêm từ E sang F khi bàn giao của ca đang Bản nháp có ghi chú của E | F là người phụ trách và người bàn giao; ghi chú giữ tên E; F nhận thông báo; E mất quyền | US2 #8, #10, FR-019 |
| Q8 | Phân công Nháp E làm Điều dưỡng phụ trách khi giấy phép còn hạn; giấy phép hết hạn rồi công bố; công bố với `loaiBoPhanCong` | 201 Nháp; 409 `PHAN_CONG_KHONG_DAT` kèm lý do; 200 | US2 #9, FR-016, Q-82 |
| Q9 | Phân công E (giấy phép hết 04/10) làm Điều dưỡng phụ trách ca 05/10; giấy phép hết 05/10 cho ca đêm 05/10 | 409 `THIEU_CHUNG_CHI`; 409 (không phủ toàn bộ ca) | US3 #1, #2, BR-M09-01 |
| Q10 | Phân S chính không giới hạn cho A khi "thay băng" cần đào tạo S không có | 201 kèm cảnh báo loại công việc không đủ điều kiện | US3 #3, FR-031 |
| Q11 | S chính theo phòng 201, M chính theo A (phòng 201); hỏi `NhanSuCaPort.phanCong(A)`; người mới vào phòng 201 lúc 10:00 | M cho A; S cho người khác và người mới | US3 #4, #8, FR-026, FR-028 |
| Q12 | Phân M chính cho A khi S đã chính theo A; gửi đồng thời hai phân công chính cho A | 409 `TRUNG_NHAN_VIEN_CHINH`; một lệnh thành công, lệnh sau 409 | US3 #5, SC-011 |
| Q13 | Sao chép phân công thứ Hai → thứ Ba khi A đã chuyển tầng 3, B Kết thúc lưu trú | S theo phòng sao chép; M theo A, N theo B trong danh sách không sao chép kèm lý do | US3 #7, #10 |
| Q14 | QL ghi nhận thu hồi giấy phép D từ hết 10/10 khi D có phân công 11 → 15/10 | Tại mốc, phân công Đã hủy/Đã kết thúc, 005, 006 nhận gán lại, T nhận danh sách | US3 #9, FR-009 |
| Q15 | Tua tới 01/10 (60 ngày trước hạn đào tạo S); QL gia hạn | `ChungChiSapHetHan` cho QL, S, Trưởng tầng có ca; gia hạn: bản cũ Đã thay thế, `CanhBaoChungChiDaDong` | US4 #2, #3, BR-M09-05 |
| Q16 | QL cho nghỉ việc S hiệu lực ngay | Một lần: `khoaDoNghiViec`, rút ca, phân công kết thúc, `CongViecCaApi.khiNghiViec`, Trưởng tầng nhận; tỷ lệ tính lại | US4 #5, BR-M09-04, SC-008 |
| Q17 | Cho nghỉ việc E là người phụ trách ca đêm tối nay | Ca gắn `thieu_nguoi_phu_trach`; T, QL nhận `CaThieuNguoiPhuTrach`; T hoàn tất được bàn giao của ca đó | US4 #6 |
| Q18 | QL xóa hồ sơ nhân viên; nhận lại không lý do; có lý do | 405; 422; Đang làm việc, tài khoản vẫn khóa | US4 #7, #8 |
| Q19 | Cho nghỉ việc Phụ trách bộ phận Bếp không chỉ định người thay; chỉ định C; chỉ định K làm Phụ trách bộ phận Hành chính – Kế toán | 409 `CAN_NGUOI_THAY_PHU_TRACH`; 200; 409 | US4 #9, FR-001a |
| Q20 | Ca ngày tầng 2: tổng trọng số 35, 4 nhân viên được tính; công bố | Tỷ lệ 8,75, cảnh báo dưới ngưỡng, công bố vẫn được | US5 #1, BR-M09-02 |
| Q21 | Ghi nhận vắng một nhân viên ca đã công bố đang đạt; A tạm vắng suốt ca đêm; A đổi mức sang đặc biệt | `CaDuoiNguongPhucVu` một lần cho T; A không tính; tỷ lệ các ca tương lai tính lại | US5 #2 → #4, SC-007 |
| Q22 | D (Người phụ trách ca) ghi nhận S vắng lúc 07:20; ghi vắng ca tuần sau | Một lần: dấu vắng, phân công kết thúc, `CongViecCaApi.khiVangCa`, T nhận, tỷ lệ tính lại; 409 `NGOAI_KHOANG_GHI_VANG` | US6 #1, Edge Case, Q-81 |
| Q23 | T ghi nhận D (người phụ trách) vắng không chỉ định người mới khi không ai đủ điều kiện | Ca gắn dấu thiếu người phụ trách; T nhận nhắc thay | US6 #2 |
| Q24 | QL giao U tầng 2 10/10 → 20/10 khi T giao không thời hạn; kết thúc T 09/10, giao U, giao lại T 21/10 | 409 `CHONG_GIAO_TRUONG_TANG`; 201 ba lệnh, mỗi thời điểm một người | US6 #6, FR-024 |
| Q25 | QL hủy ca đêm 20/10 chưa bắt đầu; hủy ca đã bắt đầu | `DA_HUY`, phân công hủy, nhân viên nhận; 409 `CA_DA_BAT_DAU` | US6 #5 |
| Q26 | Tầng 2 chưa có Trưởng tầng: tua đầu ca; F (Người phụ trách ca) xác nhận bàn giao | QL nhận `TangChuaCoTruongTang`; F xác nhận được, nhật ký căn cứ `THAY_TRUONG_TANG` | Edge Case, Q-90 |
| Q27 | Ca ngày tầng 2 có công việc Quá hạn, liều Bỏ lỡ, chỉ số vượt ngưỡng, cảnh báo mở, người trở về; tua 17:30 | D nhận `BanGiaoChuaLap`; không có bàn giao tự tạo; D lập: `GET …/ban-ghi-nguon` trả đủ năm nguồn; bàn giao không có mục | US1 #1, SC-001 |
| Q28 | D thêm mục công việc X và chỉ số của A; 17:40 X Hoàn thành, cảnh báo đóng | Mục không tự đổi; X hiện "Hoàn thành"; bỏ mục X được; bỏ mục chỉ số 409 `MUC_KHONG_BO_DUOC` | US1 #2, #3, Q-88 |
| Q29 | D hoàn tất khi liều Bỏ lỡ của B thiếu ghi chú; bổ sung rồi hoàn tất 17:50; tua 18:00 | 409 `THIEU_GHI_CHU_MUC_NGHIEM_TRONG`; `DA_LAP`; ca `DA_DONG` | US1 #4, FR-041 |
| Q30 | 17:55 cảnh báo mới ở tầng 2; E mở bàn giao | Cảnh báo có trong `ban-ghi-nguon?sauKhiLap=true`; không có mục tự ghi | US1 #5, FR-045 |
| Q31 | Ca tới 18:00 khi bàn giao còn Bản nháp | Ca `CHO_BAN_GIAO`; D, T nhận nhắc | US1 #6, BR-M09-07 |
| Q32 | E xác nhận | `DA_XAC_NHAN`; cùng transaction: 005 `khiXacNhanBanGiao`, 007 `khiXacNhanBanGiao`; nhật ký người xác nhận | US1 #7, SC-004 |
| Q33 | E gửi ý kiến; E xác nhận và gửi ý kiến đồng thời; D bổ sung và gửi lại; E xác nhận | `CO_Y_KIEN`, D, T nhận; một lệnh thắng, lệnh sau 409 `TRANG_THAI_DA_DOI`; `DA_LAP`; `DA_XAC_NHAN` | US1 #8, FR-042 |
| Q34 | 18:30 bàn giao chưa xác nhận | T nhận `BanGiaoChuaXacNhan` | US1 #9 |
| Q35 | Sửa bàn giao đã xác nhận bằng API; `UPDATE` trực tiếp; đính chính | Không có thao tác; trigger chặn; đính chính được, bản gốc giữ | US1 #10, SC-009 |
| Q36 | Lập bàn giao lần hai cho cùng ca | 409 `DA_CO_BAN_GIAO` | US1 #11, DBR-20 |
| Q37 | 18:00 bàn giao Có ý kiến; ca đêm bắt đầu | `NhanSuCanhBaoPort.caDaBatDauChuaXacNhanBanGiao` trả (ca ngày, ca đêm); `NhanSuCaPort.banGiaoDaXacNhan` sai; 005, 006, 007 tự chuyển tạm | US1 #12, FR-044a |
| Q38 | Mất kết nối khi hoàn tất | App báo cần kết nối, không đưa vào hàng đợi | US1 #13, FR-060 |
| Q39 | Bàn giao Có ý kiến, D quá giờ kết thúc + 2 giờ thử gửi lại; T gửi lại | 403 `NGOAI_PHAM_VI_CA`; `DA_LAP`, người viết T | US1 #14 |
| Q40 | Thông báo vắng ca, nghỉ việc, thu hồi qua kênh ngoài ứng dụng | Không có lý do, họ tên người cao tuổi, sức khỏe | SC-012, FR-053, FR-059 |
| Q41 | S (Nhân viên chăm sóc) mở bàn giao | Chỉ thấy mục của người trong phạm vi và nhận định chung | FR-047 |
| Q42 | Dừng backend qua 18:00 rồi khởi động | Ca chuyển trạng thái với thời điểm dự kiến; nhắc gửi một lần | NFR-04 |

## 4. Test tự động: truy vết kịch bản → test

| Nhóm | Test | Kịch bản |
| --- | --- | --- |
| Hàm thuần | `HieuLucChungChiTest`, `ChuoiCaLienTucTest`, `XacDinhNhanVienChinhTest`, `XacDinhCaSauTest`, `TinhTyLePhucVuTest`, `MucNghiemTrongTest`, `SC_005_XepCaPhanCongTest` (200 lần) | Q2, Q3, Q9, Q11, Q20, Q29 |
| Nhân viên, bộ phận | `BR_M09_04_NghiViecIT`, `FR_003_NhanLaiIT`, `FR_001a_PhuTrachBoPhanIT`, `DBR_36_MotBoPhanIT`, `NhanVienTriggerIT` | Q16 → Q19 |
| Chứng chỉ | `BR_M09_05_CanhBaoDaoTaoIT`, `FR_009_ThuHoiIT`, `BR_M09_01_ChanThieuChungChiIT` | Q9, Q14, Q15 |
| Lịch ca | `BR_M09_03_ChongCaIT`, `ChongCaDongThoiIT`, `FR_016_CongBoIT`, `FR_019_ChuyenNguoiPhuTrachIT`, `FR_021_HuyCaIT`, `FR_022_TrangThaiCaIT` | Q1 → Q8, Q25 |
| Phân công | `FR_028_NhanVienChinhIT`, `PhanCongDongThoiIT`, `FR_032_SaoChepIT`, `FR_024_GiaoTruongTangIT`, `FR_028a_DieuDuongViecChamSocIT` | Q10 → Q13, Q24 |
| Vắng ca | `FR_020_VangCaIT`, `FR_020_NgoaiKhoangIT` | Q22, Q23 |
| Tỷ lệ | `BR_M09_02_TyLePhucVuIT`, `FR_036_CanhBaoMotLanIT` | Q20, Q21 |
| Bàn giao | `BR_M09_06_DanhSachNguonIT`, `FR_040_MucBanNhapIT`, `FR_041_HoanTatIT`, `BR_M09_07_ChoBanGiaoIT`, `BR_M09_08_XacNhanBanGiaoIT`, `FR_044_XacNhanNguyenTuIT`, `FR_042_LenhDongThoiIT`, `DBR_20_MotBanGiaoMoiCaIT`, `BanGiaoTriggerIT`, `FR_043_NhacBanGiaoIT` | Q27 → Q39 |
| Nhiệm vụ | `NhiemVuApiIT` (người phụ trách ca theo thời điểm, thay Trưởng tầng, Bác sĩ trực nhiều người), `ThamChieuViTriIT` | Q26, Q37 |
| Quyền riêng tư | `FR_052_GioiHanTruongNhanVienIT`, `FR_053_LyDoVangIT`, `FR_047_ChamSocXemBanGiaoIT`, `SC_012_ThongBaoNgoaiUngDungIT` | Q40, Q41 |
| Bộ lập lịch | `ChayBuM09IT` | Q42 |
| Kiến trúc | `ApplicationModules.verify()`; ArchUnit: chỉ `m09.doica`, `m09.xoayca` dùng port nội bộ; gói `..lich..` không lập ca, phân công, bàn giao | |
| Web (Vitest) | Lịch Nháp (cảnh báo xác nhận, danh sách chặn công bố); màn bàn giao (danh sách nguồn không có nút "chép tất cả", mục không bỏ được, lỗi thiếu ghi chú) | Q3, Q8, Q27 → Q29 |
| App (`flutter_test`) | `BanGiaoNgoaiTuyenChiXemTest`, `XoaBanGiaoKhiHetPhamViCaTest`, `GhiNhanVangCaTest` | Q22, Q38 |

## 5. Tiêu chí hoàn tất (đối chiếu SC)

| SC | Cách đo |
| --- | --- |
| SC-001 | Q27 → Q29: danh sách nguồn đủ 100%; 0 bàn giao, mục do hệ thống tạo; nhắc ≤ 1 phút sau mốc (đồng hồ giả lập) |
| SC-002 | Thử nghiệm với điều dưỡng: 10 lần, trung vị ≤ 10 phút (đo trên web và app) |
| SC-003 | Vận hành thử một tháng: tỷ lệ bàn giao xác nhận trong CFG-M09-05 từ `NhanSuThongKeApi.banGiao` |
| SC-004 | Q32: sau xác nhận, công việc tồn trong checklist ca mới, cảnh báo đổi người phụ trách trong cùng transaction; test lỗi giả lập ở 007 → không gì đổi |
| SC-005 | `SC_005_XepCaPhanCongTest` |
| SC-006 | Đồng hồ giả lập cho Q14, Q15, Q22, Q31, Q34 |
| SC-007 | Q20, Q21 so với bảng tính tay |
| SC-008 | Q16 |
| SC-009 | Q35 |
| SC-010 | Thử nghiệm với Trưởng tầng: 10 lần sao chép, trung vị ≤ 5 phút |
| SC-011 | Q12, Q24 và test đồng thời |
| SC-012 | Q40 và test nhật ký mọi lệnh nhóm 2 có căn cứ quyền |

## 6. Kịch bản bổ sung 2026-10-22 (Q-382; 007 FR-047i)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| I1 | QLV tải mẫu, điền 40 dòng hợp lệ, gọi `xem-truoc` | 200, 40 dòng, `loi` rỗng, `xacNhanDuoc = true`; 0 hồ sơ mới | US4 #10, FR-001b |
| I2 | Gọi `xac-nhan` với cùng file | 201, 40 hồ sơ `DANG_LAM_VIEC`, `nguon = IMPORT`, không tài khoản, không bộ phận; một `lan_import_nhan_vien`; nhật ký `NV_IMPORT` có tên file, 40 id | US4 #10, FR-001c |
| I3 | File dòng 7 thiếu chức danh, dòng 12 số 9 chữ số | `loi` có (7, "Chức danh", `THIEU_O_BAT_BUOC`), (12, "Số điện thoại", `DIEN_THOAI_SAI_DINH_DANG`); `xac-nhan` 409, 0 hồ sơ | US4 #11 |
| I4 | Hồ sơ S (Nghỉ việc) số 0912345678; file có dòng số đó, hai dòng khác cùng một số | `TRUNG_HO_SO` kèm id S; `TRUNG_DONG_TRONG_FILE` kèm số dòng | US4 #12 |
| I5 | Xem trước không lỗi; tạo tay hồ sơ trùng số với dòng 5; xác nhận | 409 `IMPORT_CO_DONG_LOI` báo dòng 5; 0 hồ sơ | US4 #13 |
| I6 | File 501 dòng (CFG-M09-13 = 500); file .csv; file thiếu cột Email; Trưởng tầng gọi `xem-truoc` | 422 `QUA_SO_DONG`; 422 `FILE_KHONG_DUNG_MAU` (hai lần); 403 | US4 #14, FR-001c |
| I7 | 500 dòng hợp lệ: đo `xem-truoc`, `xac-nhan` | Mỗi bước ≤ 10 giây | SC-013 |
| K1 | Bản cài `AlertStaffPort`: C có ca Tầng 2 đang diễn ra; H (Hành chính) không xếp ca trong giờ hành chính CFG-M13-06; C vắng ca | `onShift(C)` true, `floorDuringShift(C)` = 2; `onShift(H)` true, tầng rỗng; C vắng → false; `floorsWithShift` vẫn gồm tầng 2. Theo Q-332, Bếp không thuộc ba vai trò mặc định trong giờ hành chính | 007 FR-047i, Q-332 |

Test: `FR_001b_ImportNhanVienIT` (I1 → I6), `KiemTraDongImportTest` (bảng mọi loại lỗi), `ImportHieuNangIT` (I7), `NhanSuCanhBaoPortIT` mở rộng (K1).
