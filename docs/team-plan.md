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

Luật chung cho mọi công cụ ở [AGENTS.md](../AGENTS.md): Claude Code đọc CLAUDE.md; Codex và nhiều công cụ khác đọc AGENTS.md; Cursor đọc `.cursor/rules/project.mdc`; Copilot đọc `.github/copilot-instructions.md`; ba file sau đều trỏ về AGENTS.md. Công cụ nào không tự đọc (ChatGPT, Gemini trên web…) thì dán câu mở đầu ở đầu AGENTS.md vào cuộc trò chuyện. Cấu trúc thư mục web, mobile ở `frontend/README.md`, `mobile/README.md`.

Mỗi người một phiên, trên nhánh của mình. Lệnh đầu: "Làm feature NNN theo CLAUDE.md và docs/team-plan.md; chỉ sửa trong <module, thư mục của mình>; cần module khác thì dùng bản giả trong support/fake".

## 8. Việc đang chờ giữa các thành viên

Bảng này cho mọi người và mọi công cụ AI biết ai đang chờ ai. **Đầu mỗi phiên làm việc, xem các dòng có tên mình ở cột "Ai làm".** Khi đang làm mà cần người khác làm gì (khai báo, cài interface, bản giả, sửa hợp đồng), thêm một dòng ở đây trong PR của mình. Xong thì đổi trạng thái thành "Xong (PR, ngày)"; dòng xong quá hai tuần thì xóa. Chi tiết chữ ký ở `contracts/module-interfaces.md` của feature ghi ở cột "Căn cứ".

| Ai làm | Việc | Ai đang chờ, để làm gì | Căn cứ | Trạng thái |
| --- | --- | --- | --- | --- |
| B | Khai báo `NguoiCaoTuoiApi` ở `m01.api` (`trangThai`, `nhanDang`, `laTrangThaiCuoi`, `thoiDiemTrangThaiCuoi`) và bản giả ở `support/fake/m01/` | A (002): FR-044a chỉ cho ghi sự cố với người chưa ở trạng thái cuối (hiện chưa kiểm, đang nới hơn spec); US8 tạo, khóa tài khoản người cao tuổi (T090, T091); họ tên người cao tuổi cho `TaiKhoanApi.hoTen` (T032a). D (007): hiện ba trường nhận dạng cho người ngoài phạm vi | 001 module-interfaces mục 1.1; 002 plan "Việc dang dở sau Phase 3" | Chờ |
| B | Cài `FootnoteCondition` mã "1" (bản đồng ý chia sẻ dữ liệu bao gồm người thân đó, BR-M01-08) | A (002 US6): người thân xem sức khỏe; chưa có thì 002 từ chối | 002 research A-04, contracts mục 1.8; T071 | Chờ |
| B | Cài `ResidencyEndApi` (`KetThucLuuTruApi.thoiDiemTrangThaiCuoi`, M02, feature 004) | A (002 US6): job khóa tài khoản người thân sau kết thúc lưu trú (T070) | 002 contracts mục 2 | Chờ (bản giả có sẵn) |
| C | Cài `BedAllocationApi` (`openAt`, `residentsIn`, M03, feature 003); dữ liệu phân bổ mâu thuẫn thì ném `InvalidPermissionDataException` (nguồn `M03.BED_ALLOCATION`) | A (002): phạm vi dữ liệu theo phòng, tầng của người cao tuổi; báo lỗi dữ liệu nguồn (FR-041b) | 002 research A-07, A-10 | Chờ (bản giả có sẵn) |
| E | Cài các SPI 002 khai báo cho M09 (feature 008): `StaffProfileApi` (cả `createMinimal` cho công cụ khởi tạo), `LicenseApi`, `PracticeScopeCatalogApi`, `AssignmentApi`, `DutyApi`; xem lại kiểu trả về tối thiểu khi dịch hợp đồng 008 (đổi theo luật "Đổi api") | A (002): phạm vi theo phân công, quyền theo nhiệm vụ (T053), điều kiện pháp lý (T064), tạo Quản lý viện đầu tiên | 002 contracts mục 2; plan 002 ghi chú T009, T016 | Chờ (bản giả có sẵn) |
| E | Cài `FamilyRelationshipApi` (M10, feature 012) và `FootnoteCondition` mã "2", "7" (người đại diện); chốt `QuanHeNguoiThanApi.quyenDangBat` khi dịch hợp đồng 012 | A (002 US6): phạm vi và quyền của người thân | 002 contracts mục 2; research A-04 | Chờ (bản giả có sẵn) |
| D | Lệnh ghi sự cố (007) chép `PermissionResult.basis` vào nhật ký: căn cứ `OUT_OF_SCOPE` thì ghi `ngoaiPhamVi` và chỉ hiện họ tên, ảnh, phòng/giường. Lệnh nút SOS báo A để thêm vào danh sách `SELF_CHECKED` của `RequiresPermissionArchTest` | Đúng FR-044a, BR-M15-01 | 002 plan "Phase 3", T015 | Ghi nhớ khi làm 007 |
| A | Khai báo `GiayPhepCoSoApi` (`coPhamVi`, `sapHetHan`) ở `m15.account.api` và bản giả | D (007) cài ở M06; A (002 US5, T064) kiểm điều kiện pháp lý | 007 module-interfaces (bảng SPI); 002 contracts mục 2 | Chờ |
| A | `ScopeApi` đầy đủ (`filter`, `historyFilter`, `shiftLead`, `assignedFloorManager`, `reviewDecider`; T052), `ApproverApi` (T054), `AccountApi` (T032); hiện bản thật mới có `inScope` | Mọi module khi chạy thật (test dùng bản giả); 000 đính chính, phê duyệt | 002 plan "Việc dang dở sau Phase 3" | Đang làm (002 US2 → US4) |
| A | Thêm `hoTen` vào `HoSoNhanVienApi`, `QuanHeNguoiThanApi` và `TaiKhoanApi.hoTen` (T032a), báo E | 000 (`Actor.fullName`), 002 (`/me`) | 002 contracts mục 1.6, mục 2 | Chờ (002 US2) |
| Mọi người | DTO có trường thuộc nhóm thông tin giới hạn (sức khỏe, thuốc, nguyện vọng, tài chính…): gắn `@FieldGroup`, cài `VisibilityTarget`, khai trường `hiddenFields` (tên logic `truongBiAn`). Lệnh `@BusinessCommand` phải có `@RequiresPermission`; khóa quyền chỉ biết lúc chạy thì báo A thêm vào `SELF_CHECKED` | Giới hạn trường FR-022, FR-034; kiểm quyền BR-M15-01 | 002 contracts mục 1.4; `RequiresPermissionArchTest` | Áp dụng khi viết mã |
