# Implementation Plan: Phòng, giường và phân bổ giường

## Ghi chú triển khai Phase 1 (2026-10-22)

- Phạm vi: T001–T002, không migration, controller, lệnh nghiệp vụ hay giao diện web. Dùng spec, contracts mục 2.1 và mục 3, research G-01/G-02/G-16 làm căn cứ.
- T001 dùng tên tiếng Anh và cấu trúc mới theo backend/README.md: `api/{dto,spi}`, `internal/{structure,bed,allocation,isolation,daycare}`, `scheduling`; port giữa giường và tài sản ở `internal/bed/domain/port`. Các package-info khung được tạo theo yêu cầu tường minh của T001; lớp chức năng sẽ thêm ở phase tương ứng.
- Phụ thuộc T002 → T004: bản giả không thể biên dịch khi chưa có SPI. Khai báo trước bốn SPI đọc/lệnh gọi lại của contracts mục 2.1, `BedAssetPort` và các kiểu tối thiểu. `BedAssetStatePort` (tên logic `GiuongTheoTaiSanPort`) cùng các API công khai/lệnh khác vẫn ở Phase 2, không đánh hoàn tất T004 trong Phase 1.
- Bản giả do 003 sở hữu nằm ở `support/fake/m03`, được đăng ký bằng `FakeM03Config` (`@TestConfiguration`, `@Primary`), không đưa vào runtime chính. Test tiêm bản giả để đặt dữ liệu và gọi `reset()` giữa các kịch bản. Chưa thêm endpoint điều khiển giả lập: hợp đồng REST chưa có endpoint này và Phase 1 chỉ yêu cầu cấu hình test.
- Test trước: `M03SetupTest` kiểm khung và NamedInterface; `FakeM03Test` kiểm dữ liệu rỗng/mẫu, lọc ngày/buổi, dữ liệu lây nhiễm theo thời điểm, tham chiếu lịch sử, dấu tài sản và đăng ký bean. Lần chạy đầu lỗi biên dịch vì SPI/DTO chưa tồn tại. Không tạo bản giả thứ hai cho SPI của 001/002.
- Rà soát phát hiện luật tài sản → giường chưa chặn tài sản → phân bổ. Test hồi quy `FR_013_boundaryRejectsAssetAccessToAllocation_butAllowsTheBedPort` đã chạy lỗi trước khi mở rộng luật để bao phủ cả hai phần (research G-01). Fixture truy cập sai chỉ có trong nguồn test, không được đưa vào runtime chính.
- `spotless:apply` và `spotless:check` đã chạy thành công, chỉ áp dụng các file Java của m03 qua bộ lọc `spotlessFiles`. Lần cuối trên JDK 25: `mvnw.cmd spotless:check -DspotlessFiles=.*m03.*\.java -Dtest=M03SetupTest,FakeM03Test,M03PartBoundaryArchTest,LayerArchTest,ApiNamedInterfaceArchTest,ModuleBoundaryTest test` xanh 22 test, không lỗi/bỏ qua (14 test mới của 003, 8 kiểm tra kiến trúc có sẵn).
- Đã chạy `mvnw.cmd verify`: 137 test, 136 qua, 1 lỗi `BackendApplicationTests.contextLoads`; nguyên nhân gốc là thiếu `TEST_DB_URL` (cũng chưa có `TEST_DB_ADMIN_USER`, `TEST_DB_ADMIN_PASSWORD`). Build dừng ở Surefire trước các test tích hợp Failsafe; chưa thể xác nhận toàn bộ `verify` xanh. Không tắt/bỏ qua/sửa test đó.
- Môi trường ban đầu có JDK 24; đã tải JDK 25 và Maven wrapper vào thư mục tạm để kiểm, không đổi cấu hình Java hệ thống hoặc pom.xml. Người dùng xác nhận chưa có MySQL test hay các biến `TEST_DB_*`, nên chưa chốt phase/đánh `[x]` cho tới khi kiểm chứng đầy đủ. Thay đổi đang ở working tree trên `develop`, không tạo/đổi nhánh, commit, push hay PR.

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
