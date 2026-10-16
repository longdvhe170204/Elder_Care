# Research: Phòng, giường và phân bổ giường (003)

**Ngày**: 2026-10-07 | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

Mỗi mục gồm Quyết định, Lý do, Phương án đã cân nhắc. Các quyết định nền R-01 → R-14 của 000, A-01 → A-14 của 002 và H-01 → H-15 của 001 được dẫn lại, không quyết lại. Không mục nào đổi nghiệp vụ (constitution IX); chỗ spec hoặc nguồn còn thiếu ghi ở "Điểm cần báo lại" của plan.

---

## G-01. Vị trí trong backend và ranh giới với feature 019

**Quyết định**:
- Feature 003 là phần "phòng và giường" của module M03 (mục 7). Gói `com.cambae.backend.m03` có các gói con:
  - `cocau`: khu vực, tầng, loại phòng, phòng, giường (thuộc tính danh mục), danh mục lý do chuyển giường;
  - `giuong`: trạng thái giường, bảng chuyển, lịch sử trạng thái, Xác nhận giường sẵn sàng, Tạm ngừng sử dụng;
  - `phanbo`: phân bổ, chuyển giường, nhập bù, dấu trên phân bổ, tra cứu lịch sử vị trí;
  - `cachly`: đặt, gỡ cách ly phòng, xác định phòng chịu chặn cách ly;
  - `bantru`: khu nghỉ bán trú, buổi, kiểm tra sức chứa;
  - `lich`: các job của 003 (G-09);
  - `api`: giao diện, DTO, sự kiện, SPI công khai.
- Tài sản (7.7) cũng thuộc M03. Plan 019 dự kiến đặt phần tài sản ở `m03.taisan`. Hai feature gọi nhau qua giao diện nội bộ của module (`GiuongTheoTaiSanPort` do 003 cài, `TaiSanGiuongPort` do 019 cài; [contracts/module-interfaces.md](contracts/module-interfaces.md) mục 3). ArchUnit cấm `m03.taisan` đọc bảng của `m03.giuong`, `m03.phanbo` và ngược lại. Nếu plan 019 chọn đặt tài sản ở module khác, hai port đổi thành SPI công khai mà không đổi chữ ký.

**Lý do**: Constitution X chia gói theo M01–M15. Đặt cả giường và tài sản giường trong M03 tránh vòng phụ thuộc giữa hai module (Tạo giường ghi tăng tài sản, Q-238; lệnh tài sản đổi trạng thái giường, Q-234), mà vẫn giữ ranh giới feature bằng port và ArchUnit.

**Phương án đã cân nhắc**: Tách tài sản thành module riêng: tạo vòng 003 ↔ 019 mà Modulith `verify()` từ chối, phải dùng sự kiện sau commit và mất tính "trong cùng một lần" của Q-238, FR-013a.

---

## G-02. Chiều phụ thuộc giữa module: module thấp hơn khai báo SPI

**Quyết định**: Theo mẫu `ThamGiaChuyenTrangThai` của 001 (H-02): khi hai module cần nhau, module ở tầng thấp hơn khai báo giao diện trong gói `api` của mình, module kia cài. Với 003:

| Cặp | Chiều phụ thuộc mã | Cách giữ một chiều |
| --- | --- | --- |
| 003 ↔ 001 | 003 → 001 | 003 đọc `NguoiCaoTuoiApi`, `MucChamSocApi`, `TrangThaiNguoiCaoTuoiApi`; 003 cài SPI `ThamGiaChuyenTrangThai` của 001 |
| 003 ↔ 002 (M15) | 003 → M15 | `PhanBoGiuongApi` mà 002 cần được khai báo trong `m15.taikhoan.api` dưới dạng SPI, M03 cài (chữ ký giữ như plan 002) |
| 003 ↔ 004 | 004 → 003 | 004 gọi lệnh của 003 (`PhanBoGiuongLenhApi`, `SucChuaBanTruApi`). 003 cần dữ liệu lưu trú của 004 qua SPI `NguonLuuTruChoPhanBo`, `DemLichDenBanTru` khai báo ở `m03.api`, 004 cài |
| 003 ↔ 007 | 007 → 003 | 007 đọc lịch sử vị trí, gọi tra cứu cùng phòng. 003 cần vùng khoanh vùng và danh sách nghi nhiễm, tiếp xúc qua SPI `TinhTrangLayNhiemChoPhanBo` khai báo ở `m03.api`, 007 cài |
| 003 ↔ 008 | 008 → 003 | 008 kiểm vị trí của phân công. 003 cần biết vị trí còn phân công trỏ vào (FR-005 c) qua SPI `ThamChieuViTri`, 008 cài |
| 003 → 005, 009, 011, 016 | Một chiều qua sự kiện | 003 phát sự kiện sau commit (G-12) |

Mỗi SPI chưa có bản cài thật thì test dùng bản giả giữ đúng chữ ký (G-16).

**Lý do**: Modulith `verify()` từ chối vòng phụ thuộc giữa module (000 R-02). SPI giữ một chiều mà vẫn cho lời gọi đồng bộ trong cùng transaction khi spec đòi "trong cùng một lần".

**Phương án đã cân nhắc**:
- Sao chép dữ liệu của module khác vào bảng của 003 (vùng khoanh vùng, dấu "chờ chuyển người"): nhanh khi lọc nhưng là bản sao có thể lệch. Truy vấn qua SPI chỉ trả vài chục id, đủ nhanh với 300 giường.
- Sự kiện sau commit cho mọi tương tác: vi phạm các yêu cầu "trong cùng một lần" (FR-023, FR-025, FR-028, FR-013a).

---

## G-03. Chống phân bổ trùng và lệnh đồng thời (DBR-09, FR-019, FR-020, FR-033, BR-M03-02)

**Quyết định**:
1. **Khóa theo thứ tự cố định**. Mọi lệnh làm đổi phân bổ hoặc trạng thái giường mở transaction và khóa `SELECT … FOR UPDATE` theo thứ tự:
   1. dòng `nguoi_cao_tuoi` của M01, chỉ khi lệnh đi qua `ChuyenTrangThaiService` của 001 (001 tự khóa trước khi gọi bean của 003);
   2. dòng `vi_tri_nct` của người cao tuổi (bảng của 003, mỗi người một dòng, tạo khi cần bằng `INSERT … ON DUPLICATE KEY UPDATE`);
   3. các dòng `giuong` liên quan, theo `id` tăng dần.

   003 không bao giờ gọi API ghi của 001, nên không có đường khóa ngược. Lỗi deadlock của MySQL (`CannotAcquireLockException`) được thử lại tối đa hai lần trong cùng yêu cầu HTTP.
2. **Kiểm tra lại sau khi có khóa**. Mọi điều kiện FR-018 được tính sau khi khóa xong. Lệnh đến sau chờ khóa rồi được kiểm tra trên trạng thái mới; còn hợp lệ thì vẫn thành công (FR-020, FR-033 "lệnh được ghi nhận trước thắng"). Vì vậy lệnh phân bổ, chuyển giường không dùng `If-Match`, giống lệnh trạng thái của 001 (H-02).
3. **Ràng buộc ở cơ sở dữ liệu** trên `phan_bo_giuong` (data-model mục 4):
   - cột sinh `giuong_dang_hieu_luc = IF(trang_thai='DANG_HIEU_LUC', giuong_id, NULL)` có `UNIQUE`: mỗi giường tối đa một phân bổ đang hiệu lực;
   - cột sinh `nct_dang_hieu_luc`, `nct_tuong_lai` có `UNIQUE`: mỗi người tối đa một phân bổ đang hiệu lực và một phân bổ tương lai (FR-019);
   - trigger `trg_phan_bo_giuong_khong_chong` (`BEFORE INSERT`, `BEFORE UPDATE`) `SIGNAL` khi có phân bổ khác của cùng giường, không ở Đã hủy, giao khoảng `[bat_dau, ket_thuc_hieu_dung)`. Trigger là lưới an toàn khi mã có lỗi; khóa ở bước 1 mới là cơ chế chính, vì đọc trong trigger không khóa khoảng.
4. **Khoảng dùng để xét chồng** là nửa mở `[bắt đầu, kết thúc)` (FR-019). Kết thúc hiệu dụng là cột sinh `ket_thuc_hieu_dung = COALESCE(ket_thuc, ket_thuc_du_kien, '9999-12-31')`. Nhờ đó phân bổ nội trú ngắn ngày có kết thúc dự kiến nằm trước được một phân bổ tương lai (US2 #6), còn phân bổ không có kết thúc chặn mọi phân bổ sau nó trên cùng giường.
5. **Mã lỗi**: vi phạm (g) có mã `PB_CHONG_THOI_GIAN`, mô tả "Giường đã được phân bổ trong khoảng thời gian này" (BR-M03-02). Vi phạm ràng buộc `UNIQUE` hay trigger do lỗi mã được ánh xạ về cùng mã, không trả 500.

**Lý do**: MySQL không có ràng buộc loại trừ khoảng (000 data-model 1.2). Khóa dòng giường tuần tự hóa mọi lệnh trên cùng giường; khóa `vi_tri_nct` tuần tự hóa mọi lệnh trên cùng người. SC-001 đòi đúng cả với 10 người dùng trong cùng một giây; test đồng thời ở G-16.

**Phương án đã cân nhắc**:
- Khóa lạc quan bằng `phien_ban` trên giường: lệnh đến sau bị từ chối kể cả khi vẫn hợp lệ, trái FR-020.
- Khóa mức bảng hoặc `SERIALIZABLE`: đúng nhưng chặn cả các lệnh trên giường khác.
- Khóa dòng `nguoi_cao_tuoi` của M01 từ lệnh của 003: phải đọc khóa bảng của module khác, trái constitution X.

---

## G-04. Trạng thái giường (mục C, FR-011 → FR-016)

**Quyết định**:
- `giuong.trang_thai` là một trong bảy mã `TRONG`, `DANG_SU_DUNG`, `DANG_GIU_CHO`, `CHO_SAN_SANG`, `DANG_BAO_TRI`, `TAM_NGUNG`, `KHONG_SU_DUNG`. Thuộc tính đi kèm (lý do giữ chỗ, người hoặc hồ sơ được giữ, hạn giữ, hạn xác nhận sẵn sàng, phân bổ vừa đóng) nằm trên cùng dòng, có `CHECK` ràng buộc theo trạng thái (data-model mục 3).
- Bảng chuyển của spec (mục C) cài thành hằng bảng `BangChuyenTrangThaiGiuong`. Chỉ `GiuongTrangThaiService.chuyen(giuong, suKien, canCu)` (package-private trong `m03.giuong`) được ghi cột `trang_thai`; ArchUnit kiểm. Cặp không có trong bảng trả 409 `CHUYEN_KHONG_HOP_LE`. Không có endpoint đặt trạng thái giường (US4 #10, #11).
- Mỗi lần chuyển thêm một dòng `lich_su_trang_thai_giuong` (nhóm 3) trong cùng transaction, gồm từ, đến, sự kiện, thời điểm thực tế, thời điểm dự kiến (với việc của Bộ lập lịch), người thực hiện, lý do, căn cứ (FR-016, FR-016a).
- **Giường về Trống sau bảo trì**: trạng thái đích "Trống, hoặc Tạm ngừng sử dụng nếu giường vào bảo trì từ Tạm ngừng sử dụng" lấy từ dòng lịch sử gần nhất có `den = 'DANG_BAO_TRI'` (spec: "lấy từ lịch sử trạng thái giường").
- **Chờ sẵn sàng**: khi vào trạng thái này, dòng giường lưu `cho_san_sang_tu`, `han_xac_nhan_san_sang = cho_san_sang_tu + CFG-M03-04` và `phan_bo_vua_dong_id` (rỗng khi vào từ Sử dụng lại, Q-261). `phan_bo_vua_dong_id` cho biết ai vừa rời giường (FR-045) và bảo đảm DBR-26: lệnh Xác nhận giường sẵn sàng chỉ hợp lệ khi giường đang `CHO_SAN_SANG`, nên luôn đến sau lần đóng phân bổ gần nhất.
- **Kích hoạt BR-M02-01** (FR-014): mỗi lần giường vào `TRONG` mà không có phân bổ tương lai và `TaiSanGiuongPort` báo không mang dấu "chờ chuyển người", 003 phát sự kiện `GiuongDaTrong` sau commit. 004 nghe để báo Hành chính chọn hồ sơ chờ (004 FR-012); 003 không đề xuất hồ sơ (Q-307).

**Lý do**: Một điểm ghi trạng thái giường cho mọi nguồn (lệnh của 003, tác động từ 001, 004, lệnh tài sản của 019) giữ đúng một bảng chuyển. Cột trạng thái trên dòng giường cho bảng giường và dashboard đọc nhanh; lịch sử chỉ thêm giữ truy vết.

**Phương án đã cân nhắc**: Suy trạng thái giường từ phân bổ và lịch sử mỗi lần đọc: không có cột nào để khóa và mọi danh sách đều phải tính lại.

---

## G-05. Phân bổ giường: lệnh và vòng đời (mục D, FR-017 → FR-027)

**Quyết định**:
- **Một lệnh "Phân bổ giường"** `POST /phan-bo-giuong`. Kết quả phụ thuộc trạng thái người cao tuổi (001):
  - Đang tiếp nhận: tạo phân bổ `TUONG_LAI` với `bat_dau` là thời điểm bắt đầu dự kiến (FR-023);
  - Đang lưu trú mang dấu "chưa có giường" (FR-025a): tạo phân bổ `DANG_HIEU_LUC` bắt đầu tại thời điểm thực hiện;
  - đã có phân bổ đang hiệu lực: 409 `DA_CO_PHAN_BO`, hướng tới lệnh Chuyển giường (US2 #9);
  - trạng thái cuối: vi phạm (h).

  Kết thúc dự kiến lấy từ hợp đồng nội trú ngắn ngày qua `NguonLuuTruChoPhanBo.ketThucDuKien` (004), không do người dùng nhập.
- **Kiểm tra FR-018** do một thành phần `KiemTraPhanBo` thực hiện, trả đủ danh sách vi phạm (a) → (i) trong một lần (SC-002). Danh sách giường để chọn (FR-022) dùng cùng thành phần ở dạng truy vấn tập hợp, nên danh sách và lệnh không thể lệch nhau.
  - (a) mức: `MucChamSocApi.hienHanh`. Không có thì `NguonLuuTruChoPhanBo.mucTheoHopDong`, rồi `mucTheoLuotDangKy` (Q-301). Phân bổ lưu `muc_da_kiem_tra` và `nguon_muc` (FR-018).
  - (b) loại hình lưu trú: `NguonLuuTruChoPhanBo.loaiLuuTru` theo thứ tự hợp đồng Hiệu lực, hợp đồng đang lập, lượt đăng ký (CHK007).
  - (c) giới tính: `NguoiCaoTuoiApi.gioiTinh`.
  - (d) cách ly: G-08.
  - (f) giữ chỗ: giường `DANG_GIU_CHO` chỉ nhận chính người được giữ (`giu_cho_nct_id`) (DBR-10).
  - (i) dấu: `TaiSanGiuongPort.mangDauChoChuyenNguoi`.
- **Phân bổ tương lai bắt đầu** chỉ theo sự kiện: lệnh Hoàn tất tiếp nhận (G-06), chuyển giường theo lịch (G-07), hoặc giường về Trống khi chuyển theo lịch đang chờ (FR-011a). Không có job nào bắt đầu phân bổ theo đồng hồ.
- **Hủy phân bổ tương lai** `POST /phan-bo-giuong/{id}/huy` có lý do. Các trường hợp tự hủy (Hủy tiếp nhận, giường hỏng, kết thúc lưu trú, đổi giường khi tiếp nhận, chuyển theo lịch áp dụng không thành) đều là tác động kèm lệnh của actor hoặc của 000. Bản ghi Đã hủy được giữ.
- **Bất biến sau đóng**: trigger `trg_phan_bo_giuong_khong_sua` chặn `UPDATE` khi `OLD.trang_thai IN ('DA_DONG','DA_HUY')`; không có endpoint sửa. Sai sót đi đường đính chính của 000 (G-10).
- **Nhập bù** (FR-027) là tham số `thoiDiemChuyen` trong quá khứ của lệnh Chuyển giường:
  - lùi không quá CFG-M03-07: thực hiện ngay, đánh dấu `nhap_bu`, lưu `thoi_diem_nhap_thuc`;
  - lùi xa hơn: lập yêu cầu phê duyệt loại `M03.NHAP_BU_PHAN_BO` (000 FR-031, bảng loại yêu cầu) qua `YeuCauPheDuyetApi.lap`, người lập là người thực hiện lệnh. Handler của 003 kiểm tra lại toàn bộ điều kiện khi áp dụng. Bảng chỉ mục `yeu_cau_nhap_bu` ghi người cao tuổi và giường của yêu cầu để tra cứu FR-035 gắn dấu "có nhập bù đang chờ duyệt" (CHK051);
  - (c) chồng khoảng vắng: `TrangThaiNguoiCaoTuoiApi.khoangTrangThai` (bổ sung vào hợp đồng 001, G-13) trả các khoảng Tạm vắng, Điều trị tại bệnh viện, Hoạt động bên ngoài.

**Lý do**: Một lệnh phân bổ khớp UC-20 và cách người dùng nghĩ ("chọn giường cho người này"); kiểu bản ghi do trạng thái quyết định đúng FR-023, FR-025a. Một thành phần kiểm tra dùng chung cho danh sách và lệnh bảo đảm SC-002 và SC-005.

**Phương án đã cân nhắc**: Ba endpoint riêng (đặt trước, phân bổ ngay, phân bổ lại khi trở về): người dùng phải tự chọn đúng loại, dễ sai mà không thêm kiểm soát.

---

## G-06. Tham gia máy trạng thái người cao tuổi (001 H-02)

**Quyết định**: 003 cài một bean `PhanBoThamGiaChuyenTrangThai`, `@Order` sau 004 (004 mở hoặc đóng lượt vắng, hợp đồng trước). Bean không tự khóa `nguoi_cao_tuoi`; nó khóa `vi_tri_nct` và giường theo G-03 trong `thucHienTacDong`, rồi kiểm tra lại.

| Lệnh của 001 | Điều kiện (`kiemTraDieuKien`) | Tác động (`thucHienTacDong`) |
| --- | --- | --- |
| `HOAN_TAT_TIEP_NHAN` | Chỉ khi loại lưu trú là nội trú: có phân bổ Tương lai; giường đặt trước `TRONG` hoặc giữ chỗ cho chính người này, không mang dấu, FR-018 thỏa theo mức hiện hành. Khác đi thì vi phạm `GIUONG_DAT_TRUOC_CHUA_SAN_SANG` hoặc liệt kê vi phạm FR-018, trừ khi `chiTiet.doiPhanBoSangGiuongId` là giường `TRONG` thỏa FR-018 (FR-023, Q-50) | Phân bổ chuyển `DANG_HIEU_LUC`, `bat_dau` là thời điểm lệnh; giường sang `DANG_SU_DUNG`. Có đổi giường thì phân bổ cũ `DA_HUY` (lý do "đổi giường khi tiếp nhận") và mở phân bổ mới. Kết thúc giữ chỗ hồ sơ chờ của người này ở giường khác (004 FR-016) |
| `HUY_TIEP_NHAN` | — | Phân bổ Tương lai `DA_HUY`; giường đang giữ chỗ cho hồ sơ chờ của người này về `TRONG` (không qua Chờ sẵn sàng) |
| `CHO_TAM_VANG`, `CHUYEN_VIEN` | — | Hỏi `NguonLuuTruChoPhanBo.giuGiuongKhiVang(nctId)` (004 đã mở lượt vắng ở bean chạy trước). Giữ giường: giường `DANG_GIU_CHO` lý do `NGUOI_VANG`, hạn theo kết quả, phân bổ giữ nguyên (FR-024). Không giữ: đóng phân bổ tại thời điểm rời viện, giường `CHO_SAN_SANG` |
| `GHI_NHAN_TRO_VE` | — (không chặn, FR-024a, FR-025a) | Có phân bổ đang hiệu lực: giường về `DANG_SU_DUNG`. Phòng đang chịu chặn cách ly: đặt dấu "chờ bác sĩ xác nhận vùng cách ly", phát `TroVePhongCachLy` (Khẩn cấp, Q-114). Không có phân bổ: đặt dấu "chưa có giường" trên `vi_tri_nct` |
| `KET_THUC_LUU_TRU`, `GHI_NHAN_QUA_DOI` | — | Đóng phân bổ tại thời điểm hiệu lực của lệnh, hủy phân bổ Tương lai, giường `CHO_SAN_SANG`, gỡ dấu "chưa có giường" (FR-025, CHK004) |

- **Cảnh báo trước khi xác nhận** (FR-024a (1)): SPI của 001 cần thêm `default List<CanhBao> canhBao(NguCanhLenh nc)`. 001 gọi hàm này trong `GET …/dieu-kien/{lenh}` và trả kèm phản hồi lệnh, cùng cách 001 đã trả `THIEU_BAN_DONG_Y`. Bổ sung này được ghi vào hợp đồng 001 (plan, mục "Đồng bộ").
- **Kiểu `chiTiet` 003 sở hữu**: chỉ `HOAN_TAT_TIEP_NHAN.doiPhanBoSangGiuongId` (`int64`, tùy chọn), đúng chỗ đã dành ở api-001.yaml. Bean kiểm trong `kiemTraChiTiet`.

**Lý do**: Spec đòi các tác động trên giường xảy ra trong cùng một lần với lệnh của 001 (FR-023, FR-025). SPI của 001 là cơ chế đã chốt cho việc này.

**Phương án đã cân nhắc**: Nghe `NguoiCaoTuoiDaChuyenTrangThai` sau commit: có khoảng người đã Kết thúc lưu trú mà giường vẫn Đang sử dụng, trái FR-025.

---

## G-07. Chuyển giường (mục E, FR-028 → FR-034)

**Quyết định**:
- **Hai endpoint lệnh** trên phân bổ đang hiệu lực:
  - `POST /phan-bo-giuong/{id}/chuyen-giuong`: chuyển trực tiếp, có `lyDoChuyenId` (danh mục), `lyDo`, `thoiDiemChuyen` tùy chọn (mặc định là bây giờ; quá khứ là nhập bù, G-05);
  - `POST /phan-bo-giuong/{id}/chuyen-giuong-kiem-soat-lay-nhiem`: bắt buộc `bacSiChiDinhId` và lý do; miễn điều kiện (d) cho giường đích và cho phép chuyển ra khỏi vùng chịu chặn (FR-010). Bác sĩ chỉ định phải là tài khoản có vai trò AC-03 đang Hoạt động, kiểm qua `TaiKhoanApi.coVaiTro` của 002 (BR-M03-21); bác sĩ được báo qua sự kiện `BacSiDuocGhiChiDinh`.
- **Trong một transaction** (FR-028): khóa theo G-03, kiểm FR-018 cho giường mới, đóng phân bổ cũ (`ket_thuc` = thời điểm chuyển, lý do "chuyển giường"), mở phân bổ mới (`bat_dau` = cùng thời điểm, `phan_bo_truoc_id`), đổi trạng thái hai giường, ghi nhật ký vị trí cũ và mới. Người vắng có giữ giường: giường mới `DANG_GIU_CHO` với cùng lý do và hạn còn lại (FR-032).
- **Chặn chuyển ra khỏi vùng cách ly** (FR-010): giường hiện tại thuộc phòng chịu chặn thì chỉ endpoint kiểm soát lây nhiễm được dùng.
- **Khác đơn giá** (FR-030): `NguonLuuTruChoPhanBo.khacDonGia(nctId, loaiPhongCu, loaiPhongMoi, ngay)` (004) so phí theo loại phòng trong phiên bản đơn giá của hợp đồng (CHK016).
  - Khác đơn giá và lý do không thuộc nhóm "y tế/an toàn" (cột `y_te_an_toan` của danh mục lý do; lý do "kiểm soát lây nhiễm" là dòng cố định có cờ này): 409 `CAN_YEU_CAU_THAY_DOI_LUU_TRU`.
  - Thuộc nhóm: chuyển ngay, phân bổ mới mang dấu "chờ lập yêu cầu thay đổi lưu trú", phát `DaChuyenGiuongKhacGia` cho Hành chính và Quản lý viện. Hệ thống không tự lập yêu cầu (Q-306).
  - 004 gọi `PhanBoGiuongLenhApi.daLapYeuCauSauChuyenGap` khi Hành chính lập yêu cầu (gỡ dấu, ngừng nhắc), và `ketQuaYeuCauSauChuyenGap` khi yêu cầu được quyết định. Từ chối thì đặt dấu "chờ chuyển về phòng cùng giá".
  - Dấu này được gỡ khi người đó được chuyển sang giường mà `khacDonGia` so với hợp đồng hiện hành trả "không", hoặc khi một yêu cầu thay đổi lưu trú khác được áp dụng.
- **Chuyển giường theo lịch** (FR-029) đi qua yêu cầu `M02.THAY_DOI_LUU_TRU` của 004:
  - **lúc duyệt**: handler của 004 gọi `PhanBoGiuongLenhApi.datTruocChuyenTheoLich` trong transaction quyết định. FR-018 không thỏa thì lệnh Duyệt bị từ chối với danh sách vi phạm. Việc này cần thêm hai hàm mặc định vào `LoaiYeuCauHandler` của 000 (`kiemTraKhiDuyet`, `khiDuyet`); bổ sung được ghi vào hợp đồng 000 (plan, mục "Đồng bộ");
  - **ngày hiệu lực**: job `ApDungYeuCauDenHan` của 000 (00:05) gọi handler 004; `apDung` gọi `thucHienChuyenTheoLich(yeuCauId, thoiDiemDuKien = 00:00 ngày hiệu lực)`:
    - giường mới `TRONG`: thực hiện chuyển như trên, người thực hiện là Bộ lập lịch, căn cứ là yêu cầu;
    - giường mới `CHO_SAN_SANG` hoặc mang dấu "chờ chuyển người": phân bổ giữ `TUONG_LAI`, đặt cờ `cho_giuong_san_sang`, phát `ChuyenTheoLichChoGiuong` cho Trưởng tầng và Hành chính (FR-011a). Lệnh Xác nhận giường sẵn sàng (hoặc gỡ dấu của 019) trên giường đó thực hiện lệnh chuyển ngay trong transaction của mình, thời điểm thực tế là lúc giường về Trống;
  - **điều kiện khác không thỏa**: `kiemTraDieuKienApDung` của 004 hỏi `PhanBoGiuongLenhApi.kiemTraChuyenTheoLich` và trả vi phạm, 000 chuyển yêu cầu "Áp dụng không thành". 004 nghe `YeuCauApDungKhongThanh` và gọi `huyPhanBoTheoLich` (idempotent). Phân bổ cũ giữ nguyên.
- **Sau khi chuyển** (FR-031): phát `DaChuyenGiuong(nctId, phanBoCu, phanBoMoi, tangCu, tangMoi)` sau commit. 005 gán lại công việc chưa thực hiện (BR-M03-04), 011 đổi nơi chia suất. Phạm vi của nhân viên đổi ngay ở yêu cầu kế tiếp vì 002 đọc phân bổ trực tiếp, không đệm (002 A-07).
- **Dấu "phòng không còn phù hợp"** (FR-034, BR-M03-23): hàm `danhGiaLaiPhuHop(nctId)` tính lại (a), (b), (c) cho phân bổ đang hiệu lực và Tương lai, rồi đặt hoặc gỡ dấu. Hàm chạy khi nhận `MucChamSocDaDoi`, `DanhGiaDauVaoDaXacNhan` (001), `ThongTinCaNhanDaCapNhat` có trường giới tính (bổ sung vào hợp đồng 001), `LoaiLuuTruDaDoi` (004). Hàm idempotent. Chỉ lần đặt dấu mới phát thông báo một lần. `PhongChoPhepApi.phongHienTaiChoPhepMuc` cho 004 hiện cảnh báo trên yêu cầu đổi mức (FR-034 (1)).

**Lý do**: Endpoint riêng cho lệnh có ngoại lệ (kiểm soát lây nhiễm) giữ constitution XI và làm rõ trong nhật ký lệnh nào đã miễn điều kiện (d). Gắn chuyển theo lịch vào vòng đời yêu cầu của 000 dùng lại job áp dụng đúng một lần (000 R-10), không thêm job thứ hai.

**Phương án đã cân nhắc**:
- Job riêng của 003 quét phân bổ tương lai tới ngày: hai job cùng xử lý một yêu cầu, khó giữ "đúng một lần".
- Tạo phân bổ tương lai sau commit của lệnh duyệt (nghe `YeuCauDaQuyetDinh`): giường có thể bị lấy giữa hai transaction, khi đó yêu cầu đã duyệt mà không giữ được giường.

---

## G-08. Cách ly phòng và khoanh vùng (mục B, FR-008 → FR-010)

**Quyết định**:
- `phong.dang_cach_ly` (boolean) đổi qua hai lệnh `POST /phong/{id}/dat-cach-ly` và `/go-cach-ly`, bắt buộc lý do, `suCoId` tùy chọn (tham chiếu logic, không khóa ngoại sang 007). Mỗi lệnh thêm một dòng `lich_su_cach_ly_phong` (nhóm 3).
- **Phòng chịu chặn cách ly** tại thời điểm t: `dang_cach_ly`, hoặc `tang_id` thuộc tập tầng đang khoanh vùng, hoặc khu vực của phòng thuộc tập khu vực đang khoanh vùng. Hai tập lấy một lần mỗi lệnh từ SPI `TinhTrangLayNhiemChoPhanBo.vungDangKhoanh(t)` (007 cài). Danh sách giường dùng hai tập này như tham số `NOT IN`.
- **Gỡ chặn**: gỡ cách ly phòng, hoặc nhận sự kiện `KhoanhVungDaGo` của 007, thì 003 gỡ dấu "chờ bác sĩ xác nhận vùng cách ly" trên các phân bổ của phòng không còn chịu chặn (FR-024a (c)).
- **Bác sĩ xác nhận ở lại** `POST /phan-bo-giuong/{id}/xac-nhan-o-lai-vung-cach-ly`: AC-03 bất kỳ đang Hoạt động, có lý do; ghi người, lý do, thời điểm lên phân bổ và nhật ký (FR-024a (a), CHK057).
- Dấu còn mở được trả cho 008 qua `CanhBaoBanGiaoApi.theoTang` để đưa vào bàn giao ca. Đây là cảnh báo chưa xử lý (1.3), không phải mục bàn giao lập sẵn.

**Lý do**: Spec chỉ cần biết "đang chịu chặn hay không" tại lúc lệnh chạy. Tập tầng, khu vực khoanh vùng nhỏ, nên hỏi 007 mỗi lệnh đủ nhanh và không sinh bản sao.

**Phương án đã cân nhắc**: Bảng `chan_khoanh_vung` do 007 ghi vào 003: là bản sao dữ liệu của 007, phải giữ đồng bộ khi 007 đổi vùng.

---

## G-09. Bộ lập lịch của 003 (FR-016a)

**Quyết định**: Bốn job, mỗi job chạy mỗi 15 phút (chu kỳ kỹ thuật như 000, 001) và khi ứng dụng khởi động, có ShedLock và một dòng `lan_chay_lich` (000 R-10). Job chỉ chuyển trạng thái theo thời gian, nhắc, báo; không tạo phân bổ hay bản ghi nghiệp vụ (000 FR-010a).

| Job | FR | Việc | Chống lặp |
| --- | --- | --- | --- |
| `HetHanGiuCho` | FR-015 | Giữ cho hồ sơ chờ quá `han_giu_cho`: giường về `TRONG`, gọi `NguonLuuTruChoPhanBo.hoSoChoHetHanGiuCho` trong cùng transaction để 004 trả hồ sơ về Đang chờ, phát `GiuongDaTrong`. Giữ cho người vắng quá hạn: không giải phóng, phát `GiuChoNguoiVangHetHan` để 004 xử lý theo BR-M02-07 | Trạng thái giường đổi (hồ sơ chờ); cột `da_bao_het_han_luc` (người vắng) |
| `BaoQuaBatDauDuKien` | FR-023 | Phân bổ ban đầu `TUONG_LAI` quá `bat_dau` mà người vẫn Đang tiếp nhận: báo Hành chính | Cột `da_bao_qua_bat_dau_cho` lưu mốc đã báo; mốc đổi thì báo lại |
| `NhacChuKyPhanBo` | FR-025a, FR-030, FR-034 | Nhắc theo CFG-M03-02 cho dấu "chưa có giường" (Hành chính), "chờ lập yêu cầu thay đổi lưu trú" (Hành chính), "chờ chuyển về phòng cùng giá" và "phòng không còn phù hợp" (Trưởng tầng phụ trách, Hành chính) | Bảng `nhac_phan_bo` lưu lần nhắc cuối cho mỗi (đối tượng, loại dấu); dấu gỡ thì xóa dòng nên không nhắc nữa (SC-013) |
| `NhacGiuongChoSanSang` | FR-044 | Giường `CHO_SAN_SANG` quá `han_xac_nhan_san_sang`: nhắc Trưởng tầng được giao của tầng một lần | Cột `da_nhac_san_sang_luc`, đặt lại khi giường vào `CHO_SAN_SANG` lần sau |

- **Thời điểm ghi** (FR-016a): mọi việc ghi thời điểm thực tế (`Clock`) cùng thời điểm dự kiến (hạn giữ, mốc nhắc, 00:00 ngày hiệu lực). Trễ hơn CFG-M03-08 thì bản ghi (phân bổ, lịch sử giường) mang `thuc_hien_tre = true` và 003 phát `ViecLichTre` cho Quản lý viện. Lần nhắc, báo không có bản ghi nghiệp vụ; mốc dự kiến và cờ trễ nằm trong `chi_tiet` của nhật ký.
- **Chuyển giường theo lịch** chạy trong job của 000 (G-07). 003 nhận thời điểm dự kiến qua tham số và áp cùng quy tắc trễ.
- Nhắc giường mang dấu "chờ chuyển người" về Trống là việc của 019 (FR-013b); 003 không lặp.
- Hạn giữ hồ sơ chờ: 004 FR-015 cũng mô tả việc này. Plan chọn một job duy nhất ở 003, vì `han_giu_cho` là dữ liệu của 003; 004 chỉ cài phần trả hồ sơ về Đang chờ qua SPI. Plan 004 không được thêm job thứ hai (plan, mục "Đồng bộ").

**Lý do**: NFR-04 (xong trong 5 phút, không gửi trùng). Chu kỳ 15 phút làm trễ tối đa 15 phút, dưới ngưỡng mặc định 30 phút của CFG-M03-08, nên dấu "thực hiện trễ" chỉ xuất hiện khi hệ thống thực sự gián đoạn.

**Phương án đã cân nhắc**: Hẹn giờ chính xác từng mốc (Quartz trigger mỗi bản ghi): thừa với quy mô 300 giường; 000 đã chọn `@Scheduled` cùng ShedLock.

---

## G-10. Tra cứu lịch sử vị trí và đính chính (mục F, FR-026, FR-035 → FR-037)

**Quyết định**:
- View `v_phan_bo_hien_hanh` trên `phan_bo_giuong` `LEFT JOIN` view `v_dinh_chinh_moi_nhat` của 000 (loại `M03.PHAN_BO_GIUONG`). Các cột giường, bắt đầu, kết thúc lấy giá trị sau đính chính bằng `COALESCE(JSON_EXTRACT(...), cột gốc)`. View có cột `da_dinh_chinh`. Mọi tra cứu FR-035 và báo cáo dùng view này, không tự cài lại logic "mới nhất" (000 R-08).
- Ba truy vấn:
  - (a) vị trí của A tại t: `bat_dau <= t < ket_thuc_hieu_dung` với trạng thái `DANG_HIEU_LUC`, `DA_DONG`;
  - (b) mỗi giường của phòng, tầng hoặc khu vực tại t hoặc trong ngày D: giao với `[00:00 D, 00:00 D+1)`;
  - (c) người cùng phòng với A trong `[tu, den)`: tự nối view theo `phong_id` qua giường, tính khoảng chồng bằng `GREATEST`, `LEAST`.

  Chỉ mục `(giuong_id, bat_dau)`, `(nct_id, bat_dau)`. Vài nghìn dòng mỗi năm nên mỗi truy vấn dưới 100 ms.
- **Khoảng vắng** (FR-036) lấy từ `TrangThaiNguoiCaoTuoiApi.khoangTrangThai(nctId, {TAM_VANG, DIEU_TRI_BENH_VIEN, HOAT_DONG_BEN_NGOAI}, tu, den)` của 001, rồi gắn vào từng dòng kết quả.
- **Kết quả chỉ có** họ tên (qua `NguoiCaoTuoiApi.nhanDang`), giường, phòng, khoảng thời gian, dấu "đã đính chính", dấu "có nhập bù đang chờ duyệt". Không có trường sức khỏe (CHK041).
- **Đính chính**: 003 cài `DinhChinhDuocProvider` cho `M03.PHAN_BO_GIUONG`:
  - `daXacNhan` khi `DA_DONG`, `DA_HUY`; `tangKhuVuc` là tầng của giường gốc;
  - `kiemTraNoiDung` khóa các giường liên quan theo G-03 rồi kiểm FR-019 trên view sau đính chính. Trigger chống chồng không áp được ở đây vì bản đính chính nằm ở bảng của 000;
  - `sauKhiDinhChinh` phát `PhanBoDaDinhChinh(phanBoId, truoc, sau)`. 007 tự xét khoảng đó có nằm trong khoảng đã dùng lập danh sách tiếp xúc không (FR-026, BR-M03-24).

  Lần Xác nhận giường sẵn sàng (dòng lịch sử giường) có provider `M03.XAC_NHAN_SAN_SANG`; chỉ sửa được ghi chú và hai ô khử khuẩn, bảo hộ.
- **Quyền tra cứu toàn viện** (FR-037): `(PHAN_BO_CHUYEN_GIUONG, XEM)` hoặc `(KHOANH_VUNG_LAY_NHIEM, TAO)`. Trưởng tầng bị lọc theo `PhamViApi.boLocLichSu`, chỉ dùng để đọc (002 A-07). Điều dưỡng tra qua chức năng khoanh vùng của 007, 007 gọi `LichSuViTriApi`.

**Lý do**: Spec đòi tra cứu dùng giá trị sau đính chính (CHK017). Một view chung giữ một định nghĩa cho 003, 007, 016.

**Phương án đã cân nhắc**: Ghi giá trị đính chính ngược vào `phan_bo_giuong`: vi phạm bất biến (000 R-05).

---

## G-11. Khu nghỉ bán trú và sức chứa (mục G, FR-038 → FR-042)

**Quyết định**:
- `khu_nghi_ban_tru` có cột sinh `hieu_luc_duy_nhat = IF(trang_thai='HIEU_LUC', 1, NULL)` với `UNIQUE`, nên cơ sở dữ liệu chặn khu Hiệu lực thứ hai (FR-038). Khu nghỉ gắn đúng một tầng hoặc một khu vực (`CHECK`).
- `buoi_ban_tru` (nhóm 1) có tên, `gio_bat_dau`, `gio_ket_thuc` (`TIME`). Kiểm không chồng khung giờ ở backend dưới khóa dòng khu nghỉ.
- **Kiểm tra sức chứa** khi 004 đăng ký hoặc đổi lịch đến (004 sở hữu lịch đến):
  1. 004 gọi `SucChuaBanTruApi.khoa()` (MANDATORY, `FOR UPDATE` trên dòng khu nghỉ), nên mọi đăng ký được tuần tự hóa;
  2. 004 gọi `buoiBiChiem(ngay, gioDen, gioVe)`. Hàm trả các buổi có khung giao `[gioDen, gioVe)`, theo cùng quy ước nửa mở với FR-019 (FR-039, Q-47);
  3. 004 đếm số đăng ký trước và sau thay đổi cho từng (ngày, buổi), không tính Vắng có báo;
  4. 004 gọi `kiemTraSucChua(Map<BuoiNgay, SoDangKy>)`. Vi phạm khi số sau lớn hơn sức chứa và lớn hơn số trước. Vì vậy bớt người khỏi buổi đang vượt vẫn được (FR-040). Hàm thêm vi phạm khi tầng hoặc khu vực của khu nghỉ đang khoanh vùng (FR-042). Kết quả liệt kê mọi buổi vượt cùng số chỗ còn lại.
- **Sức chứa** là CFG-M03-01, đọc một lần mỗi lần kiểm tra (000 FR-018).
- **Đổi sức chứa hoặc khung giờ** (FR-040, BR-M03-25): 003 nghe `ThamSoDaDoi(CFG-M03-01)` và lệnh sửa buổi của chính nó, gọi SPI `DemLichDenBanTru.demTuNgay(homNay)` (004 cài) để đếm lại theo khung mới, rồi phát `KhuNghiVuotSucChua` cho Quản lý viện với danh sách buổi vượt. Không hủy lịch nào.
- `CoCauViTriApi.viTriKhuNghiBanTru()` trả tầng hoặc khu vực của khu nghỉ cho 011 (FR-038).

**Lý do**: Lịch đến thuộc 004, quy tắc sức chứa thuộc 003. Chia như trên giữ mỗi bên đúng dữ liệu của mình và tránh vòng phụ thuộc (G-02). Khóa một dòng duy nhất là đủ vì đăng ký bán trú ít (vài chục mỗi ngày).

**Phương án đã cân nhắc**: 003 đọc thẳng bảng lịch đến của 004: trái constitution X.

---

## G-12. Sự kiện và thông báo

**Quyết định**: Mọi sự kiện phát sau commit qua outbox của 000 (R-09); 009 gửi thông báo. Mức thông báo theo nguyên tắc 3 bậc mà 001 đã áp (001 H-14), vì spec 003 chỉ nêu mức của Q-46:

| Sự kiện | Người nhận | Mức |
| --- | --- | --- |
| `TroVePhongCachLy` (FR-024a) | Bác sĩ trực, Trưởng tầng phụ trách | Khẩn cấp (7.4, Q-114) |
| `PhanBoTuongLaiBiHuyDoGiuongHong` (FR-013a); `ChuyenTheoLichChoGiuong` (FR-011a); `ViecLichTre` (FR-016a) | Hành chính, Trưởng tầng phụ trách giường; với `ViecLichTre` là Quản lý viện | Trung bình |
| `DaChuyenGiuongKhacGia` (FR-030); `PhanBoKhongConPhuHop` (FR-034); `PhanBoTuongLaiQuaBatDau` (FR-023); `KhuNghiVuotSucChua` (FR-040); `NguoiRoiGiuongVaoDanhSachTiepXuc` (FR-045) | Theo FR tương ứng | Trung bình |
| `NhacPhanBo` (CFG-M03-02), `NhacGiuongChoSanSang` (FR-044); `BacSiDuocGhiChiDinh` (BR-M03-21) | Theo FR tương ứng | Nhẹ |
| `GiuongDaTrong` (FR-014) | 004 (báo Hành chính, 004 FR-012), 016 | Theo 004 |
| `DaChuyenGiuong`, `PhanBoDaBatDau`, `PhanBoDaDong` | 005, 011, 016 | Không gửi thông báo |
| `PhongDaDatCachLy`, `PhongDaGoCachLy`; `PhanBoDaDinhChinh` | 007, 016 | Không gửi thông báo (007 tự quyết) |

Mỗi sự kiện gửi 009 mang mức, nhóm người nhận, loại thông tin và khóa sự kiện (`loại + id + mốc`) để 009 không gửi trùng khi phát lại (NFR-04). Người nhận "Trưởng tầng phụ trách" lấy từ `PhamViApi.truongTangDuocGiao(tang)`; không có thì Người phụ trách ca của tầng (`nguoiPhuTrachCa`). "Bác sĩ trực" lấy từ `NhiemVuApi` của 008, không có thì mọi bác sĩ.

**Lý do**: Constitution XIV. Mức của các thông báo khác Q-46 chưa có trong spec nên plan áp nguyên tắc chung và báo lại (plan, điểm 6).

---

## G-13. Bổ sung giao diện của 000, 001, 002 cần cho 003

**Quyết định**: Plan này thêm vào hợp đồng đã thiết kế các mục sau, không đổi hành vi đã có:

| Hợp đồng | Bổ sung | Vì sao |
| --- | --- | --- |
| 000 `LoaiYeuCauHandler` | `default List<DieuKien> kiemTraKhiDuyet(YeuCauView yc)`, `default void khiDuyet(YeuCauView yc, LocalDate ngayHieuLucThucTe)` (MANDATORY, trong transaction của lệnh Duyệt) | FR-029, 004 US "khi duyệt thì tạo phân bổ tương lai" |
| 001 `ThamGiaChuyenTrangThai` | `default List<CanhBao> canhBao(NguCanhLenh nc)` | FR-024a (1): cảnh báo trước khi xác nhận Ghi nhận trở về |
| 001 `TrangThaiNguoiCaoTuoiApi` | `List<KhoangTrangThai> khoangTrangThai(long nctId, Set<TrangThaiNct> tt, Instant tu, Instant den)` | FR-027 (c), FR-036 |
| 001 sự kiện | `ThongTinCaNhanDaCapNhat(nctId, truongDaDoi)` | FR-034, Edge Case đổi giới tính |
| 002 `PhanBoGiuongApi` | Khai báo trong `m15.taikhoan.api` dưới dạng SPI, M03 cài; chữ ký giữ nguyên | Tránh vòng M15 ↔ M03 (G-02) |

Các bổ sung đã được ghi vào file `contracts/module-interfaces.md` của 000, 001, 002 kèm dòng "bổ sung theo plan 003".

**Lý do**: Cùng cách plan 001 đã bổ sung `TepApi`, `chuyenMoRong` vào hợp đồng 000.

---

## G-14. Quyền và phạm vi

**Quyết định**: Mỗi lệnh khai báo `@CanQuyen(chucNang, thaoTac)` (002 A-02), `chucNang` là dòng Phụ lục 27:

| Lệnh | Khóa quyền | Phạm vi, thu hẹp thêm ở 003 |
| --- | --- | --- |
| Tạo, sửa, Ngừng hiệu lực khu vực, tầng, phòng, loại phòng, khu nghỉ, buổi, lý do chuyển; tạo, sửa giường | `CAU_HINH_PHONG_GIUONG, TAO/SUA` | Chỉ AC-01 (C³⁵) |
| Tạm ngừng sử dụng, Sử dụng lại | `CAU_HINH_PHONG_GIUONG, SUA` | Chỉ AC-01 (chú thích ³⁵) |
| Đặt, gỡ cách ly phòng | `DAT_GO_CACH_LY_PHONG, TAO` | AC-01, AC-03 toàn viện |
| Phân bổ giường | `PHAN_BO_CHUYEN_GIUONG, TAO` | Hành chính toàn viện; Trưởng tầng: chỉ xét giường thuộc phạm vi (BR-M03-20) |
| Chuyển giường (cả hai endpoint), Hủy phân bổ tương lai | `PHAN_BO_CHUYEN_GIUONG, TAO` | Trưởng tầng: cả người cao tuổi và giường mới thuộc phạm vi (FR-029) |
| Bác sĩ xác nhận ở lại vùng cách ly | `DAT_GO_CACH_LY_PHONG, TAO` | Chỉ AC-03 (FR-024a); AC-01 bị loại ở 003 |
| Xác nhận giường sẵn sàng | `XU_LY_HU_HONG_GIUONG, TAO` | Người thực hiện phải là Trưởng tầng được giao của tầng chứa giường, hoặc Người phụ trách ca của tầng đó tại thời điểm lệnh (`PhamViApi`). AC-01 bị loại dù ô Phụ lục 27 là T, vì chú thích ¹⁵ chỉ nêu Trưởng tầng và Người phụ trách ca (FR-044, US4 #11) |
| Tra cứu lịch sử vị trí | G-10 | |

- **Người phụ trách ca**: quyền theo nhiệm vụ chỉ do 002 cấp (002 FR-019, A-06). Danh sách FR-019a → f của 002 chưa có ngoại lệ cho Xác nhận giường sẵn sàng, nên nếu chưa có ngoại lệ đó, nhân viên vai trò Điều dưỡng hoặc Nhân viên chăm sóc giữ nhiệm vụ này bị lớp vai trò từ chối. Plan báo lại (plan, điểm 1). Thiết kế 003 không tự cấp quyền; chỉ cần 002 thêm một dòng vào `QuyenTheoNhiemVu`.
- `GanPhamViProvider`: `M03.GIUONG` và `M03.PHONG` trả tầng; `M03.PHAN_BO_GIUONG` trả người cao tuổi và tầng của giường.
- Danh sách giường (FR-022) hiển thị người cùng phòng ở mức họ tên, giới tính, mức chăm sóc. DTO không có trường thuộc nhóm `SUC_KHOE`.

**Lý do**: Constitution VI, XI. Các thu hẹp đều có nguồn ở spec hoặc chú thích Phụ lục 27, không thêm quyền.

---

## G-15. Lưu giữ, nhật ký, phân loại trường

**Quyết định**:
- `LuuTruProvider`:
  - `M03.PHAN_BO_GIUONG`: `THEO_HO_SO` (CFG-M15-03), mốc là trạng thái cuối của người cao tuổi; tham chiếu tới gồm `phan_bo_truoc_id`, yêu cầu phê duyệt, đính chính;
  - `M03.LICH_SU_TRANG_THAI_GIUONG`, `M03.LICH_SU_CACH_LY_PHONG`: `KHONG_GAN_HO_SO` (CFG-M15-04). Dòng lịch sử giường có căn cứ là một phân bổ còn trong hạn thì không bị loại (000 FR-051a).
- Danh mục không bị loại bỏ (nhóm 1, còn được lịch sử tham chiếu).
- Hành động nhật ký: `M03_CO_CAU_*`, `M03_GIUONG_CHUYEN`, `M03_PB_PHAN_BO`, `M03_PB_CHUYEN`, `M03_PB_DONG`, `M03_PB_HUY`, `M03_PB_DAU`, `M03_CACH_LY`, `M03_BAN_TRU_*`, `M03_NHAC`, `M03_BAO` (data-model mục 9).
- `PhanLoaiTruongProvider`: các trường của 003 là `CONG_KHAI_NOI_BO`, trừ `lyDo` của chuyển giường kiểm soát lây nhiễm và của đặt cách ly (`SUC_KHOE`, vì có thể nêu tình trạng nhiễm).

---

## G-16. Kiểm thử

**Quyết định** (theo 000 R-14):
- **Đồng thời** (SC-001, FR-020, FR-033): 10 luồng cùng phân bổ một giường cho 10 người trong một giây; 2 luồng chuyển cùng một người; phân bổ và lệnh tài sản đồng thời trên một giường. Kiểm sau cùng: đúng một phân bổ đang hiệu lực mỗi giường, không có khoảng chồng, không có lỗi 500.
- **Cơ sở dữ liệu** (tích hợp MySQL, 000 R-14): cột sinh `UNIQUE`, trigger chống chồng, trigger bất biến, `CHECK` trạng thái giường, `UNIQUE` khu nghỉ.
- **Bảng chuyển**: test tham số hóa đi hết mọi dòng của bảng trạng thái giường và thử mọi cặp ngoài bảng (SC-007).
- **Đồng hồ giả lập** cho CFG-M02-02, CFG-M03-02, 04, 07, 08 và hạn giữ người vắng (SC-011, SC-013, SC-014).
- **Bản giả**: `NguonLuuTruChoPhanBo`, `DemLichDenBanTru` (004), `TinhTrangLayNhiemChoPhanBo` (007), `ThamChieuViTri` (008), `TaiSanGiuongPort` (019), `MucChamSocApi`, `NguoiCaoTuoiApi`, `TrangThaiNguoiCaoTuoiApi` (001, khi chạy 003 độc lập). Mỗi bản giả có công tắc qua `/api/v1/kiem-thu/gia-lap/{module}` ở profile `test`, `demo` như 001.
- **Tên test** theo BR, FR (`BR_M03_02_haiNguoiCungGiuong_nguoiSauBiTuChoi`); truy vết ở [quickstart.md](quickstart.md) mục 4.
- **Client**: Vitest cho bảng giường (dấu, trạng thái), màn chọn giường (không có giường chọn sẵn, Q-307), hiển thị đủ danh sách vi phạm. `flutter_test` cho màn Xác nhận giường sẵn sàng (ô khử khuẩn bắt buộc khi API trả `canKhuKhuan = true`). Không có BR nào của 003 thực hiện ở client.
