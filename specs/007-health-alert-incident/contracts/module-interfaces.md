# Giao diện giữa module: Feature 007

**Ngày**: 2026-10-10 | **Plan**: [../plan.md](../plan.md)

Giao diện Java công khai ở `health.api` và `incident.api` (research S-01). Module khác chỉ dùng các giao diện này (constitution X). Kiểu `DieuKien`, `CanCu`, `LyDo`, `NguoiDungHienTai`, `DoiTuong` theo 000, 001, 002. Mọi hàm ghi có ghi chú `MANDATORY` chạy trong transaction của lệnh gọi; hàm đọc không khóa.

Plan làm sau (008, 009, 011, 012, 013, 014, 016) giữ đúng chữ ký hoặc ghi rõ thay đổi.

---

## 1. 007 cung cấp

### 1.1. SPI khai báo ở module thấp hơn, M05 và M06 cài

| SPI (gói khai báo) | Bản cài | Dùng bởi |
| --- | --- | --- |
| `ThamGiaChuyenTrangThai` (`resident.api`) | Hai bean, `@Order` sau 006: `health` rồi `incident`. Điều kiện `KT_D_CANH_BAO_SU_CO` (`coTheMien = true`) cho `KET_THUC_LUU_TRU`; tác động và `canhBao` theo research S-17 | 001 H-02 |
| `ChiSoApi.ganNhat(nctId)` (`resident.api` khai báo theo plan 001) | `health`: giá trị hiện hành gần nhất mỗi chỉ số kèm thời điểm | 001 FR-024 |
| `GiayPhepCoSoApi` (`administration.taikhoan.api`) | `health`: `coPhamVi(phamVi, ngay)`, `sapHetHan(trongVong)` (S-16) | 002 FR-037 (c), FR-039 |
| `TinhTrangLayNhiemChoPhanBo` (`roombedallocation.api`) | `incident`: `vungDangKhoanh(t)` (tập tầng, khu vực); `thuocDanhSach(nctId, t)` = nghi nhiễm hoặc tiếp xúc | 003 FR-009, FR-045 |
| `MucViecSauQuaDoiPort` (`admission.api`) | `incident`: `ma() = CANH_BAO_SU_CO` (S-17) | 004 FR-071 |
| `NguonCongViecPort` (`care.api`) | `health`: `LAN_DO` (lịch đo; trả "theo chỉ định" và mức quan trọng của lịch). `incident`: `THEO_DOI_SAU_NGA` (căn cứ `suCoId`), `THEO_DOI_TIEP_XUC` (căn cứ `nguoiTiepXucId`), `CANH_BAO_UONG_NUOC` (căn cứ `canhBaoId` loại `UONG_NUOC_THIEU` đang mở) | 005 FR-017, FR-023 |
| `ChiSoPort` (`care.api`) | `health`: `ghi(...)` trả `DA_LUU`, `BI_CHAN`, `CHO_DO_LAI` (S-06); giữ đúng chữ ký | 005 FR-034, BR-M04-10 |
| `CanhBaoDocPort` (`care.api`) | `incident`: `dangMo(nctIds)`; `theoCongViec(congViecIds)` qua `nguon_canh_bao` loại `CONG_VIEC` | 005 FR-029, FR-046 *(R-15: `nguon_canh_bao` → `su_kien_chung`)* |
| `NgoaiTuyenHandler` (`administration.taikhoan.api`) | `health`: `M06.BAN_GHI_CHI_SO` (gồm lần đo lại). Không có handler cho sự cố, kích hoạt khẩn cấp (bắt buộc trực tuyến) | 002 A-11, Q-01 |
| `DinhChinhDuocProvider` (000) | `M06.BAN_GHI_CHI_SO`, `M06.BAN_GHI_KHAM`, `M05.SU_CO`, `M05.DIEN_BIEN_SU_CO`, `M05.KET_QUA_XAC_NHAN_LAI`, `M05.LUA_CHON_THUC_HIEN` (S-06, S-11, S-13) | 000 R-08 |
| `PhanLoaiTruongProvider`, `LuuTruProvider` (000) | data-model mục 13, 14 | |
| `GanPhamViProvider` (002) | Mọi loại của 007: người cao tuổi và tầng/khu; sự cố cơ sở vật chất: tầng/khu của sự cố; khoanh vùng: các tầng của vùng | 002 FR-034 |
| `QuyenTepProvider` (`common.tep`) | `M06.SCAN_GIAY_PHEP_CO_SO`, `M06.TEP_KHAM`, `M05.TEP_SU_CO` | NFR-08 |

**Kiểu `chiTiet` 007 đặt khi gọi 001**: `CHUYEN_VIEN.canCu = { suCoId }` hoặc `{ canhBaoNguyKichId }`, cùng các trường 004 sở hữu (`donViDuaDi`, `duKienTroLai`, `nhanVienBanGiaoId`).

### 1.2. `health.api.ChiSoApi` (đọc; 005, 007 nội bộ, 008, 012, 016)

Tên Java đã triển khai cho nguồn chỉ số gần nhất: `VitalSignApi.latestPerType` (chi tiết mục 2.5). Giữ nguyên `aboveThresholdInRange` của phần bàn giao đã có.

```java
Map<Long, ChiSoGanNhatView> ganNhatMoiLoai(long nctId);                   // giá trị hiện hành (sau đính chính), thời điểm đo; FR-050
List<BanGhiChiSoView> trongKhoang(long nctId, long chiSoId, Instant tu, Instant den); // đã xác nhận, giá trị hiện hành; S-10
List<BanGhiChiSoView> vuotNguongTrongKhoang(long tangKhuVucId, Instant tu, Instant den); // ChiSoBanGiaoApi: bàn giao (FR-083)
Set<Long> chiSoSinhHieu();                                                 // FR-051
```

### 1.3. `health.api.LichDoApi` (đọc; 005, 016)

```java
List<LichDoView> hieuLuc(long nctId, Instant t);
List<Instant> mocDo(long lichDoId, Instant tu, Instant den);   // ChuoiMocDo: chỉ để hiển thị, không lập công việc (Q-306)
```

### 1.4. `health.api.SoLienHeVienApi` (009, 007)

```java
SoLienHeView hienHanh();   // FR-070: số liên hệ của viện cho thông báo khẩn cấp, tiếp xúc
```

### 1.5. `incident.api.CanhBaoBanGiaoApi` (008)

```java
List<MucBanGiaoView> dangMo(long tangKhuVucId, long caId);
    // FR-037: cảnh báo, sự cố đang mở của người thuộc tầng (kể cả vắng mặt, BR-M05-19); người phụ trách, mức, hạn, dấu tạm nhận.
    // Chỉ để người lập bàn giao xem và ghi (Q-307); 007 không lập mục bàn giao
void khiXacNhanBanGiao(long banGiaoId, long caTruocId, long caSauId, long tangKhuVucId);
    // MANDATORY, trong transaction xác nhận bàn giao của 008 (BR-M05-05, BR-M09-08, CHK016):
    // chuyển người phụ trách các mục thuộc ca trước và các mục tạm nhận; gỡ dấu tạm nhận; giữ trạng thái, cấp, hạn
```

### 1.6. `incident.api.SuCoLenhApi` (011, 013, 014; MANDATORY)

```java
long ghiNhan(GhiNhanSuCo lenh, NguoiDungHienTai nd);
    // Lệnh của actor ở module khác ghi nhận sự cố (Q-307): 011 "đã ăn" khi phục vụ suất bị chặn do dị ứng (loại PHUC_VU_SAI_SUAT_AN, nguồn AN_UONG; 011 FR-049); 013 "đồ gửi thất lạc/hư hỏng" (FR-040), 014 "Báo thiếu người"
    // (loại DI_LAC_KHONG_TRO_VE, nguồn HOAT_DONG_NGOAI_VIEN). Người ghi là nd; mọi kiểm tra và tác động FR-043 như lệnh REST
void themDienBien(long suCoId, String maLoaiDienBien, String noiDung, NguoiDungHienTai nd);   // 013: DA_TIM_THAY
    // FR-046b (sửa 2026-10-10, Q-307): không có hàm hủy sự cố theo nguồn đồ gửi; 013 chỉ báo người xử lý,
    // người xử lý đóng hoặc đính chính sự cố bằng lệnh (FR-045, FR-046)
void tiepNhan(long suCoId, NguoiDungHienTai nd);
    // bổ sung theo plan 009 (009 FR-026a "Xác nhận và tiếp nhận"): cùng kiểm tra quyền, trạng thái và tác động
    // như POST /incidents/{id}/receive; transaction riêng của 009; từ chối thì ném lỗi có maLoi (DA_CO_NGUOI_TIEP_NHAN…)
```

`incident.api.CanhBaoLenhApi` (009; bổ sung theo plan 009):

```java
void tiepNhan(long canhBaoId, NguoiDungHienTai nd);   // như POST /alerts/{id}/receive (FR-029, FR-036)
```

### 1.7. `incident.api.SuCoApi`, `CanhBaoApi` (đọc; 012, 013, 016)

```java
Optional<SuCoView> xem(long suCoId);                  // 013: trạng thái mở/đóng cho điều kiện đồ gửi (013 FR-021, FR-025)
List<SuCoView> theoNguoi(long nctId, Instant tu, Instant den, boolean chiChuaHuy);
boolean coMucDangMo(long nctId);                      // cảnh báo hoặc sự cố mở; dùng cho KT_D (S-17)
DauTheoDoiView dau(long nctId, Instant t);            // nghi nhiễm, tiếp xúc, theo dõi sau ngã, nguy kịch: thẻ người cao tuổi (005, 012)
```

### 1.8. `incident.api.KhoanhVungApi` (đọc; 003 qua SPI, 012, 014, 016)

```java
List<VungView> dangChiuTheoPhong(long phongId, Instant t);
List<VungView> dangChiuTheoTang(long tangKhuVucId, Instant t);
List<VungView> dangChiuTheoNguoi(long nctId, Instant t);   // theo giường đang giữ (BR-M10-02, BR-M04-15)
List<VungView> dangKhoanh();                                // dashboard (18.5)
```

### 1.9. `incident.api.SucKhoeCongNguoiThanApi` (012; `BanTinSucKhoeApi` bỏ theo Q-396)

```java
List<SuCoNguoiThanView> suCo(long nctId, long nguoiThanId, Instant tu, Instant den);
    // sửa 2026-10-18 (Q-352, research S-19a): mỗi sự cố có phanChung; phanSucKhoe chỉ khi BanDongYApi.baoGom;
    // 012 ghi nhật ký lượt xem phần sức khỏe (NFR-08, 012 FR-048)
record SuCoNguoiThanView(long suCoId, PhanChungSuCo phanChung, Optional<PhanSucKhoeSuCo> phanSucKhoe) {}
List<ChiSoNguoiThanView> chiSo(long nctId, long nguoiThanId, Instant tu, Instant den);
```

### 1.10. `incident.api.CanhBaoThongKeApi`, `health.api.ChiSoThongKeApi` (016; FR-081)

```java
Stream<CanhBaoBaoCaoView> canhBao(PhamViLoc phamVi, Instant tu, Instant den);
    // mức hiện hành, mức lúc tiếp nhận, lan_dau_luc, tiep_nhan_luc, số lần leo thang, dấu leo thang tối đa, loại, tầng; nguon (SOS / KHAC, bổ sung 2026-10-18, FR-081)
    // bổ sung 2026-10-22 (Q-374, Q-375): nguon thêm NUT_HO_TRO; daToiNoiLuc, khongGhiMocToiNoi (thời gian tới nơi, 18.3)
Stream<SuCoBaoCaoView> suCo(PhamViLoc phamVi, Instant tu, Instant den);
    // mức, loại (kèm dấu khong_thuoc_suc_khoe), tầng, thời điểm tạo, tiếp nhận xử lý (Q-199), số lần leo thang; bỏ Đã hủy
List<TheoDoiView> dangTheoDoi(PhamViLoc phamVi);        // sau ngã, tiếp xúc
List<Long> dangNguyKich(PhamViLoc phamVi);              // dấu Đang mở, Tạm, kèm tầng
Stream<ChiSoBaoCaoView> chiSo(PhamViLoc phamVi, Instant tu, Instant den);
    // lần đo đã xác nhận (lần đo lại thay bản ghi đầu; "chưa đo lại" được tính), mức vượt ngưỡng lúc đo theo thành phần nặng nhất
```

### 1.11. Sự kiện `health` phát (sau commit, qua outbox của 000)

| Sự kiện | Người nghe | FR |
| --- | --- | --- |
| `ChiSoDaXacNhan(banGhiId, nctId, chiSoId, muc, phienBanNguongId, congViecId, thoiDiemDo, nhan)` | `incident` (cảnh báo, sụt cân), 016 | FR-013, FR-014 |
| `ChiSoDaDinhChinh(banGhiGocId, nctId, chiSoId, mucGoc, mucMoi)` | `incident` | FR-019 |
| `LichDoDaDoi(lichDoId, nctId)` | 005 (khóa bỏ trùng) | FR-006 |
| `NhacThieuNguongCaNhan(nctId)` | 009 (Nhẹ, `BAC_SI_TOAN_VIEN`) | FR-025 |

### 1.12. Sự kiện `incident` phát

| Sự kiện | Người nghe | FR |
| --- | --- | --- |
| `CanhBaoDaTao`, `CanhBaoDaNangMuc` (mức, nhóm người nhận, `khongCoBacSiTruc`) | 009 | FR-035, FR-047a, FR-047b |
| `CanhBaoDaLeoThang(canhBaoId, capMoi, nhom[] khongThay)`; `NhacLeoThangToiDa(canhBaoId, soThuTu)` | 009 (Trung bình) | FR-033, FR-034 |
| *(sửa 2026-11-08, chủ dự án chốt 2026-11-07, "Đổi api")* Sự cố dùng chung hai sự kiện của cảnh báo, thêm loại nguồn: `AlertEscalated(sourceType, sourceId, escalationLevel, eventKey, occurredAt, notificationRequests)`, `MaximumEscalationReminder(sourceType, sourceId, sequence, eventKey, occurredAt, notificationRequests)`; `sourceType` là `M05.ALERT` hoặc `M05.INCIDENT`; bỏ `IncidentEscalated`, `IncidentMaximumEscalationReminder` (bổ sung 2026-11-04); sự cố giữ Mới | 009 (Trung bình; nhận qua `HasNotificationRequests`, không cần listener riêng) | FR-044 |
| `CanhBaoLapLai(canhBaoId, soLan)` | 009 | FR-038a |
| `NguonCanhBaoDaDinhChinh(canhBaoId)`; `NguoiPhuTrachDaDoi(loai, id, nguoiMoi)` | 009 (Nhẹ) | FR-019, FR-037 |
| `SuCoDaGhiNhan(suCoId, nctId, loai, muc, canDanhGiaLai)` (thêm `canDanhGiaLai` so với hợp đồng 001; plan, mục Đồng bộ 1) | 001, 012, 016 | FR-043 (b) |
| `SuCoDaTaoThongBao(suCoId, muc, nhom[])` | 009 | FR-043 (a) |
| `SuCoDaGiaoNguoiXuLy(suCoId, nguoiGiaoId, nguoiDuocGiaoId, muc)` *(bổ sung 2026-10-11, Q-339)* | 009 (mức theo mức hiện hành của sự cố; người nhận là người được giao) | FR-045a |
| `SuCoKhanCapDaKichHoat(suCoId, nctId, nhom[], noiDungNguoiThan, khongCoBacSiTruc)` | 009 (Khẩn cấp, song song) | FR-047 |
| `YeuCauDungNhacGoiDien(nguonLoai, nguonId)` | 009 | FR-044, BR-M05-22 |
| `SuCoNgaDaGhiNhan(nctId, suCoId)` | 005 (xem xét kế hoạch) | FR-051 |
| `CanLapCongViecTheoDoiSauNga(nctId, suCoId, moc, ketThuc, khoangDo)` | 009 (Người phụ trách ca của tầng) | FR-051, FR-052 |
| `CanLapDanhSachTiepXuc(suCoId, nctId)`; `DanhSachTiepXucCanXemLai(danhSachId)` | 009 | FR-060, FR-061 |
| `DaThemVaoDanhSachTiepXuc(nctId, thoiDiem)` | 003, 009 | FR-062 |
| `CanLapCongViecDoNhietDo(nctId, nguoiTiepXucId, tu, den)`; `ThongBaoTiepXuc(danhSachId, nguoiNhan[])` | 009 | FR-062 |
| `KhoanhVungDaTao(vungId, tangIds, khuVucIds)`; `KhoanhVungDaGo(vungId, tangIds, khuVucIds)` | 003, 012 (hủy lượt thăm), 014 (hủy buổi), 009, 016 | FR-064, FR-065 |
| `NguyKichDaGhi(dauId, canhBaoId, nctId, loai)`; `CanXacNhanLaiNguyenVong(canhBaoId, nctId)`; `CanXacNhanDauTam(dauId)`; `DaCoKetQuaXacNhanLai(canhBaoId)` | 009 | FR-050a, FR-050b, FR-050c |
| `CanChoTamVangVeNha(nctId, canhBaoId)` | 009 (Hành chính, Trưởng tầng) | FR-050d |
| `LuaChonChamSocGiamNheDaGhi(nctId)` | 005 | FR-050d |
| `SutCanPhatHien(nctId, banGhiCaoNhatId, banGhiMoiId)` | 011 (BR-M08-05) | FR-039a |
| `DauNghiNhiemDaDoi(nctId, suCoId, daGan, thoiDiem)` (bổ sung theo plan 014): gắn hoặc gỡ dấu "nghi nhiễm" | 014 (hủy đăng ký, chuyển Không đi, báo trưởng đoàn; 014 FR-021a) | FR-060 |
| `TacDongConHieuLuc(suCoId, danhSach)` | 009 | FR-046a |

Mỗi sự kiện mang khóa `loại + id + mốc` (NFR-04) và, với sự kiện gửi 009, mức, nhóm người nhận, loại thông tin theo research S-19.

### 1.13. Sự kiện `incident.api` cho module làm plan sau phát

```java
record YeuCauTaoCanhBao(long nctId, String loai, MucCanhBao muc, String loaiNguon, long nguonId,
                        Optional<String> khoaDoiTuong, Instant thoiDiem, String khoaSuKien) {}
    // 011 (xung đột dị ứng chế độ ăn, thực đơn; phục vụ suất dị ứng bị chặn), 014 (thiếu người khi về: KHAN_CAP; nguy cơ cô lập)
record YeuCauDongCanhBaoNguon(String loaiNguon, long nguonId, String lyDo, String khoaSuKien) {}   // FR-032
record YeuCauGhiDienBienCanhBao(String loaiNguon, long nguonId, String maDienBien, String noiDung, String khoaSuKien) {}
    // bổ sung theo plan 014 (research L-11): ghi diễn biến vào cảnh báo đang mở của nguồn, không đổi trạng thái, không đóng;
    // không có cảnh báo mở thì bỏ qua. 014: CO_LAP "đã tham gia hoạt động nhóm" (014 FR-056), NGUOI_THAM_GIA_CHUYEN "đã trở về" (014 FR-047)
```

---

## 2. 007 cần từ module khác

### 2.1. SPI 007 khai báo ở `incident.api`, module khác cài

```java
interface NhanSuCanhBaoPort {                      // 008 cài; chỉ đọc
  Optional<CaView> caChua(long tangKhuVucId, Instant t);            // giờ bắt đầu, kết thúc (hạn Nhẹ, FR-031)
  Optional<CaView> caKeTiep(long tangKhuVucId, Instant t);          // Nhẹ tạo sau mốc CFG-M09-04 (CHK020)
  Optional<Long> dieuDuongPhuTrach(long nctId, Instant t);          // người phụ trách ban đầu (FR-031)
  List<Long> bacSiTruc(Instant t);                                  // rỗng = không có Bác sĩ trực (FR-047b; nhiều người được)
  Optional<Long> truongTangDuocGiao(long tangKhuVucId, Instant t);  // cấp 1
  boolean coTenTrongCa(long nhanVienId, long caId);                 // "thuộc ca trước" (FR-037)
  boolean coTenTrongCaDangDienRa(long nhanVienId, Instant t);       // tạm nhận (Q-94)
  List<CaChuaXacNhanView> caDaBatDauChuaXacNhanBanGiao(Instant tu, Instant den);   // ca sau, ca trước, tầng
  List<PhanCongTruyVetView> phanCongTrongKhoang(long nctId, Instant tu, Instant den); // FR-060 (c)
  // bổ sung 2026-10-22 (FR-047i, Q-375; research S-24):
  boolean dangTrongCa(long nhanVienId, Instant t);                  // có ca đang diễn ra; vai trò không xếp ca: trong giờ hành chính CFG-M13-06
  Optional<Long> tangTrongCa(long nhanVienId, Instant t);           // tầng theo phân công ca đang diễn ra; rỗng: không có tầng (bếp, hành chính…)
  List<Long> tangDangCoCa(Instant t);                               // các tầng đang có ca, để báo Người phụ trách ca từng tầng khi chưa biết tầng
}

interface NguoiThanKhanCapPort {                   // 012 cài; chỉ đọc
  Optional<NguoiThanView> nguoiLienHeChinh(long nctId);             // thẻ khẩn cấp; nội dung FR-047
  List<NguoiThanView> nguoiDaiDien(long nctId);                     // FR-050b (c), thứ tự gọi
  List<LuotThamTruyVetView> luotThamTrongKhoang(long nctId, Instant tu, Instant den);
      // FR-060 (d): lượt Đã vào, Đã ra; người thân, người đi cùng thực tế (họ tên, quan hệ, số điện thoại), có kênh nhận hay không
}

interface DiemDanhTruyVetPort {                    // 014 cài; chỉ đọc
  List<BuoiCungThamGiaView> cungThamGia(long nctId, Instant tu, Instant den);   // FR-060 (b): buổi, thời điểm, người cùng điểm danh
}

interface ChuyenDiSosPort {                       // 014 cài; chỉ đọc; bổ sung 2026-10-18 (FR-047e, 014 FR-068a)
  Optional<ChuyenDangDienRaView> dangDienRa(long nctId);           // chuyến người cao tuổi đang ở Hoạt động bên ngoài; trưởng đoàn, người đi cùng hiện tại
}

interface NhatKyThongBaoPort {                     // 009 cài; chỉ đọc
  List<DaThongBaoView> theoNguon(String nguonLoai, long nguonId);   // người nhận, kênh, gửi lúc, đã xem lúc (FR-040, FR-048b)
}
```

Bean thiếu thì mặc định an toàn: không có `NhanSuCanhBaoPort` thì người phụ trách ban đầu là Người phụ trách ca (`PhamViApi`), không có Bác sĩ trực (áp FR-047b), hạn Nhẹ là cuối ngày; thiếu `NguoiThanKhanCapPort` thì thẻ ghi "không lấy được người liên hệ chính"; thiếu `DiemDanhTruyVetPort`, `NguoiThanKhanCapPort` khi tra cứu thì nhóm tương ứng trả "chưa có dữ liệu" và người lập được báo. Thiếu `ChuyenDiSosPort` thì cảnh báo SOS khi Hoạt động bên ngoài chỉ gửi nhóm của FR-047a (bổ sung 2026-10-18). *(Bổ sung 2026-10-22)* Thiếu `NhanSuCanhBaoPort` thì nút hỗ trợ khẩn cấp từ chối mọi lần bấm với `KHONG_TRONG_CA` kèm số liên hệ (không xác định được ca), chạy được với bản giả.

### 2.2. Sự kiện 007 nghe

| Sự kiện | Module | Xử lý | FR |
| --- | --- | --- | --- |
| `care.api.YeuCauCanhBao`, `YeuCauDongCanhBao` | 005 | S-09; công việc căn cứ `LAN_DO` dùng khóa `BO_LO_LAN_DO` | FR-026, FR-032, FR-039b |
| `care.api.CongViecDoBiBo(congViecId, nctId, lichDoId, chiSoIds, lyDo, mucQuanTrong)` (bổ sung theo plan này) | 005 | Quy tắc bỏ lỡ lần đo | FR-039b |
| `care.api.KetQuaNguDaGhi(ketQuaId, nctId, ngay, nhomGiaTri)` (bổ sung theo plan này; phát cả khi đính chính) | 005 | Quy tắc mất ngủ | FR-039c |
| `medication.api.YeuCauCanhBao`, `YeuCauDongCanhBao` | 006 | S-09; áp bật/tắt quy tắc `TU_CHOI_THUOC` | FR-026, FR-032 |
| `incident.api.YeuCauTaoCanhBao`, `YeuCauDongCanhBaoNguon` | 011, 014 | S-09 | FR-026 |
| `PhanBoDaDinhChinh(phanBoId, truoc, sau)` | 003 | `DanhSachTiepXucCanXemLai` | FR-061, Q-274 |
| `ThamSoDaDoi(CFG-M05-*, CFG-M06-*)` | 000 | Xóa bộ đệm; không tính lại kết quả cũ | FR-002, Edge Case |

### 2.3. Giao diện có sẵn hoặc đã bổ sung

| Giao diện | Module (feature) | Dùng cho | Khi chưa có |
| --- | --- | --- | --- |
| `NguoiCaoTuoiApi.trangThai`, `nhanDang`, `laTrangThaiCuoi`; `TrangThaiNguoiCaoTuoiApi.thucHien(CHUYEN_VIEN)` (*2026-10-22:* kèm `tepDinhKemIds`), `khoangTrangThai`; `NguyenVongApi.hieuLuc`, `ghiPhienBan`, `dieuTriHieuLuc` (*bổ sung 2026-10-22*, 001 FR-023); `MucSucKhoeApi.hieuLuc`; `DanhGiaApi.luuYHienHanh`; `BanDongYApi.baoGom` | M01 (001) | FR-031, FR-048, FR-049, FR-050c, FR-060, FR-025 | Có sẵn |
| `ViTriApi.hienTai`, `LichSuViTriApi.cungPhong`, `theoNguoi`, `CoCauViTriApi` | M03 (003) | Tầng, truy vết, khoanh vùng | Có sẵn |
| `DanhSachViecSauQuaDoiApi.capNhat` | M02 (004) | S-17 | Có sẵn |
| `CongViecDoChiSoApi.daXacNhan`, `NguonCongViecApi.nguonBiHuy`, `nguonDaDoi`, `CongViecThongKeApi.ketQuaTheoKhoang`; `CongViecThongKeApi.nguoiDaGhiKetQua(nctId, tu, den)` (bổ sung theo plan này) | M04 (005) | FR-018, FR-006, FR-052, FR-061, FR-039c, FR-060 (c) | Hàm cuối: bản giả |
| `DonThuocApi.thuocDangDung`; `LichThuocApi.nguoiDaGhiLieu(nctId, tu, den)` (bổ sung theo plan này) | M07 (006) | FR-048, FR-050, FR-060 (c) | Hàm cuối: bản giả |
| `QuyenApi` (gồm khóa `CHUYEN_MON`), `PhamViApi` (`boLoc`, `nguoiPhuTrachCa`, `truongTangDuocGiao`), `QuyenXemTruongApi`, `NguoiDungHienTai`, `TaiKhoanApi.coVaiTro` và `NguoiDuyetApi.danhSach` (Giao người xử lý, FR-045a, Q-339; bổ sung 2026-10-11), ngoại lệ ghi sự cố ngoài phạm vi (002 FR-044a) | M15 (002) | Research S-20 | Có sẵn |
| `NhatKyApi`, `ThamSoApi`, `DinhChinhApi`, `TepApi`, `common` | M15 (000) | Mọi lệnh | Có sẵn |
| Bản cài các SPI mục 2.1 | 008, 009, 012, 014 | | Bản giả có công tắc `/api/v1/kiem-thu/gia-lap/{module}` |
| *(Bổ sung 2026-10-18)* `ThietBiGanApi.xacDinh` | M15 (002) | FR-047d (S-23) | Bản giả |
| *(Bổ sung 2026-10-18)* `CoMatBanTruApi.theoNgay` | M04 (005) | FR-047d (S-23) | Có sẵn |
| *(Bổ sung 2026-10-22)* `FileApi.upload`, `attach` (`common.tep`); `common.bieumau` (`PdfBieuMau`) | `common` (A) | FR-050h (S-25) | `common.bieumau` làm trong task T104 *(xong 2026-10-22, gói `common.document`)* |
| *(Bổ sung 2026-10-22)* Nhóm người nhận `OnDutyDoctor`, `ShiftLead`, `OnShiftStaff` (`common.notification`) | 009 | FR-047i (S-24) | Có sẵn (009 Phase 2) |

### 2.4. Tên Java của SPI và bản giả T002 (2026-10-31)

"Đổi api" của D: bổ sung ba SPI đọc của mục 2.1, không sửa chữ ký SPI đã có `AlertStaffPort`. Gói `incident.api.spi`, DTO ở `incident.api.dto`; không thay hợp đồng HTTP. 012, 014, 009 cài SPI do 007 sở hữu để 007 không gọi ngược phần triển khai của các module đó.

```java
interface EmergencyFamilyPort { // NguoiThanKhanCapPort, 012 cài
  Optional<EmergencyFamilyContactView> primaryContact(long residentId);
  List<EmergencyFamilyContactView> representatives(long residentId);
  List<VisitTraceView> visitsInRange(long residentId, Instant from, Instant to);
}
record EmergencyFamilyContactView(long familyMemberId, String fullName, Optional<String> phoneNumber) {}
record VisitTraceView(long visitId, Instant enteredAt, Optional<Instant> exitedAt, List<VisitTracePersonView> people) {}
record VisitTracePersonView(Optional<Long> familyMemberId, String fullName,
    Optional<String> relationship, Optional<String> phoneNumber, boolean hasNotificationChannel) {}

interface AttendanceTracePort { // DiemDanhTruyVetPort, 014 cài; *(sửa 2026-11-08)* khai ở care.api.spi, DTO care.api.dto (C-02, team-plan)
  List<ActivityAttendanceTraceView> coAttendance(long residentId, Instant from, Instant to);
}
record ActivityAttendanceTraceView(long sessionId, Instant from, Instant to, List<Long> residentIds) {}

interface SosTripPort { // ChuyenDiSosPort, 014 cài (bổ sung 2026-11-06, T073)
  Optional<SosTripView> ongoing(long residentId);
}
record SosTripView(long tripId, List<Long> companionStaffIds) {} // trưởng đoàn nhận qua RecipientGroup.TripLeader(tripId)

interface NotificationLogPort { // NhatKyThongBaoPort, 009 cài
  List<IncidentNotificationView> bySource(String sourceRecordType, long sourceRecordId);
}
record IncidentNotificationView(String recipientType, long recipientId, String channel,
    Optional<Instant> sentAt, Optional<Instant> viewedAt, List<CallReminderSummary> callReminders) {}
// Kiểu lồng trong IncidentNotificationView
record CallReminderSummary(long callReminderId, String personToCallType, long personToCallId,
    String status, List<CallOutcome> results) {}
record CallOutcome(String result, Instant calledAt) {}
```

- Liên hệ: chỉ mã người thân, họ tên và điện thoại phục vụ FR-047/048; danh sách người đại diện giữ thứ tự gọi do 012 cung cấp (FR-050b). Lượt thăm gồm người thân và người đi cùng thực tế, không đưa lượt chưa vào vào nguồn truy vết (FR-060); `familyMemberId` có thể rỗng cho người đi cùng, `exitedAt` rỗng khi chưa ra.
- Buổi hoạt động trả nguồn buổi, khoảng thời gian và mã những người cao tuổi cùng Có mặt với người được truy vấn, kể cả chuyến (FR-060, module-interfaces 014). Truy vấn khoảng dùng chồng thời gian `[from, to)`, không tính hai khoảng chỉ chạm biên; không cắt mất khoảng gốc của nguồn.
- Nhật ký trả đúng cặp loại/mã nguồn, giữ mốc gửi/xem nếu có; một dòng cho một người nhận trên một kênh (FR-040, FR-048b). Loại người nhận và kênh giữ mã của 009; không phụ thuộc DTO của 009 để tránh chiều phụ thuộc ngược.
- Q-388: mỗi dòng mang đầy đủ các lượt của chuỗi nhắc gọi, kể cả lượt gọi người thân kế tiếp. Giữ mã người cần gọi, trạng thái và kết quả của 009. Kết quả gọi được người kế tiếp không xác nhận thông báo của người nhận ban đầu. A nối bản thật bằng `NotificationLookupApi.bySource`, đổi mã nguồn từ `long` sang `String`.
- Bộ fake duy nhất ở `support/fake/incident/`: `FakeAlertStaff` có sẵn; bổ sung `FakeEmergencyFamily`, `FakeAttendanceTrace`, `FakeNotificationLog`, cùng nạp qua `FakeM05Config`. Các hàm đặt dữ liệu chỉ dành cho test; chưa đặt thì trả rỗng, không tự sinh bản ghi, không gửi thông báo. Đây không phải bản cài nghiệp vụ thật hay endpoint giả lập HTTP.

### 2.5. API chỉ số gần nhất: phần T003 cho 001 (2026-10-31)

"Đổi api" của D, chỉ thêm hàm vào `health.api.VitalSignApi`, không sửa chữ ký bàn giao cũ:

```java
Map<Long, LatestVitalSignView> latestPerType(long residentId);

// health.api.dto
record LatestVitalSignView(long recordId, long indicatorId, String indicatorCode,
    String indicatorName, String unit, Map<String, BigDecimal> values, Instant measuredAt) {}
```

Đây là tên Java của `ganNhatMoiLoai` ở mục 1.2; tên logic `ChiSoApi.ganNhat` trong tài liệu 001 cùng trỏ nguồn này, không tạo một API hoặc bản giả thứ hai. Khóa map là mã danh mục chỉ số; mỗi loại tối đa một bản gần nhất, giá trị hiện hành sau đính chính và mốc đo gốc. Mã/tên/đơn vị lấy từ danh mục; giá trị từng thành phần giữ `BigDecimal`, không gắn cứng mã chỉ số, thành phần hoặc ngưỡng trong API.

Bổ sung khả năng đặt kết quả `FakeVitalSigns.latest(residentId, map)` vào bản giả chung có sẵn `support/fake/health/FakeVitalSigns`, giữ `source` và `aboveThresholdInRange` cho 008. Bản giả chỉ trả kết quả đã được test đặt, mặc định map rỗng, không cài thuật toán chọn bản gần nhất hoặc đính chính. Bản cài đọc thật được kiểm riêng bằng `LatestVitalSignReadIT`; consumer 001 vẫn do B nối. Không coi các test hợp đồng bản giả là kiểm chứng truy vấn dữ liệu thật.

### 2.6. T003 — tên Java đầy đủ cho API và sự kiện M06 (2026-10-31)

```java
List<VitalSignReadingView> VitalSignApi.withinRange(long residentId, long indicatorId, Instant from, Instant to);
Set<Long> VitalSignApi.vitalSignIndicatorIds();
List<MeasurementScheduleView> MeasurementScheduleApi.effectiveAt(long residentId, Instant at);
List<Instant> MeasurementScheduleApi.occurrences(long scheduleId, Instant from, Instant to);
FacilityContactView FacilityContactApi.current();
Stream<VitalSignReportView> VitalSignStatisticsApi.readings(ScopeFilter scope, Instant from, Instant to);
```

- Khoảng thời gian `[from, to)`. `VitalSignReadingView(id, residentId, indicatorId, values, level, confirmationStatus, measuredAt, labels)` dùng giá trị hiện hành; `level` là mức lưu tại lúc đo. `LatestVitalSignView` chọn bản CONFIRMED, bỏ bản đang chờ, bản đầu đã REMEASURED và bản bị hủy; thứ tự mốc đo rồi ID. Nếu bản mới bị hủy thì tìm bản hợp lệ cũ hơn. Lịch sử vẫn giữ bản đầu đã được đo lại; thống kê nghiệp vụ phải dùng API thống kê theo FR-081, không đếm trực tiếp lịch sử.
- `MeasurementScheduleView(id, residentId, indicatorIds, frequencyType, frequencyParameters, startsAt, endsAt, importance, note)`; `MeasurementFrequency`: `CLOCK_TIMES`, `EVERY_N_HOURS`, `WEEKDAYS`. Đọc lịch không sinh công việc. `FacilityContactView(phoneNumber, note, version)`; chưa có số cấu hình thì phone/note `null`, version 0 trong fake.
- `VitalSignReportView(recordId, residentId, indicatorId, floorAreaId, measuredAt, level)`; `ScopeFilter` dùng bản công khai 002 hiện có. Chưa cài thuật toán thống kê trong T003.
- `VitalSignLevel`: `NORMAL`, `WARNING`, `DANGEROUS`, `NO_THRESHOLD`; `VitalSignConfirmationStatus`: `AWAITING_REMEASUREMENT`, `CONFIRMED`, `REMEASURED`; `VitalSignLabel`: `IMMEDIATE_ACTION`, `NOT_REMEASURED`, `LATE_RECORDED`, `OFFLINE_RECORDED`.
- Sự kiện ở `health.api.events`, công khai NamedInterface `events`: `VitalSignConfirmed(recordId, residentId, indicatorId, level, thresholdVersionId, taskId, measuredAt, labels, eventKey, occurredAt)`; `VitalSignCorrected(originalRecordId, residentId, indicatorId, originalLevel, currentLevel, measuredAt, eventKey, occurredAt)` (`measuredAt` thêm 2026-11-06 cho sụt cân khi đính chính, FR-039a); `MeasurementScheduleChanged(scheduleId, residentId, eventKey, occurredAt)`; `MissingPersonalThresholdReminder(residentId, eventKey, occurredAt)`. Hai trường cuối theo phong bì sự kiện của 000; chưa thêm publisher nghiệp vụ trước khi có lệnh ghi.
- Nhắc thiếu ngưỡng dùng `HasNotificationRequests`, mức LOW, nhóm vai trò DOCTOR toàn viện theo FR-023; nội dung chỉ số xếp HEALTH, thông báo ngoài ứng dụng dùng câu trung tính. Không nhét danh sách người nhận vào outbox.
- Bản giả chung: mở rộng `FakeVitalSigns.history`/`vitalSignIndicators`; `FakeMeasurementSchedules`, `FakeFacilityContact`, `FakeVitalSignStatistics`, `FakeHealthEvents`, lắp bằng `FakeHealthConfig` (import cấu hình FakeVitalSigns có sẵn). Không thêm bean giả vào ứng dụng thật. Bộ giả lịch chỉ trả dữ liệu test đặt theo truy vấn, không cài lịch sinh mốc; thống kê dùng đúng phạm vi đã đặt và lọc thời gian.

### 2.7. T004 — phần giao diện M05 đã khai báo (rà 2026-11-01)

Tên Java của mục 1.6–1.10: `IncidentCommandApi.record(RecordIncidentCommand, CurrentUser)`, `addProgression(long, String, String, CurrentUser)`, `acknowledge(long, CurrentUser)`; `AlertCommandApi.acknowledge(long, CurrentUser)`. Bản cài lệnh thật phải dùng `MANDATORY`, kiểm quyền và tác động như REST; bản giả chỉ ghi lại lời gọi của test, không xác nhận nghiệp vụ production.

- `IncidentApi.view`, `byResident`, `hasOpenItem`, `monitoringFlag`; `AlertApi.view`, `byResident`, `hasOpenAlert`. Khoảng thời gian là `[from, to)`. Phần đọc sự cố giữ bản đã đóng trong lịch sử; cờ lọc bản hủy theo mục 1.7. Không dùng bản giả để suy ra quyền đọc thật.
- `IsolationApi.activeByRoom`, `activeByFloorArea`, `activeByResident`, `activeZones`.
- `FamilyPortalHealthApi.incidents`, `vitalSigns`: nguồn cho đúng cặp người cao tuổi/người thân. `FamilyIncidentView(incidentId, general, health)` tách `FamilyIncidentGeneralPart(typeName, occurredAt, level, processingStatus)` và `FamilyIncidentHealthPart` theo danh sách trường S-19a; `health` chỉ có khi bản đồng ý cho phép.
- `IncidentStatisticsApi.alerts`, `incidents`, `underMonitoring`, `criticalResidentIds` dùng `ScopeFilter`. Fake chỉ trả nguồn đã được test đặt cho đúng phạm vi; không lấy nguồn toàn viện cho một phạm vi chưa cấu hình. Các hàm đặt không có tham số phạm vi là nguồn `ScopeFilter.FACILITY`.
- Fake chung duy nhất: `FakeIncidents`, `FakeAlerts`, `FakeIncidentCommands`, `FakeIsolation`, `FakeFamilyPortalHealth`, `FakeIncidentStatistics`, nạp qua `FakeM05Config`. `AlertCommandApi` ủy quyền tới `FakeIncidentCommands.acknowledgeAlert`, tách đúng đích khỏi `IncidentCommandApi.acknowledge`.

### 2.8. T004 — sự kiện công khai hoàn tất (2026-11-01)

Gói `incident.api.events` công khai bằng `@NamedInterface`. Ngoài các sự kiện và yêu cầu đã có, T004 bổ sung `MaximumEscalationReminder`, `AlertRepeated`, `AlertSourceCorrected`, `AssigneeChanged`, `IncidentNotificationCreated`, `IncidentAssigned`, `EmergencyIncidentActivated`, `PostFallFollowUpRequested`, `ContactListRequested`, `ContactListReviewRequested`, `ContactTemperatureCheckRequested`, `ContactNotificationRequested`, `WishReconfirmationRequested`, `TemporaryCriticalFlagConfirmationRequested`, `TemporaryHomeLeaveRequested`, `PalliativeCareChoiceRecorded`, `WeightLossDetected`, `EffectiveImpactsRemain`.

Mọi sự kiện gửi 009 cài `HasNotificationRequests` và mang `List<NotificationRequest>`; publisher nghiệp vụ tạo yêu cầu với mức, nhóm người nhận, loại thông tin, `InformationSubject.ABOUT_RESIDENT` và `Q354Case` theo S-19. Danh sách yêu cầu nằm trong sự kiện để mỗi nhóm người thân có đúng mã trường hợp; không đưa danh sách id người nhận dài vào outbox. `FakeIncidentEvents.publish` phát được mọi sự kiện công khai bằng một fake chung. T004 chỉ chốt hợp đồng; task nghiệp vụ tương ứng chịu trách nhiệm phát sự kiện trong transaction thật.

**Bổ sung kiểm đủ danh sách, 2026-11-01:** `AlertCreated`, `AlertLevelRaised`, `AlertEscalated`, `CriticalFlagRecorded`, `IsolationCreated`, `IsolationLifted` cũng mang phong bì thông báo trên. `T004_IncidentEventCatalogTest` kiểm đủ các kiểu mục 1.12–1.13, không coi sự tồn tại kiểu là bằng chứng đã có publisher.

Tên Java để 009 nối listener: `CallReminderStopRequested(String sourceRecordType, long sourceRecordId)` = `YeuCauDungNhacGoiDien`; `WishReconfirmationCompleted(long alertId)` = `DaCoKetQuaXacNhanLai`. Hai sự kiện này điều khiển lượt nhắc, không cài `HasNotificationRequests`. 003 dùng `ContactAdded(long residentId, Instant at)` và `IsolationLifted(long zoneId, List<Long> floorAreaIds, List<Long> areaIds, String eventKey, Instant occurredAt, List<NotificationRequest> notificationRequests)`. Phần phát thật vẫn ở task hạ mức, xác nhận lại, tiếp xúc và khoanh vùng.

Yêu cầu module sau phát: `AlertRequested(residentId, type, level, sourceType, sourceId, Optional<String> objectKey, occurredAt, eventKey)`, `AlertSourceCloseRequested(sourceType, sourceId, reason, eventKey)`, `AlertProgressionRequested(sourceType, sourceId, progressionTypeCode, content, eventKey, actorId, occurredAt)` (*sửa 2026-11-08, FR-086, chủ dự án duyệt 2026-11-07*: thêm người thực hiện, thời điểm; 007 ghi diễn biến `M05.ALERT_PROGRESSION` vào cảnh báo đang mở của nguồn); không trực tiếp thực thi lệnh hoặc gửi tin khi khởi tạo record sự kiện.

**DTO đọc T004:** `IncidentView(id, Long residentId, type, level, status, createdAt, Long floorAreaId)`; `AlertView(id, Long residentId, type, level, status, firstAt, Long floorAreaId)`; `MonitoringFlagView(suspectedInfection, contact, postFall, critical)`. Đây là nguồn giữa module, không thay schema phản hồi REST. `RecordIncidentCommand` dịch đủ trường của `GhiNhanSuCo` trong hợp đồng HTTP: `residentId`, `incidentTypeId`, `source`, `occurredAt`, `detectedAt`, `floorAreaId`, `location`, `ongoingActivity`, `description`, `level`, `lowerLevelReason`, `initialResponse`, `createDespiteExisting`.

**Bổ sung rà T004 (2026-11-02):** `ContactAdded(residentId, at, eventKey, notificationRequests)` cũng cài `HasNotificationRequests` vì mục 1.12 có consumer 009. Giữ constructor hai tham số cho lời gọi nguồn đọc của 003; constructor này mang khóa theo người/thời điểm và danh sách yêu cầu rỗng. Publisher FR-062 phải dùng constructor đầy đủ với yêu cầu thông báo theo S-19, không coi constructor tương thích là đã gửi tin. `FakeIncidents` nhận `AlertApi` qua constructor; `FakeM05Config` nối đúng `FakeAlerts` dùng chung để `hasOpenItem` xét cả cảnh báo mở theo mục 1.7/S-17.

### 2.9. Nguồn thống kê chỉ số thật cho 016 (2026-11-01)

`DefaultVitalSignStatistics` cài `VitalSignStatisticsApi.readings`: chỉ dòng `CONFIRMED` trong `[from,to)`, kể cả nhãn `NOT_REMEASURED`; không đếm bản đầu `REMEASURED`, bản đang chờ hoặc bản đã hủy qua `CorrectionApi.current`. Giữ mức lúc đo theo FR-081. Phạm vi theo mã người cao tuổi hoặc tầng/phòng tại lúc đo từ `ResidentLocationApi.at`; phạm vi rỗng không trả dữ liệu. Không tự tạo bản ghi hoặc đọc SQL của module khác.
