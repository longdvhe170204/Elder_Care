# Research: Dịch vụ thông báo (009)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

Mỗi mục gồm Quyết định, Lý do, Phương án đã cân nhắc. Quyết định nền R-01 → R-14 của 000 và các mẫu đã chốt ở 001 → 008 được dùng lại, không quyết lại. Đặc biệt 000 R-09: "Mức thông báo do module nguồn nêu trong payload sự kiện; 009 lắng nghe và gửi". Các quyết định dưới đây là kỹ thuật; chúng không đổi nghiệp vụ (constitution IX). Chỗ spec chưa rõ hoặc mâu thuẫn được ghi ở plan, mục "Điểm cần báo lại".

---

## B-01. Vị trí trong backend

**Quyết định**: Feature 009 là module M13 – Thông báo (mục 17), gói `com.eldercare.backend.notification`:

| Gói con | Nội dung | Mục spec |
| --- | --- | --- |
| `notification.tiepnhan` | Listener chung, kiểm tra yêu cầu, chống trùng, danh sách lỗi gửi | A (FR-001 → FR-006) |
| `notification.nguoinhan` | `XacDinhNguoiNhan`: nhóm, cách thay, lọc người thân, gộp trùng | B (FR-007 → FR-015) |
| `notification.gui` | Kênh, chọn kênh theo mức và ca, gửi song song, gửi lại, gộp tin nhắn, kiểm nội dung kênh ngoài | C (FR-016 → FR-021) |
| `notification.yentinh` | Giờ yên tĩnh, giữ và giải phóng | D (FR-022 → FR-025) |
| `notification.khancap` | Xác nhận, lượt nhắc gọi, kết quả gọi, chuyển nhắc, chuỗi gọi | E (FR-026 → FR-032) |
| `notification.hopthu` | Hộp thông báo, đã xem, ẩn, hiển thị theo quyền hiện hành | F (FR-033, FR-034) |
| `notification.lienhe` | Danh sách "cần liên hệ trực tiếp" | C (FR-021) |
| `notification.tracuu` | Lịch sử, lỗi gửi, dữ liệu cho module nguồn | F, G (FR-035 → FR-038) |
| `notification.cauhinh` | Kênh và gắn kênh khác vào mức | G (FR-039, FR-040) |
| `notification.lich`, `notification.api` | Job; giao diện công khai, SPI cho 012 | |

**Lý do**: Constitution X, XIV ("mọi thông báo MUST đi qua dịch vụ thông báo chung, spec 009").

---

## B-02. Một khuôn yêu cầu thông báo cho mọi module (FR-001, FR-043a, 000 R-09)

**Quyết định**:
- Khuôn yêu cầu đặt ở gói kỹ thuật `com.eldercare.backend.common.thongbao` (bổ sung vào hợp đồng `common` của 000), không ở `notification.api`, vì mọi module (kể cả 000 → 008 đã làm plan trước, và các module mà 009 phải đọc dữ liệu) cần phát yêu cầu mà không phụ thuộc M13:

  ```java
  interface CoYeuCauThongBao {                     // sự kiện miền của module nguồn cài
    List<YeuCauThongBao> yeuCauThongBao();         // có thể rỗng; một sự kiện có thể sinh nhiều yêu cầu (FR-007 nội dung theo từng người)
  }
  record YeuCauThongBao(
      String moduleNguon, String loaiSuKien, NhomNguon nhomNguon,      // nhóm 17: SU_CO, CANH_BAO_SUC_KHOE, THUOC, CONG_VIEC_QUA_HAN, ...
      BanGhiNguon banGhiNguon, String khoaSuKien,                       // (loại, id); khóa có số thứ tự với nhắc lặp lại (FR-005)
      Optional<MucThongBao> muc,                                        // thiếu → Nhẹ + dấu (FR-003)
      Optional<Long> nctId, Optional<Long> tangKhuVucId,
      List<NhomNhan> nhomNguoiNhan,                                     // B-04; mỗi nhóm loại (a) kèm CachThay
      List<PhanNoiDung> noiDung,                                        // mỗi phần gắn LoaiThongTin: CHUNG | SUC_KHOE | CHI_PHI | THAM_NOM | HOP_DONG
      Optional<String> noiDungRutGon,                                   // thuộc CHUNG (FR-019)
      Optional<String> tieuDe,                                          // không chứa sức khỏe (FR-034)
      Optional<Long> nguoiGayRa, boolean baoNguoiGayRa,                 // FR-012
      Instant thoiDiemPhatSinh, Map<String, Object> thamSoHienThi,
      Optional<DoiTuongThongTin> doiTuongThongTin,                      // bổ sung 2026-10-18 (FR-001a): VE_NGUOI_CAO_TUOI | VE_VIEC_NGUOI_THAN
      Optional<MaTruongHopQ354> maTruongHop) {}                         // bổ sung 2026-10-18 (FR-001a, FR-014a)
  ```
- Mỗi sự kiện mà plan 000 → 008 đã ghi "009 nghe" (mức, nhóm người nhận, loại thông tin, khóa) cài `CoYeuCauThongBao`; module nguồn tự dựng yêu cầu. 009 có **một** listener `@ApplicationModuleListener` cho kiểu `CoYeuCauThongBao` (Spring phân phát theo kiểu giao diện), nên mọi module nguồn dùng đúng một cách gửi (FR-001). Module làm plan sau cũng vậy.
- Sự kiện chỉ để điều khiển 009 (không phải thông báo): `incident.api.YeuCauDungNhacGoiDien`, `incident.api.DaCoKetQuaXacNhanLai` (007) — listener riêng (B-08).
- Kiểm tra (FR-003): thiếu bản ghi nguồn, khóa, nhóm người nhận hoặc nội dung đầy đủ → lưu yêu cầu với kết quả `BI_TU_CHOI` và vào danh sách lỗi gửi; thiếu mức → Nhẹ, dấu `THIEU_MUC`; thiếu loại thông tin ở một phần → coi phần đó `SUC_KHOE`, dấu `THIEU_LOAI_THONG_TIN` (FR-013); Trung bình, Khẩn cấp thiếu nội dung rút gọn → nội dung mặc định cho kênh ngoài. Yêu cầu có nhóm người thân (loại (e), hoặc (d) `NGUOI_THAN`) mà thiếu `doiTuongThongTin` → coi là `VE_NGUOI_CAO_TUOI`, dấu `THIEU_DOI_TUONG_THONG_TIN` (FR-001a; bổ sung 2026-10-18). *(Bổ sung 2026-10-18, FR-022a)* Yêu cầu có nhóm `NGUOI_CAO_TUOI` mà mức khác Nhẹ hoặc có phần khác `CHUNG` → `BI_TU_CHOI`, mã `NHOM_NGUOI_CAO_TUOI_KHONG_HOP_LE`.
- **Chống trùng** (FR-005, NFR-04): `yeu_cau_thong_bao` có `UNIQUE (loai_ban_ghi_nguon, id_ban_ghi_nguon, khoa_su_kien)`; lần nhận trùng ghi `lan_nhan_trung` (đếm) và không tạo thông báo.

**Lý do**: Một khuôn duy nhất là yêu cầu của FR-001; đặt ở `common` tránh vòng phụ thuộc (M13 cần đọc dữ liệu của M01, M05, M09, M15). 000 R-09 đã chọn sự kiện có lưu trữ (outbox) làm đường đi.

**Phương án đã cân nhắc**:
- `ThongBaoApi.gui(...)` gọi đồng bộ trong `notification.api`: module thấp hơn phụ thuộc M13 → vòng; và lỗi gửi có thể làm hỏng lệnh nguồn (trái FR-006).
- 009 cài một bộ chuyển cho từng kiểu sự kiện (hàng chục kiểu): 009 phải biết nghiệp vụ của mọi module, trái "module nguồn sở hữu mức, nhóm, nội dung".

---

## B-03. Đường xử lý và độ trễ (FR-004, FR-006, FR-017, NFR-03)

**Quyết định**:
1. Lệnh nguồn ghi sự kiện vào outbox cùng transaction (R-09). Commit xong thì Modulith giao sự kiện ngay cho listener bất đồng bộ của 009. Với Khẩn cấp, "tiếp nhận" trùng thời điểm commit của lệnh kích hoạt (FR-006 ngoại lệ, FR-017).
2. Transaction tiếp nhận: lưu `yeu_cau_thong_bao` (FR-004) → xác định người nhận (B-04) → thêm `thong_bao` mỗi người, `lan_gui` dự kiến mỗi kênh (B-05), hoặc trạng thái giữ (B-06). *(R-15: `lan_gui` → `su_kien_chung`)*
3. Sau commit, bộ phát (`BoPhatKenh`) gửi các `lan_gui` song song trên luồng ảo; mỗi kết quả ghi một dòng `ket_qua_lan_gui` (nhóm 3) trong transaction ngắn. Lỗi kênh không chặn kênh khác hay người khác (FR-017). *(R-15: `lan_gui`, `ket_qua_lan_gui` → `su_kien_chung`)*
4. Khẩn cấp có hàng ưu tiên riêng (executor riêng, không xếp sau các thông báo Nhẹ/Trung bình đang dồn).
- **Mục tiêu**: Khẩn cấp đến thiết bị ≤ 10 giây (NFR-03, SC-001): outbox giao trong < 1 giây; xác định người nhận ≤ 1 giây (truy vấn theo chỉ mục 008, 012); đẩy qua FCM thường vài giây.
- Listener lỗi (ví dụ 012 tạm không đọc được) thì Modulith giao lại sự kiện (bản ghi outbox chưa hoàn tất); khóa `UNIQUE` của B-02 bảo đảm không gửi trùng.

**Lý do**: FR-006 (lỗi gửi không làm hỏng lệnh nguồn), NFR-03.

---

## B-04. Xác định người nhận (FR-007 → FR-015, BR-M13-04)

**Quyết định**:
- `NhomNhan` là kiểu đóng (sealed) ở `common.thongbao`, khớp năm loại của FR-007:

  | Loại | Biến thể | Nguồn tra cứu tại thời điểm gửi |
  | --- | --- | --- |
  | (a) nhiệm vụ | `DIEU_DUONG_PHU_TRACH(nct)`, `NHAN_VIEN_PHAN_CONG(nct \| phong, vaiTro?)`, `NHAN_VIEN_CO_CA(tang, vaiTro[], chungChi[])`, `NGUOI_PHU_TRACH_CA(tang)`, `BAC_SI_TRUC`, `TRUONG_TANG(tang)`, `TRUONG_DOAN(chuyen)`, `PHU_TRACH_BO_PHAN(boPhan)` | `NhiemVuApi`, `PhanCongApi` (`administration.taikhoan.api`, 008 cài); `NhanSuCanhBaoPort`/`CaTrucApi` (008) |
  | (b) vai trò toàn viện | `VAI_TRO(QUAN_LY_VIEN \| HANH_CHINH \| BAC_SI \| NHAN_VIEN_BEP \| KE_TOAN)` | `TaiKhoanApi` (002): tài khoản Hoạt động có vai trò |
  | (c) người có quyền | `NGUOI_CO_QUYEN_DUYET(loaiYeuCau, taiKhoanIds)` | Danh sách tài khoản do 000 tính sẵn khi phát sự kiện, đã lọc theo `gioiHanVaiTroDuyet` (sửa theo plan 004, Q-338: trước đây 009 tự gọi `NguoiDuyetApi.danhSach`) |
  | (d) gắn bản ghi nguồn | `NHAN_VIEN(id)`, `NGUOI_THAN(id)` (mang căn cứ: người lập, người thực hiện, người tạm nhận…) | Module nguồn đã phân giải |
  | (e) quan hệ người thân | `NGUOI_LIEN_HE_CHINH(nct)`, `NGUOI_DAI_DIEN(nct)`, `NGUOI_THAN_CO_QUYEN(nct, quyen)` | `NguoiThanNhanThongBaoPort` (012 cài) |
  | (f) chính người cao tuổi *(bổ sung 2026-10-18, Q-366)* | `NGUOI_CAO_TUOI(nct)` | `TaiKhoanApi.theoNguoiCaoTuoi` (002): tài khoản không ở Đã khóa, Không hoạt động; không có cách thay; rỗng thì không ghi `KHONG_CO_NGUOI_NHAN` |

- `CachThay` mỗi nhóm (a): `MAC_DINH`, `KHONG_THAY`, `NGUOI_THAY_CHI_DINH(NhomNhan)` (FR-010, Q-110). Hàm thuần `ChuoiThay(nhom)`: Điều dưỡng phụ trách → Người phụ trách ca của tầng → Trưởng tầng được giao → Quản lý viện; nhóm (a) khác → Trưởng tầng được giao → Quản lý viện; Trưởng tầng trống → Người phụ trách ca đang diễn ra của tầng (`NhiemVuApi.nguoiThayTruongTang`, Q-90) → Quản lý viện.
- **Tạm nhận** (FR-009): 007 và 008 đã phân giải người tạm nhận thành nhóm (d) `NHAN_VIEN(id)` kèm căn cứ `TAM_NHAN` (007 S-18), nên 009 không cần biết trạng thái tạm nhận.
- **Loại trừ** (FR-008): nhân viên vắng ca, Nghỉ việc, tài khoản Đã khóa hay Không hoạt động không là người nhận loại (a); tài khoản Khóa tạm vẫn là người nhận (Q-109).
- **Không có ai** (FR-011): ghi `KHONG_CO_NGUOI_NHAN` vào lỗi gửi; Khẩn cấp thì mọi tài khoản Quản lý viện và Trưởng tầng Hoạt động nhận thay, với `chiNhanDang = true` khi người đó không có phạm vi với người cao tuổi (nội dung chỉ họ tên, phòng, tầng, loại sự kiện).
- **Gộp trùng** (FR-012): mỗi người tối đa một `thong_bao` mỗi yêu cầu, `UNIQUE (yeu_cau_id, loai_nguoi_nhan, nguoi_nhan_id)`; ghi mọi căn cứ vào `can_cu` (JSON). Người gây ra sự kiện không nhận mức Nhẹ trừ khi `baoNguoiGayRa`.
- **Lọc người thân** (FR-013, FR-014): hàm thuần `LocPhanNoiDung(quyenNguoiThan, banDongYBaoGom, phanNoiDung)`:
  - `SUC_KHOE` chỉ khi có quyền xem sức khỏe và `BanDongYApi.baoGom(nct, nguoiThan)` (DBR-03);
  - `CHI_PHI`, `THAM_NOM`, `HOP_DONG` theo quyền tương ứng; `CHUNG` luôn;
  - người thân nhận nếu thấy được ít nhất một phần; nhóm (d) không có quan hệ với người cao tuổi chỉ phần `CHUNG` (Q-112);
  - Khẩn cấp: người liên hệ chính luôn nhận; người thân khác cần quyền "nhận thông báo khẩn" (Q-96).
- **Phạm vi "nhận thông báo" và mã trường hợp** (bổ sung 2026-10-18; FR-001a, FR-014, FR-014a, Q-353, Q-354): hàm thuần `XetPhamViDongY(yeuCau, nguoiThan, banDongY)` chạy cho từng người thân, trước `LocPhanNoiDung`:
  - `VE_VIEC_NGUOI_THAN`: không xét bản đồng ý; tiếp tục như trước.
  - `VE_NGUOI_CAO_TUOI` (hoặc thiếu): người thân **trong phạm vi** khi người cao tuổi có bản đồng ý Hiệu lực với `dongYThongBao = true` (`BanDongYApi.hienHanh`; plan, điểm báo lại 5). Trong phạm vi thì gửi như trước, không ghi mã.
  - Ngoài phạm vi, yêu cầu có `maTruongHop`: gửi (Khẩn cấp vẫn theo quy tắc Q-96), chỉ các phần người đó được thấy theo `LocPhanNoiDung`; ghi `ma_truong_hop` vào dòng `thong_bao` của người đó.
  - Ngoài phạm vi, không có mã, mức không khẩn: không tạo thông báo cho người đó (FR-014); không phải lỗi.
  - Ngoài phạm vi, không có mã, mức Khẩn cấp: không tạo thông báo cho người đó, ghi danh sách lỗi gửi `THIEU_MA_TRUONG_HOP` (FR-001a "từ chối … ghi vào danh sách lỗi gửi"); các nhóm nhân viên của yêu cầu vẫn gửi (plan, điểm báo lại 6).
  - Lúc gửi thật (giải phóng giờ yên tĩnh, FR-025) hàm chạy lại với bản đồng ý hiện hành.
- `MaTruongHopQ354` (`common.thongbao`): `KHAN_CAP_NGUOI_LIEN_HE_CHINH`, `DANH_SACH_TIEP_XUC`, `XAC_NHAN_LAI_NGUYEN_VONG`, `CHUYEN_VIEN` (chuyển viện, Điều trị tại bệnh viện), `QUA_DOI`. Trường hợp mới phải có Q mới (9.5) rồi mới thêm giá trị.
- **Qua đời** (FR-015): yêu cầu có `thoiDiemPhatSinh` sau thời điểm qua đời (`NguoiCaoTuoiApi.thoiDiemTrangThaiCuoi` với trạng thái Qua đời) không gửi người thân, trừ dấu `VIEC_SAU_QUA_DOI` của 004.
- Kết quả là danh sách `NguoiNhanDaXacDinh(loai, id, canCu[], phanDuocXem[], chiNhanDang)`; ghi đủ vào `thong_bao`.

**Lý do**: BR-M13-04 (theo phân công hiện tại); một hàm thuần dễ kiểm bằng bộ 200 yêu cầu (SC-004).

---

## B-05. Kênh, chọn kênh, gửi lại, gộp tin nhắn (FR-016 → FR-021)

**Quyết định**:
- **Kênh** (`kenh_thong_bao`, nhóm 1): `TRONG_UNG_DUNG` (nhân viên; người cao tuổi, bổ sung 2026-10-18, FR-022a), `CONG_NGUOI_THAN`, `TIN_NHAN`, `KHAC_<ma>`. Bật/tắt và gắn kênh khác vào mức do Quản lý viện cấu hình (FR-016), có nhật ký. *(R-15: `kenh_thong_bao` → `danh_muc_chung`)*
- **Hiện thực kênh**:
  - "Trong ứng dụng" / "cổng người thân": dòng `thong_bao` là hộp thông báo; đồng thời đẩy tới thiết bị qua **Firebase Cloud Messaging** (Firebase Admin SDK cho Java; `firebase_messaging` cho hai app Flutter) theo token thiết bị đăng ký ở 002 (Q-305); web nhận qua **Server-Sent Events** (`/api/v1/thong-bao/luong`) của Spring MVC. Kết quả đẩy không quyết định "Đã gửi" của kênh trong ứng dụng (thông báo đã lưu vào hộp là đã gửi, FR-018).
  - "Tin nhắn": SPI `NhaCungCapTinNhan` (`gui(soDienThoai, noiDung) → KetQua`); mặc định bean `ChuaCauHinh` trả `KENH_CHUA_CAU_HINH` (Q-06). Bộ chuyển cho nhà cung cấp cụ thể viết khi viện chọn; không đổi nghiệp vụ.
  - *(Bổ sung 2026-10-19)* Chủ dự án chọn dùng dịch vụ SMS bên ngoài: bộ chuyển `NhaCungCapTinNhanSms` gửi yêu cầu tới API của nhà cung cấp, đổi phản hồi thành `KetQua` (thành công, mã lỗi nhà cung cấp, lỗi tạm để gửi lại theo CFG-M13-05, lỗi vĩnh viễn như số không hợp lệ); khóa API đọc từ biến môi trường, không ghi vào kho mã; bean chỉ nạp khi có cấu hình, thiếu thì vẫn dùng `ChuaCauHinh`. **Nhà cung cấp (trong nước có brandname, hay AWS SNS…) chờ chủ dự án chốt**, cùng việc sửa Q-06 ở tài liệu nghiệp vụ (hiện ghi "chưa chọn; giai đoạn đầu chỉ trong ứng dụng"). Kênh `TIN_NHAN` bật hay tắt vẫn do Quản lý viện cấu hình (FR-016). Test dùng bản giả (009 T003), không gọi nhà cung cấp thật.
- **Chọn kênh** bằng hàm thuần `ChonKenh(muc, loaiNguoiNhan, dangTrongCa, kenhBat)` theo bảng FR-016 và FR-016a: nhân viên không đang trong ca (`CaTrucApi.dangTrongCa`; vai trò không xếp ca — Quản lý viện, Hành chính, Kế toán — trong ca khi trong CFG-M13-06) chỉ nhận Trung bình trong ứng dụng, tin nhắn ghi `BO_QUA_NGOAI_CA`; Khẩn cấp mọi kênh.
- **Nội dung kênh ngoài** (FR-019, FR-020): dùng nội dung rút gọn; `KiemTraNoiDungKenhNgoai` từ chối khi nội dung chứa trường thuộc danh mục sức khỏe chung. Cách kiểm: module nguồn dựng nội dung rút gọn từ `thamSoHienThi` có khóa; khóa thuộc danh mục `TruongSucKhoe` (chẩn đoán, dị ứng, bệnh nền, chỉ số, thuốc, kết quả đánh giá, nguyện vọng, nội dung sự cố; khai ở `common.thongbao`) bị cấm ở nội dung rút gọn; vi phạm → lần gửi kênh ngoài `NOI_DUNG_KHONG_HOP_LE`, lỗi gửi, phần trong ứng dụng vẫn gửi. Tin nhắn nhân viên không có họ tên người cao tuổi; tin nhắn người thân không có bản đồng ý dùng trần nội dung Q-72.
- **Kết quả, gửi lại** (FR-018): mỗi lần thử một dòng `ket_qua_lan_gui`; kênh ngoài thất bại thì gửi lại tối đa CFG-M13-05 lần (job B-10); Khẩn cấp xét "mọi kênh thất bại" ngay sau lần thử đầu. *(R-15: `ket_qua_lan_gui` → `su_kien_chung`)*
- **Gộp tin nhắn Trung bình** (FR-016b, Q-101, Q-107): bảng kỹ thuật `khung_gop_tin_nhan (nguoi_nhan, bat_dau, ket_thuc)`. Hàm thuần `KhungGop(thoiDiem, khungMo)`: không có khung mở → gửi ngay và mở khung CFG-M13-03; có khung → dồn (`DA_GOP` chờ); job mỗi 20 giây đóng khung hết hạn: có dồn thì gửi một tin tổng hợp (số thông báo, yêu cầu mở ứng dụng; nhân viên thêm tầng/khu, ca) và mở khung mới; người thân mà hết khung trong giờ yên tĩnh thì giữ tới khi hết giờ.
- **Liên hệ trực tiếp** (FR-021): Trung bình gửi người thân thất bại mọi kênh, hoặc người thân nhóm (d) không có kênh nào → thêm `lien_he_truc_tiep` (Chờ liên hệ); Hành chính ghi Đã liên hệ / Không liên hệ được (lý do). Job đầu ngày nhắc Hành chính, báo Quản lý viện cho mục còn chờ. *(R-15: `lien_he_truc_tiep` → `ho_so_chung`)*

**Lý do**: Q-305 chốt thông báo đẩy cho hai app; FCM là kênh đẩy chuẩn cho Flutter trên Android và iOS. SSE đủ cho web một chiều, không cần thêm thư viện WebSocket.

**Phương án đã cân nhắc**:
- Web polling mỗi vài giây: tải không cần thiết với 60 người dùng đồng thời; vẫn dùng làm dự phòng khi SSE mất kết nối.
- Tự dựng dịch vụ đẩy (APNs, FCM trực tiếp qua HTTP): thêm việc xác thực, không lợi gì ở quy mô đồ án.

---

## B-06. Giờ yên tĩnh (FR-022 → FR-025, BR-M13-03)

**Quyết định**:
- Hàm thuần `GioYenTinh(cfg13_02, t)`: khoảng có thể qua nửa đêm; mốc bắt đầu thuộc, mốc kết thúc không thuộc; trả `ketThucGanNhat`.
- Thông báo Nhẹ/Trung bình cho người thân rơi vào giờ yên tĩnh: `thong_bao` tạo với sự kiện `GIU`, `gui_du_kien = ketThucGanNhat`; không tạo `lan_gui`. *(Bổ sung 2026-10-18, Q-366)* Áp như vậy cho người nhận `NGUOI_CAO_TUOI` (chỉ có mức Nhẹ). *(R-15: `lan_gui` → `su_kien_chung`)*
- Job mỗi 20 giây giải phóng thông báo có `gui_du_kien <= bây giờ` theo thứ tự `tiep_nhan_luc`: xét lại điều kiện (quan hệ, quyền, Qua đời; FR-025) → `GUI` hoặc `KHONG_GUI` kèm lý do. Tin nhắn của các thông báo giải phóng cùng lúc cho một người thân gộp một tin tổng hợp (Q-101).
- `ThamSoDaDoi(CFG-M13-02)`: tính lại `gui_du_kien` của thông báo đang giữ theo giá trị mới (cập nhật bảng trạng thái kỹ thuật, B-09); nếu bây giờ không còn trong giờ yên tĩnh thì giải phóng ngay (FR-024).
- Khẩn cấp không bao giờ bị giữ (FR-023). Không có giờ yên tĩnh cho nhân viên.

---

## B-07. Xác nhận thông báo Khẩn cấp (FR-026, FR-026a)

**Quyết định**:
- `POST /thong-bao/{id}/xac-nhan` (trực tuyến; app chặn khi mất kết nối): thêm sự kiện `XAC_NHAN` (cách `TRONG_UNG_DUNG`, hoặc `XAC_NHAN_MUON` khi đang `KHONG_LIEN_LAC_DUOC`); dừng lượt nhắc gọi nếu mọi thông báo gộp vào lượt đã xác nhận (B-08). Mở thông báo chỉ ghi `XEM` (FR-026).
- **"Xác nhận và tiếp nhận"** (FR-026a, Q-102): `POST /thong-bao/{id}/xac-nhan-va-tiep-nhan`. Transaction 1 ghi xác nhận và commit. Transaction 2 gọi `CanhBaoLenhApi.tiepNhan` hoặc `SuCoLenhApi.tiepNhan` của 007 (bổ sung theo plan này vào `incident.api`) với người dùng hiện tại; 007 tự kiểm quyền và trạng thái. 007 từ chối thì phản hồi 200 kèm `tiepNhan = TU_CHOI` và lý do (ví dụ `DA_CO_NGUOI_TIEP_NHAN` kèm người và thời điểm); xác nhận vẫn còn.
- Nút "Xác nhận và tiếp nhận" chỉ hiện khi bản ghi nguồn là cảnh báo, sự cố đang cho phép tiếp nhận và người dùng có quyền: 009 hỏi `QuyenApi.lenhDuocPhep` (002) với đối tượng nguồn khi trả thông báo.

**Lý do**: Hai bản ghi riêng, xác nhận không phụ thuộc kết quả tiếp nhận (FR-026a).

---

## B-08. Lượt nhắc gọi, kết quả gọi, chuyển nhắc (FR-027 → FR-032, BR-M13-02, Q-95, Q-100, Q-103, Q-307)

**Quyết định**:
- `luot_nhac_goi` (nhóm 3 cho dòng tạo; trạng thái tính được, lưu ở bảng kỹ thuật `luot_nhac_goi_trang_thai`): người cần gọi (nhân viên hoặc người thân), người cao tuổi, chuỗi gọi (`chuoi_goi_id`), nội dung cần báo (ghép theo thời gian từ các thông báo gộp), tạo lúc. `luot_nhac_goi_thong_bao` (gộp, FR-027a). `chuyen_nhac` (nhóm 3): người được nhắc trước, sau, lý do `KHOI_TAO | QUA_HAN | HET_CA | GOP_CAP_NHAT`, thời điểm, hạn. `ket_qua_goi` (nhóm 3): người gọi, thời điểm, số đã gọi, kết quả `DA_LIEN_LAC | KHONG_LIEN_LAC`, lý do, người nghe máy, ghi chú, `bo_sung` (lần gọi bổ sung). *(R-15: `luot_nhac_goi_trang_thai` → `luot_nhac_goi` (cột dẫn xuất); `luot_nhac_goi_thong_bao`, `chuyen_nhac`, `ket_qua_goi` → `su_kien_chung`)*
- **Bất biến** (FR-032): mỗi thông báo thuộc tối đa một lượt (`UNIQUE (thong_bao_id)` trên `luot_nhac_goi_thong_bao`); mỗi (người cần gọi, người cao tuổi) tối đa một lượt Chờ kết quả: cột sinh `khoa_cho = IF(trang_thai = 'CHO_KET_QUA', CONCAT(loai_nguoi_can_goi, ':', nguoi_can_goi_id, ':', IFNULL(nct_id, CONCAT('nguon:', nguon_id))), NULL)` `UNIQUE` trên `luot_nhac_goi_trang_thai`. *(R-15: `luot_nhac_goi_thong_bao` → `su_kien_chung`; `luot_nhac_goi_trang_thai` → `luot_nhac_goi` (cột dẫn xuất))*
- **Tạo** (FR-027, FR-028): job mỗi 20 giây xét thông báo Khẩn cấp chưa xác nhận có `gui_luc + CFG-M13-01 <= bây giờ` mà chưa thuộc lượt nào, và thông báo Khẩn cấp vừa thất bại mọi kênh (xét ngay trong bộ phát). Khóa dòng `luot_nhac_goi_trang_thai` theo `khoa_cho`: có lượt Chờ → gộp (thêm `luot_nhac_goi_thong_bao`, `chuyen_nhac` lý do `GOP_CAP_NHAT`, thông báo nhắc cập nhật); không có → tạo lượt. *(R-15: `luot_nhac_goi_trang_thai` → `luot_nhac_goi` (cột dẫn xuất); `luot_nhac_goi_thong_bao`, `chuyen_nhac` → `su_kien_chung`)*
- **Người được nhắc gọi** (FR-029): hàm `NguoiNhacGoi(tang, nguoiCanGoi, t)`: Người phụ trách ca của tầng → Trưởng tầng được giao → Quản lý viện; bỏ người vắng ca, tài khoản không Hoạt động, chính là người cần gọi. "Người có quyền gọi": Người phụ trách ca đang diễn ra, Trưởng tầng được giao của tầng, mọi Quản lý viện; kiểm ở lệnh ghi kết quả (403 với người khác).
- **Lượt nhắc là một thông báo Khẩn cấp** gửi người được nhắc: 009 tự dựng `YeuCauThongBao` nội bộ (nguồn `M13.LUOT_NHAC_GOI`, khóa `(luotId, lanNhac)`), đi qua cùng đường gửi; số điện thoại không đặt trong thông báo mà hiển thị lúc mở (lấy hiện hành từ 002/012, FR-027).
- **Chuyển nhắc** (FR-030a): job xét lượt Chờ có hạn (`han_luc` = lần nhắc gần nhất + CFG-M13-04) đã qua → nhắc người kế tiếp; người được nhắc không còn trong ca → Người phụ trách ca đang diễn ra của tầng; đã tới Quản lý viện thì nhắc lại mỗi CFG-M13-04 và báo các Quản lý viện khác. Mỗi lần ghi `chuyen_nhac`. *(R-15: `chuyen_nhac` → `su_kien_chung`)*
- **Kết quả gọi** (`POST /luot-nhac-goi/{id}/ket-qua`): `DA_LIEN_LAC` → mọi thông báo gộp ghi `XAC_NHAN` cách `QUA_CUOC_GOI` (hoặc `XAC_NHAN_MUON_QUA_CUOC_GOI`); `KHONG_LIEN_LAC` → FR-029a: người thân kế tiếp chưa gọi trong chuỗi (thứ tự từ `NguoiThanNhanThongBaoPort.thuTuGoi`: người đại diện, rồi người có quyền nhận khẩn) → lượt mới cùng chuỗi (hoặc gộp vào lượt Chờ sẵn có của người đó); hết người → thông báo Khẩn cấp cho Quản lý viện kèm danh sách cuộc gọi; nhân viên → báo Quản lý viện. Mọi thông báo của lượt không còn lượt Chờ nào → sự kiện `KHONG_LIEN_LAC_DUOC` (FR-044). Lần gọi bổ sung sau "Không liên lạc được" được ghi với `bo_sung = true`.
- **Dừng**: người nhận tự xác nhận mọi thông báo của lượt → `DA_DUNG` "người nhận đã tự xác nhận", báo người được nhắc; người cần gọi không còn đủ điều kiện (quan hệ hết hiệu lực, mất quyền nhận khẩn) → `DA_DUNG` và áp FR-029a (a); `YeuCauDungNhacGoiDien(SU_CO, id)` của 007 → `DA_DUNG` "sự cố đã hạ mức", không tạo lượt mới cho chuỗi (BR-M05-22).
- Sự kiện `DaCoKetQuaXacNhanLai(canhBaoId)` của 007 (nguy kịch, FR-050b d): việc gọi xác nhận lại nguyện vọng do 007 ghi (S-13); 009 chỉ dừng các lượt nhắc gọi đang Chờ mà mọi thông báo gộp vào đều có bản ghi nguồn là cảnh báo nguy kịch đó, lý do "đã có kết quả xác nhận lại". Lượt có thông báo của nguồn khác giữ nguyên.

**Lý do**: Q-307 bỏ "yêu cầu gọi điện" như việc giao; lượt nhắc là thông báo, trạng thái tính được từ kết quả và xác nhận.

---

## B-09. Trạng thái thông báo: sự kiện ghi thêm và bảng trạng thái kỹ thuật (FR-036, FR-044)

**Quyết định**:
- `thong_bao` (nhóm 3): dòng tạo không bao giờ sửa. Mọi thay đổi là một dòng `su_kien_thong_bao` (nhóm 3): `GIU`, `GUI`, `KHONG_GUI`, `XEM`, `XAC_NHAN` (cách), `KHONG_LIEN_LAC_DUOC`, `AN` (người nhận ẩn), `TAI_DINH_THOI` (giờ yên tĩnh đổi). *(R-15: `su_kien_thong_bao` → `su_kien_chung`)*
- Trạng thái theo bảng FR-044 là hàm thuần `TrangThaiThongBao(suKien[], lanGui[])`. Để truy vấn hộp thông báo, danh sách chưa xem, chưa xác nhận nhanh, một bảng kỹ thuật `thong_bao_trang_thai` (khóa `thong_bao_id`) giữ trạng thái hiện hành, `gui_du_kien`, `xem_luc`, `xac_nhan_luc`, `an`; cập nhật trong cùng transaction thêm sự kiện. Đây là dữ liệu dẫn xuất kỹ thuật như `chuoi_dinh_chinh` của 000 R-08; test tích hợp kiểm nó luôn bằng hàm thuần trên sự kiện. *(R-15: `thong_bao_trang_thai` → `thong_bao` (cột dẫn xuất); `chuoi_dinh_chinh` → `dinh_chinh` (cột gốc, nếu không phá view))*
- Thời điểm xem lần đầu: chỉ thêm `XEM` khi chưa có (FR-034).

---

## B-10. Bộ lập lịch của 009 (FR-016b, FR-018, FR-021, FR-025, FR-027, FR-030a, FR-032, NFR-04)

**Quyết định**:

| Job | Chu kỳ | Việc |
| --- | --- | --- |
| `ViecNhanhM13` | Mỗi 20 giây | Tạo lượt nhắc gọi quá CFG-M13-01; chuyển nhắc quá CFG-M13-04, hết ca; đóng khung gộp tin nhắn; giải phóng thông báo hết giờ yên tĩnh; gửi lại kênh ngoài thất bại (CFG-M13-05) |
| `ViecHangNgayM13` | 07:30 | Nhắc Hành chính, báo Quản lý viện mục liên hệ trực tiếp còn chờ |

- Chu kỳ 20 giây để đạt SC-002 (≤ 1 phút sau hạn) và SC-003 (≤ 5 phút sau giờ yên tĩnh); là thông số vận hành, không phải tham số nghiệp vụ.
- Mọi việc khóa dòng và có ràng buộc `UNIQUE` (lượt nhắc, lần gửi lại theo số lần thử) nên chạy lại không gửi trùng (NFR-04). Job chỉ gửi, nhắc, chuyển trạng thái; không tạo bản ghi nghiệp vụ của module khác (000 R-10).

---

## B-11. Hộp thông báo, hiển thị theo quyền hiện hành, tra cứu (FR-033 → FR-038)

**Quyết định**:
- `GET /toi/thong-bao`: sắp theo `gui_luc` giảm dần, Khẩn cấp chưa xác nhận lên đầu; lọc mức, chưa xem, loại sự kiện, người cao tuổi; phân trang theo khóa.
- **Hiển thị theo quyền hiện hành** (FR-034, Q-99): khi trả nội dung, với mỗi phần: nhân viên — `QuyenXemTruongApi.duocXem(nd, nhomTuongUng, doiTuongNguon)` và `PhamViApi.trongPhamVi`; người thân — `LocPhanNoiDung` với quyền hiện hành. Không còn quyền thì chỉ trả tiêu đề, loại sự kiện, thời điểm. Liên kết bản ghi nguồn chỉ trả khi `QuyenApi.coQuyen(XEM)`.
- **Dữ liệu cho module nguồn** (FR-035): `ThongBaoTraCuuApi.theoNguon(loai, id)` trả mỗi người nhận: căn cứ, trạng thái, kết quả từng kênh, thời điểm gửi, xem, xác nhận và cách, lượt nhắc gọi và kết quả, dấu thiếu mức. Đây là bản cài `NhatKyThongBaoPort` của 007 và nguồn cho 012 (BR-M10-06), 000 (yêu cầu phê duyệt).
- **Tra cứu lịch sử** (FR-037): Quản lý viện toàn viện; Trưởng tầng theo `tang_khu_vuc_id` của yêu cầu thuộc tầng được giao; người xem bản ghi nguồn qua API của module nguồn. Nội dung sức khỏe theo giới hạn trường của 002. Chỉ mục `(loai_ban_ghi_nguon, id_ban_ghi_nguon)`, `(tang_khu_vuc_id, tiep_nhan_luc)`, `(nguoi_nhan_id, gui_luc)`.
- **Lỗi gửi** (FR-038): view trên `yeu_cau_thong_bao` có dấu (`BI_TU_CHOI`, `THIEU_MUC`, `THIEU_LOAI_THONG_TIN`, `KHONG_CO_NGUOI_NHAN`) và `thong_bao` gửi thất bại mọi kênh, `ket_qua_lan_gui` `NOI_DUNG_KHONG_HOP_LE`. *(R-15: `ket_qua_lan_gui` → `su_kien_chung`)*

---

## B-12. Quyền (FR-039)

**Quyết định**: Khóa quyền theo bảng FR-039:

| `ChucNang` | Thao tác |
| --- | --- |
| `THONG_BAO_CUA_TOI` | `XEM`, `XAC_NHAN` (mọi tài khoản; chỉ thông báo của mình) |
| `KET_QUA_GOI` | `GHI` (Người phụ trách ca theo nhiệm vụ; Trưởng tầng tầng được giao; Quản lý viện) — kiểm thêm "người có quyền gọi" của từng lượt (B-08) |
| `LIEN_HE_TRUC_TIEP` | `THUC_HIEN` (Hành chính); `XEM` (Quản lý viện) |
| `LICH_SU_THONG_BAO` | `XEM` (Quản lý viện toàn viện; Trưởng tầng tầng được giao) |
| `LOI_GUI` | `XEM` (Quản lý viện) |
| `KENH_THONG_BAO` | `CAU_HINH` (Quản lý viện); CFG-M13-01 → 06 đổi qua màn tham số của 000 |

---

## B-13. Lưu giữ, nhật ký, kiểm thử

**Quyết định**:
- **Lưu giữ** (FR-041): `LuuTruProvider` `DAI_HON(CFG-M15-04)` và theo chuỗi tham chiếu tới bản ghi nguồn (yêu cầu mang `loai_ban_ghi_nguon`, `id`; thông báo về người cao tuổi giữ ít nhất bằng CFG-M15-03 của hồ sơ). Thông báo không bị loại bỏ trước bản ghi nguồn (FR-051a của 000).
- **Nhật ký** (FR-040): đổi cấu hình kênh có nhật ký và lý do; hoạt động do hệ thống (giữ, gửi, không gửi, nhắc, chuyển nhắc, dừng) đã là dòng sự kiện nhóm 3 của chính 009 với người thực hiện "Hệ thống" và sự kiện kích hoạt; không ghi trùng vào `nhat_ky` từng lần gửi (khối lượng lớn), chỉ ghi lệnh của người dùng (xác nhận, kết quả gọi, liên hệ trực tiếp, cấu hình).
- **Phân loại trường**: phần nội dung `SUC_KHOE` của yêu cầu, nội dung cần báo của lượt nhắc → `SUC_KHOE`; số điện thoại → `LIEN_HE`.
- **Kiểm thử** (000 R-14, constitution XV):
  - test bảng cho hàm thuần `ChuoiThay`, `LocPhanNoiDung`, `ChonKenh`, `KhungGop`, `GioYenTinh`, `NguoiNhacGoi`, `TrangThaiThongBao`;
  - SC-004: bộ 200 yêu cầu trên phân công dựng sẵn (bản giả 008, 012 có dữ liệu);
  - đồng hồ giả lập cho CFG-M13-01 → 06, khung gộp, giờ yên tĩnh đổi giữa chừng (FR-042);
  - kênh giả lập: `NhaCungCapTinNhan` giả (bật/tắt, lỗi theo kịch bản), FCM giả ghi lại lần đẩy và thời điểm; đo SC-001 bằng thời điểm FCM giả nhận;
  - test đồng thời: hai yêu cầu cùng khóa; hai thông báo Khẩn cấp cùng người cao tuổi cùng lúc quá hạn (một lượt nhắc); ba người cùng "Xác nhận và tiếp nhận";
  - Flutter: nhận đẩy, xác nhận hai thao tác từ màn hình khóa (SC-008), chặn xác nhận khi mất kết nối, ghi kết quả gọi.
- Tên test theo BR/FR (`BR_M13_02_LuotNhacGoiIT`, `FR_016b_GopTinNhanTest`).
