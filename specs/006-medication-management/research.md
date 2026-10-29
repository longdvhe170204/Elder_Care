# Research: Quản lý thuốc (006)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

Mỗi mục gồm Quyết định, Lý do, Phương án đã cân nhắc. Quyết định nền R-01 → R-14 (000), A-01 → A-14 (002), H-01 → H-15 (001), G-01 → G-16 (003), T-01 → T-16 (004), C-01 → C-17 (005) được dẫn lại, không quyết lại. Không mục nào đổi nghiệp vụ (constitution IX); chỗ spec hoặc nguồn còn thiếu ghi ở "Điểm cần báo lại" của plan.

---

## D-01. Vị trí trong backend

**Quyết định**: Feature 006 là module M07 (mục 11). Gói `com.eldercare.backend.medication` có các gói con:

| Gói | Nội dung | Mục spec |
| --- | --- | --- |
| `danhmuc` | Danh mục thuốc, hoạt chất | A |
| `donthuoc` | Đơn thuốc, kiểm tra trùng hoạt chất và dị ứng, đổi liều, gia hạn, ngừng, tạm dừng | B |
| `lichthuoc` | Tính lịch (hàm thuần), vật hóa liều khi tới đầu cửa sổ, bảng chuyển liều | C |
| `lieu` | Xác nhận liều, liều chung của tầng, phản ứng, từ chối liên tiếp, xem lại thuốc kiểm soát đặc biệt | C, E |
| `prn` | Lần dùng PRN, giới hạn khoảng cách và số lần | D |
| `doichieu` | Phiếu đối chiếu, dòng, xác nhận hai người, đính chính phiếu | F |
| `thuocgui` | Lô thuốc gia đình gửi, biến động số lượng, mốc nhắc, hoàn trả, hủy | G |
| `mangtheo` | Khoảng vắng của thuốc, lần giao và nhận lại thuốc mang theo, lệnh "Không mang thuốc" | H (FR-048 → FR-048b) |
| `tiepnhanthuoc` | Lần tiếp nhận thuốc cung ứng theo yêu cầu mua sắm loại "thuốc" (bổ sung 2026-10-11, D-21) | I (FR-054, FR-055) |
| `trangthai` | Bean `ThamGiaChuyenTrangThai` của 001; listener sự kiện của 001, 003, 004 | H (FR-034, FR-047) |
| `lich` | Job theo phút và job hằng ngày (D-19) | C, F, G |
| `api` | Giao diện, DTO, sự kiện, SPI công khai | FR-050 → FR-052 |

Bảng `thuoc_nct` (một dòng mỗi hồ sơ người cao tuổi) là đích khóa tuần tự cho các lệnh của 006 cần nhìn toàn bộ đơn của một người (nhập đơn, đổi liều, phiếu đối chiếu, lần dùng PRN, giao thuốc mang theo) và giữ hạn đối chiếu hiện hành (D-13), giống `luu_tru_nct` của 004, `cham_soc_nct` của 005. *(R-15: `thuoc_nct`, `luu_tru_nct`, `cham_soc_nct` → `trang_thai_nct`)*

**Lý do**: Constitution X. Khóa dòng `nguoi_cao_tuoi` của M01 là truy cập dữ liệu module khác.

---

## D-02. Chiều phụ thuộc giữa module

**Quyết định**: Theo mẫu 001 H-02, 005 C-02: module làm plan trước khai báo SPI trong gói `api`, module làm plan sau cài.

| Cặp | Chiều mã | Cách giữ một chiều |
| --- | --- | --- |
| 006 → 000, 002 | 006 dùng | `NhatKyApi`, `ThamSoApi`, `DinhChinhApi`, `TepApi`, `QuyenApi`, `PhamViApi`, `QuyenXemTruongApi`, `NguoiDungHienTai`; cài `NgoaiTuyenHandler`, `DinhChinhDuocProvider`, `PhanLoaiTruongProvider`, `LuuTruProvider`, `GanPhamViProvider`, `QuyenTepProvider` |
| 006 ↔ 001 | 006 → 001 | Cài `ThamGiaChuyenTrangThai`; đọc `NguoiCaoTuoiApi`, `MucSucKhoeApi.hieuLuc`, `HoSoSucKhoeApi.banDau`, `DiNguyenApi`, `BanDongYApi.baoGom`, `QuanHeNguoiThanApi` (012 cài); nghe `DiUngDaGhiNhan`, `HoSoSucKhoeBanDauDaDoi` (bổ sung, D-13) |
| 006 → 003 | Đọc | `ViTriApi.hienTai` (tầng/khu của người cao tuổi lúc xét liều) |
| 006 ↔ 004 | 006 → 004 | Cài `MucViecSauQuaDoiPort` (`THUOC_GUI`); gọi `DanhSachViecSauQuaDoiApi.capNhat`; đọc `DonGiaApi.thuocNguonVien` chỉ để hiển thị cảnh báo thiếu mục vật phẩm (FR-003); nghe `LuotVangDaGiaHan` |
| 006 → 005 | 006 cài SPI của 005 | `LieuThuocChecklistPort`, `ThoiKhoaBieuNguonPort` (005 module-interfaces mục 2.1; giữ đúng chữ ký) |
| 006 ↔ 007, 008, 010, 012, 014, 016 | Các module này → 006 | Họ làm plan sau. 006 cần dữ liệu của 008 (ca, phân công điều dưỡng, Người phụ trách ca, bàn giao đã xác nhận), 014 (chuyến đi), 010 (đề nghị mua hộ) qua SPI khai báo ở `medication.api`: `DieuDuongCaPort` (008), `ChuyenDiThuocPort` (014), `DeNghiMuaHoPort` (010). 007, 008, 012, 016 đọc API của 006; 014 gọi `MangThuocChuyenDiApi` (MANDATORY) |
| 006 → 007, 009, 010 | Sự kiện | Yêu cầu tạo/đóng cảnh báo, thông báo, sự kiện tính phí đi qua outbox ghi cùng transaction (D-17) |
| 006 ↔ 019 (M11) *(bổ sung 2026-10-11, Q-337)* | M11 → M07 | 019 cài SPI `YeuCauThuocCungUngPort` khai báo ở `medication.api` (cùng chiều với `DeNghiMuaHoPort` mà 010, cùng M11, đã cài) để 006 đọc dòng của yêu cầu mua sắm loại "thuốc" đã ghi đã mua; 019 đọc `DonThuocApi.donNguonVienChuaKetThuc` và `TiepNhanThuocCungUngApi` của `medication.api`. M07 không gọi `billing.api`, nên không có vòng (D-21) |

**Lý do**: Modulith `verify()` từ chối vòng (000 R-02). 007 cần "thuốc đang dùng" cho thẻ khẩn cấp (FR-050) nên phụ thuộc 006; nếu 006 gọi đồng bộ 007 để tạo cảnh báo thì có vòng.

**Phương án đã cân nhắc**: Dùng lại `NhanSuCaPort` của `care.api` cho dữ liệu ca: bị loại vì 006 cần thêm "Điều dưỡng có ca tại tầng" và "người được phân công tại thời điểm" mà 005 không khai báo; mở rộng SPI của 005 làm hợp đồng 005 phụ thuộc nhu cầu của 006.

---

## D-03. Khóa và đồng thời

**Quyết định**:
- **Thứ tự khóa toàn hệ thống** (nối 005 C-03): `nguoi_cao_tuoi` (001) → `luu_tru_nct` (004) → `vi_tri_nct`, `giuong` (003) → `cham_soc_nct` (005) → `thuoc_nct` (006) → `don_thuoc` (theo `id` tăng) → `lieu_thuoc` → `thuoc_gia_dinh_gui` (theo `id` tăng) → `phieu_doi_chieu`. Bean của 006 chạy sau 005 theo `@Order` của 001. *(R-15: `luu_tru_nct`, `vi_tri_nct`, `cham_soc_nct`, `thuoc_nct` → `trang_thai_nct`)*
- **Xác nhận liều**: thêm hoặc khóa dòng `lieu_thuoc` theo khóa `(don_thuoc_id, thoi_diem_du_kien)` (`UNIQUE`, DBR-13). Lệnh thứ hai trên liều đã đóng trả 409 `LIEU_DA_XAC_NHAN` kèm `dongBoi`, `dongLuc` và `noiDungBiTuChoi` để client giữ và chuyển thành đính chính (Edge Case "hai điều dưỡng cùng xác nhận"). Không dùng `If-Match`, giống 005 C-03.
- **Lần dùng PRN, giao thuốc mang theo, lệnh đơn thuốc, phiếu đối chiếu**: khóa `thuoc_nct` của người đó trước, để kiểm giới hạn PRN theo chuỗi thay thế (FR-025), trùng hoạt chất (FR-007) và "không có phiếu Mở" (FR-037) trên dữ liệu không đổi giữa lúc kiểm và lúc ghi. *(R-15: `thuoc_nct` → `trang_thai_nct`)*
- **Số lượng thuốc gia đình gửi**: khóa dòng lô `thuoc_gia_dinh_gui`; biến động và số lượng còn đổi trong cùng transaction (D-14).
- **Job** (D-19) khóa từng dòng liều, đơn, phiếu như lệnh, nên lệnh và job không cùng đóng một liều.

**Lý do**: DBR-13 đòi một lần xác nhận mỗi liều; FR-025, FR-007 là kiểm tra trên tập bản ghi, cần tuần tự theo người.

---

## D-04. Danh mục thuốc (mục A, FR-001 → FR-003)

**Quyết định**:
- `thuoc` (nhóm 1, mẫu 000 mục 1.1): `ten`, `ham_luong`, `dang_bao_che`, `duong_dung_mac_dinh`, `don_vi_dung`, `cua_so_truoc_phut`, `cua_so_sau_phut` (rỗng thì CFG-M07-01), `kiem_soat_dac_biet`, `trang_thai_hieu_luc`. Bảng con `thuoc_hoat_chat (thuoc_id, di_nguyen_id)` trỏ danh mục dị nguyên nhóm thuốc của 001 (`DiNguyenApi.diNguyenHieuLuc(THUOC)`), vì FR-001 đòi hoạt chất "liên kết danh mục dị nguyên". *(R-15: `thuoc` → `danh_muc_chung`)*
- Quyền: chỉ Quản lý viện tạo, sửa, Ngừng hiệu lực (Phụ lục 27 dòng "Danh mục thuốc": QL C; BS, ĐD X). Thuốc đã có đơn tham chiếu thì không xóa (khóa ngoại `don_thuoc.thuoc_id`, `ON DELETE RESTRICT`).
- "Sửa hoạt chất hoặc cửa sổ chỉ áp cho đơn tạo sau và liều chưa tới đầu cửa sổ" (FR-002): đơn chép tập hoạt chất vào `don_thuoc_hoat_chat` lúc lưu; cửa sổ được chốt vào dòng liều khi liều được vật hóa ở đầu cửa sổ (D-06). Liều chưa vật hóa luôn tính theo giá trị hiện hành. *(R-15: `don_thuoc_hoat_chat` → cột `JSON` của `don_thuoc`)*
- Đơn giá (FR-003): 006 không lưu giá. Mục vật phẩm của thuốc nguồn viện là `dich_vu` loại `THUOC_NGUON_VIEN` mang `thuoc_id` của 004 (004 research T-04); 010 tra `DonGiaApi.thuocNguonVien(thuocId)`. Màn danh mục thuốc hiển thị dấu "chưa có mục vật phẩm" khi hàm này trả rỗng, để Quản lý viện bổ sung ở 004. Không chặn kê đơn. *(R-15: `dich_vu` → `danh_muc_chung`)*

**Lý do**: Spec FR-001 → FR-003; một danh mục giá chung ở 004.

---

## D-05. Đơn thuốc (mục B, FR-004 → FR-017, BR-M07-05 → 08, 15, 16, DBR-14)

**Quyết định**:
- **Bảng** `don_thuoc` (nhóm 2): trường FR-004; tần suất có cấu trúc `kieu_tan_suat` (`CAC_MOC`, `MOI_N_GIO`, `NGAY_TRONG_TUAN`), `cac_moc` (JSON danh sách `HH:mm`), `n_gio`, `moc_dau` (`DATETIME`, với `MOI_N_GIO`), `ngay_trong_tuan`; với PRN `khoang_cach_toi_thieu_phut`, `so_lan_toi_da_24h`; `bat_dau_luc`, `ngay_ket_thuc`, `ngung_tu` (thời điểm ngừng đã hẹn), `thay_the_cho_id` (`UNIQUE`, FR-016a), `dau_cho_tiep_nhan`, `ly_do_tam_dung` (`LENH`, `CHO_DOI_CHIEU`).
- **Lệnh** (một endpoint mỗi lệnh, constitution XI): Nhập đơn `POST /nguoi-cao-tuoi/{nctId}/don-thuoc`; Tạm dừng, Tiếp tục, Ngừng, Đổi liều, Gia hạn `POST /don-thuoc/{id}/<lenh>`; sửa ghi chú `PATCH /don-thuoc/{id}/ghi-chu` với `If-Match` (FR-014, là trường duy nhất sửa được). Không có endpoint sửa thuốc, liều, tần suất, trạng thái; trigger `trg_don_thuoc_khoa` chặn `UPDATE` các cột đó (DBR-14).
- **Quyền kê** (FR-005, FR-006, FR-013):
  - đơn nội bộ: `QuyenApi.coQuyen(nd, (DON_THUOC, CHUYEN_MON), doiTuong)`; 002 A-09 kiểm giấy phép Bác sĩ đúng phạm vi và phạm vi khám chữa bệnh của cơ sở (qua `GiayPhepCoSoApi` của 007) ở mục mặc định "chẩn đoán và kê đơn nội bộ". Thiếu thì 403 `DIEU_KIEN_PHAP_LY`;
  - đơn bên ngoài: `(DON_THUOC, TAO)` cho Bác sĩ, Điều dưỡng (chú thích ⁴); bắt buộc khối `coSoKe` (tên cơ sở, người kê, ngày kê, ảnh tùy chọn qua `TepApi`);
  - lệnh của Điều dưỡng trên đơn bên ngoài (Tạm dừng, Tiếp tục, Ngừng, Gia hạn, Đổi liều sang đơn bên ngoài) bắt buộc khối `canCuCoSoKe` (BR-M07-16): tên cơ sở, người chỉ định, ngày, hình thức (`DON_MOI`, `GIAY_RA_VIEN`, `DIEN_THOAI` kèm người gọi, người trả lời, thời điểm), tệp đính kèm khi hình thức có giấy tờ. Thiếu thì 422 `THIEU_CAN_CU_CO_SO_KE`. Lưu ở bảng `can_cu_co_so_ke` (nhóm 3) trỏ lệnh. *(R-15: `can_cu_co_so_ke` → `su_kien_chung`)*
- **Kiểm tra trùng hoạt chất và dị ứng** (FR-007, FR-008, BR-M07-06): hàm thuần `KiemTraDonMoi(hoatChatMoi, donDangCo, diUng, donBiThayThe)`:
  - (a) giao tập hoạt chất với mọi đơn `HIEU_LUC`, `TAM_DUNG`, `CHO_HIEU_LUC` của người đó, trừ đơn đang được thay thế;
  - (b) giao hoạt chất với mục dị ứng Hiệu lực chọn từ danh mục (`MucSucKhoeApi.hieuLuc(nctId, DI_UNG)` có `diNguyenId`);
  - mục dị ứng "khác" trả riêng trong `canXemXet[]` với dấu `khongKiemTraTuDong`, không chặn.

  Lệnh nhận `xacNhanVanDe[] {maVanDe, lyDo}`. Còn vấn đề chưa có lý do thì 409 `CAN_XAC_NHAN_VAN_DE` kèm danh sách (đơn trùng, mục dị ứng và mức độ); client hiện và gửi lại. Danh sách vấn đề và lý do lưu ở `van_de_don_thuoc` (nhóm 3). Kiểm tra chạy dưới khóa `thuoc_nct` nên không có hai đơn trùng cùng lọt. *(R-15: `van_de_don_thuoc` → `su_kien_chung`; `thuoc_nct` → `trang_thai_nct`)*
- **Kiểm tra lại khi có dị ứng mới** (FR-012): listener `DiUngDaGhiNhan(mucId, nctId, diNguyenId)` của 001 (sau commit, idempotent theo `mucId`) chạy (b) trên đơn `HIEU_LUC`, `TAM_DUNG`, `CHO_HIEU_LUC`; mỗi đơn xung đột phát `YeuCauCanhBao(XUNG_DOT_DI_UNG, TRUNG_BINH, donId)`; rồi gọi `MucSucKhoeApi.daKiemTra(mucId, THUOC)` (001 BR-M01-12). Không ngừng đơn.
- **Trạng thái lúc nhập** (bảng FR-011): `bat_dau_luc <= bây giờ` thì `HIEU_LUC`, ngược lại `CHO_HIEU_LUC`. Người cao tuổi phải Đang lưu trú, Tạm vắng hoặc Điều trị tại bệnh viện (`NguoiCaoTuoiApi.trangThai`), và không có phiếu đối chiếu `MO`, `CHO_XAC_NHAN` (FR-037; ngoại lệ: lệnh Ngừng đơn).
- **Đổi liều / đổi thuốc** (FR-016, BR-M07-07, 15, DBR-14): một transaction tạo đơn thay thế (qua `KiemTraDonMoi` với `donBiThayThe`), đặt `thay_the_cho_id`, đặt `ngung_tu` của đơn cũ bằng thời điểm hiệu lực của đơn thay thế. Hiệu lực ngay: đơn cũ `DA_NGUNG` trong cùng lệnh, đơn thay thế `HIEU_LUC`. Hiệu lực tương lai: đơn cũ giữ `HIEU_LUC` với `ngung_tu`; đơn thay thế `CHO_HIEU_LUC`; job chuyển cả hai cùng một transaction tại mốc (D-19). Liều chưa đóng của đơn cũ từ mốc chuyển `DA_HUY` (dòng đã vật hóa) hoặc được tính là Đã hủy (chưa vật hóa, D-06).
- **Hủy thay đổi chưa hiệu lực** (BR-M07-15): Ngừng đơn thay thế còn `CHO_HIEU_LUC` thì trong cùng lệnh xóa `ngung_tu` của đơn cũ (cột nhóm 2, ghi nhật ký trước/sau); lịch thuốc tự tính lại liều đơn cũ. Ngừng đơn cũ sớm hơn mốc thì không đổi đơn thay thế; phản hồi kèm `canhBao: KHOANG_KHONG_CO_LIEU {tu, den}`.
- **Gia hạn** (FR-015): đặt `ngay_ket_thuc` mới muộn hơn hoặc rỗng, bắt buộc lý do; lịch sử qua nhật ký. Rút ngắn dùng Ngừng đơn với `ngungTu` tương lai.
- **Nhắc hết hạn và Hết hạn** (FR-017, BR-M07-08): job hằng ngày (D-19). Nhắc khi `ngay_ket_thuc − hôm nay = CFG-M07-04` (một lần, cột `da_nhac_het_han_luc`); người nhận: người kê với đơn nội bộ; với đơn bên ngoài, nhóm `BAC_SI_TOAN_VIEN` (mọi tài khoản Hoạt động vai trò Bác sĩ, CHK014; 009 phân giải nhóm) và Điều dưỡng phụ trách. Hết ngày kết thúc thì `HET_HAN` từ `HIEU_LUC` hoặc `TAM_DUNG`.
- **Dấu "chưa có thuốc"** (FR-009): suy ra lúc đọc cho đơn nguồn `GIA_DINH_GUI` không có lô `DUOC_SU_DUNG` gắn; không lưu cột.

**Lý do**: Bảng FR-011 là căn cứ; một hàm kiểm tra dùng chung cho nhập đơn, đổi liều, dòng phiếu đối chiếu (FR-007 "kể cả đơn thay thế và đơn tạo từ phiếu").

**Phương án đã cân nhắc**: Tách "đơn" và "phiên bản đơn" để đổi liều là phiên bản mới: không khớp 11.1 (hai đơn liên kết "thay thế cho") và DBR-14.

---

## D-06. Lịch thuốc tính và vật hóa liều (mục C, FR-018 → FR-022, BR-M07-01, Q-306)

**Quyết định**:
- **Không lập sẵn bản ghi liều** (Q-306, SC-001). Lịch thuốc là hàm thuần `LichThuoc.tinh(don, tu, den, boiCanh)` trả `LieuDuKien(donId, thoiDiemDuKien, cuaSo, trangThaiTinh)`:
  - thời điểm từ `ChuoiThoiDiemDon`: `CAC_MOC` lặp mỗi ngày; `NGAY_TRONG_TUAN` lọc theo ngày; `MOI_N_GIO` là chuỗi liên tục từ `moc_dau` qua các ngày (FR-046 nói tới "mỗi N giờ không chia hết 24 giờ"; Edge Case CHK036: đơn thay thế tự nhập mốc đầu); chỉ lấy thời điểm trong `[bat_dau_luc, cuối ngày ngay_ket_thuc]`;
  - cửa sổ: cửa sổ riêng của thuốc, nếu không thì `ThamSoApi.doc(CFG-M07-01)`; cửa sổ qua nửa đêm không bị cắt;
  - `boiCanh` gồm trạng thái đơn, `ngung_tu`, khoảng vắng của thuốc (D-15), dấu "chờ tiếp nhận".
- **Trạng thái tính** cho liều chưa vật hóa (dòng "—" của bảng FR-021): thời điểm sau `ngung_tu` hoặc sau thời điểm ngừng của đơn → Đã hủy (lý do theo nguyên nhân); đơn `TAM_DUNG` hoặc thời điểm trong khoảng vắng không mang thuốc, bệnh viện → Tạm dừng; trong khoảng đi có mang thuốc → Mang theo; còn lại → Chưa đến giờ, Đến giờ (đã qua đầu cửa sổ). Đơn PRN không có liều (FR-010).
- **Vật hóa** dòng `lieu_thuoc` (nhóm 2 do hệ thống điều khiển) chỉ khi:
  1. job theo phút tới đầu cửa sổ của liều: thêm dòng với trạng thái tính lúc đó (`DEN_GIO`, `TAM_DUNG` hoặc `MANG_THEO`), hoặc chuyển dòng `CHUA_DEN_GIO` đã có (trường hợp 3 sau khi trở về sớm) sang `DEN_GIO`; chốt `cua_so_tu`, `cua_so_den` (Edge Case "chốt khi tới đầu cửa sổ");
  2. lệnh ghi nhận trên liều chưa có dòng (liều Mang theo ghi trước đầu cửa sổ, FR-027): dòng được thêm trong transaction của lệnh;
  3. lần giao thuốc mang theo gắn liều (D-15): dòng được thêm ở trạng thái `MANG_THEO` để giữ liên kết.

  Ba trường hợp đều là tác động theo giờ hoặc kèm lệnh của actor, không phải lập sẵn (000 FR-010a). Số dòng ở quy mô 300 người khoảng 1.800 mỗi ngày, chỉ cho liều đã tới hoặc đã qua.
- **Đọc lịch** (checklist, lịch thuốc người cao tuổi, thời khóa biểu): ghép kết quả `LichThuoc.tinh` với dòng đã vật hóa theo khóa `(don_thuoc_id, thoi_diem_du_kien)`; dòng có thì dùng dòng.
- **Mã liều trên API**: `lieuKey = {donThuocId}-{yyyyMMddHHmm}` (thời điểm dự kiến theo Asia/Ho_Chi_Minh). Endpoint nhận `lieuKey` cho liều chưa vật hóa; backend kiểm `thoiDiemDuKien` có thuộc `ChuoiThoiDiemDon` (sai thì 404).
- **Duy nhất** (FR-019, DBR-13): `UNIQUE (don_thuoc_id, thoi_diem_du_kien)` trên `lieu_thuoc`.
- **Phản ánh ngay** (FR-018): lưu, gia hạn, tiếp tục đơn hoặc xác nhận phiếu thì lần đọc kế tiếp đã tính theo dữ liệu mới, không có bước sinh lại.

**Lý do**: 11.2 và BR-M07-01 sau Q-306. Vật hóa ở đầu cửa sổ giữ được "chốt cửa sổ", lưu được trạng thái do bộ lập lịch chuyển (FR-018 câu "được lưu khi liều đổi trạng thái") và là chỗ cho xác nhận, đính chính, báo cáo trỏ tới.

**Phương án đã cân nhắc**:
- Sinh liều trước N ngày (CFG-M07-03 cũ): trái Q-306, CFG-M07-03 đã bỏ.
- Không vật hóa, chỉ lưu bản ghi xác nhận: Trễ, Bỏ lỡ, Tạm dừng kết thúc không có chỗ lưu người đóng "Hệ thống" (FR-022) và cửa sổ đã chốt.

---

## D-07. Bảng chuyển liều và bộ lập lịch của liều (FR-021 → FR-023, BR-M07-02, 03)

**Quyết định**:
- **Bảng chuyển** cài thành hằng bảng `BangChuyenLieu` đúng bảng FR-021; chuyển ngoài bảng trả 409 `CHUYEN_KHONG_HOP_LE`. Trigger `trg_lieu_thuoc_trang_thai_cuoi` chặn `UPDATE` khi trạng thái cũ là `DA_DUNG`, `TU_CHOI`, `KHONG_THUC_HIEN`, `BO_LO`, `DA_HUY` (FR-022).
- **Tạm dừng kết thúc**: dòng `TAM_DUNG` có `cua_so_den` đã qua được coi là kết thúc; cột sinh `ket_thuc_tam_dung = (trang_thai='TAM_DUNG' AND …)` không dùng được vì phụ thuộc giờ, nên job theo phút đặt `ket_thuc_luc` một lần khi qua cuối cửa sổ, và trigger chặn đổi trạng thái của dòng có `ket_thuc_luc` (FR-022 "MUST NOT chuyển trạng thái nữa").
- **Job theo phút** (D-19) trên dòng đã vật hóa:
  - `DEN_GIO` có `cua_so_den <= bây giờ` → `TRE`, phát `NhacLieuTre(lieuId, nguoiNhan)` một lần (cột `da_nhac_tre_luc`), người nhận theo FR-022: Điều dưỡng phụ trách của ca chứa thời điểm dự kiến; đã hết ca hoặc liều chung thì theo FR-026a (D-09);
  - `TRE` có `tre_luc + CFG-M07-02 <= bây giờ` → `BO_LO`, người đóng "Hệ thống" quy tắc `BR_M07_02`, thêm `ket_qua_lieu` loại `BO_LO` (D-08) và phát `YeuCauCanhBao(BO_LO_LIEU, TRUNG_BINH, lieuId)`.
- **Người đóng** (FR-022): `dong_boi_loai` (`NHAN_VIEN` | `HE_THONG`), `dong_boi_id`, `can_cu_quyen` (`DUOC_PHAN_CONG`, `NGUOI_PHU_TRACH_CA`, `LIEU_CHUNG_TANG`, `MANG_THEO`, `GHI_THEO_BAO_LAI`), `quy_tac_dong` (`BR_M07_02`, `FR_047_TRANG_THAI_CUOI`, `NGUNG_DON`, `DOI_LIEU`, `HET_HAN`, `PHIEU_DOI_CHIEU`).
- **Liều Đến giờ, Trễ lúc người cao tuổi rời viện** (FR-023): bean không đổi các dòng này; điều dưỡng đóng bằng xác nhận hoặc Không thực hiện "vắng mặt".

**Lý do**: SC-004 đòi chuyển và nhắc trong 1 phút; mẫu job theo phút của 005 C-12.

---

## D-08. Xác nhận liều (mục E, FR-026, FR-030 → FR-033, FR-044, BR-M07-12 → 14)

**Quyết định**:
- **Bảng kết quả** `ket_qua_lieu` (nhóm 3, chỉ thêm, `UNIQUE (lieu_thuoc_id)`): `loai` (`DA_DUNG`, `TU_CHOI`, `KHONG_THUC_HIEN`, `BO_LO`), `thoi_diem_dung`, `thoi_diem` (máy chủ), `thoi_diem_thiet_bi`, `nguoi_ghi_id` hoặc Hệ thống, `can_cu_quyen`, `nguoi_bao_lai` (FR-027), `phan_ung`, `ly_do`, `ghi_chu`, `nhan_dung_tre`, `nhan_ghi_sau_mat_ket_noi`, `nhan_dong_bo_sau_mat_quyen`, `lo_thuoc_gui_id`, `lan_giao_id`. Mỗi lần đóng liều (kể cả Bỏ lỡ và Không thực hiện do trạng thái cuối) có đúng một dòng gốc; đính chính trỏ dòng này (D-12). Liều Đã hủy và Tạm dừng kết thúc không có dòng kết quả.
- **Lệnh** `POST /lieu-thuoc/{lieuKey}/da-dung`, `/tu-choi`, `/khong-thuc-hien`. Một transaction: vật hóa nếu cần, khóa dòng liều, kiểm bảng chuyển, thêm `ket_qua_lieu`, đổi trạng thái, tác động.
- **Quyền** (FR-026): `QuyenApi.coQuyen(nd, (PHAT_THUOC, CHUYEN_MON), doiTuongLieu)` cho lớp vai trò và giấy phép "phát thuốc và xác nhận liều" (002 A-09). Lớp phạm vi do 006 thu hẹp theo liều (`KiemTraNguoiXacNhan`):
  1. được phân công người cao tuổi trong ca chứa thời điểm ghi (`DieuDuongCaPort.duocPhanCong(nctId, t)`);
  2. giữ nhiệm vụ Người phụ trách ca của tầng/khu (`PhamViApi.nguoiPhuTrachCa(tang, t)`);
  3. liều chung của tầng (D-09) và người ghi có ca tại tầng đó lúc ghi.

  Tầng của liều là tầng của người cao tuổi lúc ghi (`ViTriApi.hienTai`), nên chuyển giường sang tầng khác có hiệu lực từ thao tác kế tiếp (Edge Case CHK033). Trưởng tầng, Bác sĩ, Quản lý viện bị 403 dù có quyền xem.
- **Kiểm tra theo trạng thái** (bảng FR-021): `DEN_GIO`: thời điểm dùng trong cửa sổ; `TRE`: thời điểm dùng sau cửa sổ và không sau thời điểm ghi, đặt `nhan_dung_tre`; từ `TRE` với thời điểm dùng trong cửa sổ (ghi muộn) thì không nhãn trễ (FR-029); Từ chối, Không thực hiện bắt buộc `lyDo`. Không cho từ `CHUA_DEN_GIO` (Assumptions).
- **Nguồn gia đình gửi** (FR-044, FR-009): thân lệnh có `loThuocGuiId`; hệ thống trả danh sách lô `DUOC_SU_DUNG` gắn đơn kèm hạn dùng, số lượng còn ở `GET` liều, không chọn sẵn (Q-307). Chặn 409 `LO_KHONG_HOP_LE` khi lô không gắn đơn, không `DUOC_SU_DUNG`, hết hạn dùng hoặc thiếu số lượng; không có lô nào thì `CHUA_CO_THUOC` (US7 #8). Trừ số lượng bằng liều của đơn, thêm biến động `DUNG_LIEU` trỏ `ket_qua_lieu` (D-14). Liều thuộc lần giao (`lan_giao_id` khác rỗng) không trừ.
- **Tác động sau Đã dùng**:
  - nguồn viện, không thuộc lần giao: phát `SuKienTinhPhiThuoc(nguon = LIEU, id, nctId, thuocId, soLuong, thoiDiemDung)` cho 010 (FR-033, DBR-15); nguồn gia đình gửi và thuốc mua hộ: không (Q-139);
  - có `phanUng`: `YeuCauCanhBao(PHAN_UNG_THUOC, TRUNG_BINH, lieuId)` (FR-031);
  - liều đã có cảnh báo Bỏ lỡ (đóng muộn bằng đồng bộ, đính chính): `YeuCauDongCanhBao(lieuId, DA_DUNG_TRONG_CUA_SO)` khi thời điểm dùng trong cửa sổ;
  - liều quay về từ lần giao còn mở (Q-60): phản hồi kèm `nhac: GHI_NHAN_LAI_THUOC_MANG_THEO {lanGiaoId}`, không chặn (FR-048a).
- **Từ chối liên tiếp** (FR-032, BR-M07-13): hàm thuần `DemTuChoiLienTiep(cacLieuDaDong)` chạy khi ghi Từ chối hoặc đính chính. Tập liều: liều đã đóng của các đơn cùng `thuoc_id` của người đó hoặc cùng chuỗi `thay_the_cho`; theo thời điểm dự kiến giảm dần; bỏ Tạm dừng, Đã hủy; dùng trạng thái hiện hành sau đính chính (`DinhChinhApi.hienHanh`). Chuỗi Từ chối ≥ `CFG-M05-04` thì `YeuCauCanhBao(TU_CHOI_LIEN_TIEP, TRUNG_BINH, khoa = nctId:thuocGoc)`; 007 gộp theo BR-M05-02.
- **Không có lệnh sửa** liều đã xác nhận (BR-M07-12): `ket_qua_lieu` có trigger nhóm 3 của 000.

**Lý do**: Spec FR-026 → FR-033; một bảng kết quả cho mọi lần đóng giữ được người đóng "Hệ thống" và là gốc duy nhất cho đính chính.

---

## D-09. Liều chung của tầng và tầng không có điều dưỡng (FR-026a, FR-028, BR-M07-21, Q-61, Q-85)

**Quyết định**:
- **Không lưu người phụ trách trên liều**. Điều dưỡng phụ trách của một liều được tính lúc đọc và lúc ghi bằng hàm `XacDinhNguoiPhuTrachLieu(nctId, thoiDiemDuKien, bayGio)`:
  1. ca chứa thời điểm dự kiến (`DieuDuongCaPort.caChua(tang, t)`) và điều dưỡng được phân công người đó trong ca (`duocPhanCong`);
  2. không có (chưa phân công, vắng ca, nghỉ việc): liều chung của tầng, tập người thấy là `dieuDuongCoCa(tang, bayGio)`;
  3. liều chưa đóng của ca trước mà người được giao không có tên trong ca nào đang diễn ra và `banGiaoDaXacNhan(caTruoc)` sai: liều chung với dấu `TON_CHO_XAC_NHAN_BAN_GIAO`; đã xác nhận thì người phụ trách là người được phân công của ca hiện tại (FR-028, 008 FR-044 (c)).

  Phân công mới trong ca làm lần đọc kế tiếp trả người mới (FR-026a "liều chưa đóng MUST chuyển về điều dưỡng được phân công") mà không cần 008 gọi 006. Mỗi lần đóng lưu `can_cu_quyen` (D-08) để 016 biết liều chung (FR-051).
- **Thông báo một lần mỗi ca** "người cao tuổi chưa có điều dưỡng phụ trách": job theo phút, tại đầu ca và khi một liều của người đó tới đầu cửa sổ, nếu người đó không có phân công thì phát `NctChuaCoDieuDuongPhuTrach(nctId, caId)` cho Trưởng tầng và Người phụ trách ca (Trung bình, spec Cập nhật 2026-09-26 với 009); bảng `lan_chay_m07 (viec = NCT_CHUA_PHU_TRACH, moc = caId:nctId)` chặn gửi lặp.
- **Nhắc Trễ của liều chung**: gửi mọi Điều dưỡng có ca tại tầng và Người phụ trách ca (FR-026a).
- **Tầng không có Điều dưỡng nào có ca** (BR-M07-21): job theo phút, tại đầu ca và khi `dieuDuongCoCa(tang, bayGio)` rỗng lúc một liều tới đầu cửa sổ, phát `TangKhongCoDieuDuong(tang, caId, lieuSapToiGio[])` mức Trung bình cho Trưởng tầng và Quản lý viện; một lần mỗi (tầng, ca) qua `lan_chay_m07`. Liều vẫn theo vòng đời thường. *(R-15: `lan_chay_m07` → `moc_da_nhac`)*

**Lý do**: Phân công thuộc 008 và đổi liên tục trong ca; tính lúc đọc tránh phải đồng bộ một cột người phụ trách trên hàng nghìn liều.

**Phương án đã cân nhắc**: Lưu `dieu_duong_phu_trach_id` lúc vật hóa và để 008 gọi 006 khi đổi phân công (mẫu 005 C-10): thêm một giao diện MANDATORY cho 008 chỉ để giữ một cột suy ra được.

---

## D-10. Thuốc khi cần (mục D, FR-024 → FR-025a, BR-M07-04)

**Quyết định**:
- **Bảng** `lan_dung_prn` (nhóm 3, chỉ thêm): `don_thuoc_id`, `nct_id`, `thoi_diem_dung`, `lieu_dung`, `ly_do` (bắt buộc), `phan_ung`, `ghi_chu`, `lo_thuoc_gui_id`, `lan_giao_id`, `can_cu_quyen`, `nguoi_bao_lai`, `vuot_gioi_han`, `thoi_diem`, `thoi_diem_thiet_bi`, `nhan_ghi_sau_mat_ket_noi`. *(R-15: `lan_dung_prn` → `ket_qua_lieu` (`dose_result`))*
- **Lệnh** `POST /don-thuoc/{id}/lan-dung-prn`. Điều kiện: đơn PRN `HIEU_LUC` (chặn `TAM_DUNG`, CHK022); người cao tuổi Đang lưu trú (đang có mặt), trừ lần dùng ghi theo FR-027 khi Hoạt động bên ngoài hoặc Tạm vắng có mang thuốc; `lieuDung <= lieu` của đơn; quyền như D-08.
- **Giới hạn** (FR-025): hàm thuần `KiemTraGioiHanPrn(cacLanDaDung, thoiDiemMoi, khoangCach, soLanToiDa)`:
  - tập lần dùng: mọi đơn trong chuỗi thay thế (đi ngược `thay_the_cho_id` tới gốc), trạng thái hiện hành Đã dùng (bỏ lần đã bị đính chính khỏi Đã dùng);
  - (a) lần gần nhất trước `thoiDiemMoi` + khoảng cách > `thoiDiemMoi` thì chặn;
  - (b) số lần trong `(thoiDiemMoi − 24h, thoiDiemMoi]` ≥ số lần tối đa thì chặn;
  - trả `somNhatDuocDung = max(lanGanNhat + khoangCach, lanCuNhatTrong24h + 24h)`; 409 `VUOT_GIOI_HAN_PRN` kèm mốc này. Không có tham số vượt chặn.

  Chạy dưới khóa `thuoc_nct` (D-03). Lần dùng có thời điểm xen giữa các lần đã có cũng kiểm cả các lần sau nó, vì giới hạn là trên chuỗi thời gian. *(R-15: `thuoc_nct` → `trang_thai_nct`)*
- **Ghi theo báo lại** (FR-027): không chặn; đặt `vuot_gioi_han = true` khi hàm trên chặn, phát `YeuCauCanhBao(PRN_VUOT_GIOI_HAN, TRUNG_BINH, lanDungId)`.
- **Tính phí, số lượng** (FR-025a): như D-08 (`SuKienTinhPhiThuoc(nguon = LAN_DUNG_PRN)`; trừ lô với nguồn gia đình gửi; lần thuộc lần giao thì không).

**Lý do**: Q-56 (24 giờ trượt), CHK012 (toàn bộ chuỗi thay thế).

---

## D-11. Ngoại tuyến và thuốc kiểm soát đặc biệt (FR-029, Q-01, Q-62, Q-66, BR-M07-20, BR-M15-20)

**Quyết định**:
- **Phạm vi ngoại tuyến**: xác nhận liều (Đã dùng, Từ chối, Không thực hiện) và lần dùng PRN của thuốc không có dấu kiểm soát đặc biệt. Mọi lệnh khác (đơn thuốc, phiếu đối chiếu, thuốc gia đình gửi, giao và nhận lại thuốc mang theo, đính chính) chỉ trực tuyến.
- **Hai `NgoaiTuyenHandler`** (002 A-11): `M07.KET_QUA_LIEU`, `M07.LAN_DUNG_PRN`. `khoaQuyen = (PHAT_THUOC, CHUYEN_MON)`; `apDung` gọi cùng service với lệnh trực tuyến, `thoi_diem_thiet_bi` là thời điểm ghi; quyền và giấy phép kiểm theo thời điểm thiết bị (`coQuyenTaiThoiDiem`). `kiemTraNoiDung` từ chối bản ghi của thuốc kiểm soát đặc biệt với lý do `BAT_BUOC_TRUC_TUYEN`; app không cho đưa các liều này vào hàng đợi (dấu `batBuocTrucTuyen` trong dữ liệu checklist).
- **Trạng thái khi đồng bộ muộn**: xét theo `thoi_diem_dung` trên thiết bị. Liều đã `TRE` hoặc `BO_LO` mà thời điểm dùng trong cửa sổ:
  - `TRE` → `DA_DUNG` không nhãn trễ (bảng chuyển có dòng này cho bản ghi có `thoi_diem_thiet_bi`);
  - `BO_LO` là trạng thái cuối do Hệ thống đóng: handler ghi bản ghi đến thành **đính chính** của `ket_qua_lieu` loại `BO_LO` (D-12, người tạo là người ghi), trạng thái hiện hành Đã dùng, phát `YeuCauDongCanhBao(lieuId, DA_DUNG_TRONG_CUA_SO)`. Cách này giữ trigger trạng thái cuối và DBR-13.
- **Hai bản ghi ngoại tuyến cùng một liều** (BR-M07-20): bản đến trước đóng liều; bản sau `TU_CHOI` kèm `dongBoi` và nội dung để app giữ ở mục "bị từ chối" với lệnh "tạo đính chính"; nếu cả hai là Đã dùng thì phát `YeuCauCanhBao(NGHI_DUNG_TRUNG_LIEU, TRUNG_BINH, lieuId)`.
- **Thuốc kiểm soát đặc biệt "ghi sau mất kết nối"** (FR-029, Q-62, Q-66): lệnh trực tuyến thường với `ghiSauMatKetNoi = true` và `thoiDiemDung` thực tế (không có mốc riêng, CHK048). Tác động thêm:
  - `nhan_ghi_sau_mat_ket_noi = true`; trạng thái theo thời điểm dùng thực tế như trên;
  - thêm dòng `xem_lai_lieu` (nhóm 2: `CHO_XEM_LAI` → `CHAP_NHAN` | `CAN_DINH_CHINH`) với người xem lại được chọn: Điều dưỡng giữ nhiệm vụ Người phụ trách ca của tầng nếu khác người ghi, ngược lại mọi Điều dưỡng khác có ca tại tầng (người đầu tiên thực hiện lệnh là người xem lại); phát `LieuKsdbChoXemLai` cho người xem lại (Trung bình, 009 FR-043b) và thông báo Trưởng tầng; *(R-15: `xem_lai_lieu` → `ho_so_chung`)*
  - lệnh `POST /xem-lai-lieu/{id}/ket-qua` (người xem lại ≠ người ghi, đáp ứng giấy phép phát thuốc); `CAN_DINH_CHINH` thì người xem lại hoặc người ghi tạo đính chính theo D-12. Việc xem lại không đổi bản ghi.
- **App nhân viên**: drift lưu danh sách liều tải gần nhất kèm `taiLuc`; hàng đợi có `Idempotency-Key`; sau đồng bộ hiện kết quả từng bản ghi (BR-M15-20). Dữ liệu tải về mang trường `SUC_KHOE` nên chỉ giữ trong phạm vi được xem (002 A-08).

**Lý do**: 8.6 và Q-01 (mặc định); khung chung của 002. Bảng `ban_ghi_cho_xem_lai` của 002 không dùng cho thuốc kiểm soát đặc biệt, vì bản ghi "ghi sau mất kết nối" có hiệu lực ngay, việc xem lại chỉ gắn kết quả (FR-029). *(R-15: `ban_ghi_cho_xem_lai` → `ho_so_chung`)*

---

## D-12. Đính chính (FR-030, FR-039, BR-M07-12, 17, DBR-23)

**Quyết định**:
- **`DinhChinhDuocProvider` `M07.KET_QUA_LIEU`, `M07.LAN_DUNG_PRN`** (000 R-08):
  - `tangKhuVuc` = tầng của người cao tuổi lúc ghi gốc (lưu trên bản ghi);
  - `choPhep` thu hẹp theo FR-030: người tạo phải đáp ứng giấy phép phát thuốc và là người ghi gốc; hoặc Điều dưỡng giữ nhiệm vụ Người phụ trách ca của tầng; hoặc, với bản ghi do Hệ thống đóng (`BO_LO`, `KHONG_THUC_HIEN` do trạng thái cuối), Điều dưỡng phụ trách người cao tuổi trong ca của liều. Trưởng tầng không là Điều dưỡng bị chặn dù 000 cho cách B;
  - kiểu `DOI_KET_QUA` (đổi loại và thời điểm dùng, ví dụ Đã dùng → Không thực hiện, Bỏ lỡ → Đã dùng lúc 08:20) và `SUA_NOI_DUNG` (phản ứng, ghi chú);
  - `sauKhiDinhChinh`: tính lại tác động theo chênh lệch trạng thái hiện hành (D-14 biến động `DINH_CHINH_LIEU`; `SuKienTinhPhiThuocDaDinhChinh` cho 010, DBR-17; `YeuCauDongCanhBao` hoặc `YeuCauCanhBao` cho 007; chạy lại `DemTuChoiLienTiep`).
- **Trạng thái hiện hành** của liều lấy qua `DinhChinhApi.hienHanh` (báo cáo `JOIN` view `v_dinh_chinh_moi_nhat` của 000). Cột `trang_thai` của `lieu_thuoc` giữ giá trị gốc.
- **Đính chính lần giao, lần nhận lại** (FR-048a câu cuối): provider `M07.LAN_GIAO_THUOC`, `M07.LAN_NHAN_LAI_THUOC`; người tạo: người ghi gốc hoặc Điều dưỡng giữ nhiệm vụ Người phụ trách ca; `sauKhiDinhChinh` thêm biến động và sự kiện chi phí theo chênh lệch.
- **Đính chính phiếu đối chiếu Đã xác nhận** (FR-039, BR-M07-17): cần một người thứ hai xác nhận trước khi có hiệu lực, nên 006 có bảng `de_nghi_dinh_chinh_phieu` (nhóm 2: `CHO_XAC_NHAN` → `DA_XAC_NHAN` | `TRA_LAI`). Người lập là người thực hiện hoặc người xác nhận của phiếu; người xác nhận là Bác sĩ hoặc Điều dưỡng khác người lập. Lệnh xác nhận gọi `DinhChinhApi.taoDinhChinh("M07.PHIEU_DOI_CHIEU", …)` trong cùng transaction; provider `M07.PHIEU_DOI_CHIEU` chỉ cho gọi từ lệnh này (`choPhep` kiểm cờ ngữ cảnh) và `sauKhiDinhChinh` không đụng đơn thuốc. *(R-15: `de_nghi_dinh_chinh_phieu` → `ho_so_chung`)*

**Lý do**: Khung đính chính của 000; cách C (yêu cầu phê duyệt) của 000 dành cho bản ghi không gắn tầng và do vai trò cùng loại duyệt, không khớp quy tắc hai người Bác sĩ/Điều dưỡng của FR-039.

---

## D-13. Đối chiếu thuốc (mục F, FR-034 → FR-039, BR-M07-09, 17, 22, Q-55, Q-57, Q-306, Q-307)

**Quyết định**:
- **Bảng** `phieu_doi_chieu` (nhóm 2 khi `MO`, `CHO_XAC_NHAN`; nhóm 3 khi `DA_XAC_NHAN`): `nct_id`, `ly_do` (`TIEP_NHAN`, `TRO_VE_BENH_VIEN`), `trang_thai` (`MO`, `CHO_XAC_NHAN`, `DA_XAC_NHAN`, `DA_HUY`), `han_luc` (rỗng khi lập trước tiếp nhận), `qua_han_luc`, người, thời điểm. Bảng con `dong_doi_chieu`: `nguon` (`DON_HIEN_CO`, `DON_RA_VIEN`, `THUOC_DANG_DUNG_TIEP_NHAN`), `don_thuoc_id` (với đơn hiện có), `muc_thuoc_dang_dung_id` (mục của hồ sơ ban đầu, 001), nội dung thuốc tự do cho đơn ra viện, `quyet_dinh` (`TIEP_TUC`, `NGUNG`, `THAY_DOI_LIEU`, `THEM_MOI`), `ly_do`, `don_moi` (JSON nội dung đơn theo FR-004 cho Thay đổi liều, Thêm mới), `van_de_xac_nhan` (FR-007), `nguoi_ghi_id`, `da_ngung_ngoai_phieu`, `don_tao_id` (đặt khi xác nhận). Trigger khóa phiếu và dòng khi phiếu `DA_XAC_NHAN` (FR-039). *(R-15: `dong_doi_chieu` → cột `JSON` của `phieu_doi_chieu`)*
- **Tối đa một phiếu mở mỗi người**: cột sinh `mo_nct = IF(trang_thai IN ('MO','CHO_XAC_NHAN'), nct_id, NULL)` `UNIQUE`; tối đa một phiếu `TIEP_NHAN` chưa hủy mỗi hồ sơ: cột sinh `tiep_nhan_nct = IF(ly_do='TIEP_NHAN' AND trang_thai <> 'DA_HUY', nct_id, NULL)` `UNIQUE` (FR-034a).
- **Hệ thống không tạo phiếu, không tạo dòng** (Q-306, Q-307). Lệnh lập phiếu `POST /nguoi-cao-tuoi/{nctId}/phieu-doi-chieu`; lệnh `PUT /phieu-doi-chieu/{id}/dong` (If-Match) thay danh sách dòng khi `MO`. Màn hình hiện dữ liệu nguồn đã có (đơn Tạm dừng "chờ đối chiếu", thuốc đang dùng khi tiếp nhận) để người thực hiện đối chiếu; không có nút chép sẵn.
- **Gửi xác nhận** chặn 409 `THIEU_DONG` khi: còn đơn `TAM_DUNG` của người đó chưa có dòng (FR-034); với `TIEP_NHAN`, còn mục "thuốc đang sử dụng khi tiếp nhận" (`HoSoSucKhoeApi.banDau`) chưa có dòng (FR-034a); dòng thiếu quyết định, dòng Ngừng thiếu lý do; dòng Thay đổi liều, Thêm mới không qua `KiemTraDonMoi` hoặc thiếu trường FR-004.
- **Xác nhận** (FR-038, Q-55): người xác nhận là Bác sĩ hoặc Điều dưỡng có `(DOI_CHIEU_THUOC, XAC_NHAN)`, khác người gửi và khác mọi `nguoi_ghi_id` của dòng (409 `QUY_TAC_HAI_NGUOI`); phiếu có dòng tạo đơn nội bộ thì thêm `(DON_THUOC, CHUYEN_MON)` (403 `CAN_BAC_SI_KE_DON`). Trong cùng transaction áp FR-036 từng dòng bằng chính các service của D-05 (đơn thay thế, đơn mới, ngừng, tiếp tục), gỡ `qua_han_luc`, đặt `thuoc_nct.han_doi_chieu = NULL`, phát `YeuCauDongCanhBao(nctId, CHUA_DOI_CHIEU)`. Một dòng lỗi thì rollback cả lệnh.
- **Tiếp nhận và trở về** (FR-034): bean `ThamGiaChuyenTrangThai` cho `HOAN_TAT_TIEP_NHAN` và `GHI_NHAN_TRO_VE` từ Điều trị tại bệnh viện (D-16):
  - đã có phiếu `TIEP_NHAN` `DA_XAC_NHAN`: đơn `CHO_HIEU_LUC` mang `dau_cho_tiep_nhan` chuyển `HIEU_LUC`, `bat_dau_luc` = thời điểm lệnh;
  - còn lại: đơn `HIEU_LUC`, `CHO_HIEU_LUC` chuyển `TAM_DUNG` lý do `CHO_DOI_CHIEU`; dòng liều đã vật hóa chưa đóng từ thời điểm lệnh chuyển `TAM_DUNG`; đặt `thuoc_nct.han_doi_chieu = thời điểm lệnh + CFG-M07-05`; phiếu lập trước còn `MO`, `CHO_XAC_NHAN` nhận `han_luc`; phát `CanLapPhieuDoiChieu(nctId, lyDo)` cho Điều dưỡng phụ trách và Bác sĩ toàn viện (Trung bình, 009 FR-043b).
- **Quá hạn** (bảng FR-035, BR-M07-09): job theo phút xét `thuoc_nct.han_doi_chieu <= bây giờ` (có cả khi chưa lập phiếu): phát `YeuCauCanhBao(CHUA_DOI_CHIEU, TRUNG_BINH, nctId)` một lần (cột `da_canh_bao_doi_chieu_luc`) và đặt `qua_han_luc` của phiếu mở nếu có.
- **Chặn lệnh ngoài phiếu** (FR-037): khi có phiếu mở, Nhập đơn, Tiếp tục, Đổi liều trả 409 `DANG_CO_PHIEU_DOI_CHIEU`; Ngừng đơn được và đặt `da_ngung_ngoai_phieu` của dòng tương ứng.
- **Hồ sơ ban đầu đổi sau khi lập phiếu** (BR-M07-22, CHK008): cần sự kiện `HoSoSucKhoeBanDauDaDoi(nctId)` từ 001; 001 chưa khai báo nên plan bổ sung vào hợp đồng 001 (plan, mục "Đồng bộ"). Listener của 006: phiếu `MO` thì phát `ThuocDangDungTiepNhanDaDoi` cho người thực hiện; phiếu `CHO_XAC_NHAN`, `DA_XAC_NHAN` thì báo người thực hiện và người xác nhận. Không đổi phiếu. Dấu "thuốc đã bỏ khỏi hồ sơ" trên dòng được tính lúc đọc (so `muc_thuoc_dang_dung_id` với danh sách hiện hành), không ghi vào dòng.
- **Hủy** (bảng FR-035): bean cho `CHUYEN_VIEN` (phiếu mở → `DA_HUY` "chuyển viện lại"; chỉ người đã lưu trú, CHK034), trạng thái cuối và `HUY_TIEP_NHAN` (FR-034a, FR-047).
- **Phiếu theo yêu cầu chuyên môn** *(bổ sung 2026-10-11, FR-034b, BR-M07-25, Q-340)*: `ly_do = THEO_YEU_CAU_CHUYEN_MON`, cột `ly_do_lap` bắt buộc; lập bằng cùng `POST /nguoi-cao-tuoi/{nctId}/phieu-doi-chieu` khi người cao tuổi Đang lưu trú, Tạm vắng hoặc Hoạt động bên ngoài (409 với trạng thái khác). Không có bean tham gia lệnh trạng thái cho loại này, nên không tạm dừng đơn, không đặt `thuoc_nct.han_doi_chieu` (cột và `ly_do_doi_chieu` chỉ dùng cho hai lý do có hạn), `han_luc` rỗng, job quá hạn không xét. `mo_nct` vẫn chặn phiếu mở thứ hai; FR-037 (chặn lệnh đơn ngoài phiếu) áp như phiếu khác. Gửi xác nhận chặn `THIEU_DONG` khi còn đơn `HIEU_LUC`, `TAM_DUNG`, `CHO_HIEU_LUC` của người đó chưa có dòng. Xác nhận áp FR-036 như phiếu khác; dòng "Tiếp tục" với đơn đang `HIEU_LUC` không đổi đơn. Hệ thống không tự lập, không nhắc lập (Q-307). Hủy theo bảng FR-035 (chuyển viện, trạng thái cuối) như phiếu khác.

**Lý do**: Bảng FR-035 và Q-306: phiếu do actor lập; hạn tính từ lệnh tiếp nhận/trở về nên phải giữ ở `thuoc_nct`, không ở phiếu. *(R-15: `thuoc_nct` → `trang_thai_nct`)*

---

## D-14. Thuốc gia đình gửi (mục G, FR-040 → FR-046, BR-M07-10, 11, 18, 19)

> *(Sửa 2026-10-29, Q-393)* Thuốc gia đình gửi làm đơn giản: không còn `so_luong_con`, bảng `bien_dong_thuoc_gui`, `moc_nhac_thuoc_gui`, hàm `TinhSoNgayConLai`, lệnh Kiểm kê điều chỉnh, cột lô ở `ket_qua_lieu`, `lan_dung_prn`. Thêm lệnh Ghi dùng hết, Báo gia đình sắp hết (sự kiện `ThuocGuiSapHet(loId, lanBao, ghiChu)` cho 009, ghi nhật ký). Hoàn trả, Hủy ghi số lượng đếm thực tế, cả lô. Các gạch đầu dòng về biến động, số ngày còn lại, mốc nhắc dưới đây chỉ còn là lịch sử. *(R-15: `lan_dung_prn` → `ket_qua_lieu` (`dose_result`))*

**Quyết định**:
- **Lô** `thuoc_gia_dinh_gui` (nhóm 2): trường FR-040, `thuoc_id` (rỗng khi ghi tự do cho tới lúc Xác nhận sử dụng, CHK015), `ten_tu_do`, `hoat_chat_tu_do`, `so_luong_ban_dau`, `so_luong_con`, `don_vi`, `han_dung`, `tinh_trang_bao_bi`, `co_toa`, tệp ảnh, `nguoi_giao_loai` (`NGUOI_THAN`, `TU_DO`, `NHAN_VIEN_MUA_HO`), `nguoi_giao_id`, `nguoi_giao_ten`, `quan_he`, `de_nghi_mua_ho_id`, `trang_thai` (`CHO_DOI_CHIEU`, `DUOC_SU_DUNG`, `CHI_GIU_HO`, `DA_DUNG_HET`, `DA_HOAN_TRA`, `DA_HUY_THEO_YEU_CAU`). Bảng nối `thuoc_gui_don (lo_id, don_thuoc_id, tu, den)` nhiều–nhiều (FR-043, BR-M07-18).
- **Biến động** `bien_dong_thuoc_gui` (nhóm 3): `lo_id`, `loai` (`TIEP_NHAN`, `DUNG_LIEU`, `DUNG_PRN`, `DINH_CHINH_LIEU`, `GIAO_MANG_THEO`, `NHAN_LAI`, `KIEM_KE_DIEU_CHINH`, `HOAN_TRA`, `HUY`), `so_luong` (có dấu), `nguon_loai`, `nguon_id`, người, thời điểm, lý do. `so_luong_con` chỉ đổi qua trigger `AFTER INSERT` của bảng này (cộng `so_luong`); trigger `BEFORE UPDATE` trên lô chặn đổi `so_luong_con` khi biến phiên `@m07_bien_dong` không được đặt bởi trigger kia; `CHECK (so_luong_con >= 0)` (FR-045, SC-007).
- **Lệnh** (bảng FR-041): Tiếp nhận, Xác nhận sử dụng (gắn đơn), Chỉ giữ hộ, Ngừng sử dụng, Kiểm kê điều chỉnh, Hoàn trả, Hủy theo yêu cầu gia đình: mỗi lệnh một endpoint `POST /thuoc-gia-dinh-gui/{id}/<lenh>`; tiếp nhận `POST /nguoi-cao-tuoi/{nctId}/thuoc-gia-dinh-gui`. Người giao là người thân thì `QuanHeNguoiThanApi.thuocNguoiCaoTuoi`; Hủy cần người đại diện (`NguoiDaiDienPort` của 004, 012 cài). Hoàn trả, Hủy: `soLuong` phải bằng `so_luong_con` (409 `SO_LUONG_KHAC_SO_CON`, BR-M07-19). Thuốc mua hộ: `DeNghiMuaHoPort.daMua(deNghiId, nctId)` (010 cài) phải đúng (FR-040, Q-139).
- **Xác nhận sử dụng**: chọn `thuocId` khi lô ghi tự do; điều kiện cùng thuốc hoặc cùng hoạt chất và hàm lượng với đơn `GIA_DINH_GUI` Hiệu lực, Tạm dừng, Chờ hiệu lực; có toa hoặc đơn của viện; chưa hết hạn dùng; bao bì nhận diện được (trường xác nhận trong lệnh). Lô hết hạn dùng lúc tiếp nhận không chuyển được (409 `HET_HAN_DUNG`).
- **Chuyển tự động kèm lệnh hoặc theo giờ**:
  - đơn gắn chuyển `DA_NGUNG`, `HET_HAN` không có đơn thay thế cùng thuốc nguồn gia đình gửi → lô `CHI_GIU_HO` (cùng transaction của lệnh, job); có đơn thay thế cùng thuốc nguồn gia đình gửi → thêm dòng `thuoc_gui_don` cho đơn thay thế (FR-043); *(R-15: `thuoc_gui_don` → cột `JSON` của `thuoc_gia_dinh_gui`)*
  - qua ngày hạn dùng → `CHI_GIU_HO` "hết hạn dùng" (job hằng ngày 00:00), phát `ThuocGuiHetHanDung` cho Điều dưỡng phụ trách, người liên hệ chính, người đại diện;
  - `so_luong_con` về 0 → `DA_DUNG_HET` trong cùng transaction của biến động, phát `ThuocGuiDaDungHet` cho Điều dưỡng phụ trách.
- **Số ngày còn lại và mốc nhắc** (FR-046, BR-M07-11, 18, Q-247, Q-264): hàm thuần `TinhSoNgayConLai(nhom)` trên **nhóm dùng chung**: thành phần liên thông của đồ thị lô `DUOC_SU_DUNG` – đơn qua `thuoc_gui_don`. Số ngày = tổng `so_luong_con` của các lô trong nhóm ÷ tổng lượng dùng mỗi ngày của các đơn trong nhóm; lượng dùng mỗi ngày của đơn định kỳ = tổng liều theo `ChuoiThoiDiemDon` trong 7 ngày tới ÷ 7 (mọi tần suất, khớp FR-046 cho tần suất đều); đơn PRN = liều × số lần tối đa. Khi nhóm không có lô chung, kết quả trùng công thức "của mỗi đơn" ở FR-046 (plan, điểm báo lại 1). *(R-15: `thuoc_gui_don` → cột `JSON` của `thuoc_gia_dinh_gui`)*
  - Bảng `moc_nhac_thuoc_gui (khoa_nhom, moc, so_thu_tu, da_bao)`; `khoa_nhom` = `thuoc_id` + `nct_id` của nhóm (một người có một nhóm mỗi thuốc trong thực tế; nhiều nhóm cùng thuốc thì thêm id lô nhỏ nhất).
  - Chạy sau mỗi biến động làm giảm và sau Xác nhận sử dụng; so `truoc`, `sau`: vượt xuống CFG-M07-07 (kể cả vượt cả hai mốc, hoặc lần đầu đã dưới) thì một sự kiện `ThuocGuiSapHet(moc = M07_07)` và đánh dấu cả hai mốc; chỉ vượt CFG-M07-06 thì `ThuocGuiSapHet(moc = M07_06)`; lên lại ≥ mốc thì tăng `so_thu_tu` cho lần sau. Khóa sự kiện `(khoa_nhom, moc, so_thu_tu)` (009 FR-005).
- **Điều kiện kết thúc lưu trú, việc sau qua đời** (FR-047, Q-64): bean `ThamGiaChuyenTrangThai` trả `DieuKien("KT_C_THUOC_GUI", coTheMien = true)` khi còn lô `CHO_DOI_CHIEU`, `DUOC_SU_DUNG`, `CHI_GIU_HO` hoặc lần giao chưa `DA_NHAN_LAI`; `MucViecSauQuaDoiPort` mã `THUOC_GUI` trả trạng thái từ cùng điều kiện; mỗi lệnh Hoàn trả, Hủy, Nhận lại gọi `DanhSachViecSauQuaDoiApi.capNhat(nctId)` khi người đó đã qua đời.

**Lý do**: *(Sửa 2026-10-29, Q-393)* Chủ dự án chọn bỏ theo dõi số lượng còn để phần thuốc gửi không thành một kho nhỏ; việc báo gia đình do Điều dưỡng quyết định.

**Phương án đã cân nhắc**: Không lưu `so_luong_con`, luôn cộng biến động: đọc checklist và cổng người thân phải cộng hàng trăm dòng mỗi lô.

---

## D-15. Vắng mặt, mang theo, giao và nhận lại thuốc (FR-021, FR-027, FR-048 → FR-048b, BR-M07-03, BR-M04-16, Q-54, Q-58, Q-60, Q-64, Q-65)

**Quyết định**:
- **Khoảng vắng của thuốc** `khoang_vang_thuoc` (nhóm 2): `nct_id`, `loai` (`TAM_VANG_KHONG_THUOC`, `TAM_VANG_MANG_THUOC`, `BENH_VIEN`, `CHUYEN_DI_MANG_THUOC`, `CHUYEN_DI_KHONG_THUOC`), `tu`, `den_du_kien`, `den_thuc_te`, `luot_vang_id` (004) hoặc `chuyen_id` (014), `lenh_khong_mang_thuoc_id`. Ghi bởi bean trong transaction của lệnh trạng thái (D-16); là đầu vào `boiCanh` của `LichThuoc.tinh` (D-06). Gia hạn lượt vắng (`LuotVangDaGiaHan` của 004) cập nhật `den_du_kien`. *(R-15: `khoang_vang_thuoc` → `ho_so_chung`)*
- **Rời viện**: liều có thời điểm dự kiến trong `[tu, den_du_kien]` được tính Tạm dừng hoặc Mang theo; dòng đã vật hóa ở `CHUA_DEN_GIO` không có (vật hóa bắt đầu ở Đến giờ), nên không cần cập nhật; dòng `DEN_GIO`, `TRE` giữ nguyên (FR-023). Rời viện có mang thuốc: phản hồi của 006 cho màn điều dưỡng liệt kê "thuốc cần mang" (đơn có liều Mang theo, đơn PRN) để chuẩn bị (8.9).
- **Trở về** (FR-048): đặt `den_thuc_te`; dòng `TAM_DUNG` (không phải bệnh viện, đơn `HIEU_LUC`) hoặc `MANG_THEO` chưa ghi nhận có cuối cửa sổ chưa qua → `DEN_GIO` nếu đã trong cửa sổ, `CHUA_DEN_GIO` nếu chưa (dòng `MANG_THEO` được vật hóa sớm khi gắn lần giao); dòng nối lần giao của liều đó được đánh dấu thôi thuộc (Q-60); liều Mang theo chưa ghi nhận đã qua cuối cửa sổ: dấu `cho_ghi_nhan_sau_tro_ve` (FR-027, FR-028). Liều chưa vật hóa tự được tính lại từ khoảng vắng đã đóng. Trở về từ bệnh viện: không khôi phục (D-13).
- **Giao thuốc mang theo** (FR-048a): `lan_giao_thuoc` (nhóm 3 cho nội dung; cột trạng thái do hệ thống: `DANG_MANG_THEO`, `CHO_NHAN_LAI`, `DA_NHAN_LAI`), bảng con `lan_giao_thuoc_dong (don_thuoc_id, so_luong, lo_id)`; `lan_nhan_lai_thuoc` (nhóm 3) với dòng số lượng trả từng đơn (≤ số giao, bao gồm thuốc của liều thôi thuộc lần giao). Lệnh `POST /nguoi-cao-tuoi/{nctId}/lan-giao-thuoc`, `POST /lan-giao-thuoc/{id}/nhan-lai`. Tác động: biến động `GIAO_MANG_THEO` / `NHAN_LAI` cho lô; `SuKienTinhPhiThuoc(nguon = LAN_GIAO)` và `SuKienTinhPhiThuocDaDieuChinh(nguon = LAN_GIAO, soLuongMoi)` cho 010. Một lượt vắng có nhiều lần giao (CHK045). *(R-15: `lan_giao_thuoc`, `lan_nhan_lai_thuoc` → `lan_giao_nhan_thuoc` (`medication_handover`))*
- **Liều thuộc lần giao**: bảng `lan_giao_lieu (lan_giao_id, lieu_thuoc_id)`; liều Mang theo của đơn trong khoảng vắng được gắn khi giao (vật hóa ở `MANG_THEO`, D-06). Khi trở về sớm và liều quay về Đến giờ, dòng nối được đánh dấu `thoi_thuoc_luc` (không xóa) (Q-60).
- **Không có lần giao** mà có liều Mang theo: ghi Đã dùng trừ lô và tính phí từng liều (FR-048a).
- **Nhắc Nhận lại**: lần giao chuyển `CHO_NHAN_LAI` khi người cao tuổi trở về (bean), hiện trong checklist (`LieuThuocChecklistPort`) và danh sách cho bàn giao (`LieuBanGiaoApi`) tới khi Nhận lại (kể cả số lượng 0 kèm lý do).
- **Chuyến đi** (FR-048b, Q-171):
  - SPI `ChuyenDiThuocPort` (014 cài): `danhGiaDat(nctId, chuyenId)` trả khoảng đi dự kiến khi người đó được đánh giá Đạt;
  - lệnh `POST /nguoi-cao-tuoi/{nctId}/khong-mang-thuoc` (Điều dưỡng; chuyến, lý do) ghi `lenh_khong_mang_thuoc` (nhóm 3); *(R-15: `lenh_khong_mang_thuoc` → `lan_giao_nhan_thuoc` (`medication_handover`))*
  - `MangThuocChuyenDiApi.daChuanBi(nctId, chuyenId)` (014 đọc trước khi cho điểm danh rời viện): có lần giao cho chuyến hoặc lệnh "Không mang thuốc", hoặc không có liều nào trong khoảng đi;
  - `MangThuocChuyenDiApi.chuyenThayDoi(chuyenId, nctId, loai)` (MANDATORY, 014 gọi khi hủy chuyến, "Không đi", dời ngày): lần giao đang mở của chuyến phát `CanNhanLaiThuocMangTheo` cho Điều dưỡng phụ trách; `tinhLaiKhoang(chuyenId, nctId, khoangMoi)` (MANDATORY, khi đính chính thời điểm rời/về) cập nhật `khoang_vang_thuoc`. *(R-15: `khoang_vang_thuoc` → `ho_so_chung`)*
  - Điểm danh rời viện và về đi qua lệnh trạng thái của 001 (`DIEM_DANH_ROI_VIEN`, `DIEM_DANH_VE`, `HUY_GHI_NHAN_ROI_VIEN`); bean của 006 đọc `chuyenId` từ `chiTiet` và khoảng đi từ `ChuyenDiThuocPort`.

**Lý do**: 004 sở hữu lượt vắng và 014 sở hữu chuyến; 006 chỉ giữ bản sao khoảng thời gian cần để tính lịch, ghi kèm lệnh của họ, không đọc bảng của họ.

---

## D-16. Bean tham gia lệnh trạng thái của 001 (FR-034, FR-034a, FR-047, FR-048)

**Quyết định**: Bean `@Order` sau 005. Điều kiện chặn và tác động (trong transaction của 001, `Propagation.MANDATORY`):

| Lệnh | Điều kiện | Tác động | FR |
| --- | --- | --- | --- |
| `HOAN_TAT_TIEP_NHAN` | — | D-13 (kích hoạt đơn "chờ tiếp nhận" hoặc tạm dừng chờ đối chiếu, đặt hạn) | FR-034, FR-034a |
| `CHO_TAM_VANG` | — | `khoang_vang_thuoc` loại theo `chiTiet.mangThuoc` của 004 và `duKienTroLai` | FR-048, BR-M07-03 *(R-15: `khoang_vang_thuoc` → `ho_so_chung`)* |
| `CHUYEN_VIEN` | — | `khoang_vang_thuoc` `BENH_VIEN`; phiếu mở → `DA_HUY` "chuyển viện lại" | FR-035, FR-048 *(R-15: `khoang_vang_thuoc` → `ho_so_chung`)* |
| `GHI_NHAN_TRO_VE` | — | Đóng khoảng vắng; khôi phục liều (Tạm vắng) hoặc tạm dừng chờ đối chiếu (bệnh viện); lần giao → `CHO_NHAN_LAI` | FR-021, FR-034, FR-048a |
| `DIEM_DANH_ROI_VIEN`, `DIEM_DANH_VE`, `HUY_GHI_NHAN_ROI_VIEN` | — | Khoảng `CHUYEN_DI_*` theo D-15 | FR-048, FR-048b |
| `KET_THUC_LUU_TRU` | `KT_C_THUOC_GUI` (D-14) | FR-047 | FR-047, Q-64 |
| `GHI_NHAN_QUA_DOI`, `HUY_TIEP_NHAN` | — | FR-047; với Hủy tiếp nhận thêm FR-034a (phiếu lập trước → `DA_HUY`, đơn "chờ tiếp nhận" → `DA_NGUNG`) | FR-047, FR-034a |

Tác động FR-047: đơn `HIEU_LUC`, `TAM_DUNG`, `CHO_HIEU_LUC` → `DA_NGUNG` (lý do trạng thái cuối); dòng liều `TAM_DUNG`, `MANG_THEO` chưa ghi nhận → `DA_HUY`; `DEN_GIO`, `TRE` → `KHONG_THUC_HIEN` (người đóng Hệ thống, quy tắc `FR_047_TRANG_THAI_CUOI`, thêm `ket_qua_lieu`); liều chưa vật hóa được tính Đã hủy vì đơn đã ngừng; phiếu `MO`, `CHO_XAC_NHAN` → `DA_HUY`; `thuoc_nct.han_doi_chieu = NULL`; phát `YeuCauDongCanhBao(nctId, BO_LO_LIEU | CHUA_DOI_CHIEU, TRANG_THAI_CUOI)`. Lô và lần giao giữ nguyên. Không nhắc, không cảnh báo, không vào bàn giao (`LieuBanGiaoApi` lọc người ở trạng thái cuối).

Qua đời với thời điểm trong quá khứ (Edge Case): 006 không sửa bản ghi đã xác nhận; `GET` lịch sử liều hiển thị dấu "cần rà soát" cho kết quả có `thoi_diem_dung` sau `NguoiCaoTuoiApi.thoiDiemTrangThaiCuoi` (tính lúc đọc).

---

## D-17. Sự kiện và thông báo

**Đối tượng thông tin** (bổ sung 2026-10-18; FR-053, 009 FR-001a): thông báo tới người liên hệ chính, người đại diện về thuốc gia đình gửi (sắp hết FR-046, hết khi đến liều, hết hạn dùng BR-M07-11, cần nhận lại thuốc mang theo) khai `doiTuongThongTin = VE_VIEC_NGUOI_THAN` (Q-362); phần `SUC_KHOE` (tên thuốc, hàm lượng) giữ như cũ để 009 lọc theo bản đồng ý.

**Quyết định**: Mọi yêu cầu tới 007, 009, 010 là sự kiện ghi cùng transaction qua outbox của 000 (R-09), mang khóa `loại + id + mốc` để bên nghe bỏ trùng (NFR-04). Không có lời gọi đồng bộ tới module làm plan sau, trừ các SPI đọc ở D-02.

Mức thông báo (spec, 009 FR-043b; dòng có dấu * là plan áp nguyên tắc 3 bậc, xem plan điểm báo lại 5):

| Mức | Thông báo |
| --- | --- |
| Trung bình | nhắc liều Trễ (FR-022); người cao tuổi chưa có điều dưỡng phụ trách (FR-026a); tầng không có Điều dưỡng (BR-M07-21); liều kiểm soát đặc biệt chờ xem lại (FR-029); cần lập / hoàn thành phiếu đối chiếu (FR-034); thuốc gia đình gửi sắp hết hai mốc (FR-046); thuốc gia đình gửi hết hạn dùng*; hết thuốc gia đình gửi khi đến liều (US7 #8)*; cần nhận lại thuốc mang theo (FR-048b)* |
| Nhẹ | đơn sắp hết hạn (FR-017); phiếu gửi xác nhận, trả lại*; thuốc gửi đã dùng hết, chuyển Chỉ giữ hộ do đơn ngừng*; thuốc đang dùng khi tiếp nhận đã đổi (BR-M07-22)* |

Sự kiện yêu cầu 007 (`YeuCauCanhBao(loai, muc, nctId, nguon, khoa)`, mọi loại mức Trung bình là mức tối thiểu, FR-031): `BO_LO_LIEU`, `PHAN_UNG_THUOC`, `TU_CHOI_LIEN_TIEP`, `XUNG_DOT_DI_UNG`, `CHUA_DOI_CHIEU`, `PRN_VUOT_GIOI_HAN`, `NGHI_DUNG_TRUNG_LIEU`; và `YeuCauDongCanhBao(nguon, lyDo)`.

Gửi 010: `SuKienTinhPhiThuoc(nguon: LIEU | LAN_DUNG_PRN | LAN_GIAO, id, nctId, thuocId, soLuong, thoiDiem)`, `SuKienTinhPhiThuocDaDieuChinh(nguon, id, soLuongMoi, lyDo: DINH_CHINH | NHAN_LAI)`. 010 quyết định đơn giá, gói, khoản điều chỉnh (DBR-15 → 17).

**Lý do**: Constitution XIV; mẫu 005 C-13.

---

## D-18. Quyền và xem (FR-049, Phụ lục 27, 19.3)

**Quyết định**: Khóa quyền theo dòng Phụ lục 27, ánh xạ ký hiệu → thao tác theo 002 A-04:

| Chức năng (`ChucNang`) | Dòng Phụ lục 27 | Thu hẹp ở 006 |
| --- | --- | --- |
| `DANH_MUC_THUOC` | "Danh mục thuốc" | QL `TAO`, `SUA`; BS, ĐD `XEM` |
| `DON_THUOC` | "Đơn thuốc" (³ ⁴) | `CHUYEN_MON` cho đơn nội bộ (Bác sĩ); `TAO`, `SUA` cho đơn bên ngoài (BS, ĐD); NT `XEM` với chú thích ¹; TT không có |
| `PHAT_THUOC` | "Phát thuốc, thuốc khi cần" | ĐD `CHUYEN_MON` + D-08 bước 1 → 3; QL, TT, BS `XEM` (TT chỉ liều, không đơn) |
| `DOI_CHIEU_THUOC` | "Đối chiếu thuốc, thuốc gia đình gửi" | BS, ĐD `TAO`, `XAC_NHAN`; NT `XEM` phiếu Đã xác nhận với chú thích ¹ (Q-59); NT `XEM` thuốc gia đình gửi không cần chú thích; TT không có |
| `TIEP_NHAN_THUOC_CUNG_UNG` *(bổ sung 2026-10-11, Q-337)* | "Tiếp nhận thuốc cung ứng" (⁴⁴) | ĐD `TAO` trong phạm vi phân công với người cao tuổi của dòng (002 A-04, NV 19.3); QL, BS, KT `XEM`; chú thích ⁴⁴ chỉ mô tả, kiểm ở 006 (002 A-04, U2) |

- Trường tên thuốc, hoạt chất, liều, phản ứng, lý do mang `@NhomTruong(SUC_KHOE)` (002 A-08); Nhân viên chăm sóc, Bếp, Hành chính không thấy (FR-049, 19.3). Thông báo nêu tên thuốc chỉ gửi người có quyền xem; thông báo cho người thân chia phần `CHUNG` và `SUC_KHOE` (009, Q-116).
- Cổng người thân (012 đọc `ThuocCongNguoiThanApi`): thuốc gia đình gửi (tên, trạng thái, số lượng còn, hạn dùng) cho mọi người thân có quan hệ Hiệu lực và tài khoản (CHK051); đơn và phiếu Đã xác nhận khi `BanDongYApi.baoGom(nctId, nguoiThanId)`; không có liều. Lần xem ghi nhật ký (NFR-08).

---

## D-19. Bộ lập lịch của 006 (FR-017, FR-021, FR-026a, FR-035, FR-041, FR-046, NFR-04)

**Quyết định**: Hai job có ShedLock, chạy cả khi khởi động (chạy bù).

| Job | Chu kỳ | Việc |
| --- | --- | --- |
| `ViecTheoPhutM07` | Mỗi phút | (1) đơn `CHO_HIEU_LUC` tới `bat_dau_luc` → `HIEU_LUC` (trừ dấu "chờ tiếp nhận"); đơn có `ngung_tu <= bây giờ` → `DA_NGUNG` cùng đơn thay thế → `HIEU_LUC`; (2) vật hóa liều tới đầu cửa sổ (D-06); (3) Đến giờ → Trễ, nhắc; (4) Trễ → Bỏ lỡ, cảnh báo; (5) đặt `ket_thuc_luc` cho `TAM_DUNG` qua cuối cửa sổ; (6) hạn đối chiếu (D-13); (7) đầu ca: người chưa có điều dưỡng phụ trách, tầng không có Điều dưỡng (D-09) |
| `ViecHangNgayM07` | 00:00 và khi khởi động | (1) đơn hết ngày kết thúc → `HET_HAN`, lô gắn → `CHI_GIU_HO` khi không có đơn thay thế; (2) nhắc CFG-M07-04; (3) lô qua hạn dùng → `CHI_GIU_HO` |

- **Một lần**: chuyển xét `trang_thai` dưới khóa dòng; mốc theo ca, theo ngày dùng `lan_chay_m07 (viec, moc)`; nhắc, cảnh báo có cột `da_*_luc`.
- **Chạy bù**: việc (2) vật hóa mọi liều có đầu cửa sổ trong khoảng bị lỡ với trạng thái tính tại mốc dự kiến; (3), (4) dùng mốc dự kiến cho quyết định và thời điểm chạy thật cho `*_luc`; nhắc Trễ bị dồn của cùng người nhận gộp thành `NhacLieuTreGop` như 005 C-12.
- **Hiệu năng**: chỉ mục `(trang_thai, cua_so_den)`, `(trang_thai, tre_luc)`; việc (2) duyệt đơn `HIEU_LUC`, `TAM_DUNG` (khoảng 1.500 đơn) bằng `ChuoiThoiDiemDon` cho khoảng `(lanChayTruoc, bayGio]`, vài mili giây mỗi đơn. Mỗi liều chuyển trong một transaction ngắn.
- Người thực hiện ghi ở nhật ký là Bộ lập lịch, kèm căn cứ (000 FR-045).

**Lý do**: SC-004 đòi ≤ 1 phút; NFR-04 đòi cả viện ≤ 5 phút, không gửi trùng.

---

## D-20. Lưu giữ, nhật ký, kiểm thử

**Quyết định**:
- **Lưu giữ** (000 R-11, NFR-07): mọi bảng gắn người cao tuổi (`don_thuoc` cùng bảng con, `lieu_thuoc`, `ket_qua_lieu`, `lan_dung_prn`, `phieu_doi_chieu`, `dong_doi_chieu`, `thuoc_gia_dinh_gui`, `bien_dong_thuoc_gui`, `lan_giao_thuoc`, `lan_nhan_lai_thuoc`, `khoang_vang_thuoc`, `xem_lai_lieu`) đăng ký `LuuTruProvider` loại `THEO_HO_SO` (CFG-M15-03). Danh mục thuốc không bị loại bỏ. *(R-15: `lan_dung_prn` → `ket_qua_lieu` (`dose_result`); `dong_doi_chieu` → cột `JSON` của `phieu_doi_chieu`; `lan_giao_thuoc`, `lan_nhan_lai_thuoc` → `lan_giao_nhan_thuoc` (`medication_handover`); `khoang_vang_thuoc`, `xem_lai_lieu` → `ho_so_chung`)*
- **Nhật ký**: hành động `M07_THUOC_*`, `M07_DON_*` (nhập, tạm dừng, tiếp tục, ngừng, đổi liều, gia hạn, ghi chú, hủy thay đổi), `M07_LIEU_CHUYEN`, `M07_LIEU_XAC_NHAN`, `M07_PRN`, `M07_PHIEU_*`, `M07_THUOC_GUI_*`, `M07_GIAO_THUOC`, `M07_NHAN_LAI_THUOC`, `M07_XEM_LAI`, `M07_NHAC`, `M07_BAO`; dấu "chạy bù" khi có.
- **Kiểm thử** (000 R-14):
  - hàm thuần có test bảng: `ChuoiThoiDiemDon`, `LichThuoc.tinh`, `BangChuyenLieu`, `KiemTraDonMoi`, `KiemTraGioiHanPrn`, `DemTuChoiLienTiep`, `TinhSoNgayConLai`, `XacDinhNguoiPhuTrachLieu`;
  - Test tích hợp MySQL: trigger khóa đơn, trigger trạng thái cuối liều, trigger số lượng thuốc gửi, cột sinh `UNIQUE` (DBR-13, một phiếu mở, một phiếu tiếp nhận, một đơn thay thế);
  - đồng thời: hai lần xác nhận một liều; xác nhận và job Bỏ lỡ; hai lần dùng PRN gần đồng thời; hai lần trừ cùng một lô;
  - đồng hồ giả lập: 20 người × 60 đơn × 7 ngày (SC-001), chạy bù sau 3 giờ dừng;
  - bản giả có công tắc `/api/v1/kiem-thu/gia-lap/{module}` cho `DieuDuongCaPort` (008), `ChuyenDiThuocPort` (014), `DeNghiMuaHoPort` (010), `QuanHeNguoiThanApi`, `NguoiDaiDienPort` (012);
  - client: Vitest cho màn nhập đơn (danh sách vấn đề cần lý do), phiếu đối chiếu (không có nút chép sẵn), thuốc gia đình gửi; `flutter_test` cho xác nhận liều ngoại tuyến, chặn liều kiểm soát đặc biệt khỏi hàng đợi, chọn lô, kết quả đồng bộ (BR-M15-20, constitution XV).

---

## D-21. Tiếp nhận thuốc cung ứng (mục I, FR-054, FR-055, BR-M07-24, Q-337) *(bổ sung 2026-10-11)*

**Quyết định**:
- **Bảng** `lan_tiep_nhan_thuoc_cung_ung` (nhóm 3, data-model mục 9a): mỗi lần kiểm một dòng của yêu cầu mua sắm loại "thuốc" (019) là một bản ghi Đạt hoặc Không đạt; giao nhiều đợt, giao bù là nhiều bản ghi. Không có số lượng tồn, không đổi đơn, liều, không phát `SuKienTinhPhiThuoc` (chi phí cho người cao tuổi vẫn theo liều, BR-M07-14). *(R-15: `lan_tiep_nhan_thuoc_cung_ung` → `lan_giao_nhan_thuoc` (`medication_handover`))*
- **Dữ liệu yêu cầu**: đọc qua SPI `YeuCauThuocCungUngPort` ở `medication.api`, 019 (M11) cài. Port chỉ trả dòng khi yêu cầu đã ghi đã mua (Chờ đối chiếu, Đã đối chiếu, Lệch; BR-M11-17); rỗng thì 409. Bảng của 006 không khóa ngoại sang bảng của M11.
- **Quyền**: khóa `(TIEP_NHAN_THUOC_CUNG_UNG, TAO)` cho Điều dưỡng; phạm vi là người cao tuổi của dòng, theo quy tắc phạm vi T của Điều dưỡng (19.3: Điều dưỡng thực hiện trong phạm vi phân công; BR-M15-01; 002 A-04). Quyết định của chủ dự án 2026-10-11; không phải quy tắc mới. Chú thích ⁴⁴ chỉ mô tả, kiểm ở 006 (U2).
- **Không đạt** (FR-055): phát `TiepNhanThuocKhongDat` cho 009, mức Nhẹ, người nhận là người lập yêu cầu (`NhanVien`) và Phụ trách bộ phận hiện tại của bộ phận của yêu cầu (`PhuTrachBoPhan(boPhanId)` của 009, `boPhanId` lấy từ port là bộ phận gắn với yêu cầu lúc lập; sửa 2026-10-11, lượt 3). 006 chỉ phát một sự kiện; xác định người, người thay và gộp người trùng do 009 (FR-012), 006 không tự gửi. Hệ thống không lập yêu cầu mới, không hủy yêu cầu (Q-307).
- **"Đã nhận"** là số liệu tính khi đọc (`TiepNhanThuocCungUngApi.theoYeuCau`): mọi dòng có ít nhất một lần Đạt.

**Lý do**: Giữ một chiều M11 → M07 như `DeNghiMuaHoPort` (D-02); không tạo kho thuốc (1.2, Q-337).

**Phương án đã cân nhắc**: Đặt lần tiếp nhận ở 019 (M11): 019 không có quyền và dữ liệu thuốc của người cao tuổi, và Điều dưỡng ghi nhận trong luồng thuốc; 006 gọi `billing.api` để đọc yêu cầu: tạo vòng M07 ↔ M11 vì M11 đã phụ thuộc M07.
