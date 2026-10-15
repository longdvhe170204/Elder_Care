# Data Model: Nền tảng quy tắc nghiệp vụ dùng chung (000)

**Ngày**: 2026-10-06 | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu này mô tả thực thể, trường, ràng buộc và cách bảo đảm từng DBR. Đây không phải script SQL; migration Flyway được viết ở giai đoạn triển khai theo mô tả này.

Quy ước kiểu dữ liệu:
- `id`: `BIGINT` tự tăng, trừ khi ghi khác.
- `thời điểm`: `DATETIME(6)` theo UTC (research R-04).
- `ngày`: `DATE` theo Asia/Ho_Chi_Minh.
- `JSON`: kiểu JSON của MySQL.
- Tên bảng và cột: tiếng Việt không dấu, snake_case.

---

## 1. Quy ước chung cho mọi module

Spec 000 mục A quy định khung cho mọi loại dữ liệu. Plan của từng module áp các quy ước dưới đây cho bảng của mình.

### 1.1. Nhóm 1 – Danh mục (FR-002 → FR-004)

| Cột chuẩn | Kiểu | Ý nghĩa |
| --- | --- | --- |
| `trang_thai` | `ENUM('HIEU_LUC','NGUNG_HIEU_LUC')` | Mặc định `HIEU_LUC` |
| `ten` | `VARCHAR` | Tên hiển thị |
| `ten_hieu_luc` | Cột sinh: `IF(trang_thai='HIEU_LUC', ten, NULL)` | Có `UNIQUE` trong cùng danh mục, nên kích hoạt lại một mục trùng tên mục đang Hiệu lực bị từ chối (FR-003, BR-M15-06) |
| `phien_ban` | `INT` | Khóa lạc quan |

Các ràng buộc:
- **Tham chiếu và xóa (FR-002)**: mọi bảng nhóm 2, 3 tham chiếu danh mục bằng khóa ngoại `ON DELETE RESTRICT`. Lệnh xóa vi phạm khóa ngoại được ánh xạ thành 409 `DANH_MUC_DA_DUOC_THAM_CHIEU`, và mục đó chỉ còn lệnh Ngừng hiệu lực. Bảng `nhat_ky` không có khóa ngoại, nên nhật ký của chính mục không tính là tham chiếu (CHK001).
- **Ngừng hiệu lực (FR-003)**: dữ liệu mới chỉ được chọn mục `HIEU_LUC`. Kiểm tra ở backend, trong lệnh của module dùng danh mục. Bản ghi cũ vẫn trỏ khóa ngoại nên hiển thị đúng.
- **Thuộc tính theo phiên bản (FR-004)**: tách thành bảng phiên bản theo mẫu 1.2, ví dụ phiên bản đơn giá (DBR-08, do plan 004 hoặc 010 mô tả).

### 1.2. Nhóm 2 – Nghiệp vụ có trạng thái (FR-005 → FR-008)

| Cột chuẩn | Ý nghĩa |
| --- | --- |
| `trang_thai` | Mã trạng thái; chỉ đổi qua lệnh nghiệp vụ |
| `phien_ban` | Khóa lạc quan, đi với `If-Match` |
| `tao_luc`, `tao_boi` | Thời điểm và người tạo |

- Mỗi lệnh ghi người thực hiện, thời điểm, lý do vào `nhat_ky` (FR-007). Không cần cột "lý do cuối" trên bảng.
- **Phiên bản có hiệu lực (FR-008, BR-M15-07)**:
  - bảng phiên bản có `doi_tuong_id`, `hieu_luc_tu DATE`, `hieu_luc_den DATE NULL` (gồm cả ngày cuối) và `CHECK (hieu_luc_den IS NULL OR hieu_luc_den >= hieu_luc_tu)`;
  - MySQL không có ràng buộc loại trừ khoảng, nên việc không chồng khoảng được kiểm ở backend. Backend khóa dòng cha (`SELECT … FOR UPDATE` trên đối tượng), kiểm `NOT EXISTS` phiên bản giao khoảng, rồi mới thêm;
  - test tích hợp chạy đồng thời hai luồng để chứng minh.

### 1.3. Nhóm 3 – Ghi nhận đã xác nhận (FR-009, FR-010)

| Cột chuẩn | Ý nghĩa |
| --- | --- |
| `thoi_diem` | Thời điểm máy chủ nhận bản ghi (FR-046) |
| `thoi_diem_thiet_bi` | `NULL` nếu ghi trực tuyến; có giá trị nếu đồng bộ từ ngoại tuyến (DBR-25) |
| `nguoi_thuc_hien_id` | Người ghi gốc, dùng cho FR-026 (a) |
| `thoi_diem_xac_nhan` | Chỉ có ở bảng có bản nháp; `NULL` khi còn nháp |
| `tang_khu_vuc_id` | Có khi loại bản ghi "gắn tầng/khu vực" (FR-026 b, CHK051) |

Bản ghi không có cột "đã đính chính" hay "đã hủy". Hai thông tin này suy ra từ `dinh_chinh` (mục 4).

### 1.4. Quy ước bảo đảm ở cơ sở dữ liệu

| Bảo đảm | Cách làm | DBR / FR |
| --- | --- | --- |
| Không sửa bản ghi nhóm 3 đã xác nhận | Trigger `trg_<bang>_khong_sua` `BEFORE UPDATE`: `SIGNAL SQLSTATE '45000'` khi `OLD.thoi_diem_xac_nhan IS NOT NULL`. Bảng không có nháp thì tài khoản `eldercare_app` không có quyền `UPDATE` | DBR-23, FR-009 |
| Không xóa nhóm 3 và nhật ký | Trigger `trg_<bang>_khong_xoa` `BEFORE DELETE`: `SIGNAL` trừ khi `SUBSTRING_INDEX(CURRENT_USER(),'@',1) = 'eldercare_luu_tru'`. Tài khoản `eldercare_app` không có quyền `DELETE` trên các bảng này | FR-009, FR-048, FR-051 |
| Danh sách bảo vệ BR-M15-03 | Cùng hai trigger trên, bắt buộc với: liều thuốc đã xác nhận, sự cố khẩn cấp, chi phí đã chốt, bàn giao đã xác nhận, bàn giao đồ gửi. Test tích hợp riêng cho từng bảng ở plan module sở hữu | FR-010 |
| Danh sách bảng nhóm 3 | ArchUnit đọc `@NhomDuLieu(GHI_NHAN)`. Một test tích hợp đối chiếu danh sách này với `information_schema.TRIGGERS`; bảng nhóm 3 thiếu trigger thì test lỗi | FR-001 |

---

## 2. THAM_SO và LICH_SU_THAM_SO (mục B, FR-011 → FR-022)

### `tham_so` – nhóm 1 (mã cố định, không tạo, xóa qua nghiệp vụ)

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `ma` | `VARCHAR(16)` | Khóa chính, ví dụ `CFG-M15-05` | Phụ lục 25 |
| `mo_ta` | `VARCHAR(500)` | | Phụ lục 25 cột "Tham số" |
| `kieu` | `ENUM('THOI_LUONG','SO_NGUYEN','TY_LE','SO_TIEN','GIO_TRONG_NGAY','BANG')` | | R-07 |
| `don_vi` | `VARCHAR(16)` | Ví dụ `PHUT`, `GIO`, `NGAY`, `NAM`, `LAN`, `PHAN_TRAM` | |
| `rang_buoc` | `JSON` | Ví dụ `{"min":1}`, `{"lonHon":"CFG-M15-05"}` hoặc JSON Schema cho `BANG` | FR-015 |
| `gia_tri_mac_dinh` | `JSON` | | Phụ lục 25 cột "Mặc định" |
| `gia_tri_hien_hanh` | `JSON` | `NOT NULL` | DBR-24 |
| `dung_tai` | `VARCHAR(500)` | | Phụ lục 25 cột "Dùng tại" |
| `phien_ban` | `INT` | Khóa lạc quan; trả về dưới dạng ETag | FR-021 |
| `doi_luc`, `doi_boi` | | Lần đổi gần nhất | |

### `lich_su_tham_so` – nhóm 3, chỉ thêm

| Cột | Kiểu | Ràng buộc |
| --- | --- | --- |
| `id` | `BIGINT` | Khóa chính |
| `ma` | `VARCHAR(16)` | Khóa ngoại tới `tham_so.ma` |
| `gia_tri` | `JSON` | Toàn bộ giá trị, kể cả với tham số dạng bảng (FR-022) |
| `hieu_luc_tu` | `DATETIME(6)` | `UNIQUE (ma, hieu_luc_tu)` |
| `nguoi_thuc_hien_id` | `BIGINT` | Quản lý viện, hoặc Bộ lập lịch ở lần nạp ban đầu |
| `ly_do` | `VARCHAR(1000)` | `NOT NULL`, trừ lần nạp ban đầu (FR-014) |
| `la_khoi_phuc_mac_dinh` | `BOOLEAN` | FR-020 |

**Bảo đảm DBR-24**:
- Mỗi mã có đúng một dòng `tham_so`, nên có đúng một giá trị hiện hành.
- Lệnh đổi giá trị chạy trong một transaction: `UPDATE tham_so … WHERE ma=? AND phien_ban=?`, rồi `INSERT lich_su_tham_so`, rồi `INSERT nhat_ky`. Nếu `UPDATE` không sửa dòng nào thì trả 412 kèm giá trị hiện hành (FR-021).
- Giá trị tại thời điểm `t` lấy từ dòng lịch sử có `hieu_luc_tu` lớn nhất mà `<= t` (FR-019).

**Kiểm tra giá trị (FR-015)**: `KiemTraThamSo` đọc `kieu` và `rang_buoc`, rồi kiểm tham chiếu chéo. Ví dụ CFG-M15-06 > CFG-M15-05: đổi một mã thì kiểm lại ràng buộc của mã kia. Vi phạm thì trả 422 `GIA_TRI_KHONG_HOP_LE`.

**Nguồn dữ liệu nạp**: 118 mã ở Phụ lục 25. Giá trị mặc định chuyển sang kiểu có cấu trúc, ví dụ "48 giờ" thành `{"giaTri":48,"donVi":"GIO"}`, còn "5 lần / khóa 15 phút" thành một đối tượng hai trường. Việc chuyển đổi được rà tay trong task. Phụ lục 25 không ghi khoảng hợp lệ (xem plan, "Điểm cần báo lại").

---

## 3. YEU_CAU_PHE_DUYET (mục D, FR-031 → FR-041)

### `yeu_cau_phe_duyet` – nhóm 2

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `id` | `BIGINT` | Khóa chính | |
| `ma_hien_thi` | `VARCHAR(20)` | `UNIQUE`, ví dụ `YC-2026-000123` | Căn cứ ở nhật ký (FR-045) |
| `loai` | `VARCHAR(40)` | Mã loại do module đăng ký, ví dụ `M02.THAY_DOI_LUU_TRU`, `M15.DINH_CHINH` | FR-031 |
| `trang_thai` | `VARCHAR(30)` | Theo bảng chuyển bên dưới, hoặc trạng thái loại đó khai báo thêm | 6.6 |
| `noi_dung` | `JSON` | Khóa khi rời Nháp (FR-033) | FR-032 |
| `doi_tuong_loai`, `doi_tuong_id` | | Đối tượng chính, dùng cho phạm vi duyệt | FR-034 |
| `nguoi_cao_tuoi_id` | `BIGINT NULL` | Để lưu giữ theo hồ sơ (BR-M15-12) | FR-050 |
| `tang_khu_vuc_id` | `BIGINT NULL` | Phạm vi duyệt theo tầng | |
| `nguoi_yeu_cau_id` | `BIGINT` | | |
| `tao_luc` | `DATETIME(6)` | | |
| `ly_do` | `VARCHAR(2000)` | `NOT NULL` | |
| `ngay_hieu_luc_mong_muon` | `DATE` | | |
| `ngay_hieu_luc_thuc_te` | `DATE NULL` | Đặt khi duyệt: `max(mong muốn, ngày duyệt)` | FR-037 |
| `thoi_diem_gui_duyet` | `DATETIME(6) NULL` | Gốc tính CFG-M15-05/06 | FR-031a |
| `nguoi_quyet_dinh_id`, `thoi_diem_quyet_dinh`, `y_kien_quyet_dinh` | | `y_kien` bắt buộc khi từ chối, hoặc khi tự duyệt | FR-035, FR-036 |
| `tu_duyet` | `BOOLEAN` | `nguoi_quyet_dinh_id = nguoi_yeu_cau_id` khi duyệt | FR-035 |
| `ly_do_ket_thuc` | `VARCHAR(2000) NULL` | Lý do hủy, hoặc danh sách điều kiện không thỏa khi Áp dụng không thành | FR-038 |
| `thoi_diem_ap_dung` | `DATETIME(6) NULL` | | |
| `ap_dung_bu` | `BOOLEAN` | `ngay_hieu_luc_thuc_te` < ngày áp dụng | FR-037a |
| `da_nhac_luc`, `da_bao_qlv_luc` | `DATETIME(6) NULL` | Chống nhắc lặp | FR-031a |
| `phien_ban` | `INT` | Khóa lạc quan | FR-040 |

Chỉ mục:
- `(trang_thai, ngay_hieu_luc_thuc_te, thoi_diem_quyet_dinh)` cho job áp dụng (BR-M15-10);
- `(trang_thai, thoi_diem_gui_duyet)` cho job nhắc;
- `(nguoi_yeu_cau_id, trang_thai)`;
- `(loai, trang_thai)`.

### `ket_qua_ap_dung` – nhóm 3

| Cột | Ràng buộc |
| --- | --- |
| `yeu_cau_phe_duyet_id` | Khóa chính, cũng là khóa ngoại. Một yêu cầu có tối đa một kết quả, nên tác động không thể được ghi hai lần (FR-037, SC-003) |
| `ket_qua_loai`, `ket_qua_id` | Tham chiếu bản ghi do module tạo, ví dụ phụ lục hợp đồng (FR-039, DBR-07) |
| `thoi_diem` | |

Bản ghi do module tạo cũng mang cột `yeu_cau_phe_duyet_id` (khóa ngoại) khi DBR yêu cầu, ví dụ DBR-07 với phụ lục hợp đồng.

### Bảng chuyển trạng thái chung

Cài đặt đúng bảng trạng thái của spec (FR-031). Mã trạng thái:

| Mã | Tên trong spec | Kết thúc |
| --- | --- | --- |
| `NHAP` | Nháp | |
| `CHO_DUYET` | Chờ duyệt | |
| `DA_DUYET` | Đã duyệt (chờ hiệu lực) | |
| `DA_AP_DUNG` | Đã áp dụng | ✔ |
| `AP_DUNG_KHONG_THANH` | Áp dụng không thành | ✔ |
| `TU_CHOI` | Từ chối | ✔ |
| `HUY` | Hủy | ✔ |

| Từ | Lệnh (endpoint) | Đến | Kiểm tra ở backend | Nhật ký (`hanh_dong`) |
| --- | --- | --- | --- | --- |
| — | `POST /yeu-cau-phe-duyet` | `NHAP` | `QuyenApi` với khóa quyền tạo của loại; `handler.kiemTraNoiDung`; có `lyDo` | `YC_LAP` |
| `NHAP` | `POST …/sua-noi-dung` | `NHAP` | Chỉ người yêu cầu; `If-Match`; có `lyDo` (FR-007, FR-033) | `YC_SUA_NHAP` |
| `NHAP` | `POST …/gui-duyet` | `CHO_DUYET` | Người yêu cầu; nội dung đủ | `YC_GUI_DUYET` |
| `NHAP`, `CHO_DUYET` | `POST …/huy` | `HUY` | Người yêu cầu; chưa có quyết định; có `lyDo` (BR-M15-08) | `YC_HUY` |
| `CHO_DUYET` | `POST …/duyet` | `DA_DUYET` | `QuyenApi` với khóa quyền duyệt và phạm vi, xét tại lúc này (FR-034a); tự duyệt thì `handler.choTuDuyet()` và bắt buộc `yKien` | `YC_DUYET` (+`tuDuyet`) |
| `CHO_DUYET` | `POST …/tu-choi` | `TU_CHOI` | Như trên; bắt buộc `yKien` | `YC_TU_CHOI` |
| `DA_DUYET` | `POST …/huy` | `HUY` | Người có quyền duyệt loại đó; có `lyDo` | `YC_HUY` |
| `DA_DUYET` | (hệ thống) áp dụng | `DA_AP_DUNG` | Ngày hiệu lực ≤ hôm nay; `handler.kiemTraDieuKienApDung` không trả lỗi | `YC_AP_DUNG` (+`apDungBu`) |
| `DA_DUYET` | (hệ thống) áp dụng | `AP_DUNG_KHONG_THANH` | Điều kiện không thỏa | `YC_AP_DUNG_KHONG_THANH` |
| `DA_DUYET` | (hệ thống) áp dụng lỗi | `DA_DUYET` | Ngoại lệ khi áp dụng, transaction rollback | `YC_AP_DUNG_LOI` (ghi bằng transaction riêng) |

Mỗi lệnh chuyển trạng thái kiểm `phien_ban`. Quyết định đến sau trên yêu cầu đã rời `CHO_DUYET` bị trả 409 `DA_DUOC_XU_LY` (FR-040).

Một loại yêu cầu có thể khai báo thêm trạng thái và chuyển (`LoaiYeuCauHandler.chuyenMoRong()`):
- `M02.THAY_DOI_LUU_TRU` (004): `DUOC_THAY_THE`, do hệ thống chuyển kèm lệnh xác nhận đánh giá lại của 001 qua `YeuCauPheDuyetApi.chuyenMoRong` (plan 001, research H-10);
- 015: `CHO_NGUOI_NHAN_DONG_Y`, `NGUOI_NHAN_TU_CHOI`, `DA_AP_DUNG_MOT_PHAN`, `HET_HIEU_LUC`; yêu cầu đổi ca không có `NHAP`.

Phần mở rộng không được bỏ các kiểm tra chung: lý do, nhật ký, khóa lạc quan, quyền tại lúc quyết định. Plan 001 và 015 chi tiết phần mở rộng.

Vòng đời riêng của 012 (Chờ xác nhận → Hiệu lực / Từ chối / Hủy), 010 (đề nghị mua hộ), 013, 017, 019 **không** dùng bảng này. Các module đó chỉ dùng lại `NhacQuaHan` (FR-031a) và khóa lạc quan (FR-040) qua `contracts/module-interfaces.md`.

---

## 4. DINH_CHINH (mục C, FR-023 → FR-030)

### `dinh_chinh` – nhóm 3, chỉ thêm

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `id` | `BIGINT` | Khóa chính | |
| `loai_ban_ghi` | `VARCHAR(40)` | Mã loại do module đăng ký provider | |
| `id_ban_ghi_goc` | `BIGINT` | Luôn là bản gốc ban đầu (DBR-23, FR-027) | DBR-23 |
| `kieu` | `ENUM('SUA_NOI_DUNG','HUY_GHI_NHAN')` | | FR-029 |
| `noi_dung_dung` | `JSON NULL` | `NOT NULL` khi `SUA_NOI_DUNG` (`CHECK`) | FR-024 |
| `ly_do` | `VARCHAR(2000)` | `NOT NULL`, `CHECK (CHAR_LENGTH(TRIM(ly_do)) > 0)` | FR-024 |
| `nguoi_thuc_hien_id` | `BIGINT` | Với cách (c): người lập yêu cầu | |
| `cach_tao` | `ENUM('A','B','C')` | | FR-026 |
| `yeu_cau_phe_duyet_id` | `BIGINT NULL` | Khóa ngoại; `NOT NULL` khi `cach_tao='C'` (`CHECK`) | FR-026 (c) |
| `thoi_diem` | `DATETIME(6)` | | |
| `so_thu_tu` | `INT` | `UNIQUE (loai_ban_ghi, id_ban_ghi_goc, so_thu_tu)`, nên thứ tự các lần đính chính xác định | FR-027, FR-028 |

`id_ban_ghi_goc` là khóa logic, không có khóa ngoại cứng vì bảng gốc thay đổi theo loại. Khi thêm, `DinhChinhService` gọi `provider.daXacNhan(id)` để kiểm bản gốc tồn tại và đã xác nhận (US2 kịch bản 8).

### `chuoi_dinh_chinh` – dữ liệu kỹ thuật, không thuộc nhóm nghiệp vụ

| Cột | Ý nghĩa |
| --- | --- |
| `loai_ban_ghi`, `id_ban_ghi_goc` | Khóa chính |
| `so_ban` | Số bản đính chính hiện có; bản mới lấy `so_ban + 1` làm `so_thu_tu` |
| `da_huy` | `true` khi đã có `HUY_GHI_NHAN`. Đính chính nội dung sau đó bị từ chối, trả 409 `BAN_GHI_DA_HUY_GHI_NHAN` (FR-029) |
| `phien_ban` | Khóa lạc quan, tuần tự hóa các lần đính chính đồng thời |

### View đọc giá trị hiện hành (FR-030)

- `v_dinh_chinh_moi_nhat (loai_ban_ghi, id_ban_ghi_goc, dinh_chinh_id, noi_dung_dung, thoi_diem)`: bản `SUA_NOI_DUNG` có `so_thu_tu` lớn nhất.
- `v_ban_ghi_da_huy (loai_ban_ghi, id_ban_ghi_goc, thoi_diem_huy)`: dùng để loại khỏi thống kê "từ thời điểm hủy về sau". Báo cáo đã xuất và kỳ đã chốt không đổi (FR-029).

Báo cáo và quy tắc tự động của module `LEFT JOIN` hai view này, hoặc gọi `DinhChinhApi.hienHanh` theo lô.

### Chọn cách tạo (FR-026, CHK033, CHK051)

| Bước | Điều kiện | Kết quả |
| --- | --- | --- |
| 1 | Người dùng là `nguoi_thuc_hien_id` của bản gốc và `provider.coQuyenGhiNhan(nguoiDung)` | Cách A: thêm trực tiếp |
| 2 | Bản gốc gắn tầng/khu vực và người dùng là người phụ trách ca hoặc trưởng tầng của tầng đó trong phạm vi hiện tại (`PhamViApi`) | Cách B: thêm trực tiếp |
| 2' | Như bước 2, người ghi gốc không còn quyền ghi nhận và tầng không có người phụ trách ca: người dùng là Trưởng tầng được giao | Cách B |
| 3 | Bản gốc không gắn tầng, hoặc gắn tầng nhưng không có ai theo bước 2 và 2': người dùng cùng vai trò với người ghi gốc và `coQuyenGhiNhan` | Cách C: tạo yêu cầu `M15.DINH_CHINH` ở `NHAP`. Lệnh gửi duyệt đi kèm, theo kịch bản US2 #4 ("tạo yêu cầu … ở Chờ duyệt"). Người duyệt là Quản lý viện; áp dụng thì thêm `dinh_chinh` với `cach_tao='C'` |
| — | Không thỏa bước nào | 403 `KHONG_DUOC_DINH_CHINH` |

Với bản ghi không gắn tầng, Trưởng tầng và người phụ trách ca không qua được bước 2. Họ chỉ qua bước 3 nếu cùng vai trò với người ghi gốc.

---

## 5. NHAT_KY (mục E, FR-042 → FR-051a)

### `nhat_ky` – nhóm 3, chỉ thêm, phân vùng theo năm

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `id` | `BIGINT` | Khóa chính `(id, thoi_diem)` | |
| `thoi_diem` | `DATETIME(6)` | Giờ máy chủ; khóa phân vùng | FR-046 |
| `thoi_diem_thiet_bi` | `DATETIME(6) NULL` | Bản ghi ngoại tuyến | DBR-25 |
| `nguoi_thuc_hien_loai` | `ENUM('NGUOI_DUNG','BO_LAP_LICH','KHOI_TAO')` | `KHOI_TAO` chỉ dùng cho lệnh khởi tạo Quản lý viện đầu tiên (BR-M15-22, plan 002) | FR-045 |
| `nguoi_thuc_hien_id` | `BIGINT NULL` | `NOT NULL` khi `NGUOI_DUNG` | |
| `vai_tro` | `VARCHAR(10)` | Vai trò tại lúc thực hiện (AC-xx) | 19.4 (xuất báo cáo) |
| `hanh_dong` | `VARCHAR(40)` | Ví dụ `YC_DUYET`, `THAM_SO_DOI`, `DINH_CHINH_THEM`, `DANG_NHAP_THAT_BAI`, `XUAT_BAO_CAO`, `LOAI_BO_DU_LIEU_HET_HAN` | |
| `doi_tuong_loai`, `doi_tuong_id` | `VARCHAR(40)`, `VARCHAR(64)` | | FR-044 |
| `nguoi_cao_tuoi_id` | `BIGINT NULL` | Để lưu giữ cùng hồ sơ (FR-050) | BR-M15-12 |
| `du_lieu_truoc`, `du_lieu_sau` | `JSON NULL` | Chỉ các trường đổi; rỗng với hành động không đổi dữ liệu | FR-043a |
| `ly_do` | `VARCHAR(2000) NULL` | Bắt buộc theo danh sách FR-044, kiểm ở backend | FR-044 |
| `can_cu` | `JSON NULL` | `{maQuyTac?, yeuCauPheDuyetId?, banGhiNguonId?, lanChayLichId?}`; ít nhất một khóa khi `BO_LAP_LICH` | FR-045 |
| `chi_tiet` | `JSON NULL` | Phần riêng: bộ lọc xuất báo cáo, dấu `tuDuyet`, `apDungBu`, `ngoaiPhamVi` (BR-M15-01) | FR-043a |
| `khong_loai_bo` | `BOOLEAN` | `true` với bản tóm tắt đợt loại bỏ | FR-051 |

Chỉ mục: `(doi_tuong_loai, doi_tuong_id, thoi_diem)`, `(nguoi_thuc_hien_id, thoi_diem)`, `(hanh_dong, thoi_diem)`, `(nguoi_cao_tuoi_id)`.

Bảo đảm:
- **Nguyên tử với thay đổi (FR-047)**: cùng transaction (R-06).
- **Chỉ ghi thêm (FR-048)**: không có quyền `UPDATE`, `DELETE` cho `eldercare_app`, cộng trigger mục 1.4.
- **Lý do bắt buộc (FR-044, SC-008)**: `BanGhiNhatKy.builder()` báo lỗi khi `hanh_dong` thuộc tập bắt buộc lý do mà `ly_do` rỗng. Lệnh nghiệp vụ trả 422 `THIEU_LY_DO` trước khi chạm dữ liệu.
- **Xem và che (FR-049, BR-M15-11)**: theo R-06.

---

## 6. Bảng kỹ thuật dùng chung

| Bảng | Mục đích | Ghi chú |
| --- | --- | --- |
| `lan_chay_lich` | Định danh lần chạy job (FR-045): `id`, `job`, `bat_dau`, `ket_thuc`, `ket_qua`, `so_xu_ly` | Dữ liệu kỹ thuật |
| `khoa_lap_lai` | `Idempotency-Key`: `(khoa, nguoi_dung_id)` là khóa chính, cùng `ma_bam_yeu_cau`, `ket_qua_json`, `het_han_luc` | Giữ 7 ngày (kỹ thuật, không phải CFG) |
| `event_publication` | Event Publication Registry của Spring Modulith (R-09) | Do thư viện quản lý |
| `shedlock` | Khóa job (R-10) | Do thư viện quản lý |

Các bảng kỹ thuật không thuộc ba nhóm nghiệp vụ, nên không ghi nhật ký nghiệp vụ.

---

## 7. Bảng truy vết DBR, BR do feature 000 bảo đảm

| Mã | Bảo đảm bằng | Nơi kiểm thử |
| --- | --- | --- |
| DBR-23 (nhật ký cho mọi thay đổi) | Ghi trong cùng transaction (R-06); proxy đếm câu lệnh | `NhatKyDayDuIT` |
| DBR-23 (đính chính trỏ đúng một gốc) | Cột `id_ban_ghi_goc NOT NULL`; luôn là bản gốc ban đầu; `provider.daXacNhan` | `DinhChinhIT` |
| DBR-24 | Khóa chính `tham_so.ma`; `UNIQUE (ma, hieu_luc_tu)` | `ThamSoIT` |
| DBR-25 | `Clock` duy nhất (ArchUnit); `thoi_diem_thiet_bi` | `ThoiGianArchTest`, `NhatKyNgoaiTuyenIT` |
| BR-M15-03 | Trigger mục 1.4 cho năm loại bản ghi bảo vệ (bảng thuộc module khác; 000 cung cấp mẫu và test đối chiếu `information_schema`) | `NhomGhiNhanCoTriggerIT` |
| BR-M15-04 | `QuyenApi` với khóa `THAM_SO_DOI` (chỉ AC-01); nhật ký trước/sau | `ThamSoQuyenIT` |
| BR-M15-06 | Cột sinh `ten_hieu_luc` `UNIQUE` | `DanhMucMauIT` |
| BR-M15-07 | `ThoiGianNghiepVu.batDauNgay` | `NgayHieuLucTest` |
| BR-M15-08 | Bảng chuyển: `huy` bắt buộc `lyDo` | `YeuCauHuyTest` |
| BR-M15-09 | `NhacYeuCauChoDuyet` | `NhacYeuCauIT` (đồng hồ giả lập) |
| BR-M15-10 | Thứ tự `(ngay_hieu_luc_thuc_te, thoi_diem_quyet_dinh)` | `ApDungBuIT` |
| BR-M15-11 | `NhatKyQueryService` che `lyDo` | `NhatKyCheTruongIT` |
| BR-M15-12, 13 | `LoaiBoDuLieuHetHan` theo chuỗi | `LoaiBoDuLieuIT` |
