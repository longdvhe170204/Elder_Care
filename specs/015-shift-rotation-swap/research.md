# Research: Lịch ca xoay vòng, phủ ca tối thiểu, đổi ca và nghỉ đột xuất (015)

> **Sửa 2026-10-29 (Q-397):** bỏ mẫu xoay ca, nhóm xoay ca, lập lịch từ mẫu, phủ tối thiểu, thiếu ca, cảnh báo thiếu phủ, lời mời nhận ca; 015 chỉ còn đổi ca và nghỉ đột xuất. Nghỉ "theo ngày" mở cho mọi nhân viên, người lập chọn phạm vi. Phần dưới còn nhắc các phần đã bỏ thì bỏ qua; spec là căn cứ.

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

Mỗi mục gồm Quyết định, Lý do, Phương án đã cân nhắc. Quyết định nền R-01 → R-14 của 000 và các mẫu đã chốt ở 001 → 014 (N-01 → N-15 của 008, đặc biệt N-03 khóa, N-07 port nội bộ cho 015; B-02 khuôn thông báo) được dùng lại, không quyết lại. Các quyết định dưới đây là kỹ thuật; chúng không đổi nghiệp vụ (constitution IX). Chỗ spec chưa rõ hoặc mâu thuẫn được ghi ở plan, mục "Điểm cần báo lại".

---

## X-01. Vị trí trong backend

**Quyết định**: Feature 015 thuộc module M09 (mục 13.2 phần lịch ca xoay vòng; UC-50 phần lập từ mẫu, UC-54), thêm gói con vào `com.eldercare.backend.staffing` (008 N-01).

| Gói con | Nội dung | Mục spec |
| --- | --- | --- |
| `staffing.xoayca` | Mẫu xoay ca, nhóm xoay ca, thành viên, lập lịch từ mẫu, lập lại, báo cáo lập lịch | A, B |
| `staffing.phuca` | Yêu cầu phủ tối thiểu (đọc CFG-M09-07), tính phủ, lần tính, cảnh báo thiếu phủ, mục "thiếu ca", kiểm tra khi công bố | A, C, F |
| `staffing.doica` | Yêu cầu đổi ca, yêu cầu nghỉ đột xuất (theo ca, theo ngày), lời mời nhận ca thay, tra cứu người thay | D, E, F |
| `staffing.xoayca.lich`, `staffing.doica.lich` | Job | |

015 chỉ đổi ca, phân công, người phụ trách qua port nội bộ của 008 (`CaLenhPort`, `PhanCongLenhPort`, bổ sung `LichNhapLenhPort`; X-03); ArchUnit chỉ cho `staffing.doica`, `staffing.xoayca` dùng các port này (008 N-07).

---

## X-02. Chiều phụ thuộc giữa module

**Quyết định**:

| Cặp | Cách nối |
| --- | --- |
| 015 → 008 (cùng M09) | Port nội bộ (X-03); đọc ca, mẫu ca, nhân viên, giấy phép/đào tạo, giao trưởng tầng, tỷ lệ phục vụ; cài `KiemTraCongBoMoRong`; nghe sự kiện nội bộ M09 (`LichCaDaCongBo`, `CaDaThayDoi`, `CaDaHuy`, vắng ca, `NhanVienDaNghiViec`, giấy phép/đào tạo hết hiệu lực, gia hạn) |
| 015 → 002 | Vai trò hệ thống, trạng thái tài khoản (`TaiKhoanApi.theoNhanVien`), `QuyenApi` |
| 015 → 005 | `CongViecCaApi.khiNghiCoDuyet`, `khiKetThucPhanCong` (MANDATORY; đã khai cho 015) |
| 015 → 006 | Không gọi: liều chung suy ra từ `DieuDuongCaPort` (008 cài) vốn đã loại người nghỉ có duyệt |
| 015 → 007 | Không gọi: khi ca không còn Bác sĩ trực, 007 đọc `NhanSuCanhBaoPort` (008 cài) lúc chọn người nhận (007 FR-047b) |
| 015 → 014 | `ChuyenDiNhiemVuLenhApi.goNhiemVu(nhanVienId, caId, canCu)` (MANDATORY, `care.api`, plan 014); đọc `ChuyenDiNhiemVuPort` (008 → 014) để hiển thị FR-021 (h), FR-027 |
| 015 → 009, 016 | Thông báo cài `CoYeuCauThongBao`; 016 đọc `XoayCaThongKeApi` |

**Lý do**: 015 làm plan sau 005, 008, 014 nên gọi thẳng API của họ; không tạo vòng (Modulith `verify()`).

---

## X-03. Port nội bộ M09 dùng và bổ sung

**Quyết định**: Dùng `CaLenhPort`, `PhanCongLenhPort`, cài `KiemTraCongBoMoRong` theo 008 N-07. Bổ sung theo plan này (gói `staffing.lichca.port`):

```java
interface LichNhapLenhPort {        // 008 cài; MANDATORY; chỉ staffing.xoayca dùng
  long taoNhap(PhamViCa phamVi, YearMonth thang, List<CaNhap> ca, CanCu canCu);
      // tạo lịch Nháp, ca, ca_nhan_vien với can_cu_vao_ca = LAP_LICH; kiểm FR-013, FR-014 của 008 theo từng dòng;
      // ném lỗi khi phạm vi đã có lịch tháng (015 FR-014)
  void thayNoiDungNhap(long lichCaId, List<CaNhap> ca, CanCu canCu);   // "Lập lại" (FR-013): chỉ lịch Nháp
  Optional<LichView> lichThang(PhamViCa phamVi, YearMonth thang);
}
interface NghiCoDuyetPort {         // 015 cài; 008 dùng
  boolean nghiTheoNgay(long nhanVienId, LocalDate ngay);
      // 008 FR-012: cảnh báo khi sửa tay lịch Nháp xếp nhân viên vào ngày đã duyệt nghỉ; 008 gọi
      // NghiCoDuyetPort.ghiXepLai(...) khi người sửa xác nhận (015 FR-026a)
  void ghiXepLai(long nhanVienId, LocalDate ngay, long lichCaId, LyDo lyDo, NguoiDungHienTai nd);   // MANDATORY
  List<YeuCauNghiView> nghiTheoNgayChoDuyet(long lichCaId);   // 008 FR-016: hiển thị khi công bố
}
```

`CaLenhPort.roiCa(caId, nhanVienId, CanCuRoiCa.NGHI_CO_DUYET(yeuCauId))` đặt `ca_nhan_vien.nghi_co_duyet_yeu_cau_id` (dòng giữ trong lịch sử ca), 008 tính lại tỷ lệ phục vụ; `roiCa(…, DOI_CA(yeuCauId))` đặt `roi_ca_luc`. `themVaoCa(…, CanCuVaoCa.DOI_CA | NHAN_THAY | LOI_MOI)` chạy cùng kiểm tra FR-013, FR-014 của 008.

**Lý do**: Lịch Nháp do lệnh của 015 lập "thuộc 008 từ lúc lệnh hoàn tất" (spec, Ngoài phạm vi); 008 giữ quyền ghi bảng lịch, ca.

---

## X-04. Khóa và đồng thời

**Quyết định**:
- Nối thứ tự khóa của 008 N-03: `nhan_vien` (theo `id` tăng) → `tang_ca_khoa` → `lich_ca` → `ca` → `phan_cong`; 015 thêm `yeu_cau_doi_ca` / `yeu_cau_nghi` / `canh_bao_thieu_phu` / `loi_moi` sau `ca`. *(R-15: `tang_ca_khoa` → `khoa_ky_thuat`; `yeu_cau_doi_ca`, `yeu_cau_nghi` → `ho_so_chung`)*
- **Đổi ca**: lệnh Duyệt khóa `nhan_vien` của A, B (theo `id`), rồi `ca` X, Y (theo `id`), rồi yêu cầu; kiểm FR-021 với dữ liệu tại lúc duyệt.
- **Ca không ở hai yêu cầu mở** (FR-019, FR-026): bảng `yeu_cau_ca_mo (ca_id, nhan_vien_id)` `UNIQUE`, dòng xóa khi yêu cầu vào trạng thái cuối hoặc ca rời yêu cầu (bảng kỹ thuật, không phải nhóm 3).
- **Một lời mời Chờ trả lời mỗi (ca, nhân viên)**: cột sinh `UNIQUE`.
- **Một cảnh báo thiếu phủ mở mỗi ca**: cột sinh `khoa_mo = IF(trang_thai = 'DANG_MO', ca_id, NULL)` `UNIQUE`.
- **Lập lịch từ mẫu**: khóa `tang_ca_khoa` của phạm vi (hoặc khóa toàn viện), kiểm "chưa có lịch tháng" trong khóa; hai lệnh song song → một thành công. *(R-15: `tang_ca_khoa` → `khoa_ky_thuat`)*

---

## X-05. Mẫu xoay ca, nhóm, xếp theo chu kỳ (mục A, B; BR-M09-09, Q-09, Q-190)

**Quyết định**:
- `mau_xoay_ca` (nhóm 1): loại phạm vi, độ dài chu kỳ, `mau_xoay_ca_ngay (thu_tu, mau_ca_id NULL = Nghỉ)`. Sửa/Ngừng không chạm lịch đã lập.
- `nhom_xoay_ca` (nhóm 1 có lịch sử): phạm vi, mẫu, `ngay_goc`, `thu_tu`; cấm sửa mẫu, ngày gốc khi đã từng có thành viên (trigger kiểm `da_tung_co_thanh_vien`). `thanh_vien_nhom (nhom_id, nhan_vien_id, vi_tri_bat_dau, tu_ngay, den_ngay NULL)`; chồng khoảng tham gia của một nhân viên ở hai nhóm bị chặn trong khóa `nhan_vien`.
- Hàm thuần `MauCaTaiNgay(nhom, thanhVien, ngay) = day[(ngay − ngay_goc + vi_tri) mod L]` (FR-004; số âm chuẩn hóa về [0, L)).
- Hàm thuần `LapLichThang(phamVi, thang, nhom[], thanhVien[], mauCa, nghiTheoNgay, trangThaiNv, caKhac)` → danh sách `CaNhap` (gộp thành viên cùng ngày, cùng mẫu ca vào một ca; ca kéo sang tháng sau thuộc tháng bắt đầu) và báo cáo: bỏ qua (không Đang làm việc, tài khoản Đã khóa, chồng giờ ca khác, mẫu ca ngừng, nghỉ có duyệt theo ngày), cảnh báo (vượt CFG-M09-02, đào tạo không hiệu lực), ca chưa có người phụ trách (luôn trống, Q-307), mục "thiếu ca" (FR-006a), ca thiếu phủ, ca dưới ngưỡng phục vụ (đọc 008 sau khi tạo).
- Lệnh "Lập lịch từ mẫu xoay ca" (Trưởng tầng phạm vi được giao; Quản lý viện cho toàn viện hoặc tầng chưa có trưởng tầng): `LapLichThang` → `LichNhapLenhPort.taoNhap` → tính phủ → ghi `lan_lap_lich` (nhóm 3) và báo cáo; một transaction ("hoặc toàn bộ, hoặc không"). Không có job lập lịch (Q-306).
- "Lập lại": `GET` xem trước các ca, phân công Nháp sẽ bị thay (so lịch hiện tại với kết quả mới); lệnh cần `xacNhan = true` và lý do; `thayNoiDungNhap`.

---

## X-06. Phủ tối thiểu (FR-005 → FR-007; BR-M09-11, Q-181, Q-188, Q-221)

**Quyết định**:
- CFG-M09-07 kiểu `BANG` (dòng: mẫu ca hoặc mọi, phạm vi, vai trò, số tối thiểu, thứ tự khai báo). Hàm `DongApDung(ca, cfg)` chọn dòng theo độ cụ thể (FR-005).
- Hàm thuần `TinhPhu(ca, dongApDung, nhanVienTinh)`: nhân viên được tính = có tên, không vắng, không nghỉ có duyệt, không rút; mỗi người chỉ đủ điều kiện cho dòng có **đúng** vai trò hệ thống của mình (Q-221) và giấy phép/đào tạo còn hiệu lực trong toàn ca (Q-188). Phân người vào dòng là bài toán ghép cặp hai phía có sức chứa: dùng luồng cực đại (đồ thị nhỏ: ≤ vài chục người, ≤ 10 dòng) với thứ tự dòng theo khai báo để tie-break xác định (FR-006: tối đa số dòng đạt, rồi ưu tiên thứ tự). Kết quả: `DAT` / `THIEU_PHU` với vai trò và số thiếu.
- `lan_tinh_phu` (nhóm 3) ghi mỗi lần tính (thời điểm, sự kiện, từng dòng: yêu cầu/đạt). Kích hoạt: lệnh của 015 (trong transaction); sự kiện 008 trong M09 (listener ngay sau commit: lưu Nháp, công bố, vắng ca, bổ sung, nghỉ việc, giấy phép/đào tạo đổi hiệu lực); `ThamSoDaDoi(CFG-M09-07)` → mọi ca chưa bắt đầu. Ca đã kết thúc không tính lại.
- Mục "thiếu ca" (FR-006a): `muc_thieu_ca` (nhóm 2 kỹ thuật) của lịch Nháp, tính lại khi lưu Nháp; mất khi có ca; khi công bố, ghi bản chụp vào nhật ký công bố và giữ cho báo cáo.

**Phương án loại**: tham lam theo thứ tự dòng — không bảo đảm số dòng đạt lớn nhất khi người có nhiều vai trò.

---

## X-07. Công bố (FR-015 → FR-018; Q-178)

**Quyết định**: Bản cài `KiemTraCongBoMoRong.kiemTra(lichCaId)` trả: ca thiếu phủ, mục "thiếu ca", yêu cầu nghỉ theo ngày còn Chờ duyệt — đều là cảnh báo không chặn; lệnh công bố của 008 khi có cảnh báo của 015 cần `xacNhanDaXem = true` và một lý do (dùng chung với lý do tự duyệt Q-10). Sau commit công bố (listener `LichCaDaCongBo`): mở cảnh báo thiếu phủ cho từng ca thiếu phủ; chuyển yêu cầu nghỉ theo ngày còn chờ sang dạng theo ca (FR-026a). Job 00:00: nhắc lập lịch tại mốc CFG-M09-06 (FR-008) và nhắc công bố từ mốc CFG-M09-10 mỗi ngày (FR-015); không phát bù mốc đã qua khi đổi tham số (lưu `moc_da_nhac` theo phạm vi, tháng).

---

## X-08. Yêu cầu đổi ca (mục D; BR-M09-10, UC-54, Q-179, Q-191, Q-185, Q-187, Q-10)

**Quyết định**:
- Loại yêu cầu phê duyệt `M09.DOI_CA` của 000 với ngoại lệ vòng đời đã khai ở 000 FR-031: `CHO_NGUOI_NHAN_DONG_Y → CHO_DUYET → DA_AP_DUNG`, cuối `NGUOI_NHAN_TU_CHOI`, `TU_CHOI`, `DA_HUY`, `HET_HIEU_LUC`. Handler cài `chuyenMoRong()` cho hai trạng thái riêng; bảng nghiệp vụ `yeu_cau_doi_ca` giữ dạng, A, X, B, Y, kết quả kiểm tra từng bước. *(R-15: `yeu_cau_doi_ca` → `ho_so_chung`)*
- Hàm thuần `KiemTraDoiCa(yc, trangThaiSauDoi)` → (a) chặn chồng giờ; (b) cảnh báo vượt CFG-M09-02; (c) phân công không chuyển được; (d) người phụ trách sau đổi; (e) phủ trước/sau (`TinhPhu`); (f) tỷ lệ phục vụ trước/sau (008); (g) giờ làm trong tháng (chỉ người duyệt); (h) chuyến đi chồng (cảnh báo). Chạy lúc lập, đồng ý, duyệt với dữ liệu tại thời điểm đó.
- Xem ca của B khi chọn Y (FR-020): endpoint trả DTO chỉ ngày, giờ, mẫu ca của các ca thỏa FR-019.
- **Duyệt** (FR-024), một transaction: `CaLenhPort.roiCa`/`themVaoCa` cho A, B; với mỗi phân công Tương lai: `PhanCongLenhPort.huy` + `kiemTra`/`lap` cho người kia, không đạt thì `CongViecCaApi.khiKetThucPhanCong` và nhắc trưởng tầng; người phụ trách: B nếu đạt 008 FR-015, nếu không người duyệt chọn trong thân lệnh, không có → 409 `CAN_NGUOI_PHU_TRACH` (Q-187); `doiNguoiPhuTrach` (hiệu lực như 008 FR-019); dấu Bác sĩ trực chuyển; `ChuyenDiNhiemVuLenhApi.goNhiemVu` nếu chồng chuyến; tính lại phủ; thông báo.
- **Hết hiệu lực**: job theo phút tại giờ bắt đầu ca sớm hơn; listener `CaDaHuy`, vắng ca, nghỉ việc, rút làm A/B không còn thỏa FR-019.
- **Nhắc** (FR-025): job theo phút tại giờ bắt đầu − CFG-M09-11; lập hoặc chuyển bước khi đã trong khoảng → nhắc ngay. `NhacQuaHan` của 000 (CFG-M15-05, 06) vẫn chạy.

---

## X-09. Yêu cầu nghỉ đột xuất (mục E; Q-81, Q-91, Q-182, Q-184, Q-185, Q-187)

**Quyết định**:
- Loại yêu cầu `M09.NGHI_DOT_XUAT` với ngoại lệ vòng đời: `CHO_DUYET → DA_AP_DUNG | DA_AP_DUNG_MOT_PHAN | TU_CHOI | DA_HUY | HET_HIEU_LUC`; dạng `THEO_CA` / `THEO_NGAY`, chuyển `THEO_NGAY → THEO_CA` khi lịch công bố. Bảng `yeu_cau_nghi_muc` (ca hoặc ngày, kết quả từng mục `CHO`, `DUYET`, `TU_CHOI`, `DA_TACH`, `DA_ROI`, ghi chú "xếp lại dù đã duyệt nghỉ"). *(R-15: `yeu_cau_nghi_muc` → cột `JSON` của yêu cầu nghỉ (`ho_so_chung`))*
- Lập: tách theo phạm vi thành nhiều yêu cầu trong cùng lệnh (FR-026, FR-026a); lập thay mang dấu.
- **Duyệt (theo ca)**, một transaction cho các ca được duyệt: `CaLenhPort.roiCa(…, NGHI_CO_DUYET)`; hủy phân công Tương lai (`PhanCongLenhPort.huy`); `CongViecCaApi.khiNghiCoDuyet(nhanVienId, caIds)`; người phụ trách: người duyệt chọn trong lệnh, không có → `ganThieuNguoiPhuTrach` (lệnh vẫn thực hiện, Q-187); dấu Bác sĩ trực mất; `goNhiemVu` chuyến; tính lại phủ, mở cảnh báo nếu chuyển thiếu phủ; thông báo.
- **Duyệt (theo ngày)**: ghi `nghi_theo_ngay (nhan_vien_id, ngay)` (nhóm 3, dùng cho `NghiCoDuyetPort`, `LapLichThang`); lịch Nháp có sẵn → gỡ nhân viên khỏi ca Nháp bắt đầu trong ngày qua `LichNhapLenhPort.thayNoiDungNhap` (chỉ phần của nhân viên đó; người phụ trách để trống và báo người lập lịch); tính lại phủ.
- **Tách ca / rời ngày** (FR-030, FR-026a): job theo phút tại giờ bắt đầu từng ca chờ; job 00:00 cho ngày chờ; nhắc trưởng tầng dùng "Ghi nhận vắng ca"; không còn mục → `HET_HIEU_LUC`.
- Thông tin trước khi duyệt (FR-027): phủ, tỷ lệ phục vụ trước/sau, người phụ trách, phân công, chuyến đi chồng.

---

## X-10. Cảnh báo thiếu phủ, tra cứu người thay, lời mời (mục F; Q-180, Q-186, Q-307)

**Quyết định**:
- `canh_bao_thieu_phu` (nhóm 2) `DANG_MO | DONG_DU_PHU | DONG_HET_CA`, vai trò và số thiếu hiện tại. Mở khi tính phủ chuyển `DAT → THIEU_PHU` trên ca Đã công bố/Đang diễn ra, hoặc khi công bố với ca thiếu phủ; cập nhật khi chi tiết đổi; đóng khi Đạt, tới giờ kết thúc, ca bị hủy. Job theo phút: giờ bắt đầu ca còn cảnh báo mở → nhắc trưởng tầng, người phụ trách ca, Quản lý viện; giờ kết thúc → đóng.
- **Tra cứu** (FR-033, FR-034): `GET /ca/{id}/tra-cuu-nguoi-thay?vaiTro=&dangKhongCoCa=true` trả nhân viên có vai trò, Đang làm việc, tài khoản không khóa, không có ca chồng giờ, không nghỉ có duyệt, sắp theo họ tên; kèm `gioDaLam`, `gioDaXepConLai` (hàm `GioTrongThang(nhanVien, thang)`: tổng thời lượng ca có tên, không vắng, không nghỉ, theo tháng chứa giờ bắt đầu ca) và cảnh báo vượt giờ liên tục. Không xếp hạng, không gợi ý. Chỉ người nhận cảnh báo và Quản lý viện.
- **Lời mời** `loi_moi_nhan_ca` (nhóm 2) `CHO_TRA_LOI | DA_NHAN | DA_TU_CHOI | DA_RUT | HET_HIEU_LUC`. Gửi: kiểm `DieuKienNguoiThay` (FR-033); thông báo người được mời mức Trung bình, nội dung chỉ ngày, giờ, phạm vi, vai trò (009 không gửi tin nhắn cho người ngoài ca, Q-186). Nhận: kiểm lại điều kiện và quyền "Bổ sung nhân viên vào ca" của người gửi tại lúc nhận; `CaLenhPort.themVaoCa(…, LOI_MOI(loiMoiId))` (nhật ký hai người: người ra lệnh là người gửi, người xác nhận là người nhận; căn cứ `LOI_MOI`); tính lại phủ; đủ phủ vai trò → lời mời khác của vai trò đó `HET_HIEU_LUC`.

---

## X-11. Quyền, riêng tư (FR-040, FR-045; Q-177)

**Quyết định**:

| `ChucNang` | Thao tác |
| --- | --- |
| `MAU_XOAY_CA` | `CAU_HINH` (Quản lý viện); `XEM` (Trưởng tầng) |
| `NHOM_XOAY_CA` | `THUC_HIEN` (Trưởng tầng tầng được giao; Quản lý viện mọi phạm vi; toàn viện chỉ Quản lý viện) |
| `LAP_LICH_TU_MAU` | `THUC_HIEN` (như nhóm xoay ca) |
| `DOI_CA` | `LAP`, `DONG_Y` (mọi nhân viên có ca); `DUYET` (Trưởng tầng được giao; Quản lý viện cho ca toàn viện, Q-177); `XEM` (Quản lý viện) |
| `NGHI_DOT_XUAT` | `LAP` (nhân viên cho mình); `LAP_THAY`, `DUYET` (Trưởng tầng được giao; Quản lý viện cho ca toàn viện) |
| `THIEU_PHU` | `XU_LY` (người nhận cảnh báo FR-032); `TRA_LOI_LOI_MOI` (người được mời) |

Riêng tư: lý do nghỉ, đổi ca mang `@NhomTruong(LY_DO_NHAN_SU)` (008), chỉ người lập, người liên quan, người duyệt, Quản lý viện; lý do từ chối của B chỉ A; kết quả tra cứu chỉ người nhận cảnh báo và Quản lý viện; DTO lời mời không có danh sách người được mời khác. Mọi lệnh chỉ trực tuyến (FR-043): không đăng ký `NgoaiTuyenHandler`; bản lưu thiết bị chỉ yêu cầu, lời mời của chính mình, không có lý do người khác.

---

## X-12. Thông báo (FR-044)

**Quyết định**: Mức theo FR-044 (Nhẹ / Trung bình; không Khẩn cấp). Lời mời, yêu cầu đổi ca gửi người nhận, nhắc gửi người ngoài ca: chỉ trong ứng dụng (009 FR-016a, Q-186) — 015 không đặt cờ đặc biệt, 009 tự áp quy tắc ngoài ca. Nội dung kênh ngoài ứng dụng không có lý do (FR-045). Khóa `(loại, yêu cầu / ca / lời mời[, mốc])`.

---

## X-13. Bộ lập lịch, lưu giữ, nhật ký, kiểm thử

**Quyết định**:
- **Job**: `ViecTheoPhutM09XoayCa` (yêu cầu hết hiệu lực, tách ca, nhắc CFG-M09-11, cảnh báo thiếu phủ tại giờ bắt đầu/kết thúc ca, lời mời hết hiệu lực khi hết ca); `ViecNuaDemM09XoayCa` 00:00 (nhắc lập lịch CFG-M09-06; nhắc công bố CFG-M09-10; ngày nghỉ theo ngày rời yêu cầu). Không lập lịch, không chỉ định người phụ trách, không mời người thay (Q-306, Q-307). Cột "đã làm lúc" chống trùng; không phát bù mốc đã qua khi đổi tham số.
- **Lưu giữ**: mọi bảng của 015 `KHONG_GAN_HO_SO`, ≥ CFG-M15-04 và ≥ ca liên quan (FR-046).
- **Nhật ký** (FR-046): mọi lệnh với căn cứ quyền; hệ thống ghi "Hệ thống" và sự kiện; dấu "tự duyệt", "lập thay".
- **Kiểm thử**:
  - test bảng `MauCaTaiNgay`, `LapLichThang` (SC-001: 3 mẫu × 4 tháng, ca đêm cuối tháng), `DongApDung`, `TinhPhu` (người nhiều vai trò, tie-break), `KiemTraDoiCa`, `DieuKienNguoiThay`, `GioTrongThang`;
  - bộ 200 yêu cầu (SC-004) gồm đồng thời, ca bị hủy khi chờ, duyệt một phần, nghỉ theo ngày trước/sau công bố;
  - Test tích hợp MySQL: `UNIQUE` cảnh báo mở mỗi ca, lời mời chờ, `yeu_cau_ca_mo`; trigger nhóm 3; *(R-15: `yeu_cau_ca_mo` → `khoa_duy_nhat` của `ho_so_chung`)*
  - test đồng thời: hai lệnh lập lịch cùng phạm vi; duyệt đổi ca cùng lúc với ghi vắng ca; hai người nhận lời mời cho chỗ cuối;
  - đồng hồ giả lập cho CFG-M09-02, 06, 10, 11, CFG-M15-05, 06.
- Tên test theo BR/FR (`BR_M09_09_LapLichTuMauIT`, `BR_M09_11_ThieuPhuIT`).
