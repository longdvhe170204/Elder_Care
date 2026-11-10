# Kiểm T026, T031, T040, T046 — 10/11/2026

Chủ C (`HungNX-nguyen`), nhánh `impl/019-inventory-assets`. Giữ nhánh và công việc chưa commit có sẵn (019, DTO care, DESIGN, tài liệu); không commit/push. Không sửa backend, API, migration, common, StaffRoot hoặc test của thành viên khác trong lượt này.

## Đối chiếu trạng thái hiện tại

| Task | Mã đã có, phần hoàn thiện lượt này | Phần chưa nghiệm thu và chủ xử lý |
| --- | --- | --- |
| T026 [ ] | Web/app lập lô và adapter `requestJson` đã có; không có nút chép sẵn. Sửa web đổi loại căn cứ không gửi kèm ID căn cứ cũ, đổi người/vị trí bỏ người thực hiện và căn cứ cũ; sửa web/gateway app đọc tên theo Profile.personalInfo.fullName sau finding từ API thật. Lỗi theo dòng và nguyên tử đã kiểm HTTP thật qua nguồn ca 008. | C kiểm trình duyệt ba bề rộng/sáng tối với dữ liệu và app trên thiết bị. Không còn chờ A cung cấp transport/shell, không còn thiếu bean `ShiftStaffPort`. Chưa kiểm UI lập công việc theo kế hoạch của người cao tuổi với dữ liệu thật. |
| T031 [ ] | Bốn endpoint, web/app đến/về/đính chính đã có. Sửa web/app xóa dữ liệu ngày trước khi đổi ngày, không giữ nút lệnh trên dữ liệu cũ khi tải/lỗi; app bỏ phản hồi đến muộn, hiện `ApiProblem.detail`; web dùng trạng thái chữ màu theo DESIGN. | C kiểm đến → có mặt → về với lịch 004 và bản ghi đón 012 thật, đính chính, trực quan/thiết bị. Người dùng đã duyệt trọn bộ dữ liệu hư cấu trên demo riêng. Đã kiểm HTTP thật đến200 → Có mặt → bản ghi đón012/ngoại lệ được quản lý duyệt → về200/Đã về, dùng lại409, đính chính201/giờ mới được áp dụng, Kế toán xem/ghi403. Còn C kiểm trực quan/thiết bị. T029 AddendumApplied thật vẫn là việc riêng, không chặn lại luồng đến/về vừa kiểm. |
| T040 [ ] | Checklist/cache, gateway, form theo loại, Không thực hiện, phát sinh, hàng đợi dùng chung đã có. Thêm `CareTaskContext`: phòng/giường, nội dung, dự kiến, khung giờ, mức, nguồn, người phụ trách, lưu ý, dấu tồn. Checklist chuyển khối việc sang các dòng bề mặt trắng có đường ngăn; Ghi nhận phát sinh app đọc tên từ personalInfo.fullName. Widget test 375 logical px, chữ 200%, theme tối; cache/offline/giữ nội dung bị từ chối có test sẵn. HTTP thật kiểm checklist có dữ liệu, `/sync` ACCEPTED và REJECTED. | C kiểm chuỗi mất mạng → SQLite → đồng bộ trên thiết bị. A/C bàn giao cách mở nội dung bị từ chối và lệnh đính chính/chuyển phát sinh: `SyncResultsScreen` hiện chỉ có kết quả/lý do, chưa có các lối mở này; `CareTask` không trả `taskTypeId` nên không suy ID bằng tên để dựng lệnh phát sinh. D/chủ dự án giải quyết nhánh đo phát sinh và metadata đo lại ở mục 8 team-plan. Chưa nghiệm thu ảnh nhận diện/dị ứng/thỏa thuận đầy đủ với nguồn thật. |
| T046 [ ] | Backend/web/app danh sách đã có. Dùng cùng `CareTaskContext` cho app quá hạn. Sửa mã dấu tồn HTTP thành `AWAITING_CONFIRMATION` trên cả web/app; giữ mã nội bộ khác tên. Có dữ liệu quá hạn qua HTTP thật của 005 + 008. | C kiểm danh sách/chi tiết trong trình duyệt ba bề rộng, sáng/tối và thiết bị. Không còn lý do giữ mở vì thiếu transport hoặc bean ca 008. |

## Những dòng mục 8 liên quan

- C tích hợp/E bàn giao `CareStaffLookup` và 12 hàm `ShiftStaffPort`, nguồn `ongoingShifts`, hai cờ bàn giao: nguồn đã có trong checkout. Lượt này kiểm nguồn ca bằng HTTP thật cho lập công việc/checklist, không nghiệm thu thay toàn bộ callback vắng/nghỉ/bàn giao.
- C adapter `DayCareCalendarPort` → B `DayCareCalendarApi`: đã có; `DayCareAttendancePort` cũng đã có. T029 còn IT chạy lệnh `ResidencyChangeHandler` thật và quyết định ngày bị bỏ khỏi lịch.
- D/C nhánh đo phát sinh `AWAITING_REMEASUREMENT` và cổng đo: còn thật. `DefaultVitalSignPort` dùng `care.api.VitalSignPort`; caller 005 dùng `care.api.spi.VitalSignPort`, thiếu mapping và metadata; không tự sửa.
- C retest lối Chăm sóc của StaffRoot: A đã review xong từ 07/11; accountId/queue/transport có thật. Phần còn lại là nghiệm thu trực quan/thiết bị, không chờ cài lại shell.

## TDD và kiểm tự động

Không thêm lớp backend/IT, không sửa test người khác, không đổi timeout hoặc nới assertion.

| Hành vi | RED trước sửa | GREEN sau sửa |
| --- | --- | --- |
| Web điểm danh đổi ngày | `FR_012_doiNgayAnTrangThaiCuTrongKhiTaiVaKhiLoi`: vẫn thấy Có mặt của ngày trước | 6/6 test màn điểm danh |
| Web đổi sang căn cứ Phát sinh | `FR_023_doiSangPhatSinhKhongGuiKemIdCanCuCu`: gửi `basis.id=90` | Nhóm care web 59/59 |
| Web dấu tồn chờ xác nhận | Bổ sung vào test FR-045 có sẵn: không thấy dấu `AWAITING_CONFIRMATION` | Nhóm care web 59/59 |
| App ngữ cảnh FR-030 | `FR_030_dayDuNguCanhCongViecKhiChu200PhanTram`: thiếu phòng/giường | 11/11 test checklist, gồm chữ 200% ở theme tối |
| App quá hạn | Bổ sung ngữ cảnh vào test FR-045 có sẵn: thiếu phòng/giường/nguồn/khung/dấu tồn | 4/4 test quá hạn |
| App đổi ngày/lỗi quyền | `FR_012_doiNgayKhongHienLaiTrangThaiCuKhiTaiHoacLoi`: còn dữ liệu ngày cũ | 4/4 test điểm danh |

Lượt RED đầu của test đổi người trên web bị HTML required chặn trước gửi nên không dùng làm RED hành vi; thay bằng kịch bản đổi sang Phát sinh tái hiện đúng payload thừa, rồi mới cài lại. Log RED/GREEN giữ ở `backend/logs/005/20261110-ui-*`.

Lượt kiểm trước sửa Profile: `flutter analyze` sạch; `flutter test` 506/506 xanh (`20261110-ui-flutter-{analyze,test}-final.log`). Web care 59/59. `npm run lint`, `npm run build` qua; build còn cảnh báo chunk lớn. Web toàn kho lần đầu sau sửa 945/947 xanh, hai test timeout tại App và AssignmentPanel khi chạy cùng Flutter; kiểm lại toàn kho bằng `npm test -- --maxWorkers=2`: **947/947 test, 161/161 file xanh**, 212 giây (`20261110-ui-web-test-limited.log`), không sửa mã/test/timeout ngoài 005. `git diff --check` qua. Dữ liệu demo được giữ; các kết quả ở đoạn này thuộc lượt trước finding Profile, không ghi thay cho lượt cuối bên dưới.

## Bằng chứng tích hợp demo thật

Backend profile demo trên MySQL `elder_care_c005`, không sửa migration hoặc ghi DB trực tiếp. Backend hiện tại được biên dịch từ checkout, không nạp `FakeM04Config`, không dùng test mock làm bằng chứng tích hợp. Web Vite chạy cổng 5173.

Chuẩn bị bằng lệnh công khai: khu/tầng `UI005`, hồ sơ nhân viên hư cấu Nguyễn Minh Kiểm, tài khoản demo riêng, giao Trưởng tầng, mẫu ca, lịch tháng 12/2026 và ca 2 ngày 10/12. Lịch tháng 11 chỉ Nháp vì không có Trưởng tầng phủ cả tháng; không vượt điều kiện `FLOOR_MANAGER_REQUIRED`. Tài khoản/mật khẩu tạm/token không ghi log; không thay quyền của tài khoản thật.

Kịch bản ca tháng 12 dùng `/testing/clock` đã có; khôi phục giờ máy sau kiểm bằng API. Phiên hết hạn khi tua giờ được đăng nhập lại. Sau lần finally đầu gặp phiên cũ hết hạn, đã khôi phục ngay bằng phiên mới; lần chạy đạt có finally đăng nhập mới và khôi phục thành công. Dữ liệu chỉ nằm trong schema demo riêng.

| Kịch bản | Kết quả thật |
| --- | --- |
| Công bố lịch qua 008, đọc source-data qua 005 | 200; nguồn `CareStaffLookup` thật |
| Gửi hai dòng, dòng 2 người thực hiện không hợp lệ | 409 `UNMET_CONDITIONS`, `rowErrors[0].row=2`, `INVALID_PERFORMER`, `MISSING_CERTIFICATE`; đọc lại danh sách rỗng |
| Gửi một dòng hợp lệ | 201; công việc 1 có nội dung Giám sát, người thực hiện và vị trí |
| Đọc quá hạn và checklist | 200; task 1 OVERDUE, dự kiến/khung ca/nguồn/người thực hiện; checklist cho phép record/notPerform |
| Đồng bộ kết quả PERFORMED | 200, kết quả từng dòng ACCEPTED, reference 1 |
| Gửi kết quả khác sau khi công việc đã đóng | 200, kết quả từng dòng REJECTED, lý do CARE_TASK_CLOSED kèm người và lúc đóng |

Chi tiết JSON không chứa token ở `backend/logs/005/20261110-demo-api.json`; script điều phối API tại `20261110-demo-check.mjs`. Đây là kiểm HTTP, **không phải bằng chứng thao tác UI hay mất mạng trên thiết bị**. Dữ liệu quá hạn được kiểm ở mốc giả lập, giờ demo đã trở về giờ máy.

T031, tiếp tục sau xác nhận của người dùng (“1”, cho phép hồ sơ hư cấu trên demo riêng): `20261110-daycare-check.mjs` đã tạo hồ sơ Trần An Kiểm id1 và lượt đăng ký bán trú id1 qua API (201), rồi đọc điều kiện tiếp nhận (200). `POST /day-care-attendances/check-in` trả503 CARE_SOURCE_UNAVAILABLE: “Chưa xác định vị trí khu bán trú”. Năm điều kiện Chưa đạt gồm INITIAL_ASSESSMENT_NOT_CONFIRMED, ADM_B_CONTRACT, ADM_C_DEPOSIT, ADM_E_CONTRACT_LEVEL, DBR_02; giường không áp dụng cho bán trú. Dữ liệu HTTP ở `20261110-daycare-api.json`. Chưa có bằng chứng đến/về dương tính.

Script tiếp `20261110-daycare-prepare.mjs` được chuẩn bị để cấu hình khu nghỉ và tạo đủ đánh giá/người đại diện/hợp đồng demo trước Hoàn tất tiếp nhận. Tệp minh họa sẽ ghi rõ DEMO ONLY, không là giấy tờ ký thật. Automatic approval review đã từ chối trước khi chạy vì xác nhận trước chỉ nêu hồ sơ/lượt đăng ký, chưa cho phép cụ thể các bản ghi đánh giá, đại diện, hợp đồng ký và hoàn tất tiếp nhận. Đã xin xác nhận riêng cho trọn bộ dữ liệu, không chia nhỏ lệnh để vượt chặn hoặc sửa DB. Không đổi mã sản phẩm/API/test trong lượt tiếp tục này.

### T031 — sau xác nhận trọn bộ dữ liệu demo (10/11/2026)

Người dùng đã trả lời1 lần tiếp cho toàn bộ khu nghỉ, đánh giá xác nhận, người đại diện/liên hệ chính, dịch vụ/giá, hợp đồng/tệp DEMO ONLY/ghi nhận ký, hoàn tất tiếp nhận, điểm danh và đính chính. Hai lần chặn chuẩn bị dữ liệu phía trên là lịch sử, đã được giải quyết bằng xác nhận cụ thể; **không còn là phụ thuộc hiện tại**. Không sửa DB trực tiếp, migration, API hoặc nới kiểm tra.

Chuẩn bị API thật thành công: khu nghỉ gắn tầngUI005, CFG-M03-01=10 trên demo qua change-value/If-Match/lý do, đánh giá đầu vào BASIC/DAY_CARE đã xác nhận bằng tài khoản demo, người thân hư cấu/liên hệ chính/người đại diện, dịch vụ CARE/BASIC và phiên bản giá demo, hợp đồng id1 không yêu cầu cọc. Tệp minh họa ghi rõ “DEMO ONLY - NOT A REAL SIGNED DOCUMENT”, tải qua POST/files và gắn đúng lệnh. Gửi ký/ghi nhận ký hợp đồng, đọc điều kiện, Hoàn tất tiếp nhận đều qua API và giữ ràng buộc của nguồn; các cảnh báo không chặn (đồng ý chia sẻ dữ liệu, hồ sơ sức khỏe ban đầu, nguyện vọng, ý kiến vào ở) được giữ nguyên.

| Kiểm T031 | Kết quả HTTP thật |
| --- | --- |
| Điểm danh đến | 200, attendance1/resident1 PRESENT; lịch004 07:00–18:00, adHocSession=false |
| Nguồn đón012 | Tạo ngoại lệ hư cấu, quản lý demo duyệt; POST/pickups loại DAY_CARE_DEPARTURE trả201 |
| Điểm danh về005 | 200, LEFT, checkoutType=PICKUP; GET danh sách xác nhận Đã về |
| Dùng lại bản ghi đón | 409, không tạo lần về thứhai |
| Đính chính giờ đến | Lấy arrivalRecordId69 từ nhật ký công khai/API, POST/day-care-attendance-records/69/correct trả201; GET xác nhận giờ đến07:47:35.858 thay07:48:35.858, vẫn LEFT |
| Quyền | Kế toán demo GET trạng thái và POST/check-in đều403 |

Chuỗi API và phản hồi giữ trong `backend/logs/005/20261110-daycare-api.json`, các script `20261110-daycare-{prepare,finish}.mjs`; backend `20261110-t031-demo-approved.log`. Dữ liệu hư cấu được giữ trên schema riêng; không gọi đây là nghiệm thu thao tác UI.

Finding từ dữ liệu thật: Profile001 trả tên ở personalInfo.fullName nhưng webT026/T031 và app gatewayT026/T031/phát sinhT040 đọc trường fullName ngoài cùng. Chỉnh fixture của chính005 thành đúng Profile, bổ sung assertion tên vào test FR017/FR012 có sẵn và chạy RED2/12web; thêm test gateway một ca cho hai consumer và cập nhật hai fixture phát sinh, RED3/5mobile. Sau sửa chỉ phần005: webcare59/59 và mobilecare40/40xanh. Không giữ fallback sang trường ngoài hợp đồng để che fixture sai. Màn CarePlanPage(T017) có cùng cách đọc tên nhưng ngoài bốn task được giao, không sửa trong lượt này.

**Kiểm cuối sau sửa Profile:** `npm test -- --maxWorkers=2`952/952test,162/162filexanh (283,50giây); `npm run lint`, `npm run build` qua, build cảnh báo chunk lớn có sẵn. `flutter analyze` sạch; `flutter test --concurrency=2`507/507xanh (175giây). Log `20261110-t031-web-final.log`, `20261110-t031-web-lint.log`, `20261110-t031-web-build.log`, `20261110-t031-flutter-analyze.log`, `20261110-t031-flutter-final.log`. Giữ mọi timeout/assertion; không sửa test A/B/D/E. `git diff --check` qua. Không đổi mã backend nên không nhận compile/start demo thành bằng chứng IT/Spotless mới.

## Trực quan và thiết bị

- IAB mở `http://127.0.0.1:5173/`: `net::ERR_CONNECTION_TIMED_OUT`.
- Chrome cục bộ: kiểm lại sau khi có dữ liệu thật; đọc được cửa sổ GitHub, nhưng lệnh mở thẻ kiểm thử bị Computer Use dừng vì không xác định đủ chắc URL để áp chính sách. Đã dừng và gửi completion_summary; không dùng công cụ khác để né chặn. Chưa mở/quan sát được web005 trên Chrome trong lượt này.
- `flutter devices`: chỉ Windows, Chrome, Edge. `flutter emulators`: không có nguồn máy ảo. Không có Android/iOS được kết nối.
- Vì vậy chưa kiểm trực tiếp ba bề rộng theo DESIGN, dark có dữ liệu, điều hướng app và mất mạng trên thiết bị. Widget test 200%/theme tối không thay thế các bước đó.

Cả bốn checkbox giữ mở. Không ghi màn đã nghiệm thu hoặc phase hoàn thành.
