# Research: Quản lý dinh dưỡng (011)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

Mỗi mục gồm Quyết định, Lý do, Phương án đã cân nhắc. Quyết định nền R-01 → R-14 của 000 và các mẫu đã chốt ở 001 → 010 (H-02 bean trạng thái, C-02 / S-02 chiều phụ thuộc, B-02 khuôn thông báo, P-04 sự kiện tính phí) được dùng lại, không quyết lại. Các quyết định dưới đây là kỹ thuật; chúng không đổi nghiệp vụ (constitution IX). Chỗ spec chưa rõ hoặc mâu thuẫn được ghi ở plan, mục "Điểm cần báo lại".

---

## M-01. Vị trí trong backend

**Quyết định**: Feature 011 thuộc module M08 – Dinh dưỡng (mục 12), gói `com.cambae.backend.m08`. Kho nguyên liệu (12.7, BR-M08-17 → 20) bỏ theo Q-392; không có gói `m08.kho`, port `TonKhoNguyenLieuPort` (M-12 không còn hiệu lực).

| Gói con | Nội dung | Mục spec |
| --- | --- | --- |
| `m08.danhmuc` | Chế độ ăn, món ăn và bản sửa chờ xác nhận, bữa ăn, loại đồ ăn bị cấm | A |
| `m08.chedoan` | Bản gán chế độ ăn | B |
| `m08.doichieu` | Hàm thuần `XungDot`, `ChonMonThayThe`, kiểm tra FR-005 | C |
| `m08.thucdon` | Thực đơn tuần, kiểm tra độ phủ, lặp món, dị ứng, đổi món | D |
| `m08.chotsuat` | Bữa trong ngày, lần chốt, người được tính, suất, phát sinh, suất ăn người thân, món an toàn | E |
| `m08.phieu` | Phiếu bữa ăn, suất đặc biệt, sai lệch, phần bổ sung | F |
| `m08.phucvu` | Xác nhận phục vụ | G |
| `m08.doangiadinh` | Đồ ăn gia đình mang vào | H |
| `m08.xemlai` | Thông báo xem lại chế độ ăn | I |
| `m08.luumau` | Lưu mẫu thức ăn | J |
| `m08.trangthai`, `m08.lich`, `m08.api` | Bean trạng thái; job; giao diện công khai | |

---

## M-02. Chiều phụ thuộc giữa module

**Quyết định**:

| Cặp | Chiều mã | Cách giữ một chiều |
| --- | --- | --- |
| 011 → 000, 002 | 011 dùng | `NhatKyApi`, `ThamSoApi`, `DinhChinhApi`, `QuyenApi`, `PhamViApi`, `QuyenXemTruongApi`, `NhiemVuApi` (`nguoiNhanTaiTang`, `nguoiPhuTrachCa`, `truongTangDuocGiao`), `PhanCongApi`; cài `DinhChinhDuocProvider`, `PhanLoaiTruongProvider`, `LuuTruProvider`, `GanPhamViProvider` |
| 011 → 001 | Cài, đọc, nghe | Cài `ThamGiaChuyenTrangThai`; đọc `MucSucKhoeApi.hieuLuc(DI_UNG, …)`, `DiNguyenApi`, `themDiNguyen`, `daKiemTra(mucId, AN)`, `QuanHeNguoiThanApi.thuocNguoiCaoTuoi` (012 cài); nghe `DiUngDaGhiNhan`, `DiUngKhacDaGhiNhan`, `DiUngDaLoaiTru`, `NguoiCaoTuoiDaChuyenTrangThai` |
| 011 → 003 | Đọc, nghe | `ViTriApi.hienTai`, `taiThoiDiem`; khu bán trú; nghe `DaChuyenGiuong` |
| 011 → 004 | Đọc, nghe | `HopDongDocApi.lichDenTaiNgay`, `LuotVangApi.dangMo`; nghe `LuotVangDaMo/DaGiaHan/DaDong`, `NgayNghiBanTruDaDoi`, `BaoVangBanTruDaGhi`, `PhuLucDaApDung` |
| 011 → 005 | Cài, gọi, nghe | Cài `NguonCongViecPort` (loại `BUA`, kèm `xacNhanPhucVu`); gọi `NguonCongViecApi.nguonBiHuy(BUA, …)` (MANDATORY); đọc `CoMatBanTruApi`; nghe `TrangThaiCoMatDaXacDinh`, `TrangThaiCoMatDaDoi`, `AnKemKeoDai` |
| 011 → 007 | Phát, gọi, nghe | Phát `m05.api.YeuCauTaoCanhBao`, `YeuCauDongCanhBaoNguon`; gọi `SuCoLenhApi.ghiNhan` (MANDATORY) khi khai "đã ăn"; nghe `SutCanPhatHien` |
| 011 → 008 | Đọc | `CaTrucApi.caDangDienRa`, `dangTrongCa`; điều dưỡng phụ trách qua `NhiemVuApi`/`PhanCongApi` |
| 011 → 010 | Phát | `m11.api.SuKienTinhPhiNguon` / `SuKienTinhPhiNguonDaDoi` cho suất ăn người thân |
| 011 ↔ 012, 014 | Họ → 011 | SPI khai báo ở `m08.api`: `LuotOLaiAnPort` (012), `ChuyenDiBuaAnPort` (014); sự kiện khai báo ở `m08.api` họ phát khi thay đổi (`LuotOLaiDaDoi`, `ChuyenDiDaDoi`) |
| 011 → 009, 016 | Sự kiện; đọc | Thông báo cài `CoYeuCauThongBao`; 016 đọc `DinhDuongThongKeApi` |

**Lý do**: Modulith `verify()` từ chối vòng (000 R-02).

---

## M-03. Khóa và đồng thời

**Quyết định**:
- **Thứ tự khóa**: … → `thuoc_nct` (006) → `chi_so_nct` (M06) → `theo_doi_nct` (M05) → `dinh_duong_nct` (M08, một dòng mỗi người) → `bua_ngay` (bữa trong ngày) → `phieu_bua_an` (theo `id` tăng) → dòng con. Bean trạng thái của 011 chạy sau 006 theo `@Order` của 001 (thứ tự 001 H-02).
- **Bản gán** (FR-011, FR-012): khóa `dinh_duong_nct`; cột sinh `khoa_cho = IF(trang_thai IN ('DE_XUAT','CHO_HIEU_LUC'), nct_id, NULL)` `UNIQUE`, `khoa_hieu_luc = IF(trang_thai = 'HIEU_LUC', nct_id, NULL)` `UNIQUE`.
- **Chốt suất** (FR-033): khóa `bua_ngay`; `UNIQUE (bua_ngay_id)` trên `lan_chot`; lệnh thứ hai 409 `DA_CHOT`.
- **Phát sinh**: khóa `bua_ngay` để thứ tự phát sinh xác định; phiếu của tầng chưa có lập cùng lệnh xác nhận phát sinh `+` (`UNIQUE (bua_ngay_id, tang_khu_vuc_id)`, DBR-27).
- **Lệnh trên một đối tượng** (xác nhận món, công bố/trả lại thực đơn, xác nhận đồ ăn, chuyển phiếu, nhận phiếu): khóa dòng, kiểm tra lại; lệnh thứ hai 409 `TRANG_THAI_DA_DOI` (000 FR-040). Sửa nội dung Nháp (thực đơn, món) dùng `If-Match`.

---

## M-04. Danh mục: chế độ ăn, món ăn có xác nhận, bữa, loại cấm (mục A, FR-001 → FR-007, Q-308)

**Quyết định**:
- `che_do_an` (nhóm 1): `lien_quan_dieu_tri`, `mac_dinh` (cột sinh `khoa_mac_dinh` `UNIQUE`: đúng một mặc định), danh sách hạn chế (dị nguyên), `mon_an_toan_id` (bắt buộc với chế độ ăn mới; kiểm FR-005). Ngừng hiệu lực bị chặn khi còn bản gán Hiệu lực/Chờ hiệu lực hoặc món trong thực đơn chưa hết tuần (FR-002). Sửa hạn chế: chạy lại FR-005 với thực đơn chưa hết tuần, báo Bác sĩ và Bếp; không tự đổi thực đơn.
- `mon_an` (nhóm 1 có xác nhận): trạng thái `NHAP | CHO_XAC_NHAN | HIEU_LUC | NGUNG_HIEU_LUC | DA_HUY` theo bảng của spec; nội dung đã xác nhận ở `mon_an_phien_ban` (nhóm 3: thành phần, chế độ ăn phù hợp, kết cấu, xác nhận bởi, lúc). `mon_an.phien_ban_hieu_luc_id` trỏ phiên bản dùng cho kiểm tra, chốt, phục vụ; bản sửa chờ là một `mon_an_phien_ban` chưa xác nhận (`xac_nhan_luc NULL`), tối đa một (cột sinh `UNIQUE`). Xác nhận bản sửa: đặt `phien_ban_hieu_luc_id` mới, chạy lại kiểm tra thực đơn chưa hết tuần (FR-004) và FR-005 với chế độ ăn có món này làm món an toàn. Sửa tên, ghi chú không cần xác nhận.
- **Chọn nội dung theo thời điểm**: kiểm tra, chốt, phục vụ đọc phiên bản `HIEU_LUC` tại lúc đó (`phien_ban_hieu_luc_id`); lần chốt lưu id phiên bản món đã dùng.
- `bua_an` (nhóm 1): tên, loại lưu trú phục vụ, thứ tự; giờ dự kiến và ngưỡng giao trễ ở CFG-M08-04 (kiểu `BANG` theo mã bữa). `loai_do_an_cam` (nhóm 1).
- Thành phần thực phẩm mới: Bác sĩ thêm qua `DiNguyenApi.themDiNguyen` (nhóm "thực phẩm", 001 FR-018a); 011 không giữ danh mục riêng.

---

## M-05. Bản gán chế độ ăn (mục B, FR-010 → FR-017, BR-M08-03, Q-283, Q-145)

**Quyết định**:
- `ban_gan_che_do_an` (nhóm 2): trạng thái `DE_XUAT | CHO_HIEU_LUC | HIEU_LUC | NGUNG | DA_HUY`; nội dung không đổi sau Đề xuất (trigger).
- Hàm thuần `CanLyDoChuyenMon(banGanHienCo, banGanMoi)` theo FR-013 (a)–(d) với thứ tự kết cấu cố định `XAY_NHUYEN < MEM < THUONG`.
- **Áp dụng**: kiểm `CanLyDoChuyenMon`, xác nhận các cảnh báo của FR-016 (đối chiếu với thực đơn tuần hiện tại và đã công bố; chế độ ăn không có món ở bữa nào cũng cảnh báo). Thời điểm hiệu lực thực tế theo FR-017; ≤ bây giờ → `HIEU_LUC` và bản cũ `NGUNG` trong cùng lệnh, tạo phát sinh cho bữa đã chốt chưa tới giờ (M-08); tương lai → `CHO_HIEU_LUC`, job theo phút chuyển. Tác động phụ (đồ ăn gia đình, FR-056; tính lại suất đặc biệt) chạy trong cùng lệnh hoặc job.
- **Chế độ ăn mặc định** (FR-014): người Đang lưu trú không có bản gán Hiệu lực → chế độ mặc định, kết cấu thường; suất mang dấu `CHUA_GAN_CHE_DO_AN`; thông báo Bác sĩ khi Hoàn tất tiếp nhận (bean trạng thái) và ở mỗi lần chốt còn thiếu.
- **Trạng thái cuối** (FR-015): bean `ThamGiaChuyenTrangThai` cho `KET_THUC_LUU_TRU`, `GHI_NHAN_QUA_DOI`, `HUY_TIEP_NHAN`: bản Hiệu lực → `NGUNG`, Đề xuất/Chờ hiệu lực → `DA_HUY`; tạo phát sinh "−1" cho bữa đã chốt chưa tới giờ; không gửi thông báo.

---

## M-06. Đối chiếu xung đột (mục C, FR-020 → FR-022, BR-M08-02, BR-M01-07)

**Quyết định**:
- Hàm thuần `XungDot(thanhPhanMon, diUngHieuLuc, hanCheRieng, hanCheCheDo)`: trùng theo id mục danh mục dị nguyên; mục dị ứng loại "khác" (`khongKiemTraTuDong`) không so khớp. Dùng ở mọi điểm của FR-020.
- `ChonMonThayThe(danhSachThayThe, nguoi)` lấy món đầu tiên không xung đột; không có → dấu `THIEU_MON_THAY_THE` (FR-036).
- **Dị ứng mới** (FR-021): listener `DiUngDaGhiNhan` (idempotent theo `mucId`): đối chiếu thực đơn chưa hết tuần của chế độ người đó dùng; bữa xung đột không có món thay thế phù hợp → `YeuCauTaoCanhBao(XUNG_DOT_DI_UNG_CHE_DO_AN, TRUNG_BINH, loaiNguon = THUC_DON_MON, nguonId = thuc_don_mon.id, khoaDoiTuong = mục dị ứng)` (007 gộp theo BR-M05-02) và thông báo Bác sĩ; bữa đã chốt chưa phục vụ → tính lại suất đặc biệt, tạo phát sinh; đồ ăn gia đình Được sử dụng xung đột → Không sử dụng (FR-056); cuối cùng `MucSucKhoeApi.daKiemTra(mucId, AN)` (001 BR-M01-12). `DiUngDaLoaiTru` → tính lại từ lần chốt kế tiếp, `YeuCauDongCanhBaoNguon` cho các nguồn của mục đó. `DiUngKhacDaGhiNhan` → người đó vào danh sách cần đối chiếu khi phục vụ (FR-036a, tính lúc chốt).
- Kết quả hiển thị cho bếp chỉ qua trường của FR-046 (M-09); cảnh báo dị ứng ở thực đơn cho bếp chỉ có số người (FR-025 c).

---

## M-07. Thực đơn tuần (mục D, FR-023 → FR-032, BR-M08-06 → 08, Q-308)

**Quyết định**:
- `thuc_don` (nhóm 2): tuần (`ngay_dau_tuan`, thứ 2), trạng thái `NHAP | CHO_CONG_BO | CONG_BO | DA_AP_DUNG | DA_HUY`; cột sinh `khoa_tuan = IF(trang_thai <> 'DA_HUY', ngay_dau_tuan, NULL)` `UNIQUE` (FR-023). Nội dung `thuc_don_mon` (ngày, bữa, chế độ ăn, món, thứ tự) và `thuc_don_mon_thay_the` (thứ tự ưu tiên); sửa tự do khi Nháp (`If-Match`), sao chép từ thực đơn trước (món không còn Hiệu lực đánh dấu thay).
- **Gửi kiểm tra** (FR-025, FR-025a): bắt buộc `xacNhanKiemTraCheBien = true` (lưu ghi chú, người, lúc); hiển thị trước số người theo chế độ ăn trong tuần (không tên). Hàm thuần trong một lần:
  - `KiemTraDoPhu(noiDung, cheDoDangDung, buaPhucVu)`: chặn, liệt kê (chế độ ăn, ngày, bữa) thiếu; "chế độ ăn đang có người dùng" theo FR-027 (mặc định luôn có);
  - `LapMon(noiDung, tuanTruoc, tuanSau, cfg03)`: chuỗi ngày liên tiếp theo món và chế độ ăn, qua ranh giới tuần với thực đơn Công bố/Đã áp dụng liền kề; món thay thế không tính;
  - `CanhBaoDiUng(noiDung, nguoiTheoCheDo)`: món xung đột với ít nhất một người không có món thay thế phù hợp; số người (bếp), tên (Bác sĩ).
  Cảnh báo phải được bếp xác nhận đã xem (`canh_bao_da_xac_nhan`, nhóm 3 gắn thực đơn).
- **Công bố / Trả lại** (Bác sĩ): công bố chạy lại các kiểm tra với dữ liệu tại lúc công bố; độ phủ thiếu → chặn; cảnh báo mới → Bác sĩ xác nhận; tuần đã bắt đầu → thẳng `DA_AP_DUNG`. Trả lại bắt buộc lý do. Nhân viên bếp không có quyền công bố (403).
- **Đổi món** (FR-031, FR-032): Nhân viên bếp, lý do; món Hiệu lực; bữa chưa qua giờ dự kiến; chạy lại độ phủ (chặn), lặp món, dị ứng; lưu `doi_mon` (nhóm 3, trước/sau); thông báo Bác sĩ (Nhẹ; Trung bình nếu bữa trong ngày); sau thời điểm chốt → phát sinh (M-08).
- Job hằng ngày: 00:00 thứ 2 `CONG_BO → DA_AP_DUNG`; mốc CFG-M08-07 nhắc (FR-027).
- `ThucDonChungApi` cho 012 (FR-028): món theo ngày, bữa của chế độ mặc định và tên các chế độ ăn.

---

## M-08. Chốt suất, phát sinh, suất ăn người thân (mục E, FR-033 → FR-043, BR-M08-01, 09, 13, Q-306)

**Quyết định**:
- `bua_ngay` (bữa trong ngày; nhóm 2 kỹ thuật): bữa danh mục + ngày + giờ dự kiến (chốt từ CFG-M08-04 lúc tạo), lập khi cần (mở lệnh chốt, phát sinh, nhắc) — không phải bản ghi nghiệp vụ.
- **Tính sẵn khi mở lệnh** (`GET /bua-ngay/{id}/tinh-san`): hàm `NguoiDuocTinh(t, giờ bữa, …)` theo FR-034 với dữ liệu tại lúc gọi:
  - nội trú Đang lưu trú (`NguoiCaoTuoiApi.theoTrangThai`, `ViTriApi`), trừ lượt vắng / chuyến đi trùng giờ bữa (`LuotVangApi`, `ChuyenDiBuaAnPort`); người đang vắng có mốc trở lại ≤ giờ bữa → tính, dấu `DU_KIEN_TRO_VE`;
  - bán trú có lịch (`HopDongDocApi.lichDenTaiNgay`) trùng giờ bữa, không phải ngày nghỉ, trạng thái Chưa đến/Có mặt (`CoMatBanTruApi`), cộng buổi phát sinh Có mặt;
  - người thân Đang ở lại, có đăng ký ăn, bao trùm giờ bữa (`LuotOLaiAnPort`);
  - mỗi người: chế độ ăn (bản gán Hiệu lực hoặc mặc định), kết cấu, tầng/khu, suất đặc biệt (`ChonMonThayThe`, kết cấu khác thường, chỉ định riêng), danh sách cần đối chiếu (dị ứng "khác").
- **Lệnh Chốt suất** (Nhân viên bếp, từ giờ bữa − CFG-M08-01): tính lại tại thời điểm lệnh (không tin số đã xem), ghi `lan_chot` (nhóm 3: người, thời điểm, dấu `CHOT_SAU_GIO_BUA`), `nguoi_duoc_tinh` (nhóm 3), `suat_theo_che_do` (nhóm 3, theo tầng/khu), `suat_an_nguoi_than` (nhóm 2: `DA_GHI`) và phát `SuKienTinhPhiNguon(SUAT_AN_NGUOI_THAN, …)` cho 010; lập `phieu_bua_an` mỗi tầng/khu có suất và `suat_dac_biet`. Món an toàn ngoài giờ hành chính (FR-037a i) áp ngay trong lệnh. Thông báo theo FR-065 (thiếu món thay thế, chưa có món, chưa có thực đơn, chốt sau giờ bữa: Trung bình). Hệ thống không tự chốt; job nhắc tại mốc (FR-033).
- **Phát sinh** (FR-039): `phat_sinh` (nhóm 3): bữa ngày, tầng/khu, chế độ ăn, `so_suat` ±, người liên quan, loại thay đổi, sự kiện nguồn (khóa bỏ trùng), `can_giao` (false với ghi nhận suất giữ, FR-040a), `bep_xac_nhan_luc/boi` (cột đặt một lần), thời điểm. Tạo kèm:
  - lệnh của 011 (áp dụng bản gán, đổi món, chỉ định món thay thế, xác nhận bản sửa món, yêu cầu suất bổ sung, phục vụ suất giữ);
  - listener sau commit cho thay đổi ở module khác (trở về, vắng, chuyển viện, điểm danh chuyến, trạng thái có mặt, chuyển giường, dị ứng, lượt ở lại, ngày khu nghỉ, trạng thái cuối) — mỗi sự kiện xét các bữa đã chốt chưa tới giờ (FR-039 "mỗi bữa một phát sinh"); SC-002 ≤ 5 phút;
  - hàm thuần `TinhPhatSinh(ketQuaChotCu, trangThaiMoi)` trả danh sách ± theo (tầng, chế độ ăn) và suất đặc biệt đổi.
- **Sau giờ bữa** (FR-040): không tự tạo phát sinh; người nhận tại tầng lập `yeu_cau_suat_bo_sung` (gửi bếp như phát sinh) tới hết ngưỡng giao trễ.
- **Suất giữ** (FR-040a): job theo phút tại giờ bữa: suất `DU_KIEN_TRO_VE` của người chưa trở về → phát sinh "−1" `can_giao = false` nguồn `CHUA_TRO_VE_DUNG_DU_KIEN`, suất đánh dấu `SUAT_GIU`; người về trong ngưỡng → lệnh "phục vụ suất giữ" tạo "+1" `can_giao = false`; hết ngưỡng → người nhận ghi hủy bỏ.
- **Món an toàn** (FR-037a ii): job theo phút tại giờ bữa − CFG-M08-05: suất `THIEU_MON_THAY_THE` hoặc chế độ thiếu món chưa được Bác sĩ xử lý → dùng món an toàn nếu không xung đột (phát sinh nếu sau chốt; thông báo Bác sĩ Nhẹ); xung đột → thông báo điều dưỡng phụ trách Trung bình.
- **Nhắc** (FR-041): job theo phút: phát sinh `can_giao` chưa xác nhận tại giờ bữa − CFG-M08-05 → bếp, Bác sĩ (Trung bình); phát sinh tạo sau mốc → nhắc ngay.
- **Suất ăn người thân** (FR-043): phát sinh "−" trước giờ bữa → `DA_HUY`, phát `SuKienTinhPhiNguonDaDoi(daHuy = true)`; sau giờ bữa không hủy.
- **Lịch bữa cho 005** (FR-042): bean `NguonCongViecPort` loại `BUA`: `kiemTra(BUA, buaNgayId, nctId, t)` (bữa tồn tại, người có trong lịch bữa, thời điểm khớp giờ bữa); `xacNhanPhucVu(buaId, nctId)` (FR-050). Bữa bị bỏ vì vắng (từ listener lượt vắng/chuyến/trạng thái) → `NguonCongViecApi.nguonBiHuy(BUA, buaNgayId:nctId, lyDo)` trong transaction của listener. `ThoiKhoaBieuNguonPort` không cài (005 không khai 011).

---

## M-09. Phiếu bữa ăn (mục F, FR-044 → FR-048b, BR-M08-09 → 12, DBR-27)

**Quyết định**:
- `phieu_bua_an` (nhóm 2): `UNIQUE (bua_ngay_id, tang_khu_vuc_id)`; trạng thái `DA_CHOT | DA_CHUAN_BI | DA_GIAO | CO_SAI_LECH | DA_NHAN` theo bảng của spec; người, thời điểm giao, nhận; dấu phiếu (`CHUA_CO_THUC_DON`, `CHOT_SAU_GIO_BUA`).
- `suat_dac_biet` (nhóm 2): phiếu, người, phòng, chế độ ăn, kết cấu, món (phiên bản), món thay thế, dấu (`THIEU_MON_THAY_THE`, `MON_AN_TOAN`, `CHUA_CO_MON`, `CHUA_GAN_CHE_DO_AN`, `DU_KIEN_TRO_VE`, `SUAT_GIU`), `da_chuan_bi`.
- **DBR-27**: tổng suất trên các phiếu của bữa = số đã chốt + phát sinh `can_giao` đã xác nhận; bảo đảm bằng cách phiếu chỉ đổi qua lệnh chốt và lệnh xác nhận phát sinh (cùng transaction, khóa `bua_ngay`); test kiểm bất biến sau mọi kịch bản. Số suất được phục vụ (gồm phát sinh ghi nhận) tính riêng (FR-044).
- **Giao** (FR-047): mọi suất đặc biệt đã chuẩn bị, không còn `THIEU_MON_THAY_THE` chưa xử lý, mọi phát sinh `can_giao` của tầng đã xác nhận, bữa có `luu_mau`.
- **Nhận, sai lệch** (FR-048): người nhận tại tầng = `NhiemVuApi.nguoiNhanTaiTang(tang, ca)` (TT, ĐD, CS có phân công trong ca ± CFG-M15-07, Người phụ trách ca); khu bán trú: tầng/khu mà khu nghỉ gắn vào (003 FR-038). `sai_lech_phieu` (nhóm 3) + cột xử lý đặt một lần. **Phần bổ sung** (FR-048b) là bảng `phan_bo_sung` (phát sinh nguồn, giao, nhận) không đổi trạng thái phiếu.
- **Giới hạn hiển thị cho bếp** (FR-046, SC-008): DTO bếp là lớp riêng `PhieuChoBep` chỉ có trường được phép và danh sách dấu đóng; không dùng giới hạn trường động, để không lọt trường khác. Dị ứng, danh sách cần đối chiếu chỉ ở DTO tầng (người có quyền xem dị ứng).
- **Giao trễ** (FR-048a): job theo phút; một lần mỗi phiếu (`da_bao_tre_luc`); không áp cho phiếu `CHOT_SAU_GIO_BUA`.

---

## M-10. Xác nhận phục vụ (mục G, FR-049 → FR-051, BR-M08-14, Q-307)

**Quyết định**:
- `POST /suat-dac-biet/{id}/xac-nhan-phuc-vu` (Nhân viên chăm sóc, Điều dưỡng trong phạm vi): thân lệnh gồm người đang được phục vụ, khai đã ăn/chưa ăn (khi bị chặn do dị ứng), dữ liệu sự cố (khi đã ăn). Kiểm:
  1. (a) suất đúng người; (b) `XungDot(monTrongSuat, diUngHienHanh, hanCheHienHanh)` tại lúc phục vụ;
  2. sai người, không xung đột → 409 `SUAT_KHONG_DUNG_NGUOI`, không tạo sự cố;
  3. xung đột dị ứng → transaction riêng phát `YeuCauTaoCanhBao(PHUC_VU_SUAT_DI_UNG, TRUNG_BINH, loaiNguon = SUAT_DAC_BIET, nguonId = id)` (commit dù lệnh bị chặn), trả 409 `SUAT_CO_THANH_PHAN_DI_UNG` yêu cầu khai đã ăn/chưa ăn; gửi lại với `daAn = true` kèm sự cố → gọi `SuCoLenhApi.ghiNhan(lenh loại PHUC_VU_SAI_SUAT_AN, nguồn AN_UONG, mức theo triệu chứng, nd)` trong cùng transaction và lưu xác nhận (kết quả `CHAN_XUNG_DOT_DA_AN`) trỏ sự cố; `daAn = false` → lưu xác nhận `CHAN_XUNG_DOT_CHUA_AN`, không sự cố;
  4. chỉ xung đột hạn chế → 409 `SUAT_TRAI_HAN_CHE`.
- `xac_nhan_phuc_vu` (nhóm 3) gắn tầng/khu; đính chính qua 000 (`M08.XAC_NHAN_PHUC_VU`).
- Người trong danh sách cần đối chiếu (FR-036a) cũng phải xác nhận; app hiện nhắc hỏi lại dị ứng trong hồ sơ.
- `NguonCongViecPort.xacNhanPhucVu(buaId, nctId)` trả `true` khi người đó có suất thường và không thuộc danh sách cần đối chiếu, hoặc đã có xác nhận phục vụ thành công (FR-050).

---

## M-11. Đồ ăn gia đình, xem lại chế độ ăn, lưu mẫu (mục H, I, J)

**Quyết định**:
- `do_an_gia_dinh` (nhóm 2): trạng thái `KHONG_SU_DUNG | CAN_XAC_NHAN | DUOC_SU_DUNG | DA_KET_THUC`; ghi nhận chạy hàm thuần `DoiChieuDoAn(doAn, nguoi, danhMucCam)` → trạng thái và lý do (FR-053); `DiemNghiVan` theo FR-054. Không có lệnh từ Không sử dụng sang Được sử dụng. Hạn dùng: job theo phút chuyển Không sử dụng (FR-055). Người gửi có tài khoản và quan hệ Hiệu lực (`QuanHeNguoiThanApi`) nhận thông báo Nhẹ phần `CHUNG`, lý do sức khỏe ở phần `SUC_KHOE` (FR-057).
- **Xem lại chế độ ăn** (FR-058, Q-307): listener `AnKemKeoDai` (005) và `SutCanPhatHien` (007): thông báo Bác sĩ Nhẹ kèm nguồn và chế độ ăn hiện hành; lưu `thong_bao_xem_lai` (kỹ thuật, để bản gán mới chọn tham chiếu, FR-059); không yêu cầu, không hạn, không nhắc lại.
- **Lưu mẫu** (FR-060 → FR-062, Q-39, Q-146): `luu_mau` (nhóm 3, một mỗi bữa ngày, `UNIQUE`), `luu_mau_mon` (món, đã lưu, lý do không lưu); danh sách món cần lưu tính bằng hàm `MonCanLuuMau(thucDon, lanChot, phatSinh)`; ghi thêm món mới sau phát sinh; cột hủy mẫu đặt một lần; job nhắc quá CFG-M08-06.

---

## M-12. Port và SPI cho module làm plan sau

**Quyết định** (`m08.api` cho 012, 014; port nội bộ cho 019):

```java
interface LuotOLaiAnPort {                 // 012 cài; chỉ đọc
  List<LuotOLaiView> dangOLaiCoDangKyAn(Instant t);    // người thân, người cao tuổi được gắn, khoảng, đăng ký ăn (FR-034 c)
}
record LuotOLaiDaDoi(long luotId, long nctId, long nguoiThanId, String trangThai, boolean dangKyAn,
                     Instant tu, Instant den, String khoaSuKien) {}      // 012 phát khi bắt đầu, kết thúc sớm, hủy, đổi đăng ký ăn

interface ChuyenDiBuaAnPort {              // 014 cài; chỉ đọc
  List<ChuyenTrungBuaView> trungGioBua(Instant gioBua);   // chuyến đã lên lịch trùng giờ bữa: người tham gia, dự kiến trở lại
}
record ChuyenDiDaDoi(long chuyenId, Set<Long> nctIds, Instant tu, Instant denDuKien, String loai, String khoaSuKien) {}
    // 014 phát: lên lịch, đổi người tham gia, gia hạn giờ về, hủy chuyến, đính chính điểm danh
```

Bean thiếu: `LuotOLaiAnPort` → không tính người thân (và báo "chưa có dữ liệu" trong màn tính sẵn); `ChuyenDiBuaAnPort` → không trừ người đi chuyến đã lên lịch (người đã điểm danh rời viện vẫn trừ theo trạng thái Hoạt động bên ngoài của 001).

---

## M-13. Sự kiện và thông báo (FR-065)

**Đối tượng thông tin** (bổ sung 2026-10-18; FR-065, 009 FR-001a): thông báo "Đồ ăn gia đình Không sử dụng" tới người gửi khai `doiTuongThongTin = VE_VIEC_NGUOI_THAN`; lý do có nội dung sức khỏe đặt ở phần `SUC_KHOE`; các thông báo khác chỉ tới nhân viên.

**Quyết định**: Mức theo bảng FR-065 của spec (đủ mức). Mọi thông báo cài `CoYeuCauThongBao` (009 B-02); thông báo tới bếp không có dị ứng, bệnh lý, thành phần xung đột (nội dung dựng từ DTO bếp). Sự kiện khác: `SuKienTinhPhiNguon*` (010), `YeuCauTaoCanhBao`, `YeuCauDongCanhBaoNguon` (007), `PhieuDaGiao`, `PhieuCoSaiLech` (016).

---

## M-14. Quyền (FR-063)

**Quyết định**:

| `ChucNang` | Thao tác |
| --- | --- |
| `CHE_DO_AN` | `THUC_HIEN` (Bác sĩ: danh mục chế độ ăn, món an toàn, thành phần, bản gán); `XEM` (Điều dưỡng, Quản lý viện; người thân khi có quyền xem sức khỏe) |
| `MON_AN_THUC_DON` | `LAP` (Nhân viên bếp: món, thực đơn, đổi món); `DUYET` (Bác sĩ: xác nhận món, công bố, trả lại); `XEM` (Điều dưỡng, Quản lý viện) |
| `SUAT_PHIEU` | `CHOT`, `CHUAN_BI_GIAO`, `XAC_NHAN_PHAT_SINH`, `LUU_MAU` (Nhân viên bếp); `XEM` (Trưởng tầng trong phạm vi, Bác sĩ, Quản lý viện) |
| `NHAN_PHIEU` | `THUC_HIEN` (người nhận tại tầng theo nhiệm vụ, 002 FR-019) |
| `PHUC_VU_SUAT_DAC_BIET` | `THUC_HIEN` (Nhân viên chăm sóc, Điều dưỡng trong phạm vi) |
| `DO_AN_GIA_DINH` | `THUC_HIEN` (Điều dưỡng trong phạm vi, Bác sĩ) |
| `DANH_MUC_DINH_DUONG` | `CAU_HINH` (Quản lý viện: bữa, loại cấm) |

Trưởng tầng không xem bản gán, thực đơn qua chức năng 011 (Phụ lục 27). Hành chính không có quyền. Nhân viên bếp không xem bản gán, dị ứng, tên người trong cảnh báo thực đơn.

---

## M-15. Bộ lập lịch, lưu giữ, nhật ký, kiểm thử

**Quyết định**:
- **Job**: `ViecTheoPhutM08` (bản gán Chờ hiệu lực; nhắc chốt; món an toàn tại mốc CFG-M08-05; suất giữ tại giờ bữa; nhắc phát sinh; giao trễ; hạn dùng đồ ăn gia đình; nhắc hủy mẫu); `ViecHangNgayM08` 00:00 (thực đơn Đã áp dụng; nhắc CFG-M08-07). Cột "đã làm lúc" trên dòng nguồn chống trùng (NFR-04); chạy bù khi khởi động. Job không chốt suất, không lập phiếu (Q-306).
- **Lưu giữ**: bản gán, xác nhận phục vụ, đồ ăn gia đình, suất ăn người thân: `THEO_HO_SO`; danh mục, thực đơn, lần chốt, phiếu, phát sinh, lưu mẫu: `KHONG_GAN_HO_SO` (giữ khi còn tham chiếu từ hồ sơ).
- **Nhật ký** (FR-064): mọi lệnh; trường dị ứng, hạn chế trong trước/sau mang `SUC_KHOE`.
- **Kiểm thử**:
  - test bảng `CanLyDoChuyenMon`, `XungDot`, `ChonMonThayThe`, `KiemTraDoPhu`, `LapMon` (qua tuần), `CanhBaoDiUng`, `NguoiDuocTinh`, `TinhPhatSinh`, `DoiChieuDoAn`, `MonCanLuuMau`;
  - SC-001: bộ 7 ngày × 60 người dựng sẵn so tính tay; SC-002: phát sinh ≤ 5 phút và DBR-27 sau mọi kịch bản;
  - Test tích hợp MySQL: `UNIQUE` bản gán, lần chốt, phiếu, thực đơn mỗi tuần, một mặc định; trigger nhóm 3;
  - test đồng thời: hai lệnh chốt; hai bác sĩ áp dụng; xác nhận phát sinh và giao cùng lúc;
  - SC-008: quét mọi DTO, thông báo tới bếp không có trường sức khỏe;
  - đồng hồ giả lập cho CFG-M08-01, 03 → 07, giờ bữa;
  - bản giả `LuotOLaiAnPort`, `ChuyenDiBuaAnPort`.
- Tên test theo BR/FR (`BR_M08_01_ChotSuatIT`, `DBR_27_TongPhieuIT`).

---

## M-16. Suất ăn dự kiến cho kênh người cao tuổi *(bổ sung 2026-10-18; FR-063, Q-366, Q-367)*

**Quyết định**: `SuatAnDuKienApi.cuaNguoiCaoTuoi(nctId, tu, den)` tính bằng cùng cách dựng suất đặc biệt của phiếu bữa ăn (FR-036) nhưng không chốt, không ghi gì: với mỗi ngày, bữa, trả món của chế độ ăn hiện hành trên thực đơn đã Công bố hoặc Đã áp dụng, và món thay thế nếu có. DTO chỉ có ngày, bữa, tên món, cờ "món thay thế"; không có tên chế độ ăn, kết cấu, thành phần dị ứng (Q-367). Tuần chưa có thực đơn công bố thì trả rỗng.

**Lý do**: Dùng lại quy tắc đã kiểm (FR-036) nên người cao tuổi thấy đúng món sẽ được phục vụ; DTO hẹp giữ BR-M10-11.

**Phương án đã cân nhắc**: Chỉ trả thực đơn chung (người dùng chế độ ăn riêng sẽ thấy món không phải của mình). Trả cả tên chế độ ăn (là thông tin sức khỏe theo cổng người thân).

