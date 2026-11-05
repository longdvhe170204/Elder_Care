# Implementation Plan: Chi phí phát sinh

> **Sửa 2026-10-29 (Q-395):** bỏ bảng hệ số phí khi vắng; nội trú vắng tính như ngày lưu trú, từ ngày Giải phóng giường không tính phí lưu trú; bỏ dấu "chờ quyết định", "không có dòng chính sách". Spec FR-005a, FR-007, FR-009 là căn cứ; chỗ nào dưới đây còn nhắc hệ số vắng theo 004 thì theo spec.

**Branch**: `010-incurred-costs` | **Date**: 2026-10-10, cập nhật 2026-10-18 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/010-incurred-costs/spec.md`

## Summary

Feature 010 là phần chi phí phát sinh của module M11 (mục 15.1 → 15.8; UC-61 → UC-64, UC-79). Nó cung cấp:
1. Kỳ chi phí tháng dương lịch, bảng chi phí thường theo người theo kỳ, bảng bổ sung, kỳ cuối (mục A, G).
2. Hành chính lập khoản Nháp cho từng sự kiện tính phí (từng khoản hoặc hàng loạt) sau khi tra cứu bản ghi nguồn; mỗi khoản trỏ đúng một nguồn (mục B, Q-306, Q-307).
3. Đơn giá và số tiền theo hợp đồng hoặc phiên bản đơn giá tại ngày phát sinh, gói, nội trú tính đủ phí khi vắng, tỷ lệ bán trú theo nguồn, chia giá tháng, làm tròn (mục C, Q-395).
4. Vòng đời khoản Nháp → Đã kiểm tra → Đã duyệt → Đã chốt / Đã hủy; tính lại khi nguồn đổi trước chốt; báo Hành chính sau chốt (mục D).
5. Khoản nhập tay, đề nghị mua hộ với đồng ý của người đại diện hoặc duyệt của Quản lý viện (mục E).
6. Khoản điều chỉnh do Hành chính lập (mục F).
7. Gửi chốt, chốt bảng, cảnh báo biến động, điều kiện chi phí kỳ cuối cho Kết thúc lưu trú, qua đời (mục G).
8. Bảng chi phí và tạm tính cho người thân, dữ liệu cho 017, 016, xuất kế toán (mục H).

Hướng kỹ thuật:
- Gói `billing` với tám gói con; 017 và phần mua sắm của 019 (cùng M11) dùng port nội bộ (research P-01, P-12).
- Ngày lưu trú và buổi bán trú tính từ 004 lúc tra cứu và lúc lập, không lưu; nguồn của 005, 006, 011, 012, 014 vào một **chỉ mục kỹ thuật** dựng từ sự kiện, chỉ để tra cứu và tìm khoản khi nguồn đổi; không có danh sách "chưa lập khoản" (P-04).
- Đơn giá, chia giá tháng, buổi bán trú, thời gian tính phí, biến động, điều kiện kỳ cuối, tạm tính là hàm thuần kiểm thử bằng bảng (P-05, P-07, P-08, P-10, P-11).
- DBR-15 sau R-15 bằng unique_key do caller đặt, CHECK đối chiếu theo (nguồn, đơn vị) và UNIQUE cho khoản đang hiệu lực; DBR-17 bằng locked_at/trigger của bảng và khoản (data-model mục 0.1).
- Kỳ cuối: `KyCuoiPort` chạy trong lệnh hồ sơ kết thúc của 004; điều kiện `KT_A_CHI_PHI` qua bean `ThamGiaChuyenTrangThai` (P-10).
- Bảng Đã chốt phát `BangChiPhiDaChot` đúng một lần cho 017 (DBR-30 ở 017).

Plan tuân theo FR-010a của 000 (Q-306, Q-307):
- 010 không lập khoản, bảng (trừ tác động kèm lệnh lập khoản và lệnh hồ sơ kết thúc), khoản điều chỉnh; không gợi ý đơn giá, không tính sẵn số chênh lệch, không có dấu "không tính";
- hệ thống chỉ: tính đơn giá và số tiền khi Hành chính lập; tính lại, đưa về Nháp, hủy khoản chưa chốt khi nguồn đổi; chuyển trạng thái kỳ theo lịch; kiểm tra và chặn; nhắc, thông báo.

### Cập nhật 2026-10-18 (spec mục "Cập nhật 2026-10-18": feature 009 FR-001a)

- **Đối tượng thông tin** (FR-044, 009 FR-001a): mọi thông báo tới người thân (bảng Đã chốt, đề nghị mua hộ cần đồng ý, bị hủy sau khi đồng ý, vượt số đã đồng ý, quá hạn chờ đồng ý) khai `doiTuongThongTin = VE_VIEC_NGUOI_THAN`; không yêu cầu nào mang mã Q-354 (research P-13).

### Cập nhật 2026-10-22 (spec mục "Cập nhật 2026-10-22": Q-378, Q-380, Q-385)

- **Gửi 017 chi tiết khoản Đã chốt** (FR-042, bảng giao tiếp "Gửi 017"; 017 FR-031, FR-032, FR-034): port nội bộ `ChiPhiChoSoDuPort` thêm `bangDaChot(bangId)` trả bảng (loại, kỳ, tổng, tổng theo loại từ `bang_chi_phi_tong_loai`) và khoản Đã chốt (ngày phát sinh, loại, mô tả, số lượng, đơn vị, đơn giá, thành tiền; khoản điều chỉnh kèm kỳ gốc; khoản thuốc chỉ "Thuốc" và mã vật phẩm theo FR-019). Chỉ đọc, dữ liệu đã có; không thêm bảng, không đổi REST (research P-12). *(R-15: `bang_chi_phi_tong_loai` → cột `JSON` của `bang_chi_phi`)*
- **Q-380 (sửa theo Q-385)**: phí chấm dứt sớm là khoản nhập tay có lý do của FR-021 vào bảng kỳ cuối, Quản lý viện duyệt từng khoản; không thêm loại khoản, tham số hay cách tính.
- **Q-385**: 017 không còn đối soát chuyển khoản; không ảnh hưởng 010.

## Technical Context

**Language/Version**: Java 25; JavaScript (JSX); Dart 3.x với Flutter stable 3.x (000 R-01).

**Primary Dependencies**: Như 000 → 009. Thêm thư viện ghi XLSX dạng luồng (fastexcel) cho xuất kế toán (Q-05); CSV không cần thư viện.

**Storage**: MySQL 8.0. Bảng ở [data-model.md](data-model.md).

**Testing**:
- Như 000 R-14.
- Test bảng cho tám hàm thuần: `LamTronDong`, `PhiNgayLuuTru`, `PhiBuoiBanTru`, `ThoiGianTinhPhi`, `BienDongKy`, `DieuKienChiPhiKyCuoi`, `KiemTraVuot`, `TongTamTinh`; SC-002 với 12 tháng.
- Test tích hợp MySQL cho `UNIQUE` (DBR-15, bảng mỗi kỳ) và trigger đã chốt (DBR-17).
- Test đồng thời: hai lệnh lập cùng đơn vị; chốt trong lúc lập khoản; hai người đại diện quyết định.
- Đồng hồ giả lập cho CFG-M11-01, 02, 03, 06, CFG-M15-05, 06, CFG-M02-09, chuyển kỳ.
- Bản giả nguồn của 011, 012, 014, `NguoiDaiDienPort`, `SoDuMuaHoPort`.

**Target Platform**: Như 000.
- Web: tra cứu nguồn và lập khoản, bảng chi phí (kiểm tra, duyệt, chốt), nhập tay, điều chỉnh, biến động sau chốt, đề nghị mua hộ, xuất kế toán; cổng người thân trên trình duyệt (bảng, tạm tính, đồng ý mua hộ).
- App nhân viên: không có màn của 010.
- App người thân: bảng chi phí, tạm tính, đồng ý/từ chối đề nghị mua hộ.

**Project Type**: Web service (REST `/api/v1`), một web client và một mobile client chung (Q-346).

**Performance Goals**:
- Lập hàng loạt 30 ngày hoặc 300 nguồn ≤ 30 giây (SC-006): một transaction, đọc ngày lưu trú theo khoảng một lần, đơn giá đệm theo (dịch vụ, ngày) trong lệnh.
- Tính lại khi nguồn đổi ≤ 5 phút (SC-005): listener ngay sau commit nguồn; tìm khoản qua chỉ mục `(loai_nguon, id_nguon)`.
- Bảng 60 → vài trăm khoản mở ≤ 2 giây; kiểm tra, duyệt hàng loạt theo bảng một transaction.
- Quy mô: 300 người, khoảng 10.000 → 20.000 khoản mỗi tháng (phí lưu trú, thuốc, vật phẩm).

**Constraints**:
- Không có thao tác sửa trạng thái; mỗi lệnh một endpoint (constitution XI); khoản theo sự kiện không nhận số tiền từ người dùng.
- Bảng, khoản Đã chốt, lịch sử tính lại, biến động, quyết định mua hộ, lần xuất không bị `UPDATE`, `DELETE` (constitution XII, DBR-17).
- Nhật ký trong transaction (000 FR-047). CFG đọc lúc chạy, giá trị dùng cho đề nghị lưu lúc gửi (FR-023).

**Scale/Scope**:
- 5 thực thể có vòng đời (kỳ, bảng, khoản, đề nghị mua hộ, trạng thái điều kiện kỳ cuối tính được); 16 bảng (gồm bảng kỹ thuật).
- 2 job (hằng ngày, hằng giờ).
- 28 đường dẫn, 29 thao tác REST ([contracts/api-010.yaml](contracts/api-010.yaml)).
- 4 SPI 010 cài cho module thấp hơn; 2 sự kiện khai báo cho 011, 012, 014; 2 port nội bộ cho 017; 3 giao diện công khai.

Không còn mục NEEDS CLARIFICATION. Các lựa chọn kỹ thuật đã chốt ở [research.md](research.md). Các điểm về tài liệu nguồn ghi ở "Điểm cần báo lại"; chúng không chặn thiết kế.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Bằng chứng |
| --- | --- | --- |
| I. Nguồn gốc, truy vết | Đạt | Mỗi quyết định dẫn FR, BR, DBR. Data-model mục 11 là bảng truy vết DBR-15, 16, 17 và BR-M11-01 → 09 |
| II. Spec không nêu công nghệ | Đạt | Spec không đổi; công nghệ chỉ ở plan, research |
| III. Không mặc định CRUD | Đạt | Kỳ, bảng, khoản, đề nghị mua hộ chỉ có lệnh theo bảng trạng thái; khoản Đã chốt chỉ nhận điều chỉnh |
| IV. Trạng thái kiểm tra được | Đạt | Bảng chuyển ở data-model mục 3, 4, 6 khớp bốn bảng trạng thái của spec. Mỗi kịch bản có test (quickstart mục 4) |
| V. Không cố định giá trị | Đạt | CFG-M11-01, 02, 03, 06, CFG-M02-05 (qua 004), CFG-M02-09, CFG-M15-05, 06 đọc từ `ThamSoApi` |
| VI. Vai trò, quyền | Đạt | Tách quyền Hành chính / Quản lý viện / Kế toán theo FR-017, FR-043 (P-14); người đại diện qua 012 |
| VII. Điểm chưa rõ | Đạt | Không tự đoán nghiệp vụ; điểm còn lệch ghi ở mục báo lại |
| VIII. Tiêu chí thành công | Đạt | SC-001 → SC-012 có cách đo ở quickstart mục 5 |
| IX. Phạm vi giai đoạn | Đạt | Triển khai backend/web đã mở; mobile người thân mở 2026-10-27. Phiên chuẩn bị 2026-10-29 chỉ sửa tài liệu/bản dịch, chưa mã hoặc SQL; không đổi nghiệp vụ hay hợp đồng của 004 |
| X. Kiến trúc | Đạt | Một khối; gói `billing`; module khác dùng `billing.api`. Không có vòng (P-02). REST; MySQL; hai client (Q-346). NFR ở bảng bên dưới |
| XI. Nghiệp vụ ở backend | Đạt | Đơn giá, điều kiện lập, chốt, vượt mua hộ, điều kiện kỳ cuối ở backend. Mỗi lệnh có endpoint riêng |
| XII. Dữ liệu | Đạt | DBR-15, 17 bằng `UNIQUE` và trigger; DBR-16 bằng căn cứ đơn giá lưu theo khoản; nhật ký trong transaction; `Clock` |
| XIII. Tham số | Đạt | Như V; CFG-M11-02 lúc gửi được lưu trên đề nghị (FR-023) |
| XIV. Ngoại tuyến, thông báo | Đạt | 010 không có ghi ngoại tuyến. Mọi thông báo là sự kiện tới 009 với mức của FR-044 |
| XV. Truy vết, kiểm thử | Đạt | Tên test theo BR, FR. Mỗi BR của 010 có test (data-model mục 11) |

**Kiểm lại sau Phase 1**: Đạt. Thiết kế không thêm vai trò, trạng thái hay quy tắc nghiệp vụ ngoài spec. Có bốn bổ sung kỹ thuật, giải thích ở Complexity Tracking:
1. Chỉ mục kỹ thuật sự kiện tính phí.
2. Port nội bộ M11 cho 017.
3. Bảng `bien_dong_nguon_sau_chot`. *(R-15: `bien_dong_nguon_sau_chot` → `su_kien_chung`)*
4. Thư viện ghi XLSX.

### Đáp ứng yêu cầu phi chức năng (constitution X)

| NFR | 010 chịu ảnh hưởng | Cách đáp ứng |
| --- | --- | --- |
| NFR-01 Quy mô | Có | 300 người, hàng chục nghìn khoản mỗi tháng; chỉ mục theo bảng, nguồn, người |
| NFR-02 Hiệu năng | Có | Lập hàng loạt một transaction; tính lại theo chỉ mục |
| NFR-03 Thông báo khẩn cấp | Không | 010 không có thông báo Khẩn cấp |
| NFR-04 Tác vụ định kỳ | Có | Job hằng ngày, hằng giờ; khóa nhắc theo ngày; chống trùng sự kiện nguồn |
| NFR-05 Khả dụng | Gián tiếp | Listener nguồn chạy lại khi outbox giao lại |
| NFR-06 Sao lưu | Gián tiếp | Dữ liệu ở MySQL |
| NFR-07 Lưu trữ | Có | Theo hồ sơ (CFG-M15-03); kỳ, lần xuất theo CFG-M15-04 |
| NFR-08 Bảo mật | Có | Tên thuốc chỉ với người được xem thuốc (FR-019); người thân chỉ khi có quyền chi phí |
| NFR-09 Thời gian | Có | `Clock`; ngày phát sinh, kỳ theo Asia/Ho_Chi_Minh; sự kiện sau thời điểm qua đời theo giờ |
| NFR-10 Thiết bị | Có | Web đủ chức năng; cổng người thân hai kênh |
| NFR-11 Ngôn ngữ | Có | Tiền VND số nguyên, làm tròn nửa lên; ngày dd/MM/yyyy |
| NFR-12 Cấu hình | Có | CFG-M11 đổi qua tham số |
| NFR-13 Kiểm thử | Có | Đồng hồ giả lập; bản giả nguồn |

## Project Structure

### Documentation (this feature)

```text
specs/010-incurred-costs/
├── plan.md                      # File này
├── research.md                  # Phase 0: P-01 → P-15
├── data-model.md                # Phase 1: bảng, bảng chuyển, bảo đảm ở cơ sở dữ liệu, truy vết
├── quickstart.md                # Phase 1: kịch bản kiểm chứng, truy vết kịch bản → test
├── contracts/
│   ├── api-010.yaml             # OpenAPI 3.1: tra cứu nguồn, khoản, bảng, mua hộ, cổng, xuất kế toán
│   └── module-interfaces.md     # SPI 010 cài, API cung cấp, sự kiện, port nội bộ M11
└── tasks.md                     # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

Sơ đồ dưới dùng tên logic của thiết kế. Khi cài đặt dùng tên tiếng Anh và tầng theo backend/README.md; chỉ tạo gói có lớp, không tạo trước cây thư mục rỗng. Tên dự kiến của 010 ở glossary mục 16; phiên chuẩn bị chưa tạo mã.

```text
backend/src/main/java/com/eldercare/backend/billing/
├── api/           # ChiPhiCongNguoiThanApi, ChiPhiKyCuoiApi, ChiPhiThongKeApi, SuKienTinhPhiNguon*, sự kiện
├── ky/            {internal, web}   # kỳ, bảng, gửi chốt, chốt, BienDongKy
├── khoan/         {internal, web}   # vòng đời, kiểm tra, duyệt, tính lại, lịch sử
├── nguon/         {internal, web}   # chỉ mục, tra cứu, lập theo sự kiện, listener nguồn đổi
├── tinhgia/       {internal}        # LamTronDong, PhiNgayLuuTru, PhiBuoiBanTru, ThoiGianTinhPhi
├── dieuchinh/     {internal, web}   # khoản điều chỉnh, biến động sau chốt
├── muaho/         {internal, web, port}   # đề nghị, quyết định, ghi đã mua, DeNghiMuaHoPort, SoDuMuaHoPort
├── kycuoi/        {internal}        # KyCuoiPort, DieuKienChiPhiKyCuoi, bean trạng thái, MucViecSauQuaDoiPort
├── xuat/          {internal, web}   # xuất kế toán
├── cungcap/       {internal, web, port}   # cổng, tạm tính, thống kê, ChiPhiChoSoDuPort
└── lich/                            # ViecHangNgayM11, ViecHangGioM11

frontend/src/tinh-nang/chi-phi/
├── tra-cuu-nguon/           # tra cứu, lập từng khoản / hàng loạt
├── bang-chi-phi/            # kiểm tra, duyệt, trả lại, gửi chốt, chốt
├── dieu-chinh/              # biến động sau chốt, lập điều chỉnh
├── mua-ho/
├── xuat-ke-toan/
└── cong-nguoi-than/chi-phi/

mobile/lib/nguoi-than/chi-phi/   # bảng, tạm tính, đồng ý mua hộ
```

**Structure Decision**: Theo R-02 của 000. Feature 010 nằm ở `backend/…/billing` với chín gói con và gói `api`; 017 và phần mua sắm của 019 thêm gói con vào cùng `billing`. Web có đủ chức năng. App người thân có bảng, tạm tính và đồng ý mua hộ.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Chỉ mục kỹ thuật `su_kien_tinh_phi` | Tra cứu nguồn theo người, loại, khoảng ngày (FR-005) và tìm khoản khi nguồn đổi (FR-020) trên năm module | Gọi API đọc của từng module mỗi lần tra cứu: 005, 006 không có API tìm theo tính phí; vẫn cần ánh xạ nguồn → khoản |
| Port nội bộ M11 (`ChiPhiChoSoDuPort`, `SoDuMuaHoPort`) | 017 cùng module cần tổng chưa chốt, phí ngày cơ sở; 010 cần dấu số dư không đủ | Đặt ở `billing.api`: mở dữ liệu nội bộ cho mọi module |
| `bien_dong_nguon_sau_chot` | FR-027 đòi báo kèm khoản gốc, sự kiện, giá trị trước/sau và khoản điều chỉnh trỏ về sự kiện gây ra; không được tính sẵn số chênh lệch | Chỉ gửi thông báo: mất liên kết khoản điều chỉnh → sự kiện *(R-15: `bien_dong_nguon_sau_chot` → `su_kien_chung`)* |
| Thư viện ghi XLSX | Q-05 chọn Excel/CSV; kế toán quen Excel | Chỉ CSV: lỗi mã hóa khi mở bằng Excel ở một số máy; vẫn giữ CSV làm tùy chọn |

## Đồng bộ với hợp đồng của feature khác

Plan này bổ sung hoặc dựa vào các điểm dưới đây; plan của feature liên quan giữ đúng hoặc ghi rõ thay đổi.

1. **004** (đã ghi bổ sung vào [../004-admission-residency/contracts/module-interfaces.md](../004-admission-residency/contracts/module-interfaces.md) mục 1.8): sự kiện `PhienBanDonGiaDaTao(dichVuId, hieuLucTu, hieuLucDen)` khi tạo phiên bản đơn giá (kể cả hiệu lực lùi cho khoảng chưa có phiên bản, DBR-08), để 010 tính lại khoản thiếu đơn giá (FR-012). 010 cài `KyCuoiPort`, `MucViecSauQuaDoiPort` (`CHI_PHI_KY_CUOI`), dùng `KT_A_CHI_PHI` và các API, sự kiện của 004 như đã khai báo.
2. **005, 006**: dùng các sự kiện tính phí đã khai báo (005 C-13, 006 D-17); 010 cài `DeNghiMuaHoPort` đúng chữ ký. Không đổi.
3. **011, 012, 014**: phát `billing.api.SuKienTinhPhiNguon`, `SuKienTinhPhiNguonDaDoi` cho suất ăn người thân, đêm ở lại, điểm danh có phí ([contracts/module-interfaces.md](contracts/module-interfaces.md) mục 1.5). **012** cài `NguoiDaiDienPort` (đã khai báo ở 004) và đọc `ChiPhiCongNguoiThanApi`.
4. **017** (cùng M11): cài `SoDuMuaHoPort`; dùng `ChiPhiChoSoDuPort` (*2026-10-22:* thêm `bangDaChot` cho phiếu báo phí, bảng quyết toán); nghe `BangChiPhiDaChot` (đúng một lần mỗi bảng) và `DeNghiMuaHo*`. **016** đọc `ChiPhiThongKeApi`. **009**: thông báo theo FR-044 cài `CoYeuCauThongBao`.

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX). Đề nghị chủ dự án quyết bằng mã Q mới khi cần, rồi sửa `docs/business/nghiep-vu.md` và spec liên quan.

1. **Tính lại khoản thiếu đơn giá "kèm lệnh tạo phiên bản đơn giá"** (FR-012): lệnh tạo phiên bản thuộc 004; plan làm việc tính lại ngay sau commit của lệnh đó qua sự kiện `PhienBanDonGiaDaTao` (vài giây), không trong cùng transaction. Kết quả nghiệp vụ không đổi (khoản chưa chốt được tính lại, gỡ dấu); ghi lại để chủ dự án biết "kèm lệnh" được hiểu là "do lệnh đó kích hoạt". *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

2. **Kỳ của viện "Tới ngày 1 của tháng → Đang mở, Hệ thống (theo lịch)"**: Q-306 cấm hệ thống tự lập bản ghi nghiệp vụ, nhưng bảng trạng thái kỳ của spec cho hệ thống mở kỳ theo lịch. Plan coi kỳ của viện là mốc thời gian (không gắn người, không có tác động), mở bằng job; bảng và khoản vẫn chỉ lập kèm lệnh của Hành chính. Đề nghị xác nhận cách hiểu này hoặc ghi rõ ở 15.7. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

3. **Câu chữ còn lẫn trong spec 010** (không đổi nghiệp vụ): *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - User Story 5 đánh số kịch bản 1 → 6, 8, 9, 7 (kịch bản 8, 9 đứng trước 7).
   - Điểm báo lại 13 còn nêu BR-M11-05 "hệ thống tạo khoản Điều chỉnh chờ duyệt"; sau Q-307 hệ thống chỉ báo (FR-027). Đề nghị xác nhận BR-M11-05 ở `docs/business/nghiep-vu.md` đã viết theo Q-307.
   - Assumptions còn câu dẫn `docs/phan-tich-yeu-cau.md` mục 4.2 (đã bỏ).

4. **Mức thông báo nhắc đề nghị mua hộ chờ đồng ý** (bảng FR-044 ghi "Theo feature 000 FR-031a"): plan dùng Trung bình như dòng 000 của bảng 009 FR-043b. Đề nghị ghi mức cụ thể vào FR-044. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

## Gộp bảng theo R-15 *(bổ sung 2026-10-29, constitution XII 3.2.0)*

- Ánh xạ bảng ở data-model mục 0; task T038. Trước khi viết migration, viết data-model mục 0.1 theo mẫu của 001, 009 (bảng gộp, loại ở bảng chung kèm nội dung, ràng buộc chuyển về backend, bảo đảm và test), rồi sửa bảng DBR áp dụng.
- Bảo đảm DBR, BR (XII): như feature đã cài, mỗi ràng buộc của bảng cũ có nơi bảo đảm mới (`khoa_duy_nhat`, cột sinh, `CHECK` theo `kind`, trigger khóa theo dòng hoặc domain kèm test tích hợp).
- Task, plan, research nhắc bảng cũ có ghi chú "R-15:" chỉ bảng đích: tạo bảng đích hoặc dùng bảng chung thay cho bảng cũ.

## Chuẩn bị triển khai 2026-10-29 — impl/010-incurred-costs

- Người dùng tự tạo nhánh/worktree từ origin/develop da9636a; trạng thái lúc bắt đầu sạch. Giữ nguyên mọi worktree 008. Người dùng tự làm các bước tạo nhánh, commit và push; phiên này không commit/push/PR/merge.
- Đã đọc luật và nguồn thiết kế, đối chiếu API thật; 37 task còn [ ], M11 chỉ có package-info.java. Bản dịch HTTP và glossary mục 16 đã được A/chủ dự án duyệt qua xác nhận của E ngày 2026-10-29 trước khi bắt đầu mã; giữ 28 đường dẫn/29 thao tác, 17 schema, 101 ref, 16 request body; giữ phương thức, response, cấu trúc, kiểu, required, enum theo ánh xạ, giới hạn và FR/BR. Không sinh client hoặc cài HTTP từ bản chưa duyệt.
- "Chốt" dùng finalize/FINALIZED theo yêu cầu 016. Tiền SQL DECIMAL(15,0) theo audit mới của 000; JSON vẫn integer/int64 như hợp đồng. Data-model bổ sung khóa UPDATE/DELETE của bảng con theo trạng thái cha; chưa migration/test trigger. Không sửa spec.md hoặc nghiệp vụ.
- Kỹ thuật: bỏ cách hiểu đường dẫn BE/BT là tên vật lý; chỉ tạo gói có lớp theo README, hàm thuần đặt billing.internal.pricing.domain, bản giả chung support/fake/<module>/. Web charges, app family/charges; mobile người thân đã mở, 010 không có màn app nhân viên. Dùng DESIGN.md 6.x khi tới task màn; lượt này không sửa màn/theme.
- Không còn coi API 004 "Có sẵn": ContractReadApi chỉ có attendanceOn, chưa nội dung hợp đồng/đơn giá/ngày lưu trú/SPI kỳ cuối/người đại diện và fake. Chờ B khai báo ở 004; không tự mở rộng api của B. Nguồn 005 chờ C khai báo/phát các sự kiện tính phí/có mặt. D đã có MedicationBilledEvent (phát từ DoseAdministrationService), MedicationBillAdjustedEvent (chưa phát), PurchaseAssistancePort và fake; chờ producer điều chỉnh/nguồn PRN/giao thuốc. Người đại diện/số dư bản thật của 012/017 là phần E làm sau, không đẩy sang người khác.
- E cần khai báo ba API/sự kiện của mình và fake tại T003/T004 sau duyệt; A (016) đang chờ thống kê. Khi API nguồn chưa có interface/fake đúng chủ, dừng phần tích hợp phụ thuộc và báo người dùng; không tạo fake thứ hai hoặc fake production. Gate duyệt đã qua; T001 và phần độc lập của T006 đã thực hiện theo phần cập nhật bên dưới.
- Bản gốc còn 17 response 2xx không có content/schema và vài trường object/dấu chưa chi tiết. Bản dịch giữ nguyên, chưa tự bổ sung cấu trúc để cài theo suy đoán; rà riêng với chủ dự án trước các task HTTP tương ứng. Chưa giải quyết các xác nhận vận hành Q-05 bằng giả định.
- Kiểm tài liệu: parser YAML không trùng khóa; so cấu trúc trước/sau bằng ánh xạ tên; tham số path khớp; 101 ref đều có đích. Chưa chạy Maven/npm/Flutter vì chưa sửa mã/client. Bằng chứng và gate còn chờ ở preparation-review.md; log/script kiểm ignored tại backend/target/010-preparation của checkout gốc. Progress chỉ thêm, file CRLF UTF-8.

## T001 và phần độc lập T006 — sau duyệt 2026-10-29

- Phạm vi: M11ArchTest và ba hàm thuần DongRounding/ResidencyDayFee/DayCareSessionFee trong pricing.internal.domain; không có SQL, job, HTTP, API công khai/fake hoặc mã web/mobile mới. Root M11 đã có @ApplicationModule; chỉ tạo gói có lớp theo README. T001 xong kiểm khung/rule, không coi phase nền hoặc rule chưa có consumer là nghiệm thu tích hợp.
- Lượt trước pull trên da9636a: ba test tính phí và M11ArchTest được viết trước. Lệnh đầu thất bại ở testCompile vì ba lớp domain chưa tồn tại; sau cài, 46/46 test xanh (12 làm tròn, 20 phí ngày, 10 phí buổi, 4 kiến trúc). Không nói 46 test đã chạy đỏ ở lượt chưa biên dịch được.
- Công thức hiện tại theo FR-009/013 và Q-395: BigDecimal HALF_UP tới số nguyên đồng; nội trú dùng đủ giá ngày cơ sở, không nhận hệ số giảm khi vắng; bán trú làm tròn giá cơ sở trước khi nhân tỷ lệ phần trăm từ nguồn. Chỉ ngày cuối tháng/buổi có lịch cuối nhận phần dư. Đơn giá tại ngày, số buổi sau lọc khu nghỉ và buổi cuối do nguồn cung cấp; helper không truy cập DB/API, không tự đếm buổi hoặc lập khoản. Test kiểm 12 tháng 2026 và tháng 2 nhuận 2028, giá thay đổi qua phụ lục, kỳ lưu trú ngắn, hệ số 0/30/100 và giá trực tiếp.
- DayCareSessionFee dùng scheduledSessionCount/lastScheduledSession thay DTO của 004 còn thiếu. Đây là đầu vào toán học tối thiểu cho phần hàm thuần của P-05, không tạo bản giả hoặc khẳng định đã kiểm lọc dữ liệu thật. Thiếu số buổi dương thì không chia giá tháng. Chưa cài BillingPeriod vì còn chờ kiểu hợp đồng/hồ sơ kết thúc của B/004; T006 giữ [ ]. Các quy tắc FR-011/Q-22/Q-26/Q-27/Q-141 không bị đoán lại hoặc coi đã kiểm.
- P-12/tasks làm rõ hai chiều port thay câu gộp cũ: CostForBalancePort do 010 cài, balance/procurement gọi; PurchaseBalancePort do 017 cài, purchase gọi. M11ArchTest kiểm gọi hàm qua tên port trong M11; rule job cấm createItem/createStatement/createAdjustment/createPurchaseAssistanceRequest, không cấm tạo kỳ theo bảng trạng thái đã chốt. Chưa có consumer/port/job thật nên không đánh nghiệm thu luồng.
- Spotless giới hạn Java M11 đã qua. Clean verify toàn kho hoàn tất 09:48 sau nạp .env/backend/.env từ checkout gốc, giữ DB_PASSWORD/DB_RETENTION_PASSWORD: 683 unit/arch xanh (M11 46), 1426 IT còn 7 thất bại/9 lỗi ở đúng 9 lớp M01/M03 đã ghi team-plan, không có lớp đỏ mới hoặc test bỏ qua; Spotless check 2013 file sạch. Không đổi test/source của B/C/D/A để làm xanh baseline. Chi tiết foundation-verification.md.

- Sau khi hợp đồng được duyệt, đã thử npm run gen:api để đồng bộ client. Generator của A lọc TRANSLATED ở frontend/scripts/gen-api.mjs dòng 9, chưa có 010 nên chỉ sinh lại 8 feature cũ và không có diff frontend sau giữ CRLF. Ghi A thêm 010 ở team-plan; foundation-verification.md mục 5 có đúng dòng cần đổi và cách kiểm. Chưa sửa file A/viết tay client, không có thay đổi web nên chưa chạy npm test/lint/build.

## Đồng bộ develop e7b2b71 và Q-394 → Q-397 — 2026-10-29

- Theo yêu cầu người dùng, đã pull --ff-only từ da9636a lên e7b2b71 sau sao lưu/kiểm SHA-256 18 tệp đang làm; giữ công việc 010, progress chỉ thêm, không đổi worktree 008. Chi tiết ghép bảy tài liệu và ảnh hưởng tới 010/012/015/008 ở develop-sync-2026-10-29.md.
- Test Q-395 viết trước, lượt đỏ testCompile do chữ ký cũ còn yêu cầu hệ số; sau sửa, 49 test M11 xanh. ResidencyDayFee.monthly/direct không nhận hệ số; bán trú giữ tỷ lệ nguồn (thêm ca 0/50/100%). Không có thay đổi API công khai hoặc hợp đồng HTTP đã duyệt.
- clean verify nền đã nạp .env/backend/.env và giữ mật khẩu demo; hoàn tất 08:03: 689 unit/arch xanh (M11 49), 1426 IT còn 7 thất bại/9 lỗi, 0 bỏ qua, đúng 9 lớp M01/M03 đã có trong team-plan và lượt trước pull; không có lớp lỗi mới. Spotless check 2009 file sạch. Maven trả BUILD FAILURE do các lỗi nền này, không coi toàn kho xanh.
- T006 vẫn [ ] (3/4): BillingPeriod và kiểm ngày lưu trú cần API/kiểu dữ liệu B/004, gồm ngày Giải phóng giường để chặn từ chính ngày đó; helper không quyết định ngày được tính phí. T003/T004/T005/T007/T008 vẫn là phần E còn làm; các đoạn phụ thuộc nguồn mới chờ B/C/D, client chờ A đăng ký 010. Team-plan giữ phần còn dở, bỏ yêu cầu trigger bản tin đã hủy và đổi tên thống kê 015 theo Q-397. Một số câu chữ cũ trong spec được ghi để báo lại, chưa tự sửa spec.

## T003/T004 — khai báo hợp đồng Java và fake chung 2026-10-29

- Viết CostApiContractsTest trước: lượt đỏ testCompile do API/port chưa có; thêm CostSharedFakesTest/CostViewSchemaTest trước fake/DTO, lượt đỏ tiếp theo cũng testCompile. Sau khai báo và fake: 71 test M11 xanh (22 ca mới + 49 ca trước). Không coi đây là test nghiệp vụ producer hoặc quyền thật.
- Ba API ở billing.api, 11 DTO/ba enum ở api.dto, tám sự kiện ở api.events, mỗi gói có NamedInterface. DTO đọc đối chiếu trường từ YAML đã duyệt/data-model/contract liên module; object chưa rõ giữ Map/List<Map>, không bổ sung HTTP schema. Chữ ký tiếng Anh ghi contracts/module-interfaces mục 4, glossary chỉ mục 16.6. Không sửa source/API của chủ khác.
- Đường dẫn logic port chuyển thành provision/internal/domain/CostForBalancePort và purchase/internal/domain/PurchaseBalancePort theo tầng README; chỉ công khai Java trong nội bộ M11, không đưa vào api. Hai chiều truy cập giữ đúng P-12/M11ArchTest. bangDaChot để T036/T037; hành vi thiếu bean để consumer T027 kiểm.
- Chủ E cung cấp năm fake qua FakeM11Config ở support/fake/m11, chỉ TestConfiguration. Fixture thống kê đặt sẵn tập dòng theo từng ScopeFilter, không đoán phạm vi từ nguồn B/C; fake kỳ cuối/tạm tính bắt test cấu hình kết quả rõ. T002 vẫn dở: fixture nguồn E chưa làm đủ, người đại diện chờ B/004 khai báo; không dùng fake production.
- T003/T004 [x] theo phạm vi khai báo và test xanh, chưa đóng Phase 1/2. Các API/port mới chưa có bản thật; cả tám sự kiện chưa có producer. D/011, C/014, E/012 phát sự kiện nguồn khi cài; E tiếp tục T005/T007/T008, BillingPeriod và các task nghiệp vụ. Dòng team-plan giữ phần chưa merge/bản thật/phát nguồn, có dòng review hợp đồng mới; người dùng tự commit/push/PR.
- Spotless M11 đã qua. Clean verify nền sau nạp .env/backend/.env, giữ mật khẩu demo, đã xong 15:49 min: 711 unit/arch xanh (M11 71), 1426 IT còn 7 thất bại/9 lỗi, 0 bỏ qua, đúng 9 lớp M01/M03 đã ghi nhận ở team-plan/lượt trước, không có lớp đỏ mới. Spotless check qua. Maven trả BUILD FAILURE do lỗi nền; chưa coi toàn backend xanh hoặc Phase 1/2 hoàn tất. Chi tiết api-declaration-verification.md; progress chỉ thêm. Không sửa web/mobile, không chạy kiểm web/Flutter trong lượt này.

## T005a và fixture nguồn — R-15, 2026-10-30

- Đồng bộ develop e7b2b71 → 8573190 bằng fast-forward, không tạo commit/stash/chuyển nhánh; sao lưu SHA-256 59 tệp, ghép bảy tài liệu, giữ toàn bộ Java và API đang làm. Snapshot ở .worktrees/.snapshots/010-before-r15-20261030-1793294325047 của checkout gốc. Progress chỉ ghép thêm dòng hai phía, các worktree 008 không sửa.
- R-15/constitution XII 3.2.0 làm T005 cũ không còn phù hợp: thêm data-model 0.1 trước migration; giữ ba bảng riêng, phần trạng thái/lịch sử/marker qua API chung A. Thêm T005a để kiểm phần độc lập, giữ T005/T038 chưa đóng. T038 đã có sẵn trong tasks; giữ một dòng gốc, cập nhật phần đã kiểm và phần tích hợp còn chờ, chưa đánh [x].
- V010_01__costs.sql chỉ tạo cost_statement (kỳ và bảng phân kind), cost_item, cost_source_index. Unique_key do caller đặt, CHECK đối chiếu và UNIQUE ngăn né khóa; thay cách cột sinh cũ theo R-15, vẫn giữ DBR-15. FK chỉ trong M11, không FK sang nguồn/bảng chung chưa có.
- Hai bảng nghiệp vụ có locked_at, trigger giữ mọi dữ liệu đã khóa và kiểm cha cũ/mới của khoản (kể cả INSERT sau chốt); ngoại lệ first_export_id chỉ đặt một lần. @m11_recalculating dùng đúng connection, không mở khóa dữ liệu đã chốt. Retention dùng tài khoản riêng, admin cũng không được sửa dòng đã khóa. Kind/id giữ định danh dòng, backend vẫn phải kiểm nghiệp vụ chốt/tổng/nguồn và trạng thái lệnh ở task sau.
- Fixture FakeCostSources dùng ApplicationEventPublisher, đăng ký ở FakeM11Config, chỉ src/test. T002 đã xong phần fixture nguồn E và port số dư fake, còn fake người đại diện chờ B/004 khai báo. Không coi fixture là producer C/D/012 hoặc fake production.
- TDD: fixture đỏ ở testCompile do chưa có lớp; CostSchemaIT trước SQL chạy 15 ca, 1 thất bại/14 lỗi do bảng chưa có. Sau migration 14/15 qua, kiểm quyền DELETE của một ca cần đọc SQLException gốc thay thông báo bọc của Spring; sửa helper, không đổi quyền hoặc nới điều kiện. Lượt sau 74 unit/arch M11 và 15 IT schema đều xanh, 0 bỏ qua. Spotless apply M11 qua; clean verify toàn backend đã xong 05:59 min: 714 unit/arch xanh (M11 74), 1441 IT còn 7 thất bại/9 lỗi tại đúng 9 lớp nền M01/M03, 0 bỏ qua/không lớp đỏ mới; 15 IT M11 xanh. Spotless check 2051 file sạch. Maven BUILD FAILURE do lỗi nền, chưa nghiệm thu toàn backend/Phase 1–2. Kết quả ở r15-storage-verification.md.
- Chờ A/000 T108: V000_09, administration.shared.api và fake, ReminderMarkers/TechnicalLocks. Không tự cài common hoặc gói A, không tạo bảng legacy để né chờ. T006 còn cần B/004; nguồn C/D và client A vẫn chờ. JSON/REST đã duyệt giữ nguyên, không sửa spec/nghiệp vụ hoặc mã web/mobile.

## Merge develop vào nhánh 008 có phần 010 — 2026-11-02

- Nhánh `impl/008-fr057-handover-notifications` mang cả phần 010 (T001, T003, T004, T005a, một phần T006) viết trên gói cũ `com.cambae.backend.m11`. Khi merge `develop` (đã đổi gói gốc ở `b60cfb6a`), chuyển sang `com.eldercare.backend.billing`, bản giả sang `support/fake/billing`.
- Lệch kỹ thuật: `LayerArchTest.moduleTopLevelPackages` của develop chỉ cho `api`, `web`, `internal`, `scheduling` ở gốc module, nên `billing.pricing.internal.domain`, `billing.provision.internal.domain`, `billing.purchase.internal.domain` chuyển thành `billing.internal.{pricing, provision, purchase}.domain` theo backend/README.md mục "Module lớn". `M11ArchTest` đổi theo (`billing.internal.balance..`, `billing.internal.procurement..`, `billing.internal.purchase..`); `CostApiContractsTest` kiểm gói port là `.internal.provision.domain`, `.internal.purchase.domain`. Không đổi chữ ký, hợp đồng hay nghiệp vụ.
- `RecordGroupTriggerIT.F_04` của develop bắt ba bảng V010_01 chưa khai nhóm dữ liệu; thêm `V010_02__cost_data_groups.sql` (`cost_statement`, `cost_item` MULTIPLE vì đã có `locked_at` và trigger `row_lock`, `no_delete`; `cost_source_index` TECHNICAL). Không sửa V010_01 vì đã chạy trên schema test của máy E và máy merge.

## Nền 010 trên develop `62fdb4f2` — 2026-11-04

- Nhánh riêng `impl/010-foundation-next`, worktree `.worktrees/010-foundation-next`; không mang thay đổi R-15 chưa commit của 008 vào đây. API bảng chung A và nguồn tài chính B đã có; các ghi chú chờ khai báo ngày 29–30/10 phía trên là lịch sử. Fixture dùng lại FakeRepresentatives của B, không tạo fake thứ hai hoặc bean giả production.
- T005 dùng BillingResidentState/CostRecalculation và API bảng chung thật; JSON history giữ precision bằng validation trước ghi. Dòng trạng thái chỉ một payload mỗi người/M11: khi làm 017 phải mở rộng record chung, không ghi đè bằng một SharedKind M11 khác. T007 dùng TechnicalLocks trước lockForUpdate để tuần tự hóa cả lần chưa có dòng; sau đó khóa bảng đúng người, transaction MANDATORY. Không thêm migration/API công khai.
- T006 hoàn tất hàm thuần BillingPeriod; không kéo kiểu B vào domain Java thuần. Adapter chọn nguồn/mốc dừng và kiểm bedReleased còn T011/T014. Không dùng ngày hết hạn hợp đồng thay ngày kết thúc lưu trú; cần B/E rà `billingStopDate` khi hồ sơ còn PREPARING sau ngày dự kiến (FR-035).
- T007 chỉ dựng khung hai job và CostTimedTask. Mốc đầu có transaction riêng, slot thành công cùng transaction với tác động; marker SQL chỉ đọc tiền tố M11. Dùng SchedulerRuns, giữ/khôi phục MDC, ShedLock cho lịch và startup, tắt startup bằng công tắc chung trong test. Tác vụ chuyển kỳ/nhắc thật làm ở story tương ứng; chưa thêm vào API runJobs. Thêm đúng hai mục job M11 vào SchedulerScopeArchTest theo AGENTS.md mục 4, chờ A review phần test đăng ký; không sửa mã common của A.
- T008 tách T008a để nghiệm thu phần FieldClassificationProvider/ScopeAssignmentProvider cho bảng hiện có. Parent T008 vẫn mở: retention toàn chuỗi, record mua hộ/quyền tệp chưa có nghiệp vụ. Không tạo provider rỗng hoặc CorrectableRecordProvider để đánh xong nền.
- TDD và kết quả kiểm ở [foundation-checkpoint-2026-11-04.md](foundation-checkpoint-2026-11-04.md). Các lượt đỏ thiếu kiểu Java là lỗi testCompile, tách rõ với lượt đỏ runtime (fake, precision, governance); không gọi lỗi biên dịch là assertion nghiệp vụ đã thất bại.
- Giữ nguyên gate HTTP: bản dịch đã duyệt, nhưng 17 response 2xx thiếu schema và vài object chưa đủ cấu trúc cần chủ dự án chốt trước endpoint liên quan. Quantity/newQuantity của 005 đã chốt: xem [quyết định 05/11](care-quantity-decision-2026-11-05.md); còn C cài producer và E cài consumer, không tự quy đổi ml/phút/giá trị đo. Không sửa spec.md/nghiệp vụ hoặc API nguồn của người khác.

## Retention hiện có và quyết định quantity — 2026-11-05

- Tách T008b từ parent T008 tương tự T008a: chỉ nghiệm thu ba loại kỳ/bảng/khoản đang có dữ liệu thật. CostRetention ở persistence giữ JDBC; M11GovernanceConfig chỉ nhận store và đăng ký provider. Mốc hồ sơ qua ResidentApi công khai; xóa bằng RetentionDatabase trong transaction của job. Không đổi API, schema, CFG hoặc cơ chế bảng chung A.
- 106 unit/arch + 62 IT xanh, gồm 9 ca retention mới. Chi tiết TDD, lỗi đã sửa, phạm vi và bằng chứng ở [retention-checkpoint-2026-11-05.md](retention-checkpoint-2026-11-05.md). Không chạy full verify.
- Lệch phân rã task: parent T008 không thể đóng bằng provider rỗng trước khi có record mua hộ/chỉ mục thật; các phần này đi cùng T010/T021/T025. Phase 2 vẫn mở. Chuỗi hồ sơ thật 001 → 010 và định danh chỉ mục theo nguồn cần A/B/chủ nguồn review trước khi nghiệm thu; không tự sửa module khác.
- Quyết định quantity/newQuantity đã nhận đủ, được ghi ở [care-quantity-decision-2026-11-05.md](care-quantity-decision-2026-11-05.md). Hai dòng chờ E/C chuyển sang cài producer/consumer; không giữ lý do chờ người dùng gửi quyết định.
