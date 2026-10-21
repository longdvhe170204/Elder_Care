# Quickstart: Kiểm chứng feature 002

**Ngày**: 2026-10-07 | **Plan**: [plan.md](plan.md)

Hướng dẫn dùng ở giai đoạn triển khai; giai đoạn thiết kế chưa có mã nên các lệnh chưa chạy được. Hợp đồng xem [contracts/api-002.yaml](contracts/api-002.yaml) và [contracts/module-interfaces.md](contracts/module-interfaces.md); bảng và ràng buộc xem [data-model.md](data-model.md).

## 1. Điều kiện cần

- Như quickstart của 000: JDK 25, MySQL 8.0 cài trên máy (000 R-14), Node.js LTS, Flutter stable; profile `demo` có đồng hồ giả lập.
- Dữ liệu mẫu của profile `demo` (cùng bản giả cho 008, 003, 012, 001, 004, 007 khi các module đó chưa có):
  - hai tầng (T1, T2), mỗi tầng hai phòng; người cao tuổi A (T1), B (T2) có phân bổ giường mở;
  - một tài khoản cho mỗi vai trò nhân viên (AC-01 → 05, 07, 09, 14), phân công tầng 1 với vai trò theo phân công; thêm một Quản lý viện thứ hai;
    *(2026-10-21, T079a: `demo/V002_90__sample_accounts.sql`)* tên đăng nhập `demo.quanly`, `demo.quanly2` (AC-01), `demo.truongtang` (AC-02), `demo.bacsi` (AC-03), `demo.dieuduong` (AC-04), `demo.chamsoc` (AC-05), `demo.bep` (AC-07), `demo.hanhchinh` (AC-09), `demo.ketoan` (AC-14); mật khẩu chung `Demo2026!`, chỉ dùng cho `demo`; hồ sơ nhân viên id 9001 → 9009 theo thứ tự trên. Phân công tầng 1 thêm khi 008 có dữ liệu demo;
  - ca ngày 07:00–18:00 T1, có Người phụ trách ca là Điều dưỡng N; Trưởng tầng H được giao T1;
  - bác sĩ BS có giấy phép đúng phạm vi, hết hạn ngày D cấu hình được; giấy phép cơ sở có phạm vi khám chữa bệnh;
  - người thân R (người đại diện của A, có quyền xem sức khỏe, nằm trong bản đồng ý), M (quyền xem chi phí), cả hai chưa có tài khoản.
- Tài khoản Quản lý viện đầu tiên của môi trường thật được tạo bằng lệnh `khoi-tao-quan-ly-vien` một lần khi cài đặt (FR-004b, BR-M15-22, research A-03).

## 2. Chạy

```text
backend/  ./mvnw verify
backend/  ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
frontend/ npm ci && npm test && npm run dev
mobile/   flutter test
mobile/  flutter test
```

Kết quả mong đợi: `verify` xanh, gồm `MaTranQuyenKhopNguonTest` (ma trận khớp Phụ lục 27 và 19.3) và các nhóm test ở mục 4.

## 3. Kịch bản kiểm chứng đầu–cuối (profile demo)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| K1 | Nhân viên chăm sóc T1 xem hồ sơ A, rồi B | A: 200; B: 404 thông báo chung | US1 #2, FR-043 |
| K2 | Nhân viên bếp mở hồ sơ sức khỏe A; mở phiếu bữa ăn có suất đặc biệt của A | 403/404; phiếu có họ tên, phòng, chế độ ăn, món thay thế, kết cấu, không có dị ứng; `truongBiAn` liệt kê trường bị bỏ | US1 #6, FR-022 |
| K3 | Hành chính xem phần sức khỏe A | Chỉ `mucChamSoc`; dị ứng, thuốc nằm trong `truongBiAn` | US1 #11, FR-022 |
| K4 | Tua đồng hồ tới D+1; BS kê đơn nội bộ cho A | 403 `DIEU_KIEN_PHAP_LY` kèm tên giấy phép, ngày hết hạn; BS vẫn xem được hồ sơ | US1 #4, US5 #2 |
| K5 | Gia hạn giấy phép BS ở bản giả 008; kê đơn lại | 200, không cần cấp quyền | US5 #6, SC-006 |
| K6 | QLV tạo tài khoản cho hồ sơ nhân viên mới, vai trò AC-04 | 201; mật khẩu tạm trả một lần; đăng nhập lần đầu chỉ gọi được `doi-mat-khau` | US2 #1, US3 #9 |
| K7 | Bản giả 008 chuyển nhân viên đó sang Nghỉ việc trong một lệnh | Tài khoản `DA_KHOA` cùng transaction; yêu cầu kế tiếp của phiên cũ 401; nhật ký người thực hiện Bộ lập lịch, căn cứ BR-M09-04 | US2 #4, SC-004 |
| K8 | Đăng nhập sai 5 lần; đăng nhập đúng ngay sau; tua 15 phút; đăng nhập đúng | `TAI_KHOAN_KHOA_TAM`; vẫn bị từ chối; Hoạt động, đăng nhập được | US3 #2, #3 |
| K9 | Khóa tạm Điều dưỡng D ở T1 giữa ca; N (Người phụ trách ca T1) mở khóa sớm; Người phụ trách ca T2 thử mở | 200 kèm `canCu` nhiệm vụ; 403 | US3 #5, FR-019c |
| K10 | Đăng nhập tài khoản X rồi tài khoản Y trên cùng `X-Thiet-Bi` | Phiên X kết thúc; yêu cầu của X trả 401 | FR-014a, BR-M15-15 |
| K11 | QLV gỡ vai trò AC-01 của chính mình khi chỉ còn một QLV | 409 `KHONG_CON_QUAN_LY_VIEN` | US2 #9, FR-012 |
| K12 | Hai QLV cùng thu hẹp quyền một tài khoản với cùng `If-Match` | Một bên 200, một bên 412 kèm quyền hiện hành | FR-026, BR-M15-17 |
| K13 | QLV thêm quyền xem đơn thuốc cho AC-05 | 409 `VUOT_QUYEN_TOI_DA` | US7 #3, FR-025 |
| K14 | QLV gán quyền Duyệt kế hoạch chăm sóc cho Điều dưỡng N; N duyệt kế hoạch của A; Điều dưỡng khác thử | 200; 403 | US7 #2, FR-024 |
| K15 | `GET /tai-khoan/{id}/quyen?taiThoiDiem=<trước K14>` | Không có quyền Duyệt kế hoạch | FR-026a, BR-M15-16 |
| K16 | Đổi phân công Nhân viên chăm sóc từ T1 sang T2; xem A rồi B ngay sau | A: 404; B: 200 | US4 #4, SC-003 |
| K17 | Ca kết thúc 18:00, CFG-M15-07 = 2 giờ; xem A lúc 19:30, rồi 20:30 | 200; 404 | US4 #8 |
| K18 | Nhân viên bếp ghi nhận sự cố ngã cho B (ngoài phạm vi) | Chỉ thấy họ tên, ảnh, phòng/giường; tạo được; nhật ký `ngoaiPhamVi` | US1 #9, FR-044a |
| K19 | Đồng bộ ba bản ghi: (a) ghi 17:00, tài khoản khóa 18:30, đồng bộ 19:00; (b) ghi 18:45 sau khi khóa; (c) `lanKetNoiCuoi` cách lúc đồng bộ quá 24 giờ | (a) `CHAP_NHAN_KEM_NHAN`, H được báo; (b) `TU_CHOI`; (c) `CHO_XEM_LAI`; H từ chối không lý do bị 422 | US4 #12, #14, FR-041a, FR-041c |
| K20 | Phân công trỏ tới tầng đã ngừng hiệu lực; xem hồ sơ; ghi nhận sự cố | 403 `DU_LIEU_NGUON`, QLV được báo một lần; sự cố vẫn ghi được | US4 #13, FR-041b |
| K21 | Hành chính tạo tài khoản cho R với `GOI_LAI_SO_DA_DANG_KY` và số khác số đã đăng ký; rồi số đúng | 409 `XAC_MINH_KHONG_HOP_LE`; 201 | US6 #8, #9, FR-052 |
| K22 | R và M đăng nhập app người thân, xem A | R thấy sức khỏe; M chỉ thấy chi phí. Rút bản đồng ý của R: lần xem kế tiếp R không thấy sức khỏe | US6 #1 → #3 |
| K23 | Tua đồng hồ quá CFG-M15-02 không đăng nhập; chạy job | Tài khoản người thân `KHONG_HOAT_DONG`; tài khoản nhân viên không đổi | US3 #6, #7 |
| K25 | Đổi mật khẩu thành "abcdefgh", rồi "abcd1234" | 422 `MAT_KHAU_KHONG_DAT`; 204 | US3 #10, FR-006a |
| K26 | Web: thao tác lúc 09:40, tua đồng hồ tới 10:20, bấm tiếp; app nhân viên: thao tác 15:00, tua tới 17:30 | Web 401 `PHIEN_HET_HAN`, tài khoản vẫn Hoạt động; app vẫn dùng được | US3 #11, FR-013a |
| K27 | Chạy `khoi-tao-quan-ly-vien` trên cơ sở dữ liệu trống, rồi chạy lại | Lần đầu tạo hồ sơ và tài khoản AC-01 buộc đổi mật khẩu; lần sau từ chối | US2 #10, FR-004b |
| K24 | A Kết thúc lưu trú; tua quá CFG-M01-04; chạy job | Tài khoản R, M `DA_KHOA` (nếu không còn người cao tuổi khác) | US6 #5, #6 |
| K28 | *(Bổ sung 2026-10-18)* HC tạo tài khoản người cao tuổi cho A (Đang lưu trú) không có `xacNhan`; rồi có `xacNhan` YEU_CAU với HC; tạo lần hai | 422; 201, vai trò AC-16; 409 `DA_CO_TAI_KHOAN` | US8 #1, FR-067 |
| K29 | Điều dưỡng D phụ trách A tạo mã QR 09:00; quét trên máy M1 lúc 09:03; quét lại cùng mã; tạo mã 09:10, tua tới 09:16 rồi quét | 201; 200 kèm `token`, `tokenThietBi`; 401 `MA_QR_KHONG_HOP_LE`; 401 | US8 #2, FR-069 |
| K30 | Tua đồng hồ 200 ngày; A gọi `/toi`; chạy job Không hoạt động | 200, phiên còn; tài khoản vẫn Hoạt động | US8 #3, FR-010, FR-013a |
| K31 | A quét mã mới trên M2; gọi API bằng token cũ trên M1; D đăng nhập tài khoản của mình trên M2 | Token M1 401; M2 gắn; 409 `THIET_BI_DA_GAN` | US8 #4, #5, FR-070, FR-014a |
| K32 | Gọi `ThietBiGanApi.xacDinh` với `tokenThietBi` của M2 khi không có phiên; HC gỡ thiết bị; gọi lại | Trả A; rỗng | FR-071, SC-013 |
| K33 | HC lập yêu cầu `M15.KHOA_TAI_KHOAN_NCT` theo đề nghị của R; QLV duyệt | Tài khoản Hoạt động tới khi duyệt; sau duyệt `DA_KHOA` `THEO_DE_NGHI_NGUOI_THAN`; R không có endpoint khóa | US8 #7, FR-072 |
| K34 | Bản giả 001 phát `NguoiCaoTuoiDaChuyenTrangThai` A → Kết thúc lưu trú; HC thử `mo-khoa-nguoi-cao-tuoi` | `DA_KHOA` `NCT_KET_THUC_LUU_TRU`, thiết bị gỡ; 409 | US8 #6, BR-M15-23 |
| K35 | R (AC-10) gọi `GET /tai-khoan/{id}` của tài khoản A | 403/404 | US8 #9, FR-075 |

## 4. Test tự động: truy vết kịch bản → test

| Spec | Lớp test (backend, `com.cambae.backend.m15.taikhoan…`) | Loại |
| --- | --- | --- |
| US1 #1–13, FR-016 → FR-022, FR-041 → FR-044a | `MaTranQuyenKhopNguonTest`, `MaTranQuyenIT` (SC-001), `QuyenBaLopIT`, `GioiHanTruongIT`, `SuCoNgoaiPhamViIT`, `CanQuyenArchTest`, `PhamViBaoCaoLichSuIT` (#13) | Đơn vị, tích hợp, kiến trúc |
| US2 #1–9, FR-001 → FR-006, FR-009, FR-011, FR-012 | `TaoTaiKhoanIT`, `NghiViecKhoaTaiKhoanIT`, `KhoaMoKhoaIT`, `LuonConQuanLyVienIT`, `KhongXoaTaiKhoanIT` | Tích hợp |
| US3 #1–9, FR-007 → FR-015 | `DangNhapKhoaTamIT`, `KhongHoatDongIT`, `MoKhoaSomIT`, `DangNhapKhongLoTonTaiTest`, `BuocDoiMatKhauIT`, `MotThietBiMotPhienIT`, `KetThucPhienNgayIT` | Đơn vị, tích hợp, đồng hồ giả lập |
| US4 #1–16, FR-028 → FR-035a, FR-019a → f, FR-041a → c | `PhamViTheoCaIT`, `PhamViDoiPhanCongIT`, `PhamViHopPhanCongIT`, `QuyenTheoNhiemVuIT`, `DongBoKetQuaIT`, `NgoaiTuyenChoXemLaiIT`, `DuLieuNguonLoiIT` | Tích hợp |
| US5 #1–8, FR-036 → FR-040 | `DieuKienPhapLyIT`, `PhamViHanhNgheIT`, `CanhBaoGiayPhepIT` | Tích hợp, đồng hồ giả lập |
| US6 #1–9, FR-045 → FR-052 | `NguoiThanSucKhoeIT`, `NguoiThanPhamViIT`, `HetQuanHeKhoaTaiKhoanIT`, `XacMinhNguoiThanIT` | Tích hợp |
| US7 #1–7, FR-024 → FR-027 | `ThuTuThuHepTest`, `ThuHepKhongVuotIT`, `DoiQuyenDongThoiIT`, `QuyenTaiThoiDiemIT`, `PhanQuyenChiQuanLyIT` | Đơn vị, tích hợp |
| US8 #1–9, FR-067 → FR-075 *(bổ sung 2026-10-18)* | `TaoTaiKhoanNguoiCaoTuoiIT`, `MaQrDangNhapIT`, `ThietBiGanIT`, `PhienNguoiCaoTuoiKhongHetIT`, `KhoaTaiKhoanNguoiCaoTuoiIT`, `XacDinhThietBiSosIT`, `NguoiThanKhongXemTaiKhoanNctIT` | Tích hợp, đồng hồ giả lập |
| Edge Cases | Mỗi gạch đầu dòng một phương thức trong các lớp trên | |
| Client web | `DangNhap.test.jsx` (thông báo chung), `DoiMatKhau.test.jsx`, `PhanQuyen.test.jsx` (412, chặn vượt ma trận), `AnNutTheoQuyen.test.jsx` | Vitest |
| App nhân viên | `dang_nhap_thiet_bi_chung_test.dart`, `mo_khoa_som_test.dart`, `ket_qua_dong_bo_test.dart` (BR-M15-20), `cho_xem_lai_test.dart` | flutter_test |
| App người thân | `dang_nhap_nguoi_than_test.dart`, `an_suc_khoe_test.dart` | flutter_test |
| App người cao tuổi *(bổ sung 2026-10-18)* | `dang_nhap_qr_test.dart` (lưu token phiên, token thiết bị; máy mới thay máy cũ), `mo_khoa_sinh_trac_test.dart` (không có PIN; nút SOS ngoài khóa) | flutter_test |

## 5. Tiêu chí hoàn tất (đối chiếu SC)

- SC-001, SC-002, SC-003, SC-004, SC-006, SC-007, SC-009, SC-010, SC-011, SC-012, SC-013: test tự động ở mục 4 trên MySQL thật.
- SC-005 (tạo tài khoản trong 3 phút) và SC-008 (90% người dùng hiểu lý do từ chối): đo với người dùng thật trên profile `demo`, theo cách đo ghi trong spec.
