# Đề nghị duyệt hợp đồng Phase 6 — 2026-11-01

Trạng thái: **chủ dự án đã duyệt bằng câu trả lời “1” ngày 2026-11-01**. Theo AGENTS.md mục 2.5 và CLAUDE.md, bản dịch được duyệt trước khi sửa YAML, controller và client. Chỉ dịch sáu operation của T039; không thay nghiệp vụ, phương thức, mã HTTP, required, Idempotency-Key hoặc ETag.

## 1. Operation

| Hiện tại | Đề nghị |
| --- | --- |
| POST /cong-viec/{id}/nhan-viec | POST /care-tasks/{id}/claim |
| GET /checklist?caId= | GET /checklist?shiftId= |
| POST /cong-viec/{id}/ghi-nhan | POST /care-tasks/{id}/record |
| POST /cong-viec/{id}/khong-thuc-hien | POST /care-tasks/{id}/not-perform |
| POST /ghi-nhan-phat-sinh | POST /ad-hoc-care-recordings |
| GET /nguoi-cao-tuoi/{nctId}/ket-qua-ghi-nhan?tu=&den=&loai= | GET /residents/{residentId}/care-results?from=&to=&types= |

Giữ response record 200/202/403/409/422; not-perform 200/409; claim 200/409; ad-hoc 200 (chỉ trả công việc cùng loại, chưa ghi)/201/409; checklist và history 200. Các lỗi từ kiểm quyền/validation vẫn dùng cơ chế RFC 9457 chung. Không thêm operation đính chính của Phase 8 vào Phase 6.

## 2. Request và response

- `GhiNhan` → `CareResultWrite`: `thoiDiemThucHien` → `performedAt`, `giaTriSo` → `numericValue`, `giaTriMa` → `codeValue`, `giaTriJson` → `jsonValue`, `xacNhanVuotGioiHan` → `upperLimitConfirmed`, `ghiChu` → `note`, `lyDo` → `reason` (schema Reason đã có). Giữ tính tùy chọn của trường; service kiểm giá trị theo resultType của task.
- Không thực hiện: `loaiLyDoId` → `reasonTypeId` (bắt buộc), `ghiChu` → `note` (maxLength 2000).
- Phát sinh: kế thừa CareResultWrite; `nctId` → `residentId`, `loaiCongViecId` → `taskTypeId` (đều bắt buộc); `daXemCongViecCungLoai` → `sameTypeTasksReviewed`, `congViecGocId` → `originalTaskId`. Response 200: `congViecCungLoai` → `sameTypeTasks`, mỗi phần tử dùng CareTask đã được duyệt ở Phase 4.
- Checklist: giữ tên schema `Checklist`; `caId` → `shiftId`, `taiLuc` → `loadedAt`, `congViec` → `tasks` (CareTask), `thoaThuan` → `riskAgreements` (mỗi phần tử: `residentId`, `agreements` dùng RiskAgreement), `loaiLyDoKhongThucHien` → `nonPerformanceReasonTypes`, `lieuThuoc` → `medicationDoses`, `canhBaoDangMo` → `openAlerts`, `viecBanGiao` → `handoverTasks`. Giữ object additionalProperties ở ba nguồn cuối; không thêm trường ngoài hợp đồng.
- Response ghi nhận dùng CareTaskDetail và CareTaskResult đã dịch ở Phase 4. Lịch sử trả mảng CareTaskResult, giữ nguyên các trường đã duyệt: id, taskId, resultType, currentValue, corrected, recordingCancelled, performedAt, recordedAt, deviceAt, lateRecording, performer, permissionBasis, vitalSignRecordId.
- Lỗi task đã đóng: dùng schema lỗi tiếng Anh đã có, dịch trường riêng `dongBoi` → `closedBy`, `dongLuc` → `closedAt`, `trangThai` → `status` (CareTaskStatus), `noiDungBiTuChoi` → `rejectedContent` (CareResultWrite); mã `CONG_VIEC_DA_DONG` → `CARE_TASK_CLOSED`. Các mã kết quả: `GIA_TRI_BI_CHAN` → `VALUE_BLOCKED`, `CHO_DO_LAI` → `AWAITING_REMEASUREMENT`, `CHUA_XAC_NHAN_PHUC_VU` → `SERVICE_NOT_CONFIRMED`, `CAN_XAC_NHAN_LAI` → `CONFIRMATION_REQUIRED`.

## 3. Giới hạn và tích hợp

Không đề nghị thêm thư viện, sửa common, thay chữ ký API/SPI công khai. T039 cài VitalSignTaskApi/PublicCareResultApi hiện có; T037 dùng OfflineHandler hiện có. Nếu rà triển khai phát hiện thiếu hợp đồng thì lập đề nghị riêng trước khi đổi.

Các gate cũ về nguồn thật B/D/E, cảnh báo vị trí và transport mobile A vẫn còn. Không dùng fake trong production hoặc tự sửa mobile common để vượt gate. Khi merge, ưu tiên listener/kiểu sự kiện tương ứng của B/E theo ghi chú Phase 5 đã được chủ dự án chốt, tránh hai listener xử lý cùng sự kiện.

## 4. Nội dung phong bì ngoại tuyến T037 — đề nghị duyệt bổ sung

**Chủ dự án chọn “2” — giữ lại để điều chỉnh; không được áp dụng mục này.** Hợp đồng `/sync` của 002 để `content` là object tùy loại; hai mã loại M04 đã có ở C-14, nhưng chưa mô tả các trường nội dung. Đề nghị chưa duyệt dưới đây không đổi endpoint/envelope/OfflineHandler của A; chưa tạo handler hoặc nối app theo payload này:

- `M04.KET_QUA_CONG_VIEC`: `{taskId, action, recording?, reasonTypeId?, note?}`; taskId và action bắt buộc, action là `RECORD` hoặc `NOT_PERFORM`. RECORD dùng `recording` là CareResultWrite đã duyệt; NOT_PERFORM dùng reasonTypeId bắt buộc và note tùy chọn như HTTP. Không chấp nhận trường deviceAt do client đưa vào content; khung 002 truyền deviceTime vào handler.
- `M04.GHI_NHAN_PHAT_SINH`: dùng cùng body `/ad-hoc-care-recordings` đã duyệt: `{residentId, taskTypeId, performedAt?, numericValue?, codeValue?, jsonValue?, upperLimitConfirmed?, note?, reason?, sameTypeTasksReviewed?, originalTaskId?}`. residentId/taskTypeId bắt buộc. Khung 002 cung cấp thời điểm thiết bị và kết quả quyền; không thêm trường envelope.
- Giữ rejectedContent ở hàng đợi local. Phản hồi `/sync` hiện chỉ có result/reason/reference; app đọc chi tiết task qua GET đã có khi cần người đóng/lịch sử đóng, không tự thêm properties vào response của A.
- Loại lý do còn hiệu lực tại deviceTime: đọc nhật ký của chính M04 qua AuditLogApi đã có để xác định tạo/ngừng, không SQL bảng chung hoặc xin thêm CatalogApi. Dòng cố định không được ngừng theo T007a. Khi thiếu bằng chứng cho loại không cố định, từ chối rõ ràng; không suy trạng thái hiện tại thành trạng thái quá khứ.

Gate nghiệp vụ đã chốt: PublicCareResultApi.summary chờ thống nhất công thức với E/012 theo lựa chọn “1” của chủ dự án; không tự quy đổi mức ăn hoặc chọn mẫu số.

## 5. Loại kết quả snapshot cho form T040/T041 — đã duyệt

**Chủ dự án đã duyệt (lựa chọn 1).** CareTask trước thay đổi chỉ có `taskType` là tên hiển thị, không có loại kết quả để dựng form. Ghép theo tên với danh mục hiện tại sai khi danh mục đổi tên/đổi loại kết quả, trái FR-038 (task giữ snapshot).

Đề nghị thêm duy nhất trường chỉ đọc `CareTask.resultType`, enum `EATING|ML|MEASURED_VALUE|MOVEMENT|SLEEP|MOOD|BEHAVIOR|ITEM_QUANTITY|PERFORMED|NONE`, lấy từ CareTaskData.resultType đã lưu lúc lập. Không thêm trường request, không đổi chữ ký API/SPI hoặc endpoint. CareTaskDetail kế thừa trường này, checklist và bảy operation Phase4 dùng cùng schema được cập nhật tương ứng. Client cũ có thể bỏ qua trường mới. App/web dùng đúng snapshot để dựng ô nhập; không suy theo tên loại công việc hoặc danh mục hiện tại.

Các giới hạn số vẫn do backend kiểm; client chỉ hỏi xác nhận vượt giới hạn khi nhận CONFIRMATION_REQUIRED. Danh mục mã ngủ/tâm trạng/hành vi dùng API result-values đã có; phần mobile trả mảng vẫn chờ transport A như gate cũ, không sửa common.

Nhánh đo chỉ số phát sinh: chủ dự án chọn “1”, chờ D/007 thống nhất trạng thái và phản hồi khi AWAITING_REMEASUREMENT; chưa mở rộng response 202 của endpoint phát sinh.

## 6. Các nhánh tiếp tục chờ thống nhất — quyết định 01/11/2026

- Chủ dự án chọn chờ E/010 cho quantity của BillableCareResultRecorded. Không chốt quy đổi ml/phút/giá trị đo, cũng chưa chốt mặc định một lần dịch vụ. Lệnh billable trả CARE_SOURCE_UNAVAILABLE trước lưu; không phát sự kiện với công thức tự đặt, không lưu thành công rồi bỏ sự kiện.
- Chủ dự án chọn tiếp tục chờ D/007 cho callback đo lại khác người/thời điểm gốc, giữ chữ ký hiện có. Không mở rộng callback trong lượt này. Callback cùng metadata gốc được kiểm riêng; metadata khác bị chặn rõ ràng, chưa coi toàn bộ callback đo lại đã hoàn tất.
- Nhận việc tồn của ca sau: chủ dự án chọn chờ E/008 ở Phase7 vì C10/Q93 cần chốt ca/thời điểm kiểm người nhận. Không tự đổi sang ca hiện tại trong lượt Phase6. Checklist chỉ hiển thị lệnh mà kiểm service hiện có cho phép.
