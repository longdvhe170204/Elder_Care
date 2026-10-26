# Kiểm gỡ conflict develop sau Phase 9

Ngày 2026-10-26. Nhánh `impl/008-staff-shift-handover`, HEAD `4fc4bdf`, commit đang được merge từ develop `4e1b7dd`. Người dùng đã tự commit/push Phase 9 và khởi động merge; AI chỉ xử lý conflict theo yêu cầu, không commit/push/đổi nhánh hoặc chạy merge lại.

## Phần đã ghép

- 28 file conflict ban đầu: 21 add/add và 7 nội dung. Đã sao lưu bản đang conflict trước khi sửa vào `TEMP/ElderCare-008-merge-20261026-025605`.
- Giữ API/DTO/NamedInterface mới của C/D trên develop. Consumer M09 dùng ResidentLocationApi, LocationHistoryApi.byLocation/byLocationOn, floorId, dayCareRestAreaLocation; LocationReferences.currentReferences dùng view của SPI. Tra cứu toàn viện qua BedAllocationApi.residentsIn(FACILITY), không truyền LocationRef null cho nguồn C.
- Nguồn giường dùng BedStatusApi.overdueAwaitingReady và BedStatisticsApi.awaitingReady/stateAt. Vẫn đọc trạng thái thật khi danh sách quá hạn thay đổi do CFG; không suy rằng nguồn biến mất là đã xử lý.
- Nguồn liều dùng DosePendingHandoverView/currentStatuses của D. NursingShiftLookup cài ShiftNursePort mới bằng cùng dữ liệu ca/phụ trách/phân công, giữ SPI cũ cho consumer hiện có. Không sửa thuật toán thuốc hoặc API chính thức của D.
- Nối BedAllocationStarted/BedAllocationClosed/BedTransferred của C vào logic tỷ lệ hiện có. Hai test đổi sang kiểu sự kiện thật đã đỏ do không có consumer, sau đó mới bổ sung listener; giữ listener cũ cho sự kiện đã lưu. Giữ nguyên assertion nghiệp vụ.
- Giữ cấu hình fake của C/D; cấu hình vị trí giả cho test consumer đặt tại FakeLocations.Config trong cùng bản giả hiện có để không che bản cài thật ở test của C. Không tạo bản giả thứ hai hoặc bean fake production.
- Gộp đăng ký ApiContractIT cho cả 003/008 và các job M03/M07/M09; giữ toàn bộ assertion. Web giữ màn/menu Phòng giường/Báo cáo/Thuốc và Nhân sự/Lịch ca; generator đăng ký cả 003/008, giữ FormData/Blob. Đã npm ci và npm run gen:api, không sửa tay client sinh.
- Team-plan giữ tiến độ hai bên, bỏ hai dòng E "Chờ" đã trùng với dòng hoàn thành, cập nhật tên SPI tham chiếu chính thức. Không sửa migration, pom.xml, manifest hoặc mã nghiệp vụ M07 để làm test xanh.

## Kết quả kiểm

| Phần | Kết quả |
| --- | --- |
| Biên dịch main/test | Xanh sau khi chuyển consumer 008 sang API mới |
| Spotless phần ghép | Xanh; chỉ định dạng M09 và các file hỗ trợ/đăng ký vừa ghép, không định dạng lại mã D |
| Hồi quy 008 | **87 test đơn vị + 246 IT = 333**, không lỗi/thất bại/bỏ qua, **03:18** |
| Frontend | **320 test/52 file**, lint và build xanh; Vite có cảnh báo kích thước chunk |
| ArchTest toàn kho | Có sáu test đỏ trong ba lớp dưới đây |
| Đối chiếu develop gốc | Xuất nguyên backend của MERGE_HEAD bằng git archive vào thư mục tạm, chạy ba lớp kiểm nguyên gốc: **11 test, cùng 6 thất bại**, 46 giây; không checkout/đổi nhánh hoặc sửa nguồn |
| clean verify cuối | **Đỏ: 617 test đơn vị/kiến trúc, 6 thất bại, 0 lỗi, 0 bỏ qua; 01:55**. Dừng ở Surefire nên chưa chạy IT toàn kho. Cùng sáu lỗi đã tái hiện trên develop gốc; không dùng kết quả xanh trước merge thay kết quả của bản ghép |
| Kiểm riêng ApiContractIT | **Đỏ**, 1 test: 89 đường dẫn thuốc có trong mã nhưng ngoài tập đường dẫn hợp đồng đã đăng ký. Chạy riêng trên develop gốc cũng đỏ với **đúng cùng 89 đường dẫn**, 26 giây. Assertion đầu chặn trước phần so operation/schema của từng hợp đồng; chưa thể kết luận bộ so toàn kho đã xanh |
| Git | Đã git add các file giải quyết và phần tương thích. **0 unmerged entry**, không còn dấu conflict, không còn thay đổi tracked chưa stage; HEAD vẫn 4fc4bdf, MERGE_HEAD vẫn 4e1b7dd. Git báo `All conflicts fixed but you are still merging`; người dùng tự commit để kết thúc merge |
| Kiểm khoảng trắng | `git diff --check` sạch; `git diff --cached --check` chỉ báo một dòng trống cuối `specs/006-medication-management/plan.md:259`. File này giữ nguyên nội dung develop; không sửa định dạng tài liệu D ngoài phạm vi ghép |

## Kiểm trực tiếp demo sau merge: trang trắng sau đăng nhập

Ngày 2026-10-26, người dùng chạy backend demo cổng 8080 và Vite cổng 5173, báo trang trắng sau đăng nhập. Nguyên nhân: menu Danh mục thuốc dùng BookOpen nhưng AppShell chưa đăng ký biểu tượng. StaffNavigation.test.jsx tái hiện lỗi React component undefined trước khi sửa. Chủ dự án đã duyệt thêm đúng BookOpen vào import và ICONS của khung chung, ghi A review trong team-plan mục 8.

- Test mới và App.test.jsx: 4 test xanh sau sửa; toàn frontend **321 test/53 file**, lint/build xanh, còn cảnh báo kích thước chunk của Vite.
- Edge headless profile riêng, không mock HTTP: đăng nhập demo.quanly, mở Nhân sự, thấy tiêu đề và nút Thêm nhân viên; **0 lỗi JavaScript, 0 phản hồi API lỗi** trong lượt kiểm. Không tạo/sửa hồ sơ hoặc chụp màn hình; đóng trình duyệt kiểm riêng sau khi xong.
- Log web ở TEMP/ElderCare-008-navigation-20261026-081803; kết quả trình duyệt ở frontend/node_modules/.cache/008-navigation-browser-result.json (Git bỏ qua). Không chạy lại IT hoặc đổi database; kết quả backend đỏ của lượt merge phía trên vẫn còn, T044b chưa nghiệm thu toàn bộ.
- Bản sửa này chưa commit/push và chưa stage cùng bản merge đang chờ người dùng chốt.

## Các lỗi đã tái hiện trên develop gốc

| Chủ phần | Lớp / test | Lỗi được ghi nhận |
| --- | --- | --- |
| D (006/M07) | LayerArchTest.apiDoesNotDependOnImplementation | 12 vi phạm API phụ thuộc internal/web |
| D (006/M07) | LayerArchTest.domainDependsOnNoOtherLayer | 10 vi phạm domain gọi service, gồm MedicationDose gọi DoseTransitionTable |
| D (006/M07) | LayerArchTest.serviceDoesNotDependOnWeb | 395 vi phạm service phụ thuộc kiểu web |
| D (006/M07) | TimeArchTest.DBR_25_noNowOutsideClockConfig | 95 lời gọi now trực tiếp thay Clock/BusinessTime |
| D (006/M07) | DataGroupArchTest.FR_001_everyEntity_declaresDataGroup | 15 entity chưa khai DataGroup |
| D (006/M07) | DataGroupArchTest.R_05_businessAndRecordEntities_haveNoPublicSetters | 12 setter công khai trên entity nhóm 2/3 |

Các con số vi phạm không phải số test. Đây là sáu test thất bại được tái hiện cả khi chưa ghép 008; không nới luật hoặc xóa/đổi assertion để bỏ qua. D cần xử lý phần này, rồi chạy lại clean verify trước khi coi bản đồng bộ đã qua kiểm toàn kho.

Ngoài sáu test trên, ApiContractIT chạy riêng dừng ở tập đường dẫn thuốc chưa được đăng ký (ví dụ `/api/v1/don-thuoc/{id}` và `/api/v1/prescriptions/{id}`). Danh sách hợp đồng đang có 000/001/002/003/008/009/016; cần D/A đối chiếu 006 với endpoint thực tế và đăng ký theo quy trình hợp đồng, không tắt kiểm hoặc tự đổi tên endpoint trong lượt gỡ conflict. Số 89 là số đường dẫn, không phải số test; danh sách lỗi của hai bản đã được so và bằng nhau hoàn toàn.

Log và bản develop đối chiếu nằm trong thư mục TEMP nêu trên: `full-verify-red.log`, `architecture-red.log`, `incoming-architecture.log`, `api-contract-red.log`, `incoming-api-contract.log`, `m09-regression-green.log`, `real-allocation-events-red.log`, `web-test.log`, `web-lint.log`, `web-build.log`. Không ghi mật khẩu hoặc sao chép `.env` vào Git.
