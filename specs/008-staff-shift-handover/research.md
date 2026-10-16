# Research: Nhân sự, ca trực và bàn giao (008)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

Mỗi mục gồm Quyết định, Lý do, Phương án đã cân nhắc. Quyết định nền R-01 → R-14 của 000 và các mẫu đã chốt ở 001 → 007 (H-02 bean trạng thái, C-02 / D-02 / S-02 chiều phụ thuộc, C-03 khóa, D-17 / S-19 sự kiện) được dùng lại, không quyết lại. Các quyết định dưới đây là kỹ thuật; chúng không đổi nghiệp vụ (constitution IX). Chỗ spec chưa rõ hoặc mâu thuẫn được ghi ở plan, mục "Điểm cần báo lại".

---

## N-01. Vị trí trong backend

**Quyết định**: Feature 008 là module M09 – Nhân sự và ca trực (mục 13), gói `com.cambae.backend.m09`:

| Gói con | Nội dung | Mục spec |
| --- | --- | --- |
| `m09.nhanvien` | Hồ sơ nhân viên, trạng thái làm việc, bộ phận, Phụ trách bộ phận | A (FR-001 → FR-004) |
| `m09.giayphep` | Loại giấy phép/đào tạo, phạm vi hành nghề, giấy phép/đào tạo của nhân viên, hiệu lực, cảnh báo hết hạn | A (FR-005 → FR-010) |
| `m09.lichca` | Mẫu ca, lịch ca, ca, nhân viên trong ca, người phụ trách ca, Bác sĩ trực, lệnh sau công bố | B (FR-011 → FR-023) |
| `m09.phancong` | Giao trưởng tầng, phân công, `XacDinhNhanVienChinh`, sao chép | C (FR-024 → FR-033) |
| `m09.tyle` | Tỷ lệ phục vụ, lần tính | D (FR-034 → FR-037a) |
| `m09.bangiao` | Bàn giao, mục, ý kiến, danh sách bản ghi nguồn, xác nhận | E (FR-038 → FR-047) |
| `m09.nhiemvu` | `TraCuuCaService`: nhiệm vụ, ca, phân công theo thời điểm; bản cài các SPI của 002, 003, 005, 006, 007 | F (FR-049) |
| `m09.lich`, `m09.api` | Job; giao diện công khai, SPI cho 013, 014, sự kiện | |

Feature 015 (đổi ca, xoay ca, nghỉ đột xuất, phủ tối thiểu) cũng thuộc M09; plan 015 thêm gói con vào cùng `m09` và dùng các port nội bộ ở `m09.lichca.port`, `m09.phancong.port` (N-07) thay vì API công khai, như mẫu 003 ↔ 019 (G-01) và 005 ↔ 014.

**Lý do**: Constitution X chia gói theo M01–M15; Phụ lục 26.2 đặt UC-49 → UC-53 ở 008, UC-50 phần xoay ca và UC-54 ở 015, cùng M09.

**Phương án đã cân nhắc**: Tách 015 thành module riêng: sinh module ngoài M01–M15, và 015 sửa trực tiếp ca, phân công của 008.

---

## N-02. Chiều phụ thuộc giữa module

**Quyết định**: Theo mẫu 003 G-02 (SPI khai báo ở module thấp hơn) và 005 C-02.

| Cặp | Chiều mã | Cách giữ một chiều |
| --- | --- | --- |
| 008 ↔ 002 | 008 → 002 | 002 làm plan trước và cần dữ liệu của 008 (FR-004, FR-019a, FR-028, FR-037). Các giao diện `HoSoNhanVienApi`, `GiayPhepApi`, `DanhMucPhamViHanhNgheApi`, `PhanCongApi`, `NhiemVuApi` được khai báo ở `m15.taikhoan.api` dưới dạng SPI, M09 cài (bổ sung theo plan này; chữ ký giữ đúng 002 module-interfaces mục 2). 008 gọi `TaiKhoanApi.khoaDoNghiViec`, `theoNhanVien`, `coVaiTro`; `QuyenApi`, `PhamViApi`, `NhatKyApi`, `ThamSoApi`, `DinhChinhApi` |
| 008 → 001 | Đọc, nghe | `MucChamSocApi.hienHanh`, `taiNgay`; `TrangThaiNguoiCaoTuoiApi.khoangTrangThai`; `NguoiCaoTuoiApi.trangThai`; nghe `MucChamSocDaDoi`, `NguoiCaoTuoiDaChuyenTrangThai` |
| 008 → 003 | Đọc, cài | `ViTriApi`, `LichSuViTriApi.theoViTriTrongNgay`, `CoCauViTriApi`, `CanhBaoBanGiaoApi.theoTang`; cài `ThamChieuViTri`; nghe `PhanBoDaBatDau`, `PhanBoDaDong`, `DaChuyenGiuong` |
| 008 → 004 | Đọc | `LuotVangApi.dangMo` (mốc dự kiến trở lại; khai báo ở `m01.api`, 004 cài); `HopDongDocApi.lichDenTaiNgay` (bán trú dự kiến có mặt, FR-034) |
| 008 → 005 | Cài, gọi | Cài `NhanSuCaPort`; gọi `CongViecCaApi` (MANDATORY), đọc `CongViecBanGiaoApi` |
| 008 → 006 | Cài, đọc | Cài `DieuDuongCaPort`; đọc `LieuBanGiaoApi` |
| 008 → 007 | Cài, gọi | Cài `NhanSuCanhBaoPort`; gọi `CanhBaoBanGiaoApi.dangMo`, `khiXacNhanBanGiao` (MANDATORY); đọc `ChiSoApi.vuotNguongTrongKhoang` |
| 008 ↔ 013, 014 | 013, 014 → 008 | Họ làm plan sau. 008 cần đồ gửi có giá trị đang giữ (013) và chuyến đi, trưởng đoàn, người đi cùng (014) qua SPI khai báo ở `m09.api`: `DoGuiBanGiaoPort`, `ChuyenDiNhiemVuPort` |
| 015 → 008 | Cùng module | Port nội bộ `m09.*.port` (N-07) |
| 008 → 009, 016 | Sự kiện; đọc | Thông báo là sự kiện (N-14); 016 đọc `NhanSuThongKeApi` |

**Lý do**: Modulith `verify()` từ chối vòng (000 R-02). 002 đã gọi tên các giao diện này trong plan của mình; đặt chúng ở `m15.taikhoan.api` giữ chiều `m09 → m15`.

**Phương án đã cân nhắc**: 002 đọc bảng của 008 qua view MySQL: truy cập dữ liệu module khác (constitution X).

---

## N-03. Khóa và đồng thời

**Quyết định**:
- **Thứ tự khóa trong M09**: `nhan_vien` (theo `id` tăng) → `tang_ca_khoa` (theo `tang_khu_vuc_id` tăng) → `lich_ca` → `ca` → `phan_cong` → `ban_giao`. Khi lệnh của 008 gọi sang 005, 007 (vắng ca, nghỉ việc, xác nhận bàn giao), các module đó khóa dòng của mình sau các khóa trên; 005, 006, 007 đọc 008 không khóa, nên không có vòng chờ.
- **Chồng ca của một nhân viên** (FR-013, DBR-21): mọi lệnh thêm nhân viên vào ca khóa dòng `nhan_vien`, rồi kiểm tra trên `ca_nhan_vien` còn hiệu lực của người đó. Khoảng thời gian không đưa được vào `UNIQUE` nên kiểm ở backend dưới khóa; test đồng thời chứng minh.
- **Phân công, giao trưởng tầng** (FR-024, FR-028, SC-011): khóa `tang_ca_khoa` của tầng/khu (dòng kỹ thuật, tạo khi tạo tầng hoặc khi lần đầu dùng), rồi kiểm tra chồng; lệnh đến sau thấy dữ liệu của lệnh trước và bị từ chối (FR-028 "lệnh ghi nhận sau bị từ chối").
- **Bàn giao** (FR-042, DBR-20): `UNIQUE (ca_id)` trên `ban_giao`; lệnh trạng thái khóa dòng `ban_giao` và kiểm tra lại; lệnh thứ hai trả 409 `TRANG_THAI_DA_DOI`. Không dùng `If-Match` cho lệnh trạng thái (005 C-03); lệnh sửa mục ở Bản nháp dùng `If-Match` vì nhiều người cùng sửa (FR-040: người bàn giao và Điều dưỡng có tên trong ca).
- **Lịch Nháp**: sửa tự do dùng `If-Match` trên `lich_ca.phien_ban`.
- **Tính tỷ lệ phục vụ đồng thời** (Edge Case): khóa dòng `ca` khi ghi lần tính; lần tính sau đọc dữ liệu mới nhất nên phản ánh cả hai thay đổi; cột `dang_duoi_nguong` chống cảnh báo trùng (FR-036).

---

## N-04. Hồ sơ nhân viên, trạng thái làm việc, bộ phận (FR-001 → FR-004)

**Quyết định**:
- `nhan_vien`: phần mô tả (nhóm 1: họ tên, chức danh, chuyên môn, ngày bắt đầu, liên hệ) và `trang_thai` (nhóm 2: `DANG_LAM_VIEC`, `NGHI_VIEC`) chung một bảng; trạng thái chỉ đổi qua lệnh (trigger chặn đổi `trang_thai` khi không kèm biến phiên `@lenh_trang_thai_nv`, mẫu 000). `tai_khoan_id` tham chiếu logic tới 002, có thể rỗng (Q-282). Không `DELETE` (trigger), chỉ Nghỉ việc.
- **Cho nghỉ việc** (FR-003, FR-004): lệnh ghi `nghi_viec_hieu_luc`. Hiệu lực ngay thì tác động chạy trong cùng transaction; hiệu lực tương lai thì job theo phút (N-16) chạy đúng một lần tại mốc (cột `da_ap_nghi_viec_luc`). Tác động FR-004 trong một transaction: (a) `TaiKhoanApi.khoaDoNghiViec`; (b) rút khỏi ca kết thúc sau mốc (`ca_nhan_vien.rut_do_nghi_viec`); (c) kết thúc phân công, giao trưởng tầng; (d) `CongViecCaApi.khiNghiViec`; (e) ca người đó phụ trách gắn `thieu_nguoi_phu_trach`, phát `CaThieuNguoiPhuTrach`; (f), (g) chuyển quyền bàn giao (tính lúc đọc từ dấu thiếu người phụ trách, N-12); (h) tính lại tỷ lệ phục vụ. Lỗi ở một bước rollback cả lệnh.
- **Nhận lại làm việc**: lý do; không mở tài khoản; không khôi phục ca, phân công.
- **Bộ phận** (FR-001a, Q-284, Q-292, Q-293, Q-299, DBR-36): `bo_phan` (nhóm 1, `phu_trach_id NOT NULL` sau khi có người), `nhan_vien_bo_phan` (lịch sử: `tu`, `den`), `lich_su_phu_trach_bo_phan` (nhóm 3). Bất biến:
  - mỗi nhân viên có tài khoản thuộc đúng một bộ phận tại một thời điểm (trừ tài khoản Quản lý viện): cột sinh `khoa_hien_hanh = IF(den IS NULL, nhan_vien_id, NULL)` `UNIQUE`; đổi bộ phận là đóng dòng cũ và mở dòng mới cùng transaction;
  - Phụ trách bộ phận thuộc bộ phận đó, có tài khoản Hoạt động, không có vai trò AC-01, AC-14 (`TaiKhoanApi.coVaiTro`), Phụ trách bộ phận Hành chính – Kế toán có vai trò Hành chính;
  - lệnh Cho nghỉ việc, đổi bộ phận của người đang là Phụ trách bộ phận bắt buộc `nguoiThayId` (409 `CAN_NGUOI_THAY_PHU_TRACH`); 002 gọi `NhiemVuApi.laPhuTrachBoPhan` trước khi khóa tài khoản để áp cùng điều kiện;
  - bộ phận mới chưa có phụ trách: job hằng ngày nhắc Quản lý viện (`BoPhanChuaCoPhuTrach`); 019 chặn lệnh của bộ phận đó qua `NhiemVuApi.phuTrachBoPhan` rỗng.

---

## N-05. Giấy phép, đào tạo (FR-005 → FR-010, BR-M09-01, 05, BR-M06-05)

**Quyết định**:
- `loai_chung_chi` (nhóm 1): nhóm `GIAY_PHEP_HANH_NGHE | DAO_TAO`, `co_thoi_han`, `bat_buoc_voi_vai_tro` (tập), `co_pham_vi`. Khởi tạo: giấy phép hành nghề bác sĩ, điều dưỡng, đào tạo sơ cứu. `pham_vi_hanh_nghe` (nhóm 1): danh mục dùng chung cho 002 (`nghiep_vu_chuyen_mon_pham_vi`), 007 (giấy phép cơ sở) qua `DanhMucPhamViHanhNgheApi`.
- `chung_chi` (nhóm 1 có lịch sử): số, đơn vị cấp, ngày cấp, ngày hết hạn, `thay_the_cho`, `thu_hoi_tu`, lý do thu hồi, tệp scan; bảng con phạm vi. Lệnh Thêm, Gia hạn (bản mới, bản cũ "Đã thay thế" suy ra từ `thay_the_cho`), Ghi nhận thu hồi, Sửa sai (lý do, nhật ký trước/sau; FR-006). Không `DELETE`.
- **Hiệu lực** là hàm thuần `HieuLucChungChi(banGhi, thoiDiem)` theo FR-007: Còn hiệu lực từ 00:00 ngày cấp tới hết 23:59:59 ngày hết hạn (Asia/Ho_Chi_Minh, 002 FR-038), chưa thu hồi, chưa bị thay. `coHieuLucTrongKhoang(nhanVien, loai, t1, t2)` đúng khi một bản cùng loại Còn hiệu lực tại mọi thời điểm trong `[t1, t2]` (ca qua đêm: US3 #2). `GiayPhepApi.hieuLuc` của 002 dùng cùng hàm.
- **Cảnh báo sắp hết hạn** (FR-008, BR-M09-05): job hằng ngày 00:10 xét đào tạo bắt buộc với vai trò hoặc được nhiệm vụ phân công hiện tại/tương lai yêu cầu, hết hạn trong CFG-M09-03 mà chưa có bản gia hạn: phát `ChungChiSapHetHan(chungChiId, ngayHetHan)` một lần (cột `da_canh_bao_luc`); gia hạn thì phát `CanhBaoChungChiDaDong`. Giấy phép hành nghề dùng CFG-M06-02 và thuộc 002 FR-039; 008 chỉ cung cấp `GiayPhepApi.sapHetHan`.
- **Hết hiệu lực** (FR-009): thu hồi, sửa sai làm ngày hết hạn sớm lên: lệnh tính mốc hết hiệu lực. Mốc ≤ bây giờ thì trong cùng transaction kết thúc (Hiệu lực) hoặc hủy (Tương lai) mọi phân công có nhiệm vụ yêu cầu loại đó, gọi `CongViecCaApi.khiKetThucPhanCong`, phát `PhanCongKetThucDoChungChi` cho Trưởng tầng; mốc tương lai thì job theo phút làm tại mốc (cột `da_ap_het_hieu_luc_luc`). Phân công lập sau đã bị chặn bởi FR-030.
- **Đào tạo bắt buộc với vai trò hết hiệu lực** (FR-010): xếp ca vẫn được, phản hồi kèm cảnh báo `DAO_TAO_BAT_BUOC_HET_HIEU_LUC`.
- Quyền xem theo nhóm trường FR-052: nhóm (b) mang `@NhomTruong(LIEN_HE_NHAN_VIEN)` / `HAN_CHE_NHAN_VIEN`, 002 che theo vai trò và phạm vi (Trưởng tầng thấy liên hệ của nhân viên có ca trong tầng trong tháng).

---

## N-06. Mẫu ca, lịch ca, ca (FR-011 → FR-017, FR-022, FR-023)

**Quyết định**:
- `mau_ca` (nhóm 1): giờ bắt đầu, giờ kết thúc (`LocalTime`), loại phạm vi, `yeu_cau_ban_giao`; ca qua ngày khi kết thúc ≤ bắt đầu, bằng nhau là 24 giờ (FR-011, CHK042).
- `lich_ca` (nhóm 2): tháng, phạm vi, trạng thái `NHAP | DA_CONG_BO`. `ca` (nhóm 2): `lich_ca_id`, `mau_ca_id`, `bat_dau`, `ket_thuc` (`DATETIME`, chép từ mẫu, chỉnh được ở Nháp), phạm vi, `yeu_cau_ban_giao` (chép từ mẫu), `nguoi_phu_trach_id`, `thieu_nguoi_phu_trach`, trạng thái theo bảng FR-022. Ca thuộc lịch của tháng chứa giờ bắt đầu.
- `ca_nhan_vien` (nhóm 2): nhân viên trong ca, `bac_si_truc`, `vang_luc`, lý do vắng, người ghi, `rut_do_nghi_viec`, `bo_sung_luc`, lý do bổ sung; cột cho 015 (`nghi_co_duyet_yeu_cau_id`, `can_cu_vao_ca`, `can_cu_roi_ca`) do 015 đặt qua port. Dòng chỉ thêm và đặt cột; không xóa sau công bố (lịch sử, FR-037a). Ở Nháp gỡ nhân viên là xóa dòng.
- `nguoi_phu_trach_ca` (lịch sử, nhóm 3): ca, nhân viên, từ, đến, căn cứ — nguồn của `PhamViApi.nguoiPhuTrachCa(tang, t)` và FR-037a.
- **Kiểm tra khi xếp** (mọi lệnh thêm nhân viên): FR-002 (Đang làm việc, tài khoản không Đã khóa, có vai trò xếp ca qua `TaiKhoanApi.theoNhanVien`); FR-013 chồng giờ (chặn); FR-014 chuỗi liên tục bằng hàm thuần `ChuoiCaLienTuc(caCuaNhanVien, caMoi)` (ca tiếp giáp nối nhau) > CFG-M09-02 → phản hồi 409 `CAN_XAC_NHAN_CANH_BAO` trừ khi thân lệnh có `xacNhanCanhBao = true` và `lyDo`; FR-010 cảnh báo đào tạo.
- **Công bố** (FR-016): người có quyền (Trưởng tầng được giao của phạm vi hoặc Quản lý viện; lịch toàn viện chỉ Quản lý viện). Kiểm tra: ca có yêu cầu bàn giao đều có người phụ trách; tầng có giao trưởng tầng phủ cả tháng (trừ lịch toàn viện); phân công Nháp đạt FR-028, FR-030 tại thời điểm công bố (không đạt thì 409 kèm danh sách; thân lệnh có `loaiBoPhanCong[]` để loại trong cùng lệnh); phần kiểm tra của 015 qua port `KiemTraCongBoMoRong` (phủ tối thiểu, nghỉ theo ngày; 015 FR-006a, FR-017, FR-026a). Trả `canhBao[]` tổng hợp (FR-014, FR-034, FR-010) trước khi xác nhận (`GET …/xem-truoc-cong-bo`). Tự công bố: lý do bắt buộc, nhật ký `tuDuyet` (Q-10); một lý do dùng chung cho tự duyệt và công bố khi thiếu phủ.
- Khi công bố, trong cùng transaction: ca → `DA_CONG_BO`; phân công Nháp → `TUONG_LAI` / `HIEU_LUC`; ghi `ban_cong_bo_lich` (nhóm 3, JSON toàn bộ ca, nhân viên, phân công tại thời điểm công bố; FR-055 "bản gốc có nhật ký"); tính tỷ lệ phục vụ mọi ca; phát `LichCaDaCongBo(lichId, nhanVienIds)` (mỗi người nhận chỉ ca của mình, FR-054).
- **Chuyển trạng thái theo giờ** (job theo phút): `DA_CONG_BO → DANG_DIEN_RA` tại `bat_dau`; `DANG_DIEN_RA → DA_DONG` hoặc `CHO_BAN_GIAO` tại `ket_thuc` theo trạng thái bàn giao (FR-022); phát `CaDaBatDau`, `CaDaKetThuc` (013, 005, 006, 007 dùng làm khóa bỏ trùng). Phạm vi dữ liệu theo ca do 002 tính từ giờ ca, không phụ thuộc job.
- **Bác sĩ trực** (FR-023): `ca_nhan_vien.bac_si_truc` trên ca phạm vi toàn viện; `bacSiTruc(t)` = mọi Bác sĩ có dòng đó, không vắng, trong ca `DANG_DIEN_RA` chứa `t` (nhiều người được).

---

## N-07. Lệnh sau công bố và port cho 015 (FR-017 → FR-021, Q-77)

**Quyết định**:
- **Bổ sung nhân viên vào ca** (FR-018): như kiểm tra khi xếp; phạm vi dữ liệu có ngay (002 tính từ dòng mới); phát `NhanVienDaBoSungVaoCa`.
- **Chuyển người phụ trách ca** (FR-019): đóng dòng `nguoi_phu_trach_ca` cũ, mở dòng mới; gỡ `thieu_nguoi_phu_trach`; nếu bàn giao của ca ở Bản nháp hoặc Có ý kiến thì đổi `ban_giao.nguoi_ban_giao_id`, giữ người viết trên từng ghi chú, phát `BanGiaoDangDoDaChuyen` cho người mới.
- **Ghi nhận vắng ca** (FR-020, Q-80, Q-81, Q-91): Trưởng tầng, hoặc Người phụ trách ca của chính ca đó trong khoảng phạm vi; khoảng cho phép `[bat_dau − CFG-M15-07, ket_thuc]`, ngoài khoảng 409 `NGOAI_KHOANG_GHI_VANG` kèm gợi dẫn sang 015. Trong cùng transaction: `vang_luc`; phân công của người đó trong ca → `DA_KET_THUC` (Hiệu lực) hoặc `DA_HUY` (Tương lai); `CongViecCaApi.khiVangCa`; nếu là người phụ trách thì bắt buộc `nguoiPhuTrachMoiId` hoặc gắn `thieu_nguoi_phu_trach` khi không ai đủ điều kiện; tính lại tỷ lệ; phát `NhanVienVangCa` (không nêu lý do, FR-053).
- **Hủy ca** (FR-021): Quản lý viện, ca chưa bắt đầu; phân công `DA_HUY`; phát `CaDaHuy`.
- **Port nội bộ cho 015** (gói `m09.lichca.port`, `m09.phancong.port`; ArchUnit chỉ cho `m09.doica`, `m09.xoayca` dùng):

  ```java
  interface CaLenhPort {                       // MANDATORY; cùng kiểm tra và tác động như lệnh REST tương ứng
    KetQuaThemVaoCa themVaoCa(long caId, long nhanVienId, CanCuVaoCa canCu, boolean xacNhanCanhBao); // FR-013, FR-014; lời mời, đổi ca
    void roiCa(long caId, long nhanVienId, CanCuRoiCa canCu);           // đổi ca, nghỉ có duyệt (đánh dấu, không xóa dòng)
    void doiNguoiPhuTrach(long caId, long nhanVienMoiId, CanCu canCu);  // như FR-019
    void ganThieuNguoiPhuTrach(long caId, CanCu canCu);
  }
  interface PhanCongLenhPort {                 // MANDATORY
    List<DieuKien> kiemTra(LapPhanCong lenh);  // FR-025, FR-028, FR-030
    long lap(LapPhanCong lenh, CanCu canCu);  void huy(long phanCongId, CanCu canCu);
  }
  interface KiemTraCongBoMoRong {              // 015 cài; 008 gọi trong lệnh công bố
    List<CanhBaoCongBo> kiemTra(long lichCaId); // phủ tối thiểu, nghỉ theo ngày chưa quyết định
  }
  ```

---

## N-08. Giao trưởng tầng và phân công (FR-024 → FR-033)

**Quyết định**:
- `giao_truong_tang` (nhóm 2): nhân viên (vai trò Trưởng tầng), tầng/khu, `tu`, `den` (tùy chọn), trạng thái `HIEU_LUC | DA_KET_THUC`; lệnh Giao, Kết thúc/rút ngắn (lý do). Chồng thời gian trên cùng tầng bị chặn dưới khóa `tang_ca_khoa` (FR-024, SC-011).
- **Tầng chưa có Trưởng tầng** (FR-024, Q-90): `NhiemVuApi.truongTangDuocGiao(tang, t)` rỗng thì `NhiemVuApi.nguoiThayTruongTang(tang, t)` trả Người phụ trách ca đang diễn ra của tầng; 002 cho các lệnh bàn giao, tạm nhận theo căn cứ `THAY_TRUONG_TANG`. Job đầu mỗi ca phát `TangChuaCoTruongTang` cho Quản lý viện tới khi có giao.
- `phan_cong` (nhóm 2): ca, nhân viên, đối tượng (`TANG | KHU_VUC | PHONG | NGUOI_CAO_TUOI`, id), `loai_cong_viec_id` (tùy chọn), vai trò phân công (`VaiTro` hệ thống + `CHINH | HO_TRO`), `dieu_duong_phu_trach`, `bat_dau`, `ket_thuc`, `ket_thuc_thuc_te`, trạng thái (`NHAP`, `TUONG_LAI`, `HIEU_LUC`, `DA_KET_THUC`, `DA_HUY`), lý do, nguồn (`LAP_TAY`, `SAO_CHEP`, `DOI_CA_015`). Bảng chuyển FR-027; Tương lai → Hiệu lực → Đã kết thúc theo giờ do job theo phút.
- **Kiểm tra lập** (FR-025, FR-028, FR-028a, FR-030):
  - nhân viên có tên trong ca, không vắng; đối tượng thuộc phạm vi ca; Điều dưỡng được phân việc vai trò Nhân viên chăm sóc, chiều ngược lại bị chặn (Q-214);
  - FR-028: không có phân công chính khác cùng mức đối tượng, cùng vai trò, cùng loại công việc giới hạn, cùng đối tượng, chồng thời gian;
  - FR-030: nhiệm vụ yêu cầu loại giấy phép/đào tạo (Điều dưỡng phụ trách: loại 002 khai cho nghiệp vụ "phát thuốc và xác nhận liều", đọc qua `NghiepVuChuyenMonApi.loaiYeuCau(PHAT_THUOC)` — bổ sung theo plan này; loại công việc giới hạn: `LoaiCongViecApi.yeuCau(loaiCongViecId)` của 005 — bổ sung theo plan này) phải `coHieuLucTrongKhoang`.
  - FR-031: phân công chính không giới hạn: phản hồi `canhBao.loaiCongViecKhongDuDieuKien[]` (loại công việc trong kế hoạch hiện hành của đối tượng qua `KeHoachChamSocApi`, 005).
- **Xác định nhân viên chính** (FR-028, US3 #4, #8): hàm thuần `XacDinhNhanVienChinh(phanCongHieuLuc, viTriNct, vaiTro, loaiCongViec)`: lọc phân công chính cùng vai trò bao người đó tại `t` (người cao tuổi > phòng > tầng/khu; vị trí theo `ViTriApi` tại `t`, FR-026), cùng mức thì có giới hạn loại công việc thắng. Đây là nguồn của `NhanSuCaPort.phanCong`, `DieuDuongCaPort.duocPhanCong`, `NhanSuCanhBaoPort.dieuDuongPhuTrach` và `PhanCongApi`.
- **Người cao tuổi chưa có Điều dưỡng phụ trách / nhân viên chăm sóc chính** (FR-029): tính lúc đọc; phát `NctChuaCoNguoiPhuTrach(caId, nctIds)` khi công bố và tại giờ bắt đầu ca (job, một lần mỗi ca).
- **Sao chép phân công** (FR-032): lệnh của Trưởng tầng; người thực hiện ghi ở nhật ký là Trưởng tầng, nguồn `SAO_CHEP` (plan điểm báo lại 2); bỏ phân công theo người cao tuổi đã chuyển tầng hoặc trạng thái cuối, nhân viên không có tên hoặc vắng ở ca đích; trả danh sách không sao chép kèm lý do.
- **Trạng thái cuối giữa ca** (Edge Case): listener `NguoiCaoTuoiDaChuyenTrangThai` kết thúc phân công theo người cao tuổi đó và tính lại tỷ lệ.

---

## N-09. Tỷ lệ phục vụ (FR-034 → FR-037a, BR-M09-02)

**Quyết định**:
- Hàm thuần `TinhTyLePhucVu(nguoiDuKien, nhanVienDuocTinh, trongSo, nguong)`:
  - người dự kiến có mặt: nội trú có phân bổ trong phạm vi ca (`LichSuViTriApi.theoViTriTrongNgay` cắt theo giờ ca) trừ người vắng suốt ca (`khoangTrangThai` của 001 phủ cả ca); bán trú có lịch đến trùng ca (`HopDongDocApi.lichDenTaiNgay`);
  - trọng số: mức chăm sóc hiện hành (`MucChamSocApi.hienHanh`) ánh xạ qua CFG-M09-12; yêu cầu đổi mức chưa áp dụng không tính (CHK049);
  - nhân viên được tính: có tên trong ca, không vắng, không nghỉ có duyệt, có vai trò Điều dưỡng hoặc Nhân viên chăm sóc, mỗi người một lần;
  - ngưỡng: CFG-M09-01 theo mẫu ca (kiểu `BANG`); không có nhân viên được tính mà có người dự kiến → dưới ngưỡng.
- Mỗi lần tính ghi `lan_tinh_ty_le` (nhóm 3): ca, thời điểm, sự kiện kích hoạt, tổng trọng số, số nhân viên, tỷ lệ, ngưỡng, đạt/không đạt (FR-037). Báo cáo ca đã qua lấy lần tính cuối trước khi ca kết thúc và dấu "từng không đạt" (Q-200) qua `NhanSuThongKeApi`.
- **Khi nào tính lại** (FR-035): lưu ca Nháp; công bố; vắng ca, bổ sung, nghỉ việc; lệnh 015 qua port; listener sau commit `MucChamSocDaDoi`, `PhanBoDaBatDau`, `PhanBoDaDong`, `DaChuyenGiuong`, `NguoiCaoTuoiDaChuyenTrangThai` cho các ca chưa đóng của tầng liên quan (từ thời điểm hiệu lực).
- **Cảnh báo** (FR-036): lịch Nháp: trả trong phản hồi; lịch đã công bố: chuyển từ đạt sang không đạt thì phát `CaDuoiNguongPhucVu` (Trưởng tầng; thêm Quản lý viện khi ca bắt đầu trong CFG-M09-08); cột `ca.dang_duoi_nguong` chống phát lại khi còn không đạt. Job hằng giờ xét ca sắp bắt đầu trong CFG-M09-08 đang dưới ngưỡng mà Quản lý viện chưa được báo (`da_bao_qlv_duoi_nguong_luc`).

---

## N-10. Bàn giao (FR-038 → FR-047, BR-M09-06 → 08, DBR-20, Q-307)

**Quyết định**:
- **Bảng**: `ban_giao` (nhóm 2 cho trạng thái; `UNIQUE (ca_id)`, DBR-20): ca trước, ca sau (tính lúc lập, cập nhật được tới khi xác nhận nếu lịch đổi), người bàn giao, người xác nhận, trạng thái `BAN_NHAP | DA_LAP | CO_Y_KIEN | DA_XAC_NHAN`, `chot_luc`, `xac_nhan_luc`, căn cứ quyền; `muc_ban_giao` (nhóm 3 có bản nháp): nhóm (a) → (g) hoặc `VAN_DE_THEO_DOI`, nguồn (loại, id), người cao tuổi hoặc phòng, mô tả, trạng thái nguồn lúc chốt, ghi chú (người viết), `phan` (`DA_CHOT`, `PHAT_SINH_SAU_KHI_LAP`, `DA_XU_LY_TRONG_CA`); `nhan_dinh_ban_giao` (chung, theo người cao tuổi); `y_kien_ban_giao`, `phan_hoi_y_kien` (nhóm 3).
- **Trigger**: mục `DA_CHOT`, nhận định không đổi khi `ban_giao.chot_luc IS NOT NULL`; mục `PHAT_SINH_SAU_KHI_LAP`, ý kiến, phản hồi chỉ thêm; mọi bảng con không đổi khi `DA_XAC_NHAN`.
- **Ca sau** (FR-038): hàm thuần `XacDinhCaSau(caTruoc, caCungPhamVi, cfg09_09)`: ca có yêu cầu bàn giao cùng phạm vi bắt đầu sau giờ bắt đầu ca trước và không muộn hơn giờ kết thúc; không có thì ca bắt đầu sớm nhất sau giờ kết thúc trong CFG-M09-09; không có thì người xác nhận là Trưởng tầng được giao.
- **Lập** (FR-039): từ `ket_thuc − CFG-M09-04` (hoặc `bat_dau` nếu ca ngắn hơn); người phụ trách ca, hoặc Trưởng tầng khi người phụ trách đã ra khỏi phạm vi ca, vắng, nghỉ việc, hoặc ca thiếu người phụ trách (FR-041, FR-019); tầng chưa có Trưởng tầng thì người thay theo N-08. Không có mục nào được lập sẵn.
- **Danh sách bản ghi nguồn** (`GET /ban-giao/{id}/ban-ghi-nguon`, chỉ đọc): `BanGhiNguonBanGiaoService` gọi song song:
  - (a) `CongViecBanGiaoApi.chuaDong(tang, ca)` (005);
  - (b) `LieuBanGiaoApi.trongCa(tang, ca)` (006);
  - (c) `ChiSoApi.vuotNguongTrongKhoang(tang, batDau, ketThuc)` (007);
  - (d) `CanhBaoBanGiaoApi.dangMo(tang, ca)` (007) và `CanhBaoBanGiaoApi.theoTang` (003, phân bổ chờ xác nhận vùng cách ly);
  - (e) người mới nhập, trở về, bắt đầu vắng trong ca: `LichSuViTriApi.theoViTriTrongNgay` + `khoangTrangThai` (001); quá giờ dự kiến trở về: `LuotVangApi.dangMo` (004); người đang đi chuyến, chuyến quá giờ về: `ChuyenDiNhiemVuPort.chuyenTrongCa` (014);
  - (f) giường Chờ sẵn sàng quá CFG-M03-04: `TinhTrangGiuongApi.choSanSangQuaHan(tang, t)` (003; bổ sung theo plan này);
  - (g) đồ gửi có giá trị Đang giữ do nhân viên có tên trong ca giữ: `DoGuiBanGiaoPort.dangGiu(nhanVienIds)` (013).
  Mỗi dòng mang `daCoMuc` nếu đã có mục trỏ tới nguồn đó. Không ghi gì.
- **Sửa ở Bản nháp** (FR-040): thêm, sửa, bỏ mục, ghi chú, nhận định (người bàn giao, Điều dưỡng có tên trong ca); `If-Match`. Bỏ mục nhóm (c), (e) bị chặn (Q-88). Mục có nguồn hiển thị trạng thái hiện hành (`trangThaiHienHanh` của 005, 006; trạng thái cảnh báo, sự cố của 007; v.v.); người bàn giao ghi "đã xử lý" (chuyển `phan = DA_XU_LY_TRONG_CA`) hoặc bỏ. Thêm mục công việc gọi `CongViecBanGiaoApi.daGhiVaoBanGiao` (MANDATORY).
- **Hoàn tất** (FR-041): có nhận định chung; mọi mục nghiêm trọng có ghi chú (hàm thuần `MucNghiemTrong`: cảnh báo, sự cố Khẩn cấp/Trung bình; liều Bỏ lỡ, Từ chối; công việc Bắt buộc đang Quá hạn — trạng thái hiện hành lúc hoàn tất); thiếu thì 409 `THIEU_GHI_CHU_MUC_NGHIEM_TRONG` kèm danh sách. Đặt `chot_luc`, lưu trạng thái nguồn lúc chốt; ca `CHO_BAN_GIAO` → `DA_DONG`; phát `BanGiaoDaLap` cho người xác nhận.
- **Phát sinh sau khi lập**: người bàn giao hoặc Trưởng tầng thêm mục `PHAT_SINH_SAU_KHI_LAP` tới khi xác nhận (FR-041). Danh sách bản ghi nguồn phát sinh sau `chot_luc` hiển thị cho người xác nhận (FR-045).
- **Xác nhận** (FR-044): người phụ trách ca sau trong phạm vi ca sau, hoặc Trưởng tầng của phạm vi (hoặc người thay theo N-08). Trong một transaction:
  1. khóa `ban_giao`, kiểm tra còn `DA_LAP`;
  2. `CongViecCaApi.khiXacNhanBanGiao(banGiaoId, caTruoc, caSau, congViecIdsTrongBanGiao, t)` (005 FR-047, FR-047a);
  3. `CanhBaoBanGiaoApi.khiXacNhanBanGiao(banGiaoId, caTruoc, caSau, tang)` (007 FR-037);
  4. liều Mang theo chờ ghi nhận: 006 tự hiển thị cho Điều dưỡng phụ trách ca mới khi đọc `DieuDuongCaPort.banGiaoDaXacNhan(caTruoc)` (006 D-09); không cần lời gọi;
  5. `DA_XAC_NHAN`, nhật ký người xác nhận, căn cứ quyền.
  Một bước lỗi thì rollback, bàn giao giữ `DA_LAP` (FR-044).
- **Gửi ý kiến, bổ sung và gửi lại** theo bảng FR-042; bổ sung bắt buộc phản hồi cho từng ý kiến; ca `CHO_BAN_GIAO` → `DA_DONG` khi gửi lại.
- **Tạm nhận** (FR-044a): 005, 006, 007 tự làm từ giờ bắt đầu ca sau dựa vào `NhanSuCaPort.banGiaoDaXacNhan`, `DieuDuongCaPort.banGiaoDaXacNhan`, `NhanSuCanhBaoPort.caDaBatDauChuaXacNhanBanGiao` (đã thiết kế ở 005 C-10, 006 D-09, 007 S-18). 008 chỉ cung cấp dữ liệu và phát `CaDaBatDau`.
- **Nhắc** (FR-039, FR-043): job theo phút: mốc lập (`BanGiaoChuaLap`, một lần mỗi ca), hết ca còn Bản nháp hoặc Có ý kiến (`BanGiaoChuaHoanTatKhiHetCa`), ca sau + CFG-M09-05 chưa xác nhận (`BanGiaoChuaXacNhan`); cột `da_nhac_*` trên `ca`.
- **Đính chính** (FR-046): `DinhChinhDuocProvider` loại `M09.BAN_GIAO` (nội dung mục, nhận định, ý kiến); `choPhep`: người bàn giao, người xác nhận, Trưởng tầng, Người phụ trách ca của phạm vi (000 cách a, b); `sauKhiDinhChinh` phát thông báo FR-057; không thay đổi tác động FR-044.
- **Ngoại tuyến** (FR-060): hoàn tất, ý kiến, bổ sung, xác nhận bắt buộc trực tuyến (không có `NgoaiTuyenHandler`); app nhân viên lưu bản xem bàn giao trong phạm vi ca hiện tại và xóa khi hết phạm vi ca.

**Lý do**: Q-307 bỏ bản nháp tự lập; danh sách bản ghi nguồn là phép đọc. Xác nhận phải cùng transaction với chuyển công việc và cảnh báo (FR-044 "hoặc toàn bộ, hoặc không").

**Phương án đã cân nhắc**: Lưu bản sao danh sách nguồn khi lập: là "lập sẵn" trá hình và lệch khi nguồn đổi.

---

## N-11. Nhiệm vụ và tra cứu ca theo thời điểm (SPI cài cho 002, 003, 005, 006, 007)

**Quyết định**: Một service đọc `TraCuuCaService` (không khóa, chỉ mục `ca (pham_vi, bat_dau, ket_thuc)`, `ca_nhan_vien (nhan_vien_id)`, `phan_cong (ca_id, trang_thai)`) cài:

| SPI | Hàm chính |
| --- | --- |
| `NhiemVuApi` (002) | `nguoiPhuTrachCa(tang, t)`, `truongTangDuocGiao(tang, t)`, `nguoiThayTruongTang(tang, t)`, `caKhongCoTruongTang(caId)`, `phuTrachBoPhan(boPhanId)`, `laPhuTrachBoPhan(nv)`, `truongDoan(chuyenId)`, `nguoiDiCung(chuyenId)` (ủy cho `ChuyenDiNhiemVuPort` của 014), `nguoiNhanTaiTang(tang, caId)` (011 UC-76: TT, ĐD, CS có phân công trong tầng và ca) |
| `PhanCongApi` (002) | `dangHieuLuc(nv, t)`, `lichSu(nv, tu, den)` (FR-037a) |
| `HoSoNhanVienApi`, `GiayPhepApi`, `DanhMucPhamViHanhNgheApi` (002) | N-04, N-05 |
| `ThamChieuViTri` (003) | Phân công Nháp, Tương lai, Hiệu lực; giao trưởng tầng đang/sẽ hiệu lực; ca chưa Đã đóng, Đã hủy của tầng/khu/phòng (003 FR-005, CHK017) |
| `NhanSuCaPort` (005) | Theo chữ ký 005; `duDieuKien` dùng `HieuLucChungChi` |
| `DieuDuongCaPort` (006) | Theo chữ ký 006; `dieuDuongCoCa(tang, t)` = Điều dưỡng có tên trong ca đang diễn ra của tầng, không vắng |
| `NhanSuCanhBaoPort` (007) | Theo chữ ký 007 |

Các hàm "tại thời điểm t" đọc dữ liệu lịch sử (`nguoi_phu_trach_ca`, `ket_thuc_thuc_te`, `vang_luc`), nên cùng cho phạm vi báo cáo khoảng đã qua (002 `boLocLichSu`, FR-037a).

---

## N-12. Quyền và quyền riêng tư (FR-047, FR-048, FR-050 → FR-054, FR-056)

**Quyết định**: Khóa quyền theo bảng FR-048:

| `ChucNang` | Thao tác |
| --- | --- |
| `HO_SO_NHAN_VIEN` | `THUC_HIEN` (QL); `XEM` (TT, giới hạn trường FR-052; nhân viên nhóm (a) của người cùng ca) |
| `DANH_MUC_NHAN_SU` | `CAU_HINH` (QL: mẫu ca, loại giấy phép/đào tạo, phạm vi hành nghề, bộ phận) |
| `LICH_CA_PHAN_CONG` | `THUC_HIEN`, `CONG_BO` (QL toàn viện; TT tầng được giao); `XEM_CUA_MINH` (mọi nhân viên có ca, "P") |
| `GIAO_TRUONG_TANG` | `THUC_HIEN` (QL); `XEM` (TT) |
| `VANG_CA` | `THUC_HIEN` (TT; Người phụ trách ca theo nhiệm vụ, 002 FR-019d) |
| `BAN_GIAO` | `XEM` (QL, BS; CS trong phạm vi phân công); `GHI_CHU` (ĐD có tên trong ca); `HOAN_TAT`, `XAC_NHAN`, `Y_KIEN` (TT tầng được giao; ĐD khi là Người phụ trách ca; người thay Trưởng tầng theo Q-90) |

- Nhân viên chăm sóc xem bàn giao chỉ phần gắn người trong phạm vi phân công và nhận định chung (FR-047); trường sức khỏe che theo 002 (FR-050). Hành chính, Bếp, Kế toán không xem bàn giao.
- Lý do vắng ca chỉ người ghi, Trưởng tầng của tầng, Quản lý viện xem (FR-053): cột mang `@NhomTruong(LY_DO_NHAN_SU)`.
- **Nhật ký xem bàn giao**: không ghi lần xem (000 FR-043a, Q-03); hoàn tất, xác nhận, gửi ý kiến, bổ sung là lệnh nên có nhật ký (plan điểm báo lại 1).
- Căn cứ quyền ghi vào nhật ký mỗi lệnh (FR-056): `VAI_TRO`, `NHIEM_VU_NGUOI_PHU_TRACH_CA`, `THAY_TRUONG_TANG`, `TAM_NHAN`, `LOI_MOI` (015); Quản lý viện tra cứu danh sách căn cứ khác `VAI_TRO` qua `GET /nhat-ky-can-cu-nhiem-vu`.

---

## N-13. Bộ lập lịch của 008 (FR-003, FR-008, FR-009, FR-022, FR-024, FR-027, FR-029, FR-036, FR-039, FR-043)

**Quyết định**:

| Job | Chu kỳ | Việc |
| --- | --- | --- |
| `ViecTheoPhutM09` | Mỗi phút | Ca: Đã công bố → Đang diễn ra, Đang diễn ra → Đã đóng / Chờ bàn giao; phân công Tương lai → Hiệu lực → Đã kết thúc; nghỉ việc có hiệu lực tương lai; chứng chỉ hết hiệu lực theo mốc; nhắc bàn giao (mốc lập, hết ca, ca sau + CFG-M09-05); người cao tuổi chưa có người phụ trách tại giờ bắt đầu ca; tầng chưa có Trưởng tầng (đầu ca) |
| `ViecHangGioM09` | Mỗi giờ | Ca sắp bắt đầu trong CFG-M09-08 đang dưới ngưỡng |
| `ViecHangNgayM09` | 00:10 | Đào tạo sắp hết hạn (CFG-M09-03); bộ phận chưa có Phụ trách bộ phận |

- Mỗi việc ghi cột "đã làm lúc" trên dòng nguồn để chạy lại không gửi trùng (NFR-04); chạy bù khi khởi động, thời điểm dự kiến ghi cùng thời điểm thực tế.
- Job chỉ chuyển trạng thái theo giờ và nhắc; không lập ca, phân công, bàn giao (000 R-10, Q-306).

---

## N-14. Sự kiện và thông báo (FR-058, FR-059)

**Quyết định**: Mức theo FR-058 (spec đã nêu đủ):

| Mức | Sự kiện |
| --- | --- |
| Nhẹ | `LichCaDaCongBo`; `CaDaThayDoi` (bổ sung, chuyển người phụ trách, hủy ca); `ChungChiSapHetHan`; tỷ lệ dưới ngưỡng khi lịch còn Nháp (chỉ trả trong phản hồi, không gửi) |
| Trung bình | `BanGiaoChuaLap`; `BanGiaoChuaHoanTatKhiHetCa`; `BanGiaoChuaXacNhan`; `BanGiaoCoYKien`; `BanGiaoDaLap` (người xác nhận); `BanGiaoDangDoDaChuyen`; `CaThieuNguoiPhuTrach`; `TangChuaCoTruongTang`; `NhanVienVangCa`; `NhanVienNghiViec`; `PhanCongKetThucDoChungChi`; `CaDuoiNguongPhucVu`; `NctChuaCoNguoiPhuTrach`; `BoPhanChuaCoPhuTrach`* |
| Không gửi | `CaDaBatDau`, `CaDaKetThuc`, `NhanVienDaBoSungVaoCa`, `PhanCongDaDoi` (dùng cho 005, 006, 007, 013, 016 làm khóa bỏ trùng hoặc cập nhật) |

\* `BoPhanChuaCoPhuTrach` (FR-001a) spec không nêu mức; plan xếp Trung bình vì chặn lệnh của bộ phận (plan điểm báo lại 4).

- Nội dung không nêu lý do vắng ca, nghỉ việc, thu hồi (FR-053); kênh ngoài ứng dụng không chứa họ tên người cao tuổi, sức khỏe (FR-059). Sự kiện mang mức, nhóm người nhận (`TRUONG_TANG(tang)`, `NGUOI_PHU_TRACH_CA(ca)`, `QUAN_LY_VIEN`, `NHAN_VIEN(id)`), khóa sự kiện (009 FR-005).
- Spec không phát thông báo Khẩn cấp (FR-058).

---

## N-15. Lưu giữ, nhật ký, kiểm thử

**Quyết định**:
- **Lưu giữ** (FR-047, FR-057, 000 R-11): bàn giao và bảng con giữ tới khi hết hạn của mọi người cao tuổi có mục trong bàn giao (`LuuTruProvider` trả `THEO_HO_SO` với tập người; chuỗi tham chiếu là cả bàn giao, không loại bỏ từng phần); hồ sơ nhân viên, chứng chỉ, ca, phân công, lần tính tỷ lệ: `KHONG_GAN_HO_SO` (CFG-M15-04) và không ngắn hơn bản ghi tham chiếu tới chúng.
- **Nhật ký** (FR-055): mọi lệnh nhóm 2 có nhật ký kèm căn cứ quyền; thay đổi trên lịch Nháp không ghi từng lần, công bố lưu `ban_cong_bo_lich`; thay đổi do hệ thống ghi người thực hiện "Hệ thống" và sự kiện kích hoạt.
- **Kiểm thử** (000 R-14, constitution XV):
  - test bảng cho hàm thuần `HieuLucChungChi`, `ChuoiCaLienTuc`, `XacDinhNhanVienChinh`, `XacDinhCaSau`, `TinhTyLePhucVu`, `MucNghiemTrong`;
  - SC-005: bộ 200 lần xếp ca và phân công dựng từ bảng tính tay;
  - Test tích hợp MySQL cho `UNIQUE (ca_id)`, trigger bàn giao, trigger trạng thái nhân viên, `khoa_hien_hanh` bộ phận;
  - test đồng thời: hai phân công chính cùng đối tượng; hai giao trưởng tầng chồng; xếp một nhân viên vào hai ca cùng lúc; Xác nhận và Gửi ý kiến cùng lúc;
  - đồng hồ giả lập cho CFG-M09-02 → 05, 08, 09, CFG-M15-07, ca qua đêm, ca qua tháng, hiệu lực tới hết ngày (SC-006);
  - bản giả `DoGuiBanGiaoPort`, `ChuyenDiNhiemVuPort`, `KiemTraCongBoMoRong` có công tắc `/api/v1/kiem-thu/gia-lap/{module}`;
  - Flutter: xem bàn giao ngoại tuyến và xóa khi hết phạm vi ca; chặn hoàn tất/xác nhận khi mất kết nối.
- Tên test theo BR/FR (`BR_M09_03_ChongCaIT`, `FR_044_XacNhanBanGiaoNguyenTuIT`).
