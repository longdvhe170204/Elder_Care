# Research: Báo cáo và dashboard (016)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

Mỗi mục gồm Quyết định, Lý do, Phương án đã cân nhắc. Quyết định nền R-01 → R-14 của 000, A-01 → A-12 của 002 (phạm vi, giới hạn trường) và các giao diện thống kê mà 001 → 019 đã khai cho 016 được dùng lại, không quyết lại. Các quyết định dưới đây là kỹ thuật; chúng không đổi nghiệp vụ (constitution IX). Chỗ spec chưa rõ hoặc mâu thuẫn được ghi ở plan, mục "Điểm cần báo lại".

---

## O-01. Vị trí trong backend

**Quyết định**: Feature 016 là module M14 – Báo cáo (mục 18), gói `com.cambae.backend.m14`, chỉ đọc. Gói con:

| Gói con | Nội dung |
| --- | --- |
| `m14.chitieu` | Danh mục chỉ tiêu (định nghĩa cố định trong mã, FR-003): mã, nhóm, định nghĩa hiển thị, nguồn, hàm tính |
| `m14.phamvi` | Dựng phạm vi báo cáo theo FR-010, FR-012 (dashboard, Trưởng tầng, Điều dưỡng, Người phụ trách ca) |
| `m14.dashboard` | Dashboard theo vai trò |
| `m14.baocao` | Báo cáo 18.1 → 18.6 |
| `m14.chitiet` | Danh sách chi tiết của một con số |
| `m14.xuat` | Xuất file, nhật ký lần xuất |
| `m14.api` | (không có giao diện cho module khác) |

**Lý do**: 016 là module cao nhất; mọi module khác đã khai giao diện thống kê cho nó.

---

## O-02. Đọc dữ liệu: qua giao diện thống kê của từng module

**Quyết định**: M14 chỉ đọc qua `*ThongKeApi` và API đọc mà module nguồn đã khai (constitution X; không đọc bảng, không view chung). Mỗi API thống kê nhận `PhamViLoc` (002) và khoảng thời gian, trả luồng bản ghi đã xác định trạng thái (FR-002) kèm mốc xếp khoảng, tầng/khu theo FR-011, và cờ "chờ xem lại", "đã hủy ghi nhận". M14 chỉ đếm, nhóm, tính tỷ lệ theo định nghĩa của spec; không suy lại trạng thái.

Giao diện dùng (đã khai ở hợp đồng các feature):

| Nhóm chỉ tiêu | Giao diện |
| --- | --- |
| Người cao tuổi, biến động | `NguoiCaoTuoiThongKeApi` (001, bổ sung theo plan này), `TrangThaiNguoiCaoTuoiApi.khoangTrangThai` |
| Danh sách chờ, loại lưu trú, tạm vắng | `LuuTruThongKeApi` (004, bổ sung theo plan này), `HopDongDocApi`, `LuotVangApi` |
| Phòng, giường, Chờ sẵn sàng, cách ly | `TinhTrangGiuongApi.demTheoTrangThai`, `GiuongThongKeApi` (003, bổ sung theo plan này) |
| Công việc, ghi nhận muộn, bán trú | `CongViecThongKeApi` (005), `CoMatBanTruApi` |
| Liều, đối chiếu | `ThuocThongKeApi` (006) |
| Chỉ số, cảnh báo, sự cố, theo dõi, khoanh vùng, nguy kịch | `ChiSoThongKeApi`, `CanhBaoThongKeApi`, `SuCoApi`, `KhoanhVungApi` (007) |
| Ca, tỷ lệ phục vụ, bàn giao, chứng chỉ | `NhanSuThongKeApi`, `CaTrucApi` (008); `GiayPhepCoSoApi` (007) |
| Phủ ca, yêu cầu, giờ làm | `XoayCaThongKeApi` (015) |
| Chi phí | `ChiPhiThongKeApi` (010) |
| Phiếu bữa ăn | `DinhDuongThongKeApi` (011) |
| Hoạt động, kiểm tra chất lượng, cô lập | `HoatDongThongKeApi` (014) |
| Số dư | `SoDuThongKeApi` (017) |
| Khai báo tạm trú | `KhaiBaoThongKeApi` (018) |
| Tài sản, kho, mua sắm | `TaiSanThongKeApi`, `KhoThongKeApi`, `MuaSamThongKeApi` (019) |
| Lần loại bỏ dữ liệu, giá trị sau đính chính | `NhatKyApi` tóm tắt loại bỏ (000 FR-051); module nguồn trả giá trị hiện hành |

`NguoiThanThongKeApi` (012), `DoGuiThongKeApi` (013), thống kê thông báo (009) không dùng ở giai đoạn này (Q-194).

**Phương án loại**: kho dữ liệu báo cáo (bảng tổng hợp do sự kiện cập nhật) — thêm bản sao dữ liệu nghiệp vụ, khó giữ "giá trị hiện hành sau đính chính" (FR-007) và phạm vi theo thời gian; quy mô 300 người không cần.

---

## O-03. Phạm vi báo cáo (FR-010 → FR-017; Q-192, Q-195, Q-196, Q-263)

**Quyết định**:
- Bảng FR-010 mã hóa thành `QuyenNhomChiTieu`: (nhóm chỉ tiêu, vai trò) → `TOAN_VIEN` / `TANG_DUOC_GIAO` / `PHAN_CONG` / `NHAN_VIEN_TANG` / `KHONG`; áp thêm thu hẹp của 002 FR-025 (`QuyenApi` với khóa `M14.<NHOM>.DASHBOARD` / `.BAO_CAO`). Kế toán chỉ hai nhóm chi phí, số dư.
- Hàm `PhamViBaoCao(nd, nhom, tu, den)`:
  - Dashboard: `PhamViApi.boLoc(nd, khoa)` (phạm vi hiện hành, gồm CFG-M15-07).
  - Trưởng tầng: tầng/khu đang được giao lúc xem (`NhiemVuApi.truongTangDuocGiao`), mọi thời điểm.
  - Điều dưỡng: danh sách khoảng (ca có tên ± CFG-M15-07, trừ ca hủy, vắng, nghỉ có duyệt) × đối tượng phân công; dựng từ `PhamViApi.boLocLichSu` và lịch sử ca của 008 (`CaTrucApi.caCuaNhanVien`, phân công theo ca).
  - Người phụ trách ca: cả tầng/khu trong giờ ca mình phụ trách; ngoài giờ đó theo quy tắc Điều dưỡng.
  - Nhiều vai trò: hợp theo từng vai trò, khử trùng theo khóa bản ghi (FR-015).
- Lọc hai bước: gọi API thống kê với `PhamViLoc` thô (hợp tầng/người), rồi lọc chính xác từng bản ghi theo khoảng thời gian của phạm vi. Bộ lọc giao diện chỉ liệt kê giá trị thuộc phạm vi (FR-013).
- Không xác định được phạm vi (002 báo lỗi) → 403 `KHONG_XAC_DINH_PHAM_VI`, báo Quản lý viện qua 002 FR-041b (FR-016).
- Danh sách chi tiết người ngoài phạm vi hiện hành (Q-196): DTO rút gọn (họ tên, phòng/giường lúc phát sinh, nội dung bản ghi); không có liên kết hồ sơ hiện tại; liên kết bản ghi đầy đủ chỉ khi `QuyenApi` ở feature sở hữu cho phép tại lúc mở.

---

## O-04. Giới hạn trường (FR-014, FR-042a, FR-054, FR-072; Q-133, Q-202)

**Quyết định**: DTO của M14 mang `@NhomTruong` như mọi DTO; giới hạn trường của 002 (A-08) áp khi tuần tự hóa màn hình và khi ghi file. Chỉ tiêu dựng từ trường bị giới hạn (dị ứng, chỉ số, thuốc…) không có trong nhóm hiển thị của Hành chính (bảng FR-010). Sự cố cho Hành chính lọc theo dấu "không thuộc sức khỏe" hiện hành của danh mục loại sự cố (007) và DTO không có mô tả, diễn biến. Lý do nghỉ, đổi ca không có trong bất kỳ DTO nào của M14 (FR-072).

---

## O-05. Định nghĩa chỉ tiêu và tính toán (FR-002 → FR-008, FR-020 → FR-073)

**Quyết định**:
- Mỗi chỉ tiêu là một lớp `ChiTieu` với: mã (ví dụ `CS.TY_LE_DUNG_HAN_BAT_BUOC`), nhóm, mô tả định nghĩa (hiển thị tại chỗ, FR-003), nguồn, hàm thuần `tinh(banGhi[]) → GiaTri` (đếm, tổng, tỷ lệ kèm tử/mẫu, trung bình, lớn nhất). Hàm `chiTiet` trả đúng tập bản ghi đã đưa vào phép tính (FR-009).
- Quy tắc chung trong hàm thuần: loại bản ghi Hủy ghi nhận; mẫu số 0 → "—" "không có dữ liệu"; thiếu cấu hình → "chưa cấu hình"; nhãn "ngày chưa kết thúc", "tính đến", "N bản ghi chờ xem lại", "số liệu không đầy đủ do loại bỏ…".
- Mốc xếp khoảng (FR-006): bản ghi ngoại tuyến dùng thời điểm thiết bị; "theo ca" xếp bản ghi vào ca của tầng/khu có khoảng giờ chứa mốc.
- Bảng kiểm thử SC-001: bộ dữ liệu dựng sẵn với kết quả tính tay cho mọi chỉ tiêu.

---

## O-06. Dashboard (FR-020, FR-021; SC-002, SC-009)

**Quyết định**:
- `GET /dashboard` trả các khối được phép theo vai trò và phạm vi hiện hành; mỗi khối một lời gọi API thống kê "đếm theo nhóm" (không tải bản ghi), chạy song song bằng virtual threads (Java 25) với thời hạn chung 2,5 giây; khối quá hạn trả "đang cập nhật" thay vì chặn cả trang.
- Làm mới: web gọi lại mỗi 30 giây với `If-None-Match` (ETag là băm kết quả); không đổi → 304. Đáp ứng "≤ 1 phút" (SC-002) mà không cần kênh đẩy riêng.
- Bộ đệm phía máy chủ 20 giây, khóa theo (khối, băm phạm vi) — không chia sẻ giữa người có phạm vi khác nhau.
- Không có lệnh xuất từ dashboard.

**Phương án loại**: đẩy qua SSE của 009 — 009 là kênh thông báo, không phải kênh dữ liệu báo cáo; polling 30 giây đủ cho 60 người dùng.

---

## O-07. Báo cáo theo khoảng (FR-022 → FR-073; SC-003)

**Quyết định**:
- `GET /bao-cao/{nhom}` với khoảng, bộ lọc; khoảng > CFG-M14-01 → 422 `VUOT_KHOANG_TOI_DA` (không tự cắt).
- Báo cáo tại một ngày đã qua (FR-022, FR-024) dùng API trả trạng thái theo lịch sử (001 `khoangTrangThai`, phiên bản mức, 003 trạng thái giường tại thời điểm).
- Chi phí (FR-050 → FR-055a): hai chế độ "đã chốt" / "tạm tính"; khoản điều chỉnh theo kỳ chứa kèm kỳ gốc; kiểm khớp tổng với `BangChiPhiDaChot` và dòng tổng kiểm soát của file kế toán (010) trong test SC-007. Tên thuốc theo `QuyenXemTruongApi` từng người cao tuổi (FR-054).
- Mục tiêu thời gian: 1 tháng ≤ 10 giây, 12 tháng ≤ 30 giây (SC-003): API thống kê nguồn dùng truy vấn nhóm có chỉ mục theo (mốc, tầng); M14 xử lý luồng không giữ toàn bộ trong bộ nhớ.

---

## O-08. Xuất file và nhật ký (FR-060 → FR-062; Q-193, Q-197, Q-198)

**Quyết định**:
- Định dạng XLSX (fastexcel, đã dùng ở 010, 017) — một trang tổng hợp, một trang chi tiết (nếu được), dòng đầu ghi người xuất, "tính đến", bộ lọc, chế độ; CSV là tùy chọn cho trang tổng hợp.
- Hàm `NoiDungDuocXuat(vaiTro, baoCao)`: Quản lý viện có định danh người cao tuổi (bắt buộc `mucDich`); vai trò khác chỉ tổng hợp, bỏ mọi dòng có định danh và mọi nhóm chỉ có đúng một người (FR-061); giờ làm theo nhân viên chỉ Quản lý viện.
- Trình tự (FR-062): dựng file vào kho tệp → ghi bản ghi nhật ký "Xuất báo cáo" (`NhatKyApi`, chi tiết: bộ lọc, khoảng, phạm vi, chế độ, số dòng, dấu định danh, mục đích) trong transaction → trả liên kết tải. Lỗi ghi nhật ký → không trả file. File không đổi sau đó.
- Không ghi nhật ký khi chỉ xem (FR-062; Q-03 chọn không).

---

## O-09. Chỉ đọc (FR-001; SC-008)

**Quyết định**: Mọi transaction của M14 là `readOnly = true` trừ lệnh xuất (chỉ ghi nhật ký qua 000 và tệp). ArchUnit: `m14` không gọi API có tên `*Lenh*`, `*Port` ghi, không phát sự kiện ngoài nhật ký. Test SC-008 đếm bản ghi nghiệp vụ, thông báo trước/sau bộ kiểm thử.

---

## O-10. Bổ sung giao diện thống kê cho 001, 003, 004

**Quyết định**: Ba module làm plan đầu chưa khai giao diện thống kê cho 016; thêm (đọc, không khóa):

```java
// m01.api.NguoiCaoTuoiThongKeApi (001)
Stream<TrangThaiTaiNgayView> trangThaiTaiNgay(PhamViLoc phamVi, LocalDate ngay);        // trạng thái 5.5, mức hiệu lực tại ngày
Stream<BienDongView> bienDong(PhamViLoc phamVi, Instant tu, Instant den);              // FR-023: lệnh trạng thái, đổi mức theo thời điểm hiệu lực
Stream<DanhGiaQuaHanView> danhGiaLaiQuaHan(PhamViLoc phamVi);                           // FR-044 (trạng thái do 001 xác định)
// m02.api.LuuTruThongKeApi (004)
Stream<LoaiLuuTruTaiNgayView> loaiLuuTruTaiNgay(PhamViLoc phamVi, LocalDate ngay);      // FR-022 (hợp đồng hiệu lực, chờ hiệu lực, chưa xác định)
Stream<HoSoChoView> danhSachCho(LocalDate tu, LocalDate den);                           // FR-025
// m03.api.GiuongThongKeApi (003)
Stream<TrangThaiGiuongTaiNgayView> trangThaiTaiNgay(PhamViLoc phamVi, Instant t);       // FR-024
Stream<KhoangChoSanSangView> choSanSang(PhamViLoc phamVi, Instant tu, Instant den);     // FR-033 (CFG-M03-04)
Stream<PhongCachLyView> phongCachLy(PhamViLoc phamVi, Instant t);
```

---

## O-11. Quyền, kiểm thử

**Quyết định**:
- `ChucNang` `BAO_CAO` với khóa con theo nhóm chỉ tiêu và loại (`DASHBOARD`, `BAO_CAO`); quyền xuất đi theo quyền xem (FR-010a).
- Kiểm thử:
  - test bảng cho hàm tính của mọi chỉ tiêu (SC-001), `PhamViBaoCao` (SC-011), `NoiDungDuocXuat` (SC-010, SC-013);
  - bộ 11 tài khoản quyền (SC-004), quét DTO, file cho trường bị giới hạn (SC-005, SC-012);
  - đo hiệu năng dashboard 60 người dùng (SC-002), báo cáo 12 tháng (SC-003);
  - khớp dashboard ↔ báo cáo ↔ chi tiết (SC-006); chi phí ↔ bảng đã chốt ↔ file kế toán (SC-007);
  - SC-008: đếm bản ghi nghiệp vụ, thông báo trước/sau;
  - SC-014: so với trạng thái giường của 003 cùng thời điểm;
  - bản giả cho mọi `*ThongKeApi` (dữ liệu dựng sẵn) để test M14 độc lập.
- Tên test theo FR (`FR_031_TyLeDungHanIT`, `FR_012_PhamViDieuDuongIT`).
