# Kiểm triển khai T054/T055 và màn Phân công T022b — 2026-10-29

Nhánh `impl/008-mobile-assignments`, worktree `.worktrees/008-mobile-assignments`, từ `origin/develop` **685dea6**. Chưa commit, push, PR hoặc merge. Xác nhận duyệt HTTP, bố cục/hai file nền và staffId được ghi ở [biên bản](assignment-contract-review.md).

## 1. Kết quả trong phạm vi

- **T054:** GET chi tiết/version, xem trước sửa và cập nhật Nháp trên cùng id; UPDATE không cần CREATE, If-Match, chống lặp, cảnh báo FR-031 đúng enum hợp đồng. Kiểm ghi SQL lỗi/đồng thời giữ bản cũ; Nháp không tạo tác động có hiệu lực.
- **T055:** hai GET người phụ trách/danh sách thiếu người; backend tính theo ca và mốc lịch sử, kiểm phạm vi, vắng/bán trú/trạng thái cuối. Thiếu nguồn trả lỗi, không giả danh sách rỗng hoặc phát nhắc khi đọc.
- **T022b:** đã cài chọn tháng/lịch/ca, phân công, form preview/lưu/sửa Nháp, xóa/hủy/kết thúc, sao chép, giao/rút ngắn Trưởng tầng, tra kết quả. Đọc lại sau 412 nhận cả dữ liệu hiện hành và version; thử lại cùng nội dung giữ body/khóa dù thứ tự trường form thay đổi. Ngày giờ nhập tiếng Việt, gửi +07:00; ô chọn không cắt chữ ở 200%. **Chưa đánh [x]**, còn nguồn thật và nghiệm thu thiết bị ở mục 3.

## 2. Kiểm đã chạy

| Phần | Kết quả |
| --- | --- |
| TDD backend | Sửa Nháp: 10 test đỏ vì thiếu endpoint trước cài; đọc: 12 ca đỏ 404 trước cài; thêm test JSON không-null và mã cảnh báo sai đã đỏ rồi xanh |
| Backend theo phạm vi cuối | 76 IT/hồi quy và 3 unit xanh (70 ca liên quan phân công/hợp đồng, 6 ca M15); gồm ApiContractIT của A giữ nguyên, AssignmentDraftUpdateIT, AssignmentDraftPermissionIT, AssignmentDraftAtomicityIT, AssignmentResolutionHttpIT, AssignmentIT, AssignmentHttpIT, AssignmentReadSourcesTest, AssignmentDetailSerializationTest, AssignmentFixtureIsolationIT; hai lớp M15 giữ nguyên |
| Hợp đồng hoạt động | Giữ nguyên 62 thao tác cũ và schema cũ; thêm 5 thao tác, 262 tham chiếu phân giải, 6 schema mới/12 mẫu đúng kết quả; YAML không trùng khóa |
| Web | Sinh lại client bằng npm run gen:api; 339 test/58 file xanh, npm run lint và npm run build qua |
| Mobile | 75 test xanh, flutter analyze sạch sau review; thêm 3 ca đỏ rồi xanh về xác nhận cảnh báo mới, bỏ preview sau lỗi và tìm kiếm trả chậm; các ca client/menu/form, +07:00, đọc lại 412, chống lặp và cắt chữ vẫn xanh |
| Clean verify toàn kho | 640 unit/arch xanh; 1457 IT: 7 thất bại, 10 lỗi, không bỏ qua. M09: 96 unit + 282 IT = 378 test xanh. Hai lớp M15 từng bị fixture tác động đã xanh trong chính lượt toàn kho cuối. Còn 9 lớp M01/M03 đã biết và một deadlock M13 ở bước dọn test; chạy riêng M13 sau đó 5/5 xanh |

Spotless đã chạy cho Java của M09 sửa trong nhánh. Lệnh backend nạp `.env` và `backend/.env` ở gốc kho dùng chung; không sao chép/ghi giá trị bí mật. Giữ pool mặc định 5; lượt cuối chỉ giảm log SQL, không bỏ test hoặc đổi cấu hình nghiệp vụ.

Lượt toàn kho đầu có lỗi cạn kết nối ở CredentialLifecycleIT và tắc khi job bàn giao xử lý nhiều dữ liệu tồn; đã dừng đúng JVM test của phiên sau khi chụp stack (hơn 1.100 luồng). Bản develop nguyên gốc được xuất vào thư mục ignored, chạy riêng CredentialLifecycleIT 11/11 xanh. Fixture mới sau đó được cô lập: hủy đúng ca/phân công, vô hiệu hóa tài khoản và khép khoảng vai trò do fixture tạo, kể cả dữ liệu cùng mẫu/mốc từ lượt đỏ/xanh cũ; không sửa test hoặc dữ liệu fixture của người khác. Lượt toàn kho cuối hoàn tất 14 phút 09 giây, Spotless qua; kết quả đỏ vẫn được giữ nguyên trong báo cáo, không suy toàn kho xanh từ lần chạy riêng.

Đã kiểm ảnh widget form sáng/tối, chữ 200% và sửa lỗi cắt vai trò dài bằng test đo vùng chữ. Ảnh trong `mobile/build/008-assignment-review/`, probe trong `.dart_tool/`, đều ignored. Đây không phải bằng chứng chạy trên máy ảo/máy thật hoặc backend thật. Pub get đã lấy dependency từ cache; máy còn thiếu hỗ trợ symlink của Windows, không đổi cài đặt máy, test chạy `--no-pub`.

Các lớp đỏ có sẵn đối chiếu team-plan: M01 BR_M01_01_TemporaryAbsenceOverdueIT, BR_M01_02_ReassessmentNoticeIT, CompleteAdmissionConsentIT, CompleteAdmissionWishWarningsIT, FR_023h_KhongDauNangLucIT, FR_034a_CompleteAdmissionIT, ResidentContractIT, StatusHistoryIT; M03 FR_035_LocationHistoryHttpIT (mong 503 nhận 404).

Lỗi bổ sung của lượt cuối: FR_029a_CallResultIT.FR_029_reachedByFloorManagerWhileAtShiftLead_othersRefused lỗi trước thân test, tại CallReminderTestBase.isolateFromOtherTests dòng 109: UPDATE notification_state trên toàn schema gặp deadlock. Chạy lại riêng đúng lớp, giữ nguyên mã, 5/5 xanh. Chưa kết luận lỗi này đã được sửa; đã thêm việc A rà tình huống dọn test chạy đua ở team-plan. Hai lớp FirstManagerBootstrapIT và ManagerAlwaysExistsIT xanh trong lượt toàn kho sau sửa fixture M09. Bằng chứng raw và tổng hợp ở backend/target/008-assignment-verification (ignored), không chứa trong thay đổi Git.

## 3. Phần còn chờ

- **B (004):** `ContractReadApi.attendanceOn` bản thật; T055 cần lịch đến bán trú để xác định danh sách đầy đủ. Chưa có nguồn thì trả 422 theo hợp đồng.
- **C (005):** `TaskTypeApi`, `CarePlanApi`, `ShiftTaskApi` và danh mục HTTP tên tiếng Anh/bản thật. App không gọi `/loai-cong-viec` là tên logic chưa dịch, không tự đặt endpoint mới. Chưa chọn được loại công việc mới từ nguồn; giữ loại đang áp dụng hoặc lựa chọn không giới hạn và backend vẫn kiểm. Các kiểm nguồn đang dùng fake chung của chủ interface trong test.
- **A (009):** rà deadlock tại `backend/src/test/java/com/cambae/backend/m13/CallReminderTestBase.java:109` trong lượt toàn kho; chạy riêng xanh chưa chứng minh hết lỗi chạy đua. Không sửa test của A trong nhánh này.
- **A:** review mã [ApiClient](../../mobile/lib/common/api/api_client.dart), [StaffRoot](../../mobile/lib/staff/staff_root.dart), sau phạm vi/bố cục đã được duyệt. Đã ghi dòng team-plan mục 8; chưa mở PR.
- **E:** nối/kiểm nguồn thật, tên được phép xem và đầy đủ lựa chọn loại công việc; nghiệm thu các luồng trên thiết bị trước khi đóng T022b. T016b Ca của tôi nằm ở nhánh riêng, khi gộp giữ cả hai tab. Các nguồn bàn giao/việc khác của 008 không được nghiệm thu thêm trong nhánh này.

## 4. File để xem mã

| Phần | File chính |
| --- | --- |
| Sửa Nháp | `backend/.../m09/assignment/internal/service/AssignmentService.java`, `internal/persistence/AssignmentStore.java`, `web/AssignmentController.java`, `web/response/AssignmentResponses.java` |
| Đọc kết quả | `AssignmentReadService.java`, `AssignmentReadSources.java`, `AssignmentReadController.java`, `AssignmentReadResponses.java` trong cùng phần assignment |
| Mobile | `mobile/lib/staff/shifts/assignments_screen.dart`, `assignments_controller.dart`, `assignment_form.dart`, `assignment_moment.dart`, `assignment_labels.dart`, `floor_manager_sheet.dart` |
| Nền A được duyệt | `mobile/lib/common/api/api_client.dart`, `mobile/lib/staff/staff_root.dart`; dùng lại riêng getList/test từ nhánh T016b, không chép màn Ca của tôi |
| Test | `backend/src/test/java/com/cambae/backend/m09/Assignment*`, `mobile/test/staff/shifts/` |
| Tài liệu/client | `contracts/api-008.yaml`, hai bản duyệt riêng, `tasks.md`, `plan.md`, `data-model.md`, glossary mục 14, `docs/team-plan.md`, chỉ thêm `docs/progress.md`; `frontend/src/api/api008.js` do generator sinh |

## 5. Review sau triển khai — 2026-10-29

Đã rà diff backend, HTTP/DTO so với hợp đồng đã duyệt, client sinh, mobile, test và tài liệu của nhánh. Phần backend đối chiếu khóa/phiên bản, giữ id khi sửa, quyền/phạm vi đọc, mốc lịch sử và trường hợp thiếu nguồn; mobile đối chiếu allowedCommands, API, lỗi, xem trước và kết quả bất đồng bộ. Chưa thấy lỗi khác cần sửa trong diff đã rà; các giới hạn nguồn thật/thiết bị tại mục 3 vẫn còn. Review này không thay việc A review hai file nền của A.

| Phát hiện | Ảnh hưởng trước sửa | Sửa và test tái hiện |
| --- | --- | --- |
| P2 — giữ preview/xác nhận cũ khi kiểm lại | Sau khi đã xác nhận, cảnh báo mới không buộc đọc lại; preview mới lỗi 412 vẫn cho bấm Lưu theo preview cũ | `assignment_form.dart`: xóa preview, xác nhận và lỗi cũ trước mỗi lượt kiểm. Hai test `FR_031_xem trước lại phải xác nhận cảnh báo mới`, `FR_025_xem trước lại lỗi phải bỏ kết quả cũ và khóa lưu` đỏ đúng assertion trước sửa, sau sửa xanh |
| P2 — tìm người trả chậm ghi đè đối tượng | Đổi từ Người cao tuổi sang Tầng lúc tìm kiếm còn chạy: kết quả người cũ thay danh sách tầng, có thể chọn sai loại id | `assignment_form.dart`: tìm kiếm dùng cùng số thứ tự yêu cầu với tải đối tượng, bỏ kết quả cũ. Test `FR_025_tìm người trả chậm không ghi đè danh sách tầng vừa chọn` đỏ với id người 77 thay id tầng 2, sau sửa xanh |
| Ghi chú quyết định đã cũ | YAML hoạt động và bản duyệt còn ghi chờ chốt staffId dù chủ dự án đã xác nhận | Cập nhật mô tả và `x-owner-decision` của cả hai YAML theo biên bản đã duyệt; không đổi endpoint, trường hay schema. Sửa câu chờ quyết định cũ trong đề xuất mobile |

Kiểm sau sửa: test form 7/7 xanh, `flutter test --no-pub` toàn mobile 75/75 xanh, `flutter analyze --no-pub` sạch. Log đỏ/xanh/toàn bộ/analyze ở `mobile/build/008-assignment-review/review-*.log` (ignored). Lượt review chỉ sửa Dart và ghi chú tài liệu/quyết định; kết quả backend/web ở mục 2 là lượt triển khai đã chạy trước đó, không phải một lượt clean verify mới. T022b giữ [ ]; các dòng chờ A/B/C ở team-plan mục 8 giữ lại vì chưa hoàn thành phần của họ. Chưa commit, push, PR hoặc merge.
