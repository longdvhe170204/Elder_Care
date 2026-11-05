# Cần chốt định danh khi gộp bảng 008 — T050

Ngày 2026-10-31. Người dùng đã chọn “Giữ ID logic và liên kết cũ” trong phiên làm việc. Bắt đầu cài phương án 1; không cần duyệt lại lựa chọn này.

## 1. Quyết định đã chốt trước khi chép dữ liệu

**Đề xuất giữ liên kết nhật ký, tham chiếu chuyên môn và id công khai cũ bằng lớp ánh xạ trong phần E.** Bảng chung dùng id vật lý mới; lớp truy cập dữ liệu của E ánh xạ id logic cũ từ chi tiết legacy. Bản ghi mới dùng id không trùng không gian id logic cũ. Không sửa bản nhật ký/đính chính cũ, không tự coi duyệt cho 002 là duyệt cho 008.

Người dùng đã chọn hướng 1 dưới đây; hướng 2 chỉ giữ làm căn cứ so sánh, không áp dụng cho 008:

1. **Giữ liên kết cũ (đề xuất):** chốt bổ sung thiết kế ánh xạ id logic của E, kiểm cả chiều đọc/ghi và lưu giữ. Giữ chữ ký/trường API; bổ sung kiểm tránh trùng id logic khi lập bản ghi mới. Ghi rõ lệch kỹ thuật với câu “cấp id mới” trong data-model mục 0.1.
2. **Cho phép liên kết nhật ký cũ không mở được tới bản ghi đã đổi id, như quyết định riêng cho 002:** cần chủ dự án xác nhận áp dụng riêng cho 008. Vẫn bắt buộc bảo toàn dữ liệu, sửa mọi tham chiếu đang có hiệu lực; nếu phải cập nhật attributes.practiceScopeIds của M15.PROFESSIONAL_ACTIVITY thì A phải duyệt phạm vi dữ liệu của mình và review migration. Không được để quyền chuyên môn sai vì id bị đổi.

Không có phương án tự sửa/xóa nhật ký cũ hoặc lấy id cũ tra thẳng bảng chung khi có thể trùng với loại khác.

## 2. Bằng chứng tại mốc trước triển khai

- data-model.md:29 yêu cầu giữ id staff_assignment nhưng cấp id mới cho floor_manager_assignment khi nhập chung. FloorManagerService:141, AssignmentLifecycle:86 và StaffEmploymentEffects:152 đang ghi audit entity M09.FLOOR_MANAGER_ASSIGNMENT với id cũ.
- data-model.md:51 chuyển bốn danh mục vào catalog_item và đổi khóa ngoại theo id mới. CatalogApi tạo id tự tăng chung; bốn bảng cũ có dãy id độc lập, không thể mặc định mọi id cũ đều là id vật lý mới.
- DepartmentDutyService:60/91/137, CredentialService:84, ShiftScheduleService:108/125 và StaffImportService:88 đang ghi audit theo id các bản ghi sẽ chuyển.
- V002_95__table_consolidation.sql chép professional_activity_practice_scope.practice_scope_id vào catalog_item.attributes.practiceScopeIds (loại M15.PROFESSIONAL_ACTIVITY). ProfessionalActivityService:152 và LegalConditionService:79 so khớp các id này trực tiếp với LicenseValidity.practiceScopeIds do E trả.
- LegacyRecordTypeSource chỉ ánh xạ tên loại; RecordTypeAliasService không ánh xạ id. Chỉ thêm alias loại không giải quyết được id đổi hoặc trùng.
- progress.md ghi rõ chủ dự án đã cho phép đứt liên kết id cũ trong phạm vi 002 T098. Chưa có xác nhận tương tự cho 008 ở nhánh/nội dung phiên này.

## 3. Đính chính bàn giao: giữ căn cứ spec đang có

data-model.md:31 ghi M09.HANDOVER + id cục bộ của mục, trái spec FR-046 (spec.md:454) và HandoverCorrection:61–83: bản đính chính trỏ bàn giao gốc, nội dung bản đính chính chứa id các mục.

Phương án đề xuất giữ M09.HANDOVER + id bàn giao gốc như mã/spec hiện có; id mục chỉ nhận diện phần nội dung trong cùng bàn giao. Hai bàn giao có thể cùng id mục cục bộ, nên không dùng id mục làm originalRecordId của loại HANDOVER. Không sửa spec hoặc tài liệu nghiệp vụ.

## 4. Phạm vi triển khai đã chọn

- Hoàn tất cấu trúc và chuyển SQL về persistence; clean verify so baseline.
- Rà đủ điều kiện migration, bảng/khóa/trigger và lập kiểm thử cho trường hợp trùng id.
- Migration chỉ chạy trên schema test/scratch; chưa tác động dữ liệu demo. V008_95 đã chuyển và DROP bảng cũ trong schema test; API của A/C/D giữ nguyên.


## 5. Rà điều kiện và ma trận kiểm trước migration

Có 25 bảng legacy của 008, đích là 8 bảng riêng và bảy bảng chung hiện có. Không còn chờ 000 T108 hoặc SimpleRecordApi.listByStatus. Không có file demo V008 trong db/migration/demo trên nền d2bd7de0: test schema tạm phải tự lập tối thiểu một dòng hợp lệ cho từng bảng cũ, đồng thời vẫn nạp dữ liệu demo chung qua scratchFlyway.

| Nhóm | Kiểm bắt buộc sau chuyển |
| --- | --- |
| Bốn danh mục | Đủ số dòng, mã/trạng thái/tên/phiên, legacy.table/id, tham chiếu bộ phận/chứng chỉ/mẫu ca và id logic theo phương án đã chốt |
| Hai lịch sử | Đủ khoảng đóng/mở, thời điểm microsecond, lý do/actor/basis; một khoảng mở, chỉ đóng một lần |
| Bản công bố | Giữ nội dung snapshot, thời điểm, liên kết đúng lịch; chỉ thêm |
| Ý kiến/phản hồi | Đủ nội dung/người gửi/thời điểm; reply trỏ đúng comment sau chuyển, vẫn cùng bàn giao; chỉ thêm |
| Tỷ lệ | Giữ số thập phân, trường ratio NULL khi staffCount=0, event; parent entity_type khớp M09.SHIFT để lưu giữ xóa kèm |
| Import | Giữ fileName/rowCount/actor/thời điểm và tham chiếu nhân viên; bản chép khóa ngay |
| Phạm vi chứng chỉ | JSON rỗng khi không có phạm vi; đầy đủ id, đối chiếu mã và quyền chuyên môn của A; không tự sửa actor hoặc dữ liệu cũ |
| Giao Trưởng tầng | Giữ dữ liệu và hiệu lực, ID theo quyết định mục 1; CHECK phân biệt CARE/FLOOR_MANAGER kiểm rõ NULL; không tự bịa assigned_by/reason cho dòng cũ mà lược đồ cho NULL |
| Bàn giao | Giữ id cha, mọi mục/nhận định/trạng thái/section; không trùng nhận định; đính chính trỏ cha; giữ bổ sung AFTER_PREPARATION |
| Khóa/job | TechnicalLocks khóa cùng tầng; ReminderMarkers chống xử lý lặp đúng scheduled_at; SchedulerRuns ghi UUID/lần chạy ra file, không tạo bảng job mới |
| Nhóm dữ liệu | COMMENT DataGroup trên 8 bảng giữ; MULTIPLE có locked_at và trigger theo dòng/no_delete; kiểm tài khoản eldercare_app không sửa/xóa phần khóa |
| Quyền và hợp đồng | Giữ bài test nghiệp vụ 33 fixture đã liệt kê và DutyDirectoryIT mới; ApiContractIT, DutyPermissionIT, toàn bộ staffing xanh; test reporting đổi fixture phải C/A review theo chủ module |
| Chép và dọn | Chỉ DROP sau đối chiếu số dòng/FK; migration cũ không đổi; test schema tạm riêng, không tác động schema demo |

### Lưu ý kỹ thuật giữ nghiệp vụ bàn giao

Câu “hai cột JSON chỉ đổi khi bàn giao còn Nháp” ở data-model mục 0.1 chưa mô tả đủ việc thêm mục ở phần phát sinh sau khi lập. V008_04__shift_handovers.sql hiện cho thêm AFTER_PREPARATION khi bàn giao đã lập/chưa xác nhận; spec FR-044 và mã HandoverService cũng giữ phần này. Khi cài trigger JSON cần bảo vệ phần đã chốt, cho thêm đúng phần mới và chặn sửa/xóa nội dung cũ; không áp dụng cấm mọi UPDATE JSON khiến mất lệnh đã có.

Điểm này có căn cứ nghiệp vụ hiện có, xử lý bằng cập nhật mô tả kỹ thuật/plan và test bảo vệ, không tự sửa spec hoặc nghiệp vụ.


## 6. Phác thảo kiểm được cho hướng giữ liên kết

Ví dụ bộ phận cũ id=7 và loại chứng chỉ cũ id=7 có thể thành catalog_item id=501 và id=502. API nghiệp vụ vẫn dùng (M09.DEPARTMENT, 7) và (M09.CREDENTIAL_TYPE, 7); Store đổi sang id vật lý đúng loại khi gọi CatalogApi. Không dùng id=7 tra trực tiếp vào bảng chung vì có thể là bản ghi của module khác.

1. Mỗi dòng chép có legacy.table/id; định danh cũ luôn đi kèm loại, không chỉ một số id.
2. Tham chiếu vật lý giữa các bảng đổi theo mapping; DTO nghiệp vụ và Target/audit của bản ghi cũ giữ id logic. Adapter kiểm cả loại và legacy khi đọc, ghi, xóa lưu giữ.
3. Với bản ghi mới, trước khi mở lại ghi dữ liệu sau migration, dãy id được bảo đảm vượt miền id cũ của các loại đã gộp; test tạo mới sau migration kiểm không trùng id logic. Không lấy max+1 trong request concurrent; việc đặt dãy chỉ thực hiện trong migration.
4. Phạm vi hành nghề do LicenseValidity trả và M15.PROFESSIONAL_ACTIVITY đang giữ vẫn cùng id logic; không tự đổi attributes của A khi chưa cần và chưa duyệt.
5. Test schema tạm cố tình đặt trùng id giữa bảng cũ và một dòng bảng chung loại khác, kiểm liên kết nhật ký, truy quyền chuyên môn, đọc/sửa/retention và tạo bản ghi mới sau chuyển. Không coi chỉ đủ số dòng là đạt.

Phần kỹ thuật đã được chọn và cài trong V008_94/V008_95 cùng các Store của E. Kết quả từng bước ở r15-implementation-review.md. API quét sự kiện để lưu giữ còn thiếu: người dùng chọn chờ A theo shared-event-scan-proposal.md; E không sửa API/bản giả của A hoặc dùng SQL đọc bảng chung để thay thế.
