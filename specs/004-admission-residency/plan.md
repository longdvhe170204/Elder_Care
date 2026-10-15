# Implementation Plan: Tiếp nhận và lưu trú

**Branch**: `004-admission-residency` | **Date**: 2026-10-07 | **Spec**: [spec.md](spec.md)

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

## Technical Context

**Language/Version**: Java 21 LTS; TypeScript 5.x; Dart 3.x với Flutter stable 3.x (000 R-01).

**Primary Dependencies**: Như 000 → 003: Spring Boot 3.5.x, Spring Modulith 1.4.x, Flyway 11.x, springdoc-openapi 2.8.x, ShedLock, Spring Security 6.5. Web: React 19, TanStack Query. App: Flutter. Không thêm thư viện mới.

**Storage**: MySQL 8.4 LTS. Bảng ở [data-model.md](data-model.md). Tiền `DECIMAL(15,0)` VND. Bản scan, bằng chứng qua `common.tep` (001 H-13).

**Testing**:
- Như 000 R-14.
- Test đơn vị dạng bảng cho bốn hàm thuần: điểm ưu tiên, tra chính sách vắng, điều khoản khác chuẩn, nội dung tại ngày.
- Testcontainers cho trigger, cột sinh.
- Test đồng thời cho DBR-06, FR-046 và "Đã liên hệ".
- Đồng hồ giả lập cho mọi CFG-M02, gồm chạy bù.
- Bản giả cho SPI của 005, 010, 012, 017 và bean điều kiện của 006, 007, 010, 013, 017 (research T-16).

**Target Platform**: Như 000.
- Web: mọi màn của 004.
- App nhân viên: Cho tạm vắng, Gia hạn dự kiến, Ghi nhận trở về cho Trưởng tầng; xem lượt vắng cho Điều dưỡng.
- App người thân: tiến trình tiếp nhận, hợp đồng và phụ lục (người đại diện), lượt vắng, tiến trình kết thúc, gửi yêu cầu thay đổi ở Nháp.
- Lệnh của 004 chỉ chạy trực tuyến (ngoài phạm vi 8.6).

**Project Type**: Web service (REST `/api/v1`), một web client và hai mobile client.

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
| X. Kiến trúc | Đạt | Một khối; gói `m02`; module khác chỉ dùng `m02.api`. Không có vòng phụ thuộc (T-02). REST `/api/v1`; MySQL; ba client. NFR ở bảng bên dưới |
| XI. Nghiệp vụ ở backend | Đạt | Điều kiện, bảng chuyển, quyền, giới hạn trường ở backend. Mỗi lệnh có `POST …/{id}/<lenh>` riêng; lệnh trạng thái người cao tuổi dùng endpoint của 001 với `chiTiet` do 004 kiểm |
| XII. Dữ liệu | Đạt | DBR-06 có khóa và trigger; DBR-07 có `UNIQUE NOT NULL`; DBR-08 có khóa và trigger; nhóm 3 theo mẫu của 000; nhật ký trong transaction; thời gian theo `Clock` |
| XIII. Tham số | Đạt | Như V. Bảng CFG-M02-05, CFG-M02-08 đọc giá trị hiện hành tại lúc tính (FR-052, FR-077) |
| XIV. Ngoại tuyến, thông báo | Đạt | Lệnh của 004 chỉ trực tuyến. Mọi thông báo là sự kiện tới 009, có mức (T-12) |
| XV. Truy vết, kiểm thử | Đạt | Tên test theo BR, FR. Mỗi BR của 004 có test (data-model mục 15). Không có BR nào của 004 thực hiện ở client |

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
└── tasks.md                     # Phase 2 (/speckit-tasks; chưa tạo)
```

### Source Code (repository root)

Cấu trúc dự kiến cho giai đoạn triển khai. Giai đoạn thiết kế không tạo các thư mục này (CLAUDE.md).

```text
backend/src/main/java/vn/eldercare/m02/
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

web/src/tinh-nang/
├── dang-ky-tiep-nhan/       # lượt đăng ký, tiến trình, điều kiện tiếp nhận
├── danh-sach-cho/           # hồ sơ chờ, điểm, hồ sơ phù hợp với giường, liên hệ
├── hop-dong/                # nháp, điều khoản khác chuẩn, gửi ký, ghi nhận đã ký, nội dung tại ngày, phụ lục
├── dich-vu-don-gia/
├── yeu-cau-thay-doi/
├── vang-mat/                # lượt vắng, ngày vắng, quyết định giữ giường, báo vắng, ngày nghỉ bán trú
├── ket-thuc-luu-tru/
└── qua-doi/

app-nhan-vien/lib/tinh-nang/vang-mat/      # Cho tạm vắng, Gia hạn, Ghi nhận trở về
app-nguoi-than/lib/tinh-nang/
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
