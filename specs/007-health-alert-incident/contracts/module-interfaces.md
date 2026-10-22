# Giao diện giữa module: Feature 007

**Ngày**: 2026-10-10 | **Plan**: [../plan.md](../plan.md)

Giao diện Java công khai ở `m06.api` và `m05.api` (research S-01). Module khác chỉ dùng các giao diện này (constitution X). Kiểu `DieuKien`, `CanCu`, `LyDo`, `NguoiDungHienTai`, `DoiTuong` theo 000, 001, 002. Mọi hàm ghi có ghi chú `MANDATORY` chạy trong transaction của lệnh gọi; hàm đọc không khóa.

Plan làm sau (008, 009, 011, 012, 013, 014, 016) giữ đúng chữ ký hoặc ghi rõ thay đổi.

---

## 1. 007 cung cấp

### 1.1. SPI khai báo ở module thấp hơn, M05 và M06 cài

| SPI (gói khai báo) | Bản cài | Dùng bởi |
| --- | --- | --- |
| `ThamGiaChuyenTrangThai` (`m01.api`) | Hai bean, `@Order` sau 006: `m06` rồi `m05`. Điều kiện `KT_D_CANH_BAO_SU_CO` (`coTheMien = true`) cho `KET_THUC_LUU_TRU`; tác động và `canhBao` theo research S-17 | 001 H-02 |
| `ChiSoApi.ganNhat(nctId)` (`m01.api` khai báo theo plan 001) | `m06`: giá trị hiện hành gần nhất mỗi chỉ số kèm thời điểm | 001 FR-024 |
| `GiayPhepCoSoApi` (`m15.taikhoan.api`) | `m06`: `coPhamVi(phamVi, ngay)`, `sapHetHan(trongVong)` (S-16) | 002 FR-037 (c), FR-039 |
| `TinhTrangLayNhiemChoPhanBo` (`m03.api`) | `m05`: `vungDangKhoanh(t)` (tập tầng, khu vực); `thuocDanhSach(nctId, t)` = nghi nhiễm hoặc tiếp xúc | 003 FR-009, FR-045 |
| `MucViecSauQuaDoiPort` (`m02.api`) | `m05`: `ma() = CANH_BAO_SU_CO` (S-17) | 004 FR-071 |
| `NguonCongViecPort` (`m04.api`) | `m06`: `LAN_DO` (lịch đo; trả "theo chỉ định" và mức quan trọng của lịch). `m05`: `THEO_DOI_SAU_NGA` (căn cứ `suCoId`), `THEO_DOI_TIEP_XUC` (căn cứ `nguoiTiepXucId`), `CANH_BAO_UONG_NUOC` (căn cứ `canhBaoId` loại `UONG_NUOC_THIEU` đang mở) | 005 FR-017, FR-023 |
| `ChiSoPort` (`m04.api`) | `m06`: `ghi(...)` trả `DA_LUU`, `BI_CHAN`, `CHO_DO_LAI` (S-06); giữ đúng chữ ký | 005 FR-034, BR-M04-10 |
| `CanhBaoDocPort` (`m04.api`) | `m05`: `dangMo(nctIds)`; `theoCongViec(congViecIds)` qua `nguon_canh_bao` loại `CONG_VIEC` | 005 FR-029, FR-046 |
| `NgoaiTuyenHandler` (`m15.taikhoan.api`) | `m06`: `M06.BAN_GHI_CHI_SO` (gồm lần đo lại). Không có handler cho sự cố, kích hoạt khẩn cấp (bắt buộc trực tuyến) | 002 A-11, Q-01 |
| `DinhChinhDuocProvider` (000) | `M06.BAN_GHI_CHI_SO`, `M06.BAN_GHI_KHAM`, `M05.SU_CO`, `M05.DIEN_BIEN_SU_CO`, `M05.KET_QUA_XAC_NHAN_LAI`, `M05.LUA_CHON_THUC_HIEN` (S-06, S-11, S-13) | 000 R-08 |
| `PhanLoaiTruongProvider`, `LuuTruProvider` (000) | data-model mục 13, 14 | |
| `GanPhamViProvider` (002) | Mọi loại của 007: người cao tuổi và tầng/khu; sự cố cơ sở vật chất: tầng/khu của sự cố; khoanh vùng: các tầng của vùng | 002 FR-034 |
| `QuyenTepProvider` (`common.tep`) | `M06.SCAN_GIAY_PHEP_CO_SO`, `M06.TEP_KHAM`, `M05.TEP_SU_CO` | NFR-08 |

**Kiểu `chiTiet` 007 đặt khi gọi 001**: `CHUYEN_VIEN.canCu = { suCoId }` hoặc `{ canhBaoNguyKichId }`, cùng các trường 004 sở hữu (`donViDuaDi`, `duKienTroLai`, `nhanVienBanGiaoId`).

### 1.2. `m06.api.ChiSoApi` (đọc; 005, 007 nội bộ, 008, 012, 016)

```java
Map<Long, ChiSoGanNhatView> ganNhatMoiLoai(long nctId);                   // giá trị hiện hành (sau đính chính), thời điểm đo; FR-050
List<BanGhiChiSoView> trongKhoang(long nctId, long chiSoId, Instant tu, Instant den); // đã xác nhận, giá trị hiện hành; S-10
List<BanGhiChiSoView> vuotNguongTrongKhoang(long tangKhuVucId, Instant tu, Instant den); // ChiSoBanGiaoApi: bàn giao (FR-083)
Set<Long> chiSoSinhHieu();                                                 // FR-051
```

### 1.3. `m06.api.LichDoApi` (đọc; 005, 016)

```java
List<LichDoView> hieuLuc(long nctId, Instant t);
List<Instant> mocDo(long lichDoId, Instant tu, Instant den);   // ChuoiMocDo: chỉ để hiển thị, không lập công việc (Q-306)
```

### 1.4. `m06.api.SoLienHeVienApi` (009, 007)

```java
SoLienHeView hienHanh();   // FR-070: số liên hệ của viện cho thông báo khẩn cấp, tiếp xúc
```

### 1.5. `m05.api.CanhBaoBanGiaoApi` (008)

```java
List<MucBanGiaoView> dangMo(long tangKhuVucId, long caId);
    // FR-037: cảnh báo, sự cố đang mở của người thuộc tầng (kể cả vắng mặt, BR-M05-19); người phụ trách, mức, hạn, dấu tạm nhận.
    // Chỉ để người lập bàn giao xem và ghi (Q-307); 007 không lập mục bàn giao
void khiXacNhanBanGiao(long banGiaoId, long caTruocId, long caSauId, long tangKhuVucId);
    // MANDATORY, trong transaction xác nhận bàn giao của 008 (BR-M05-05, BR-M09-08, CHK016):
    // chuyển người phụ trách các mục thuộc ca trước và các mục tạm nhận; gỡ dấu tạm nhận; giữ trạng thái, cấp, hạn
```

### 1.6. `m05.api.SuCoLenhApi` (011, 013, 014; MANDATORY)

```java
long ghiNhan(GhiNhanSuCo lenh, NguoiDungHienTai nd);
    // Lệnh của actor ở module khác ghi nhận sự cố (Q-307): 011 "đã ăn" khi phục vụ suất bị chặn do dị ứng (loại PHUC_VU_SAI_SUAT_AN, nguồn AN_UONG; 011 FR-049); 013 "đồ gửi thất lạc/hư hỏng" (FR-040), 014 "Báo thiếu người"
    // (loại DI_LAC_KHONG_TRO_VE, nguồn HOAT_DONG_NGOAI_VIEN). Người ghi là nd; mọi kiểm tra và tác động FR-043 như lệnh REST
void themDienBien(long suCoId, String maLoaiDienBien, String noiDung, NguoiDungHienTai nd);   // 013: DA_TIM_THAY
    // FR-046b (sửa 2026-10-10, Q-307): không có hàm hủy sự cố theo nguồn đồ gửi; 013 chỉ báo người xử lý,
    // người xử lý đóng hoặc đính chính sự cố bằng lệnh (FR-045, FR-046)
void tiepNhan(long suCoId, NguoiDungHienTai nd);
    // bổ sung theo plan 009 (009 FR-026a "Xác nhận và tiếp nhận"): cùng kiểm tra quyền, trạng thái và tác động
    // như POST /su-co/{id}/tiep-nhan; transaction riêng của 009; từ chối thì ném lỗi có maLoi (DA_CO_NGUOI_TIEP_NHAN…)
```

`m05.api.CanhBaoLenhApi` (009; bổ sung theo plan 009):

```java
void tiepNhan(long canhBaoId, NguoiDungHienTai nd);   // như POST /canh-bao/{id}/tiep-nhan (FR-029, FR-036)
```

### 1.7. `m05.api.SuCoApi`, `CanhBaoApi` (đọc; 012, 013, 016)

```java
Optional<SuCoView> xem(long suCoId);                  // 013: trạng thái mở/đóng cho điều kiện đồ gửi (013 FR-021, FR-025)
List<SuCoView> theoNguoi(long nctId, Instant tu, Instant den, boolean chiChuaHuy);
boolean coMucDangMo(long nctId);                      // cảnh báo hoặc sự cố mở; dùng cho KT_D (S-17)
DauTheoDoiView dau(long nctId, Instant t);            // nghi nhiễm, tiếp xúc, theo dõi sau ngã, nguy kịch: thẻ người cao tuổi (005, 012)
```

### 1.8. `m05.api.KhoanhVungApi` (đọc; 003 qua SPI, 012, 014, 016)

```java
List<VungView> dangChiuTheoPhong(long phongId, Instant t);
List<VungView> dangChiuTheoTang(long tangKhuVucId, Instant t);
List<VungView> dangChiuTheoNguoi(long nctId, Instant t);   // theo giường đang giữ (BR-M10-02, BR-M04-15)
List<VungView> dangKhoanh();                                // dashboard (18.5)
```

### 1.9. `m05.api.SucKhoeCongNguoiThanApi`, `BanTinSucKhoeApi` (012)

```java
List<SuCoNguoiThanView> suCo(long nctId, long nguoiThanId, Instant tu, Instant den);
    // sửa 2026-10-18 (Q-352, research S-19a): mỗi sự cố có phanChung; phanSucKhoe chỉ khi BanDongYApi.baoGom;
    // 012 ghi nhật ký lượt xem phần sức khỏe (NFR-08, 012 FR-048)
record SuCoNguoiThanView(long suCoId, PhanChungSuCo phanChung, Optional<PhanSucKhoeSuCo> phanSucKhoe) {}
List<ChiSoNguoiThanView> chiSo(long nctId, long nguoiThanId, Instant tu, Instant den);
BanTinSucKhoeView choBanTin(long nctId, LocalDate tu, LocalDate den);
    // FR-082: cân nặng và xu hướng, chỉ số chính, sự cố trong kỳ kèm mức (bỏ sự cố Đã hủy, FR-046a)
    // sửa 2026-10-18 (Q-352): mỗi sự cố gồm phanChung (kèm mức, 012 FR-052) và phanSucKhoe; 012 lọc theo người nhận
```

### 1.10. `m05.api.CanhBaoThongKeApi`, `m06.api.ChiSoThongKeApi` (016; FR-081)

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

### 1.11. Sự kiện `m06` phát (sau commit, qua outbox của 000)

| Sự kiện | Người nghe | FR |
| --- | --- | --- |
| `ChiSoDaXacNhan(banGhiId, nctId, chiSoId, muc, phienBanNguongId, congViecId, thoiDiemDo, nhan)` | `m05` (cảnh báo, sụt cân), 016 | FR-013, FR-014 |
| `ChiSoDaDinhChinh(banGhiGocId, nctId, chiSoId, mucGoc, mucMoi)` | `m05` | FR-019 |
| `LichDoDaDoi(lichDoId, nctId)` | 005 (khóa bỏ trùng) | FR-006 |
| `NhacThieuNguongCaNhan(nctId)` | 009 (Nhẹ, `BAC_SI_TOAN_VIEN`) | FR-025 |

### 1.12. Sự kiện `m05` phát

| Sự kiện | Người nghe | FR |
| --- | --- | --- |
| `CanhBaoDaTao`, `CanhBaoDaNangMuc` (mức, nhóm người nhận, `khongCoBacSiTruc`) | 009 | FR-035, FR-047a, FR-047b |
| `CanhBaoDaLeoThang(canhBaoId, capMoi, nhom[] khongThay)`; `NhacLeoThangToiDa(canhBaoId, soThuTu)` | 009 (Trung bình) | FR-033, FR-034 |
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
| `TacDongConHieuLuc(suCoId, danhSach)`; `SuCoDaHuyTrongBanTin(suCoId)` | 009; 012 | FR-046a |

Mỗi sự kiện mang khóa `loại + id + mốc` (NFR-04) và, với sự kiện gửi 009, mức, nhóm người nhận, loại thông tin theo research S-19.

### 1.13. Sự kiện `m05.api` cho module làm plan sau phát

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

### 2.1. SPI 007 khai báo ở `m05.api`, module khác cài

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
| `m04.api.YeuCauCanhBao`, `YeuCauDongCanhBao` | 005 | S-09; công việc căn cứ `LAN_DO` dùng khóa `BO_LO_LAN_DO` | FR-026, FR-032, FR-039b |
| `m04.api.CongViecDoBiBo(congViecId, nctId, lichDoId, chiSoIds, lyDo, mucQuanTrong)` (bổ sung theo plan này) | 005 | Quy tắc bỏ lỡ lần đo | FR-039b |
| `m04.api.KetQuaNguDaGhi(ketQuaId, nctId, ngay, nhomGiaTri)` (bổ sung theo plan này; phát cả khi đính chính) | 005 | Quy tắc mất ngủ | FR-039c |
| `m07.api.YeuCauCanhBao`, `YeuCauDongCanhBao` | 006 | S-09; áp bật/tắt quy tắc `TU_CHOI_THUOC` | FR-026, FR-032 |
| `m05.api.YeuCauTaoCanhBao`, `YeuCauDongCanhBaoNguon` | 011, 014 | S-09 | FR-026 |
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
| *(Bổ sung 2026-10-22)* `FileApi.upload`, `attach` (`common.tep`); `common.bieumau` (`PdfBieuMau`) | `common` (A) | FR-050h (S-25) | `common.bieumau` làm trong task T104 |
| *(Bổ sung 2026-10-22)* Nhóm người nhận `OnDutyDoctor`, `ShiftLead`, `OnShiftStaff` (`common.notification`) | 009 | FR-047i (S-24) | Có sẵn (009 Phase 2) |
