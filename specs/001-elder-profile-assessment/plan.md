# Implementation Plan: Hồ sơ và đánh giá người cao tuổi

**Branch**: `001-elder-profile-assessment` | **Date**: 2026-10-07, cập nhật 2026-10-18, 2026-10-22 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-elder-profile-assessment/spec.md`

## Summary

Feature 001 là module M01. Nó cung cấp:
1. Hồ sơ cá nhân người cao tuổi, tra cứu theo quyền, liên kết hồ sơ của người quay lại (mục A; UC-01, UC-02).
2. Bản đồng ý chia sẻ dữ liệu, là căn cứ cho chú thích ¹ của ma trận quyền (mục B; UC-03).
3. Hồ sơ sức khỏe ban đầu; dị ứng, bệnh nền, tiền sử bệnh lưu thành từng mục chỉ loại trừ (mục C; UC-04).
4. Phiếu nguyện vọng cuối đời có phiên bản (mục C1; UC-82); phiếu nguyện vọng khi điều trị có phiên bản (mục C1a; UC-103, bổ sung 2026-10-22).
5. Đánh giá đầu vào và đánh giá lại bằng nhận xét, lưu ý chăm sóc, mức và nhu cầu chăm sóc; báo đánh giá lại (mục D, E; UC-05 → UC-07).
6. Máy trạng thái người cao tuổi: mọi lệnh chuyển trạng thái, điều kiện chặn, lịch sử, mức chăm sóc hiện hành (mục F, G; UC-08, cùng UC-15 → UC-18 của 004).

Hướng kỹ thuật:
- Một điểm thực hiện chuyển trạng thái, khóa dòng hồ sơ. Các module có điều kiện hoặc tác động cài SPI `ThamGiaChuyenTrangThai`. Điều kiện được gộp và trả đủ trong một lần. Tác động chạy trong cùng transaction, nên hoặc toàn bộ, hoặc không (research H-02).
- Quy tắc đếm và quy tắc bắt buộc đưa xuống MySQL bằng cột sinh có `UNIQUE`, `CHECK` và trigger. Các quy tắc gồm: CCCD duy nhất trong hồ sơ chưa ở trạng thái cuối; tối đa một bản đồng ý, một phiên bản nguyện vọng, một bản nháp đánh giá; đánh giá xác nhận có nhận xét và mức (H-04, H-06, H-09, H-10).
- Kiểm tra dị ứng với đơn thuốc và chế độ ăn chạy sau commit qua outbox, có cờ "chờ kiểm tra" và job thử lại. Mục dị ứng không bao giờ mất vì 006 hay 011 lỗi (H-07).
- "Cần đánh giá lại" là trạng thái tính được từ lần báo; không có bản ghi yêu cầu (Q-307, H-11).
- Thêm thành phần tệp đính kèm dùng chung `common.tep` cho ảnh và bản scan (H-13).

Plan tuân theo FR-010a của 000 (Q-306, Q-307): M01 không tự sinh bản ghi nghiệp vụ, không gợi ý. Bảy job chỉ báo, nhắc, cảnh báo theo thời gian. Mọi thay đổi khác là lệnh của actor, hoặc tác động kèm lệnh đó.

### Cập nhật 2026-10-18 (spec mục "Cập nhật 2026-10-18": Q-347, Q-348, Q-353, Q-355, Q-356)

- **Bản đồng ý** (FR-010, FR-011, FR-014): người ký thay chỉ là người đại diện theo pháp luật, kèm số, ngày, Tòa án và bản scan quyết định của Tòa án; bỏ `ly_do_dai_dien` và lời gọi `laNguoiDaiDienHieuLuc` (research H-06, data-model mục 4). Q-355 (chốt 2026-10-19): không có đường ký thay khi chưa có quyết định của Tòa án.
- **Phiếu nguyện vọng** (FR-023a → FR-023c): `du_nang_luc` thay bằng nhận định gắn với phiếu (`tu_quyet_duoc`, thời điểm, cách hỗ trợ hiểu, ngày xem lại); người ký thay là người đại diện của người bệnh (kèm cách chọn) hoặc người đại diện theo pháp luật; ý kiến khác lựa chọn là bảng con `nguyen_vong_y_kien` có nguồn (research H-09, data-model mục 6).
- **Nguồn của ý kiến dùng chung** (FR-023g): `NguonYKien`, `NguonYKienApi.kiemTra` ở `m01.api`, mẫu cột ở data-model mục 6a, schema `NguonYKien` ở api-001.yaml; 004, 005, 012, 014 dùng lại (research H-16).
- **Không có dấu năng lực** (FR-023h): test kiến trúc quét schema và OpenAPI (research H-16).
- **Nhận thông báo** (Q-353): M01 không đổi cách lưu `dong_y_thong_bao`; 009 lọc người nhận (plan 009).
- **Q-356**: điều kiện, cảnh báo và yêu cầu lý do của Hoàn tất tiếp nhận về ý kiến vào ở do bean `ThamGiaChuyenTrangThai` của 004 cung cấp qua `canhBao` (plan 004); M01 không đổi SPI (điểm báo lại 12).

### Cập nhật 2026-10-18, lượt 3 (spec mục "Cập nhật 2026-10-18, lượt 3": Q-366)

- **Mục ý kiến ghi từ phản hồi Mong muốn** (FR-023g): mẫu cột nguồn ý kiến (data-model mục 6a) có thêm cột tùy chọn `phan_hoi_id NULL`, chỉ bảng có lệnh ghi mục từ phản hồi mới dùng; khi lệnh ghi mục có `phanHoiId`, module ghi mục phát sự kiện chung `YKienDaGhiTuPhanHoi` khai báo ở `m01.api`; 012 nghe để gắn mục vào phản hồi (research H-17). Hiện có hai nơi ghi: 012 (mong muốn về khách thăm, người đón) và 014 (sở thích, gồm thói quen theo Q-363).

### Cập nhật 2026-10-22 (spec mục "Cập nhật 2026-10-22": Q-383, Q-384)

- **Phiếu nguyện vọng khi điều trị** (FR-023j → FR-023m, BR-M01-16, DBR-39, UC-103): bảng `nguyen_vong_dieu_tri` nhóm 2 có phiên bản (`HIEU_LUC` / `HET_HIEU_LUC`, cột sinh `dang_hieu_luc` `UNIQUE`), bảng con chỉ thêm `nguyen_vong_dieu_tri_nguoi_bao` (người cần báo, 0..n người thân có quan hệ Hiệu lực, chủ dự án chọn 2026-10-22) và `nguyen_vong_dieu_tri_y_kien` (ý kiến gia đình khác ý kiến người cao tuổi, mẫu cột nguồn mục 6a). Endpoint `GET/POST /nguoi-cao-tuoi/{id}/nguyen-vong-dieu-tri`; khóa quyền `DI_UNG_BENH_NEN` (dòng "Dị ứng, bệnh nền | UC-04, 103"), nội dung nhóm `SUC_KHOE`; hồ sơ có cờ `coNguyenVongDieuTri` cho Hành chính, Kế toán không thấy (research H-18, data-model mục 6b).
- **Cảnh báo khi Hoàn tất tiếp nhận** (FR-023l): `ThamGiaM01` thêm cảnh báo `THIEU_NGUYEN_VONG_DIEU_TRI` vào `canhBao`, không chặn, không nhắc định kỳ (không thêm job).
- **Cho 007** (FR-023): `NguyenVongApi.dieuTriHieuLuc(nctId)` cho thẻ thông tin khẩn cấp, lệnh Chuyển viện và phiếu tóm tắt chuyển viện (module-interfaces mục 1.8).
- **Giấy tờ khi trở về từ bệnh viện** (FR-047c, BR-M05-32): lệnh `GHI_NHAN_TRO_VE` từ `DIEU_TRI_BENH_VIEN` nhận `tepDinhKemIds` (không bắt buộc); tệp gắn vào dòng `lich_su_trang_thai_nct` (loại chủ sở hữu `M01.GIAY_TO_BENH_VIEN`); 006 đọc qua `TrangThaiNguoiCaoTuoiApi.giayToDinhKem(lichSuId)` làm căn cứ đối chiếu thuốc (BR-M07-16). Cùng cơ chế cho lệnh `CHUYEN_VIEN` do 007 gọi (giấy chuyển viện, plan 007).
- Không thêm thư viện, vai trò, trạng thái người cao tuổi hay job.

## Technical Context

**Language/Version**: Java 25; JavaScript (JSX); Dart 3.x với Flutter stable 3.x (000 R-01).

**Primary Dependencies**: Như 000 và 002: Spring Boot 4.1.x, Spring Modulith 2.x, Flyway (bản do Spring Boot quản lý), springdoc-openapi 3.x, ShedLock, Spring Security 7 (002). Web: React 19, TanStack Query. App: Flutter. Thêm AWS SDK for Java v2 (`software.amazon.awssdk:s3`) cho tệp đính kèm *(Sửa 2026-10-19, chủ dự án chọn AWS S3)*.

**Storage**:
- MySQL 8.0 (000 R-01). Bảng ở [data-model.md](data-model.md).
- Cột so khớp họ tên dùng collation `utf8mb4_0900_as_ci` (H-04).
- Tệp đính kèm lưu trong bucket AWS S3 riêng tư, siêu dữ liệu ở bảng `tep` của MySQL (H-13) *(Sửa 2026-10-19, chủ dự án chọn AWS S3)*.

**Testing**:
- Như 000 R-14 và 002 A-14.
- Bean giả cho mọi `ThamGiaChuyenTrangThai` của module chưa có (CHK035, CHK045).
- Test tích hợp MySQL cho trigger, cột sinh và `CHECK`; đồng hồ giả lập cho CFG-M01-01, 02, 03, 06 và CFG-M03-02.
- Test đồng thời cho FR-013, FR-048.

**Target Platform**: Như 000.
- Web: mọi màn của 001.
- App nhân viên: tra cứu, thẻ người cao tuổi, ghi mục sức khỏe, lập nháp đánh giá, các lệnh trạng thái của Trưởng tầng, Điều dưỡng, Bác sĩ.
- App người thân: xem hồ sơ (phần sức khỏe theo chú thích ¹).
- Lệnh của 001 chỉ chạy trực tuyến. 8.6 chỉ cho ghi tạm với "ghi nhận thực hiện", không có lệnh nào của 001.

**Project Type**: Web service (REST `/api/v1`), một web client và một mobile client chung (Q-346).

**Performance Goals**:
- Lệnh và tra cứu nằm trong 2 giây của NFR-02:
  - mỗi lệnh trạng thái khóa một dòng, rồi gọi khoảng 10 bean, mỗi bean vài truy vấn có chỉ mục;
  - tra cứu dùng chỉ mục `(ho_ten_so_khop, ngay_sinh)`, `ma_ho_so`, `cccd_dang_mo` trên tối đa vài nghìn hồ sơ (300 đang ở, cộng hồ sơ cũ).
- Thẻ người cao tuổi của checklist (005) lấy lưu ý chăm sóc theo lô qua `DanhGiaApi.luuYHienHanh`, đóng góp vào mốc 3 giây mở checklist của NFR-02.

**Constraints**:
- Không có thao tác sửa trạng thái; mỗi lệnh một endpoint (constitution XI).
- Nhật ký trong cùng transaction (000 FR-047).
- Không có `UPDATE`, `DELETE` trên bản ghi nhóm 3 đã xác nhận, cũng không có trên nội dung mục sức khỏe (constitution XII, DBR-04).
- CFG đọc lúc chạy (constitution XIII).
- Không tự sinh bản ghi nghiệp vụ (000 FR-010a).

**Scale/Scope**:
- 300 người cao tuổi đang ở (NFR-01).
- 8 trạng thái, 11 lệnh chuyển; 9 module tham gia chuyển trạng thái.
- 7 job (thêm `NhacXemLaiNhanDinh` 2026-10-18, Q-360).
- Khoảng 37 endpoint (contracts/api-001.yaml; thêm hai endpoint phiếu nguyện vọng khi điều trị 2026-10-22).

Không còn mục NEEDS CLARIFICATION. Các lựa chọn kỹ thuật đã chốt ở [research.md](research.md). Các điểm về tài liệu nguồn ghi ở "Điểm cần báo lại"; chúng không chặn thiết kế.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Bằng chứng |
| --- | --- | --- |
| I. Nguồn gốc, truy vết | Đạt | Mọi quyết định dẫn FR, BR, DBR; data-model mục 12 là bảng truy vết DBR-01 → 05, 23, 25, 34, 39 và BR-M01-01 → 16 (BR-M01-14, 15 phần của 001, bổ sung 2026-10-18; BR-M01-16, DBR-39 bổ sung 2026-10-22) |
| II. Spec không nêu công nghệ | Đạt | Spec không đổi; công nghệ chỉ ở plan, research |
| III. Không mặc định CRUD | Đạt | Nhóm 2 (hồ sơ, bản đồng ý, mục sức khỏe, nguyện vọng) chỉ có lệnh. Nhóm 3 (lịch sử, đánh giá, hồ sơ sức khỏe ban đầu sau xác nhận) chỉ thêm và đính chính. "Sửa nháp" chỉ khi chưa xác nhận. Danh mục theo mẫu nhóm 1 |
| IV. Trạng thái kiểm tra được | Đạt | Bảng chuyển ở data-model mục 2, 4 → 7 khớp hai bảng trạng thái của spec; mỗi kịch bản có test (quickstart mục 4) |
| V. Không cố định giá trị | Đạt | CFG-M01-01, 02, 03, 06, CFG-M03-02 đọc từ `ThamSoApi`. Chu kỳ job 15 phút, giới hạn tệp là thông số kỹ thuật (Complexity Tracking) |
| VI. Vai trò, quyền | Đạt | Khóa quyền theo dòng Phụ lục 27 (H-05); 19.3 qua `@NhomTruong` của 002; không thêm vai trò. Chuyển viện chưa có dòng ở Phụ lục 27, tạm dùng UC-16 và BR-M01-06, báo lại ở điểm 1 |
| VII. Điểm chưa rõ | Đạt | Không tự đoán nghiệp vụ. Chỗ nguồn còn thiếu ghi ở "Điểm cần báo lại"; thiết kế chọn cách hẹp nhất (ví dụ chỉ liên kết hồ sơ qua CCCD) |
| VIII. Tiêu chí thành công | Đạt | SC-001 → SC-014 có cách đo ở quickstart mục 5 |
| IX. Phạm vi giai đoạn | Đạt | Chỉ tài liệu thiết kế; không mã, không `src/`, không cấu hình build, không SQL; không đổi nghiệp vụ. Hợp đồng của 000 được bổ sung giao diện (`TepApi`, `chuyenMoRong`), không đổi hành vi đã thiết kế |
| X. Kiến trúc | Đạt | Một khối; gói `m01`; module khác chỉ dùng `m01.api`; điều kiện, tác động chéo module đi qua SPI. REST `/api/v1`; MySQL; hai client (Q-346). NFR ở bảng bên dưới |
| XI. Nghiệp vụ ở backend | Đạt | Mọi điều kiện chặn, bảng chuyển, quyền, giới hạn trường ở backend; `lenhDuocPhep` chỉ để hiển thị; mỗi lệnh trạng thái có `POST …/{id}/<lenh>` riêng |
| XII. Dữ liệu | Đạt | Trigger theo mẫu 000 cho mọi bảng nhóm 3 và cho nội dung mục sức khỏe; cột sinh `UNIQUE` cho DBR-01, FR-013, DBR-34, BR-M01-13; `CHECK` cho DBR-05; nhật ký trong transaction; thời gian theo `Clock` |
| XIII. Tham số | Đạt | Như V |
| XIV. Ngoại tuyến, thông báo | Đạt | Lệnh của 001 chỉ trực tuyến (ngoài phạm vi 8.6). Mọi thông báo là sự kiện tới 009, có mức, người nhận, khóa sự kiện (H-14) |
| XV. Truy vết, kiểm thử | Đạt | Tên test theo BR, FR; mỗi BR-M01-01 → 13 có test tích hợp (data-model mục 12). Không có BR nào của 001 thực hiện ở client |

**Kiểm lại sau Phase 1 (2026-10-22)**: Đạt. Q-383, Q-384 thêm một loại phiếu có phiên bản theo đúng mẫu nguyện vọng cuối đời, một cảnh báo trong lệnh Hoàn tất tiếp nhận đã có và tệp đính kèm cho lệnh trạng thái đã có; không thêm vai trò, trạng thái người cao tuổi, job hay thư viện. Hai điểm báo lại ở điểm 13.

**Kiểm lại sau Phase 1 (2026-10-18)**: Đạt. Cập nhật Q-347, Q-348 chỉ đổi trường của bản đồng ý và phiếu nguyện vọng, thêm bảng con chỉ thêm `nguyen_vong_y_kien` và giao diện `NguonYKienApi` theo FR-023g; không thêm vai trò, trạng thái hay lệnh. Các điểm còn thiếu ở nguồn ghi ở điểm báo lại 10 → 12.

**Kiểm lại sau Phase 1**: Đạt. Thiết kế không thêm vai trò, trạng thái hay quy tắc nghiệp vụ ngoài spec. Có ba bổ sung kỹ thuật. Thứ nhất, hai giao diện mới trên hợp đồng của 000: `TepApi`, `YeuCauPheDuyetApi.chuyenMoRong`. Thứ hai, bảng kỹ thuật `lan_bao_danh_gia_lai` lưu lần thông báo, không phải yêu cầu. Thứ ba, lệnh trạng thái không dùng `If-Match`. Cả ba được giải thích ở Complexity Tracking.

### Đáp ứng yêu cầu phi chức năng (constitution X)

| NFR | 001 chịu ảnh hưởng | Cách đáp ứng |
| --- | --- | --- |
| NFR-01 Quy mô | Có | 300 hồ sơ đang ở, vài nghìn hồ sơ cũ; chỉ mục ở data-model |
| NFR-02 Hiệu năng | Có | Lệnh trạng thái khóa một dòng và gọi bean có chỉ mục; lưu ý chăm sóc lấy theo lô cho checklist |
| NFR-03 Thông báo khẩn cấp | Có | Chuyển viện, qua đời phát sự kiện sau commit ngay trong lệnh; 009 gửi Khẩn cấp. Kiểm tra dị ứng không chặn đường ghi |
| NFR-04 Tác vụ định kỳ | Có | Bảy job chống lặp bằng khóa duy nhất hoặc cột "đã cảnh báo"; khóa sự kiện cho 009 (H-12, H-14) |
| NFR-05 Khả dụng | Gián tiếp | Job chạy lại khi khởi động; không phụ thuộc giờ giao ca |
| NFR-06 Sao lưu | Có | Tệp ở S3 có versioning (giữ bản cũ khi ghi đè, xóa nhầm); siêu dữ liệu `tep` sao lưu cùng MySQL (H-13) |
| NFR-07 Lưu trữ | Có | `LuuTruProvider` theo hồ sơ cho mọi bảng và tệp (data-model mục 11) |
| NFR-08 Bảo mật | Có | Giới hạn trường ở backend; tệp chỉ tải được khi có quyền xem bản ghi chủ sở hữu; nhật ký lượt người thân xem sức khỏe thuộc 012 (Assumptions của spec) |
| NFR-09 Thời gian | Có | Mọi mốc từ `Clock`; ngày hiệu lực của mức chăm sóc theo BR-M15-07 |
| NFR-10 Thiết bị | Có | Web đủ chức năng; app nhân viên có thẻ người cao tuổi, mục sức khỏe, lệnh trạng thái; app người thân xem hồ sơ |
| NFR-11 Ngôn ngữ | Có | Thông báo từ chối tiếng Việt, liệt kê điều kiện kèm mã BR |
| NFR-12 Cấu hình | Có | CFG đọc lúc chạy; danh mục cấu hình được |
| NFR-13 Kiểm thử | Có | Đồng hồ giả lập; bean giả bật tắt được ở profile `test`, `demo` |

## Project Structure

### Documentation (this feature)

```text
specs/001-elder-profile-assessment/
├── plan.md                      # File này
├── research.md                  # Phase 0: H-01 → H-16
├── data-model.md                # Phase 1: bảng, bảng chuyển, bảo đảm ở cơ sở dữ liệu, truy vết
├── quickstart.md                # Phase 1: kịch bản kiểm chứng, truy vết kịch bản → test
├── contracts/
│   ├── api-001.yaml             # OpenAPI 3.1: hồ sơ, trạng thái, bản đồng ý, sức khỏe, nguyện vọng, đánh giá, danh mục, tệp; schema NguonYKien dùng chung
│   └── module-interfaces.md     # Giao diện Java: 001 cung cấp / 001 cần; SPI ThamGiaChuyenTrangThai; NguonYKienApi
└── tasks.md                     # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

Cấu trúc dự kiến cho giai đoạn triển khai. Giai đoạn thiết kế không tạo các thư mục này (CLAUDE.md).

```text
backend/src/main/java/com/cambae/backend/
├── common/tep/                    # TepApi, QuyenTepProvider, job dọn tệp chưa gắn (H-13)
└── m01/
    ├── api/                       # NguoiCaoTuoiApi, TrangThaiNguoiCaoTuoiApi, MucChamSocApi, DanhGiaApi,
    │                              # MucSucKhoeApi, BanDongYApi, NguyenVongApi, ThamGiaChuyenTrangThai, sự kiện
    ├── hoso/        {internal, web}
    ├── trangthai/   {internal, web}   # ChuyenTrangThaiService, HoSoMoGuard, lịch sử, mức chăm sóc
    ├── dongy/       {internal, web}   # DieuKienChuThich "1"
    ├── suckhoe/     {internal, web}   # hồ sơ ban đầu, mục sức khỏe, danh mục
    ├── nguyenvong/  {internal, web}
    ├── danhgia/     {internal, web}   # đánh giá, báo đánh giá lại, provider đính chính
    └── lich/                          # 7 job (H-12)

frontend/src/tinh-nang/
├── ho-so-nguoi-cao-tuoi/          # tra cứu, tạo, cập nhật, lịch sử, hồ sơ liên kết, lệnh trạng thái
├── ban-dong-y/
├── suc-khoe/                      # hồ sơ ban đầu, mục sức khỏe
├── nguyen-vong/
├── danh-gia/                      # lập, xác nhận, danh sách cần đánh giá lại
└── danh-muc-m01/

mobile/lib/nhan-vien/
├── tra-cuu-nguoi-cao-tuoi/        # thẻ người cao tuổi: lưu ý chăm sóc, dị ứng, dấu kiểm tra
├── suc-khoe/
├── danh-gia/
└── trang-thai/                    # Cho tạm vắng, Ghi nhận trở về, Chuyển viện, Ghi nhận qua đời

mobile/lib/nguoi-than/ho-so/
```

**Structure Decision**: Theo R-02 của 000. Feature 001 nằm ở `backend/…/m01` với bảy gói con và gói `api` công khai. Thành phần tệp nằm ở `common/tep` vì mọi module có bản scan đều dùng (H-13). Web có đủ chức năng của 001. App nhân viên có các thao tác tại tầng. App người thân chỉ xem hồ sơ.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| SPI `ThamGiaChuyenTrangThai` gọi đồng bộ khoảng 10 bean của module khác trong transaction của M01 | FR-045 đòi liệt kê mọi điều kiện của nhiều module trong một lần; FR-047 đòi đổi trạng thái và tác động cùng thành công hoặc không có gì | M01 gọi trực tiếp API của từng module thì tạo vòng phụ thuộc; sự kiện sau commit thì vi phạm FR-047 |
| Lệnh trạng thái không dùng `If-Match` (khác quy ước R-03 "lệnh sửa bắt buộc `If-Match`") | FR-048 đòi lệnh đến sau được **kiểm tra lại** trên trạng thái mới và chỉ bị từ chối khi không còn hợp lệ. `SELECT … FOR UPDATE` làm đúng điều đó | `If-Match` sẽ từ chối lệnh đến sau kể cả khi nó vẫn hợp lệ trên trạng thái mới |
| Bảng kỹ thuật `lan_bao_danh_gia_lai`, `nhac_chu_ky`, `canh_bao_tro_lai`, `tep` được `UPDATE`, không thuộc ba nhóm của 1.5 | Lưu lần báo, lần nhắc, lần cảnh báo để tính hạn (FR-041) và chống lặp (NFR-04). Mỗi lần báo hoặc nhắc vẫn ghi `nhat_ky` (Bộ lập lịch, có căn cứ CFG). Đây không phải "yêu cầu đánh giá lại" (Q-307): không có lệnh, không có vòng đời | Suy ra từ bảng thông báo của 009 thì M01 đọc dữ liệu module khác và hạn phụ thuộc vào gửi thành công |
| Hai cột cờ `cho_kiem_tra_thuoc`, `cho_kiem_tra_an` trên `muc_suc_khoe` (nhóm 2) do 006, 011 gỡ | BR-M01-12 cần biết mục nào chưa được kiểm tra để hiện dấu và thử lại | Bảng riêng không thêm thông tin; outbox của thư viện không cho biết kiểm tra đã chạy xong ở module nhận |
| Thành phần `common.tep` do plan 001 thêm, nằm ngoài M01 | Nhiều module cần bản scan (001, 004, 007, 012, 013); quyền tải phải theo bản ghi chủ sở hữu | Mỗi module tự lưu tệp thì lặp quy tắc quyền và lưu giữ |
| Chu kỳ job 15 phút, giới hạn tệp (10 MB; PDF, JPEG, PNG), hạn dọn tệp chưa gắn 24 giờ đặt trong cấu hình ứng dụng, không phải mã CFG | Thông số vận hành, không có trong Phụ lục 25, không đổi quy tắc nghiệp vụ. Cùng cách xử lý như plan 000 | Thêm mã CFG thì phải sửa tài liệu nguồn chỉ vì thông số kỹ thuật |

## Điểm cần báo lại về tài liệu nguồn

**(2026-10-19) Lưu tệp trên AWS S3: region và nghĩa vụ chuyển dữ liệu ra nước ngoài — chờ chủ dự án chốt.** Bucket ở region nước ngoài (AWS chưa có region ở Việt Nam) chứa bản scan bản đồng ý, giấy tờ sức khỏe, ảnh người cao tuổi. Cần đối chiếu Luật 91/2025/QH15 và Nghị định 356/2025/NĐ-CP (hồ sơ đánh giá tác động chuyển dữ liệu ra nước ngoài) và chọn region; đề xuất ghi một dòng vào 24.4 của tài liệu nghiệp vụ cạnh Q-03. Không chặn thiết kế, chặn trước khi vận hành.

Plan không tự sửa các điểm dưới đây (constitution I, IX). Đề nghị chủ dự án quyết bằng mã Q mới khi cần, rồi sửa `docs/business/nghiep-vu.md` và spec 001.

1. **Phụ lục 27 không có dòng cho Chuyển viện (UC-16).** UC-16 ghi actor "Điều dưỡng, Bác sĩ" (feature 004, 007), BR-M01-06 cũng ghi vậy. Nhưng ma trận quyền không có dòng nào để 002 nạp khóa quyền. Plan tạm dùng khóa `CHUYEN_VIEN` với T cho AC-03, AC-04 (research H-05). Test `MaTranQuyenKhopNguonTest` của 002 sẽ báo lệch tới khi Phụ lục 27 có dòng này. Đề nghị: thêm dòng "Chuyển viện | UC-16" vào Phụ lục 27. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

2. **Bảng trạng thái của spec 001 thiếu điều kiện so với 5.6.** *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - Dòng "→ Kết thúc lưu trú" của 5.6 có thêm "đã bàn giao người cao tuổi" và "số dư và tiền cọc đã quyết toán (15.9), được quản lý duyệt ngoại lệ". Spec 001 không có hai điều kiện này (spec 004 có trình tự 6.8).
   - Dòng "Đang tiếp nhận → Đang lưu trú" của 5.6 có "thời điểm tiếp nhận không sớm hơn ngày bắt đầu hợp đồng (Q-22)". Spec 004 FR-075 đã có, spec 001 chưa nhắc.
   - Thiết kế không bị ảnh hưởng, vì điều kiện do bean của 004, 017 cung cấp qua SPI. Đề nghị: khi đồng bộ spec 001, bổ sung các điều kiện này vào bảng trạng thái, ghi rõ feature cung cấp.

3. **Người cấu hình một số danh mục chưa có nguồn.** *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - 006 FR-002 và 011 FR-003 nêu người ghi dị nguyên nhóm thuốc và thực phẩm.
   - Chưa có nguồn cho dị nguyên nhóm "khác", danh mục mức độ dị ứng, nguồn thông tin (BR-M01-11) và nhu cầu chăm sóc (Q-291).
   - Plan tạm cho Quản lý viện theo quyền cấu hình chung (UC-69). Đề nghị chốt bằng Q và thêm dòng vào Phụ lục 27.

4. **Spec 001 chưa nêu mức của phần lớn thông báo (009 FR-043a, Q-108).** Bảng 009 FR-043b chỉ có hai dòng của 001. Plan áp nguyên tắc 3 bậc cho các thông báo còn lại (research H-14): *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - báo cần đánh giá lại, nhắc thiếu bản đồng ý, nhắc thiếu nguyện vọng, nhắc lập yêu cầu: Nhẹ;
   - quá hạn đánh giá lại, tạm vắng quá hạn, báo Hành chính lần đầu khi mức đổi: Trung bình.
   - Người nhận "bác sĩ" của BR-M01-02 chưa rõ là bác sĩ nào; plan chọn bác sĩ trực theo phân công của 008, không có thì mọi bác sĩ.

   Đề nghị: ghi các mức và người nhận này vào spec 001 (và bảng 009 FR-043b) khi đồng bộ.

5. **Nhắc lập yêu cầu đổi mức sau khi yêu cầu bị Từ chối.** FR-036 và BR-M01-03 ghi "chưa có yêu cầu đổi mức chưa kết thúc thì nhắc". Theo đúng chữ, khi Quản lý viện Từ chối yêu cầu, mức hiện hành vẫn khác mức đánh giá, nên Hành chính lại được nhắc mỗi CFG-M03-02 cho tới khi có yêu cầu mới hoặc lần đánh giá lại mới. Plan làm đúng như chữ. Đề nghị xác nhận đây là ý định, hay Từ chối thì dừng nhắc. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

6. **Người quay lại không có CCCD.** FR-004 tạo hồ sơ mới liên kết hồ sơ cũ, nhưng không nói nhận ra "cùng người" bằng gì khi không có CCCD. Plan chỉ tự liên kết khi trùng CCCD. Không có CCCD thì chỉ có cảnh báo trùng họ tên và ngày sinh (FR-005) và không liên kết. Đề nghị: chốt có cho Hành chính chọn hồ sơ trước bằng tay hay không. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

7. **Lý do ở bản ghi lịch sử đầu tiên.** FR-046 bắt buộc lý do "với mọi lệnh do người dùng thực hiện". Plan coi Tạo hồ sơ không phải chuyển trạng thái (không có trạng thái "từ"), nên không bắt lý do (data-model mục 2). Kịch bản US1 #1 cũng không nêu lý do. Đề nghị ghi rõ trong spec. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

8. **(Đồng bộ câu chữ spec 001, không đổi nghiệp vụ)** *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - Bảng phân nhóm dữ liệu ở đầu mục Requirements còn dòng "Yêu cầu đánh giá lại | 2", đã bỏ theo Q-307.
   - Dòng "Lịch sử trạng thái | 3" bị tách ra ngoài bảng bởi câu ghi chú in nghiêng.
   - Assumptions còn câu "Yêu cầu đánh giá lại có tối đa một yêu cầu mở mỗi người", trái Q-307.
   - Assumptions còn câu dẫn `docs/phan-tich-yeu-cau.md` (đã bỏ); có thể giữ làm lịch sử.

   Plan theo FR-039 → FR-043 và Key Entities (không có thực thể yêu cầu).

9. **(Đồng bộ với plan 000, đã sửa trong plan này)** *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - Hợp đồng của 000 thêm `TepApi` (H-13) và `YeuCauPheDuyetApi.chuyenMoRong` (H-10).
   - Data-model của 000 ghi trạng thái mở rộng `DUOC_THAY_THE` "của 001"; đã sửa thành của loại `M02.THAY_DOI_LUU_TRU` (004), do 001 kích hoạt.
   - Đã sửa ở [../000-shared-foundation/contracts/module-interfaces.md](../000-shared-foundation/contracts/module-interfaces.md) và [../000-shared-foundation/data-model.md](../000-shared-foundation/data-model.md).

10. **(2026-10-18) Người đại diện theo pháp luật không phải người thân.** Spec 001 "Điểm cần báo lại" 2 đã nêu. Plan chỉ cho người ký thay (FR-011, FR-023c) là người thân có quan hệ Hiệu lực ở 012, vì không có chỗ lưu người khác. Người do Tòa án chỉ định ngoài gia đình chưa ghi được. Đề nghị chốt bằng Q mới: thêm loại quan hệ ở 012, hay lưu họ tên, giấy tờ ngay trên bản đồng ý. *(Đã xử lý 2026-10-18: Q-359: người đại diện ngoài gia đình được lập là người thân quan hệ "Khác" ở 012; thiết kế giữ nguyên)*

11. **(2026-10-18) Ngày xem lại nhận định của Bác sĩ.** FR-023a, FR-023h bắt buộc ghi ngày xem lại nhưng không nói đến hạn thì làm gì. Plan chỉ lưu và hiển thị, không nhắc (Q-306: không tự sinh; nhắc thì cần FR). Đề nghị xác nhận, hoặc thêm FR nhắc Bác sĩ khi tới ngày xem lại. *(Đã xử lý 2026-10-18: Q-360, FR-023i: thêm job `NhacXemLaiNhanDinh` (research H-12))*

12. **(2026-10-18) Q-356 "bắt buộc lý do" trùng FR-046.** FR-046 đã bắt lý do với mọi lệnh chuyển trạng thái do người dùng thực hiện, gồm Hoàn tất tiếp nhận; `LenhTrangThai.lyDo` luôn bắt buộc. Nên khi ý kiến là "Không đồng ý", điều mới chỉ là hệ thống hiện ý kiến đó (cảnh báo của bean 004) để lý do nói tới nó. Plan không thêm cơ chế riêng. Đề nghị: nếu ý định là lý do phải nói riêng về ý kiến của người cao tuổi (ví dụ trường lý do riêng), ghi rõ ở 004 FR-004a. *(Đã xử lý 2026-10-18: Q-361: dùng lý do của lệnh; thiết kế giữ nguyên)*

## Ghi chú triển khai *(2026-10-21, CP-001-API: T001, T007, T008; T002, T009 một phần)*

- Gói `m01.api` công bố theo glossary mục 8.5: giao diện ở gốc, `dto`, `spi`, `events` (`@NamedInterface` `resident`, `resident-dto`, `resident-spi`, `resident-events`). Chỉ tạo gói có lớp (backend/README.md); các gói `internal/…` tạo cùng lớp đầu tiên.
- Lệch tên: `HealthItemApi.isChecked` (glossary cũ) thành `markChecked` vì là lệnh `void`; glossary đã sửa. `WishRecordSource` có thêm `CRITICAL_RECONFIRMATION` theo data-model (`XAC_NHAN_LAI_NGUY_KICH`).
- Trường của các view (`HealthItemView`, `ConsentView`, `EndOfLifeWishView`, `RecordEndOfLifeWish`, `AssessmentSummary`, `ResidentChangeView`…) do 001 đặt vì module-interfaces.md chỉ nêu tên; module khác cần thêm trường thì báo B (Đổi api).
- Bản giả ở `support/fake/m01/` (team-plan mục 3), không phải `support/gia001/`. Chưa có bản giả `ChiSoApi`, `SuCoDaGhiNhan` (D khai báo) nên T002 chưa `[x]`.
- Sự kiện gửi 009 chưa cài `HasNotificationRequests` vì `RecipientGroup` của 000 chưa có nhóm Bác sĩ trực, Điều dưỡng phụ trách, Trưởng tầng, người liên hệ chính (009 T004); mức và người nhận theo H-14 ghi ở javadoc từng sự kiện. T009 chưa `[x]`.

## Ghi chú triển khai *(2026-10-21, CP-001-P2-IMPL: T005, T006, T010, T012a; T011, T012 một phần)*

- Migration `V001_02__resident.sql` (`resident`, `resident_status_history`, `care_level`, `care_level_history`) và `V001_03__m01_catalogs.sql` (`allergen`, `allergy_severity`, `information_source`, `care_need`). `profile_code` nullable cho tới lúc đặt từ `id` trong cùng transaction (cột sinh không dùng được với AUTO_INCREMENT). `photo_file_id` chưa có khóa ngoại tới `file`; thêm bằng migration riêng sau T003.
- (T012 chưa xong phần web: controller `/catalogs/{type}` đã viết nhưng chờ duyệt đăng ký `api-001` vào `ApiContractIT` và cách khớp schema `Problem`) Danh mục dùng JPA (`CatalogBase`) và một `CatalogService` điều phối theo `type` của đường dẫn; `care-levels` là bảng cố định đọc bằng `CareLevelStore`. Hợp đồng không có trường lý do cho tạo, sửa nên chỉ ngừng hiệu lực và kích hoạt lại là `@BusinessCommand`.
- `ProfileOpenGuardArchTest` kiểm lệnh gọi guard trực tiếp trong phương thức; lệnh không ghi hồ sơ khai trong danh sách ngoại lệ kèm lý do. Lớp mẫu của test nằm ở `src/test/.../archfixture/m01guard`.
- Test tích hợp chạy trên MySQL 8.4 phụ cổng 3307 (datadir riêng, không binlog, `TEST_DB_URL` trỏ cổng này), không đụng dịch vụ MySQL cổng 3306.
- T011 mới có phân loại trường và gắn phạm vi cho `M01.RESIDENT`; phần còn lại làm cùng bảng của từng user story, riêng `FilePermissionProvider` chờ `common/tep` (T003, T004).

## Ghi chú triển khai *(2026-10-22, CP-001-P4: T024 → T032; T033 phần web)*

- Migration `V001_04__health.sql`: `initial_health_record` (nhóm 3 có bản nháp, trigger `trg_initial_health_record_no_update` chặn `UPDATE` khi đã xác nhận, đặt đúng tên để `RecordGroupTriggerIT` nhận) và `health_item` (nhóm 2, nội dung bất biến: trigger `trg_health_item_immutable`, loại trừ một chiều, `CHECK` ở cơ sở dữ liệu cho dị ứng cần mức độ, mục cần nội dung, loại trừ đủ ba cột). Cả hai bảng có trigger chặn `DELETE` trừ tài khoản `eldercare_retention`.
- Quyền: ma trận Phụ lục 27 không có dòng riêng cho hồ sơ sức khỏe ban đầu nên dùng dòng "Dị ứng, bệnh nền" (`ALLERGY_CONDITION`); "chỉ Bác sĩ xác nhận" kiểm thêm theo vai trò trong dịch vụ và lọc khỏi `allowedCommands`. `ResidentController` thêm bốn lệnh sức khỏe vào `allowedCommands` để màn web ẩn hiện nút.
- Đính chính hồ sơ ban đầu đi qua `CorrectionApi` của 000; endpoint `/initial-health-records/{id}/correct` của hợp đồng chỉ chuyển tiếp. Form đính chính dùng chung của web gọi `/corrections/{recordType}/{id}` của 000 với loại `M01.INITIAL_HEALTH_RECORD`.
- `HealthItemResult` giữ tên trường `items` (một mục) đúng `api-001.yaml`; glossary ghi `item`. Chờ chủ dự án chốt sửa hợp đồng hay glossary.
- Job `RetryAllergyChecksJob` (`RETRY_ALLERGY_CHECKS`) chưa nằm trong `runJobs` của api-000 (hợp đồng của A); test gọi `AllergyCheckRetry.run()` trực tiếp. Ngưỡng 15 phút là cấu hình kỹ thuật `eldercare.jobs.retry-allergy-checks.overdue-after`, không phải mốc nghiệp vụ nên không qua `ParameterApi` (`TimeArchTest` cấm hằng số `Duration` ở mã nghiệp vụ).
- Kiểm tra dị ứng tách khỏi transaction ghi mục bằng sự kiện `AllergyRecorded` (Spring Modulith, `@ApplicationModuleListener`); `BR_M01_12_PendingCheckIT` có listener giả ném lỗi để chứng minh mục vẫn được lưu và cờ chờ kiểm tra còn.
- Web: màn nằm ở `frontend/src/features/residents/` (`HealthTab`, `InitialHealthRecord`, `HealthItems`), vì danh sách và khung chi tiết hồ sơ ở đó (frontend/README mục 2). Danh mục dị nguyên, mức độ, nguồn lấy từ `GET /catalogs/{type}` (controller T012 lấy lại từ stash `CP-001-P2-IMPL`, không kèm phần sửa `ApiContractIT` và `api-001.yaml` đang chờ A duyệt).
- `ProfileBody.activeHealthItems` vẫn để rỗng; tab Sức khỏe tự gọi `GET /residents/{id}/health-items`. Nạp `activeHealthItems` vào hồ sơ thuộc phần chi tiết T079 trở đi.

13. **(2026-10-22) Q-383, Q-384.**
    - **Số thẻ bảo hiểm y tế** (FR-023j): plan lưu `bhyt_so_the`, `bhyt_han_the`, `bhyt_noi_dang_ky` trong nhóm `SUC_KHOE`, không bắt buộc. Căn cứ xử lý dữ liệu cá nhân chờ đối chiếu ở 24.4 (Q-384), chặn trước khi vận hành, không chặn thiết kế.
    - **Kế toán thấy cờ "đã có phiếu"**: FR-023m chỉ nêu Hành chính thấy "đã có phiếu". *(Đã xử lý 2026-10-22: chủ dự án chọn Kế toán không thấy cờ, đúng chữ FR-023m.)* Quy tắc xem trường của 002 chỉ theo vai trò × nhóm thông tin nên không tách được một trường; 001 tự kiểm khi dựng DTO hồ sơ: cờ `coNguyenVongDieuTri` chỉ có khi người xem có `(DI_UNG_BENH_NEN, XEM)` với người cao tuổi đó hoặc có `(HO_SO_NGUOI_CAO_TUOI, TAO)` (Hành chính, chú thích ⁵); ngược lại bỏ trường và liệt kê ở `truongBiAn`. Không đổi 002 (research H-18).
    - **Người cần báo** 0..n người, không bắt buộc (chủ dự án chọn 2026-10-22); "cần hỏi trước = Có" mà danh sách rỗng thì lệnh ghi phiếu trả cảnh báo `CHUA_CO_NGUOI_CAN_BAO`, không chặn.

## Ghi chú triển khai *(2026-10-22, CP-001-P5: T034 → T041; T042 chờ duyệt bố cục web)*

- Migration `V001_05__assessment.sql`: `assessment` (nhóm 3 có bản nháp) và `assessment_care_need`. Hai cột sinh `open_draft`, `confirmed_initial` có `UNIQUE` (`uq_assessment_open_draft`, `uq_assessment_confirmed_initial`), `CHECK chk_assessment_confirm` (nhận xét không rỗng sau `TRIM`, có mức, có người và thời điểm xác nhận), `chk_assessment_discard`, `chk_assessment_proposed_residence` (loại hình lưu trú đề xuất chỉ với đầu vào). Trigger `trg_assessment_no_update` chặn `UPDATE` khi đã xác nhận hoặc đã hủy; `assessment_care_need` chặn thêm, xóa khi lần đánh giá cha không còn Nháp (job loại bỏ dữ liệu hết hạn được xóa). Tên bảng, cột, lớp tiếng Anh theo glossary mục 8.
- Quyền dùng dòng "Đánh giá, mức chăm sóc" (`ASSESSMENT_CARE_LEVEL`): Điều dưỡng (theo phân công) và Bác sĩ có `CREATE`, `UPDATE`; chỉ Bác sĩ có `APPROVE` (xác nhận). Sửa nháp, hủy nháp: người tạo hoặc Bác sĩ (kiểm thêm ở dịch vụ, 403). Nhân viên chăm sóc không có `VIEW` nên danh sách đánh giá trả 403. `ResidentController` thêm hai lệnh `CREATE_ASSESSMENT`, `CONFIRM_ASSESSMENT` vào `allowedCommands`.
- **Lệch nhỏ so với tasks**: xác nhận thiếu nhận xét hoặc mức trả **409 `CONDITION_NOT_MET`** kèm danh sách trường thiếu (`comment`, `careLevel`) chứ không phải 422 như câu chữ T034; theo `api-001.yaml` (POST `/assessments/{id}/confirm`, 409) và `ConditionNotMetException` của 000. `CHECK` ở MySQL vẫn là lớp chặn thứ hai (test cập nhật thẳng bằng SQL).
- Mã lỗi mới: `NOT_A_DRAFT` (409; sửa hoặc hủy lần đánh giá đã xác nhận hay đã hủy). `DRAFT_ALREADY_EXISTS`, `INITIAL_ASSESSMENT_EXISTS`, `STATUS_NOT_ASSESSABLE` theo glossary 8.4.
- Tác động xác nhận (H-10) trong một transaction của `AssessmentService.confirm`: đầu vào, hoặc đánh giá lại khi Đang tiếp nhận → `CareLevelHistoryStore.apply` (cột `resident.current_care_level` và dòng `care_level_history`, chỉ khi mức khác mức hiện hành) và sự kiện `CareLevelChanged`, `InitialAssessmentConfirmed`; đánh giá lại khi đã lưu trú → `CareLevelChangeRequestApi.supersedeByAssessment` (nhận qua `ObjectProvider`, chưa có bản 004 thì bỏ qua) và `CareLevelChangeRequestNeeded` khi mức khác. Mức hoặc tập nhu cầu khác lần Đã xác nhận trước (theo nội dung sau đính chính) → `AssessmentChangedCareLevelOrNeeds`, `carePlanReviewNotified = true`. "Đáp ứng lần báo đánh giá lại" (H-11 mục 5) là trạng thái tính từ thời điểm xác nhận, không ghi gì thêm.
- Chưa làm (thuộc task sau, không nằm trong T034 → T041): **nhắc Hành chính mỗi CFG-M03-02** khi chưa có yêu cầu đổi mức (H-12; job), bảng `lan_bao_danh_gia_lai` (T062). Đính chính đổi mức của lần gần nhất phát sự kiện nội bộ `AssessmentCorrectedLevel(residentId, assessmentId, correctionId)` (test `AssessmentCorrectionIT`); listener của T062 ghi lần báo `DINH_CHINH_DANH_GIA`, nên T036 mới xong phần phát sự kiện.
- Đính chính qua 000 loại `M01.ASSESSMENT`: người ghi gốc là Bác sĩ xác nhận, không gắn tầng (cách A hoặc C), chỉ Bác sĩ ghi được; đính chính được `comment`, `careNote`, `careLevel`. `AssessmentContents` tính nội dung hiện hành (theo bản đính chính mới nhất, từng trường), nhận `CorrectionApi` qua `ObjectProvider` vì 000 tiêm mọi provider (tránh vòng phụ thuộc). `ActorNames` (health) đổi thành `public` để dùng chung.
- `CareLevelApi` (`CareLevelApiImpl`: `current`, `atDate`, `currentCareNeeds`, `applyChange` bắt buộc có transaction, chỉ 004 gọi) và `AssessmentApi` (`currentCareNote` theo lô, `latestConfirmed`) cài xong. Endpoint `GET /residents/{id}/care-level`, `/care-level-history` đi qua `CareLevelQueryService` (quyền xem hồ sơ người cao tuổi). `ResidentContractIT` mở rộng cho 9 endpoint mới và ba schema `Assessment`, `ConfirmAssessmentResult`, `CareLevelAtDate`.
- `GET /assessments/{id}`: phần `reference.latestVitalSigns` luôn rỗng vì 007 chưa khai báo `VitalSignApi` (`ChiSoApi.ganNhat`); chờ D (team-plan mục 8). Hai trường `initialHealthRecord`, `activeHealthItems` lấy qua `InitialHealthRecordService`, `HealthItemService`.
- Test: `AssessmentTestBase` thêm `FakeCareLevelChangeRequests` (bản giả 004 do 001 khai báo), không import `FakeM01Config` vì `FakeResidents` `@Primary` của nó thay `ResidentApi` thật.
- **Web (T042, 2026-10-23)**: `frontend/src/features/assessments/` (`AssessmentTab`, `AssessmentForm`, `assessmentLabels.js`, 13 test Vitest); tab "Đánh giá" thêm vào `ResidentDetail`, khung chi tiết rộng `sm:max-w-5xl` khi mở tab này (`ResidentSearch` giữ trạng thái rộng). Bố cục duyệt 2026-10-23, ghi ở DESIGN.md 5.18.2. Nút theo `allowedCommands` (`CREATE_ASSESSMENT`, `CONFIRM_ASSESSMENT`); Bác sĩ cũng là người đính chính (nút Đính chính ẩn với Điều dưỡng); 403 khi xem danh sách thì hiện "Không có quyền xem đánh giá". `reference.latestVitalSigns` rỗng nên màn ghi đúng "Chưa có dữ liệu chỉ số đo", không dữ liệu giả. Chưa duyệt bằng mắt trên trình duyệt: chủ dự án kiểm thử 2026-10-24.
- **Đính chính qua form dùng chung (rà soát 2026-10-23)**: form `correction-sheet` của 000 gửi mọi ô, nên `AssessmentContents` và `AssessmentCorrectionProvider.validateContent` coi nhận xét và mức để trống là giữ nguyên (không được rỗng, DBR-05), còn lưu ý chăm sóc để trống là bỏ lưu ý (test `FR_032_sharedFormWithBlankCommentAndLevel…` bắt lỗi `IllegalArgumentException` ở bản trước). Ô mức trong form đính chính phải gõ mã `BASIC`, `REGULAR`, `SPECIAL` vì component chung chỉ có ô văn bản; đề xuất A thêm kiểu lựa chọn cho `fields` (file chung của 000, chưa làm).
- Tab Đánh giá hiện với mọi vai trò có khung chi tiết hồ sơ; vai trò không có quyền xem (Nhân viên chăm sóc) nhận 403 và thấy trạng thái không có quyền, vì `allowedCommands` không có lệnh xem để ẩn tab.
- T042 phần `mobile/lib/nhan-vien/danh-gia/` và `the_nguoi_cao_tuoi_test.dart` chưa làm (mobile chưa mở).
- **Chốt Phase 5 (2026-10-23)**: `./mvnw verify` trên MySQL 3307: 170 test đơn vị, kiến trúc xanh; 598 test tích hợp, 597 xanh, 1 đỏ là `ApiContractIT.R_14` (BL-23 của A, có từ trước Phase 5, không sửa). Vitest 142 (một lần chạy dưới tải máy bị `XacNhanTrung.test.jsx` quá 5 giây, chạy lại xanh), lint, build xanh. Chủ dự án chưa nghiệm thu giao diện web. Phase 6 cần nhớ: `lan_bao_danh_gia_lai` (T062) nghe `AssessmentCorrectedLevel` để ghi lần báo `DINH_CHINH_DANH_GIA`; job nhắc Hành chính mỗi CFG-M03-02; `CareLevelChangeRequestApi` thật do 004 cài (hiện nhận qua `ObjectProvider`); `VitalSignApi` của D (BL-36).

## Ghi chú triển khai *(2026-10-23, CP-001-P6: T043 → T051 phần backend)*

- **Mã**: `m01/internal/status/` (`StatusTransitionTable`, `ResidentStatusService` là điểm duy nhất đổi trạng thái, `ResidentStatusApiImpl`, `M01StatusParticipant`, `TemporaryAbsenceOverdueAlert`), `web/StatusController` (9 endpoint theo api-001), `scheduling/TemporaryAbsenceOverdueJob`, migration `V001_06__return_overdue_alert.sql`; `ResidentController` thêm bảy lệnh vào `allowedCommands`, lọc theo trạng thái và vai trò (`ResidentStatusService.availableCommands`).
- **Lệch có chủ đích**: điều kiện chưa thỏa trả 409 `UNMET_CONDITIONS` (theo api-001, khác `CONDITION_NOT_MET` chung của 000); mỗi lệnh người dùng là một phương thức riêng có `@RequiresPermission` (khóa quyền khác nhau theo lệnh) nên `ProfileOpenGuardArchTest` thấy được lời gọi guard; `Condition` của SPI chưa có trường `module` như schema `Condition` của api-001 nên `ResidentContractIT` chưa so schema này; `evidenceFileId` (qua đời) chưa kiểm tệp tồn tại vì `FileApi` chưa có bản cài (T003).
- **Chưa làm**: T048 phần Ghi nhận trở về từ bệnh viện (cần `lan_bao_danh_gia_lai`, T062); `statusPeriods` của `ResidentStatusApi`; `DinhChinhDuocProvider` cho `M01.LICH_SU_TRANG_THAI` (T050); tệp đính kèm giấy tờ bệnh viện (T081, T082) chờ T003, T004; mọi lệnh tạm từ chối `attachmentFileIds` bằng 422 `ATTACHMENTS_NOT_ACCEPTED`; T052 web chờ duyệt bố cục.
- **Kiểm chứng**: test chạy trên MySQL 3307.
- File chung đã sửa với phép của chủ dự án: `SchedulerScopeArchTest.ALLOWED` thêm job, `application.yml` profile `test` thêm `temporary-absence-overdue.cron: "-"`.
- **Web (T052 phần web, 2026-10-23)**: `frontend/src/features/status/` (`StatusTab`, `statusLabels.js`, 10 test Vitest); tab "Trạng thái" thêm vào `ResidentDetail` (thẻ trạng thái hiện tại và nút lệnh theo `allowedCommands`, form lệnh mở ngay dưới thẻ với xem trước điều kiện, ô lý do, ô căn cứ của Chuyển viện, ô mã tệp bằng chứng của Ghi nhận qua đời, lịch sử trạng thái). Bố cục do B giao Claude quyết định; B chưa nghiệm thu trên trình duyệt. Mã tệp bằng chứng nhập tay tới khi `FileApi` (T003) có bản cài. `CompleteAdmissionSheet.jsx` (bản mẫu dữ liệu giả) chưa bị thay.
- **Chốt Phase 6 (2026-10-23)**: `./mvnw verify` trên MySQL 3307 (`TEST_DB_URL`, `DB_URL` đã đặt về 3307): 174 test đơn vị xanh; 645 test tích hợp, 644 xanh, 1 đỏ là `ApiContractIT.R_14` (BL-23 của A, có từ trước Phase 6, không sửa). ArchTest xanh. Frontend: `npm run lint` sạch, `npm run build` qua, 152 test Vitest xanh (chạy `--pool=threads`). Thiếu so với spec: thông báo Khẩn cấp cho người liên hệ chính khi Chuyển viện, Ghi nhận qua đời (`ResidentStatusChanged` chưa cài `HasNotificationRequests`, chờ 009 có nhóm người nhận, US4 kịch bản 11); báo đánh giá lại khi trở về từ bệnh viện (kịch bản 7, chờ T062). Phase 7 cần nhớ: `ProfileOpenGuard` khi ghi bản đồng ý, ngoại lệ Rút lại đồng ý (FR-014a); `BanDongYApi` cần khai báo sớm (team-plan mục 8, dòng B).


## Ghi chú triển khai *(2026-10-23, CP-001-P7: T003, T004, T053 → T058; T059 chờ duyệt bố cục)*

- **Tệp (T003, T004)**: `common/file` (`FileService` cài `FileApi` và `download`, `FileStore`, `FileStorageConfig`, `UnattachedFileCleanup` + job `CLEAN_UNATTACHED_FILES`), `m01/web/FileController`. Thêm `software.amazon.awssdk:s3` 2.49.6 (chủ dự án duyệt 2026-10-23, bỏ `netty-nio-client`). Máy khách S3 chỉ tạo khi có `ELDERCARE_S3_BUCKET`; thiếu thì 503 `FILE_STORAGE_UNAVAILABLE`. Test dùng `support/fake/file/InMemoryS3Client`. Migration đặt số `V001_06_1` thay `V001_01` vì V001_02 → V001_06 đã áp dụng ở các cơ sở dữ liệu đang chạy; ứng dụng chỉ xóa được tệp chưa gắn, tệp đã gắn chỉ job lưu giữ xóa; trigger chặn đổi tệp đã gắn. `FileController` ở M01 vì `common` không gọi ngược M15. `FileService` tra `FilePermissionProvider` lười (`ObjectProvider`) để tránh vòng phụ thuộc với provider của module sở hữu. Tệp chưa gắn: chỉ người tải lên tải được. Giới hạn multipart 10 MB, vượt thì 422 `FILE_TOO_LARGE`.
- **Bản đồng ý (T053 → T058)**: `m01/internal/consent/` (`ConsentService` cài `ConsentApi`, `ConsentStore` JDBC, `ConsentFootnoteCondition` mã "1" kiểm theo thời điểm, `ConsentFilePermission` cho `M01.CONSENT`, `ConsentFieldClassification`, `MissingConsentReminderService`), `web/ConsentController`, `scheduling/MissingConsentReminderJob`, migration `V001_07__consent.sql` (bảng `consent`, `consent_family_member`, `periodic_reminder`; tên tiếng Anh, ghi ở glossary mục 8.6). Thay bản cũ theo ba bước (chuyển bản cũ Đã rút lại không lý do, thêm bản mới, điền `superseded_by_id`) vì cột sinh `UNIQUE` không cho hai bản Hiệu lực cùng lúc; trigger cho điền `superseded_by_id` đúng một lần.
- **Quyết định kỹ thuật**: lần nhắc thiếu đồng ý đầu tiên sau một chu kỳ CFG-M01-06 kể từ Hoàn tất tiếp nhận (lệnh đã cảnh báo `MISSING_CONSENT` người thực hiện); `ConsentService.withdraw` vào ngoại lệ của `ProfileOpenGuardArchTest` (FR-014a); `ConsentChanged.activeConsentId` nay là `Long` nullable (rỗng khi không còn bản Hiệu lực, đúng module-interfaces); `ProfileBody.hasActiveConsent` có giá trị thật, `allowedCommands` thêm `RECORD_CONSENT`, `WITHDRAW_CONSENT`; người thân trong phạm vi không kiểm quan hệ lúc ghi (phạm vi RELATIONSHIP của 002 đã lọc khi xem).
- **Sửa hợp đồng api-001 (chủ dự án duyệt 2026-10-23)**: schema `Consent` thêm `consenterFamilyMemberId`, `evidenceFileId`, `withdrawal.evidenceFileId` để giao diện tải được bản ký; trường cũ không đổi. Người đại diện theo pháp luật không phải người thân vẫn được ghi là người thân quan hệ "Khác" ở 012 (Q-359) nên `consenterFamilyMemberId` luôn là mã người thân.
- **Chưa làm, còn nợ**: phần mobile; T011 mới có phần bản đồng ý (`ConsentFieldClassification`, `ConsentFilePermission`); T081, T082 (giấy tờ bệnh viện) nay đã có `FileApi` nhưng chưa làm; `MissingConsentReminder` chưa cài `HasNotificationRequests` (T009).
- **File chung đã sửa với phép của chủ dự án 2026-10-23**: `SchedulerScopeArchTest.ALLOWED` (chuyển `Map.of` sang `Map.ofEntries`, thêm hai job), `application.yml` profile test (hai cron `"-"`), `pom.xml` (AWS SDK).
- **Kiểm chứng**: MySQL 3307; 282 test tích hợp của `m01.*` và `common.file.*`, ArchTest xanh sau sửa hai lỗi trong test (`ResidentContractIT` thêm endpoint `consents`, `files` và schema `Consent`). Chưa chạy `verify` toàn dự án.
- **Web (T059, 2026-10-23)**: `frontend/src/features/consents/` (`ConsentTab`, `consentLabels.js`, 12 test Vitest), tab "Đồng ý" trong `ResidentDetail`; `frontend/src/common/files.js` (`uploadFile`, `openFile`) vì client sinh từ hợp đồng chỉ gửi JSON. Bố cục duyệt 2026-10-23 (DESIGN.md 5.18.3). Nút theo `allowedCommands`; hồ sơ trạng thái cuối không ghi nhận bản mới nhưng vẫn rút lại. Bản ký tải qua `GET /files/{id}` bằng cookie phiên rồi mở bằng URL `blob:` trong trình duyệt; backend kiểm quyền mỗi lần, không có đường dẫn công khai. **Tạm thời**: mã người thân (người được xem sức khỏe, người ký thay) nhập tay vì 012 chưa có danh sách; mã được kiểm là số nguyên dương, người ký thay còn được backend kiểm quan hệ (409 `INVALID_SIGNER`); mã trong danh sách xem sức khỏe không kiểm tồn tại. Profile `demo` có `DemoFileStorageConfig` lưu tệp trong bộ nhớ khi chưa có `ELDERCARE_S3_BUCKET` (mất khi khởi động lại; **không phải kiểm thử với S3 thật**, chưa có môi trường AWS để chạy upload/download thật).
- **Chốt Phase 7 (2026-10-23)**: `./mvnw verify` trên MySQL 3307 (TEST_DB_URL, DB_URL đã xác minh): 410 test đơn vị và kiến trúc xanh; 706 test tích hợp, 702 xanh, 4 đỏ: `ApiContractIT.R_14` (BL-23 của A, có từ trước) và 3 test của `m15.account.FamilyHealthIT` do bản giả `FakeConsents` (mã chú thích "1") trùng với `ConsentFootnoteCondition` thật nên `PermissionService.registeredConditions` báo "Duplicate key 1": test của A cần dùng bản thật hoặc gỡ bản giả (chờ chủ dự án cho phép sửa file của A). Vitest 164, lint, build xanh.


## Ghi chú triển khai *(2026-10-23, CP-001-P8: T060 → T066)*

- **Mã**: migration `V001_08__reassessment_notice.sql` (tên tiếng Anh theo glossary mục 8); `m01/internal/assessment/service/` gồm `ReassessmentNoticeService` (báo, tính trạng thái, chạy job), `ReassessmentApiImpl`, `ReassessmentNoticeListener`, `CareLevelChangeRequestReminderService` (nhắc CFG-M03-02, dùng bảng `periodic_reminder`), `ReassessmentQueryService`; `web/ReassessmentController`; ba job ở `m01/scheduling/`. Hạn tính từ lần báo sớm nhất chưa đáp ứng; chưa đáp ứng là lần báo sau lần đánh giá Đã xác nhận gần nhất (mọi loại). Đang Điều trị tại bệnh viện không tính hạn; Ghi nhận trở về đặt mốc cho mọi lần báo chưa đáp ứng và xóa dấu đã cảnh báo.
- **Lệch có chủ đích**: (1) báo sau đính chính và sau sự cố cho mọi hồ sơ chưa ở trạng thái cuối (kể cả Đang tiếp nhận); chỉ job định kỳ giới hạn bốn trạng thái của FR-039. (2) `GET /reassessments/due` trả mảng đầy đủ, không phân trang, vì hợp đồng không có `nextCursor`. (3) `basis[].source` có dạng `LOẠI:mã`. (4) `applyChange` đặt mức hiện hành ngay, nên 004 phải gọi vào hoặc sau ngày hiệu lực.
- **Chưa làm**: listener sự cố của T063 (chờ D khai báo `IncidentRecorded`); `ResidentStatisticsApi.overdueReassessments` chưa có bản cài. Chờ A thêm ba tên job vào `runJobs` (team-plan mục 8). File chung đã sửa với phép của chủ dự án: `SchedulerScopeArchTest.ALLOWED`, `application.yml` profile test.
- **Web**: dải `Alert` đầu tab Đánh giá, cột và bộ lọc ở bảng hồ sơ (DESIGN.md 5.18.4); 52 Vitest xanh ở assessments, residents; lint, build xanh.
- **Chốt Phase 8**: `./mvnw verify` trên MySQL 3307: 729 test, 4 đỏ đều có từ trước và thuộc A (`ApiContractIT.R_14` BL-23; ba test `FamilyHealthIT` BL-39). Chưa nghiệm thu giao diện trên trình duyệt.

## Bàn giao checkpoint Phase 8 *(2026-10-23, trước khi tạm nghỉ; B duyệt)*

**Git (xác minh bằng `git ls-remote origin refs/heads/impl/001-elder-profile-assessment`, không dựa nhánh theo dõi local, vì nhánh này từng cũ hơn thực tế):** các commit Phase 1 → 7 đã ở remote (remote khớp `3b27943` trước khi commit Phase 8). Phase 8 gồm `a4c8dd5` (test), `3ee1b9b` (backend), `69f1089` (web) và một commit tài liệu; push theo fast-forward, không force. Stash `CP-001-P2-IMPL` còn nguyên (web danh mục, `ApiContractIT` đăng ký api-001, `api-001.yaml`; chờ A duyệt file chung).

| Phase | Task xong / chưa | Commit | Test tự động | Giao diện web B nghiệm thu |
| --- | --- | --- | --- | --- |
| 1, 2 | Xong 10 (có T003, T004 từ `8aa21d3`). Chưa: T002 (thiếu bản giả `ChiSoApi`, `SuCoDaGhiNhan` do D), T009 (thiếu `HasNotificationRequests`, chờ 009), T011 (chưa phân loại trường các loại còn lại, `RetentionProvider`), T013 (mới phủ lệnh danh mục), T013a (Excel và commit `e9483e5` ghi xong, test `FR_023h_KhongDauNangLucIT` có, nhưng tasks.md chưa `[x]`: cần đối chiếu T013a nói ArchTest trước khi đánh dấu), T083 | `a0482bc`, `e75ac3e`, `5be4575`, `e422566`, `e9483e5`; T003, T004: `8aa21d3` | Có | Chưa |
| 3 | Xong 7 / chưa T018, T019, T023 (mobile) | `24a09ab`, `b108441`, `29dcde1`, `884657b` | Có | Chưa |
| 4 | Xong 9 / chưa T033 (mobile) | `fdece81`, `a8956f7`, `55d8b5d` | Có | Chưa |
| 5 | Xong 8 / chưa T042 (mobile) | `ba4bd33`, `b669c62`, `e547a42`, `344eb2f`, `c0c374e` | Có | Chưa |
| 6 | Xong 7 / chưa T049 (`statusPeriods`), T050 (`DinhChinhDuocProvider`), T052 (mobile), T081, T082 (giấy tờ bệnh viện; `FileApi` đã có từ `8aa21d3`, chưa làm) | `eaf473e`, `fa3ee79`, `82152ff` | Có | Chưa |
| 7 | Xong 7 / chưa: không | `e8ef59c`, `55f0a87`, `3b27943` | Có | Chưa |
| 8 | Xong 6 (T060 → T062, T064 → T066; T036 và T048 thuộc Phase 5, 6 cũng xong ở Phase 8) / chưa T063 (listener sự cố, chờ D) | `a4c8dd5`, `3ee1b9b`, `69f1089`, commit tài liệu | Có | Chưa |

Tổng theo tasks.md (số liệu 2026-10-23, trước Phase 9; số hiện hành ở mục "Bàn giao checkpoint cuối Phase 9" cuối file): 54 task `[x]`, 31 task `[ ]` (Phase 9: 8, Phase 10: 6, còn lại là tồn đọng ở Phase 1 → 8 ở bảng trên). Đã merge `origin/develop` ba lần (`f7680b5`, `5a6335a`, `5cf69e5`).

**Test còn đỏ (không phải của B, không sửa):** `ApiContractIT.R_14` (BL-23, A đăng ký api-001) và ba test `FamilyHealthIT` do `FakeConsents` trùng `ConsentFootnoteCondition` thật (BL-39, A). `./mvnw verify` Phase 8: 729 test, chỉ 4 đỏ này.

**Chờ người khác:** A (duyệt ba tên job vào `runJobs`, BL-23, BL-39, `TEMPORARY_ABSENCE_OVERDUE` và các job trước), D (`IncidentRecorded`, `VitalSignApi`), 009 (`HasNotificationRequests`).

**Còn phải làm cho Feature 001:** Phase 9 (nguyện vọng cuối đời T067 → T070, T077 → T080), Phase 10 (T071 → T076), tồn đọng ở bảng trên, toàn bộ phần `mobile/` (chưa mở), nghiệm thu giao diện web các tab Hồ sơ, Sức khỏe, Đánh giá, Trạng thái, Đồng ý, dải đánh giá lại.

**Nhớ khi làm Phase 9:** luôn đặt `DB_URL`, `TEST_DB_URL` về cổng 3307 (biến máy trỏ 3306); sau verify khôi phục quyền `elder_care_demo`; dùng `git ls-remote` để biết remote thật; không đánh dấu nghiệm thu giao diện khi B chưa thử; tạo bản giả `support/fake/m05/` khi D khai báo `IncidentRecorded`.

## Ghi chú triển khai *(2026-10-23, CP-001-P9: T067 → T069, T077, T078; T070, T079, T080 một phần)*

- **Mã**: migration `V001_09__end_of_life_wish.sql` (`end_of_life_wish`, `end_of_life_wish_opinion`) và `V001_10__treatment_wish.sql` (`treatment_wish`, `treatment_wish_notify`, `treatment_wish_opinion`), tên tiếng Anh theo glossary mục 8. Gói `m01/internal/wish/{domain, persistence, service}`: `EndOfLifeWishService`, `TreatmentWishService`, `WishReminderService`, `EndOfLifeWishApiImpl`, hai `FieldClassificationProvider`, hai `FilePermissionProvider`. `web/EndOfLifeWishController`, `web/TreatmentWishController`, request/response theo tên schema của api-001. `M01StatusParticipant` trả thêm cảnh báo `MISSING_END_OF_LIFE_WISH`, `MISSING_TREATMENT_WISH` lúc Hoàn tất tiếp nhận và bắt đầu nhắc phiếu cuối đời. `ProfileBody` có `hasEndOfLifeWish`, `hasTreatmentWish` thật; `allowedCommands` thêm `RECORD_END_OF_LIFE_WISH`, `RECORD_TREATMENT_WISH`.
- **Quy tắc đã cài**: ghi phiếu là ghi phiên bản trong một transaction (chuyển bản cũ, thêm bản mới, đặt `superseded_by_id`); trigger chỉ cho đổi Hiệu lực → Được thay thế (cuối đời) hoặc Hết hiệu lực (khi điều trị), bảng con chỉ thêm. Người ký: Bác sĩ nhận định tự quyết được thì người cao tuổi ký, ngược lại người đại diện của người bệnh (kèm cách được chọn) hoặc người đại diện theo pháp luật (kèm đủ quyết định của Tòa án), cả hai là người thân có quan hệ Hiệu lực (409 `INVALID_SIGNER`). Nhận định thiếu cách hỗ trợ hiểu hoặc ngày xem lại, ngày xem lại trước hôm nay, Bác sĩ nhận định không có vai trò AC-03, thiếu bản ký: 422. Ý kiến khác lưu từng dòng có nguồn qua `OpinionSourceApi`. Nhắc thiếu phiếu cuối đời theo CFG-M01-06 (bảng `periodic_reminder` loại `MISSING_END_OF_LIFE_WISH`); nhắc xem lại nhận định một lần mỗi phiếu (loại `REVIEW_SELF_DETERMINATION`, `milestone_id` là id phiếu). Phiếu khi điều trị không có nhắc (FR-023l).
- **Lệch có chủ đích**: (1) việc nhắc xem lại nhận định không có lớp job riêng như T069 ghi mà chạy trong `MissingConsentReminderJob` cùng nhắc thiếu phiếu (H-12 chỉ có một job nhắc theo chu kỳ cho đồng ý và nguyện vọng); lý do là thêm job mới phải sửa file chung của A (`SchedulerScopeArchTest.ALLOWED`, `application.yml`, `runJobs`). (2) `RecordEndOfLifeWish` của `m01.api` mở rộng đủ trường (Bác sĩ nhận định, người ký, bản ký, ý kiến khác) vì bản khai báo Phase 1 không đủ cho cột `NOT NULL`; chưa ai dùng, đã báo D ở team-plan mục 8. (3) `EndOfLifeWishApi` có thêm `currentTreatmentWish` (hợp đồng module-interfaces 1.8 đặt `dieuTriHieuLuc` ở `NguyenVongApi`). (4) Mã lỗi nghiệp vụ mới (422): `SELF_DETERMINATION_ASSESSMENT_REQUIRED`, `INVALID_REVIEW_DATE`, `INVALID_ASSESSING_DOCTOR`, `RESPONDENT_REQUIRED`, `REPRESENTATIVE_SELECTION_REQUIRED`, `REPRESENTATIVE_SELECTION_NOT_ALLOWED`, `COURT_DECISION_NOT_ALLOWED`, `FIRST_CONTACT_REQUIRED`, `OPINION_CONTENT_REQUIRED`, `SOURCE_NOT_ALLOWED`, `PREFERRED_HOSPITAL_REQUIRED`, `TOO_MANY_PREFERRED_HOSPITALS`, `INCOMPLETE_TREATMENT_WISH`; 409 `INVALID_RESPONDENT`. (5) Kế toán không có cờ `hasTreatmentWish`: DTO để `null` thay vì ghi tên trường vào `hiddenFields`, vì bộ tuần tự hóa của 002 chỉ liệt kê trường có `@FieldGroup` (team-plan mục 8, A). (6) `notifyFamilyMembers[].relationship` rỗng vì `FamilyRelationshipApi` chưa có nhãn quan hệ (team-plan mục 8, E và A). (7) Người xem ngoài phạm vi phân công nhận 404 (hành vi chung của 002), không phải 403; người trong phạm vi nhưng không có quyền (Hành chính, Kế toán) nhận 403.
- **Web (T070, T080)**: tab "Nguyện vọng" trong `ResidentDetail` (`frontend/src/features/wishes/`: `WishTab`, `EndOfLifeWish`, `TreatmentWish`, `Opinions`, `wishLabels.js`, `wishUtils.js`), hai khối xếp dọc theo bố cục đề xuất (DESIGN.md 5.19.1); nhận định chỉ nằm trong thẻ phiếu cuối đời kèm dòng "Người quyết định"; Hành chính chỉ thấy "Đã có phiếu", Kế toán không thấy khối khi điều trị. 22 Vitest (`EndOfLifeWish.test.jsx` 11, `TreatmentWish.test.jsx` 11); `api001.js` sinh lại từ hợp đồng. Lệch: tạm nhập tay mã Bác sĩ nhận định và mã người thân (chưa có danh sách của 008, 012); phản hồi `EndOfLifeWish` không có mã tệp bản ký nên chưa có nút tải bản ký; người trả lời đồng thời là người ký thay; `ConsentTab` xuất thêm `FileField`, `Row` để dùng lại.
- **Đổi tên nội bộ (2026-10-23, constitution X)**: 16 lớp test của 001 mang tên tiếng Việt đổi sang tiếng Anh (bảng đối chiếu ở glossary mục 8), nội dung giữ nguyên; tên logic trong tasks, quickstart, data-model giữ nguyên để truy vết. Tên Phase 9 ở tasks.md thêm "và phiếu nguyện vọng khi điều trị" cho khớp nội dung. Không đổi API công khai, sự kiện, migration đã áp dụng.
- **Đồng bộ develop (2026-10-23)**: đã merge `origin/develop` (3f43cb2: PR #21 Problem chung, PR #22 009). Conflict chỉ ở tài liệu và script sinh API (`DESIGN.md` hợp nhất bảng tông và lịch sử phiên bản; `frontend/scripts/gen-api.mjs` `TRANSLATED = ['000', '001', '002', '009']`). Migration `V009_02`, `V009_03` không xung đột `V001_09`, `V001_10`.
- **Chưa làm**: `LuuTruProvider` cho các bảng phiếu (T079, cùng provider của M01 ở T011); phần `mobile/`; nghiệm thu giao diện trên trình duyệt.
- **Chốt Phase 9**: `./mvnw verify` trên MySQL 3307 sau khi merge develop: 186 test đơn vị và kiến trúc xanh, 828 test tích hợp, 4 đỏ đều có từ trước và thuộc A (`ApiContractIT.R_14` BL-23: còn đường dẫn của nhiều module chưa vào hợp đồng đăng ký, kể cả api-001; `FamilyHealthIT` ×3 BL-39). Frontend: 211 Vitest xanh, `npm run lint`, `npm run build` qua.

## Ghi chú triển khai *(2026-10-24, BL-23: hợp đồng api-001 khớp mã, 202 chỗ lệch về 0)*

- **Cách kiểm**: bản sao tạm của `ApiContractIT` chỉ gồm api-001 với so operation bật (đã xóa sau khi dùng, không đụng file của A) báo 202 chỗ trên 37 operation; sau sửa báo 0. `ApiContractIT` của A vẫn để `checkOperations = false` cho api-001 chờ A bật.
- **Sửa hợp đồng** (`api-001.yaml`, không thêm bớt endpoint hay trường nghiệp vụ): `Problem` theo bản chung của 000 (thêm `failedLayer`, `license`, `unlocksAt`, `required`; 96 chỗ); `GET /residents/{id}` trả một schema `Profile` (người thân nhận tập con, trường bị bỏ nằm ở `hiddenFields`, `FamilyProfile` chỉ còn mô tả); `CourtDecision` kiểu `[object, null]` thay `oneOf`; `HealthItem.allergen` dùng `AllergenRef` (`id`, `name`) vì mã chỉ trả hai trường; thêm enum cho `EndOfLifeWish.choice`, `source`, `Assessment.proposedResidenceType`, bỏ `null` khỏi enum `representativeSelection`; tham số `type` của `/catalogs/{type}` khai ở từng operation; bỏ `cursor` của `GET /reassessments/due` (mã trả đủ danh sách, không phân trang).
- **Sửa mã** (chỉ M01, hành vi không đổi): `ProblemResponses` (meta-annotation `@Forbidden`, `@NotFound`, `@StatusCommand`) khai 200/403/404/409/422; đính chính và `POST /files` khai 201; `GET /files/{id}` khai `application/octet-stream` và 404; `UploadedFileBody` thay `Map`; enum `currentCareLevel`, `conditions/{command}`, `type` của danh mục, `source` của thân ghi phiếu cuối đời (không nhận `CRITICAL_RECONFIRMATION`, chỉ 007 ghi qua `EndOfLifeWishApi`); trường bắt buộc của `CourtDecision`, `SelfDeterminationAssessment`, `OpinionSource.type`, `AllergenRef`; schema `CorrectionBody` đổi `ResidentCorrectionBody` vì trùng tên schema của 000 làm OpenAPI lẫn hai thân.
- **Kiểm thử** (MySQL 3307): `./mvnw clean verify` 185 đơn vị và kiến trúc, 828 tích hợp, 0 lỗi; 211 Vitest, lint, build qua.

## Bàn giao checkpoint cuối Phase 9 *(2026-10-24, trước khi chuyển sang session làm Phase 10; B duyệt)*

**Git**: nhánh `impl/001-elder-profile-assessment`, HEAD trước commit bàn giao là `a8dfdd9` (merge `origin/develop` `ebfcd38`, PR #25 của 009); commit bàn giao là commit tài liệu ngay sau `a8dfdd9`. Remote `origin/develop` đã chứa Phase 1 → 9 tới `e4d8cdc` (PR #23); **chưa push, chưa PR** cho 4 commit sau PR #23: `c41e5e5` (mã BL-23), `8037a8f` (hợp đồng api-001), `81dff74` (tài liệu BL-23), `a8dfdd9` (merge) và commit bàn giao. Stash `CP-001-P2-IMPL` còn nguyên (web danh mục, `ApiContractIT` đăng ký api-001 — A đã đăng ký bản khác ở PR #24 nên stash chủ yếu còn để tham khảo — `api-001.yaml`). Bản sao lưu BL-23: `…/scratchpad/backup_bl23/` của phiên làm việc (patch, 2 file mới), chỉ để đối chiếu.

**Tiến độ**: tasks.md 61 `[x]`, 24 `[ ]`. Phase 1 → 9 xong phần backend và web trừ các task dưới; `mobile/` chưa mở. Phase 9: T067 → T070, T077, T078, T080 xong; T079 `[ ]`.

**Task còn `[ ]` (24)**: nền/Phase 1–2: T002 (bản giả `ChiSoApi`, `SuCoDaGhiNhan` của D), T009 (`HasNotificationRequests`, 009 đã có, cần xem lại), T011 (`RetentionProvider`, `LuuTruProvider`, phân loại trường còn lại), T013, T013a (test có, chưa đánh dấu), T083 (`EndOfLifeWishApi.currentTreatmentWish` đã làm một phần ở Phase 9; đối chiếu trước khi đánh dấu); Phase 3–6: T018, T019, T023 (mobile), T033, T042, T052 (mobile), T049 (`statusPeriods`), T050 (`DinhChinhDuocProvider`), T081, T082 (giấy tờ bệnh viện); Phase 8: T063 (listener sự cố, chờ D); Phase 9: T079 (`LuuTruProvider`, cùng T011); Phase 10: T071 → T076.

**BL-23**: 202 chỗ lệch giữa `api-001.yaml` và mã M01 đã sửa về 0 (ghi chú 2026-10-24 ở trên). Xác nhận lần cuối sau merge develop `ebfcd38`: bản sao tạm của `ApiContractIT` bật so operation cho api-001 qua, đã xóa. **Còn chờ A** đặt `checkOperations = true` cho api-001 trong `ApiContractIT` (file của A) để xác nhận chính thức; sau đó B không còn việc ở BL-23. BL-39 đã được A xử lý ở PR #24.

**Blocker/chờ người khác** (team-plan mục 8): A bật `checkOperations`; D (`IncidentRecorded`, `VitalSignApi`) cho T002, T063; E (012 `familyMemberName`, `registeredPhoneNumber`, nhãn quan hệ ở `FamilyRelationshipApi`) cho phiếu điều trị; A ghi `hiddenFields` cho cờ `hasTreatmentWish` (BL-42, BL-43).

**Kết quả test cuối** (MySQL 3307): `./mvnw clean verify` 185 đơn vị và kiến trúc, 835 tích hợp, 0 lỗi; 213 Vitest; `npm run lint`, `npm run build` qua. `FakeConsentApi` (A, PR #25) không trùng bean với `ConsentService` thật; M13 dùng `ConsentApi`, `ResidentApi`, `ResidentStatusChanged` tương thích, `FR_014_015_ConsentAndDeathIT`, `InboxIT` xanh.

**Lưu ý để bắt đầu Phase 10 (T071 → T076)**:
- Đọc `tasks.md` Phase 10 và dòng B ở `docs/team-plan.md` mục 8; `.specify/feature.json` là `{"feature_directory": "specs/001-elder-profile-assessment"}`.
- T071 (`HopDongApiTest` so springdoc với `api-001.yaml`): `ApiContractIT` của A đã so cả api-001 (đường dẫn, endpoint); phần so operation chờ A bật, nên T071 nên dùng phần đó chứ không viết test so sánh thứ hai.
- Luôn đặt `DB_URL`, `TEST_DB_URL` về `jdbc:mysql://localhost:3307/…` (biến máy trỏ 3306, không dùng); instance 3307 có thể bị tắt giữa chừng: nếu `Communications link failure` thì bật lại bằng `mysqld --no-defaults --datadir=<scratchpad phiên 221e9972>mysql3307data --port=3307 --mysqlx=OFF --skip-log-bin --max_connections=400`.
- Sau mỗi lần `verify`, quyền `elder_care_demo` mất (test DROP/CREATE user); khôi phục chưa làm, cần B cho phép.
- File `.java` trong cây làm việc dùng CRLF; script sửa bằng node cần xử lý CRLF (xem cách ở các script `edit*.js` trong scratchpad); `git commit -F <file>`; không có `gh`/token: PR tạo qua URL compare của GitHub.
- Không đánh dấu nghiệm thu giao diện khi B chưa tự thử; không đổi `CLAUDE.md`; không push/PR/merge khi B chưa duyệt.

## Ghi chú triển khai *(2026-10-24, Phase 10 một phần: T072, T075, T076)*

- **T076**: sự kiện `m01.api.events.OpinionRecordedFromFeedback(feedbackId, sourceModule, itemType, itemId, residentId, source)` (tên logic `YKienDaGhiTuPhanHoi`); cột `phan_hoi_id` thuộc bảng của 014 (data-model mục 6a), 001 không có bảng nào dùng nên chỉ khai báo sự kiện. Test `FR_023g_OpinionFromFeedbackIT` (3 test). 014 phát, 012 nghe: báo ở team-plan mục 8.
- **T075**: rà truy vết ghi ở data-model mục 12 (cuối bảng). Chưa có test nhưng có task: FR-047c (T082), FR-054 (T009).
- **T072** (bố cục chủ dự án duyệt 2026-10-24, DESIGN.md 5.19.2): tab "Danh mục" trong `AdminPage`, mã ở `frontend/src/features/catalogs-m01/` (`CatalogList`, `CatalogItemForm`, `CatalogReasonForm`, `catalogLabels.js`); bốn loại danh mục, lọc trạng thái và nhóm dị nguyên, thêm, sửa (If-Match = `version` của dòng), ngừng hiệu lực và kích hoạt lại kèm lý do; 12 Vitest (403 xem và thao tác, 409 trùng tên, 412). Nút thao tác chỉ hiện cho Quản lý viện theo ma trận `HEALTH_CATALOGS` (AC-01 xem, tạo, sửa; AC-03, AC-04 chỉ xem). **Điểm còn thiếu ở UC-69**: nghiep-vu.md Phụ lục 26 chỉ nêu "Cấu hình tham số" (Quản lý viện), không có dòng cho danh mục dị nguyên, mức độ dị ứng, nguồn thông tin, nhu cầu chăm sóc; ma trận nạp ở `V002_04` đã có `HEALTH_CATALOGS` (plan, điểm báo lại 3) nên web theo ma trận, không tự thêm quyền. Chưa nghiệm thu trên trình duyệt.
- **Đổi hợp đồng (chủ dự án duyệt 2026-10-24)**: schema `CatalogItem` của api-001 thêm `version` (int32, không bắt buộc) và `CatalogItemBody` trả `version`, vì `PUT /catalogs/{type}/{id}` cần `If-Match` mà danh sách không có nguồn phiên bản; test `BR_M01_11_CatalogIT.FR_018a_listCarriesVersionSoTheClientCanSendIfMatch`. `ApiContractIT` bật so operation tạm cho api-001 vẫn qua (0 lệch). Thay đổi cộng thêm, không ảnh hưởng module khác.
- **Sửa test flaky** (lỗi thật, không đổi hành vi): `BR_M01_01_TemporaryAbsenceOverdueIT` lấy mốc dự kiến trở về đúng giá trị đã đưa cho `open` (trước đọc lại `now()` lệch 1 µs vì đồng hồ test chạy tiếp) và so trong 1 mili giây; sai lệch nghiệp vụ (giờ) vẫn bị bắt.
- **Chưa làm / phần B tự làm được**: T071 — hợp đồng đã khớp (bật tạm `checkOperations` cho api-001 trong bản sao, `ApiContractIT` qua rồi hoàn lại); chỉ còn A đặt `true` ở file của A. T073 — chưa module nào ngoài 001 cài `StatusTransitionParticipant` thật (003–013 chưa vào develop), B không tự làm được. T074 — đã chạy `verify`, Vitest, lint, build; còn `flutter test` (mobile chưa mở), kịch bản quickstart mục 3 chạy tay trên profile demo (đa số đã có IT tương ứng, không đụng demo), đo SC cần người thật.
- **Kiểm thử** (MySQL 3307): `./mvnw clean verify` 186 đơn vị và kiến trúc, 839 tích hợp, 0 lỗi; 225 Vitest, `npm run lint`, `npm run build` qua.

## Ghi chú triển khai *(2026-10-24, đợt 1 sau rà soát: T011 phần phân loại trường, T018, T047)*

- **Che trường nhật ký sức khỏe (T011)**: thêm `HealthItemFieldClassification`, `InitialHealthRecordFieldClassification`, `AssessmentFieldClassification` (mọi trường nhóm `HEALTH`, `careLevel` của đánh giá nhóm `CARE_LEVEL`). **Đính chính mức độ**: rà soát ghi Critical "Hành chính đọc được nội dung sức khỏe", nhưng `AuditLogQueryService` đang an toàn theo hướng đóng: loại đối tượng chưa phân loại thì `history()` trả 404 cho người không phải Quản lý viện và `search()` chỉ cho Quản lý viện, nên chưa lộ dữ liệu. Phân loại cần thêm vì (1) data-model mục 10 yêu cầu Hành chính thấy "đã thay đổi" và Bác sĩ thấy giá trị, (2) khi lọc theo phạm vi của 000/002 chạy thật (hiện `history()` dựng `Target` không mang `residentId` nên người có quyền `ASSIGNED` luôn nhận 404; `ScopeAssignmentProvider` chưa ai gọi) thì trường chưa phân loại sẽ mặc định công khai nội bộ. `AuditLogHealthMaskingIT` chặn trôi: mọi trường lệnh thật ghi vào `audit_log` phải có trong phân loại. **Báo A**: `AuditLogQueryService.history` nên lấy `Target` từ `ScopeAssignmentProvider`; M01 chưa có provider này cho bản ghi con.
- **Bằng chứng qua đời (T047)**: `ResidentStatusService.apply` gắn tệp bằng `FileApi.attach(fileId, "M01.DEATH_EVIDENCE", historyId)` ngay sau khi ghi dòng lịch sử; tệp không có thật → 404, đã gắn → 409 `FILE_ALREADY_ATTACHED`, cả lệnh rollback (không đổi trạng thái, không gắn tệp). Chỉ số nguyên mới tính là bằng chứng (`isIntegralNumber`). Bác sĩ cũng được kèm bằng chứng. `DeathEvidenceFilePermission`: tải khi có quyền xem `RESIDENCY_END_DEATH` của hồ sơ, **loại người thân** dù ma trận cho AC-10 xem. **Quyết định tạm thời của chủ dự án (2026-10-24), chờ A xác nhận nghiệp vụ**: giấy tờ do cơ sở y tế cấp là chuyên môn nội bộ; chỉ vai trò có quyền xem "Kết thúc lưu trú, qua đời" theo chính sách hiện tại được tải, không mở rộng thêm. Web `StatusTab` vẫn nhập tay mã tệp, chưa có ô tải lên (thuộc T052, chưa làm trong đợt này).
- **Ảnh hồ sơ (T018)**: `ResidentProfileService` gắn ảnh khi tạo và khi đổi `photoFileId` (`M01.RESIDENT_PHOTO`, id hồ sơ); ảnh cũ vẫn gắn làm lịch sử (không xóa tệp). Trước sửa, ảnh không được gắn nên job dọn tệp chưa gắn xóa sau 24 giờ (test `NFR_08_attachedPhoto_isNotRemovedByTheUnattachedFileCleanup` đỏ rồi xanh). `ResidentPhotoFilePermission`: quyền xem hồ sơ trong phạm vi và thấy đủ nhóm nhận dạng.
- **Kiểm thử** (MySQL 3307): `./mvnw clean verify` 186 đơn vị và kiến trúc, 856 tích hợp, 0 lỗi. Instance 3307 đã tắt giữa chừng và được bật lại bằng đúng data dir cũ.

## Ghi chú triển khai *(2026-10-24, đợt 2 sau rà soát: T009, T081 → T083)*

- **Thông báo M01 → M13 (T009)**: chín sự kiện cài `HasNotificationRequests` qua lớp `m01.api.events.Notices` (mức, nhóm nhận, khóa sự kiện theo research H-14, FR-054): `ReassessmentDue` (Nhẹ) và `ReassessmentOverdue` (Trung bình) → Bác sĩ trực; `CareLevelChangeRequestNeeded` (Trung bình) và `...Reminder` (Nhẹ), `MissingConsentReminder` (Nhẹ) → Hành chính; `MissingEndOfLifeWishReminder`, `SelfDeterminationReviewReminder` (Nhẹ) → Bác sĩ (trực, hoặc Bác sĩ đã nhận định qua `Staff`) và Điều dưỡng phụ trách; `TemporaryAbsenceOverdue` (Trung bình) → Trưởng tầng của tầng (theo phân bổ giường qua `BedAllocationApi`) và Hành chính, không có tầng thì Hành chính và Quản lý viện. Phần sức khỏe chỉ ở nội dung đầy đủ loại `HEALTH`; nội dung rút gọn và tiêu đề không có họ tên, trường sức khỏe. Sáu sự kiện thêm `Instant at` (thời điểm từ `Clock` của lệnh phát), `TemporaryAbsenceOverdue` thêm `floorAreaId`, `SelfDeterminationReviewReminder` thêm `assessingDoctorStaffId` (tra qua `AccountApi.byId`); không module nào khác dùng các hàm dựng này.
- **Sự kiện mới `ResidentStatusNotice`**: Chuyển viện, Ghi nhận qua đời → người liên hệ chính, Khẩn cấp, `ABOUT_RESIDENT`, Q-354 `TRANSFER`/`DEATH` (Q-97, Q-353, Q-354), phát cùng `ResidentStatusChanged` trong `ResidentStatusService.apply`. **Lý do tách**: cho `ResidentStatusChanged` cài `HasNotificationRequests` thì `ResidentDeathListener` của 009 (nghe `ResidentStatusChanged`) bị luật `M13ArchTest.FR_001_exactlyOneListenerForNotificationRequests_inM13` của A đếm là listener thứ hai (phát hiện ở `clean verify`, đã tránh bằng sự kiện riêng, không sửa file của A). Tin báo qua đời có `occurredAt` bằng thời điểm qua đời nên 009 FR-015 không chặn.
- **Người nhận thật** còn phụ thuộc người khác: `OnDutyDoctor` cần `CaTrucApi` (E); `AssignedNurse` cần `AssignmentApi.staffAssignedToResident` (E); `PrimaryContact` cần `FamilyNotificationPort` (E, 012). `ResidentNotificationsIntakeIT` kiểm yêu cầu hợp lệ không thiếu dấu nào (trừ `NO_RECIPIENT` tùy dữ liệu), chống trùng theo khóa sự kiện, lệnh trạng thái khác không gửi.
- **Giấy tờ bệnh viện (T082)**: `ResidentStatusService` nhận `attachmentFileIds` trong `details` (REST gộp từ thân yêu cầu ở `StatusController`, đường cho module khác là `ResidentStatusApi.execute`, không đổi chữ ký): chỉ `RECORD_RETURN` từ `HOSPITALIZED` và `TRANSFER_OUT`, tối đa 10, trùng chỉ gắn một lần; lệnh khác → 422 `ATTACHMENTS_NOT_ACCEPTED`, quá 10 → 422 `TOO_MANY_ATTACHMENTS`. `FileApi.attach` vào dòng lịch sử (`M01.HOSPITAL_DOCUMENT`) cùng transaction: tệp không có → 404, đã gắn → 409 `FILE_ALREADY_ATTACHED`, cả lệnh rollback. `HospitalDocumentFilePermission`: quyền xem dị ứng, bệnh nền trong phạm vi (Hành chính, Kế toán không tải). Sửa lỗi ở cả hai provider tệp của lệnh trạng thái: bỏ `@Transactional(readOnly = true)` vì kiểm quyền theo phạm vi của 002 có thể ghi dấu khắc phục lỗi dữ liệu nguồn (provider người dùng thuộc phạm vi bị `TransientDataAccessResourceException`). `HospitalDocumentsIT` 10 test.
- **Bản giả**: `FakeEndOfLifeWishes` cho 007, 006 (`@Import(FakeEndOfLifeWishes.Config.class)`, không nằm trong `FakeM01Config` vì `@Primary`).
- **`FileApi.attachedTo` và `hospitalDocuments` (chủ dự án duyệt 2026-10-24)**: `FileApi` (A) thêm `List<FileView> attachedTo(String ownerType, long ownerId)` (`FileView(id, originalName, mimeType, sizeBytes)`, theo thứ tự tải lên, chỉ siêu dữ liệu), cài ở `FileService`/`FileStore.findAttached`; cộng thêm, không đổi `upload`, `attach`, `download` hay quyền tải. `ResidentStatusApi.hospitalDocuments(historyId)` (tên `giayToDinhKem` của hợp đồng) trả các tệp `M01.HOSPITAL_DOCUMENT` của dòng lịch sử, người gọi tải qua `GET /files/{id}` đã kiểm quyền. Ghi nhận thay đổi code của A ở docs/team-plan.md mục 8.
- **Web `StatusTab` (T082, bố cục duyệt 2026-10-24)**: khối "Giấy tờ bệnh viện" ở Ghi nhận trở về (chỉ khi đang Điều trị tại bệnh viện) và Chuyển viện: chọn nhiều tệp, danh sách tên kèm nút "Bỏ", tối đa 10 (kiểm trước khi tải), tệp tải lên khi bấm xác nhận rồi gửi `attachmentFileIds`; Ghi nhận qua đời dùng `FileField` thay ô nhập mã tệp và gửi `details.evidenceFileId`. `FileField` (consents) thêm tham số `multiple`. `StatusTab.test.jsx` 17 test. App `mobile/` chưa mở nên T082 còn `[ ]`.

## Ghi chú triển khai *(2026-10-24, đợt 3 sau rà soát: T013, T075)*

- **Kiểm kê nhật ký của M01** (DBR-23, FR-049): mọi lệnh người dùng của M01 ghi đúng một dòng `audit_log` có `resident_id` (supersede ghi thêm một dòng cho bản cũ), kể cả lệnh do module khác gọi (`ResidentStatusApi.execute`) và `markChecked`; người thực hiện, vai trò, thời điểm do 000 điền từ người dùng hiện tại. `M01CommandAuditTrailIT` (12 test) kiểm từng lệnh; `M01AuditCompletenessIT` giữ phần đếm `INSERT` cho lệnh danh mục.
- **Ba chỗ thiếu tìm ra và đã sửa trong M01**: (1) `CareLevelApiImpl.applyChange` (004 gọi khi yêu cầu thay đổi lưu trú được duyệt) đổi mức chăm sóc, nhóm 2, mà không ghi nhật ký; nay ghi `CARE_LEVEL_CHANGED` loại `M01.RESIDENT` (trước/sau `currentCareLevel`, căn cứ là yêu cầu phê duyệt, `effectiveFrom` ở chi tiết); trường `currentCareLevel` đã thuộc nhóm mức chăm sóc. (2) Đính chính lần đánh giá, hồ sơ sức khỏe ban đầu: dòng nhật ký `CORRECTION_ADDED` của 000 không có `resident_id` (`CorrectableRecordProvider` không cho biết người cao tuổi) nên tra theo người cao tuổi không thấy; hai provider của M01 ghi thêm dòng `RECORD_CORRECTED` cùng bản gốc, gắn người cao tuổi, trỏ tới bản đính chính (`correctionId`, `sequenceNo`, loại), không chép nội dung (có thể là dữ liệu sức khỏe). Đây là cách tạm trong phạm vi M01; cách đúng là SPI trả `residentId`, ghi ở docs/team-plan.md mục 8 cho A. (3) Phân loại trường: kiểm mọi trường lệnh ghi vào nhật ký của `M01.RESIDENT`, `M01.CONSENT`, `M01.END_OF_LIFE_WISH`, `M01.TREATMENT_WISH`; trường không phải dữ liệu nhạy cảm (`status`, `supersededById`, `source`, `consenterType`, `photoConsent`, `notificationConsent`, `consentedAt`, `evidenceFileId`, họ tên, ngày sinh, giới tính, mã hồ sơ) cố ý để nhóm công khai nội bộ, ghi thành danh sách cho phép trong test. **Chủ dự án chỉ đạo 2026-10-24**: `specialNotes` (văn bản tự do, có thể chứa tình trạng sức khỏe) của `M01.RESIDENT` và `familyMemberIdsAllowedHealthView` (người thân được xem sức khỏe) của `M01.CONSENT` thuộc nhóm `HEALTH`, không để công khai nội bộ chỉ vì Hành chính xem được trên giao diện. Phân loại chỉ dùng để che giá trị khi tra nhật ký (`AuditLogQueryService` là nơi duy nhất dùng), không đổi quyền xem hồ sơ hay bản đồng ý (kiểm bằng `M01CommandAuditTrailIT`: Hành chính vẫn xem `specialNotes` trên hồ sơ; `FieldVisibilityApi` thật cho Hành chính không xem được nhóm `HEALTH`, Bác sĩ xem được). Test không đọc qua `AuditLogQueryService.history` vì hàm đó trả 404 cho người xem theo phạm vi (lỗi của A đã báo ở đợt 1).
- **Không coi là dữ liệu cần nhật ký**: `reassessment_notice`, `periodic_reminder`, `return_overdue_alert` là sổ theo dõi nhắc, cảnh báo do job ghi (không phải dữ liệu nhóm 2, 3 của người cao tuổi); thay đổi trạng thái thật đã có `RESIDENT_STATUS_CHANGED`.
- **Chưa kiểm**: `AuditLogQueryService.history()` của A (đã báo ở đợt 1) vẫn chặn người xem theo phạm vi; test đọc thẳng `audit_log`, không qua API tra cứu.

## Ghi chú triển khai *(2026-10-24, đợt 4: lượt hoàn thiện cuối của B trước khi Opus review)*

- **Lưu giữ dữ liệu hết hạn (T011, T079; data-model mục 11)**: `ResidentRetention` (gốc `M01.RESIDENT`, CFG-M15-03, mốc là lúc hồ sơ vào trạng thái cuối) và bảy `ResidentChildRetention` (`M01.RESIDENT_STATUS_HISTORY`, `M01.CONSENT`, `M01.INITIAL_HEALTH_RECORD`, `M01.HEALTH_ITEM`, `M01.ASSESSMENT`, `M01.END_OF_LIFE_WISH`, `M01.TREATMENT_WISH`). **Quyết định thiết kế, rút ra từ một lỗi test bắt được**: bản ghi con chỉ có mặt để nhật ký và đính chính của chúng đi theo chuỗi; `referencesTo` của bản ghi con trả về hồ sơ nên mọi chuỗi bắt đầu từ bản ghi con đều kéo cả hồ sơ, và `delete` của bản ghi con không làm gì. Mọi dòng dữ liệu chỉ bị xóa trong `ResidentRetention.delete`, một transaction, theo thứ tự: bảng không nhật ký riêng, bảng con, bản ghi con theo mã tăng dần (vì `superseded_by_id` trỏ tới bản mới hơn), hồ sơ. Bản đầu cho bản ghi con tự là gốc chuỗi nên có thể bị xóa riêng khi hồ sơ còn được giữ (lịch sử trạng thái mất, hồ sơ còn); `ResidentRetentionIT` bắt được và đã sửa. Migration không đổi: quyền `DELETE` và trigger cho `eldercare_retention` đã có từ V001_02 → V001_10.
- **Chưa xóa được thật** vì `AuditLogRetention` của A giữ mọi dòng nhật ký gắn người cao tuổi; `ResidentRetentionIT` mô phỏng bước đó qua kết nối lưu giữ và có thêm test chuỗi được giữ khi còn nhật ký. Tệp đính kèm chưa lưu giữ (xóa nội dung S3 cần quyết định). Cả hai hướng đều là giữ lại nhiều hơn, không mất dữ liệu.
- **Gắn phạm vi bản ghi con (T011)**: `ChildScopeAssignment` cho bảy loại con, `Target` mang `residentId`, tầng để trống; mã lạ → không có người cao tuổi.
- **`statusPeriods` (T049)**: dựng từ lịch sử trạng thái, lấy khoảng giao với [từ, đến], trả nguyên khoảng không cắt (người gọi, tức 003, tự tính phần giao); khoảng kết thúc đúng lúc `from` không giao.
- **Đính chính lịch sử trạng thái (T050)**: `StatusHistoryCorrectionProvider` (loại `M01.RESIDENT_STATUS_HISTORY`): chỉ đính chính được `reason`; cách A cho người ghi gốc, trên hồ sơ trạng thái cuối chỉ cách C (người ghi gốc bị chặn như với lần đánh giá, đồng nghiệp cùng vai trò lập yêu cầu); `history()` hiển thị lý do đã đính chính, bản gốc giữ nguyên. Có ghi thêm dòng `RECORD_CORRECTED` gắn người cao tuổi như hai provider đính chính khác.
- **Sự kiện T076**: `sourceModule = "M04"`, `itemType = "INTEREST"`, bản dựng sự kiện từ chối giá trị khác và id không dương; test đi qua kho sự kiện Modulith thật.
- **Tab Danh mục (T072; Opus #12, #13)**: theo quyền thật `HEALTH_CATALOGS` từ `GET /me/permissions`; `AdminPage` bọc `PermissionsProvider` vì `App` chưa gắn.

## Ghi chú triển khai *(2026-10-24, lượt sửa cuối sau Opus final review)*

- **Opus #6 (High) đã sửa**: `reason`, `basis`, `conditionWaiver` của `StatusHistoryEntryBody` thuộc nhóm `HEALTH` (`@FieldGroup`, `VisibilityTarget` theo người cao tuổi của dòng, `hiddenFields` mới trong schema `StatusHistoryEntry` của api-001); áp cho cả `GET /residents/{id}/status-history` và `StatusCommandResult.history`. Hành chính, Kế toán, người thân chưa được chia sẻ sức khỏe vẫn thấy từ, đến, lệnh, thời điểm, người thực hiện; lý do đã đính chính cũng bị che. Quyền xem lịch sử (`RESIDENT_PROFILE VIEW`) không đổi. `HistoryEntry` của service thêm `residentId`. Web: `StatusTab` báo "lý do và căn cứ chỉ hiện với người được xem thông tin sức khỏe". Test: `StatusHistoryVisibilityIT` 7 test (Hành chính, Kế toán, Bác sĩ, Điều dưỡng được phân công và không được phân công, người thân có và không có trong bản đồng ý, đính chính, kết quả lệnh); `StatusEndpointsIT` sửa một dòng cũ (Hành chính không còn thấy lý do ở kết quả lệnh).
- **`SelfHiddenFields` (002, "Đổi api" 2026-10-24)**: `ProfileBody` cài giao diện, cờ `hasTreatmentWish` `null` thì ẩn và ghi vào `hiddenFields` (Kế toán); `TreatmentWishVisibilityIT` thêm kiểm `hiddenFields` và kiểm người thấy cờ thì không bị liệt kê.
- **Hai lớp A viết trong m01** (`ResidentProfileForAccount`, `ResidentAccountLockOnFinalStatus`): tương thích, `ResidentProfileForAccountIT`, `ResidentAccountFinalStatusIT` qua. Lưu ý `inResidence` của A chỉ đúng khi `IN_RESIDENCE` (Tạm vắng, Chuyển viện không tính).
- **`ResidentStatisticsApi` (016)**: hợp đồng đủ (ba hàm, ba DTO) nhưng chưa có task 001 và chưa có bản cài hay bản giả; không làm trong lượt này, đề xuất lượt riêng sau PR 001 (team-plan mục 8).
- **`SourceRecord` của `ResidentStatusNotice`** đổi thành `M01.RESIDENT_STATUS_HISTORY` (cùng loại với nhật ký, đính chính, lưu giữ); chưa có bản ghi nào dùng tên cũ.
- **Retention**: theo quyết định chủ dự án, không xóa vật lý tệp S3, không thêm `FileApi.delete`; T011 giữ `[ ]`. Việc A phải xong trước khi bật xóa thật (team-plan mục 8): `AuditLogRetention` theo mốc hồ sơ, SPI phần đi kèm cho tệp, cơ chế khai tham chiếu giữa các module, quy tắc lifecycle S3 (bucket bật versioning).
- **`checkOperations` (T071 `[x]`)**: bản tạm báo 0 lệch, chủ dự án duyệt nên `ApiContractIT` (file của A) đổi api-001 sang `checkOperations = true` (một dòng và dòng chú thích); `ApiContractIT` chính thức qua. Tổng 74/85 task `[x]`.
- **Đồng bộ develop**: merge `origin/develop` `b049b1f`; xung đột chỉ ở `DESIGN.md` (phiên bản 5.20.1 cho bố cục tab Danh mục, giữ 5.20.0 của 002) và `docs/team-plan.md` (giữ đủ dòng A và B).
- **Kiểm thử cuối (MySQL 3307)**: `./mvnw clean verify`: 244 test đơn vị và kiến trúc, 936 test tích hợp, 0 lỗi; lần chạy đầu dừng ở `spotless:check` vì `ResidentStatusNotice.java` lệch dòng sau `sed`, đã `spotless:apply` và `verify -DskipTests` BUILD SUCCESS. Vitest 247, lint sạch, build đạt (sau `npm ci` vì develop thêm `qrcode.react`).

## Bàn giao cho Opus review cuối *(2026-10-24)*

### 1. Tổng tiến độ 85 task của Phase 1 → 10
- **73 task `[x]`**, **12 task `[ ]`**. Không đánh dấu `[x]` task còn phụ thuộc thật.
- Đã hoàn thành: T001, T003 → T010, T012, T012a, T013, T013a, T014 → T018, T020 → T022, T024 → T032, T034 → T041, T043 → T051, T053 → T062, T064 → T070, T072, T075 → T081, T083.
- Chưa hoàn thành (12):

| Task | Điều kiện còn thiếu | Ai |
| --- | --- | --- |
| T002 | `ChiSoApi`, sự kiện `SuCoDaGhiNhan` do 007 khai báo, chưa có trong `develop` | D |
| T011 | Lưu giữ tệp đính kèm: cần quyết cách xóa nội dung S3 (`FileApi` chưa có hàm xóa); phần còn lại xong | Chủ dự án |
| T019 | Lọc theo tầng, phòng cần phân bổ giường của 003; hợp đồng api-001 chưa có tham số lọc này | C (003) |
| T023, T033, T042, T052, T082 | Phần web đã xong; phần app `mobile/` chưa mở (CLAUDE.md) | Chủ dự án mở mobile |
| T063 | Listener sự cố cần `IncidentRecorded` của 007 | D |
| T071 | So sánh operation api-001 với `checkOperations = true`: file `ApiContractIT` của A | A |
| T073 | Chạy lại `FR_045`, `FR_047` với bean `StatusTransitionParticipant` thật của 003 → 013 | C, D, E, B (004) |
| T074 | `flutter test`, kịch bản quickstart mục 3, đo SC bằng người thật | Nghiệm thu |

### 2. Kết quả xử lý 16 vấn đề của báo cáo Opus

| # | Vấn đề | Kết quả | Bằng chứng |
| --- | --- | --- | --- |
| 1 | Tra nhật ký lộ dữ liệu sức khỏe (thiếu phân loại trường) | **Đã sửa** (đợt 1) và siết ở đợt 3: `specialNotes`, `familyMemberIdsAllowedHealthView` thuộc `HEALTH` | `AuditLogHealthMaskingIT`, `M01CommandAuditTrailIT` |
| 2 | Bằng chứng qua đời chỉ kiểm "là số" | **Đã sửa** (đợt 1): kiểm tệp, gắn tệp, quyền tải, web tải tệp (đợt 2) | `RecordDeathIT` |
| 3 | Ảnh không gắn tệp | **Đã sửa** (đợt 1) | `ResidentPhotoIT` |
| 4 | M01 không gửi thông báo | **Đã sửa phía M01** (đợt 2); người nhận thật chờ E (BL-47) | `ResidentNotificationRequestsTest`, `ResidentNotificationsIntakeIT` |
| 5 | Giấy tờ bệnh viện chưa làm | **Đã sửa** backend, web, `hospitalDocuments`; mobile chưa | `HospitalDocumentsIT`, `StatusTab.test.jsx` |
| 6 | Lịch sử trạng thái có thể lộ lý do, căn cứ, miễn điều kiện | **Đã sửa (lượt cuối 2026-10-24)**: nhóm `HEALTH`, `hiddenFields` | `StatusHistoryVisibilityIT` 7 test |
| 7 | Truy vết DBR-23 chưa đủ | **Đã sửa** (đợt 3); T075 từng ghi quá sớm, đã điều chỉnh | `M01CommandAuditTrailIT` 14 test |
| 8 | Thiếu `RetentionProvider` | **Đã sửa phía M01**; chưa xóa thật do A và tệp chờ quyết định | `ResidentRetentionIT` 4 test |
| 9 | Thiếu provider đính chính lịch sử trạng thái | **Đã sửa** | `StatusHistoryCorrectionIT` 4 test |
| 10 | Hợp đồng sự kiện T076 còn lỏng | **Đã sửa**: bộ giá trị `M04`, `INTEREST`; glossary, module-interfaces, team-plan (014 do C) | `FR_023g_OpinionFromFeedbackIT` |
| 11 | `statusPeriods` chưa cài | **Đã sửa** | `StatusPeriodsIT` 5 test |
| 12 | Hành chính thấy tab Danh mục luôn nhận 403 | **Đã sửa**: ẩn tab khi không có quyền xem | `AdminPage.test.jsx` |
| 13 | Nút sửa danh mục theo vai trò | **Đã sửa**: theo CREATE, UPDATE thật | `AdminPage.test.jsx` |
| 14 | Test T076 tự khẳng định | **Đã sửa**: đi qua kho sự kiện Modulith | `FR_023g_OpinionFromFeedbackIT` |
| 15 | T013a tên tiếng Việt | **Đã sửa**: `FR_023h_NoCapacityFlagIT` | `[x]` |
| 16 | `specialNotes` xếp công khai nội bộ | **Đã sửa** theo chỉ đạo: nhóm `HEALTH` | `M01CommandAuditTrailIT` |

Không còn vấn đề mở ở phía B. #4, #5, #8 đã xong phần B, còn phụ thuộc người khác hoặc mobile như bảng trên.

### 3. Lỗi và hạn chế còn tồn tại

| Mức | Vấn đề | Bằng chứng | Ai |
| --- | --- | --- | --- |
| Trung bình | Dữ liệu M01 hết hạn chưa bao giờ bị xóa thật: `AuditLogRetention` giữ nhật ký gắn người cao tuổi | `ResidentRetentionIT.FR_050_residentLinkedAuditLog_keepsTheWholeChain…` | A |
| Thấp | Đính chính "Hủy ghi nhận" (VOID) áp dụng được cho dòng lịch sử trạng thái nhưng không đổi trạng thái hiện tại; provider không chặn được | không có test; do SPI của A | A |
| Thấp | Đính chính không có `resident_id` ở dòng của 000; M01 ghi dòng phụ `RECORD_CORRECTED` (BL-48) | `M01CommandAuditTrailIT` | A |
| Thấp | `AuditLogQueryService.history()` trả 404 cho người xem theo phạm vi | báo từ đợt 1 | A |
| Thấp | Tệp đính kèm không được lưu giữ; nội dung S3 không bao giờ bị xóa | data-model mục 11 | Chủ dự án |
| Thấp | `PermissionsProvider` chưa gắn ở `App`; `AdminPage` tự bọc | `AdminPage.jsx` | A |
| Thấp | Giao diện web mới chưa nghiệm thu trên trình duyệt: `StatusTab` (giấy tờ bệnh viện, bằng chứng qua đời), tab Danh mục theo quyền | chỉ có Vitest | Chủ dự án |

### 4. Kết quả kiểm thử cuối cùng (MySQL 3307; không đụng 3306, dữ liệu demo, lịch sử Flyway)
- (Số liệu đợt 4; số liệu cuối xem ghi chú "lượt sửa cuối" ở trên: 244 đơn vị, 936 tích hợp, 247 Vitest) `./mvnw clean verify`: **199 test đơn vị và kiến trúc, 905 test tích hợp, 0 lỗi**, BUILD SUCCESS (gồm `ModuleBoundaryTest`, `LayerArchTest`, `M13ArchTest`, `ApiContractIT`).
- Frontend: **237 Vitest**, `npm run lint` sạch, `npm run build` đạt. `prettier --check` còn báo 6 file lệch định dạng (cũ và file sửa có dòng cũ); không áp hàng loạt theo chỉ đạo.
- Test mới của đợt 2 → 4: `HospitalDocumentsIT` 11, `ResidentNotificationRequestsTest` 12, `ResidentNotificationsIntakeIT` 5, `M01CommandAuditTrailIT` 14, `StatusPeriodsIT` 5, `StatusHistoryCorrectionIT` 4, `ResidentRetentionIT` 4, `ChildScopeAssignmentIT` 3, `FR_023g_OpinionFromFeedbackIT` 5, `AdminPage.test.jsx` 5, 7 test `StatusTab`.

### 5. Thay đổi API, migration, file chung cần A xem
- **Migration: không có** trong cả bốn đợt; không sửa migration đã áp dụng.
- **`common.file`** (đợt 2, chủ dự án duyệt): `FileApi.attachedTo(ownerType, ownerId)`, record `FileView`; cộng thêm, không đổi `upload`, `attach`, `download`.
- **Provider M01 cài cho 000, 002** (không sửa code A): `StatusHistoryCorrectionProvider`, `ResidentRetention` và bảy `ResidentChildRetention`, `ChildScopeAssignment` (bảy loại), phân loại trường `specialNotes` và `familyMemberIdsAllowedHealthView`.
- **File chung đã sửa**: `frontend/src/features/admin/AdminPage.jsx` (bọc `PermissionsProvider`, ẩn tab theo quyền; T072 đã được duyệt), `docs/team-plan.md`, `specs/000-shared-foundation/glossary.md` (chỉ mục 8 của 001).
- **Cần A**: `AuditLogRetention` (nhật ký gắn hồ sơ), `CorrectableRecordProvider.residentId` và kiểu đính chính (BL-48), `AuditLogQueryService.history()`, gắn `PermissionsProvider` ở `App`, bật `checkOperations` cho api-001. Chi tiết ở docs/team-plan.md mục 8.
- **Cần chủ dự án quyết định**: cách lưu giữ tệp đính kèm (S3); che lý do, căn cứ lịch sử trạng thái (#6).

### 6. Phần chưa nghiệm thu
- Web: toàn bộ giao diện 001 chưa được chủ dự án nghiệm thu trên trình duyệt (T052, T072, T033, T042 ghi rõ).
- Mobile: chưa mở nên T023, T033, T042, T052, T082 giữ `[ ]`; không có `flutter test`.
- Quickstart mục 3 và đo SC (T074): chưa chạy.

### 7. Git
- Nhánh `impl/001-elder-profile-assessment`; stash `CP-001-P2-IMPL` còn nguyên. Chưa push, PR, merge.
- Commit đợt 4: `71ebe67` (lưu giữ, gắn phạm vi, T011/T079), `613db2c` (`statusPeriods`, đính chính lịch sử, T049/T050), `5c41d25` (sự kiện T076, đổi tên T013a), `a1407aa` (tab Danh mục theo quyền, T072); commit tài liệu cuối cùng là HEAD (xem `git log`).
- Đợt 3: `208c0a2`, `8153393`. Đợt 2: `3489df9`, `1495bcf`, `fc36c9f`, `91bb43a`. Đợt 1: `e960560`, `f35b9bd`, `842c6bc`, `5417206`.

### 8. File và chức năng Opus cần kiểm tra kỹ
1. `m01/internal/retention/service/ResidentRetention.java`, `ResidentChildRetention.java`, `ResidentRetentionBeans.java`: thứ tự xóa, FK tự tham chiếu, ranh giới "chỉ xóa cùng hồ sơ"; `ResidentRetentionIT`. Đây là mã xóa dữ liệu, kiểm kỹ nhất.
2. `m01/internal/status/service/ResidentStatusService.java` (`periods`, `history` với đính chính, `apply` đính kèm tệp và phát `ResidentStatusNotice`) và `StatusHistoryCorrectionProvider.java`.
3. `m01/internal/assessment/service/CareLevelApiImpl.applyChange` (nhật ký mới) và hai provider đính chính ghi `RECORD_CORRECTED`.
4. `m01/api/events/*` (chín sự kiện `HasNotificationRequests`, `Notices`, `ResidentStatusNotice`, `OpinionRecordedFromFeedback`): mức, nhóm người nhận, khóa chống trùng.
5. `m01/internal/status/service/HospitalDocumentFilePermission.java`, `DeathEvidenceFilePermission.java`, và `common/file` (`attachedTo`).
6. Phân loại trường: `ResidentFieldClassification`, `ConsentFieldClassification` và danh sách "cố ý công khai nội bộ" trong `M01CommandAuditTrailIT`.
7. Web: `frontend/src/features/status/StatusTab.jsx` (tải nhiều tệp), `features/admin/AdminPage.jsx`, `features/catalogs-m01/CatalogList.jsx`.
