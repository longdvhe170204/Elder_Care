# Rà soát task còn lại của Spec 003 — 2026-10-25

## Phạm vi và cách đọc

Đối chiếu `tasks.md`, ghi chú triển khai trong `plan.md`, `verification.md` và source hiện có. Đây là lượt rà soát, không triển khai thêm, không chạy lại build/test và không đánh checkbox. Kết quả kiểm dưới đây là bằng chứng của lượt Phase 11 gần nhất, không phải kết quả chạy mới.

`tasks.md` có 65 task: 20 task `[x]`, **45 task `[ ]`**. Phase 1–2 không còn task bỏ trống; Phase 6 đã đánh T037, T041. Checkbox trống không đồng nghĩa chưa viết code. Một task có thể đồng thời còn phần chưa cài, nguồn tích hợp chưa có và gate nghiệm thu chưa đạt.

Gate chung: lượt nền `mvnw.cmd clean verify` gần nhất kết thúc exit 1; 393 test đơn vị/kiến trúc xanh, 1073 IT có 6 failure + 9 error, không skip; 227 IT M03 xanh. 15 lỗi thuộc 8 lớp M01 đã nêu trong `verification.md` và `Working-history.md`. Frontend 250 test xanh, lint/build qua. Theo yêu cầu cuối phase phải clean verify qua trước khi đánh `[x]`, chưa thể đóng các task chỉ mới qua kiểm hẹp. Các lỗi toàn kho này không giải thích thay cho những chức năng còn thiếu bên dưới.

## Danh sách đủ 45 task

### Phase 3 — 3 task

| Task | Thực tế và lý do còn mở |
|---|---|
| T012 | Test chính sách phòng/đổi cha đã được bổ sung qua `FR_006_RoomPolicyIT`, `FR_006_RoomPolicyConcurrencyIT` và các test cấu trúc. Lý do chờ API M01 ban đầu đã được giải quyết cho phần này; còn gate chung, không nên tiếp tục ghi là chưa có test. |
| T015 | `StructureService` đã cài CRUD, kiểm chính sách/tham chiếu, có kiểm hẹp xanh. Bản thật `LocationReferences` từ 008 chưa có để nghiệm thu tích hợp; test dùng fake của hợp đồng. Còn gate chung, không phải toàn bộ service chưa cài. |
| T017 | Controller và API nội bộ phần cấu trúc đã có. Nhánh đổi phòng của giường chưa từng phân bổ vẫn trả 503 vì chưa có thao tác đồng bộ vị trí tài sản được duyệt; giường đã có phân bổ bị chặn theo FR-007. Cần chốt hợp đồng tài sản cho nhánh còn thiếu và qua gate chung. |

### Phase 4 — 7 task

| Task | Thực tế và lý do còn mở |
|---|---|
| T019 | `BR_M03_01_AllocationConditionsIT` đã kiểm checker; nguồn M01 cần cho trạng thái/giới tính/mức đã nối. Còn gate chung; nguồn 004/007/019 thật dành cho nghiệm thu tích hợp T063, không được gọi phần test dùng fake hợp đồng là chưa viết. |
| T020 | Đã có test schema chống chồng, khóa/retry và 10 transaction tranh một giường trong nhóm `FR_020_*`. Còn gate chung. |
| T021 | Đã có test phân bổ ban đầu, quyền/query/HTTP và participant trở về chưa có giường trong các nhóm `FR_023_*`, `FR_022_*`, `FR_024_*`. Còn gate chung và kiểm với các nguồn thật ở T063. |
| T025 | `AllocationChecker`, `AllocationLocks` và retry đã cài, kiểm hẹp xanh. Còn gate chung; không còn bị chặn bởi việc thiếu API M01 như ghi chú trước merge. |
| T026 | `AllocationCommands`, query/location API đã có; tạo/hủy và liên kết participant được kiểm. Còn gate chung và nghiệm thu nguồn lưu trú/tài sản thật. |
| T027 | HTTP danh sách/tạo/chi tiết/hủy/giường phù hợp/vị trí/tổng hợp đã cài, được `ApiContractIT` kiểm. Còn gate chung; endpoint chuyển thường thuộc T036 vẫn thiếu, không gộp thành toàn bộ T027 chưa làm. |
| T028 | Web `BedAllocationForm`, `BedAllocationsPanel`, bảng giường và test đã có; frontend kiểm xanh. Còn gate cuối phase, dữ liệu backend thật đầy đủ cho nghiệm thu liên thông. |

### Phase 5 — 8 task

| Task | Thực tế và lý do còn mở |
|---|---|
| T029 | Chưa có bộ test đầy đủ cho chuyển thường và hai lệnh chuyển thường đồng thời. Test `ControlledTransferService` ở Phase 9 chỉ chứng minh luồng ngoại lệ kiểm soát lây nhiễm, không thay thế task này. Cần T032. |
| T030 | Chưa có kiểm trọn luồng giữ trước/áp dụng chuyển theo lịch/chờ sẵn sàng/thất bại hủy. Thiếu implementation T033; tích hợp yêu cầu lưu trú 004 thật sẽ làm sau. |
| T031 | Đã có kiểm đánh giá lại phù hợp, nhưng chưa đủ chuyển gấp khác giá, kết quả yêu cầu sau chuyển và workflow nhập bù. Cần T032–T035; không thể dùng test schema nhập bù để coi workflow đã đạt. |
| T032 | **Chưa cài chuyển giường thường**. Hiện chỉ có `ControlledTransferService` cho Phase 9. Đây là phần implementation của chính 003; checker/locks và API M01 cơ bản đã có để tiếp tục trong phạm vi hợp đồng hiện hành. |
| T033 | **Chưa có bean thực thi `BedAllocationCommandApi`** và các lệnh chuyển theo lịch/kết quả yêu cầu sau chuyển gấp. Interface đã khai báo; không phải chờ người khác khai báo thay 003. Cần T032 và nối 004 cho kiểm tích hợp sau đó. |
| T034 | Migration `V003_04__bed_backfill_request.sql` và test schema đã có; **handler `M03.NHAP_BU_PHAN_BO` chưa cài**. Cần lệnh chuyển và khoảng vắng. `ResidentStatusApiImpl.statusPeriods()` của M01 hiện ném `UnsupportedOperationException`, cần B hoàn thiện nguồn này. |
| T035 | `AllocationSuitabilityListener` đã nghe 3 sự kiện M01: `CareLevelChanged`, `InitialAssessmentConfirmed`, `PersonalInfoUpdated`; service/marker đã kiểm. Còn listener sự kiện đổi loại lưu trú do 004 cung cấp, và gate chung. |
| T036 | Contract chuyển thường đã dịch/duyệt; web form/panel đã có. **`POST /bed-allocations/{id}/transfer` chưa có controller**, vẫn nằm trong pending của `ApiContractIT`; cần T032/T034. Mobile nhân viên chưa được mở theo CLAUDE.md, giữ phần đó chưa làm. |

### Phase 6 — 6 task

| Task | Thực tế và lý do còn mở |
|---|---|
| T038 | Nhiều ca trạng thái/giữ chỗ/tài sản đã xanh. Nhánh báo hỏng có phân bổ tương lai gắn yêu cầu/căn cứ vẫn trả 503, chưa thực hiện toàn bộ FR-013a; test hiện ghi rõ nhánh chưa có workflow, không chứng minh thành công. Cần luồng yêu cầu/chuyển theo lịch và nguồn tài sản/lưu trú thật. |
| T039 | Participant 7 lệnh M01, rollback/race đã có test xanh. Còn gate chung và nguồn thật 004/007/019. Các vấn đề transaction/lỗi kiểm M01 đã bàn giao B, không tự sửa M01 theo quyết định hiện hành. |
| T040 | `BedHoldService` và các tác động giữ/giải phóng đã có. Chưa xuất đủ lệnh qua bean `BedAllocationCommandApi` vì facade T033 chưa cài; cần nối các thao tác này vào bản thật và kiểm nguồn 004. |
| T042 | Adapter/service trạng thái tài sản đã có. Ngoại lệ hủy phân bổ tương lai gắn căn cứ/yêu cầu chưa nối, trả 503 để giữ nguyên dữ liệu; cần workflow tương ứng và phần tài sản 019. **019 thuộc C**, không được ghi chung là chờ teammate khác. |
| T043 | `AllocationStatusParticipant` đã cài, có test các lệnh/rollback/trở về phòng chịu chặn. Còn gate chung và nghiệm thu với nguồn thật; không còn lý do “chưa có interface trạng thái M01”. |
| T044 | Hai job hết hạn giữ chỗ/báo quá bắt đầu đã cài, kiểm trực tiếp bằng đồng hồ nghiệp vụ. Còn gate chung và callback 004 thật. Chủ dự án không duyệt mở enum `runJobs` của API 000: chưa chạy chúng qua `POST /testing/clock`; đây là giới hạn harness, không phải chưa có job. |

### Phase 7 — 4 task

| Task | Thực tế và lý do còn mở |
|---|---|
| T045 | Test lịch sử, quyền, đính chính đã có. Chưa kiểm trọn dữ liệu thật vì M01 `statusPeriods()` chưa cài, workflow nhập bù chưa có, receiver 007 chưa có. Còn gate chung và nghiệm thu/đo theo kịch bản đầy đủ; test hẹp không thay toàn bộ SC-003. |
| T046 | `LocationHistoryQueries`/API lịch sử đã cài. Đọc khoảng vắng thực tế gọi `ResidentStatusApi.statusPeriods()` đang chưa được B cài; dấu yêu cầu nhập bù thật cũng cần T034. Còn gate chung. |
| T047 | Provider đính chính phân bổ/xác nhận sẵn sàng và phát sự kiện đã cài/test. Receiver 007 thật chưa có để xác minh xử lý tiếp; còn gate chung, không phải provider chưa viết. |
| T048 | 5 operation HTTP và web lịch sử/đính chính đã có. Dấu nhập bù theo từng giường trên HTTP chưa biểu diễn đủ vì đề nghị thêm `pendingBackfill` vào `BedLocationHistoryRow` đã bị từ chối; còn QA web theo ghi chú phase, nguồn khoảng vắng thật và gate chung. |

### Phase 8 — 4 task

| Task | Thực tế và lý do còn mở |
|---|---|
| T049 | Test sức chứa, khung giờ, tham số, UNIQUE và chặn ngừng khi có lịch tương lai đã được thực hiện, kiểm hẹp xanh. **Không cần chờ spec khác để viết tiếp task này**; hiện còn gate chung/nghiệm thu, không phải chưa có test. |
| T050 | Migration `V003_05__day_care_capacity.sql` đã có và kiểm schema xanh. **Không còn phần migration cần chờ spec khác**; còn gate chung để đóng task theo yêu cầu. |
| T051 | `DayCareCapacityService`, đếm lại/khóa/cảnh báo và API sức chứa đã cài, test với fake hợp đồng. Bản thật `DayCareArrivalCounter` từ 004 chưa có để chạy dữ liệu thật; còn gate chung. |
| T052 | Backend 9 operation và UI xem/tạo/đếm đã có. UI sửa/ngừng chưa đủ vì contract yêu cầu `If-Match` nhưng response thiếu version, đề nghị bổ sung đã bị từ chối. Lỗi ngừng khu nghỉ chưa liệt kê residentId do SPI chỉ trả boolean; đề nghị mở SPI cũng bị từ chối. Cần chốt cách đáp ứng hợp đồng/nghiệp vụ trước khi hoàn thiện, không chỉ chờ 004. |

### Phase 9 — 4 task

| Task | Thực tế và lý do còn mở |
|---|---|
| T053 | Test cách ly/quyền/chuyển kiểm soát/trở về đã có và xanh. Chưa đủ nhánh gỡ khoanh vùng/thêm tiếp xúc bằng sự kiện thật 007 vì chưa có các kiểu API/fake sự kiện; còn gate chung. |
| T054 | Migration, lệnh đặt/gỡ, audit và sự kiện nguồn đã cài. **Hai listener gỡ khoanh vùng/thêm danh sách tiếp xúc chưa cài** do 007 chưa cung cấp kiểu sự kiện công khai/fake; thuộc D, incident/health. |
| T055 | `ControlledTransferService` và xác nhận ở lại đã cài, có test atomicity/rollback/Bác sĩ Hoạt động/giữ hạn khi vắng. Còn gate chung và kiểm nguồn 004/007 thật, không phải chờ T032 để viết luồng ngoại lệ đã có. |
| T056 | HTTP và web cách ly/chuyển/xác nhận đã cài; test và QA giao diện đã thực hiện trong Phase 9. Còn gate chung/tích hợp thật; mobile nhân viên chưa được mở. |

### Phase 10 — 4 task

| Task | Thực tế và lý do còn mở |
|---|---|
| T057 | Test xác nhận thường, quyền, khử khuẩn/bảo hộ, nhắc/rollback đã xanh. Chưa có đường thành công “xác nhận rồi chuyển ngay theo lịch” với provider thật do T032–T033 thiếu; còn gate chung. |
| T058 | `BedReadinessService` đã cài xác nhận thường và kiểm nguồn lây nhiễm. Nhánh đang chờ gọi `BedAllocationCommandApi.applyScheduledTransfer()` nhưng chưa có bean; hiện 503/rollback, chưa hoàn tất chuyển tiếp. Đây trước hết là dependency nội bộ 003 T033; sau đó kiểm 004/007 thật. |
| T059 | Hai job nhắc đã cài/test xanh. Writer `AWAITING_REQUEST`/`AWAITING_SAME_PRICE` ở T032–T033 chưa có để tạo marker đúng lúc trong luồng thật; còn gate chung/kiểm liên thông. Enum clock 000 giữ nguyên theo quyết định trước, không phủ nhận job đã chạy trong test trực tiếp. |
| T060 | Hai HTTP operation đã cài/kiểm hợp đồng và sinh client. Mobile bảng giường/xác nhận cho nhân viên chưa được mở; backend không cần chờ mở mobile nhưng vẫn còn gate chung. |

### Phase 11 — 5 task

| Task | Thực tế và lý do còn mở |
|---|---|
| T061 | Ba API báo cáo/trạng thái/bàn giao đã cài, 7 IT reporting xanh; `BedFlagView.bedId` nullable đã được duyệt. Còn gate chung; giới hạn dữ liệu lịch sử cơ cấu/phòng/tài sản đã ghi ở `verification.md`, không thể coi snapshot hiện hành là lịch sử đầy đủ. |
| T062 | `ApiContractIT` đã đăng ký 003, kiểm endpoint đã cài; còn pending duy nhất chuyển thường T036. Kiểm trigger hai bảng nhóm ghi nhận đã bổ sung/xanh. Cần hoàn tất endpoint còn thiếu và gate chung, không xóa pending để giả hoàn tất. |
| T063 | **Chưa thể chạy với đủ bean thật**: 004→admission, 007→incident/health, 008→staffing hiện chỉ có package-info; chưa có các provider SPI cần thiết. 019 thuộc C/roombedallocation, phần tài sản billing qua API E; phần này cũng chưa có bản thật cần kiểm. Đây là task phụ thuộc liên feature rõ ràng. |
| T064 | Backend đã chạy nhưng gate đỏ 15 lỗi M01; web xanh. Flutter dừng trước test do Windows thiếu symlink/Developer Mode. Chưa chạy đủ 37 kịch bản demo hoặc đo SC-005 người dùng thực tế. Benchmark 300 giường qua MockMvc đạt <1 giây không thay phép đo 60 người dùng/10 thao tác/trung vị <2 phút trong quickstart. |
| T065 | `verification.md` đã có bảng truy vết và test tương ứng nhiều quy tắc. Còn khoảng trống workflow/chuyển thường/nhập bù và tích hợp đã chỉ ở task gốc; không thể đóng chỉ vì đã lập bảng mapping, cũng chưa qua gate chung. |

## Điểm cần phân biệt và thứ tự xử lý

1. **Việc chính 003 còn có thể tiếp tục**: bổ sung test T029–T031 trước implementation T032–T033; sau đó hoàn thiện phần handler/endpoint T034/T036 khi các điều kiện nguồn tương ứng có đủ. T040, T042, T057–T059 sẽ tháo được nhiều dependency nội bộ khi facade và workflow chuyển theo lịch có thật. Không đồng nhất lời mời tiếp tục này với phê duyệt sửa contract hoặc phần người khác.
2. **Nguồn người khác cần bàn giao**: B hoàn thiện M01 `statusPeriods()` và xử lý 15 lỗi kiểm M01; B cung cấp 004; D cung cấp nguồn/sự kiện 007; E cung cấp nguồn 008/phần tài sản liên quan. Fakes hợp đồng vẫn dùng được cho kiểm riêng, không thay cho T063.
3. **Quyết định chưa mở/đã từ chối**: đổi phòng giường cần đồng bộ tài sản; dấu nhập bù HTTP theo giường; version/UI và danh sách người có lịch bán trú; mở enum clock 000; mobile nhân viên chưa mở. Không tự suy yêu cầu rà soát thành duyệt những thay đổi này.
4. **Nghiệm thu**: sau các task nguồn mới chạy lại phạm vi liên quan, kiểm QA/demo/hiệu năng còn thiếu, cuối cùng clean verify nền. Chỉ đánh `[x]` khi đủ nội dung từng task và gate đã được yêu cầu; không phải chỉ sửa 15 lỗi M01 là mọi task còn lại tự hoàn tất.

Không có Phase 12 trong `tasks.md`. Không thay đổi code, contract, nghiệp vụ hoặc trạng thái 65 checkbox trong lượt rà soát này.
