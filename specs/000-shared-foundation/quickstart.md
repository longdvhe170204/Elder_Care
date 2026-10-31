# Quickstart: Kiểm chứng feature 000

**Ngày**: 2026-10-06 | **Plan**: [plan.md](plan.md)

Hướng dẫn này dùng để chạy và kiểm chứng ở giai đoạn triển khai. Ở giai đoạn thiết kế hiện tại chưa có mã, nên các lệnh dưới đây chưa chạy được. Hợp đồng chi tiết xem [contracts/api-000.yaml](contracts/api-000.yaml) và [contracts/module-interfaces.md](contracts/module-interfaces.md). Bảng và ràng buộc xem [data-model.md](data-model.md).

## 1. Điều kiện cần

- JDK 25, MySQL 8.0 cài trên máy (test tích hợp theo research R-14), Node.js LTS (web), Flutter stable.
- Profile `demo` cho chạy tay: đồng hồ giả lập bật, có endpoint `/api/v1/kiem-thu/dong-ho` (research R-04).
- Dữ liệu mẫu của profile `demo`:
  - các tài khoản QLV (AC-01), Trưởng tầng tầng 2, Điều dưỡng tầng 2, hai Hành chính;
  - một loại yêu cầu mẫu `M15.MAU`, có handler kiểm thử với điều kiện áp dụng bật hoặc tắt được;
  - một loại bản ghi nhóm 3 mẫu `M15.GHI_NHAN_MAU` gắn tầng, một loại không gắn tầng;
  - một danh mục mẫu.

  Spec US1 cho phép dùng dữ liệu mẫu khi module thật chưa có (CHK029).

## 2. Chạy

```text
backend/  ./mvnw verify                  # test đơn vị, tích hợp (MySQL trên máy), kiến trúc, so hợp đồng
backend/  ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
frontend/ npm ci && npm test && npm run dev
```

*(2026-11-01, T103)* Loại yêu cầu, bản ghi mẫu ở mục 1 chỉ có trong mã test: chạy mục 3 bằng `QuickstartDemoApplication` (`spring-boot:test-run`) và script `backend/tools/quickstart-000/`, cách chạy ở README của thư mục đó.

Kết quả mong đợi: `verify` xanh, gồm các nhóm test ở mục 4. Khởi động với profile `demo` thì bảng `tham_so` có đủ 119 mã của Phụ lục 25, mỗi mã có một dòng `lich_su_tham_so` ban đầu.

## 3. Kịch bản kiểm chứng đầu–cuối (chạy tay ở profile demo)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| K1 | QLV `GET /tham-so/CFG-M15-05`, rồi `POST …/doi-gia-tri` (24 giờ, có lý do, `If-Match` đúng) | 200; lịch sử có 2 dòng; nhật ký `THAM_SO_DOI` có trước 48, sau 24 | US4 #1–3 |
| K2 | Lặp lại K1 với `If-Match` cũ | 412, body có `giaTriHienHanh` = 24 | US4 #9, FR-021 |
| K3 | Đổi CFG-M15-06 thành 20 giờ (nhỏ hơn CFG-M15-05) | 422 `GIA_TRI_KHONG_HOP_LE` | FR-015 |
| K4 | Điều dưỡng gọi `doi-gia-tri` | 403 | US4 #6, BR-M15-04 |
| K5 | `GET /tham-so/CFG-M15-05/lich-su?taiThoiDiem=<trước K1>` | Trả 48 giờ | US4 #8, FR-019 |
| K6 | Hành chính A lập yêu cầu `M15.MAU`, ngày hiệu lực là ngày mai, rồi gửi duyệt. Tua đồng hồ 25 giờ, chạy job nhắc | Sự kiện `YeuCauCanNhacNguoiDuyet` (CFG-M15-05 đã đổi thành 24 giờ ở K1). Yêu cầu vẫn `CHO_DUYET` | US5 #4 |
| K7 | Tua thêm tới quá 96 giờ, chạy job nhắc | `YeuCauCanBaoQuanLyVien`; không tự hủy | US5 #4, FR-031a |
| K8 | QLV và một QLV thứ hai cùng `duyet` với cùng `If-Match` | Một bên 200, một bên 409 `DA_DUOC_XU_LY` | US5 #12, FR-040 |
| K9 | Tua tới ngày hiệu lực, chạy job áp dụng 3 lần | `DA_AP_DUNG`; đúng 1 dòng `ket_qua_ap_dung` | US5 #7, #11, SC-003 *(R-15: `ket_qua_ap_dung` → `yeu_cau_phe_duyet` (cột))* |
| K10 | Yêu cầu thứ hai đã duyệt; tắt Bộ lập lịch 2 ngày qua ngày hiệu lực, rồi chạy | `apDungBu = true`; nhật ký có ngày hiệu lực và thời điểm áp dụng | US5 #9, FR-037a |
| K11 | Yêu cầu thứ ba, bật "điều kiện không thỏa" ở handler mẫu, đến ngày | `AP_DUNG_KHONG_THANH`; có sự kiện báo người duyệt và người yêu cầu | US5 #8, FR-038 |
| K12 | Yêu cầu thứ tư, bật "lỗi khi áp dụng" | Vẫn `DA_DUYET`; nhật ký `YC_AP_DUNG_LOI`; không có tác động. Tắt lỗi, chạy lại thì `DA_AP_DUNG` | US5 #10, FR-037b |
| K13 | QLV lập và tự duyệt không có ý kiến; sau đó có ý kiến | Lần đầu 409/422; lần sau 200; nhật ký có `tuDuyet` | US5 #13, FR-035 |
| K14 | Điều dưỡng tầng 2 ghi bản ghi mẫu gắn tầng, rồi đính chính `SUA_NOI_DUNG` | 201, `cachTao = A`; bản gốc không đổi; `GET /dinh-chinh/...` có `daDinhChinh` | US2 #1, #5 |
| K15 | Thử `UPDATE` và `DELETE` trực tiếp bảng mẫu nhóm 3 bằng tài khoản `eldercare_app` | MySQL báo lỗi (thiếu quyền hoặc trigger `45000`) | US1 #7, SC-002 |
| K16 | Hành chính B đính chính bản ghi không gắn tầng của Hành chính A | 202, yêu cầu `M15.DINH_CHINH` ở `CHO_DUYET`. QLV duyệt thì có `dinh_chinh` với `cachTao = C`. Trưởng tầng thử đính chính thì 403 | US2 #4 |
| K17 | `HUY_GHI_NHAN`, rồi `SUA_NOI_DUNG` trên cùng bản gốc | Lần sau trả 409 `BAN_GHI_DA_HUY_GHI_NHAN` | FR-029 |
| K18 | Hành chính xem nhật ký của hồ sơ mẫu có đổi `diaChi` và `diUng` | `diaChi` hiện giá trị; `diUng` hiện `{"daThayDoi": true}`; `lyDo` là `null` và `lyDoBiAn = true` | US3 #8, BR-M15-11 |
| K19 | QLV tra `/nhat-ky?hanhDong=YC_DUYET&tuDuyet=true` | Thấy lần tự duyệt ở K13 | US3 #10 |
| K20 | Danh mục mẫu: xóa mục chưa dùng; xóa mục đã dùng; ngừng hiệu lực rồi kích hoạt lại khi đã có mục trùng tên | 204; 409 `DANH_MUC_DA_DUOC_THAM_CHIEU`; 409 khi trùng tên | US1 #1, #2, #8 |
| K21 | *(Bổ sung theo plan 004, Q-338)* Bật "giới hạn vai trò duyệt" = Quản lý viện ở handler mẫu. Hành chính có quyền duyệt `M15.MAU` lập và gửi một yêu cầu; xem chi tiết; thử Duyệt, Từ chối; tua quá CFG-M15-05 | `lenhDuocPhep` không có `duyet`, `tu-choi`; Duyệt, Từ chối 403; `YeuCauDaGuiDuyet`, `YeuCauCanNhacNguoiDuyet` chỉ có Quản lý viện; quá CFG-M15-06 không phát `YeuCauCanBaoQuanLyVien` vì người duyệt đều là Quản lý viện; tắt giới hạn thì Hành chính duyệt được như cũ | FR-034, FR-031a, BR-M15-09 |

## 4. Test tự động: truy vết kịch bản → test

Mỗi kịch bản Given/When/Then của spec có ít nhất một test (constitution XV). Tên lớp test theo data-model mục 7.

| Spec | Lớp test (backend, `com.eldercare.backend.administration…`) | Loại |
| --- | --- | --- |
| US1 #1–8, FR-001 → FR-010 | `DanhMucMauIT`, `NhomDuLieuArchTest`, `NhomGhiNhanCoTriggerIT`, `PhienBanKhongChongKhoangIT`, `LenhNghiepVuLyDoTest` | Tích hợp, kiến trúc |
| US2 #1–8, FR-023 → FR-030 | `DinhChinhChonCachTest` (bảng quyết định A/B/C, CHK033), `DinhChinhIT`, `DinhChinhDongThoiIT`, `DinhChinhHienHanhViewIT` | Đơn vị, tích hợp |
| US3 #1–12, FR-042 → FR-051a | `NhatKyDayDuIT` (SC-001), `NhatKyNguyenTuIT` (#6), `NhatKyCheTruongIT`, `NhatKyPhamViIT`, `NhatKyNgoaiTuyenIT` (#12), `NhatKyXuatBaoCaoIT` (#11), `LoaiBoDuLieuIT` (#4, #5, gián đoạn) | Tích hợp, đồng hồ giả lập |
| US4 #1–10, FR-011 → FR-022 | `ThamSoIT`, `ThamSoQuyenIT`, `KiemTraThamSoTest`, `ThamSoBoDemSauCommitIT` (SC-004), `ThamSoTaiThoiDiemIT` (SC-005), `ThamSoBangIT` | Đơn vị, tích hợp |
| US5 #1–14, FR-031 → FR-041 | `BangChuyenTrangThaiTest`, `YeuCauVongDoiIT`, `ApDungMotLanIT` (SC-003), `ApDungBuIT` (BR-M15-10), `ApDungLoiIT`, `NhacYeuCauIT` (BR-M15-09), `QuyetDinhDongThoiIT`, `TuDuyetIT`, `GioiHanVaiTroDuyetIT` (bổ sung theo plan 004) | Đơn vị, tích hợp |
| Edge Cases | Gộp vào các lớp trên, mỗi gạch đầu dòng một phương thức test | |
| Kiến trúc | `ModuleBoundaryTest` (`ApplicationModules.verify()`), `ThoiGianArchTest` (cấm `now()`), `HopDongApiTest` (so springdoc với `contracts/api-000.yaml`) | Kiến trúc |
| Client web | `ThamSoForm.test.jsx` (412 hiện giá trị mới), `NhatKyBang.test.jsx` ("đã thay đổi"), `HopDuyet.test.jsx` | Vitest |
| App nhân viên | `hop_duyet_test.dart` (Trưởng tầng duyệt yêu cầu 015) | flutter_test |

## 5. Tiêu chí hoàn tất (đối chiếu SC)

- SC-001, SC-002, SC-003, SC-005, SC-008, SC-009, SC-010, SC-011 được chứng minh bằng test tự động ở mục 4, chạy trên MySQL thật (R-14).
- SC-004: `ThamSoBoDemSauCommitIT` đổi tham số rồi kiểm tra quy tắc ngay sau commit, không khởi động lại.
- SC-006 và SC-007 đo bằng người dùng (10 lần, lấy trung vị) trên profile `demo`. SC-006 cần dữ liệu sinh ở quy mô NFR-01 với 1 năm nhật ký (khoảng 22 triệu dòng, research R-06), tạo bằng công cụ sinh dữ liệu của task.
