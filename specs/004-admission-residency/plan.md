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
