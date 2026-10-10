# Implementation Plan: Hồ sơ và đánh giá người cao tuổi

**Branch**: `001-elder-profile-assessment` | **Date**: 2026-10-07 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-elder-profile-assessment/spec.md`

## Summary

Feature 001 là module M01. Nó cung cấp:
1. Hồ sơ cá nhân người cao tuổi, tra cứu theo quyền, liên kết hồ sơ của người quay lại (mục A; UC-01, UC-02).
2. Bản đồng ý chia sẻ dữ liệu, là căn cứ cho chú thích ¹ của ma trận quyền (mục B; UC-03).
3. Hồ sơ sức khỏe ban đầu; dị ứng, bệnh nền, tiền sử bệnh lưu thành từng mục chỉ loại trừ (mục C; UC-04).
4. Phiếu nguyện vọng cuối đời có phiên bản (mục C1; UC-82).
5. Đánh giá đầu vào và đánh giá lại bằng nhận xét, lưu ý chăm sóc, mức và nhu cầu chăm sóc; báo đánh giá lại (mục D, E; UC-05 → UC-07).
6. Máy trạng thái người cao tuổi: mọi lệnh chuyển trạng thái, điều kiện chặn, lịch sử, mức chăm sóc hiện hành (mục F, G; UC-08, cùng UC-15 → UC-18 của 004).

Hướng kỹ thuật:
- Một điểm thực hiện chuyển trạng thái, khóa dòng hồ sơ. Các module có điều kiện hoặc tác động cài SPI `ThamGiaChuyenTrangThai`. Điều kiện được gộp và trả đủ trong một lần. Tác động chạy trong cùng transaction, nên hoặc toàn bộ, hoặc không (research H-02).
- Quy tắc đếm và quy tắc bắt buộc đưa xuống MySQL bằng cột sinh có `UNIQUE`, `CHECK` và trigger. Các quy tắc gồm: CCCD duy nhất trong hồ sơ chưa ở trạng thái cuối; tối đa một bản đồng ý, một phiên bản nguyện vọng, một bản nháp đánh giá; đánh giá xác nhận có nhận xét và mức (H-04, H-06, H-09, H-10).
- Kiểm tra dị ứng với đơn thuốc và chế độ ăn chạy sau commit qua outbox, có cờ "chờ kiểm tra" và job thử lại. Mục dị ứng không bao giờ mất vì 006 hay 011 lỗi (H-07).
- "Cần đánh giá lại" là trạng thái tính được từ lần báo; không có bản ghi yêu cầu (Q-307, H-11).
- Thêm thành phần tệp đính kèm dùng chung `common.tep` cho ảnh và bản scan (H-13).

Plan tuân theo FR-010a của 000 (Q-306, Q-307): M01 không tự sinh bản ghi nghiệp vụ, không gợi ý. Sáu job chỉ báo, nhắc, cảnh báo theo thời gian. Mọi thay đổi khác là lệnh của actor, hoặc tác động kèm lệnh đó.

## Technical Context

**Language/Version**: Java 21 LTS; TypeScript 5.x; Dart 3.x với Flutter stable 3.x (000 R-01).

**Primary Dependencies**: Như 000 và 002: Spring Boot 3.5.x, Spring Modulith 1.4.x, Flyway 11.x, springdoc-openapi 2.8.x, ShedLock, Spring Security 6.5 (002). Web: React 19, TanStack Query. App: Flutter. Không thêm thư viện mới.

**Storage**:
- MySQL 8.4 LTS (000 R-01). Bảng ở [data-model.md](data-model.md).
- Cột so khớp họ tên dùng collation `utf8mb4_0900_as_ci` (H-04).
- Tệp đính kèm lưu trên volume của máy chủ, siêu dữ liệu ở bảng `tep` (H-13).

**Testing**:
- Như 000 R-14 và 002 A-14.
- Bean giả cho mọi `ThamGiaChuyenTrangThai` của module chưa có (CHK035, CHK045).
- Testcontainers cho trigger, cột sinh và `CHECK`; đồng hồ giả lập cho CFG-M01-01, 02, 03, 06 và CFG-M03-02.
- Test đồng thời cho FR-013, FR-048.

**Target Platform**: Như 000.
- Web: mọi màn của 001.
- App nhân viên: tra cứu, thẻ người cao tuổi, ghi mục sức khỏe, lập nháp đánh giá, các lệnh trạng thái của Trưởng tầng, Điều dưỡng, Bác sĩ.
- App người thân: xem hồ sơ (phần sức khỏe theo chú thích ¹).
- Lệnh của 001 chỉ chạy trực tuyến. 8.6 chỉ cho ghi tạm với "ghi nhận thực hiện", không có lệnh nào của 001.

**Project Type**: Web service (REST `/api/v1`), một web client và hai mobile client.

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
- 6 job.
- Khoảng 35 endpoint (contracts/api-001.yaml).

Không còn mục NEEDS CLARIFICATION. Các lựa chọn kỹ thuật đã chốt ở [research.md](research.md). Các điểm về tài liệu nguồn ghi ở "Điểm cần báo lại"; chúng không chặn thiết kế.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Bằng chứng |
| --- | --- | --- |
| I. Nguồn gốc, truy vết | Đạt | Mọi quyết định dẫn FR, BR, DBR; data-model mục 12 là bảng truy vết DBR-01 → 05, 23, 25, 34 và BR-M01-01 → 13 |
| II. Spec không nêu công nghệ | Đạt | Spec không đổi; công nghệ chỉ ở plan, research |
| III. Không mặc định CRUD | Đạt | Nhóm 2 (hồ sơ, bản đồng ý, mục sức khỏe, nguyện vọng) chỉ có lệnh. Nhóm 3 (lịch sử, đánh giá, hồ sơ sức khỏe ban đầu sau xác nhận) chỉ thêm và đính chính. "Sửa nháp" chỉ khi chưa xác nhận. Danh mục theo mẫu nhóm 1 |
| IV. Trạng thái kiểm tra được | Đạt | Bảng chuyển ở data-model mục 2, 4 → 7 khớp hai bảng trạng thái của spec; mỗi kịch bản có test (quickstart mục 4) |
| V. Không cố định giá trị | Đạt | CFG-M01-01, 02, 03, 06, CFG-M03-02 đọc từ `ThamSoApi`. Chu kỳ job 15 phút, giới hạn tệp là thông số kỹ thuật (Complexity Tracking) |
| VI. Vai trò, quyền | Đạt | Khóa quyền theo dòng Phụ lục 27 (H-05); 19.3 qua `@NhomTruong` của 002; không thêm vai trò. Chuyển viện chưa có dòng ở Phụ lục 27, tạm dùng UC-16 và BR-M01-06, báo lại ở điểm 1 |
| VII. Điểm chưa rõ | Đạt | Không tự đoán nghiệp vụ. Chỗ nguồn còn thiếu ghi ở "Điểm cần báo lại"; thiết kế chọn cách hẹp nhất (ví dụ chỉ liên kết hồ sơ qua CCCD) |
| VIII. Tiêu chí thành công | Đạt | SC-001 → SC-013 có cách đo ở quickstart mục 5 |
| IX. Phạm vi giai đoạn | Đạt | Chỉ tài liệu thiết kế; không mã, không `src/`, không cấu hình build, không SQL; không đổi nghiệp vụ. Hợp đồng của 000 được bổ sung giao diện (`TepApi`, `chuyenMoRong`), không đổi hành vi đã thiết kế |
| X. Kiến trúc | Đạt | Một khối; gói `m01`; module khác chỉ dùng `m01.api`; điều kiện, tác động chéo module đi qua SPI. REST `/api/v1`; MySQL; ba client. NFR ở bảng bên dưới |
| XI. Nghiệp vụ ở backend | Đạt | Mọi điều kiện chặn, bảng chuyển, quyền, giới hạn trường ở backend; `lenhDuocPhep` chỉ để hiển thị; mỗi lệnh trạng thái có `POST …/{id}/<lenh>` riêng |
| XII. Dữ liệu | Đạt | Trigger theo mẫu 000 cho mọi bảng nhóm 3 và cho nội dung mục sức khỏe; cột sinh `UNIQUE` cho DBR-01, FR-013, DBR-34, BR-M01-13; `CHECK` cho DBR-05; nhật ký trong transaction; thời gian theo `Clock` |
| XIII. Tham số | Đạt | Như V |
| XIV. Ngoại tuyến, thông báo | Đạt | Lệnh của 001 chỉ trực tuyến (ngoài phạm vi 8.6). Mọi thông báo là sự kiện tới 009, có mức, người nhận, khóa sự kiện (H-14) |
| XV. Truy vết, kiểm thử | Đạt | Tên test theo BR, FR; mỗi BR-M01-01 → 13 có test tích hợp (data-model mục 12). Không có BR nào của 001 thực hiện ở client |

**Kiểm lại sau Phase 1**: Đạt. Thiết kế không thêm vai trò, trạng thái hay quy tắc nghiệp vụ ngoài spec. Có ba bổ sung kỹ thuật. Thứ nhất, hai giao diện mới trên hợp đồng của 000: `TepApi`, `YeuCauPheDuyetApi.chuyenMoRong`. Thứ hai, bảng kỹ thuật `lan_bao_danh_gia_lai` lưu lần thông báo, không phải yêu cầu. Thứ ba, lệnh trạng thái không dùng `If-Match`. Cả ba được giải thích ở Complexity Tracking.

### Đáp ứng yêu cầu phi chức năng (constitution X)

| NFR | 001 chịu ảnh hưởng | Cách đáp ứng |
| --- | --- | --- |
| NFR-01 Quy mô | Có | 300 hồ sơ đang ở, vài nghìn hồ sơ cũ; chỉ mục ở data-model |
| NFR-02 Hiệu năng | Có | Lệnh trạng thái khóa một dòng và gọi bean có chỉ mục; lưu ý chăm sóc lấy theo lô cho checklist |
| NFR-03 Thông báo khẩn cấp | Có | Chuyển viện, qua đời phát sự kiện sau commit ngay trong lệnh; 009 gửi Khẩn cấp. Kiểm tra dị ứng không chặn đường ghi |
| NFR-04 Tác vụ định kỳ | Có | Sáu job chống lặp bằng khóa duy nhất hoặc cột "đã cảnh báo"; khóa sự kiện cho 009 (H-12, H-14) |
| NFR-05 Khả dụng | Gián tiếp | Job chạy lại khi khởi động; không phụ thuộc giờ giao ca |
| NFR-06 Sao lưu | Có | Thư mục tệp được sao lưu cùng MySQL (H-13) |
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
├── research.md                  # Phase 0: H-01 → H-15
├── data-model.md                # Phase 1: bảng, bảng chuyển, bảo đảm ở cơ sở dữ liệu, truy vết
├── quickstart.md                # Phase 1: kịch bản kiểm chứng, truy vết kịch bản → test
├── contracts/
│   ├── api-001.yaml             # OpenAPI 3.1: hồ sơ, trạng thái, bản đồng ý, sức khỏe, nguyện vọng, đánh giá, danh mục, tệp
│   └── module-interfaces.md     # Giao diện Java: 001 cung cấp / 001 cần; SPI ThamGiaChuyenTrangThai
└── tasks.md                     # Phase 2 (/speckit-tasks; chưa tạo)
```

### Source Code (repository root)

Cấu trúc dự kiến cho giai đoạn triển khai. Giai đoạn thiết kế không tạo các thư mục này (CLAUDE.md).

```text
backend/src/main/java/vn/eldercare/
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
    └── lich/                          # 6 job (H-12)

web/src/tinh-nang/
├── ho-so-nguoi-cao-tuoi/          # tra cứu, tạo, cập nhật, lịch sử, hồ sơ liên kết, lệnh trạng thái
├── ban-dong-y/
├── suc-khoe/                      # hồ sơ ban đầu, mục sức khỏe
├── nguyen-vong/
├── danh-gia/                      # lập, xác nhận, danh sách cần đánh giá lại
└── danh-muc-m01/

app-nhan-vien/lib/tinh-nang/
├── tra-cuu-nguoi-cao-tuoi/        # thẻ người cao tuổi: lưu ý chăm sóc, dị ứng, dấu kiểm tra
├── suc-khoe/
├── danh-gia/
└── trang-thai/                    # Cho tạm vắng, Ghi nhận trở về, Chuyển viện, Ghi nhận qua đời

app-nguoi-than/lib/tinh-nang/ho-so/
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

Plan không tự sửa các điểm dưới đây (constitution I, IX). Đề nghị chủ dự án quyết bằng mã Q mới khi cần, rồi sửa `docs/nghiep-vu.md` và spec 001.

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
