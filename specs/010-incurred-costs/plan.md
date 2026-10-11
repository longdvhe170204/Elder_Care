# Implementation Plan: Chi phí phát sinh

**Branch**: `010-incurred-costs` | **Date**: 2026-10-10 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/010-incurred-costs/spec.md`

## Summary

Feature 010 là phần chi phí phát sinh của module M11 (mục 15.1 → 15.8; UC-61 → UC-64, UC-79). Nó cung cấp:
1. Kỳ chi phí tháng dương lịch, bảng chi phí thường theo người theo kỳ, bảng bổ sung, kỳ cuối (mục A, G).
2. Hành chính lập khoản Nháp cho từng sự kiện tính phí (từng khoản hoặc hàng loạt) sau khi tra cứu bản ghi nguồn; mỗi khoản trỏ đúng một nguồn (mục B, Q-306, Q-307).
3. Đơn giá và số tiền theo hợp đồng hoặc phiên bản đơn giá tại ngày phát sinh, gói, hệ số vắng, chia giá tháng, làm tròn (mục C).
4. Vòng đời khoản Nháp → Đã kiểm tra → Đã duyệt → Đã chốt / Đã hủy; tính lại khi nguồn đổi trước chốt; báo Hành chính sau chốt (mục D).
5. Khoản nhập tay, đề nghị mua hộ với đồng ý của người đại diện hoặc duyệt của Quản lý viện (mục E).
6. Khoản điều chỉnh do Hành chính lập (mục F).
7. Gửi chốt, chốt bảng, cảnh báo biến động, điều kiện chi phí kỳ cuối cho Kết thúc lưu trú, qua đời (mục G).
8. Bảng chi phí và tạm tính cho người thân, dữ liệu cho 017, 016, xuất kế toán (mục H).

Hướng kỹ thuật:
- Gói `m11` với tám gói con; 017 và phần mua sắm của 019 (cùng M11) dùng port nội bộ (research P-01, P-12).
- Ngày lưu trú và buổi bán trú tính từ 004 lúc tra cứu và lúc lập, không lưu; nguồn của 005, 006, 011, 012, 014 vào một **chỉ mục kỹ thuật** dựng từ sự kiện, chỉ để tra cứu và tìm khoản khi nguồn đổi; không có danh sách "chưa lập khoản" (P-04).
- Đơn giá, chia giá tháng, buổi bán trú, thời gian tính phí, biến động, điều kiện kỳ cuối, tạm tính là hàm thuần kiểm thử bằng bảng (P-05, P-07, P-08, P-10, P-11).
- DBR-15 bằng cột sinh `UNIQUE` theo (nguồn, đơn vị) cho khoản đang hiệu lực; DBR-17 bằng trigger trạng thái cuối của bảng và khoản (data-model mục 8).
- Kỳ cuối: `KyCuoiPort` chạy trong lệnh hồ sơ kết thúc của 004; điều kiện `KT_A_CHI_PHI` qua bean `ThamGiaChuyenTrangThai` (P-10).
- Bảng Đã chốt phát `BangChiPhiDaChot` đúng một lần cho 017 (DBR-30 ở 017).

Plan tuân theo FR-010a của 000 (Q-306, Q-307):
- 010 không lập khoản, bảng (trừ tác động kèm lệnh lập khoản và lệnh hồ sơ kết thúc), khoản điều chỉnh; không gợi ý đơn giá, không tính sẵn số chênh lệch, không có dấu "không tính";
- hệ thống chỉ: tính đơn giá và số tiền khi Hành chính lập; tính lại, đưa về Nháp, hủy khoản chưa chốt khi nguồn đổi; chuyển trạng thái kỳ theo lịch; kiểm tra và chặn; nhắc, thông báo.

## Technical Context

**Language/Version**: Java 21 LTS; TypeScript 5.x; Dart 3.x với Flutter stable 3.x (000 R-01).

**Primary Dependencies**: Như 000 → 009. Thêm thư viện ghi XLSX dạng luồng (fastexcel) cho xuất kế toán (Q-05); CSV không cần thư viện.

**Storage**: MySQL 8.4 LTS. Bảng ở [data-model.md](data-model.md).

**Testing**:
- Như 000 R-14.
- Test bảng cho tám hàm thuần: `LamTronDong`, `PhiNgayLuuTru`, `PhiBuoiBanTru`, `ThoiGianTinhPhi`, `BienDongKy`, `DieuKienChiPhiKyCuoi`, `KiemTraVuot`, `TongTamTinh`; SC-002 với 12 tháng.
- Testcontainers cho `UNIQUE` (DBR-15, bảng mỗi kỳ) và trigger đã chốt (DBR-17).
- Test đồng thời: hai lệnh lập cùng đơn vị; chốt trong lúc lập khoản; hai người đại diện quyết định.
- Đồng hồ giả lập cho CFG-M11-01, 02, 03, 06, CFG-M15-05, 06, CFG-M02-09, chuyển kỳ.
- Bản giả nguồn của 011, 012, 014, `NguoiDaiDienPort`, `SoDuMuaHoPort`.

**Target Platform**: Như 000.
- Web: tra cứu nguồn và lập khoản, bảng chi phí (kiểm tra, duyệt, chốt), nhập tay, điều chỉnh, biến động sau chốt, đề nghị mua hộ, xuất kế toán; cổng người thân trên trình duyệt (bảng, tạm tính, đồng ý mua hộ).
- App nhân viên: không có màn của 010.
- App người thân: bảng chi phí, tạm tính, đồng ý/từ chối đề nghị mua hộ.

**Project Type**: Web service (REST `/api/v1`), một web client và hai mobile client.

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
| IX. Phạm vi giai đoạn | Đạt | Chỉ tài liệu thiết kế; không mã, không `src/`, không cấu hình build, không SQL; không đổi nghiệp vụ. Hợp đồng 004 được bổ sung một sự kiện (mục "Đồng bộ") |
| X. Kiến trúc | Đạt | Một khối; gói `m11`; module khác dùng `m11.api`. Không có vòng (P-02). REST; MySQL; ba client. NFR ở bảng bên dưới |
| XI. Nghiệp vụ ở backend | Đạt | Đơn giá, điều kiện lập, chốt, vượt mua hộ, điều kiện kỳ cuối ở backend. Mỗi lệnh có endpoint riêng |
| XII. Dữ liệu | Đạt | DBR-15, 17 bằng `UNIQUE` và trigger; DBR-16 bằng căn cứ đơn giá lưu theo khoản; nhật ký trong transaction; `Clock` |
| XIII. Tham số | Đạt | Như V; CFG-M11-02 lúc gửi được lưu trên đề nghị (FR-023) |
| XIV. Ngoại tuyến, thông báo | Đạt | 010 không có ghi ngoại tuyến. Mọi thông báo là sự kiện tới 009 với mức của FR-044 |
| XV. Truy vết, kiểm thử | Đạt | Tên test theo BR, FR. Mỗi BR của 010 có test (data-model mục 11) |

**Kiểm lại sau Phase 1**: Đạt. Thiết kế không thêm vai trò, trạng thái hay quy tắc nghiệp vụ ngoài spec. Có bốn bổ sung kỹ thuật, giải thích ở Complexity Tracking:
1. Chỉ mục kỹ thuật sự kiện tính phí.
2. Port nội bộ M11 cho 017.
3. Bảng `bien_dong_nguon_sau_chot`.
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
└── tasks.md                     # Phase 2 (/speckit-tasks; chưa tạo)
```

### Source Code (repository root)

Cấu trúc dự kiến cho giai đoạn triển khai. Giai đoạn thiết kế không tạo các thư mục này (CLAUDE.md).

```text
backend/src/main/java/vn/eldercare/m11/
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

web/src/tinh-nang/chi-phi/
├── tra-cuu-nguon/           # tra cứu, lập từng khoản / hàng loạt
├── bang-chi-phi/            # kiểm tra, duyệt, trả lại, gửi chốt, chốt
├── dieu-chinh/              # biến động sau chốt, lập điều chỉnh
├── mua-ho/
├── xuat-ke-toan/
└── cong-nguoi-than/chi-phi/

app-nguoi-than/lib/tinh-nang/chi-phi/   # bảng, tạm tính, đồng ý mua hộ
```

**Structure Decision**: Theo R-02 của 000. Feature 010 nằm ở `backend/…/m11` với chín gói con và gói `api`; 017 và phần mua sắm của 019 thêm gói con vào cùng `m11`. Web có đủ chức năng. App người thân có bảng, tạm tính và đồng ý mua hộ.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Chỉ mục kỹ thuật `su_kien_tinh_phi` | Tra cứu nguồn theo người, loại, khoảng ngày (FR-005) và tìm khoản khi nguồn đổi (FR-020) trên năm module | Gọi API đọc của từng module mỗi lần tra cứu: 005, 006 không có API tìm theo tính phí; vẫn cần ánh xạ nguồn → khoản |
| Port nội bộ M11 (`ChiPhiChoSoDuPort`, `SoDuMuaHoPort`) | 017 cùng module cần tổng chưa chốt, phí ngày cơ sở; 010 cần dấu số dư không đủ | Đặt ở `m11.api`: mở dữ liệu nội bộ cho mọi module |
| `bien_dong_nguon_sau_chot` | FR-027 đòi báo kèm khoản gốc, sự kiện, giá trị trước/sau và khoản điều chỉnh trỏ về sự kiện gây ra; không được tính sẵn số chênh lệch | Chỉ gửi thông báo: mất liên kết khoản điều chỉnh → sự kiện |
| Thư viện ghi XLSX | Q-05 chọn Excel/CSV; kế toán quen Excel | Chỉ CSV: lỗi mã hóa khi mở bằng Excel ở một số máy; vẫn giữ CSV làm tùy chọn |

## Đồng bộ với hợp đồng của feature khác

Plan này bổ sung hoặc dựa vào các điểm dưới đây; plan của feature liên quan giữ đúng hoặc ghi rõ thay đổi.

1. **004** (đã ghi bổ sung vào [../004-admission-residency/contracts/module-interfaces.md](../004-admission-residency/contracts/module-interfaces.md) mục 1.8): sự kiện `PhienBanDonGiaDaTao(dichVuId, hieuLucTu, hieuLucDen)` khi tạo phiên bản đơn giá (kể cả hiệu lực lùi cho khoảng chưa có phiên bản, DBR-08), để 010 tính lại khoản thiếu đơn giá (FR-012). 010 cài `KyCuoiPort`, `MucViecSauQuaDoiPort` (`CHI_PHI_KY_CUOI`), dùng `KT_A_CHI_PHI` và các API, sự kiện của 004 như đã khai báo.
2. **005, 006**: dùng các sự kiện tính phí đã khai báo (005 C-13, 006 D-17); 010 cài `DeNghiMuaHoPort` đúng chữ ký. Không đổi.
3. **011, 012, 014**: phát `m11.api.SuKienTinhPhiNguon`, `SuKienTinhPhiNguonDaDoi` cho suất ăn người thân, đêm ở lại, điểm danh có phí ([contracts/module-interfaces.md](contracts/module-interfaces.md) mục 1.5). **012** cài `NguoiDaiDienPort` (đã khai báo ở 004) và đọc `ChiPhiCongNguoiThanApi`.
4. **017** (cùng M11): cài `SoDuMuaHoPort`; dùng `ChiPhiChoSoDuPort`; nghe `BangChiPhiDaChot` (đúng một lần mỗi bảng) và `DeNghiMuaHo*`. **016** đọc `ChiPhiThongKeApi`. **009**: thông báo theo FR-044 cài `CoYeuCauThongBao`.

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX). Đề nghị chủ dự án quyết bằng mã Q mới khi cần, rồi sửa `docs/business/nghiep-vu.md` và spec liên quan.

1. **Tính lại khoản thiếu đơn giá "kèm lệnh tạo phiên bản đơn giá"** (FR-012): lệnh tạo phiên bản thuộc 004; plan làm việc tính lại ngay sau commit của lệnh đó qua sự kiện `PhienBanDonGiaDaTao` (vài giây), không trong cùng transaction. Kết quả nghiệp vụ không đổi (khoản chưa chốt được tính lại, gỡ dấu); ghi lại để chủ dự án biết "kèm lệnh" được hiểu là "do lệnh đó kích hoạt". *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

2. **Kỳ của viện "Tới ngày 1 của tháng → Đang mở, Hệ thống (theo lịch)"**: Q-306 cấm hệ thống tự lập bản ghi nghiệp vụ, nhưng bảng trạng thái kỳ của spec cho hệ thống mở kỳ theo lịch. Plan coi kỳ của viện là mốc thời gian (không gắn người, không có tác động), mở bằng job; bảng và khoản vẫn chỉ lập kèm lệnh của Hành chính. Đề nghị xác nhận cách hiểu này hoặc ghi rõ ở 15.7. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

3. **Câu chữ còn lẫn trong spec 010** (không đổi nghiệp vụ): *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - User Story 5 đánh số kịch bản 1 → 6, 8, 9, 7 (kịch bản 8, 9 đứng trước 7).
   - Điểm báo lại 13 còn nêu BR-M11-05 "hệ thống tạo khoản Điều chỉnh chờ duyệt"; sau Q-307 hệ thống chỉ báo (FR-027). Đề nghị xác nhận BR-M11-05 ở `docs/business/nghiep-vu.md` đã viết theo Q-307.
   - Assumptions còn câu dẫn `docs/phan-tich-yeu-cau.md` mục 4.2 (đã bỏ).

4. **Mức thông báo nhắc đề nghị mua hộ chờ đồng ý** (bảng FR-044 ghi "Theo feature 000 FR-031a"): plan dùng Trung bình như dòng 000 của bảng 009 FR-043b. Đề nghị ghi mức cụ thể vào FR-044. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
