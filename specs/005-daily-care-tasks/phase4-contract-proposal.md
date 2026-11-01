# Hợp đồng Phase 4 — phần đã duyệt và phần đang chờ

Ngày: 2026-11-01. Phạm vi: T018–T026. Chủ dự án đã duyệt mục 1–3 bằng câu trả lời “1” trong phiên này; bản dịch HTTP, hai bổ sung SPI và client sinh đã được áp dụng. Mục 5 và 6 đã được chủ dự án chọn hoãn, chưa áp dụng; quyết định cuối ở mục 7. Căn cứ: AGENTS.md mục 2.5, CLAUDE.md mục Khi vào triển khai, tasks T022/T025 và research C-07.

## 1. Dịch đúng bảy operation của T025

Giữ HTTP method, ý nghĩa, response status, required/optional và header `Idempotency-Key`; lệnh trạng thái không thêm `If-Match`.

| Hiện tại | Đề xuất | operationId |
| --- | --- | --- |
| GET `/ca/{caId}/du-lieu-nguon` | GET `/shifts/{shiftId}/source-data` | `getCareShiftSourceData` |
| GET `/ca/{caId}/cong-viec` | GET `/shifts/{shiftId}/care-tasks` | `listShiftCareTasks` |
| POST `/ca/{caId}/cong-viec/lap` | POST `/shifts/{shiftId}/care-tasks/plan` | `planShiftCareTasks` |
| POST `/cong-viec/phat-sinh` | POST `/care-tasks/ad-hoc` | `createAdHocCareTask` |
| POST `/cong-viec/{id}/lam-lai` | POST `/care-tasks/{id}/rework` | `createCareTaskRework` |
| GET `/cong-viec/{id}` | GET `/care-tasks/{id}` | `getCareTask` |
| POST `/cong-viec/{id}/huy` | POST `/care-tasks/{id}/cancel` | `cancelCareTask` |

Tiền tố vẫn `/api/v1`. Mỗi operation đánh `x-implementation-phase: 4` theo cách Phase 3. Chỉ sinh client của phần đã duyệt. Những operation `/nhan-viec`, `/phan-lai`, `/qua-han`, ghi nhận/checklist giữ cho story tương ứng, không tự đưa vào phạm vi này.

## 2. Trường JSON và kiểu

`CongViecGhi` → `CareTaskWrite`, giữ đúng bốn trường required hiện có: `taskTypeId`, `basis`, `plannedAt`, `importance`.

| Trường cũ | Trường mới |
| --- | --- |
| `nctId` | `residentId` |
| `viTri {loai,id}` | `location {type,id}` |
| `loaiCongViecId` | `taskTypeId` |
| `canCu {loai,id}` | `basis {type,id}` |
| `noiDung` | `content` |
| `thoiDiemDuKien` | `plannedAt` |
| `khungTruocPhut`, `khungSauPhut` | `windowBeforeMinutes`, `windowAfterMinutes` |
| `mucQuanTrong` | `importance` |
| `nguoiThucHienId` | `performerId` |
| `congViecChung` | `shared` |

Lập lô nhận mảng `CareTaskWrite` không rỗng và trả mảng `CareTask`; không thêm wrapper/payload tự điền. Phát sinh nhận một `CareTaskWrite`. Làm lại giữ required `qualityCheckResultId`, `plannedAt`; hai trường tùy chọn `performerId`, `shared`. Hủy nhận `{reason}` dùng `Reason` đã duyệt của Phase 3.

`CongViec` → `CareTask`, ánh xạ `id` giữ nguyên; các trường cùng tên cũ ở bảng trên dùng cùng tên mới. Những trường riêng của phản hồi:

| Trường cũ | Trường mới |
| --- | --- |
| `theNguoiCaoTuoi {hoTen,anh,phongGiuong,luuYChamSoc}` | `residentCard {fullName,photo,roomBed,careNotes}` |
| `viTri`, `loaiCongViec`, `nguoiThucHien` (chuỗi hiển thị) | `location`, `taskType`, `performer` (chuỗi hiển thị) |
| `dauKhung`, `cuoiKhung` | `windowFrom`, `windowTo` |
| `dauTon` | `carryover` |
| `trangThai` | `status` |
| `choXacNhanDoLai` | `awaitingRemeasurementConfirmation` |
| `soLanNhac` | `reminderCount` |
| `lenhDuocPhep` | `allowedCommands` |

`CongViecChiTiet` → `CareTaskDetail`, giữ allOf với `CareTask`: `ketQua` → `result`; `lyDo` → `reason`; `dongBoi` → `closedBy`; `quyTacDong` → `closureRule`; `lichSuXuLy` → `handlingHistory` gồm `type`, `actor`, `basis`, `at`, `reason`; `truongBiAn` → `hiddenFields`.

Projection `KetQua` → `CareTaskResult` chỉ dịch các trường đã có: `id`, `taskId`, `resultType`, `currentValue`, `corrected`, `recordingCancelled`, `performedAt`, `recordedAt`, `deviceAt`, `lateRecording`, `performer`, `permissionBasis`, `vitalSignRecordId`. Chưa triển khai lệnh ghi nhận của Phase 6. Các enum permissionBasis dịch sang `ASSIGNED`, `FLOOR_MANAGER_ROLE`, `SHIFT_LEAD_DUTY`, giữ nguyên ba ý nghĩa.

`DuLieuNguonCa` → `CareShiftSourceData`: `ca` → `shift` gồm `id`, `startsAt`, `endsAt`, `floorAreaId`; `nguoiCaoTuoi` → `residents`, từng dòng gồm `residentId`, `attendance`, `assignments`, `planItems`, `otherSources`. `assignments` giữ object mở như thiết kế, không thêm field bắt buộc; `planItems` dùng `CarePlanItem` đã duyệt. `otherSources` gồm `type`, `id`, `at`, `description`, là dữ liệu đã có, không phải công việc đề xuất.

`LoiTheoDong` → `CareTaskRowProblem`: dùng trường RFC 9457 đã dịch (`errorCode`, `unmetConditions`); `loiTheoDong` → `rowErrors`, mỗi phần tử `{row, codes}`. Chưa có quy định đánh số dòng trong YAML: đề xuất `row` bắt đầu từ **1** tương ứng hàng người dùng nhìn thấy, cần duyệt để client/server cùng hiểu. Giữ HTTP 409 và tính nguyên tử cả lô.

Enum sử dụng tên tiếng Anh đã công bố ở `TaskStatus`, `Importance`, `TaskSourcePort.SourceType`; không tự thêm giá trị. Loại vị trí: `ROOM`, `BED`, `FLOOR`, `AREA`. Dấu tồn: `FROM_PREVIOUS_SHIFT`, `AWAITING_CONFIRMATION`. Mã lỗi research C-07 dịch tương ứng: `RESIDENT_NOT_STAYING`, `RESIDENT_ABSENT`, `DAY_CARE_NOT_PRESENT`, `OUTSIDE_SHIFT`, `PLAN_ITEM_MISMATCH`, `INVALID_BASIS`, `MEDICATION_IS_NOT_A_TASK`, `INVALID_PERFORMER`, `MISSING_CERTIFICATE`, `DUPLICATE_BASIS`, `OLD_REVISION_WINDOW_OVERLAP`, `SHIFT_CHECK_ALREADY_EXISTS`. Đây là đề xuất, chưa phát hành mã lỗi HTTP mới.

## 3. Hai bổ sung SPI tối thiểu cần duyệt

Hợp đồng hiện tại đủ **kiểm** căn cứ đã biết nhưng không đủ **liệt kê** nguồn thật cho T025. `TimetableSourcePort.onDate` thiếu id căn cứ nên không thể dùng chuỗi mô tả để đoán id. Không gọi API trực tiếp 007/008, không SQL bảng của module khác, không trả nguồn rỗng giả như đã tích hợp.

Đề xuất thêm vào `TaskSourcePort`, không sửa chữ ký cũ:

```java
List<SourceView> sources(long residentId, Instant from, Instant to);

record SourceView(SourceType type, long id, Instant at, String description) {}
```

Khoảng `[from,to)` là ca; `residentId` là người đang đọc. Mỗi bean trả dữ liệu thực thuộc `managedTypes()` của mình. Không tạo/đề xuất công việc. Chủ nguồn D (007)/D (011)/C (014) cài theo nguồn sở hữu; `FakeTaskSource` hiện có của 005 mở rộng trong cùng thay đổi, không tạo fake thứ hai hoặc default trả rỗng để che provider thiếu.

Đề xuất thêm vào `ShiftStaffPort` để kiểm vai trò người nhận kiểm tra đầu ca/giám sát gắn vị trí, không có resident để hỏi `assignments(residentId,shiftId)`:

```java
List<ShiftMemberView> members(long shiftId);

record ShiftMemberView(long staffId, Role role) {}
```

Chỉ trả người còn có mặt trong lịch ca đã công bố; `onShift`/`qualified` vẫn kiểm thời điểm và chứng chỉ theo chữ ký cũ. E (008) cài nguồn thật; fake hiện có `FakeShiftStaff` do 005 sở hữu bổ sung fixture. Không cài thay module E hoặc dùng `CareShiftPort` cũ làm bằng chứng đã có `ShiftStaffPort`.

Đây là **Đổi api** đã được chủ dự án duyệt theo AGENTS.md; cập nhật `contracts/module-interfaces.md`, glossary mục 11, fake hiện có và dòng chờ chủ nguồn ở team-plan trong cùng thay đổi. Chỉ áp dụng hai bổ sung đã duyệt ở mục này, không áp dụng mục 5 khi chưa có câu trả lời.

## 4. Phần độc lập đã chọn làm trước

T018: `CareTaskTransitions` nằm `care/internal/task/domain`, dùng `TaskStatus`/`Importance` đã có. Hàm thuần trả `Optional.empty()` khi cặp trạng thái/sự kiện bị cấm; không phát hành mã lỗi hoặc endpoint mới. Kiểm quyền, lý do, nội dung và tác động giao dịch vẫn thuộc service của T023. `initialStatus` nhận thời điểm từ caller; không đọc đồng hồ hệ thống hoặc sinh công việc.

Không coi đây là T018 hoàn tất: còn test/hàm kiểm từng dòng T022 và IT T019. Phase 3 vẫn có gate outbox/A/B đã ghi ở plan; không đóng hoặc sửa những gate đó bằng phần thuần của Phase 4.

## 5. Bổ sung phát hiện khi cài T024 — đã chọn hoãn

Mục 1–3 đã được chủ dự án duyệt bằng câu trả lời “1” ngày 2026-11-01. Đang áp dụng bản dịch HTTP và hai SPI đã duyệt.

Job nhắc ca chưa có công việc cần đọc mọi ca đã bắt đầu nhưng còn đang diễn ra, kể cả tầng/khu chưa có giường hoặc chưa có công việc. `publishedShifts(floorAreaId,from,to)` cần biết trước floorAreaId; không API nào hiện có liệt kê đầy đủ mọi tầng/khu có ca. Không dùng id 0 làm sentinel hoặc lấy tầng từ công việc đã có vì sẽ bỏ sót chính ca cần nhắc.

Đề xuất thêm vào ShiftStaffPort, giữ chữ ký cũ:

```java
List<ShiftView> ongoingShifts(Instant at);
```

Trả toàn bộ ca đã công bố thỏa startsAt <= at < endsAt, không tự tạo ca. E008 cài nguồn thật; FakeShiftStaff hiện có lọc fixture. Job chỉ đọc nguồn này để nhắc một lần mỗi ca qua ReminderMarkers, không tạo công việc. Chưa sửa SPI theo mục 5 trước khi được duyệt.

## 6. Client mobile cho T026 — đề xuất đã chọn chờ A

`mobile/lib/common/api/api_client.dart` hiện chỉ post Map và chỉ trả Map; endpoint lập lô bắt buộc body/response dạng mảng. ApiProblem hiện bỏ rowErrors nên app không thể gắn lỗi đúng dòng. Không đổi HTTP thành wrapper để né thiếu sót client.

Đề xuất thay đúng file nền này: thêm `Future<Object?> requestJson(String method, String path, {Object? body, Map<String,String>? headers})` dùng lại transport/token/deviceId hiện có; get/post cũ giữ chữ ký và ủy quyền sang hàm mới rồi giữ cách trả Map như trước. ApiProblem thêm tham số positional tùy chọn thứ tư `Map<String,dynamic> properties = const {}` để giữ Problem Details; mọi constructor ba tham số cũ giữ tương thích. Không thêm thư viện, không đổi API backend.

005 thêm test riêng `mobile/test/staff/care_tasks/care_task_transport_test.dart` kiểm JSON array request/response, Idempotency-Key, rowErrors và lời gọi get/post cũ. Chủ dự án cần cho phép C sửa file nền A và bàn giao A review trước merge theo AGENTS.md mục 2.2/6; nếu không, chỉ có thể kiểm widget với gateway được tiêm và ghi T026 còn chờ adapter production.

## 7. Quyết định của chủ dự án sau các đề xuất — 2026-11-01

- Mục 5: chọn phương án 2, giữ SPI; không thêm ongoingShifts. T024 nhắc ca chưa có công việc chờ nguồn đầy đủ từ E.
- Mục 6: chọn phương án 2, không sửa ApiClient của A. Widget/gateway được kiểm độc lập; adapter production và đăng ký màn mobile chờ A.
- Cảnh báo công việc MANDATORY gắn vị trí: chủ dự án chọn chờ thống nhất với 007. AlertRequested hiện yêu cầu residentId; không gán id giả, không đổi event khi chưa có hợp đồng được duyệt. Nhánh hiện trả503 và có thể rollback cả lượt chạy bù, vì vậy T024 còn gate nghiệp vụ/hợp đồng.

Các mục 5–6 là đề xuất đã được chọn hoãn, không phải thay đổi đã duyệt. Chỉ mục 1–3 được áp dụng trong lượt này.
