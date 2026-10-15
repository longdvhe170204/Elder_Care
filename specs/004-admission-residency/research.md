# Research: Tiếp nhận và lưu trú (004)

**Ngày**: 2026-10-07 | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

Mỗi mục gồm Quyết định, Lý do, Phương án đã cân nhắc. Quyết định nền R-01 → R-14 (000), A-01 → A-14 (002), H-01 → H-15 (001), G-01 → G-16 (003) được dẫn lại, không quyết lại. Không mục nào đổi nghiệp vụ (constitution IX); chỗ spec hoặc nguồn còn thiếu ghi ở "Điểm cần báo lại" của plan.

---

## T-01. Vị trí trong backend

**Quyết định**: Feature 004 là module M02 (mục 6). Gói `vn.eldercare.m02` có các gói con:

| Gói | Nội dung | Mục spec |
| --- | --- | --- |
| `dangky` | Lượt đăng ký, danh sách điều kiện tiếp nhận, tiến trình | A |
| `danhsachcho` | Hồ sơ chờ, điểm ưu tiên, liên hệ giường | B |
| `hopdong` | Hợp đồng, phiên bản nội dung, duyệt điều khoản | C, D |
| `dichvu` | Dịch vụ, phiên bản đơn giá | E |
| `thaydoi` | Yêu cầu thay đổi lưu trú, phụ lục | F |
| `vang` | Lượt vắng, ngày vắng, quyết định giữ giường, bán trú (báo vắng, ngày nghỉ) | G |
| `ketthuc` | Hồ sơ kết thúc lưu trú, bàn giao, ngoại lệ | H |
| `quadoi` | Bản ghi qua đời, danh sách việc sau qua đời | I |
| `tinhphi` | Dữ liệu ngày lưu trú cho 010 | J |
| `lich` | Job hằng ngày (T-11) | K |
| `api` | Giao diện, DTO, sự kiện, SPI công khai | |

Một bảng `luu_tru_nct` (một dòng mỗi hồ sơ người cao tuổi) là đích khóa tuần tự cho mọi lệnh của 004 trên cùng người và giữ dấu "hồ sơ lưu trú đã đóng" (FR-072).

**Lý do**: Constitution X. Mỗi hồ sơ người cao tuổi là một lần lưu trú, vì người quay lại có hồ sơ mới (001 FR-004), nên dấu "đã đóng" đặt theo hồ sơ là đúng nghĩa FR-072.

---

## T-02. Chiều phụ thuộc giữa module

**Quyết định**: Theo mẫu của 001 H-02 và 003 G-02: module ở tầng thấp hơn khai báo SPI trong gói `api` của mình, module kia cài.

| Cặp | Chiều mã | Cách giữ một chiều |
| --- | --- | --- |
| 004 ↔ 001 | 004 → 001 | 004 cài `ThamGiaChuyenTrangThai` của 001 và gọi `MucChamSocApi`, `DanhGiaApi`, `TrangThaiNguoiCaoTuoiApi`. Các giao diện 001 cần từ 004 (`YeuCauDoiMucApi`, `HopDongApi`, `LuotVangApi`) chuyển thành SPI khai báo ở `m01.api`, M02 cài; chữ ký giữ như plan 001 |
| 004 ↔ 002 | 004 → M15 | `KetThucLuuTruApi` mà 002 cần được khai báo ở `m15.taikhoan.api` dưới dạng SPI, M02 cài |
| 004 ↔ 003 | 004 → 003 | 004 gọi `PhanBoGiuongLenhApi`, `SucChuaBanTruApi`, `ViTriApi`, `PhongChoPhepApi`; 004 cài `NguonLuuTruChoPhanBo`, `DemLichDenBanTru` của 003 |
| 004 ↔ 010, 017, 012, 005 | Các module này → 004 | 010 đọc đơn giá, nội dung hợp đồng, ngày lưu trú; 017 đọc hợp đồng; 012 hiện hợp đồng trên cổng; 005 đọc lịch đến. 004 cần lại một ít dữ liệu của họ qua SPI khai báo ở `m02.api`: `KyCuoiPort` (010), `TrangThaiDatCocPort` (017), `NguoiDaiDienPort`, `BanGhiDonPort` (012), `TrangThaiCoMatBanTruPort` (005), `MucViecSauQuaDoiPort` (010, 013, 006, 007, 017) |
| 004 → 009, 011, 018 | Sự kiện | 004 phát sự kiện sau commit (T-12) |

Điều kiện kết thúc lưu trú (a) → (d), (h) không đi qua SPI của 004: 010, 013, 006, 007, 017 đã cài bean `ThamGiaChuyenTrangThai` cho lệnh `KET_THUC_LUU_TRU` của 001 (001 H-02). 004 hiển thị danh sách bằng `TrangThaiNguoiCaoTuoiApi.xemDieuKien` (T-09).

**Lý do**: Modulith `verify()` từ chối vòng phụ thuộc (000 R-02). Plan 001 đã ghi "001 cần từ 004" mà chưa chọn chiều; chọn SPI ở `m01.api` giữ chữ ký đã thiết kế.

**Phương án đã cân nhắc**: Sự kiện sau commit cho mọi tương tác: không đáp ứng "trong cùng một lần" của FR-002, FR-065, FR-070, FR-066 (1).

---

## T-03. Khóa và đồng thời

**Quyết định**:
- **Thứ tự khóa toàn hệ thống** (nối tiếp 003 G-03): `nguoi_cao_tuoi` (001, chỉ khi lệnh đi qua 001), rồi `luu_tru_nct` (004), rồi dòng nghiệp vụ của 004 (hợp đồng, hồ sơ chờ, lượt vắng, hồ sơ kết thúc), rồi `vi_tri_nct` và `giuong` (003).
- **Lệnh hồ sơ chờ dẫn tới Hủy tiếp nhận** (FR-002: Từ chối, Hủy chờ): lệnh của 004 không khóa hồ sơ chờ trước. Nó gọi `TrangThaiNguoiCaoTuoiApi.thucHien(HUY_TIEP_NHAN, chiTiet = {hoSoChoId, lenhHoSoCho})`; 001 khóa người cao tuổi rồi gọi bean của 004, và bean thực hiện chuyển hồ sơ chờ sang trạng thái được yêu cầu. Một đường duy nhất giữ đúng thứ tự khóa. Khi Hủy tiếp nhận đến từ 001, bean thấy không có `lenhHoSoCho` nên chuyển hồ sơ chờ sang Hủy chờ mà không gọi lại 001 (FR-002 câu cuối). Việc này cần thêm 004 vào bảng người gọi của `TrangThaiNguoiCaoTuoiApi.thucHien` ở hợp đồng 001.
- **Lệnh có nhiều lệnh đến gần đồng thời** (Edge Case cuối; duyệt hai yêu cầu, hai người cùng "Đã liên hệ"): khóa dòng rồi kiểm tra lại trên trạng thái mới, không dùng `If-Match`, giống 001 H-02, 003 G-03. `If-Match` chỉ dùng cho lệnh sửa nội dung (sửa hợp đồng Nháp, cập nhật lượt đăng ký, danh mục).
- **Deadlock** giữa "Ghi nhận đã liên hệ" (khóa hồ sơ chờ rồi giường) và phân bổ của 003 (khóa giường rồi gọi `daPhanBo` khóa hồ sơ chờ) là hiếm. MySQL phát hiện, và lệnh thua được thử lại tối đa hai lần như 003 G-03.

**Lý do**: Spec đòi lệnh đến sau được kiểm tra lại trên trạng thái mới, không bị từ chối vì xung đột phiên bản.

---

## T-04. Hợp đồng và nội dung hiệu lực tại ngày D (mục C, FR-019 → FR-029, BR-M02-09)

**Quyết định**:
- **Bản nháp**: `hop_dong` (đầu hợp đồng) cùng các bảng con `hop_dong_dich_vu`, `hop_dong_chinh_sach_vang` (dòng ghi đè), `hop_dong_lich_den` (bán trú). Chỉ sửa được khi `NHAP` và không mang dấu "đang chờ duyệt điều khoản"; trigger chặn `UPDATE` nội dung ở trạng thái khác.
- **Phiên bản nội dung**: khi "Ghi nhận đã ký", hệ thống ghi phiên bản 0 vào `phien_ban_noi_dung_hop_dong` (nhóm 3, chỉ thêm). Phiên bản gồm toàn bộ nội dung ở dạng JSON cùng các cột hay đọc: loại lưu trú, mức, ngày kết thúc, loại phòng, khoản cọc. Đơn giá được ghi lại tại lúc ký (FR-036). Mỗi phụ lục áp dụng thêm một phiên bản, `hieu_luc_tu` là ngày hiệu lực thực tế.
- **Nội dung tại ngày D** (FR-024) là phiên bản có `hieu_luc_tu` lớn nhất mà không muộn hơn D. Không cần ghép phụ lục mỗi lần đọc.
- Phụ lục luôn được áp dụng theo thứ tự ngày hiệu lực. Ngày hiệu lực thực tế là `max(mong muốn, ngày duyệt)` và việc áp dụng chạy vào đúng ngày đó, theo thứ tự ngày (000 R-10). Vì vậy phiên bản mới luôn dựng từ phiên bản mới nhất. FR-046 chặn hai yêu cầu cùng thuộc tính chờ hiệu lực cùng lúc.
- **Không chồng thời gian** (DBR-06, FR-022):
  - `hop_dong.ngay_ket_thuc_hien_hanh` là bản sao của ngày kết thúc ở phiên bản mới nhất; chỉ handler áp dụng phụ lục ghi cột này;
  - lệnh "Ghi nhận đã ký" và lệnh duyệt yêu cầu đổi thời gian khóa `luu_tru_nct`, rồi kiểm không có hợp đồng `HIEU_LUC` khác giao khoảng `[ngay_bat_dau, ngay_ket_thuc_hien_hanh]` (gồm cả hai đầu; ngày kết thúc rỗng coi là vô hạn);
  - trigger `trg_hop_dong_khong_chong` là lưới an toàn khi mã có lỗi.
- **Số hợp đồng** `HD-<năm>-<id 6 chữ số>`, cùng cách cấp mã như 001 H-04.
- **Điều khoản khác chuẩn** (FR-025a): hàm `XacDinhKhacChuan` so giá với phiên bản đơn giá tại ngày bắt đầu, và so dòng ghi đè với CFG-M02-05.
  - Lệnh "Gửi duyệt điều khoản" lập yêu cầu `M02.DUYET_DIEU_KHOAN_HOP_DONG`, lưu `bam_noi_dung` (SHA-256 của nội dung nháp chuẩn hóa) vào yêu cầu và hợp đồng.
  - "Gửi ký" chỉ qua khi có yêu cầu Đã áp dụng cùng bản băm với nội dung hiện tại. Nội dung đổi thì băm lệch và phải duyệt lại.
- **Dịch vụ chăm sóc theo mức** (FR-025, Q-297): khi "Gửi ký", hợp đồng phải có đúng một dòng dịch vụ loại `CHAM_SOC` và mức của dòng đó trùng mức của hợp đồng.
- **Ghi nhận đã ký** (FR-021):
  - người đại diện kiểm qua `NguoiDaiDienPort.laNguoiDaiDienHieuLuc` (012, DBR-02);
  - bản scan qua `TepApi` (001 H-13);
  - nội trú: `ViTriApi` (003) trả phân bổ đang hiệu lực hoặc tương lai bắt đầu không muộn hơn ngày bắt đầu hợp đồng;
  - bán trú: kiểm sức chứa khu nghỉ cho lịch đến (T-10).
- **Quá hạn hợp đồng** (FR-029): cột `dau_qua_han_tu`, `da_bao_qlv_qua_han_luc`; job T-11 đặt và gỡ.

**Lý do**: Phiên bản nội dung chỉ thêm cho 010 và 003 đọc "nội dung tại ngày D" bằng một truy vấn có chỉ mục, và giữ nguyên văn điều khoản đã áp dụng làm căn cứ đối chiếu với gia đình (SC-002).

**Phương án đã cân nhắc**: Ghép hợp đồng gốc và các phụ lục mỗi lần đọc: đúng nhưng mọi nơi đọc đều phải cài cùng logic ghép, dễ lệch.

---

## T-05. Dịch vụ và phiên bản đơn giá (mục E, FR-034 → FR-036, DBR-08)

**Quyết định**:
- `dich_vu` (nhóm 1) có loại: `CHAM_SOC` (kèm `ma_muc`, tối đa một dịch vụ Hiệu lực mỗi mức qua cột sinh `UNIQUE`), `PHI_LUU_TRU` (kèm `loai_phong_id` của 003), `BUOI_BAN_TRU`, `PHUC_HOI`, `HOAT_DONG`, `DUA_DI_KHAM`, `TIEM`, `NGUOI_THAN_O_LAI`, `SUAT_AN`, `THUOC_NGUON_VIEN` (kèm `thuoc_id` của 006), `KHAC`. Mỗi dịch vụ có `ky_tinh` (`THANG`, `NGAY`, `BUOI`, `LAN`, `DON_VI`).
- `phien_ban_don_gia` theo mẫu 000 mục 1.2:
  - khóa dòng `dich_vu`, kiểm `NOT EXISTS` khoảng giao, rồi thêm;
  - trigger `trg_phien_ban_don_gia_khong_chong` là lưới an toàn;
  - tạo phiên bản mới thì đặt `hieu_luc_den` của phiên bản đang mở là ngày liền trước. Đây là lệnh "đóng khoảng", được phép kể cả khi phiên bản đã được tham chiếu, vì không đổi giá hay ngày bắt đầu;
  - phiên bản hiệu lực lùi (FR-035) chỉ cho khoảng chưa có phiên bản. Kết thúc của nó bị cắt ở ngày liền trước phiên bản kế tiếp; không có khoảng trống thì bị chặn.
- **"Đã được tham chiếu"** là cột `da_tham_chieu`. Hợp đồng ký đặt cột này; 010 đặt qua `DonGiaApi.donGiaTaiNgay(…, danhDauThamChieu = true)` trong transaction lập khoản. Trigger chặn sửa `don_gia`, `hieu_luc_tu` khi `da_tham_chieu`.
- **Hợp đồng lệch phiên bản mới** (FR-036): `GET /dich-vu/{id}/hop-dong-lech-gia` liệt kê hợp đồng Hiệu lực có đơn giá đã ghi khác phiên bản mới. Hệ thống không đổi gì trên hợp đồng.
- **So đơn giá khi chuyển giường** (`NguonLuuTruChoPhanBo.khacDonGia` cho 003 FR-030): so giá dịch vụ `PHI_LUU_TRU` của hai loại phòng trong phiên bản hiệu lực tại ngày chuyển.

**Lý do**: DBR-08, BR-M11-02. Một danh mục chung cho phí lưu trú, dịch vụ, thuốc nguồn viện để 010 lấy giá ở một chỗ (FR-034).

---

## T-06. Yêu cầu thay đổi lưu trú và phụ lục (mục F, FR-037 → FR-047)

**Quyết định**:
- **Một loại yêu cầu** `M02.THAY_DOI_LUU_TRU` (000) với nội dung JSON gồm tập loại thay đổi (loại lưu trú, mức, dịch vụ, thời gian, giường, lịch đến), giá trị trước (hệ thống điền từ phiên bản nội dung hiện hành), giá trị sau, ảnh hưởng chi phí (hệ thống tính), tham chiếu (lần đánh giá, lệnh chuyển giường). Chuyển mở rộng `DUOC_THAY_THE` theo plan 001.
- **Người lập** (FR-039): Hành chính; Bác sĩ chỉ loại đổi mức; người đại diện tạo ở Nháp qua cổng (kiểm `NguoiDaiDienPort`), Hành chính được báo để hoàn thiện. Hệ thống không tự lập. Không có job nhắc ở 004: việc nhắc lập yêu cầu đổi mức là job `NhacLapYeuCauDoiMuc` của 001 (dùng `coYeuCauDoiMucChuaKetThuc` do 004 cài), việc nhắc sau chuyển gấp là job của 003. Khi Hành chính lập yêu cầu tham chiếu một lệnh chuyển giường, 004 gọi `PhanBoGiuongLenhApi.daLapYeuCauSauChuyenGap` trong cùng transaction.
- **Thay đổi nhỏ** *(bổ sung 2026-10-11, FR-043, Q-338)*: `thayDoiNho` là số liệu tính lúc đọc, không lưu trong nội dung JSON (sửa 2026-10-11 sau audit). Lý do: nội dung khóa từ Chờ duyệt (000 FR-033) và SPI của 000 không có điểm gắn lúc gửi duyệt; nếu điền lúc lập, sửa Nháp thì hợp đồng đổi giữa lần sửa cuối và lúc gửi duyệt làm dấu lưu sai, và lệnh `sua-noi-dung` chung của 000 có thể ghi đè trường này. Tính lúc đọc trên hợp đồng hiện hành nên dấu hiển thị ở mọi thời điểm (gồm lúc gửi duyệt, FR-043) luôn trùng với kết quả kiểm lúc duyệt. Hàm thuần `LaThayDoiNho(noiDung, noiDungHopDongHienHanh)` đúng khi tập loại thay đổi chỉ có `DICH_VU`, mọi dịch vụ được thêm hoặc bớt mang dấu thuộc gói (`hop_dong_dich_vu.thuoc_goi`, FR-019) và ảnh hưởng chi phí bằng 0 ở mọi khoản. Handler cài `gioiHanVaiTroDuyet(yc)` (000, bổ sung theo plan 004): tính lại `LaThayDoiNho` trên hợp đồng hiện hành; đúng thì trả rỗng (Quản lý viện, Hành chính đều duyệt được theo Phụ lục 27 ô D và D⁴²), sai thì trả `{QUAN_LY_VIEN}`. Kiểm lại lúc duyệt là bước "kiểm tra lại khi duyệt" của FR-043: dấu lúc gửi là đúng mà nay sai thì Hành chính bị chặn. 000 dùng cùng giới hạn cho `lenhDuocPhep`, nhắc và người nhận thông báo, nên Hành chính không được nhắc với yêu cầu không phải thay đổi nhỏ.
- **Lúc duyệt** (hai hàm bổ sung vào `LoaiYeuCauHandler` theo plan 003):
  - `kiemTraKhiDuyet` chạy FR-041, FR-046 (`UNIQUE` theo cột sinh trên bảng chỉ mục `yeu_cau_thay_doi_thuoc_tinh`), FR-022 với ngày kết thúc mới, FR-040. Vi phạm thì 409 liệt kê đủ;
  - `khiDuyet` tạo `phu_luc_hop_dong` ở `CHO_HIEU_LUC` (DBR-07), gọi `datTruocChuyenTheoLich` của 003 khi có đổi giường, và giữ chỗ sức chứa khi có lịch đến mới.
- **Áp dụng** (`apDung`, job 000 vào ngày hiệu lực, trong một transaction):
  - thêm phiên bản nội dung;
  - đổi mức qua `MucChamSocApi.apDungThayDoi` (001), đổi dịch vụ chăm sóc theo mức mới (Q-297);
  - chuyển giường qua `thucHienChuyenTheoLich` (003);
  - gia hạn nội trú ngắn ngày qua `giaHanKetThucDuKien` (003), cập nhật `ngay_ket_thuc_hien_hanh`, gỡ dấu quá hạn;
  - phụ lục sang `DA_AP_DUNG`;
  - phát `PhuLucDaApDung` cho 010, 018 (FR-033b), 005 (báo điều dưỡng phụ trách xem xét kế hoạch, Q-306), 011.
- **`kiemTraDieuKienApDung`** chạy FR-045: hợp đồng gốc còn Hiệu lực, người cao tuổi chưa ở trạng thái cuối, điều kiện FR-041 còn thỏa (đổi giường qua `kiemTraChuyenTheoLich` của 003).
- **Áp dụng không thành, Hủy**: 004 nghe `YeuCauApDungKhongThanh`, `YeuCauDaHuy` (000), chuyển phụ lục `KHONG_AP_DUNG` và gọi `huyPhanBoTheoLich` (003). Listener idempotent.
- **Trạng thái cuối** (FR-065, FR-070): bean của 004 đóng mọi yêu cầu đang mở của người đó trong cùng lệnh. Nháp và Chờ duyệt chuyển Hủy; Đã duyệt chuyển Áp dụng không thành. Việc này cần hàm `YeuCauPheDuyetApi.dongDoTrangThaiCuoi(yeuCauId, canCu)` (MANDATORY), bổ sung vào hợp đồng 000. Hàm cũng dùng cho các loại `M02.DIEU_CHINH_DIEM`, `M02.NGOAI_LE_KET_THUC` và loại `M03.NHAP_BU_PHAN_BO` của 003.
- Phụ lục không có endpoint sửa hay xóa (FR-047).

**Lý do**: Tái dùng vòng đời và job áp dụng đúng một lần của 000 (R-10). Tạo phụ lục và giữ giường trong transaction duyệt bảo đảm FR-042 "khi duyệt thì tạo phụ lục" mà không có khoảng hở.

**Phương án đã cân nhắc**: Mỗi loại thay đổi là một loại yêu cầu riêng: spec cho một yêu cầu có nhiều loại thay đổi (FR-037), tách ra thì một thay đổi gộp (đổi mức kèm đổi giường) không còn áp dụng được trong cùng một lần.

---

## T-07. Danh sách chờ và điểm ưu tiên (mục B, FR-007 → FR-018)

**Quyết định**:
- **Điểm** = thành phần mức (CFG-M02-08 theo mức của FR-008) + thời gian chờ (`floor(soNgayCho / doDaiChuKy) * diemMoiChuKy`) + tổng tình huống + tổng điều chỉnh đã duyệt. Hàm thuần `TinhDiemUuTien` có test đơn vị theo FR-009 (ví dụ 15 ngày, chu kỳ 7 thì 2 điểm).
- **Lưu**: `ho_so_cho.diem_hien_hanh` cùng `diem_chi_tiet` (JSON). Mỗi lần điểm đổi thêm một dòng `lich_su_diem` (nhóm 3: điểm cũ, mới, thành phần, nguyên nhân).
- **Tính lại** khi: job hằng ngày (FR-010); `MucChamSocDaDoi` (001); lệnh cập nhật tình huống; điều chỉnh được áp dụng; `ThamSoDaDoi(CFG-M02-08)`, chạy cho mọi hồ sơ trong lần tính kế tiếp (US2 #2).
- **Điều chỉnh điểm** là loại yêu cầu `M02.DIEU_CHINH_DIEM`; `apDung` thêm dòng điều chỉnh và tính lại. Hồ sơ đang Đã liên hệ vẫn giữ giường (US2 #11).
- **Danh sách phù hợp với một giường** (FR-012):
  - thuộc tính phòng (chính sách giới tính, mức cho phép, loại hình phục vụ, đang chịu chặn cách ly) lấy từ `PhongChoPhepApi.thuocTinhPhanBo(giuongId)` của 003; giới tính từ 001;
  - "lượt trống hiện tại" bắt đầu ở `TinhTrangGiuongApi.batDauLuotTrong(giuongId)` của 003;
  - hồ sơ đã liên hệ, từ chối hay hết hạn với chính giường này từ mốc đó bị loại, theo bảng `lien_he_giuong` (nhóm 3);
  - sắp theo điểm giảm dần, rồi ngày đăng ký, rồi thời điểm tạo hồ sơ.

  Hai hàm `thuocTinhPhanBo`, `batDauLuotTrong` được bổ sung vào hợp đồng 003 (plan, mục "Đồng bộ").
- **Báo Hành chính** (FR-013, SC-003): nghe `GiuongDaTrong` của 003 (003 đã loại giường có phân bổ tương lai hoặc mang dấu), tính danh sách, phát `GiuongTrongCanChonHoSo` kèm số hồ sơ phù hợp hoặc "không có người phù hợp". Sự kiện xử lý sau commit qua outbox, nên thông báo tới trong vài giây, dưới chỉ tiêu 5 phút.
- **Ghi nhận đã liên hệ** (FR-014):
  - khóa hồ sơ chờ, tính lại danh sách phù hợp tại lúc lệnh;
  - chọn hồ sơ không đứng đầu (sau khi bỏ các hồ sơ đã liên hệ, từ chối, hết hạn trong lượt) thì bắt buộc `lyDoKhongTheoThuTu`;
  - gọi `giuChoHoSoCho` (003); một hồ sơ chỉ giữ một giường, bảo đảm bằng cột sinh `UNIQUE`.
- **Hết hạn giữ chỗ** do job `HetHanGiuCho` của 003 (003 G-09). 004 cài `hoSoChoHetHanGiuCho` để đưa hồ sơ về Đang chờ, ghi dòng `lien_he_giuong` kết quả `HET_HAN` và phát `GiuongTrongCanChonHoSo` cho giường đó (FR-015). 004 không có job riêng cho việc này.
- **Phân bổ giường** (FR-016): `daPhanBo(nctId, phanBoId)` do 003 gọi trong transaction phân bổ. Hồ sơ chuyển Đã tiếp nhận, lượt đăng ký về Đang xử lý; 003 tự trả giường đang giữ khác về Trống.
- **"Từng lưu trú tại viện"** (FR-007): khi tạo hồ sơ chờ, nếu `NguoiCaoTuoiApi` cho biết có hồ sơ trước ở Kết thúc lưu trú, tình huống được đánh dấu sẵn ở trạng thái "chờ xác nhận". Hành chính xác nhận hoặc bỏ kèm lý do. Đây là đánh dấu do nguồn quy định (FR-007, Q-275), không phải gợi ý.

---

## T-08. Lượt vắng và chính sách phí khi vắng (mục G, FR-048 → FR-061)

**Quyết định**:
- **Mở lượt vắng**: bean của 004 cho `CHO_TAM_VANG`, `CHUYEN_VIEN` (`@Order` đầu tiên):
  - `kiemTraChiTiet` kiểm nội dung lệnh: loại vắng, dự kiến trở lại, người đón, người bàn giao, có mang thuốc, lý do chuẩn "về nhà theo nguyện vọng cuối đời";
  - `kiemTraDieuKien` kiểm bản ghi đón qua `BanGhiDonPort` (012) với Tạm vắng;
  - `thucHienTacDong` mở lượt vắng, tra dòng chính sách ngày 1 để biết giữ giường. 003 hỏi lại qua `NguonLuuTruChoPhanBo.giuGiuongKhiVang`;
  - Tạm vắng chuyển sang bệnh viện (FR-057) thì đóng lượt cũ và mở lượt Bệnh viện cùng thời điểm, số thứ tự ngày bắt đầu lại; ngày chuyển thuộc lượt mới.
- **Tra chính sách** `TraChinhSachVang`, hàm thuần:
  - đầu vào: loại lưu trú (phiên bản nội dung tại ngày), loại vắng, số thứ tự ngày, dòng ghi đè của hợp đồng, bảng CFG-M02-05 tại thời điểm tính;
  - đầu ra: hệ số, giữ giường, nguồn dòng, dấu;
  - thứ tự: dòng hợp đồng, rồi dòng bảng cơ sở; "Theo hợp đồng" mà hợp đồng không có dòng thì áp FR-053; không có dòng thì 100%, giữ giường, dấu "không có dòng chính sách" (FR-053); "Quản lý quyết định" thì hệ số của dòng liền trước, hoặc 100% nếu là ngày 1, kèm dấu "chờ quyết định" (FR-056, BR-M02-17).
- **Ngày vắng** (FR-052, FR-058): job hằng ngày ghi một dòng `ngay_vang` cho mỗi ngày dương lịch có phần vắng, với bảng hiện hành tại lúc tính. Dòng đã tính không đổi khi bảng đổi. Dòng chỉ được tính lại khi có quyết định giữ giường hoặc đính chính; mỗi lần tính lại ghi nhật ký và phát `NgayLuuTruDaDoi` cho 010.
- **Ngưỡng giữ giường** (FR-055): ngày đầu tiên có giữ giường "Không" hoặc "Quản lý quyết định" trong khi giường đang giữ.
  - Tới ngưỡng thì phát `CanQuyetDinhGiuGiuong` cho Quản lý viện một lần, cột `da_bao_nguong_luc`. Không tạo yêu cầu (Q-307).
  - Dòng ngưỡng là "Không": quy tắc FR-055 cũng áp dụng. Giường vẫn giữ tới khi Quản lý viện quyết, không tự giải phóng (003 FR-015).
- **Quyết định giữ giường** `POST /luot-vang/{id}/quyet-dinh-giu-giuong` (AC-01; Phụ lục 27 ô T⁸ của Quản lý viện, sửa 2026-10-11 theo Q-345: đây là lệnh, không phải quyền duyệt): thêm bản ghi `quyet_dinh_giu_giuong` (nhóm 3), tính lại các ngày "chờ quyết định".
  - "Giải phóng" gọi `giaiPhongGiuongKhiVang` (003) trong cùng transaction.
  - "Giữ tiếp" lưu ngày xem xét lại; job báo lại Quản lý viện vào ngày đó.
  - Lượt vắng đã đóng (đính chính vượt ngưỡng, FR-061): quyết định chỉ đặt hệ số, không gọi 003.
- **Gia hạn dự kiến trở lại**: `POST /luot-vang/{id}/gia-han-du-kien` có lý do, thêm dòng `lich_su_du_kien_tro_lai`, phát `LuotVangDaGiaHan` (001 tính lại mốc quá hạn qua `LuotVangApi`; 011 tính suất).
- **Đóng lượt vắng**: bean cho `GHI_NHAN_TRO_VE`, `KET_THUC_LUU_TRU`, `GHI_NHAN_QUA_DOI`. Lượt đã đóng bất biến (trigger). Sai sót đi đường đính chính `M02.LUOT_VANG`; `sauKhiDinhChinh` tính lại các ngày bị ảnh hưởng (FR-061, BR-M02-18).
- **Bán trú** (FR-060, FR-060a):
  - báo vắng `POST /bao-vang-ban-tru` (nhóm 3) lưu thời điểm gia đình báo; hệ thống tự xác định "báo trước đủ CFG-M02-06";
  - trạng thái có mặt lấy từ `TrangThaiCoMatBanTruPort` (005); hệ số tra dòng bán trú của CFG-M02-05;
  - ngày khu bán trú nghỉ `ngay_nghi_ban_tru` (nhóm 2, `UNIQUE` ngày đang hiệu lực); khai báo cho ngày đã qua bắt buộc lý do; phát `NgayNghiBanTruDaDoi` cho 005, 010, 011.

---

## T-09. Danh sách điều kiện tiếp nhận và kết thúc lưu trú (FR-004, FR-063 → FR-068)

**Quyết định**:
- **Điều kiện tiếp nhận** `GET /luot-dang-ky/{id}/dieu-kien-tiep-nhan` dựng đủ sáu điều kiện với Đạt / Chưa đạt / Không áp dụng và căn cứ (sửa 2026-10-11, Q-343):
  - (a) `DanhGiaApi.xacNhanGanNhat` (001) và CFG-M01-02;
  - (b) hợp đồng Hiệu lực, thời điểm tiếp nhận không sớm hơn ngày bắt đầu (FR-075);
  - (c) `TrangThaiDatCocPort` (017);
  - (d) `ViTriApi` (003), "Không áp dụng" với bán trú;
  - (e) mức của phiên bản nội dung hiện hành so với `MucChamSocApi.hienHanh`;
  - (f) đúng một người liên hệ chính và ít nhất một người đại diện có quan hệ Hiệu lực (DBR-02): lấy kết quả điều kiện `DBR_02` do bean 012 trả trong `xemDieuKien` của 001; 004 không đọc dữ liệu người thân và không thêm hàm vào `NguoiDaiDienPort`.

  Bean `ThamGiaChuyenTrangThai` của 004 cho `HOAN_TAT_TIEP_NHAN` kiểm (b), (c), (e) bằng cùng các hàm. (a) do 001, (d) do 003, (f) do 012 kiểm (bean `ThamGiaChuyenTrangThai` của 012, `coTheMien = false`). Test `FR_004_DanhSachKhopLenhIT` so danh sách với kết quả `xemDieuKien` của 001 trên cùng dữ liệu.
- **Tiến trình** (FR-003) suy từ dữ liệu, không lưu: đánh giá (001), hợp đồng, dịch vụ, điều kiện.
- **Điều kiện kết thúc** `GET /ho-so-ket-thuc/{id}/dieu-kien` hiển thị (a) → (h) theo mã cố định:

  | Mã | Điều kiện | Ai cài bean |
  | --- | --- | --- |
  | `KT_A_CHI_PHI` | (a) chi phí kỳ cuối đã chốt | 010 |
  | `KT_B_DO_GUI` | (b) đồ gửi | 013 |
  | `KT_C_THUOC_GUI` | (c) thuốc gia đình gửi | 006 |
  | `KT_D_CANH_BAO_SU_CO` | (d) cảnh báo, sự cố mở | 007 |
  | `KT_E_BAN_GIAO` | (e) bàn giao người cao tuổi | 004 |
  | `KT_H_QUYET_TOAN` | (h) số dư và tiền cọc | 017 |

  Điều kiện Đạt khi không có trong kết quả `xemDieuKien(nctId, KET_THUC_LUU_TRU)` của 001. "Đạt (ngoại lệ)" khi có ngoại lệ đã duyệt. (f), (g) hiển thị "Tự động". Plan của 006, 007, 010, 013, 017 phải dùng đúng mã này (module-interfaces mục 2.1).
- **Bean của 004 cho `KET_THUC_LUU_TRU`**:
  - điều kiện: có hồ sơ kết thúc Đang chuẩn bị; (e) đã có bàn giao; không mang dấu "quá ngày dự kiến"; thời điểm hiệu lực nằm trong ngày kết thúc dự kiến (FR-066, FR-066a);
  - miễn điều kiện: `mienDieuKien` trả ngoại lệ đã duyệt (loại `M02.NGOAI_LE_KET_THUC`), chỉ cho (a) → (d), (h). (e) không có ngoại lệ (FR-064).
- **Tác động của `KET_THUC_LUU_TRU`** (FR-065): hồ sơ kết thúc sang Hoàn tất; hợp đồng Kết thúc hoặc Chấm dứt (so ngày hiệu lực với ngày kết thúc hiện hành, FR-027); đóng lượt vắng đang mở; hồ sơ chờ sang Hủy chờ; đóng yêu cầu mở (T-06); đặt `luu_tru_nct.da_dong_luc` (FR-072); phát sự kiện cho 010, 017, 018.
- **Lập hồ sơ kết thúc** (FR-062, FR-066 (1)):
  - Hành chính, hoặc Bác sĩ với Xuất viện, Chuyển cơ sở vì lý do y tế; Chuyển cơ sở bắt buộc nơi chuyển đến;
  - trong cùng transaction gọi `KyCuoiPort.taoKyCuoiNhap(nctId, ngayKetThucDuKien)` (010). Đây là tác động kèm lệnh, được phép theo Q-306;
  - đổi ngày gọi `KyCuoiPort.tinhLai`; hủy gọi `KyCuoiPort.huyMocDung`.
- **Bàn giao** `POST /ho-so-ket-thuc/{id}/ban-giao` (nhóm 3): người nhận (người thân hoặc cơ sở tiếp nhận), thời điểm, nhân viên bàn giao.
- **Ngoại lệ** là loại yêu cầu `M02.NGOAI_LE_KET_THUC`, lập bởi Hành chính, hoặc Kế toán với (h); `apDung` thêm dòng `ngoai_le_dieu_kien`.

**Lý do**: Điều kiện (a) → (d), (h) đã thuộc bean của module sở hữu (001 H-02). Đọc lại qua 001 tránh 004 phụ thuộc năm module và giữ một nguồn kết quả cho cả lệnh lẫn màn hình.

---

## T-10. Lịch đến bán trú và sức chứa

**Quyết định**:
- **Lịch đến** là nội dung hợp đồng (`hop_dong_lich_den`: thứ trong tuần hoặc ngày cụ thể, giờ đến, giờ về). `CHECK (gio_ve > gio_den)` bảo đảm không qua 00:00 (FR-019, Q-286).
- **Kiểm sức chứa** (003 FR-039) ở hai lệnh làm lịch có hiệu lực ràng buộc:
  - "Ghi nhận đã ký" hợp đồng bán trú;
  - `kiemTraKhiDuyet` của yêu cầu đổi lịch đến hoặc đổi sang bán trú (FR-041).

  Quy trình theo 003 G-11: `SucChuaBanTruApi.khoa()`, rồi `buoiBiChiem`, rồi đếm trước và sau, rồi `kiemTraSucChua`.
- **Số đăng ký** của một (ngày, buổi) = số lịch đến trong phiên bản nội dung hiệu lực tại ngày đó (hợp đồng Hiệu lực), cộng lịch của phụ lục Chờ hiệu lực có ngày hiệu lực không muộn hơn ngày đó, trừ người có báo vắng đủ hạn (Vắng có báo). 004 cài `DemLichDenBanTru` của 003 theo cách đếm này.

**Lý do**: Spec 003 nêu "khi đăng ký hoặc thay đổi lịch đến" mà không nói lệnh nào của 004 là "đăng ký". Plan chọn hai lệnh làm lịch ràng buộc với gia đình; báo lại ở plan, điểm 4.

---

## T-11. Bộ lập lịch của 004 (FR-077, BR-M02-19)

**Quyết định**: Một job `ViecHangNgayM02` chạy 00:15 hằng ngày và khi khởi động, có ShedLock. Job xử lý từng ngày D từ ngày kế tiếp ngày đã xong gần nhất tới hôm nay, theo thứ tự ngày (chạy bù). Bảng `lan_chay_theo_ngay (viec, ngay)` có khóa chính hai cột, nên mỗi việc của mỗi ngày chỉ chạy một lần. Các việc:

| Việc | FR | Ghi chú chạy bù |
| --- | --- | --- |
| `TINH_DIEM` | FR-010 | Chỉ ngày mới nhất có ý nghĩa; ngày cũ ghi "bỏ qua" |
| `NGAY_VANG` | FR-052, FR-055, FR-056 | Mỗi ngày bị lỡ được tra và ghi; ngưỡng, ngày xem xét lại xét theo ngày đó |
| `HOP_DONG` | FR-026, FR-029 | Dấu "quá hạn hợp đồng" đặt với `dau_qua_han_tu` là ngày lẽ ra phải đặt; báo Quản lý viện khi quá CFG-M02-10 |
| `KET_THUC` | FR-066a | Dấu "quá ngày dự kiến" với đúng ngày |
| `NHAC` | FR-017, FR-021, FR-026, FR-029, FR-066a, FR-072 (CFG-M02-09), FR-076 | Nhắc bị lỡ gộp thành một lần ở ngày mới nhất |

- Tham số đọc giá trị hiện hành tại lúc chạy (000 FR-018).
- Nhắc theo chu kỳ dùng bảng `nhac_m02 (loai, doi_tuong_id, lan_nhac_cuoi)` như 001, 003.
- Job chỉ chuyển dấu theo thời gian, tính ngày vắng, nhắc, báo; không tạo yêu cầu, chi phí hay hồ sơ (000 FR-010a).

**Lý do**: NFR-04 và FR-077 đòi chạy lại không trùng và chạy bù đúng ngày.

---

## T-12. Sự kiện và thông báo

**Quyết định**: Sự kiện phát sau commit qua outbox (000 R-09). Mức thông báo: FR-025b là Trung bình (Q-302); thông báo qua đời Khẩn cấp do 001, 009 gửi (Q-97); các thông báo khác theo nguyên tắc 3 bậc như 001 H-14, 003 G-12:

| Mức | Thông báo |
| --- | --- |
| Trung bình | lệch mức hợp đồng (FR-025b); cần quyết định giữ giường (FR-055); hợp đồng quá hạn vượt CFG-M02-10 (FR-029); đặt cọc về Chưa đáp ứng khi đã lưu trú (FR-032); không có dòng chính sách (FR-053) |
| Nhẹ | giường Trống cần chọn hồ sơ (FR-013); các lần nhắc FR-017, FR-021, FR-026, FR-029, FR-066a, FR-072, FR-076; yêu cầu do người đại diện tạo (FR-039); ngày xem xét lại giữ giường |

Sự kiện không gửi thông báo, dành cho module khác: `HopDongDaDoiTrangThai`, `KhoanCocDaDoi` (017, FR-033a); `PhuLucDaApDung` (010, 018, 005, 011); `LuotVangDaMo`, `LuotVangDaGiaHan`, `LuotVangDaDong` (001, 006, 011); `NgayLuuTruDaDoi` (010); `HoSoKetThucDaLap`, `…DaDoiNgay`, `…DaHuy`, `…HoanTat` (010, 017, 018, 012); `QuaDoiDaGhiNhan` (017, 018); `NgayNghiBanTruDaDoi`, `BaoVangBanTruDaGhi` (005, 010, 011); `TiepNhanHoanTat` (018, FR-033b).

---

## T-13. Qua đời (mục I, FR-069 → FR-073)

**Quyết định**:
- **Bean của 004 cho `GHI_NHAN_QUA_DOI`**:
  - `kiemTraChiTiet` kiểm thời điểm, địa điểm, người phát hiện, người xác nhận, nguyên nhân (tùy chọn), tệp bằng chứng (bắt buộc khi Hành chính thực hiện; quyền theo 001 FR-047b);
  - `thucHienTacDong` thêm `ban_ghi_qua_doi` (nhóm 3); hợp đồng Chấm dứt "Qua đời"; đóng lượt vắng tại thời điểm qua đời; hồ sơ kết thúc đang mở sang Đã hủy và chép các điều kiện chưa đạt sang danh sách việc; hồ sơ chờ sang Hủy chờ; đóng yêu cầu mở; tạo `muc_viec_sau_qua_doi` cho năm mục (FR-071).
- **Trạng thái từng mục** lấy từ `MucViecSauQuaDoiPort` (010, 013, 006, 007, 017 cài; trả `HOAN_THANH`, `CHUA_HOAN_THANH`, `KHONG_AP_DUNG` kèm căn cứ). Hệ thống tính lại khi:
  - mở màn danh sách;
  - job nhắc chạy (CFG-M02-09);
  - module sở hữu gọi `DanhSachViecSauQuaDoiApi.capNhat(nctId)` sau một lệnh của họ.

  Mọi mục xong thì đặt `luu_tru_nct.da_dong_luc` và dừng nhắc (FR-072). Dấu không gỡ được sau đó (FR-072 câu cuối).
- **Giới hạn trường** (FR-069, BR-M02-21): `nguyenNhan` mang `@NhomTruong(SUC_KHOE)`. Hành chính và Kế toán không xem được, kể cả người đã ghi; người thân xem theo chú thích ¹. Bằng chứng do Hành chính tải lên thì người tải xem được (`QuyenTepProvider`).
- **Bổ sung nguyên nhân** đi đường đính chính `M02.BAN_GHI_QUA_DOI`.

---

## T-14. Quyền

**Quyết định**: Khóa quyền theo dòng Phụ lục 27 (002 A-02):

| Lệnh | Khóa | Thu hẹp ở 004 |
| --- | --- | --- |
| Lượt đăng ký, hợp đồng | `TIEP_NHAN_HOP_DONG_DAT_COC, TAO/SUA` | Chỉ AC-09 (²⁶); Kế toán chỉ thu cọc ở 017 |
| Duyệt điều khoản khác chuẩn | `TIEP_NHAN_HOP_DONG_DAT_COC, DUYET` | AC-01 (⁶) |
| Danh sách chờ | `DANH_SACH_CHO_DIEM_UU_TIEN, TAO` / `DUYET` | Hành chính / Quản lý viện; vai trò khác không xem (FR-018) |
| Yêu cầu thay đổi lưu trú | `YEU_CAU_THAY_DOI_LUU_TRU, TAO` / `DUYET` | Bác sĩ chỉ loại đổi mức; người thân chỉ người đại diện (²), tạo ở Nháp; `DUYET` của Hành chính (D⁴²) chỉ với thay đổi nhỏ, kiểm qua `gioiHanVaiTroDuyet` (FR-043, Q-338) |
| Gia hạn dự kiến trở lại | `TAM_VANG_TRO_VE, TAO` | Hành chính, Trưởng tầng (phạm vi) |
| Quyết định giữ giường | `TAM_VANG_TRO_VE, TAO` | Chỉ AC-01 (⁸), kiểm thêm `TaiKhoanApi.coVaiTro(QUAN_LY_VIEN)` trong service (chú thích ⁸ chỉ mô tả, U2); Trưởng tầng, Hành chính có T ở dòng này nhưng không được lệnh này (sửa 2026-10-11, Q-345: Phụ lục 27 đổi D⁸ thành T⁸) |
| Báo vắng bán trú, ngày nghỉ khu bán trú | `TAM_VANG_TRO_VE, TAO` | Hành chính |
| Hồ sơ kết thúc, bàn giao | `KET_THUC_LUU_TRU_QUA_DOI, TAO` | Hành chính; Bác sĩ chỉ trường hợp Xuất viện, Chuyển cơ sở |
| Ngoại lệ kết thúc | `KET_THUC_LUU_TRU_QUA_DOI, TAO` / `DUYET` | Thêm Kế toán với (h) (Q-222, 19.3) |
| Dịch vụ, đơn giá | `DICH_VU_DON_GIA, TAO/SUA` | AC-01 |

Nội dung tài chính của hợp đồng (FR-028) mang `@NhomTruong(TAI_CHINH)` (nhóm có sẵn ở 002 A-08). Chỉ Quản lý viện, Hành chính, Kế toán (19.3) và người đại diện (⁷) xem được. Loại lưu trú, thời hạn, mức, dịch vụ là trường công khai nội bộ.

---

## T-15. Lưu giữ, nhật ký

**Quyết định**:
- Mọi bảng gắn hồ sơ người cao tuổi (lượt đăng ký, hồ sơ chờ, hợp đồng và phiên bản, phụ lục, lượt vắng, ngày vắng, quyết định, hồ sơ kết thúc, bàn giao, qua đời, danh sách việc) đăng ký `LuuTruProvider` loại `THEO_HO_SO`.
- Dịch vụ, phiên bản đơn giá là danh mục, không bị loại bỏ.
- `ngay_nghi_ban_tru` theo `KHONG_GAN_HO_SO`.
- Hành động nhật ký: `M02_DANG_KY_*`, `M02_HO_SO_CHO_*`, `M02_DIEM`, `M02_HOP_DONG_*`, `M02_PHU_LUC_*`, `M02_VANG_*`, `M02_KET_THUC_*`, `M02_QUA_DOI`, `M02_DICH_VU_*`, `M02_NHAC`, `M02_BAO`. Nhật ký ghi kèm dấu "áp dụng bù" và "chạy bù" khi có.

---

## T-16. Kiểm thử

**Quyết định** (theo 000 R-14):
- **Hàm thuần có test đơn vị bảng**: `TinhDiemUuTien`, `TraChinhSachVang` (phủ mọi dòng 6.7, ghi đè, "Theo hợp đồng", "Quản lý quyết định" ngày 1, không có dòng — SC-006), `XacDinhKhacChuan`, `NoiDungTaiNgay`.
- **Cơ sở dữ liệu** (Testcontainers): trigger chống chồng hợp đồng và phiên bản đơn giá; trigger khóa nội dung hợp đồng ngoài Nháp; bất biến lượt vắng đã đóng; cột sinh `UNIQUE`.
- **Đồng thời**: hai lệnh "Ghi nhận đã ký" chồng thời gian; hai lệnh duyệt yêu cầu cùng thuộc tính; hai Hành chính cùng "Đã liên hệ" (SC-001).
- **Đồng hồ giả lập**: 30 ngày điểm ưu tiên (SC-004); lượt vắng 35 ngày; quá hạn hợp đồng; quá ngày dự kiến; chạy bù khi bỏ lỡ 3 ngày (FR-077).
- **Bản giả** cho các SPI 004 khai báo (010, 017, 012, 005 và năm bên cài `MucViecSauQuaDoiPort`), và cho bean `ThamGiaChuyenTrangThai` của 006, 007, 010, 013, 017 khi kiểm danh sách kết thúc. Công tắc qua `/api/v1/kiem-thu/gia-lap/{module}` như 001, 003.
- **Client**: Vitest cho danh sách điều kiện tiếp nhận và kết thúc (Đạt, Chưa đạt, Không áp dụng, ngoại lệ), danh sách hồ sơ phù hợp (không chọn sẵn, bắt buộc lý do khi bỏ qua thứ tự), màn hợp đồng ẩn trường tài chính. `flutter_test` cho Cho tạm vắng và Gia hạn ở app nhân viên (Trưởng tầng).
