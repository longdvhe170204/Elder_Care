# Research: Chi phí phát sinh (010)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

Mỗi mục gồm Quyết định, Lý do, Phương án đã cân nhắc. Quyết định nền R-01 → R-14 của 000 và các mẫu đã chốt ở 001 → 009 (H-02 bean trạng thái, C-02 / D-02 chiều phụ thuộc, D-17 sự kiện tính phí của 006, B-02 khuôn thông báo) được dùng lại, không quyết lại. Các quyết định dưới đây là kỹ thuật; chúng không đổi nghiệp vụ (constitution IX). Chỗ spec chưa rõ hoặc mâu thuẫn được ghi ở plan, mục "Điểm cần báo lại".

---

## P-01. Vị trí trong backend

**Quyết định**: Feature 010 thuộc module M11 – Chi phí phát sinh (mục 15), gói `com.cambae.backend.m11`. Feature 017 (số dư, 15.9) và phần mua sắm của 019 (15.10) cũng thuộc M11; plan của chúng thêm gói con vào cùng `m11` và dùng port nội bộ (P-12), như mẫu 003 ↔ 019 và 008 ↔ 015.

| Gói con | Nội dung | Mục spec |
| --- | --- | --- |
| `m11.ky` | Kỳ chi phí của viện, bảng chi phí (thường, bổ sung, kỳ cuối), gửi chốt, chốt | A, G |
| `m11.khoan` | Khoản chi phí, vòng đời, kiểm tra, duyệt, trả lại, hủy, tính lại, lịch sử | D |
| `m11.nguon` | Chỉ mục sự kiện tính phí, tra cứu bản ghi nguồn, lập theo sự kiện (từng khoản, hàng loạt), listener nguồn đổi | B |
| `m11.tinhgia` | Hàm thuần đơn giá và số tiền, thời gian tính phí | C |
| `m11.dieuchinh` | Khoản điều chỉnh, biến động nguồn sau chốt | F |
| `m11.muaho` | Đề nghị mua hộ, đồng ý, ghi đã mua | E |
| `m11.kycuoi` | `KyCuoiPort`, điều kiện chi phí kỳ cuối, bean trạng thái, việc sau qua đời | G |
| `m11.xuat` | Xuất kế toán | H |
| `m11.cungcap` | Bảng chi phí cho cổng, tạm tính, tổng chưa chốt, thống kê | H |
| `m11.lich`, `m11.api` | Job; giao diện công khai, sự kiện | |

---

## P-02. Chiều phụ thuộc giữa module

**Quyết định**:

| Cặp | Chiều mã | Cách giữ một chiều |
| --- | --- | --- |
| 010 → 000, 002 | 010 dùng | `NhatKyApi`, `ThamSoApi`, `DinhChinhApi`, `TepApi`, `NhacQuaHan`, `QuyenApi`, `QuyenXemTruongApi`, `NguoiDungHienTai`; cài `PhanLoaiTruongProvider`, `LuuTruProvider`, `GanPhamViProvider`, `QuyenTepProvider`. Không đăng ký `DinhChinhDuocProvider` cho khoản đã chốt (000 R-08: gọi đính chính trả `KHONG_HO_TRO_DINH_CHINH`) |
| 010 → 001 | Cài, nghe | Cài `ThamGiaChuyenTrangThai` (`KT_A_CHI_PHI`; tác động khi qua đời, kết thúc, hủy tiếp nhận); đọc `NguoiCaoTuoiApi`, `thoiDiemTrangThaiCuoi` |
| 010 → 004 | Cài, đọc, nghe | Cài `KyCuoiPort`, `MucViecSauQuaDoiPort` (`CHI_PHI_KY_CUOI`); đọc `HopDongDocApi`, `DonGiaApi` (gồm `donGiaTaiNgay(…, danhDauThamChieu = true)` MANDATORY), `NgayLuuTruApi`; nghe `NgayLuuTruDaDoi`, `PhuLucDaApDung`, `HoSoKetThuc*`, `NgayNghiBanTruDaDoi`, `BaoVangBanTruDaGhi`, `HopDongDaDoiTrangThai`, `PhienBanDonGiaDaTao` (bổ sung, P-05) |
| 010 → 005 | Nghe, đọc | `KetQuaCoTinhPhi`, `KetQuaTinhPhiDaDinhChinh`, `TrangThaiCoMatDaXacDinh`, `TrangThaiCoMatDaDoi` |
| 010 → 006 | Cài, nghe | Cài `DeNghiMuaHoPort`; nghe `SuKienTinhPhiThuoc`, `SuKienTinhPhiThuocDaDieuChinh`, `SuKienTinhPhiThuocDaDinhChinh` |
| 010 ↔ 011, 012, 014 | Họ → 010 | Họ làm plan sau, phát `m11.api.SuKienTinhPhiNguon`, `SuKienTinhPhiNguonDaDoi` (P-04). Người đại diện đồng ý mua hộ đi qua REST của 010 với kiểm tra `NguoiDaiDienPort` (`m02.api`, 012 cài) |
| 010 ↔ 017, 019 | Cùng M11 | Port nội bộ (P-12) |
| 010 → 009, 016 | Sự kiện; đọc | Thông báo cài `CoYeuCauThongBao` (009 B-02); 016 đọc `ChiPhiThongKeApi` |

**Lý do**: Modulith `verify()` từ chối vòng (000 R-02).

---

## P-03. Khóa và đồng thời

**Quyết định**:
- **Thứ tự khóa** (nối 007 S-03): `nguoi_cao_tuoi` (001) → `luu_tru_nct` (004) → … → `chi_phi_nct` (M11, một dòng mỗi người) → `bang_chi_phi` → `khoan_chi_phi` (theo `id` tăng). Bean trạng thái của 010 chạy sau 006, 007 theo `@Order` của 001; `KyCuoiPort` chạy trong transaction lệnh hồ sơ kết thúc của 004 (sau khóa `luu_tru_nct`).
- **Lập khoản theo sự kiện** (FR-006, DBR-15): khóa `chi_phi_nct`; lấy hoặc lập bảng (`UNIQUE (ky_id, nct_id, loai_bang)`); thêm khoản. Lưới an toàn: cột sinh `khoa_don_vi_hieu_luc = IF(nguon_sinh = 'THEO_SU_KIEN' AND trang_thai <> 'DA_HUY', CONCAT(loai_nguon, ':', id_nguon, ':', don_vi_tinh_phi), NULL)` `UNIQUE`. Lệnh đến sau trả 409 `DA_CO_KHOAN` (hàng loạt: bỏ qua kèm lý do).
- **Lệnh trên bảng** (gửi chốt, trả lại, chốt): khóa `bang_chi_phi` và kiểm tra lại; lệnh chốt dựa trên trạng thái cũ bị từ chối (FR-033) bằng `If-Match` trên `bang_chi_phi.phien_ban` vì nhiều khoản có thể đổi giữa lúc xem và lúc chốt (US3 Edge Case "hai người cùng thao tác").
- **Lệnh trên khoản** (kiểm tra, duyệt, trả lại, hủy, tính lại): khóa dòng khoản, kiểm tra lại; khi đổi trạng thái bảng (về Đang mở) khóa thêm dòng bảng theo thứ tự trên.
- **Đề nghị mua hộ** (quyết định đầu tiên thắng, 000 FR-040): khóa dòng đề nghị; quyết định thứ hai trả 409 `DA_DUOC_XU_LY`.

---

## P-04. Sự kiện tính phí: chỉ mục, tra cứu, lập khoản (mục B, FR-005 → FR-007, Q-306, Q-307)

**Quyết định**:
- **Hai loại nguồn**:
  1. **Tính từ 004, không lưu**: ngày lưu trú (`NgayLuuTruApi.theoKhoang`) và buổi bán trú có lịch hoặc phát sinh (`NgayLuuTruApi.buoiBanTru`, có trạng thái có mặt của 005 và hệ số). Đơn vị tính phí: `NGAY_LUU_TRU:<nct>:<ngay>`, `BUOI_BAN_TRU:<nct>:<ngay>:<buoi>`.
  2. **Có bản ghi ở module nguồn**: 005 (kết quả công việc tính phí, vật phẩm), 006 (liều, lần dùng PRN, lần giao), 011 (suất ăn người thân), 012 (lượt ở lại, mỗi đêm một đơn vị), 014 (lượt điểm danh có phí). 010 giữ **chỉ mục kỹ thuật** `su_kien_tinh_phi` dựng từ sự kiện của các module này: loại nguồn, id, đơn vị, người cao tuổi, ngày phát sinh, thời điểm, dịch vụ/vật phẩm/thuốc, số lượng hiện hành, đã hủy, phiên bản. Chỉ mục chỉ dùng cho tra cứu và cho việc tìm khoản khi nguồn đổi; không phải khoản, không có trạng thái "chưa lập".
- **Sự kiện của module làm plan sau** (`m11.api`):
  ```java
  record SuKienTinhPhiNguon(String loaiNguon, String nguonId, String donVi, long nctId, Optional<Long> dichVuId,
                            BigDecimal soLuong, LocalDate ngayPhatSinh, Instant thoiDiem, String khoaSuKien) {}
  record SuKienTinhPhiNguonDaDoi(String loaiNguon, String nguonId, String donVi, Optional<BigDecimal> soLuongMoi,
                                 boolean daHuy, String canCu, String khoaSuKien) {}   // đính chính, hủy ghi nhận, hủy suất, hủy điểm danh
  ```
  005, 006 giữ các sự kiện đã khai báo; listener của 010 chuẩn hóa về cùng dạng.
- **Tra cứu** (`GET /chi-phi/ban-ghi-nguon?nctId&loai&tu&den`): trả mọi bản ghi nguồn khớp tiêu chí, kèm `khoanHienCo` (id khoản đang hiệu lực nếu có) để Hành chính mở xem. Không có bộ lọc "chưa lập khoản", không sắp ưu tiên, không đề xuất đơn giá trước khi lập (FR-005, Q-307).
- **Lập** (`POST /chi-phi/khoan/theo-su-kien`, một hoặc nhiều nguồn cùng loại; phí lưu trú nhận một khoảng ngày): mỗi nguồn chạy kiểm tra FR-006, FR-007 (ngày đã kết thúc, trừ lập trước kỳ cuối), FR-011 (thời gian tính phí), FR-005b (buổi có trạng thái; ngày khu nghỉ), FR-023 (liều nguồn gia đình gửi, thuốc mua hộ: 006 không phát `SuKienTinhPhiThuoc` nên không có trong chỉ mục; lệnh với nguồn đó trả `KHONG_PHAI_SU_KIEN_TINH_PHI`). Nguồn đạt thì tính giá (P-05), lập khoản, ghi giá trị nguồn đã dùng vào khoản. Hàng loạt: một transaction cho cả lô; nguồn không đạt bỏ qua kèm lý do (FR-005); SC-006 ≤ 30 giây cho 300 nguồn.
- **Bảng nhận khoản** (FR-002, FR-003): ngày phát sinh thuộc bảng thường chưa chốt → bảng đó (lập nếu chưa có); thuộc bảng đã chốt → khoản điều chỉnh bổ sung ở bảng chưa chốt của kỳ hiện tại hoặc bảng bổ sung, dấu `PHAT_SINH_THUOC_KY_DA_CHOT` (Edge Case).

**Lý do**: Q-307 bỏ danh sách sự kiện chưa lập khoản, nhưng spec vẫn đòi tra cứu theo người, loại, khoảng ngày và xử lý khoản khi nguồn đổi; chỉ mục kỹ thuật cho cả hai mà không gọi đồng bộ vào năm module ở mỗi lần tra cứu. Ngày lưu trú và buổi bán trú đã có API tính của 004 nên không cần chép.

**Phương án đã cân nhắc**:
- Gọi API đọc của 005, 006, 011, 012, 014 mỗi lần tra cứu: 005, 006 không có API tìm theo tính phí; mở thêm năm API làm 010 phụ thuộc đồng bộ vào nhiều module, và vẫn cần chỉ mục để ánh xạ "nguồn đổi → khoản".
- Lưu ngày lưu trú vào bảng của 010: là bản sao dữ liệu của 004 và trái Q-306 (FR-005a: "số liệu tính, không do Bộ lập lịch lập").

---

## P-05. Đơn giá và số tiền (mục C, FR-008 → FR-013, BR-M11-02, 03, 09, DBR-16)

**Quyết định**:
- Hàm thuần trong `m11.tinhgia`:
  - `LamTronDong(x)`: nửa lên theo giá trị tuyệt đối (FR-013);
  - `PhiNgayLuuTru(giaThang, soNgayThang, laNgayCuoiThang, heSo)`: phí ngày cơ sở = `LamTronDong(giaThang / soNgay)`, ngày cuối tháng = `giaThang − (soNgay − 1) × co_so` (FR-009); áp hệ số rồi làm tròn;
  - `PhiBuoiBanTru(giaThang, cacBuoiCoLich, buoi, heSo)`: số buổi có lịch theo lịch đến của hợp đồng hiệu lực trừ ngày khu nghỉ, không đếm buổi phát sinh; buổi có lịch cuối nhận chênh lệch;
  - `ThoiGianTinhPhi(hopDong, hoanTatTiepNhan, hoSoKetThuc, quaDoi, huyTiepNhan)`: khoảng và dấu "chưa vào ở" (FR-011, Q-22).
- **Căn cứ đơn giá** (FR-008): khoản thuộc hợp đồng → `DonGiaApi.donGiaHopDong(nctId, dichVuId, ngay)` / `HopDongDocApi.noiDungTaiNgay`; ngoài hợp đồng → `DonGiaApi.donGiaTaiNgay(dichVuId, ngay, danhDauThamChieu = true)` trong transaction lập khoản (004 đặt `da_tham_chieu`, T-05). Thuốc nguồn viện: `DonGiaApi.thuocNguonVien(thuocId)` ra dịch vụ. Dịch vụ chăm sóc theo mức: `DonGiaApi.dichVuChamSoc(muc)` (Q-297). Khoản lưu `can_cu_don_gia` (loại, id phiên bản nội dung hoặc phiên bản đơn giá).
- **Thuộc gói** (FR-010): nội dung hợp đồng tại ngày có dịch vụ trong gói → dấu `THUOC_GOI`, số tiền 0.
- **Thiếu đơn giá** (FR-012): không có giá → khoản Nháp dấu `THIEU_DON_GIA`, `so_tien = NULL`; thông báo Hành chính (Nhẹ). 004 tạo phiên bản đơn giá (kể cả hiệu lực lùi cho khoảng chưa có phiên bản, DBR-08) và phát `PhienBanDonGiaDaTao(dichVuId, hieuLucTu, hieuLucDen)` (bổ sung vào hợp đồng 004); listener của 010 tính lại mọi khoản chưa chốt `THIEU_DON_GIA` của dịch vụ đó có ngày phát sinh trong khoảng, gỡ dấu, ghi lịch sử tính lại. Khoản đã có đơn giá không tính lại (000 FR-017).
- Người dùng không nhập số lượng, đơn giá, hệ số, số tiền của khoản theo sự kiện (FR-013, FR-016); API không có trường đó.

---

## P-06. Vòng đời khoản, kiểm tra, duyệt, tính lại (mục D, FR-014 → FR-020, BR-M11-04 → 06)

**Quyết định**:
- `khoan_chi_phi` là nhóm 2 tới khi Đã chốt, sau đó nhóm 3 (mẫu "nhóm 3 có bản nháp" của 000 R-05 với `thoi_diem_xac_nhan = chot_luc`): trigger chặn mọi đổi khi `trang_thai IN ('DA_CHOT','DA_HUY')`.
- Bảng chuyển theo spec (data-model mục 5). Hàng loạt (FR-015): kiểm tra hàng loạt theo bộ lọc bỏ khoản `THIEU_DON_GIA`; duyệt hàng loạt loại khoản nguồn `NHAP_TAY`, `MUA_HO`, `DIEU_CHINH`; mỗi khoản bị bỏ có lý do trong phản hồi.
- Duyệt khoản mua hộ mang dấu `VUOT_SO_DONG_Y` hoặc `MUA_GAP_VUOT` bắt buộc `lyDoChapNhanVuot` (FR-024); duyệt xong phát thông báo người đại diện (Nhẹ, `CHI_PHI`).
- **Tính lại khi nguồn đổi** (FR-020, BR-M11-05): listener sau commit (`NgayLuuTruDaDoi`, `KetQuaTinhPhiDaDinhChinh`, `SuKienTinhPhiThuocDaDieuChinh`, `TrangThaiCoMatDaDoi`, `NgayNghiBanTruDaDoi`, `PhuLucDaApDung`, `SuKienTinhPhiNguonDaDoi`, `PhienBanDonGiaDaTao`) tìm khoản theo đơn vị tính phí:
  - khoản Nháp: tính lại hoặc `DA_HUY` (nguồn bị hủy), thêm `lich_su_tinh_lai`;
  - khoản Đã kiểm tra, Đã duyệt: về Nháp rồi tính lại/hủy, bảng Chờ chốt về Đang mở, thông báo Hành chính (FR-004);
  - khoản Đã chốt: không đổi; thêm `bien_dong_nguon_sau_chot` (nguồn, giá trị trước/sau, sự kiện gây ra) và thông báo Hành chính (FR-027). Không tạo khoản điều chỉnh, không tính sẵn số chênh lệch (Q-307).
  Mỗi sự kiện nguồn mang khóa để bỏ trùng (`su_kien_nguon_da_xu_ly`, NFR-04). SC-005 ≤ 5 phút: listener chạy ngay sau commit của nguồn.
- **Quyền xem tên thuốc** (FR-019, Q-133): trường `ten_thuoc_hien_thi` của khoản thuốc mang `@NhomTruong(THUOC)`; 002 che với Hành chính, Kế toán, người thân không có quyền xem sức khỏe có tác dụng; mã vật phẩm luôn hiện. Lịch sử tính lại, thông báo, bộ lọc chỉ dùng mã. File kế toán dùng mã (FR-039).

---

## P-07. Kỳ, bảng, gửi chốt, chốt (mục A, G, FR-001 → FR-004, FR-029 → FR-033)

**Quyết định**:
- `ky_chi_phi` (nhóm 2): `thang`, trạng thái `DANG_MO | CHO_CHOT | DA_CHOT`, `han_chot` (tính từ CFG-M11-01 lúc kỳ sang Chờ chốt). Job hằng ngày 00:20: tạo kỳ của tháng mới (chuyển trạng thái theo lịch, không phải bản ghi nghiệp vụ của actor; bảng trạng thái kỳ của spec ghi "Hệ thống (theo lịch)"); kỳ hết tháng → `CHO_CHOT`. Kỳ → `DA_CHOT` khi bảng cuối cùng của kỳ chốt và mọi người có thời gian tính phí trong kỳ đã có bảng thường (kiểm trong lệnh chốt bảng).
- `bang_chi_phi` (nhóm 2 → 3): `UNIQUE (ky_id, nct_id, loai_bang)` với `loai_bang IN ('THUONG','BO_SUNG')` (FR-002, FR-002a); `ky_cuoi`, `den_ngay`. Bảng được lập cùng lệnh lập khoản đầu tiên, lệnh lập khoản điều chỉnh cần bảng bổ sung, hoặc `KyCuoiPort.taoKyCuoiNhap` (Q-306).
- **Gửi chốt** (Hành chính): bảng thường đã qua ngày cuối hoặc là kỳ cuối; không còn khoản Nháp (`FR-030` liệt kê); tính biến động (P-08); phát thông báo Quản lý viện.
- **Chốt** (Quản lý viện, `If-Match`): mọi khoản Đã duyệt hoặc Đã hủy; trong một transaction (FR-033): khoản Đã duyệt → Đã chốt, lưu `bang_chi_phi_tong_loai` (nhóm 3), tổng, người chốt; phát `BangChiPhiDaChot(bangId, loaiBang, kyId, nctId, tong, chotLuc)` (017 nghe — đúng một lần mỗi bảng, khóa `bangId`, FR-042), thông báo người thân có quyền xem chi phí (Trung bình, FR-040); kiểm lại điều kiện kỳ cuối (P-10) và kỳ của viện.
- **Nhắc** (FR-031, FR-029): job hằng ngày: bảng của kỳ đã qua hạn chốt chưa chốt → Hành chính (Đang mở) / Quản lý viện (Chờ chốt); còn khoản thiếu đơn giá → thêm Quản lý viện; người có thời gian tính phí trong kỳ đã kết thúc mà chưa có bảng thường → Hành chính (khóa `(kỳ, nct, ngày)`); bảng bổ sung chưa chốt → Hành chính mỗi CFG-M02-09. Danh sách "người chưa có bảng thường" tính lúc chạy từ `HopDongDocApi` và trạng thái người cao tuổi; đây là nhắc, không lập bảng.

---

## P-08. Cảnh báo biến động (FR-032, BR-M11-07)

**Quyết định**: Hàm thuần `BienDongKy(bangNay, bangTruoc, cfg)`: chỉ bảng thường; tổng gồm khoản chưa hủy và khoản điều chỉnh có kỳ gốc là chính kỳ; bỏ khi kỳ trước không có bảng thường hoặc một trong hai bảng không đủ tháng (thời gian tính phí phủ cả tháng); tăng > CFG-M11-03 hiện hành → trả `canhBao` (tỷ lệ, ba loại tăng nhiều nhất) trong phản hồi gửi chốt và phát thông báo Hành chính (Nhẹ). Không chặn.

---

## P-09. Đề nghị mua hộ (mục E, FR-021 → FR-024, BR-M11-07, Q-137, Q-139, Q-251, Q-259, Q-269)

**Quyết định**:
- `de_nghi_mua_ho` (nhóm 2): mục (vật phẩm trong danh mục hoặc mô tả), số lượng, số tiền dự kiến, `so_tien_duoc_phep`, `cfg_m11_02_luc_gui`, lý do, người yêu cầu (nhân viên hoặc người thân), trạng thái theo bảng của spec (`NHAP`, `CHO_DONG_Y`, `DUOC_PHEP_MUA`, `TU_CHOI`, `HUY`, `DA_MUA`), cờ `dang_cho_bo_sung`. `quyet_dinh_mua_ho` (nhóm 3): mỗi lần đồng ý/từ chối/rút bổ sung, số cũ, số mới, người quyết định, kênh (`CONG_NGUOI_THAN`, `QUAN_LY_VIEN`), lý do. `de_nghi_gui_nguoi_dai_dien` (danh sách người đại diện Hiệu lực lúc gửi, FR-023a).
- **Gửi**: so số dự kiến với CFG-M11-02 đọc lúc gửi; > ngưỡng → `CHO_DONG_Y`, thông báo người đại diện (Trung bình, `CHI_PHI`) và Quản lý viện (Nhẹ). Không có người đại diện có tài khoản cổng (qua `NguoiDaiDienPort`, 012) → chỉ Quản lý viện, thông báo Hành chính.
- **Đồng ý / từ chối**: người đại diện qua cổng (`POST /de-nghi-mua-ho/{id}/dong-y`, kiểm `NguoiDaiDienPort.laNguoiDaiDienHieuLuc` lúc quyết định và có tên trong danh sách gửi) hoặc Quản lý viện; quyết định đầu tiên thắng. Người đại diện thôi làm đại diện thì mất quyền (FR-023a). Dấu "số dư không đủ" (Q-219) đọc từ port `SoDuMuaHoPort` của 017 (P-12) và hiển thị, không chặn.
- **Xin đồng ý bổ sung**, **Rút yêu cầu bổ sung** theo bảng (Q-251, Q-269); số được phép là lần đồng ý có hiệu lực gần nhất (Q-259).
- **Ghi đã mua** (FR-024): hàm thuần `KiemTraVuot(soThucTe, soDuocPhep, cfg02, cfg06)`; vượt ngoài hai ngưỡng thì chặn trừ lý do "mua gấp thuốc, vật tư y tế" (mục trong danh mục loại thuốc/vật tư y tế, kèm Bác sĩ/Điều dưỡng yêu cầu). Trong cùng transaction: `DA_MUA`, lập khoản `MUA_HO` Nháp với dấu tương ứng, tệp chứng từ gắn qua `TepApi`.
- **Nhắc chờ đồng ý** (CFG-M15-05, 06): job hằng giờ dùng `NhacQuaHan` của 000 với mốc gửi (hoặc lần xin bổ sung).
- `DeNghiMuaHoPort.daMua(deNghiId, nctId)` (006 khai báo) trả đề nghị `DA_MUA`, thuốc, số lượng, nhân viên mua (Q-139).

---

## P-10. Kỳ cuối, qua đời, điều kiện chi phí (FR-029, FR-034 → FR-036, BR-M11-08, Q-21, Q-26, Q-134, Q-141)

**Quyết định**:
- `KyCuoiPort` (`m02.api`, 010 cài; MANDATORY trong lệnh hồ sơ kết thúc của 004):
  - `taoKyCuoiNhap(nctId, E)`: lập hoặc đánh dấu bảng thường của kỳ chứa E là kỳ cuối, `den_ngay = E`; thông báo Hành chính; trả `bangId`;
  - `tinhLai(nctId, Emoi)`: kỳ cuối chưa chốt → khoản ngày sau Emoi → `DA_HUY` "đổi ngày kết thúc"; đã chốt → `bien_dong_nguon_sau_chot` loại `DOI_NGAY_KET_THUC` và thông báo; cập nhật `den_ngay`; tính lại điều kiện;
  - `huyMocDung(nctId)`: bỏ dấu kỳ cuối, thông báo Hành chính.
- **Lập trước** (FR-007): khoản phí lưu trú ngày > hôm nay trong kỳ cuối được phép, dấu `LAP_TRUOC`. Job hằng ngày: ngày đã kết thúc → gỡ dấu; nếu `NgayLuuTruApi` cho ngày đó khác giá trị đã dùng → xử lý như nguồn đổi (P-06).
- **Điều kiện `KT_A_CHI_PHI`** (FR-034a): hàm `DieuKienChiPhiKyCuoi(bangKyCuoi, cacKhoanPhiLuuTru, E, homNay, cacDieuChinhAnhHuong)`: (1) bảng kỳ cuối Đã chốt; (2) mọi ngày lưu trú tới E, và mọi ngày đã kết thúc sau E khi chưa Kết thúc lưu trú, có khoản phí lưu trú (gốc hoặc điều chỉnh bổ sung) trong bảng Đã chốt; (3) không còn khoản `ANH_HUONG_KET_THUC` chưa chốt. Bean `ThamGiaChuyenTrangThai` trả `DieuKien("KT_A_CHI_PHI", …, coTheMien = true)` khi chưa đạt; trạng thái hiện hành cũng có ở `ChiPhiKyCuoiApi` cho 004 hiển thị.
- Dấu `ANH_HUONG_KET_THUC` gắn lúc lập khoản khi ngày phát sinh ngoài khoảng mà kỳ cuối đã chốt và việc lập do đổi E (FR-034) hoặc quá E (FR-035); khoản bổ sung theo FR-029 (sự kiện thường sau khi kỳ cuối đã chốt) không mang dấu.
- **Quá E** (FR-035): job hằng ngày phát thông báo Hành chính khi E đã kết thúc mà người đó chưa ở trạng thái cuối; điều kiện tính lại.
- **Bean `ThamGiaChuyenTrangThai`** (`@Order` sau 006):
  - `KET_THUC_LUU_TRU`: điều kiện `KT_A_CHI_PHI`; không tác động khoản.
  - `GHI_NHAN_QUA_DOI`: tác động: khoản lập trước cho ngày sau ngày qua đời chưa chốt → `DA_HUY`; đã chốt → `bien_dong_nguon_sau_chot` và thông báo; kỳ cuối tính đến ngày qua đời (Q-141: ngày qua đời tính trọn).
  - `HUY_TIEP_NHAN`: thời gian tính phí kết thúc sau ngày hủy (FR-011).
- `MucViecSauQuaDoiPort` (`CHI_PHI_KY_CUOI`): `HOAN_THANH` khi kỳ cuối và mọi bảng bổ sung tồn tại lúc qua đời đã chốt (lưu `mien_hoan_thanh_tu` để bảng bổ sung mở sau không kéo lại, FR-036); gọi `DanhSachViecSauQuaDoiApi.capNhat` khi chốt các bảng đó.

---

## P-11. Cung cấp cho người thân, 017, 016 và xuất kế toán (mục H, FR-037 → FR-042)

**Quyết định**:
- **Cổng người thân**: `GET /nguoi-cao-tuoi/{nctId}/chi-phi/bang` (bảng Đã chốt), `…/tam-tinh?tu&den` (FR-038): người thân cần `QuyenApi` (`CHI_PHI, XEM`) với điều kiện của 012; tên thuốc theo P-06. Tạm tính dùng hàm `TongTamTinh(cacKhoan, khoang)` (không cộng nhập tay, mua hộ chưa duyệt; không cộng thiếu đơn giá, kèm ghi chú; khoản điều chỉnh có kỳ gốc đã chốt xếp theo ngày tạo).
- **017** (cùng M11, port `ChiPhiChoSoDuPort`): `tongChuaChot(nctId)` (FR-038a, cùng thành phần tạm tính, mọi bảng chưa chốt), `phiNgayCoSo(nctId, ngay)` (FR-009), sự kiện `BangChiPhiDaChot`, `DeNghiMuaHoDaGui/DaQuyetDinh/DaHuy/DaMua`.
- **016**: `ChiPhiThongKeApi` (khoản theo loại, nguồn sinh, người lập, dấu, trạng thái; bảng; khoản điều chỉnh với khoản gốc, kỳ gốc; kỳ và trạng thái).
- **Xuất kế toán** (Kế toán, FR-039, FR-041, Q-05, Q-212): `POST /chi-phi/ky/{kyId}/xuat` với phạm vi `TOAN_BO | CHUA_TUNG_XUAT`, định dạng `XLSX | CSV`. Chỉ khoản Đã chốt của bảng Đã chốt; cột theo FR-039 (thuốc dùng mã vật phẩm); dòng tổng kiểm soát; mỗi bảng đánh dấu "xuất lần đầu"/"đã xuất trước đó"; danh sách người có bảng chưa chốt. Ghi `lan_xuat_ke_toan` (nhóm 3) và `lan_xuat_bang`, đặt `bang_chi_phi.lan_xuat_dau_id` nếu trống (cột kỹ thuật được đặt một lần sau chốt — trigger cho phép đúng cột này). Ghi nhật ký hành động xuất (000 FR-043a). Tệp sinh bằng thư viện ghi XLSX dạng luồng (fastexcel) hoặc CSV UTF-8 có BOM; xuất lại phạm vi `TOAN_BO` không có bảng mới cho cùng nội dung (sắp xếp xác định).

---

## P-12. Port nội bộ M11 cho 017 và 019

**Quyết định**: Gói `m11.cungcap.port`, `m11.muaho.port`; ArchUnit chỉ cho `m11.sodu`, `m11.muasam` dùng:

```java
interface ChiPhiChoSoDuPort {               // 010 cài, 017 dùng
  long tongChuaChot(long nctId);            // FR-038a
  Optional<Long> phiNgayCoSo(long nctId, LocalDate ngay);   // FR-009, 017 FR-016
}
interface SoDuMuaHoPort {                   // 017 cài, 010 dùng (mặc định: không có dấu)
  boolean soDuKhongDu(long nctId, long soTien, Instant t);  // Q-219, BR-M11-15
}
```

Sự kiện `BangChiPhiDaChot`, `DeNghiMuaHo*` là sự kiện thường của `m11.api` (017 nghe như module khác). Yêu cầu mua sắm của 019 không sinh khoản (FR-023 Q-284); 010 không cần port cho 019.

---

## P-13. Sự kiện và thông báo (FR-044)

**Quyết định**: Mức theo bảng FR-044 của spec (đủ mức). Mọi sự kiện thông báo cài `CoYeuCauThongBao` (009 B-02), loại thông tin `CHI_PHI` với người thân, không có tên thuốc. Nhắc chờ đồng ý quá CFG-M15-05 / 06 theo mức của 009 FR-043b dòng 000 (Trung bình). Sự kiện khác: `BangChiPhiDaChot`, `DeNghiMuaHo*` (017), `KhoanChiPhiDaDoi` (016, 012 tạm tính — tùy chọn, 012 có thể đọc lại).

---

## P-14. Quyền (FR-043, FR-017)

**Quyết định**:

| `ChucNang` | Thao tác |
| --- | --- |
| `CHI_PHI` | `LAP`, `KIEM_TRA`, `GUI_CHOT` (Hành chính); `DUYET`, `CHOT` (Quản lý viện); `XEM` (Quản lý viện, Hành chính, Kế toán toàn viện; người thân có quyền xem chi phí theo 012) |
| `XUAT_KE_TOAN` | `THUC_HIEN` (Kế toán) |
| `MUA_HO` | `LAP` (Hành chính); `QUYET_DINH` (Quản lý viện; người đại diện qua `NguoiDaiDienPort`) |

Quản lý viện không có `LAP`, `KIEM_TRA`, `GUI_CHOT`, `XUAT_KE_TOAN`; Hành chính không có `DUYET`, `CHOT` (FR-017). Vai trò khác không xem chi phí.

---

## P-15. Bộ lập lịch, lưu giữ, nhật ký, kiểm thử

**Quyết định**:
- **Job**: `ViecHangNgayM11` 00:20 (kỳ theo lịch; gỡ dấu lập trước; quá E; nhắc hạn chốt, bảng bổ sung, người chưa có bảng thường); `ViecHangGioM11` (nhắc đề nghị chờ đồng ý). ShedLock, `lan_chay_lich`, cột "đã nhắc" theo khóa ngày; chạy bù khi khởi động (NFR-04). Job không lập khoản, bảng (trừ kỳ của viện theo lịch), khoản điều chỉnh (Q-306).
- **Lưu giữ**: bảng, khoản, lịch sử tính lại, đề nghị mua hộ: `THEO_HO_SO` (CFG-M15-03, hồ sơ chi phí của người cao tuổi; NFR-07); kỳ của viện, lần xuất: `KHONG_GAN_HO_SO`. Chỉ mục sự kiện tính phí: theo nguồn.
- **Nhật ký**: mọi lệnh có nhật ký; tính lại do hệ thống ghi người thực hiện Hệ thống và sự kiện nguồn; xuất kế toán là nhật ký hành động (000 FR-043a).
- **Kiểm thử**:
  - test bảng `PhiNgayLuuTru` (SC-002: 12 tháng 28 → 31 ngày, có phụ lục), `PhiBuoiBanTru`, `LamTronDong`, `ThoiGianTinhPhi`, `BienDongKy`, `DieuKienChiPhiKyCuoi`, `KiemTraVuot`, `TongTamTinh`;
  - Test tích hợp MySQL: `UNIQUE khoa_don_vi_hieu_luc` (DBR-15), `UNIQUE (ky, nct, loai_bang)`, trigger khoản và bảng đã chốt (DBR-17), tổng gốc + điều chỉnh không âm (kiểm ở backend dưới khóa bảng chứa khoản gốc);
  - test đồng thời: hai lệnh lập cùng đơn vị; chốt trong lúc lập khoản mới; hai người đại diện quyết định cùng lúc;
  - đồng hồ giả lập cho CFG-M11-01, 02, 03, 06, CFG-M15-05, 06, CFG-M02-09;
  - bản giả `SuKienTinhPhiNguon` của 011, 012, 014; `NguoiDaiDienPort`; `SoDuMuaHoPort`;
  - so file xuất với bảng (SC-009).
- Tên test theo BR/FR (`BR_M11_09_ChiaGiaThangTest`, `DBR_15_MotKhoanMoiDonViIT`).
