# Research: Tài sản, lịch xe và mua sắm của viện (019)

> **Sửa 2026-10-29 (Q-392):** hệ thống không quản lý kho nguyên liệu. Các mục về kho (nguyên liệu, lô, phiếu nhập, xuất, đảo, kiểm kê, cảnh báo hạn dùng, tồn tối thiểu, `nutrition.kho`) trong tài liệu này không còn hiệu lực; xem spec 019 mục Cập nhật 2026-10-29.

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

Mỗi mục gồm Quyết định, Lý do, Phương án đã cân nhắc. Quyết định nền R-01 → R-14 của 000 và các mẫu đã chốt ở 001 → 018 (G-01 ranh giới 003 ↔ 019, M-12 port kho của 011, P-12 port M11, N-02 SPI ở `administration.taikhoan.api`, B-02 khuôn thông báo) được dùng lại, không quyết lại. Các quyết định dưới đây là kỹ thuật; chúng không đổi nghiệp vụ (constitution IX). Chỗ spec chưa rõ hoặc mâu thuẫn được ghi ở plan, mục "Điểm cần báo lại".

---

## W-01. Vị trí trong backend: ba module

**Quyết định**: Feature 019 trải trên ba module theo mục của tài liệu nguồn (constitution X chia gói theo M01–M15):

| Phần | Module, gói | Mục nguồn | Mục spec |
| --- | --- | --- | --- |
| Kho nguyên liệu, kiểm kê | M08, `nutrition.kho` | 12.7, BR-M08-17 → 20 | A (FR-001 → FR-007, FR-009, FR-010) |
| Yêu cầu mua sắm, đối chiếu | M11, `billing.muasam` | 15.10, BR-M11-17 → 20 | A (FR-008 → FR-008c) |
| Tài sản, bảo trì, báo hỏng, giường là tài sản | M03, `roombedallocation.taisan` | 7.7, BR-M03-15, 17, 18 | B |
| Lịch xe | M03, `roombedallocation.lichxe` | 7.7, BR-M03-16 | C |

**Lý do**: 003 G-01, 011 M-01, 010 P-01 đã dành chỗ cho 019 trong ba module này và khai port nội bộ tương ứng.

**Phương án loại**: một module riêng cho 019 — ngoài M01–M15 và tạo vòng với 003 (G-01).

---

## W-02. Chiều phụ thuộc giữa module

**Quyết định**:

| Cặp | Cách nối |
| --- | --- |
| `roombedallocation.taisan` ↔ `roombedallocation.giuong` (003) | Port nội bộ M03: dùng `GiuongTheoTaiSanPort`; cài `TaiSanGiuongPort` (003 hợp đồng mục 3) |
| `nutrition.kho` ↔ `nutrition` (011) | Cài `TonKhoNguyenLieuPort`; đọc `LichBuaApi` (bữa ngày) để chọn mục đích xuất |
| M03, M08 → bộ phận (008) | `NhiemVuApi.boPhanCua`, `laPhuTrachBoPhan`, `phuTrachBoPhan(boPhanId)` qua `administration.taikhoan.api` (008 cài; 008 N-02) — không phụ thuộc M09 trực tiếp |
| M08 kho → M11 mua sắm | `billing.api.YeuCauMuaSamApi` (M08 đã phụ thuộc M11 từ 011): `kiemTraDuocDung`, `ganNguon` (MANDATORY) |
| M03 → M11 mua sắm | M11 → M02 → M03 nên M03 không gọi M11 trực tiếp. SPI `YeuCauMuaSamPort` khai ở `roombedallocation.api`, M11 cài (cùng chữ ký `kiemTraDuocDung`, `ganNguon`) |
| M11 mua sắm → M07 (006) *(bổ sung 2026-10-11, Q-337)* | M11 đọc `medication.api.DonThuocApi.donNguonVienChuaKetThuc` (gắn dòng loại "thuốc"), `TiepNhanThuocCungUngApi.theoYeuCau` (hiển thị "Đã nhận"); M11 cài SPI `medication.api.YeuCauThuocCungUngPort` cho 006 đọc dòng đã ghi đã mua. Cùng chiều với `DeNghiMuaHoPort` mà 010 (M11) đã cài; M07 không gọi `billing.api`, nên không có vòng |
| M04 (014) → M03 lịch xe | 019 cung cấp `roombedallocation.api.LichXeApi` (đọc) và `LichXeLenhApi` (MANDATORY); 014 gọi trong lệnh của chuyến. Sửa hợp đồng 014 (trước đó 014 khai `LichXeChuyenPort` ở `care.api` cho 019 cài, sẽ tạo vòng M03 ↔ M04) |
| M08 → M01 | `DiNguyenApi` (thành phần gây dị ứng của nguyên liệu) |
| M03 → M15 | `HoSoNhanVienApi` (người lái, có thể không có tài khoản), `TaiKhoanApi.theoNhanVien` (báo người lái có tài khoản) |
| 019 → 009, 016 | Thông báo cài `CoYeuCauThongBao`; 016 đọc `TaiSanThongKeApi` (`roombedallocation.api`), `KhoThongKeApi` (`nutrition.api`), `MuaSamThongKeApi` (`billing.api`) |

**Lý do**: Modulith `verify()` từ chối vòng (000 R-02).

---

## W-03. Khóa và đồng thời

**Quyết định**:
- **Kho** (FR-004, DBR-31): mỗi phiếu khóa các dòng `lo_nguyen_lieu` liên quan theo `id` tăng `FOR UPDATE`, tính tồn mới, chặn âm, ghi phiếu và cập nhật tồn trong một transaction; hai phiếu trên cùng lô cho kết quả như tuần tự. Lô mới tạo từ phiếu nhập (`UNIQUE (nguyen_lieu_id, so_lo, han_dung)`).
- **Kiểm kê**: một phiếu Chờ duyệt mỗi kỳ (cột sinh `UNIQUE`); lệnh duyệt khóa lô, kiểm tồn sau điều chỉnh không âm.
- **Tài sản giường** (FR-015, SC-004): lệnh tài sản loại giường khóa dòng `giuong` (003) trước, rồi `tai_san`, theo thứ tự khóa của 003 G-03; gọi `GiuongTheoTaiSanPort` trong cùng transaction.
- **Lịch xe** (FR-017, SC-005): khóa dòng `tai_san` của xe, kiểm chồng giờ với lịch chưa hủy (khoảng nửa mở), trừ chồng do gia hạn của lịch Đang dùng.
- **Yêu cầu mua sắm**: khóa dòng yêu cầu khi gắn nguồn, ghi đã mua, đối chiếu; tổng gắn và số tiền kiểm dưới khóa.

---

## W-04. Kho nguyên liệu (mục A; BR-M08-17 → 20, DBR-31, Q-220, Q-233)

**Quyết định**:
- `nguyen_lieu` (nhóm 1; thành phần dị ứng là danh sách `di_nguyen_id`), `lo_nguyen_lieu` (nhóm 2 dẫn xuất: `ton`), `phieu_kho` (nhóm 3: `NHAP`, `XUAT`, `DAO`, `DIEU_CHINH`), `phieu_kho_dong`.
- Phiếu nhập hàng mua: `YeuCauMuaSamApi.kiemTraDuocDung(yeuCauId, boPhanBep, NGUYEN_LIEU)` (Đã duyệt, cùng bộ phận) rồi `ganNguon(yeuCauId, PHIEU_NHAP, phieuId, tongTien)` trong cùng transaction (FR-008c); hàng tặng/chuyển ghi nguồn. Dòng Không đạt không vào tồn.
- Phiếu xuất: hàm thuần `ThuTuLoTheoHan(nguyenLieu, lo[], ngay)` trả các lô còn tồn theo thứ tự hạn dùng để hiển thị (không chọn sẵn, Q-307); dòng xuất từ lô không phải lô hạn sớm nhất còn tồn cần lý do (lưu trên dòng; FR-003). Lô quá hạn chỉ xuất mục đích "hủy" (FR-005). Mục đích "cho bữa" chọn bữa ngày từ `LichBuaApi`.
- Phiếu đảo: trỏ đúng một phiếu gốc (toàn bộ hoặc một số dòng), người lập có quyền lập loại phiếu gốc.
- Không tự trừ kho theo thực đơn, suất (FR-007).
- **Kiểm kê** (FR-010): `phieu_kiem_ke_kho` `CHO_DUYET | DA_DUYET | BI_TRA_LAI | DA_HUY`; người duyệt = Phụ trách bộ phận Bếp, hoặc Quản lý viện khi người lập là Phụ trách bộ phận Bếp; người duyệt ≠ người lập. Duyệt tạo phiếu `DIEU_CHINH` cho dòng lệch.
- **Job hằng ngày** 05:00: lô còn tồn có hạn trong CFG-M08-08 hoặc quá hạn; nguyên liệu dưới tồn tối thiểu kèm nhắc lập yêu cầu mua sắm (không tạo yêu cầu, Q-306); tới kỳ CFG-M08-09 chưa có phiếu kiểm kê → nhắc.
- Bản cài `TonKhoNguyenLieuPort.tonHienHanh()` cho 011 FR-025a.
- Bộ phận Bếp chưa có Phụ trách bộ phận → lệnh của Phụ trách bị chặn, Quản lý viện được nhắc (Q-292).

---

## W-05. Yêu cầu mua sắm (FR-008 → FR-008c; BR-M11-17 → 20, DBR-36, CFG-M11-07 → 09, Q-284, Q-290, Q-292, Q-293, Q-294)

**Quyết định**:
- `yeu_cau_mua_sam` (nhóm 2 → 3 ở trạng thái cuối): bộ phận, người lập, loại (`NGUYEN_LIEU`, `TAI_SAN`, `DICH_VU`, `KHAC`), dòng, tổng dự kiến, số được duyệt, lý do giảm, ngày cần, tham chiếu, yêu cầu gốc (bổ sung), số tiền thực tế, ngày mua, nhà cung cấp, chứng từ, kết quả đối chiếu, số lệch, cách xử lý; trạng thái `NHAP | CHO_DUYET | DA_DUYET | CHO_DOI_CHIEU | LECH | DA_DOI_CHIEU | TU_CHOI | DA_HUY`. Vòng đời riêng (bảng FR-008), không dùng `yeu_cau_phe_duyet` của 000 vì có bước sau duyệt (mua, đối chiếu); nhật ký, lý do, khóa theo 000.
- Người lập phải là Phụ trách bộ phận của bộ phận đó tại thời điểm lệnh (`NhiemVuApi.laPhuTrachBoPhan`); Kế toán, Quản lý viện không được (Q-293, Q-299). **(Bổ sung 2026-10-11, FR-008d, Q-337)** Ngoại lệ duy nhất: loại `THUOC` thì vai trò Điều dưỡng cũng lập, gửi, rút lại, hủy được dù không là Phụ trách bộ phận; bộ phận của yêu cầu là bộ phận của người lập (`NhiemVuApi.boPhanCua`). Điều dưỡng lập loại khác bị 403. "Ghi đã mua" mọi loại vẫn chỉ Phụ trách bộ phận.
- **Loại `THUOC`** *(bổ sung 2026-10-11, FR-008d, Q-337)*: yêu cầu có thêm `muc_uu_tien` (`THUONG`, `GAP`), `thoi_diem_can`, tài liệu liên quan; mỗi dòng có thêm người cao tuổi, đơn thuốc, hoạt chất, hàm lượng, dạng, đơn vị. Khi lập và sửa Nháp, mỗi dòng phải gắn một đơn trong `DonThuocApi.donNguonVienChuaKetThuc(nctId)` (nguồn viện, Hiệu lực, Tạm dừng, Chờ hiệu lực), nếu không thì 409. Điều dưỡng lập: người cao tuổi của mỗi dòng phải trong phạm vi phân công của người lập (19.3: quyền T của Điều dưỡng trong phạm vi phân công; BR-M15-01; 002 A-04), quyết định của chủ dự án 2026-10-11; không phải quy tắc mới. Phụ trách bộ phận (³⁷, mọi vai trò) giữ quyền lập loại `THUOC`, không có quy tắc cấm riêng theo loại; khi lập, sửa Nháp, mỗi dòng gắn đơn chỉ được nếu người lập xem được thông tin thuốc của người cao tuổi đó (`QuyenXemTruongApi.duocXem(nd, SUC_KHOE, doiTuong) = DAY_DU`, không thì 403) và đơn thuộc `donNguonVienChuaKetThuc(nctId)` (không thì 409). 019 không cấp thêm quyền xem `SUC_KHOE`; Phụ trách bộ phận không xem được thuốc thì không gắn được dòng. Hệ thống không tự lập yêu cầu (Q-307). Không có phiếu nhập, tồn kho hay `mua_sam_gan_nguon` cho thuốc; lần tiếp nhận do 006 ghi (006 FR-054). Yêu cầu loại này không sinh chi phí cho người cao tuổi. *(R-15: `mua_sam_gan_nguon` → cột `JSON` của yêu cầu mua sắm)*
- `mua_sam_gan_nguon` (nhóm 3): yêu cầu, loại nguồn (`PHIEU_NHAP`, `TAI_SAN`, `BAO_TRI`), id, số tiền (tổng phiếu nhập / nguyên giá / chi phí bảo trì). `ganNguon` chỉ khi yêu cầu `DA_DUYET` và cùng bộ phận, loại phù hợp. Hủy yêu cầu Đã duyệt bị chặn khi đã có nguồn gắn (Q-290). *(R-15: `mua_sam_gan_nguon` → cột `JSON` của yêu cầu mua sắm)*
- Ghi đã mua (FR-008a): chứng từ bắt buộc; hàm `KiemTraVuot(soDuocDuyet, thucTe, cfg07)`; vượt → 409 kèm hướng dẫn lập yêu cầu bổ sung (`yeu_cau_goc_id`).
- Đối chiếu (FR-008b): hàm `KhopChungTu(chungTu, thucTe, tongNguon, loai)`; người ghi đã mua không đối chiếu (Phụ lục 27 ³⁸). Với `THUOC` (và `DICH_VU`, `KHAC`) không có tổng nguồn, chỉ so chứng từ với số tiền thực tế (FR-008b chỉ đòi tổng nguồn với nguyên liệu, tài sản).
- Bản cài `YeuCauThuocCungUngPort` (`medication.api`, bổ sung 2026-10-11): trả dòng của yêu cầu `THUOC` ở `CHO_DOI_CHIEU`, `DA_DOI_CHIEU`, `LECH` (BR-M11-17), kèm id nhân viên của người lập và `bo_phan_id` của yêu cầu (gắn lúc lập, không đổi khi người lập chuyển bộ phận; 006 dùng để báo Phụ trách bộ phận hiện tại, 006 FR-055, sửa 2026-10-11 lượt 3); trạng thái khác trả rỗng. Kiểu trả về là `DongYeuCauThuocView` ở 006 contracts mục 2.1 (không có id thuốc; 006 lấy từ đơn).
- Job hằng ngày: Đã duyệt quá CFG-M11-08 chưa ghi đã mua → nhắc Phụ trách bộ phận; Chờ đối chiếu quá CFG-M11-09 → nhắc Kế toán (BR-M11-20).
- Bản cài `YeuCauMuaSamPort` (`roombedallocation.api`) ủy cho cùng dịch vụ của `YeuCauMuaSamApi`.

---

## W-06. Tài sản (mục B; BR-M03-15, 17, 18, DBR-32, Q-238, Q-242, Q-243, Q-284, Q-294)

**Quyết định**:
- `loai_tai_san` (nhóm 1, Quản lý viện), `tai_san` (nhóm 1 mô tả + nhóm 2 trạng thái, vị trí, bộ phận): mã `UNIQUE`, loại, tên, bộ phận quản lý, yêu cầu mua sắm, ngày đưa vào sử dụng, nguyên giá, lịch bảo trì/kiểm định (chu kỳ, hạn kế tiếp), với xe: biển số, số chỗ, hạn đăng kiểm, hạn bảo hiểm; `giuong_id` `UNIQUE` (loại giường); trạng thái `SAN_SANG | DANG_SU_DUNG | HONG | DANG_BAO_TRI | DA_THANH_LY`; dấu `qua_han_bao_tri`, `qua_han_dang_kiem`, `qua_han_bao_hiem`, `cho_chuyen_nguoi`. *(R-15: `loai_tai_san` → `danh_muc_chung`)*
- Lịch sử (nhóm 3): `tai_san_vi_tri` (không chồng khoảng: cột sinh `UNIQUE` một vị trí mở), `tai_san_bo_phan`, `tai_san_bao_tri` (lần bảo trì/kiểm định/đăng kiểm/bảo hiểm; thuê ngoài có chi phí gắn yêu cầu mua sắm), `tai_san_hu_hong` (mức ảnh hưởng, dấu chờ chuyển người, gỡ dấu kèm nội dung đã sửa, người sửa). *(R-15: `tai_san_vi_tri`, `tai_san_bo_phan` → `lich_su_chung`; `tai_san_bao_tri`, `tai_san_hu_hong` → `su_kien_chung`)*
- **Ghi tăng** (FR-011): không phải loại giường; mua mới: `YeuCauMuaSamPort.kiemTraDuocDung(…, TAI_SAN)`, nguyên giá bắt buộc, `ganNguon(TAI_SAN, id, nguyenGia)`. Giường: bản cài `TaiSanGiuongPort.ghiTangChoGiuong(giuongId)` trong lệnh Tạo giường của 003 (Sẵn sàng, bộ phận Chăm sóc, vị trí phòng).
- **Lệnh trạng thái** theo bảng FR-013. Với loại giường (FR-015), hàm thuần `AnhXaTrangThaiGiuong(lenh, coNguoi, tamNgung)` → `LenhTaiSanGiuong` (`BAO_TRI`, `SAN_SANG`, `THANH_LY`) hoặc "ghi hư hỏng, gắn dấu chờ chuyển người"; gọi `GiuongTheoTaiSanPort.kiemTraLenhTaiSan` rồi `apLenhTaiSan` (MANDATORY), `daGanDauChoChuyenNguoi` / `daGoDauChoChuyenNguoi`. Kết quả bảo trì đạt với giường Tạm ngừng sử dụng → giường về Tạm ngừng (Q-246, xử lý ở 003).
- **Báo hỏng giường có người** (Q-245, Q-256, Q-265): mức ảnh hưởng bắt buộc; "mất an toàn" → `YeuCauThongBao` Khẩn cấp cho Trưởng tầng và Người phụ trách ca của tầng; nếu `giuongTrongPhuHopChoNguoiDangNam` rỗng ở tầng → thêm Quản lý viện, Hành chính, kèm danh sách tầng khác (hoặc "không còn giường phù hợp trong viện"). Gỡ dấu "đã sửa tại chỗ": Trưởng tầng (tầng mình) hoặc Quản lý viện; mức mất an toàn cần nội dung đã sửa, người sửa.
- **Thanh lý** (FR-013): loại yêu cầu phê duyệt `M03.THANH_LY_TAI_SAN` của 000 (Phụ trách bộ phận lập, Quản lý viện duyệt, không tự duyệt); `apDung` chuyển `DA_THANH_LY`, với giường gọi `apLenhTaiSan(THANH_LY)` (Q-51 của 003 áp cho phân bổ tương lai).
- **Chuyển vị trí giường** (FR-012, Q-243): chặn khi giường đã từng có phân bổ (003).
- **Chuyển bộ phận**: Quản lý viện, lý do; lịch sử.
- **Hư hỏng từ dọn giường** (FR-016): 003 ghi hư hỏng; Trưởng tầng hoặc Phụ trách bộ phận xác nhận gắn tài sản (`tai_san_hu_hong` nguồn `DON_GIUONG`), MAY dẫn tới Báo hỏng. *(R-15: `tai_san_hu_hong` → `su_kien_chung`)*
- **Job hằng ngày**: nhắc trước hạn CFG-M03-09; quá hạn → gắn dấu, nhắc mỗi ngày; giường mang dấu chờ chuyển người còn người → nhắc Trưởng tầng mỗi CFG-M03-02; giường mang dấu đã Trống → nhắc báo hỏng lại.

---

## W-07. Lịch xe (mục C; BR-M03-16, Q-220, Q-231, Q-236, Q-239, Q-240, Q-295, Q-298)

**Quyết định**:
- `lich_xe` (nhóm 2): xe (tài sản loại xe), giờ đi, giờ về dự kiến, mục đích (`CHUYEN_DI`, `DI_KHAM`, `CHUYEN_VIEN_KHONG_CAP_CUU`, `KHAC`), tham chiếu (chuyến, người cao tuổi), người lái (hồ sơ nhân viên), người đặt, trạng thái `DA_DAT | DANG_DUNG | DA_HOAN_THANH | DA_HUY`, xuất phát/trả thực tế, `da_nhac_qua_gio`. *(R-15: `lich_xe` → `ho_so_chung`)*
- Hàm thuần `KiemTraDatLich(xe, lichKhac[], tu, den, laGiaHan)`: chồng giờ với lịch chưa hủy (trừ gia hạn của lịch Đang dùng), xe Hỏng/Đang bảo trì/Đã thanh lý, dấu quá hạn đăng kiểm, bảo hiểm tại giờ đi.
- `LichXeApi` (đọc, `roombedallocation.api`): `coLichDaDat(chuyenId, gioRoi)`, `kiemTraDoi(chuyenId, roiMoi, veMoi)`.
- `LichXeLenhApi.theoChuyen(chuyenId, loai, roiDuKien, veDuKien, thucTe)` (MANDATORY, trong lệnh của 014): `DOI` (dời lịch, kiểm lại; không thỏa → ném lỗi, 014 chặn lệnh dời), `HUY` (Đã hủy, báo người đặt, Phụ trách bộ phận quản lý xe, người lái có tài khoản), `ROI_VIEN_DAU` (Đang dùng), `GIA_HAN` (dời giờ về, không chặn khi chồng; chồng → báo Quản lý viện và người đặt lịch kế tiếp), `HUY_ROI_VIEN_CUOI` (về Đã đặt, bỏ thời điểm xuất phát), `KET_THUC_VE` (Đã hoàn thành).
- Lệnh của người: đặt lịch, dời, hủy, ghi xuất phát, ghi trả xe (người đặt, Quản lý viện). Job theo phút: lịch Đang dùng quá giờ về → nhắc người đặt một lần; chồng lịch kế tiếp → báo Quản lý viện.
- Không có ghi ngoại tuyến cho lịch xe.

---

## W-08. Quyền (FR-020; Phụ lục 27 ³³, ³⁴, ³⁷, ³⁸; Q-283, Q-284, Q-292, Q-293, Q-299)

**Quyết định**:

| `ChucNang` | Thao tác |
| --- | --- |
| `KHO_NGUYEN_LIEU` | `LAP_PHIEU` (Nhân viên bếp: nhập, xuất, đảo, kiểm kê); `DANH_MUC`, `DUYET_KIEM_KE` (Phụ trách bộ phận Bếp, theo nhiệm vụ); `DUYET_KIEM_KE` (Quản lý viện khi người lập là Phụ trách bộ phận Bếp); `XEM` (Quản lý viện, Bác sĩ) |
| `MUA_SAM` | `LAP`, `GHI_DA_MUA` (Phụ trách bộ phận, chỉ bộ phận mình); `LAP` với loại `THUOC` cho Điều dưỡng, phạm vi theo người cao tuổi của dòng (⁴⁴ chỉ mô tả, kiểm ở 019; bổ sung 2026-10-11, Q-337); `DUYET`, `XU_LY_LECH` (Quản lý viện); `DOI_CHIEU` (Kế toán, không phải người ghi đã mua) |
| `TAI_SAN` | `THUC_HIEN` (Phụ trách bộ phận quản lý tài sản); `BAO_HONG_BAO_TRI` (Trưởng tầng, tài sản trong tầng mình); `DUYET_THANH_LY`, `CHUYEN_BO_PHAN`, `DANH_MUC_LOAI` (Quản lý viện); `XEM` (Quản lý viện, Hành chính) |
| `LICH_XE` | `DAT` (Trưởng tầng, Hành chính, Quản lý viện); `GHI_XUAT_PHAT_TRA` (người đặt, Quản lý viện) |

Quyền theo nhiệm vụ Phụ trách bộ phận do 002 FR-019f cấp (nhiệm vụ từ 008). Quản lý viện không làm lệnh kho và không làm thay Phụ trách bộ phận (Q-292). Không có thao tác ngoại tuyến.

---

## W-09. Bộ lập lịch, lưu giữ, nhật ký, kiểm thử

**Quyết định**:
- **Job**: `ViecHangNgayM08Kho` 05:00; `ViecHangNgayM11MuaSam` 07:00; `ViecHangNgayM03TaiSan` 06:00; `ViecTheoPhutM03LichXe`. Cột "đã làm lúc" theo ngày; chạy bù. Không tạo yêu cầu, phiếu, lịch, tài sản (Q-306) — ngoài tài sản giường kèm lệnh Tạo giường.
- **Lưu giữ**: `KHONG_GAN_HO_SO`, ≥ CFG-M15-04 (nhật ký); chứng từ ở kho tệp.
- **Kiểm thử**:
  - test bảng `ThuTuLoTheoHan`, `KiemTraVuot`, `KhopChungTu`, `AnhXaTrangThaiGiuong`, `KiemTraDatLich`;
  - SC-001: bộ 200 phiếu; SC-004: bất biến giường ↔ tài sản sau mọi lệnh (003 + 019); SC-005: bất biến không chồng lịch xe;
  - Test tích hợp MySQL: `UNIQUE` lô, một kiểm kê chờ mỗi kỳ, một vị trí mở, giường ↔ tài sản; trigger nhóm 3;
  - test đồng thời: hai phiếu xuất cùng lô chỗ cuối; hai lịch xe cùng giờ; báo hỏng giường cùng lúc với phân bổ;
  - đồng hồ giả lập cho CFG-M03-02, 09, CFG-M08-08, 09, CFG-M11-07 → 09;
  - bản giả `NhiemVuApi` (bộ phận), 014 gọi `LichXeLenhApi`.
- Tên test theo BR (`BR_M08_17_TonKhoIT`, `BR_M11_17_MuaSamIT`, `BR_M03_15_GiuongTaiSanIT`, `BR_M03_16_LichXeIT`).
