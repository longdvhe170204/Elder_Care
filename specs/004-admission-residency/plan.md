# Implementation Plan: Tiếp nhận và lưu trú

**Branch**: `004-admission-residency` | **Date**: 2026-10-07, cập nhật 2026-10-18 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/004-admission-residency/spec.md`

## Summary

Feature 004 là module M02 (mục 6). Nó cung cấp:
1. Lượt đăng ký tiếp nhận, tiến trình và danh sách điều kiện tiếp nhận (mục A; UC-09, UC-08 phần điều kiện); từ 2026-10-11 có sáu điều kiện, thêm (f) DBR-02 (Q-343).
2. Danh sách chờ có điểm ưu tiên tự tính. Khi giường về Trống, Hành chính được báo và tự chọn hồ sơ; hệ thống giữ chỗ tạm cho hồ sơ được liên hệ (mục B; UC-10).
3. Hợp đồng: khóa khi Hiệu lực, không chồng thời gian, duyệt điều khoản khác chuẩn, quá hạn hợp đồng; khoản cần đặt cọc lấy trạng thái từ 017 (mục C, D; UC-11, UC-12).
4. Danh mục dịch vụ và phiên bản đơn giá (mục E).
5. Yêu cầu thay đổi lưu trú thành phụ lục có ngày hiệu lực (mục F; UC-13, UC-14). Từ 2026-10-11 Hành chính duyệt được thay đổi nhỏ (FR-043, Q-338) qua `gioiHanVaiTroDuyet` bổ sung vào SPI của 000 (research T-06).
6. Lượt vắng, bảng chính sách phí khi vắng, quyết định giữ giường; báo vắng và ngày nghỉ bán trú (mục G; UC-15, UC-16).
7. Kết thúc lưu trú với danh sách điều kiện, bàn giao, ngoại lệ (mục H; UC-17).
8. Ghi nhận qua đời và danh sách việc sau qua đời (mục I; UC-18).
9. Dữ liệu ngày lưu trú cho 010 tính phí (mục J), và việc hằng ngày có chạy bù (mục K).

Hướng kỹ thuật:
- Nội dung hợp đồng lưu thành phiên bản chỉ thêm: phiên bản 0 lúc ký, mỗi phụ lục thêm một phiên bản. "Nội dung tại ngày D" là một truy vấn có chỉ mục (research T-04).
- Ràng buộc đưa xuống MySQL:
  - khóa dòng `luu_tru_nct` cùng trigger chống chồng hợp đồng, chống chồng phiên bản đơn giá;
  - trigger khóa nội dung hợp đồng ngoài Nháp;
  - cột sinh `UNIQUE` cho "một bản ghi mở mỗi người" (data-model mục 12).
- Mọi tác động lên trạng thái người cao tuổi đi qua SPI `ThamGiaChuyenTrangThai` của 001; bean của 004 chạy đầu tiên. Lệnh danh sách chờ dẫn tới Hủy tiếp nhận cũng đi qua 001, nên chỉ có một thứ tự khóa (T-03).
- Yêu cầu thay đổi lưu trú dùng vòng đời của 000:
  - phụ lục tạo và giường giữ trong transaction duyệt;
  - áp dụng bằng job của 000 vào ngày hiệu lực (T-06).
- Điều kiện kết thúc (a) → (d), (h) đọc lại qua `xemDieuKien` của 001, nên 004 không phụ thuộc năm module sở hữu (T-09).
- Module thấp hơn khai báo SPI để không có vòng phụ thuộc. Giao diện 001, 002 cần từ 004 chuyển thành SPI ở gói của 001, 002 (T-02).

Plan tuân theo FR-010a của 000 (Q-306, Q-307):
- 004 không tự lập yêu cầu, chi phí hay hồ sơ;
- không đề xuất hồ sơ chờ, không tạo yêu cầu quyết định giữ giường;
- job hằng ngày chỉ tính điểm, tính ngày vắng, đặt dấu theo thời gian, nhắc, báo;
- tạo bảng kỳ cuối nháp ở 010 là tác động kèm lệnh lập hồ sơ kết thúc (FR-066).

### Cập nhật 2026-10-18 (spec mục "Cập nhật 2026-10-18" và "Cập nhật 2026-10-18, lượt 2": Q-356, 009 FR-001a)

- **Ý kiến của người cao tuổi về việc vào ở** (FR-001, Q-356): bảng con `y_kien_vao_o` của lượt đăng ký (nhóm 2, nội dung bất biến, thay thế bằng bản mới) có nguồn theo mẫu 001 (`NguonYKienApi`); lệnh `POST /luot-dang-ky/{id}/y-kien-vao-o` của Hành chính (research T-09a, data-model mục 3).
- **Hoàn tất tiếp nhận** (FR-004a): ý kiến không là điều kiện. Danh sách điều kiện trả `yKienVaoO` hiện hành kèm nguồn; bean `ThamGiaM02` trả `canhBao` `CHUA_GHI_Y_KIEN_VAO_O` khi chưa có ý kiến và `NCT_KHONG_DONG_Y_VAO_O` khi ý kiến hiện hành là "Không đồng ý"; lý do của lệnh đã bắt buộc theo 001 FR-046 và lưu ở lịch sử trạng thái của 001 (plan 001, điểm báo lại 12). Tác động của lệnh ghi lại ý kiến hiện hành lúc tiếp nhận lên lượt đăng ký.
- **Đối tượng thông tin** (FR-078, 009 FR-001a): 004 không tự gửi thông báo nào tới người thân; thông báo qua đời do 001 phát (mã Q-354 `QUA_DOI`); dấu `viecSauQuaDoi` cho module khác giữ nguyên (research T-12).
- Không thêm thư viện, vai trò, trạng thái. Điểm báo lại 9 bên dưới.

### Cập nhật 2026-10-22 (spec mục "Cập nhật 2026-10-22": Q-379, Q-380, Q-385)

- **Điều kiện (h) tách hai ý** (FR-063 (h), FR-064, 017 FR-022): bean của 017 trả hai mã cho `KET_THUC_LUU_TRU`: `KT_H_QUYET_TOAN` (ba ý về tiền: kỳ cuối đã chốt và trừ, cọc 0, số dư 0; `coTheMien = true`) và `KT_H_XAC_NHAN_QUYET_TOAN` (không còn giao dịch Chờ duyệt, Kế toán đã xác nhận quyết toán và số liệu chưa đổi sau đó; `coTheMien = false`; theo BR-M11-22, chủ dự án chọn 2026-10-22, xem plan 017 điểm báo lại). Màn điều kiện vẫn hiện một dòng (h): Chưa đạt khi còn một trong hai mã; "Đạt (ngoại lệ)" khi chỉ `KT_H_QUYET_TOAN` được miễn; yêu cầu ngoại lệ (h) chỉ nhận `KT_H_QUYET_TOAN` (research T-09, module-interfaces mục 2.1).
- **Cho 017** (017 FR-022, FR-034, bảng giao tiếp "Nhận 004"): API đọc mới `HoSoKetThucDocApi` (hồ sơ kết thúc Đang chuẩn bị, thông tin cho bảng quyết toán, ngoại lệ (h) đã duyệt) và sự kiện `NgoaiLeKetThucDaDuyet` (module-interfaces mục 1.5b, 1.8).
- **Cổng người thân** (FR-068): `DieuKienTiepNhanApi.dieuKienKetThuc` giữ nguyên; bảng quyết toán do 012 lấy từ 017 (017 FR-038), 004 không lưu file quyết toán.
- **Sửa sai tiền cọc** (Q-385): không còn giao dịch đảo, số phiếu thu; 017 báo trạng thái đặt cọc mới sau điều chỉnh có duyệt, 004 xử lý như mọi lần đổi trạng thái (FR-032 giữ nguyên).
- **Q-380 (sửa theo Q-385)**: không có thời hạn báo trước, không thêm tham số; phí chấm dứt sớm là khoản nhập tay của 010. 004 không đổi.
- **Q-387** (FR-071a, chốt 2026-10-22 sau plan): ngoại lệ cho mục quyết toán của danh sách việc sau qua đời dùng cùng loại yêu cầu `M02.NGOAI_LE_KET_THUC` với mã `KT_H_QUYET_TOAN`; `ngoai_le_dieu_kien` thêm `nct_id`, `ho_so_ket_thuc_id` được rỗng khi căn cứ là danh sách việc sau qua đời; endpoint mới `POST /nguoi-cao-tuoi/{nctId}/viec-sau-qua-doi/yeu-cau-ngoai-le-quyet-toan` (Hành chính, Kế toán); `HoSoKetThucDocApi.coNgoaiLeQuyetToanDaDuyet` trả cả hai căn cứ; `NgoaiLeKetThucDaDuyet` mang `hoSoKetThucId` rỗng khi qua đời (research T-13).
- Không thêm bảng, thư viện, vai trò, trạng thái.

## Technical Context

**Language/Version**: Java 25; JavaScript (JSX); Dart 3.x với Flutter stable 3.x (000 R-01).

**Primary Dependencies**: Như 000 → 003: Spring Boot 4.1.x, Spring Modulith 2.x, Flyway (bản do Spring Boot quản lý), springdoc-openapi 3.x, ShedLock, Spring Security 7. Web: React 19, TanStack Query. App: Flutter. Không thêm thư viện mới.

**Storage**: MySQL 8.0. Bảng ở [data-model.md](data-model.md). Tiền `DECIMAL(15,0)` VND. Bản scan, bằng chứng qua `common.tep` (001 H-13).

**Testing**:
- Như 000 R-14.
- Test đơn vị dạng bảng cho bốn hàm thuần: điểm ưu tiên, tra chính sách vắng, điều khoản khác chuẩn, nội dung tại ngày.
- Test tích hợp MySQL cho trigger, cột sinh.
- Test đồng thời cho DBR-06, FR-046 và "Đã liên hệ".
- Đồng hồ giả lập cho mọi CFG-M02, gồm chạy bù.
- Bản giả cho SPI của 005, 010, 012, 017 và bean điều kiện của 006, 007, 010, 013, 017 (research T-16).

**Target Platform**: Như 000.
- Web: mọi màn của 004.
- App nhân viên: Cho tạm vắng, Gia hạn dự kiến, Ghi nhận trở về cho Trưởng tầng; xem lượt vắng cho Điều dưỡng.
- App người thân: tiến trình tiếp nhận, hợp đồng và phụ lục (người đại diện), lượt vắng, tiến trình kết thúc, gửi yêu cầu thay đổi ở Nháp.
- Lệnh của 004 chỉ chạy trực tuyến (ngoài phạm vi 8.6).

**Project Type**: Web service (REST `/api/v1`), một web client và một mobile client chung (Q-346).

**Performance Goals**:
- Lệnh nằm trong 2 giây của NFR-02: mỗi lệnh khóa 1–3 dòng, vài truy vấn có chỉ mục.
- Danh sách hồ sơ phù hợp với một giường: lọc vài chục hồ sơ chờ trong bộ nhớ sau một truy vấn. Thông báo giường Trống đi qua outbox ngay sau commit của 003, dưới chỉ tiêu 5 phút của SC-003.
- Job hằng ngày cho 300 người, khoảng 30 lượt vắng: dưới 1 phút, trong NFR-04.

**Constraints**:
- Không có thao tác sửa trạng thái; mỗi lệnh một endpoint (constitution XI).
- Hợp đồng Hiệu lực, phụ lục, phiên bản nội dung, bản ghi nhóm 3 không bị `UPDATE`, `DELETE` (constitution XII).
- Nhật ký trong transaction (000 FR-047).
- CFG đọc lúc chạy (constitution XIII).
- Không tự sinh bản ghi nghiệp vụ (000 FR-010a).

**Scale/Scope**:
- 300 người cao tuổi, khoảng 100 hồ sơ chờ, khoảng 600 hợp đồng mỗi năm.
- 9 thực thể có vòng đời; 4 loại yêu cầu phê duyệt.
- 1 job hằng ngày với 5 việc.
- 52 đường dẫn REST (contracts/api-004.yaml).
- 6 SPI 004 khai báo; 6 SPI 004 cài cho module thấp hơn.

Không còn mục NEEDS CLARIFICATION. Các lựa chọn kỹ thuật đã chốt ở [research.md](research.md). Các điểm về tài liệu nguồn ghi ở "Điểm cần báo lại"; chúng không chặn thiết kế.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Bằng chứng |
| --- | --- | --- |
| I. Nguồn gốc, truy vết | Đạt | Mỗi quyết định dẫn FR, BR, DBR. Data-model mục 15 là bảng truy vết DBR-02, 06, 07, 08, 23, 25 và BR-M02-01 → 10, 15 → 22 |
| II. Spec không nêu công nghệ | Đạt | Spec không đổi; công nghệ chỉ ở plan, research |
| III. Không mặc định CRUD | Đạt | Dịch vụ theo mẫu nhóm 1, đơn giá theo phiên bản. Hợp đồng chỉ sửa ở Nháp. Mọi đối tượng nhóm 2 khác chỉ có lệnh. Phiên bản nội dung, lịch sử điểm, quyết định giữ giường, bàn giao, qua đời chỉ thêm và đính chính |
| IV. Trạng thái kiểm tra được | Đạt | Bảng chuyển ở data-model mục 3 → 10 khớp các bảng trạng thái của spec. Mỗi kịch bản có test (quickstart mục 4) |
| V. Không cố định giá trị | Đạt | CFG-M01-02, M02-02 → 10, M03-02 đọc từ `ThamSoApi`. Hệ số mặc định 0% khi giải phóng là giá trị điền sẵn trên quyết định (FR-055). Giờ chạy job 00:15 là thông số kỹ thuật (Complexity Tracking) |
| VI. Vai trò, quyền | Đạt | Khóa quyền theo dòng Phụ lục 27 (T-14); chú thích ², ⁶, ⁷, ⁸, ²⁶ được cài. Không thêm vai trò. Quyền Kế toán lập ngoại lệ (h) chỉ có ở spec, báo lại ở điểm 6 |
| VII. Điểm chưa rõ | Đạt | Không tự đoán nghiệp vụ. BR-M02-20 lệch FR-012: plan theo FR-012, báo lại ở điểm 1. Lệnh "đăng ký lịch đến" chưa rõ: plan chọn cách hẹp, báo lại ở điểm 4 |
| VIII. Tiêu chí thành công | Đạt | SC-001 → SC-012 có cách đo ở quickstart mục 5 |
| IX. Phạm vi giai đoạn | Đạt | Chỉ tài liệu thiết kế; không mã, không `src/`, không cấu hình build, không SQL; không đổi nghiệp vụ. Hợp đồng 000, 001, 002, 003 được bổ sung giao diện, không đổi hành vi đã thiết kế |
| X. Kiến trúc | Đạt | Một khối; gói `m02`; module khác chỉ dùng `m02.api`. Không có vòng phụ thuộc (T-02). REST `/api/v1`; MySQL; hai client (Q-346). NFR ở bảng bên dưới |
| XI. Nghiệp vụ ở backend | Đạt | Điều kiện, bảng chuyển, quyền, giới hạn trường ở backend. Mỗi lệnh có `POST …/{id}/<lenh>` riêng; lệnh trạng thái người cao tuổi dùng endpoint của 001 với `chiTiet` do 004 kiểm |
| XII. Dữ liệu | Đạt | DBR-06 có khóa và trigger; DBR-07 có `UNIQUE NOT NULL`; DBR-08 có khóa và trigger; nhóm 3 theo mẫu của 000; nhật ký trong transaction; thời gian theo `Clock` |
| XIII. Tham số | Đạt | Như V. Bảng CFG-M02-05, CFG-M02-08 đọc giá trị hiện hành tại lúc tính (FR-052, FR-077) |
| XIV. Ngoại tuyến, thông báo | Đạt | Lệnh của 004 chỉ trực tuyến. Mọi thông báo là sự kiện tới 009, có mức (T-12) |
| XV. Truy vết, kiểm thử | Đạt | Tên test theo BR, FR. Mỗi BR của 004 có test (data-model mục 15). Không có BR nào của 004 thực hiện ở client |

**Kiểm lại sau Phase 1 (2026-10-22)**: Đạt. Q-379, Q-385 chỉ thêm một mã điều kiện không miễn được do 017 trả, một API đọc và một sự kiện cho 017; không thêm bảng, trạng thái, vai trò. Điểm báo lại 10.

**Kiểm lại sau Phase 1**: Đạt. Thiết kế không thêm vai trò, trạng thái hay quy tắc nghiệp vụ ngoài spec. Có năm bổ sung kỹ thuật, giải thích ở Complexity Tracking:
1. Bảng `luu_tru_nct` làm đích khóa theo người.
2. Phiên bản nội dung hợp đồng, cùng cột sao `ngay_ket_thuc_hien_hanh`.
3. Bảng kết quả tính `ngay_vang`, `muc_viec_sau_qua_doi`.
4. Lệnh chuyển trạng thái không dùng `If-Match`.
5. Các giao diện bổ sung vào hợp đồng 000, 001, 002, 003.

### Đáp ứng yêu cầu phi chức năng (constitution X)

| NFR | 004 chịu ảnh hưởng | Cách đáp ứng |
| --- | --- | --- |
| NFR-01 Quy mô | Có | Vài trăm người, vài trăm hợp đồng, khoảng 100 hồ sơ chờ; chỉ mục ở data-model |
| NFR-02 Hiệu năng | Có | Lệnh khóa ít dòng; nội dung tại ngày là một truy vấn; danh sách điều kiện gộp từ API có chỉ mục |
| NFR-03 Thông báo khẩn cấp | Gián tiếp | Thông báo qua đời Khẩn cấp do 001, 009 gửi từ sự kiện của lệnh (Q-97); 004 không chặn đường đó |
| NFR-04 Tác vụ định kỳ | Có | `lan_chay_theo_ngay` bảo đảm mỗi việc mỗi ngày một lần; nhắc gộp; khóa sự kiện (T-11) |
| NFR-05 Khả dụng | Có | Chạy bù từng ngày khi khởi động lại (FR-077) |
| NFR-06 Sao lưu | Gián tiếp | Dữ liệu ở MySQL; tệp theo 001 H-13 |
| NFR-07 Lưu trữ | Có | `LuuTruProvider` theo hồ sơ cho mọi bảng gắn người cao tuổi (data-model mục 14) |
| NFR-08 Bảo mật | Có | Trường tài chính `TAI_CHINH`; nguyên nhân tử vong `SUC_KHOE`; danh sách chờ chỉ Hành chính, Quản lý viện |
| NFR-09 Thời gian | Có | `Clock`; ngày vắng, ngày tính phí theo ngày dương lịch Asia/Ho_Chi_Minh (FR-058) |
| NFR-10 Thiết bị | Có | Web đủ chức năng; app nhân viên cho tạm vắng, trở về; app người thân xem và gửi yêu cầu |
| NFR-11 Ngôn ngữ | Có | Tiền VND, ngày dd/MM/yyyy ở client; thông báo từ chối liệt kê điều kiện kèm mã |
| NFR-12 Cấu hình | Có | Bảng chính sách vắng, bảng điểm, các hạn nhắc đổi qua tham số |
| NFR-13 Kiểm thử | Có | Đồng hồ giả lập; bản giả bật tắt được |

## Project Structure

### Documentation (this feature)

```text
specs/004-admission-residency/
├── plan.md                      # File này
├── research.md                  # Phase 0: T-01 → T-16
├── data-model.md                # Phase 1: bảng, bảng chuyển, bảo đảm ở cơ sở dữ liệu, truy vết
├── quickstart.md                # Phase 1: kịch bản kiểm chứng, truy vết kịch bản → test
├── contracts/
│   ├── api-004.yaml             # OpenAPI 3.1: đăng ký, danh sách chờ, hợp đồng, dịch vụ, thay đổi, vắng, kết thúc, qua đời
│   └── module-interfaces.md     # Giao diện Java: 004 cung cấp / 004 cần; chiTiet của lệnh trạng thái; mã điều kiện kết thúc
└── tasks.md                     # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

Cấu trúc dự kiến cho giai đoạn triển khai. Giai đoạn thiết kế không tạo các thư mục này (CLAUDE.md).

```text
backend/src/main/java/com/cambae/backend/m02/
├── api/          # HopDongDocApi, DonGiaApi, NgayLuuTruApi, DieuKienTiepNhanApi, DanhSachViecSauQuaDoiApi,
│                 # SPI 004 khai báo (KyCuoiPort, TrangThaiDatCocPort, NguoiDaiDienPort, BanGhiDonPort,
│                 # TrangThaiCoMatBanTruPort, MucViecSauQuaDoiPort), sự kiện
├── dangky/       {internal, web}
├── danhsachcho/  {internal, web}   # TinhDiemUuTien, danh sách phù hợp, liên hệ giường
├── hopdong/      {internal, web}   # phiên bản nội dung, XacDinhKhacChuan, handler duyệt điều khoản
├── dichvu/       {internal, web}
├── thaydoi/      {internal, web}   # handler M02.THAY_DOI_LUU_TRU, phụ lục
├── vang/         {internal, web}   # TraChinhSachVang, ngày vắng, quyết định giữ giường, bán trú
├── ketthuc/      {internal, web}   # hồ sơ kết thúc, bàn giao, handler ngoại lệ
├── quadoi/       {internal, web}
├── tinhphi/      {internal}        # NgayLuuTruApi
├── trangthai/    {internal}        # bean ThamGiaChuyenTrangThai; cài SPI của 001, 002, 003
└── lich/                           # ViecHangNgayM02 (T-11)

frontend/src/tinh-nang/
├── dang-ky-tiep-nhan/       # lượt đăng ký, tiến trình, điều kiện tiếp nhận
├── danh-sach-cho/           # hồ sơ chờ, điểm, hồ sơ phù hợp với giường, liên hệ
├── hop-dong/                # nháp, điều khoản khác chuẩn, gửi ký, ghi nhận đã ký, nội dung tại ngày, phụ lục
├── dich-vu-don-gia/
├── yeu-cau-thay-doi/
├── vang-mat/                # lượt vắng, ngày vắng, quyết định giữ giường, báo vắng, ngày nghỉ bán trú
├── ket-thuc-luu-tru/
└── qua-doi/

mobile/lib/nhan-vien/vang-mat/      # Cho tạm vắng, Gia hạn, Ghi nhận trở về
mobile/lib/nguoi-than/
├── hop-dong/                # người đại diện: hợp đồng, phụ lục, đặt cọc
├── yeu-cau-thay-doi/        # gửi yêu cầu ở Nháp
└── luu-tru/                 # tiến trình tiếp nhận, lượt vắng, tiến trình kết thúc
```

**Structure Decision**: Theo R-02 của 000. Feature 004 nằm ở `backend/…/m02` với mười gói con nghiệp vụ, gói `trangthai` gom các SPI cài cho module thấp hơn, gói `lich` và gói `api`. Web có đủ chức năng. App nhân viên chỉ có các thao tác vắng mặt tại tầng. App người thân có phần xem và gửi yêu cầu của người đại diện.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Bảng `luu_tru_nct` (một dòng mỗi người) | Đích khóa tuần tự cho DBR-06 và cho các lệnh của 004 trên cùng người; giữ dấu "hồ sơ lưu trú đã đóng" (FR-072) | Khóa dòng `nguoi_cao_tuoi` của M01 là truy cập dữ liệu module khác |
| Phiên bản nội dung hợp đồng, cùng cột sao `ngay_ket_thuc_hien_hanh` trên hợp đồng Hiệu lực | FR-024 đòi trả nội dung tại ngày bất kỳ cho 001, 003, 005, 010; DBR-06 cần ngày kết thúc sau gia hạn để kiểm chồng bằng trigger | Ghép phụ lục mỗi lần đọc thì mọi module đọc phải cài lại logic ghép; không có cột ngày kết thúc hiện hành thì trigger không kiểm được |
| Bảng kết quả tính `ngay_vang`, `muc_viec_sau_qua_doi`, được ghi lại khi có quyết định, đính chính hoặc module sở hữu báo | FR-052 đòi lưu kết quả theo ngày, dùng bảng hiện hành tại lúc tính và không đổi khi bảng đổi; FR-071 đòi trạng thái mục tự cập nhật. Mỗi lần ghi lại có nhật ký | Tính lại mỗi lần đọc thì không giữ được "bảng tại lúc tính" |
| Lệnh chuyển trạng thái (hồ sơ chờ, hợp đồng, hồ sơ kết thúc) không dùng `If-Match` | Edge Case "lệnh đến sau được kiểm tra lại trên trạng thái mới" | `If-Match` từ chối cả lệnh còn hợp lệ. Cùng lý do với 001 H-02, 003 G-03 |
| Giao diện bổ sung vào hợp đồng 000 (`dongDoTrangThaiCuoi`), 001 (người gọi `HUY_TIEP_NHAN`; ba giao diện thành SPI), 002 (`KetThucLuuTruApi` thành SPI), 003 (`thuocTinhPhanBo`, `batDauLuotTrong`) | FR-065, FR-070 đóng yêu cầu khi trạng thái cuối; FR-002 Hủy tiếp nhận từ danh sách chờ; FR-012 cần thuộc tính phòng và lượt trống; Modulith cần một chiều phụ thuộc | Không bổ sung thì phải đọc dữ liệu module khác hoặc tạo vòng phụ thuộc |
| Một job hằng ngày lúc 00:15 và bảng `lan_chay_theo_ngay` | FR-077 đòi chạy bù từng ngày theo thứ tự và không trùng. Giờ chạy là thông số vận hành, không có trong Phụ lục 25; chạy sau job áp dụng yêu cầu 00:05 của 000 để ngày vắng dùng nội dung hợp đồng đã áp dụng | Mỗi việc một job riêng thì thứ tự giữa các việc của cùng ngày không bảo đảm |

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX). Đề nghị chủ dự án quyết bằng mã Q mới khi cần, rồi sửa `docs/business/nghiep-vu.md` và spec liên quan.

1. **BR-M02-20 tính giường "Chờ sẵn sàng" vào "lượt trống hiện tại".** *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - BR-M02-20 ghi lượt trống là "một giường Trống hoặc Chờ sẵn sàng chưa có giữ chỗ".
   - Spec 004 FR-012 ghi rõ "giường Chờ sẵn sàng chưa được tính là Trống". BR-M02-01, BR-M03-06, BR-M03-09 cũng chỉ kích hoạt danh sách chờ khi giường về Trống.
   - Plan theo FR-012: lượt trống bắt đầu khi giường về Trống (003 `batDauLuotTrong`).
   - Đề nghị sửa BR-M02-20 bỏ chữ "hoặc Chờ sẵn sàng" (có thể là dấu vết của Q-282).

2. **Nhắc lập yêu cầu thay đổi lưu trú bị mô tả ở ba nơi.** *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - 004 FR-039 ghi "chưa có yêu cầu thì nhắc Hành chính mỗi CFG-M03-02" cho cả đổi mức và chuyển giường gấp.
   - 001 FR-036 đã có nhắc đổi mức (job của 001); 003 FR-030 đã có nhắc sau chuyển gấp (job của 003).
   - Plan không thêm job ở 004 để tránh nhắc trùng. 004 chỉ cài `coYeuCauDoiMucChuaKetThuc` và báo 003 khi yêu cầu được lập.
   - Không đổi nghiệp vụ; đề nghị ghi ở FR-039 rằng việc nhắc do 001, 003 thực hiện.

3. **(Đồng bộ câu chữ spec 004, không đổi nghiệp vụ)** *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - User Story 8 (đoạn mô tả) còn "dừng sinh chi phí"; US8 kịch bản 1 còn "sinh chi phí tự động dừng từ thời điểm qua đời", là cách nói trước Q-306. FR-070 đã viết đúng: các ngày sau ngày qua đời không là sự kiện tính phí.
   - Câu dưới bảng loại yêu cầu "Hai loại sau có ngày hiệu lực là ngày duyệt" không còn rõ là hai loại nào sau khi bỏ "Quyết định giữ giường" (Q-307). Plan hiểu là "Điều chỉnh điểm ưu tiên" và "Ngoại lệ điều kiện kết thúc".
   - Assumptions còn câu dẫn `docs/phan-tich-yeu-cau.md` mục 4.2 (đã bỏ); có thể giữ làm lịch sử.

4. **Lệnh nào của 004 là "đăng ký lịch đến" để kiểm sức chứa khu nghỉ (003 FR-039, BR-M03-03)?** *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - Spec 003 viết "khi đăng ký hoặc thay đổi lịch đến (feature 004/005)". Spec 004 coi lịch đến là nội dung hợp đồng (FR-019) và chỉ nêu việc kiểm khi duyệt yêu cầu thay đổi (FR-037, FR-041).
   - Plan kiểm ở "Ghi nhận đã ký" hợp đồng bán trú và khi duyệt yêu cầu đổi lịch đến hoặc đổi sang bán trú.
   - Số đăng ký của một buổi gồm lịch trong hợp đồng Hiệu lực và lịch của phụ lục chờ hiệu lực (research T-10).
   - Đề nghị xác nhận, hoặc chốt bằng Q nếu muốn kiểm sớm hơn (ví dụ lúc Gửi ký).

5. **Liên quan điểm báo lại 3 của plan 003.** Chuyển giường theo lịch bị hoãn vì giường chưa sẵn sàng làm yêu cầu thay đổi lưu trú đã "Đã áp dụng" (đơn giá mới) trong khi người cao tuổi chưa chuyển. Nếu sau đó việc chuyển không còn hợp lệ, 004 không có trạng thái nào cho phụ lục ngoài Đã áp dụng. Plan 004 giữ cách của plan 003 (Hành chính lập yêu cầu mới); cần chốt chung một mã Q. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

6. **Quyền Kế toán lập ngoại lệ điều kiện (h).** FR-064 cho Kế toán lập yêu cầu ngoại lệ cho điều kiện (h) (Q-222). Nhưng Phụ lục 27 dòng "Kết thúc lưu trú, qua đời" có ô Kế toán là "—", và 19.3 chỉ ghi "quyết toán khi kết thúc lưu trú". Plan cài theo FR-064 bằng một dòng điều chỉnh theo 19.3 trong ma trận của 002. Test `MaTranQuyenKhopNguonTest` của 002 sẽ báo lệch tới khi nguồn có quyền này. Đề nghị thêm chú thích cho ô Kế toán của dòng này ở Phụ lục 27, hoặc ghi vào 19.3. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

7. **Spec 004 chưa nêu mức của phần lớn thông báo (009 FR-043a, Q-108).** Chỉ FR-025b có mức (Trung bình, Q-302). Plan áp nguyên tắc 3 bậc (research T-12): báo cần quyết định giữ giường, hợp đồng quá hạn báo Quản lý viện, đặt cọc về Chưa đáp ứng khi đã lưu trú, không có dòng chính sách ở mức Trung bình; báo giường Trống và các lần nhắc ở mức Nhẹ. Đề nghị ghi vào spec 004 và bảng 009 FR-043b khi đồng bộ. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

8. **(Đồng bộ với plan 000, 001, 002, 003, đã sửa trong plan này)** *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - [../000-shared-foundation/contracts/module-interfaces.md](../000-shared-foundation/contracts/module-interfaces.md): thêm `YeuCauPheDuyetApi.dongDoTrangThaiCuoi`.
   - [../001-elder-profile-assessment/contracts/module-interfaces.md](../001-elder-profile-assessment/contracts/module-interfaces.md):
     - thêm 004 (`HUY_TIEP_NHAN`) vào bảng người gọi `TrangThaiNguoiCaoTuoiApi.thucHien`;
     - ghi `YeuCauDoiMucApi`, `HopDongApi`, `LuotVangApi` là SPI khai báo ở `m01.api`, M02 cài.
   - [../002-account-permission/contracts/module-interfaces.md](../002-account-permission/contracts/module-interfaces.md): ghi `KetThucLuuTruApi` là SPI khai báo ở `m15.taikhoan.api`, M02 cài.
   - [../003-room-bed-allocation/contracts/module-interfaces.md](../003-room-bed-allocation/contracts/module-interfaces.md): thêm `PhongChoPhepApi.thuocTinhPhanBo`, `TinhTrangGiuongApi.batDauLuotTrong`, `ViTriApi.phanBoHienHanhHoacTuongLai`.

9. **(2026-10-18) Q-356 "bắt buộc lý do" đã có sẵn ở 001 FR-046.** Mọi lệnh chuyển trạng thái do người dùng thực hiện, gồm Hoàn tất tiếp nhận, đã bắt buộc lý do. Plan không thêm trường lý do riêng; cảnh báo `NCT_KHONG_DONG_Y_VAO_O` hiện ý kiến để người thực hiện ghi lý do nói tới nó. Nếu ý định là một lý do riêng cho ý kiến "Không đồng ý", cần sửa FR-004a cho rõ (cùng nội dung plan 001 điểm báo lại 12). *(Đã xử lý 2026-10-18: Q-361: lý do của lệnh; thiết kế giữ nguyên, spec FR-004a đã làm rõ)*

10. **(2026-10-22) Spec 004 User Story 4 kịch bản 4 còn chữ "được đảo ở feature 017".** Q-385 bỏ giao dịch đảo (sai sót bằng điều chỉnh có duyệt, 017 FR-011); Clarification 2026-09-28 và bảng nhóm dữ liệu của spec 004 đã sửa nhưng kịch bản 4 chưa. Plan, tasks theo điều chỉnh có duyệt (T039 sửa câu chữ). Đề nghị sửa kịch bản 4 của spec khi đồng bộ. *(Đã xử lý 2026-10-22: spec đã sửa.)*

## Ghi chú triển khai

### Phase 1 (2026-10-25, nhánh `impl/004-admission-residency`)

- **T001**: khung gói `m02` theo cấu trúc trong module của 000 R-02 (không theo `BE/m02/dangky/web/…` của tasks): `api/{dto, spi, events}` (`@NamedInterface` `admission`, `admission-dto`, `admission-spi`, `admission-events`), `web/{request, response}`, `scheduling`, `internal/<chức năng>` với mười chức năng `application`, `waitinglist`, `contract`, `servicecatalog` (không đặt `service` để khỏi trùng tầng), `change`, `absence`, `ending`, `death`, `residencyday`, `transition`. Mỗi gói có `package-info.java`; tầng `service`, `domain`, `persistence`, `config` tạo khi có lớp đầu tiên.
- **T002**: bản giả ở `support/fake/m02/` (`FakeM02Config`, năm bản giả cộng một bean `FakePostDeathItem` mỗi mã) theo glossary mục 10 (không phải `support/gia004/`). Bản giả cần SPI để biên dịch, nên **phần SPI của T003 được kéo lên làm trước**: sáu interface ở `m02.api.spi` (`RepresentativePort`, `PickupRecordPort`, `DepositStatusPort`, `FinalPeriodPort`, `DayCareAttendancePort`, `PostDeathItemPort`) và bảy kiểu ở `m02.api.dto` (`DepositStatus`, `DepositStatusView`, `PickupRecordView`, `Attendance`, `PostDeathItemCode`, `PostDeathItemStatus`, `PostDeathItemStatusView`). T003 vẫn `[ ]`: còn các giao diện đọc (`ContractReadApi`, `UnitPriceApi`, …).
- **Provisional**: tên theo glossary mục 10 chờ A duyệt; chữ ký view (`DepositStatusView`, `PickupRecordView`, `PostDeathItemStatusView`) do 004 tạm đặt tối thiểu vì tài liệu 012, 017 chưa định nghĩa, chờ E (012, 017), D (006, 007), C (005) xác nhận. Đổi tên thì đổi cả bản giả cùng PR ("Đổi api").
- Kiểm: `FakeM02Test` (6), `ApiNamedInterfaceArchTest`, `LayerArchTest` (6), `ModuleBoundaryTest` qua trên JDK 25; `spotless:apply` đã chạy.

### Phase 2 (2026-10-25, nhánh `impl/004-admission-residency`, chưa commit)

- **Xong `[x]`**: T003 (sáu API đọc ở `m02.api`: `ContractReadApi`, `UnitPriceApi`, `ResidencyDayApi`, `AdmissionConditionApi`, `ResidencyStatisticsApi`, `PostDeathTaskApi` cùng các view ở `api.dto`; tên kiểu ở glossary 10.8), T004 (28 sự kiện ở `api.events`; chín sự kiện gửi thông báo cài `HasNotificationRequests`, người nhận và mức theo research T-12), T005 (`V004_01__resident_residency.sql`), T006 (`ResidencyLock`), T007 (`V004_02__service_catalog.sql`), T008 (`UnitPriceService`), T009 (`M02StatusParticipant` `@Order(1)`, `M02DailyRunner`, `M02DailyTask`, `M02DailyJob`), T012 (`BR_M02_19_CatchUpRunIT`).
- **Còn `[ ]` có chủ đích**: T010 (đã cài `ResidencyEndApi` thật qua lịch sử trạng thái của 001; `AbsenceApi`, `ContractApi`, `CareLevelChangeRequestApi` là khung trả rỗng tới khi có bảng của story; `NguonLuuTruChoPhanBo`, `DemLichDenBanTru` chờ C công bố ở m03). T011 (mới có `FieldClassificationProvider` cho bốn loại; `RetentionProvider`, `ScopeAssignmentProvider`, `FilePermissionProvider` cần bảng thực thể nên cài cùng migration `V004_03` → `V004_09` của từng story, vì provider lưu giữ gọi vào bảng chưa có sẽ làm hỏng job loại bỏ dữ liệu của 000).
- **Lệch kỹ thuật**: (1) `daily_run.result` chỉ `SUCCEEDED`, `SKIPPED`; việc lỗi không ghi dòng để làm lại (chạy bù theo thứ tự ngày, ngày lỗi chặn các ngày sau của việc đó trong lần chạy). (2) Hôm nay chỉ xử lý từ 00:15. (3) `validateDetails` của `M02StatusParticipant` đặt trường bắt buộc theo FR-048, FR-062, FR-069 (xem javadoc); bằng chứng qua đời kiểm ở điều kiện vì cần vai trò. (4) Bean thật làm test M01 (`StatusTestBase`) cần gỡ bean `m02StatusParticipant` khỏi context để dùng thân lệnh rỗng. (5) `UnitPriceApi.contractUnitPrice` trả rỗng tới story hợp đồng; `residencyFeeServiceOf`, `facilityMedicationServiceOf` lấy dịch vụ Hiệu lực mã nhỏ nhất vì chưa có ràng buộc duy nhất (cần quyết khi làm story dịch vụ). (6) Hai trigger không chồng khoảng tách `_insert`, `_update`.
- **File chung đã sửa (chủ dự án duyệt 2026-10-25)**: `M02_DAILY` ở `runJobs` của api-000, `TestClockController.TestJob`, `SchedulerScopeArchTest.ALLOWED`, cron `"-"` ở profile test của `application.yml`.
- **T108 (làm sớm một phần, chưa đánh dấu)**: đã có sự kiện `EndingExceptionApproved`; chưa có `ResidencyEndingReadApi`, mã `END_H_SETTLEMENT_CONFIRMATION`, bản giả.
- **Kiểm**: `./mvnw clean verify` trên JDK 25, MySQL 3307: 263 test đơn vị, 898 test tích hợp qua.
## PHASE 2 HANDOFF (2026-10-25)

**Trạng thái Git**
- Nhánh `impl/004-admission-residency`, tạo từ `develop` tại `cc9761f`; đã push lên `origin` (không có PR). Commit code Phase 1: `4e85599` (trên `dd4cf9c` = dịch `api-004.yaml` và glossary mục 10). Commit tài liệu bàn giao này nằm sau `4e85599`.
- Nhánh `docs/004-api-contract` (chỉ `dd4cf9c`) **chỉ ở local, chưa push, chưa PR**; PR hợp đồng đầu tiên của feature (team-plan 3.7) mở khi chủ dự án cho phép. Mô tả PR cần nêu các điểm A quyết định: `Problem` của 004 chưa trùng `Problem` chung của api-000; mã điều kiện `END_*`, `ADM_*` dùng chung với 006, 007, 010, 013, 017; `Deposit.basis.receiptNumber` còn trong hợp đồng dù Q-385 bỏ số phiếu thu; tên 003 (C), sự kiện 017 (E) chờ chốt; việc cho A: thêm `'004'` vào `TRANSLATED` của `frontend/scripts/gen-api.mjs`, `M02_DAILY` vào `runJobs` của api-000, đăng ký api-004 vào `ApiContractIT`.
- Stash `CP-001-P2-IMPL` (của 001) giữ nguyên, không đụng. Nhánh 001 không sửa.

**Phase 1 (xong)**
- T001 `[x]`: khung gói M02 (mục "Ghi chú triển khai" ở trên).
- T002 `[x]`: bản giả `support/fake/m02/`. 14 test qua trên JDK 25 (`FakeM02Test` 6, `LayerArchTest` 6, `ApiNamedInterfaceArchTest` 1, `ModuleBoundaryTest` 1).

**T003 đã làm trước một phần** (T003 vẫn `[ ]`)
- Đã có: sáu SPI ở `m02.api.spi` (`RepresentativePort`, `PickupRecordPort`, `DepositStatusPort`, `FinalPeriodPort`, `DayCareAttendancePort`, `PostDeathItemPort`) và bảy kiểu ở `m02.api.dto`. Tên và chữ ký view là **provisional**.
- Còn thiếu của T003: `ContractReadApi`, `UnitPriceApi`, `ResidencyDayApi`, `AdmissionConditionApi`, `ResidencyStatisticsApi` (016), `PostDeathTaskApi` và các view của chúng (glossary mục 10.5; chữ ký ở contracts/module-interfaces.md mục 1.2 → 1.6). `PickupRecordPort.markUsed` cần ghi `Propagation.MANDATORY` ở bản cài.

**Task Phase 2 còn lại** (theo tasks.md; làm đúng thứ tự, test viết trước)
- T003 (phần còn lại), T004 (sự kiện `m02.api.events`, mục 1.8, `package-info` đã có), T005 (`V004_01__resident_residency.sql`: `resident_residency`, `daily_run`, `m02_reminder`), T006 (`ResidencyLock`), T007 (`V004_02__service_catalog.sql`: `service`, `price_version`, hai trigger), T008 (`UnitPriceService`), T009 (`M02StatusParticipant` khung `@Order(1)`, `M02DailyJob` khung), T010 (SPI M02 cài cho module thấp hơn: `ContractApi`, `AbsenceApi`, `CareLevelChangeRequestApi` của 001 và `ResidencyEndApi` của 002 đã có trên develop), T011 (bốn provider cho 000, 002), T012 (`BR_M02_19_CatchUpRunIT`, test này cần MySQL).
- Bổ sung 2026-10-22 làm sớm: T108 (`HoSoKetThucDocApi`, sự kiện `NgoaiLeKetThucDaDuyet`, mã `END_H_SETTLEMENT_CONFIRMATION`) vì 017 cần.
- Quy ước tên: theo glossary mục 10 (ví dụ `ResidencyLock` ở `internal.transition`); migration đặt tên theo glossary 10.6 (`V004_01__resident_residency.sql`…).

**Phụ thuộc A/C/D/E**
- A (000, 002): `ApprovalRequestApi`, `RequestTypeHandler` (đã có `approverRoleRestriction` cho thay đổi nhỏ), `CorrectableRecordProvider`, `RetentionProvider`, `FieldClassificationProvider`, `ScopeAssignmentProvider`, `FilePermissionProvider`, `ParameterApi` đã có trên develop. Thêm job `M02_DAILY` cần sửa file chung (`runJobs` api-000, `TestClockController.TestJob`, `SchedulerScopeArchTest.ALLOWED`, cron `"-"` ở profile `test`): làm như 001 đã làm, xin chủ dự án cho phép, ghi vào team-plan mục 8 cho A.
- C (003): `m03` còn rỗng trên develop; chưa có `NguonLuuTruChoPhanBo`, `DemLichDenBanTru`, `PhanBoGiuongLenhApi`, `SucChuaBanTruApi`. Phần T010 liên quan hai SPI đầu và mọi gọi sang 003 (Phase 4 trở đi) bị chặn tới khi C công bố; ghi blocker, không tự khai báo thay C. Khóa ngoại tới bảng module khác chỉ thêm khi module đó vào develop (team-plan 3.5).
- D (005, 006, 007) và E (010, 012, 013, 017): chưa có bản thật của các SPI M02 khai báo; dùng bản giả `FakeM02Config`. Chữ ký `DepositStatusView`, `PickupRecordView`, `PostDeathItemStatusView` chờ E, D, C xác nhận; đổi thì sửa bản giả cùng PR ("Đổi api") và báo họ ở team-plan mục 8.
- 001 (B, đã push nhưng chưa merge vào develop): 004 chỉ dùng `m01.api` đã có trên develop; không lấy code 001 chưa merge.

**Quy định MySQL và môi trường**
- Chỉ dùng MySQL cổng **3307** cho `*IT`. Biến môi trường vĩnh viễn của máy (`TEST_DB_URL`, `DB_URL`) trỏ cổng 3306 (MySQL84, dev DB của B): **luôn đặt `TEST_DB_URL`, `DB_URL` về `jdbc:mysql://localhost:3307/...` trong phiên chạy** và kiểm tra instance 3307 đang chạy trước. `MySqlIntegrationTestBase` xóa và tạo lại schema test cùng hai tài khoản `eldercare_app`, `eldercare_retention`; không chạy trên 3306 và không đụng dữ liệu demo.
- Máy không có Python và không có `gh`; commit bằng `git commit -F <file>` (PowerShell 5.1); JDK 25; chạy test nhẹ không cần DB: `./mvnw test -Dtest=...`.

**Excel và quy trình**
- Chỉ cập nhật file `C:\Users\Admin\Documents\ElderCare_ThanhVienB\CHECKLIST_ELDER_CARE_THANH_VIEN_B.xlsx` (qua Excel COM); không tạo bản sao, backup. Cập nhật `FEATURE_004`, `PHASES`, `DASHBOARD`, `TEST_LOG`, `GIT_CHECKPOINT` đến đâu làm đến đó.
- Chưa tạo PR, chưa merge vào develop; hết Phase 2 báo checkpoint rồi mới sang Phase 3. Trước khi làm giao diện web hoặc mobile, trình bày bố cục để chủ dự án duyệt.

## PHASE 3 HANDOFF (2026-10-25)

**Trạng thái Git**
- Nhánh `impl/004-admission-residency`, Git sạch. Commit Phase 2 (trên `97d0c97`): `df7d335` (api đọc và sự kiện, T003, T004), `be2c73e` (khóa, migration, đơn giá, T005 → T008), `da5c52b` (bean trạng thái, job hằng ngày, SPI cho module thấp, T009 → T012), `347bf79` (`M02_DAILY` ở file chung), `4d41347` (tài liệu: tasks, plan, glossary 10.8, team-plan). Commit bàn giao này nằm sau `4d41347`; HEAD cuối ghi ở báo cáo của session.
- Chưa push, chưa PR. Nhánh `docs/004-api-contract` (chỉ `dd4cf9c`) vẫn chỉ ở local (xem PHASE 2 HANDOFF). Stash `CP-001-P2-IMPL` của 001 giữ nguyên, không đụng.
- Kiểm cuối Phase 2: `./mvnw clean verify` JDK 25, MySQL 3307: 263 test đơn vị + 898 test tích hợp qua.

**Trạng thái task**
- `[x]`: T001, T002, T003, T004, T005, T006, T007, T008, T009, T012.
- `[ ]` còn thiếu: **T010** (xong `ResidencyEndApi`; `AbsenceApi`, `ContractApi`, `CareLevelChangeRequestApi` là khung trả rỗng; thiếu hai SPI 003 `NguonLuuTruChoPhanBo`, `DemLichDenBanTru`), **T011** (xong `FieldClassificationProvider`; thiếu `RetentionProvider`, `ScopeAssignmentProvider`, `FilePermissionProvider`, cài cùng migration của từng story vì cần bảng thực thể).
- **T108** làm một phần: có sự kiện `EndingExceptionApproved`; thiếu `ResidencyEndingReadApi`, mã `END_H_SETTLEMENT_CONFIRMATION`, bản giả bean 017.

**Đã có trong mã (đường dẫn gốc `backend/src/main/java/com/cambae/backend/m02/`)**
- `api/`: `ContractReadApi`, `UnitPriceApi`, `ResidencyDayApi`, `AdmissionConditionApi`, `ResidencyStatisticsApi`, `PostDeathTaskApi`; `api/dto` (16 view/enum, tên ở glossary 10.8); `api/spi` (6 SPI M02 khai báo, từ Phase 1); `api/events` (28 sự kiện, 9 sự kiện gửi thông báo qua `HasNotificationRequests`, người nhận Hành chính hoặc Quản lý viện).
- Migration: `V004_01__resident_residency.sql` (`resident_residency`, `daily_run`, `m02_reminder`), `V004_02__service_catalog.sql` (`service`, `price_version`, trigger không chồng khoảng và bất biến). Migration kế tiếp là `V004_03` (T015). Không sửa V004_01/02 (đã vào lịch sử commit).
- `internal/transition/service/`: `ResidencyLock` (`lock(residentId)`, `MANDATORY`), `M02StatusParticipant` (khung: `@Order(1)`, bảy lệnh, `validateDetails`; `checkConditions` trả rỗng, `applyEffects` rỗng, chưa có `warnings`, `waiveConditions`), `M02DailyRunner` + `M02DailyTask` (chạy bù theo ngày, `latestDayOnly`), `ResidencyEndLookup`, `AbsenceLookup`, `ContractLookup`, `CareLevelChangeRequestHandler`; `internal/transition/config/M02FieldClassificationConfig`; `internal/servicecatalog/service/UnitPriceService` (`UnitPriceApi`); `scheduling/M02DailyJob` (00:15 và khi khởi động, ShedLock `M02_DAILY`).
- Bản giả: `src/test/java/com/cambae/backend/support/fake/m02/` (`FakeM02Config` và 6 bản giả SPI).
- File chung đã sửa với chủ dự án duyệt (2026-10-25): `M02_DAILY` ở `runJobs` của api-000, `TestClockController.TestJob`, `SchedulerScopeArchTest.ALLOWED`, `application.yml` profile test (`m02-daily` cron `"-"`, `on-startup: false`); dòng cho A ở team-plan mục 8.

**Cách đăng ký việc hằng ngày** (cho T016 và các story sau): bean `M02DailyTask` (`name` là một trong `CALCULATE_SCORES`, `ABSENCE_DAYS`, `CONTRACTS`, `ENDINGS`, `REMINDERS`; `@Order` quyết thứ tự trong ngày; `latestDayOnly()` cho việc chỉ có nghĩa ngày mới nhất). Test gọi `M02DailyRunner.catchUp()`. Khi mỗi story thêm bean thật, `BR_M02_19_CatchUpRunIT` vẫn chạy cùng chúng (dùng tên việc `TEST_*` riêng).

**Blocker và phụ thuộc**
- **Flyway**: V004 nằm giữa V002 và V009 đã có trên develop. DB dev đã áp V009 mà chạy không `out-of-order` sẽ báo lệch; profile `demo` và test (schema tạo mới) không bị. Cân nhắc trước khi chạy trên DB 3306.
- **A**: `'004'` vào `TRANSLATED` của `frontend/scripts/gen-api.mjs`; đăng ký api-004 vào `ApiContractIT`; sửa `Problem` của api-004 cho trùng `Problem` chung (khi mở PR hợp đồng); `M02_DAILY` đã sửa, chờ A merge.
- **C (003)**: m03 còn rỗng; chưa có `NguonLuuTruChoPhanBo`, `DemLichDenBanTru`, `PhanBoGiuongLenhApi`, `SucChuaBanTruApi`. Phase 3 (US1) không cần các giao diện này trực tiếp, nhưng điều kiện "chưa có phân bổ giường" của FR-021/FR-076 và Phase 4 trở đi cần. Không tự khai báo thay C (BL-56).
- **E (012, 017), D (005, 006, 007)**: xác nhận chữ ký `DepositStatusView`, `PickupRecordView`, `PostDeathItemStatusView` và tên 6 SPI (BL-57). `RepresentativePort` (012) dùng cho điều kiện (f) DBR-02 ở US1: dùng bản giả `FakeRepresentatives`. `DepositStatusPort` (017) dùng cho điều kiện đặt cọc.
- **001 (B, đã vào develop)**: dùng `ResidentStatusApi.previewConditions`, `NguonYKienApi`/`OpinionSourceApi` (kiểm nguồn ý kiến), `ResidentApi`. Kiểm tên thực tế của giao diện 001 trong `m01.api` trước khi viết T016/T016a (tên trong tasks là tên logic cũ `xemDieuKien`, `NguonYKienApi.kiemTra`).

**Quyết định chưa chốt (ghi để hỏi chủ dự án khi gặp, không đoán)**
- Trường `details` bắt buộc của `M02StatusParticipant.validateDetails` suy từ FR-048/062/069 vì api-001 không định nghĩa; 001 chưa xác nhận.
- `residencyFeeServiceOf`, `facilityMedicationServiceOf` chưa có ràng buộc duy nhất (lấy dịch vụ Hiệu lực mã nhỏ nhất).
- Tên loại đối tượng nhật ký của phụ lục `M02.ADDENDUM` là tên tạm.
- Khoá `ResidencyLock` trong bean tham gia lệnh trạng thái: thứ tự (001 khóa `resident` trước, rồi 004) chưa quyết nơi gọi `lock` (checkConditions chạy chỉ đọc); quyết khi làm T017.
- Điểm báo lại 4 (lịch đến bán trú), 6 (quyền Kế toán lập ngoại lệ (h)) của plan đã xử lý ở spec; Q-387 ảnh hưởng T109 (Phase 10).

**Phase 3 (US1, T013 → T019), thứ tự làm**
1. T013, T014 (test trước, phải đỏ): `LuotDangKyIT`, `CapNhatLuotDangKyIT`, `TienTrinhIT`, `FR_004a_YKienVaoOIT`, `FR_004_DanhSachKhopLenhIT`, `BR_M02_22_MucHopDongIT`, `FR_076_CanhBaoChuaVaoOIT`. Có thể bắt đầu ngay các phần không cần C: lượt đăng ký, ý kiến, tiến trình, điều kiện (a)-(f) với bản giả 012/017.
2. T015: `V004_03__admission_application.sql` (`admission_application`, `admission_opinion`; tên cột theo glossary 10.6; trigger ý kiến chỉ cho `ACTIVE → SUPERSEDED`, không xóa; nhóm 2/3 theo data-model mục 3).
3. T016, T016a: `AdmissionApplicationService`, `AdmissionConditionService` (cài `AdmissionConditionApi`), `AdmissionOpinionService`; việc nhắc FR-076 qua `M02DailyTask` `REMINDERS`.
4. T017: hoàn thiện `M02StatusParticipant` cho `COMPLETE_ADMISSION`, `CANCEL_ADMISSION` (điều kiện, cảnh báo `ADMISSION_OPINION_MISSING`, `RESIDENT_DISAGREES_WITH_ADMISSION`, tác động, sự kiện `AdmissionCompleted`). Phần phụ thuộc hợp đồng (Phase 5) và hồ sơ chờ (Phase 4) dùng bản giả hoặc để trống có chủ đích. Sửa/thêm test của `StatusTestBase` nếu cần; bean thật đã bị gỡ khỏi context test M01.
5. T018: `AdmissionApplicationController` theo api-004 (đường dẫn glossary 10.1); web T019 chỉ làm sau khi chủ dự án duyệt bố cục (DESIGN.md); mobile T019 người thân chưa mở.
6. Cùng migration của US1 cài `RetentionProvider`, `ScopeAssignmentProvider` cho `M02.ADMISSION_APPLICATION` (phần của T011), ghi vào plan.

**Quy định**
- MySQL: chỉ cổng 3307. Instance chạy bằng `mysqld.exe --no-defaults --datadir=<...>\mysql3307\data --port=3307 --mysqlx=OFF --skip-log-bin --max_connections=400` (datadir ở scratchpad phiên cũ, xem memory `reference-mysql-3307-test`); đặt `TEST_DB_URL`, `DB_URL` về `jdbc:mysql://localhost:3307/...` trong phiên (biến máy trỏ 3306). Không chạy `*IT` trên 3306.
- TDD: test trước, thấy đỏ rồi mới cài; tên test bắt đầu bằng mã quy tắc; chỉ `[x]` khi đã chạy xanh. Test `*IT` kế thừa `MySqlIntegrationTestBase`; schema dùng chung nên test tự dọn dữ liệu (tài khoản app không có `DELETE`, dùng `adminConnection()`).
- Modulith: module khác chỉ qua `m02.api`; `ModuleBoundaryTest`, `LayerArchTest`, `ApiNamedInterfaceArchTest`, `SchedulerScopeArchTest`, `RequiresPermissionArchTest` phải qua; thời gian lấy từ `BusinessTime`; không tự sinh bản ghi (job chỉ chuyển trạng thái, nhắc, báo).
- Quyền sở hữu file: chỉ sửa `m02`, `V004_*`, glossary mục 10, `specs/004-*`. File chung (`runJobs`, `TestClockController`, `application.yml`, `SchedulerScopeArchTest`, `ApiContractIT`, `gen-api.mjs`, `pom.xml`) cần chủ dự án duyệt trước. Không sửa code của A/C/D/E; bản giả của họ ở `support/fake/<module>/`.
- Git: không commit, push, PR, merge khi chưa được yêu cầu; commit bằng `git commit -F <file>` (PowerShell 5.1; máy không có `gh`).
- Excel: chỉ cập nhật `C:\Users\Admin\Documents\ElderCare_ThanhVienB\CHECKLIST_ELDER_CARE_THANH_VIEN_B.xlsx` (Excel COM, script ASCII, chuỗi Việt từ JSON UTF-8); không tạo bản sao hay backup.

## Ghi chú triển khai Phase 3 (US1, T013 → T018, 2026-10-25)

- Đã cài: `V004_03` (`admission_application`, `admission_opinion`, trigger, quyền cột), `m02/internal/application/{domain,persistence,service}`, `AdmissionApplicationController` (đủ 7 endpoint api-004 mục A), `M02StatusParticipant` cho `COMPLETE_ADMISSION` (điều kiện (b), (c), (e), cảnh báo ý kiến, tác động lên lượt, sự kiện `AdmissionCompleted`) và `CANCEL_ADMISSION` (lượt Đã hủy). Việc nhắc FR-076 qua `M02ReminderTask` + `ReminderRule` (mỗi story sau thêm một `ReminderRule`). Glossary mục 10.9.
- T011: đã thêm `ScopeAssignmentProvider` và `RetentionProvider` cho `M02.ADMISSION_APPLICATION`; `referencesTo` rỗng cho tới khi có hồ sơ chờ (Phase 4).
- Test: 7 lớp IT mới ở `m02` (44 test) xanh trên MySQL 3307; `clean verify` 944 test, chỉ `ApiContractIT.R_14` đỏ vì api-004 chưa đăng ký (file chung, chờ chủ dự án duyệt).
- Giả định chờ xác nhận: (1) hợp đồng đọc qua `ContractReadApi` bằng `ObjectProvider`; chưa có bản cài (Phase 5) thì (b), (c), (e) Chưa đạt, test dùng `FakeContractReads`; (2) điều kiện lệnh không thuộc 001, 004, 012 gom vào dòng (d) của 003 (mã thật của C chưa có); (3) FR-076 cảnh báo khi hôm nay > ngày bắt đầu (đúng ngày bắt đầu chưa báo); (4) Hoàn tất tiếp nhận khi người cao tuổi không có lượt mở: không có gì để chuyển, vẫn cảnh báo thiếu ý kiến; (5) `cursor` của danh sách là mã dòng cuối trang trước (hợp đồng trả mảng trơn); (6) mã lỗi `RESIDENT_NOT_ADMISSION_IN_PROGRESS`, `APPLICATION_CLOSED`, `APPLICATION_DATE_LOCKED` do 004 đặt.
- Chưa làm: T019 (web cần duyệt bố cục; mobile người thân chưa mở); T010, T011 (phần còn lại), T108 vẫn mở; không đổi chữ ký provisional.

## PHASE 4 HANDOFF (2026-10-25)

**Git**: nhánh `impl/004-admission-residency`; checkpoint trước Phase 3 `31c1be6`; commit code Phase 3 `a983748`; commit tài liệu handoff nằm sau đó (xem `git log`). Chưa push/PR/merge. Stash `CP-001-P2-IMPL` giữ nguyên.

**Task**: `[x]` T013, T014, T015, T016, T016a, T017, T018. `[ ]` T019 (web cần chủ dự án duyệt bố cục DESIGN.md; mobile người thân chưa mở; chưa viết dòng nào). Còn mở từ Phase 2: T010 (thiếu hai SPI của 003 và `AbsenceApi`, `ContractApi`, `CareLevelChangeRequestApi` còn khung), T011 (xong `FieldClassificationProvider` + `ScopeAssignmentProvider`, `RetentionProvider` cho `M02.ADMISSION_APPLICATION`; còn `FilePermissionProvider` và các loại dữ liệu của story sau), T108.

**Đã viết**: migration `V004_03`; `m02/internal/application/{domain,persistence,service}`; `m02/web/AdmissionApplicationController` + request/response; `M02StatusParticipant` (COMPLETE/CANCEL_ADMISSION); `ReminderRule`/`ReminderLog`/`M02ReminderTask` (mỗi story sau thêm một `ReminderRule`); test `m02/{AdmissionApplicationIT, UpdateAdmissionApplicationIT, ProgressIT, FR_004a_AdmissionOpinionIT, FR_004_ConditionsMatchCommandIT, BR_M02_22_ContractCareLevelIT, FR_076_NotYetAdmittedWarningIT, M02TestBase}`, bản giả `support/fake/m02/FakeContractReads`. Glossary mục 10.9.

**Kết quả test đã xác nhận (MySQL 3307)**: 7 lớp IT trên xanh (44 test; riêng `FR_004a_AdmissionOpinionIT` 11/11 sau khi sửa). `clean verify` đầy đủ chạy trước lần sửa cuối: 944 test, đỏ `ApiContractIT.R_14` (api-004 chưa đăng ký) và 1 test của tôi (đã sửa, chạy lại riêng thì xanh). **Chưa chạy lại `clean verify` đầy đủ sau lần sửa cuối.** `M02StatusParticipantTest` (đã sửa constructor) chưa chạy riêng; ArchTest không báo lỗi trong lần chạy đầy đủ.

**Blocker**: (1) đăng ký api-004 vào `ApiContractIT` (file chung, cần chủ dự án duyệt; thêm `Contract` với `pending` là endpoint chưa cài, `checkOperations=false` nếu còn lệch); (2) Phase 5 hợp đồng: `ContractReadApi` chưa có bản cài nên (b), (c), (e) luôn Chưa đạt ngoài test, `FakeContractReads` cần thay/chạy lại; (3) mã điều kiện giường của C; (4) 017 chưa cài `DepositStatusPort`.

**Giả định chờ xác nhận**: xem "Ghi chú triển khai Phase 3" phía trên.

**Cho session mới**: chạy với `TEST_DB_URL`, `DB_URL` trỏ 3307 (xem memory `reference-mysql-3307-test`); chạy `./mvnw clean verify` (phải `clean` vì IDE có thể để class cũ ở target); xử lý blocker (1) khi được duyệt; rồi Phase 4 (US2, hồ sơ chờ, `V004_04`, T020 trở đi) — không làm lại Phase 3. Khi Phase 4 có hồ sơ chờ: cập nhật `AdmissionApplicationRetention.referencesTo`, trạng thái `WAITING`, khóa ngày đăng ký (BR-M02-15 hiện chỉ dựa vào `WAITING`).

**Cập nhật cuối Phase 3 (2026-10-25, sau chốt handoff)**: blocker (1) đã xử lý — chủ dự án duyệt đăng ký api-004 vào `ApiContractIT` (`backend/src/test/java/com/cambae/backend/common/ApiContractIT.java`, file của A cần review: thêm một `Contract` với `pending` là 55 endpoint chưa cài, `checkOperations = true`, không tắt kiểm tra). Để khớp hợp đồng: khai phản hồi 201/403/409/412/422 ở controller; `POST …/update` trả 200 không thân (đúng hợp đồng; ETag mới ở header); `admissionOpinion` của danh sách điều kiện kiểu tự do; schema nguồn ý kiến của 004 tên `AdmissionOpinionSource` (tên `OpinionSource` đã bị 001 chiếm); schema `Problem` của `api-004.yaml` đổi cho trùng `Problem` chung (như api-002). **`./mvnw clean verify` đầy đủ trên MySQL 3307: 944 test, 0 lỗi, BUILD SUCCESS** (`ApiContractIT`, `M02StatusParticipantTest` xanh). Blocker (1) hết; còn (2), (3), (4).

## Ghi chú triển khai Phase 4 (US2, T020, T022 → T024, 2026-10-25)

- **Đã cài** (nhánh `impl/004-admission-residency`, chưa commit): `V004_04__waiting_list.sql` (`waiting_list_entry` có cột sinh `UNIQUE` cho hồ sơ mở mỗi người và giường giữ, `CHECK` Đã liên hệ phải có giường; `waiting_list_circumstance`; `score_history`, `bed_contact` nhóm 3 chỉ thêm; trigger chặn sửa hồ sơ ở trạng thái cuối, chỉ job loại bỏ xóa); `m02/internal/waitinglist/{domain,persistence,service}`; `WaitingListController` (chín endpoint của api-004, tag `waiting-list`); `WaitingListTransition` do `M02StatusParticipant` gọi cho `CANCEL_ADMISSION`; việc `CALCULATE_SCORES` (`latestDayOnly`, theo research T-11) và nhắc `WAITING_ENTRY_UPDATE`; `AdmissionApplicationRetention.referencesTo` trả hồ sơ chờ của lượt, `WaitingListEntryRetention`, `WaitingListScopeAssignment`; lượt đăng ký hiện `waitingListEntryId`. Tên ở glossary 10.10.
- **Quyết định kỹ thuật** (chờ chủ dự án duyệt cùng PR):
  1. Bảng CFG-M02-08 chưa có cấu trúc cho phần điểm theo mức và tình huống (mặc định `null`): đặt `careLevelPoints {BASIC, REGULAR, SPECIAL}` và `specialSituationPoints {mã → điểm}`; `null` là chưa đặt, tính 0 điểm. Tình huống chọn được là khóa của `specialSituationPoints`.
  2. Chỉ tình huống Đã xác nhận được tính điểm; tình huống Chờ xác nhận không tính cho tới khi Hành chính xác nhận qua `update-circumstances` (lệnh gửi toàn bộ danh sách sau sửa).
  3. `propose-score-adjustment` lập và gửi duyệt ngay (trạng thái Chờ duyệt) vì kịch bản US2 #3 đòi "đề nghị ở Chờ duyệt"; hợp đồng chỉ ghi "gửi duyệt và duyệt qua api-000".
  4. Điều chỉnh đã duyệt cộng dồn vào `score_breakdown.adjustment`; tính hằng ngày giữ nguyên tổng này.
  5. Tính điểm hằng ngày chỉ ngày mới nhất (ngày lỡ ghi `SKIPPED`), đúng research T-11 và chú thích `M02DailyTask`; điểm đổi vì mức chăm sóc (`CareLevelChanged`) và vì bảng CFG-M02-08 (`ParameterChanged`) tính ngay, nghe bằng `@ApplicationModuleListener`; listener tham số đọc bảng qua `ParameterApi.readAt(now)` (không dùng bộ đệm, không dùng giá trị trong sự kiện) để sự kiện tới muộn không ghi đè bảng mới hơn.
  6. Nhắc `WAITING_ENTRY_UPDATE`: mỗi ngày một lần cho hồ sơ Đang chờ có mốc cập nhật cũ hơn CFG-M02-03; xác nhận còn nhu cầu hoặc sửa tình huống đặt lại mốc; không tự hủy hồ sơ.
  7. Từ chối, Hủy chờ: `ResidentStatusApi.execute(CANCEL_ADMISSION, details {waitingListEntryId, waitingListCommand})`; Hủy tiếp nhận đến từ 001 thì hồ sơ mở của người đó thành Hủy chờ, không gọi lại.
  8. `MatchingEntryService` nhận điều kiện phòng dạng `MatchCriteria(residenceTypes, careLevels, genders)` và đầu lượt trống do người gọi truyền, nên 004 không khai báo thay giao diện của 003. Chưa có endpoint.
- **Chưa làm, có chủ đích**:
  - Chờ 003 công bố giao diện (BL-56): `record-contacted` (cần `giuChoHoSoCho`), `decline-bed` (`huyGiuCho`), `GET /beds/{bedId}/matching-entries` (cần `thuocTinhPhanBo`, `batDauLuotTrong`), listener `GiuongDaTrong` và sự kiện `BedAvailableForWaitingList`, `hoSoChoHetHanGiuCho`, `daPhanBo`; nhắc `REALLOCATION` còn cần hợp đồng (Phase 5). Vì vậy T021, T025, T026, T027 còn `[ ]`; ba endpoint này nằm lại ở `pending` của `ApiContractIT`.
  - ~~Đánh dấu sẵn tình huống "từng lưu trú tại viện" (FR-007)~~ — **đã làm (2026-10-25, chủ dự án cho phép thêm hàm đọc tối thiểu)**: `ResidentApi.previousProfileId(residentId): Optional<Long>` (bản cài `ResidentApiImpl` của 001, bản giả `FakeResidents.previousProfile`). `WaitingListService.create` đánh dấu `PREVIOUSLY_RESIDED` ở `PENDING_CONFIRMATION` khi hồ sơ trước đang `RESIDENCY_ENDED` (hồ sơ trước Hủy tiếp nhận hoặc Qua đời thì không đánh dấu, đúng spec "Kết thúc lưu trú"); mã này không phụ thuộc bảng CFG-M02-08 (chưa có mã trong bảng thì chưa tính điểm, không tự đặt điểm); Hành chính xác nhận hoặc bỏ bằng `update-circumstances` kèm lý do. Test `FR_007_PreviousResidencyIT` (5). A xem hàm mới ở `ResidentApi` (team-plan mục 8).
  - Hoàn tất tiếp nhận khi hồ sơ chờ còn mở: bình thường 003 gọi `daPhanBo` trước; chưa có 003 nên `AdmissionTransition.completed` chưa đóng hồ sơ chờ (không có trong spec).
  - T028 (web), T019 (web, mobile): chờ chủ dự án duyệt bố cục.
- **Kiểm chứng** (MySQL 3307, JDK 25): `./mvnw clean verify` BUILD SUCCESS, 271 test đơn vị + 999 test tích hợp, 0 lỗi, 0 bỏ qua (chạy lại sau khi thêm `ResidentApi.previousProfileId`). Test mới: `FR_007_PreviousResidencyIT` (5), `BR_M02_10_PriorityScoreTest` (8), `WaitingListEntryIT` (15), `DailyScoreIT` (7), `ScoreAdjustmentIT` (9), `BR_M02_01_MatchingEntriesIT` (6), `FR_002_DeclineCancelAdmissionIT` (6), `BR_M02_03_WaitingEntryReminderIT` (4), `WaitingListRetentionIT` (3). `ApiContractIT` xanh sau khi bớt chín endpoint khỏi `pending`.

## PHASE 5 HANDOFF (2026-10-25)

**Git**: nhánh `impl/004-admission-residency`; checkpoint trước Phase 4 `cd5febf`. Commit Phase 4: `fec5556` (`ResidentApi.previousProfileId`), `09f7c93` (V004_04 và mã danh sách chờ), `6beb4a0` (test), `21f6d6a` (`ApiContractIT`), `3a62c0e` (tài liệu, đề xuất bố cục DESIGN.md); commit handoff nằm sau đó (xem `git log`). Đã push nhánh này; không PR, không merge. Stash `CP-001-P2-IMPL` giữ nguyên. Không làm lại Phase 1–4.

**Task Phase 4**: `[x]` T020, T022, T023, T024. `[ ]` có chủ đích: T021 (đã làm `BR_M02_01_MatchingEntriesIT`; còn `FR_012_LuotTrongIT`, `BR_M02_02_GiuChoIT`, `GhiNhanLienHeDongThoiIT`), T025 (còn ghi nhận đã liên hệ, từ chối giường, listener `GiuongDaTrong`, `hoSoChoHetHanGiuCho`, `daPhanBo`), T026 (9/12 endpoint; thiếu `GET /beds/{bedId}/matching-entries`, `record-contacted`, `decline-bed`), T027 (còn nhắc `REALLOCATION`) — tất cả chờ 003 (C) công bố giao diện giường (BL-56, BL-59). T019, T028 (web): chưa viết mã, đề xuất bố cục đã nằm ở DESIGN.md mục 14 "Đề xuất bố cục chờ duyệt" (chờ chủ dự án chọn 3 điểm cuối đề xuất). Còn mở từ trước: T010, T011 (phần còn lại), T108.

**Đã triển khai**: migration `V004_04__waiting_list.sql`; `m02/internal/waitinglist/{domain,persistence,service}` (`WaitingListService`, `PriorityScoreCalculator`, `PriorityScoreTable`, `ScoreRecalculation`, `CalculateScoresTask`, `WaitingListScoreListeners`, `ScoreAdjustmentRequestHandler`, `WaitingListTransition`, `WaitingEntryUpdateReminder`, `MatchingEntryService`, `WaitingListEntryRetention`, `WaitingListScopeAssignment`); `WaitingListController` (9 endpoint); `AdmissionApplicationRetention.referencesTo` trả hồ sơ chờ; `M02StatusParticipant` gọi `WaitingListTransition`; "Đổi api" `ResidentApi.previousProfileId` (đánh dấu `PREVIOUSLY_RESIDED`, FR-007). Test: `BR_M02_10_PriorityScoreTest` (8), `WaitingListEntryIT` (15), `DailyScoreIT` (7), `ScoreAdjustmentIT` (9), `BR_M02_01_MatchingEntriesIT` (6), `FR_002_DeclineCancelAdmissionIT` (6), `BR_M02_03_WaitingEntryReminderIT` (4), `WaitingListRetentionIT` (3), `FR_007_PreviousResidencyIT` (5). Glossary mục 10.10; chi tiết quyết định ở "Ghi chú triển khai Phase 4".

**Kết quả**: `./mvnw clean verify` (MySQL 3307, JDK 25) BUILD SUCCESS: 271 test đơn vị + 999 test tích hợp, 0 lỗi, 0 bỏ qua; ModuleBoundaryTest, LayerArchTest, RequiresPermissionArchTest, ApiContractIT xanh.

**Blocker / chờ duyệt**: C (003): BL-56, BL-59 (giao diện giường, `GiuongDaTrong`, giữ chỗ). A: xem `ApiContractIT` (bớt 9 endpoint khỏi `pending`) và `ResidentApi.previousProfileId` (team-plan mục 8). Chủ dự án: BL-61 (cấu trúc CFG-M02-08, chỉ tình huống Đã xác nhận được tính, điều chỉnh điểm gửi duyệt ngay, tính điểm ngày mới nhất), BL-62 (bố cục web T019, T028). Ngoài ra Phase 5 cần: `ContractReadApi` đang là khung (điều kiện (b), (c), (e) luôn Chưa đạt ngoài test, `FakeContractReads`), `NguoiDaiDienPort` (001/012), `SucChuaBanTruApi` (003), 017 `DepositStatusPort` (Phase 6).

**Kế hoạch Phase 5 (US3, T029 → T038)**: (1) viết test T029 → T031 trước (`HopDongVongDoiIT`, `HopDongKhoaNoiDungDbIT`, `DBR_06_HopDongKhongChongIT`, `FR_021_GhiNhanDaKyIT`, `XacDinhKhacChuanTest`, `DuyetDieuKhoanIT`, `BR_M02_09_NoiDungTaiNgayIT`, `BR_M02_05_NhacHopDongIT`, `FR_029_QuaHanHopDongIT`; đặt tên tiếng Anh theo glossary mục 10, bổ sung glossary trước); (2) T032 `V004_05` (hợp đồng, trigger khóa nội dung ngoài Nháp, trigger chống chồng Hiệu lực, phiên bản nội dung); (3) T033 `ContractService`, T034 xác định khác chuẩn và handler `M02.…` duyệt điều khoản (FR-025a), T035 `HopDongDocApi`/`HopDongApi` (nội dung tại ngày D); (4) T036 việc `CONTRACT` và nhắc hợp đồng (`ReminderRule` mới, quá hạn CFG-M02-10); (5) T037 controller; (6) T038 web chỉ sau khi chủ dự án duyệt bố cục. Khóa theo research T-03 (người cao tuổi → `resident_residency` → hàng 004). Sau khi hợp đồng có bản cài thật, thay `FakeContractReads` và chạy lại `BR_M02_22_ContractCareLevelIT`.

**Cho session mới**: chạy với `DB_URL`, `TEST_DB_URL` trỏ 3307 (memory `reference-mysql-3307-test`), `./mvnw clean verify`; chỉ `Edit` Excel `CHECKLIST_ELDER_CARE_THANH_VIEN_B.xlsx` tại chỗ.

## Ghi chú triển khai Phase 5 (2026-10-25, chờ chủ dự án duyệt checkpoint)

Nhánh `impl/004-admission-residency` từ HEAD `0bff779`; chưa commit. US3 (T029 → T037) phần backend; T038 (web, app người thân) chờ duyệt bố cục.

- **Đã triển khai**: migration `V004_05__contract.sql` (bảng `contract`, `contract_service`, `contract_absence_policy`, `contract_arrival_schedule`, `contract_content_version`; trigger khóa nội dung, chống chồng Hiệu lực, nhóm 3 chỉ thêm; CHECK theo data-model mục 5); `m02/internal/contract/{domain,persistence,service}` (`ContractService`, `ContractRules`, `ContractSnapshot`, `NonStandardTermDetector`, `ContractTermsRequestHandler`, `ContractTermsListeners`, `ContractReadService`, `ContractTransition`, `ContractOverdueTask`, `ContractReminders`, `ContractScopeAssignment`, `ContractScanFilePermission`); `ContractController` (12/13 endpoint, trừ `/deposit` của 017); bốn thân yêu cầu/phản hồi mới; `M02StatusParticipant` gọi `ContractTransition` ở lệnh Hủy tiếp nhận; `ApiContractIT` bớt 12 endpoint hợp đồng khỏi `pending`. Chi tiết tên ở glossary mục 10.11.
- **Thay bản giả bằng bản thật** (theo handoff): `FakeContractReads` đã xóa; test Phase 3, 4 dùng `ContractFixtures` (ghi thẳng bảng `contract`) và chạy lại xanh, gồm `BR_M02_22_ContractCareLevelIT`, `FR_004_ConditionsMatchCommandIT`, `ProgressIT`, `FR_076_NotYetAdmittedWarningIT`. `ContractLookup` (khung T010 cài `ContractApi` của 001) xóa; `ContractReadService` cài cả `ContractReadApi` lẫn `ContractApi` (loại lưu trú theo phiên bản nội dung).
- **Quyết định kỹ thuật cần duyệt** (chưa có trong spec, plan; không đổi nghiệp vụ):
  1. **Mở khóa khi từ chối, hủy yêu cầu duyệt điều khoản** bằng listener sau commit (`ContractTermsListeners`), không ghi nhật ký riêng (quyết định đã ở nhật ký của yêu cầu); duyệt thì `ContractTermsRequestHandler.apply` mở khóa ngay. Bản duyệt gắn `content_hash` (SHA-256 của bản chụp chuẩn hóa `ContractSnapshot`); sửa làm băm đổi thì `terms_approval_request_id` về `NULL` (FR-025a).
  2. **Chỉ Hành chính thực hiện lệnh hợp đồng** (`requireOffice` trong `ContractService`): Phụ lục 27 cho Kế toán ô T²⁶ ở dòng này để thu cọc ở 017, research T-14 thu hẹp về AC-09. Lệnh của `AdmissionApplicationService` (Phase 3) chưa thu hẹp tương tự: báo, không sửa trong phase này.
  3. **"Đủ nội dung bắt buộc" khi Gửi ký (FR-019)** hiểu là: nội trú có loại phòng hoặc giường; bán trú có lịch đến; có khoản cọc hoặc chọn không yêu cầu; đúng một dịch vụ chăm sóc đúng mức (Q-297); điều khoản khác chuẩn đã duyệt cho đúng nội dung (mã `CONTRACT_ROOM_REQUIRED`, `CONTRACT_ARRIVAL_SCHEDULE_REQUIRED`, `CONTRACT_DEPOSIT_REQUIRED`, `CONTRACT_CARE_SERVICE_REQUIRED`, `TERMS_APPROVAL_REQUIRED`). Chính sách giữ giường, điều kiện chấm dứt là văn bản mô tả, không bắt buộc.
  4. **So điều khoản chính sách vắng với CFG-M02-05** (`NonStandardTermDetector`): dòng ghi đè khớp dòng bảng theo (loại lưu trú, loại vắng, từ ngày, đến ngày); bốn loại vắng tạm (`HOME_VISIT`, `FAMILY_OUTING`, `SAME_DAY_MEDICAL_VISIT`, `OTHER`) so với `HOME_OR_OUTING`, `PER_CONTRACT` so với `BY_CONTRACT`; khác tỷ lệ phí là điều khoản `ABSENCE_POLICY`, khác giữ giường là `BED_HOLD_POLICY`, không khớp dòng nào là `ABSENCE_POLICY` không có giá trị chuẩn. `bedHoldPolicy` dạng văn bản không so với bảng.
  5. **Đơn giá ghi lúc ký**: giá riêng nếu có, không thì đơn giá chuẩn tại ngày bắt đầu và đánh dấu `referenced`; giá riêng không đánh dấu phiên bản nào. Dịch vụ không có giá hiệu lực ngày bắt đầu và không có giá riêng thì chặn `SERVICE_PRICE_MISSING`; Gửi ký chỉ cảnh báo `SERVICE_PRICE_NOT_EFFECTIVE`.
  6. **Việc `CONTRACTS`, nhắc**: quá hạn chỉ xét người cao tuổi `IN_RESIDENCE`, `TEMPORARILY_ABSENT`, `OFF_SITE_ACTIVITY`, `HOSPITALIZED` (FR-029 nêu ba trạng thái đầu; hoạt động ngoài viện coi là vẫn lưu trú); ngày quá hạn đặt đúng `ngày kết thúc + 1` kể cả chạy bù; báo Quản lý viện khi số ngày quá hạn lớn hơn CFG-M02-10, một lần (`manager_notified_overdue_at`); `CONTRACT_EXPIRING` nhắc mỗi ngày từ CFG-M02-04 ngày trước tới ngày kết thúc (hợp đồng dài hạn và bán trú); `SHORT_TERM_CONTRACT` đúng vào ngày kết thúc; `CONTRACT_OVERDUE` mỗi ngày.
  7. **Hủy tiếp nhận kéo theo hợp đồng** (FR-002, FR-027): Nháp, Chờ ký thành Đã hủy; Hiệu lực thành Chấm dứt với `termination_reason` = "Hủy tiếp nhận (lịch sử trạng thái N)", vì `TransitionResult` không mang lý do của lệnh (lý do ở lịch sử trạng thái của 001).
  8. **Đọc hiện hành khi kiểm chồng thời gian**: `ContractStore.overlapsActive` dùng `FOR SHARE` vì transaction đã mở snapshot (đọc hợp đồng) trước khi chờ khóa `resident_residency`; không có thì hai lệnh Ghi nhận đã ký đồng thời đều qua kiểm tra (trigger vẫn chặn, nhưng báo lỗi SQL thay vì 409 `CONTRACT_OVERLAP`). Test `FR_022_twoConcurrentRecordSigned_exactlyOneBecomesActive`.
  9. **Thiếu trường bắt buộc trong thân** (ví dụ thân `{}` cho `return-to-draft`, thiếu `scanFileId`) trả 400 theo xử lý validation hiện có của dự án; lý do rỗng, chỉ khoảng trắng trả 422 `REASON_REQUIRED`.
  10. **`GET /contracts/{id}/addenda`** trả danh sách rỗng tới Phase 7 (phụ lục chỉ do yêu cầu thay đổi lưu trú đã duyệt tạo). `AddendumBody.Status` theo hợp đồng api-004 là `NOT_APPLICABLE`, glossary 10.3 ghi `NOT_APPLIED`: lệch, chờ chọn khi làm Phase 7.
  11. `QuyenTepProvider` cho bản scan (`M02.CONTRACT_SCAN`) làm luôn ở phase này (T011 phần bản scan); T011 còn `LuuTruProvider`, `M02.BANG_CHUNG_QUA_DOI`.
- **Chưa làm, có chủ đích**:
  - T033 còn hai kiểm tra của "Ghi nhận đã ký" chờ 003 công bố giao diện: nội trú cần phân bổ giường đang hiệu lực hoặc tương lai bắt đầu không muộn hơn ngày bắt đầu (FR-021, `ViTriApi`); bán trú kiểm sức chứa khu nghỉ (research T-10, `SucChuaBanTruApi`). Chưa có `m03.api` trên develop (team-plan mục 8, dòng C).
  - T036: nhắc ngắn ngày "chưa có hồ sơ kết thúc hay yêu cầu đổi thời gian đang mở" cần bảng của Phase 7, 9 nên hiện nhắc vào ngày kết thúc mà không loại trừ hai trường hợp đó.
  - T038 (web `hop-dong`, app người thân): chờ chủ dự án duyệt DESIGN.md mục 14. `GET /contracts/{id}/deposit` và điều kiện (c) đọc `DepositStatusPort` thật: Phase 6. `UnitPriceApi.contractUnitPrice` vẫn rỗng (thuộc task dịch vụ, cần phiên bản nội dung). `LuuTruProvider` của hợp đồng (T011).
  - `RepresentativePort` (012) chưa có bản cài thật nên lập hợp đồng ở prod trả 409 `REPRESENTATIVE_REQUIRED` tới khi E cài (đã ở team-plan mục 8).
- **Kiểm chứng** (MySQL 3307, JDK 25): `./mvnw clean verify` BUILD SUCCESS: 281 test đơn vị + 1079 test tích hợp, 0 lỗi, 0 bỏ qua (trước Phase 5: 271 + 999). Test mới: `NonStandardTermDetectorTest` (10), `ContractContentLockDbIT` (9), `DBR_06_ContractNoOverlapIT` (6, gồm hai lệnh ký đồng thời), `ContractLifecycleIT` (19), `FR_021_RecordSignedIT` (7), `TermsApprovalIT` (10), `BR_M02_09_ContentAtDateIT` (10), `BR_M02_05_ContractReminderIT` (8), `FR_029_ContractOverdueIT` (8), `FR_004_AdmissionWithRealContractIT` (3: điều kiện (b), (c), (e) và Hoàn tất tiếp nhận đọc hợp đồng thật). Sửa hai test đơn vị cũ cho khớp mã (`LowerModuleSpiTest` bỏ phần `ContractLookup` khung, `M02StatusParticipantTest` thêm tham số `ContractTransition`). Test viết trước: lần chạy đầu là lỗi biên dịch (chưa có lớp), rồi 9 lỗi thật ở lần chạy IT đầu (mã 400 thay 422 cho thân thiếu trường, CHECK của `ContractFixtures` với nội trú ngắn ngày, tài khoản ứng dụng không có DELETE trên `price_version`, Awaitility không mang SecurityContext, và đọc snapshot ở kiểm chồng thời gian) đã sửa theo ghi chú trên.

## PHASE 6 HANDOFF (2026-10-25)

Checkpoint Phase 5 chốt trên nhánh `impl/004-admission-residency` (từ `0bff779`). HEAD cuối ghi trong báo cáo commit/push; không merge, không PR.

- **Task Phase 5**: `[x]` T029, T030, T031, T032, T034, T035, T037. `[ ]` T033 (thiếu kiểm giường nội trú FR-021 `ViTriApi` và sức chứa bán trú T-10 `SucChuaBanTruApi`, chờ C/003), T036 (nhắc ngắn ngày chưa loại trừ hồ sơ kết thúc, yêu cầu đổi thời gian đang mở; cần bảng Phase 7, 9), T038 (web, app người thân chờ duyệt DESIGN.md mục 14).
- **Đã có**: migration `V004_05__contract.sql` (không sửa nữa, đã commit); `m02/internal/contract/{domain,persistence,service}`; `ContractController` 12/13 endpoint (thiếu `GET /contracts/{id}/deposit`, còn `pending` trong `ApiContractIT`); `ContractReadService` cài `ContractReadApi` và `ContractApi` thật, thay `FakeContractReads` và `ContractLookup` (đã xóa; test dùng `ContractFixtures`); `M02StatusParticipant` gọi `ContractTransition` khi Hủy tiếp nhận. Tên lớp ở glossary 10.11, chi tiết ở "Ghi chú triển khai Phase 5" bên trên.
- **Kết quả verify** (không đổi code sau đó): `./mvnw clean verify` trên MySQL 3307, JDK 25: BUILD SUCCESS, 281 đơn vị + 1079 IT, 0 lỗi, 0 bỏ qua.
- **Blocker**: C/003 `ViTriApi`, `SucChuaBanTruApi` (BL-64); E/012 `RepresentativePort` bản cài thật, thiếu thì lập hợp đồng chạy thật trả 409 `REPRESENTATIVE_REQUIRED` (BL-65); A: `ApiContractIT` đã bớt 12 endpoint hợp đồng khỏi `pending`.
- **Cần chủ dự án duyệt (BL-66)**: 11 quyết định kỹ thuật ở "Ghi chú triển khai Phase 5" (mở khóa điều khoản bằng listener; chỉ Hành chính được lệnh hợp đồng; nghĩa "đủ nội dung bắt buộc" khi Gửi ký; so điều khoản vắng với CFG-M02-05; đơn giá ghi lúc ký; nhắc, quá hạn; Hủy tiếp nhận kéo theo hợp đồng; `FOR SHARE` khi kiểm chồng; 400 so với 422; phụ lục `NOT_APPLICABLE` (api-004) lệch `NOT_APPLIED` (glossary 10.3); `QuyenTepProvider` bản scan). Thêm: `AdmissionApplicationService` (Phase 3) chưa chặn Kế toán như hợp đồng. Chưa quyết định gì thay chủ dự án.
- **Bắt đầu Phase 6** (US4 đặt cọc) theo `tasks.md`: đọc lại tasks Phase 6, viết test trước (phải đỏ), cài `DepositStatusPort` thật thay bản giả cho điều kiện (c) và `GET /contracts/{id}/deposit` (tiêu chí `ApiContractIT` bỏ dòng này khỏi `pending`); phụ thuộc 017 (E/D) xem team-plan mục 8. Chạy test: MySQL 3307, ghi đè `DB_URL`, `TEST_DB_URL`; không đụng 3306, không sửa migration đã áp dụng (migration mới `V004_06`). Không tự làm T033, T036, T038 khi blocker chưa gỡ.

## Ghi chú triển khai Phase 6 (2026-10-26, chờ chủ dự án duyệt checkpoint)

- **Task**: `[x]` T039, T041. `[ ]` T040 (điều kiện (c), `GET /contracts/{id}/deposit` và `DepositRecheckService` xong; còn listener của sự kiện 017 `TrangThaiDatCocDaDoi`, chờ `m11.api` của E, team-plan mục 8).
- **Đã cài**: `m02/internal/deposit/service/DepositViewService` (khoản từ hợp đồng, trạng thái từ `DepositStatusPort`, chưa có 017 thì Chưa đáp ứng giống điều kiện (c); `accountant` lấy họ tên qua `M02ActorNames`), `DepositRecheckService.recheck(contractId)` (FR-032: hợp đồng Chờ ký/Hiệu lực + người cao tuổi Đang lưu trú + Chưa đáp ứng thì phát `DepositNotSatisfiedAfterAdmission`, không đổi trạng thái lưu trú), `GET /contracts/{id}/deposit` (cùng quyền xem hợp đồng; bỏ khỏi `pending` của `ApiContractIT`), `DepositAmountChanged` phát trong `ContractService.sendForSignature`. Không có migration mới (không cần `V004_06`). Tên ở glossary 10.12.
- **Quyết định kỹ thuật (tự giải quyết, không đổi nghiệp vụ)**: (1) 017 theo dõi cọc từ Chờ ký (FR-030) nên khoản cọc gửi lúc Gửi ký, gồm gửi lại sau Trả về nháp; `newAmount` = 0 khi không yêu cầu cọc; phụ lục đổi khoản phát ở Phase 7. (2) Sự kiện cho 018 dùng `AdmissionCompleted`, `ContractStatusChanged` đã có; 018 tự dựng chuỗi nối tiếp từ `ContractReadApi.ofResident`, nên không thêm sự kiện. (3) Recheck chỉ xét hợp đồng Chờ ký hoặc Hiệu lực.
- **Lệch hợp đồng, cần chủ dự án quyết**: `Deposit.basis.receiptNumber` ở api-004 còn, nhưng Q-385 bỏ số phiếu thu chi và `DepositStatusView` không mang số phiếu; mã giữ trường, luôn rỗng. Đề nghị bỏ khỏi hợp đồng (đổi hợp đồng nên chưa tự làm). Thêm: chữ ký `DepositStatusView` vẫn chờ E xác nhận.
- **Kiểm chứng** (MySQL 3307, JDK 25): `./mvnw clean verify` BUILD SUCCESS: 281 đơn vị + 1091 tích hợp (+12), 0 lỗi, 0 bỏ qua. Test mới: `DepositConditionIT` (5), `FR_032_DepositAfterResidencyIT` (4), `Events017018IT` (3). Lần chạy đầu RED vì thiếu lớp, rồi `ApiContractIT` đỏ vì thiếu `receiptNumber`, đã sửa.
- **Chưa làm, có chủ đích**: listener của T040 (chờ E); T033, T036, T038 như Phase 5 (không đổi); web, app người thân vẫn chờ duyệt DESIGN.md mục 14. 11 quyết định của BL-66 và `AdmissionApplicationService` chưa chặn Kế toán: vẫn chờ chủ dự án, chưa quyết gì thay.

## PHASE 7 HANDOFF (2026-10-26)

Checkpoint Phase 6 chốt trên nhánh `impl/004-admission-residency` (từ `8e5f881`), chủ dự án đã duyệt. HEAD cuối ghi trong báo cáo commit/push; không PR, không merge.

- **Task Phase 6**: `[x]` T039, T041 (2/3). `[ ]` T040: điều kiện (c), `GET /contracts/{id}/deposit`, `DepositRecheckService` xong; thiếu listener `TrangThaiDatCocDaDoi` của 017, chờ E khai báo `m11.api.events` và cài `DepositStatusPort` thật (BL-67, team-plan mục 8).
- **Đã có**: `m02/internal/deposit/service/{DepositViewService, DepositRecheckService}`; `web/response/DepositBody` (`basis.receiptNumber` giữ, luôn rỗng); `GET /contracts/{id}/deposit` (đã bỏ khỏi `pending` của `ApiContractIT`); `DepositAmountChanged` phát trong `ContractService.sendForSignature` (kể cả gửi lại sau Trả về nháp, 0 khi không yêu cầu cọc). Test: `DepositConditionIT` (5), `FR_032_DepositAfterResidencyIT` (4), `Events017018IT` (3). Tên ở glossary 10.12. Không có migration mới ở Phase 6.
- **Blocker, chờ**: BL-66 (11 quyết định kỹ thuật Phase 5, gồm chỉ Hành chính được lệnh hợp đồng, nghĩa "đủ nội dung bắt buộc", phụ lục `NOT_APPLICABLE` so với `NOT_APPLIED`; chưa được duyệt về nghiệp vụ, không tự sửa theo suy đoán; thêm `AdmissionApplicationService` chưa chặn Kế toán); BL-67 (E/017 `DepositStatusPort` bản thật và sự kiện đổi trạng thái cọc); BL-68 (đề xuất đối chiếu Q-385 để bỏ `receiptNumber` khỏi api-004, chủ dự án giữ như hiện tại); C/003 `ViTriApi`, `SucChuaBanTruApi` (BL-64); E/012 `RepresentativePort` bản thật (BL-65); A xem lại đăng ký api-004 trong `ApiContractIT`.
- **Task còn `[ ]` của Phase 4, 5**: Phase 4 theo "Ghi chú triển khai Phase 4" (ba endpoint danh sách chờ theo giường, giữ chỗ, hết hạn giữ chỗ, nhắc phân bổ lại: chờ C/003, `T021`, `T025`, `T026`, `T027` phần còn lại, web chờ duyệt DESIGN.md); Phase 5: T033 (kiểm giường nội trú FR-021 `ViTriApi`, sức chứa bán trú T-10), T036 (nhắc ngắn ngày chưa loại trừ hồ sơ kết thúc, yêu cầu đổi thời gian đang mở; cần bảng Phase 7, 9), T038 (web, app người thân chờ DESIGN.md mục 14). Không làm khi blocker chưa gỡ.
- **Bắt đầu Phase 7** (US5 thay đổi lưu trú, T042 → T047, T075 theo `tasks.md`): xác minh HEAD và `git status` sạch, `git fetch`; đọc lại tasks Phase 7, quickstart mục 4 và nghiệp vụ FR-037 → FR-047, DBR-07, BR-M02-04, BR-M02-16; test viết trước, phải đỏ (T042, T043, T075). Migration mới là `V004_06__phu_luc.sql` (T044; không sửa `V004_01` → `V004_05`). Phụ thuộc: `YeuCauPheDuyetApi` của 000 (handler `M02.THAY_DOI_LUU_TRU`), `MucChamSocApi`/`YeuCauDoiMucApi` của 001, `PhanBoGiuongLenhApi` của C/003 (đặt trước chuyển theo lịch: chưa có thì dùng bản giả, không khai báo thay C). Phase 7 phát `DepositAmountChanged` khi phụ lục đổi khoản cọc, thêm `ngay_ket_thuc_hien_hanh`, chốt `AddendumBody.Status` khi chủ dự án chọn (BL-66). T036 có thể gỡ phần loại trừ khi Phase 7 có yêu cầu đổi thời gian. Chưa code web, app khi DESIGN.md mục 14 chưa duyệt.
- **Kiểm chứng** (MySQL 3307, JDK 25): `./mvnw clean verify` BUILD SUCCESS: 281 đơn vị + 1091 tích hợp, 0 lỗi, 0 bỏ qua. Chạy test: `export DB_URL=jdbc:mysql://localhost:3307/elder_care_demo TEST_DB_URL=jdbc:mysql://localhost:3307/eldercare_test`; không đụng 3306.
- **Git**: nhánh `impl/004-admission-residency`; stash `CP-001-P2-IMPL` giữ nguyên (nhánh 001); Phase 6 commit và push, chưa PR vào `develop`.

## Ghi chú triển khai Phase 7 (2026-10-26, chờ chủ dự án duyệt checkpoint)

Nhánh `impl/004-admission-residency` từ HEAD `1574742`; chưa commit. US5 (T042 → T047, T075) phần backend; T047 phần web, app người thân chờ duyệt bố cục (DESIGN.md mục 14). Test viết trước mã; lần chạy đầu của mã chính là sau khi cài (không chạy ở trạng thái đỏ riêng, như ghi ở TEST_LOG).

- **Task**: `[x]` T044, T045, T046, T075. `[ ]` có chủ đích: T042 (đã làm các phần độc lập; thiếu "giữ giường" `datTruocChuyenTheoLich`, chờ 003), T043 (đã làm trùng thuộc tính, áp dụng không thành, người đại diện tạo Nháp, đổi lịch đến ở mức hình dạng, thay thế bởi đánh giá; thiếu hủy phân bổ theo lịch và kiểm sức chứa bán trú chờ 003, thiếu Hành chính hoàn thiện Nháp chờ A), T047 (controller `POST`, `GET` xong; `PUT /residency-change-requests/{id}` chờ A; màn web, app người thân chờ duyệt bố cục).
- **Đã cài**: migration `V004_06__addendum.sql` (`contract_addendum`, `change_request_attribute`, trigger ngày hiệu lực, bất biến, chỉ retention xóa; thay `UNIQUE (contract_id, effective_from)` của `contract_content_version` bằng chỉ mục, thêm khóa ngoại tới phụ lục); `m02/internal/change/{domain,persistence,service}` (`ResidencyChangeHandler` loại `M02.RESIDENCY_CHANGE`, `ResidencyChangeRules`, `AddendumContent`, `IsMinorChange`, `AddendumClosing`, `ResidencyChangeService`, `ResidencyChangeListeners`, `CareLevelChangeRequestHandler` thay khung ở `transition`); `ResidencyChangeRequestController` (2/3 endpoint); `GET /contracts/{id}/addenda` đọc dữ liệu thật; `ContractStore` thêm `appendVersion`, `latestVersion`, `setCurrentEndDate`. Tên ở glossary 10.13.
- **Quyết định kỹ thuật** (chưa có trong spec, plan; không đổi nghiệp vụ; chờ chủ dự án duyệt cùng BL-66):
  1. **Nội dung yêu cầu**: `newValue.services` là thay đổi theo dòng (`ADD`, `REMOVE`), không phải danh sách đích, để hai phụ lục dịch vụ nối tiếp không ghi đè nhau và "thay đổi nhỏ" tính được từ dòng thêm, bớt. Khoản cọc đổi đi kèm là `newValue.depositAmount`, `newValue.noDepositRequired` (hợp đồng api-004 để `newValue` tự do; FR-030 "kể cả khi phụ lục đổi khoản cọc"): nêu số tiền mà không nêu cờ thì hợp đồng nay yêu cầu cọc; phát `DepositAmountChanged` khi khoản cọc hiệu lực đổi.
  2. **Phụ lục chỉ giữ thuộc tính bị đổi** (`content_before`, `content_after`), nên không lệch khi nội dung cơ sở đổi giữa lúc duyệt và lúc áp dụng; nội dung đầy đủ nằm ở phiên bản nội dung mới, xây từ phiên bản mới nhất lúc áp dụng.
  3. **Bỏ `UNIQUE (contract_id, effective_from)`** của `contract_content_version` (đã áp dụng ở `V004_05`, sửa bằng `ALTER` ở `V004_06`, không sửa migration cũ): hai phụ lục khác thuộc tính cùng ngày hiệu lực là hợp lệ (FR-046 chỉ chặn cùng thuộc tính) nhưng bị ràng buộc cũ chặn, làm yêu cầu thứ hai Áp dụng không thành. Tra theo ngày chọn `effective_from` lớn nhất rồi `sequence_no` lớn nhất (`ContractStore.versionAt`).
  4. **Chỉ mục thuộc tính** `change_request_attribute` ghi khi lập yêu cầu qua `POST /residency-change-requests` (`pending_effective = FALSE`, để tra yêu cầu đang mở cho `CareLevelChangeRequestApi`), đặt TRUE khi duyệt, FALSE khi phụ lục rời Chờ hiệu lực. Yêu cầu lập qua `POST /approval-requests` chung của 000 không có dòng chỉ mục lúc Nháp, Chờ duyệt (có khi duyệt), nên chưa bị thay thế bởi đánh giá khi còn Nháp; 004 coi `/residency-change-requests` là đường lập chính thức.
  5. **Đóng phụ lục khi Áp dụng không thành** ngay trong `checkApplyConditions` (cùng transaction với chuyển `APPLY_FAILED`), không nghe `RequestApplyFailed`: `M13ArchTest` chỉ cho M13 nghe sự kiện có thông báo. Hủy yêu cầu Đã duyệt dùng listener `RequestCancelled`; thay thế bởi đánh giá đóng trong chính lệnh của 001. Khi làm Phase 9, lệnh Kết thúc lưu trú, Qua đời, Hủy tiếp nhận tự đóng yêu cầu mở bằng `closeForFinalState` cho các mã trong `AddendumStore.requestsOf(residentId)` rồi gọi `AddendumClosing` (team-plan mục 8, dòng ghi nhớ).
  6. **Điều kiện của 003 chưa kiểm được thì chặn, không cho qua** (BL-56, BL-59, BL-64): đổi giường, đổi lịch đến, đổi giữa bán trú và nội trú bị 409 `BED_ALLOCATION_CHECK_UNAVAILABLE`, `DAY_CARE_CAPACITY_CHECK_UNAVAILABLE` khi duyệt. Khi 003 công bố giao diện thì thay hai điều kiện này bằng lời gọi thật, thêm `datTruocChuyenTheoLich` ở `onApprove`, `thucHienChuyenTheoLich` ở `apply`, hủy ở đóng phụ lục. Đổi giữa nội trú dài hạn và ngắn ngày không cần giường nên chạy được.
  7. **Thay đổi nhỏ** (FR-043): `IsMinorChange(change, costImpact, nội dung hiện hành)`; ảnh hưởng chi phí là mảng `{item, previousUnitPrice, newUnitPrice}` do hệ thống điền lúc lập (dịch vụ thuộc gói: cả hai bằng 0); tính lại ở mọi lần xét qua `approverRoleRestriction`, nên Hành chính bị chặn khi hợp đồng đổi làm yêu cầu hết là thay đổi nhỏ và không có trong danh sách người duyệt của yêu cầu khác.
  8. **`DUOC_THAY_THE` đặt tên `SUPERSEDED`** (chuyển mở rộng `SUPERSEDE` từ Nháp, Chờ duyệt, Đã duyệt), theo glossary 001 (`ACTIVE`, `SUPERSEDED`). Yêu cầu có loại đổi mức cùng loại khác bị thay thế nguyên yêu cầu, vì không tách được một phần; người lập lại phần còn lại.
  9. **Ai lập** (FR-039) kiểm trong `validateContent` nên đúng cả đường chung của 000: Bác sĩ chỉ `CARE_LEVEL` (trừ khi cũng là Hành chính); người thân phải là người đại diện hiệu lực (`RepresentativePort`). Chú thích ² của Phụ lục 27 chưa có bản cài (E), nên 002 từ chối người thân; test dùng bản thử `RepresentativeRequestIT.Footnote2`.
  10. **FR-028**: "ảnh hưởng chi phí" chỉ hiện với người có quyền xem hợp đồng (`ADMISSION_CONTRACT_DEPOSIT`, xem); người khác bị bỏ trường và tên ghi vào `hiddenFields` qua `SelfHiddenFields`, vì nhóm `FINANCE` của 002 không thu hẹp vai trò nào.
  11. **Khóa**: người cao tuổi (`ResidencyLock`), rồi hợp đồng, rồi phụ lục (research T-03). Hai yêu cầu cùng thuộc tính duyệt cùng lúc: một được 200, một 409 `DUPLICATE_PENDING_ATTRIBUTE` (nhờ `UNIQUE(lock_key)`, bắt `DuplicateKeyException`), có test hai luồng.
  12. **`reference` không bắt buộc `type`, `id`** ở hợp đồng, nên thân yêu cầu không đánh `@NotNull` hai trường đó; thiếu thì `ResidencyChangeContent.parse` trả 422.
- **Lệch hợp đồng, cần quyết định**: `AddendumBody.Status` giữ `NOT_APPLICABLE` theo api-004 trong khi cột và glossary dùng `NOT_APPLIED` (BL-66). `PUT /residency-change-requests/{id}` còn ở `pending` của `ApiContractIT` vì `editContent`, `submit` của 000 chỉ cho chính người lập; người đại diện tạo Nháp nhưng Hành chính chưa hoàn thiện, gửi duyệt được, và người đại diện tự gọi được `submit` của 000, trái FR-039 (team-plan mục 8, dòng A).
- **Chưa làm, có chủ đích**: web `frontend/src/features/residency-change/`, app người thân (chờ duyệt bố cục, DESIGN.md mục 14); phần 003 ở trên; `LoaiLuuTruDaDoi` (không có trong `m02.api.events`, glossary không có tên; không phát). `GET /contracts/{id}/addenda` nay trả phụ lục thật (ghi chú Phase 5 mục 10 đã hết hiệu lực); `hasOpenRequest` của 001 đã thật (nhắc đổi mức dừng khi đã có yêu cầu đang mở).
- **Kiểm chứng** (MySQL 3307, JDK 25): `./mvnw clean verify` BUILD SUCCESS: 290 đơn vị + 1151 tích hợp, 0 lỗi, 0 bỏ qua (Phase 6: 281 + 1091). Test mới: `IsMinorChangeTest` (9), `BR_M02_04_ApplyAddendumIT` (11), `DBR_07_AddendumIT` (7), `FR_041_CheckOnApprovalIT` (5), `FR_046_DuplicateAttributeIT` (6), `ApplyFailedIT` (3), `RepresentativeRequestIT` (9), `ChangeArrivalScheduleIT` (8), `FR_043_MinorChangeIT` (6), `CareLevelChangeRequestIT` (4). `ApiContractIT` xanh sau khi bớt `POST`, `GET /residency-change-requests/{id}` khỏi `pending`. Lần chạy `clean verify` đầu đỏ ở `M13ArchTest` (listener `RequestApplyFailed` của 004), đã đổi theo mục 5.

## PHASE 7 REPORT (2026-10-26) – cho session sau và chủ dự án

- **Git**: nhánh `impl/004-admission-residency`, HEAD `1574742`, **chưa commit/push, chưa PR**; stash `CP-001-P2-IMPL` giữ nguyên. Không bắt đầu Phase 8.
- **Task**: `[x]` T044, T045, T046, T075. `[ ]` có chủ đích: T042 (giữ giường, chuyển giường theo lịch: chờ C/003), T043 (hủy phân bổ theo lịch, sức chứa bán trú: chờ C/003; Hành chính hoàn thiện Nháp của người đại diện: chờ A), T047 (`PUT`: chờ A; web, app người thân: chờ duyệt bố cục DESIGN.md mục 14). T040 (E/017), T033 (C/003) giữ nguyên `[ ]`.
- **Blocker mới** (đã ghi team-plan mục 8): A (000) "Đổi api" cho Nháp do người khác lập; C (003) giao diện giường, sức chứa cho Phase 7; E (012) chú thích ² người đại diện.
- **Cần chủ dự án duyệt**: 12 quyết định kỹ thuật ở "Ghi chú triển khai Phase 7" (nổi bật: bỏ `UNIQUE (contract_id, effective_from)` bằng `ALTER` ở `V004_06`; `newValue.services` theo dòng ADD/REMOVE; khoản cọc đi kèm `newValue.depositAmount`; đóng phụ lục trong `checkApplyConditions`; chặn đổi giường, đổi lịch đến khi 003 chưa có; `SUPERSEDED`; ẩn `costImpact` theo quyền xem hợp đồng), glossary 10.13, cùng BL-66, BL-62.
- **Sửa ngoài phạm vi chính nhưng cùng phase**: `ContractReadService` (loại lưu trú, mức, khoản cọc hiện hành đọc từ phiên bản nội dung mới nhất), `ContractStore.activeWithEndDate` (loại lưu trú theo phiên bản), `ContractContentLockDbIT` (kỳ vọng UNIQUE theo ngày), `LowerModuleSpiTest` (khung đổi mức đã thay bản thật).
- **Cách chia commit đề xuất**: (1) `feat(m02): addendum schema` – `V004_06` + `ContractContentLockDbIT`; (2) `feat(m02): residency change request handler and addendum` – `m02/internal/change`, `ContractStore`, `ContractReadService`, bỏ khung `transition/CareLevelChangeRequestHandler`; (3) `feat(m02): residency change request endpoints` – controller, body, `AddendumBody`, `ContractController`, `ApiContractIT`; (4) `test(m02): residency change tests` – mọi `*IT`, `IsMinorChangeTest`, `LowerModuleSpiTest`; (5) `docs(004): Phase 7 report` – plan, tasks, glossary 10.13, team-plan.
- **Bắt đầu Phase 8** (US6 tạm vắng, T048 → T056) chỉ khi chủ dự án duyệt checkpoint này: `git status` sạch, test trước, migration mới `V004_07__absence.sql`.

## PHASE 8 HANDOFF (2026-10-26)

Checkpoint Phase 7 do chủ dự án duyệt theo phạm vi đã hoàn thành (2026-10-26); commit và push lên `impl/004-admission-residency` (HEAD gốc `1574742`; hash cuối ghi trong báo cáo commit và `git log`); không PR, không merge. Stash `CP-001-P2-IMPL` (nhánh 001) giữ nguyên. Không làm lại Phase 1–7.

- **Task Phase 7**: `[x]` T044, T045, T046, T075. `[ ]` có chủ đích T042 (giữ giường, chuyển giường theo lịch: C/003), T043 (hủy phân bổ theo lịch, sức chứa bán trú: C/003; Hành chính hoàn thiện Nháp của người đại diện: A/000), T047 (`PUT`: A/000; web, app người thân: chờ duyệt bố cục DESIGN.md mục 14). Còn mở từ trước: T021, T025, T026, T027 (C/003), T033 (C/003), T036 (cần bảng Phase 9), T038 (web), T040 (E/017), T010, T011, T019, T028, T108.
- **Database**: `V004_06__addendum.sql` (không sửa nữa): `contract_addendum`, `change_request_attribute` (cột sinh `lock_key` + `UNIQUE`), trigger ngày hiệu lực ≥ ngày bắt đầu hợp đồng, phụ lục bất biến, chỉ job lưu giữ xóa; `contract_content_version`: bỏ `UNIQUE (contract_id, effective_from)`, thêm chỉ mục `(contract_id, effective_from, sequence_no)` và khóa ngoại tới phụ lục. Vẫn còn `UNIQUE (contract_id, sequence_no)` (chặn phiên bản trùng số thứ tự, test `ContractContentLockDbIT`), `UNIQUE (approval_request_id)` (một phụ lục mỗi yêu cầu, `DBR_07_AddendumIT`) và `UNIQUE (lock_key)` (một yêu cầu chờ hiệu lực mỗi thuộc tính, `FR_046_DuplicateAttributeIT`); hai phụ lục khác thuộc tính cùng ngày được phép (`BR_M02_04_ApplyAddendumIT.FR_044_twoAddendaOnTheSameDate_…`). Migration mới tiếp theo là `V004_07__absence.sql`.
- **API**: `POST`, `GET /residency-change-requests/{id}`; `GET /contracts/{id}/addenda` thật; SPI `CareLevelChangeRequestApi` thật; `ContractReadApi` (`contract`, `depositAmount`) và `activeWithEndDate` đọc phiên bản nội dung mới nhất. `PUT /residency-change-requests/{id}` còn ở `pending` của `ApiContractIT`. Chi tiết tên ở glossary 10.13.
- **Test**: `clean verify` (MySQL 3307, JDK 25) 290 đơn vị + 1151 tích hợp, 0 lỗi, 0 bỏ qua; không đổi mã sau lần xanh đó.
- **Quyết định kỹ thuật chưa được duyệt** (không tự đổi nghiệp vụ; ghi ở "Ghi chú triển khai Phase 7", BL-72, BL-66): bỏ `UNIQUE` theo ngày hiệu lực; `newValue.services` theo dòng ADD/REMOVE; khoản cọc đi kèm `newValue.depositAmount`; phụ lục chỉ giữ thuộc tính đổi; đóng phụ lục trong `checkApplyConditions`; chặn bằng điều kiện khi 003 chưa có; `SUPERSEDED`; ẩn `costImpact` theo quyền xem hợp đồng; `NOT_APPLICABLE` (hợp đồng) so với `NOT_APPLIED` (glossary). Chưa sửa API/SPI của A, C, D, E.
- **Blocker**: BL-69 (A: Đổi api Nháp do người khác lập), BL-70 (C: giao diện giường, sức chứa), BL-71 (E: chú thích ²), BL-72 (chủ dự án duyệt Phase 7), cộng BL-56, 59, 62, 64–68 như trước.
- **Bắt đầu Phase 8** (US6 tạm vắng, T048 → T056, `tasks.md`): xác minh HEAD và `git status` sạch, `git fetch`; đọc lại tasks Phase 8, spec FR-048 → FR-061, research T-07 → T-09, nghiệp vụ BR-M02-06, 07, 17, 18; test trước (T048 → T050) phải đỏ; migration `V004_07__absence.sql` (data-model mục 8). Phụ thuộc: `ResidentStatusApi` của 001 (`CHO_TAM_VANG`, `CHUYEN_VIEN`, `GHI_NHAN_TRO_VE` đã có bean `M02StatusParticipant` kiểm `validateDetails`), `BanGhiDonPort` (012, bản giả), `TrangThaiCoMatBanTruPort` (005, bản giả); giữ giường cần 003 (chưa có, không tự khai báo). Chạy test: `export DB_URL=jdbc:mysql://localhost:3307/elder_care_demo TEST_DB_URL=jdbc:mysql://localhost:3307/eldercare_test`, `./mvnw verify -Dtest=NoSuchTest -Dit.test='A,B' -Dsurefire.failIfNoSpecifiedTests=false`; không đụng 3306. Đến Phase 9 nhớ đóng yêu cầu thay đổi lưu trú còn mở bằng `closeForFinalState` (team-plan mục 8).

## Ghi chú triển khai Phase 8 (2026-10-26, chờ chủ dự án duyệt checkpoint)

- **Đã làm**: T048, T050, T051, T052, T055 `[x]`. T049, T053, T054 `[ ]` vì còn phần chờ 003 (xem tasks.md); T056 chờ duyệt bố cục. Tên theo glossary 10.14.
- **Kiểm**: `./mvnw clean verify` (JDK 25, MySQL 3307): 301 đơn vị + 1185 tích hợp, 0 lỗi (Phase 7: 290 + 1151). Test mới: 11 đơn vị (`BR_M02_06_AbsencePolicyLookupTest`, `BR_M02_17_ManagerDecidesDay1Test`), 34 tích hợp (`AbsenceDayIT` 9, `BR_M02_07_BedHoldDecisionIT` 6, `BR_M02_18_CorrectionOverThresholdIT` 6, `FR_057_TransferOutDuringAbsenceIT` 4, `DayCareCoefficientIT` 4, `DayCareClosureDayIT` 5). Sửa test cũ: `M02StatusParticipantTest` (5 tham số), `LowerModuleSpiTest` (`AbsenceLookup` thật), `FakeM02Test`/`FakePickupRecords` (lỗi 409 `PICKUP_RECORD_ALREADY_USED`), `ApiContractIT` (bớt 9 endpoint khỏi `pending`).
- **Quyết định kỹ thuật (B, chờ duyệt)**:
  1. Migration `V004_07__absence.sql` (6 bảng, trigger bất biến khi đóng; `absence_day.hold_policy` lưu chế độ giữ giường của dòng áp dụng để tra ngưỡng bằng một truy vấn; bản đầu tính lại cả khoảng ngày làm `BR_M02_19_CatchUpRunIT` treo).
  2. `AbsencePolicyResolver` (hàm thuần): nhóm `HOME_OR_OUTING` chỉ gồm Về nhà, Đi chơi (6.7); Đi khám trong ngày, Lý do khác chỉ có dòng khi bảng hoặc hợp đồng nêu riêng hoặc dùng `ANY`. Lệch với `NonStandardTermDetector.absence()` (Phase 5) gộp cả bốn loại: chưa sửa, cần chủ dự án xem.
  3. `absence_day.hold_bed` = giường đang được giữ vào ngày đó; quyết định thay hệ số mọi ngày nó phủ tới quyết định sau; `applies_from` = ngày ngưỡng (quyết định đầu) hoặc ngày quyết định (sau); COEFFICIENT_ONLY = các ngày chờ quyết định của lượt đã đóng.
  4. Ngưỡng báo một lần (`threshold_notified_at`); ngày 1 "Quản lý quyết định" báo ngay lúc mở (BR-M02-17); không tạo yêu cầu (Q-307).
  5. Chuyển viện khi đang Tạm vắng: đóng lượt cũ rồi mở lượt Bệnh viện (UNIQUE lượt mở), ngày chuyển thuộc lượt mới, giường giữ liên tục.
  6. `CorrectionApi` tra trễ qua `ObjectProvider` (tránh vòng bean với `CorrectionService`); đính chính chỉ tính lại từ chỗ đổi; `M02.BED_HOLD_DECISION` sửa được hệ số, ngày xem xét lại.
  7. Hợp đồng api-004 giữ nguyên: gia hạn, khai báo, hủy ngày nghỉ không trả nội dung; `coefficient` bắt buộc (0% là giá trị điền sẵn của giao diện); thân đính chính đặt tên schema `AbsenceCorrectionBody` (trùng `CorrectionBody` của m15 làm springdoc lẫn).
  8. `PickupRecordPort` tra qua `ObjectProvider`: chưa có 012 thì điều kiện `ABS_PICKUP_RECORD` chưa thỏa (Cho tạm vắng 409 ở môi trường chạy thật).
  9. `DayCareAbsenceNoticeRecorded.sessionId` đổi `long` → `Long` (rỗng là cả ngày). Buổi thuộc 005 nên 004 dùng giờ đến sớm nhất của ngày để xác định báo trước đủ CFG-M02-06.
  10. `absence.reason` chưa ghi: `CommandContext` của 001 không mang lý do lệnh; cần thêm trường (module 001 của B) nếu chủ dự án muốn.
- **Bẫy**: test đổi tham số phải `clock.reset()` trước, vì `parameter_history` xếp theo `effective_from` và dòng ở tương lai làm lệch `ParameterTableIT` (A).

## PHASE 9 HANDOFF (2026-10-26)

- Chủ dự án duyệt checkpoint Phase 8 (backend, 2026-10-26); đã commit và push nhánh `impl/004-admission-residency`, chưa PR. Bắt đầu Phase 9 (US7, T057 → T061): `git status`, test trước, migration `V004_08__residency_ending.sql`.
- Task Phase 8: `[x]` T048, T050, T051, T052, T055; `[ ]` T049, T053, T054 (chờ 003, T069), T056 (chờ bố cục).
- Đã có: migration `V004_07__absence.sql` (không sửa), `internal/absence` (policy, calculator, service, transition, task `ABSENCE_DAYS`, provider đính chính), 9 endpoint khớp api-004 trong `ApiContractIT`, `AbsenceApi.open` thật, sự kiện `DayCareAbsenceNoticeRecorded.sessionId` là `Long`.
- Blocker: BL-73 (C/003 giữ/giải phóng giường), BL-74 (E/012 `PickupRecordPort`), BL-75 (10 quyết định kỹ thuật, `NonStandardTermDetector` lệch nhóm, `absence.reason`), BL-76 (bố cục web). Chưa quyết định thay chủ dự án.
- Verify cuối Phase 8: `./mvnw clean verify` JDK 25, MySQL 3307: 301 đơn vị + 1185 IT, 0 lỗi, 0 bỏ qua, BUILD SUCCESS.
- Còn mở từ Phase 8: `giuGiuongKhiVang`, `giaiPhongGiuongKhiVang` (C/003), web `vang-mat` và app (T056, bố cục), `TrangThaiCoMatBanTruPort` cho ngày bán trú ở T069.
- Phase 9/10 phải đóng lượt vắng đang mở khi `KET_THUC_LUU_TRU`, `GHI_NHAN_QUA_DOI` (thêm hàm đóng ở `AbsenceTransition`, ghi nốt ngày trước khi đóng) và gọi `closeForFinalState` cho yêu cầu thay đổi lưu trú.
- Chạy test: `DB_URL`, `TEST_DB_URL` về 3307; mysqld 3307 dùng datadir cũ ở scratchpad phiên 221e9972 (bật bằng `mysqld --no-defaults --datadir=… --port=3307`).


## Ghi chú triển khai Phase 9 (US7, 2026-10-26, chờ chủ dự án duyệt checkpoint)

- **Đã làm**: T057, T058, T060, T108 `[x]`. T059 `[ ]` (thiếu một ý, xem quyết định 1), T061 `[ ]` (web chờ bố cục DESIGN.md mục 14), T106 `[ ]` (còn trường hợp qua đời, Phase 10), T107 `[ ]` (web). Tên theo glossary 10.15.
- **Kiểm**: `./mvnw clean verify` (JDK 25, MySQL 3307): 301 đơn vị + 1242 tích hợp, 0 lỗi, 0 bỏ qua, BUILD SUCCESS (Phase 8: 301 + 1185; lần chạy đầu và hai lần chạy sau đó lỗi 3 test `FR_043_MinorChangeIT` vì `EVENT_PUBLICATION.SERIALIZED_EVENT` dài 4000 bị vượt: danh sách `approvers` của sự kiện `RequestSubmitted` dài thêm với mỗi tài khoản Quản lý viện test tạo ra; `ContractTestBase` nay dùng chung một Quản lý viện giữa các test). Test mới 47 + 12 lặp: `ResidencyEndingIT` 10, `EndingConditionsIT` 12, `EndingExceptionIT` 9, `FR_066_FinalPeriodIT` 4, `FR_066a_PastExpectedDateIT` 6, `ResidencyEndingReadApiIT` 3, `EndingConcurrencyIT` 2 (lặp 8 + 4), `EndingClosesRequestsIT` 1. Sửa test cũ: `M02StatusParticipantTest` (6 tham số), `ApiContractIT` (bớt 7 endpoint khỏi `pending`). Test viết trước mã nhưng chưa chạy ở trạng thái đỏ riêng (lần chạy đầu sau khi cài còn 5 lỗi của test: số mã kỳ cuối, loại ngoại lệ, UPDATE hợp đồng bị trigger khóa, lỗi servlet của MockMvc).
- **Lỗi thật phát hiện và sửa**: (1) `EndingConcurrencyIT` lộ bế tắc ở `ResidencyLock.lock`: `INSERT IGNORE` lấy khóa S trên dòng sẵn có, hai lệnh đồng thời cùng nâng lên X ở `SELECT … FOR UPDATE`; đổi sang `INSERT … ON DUPLICATE KEY UPDATE resident_id = resident_id` (khóa X ngay). `ResidencyLockIT`, `DBR_06_ContractNoOverlapIT` vẫn xanh. (2) lập hồ sơ kiểm "tối đa một hồ sơ mở" bằng đọc khóa (`lockPreparingOf`) vì ảnh chụp REPEATABLE READ cũ làm lệnh thua bị `DuplicateKeyException` thay vì 409.
- **Quyết định kỹ thuật (B, chờ duyệt)**:
  1. **Bàn giao không gọi `BanGhiDonPort.daDung`** (tasks T059 nêu): `POST …/handover` của api-004 và research T-09 không có mã bản ghi đón; không tự thêm trường vào hợp đồng. Cần chủ dự án chọn: thêm `pickupRecordId` (chỉ khi người nhận là người thân), hoặc bỏ ý này khỏi T059. BL-77.
  2. **Yêu cầu Nháp, Chờ duyệt của người đó chưa đóng được**: `ResidencyChangeClosing.closeAll` đóng các yêu cầu đã ghi ở `change_request_attribute` (Đã duyệt chờ hiệu lực, đủ để phụ lục về Không áp dụng); yêu cầu thay đổi lưu trú còn Nháp, Chờ duyệt và yêu cầu ngoại lệ chưa duyệt không có chỗ liệt kê theo người cao tuổi ở 000. Ngoại lệ chưa duyệt bị chặn duyệt khi hồ sơ không còn Đang chuẩn bị (`checkOnApprove`, `checkApplyConditions`). Cần A "Đổi api" `ApprovalRequestApi.openRequestsOf(residentId, types)` hoặc 004 nghe `RequestCreated`. BL-78.
  3. **Chú thích 39 (Kế toán) cài bằng `FootnoteCondition` ở 004**: thỏa khi đối tượng là `M02.RESIDENCY_ENDING` (xem) hoặc `M02.ENDING_EXCEPTION.SETTLEMENT` (lập ngoại lệ ý tiền); lệnh Kết thúc lưu trú, qua đời của 001 mang đối tượng người cao tuổi nên Kế toán bị chặn; các lệnh khác trên hồ sơ (lập, đổi ngày, hủy, bàn giao) chặn Kế toán theo vai trò ở service. Phụ lục 27 cho Kế toán cả ô xem và sửa với chú thích 39; chỉ ô xem được mở rộng cho hồ sơ kết thúc. BL-80.
  4. **Mã dòng (f), (g)**: research T-09 không đặt mã; 004 hiển thị `END_F_CONTRACT`, `END_G_BED` (trạng thái `AUTOMATIC`). Dòng (h) hiện mã `END_H_SETTLEMENT`, căn cứ ghép lý do của hai mã 017. `ConditionWaiver` chỉ mang một mã yêu cầu nên lịch sử trạng thái ghi yêu cầu được duyệt đầu tiên, mọi mã được miễn đều có trong `conditionCodes`.
  5. **Hợp đồng không kỳ hạn** (`current_end_date` rỗng) khi Kết thúc lưu trú thì Chấm dứt (FR-027 chỉ nêu "không sớm hơn ngày kết thúc" → Kết thúc); có ngày kết thúc và thời điểm hiệu lực không sớm hơn thì Kết thúc, gỡ dấu quá hạn. Hợp đồng Nháp, Chờ ký của lần tái ký giữ nguyên (FR-065 chỉ nêu hợp đồng Hiệu lực).
  6. **Điều kiện của 004 cho lệnh**: `END_NO_PREPARING_ENDING` (không có hồ sơ Đang chuẩn bị hoặc `residencyEndingId` không khớp), `END_PAST_EXPECTED_DATE` (dấu quá ngày dự kiến), `END_EFFECTIVE_DATE_OUT_OF_RANGE` (thời điểm hiệu lực không cùng ngày kết thúc dự kiến, tính theo giờ Việt Nam), `END_E_HANDOVER`. Khi đã có dấu quá ngày thì chỉ báo `END_PAST_EXPECTED_DATE`. Không chặn ngày dự kiến ở quá khứ khi lập (spec chỉ yêu cầu "không sớm hơn hôm nay" để gỡ dấu); đổi sang ngày đã qua thì dấu tính ngay từ ngày liền sau ngày mới.
  7. **Việc `ENDINGS`** (`@Order(40)`): gắn dấu từ ngày liền sau ngày dự kiến (đúng ngày kể cả chạy bù), phát `ResidencyDaysChanged(cause = RESIDENCY_ENDING_OVERDUE)` cho 010 (lần đầu từ ngày gắn tới ngày chạy, các ngày sau mỗi ngày một lần). `billingStopDate` của `ResidencyDayApi` chưa cài (thuộc dịch vụ ngày lưu trú, T067 trở đi); khi cài đọc `residency_ending.expected_end_date` của hồ sơ Đang chuẩn bị.
  8. **Hợp đồng api-004 giữ nguyên**: `ResidencyEnding` không có `medicalReason`, `requestedBy`, `reason` (có trong glossary 10.2 nhưng không có trong yaml); đã lưu ở bảng, chưa trả ra. `endCase` của thân phản hồi là chuỗi (yaml không liệt kê enum); thân yêu cầu dùng enum bốn giá trị (không có `DEATH`). `conditionCode` của thân lập ngoại lệ là chuỗi có `allowableValues` để mã ngoài danh sách (bàn giao, xác nhận quyết toán) trả 422 như hợp đồng.
  9. **Lập hồ sơ khi chưa có `FinalPeriodPort` thật** (010): 409 `CONDITION_NOT_MET` mã `FINAL_PERIOD_UNAVAILABLE` (không lập hồ sơ mà thiếu bảng kỳ cuối nháp, FR-066). BL-79.
  10. **Chưa làm**: `LuuTruProvider` cho `M02.HO_SO_KET_THUC` (cùng bàn giao, ngoại lệ) như các bảng lượt vắng ở Phase 8; nhắc hợp đồng ngắn ngày vào ngày kết thúc chưa lọc "chưa có hồ sơ kết thúc đang mở" (FR-026, `ContractReminders`); `Hủy tiếp nhận` chưa gọi `ResidencyChangeClosing`.
- **Bẫy**: MockMvc ném `ServletException` (không trả 500) khi bản giả `FinalPeriodPort` lỗi; test dùng `assertThatThrownBy(...).hasRootCauseMessage`. Trigger `trg_contract_lock_content` chặn `UPDATE contract SET end_date` ở hợp đồng ngoài Nháp: test chỉ đặt `current_end_date`. `DATETIME(6)` lưu UTC: dùng `LocalDateTime.ofInstant(..., UTC)`, không dùng `Timestamp.from`.

## PHASE 10 HANDOFF (2026-10-26)

- Phase 9 backend xong, chờ chủ dự án duyệt checkpoint; chưa commit, chưa push. Bắt đầu Phase 10 (US8, T062 → T065, T109) chỉ khi được yêu cầu: `git status`, test trước, migration `V004_09__death_record.sql` (data-model mục 10).
- Đã có để dùng lại: `AbsenceTransition.closedByFinalState(residentId, closeAt, cause)` (đóng lượt vắng, ghi nốt ngày), `ResidencyChangeClosing.closeAll(residentId, ruleCode)` (đóng yêu cầu thay đổi lưu trú), `WaitingListTransition.cancelled(result)`, `ContractTransition` (thêm hàm Chấm dứt "Qua đời" theo mẫu `residencyEnded`), `resident_residency.closed_at` (đặt một lần, `UPDATE … WHERE closed_at IS NULL`), `ending_condition_exception.ending_id` rỗng được kèm `CHECK` (T109 chỉ còn endpoint, handler nhận căn cứ danh sách việc), `EndingExceptionHandler` (mở rộng nội dung `{residentId, basis = POST_DEATH, conditionCode}`), đối tượng kiểm quyền `M02.ENDING_EXCEPTION.SETTLEMENT` (chú thích 39), `ResidencyEndingReadApiImpl.settlementInfo` (thêm nhánh `DEATH`, `hasApprovedSettlementException` thêm căn cứ danh sách việc).
- Khi qua đời mà hồ sơ kết thúc còn Đang chuẩn bị: chuyển `CANCELLED` (lý do "Qua đời"), `FinalPeriodPort.cancelStop` không gọi (mốc dừng là ngày qua đời), chép điều kiện chưa đạt sang danh sách việc (research T-13); trigger đã cho phép chuyển từ `PREPARING`.
- Blocker mới: BL-77 (bàn giao không có mã bản ghi đón), BL-78 (A: liệt kê yêu cầu mở theo người), BL-79 (E/010: `FinalPeriodPort` thật), BL-80 (chú thích 39 cho Kế toán xem), cùng BL-76 (bố cục web), BL-73, BL-74, BL-75 như trước.
- Task Phase 9: `[x]` T057, T058, T060, T108; `[ ]` T059 (thiếu `BanGhiDonPort.daDung`), T061 (web), T106 (nhánh qua đời), T107 (web). Chủ dự án duyệt checkpoint (2026-10-26): chưa thêm `pickupRecordId` vào api-004, BL-78 chờ A, BL-79 chờ E/010, giữ cách cài BL-80, không sửa `V004_08`.
- Verify cuối Phase 9: `./mvnw clean verify` JDK 25, MySQL 3307: 301 đơn vị + 1242 IT, 0 lỗi, 0 bỏ qua, BUILD SUCCESS. Đã commit và push `impl/004-admission-residency` (HEAD gốc `da77a46`; hash cuối ở `git log`), chưa PR. Bắt đầu Phase 10: đọc tasks.md T062 → T065, T109, test trước.
- Chạy test: `DB_URL`, `TEST_DB_URL` về 3307 (mysqld 3307 dùng datadir cũ ở scratchpad phiên 221e9972, đã bật sẵn); `spotless:apply` trước khi `verify` (file cũ ở working tree dùng CRLF, file mới LF: spotless chuẩn hóa).


## Ghi chú triển khai Phase 10 (US8, 2026-10-26, chờ chủ dự án duyệt checkpoint)

- **Đã làm**: T062, T063, T064, T106 `[x]`. T065, T107, T109 `[ ]`: backend xong, còn màn web (nút "Lập ngoại lệ quyết toán" ở màn danh sách việc, màn `qua-doi/`) chờ chủ dự án duyệt bố cục DESIGN.md mục 14 (BL-76). Tên theo glossary 10.16.
- **Kiểm**: `./mvnw clean verify` (JDK 25, MySQL 3307): 301 đơn vị + 1272 tích hợp, 0 lỗi, 0 bỏ qua, BUILD SUCCESS (Phase 9: 301 + 1242). Test mới: `RecordDeathEffectsIT` (8), `PostDeathTaskIT` (8), `BR_M02_21_CauseOfDeathIT` (6), `PostDeathSettlementExceptionIT` (8) và nền `DeathTestBase`; `m01.RecordDeathIT` đã tồn tại nên lớp mới tên khác.
- **Đã cài**: migration `V004_09__death_record.sql`; `DeathTransition` (do `M02StatusParticipant` gọi cho `RECORD_DEATH`: bản ghi, tệp bằng chứng gắn `M02.DEATH_EVIDENCE`, hợp đồng Chấm dứt "Qua đời" qua `ContractTransition.residentDied`, đóng lượt vắng tại thời điểm qua đời, hồ sơ chờ Hủy chờ, yêu cầu thay đổi đóng, hồ sơ kết thúc Đang chuẩn bị sang Đã hủy "Qua đời" không gọi `cancelStop`, phát `DeathRecorded` và `ResidencyEndingCancelled`); `PostDeathTaskService` (`PostDeathTaskApi`, tính lại, đặt `resident_residency.closed_at` một lần, danh sách đóng băng sau đó); `PostDeathReminders` (`POST_DEATH_TASKS`, CFG-M02-09); `DeathRecordService`, `DeathRecordCorrectionProvider` (`M02.DEATH_RECORD`), `DeathEvidenceFilePermission`; `DeathController` 4 endpoint khớp api-004 (đã bớt khỏi `pending` của `ApiContractIT`); `EndingExceptionHandler` nhận `basis = POST_DEATH`; `ResidencyEndingReadApiImpl` trả `DEATH` kèm ngày qua đời và ngoại lệ không gắn hồ sơ.
- **Sáu quyết định chờ chủ dự án duyệt (BL-82; mục 1 → 6) và hai ghi chú kỹ thuật (mục 7, 8). Code giữ nguyên như đã kiểm thử, chưa đổi nghiệp vụ**:
  1. Đính chính bản ghi qua đời chỉ nhận `cause`, `place`, `discoveredBy`, `confirmedById` và chỉ `EDIT_CONTENT`; hủy (`CANCEL_RECORD`) và sửa `deathAt`, `evidenceFileId` bị 422 vì kéo theo lượt vắng, hợp đồng, tính phí đã chốt theo thời điểm đó.
  2. Hợp đồng Nháp, Chờ ký giữ nguyên khi qua đời (như Kết thúc lưu trú); chỉ hợp đồng Hiệu lực Chấm dứt. Spec FR-070 chỉ nêu "hợp đồng chuyển Chấm dứt".
  3. Module chưa cài `PostDeathItemPort` cho một mục thì mục đó Chưa hoàn thành, không đóng hồ sơ khi chưa biết trạng thái.
  4. Ngoại lệ quyết toán sau qua đời chỉ lập/duyệt/áp dụng khi danh sách còn mở và mục quyết toán Chưa hoàn thành (409 `POST_DEATH_LIST_CLOSED`, `POST_DEATH_SETTLEMENT_NOT_OPEN`); không có bản ghi qua đời thì 404.
  5. Tệp bằng chứng chỉ tải được bởi người đã ghi nhận lệnh (`DeathEvidenceFilePermission`); người khác xem qua màn bản ghi.
  6. **VẤN ĐỀ QUYỀN, cần duyệt trước khi đổi**: Hành chính không xem được nguyên nhân qua đời (FR-069, BR-M02-21) nhưng `DeathRecordCorrectionProvider.canRecord` cho Hành chính (cùng Bác sĩ) đính chính `cause`, tức sửa "mù"; cách A cho người ghi gốc, cách C qua yêu cầu phê duyệt. Phương án: chỉ Bác sĩ đính chính `cause` (Hành chính vẫn sửa nơi xảy ra, người phát hiện, người xác nhận), hoặc giữ như hiện tại.
  7. Điều kiện kết thúc chưa đạt của hồ sơ bị hủy không chép thành dòng riêng: danh sách việc luôn tính từ port của module sở hữu nên đã chứa chúng (research T-13).
  8. api-004 giữ nguyên: bỏ khai báo 422 (correct) và 404 (post-death-tasks) khỏi annotation vì `ApiContractIT` so từng phản hồi; hành vi vẫn trả các mã đó. Bộ lọc `/residents` của `ResidentContractIT` (m01) loại hai endpoint qua đời của 004.
- **Blocker môi trường thật** (BL-81: `PostDeathItemPort` do D/E cài; không tự tạo thay họ; T011 `LuuTruProvider` cho `death_record`, `post_death_task` chưa làm): 010, 013, 006, 007, 017 (E, D) chưa cài `PostDeathItemPort` nên ngoài test mọi mục Chưa hoàn thành và hồ sơ lưu trú sau qua đời chưa đóng; `LuuTruProvider` cho `death_record`, `post_death_task` thuộc T011 chưa làm; BL-76 bố cục web; các blocker BL-66, BL-77 → BL-80 giữ nguyên, chưa quyết định thay chủ dự án.
- **Bẫy**: `DATETIME(6)` cắt về micro giây: ghi `Instant` phải `truncatedTo(MICROS)` nếu giá trị trả về so với giá trị đọc lại (đồng hồ test có nano giây).

## PHASE 11 HANDOFF (2026-10-26)

- Phase 10 backend xong, chờ chủ dự án duyệt checkpoint; chưa commit, chưa push. Bắt đầu Phase 11 (US9, dịch vụ và phiên bản đơn giá) chỉ khi được yêu cầu: `git status`, đọc tasks.md Phase 11, test trước.
- Task Phase 10: `[x]` T062, T063, T064, T106; `[ ]` T065, T107, T109 (chỉ còn phần web, chờ duyệt bố cục DESIGN.md mục 14, BL-76).
- Đã có ở Phase 10: migration `V004_09__death_record.sql` (không sửa), `internal/death` (`DeathTransition`, `PostDeathTaskService`, `PostDeathReminders`, `DeathRecordService`, `DeathRecordCorrectionProvider`, `DeathEvidenceFilePermission`), `DeathController` 4 endpoint khớp api-004 trong `ApiContractIT`, `ContractTransition.residentDied`, `EndingExceptionHandler` nhận `basis = POST_DEATH` (ngoại lệ quyết toán sau qua đời, FR-071a), `ResidencyEndingReadApiImpl` trả `DEATH`. Test: `RecordDeathEffectsIT`, `PostDeathTaskIT`, `BR_M02_21_CauseOfDeathIT`, `PostDeathSettlementExceptionIT`, nền `DeathTestBase`.
- Blocker: BL-81 (E, D cài `PostDeathItemPort`), BL-82 (6 quyết định, nổi bật là quyền đính chính nguyên nhân ở mục 6), BL-76 (bố cục web), BL-73 → BL-75, BL-77 → BL-80, BL-66 như trước; T011 (`LuuTruProvider`) chưa làm.
- Task các Phase trước còn mở: T010, T011, T019, T028, T038 (web), T040 (E/017), T021, T025, T026, T027, T033 (C/003), T036, T042, T043, T047, T049 (phần trưởng đoàn, người đi cùng), T053, T054, T056, T059 (`BanGhiDonPort.daDung`, BL-77), T061, T107 (web), T065, T109 (web).
- Bắt đầu Phase 11 (US9, dịch vụ và phiên bản đơn giá): đọc tasks.md Phase 11 → plan → data-model → contracts → research (thứ tự CLAUDE.md), `git status` sạch, test trước phải đỏ, `ServiceCatalog` đã có từ Phase 2 nên chỉ thêm lệnh dịch vụ và phiên bản đơn giá; không tạo `PostDeathItemPort` thay D/E.
- Verify cuối: `./mvnw clean verify` JDK 25, MySQL 3307: 301 đơn vị + 1272 IT, 0 lỗi, 0 bỏ qua, BUILD SUCCESS; không đổi mã nguồn sau lần verify xanh.
- Git: nhánh `impl/004-admission-residency`, HEAD trước Phase 10 là `43f0dda`; năm commit Phase 10 đã push (hash cuối ở `git log`), chưa PR. Stash `CP-001-P2-IMPL` giữ nguyên.
- Chạy test: `DB_URL`, `TEST_DB_URL` về 3307; `spotless:apply` trước `verify`.

## Ghi chú triển khai Phase 11 (US9, 2026-10-27, chờ chủ dự án duyệt checkpoint)

Backend T066, T067 xong; T068 xong phần backend, màn web `frontend/src/features/` dịch vụ và đơn giá chờ duyệt bố cục DESIGN.md mục 14 (BL-76). Tên lớp ở glossary 10.17. Quyết định kỹ thuật (BL-83):

1. **Cấu trúc.** `m02.internal.servicecatalog.{domain, persistence, service}` (cùng gói `UnitPriceService` của Phase 2): `ServiceCatalogService` (lệnh của Quản lý viện), `ServiceCatalogStore`; `UnitPriceService` giữ nguyên (đọc cho 010). `ServiceController` ở `m02.web`. Không thêm migration (`V004_02` đã có bảng, cột sinh, trigger).
2. **Quyền.** `SERVICE_PRICE`: AC-01 xem, tạo, sửa; AC-09 chỉ xem (Phụ lục 27). `GET …/mismatched-price-contracts` chỉ Quản lý viện (kiểm vai trò trong service như `decideBedHold`, 403 với vai trò khác; spec FR-036 "cho Quản lý viện xem", hợp đồng api-004 ghi chú tương tự); `MismatchedPriceContractsIT` có test Hành chính bị từ chối.
3. **Ràng buộc theo loại (không có trong hợp đồng, rút từ research T-05).** `CARE` bắt buộc `careLevelCode`; `RESIDENCY_FEE` bắt buộc `roomTypeId`; `FACILITY_SUPPLIED_MEDICATION` bắt buộc `medicationId`; loại khác không được mang các trường này (422). Căn cứ: research T-05 (loại kèm `ma_muc`/`loai_phong_id`/`thuoc_id`), Q-297; `UnitPriceApi.residencyFeeServiceOf`, `facilityMedicationServiceOf` chỉ tìm được qua các cột đó. Chủ dự án giữ (2026-10-27).
4. **Sửa dịch vụ.** `type`, `careLevelCode`, `roomTypeId`, `medicationId` không đổi sau khi tạo (409 `SERVICE_FIELD_LOCKED`); `billingPeriod` không đổi khi có phiên bản đơn giá đã tham chiếu; dịch vụ đã ngừng hiệu lực không sửa (409 `SERVICE_INACTIVE`). Chỉ `name`, `unit` luôn sửa được. Không có lệnh kích hoạt lại (hợp đồng không có): tạo dịch vụ mới, tên Hiệu lực không trùng (BR-M15-06) nên dùng lại được tên cũ.
5. **ETag.** Hợp đồng `PUT`, `deactivate` có `If-Match` nhưng `Service` không có phiên bản để lấy; thêm `version` vào schema `Service` của `api-004.yaml` (chủ dự án duyệt 2026-10-27, trường thêm nên không phá client hiện có); `ServiceBody.version` cũng là ETag của `POST` (201), `PUT`, `deactivate` (200, không thân). Hợp đồng `PUT /services/{id}` và `deactivate` thêm phản hồi 409, 412 (`Problem`) cho khớp hành vi thực; `ServiceController` khai tương ứng, `ApiContractIT` xanh.
6. **Phiên bản đơn giá (`createPriceVersion`).** Khóa dòng `service` rồi chia ba nhánh: (a) không có `effectiveTo` và dịch vụ đang có phiên bản mở bắt đầu trước `effectiveFrom`: đóng phiên bản đó ở ngày liền trước (`EffectiveVersioning.addVersionFrom`), **`effectiveFrom` phải sau hôm nay**, nếu không 409 `PRICE_VERSION_OVERLAPS_APPLIED` (không rút ngắn phần đã áp dụng); cho phép kể cả khi phiên bản mở đã `referenced` (trigger chỉ cho đặt `effective_to` khi đang `NULL`); (b) không có `effectiveTo` và không có phiên bản mở trước đó (hiệu lực lùi vào khoảng trống): `effectiveTo` tự đặt ở ngày liền trước phiên bản kế tiếp, không có phiên bản kế tiếp thì để mở; (c) có `effectiveTo`: khoảng phải không giao phiên bản nào. Giao khoảng: 409 `BR-M15-07` (từ `EffectiveVersioning`). Giá phải là số nguyên không âm (hợp đồng `int64`; 400 khi âm). Tạo xong phát `PriceVersionCreated(serviceId, effectiveFrom, effectiveTo)` cho 010 và ghi nhật ký. Hạn chế: phiên bản có `effectiveTo` rõ ràng không tách đôi phiên bản mở đang có (cần ngày kết thúc thì đóng phiên bản mở trước).
7. **Hợp đồng lệch giá.** So `contract_service.recorded_unit_price` của hợp đồng `ACTIVE` với đơn giá của phiên bản có `effective_from` lớn nhất (kể cả phiên bản tương lai). Hợp đồng chưa có đơn giá ghi, hợp đồng Nháp, Đã kết thúc không liệt kê. Giá riêng đã duyệt (FR-025a) cũng nằm trong `recorded_unit_price` nên hợp đồng đó luôn hiện trong danh sách: giữ vì FR-036 nói đúng "đơn giá khác phiên bản mới", không có ngoại lệ cho giá riêng (ghi chú vào mô tả endpoint của hợp đồng); nếu nghiệp vụ muốn loại thì cần chủ dự án quyết. Không đổi gì trên hợp đồng.
8. **Nhật ký.** `M02_SERVICE_CREATED`, `M02_SERVICE_UPDATED`, `M02_SERVICE_DEACTIVATED` (bắt buộc lý do), `M02_SERVICE_PRICE_VERSION_CREATED`.
9. **Test (TDD).** Test viết trước nhưng chưa chạy riêng trạng thái đỏ trước khi cài (viết cùng lượt với mã); đã chạy xanh: `ServiceCatalogIT` 9, `DBR_08_PriceVersionIT` 8, `MismatchedPriceContractsIT` 2; `ApiContractIT` bớt 7 endpoint khỏi `pending`. Một lỗi bắt được ở lần chạy đầu: lệnh chèn dùng `effectiveTo` gốc thay giá trị đã cắt, trigger `trg_price_version_no_overlap_insert` chặn (đã sửa).

## PHASE 12 HANDOFF (2026-10-27)

- Phase 11 backend xong, chờ chủ dự án duyệt checkpoint; chưa commit, chưa push. Phase 12 (Polish, T069 → T074) chỉ bắt đầu khi được yêu cầu.
- Task Phase 11: `[x]` T066, T067; `[ ]` T068 (chỉ còn màn web, chờ duyệt bố cục DESIGN.md mục 14, BL-76).
- BL-83 đã xử lý (chủ dự án duyệt 2026-10-27): `version` vào `Service`, ràng buộc loại dịch vụ giữ, danh sách hợp đồng lệch giá chỉ Quản lý viện (sửa, có test từ chối Hành chính), 409/412 thêm vào hợp đồng; không còn quyết định chờ duyệt. Giữ BL-66, BL-73 → BL-82, T011, và các task mở ở PHASE 11 HANDOFF.
- Verify cuối Phase 11: `./mvnw clean verify` JDK 25, MySQL 3307: 301 đơn vị + 1291 IT, 0 lỗi, 0 bỏ qua, BUILD SUCCESS.
- API: `/services` (GET, POST), `/services/{id}` (PUT), `/deactivate`, `/price-versions` (GET, POST), `/mismatched-price-contracts` (GET, chỉ Quản lý viện); service `ServiceCatalogService`; sự kiện `PriceVersionCreated`. Test: `ServiceCatalogIT`, `DBR_08_PriceVersionIT`, `MismatchedPriceContractsIT`.
- Phase 12 (T069 → T074) cần 005, 006, 007, 010, 012, 013, 017 có bản thật cho T072 (giữ `[ ]` nếu chưa); T073 chạy cả npm test, flutter test.
- Chạy test: MySQL 3307 (datadir cũ ở scratchpad phiên 221e9972), `DB_URL`, `TEST_DB_URL` về 3307; `spotless:apply` trước `verify`.
