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
