# Kế hoạch làm song song (5 người)

**Ngày**: 2026-10-19 | **Người giữ**: A (chủ dự án) | Căn cứ: dòng "Prerequisites" đầu `tasks.md` của từng feature, `contracts/module-interfaces.md`.

000 đã merge vào `develop`. 002 công bố gói `api` trước (đợt 0); sau đó các feature làm song song theo bảng dưới, phụ thuộc chưa xong thì dùng bản giả.

## 1. Chủ module

Một module (gói `backend/.../mNN/`) chỉ có một chủ. Feature ghi vào module nào thì thuộc chủ module đó.

| Người (GitHub) | Module | Feature (số task) | Thư mục web `frontend/src/features/` | Migration |
| --- | --- | --- | --- | --- |
| A (`longdvhe170204`) | m15, m13, m14, `common` | 002 (97), 009 (44), 016 (31); giữ 000 | màn của 002, 009, 016 | `V002_*`, `V009_*`, `V016_*` |
| B (`AAnh004`) | m01, m02, m12 | 001 (78), 004 (76), 018 (17), 013 (33) | màn của 001, 004, 018, 013 | `V001_*`, `V004_*`, `V018_*`, `V013_*` |
| C (`HungNX-nguyen`) | m03, m04 | 003 (65), 005 (65), 014 (33), 019 (28) | màn của 003, 005, 014, 019 | `V003_*`, `V005_*`, `V014_*`, `V019_*` |
| D (`03coc08`) | m05, m06, m07, m08 | 007 (77), 006 (65), 011 (45) | màn của 006, 007, 011 | `V006_*`, `V007_*`, `V011_*` |
| E (`ptlog18`) | m09, m10, m11 | 008 (45), 015 (31), 012 (53), 010 (35), 017 (29) | màn của 008, 010, 012, 015, 017 | `V008_*`, `V010_*`, `V012_*`, `V015_*`, `V017_*` |

Feature → module (theo `BE/mNN` trong tasks.md): 001 m01; 002 m15; 003 m03; 004, 018 m02; 005, 014 m04; 006 m07; 007 m05, m06; 008, 015 m09; 009 m13; 010, 017 m11; 011 m08; 012 m10; 013 m12; 016 m14; 019 m03 (phần m11 làm qua `api` của E).

Tên thư mục web cụ thể do chủ feature chốt khi dịch hợp đồng (glossary).

## 2. Đợt

| Đợt | A | B | C | D | E |
| --- | --- | --- | --- | --- | --- |
| 0 | Công bố `api` 002 kèm bản giả; khung glossary; CODEOWNERS | Dịch hợp đồng 001; công bố `api` m01, m02 | Dịch hợp đồng 003; công bố `api` m03, m04 | Dịch hợp đồng 006, 007; công bố `api` m05 → m08 | Dịch hợp đồng 008; công bố `api` m09 → m11 |
| 1 | 002 | 001 | 003 | 006 | 008 |
| 2 | 009 (sau 008) | 004 | 005 | 007 | 010 |
| 3 | 016 | 018 | 014, 019 | 011 (sau 005) | 015, 017 (sau 010) |
| 4 | Tích hợp; 000 T103, T106 | 013 | Sửa lỗi tích hợp | Sửa lỗi tích hợp | 012 |

## 3. Luật

1. **Chỉ sửa phần của mình**: gói module, `specs/NNN-*`, thư mục web, migration và mục glossary của mình. `.github/CODEOWNERS` tự yêu cầu chủ module review khi người khác sửa vào.
2. **Gọi module khác chỉ qua gói `api`** (Spring Modulith; `ModuleBoundaryTest` kiểm).
3. **Ai khai báo interface thì viết bản giả** của interface đó, đặt ở `backend/src/test/java/com/cambae/backend/support/fake/<module>/` dạng `@TestConfiguration`; người dùng chỉ `@Import`, không tự viết bản giả khác. Ví dụ SPI do 002 khai báo nhưng module khác cài (`BedAllocationApi`…) thì bản giả do A viết.
4. **Đổi `api` đã công bố**: sửa interface và bản giả trong cùng PR, ghi "Đổi api" ở mô tả PR, báo người đang dùng.
5. **Khóa ngoại tới bảng module khác** chỉ thêm khi module đó đã vào `develop`; trước đó lưu cột id không khóa ngoại, thêm khóa ngoại ở migration sau. Migration số nhỏ có thể vào `develop` sau migration số lớn: schema test tự tạo lại; database cá nhân thì chạy profile `demo` (`out-of-order`) hoặc tạo lại schema.
6. **File dùng chung**:

   | File | Quy tắc |
   | --- | --- |
   | `backend/pom.xml`, `frontend/package.json` | Nhắn A thêm thư viện |
   | `CLAUDE.md` | A cập nhật tiến độ khi merge |
   | `specs/000-shared-foundation/glossary.md` | Mỗi feature một mục tạo sẵn, chỉ sửa mục của mình |
   | `frontend/src/App.jsx`, `frontend/src/common/layout/navigation.js` | Chỉ thêm dòng của màn mình; trùng thì giữ cả hai dòng |
   | `frontend/src/api/` | Không sửa tay; `npm run gen:api` sau khi kéo `develop`, trùng thì chạy lại lệnh |
   | `backend/src/test/.../support/` | Mỗi bản giả trong `fake/<module>/` của chủ interface |

7. **Nhánh, PR**: mỗi feature một nhánh `impl/NNN-ten` từ `develop` mới nhất; mỗi phase một PR vào `develop`, một người khác review; PR đầu tiên của feature chỉ gồm bản dịch hợp đồng và glossary (chủ dự án duyệt). Đồng bộ hằng ngày bằng `git fetch origin; git merge origin/develop` (không rebase nhánh đã push).
8. **Nghiệp vụ chưa rõ**: không đoán, tra Q-xx, ghi vào PR và hỏi chủ dự án; không sửa `docs/business/*.md`.

## 4. Quy trình một feature

1. `git checkout develop; git pull; git checkout -b impl/NNN-ten`; đặt `.specify/feature.json` thành `{"feature_directory": "specs/NNN-ten"}`.
2. Dịch `contracts/api-NNN.yaml` sang tên tiếng Anh, viết mục glossary; mở PR riêng; merge xong chạy `npm run gen:api`.
3. Công bố `api` và bản giả của module; merge sớm.
4. Từng phase: viết test trước (tên theo `quickstart.md` mục 4, có mã quy tắc ở đầu) → thấy lỗi → cài → `.\mvnw.cmd verify`, `npm test`, `npm run lint`, `npm run build` → đánh `[x]` task có test đã chạy → ghi chú ở `plan.md` → PR.
5. Xong feature: chạy đủ lệnh ở mục "Lệnh" của CLAUDE.md, PR cuối.

## 5. Review

- [ ] `verify`, test web xanh (dán kết quả vào PR)
- [ ] test viết trước, tên có mã quy tắc
- [ ] không sửa ngoài phần của mình
- [ ] đổi `api` thì đổi bản giả và đã báo người dùng
- [ ] glossary, tasks, plan đã cập nhật
- [ ] không sửa `docs/business/*.md`

## 6. Họp tuần (15 phút)

Mỗi người: `api` vừa công bố hoặc đổi; bản giả sắp được thay bằng bản thật; đang vướng gì ở module người khác. Rà bảng ở mục 8: dòng nào xong, dòng nào cần thêm.

## 7. Dùng công cụ AI

Luật chung cho mọi công cụ ở [AGENTS.md](../AGENTS.md): Claude Code đọc CLAUDE.md; Codex và nhiều công cụ khác đọc AGENTS.md. Công cụ nào không tự đọc (Cursor, Copilot, ChatGPT, Gemini…) thì dán câu mở đầu ở đầu AGENTS.md vào cuộc trò chuyện (2026-10-21: bỏ `.cursor/rules/project.mdc`, `.github/copilot-instructions.md`, chủ dự án quyết). Cấu trúc thư mục ở `backend/README.md`, `frontend/README.md`, `mobile/README.md`.

Mỗi người một phiên, trên nhánh của mình. Lệnh đầu: "Làm feature NNN theo CLAUDE.md và docs/team-plan.md; chỉ sửa trong <module, thư mục của mình>; cần module khác thì dùng bản giả trong support/fake".

## 8. Việc đang chờ giữa các thành viên

Bảng này cho mọi người và mọi công cụ AI biết ai đang chờ ai. **Đầu mỗi phiên làm việc, xem các dòng có tên mình ở cột "Ai làm".** Khi đang làm mà cần người khác làm gì (khai báo, cài interface, bản giả, sửa hợp đồng), thêm một dòng ở đây trong PR của mình. Xong thì đổi trạng thái thành "Xong (PR, ngày)"; dòng xong quá hai tuần thì xóa. Chi tiết chữ ký ở `contracts/module-interfaces.md` của feature ghi ở cột "Căn cứ".

| Ai làm | Việc | Ai đang chờ, để làm gì | Căn cứ | Trạng thái |
| --- | --- | --- | --- | --- |
| B | Khai báo `NguoiCaoTuoiApi` ở `m01.api` (`trangThai`, `nhanDang`, `laTrangThaiCuoi`, `thoiDiemTrangThaiCuoi`) và bản giả ở `support/fake/m01/` | A (009 FR-015, FR-011): không gửi người thân sau qua đời, nội dung "chỉ nhận dạng" (`thoiDiemTrangThaiCuoi`, `nhanDang`). A (002): FR-044a chỉ cho ghi sự cố với người chưa ở trạng thái cuối (hiện chưa kiểm, đang nới hơn spec); US8 tạo tài khoản người cao tuổi (T091), họ tên người cao tuổi cho `TaiKhoanApi.hoTen` (T032a). D (007): hiện ba trường nhận dạng cho người ngoài phạm vi. *(2026-10-21)* Phần US8 không cần 001 đã xong; T091 là phần cuối của 002 còn chờ dòng này | 001 module-interfaces mục 1.1; 002 plan "Việc dang dở sau Phase 3", ghi chú Phase 9a | **Cần sớm** (chặn 002 T091) |
| B | Khai báo sự kiện `NguoiCaoTuoiDaChuyenTrangThai(nctId, tu, den, lenh, lichSuId, thoiDiem)` ở `m01.api.events` (`package-info` gắn `@NamedInterface`) kèm cách phát trong test (bản giả phát sự kiện) | A (002 T094, T087 #6): khóa tài khoản người cao tuổi ngay khi sang trạng thái cuối (BR-M15-23, research A-16); 009, 016 cũng nghe | 001 module-interfaces (bảng sự kiện); 002 research A-16 | **Cần sớm** (chặn 002 T094 phần listener) |
| B | Cài `FootnoteCondition` mã "1" (bản đồng ý chia sẻ dữ liệu bao gồm người thân đó, BR-M01-08) | A (002 US6): người thân xem sức khỏe (ô có ¹ và nhóm trường sức khỏe, thuốc, nguyện vọng); chưa có thì 002 từ chối | 002 research A-04, contracts mục 1.8; T071; plan 002 ghi chú Phase 8 | Chờ (bản giả `FakeConsents`) |
| B | Cài `ResidencyEndApi` (`KetThucLuuTruApi.thoiDiemTrangThaiCuoi`, M02, feature 004) | A (002 US6): job khóa tài khoản người thân sau kết thúc lưu trú (T070) | 002 contracts mục 2 | Chờ (bản giả có sẵn) |
| C | Cài `BedAllocationApi` (`openAt`, `residentsIn`, M03, feature 003); dữ liệu phân bổ mâu thuẫn thì ném `InvalidPermissionDataException` (nguồn `M03.BED_ALLOCATION`) | A (002): phạm vi dữ liệu theo phòng, tầng của người cao tuổi; báo lỗi dữ liệu nguồn (FR-041b) | 002 research A-07, A-10 | Chờ (bản giả có sẵn) |
| E | Cài các SPI 002 khai báo cho M09 (feature 008): `StaffProfileApi` (cả `createMinimal` cho công cụ khởi tạo), `LicenseApi`, `PracticeScopeCatalogApi`, `AssignmentApi`, `DutyApi`; xem lại kiểu trả về tối thiểu khi dịch hợp đồng 008 (đổi theo luật "Đổi api") | A (002): phạm vi theo phân công, quyền theo nhiệm vụ (T053), điều kiện pháp lý (T064), tạo Quản lý viện đầu tiên | 002 contracts mục 2; plan 002 ghi chú T009, T016 | Chờ (bản giả có sẵn) |
| E | Cài `DutyApi.replaceDepartmentLead` (`NhiemVuApi.thayPhuTrachBoPhan`, 2026-10-20) với `Propagation.MANDATORY`, kiểm người thay theo luật 008 (Q-293, Q-299) | A (002 T031): khóa tài khoản người đang là Phụ trách bộ phận phải thay người trong cùng lệnh (Q-292) | 002 contracts mục 2; plan 002 "Phase 4" | Chờ (bản giả có sẵn) |
| E | Cài `FamilyRelationshipApi` (M10, feature 012) và `FootnoteCondition` mã "2", "7" (người đại diện); chốt `QuanHeNguoiThanApi.quyenDangBat` khi dịch hợp đồng 012 | A (002 US6): phạm vi người thân đã chạy với bản giả (2026-10-21); còn quyền riêng của từng người thân (FR-045 (3), US6 #2: xem sức khỏe, xem chi phí…) chờ `quyenDangBat`, tới đó phần sức khỏe chỉ theo bản đồng ý | 002 contracts mục 2; research A-04; plan 002 ghi chú Phase 8 | Chờ (bản giả có sẵn) |
| E (012) | Lệnh chấm dứt quan hệ người thân gọi `AccountApi.recheckFamilyMember(familyMemberId, basis)` trong transaction của lệnh (`MANDATORY`), sau khi đã ghi quan hệ chấm dứt | Đúng FR-049, BR-M15-14: khóa tài khoản khi hết quan hệ | 002 contracts mục 1.6; plan 002 ghi chú Phase 8 | Ghi nhớ khi làm 012 |
| D | Lệnh ghi sự cố (007) chép `PermissionResult.basis` vào nhật ký: căn cứ `OUT_OF_SCOPE` thì ghi `ngoaiPhamVi` và chỉ hiện họ tên, ảnh, phòng/giường. Lệnh nút SOS báo A để thêm vào danh sách `SELF_CHECKED` của `RequiresPermissionArchTest` | Đúng FR-044a, BR-M15-01 | 002 plan "Phase 3", T015 | Ghi nhớ khi làm 007 |
| A | Khai báo `GiayPhepCoSoApi` (`coPhamVi`, `sapHetHan`) ở `m15.account.api` và bản giả | D (007) cài ở M06 bằng `FacilityLicenseApi` (`coversScope`, `expiringWithin`), test dùng `FakeFacilityLicenses` trong `FakeM15Config`; A (002 US5, T064) kiểm điều kiện pháp lý | 007 module-interfaces (bảng SPI); 002 contracts mục 2 | Xong (PR #8, 2026-10-20) |
| A | `ScopeApi` đầy đủ (`filter`, `historyFilter`, `shiftLead`, `assignedFloorManager`, `reviewDecider`, thêm `inScope(user, target, at)`; T052), `ApproverApi` (T054); `AccountApi` bản thật xong (T032, 2026-10-20) | Mọi module khi chạy thật (test dùng bản giả); 000 đính chính, phê duyệt | 002 plan "Việc dang dở sau Phase 3", ghi chú Phase 6 | Xong (PR #11–#14, 2026-10-21) |
| C, D (005, 006, 007, 011, 014…) | Cài `OfflineHandler` cho từng loại bản ghi gửi qua `POST /sync`; `requiresOnline()` cho kích hoạt khẩn cấp, liều có kiểm soát đặc biệt (Q-01); lưu cột "đồng bộ sau khi mất quyền" khi kết quả `ACCEPTED_WITH_FLAG` | A (002): đồng bộ đã chạy, chưa có loại bản ghi thật nào | 002 contracts mục 1.7; plan 002 ghi chú Phase 6 | Ghi nhớ khi làm lệnh ghi nhận |
| B (013), D (011) | Lệnh của Phụ trách bộ phận tự so bộ phận của đối tượng với `DutyApi.departmentOf`: lớp nhiệm vụ của 002 cho phép theo khóa vì `Target` không mang bộ phận (FR-019f) | Đúng FR-019f | 002 plan ghi chú Phase 6 | Ghi nhớ khi làm lệnh |
| E | Xem lại hai đổi SPI 002 khai báo cho 008 (luật "Đổi api", 2026-10-21): `LicenseApi.latest(staffId, licenseType)` (giấy phép gần nhất kể cả đã hết hạn, cho lỗi 403 nêu tên, ngày hết hạn) và `ExpiringLicense.licenseType`; `licenseType` là mã loại trong danh mục loại giấy phép/đào tạo của 008. `ProfessionalActivityApi.requiredLicenseType` (`NghiepVuChuyenMonApi.loaiYeuCau`) đã có cho 008 FR-030 | A (002 US5): lớp pháp lý, cảnh báo giấy phép | 002 plan ghi chú Phase 7 | Chờ (bản giả có sẵn) |
| D | `FacilityLicenseApi.coversScope(operationScope, date)` nhận mã nghiệp vụ chuyên môn của 002 (`INTERNAL_DIAGNOSIS_PRESCRIBING`, mục mới `PA-<id>`) làm mã phạm vi; danh sách phạm vi chuyên môn được phép của giấy phép cơ sở (007 FR-070) dùng mã này | A (002 T064): điều kiện (c) của FR-037 | 002 plan ghi chú Phase 7 | Chờ (bản giả có sẵn) |
| C (014) | Khai báo SPI người tham gia chuyến và chuyến của nhân viên cho phạm vi Trưởng đoàn, Người đi cùng (FR-031) | A (002 T049, T052) | 002 plan bảng việc dang dở | Chờ |
| A | Thêm `hoTen` vào `HoSoNhanVienApi`, `QuanHeNguoiThanApi` và `TaiKhoanApi.hoTen` (T032a), báo E | 000 (`Actor.fullName`), 002 (`/me`) | 002 contracts mục 1.6, mục 2 | Xong (PR #8, 2026-10-20); E cài ở dòng dưới |
| E | Cài `StaffProfileApi.fullName` (008) và `FamilyRelationshipApi.familyMemberName` (012); chưa cài thì họ tên người thực hiện trống, không lỗi | 000 (`Actor.fullName` ở nhật ký, phê duyệt, đính chính, tham số), 002 (`/me`, bảng tài khoản) | 002 contracts mục 1.6, mục 2; plan 002 "Phase 4" | Chờ (bản giả có sẵn) |
| E (008), C (003), E (012) | Dữ liệu demo của module mình theo quickstart 002 mục 1: 008 hồ sơ nhân viên id 9001 → 9009 (khớp chín tài khoản `demo.*` của `demo/V002_90__sample_accounts.sql`, thứ tự ở quickstart 002), ca ngày T1 có Người phụ trách ca là `demo.dieuduong`, Trưởng tầng `demo.truongtang` giao T1, phân công tầng 1, giấy phép của `demo.bacsi`; 003 hai tầng, mỗi tầng hai phòng, A (T1), B (T2) có phân bổ mở; 012 người thân R, M | A (002 T081): chạy kịch bản quickstart 002 mục 3 trên `demo`; 000 T106, T103 | 002 quickstart mục 1; plan 002 ghi chú Phase 10 | Ghi nhớ khi làm dữ liệu demo |
| Mọi người (001, 003–019) | Test tích hợp cần người dùng có quyền thật dùng `support/TestUsers` (`staff(Role...)`, `family(id)`, `session`, `bearer`) cùng bản giả SPI của `FakeM15Config` (đặt phân công cho `TestUser.staffId()`); `FakePermissions` chỉ còn cho test của 000 hoặc khi cần quyền khác ma trận | Quyền trong test khớp Phụ lục 27 | 002 T080; plan 002 ghi chú Phase 10 | Áp dụng khi viết test |
| Mọi người | Theo cấu trúc trong module mới (000 research R-02 mục "Cấu trúc trong module", 2026-10-21): `api/{dto, spi, events}`, `web/{request, response}`, `internal/{service, domain, persistence, config}` (module lớn chia theo chức năng trước), `scheduling/`. **Đổi gói M15** (luật "Đổi api"): SPI 002 khai báo cho module khác cài nay ở `m15.account.api.spi` (`StaffProfileApi`, `LicenseApi`, `DutyApi`, `FamilyRelationshipApi`, `ResidencyEndApi`, `BedAllocationApi`, `FootnoteCondition`, `OfflineHandler`…); `Target`, `PermissionKey`, `Function`, `Operation`… ở `m15.account.api.dto`; SPI của 000 ở `<phần>.api.spi`. Bản giả ở `support/fake/` đã đổi theo | Ranh giới module (Modulith, `M15PartBoundaryArchTest`) | 000 research R-02; plan 002 "Tổ chức lại backend" | Áp dụng khi viết mã |
| Mọi người | DTO có trường thuộc nhóm thông tin giới hạn (sức khỏe, thuốc, nguyện vọng, tài chính…): gắn `@FieldGroup`, cài `VisibilityTarget`, khai trường `hiddenFields` (tên logic `truongBiAn`). Lệnh `@BusinessCommand` phải có `@RequiresPermission`; khóa quyền chỉ biết lúc chạy thì báo A thêm vào `SELF_CHECKED` | Giới hạn trường FR-022, FR-034; kiểm quyền BR-M15-01 | 002 contracts mục 1.4; `RequiresPermissionArchTest` | Áp dụng khi viết mã |
| A | Thành phần chứng từ `common.document` (tên logic `common.bieumau`: `PdfDocument`, `ExcelDocument`, `ExcelRow`, `FacilityInfo`; 007 T104), kiểu `.xlsx` cho tệp hệ thống tạo (`common.file.FileTypes`; 017 T003), SPI `ParameterValueCheck` (`KiemTraGiaTriThamSo`, `m15.parameter.api.spi`; phần 000 của 017 T045) | D (007 phiếu tóm tắt chuyển viện T103 → T105), E (008 import Excel T047, 017 chứng từ, sao kê, CFG-M11-10). Module dựng chứng từ tự đọc CFG-M11-11 (`asTable(FacilityInfo.class)`, chưa đặt thì truyền `null`) rồi truyền cho `PdfDocument.a4` (`common` không gọi ngược `m15`) | 007 plan, 017 plan "Ghi chú triển khai"; glossary mục 6.5 | Xong (PR #18, 2026-10-22) |
| B (001) | Bản cài `FileApi` (001 T003) gọi `FileTypes.check(mimeType, content, FileTypes.SYSTEM_GENERATED)` trong `upload`; controller `POST /files` gọi trước với `FileTypes.USER_UPLOAD` (người dùng không tải lên `.xlsx`); lỗi 422 `FILE_TYPE_NOT_ALLOWED`, `FILE_TOO_LARGE` | E (017 chứng từ, file xuất `.xlsx`), D (007 phiếu PDF) lưu tệp hệ thống tạo | 017 research E-11; 017 plan "Ghi chú triển khai" | Chờ (khi làm 001 T003) |
| E (017) | Cài `ParameterValueCheck` cho CFG-M11-10 (`code()` = `CFG_M11_10`; `check` kiểm `maBin` có trong `ngan_hang` và còn hiệu lực, `soTaiKhoan` 6 → 19 chữ số, `chuTaiKhoan` không rỗng; ném `InvalidValueException`). SPI mới (luật "Đổi api"); test dùng `FakeParameterValueCheck` | Đúng 017 research E-04: Quản lý viện không lưu được tài khoản thu sai | 017 T045; 017 plan "Ghi chú triển khai" | Chờ (khi làm 017 T045) |
| E (008) | Cài hai hàm SPI mới của `AssignmentApi` (`m15.account.api.spi`, luật "Đổi api" 2026-10-22): `staffAssignedToResident(residentId, at)`, `staffAssignedToRoom(roomId, at)` (tên logic `nhanVienPhanCongChoNguoiCaoTuoi`, `nhanVienPhanCongChoPhong`): nhân viên đang có phân công cho người cao tuổi (trực tiếp hoặc qua phòng, tầng người đó đang ở theo luật 008), cho phòng | A (009 US2): nhóm Điều dưỡng phụ trách, nhân viên phân công (FR-007 (a), BR-M13-04). Test dùng `FakeAssignments` | 002 contracts mục 2; plan 009 ghi chú Phase 3 | Chờ (bản giả có sẵn) |
| B (001) | Khai báo `BanDongYApi` ở `m01.api` (`baoGom(nctId, nguoiThanId)`, `hienHanh(nctId)` có mục `dongYThongBao`) và bản giả ở `support/fake/m01/` | A (009 US2): phần sức khỏe gửi người thân (FR-013, DBR-03), phạm vi "nhận thông báo" (FR-014, FR-014a). Tới đó 009 coi mọi người thân ngoài phạm vi: thông báo không khẩn về người cao tuổi chỉ gửi khi có mã Q-354 | 009 contracts mục 2.3; research B-04; plan 009 ghi chú Phase 3 | **Cần sớm** (người thân gần như chưa nhận thông báo) |
| E (008) | Khai báo `CaTrucApi` ở `m09.api` (`dangTrongCa`, `caDangDienRa` theo 008 module-interfaces; vai trò không xếp ca "trong ca" theo CFG-M13-06) và bản giả ở `support/fake/m09/` | A (009): nhóm người nhận `OnShiftStaff`, `OnDutyDoctor` (Phase 3, T015), người được nhắc gọi còn trong ca (Phase 4, FR-029, FR-030a), chọn kênh ngoài ca (Phase 5, FR-016a). Phase 1, 2 của 009 không cần | 009 module-interfaces mục 2.3; plan 009 ghi chú Phase 2 | **Cần trước Phase 3 của 009** |
| E (012) | Cài `FamilyNotificationPort` (`NguoiThanNhanThongBaoPort`, `m13.api.spi`: `activeRelationships`, `callOrder`, `contact`); người liên hệ chính luôn có `RECEIVE_URGENT` (Q-96) | A (009): nhóm quan hệ người thân (e), chuỗi gọi (FR-029a), số của người thân nhóm (d). Test dùng `FakeFamilyNotifications` trong `FakeM13Config` | 009 contracts mục 2.1; plan 009 ghi chú Phase 2 | Chờ (bản giả có sẵn) |
| E (008) | Cài `StaffProfileApi.phoneNumber(staffId)` (`HoSoNhanVienApi.soDienThoai`, `m15.account.api.spi`; luật "Đổi api", chủ dự án duyệt 2026-10-23): số điện thoại liên hệ trên hồ sơ nhân viên, không có thì rỗng | A (009 US3): tin nhắn Trung bình, Khẩn cấp cho nhân viên (FR-016, FR-019); chưa cài thì lần gửi tin nhắn ghi `NO_ADDRESS`. Test dùng `FakeStaffProfiles.phone` | 002 contracts mục 2; plan 009 ghi chú Phase 5 | Chờ (bản giả có sẵn) |
| D (007) | Khai báo và cài `CanhBaoLenhApi.tiepNhan(canhBaoId, nd)`, `SuCoLenhApi.tiepNhan(suCoId, nd)` ở `m05.api` (cùng kiểm tra, tác động như `POST /canh-bao/{id}/tiep-nhan`, `/su-co/{id}/tiep-nhan`; từ chối thì ném lỗi có mã, ví dụ `DA_CO_NGUOI_TIEP_NHAN`) kèm bản giả ở `support/fake/m05/` | A (009 US1, FR-026a): `POST /notifications/{id}/acknowledge-and-take-charge` "Xác nhận và tiếp nhận" (T019, T021); hiện chỉ có xác nhận | 007 module-interfaces mục 1.6; plan 009 "Phụ thuộc" mục 3 | Chờ |
| B (001) | Theo `Problem` chung (A chọn 2026-10-23, BL-23, nhánh `impl/000-shared-problem`): `ProblemBody` thêm `candidates` (mảng object), `UnmetCondition` thêm `basis`, `module`, `waivable` (không bắt buộc, `null` thì không ghi ra; hàm dựng hai tham số giữ nguyên). Sửa schema `Problem` của api-001 cho giống api-000 (thêm `failedLayer`, `license`, `unlocksAt`, `currentValue` bỏ `type: object`, `Condition` trong `unmetConditions` thêm `module`), ném `ConditionNotMetException` với `UnmetCondition` năm tham số cho điều kiện trạng thái; đăng ký api-001 vào `ApiContractIT` (A đã duyệt) cùng PR 001 | Đúng R-03, R-14: một `Problem` cho mọi hợp đồng | 000 research R-03; glossary mục 6 | Chờ (PR #21 đã vào develop 2026-10-23) |
| B (001) | Thêm `RETRY_ALLERGY_CHECKS` vào `runJobs` của api-000 và `TestClockController.TestJob` cùng PR 001 (A duyệt 2026-10-23); giữ hai sửa của B ở `application.yml` profile `test`, `SchedulerScopeArchTest.ALLOWED` | B (001 T031): test job qua `POST /testing/clock` | 001 research H-12 | Duyệt, B làm |
| Mọi người | Khuôn `common.notification` đủ nhóm người nhận của 009 (2026-10-21): `RecipientGroup.DutyGroup` (`AssignedNurse`, `AssignedStaff`, `OnShiftStaff`, `ShiftLead`, `OnDutyDoctor`, `FloorManager`, `TripLeader`, `DepartmentLead`, mỗi nhóm kèm `Substitution`), `Staff`, `FamilyMember`, `PrimaryContact`, `Representative`, `FamilyWithRight` (`FamilyRight`); khóa `displayParams` cấm ở nội dung rút gọn: `HealthFields.KEYS`. Sự kiện cài `HasNotificationRequests` nay được M13 tiếp nhận thật (một listener); có nhóm người thân thì khai `informationSubject` (FR-001a). Không gọi FCM, `SmsProvider` trực tiếp (`M13ArchTest`) | Một cách gửi thông báo (009 FR-001) | glossary mục 15.5; plan 009 ghi chú Phase 2 | Áp dụng khi viết sự kiện |
