# Research: Người thân và cổng thông tin gia đình (012)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

Mỗi mục gồm Quyết định, Lý do, Phương án đã cân nhắc. Quyết định nền R-01 → R-14 của 000 và các mẫu đã chốt ở 001 → 011 (H-02 bean trạng thái, C-02 / S-02 chiều phụ thuộc, A-08 giới hạn trường, B-02 khuôn thông báo, P-04 sự kiện tính phí) được dùng lại, không quyết lại. Các quyết định dưới đây là kỹ thuật; chúng không đổi nghiệp vụ (constitution IX). Chỗ spec chưa rõ hoặc mâu thuẫn được ghi ở plan, mục "Điểm cần báo lại".

---

## F-01. Vị trí trong backend

**Quyết định**: Feature 012 là module M10 – Người thân (mục 14), gói `com.cambae.backend.m10`.

| Gói con | Nội dung | Mục spec |
| --- | --- | --- |
| `m10.nguoithan` | Hồ sơ người thân, quan hệ, vai trò, thứ tự liên hệ, phiếu đăng ký người thân | A |
| `m10.quyen` | Quyền theo quan hệ, yêu cầu thay đổi quyền (BR-M10-07), dấu "được tự về", tính quyền có tác dụng | B, C |
| `m10.don` | Bản ghi đón, lần chặn đón, yêu cầu ngoại lệ đón | D |
| `m10.tham` | Lượt thăm, giữ chỗ khung | E |
| `m10.olai` | Lượt người thân ở lại, đêm tính phí | F |
| `m10.cong` | Cổng: tổng quan, các phần sức khỏe gọi module nguồn, nhật ký lượt xem sức khỏe | G |
| `m10.bantin` | Bản tin tuần, số liệu tổng hợp, đính chính | H |
| `m10.phanhoi` | Phản hồi, khiếu nại, hạn, leo thang | I |
| `m10.trangthai`, `m10.lich`, `m10.api` | Bean trạng thái; job; giao diện công khai và bản cài SPI | |

---

## F-02. Chiều phụ thuộc giữa module

**Quyết định**: 012 phụ thuộc 000 → 011. Mọi chỗ module thấp hơn cần dữ liệu người thân đã được khai báo SPI ở module đó; M10 cài. Riêng số hoạt động cho bản tin, 012 khai SPI `HoatDongBanTinPort` ở `m10.api` cho 014 cài (F-14).

| SPI (gói khai báo) | Người khai | Bản cài 012 |
| --- | --- | --- |
| `QuanHeNguoiThanApi` (`m15.taikhoan.api`) | 002 (dùng bởi 001, 002, 006, 011) | Quan hệ Hiệu lực, người đại diện, quyền, số điện thoại đã đăng ký (F-04) |
| `DieuKienChuThich` ² (`m15.quyen.api`) | 002 A-05 | Chú thích ² "chỉ người đại diện" |
| `ThamGiaChuyenTrangThai` (`m01.api`) | 001 H-02 | Điều kiện DBR-02 (Hoàn tất tiếp nhận); tác động trạng thái cuối, vắng (F-13) |
| `NguoiDaiDienPort`, `BanGhiDonPort` (`m02.api`) | 004 | F-04, F-07 |
| `BanGhiDonBanTruPort`, `ThoiKhoaBieuNguonPort` (`m04.api`) | 005 | F-07; lượt thăm cho thời khóa biểu (FR-048 của 005) |
| `NguoiThanKhanCapPort` (`m05.api`) | 007 | Liên hệ chính, người đại diện, lượt thăm cho truy vết (FR-038) |
| `NguoiThanNhanThongBaoPort` (`m13.api`) | 009 | Quan hệ, quyền, thứ tự gọi (FR-012) |
| `LuotOLaiAnPort` + sự kiện `LuotOLaiDaDoi` (`m08.api`) | 011 | F-09 |
| `m11.api.SuKienTinhPhiNguon*` | 010 | Đêm ở lại (F-09) |

012 đọc: `NguoiCaoTuoiApi`, `BanDongYApi`, `MucSucKhoeApi` (001); `ViTriApi`, khu nghỉ bán trú (003); `HopDongDocApi`, `LuotVangApi`, `DieuKienTiepNhanApi` (004); `CoMatBanTruApi` (gồm `diemDanhVe` MANDATORY), `KeHoachChamSocApi`, `KetQuaChamSocCongApi` (005, bổ sung theo plan này, F-10); `ThuocCongNguoiThanApi` (006); `SucKhoeCongNguoiThanApi`, `BanTinSucKhoeApi`, `KhoanhVungApi`, `SuCoApi` (007); `NhiemVuApi` (Trưởng tầng được giao, 008 qua 002); `ThongBaoTraCuuApi` (009); `ChiPhiCongNguoiThanApi` (010); `CheDoAnApi`, `ThucDonChungApi` (011). Gọi `TaiKhoanApi.xetLaiNguoiThan` (002, MANDATORY) khi kết thúc quan hệ.

013, 014, 016, 017, 018 làm plan sau, phụ thuộc 012 qua `m10.api` (F-14); phần cổng của họ (đồ gửi, hoạt động và đồng ý về muộn, số dư) do REST của chính họ phục vụ, kiểm quyền người thân qua `QuyenNguoiThanApi`.

**Lý do**: Modulith `verify()` từ chối vòng (000 R-02). Cách 010 phục vụ chi phí cho người thân (REST của 010, kiểm quyền qua 012) được dùng lại.

**Phương án loại**: 012 làm cổng gom mọi dữ liệu qua SPI của 013, 014, 017: thêm ba SPI chỉ để chuyển tiếp, trong khi web và app gọi được thẳng REST của từng feature.

---

## F-03. Khóa và đồng thời

**Quyết định**:
- **Thứ tự khóa**: … → `chi_phi_nct` (M11) → `nguoi_than_nct` (M10, một dòng mỗi người cao tuổi) → `nguoi_than` (theo `id` tăng) → `khung_tham_ngay` → dòng con. Bean trạng thái của 012 chạy sau 010 (`@Order` của 001).
- **DBR-02**: mọi lệnh đổi quan hệ, vai trò của một người cao tuổi khóa `nguoi_than_nct`, áp thay đổi, rồi kiểm lại bất biến trước commit (F-05). Cột sinh `khoa_lien_he_chinh = IF(trang_thai = 'HIEU_LUC' AND la_lien_he_chinh, nct_id, NULL)` `UNIQUE`.
- **Yêu cầu thay đổi quyền** (FR-018): cột sinh `khoa_cho = IF(trang_thai = 'CHO_XAC_NHAN', CONCAT(doi_tuong, ':', quyen), NULL)` `UNIQUE`, với `doi_tuong` là quan hệ hoặc người cao tuổi (dấu "được tự về"). Yêu cầu tắt "được phép đón" hủy yêu cầu bật đang chờ trong cùng transaction.
- **Sức chứa khung thăm** (FR-030 b, SC-004): dòng `khung_tham_ngay (ngay, gio_bat_dau, gio_ket_thuc)` khóa `FOR UPDATE`; tổng số người tính lại trong khóa; đăng ký ghi nhận trước giữ chỗ. 20 đăng ký đồng thời cho chỗ cuối: một thành công.
- **Quyết định trên yêu cầu, ngoại lệ, lượt ở lại, phản hồi**: khóa dòng, kiểm tra lại; lệnh thứ hai 409 `TRANG_THAI_DA_DOI` (000 FR-040).

---

## F-04. Quan hệ, vai trò, quyền và quyền có tác dụng (mục A, B; DBR-02, DBR-03)

**Quyết định**:
- `nguoi_than` (nhóm 1) và `quan_he_nguoi_than` (nhóm 2) với sáu cột quyền (`xem_suc_khoe`, `xem_chi_phi`, `nhan_khan`, `dang_ky_tham`, `duoc_phep_don`, `yeu_cau_dich_vu`), `la_dai_dien`, `la_lien_he_chinh`, `thu_tu_lien_he`. Quyền chỉ đổi qua yêu cầu (F-06) hoặc tác động tự động của FR-009, FR-010, FR-006. Lịch sử từng quyền lưu ở `lich_su_quyen` (nhóm 3: quan hệ, quyền, giá trị, từ lúc, yêu cầu nguồn) để trả lời "tại thời điểm T ai được phép đón" (FR-068) bằng truy vấn khoảng.
- Hàm thuần `QuyenCoTacDung(quanHe, quyen, banDongY, t)`: quyền đang bật và, với `XEM_SUC_KHOE`, bản đồng ý Hiệu lực bao gồm người thân (FR-011). Kết quả "bật nhưng không có tác dụng" hiển thị cho Hành chính và người thân.
- **Bản cài `QuanHeNguoiThanApi`** (khai báo ở `m15.taikhoan.api`, F-14): `hieuLuc(nguoiThanId)`, `soDienThoaiDaDangKy`, `laNguoiDaiDienHieuLuc(nguoiThanId, nctId, t)`, `thuocNguoiCaoTuoi`, `quyenDangBat(nguoiThanId, nctId, quyen, t)`. Người được đề nghị làm người đại diện khi yêu cầu còn Chờ xác nhận không là người đại diện (FR-005). 002 FR-046 ghép lớp quyền của 002 với lớp này; 012 không tính phạm vi tài khoản.
- `NguoiDaiDienPort` (004) và `QuanHeNguoiThanApi.laNguoiDaiDienHieuLuc` dùng chung một hàm nội bộ.
- **Kết thúc quan hệ** (FR-006): trong cùng transaction: bỏ khỏi danh sách đón (bản ghi đón chưa dùng → Không dùng), hủy lượt thăm tương lai do người đó đăng ký, hủy yêu cầu Chờ xác nhận của người đó, gọi `TaiKhoanApi.xetLaiNguoiThan` (MANDATORY).

---

## F-05. DBR-02 và điều kiện Hoàn tất tiếp nhận (FR-003 → FR-005)

**Quyết định**:
- Hàm thuần `KiemTraDbr02(quanHeHieuLuc, trangThaiNct)`: với `DANG_LUU_TRU`, `TAM_VANG`, `DIEU_TRI_BV`: đúng một liên hệ chính, ≥ 1 người đại diện. Chạy sau mọi lệnh Đổi liên hệ chính, Đặt/Thôi người đại diện (kể cả khi yêu cầu chuyển Hiệu lực), Kết thúc quan hệ; vi phạm → 409 `VI_PHAM_DBR_02`, rollback.
- Bean `ThamGiaChuyenTrangThai` cho `HOAN_TAT_TIEP_NHAN`: trả `DieuKien("DBR_02", …, coTheMien = false)` khi chưa đạt. Với `DANG_TIEP_NHAN` các lệnh của 012 cho lưu khi chưa đủ.
- "Thôi người đại diện" chặn khi người đó là người đồng ý trên bản đồng ý Hiệu lực (`BanDongYApi.hienHanh(nctId).nguoiDongY`) mà chưa có bản thay thế (FR-005).
- Người đại diện đầu tiên: lệnh trực tiếp của Hành chính (bằng chứng, lý do). Khi đã có người đại diện: lệnh Đặt/Thôi tạo yêu cầu loại `DAT_DAI_DIEN` / `THOI_DAI_DIEN` (F-06), xác nhận bởi người đại diện hiện có khác người bị tác động hoặc Quản lý viện duyệt.

---

## F-06. Yêu cầu thay đổi quyền (mục C, FR-013 → FR-020; Q-118, Q-126, Q-127, Q-249, Q-267)

**Quyết định**:
- `yeu_cau_quyen` (nhóm 2): loại (`QUYEN`, `TU_VE`, `DAT_DAI_DIEN`, `THOI_DAI_DIEN`), quan hệ hoặc người cao tuổi, quyền, giá trị mới, thông tin người đón mới, nguồn (`QUAY`, `CONG`, `PHIEU_DANG_KY`), người lập, lý do, trạng thái `CHO_XAC_NHAN | HIEU_LUC | TU_CHOI | HUY`, cách xác nhận (`CONG`, `BAN_KY`, `QL_DUYET`), bằng chứng (tệp), thời điểm hiệu lực. Vòng đời riêng, không dùng `yeu_cau_phe_duyet` của 000 (FR-020); nhật ký, lý do, khóa theo 000.
- Hàm thuần `KiemTraYeuCau(yc, quanHe, banDongY, nguoiLap)` (FR-013, FR-014, FR-007, FR-011): chạy lúc lập và lúc xác nhận/duyệt.
- Lập bởi người đại diện qua cổng hoặc Hành chính kèm bản ký → thẳng `HIEU_LUC` (FR-015 a, b). Yêu cầu tắt `duoc_phep_don` hoặc tắt `TU_VE`: áp ngay khi lập (quyền về false, bản ghi đón chưa dùng của người đó → Không dùng), trạng thái yêu cầu vẫn Chờ xác nhận; Từ chối/Hủy → khôi phục từ thời điểm có kết quả (ghi `lich_su_quyen` mới, không hồi tố).
- Áp khi Hiệu lực: cập nhật cột quyền, ghi `lich_su_quyen`; giá trị hiện hành là của yêu cầu có thời điểm Hiệu lực muộn nhất (khóa `nguoi_than_nct` bảo đảm thứ tự). Thông báo Q-249 tính tập người đại diện **sau** khi áp (Q-267).
- **Phiếu đăng ký người thân** (FR-008): `phieu_dang_ky_nguoi_than` (nhóm 3: bản scan bắt buộc, người đại diện ký, danh sách (người thân, quyền)); lệnh ghi phiếu tạo mỗi quyền một yêu cầu `HIEU_LUC` cách xác nhận `BAN_KY`, vẫn qua `KiemTraYeuCau`; một quyền không đạt → cả phiếu bị chặn kèm danh sách lý do.
- **Trái mong muốn** (FR-020b, bổ sung 2026-10-18): F-06a.
- **Nhắc** (FR-019): job hằng giờ, `NhacQuaHan` của 000 với mốc CFG-M10-11 (nhắc người đại diện + Quản lý viện; báo Quản lý viện); không tự Từ chối.

---

## F-07. Quy trình đón, ngoại lệ đón (mục D, FR-021 → FR-028; BR-M10-03, Q-121, Q-125, Q-295)

**Quyết định**:
- **Lệnh Ghi đón** (`POST /nguoi-cao-tuoi/{id}/don`) chỉ trực tuyến (FR-028; không đăng ký `NgoaiTuyenHandler`). Kiểm: người thực hiện có quyền lệnh nguồn theo loại đón (FR-022, qua `QuyenApi` với khóa của lệnh nguồn: `M01.CHO_TAM_VANG`, `M04.DIEM_DANH_VE`, `M02.KET_THUC_LUU_TRU`) và phạm vi; người đón thuộc danh sách tại thời điểm (`lich_su_quyen`) hoặc có ngoại lệ `HIEU_LUC` cho đúng (người cao tuổi, số giấy tờ); số giấy tờ xuất trình khớp số đã đăng ký. Không đạt → ghi `lan_chan_don` (nhóm 3, transaction riêng để còn sau rollback) và 409 `CHAN_DON` kèm lý do; Trưởng tầng được báo Nhẹ.
- **Loại "Tự về"**: không người đón; kiểm dấu `TU_VE` đang bật; nhân viên tiễn bắt buộc.
- **Loại "Viện đưa về"** (Q-295): người đưa về là người có quyền Điểm danh về; lệnh ghi bản ghi đón và gọi `CoMatBanTruApi.diemDanhVe(…, VIEN_DUA_VE)` (MANDATORY) trong cùng transaction; bản ghi mang sự kiện `DA_DUNG` ngay, không áp CFG-M10-09.
- `ban_ghi_don` (nhóm 3) + `ban_ghi_don_su_kien` (nhóm 3: `DA_DUNG` kèm lệnh nguồn và id, `KHONG_DUNG` kèm lý do). Trạng thái suy ra từ sự kiện gần nhất: `CHUA_DUNG`, `DA_DUNG`, `KHONG_DUNG`. `UNIQUE (ban_ghi_don_id)` trên sự kiện `DA_DUNG` (một lần dùng).
- **Bản cài port** (004 `BanGhiDonPort`, 005 `BanGhiDonBanTruPort`): `banGhiDonHopLe` / `hopLe` trả bản ghi `CHUA_DUNG`, đúng người cao tuổi, chưa quá CFG-M10-09, loại phù hợp lệnh. Bổ sung theo plan này hàm `daDung(banGhiDonId, lenhNguon, nguonId)` (MANDATORY) cho 004 (Cho tạm vắng, bàn giao khi Kết thúc lưu trú) và 005 (Điểm danh về): ghi sự kiện `DA_DUNG` trong transaction của lệnh nguồn; gọi lần hai → lỗi `BAN_GHI_DON_DA_DUNG` (rollback lệnh nguồn).
- **Hết hiệu lực** (FR-027): job theo phút ghi `KHONG_DUNG` cho bản ghi `CHUA_DUNG` quá CFG-M10-09 (giá trị đọc lúc ghi đón, lưu `het_hieu_luc_luc`).
- **Ngoại lệ đón** (FR-024): `yeu_cau_ngoai_le_don` (nhóm 2) `CHO_XAC_NHAN | HIEU_LUC | DA_DUNG | HET_HAN | TU_CHOI | HUY`; `het_han_luc` = lúc Hiệu lực + CFG-M10-08; `ban_ghi_don_id` đặt một lần khi dùng; job chuyển Hết hạn. Thông báo Quản lý viện gửi ngay (`CachThay.KhongThay` giờ yên tĩnh của 009 không áp cho nhân viên), người đại diện mức Trung bình theo giờ yên tĩnh.

---

## F-08. Lượt thăm (mục E, FR-029 → FR-039; BR-M10-02, Q-119, Q-120)

**Quyết định**:
- `luot_tham` (nhóm 2): `DA_DUYET | DA_VAO | DA_RA | KHONG_DEN | HUY`; khung (giờ bắt đầu, kết thúc) chép từ CFG-M10-04 lúc đăng ký (FR-032); `nguoi_di_cung` (bảng con: họ tên, quan hệ, số điện thoại tùy chọn, `thuc_te_vao`).
- Hàm thuần `KiemTraDangKyTham(khung, cfg04, cfg05, cfg06, trangThaiNct, luotVang, lichBanTru, vung, t, nguon)` trả danh sách lý do theo (a) → (e); không đạt → 422 `KHONG_DAT_DIEU_KIEN_THAM` kèm mọi lý do, không tạo lượt. (c) dùng `KhoanhVungApi.dangChiuTheoNguoi`, theo khu nghỉ bán trú, và khu tiếp khách (mã khu cấu hình của 003). Đạt → `DA_DUYET` và giữ chỗ trong khóa `khung_tham_ngay` (F-03).
- Vào (FR-033): kiểm lại (c), (d); ghi người thực tế (thêm tên người không có trong danh sách trước khi vào; tổng ≤ số đã đăng ký). Ra: giờ ra ≤ thời điểm ghi, ≥ giờ vào.
- **Tự động**: job theo phút: hết khung chưa Vào → `KHONG_DEN`; Đã vào quá giờ kết thúc + CFG-M10-07 → nhắc Hành chính một lần. Listener: `LuotVangDaMo`, `BaoVangBanTruDaGhi` (004), `NguoiCaoTuoiDaChuyenTrangThai` (Điều trị bệnh viện, trạng thái cuối) → hủy lượt trùng thời gian (FR-036); `KhoanhVungDaTao` (007) → hủy lượt có giường trong vùng chưa kết thúc khung, liệt kê cho Hành chính và Trưởng tầng, báo Hành chính lượt Đang vào (FR-037). Hủy trả chỗ.
- Bản cài `NguoiThanKhanCapPort.luotThamTrongKhoang` (FR-038) và `ThoiKhoaBieuNguonPort.theoNgay` (lượt Đã duyệt, Đã vào trong ngày).

---

## F-09. Người thân ở lại (mục F, FR-040 → FR-045; BR-M10-04, Q-123, Q-128)

**Quyết định**:
- `luot_o_lai` (nhóm 2): `CHO_XAC_NHAN | DA_XAC_NHAN | TU_CHOI | DANG_O_LAI | DA_KET_THUC | HUY`; bắt đầu, kết thúc dự kiến (lịch sử gia hạn ở `luot_o_lai_gia_han`, nhóm 3), thực tế; vị trí; `dang_ky_an`; ý kiến người cùng phòng (bắt buộc khi phòng có ≥ 2 giường đang sử dụng, `ViTriApi`).
- Giới hạn chồng thời gian (FR-040): kiểm trong khóa `nguoi_than_nct` và khóa `nguoi_than`: số lượt `DA_XAC_NHAN`, `DANG_O_LAI` chồng của người cao tuổi ≤ CFG-M10-10; người thân không có hai lượt mở chồng.
- Chặn khoanh vùng/cách ly và trạng thái (FR-041) lúc đăng ký, xác nhận, bắt đầu.
- **Đêm tính phí** (FR-043): hàm thuần `SoDem(batDau, ketThuc)` đếm số mốc 00:00. Job 00:00: mỗi lượt `DANG_O_LAI` bắt đầu trước mốc → phát `SuKienTinhPhiNguon(DEM_O_LAI, nguonId = luotId + ngày bắt đầu đêm, donVi = DEM, ngayPhatSinh = ngày bắt đầu đêm)`; lưu `dem_o_lai` (kỹ thuật, `UNIQUE (luot_id, ngay)`) để chống trùng và chạy bù. Kết thúc ghi giờ thực tế sớm hơn một mốc đã phát → `SuKienTinhPhiNguonDaDoi(daHuy = true)` cho đêm đó. Không tạo khoản (Q-306).
- **Cho 011**: bản cài `LuotOLaiAnPort.dangOLaiCoDangKyAn(t)`; phát `m08.api.LuotOLaiDaDoi` khi Bắt đầu, Gia hạn, Kết thúc, Hủy.
- Người cao tuổi rời viện khi có lượt Đang ở lại (FR-044): listener nhắc Hành chính và Trưởng tầng (Trung bình); không tự kết thúc.

---

## F-10. Cổng thông tin (mục G, FR-046 → FR-050; BR-M10-01, NFR-08)

**Quyết định**:
- Một màn cổng, bốn loại thông tin. Web và app người thân dựng màn bằng các REST:
  - 012 phục vụ: danh sách người cao tuổi của người thân, thẻ **chung** (họ tên, ảnh, phòng, trạng thái, lượt vắng, lịch đến/về bán trú, lượt thăm, phản hồi, bản tin của mình, thực đơn chung qua `ThucDonChungApi`), phần **sức khỏe** (cổng trung gian, F-11), phần **hợp đồng** (`HopDongDocApi`, `DieuKienTiepNhanApi`; chỉ người đại diện).
  - REST của feature sở hữu: chi phí, mua hộ (010); số dư (017); đồ gửi (013); hoạt động, đồng ý về muộn (014); thông báo (009). Các feature đó kiểm quyền qua `m10.api.QuyenNguoiThanApi`.
- Kiểm quyền người thân ở backend theo `QuyenCoTacDung` tại thời điểm xem; client chỉ ẩn nút. DTO trả cho người thân là lớp riêng, chỉ giá trị hiện hành (FR-047).
- **Sự cố trên cổng** (FR-046, Q-352, bổ sung 2026-10-18): thẻ chung gồm phần chung của sự cố (kèm mức) cho mọi người thân có quan hệ, lấy từ `SucKhoeCongNguoiThanApi.suCo` của 007 (`phanChung`); phần sức khỏe của sự cố đi qua F-11 như các mục sức khỏe khác và chỉ khi có quyền xem sức khỏe có tác dụng; nhật ký xem chỉ ghi cho phần sức khỏe.
- Mục đồ gửi (FR-046b): `QuyenNguoiThanApi.xemDoGui(nguoiThanId, nctId, t)` = người đại diện hoặc `duoc_phep_don` đang bật; 013 dùng.
- Yêu cầu thay đổi dịch vụ (FR-049): chỉ người đại diện có `yeu_cau_dich_vu`; REST của 004 kiểm qua `NguoiDaiDienPort` và `QuyenNguoiThanApi`.

---

## F-11. Phần sức khỏe và nhật ký lượt xem (FR-046 (2), FR-048)

**Quyết định**:
- 012 là điểm ghi duy nhất của `nhat_ky_xem_suc_khoe` (nhóm 3). Phần sức khỏe trên cổng đi qua `GET /cong/nguoi-cao-tuoi/{id}/suc-khoe/{phan}` của 012; 012 kiểm `QuyenCoTacDung(XEM_SUC_KHOE)`, gọi API nguồn (dị ứng, bệnh nền: 001 `MucSucKhoeApi`; chỉ số, cân nặng, sự cố: 007 `SucKhoeCongNguoiThanApi`; thuốc: 006 `ThuocCongNguoiThanApi`; kết quả chăm sóc, tỷ lệ ăn, lượng nước: 005 `KetQuaChamSocCongApi`; chế độ ăn riêng: 011 `CheDoAnApi`; lịch khám bên ngoài: 004 lượt vắng loại khám) và ghi nhật ký trong cùng yêu cầu. Chú thích "ghi nhật ký xem" ở hợp đồng 006, 007 được hiểu là do 012 ghi (sửa chú thích, mục Đồng bộ của plan).
- Lượt mở thông báo, bản tin có phần sức khỏe (FR-048): listener `ThongBaoDaXem` (009) ghi một dòng với nguồn `THONG_BAO` hoặc `BAN_TIN` khi bản đã gửi người đó có phần `SUC_KHOE`.
- Quản lý viện tra cứu nhật ký (`GET /nhat-ky-xem-suc-khoe`); người thân không có đường dẫn.

---

## F-12. Bản tin tuần (mục H, FR-051 → FR-057; BR-M10-06, 08, 09, Q-124, Q-129, Q-306, Q-307)

**Quyết định**:
- `ban_tin` (nhóm 2 khi `CHO_DUYET`, nhóm 3 khi `DA_GUI` / `KHONG_GUI`): người cao tuổi, kỳ (`ky_tu`, `ky_den`, `moc`), `ban_goc_id NULL`; cột sinh `khoa_ky = IF(ban_goc_id IS NULL, CONCAT(nct_id, ':', ky_tu), NULL)` `UNIQUE` (lệnh lập trùng bị chặn). `ban_tin_phan` (loại `CHUNG`, `SUC_KHOE`, `CHI_PHI`; nội dung), `ban_tin_so_lieu` (mã số liệu, giá trị lưu lúc duyệt, phần), `ban_tin_nhan_xet` (nhóm 3), `giai_thich_su_co`, người duyệt, `duyet_thay`.
- **Số liệu tổng hợp** (FR-052): `GET /ban-tin/{id}/so-lieu` tính lúc gọi từ 005 (`KetQuaChamSocCongApi.tongHop`), 007 (`BanTinSucKhoeApi.choBanTin`; sửa 2026-10-18 theo Q-352: phần chung của mỗi sự cố, kèm mức, vào nhóm số liệu `CHUNG`, phần sức khỏe vào nhóm `SUC_KHOE`), 010 (`ChiPhiCongNguoiThanApi.tamTinh` dạng tổng theo loại), 014 (số hoạt động đã tham gia qua SPI `HoatDongBanTinPort` khai ở `m10.api`, 014 cài vì làm plan sau; bean thiếu → "không có dữ liệu trong kỳ"). Không soạn sẵn nội dung (Q-307).
- **Duyệt** (FR-053, BR-M10-09): người duyệt là Điều dưỡng có phạm vi lúc thao tác, hoặc Trưởng tầng được giao sau CFG-M10-12 kể từ mốc (dấu `duyet_thay`). Mức sự cố cao nhất trong kỳ theo `SuCoApi.theoNguoi(…, chiChuaHuy = true)` mức cao nhất từng có (Q-129) ≥ Trung bình → bắt buộc `giai_thich_su_co`. Duyệt → `DA_GUI`, phát `YeuCauThongBao` mức Nhẹ tới mọi người thân có quan hệ Hiệu lực, `doiTuongThongTin = VE_NGUOI_CAO_TUOI`, phần theo loại thông tin; 009 lọc theo mục "nhận thông báo" của bản đồng ý (FR-054, Q-353; sửa 2026-10-18) rồi theo quyền từng người (SC-007), ghi thời điểm gửi và xem (BR-M10-06). Người cao tuổi chưa có bản đồng ý Hiệu lực thì không người thân nào nhận; bản tin vẫn ở `DA_GUI` và xem được trên cổng theo quan hệ (spec "Điểm cần báo lại" 2, 3 của lượt 2026-10-18).
- **Đính chính** (FR-056): bản mới `ban_goc_id` trỏ bản gốc, lý do; người nhận theo quan hệ, quyền hiện hành. Listener `SuCoDaHuyTrongBanTin` (007) → báo Hành chính và người đã duyệt; không tự gửi.
- **Job** (theo lịch CFG-M10-03, đầu ngày mốc): nhắc lập cho người đủ điều kiện (FR-051, loại trừ người vắng cả kỳ); bản tin `CHO_DUYET` kỳ trước → `KHONG_GUI`; báo Quản lý viện danh sách chưa lập / Không gửi (FR-073). Job theo giờ: mốc 48 giờ (nhắc Trưởng tầng), mốc CFG-M10-12 (báo Trưởng tầng có thể duyệt thay). Hệ thống không tạo bản tin (SC-006).

---

## F-13. Phản hồi, khiếu nại (mục I, FR-058 → FR-066; BR-M10-05, Q-122)

**Quyết định**:
- `phan_hoi` (nhóm 2 cho trạng thái): `MOI | DANG_XU_LY | DA_PHAN_HOI | MO_LAI | DONG`; loại, nhóm nội dung, mức (`CAO`, `THUONG`), `han_xu_ly`, `moc_tinh_han` (gửi hoặc Mở lại gần nhất), người phụ trách, `da_leo_thang_cho_han` (giá trị hạn đã leo thang: một lần mỗi hạn), cách đóng. Nội dung, trả lời, hướng xử lý, ghi chú ở `phan_hoi_muc` (nhóm 3, loại `NOI_DUNG`, `TRA_LOI`, `HUONG_XU_LY`, `GHI_CHU`, `GIAO_LAI`, `DOI_MUC`).
- Người phụ trách mặc định theo FR-059 (Trưởng tầng được giao qua `NhiemVuApi`; Hành chính là nhóm: phụ trách là "Hành chính" cho tới khi một người Nhận xử lý). Trả lời nhóm "sức khỏe, thuốc" xếp `SUC_KHOE`; người gửi không có quyền có tác dụng → cảnh báo lúc soạn, bản cho người gửi chỉ phần `CHUNG`.
- Hạn = `moc_tinh_han` + CFG-M10-01 theo mức (đọc lúc tính); đổi mức tính lại; hạn mới đã qua → leo thang ngay.
- Job theo phút: quá hạn chưa Đã phản hồi → dấu quá hạn, thông báo Quản lý viện Trung bình một lần (SC-005 ≤ 5 phút); Đã phản hồi quá CFG-M10-02 → `DONG` cách "tự đóng".
- `GanPhamViProvider` `M10.PHAN_HOI` theo người cao tuổi; Quản lý viện xem mọi phản hồi.

---

## F-14. Giao diện cho module làm plan sau và SPI cho module thấp hơn

**Quyết định**:
- **Đồng bộ hợp đồng 002**: `QuanHeNguoiThanApi` được khai báo trong `m15.taikhoan.api` dưới dạng SPI, M10 cài (như `HoSoNhanVienApi` theo plan 008); 001, 006, 011 dùng cùng khai báo đó. Chữ ký hợp nhất ở [contracts/module-interfaces.md](contracts/module-interfaces.md) mục 1.1.
- `m10.api.QuyenNguoiThanApi` (013, 014, 016, 017, 018): quan hệ Hiệu lực, quyền có tác dụng, người đại diện, liên hệ chính, người được phép đón tại thời điểm, giấy tờ tùy thân (013 xác minh người nhận), `xemDoGui`.
- `m10.api.HoatDongBanTinPort` (014 cài; chỉ đọc): số hoạt động đã tham gia trong kỳ. Bean thiếu → "không có dữ liệu trong kỳ".
- Sự kiện `m10.api`: `QuanHeDaDoi`, `QuyenNguoiThanDaDoi` (002 xóa đệm quyền, 009, 013), `LuotThamDaVao/DaRa` (016), `PhanHoiQuaHan` (016), `BanTinDaGui` (016).

---

## F-15. Thông báo (FR-069)

**Quyết định**: Mức, người nhận, loại thông tin theo bảng FR-069 (đủ mức). Mọi thông báo cài `CoYeuCauThongBao` (009 B-02), khóa `(bản ghi nguồn, loại sự kiện[, lần nhắc])`.

**Đối tượng thông tin** (bổ sung 2026-10-18; 009 FR-001a, FR-054, Q-353; plan, điểm báo lại 7): yêu cầu có người nhận là người thân khai:

| Thông báo (FR-069) | Đối tượng |
| --- | --- |
| Bản tin; bản tin đính chính | `VE_NGUOI_CAO_TUOI` (009 chỉ gửi người trong phạm vi "nhận thông báo") |
| Yêu cầu thay đổi quyền cần xác nhận/duyệt, có kết quả, đã có hiệu lực (Q-249); đặt/thôi người đại diện; nhắc quá mốc; ngoại lệ đón cần xác nhận | `VE_VIEC_NGUOI_THAN` |
| Lượt thăm bị hủy (Hành chính, người cao tuổi vắng, khoanh vùng); lượt ở lại được xác nhận hoặc từ chối; phản hồi đã được trả lời | `VE_VIEC_NGUOI_THAN` |

Không thông báo nào của 012 cần mã Q-354. Nội dung rút gọn cho tin nhắn không có phần sức khỏe (FR-070) — 009 chọn phần `CHUNG`. "Trưởng tầng" luôn là Trưởng tầng được giao qua `NhiemVuApi.truongTangDuocGiao`; không có thì Quản lý viện (Quy ước của spec).

---

## F-16. Quyền (FR-067)

**Quyết định**:

| `ChucNang` | Thao tác |
| --- | --- |
| `NGUOI_THAN_QUAN_HE` | `THUC_HIEN` (Hành chính); `DUYET` (Quản lý viện: đặt/thôi người đại diện); `XEM` (Quản lý viện) |
| `YEU_CAU_QUYEN` | `LAP` (Hành chính; người thân theo FR-014); `XAC_NHAN` (người đại diện, chú thích ²); `DUYET` (Quản lý viện) |
| `QUY_TRINH_DON` | `THUC_HIEN` theo khóa của lệnh nguồn (F-07); `DUYET` ngoại lệ (Quản lý viện); `XAC_NHAN` ngoại lệ (người đại diện) |
| `LUOT_THAM` | `DANG_KY` (Hành chính; người thân có `dang_ky_tham`); `VAO_RA` (Hành chính); `XEM` (Quản lý viện, Trưởng tầng) |
| `LUOT_O_LAI` | `LAP` (Hành chính); `XAC_NHAN` (Trưởng tầng được giao; Quản lý viện khi tầng không có); `BAT_DAU_KET_THUC` (Hành chính, Trưởng tầng) |
| `PHAN_HOI` | `GUI` (người thân, Hành chính); `XU_LY` (Trưởng tầng, Điều dưỡng, Hành chính trong phạm vi); `DUYET` (Quản lý viện: xem mọi, giao lại) |
| `BAN_TIN` | `THUC_HIEN` (Điều dưỡng có phạm vi); `DUYET_THAY` (Trưởng tầng được giao sau CFG-M10-12) |
| `NHAT_KY_XEM_SUC_KHOE` | `XEM` (Quản lý viện) |

---

## F-17. Bộ lập lịch, lưu giữ, nhật ký, kiểm thử

**Quyết định**:
- **Job**: `ViecTheoPhutM10` (bản ghi đón hết hiệu lực; ngoại lệ hết hạn; lượt thăm Không đến; nhắc ghi giờ ra; phản hồi quá hạn, tự đóng); `ViecHangGioM10` (nhắc yêu cầu Chờ xác nhận CFG-M10-11; mốc 48 giờ và CFG-M10-12 của bản tin); `ViecNuaDemM10` 00:00 (đêm ở lại); `ViecBanTinM10` theo CFG-M10-03 (nhắc lập, Không gửi, báo Quản lý viện). Cột "đã làm lúc" chống trùng (NFR-04); chạy bù khi khởi động.
- **Lưu giữ**: quan hệ, yêu cầu, bản ghi đón, lượt ở lại, bản tin, phản hồi, nhật ký lượt xem: `THEO_HO_SO` (CFG-M15-03); hồ sơ người thân giữ khi còn quan hệ được giữ.
- **Nhật ký** (FR-068): mọi lệnh; số giấy tờ tùy thân là trường `CA_NHAN` (che với vai trò không cần).
- **Kiểm thử**:
  - test bảng `QuyenCoTacDung`, `KiemTraDbr02`, `KiemTraYeuCau`, `KiemTraDangKyTham`, `SoDem`, hàm hạn phản hồi, hàm người duyệt bản tin;
  - SC-001: bộ 100 lượt đón; SC-008: bộ 30 ngày ở lại so tính tay; SC-010: kiểm bất biến DBR-02 sau mọi kịch bản;
  - Test tích hợp MySQL: `UNIQUE` liên hệ chính, yêu cầu chờ, bản tin mỗi kỳ, đêm ở lại, một lần dùng bản ghi đón; trigger nhóm 3;
  - test đồng thời: 20 đăng ký cho chỗ cuối; hai người đại diện xác nhận cùng lúc; yêu cầu bỏ người đón cùng lúc với ghi đón;
  - đồng hồ giả lập cho CFG-M10-01 → 12, CFG-M01-04;
  - bản giả `HoatDongBanTinPort` và API của 005, 006, 007, 010, 011.
- Tên test theo BR/FR (`BR_M10_07_YeuCauQuyenIT`, `DBR_02_LienHeChinhIT`).

---

## F-06a. Mong muốn về khách thăm, người đón và yêu cầu trái mong muốn *(bổ sung 2026-10-18; FR-020a → FR-020c; Q-347, Q-348, Q-351)*

**Quyết định**:
- **Bảng** `mong_muon_nct` (nhóm 2; data-model mục 3a): người được nói tới là quan hệ (`quan_he_id`) hoặc người ngoài (họ tên, số giấy tờ); loại `THAM | DON`; `muon BOOLEAN`; nguồn theo mẫu 001 (`NguonYKienApi.kiemTra`); người ghi, thời điểm; `HIEU_LUC | DUOC_THAY_THE`. Tối đa một mong muốn Hiệu lực mỗi (người cao tuổi, người được nói tới, loại): cột sinh `UNIQUE`. Ghi mới cho cùng khóa thì bản cũ chuyển Được thay thế trong cùng transaction; nội dung không sửa (trigger).
- **Lệnh**: `POST /nguoi-cao-tuoi/{nctId}/mong-muon` (Hành chính, Trưởng tầng, Điều dưỡng có phạm vi; khóa quyền `MONG_MUON_NCT` theo dòng Phụ lục 27 "Mong muốn về khách thăm, người đón": TAO cho HC, TT, ĐD; XEM cho QL; Q-364); `GET` trả danh sách Hiệu lực và lịch sử. Người thân không có lệnh này.
- **Kiểm trái mong muốn** trong `KiemTraYeuCau` (lúc lập, lúc xác nhận, lúc duyệt, và cho từng dòng phiếu): yêu cầu bật `dang_ky_tham` / `duoc_phep_don` cho người có mong muốn Hiệu lực nguồn `NGUOI_CAO_TUOI` `muon = false` cùng loại, hoặc tắt hai quyền đó của người có mong muốn `muon = true`, là "trái mong muốn". Khi đó: không có hiệu lực bằng cổng hay bản ký; xác nhận của người đại diện bị từ chối 409 `CAN_QUAN_LY_DUYET_TRAI_MONG_MUON`; Quản lý viện duyệt phải có `lyDo` và `traoDoiVoiNguoiCaoTuoi`. Ngoại lệ: tắt `duoc_phep_don` vẫn áp ngay (FR-017). Yêu cầu do người đại diện theo pháp luật lập (`BanDongYApi.daiDienPhapLuat(nctId)` của 001 trả đúng người lập) không bị coi là trái (FR-020c); mong muốn vẫn hiển thị.
- **Phiếu đăng ký** (FR-008): dòng trái mong muốn tạo yêu cầu `CHO_XAC_NHAN` (đánh dấu `trai_mong_muon_id`) thay vì `HIEU_LUC`; dòng khác vẫn Hiệu lực. Thay quy tắc cũ "một quyền không đạt → cả phiếu bị chặn" chỉ cho trường hợp trái mong muốn; lỗi khác vẫn chặn cả phiếu.
- **Cảnh báo** (FR-020b câu cuối): `GET /nguoi-cao-tuoi/{nctId}/quan-he` và màn mong muốn trả `quyenTraiMongMuon` tính lúc đọc cho Hành chính, Trưởng tầng. Không tự đổi quyền, không job (Q-307).
- **Nhận ra cùng người**: theo `quan_he_id`, hoặc số giấy tờ của hồ sơ người thân trùng số giấy tờ của mong muốn (plan, điểm báo lại 8).

**Lý do**: FR-020b đặt điều kiện trên đường đi tới Hiệu lực của yêu cầu sẵn có; đặt trong `KiemTraYeuCau` giữ một chỗ kiểm cho mọi đường (cổng, quầy, phiếu).

**Phương án đã cân nhắc**: Tự tắt quyền khi ghi mong muốn "không muốn": trái Q-307 và FR-020b; bị loại.
