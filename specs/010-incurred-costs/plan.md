# Implementation Plan: Chi phí phát sinh

> **Sửa 2026-10-29 (Q-395):** bỏ bảng hệ số phí khi vắng; nội trú vắng tính như ngày lưu trú, từ ngày Giải phóng giường không tính phí lưu trú; bỏ dấu "chờ quyết định", "không có dòng chính sách". Spec FR-005a, FR-007, FR-009 là căn cứ; chỗ nào dưới đây còn nhắc hệ số vắng theo 004 thì theo spec.

**Branch**: `010-incurred-costs` | **Date**: 2026-10-10, cập nhật 2026-10-18 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/010-incurred-costs/spec.md`

## Summary

### Phase 9–10, 08/11/2026

- `CostSummaries` dùng chung thành phần FR-038/038a: khoản chưa hủy/chưa chốt, nhập tay và mua hộ chỉ sau duyệt; thiếu giá có ghi chú; điều chỉnh kỳ gốc đã chốt tính theo ngày tạo tại Việt Nam. Tổng cho 017 không giới hạn kỳ. `CostForBalance` trả phí ngày cơ sở theo hợp đồng tháng và chi tiết bảng đã chốt với tổng lưu sẵn, kỳ gốc, không tên thuốc.
- `FamilyPortalCosts` kiểm quyền chi phí theo người cao tuổi. Chỉ khi quyền nhóm MEDICATION là FULL mới đọc `PrescriptionApi.history`; nối tên bằng medicationId trong chỉ mục nguồn của 010, không suy từ mô tả. Chưa nối được thì chỉ hiện mã. Không trả lịch sử nội bộ hoặc điều chỉnh chưa chốt.
- `CostAccountingExport`: ALL/NEVER_EXPORTED, CSV UTF-8 BOM/XLSX, tổng kiểm soát, danh sách người có bảng chưa chốt; khóa kỳ và ghi nhật ký xuất/từng bảng/first_export_id cùng transaction. Snapshot dòng của lần ALL cùng tập bảng giữ tên và nhãn khi xuất lại; nhật ký được đọc theo trang. Không tạo bảng/migration xuất riêng theo R-15. Ô CSV dạng công thức được xử lý như văn bản; số tiền âm giữ kiểu số.
- Ba HTTP operation giữ hợp đồng đã duyệt. `AccountingExport` nằm trong màn Chi phí của Kế toán; `FamilyCosts` và `FamilyCostsScreen` nhận residentId để A nối vào cổng 012. Dùng component/theme có sẵn; không sửa khung A hoặc import chéo feature. Port nội bộ E thêm `finalizedStatement`, fake chung cập nhật tương ứng; xem contracts/module-interfaces.
- T032: bỏ ba operation đã cài khỏi pending Contract 010, không nới assertion. T033: SPI `RetentionProvider.referencing` của 000 T112 đã có, nên E nối M01.RESIDENT → bảng/khoản và phân trang ứng viên ngay trong provider của mình. PurchaseRetention đã nối từ Phase 8. Dọn cost_source_index vẫn cần hợp đồng định danh loại/ID nguồn; không băm ID, tự đổi sự kiện hoặc sửa bảng private của module khác.
- Gate còn lại của toàn feature: nhập tay/danh mục/nguồn thuốc-bán trú từ phase trước; fixture tích hợp B/D, chuỗi retention thật/chỉ mục nguồn và 017; điều hướng cổng do A giữ; quickstart/đo SC. Không coi test provider dùng hồ sơ giả là nghiệm thu toàn chuỗi. Không chạy verify theo yêu cầu, không commit/push.
- Ghi chú/log chi tiết nằm trong `.local/phase9-10/` đã gitignore; không thêm file phụ trong spec. Kết quả kiểm cuối phiên được ghi ở docs/progress.md.

**09/11/2026:** người dùng yêu cầu chạy lại toàn bộ, gồm `clean verify -Dit.forkCount=3`; yêu cầu này thay ngoại lệ bỏ verify của ngày 08/11. Chỉ hướng dẫn merge develop/push, chưa tự thực hiện các thao tác Git đó.

Rà truy vết T035 (tên lớp thực tế theo README, không tạo lớp trùng tên logic trong tasks):

| Quy tắc | Bằng chứng / phần còn mở |
| --- | --- |
| DBR-15, BR-M11-01 | CostSchemaIT, CostLockIT, BillableSourceIndexIT, BillingSourceLookupIT; nguồn thật thuốc/bán trú còn T011–T014/T021 |
| DBR-16, BR-M11-02/03/09, BR-M02-06 | CostPricingServiceTest, CostPricingTransactionIT, ResidencyDayFeeTest, DayCareSessionFeeTest, SourceCostPricingTest; adapter bán trú còn thiếu nguồn |
| DBR-17, BR-M11-06 | CostWorkflowIT, CostSchemaIT, CostRecalculationIT; không sửa bản ghi đã chốt |
| DBR-23 | CostWorkflowIT, PurchaseAssistanceIT.DBR_23_auditFailureRollsBackCreation, CostSharedRowsIT |
| BR-M11-04 | T025 còn API đọc danh mục kể cả ngừng hiệu lực; chưa có kiểm chứng nhập tay đầy đủ, không coi đã xong |
| BR-M11-05, FR-028 | CostRecalculationIT, CostRecalculationServiceTest, CostAdjustmentServiceTest; nguyên bảng chốt, điều chỉnh thuộc bảng đích và giữ kỳ gốc |
| BR-M11-07, BR-M10-10 | PeriodCostChangeTest, PurchaseExcessCheckTest, PurchaseConsentTest, PurchaseAssistanceIT; ngoại lệ thuốc/vật tư y tế cần nguồn T027 |
| BR-M11-08 | FinalPeriodConditionTest, FinalPeriodIT, FinalPeriodStatusIT; fixture B và chuỗi thật ở T033 |
| FR-037/038/038a/039/041/042 | ProvisionalCostTotalTest, CostSummariesTest, FamilyPortalCostsTest, CostProvisionIT; test web FamilyCosts và test mobile family_costs_screen |
| FR-043/044 | CostGovernanceIT, CostWorkflowIT, CostAdjustmentServiceTest, FamilyPortalCostsTest, CostApiContractsTest, PurchaseAssistanceIT; producer nguồn còn theo task tương ứng |

Đây là kết quả rà và xác định khoảng trống, không phải khẳng định mọi BR/FR đã có nghiệm thu đầy đủ. T035 giữ mở cùng các nhánh nhập tay/nguồn chưa cài.

**Cập nhật triển khai 07/11/2026 — T011–T014, T020–T022:** đọc [note triển khai và đề xuất nguồn](plan.md#implementation-notes). Lượt này thêm mã lưu trú/chọn nguồn/tính lại/điều chỉnh trong phạm vi E; không verify/test và không nghiệm thu phase. Phân rã kỹ thuật giữ nguồn thiếu metadata ở trạng thái từ chối có lý do; bán trú và consumer điều chỉnh thuốc chưa đủ hợp đồng để hoàn tất. Dùng bảng chung hiện có cho biến động, không tạo migration `V010_02__bien_dong.sql` đã bị R-15 thay thế. Giữ nguyên các gate nguồn thật/retention/HTTP nêu trong note.

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
- Kiểm tài liệu: parser YAML không trùng khóa; so cấu trúc trước/sau bằng ánh xạ tên; tham số path khớp; 101 ref đều có đích. Chưa chạy Maven/npm/Flutter vì chưa sửa mã/client. Bằng chứng và gate còn chờ ở plan.md#implementation-notes; log/script kiểm ignored tại backend/target/010-preparation của checkout gốc. Progress chỉ thêm, file CRLF UTF-8.

## T001 và phần độc lập T006 — sau duyệt 2026-10-29

- Phạm vi: M11ArchTest và ba hàm thuần DongRounding/ResidencyDayFee/DayCareSessionFee trong pricing.internal.domain; không có SQL, job, HTTP, API công khai/fake hoặc mã web/mobile mới. Root M11 đã có @ApplicationModule; chỉ tạo gói có lớp theo README. T001 xong kiểm khung/rule, không coi phase nền hoặc rule chưa có consumer là nghiệm thu tích hợp.
- Lượt trước pull trên da9636a: ba test tính phí và M11ArchTest được viết trước. Lệnh đầu thất bại ở testCompile vì ba lớp domain chưa tồn tại; sau cài, 46/46 test xanh (12 làm tròn, 20 phí ngày, 10 phí buổi, 4 kiến trúc). Không nói 46 test đã chạy đỏ ở lượt chưa biên dịch được.
- Công thức hiện tại theo FR-009/013 và Q-395: BigDecimal HALF_UP tới số nguyên đồng; nội trú dùng đủ giá ngày cơ sở, không nhận hệ số giảm khi vắng; bán trú làm tròn giá cơ sở trước khi nhân tỷ lệ phần trăm từ nguồn. Chỉ ngày cuối tháng/buổi có lịch cuối nhận phần dư. Đơn giá tại ngày, số buổi sau lọc khu nghỉ và buổi cuối do nguồn cung cấp; helper không truy cập DB/API, không tự đếm buổi hoặc lập khoản. Test kiểm 12 tháng 2026 và tháng 2 nhuận 2028, giá thay đổi qua phụ lục, kỳ lưu trú ngắn, hệ số 0/30/100 và giá trực tiếp.
- DayCareSessionFee dùng scheduledSessionCount/lastScheduledSession thay DTO của 004 còn thiếu. Đây là đầu vào toán học tối thiểu cho phần hàm thuần của P-05, không tạo bản giả hoặc khẳng định đã kiểm lọc dữ liệu thật. Thiếu số buổi dương thì không chia giá tháng. Chưa cài BillingPeriod vì còn chờ kiểu hợp đồng/hồ sơ kết thúc của B/004; T006 giữ [ ]. Các quy tắc FR-011/Q-22/Q-26/Q-27/Q-141 không bị đoán lại hoặc coi đã kiểm.
- P-12/tasks làm rõ hai chiều port thay câu gộp cũ: CostForBalancePort do 010 cài, balance/procurement gọi; PurchaseBalancePort do 017 cài, purchase gọi. M11ArchTest kiểm gọi hàm qua tên port trong M11; rule job cấm createItem/createStatement/createAdjustment/createPurchaseAssistanceRequest, không cấm tạo kỳ theo bảng trạng thái đã chốt. Chưa có consumer/port/job thật nên không đánh nghiệm thu luồng.
- Spotless giới hạn Java M11 đã qua. Clean verify toàn kho hoàn tất 09:48 sau nạp .env/backend/.env từ checkout gốc, giữ DB_PASSWORD/DB_RETENTION_PASSWORD: 683 unit/arch xanh (M11 46), 1426 IT còn 7 thất bại/9 lỗi ở đúng 9 lớp M01/M03 đã ghi team-plan, không có lớp đỏ mới hoặc test bỏ qua; Spotless check 2013 file sạch. Không đổi test/source của B/C/D/A để làm xanh baseline. Chi tiết plan.md#implementation-notes.

- Sau khi hợp đồng được duyệt, đã thử npm run gen:api để đồng bộ client. Generator của A lọc TRANSLATED ở frontend/scripts/gen-api.mjs dòng 9, chưa có 010 nên chỉ sinh lại 8 feature cũ và không có diff frontend sau giữ CRLF. Ghi A thêm 010 ở team-plan; plan.md#implementation-notes mục 5 có đúng dòng cần đổi và cách kiểm. Chưa sửa file A/viết tay client, không có thay đổi web nên chưa chạy npm test/lint/build.

## Đồng bộ develop e7b2b71 và Q-394 → Q-397 — 2026-10-29

- Theo yêu cầu người dùng, đã pull --ff-only từ da9636a lên e7b2b71 sau sao lưu/kiểm SHA-256 18 tệp đang làm; giữ công việc 010, progress chỉ thêm, không đổi worktree 008. Chi tiết ghép bảy tài liệu và ảnh hưởng tới 010/012/015/008 ở plan.md#implementation-notes.
- Test Q-395 viết trước, lượt đỏ testCompile do chữ ký cũ còn yêu cầu hệ số; sau sửa, 49 test M11 xanh. ResidencyDayFee.monthly/direct không nhận hệ số; bán trú giữ tỷ lệ nguồn (thêm ca 0/50/100%). Không có thay đổi API công khai hoặc hợp đồng HTTP đã duyệt.
- clean verify nền đã nạp .env/backend/.env và giữ mật khẩu demo; hoàn tất 08:03: 689 unit/arch xanh (M11 49), 1426 IT còn 7 thất bại/9 lỗi, 0 bỏ qua, đúng 9 lớp M01/M03 đã có trong team-plan và lượt trước pull; không có lớp lỗi mới. Spotless check 2009 file sạch. Maven trả BUILD FAILURE do các lỗi nền này, không coi toàn kho xanh.
- T006 vẫn [ ] (3/4): BillingPeriod và kiểm ngày lưu trú cần API/kiểu dữ liệu B/004, gồm ngày Giải phóng giường để chặn từ chính ngày đó; helper không quyết định ngày được tính phí. T003/T004/T005/T007/T008 vẫn là phần E còn làm; các đoạn phụ thuộc nguồn mới chờ B/C/D, client chờ A đăng ký 010. Team-plan giữ phần còn dở, bỏ yêu cầu trigger bản tin đã hủy và đổi tên thống kê 015 theo Q-397. Một số câu chữ cũ trong spec được ghi để báo lại, chưa tự sửa spec.

## T003/T004 — khai báo hợp đồng Java và fake chung 2026-10-29

- Viết CostApiContractsTest trước: lượt đỏ testCompile do API/port chưa có; thêm CostSharedFakesTest/CostViewSchemaTest trước fake/DTO, lượt đỏ tiếp theo cũng testCompile. Sau khai báo và fake: 71 test M11 xanh (22 ca mới + 49 ca trước). Không coi đây là test nghiệp vụ producer hoặc quyền thật.
- Ba API ở billing.api, 11 DTO/ba enum ở api.dto, tám sự kiện ở api.events, mỗi gói có NamedInterface. DTO đọc đối chiếu trường từ YAML đã duyệt/data-model/contract liên module; object chưa rõ giữ Map/List<Map>, không bổ sung HTTP schema. Chữ ký tiếng Anh ghi contracts/module-interfaces mục 4, glossary chỉ mục 16.6. Không sửa source/API của chủ khác.
- Đường dẫn logic port chuyển thành provision/internal/domain/CostForBalancePort và purchase/internal/domain/PurchaseBalancePort theo tầng README; chỉ công khai Java trong nội bộ M11, không đưa vào api. Hai chiều truy cập giữ đúng P-12/M11ArchTest. bangDaChot để T036/T037; hành vi thiếu bean để consumer T027 kiểm.
- Chủ E cung cấp năm fake qua FakeM11Config ở support/fake/m11, chỉ TestConfiguration. Fixture thống kê đặt sẵn tập dòng theo từng ScopeFilter, không đoán phạm vi từ nguồn B/C; fake kỳ cuối/tạm tính bắt test cấu hình kết quả rõ. T002 vẫn dở: fixture nguồn E chưa làm đủ, người đại diện chờ B/004 khai báo; không dùng fake production.
- T003/T004 [x] theo phạm vi khai báo và test xanh, chưa đóng Phase 1/2. Các API/port mới chưa có bản thật; cả tám sự kiện chưa có producer. D/011, C/014, E/012 phát sự kiện nguồn khi cài; E tiếp tục T005/T007/T008, BillingPeriod và các task nghiệp vụ. Dòng team-plan giữ phần chưa merge/bản thật/phát nguồn, có dòng review hợp đồng mới; người dùng tự commit/push/PR.
- Spotless M11 đã qua. Clean verify nền sau nạp .env/backend/.env, giữ mật khẩu demo, đã xong 15:49 min: 711 unit/arch xanh (M11 71), 1426 IT còn 7 thất bại/9 lỗi, 0 bỏ qua, đúng 9 lớp M01/M03 đã ghi nhận ở team-plan/lượt trước, không có lớp đỏ mới. Spotless check qua. Maven trả BUILD FAILURE do lỗi nền; chưa coi toàn backend xanh hoặc Phase 1/2 hoàn tất. Chi tiết plan.md#implementation-notes; progress chỉ thêm. Không sửa web/mobile, không chạy kiểm web/Flutter trong lượt này.

## T005a và fixture nguồn — R-15, 2026-10-30

- Đồng bộ develop e7b2b71 → 8573190 bằng fast-forward, không tạo commit/stash/chuyển nhánh; sao lưu SHA-256 59 tệp, ghép bảy tài liệu, giữ toàn bộ Java và API đang làm. Snapshot ở .worktrees/.snapshots/010-before-r15-20261030-1793294325047 của checkout gốc. Progress chỉ ghép thêm dòng hai phía, các worktree 008 không sửa.
- R-15/constitution XII 3.2.0 làm T005 cũ không còn phù hợp: thêm data-model 0.1 trước migration; giữ ba bảng riêng, phần trạng thái/lịch sử/marker qua API chung A. Thêm T005a để kiểm phần độc lập, giữ T005/T038 chưa đóng. T038 đã có sẵn trong tasks; giữ một dòng gốc, cập nhật phần đã kiểm và phần tích hợp còn chờ, chưa đánh [x].
- V010_01__costs.sql chỉ tạo cost_statement (kỳ và bảng phân kind), cost_item, cost_source_index. Unique_key do caller đặt, CHECK đối chiếu và UNIQUE ngăn né khóa; thay cách cột sinh cũ theo R-15, vẫn giữ DBR-15. FK chỉ trong M11, không FK sang nguồn/bảng chung chưa có.
- Hai bảng nghiệp vụ có locked_at, trigger giữ mọi dữ liệu đã khóa và kiểm cha cũ/mới của khoản (kể cả INSERT sau chốt); ngoại lệ first_export_id chỉ đặt một lần. @m11_recalculating dùng đúng connection, không mở khóa dữ liệu đã chốt. Retention dùng tài khoản riêng, admin cũng không được sửa dòng đã khóa. Kind/id giữ định danh dòng, backend vẫn phải kiểm nghiệp vụ chốt/tổng/nguồn và trạng thái lệnh ở task sau.
- Fixture FakeCostSources dùng ApplicationEventPublisher, đăng ký ở FakeM11Config, chỉ src/test. T002 đã xong phần fixture nguồn E và port số dư fake, còn fake người đại diện chờ B/004 khai báo. Không coi fixture là producer C/D/012 hoặc fake production.
- TDD: fixture đỏ ở testCompile do chưa có lớp; CostSchemaIT trước SQL chạy 15 ca, 1 thất bại/14 lỗi do bảng chưa có. Sau migration 14/15 qua, kiểm quyền DELETE của một ca cần đọc SQLException gốc thay thông báo bọc của Spring; sửa helper, không đổi quyền hoặc nới điều kiện. Lượt sau 74 unit/arch M11 và 15 IT schema đều xanh, 0 bỏ qua. Spotless apply M11 qua; clean verify toàn backend đã xong 05:59 min: 714 unit/arch xanh (M11 74), 1441 IT còn 7 thất bại/9 lỗi tại đúng 9 lớp nền M01/M03, 0 bỏ qua/không lớp đỏ mới; 15 IT M11 xanh. Spotless check 2051 file sạch. Maven BUILD FAILURE do lỗi nền, chưa nghiệm thu toàn backend/Phase 1–2. Kết quả ở plan.md#implementation-notes.
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
- TDD và kết quả kiểm ở [plan.md#implementation-notes](plan.md#implementation-notes). Các lượt đỏ thiếu kiểu Java là lỗi testCompile, tách rõ với lượt đỏ runtime (fake, precision, governance); không gọi lỗi biên dịch là assertion nghiệp vụ đã thất bại.
- Giữ nguyên gate HTTP: bản dịch đã duyệt, nhưng 17 response 2xx thiếu schema và vài object chưa đủ cấu trúc cần chủ dự án chốt trước endpoint liên quan. Quantity/newQuantity của 005 đã chốt: xem [quyết định 05/11](research.md#care-quantity); còn C cài producer và E cài consumer, không tự quy đổi ml/phút/giá trị đo. Không sửa spec.md/nghiệp vụ hoặc API nguồn của người khác.

## Retention hiện có và quyết định quantity — 2026-11-05

- Tách T008b từ parent T008 tương tự T008a: chỉ nghiệm thu ba loại kỳ/bảng/khoản đang có dữ liệu thật. CostRetention ở persistence giữ JDBC; M11GovernanceConfig chỉ nhận store và đăng ký provider. Mốc hồ sơ qua ResidentApi công khai; xóa bằng RetentionDatabase trong transaction của job. Không đổi API, schema, CFG hoặc cơ chế bảng chung A.
- 106 unit/arch + 62 IT xanh, gồm 9 ca retention mới. Chi tiết TDD, lỗi đã sửa, phạm vi và bằng chứng ở [plan.md#implementation-notes](plan.md#implementation-notes). Không chạy full verify.
- Lệch phân rã task: parent T008 không thể đóng bằng provider rỗng trước khi có record mua hộ/chỉ mục thật; các phần này đi cùng T010/T021/T025. Phase 2 vẫn mở. Chuỗi hồ sơ thật 001 → 010 và định danh chỉ mục theo nguồn cần A/B/chủ nguồn review trước khi nghiệm thu; không tự sửa module khác.
- Quyết định quantity/newQuantity đã nhận đủ, được ghi ở [research.md#care-quantity](research.md#care-quantity). Hai dòng chờ E/C chuyển sang cài producer/consumer; không giữ lý do chờ người dùng gửi quyết định.

## Tiếp tục nền đơn giá trên nhánh riêng — 2026-11-06

- Người dùng yêu cầu tiếp tục code, chưa cần PR hoặc merge develop; chờ leader quyết định merge. Giữ nguyên nhánh/worktree hiện có, không lấy PR #112 làm điều kiện để làm phần độc lập của 010.
- Tách T006a từ phần chọn giá của T014 vào nền để tiến tiếp khi T008 còn phụ thuộc liên module và record chưa tạo. Không bỏ gate Phase 2 hoặc đánh xong T014: chỉ chọn giá và tính tiền sự kiện, chưa lập khoản, không mở API hay cài listener làm thay đổi khoản.
- CostPricingService đặt ở billing.internal.pricing.service, gọi ContractReadApi/UnitPriceApi công khai. Dịch vụ đã đăng ký/giá đã ghi theo nội dung hợp đồng dùng giá hợp đồng tại ngày phát sinh, không thay bằng giá danh mục khi giá hợp đồng thiếu. Trường services/serviceId/inPackage theo ContractInput API004 đã có; giá dùng recordedUnitPrices đã công bố. Ngoài hợp đồng mới gọi unitPriceAt với markReferenced=true trong transaction MANDATORY của caller.
- Khoản trong gói có số tiền 0 và căn cứ hợp đồng; giữ đơn giá đã ghi nếu có, không bịa đơn giá riêng nếu nguồn không có. Ngoài gói mà thiếu giá giữ unitPrice/amount NULL, không đổi thành 0. Căn cứ nội bộ giữ contractId/contentVersion/addendumId hoặc priceVersionId. Không thêm trường HTTP/Java API công khai hoặc fake của B; không suy đổi đơn vị nguồn. Adapter lưu trú/bán trú và listener tính lại/thông báo của T014 vẫn còn.
- T006a hoàn tất: 12 unit mới đỏ runtime với khung chưa cài (2 assertion failure/10 error); 2 IT mới có 1 control transaction xanh và 1 error do chưa cài. Sau cài: 148 unit/arch (30 lớp), 61 IT billing/hợp đồng/nhóm dữ liệu (9 lớp) và 24 IT nguồn 004 (3 lớp), tất cả xanh, 0 skip; XML mới được đối chiếu, không lấy Maven exit 0 thay kết quả Failsafe. Spotless đúng ba file Java. Không chạy full verify, không sửa nguồn/test B hoặc common. Chi tiết ở [plan.md#implementation-notes](plan.md#implementation-notes).

## Rà T008 và sửa thứ tự xóa — 2026-11-06

- Nền 18e34176; giữ nguyên file plan.md#implementation-notes mà người dùng đang sửa (đã sao lưu trước). Không đổi nhánh hoặc đồng bộ/merge.
- TDD: thêm FR_051_batchDeletionPreservesTheCallersChildBeforeOriginalOrder vào CostRetentionIT; MySQL tái hiện 1 error ở fk_cost_item_original khi caller truyền khoản điều chỉnh trước gốc nhưng provider lại sort tăng ID. Sửa tối thiểu bỏ sorted(), vẫn distinct và giữ thứ tự caller; chỉ xóa các ID được truyền, không xóa bảng hoặc khoản không liên quan. Job nền hiện gọi từng ID theo thứ tự ngược chuỗi; không đổi job hoặc tự dựng thứ tự xóa riêng.
- Sau sửa: 148 unit/arch (30 lớp) + 72 IT (10 lớp) xanh, 0 failure/error/skip; CostRetentionIT 10/10, có ExpiredDataPurgeIT/ApiContractIT/RecordGroupTriggerIT. Spotless đúng hai file. Không chạy full verify vì chỉ sửa provider/test của E; không lấy lượt xanh này làm bằng chứng đã nối hồ sơ thật 001 → 010. Raw log/XML mỗi lượt giữ riêng theo .worktrees/.snapshots/010-retention-followup-{red,green}-latest-path.txt.
- Đã đối chiếu: SPI hiện tại chỉ có referencesTo(long) cho loại của provider; job chỉ hỏi provider của target, chưa có điểm nối để E báo khi target là M01.RESIDENT. Bản [đề xuất mở rộng](contracts/proposal-retention-cross-module-2026-11-06.md) giữ hàm cũ, thêm overload DataRef mặc định và hỏi các provider qua overload; E chỉ báo các bảng chi phí của hồ sơ, khoản đi theo bảng. Chưa sửa API/job/test/hợp đồng A hoặc mã B, người dùng đã chốt giữ E và bàn giao A review/cài. Các task 000 T070/T071 gốc đã hoàn tất; không ghi A chưa xong task gốc.
- Chỉ mục nguồn có khóa bộ ba còn cần cơ chế dọn theo nguồn và hợp đồng ánh xạ loại/ID, không tự băm ID hoặc đổi source event. Record/quyền tệp mua hộ vẫn đi cùng T025–T027. T008 và Phase 2 giữ mở; 11/42 task, không thêm task hình thức hoặc đánh xong toàn feature.

## Phân rã lại provider theo dữ liệu thực có — 2026-11-06

- Rà task gốc T008 và tiền lệ 012 plan (T008 chỉ cài provider cho hai bảng đã có, provider đối tượng chưa có bảng đi cùng task tạo dữ liệu). Trước đó E đã gộp kiểm toàn chuỗi và provider mua hộ ở phase sau vào gate nền, tạo phụ thuộc vòng. Điều chỉnh kỹ thuật theo AGENTS.md mục 2.9; không đổi nghiệp vụ/spec, hạn giữ, quyền hoặc hợp đồng API. Các ghi chú Phase 2 mở phía trên là trạng thái tại thời điểm ghi.
- T008 chỉ nghiệm thu FieldClassificationProvider/ScopeAssignmentProvider và ba RetentionProvider cho kỳ/bảng/khoản hiện có, gồm T008a/T008b. Không tạo provider rỗng hoặc CorrectableRecordProvider. Metadata/phạm vi/retention của record mua hộ thuộc T026; quyền tệp chứng từ thuộc T027, có test cùng task tạo record/tệp theo R-15.
- Toàn chuỗi hồ sơ thật 001 → 010, phần đính kèm bảng chung, kiểm liên kết 017 khi có bản thật và dọn cost_source_index theo nguồn là tiêu chí bắt buộc của T033, trước hoàn tất feature/merge develop/vận hành. SPI hiện chưa cho E nối tham chiếu M01; đề xuất đã bàn giao A theo lựa chọn người dùng. T033 không thể đánh xong bằng các test FakeM01Config hiện có. Không sửa module/API/test của A/B/C/D; không tự đặt ánh xạ khóa chỉ mục nguồn.
- Chạy một lượt clean verify toàn backend để nghiệm thu nền có provider/bean tham gia cơ chế chung. T008 chỉ đánh [x] khi kết quả kiểm cuối xanh; kết quả và giới hạn sẽ ghi ở plan.md#implementation-notes.

## Phase 3 độc lập, giữ nghiệm thu nền mở — 2026-11-06

- Lượt full cuối đặt DEBUG=false/TRACE=false để tránh DEBUG kế thừa từ công cụ bật log SQL của Spring Boot 4.1.1; giữ pool test 20/cache 2 qua môi trường tiến trình. Không sửa cấu hình kho hoặc test A/B. JVM/runner không còn khi kiểm lại; lưu XML mới và ghi nhận bị gián đoạn, không tự gán Maven exit 0.
- Kết quả dở: 1099 unit/arch xanh; 1411 IT có 1 error ở FR_046_DuplicateAttributeIT của 004/T043 (hủy yêu cầu, phụ lục vẫn PENDING_EFFECTIVE sau 10 giây). Các lớp billing đã có 157 ca/17 lớp xanh. Chưa coi full verify hoặc Phase 2 đã qua; T008 giữ mở. Không kết luận lỗi đang đỏ trên develop chỉ vì source/test không khác mốc 2fd4cd8c.
- Người dùng đã chốt: “Giữ E, bàn giao lỗi cho B; cho làm Phase 3 độc lập trong khi giữ nghiệm thu Phase 2 mở.” Đây là cho phép tiếp tục tiến độ, không bỏ test, không đổi nghiệp vụ hoặc bỏ gate cuối feature/PR. Không sửa 004. Bản ghi cụ thể: contracts/004-cancellation-observation-2026-11-06.md; kết quả cuối của từng phần sẽ cập nhật theo lượt test riêng.
- Tiếp tục T009/T010 bằng TDD nguồn tính phí trong billing, sau đó T011/T012 theo hợp đồng. Chỉ chạy Maven mới sau khi lượt cũ đã kết thúc/dừng, không dùng đồng thời target/schema test. Luôn tách DEBUG/TRACE của công cụ khỏi môi trường JVM test. Chưa commit/push/PR/merge/đổi nhánh.

## T010 — Chỉ mục nguồn và độ chính xác số lượng, 06/11/2026

- Cài trong billing.internal.source: IndexedBillingSource (domain thuần Java), BillingSourceIndexStore (JDBC), BillingSourceIndex (transaction MANDATORY, dấu và insert cùng transaction), BillingSourceEvents (ba listener sau commit cho generic, care, medication). Không sửa API/producer C/D hoặc tạo fake mới; fixture generic dùng FakeCostSources sẵn có. Không tự lập bảng/khoản/điều chỉnh, không chép ngày lưu trú/buổi bán trú vào chỉ mục.
- Generic giữ sourceId/unit/ngày đã gửi. Care lấy resultId, lượng thực và performedAt (ngày Việt Nam), không lấy giờ phát lại. Thuốc giữ source và sourceId, dùng medicationId làm khóa đơn vị kỹ thuật để không gom hai thuốc khác nhau. Đơn vị hiển thị và quy đổi số lượng không được suy từ khóa kỹ thuật. Nguồn thật của C và nhánh DISPENSE_RECORD của D vẫn cần nghiệm thu cùng chủ nguồn ở T033.
- M11.SOURCE_PROCESSED/M11.BILLING_SOURCE gắn residentId theo nền T005; marker SHA-256 của loại/id/đơn vị/eventKey để khóa dài không bị cắt. ID/khóa chính nguồn giữ nguyên văn; đây không phải cách băm ID để lách DataRef long. Insert chỉ lần đầu: duplicate PK không ghi đè nguồn đã đính chính/hủy; các lỗi SQL khác truyền ra để rollback dấu. Không cài CostSourceChanged/care correction/medication adjustment hoặc tính lại ở T021.
- TDD: 13 IT đầu có 12 timeout do chưa có chỉ mục và 1 control rollback xanh; ca hai thuốc cùng nguồn đỏ riêng. Sau cài có 4 lỗi fixture do Connector/J trả LocalDateTime thay Timestamp; sửa cách đọc UTC, giữ nguyên kỳ vọng thời gian.
- TDD độ chính xác: 3 ca mới đỏ thật (2 lần 0.125 bị lưu 0.13 tại nguồn/khoản, 1 lỗi validation lịch sử); guard quantity có 2 assertion failure/3 control xanh. V010_03__source_quantity_precision mở cả hai cột lượng lên DECIMAL(12,3), @Digits lịch sử là 9,3. Đổi ca biên âm của test theo đúng biên mới (4 chữ số lẻ hoặc 10 chữ số phần nguyên vẫn bị từ chối), giữ ca kiểm tiền/hệ số. Không sửa migration đã merge; số 04 dành dự kiến cho T026 nếu cần.
- Chỉ mở rộng khả năng lưu của E để giữ lượng 006 đang phát, không thay đơn vị, không làm tròn lượng; tiền VND vẫn làm tròn theo FR-013. Data-model 006 ghi 10,2 nhưng implementation có 12,3: cần D đối chiếu tài liệu/producer, không sửa phần D. Chưa đánh T010 xong trước kết quả kiểm cuối; không đóng Phase 2 hoặc toàn Phase 3 bằng phần này.

## Kết quả T010 và tiếp tục T011/T012 — 2026-11-06

- T010 đã kiểm xanh trong lượt cuối 164 unit/arch + 97 IT, 0 failure/error/skip, Maven exit 0 và Spotless qua. Đánh xong riêng T010; 12/42 task. T008/Phase 2 vẫn mở, Phase 3 còn T009/T011/T012. Không dùng checkpoint này thay full verify đang có lỗi 004.
- T011 cài thêm IndexedSourceMatch và BillingSourceIndexStore.find: scoped resident/type/inclusive dates, LEFT JOIN khoản EVENT_BASED chưa hủy đúng bộ khóa và người, thứ tự ổn định không ưu tiên nguồn chưa có khoản. Giữ cancelled/version hiện hành. BillingSourceSelection cài chọn theo sourceUnits hoặc sourceIds duy nhất, trả skipped theo quyết định đã duyệt; query IN chỉ dùng tham số, danh sách rỗng không mở rộng toàn nguồn. Không gọi bảng private C/D hoặc sinh record khi tra cứu; chưa cài nguồn tính ngày lưu trú/bán trú, định giá và lệnh tạo khoản.
- Chủ dự án duyệt proposal-source-unit-selection-2026-11-06.md. Đổi api của E: YAML thêm sourceUnits/billingUnit và điều kiện chọn một kiểu; BillingSourceView thêm billingUnit, không có nơi tạo record cũ cần sửa trong kho. Test hợp đồng đỏ trước/xanh sau. Chưa sửa script client chung của A, chưa gọi API trực tiếp ngoài client sinh.
- Metadata dùng/loại dịch vụ/vật phẩm và đơn vị thuốc chưa có qua API công khai phù hợp, B cần đối chiếu cổng metadata dịch vụ; đã ghi bàn giao ở contracts/source-metadata-handoff-2026-11-06.md và team-plan. Không đoán đơn vị hoặc lập khoản thiếu căn cứ; T011/T012 giữ mở. Chi tiết kiểm và môi trường ở plan.md#implementation-notes.

### Checkpoint chung theo yêu cầu người dùng — 2026-11-06

Người dùng yêu cầu tạm dừng triển khai và lưu toàn bộ hiện trạng lên develop cùng 008. Giữ test T011/T012 đang đỏ trước cài đặt, không triển khai thêm hoặc đánh xong task để đổi kết quả checkpoint. Full verify dừng ở 16 failure của CostSourceRequestTest và 5 error của ResidencyBillingSourcesTest; báo cáo ở ../008-staff-shift-handover/publish-checkpoint-2026-11-06.md. T008/Phase 2 và nghiệm thu toàn chuỗi vẫn mở.

## Phase 5 — triển khai độc lập ngày 2026-11-07

- Người dùng yêu cầu làm Phase 5, giao quyền chọn cách triển khai và nhánh, cấm chạy `verify`. Chọn 010 vì Phase 5 backend/web của 008 đã xong; nhánh `impl/010-phase5` từ develop `d9a62fc5`. Không đóng các gate T008/Phase 2, T009/T011/T012 hoặc T013/T014 bằng phần này; không sửa test đang đỏ của Phase 3.
- T015–T019 đặt tại `billing.internal.{item,period,purchase,governance}.{domain,persistence,service}` và `billing.web`, theo README. Dùng CostLock, bảng riêng và các API nguồn đã có, không tạo fake mới; bổ sung mô tả kỹ thuật của hợp đồng 010 như ghi bên dưới. `PeriodCostChange` tính bằng BigDecimal, so ngưỡng nghiêm ngặt; `PurchaseExcessCheck` chỉ kiểm biên, không thay luồng mua hộ T027.
- T017 dùng một transaction cho cả lô kiểm tra/duyệt và cho chốt. Khóa người → kỳ → bảng → khoản, thứ tự id tăng. Sau khi chờ khóa dùng locking read, kể cả khi caller đã có snapshot cũ ở REPEATABLE READ. Test hai transaction đã tái hiện lỗi bỏ sót khoản mới trước sửa. Chốt lưu khoản/tổng theo loại/tổng bảng, cập nhật kỳ nếu đủ bảng, ghi nhật ký và phát CostStatementFinalized cùng transaction; không ghi sổ số dư hoặc chứng từ của 017.
- Lệch kỹ thuật có chủ đích cho FR-004/033: thêm migration mới `V010_04__draft_statement_version.sql` (số 04 trước đây mới dự kiến cho T026, chưa có file) để khoản mới tăng phiên bản bảng và mở lại bảng khi còn Nháp. Giữ khóa cha/trigger đã merge, không sửa V010_01. Lệnh lập khoản T011 vẫn phải phát thông báo mở lại bảng và dùng cùng khóa; migration không thay nghiệm thu luồng lập nguồn còn thiếu.
- CostDailyTask đăng ký vào khung T007: kỳ theo tháng, CFG-M11-01 khi hết tháng, gỡ dấu lập trước ở khoản chưa khóa, nhắc thiếu bảng và tới hạn. Không tạo bảng/khoản/điều chỉnh. Chưa lấy test đơn vị tác vụ thay cho nghiệm thu job với nguồn lưu trú/bán trú thật, hoặc tính lại giá trị nguồn của T014/T021.
- T019 hoàn tất endpoint kỳ theo CostPeriodView đã công bố, bổ sung commandHistory (operation, actorId, at, reason tùy chọn) cho FR-018; lưu lịch sử qua StatusHistoryApi, cùng transaction lệnh, không quét audit toàn viện. Người dùng giao tự chọn cách hợp lý theo spec: chọn mô tả các trường kỹ thuật còn thiếu trong hợp đồng 010, không thêm quy tắc nghiệp vụ. Tổng kỳ trước bằng 0 vẫn cảnh báo, bỏ increaseRatio vốn tùy chọn, giữ topThreeIncreases; không thêm previousTotal/currentTotal. T017/T019 đã cài và kiểm theo phạm vi phase.
- Lượt hồi quy phát hiện ApiContractIT chưa đăng ký 010 nên báo endpoint mới ngoài hợp đồng. Kéo phần đăng ký của T032 lên Phase 5 theo chính phạm vi task; chỉ thêm Contract 010, pending cho 17 operation chưa cài, bật checkOperations=true, giữ nguyên mọi assertion và hợp đồng khác. A review phần đăng ký trong PR; không sửa mã common production hoặc coi test chưa cài là đã xong.
- Chi tiết kết quả, phạm vi và việc còn lại ghi tại `plan.md#implementation-notes`; không chạy lifecycle/goal `verify`, không commit/push/merge.

- Web tại frontend/src/features/charges: chỉ dùng client sinh, đăng ký riêng 010 vào TRANSLATED và chạy gen:api; A review dòng đăng ký và test chung trong PR. Màn chọn kỳ, bộ lọc, xem khoản/lịch sử, kiểm tra/duyệt lô, gửi/trả/chốt có xác nhận. Không chọn sẵn khoản; If-Match lấy từ GET; 412 giữ nội dung và bắt tải lại; 403 xóa dữ liệu màn; khóa khi gửi để tránh lặp. Không sửa common production hoặc dependency.

### Tiếp tục T011–T014/T020–T022 — 07/11/2026

Sửa validation ở biên HTTP, dùng DTO phản hồi sẵn có để bỏ null tùy chọn, tính lại bằng khoản đọc dưới khóa, điều chỉnh được khoản bổ sung muộn theo FR-026 và tải lại dữ liệu màn sau lệnh. Không đổi hợp đồng/API B/C/D; schema GET biến động còn là đề xuất chờ review. Test chọn lọc 43 backend và 2 web qua; không chạy verify, IT hay nghiệm thu toàn phase. Giữ task mở vì còn nguồn thật và các ca tích hợp. Căn cứ, lệnh và bàn giao tại plan.md#implementation-notes mục 5.

### Phase 6 — T020 tích hợp MySQL, 07/11/2026

Gom ba nhóm IT logic của T020 vào CostRecalculationIT (8 ca), dùng persistence/lịch sử/biến động thật và cô lập giá ở CostPricingService của billing. Thêm MANDATORY ở CostRecalculationStore.recalculate sau test đỏ ngoài transaction để bảo đảm biến phiên và UPDATE cùng kết nối. Lượt cuối 8 IT + 8 unit liên quan xanh, chỉ Maven test chọn lọc, không verify. T020 [x], T021/T022 còn mở theo plan.md#implementation-notes mục 6; không sửa nguồn B/C/D hay xem test cô lập là nghiệm thu producer.

### Phase 6 — hoàn thiện phần E độc lập, 07/11/2026

T022 dùng CostSourceChangeView (domain) và PostFinalizationChangeBody (web) cho đúng tám trường đã trả; mô tả schema GET biến động và enum/mã HTTP theo hợp đồng, không thêm đường dẫn hay trường chênh lệch. Không tạo V010_02/bảng riêng theo R-15. Xóa bốn operation 010 đã cài khỏi pending của ApiContractIT, giữ mọi assertion và hợp đồng khác; đây là phần đăng ký T032 đã kéo lên Phase 5, A review trong PR. Màn biến động sửa type submit và hiện snapshot nguồn trước/sau (gồm giải phóng giường). CostRecalculationIT mở lên 15 ca: thêm HTTP/idempotency/quyền, bảng bổ sung, đồng thời tổng không âm, trạng thái mới sau khi chờ chốt và consumer publication thật trước/sau chốt; không thay producer B/C/D. Lượt cuối 15 IT + ApiContractIT xanh, 15 ca kiến trúc trước đó xanh; web 12 test, lint phần sửa và build native qua. T020/T022 [x], T021 giữ [ ] cho nguồn thiếu và thứ tự đính chính; không verify/commit/push. Chi tiết và đề xuất đã áp dụng: plan.md#implementation-notes.

<a id="implementation-notes"></a>

## Ghi chú triển khai

### Quy ước theo yêu cầu chủ dự án — 07/11/2026

Trong phần E sở hữu, tự đối chiếu spec/nghiệp vụ, ghi phương án khớp mã thực tế và triển khai; không dừng để hỏi lựa chọn kỹ thuật không ảnh hưởng người khác. Giữ hợp đồng API, quyền sở hữu module và các gate tích hợp. Ghi quyết định bền vững vào plan/research/data-model/contracts có sẵn. Checkpoint, log và script tạm ở `.local/010-notes/` hoặc `backend/target/`, đều được Git bỏ qua. Không tạo thêm báo cáo theo ngày ở gốc spec. Mười lăm báo cáo cũ đã chuyển ra bản lưu cục bộ; quyết định lượng 005 → 010 giữ công khai tại research P-16. Nhật ký cũ trong docs/progress.md chỉ được thêm, không sửa các dòng lịch sử.

### Phase 7 — kỳ cuối, 07/11/2026

- `FinalPeriodService` cài FinalPeriodPort (ba lệnh MANDATORY), FinalPeriodCostApi và PostDeathItemPort đúng chữ ký đã công bố. Không thêm REST, migration hoặc API nguồn. Đường dẫn logic `kycuoi` đặt thực ở `billing.internal.finalperiod/{domain,persistence,service}` theo README; các lớp test logic T023 gộp thành FinalPeriodConditionTest, FinalPeriodIT, FinalPeriodSourceTest và FinalPeriodStatusIT để dùng chung fixture.
- Lệnh 004 lập/đánh dấu bảng thường làm kỳ cuối; đổi E qua tháng khác chuyển dấu của bảng chưa chốt; hủy hồ sơ bỏ mốc dừng. Khóa người → các kỳ tăng dần → bảng/khoản. Lỗi audit rollback cả bảng và trạng thái. Không tự lập khoản khi mở kỳ cuối hoặc chạy job.
- Bảng đã chốt giữ nguyên ngày/căn cứ/tiền. Mốc hiện hành và giờ qua đời nằm trong payload BillingResidentState sẵn có; bảng gốc vẫn cung cấp khoảng đã chốt để nhận biết AFFECTS_TERMINATION. Ngày đã lập sau E mới bị hủy nếu chưa chốt; nếu đã chốt chỉ ghi biến động/thông báo bằng cơ chế T020. Hành chính tự lập khoản điều chỉnh.
- Điều kiện thuần kiểm bảng kỳ cuối đã chốt, đủ ngày lưu trú có phí trong bảng đã chốt và không còn khoản AFFECTS_TERMINATION chưa chốt. `CostStatusParticipant` dùng mã tiếng Anh dùng chung **END_A_COSTS** (KT_A_CHI_PHI), waivable=true; @Order(60) sau nguồn và trước M10(70). Xem trước không ghi trạng thái; lệnh thật giữ khóa M11 trong cùng transaction 001. Test tích hợp 001 thật kiểm chặn, cho phép, ngoại lệ và rollback sau tác động qua đời.
- FR-035 dùng ResidencyEndingReadApi.preparing cùng ResidencyDayApi.billingStopDate hiện có để phân biệt mốc dự kiến với mốc dừng thật: chỉ mở lại ngày đã kết thúc sau E khi hồ sơ còn Đang chuẩn bị và người chưa ở trạng thái cuối. Giữ chặn từ ngày giải phóng giường. Không sửa API/mã B hoặc lấy hạn hợp đồng làm ngày kết thúc.
- Nguồn sau giờ qua đời bị chặn, ngày qua đời vẫn trọn ngày. Ngày đã lập trước sau ngày qua đời bị hủy/ghi biến động theo trạng thái. Mục FINAL_PERIOD_COSTS hoàn tất khi kỳ cuối và các bảng bổ sung hiện có đã chốt; lưu finalCostCompletedFrom để bảng mở muộn không kéo lại. CostStatementFinalized/DeathRecorded gọi PostDeathTaskApi.refresh sau commit; sự kiện hồ sơ kết thúc chỉ kiểm lại trạng thái hiện hành, không tạo lại kỳ cuối từ payload cũ.
- Chi phí phát sinh sau kỳ cuối đã chốt vào bảng bổ sung, kể cả khác tháng; ngày ngoài khoảng đã chốt mang AFFECTS_TERMINATION, khoản đến muộn trong khoảng không mang dấu đó. Job chỉ nhắc quá E và nhắc bảng bổ sung theo CFG-M02-09, marker chống lặp. Đã sửa phụ thuộc sai tầng `CalculatedCost.snapshot`: dựng snapshot ở service tính lại, giữ pricing.domain thuần Java; không nới M11ArchTest.

**Kiểm chứng và giới hạn:** test mới đã có lượt đỏ thiếu lớp/bean trước khi cài. Bộ hồi quy 76 ca từng xanh; lượt tích hợp mở rộng 77 ca có 68 xanh, **9 assertion đỏ ở fixture cũ của B**, không có error/skip: FR_045_ListConditionsIT (4, xuất hiện thêm END_A_COSTS), FR_066a_PastExpectedDateIT (1, fake kỳ cuối không đáp ứng điều kiện thật), PostDeathTaskIT (3) và RecordDeathEffectsIT (1, fake/port thật cùng mã FINAL_PERIOD_COSTS). FR_066_FinalPeriodIT 4/4, M02StatusParticipantIT 1/1, FR_047_RollbackEffectsIT 2/2, 24 ca kỳ cuối mới và kiểm kiến trúc/hợp đồng của lượt đó đều xanh. Bàn giao B chỉnh fixture/cách thay fake ở team-plan mục 8; E không sửa test của B hoặc coi những lỗi mới này là baseline cũ. Gate thay fake/nguồn thật vẫn ở T033; chưa được tuyên bố toàn bộ nghiệm thu liên module xanh. Log ở backend/target/phase7-*.log; không chạy verify, không commit/push/merge hoặc đổi nhánh.

**Kết quả cuối phần E (07/11/2026):** `phase7-final.log` có **116/116 ca qua**, 0 failure/error/skip, gồm hồi quy billing, hợp đồng HTTP và ModuleBoundaryTest/LayerArchTest/TimeArchTest/RequiresPermissionArchTest/M11ArchTest/ApiNamedInterfaceArchTest/SchedulerScopeArchTest. Rà thêm trường hợp bảng tháng đã chốt trước khi lập hồ sơ kết thúc: job quét người có bảng chi phí rồi đọc trạng thái M11 qua API để không bỏ sót trường hợp không được sửa final_period trên snapshot; bảng tháng không có through_date dùng ngày cuối tháng làm khoảng đã chốt. Sau sửa ranh giới này, `phase7-boundary-final.log` có **26/26 ca kỳ cuối qua**, 0 failure/error/skip (16 FinalPeriodIT, 4 FinalPeriodStatusIT, 3 FinalPeriodSourceTest, 3 FinalPeriodConditionTest). Spotless áp cho Java billing qua. T023/T024 đánh [x] theo test riêng; nghiệm thu liên module vẫn mở đúng 9 lỗi fixture kể trên và T033. Không chạy verify hoặc thay đổi test B.

### Phase 8 — triển khai và phần còn phụ thuộc (08/11/2026)

**Đã cài trong phạm vi E:** luồng mua hộ ngoài danh mục từ Nháp → gửi → đồng ý/từ chối → xin/rút bổ sung → hủy/ghi đã mua. Khóa dòng bảo đảm quyết định đầu tiên thắng; lưu snapshot đại diện mỗi vòng và kiểm lại quyền hiện hành. Ghi đã mua cùng transaction với khoản Nháp, chứng từ, lịch sử/audit; lưu số được phép và số thực tế, duyệt phần vượt bắt buộc lý do và thông báo đủ ba thông tin này. Dấu số dư được tính lại khi đọc đề nghị đang chờ và lúc quyết định. Nhắc theo CFG-M15-05/06; thông báo cần đồng ý cho đại diện mức Trung bình, Quản lý viện mức Nhẹ; cảnh báo Hành chính khi không còn đại diện trong snapshot có tài khoản cổng.

**Lựa chọn kỹ thuật:** dùng bảng chung theo R-15, không tạo migration V010_04 đã được dùng. Chứng từ gắn vào `PurchaseReceipt`, sự kiện con `M11.PURCHASE_ASSISTANCE_RECEIPT`; ownerId là id sự kiện để framework thấy tệp khi duyệt chuỗi. Provider phạm vi/phân loại/lưu giữ và quyền tải thuộc billing. Dùng API đại diện hiện có và `FamilyNotificationPort.activeRelationships`; không đọc bảng nội bộ của A/B/D hoặc tạo fake production. Không đổi chữ ký API công khai/HTTP; DTO đã được sửa cho đúng trường bắt buộc và enum trong YAML. ApiContractIT bỏ đúng 9 endpoint mua hộ đã cài khỏi pending, giữ nguyên assertions (A review cùng đăng ký 010 trước đó).

**Web/mobile:** web chọn và tải chứng từ trực tiếp qua helper chung; thử lại giữ mã tệp và Idempotency-Key, 409 tải lại và báo đề nghị đã đổi, 403 xóa dữ liệu riêng tư. Mobile có màn đồng ý/từ chối độc lập, xem lại số cũ/số xin bổ sung và lý do; không báo thành công khi xung đột. Chưa nối màn gốc/cổng của A; đầu việc ở team-plan mục 8.

**Kiểm chứng cuối:** bộ hồi quy rộng billing + kiến trúc có 375/376 ca qua; ca đỏ M13ArchTest phát hiện listener kỳ cuối của E nghe sự kiện mang yêu cầu thông báo. Đã chuyển FinalPeriodEvents sang CostItemChanged có sẵn (lệnh chốt đã phát sự kiện này), không sửa test M13 hoặc API. Lượt sau sửa `phase8-final-resumed.log` có **114/114 ca qua**, 0 failure/error/skip: 14 PurchaseAssistanceIT, máy trạng thái/ngưỡng, CostWorkflowIT, FinalPeriodIT/FinalPeriodStatusIT, ApiContractIT, ModuleBoundaryTest và các ArchTest. Lượt full billing cuối trước đó bị ngắt khi chưa có tổng kết nên không tính là xanh. Các ca mới kiểm quyết định đồng thời, idempotency HTTP, quyền, số dư, thông báo, rollback audit, phân trang hạn và xóa/rollback chuỗi retention. T026 đánh [x]; các task còn phụ thuộc nguồn giữ mở.

Web **617/617 test**, lint và build qua (build có cảnh báo kích thước chunk); mobile **3/3 widget**, analyze phần mới không có vấn đề. Spotless thực sự định dạng 21 file trong 33 file Java được chọn qua regex hỗ trợ đường dẫn Windows. Không chạy verify, chưa commit/push. Log ở `backend/target/phase8-*.log` và `.local/010-notes/` đã gitignore; không thêm file note trong spec.
**Chưa được gọi toàn phase hoàn tất:** T025/T027/T028 còn nhánh nhập tay FR-021, mua hộ theo danh mục, ngoại lệ y tế khẩn và thuốc Q139 vì chưa có API công khai đủ dữ liệu từ B/D. `UnitPriceApi` không tra được toàn danh mục kể cả Ngừng hiệu lực; không tự dựng lại SharedKind/attributes của nguồn. `PurchaseAssistancePort` của D có sẵn nhưng cần tên/phân loại thuốc từ nguồn trước khi cài đúng. Nguồn số dư thật 017 thuộc phase riêng của E, fallback thiếu bean đúng P-12. A còn nối màn mua hộ vào điều hướng cổng/app. Bàn giao cụ thể ở [contracts/module-interfaces.md mục 5](contracts/module-interfaces.md) và `docs/team-plan.md` mục 8; chuỗi nguồn thật/xóa blob vẫn nghiệm thu ở T033. Không đánh xong những nhánh này bằng mock hoặc phản hồi 503.

### Kiểm toàn bộ và đối chiếu Git, 09/11/2026

Người dùng tạo nhánh `impl/010-latest` từ `impl/010-phase5`, commit/push `77c9dc42` trong lúc còn kiểm. Remote đã đối chiếu; chưa merge develop. Clean verify chạy hết 19 phút 35 giây: 1.242 unit/arch xanh; 3.057 IT có 27 failure, 26 error, 2 skip; Spotless còn phát hiện test được bổ sung giữa lượt chạy chưa định dạng. Không công bố toàn bộ xanh.

Sửa thuộc E: cô lập truy vấn danh sách hồ sơ trong fixture CostWorkflowIT/CostProvisionIT để không đọc dữ liệu sót của test khác; CostStatusParticipant giữ deathAt khi lệnh 004 cung cấp, dùng TransitionResult.at khi lệnh 001 không có trường đó. Ca hồi quy kiểm đúng mốc lịch sử chuyển trạng thái, không so với đồng hồ sau lệnh. Bổ sung xuất ba bảng đã chốt/một bảng chưa chốt, lần sau chỉ xuất bảng mới chốt; kiểm tên thuốc chỉ theo nguồn khớp khi có quyền. Không sửa test B hoặc bỏ assertion. Kết quả kiểm lại và gate còn mở ghi ở docs/progress.md; log và hướng dẫn merge/push ở .local/phase9-10/ (gitignore).
