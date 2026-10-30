# Implementation Plan: Phòng, giường và phân bổ giường

**Branch**: `003-room-bed-allocation` | **Date**: 2026-10-07 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/003-room-bed-allocation/spec.md`

## Summary

Feature 003 là phần phòng và giường của module M03 (mục 7). Nó cung cấp:
1. Cơ cấu Khu vực → Tầng → Phòng → Giường, loại phòng, mức chăm sóc cho phép, chính sách giới tính (mục A; UC-19).
2. Cách ly phòng, cùng việc áp chặn của vùng khoanh vùng do 007 quyết (mục B; UC-37 phần đặt, gỡ).
3. Vòng đời bảy trạng thái giường. Đang bảo trì, Không sử dụng chỉ đến từ lệnh tài sản của 019. Tạm ngừng sử dụng, Chờ sẵn sàng có lệnh riêng (mục C, H; UC-74).
4. Bản ghi phân bổ có thời điểm bắt đầu, kết thúc; kiểm tra BR-M03-01; không bao giờ có hai người trên một giường (mục D; UC-20).
5. Chuyển giường một lệnh, gồm chuyển theo lịch, chuyển gấp khác giá, chuyển theo chỉ định kiểm soát lây nhiễm, nhập bù (mục E; UC-21).
6. Tra cứu lịch sử vị trí cho truy vết lây nhiễm (mục F).
7. Khu nghỉ bán trú duy nhất với sức chứa theo buổi (mục G).

Hướng kỹ thuật:
- Khóa dòng theo thứ tự cố định: người cao tuổi (khi lệnh đi qua 001), rồi `vi_tri_nct`, rồi giường theo `id`. Mọi điều kiện kiểm sau khi khóa, nên lệnh đến sau được kiểm lại trên trạng thái mới. Cơ sở dữ liệu là lưới an toàn: cột sinh có `UNIQUE` cho "một đang hiệu lực mỗi giường, mỗi người", trigger chống chồng khoảng, trigger bất biến khi đã đóng (research G-03).
- Một điểm ghi trạng thái giường với bảng chuyển cài trong mã; mỗi lần chuyển thêm một dòng lịch sử (G-04).
- Một thành phần kiểm tra FR-018 dùng chung cho danh sách giường và lệnh, nên danh sách và lệnh không lệch nhau (G-05).
- Tác động lên giường từ lệnh trạng thái của 001 đi qua SPI `ThamGiaChuyenTrangThai`, trong cùng transaction (G-06).
- Chuyển theo lịch gắn vào vòng đời yêu cầu thay đổi lưu trú: giữ giường lúc duyệt, chuyển ở lần áp dụng của 000 (G-07).
- Module thấp hơn khai báo SPI để không có vòng phụ thuộc: 003 khai báo `NguonLuuTruChoPhanBo` (004), `TinhTrangLayNhiemChoPhanBo` (007), `ThamChieuViTri` (008), `DemLichDenBanTru` (004) (G-02).

Plan tuân theo FR-010a của 000 (Q-306, Q-307): 003 không tự lập yêu cầu, không gợi ý hay chọn sẵn giường, không đề xuất hồ sơ chờ. Bốn job chỉ chuyển trạng thái theo hạn (giữ chỗ hồ sơ chờ), nhắc, báo. Mọi phân bổ do actor lập bằng lệnh, hoặc là tác động kèm lệnh đó.

## Technical Context

**Language/Version**: Java 25; JavaScript (JSX); Dart 3.x với Flutter stable 3.x (000 R-01).

**Primary Dependencies**: Như 000, 001, 002: Spring Boot 4.1.x, Spring Modulith 2.x, Flyway (bản do Spring Boot quản lý), springdoc-openapi 3.x, ShedLock, Spring Security 7. Web: React 19, TanStack Query. App: Flutter. Không thêm thư viện mới. Thử lại khi deadlock dùng `@Retryable` của Spring Retry, đã có trong Spring Boot.

**Storage**:
- MySQL 8.0. Bảng ở [data-model.md](data-model.md).
- Cột sinh có `UNIQUE` (`giuong_dang_hieu_luc`, `nct_dang_hieu_luc`, `nct_tuong_lai`, `hieu_luc_duy_nhat`); trigger chống chồng, bất biến, chỉ thêm; view `v_phan_bo_hien_hanh` áp đính chính.

**Testing**:
- Như 000 R-14 và 001 H-15.
- Test đồng thời: 10 luồng trên một giường (SC-001), hai lệnh chuyển cùng người.
- Test tích hợp MySQL cho trigger, cột sinh, `CHECK`.
- Đồng hồ giả lập cho CFG-M02-02, CFG-M03-02, 04, 07, 08.
- Bản giả cho SPI của 004, 007, 008, 019 (research G-16).

**Target Platform**: Như 000.
- Web: mọi màn của 003: cơ cấu, bảng giường, phân bổ, chuyển giường, tra cứu lịch sử, cách ly, khu nghỉ bán trú, danh mục lý do chuyển.
- App nhân viên:
  - bảng giường của tầng;
  - Xác nhận giường sẵn sàng (Trưởng tầng, Người phụ trách ca);
  - chuyển giường (Trưởng tầng);
  - đặt, gỡ cách ly và xác nhận ở lại vùng cách ly (Bác sĩ).
- App người thân: không có màn của 003. Phòng, giường của người cao tuổi hiện trên hồ sơ (001, 012).
- Lệnh của 003 chỉ chạy trực tuyến. Mục 8.6 chỉ cho ghi tạm với "ghi nhận thực hiện", không có lệnh nào của 003.

**Project Type**: Web service (REST `/api/v1`), một web client và một mobile client chung (Q-346).

**Performance Goals**:
- Lệnh phân bổ, chuyển giường nằm trong 2 giây của NFR-02. Mỗi lệnh khóa 1 dòng người và 1–2 dòng giường, gọi vài API có chỉ mục.
- Danh sách giường phù hợp cho 300 giường là một truy vấn tập hợp, cộng ba lời gọi SPI trả tập nhỏ (vùng khoanh, dấu tài sản, loại lưu trú). Mục tiêu dưới 1 giây, đủ cho SC-005.
- Tra cứu cùng phòng trên vài nghìn bản ghi mỗi năm, có chỉ mục `(giuong_id, bat_dau)`: dưới 1 giây (SC-003).

**Constraints**:
- Không có thao tác đặt trạng thái giường, không sửa hay xóa phân bổ; mỗi lệnh một endpoint (constitution XI).
- Nhật ký trong cùng transaction (000 FR-047).
- Bản ghi phân bổ đã đóng, đã hủy và lịch sử nhóm 3 không bị `UPDATE`, `DELETE` (constitution XII).
- CFG đọc lúc chạy (constitution XIII).
- Không tự sinh bản ghi nghiệp vụ (000 FR-010a).

**Scale/Scope**:
- 300 giường, 300 người cao tuổi (NFR-01).
- 7 trạng thái giường với 17 dòng chuyển; 4 trạng thái phân bổ.
- 4 job.
- 49 đường dẫn REST (contracts/api-003.yaml).
- 4 SPI 003 khai báo, 7 SPI 003 cài.

Không còn mục NEEDS CLARIFICATION. Các lựa chọn kỹ thuật đã chốt ở [research.md](research.md). Các điểm về tài liệu nguồn ghi ở "Điểm cần báo lại"; chúng không chặn thiết kế.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Bằng chứng |
| --- | --- | --- |
| I. Nguồn gốc, truy vết | Đạt | Mỗi quyết định dẫn FR, BR, DBR. Data-model mục 11 là bảng truy vết DBR-09, 10, 23, 25, 26, 32 và BR-M03-01 → 10, 13, 15, 19 → 25 |
| II. Spec không nêu công nghệ | Đạt | Spec không đổi; công nghệ chỉ ở plan, research |
| III. Không mặc định CRUD | Đạt | Danh mục theo mẫu nhóm 1. Giường không có Ngừng hiệu lực, không xóa (Q-237). Trạng thái giường và phân bổ chỉ đổi qua lệnh. Lịch sử giường, lịch sử cách ly chỉ thêm; phân bổ đã đóng chỉ đính chính |
| IV. Trạng thái kiểm tra được | Đạt | Bảng chuyển giường, phân bổ, cách ly ở data-model mục 3 → 5 khớp bảng của spec. Mỗi kịch bản có test (quickstart mục 4) |
| V. Không cố định giá trị | Đạt | CFG-M02-02, CFG-M03-01, 02, 04, 07, 08 đọc từ `ThamSoApi`. Chu kỳ job 15 phút là thông số kỹ thuật (Complexity Tracking) |
| VI. Vai trò, quyền | Đạt | Khóa quyền theo dòng Phụ lục 27 (G-14). Các thu hẹp đều có nguồn: chú thích ³⁵, ¹⁵, FR-021, FR-029, FR-044. Không thêm vai trò. Người phụ trách ca cần ngoại lệ ở 002, báo lại ở điểm 1 |
| VII. Điểm chưa rõ | Đạt | Không tự đoán nghiệp vụ. Ba chỗ spec mâu thuẫn hoặc thiếu được báo lại (điểm 2, 3, 4); thiết kế theo FR, không theo ví dụ |
| VIII. Tiêu chí thành công | Đạt | SC-001 → SC-014 có cách đo ở quickstart mục 5 |
| IX. Phạm vi giai đoạn | Đạt | Chỉ tài liệu thiết kế; không mã, không `src/`, không cấu hình build, không SQL; không đổi nghiệp vụ. Hợp đồng của 000, 001, 002 được bổ sung giao diện (G-13), không đổi hành vi đã thiết kế |
| X. Kiến trúc | Đạt | Một khối; gói `m03`; module khác chỉ dùng `m03.api`. Không có vòng phụ thuộc: module thấp khai báo SPI (G-02). REST `/api/v1`; MySQL; hai client (Q-346). NFR ở bảng bên dưới |
| XI. Nghiệp vụ ở backend | Đạt | FR-018, bảng chuyển, quyền, phạm vi đều ở backend. `lenhDuocPhep` chỉ để hiển thị. Mỗi lệnh có `POST …/{id}/<lenh>` riêng, kể cả hai kiểu chuyển giường |
| XII. Dữ liệu | Đạt | DBR-09 có `UNIQUE` trên cột sinh, trigger và khóa; DBR-26 có bảng chuyển và `CHECK`; BR-M03-10 có `CHECK`; nhóm 3 theo mẫu trigger của 000; nhật ký trong transaction; thời gian theo `Clock` (data-model mục 8) |
| XIII. Tham số | Đạt | Như V. Sức chứa khu nghỉ không lưu riêng, luôn là CFG-M03-01 |
| XIV. Ngoại tuyến, thông báo | Đạt | Lệnh của 003 chỉ trực tuyến. Mọi thông báo là sự kiện tới 009, có mức, người nhận, khóa sự kiện (G-12) |
| XV. Truy vết, kiểm thử | Đạt | Tên test theo BR, FR. Mỗi BR của 003 có test tích hợp (data-model mục 11). Không có BR nào của 003 thực hiện ở client |

**Kiểm lại sau Phase 1**: Đạt. Thiết kế không thêm vai trò, trạng thái hay quy tắc nghiệp vụ ngoài spec. Có bốn bổ sung kỹ thuật, giải thích ở Complexity Tracking:
1. Bảng `vi_tri_nct` làm đích khóa theo người.
2. Bảng kỹ thuật `yeu_cau_nhap_bu`, `nhac_phan_bo`.
3. Lệnh phân bổ, chuyển giường không dùng `If-Match`.
4. Năm giao diện bổ sung vào hợp đồng 000, 001, 002 (G-13).

### Đáp ứng yêu cầu phi chức năng (constitution X)

| NFR | 003 chịu ảnh hưởng | Cách đáp ứng |
| --- | --- | --- |
| NFR-01 Quy mô | Có | 300 giường; vài nghìn phân bổ mỗi năm; chỉ mục ở data-model mục 4 |
| NFR-02 Hiệu năng | Có | Lệnh khóa tối đa ba dòng; danh sách giường một truy vấn tập hợp; tra cứu có chỉ mục |
| NFR-03 Thông báo khẩn cấp | Có | `TroVePhongCachLy` phát sau commit ngay trong lệnh Ghi nhận trở về; 009 gửi Khẩn cấp |
| NFR-04 Tác vụ định kỳ | Có | Bốn job chống lặp bằng trạng thái hoặc cột "đã báo, đã nhắc"; khóa sự kiện cho 009 (G-09, G-12) |
| NFR-05 Khả dụng | Gián tiếp | Job chạy lại khi khởi động; trễ được ghi và báo theo CFG-M03-08 (FR-016a) |
| NFR-06 Sao lưu | Gián tiếp | Dữ liệu chỉ ở MySQL |
| NFR-07 Lưu trữ | Có | `LuuTruProvider` theo hồ sơ cho phân bổ; lịch sử giường, cách ly theo CFG-M15-04 (data-model mục 10) |
| NFR-08 Bảo mật | Có | Danh sách giường và tra cứu không trả trường sức khỏe (FR-022, FR-035); lý do cách ly, lý do chuyển kiểm soát lây nhiễm thuộc nhóm `SUC_KHOE` khi xem nhật ký |
| NFR-09 Thời gian | Có | Mọi mốc từ `Clock`; khoảng nửa mở `[bắt đầu, kết thúc)`; thời điểm thực tế kèm dự kiến với việc của Bộ lập lịch |
| NFR-10 Thiết bị | Có | Web đủ chức năng; app nhân viên có bảng giường tầng, xác nhận sẵn sàng, chuyển giường, cách ly |
| NFR-11 Ngôn ngữ | Có | Thông báo từ chối tiếng Việt, liệt kê điều kiện kèm mã BR; nhãn vị trí "Khu – Tầng – Phòng – Giường" |
| NFR-12 Cấu hình | Có | CFG đọc lúc chạy; danh mục lý do chuyển, buổi bán trú cấu hình được |
| NFR-13 Kiểm thử | Có | Đồng hồ giả lập; bản giả bật tắt được ở profile `test`, `demo` |

## Project Structure

### Documentation (this feature)

```text
specs/003-room-bed-allocation/
├── plan.md                      # File này
├── research.md                  # Phase 0: G-01 → G-16
├── data-model.md                # Phase 1: bảng, bảng chuyển, bảo đảm ở cơ sở dữ liệu, truy vết
├── quickstart.md                # Phase 1: kịch bản kiểm chứng, truy vết kịch bản → test
├── contracts/
│   ├── api-003.yaml             # OpenAPI 3.1: cơ cấu, giường, cách ly, phân bổ, tra cứu, bán trú
│   └── module-interfaces.md     # Giao diện Java: 003 cung cấp / 003 cần; port nội bộ M03 với 019
└── tasks.md                     # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

Cấu trúc dự kiến cho giai đoạn triển khai. Giai đoạn thiết kế không tạo các thư mục này (CLAUDE.md).

```text
backend/src/main/java/com/cambae/backend/m03/
├── api/                       # PhanBoGiuongLenhApi, ViTriApi, LichSuViTriApi, SucChuaBanTruApi, CoCauViTriApi,
│                              # PhongChoPhepApi, TinhTrangGiuongApi, CanhBaoBanGiaoApi, SPI 003 khai báo, sự kiện
├── cocau/      {internal, web}     # khu vực, tầng, loại phòng, phòng, giường (danh mục), lý do chuyển
├── giuong/     {internal, web, port}  # GiuongTrangThaiService, bảng chuyển, lịch sử, xác nhận sẵn sàng, tạm ngừng
├── phanbo/     {internal, web}     # KiemTraPhanBo, phân bổ, chuyển giường, nhập bù, dấu, tra cứu, SPI 001, handler 000
├── cachly/     {internal, web}
├── bantru/     {internal, web}
├── lich/                           # 4 job (G-09)
└── taisan/                         # feature 019 (plan sau)

frontend/src/tinh-nang/
├── co-cau-phong-giuong/       # khu vực, tầng, loại phòng, phòng, giường, lý do chuyển
├── bang-giuong/               # trạng thái, dấu, tạm ngừng, xác nhận sẵn sàng
├── phan-bo-giuong/            # chọn giường, phân bổ, chuyển giường, nhập bù, hủy phân bổ tương lai
├── lich-su-vi-tri/
├── cach-ly-phong/
└── khu-nghi-ban-tru/

mobile/lib/nhan-vien/
├── bang-giuong-tang/          # Xác nhận giường sẵn sàng, dấu
├── chuyen-giuong/
└── cach-ly-phong/             # Bác sĩ: đặt, gỡ, xác nhận ở lại
```

**Structure Decision**: Theo R-02 của 000. Feature 003 nằm ở `backend/…/m03` với năm gói con nghiệp vụ, gói `lich` và gói `api` công khai. Phần tài sản (019) sẽ ở `m03.taisan` và chỉ gọi 003 qua port `m03.giuong.port` (G-01). Web có đủ chức năng của 003. App nhân viên có các thao tác tại tầng. App người thân không có màn của 003.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Lệnh phân bổ, chuyển giường, đổi trạng thái giường không dùng `If-Match` (khác R-03 "lệnh sửa bắt buộc `If-Match`") | FR-020, FR-033 đòi lệnh đến sau được kiểm tra lại trên trạng thái mới, chỉ bị từ chối khi không còn hợp lệ | `If-Match` từ chối cả lệnh đến sau còn hợp lệ. Cùng lý do với 001 H-02 |
| Bảng `vi_tri_nct` (một dòng mỗi người) | Cần một dòng của 003 để khóa tuần tự mọi lệnh trên cùng người, và để giữ dấu "chưa có giường" (FR-025a) | Khóa dòng `nguoi_cao_tuoi` của M01 từ lệnh của 003 là truy cập dữ liệu module khác (constitution X) |
| Trigger chống chồng khoảng song song với khóa ở backend | DBR-09 cần một bảo đảm ở cơ sở dữ liệu khi mã có lỗi (constitution XII); MySQL không có ràng buộc loại trừ khoảng | Chỉ kiểm ở backend thì một lỗi mã là phá được SC-001 |
| Bảng kỹ thuật `yeu_cau_nhap_bu`, `nhac_phan_bo`; cột `da_bao_*`, `da_nhac_*` trên `giuong`, `phan_bo_giuong` | Chống báo, nhắc lặp (NFR-04); gắn dấu "có nhập bù đang chờ duyệt" mà không đọc JSON của 000. Mỗi lần báo, nhắc vẫn ghi nhật ký | Suy từ bảng thông báo của 009 thì 003 đọc dữ liệu module khác |
| Năm giao diện bổ sung vào hợp đồng 000, 001, 002 (G-13) | FR-029 cần tác động lúc duyệt; FR-024a cần cảnh báo trước lệnh; FR-027, FR-036 cần khoảng trạng thái; FR-034 cần sự kiện đổi giới tính; Modulith cần một chiều phụ thuộc M03 → M15 | Không bổ sung thì phải sao chép dữ liệu của module khác hoặc dùng sự kiện sau commit, trái các yêu cầu "trong cùng một lần" |
| Chu kỳ job 15 phút đặt trong cấu hình ứng dụng, không phải mã CFG | Thông số vận hành, không có trong Phụ lục 25. Trễ tối đa 15 phút, dưới ngưỡng mặc định của CFG-M03-08 | Thêm mã CFG thì phải sửa tài liệu nguồn chỉ vì thông số kỹ thuật |

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX). Đề nghị chủ dự án quyết bằng mã Q mới khi cần, rồi sửa `docs/business/nghiep-vu.md` và spec liên quan.

1. **Người phụ trách ca Xác nhận giường sẵn sàng chưa có ngoại lệ quyền ở 002.** *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - Spec 003 FR-044, UC-74 và chú thích ¹⁵ Phụ lục 27 cho Người phụ trách ca của tầng thực hiện lệnh này.
   - Spec 002 FR-019 ghi nhiệm vụ không tạo quyền thao tác, trừ đúng sáu ngoại lệ FR-019a → FR-019f. Không ngoại lệ nào nói về Xác nhận giường sẵn sàng.
   - Người phụ trách ca thường có vai trò Điều dưỡng hoặc Nhân viên chăm sóc. Hai vai trò này có "—" ở dòng "Xử lý hư hỏng, đưa giường vào bảo trì", nên `QuyenApi` sẽ từ chối họ.
   - Đề nghị: thêm FR-019g vào spec 002 (Người phụ trách ca được Xác nhận giường sẵn sàng cho giường thuộc tầng/khu vực của ca, trong khoảng phạm vi của ca), kèm Q mới. Plan 002 chỉ cần thêm một dòng vào `QuyenTheoNhiemVu`; 003 không cần sửa.
   - Cùng dòng Phụ lục 27 có QL "T". Chú thích ¹⁵ cho thấy chữ T đó chỉ áp cho phần xử lý hư hỏng, nên plan loại Quản lý viện khỏi Xác nhận giường sẵn sàng theo FR-044 và US4 #11. Đề nghị tách dòng Phụ lục 27 thành hai dòng ("Xác nhận giường sẵn sàng" và "Xử lý hư hỏng giường") cho rõ.

2. **Chuyển giường theo lịch khi người cao tuổi đang Điều trị tại bệnh viện: spec tự mâu thuẫn.** *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - Edge Case "Chuyển giường theo lịch không thực hiện được" lấy "người cao tuổi đang Điều trị tại bệnh viện" làm ví dụ của "Áp dụng không thành".
   - Edge Case "Người cao tuổi đang Tạm vắng hoặc Điều trị tại bệnh viện có giường giữ chỗ" lại ghi chuyển giường vẫn thực hiện được (FR-032).
   - FR-029 chỉ cho "Áp dụng không thành" khi FR-018 không thỏa, mà FR-018 không có điều kiện về việc đang nằm viện.
   - Plan theo FR-029 và FR-032: chuyển theo lịch vẫn thực hiện, giường mới nhận giữ chỗ với hạn còn lại. Đề nghị sửa ví dụ ở Edge Case, hoặc nếu muốn chặn thì thêm điều kiện vào FR-029 bằng Q mới.

3. **Chuyển theo lịch bị hoãn vì giường chưa sẵn sàng, rồi không còn thỏa FR-018 lúc giường về Trống.** *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - Theo FR-011a, yêu cầu được áp dụng vào ngày hiệu lực (đơn giá mới tính từ ngày đó theo FR-016a), còn việc chuyển chờ tới khi giường về Trống.
   - Nếu lúc đó phòng mới vừa bị cách ly hoặc người cao tuổi đổi mức, FR-018 không còn thỏa. Yêu cầu đã "Đã áp dụng" nên không chuyển "Áp dụng không thành" được.
   - Plan hủy phân bổ tương lai, người cao tuổi ở lại giường cũ, báo Hành chính, Trưởng tầng và Quản lý viện. Đơn giá đã áp dụng thì Hành chính xử lý bằng một yêu cầu thay đổi lưu trú mới.
   - Đề nghị chốt bằng Q: giữ cách này, hay hoãn cả việc áp dụng yêu cầu tới lúc chuyển thực hiện.

4. **BR-M03-06 ghi "hết hạn giữ chỗ cho người đang vắng … giường chuyển Chờ sẵn sàng"**, trong khi BR-M02-07 và spec 003 FR-015 ghi hết hạn thì không tự giải phóng mà chuyển cho Quản lý viện chọn. Plan theo BR-M02-07 và FR-015. Đề nghị sửa câu của BR-M03-06 thành "khi giường giữ cho người vắng được giải phóng". *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

5. **Hai feature cùng nhận job hết hạn giữ chỗ cho hồ sơ chờ.** 003 FR-015 và 004 FR-015 đều ghi Bộ lập lịch của mình xử lý. Plan đặt một job ở 003, vì hạn giữ là dữ liệu của giường. Job gọi SPI của 004 để trả hồ sơ về Đang chờ trong cùng transaction (research G-09). Plan 004 không thêm job thứ hai. Không đổi nghiệp vụ; đề nghị ghi rõ ở spec 004 FR-015 rằng việc trả giường do 003 làm. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

6. **Spec 003 chưa nêu mức của phần lớn thông báo (009 FR-043a, Q-108).** Chỉ "trở về phòng đang cách ly" có mức Khẩn cấp (7.4, Q-114). Plan áp nguyên tắc 3 bậc như 001 (research G-12): nhắc theo chu kỳ và báo bác sĩ chỉ định ở mức Nhẹ; báo giường hỏng có phân bổ tương lai, chuyển theo lịch chờ giường, việc lịch trễ, chuyển gấp khác giá, phòng không còn phù hợp, quá thời điểm bắt đầu dự kiến, khu nghỉ vượt sức chứa ở mức Trung bình. Đề nghị ghi vào spec 003 và bảng 009 FR-043b khi đồng bộ. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

7. **(Đồng bộ câu chữ spec 003, không đổi nghiệp vụ)** *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - US3 kịch bản 6 còn ghi "kích hoạt đề xuất hồ sơ chờ BR-M02-01", trái Q-307 và FR-014 ("báo Hành chính, không đề xuất").
   - Key Entities còn "Khu vực chung". Sau Q-282 không FR nào dùng thực thể này, nên plan không mô hình hóa nó.
   - Assumptions còn câu dẫn `docs/phan-tich-yeu-cau.md` mục 4.2 (đã bỏ); có thể giữ làm lịch sử.
   - US4 kịch bản 3 còn cụm "hư hỏng phát hiện khi dọn giường" theo nghĩa cũ; vẫn đúng với FR-013a.

8. **(Đồng bộ với plan 000, 001, 002, đã sửa trong plan này)** Các bổ sung ở research G-13 đã được ghi vào: *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - [../000-shared-foundation/contracts/module-interfaces.md](../000-shared-foundation/contracts/module-interfaces.md): `LoaiYeuCauHandler.kiemTraKhiDuyet`, `khiDuyet`;
   - [../001-elder-profile-assessment/contracts/module-interfaces.md](../001-elder-profile-assessment/contracts/module-interfaces.md): `ThamGiaChuyenTrangThai.canhBao`, `TrangThaiNguoiCaoTuoiApi.khoangTrangThai`, sự kiện `ThongTinCaNhanDaCapNhat`;
   - [../002-account-permission/contracts/module-interfaces.md](../002-account-permission/contracts/module-interfaces.md): `PhanBoGiuongApi` là SPI khai báo ở `m15.taikhoan.api`, M03 cài.

## Ghi chú triển khai

### 2026-10-22 — Phase 1, T001–T002

- Phạm vi: khung M03, năm bản giả và khai báo tối thiểu để các bản giả biên dịch. Không làm migration, controller, nghiệp vụ phân bổ, web hay mobile; không sửa hợp đồng REST, không thêm thư viện.
- Đặt gói theo backend/README.md và R-02 hiện hành: `api/{dto,spi}`, `internal/{structure,bed,allocation,isolation,daycare}`, `internal/bed/domain`, `scheduling`. T001 chỉ tạo `package-info.java` làm khung tài liệu theo yêu cầu task; không tạo entity/service rỗng hay cả bốn tầng khi chưa có lớp. Tên tiếng Việt trong sơ đồ cũ là tên logic, không phải đường dẫn mã.
- Port `TaiSanGiuongPort` được đặt tên `BedAssetPort` tại `internal/bed/domain`; đây là cổng nội bộ cùng module, không mở tài sản thành module mới. Cổng chiều ngược lại `GiuongTheoTaiSanPort` và luật ranh giới cụ thể cho phần tài sản còn thuộc T004/019.
- T002 cần kiểu để `implements`, nên kéo lên đúng bốn khai báo SPI từ T004 và các kiểu chữ ký liên quan; T004 vẫn chưa đánh xong. SPI nằm ở `api/spi`, kiểu dùng chung cho cơ cấu/bán trú ở `api/dto`; mỗi gói API có `@NamedInterface`. Không đổi số hàm hay ý nghĩa chữ ký trong contracts mục 2.1, mục 3.
- `CareLevelCode` và `ResidencyType` là mã trao đổi của SPI M03; bộ chuyển đổi ở 004 sẽ ánh xạ dữ liệu của 001/004 sang các mã đó, không khai báo hộ API M01. Phase 1 không cần bean M01; prerequisite M01 vẫn cần được xác nhận trước các task nghiệp vụ dùng hồ sơ/chuyển trạng thái.
- Bản giả đặt tại `support/fake/m03/`, chỉ dùng `@TestConfiguration` + `@Import(FakeM03Config.class)`, các bean `@Primary`. Không có bean giả trong ứng dụng chạy thật, không có endpoint giả lập mới. Công tắc trong test là các hàm đặt dữ liệu và `reset()`; endpoint kiểm thử/demo mô tả trong G-16 chưa thuộc phần này.
- Bản giả mặc định không có dữ liệu, không có vùng khoanh/dấu tài sản. Nguồn lưu trú giữ riêng mức theo hợp đồng và đăng ký; bộ đếm bán trú lọc ngày/buổi; tình trạng lây nhiễm trả trạng thái theo mốc truyền vào; tham chiếu vị trí giữ lịch sử sau khi gỡ tham chiếu hiện tại; cổng tài sản không ghi dữ liệu. Lệnh gọi lại nguồn lưu trú được ghi trong bộ nhớ để test kiểm.
- TDD: T001 có 2 assertion thất bại trước khi tạo khung; sau cài, test task và các ArchTest/ModuleBoundaryTest qua 23/23. T002 ban đầu chưa biên dịch vì thiếu kiểu, sau khi dựng chữ ký và khung bản giả có 5/7 test hành vi thất bại; sau cài bộ đặt dữ liệu, test task và các ArchTest/ModuleBoundaryTest qua 28/28. Java đã chạy `spotless:apply`.
- Máy có JDK 25 ở `E:\JAVA\JDK_GE\INS_25`; dùng `JAVA_HOME` riêng cho từng lệnh, không đổi cấu hình máy. Maven wrapper đã được phép ghi cache sau lần thử đầu bị sandbox chặn. Chưa có `TEST_DB_URL`, `TEST_DB_ADMIN_USER`, `TEST_DB_ADMIN_PASSWORD` trong môi trường process/user/machine; kết quả kiểm cuối phase sẽ được ghi tiếp sau khi lệnh nền kết thúc.
- Các dòng của C ở team-plan mục 8 chưa xong bởi Phase 1: chưa cài `BedAllocationApi`, chưa tạo dữ liệu demo, chưa làm 005/014. Checkout vẫn là `develop`; không đổi nhánh, commit, push hay tạo PR.
- Kết quả cuối Phase 1: lần `clean verify` đầu dừng ở test nạp context vì thiếu `TEST_DB_URL` (154 test, 0 failure, 1 error); sau khi người dùng cung cấp cấu hình, nạp ba biến `TEST_DB_*` vào riêng tiến trình chạy và chạy lại `clean verify` nền: **BUILD SUCCESS**, 155 test đơn vị/kiến trúc và 411 test tích hợp, 0 failure/error/skip, Spotless check qua (01:52). Chỉ sau kết quả này mới đánh `[x]` T001/T002. Không lưu thông tin đăng nhập vào kho mã hay cấu hình máy.
- Review độc lập phát hiện hai hàm đọc của `BedAssetPort` còn thiếu `MANDATORY` theo contracts mục 3; test `FR_013_BedAssetPortTest` đã đỏ trước khi thêm khai báo và xanh sau sửa. Lệnh kiểm liên quan sau sửa qua 29/29. Không còn phát hiện cần xử lý từ lượt review này.
- Đã thêm ba dòng chờ B (004), D (007), E (008) cài các SPI M03 ở team-plan mục 8; bản giả có trong thay đổi này nhưng chưa merge. Công việc tiếp theo là T003, sau đó hoàn thiện T004; không mở Phase 2 trong lượt này.

### 2026-10-23 — Phase 2, T003–T010

- Nhánh đầu phiên: `feature/003-manage-bedroom`, HEAD `b61fc6b` (Phase 1 đã có trong Git); chỉ `Working-history.md` chưa được theo dõi. Không đổi nhánh, commit, push, merge hay sửa file của thành viên khác.
- T003: khai báo 9 interface/43 thao tác theo contracts mục 1.2–1.9; dùng lại `ScopeFilter`, `BedAllocationView`, `Reason`, `Basis`, `UnmetCondition` đã có. Không đổi gói/tên SPI Phase 1. `ResidentGender` (MALE/FEMALE) khác `GenderPolicy` (có UNRESTRICTED); các DTO chữ ký ở `api/dto`. Đây là khai báo API nội bộ lần đầu, không đổi endpoint/trường REST.
- T004: hoàn thiện cổng chiều 019 gọi 003 là `internal/bed/domain/BedAssetCommandPort` (5 hàm MANDATORY), giữ `BedAssetPort` chiều ngược lại và 4 SPI đã có. `FakeM03ApiConfig` cung cấp fake tập trung của 9 API và cổng lệnh; test phải đặt kịch bản bằng `doReturn/doAnswer`, lời gọi chưa cấu hình báo lỗi. Không tạo bản giả thứ hai của SPI do 001/002 khai báo.
- T005: 20 record sự kiện trong `api/events`; 12 loại gửi 009 cài `HasNotificationRequests`, 8 loại còn lại không tự gửi. Metadata `BedNotificationContext` mang mốc, tầng và nhóm người nhận do lệnh nguồn xác định theo G-12; mức thông báo nằm trong từng loại sự kiện. Khóa sự kiện gồm loại/id/chu kỳ/mốc, ổn định khi phát lại; không gọi FCM/SMS. Các user story sau phải chọn đúng nhóm G-12 và truyền vào context.
- T006/T007: `V003_01__room_structure.sql` tạo 7 bảng cơ cấu; `V003_02__bed_state.sql` tạo giường, lịch sử và trigger chỉ thêm, phân quyền app/retention. Mã bảng/cột/enum bằng tiếng Anh, ánh xạ ở glossary mục 9. CHECK khử khuẩn chặn cả NULL/thiếu bảo hộ, tránh ba giá trị logic của SQL làm lọt BR-M03-10. `last_closed_allocation_id` hiện là cột id; khóa ngoại thêm ở V003_03/T023 khi bảng phân bổ được tạo, không tham chiếu bảng chưa tồn tại.
- T008/T009: `BedStateService` là cổng ghi trạng thái duy nhất; SQL đặt tại `BedStateStore` theo tầng persistence. `SingleBedStateWriterArchTest` bắt buộc cổng tồn tại và chỉ dịch vụ này gọi `writeState`. Đây là cách đặt lớp theo R-02, thay tên/gói logic cũ; T009 được viết đỏ trước phần cài T008. Dịch vụ MANDATORY, khóa FOR UPDATE, dùng bảng chuyển thuần `BedTransitions`, ghi lịch sử và nhật ký cùng transaction; chưa có controller hay lệnh công khai bỏ qua kiểm quyền. Điều kiện phân bổ/tài sản của từng user story vẫn do lệnh nguồn kiểm trong cùng transaction.
- Thời gian thực từ `BusinessTime`, chuẩn hóa microsecond theo DATETIME(6); hạn sẵn sàng và ngưỡng trễ đọc `ParameterApi` CFG-M03-04/08. Lưu cả planned/actual, actor lịch sử NULL với tác vụ theo lịch; trễ phát `DelayedScheduledWork`. Bảo trì khôi phục trạng thái tạm ngừng và lý do từ lịch sử, không giữ thuộc tính tạm ngừng trái CHECK khi đang bảo trì.
- T010: provider phân loại trường, 3 provider phạm vi, 3 provider lưu giữ đăng ký qua `M03GovernanceConfiguration`. Phạm vi thiếu dữ liệu ném `InvalidPermissionDataException`, không mở thành toàn viện. Để giữ đúng lý do thường công khai nội bộ nhưng che lý do lây nhiễm, audit dùng khóa `infectionControlReason`/`isolationReason` trong before/after; cơ chế che hiện có của 000 đồng thời che cột reason khi có trường bị ẩn. Lệnh chuyển kiểm soát lây nhiễm sau này phải đặt `healthReason=true` khi gọi dịch vụ trạng thái và dùng khóa phân loại tương ứng trong nhật ký nghiệp vụ.
- Lưu giữ chỉ đọc bảng M03, xóa qua `RetentionDatabase`; dùng `ResidencyEndApi.finalStatusAt` đã được 002 khai báo để đọc mốc trạng thái cuối, không khai báo hộ M01/M02. Thiếu adapter/mốc cuối thì giữ hồ sơ; bảng phân bổ/cách ly chưa có được nhận diện trước khi truy vấn. Lịch sử căn cứ phân bổ chỉ là ứng viên khi hồ sơ liên quan hết hạn; chuỗi tham chiếu gồm phân bổ kế tiếp, giường đang giữ căn cứ và lịch sử; phần đính chính do cơ chế attached retention của 000 xử lý. Các SQL cho bảng tương lai dùng tên tiếng Anh đã ghi ở glossary, cần đối chiếu lại khi làm T023/T034/T054; chưa tuyên bố đã tích hợp bản thật 001/004/007/008/019.
- Kiểm từng task: T003 đỏ 2 assertion rồi xanh 23; T004 đỏ 2 rồi xanh 23; T005 đỏ 2 rồi xanh 23. T006: 21 kiểm tra kiến trúc + 1 IT đỏ/xanh; T007: 21 kiểm tra kiến trúc + 3 IT đỏ/xanh. T008: test ban đầu chưa biên dịch do thiếu kiểu, sau khung biên dịch có 5 IT thất bại, rồi 23 kiểm tra kiến trúc + 5 IT qua. T009 đỏ trước dịch vụ, xanh trong kiểm T008. T010: 23 kiểm tra kiến trúc + 3 IT đỏ/xanh. Rà kiểu giới tính bổ sung 1 assertion đỏ trong 3 test T003; kết quả kiểm lại và toàn phase ghi sau khi chạy xong. Đã chạy `spotless:apply`; không tắt/bỏ test.
- Chưa đánh [x] T003–T010 trước `clean verify` cuối phase. `Working-history.md` được dịch toàn bộ ghi chú Phase 1 sang tiếng Việt, giữ nguyên sự kiện/mốc/kết quả cũ và bổ sung Phase 2. Không sửa spec/nghiệp vụ, không thêm thư viện, migration ngoài V003, giao diện hay ảnh chụp.
- Rà chữ ký T003: `ResidentGender` tách khỏi chính sách phòng sau test đỏ; kiểm lại task + kiến trúc qua 26/26. Lần `clean verify` đầu qua 164 unit/kiến trúc + 423 IT (01:50), nhưng chưa đánh task vì review độc lập còn phát hiện hai lỗi.
- Sửa review theo TDD: (1) test khóa `DayCareOverCapacity` đo 917 > 191 ký tự rồi đỏ; dùng SHA-256 của chu kỳ, chuẩn hóa tập buổi bằng distinct/sort theo ngày/id để giữ khóa ổn định và ngắn, không đổi schema 009; (2) test retention chọn hai history còn cần rồi đỏ; truy vấn giữ ASSET_MAINTENANCE mới nhất khi giường UNDER_MAINTENANCE và SUSPEND mới nhất khi SUSPENDED/UNDER_MAINTENANCE, vì dịch vụ còn dùng chúng để khôi phục. Kiểm sau sửa: 26 test thông báo/kiến trúc và 9 IT trạng thái/provider qua; review lại không còn phát hiện trong hai phần này. Chạy lại `clean verify` nền sau sửa, kết quả chốt ghi khi tiến trình hoàn tất.
- Kết quả chốt sau review: `mvnw.cmd clean verify` chạy nền **BUILD SUCCESS**, exit 0, Spotless check qua, **165 test đơn vị/kiến trúc + 424 test tích hợp**, 0 failure/error/skip, tổng **01:40**. Sau kết quả này mới đánh `[x]` T003–T010; T011 là task chưa làm tiếp theo. Các dòng của C ở team-plan mục 8 vẫn chờ/ghi nhớ vì chưa có bản thật `BedAllocationApi`, demo hay phần 005/014. Không commit/push/PR trong lượt này.

### 2026-10-23 — Phase 3, T011–T018 (đang triển khai)

- Baseline `7d64e43`, nhánh `feature/003-manage-bedroom`; Phase 2 đã có trong Git, chỉ Working-history.md chưa theo dõi. Không đổi nhánh/commit/push. Tiếp tục trên checkout hiện tại theo yêu cầu người dùng.
- Đã báo các dòng C ở team-plan mục 8. Bổ sung C/003 vào dòng chờ B cài ResidencyEndApi bị bỏ sót ở Phase 2; thêm phụ thuộc API/fake M01 cho FR-006.
- Tiền kiểm: T017/T018 cần hợp đồng HTTP đã dịch, hiện api-003.yaml còn tiếng Việt; đã hỏi cho phép dịch phần Phase 3/toàn bộ, chưa sửa hợp đồng khi chưa có trả lời. FR-006 cần giới tính/mức hiện hành từ M01, hiện chưa có API/fake; đã hỏi quyền bổ sung ngoài phần C hoặc ghi chờ B. FR-007/Q-243 cần đồng bộ vị trí tài sản khi đổi phòng giường, BedAssetPort chưa có thao tác đó; đã hỏi cho phép bổ sung cổng MANDATORY.
- Thứ tự kỹ thuật: chuẩn bị entity T014 độc lập trước service/test tích hợp. Test FR_001_StructureEntitiesTest có 2 assertion đỏ trước khai báo; sau cài và spotless:apply, 25 test của task + ArchTest/ModuleBoundaryTest qua. Chưa đánh [x] trước kiểm cuối phase. Năm danh mục dùng CatalogBase và JPA hiện có; Bed là entity nhóm BUSINESS với cột state không cho JPA INSERT/UPDATE, trạng thái do gateway Phase 2 ghi.
- T011: test tích hợp ban đầu không nạp context vì RoomType.standardBeds mặc định ánh xạ INTEGER, trong khi migration là SMALLINT; khai rõ columnDefinition SMALLINT, giữ schema đã có. Sau sửa môi trường test có 8/8 assertion đỏ do service chưa cài; cài phần cơ cấu độc lập, kiểm 25 unit/kiến trúc + 8 IT qua. Bổ sung kiểm mã phòng, mã giường trong/tách phòng: 23 ArchTest + 5 IT mã duy nhất qua. Không tuyên bố đã kiểm nhánh có phân bổ thật khi bảng của T023 chưa có.
- T015 hiện dùng JPA hiện có cho danh mục, JDBC chỉ đọc tham chiếu M03; khóa mục khi sửa, If-Match qua VersionEtag, audit cùng transaction và chỉ ghi thuộc tính thay đổi. ObjectProvider nhận SPI vị trí/tài sản, thiếu adapter thì báo SOURCE_UNAVAILABLE thay vì giả lập dữ liệu runtime. Kiểm đổi chính sách phòng có người đang/tương lai chưa hoàn tất: nếu phát hiện phân bổ thì chặn do thiếu nguồn M01, không mặc định coi phù hợp.
- T013/T016: khóa phòng trước khi tạo giường để tuần tự việc đếm; JPA tạo thuộc tính, BedStateService ghi CREATE_BED/lịch sử/audit, BedAssetPort.recordIncreaseForBed trong cùng transaction; đếm mọi giường chưa RETIRED để cảnh báo. Test đầu có 1 failure + 1 error do spy cấu hình port MANDATORY bên ngoài transaction; override đúng bean fake bằng MockitoBean, chạy lại 2/2 assertion đỏ, sau cài 23 ArchTest + 2 IT qua. Không tạo bản fake lớp thứ hai. BedStore.refresh đồng bộ version sau gateway JDBC.
- Test bổ sung If-Match/currentValue có 2 test, 1 failure vì thiếu version hiện hành; snapshot thêm version cho 412, loại version khỏi audit nghiệp vụ. Kiểm sau sửa 23 ArchTest + 2 IT qua; audit đổi tên chỉ có trường name.
- Review phần đã cài phát hiện thiếu nhật ký cơ cấu tạo giường/cảnh báo theo data-model mục 9; bổ sung test đỏ (2 IT, 1 failure) trước sửa, thêm M03_STRUCTURE_CREATED với roomId/code và details.warnings cùng transaction. Review còn đặt câu hỏi kích hoạt con dưới cha INACTIVE: chưa tự thêm điều kiện nghiệp vụ; BR-M15-06/Q-271 chỉ chốt chặn tên trùng, đã hỏi người dùng chốt quy tắc.
- Hợp đồng HTTP còn thiếu GET chi tiết + ETag cho area/floor/room-type/transfer-reason, DELETE/reactivate của room-type/transfer-reason so với T015 và nhu cầu đọc trước If-Match. Đã hỏi bổ sung cụ thể, chưa thêm endpoint khi chưa có phép. Chưa sửa giao diện, chạy gen API hay viết nguồn M01/cổng đổi vị trí; không thêm thư viện.
- Kiểm toàn backend cho phần độc lập sau review: `mvnw.cmd clean verify` chạy nền **BUILD SUCCESS**, exit 0, Spotless qua; **167 test đơn vị/kiến trúc + 439 test tích hợp**, 0 failure/error/skip, tổng **01:39**. `git diff --check` qua; 21 file Java mới đều thuộc M03/test M03 và có đủ trong lịch sử. Phase 3 vẫn chưa hoàn thành; giữ T011–T018 chưa đánh để không lẫn phần backend xanh với nghiệm thu cả phase. Tiếp tục sau khi người dùng trả lời các điểm nghiệp vụ/phạm vi hợp đồng đã hỏi.
### 2026-10-23 — Tiếp tục Phase 3 sau khi chủ dự án trả lời

- Chủ dự án duyệt dịch riêng phần Phase 3 của HTTP contract sang tiếng Anh, bổ sung GET chi tiết/ETag cho khu vực, tầng, loại phòng, lý do chuyển và DELETE/reactivate còn thiếu của hai danh mục sau. Có 39 operation mang `x-implementation-phase: 3`; 31 operation tương lai giữ tên logic cũ. Clone các schema/parameter/requestBody/response tiếng Anh để không đổi ngầm hợp đồng của story chưa triển khai. Ba path cách ly trùng tiền tố phòng được đối chiếu và giữ nguyên từ baseline.
- Chủ dự án chọn giữ phân công M01: chờ B công bố API/fake giới tính và mức chăm sóc, không viết hộ. Không duyệt thêm thao tác đổi vị trí vào BedAssetPort; đổi phòng của giường chưa phân bổ tạm trả 503 SOURCE_UNAVAILABLE, không thay dữ liệu/tài sản. Giường đã có lịch sử phân bổ vẫn bị chặn FR-007. Web chỉ cho sửa mã giường, khóa lựa chọn phòng khi sửa.
- Chủ dự án chốt kích hoạt cha trước con. Bổ sung hai test phòng/tầng dưới cha INACTIVE: 5 IT có 2 failure trước cài; sau cài kiểm task và 23 ArchTest qua. Không sửa spec hoặc tài liệu nghiệp vụ; đây là quyết định đã hỏi trực tiếp.
- T017: sáu controller theo danh mục, request/response riêng, không trả entity JPA. GET chi tiết trả ETag; lệnh dùng If-Match, Idempotency-Key và quyền thật ROOM_BED_CONFIGURATION. GET bảng giường lọc areaId/floorId/roomId/state, trả vị trí đầy đủ, trạng thái/hạn/quyền lệnh; dữ liệu người ở, phân bổ tương lai và các dấu của story sau chưa được cài. Không bịa dữ liệu người cao tuổi.
- StructureQueries cài LocationStructureApi/RoomPolicyApi: đọc cơ cấu M03, hiệu lực cả cha, tập tầng, loại phòng, mức cho phép và điều kiện nguồn qua SPI đã có. Bảng phân bổ/khu nghỉ chưa tồn tại được nhận diện trước truy vấn; phải đối chiếu tên cột khi làm migration tương ứng. BedQueries đọc projection BedDetails, quyền từ PermissionApi, thời gian BusinessTime; đọc port MANDATORY nằm trong transaction.
- Kiểm HTTP có 4 rồi 6 test thất bại trước controller đầy đủ; kiểm API đọc có 2 thất bại trước cài. Test bổ sung lý do thiếu đòi 422 REASON_REQUIRED nhưng nhận 400: bỏ Bean Validation @NotNull riêng ReasonBody để BusinessCommandAspect dùng quy tắc lỗi chung; không bỏ kiểm lý do. Kiểm sau sửa có 23 ArchTest + 10 IT (ApiContractIT, HTTP 7, API đọc 2) qua. Lý do tùy chọn của AreaWrite/RoomWrite được chuyển vào StructureChange và audit.
- ApiContractIT chỉ đăng ký hợp đồng 003 và danh sách 31 operation chưa cài; không bỏ kiểm hợp đồng 000/002. Đây là đăng ký test dùng chung cần A review khi mở PR. Generator thêm 003 và chỉ sinh operation được duyệt; chạy npm run gen:api, không sửa tay file sinh.
- T018 dùng frontend/src/features/beds, các thành phần Field/Input/Button/Sheet và token có sẵn. Cây bốn cấp, tab loại phòng/lý do, form CRUD, 409 ProblemAlert, 412 hiển thị giá trị mới và tải lại ETag, cảnh báo tạo giường được giữ sau khi tải lại. Quyền nút lấy từ /me/permissions, không tự suy từ role.
- Web: bốn test hành vi đỏ trên khung ban đầu, xanh sau cài; thêm một test hồi quy giữ cảnh báo tạo giường, tổng 5 test mới. Lint phát hiện set-state-in-effect trong luồng tải; chuyển việc tải sang promise có cờ hủy và callback dùng cho thao tác. Không tắt luật lint, không thêm thư viện. npm ci chỉ cài theo lockfile hiện có.
- Kiểm cuối frontend: npm test -- --run qua 105/105 test; npm run lint và npm run build exit 0. Build còn cảnh báo bundle khoảng 630 kB > 500 kB, chưa mở rộng phạm vi tối ưu bundle.
- Kiểm giao diện bằng build và fixture trong bộ nhớ trên localhost: cây bốn cấp, form tạo G2, lỗi 409 khi ngừng phòng còn G1. Chụp màn hình vì có thay đổi UI; fixture không ghi CSDL thật và không có dữ liệu người cao tuổi. Backend được kiểm riêng trên MySQL test. Ảnh kiểm lỗi ở C:/Users/nguye/AppData/Local/Temp/003-p3-web-409.png.
- Chưa tuyên bố cả Phase 3 hoàn tất: T012 và phần FR-006/T015 còn thiếu nguồn M01/bảng phân bổ T023; thao tác đổi phòng giường còn thiếu đồng bộ tài sản. T017 giữ chưa đánh để phản ánh phần hợp đồng sửa giường chưa đủ; T018 chỉ nghiệm thu phần màn cơ cấu đã cài. Task chưa xong đầu tiên vẫn là T012; không chuyển sang T019 trong lượt này.

- Kết quả chốt lượt tiếp tục: `mvnw.cmd clean verify` nền **BUILD SUCCESS**, exit 0, **167 unit/kiến trúc + 450 IT**, 0 failure/error/skip, tổng **02:02**; Spotless check qua. Đối chiếu tổng trực tiếp từ XML surefire/failsafe. Frontend **105 test**, lint/build exit 0; còn cảnh báo bundle >500 kB. Log cuối ngoài repo: `003-p3-final-clean-verify.log`, `003-p3-web-final-test.log`, `003-p3-web-final-lint.log`, `003-p3-web-final-build.log`.
- Review độc lập phần HTTP/API đọc/web không phát hiện Critical/Important mới. Sau kiểm cuối mới đánh [x] T011/T013/T014/T016/T018; giữ T012/T015/T017 chưa đánh vì thiếu phần đã ghi rõ. Team-plan không có dòng bàn giao cũ nào của C đủ điều kiện Xong trong Phase 3: BedAllocationApi, demo phân bổ, OfflineHandler và SPI chuyến vẫn ở story/feature sau. Đã thêm việc chờ cụ thể thay vì báo Xong sai phạm vi.
- `git diff --check` qua; đối chiếu danh sách file mới với Working-history.md thiếu 0 đường dẫn. Không có migration mới, thư viện mới, thay đổi M01 hoặc BedAssetPort; không commit/push/tạo PR. Nhánh vẫn feature/003-manage-bedroom. Bước tiếp theo theo tasks là **T012**, rồi hoàn thiện T015/T017 trước khi nghiệm thu Phase 3 và mở T019.

### 2026-10-23 — Khởi động Phase 4, chưa triển khai chức năng

- Baseline `d21e45e` (`003 phase 3`), nhánh `feature/003-manage-bedroom`; đầu phiên chỉ `Working-history.md` chưa theo dõi. Đã báo các dòng C và Mọi người ở team-plan mục 8. Giữ T012/T015/T017 chưa hoàn tất theo quyết định Phase 3; yêu cầu mới cho phép khảo sát phần Phase 4 độc lập.
- Chủ dự án duyệt dịch riêng HTTP Phase 4 (trả lời lựa chọn 1); chưa sửa HTTP contract, generator hoặc client vì backend chưa được triển khai. Chưa mở rộng phạm vi M01; câu hỏi về việc giữ chờ B hay cho bổ sung API/fake vẫn chưa có câu trả lời tại mốc này.
- Đọc tasks/plan và ghi chú cũ trước, rồi data-model mục 4, contracts/module-interfaces, research G-02/G-03/G-05 và FR-017–FR-025a khi cần đối chiếu. M01 chỉ có package-info.java, thiếu API/fake trạng thái, giới tính và mức hiện hành cho T019/T025/T026. Không đọc bảng M01, không tự tạo fake thứ hai. G-03 chỉ yêu cầu dòng M01 được 001 khóa khi đi qua lệnh trạng thái của 001; không bổ sung cổng khóa M01 mới (đính chính nhận định khảo sát ban đầu).
- Viết trước `DBR_09_AllocationSchemaIT` (phần T020/T022): 5 ca kiểm bảng/view, khoảng nửa mở và overlap INSERT/UPDATE, UNIQUE theo người, hủy không chiếm giường và bất biến, CHECK trạng thái/thời gian/bác sĩ. Tái dùng StructureTestSupport, TestUsers và FakeM03Config/FakeM15Config có sẵn. Chưa đủ bộ T020/T022, đặc biệt chưa có test 10 luồng, correction view và quyền retention.
- Tìm thấy JDK 25 tại E:/JAVA/JDK_GE/INS_25; chỉ đặt JAVA_HOME trong process Maven. Lần Maven trong sandbox không chạy được; lần chạy được phép dùng cache đã biên dịch test và tới Failsafe. `mvnw.cmd test-compile failsafe:integration-test failsafe:verify -Dit.test=DBR_09_AllocationSchemaIT`: exit 1, 5 test/0 failure/5 error/0 skip vì thiếu TEST_DB_URL lúc khởi tạo context. Chưa phải test đỏ do chức năng thiếu; không viết migration/service trước bằng chứng đỏ hợp lệ.
- `mvnw.cmd spotless:apply`: exit 0, chỉ định dạng test Java mới. `mvnw.cmd clean verify` chạy nền: exit 1, BUILD FAILURE, 29.265 giây; XML Surefire tổng 167 test, 0 failure, 1 error, 0 skip. Lỗi BackendApplicationTests.contextLoads do thiếu TEST_DB_URL. Chưa chạy tới Failsafe; không tuyên bố 167 test đều xanh hoặc toàn bộ ArchTest/IT đã được nghiệm thu. Ba biến TEST_DB_* chưa có trong process/User/Machine; không lưu thông tin đăng nhập.
- Giữ T019–T028 chưa đánh; không có mã production/migration mới, không thêm thư viện, không đổi API công khai, không đổi giao diện, không chụp màn hình, không commit/push/đổi nhánh. Log Spotless và clean verify lưu ngoài repo tại C:/Users/nguye/.codex/visualizations/2026/10/23/01a16883-8696-7971-82e2-a144c1d55517/003-p4-{spotless,clean-verify}.log. Chi tiết quá trình và file ở Working-history.md.
- Tiếp tục theo tasks: các việc Phase 3 còn T012/T015/T017; trong Phase 4 bắt đầu T019 (cần M01), T020, T021, T022, rồi T023–T028 theo phụ thuộc. Khôi phục TEST_DB_URL/TEST_DB_ADMIN_USER/TEST_DB_ADMIN_PASSWORD trước; chạy test schema thấy đỏ đúng nguyên nhân rồi mới viết V003_03 và mô hình. Chưa đánh Xong dòng bàn giao nào của C.

### 2026-10-23 — Tiếp tục Phase 4: dịch hợp đồng và chuẩn bị test DB

- Kiểm lại checkout: ba biến TEST_DB_* vẫn chưa có ở process/User/Machine; không có .env ở gốc/backend; M01 vẫn chỉ có package-info.java và chưa có thư mục fake M01. Đã hỏi cấu hình test, không dò/đoán mật khẩu. Tiếp tục phần không phụ thuộc môi trường.
- Thực hiện quyền duyệt dịch HTTP đã nhận: 6 operation thành GET /beds/summary, GET /bed-allocations/suitable-beds, POST /bed-allocations, GET /bed-allocations/{id}, POST /bed-allocations/{id}/cancel, GET /residents/{residentId}/location. Clone BedSummary/SuitableBed/BedAllocation/ResidentLocation/AllocationStatus; dùng lại Location/Reason/CareLevelCode/BedStatus/ResidentSummary. Mọi component cũ giữ nguyên cho phase sau; dữ liệu YAML so trước/sau xác nhận 64 operation khác không đổi, tổng vẫn 70 operation. Kiểm duyệt tham chiếu: 338 $ref đều giải được; 39 operation Phase 3 + 6 Phase 4 được duyệt dịch + 25 chưa dịch.
- ApiContractIT chỉ đổi tên sáu endpoint trong pending tương ứng; không bỏ kiểm 000/002, không gỡ pending của endpoint chưa cài. Đổi đăng ký test dùng chung cần A review, đã cập nhật dòng team-plan. Generator chỉ đổi chú thích để phản ánh duyệt từng phase; npm run gen:api sinh thêm 6 hàm api003.js, không sửa file sinh bằng tay. node --check qua.
- T027 yêu cầu GET danh sách phân bổ nhưng contract chưa khai báo GET ở collection; đã hỏi chủ dự án bổ sung GET /bed-allocations với residentId/bedId/status tùy chọn, trả mảng trong phạm vi quyền. Chưa có trả lời, không tự thêm endpoint. Việc này khác với dịch sáu operation đã tồn tại, không suy phép bổ sung từ phép dịch.
- DBR_09_AllocationSchemaIT tăng lên 9 ca: bổ sung khóa ACTIVE theo giường dù khoảng không chồng, effective_end ưu tiên actual/expected/vô hạn, view nhận bản đính chính mới nhất và JSON null, loại bản VOID; tách UNIQUE theo người FUTURE/ACTIVE thành hai test có fixture độc lập để không vô tình kiểm lỗi overlap thay UNIQUE. Chưa có assertion chạy trên MySQL. Chưa cài migration/service/controller/entity Phase 4, không tự tạo nguồn M01.
- Kiểm: spotless:apply test -Dtest=*ArchTest,ModuleBoundaryTest qua 23 test, 0 failure/error/skip, exit 0. npm test -- --run qua 105/105, npm run lint và npm run build exit 0; còn cảnh báo bundle ~630 kB >500 kB như trước. Không đổi UI, không chụp màn hình. clean verify nền sau bản dịch vẫn lỗi BackendApplicationTests.contextLoads do thiếu TEST_DB_URL (167 test, 0 failure, 1 error, 0 skip; chưa tới Failsafe). Sau review tách fixture UNIQUE, chạy lại spotless:apply clean verify nền; kết quả chốt ghi ở Working-history.md.
- Giữ T019–T028 chưa đánh; bước tiếp theo của Phase 4 vẫn T019 cần nguồn M01, rồi T020/T021/T022; phần DB độc lập tiếp tục khi nạp được môi trường MySQL: chạy test đỏ hợp lệ trước T023. Đã duyệt dịch không đồng nghĩa đã nghiệm thu T027/T028. Không commit/push/đổi nhánh.

- Chốt lượt tiếp tục: spotless:apply clean verify nền bản cuối exit 1 (31.492 giây), 167 test/0 failure/1 error/0 skip, BackendApplicationTests.contextLoads thiếu TEST_DB_URL, chưa tới Failsafe. git diff --check qua. Toàn bộ Phase 4 giữ chưa hoàn tất; xem Working-history.md phần Kết quả chốt lượt tiếp tục Phase 4.

### 2026-10-24 — Có MySQL test, hoàn thiện phần nền độc lập Phase 4

- Chủ dự án cung cấp cấu hình schema eldercare_test và duyệt thêm GET /bed-allocations như đề xuất. Chỉ nạp TEST_DB_* trong process Maven, không ghi mật khẩu vào file dự án hoặc biến User/Machine. Không còn vướng cấu hình test. M01 vẫn chưa có API/fake, quyết định chờ B chưa được thay đổi.
- T022/T023: DBR_09_AllocationSchemaIT ban đầu có 9 ca, 1 assertion failure (thiếu bảng/view), 8 error do bảng chưa tạo/cấp quyền; đã thấy RED trước migration. Tạo V003_03__bed_allocation.sql: bed_allocation, resident_position, allocation_reminder, FK bed.last_closed_allocation_id, 4 cột sinh/3 UNIQUE/5 CHECK/3 chỉ mục, trigger insert/update chống chồng, bất biến terminal và xóa theo retention; quyền app/retention theo mẫu 000. Chưa thêm FK M01. Chín IT +23 ArchTest xanh sau migration.
- View v_current_allocation áp v_latest_correction, loại v_voided_record, đổi bed/start/end và join vị trí theo giường sau đính chính. Dùng JSON_CONTAINS_PATH và NULLIF để phân biệt JSON null với trường vắng; CAST DATETIME trên phiên UTC nhận offset +07:00. Review bổ sung test đỏ thiếu 4 cột sinh trong view, rồi bổ sung effective_end/active_bed_id/active_resident_id/future_resident_id đúng ảnh sau đính chính. Bộ schema hiện 12 IT, kiểm thêm admin không vượt trigger, retention xóa được, cột sinh không gán được, CANCELLED không khóa khoảng. Kiểm task +23 ArchTest xanh.
- T024: FR_017_AllocationEntitiesIT đỏ 1 assertion thiếu entity trước cài. Thêm BedAllocation, ResidentPosition, AllocationReminder cùng AllocationStatus/CareLevelSource. Mapping Instant/enum và khóa nhắc ba thành phần. AllocationReminder là entity kỹ thuật đi theo dấu, dùng @DataGroup(BUSINESS) để công bố cơ chế thay đổi có trạng thái và không có setter theo ArchTest, không đổi nghiệp vụ bảng. Lần kiểm đầu ArchTest phát hiện thiếu annotation; bổ sung rồi 1 IT mapping +23 ArchTest xanh.
- T026 phần đọc: FR_017_AllocationQueriesIT có 4 assertion đỏ trước bản cài. AllocationQueries/AllocationStore/AllocationLocation cài BedAllocationApi của 002 và ResidentLocationApi của M03, dùng view đã áp đính chính. Phạm vi chỉ lấy ACTIVE/CLOSED chứa thời điểm theo [start_at,end_at), không lấy FUTURE; expected_end không tự kết thúc phân bổ thật. Batch đọc một lượt, kiểm mâu thuẫn người/giường và ném InvalidPermissionDataException nguồn M03.BED_ALLOCATION. currentOrFutureAllocation ưu tiên ACTIVE rồi FUTURE; floorOn lấy mốc đầu ngày BusinessTime. Không đổi interface/fake đã công bố. Bốn IT +23 ArchTest xanh. Chưa cài lệnh tạo/hủy nên T026 giữ chưa đánh.
- T020/T025 phần khóa: 2 test đỏ trước AllocationLocks (MANDATORY). Khóa dòng resident_position rồi distinct/sort giường theo id tăng dần. Test 10 transaction tranh một giường chỉ 1 INSERT commit; test không có transaction bị chặn và lỗi giường không tồn tại rollback dòng kỹ thuật. Đây là kiểm bộ khóa + SQL, chưa phải kiểm 10 request lệnh phân bổ đầy đủ.
- Review độc lập theo skill requesting-code-review ghi nhận INSERT IGNORE ở lockPosition có nguy cơ nâng khóa S→X. Đối chiếu G-03 xác nhận phải dùng upsert. Thêm test 10 transaction cùng resident_position đã tồn tại: RED với CannotAcquireLockException/deadlock tại SELECT FOR UPDATE. Đổi thành INSERT ... ON DUPLICATE KEY UPDATE, không đổi version/dấu của dòng đã có; 3 IT khóa +23 ArchTest xanh. Retry phải bao cả transaction lệnh mới có tác dụng, chưa thêm retry vào bộ khóa MANDATORY; phần lệnh/checker còn chờ M01, T025 chưa hoàn tất. Không thêm thư viện.
- T027 hợp đồng: thêm GET /bed-allocations với residentId/bedId/status tùy chọn, trả mảng BedAllocation trong phạm vi quyền, mã 200/403. Chỉ đăng ký pending, chưa có controller. Tổng 71 operation: 39 Phase 3, 7 Phase 4 đã duyệt, 25 chưa dịch; 341 $ref đều giải được. Sinh lại client bằng npm run gen:api, thêm getBedAllocations. Không sửa tay generated client.
- Kiểm toàn backend trước sửa deadlock: clean verify nền BUILD SUCCESS, 02:00, 167 unit/kiến trúc +469 IT, không failure/error/skip; Spotless qua. Frontend sau GET được duyệt: 105/105 test, lint/build exit 0; bundle ~630 kB còn cảnh báo. Sau sửa deadlock chạy lại clean verify nền; kết quả chốt và trạng thái task cập nhật ở Working-history.md, không lấy lượt xanh trước sửa thay cho kiểm cuối.
- Không đổi UI, không chụp màn hình, không thêm dữ liệu demo, không sửa nghiệp vụ/spec, không commit/push/đổi nhánh. T019/T021 và checker/lệnh của T025/T026 vẫn chờ API/fake trạng thái/giới tính/mức chăm sóc của M01. T027/T028 chưa cài chức năng. Phần Phase 3 còn T012/T015/T017 giữ nguyên giới hạn cũ.

- Kết quả chốt sau sửa deadlock (2026-10-24): clean verify nền BUILD SUCCESS, exit 0, 02:25; 167 unit/kiến trúc +470 IT, 0 failure/error/skip, Spotless qua. Sau kết quả này đánh T022/T023/T024; các task còn lại chưa đủ chức năng giữ chưa đánh. BedAllocationApi đã cài tại checkout, chờ PR/merge để bàn giao. Phase 4 chưa hoàn tất; xem Working-history.md phần kết quả chốt.

### 2026-10-24 — Khởi động Phase 5, chỉ triển khai phần độc lập theo quyết định chủ dự án

- Baseline dc1754c (003 phase 4), nhánh feature/003-manage-bedroom, đầu lượt chỉ Working-history.md chưa theo dõi. Đọc lại tasks/plan/Ghi chú triển khai và lịch sử, báo các dòng C ở team-plan mục 8. Không đổi nhánh/commit/push; không làm lại nền Phase 4 đã có trong Git.
- Chủ dự án duyệt dịch riêng operation/schema T036 và chọn giữ chờ B cho API/fake/sự kiện M01. Không khai báo M01/M02 thay B. T032 phụ thuộc checker FR-018 của T025 chưa có; T034 handler cần lệnh chuyển và khoảng vắng; T035 cần sự kiện/nguồn M01. Vì thế chỉ cài phần schema T034 và chuẩn bị hợp đồng T036, không nghiệm thu chuyển giường.
- T034 phần DB: FR_027_BackfillSchemaIT viết trước; RED 5 ca có 2 failure/3 error khi chưa có bed_backfill_request/quyền INSERT. Tạo V003_04__bed_backfill_request.sql với approval_request_id PK, resident_id/current_allocation_id/new_bed_id/transfer_at NOT NULL, FK tới approval_request (000 đã có), bed_allocation và bed; không FK M01. Index resident/bed theo transfer_at để tra dấu CHK051. Trigger chặn UPDATE kể cả admin và DELETE ngoài retention; app chỉ INSERT, retention DELETE, SELECT theo quyền chung hiện có. Sau migration 5 IT và 23 ArchTest/ModuleBoundaryTest xanh, 0 failure/error/skip, BUILD SUCCESS (01:38).
- Bảng kỹ thuật không tự sinh yêu cầu; handler sau này phải ghi dòng cùng transaction lập yêu cầu. Test rollback chứng minh INSERT theo transaction nguồn, không thay thế kiểm handler chưa cài. Tích hợp dọn dữ liệu dùng nhánh bed_backfill_request đã có trong M03GovernanceStore.delete: xóa chỉ mục trước phân bổ CLOSED; không sửa provider/SQL của 000. Test dùng tài khoản admin/retention để chứng minh trigger, không chỉ dựa việc app thiếu quyền.
- T036 phần contract: dịch duy nhất POST /phan-bo-giuong/{id}/chuyen-giuong thành POST /bed-allocations/{id}/transfer, giữ response 200/202/403/409/422, tham số Id/Idempotency-Key. Clone requestBody BedTransfer (newBedId,transferReasonId,reason,transferAt), schemas BedTransferResult (previousAllocation,newAllocation,warnings) và BackfillRequestCreated (approvalRequestId,status). Dùng BedAllocation/Warning/Reason/Problem đã dịch; component tên logic giữ cho các phase sau. Node/yaml so trước/sau xác nhận 70 operation khác và component cũ giữ nguyên; tổng 71 operation, nay 39 Phase 3 +7 Phase 4 +1 Phase 5 +24 chưa dịch, 345 $ref giải được.
- ApiContractIT chỉ đổi tên operation trong pending và chú thích; không gỡ pending, không bỏ kiểm hợp đồng 000/002. npm run gen:api sinh postBedAllocationsByIdTransfer, không sửa tay generated client; node --check qua. Cập nhật glossary mục 9 và dòng review A/C. Chưa có controller/UI, không chụp màn hình; mobile vẫn chưa mở triển khai chức năng.
- Frontend sau sinh client: npm test -- --run, npm run lint, npm run build đã chạy; kết quả chốt ghi ở Working-history.md. clean verify toàn backend đang chạy nền, sau khi kết thúc mới ghi kết quả. Toàn bộ T029–T036 giữ chưa đánh vì T034/T036 mới xong từng phần và các task khác chưa cài.
- Không thêm thư viện, không sửa nghiệp vụ/spec, không tạo bản giả nguồn M01. Dòng chờ B ở team-plan mở rộng cho FR-018 của Phase 5 và thêm khoảng vắng/sự kiện cần cho T034/T035. Bước tiếp theo theo tasks là T029 (cần T025/nguồn M01), T030, T031, rồi T032–T036 theo phụ thuộc; không dùng migration xanh thay cho nghiệm thu cả phase.

- Kiểm toàn backend lần đầu của Phase 5: BUILD FAILURE (05:06), 167 unit/kiến trúc qua; 475 IT có 1 failure, 0 error/skip. Lỗi ApprovalLifecycleIT.US5_1_create_isDraft_withRequesterTypeContentDateReason: mã YC-2026-5000393 vượt định dạng sáu chữ số. Nguyên nhân fixture FR_027_BackfillSchemaIT lấy TestUsers.nextId() làm approval_request.id, đẩy AUTO_INCREMENT của 000 lên cao. Sửa riêng helper approval() của test 003 bằng GeneratedKeyHolder/RETURN_GENERATED_KEYS, không sửa test/mã 000. Chạy lại task +ArchTest và ApprovalLifecycleIT để xác minh, rồi clean verify nền bản cuối; kết quả chốt ở Working-history.md.

- Kiểm sau sửa fixture: task +ApprovalLifecycleIT cùng JVM (003 chạy trước 000), 23 ArchTest và 12 IT qua, BUILD SUCCESS, 01:20. Frontend 105/105 test, lint/build exit 0; git diff --check qua. clean verify nền bản cuối đã khởi chạy, kết quả chốt bổ sung sau khi kết thúc.

- Kết quả chốt phần độc lập Phase 5 (2026-10-24): clean verify nền bản cuối BUILD SUCCESS, exit 0, 04:10; 167 unit/kiến trúc +475 IT, 0 failure/error/skip; Spotless qua 775 file. Frontend 105/105 test, lint/build exit 0, còn cảnh báo bundle. git diff --check qua. Giữ toàn bộ T029–T036 chưa đánh vì T034/T036 mới hoàn tất từng phần; chủ dự án chọn chờ B, không nghiệm thu Phase 5. Lịch sử đầy đủ ở Working-history.md.

- Xác nhận lại 2026-10-24 sau yêu cầu hoàn thiện: checkout vẫn thiếu nguồn M01/checker nền. Người dùng chọn 2 (giữ chờ B, chưa nghiệm thu Phase 5), không cho mở rộng hợp đồng/fake M01. Giữ T029–T036 chưa hoàn tất, không thêm mã/lặp test cho lượt chỉ đọc và xác nhận này; ghi chi tiết ở Working-history.md.

### 2026-10-24 — T036: triển khai UI web độc lập trong lúc chờ M01

- Đính chính điểm dừng trước: thiếu M01 chặn kiểm nghiệp vụ/lệnh backend, nhưng không chặn việc làm UI web từ hợp đồng Phase 5 đã được duyệt. Sau nhắc của chủ dự án, tiếp tục phần này; không thay quyết định chờ B và không mở rộng M01.
- Đặt UI trong `frontend/src/features/beds/` theo frontend/README.md. `BedsPage` tải quyền trước; chỉ gọi danh mục cơ cấu khi có ROOM_BED_CONFIGURATION.VIEW. BED_ALLOCATION.VIEW mở tab Chuyển giường kể cả không có quyền cấu hình; CREATE mới hiện lệnh. Không suy quyền thao tác từ vai trò.
- `BedTransfersPanel` đọc GET /bed-allocations?status=ACTIVE; hiển thị mã hồ sơ vì BedAllocation chưa có tên người. `BedTransferForm` đọc giường phù hợp theo residentId/fromAllocationId và startAt khi nhập bù, đọc lý do còn hiệu lực, để lựa chọn trống. Không tự chọn giường hoặc tự quyết điều kiện nghiệp vụ/CFG tại client.
- Thời điểm tùy chọn nhập theo giờ Việt Nam, gửi ISO-8601 +07:00; bỏ trường transferAt khi trống để backend dùng đồng hồ nghiệp vụ. POST chỉ gửi trường hợp đồng và Idempotency-Key, không If-Match. Giữ khóa khi lỗi mạng/5xx và cùng payload; đổi payload hoặc lỗi 4xx cho lần lệnh mới. Khóa đóng/gửi trùng khi đang gửi. Khi đổi thời điểm/tải lại thì xóa lựa chọn giường, bỏ kết quả tải cũ bằng cleanup effect.
- Response 200 đóng form, báo đã chuyển và giữ warnings khi tải lại danh sách; 202 chỉ báo mã yêu cầu nhập bù chờ duyệt, đọc lại dữ liệu server, không tự chuyển vị trí trên client. ProblemAlert hiển thị đầy đủ unmetConditions của 409; có tải lại giường phù hợp. Tất cả gọi qua generated client đã có, không thay hợp đồng/thư viện.
- Test trước: 6 ca panel RED khi skeleton chưa có luồng; GREEN 6/6 sau cài. Thêm test tích hợp quyền allocation-only RED (5 cũ xanh), rồi GREEN 12/12 sau gắn BedsPage. Test khóa đóng khi đang gửi RED (6 xanh/1 đỏ), sau sửa GREEN 13/13 cho hai file. Kiểm cuối toàn frontend 113/113 test, lint/build exit 0; bundle 636.84 kB còn cảnh báo >500 kB. Chạy ArchTest riêng, kết quả chốt ở Working-history.md.
- Kiểm trực quan trên production build với HTTP fixture cục bộ ngoài repo: 1440×900, 768×1024, 390×844; Sheet không tràn ngang ở ba cỡ. Chế độ tối đọc được; Enter mở, Escape đóng, Tab từ lý do đến Hủy; response 202 hiện chờ duyệt và giữ vị trí cũ. Đây là kiểm UI với fixture, chưa phải end-to-end backend. Không sửa AppShell dùng chung. Ảnh/log để Temp, không thêm dữ liệu demo vào production.
- T036 chỉ hoàn thành phần UI web, vẫn chưa [x] do endpoint backend chưa cài; mobile chưa mở theo mobile/README.md. Toàn bộ Phase 5 chưa hoàn tất; thứ tự tiếp tục vẫn T029 rồi T030/T031, T032–T036 theo phụ thuộc M01/checker. Không commit/push.

### 2026-10-24 — Khởi động Phase 6 (T037–T044)

- Baseline `473c812` (003 phase 5 wait M01), checkout sạch trừ Working-history.md chưa theo dõi. Giữ nhánh hiện tại, không commit/push. Đã báo các dòng C tại mục 8 team-plan và đọc tasks/ghi chú triển khai trước data-model/contracts/research.
- Dùng lại BedTransitions/BedStateService của T008, không tạo máy trạng thái thứ hai. T037 thêm bộ tham số hóa 184 trường hợp từ bảng data-model mục 3: 30 chuyển hợp lệ và 154 cặp bị chặn. Đây là test bổ sung cho mã đã có, không cố tạo RED giả bằng sửa mã đúng. Kết quả cả bộ cộng 23 ArchTest là 207 test xanh.
- Chủ dự án duyệt dịch 3 operation T041: POST /beds/{id}/suspend, POST /beds/{id}/reuse, GET /beds/{id}/state-history. Clone BedStateChanged/BedStateHistoryRow/After, dùng lại Reason/Bed/BedStatus/Problem; giữ các component tên logic phục vụ phase sau. Script so YAML xác nhận các operation/component cũ ngoài ba operation giữ nguyên; sinh client bằng npm run gen:api. Chỉ gỡ ba pending trong ApiContractIT sau khi có controller, cần A review đăng ký chung.
- T041: lệnh suspend/reuse tại BedService, khóa dòng giường, dùng BedStateService ghi trạng thái/lịch sử/nhật ký trong một transaction. Kiểm nguồn tài sản qua BedAssetPort, chặn dấu chờ chuyển người và phân bổ FUTURE từ view đã đính chính. Quyền ROOM_BED_CONFIGURATION.UPDATE (ma trận Phụ lục 27/Q-246), Reason bắt buộc. Hợp đồng hai lệnh không có If-Match: giữ nguyên hợp đồng đã duyệt, khóa dòng để kiểm trên dữ liệu mới nhất, không tự thêm header.
- Lịch sử T041 đọc theo bedId, cursor after là id tăng dần, trả ảnh gốc cùng dấu corrected của CorrectionApi; tên actor qua AccountApi, không đọc bảng module khác. T047 mới cài provider và các luồng đính chính nghiệp vụ. Không hiển thị mã tài khoản như họ tên. Không đổi giao diện ở Phase 6, không chụp màn hình.
- Test lệnh T041 viết trước: RED 6 ca (4 assertion failure/2 error do thiếu method sau khi siết assertion loại lỗi), rồi cài. Lần đầu sau cài còn 1 assertion so thông báo “lý do” với “Lý do”; sửa test của task sang ReasonRequiredException đúng hợp đồng, không sửa thông báo chung. HTTP test RED 4 ca sau sửa fixture thiếu basis_type/basis_id; GREEN cùng test lệnh và ApiContractIT là 11 IT +23 ArchTest, BUILD SUCCESS 41.188 giây.
- T040 đang cài tác động nội bộ trong BedHoldService/BedHoldStore: giữ/hủy hồ sơ chờ, giải phóng khi vắng, gia hạn kết thúc dự kiến, tất cả MANDATORY. Chưa đăng ký facade BedAllocationCommandApi vì interface còn các lệnh theo lịch của Phase 5 chưa có; không thêm stub cho các thao tác chưa cài. Test 5 ca RED trước khi có service, rồi GREEN cùng 23 ArchTest (34.653 giây). Giữ T040 chưa đánh cho tới khi có facade đầy đủ và kiểm các nhánh còn lại.
- T043 vẫn thiếu SPI/DTO/enum của M01; BaoQuaBatDauDuKien cần trạng thái Đang tiếp nhận từ M01. Giữ quyết định chờ B, không khai báo M01 hoặc fake thay B. Tiếp tục phần hết hạn giữ chỗ độc lập của T044; kết quả chốt bổ sung sau kiểm.

- T044 phần hết hạn: thêm HoldExpiryJob (ScheduledJob, ShedLock, cron 15 phút và startup), HoldExpiryProcessor (mỗi giường một transaction), SchedulerRuns ghi kết quả từng đợt. Giữ người vắng chỉ đặt hold_expiry_notified_at, ghi plannedAt/executedLate và phát sự kiện; hồ sơ chờ đổi về EMPTY, callback 004 và sự kiện trong cùng transaction. Profile test không tự chạy cron/startup, test gọi runNow. Đăng ký dòng FR-015 tại SchedulerScopeArchTest theo chính hướng dẫn của file, không nới luật; cần A review cùng ApiContractIT.
- Test T044 RED 3 ca trước job; sau cài hai nhánh chạy được, test mô phỏng callback lỗi vướng proxy MANDATORY khi stub, sửa test trỏ đúng ultimate target; tiếp gặp suy diễn generic varargs ở Mockito.reset, sửa tham số kiểu rõ ràng. Không bỏ test hoặc sửa fake M01/004; kết quả chốt ghi sau khi kiểm lại.

- Review cuối có hai P2 đã tái hiện RED trong FR_012_BedHoldsIT: 7 ca có 1 failure/1 error (5 ca cũ xanh), lỗi hủy là INVALID_BED_TRANSITION sau chờ khóa; lỗi gia hạn là ALLOCATION_TIME_OVERLAP từ SQL thay ConditionNotMetException. Sửa hủy bằng current/locking read trên dòng bed sau khóa; kiểm gia hạn trên view hiện hành trong khóa dòng gốc. Lần đầu câu locking read đòi quyền khóa view (app chỉ SELECT), không cấp quyền UPDATE cho view: đổi FOR UPDATE OF để chỉ khóa bảng bed_allocation gốc. Kiểm lại 7 IT +23 ArchTest xanh, BUILD SUCCESS 36.406 giây. Không sửa guard/test của người khác, không re-review sau lượt sửa.
- 348 $ref của contract giải được; 71 operation gồm 39 Phase 3 +7 Phase 4 +1 Phase 5 +3 Phase 6 đã dịch, 21 operation còn tên logic chưa dịch. clean verify nền bản cuối đã khởi chạy; chưa đánh checkbox trong khi đợi. Kết quả chốt ghi tiếp ở Working-history.md.

- Kết quả chốt Phase 6 phần độc lập (2026-10-24): `mvnw.cmd clean verify` nền BUILD SUCCESS, exit 0, 02:06; 351 unit/kiến trúc +495 IT, không failure/error/skip, Spotless 786 file qua. Frontend 18 file/113 test, lint/build exit 0; cảnh báo bundle >500 kB giữ nguyên. git diff --check qua. Chỉ sau kết quả này đánh T037/T041; T038/T039/T040/T042/T043/T044 chưa đủ giữ chưa đánh. Không nghiệm thu toàn Phase 6, không mở PR/commit/push. Không đổi UI hay chụp màn hình. Task chưa hoàn tất tiếp theo trong Phase 6 là T038; sau đó T039, hoàn thiện facade T040, T042/T043/T044 khi có nguồn/chức năng nền. M01 vẫn chờ B theo quyết định cũ; không tự thêm API/fake.

### 2026-10-24 — Tiếp tục Phase 6 sau câu hỏi “tại sao lại dừng phase 6”

- Đính chính điểm dừng: nguồn M01 chặn T039/T043 và job trạng thái, nhưng không chặn toàn bộ nhánh tài sản của T038/T042. Việc dùng kết quả clean verify xanh để kết thúc lượt trước đã bỏ sót phần độc lập còn làm được. Tiếp tục nhánh đó; không đổi quyết định chờ B hoặc tự mở rộng API.
- Đọc lại FR-013/FR-013a/FR-013b và G-03/G-12 vì cần phân biệt ngoại lệ giường hỏng với thao tác bảo trì thường. Thêm FR_013_BedAssetStateIT trước: RED đầu 7 ca (4 failure/3 error do thiếu service), rồi 9 ca gồm giữ nguyên phân bổ có căn cứ theo lịch/phòng không hiệu lực (RED 5 failure/4 error). Sau cài 9/9 IT +23 ArchTest xanh, BUILD SUCCESS 40.614 giây.
- BedAssetStateService/BedAssetStateStore là component nội bộ của nhánh T042, không implements port khi các callback FR-013b/suitable/theo lịch chưa đủ. Không đổi chữ ký BedAssetCommandPort hoặc tạo stub trả thành công. Adapter sẽ dùng component khi hoàn thiện phụ thuộc.
- Lệnh nguồn đã kiểm quyền gọi apply trong transaction MANDATORY, mang cờ hư hỏng khi dọn giường/báo hỏng lại từ 019; không thêm endpoint trên giường. Khóa resident_position của người có FUTURE tăng dần, rồi bed; đọc FUTURE lại bằng current/locking read. Nếu phát sinh người khác trong khi khóa thì yêu cầu thử lại transaction nguồn, không khóa người sau giường. Chặn ACTIVE/OCCUPIED/HELD, chặn chuyển sai bảng/phòng không hiệu lực, chặn FUTURE khi ngoại lệ không đạt.
- Ngoại lệ đã cài cho phân bổ ban đầu không gắn metadata nguồn: EMPTY/AWAITING_READY với hư hỏng khi dọn giường hoặc báo hỏng lại đang có dấu; RETIREMENT theo FR-013a. Hủy FUTURE với lý do Giường hỏng cùng transaction state/history/audit và phát FutureAllocationCancelledForDamagedBed cho Hành chính/Trưởng tầng phụ trách. READY khôi phục SUSPENDED/lý do cũ hoặc EMPTY; khi EMPTY và không FUTURE/dấu thì phát BedBecameEmpty.
- Phân bổ mang basis_type/basis_id/previous_allocation_id chưa được xử lý như phân bổ độc lập: trả SOURCE_UNAVAILABLE trước khi ghi, vì cần luồng nguồn/theo lịch để bảo đảm request cùng chuyển Áp dụng không thành. Đây là giới hạn kết nối được công bố, không phải quy tắc nghiệp vụ mới hay trạng thái thành công giả. Giữ T038/T042 chưa đánh.
- Review độc lập riêng 3 file mới không thấy P1/P2; reviewer không chạy Maven. Đang chạy lại clean verify nền sau thay đổi mới, kết quả chốt ghi sau khi process kết thúc. Không đổi UI, không chụp màn hình, không thêm API/library/commit/push.

- Chốt phần tài sản bổ sung (2026-10-24): clean verify nền bản mới BUILD SUCCESS, exit 0, 02:12; 351 unit/kiến trúc +504 IT, 0 failure/error/skip, Spotless 789 file qua. Review riêng 3 file mới không có P1/P2. git diff --check qua. Không đánh thêm checkbox, không nghiệm thu adapter/FR-013b/theo lịch còn thiếu. Frontend/UI không đổi trong lượt bổ sung, không chạy lại kiểm frontend khi không có thay đổi mới; kết quả 113 test/lint/build của lượt trước đã ghi riêng. Không commit/push.

### 2026-10-24 — Rà và tiếp tục các task Phase 1–6 sau khi M01 đã merge

- Phạm vi được chủ dự án yêu cầu: rà mọi task cần M01 trong Phase 1–6 và triển khai tiếp theo thứ tự, giữ nguyên hợp đồng đã duyệt, test trước; không commit/push/merge tiếp. Checkout vẫn có merge chưa commit từ lượt trước, giữ nguyên index và code nhóm khác.
- Phase 1–2: T001–T010 đã đánh; T010 còn nguồn mốc cuối lưu trú của 004 qua SPI, không thay bằng bảng M01. Không làm lại nền.
- Phase 3: T012/T015/T017 thiếu kiểm FR-006 với ResidentApi.gender, CareLevelApi.current và fallback nguồn lưu trú Q-301. Triển khai trước; phần đổi phòng giường cần BedAssetPort vẫn tạm hoãn theo quyết định cũ.
- Phase 4: T019/T021/T025/T026/T027/T028 cần checker dùng trạng thái/giới tính/mức của M01; T020 cần lệnh phân bổ để test đồng thời. Dùng checker chung cho query và command, tiếp tục sau FR-006.
- Phase 5: T029/T031/T032/T033/T034/T035/T036 cần checker/lệnh nền và ResidentStatusApi.statusPeriods, CareLevelChanged/InitialAssessmentConfirmed/PersonalInfoUpdated. Loại lưu trú đổi thuộc 004, không tự khai báo thay B.
- Phase 6: T039/T043 cần StatusTransitionParticipant và DTO M01, T044 cần ResidentApi.status; T038/T040/T042 cần lệnh nền Phase 4–5. Giữ các tác động nội bộ đã kiểm, nối theo thứ tự phụ thuộc.
- Quyết định kỹ thuật: ánh xạ tường minh CareLevel.REGULAR của M01 sang CareLevelCode.FREQUENT của M03 (cùng mức “thường xuyên”); không valueOf giữa hai enum khác chữ. Dùng API thật của M01, FakeResidents do B cung cấp khi cần; CareLevelApi chưa có fake riêng thì test tích hợp dùng bản thật, không tạo fake nguồn thứ hai. Fixture test có thể thiết lập dữ liệu M01, production chỉ dùng api.
- Theo dõi tiến độ và bằng chứng RED/GREEN trong Working-history.md; chưa đánh thêm [x] trước clean verify cuối.

- Tiến độ nối M01 lượt sau merge: FR-006 đã dùng API thật, 7 IT xanh; checker FR-018 7 IT xanh; lệnh tạo/hủy 5 IT xanh; 10 transaction tranh giường chỉ một thắng; sáu endpoint Phase 4 đã cài và 4 HTTP IT +ApiContractIT xanh. Chưa đủ T027 (summary/nhánh quyền cần hoàn thiện), chưa đủ Phase 4.
- Bảo đảm đọc mới sau khóa: riêng AllocationCommands.create và StructureService.update dùng READ_COMMITTED, không đổi isolation toàn hệ thống; cancel dùng current/locking read sau khóa. Test hồi quy review 14 IT có 3 failure trước sửa, sau sửa cùng 25 ArchTest xanh (56.193 giây). Giữ HELD của giường đích FUTURE, chỉ hủy giữ ở giường khác; giường AWAITING_READY chỉ nhận FUTURE từ ready_deadline. Không miễn các điều kiện này.
- T035 phần M01: AllocationSuitabilityListener nhận ba sự kiện bằng ApplicationModuleListener sau commit/outbox; AllocationSuitabilityService khóa vị trí rồi giường, dùng cùng policyConditions (a,b,c) của AllocationChecker, đặt/gỡ room_unsuitable theo dữ liệu mới nhất, chỉ báo khi dấu từ false sang true. Không dùng toàn bộ FR-018 để gắn dấu (giường ACTIVE vốn OCCUPIED); không dùng payload cũ để đánh giá lại. FR_034_AllocationSuitabilityIT RED thiếu listener rồi GREEN, cùng 25 ArchTest, 52.717 giây. Chưa có sự kiện loại lưu trú đổi của 004; không tự khai báo thay B, T035 chưa đánh trọn.
- T044 phần quá bắt đầu: FutureAllocationPastStartJob/PastStartAllocationProcessor/Store, chạy 15 phút và startup, test không tự chạy cron/startup. Lọc ResidentApi.byStatus ADMISSION_IN_PROGRESS trước và kiểm lại status sau khóa; chỉ báo, không mở ACTIVE. notified_planned_start_at lưu chính start_at đã báo, nên đổi mốc mới báo lại; không lưu thời gian chạy thay mốc. Hai IT RED trước rồi GREEN cùng 25 ArchTest (52.026 giây). Đăng ký job đúng phạm vi tại SchedulerScopeArchTest; không đổi enum runJobs/hợp đồng api-000, cần A review/đồng bộ sau. Không dùng job mới để nghiệm thu T043 chưa cài.

- Quyết định sau review T043: chủ dự án giữ M01 cho B, không đổi isolation nguồn. M03 dùng current/locking read sau khóa; thêm migration kỹ thuật V003_04_1__waiting_hold_index.sql lưu waiting_hold_bed_ids trên resident_position và backfill từ bed, không sửa migration đã merge. Tra id giữ chỗ qua dòng vị trí hiện hành trước khóa giường tăng dần; BedStateService duy trì chỉ mục trong cùng transaction. Không dùng REQUIRES_NEW production vì tách transaction phá rollback nguồn và cần connection phụ; REQUIRES_NEW chỉ dùng trong test mô phỏng lệnh khác commit giữa snapshot và khóa. Các bảng policy đọc hiện hành khi recheck; deadlock của transaction nguồn vẫn phải rollback cả lệnh, không retry riêng SPI trong transaction đã lỗi.
- Retry T025 dùng org.springframework.resilience.annotation.Retryable của Spring Framework 7 hiện có thay tên thư viện Spring Retry trong Technical Context; không thêm dependency. Advice trước transaction, chỉ PessimisticLockingFailureException, 2 lần thử lại với delay kỹ thuật 50ms. Test ghi dở rồi lỗi khóa xác minh transaction sau không giữ INSERT trước.
- T028 có BedAllocationForm/BedAllocationsPanel và tab Phân bổ giường; chọn người, giường đều do người dùng, không mặc định giường; gọi client sinh sẵn, lỗi qua ProblemAlert, ngày qua formatDateTime. Giữ mặc định trang cũ. Test hẹp 19 test beds xanh; chưa nghiệm thu task trước kiểm cuối.

### Kết quả rà phụ thuộc Phase 1–6, sau khi M01 đã merge

| Phase/task | Đã triển khai tiếp | Phần còn lại |
| --- | --- | --- |
| 1–2 / T001–T010 | Không làm lại nền đã nghiệm thu | Nguồn ResidencyEndApi của 004 vẫn chờ, không thay bằng M01.finalStatusAt |
| 3 / T012,T015,T017 | FR-006 theo API thật, fallback Q-301, chặn đổi cha phòng, kiểm race thay chính sách | T017 đổi phòng giường vẫn chờ cổng đồng bộ tài sản đã tạm hoãn; không mở rộng API |
| 4 / T019–T028 | Checker, khóa/retry, tạo/hủy, API đọc, 7 HTTP operation, web chọn giường và bảng phân bổ | Chờ clean verify toàn kho; không đánh task chỉ vì kiểm hẹp xanh |
| 5 / T031,T035 | Ba sự kiện M01 đã nối để đánh giá lại dấu phòng không phù hợp | ResidentStatusApi.statusPeriods đã có bản thật từ PR #38; nhập bù T031/T034 vẫn cần workflow của M03; sự kiện 004 và các phần T029–T036 chưa nghiệm thu vẫn giữ |
| 6 / T039,T043,T044 | Participant 7 lệnh, rollback nguồn, các race RR, job chỉ báo FUTURE quá mốc | Các phần T038/T040/T042 còn thiếu facade/ngoại lệ tài sản; enum clock job giao A theo quyết định chủ dự án |

- Sửa khóa phòng–giường sau RED tái hiện deadlock: M01 resident → M03 resident_position → shared room guards theo id → bed locks theo id. StructureService sửa phòng giữ exclusive room trước bed; BedStore.lock cũng guard phòng, BedStateStore chỉ FOR UPDATE OF b và đọc room FOR SHARE, không nâng room thành exclusive. Cấu trúc này vẫn cho hai lệnh trên hai giường cùng phòng chạy đồng thời.
- Kiểm overlap bằng FOR SHARE trên toàn v_current_allocation không dùng được: InnoDB status đã xác nhận 53 row lock S, bao gồm bản ghi phân bổ không liên quan; hai FUTURE cùng nâng lên ACTIVE tạo deadlock. Ngoài ra MAX(sequence_no) lồng trong view correction giữ snapshot RR nên có thể bỏ sót đính chính vừa commit. Test mới đã RED cả hai ca (003-m01-current-overlap-red.log: 1 failure +1 error).
- Hướng sửa chỉ M03: AllocationCurrentReadPool và CurrentAllocationOverlapStore đọc nguyên view bằng plain SELECT READ_COMMITTED, kết nối chỉ đọc/autoCommit, pool riêng tối đa mặc định 2 (eldercare.m03.current-allocation-read-pool-size). Dùng driver/property/tài khoản của DataSource chính, không thêm thư viện, không thêm bean DataSource, không đổi cấu hình toàn hệ thống. Đóng Connection/PreparedStatement/ResultSet ngay sau truy vấn, không dùng DataSourceUtils/JdbcTemplate để tránh bind connection phụ vào transaction nguồn. Do dùng pool riêng, không cần connection rảnh trong pool lệnh khi mọi parent đang giữ connection.
- Phạm vi bắt buộc của phép đọc độc lập: chỉ checkCurrent trong COMPLETE_ADMISSION, sau toàn bộ khóa và trước lần ghi bed_allocation đầu tiên của participant; loại FUTURE của parent bằng id. Mọi kiểm khác dùng dữ liệu nguồn hoặc current locking read trong parent; mọi ghi, audit, event vẫn ở transaction nguồn và rollback cùng M01. Không dùng helper này cho luồng đã ghi allocation chưa commit cần nhìn thấy. Reviewer chỉ đọc đã xác nhận call site hiện tại đáp ứng điều kiện này, không còn finding P1/P2 trong pool/store.
- Sau sửa overlap: 20 FR024 +25 ArchTest xanh (003-m01-current-overlap-green.log). Thêm ca 5 parent transaction vét toàn bộ pool chính size 5, đang chạy trong clean verify cuối. Không kết luận ca này xanh trước khi có kết quả.
- Clean verify lượt đầu: 392 unit/kiến trúc xanh; 990 IT có 5 failure +10 error. Một error M03 là deadlock đã sửa ở trên; 14 lỗi còn lại thuộc fixture/contract M01 khi nạp thêm participant/controller 003. Theo quyết định giữ M01 cho B, không sửa các test đó để cho xanh; đã thêm bàn giao ở team-plan mục 8. B cần cô lập participant thật M03 khỏi fixture StatusTestBase đang có hai participant giả, và giới hạn ResidentContractIT vào contract 001, vì endpoint location thuộc contract 003. Không đổi hợp đồng hoặc nới điều kiện production để làm test cũ qua.

- Clean verify lần hai phát hiện RR trigger vẫn có next-key deadlock trên idx_allocation_bed_start khi UPDATE start_at (đã xác nhận InnoDB status), dù phép đọc view độc lập đã sửa scan/correction. Không sửa trigger cũ và không đổi M01: migration V003_04_2__allocation_write_guard.sql thêm một dòng khóa kỹ thuật thực sự xung đột; AllocationLocks lấy nó trước position. Explicit RC giữ S để chạy song song, RR/default giữ X để không có hai RR writers cùng giữ range/gap locks. Toàn bộ writers allocation hiện tại của M03 đều vào AllocationLocks trước ghi. Đây là tuần tự hóa các lệnh nguồn RR, có thể giảm thông lượng của lệnh trạng thái; lệnh tạo phân bổ RC vẫn qua test mười luồng. Cần giữ thứ tự này ở các luồng thêm sau, nhất là T032/T033.
- Không chọn chỉ prelock gap FOR UPDATE: gap X vẫn tương thích gap X và supremum không phải real record; hai INSERT có thể cùng mắc insert intention. Đã bổ sung hồi quy hai COMPLETE_ADMISSION đổi từ hai FUTURE sang giường chưa từng có allocation trong supremum gap. Không thêm range prelock hoặc ép index trigger rồi tuyên bố hết deadlock.
- Fixture deadline dùng cùng mốc microsecond cho ghi và kiểm. `003-m01-write-guard-green.log`: 30 IT +25 ArchTest qua, trong đó 22 FR024 gồm bão hòa 5 connection và supremum replacement. Reviewer không còn blocker P1/P2 trong mode guard; vẫn cần clean verify cuối. B đang giữ phần test M01 theo quyết định chủ dự án, không nới điều kiện participant để làm fixture cũ xanh.

- Kết quả clean verify cuối (003-m01-clean-verify-final3.log, exit 1, 03:41 phút): 392 unit/kiến trúc qua; 993 IT có 5 failure +9 error, không skip. Toàn bộ 148 IT M03 /31 lớp xanh, gồm 22 participant. Lỗi còn lại chỉ ở bảy lớp fixture/contract M01, đã bàn giao B trong team-plan mục 8 theo quyết định không sửa M01. Frontend 232 test, lint/build qua, còn cảnh báo chunk. Không đánh thêm [x] hoặc ghi hoàn tất Phase 3–6 trước khi kiểm toàn kho qua; không commit/push/merge tiếp. Chi tiết lớp lỗi, file và thứ tự task tiếp ở Working-history.md.


### Phase 7 — T045–T048, triển khai ngày 2026-10-24 (chưa nghiệm thu)

- Chủ dự án duyệt dịch riêng 5 operation T048: `GET /location-history/residents/{residentId}`, `GET /location-history/beds`, `GET /location-history/roommates`, `POST /bed-allocations/{id}/corrections`, `POST /beds/{id}/state-history/{historyId}/corrections`. Request `RecordCorrection` giữ hai loại chuẩn của 000: `AMEND_CONTENT`, `VOID`; không đổi operation phase sau. Client sinh bằng `npm run gen:api`; phần đăng ký 003 trong `ApiContractIT` bỏ đúng năm pending đã cài, theo dòng bàn giao A review/C của team-plan.
- Đường dẫn logic trong tasks được đặt thành `internal/history/{domain,persistence,service}`, `internal/allocation/{domain,persistence,service}`, `internal/bed/{domain,persistence,service}` và `web/{request,response}`. `LocationHistoryStore` chỉ đọc `v_current_allocation`, dùng khoảng nửa mở, tự nối view theo phòng, ngày lịch qua `BusinessTime.startOfDay`; không tự tính correction mới nhất cho tra cứu.
- `LocationHistoryReading` cài API dữ liệu nội bộ cho 007/016 như `ResidentLocationApi`: module gọi kiểm quyền chức năng của mình. Cần tách khỏi HTTP để điều dưỡng dùng qua 007 theo G-10, không bị ép quyền trực tiếp của màn M03. HTTP chỉ gọi `LocationHistoryQueries`, kiểm khóa vai trò, `PermissionApi.hasPermission` (gồm thu hẹp tài khoản/pháp lý), hợp các `ScopeApi.historyFilter` hợp lệ rồi kiểm đối tượng trước khi lấy tên/khoảng vắng. Không dùng historyFilter để cấp quyền ghi.
- Khoảng vắng gọi `ResidentStatusApi.statusPeriods` với ba trạng thái đã công bố. Cập nhật 2026-10-28: M01 đã có bản đọc lịch sử thật từ PR #38; không còn blocker stub. M03 giữ nhánh 503 `SOURCE_UNAVAILABLE` khi nguồn ném `UnsupportedOperationException`. Test dùng `MockitoSpyBean`: chỉ ca nguồn không sẵn sàng cấu hình lỗi tường minh, ca tích hợp mới dùng hồ sơ/lịch sử M01 thật. Không sửa source/test do B sở hữu hoặc thay đổi API.
- Dấu nhập bù theo người xét giao khoảng bị ảnh hưởng với cửa sổ hỏi, đọc trạng thái yêu cầu qua `ApprovalRequestApi`, không đọc bảng phê duyệt trong production M03. Request trước thời điểm bắt đầu cửa sổ vẫn có thể ảnh hưởng kết quả. Chủ dự án đã chọn **giữ nguyên contract**, không thêm `pendingBackfill` vào `BedLocationHistoryRow`: HTTP theo giường chưa thể biểu diễn dấu FR-027, gồm giường đích trống. Ghi tồn tại, không tự bổ sung trường.
- SPI `CorrectableRecordProvider.validateContent` không nhận id nên không thể khóa đúng bản gốc tại bước này. T047 kiểm cấu trúc/whitelist ở validateContent, sau đó khóa và kiểm FR-019 ở `afterCorrection` trong cùng transaction với generic correction/audit; sai thì rollback và không phát event. Phân bổ chỉ CLOSED/CANCELLED; ready chỉ sự kiện CONFIRM_READY, ba trường note/disinfected/ppeUsed. Không cập nhật nguồn bất biến hoặc chạy lại máy trạng thái.
- HTTP correction dùng transaction RC và chuẩn hóa thời gian offset thành UTC trước khi chuyển 000. Luồng áp dụng phê duyệt đang RR giữ nguyên isolation của module sở hữu; kiểm view đã commit cho các allocation khác bằng pool RC chỉ đọc của M03, loại id đang đính chính. Không tái dùng helper COMPLETE_ADMISSION cho luồng đã ghi correction; store correction riêng mô tả giới hạn nhìn dữ liệu chưa commit. Thứ tự khóa theo AllocationLocks hiện có.
- UI trong `features/beds`: tab Lịch sử vị trí, ba kiểu tra cứu, chọn hồ sơ tường minh, giờ Việt Nam, nhãn correction/pending, khoảng vắng phân biệt A/người kia; lỗi qua ProblemAlert hoặc PermissionDeniedState. Tra cứu lại bỏ dữ liệu cũ và chống response cũ ghi đè. Dùng khung lịch sử correction chung chỉ đọc, form sửa phân bổ riêng để gửi đúng kiểu dữ liệu/endpoint đã duyệt. Không thêm thư viện hoặc đổi token thiết kế.
- Test trước đã thấy RED khi thiếu bean API, thiếu provider và endpoint 404; lỗi fixture enum/môi trường được sửa riêng, không tính là RED nghiệp vụ. Đã có các lượt hẹp xanh và SC-003 với dữ liệu 30 ngày/10 người/3 phòng dưới 1 giây trong môi trường test đã làm nóng. Kết quả review, kiểm cuối và danh sách log chính xác bổ sung ở Working-history.md sau khi lệnh kết thúc. Chưa đánh [x].

- Final review/TDD bổ sung: kiểm FR-019 cả `(bed_id=? OR resident_id=?)` để không cho cùng người chồng ở hai giường; HTTP giao khoảng vắng với từng overlap được phép, không lộ các ngày sau khi người cùng phòng chuyển khỏi tầng. Before event RR hợp chain đọc trong parent và amendment mới nhất đã commit từ public v_latest_correction qua poolRC, chọn correctionId mới hơn trong cùng chuỗi đang khóa; giữ được cả prior chưa commit của parent. RED đã tái hiện before là 01:00 thay02:00 và khoảng vắng có2 thay1 trước sửa.
- Payload generic của M03 yêu cầu UTC Z/microsecond, miền DATETIME; facade HTTP vẫn nhận offset ISO và chuẩn hóa/truncate microsecond. Không sửa view/migration cũ. `AllocationCorrectionProvider` dùng hasPermission thực tế, kiểm tầng bản gốc cho cáchA/C; không chỉ dùng role-key list. UI đọc chi tiết trạng thái trước form, chặn ACTIVE; có lối mở cho CANCELLED từ bảng phân bổ.
- Kiểm hẹp cuối `003-p7-scoped-final.log`: 22 IT Phase7 +1 ApiContractIT và25 ArchTest xanh. Frontend cuối:239 test/35file, lint/build xanh, còn cảnh báo chunk>500kB. Chưa kiểm trực quan ba bề rộng/chế độ tối trên browser; không ghi đạt mục này. Đang chờ clean verify nền, kết quả cuối ghi Working-history.md.

- Kiểm cuối sau sửa fixture ID (2026-10-24): `003-p7-clean-verify-final2.log`, clean verify nền exit1/04:09phút. 392unit/kiếntrúc xanh;1015IT có5failure+9error,0skip; toàn bộ170IT M03/34class xanh, gồm22IT Phase7. ApiContractIT và7ApprovalLifecycleIT xanh. Lỗi mới của000 ở lần1 đã hết sau khi fixtureM03 dùng id tự sinh thay TestUsers.nextId; không sửa testA. 14lỗi còn lại chỉ thuộc7class M01 đã bàn giaoB (chi tiết Working-history.md).
- Kết luận nghiệm thu: giữ T045–T048 `[ ]` vì full verify còn đỏ, dấu pendingBackfill HTTP theo giường chưa có theo quyết định giữ schema; UI chưa kiểm trực quan browser đủ ba bề rộng/chế độ tối. Frontend239test/35file, lint/build xanh. Không commit/push/merge tiếp. Task tiếp vẫn nghiệm thu T045→T048 trước khi sang T049 của Phase8.


### Phase 8 — T049–T052, đang triển khai 2026-10-24

- Chủ dự án duyệt dịch riêng9operation T052 và schema/request/response liên quan sang tiếng Anh, giữ nghiệp vụ/các phase sau. Không suy thành cho phép sửa M01, APIclock000 hoặc các quyết định Phase7 trước đó.
- T050 đặt migration tiếng Anh V003_05__day_care_capacity.sql; hai entity danh mục kế thừa CatalogBase, các lớp trong internal/daycare/{domain,persistence,service}. Thêm duy nhất một dòng day_care_capacity_guard kỹ thuật để khóa xuyên suốt kể cả khi chưa có khu nghỉ; mọi lệnh cấu hình và DayCareCapacityApi.lock/checkCapacity cùng giữ khóa này. Chỉ khóa dòng khu nghỉ không đủ cho race tạo khu đầu tiên hoặc thay khu sau khi ngừng.
- Sức chứa không có cột lưu: đọc ParameterApi CFG-M03-01 một lần mỗi lần kiểm, không lấy mặc định số tự đặt. Lịch đến chỉ qua DayCareArrivalCounter do003đã khai báo và FakeDayCareArrivals cùng chủ; không đọc bảng004 hoặc tạo fake thứ hai.
- Test schema đã RED với thiếu bảng; không đánh checkbox trước clean verify cuối. Giữ baseline14lỗiM01 đã bàn giaoB, không bỏ test hoặc sửa phầnB để xanh.

- Bằng chứng TDD Phase 8: schema RED thiếu bảng; sau migration, hai assertion CHECK được sửa để kiểm đúng `UncategorizedSQLException`/thông báo CHECK MySQL 8 thay vì kỳ vọng `DataIntegrityViolationException`. Schema 3/3 xanh trong `003-p8-capacity-red3.log`; capacity 4/4 RED vì thiếu bean thật, rồi xanh trong `003-p8-http-red.log`. HTTP 4/4 RED 404 rồi xanh trong `003-p8-http-green.log`. Parameter listener RED thiếu lớp, rồi xanh; thêm mode SCHEDULER tái hiện thiếu căn cứ audit (FR-045), sửa bằng `Basis("BR-M03-25",...)`.
- Quyết định kỹ thuật: `LocalTime` dùng `@JdbcTypeCode(SqlTypes.LOCAL_TIME)` và `ResultSet.getObject(..., LocalTime.class)` để giữ giờ địa phương của buổi. Test tạo 07:00–12:00 đã RED vì ánh xạ `java.sql.Time` mặc định qua timezone UTC làm vi phạm CHECK. Không đổi timezone dùng chung hoặc thêm thư viện.
- Reviewer độc lập xác nhận nguy cơ snapshot RR ở `occupiedSessions`: `lock()` đọc khung mới còn plain read khung cũ. Test RR giữ snapshot trước khi transaction khác đổi giờ đã RED. Đổi `occupiedSessions` sang transaction có guard và current locking read; giữ quy tắc nửa mở `[arrival, departure)`.
- T052 bố cục theo màn hiện có: tab Bán trú trong Phòng, giường; khu nghỉ, danh mục buổi, bảng đếm theo khoảng ngày, form ngay trong tab. Không thêm màu/quyền/menu dùng chung. User yêu cầu tự quyết kỹ thuật và chỉ hỏi nghiệp vụ/API/thư viện/Git được áp dụng cho lựa chọn bố cục này. UI ban đầu test RED thiếu component/tab; 12 test liên quan xanh, lint/build xanh (còn cảnh báo chunk >500 kB).
- Đang chờ hai quyết định API riêng, chưa suy từ duyệt bản dịch: thêm `version` vào hai response để UI lấy If-Match; bổ sung nguồn residentId còn lịch tương lai vào `DayCareArrivalCounter`. Trong lúc chờ chỉ cài UI xem/tạo/đếm, không giả version=0 và không bịa danh sách người có lịch. Backend chặn chung bằng boolean SPI hiện có; phần liệt kê chưa hoàn tất.
- Sau sửa reviewer: `003-p8-review-green.log` exit0, 16IT cùng ArchTests xanh; gồm test concurrent, snapshot RR, ngừng tầng và race dời khu/ngừng tầng, parameter mode scheduler, HTTP bán trú và HTTP cơ cấu cũ. Reviewer rà lại hai fix không còn blocker mới. Chuẩn bị clean verify cuối, chưa đánh checkbox.
- Clean verify đang chạy phát hiện thêm một failure M01 khác baseline14: FR_023h_KhongDauNangLucIT regex cấm mọi trường OpenAPI tên capacity, nên bắt cả DayCareSessionCapacity.capacity mang nghĩa sức chứa buổi theo contract003 đã được dịch/duyệt. Không đổi tên trường HTTP để né test, không sửa test của B. Đã thêm bàn giao B trong team-plan mục8; kết quả cuối cần tính riêng failure này, không gọi nhầm là baseline cũ.
- Kiểm cuối Phase8: clean verify nền exit1/04:13phút (`003-p8-clean-verify-final.log`):392unit/kiếntrúc xanh;1031IT có6failure+9error,0skip; toàn bộ186IT M03/39class xanh (16IT mới Phase8). ApiContractIT xanh. 15lỗi đềuM01:14baseline +1failure mới regex capacity đã bàn giaoB, không sửa testB. Frontend243test/36file xanh; sau sửa utility type-label,12testhẹp/lint/build lại xanh. Giữ T049–T052 `[ ]`; còn hai quyết định API, nguồn004 thật và kiểm browser theo DESIGN.

### Quyết định chủ dự án sau kiểm Phase 8 — 2026-10-24

- Chủ dự án chọn phương án 2 cho cả hai câu hỏi: giữ nguyên HTTP contract, không thêm version vào DayCareRestArea/DayCareSessionResponse; giữ nguyên DayCareArrivalCounter, không bổ sung phương thức trả residentId tương lai.
- Áp dụng: phần sửa/ngừng hiệu lực trên UI tiếp tục chờ cách lấy phiên bản được duyệt; không đoán If-Match=0. Backend tiếp tục chặn chung bằng hasFutureArrivals, chưa liệt kê người còn lịch. Không coi quyết định giữ contract/SPI là miễn yêu cầu T052 hoặc tự đánh hoàn tất phase.
- Không thay mã nguồn, contract hoặc fake trong lượt xác nhận này. Tiếp theo vẫn xác minh/hoàn thiện T049–T052, xử lý bàn giao nguồn004 và15lỗiM01, kiểm trực quan UI. Không tự chuyển T053 hoặc sửa phầnB.

### Xác minh riêng T049–T050 theo yêu cầu tiếp tục — 2026-10-24

- Đối chiếu từng yêu cầu T049 với các test hiện có: BR_M03_03_DayCareCapacityIT (giới hạn, giao buổi, CFG unset, khoanh vùng), BR_M03_25_DayCareParameterIT (đổi CFG và cảnh báo), FR_038_DayCareHttpIT (đổi khung/recount và ngừng khu nghỉ khi còn lịch), FR_038_DayCareSchemaIT (UNIQUE/XOR/CHECK), FR_038_DayCareConcurrencyIT (khóa đồng thời/current read/vị trí cha). Nguồn004 dùng đúng FakeDayCareArrivals đã có; không cần cài thêm spec của B để kiểm các yêu cầu này trong003.
- T050 đã có đầy đủ ở V003_05__day_care_capacity.sql, không cần tạo migration trùng hoặc sửa SQL: hai danh mục, vị trí XOR, một khu nghỉ ACTIVE, tên buổi ACTIVE duy nhất và end_time>start_time; FK chỉ tới floor/area của003. Khóa kỹ thuật và grant đã có trong cùng migration mới.
- Bổ sung coverage T049: chuỗi ba ngày cách nhau một tuần chỉ báo đúng ngày tăng lên4/3chỗ; T050 thêm assertion CHECK từ chối khung có giờ bắt đầu bằng giờ kết thúc. Hai kiểm bổ sung cho logic đã tồn tại, không phát sinh sửa production hoặc một vòng RED mới; bằng chứng RED trước implementation của Phase8 vẫn ở các mục trước.
- Lượt kiểm đầu 003-t049-t050-verify.log exit0:16IT và25testkiếntrúc xanh. Lượt bổ sung chỉ chạy hai lớp đã sửa cùng ArchTests; kết quả ghi tiếp sau khi lệnh kết thúc. Không đổi HTTP/SPI, M01,004,UI hoặc thêm thư viện; giữ hai lựa chọn không mở rộng hợp đồng của chủ dự án.
- Lượt bổ sung 003-t049-t050-extra-verify.log exit0/43.366giây:8IT (5capacity+3schema) và25testkiếntrúc xanh; spotless:apply qua. Không sửa migration hoặc mã production. Phần test/migration của T049–T050 đủ phạm vi kiểm riêng; giữ checkbox cho tới gate clean verify cuốiPhase8 như yêu cầu ban đầu. Không chạy lại kiểm toàn kho vì lượt này chỉ thêm coverage test và tài liệu; lần clean verify gần nhất vẫn có15lỗiM01, không ghi thành kết quả mới. Task kế tiếp trong thứ tự là T051, sau đóT052; không mở rộng thực hiện trong lượt chỉ địnhT049–T050 này.

### Phase 9 — T053–T056, bắt đầu 2026-10-25

- Chủ dự án yêu cầu triển khai Phase9, cho phép tiến hành dù Phase8 còn tồn đọng; không suy thành duyệt thay đổi M01,000 hoặc hai đề nghị Phase8 đã từ chối. Duyệt dịch riêng5operation T056 sang tiếng Anh, giữ nghiệp vụ/schema tương ứng. Đang hỏi bổ sung hai API đọc tối thiểu cho chọn phòng theo quyền cách ly và chọn Bác sĩ; chưa cài hai API đó trước khi được duyệt.
- Lớp đặt trong internal/isolation/{domain,persistence,service}, web/{request,response}; migration V003_06__room_isolation_history.sql. Không chép đường dẫn logic của tasks. Lịch sử là append-only, sự cố chỉ tham chiếu logic tới007. Không thêm thư viện.
- Đặt/gỡ cách ly dùng allocation_write_guard độc quyền trước khóa room để đồng bộ với thứ tự khóa của các writer phân bổ/M01. Chỉ thay room.isolated và version; không đổi trạng thái giường hoặc tự tạo phiếu. JPA Room.isolated đã updatable=false, nên Store ghi SQL lệnh riêng và refresh ảnh phản hồi. Gỡ phòng chỉ xóa dấu chờ bác sĩ khi nguồn007 xác nhận phòng hết chịu chặn; thiếu nguồn trả503, không giả hết khoanh.
- T055 dùng ControlledTransferService riêng cho ngoại lệ kiểm soát lây nhiễm, tái sử dụng AllocationLocks/Checker/CommandStore/BedStateService. Chỉ miễn ALLOCATION_ISOLATION, giữ những điều kiện FR018 khác. Nguyên tử đóng phân bổ cũ/mở mới/đổi hai giường; giữ cùng hạn/lý do giữ giường của người vắng theo FR032. Lý do danh mục cố định INFECTION_CONTROL (nhóm y tế/an toàn), không dựng lại luồng chuyển thông thường Phase5 còn thiếu. Bác sĩ kiểm AccountApi.hasRole và ghi id vào phân bổ/audit; sự kiện DoctorAssignedToAllocation báo đúng hồ sơ nhân viên của tài khoản đã chọn.
- T054 chưa thể nối listener sự kiện007: checkout m07 chỉ có package-info, chưa có kiểu API/fake ContainmentLifted/ContactAdded do D sở hữu. Không tự khai báo lớp m07 hoặc fake thứ hai. Vẫn cài phần lệnh gỡ cách ly phòng; phần tự gỡ dấu khi007 phát sự kiện và xử lý thêm danh sách tiếp xúc giữ chờ D.
- CLAUDE.md/mobile README hiện chỉ mở mobile người cao tuổi; phần nhân viên của T056 để[ ], không triển khai Flutter. Backend/web không chờ mobile theo quy định hiện hành.
- TDD: FR_008_RoomIsolationIT có3test RED 404 trước controller (003-p9-isolation-red.log), rồi xanh cùng ArchTests (003-p9-isolation-green.log). BR_M03_21_ControlledTransferIT có4test RED404 trước T055. Lần compile đầu T055 sai constructor PermissionDeniedException, đã sửa theo API hiện có; chưa ghi xanh trước kết quả lượt mới.

#### Bổ sung Phase 9 sau phê duyệt ba API đọc

- Chủ dự án đã duyệt cả ba GET: `/room-isolation/rooms` (ROOM_ISOLATION.VIEW), `/bed-allocations/prescribing-doctors` (BED_ALLOCATION.CREATE), `/room-isolation/pending-stays` (ROOM_ISOLATION.UPDATE và Bác sĩ Hoạt động). Giữ nguyên quyền API danh mục phòng/phân bổ/tài khoản cũ. Hành chính có thể đọc tài khoản người thân theo chú thích12 của bảng quyền; `GET /accounts?type=STAFF` trả danh sách rỗng, không phải403. Đã sửa giả định sai trong test mới của003, không sửa002.
- Ruling: đóng gói luồng ngoại lệ trong ControlledTransferService riêng thay vì dựng chuyển thường đang thiếu — tránh làm quá phạm vi T055 — nếu API chuyển thường được cài sau phải tiếp tục dùng cùng AllocationLocks/Checker/BedStateService.
- Ruling: gỡ cách ly phòng lấy allocation_write_guard X trước room — cùng thứ tự writer phân bổ và bảo vệ bulk xóa dấu chờ — có chi phí tuần tự hóa ngắn lúc lệnh cách ly chạy.
- Test đã phát hiện trigger không cho sửa phân bổ CLOSED: phải xóa dấu chờ trên phân bổ cũ khi còn ACTIVE rồi mới close, metadata chỉ ghi trên bản mới. Mọi bước vẫn cùng transaction; test lỗi nguồn004 sau khi đổi cả hai giường chứng minh rollback phân bổ/trạng thái/lịch sử/audit. Fixture ngày giờ chuẩn hóa microsecond để khớp DATETIME(6), không nới assertion.
- Test rollback dùng spy đích sau proxy (`AopTestUtils.getUltimateTargetObject`); stub trên proxy gọi phải MANDATORY ngoài transaction nên lượt đầu lỗi thiết lập test. Đã sửa fixture, không đổi transaction production để chiều test.
- `003-p9-backend-scoped2.log`:25 test kiến trúc +15 IT gồm ApiContractIT xanh, không skip. API contract bỏ đúng5 pending cũ đã cài, kiểm luôn3GET mới. `npm run gen:api` sinh lại api003; không sửa client bằng tay.
- Web: RoomIsolationPanel đọc lịch sử/đặt/gỡ với lý do; IsolationWorkspace chọn phân bổ/giường/Bác sĩ và xác nhận ở lại; BedsPage có tab Cách ly theo permission backend. Nút lệnh giữ Idempotency-Key khi retry, chống gửi trùng, giữ nội dung khi lỗi. Sửa đúng dòng beds trong navigation theo hướng dẫn đăng ký màn frontend README để Bác sĩ tới được trang; test menu RED trước sửa.
- `003-p9-web-full.log`:250 test/38file xanh; lint/build qua, còn cảnh báo chunk lớn. Có kiểm trực quan bằng fixture HTTP cục bộ tách khỏi backend thật; không coi fixture là nghiệm thu tích hợp007/004.
- Review cuối theo executing-plans có một finding: hasRole không kiểm trạng thái, activeWithRole bao gồm TEMPORARILY_LOCKED cho thông báo009. Cần003 kiểm AccountView.status==ACTIVE ở lựa chọn/lệnh Bác sĩ, không đổi API002; test hồi quy được chạy RED trước sửa. Kết quả cuối bổ sung sau khi lệnh kết thúc.
- Final: fixed kiểm trạng thái Bác sĩ —4ca hồi quy RED (003-p9-review-red2.log)→GREEN, scoped19IT+25testkiến trúc (003-p9-review-green.log, exit0/01:56). Áp dụng kiểm ACTIVE cả bác sĩ chỉ định, danh sách lựa chọn và bác sĩ xác nhận/xem pending. Không có finding khác được reviewer báo.
- Đã xem UI bằng fixture ở375/768/1280, chế độ tối và Tab tới nút lưu xác nhận; form chuyển chọn đúng giường/Bác sĩ từ GET đã duyệt. Server/script QA tạm đã dừng/xóa, không thêm fixture vào product. Không có thay đổi UI sau lượt frontend250test/lint/build.
- Kiểm cuối nền `003-p9-clean-verify-final.log` kết thúc exit1,06:25phút:392unit/kiến trúc xanh;1050IT có6failure+9error,0skip. Toàn bộ205IT M03 xanh, gồm18ca mới Phase9 và hồi quy trở về phòng cách ly Phase6. Spotless check1308file,0cần sửa.15lỗi còn lại thuộc8lớpM01 đã bàn giaoB: BR_M01_01_TemporaryAbsenceOverdueIT, BR_M01_02_ReassessmentNoticeIT, CompleteAdmissionConsentIT, CompleteAdmissionWishWarningsIT, FR_023h_KhongDauNangLucIT, FR_034a_CompleteAdmissionIT, ResidentContractIT, StatusHistoryIT. Không sửa test/sourceB hoặc contract003 để né regex capacity. Giữ T053–T056[ ] theo gate toàn kho và phần007/mobile chưa đủ; không ghi nghiệm thu toànPhase9.

### Phase 10 — T057–T060, bắt đầu 2026-10-25

- Chủ dự án yêu cầu làm Phase10; đã báo bốn dòng chứa HungNX-nguyen trong mục8 team-plan. Đọc tasks/ghi chú plan trước khi đối chiếu G-09/G-14, API hiện có và FR-044/045. Giữ các quyết định trước về M01,007, API đồng hồ000 và mobile nhân viên.
- Đã duyệt dịch riêng GET `/beds/{id}/readiness-conditions`, POST `/beds/{id}/confirm-readiness`, request/response liên quan; thiếu khử khuẩn/bảo hộ dùng422 theo T057 và BedStateService hiện có (contract cũ ghi409). Không đổi API phase khác.
- Ruling: phương thức nhận Target do service tra từ giường, controller truyền vào phương thức có RequiresPermission — aspect002 cần Target để cấp đúng quyền theo nhiệm vụ SHIFT_LEAD — phải kiểm lại tầng sau khóa, không tin Target do bên gọi tự dựng. 002 đã có BED_READINESS và FR-019g, không cần sửa quyền A.
- Ruling: T058 dùng BedStateService duy nhất để đổi trạng thái, lưu note trên dòng lịch sử vừa tạo trong cùng transaction. Nguồn007 được hỏi lại tại lúc xác nhận; last_closed_allocation_id rỗng (Sử dụng lại) không có người vừa rời. Thiếu nguồn khi có người vừa rời trả503, không đoán không cần khử khuẩn.
- Nhánh chờ chuyển theo lịch nối BedAllocationCommandApi.applyScheduledTransfer đã khai báo; bản thật T032–T033 chưa có trong checkout. Không tạo fake production hoặc dựng luồng duyệt004 thay B; thiếu provider phải rollback cả xác nhận. Nhánh này chưa thể nghiệm thu thật trước khi có bản cài chuyển theo lịch; test không được coi mock là hoàn tất dependency.
- Hai job nhắc đặt trong scheduling, SQL kỹ thuật ở internal/bed hoặc internal/allocation/persistence, xử lý từng đối tượng ở service. Theo G-09 dùng ScheduledJob/ShedLock/SchedulerRuns, chu kỳ15phút và startup; profiletest không tự chạy. Chỉ đăng ký hai dòng job003 trong SchedulerScopeArchTest theo hướng dẫn của chính test, bàn giaoA review; không mở rộng enum runJobs API000 (quyết định trước của chủ dự án).
- T060 yêu cầu app nhân viên, chưa mở theo CLAUDE.md:12/mobile README; giữ phần app[ ]. Không tự thêm web khi task không yêu cầu; vì vậy không chụp màn hình trong lượt chỉ backend này.

- TDD thực tế: BR_M03_09_BedReadinessIT ban đầu4failure404/5ca, CHECK đã có nên không ghi REDschema mới. Sau cài, test ngoài phạm vi sửa kỳ vọng403→404 đúng cơ chế che đối tượng002. Hai job RED thiếu bean trước implementation (`003-p10-reminders-red.log`), sau đó2IT+25ArchTest xanh. Không thêm migration vì các cột trạng thái/deadline/mốc nhắc, bảng allocation_reminder và CHECK bằng chứng khử khuẩn đã có.
- Note của xác nhận được INSERT ngay trong bed_state_history; constructor14tham số của BedTransition giữ tương thích, constructor15tham số mới có note. Không UPDATE fact append-only. Service xác nhận dùng guardX→room guard→bed lock để việc gọi chuyển đang chờ về sau không đảo khóa với writer phân bổ; chi phí là tuần tự hóa ngắn trong lệnh này.
- Review cuối:3ca hồi quy RED (`003-p10-final-review-red.log`,14IT/3failure/0error) đã chứng minh phát sai BedBecameEmpty khi còn FUTURE/assetflag và marker ROOM_UNSUITABLE chưa có cùng lệnh đổi dấu. Sửa bằng dùng lại BedHoldService.becameEmpty (gate có sẵn), và AllocationCommandStore.markUnsuitable chèn/xóa marker cùng transaction của lệnh nguồn, không reset khi xử lý lại cùng dấu. Gỡ rồi đặt lại bắt đầu counter0/mốc mới. Constructor store nhận BusinessTime để mọi mốc theo đồng hồ nghiệp vụ.
- Marker có sẵn giữ started_at/last_reminded_at; job đọc CFG-M03-02 mỗi lần kiểm. Dữ liệu cũ hoặc dấu từ luồng chưa có marker được khởi tạo ở lần quan sát đầu, không bịa thời điểm đặt dấu. Các writer AWAITING_REQUEST/AWAITING_SAME_PRICE ở T032–T033 khi được cài phải tạo/xóa marker cùng lệnh nguồn theo data-model mục7; hiện luồng đó chưa có. WITHOUT_BED đã có marker nguyên tử ở participant; ROOM_UNSUITABLE đã sửa trong lượt này.
- Test rollback dùng transaction processor thật, lỗi sau mark bị proxy Repository dịch thành DataAccessException với root cause IllegalStateException; đã sửa assertion test, không sửa production. `003-p10-final-scoped-green.log`:15IT (8readiness+4reminder+1rollback+1contract+1suitability cũ),25kiến trúc,0failure/error/skip, exit0/01:03. Final: fixed cả2finding bằng3ca RED→GREEN, không có finding minor được báo. Không yêu cầu re-review.
- Client đã sinh lại; frontend250test/38file, lint/build qua, còn cảnh báo chunk>500kB. Chỉ client đổi, không đổi giao diện. Đã khởi chạy nền clean verify (`003-p10-clean-verify-final.log`); chưa đánh[x] trước kết quả cuối.
- Kiểm cuối đã kết thúc exit1/04:06phút (`003-p10-clean-verify-final.log`):392unit/kiến trúc xanh;1063IT có6failure+9error,0skip, đều8lớpM01 đã bàn giaoB. Đọc XML xác nhận218IT M03 xanh (205trước+13Phase10); ApiContractIT xanh; spotless1322file,0cần sửa. Không sửa source/testB để qua gate. Giữ T057–T060[ ]: ngoài gate toàn kho, còn nhánh chuyển theo lịch thật cần providerT032–T033 và app nhân viên chưa mở. Backend xác nhận thường/hai job/HTTP đã cài và kiểm xanh; không đồng nhất với nghiệm thu toànPhase10.

### Phase 11 — T061–T065, bắt đầu 2026-10-25

- Đã báo sáu dòng chứa HungNX-nguyen tại mục8 team-plan; đọc tasks/ghi chú plan, README backend trước hợp đồng1.8/1.8a/1.9 và data-model11. Không suy yêu cầu Phase11 thành cho phép sửa M01, APIclock000 hoặc tự mở app nhân viên.
- Chủ dự án duyệt riêng `BedFlagView.bedId: long → Long`, null chỉ cho WITHOUT_BED. Hợp đồng module ghi rõ biểu diễn này; không đổi trường khác hoặc HTTP. API ba interface đã khai báo giữ nguyên chữ ký thao tác.
- Ruling: T061 dùng chung store đọc snapshot lịch sử cho BedStatusApi/BedStatisticsApi, phân xử cùng occurred_at bằng id, không tính giường trước CREATE_BED. ScopeFilter rỗng trả dữ liệu rỗng, floor/room/resident là hợp các tập đúng ngữ nghĩa002; không suy quyền từ vai trò trong API dữ liệu, consumer kiểm chức năng của mình. Domain record đặt trong internal/bed/domain theo README.
- Khoảng Chờ sẵn sàng giao với [from,to), deadline tính từ lúc thật vào Chờ sẵn sàng và ParameterApi.readAt(CFG-M03-04,entryAt), không lấy CFG hiện tại cho quá khứ. stateAt trả Stream từ dữ liệu đã materialize trong transaction, không giữ cursor JDBC ra ngoài. Luợt Trống giữ qua hold WAITING_RECORD, bị cắt bởi sử dụng/bảo trì/tạm ngừng/ngừng dùng. Cảnh báo bàn giao chỉ dấu chờ bác sĩ, không tự lập mục giường quá hạn.
- countAvailable đọc trạng thái tại t, nguồn007 tại t, cấu hình phòng và dấu tài sản mà các API hiện có cung cấp; không bịa lịch sử cấu hình/tài sản vì các nguồn đó chưa có thao tác đọc lịch sử. Trường floor lịch sử giường dùng floor_area_id trên fact; room và cơ cấu còn theo quan hệ lưu hiện tại (giới hạn dữ liệu cần ghi rõ khi rà T065).
- T062: ApiContractIT đã đăng ký003 và chỉ còn pending chuyển thường Phase5; không xóa pending để giả hoàn tất. Test trigger chung chỉ quét @Entity nên thiếu bảng room_isolation_history chỉ có SQL; mở rộng đúng2bảng nhóm3 của003, không nới test của module khác.
- T063: đối chiếu team-plan mục1 đúng mapping:004→m02;007→m05/m06;008→m09;019→m03 (phầnm11 quaAPI E). m02/m05/m06/m09 chỉ có package-info; tìm toàn production không có lớp cài AllocationResidencySource/AllocationInfectionStatus/LocationReferences/BedAssetPort. Chưa có các provider thật cần kiểm; không thay fake bằng bean không tồn tại, không tạo fake production. Một số ghi chú lượt trước đã nhầm đường dẫn m07(006) và m14(016); không lấy nhầm đó làm bằng chứng T063.
- T064: chạy kiểm backend/web và Flutter hiện có nếu môi trường cho phép; app nhân viên vẫn chưa mở. Đo API danh sách300giường<1giây theo task riêng, không gọi đó là SC-005 hoàn tất: quickstart5 còn yêu cầu60người dùng,10lần thao tác Hành chính, trung vị<2phút. Kịch bản demo cần đối chiếu phần có/thiếu, không ghi đã chạy nếu chỉ có test backend tương ứng.
- T065: lập verification.md với file/method thực tế và khoảng trống; giữ mã BR/DBR/FR gốc và tên logic của bảng11, không sửa spec/nghiệp vụ. Không đánh[x] trước clean verify cuối và đủ yêu cầu từng task.

- T061 RED thật sau sửa fixture CHECK: `003-p11-reporting-red2.log` có5error thiếu ba bean API,0failure; hai lỗi CHECK ở lượt đầu chỉ là fixture CONFIRM_READY chưa có disinfection_required, không nhận là lỗi production. Sau implementation5IT+25kiến trúc xanh (`003-p11-reporting-green.log`). Không tạo migration hoặc thêm thư viện.
- T062/T064 kiểm riêng `003-p11-cross-cutting.log`:7IT+25kiến trúc xanh. SQL-only room_isolation_history có kiểm chung mới; DBR_23_RecordTablesIT dùng hàng thật đã commit để admin không vô tình kiểm0dòng. T064 dựng100phòng/300giường,10request MockMvc sau warmup; lượt hẹp96–256ms, trung vị tính đúng116.5ms. Đã sửa phép in trung vị số mẫu chẵn trước lượt cuối; không đổi assertion<1000ms.
- T065 thêm kiểm12mức thông báo FR-051 vào FR_024_BedEventsTest, không nhầm với FR-051 lưu giữ000; kiểm BedTransferred từ lệnh kiểm soát thật có đúng old/new allocation và tầng. `003-p11-traceability.log` xanh:4unit sự kiện+25kiến trúc,12IT chuyển. Đây là phủ hành vi đã có, không ghi một RED mới hoặc coi receiver005/011 đã cài.
- Final review có2finding P2; thêm2test RED đúng lỗi (`003-p11-review-red.log`:7IT/2failure/0error). Đã bỏ residentsAt(from) cho cả dải: mỗi Window đọc người của phân bổ CLOSED gần nhất trước lần vào trạng thái, REUSE không có người vừa rời. Cấu hình deadline được tính/materialize trong transaction trước khi trả Stream. `003-p11-review-green.log`:7IT+25kiến trúc xanh, exit0/48.887giây. Final: fixed cả2finding, không có minor được reviewer báo, không yêu cầu re-review.
- T064 frontend250test/38file, lint/build qua (`003-p11-web-*.log`, build933ms), còn cảnh báo chunk. Flutter SDK có sẵn: chạy flutter test, pub get các phụ thuộc đã khai báo xong nhưng process exit1 trước test vì Windows thiếu symlink/Developer Mode; không tự bật cài đặt hệ thống, không thêm package hoặc đổi manifest/lockfile.
- Kiểm cuối nền `003-p11-clean-verify-final.log` exit1/04:09phút:393unit/kiến trúc xanh;1073IT có6failure+9error,0skip;227IT M03 xanh (218cũ+9mới).15lỗi vẫn8lớpM01 đã bàn giaoB, không sửa source/testB. ApiContractIT và cả hai kiểm trigger xanh; spotless1328file sạch. Lượt danh sách300giường cuối có trung vị77.5ms/max85ms. Giữ T061–T065[ ] theo gate toàn kho và các phần chưa đủ; verification.md ghi rõ mapping/quickstart/SC/giới hạn, không tuyên bố toàn bộ003 hoàn tất.

### Sửa fixture HTTP FR-036 — 2026-10-28

- Phạm vi được chủ dự án chọn: sửa FR_035_LocationHistoryHttpIT, không triển khai thêm nghiệp vụ hoặc thay đổi API M01/M03. Đã tái hiện test cũ đỏ (mong 503, nhận 404).
- Giữ MockitoSpyBean thay vì thay toàn bean bằng mock: cần giữ nguồn thật cho ca tích hợp trong cùng lớp. doThrow chỉ đặt ở ca nguồn không sẵn sàng; không gọi nguồn thật để suy ra lỗi. Test mới ghi fixture vào hồ sơ và resident_status_history, dùng TestUsers.nextId(), transaction rollback và truy vấn theo id riêng; kiểm vắng của cả A/người cùng phòng, cắt hai biên và bỏ khoảng ngoài thời gian cùng phòng. Không gọi lệnh nghiệp vụ M01 để tránh biến kiểm nguồn đọc thành kiểm workflow/participant của module khác.
- Kết quả hẹp: 23 IT của FR_035_LocationHistoryHttpIT (8), FR_035_LocationHistoryIT (7), BR_M03_24_AllocationCorrectionIT (8) xanh; FR_001_ModuleSetupTest 2/2; Spotless qua. clean verify toàn kho (đã nạp backend/.env, JDK25) kết thúc exit1 trong 08:00: 680 unit/kiến trúc xanh; 1441 IT có 7 failure/13 error, không skip. Toàn bộ 231 IT M03/50 lớp xanh, HTTP lịch sử 8/8; Spotless 2127 file sạch. 15 lỗi/8 lớp M01 khớp dòng bàn giao; thêm 5 lỗi/3 lớp tham số M15 chưa có trong baseline, đã thêm dòng cho A, không tự kết luận là lỗi cũ hoặc sửa M15.
- Không đánh toàn T045/T046/T048; các phần nhập bù, UI/nghiệm thu chưa nằm trong việc được chọn. Không có thuật ngữ mới cần bổ sung glossary; không sửa migration, production, test M01.

### Kiểm chứng và đóng T012/T015 — 2026-10-28

- Chủ dự án chọn bắt đầu nhóm task có mã/test nhưng checkbox còn trống bằng T012/T015. Đối chiếu tasks, FR-001–FR-007, hợp đồng và StructureService/StructureStore/BedService: CRUD, kiểm giới tính/mức, khóa giường trước kiểm chính sách, chặn đổi cha khi đã có phân bổ, kiểm tham chiếu hiện tại/lịch sử và audit cùng transaction đã có. Không sửa production, migration, API, glossary (không có thuật ngữ nghiệp vụ mới).
- Bổ sung ca FR_007_bedWithAllocationHistoryCannotChangeRoom trong FR_006_RoomPolicyIT và FR_003_StructureLifecycleIT (EnumSource đủ năm StructureKind): tạo/sửa/ngừng/kích hoạt/xóa không có tham chiếu, If-Match tăng theo từng bước, dữ liệu SQL và năm action audit đúng thứ tự. Các ca mới chạy xanh ngay vì kiểm chứng mã có sẵn; không tuyên bố đã thấy đỏ do thiếu implementation.
- Bộ hẹp 36IT+2unit xanh; Spotless chỉ hai file test. Clean verify nền nạp backend/.env/JDK25 kết thúc exit1/07:29:680unit/kiến trúc xanh,1447IT có6failure+9error/0skip đúng8lớpM01 baseline; toàn bộ237IT M03/51lớp xanh, Spotless2128file sạch. Ba lớp tham số M15 từng đỏ ở lượt FR036 đều xanh lần này, chưa kết luận nguyên nhân đã được sửa. Log C:/Users/nguye/AppData/Local/Temp/eldercare-t012-t015-clean-verify-20261028.log.
- Đóng riêng T012/T015 theo yêu cầu đánh task đã được kiểm; không đóng Phase3/feature hoặc T017 (cổng đồng bộ vị trí tài sản chưa được duyệt). Các dòng chờ M01/E viết nguồn cho hai task này không còn là blocker: ResidentStatus/ResidentApi thật và ShiftLocationReferences đã có. Review nguồn tham chiếu M09 với ShiftQueriesIT 7ca và AssignmentIT31ca xanh; xóa dòng E cài LocationReferences đã review, giữ các review khác ngoài phạm vi.

## T029/T032 — chuyển thường, 2026-10-29

Phạm vi chủ dự án chọn: đợt1 chuyển trực tiếp tại thời điểm hiện tại; không cài chuyển theo lịch T033, nhập bù T034, endpoint/mobile T036 hoặc nghiệp vụ005. Dùng lại AllocationLocks/AllocationChecker/AllocationCommandStore và BedStateService; tạo BedTransferService ở internal/allocation/service, BedTransferStore ở persistence. Khóa người và hai giường theo G-03, đọc lại phân bổ sau khóa, kiểm quyền trên cả hai tầng và toàn bộ điều kiện đích. Đóng/mở cùng thời điểm microsecond; giữ expectedEnd, lý do danh mục và previousAllocationId. Giường cũ Chờ sẵn sàng; giường mới Đang sử dụng hoặc giữ đúng hạn Tạm vắng/Điều trị tại bệnh viện. Ghi audit M03_BED_TRANSFER và phát BedTransferred trong transaction nguồn; receiver005 vẫn là việc riêng.

Khác đơn giá giữa phòng cũ/mới theo SPI hiện hành: lý do thường trả409 RESIDENCY_CHANGE_REQUEST_REQUIRED; lý do y tế/an toàn được chuyển, phân bổ mới có AWAITING_REQUEST và allocation_reminder tạo cùng transaction, phát DifferentPriceBedTransfer cho Hành chính/Quản lý viện. Không tự tạo yêu cầu hoặc sửa đơn giá.

Review phát hiện chuỗi chuyển sau khi đã có dấu khác giá có thể làm mất nghĩa vụ nhắc. Đã hỏi bổ sung differentContractUnitPrice(residentId,newRoomTypeId,date) để đối chiếu giá hợp đồng. **Chủ dự án chọn phương án2: giữ API và để nhánh chuyển đang có dấu chờ lại sau.** Vì vậy nếu phân bổ hiện tại có awaiting_request hoặc awaiting_same_price thì khóa/đọc lại và trả503 SOURCE_UNAVAILABLE trước mọi ghi; giữ nguyên cờ, giường, phân bổ, mốc reminder, không phát sự kiện chuyển. Không suy giá hợp đồng từ lịch sử hoặc sửa SPI/fake của B. T032 chỉ hoàn thành một phần, phải giữ[ ]; T033 và T034 không thay đổi trạng thái.

TDD: test chuyển thường viết trước, chạy đỏ13ca vì thiếu bean; lượt fixture đầu có trùng tên danh mục nên đã thêm id vào tên rồi chạy đỏ lại. Sau cài13/13xanh. Hai ca hồi quy cho hai dấu chờ chạy đỏ (lệnh chuyển vẫn thành công), rồi mới thêm guard. Test mới BR_M03_07_BedTransferIT không có transaction bao ngoài, kiểm nguồn lỗi sau đổi giường hoàn tác, hai lệnh đồng thời chỉ một thành công, payload tầng cũ/mới và audit. Kết quả kiểm hẹp/cuối bổ sung bên dưới.

**Kết quả cuối 2026-10-29**: bộ hẹp43IT/7lớp xanh, gồm15ca mới; clean verify nền sau nạp backend/.env/JDK25 kết thúc exit1/09:31phút. XML xác nhận680unit/kiến trúc xanh;1483IT có7failure+9error,0skip:15lỗi/8lớpM01 khớp baseline, thêm1failure ApiContractIT đúng ba route T007a chưa đăng ký theo lựa chọn giữ file chung A. M03 toàn bộ252IT/52lớp xanh; M15 tham số không tái hiện lỗi cũ. Spotless:check riêng exit0. Đóng T029, giữ T032 một phần và không nghiệm thu Phase5/T063; 003 hiện23/65task[x],42[ ]. Không sửa nguồn004/007/019, không đổi API, không sửa migration đã merge, không sửa frontend/mobile. Log C:/Users/nguye/AppData/Local/Temp/eldercare-t032-clean-verify-20261029.log, eldercare-t032-scoped-final-20261029.log và eldercare-t032-spotless-20261029.log. Chưa commit/push/PR/hoàn tất merge.
