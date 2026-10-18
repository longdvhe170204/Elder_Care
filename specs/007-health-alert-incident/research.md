# Research: Sức khỏe, cảnh báo và sự cố (007)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

Mỗi mục gồm Quyết định, Lý do, Phương án đã cân nhắc. Quyết định nền R-01 → R-14 của 000 và các mẫu đã chốt ở 001 → 006 (H-02 bean trạng thái, G-02 / C-02 / D-02 chiều phụ thuộc, C-03 / D-03 khóa, D-17 sự kiện) được dùng lại, không quyết lại. Các quyết định dưới đây là kỹ thuật; chúng không đổi nghiệp vụ (constitution IX). Chỗ spec chưa rõ hoặc mâu thuẫn được ghi ở plan, mục "Điểm cần báo lại".

---

## S-01. Vị trí trong backend

**Quyết định**: Feature 007 trải trên hai module của `docs/business/nghiep-vu.md`: M06 – Sức khỏe (mục 10) và M05 – Sự cố và cảnh báo (mục 9). Theo 000 R-02, mỗi module là một gói Modulith riêng:

| Gói | Nội dung | Mục spec |
| --- | --- | --- |
| `m06.danhmuc` | Danh mục chỉ số, thành phần, nhóm "sinh hiệu" | A (FR-001, FR-002) |
| `m06.lichdo` | Lịch đo, `ChuoiMocDo`, bean `NguonCongViecPort` loại `LAN_DO` | A (FR-003 → FR-006) |
| `m06.chiso` | Bản ghi chỉ số, so ngưỡng, đo lại, `ChiSoPort`, `NgoaiTuyenHandler` | B (FR-010 → FR-019) |
| `m06.nguong` | Phiên bản ngưỡng, nhắc thiếu ngưỡng cá nhân | C (FR-020 → FR-025) |
| `m06.giayphep` | Giấy phép cơ sở, số liên hệ của viện, `GiayPhepCoSoApi` | J (FR-070, FR-071, FR-075) |
| `m06.kham` | Bản ghi khám và điều trị | J (FR-072 → FR-074) |
| `m06.trangthai`, `m06.lich` | Bean trạng thái; job | FR-006, FR-022, FR-084 |
| `m05.canhbao` | Cảnh báo: khóa loại, gộp, vòng đời, leo thang, bàn giao, `CanhBaoDocPort` | D (FR-026 → FR-038a) |
| `m05.xuhuong` | Danh mục quy tắc xu hướng; sụt cân, bỏ lỡ lần đo, mất ngủ | E (FR-039 → FR-039c) |
| `m05.suco` | Sự cố, diễn biến, đổi mức, đóng, hủy, đổi loại | F (FR-040 → FR-046b) |
| `m05.khancap` | Thẻ thông tin khẩn cấp, đối chiếu nguyện vọng, chuyển viện, bản tóm tắt | G (FR-047 → FR-050) |
| `m05.nguykich` | Dấu nguy kịch, xác nhận lại nguyện vọng, lựa chọn thực hiện | G1 (FR-050a → FR-050g) |
| `m05.nga` | Khoảng theo dõi sau ngã, bean `NguonCongViecPort` loại `THEO_DOI_SAU_NGA` | H (FR-051, FR-052) |
| `m05.laynhiem` | Danh sách tiếp xúc, tra cứu truy vết, khoanh vùng | I (FR-060 → FR-066) |
| `m05.trangthai`, `m05.lich`, `m05.api` | Bean trạng thái, `MucViecSauQuaDoiPort`; job; giao diện công khai | FR-038, FR-084 |

Chiều mã giữa hai module: `m05 → m06` (thẻ khẩn cấp, bản tóm tắt chuyển viện, quy tắc sụt cân đọc chỉ số; bean `NguonCongViecPort` của `m05` không cần `m06`). `m06` không gọi `m05`: kết quả so ngưỡng đi sang `m05` bằng sự kiện `m06.api.ChiSoDaXacNhan` sau commit.

**Lý do**: Constitution X chia gói theo M01–M15. BR-M05-xx và BR-M06-xx thuộc hai module khác nhau; một chiều `m05 → m06` khớp nghiệp vụ (cảnh báo dùng dữ liệu sức khỏe, không ngược lại).

**Phương án đã cân nhắc**:
- Một gói `m05` cho cả feature: bị loại vì đặt dữ liệu M06 (giấy phép cơ sở mà 002 cần) trong module M05, sai ranh giới constitution X.
- `m06` gọi đồng bộ `m05` để tạo cảnh báo trong cùng transaction ghi chỉ số: tạo vòng `m05 ↔ m06`; SC-002 cho phép cảnh báo xuất hiện trong 10 giây nên sự kiện sau commit là đủ.

---

## S-02. Chiều phụ thuộc giữa module

**Quyết định**: Theo mẫu 005 C-02, 006 D-02: module làm plan trước khai báo SPI, module làm plan sau cài.

| Cặp | Chiều mã | Cách giữ một chiều |
| --- | --- | --- |
| 007 → 000, 002 | 007 dùng | `NhatKyApi`, `ThamSoApi`, `DinhChinhApi`, `TepApi`, `QuyenApi`, `PhamViApi`, `QuyenXemTruongApi`, `NguoiDuyetApi`, `NguoiDungHienTai`; cài `NgoaiTuyenHandler`, `DinhChinhDuocProvider`, `PhanLoaiTruongProvider`, `LuuTruProvider`, `GanPhamViProvider`, `QuyenTepProvider`. `m06` cài `GiayPhepCoSoApi` (khai báo ở `m15.taikhoan.api`) |
| 007 → 001 | 007 dùng | Cài `ThamGiaChuyenTrangThai` (hai bean, S-17); gọi `TrangThaiNguoiCaoTuoiApi.thucHien(CHUYEN_VIEN)` (đã có trong bảng người gọi của 001), `NguyenVongApi.hieuLuc`, `ghiPhienBan`; đọc `NguoiCaoTuoiApi`, `MucSucKhoeApi`, `DanhGiaApi.luuYHienHanh`, `BanDongYApi.baoGom`, `khoangTrangThai`. Cài `ChiSoApi.ganNhat` (001 khai báo); 001 nghe `SuCoDaGhiNhan` |
| 007 → 003 | Đọc, cài | Đọc `LichSuViTriApi.cungPhong`, `theoNguoi`, `ViTriApi.hienTai`, `CoCauViTriApi`; cài `TinhTrangLayNhiemChoPhanBo` (khai báo ở `m03.api`); nghe `PhanBoDaDinhChinh`, `PhongDaDatCachLy` |
| 007 → 004 | Cài, gọi | Cài `MucViecSauQuaDoiPort` (`CANH_BAO_SU_CO`); gọi `DanhSachViecSauQuaDoiApi.capNhat`; dùng mã điều kiện `KT_D_CANH_BAO_SU_CO` |
| 007 → 005 | Cài, gọi | Cài `NguonCongViecPort` (hai bean), `ChiSoPort`, `CanhBaoDocPort`; gọi `CongViecDoChiSoApi.daXacNhan`, `NguonCongViecApi.nguonBiHuy`, `nguonDaDoi` (MANDATORY); đọc `CongViecThongKeApi.ketQuaTheoKhoang`; nghe `YeuCauCanhBao`, `YeuCauDongCanhBao` và hai sự kiện bổ sung (S-10) |
| 007 → 006 | Đọc, nghe | `DonThuocApi.thuocDangDung`; nghe `m07.api.YeuCauCanhBao`, `YeuCauDongCanhBao` |
| 007 ↔ 008, 009, 011, 012, 013, 014, 016 | Các module này → 007 | Họ làm plan sau. 007 cần dữ liệu của 008 (ca, phân công, Bác sĩ trực), 009 (nhật ký gửi), 012 (người liên hệ chính, người đại diện, lượt thăm), 014 (điểm danh hoạt động) qua SPI khai báo ở `m05.api` (module-interfaces mục 2.1). 008 gọi `CanhBaoBanGiaoApi`; 013, 014 gọi `SuCoLenhApi`; 011, 014 phát `m05.api.YeuCauTaoCanhBao`; 012, 014 đọc `KhoanhVungApi`; 016 đọc các API thống kê |
| 007 → 009 | Sự kiện | Mọi thông báo là sự kiện ghi cùng transaction qua outbox (R-09, S-19) |

**Lý do**: Modulith `verify()` từ chối vòng (000 R-02). 008 phải gọi 007 trong transaction xác nhận bàn giao (FR-037 chuyển người phụ trách "khi ca sau xác nhận"); 007 lại cần dữ liệu ca của 008. SPI ở `m05.api` cắt vòng này.

**Phương án đã cân nhắc**: Dùng lại `NhanSuCaPort` (005) hoặc `DieuDuongCaPort` (006) cho dữ liệu ca: bị loại vì 007 cần thêm Bác sĩ trực, Trưởng tầng được giao, giờ kết thúc ca, ca chưa xác nhận bàn giao, phân công trong khoảng truy vết; mở rộng SPI của module khác làm hợp đồng của họ phụ thuộc nhu cầu của 007.

---

## S-03. Khóa và đồng thời

**Quyết định**:
- **Thứ tự khóa toàn hệ thống** (nối 006 D-03): `nguoi_cao_tuoi` (001) → `luu_tru_nct` (004) → `vi_tri_nct`, `giuong` (003) → `cham_soc_nct` (005) → `thuoc_nct` (006) → `chi_so_nct` (m06) → `theo_doi_nct` (m05) → dòng nghiệp vụ của 007 (cảnh báo, sự cố, dấu nguy kịch, danh sách tiếp xúc). Bean trạng thái của 007 chạy sau 006 theo `@Order` của 001.
- **Gộp cảnh báo** (DBR-18, FR-030, SC-003): listener của mọi yêu cầu tạo cảnh báo chạy trong transaction riêng, khóa `theo_doi_nct` của người đó (`SELECT … FOR UPDATE`, tạo dòng nếu chưa có), rồi tìm cảnh báo đang mở theo `(nct_id, khoa_loai)`. Có thì gộp; không thì thêm. Lưới an toàn: cột sinh `khoa_mo = IF(trang_thai IN ('MOI','LEO_THANG','DA_TIEP_NHAN','DANG_XU_LY'), khoa_loai, NULL)` với `UNIQUE (nct_id, khoa_mo)`; vi phạm thì thử lại một lần (lần thử lại thấy cảnh báo đang mở và gộp).
- **Lệnh trên một cảnh báo, sự cố** (tiếp nhận, giao, xử lý, đóng, đổi mức, chuyển sự cố): khóa dòng cảnh báo hoặc dòng `su_co_xu_ly`, kiểm tra lại trên trạng thái mới. Lần tiếp nhận thứ hai trả 409 `DA_CO_NGUOI_TIEP_NHAN` kèm người và thời điểm (dùng cho "Xác nhận và tiếp nhận" của 009 FR-026a). Không dùng `If-Match`, giống 005 C-03.
- **Đo lại** (FR-016, FR-017): lệnh đo lại và job hết hạn đo lại cùng khóa dòng `ban_ghi_chi_so` đang `CHO_DO_LAI`; bên đến sau thấy `thoi_diem_xac_nhan` đã có và dừng (lệnh đo lại khi đó trả 409 `DA_XAC_NHAN_KHONG_DO_LAI`).
- **Phiên bản ngưỡng, lịch đo, dấu nguy kịch, khoảng theo dõi sau ngã**: khóa `chi_so_nct` (ngưỡng cá nhân, lịch đo) hoặc `theo_doi_nct` (dấu nguy kịch, ngã, tiếp xúc) của người đó; ngưỡng mặc định khóa dòng kỹ thuật `nguong_khoa (chi_so_id)`. Mỗi bất biến "tối đa một" có thêm cột sinh `UNIQUE` (data-model mục 12).
- **Kích hoạt khẩn cấp đồng thời** (Edge Case, FR-041): lệnh ghi sự cố Khẩn cấp khóa `theo_doi_nct`, tìm sự cố Khẩn cấp đang mở; có thì trả 409 `DA_CO_SU_CO_KHAN_CAP_MO` kèm sự cố đó, trừ khi thân lệnh có `taoMoiDuDaCo = true`. Người sau chọn "tham gia xử lý" bằng `POST /su-co/{id}/tham-gia-xu-ly`. Không tự gộp.

**Lý do**: SC-003 (10 phát sinh đồng thời → một cảnh báo), DBR-18, DBR-34 và Edge Case hai người kích hoạt khẩn cấp đều là kiểm tra trên tập bản ghi của một người, cần tuần tự theo người.

**Phương án đã cân nhắc**: Chỉ dựa vào `UNIQUE` và bắt lỗi trùng: đủ cho DBR-18 nhưng không đủ cho số lần gộp chính xác khi hai bên cùng đọc "chưa có" rồi một bên thử lại; khóa dòng người làm số lần đúng ngay lần đầu.

---

## S-04. Danh mục chỉ số và khoảng hợp lệ vật lý (FR-001, FR-002, CFG-M06-03)

**Quyết định**:
- Bảng `chi_so` (nhóm 1) giữ cấu trúc: mã, tên, đơn vị, thứ tự, `sinh_hieu`, trạng thái hiệu lực; bảng con `chi_so_thanh_phan` (mã thành phần, tên, số chữ số thập phân). Sáu chỉ số khởi tạo theo FR-001; huyết áp có hai thành phần `TAM_THU`, `TAM_TRUONG`.
- **Khoảng hợp lệ vật lý nằm ở CFG-M06-03**, kiểu `BANG` của 000 R-07: `{ "<maChiSo>": { "<maThanhPhan>": { "min": …, "max": … } } }`. Migration nạp giá trị cho sáu chỉ số khởi tạo. Quản lý viện đổi khoảng qua màn tham số của 000; lịch sử, giá trị trước/sau, lý do do `lich_su_tham_so` giữ (FR-002, BR-M15-04). Không lưu khoảng ở bảng `chi_so` để có một nguồn duy nhất (constitution XIII).
- Mỗi lần ghi đọc CFG-M06-03 một lần (000 FR-018). Chỉ số mới thêm vào danh mục mà chưa có dòng trong CFG-M06-03 thì lệnh ghi trả 409 `CHUA_CO_KHOANG_HOP_LE`; màn danh mục hiện dấu "chưa có khoảng hợp lệ" và liên kết tới màn tham số.
- Đổi khoảng chỉ áp cho lần ghi sau (FR-002): bản ghi đã lưu không bị xét lại; lần đo lại của bản ghi đang chờ dùng khoảng hiện hành lúc ghi (Edge Case CHK041).
- Danh mục loại sự cố, quy tắc xu hướng, loại diễn biến (có dấu "biện pháp hồi sức") cũng là nhóm 1 theo mẫu 000 (tạo, sửa, Ngừng hiệu lực; không xóa khi đã tham chiếu).

**Lý do**: CFG-M06-03 ghi "Mặc định: theo danh mục chỉ số"; constitution XIII đòi mọi CFG có lịch sử và đọc lúc chạy.

**Phương án đã cân nhắc**: Lưu khoảng trên `chi_so_thanh_phan` và coi bảng là "cấu hình": không có lịch sử giá trị theo thời điểm như `lich_su_tham_so`, và tách CFG-M06-03 khỏi cơ chế chung của 000.

---

## S-05. Lịch đo (mục A, FR-003 → FR-006)

**Quyết định**:
- Bảng `lich_do` (nhóm 2) và `lich_do_chi_so` (một lịch nhiều chỉ số). Tần suất lưu bằng `kieu_tan_suat` (`MOC_GIO`, `MOI_N_GIO`, `THEO_THU`) và JSON tham số (mốc giờ, N, mốc gốc, các thứ). Nguồn chỉ có `CHI_DINH_BAC_SI` (Q-307); không có cột "sự cố nguồn".
- Hàm thuần `ChuoiMocDo(lichDo, tu, den)` trả các mốc đo trong khoảng. Hàm chỉ dùng để: kiểm tra căn cứ `LAN_DO` khi Người phụ trách ca lập công việc (bean `NguonCongViecPort` của `m06`: lịch Hiệu lực tại mốc, người khớp, mốc thuộc chuỗi, trả "theo chỉ định" để 005 buộc mức Bắt buộc theo mức quan trọng của lịch, 005 FR-023); hiển thị lịch đo dự kiến. **Không có bản ghi mốc đo** và không job nào sinh công việc (FR-004, Q-306).
- Lệnh: Tạo (`HIEU_LUC` nếu bắt đầu không sau thời điểm lệnh, ngược lại `CHO_HIEU_LUC`), Thay đổi lịch đo (lịch mới `thay_the_cho`, lịch cũ `DA_NGUNG` tại thời điểm hiệu lực của lịch mới, cùng transaction, FR-005), Ngừng lịch đo. Job theo phút chuyển `CHO_HIEU_LUC → HIEU_LUC` và `HIEU_LUC → HET_HAN`. Trigger chặn đổi mọi cột nội dung (chỉ số, tần suất, bắt đầu, kết thúc, mức quan trọng) ở mọi trạng thái; chỉ cột trạng thái, `ngung_luc`, `ly_do_ngung` đổi được (FR-005).
- Tác động lên 005 (FR-022 của 005; plan điểm báo lại 2):
  - Thay đổi lịch đo: gọi `NguonCongViecApi.nguonDaDoi(LAN_DO, lichCuId)` (MANDATORY): 005 báo Người phụ trách ca, Trưởng tầng; không hủy công việc.
  - Ngừng lịch đo, trạng thái cuối: gọi `NguonCongViecApi.nguonBiHuy(LAN_DO, lichId, lyDo)`: 005 hủy công việc Chưa đến hạn theo 005 FR-022 ("lịch đo ngừng").
  - Phát `LichDoDaDoi(lichDoId, nctId)` (tên 005 đã đề xuất) để 005 dùng làm khóa bỏ trùng.
- Quyền: Bác sĩ tạo, đổi, ngừng; Quản lý viện, Điều dưỡng xem; Trưởng tầng xem trong phạm vi (Phụ lục 27 "P", Q-279).

**Lý do**: Q-306 cấm sinh công việc; lịch đo chỉ là căn cứ. Hàm thuần đủ cho kiểm tra căn cứ và hiển thị.

**Phương án đã cân nhắc**: Lưu sẵn bảng mốc đo N ngày: trái Q-306 và không cần thiết vì không có trạng thái theo mốc.

---

## S-06. Ghi chỉ số, so ngưỡng, đo lại (mục B, FR-010 → FR-019, BR-M06-03, 04, 06)

**Quyết định**:
- **Ba đường ghi**, cùng một service `GhiChiSoService`:
  1. Từ công việc đo: 005 gọi `ChiSoPort.ghi(...)` (MANDATORY) trong transaction ghi kết quả công việc; kết quả `DA_LUU(banGhiId)`, `BI_CHAN(lyDo, CFG-M06-03)` hoặc `CHO_DO_LAI(banGhiId)` giữ đúng hợp đồng 005.
  2. Đo ngoài lịch: `POST /nguoi-cao-tuoi/{nctId}/chi-so`.
  3. Ngoại tuyến: `NgoaiTuyenHandler` loại `M06.BAN_GHI_CHI_SO` qua khung 002 A-11; bản ghi lưu `thoi_diem_thiet_bi`, so ngưỡng lúc đồng bộ, nhãn `GHI_NGOAI_TUYEN` (Edge Case). Lần đo đầu và lần đo lại trong cùng lô đồng bộ được xử lý theo thứ tự lô.
- **Kiểm tra** theo thứ tự: quyền ghi (`CHI_SO, GHI` trong phạm vi), thời điểm đo không sau thời điểm ghi (FR-012), mỗi thành phần trong khoảng CFG-M06-03 (FR-011; sai thì 422 `NGOAI_KHOANG_VAT_LY` kèm khoảng, không lưu). Nhãn "ghi nhận muộn" theo CFG-M04-06 và 005 FR-035.
- **So ngưỡng** bằng hàm thuần `SoNguong(giaTri, phienBan)`: từng thành phần → `BINH_THUONG | CANH_BAO | NGUY_HIEM | CHUA_CO_NGUONG`; cận để trống thì không so phía đó; bằng cận không vượt (FR-024). Bản ghi xếp theo thành phần nặng nhất (FR-013). Phiên bản áp dụng chọn theo FR-024 tại `thoi_diem_do` (ngưỡng cá nhân Hiệu lực, không có thì mặc định); id phiên bản và kết quả từng thành phần lưu trên bản ghi nên đổi ngưỡng sau đó không đổi kết quả (FR-023).
- **Trạng thái xác nhận** (bản ghi nhóm 3 có bản nháp, 000 R-05):
  - `BINH_THUONG`, `CANH_BAO`, `CHUA_CO_NGUONG`, hoặc `NGUY_HIEM` có `xuLyNgay = true`: xác nhận ngay (`thoi_diem_xac_nhan` = thời điểm ghi).
  - `NGUY_HIEM` không có `xuLyNgay`: `CHO_DO_LAI`, `han_do_lai = thoi_diem_do + CFG-M06-04`; chưa phát gì cho cảnh báo.
  - Lần đo lại: `POST /ban-ghi-chi-so/{id}/do-lai`, người có quyền ghi chỉ số cho người đó (FR-016). Lần đo lại là bản ghi mới `do_lai_cho = id`, xác nhận ngay, không yêu cầu đo lại thêm; bản ghi đầu được xác nhận cùng lúc với nhãn `DA_DO_LAI` (trigger cho phép đúng một lần đặt `thoi_diem_xac_nhan`). Lần đo lại ngoài khoảng vật lý bị chặn như lần mới, bản ghi đầu vẫn chờ (Edge Case).
  - Job theo phút: `CHO_DO_LAI` có `han_do_lai <= bây giờ` → xác nhận với nhãn `CHUA_DO_LAI` (FR-017). Chạy bù xác nhận ngay (FR-084 b).
- **Sau khi xác nhận** (trong transaction xác nhận): nếu có công việc nguồn thì gọi `CongViecDoChiSoApi.daXacNhan(congViecId, banGhiXacNhanId, thoiDiemDo, nguoiDoId)` (FR-018); phát `ChiSoDaXacNhan(banGhiId, nctId, chiSoId, muc, phienBanNguongId, congViecId, thoiDiemDo, nhan)` sau commit. Listener của `m05.canhbao` tạo hoặc gộp cảnh báo: `CANH_BAO` → Trung bình, `NGUY_HIEM` → Khẩn cấp, khóa loại `VUOT_NGUONG:<chiSo>` (FR-014, FR-028). Với lần đo lại, bản ghi nguồn của cảnh báo là lần đo lại; với `CHUA_DO_LAI` là chính bản ghi đầu.
- **Đính chính** (FR-019): `DinhChinhDuocProvider` loại `M06.BAN_GHI_CHI_SO`; `daXacNhan` sai với bản ghi chờ đo lại (CHK040); `kiemTraNoiDung` chạy lại kiểm tra khoảng vật lý; `tangKhuVuc` là tầng của người cao tuổi lúc đo. `sauKhiDinhChinh`: so giá trị đính chính với **cùng phiên bản ngưỡng** đã áp cho bản gốc; phát `ChiSoDaDinhChinh(banGhiGocId, nctId, mucGoc, mucMoi)`; `m05` thông báo người phụ trách các cảnh báo có nguồn là bản ghi đó "nguồn đã được đính chính" (Nhẹ) và không đóng; nếu mức mới vượt mà mức gốc không vượt (hoặc nặng hơn) thì tạo hoặc gộp cảnh báo theo mức mới. Xu hướng dùng giá trị hiện hành qua `DinhChinhApi.hienHanh` kể từ thời điểm đính chính.
- Cảnh báo Khẩn cấp từ chỉ số không tạo sự cố (FR-047a).

**Lý do**: BR-M06-04 cần trạng thái chờ trước khi xác nhận; mẫu nhóm 3 có bản nháp của 000 R-05 cho đúng điều này mà không mở `UPDATE` sau xác nhận.

**Phương án đã cân nhắc**: Lưu lần đo đầu ở bảng nháp riêng rồi chép sang bảng chính khi xác nhận: mất bản ghi "đã đo lại" mà FR-016 yêu cầu giữ trong lịch sử.

---

## S-07. Ngưỡng cảnh báo (mục C, FR-020 → FR-025)

**Quyết định**:
- `nguong_phien_ban` (nhóm 2): phạm vi `CO_SO | CA_NHAN`, `nct_id` (rỗng với cơ sở), `chi_so_id`, `hieu_luc_tu`, `hieu_luc_den` (đặt khi bị thay hoặc ngừng), trạng thái, người thiết lập, lý do; `nguong_thanh_phan` (bốn cận `DECIMAL`, mỗi cận cho phép `NULL`).
- Kiểm tra FR-021 bằng hàm thuần `KiemTraNguong`: mọi cận có giá trị nằm trong khoảng CFG-M06-03; thứ tự nghiêm ngặt dưới nguy hiểm < dưới cảnh báo < trên cảnh báo < trên nguy hiểm, bỏ qua cận trống. Sai thì 422 `NGUONG_KHONG_HOP_LE` kèm cặp vi phạm.
- Lệnh "Thiết lập ngưỡng" (cùng transaction, sau khi khóa `chi_so_nct` hoặc `nguong_khoa`): phiên bản `CHO_HIEU_LUC` cũ của cùng (phạm vi, chỉ số) → `DA_HUY`; nếu hiệu lực ngay thì phiên bản `HIEU_LUC` → `HET_HIEU_LUC` với `hieu_luc_den` = thời điểm lệnh; nếu hiệu lực sau thì để job theo phút làm tại `hieu_luc_tu`. Bắt buộc lý do khi đã có phiên bản trước (FR-020).
- Bất biến FR-022 "tối đa một Hiệu lực" bằng cột sinh `khoa_hieu_luc` `UNIQUE`; thêm "tối đa một Chờ hiệu lực" (suy ra từ cột Tác động của bảng FR-022) bằng `khoa_cho`. Trigger chặn sửa cột cận và `hieu_luc_tu`.
- Tra phiên bản áp dụng tại thời điểm `t`: truy vấn `(pham_vi, nct_id, chi_so_id, hieu_luc_tu <= t)` có `hieu_luc_den IS NULL OR hieu_luc_den > t` và trạng thái `HIEU_LUC`, `HET_HIEU_LUC`; chỉ mục `(nct_id, chi_so_id, hieu_luc_tu)`. Một phiên bản Đã hủy chưa từng hiệu lực nên không bao giờ được chọn.
- Nhắc thiếu ngưỡng cá nhân (FR-025, BR-M06-02): job hằng ngày (00:15) lấy người Đang lưu trú có mốc Hoàn tất tiếp nhận (lần chuyển vào Đang lưu trú đầu tiên của lượt hiện tại, qua `TrangThaiNguoiCaoTuoiApi.khoangTrangThai`) cũ hơn CFG-M06-01 và không có phiên bản cá nhân `HIEU_LUC`, `CHO_HIEU_LUC` nào; với mỗi người chưa có `chi_so_nct.da_nhac_thieu_nguong_luc` thì phát `NhacThieuNguongCaNhan(nctId)` (Nhẹ, nhóm `BAC_SI_TOAN_VIEN`) và đặt cột. Danh sách "chưa có ngưỡng cá nhân" tính lúc đọc. Người được thiết lập ngưỡng rồi sau đó Ngừng thì cột được xóa để nhắc lại khi lại thiếu (suy ra từ "giữ trong danh sách cho tới khi có ngưỡng").

**Lý do**: Phiên bản và thời điểm hiệu lực là dữ liệu nghiệp vụ (2.4 "Phiên bản"); ngưỡng mặc định do Bác sĩ thiết lập, không phải CFG (spec Assumptions CHK047).

---

## S-08. Cảnh báo: khóa loại, vòng đời, hạn tiếp nhận, leo thang (mục D, FR-026 → FR-038a)

**Quyết định**:
- **Khóa loại** (FR-028) do hàm thuần `KhoaLoai.tu(nguon)` dựng, dạng chuỗi:

  | Nguồn | Khóa |
  | --- | --- |
  | Vượt ngưỡng | `VUOT_NGUONG:<chiSoId>` |
  | Xu hướng | `XU_HUONG:<maQuyTac>` (bỏ lỡ lần đo: `BO_LO_LAN_DO:<chiSoId>`) |
  | Công việc Bắt buộc quá hạn | `QUA_HAN:<loaiCongViecId>`; công việc có căn cứ `LAN_DO`: `BO_LO_LAN_DO:<chiSoId>` (FR-039b) |
  | Thuốc | `THUOC:<loai>:<donThuocId>`; `XUNG_DOT_DI_UNG`, `TU_CHOI_LIEN_TIEP`: `THUOC:<loai>:<thuocGocId>` (theo `khoa` trong sự kiện của 006) |
  | Uống nước, ăn kém, tâm trạng, chưa đối chiếu thuốc, nguy cơ cô lập | `<LOAI>` |
  | Thủ công | `THU_CONG:<loaiSuCoId>` (danh mục loại do người tạo chọn) |
  | Nguy kịch | `NGUY_KICH:<dauNguyKichId>` (không gộp, BR-M05-15) |

- **Bản ghi nguồn**: bảng `nguon_canh_bao` (nhóm 3) một dòng mỗi phát sinh: loại nguồn, id nguồn, mức của phát sinh, thời điểm, `da_xu_ly_luc`, lý do xử lý. Số lần = số dòng. Đây cũng là đầu vào của báo lặp lại (FR-038a) và của việc tự đóng (FR-032).
- **Mức, hạn tiếp nhận, người phụ trách ban đầu** (FR-031):
  - Nhẹ: `han_tiep_nhan` = giờ kết thúc ca hiện tại của tầng (qua `NhanSuCanhBaoPort.caChua(tang, t)`); tạo sau mốc CFG-M09-04 trước giờ kết ca thì hạn là hết ca kế tiếp (CHK020).
  - Trung bình: tạo, nâng mức hoặc leo thang + CFG-M05-01.
  - Khẩn cấp: không có hạn leo thang; `han_tiep_nhan = NULL`, mọi người nhận được báo đồng thời.
  - Người phụ trách ban đầu: Điều dưỡng phụ trách người đó trong ca (`NhanSuCanhBaoPort.dieuDuongPhuTrach`), không có thì Người phụ trách ca của tầng (`PhamViApi.nguoiPhuTrachCa`). Tầng của người vắng mặt là tầng của giường đang giữ, không còn giường thì tầng của phân bổ gần nhất (`ViTriApi`, `LichSuViTriApi.theoNguoi`). Vắng mặt không dừng hạn (CHK006).
- **Chuỗi leo thang** (FR-033): cấp 0 người phụ trách ban đầu; cấp 1 Trưởng tầng được giao của tầng và mọi Bác sĩ trực; cấp 2 Quản lý viện. Job theo phút (`ViecTheoPhutM05`) lấy cảnh báo `MOI`, `LEO_THANG` có `han_tiep_nhan <= bây giờ`, không `dung_leo_thang`; khóa dòng, lên **một** cấp (FR-084 a), bỏ qua cấp không có ai (hỏi `NhanSuCanhBaoPort`), đặt hạn mới = bây giờ + CFG-M05-01, thêm `lich_su_canh_bao` (cấp cũ, cấp mới, nhóm người nhận), phát `CanhBaoDaLeoThang`. Cấp 1 vẫn gửi cho người cấp 0 nếu người đó là Trưởng tầng (CHK027). Cấp 2 mà vẫn quá hạn: đặt `leo_thang_toi_da_luc`, không lên nữa.
- **Nhắc leo thang tối đa** (FR-034, Q-248, Q-266): job theo phút phát `NhacLeoThangToiDa(canhBaoId, soThuTu)` khi `leo_thang_toi_da_luc + k × khoảng <= bây giờ` (khoảng = CFG-M05-01 với Trung bình; hết mỗi ca của tầng với Nhẹ); `so_lan_nhac_toi_da` tăng; khóa sự kiện `(canhBao, NHAC_LEO_THANG_TOI_DA, soThuTu)`. Lần nhắc ghi ở `lan_nhac_canh_bao`, không vào `lich_su_canh_bao` hay số lần leo thang. Dừng khi tiếp nhận hoặc đóng.
- **Nhẹ khi hết ca** (FR-033): job theo phút xét ca của tầng vừa kết thúc; cảnh báo Nhẹ chưa tiếp nhận lên một cấp (Nhẹ không có hạn theo CFG-M05-01; cấp kế tiếp có hạn là hết ca kế tiếp). Việc ghi vào bàn giao do người lập bàn giao làm (FR-037, Q-307).
- **Tiếp nhận**: người có quyền FR-036; Quản lý viện chỉ khi `LEO_THANG` cấp 2. Lưu `tiep_nhan_luc`, `muc_luc_tiep_nhan` (FR-081); dừng leo thang. "Giao người phụ trách" chỉ Quản lý viện khi là người phụ trách; người được giao phải có quyền với người cao tuổi (`QuyenApi` với `CANH_BAO, XU_LY`).
- **Đổi mức** (bảng FR-029): Nâng/Hạ có lý do; hạ từ Khẩn cấp chỉ Bác sĩ. Nếu `MOI`, `LEO_THANG`: hạn tính lại từ thời điểm đổi. Nâng thì phát thông báo theo mức mới. Gộp phát sinh mức cao hơn áp cùng tác động (FR-030).
- **Báo lặp lại** (FR-038a): sau mỗi lần gộp phát sinh mức Trung bình, hàm thuần `DemLap24h(nguon, t)` đếm dòng `nguon_canh_bao` mức Trung bình trong `(t − 24h, t]`; đạt CFG-M05-02 thì phát `CanhBaoLapLai(canhBaoId, soLan)` cho người phụ trách và Bác sĩ trực. Không đề xuất. Mỗi phát sinh tiếp theo khi điều kiện còn đúng gửi lại (khóa theo dòng nguồn).
- **Đóng**: kết quả bắt buộc; nguồn là công việc Bắt buộc quá hạn thì `lyDo` bắt buộc (BR-M04-06). Cảnh báo nguy kịch thêm điều kiện FR-050d (S-13).
- **Chuyển sự cố**, **Kích hoạt khẩn cấp từ cảnh báo**: tạo sự cố liên kết trong cùng transaction (S-11, S-12); lệnh kích hoạt ghi lần lượt ba dòng lịch sử Tiếp nhận, Bắt đầu xử lý, Chuyển sự cố.
- **Trạng thái cuối**: bean đặt `dung_leo_thang = true` cho mọi cảnh báo đang mở của người đó (FR-038); job bỏ qua.

**Lý do**: Spec đặt các mốc theo phút (SC-004 ≤ 1 phút); job theo phút với khóa dòng và cột "đã làm" đáp ứng NFR-04 (không gửi trùng khi chạy lại).

**Phương án đã cân nhắc**: Lên lịch từng hạn bằng bộ hẹn giờ trong bộ nhớ: mất khi khởi động lại; vẫn cần job quét để chạy bù.

---

## S-09. Yêu cầu tạo và đóng cảnh báo từ module khác (FR-026 (b), FR-032)

**Quyết định**:
- Listener sau commit (`@ApplicationModuleListener`, transaction riêng) cho:
  - `m04.api.YeuCauCanhBao`, `m04.api.YeuCauDongCanhBao` (005);
  - `m07.api.YeuCauCanhBao`, `m07.api.YeuCauDongCanhBao` (006);
  - `m05.api.YeuCauTaoCanhBao`, `m05.api.YeuCauDongCanhBaoNguon` (khai báo ở `m05.api` cho 011, 014 và module làm plan sau);
  - `m06.api.ChiSoDaXacNhan`, `ChiSoDaDinhChinh` (S-06).
- Mỗi sự kiện mang khóa `loại + id + mốc` (005, 006); bảng `su_kien_canh_bao_da_nhan (khoa UNIQUE)` chống xử lý trùng khi outbox giao lại (NFR-04).
- Người cao tuổi đã ở trạng thái cuối: yêu cầu tạo bị bỏ qua và ghi nhật ký kỹ thuật (bảng FR-029 dòng Tạo: "chưa ở trạng thái cuối").
- **Yêu cầu đóng** (FR-032): tìm mọi dòng `nguon_canh_bao` khớp (loại nguồn, id nguồn) của cảnh báo đang mở hoặc `CHUYEN_SU_CO`; đặt `da_xu_ly_luc`, `ly_do_xu_ly` (dòng nhóm 3 có cột xử lý một lần; trigger chỉ cho đặt từ `NULL`); thêm dòng `lich_su_canh_bao` "nguồn đã được xử lý". Nếu mọi nguồn đã xử lý:
  - cảnh báo đang mở → `DA_DONG`, người đóng Hệ thống, kết quả "nguồn đã được xử lý: <lý do>";
  - cảnh báo `CHUYEN_SU_CO` → giữ nguyên; thêm diễn biến loại `NGUON_DA_XU_LY` (người ghi Hệ thống) vào sự cố liên kết; thông báo người xử lý sự cố (Nhẹ) trừ khi sự cố đã `DA_DONG`, `DA_HUY` (CHK055).
- Đính chính bản ghi chỉ số không là "nguồn đã được xử lý" (FR-019).
- Yêu cầu tạo của 005 cho công việc có căn cứ `LAN_DO` được ghi với khóa `BO_LO_LAN_DO:<chiSo>` (FR-039b a); 005 cần đưa `loaiCanCu`, `canCuId` vào payload (plan, mục Đồng bộ 2).

**Lý do**: Mẫu 005 C-13, 006 D-17. Một bảng nguồn cho cả gộp, đóng và báo lặp lại giữ đúng một cách đếm.

---

## S-10. Quy tắc xu hướng (mục E, FR-039 → FR-039c, BR-M05-04)

**Quyết định**:
- `quy_tac_xu_huong` (nhóm 1): mã cố định `SUT_CAN`, `BO_LO_LAN_DO`, `MAT_NGU_KEO_DAI`, `TU_CHOI_THUOC`; `bat`, `muc`, mã CFG (tham số vẫn ở `ThamSoApi`). Quản lý viện bật/tắt, đổi mức có lý do, nhật ký trước/sau (FR-039). Quy tắc tắt thì không tạo cảnh báo; bật lại không xét bù các phát sinh trong lúc tắt.
- **Sụt cân** (FR-039a): listener `ChiSoDaXacNhan` và `ChiSoDaDinhChinh` của chỉ số cân nặng. Hàm thuần `SutCan(lanDo, cacLanTruoc, cfg)`: lấy giá trị hiện hành cao nhất trong `[t − N ngày, t)` (qua `ChiSoApi.trongKhoang` + `DinhChinhApi.hienHanh`); không có lần nào thì không chạy (CHK042); giảm ≥ tỷ lệ thì tạo hoặc gộp `XU_HUONG:SUT_CAN` nêu hai lần đo, và phát `SutCanPhatHien(nctId, banGhiCaoNhatId, banGhiMoiId)` cho 011 (BR-M08-05, Q-283).
- **Bỏ lỡ lần đo theo lịch** (FR-039b, Q-75): nguồn là (a) `YeuCauCanhBao(CONG_VIEC_BAT_BUOC_QUA_HAN)` của 005 với căn cứ `LAN_DO` (Trung bình); (b) sự kiện bổ sung của 005 `CongViecDoBiBo(congViecId, nctId, lichDoId, chiSoIds, lyDo: KHONG_THUC_HIEN | QUA_HAN_HET_CA, mucQuanTrong)` (mức cấu hình của quy tắc; Trung bình nếu Bắt buộc). Khóa `BO_LO_LAN_DO:<chiSo>`; mức cảnh báo là mức cao nhất đã gộp. Công việc Hủy và đóng do trạng thái cuối không tính (005 không phát). Yêu cầu đóng "đã thực hiện đúng khung" áp theo dòng nguồn (công việc), không theo khóa (CHK064).
- **Mất ngủ kéo dài** (FR-039c): listener sự kiện bổ sung của 005 `KetQuaNguDaGhi(ketQuaId, nctId, ngay, nhomGiaTri)` (cả khi đính chính). Hàm thuần `DemDemMatNgu(ketQuaTheoNgay, ngayVang)`: lấy kết quả ngủ hiện hành mới nhất mỗi ngày (qua `CongViecThongKeApi.ketQuaTheoKhoang`), đếm ngày liên tiếp nhóm `MAT_NGU` lùi từ ngày của kết quả; ngày vắng (từ `khoangTrangThai`) không tính và không làm đứt chuỗi; ngày không có ghi nhận làm đứt chuỗi. Đủ CFG-M05-05 thì tạo hoặc gộp `XU_HUONG:MAT_NGU_KEO_DAI`.
- **Từ chối cùng một thuốc**: 006 đếm (BR-M07-13) và gửi `YeuCauCanhBao(TU_CHOI_LIEN_TIEP)`; `m05` chỉ áp bật/tắt và mức của quy tắc `TU_CHOI_THUOC` trước khi tạo.

**Lý do**: Mỗi quy tắc chạy khi dữ liệu nguồn đổi, không cần job quét; hàm thuần kiểm thử bằng bảng (SC-001 kiểu dữ liệu).

**Phương án đã cân nhắc**: Job hằng đêm quét toàn viện cho sụt cân và mất ngủ: cảnh báo chậm tới sáng hôm sau, trái "khi một cân nặng được xác nhận" (FR-039a).

---

## S-11. Sự cố (mục F, FR-040 → FR-046b, BR-M05-08, 09)

**Quyết định**:
- **Tách nội dung và trạng thái** (spec bảng phân nhóm, điểm báo lại 7):
  - `su_co` (nhóm 3, không `UPDATE`): nội dung ghi nhận của FR-040, mức ban đầu, `ngoai_pham_vi`, `canh_bao_nguon_id`, tham chiếu đồ gửi.
  - `su_co_xu_ly` (nhóm 2, 1–1): trạng thái, mức hiện hành, từng ở Khẩn cấp (`da_tung_khan_cap`), cấp leo thang, hạn tiếp nhận, người phụ trách, tiếp nhận lúc, kết quả, người đóng, dấu "không có Bác sĩ trực", `tam_nhan_ban_giao`.
  - `dien_bien_su_co` (nhóm 3): loại diễn biến (danh mục nhóm 1, có dấu `bien_phap_hoi_suc`), nội dung, thời điểm diễn ra, người ghi (hoặc Hệ thống); gồm cả gọi hỗ trợ, gọi cấp cứu, báo cơ quan y tế (FR-066), "nguồn đã được xử lý", "đã tìm thấy" (013).
  - `doi_muc_su_co` (nhóm 3), `nguoi_xu_ly_su_co` (danh sách người đã ghi diễn biến hoặc tham gia, FR-048b).
- **Ghi nhận** (FR-041): quyền `SU_CO, TAO`; người cao tuổi ngoài phạm vi được nhờ ngoại lệ 002 FR-044a (`QuyenApi` trả `canCu = NGOAI_PHAM_VI`), khi đó cột `ngoai_pham_vi = true`, nhật ký có dấu, phản hồi chỉ có thông tin nhận dạng. Loại `CO_SO_VAT_CHAT` cho phép `nct_id` rỗng nhưng bắt buộc `tang_khu_vuc_id` (CHK004). Mức thấp hơn mặc định của loại thì bắt buộc lý do (FR-042).
- **Tác động khi tạo** (FR-043), trong cùng transaction: sự kiện thông báo theo mức (S-19); `SuCoDaGhiNhan(suCoId, nctId, loai, muc, canDanhGiaLai)` cho 001 với `canDanhGiaLai = (muc ≥ TRUNG_BINH hoặc loai = NGA) và loai ∉ {DO_GUI_THAT_LAC, DO_GUI_HU_HONG}`; loại Ngã → S-14; loại lây nhiễm → S-15; Khẩn cấp → S-12.
- **Leo thang của sự cố** (FR-044): cùng job và chuỗi của S-08, nhưng sự cố quá hạn giữ `MOI` và chỉ tăng `cap_leo_thang` (CHK022).
- **Giao người xử lý** *(bổ sung 2026-10-11, FR-045a, Q-339)*: lệnh `POST /su-co/{id}/giao-nguoi-xu-ly` (ghi chú bắt buộc). Khóa quyền `SU_CO, GIAO`, **không** dùng `SU_CO, TAO` (tức `(SU_CO_KHAN_CAP, TAO)` ở 002) vì khóa ghi nhận được miễn lớp phạm vi (002 FR-044a). Kiểm hai phía: (1) người giao: Trưởng tầng trong phạm vi tầng của sự cố, Bác sĩ, Quản lý viện (phạm vi toàn viện theo 19.3), hoặc người xử lý hiện tại (`nguoi_phu_trach_id`) khi sự cố Đang xử lý, Đang theo dõi; (2) người được giao: tài khoản vai trò Điều dưỡng, Trưởng tầng, Bác sĩ có quyền `SU_CO, XU_LY` với đối tượng của sự cố, kiểm bằng `NguoiDuyetApi.danhSach((SU_CO, XU_LY), doiTuong)` (002 tra quyền, nhiệm vụ, phạm vi của tài khoản khác tại thời điểm lệnh); đối tượng là người cao tuổi, hoặc tầng/khu vực của địa điểm với loại `CO_SO_VAT_CHAT`; người được giao khác người xử lý hiện tại. Từ `MOI`: chuyển `DANG_XU_LY`, đặt `nguoi_phu_trach_id`, `tiep_nhan_luc`, dừng leo thang như Tiếp nhận xử lý; từ `DANG_XU_LY`, `DANG_THEO_DOI`: chỉ đổi `nguoi_phu_trach_id`. Thêm một dòng `phan_cong_su_co` (nhóm 3) trong cùng transaction; phát `SuCoDaGiaoNguoiXuLy` cho 009 theo mức hiện hành. Lệnh không đổi mức, không ghi diễn biến chuyên môn; Quản lý viện được phép giao nhưng không được giao cho mình và vẫn 403 với `XU_LY` (Q-71).
  - **Cách kiểm cụ thể** *(bổ sung 2026-10-11 sau audit)*: ma trận 002 chỉ có `LoaiThaoTac` `XEM, TAO, SUA, XAC_NHAN, DUYET, CHOT, CHUYEN_MON` (002 A-04), và ô của mọi vai trò nhân viên ở dòng "Sự cố, khẩn cấp" đều là T¹⁰; chú thích ⁴³ chỉ mô tả. Vì vậy giới hạn của lệnh Giao kiểm trong 007 (U2), không qua khóa ghi nhận `(SU_CO_KHAN_CAP, TAO)` để không dính ngoại lệ phạm vi FR-044a:
    - người giao, lớp vai trò: `TaiKhoanApi.coVaiTro(nd, TRUONG_TANG, BAC_SI, QUAN_LY_VIEN)`, hoặc `nd` là `nguoi_phu_trach_id` khi sự cố Đang xử lý, Đang theo dõi;
    - người giao, lớp phạm vi: Trưởng tầng và người xử lý hiện tại phải có `PhamViApi.trongPhamVi(nd, doiTuong)`; Bác sĩ, Quản lý viện toàn viện (002 A-04);
    - người được giao: thuộc `NguoiDuyetApi.danhSach(khoaXuLy, doiTuong)` (hàm này luôn xét phạm vi, không áp ngoại lệ FR-044a) **và** `TaiKhoanApi.coVaiTro(nguoiDuocGiao, DIEU_DUONG, TRUONG_TANG, BAC_SI)` (loại Nhân viên chăm sóc, Bếp, Hành chính có T¹⁰ ở dòng này).
    - `khoaXuLy` là khóa 002 mà 007 dùng cho thao tác `SU_CO, XU_LY`; tên khóa giữ như hiện tại, đối chiếu cùng các tên khác khi chạy /speckit-analyze (002 research A-04).
  - **Loại id**: `nguoiDuocGiaoId`, `nguoi_giao_id`, `nguoi_duoc_giao_id`, `nguoi_phu_trach_id` là id tài khoản, cùng loại với `NguoiDungHienTai` và kết quả `NguoiDuyetApi.danhSach`.
  - **`tiep_nhan_luc` khi giao từ `MOI`**: đặt bằng thời điểm lệnh Giao và `nguoi_tiep_nhan_id` là người giao. Lệnh Giao là phản hồi đầu tiên của người có thẩm quyền và dừng leo thang như Tiếp nhận xử lý (FR-045a), nên số liệu thời gian tiếp nhận của 016 (FR-081) tính tới lúc này.
- **Vòng đời** theo bảng FR-045 (data-model mục 7). Đóng: Nhẹ bởi Điều dưỡng, Trưởng tầng, Bác sĩ; Trung bình, Khẩn cấp chỉ Điều dưỡng, Bác sĩ (BR-M05-09); kết quả bắt buộc; sự cố lây nhiễm chọn kết quả `XAC_NHAN_NHIEM | LOAI_TRU | KHONG_XAC_DINH` và qua FR-066. Quản lý viện ghi nhận, kích hoạt được nhưng mọi lệnh xử lý trả 403 (Q-71).
- **Hạ mức khỏi Khẩn cấp** (FR-044): phát `YeuCauDungNhacGoiDien(nguon = SU_CO:id)` cho 009; `da_tung_khan_cap` giữ `true` (bảo vệ FR-046, đối chiếu nguyện vọng giữ nguyên).
- **Hủy ghi nhận, đổi loại** (FR-045, FR-046, FR-046a): đi qua `DinhChinhApi` với loại `M05.SU_CO` (`HUY_GHI_NHAN` hoặc `SUA_NOI_DUNG` chỉ đổi `loaiSuCoId`). Provider:
  - `choPhep`: sự cố `da_tung_khan_cap` chỉ Điều dưỡng (trong phạm vi), Bác sĩ (CHK053); sự cố lây nhiễm có vùng chỉ gắn nó thì từ chối (FR-066);
  - `sauKhiDinhChinh`: Hủy → `su_co_xu_ly` sang `DA_HUY`; đổi loại → áp tác động loại mới tại thời điểm đính chính (sang Ngã: S-14; sang lây nhiễm: S-15 với khoảng truy vết tính từ thời điểm phát hiện, CHK059); không thu hồi tác động cũ; phát `TacDongConHieuLuc(suCoId, danhSach)` cho Điều dưỡng phụ trách và các Bác sĩ (Nhẹ); nếu sự cố đã nêu trong bản tin đã gửi thì phát `SuCoDaHuyTrongBanTin(suCoId)` cho 012 (CHK056).
- **Đồ gửi** (FR-040, FR-046b, Q-160): 013 ghi sự cố "đồ gửi thất lạc/hư hỏng" qua `SuCoLenhApi.ghiNhan(...)` trong lệnh đồ gửi của người dùng (người ghi là actor); khi bản ghi bàn giao bị hủy ghi nhận, 013 chỉ báo người xử lý sự cố, sự cố không tự hủy (FR-046b sửa 2026-10-10, Q-307); 013 gọi `themDienBien(suCoId, DA_TIM_THAY, …)`.
- **Thiếu người khi về** (014): 014 phát `YeuCauTaoCanhBao` mức Khẩn cấp; lệnh "Báo thiếu người" của 014 gọi `SuCoLenhApi.ghiNhan` với loại "đi lạc hoặc không trở về", người ghi là actor (Q-307).

**Lý do**: Spec xếp nội dung sự cố nhóm 3 và trạng thái xử lý nhóm 2; hai bảng giữ được trigger "không UPDATE" trên nội dung mà vẫn có lệnh chuyển trạng thái.

**Phương án đã cân nhắc**: Một bảng `su_co` có trigger chặn theo cột: được, nhưng khó kiểm trên mọi cột thêm sau này và trái mẫu nhóm 3 của 000 R-05.

---

## S-12. Quy trình khẩn cấp và chuyển viện (mục G, FR-047 → FR-050, BR-M05-06, 13, 14, DBR-19)

**Quyết định**:
- **Kích hoạt** khi sự cố Khẩn cấp được tạo (ghi trực tiếp, chuyển từ cảnh báo, kích hoạt từ cảnh báo) hoặc nâng lên Khẩn cấp. Trong cùng transaction:
  1. dựng **thẻ thông tin khẩn cấp** bằng `TheKhanCapService`: `NguoiCaoTuoiApi.nhanDang`, `NguyenVongApi.hieuLuc`, `MucSucKhoeApi.hieuLuc(DI_UNG, BENH_NEN)`, `DanhGiaApi.luuYHienHanh`, `DonThuocApi.thuocDangDung` (006), `ViTriApi.hienTai`, dấu nguy kịch, `NguoiThanKhanCapPort.nguoiLienHeChinh` (012). Các lời đọc chạy song song trên luồng ảo, mỗi lời có giới hạn thời gian; nguồn lỗi thì thẻ ghi "không lấy được <mục>" và vẫn lưu (SC-005 ≤ 3 giây);
  2. lưu **bản chụp** JSON vào `ban_chup_the_khan_cap` (nhóm 3) cùng danh sách người đã được hiển thị;
  3. phát `SuCoKhanCapDaKichHoat` với các nhóm người nhận của FR-047 (a); không có Bác sĩ trực (`NhanSuCanhBaoPort.bacSiTruc(t)` rỗng) thì thay nhóm `BAC_SI_TRUC` bằng `BAC_SI_TOAN_VIEN`, thêm `QUAN_LY_VIEN` nếu chưa có, đặt `khong_co_bac_si_truc = true` (FR-047b);
  4. nội dung cho người liên hệ chính: `TOI_THIEU` (FR-047) trừ khi `BanDongYApi.baoGom(nct, nguoiLienHeChinhId)` thì `KEM_LOAI_SU_CO`; không bao giờ có chẩn đoán, chỉ số, thuốc, nguyện vọng (SC-014). Yêu cầu tới người liên hệ chính mang `doiTuongThongTin = VE_NGUOI_CAO_TUOI` và mã Q-354 `KHAN_CAP_NGUOI_LIEN_HE_CHINH` (FR-047c; bổ sung 2026-10-18).
- **Bắt buộc trực tuyến** (8.6, Q-01): không có `NgoaiTuyenHandler` cho sự cố; app nhân viên chặn đưa vào hàng đợi và hiện "không gửi được – gọi trực tiếp theo quy trình của cơ sở"; backend từ chối phong bì ngoại tuyến loại sự cố bằng `TU_CHOI` `BAT_BUOC_TRUC_TUYEN`.
- **Hiển thị thẻ** (FR-048): `GET /su-co/{id}/the-khan-cap`; người có `QuyenXemTruongApi.duocXem(SUC_KHOE, nct) = DAY_DU` thấy đủ; người khác (gồm người kích hoạt ngoài phạm vi) chỉ thấy nhận dạng và "đã thông báo người xử lý". Lần xem lại sau đó không ghi nhật ký (Q-03, CHK046).
- **Đối chiếu nguyện vọng** (FR-048a, DBR-19): lệnh `POST /su-co/{id}/xac-nhan-doi-chieu-nguyen-vong` chỉ cho người được xem đủ thẻ; phản hồi trả lại nội dung nguyện vọng; thêm dòng `xac_nhan_doi_chieu` (nhóm 3). Lệnh ghi diễn biến loại có dấu `bien_phap_hoi_suc`: nếu `NguyenVongApi.hieuLuc(nct)` có giá trị tại thời điểm ghi và sự cố chưa có dòng xác nhận thì 409 `CHUA_DOI_CHIEU_NGUYEN_VONG` kèm nguyện vọng. Không có nguyện vọng thì không yêu cầu. Danh mục loại diễn biến khởi tạo năm biện pháp hồi sức của FR-048a (cần Bác sĩ đối chiếu, 24.4).
- **Chuyển viện** (FR-049, BR-M05-14): `POST /su-co/{id}/chuyen-vien` (Điều dưỡng, Bác sĩ; sự cố `MOI`, `DANG_XU_LY`). Trong một transaction: `TrangThaiNguoiCaoTuoiApi.thucHien(nctId, CHUYEN_VIEN, nd, …, chiTiet{canCu: {suCoId}, donViDuaDi, duKienTroLai?, nhanVienBanGiaoId}, CanCu(SU_CO, id))` (bảng người gọi của 001 đã có 007); rồi tạo `ban_tom_tat_chuyen_vien` (nhóm 3; dị ứng, thuốc đang dùng, `ChiSoApi.ganNhatMoiLoai`, diễn biến tới thời điểm, nguyện vọng, cơ sở tiếp nhận, người lập). 001 từ chối (409) thì rollback cả hai; một transaction `REQUIRES_NEW` ghi diễn biến `CHUYEN_VIEN_BI_TU_CHOI` kèm điều kiện chưa thỏa (FR-049 "sự cố ghi lại lần thử").
- **Bản tóm tắt** in được (`GET …/ban-tom-tat-chuyen-vien/{id}` trả JSON; web in bằng mẫu in); xem: Bác sĩ, Điều dưỡng, Trưởng tầng trong phạm vi, Quản lý viện.

**Lý do**: Spec đòi "trong cùng lúc" và "hoặc toàn bộ, hoặc không"; thông báo qua outbox ghi cùng transaction bảo đảm không mất, gửi song song do 009 làm (NFR-03 ≤ 10 giây).

---

## S-13. Nguy kịch và thực hiện nguyện vọng cuối đời (mục G1, FR-050a → FR-050g, BR-M05-15, 16, DBR-34)

**Quyết định**:
- `dau_nguy_kich` (nhóm 2): loại `CHINH_THUC | TAM`, trạng thái `TAM | DANG_MO | DA_GO`, nhận định, người ghi, Bác sĩ xác nhận, lý do gỡ, `canh_bao_id`. Cột sinh `khoa_mo = IF(trang_thai <> 'DA_GO', nct_id, NULL)` `UNIQUE` (DBR-34 "tối đa một dấu đang mở").
- **Ghi dấu** (Bác sĩ) hoặc **dấu tạm** (Điều dưỡng, chỉ khi `bacSiTruc(t)` rỗng; ngược lại 409 `DANG_CO_BAC_SI_TRUC`): người cao tuổi Đang lưu trú, Tạm vắng hoặc Hoạt động bên ngoài. Trong cùng transaction tạo cảnh báo Khẩn cấp khóa `NGUY_KICH:<dauId>` gọi trực tiếp `CanhBaoService` (không qua sự kiện), để DBR-34 "mỗi dấu đang mở có đúng một cảnh báo" đúng ngay khi commit; phát `NguyKichDaGhi` (thông báo FR-050b b, c: Khẩn cấp; người thân chỉ nội dung tối thiểu, không nguyện vọng) và `CanXacNhanLaiNguyenVong(canhBaoId, nctId)` cho Bác sĩ trực (hoặc mọi Bác sĩ), Điều dưỡng phụ trách, kèm thứ tự gọi bắt đầu từ người đại diện (BR-M13-02). Dấu tạm thêm `CanXacNhanDauTam` cho mọi Bác sĩ (Trung bình). Không tạo yêu cầu gọi điện (Q-307).
- **Kết quả xác nhận lại** (FR-050c): `ket_qua_xac_nhan_lai` (nhóm 3): người được liên hệ, kênh, thời điểm, kết quả `GIU | MOI | KHONG_LIEN_LAC_DUOC`, người ghi. Kết quả `MOI` gọi `NguyenVongApi.ghiPhienBan(lenh, XAC_NHAN_LAI_NGUY_KICH)` trong cùng transaction (001 đặt phiên bản cũ Được thay thế); `lenh` theo thân lệnh nguyện vọng của api-001 sau cập nhật 2026-10-18 (nhận định của Bác sĩ gắn với phiếu, người ký theo 001 FR-023c, ý kiến khác có nguồn). Thông báo nguy kịch tới người thân (FR-050b c) mang mã Q-354: `KHAN_CAP_NGUOI_LIEN_HE_CHINH` với người liên hệ chính, `XAC_NHAN_LAI_NGUYEN_VONG` với người đại diện; nhắc gọi xác nhận lại của 009 gửi nhân viên, không cần mã (plan, điểm báo lại 10). Ghi kết quả đầu tiên thì phát `DaCoKetQuaXacNhanLai(canhBaoId)` để 009 dừng nhắc gọi.
- **Lựa chọn thực hiện** (FR-050d): `lua_chon_thuc_hien` (nhóm 3): `CHUYEN_BENH_VIEN | VE_NHA | GIAM_NHE`, lý do (bắt buộc khi "không liên lạc được" mà chưa có nguyện vọng), lệnh đã thực hiện.
  - `CHUYEN_BENH_VIEN`: chạy đúng lệnh chuyển viện của S-12 (căn cứ là cảnh báo nguy kịch; nếu có sự cố Khẩn cấp đang mở thì căn cứ là sự cố) và bản tóm tắt.
  - `VE_NHA`: ghi lựa chọn; phát `CanChoTamVangVeNha(nctId, canhBaoId)` cho Hành chính, Trưởng tầng (Trung bình). Hệ thống không tự tạo lượt vắng; Hành chính hoặc Trưởng tầng thực hiện Cho tạm vắng ở 001/004 qua quy trình đón (Q-307; plan điểm báo lại 3).
  - `GIAM_NHE`: phát `LuaChonChamSocGiamNheDaGhi(nctId)` (tên 005 đã đề xuất); 005 báo điều dưỡng phụ trách xem xét kế hoạch.
- **Đóng cảnh báo nguy kịch**: thêm điều kiện "có ít nhất một kết quả xác nhận lại và một lựa chọn" (409 `CHUA_XAC_NHAN_LAI` / `CHUA_CO_LUA_CHON`).
- **Gỡ dấu**: Bác sĩ, lý do; bean `ThamGiaChuyenTrangThai` của `m05` tự gỡ khi `CHUYEN_VIEN` hoặc trạng thái cuối, lý do "tự gỡ theo trạng thái". Gỡ không đóng cảnh báo.
- Đính chính kết quả, lựa chọn qua 000 (loại `M05.KET_QUA_XAC_NHAN_LAI`, `M05.LUA_CHON_THUC_HIEN`).

---

## S-14. Sự cố ngã và theo dõi sau ngã (mục H, FR-051, FR-052, BR-M05-07)

**Quyết định**:
- `khoang_theo_doi_sau_nga` (nhóm 2): `nct_id`, `su_co_id`, `moc`, `ket_thuc` (`moc + thời gian theo dõi` của CFG-M05-06), `khoang_do` (chốt lúc tạo), trạng thái `DANG_THEO_DOI | DUOC_THAY | KET_THUC_SOM | DA_HET`, lý do. Cột sinh `UNIQUE` một khoảng `DANG_THEO_DOI` mỗi người (FR-052). "Đang theo dõi sau ngã" tính lúc đọc: `DANG_THEO_DOI` và `bây giờ < ket_thuc`.
- Hàm thuần `MocTheoDoiSauNga(thoiDiemXayRa?, thoiDiemGhiNhan, khoangDo)` theo FR-051 (CHK013): `moc = max(xayRa, ghiNhan − khoangDo)` khi có thời điểm xảy ra, ngược lại `ghiNhan`; lần đo đầu `moc + khoangDo`.
- Khi ghi sự cố Ngã (hoặc đổi loại sang Ngã) trong cùng transaction: khoảng cũ đang theo dõi → `DUOC_THAY`, thêm khoảng mới; phát `SuCoNgaDaGhiNhan(nctId, suCoId)` (005 đã đề xuất; 005 báo điều dưỡng xem xét kế hoạch), `CanLapCongViecTheoDoiSauNga(nctId, suCoId, moc, ketThuc, khoangDo)` cho Người phụ trách ca của tầng (Trung bình); `SuCoDaGhiNhan(canDanhGiaLai = true)` cho 001. Không tạo lịch đo hay công việc (Q-306, Q-307, SC-007).
- Bean `NguonCongViecPort` của `m05` cho `THEO_DOI_SAU_NGA`: căn cứ là `su_co_id`; hợp lệ khi khoảng của sự cố đó `DANG_THEO_DOI` và thời điểm dự kiến trong `(moc, ket_thuc]`; ngoài khoảng thì 005 chặn (US6 Independent Test).
- **Kết thúc sớm** (Bác sĩ, lý do): khoảng → `KET_THUC_SOM`; gọi `NguonCongViecApi.nguonBiHuy(THEO_DOI_SAU_NGA, suCoId, lyDo)` trong cùng transaction. Hết thời gian: job theo phút chuyển `DA_HET`. Vắng mặt không kéo dài khoảng (US6 #5).

---

## S-15. Sự cố lây nhiễm, truy vết và khoanh vùng (mục I, FR-060 → FR-066, BR-M05-10 → 12)

**Quyết định**:
- **Dấu "nghi nhiễm"** tính lúc đọc: người có sự cố loại lây nhiễm nghi ngờ ở `MOI`, `DANG_XU_LY`, `DANG_THEO_DOI`. Khi ghi sự cố lây nhiễm: phát `CanLapDanhSachTiepXuc(suCoId, nctId)` cho Điều dưỡng phụ trách và các Bác sĩ (Trung bình). Không tạo danh sách (Q-307, SC-008).
- **Tra cứu truy vết** `GET /su-co/{id}/tra-cuu-tiep-xuc?nhom=…`: khoảng `[thoiDiemPhatHien − CFG-M05-07, thoiDiemPhatHien]`; chỉ đọc, không lưu:
  - (a) cùng phòng: `LichSuViTriApi.cungPhong` (003, có khoảng vắng, dấu đính chính);
  - (b) cùng buổi hoạt động: `DiemDanhTruyVetPort.cungThamGia` (014 cài);
  - (c) nhân viên: `NhanSuCanhBaoPort.phanCongTrongKhoang` (008 cài) hợp với người đã ghi kết quả công việc (`CongViecThongKeApi.nguoiDaGhiKetQua`, bổ sung theo plan này) và liều (`LichThuocApi.nguoiDaGhiLieu`, bổ sung theo plan này) (CHK018);
  - (d) người thân: `NguoiThanKhanCapPort.luotThamTrongKhoang` (012 cài: lượt Đã vào, Đã ra và người đi cùng thực tế).
  Kết quả kèm nhóm, nguồn, khoảng tiếp xúc; các khoảng người nghi nhiễm vắng mặt (`khoangTrangThai` của 001) được đánh dấu (FR-060).
- `danh_sach_tiep_xuc` (nhóm 2, một danh sách mỗi sự cố: `UNIQUE (su_co_id)`) và `nguoi_tiep_xuc` (nhóm 2 theo dòng: loại `NGUOI_CAO_TUOI | NHAN_VIEN | NGUOI_THAN | NGUOI_DI_CUNG`, tham chiếu, nhóm, nguồn, khoảng tiếp xúc, `theo_doi_tu`, `theo_doi_den`, `bi_loai_luc`, lý do). Lệnh theo bảng FR-061; quyền: Điều dưỡng trong phạm vi của người nghi nhiễm hoặc Bác sĩ (Q-68).
- **Xác nhận, bổ sung** (FR-062): mỗi người cao tuổi mới: `theo_doi_tu = thời điểm xác nhận`, `theo_doi_den = + 7 ngày` (phần "trong 7 ngày" của CFG-M05-08); dấu "tiếp xúc" tính lúc đọc trên hợp các khoảng chưa bị loại của người đó (khoảng trùng gộp tới mốc muộn nhất). Phát `DaThemVaoDanhSachTiepXuc(nctId, thoiDiem)` (003 đã khai báo); `CanLapCongViecDoNhietDo(nctId, nguoiTiepXucId, tu, den, tanSuat)` cho Người phụ trách ca của tầng người đó (Trung bình); `ThongBaoTiepXuc` cho nhân viên và người thân trong danh sách (nhóm `NGUOI_TRONG_BAN_GHI_NGUON`, loại thông tin `CHUNG`, nội dung chỉ "tầng/khu vực, khoảng thời gian" và số liên hệ của viện; 009 FR-007 d, Q-112); `DanhSachTiepXucDaXacNhan` cho Quản lý viện (đầy đủ).
- Bean `NguonCongViecPort` của `m05` cho `THEO_DOI_TIEP_XUC`: căn cứ là `nguoi_tiep_xuc.id`; hợp lệ khi dòng chưa bị loại, người khớp, thời điểm trong khoảng theo dõi. Người phụ trách ca vẫn được báo dù đã có lịch đo nhiệt độ của Bác sĩ (Edge Case); trùng căn cứ và thời điểm do 005 chặn.
- **Loại người**: `bi_loai_luc`, lý do; gọi `NguonCongViecApi.nguonBiHuy(THEO_DOI_TIEP_XUC, nguoiTiepXucId)`.
- **Hành chính** chỉ xem `GET /su-co/{id}/nguoi-than-can-lien-he`: họ tên, quan hệ, số điện thoại, nội dung cần báo của người thân không có kênh nhận (CHK062). Endpoint khác của danh sách trả 403 với Hành chính.
- `PhanBoDaDinhChinh` (003): danh sách có khoảng truy vết chứa khoảng đổi thì phát `DanhSachTiepXucCanXemLai` cho người xác nhận và các Bác sĩ (Nhẹ); không tự đổi danh sách (FR-061, Q-274).
- **Khoanh vùng**: `khoanh_vung` (nhóm 2), `khoanh_vung_pham_vi` (tầng hoặc một khu vực; không có phòng, Q-69), `khoanh_vung_su_co` (≥ 1 sự cố lây nhiễm chưa đóng). Lệnh Khoanh vùng, Gỡ (Bác sĩ, Quản lý viện; lý do). Vùng tính theo cơ cấu hiện hành (`CoCauViTriApi`), nên phòng mới tạo trong tầng đang khoanh chịu chặn ngay (CHK043).
- `KhoanhVungApi` (đọc): `dangChiu(phongId | tangId | nctId, t)` trả các vùng bao; 012 (chặn đăng ký thăm), 014 (chặn đăng ký hoạt động, buổi trong vùng), 016 dùng. Bean `TinhTrangLayNhiemChoPhanBo` cho 003 (vùng đang khoanh; nghi nhiễm hoặc tiếp xúc). Phát `KhoanhVungDaTao(vungId, tangIds, khuVucIds)` (012 hủy lượt thăm đã duyệt, 014 hủy buổi, 009 thông báo Trưởng tầng, Hành chính, điều dưỡng có ca trong vùng) và `KhoanhVungDaGo(tangIds, khuVucIds)` (003 đã khai báo). Chặn còn khi còn ít nhất một vùng bao (Edge Case hai vùng chồng nhau).
- **Cảnh báo không chặn** (FR-065, CHK008): bean trạng thái của `m05` cài `canhBao(nc)` cho `CHO_TAM_VANG`, `CHUYEN_VIEN`: người thuộc vùng đang khoanh thì trả cảnh báo "thuộc vùng đang khoanh vùng"; không thêm điều kiện chặn. Quy trình đón của 012 tự đọc `KhoanhVungApi`.
- **Đóng sự cố lây nhiễm** (FR-066): còn vùng `DANG_KHOANH_VUNG` mà tập sự cố gắn chỉ còn sự cố này thì 409 `CON_VUNG_KHOANH_CHI_GAN_SU_CO`. Đóng thì danh sách → `DA_KET_THUC`, dấu tiếp xúc giữ tới hết khoảng theo dõi.

**Lý do**: Q-307 bỏ đề xuất, nên tra cứu là phép đọc có quyền, không phải bản ghi; dữ liệu nguồn nằm ở năm module nên đi qua giao diện của từng module (constitution X).

---

## S-16. Giấy phép cơ sở, số liên hệ của viện, khám và điều trị (mục J, FR-070 → FR-075, BR-M06-05)

**Quyết định**:
- `giay_phep_co_so` (nhóm 1 có lịch sử): số, cơ quan cấp, ngày cấp, ngày hết hạn, `co_kham_chua_benh`, `thay_the_cho`, `thay_the_do_sai_sot`, `tep_scan_id`; bảng con `giay_phep_co_so_pham_vi` (mã phạm vi chuyên môn theo danh mục phạm vi hành nghề của 008). Không `UPDATE`, không `DELETE` (trigger); gia hạn, đổi phạm vi, sửa sai là bản mới; bản bị thay do sai sót không dùng để xét hiệu lực (FR-075).
- `GiayPhepCoSoApi` (khai báo ở `m15.taikhoan.api`, `m06` cài): `coPhamVi(phamVi, ngay)` = tồn tại bản không bị thay do sai sót, `ngay_cap <= ngay <= ngay_het_han`, có `co_kham_chua_benh` và phạm vi đó (FR-071); `sapHetHan(trongVong)` cho job cảnh báo của 002 (CFG-M06-02). Không có công tắc riêng.
- `so_lien_he_vien` (nhóm 1, một dòng): số điện thoại, ghi chú; đổi có nhật ký (FR-070, CHK057). `SoLienHeVienApi.hienHanh()` cho 009 và cho nội dung thông báo của 007.
- `ban_ghi_kham` (nhóm 3): loại `KHAM | DANH_GIA | CHAN_DOAN | CHI_DINH | DIEU_TRI | KET_QUA`, nội dung, `co_so_thuc_hien` (`VIEN` hoặc tên cơ sở ngoài), người thực hiện (nhân viên hoặc tên ngoài), thời điểm, tệp đính kèm, người ghi. Kiểm tra:
  - Bác sĩ ghi mọi loại; `CHAN_DOAN`, `CHI_DINH`, `DIEU_TRI` tại viện gọi `QuyenApi.coQuyen(nd, (KHAM_DIEU_TRI, CHUYEN_MON), doiTuong)`: 002 xét lớp pháp lý gồm giấy phép bác sĩ (008) và `GiayPhepCoSoApi` (FR-073, SC-010);
  - Điều dưỡng: cơ sở ngoài (bắt buộc tên cơ sở, người thực hiện, ngày) hoặc `KHAM`, `KET_QUA` tại viện; `KHAM` tại viện phải có người thực hiện là một Bác sĩ khác người ghi (CHK019).
  - Đính chính qua 000 loại `M06.BAN_GHI_KHAM`.

---

## S-17. Bean trạng thái và việc sau qua đời (FR-006, FR-022, FR-038, FR-050a, Edge Case trạng thái cuối)

**Quyết định**: Hai bean `ThamGiaChuyenTrangThai`, `@Order` sau 006: `m06` rồi `m05`.

| Lệnh | `m06` | `m05` |
| --- | --- | --- |
| `KET_THUC_LUU_TRU` | Tác động: lịch đo `HIEU_LUC`, `CHO_HIEU_LUC` → `DA_NGUNG` (lý do trạng thái cuối, gọi `nguonBiHuy`); ngưỡng cá nhân `HIEU_LUC` → `HET_HIEU_LUC`, `CHO_HIEU_LUC` → `DA_HUY` | Điều kiện `KT_D_CANH_BAO_SU_CO` (`coTheMien = true`): còn cảnh báo đang mở hoặc sự cố `MOI`, `DANG_XU_LY`, `DANG_THEO_DOI` (CHK061). Tác động: `dung_leo_thang`; dấu nguy kịch → `DA_GO`; khoảng theo dõi sau ngã → `DA_HET`; danh sách tiếp xúc giữ |
| `GHI_NHAN_QUA_DOI`, `HUY_TIEP_NHAN` | Như trên | Như trên, không có điều kiện chặn |
| `CHUYEN_VIEN` | — | Dấu nguy kịch → `DA_GO` "tự gỡ theo trạng thái"; `canhBao` khi thuộc vùng khoanh (S-15) |
| `CHO_TAM_VANG` | — | `canhBao` khi thuộc vùng khoanh |

- `MucViecSauQuaDoiPort` của `m05`: `ma() = CANH_BAO_SU_CO`; `CHUA_HOAN_THANH` khi còn cảnh báo đang mở hoặc sự cố mở; `KHONG_AP_DUNG` khi người đó chưa từng có; còn lại `HOAN_THANH`. Sau mỗi lệnh đóng cảnh báo, sự cố của người ở trạng thái cuối, `m05` gọi `DanhSachViecSauQuaDoiApi.capNhat(nctId)`.
- Cảnh báo có nguồn từ 005, 006 được đóng bởi yêu cầu đóng `TRANG_THAI_CUOI` mà các bean đó phát (S-09).

---

## S-18. Bàn giao ca (FR-037, BR-M05-05, BR-M09-08, Q-78, Q-94, Q-307)

**Quyết định**:
- `CanhBaoBanGiaoApi` (`m05.api`, đọc và MANDATORY) cho 008:
  - `dangMo(tangKhuVucId, caId)`: cảnh báo, sự cố đang mở của người thuộc tầng (kể cả người vắng mặt theo quy tắc tầng của S-08), kèm người phụ trách, mức, hạn; `ChiSoBanGiaoApi.vuotNguongTrongCa(tang, tu, den)` (`m06.api`) cho chỉ số vượt ngưỡng trong ca. Người lập bàn giao tự ghi (Q-307); 007 không lập mục bàn giao.
  - `khiXacNhanBanGiao(banGiaoId, caTruocId, caSauId, tangKhuVucId)` (MANDATORY, trong transaction xác nhận của 008): với mỗi cảnh báo, sự cố đang mở của tầng có người phụ trách "thuộc ca trước" (`NhanSuCanhBaoPort.coTenTrongCa(nguoi, caTruocId)`) và không phải Bác sĩ, Quản lý viện (CHK016), hoặc đang `tam_nhan_ban_giao`: người phụ trách mới = Điều dưỡng phụ trách người đó trong ca sau, không có thì Người phụ trách ca sau; giữ trạng thái, cấp, hạn; gỡ dấu tạm nhận; ghi lịch sử; phát `NguoiPhuTrachDaDoi` (Nhẹ).
- **Tạm nhận** (FR-037 ngoại lệ, Q-94): job theo phút gọi `NhanSuCanhBaoPort.caDaBatDauChuaXacNhanBanGiao(tu, den)`; với mỗi (ca sau, tầng) chưa xử lý (`lan_chay_m05`), mục có người phụ trách thuộc ca trước và không có tên trong ca nào đang diễn ra được chuyển tạm sang Người phụ trách ca sau (không có thì Trưởng tầng được giao), đặt `tam_nhan_ban_giao = true`. Người tạm nhận thay Điều dưỡng phụ trách ở cấp 0 và người nhận FR-035; các cấp sau giữ.

---

## S-19. Sự kiện và thông báo (FR-033, FR-035, FR-043, FR-047, FR-050b, FR-062, FR-064)

**Quyết định**: Mọi thông báo là sự kiện ghi cùng transaction qua outbox (R-09); 009 gửi (constitution XIV). Mỗi sự kiện mang mức, nhóm người nhận (kèm cờ `khongThay` theo 009 FR-010, Q-110 cho nhóm cấp leo thang), loại thông tin (`SUC_KHOE`, `CHUNG`), khóa sự kiện (009 FR-005). Nhóm người nhận dùng mã chung để 009 phân giải theo phân công hiện tại (BR-M13-04): `DIEU_DUONG_PHU_TRACH(nct)`, `NGUOI_PHU_TRACH_CA(tang)`, `TRUONG_TANG(tang)`, `BAC_SI_TRUC`, `BAC_SI_TOAN_VIEN`, `QUAN_LY_VIEN`, `HANH_CHINH`, `NGUOI_LIEN_HE_CHINH(nct)`, `NGUOI_DAI_DIEN(nct)`, `NHAN_VIEN(id)`, `NGUOI_TRONG_BAN_GHI_NGUON`.

Mức (spec; dòng có * là plan áp nguyên tắc 3 bậc của 009 FR-043b, xem plan điểm báo lại 5):

| Mức | Thông báo |
| --- | --- |
| Khẩn cấp | Cảnh báo Khẩn cấp tạo hoặc nâng (FR-047a); sự cố Khẩn cấp kích hoạt (FR-047); nguy kịch cho đội ngũ và người thân, gọi xác nhận lại (FR-050b) |
| Trung bình | Cảnh báo Trung bình tạo, leo thang (FR-035, FR-033); nhắc leo thang tối đa (FR-034); sự cố Trung bình (FR-043 a); báo lặp lại (FR-038a)*; lập công việc theo dõi sau ngã*; cần lập danh sách tiếp xúc*; lập công việc đo nhiệt độ cho người tiếp xúc*; thông báo tiếp xúc cho nhân viên, người thân*; khoanh vùng cho Trưởng tầng, Hành chính, điều dưỡng*; cần xác nhận dấu tạm*; lựa chọn "về nhà" cho Hành chính, Trưởng tầng*; tạm nhận chờ xác nhận bàn giao* |
| Nhẹ | Cảnh báo Nhẹ (FR-035); sự cố Nhẹ (FR-043 a, chỉ trong ứng dụng); thiếu ngưỡng cá nhân (FR-025); nguồn đã được đính chính (FR-019)*; nguồn đã xử lý trên sự cố liên kết*; tác động còn hiệu lực sau hủy, đổi loại*; danh sách tiếp xúc cần xem lại*; gỡ khoanh vùng*; người phụ trách đổi sau bàn giao* |

- Nội dung kênh ngoài ứng dụng không có họ tên, thông tin sức khỏe (009 FR-019). Nội dung cho người thân: FR-047 (tối thiểu hoặc kèm loại sự cố), FR-062 (không danh tính người nghi nhiễm), FR-050b (không nguyện vọng).
- Ngoài thông báo, 007 phát: `YeuCauDungNhacGoiDien`, `DaCoKetQuaXacNhanLai` (009); `SuCoDaGhiNhan` (001, 012, 016); `SuCoNgaDaGhiNhan`, `LuaChonChamSocGiamNheDaGhi`, `LichDoDaDoi` (005); `SutCanPhatHien` (011); `DaThemVaoDanhSachTiepXuc`, `KhoanhVungDaTao`, `KhoanhVungDaGo` (003, 012, 014); `SuCoDaHuyTrongBanTin` (012).
- "Người được thông báo" của sự cố (FR-040, FR-048b) đọc qua `NhatKyThongBaoPort` (009 cài) lúc hiển thị; 007 không chép.
- **Đối tượng thông tin và mã trường hợp** (bổ sung 2026-10-18; FR-047c, 009 FR-001a, Q-354): mọi yêu cầu có nhóm người thân khai `doiTuongThongTin = VE_NGUOI_CAO_TUOI` và mang `maTruongHop`:

  | Thông báo | Nhóm người thân | Mã Q-354 |
  | --- | --- | --- |
  | Sự cố Khẩn cấp kích hoạt (FR-047) | `NGUOI_LIEN_HE_CHINH(nct)` | `KHAN_CAP_NGUOI_LIEN_HE_CHINH` |
  | Nguy kịch (FR-050b c) | `NGUOI_LIEN_HE_CHINH(nct)`; `NGUOI_DAI_DIEN(nct)` | `KHAN_CAP_NGUOI_LIEN_HE_CHINH`; `XAC_NHAN_LAI_NGUYEN_VONG` |
  | Thông báo tiếp xúc (FR-062) | `NGUOI_TRONG_BAN_GHI_NGUON` | `DANH_SACH_TIEP_XUC` |
  | Chuyển viện (FR-049) | Do 001 phát khi lệnh Chuyển viện chạy (001 research H-14) | `CHUYEN_VIEN` |

  Mã đi theo yêu cầu; 009 chỉ dùng với người nhận nằm ngoài phạm vi bản đồng ý và lưu vào bản ghi gửi (009 FR-014a). 007 không gửi người thân thông báo nào khác.

## S-19a. Phần chung và phần sức khỏe của sự cố *(bổ sung 2026-10-18; FR-082, Q-352)*

**Quyết định**:
- **Phần chung**: có sự cố, tên loại sự cố hiện hành (trừ loại lây nhiễm: áp Q-72, chỉ "có sự cố" và thời điểm), thời điểm xảy ra hoặc phát hiện, mức (012 FR-052 ghi "phần chung của sự cố trong kỳ kèm mức"), trạng thái xử lý rút gọn "viện đang xử lý / đang theo dõi / đã xử lý xong".
- **Phần sức khỏe**: mô tả, xử lý ban đầu, diễn biến, kết quả, khám, chỉ số, điều trị, thuốc, nguyên nhân y tế, chuyển viện (plan, điểm báo lại 9).
- **Giao diện**: `SuCoNguoiThanView(phanChung, Optional<phanSucKhoe>)`. `SucKhoeCongNguoiThanApi.suCo(nctId, nguoiThanId, …)` luôn trả phần chung và chỉ trả phần sức khỏe khi `BanDongYApi.baoGom(nctId, nguoiThanId)`; việc có hiển thị phần chung cho người thân hay không là quy tắc của 012. `BanTinSucKhoeApi.choBanTin` trả cả hai phần cho mỗi sự cố, 012 lọc theo từng người nhận (012 FR-052). Sự cố Đã hủy không có trong cả hai (FR-046a).

**Lý do**: Q-352 tách theo loại thông tin chứ không theo người; 007 giữ danh sách trường ở một chỗ, 012 và 009 chỉ lọc theo quyền.

**Phương án đã cân nhắc**: Để 012 tự chọn trường từ bản ghi sự cố: lặp quy tắc phân loại ở hai module; bị loại.

**Lý do**: Constitution XIV; mẫu 001 H-14, 006 D-17.

---

## S-20. Quyền và phạm vi (FR-080, Phụ lục 27, 19.3)

**Quyết định**: Khóa quyền theo các dòng của bảng FR-080; ba lớp do 002 xét.

| Chức năng (`ChucNang`) | Thao tác |
| --- | --- |
| `CHI_SO` | `XEM` (QL, TT trong phạm vi, BS, ĐD, CS trong phạm vi; NT điều kiện ¹); `GHI` (ĐD, CS trong phạm vi) |
| `NGUONG_LICH_DO` | `XEM` (QL, TT trong phạm vi, ĐD); `THIET_LAP` (BS) |
| `CANH_BAO` | `XEM` (QL, CS trong phạm vi); `XU_LY` (TT, ĐD trong phạm vi, BS; Người phụ trách ca theo nhiệm vụ, 002 FR-019a); `TIEP_NHAN_CAP_2`, `GIAO` (QL) |
| `SU_CO` | `TAO` (mọi nhân viên, ngoại lệ ¹⁰); `XU_LY` (TT, ĐD trong phạm vi, BS); `GIAO` (TT trong phạm vi, BS, QL; người xử lý hiện tại; chú thích ⁴³ chỉ mô tả, kiểm ở 007; bổ sung 2026-10-11, Q-339); `XEM` (QL; NT điều kiện ¹) |
| `KHOANH_VUNG` | `THUC_HIEN` (BS, QL); `XEM` (TT, ĐD, HC) |
| `DANH_SACH_TIEP_XUC` | `XAC_NHAN` (BS; ĐD trong phạm vi); `XEM` (QL, TT trong phạm vi); `XEM_NGUOI_THAN_CAN_LIEN_HE` (HC) |
| `GIAY_PHEP_CO_SO` | `CAU_HINH` (QL); `XEM` (BS) |
| `KHAM_DIEU_TRI` | `GHI` (BS, ĐD); `CHUYEN_MON` (BS, lớp pháp lý); `XEM` (QL, TT trong phạm vi; NT điều kiện ¹) |
| `NGUY_KICH` | `GHI_DAU`, `GO_DAU` (BS); `GHI_DAU_TAM` (ĐD, chỉ khi không có Bác sĩ trực); `XAC_NHAN_LAI`, `THUC_HIEN_LUA_CHON` (BS; ĐD trong phạm vi); `XEM` (QL, TT trong phạm vi) |
| `DANH_MUC_SUC_KHOE` | `CAU_HINH` (QL: danh mục chỉ số, loại sự cố, quy tắc xu hướng, loại diễn biến, số liên hệ của viện) |

- Trường `SUC_KHOE` (giá trị chỉ số, chẩn đoán, nguyện vọng, mô tả sự cố) bị che theo `QuyenXemTruongApi`; Hành chính và Bếp chỉ thấy nhận dạng khi ghi sự cố (CHK062, SC-012).
- Mọi lần người thân xem chỉ số, sự cố, khám điều trị ghi nhật ký (NFR-08).
- Quản lý viện: `XU_LY` trả 403 trên cảnh báo, sự cố (Q-71, SC-016); tiếp nhận chỉ khi cấp 2.

---

## S-21. Bộ lập lịch của 007 (FR-006, FR-017, FR-022, FR-025, FR-033, FR-034, FR-037, FR-051, FR-084, NFR-04)

**Quyết định**: `@Scheduled` + ShedLock, mỗi lần chạy một dòng `lan_chay_lich` (000 R-10); chạy khi khởi động để làm bù.

| Job | Chu kỳ | Việc |
| --- | --- | --- |
| `ViecTheoPhutM06` | Mỗi phút | Xác nhận bản ghi quá hạn đo lại (FR-017); lịch đo `CHO_HIEU_LUC → HIEU_LUC`, `HIEU_LUC → HET_HAN`; ngưỡng `CHO_HIEU_LUC → HIEU_LUC` (kèm phiên bản cũ Hết hiệu lực) |
| `ViecHangNgayM06` | 00:15 | Nhắc thiếu ngưỡng cá nhân (FR-025) |
| `ViecTheoPhutM05` | Mỗi phút | Leo thang cảnh báo, sự cố (một cấp mỗi lần); nhắc leo thang tối đa; Nhẹ khi hết ca; tạm nhận bàn giao; khoảng theo dõi sau ngã hết hạn |

- Chạy bù (FR-084): mỗi việc ghi `thoi_diem_du_kien` và thời điểm thực tế vào lịch sử; trạng thái theo thời điểm (lịch đo, ngưỡng) giữ thời điểm hiệu lực gốc; nhắc bị lỡ gửi một lần.
- Giới hạn 000 R-10: job chỉ chuyển trạng thái, cảnh báo, leo thang, nhắc; không tạo bản ghi nghiệp vụ (ArchUnit gói `..lich..`). Xác nhận bản ghi "chưa đo lại" là chuyển trạng thái bản nháp nhóm 3 theo quy tắc FR-017, không tạo bản ghi mới.

---

## S-22. Lưu giữ, nhật ký, kiểm thử

**Quyết định**:
- **Lưu giữ** (000 R-11): mọi bảng gắn người cao tuổi theo `THEO_HO_SO` (CFG-M15-03); sự cố không gắn người cao tuổi, khoanh vùng, giấy phép cơ sở theo `KHONG_GAN_HO_SO` (CFG-M15-04; spec Assumptions CHK011). Chuỗi tham chiếu: cảnh báo ↔ sự cố ↔ bản tóm tắt; danh sách tiếp xúc ↔ sự cố; khoanh vùng ↔ sự cố (giữ tới khi cả chuỗi hết hạn, FR-051a của 000).
- **Phân loại trường** (`PhanLoaiTruongProvider`): giá trị chỉ số, kết quả so, nội dung khám, chẩn đoán, mô tả và diễn biến sự cố, thẻ khẩn cấp, nguyện vọng trong bản chụp → `SUC_KHOE`; số điện thoại người thân trong danh sách tiếp xúc → `LIEN_HE`.
- **Kiểm thử** (000 R-14, constitution XV):
  - test bảng cho hàm thuần: `SoNguong`, `KiemTraNguong`, `KhoaLoai`, `DemLap24h`, `SutCan`, `DemDemMatNgu`, `MocTheoDoiSauNga`, `ChuoiMocDo`, `ChuoiLeoThang` (bỏ cấp trống, một cấp mỗi lần);
  - SC-001: bộ 300 giá trị dựng từ bảng tính tay;
  - Test tích hợp MySQL cho cột sinh `UNIQUE` (DBR-18, DBR-34, ngưỡng, ngã), trigger nhóm 3, trigger nội dung lịch đo và ngưỡng;
  - test đồng thời: 10 phát sinh cùng khóa (SC-003); hai lần kích hoạt khẩn cấp; đo lại và job hết hạn; hai lần tiếp nhận;
  - đồng hồ giả lập cho CFG-M05-01, 02, 03, 05 → 08, CFG-M06-01, 04, chạy bù;
  - bản giả cho mọi SPI 007 khai báo, có công tắc `/api/v1/kiem-thu/gia-lap/{module}`;
  - Flutter: chặn kích hoạt khẩn cấp khi mất kết nối, ghi chỉ số ngoại tuyến và kết quả đồng bộ (BR-M15-20), màn đo lại.
- Tên test theo BR/FR (`BR_M05_02_GopCanhBaoDongThoiIT`, `FR_047b_KhongCoBacSiTrucIT`). Bảng truy vết ở [quickstart.md](quickstart.md) mục 4.
