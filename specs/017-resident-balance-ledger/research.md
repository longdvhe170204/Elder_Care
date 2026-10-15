# Research: Số dư và thu chi của người cao tuổi (017)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

Mỗi mục gồm Quyết định, Lý do, Phương án đã cân nhắc. Quyết định nền R-01 → R-14 của 000 và các mẫu đã chốt ở 001 → 015 (H-02 bean trạng thái, T-09 mã điều kiện kết thúc, P-01 → P-15 của 010, đặc biệt P-12 port nội bộ M11; B-02 khuôn thông báo) được dùng lại, không quyết lại. Các quyết định dưới đây là kỹ thuật; chúng không đổi nghiệp vụ (constitution IX). Chỗ spec chưa rõ hoặc mâu thuẫn được ghi ở plan, mục "Điểm cần báo lại".

---

## E-01. Vị trí trong backend

**Quyết định**: Feature 017 là phần 15.9 của module M11, thêm gói `vn.eldercare.m11.sodu` (010 P-01).

| Gói con | Nội dung | Mục spec |
| --- | --- | --- |
| `m11.sodu.so` | Sổ số dư, sổ tiền cọc, mã nộp tiền, thông tin nộp tiền (VietQR), số dư hiển thị | A |
| `m11.sodu.thu` | Phiếu thu tiền mặt, dãy số phiếu, chốt quỹ ngày, trạng thái đặt cọc | B |
| `m11.sodu.saoke` | Nhập sao kê, khớp mã nộp tiền, dòng sao kê | B |
| `m11.sodu.giaodich` | Giao dịch, thanh toán bảng, hoàn tiền, hoàn cọc, điều chỉnh, cặp chuyển tiền, cấn trừ cọc, đảo; duyệt | C |
| `m11.sodu.tinhtrang` | Số ngày còn đủ tiền, tình trạng, báo, dừng nhắc, dấu mua hộ | D |
| `m11.sodu.quyettoan` | Điều kiện quyết toán, mục việc sau qua đời, nhắc sau trạng thái cuối, nhắc hoàn cọc | E |
| `m11.sodu.xuat` | Sao kê, báo cáo thu chi, sổ cọc (Excel) | F |
| `m11.sodu.lich`, `m11.api` (bổ sung) | Job; giao diện công khai | |

---

## E-02. Chiều phụ thuộc giữa module

**Quyết định**:

| Cặp | Cách nối |
| --- | --- |
| 017 ↔ 010 (cùng M11) | Port nội bộ P-12: dùng `ChiPhiChoSoDuPort` (`tongChuaChot`, `phiNgayCoSo`); cài `SoDuMuaHoPort.soDuKhongDu`. Nghe `m11.api.BangChiPhiDaChot`, `DeNghiMuaHo*`, `KhoanChiPhiDaDoi` như module khác |
| 017 → 004 | Cài `TrangThaiDatCocPort` (`m02.api`), `MucViecSauQuaDoiPort` (`QUYET_TOAN`), `ThamGiaChuyenTrangThai` với điều kiện `KT_H_QUYET_TOAN` (T-09); đọc `HopDongDocApi.khoanDatCoc`, lịch đến bán trú; nghe `HopDongDaDoiTrangThai`, `KhoanCocDaDoi`, `HoSoKetThucDa*`, `QuaDoiDaGhiNhan`; gọi `DanhSachViecSauQuaDoiApi.capNhat` |
| 017 → 001 | `NguoiCaoTuoiApi.trangThai`, `laTrangThaiCuoi`; nghe `NguoiCaoTuoiDaChuyenTrangThai` (Hủy tiếp nhận) |
| 017 → 012 | `QuyenNguoiThanApi` (người đại diện, quyền xem chi phí) cho người nhận hoàn tiền, thông báo, REST cổng |
| 017 → 009, 016 | Thông báo cài `CoYeuCauThongBao`; 016 đọc `SoDuThongKeApi` |

Không có SPI 017 khai cho module sau (016 đọc API).

**Lý do**: Các SPI 004 đã khai cho 017; 010 dành sẵn port nội bộ. Modulith `verify()` không có vòng.

---

## E-03. Khóa và đồng thời

**Quyết định**:
- Nối chuỗi khóa của 010 (P-03): … → `chi_phi_nct` (M11) → `so_du` (một dòng mỗi hồ sơ, giữ cả số dư cọc) → giao dịch.
- Mọi giao dịch Đã xác nhận cập nhật `so_du.so_du` / `so_du_coc` trong cùng transaction dưới khóa `so_du`; bất biến "số dư = tổng giao dịch Đã xác nhận" kiểm bằng test và truy vấn đối chiếu hằng đêm (ghi lỗi, không tự sửa).
- **Số phiếu** (FR-005): bảng `day_so_phieu (loai: THU | CHI, gia_tri_cuoi)` khóa `FOR UPDATE`; cấp trong transaction ghi giao dịch nên rollback không để lại khoảng trống (SC-011). Không dùng AUTO_INCREMENT (có thể bỏ số).
- **Dòng sao kê** (DBR-29): `UNIQUE (ma_giao_dich_ngan_hang)` trên dòng; `UNIQUE (dong_sao_ke_id)` trên giao dịch.
- **Thanh toán bảng** (DBR-30): `UNIQUE (bang_chi_phi_id)` trên giao dịch loại `THANH_TOAN_BANG`.
- **Một đảo mỗi gốc**: cột sinh `khoa_dao = IF(loai = 'DAO' AND trang_thai IN ('CHO_DUYET','DA_XAC_NHAN'), giao_dich_goc_id, NULL)` `UNIQUE`.
- **Chốt quỹ mỗi ngày** (DBR-35): cột sinh `khoa_ngay = IF(trang_thai IN ('DA_LAP','DA_XAC_NHAN'), ngay, NULL)` `UNIQUE`.

---

## E-04. Sổ, mã nộp tiền, VietQR (mục A; DBR-28, Q-211, Q-225, Q-285)

**Quyết định**:
- Listener `HopDongDaDoiTrangThai(… den = CHO_KY)` (004): nếu hồ sơ chưa có sổ → mở `so_du` (số dư 0, số dư cọc 0) và cấp `ma_nop_tien` (tác động kèm lệnh gửi ký của 004; idempotent theo hồ sơ). Hồ sơ mới của người quay lại có sổ mới (khóa theo `nct_id`).
- `ma_nop_tien`: chuỗi ngắn có chữ số kiểm tra (ví dụ `NCT` + 6 ký tự Base32 Crockford + 1 ký tự kiểm tra), `UNIQUE`, không dùng lại; sinh ngẫu nhiên và thử lại khi trùng. Chữ số kiểm tra giảm khớp nhầm khi gõ sai.
- Thông tin nộp tiền: số tài khoản thu, tên ngân hàng, mã BIN (VietQR) của viện đọc từ tham số CFG-M11-10 qua `ThamSoApi` (Q-335, sửa 2026-10-10); Quản lý viện đổi trên màn cấu hình, có lịch sử (BR-M15-04). Mã QR theo chuẩn VietQR (EMVCo, không số tiền, nội dung = mã nộp tiền) dựng ở backend thành chuỗi payload; client vẽ ảnh QR. Không có thanh toán trực tuyến (Q-285).
- REST cổng `/cong/nguoi-cao-tuoi/{nctId}/so-du` (số dư, cọc, giao dịch Đã xác nhận, thông tin nộp tiền) và tải sao kê; kiểm `QuyenNguoiThanApi.quyenCoTacDung(XEM_CHI_PHI)` mỗi lần gọi (SC-010). Giao dịch thanh toán bảng chỉ hiện tổng (Q-133).

---

## E-05. Thu tiền mặt, chốt quỹ, đặt cọc (FR-005, FR-005a, FR-006; BR-M11-16, DBR-35)

**Quyết định**:
- Phiếu thu = giao dịch `NOP_TIEN_MAT` (sổ số dư) hoặc `THU_COC` phương thức tiền mặt (sổ cọc), Đã xác nhận ngay, cấp số phiếu dãy `THU`. Hoàn tiền, hoàn cọc tiền mặt cấp số dãy `CHI` khi Đã xác nhận.
- `chot_quy_ngay` (nhóm 2 → 3): ngày, danh sách số phiếu (bảng con), tổng thu, tổng chi, phải có, thực có, chênh lệch, lý do. Hàm thuần `TongQuyNgay(giaoDichTienMat, ngay)` theo ngày giao dịch chuyển Đã xác nhận. Trạng thái `DA_LAP | DA_XAC_NHAN | BI_TRA_LAI`. Giao dịch tiền mặt Đã xác nhận cho ngày đã có chốt quỹ Đã xác nhận → dấu "phát sinh sau chốt quỹ" và nhắc Kế toán (chốt quỹ bổ sung cho ngày duyệt, theo bảng của spec).
- Job theo giờ hành chính (CFG-M13-06): hết giờ hành chính ngày có giao dịch tiền mặt mà chưa có chốt quỹ → nhắc Kế toán; hết giờ ngày làm việc kế tiếp mà chưa xác nhận → báo Quản lý viện (Trung bình).
- **Đặt cọc** (FR-006): hàm thuần `TrangThaiDatCoc(hopDong, soDuCoc)` → `KHONG_YEU_CAU | DA_DAP_UNG | CHUA_DAP_UNG`. Bản cài `TrangThaiDatCocPort.trangThai(hopDongId)` tính lúc gọi. Mỗi lần kết quả đổi (sau giao dịch cọc, đảo cọc, `KhoanCocDaDoi`) → phát `m11.api.TrangThaiDatCocDaDoi(hopDongId, nctId, trangThai, giaoDichId, keToanId, thoiDiem)` cho 004.

---

## E-06. Sao kê và đối soát (FR-007 → FR-009a; BR-M11-12, DBR-29, Q-227)

**Quyết định**:
- Nhập: đọc Excel (fastexcel-reader, cùng họ thư viện 010 dùng để ghi) hoặc CSV; ánh xạ cột theo mẫu sao kê cấu hình (tên cột của ngân hàng). File không đọc được hoặc thiếu cột → 422, không ghi gì. Dòng thiếu giá trị → danh sách từ chối; mã giao dịch đã có → "đã nhập trước đó". Các dòng còn lại ghi trong một transaction; lỗi giữa chừng rollback toàn bộ (FR-007).
- Hàm thuần `KhopMa(noiDung, soTien, maHieuLuc)`: chuẩn hóa nội dung (bỏ dấu, viết hoa, bỏ khoảng trắng thừa), tìm các chuỗi đúng mẫu mã có chữ số kiểm tra đúng; đúng một mã thuộc sổ hiệu lực và số tiền dương → `DA_KHOP`; còn lại `CHUA_KHOP` kèm lý do (`KHONG_CO_MA`, `NHIEU_MA`, `SO_TIEN_AM`, `MA_KHONG_TON_TAI`). Khớp không tạo giao dịch.
- `dong_sao_ke` (nội dung nhóm 3; trạng thái nhóm 2) `DA_KHOP | CHUA_KHOP | DA_GHI_NHAN | KHONG_THUOC_NCT`. Lệnh xác nhận vào số dư/cọc tạo giao dịch `NOP_CHUYEN_KHOAN` / `THU_COC` Đã xác nhận (`UNIQUE (dong_sao_ke_id)`), tính lại tình trạng (E-08), trạng thái đặt cọc. Dòng khớp hồ sơ đã ở trạng thái cuối → vẫn ghi, báo Kế toán (E-09).
- **Cặp điều chỉnh chuyển tiền**: hai giao dịch `DIEU_CHINH` Chờ duyệt liên kết (`cap_id`), trỏ dòng sao kê; lệnh duyệt áp cả hai trong một transaction (khóa hai sổ theo `nct_id` tăng); từ chối/hủy áp cả hai.

---

## E-07. Giao dịch, duyệt, thanh toán bảng, cấn trừ (mục C; BR-M11-10, 11, 14, DBR-17, DBR-28, DBR-30)

**Quyết định**:
- `giao_dich` (nhóm 2 khi `CHO_DUYET` → 3 khi `DA_XAC_NHAN`): sổ (`SO_DU`, `COC`), loại (FR-010), số tiền có dấu (`BIGINT`), thời điểm, phương thức, tham chiếu (số phiếu, dòng sao kê, bảng, giao dịch gốc, `cap_id`), người lập, người duyệt, lý do, trạng thái `CHO_DUYET | DA_XAC_NHAN | TU_CHOI | DA_HUY`, `da_dao`. Trigger: `DA_XAC_NHAN` chỉ cho đặt `da_dao` từ false.
- **Duyệt**: Hoàn tiền, Hoàn cọc, Điều chỉnh, Đảo là loại yêu cầu phê duyệt `M11.GIAO_DICH_SO_DU` của 000 (Chờ duyệt → Đã duyệt/áp dụng ngay; Từ chối; Hủy), `choTuDuyet` không áp vì Kế toán không có quyền duyệt. `kiemTraKhiDuyet` kiểm lại: giới hạn số tiền (hàm `GioiHanHoan(soDu, soDuCoc, tongChuaChot, laTrangThaiCuoi)`), người nhận còn là người đại diện Hiệu lực (`QuyenNguoiThanApi.laNguoiDaiDien`), một đảo mỗi gốc, gốc không phải thanh toán bảng. `apDung` ghi giao dịch Đã xác nhận, cập nhật sổ, đánh dấu gốc "đã đảo".
- **Thanh toán bảng** (FR-012): listener `BangChiPhiDaChot` (010, sau commit lệnh chốt của Quản lý viện) → giao dịch `THANH_TOAN_BANG` Đã xác nhận = −tổng; `UNIQUE (bang_chi_phi_id)` chống lặp (SC-002). Không đảo được.
- **Cấn trừ cọc** (FR-015): Kế toán, không duyệt; kiểm có hồ sơ kết thúc đang chuẩn bị hoặc danh sách việc sau qua đời đang mở (004), bảng kỳ cuối đã chốt (010 qua `ChiPhiChoSoDuPort`/sự kiện), số tiền ≤ số dư cọc và ≤ phần còn nợ; hai giao dịch liên kết trong một transaction.

---

## E-08. Số ngày còn đủ tiền, tình trạng, báo (mục D; BR-M11-13, Q-136, Q-223, Q-252, Q-257, Q-268)

**Quyết định**:
- Hàm thuần `PhiMotNgay(hopDong, ngay, lichBanTru, phiNgayCoSo)` theo FR-016 (tháng: `ChiPhiChoSoDuPort.phiNgayCoSo`; ngày: đơn giá ngày; bán trú tháng: giá ÷ số ngày tháng; bán trú buổi: giá buổi × số buổi có lịch 7 ngày tới ÷ 7).
- Hàm thuần `TinhTrang(soDu, tongChuaChot, phiMotNgay, cfg04)` → `CON_NO | SAP_HET_TIEN | BINH_THUONG` và số ngày (làm tròn xuống); phí = 0 chỉ xét Còn nợ.
- Hàm thuần `QuyetDinhBao(cu, moi, lanBaoTruoc, cfg05, suKien)` theo bảng chuyển tình trạng (lần tính hằng ngày, nộp tiền về Bình thường, nộp tiền chưa đủ, đảo nộp tiền): trả tác động (báo ngay, đặt lại chu kỳ, giữ chu kỳ, báo "đã ghi nhận khoản nộp", nhắc lại, báo Quản lý viện).
- `tinh_trang_so_du` (nhóm 2 dẫn xuất trên `so_du`: tình trạng, `chu_ky_tu`, `lan_bao_truoc`, `con_no_tu`, `dung_nhac`) và `lich_su_tinh_trang` (nhóm 3: ngày, số ngày, tình trạng, lần báo).
- **Job hằng ngày** 02:00 (sau nửa đêm): mỗi hồ sơ có hợp đồng Hiệu lực, chưa trạng thái cuối, và hồ sơ trạng thái cuối còn nợ chưa "dừng nhắc": tính, ghi lịch sử, quyết định báo; khóa `(nct_id, ngày)` chống báo trùng khi chạy lại (SC-012). Giao dịch nộp tiền (phiếu thu, xác nhận dòng sao kê) và đảo nộp tiền tính lại ngay trong lệnh (Q-252, Q-268).
- Thông báo người đại diện: `YeuCauThongBao` mức Trung bình với hai phần `CHUNG` ("có thông báo về tài chính…") và `CHI_PHI` (tình trạng, số dư, tổng chưa chốt, thông tin nộp tiền); 009 lọc theo quyền xem chi phí từng người (BR-M13-04).
- **Dấu mua hộ** (FR-021): bản cài `SoDuMuaHoPort.soDuKhongDu(nctId, soTien, t)` = (số dư − tổng chưa chốt) < số tiền; 010 gọi lúc hiển thị nên luôn mới nhất.
- Không có lệnh nào của module khác bị chặn vì số dư (FR-020, SC-006).

---

## E-09. Quyết toán (mục E; Q-222, Q-224)

**Quyết định**:
- Hàm thuần `DieuKienQuyetToan(bangKyCuoiDaChotVaThanhToan, soDu, soDuCoc, conChoDuyet)` → `DAT` / `CHUA_DAT` kèm lý do; bảng bổ sung chưa chốt không làm Chưa đạt.
- Bean `ThamGiaChuyenTrangThai` (`@Order` sau 013): điều kiện `KT_H_QUYET_TOAN` (`coTheMien = true`; 004 `M02.NGOAI_LE_KET_THUC` cho Kế toán lập ngoại lệ) cho `KET_THUC_LUU_TRU`. Mỗi lần kết quả đổi → phát `m11.api.DieuKienQuyetToanDaDoi(nctId, ketQua)` cho 004 làm mới màn điều kiện.
- `MucViecSauQuaDoiPort` (`QUYET_TOAN`): `KHONG_AP_DUNG` khi sổ chưa từng có giao dịch khác 0; `HOAN_THANH` khi `DAT`; gọi `DanhSachViecSauQuaDoiApi.capNhat` sau mỗi giao dịch của hồ sơ đã qua đời.
- Job hằng ngày: hồ sơ trạng thái cuối có số dư khác 0 → nhắc Kế toán mỗi CFG-M02-09 (FR-024); hợp đồng Đã hủy trước Hiệu lực hoặc Hủy tiếp nhận còn cọc → nhắc hoàn cọc mỗi CFG-M02-09 (FR-015a). Không mở lại hồ sơ.

---

## E-10. Xuất (mục F; UC-88, Q-05)

**Quyết định**: Dùng fastexcel (đã chọn ở 010) ghi luồng. `lan_xuat_so_du` (nhóm 3: người, vai trò, thời điểm, loại, khoảng, phạm vi, số dòng, tệp). Khoảng ≤ CFG-M14-01. Hàm thuần `SaoKe(soDuDauKy, giaoDich[])` bảo đảm đầu kỳ + tổng = cuối kỳ (SC-009). Tệp lưu ở kho tệp `common.tep`, không tạo lại khi dữ liệu đổi.

---

## E-11. Quyền (FR-028, FR-029)

**Quyết định**:

| `ChucNang` | Thao tác |
| --- | --- |
| `SO_DU` | `XEM` (Kế toán, Hành chính: số dư, cọc, đặt cọc; Quản lý viện); `THUC_HIEN` (Kế toán: phiếu thu, sao kê, lập giao dịch, cấn trừ, xuất) |
| `SO_DU_DUYET` | `DUYET` (Quản lý viện: giao dịch, chốt quỹ, dừng nhắc) |
| `SAO_KE` | `XEM` (Kế toán, Quản lý viện; file, mọi dòng) |

Kế toán không xem sức khỏe; khoản thuốc chỉ mã vật phẩm (010 FR-019). Người thân qua REST cổng với quyền xem chi phí. Không có thao tác ngoại tuyến.

---

## E-12. Bộ lập lịch, lưu giữ, nhật ký, kiểm thử

**Quyết định**:
- **Job**: `ViecHangNgayM11SoDu` 02:00 (tình trạng số dư; nhắc sau trạng thái cuối; nhắc hoàn cọc; đối chiếu bất biến sổ); `ViecGioHanhChinhM11SoDu` (nhắc chốt quỹ, báo Quản lý viện). Khóa chạy theo ngày; chạy bù khi khởi động.
- **Lưu giữ**: giao dịch, dòng sao kê, lần nhập, chốt quỹ, lần xuất `THEO_HO_SO` (CFG-M15-03; FR-029).
- **Nhật ký**: mọi lệnh; lần người thân xem số dư không ghi (FR-029).
- **Kiểm thử**:
  - test bảng `TrangThaiDatCoc`, `KhopMa`, `GioiHanHoan`, `PhiMotNgay`, `TinhTrang`, `QuyetDinhBao`, `DieuKienQuyetToan`, `TongQuyNgay`, `SaoKe`;
  - SC-001: bộ 3 tháng × 50 hồ sơ; SC-003: nhập cùng file 3 lần; SC-004: 200 dòng;
  - Testcontainers: `UNIQUE` dòng sao kê, thanh toán bảng, đảo, chốt quỹ ngày; trigger nhóm 3; dãy số phiếu không khoảng trống khi rollback;
  - test đồng thời: hai Kế toán ghi phiếu cùng lúc (số liên tục); duyệt hoàn tiền cùng lúc với thanh toán bảng;
  - đồng hồ giả lập cho CFG-M11-04, 05, CFG-M02-09, CFG-M13-06, CFG-M15-05, 06;
  - bản giả `ChiPhiChoSoDuPort`, 004 sự kiện, 012 `QuyenNguoiThanApi`.
- Tên test theo BR/FR (`BR_M11_12_DoiSoatIT`, `DBR_30_ThanhToanBangIT`).
