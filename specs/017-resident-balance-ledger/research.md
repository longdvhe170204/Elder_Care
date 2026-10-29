# Research: Số dư và thu chi của người cao tuổi (017)

**Ngày**: 2026-10-22 (lập lại theo spec viết lại; bản 2026-10-10 bỏ) | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

Mỗi mục gồm Quyết định, Lý do, Phương án đã cân nhắc. Quyết định nền R-01 → R-14 của 000 và các mẫu đã chốt (001 H-02 bean trạng thái, H-13 tệp; 004 T-09 mã điều kiện kết thúc; 007 S-25 `common.bieumau`; 009 khuôn thông báo; 010 P-01 → P-15, đặc biệt P-12 port nội bộ M11) được dùng lại, không quyết lại. Các quyết định dưới đây là kỹ thuật, không đổi nghiệp vụ (constitution IX). Chỗ spec chưa rõ hoặc lệch ghi ở plan, "Điểm cần báo lại".

Bản lập lại bỏ toàn bộ phần của bản cũ không còn trong spec (Q-385): phiếu thu chi và dãy số phiếu, chốt quỹ ngày, nhập sao kê và khớp mã, cặp điều chỉnh chuyển tiền, giao dịch đảo, biên bản quyết toán có vòng đời, xuất sổ cọc riêng.

---

## E-01. Vị trí trong backend

**Quyết định**: Gói `com.eldercare.backend.billing.sodu` trong module M11 (010 P-01), cấu trúc trong gói theo R-02 của 000.

| Gói con | Nội dung | Mục spec |
| --- | --- | --- |
| `so` | Sổ số dư, sổ tiền cọc, mã nộp tiền, VietQR, số dư hiển thị, REST cổng | A |
| `giaodich` | Nộp tiền mặt, ghi nhận chuyển khoản, thanh toán bảng, hoàn tiền, hoàn cọc, điều chỉnh, cấn trừ cọc; duyệt; trạng thái đặt cọc | B, C |
| `tinhtrang` | Số ngày còn đủ tiền, tình trạng, báo, dừng nhắc, dấu mua hộ | D |
| `quyettoan` | Điều kiện quyết toán, bean trạng thái, mục việc sau qua đời, Xác nhận quyết toán, bảng quyết toán, nhắc sau trạng thái cuối, nhắc hoàn cọc | E, J |
| `chungtu` | Phiếu báo phí | J |
| `xuat` | Sao kê, báo cáo thu chi | F |
| `lich` | Job hằng ngày | D, E |

---

## E-02. Chiều phụ thuộc giữa module

**Quyết định**:

| Cặp | Cách nối |
| --- | --- |
| 017 ↔ 010 (cùng M11) | Port nội bộ P-12: dùng `ChiPhiChoSoDuPort` (`tongChuaChot`, `phiNgayCoSo`, `bangDaChot`); cài `SoDuMuaHoPort.soDuKhongDu`. Nghe `billing.api.BangChiPhiDaChot` |
| 017 → 004 | Cài `TrangThaiDatCocPort`, `MucViecSauQuaDoiPort` (`QUYET_TOAN`), bean `ThamGiaChuyenTrangThai` (mã `KT_H_QUYET_TOAN`, `KT_H_XAC_NHAN_QUYET_TOAN`); đọc `HopDongDocApi` (`khoanDatCoc`, hợp đồng hiệu lực, lịch đến bán trú), `HoSoKetThucDocApi` (`dangChuanBi`, `thongTinQuyetToan`, `coNgoaiLeQuyetToanDaDuyet`), `DanhSachViecSauQuaDoiApi` (`xem`, `capNhat`); nghe `HopDongDaDoiTrangThai`, `KhoanCocDaDoi`, `HoSoKetThucDa*`, `QuaDoiDaGhiNhan`, `NgoaiLeKetThucDaDuyet` |
| 017 → 001 | `NguoiCaoTuoiApi` (`trangThai`, `laTrangThaiCuoi`, `nhanDang`); nghe `NguoiCaoTuoiDaChuyenTrangThai` (Hủy tiếp nhận) |
| 017 → 012 | `QuyenNguoiThanApi` (`quyenCoTacDung(XEM_CHI_PHI)`, `laNguoiDaiDien`, `nguoiDaiDien`) cho người nhận hoàn tiền, REST cổng, tên người đại diện trên chứng từ |
| 017 → 009, 016 | Thông báo theo khuôn `common.notification`; 016 đọc `SoDuThongKeApi` |
| 017 → `common` | `common.bieumau` (`PdfBieuMau`, `ExcelBieuMau`), `common.tep` (`FileApi`) |

Không có SPI 017 khai báo cho module khác cài. Modulith `verify()` không có vòng.

**Lý do**: SPI phía 004 đã khai cho 017; 010 dành sẵn port nội bộ.

---

## E-03. Khóa, đồng thời và bất biến

**Quyết định**:
- Nối chuỗi khóa của 010 (P-03): … → `so_du` (một dòng mỗi hồ sơ, giữ cả số dư cọc) → giao dịch. *(R-15: `so_du` → `trang_thai_nct`)*
- Mọi giao dịch Đã xác nhận cập nhật `so_du.so_du` / `so_du_coc` trong cùng transaction dưới `SELECT … FOR UPDATE` dòng `so_du`; cặp cấn trừ cọc cập nhật cả hai cột trong cùng lệnh. Bất biến "số dư = tổng giao dịch Đã xác nhận" (FR-002, DBR-28) kiểm bằng test và truy vấn đối chiếu hằng đêm (ghi lỗi kỹ thuật, không tự sửa). *(R-15: `so_du` → `trang_thai_nct`)*
- `UNIQUE (bang_chi_phi_id)` trên giao dịch thanh toán bảng (DBR-30); `UNIQUE (bang_chi_phi_id)` trên phiếu báo phí (DBR-37); cột sinh `khoa_hien_hanh` `UNIQUE` trên bảng quyết toán (DBR-38).
- Mã giao dịch ngân hàng **không** `UNIQUE` (FR-007: trùng chỉ cảnh báo, một lần chuyển khoản được ghi thành nhiều giao dịch cùng mã); chỉ mục thường để tìm trùng.

---

## E-04. Sổ, mã nộp tiền, VietQR (mục A; FR-001 → FR-004; DBR-28, DBR-29; Q-211, Q-225, Q-285, Q-335)

**Quyết định**:
- Listener `HopDongDaDoiTrangThai(… den = CHO_KY)` (004): nếu hồ sơ chưa có sổ → mở `so_du` (số dư 0, số dư cọc 0), cấp `ma_nop_tien` (tác động kèm lệnh gửi ký; idempotent theo `nct_id`). Hồ sơ mới của người quay lại có sổ mới (khóa theo `nct_id`). *(R-15: `so_du` → `trang_thai_nct`)*
- `ma_nop_tien`: `NCT` + 6 ký tự Base32 Crockford + 1 ký tự kiểm tra (mod 37), `UNIQUE`, không dùng lại; sinh ngẫu nhiên, thử lại khi trùng. Tìm theo mã không phân biệt hoa thường, bỏ khoảng trắng và gạch nối.
- Thông tin nộp tiền: ngân hàng, mã BIN, số tài khoản, chủ tài khoản đọc từ CFG-M11-10 (`ThamSoApi`). Hàm thuần `VietQrPayload(bin, soTaiKhoan, noiDung)` dựng chuỗi EMVCo (merchant account `A000000727`, dịch vụ chuyển nhanh tới tài khoản, không trường số tiền, nội dung = mã nộp tiền, CRC16-CCITT). REST trả chuỗi payload; client vẽ ảnh. Trên PDF (phiếu báo phí), backend vẽ ảnh QR bằng **ZXing core** rồi chèn ảnh qua `PdfBieuMau`.
- **Danh mục ngân hàng và CFG-M11-10** *(bổ sung 2026-10-22, chủ dự án duyệt)*:
  - Bảng `ngan_hang` (nhóm 1, cố định): `ma_bin` (khóa, 6 chữ số NAPAS), `ten_viet_tat`, `ten_day_du`, `con_hieu_luc`. Nạp bằng migration từ danh sách mã BIN NAPAS công bố cho VietQR; ngân hàng mới, sáp nhập, ngừng thì viết migration mới (thêm dòng hoặc tắt `con_hieu_luc`), không xóa, không có màn sửa. Hệ thống không gọi dịch vụ ngoài để tra mã. *(R-15: `ngan_hang` → `danh_muc_chung`)*
  - Giá trị CFG-M11-10 (kiểu bảng của 000) có ba trường: `maBin`, `soTaiKhoan`, `chuTaiKhoan`. Quản lý viện nhập trên màn Tham số của 000; ô ngân hàng là danh sách chọn lấy từ `GET /ngan-hang` của 017. Số tài khoản thật của viện không nằm trong mã nguồn hay migration; profile `demo` dùng tài khoản giả.
  - Kiểm khi lưu: 000 hiện chỉ kiểm chung giá trị kiểu bảng. Plan thêm SPI `KiemTraGiaTriThamSo` ở `administration.thamso.api` (000; `ma()` trả mã tham số, `kiemTra(giaTri)` ném lỗi 422 kèm lý do) để 000 gọi trước khi lưu; 017 cài cho CFG-M11-10: `maBin` có trong `ngan_hang` và còn hiệu lực, `soTaiKhoan` chỉ gồm chữ số (6 → 19 ký tự), `chuTaiKhoan` không rỗng. 000 không biết gì về ngân hàng (ranh giới module). *(R-15: `ngan_hang` → `danh_muc_chung`)*
  - CFG-M11-10 chưa có giá trị thì không có thông tin nộp tiền: cổng ẩn mục nộp tiền, phiếu báo phí in "Liên hệ Kế toán để biết tài khoản nộp tiền", không vẽ mã QR.
  - Ngân hàng của tài khoản đang dùng bị tắt hiệu lực sau đó: vẫn dựng VietQR theo `maBin` đã lưu (mã BIN không đổi khi ngân hàng chỉ đổi tên); Quản lý viện được báo ở lần lưu tham số kế tiếp.
- Hiển thị (FR-004): số dư trên hồ sơ cho Kế toán, Hành chính, Quản lý viện (`BALANCE_LEDGER, XEM`); giao dịch thanh toán bảng chỉ hiện tổng (Q-133).
- REST cổng `/cong/nguoi-cao-tuoi/{nctId}/so-du` (số dư, cọc, giao dịch Đã xác nhận, thông tin nộp tiền) kiểm `QuyenNguoiThanApi.quyenCoTacDung(nguoiThanId, nctId, XEM_CHI_PHI, now)` mỗi lần gọi (SC-010).

**Phương án đã cân nhắc**: Thư viện VietQR riêng (ít lựa chọn được duy trì; chuẩn ngắn, tự viết kèm test vector của NAPAS). Client vẽ QR cả trên PDF (không làm được vì PDF dựng ở máy chủ).

---

## E-05. Ghi nhận tiền nộp và trạng thái đặt cọc (mục B; FR-005 → FR-007; BR-M11-12, 16; DBR-29)

**Quyết định**:
- **Tiền mặt** `POST /nguoi-cao-tuoi/{nctId}/nop-tien-mat`: sổ (`SO_DU` | `COC`), số tiền > 0, người nộp, thời điểm thu, ghi chú (ghi được số chứng từ của phần mềm kế toán). Giao dịch `NOP_TIEN_MAT` (sổ số dư) hoặc `THU_COC` phương thức `TIEN_MAT` (sổ cọc), Đã xác nhận ngay. Không cấp số, không in phiếu (FR-005).
- **Chuyển khoản** `POST /nguoi-cao-tuoi/{nctId}/nop-chuyen-khoan`: tìm hồ sơ qua `GET /so-du/tim-theo-ma?ma=` (mã nộp tiền); thân: danh sách dòng (mỗi dòng: sổ, số tiền) cùng ngày nhận, mã giao dịch ngân hàng (bắt buộc), người nộp, ghi chú. Nhiều dòng (khác sổ) ghi trong một transaction cùng mã (FR-007; khác hồ sơ thì gọi lệnh cho từng hồ sơ). Mã đã có ở giao dịch khác (mọi hồ sơ, trừ các dòng của chính lệnh) và thân không có `xacNhanTrungMa = true` → 409 `MA_GIAO_DICH_DA_DUNG` kèm giao dịch trước; gửi lại với cờ thì lưu (DBR-29: cảnh báo, không chặn).
- Mỗi giao dịch nộp: cập nhật sổ; tính lại tình trạng ngay (E-07, Q-252, Q-257); tính lại trạng thái đặt cọc khi sổ cọc đổi.
- **Đặt cọc** (FR-006): hàm thuần `TrangThaiDatCoc(khoanCanCoc | KHONG_YEU_CAU, soDuCoc)` → `KHONG_YEU_CAU | DA_DAP_UNG | CHUA_DAP_UNG`. `TrangThaiDatCocPort.trangThai(hopDongId)` tính lúc gọi kèm giao dịch làm đổi gần nhất và Kế toán lập. Mỗi lần kết quả đổi (giao dịch cọc, điều chỉnh cọc được duyệt, `KhoanCocDaDoi`) phát `billing.api.TrangThaiDatCocDaDoi` cho 004.
- Hành chính không có quyền ghi (`BALANCE_LEDGER` chỉ `XEM`) → 403 (US2 #5).

**Phương án đã cân nhắc**: Một endpoint cho cả tiền mặt và chuyển khoản (thân rẽ nhánh theo phương thức, khó kiểm trường bắt buộc); bắt `UNIQUE` mã ngân hàng (trái FR-007).

---

## E-06. Giao dịch, duyệt, thanh toán bảng, cấn trừ (mục C; FR-010 → FR-015a; BR-M11-10, 11, 14; DBR-17, DBR-28, DBR-30)

**Quyết định**:
- `giao_dich` (nhóm 2 khi `CHO_DUYET` → 3 khi `DA_XAC_NHAN`): sổ, loại theo FR-010, số tiền có dấu, thời điểm, phương thức, tham chiếu (mã giao dịch ngân hàng, bảng chi phí, giao dịch được điều chỉnh, giao dịch liên kết của cặp cấn trừ), người lập, người duyệt, lý do, ghi chú, trạng thái, `da_dieu_chinh`.
- **Duyệt** (FR-013): Hoàn tiền, Hoàn cọc, Điều chỉnh là loại yêu cầu phê duyệt `M11.GIAO_DICH_SO_DU` của 000 (Chờ duyệt → Đã duyệt và áp dụng ngay; Từ chối; Hủy bởi người lập). Kế toán không có quyền duyệt. Handler:
  - `kiemTraKhiDuyet`: hàm thuần `GioiHanHoan(soDu, soDuCoc, tongChuaChot, laTrangThaiCuoi, loai, soTien)` (FR-014); người nhận còn là người đại diện Hiệu lực (`QuyenNguoiThanApi.laNguoiDaiDien`) trừ khi lúc lập hồ sơ không còn người đại diện (người nhận khác, có lý do); điều chỉnh không trỏ giao dịch thanh toán bảng (FR-011). Không đạt → chặn duyệt, báo Kế toán (US6 #3, #8).
  - `apDung`: ghi Đã xác nhận, cập nhật sổ, đặt `da_dieu_chinh` của giao dịch gốc (nếu có), tính lại tình trạng (điều chỉnh giảm khoản nộp: Q-268, sửa theo Q-385), tính lại trạng thái đặt cọc, điều kiện quyết toán.
- **Sửa sai** (FR-011): điều chỉnh có lý do, MAY trỏ giao dịch gốc (`giao_dich_goc_id`); ghi nhầm người thì điều chỉnh giảm ở hồ sơ sai và ghi nhận lại ở hồ sơ đúng (US3 #4). Không có loại `DAO`.
- **Thanh toán bảng** (FR-012): listener `BangChiPhiDaChot` (010, sau commit lệnh chốt) → giao dịch `THANH_TOAN_BANG` Đã xác nhận = −tổng (tổng 0 vẫn ghi); `UNIQUE (bang_chi_phi_id)` chống lặp (SC-002). Không điều chỉnh được.
- **Cấn trừ cọc** (FR-015): Kế toán, không duyệt; điều kiện: `HoSoKetThucDocApi.dangChuanBi` có, hoặc danh sách việc sau qua đời đang mở (`DanhSachViecSauQuaDoiApi.xem`), và bảng kỳ cuối đã chốt (đã có giao dịch thanh toán của bảng kỳ cuối); số tiền ≤ số dư cọc và ≤ phần còn nợ; hai giao dịch `CAN_TRU_COC` liên kết (`giao_dich_lien_ket_id`) cùng transaction (SC-013).
- **Nhắc hoàn cọc** (FR-015a): listener hợp đồng Đã hủy trước Hiệu lực, Hủy tiếp nhận (001) đặt `so_du.can_hoan_coc_tu`; job hằng ngày nhắc Kế toán mỗi CFG-M02-09 tới khi số dư cọc về 0.

---

## E-07. Số ngày còn đủ tiền, tình trạng, báo (mục D; FR-016 → FR-021; BR-M11-13, 15; Q-136, Q-223, Q-252, Q-257, Q-268, Q-334)

**Quyết định**:
- Hàm thuần `PhiMotNgay(hopDong, ngay, lichBanTru, phiNgayCoSo)` theo FR-016 (tháng: `ChiPhiChoSoDuPort.phiNgayCoSo`; ngày: đơn giá ngày; bán trú tháng: giá ÷ số ngày tháng; bán trú buổi: giá buổi × số buổi có lịch 7 ngày tới ÷ 7).
- Hàm thuần `TinhTrang(soDu, tongChuaChot, phiMotNgay, cfg04)` → `CON_NO | SAP_HET_TIEN | BINH_THUONG` và số ngày (làm tròn xuống); phí = 0 chỉ xét Còn nợ (FR-017).
- Hàm thuần `QuyetDinhBao(cu, moi, lanBaoTruoc, conNoTu, cfg05, suKien)` theo bảng chuyển tình trạng (lần tính hằng ngày; nộp tiền về Bình thường; nộp tiền chưa đủ; điều chỉnh giảm được duyệt): trả tác động (báo ngay, đặt lại chu kỳ, giữ chu kỳ, báo "đã ghi nhận khoản nộp", nhắc lại, báo Quản lý viện).
- Tình trạng lưu trên `so_du` (`tinh_trang`, `so_ngay_con_du`, `chu_ky_tu`, `lan_bao_truoc`, `con_no_tu`, `dung_nhac`); mỗi lần tính ghi `lich_su_tinh_trang` (nhóm 3). *(R-15: `so_du` → `trang_thai_nct`; `lich_su_tinh_trang` → `lich_su_chung`)*
- **Job hằng ngày** 02:00: hồ sơ có hợp đồng Hiệu lực chưa trạng thái cuối, và hồ sơ trạng thái cuối còn nợ chưa "dừng nhắc" (FR-020); khóa `(nct_id, ngày, nguồn)` chống báo trùng khi chạy lại (SC-012). Giao dịch nộp tiền và điều chỉnh giảm khoản nộp tính lại ngay trong lệnh.
- **Thông báo** người đại diện: mức Trung bình, hai phần `CHUNG` ("có thông báo về tài chính của [họ tên], vui lòng liên hệ Kế toán") và `CHI_PHI` (tình trạng, số dư, tổng chưa chốt, thông tin nộp tiền); nhóm `Representative(nctId)`; 009 lọc phần `CHI_PHI` theo quyền xem chi phí (BR-M13-04).
- **Dừng nhắc** (FR-020): `POST /nguoi-cao-tuoi/{nctId}/so-du/dung-nhac` (Quản lý viện, lý do; chỉ hồ sơ trạng thái cuối còn nợ).
- **Dấu mua hộ** (FR-021): `SoDuMuaHoPort.soDuKhongDu(nctId, soTien, t)` = (số dư − tổng chưa chốt) < số tiền; 010 hỏi lúc hiển thị nên luôn mới.
- Không lệnh nào của module khác bị chặn vì số dư (FR-020, SC-006).

---

## E-08. Điều kiện quyết toán và mục việc sau qua đời (mục E; FR-022 → FR-024; Q-222, Q-224, Q-379; BR-M11-22)

**Quyết định**:
- Hàm thuần `DieuKienQuyetToan(kyCuoiDaChotVaTru, soDu, soDuCoc, soChoDuyet, xacNhanHienHanh, coGiaoDichSauXacNhan, ngoaiLe)` trả hai kết quả (theo lựa chọn của chủ dự án 2026-10-22: ý Chờ duyệt không miễn được, BR-M11-22):
  - `KT_H_QUYET_TOAN` (ba ý tiền: bảng kỳ cuối Đã chốt và có giao dịch thanh toán; số dư cọc 0; số dư 0): Đạt, Chưa đạt kèm lý do; "Đạt (ngoại lệ)" do 004 áp khi ngoại lệ đã duyệt;
  - `KT_H_XAC_NHAN_QUYET_TOAN`: Đạt khi không còn giao dịch Chờ duyệt **và** có phiên bản bảng quyết toán hiện hành **và** không có giao dịch Đã xác nhận nào của hồ sơ sau thời điểm xác nhận đó; không miễn được.
  - Bảng bổ sung chưa chốt không làm Chưa đạt (Q-134).
- Bean `ThamGiaChuyenTrangThai` (`@Order` sau 013) trả hai mã cho `KET_THUC_LUU_TRU` (`coTheMien` như trên). Mỗi lần kết quả đổi phát `billing.api.DieuKienQuyetToanDaDoi(nctId)` để 004 làm mới màn điều kiện.
- `MucViecSauQuaDoiPort` (`QUYET_TOAN`): `KHONG_AP_DUNG` khi hai sổ chưa từng có giao dịch khác 0; `HOAN_THANH` khi `KT_H_XAC_NHAN_QUYET_TOAN` Đạt và `KT_H_QUYET_TOAN` Đạt hoặc có ngoại lệ đã duyệt (`HoSoKetThucDocApi.coNgoaiLeQuyetToanDaDuyet`; 004 FR-071a, Q-387); gọi `DanhSachViecSauQuaDoiApi.capNhat` sau mỗi giao dịch, xác nhận của hồ sơ đã qua đời.
- **Sau trạng thái cuối** (FR-024): job hằng ngày nhắc Kế toán mỗi CFG-M02-09 khi số dư khác 0 hoặc có giao dịch sau lần xác nhận cuối; dừng khi số dư 0 và đã xác nhận lại. Không mở lại hồ sơ.

---

## E-09. Xác nhận quyết toán và bảng quyết toán (FR-034; BR-M11-22; DBR-38; UC-101)

**Quyết định**:
- `POST /nguoi-cao-tuoi/{nctId}/quyet-toan/xac-nhan` (Kế toán, `SETTLEMENT_RECORD, THUC_HIEN`, khóa `so_du`). Điều kiện (409 kèm danh sách lý do): *(R-15: `so_du` → `trang_thai_nct`)*
  - có hồ sơ kết thúc Đang chuẩn bị (`HoSoKetThucDocApi.dangChuanBi`), hoặc danh sách việc sau qua đời đang mở, hoặc hồ sơ trạng thái cuối có giao dịch sau lần xác nhận trước (FR-024);
  - ba ý tiền Đạt, hoặc `HoSoKetThucDocApi.coNgoaiLeQuyetToanDaDuyet(nctId)` (hồ sơ kết thúc lưu trú, 004 FR-064; danh sách việc sau qua đời, 004 FR-071a, Q-387);
  - không còn giao dịch Chờ duyệt (không miễn được).
- Số liệu bảng (chụp lúc xác nhận, lưu `JSON` cùng phiên bản): thông tin viện (CFG-M11-11); người cao tuổi (`nhanDang`), người đại diện (`QuyenNguoiThanApi.nguoiDaiDien`); loại lưu trú, hợp đồng, ngày bắt đầu, ngày kết thúc dự kiến, trường hợp kết thúc (`HoSoKetThucDocApi.thongTinQuyetToan`); tổng bảng kỳ cuối theo loại (`ChiPhiChoSoDuPort.bangDaChot`); số dư trước quyết toán (số dư ngay trước giao dịch thanh toán bảng kỳ cuối); tiền cọc đã thu, đã cấn trừ, đã hoàn; các giao dịch nộp, hoàn kể từ thanh toán bảng kỳ cuối; số còn nợ hoặc còn phải hoàn (= số dư hiện tại; khác 0 chỉ khi có ngoại lệ); người xác nhận, thời điểm, số phiên bản.
- Mỗi lần xác nhận: thêm phiên bản `bang_quyet_toan` mới, chuyển phiên bản trước sang không hiện hành (chỉ cột `hien_hanh`, trigger), dựng PDF bằng `PdfBieuMau` (dòng "Chứng từ này không thay thế hóa đơn"; khoản thuốc chỉ "Thuốc" và mã vật phẩm), `FileApi.upload` + `attach` (`M11.BANG_QUYET_TOAN`), nhật ký `BQT_XAC_NHAN`, thông báo người thân có quyền xem chi phí (Nhẹ, phần `CHI_PHI`), `DieuKienQuyetToanDaDoi`. *(R-15: `bang_quyet_toan` → `su_kien_chung`)*
- **Đính kèm bản ký** `POST /bang-quyet-toan/{id}/ban-ky` (Kế toán; phiên bản hiện hành; một lần mỗi phiên bản, trigger): gắn tệp (`M11.BAN_KY_QUYET_TOAN`); không đổi điều kiện.
- Tải: `GET /bang-quyet-toan/{id}/file` trả đúng file đã lưu; mỗi lần tải ghi nhật ký `CHUNG_TU_TAI` (FR-037).

**Phương án đã cân nhắc**: Dựng PDF lúc tải từ `JSON` đã chụp (không bảo đảm giống byte qua các lần nâng thư viện, font; trái FR-037). Lưu file trong MySQL (`BLOB`; trái cách lưu tệp đã chọn, 001 H-13).

---

## E-10. Phiếu báo phí (FR-031, FR-032, FR-037, FR-038; BR-M11-21, 23; DBR-37; UC-100)

**Quyết định**:
- Danh sách chờ phát hành `GET /phieu-bao-phi/cho-phat-hanh`: bảng có giao dịch thanh toán (`THANH_TOAN_BANG`) mà chưa có phiếu.
- `POST /bang-chi-phi/{bangId}/phieu-bao-phi` (Kế toán, `PAYMENT_NOTICE, THUC_HIEN`): bảng chưa có giao dịch thanh toán → 409; đã có phiếu → 409 `DA_PHAT_HANH` (`UNIQUE (bang_chi_phi_id)`, DBR-37). Số liệu chụp: thông tin viện, mã phiếu, ngày phát hành, người phát hành; người cao tuổi, người đại diện, kỳ, loại bảng; khoản và tổng theo loại (`ChiPhiChoSoDuPort.bangDaChot`); tổng; số dư ngay trước và ngay sau giao dịch thanh toán của bảng (tính từ sổ: tổng giao dịch Đã xác nhận có thời điểm xác nhận trước giao dịch đó); khi số dư sau âm: số còn nợ, mã nộp tiền, tài khoản thu, ảnh mã QR (E-04).
- Mã phiếu `PBP-<yyyyMM của kỳ>-<bangId>`, `UNIQUE` (một bảng một phiếu nên không cần dãy số).
- Dựng PDF (`PdfBieuMau`) và Excel (`ExcelBieuMau`, cùng số liệu) trong lệnh, lưu cả hai qua `FileApi` (`M11.PHIEU_BAO_PHI`). Nhật ký `PBP_PHAT_HANH`; thông báo người thân có quyền xem chi phí (Nhẹ; nhóm `FamilyWithRight(nctId, XEM_CHI_PHI)`).
- Tải `GET /phieu-bao-phi/{id}/file?dinhDang=PDF|XLSX`: đúng file đã lưu; nhật ký `CHUNG_TU_TAI`.

---

## E-11. Sao kê và báo cáo thu chi (mục F; FR-025 → FR-027; UC-88; Q-381, Q-386)

**Quyết định**:
- `POST /xuat/sao-ke` (Kế toán; Quản lý viện xem được): danh sách hồ sơ, khoảng, định dạng `XLSX | PDF`; khoảng > CFG-M14-01 → 422. Hàm thuần `SaoKe(soDuDauKy, soDuCocDauKy, giaoDich[])` bảo đảm đầu kỳ + tổng = cuối kỳ cho từng sổ (SC-009); chỉ giao dịch Đã xác nhận.
- `POST /xuat/bao-cao-thu-chi`: khoảng, định dạng. Hàm thuần `BaoCaoThuChi(giaoDich[], soDuCuoiKhoang[])`: tổng nộp (`NOP_TIEN_MAT`, `NOP_CHUYEN_KHOAN`, `THU_COC`), tổng đã trừ theo bảng (`THANH_TOAN_BANG`, lấy trị tuyệt đối; bảng tổng âm làm giảm tổng), tổng hoàn (`HOAN_TIEN`, `HOAN_COC`), danh sách hồ sơ có số dư < 0 tại hết ngày cuối khoảng kèm số còn nợ (plan điểm báo lại 5). Không tách theo phương thức, không có tổng điều chỉnh, tổng hoàn cọc riêng (Q-386).
- File dựng một lần, lưu qua `FileApi` (`M11.FILE_XUAT`), ghi `lan_xuat_so_du` (FR-027); tải lại trả đúng file. PDF có thông tin viện (CFG-M11-11). *(R-15: `lan_xuat_so_du` → hành động `nhat_ky`)*
- `common.tep` hiện chỉ nhận PDF, JPEG, PNG; plan thêm kiểu `.xlsx` cho tệp do hệ thống tạo (không mở cho `POST /tep`), sửa nhỏ ở `FileApi.upload` (chủ `common`).
- **Cổng** `POST /cong/nguoi-cao-tuoi/{nctId}/sao-ke` (người thân có quyền xem chi phí; khoảng ≤ CFG-M14-01; `XLSX | PDF`) → cùng hàm, ghi `lan_xuat_so_du` với vai trò người thân, trả file. *(R-15: `lan_xuat_so_du` → hành động `nhat_ky`)*

---

## E-12. Quyền (FR-028, FR-029; Phụ lục 27 dòng "Số dư, thu chi", "Sao kê, báo cáo thu chi", "Phiếu báo phí", "Bảng quyết toán")

**Quyết định** (khóa `Function` của 002, V002_09):

| `Function` | QL | HC | KT | NT |
| --- | --- | --- | --- | --- |
| `BALANCE_LEDGER` | Duyệt (³²: hoàn tiền, hoàn cọc, điều chỉnh, dừng nhắc) | Xem (số dư, cọc, đặt cọc) | Thực hiện (nộp tiền, lập giao dịch, cấn trừ) | Xem¹⁶ (cổng) |
| `STATEMENT_REPORT` | Xem | — | Thực hiện | Xem¹⁶ (sao kê của người mình) |
| `PAYMENT_NOTICE` | Xem | — | Thực hiện | Xem¹⁶ |
| `SETTLEMENT_RECORD` | Xem | — | Thực hiện (xác nhận, đính kèm bản ký) | Xem¹⁶ |

Kế toán không xem sức khỏe; khoản thuốc chỉ mã vật phẩm. `QuyenTepProvider` của 017 cho các loại tệp `M11.PHIEU_BAO_PHI`, `M11.BANG_QUYET_TOAN`, `M11.BAN_KY_QUYET_TOAN`, `M11.FILE_XUAT` theo bảng trên (file xuất: người xuất và vai trò xem tương ứng). Không có thao tác ngoại tuyến.

---

## E-13. Kiểm thử và hàm thuần

**Quyết định**:
- Test bảng: `TrangThaiDatCoc`, `GioiHanHoan`, `PhiMotNgay`, `TinhTrang`, `QuyetDinhBao`, `DieuKienQuyetToan`, `SaoKe`, `BaoCaoThuChi`, `VietQrPayload` (test vector chuẩn và CRC).
- SC-001: bộ 3 tháng × 50 hồ sơ dựng từ bản giả 010 sinh sự kiện chốt bảng; so số dư với tính tay.
- Test tích hợp MySQL: `UNIQUE` thanh toán bảng, phiếu báo phí, phiên bản hiện hành; trigger nhóm 3; cột sinh.
- Test đồng thời: hai Kế toán ghi nộp cùng hồ sơ (số dư đúng); duyệt hoàn tiền cùng lúc với thanh toán bảng (một bên bị chặn theo FR-014); hai lần phát hành phiếu (một thành công); hai lần xác nhận (hai phiên bản tuần tự, một hiện hành).
- Chứng từ: trích chữ PDF để kiểm dòng "Chứng từ này không thay thế hóa đơn", không có tên thuốc; Excel đọc lại ô; tải lại so byte (SC-016); thời gian dựng ≤ 5 giây (SC-017).
- Đồng hồ giả lập cho CFG-M11-04, 05, CFG-M02-09, CFG-M15-05, 06.
- Bản giả: `ChiPhiChoSoDuPort` (010), `HoSoKetThucDocApi` và sự kiện 004, `QuyenNguoiThanApi` (012), `S3Client` (001).
- Tên test theo BR/FR (`BR_M11_12_ChuyenKhoanIT`, `DBR_30_ThanhToanBangIT`, `BR_M11_21_PhieuBaoPhiIT`).

---

## E-14. Bộ lập lịch, lưu giữ, nhật ký, thông báo

**Quyết định**:
- **Job** `ViecHangNgayM11SoDu` 02:00: tình trạng số dư (E-07); nhắc sau trạng thái cuối (FR-024); nhắc hoàn cọc (FR-015a); đối chiếu bất biến sổ. Khóa chạy theo ngày (ShedLock), chạy bù khi khởi động.
- **Lưu giữ** (FR-029): giao dịch, lịch sử tình trạng, lần xuất, phiếu báo phí, bảng quyết toán và tệp `THEO_HO_SO` (CFG-M15-03) qua `LuuTruProvider`.
- **Nhật ký**: mọi lệnh; phát hành, xác nhận, tải chứng từ, xuất (19.4); lần người thân xem số dư không ghi (FR-029).
- **Thông báo** (FR-030): theo bảng spec; nhóm `Representative`, `FamilyWithRight(XEM_CHI_PHI)`, `FacilityRole(KE_TOAN)`, `FacilityRole(QUAN_LY_VIEN)`, `Approvers`; loại thông tin `CHI_PHI` cho phần chi phí. Bỏ thông báo chốt quỹ, sao kê chưa khớp của bản cũ.
