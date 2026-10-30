# Research: Quản lý đồ gửi của người cao tuổi (013)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

Mỗi mục gồm Quyết định, Lý do, Phương án đã cân nhắc. Quyết định nền R-01 → R-14 của 000 và các mẫu đã chốt ở 001 → 012 (H-02 bean trạng thái, T-09 mã điều kiện kết thúc, R-05 nhóm dữ liệu và đính chính, B-02 khuôn thông báo) được dùng lại, không quyết lại. Các quyết định dưới đây là kỹ thuật; chúng không đổi nghiệp vụ (constitution IX). Chỗ spec chưa rõ hoặc mâu thuẫn được ghi ở plan, mục "Điểm cần báo lại".

---

## K-01. Vị trí trong backend

**Quyết định**: Feature 013 là module M12 – Đồ gửi (mục 16), gói `com.eldercare.backend.belongings`.

| Gói con | Nội dung | Mục spec |
| --- | --- | --- |
| `belongings.danhmuc` | Loại đồ (lịch sử dấu "cần đồng ý"), mức tình trạng, vị trí lưu giữ | A |
| `belongings.dogui` | Đồ gửi, bản ghi bàn giao, tách đồ, tính lại trạng thái, đính chính | B, C, D |
| `belongings.tra` | Lệnh Trả, xác minh người nhận, lần chặn trả, xác nhận người nhận khác | E |
| `belongings.dongy` | Đồng ý cho tự giữ | D (FR-015a) |
| `belongings.suco` | Thông báo thất lạc/hư hỏng, ghi hoặc liên kết sự cố 007 | F |
| `belongings.ketthuc` | Điều kiện `KT_B_DO_GUI`, mục `DO_GUI` sau qua đời, đề nghị xử lý đồ không người nhận | G |
| `belongings.kiemke` | Phiếu kiểm kê, đồ không rõ chủ | H2 |
| `belongings.trangthai`, `belongings.lich`, `belongings.api` | Bean trạng thái; job; giao diện công khai | |

---

## K-02. Chiều phụ thuộc giữa module

**Quyết định**: 013 phụ thuộc 000 → 012. Mọi chỗ module thấp hơn cần dữ liệu đồ gửi đã khai báo SPI; M12 cài.

| SPI / API | Người khai | Bản cài hoặc cách dùng của 013 |
| --- | --- | --- |
| `ThamGiaChuyenTrangThai` (`resident.api`) | 001 H-02 | Điều kiện `KT_B_DO_GUI` (`coTheMien = true`) cho `KET_THUC_LUU_TRU`; chặn Tiếp nhận, Giao sử dụng sau trạng thái cuối (FR-027) bằng đọc trạng thái, không cần tác động |
| `MucViecSauQuaDoiPort` (`admission.api`) | 004 | `ma() = DO_GUI`; trạng thái theo FR-026 |
| `DanhSachViecSauQuaDoiApi.capNhat(nctId)` | 004 | Gọi sau mỗi lệnh đổi trạng thái đồ của người đã qua đời |
| `DoGuiBanGiaoPort` (`staffing.api`) | 008 | `dangGiu(nhanVienIds)`: đồ có giá trị Đang giữ theo người giữ (FR-014a) |
| `LoaiYeuCauHandler` (000) | 000 | `M12.XU_LY_DO_KHONG_NGUOI_NHAN` (`choTuDuyet = false`; K-09) |
| `DinhChinhDuocProvider` (000) | 000 | `M12.BAN_GIAO_DO_GUI`, `M12.PHIEU_KIEM_KE` (K-05) |
| `SuCoLenhApi.ghiNhan`, `themDienBien` (MANDATORY); `SuCoApi.xem` | 007 | K-07 |
| `QuyenNguoiThanApi`, `NguoiDaiDienPort` | 012, 004 (012 cài) | Người có quyền nhận, giấy tờ, người đại diện, `xemDoGui` |
| `CaTrucApi.dangTrongCa`; sự kiện `CaDaKetThuc` | 008 | "Người giữ vắng mặt" (FR-014); nhắc hết ca (FR-014a) |
| `TaiKhoanApi.theoNhanVien` | 002 | Tài khoản không Hoạt động (FR-014) |
| `ViTriApi`; sự kiện `DaChuyenGiuong` | 003 | FR-028 |

Không có SPI 013 khai cho module sau. 016 đọc `DoGuiThongKeApi`.

**Lý do**: Modulith `verify()` từ chối vòng (000 R-02); các SPI 004, 008 đã khai đúng cho 013.

---

## K-03. Khóa và đồng thời (FR-011b)

**Quyết định**:
- **Thứ tự khóa**: … → `nguoi_than_nct` (M10) → `do_gui_nct` (M12, một dòng mỗi người cao tuổi) → `do_gui` (theo `id` tăng) → `xac_nhan_nguoi_nhan` / `dong_y_tu_giu` → `phieu_kiem_ke`. Bean trạng thái của 013 chạy sau 012. *(R-15: `nguoi_than_nct` → `trang_thai_nct`; `xac_nhan_nguoi_nhan`, `phieu_kiem_ke` → `ho_so_chung`; `dong_y_tu_giu` → `do_gui` (cột))*
- Mỗi lệnh bàn giao gửi `phienBan` của từng đồ trong thân lệnh (lệnh trên nhiều đồ: danh sách `(doGuiId, phienBan)`); khóa `do_gui` theo `id` tăng, so phiên bản; lệch → 409 `TRANG_THAI_DA_DOI` kèm trạng thái mới của đồ (FR-011b). Không dùng `If-Match` vì một lệnh có nhiều đồ.
- Lệnh trên đồ của một người cao tuổi khóa `do_gui_nct` trước để điều kiện `KT_B_DO_GUI` và lệnh Kết thúc lưu trú của 004 (đọc dưới khóa `nguoi_cao_tuoi` của 001 rồi xuống `do_gui_nct`) không lệch. *(R-15: `do_gui_nct` → `trang_thai_nct`)*

---

## K-04. Đồ gửi, bản ghi bàn giao, tách đồ (mục B, D; DBR-22, BR-M12-01)

**Quyết định**:
- `do_gui` (nhóm 2): trạng thái (`DANG_GIU`, `DANG_SU_DUNG`, `DA_TRA`, `THAT_LAC`, `HU_HONG`, `DA_XU_LY`), người giữ (`NHAN_VIEN` id / `NGUOI_CAO_TUOI`), vị trí, `ban_giao_hien_hanh_id`, `do_goc_id`, `tiep_nhan_goc_id`, `bi_loai` (Hủy ghi nhận Tiếp nhận). Các cột này là **giá trị suy ra** từ bản ghi bàn giao hiện hành gần nhất, cập nhật trong cùng transaction với lệnh hoặc đính chính (SC-008 tra cứu một dòng).
- `ban_giao_do_gui` (nhóm 3): loại (`TIEP_NHAN`, `GIAO_SU_DUNG`, `THU_LAI`, `CHUYEN_GIU`, `TRA`, `BAO_THAT_LAC`, `GHI_HU_HONG`, `TIM_THAY`, `XU_LY`), trạng thái đích, người giao, người nhận, thời điểm, số lượng, mức tình trạng và mô tả, vị trí, lý do, người ghi nhận, lần trả (`lan_tra_id`), sự cố liên kết, đề nghị xử lý, phiếu kiểm kê nguồn.
- Hàm thuần `KiemTraLenhBanGiao(do, lenh, nguoiThucHien, t)` theo bảng trạng thái và FR-014 (người giao là người giữ hiện tại; ngoại lệ Trả từ Đang sử dụng, Báo thất lạc, Tìm thấy, Hành chính với lý do "người giữ vắng mặt" khi người giữ không có ca đang diễn ra hoặc tài khoản không Hoạt động).
- **Tách** (FR-012, FR-013): hàm `TachDo(do, soLuongBanGiao)` → đồ mới (`do_goc_id`, `tiep_nhan_goc_id` của đồ gốc) mang trạng thái đích, đồ cũ giữ phần còn lại; bản ghi bàn giao của đồ mới là bản ghi của lệnh. Đếm thiếu: lệnh tách phần thiếu và ghi `BAO_THAT_LAC` cho nó cùng lần. Bất biến "tổng số lượng các đồ cùng `tiep_nhan_goc_id` = số lượng tiếp nhận (sau đính chính)" kiểm cuối mỗi lệnh và đính chính (SC-001).
- **Tiếp nhận và giao ngay** (FR-005): một lệnh, hai bản ghi; với loại cần đồng ý: người giao phải là người đại diện (`NguoiDaiDienPort`) và lệnh kèm bản ký → tạo `dong_y_tu_giu` Hiệu lực trong cùng lệnh. *(R-15: `dong_y_tu_giu` → `do_gui` (cột))*
- **Ảnh** (FR-008 → FR-010): `ban_giao_anh` (nhóm 3, tệp `common.tep`); dấu "đồ có giá trị" tính từ CFG-M12-01 lúc lệnh và lưu trên bản ghi (`la_do_co_gia_tri`), nên đổi tham số không làm hỏng bản ghi cũ. *(R-15: `ban_giao_anh` → cột `JSON` của `ban_giao_do_gui`)*
- **Mức tình trạng** (FR-015): giảm mức → phản hồi 409 `CAN_CHON_XU_LY_TINH_TRANG` nếu thân lệnh chưa có `xuLyTinhTrang` (`GHI_NHAN` hoặc chuyển sang lệnh Ghi hư hỏng); mức "Không dùng được" chỉ ở Tiếp nhận hoặc Ghi hư hỏng.

**Phương án loại**: tính trạng thái mỗi lần đọc từ bản ghi gần nhất — chậm cho danh sách, điều kiện kết thúc.

---

## K-05. Đính chính bản ghi bàn giao (FR-011a, Q-160)

**Quyết định**:
- `DinhChinhDuocProvider` cho `M12.BAN_GIAO_DO_GUI`: không gắn tầng (000 FR-026 (c), người duyệt Quản lý viện); `kiemTra` chặn đổi loại, trạng thái đích; "Hủy ghi nhận" chỉ cho bản ghi hiện hành gần nhất của đồ (và không cho `TIEP_NHAN` khi đồ đã có bản ghi sau). `sauKhiDinhChinh` (MANDATORY) tính lại cột suy ra của đồ từ bản ghi hiện hành liền trước, kiểm bất biến tách, gọi `DanhSachViecSauQuaDoiApi.capNhat`.
- Bản ghi bị hủy có sự cố liên kết → thông báo người xử lý sự cố (007 `SuCoApi.xem(…).nguoiPhuTrach`) kèm lý do "ghi nhầm đồ gửi"; **không** gọi `SuCoLenhApi.huyDoNguonDoGui` (Q-307; xem plan, điểm báo lại 1).
- Người đã nhận thông báo về sự kiện bị hủy: tra `ThongBaoTraCuuApi` theo khóa `(loại sự kiện, đồ gửi)` của bản ghi, gửi thông báo đính chính Nhẹ.
- Hủy ghi nhận `TIEP_NHAN` của đồ không có bản ghi sau → `bi_loai = true` (khỏi mọi danh sách), bản gốc giữ.

---

## K-06. Trả, xác nhận người nhận khác, đồng ý cho tự giữ (mục E; BR-M12-04, Q-149, Q-150, Q-153, Q-155, Q-253, Q-258)

**Quyết định**:
- **Lệnh Trả** là một lệnh trên nhiều đồ cho cùng người nhận (`lan_tra`, nhóm 3: người nhận, xác minh danh tính, chữ ký (tệp), ý kiến tình trạng, xác nhận người nhận khác dùng), mỗi đồ một bản ghi `TRA` trỏ `lan_tra_id` (FR-019). *(R-15: `lan_tra` → `ban_giao_do_gui` (`loai`))*
- Hàm thuần `NguoiNhanHopLe(nguoiNhan, nct, cacDo, xacNhan, vaiTro, t)`:
  1. người có quyền nhận (`QuyenNguoiThanApi.laNguoiDaiDien` hoặc `duocPhepDonTai`) → xác minh giấy tờ: khớp số đăng ký (`giayTo`); hồ sơ chưa có giấy tờ → ghi loại, số xuất trình, đối chiếu họ tên, số điện thoại, nhắc Hành chính bổ sung; không khớp → `lan_chan_tra` (nhóm 3, transaction riêng) và 409 `CHAN_TRA`; *(R-15: `lan_chan_tra` → `ban_giao_do_gui` (`loai`))*
  2. chính người cao tuổi với mọi đồ không thuộc loại "cần đồng ý" → được (Q-253);
  3. khác → cần `xac_nhan_nguoi_nhan` `HIEU_LUC`, khớp người nhận (họ tên, giấy tờ), bao gồm mọi đồ đang trả; trong lệnh xác nhận chuyển `DA_DUNG`. *(R-15: `xac_nhan_nguoi_nhan` → `ho_so_chung`)*
  Điều dưỡng: mọi đồ không có giá trị và trường hợp 1 hoặc 2; có lẫn đồ có giá trị → chặn toàn bộ (FR-016a).
- `xac_nhan_nguoi_nhan` (nhóm 2) `CHO_XAC_NHAN | HIEU_LUC | TU_CHOI | HUY | DA_DUNG | HET_HAN`; nội dung khóa từ khi lập (trigger); `het_han_luc` = lúc Hiệu lực + CFG-M10-08. Duyệt thay (FR-017a): kiểm (a) chờ quá CFG-M12-05 và `bang_chung_lien_he` đạt CFG-M12-06 (mỗi người đại diện Hiệu lực ≥ số lần, ≥ số kênh), hoặc (b) không có người đại diện Hiệu lực kèm căn cứ; Quản lý viện ≠ người lập; đã có Từ chối → không duyệt thay. Đồ rời trạng thái hợp lệ → loại khỏi danh sách; hết đồ → `HUY` "không còn đồ". *(R-15: `xac_nhan_nguoi_nhan` → `ho_so_chung`)*
- `dong_y_tu_giu` (nhóm 2) `HIEU_LUC | DA_RUT | HET_HIEU_LUC`; cột sinh `khoa_hieu_luc = IF(trang_thai = 'HIEU_LUC', do_gui_id, NULL)` `UNIQUE`. Hết hiệu lực trong cùng lệnh khi đồ chuyển Đã trả, Thất lạc, Hư hỏng, Đã xử lý. Rút khi đồ Đang sử dụng → thông báo thu lại; không đổi trạng thái. Bật dấu "cần đồng ý" cho loại đồ → liệt kê đồ loại đó Đang sử dụng không có đồng ý và thông báo như rút. *(R-15: `dong_y_tu_giu` → `do_gui` (cột))*
- Người đại diện thao tác qua REST của 013 dưới `/cong/…` (kiểm `QuyenNguoiThanApi.laNguoiDaiDien` tại thời điểm), hoặc Hành chính ghi bản ký scan.

---

## K-07. Thất lạc, hư hỏng và sự cố (mục F; BR-M12-02, Q-307)

**Quyết định**:
- Lệnh Báo thất lạc, Ghi hư hỏng có trường `suCo`: `{ cach: MOI | LIEN_KET | KHONG, ... }`.
  - `MOI`: gọi `SuCoLenhApi.ghiNhan(GhiNhanSuCo(loai = DO_GUI_THAT_LAC | DO_GUI_HU_HONG, nguon = DO_GUI, muc do người ghi chọn, thamChieu = doGuiId + banGhiId), nd)` trong cùng transaction (MANDATORY); lỗi → rollback cả lệnh (FR-021).
  - `LIEN_KET`: `SuCoApi.xem(suCoId)` tồn tại, cùng người cao tuổi, chưa Đã hủy; lưu `su_co_id` trên bản ghi.
  - Mỗi lần chuyển vào Thất lạc/Hư hỏng tối đa một sự cố (FR-024); tách nhiều đồ → liên kết riêng từng đồ.
- Liên kết sau (`POST /ban-giao-do-gui/{id}/lien-ket-su-co`): cột `su_co_id` đặt một lần trên bản ghi nhóm 3.
- Tìm thấy có sự cố liên kết → `SuCoLenhApi.themDienBien(suCoId, DA_TIM_THAY, …)` (MANDATORY); không đóng sự cố.
- Không đánh giá lại sau sự cố (FR-022): loại sự cố đồ gửi đã bị 007 loại khỏi FR-043 (b) (`canDanhGiaLai = false`).

---

## K-08. Kết thúc lưu trú, qua đời (mục G; BR-M12-03, Q-148, Q-152)

**Quyết định**:
- Hàm thuần `DieuKienDoGui(cacDo, suCoMo)` → `DAT` / `CHUA_DAT` kèm danh sách đồ Đang giữ, Đang sử dụng, Hư hỏng, và danh sách riêng đồ Thất lạc có sự cố mở ghi "chặn qua điều kiện (d)". Bean `ThamGiaChuyenTrangThai` trả `DieuKien("KT_B_DO_GUI", …, coTheMien = true)` khi `CHUA_DAT`. Tính lúc hỏi (không lưu), nên luôn cập nhật sau lệnh (SC-006 ≤ 1 phút).
- `MucViecSauQuaDoiPort` (`DO_GUI`): `KHONG_AP_DUNG` khi tại thời điểm qua đời không có đồ chưa trả (tính từ bản ghi bàn giao ≤ thời điểm qua đời); `HOAN_THANH` khi mọi đồ ở Đã trả, Thất lạc, Đã xử lý; mỗi lệnh đổi trạng thái đồ của người đã qua đời gọi `DanhSachViecSauQuaDoiApi.capNhat`. Tìm thấy sau khi mục Hoàn thành hoặc hồ sơ Đã đóng: không mở lại (FR-026a); đồ vào nhắc CFG-M12-02, CFG-M12-03 tính từ lúc Tìm thấy (`moc_tinh_xu_ly` trên đồ).
- Sau trạng thái cuối: Tiếp nhận, Giao sử dụng bị chặn (`NguoiCaoTuoiApi.trangThai`); lệnh khác cho phép (FR-027). Listener `HoSoKetThucDaLap` (004): còn đồ chưa trả → báo người đại diện Nhẹ.

---

## K-09. Đề nghị xử lý đồ không người nhận (FR-027a, Q-152, Q-157, Q-159)

**Quyết định**: Loại yêu cầu phê duyệt `M12.XU_LY_DO_KHONG_NGUOI_NHAN` của 000 (Nháp → Chờ duyệt → Đã duyệt → Đã áp dụng / Áp dụng không thành; Từ chối; Hủy). Handler:
- `kiemTraNoiDung`: danh sách đồ Đang giữ/Hư hỏng của cùng người cao tuổi; hình thức (`THANH_LY`, `TIEU_HUY`, `CHUYEN_CO_QUAN`); tiền mặt chỉ `CHUYEN_CO_QUAN` (Q-159); đồ có giá trị có ảnh; biên bản scan; bằng chứng liên hệ đạt CFG-M12-07 với mọi người đại diện và liên hệ chính còn quan hệ (hàm `DuBangChungLienHe`).
- `quyenTao` Hành chính; `quyenDuyet` Quản lý viện; `choTuDuyet() = false`; tạo bị chặn khi trạng thái cuối chưa đủ CFG-M12-03 (tính từ thời điểm trạng thái cuối hoặc `moc_tinh_xu_ly`).
- `kiemTraDieuKienApDung`: đồ còn Đang giữ/Hư hỏng; đồ đổi trạng thái bị loại khỏi phần áp dụng (ghi trong kết quả), không làm thất bại cả đề nghị.
- `apDung` (trong transaction): mỗi đồ một bản ghi `XU_LY` (người giữ → hình thức, đơn vị tiếp nhận), đồ `DA_XU_LY`, đồng ý tự giữ hết hiệu lực, `capNhat` danh sách việc sau qua đời.

---

## K-10. Kiểm kê (mục H2; BR-M12-07, Q-154, Q-306)

**Quyết định**:
- Kỳ kiểm kê tính từ CFG-M12-04 (chu kỳ, hạn) thành `ma_ky` (ví dụ `2026-10`), `han`.
- `phieu_kiem_ke` (nhóm 2 → 3 khi Hoàn thành) `UNIQUE (vi_tri_id, ma_ky)`; lệnh Lập chụp mọi đồ có giá trị Đang giữ tại vị trí (`phieu_kiem_ke_dong`: đồ, số lượng, tình trạng, ảnh lúc lập); vị trí không có đồ → 409 `KHONG_CO_DO_CO_GIA_TRI`. Không có tham số chọn đồ (không bỏ bớt được). *(R-15: `phieu_kiem_ke` → `ho_so_chung`)*
- Hoàn thành: thân lệnh có kết quả mỗi dòng (`DUNG`, `LECH_TINH_TRANG` kèm mức mới hoặc `GHI_HU_HONG`, `KHONG_TIM_THAY`), đồ thừa (`TIM_THAY` cho đồ Thất lạc; đính chính số lượng; `do_khong_ro_chu` kèm ảnh). Trong một transaction: dòng có đồ đã đổi trạng thái/vị trí sau khi lập → loại kèm ghi chú; `KHONG_TIM_THAY` → lệnh Báo thất lạc; `GHI_HU_HONG` → lệnh Ghi hư hỏng; `DUNG` không sinh bản ghi bàn giao. Đồ không rõ chủ → báo Quản lý viện. *(R-15: `do_khong_ro_chu` → `ho_so_chung`)*
- Job hằng ngày: đầu kỳ nhắc Hành chính số vị trí chưa có phiếu; sau hạn: nhắc mỗi ngày (khóa vị trí/phiếu + kỳ + ngày), báo Quản lý viện một lần; phiếu lập sau hạn hoặc hoàn thành sau hạn mang `tre_han`. Job không tạo phiếu.

---

## K-11. Hết ca, chuyển tầng (FR-014a, FR-028)

**Quyết định**:
- Listener `CaDaKetThuc` (008): nhân viên của ca còn là người giữ đồ có giá trị Đang giữ → nhắc nhân viên và Người phụ trách ca (Trung bình). Bản cài `DoGuiBanGiaoPort.dangGiu` trả danh sách cho màn bàn giao của 008; 013 không ghi mục vào bàn giao.
- Listener `DaChuyenGiuong` (003) đổi tầng: đồ Đang giữ tại vị trí gắn tầng cũ → thông báo Hành chính và điều dưỡng phụ trách tầng mới; không đổi đồ.

---

## K-12. Cổng người thân (FR-030, Q-151)

**Quyết định**: REST của 013 dưới `/cong/nguoi-cao-tuoi/{nctId}/do-gui` (danh sách, lịch sử, ảnh), `/cong/xac-nhan-nguoi-nhan/{id}/{lenh}`, `/cong/dong-y-tu-giu`. Backend kiểm `QuyenNguoiThanApi.xemDoGui(nguoiThanId, nctId, now)` mỗi lần gọi; lệnh của người đại diện kiểm `laNguoiDaiDien`. DTO cổng không có vị trí lưu giữ, người giữ là nhân viên chỉ hiện vai trò. Thông báo tới người thân chỉ loại đồ và sự kiện (SC-012).

---

## K-13. Thông báo (FR-032)

**Đối tượng thông tin** (bổ sung 2026-10-18; FR-032, 009 FR-001a): mọi thông báo tới người thân, kể cả đồ thất lạc, hư hỏng, được tìm thấy và mọi thông báo sau qua đời, khai `doiTuongThongTin = VE_VIEC_NGUOI_THAN` (Q-362).

**Quyết định**: Mức theo bảng FR-032 (đủ mức; dòng nhắc đề nghị Chờ duyệt dùng mức của 000 FR-031a qua `NhacQuaHan`). Khóa `(loại sự kiện, đồ gửi)` hoặc `(loại sự kiện, yêu cầu/đề nghị/phiếu)`, nhắc lặp thêm ngày. Sau khi người cao tuổi qua đời: nhóm người nhận người thân thay bằng `NhomNhan.NguoiDaiDien` và cờ "thuộc danh sách việc sau qua đời" (009 FR-015). Nội dung rút gọn: loại sự kiện, loại đồ.

---

## K-14. Quyền (FR-029)

**Quyết định**:

| `ChucNang` | Thao tác |
| --- | --- |
| `DO_GUI` | `THUC_HIEN` (Hành chính; Điều dưỡng trong phạm vi); `TRA_DO_CO_GIA_TRI` (Hành chính); `XEM` (Quản lý viện; Trưởng tầng trong phạm vi tầng) |
| `XAC_NHAN_NGUOI_NHAN` | `LAP` (Hành chính); `XAC_NHAN` (người đại diện, chú thích ²); `DUYET_THAY` (Quản lý viện) |
| `DONG_Y_TU_GIU` | `GHI` (người đại diện; Hành chính ghi bản ký; Hành chính, Điều dưỡng trong lệnh tiếp nhận) |
| `KIEM_KE_DO_GUI` | `THUC_HIEN` (Hành chính) |
| `DANH_MUC_DO_GUI` | `CAU_HINH` (Quản lý viện: loại đồ, mức tình trạng); vị trí: Hành chính, Quản lý viện |

Bác sĩ, Nhân viên chăm sóc, Nhân viên bếp, Kế toán: không có quyền. Mọi lệnh chỉ trực tuyến (FR-031): không đăng ký `NgoaiTuyenHandler`.

---

## K-15. Bộ lập lịch, lưu giữ, nhật ký, kiểm thử

**Quyết định**:
- **Job**: `ViecTheoPhutM12` (xác nhận người nhận khác hết hạn; yêu cầu đủ CFG-M12-05 kèm bằng chứng → báo Quản lý viện một lần); `ViecHangNgayM12` (nhắc CFG-M12-02 sau trạng thái cuối; đồ đủ CFG-M12-03 báo Hành chính; kiểm kê đầu kỳ, sau hạn). Cột "đã làm lúc" chống trùng (NFR-04); chạy bù.
- **Lưu giữ**: đồ gửi, bản ghi bàn giao, xác nhận, đồng ý, lần trả: `THEO_HO_SO` (CFG-M15-03); phiếu kiểm kê, vị trí: `KHONG_GAN_HO_SO`.
- **Nhật ký** (FR-031a): mọi lệnh, xác nhận, đính chính; nhóm "đặc biệt quan trọng" của 19.4; bản ghi bàn giao thuộc danh sách bảo vệ BR-M15-03 (000 `PhanLoaiTruongProvider`).
- **Kiểm thử**:
  - test bảng `KiemTraLenhBanGiao`, `TachDo`, `NguoiNhanHopLe`, `DieuKienDoGui`, `DuBangChungLienHe`, mục `DO_GUI`;
  - SC-001: bất biến tách và trạng thái = bản ghi gần nhất sau mọi kịch bản, kể cả đính chính;
  - Test tích hợp MySQL: trigger nhóm 3, `UNIQUE` phiếu theo (vị trí, kỳ), một đồng ý Hiệu lực mỗi đồ;
  - test đồng thời: hai nhân viên bàn giao cùng đồ; Trả trong lúc Kết thúc lưu trú;
  - đồng hồ giả lập cho CFG-M10-08, CFG-M12-02 → 07;
  - bản giả 007 `SuCoLenhApi`, 012 `QuyenNguoiThanApi`.
- Tên test theo BR/FR (`BR_M12_04_TraDoIT`, `DBR_22_TiepNhanGocIT`).

---

## K-16. Đồ gửi trên kênh người cao tuổi *(bổ sung 2026-10-18; FR-030a, Q-366)*

**Quyết định**: Bean `DoGuiKenhNctPortImpl` đọc đồ của người cao tuổi ở trạng thái Đang giữ, Đang được sử dụng; trả tên đồ, trạng thái, mã ảnh. `QuyenTepProvider` `M12.ANH_DO_GUI` cho tài khoản AC-16 xem ảnh đồ của chính mình. Không có thao tác nào; trả đồ, xác nhận người nhận giữ nguyên (BR-M12-04, BR-M12-06).

**Lý do**: SPI ở `family.api` theo cách `HoatDongBanTinPort`, không tạo vòng M10 ↔ M12.

