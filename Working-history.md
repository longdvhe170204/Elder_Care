# Lịch sử thực hiện công việc

## 2026-10-22 — Feature 003, Phase 1 (HungNX-nguyen / C)

> Phần Phase 1 ban đầu được viết bằng tiếng Anh theo yêu cầu ngày 22/10. Ngày 23/10, nội dung được chuyển sang tiếng Việt theo yêu cầu mới; các sự kiện, trình tự, đường dẫn, quyết định và kết quả cũ được giữ nguyên.

### Yêu cầu và trạng thái ban đầu

- Chỉ triển khai Phase 1 của `specs/003-room-bed-allocation/tasks.md`: T001 (khung module) và T002 (năm bản giả dùng cho test).
- Tuân thủ AGENTS.md, phân công sở hữu, cấu trúc package backend hiện hành, viết test trước, kiểm riêng task cùng ArchTest và chạy nền `mvnw.cmd clean verify` cuối phase. Chỉ đánh hoàn thành task sau khi kiểm cuối phase thành công.
- File lịch sử được tạo từ thay đổi và kết quả lệnh thực tế. File chưa tồn tại ở đầu lượt Phase 1; không có lịch sử trước đó bị xóa.
- Checkout ban đầu sạch, ở `develop`, HEAD `d806d93` (`Merge pull request #19 from longdvhe170204/impl/002-event-notifications`). Không đổi nhánh, commit, push, merge, rebase hoặc tạo PR trong lượt này.
- Đã đọc AGENTS.md, team-plan.md, backend/README.md, constitution, tasks.md, plan.md, hợp đồng module liên quan, research G-01/G-02/G-16 và R-02, ArchTest, bản giả hiện có và định nghĩa dữ liệu/chữ ký cần cho phase. Không tự đặt hoặc thay đổi quy tắc nghiệp vụ.

### Các việc đang chờ đã báo đầu phiên

1. C / 003: A cần bản thật `BedAllocationApi.openAt/residentsIn`, gồm lỗi dữ liệu nguồn mâu thuẫn. Phase 1 chưa cài bản thật này.
2. C và D / các feature ghi nhận: cài `OfflineHandler` và lưu dấu đồng bộ sau khi mất quyền khi làm các bản ghi đó. Lệnh của 003 vẫn chỉ trực tuyến.
3. C / 014: A cần khai báo SPI người tham gia chuyến/chuyến của nhân viên để tính phạm vi theo chuyến. Việc này ngoài Phase 1 của 003.
4. C / demo 003: A cần hai tầng, mỗi tầng hai phòng, hai người minh họa có phân bổ mở. Phase này chưa thêm dữ liệu demo.

### Trình tự triển khai và bằng chứng

1. Viết `FR_001_ModuleSetupTest` trước khi tạo khung. Lần gọi Maven wrapper đầu bị chặn quyền truy cập cache. Sau khi được phép dùng cache, Maven chạy bản 3.10.0 đã cấu hình với JDK 25 có sẵn trên máy.
2. Điều chỉnh test khung để kiểm file tài liệu package đối với package chưa có lớp runtime, và kiểm khai báo `@NamedInterface` đã biên dịch đối với API công khai. Lần đỏ tương ứng chạy 2 test, 2 assertion thất bại, 0 lỗi thực thi, 0 bỏ qua; nguyên nhân là thiếu khung/API.
3. Thêm tài liệu package và khai báo API của T001. Chạy định dạng cùng test T001 và ArchTest/ModuleBoundaryTest: 23 test, 0 thất bại/lỗi/bỏ qua, exit 0.
4. Viết `FR_018_FakeM03Test` trước phần hành vi bản giả. Lần đầu chưa biên dịch test vì chưa có kiểu chữ ký/bản giả. Sau khi bổ sung các khai báo tối thiểu và khung bản giả biên dịch được, lần đỏ hành vi có 7 test, 5 assertion thất bại, 0 lỗi/bỏ qua.
5. Cài năm hàm đặt dữ liệu và hành vi trong bộ nhớ. Chạy định dạng cùng test T002 và ArchTest/ModuleBoundaryTest: 28 test, 0 thất bại/lỗi/bỏ qua, exit 0.
6. Bỏ wildcard import trong các bản giả mới và chạy `spotless:apply`: exit 0. Lệnh định dạng không thay đổi file Java vốn đã được Git theo dõi.
7. Chạy nền `clean verify` toàn bộ theo yêu cầu. Lần đầu chạy 154 test: 153 qua, 0 assertion thất bại, 1 lỗi thực thi, 0 bỏ qua. `BackendApplicationTests.contextLoads` lỗi vì `MySqlIntegrationTestBase.TestDatabase.requireEnv` không đọc được `TEST_DB_URL`. Build dừng ở Surefire, chưa chạy Failsafe và các bước verify sau đó. T001/T002 chưa được đánh hoàn thành sau lần này.
8. Review độc lập chỉ đọc phát hiện hai hàm đọc của `BedAssetPort` cũng phải có `MANDATORY` theo contracts mục 3. Thêm `FR_013_BedAssetPortTest`, thấy đỏ: 1 test, 1 assertion thất bại, 0 lỗi/bỏ qua. Bổ sung hai khai báo transaction còn thiếu, không đổi hợp đồng. Kết quả kiểm tiếp được ghi ở phần cập nhật cuối bên dưới.
9. Người dùng cung cấp đường dẫn JDK và cấu hình database test sau lần kiểm toàn bộ đầu tiên. Thông tin đăng nhập chỉ dùng trong tiến trình test, không ghi giá trị vào kho mã hoặc file lịch sử.

### Các file mã nguồn được tạo và mục đích

Đường dẫn trong phần này tính từ thư mục gốc dự án. File `m03/package-info.java` có sẵn được giữ nguyên.

Trong `backend/src/main/java/com/cambae/backend/m03/`:

| File | Mục đích |
| --- | --- |
| `api/package-info.java` | Công khai API M03 dưới dạng named interface của Modulith. |
| `api/dto/package-info.java` | Công khai DTO chữ ký dùng chung dưới dạng named interface. |
| `api/spi/package-info.java` | Công khai các khai báo SPI dưới dạng named interface. |
| `api/dto/LocationRef.java` | Loại/id vị trí dùng kiểm tham chiếu từ phần khác. |
| `api/dto/SessionDate.java` | Khóa ngày/buổi để đếm đăng ký bán trú. |
| `api/dto/DayCareSession.java` | Id, khung giờ, trạng thái hiệu lực của buổi truyền cho bộ đếm. |
| `api/spi/AllocationResidencySource.java` | Tám hàm do 004 cài: loại lưu trú/mức chăm sóc, kết thúc dự kiến, giữ giường, so giá và hai callback transaction. |
| `api/spi/DayCareArrivalCounter.java` | Đếm lịch đến từ ngày cho các buổi được yêu cầu; kiểm lịch tương lai. |
| `api/spi/AllocationInfectionStatus.java` | Đọc vùng tầng/khu đang khoanh và việc người cao tuổi thuộc danh sách tại thời điểm truyền vào. |
| `api/spi/LocationReferences.java` | Đọc tham chiếu vị trí hiện tại và dấu đã từng được tham chiếu. |
| `api/spi/CareLevelCode.java` | Ba mã mức chăm sóc trao đổi qua SPI M03. |
| `api/spi/ResidencyType.java` | Các mã lưu trú dài hạn, ngắn ngày, bán trú trao đổi qua SPI. |
| `api/spi/HeldBed.java` | Quyết định giữ giường và hạn giữ nếu có. |
| `api/spi/CordonedArea.java` | Tập tầng/khu vực khoanh vùng bất biến. |
| `api/spi/LocationReferenceView.java` | Loại, id, nhãn hiển thị của tham chiếu gây chặn. |
| `internal/structure/package-info.java` | Tài liệu namespace sẽ cài cơ cấu/danh mục. |
| `internal/bed/package-info.java` | Tài liệu namespace cài trạng thái giường. |
| `internal/bed/domain/package-info.java` | Tài liệu ranh giới cổng nội bộ giữa giường và tài sản. |
| `internal/bed/domain/BedAssetPort.java` | Ba hàm do 019 cài; tích hợp tài sản trong M03 và trong transaction của lệnh gọi. |
| `internal/allocation/package-info.java` | Tài liệu namespace phân bổ/chuyển giường/lịch sử. |
| `internal/isolation/package-info.java` | Tài liệu namespace cách ly phòng. |
| `internal/daycare/package-info.java` | Tài liệu namespace khu nghỉ/sức chứa bán trú. |
| `scheduling/package-info.java` | Giữ và mô tả namespace job hiện hành; chưa thêm job. |

Trong `backend/src/test/java/com/cambae/backend/`:

| File | Mục đích |
| --- | --- |
| `m03/FR_001_ModuleSetupTest.java` | Kiểm khung tài liệu và API named interface; 2 test. |
| `m03/FR_013_BedAssetPortTest.java` | Kiểm MANDATORY trên mọi hàm cổng tài sản; 1 test. |
| `support/fake/m03/FR_018_FakeM03Test.java` | Kiểm mặc định, dữ liệu độc lập/reset, callback, lọc ngày/buổi, lịch sử lây nhiễm, tham chiếu, dấu tài sản và đăng ký Spring; 7 test. |
| `support/fake/m03/FakeM03Config.java` | Đăng ký đúng năm fake primary bằng `@TestConfiguration`, chỉ nhập tường minh vào test. |
| `support/fake/m03/FakeAllocationResidencySource.java` | Giữ dữ liệu nguồn lưu trú, khác giá và kết quả callback quan sát được trong bộ nhớ. |
| `support/fake/m03/FakeDayCareArrivals.java` | Giữ số đếm ngày/buổi được đặt và lọc khi đọc. |
| `support/fake/m03/FakeAllocationInfectionStatus.java` | Giữ các mốc hiệu lực lây nhiễm/khoanh vùng và trả trạng thái tại thời điểm. |
| `support/fake/m03/FakeLocationReferences.java` | Tách tham chiếu hiện tại với dấu đã từng tham chiếu. |
| `support/fake/m03/FakeBedAssets.java` | Ghi tăng không làm gì; bật/tắt dấu chờ chuyển người, chỉ trả giường được hỏi. |

Các tài liệu được thay đổi:

| File | Thay đổi và mục đích |
| --- | --- |
| `specs/003-room-bed-allocation/tasks.md` | Cập nhật đường dẫn kỹ thuật, tên fake và test T001/T002; giải thích đưa khai báo tối thiểu từ T004 lên trước. Checkbox phụ thuộc kiểm cuối phase. |
| `specs/003-room-bed-allocation/plan.md` | Thêm ghi chú về cấu trúc, thứ tự khai báo, dữ liệu giả, môi trường, bằng chứng kiểm và phần còn lại. |
| `specs/000-shared-foundation/glossary.md` | Chỉ điền mục 9 của 003 với tên tiếng Anh được thêm trong phase. Dịch hợp đồng REST vẫn còn chờ. |
| `docs/team-plan.md` | Thêm ba dòng chờ adapter SPI M03 của B/004, D/007, E/008; ghi rõ khai báo/fake chưa merge, giữ nguyên các việc C đang được chờ. |
| `Working-history.md` | Tạo lịch sử thực tế bằng tiếng Anh ở lượt Phase 1; nội dung hiện được chuyển sang tiếng Việt theo yêu cầu Phase 2. |

Không xóa file trong kho mã. Không thay đổi hợp đồng REST, tài liệu nghiệp vụ, spec.md, migration, cấu hình build/thư viện, mã production dùng chung, frontend hoặc mobile trong Phase 1.

### Quan hệ giữa file và luồng dữ liệu

- Interface SPI production chỉ phụ thuộc record/enum chữ ký công khai và khai báo transaction JDK/Spring; không gọi nội bộ hay đọc bảng module khác.
- Adapter 004 sau này cài `AllocationResidencySource` và `DayCareArrivalCounter`; 007 cài `AllocationInfectionStatus`; 008 cài `LocationReferences`. Phase 1 không cài các adapter thật này.
- `BedAssetPort` nằm trong M03; 003 gọi, 019 cài. Cổng lệnh chiều ngược lại còn thuộc khai báo Phase 2.
- Test nhập `FakeM03Config` để có một fake primary cho mỗi port; đặt dữ liệu, gọi interface, kiểm kết quả/callback và xóa dữ liệu bằng `reset()`.
- Nguồn lưu trú giữ riêng mức theo hợp đồng và đăng ký. Nguồn lây nhiễm dùng mốc truyền vào thay vì giờ hệ thống. Tham chiếu vị trí giữ dấu lịch sử sau khi gỡ tham chiếu hiện tại. Dấu tài sản là công tắc trong bộ nhớ, ghi tăng không tạo bản ghi nghiệp vụ.
- Phase 1 không tuyên bố đã có luồng nghiệp vụ M03 chạy thật, HTTP endpoint, ghi database, scheduler hoặc adapter thật.

### Các quyết định kỹ thuật

- Dùng `api/{dto,spi}`, `internal/<function>/<layer>`, `scheduling` với tên tiếng Anh. Yêu cầu dựng khung T001 được thực hiện bằng tài liệu package, không tạo entity/service rỗng hoặc tầng chưa cần.
- Chỉ đưa bốn SPI, cổng tài sản và kiểu chữ ký cần thiết từ T004 lên Phase 1 để T002 có thể biên dịch/implements. T003/T004 và phần còn lại của Phase 2 tại thời điểm đó chưa xong.
- SPI M03 sở hữu mã trao đổi; adapter nguồn sau này ánh xạ dữ liệu module của mình sang các kiểu này. Không khai báo hộ M01 hay tạo thêm bản giả của chủ interface khác.
- Dùng cấu hình Spring chỉ cho test và hàm đặt dữ liệu theo mẫu fake hiện có. Không thêm HTTP endpoint test/demo trong phase.
- Giữ yêu cầu transaction đã khai; sửa theo review chỉ bổ sung annotation thiếu, không đổi hành vi nghiệp vụ hoặc thao tác API.
- Dùng JDK sẵn có tại `E:\JAVA\JDK_GE\INS_25` qua `JAVA_HOME` của tiến trình. Không đổi thiết lập máy hay thêm dependency vào cấu hình dự án.

### Lệnh đã chạy và kết quả quan sát

Các lệnh Maven chạy trong `backend/`, với JDK 25 ở trên.

| Lệnh | Kết quả quan sát |
| --- | --- |
| `git status --short`, `git branch --show-current`, `git log -1` | Ban đầu sạch, nhánh `develop`, HEAD `d806d93`. |
| `mvnw.cmd test -Dtest=FR_001_ModuleSetupTest` | Đỏ: 2 assertion thất bại, không lỗi/bỏ qua. Bản test trước đó dùng class loading cho package chỉ có tài liệu đã được sửa trước phần cài. |
| `mvnw.cmd spotless:apply test -Dtest=FR_001_ModuleSetupTest,*ArchTest,ModuleBoundaryTest` | Xanh: 23 test, exit 0. |
| `mvnw.cmd test -Dtest=FR_018_FakeM03Test,*ArchTest,ModuleBoundaryTest` | Lần đầu lỗi biên dịch test vì thiếu chữ ký M03; không ghi nhận đã chạy test thành công ở lần này. |
| `mvnw.cmd test -Dtest=FR_018_FakeM03Test` | Đỏ hành vi sau khung biên dịch: 7 test, 5 assertion thất bại, 0 lỗi/bỏ qua. |
| `mvnw.cmd spotless:apply test -Dtest=FR_018_FakeM03Test,*ArchTest,ModuleBoundaryTest` | Xanh: 28 test, exit 0. |
| `mvnw.cmd spotless:apply` | Exit 0 sau sửa import. |
| `mvnw.cmd clean verify` (nền, lần đầu) | Exit 1; 154 test, 0 assertion thất bại, 1 lỗi, 0 bỏ qua. Thiếu `TEST_DB_URL` ở test nạp context có sẵn; chưa tới tích hợp/các bước verify còn lại. |
| `mvnw.cmd test -Dtest=FR_013_BedAssetPortTest` | Test hồi quy theo review đỏ: 1 test, 1 assertion thất bại, 0 lỗi/bỏ qua. |
| `git diff --check` | Qua tại mốc cập nhật tài liệu; kiểm cuối được ghi bên dưới. |

Log lệnh ở ngoài kho mã: `C:\Users\nguye\AppData\Local\Temp\eldercare-003-*.log`. Đây là bằng chứng thực thi cục bộ, không phải artifact được Git theo dõi.

### Việc còn lại và thứ tự tại mốc trước kiểm cuối Phase 1

1. Hoàn tất kiểm sau sửa review, chạy lại `mvnw.cmd clean verify` với môi trường database được cung cấp; ghi kết quả thực trước khi đánh T001/T002.
2. Task tiếp theo lúc đó là **T003**: API M03 công khai; rồi hoàn thiện **T004**, dùng lại khai báo và thêm cổng lệnh tài sản chiều ngược, không nhân đôi SPI.
3. Tiếp tục **T005** sự kiện, **T006** migration cơ cấu, **T007** migration giường/lịch sử, **T008** dịch vụ trạng thái, **T009** kiến trúc, **T010** provider; giữ test trước trong từng task.
4. Dịch hợp đồng REST và luồng review/merge riêng còn chờ trước endpoint/client. Cần xác nhận prerequisite M01 trước phần hồ sơ/trạng thái. Test fake của task chưa chứng minh triển khai vận hành hoặc tích hợp adapter thật.

### Cập nhật kết quả cuối Phase 1

- Sửa theo review đã xanh: `mvnw.cmd spotless:apply test -Dtest=FR_013_BedAssetPortTest,FR_018_FakeM03Test,*ArchTest,ModuleBoundaryTest` chạy 29 test, 0 thất bại/lỗi/bỏ qua, exit 0. Phát hiện duy nhất đã được sửa; không có phát hiện cần xử lý khác.
- Lần nền `mvnw.cmd clean verify` thứ hai được mở với biến database do người dùng cung cấp trong tiến trình. Cấu hình MySQL và test nạp context lỗi trước đó đã chạy qua; tại mốc ghi chú này các test tích hợp đang chạy, chờ ghi tổng cuối khi tiến trình kết thúc.
- Lần nền thứ hai **`mvnw.cmd clean verify` thành công**, exit 0, **01:52**: **155 test đơn vị/kiến trúc và 411 test tích hợp**, cả hai nhóm **0 thất bại, 0 lỗi, 0 bỏ qua**. Failsafe ghi 0 flaky test. Spotless kiểm 626 file Java, không cần sửa; gồm 10 test mới của Phase 1/hồi quy review.
- Chỉ sau kết quả trên mới đánh **T001/T002 `[x]`**. Plan được cập nhật vấn đề môi trường đã giải quyết, sửa review, bằng chứng và task kế tiếp. Không tắt hoặc bỏ qua test.
- Thiếu cấu hình MySQL đã được giải quyết cho lần chạy đó; biến chỉ nằm trong tiến trình Maven, shell sau vẫn phải nạp lại hoặc dùng cấu hình môi trường do người dùng đặt. Không lưu giá trị đăng nhập vào kho mã hoặc thiết lập user/machine.
- Tại cuối lượt Phase 1, task kế tiếp vẫn là **T003**, rồi phần còn lại của **T004**, T005–T010. Phase 2 chưa triển khai lúc đó. Dịch/review REST, adapter thật, prerequisite M01 cho nghiệp vụ sau và các việc C đang được chờ vẫn còn; chúng không chặn phạm vi Phase 1 đã kiểm.
- Kiểm kê cuối Phase 1: 32 file Java mới, file lịch sử mới và bốn tài liệu được sửa (tasks.md, plan.md, glossary mục 9, team-plan.md). Không xóa file; kiểm Git/khoảng trắng và đối chiếu danh sách được ghi ở cập nhật cuối dưới đây.
- Kiểm cuối Phase 1: `git diff --check` exit 0; danh sách lịch sử đủ 32 file Java mới, thiếu 0 mục, có 0 file Java ngoài M03/test/fake của nó. Chỉ T001/T002 được đánh, T003 là task chưa làm đầu tiên. Nhánh vẫn `develop`, thay đổi chưa commit trong lượt AI này. Không chạy kiểm frontend/mobile hay chụp ảnh do không thay đổi giao diện.

## 2026-10-23 — Feature 003, Phase 2 (HungNX-nguyen / C)

### Yêu cầu, phạm vi và trạng thái đầu phiên

- Triển khai T003–T010 của `specs/003-room-bed-allocation/tasks.md`: API nội bộ, SPI/port, sự kiện, schema cơ cấu/giường, điểm ghi trạng thái, ArchTest và provider nền tảng. Không triển khai user story Phase 3.
- Đã đọc quy định dự án, team-plan mục 8, backend/README.md, tasks.md và phần Ghi chú triển khai trong plan.md trước phần cài. Đối chiếu data-model, hợp đồng module, research/glossary theo yêu cầu từng task; không tự thêm nghiệp vụ.
- Nhánh đầu phiên là `feature/003-manage-bedroom`, HEAD `b61fc6b` (Phase 1 đã được người dùng đưa vào Git). Chỉ `Working-history.md` chưa được theo dõi trước lượt Phase 2. Không đổi nhánh, commit, push, merge, rebase hoặc tạo PR trong lượt này.
- Đã báo các dòng của C ở team-plan mục 8: bản thật `BedAllocationApi.openAt/residentsIn` cho A; `OfflineHandler` khi làm 005/014 và bản ghi tương ứng; SPI phạm vi chuyến của 014; dữ liệu demo 003. Phase 2 chưa hoàn tất dòng nào trong số đó nên không đánh “Xong” sai tình trạng.
- Chuyển toàn bộ ghi chú tiếng Anh cũ của lịch sử Phase 1 sang tiếng Việt theo yêu cầu mới, giữ các sự kiện, trình tự, tên kỹ thuật, đường dẫn và kết quả cũ. Mật khẩu/khóa không được ghi vào file này hay cấu hình trong kho mã.

### Trình tự triển khai thực tế

1. **T003**: viết test phản chiếu kiểm 9 API, tổng 43 thao tác và MANDATORY của lệnh; chạy thấy 2 assertion thất bại trước khai báo. Cài interface/DTO tiếng Anh theo glossary. Một lần biên dịch phần cài thiếu import được sửa, sau đó test task cùng ArchTest qua 23/23.
2. **T004**: viết test 5 thao tác cổng chiều tài sản gọi giường và fake dùng chung; 2 assertion đỏ rồi cài `BedAssetCommandPort`, `BedAssetCommand`, `FakeM03ApiConfig`; kiểm task cùng ArchTest qua 23/23. Giữ nguyên bốn SPI và `BedAssetPort` đã kéo lên Phase 1.
3. **T005**: viết test 20 record sự kiện, trong đó 12 loại mang yêu cầu thông báo; 2 assertion đỏ rồi cài records, `BedNotificationContext`, khuôn yêu cầu gửi 009 và NamedInterface; kiểm qua 23/23.
4. **T006**: viết test schema MySQL trước migration; 21 test kiến trúc qua, 1 IT đỏ. Tạo `V003_01__room_structure.sql` rồi kiểm 21 test kiến trúc + 1 IT xanh.
5. **T007**: viết test giữ chỗ, xác nhận khử khuẩn và lịch sử chỉ thêm; 21 test kiến trúc qua, 3 IT đỏ. Tạo `V003_02__bed_state.sql`, sau đó 21 test kiến trúc + 3 IT xanh.
6. **T009 trước phần cài T008**: viết `SingleBedStateWriterArchTest`; 2 assertion đỏ do gateway chưa tồn tại/chưa có phương thức bị ràng buộc.
7. **T008**: viết IT trạng thái trước cài. Lần đầu test chưa biên dịch vì thiếu kiểu; sau khung biên dịch được có 5/5 IT thất bại. Cài bảng chuyển trạng thái, khóa giường, lưu lịch sử, audit cùng transaction, xử lý trễ và khôi phục tạm ngừng sau bảo trì. Kiểm task: 23 test kiến trúc (gồm T009) + 5 IT đều qua.
8. **T010**: viết IT phân loại, phạm vi và retention; 23 test kiến trúc qua, 3 IT đỏ. Cài các provider và kho truy vấn M03; chạy lại 23 test kiến trúc + 3 IT đều xanh.
9. Rà chữ ký phát hiện giới tính người cao tuổi cần khác chính sách phòng: bổ sung test T003 (3 test, 1 failure), thêm `ResidentGender` và dùng cho `BedStatusApi.countAvailable`; chạy lại 26 test gồm task và ArchTest đều qua. Không đổi trường/endpoint REST.
10. Chạy nền `clean verify` lần đầu: **BUILD SUCCESS**, 164 test đơn vị/kiến trúc + 423 IT, 0 failure/error/skip, tổng 01:50. Chưa đánh [x] vì còn lượt review độc lập.
11. Review phát hiện hai lỗi: khóa thông báo bán trú có thể vượt `notification_request.event_key VARCHAR(191)`; retention có thể xóa lịch sử đang dùng để khôi phục giường sau bảo trì.
12. Viết hai test hồi quy trước sửa: test sự kiện có 3 test/1 failure, đo khóa dài 917 > 191; IT trạng thái có 6 test/1 failure vì retention chọn hai dòng SUSPEND/ASSET_MAINTENANCE đang cần. Sau đó băm chu kỳ bằng SHA-256, chuẩn hóa tập buổi theo ngày/id; loại lịch sử khôi phục còn sống khỏi ứng viên retention.
13. Chạy `spotless:apply` và kiểm lại phần ảnh hưởng: 26 test thông báo/kiến trúc, 9 IT trạng thái/provider đều xanh, 0 failure/error/skip. Review lại hai sửa bằng đọc mã không còn phát hiện cụ thể; reviewer không chạy Maven và không sửa file.
14. Khởi chạy nền `clean verify` lần cuối sau hai sửa review. Kết quả chốt và thời điểm đánh [x] được bổ sung bên dưới sau khi tiến trình hoàn tất; không suy từ lần build trước.

### Quan hệ giữa các file và luồng dữ liệu

- Chín interface trong `m03/api` dùng DTO ở `api/dto`, kiểu SPI M03 đã có, cùng `ScopeFilter`, `BedAllocationView`, `Basis`, `Reason`, `UnmetCondition` của API nền tảng. Đây là chữ ký cho các user story sau cài; chưa có implementation thật của chín API này trong runtime.
- Test của module sử dụng `FakeM03ApiConfig` bằng import tường minh. Các mock chỉ có hành vi sau khi test đặt `doReturn/doAnswer`; lời gọi chưa cấu hình báo lỗi để không che phụ thuộc còn thiếu. Năm fake SPI/port Phase 1 vẫn ở `FakeM03Config`, không tạo lại fake của chủ khác.
- Lệnh nguồn đã kiểm quyền/điều kiện nghiệp vụ mở transaction → `BedStateService.transition` (MANDATORY) → `BedStateStore.lock` với FOR UPDATE → `BedTransitions.next` → `writeState` + `appendHistory` → `AuditLogApi.write` trong cùng transaction. Lỗi audit làm rollback trạng thái và lịch sử, được IT kiểm.
- Dịch vụ đọc `BusinessTime` và `ParameterApi` (CFG-M03-04/08) để tính thời điểm thực, hạn sẵn sàng, độ trễ; không gọi đồng hồ hệ thống trực tiếp. Tác vụ trễ phát `DelayedScheduledWork`; các record thông báo khác được các story nguồn phát sau này.
- Record cài `HasNotificationRequests` → `BedEventNotifications` dựng `NotificationRequest` → listener 009 hiện có tiếp nhận sau commit. Context do lệnh nguồn cung cấp nhóm người nhận/tầng/mốc theo G-12; không gọi FCM/SMS trực tiếp. Kiểm ở phase này xác minh hợp đồng sự kiện/yêu cầu; không tuyên bố đã chạy đầu cuối mọi tình huống gửi thông báo.
- Flyway tạo schema cơ cấu trước bảng giường/lịch sử. Store chỉ truy cập bảng M03. Provider scope đọc phòng/tầng sở hữu; thiếu bản ghi ném lỗi dữ liệu nguồn. Provider phân loại gắn marker HEALTH cho lý do cách ly/lây nhiễm để cơ chế audit hiện có che dữ liệu đúng nhóm.
- `M03Retention` dùng `M03GovernanceStore` lấy ứng viên; dữ liệu gắn hồ sơ phải có mốc cuối từ `ResidencyEndApi` và hết hạn hồ sơ. Thiếu adapter/mốc thì giữ dữ liệu. Xóa chỉ qua `RetentionDatabase`, không dùng kết nối app; các bảng phân bổ/cách ly chưa tạo được kiểm tồn tại trước truy vấn.
- Lịch sử ASSET_MAINTENANCE mới nhất của giường UNDER_MAINTENANCE, và SUSPEND mới nhất của giường SUSPENDED/UNDER_MAINTENANCE được giữ vì `BedStateStore` còn cần để khôi phục trạng thái/lý do. Các dòng lịch sử không còn làm căn cứ vẫn đi theo chính sách hết hạn và ràng buộc hồ sơ.

### Quyết định kỹ thuật và giới hạn

- Đặt lớp đúng R-02: `api/events`, `api/dto`, `internal/bed/{domain,persistence,service}`, `internal/allocation/{domain,persistence,service,config}`. Đường dẫn tiếng Việt trong tasks là tên logic; không sao chép nguyên đường dẫn.
- T008 điều phối ở service, SQL ở persistence. T009 chặn lớp khác gọi `BedStateStore.writeState`; mọi lệnh trạng thái phải qua gateway. Luật này kiểm lời gọi Java tới phương thức ghi, không phải bộ phân tích mọi chuỗi SQL có thể được thêm trong tương lai.
- Dùng JDBC hiện có thay vì thêm lớp entity/repository trống. Không thêm thư viện, không sửa pom/common/M15 hay migration của chủ khác.
- `last_closed_allocation_id` được tạo trước; FK nội bộ tới `bed_allocation` phải thêm ở T023/V003_03 sau khi bảng này tồn tại. Đã ghi đồng thời trong tasks/plan.
- DATETIME(6) lưu UTC, thời gian dịch vụ được chuẩn hóa microsecond. Bảo trì giữ căn cứ trong lịch sử thay vì ghi suspension_reason khi state không phải SUSPENDED, tránh vi phạm CHECK.
- Chu kỳ khóa thông báo được băm SHA-256 bằng JDK; tập buổi bán trú được loại trùng/sắp xếp trước băm, nên phát lại cùng tập buổi cho khóa ổn định bất kể thứ tự đầu vào. Không cần đổi schema 009.
- Dùng API `ResidencyEndApi` đã được 002 khai báo; không tự viết API của M01/M02. SQL dành cho bảng tương lai đã đặt tên ở glossary và phải đối chiếu khi tạo các bảng ở user story tương ứng.
- Audit phân biệt `infectionControlReason`/`isolationReason` với lý do thường; lệnh chuyển kiểm soát lây nhiễm sau này phải truyền `healthReason=true` và marker tương ứng.
- Không sửa spec/nghiệp vụ, hợp đồng REST, giao diện hay chụp màn hình. Hợp đồng REST tiếng Việt hiện có cần được dịch/duyệt theo quy trình trước task controller tương ứng; Phase 2 không tự thay đổi hợp đồng đó.

### Lệnh và kết quả kiểm chứng

Các lệnh chạy từ `backend/`, bằng JDK 25 do người dùng cung cấp; ba biến `TEST_DB_*` chỉ nạp trong process chạy. Không lưu giá trị bí mật vào kho mã.

| Phần | Lệnh/kiểu chạy | Đỏ trước cài | Xanh sau cài |
| --- | --- | --- | --- |
| T003 | `mvnw.cmd test -Dtest=FR_003_PublicContractsTest`; sau cài thêm `*ArchTest,ModuleBoundaryTest` | 2/2 failure | 23 test qua |
| T004 | `test -Dtest=FR_013_AssetContractsTest`; sau cài thêm ArchTest | 2/2 failure | 23 test qua |
| T005 | `test -Dtest=FR_024_BedEventsTest`; sau cài thêm ArchTest | 2/2 failure | 23 test qua |
| T006 | `verify -Dtest=*ArchTest,ModuleBoundaryTest -Dit.test=FR_001_StructureSchemaIT` | 21 kiến trúc qua, 1 IT failure | 21 kiến trúc + 1 IT qua |
| T007 | `verify -Dtest=*ArchTest,ModuleBoundaryTest -Dit.test=DBR_26_BedSchemaIT` | 21 kiến trúc qua, 3 IT failure | 21 kiến trúc + 3 IT qua |
| T009 | `test -Dtest=SingleBedStateWriterArchTest` | 2/2 failure | xanh cùng T008 |
| T008 | `test-compile failsafe:integration-test failsafe:verify -Dit.test=FR_016_BedStateIT`; sau cài chạy ArchTest + IT | thiếu kiểu, rồi 5/5 IT failure sau khung biên dịch | 23 kiến trúc + 5 IT qua |
| T010 | `verify -Dtest=*ArchTest,ModuleBoundaryTest -Dit.test=FR_049_M03GovernanceIT` | 23 kiến trúc qua, 3 IT failure | 23 kiến trúc + 3 IT qua |
| Rà giới tính | T003, sau sửa thêm ArchTest | 3 test, 1 failure | 26 test qua |
| Review khóa thông báo | `test -Dtest=FR_024_BedEventsTest` | 3 test, 1 failure (917 > 191) | 26 test gồm thông báo/kiến trúc qua |
| Review retention | `test-compile failsafe:integration-test failsafe:verify -Dit.test=FR_016_BedStateIT` | 6 IT, 1 failure (chọn hai history còn cần) | 9 IT gồm trạng thái và governance qua |
| Định dạng | `mvnw.cmd spotless:apply` | — | thành công; không đổi Java được theo dõi của chủ khác |
| Kiểm toàn bộ lần đầu | `mvnw.cmd clean verify`, chạy nền | — | 164 unit/kiến trúc + 423 IT, BUILD SUCCESS, 01:50 |
| Kiểm toàn bộ sau review | `mvnw.cmd clean verify`, chạy nền | — | 165 unit/kiến trúc + 424 IT, 0 failure/error/skip, Spotless qua, BUILD SUCCESS, exit 0, 01:40 |

Các log task ở `C:\Users\nguye\AppData\Local\Temp\003-p2-tNNN-{red,green}.log`; bổ sung `003-p2-t008-red-compile.log`, `003-p2-gender-{red,green}.log`, `003-p2-review-key-red.log`, `003-p2-review-retention-red.log`, `003-p2-review-green.log`, `003-p2-full-verify.log`, `003-p2-final-verify.log`. Log nằm ngoài kho mã; không chép log runtime có thông tin nhạy cảm vào lịch sử.

### Danh sách file tạo/sửa/xóa

85 file mã/test/migration mới so với HEAD đầu Phase 2 được liệt kê từng file dưới đây. Không xóa file trong lượt này. Ba tài liệu được theo dõi được sửa đúng phạm vi; Working-history.md là file đã tồn tại nhưng chưa được Git theo dõi ở đầu phiên.

| Thao tác | File | Mục đích |
| --- | --- | --- |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/BedAllocationCommandApi.java` | T003: 11 lệnh phân bổ tham gia transaction nguồn (MANDATORY). |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/BedHandoverAlertApi.java` | T003: đọc cảnh báo giường phục vụ bàn giao theo tầng. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/BedStatisticsApi.java` | T003: dữ liệu trạng thái, chờ sẵn sàng, cách ly cho thống kê 016. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/BedStatusApi.java` | T003: 5 thao tác đếm trạng thái, giường phù hợp và dấu cảnh báo. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/DayCareCapacityApi.java` | T003: khóa và kiểm sức chứa theo buổi bán trú. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/LocationHistoryApi.java` | T003: 4 truy vấn lịch sử vị trí, người cùng phòng. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/LocationStructureApi.java` | T003: 8 truy vấn cơ cấu vị trí và loại phòng. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/ResidentLocationApi.java` | T003: 6 truy vấn vị trí người cao tuổi theo thời điểm/phân bổ. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/RoomPolicyApi.java` | T003: đọc điều kiện và thuộc tính phân bổ của phòng. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/dto/AbsenceInterval.java` | T003/T005: Khoảng vắng mặt trong dữ liệu vị trí. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/dto/AreaView.java` | T003/T005: Thông tin khu vực. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/dto/AwaitingReadyInterval.java` | T003/T005: Khoảng giường chờ sẵn sàng cho thống kê. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/dto/BedAlertView.java` | T003/T005: Thông tin cảnh báo giường khi bàn giao. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/dto/BedFlagView.java` | T003/T005: Dấu cảnh báo của giường. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/dto/BedLocationRow.java` | T003/T005: Dòng lịch sử vị trí theo giường. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/dto/BedNotificationContext.java` | T003/T005: Mốc phát, tầng và nhóm người nhận do lệnh nguồn cung cấp. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/dto/BedStateOnDate.java` | T003/T005: Trạng thái giường theo ngày. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/dto/BedStatus.java` | T003/T005: 7 trạng thái giường bằng tiếng Anh. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/dto/BedView.java` | T003/T005: Thông tin định danh/trạng thái giường. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/dto/CapacityViolation.java` | T003/T005: Buổi bán trú vượt sức chứa. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/dto/DayCareConfiguration.java` | T003/T005: Cấu hình sức chứa và buổi bán trú. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/dto/FloorView.java` | T003/T005: Thông tin tầng. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/dto/GenderPolicy.java` | T003/T005: Chính sách MALE/FEMALE/UNRESTRICTED của phòng. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/dto/HoldCancellationReason.java` | T003/T005: Mã lý do hủy giữ chỗ. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/dto/LocationView.java` | T003/T005: Thông tin vị trí hiện tại/theo thời điểm. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/dto/RegistrationCount.java` | T003/T005: Số đăng ký theo buổi. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/dto/ResidentGender.java` | T003/T005: Giới tính MALE/FEMALE dùng khi lọc giường; không có UNRESTRICTED. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/dto/ResidentLocationRow.java` | T003/T005: Dòng lịch sử vị trí theo người cao tuổi. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/dto/RoomAllocationProperties.java` | T003/T005: Chính sách và các dấu liên quan điều kiện phân bổ phòng. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/dto/RoomIsolationView.java` | T003/T005: Dữ liệu phòng cách ly cho thống kê. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/dto/RoomTypeRef.java` | T003/T005: Tham chiếu loại phòng. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/dto/RoomView.java` | T003/T005: Thông tin phòng. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/dto/RoommateView.java` | T003/T005: Người cùng phòng và khoảng thời gian. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/dto/ScheduledTransferResult.java` | T003/T005: Kết quả áp dụng chuyển giường theo lịch. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/events/AbsentResidentHoldExpired.java` | Sự kiện hết giữ chỗ của người vắng. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/events/AllocationNoLongerSuitable.java` | Thông báo phân bổ không còn phù hợp. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/events/AllocationReminder.java` | Thông báo nhắc theo loại dấu, đối tượng và lần nhắc. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/events/BedAllocationClosed.java` | Sự kiện đóng phân bổ. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/events/BedAllocationCorrected.java` | Sự kiện đính chính phân bổ, mang dữ liệu trước/sau. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/events/BedAllocationStarted.java` | Sự kiện bắt đầu phân bổ. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/events/BedBecameEmpty.java` | Sự kiện giường trở thành trống. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/events/BedEventNotifications.java` | Dựng NotificationRequest chung; băm chu kỳ SHA-256 để khóa không vượt 191 ký tự. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/events/BedReadinessReminder.java` | Thông báo giường quá hạn sẵn sàng. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/events/BedTransferred.java` | Sự kiện chuyển giường, gồm hai phân bổ/hai tầng. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/events/DayCareOverCapacity.java` | Thông báo bán trú quá sức chứa; chuẩn hóa tập buổi trước khi tạo khóa. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/events/DelayedScheduledWork.java` | Thông báo tác vụ theo lịch thực hiện trễ. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/events/DifferentPriceBedTransfer.java` | Thông báo chuyển giường khác đơn giá. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/events/DoctorAssignedToAllocation.java` | Thông báo bác sĩ được gắn với phân bổ. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/events/FormerOccupantListed.java` | Thông báo giường còn liệt kê người ở cũ. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/events/FutureAllocationCancelledForDamagedBed.java` | Thông báo hủy phân bổ tương lai do giường hỏng. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/events/FutureAllocationPastStart.java` | Thông báo phân bổ tương lai quá mốc bắt đầu. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/events/ReturnedToIsolatedRoom.java` | Thông báo khẩn, nhóm HEALTH khi trở về phòng cách ly. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/events/RoomIsolated.java` | Sự kiện đặt cách ly phòng. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/events/RoomIsolationLifted.java` | Sự kiện gỡ cách ly phòng. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/events/ScheduledTransferWaitingForBed.java` | Thông báo chuyển giường theo lịch còn chờ giường. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/api/events/package-info.java` | T005: công bố api/events bằng @NamedInterface. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/allocation/config/M03GovernanceConfiguration.java` | T010: đăng ký 13 provider phân loại trường, 3 phạm vi, 3 lưu giữ. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/allocation/domain/AllocationResident.java` | T010: cặp phân bổ/người cao tuổi phục vụ kiểm hạn hồ sơ. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/allocation/domain/TimedM03Record.java` | T010: dòng lịch sử có mốc thời gian và căn cứ phân bổ tùy chọn. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/allocation/persistence/M03GovernanceStore.java` | T010: truy vấn/xóa bảng M03 cho provider; giữ lịch sử cần khôi phục trạng thái. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/allocation/service/M03FieldClassification.java` | T010: cung cấp bản đồ nhóm thông tin theo entityType. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/allocation/service/M03Retention.java` | T010: lọc hạn hồ sơ/lịch sử, tham chiếu và xóa bằng kết nối retention. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/allocation/service/M03ScopeAssignment.java` | T010: gán tầng/người cao tuổi qua kho dữ liệu của M03. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/bed/domain/BedAssetCommand.java` | T004: các loại lệnh bảo trì, sẵn sàng, ngừng dùng tài sản giường. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/bed/domain/BedAssetCommandPort.java` | T004: cổng 5 thao tác MANDATORY để phần tài sản gọi nghiệp vụ giường. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/bed/domain/BedEvent.java` | T008: 18 sự kiện của bảng chuyển trạng thái. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/bed/domain/BedHold.java` | T008: thông tin giữ chỗ của người vắng/hồ sơ chờ. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/bed/domain/BedStateChange.java` | T008: kết quả trước/sau, thời điểm và dấu thực hiện trễ. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/bed/domain/BedStateSnapshot.java` | T008: ảnh trạng thái đã khóa, gồm phòng/tầng và trạng thái phòng. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/bed/domain/BedTransition.java` | T008: tham số lệnh, căn cứ, thời gian dự kiến, giữ chỗ và xác nhận khử khuẩn. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/bed/domain/BedTransitions.java` | T008: bảng chuyển trạng thái thuần trong mã. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/bed/domain/HoldReason.java` | T008: hai loại giữ chỗ ABSENT_RESIDENT/WAITING_RECORD. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/bed/persistence/BedStateStore.java` | T008: SELECT FOR UPDATE, đọc căn cứ khôi phục, ghi trạng thái/lịch sử bằng SQL. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/bed/service/BedStateService.java` | T008: điểm điều phối ghi trạng thái duy nhất, audit và sự kiện trong transaction nguồn. |
| Tạo | `backend/src/main/resources/db/migration/V003_01__room_structure.sql` | T006: 7 bảng cơ cấu, CHECK/UNIQUE/FK nội bộ và lý do kiểm soát lây nhiễm cố định. |
| Tạo | `backend/src/main/resources/db/migration/V003_02__bed_state.sql` | T007: bảng giường/lịch sử, CHECK, trigger chỉ thêm và quyền app/retention. |
| Tạo | `backend/src/test/java/com/cambae/backend/m03/DBR_26_BedSchemaIT.java` | T007: kiểm MySQL cho ràng buộc giữ chỗ/khử khuẩn và lịch sử chỉ thêm. |
| Tạo | `backend/src/test/java/com/cambae/backend/m03/FR_001_StructureSchemaIT.java` | T006: kiểm MySQL cho cơ cấu, mã duy nhất và chính sách phòng. |
| Tạo | `backend/src/test/java/com/cambae/backend/m03/FR_003_PublicContractsTest.java` | T003: kiểm 9 API/43 thao tác, MANDATORY và phân biệt hai kiểu giới tính. |
| Tạo | `backend/src/test/java/com/cambae/backend/m03/FR_013_AssetContractsTest.java` | T004: kiểm 5 thao tác cổng tài sản và fake buộc cấu hình kịch bản. |
| Tạo | `backend/src/test/java/com/cambae/backend/m03/FR_016_BedStateIT.java` | T008: kiểm commit/rollback, lịch sử, audit, trễ, chuyển sai, khôi phục sau bảo trì/retention. |
| Tạo | `backend/src/test/java/com/cambae/backend/m03/FR_024_BedEventsTest.java` | T005: kiểm 20 record/12 loại thông báo, mức khẩn/người nhận và khóa bán trú bị chặn độ dài. |
| Tạo | `backend/src/test/java/com/cambae/backend/m03/FR_049_M03GovernanceIT.java` | T010: kiểm phân loại HEALTH, phạm vi thiếu dữ liệu và xóa lịch sử qua retention. |
| Tạo | `backend/src/test/java/com/cambae/backend/m03/SingleBedStateWriterArchTest.java` | T009: bắt buộc gateway tồn tại và chỉ BedStateService được gọi writeState. |
| Tạo | `backend/src/test/java/com/cambae/backend/support/fake/m03/FakeM03ApiConfig.java` | T004/T003: 9 API giả và cổng lệnh dùng chung cho test, chưa cấu hình thì báo lỗi. |
| Sửa | `specs/003-room-bed-allocation/tasks.md` | Ghi mapping đường dẫn kỹ thuật, gateway/ArchTest và FK hoãn tới T023; đánh hoàn thành chỉ sau kiểm cuối phase. |
| Sửa | `specs/003-room-bed-allocation/plan.md` | Ghi phạm vi, quyết định kỹ thuật, điều kiện tích hợp và bằng chứng TDD/kiểm cuối phase. |
| Sửa | `specs/000-shared-foundation/glossary.md` | Chỉ mục 9 của feature 003: tên API, DTO, enum, bảng/cột, event, audit và errorCode mới. |
| Sửa | `Working-history.md` | Dịch Phase 1 sang tiếng Việt, giữ lịch sử cũ; bổ sung toàn bộ công việc và kết quả Phase 2. |

### Việc chưa giải quyết và thứ tự tiếp theo

- Phase này cung cấp nền tảng, chưa cài bản thật `BedAllocationApi` mà A đang chờ, dữ liệu demo 003 hay các chức năng 005/014. Những dòng của C ở team-plan mục 8 còn đúng trạng thái chờ/ghi nhớ; không có dòng nào đủ điều kiện đóng bởi riêng Phase 2.
- Bản thật của `AllocationResidencySource`/`DayCareArrivalCounter` (B/004), `AllocationInfectionStatus` (D/007), `LocationReferences` (E/008) còn cần khi làm nghiệp vụ liên quan. Các dòng yêu cầu đã có từ Phase 1; test hiện dùng fake của chủ interface.
- Adapter nguồn hồ sơ/chuyển trạng thái M01 và mốc kết thúc lưu trú M02 cần được xác nhận khi tích hợp user story; các truy vấn tương lai chưa thể được kiểm bằng dữ liệu phân bổ/cách ly thật ở phase này.
- Tiếp theo đúng tasks.md là **T011**: test cơ cấu/mã duy nhất/ngừng hiệu lực, kiểm 409/403 và tham chiếu vị trí. Sau đó **T012**, **T013** (test chính sách/cha/chuẩn giường/ghi tăng tài sản), rồi phần cài **T014 → T015 → T016 → T017 → T018**. Không triển khai những task này trong lượt Phase 2.
- Thêm FK `bed.last_closed_allocation_id` tại T023/V003_03; đối chiếu tên bảng tương lai với provider khi làm T023/T034/T054. Dịch/duyệt hợp đồng REST trước controller; không tự đổi endpoint/trường.

### Kết quả chốt Phase 2

- Tiến trình nền kiểm cuối sau review kết thúc với **BUILD SUCCESS**, exit 0: **165 test đơn vị/kiến trúc và 424 IT**, không thất bại/lỗi/bỏ qua; Spotless check qua; tổng **01:40**. Log `003-p2-final-verify.log` xác nhận kết quả. Chỉ sau kết quả này mới đánh `[x]` T003–T010; T011 và các task sau còn chưa đánh.
- Review độc lập sau sửa không còn phát hiện cụ thể trong hai phần đã sửa. Không có lỗi build/test còn tồn tại ở mốc chốt; các giới hạn tích hợp/user story được ghi ở trên vẫn chưa triển khai.
- Không commit, push hay tạo PR; không chạy frontend/mobile hoặc chụp màn hình vì không thay đổi giao diện.
- Rà cuối: `git diff --check` không báo lỗi; danh sách 85 file mã/test/migration mới có đủ trong bảng lịch sử (thiếu 0), không có file mới ngoài M03, test/fake M03 và migration V003. Các file được theo dõi chỉ sửa plan/tasks 003 và mục glossary 003. Failsafe summary xác nhận 424 completed, 0 errors/failures/skips/flakes; kiểm lịch sử không chứa mật khẩu đã cung cấp.

## 2026-10-23 — Feature 003, Phase 3 (đang triển khai)

- Yêu cầu: T011–T018 theo tasks/plan và README, TDD, kiểm task + ArchTest, kiểm nền clean verify cuối phase trước [x]; bổ sung lịch sử tiếng Việt từ thay đổi thực tế.
- Baseline `7d64e43`, nhánh `feature/003-manage-bedroom`, chỉ Working-history.md chưa được Git theo dõi. Không thực hiện thao tác Git ghi.
- Đã đọc AGENTS.md, team-plan, tasks, Ghi chú triển khai plan, README backend/frontend và các phần dữ liệu/hợp đồng cần cho Phase 3; báo các dòng mục 8 giao cho C ở đầu phiên.
- Đã bổ sung C/003 vào dòng ResidencyEndApi của B/004 trong team-plan để ghi phụ thuộc retention Phase 2; thêm dòng B/001 cung cấp API/fake giới tính và mức hiện hành cho FR-006. Không đóng dòng đang chờ nào.
- Đã hỏi ba điểm cần quyền theo yêu cầu người dùng: dịch hợp đồng HTTP còn tiếng Việt; bổ sung khai báo/fake M01 ngoài phần sở hữu C hoặc chờ B; bổ sung thao tác đổi vị trí vào cổng 003↔019. Chưa làm thay đổi phụ thuộc câu trả lời.
- T014: viết FR_001_StructureEntitiesTest trước, chạy 2 test/2 assertion failure vì thiếu entity. Tạo Area, Floor, RoomType, Room, BedTransferReason ở internal/structure/domain và Bed ở internal/bed/domain; 5 danh mục kế thừa CatalogBase; Bed không cho JPA ghi cột state để giữ gateway hiện có. Chạy `mvnw.cmd spotless:apply test -Dtest=FR_001_StructureEntitiesTest,*ArchTest,ModuleBoundaryTest`: 25 test qua, BUILD SUCCESS. Log `C:\Users\nguye\AppData\Local\Temp\003-p3-t014-{red,green}.log`. Chưa đánh T014 hoàn thành vì chưa kiểm cuối phase.

### Trình tự tiếp theo và kết quả thực tế

1. T011 được viết thành ba IT trước hành vi service; các kiểu chữ ký và khung service tối thiểu chỉ để test biên dịch, chưa cài hành vi. Lần đầu có 8 lỗi nạp context vì `RoomType.standardBeds` ánh xạ INTEGER thay vì SMALLINT của migration Phase 2. Xác định nguyên nhân từ SchemaManagementException, sửa bằng `columnDefinition="SMALLINT"`, giữ migration đã có.
2. Chạy lại T011: **8 test, 8 assertion failure, 0 error/skip** do service chưa cài. Sau cài phần cơ cấu độc lập, `spotless:apply verify -Dtest=FR_001_StructureEntitiesTest,*ArchTest,ModuleBoundaryTest -Dit.test=FR_001_StructureIT,FR_003_UniqueCodesIT,FR_005_DeactivateStructureIT` qua **25 unit/kiến trúc + 8 IT**.
3. T015 dùng `StructureStore` giữ JPA/JDBC, `StructureService` kiểm quyền cấu hình qua `RequiresPermission`, khóa mục đang sửa, kiểm If-Match, chặn con/tham chiếu, ghi audit trong transaction. Tập mức phòng khởi tạo từ loại phòng; loại phòng đã ngừng vẫn giữ ở phòng cũ nhưng không chọn cho phòng mới. Chưa cài nhánh FR-006 với dữ liệu người cao tuổi thật.
4. T013 có hai test tạo giường/cảnh báo và rollback tài sản. Lần đầu **1 failure + 1 error**: stubbing spy của port MANDATORY đã gọi interceptor ngoài transaction. Thay bằng `@MockitoBean(name="fakeBedAssets")` override đúng bean trong config fake đã import, không tạo lớp fake thứ hai. Chạy lại: **2 assertion failure, 0 error/skip** trước phần cài T016.
5. T016 khóa phòng, persist thuộc tính giường bằng JPA, gọi `BedStateService.transition(CREATE_BED)` để ghi trạng thái/lịch sử/audit, gọi `BedAssetPort.recordIncreaseForBed`, refresh entity sau gateway JDBC. Cả chuỗi chung transaction; nguồn tài sản thiếu trong runtime thì báo SOURCE_UNAVAILABLE, không lắp fake tự động. Kiểm `spotless:apply verify -Dtest=*ArchTest,ModuleBoundaryTest -Dit.test=BR_M03_22_BedCreationIT`: **23 kiến trúc + 2 IT qua**.
6. Viết test bổ sung T015 về version/currentValue và audit delta: **2 test, 1 assertion failure** vì 412 trả dữ liệu hiện hành thiếu version. Thêm version trong snapshot cho đối chiếu, bỏ version khỏi delta audit; kiểm sau sửa **23 kiến trúc + 2 IT qua**.
7. Bổ sung kiểm hồi quy T011 cho mã phòng toàn viện, mã giường trùng trong phòng và được lặp ở phòng khác. Đây là kiểm mở rộng của hành vi đã cài, không ghi nhận là một vòng đỏ mới. Chạy riêng `FR_003_UniqueCodesIT` cùng ArchTest: **23 kiến trúc + 5 IT qua**.
8. Review độc lập bằng agent đọc mã, không chạy Maven/không sửa file, phát hiện thiếu audit cơ cấu/cảnh báo khi tạo giường. Đối chiếu data-model mục 9 xác nhận cần cả audit cơ cấu và audit chuyển trạng thái. Bổ sung assertion trước sửa: **2 IT, 1 failure** do chưa có `M03_STRUCTURE_CREATED`; cài thêm audit roomId/code/details.warnings cùng transaction, rồi kiểm **23 kiến trúc + 2 IT qua**. Test lỗi tài sản đồng thời kiểm audit/lịch sử/giường không tăng.
9. Review còn đề nghị chặn kích hoạt lại con dưới cha INACTIVE. Tra BR-M15-06/Q-271 trong nguồn nghiệp vụ chỉ thấy chặn trùng tên, FR-005 nêu điều kiện ngừng cha; đã hỏi người dùng chốt. Chưa tự thêm điều kiện nghiệp vụ và chưa coi điểm này đã giải quyết.
10. Khởi chạy nền `mvnw.cmd clean verify` cho toàn backend sau phần độc lập/sửa review. Kết quả được bổ sung khi tiến trình hoàn tất. **Phase 3 vẫn chưa hoàn thành và chưa đánh [x] T011–T018**, vì thiếu quyết định hợp đồng và phụ thuộc nghiệp vụ.

### File đã tạo/sửa/xóa trong lượt Phase 3

So với baseline `7d64e43`, có **21 file Java mới** dưới M03/test M03, không xóa file. Mọi file mới được liệt kê riêng dưới đây.

| Thao tác | Đường dẫn | Mục đích |
| --- | --- | --- |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/bed/domain/Bed.java` | T014: entity thuộc tính giường; cột state chỉ đọc với JPA, không tạo đường ghi trạng thái thứ hai. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/bed/domain/BedCreation.java` | T016: kết quả nội bộ của lệnh tạo giường, gồm entity và danh sách cảnh báo. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/bed/domain/BedWarning.java` | T016: mã/mô tả cảnh báo không chặn tạo giường. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/bed/persistence/BedStore.java` | T016: persist thuộc tính giường, đếm giường chưa RETIRED, refresh version sau gateway JDBC. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/bed/service/BedService.java` | T016: kiểm quyền tạo, khóa phòng, gọi gateway trạng thái và ghi tăng tài sản cùng transaction; audit cơ cấu/cảnh báo. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/structure/domain/Area.java` | T014: danh mục khu vực, mã và vòng đời CatalogBase. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/structure/domain/BedTransferReason.java` | T014: danh mục lý do chuyển, mã, cờ y tế/an toàn và dấu dòng cố định. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/structure/domain/Floor.java` | T014: danh mục tầng, khu vực cha, mã và tên tòa nhà tùy chọn. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/structure/domain/Room.java` | T014: danh mục phòng, tầng/loại phòng, chính sách giới tính, bản sao tập mức; không cho JPA sửa cách ly. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/structure/domain/RoomType.java` | T014: danh mục loại phòng, chuẩn số giường SMALLINT, tập mức và loại lưu trú được phục vụ. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/structure/domain/StructureChange.java` | T015: dữ liệu lệnh nội bộ dùng chung cho năm danh mục, không phải request HTTP. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/structure/domain/StructureKind.java` | T015: enum giới hạn năm loại danh mục, tránh nhận tên bảng tùy ý. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/structure/persistence/StructureStore.java` | T015: truy cập entity qua JPA, đọc/khóa giường và kiểm tham chiếu M03 bằng JDBC. |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/structure/service/StructureService.java` | T015 phần độc lập: CRUD/vòng đời danh mục, quyền thật, If-Match, chặn con/tham chiếu, audit delta; nhánh M01 còn chờ. |
| Tạo | `backend/src/test/java/com/cambae/backend/m03/BR_M03_22_BedCreationIT.java` | T013/T016: vượt chuẩn vẫn tạo/cảnh báo, nhật ký tạo lưu cảnh báo, lỗi tài sản rollback giường/lịch sử/audit. |
| Tạo | `backend/src/test/java/com/cambae/backend/m03/FR_001_StructureEntitiesTest.java` | T014: kiểm nhóm dữ liệu/khung entity và chặn đường JPA sửa trạng thái giường. |
| Tạo | `backend/src/test/java/com/cambae/backend/m03/FR_001_StructureIT.java` | T011: dựng cơ cấu, sao mức từ loại phòng, quyền Trưởng tầng và giữ loại phòng đã ngừng trên phòng cũ. |
| Tạo | `backend/src/test/java/com/cambae/backend/m03/FR_003_StructureVersionIT.java` | T015: 412 trả currentValue/version mới và nhật ký chỉ ghi thuộc tính thực sự thay đổi. |
| Tạo | `backend/src/test/java/com/cambae/backend/m03/FR_003_UniqueCodesIT.java` | T011: mã khu/tầng/phòng/giường duy nhất và cho mã giường lặp ở phòng khác. |
| Tạo | `backend/src/test/java/com/cambae/backend/m03/FR_005_DeactivateStructureIT.java` | T011: giường SUSPENDED/con ACTIVE/phân công chặn ngừng, lịch sử tham chiếu chặn xóa. |
| Tạo | `backend/src/test/java/com/cambae/backend/m03/StructureTestSupport.java` | T011/T013: fixture id riêng, tài khoản TestUsers quyền thật, SwitchableUser, import FakeM03Config/FakeM15Config. |
| Sửa | `docs/team-plan.md` | Bổ sung C/003 đang chờ ResidencyEndApi; thêm dòng B/001 công bố API/fake cho kiểm chính sách phòng. Không đóng dòng chưa hoàn thành. |
| Sửa | `specs/003-room-bed-allocation/tasks.md` | Ghi mapping tên/đường dẫn thực, thứ tự entity trước service, các phần bị chặn; giữ task Phase 3 chưa đánh. |
| Sửa | `specs/003-room-bed-allocation/plan.md` | Ghi quyết định kỹ thuật, phụ thuộc thiếu, các vòng đỏ/xanh, review và câu hỏi cần chốt. |
| Sửa | `specs/000-shared-foundation/glossary.md` | Chỉ mục 9: tên entity/store/service, kiểu lệnh nội bộ, cảnh báo/errorCode/audit mới; chưa thay hợp đồng HTTP. |
| Sửa | `Working-history.md` | Bổ sung lịch sử Phase 3 đang làm bằng tiếng Việt, giữ nguyên lịch sử trước đó. |

### Quan hệ file và luồng dữ liệu Phase 3 đã cài

- `StructureChange` + `StructureKind` mô tả lệnh nội bộ → `StructureService` qua interceptor quyền thật 002 → `StructureStore` khóa/đọc entity `CatalogBase` → các entity Area/Floor/RoomType/Room/BedTransferReason cập nhật → flush JPA → `AuditLogApi` ghi thay đổi cùng transaction. Không có controller/request/response HTTP mới trong lượt này khi chưa có phép sửa hợp đồng.
- RoomType giữ tập mức ở `room_type_care_level`; Room sao sang `room_allowed_care_level` khi tạo nếu người dùng chưa truyền tập riêng. Việc sửa loại phòng không thay ngược bản sao ở phòng cũ.
- Khi ngừng hiệu lực, store đọc con ACTIVE/giường chưa RETIRED, service gom thêm `LocationReferences.currentReferences`; xóa kiểm cả `everReferenced` và FK nội bộ. Dữ liệu phân bổ tương lai được kiểm tồn tại bảng trước truy vấn; nhánh có phân bổ thật chưa được nghiệm thu ở phase này khi chưa có T023/M01.
- `BedService.create` kiểm quyền CREATE, khóa phòng → `BedStore.create` ghi room_id/code → gateway Phase 2 ghi CREATE_BED + history + audit trạng thái → port 019 ghi tăng tài sản → refresh version → đếm giường chưa RETIRED, tạo BedWarning nếu vượt chuẩn → audit cơ cấu gồm roomId/code và cảnh báo → trả `BedCreation`. Phản hồi HTTP 201 chưa được cài/test, vì T017 đang chờ hợp đồng.
- `Bed` có state chỉ đọc trong JPA; `BedStore` không chứa SQL đổi state. `SingleBedStateWriterArchTest` vẫn xanh. SQL thời gian của gateway dùng BusinessTime/ParameterApi đã có từ Phase 2.
- `StructureTestSupport` tạo tài khoản bằng `TestUsers` với quyền thật của 002; chỉ thay người dùng hiện hành bằng SwitchableUser và dùng config fake của chủ SPI. Không dùng FakePermissions để nới ma trận quyền.

### Quyết định kỹ thuật và giới hạn hiện tại

- Dùng JPA đã có và CatalogBase theo README/quy ước nhóm dữ liệu, không thêm thư viện hay sửa cấu hình chung. Bed giữ nhóm BUSINESS vì có trạng thái; các phương thức sửa thuộc tính không cho đổi state.
- T014 được chuẩn bị trước T015 để test/service làm việc với entity thật; thay thứ tự kỹ thuật này đã ghi cả tasks/plan. Không kéo migration phân bổ T023 lên Phase 3.
- Snapshot cho If-Match có version, audit chỉ ghi delta nghiệp vụ; không ghi toàn bộ giá trị không đổi. Dịch vụ trả lỗi thiếu nguồn khi cần adapter, không đoán hồ sơ người cao tuổi hoặc tự tạo dữ liệu nguồn.
- JPA và JDBC dùng transaction Spring hiện có; refresh Bed sau khi gateway JDBC tăng version để tránh giữ entity cũ trong persistence context.
- Nhận thiếu audit cơ cấu từ review vì có căn cứ rõ ở data-model mục 9. Với gợi ý chặn kích hoạt con dưới cha INACTIVE, chưa tự thêm rule khi nguồn chưa nêu rõ; rủi ro hiện tại là có thể có con ACTIVE trong cha INACTIVE, nên cần người dùng chốt trước hoàn tất phase.
- Không đổi hợp đồng REST/API M01/BedAssetPort, không sửa phần sở hữu B, không thêm endpoint hoặc field tùy ý. Không làm web/mobile, chạy gen API hoặc chụp màn hình trong lượt đang chờ quyết định này.

### Lệnh và log kiểm chứng Phase 3

Các lệnh Maven chạy từ `backend/`, JDK 25 ở đường dẫn người dùng cung cấp, biến TEST_DB_* chỉ nạp cho process. Không lưu thông tin đăng nhập vào kho mã.

| Phần | Bằng chứng |
| --- | --- |
| T014 | `test -Dtest=FR_001_StructureEntitiesTest`: 2/2 failure trước cài; `spotless:apply test -Dtest=FR_001_StructureEntitiesTest,*ArchTest,ModuleBoundaryTest`: 25 qua. |
| T011 | `test-compile failsafe:integration-test failsafe:verify -Dit.test=FR_001_StructureIT,FR_003_UniqueCodesIT,FR_005_DeactivateStructureIT`: lần mapping lỗi 8 error; sau sửa mapping 8/8 failure; sau cài `verify` giới hạn task + ArchTest: 25 unit/kiến trúc + 8 IT qua. |
| T013/T016 | `test-compile failsafe:integration-test failsafe:verify -Dit.test=BR_M03_22_BedCreationIT`: 1 failure/1 error do test spy, rồi 2/2 failure hành vi; sau cài `spotless:apply verify -Dtest=*ArchTest,ModuleBoundaryTest -Dit.test=BR_M03_22_BedCreationIT`: 23 kiến trúc + 2 IT qua. |
| T015 version/audit | `FR_003_StructureVersionIT`: 2 test/1 failure trước sửa version, sau sửa 23 kiến trúc + 2 IT qua. |
| T011 mở rộng | `verify -Dtest=*ArchTest,ModuleBoundaryTest -Dit.test=FR_003_UniqueCodesIT`: 23 kiến trúc + 5 IT qua. |
| Sửa review audit giường | `BR_M03_22_BedCreationIT`: 2 test/1 failure trước audit mới, sau sửa 23 kiến trúc + 2 IT qua. |
| Toàn backend phần độc lập | `mvnw.cmd clean verify` chạy nền: BUILD SUCCESS, exit 0, Spotless qua, 167 unit/kiến trúc + 439 IT, 0 failure/error/skip, tổng 01:39. |

Log ngoài kho mã tại `C:\Users\nguye\AppData\Local\Temp\`: `003-p3-t014-{red,green}.log`, `003-p3-t011-red.log`, `003-p3-t011-red-behavior.log`, `003-p3-t011-green.log`, `003-p3-t013-red.log`, `003-p3-t013-red-behavior.log`, `003-p3-t013-green.log`, `003-p3-version-{red,green}.log`, `003-p3-unique-extra-green.log`, `003-p3-review-audit-{red,green}.log`, `003-p3-independent-clean-verify.log`. Không chép log runtime chứa thông tin nhạy cảm vào lịch sử.

### Việc còn thiếu và thứ tự tiếp tục

1. **T011**: bổ sung kiểm HTTP 403/409 và rà các ca khi có dữ liệu phân bổ thật sau khi đủ hợp đồng/nguồn.
2. **T012**: viết test đổi chính sách phòng và đổi cha có lịch sử phân bổ; còn thiếu API/fake M01 do B khai báo, cùng quyết định cổng đồng bộ vị trí giường sang tài sản. Không tự tạo bảng giả thay migration T023 hoặc API M01 thay B.
3. **T013**: đã có phần service/cảnh báo/rollback, còn xác minh phản hồi HTTP 201 ở T017; **T014** có entity và kiểm, chưa đánh [x] trước chốt phase.
4. **T015**: hoàn thiện FR-006 khi đủ API/fake; chốt kích hoạt con dưới cha INACTIVE. **T016**: phần tạo giường đã cài, bản thật port tài sản vẫn do 019 cung cấp.
5. **T017**: cần quyết định cho phép dịch hợp đồng sang tiếng Anh; bổ sung các GET chi tiết/ETag và DELETE/reactivate còn thiếu; sau đó cài controller và LocationStructureApi/RoomPolicyApi.
6. **T018**: sinh client theo hợp đồng được duyệt, làm màn `frontend/src/features/beds/`, test/lint/build và kiểm giao diện khi thực sự thay đổi UI.
7. Cuối Phase 3 đầy đủ mới chạy nền clean verify lại và các lệnh frontend, đóng task đủ điều kiện và cập nhật team-plan các việc bàn giao thực sự hoàn thành. Hiện chưa commit/push/PR.

### Mốc kiểm phần backend độc lập trước khi chờ quyết định

- `mvnw.cmd clean verify` nền đã kết thúc **BUILD SUCCESS**, exit 0, **167 test đơn vị/kiến trúc + 439 IT**, 0 failure/error/skip; Spotless check qua, tổng **01:39**. Log `003-p3-independent-clean-verify.log`. Đây là kiểm phần code đã làm, không phải tuyên bố Phase 3 đủ chức năng.
- `git diff --check` không báo lỗi; 21 file Java mới có đủ trong lịch sử (thiếu 0) và nằm trong M03/test M03 (ngoài phạm vi 0). Không có code mới ở M01, frontend hoặc các module khác; không sửa API contract hay thêm thư viện.
- Phase 3 còn T012, phần FR-006/T015, controller/API T017 và web T018; các task T011–T018 vẫn chưa đánh. Những câu hỏi đã gửi chưa có trả lời tại mốc này; không coi thời gian chờ là sự đồng ý.


### 2026-10-23 — Tiếp tục Phase 3 sau khi chủ dự án trả lời

- Chủ dự án duyệt dịch riêng phần Phase 3 của HTTP contract sang tiếng Anh, bổ sung GET chi tiết/ETag cho khu vực, tầng, loại phòng, lý do chuyển và DELETE/reactivate còn thiếu của hai danh mục sau. Có 39 operation mang `x-implementation-phase: 3`; 31 operation tương lai giữ tên logic cũ. Clone các schema/parameter/requestBody/response tiếng Anh để không đổi ngầm hợp đồng của story chưa triển khai. Ba path cách ly trùng tiền tố phòng được đối chiếu và giữ nguyên từ baseline.
- Chủ dự án chọn giữ phân công M01: chờ B công bố API/fake giới tính và mức chăm sóc, không viết hộ. Không duyệt thêm thao tác đổi vị trí vào BedAssetPort; đổi phòng của giường chưa phân bổ tạm trả 503 SOURCE_UNAVAILABLE, không thay dữ liệu/tài sản. Giường đã có lịch sử phân bổ vẫn bị chặn FR-007. Web chỉ cho sửa mã giường, khóa lựa chọn phòng khi sửa.
- Chủ dự án chốt kích hoạt cha trước con. Bổ sung hai test phòng/tầng dưới cha INACTIVE: 5 IT có 2 failure trước cài; sau cài kiểm task và 23 ArchTest qua. Không sửa spec hoặc tài liệu nghiệp vụ; đây là quyết định đã hỏi trực tiếp.
- T017: sáu controller theo danh mục, request/response riêng, không trả entity JPA. GET chi tiết trả ETag; lệnh dùng If-Match, Idempotency-Key và quyền thật ROOM_BED_CONFIGURATION. GET bảng giường lọc areaId/floorId/roomId/state, trả vị trí đầy đủ, trạng thái/hạn/quyền lệnh; dữ liệu người ở, phân bổ tương lai và các dấu của story sau chưa được cài. Không bịa dữ liệu người cao tuổi.
- StructureQueries cài LocationStructureApi/RoomPolicyApi: đọc cơ cấu M03, hiệu lực cả cha, tập tầng, loại phòng, mức cho phép và điều kiện nguồn qua SPI đã có. Bảng phân bổ/khu nghỉ chưa tồn tại được nhận diện trước truy vấn; phải đối chiếu tên cột khi làm migration tương ứng. BedQueries đọc projection BedDetails, quyền từ PermissionApi, thời gian BusinessTime; đọc port MANDATORY nằm trong transaction.
- Kiểm HTTP có 4 rồi 6 test thất bại trước controller đầy đủ; kiểm API đọc có 2 thất bại trước cài. Test bổ sung lý do thiếu đòi 422 REASON_REQUIRED nhưng nhận 400: bỏ Bean Validation @NotNull riêng ReasonBody để BusinessCommandAspect dùng quy tắc lỗi chung; không bỏ kiểm lý do. Kiểm sau sửa có 23 ArchTest + 10 IT (ApiContractIT, HTTP 7, API đọc 2) qua. Lý do tùy chọn của AreaWrite/RoomWrite được chuyển vào StructureChange và audit.
- ApiContractIT chỉ đăng ký hợp đồng 003 và danh sách 31 operation chưa cài; không bỏ kiểm hợp đồng 000/002. Đây là đăng ký test dùng chung cần A review khi mở PR. Generator thêm 003 và chỉ sinh operation được duyệt; chạy npm run gen:api, không sửa tay file sinh.
- T018 dùng frontend/src/features/beds, các thành phần Field/Input/Button/Sheet và token có sẵn. Cây bốn cấp, tab loại phòng/lý do, form CRUD, 409 ProblemAlert, 412 hiển thị giá trị mới và tải lại ETag, cảnh báo tạo giường được giữ sau khi tải lại. Quyền nút lấy từ /me/permissions, không tự suy từ role.
- Web: bốn test hành vi đỏ trên khung ban đầu, xanh sau cài; thêm một test hồi quy giữ cảnh báo tạo giường, tổng 5 test mới. Lint phát hiện set-state-in-effect trong luồng tải; chuyển việc tải sang promise có cờ hủy và callback dùng cho thao tác. Không tắt luật lint, không thêm thư viện. npm ci chỉ cài theo lockfile hiện có.
- Kiểm cuối frontend: npm test -- --run qua 105/105 test; npm run lint và npm run build exit 0. Build còn cảnh báo bundle khoảng 630 kB > 500 kB, chưa mở rộng phạm vi tối ưu bundle.
- Kiểm giao diện bằng build và fixture trong bộ nhớ trên localhost: cây bốn cấp, form tạo G2, lỗi 409 khi ngừng phòng còn G1. Chụp màn hình vì có thay đổi UI; fixture không ghi CSDL thật và không có dữ liệu người cao tuổi. Backend được kiểm riêng trên MySQL test. Ảnh kiểm lỗi ở C:/Users/nguye/AppData/Local/Temp/003-p3-web-409.png.
- Chưa tuyên bố cả Phase 3 hoàn tất: T012 và phần FR-006/T015 còn thiếu nguồn M01/bảng phân bổ T023; thao tác đổi phòng giường còn thiếu đồng bộ tài sản. T017 giữ chưa đánh để phản ánh phần hợp đồng sửa giường chưa đủ; T018 chỉ nghiệm thu phần màn cơ cấu đã cài. Task chưa xong đầu tiên vẫn là T012; không chuyển sang T019 trong lượt này.

### Danh sách file bổ sung và cập nhật sau quyết định của chủ dự án

Các file mới của phần backend độc lập đã liệt kê phía trên vẫn giữ nguyên; bảng sau bổ sung các file thực sự được tạo/sửa trong lượt tiếp tục. Không có file bị xóa. Tên dưới đây là đường dẫn tương đối từ thư mục gốc.

| Thay đổi | File | Mục đích |
| --- | --- | --- |
| Tạo | backend/src/main/java/com/cambae/backend/m03/internal/bed/domain/BedDetails.java | Projection dữ liệu giường/vị trí/hạn/quyền cho màn cơ cấu. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/internal/bed/service/BedQueries.java | Truy vấn có quyền VIEW, tính quá hạn và đọc dấu tài sản qua port. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/internal/structure/service/StructureQueries.java | Cài hai API đọc cơ cấu/chính sách đã khai báo Phase 2. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/web/AreaController.java | HTTP khu vực, ETag và các lệnh danh mục. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/web/FloorController.java | HTTP tầng và lọc khu vực. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/web/RoomTypeController.java | HTTP loại phòng, gồm detail/delete/reactivate đã được duyệt. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/web/RoomController.java | HTTP phòng, chính sách và cô lập DTO. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/web/BedTransferReasonController.java | HTTP lý do chuyển, gọi service bảo vệ dòng cố định. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/web/BedController.java | GET/POST giường, detail ETag, PUT sửa mã và chặn đổi phòng chưa hỗ trợ. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/web/request/AreaWrite.java | Trường ghi khu vực và reason tùy chọn. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/web/request/FloorWrite.java | Trường ghi tầng, mã/tên và tên tòa nhà. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/web/request/RoomTypeWrite.java | Trường ghi loại phòng và các mức mặc định. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/web/request/RoomWrite.java | Trường ghi phòng/chính sách và reason tùy chọn. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/web/request/BedTransferReasonWrite.java | Trường ghi mã/tên/cờ y tế. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/web/request/BedWrite.java | roomId/code cho lệnh giường. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/web/request/ReasonBody.java | Lý do lệnh trạng thái; để interceptor chung trả REASON_REQUIRED. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/web/response/AreaResponse.java | DTO đọc khu vực. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/web/response/FloorResponse.java | DTO đọc tầng. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/web/response/RoomTypeResponse.java | DTO loại phòng và mức mặc định. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/web/response/RoomResponse.java | DTO phòng và chính sách hiện hành. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/web/response/BedTransferReasonResponse.java | DTO lý do và cờ cố định. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/web/response/LocationResponse.java | Vị trí đầy đủ khu vực/tầng/phòng/giường. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/web/response/BedResponse.java | DTO chi tiết giường, không đọc hồ sơ M01. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/web/response/BedBoardResponse.java | Bao dữ liệu danh sách theo hợp đồng. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/web/response/BedCreationResponse.java | Phản hồi 201 có giường và cảnh báo. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/web/response/WarningResponse.java | Mã và mô tả cảnh báo. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/web/response/BedFlagCode.java | Enum dấu đúng schema cho các story sau. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/web/response/ResidentSummarySchema.java | Mô tả OpenAPI của trường người ở; chưa có dữ liệu runtime Phase 3. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/web/response/StructureProblem.java | Mô tả OpenAPI Problem 003; handler chung vẫn dựng lỗi thực tế. |
| Tạo | backend/src/test/java/com/cambae/backend/m03/FR_001_StructureHttpIT.java | 7 kiểm HTTP quyền, ETag, cảnh báo, chặn, lý do và sửa mã giường. |
| Tạo | backend/src/test/java/com/cambae/backend/m03/FR_017_StructureApiIT.java | 2 kiểm API cơ cấu và chính sách qua MySQL. |
| Sửa tiếp file mới | backend/src/main/java/com/cambae/backend/m03/internal/structure/domain/StructureChange.java | Truyền reason tùy chọn tới audit. |
| Sửa tiếp file mới | backend/src/main/java/com/cambae/backend/m03/internal/structure/persistence/StructureStore.java | Truy vấn cơ cấu hiệu lực, API chính sách, guard bảng chưa tạo. |
| Sửa tiếp file mới | backend/src/main/java/com/cambae/backend/m03/internal/structure/service/StructureService.java | Bảo vệ kích hoạt cha trước con; giữ reason. |
| Sửa tiếp file mới | backend/src/main/java/com/cambae/backend/m03/internal/bed/persistence/BedStore.java | Projection/list có bộ lọc và khóa phục vụ cập nhật mã. |
| Sửa tiếp file mới | backend/src/main/java/com/cambae/backend/m03/internal/bed/service/BedService.java | Sửa mã giường, chặn đổi phòng chưa hỗ trợ và audit thuộc tính. |
| Sửa tiếp file mới | backend/src/test/java/com/cambae/backend/m03/FR_005_DeactivateStructureIT.java | Thêm 2 test kích hoạt con khi cha INACTIVE. |
| Sửa | backend/src/test/java/com/cambae/backend/common/ApiContractIT.java | Đăng ký kiểm HTTP 003 và 31 operation tương lai; cần A review. |
| Sửa | specs/003-room-bed-allocation/contracts/api-003.yaml | Phần tiếng Anh Phase 3 được duyệt, clone component bảo toàn phần tương lai. |
| Sửa | frontend/scripts/gen-api.mjs | Sinh 003 chỉ từ operation đã duyệt. |
| Tạo bằng generator | frontend/src/api/api003.js | 39 hàm client từ hợp đồng, không sửa tay. |
| Sinh lại | frontend/src/api/index.js | Export client 003. |
| Sinh lại, nội dung không đổi | frontend/src/api/api000.js, frontend/src/api/api002.js, frontend/src/api/client.js | Generator chạy lại; git có cảnh báo LF/CRLF, không có diff nội dung ba file này. |
| Sửa | frontend/src/App.jsx | Import BedsPage và gắn đúng mục beds của màn 003. |
| Tạo | frontend/src/features/beds/bedCatalog.js | Khai báo trường/client/tên danh mục và nhãn enum. |
| Tạo | frontend/src/features/beds/StructureForm.jsx | Form dùng component chung, ETag/idempotency/lý do/lỗi/cảnh báo. |
| Tạo | frontend/src/features/beds/BedsPage.jsx | Cây bốn cấp, hai tab danh mục và quyền nút từ API. |
| Tạo | frontend/src/features/beds/BedsPage.test.jsx | 5 test hành vi màn hình bằng generated-client mock. |
| Sửa | specs/000-shared-foundation/glossary.md | Chỉ mục 9: mapping HTTP/trường/schema/API đọc/web mới. |
| Sửa | specs/003-room-bed-allocation/tasks.md | Trạng thái task và giới hạn còn chờ sau kiểm cuối. |
| Sửa | specs/003-room-bed-allocation/plan.md | Quyết định được duyệt, thiết kế, kiểm chứng và giới hạn. |
| Sửa | docs/team-plan.md | Ghi rõ chờ B/M01, hoãn đồng bộ tài sản, việc cần A review; không đánh Xong các handoff chưa làm. |
| Bổ sung | Working-history.md | Giữ lịch sử cũ, ghi tiếp quyết định, trình tự, file/dataflow, kiểm và việc còn thiếu. |

### Luồng HTTP và web sau khi tiếp tục

1. BedsPage tải danh mục và quyền qua generated client api003/api002; cây liên kết areaId → floorId → roomId. Các nút ghi chỉ hiện khi API quyền cho phép.
2. StructureForm đọc detail và ETag trước sửa/lệnh, dựng đúng request contract, gửi Idempotency-Key và If-Match qua client chung. Controller ánh xạ request → StructureChange/service hoặc BedService, không truy cập M01.
3. Service dùng transaction, khóa/JPA/JDBC hiện có, kiểm quyền/quy tắc, ghi AuditLogApi; response mapper chuyển entity/projection thành DTO và ETag. Lỗi 409/412/422 từ handler chung được ProblemAlert/form hiển thị, không ghi lại dữ liệu khi thất bại.
4. BedService tạo giường vẫn gọi gateway trạng thái và port tài sản trong transaction; kết quả 201 chứa cảnh báo riêng, không coi cảnh báo là thất bại. UI giữ cảnh báo sau khi tải lại cây.
5. StructureQueries cung cấp API nội bộ cho module khác; BedQueries cấp dữ liệu hiển thị. Phân bổ/người ở/dấu của các story sau chưa được nối; không tuyên bố kiểm end-to-end bản thật của 001/004/007/008/019.

### Kết quả chốt lượt tiếp tục Phase 3

- Kết quả chốt lượt tiếp tục: `mvnw.cmd clean verify` nền **BUILD SUCCESS**, exit 0, **167 unit/kiến trúc + 450 IT**, 0 failure/error/skip, tổng **02:02**; Spotless check qua. Đối chiếu tổng trực tiếp từ XML surefire/failsafe. Frontend **105 test**, lint/build exit 0; còn cảnh báo bundle >500 kB. Log cuối ngoài repo: `003-p3-final-clean-verify.log`, `003-p3-web-final-test.log`, `003-p3-web-final-lint.log`, `003-p3-web-final-build.log`.
- Review độc lập phần HTTP/API đọc/web không phát hiện Critical/Important mới. Sau kiểm cuối mới đánh [x] T011/T013/T014/T016/T018; giữ T012/T015/T017 chưa đánh vì thiếu phần đã ghi rõ. Team-plan không có dòng bàn giao cũ nào của C đủ điều kiện Xong trong Phase 3: BedAllocationApi, demo phân bổ, OfflineHandler và SPI chuyến vẫn ở story/feature sau. Đã thêm việc chờ cụ thể thay vì báo Xong sai phạm vi.
- `git diff --check` qua; đối chiếu danh sách file mới với Working-history.md thiếu 0 đường dẫn. Không có migration mới, thư viện mới, thay đổi M01 hoặc BedAssetPort; không commit/push/tạo PR. Nhánh vẫn feature/003-manage-bedroom. Bước tiếp theo theo tasks là **T012**, rồi hoàn thiện T015/T017 trước khi nghiệm thu Phase 3 và mở T019.

- Đã dừng hai tiến trình Node preview fixture do phiên này tạo trên cổng 5187/5188 sau khi xác minh command line, và đóng tab kiểm thử. Ảnh kiểm lỗi 409 vẫn ở thư mục Temp nêu trên.

## 2026-10-23 — Feature 003, khởi động Phase 4 (chưa hoàn tất)

### Yêu cầu, trạng thái và trình tự thực tế

1. Người dùng yêu cầu Phase 4 theo AGENTS.md/team-plan.md, test trước, kiểm riêng task cùng ArchTest và clean verify nền cuối phase; bổ sung lịch sử tiếng Việt, giữ nội dung cũ. Đã đọc phần lịch sử trước và giữ nguyên toàn bộ, chỉ nối thêm mục này.
2. Đầu phiên HEAD là d21e45e (003 phase 3), nhánh feature/003-manage-bedroom; Working-history.md đã tồn tại nhưng chưa được Git theo dõi. Đã báo các dòng giao C ở mục 8: BedAllocationApi, cổng vị trí tài sản tạm hoãn, review contract/generator với A, OfflineHandler, SPI chuyến 014, demo phân bổ; thêm các dòng Mọi người về TestUsers/cấu trúc package.
3. Đọc AGENTS.md, CLAUDE.md, constitution, team-plan, backend/README; đọc tasks.md và plan.md/Ghi chú triển khai trước khi tra thiết kế. Đối chiếu data-model mục 4, contracts/module-interfaces và phần HTTP Phase 4, research G-02/G-03/G-05, các FR phân bổ cần thiết. Không sửa nghiệp vụ/spec.
4. Chủ dự án trả lời lựa chọn 1, duyệt dịch riêng HTTP Phase 4 như Phase 3. Chưa sửa contract/client vì chưa tới triển khai HTTP. Đã hỏi việc giữ chờ B hay cho bổ sung API/fake M01, chưa nhận quyết định mới tại mốc kết thúc lượt này.
5. M01 chỉ có backend/src/main/java/com/cambae/backend/m01/package-info.java; chưa có nguồn trạng thái/giới tính/mức hiện hành và fake do B sở hữu. Bổ sung phụ thuộc Phase 4 vào dòng chờ B đã có ở team-plan, không tự viết nguồn M01. Khảo sát ban đầu nhắc tới cổng khóa M01 là chưa chính xác: G-03 chỉ đòi M01 giữ khóa khi gọi từ lệnh trạng thái 001, 003 tự khóa resident_position rồi giường; đã đính chính, không thêm cổng khóa mới.
6. Viết DBR_09_AllocationSchemaIT trước production. Năm ca dự kiến bắt lỗi thiếu bảng/view, chồng khoảng khi INSERT/UPDATE nhưng cho khoảng kề nhau, hai phân bổ FUTURE/ACTIVE theo cùng người, sửa/xóa bản CANCELLED/CLOSED và các CHECK trạng thái/thời gian/bác sĩ. Đây mới là một phần T020/T022, chưa có test 10 luồng hoặc toàn bộ DB constraints.
7. JAVA_HOME ban đầu trỏ JDK 24; tìm được JDK 25 ở E:/JAVA/JDK_GE/INS_25. Chỉ đổi JAVA_HOME trong process, không sửa cấu hình máy/pom.xml. Maven trong sandbox không chạy được; chạy lại với quyền dùng cache đã tới Failsafe.
8. Test schema biên dịch thành công nhưng cả 5 ca lỗi khởi tạo do thiếu TEST_DB_URL; chưa chạy assertion. Không coi lỗi môi trường là RED hợp lệ; chưa viết migration, entity hoặc service. Kiểm ba biến TEST_DB_* ở User/Machine cũng chưa có, chỉ in trạng thái có/không, không in bí mật.
9. Chạy spotless:apply thành công, chỉ định dạng test Java mới. Chạy clean verify nền toàn backend: thất bại ở BackendApplicationTests.contextLoads do cùng biến TEST_DB_URL thiếu; chưa tới Failsafe. Giữ toàn bộ task Phase 4 chưa đánh [x].

### Danh sách file tạo/sửa/xóa và mục đích

| Thao tác | File | Mục đích |
| --- | --- | --- |
| Tạo | backend/src/test/java/com/cambae/backend/m03/DBR_09_AllocationSchemaIT.java | Khởi đầu test MySQL trực tiếp cho T020/T022 trước migration T023. |
| Sửa | docs/team-plan.md | Mở rộng dòng chờ B/M01 sẵn có để phản ánh trạng thái/giới tính/mức chăm sóc cần cho T019/T025/T026. |
| Sửa | specs/003-room-bed-allocation/tasks.md | Ghi chú khởi động Phase 4 và các phụ thuộc; giữ T019–T028 chưa đánh. |
| Bổ sung | specs/003-room-bed-allocation/plan.md | Ghi quyết định duyệt dịch, giới hạn, test đã chạy/kết quả và thứ tự tiếp theo. |
| Bổ sung | Working-history.md | Nối lịch sử lượt này, giữ nội dung cũ và trạng thái chưa được Git theo dõi. |

Không xóa file; không có thay đổi Java production, migration, HTTP contract, glossary, generated client hoặc UI. Chưa phát sinh thuật ngữ mã nguồn mới ngoài tên lớp test nên chưa sửa glossary.

### Quan hệ file và luồng dữ liệu

- DBR_09_AllocationSchemaIT kế thừa StructureTestSupport, dùng fixture cơ cấu/giường và TestUsers hiện có; không thêm fake module khác. JdbcTemplate dự kiến ghi trực tiếp bed_allocation để kiểm ràng buộc MySQL, tách kiểm ở DB khỏi service sẽ được viết sau.
- MySqlIntegrationTestBase cần TEST_DB_URL/TEST_DB_ADMIN_USER/TEST_DB_ADMIN_PASSWORD, chuẩn bị schema và cho Flyway chạy trước assertion. Phiên này dừng ở khởi tạo context do thiếu TEST_DB_URL, nên chưa thực hiện được luồng kiểm constraints.
- Các test được viết dựa trên bảng T023/data-model mục 4; migration chưa tồn tại. Không có dữ liệu phân bổ runtime mới, không có luồng HTTP/ghi audit mới để báo đã thực hiện.
- team-plan ghi đầu vào còn chờ B; tasks/plan ghi đúng phạm vi test đang chuẩn bị. Các file tài liệu không biến task chưa kiểm được thành task hoàn tất.

### Lệnh và kết quả xác minh

Chạy Maven từ backend/, dùng E:/JAVA/JDK_GE/INS_25 trong process; không ghi mật khẩu vào repo.

| Lệnh | Kết quả thực tế |
| --- | --- |
| mvnw.cmd test-compile failsafe:integration-test failsafe:verify -Dit.test=DBR_09_AllocationSchemaIT | Exit 1; 5 test, 0 failure, 5 error, 0 skip do thiếu TEST_DB_URL; chưa có bằng chứng RED chức năng. |
| mvnw.cmd spotless:apply | Exit 0, BUILD SUCCESS; định dạng 1 file test mới. |
| mvnw.cmd clean verify (nền) | Exit 1, BUILD FAILURE, 29.265 giây; Surefire XML: 167 test, 0 failure, 1 error, 0 skip; BackendApplicationTests.contextLoads lỗi thiếu TEST_DB_URL. Chưa tới Failsafe. |

Log ngoài repo: C:/Users/nguye/.codex/visualizations/2026/10/23/01a16883-8696-7971-82e2-a144c1d55517/003-p4-spotless.log và 003-p4-clean-verify.log. Lượt test schema trước clean verify được đọc từ output/Failsafe (5 lỗi); clean đã xóa báo cáo Failsafe cũ. Không lấy kết quả xanh Phase 3 thay cho kiểm Phase 4.

### Vấn đề chưa giải quyết và thứ tự tiếp theo

- Cần nạp ba biến TEST_DB_URL, TEST_DB_ADMIN_USER, TEST_DB_ADMIN_PASSWORD vào process kiểm thử; JDK 25 đã xác định được.
- Cần B công bố API/fake M01 trạng thái, giới tính, mức hiện hành; chưa có quyết định cho phép sửa phần B. Không gọi đây là điểm nghiệp vụ mới, không bịa dữ liệu để vượt phụ thuộc.
- Các việc Phase 3 chưa xong vẫn là T012 rồi hoàn thiện T015/T017; thao tác đổi phòng giường vẫn hoãn theo quyết định cũ về cổng tài sản.
- Trong phạm vi Phase 4, thứ tự tasks vẫn là T019, T020, T021, T022, T023, T024, T025, T026, T027, T028 (các task test và task độc lập có thể chuẩn bị theo phụ thuộc). T019/T021 cần M01; test schema của T020/T022 đang chuẩn bị. Có môi trường MySQL thì chạy test schema thấy RED đúng nguyên nhân trước khi cài migration T023; T025 dùng G-03, không thêm thư viện/retry nếu chưa được duyệt.
- HTTP Phase 4 đã được phép dịch, còn phải thực hiện/kiểm hợp đồng khi tới T027; chưa có endpoint/màn Phase 4. T028 chưa bắt đầu nên không chụp màn hình.
- Chưa nghiệm thu Phase 4, chưa đánh task [x], chưa đánh Xong bàn giao C. Không commit, push, merge, rebase, đổi nhánh hay tạo PR.

## 2026-10-23 — Tiếp tục Phase 4: hợp đồng đã dịch, chức năng còn chờ đầu vào

### Trình tự, yêu cầu và quyết định thực tế

1. Người dùng yêu cầu “Tiếp tục”. Đọc lại trạng thái Git, đuôi plan và lịch sử; giữ thay đổi lượt trước, không làm lại test đã có. Nhánh vẫn feature/003-manage-bedroom, không commit/push/đổi nhánh.
2. Kiểm ba biến TEST_DB_URL/TEST_DB_ADMIN_USER/TEST_DB_ADMIN_PASSWORD ở process, User và Machine: đều chưa có. Kiểm file .env gốc/backend và cấu hình local thường dùng: không có. Không mở rộng tìm bí mật, không đoán mật khẩu. Đã gửi câu hỏi cấu hình MySQL test; tại mốc này chưa nhận trả lời.
3. M01 vẫn chỉ có package-info.java, không có support/fake/m01. Quyết định chờ B trước đó vẫn được giữ; chưa có quyền mới để viết API/fake M01.
4. Triển khai phần dịch HTTP Phase 4 đã được duyệt ở lượt trước. Dịch 6 operation đang tồn tại, clone 5 schema tiếng Anh và tham chiếu lại component tiếng Anh Phase 3; giữ component tên logic cũ cho phase sau. Bộ kiểm bằng Node/yaml so dữ liệu trước/sau: 64 operation ngoài phần dịch và toàn bộ component cũ giữ nguyên, tổng vẫn 70 operation. Bộ dịch tạm gặp lỗi xử lý required dạng boolean, đã sửa để chỉ ánh xạ required khi là mảng; lần lỗi chưa ghi contract. Công cụ tạm nằm trong backend/target, được clean xóa, không thuộc thay đổi giao nộp.
5. Sáu operation đã dịch: GET /beds/summary; GET /bed-allocations/suitable-beds; POST /bed-allocations; GET /bed-allocations/{id}; POST /bed-allocations/{id}/cancel; GET /residents/{residentId}/location. Cờ x-implementation-phase: 4 chỉ thể hiện duyệt dịch. Chưa cài controller/service cho chúng.
6. Phát hiện T027 yêu cầu GET danh sách nhưng contract collection chỉ có POST. Hỏi riêng quyền thêm GET /bed-allocations, bộ lọc residentId/bedId/status tùy chọn và trả mảng trong phạm vi quyền người xem; chưa nhận trả lời, chưa tự thêm endpoint. Không suy rộng quyền dịch thành quyền thêm endpoint.
7. Đổi tên tương ứng 6 dòng pending trong ApiContractIT, giữ kiểm 000/002 và mọi endpoint pending còn lại. Cập nhật dòng chờ A review ở team-plan. Chỉ đổi chú thích generator, sau đó chạy npm run gen:api; generated client api003.js thêm sáu hàm, không sửa tay.
8. Bổ sung test DB từ 5 thành 9 ca: UNIQUE giường ACTIVE ngay cả khi hai khoảng không chồng, effective_end theo thứ tự actual/expected/vô hạn, view áp bản AMEND_CONTENT mới nhất kể cả JSON null và loại VOID. Review phát hiện fixture test UNIQUE theo người bị vướng overlap với một phân bổ khác cùng giường: tách thành test FUTURE và ACTIVE độc lập, tránh lỗi sai nguyên nhân. Chưa có chứng cứ các assertion chạy trên MySQL, không cài production trước RED hợp lệ.
9. Chạy kiểm ArchTest/ranh giới, frontend và clean verify nền. Sau tách fixture test, chạy lại spotless:apply clean verify nền để kiểm bản cuối. Kết quả chốt cuối mục này.

### File tạo/sửa/xóa trong lượt tiếp tục và mục đích

| Thao tác | File | Mục đích |
| --- | --- | --- |
| Sửa file mới lượt trước | backend/src/test/java/com/cambae/backend/m03/DBR_09_AllocationSchemaIT.java | 9 ca chuẩn bị cho T020/T022; tách fixture UNIQUE, thêm generated end và corrected view. |
| Sửa | specs/003-room-bed-allocation/contracts/api-003.yaml | Dịch riêng 6 operation Phase 4, clone BedSummary/SuitableBed/BedAllocation/ResidentLocation/AllocationStatus; không đổi phần tương lai. |
| Sửa | backend/src/test/java/com/cambae/backend/common/ApiContractIT.java | Đổi tên sáu endpoint pending theo bản dịch, không gỡ pending/không bỏ kiểm hợp đồng khác; cần A review. |
| Sửa | frontend/scripts/gen-api.mjs | Chú thích phản ánh việc duyệt từng phase; điều kiện sinh client giữ nguyên. |
| Sinh lại | frontend/src/api/api003.js | Thêm getBedsSummary, getBedAllocationsSuitableBeds, postBedAllocations, getBedAllocationsById, postBedAllocationsByIdCancel, getResidentsByResidentIdLocation từ contract. |
| Sửa mục 9 | specs/000-shared-foundation/glossary.md | Mapping path/schema/trường/enum Phase 4, ghi rõ chưa triển khai và GET collection chưa được duyệt. |
| Sửa | docs/team-plan.md | Dòng A/C review hợp đồng phản ánh 45 operation đã dịch, 25 operation tương lai; giữ phụ thuộc M01 đã ghi lượt trước. |
| Bổ sung | specs/003-room-bed-allocation/tasks.md | Ghi phần đã dịch và test chuẩn bị; giữ toàn bộ Phase 4 chưa đánh. |
| Bổ sung | specs/003-room-bed-allocation/plan.md | Thiết kế bản dịch, quyết định, phụ thuộc và kết quả kiểm thực. |
| Bổ sung | Working-history.md | Nối lịch sử lượt này, giữ các mục cũ. |

Không xóa file nguồn, không có Java production/migration/entity/controller mới. Generator chạy lại api000.js/api002.js/client.js/index.js nhưng không có thay đổi nội dung ở Git; chỉ api003.js thay đổi. Không thêm thư viện, không sửa UI, không chụp màn hình.

### Quan hệ file và luồng dữ liệu

- Contract api-003.yaml là nguồn; gen-api.mjs chỉ xuất operation có x-implementation-phase, tạo các hàm api003.js gọi request chung. Sáu hàm mới chưa được màn hình gọi; các endpoint chưa có implementation.
- ApiContractIT đối chiếu runtime OpenAPI và contract. Tên endpoint trong pending phải theo bản dịch dù controller chưa cài; không đánh đồng pending với thành công kiểm hợp đồng. Lượt này chưa chạy được ApiContractIT do thiếu cấu hình MySQL.
- Glossary mục 9 giữ mapping fields/enum đồng nhất cho việc cài backend sau này: BedAllocation dùng status (FUTURE/ACTIVE/CLOSED/CANCELLED), SuitableBed dùng state (BedStatus), các tham chiếu Location/Reason/CareLevelCode giữ như Phase 3.
- Test DB tiếp tục dùng StructureTestSupport/TestUsers và fake có sẵn. Test corrected view tạo correction ở fixture bằng SQL, đọc v_current_allocation để kiểm overlay/VOID; không sửa service đính chính dùng chung. Không có thay đổi luồng ghi nghiệp vụ runtime.

### Kiểm chứng đã quan sát

| Lệnh/kiểm | Kết quả |
| --- | --- |
| Đối chiếu dữ liệu YAML trước/sau dịch | 6 operation dịch, 64 operation khác giữ nguyên, toàn bộ component cũ giữ nguyên, tổng 70 operation. |
| Duyệt $ref bằng Node/yaml | 338 tham chiếu giải được; 39 operation Phase 3, 6 Phase 4, 25 chưa dịch. |
| npm run gen:api; node --check src/api/api003.js | Exit 0; sinh 3 file client theo generator, api003.js hợp lệ cú pháp. |
| mvnw.cmd spotless:apply test -Dtest=*ArchTest,ModuleBoundaryTest | BUILD SUCCESS, exit 0; 23 test, 0 failure/error/skip, 21.131 giây. |
| npm test -- --run | Exit 0, 105/105 test qua. |
| npm run lint | Exit 0. |
| npm run build | Exit 0; còn cảnh báo bundle khoảng 630 kB >500 kB, chưa mở phạm vi tối ưu bundle. |
| mvnw.cmd clean verify (nền sau dịch) | Exit 1, BUILD FAILURE; Surefire 167 test, 0 failure, 1 error, 0 skip, BackendApplicationTests.contextLoads thiếu TEST_DB_URL; chưa tới Failsafe. |

Log nằm ngoài repo tại C:/Users/nguye/AppData/Local/Temp/: 003-p4-cont-arch.log, 003-p4-cont-web-test.log, 003-p4-cont-web-lint.log, 003-p4-cont-web-build.log, 003-p4-cont-verify.log. Log kiểm bản cuối sau tách fixture: 003-p4-cont-final-verify.log (kết quả chốt bổ sung ngay dưới).

### Việc còn thiếu và thứ tự tiếp tục

- Nạp cấu hình MySQL test để thấy RED đúng nguyên nhân trước khi cài migration T023; JDK 25 có sẵn, không cần thêm thư viện.
- Bổ sung nguồn M01 do B sở hữu cho T019/T025/T026; giữ các SPI/fake M03 đang có, không tạo fake thay nguồn M01.
- Chờ quyết định riêng cho GET /bed-allocations; sáu operation khác đã được phép dịch và đã dịch xong về tài liệu, chưa nghiệm thu chức năng.
- Các việc Phase 3 T012/T015/T017 chưa hoàn tất vẫn giữ trạng thái cũ. Trong Phase 4, task đầu tiên chưa xong vẫn T019, tiếp T020/T021/T022 rồi T023–T028 theo phụ thuộc; test DB là phần chuẩn bị độc lập, không bỏ qua thứ tự nghiệm thu.
- Giữ toàn bộ T019–T028 chưa đánh [x], chưa đánh Xong bàn giao BedAllocationApi/demo cho A. Không báo Phase 4 hoàn tất.

### Kết quả chốt lượt tiếp tục Phase 4

- `mvnw.cmd spotless:apply clean verify` nền trên bản cuối sau tách fixture: exit 1, BUILD FAILURE, 31.492 giây. Spotless chạy qua; Surefire XML tổng 167 test, 0 failure, 1 error, 0 skip. Lỗi duy nhất là BackendApplicationTests.contextLoads do thiếu TEST_DB_URL. Chưa tới Failsafe; 9 ca DB mới đã biên dịch nhưng chưa kiểm được assertion, chưa cài migration/service.
- `git diff --check` exit 0; có cảnh báo chuyển LF/CRLF, không có lỗi khoảng trắng. Danh sách Git đúng 10 file tạo/sửa mô tả ở trên; không có diff ngoài các file đó. Working-history.md vẫn là file chưa theo dõi có từ đầu phiên.
- Giữ nguyên kết quả 23 ArchTest/ranh giới qua và 105 test frontend/lint/build qua ở lượt này; không diễn giải chúng là clean verify xanh. Phase 4 chưa hoàn tất, không đánh task [x]. Đầu vào còn chờ: ba biến TEST_DB_*, API/fake M01 và quyết định GET collection.

## 2026-10-24 — Có môi trường MySQL, triển khai nền dữ liệu và API đọc Phase 4

### Yêu cầu mới và phạm vi giữ nguyên

- Chủ dự án đã cung cấp cấu hình MySQL schema eldercare_test và duyệt lựa chọn 1 cho GET /bed-allocations (residentId/bedId/status tùy chọn, trả mảng trong phạm vi quyền). Chỉ đưa ba biến TEST_DB_* vào process chạy Maven, không thực hiện lưu User/Machine, không ghi mật khẩu vào repo/lịch sử/log.
- Tiếp tục toàn bộ phần độc lập của Phase 4. M01 vẫn thiếu API/fake trạng thái/giới tính/mức hiện hành; chưa có quyền bổ sung phần của B. Giữ T012/T015/T017 của Phase 3 chưa đủ chức năng theo ghi chú cũ. Không đổi nhánh feature/003-manage-bedroom.
- Người dùng hỏi “xong phase 4 chưa”; đã báo chưa hoàn tất, phân biệt phần nền đã kiểm với checker/lệnh/HTTP/web còn thiếu, tiếp tục xử lý review và kiểm cuối.

### Trình tự test trước và cài đặt thực tế

1. Nạp cấu hình test vào process, chạy DBR_09_AllocationSchemaIT: 9 ca, 1 failure thiếu bảng/view và 8 error ghi vào bảng chưa tạo/cấp quyền. Khác lượt trước bị thiếu môi trường, lần này context/MySQL chạy được và đã có assertion RED đúng chức năng thiếu.
2. Tạo migration V003_03__bed_allocation.sql. Schema gồm bed_allocation đủ trường data-model mục 4, bốn cột sinh và ba UNIQUE, năm CHECK, ba chỉ mục truy vấn; FK tới bed/bed_transfer_reason/tự trỏ previous_allocation và bed.last_closed_allocation_id. Không thêm FK M01. Thêm resident_position, allocation_reminder, trigger chống chồng INSERT/UPDATE, chặn sửa terminal và xóa ngoài retention; cấp quyền riêng app/retention. Chạy 9 IT cùng 23 ArchTest xanh.
3. View v_current_allocation nhận bản đính chính mới nhất từ v_latest_correction, loại VOID qua v_voided_record, join cơ cấu theo bedId đã đính chính. JSON null khác với trường không có; không dùng COALESCE đơn thuần để ghi đè endAt. Thời điểm có +07:00 được chuyển về UTC của phiên MySQL. Thêm test view giữ đủ cột sinh: 1 failure trước sửa; sau sửa 10 IT xanh. Bổ sung kiểm trigger bằng admin, quyền retention, cột sinh không gán trực tiếp, bản CANCELLED không khóa khoảng, offset đính chính; bộ schema cuối 12 IT xanh cùng 23 ArchTest.
4. Viết FR_017_AllocationEntitiesIT trước: 1 failure thiếu ba entity trong JPA metamodel. Cài BedAllocation, ResidentPosition, AllocationReminder và enum trạng thái/nguồn mức. Test mapping đọc fixture SQL qua JPQL, kiểm enum, version, khóa nhắc tổng hợp. ArchTest đầu phát hiện AllocationReminder thiếu @DataGroup; bổ sung BUSINESS cho metadata vòng đời của entity kỹ thuật theo dấu, không thêm setter. Kiểm lại 1 IT và 23 ArchTest xanh.
5. Viết FR_017_AllocationQueriesIT trước: 4 failure do chưa có bản thật ResidentLocationApi. Cài AllocationQueries, AllocationStore và projection AllocationLocation. Bản thật đồng thời cài BedAllocationApi/ResidentLocationApi. Dùng view đã đính chính, vị trí theo [start_at,end_at), FUTURE không cấp phạm vi đang ở, kiểm người/giường trùng và ném InvalidPermissionDataException với source M03.BED_ALLOCATION. Test current/batch, scope theo tầng/phòng/người, mốc đầu/cuối khoảng, FUTURE, dấu chưa có giường và ảnh đính chính mâu thuẫn. Kiểm 4 IT cùng 23 ArchTest xanh.
6. Viết FR_020_AllocationLocksIT trước: 2 failure chưa có bộ khóa. Cài AllocationLocks dùng MANDATORY, AllocationStore khóa dòng người rồi giường theo id tăng dần; lỗi giường không tồn tại rollback dòng kỹ thuật. Test 10 transaction trên một giường: chỉ 1 INSERT được commit, 9 transaction còn lại bị trigger overlap chặn. Đây là kiểm khóa thật và SQL, chưa chứng minh endpoint/checker phân bổ đầy đủ.
7. Khởi chạy clean verify nền toàn backend với cấu hình MySQL. Kết quả trước review fix: BUILD SUCCESS, 02:00; 167 test unit/kiến trúc và 469 IT, 0 failure/error/skip. Frontend sau khi sinh GET mới: 105 test qua, lint/build exit 0, cảnh báo bundle >500 kB vẫn còn.
8. Skill requesting-code-review yêu cầu lượt rà độc lập. Reviewer chỉ đọc, không sửa mã, ghi nhận rủi ro INSERT IGNORE lấy khóa chia sẻ rồi nâng sang độc quyền. Đối chiếu research G-03 (đòi INSERT ON DUPLICATE KEY UPDATE), thêm test 10 transaction cùng resident_position đã có: thực tế RED với CannotAcquireLockException, MySQL báo deadlock tại SELECT FOR UPDATE. Sửa upsert theo G-03, không thay version/dấu của dòng đã có. Kiểm task 3 IT cùng 23 ArchTest xanh. Không thêm thư viện/retry giả trong transaction đã lỗi; retry toàn transaction còn thuộc phần lệnh chưa cài của T025/T026.
9. Chạy lại clean verify nền trên bản đã sửa deadlock. Chờ kết quả này mới đánh các task nền đáp ứng đủ; kết quả chốt bổ sung cuối mục. Không dùng lượt xanh trước fix để khẳng định bản cuối đã qua.

### File được tạo/sửa và mục đích

Đường dẫn dưới đây tính từ gốc project. Nội dung các mục lịch sử cũ được giữ nguyên.

| Thao tác | File | Mục đích |
| --- | --- | --- |
| Tạo | backend/src/main/resources/db/migration/V003_03__bed_allocation.sql | Bảng phân bổ/khóa người/nhắc, view đính chính, CHECK/UNIQUE/FK/index/trigger và quyền DB. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/internal/allocation/domain/BedAllocation.java | Entity nhóm BUSINESS, mapping thuộc tính phân bổ, không có setter đổi trạng thái. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/internal/allocation/domain/ResidentPosition.java | Entity khóa theo người, dấu withoutBedSince và version. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/internal/allocation/domain/AllocationReminder.java | Entity mốc nhắc kỹ thuật và EmbeddedId Key gồm targetType/targetId/flagType. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/internal/allocation/domain/AllocationStatus.java | Enum FUTURE/ACTIVE/CLOSED/CANCELLED nội bộ. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/internal/allocation/domain/CareLevelSource.java | Enum CURRENT/CONTRACT/REGISTRATION theo Q-301. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/internal/allocation/domain/AllocationLocation.java | Projection vị trí sau đính chính, ánh xạ sang BedAllocationView. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/internal/allocation/persistence/AllocationStore.java | SQL đọc view, dấu chưa có giường và khóa dòng M03; upsert tránh nâng khóa S sang X. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/internal/allocation/service/AllocationQueries.java | Bản cài API đọc M03/002, batch/scope/time và kiểm dữ liệu nguồn mâu thuẫn. |
| Tạo | backend/src/main/java/com/cambae/backend/m03/internal/allocation/service/AllocationLocks.java | MANDATORY, khóa người rồi giường distinct/sort tăng dần. |
| Sửa file mới lượt trước | backend/src/test/java/com/cambae/backend/m03/DBR_09_AllocationSchemaIT.java | 12 ca SQL thật cho T022/T023, gồm correction/VOID, admin/retention, cột sinh, khoảng thời gian. |
| Tạo | backend/src/test/java/com/cambae/backend/m03/FR_017_AllocationEntitiesIT.java | Test mapping entity/enum/version/khóa tổng hợp T024. |
| Tạo | backend/src/test/java/com/cambae/backend/m03/FR_017_AllocationQueriesIT.java | 4 ca cho API đọc T026; @Transactional rollback fixture mâu thuẫn để không ảnh hưởng test khác. |
| Tạo | backend/src/test/java/com/cambae/backend/m03/FR_020_AllocationLocksIT.java | 3 ca transaction/10 người tranh giường/10 lệnh cùng một người, regression deadlock. |
| Sửa | specs/003-room-bed-allocation/contracts/api-003.yaml | Bổ sung GET danh sách đã được duyệt; 7 operation Phase 4 vẫn chưa triển khai HTTP. |
| Sửa | backend/src/test/java/com/cambae/backend/common/ApiContractIT.java | Thêm GET collection vào pending và cập nhật chú thích; không bỏ kiểm hợp đồng 000/002. |
| Sinh lại | frontend/src/api/api003.js | Thêm getBedAllocations bằng npm run gen:api, không sửa tay. |
| Sửa mục 9 | specs/000-shared-foundation/glossary.md | Mapping lớp/bảng/cột/enum/API đọc/khóa; cập nhật phép GET và tổng operation hiện hành. |
| Sửa | docs/team-plan.md | Cập nhật dòng review A/C thành 7 operation Phase 4; trạng thái BedAllocationApi cập nhật sau kiểm cuối. |
| Bổ sung | specs/003-room-bed-allocation/plan.md | Thiết kế/thứ tự, lệch kỹ thuật và quyết định khóa, bằng chứng RED/GREEN, giới hạn M01. |
| Bổ sung | specs/003-room-bed-allocation/tasks.md | Tiến độ phần nền sau kiểm cuối; giữ phần checker/lệnh/HTTP/web chưa hoàn tất. |
| Bổ sung | Working-history.md | Lịch sử thực tế và kiểm chứng, không lưu thông tin đăng nhập. |

Không xóa file nguồn. frontend/scripts/gen-api.mjs giữ thay đổi chú thích từ lượt trước, không thay logic mới trong lượt này. Không sửa interface/fake M01/M15 hay file của module khác ngoài đăng ký test dùng chung đã ghi cần A review. Không thêm thư viện, không đổi giao diện/chụp màn hình/demo.

### Quan hệ file và luồng dữ liệu

- Migration tạo nguồn dữ liệu thật; entity mapping thuộc tính tương ứng cho JPA. SQL thuộc AllocationStore, service không chứa câu SQL; record trả từ persistence thuộc domain theo README.
- AllocationQueries gọi AllocationStore đọc v_current_allocation. View lấy bed/start/end sau đính chính, lọc bản VOID, rồi join bed → room → floor → area. Service kiểm một người/giường tại thời điểm không xuất hiện hai lần, mới trả LocationView/BedAllocationView. Thiếu phân bổ trả Optional.empty/tập rỗng; ảnh mâu thuẫn trả lỗi nguồn, không chọn ngẫu nhiên một dòng.
- current/current batch dùng BusinessTime.now; at dùng mốc được truyền, floorOn dùng đầu ngày BusinessTime; currentOrFutureAllocation ưu tiên ACTIVE rồi FUTURE. expected_end là dự kiến dùng chống chồng, không tự đóng phân bổ thật theo đồng hồ.
- Khi lệnh tương lai được cài, nó phải mở transaction, gọi AllocationLocks: upsert/khóa resident_position rồi khóa bed theo id tăng dần, sau đó mới kiểm điều kiện và ghi. Nếu do 001 gọi, 001 đã khóa dòng người M01 trước; không thêm lời gọi ghi ngược sang M01.
- Test concurrency gọi bộ khóa thật trong TransactionTemplate rồi INSERT fixture trực tiếp để kiểm tuần tự/constraint. Nó chưa đi qua lệnh phân bổ, PermissionApi, AuditLogApi hay điều kiện M01; không báo đã nghiệm thu T020/T025/T026 đầy đủ.
- Contract GET collection là nguồn sinh getBedAllocations; ApiContractIT còn pending vì chưa có controller. UI chưa gọi các endpoint Phase 4.

### Bằng chứng test/build

Các lệnh Maven từ backend/, JDK 25 tại E:/JAVA/JDK_GE/INS_25; biến DB chỉ nạp vào process. Sau mỗi phần chỉ kiểm test task cùng *ArchTest/ModuleBoundaryTest; cuối phần độc lập chạy clean verify nền.

| Phần/lệnh | RED đã quan sát | GREEN đã quan sát |
| --- | --- | --- |
| DBR_09_AllocationSchemaIT trước migration | 9 ca: 1 failure thiếu bảng/view, 8 error chưa có bảng/quyền | 9 IT +23 ArchTest qua sau V003_03. |
| FR_017_AllocationEntitiesIT | 1 failure thiếu entity trong metamodel | 1 IT +23 ArchTest qua sau mapping và annotation. |
| FR_017_AllocationQueriesIT | 4 assertion failure thiếu bản thật | 4 IT +23 ArchTest qua. |
| FR_020_AllocationLocksIT ban đầu | 2 assertion failure thiếu bộ khóa | 2 IT +23 ArchTest qua. |
| View đủ cột sinh | 1 assertion failure thiếu 4 key | 10 IT schema +23 ArchTest qua sau sửa. |
| Schema sau review bổ sung | Kiểm bổ sung trên behavior đã cài, không tuyên bố RED mới cho behavior có sẵn | 12 IT +23 ArchTest qua, gồm offset/admin/retention. |
| Regression same-resident deadlock | 1 error do CannotAcquireLockException/MySQL deadlock, đúng lỗi cần sửa | 3 IT khóa +23 ArchTest qua sau upsert. |
| clean verify nền trước fix deadlock | — | BUILD SUCCESS, 02:00; 167 unit/kiến trúc +469 IT, 0 failure/error/skip. Chưa phải kiểm bản cuối. |
| npm test -- --run; npm run lint; npm run build | — | 105/105 test, lint/build exit 0; cảnh báo bundle khoảng 630 kB >500 kB. |
| Node/yaml duyệt tham chiếu | — | 341 $ref giải được; operation 39 Phase 3 +7 Phase 4 +25 chưa dịch =71. |

Log ngoài repo tại C:/Users/nguye/AppData/Local/Temp/: 003-p4-db-{red,green}.log; 003-p4-entities-{red,green,green2}.log; 003-p4-queries-{red,green}.log; 003-p4-view-{red,green}.log; 003-p4-schema-review.log; 003-p4-locks-{red,green}.log; 003-p4-lock-upgrade-{red,green}.log; 003-p4-full-verify.log; 003-p4-approved-web-{test,lint,build}.log; bản cuối 003-p4-final-clean-verify.log.

### Giới hạn và việc tiếp theo theo tasks

- Chưa hoàn tất Phase 4. T019/T021 và checker của T025 cần API/fake M01 do B sở hữu; không tạo nguồn thay thế hoặc đọc bảng M01.
- T020 mới kiểm constraint/bộ khóa, chưa kiểm 10 request qua lệnh. T025 chưa có kiểm đầy đủ FR-018 và retry toàn transaction. T026 mới có API đọc, chưa có lệnh tạo/hủy/audit. T027 mới duyệt/dịch contract, chưa controller; T028 chưa UI/test hành vi. Không có job tự tạo/bắt đầu phân bổ.
- Sau kiểm cuối mới đánh task nền đủ yêu cầu (T022/T023/T024); T019 là task đầu tiên chưa xong của Phase 4, tiếp T020/T021 và hoàn thiện T025/T026 rồi T027/T028 theo phụ thuộc. Các task Phase 3 T012/T015/T017 giữ giới hạn chờ cũ.
- Không commit/push/merge/rebase/đổi nhánh, không tạo PR. Mã tại checkout cần PR để các thành viên khác nhận được.

### Kết quả chốt sau sửa deadlock (2026-10-24)

- `mvnw.cmd clean verify` nền bản cuối: **BUILD SUCCESS**, process exit 0, tổng **02:25**. Đối chiếu XML: **167 unit/kiến trúc +470 IT**, cả hai nhóm **0 failure/error/skip**. Spotless check qua trên 774 file Java. Log: C:/Users/nguye/AppData/Local/Temp/003-p4-final-clean-verify.log.
- Chỉ sau kết quả này đánh [x] T022/T023/T024. T019/T020/T021/T025/T026/T027/T028 giữ chưa hoàn tất; Phase 4 chưa xong. Test M03 mới trong lượt này: schema 12, entity 1, đọc API 4, khóa 3 =20 IT (từ 450 lên 470). Không báo test 10 request phân bổ đã qua; mới kiểm 10 transaction dùng khóa thật + SQL.
- Dòng BedAllocationApi trong team-plan cập nhật “Đã cài tại checkout, chờ PR/merge”, không bịa PR. API bản thật dùng được tại checkout, nhưng chưa có lệnh tạo phân bổ và M01 vẫn thiếu nguồn kiểm.
- Web sau sinh client GET danh sách: 105 test qua, lint/build exit 0; cảnh báo bundle >500 kB vẫn còn. Không sửa giao diện/chụp màn hình. Không commit/push/đổi nhánh.
- Thứ tự còn lại: T019 cần API/fake M01 (trạng thái/giới tính/mức hiện hành) do B sở hữu, tiếp hoàn thiện T020/T021; T025 checker/retry transaction; T026 lệnh tạo/hủy/audit; T027 controller; T028 web. Các giới hạn Phase 3 T012/T015/T017 giữ nguyên. Thông tin đăng nhập không có trong các file thay đổi.

## 2026-10-24 — Feature 003, khởi động Phase 5 (phần độc lập, chưa hoàn tất)

### Yêu cầu, trạng thái đầu phiên và quyết định được duyệt

- Người dùng yêu cầu Phase 5 theo AGENTS.md/team-plan.md, test trước, kiểm task cùng ArchTest, cuối phase clean verify nền, lịch sử tiếng Việt đầy đủ và báo cáo ngắn. Giữ nội dung lịch sử cũ, chỉ nối mục mới.
- HEAD đầu phiên dc1754c (003 phase 4), nhánh feature/003-manage-bedroom, chỉ Working-history.md chưa được Git theo dõi. Mã Phase 4 đã có trong Git do thay đổi ngoài lượt này; phiên này không tự commit hay đổi nhánh. Đọc lại tasks.md, plan/Ghi chú triển khai và đuôi lịch sử trước khi tra data-model/research/contracts cần thiết.
- Đã báo các dòng giao C ở mục 8: BedAllocationApi tại checkout chờ PR/merge; cổng tài sản tạm hoãn; review contract/generator với A; OfflineHandler với D; SPI chuyến 014; demo 003; các dòng Mọi người về TestUsers/package/quyền/thông báo.
- Chủ dự án duyệt lựa chọn 1 để dịch riêng HTTP Phase 5, giữ nghiệp vụ và phần phase sau. Với API/fake/sự kiện M01 còn thiếu, chủ dự án chọn lựa chọn 1: giữ chờ B, chỉ làm phần độc lập. Không mở rộng quyền sửa M01/M02 từ yêu cầu làm Phase 5.
- Phase 4 còn thiếu bộ kiểm FR-018 và lệnh phân bổ; M01 chỉ có package-info.java. Phase 5 T032/T033/T034 handler/T035 không được lắp bằng nguồn giả thay M01 hoặc handler bỏ kiểm. Chỉ triển khai migration độc lập của T034 và bản dịch/client T036.

### Trình tự thực hiện và RED/GREEN thực tế

1. Đọc tasks Phase 5 T029–T036, data-model mục 7/10, research G-07 và hợp đồng chuyển giường. Đối chiếu RequestTypeHandler của 000, M03GovernanceStore.delete và schema approval_request. Không đọc/sửa tài liệu nghiệp vụ khi chưa phát sinh điểm nghiệp vụ mới cần chốt.
2. Viết FR_027_BackfillSchemaIT trước migration, 5 ca. Chạy trên MySQL schema test bằng JDK 25/cấu hình DB đã được người dùng cung cấp từ lượt trước, chỉ nạp trong process. RED: 2 failure/3 error vì thiếu bảng/quyền ghi bed_backfill_request; context/MySQL chạy được, không lấy lỗi thiếu môi trường làm RED.
3. Tạo V003_04__bed_backfill_request.sql. PK approval_request_id; các trường resident_id/current_allocation_id/new_bed_id/transfer_at; FK tới bảng approval_request của 000 đã có và hai bảng M03, không FK M01. Index tra theo người/giường và thời điểm. Trigger UPDATE bất biến, DELETE chỉ tài khoản retention, app chỉ INSERT. Không tạo yêu cầu phê duyệt hoặc bản ghi nghiệp vụ tự động.
4. Chạy spotless:apply và test task cùng *ArchTest/ModuleBoundaryTest. GREEN: 23 test kiến trúc +5 IT, 0 failure/error/skip, BUILD SUCCESS, 01:38. Test gồm giữ đủ dữ liệu chỉ mục mà không đọc JSON, PK/FK, admin không vượt UPDATE/DELETE trigger, retention xóa được, rollback khi transaction nguồn lỗi và nhánh retention hiện có xóa chỉ mục trước phân bổ CLOSED.
5. Dịch đúng một operation HTTP đã được duyệt: POST /bed-allocations/{id}/transfer. Clone BedTransfer requestBody, BedTransferResult và BackfillRequestCreated; dùng các schema tiếng Anh Phase 3/4 có sẵn. Giữ nguyên component tiếng Việt phục vụ operation tương lai. Node/yaml so dữ liệu xác nhận 70 operation còn lại và mọi component cũ giữ nguyên, tổng operation không tăng.
6. Đổi tên tương ứng operation trong pending của ApiContractIT, giữ nguyên kiểm 000/002 và không gỡ pending. Chạy npm run gen:api để tạo postBedAllocationsByIdTransfer; node --check client qua. Không sửa file sinh bằng tay. Chuẩn hóa khoảng trắng bản dịch sau git diff --check phát hiện dòng trống còn indent; không thay logic.
7. Cập nhật glossary mục 9, team-plan dòng chờ B và review A/C, ghi rõ task còn thiếu. Không viết controller giả hoặc màn chuyển giường gọi backend chưa đủ điều kiện. Không sửa giao diện, không chụp màn hình, không viết mobile chức năng.
8. Chạy npm test -- --run, lint, build; cả ba process exit 0, build còn cảnh báo bundle >500 kB như trước. Khởi chạy clean verify nền toàn backend cho phần độc lập; kết quả chốt bổ sung cuối mục sau khi process kết thúc. Không đánh task [x] khi chỉ một phần task hoàn tất.

### File tạo/sửa/xóa và mục đích

Đường dẫn tính từ gốc project. Các file không có trong bảng dưới không được chủ động sửa trong lượt này.

| Thao tác | File | Mục đích |
| --- | --- | --- |
| Tạo | backend/src/main/resources/db/migration/V003_04__bed_backfill_request.sql | Bảng kỹ thuật chỉ mục nhập bù, PK/FK/index, trigger bất biến và quyền INSERT/retention DELETE. |
| Tạo | backend/src/test/java/com/cambae/backend/m03/FR_027_BackfillSchemaIT.java | 5 IT MySQL viết trước schema: dữ liệu, PK/FK, trigger admin/retention, rollback và dọn cùng phân bổ. |
| Sửa | specs/003-room-bed-allocation/contracts/api-003.yaml | Dịch một operation chuyển giường và clone request/response theo duyệt; giữ phần phase sau. |
| Sửa | backend/src/test/java/com/cambae/backend/common/ApiContractIT.java | Đổi tên operation pending theo bản dịch Phase 5, không giảm phạm vi kiểm; cần A review đăng ký dùng chung. |
| Sinh lại | frontend/src/api/api003.js | Thêm postBedAllocationsByIdTransfer từ generator. |
| Sửa mục 9 | specs/000-shared-foundation/glossary.md | Mapping path, schema, field, lỗi mô tả, bảng và tên loại yêu cầu trong fixture. |
| Sửa | docs/team-plan.md | Mở rộng phụ thuộc FR-018 của Phase 5; thêm khoảng vắng/sự kiện M01/M02 cần B; cập nhật review A/C. |
| Bổ sung | specs/003-room-bed-allocation/tasks.md | Tiến độ từng phần T034/T036, giữ T029–T036 chưa đánh. |
| Bổ sung | specs/003-room-bed-allocation/plan.md | Quyết định, cách triển khai schema/bản dịch và bằng chứng kiểm/giới hạn. |
| Bổ sung | Working-history.md | Nối lịch sử thực tế, giữ mọi nội dung cũ. |

Không xóa file nguồn. Script dịch tạm trong backend/target được clean xóa cùng output build, không thuộc mã giao nộp. Generator chạy lại các client khác nhưng Git không có diff nội dung ở chúng. Không sửa frontend/scripts/gen-api.mjs, không thêm thư viện, không sửa API/fake module của B, không có Java production mới.

### Quan hệ file và luồng dữ liệu

- Khi handler T034 được cài sau này, nó sẽ lập yêu cầu qua API 000 và INSERT bed_backfill_request trong cùng transaction; bảng giữ resident_id, current_allocation_id, new_bed_id, transfer_at để tra dấu CHK051 mà không đọc JSON của module 000. Phiên này mới có bảng và test, chưa có handler hoặc đường ghi runtime.
- FK kiểm yêu cầu/phân bổ/giường tồn tại; không thể ghi đè chỉ mục sau khi lập. Bảng kỹ thuật được giữ cùng chuỗi phân bổ, trigger cho retention xóa. Nhánh M03GovernanceStore.delete đã có từ Phase 2 nhận ra bảng và xóa chỉ mục trước bed_allocation; test mới gọi nhánh thật này qua RetentionDatabase, không sửa code retention chỉ để test xanh.
- Request chuyển giường tiếng Anh gồm newBedId/transferReasonId/reason/transferAt; response 200 là BedTransferResult (previousAllocation/newAllocation/warnings), 202 là BackfillRequestCreated (approvalRequestId/status). Chưa có endpoint runtime: pending trong ApiContractIT được giữ.
- Contract → generator → api003.js: hàm postBedAllocationsByIdTransfer dùng client chung. Chưa có màn hình gọi nó; không tuyên bố chuyển giường end-to-end đã chạy.
- Các phép kiểm M01, chuyển giường atomic, chuyển theo lịch, nhập bù có phê duyệt, listener đánh giá phù hợp và UI thuộc phần còn lại. Không tự tạo bản giả nguồn M01 hoặc suy quyền từ role ở frontend.

### Lệnh kiểm và kết quả đã quan sát

Chạy Maven từ backend/, JDK 25 E:/JAVA/JDK_GE/INS_25. Thông tin đăng nhập chỉ đặt cho process, không in/lưu trong repo.

| Lệnh/kiểm | Kết quả thực tế |
| --- | --- |
| mvnw.cmd test-compile failsafe:integration-test failsafe:verify -Dit.test=FR_027_BackfillSchemaIT (trước migration) | 5 ca: 2 failure/3 error; thiếu bảng/quyền; exit 1. |
| mvnw.cmd spotless:apply test -Dtest=*ArchTest,ModuleBoundaryTest failsafe:integration-test failsafe:verify -Dit.test=FR_027_BackfillSchemaIT | BUILD SUCCESS, exit 0, 01:38; 23 unit/kiến trúc +5 IT, 0 failure/error/skip. |
| Đối chiếu Node/yaml bản dịch | 1 operation dịch; 70 operation khác và component cũ không đổi; tổng 71. |
| Duyệt tham chiếu YAML | 345 $ref giải được; 39 Phase 3 +7 Phase 4 +1 Phase 5 +24 chưa dịch. |
| npm run gen:api; node --check src/api/api003.js | Exit 0, client mới sinh hợp lệ cú pháp. |
| npm test -- --run | Process exit 0; số test xác nhận trong phần chốt. |
| npm run lint | Exit 0. |
| npm run build | Exit 0; còn cảnh báo bundle >500 kB, chưa mở rộng phạm vi tối ưu. |
| mvnw.cmd clean verify (nền cuối lượt) | Đã khởi chạy; kết quả chốt bổ sung sau khi đọc output/XML. |

Log ngoài repo tại C:/Users/nguye/AppData/Local/Temp/: 003-p5-schema-red.log, 003-p5-schema-green.log, 003-p5-clean-verify.log, 003-p5-web-test.log, 003-p5-web-lint.log, 003-p5-web-build.log.

### Vấn đề còn tồn tại và task tiếp theo theo đúng thứ tự

- Theo quyết định mới của chủ dự án: tiếp tục chờ B cung cấp API/fake trạng thái/giới tính/mức chăm sóc, khoảng vắng và các sự kiện M01/M02. Đã ghi rõ phụ thuộc trong team-plan, không sửa nguồn ngoài phần C.
- Phase 4 còn T019/T020/T021 và checker/lệnh T025/T026, HTTP/web T027/T028; đây là phụ thuộc của chuyển giường đầy đủ. Không tự tuyên bố Phase 4 xong khi bắt đầu Phase 5.
- Phase 5 theo tasks: T029 (atomic/rollback/concurrency/event/giữ giường người vắng), T030 (chuyển theo lịch), T031 (khác giá/nhập bù/dấu phù hợp), T032 (service chuyển), T033 (chuyển theo lịch), T034 (hoàn thiện handler nhập bù sau phần schema đã cài), T035 (listener), T036 (controller/web; mobile chỉ khi được mở).
- Giữ toàn bộ T029–T036 chưa đánh [x]. Migration xanh chưa đủ T034; contract/client chưa đủ T036. Không có task Phase 5 nào đủ nghiệm thu toàn phần trong lượt này.
- Không commit/push/merge/rebase/đổi nhánh hay tạo PR. Tài liệu và mã mới vẫn ở checkout để review.

### Phát hiện từ clean verify lần đầu và sửa fixture

- clean verify nền lần đầu kết thúc BUILD FAILURE, 05:06; 475 IT có 1 failure, 0 error/skip. Test lỗi là ApprovalLifecycleIT.US5_1_create_isDraft_withRequesterTypeContentDateReason: mã YC-2026-5000393 không khớp YC-\d{4}-\d{6}.
- Điều tra cho thấy helper approval() của FR_027_BackfillSchemaIT chèn approval_request.id bằng TestUsers.nextId() (dải lớn), làm tăng AUTO_INCREMENT rồi ảnh hưởng test 000 chạy sau. Đây là lỗi cách chuẩn bị fixture mới, không phải lỗi cần thay đổi định dạng mã nghiệp vụ.
- Sửa đúng helper trong file test của 003: bỏ id tự gán, dùng GeneratedKeyHolder và Statement.RETURN_GENERATED_KEYS lấy ID do MySQL cấp. Không sửa hoặc nới assertion của ApprovalLifecycleIT, không thay code production 000.
- Chạy lại test task, *ArchTest/ModuleBoundaryTest và ApprovalLifecycleIT trong cùng JVM để kiểm ảnh hưởng chuỗi ID; sau đó chạy lại clean verify nền. Kết quả cuối ghi tiếp, không bỏ qua lần kiểm thất bại.

- Sau sửa fixture: `mvnw.cmd spotless:apply test -Dtest=*ArchTest,ModuleBoundaryTest failsafe:integration-test failsafe:verify -Dit.test=FR_027_BackfillSchemaIT,ApprovalLifecycleIT -Dfailsafe.runOrder=alphabetical` **BUILD SUCCESS**, exit 0, **01:20**. Có **23 unit/kiến trúc +12 IT**, 0 failure/error/skip; log xác nhận FR_027_BackfillSchemaIT chạy trước ApprovalLifecycleIT, nên kiểm đúng ảnh hưởng thứ tự đã gây lỗi. Không sửa test 000.
- Frontend xác nhận **105/105 test** qua; lint/build exit 0. `git diff --check` qua; đối chiếu đường dẫn thay đổi với lịch sử thiếu **0** file. Đang chạy lại clean verify nền bản cuối, log `C:/Users/nguye/AppData/Local/Temp/003-p5-final-clean-verify.log`.

### Kết quả chốt phần độc lập Phase 5 (2026-10-24)

- `mvnw.cmd clean verify` nền bản cuối sau sửa fixture: **BUILD SUCCESS**, process exit 0, tổng **04:10**. Đối chiếu XML: **167 unit/kiến trúc +475 IT**, cả hai nhóm **0 failure/error/skip**. Spotless check qua trên **775 file Java**. Log cuối: C:/Users/nguye/AppData/Local/Temp/003-p5-final-clean-verify.log. Đây là kiểm phần code tại checkout, không phải nghiệm thu luồng chuyển giường chưa cài.
- Frontend: **105/105 test**, `npm run lint` và `npm run build` exit 0; bundle vẫn có cảnh báo >500 kB. Chỉ sinh client, không sửa giao diện/chụp màn hình.
- `git diff --check` qua; danh sách thay đổi khớp **10 file** đã mô tả, lịch sử thiếu **0 đường dẫn**. Không xóa file nguồn. Working-history.md đã tồn tại từ đầu và vẫn chưa được Git theo dõi; chỉ bổ sung nội dung.
- Giữ **T029–T036 chưa đánh [x]**: T034 mới có schema/test, chưa handler; T036 mới có contract/client, chưa controller/web/mobile. Chủ dự án đã chọn giữ chờ B cho nguồn M01; không thay bằng fake tự viết. Không đánh Xong dòng bàn giao nào của C trong Phase 5.
- Bước tiếp theo theo tasks: **T029**, sau khi có nguồn M01 và checker/lệnh nền Phase 4; tiếp T030/T031, rồi T032–T036 theo phụ thuộc. T034 cần handler kiểm lại FR-018/FR-019/khoảng vắng; T035 cần sự kiện M01/M02. Không commit/push/merge/rebase/đổi nhánh/tạo PR.

### 2026-10-24 — Xác nhận lại yêu cầu hoàn thiện và giới hạn chờ B

- Người dùng yêu cầu hoàn thiện nốt Phase 5. Kiểm lại checkout vẫn ở dc1754c, các thay đổi Phase 5 lượt trước còn tại checkout; M01 chỉ có package-info.java, chưa có support/fake/m01 hoặc checker FR-018 của Phase 4.
- Đã đọc frontend/README.md, cấu trúc màn beds và nguồn ngày giờ để rà phần T036; chưa sửa/tạo file giao diện. Chưa chạy thêm test vì chưa có thay đổi mã mới.
- Khi hỏi rõ có thay quyết định cũ để cho bổ sung hợp đồng/API/sự kiện/fake M01 tối thiểu hay không, người dùng chọn **2: Giữ chờ B, chưa nghiệm thu Phase 5**. Quyết định này giữ nguyên giới hạn sở hữu, không cho phép tự tạo nguồn M01 hoặc bỏ kiểm nghiệp vụ. Không hỏi lại cùng quyền sửa M01 khi chưa có chỉ đạo mới.
- Không bổ sung mã hoặc đánh task [x] trong lượt xác nhận này. T029–T036 vẫn chưa hoàn tất; các kết quả kiểm trước đó chỉ áp dụng phần độc lập đã ghi. Cần B công bố nguồn trạng thái/giới tính/mức, khoảng vắng và sự kiện, cùng hoàn thiện checker/lệnh nền, trước khi nghiệm thu luồng Phase 5.

### 2026-10-24 — Hoàn thiện phần UI web T036 sau nhắc “tưởng còn UI mà”

**Phạm vi và quyết định**

- Chủ dự án nhắc còn UI. Xác nhận điểm dừng ở lượt trước quá sớm: phần web có thể cài theo HTTP contract đã duyệt trong khi backend chờ nguồn M01. Tiếp tục UI; vẫn giữ quyết định không khai báo API/fake/sự kiện M01 thay B.
- Đọc frontend/README.md, DESIGN.md, tasks/plan, schema/request/response và thành phần hiện có; đọc mobile/README.md xác nhận mobile chưa mở chức năng, task mobile giữ chưa đánh. Không yêu cầu duyệt thêm API/thư viện; không sửa nghiệp vụ/spec, không commit/push/đổi nhánh.

**Trình tự triển khai có bằng chứng test trước**

1. Viết `BedTransfersPanel.test.jsx` trước; skeleton chỉ để import được. Lệnh `npx vitest run src/features/beds/BedTransfersPanel.test.jsx` RED **6/6** do chưa có luồng chuyển. Log: `C:/Users/nguye/AppData/Local/Temp/003-p5-ui-red.log`.
2. Cài panel/form dùng generated client: đọc phân bổ ACTIVE, mở Sheet, tải giường phù hợp và lý do còn hiệu lực, nhập lý do lệnh/thời điểm tùy chọn, gửi POST. Chạy lại **6/6 GREEN**.
3. Viết thêm test allocation-only ở `BedsPage.test.jsx`; RED **1 mới/5 cũ xanh** do trang chỉ cho quyền cấu hình. Log `003-p5-ui-page-red.log`. Sửa trang tải quyền trước, chỉ đọc cơ cấu khi có quyền cấu hình, gắn tab Chuyển giường theo BED_ALLOCATION.VIEW; hai file **12/12 GREEN**.
4. Bổ sung test lệnh đang gửi không được đóng hoặc gửi trùng: RED **1 mới/6 cũ xanh** (nút Đóng vẫn còn), log `003-p5-ui-busy-red.log`. Cài callback trạng thái gửi lên panel, ẩn Đóng, chặn onOpenChange khi đang gửi, Hủy bị vô hiệu và ref chặn gửi trùng; hai file **13/13 GREEN**.
5. Format đúng các file UI vừa sửa bằng Prettier, không format toàn dự án. Kiểm toàn frontend, lint/build và ArchTest; kết quả cuối ở dưới.
6. Dựng HTTP fixture riêng tại Temp để phục vụ production build. Lần đầu sandbox chặn socket localhost, trình duyệt timeout; dừng process đó rồi chạy preview có quyền mạng cục bộ. Không thay firewall/cấu hình bảo mật, không thêm fixture vào code production. Kiểm trực quan bằng trình duyệt sau khi kết nối được.

**File tạo/sửa trong lượt UI và vai trò**

| Đường dẫn | Thay đổi và mục đích |
| --- | --- |
| `frontend/src/features/beds/BedTransfersPanel.jsx` | Tạo: đọc danh sách ACTIVE, trạng thái tải/lỗi/rỗng, hiển thị mã hồ sơ/vị trí/ngày bắt đầu, mở Sheet, nhận kết quả 200/202 và tải lại dữ liệu. |
| `frontend/src/features/beds/BedTransferForm.jsx` | Tạo: đọc giường phù hợp/lý do, lựa chọn trống, lý do lệnh bắt buộc, thời điểm nhập bù tùy chọn, gửi đúng payload/header; hiển thị ProblemAlert và tải lại giường. |
| `frontend/src/features/beds/BedTransfersPanel.test.jsx` | Tạo: 7 ca FR_022/FR_028/FR_027/FR_018/FR_021 cho context, payload, cảnh báo, chờ duyệt, điều kiện xung đột, giờ Việt Nam, quyền chỉ xem và chống gửi trùng. |
| `frontend/src/features/beds/BedsPage.jsx` | Sửa: tab chuyển giường và tách quyền đọc phân bổ khỏi quyền đọc cấu hình; người chỉ có quyền xem phân bổ không gọi các endpoint cơ cấu. |
| `frontend/src/features/beds/BedsPage.test.jsx` | Sửa: thêm ca quyền allocation-only, giữ 5 ca cũ. |
| `specs/003-room-bed-allocation/plan.md` | Bổ sung lý do tiếp tục UI độc lập, luồng dữ liệu, quyền, thời gian, idempotency, kết quả test/QA và phạm vi còn thiếu. |
| `specs/003-room-bed-allocation/tasks.md` | Bổ sung trạng thái UI web T036 đã có; giữ checkbox vì endpoint/backend/mobile chưa hoàn tất. |
| `specs/000-shared-foundation/glossary.md` | Bổ sung tên BedTransfersPanel/BedTransferForm tại mục feature 003. |
| `Working-history.md` | Bổ sung lịch sử thực tế lượt này, không xóa phần ghi nhận trước đó. |

Không xóa file. Các thay đổi migration/contract/client/team-plan của lượt Phase 5 trước giữ nguyên; không có thay đổi Java hoặc schema mới trong lượt UI.

**Quan hệ giữa file, dữ liệu và quyết định kỹ thuật**

- `BedsPage` đọc `/me/permissions`: ROOM_BED_CONFIGURATION.VIEW cho các tab cơ cấu; BED_ALLOCATION.VIEW cho tab Chuyển giường, BED_ALLOCATION.CREATE cho lệnh. Backend tiếp tục quyết định phạm vi/điều kiện thực tế. Không suy quyền từ vai trò.
- `BedTransfersPanel` → generated `getBedAllocations(status=ACTIVE)` → mỗi bản ghi mở `BedTransferForm`. Contract chưa có tên người trong BedAllocation nên chỉ hiện Hồ sơ #residentId, không bịa tên/đọc chéo module.
- Form → `getBedAllocationsSuitableBeds(residentId,fromAllocationId,startAt?)` và `getBedTransferReasons()`. Chỉ hiển thị lý do ACTIVE; không chọn sẵn giường/lý do; đổi thời điểm hoặc tải lại xóa giường đã chọn. Cleanup effect bỏ kết quả cũ khi thời điểm đổi hoặc form đóng.
- Thời điểm từ datetime-local được biểu diễn theo giờ Việt Nam và gửi +07:00; khi để trống không gửi transferAt để server dùng BusinessTime. Client không tự đặt ngưỡng nhập bù/CFG hoặc quyết định tính phù hợp.
- POST `/bed-allocations/{id}/transfer` chỉ có newBedId,transferReasonId,reason và transferAt khi có. Gửi Idempotency-Key, không If-Match theo hợp đồng. Giữ cùng key cho payload không đổi khi lỗi mạng/5xx; đổi key sau lỗi 4xx hoặc đổi payload. Không cho đóng/gửi trùng trong khi request đang chạy.
- 200: thông báo “Đã chuyển giường.”, giữ đầy đủ warnings qua lần tải lại. 202: chỉ thông báo mã yêu cầu nhập bù chờ duyệt; không tự sửa vị trí. 409: ProblemAlert hiển thị mọi unmetConditions, form còn mở và có tải lại giường. Dữ liệu danh sách sau thao tác luôn đọc lại từ server.

**Lệnh và kết quả xác minh cuối**

- `npx vitest run src/features/beds/BedTransfersPanel.test.jsx src/features/beds/BedsPage.test.jsx`: **13/13 GREEN**, exit 0.
- `npm test`: **18 file, 113/113 test qua**, exit 0, 27.93 giây. Log `C:/Users/nguye/AppData/Local/Temp/003-p5-ui-final-tests.log`. Có thông báo jsdom chưa cài navigation to another Document từ test sẵn có; không gây test lỗi.
- `npm run lint`: exit **0**.
- `npm run build`: exit **0**, bundle **636.84 kB**; còn cảnh báo chunk >500 kB. Không thêm cấu hình/thư viện ngoài task để xử lý cảnh báo này. Log `003-p5-ui-build.log`.
- Backend `mvnw.cmd test -Dtest=*ArchTest,ModuleBoundaryTest` với JDK25: **BUILD SUCCESS**, **23 test, 0 failure/error/skip**, exit 0, 29.498 giây; log `003-p5-ui-arch.log`.
- `git diff --check`: qua. Lượt này chỉ đổi UI/tài liệu, không chạy lại toàn bộ clean verify backend; kết quả backend clean verify nền **167 unit/kiến trúc +475 IT** đã xác minh ở mục trước vẫn là bằng chứng cho backend trước lượt UI. Không lấy nó làm nghiệm thu end-to-end Phase 5 và không đánh task [x].

**Kiểm UI thực tế với fixture, không phải backend đã hoàn tất**

- Production build trên localhost với API fixture ngoài repo; chỉ dùng mã hồ sơ 11, không thêm thông tin cá nhân hoặc demo mới vào ứng dụng.
- Kiểm Sheet ở **1440×900**, **768×1024**, **390×844**: đo scrollWidth bằng clientWidth tại ba cỡ (383/383, 383/383, 292/292), không tràn ngang trong form; quan sát nhãn, nút, màu và bố cục. Khung AppShell dùng chung giữ nguyên.
- Chế độ tối hiển thị rõ; mở lệnh bằng Enter, đóng bằng Escape, Tab từ trường lý do sang Hủy. Chọn giường/lý do, gửi tới fixture nhận 202, quan sát đúng thông báo chờ duyệt #51 và vị trí cũ vẫn còn.
- Ảnh ở `C:/Users/nguye/AppData/Local/Temp/003-p5-ui-desktop.png`, `003-p5-ui-tablet.png`, `003-p5-ui-mobile.png`, `003-p5-ui-dark.png`; không đưa ảnh/test server vào repo. Khôi phục viewport và chế độ sáng sau kiểm.

**Còn tồn tại và thứ tự tiếp theo**

- UI web T036 đã cài, chưa nối chạy được luồng thật vì endpoint chuyển/suitable-beds/danh sách của Phase 4–5 vẫn chưa có controller đầy đủ. M01 vẫn thiếu nguồn trạng thái/giới tính/mức chăm sóc/khoảng vắng/sự kiện; giữ chờ B theo quyết định đã xác nhận, không hỏi lại quyền tự bổ sung.
- T036 chưa [x]; mobile giữ chưa mở theo mobile/README.md. T034 còn handler. Toàn Phase 5 chưa nghiệm thu. Task chưa hoàn tất đầu tiên vẫn **T029**, tiếp **T030 → T031 → T032 → T033 → T034 → T035 → T036** theo tasks và phụ thuộc nền Phase 4. Không lấy test UI fixture thay cho test tích hợp nghiệp vụ/backend.

### 2026-10-24 — Phase 6 Spec 003: phạm vi triển khai và lịch sử bước đầu

- Chủ dự án yêu cầu Phase 6 theo AGENTS/team-plan, test trước, kiểm task cùng ArchTest, clean verify nền trước checkbox; bổ sung lịch sử tiếng Việt. Baseline `473c812` (003 phase 5 wait M01), nhánh giữ nguyên, đầu lượt chỉ Working-history.md chưa theo dõi. Không commit/push/đổi nhánh.
- Đã đọc phân công mục 8 và báo các dòng C: BedAllocationApi, cổng vị trí tài sản 003/019 đang hoãn, review hợp đồng cùng A, OfflineHandler của các feature ghi nhận, SPI chuyến của 014, dữ liệu demo; các phụ thuộc B/D/E được nêu riêng. Đọc AGENTS, CLAUDE, constitution, backend README, tasks/ghi chú plan trước data-model/contracts/research. Chỉ tra nghiep-vu.md tại Q-246/Q-261 và Phụ lục 27 để xác minh quyền tạm ngừng/sử dụng lại.
- Dùng executing-plans, test-driven-development; khi kiểm lỗi dùng systematic-debugging; một reviewer độc lập theo requesting-code-review, không giao triển khai song song và không gửi tin cho thành viên khác.
- M01 vẫn chưa có hợp đồng/fake. Giữ quyết định chờ B đã được xác nhận ở lượt trước, không hỏi lại quyền mở rộng M01, không sửa M01. T039/T043, job quá bắt đầu dự kiến và nhánh T042 cần checker FR-018/lệnh chuyển theo lịch chưa có không được giả vờ hoàn tất.

**Trình tự thực hiện**

1. T037: dùng lại BedTransitions/BedStateService có từ T008; thêm `FR_011_BedTransitionsTest` với 30 chuyển hợp lệ và 154 cặp không hợp lệ theo bảng 17 dòng. Tổng 184 ca tham số hóa xanh cùng 23 ArchTest. Vì đây là kiểm mở rộng cho implementation đã có, không sửa mã đúng để tạo RED giả; không ghi nhận test này có RED trước implementation.
2. T041: hỏi đúng điểm đổi hợp đồng. Chủ dự án chọn 1, duyệt riêng POST /beds/{id}/suspend, POST /beds/{id}/reuse, GET /beds/{id}/state-history cùng request/response liên quan. Script dùng YAML so ngữ nghĩa xác nhận mọi operation/component cũ khác không đổi; clone BedStateChanged/BedStateHistoryRow/After, sinh lại client bằng npm run gen:api. Không sửa tay generated code, không dịch phần phase sau.
3. Viết `FR_011_BedCommandsIT` trước: RED đầu 6 ca có 3 failure/2 error/1 qua không đủ chặt, sau siết assertion theo loại lỗi RED 4 failure/2 error. Cài suspend/reuse tại BedService và kiểm phân bổ FUTURE trong BedStore. Sau cài một assertion lỗi vì chữ hoa/thường thông báo lý do; sửa test của mình sang ReasonRequiredException theo hợp đồng. Không sửa thông báo/test chung.
4. Viết `FR_016_BedHistoryHttpIT` trước: lần đầu 3 assertion failure/1 fixture error (thiếu basis_type/basis_id NOT NULL). Sửa fixture, chạy lại thấy 4 assertion RED do chưa có endpoint, rồi thêm controller/lịch sử. Cả test lệnh +HTTP +ApiContractIT **11 IT và 23 ArchTest xanh**, BUILD SUCCESS 41.188 giây.
5. T040: viết `FR_012_BedHoldsIT` trước, RED khi chưa có bedHoldService. Thêm BedHoldService/BedHoldStore với bốn tác động MANDATORY, rồi **5 IT và 23 ArchTest xanh**, 34.653 giây. Chưa nối facade BedAllocationCommandApi vì còn các thao tác theo lịch thuộc Phase 5 chưa có; không thêm stub cho những phương thức chưa triển khai.
6. T044: viết `FR_015_HoldExpiryIT` trước, RED 3 ca khi chưa có job. Thêm HoldExpiryJob/HoldExpiryProcessor. Sau cài 2 nhánh qua, ca callback lỗi vướng cách stub spy qua transaction proxy MANDATORY; sửa test dùng ultimate target. Lần kế tiếp teardown bị ClassCastException do generic varargs của Mockito.reset; thêm kiểu rõ ràng. Kiểm lại **3 IT và 23 ArchTest xanh**, BUILD SUCCESS 31.463 giây. Không bỏ kiểm rollback hoặc sửa fake nguồn.
7. Review độc lập phát hiện hai P2: cancelHold đọc snapshot cũ khi hủy đồng thời; extendExpectedEnd để lỗi SQL overlap thoát ra thay lỗi điều kiện. Đã thêm test tái hiện trước khi sửa; bằng chứng RED/GREEN và kiểm cuối được ghi ở mục chốt bên dưới sau khi chạy xong.

**Các quyết định kỹ thuật và luồng xử lý**

- Không tạo máy trạng thái thứ hai: BedService/BedHoldService/HoldExpiryProcessor đều gọi BedStateService, lớp này giữ duy nhất quyền ghi state, ghi bed_state_history và AuditLogApi cùng transaction.
- Suspend/reuse dùng ROOM_BED_CONFIGURATION.UPDATE theo Q-246/Phụ lục 27; Reason bắt buộc, khóa dòng giường, kiểm FUTURE từ v_current_allocation và dấu BedAssetPort. Sử dụng lại đi qua AWAITING_READY, hạn từ CFG-M03-04 và last_closed_allocation_id rỗng. Không tự thêm If-Match vào hợp đồng hai lệnh đã duyệt.
- BedController → BedQueries → BedStore đọc lịch sử theo bedId, cursor after là id tăng. BedHistoryEntry là ảnh nội bộ; BedStateHistoryRow là DTO web. Tên actor qua AccountApi.fullName, dấu corrected qua CorrectionApi.current; không đọc bảng M15/M01. Đây là lịch sử gốc có dấu đính chính; provider/lệnh đính chính ở T047.
- Giữ hồ sơ chờ: khóa resident_position rồi giường, đọc CFG-M02-02 cho hạn, kiểm chồng và dấu tài sản, chuyển HELD với WAITING_RECORD. Hủy giữ chỗ chỉ tác động giữ của hồ sơ đó, phát BedBecameEmpty khi không FUTURE và không dấu tài sản. Giải phóng người vắng đóng ACTIVE và chuyển AWAITING_READY, phát BedAllocationClosed; gia hạn chỉ đổi expected_end, ghi nhật ký. Các helper là tác động trong transaction nguồn, chưa thay facade API đầy đủ.
- HoldExpiryJob quét hạn, mỗi giường một transaction qua HoldExpiryProcessor; SchedulerRuns ghi đợt chạy. WAITING_RECORD: chuyển EMPTY, gọi AllocationResidencySource.waitingRecordHoldExpired trong cùng transaction. ABSENT_RESIDENT: không đổi state/phân bổ, đặt hold_expiry_notified_at và phát AbsentResidentHoldExpired một lần. CFG-M03-08 quyết định executedLate; lưu plannedAt và phát DelayedScheduledWork khi trễ. Không tạo phân bổ/yêu cầu tự động.
- Job có ShedLock, cron 15 phút, startup; profile test không tự chạy, test gọi runNow. Đăng ký FR-015 trong SchedulerScopeArchTest theo hướng dẫn sẵn của file, không bỏ/nới luật. Đăng ký test dùng chung cần A review; team-plan cập nhật dòng bàn giao, không tuyên bố đã merge.

**File tạo/sửa trong lượt Phase 6**

| File | Mục đích |
| --- | --- |
| `backend/src/test/java/com/cambae/backend/m03/FR_011_BedTransitionsTest.java` (tạo) | Bảng chuyển hợp lệ/không hợp lệ T037. |
| `backend/src/test/java/com/cambae/backend/m03/FR_011_BedCommandsIT.java` (tạo) | Tạm ngừng/sử dụng lại, lý do, quyền, FUTURE, dấu tài sản và phòng ngừng hiệu lực. |
| `backend/src/test/java/com/cambae/backend/m03/FR_016_BedHistoryHttpIT.java` (tạo) | Hợp đồng HTTP ba operation T041, quyền và cursor lịch sử. |
| `backend/src/test/java/com/cambae/backend/m03/FR_012_BedHoldsIT.java` (tạo) | T040; bổ sung hai hồi quy từ review. |
| `backend/src/test/java/com/cambae/backend/m03/FR_015_HoldExpiryIT.java` (tạo) | Hết hạn một lần, không giải phóng người vắng và rollback callback 004. |
| `backend/src/main/java/com/cambae/backend/m03/internal/bed/domain/BedHistoryEntry.java` (tạo) | Projection lịch sử nội bộ. |
| `backend/src/main/java/com/cambae/backend/m03/internal/bed/persistence/BedHoldStore.java` (tạo) | SQL đọc/ghi giữ chỗ và phần liên quan phân bổ; không ghi state. |
| `backend/src/main/java/com/cambae/backend/m03/internal/bed/service/BedHoldService.java` (tạo) | Bốn tác động giữ chỗ T040, transaction MANDATORY. |
| `backend/src/main/java/com/cambae/backend/m03/internal/bed/service/HoldExpiryProcessor.java` (tạo) | Kiểm lại/áp dụng hết hạn trong transaction từng giường. |
| `backend/src/main/java/com/cambae/backend/m03/scheduling/HoldExpiryJob.java` (tạo) | Điều phối lịch, ShedLock và scheduler_run cho hạn giữ chỗ. |
| `backend/src/main/java/com/cambae/backend/m03/web/response/BedStateHistoryRow.java` (tạo) | DTO lịch sử theo contract Phase 6. |
| `backend/src/main/java/com/cambae/backend/m03/internal/bed/persistence/BedStore.java` (sửa) | Đọc FUTURE và lịch sử theo giường/cursor. |
| `backend/src/main/java/com/cambae/backend/m03/internal/bed/service/BedService.java` (sửa) | Thêm suspend/reuse qua máy trạng thái. |
| `backend/src/main/java/com/cambae/backend/m03/internal/bed/service/BedQueries.java` (sửa) | Quyền đọc lịch sử, kiểm cursor. |
| `backend/src/main/java/com/cambae/backend/m03/web/BedController.java` (sửa) | Ba operation HTTP T041. |
| `backend/src/test/java/com/cambae/backend/common/ApiContractIT.java` (sửa) | Gỡ đúng 3 pending khi controller có thật; giữ các pending còn lại. |
| `backend/src/test/java/com/cambae/backend/common/scheduling/SchedulerScopeArchTest.java` (sửa) | Đăng ký loại việc FR-015 cho job mới theo hướng dẫn trong file. |
| `specs/003-room-bed-allocation/contracts/api-003.yaml` (sửa) | Bản dịch riêng ba operation đã duyệt và component mới, giữ phần phase sau. |
| `frontend/src/api/api003.js` (sinh lại) | Client cho ba operation Phase 6. |
| `docs/team-plan.md` (sửa) | Trạng thái review A/C, phụ thuộc M01 của Phase 6. |
| `specs/000-shared-foundation/glossary.md` (sửa mục 003) | Tên endpoint/lớp/DTO/job/mã nhật ký và lỗi mới. |
| `specs/003-room-bed-allocation/plan.md` (bổ sung) | Quyết định kỹ thuật, test và phạm vi phụ thuộc. |
| `specs/003-room-bed-allocation/tasks.md` | Trạng thái task sẽ cập nhật sau clean verify; chưa đánh trong lúc đang kiểm. |
| `Working-history.md` (bổ sung) | Giữ nguyên toàn bộ lịch sử cũ, ghi lượt này và kết quả thực tế. |

Không xóa file, không sửa migration đã merge, không thêm thư viện, không sửa nghiệp vụ/spec, không đổi UI hoặc chụp màn hình.

**Review và sửa hồi quy Phase 6**

- Reviewer chỉ đọc, không chạy Maven/sửa file; tìm hai P2 trong code đã cài. Không coi các nhánh chưa cài đã công bố là lỗi implementation.
- Thêm `FR_014_concurrentCancellationIsIdempotentAfterWaitingForTheLock`: hai transaction được chặn bằng barrier sau lần đọc đầu, để tái hiện REPEATABLE READ. RED với ExecutionException/INVALID_BED_TRANSITION ở transaction sau; không phải test hủy tuần tự.
- Thêm `FR_015_extensionOverlapIsAConditionFailureAndLeavesTheOldEnd`: ACTIVE có expected_end trước FUTURE; gia hạn qua FUTURE phải trả ConditionNotMetException và giữ mốc cũ. RED vì SQL ALLOCATION_TIME_OVERLAP thoát ra. Tổng RED của file 7 ca: 1 failure/1 error, 5 ca cũ xanh; log `003-p6-review-red.log`.
- Sửa `BedHoldStore.waitingCurrent` dùng locking/current read trên bed; cancelHold sau khóa kiểm dữ liệu hiện tại, transaction sau không gọi chuyển trạng thái lần hai. `extensionOverlaps` kiểm khoảng từ view đã đính chính; khóa bảng gốc trong cùng lệnh trước khi UPDATE. Bổ sung khóa resident_position trước khi đọc ACTIVE cho release/extend để ổn định thứ tự người → giường.
- Lần kiểm đầu sau sửa: query có locking clause trên view bị MySQL từ chối quyền (view chỉ SELECT). Không mở rộng quyền, không sửa migration; dùng `FOR UPDATE OF locked` với alias bảng bed_allocation gốc, view vẫn chỉ đọc. Kiểm lại **7/7 IT +23 ArchTest**, BUILD SUCCESS, exit 0, **36.406 giây**, log `003-p6-review-green2.log`. Không bỏ test hoặc nới assertion cho xanh.
- Contract cuối có **348 $ref giải được**, 71 operation: 39 Phase 3, 7 Phase 4, 1 Phase 5, 3 Phase 6, 21 chưa dịch. Client được sinh từ hợp đồng. Backend clean verify nền và frontend test/lint/build đang chạy, kết quả chốt sẽ ghi sau khi process kết thúc.

### Kết quả chốt Phase 6 phần độc lập — 2026-10-24

- `mvnw.cmd clean verify` chạy nền hoàn tất **BUILD SUCCESS**, process exit **0**, **02:06**. Surefire **351 test đơn vị/kiến trúc**, Failsafe **495 IT**, cả hai **0 failure/error/skip**; XML failsafe-summary xác nhận completed=495, errors=0, failures=0, skipped=0. Spotless check qua **786 file Java**. Log `C:/Users/nguye/AppData/Local/Temp/003-p6-final-clean-verify.log`.
- Frontend sau sinh client: `npm test` **18 file, 113/113 xanh**, exit 0 (16.39 giây), `npm run lint` và `npm run build` exit **0**. Build vẫn cảnh báo chunk >500 kB, không sửa cấu hình ngoài task. Log `003-p6-web-tests.log`, `003-p6-web-build.log` trong Temp. Không đổi UI, không chụp màn hình.
- `git diff --check` qua sau khi bỏ dòng trắng dư cuối team-plan. Kiểm ngữ nghĩa contract: 348 ref giải được; chỉ 3 operation Phase 6 được dịch theo duyệt; các operation/component cũ khác giữ nguyên. Tổng 24 đường dẫn tạo/sửa của lượt này khớp bảng file phía trên; không xóa file.
- Chỉ sau kết quả clean verify mới đánh **T037, T041 [x]**. T037 là test mở rộng cho máy trạng thái có sẵn, đã ghi rõ không có RED giả. Các chức năng mới T041/T040/job T044 và hai hồi quy review đều đã chạy RED thực tế trước implementation/fix.
- **Chưa hoàn tất toàn bộ Phase 6**: T038 chưa đủ kiểm lệnh tài sản; T039/T043 thiếu SPI/kiểu/sự kiện M01; T040 bốn tác động nội bộ đã có nhưng chưa facade BedAllocationCommandApi đầy đủ; T042 còn nhánh tài sản phụ thuộc checker FR-018/lệnh chuyển theo lịch từ Phase 4–5; T044 mới xong hết hạn giữ chỗ, chưa job báo phân bổ tương lai quá bắt đầu (cần trạng thái M01). Giữ nguyên quyết định chờ B, không thêm fake M01 hoặc phương thức stub cho lệnh chưa cài.
- Cần A review hai điểm đăng ký dùng chung: ApiContractIT gỡ đúng ba pending đã cài và SchedulerScopeArchTest thêm loại việc FR-015. Team-plan có dòng chờ B cho T039/T043/T044. Không đánh “Xong/đã bàn giao” khi chưa có PR/merge.
- Thứ tự tiếp theo trong Phase 6 theo tasks.md: **T038 → T039 → hoàn thiện T040 → T042 → T043 → hoàn thiện T044**; T037/T041 đã xong. Sau checkpoint đầy đủ mới sang Phase 7 T045. Các việc thiếu của Phase 4–5 vẫn còn, không được đánh thay bằng kết quả xanh của phần độc lập.
- Không thêm thư viện, không sửa common production, M01, nghiệp vụ/spec hoặc migration đã merge; không commit/push/merge/rebase/đổi nhánh/tạo PR. Kết quả review hai P2 đều đã sửa bằng RED→GREEN; không có finding minor bị gác trong lượt review này.

### 2026-10-24 — Sửa điểm dừng sớm và làm tiếp nhánh tài sản độc lập T038/T042

**Lý do tiếp tục**: Người dùng hỏi tại sao dừng Phase 6. Đã trả lời rõ M01 chỉ chặn phần cần M01; lượt trước đã dừng quá sớm khi vẫn còn nhánh tài sản độc lập. Kết quả clean verify xanh của lượt trước chỉ là kiểm phần code lúc đó, không chứng minh hết việc có thể triển khai. Tiếp tục phạm vi đã được yêu cầu, không hỏi lại quyết định chờ B.

**Trình tự và bằng chứng test trước**

1. Đọc lại tasks, contract BedAssetCommandPort, bảng chuyển và FR-013/FR-013a/FR-013b, G-03/G-12; tra nghiệp vụ liên quan BR-M03-13/BR-M03-15/Q-234. Không sửa spec/nghiệp vụ.
2. Tạo `FR_013_BedAssetStateIT` trước implementation. RED 7 ca với 4 failure/3 error do chưa có bedAssetStateService. Thêm hai ca cho phân bổ có căn cứ và phòng ngừng hiệu lực; chạy lại RED **9 ca, 5 failure/4 error**, log `C:/Users/nguye/AppData/Local/Temp/003-p6-assets-red2.log`.
3. Cài component nội bộ `BedAssetStateService` và SQL `BedAssetStateStore`; không thay public port, không tạo bản giả M01. Dùng lại BedStateService, BedTransitions, AllocationLocks và BedHoldService helper.
4. `mvnw.cmd spotless:apply test -Dtest=*ArchTest,ModuleBoundaryTest failsafe:integration-test failsafe:verify -Dit.test=FR_013_BedAssetStateIT`: **9/9 IT +23 ArchTest xanh**, exit 0, **BUILD SUCCESS 40.614 giây**. Log `003-p6-assets-green.log`.
5. Review độc lập chỉ ba file mới: không thấy P1/P2 trong phần đã cài; không nghiệm thu adapter port/FR-013b/theo lịch chưa có. Reviewer không chạy Maven/sửa file. Khởi chạy clean verify nền bản mới, kết quả ghi tiếp sau hoàn tất.

**File tạo/sửa mới so với lượt chốt Phase 6 trước**

| File | Mục đích |
| --- | --- |
| `backend/src/test/java/com/cambae/backend/m03/FR_013_BedAssetStateIT.java` (tạo) | 9 ca: chặn có người, chặn FUTURE khi bảo trì thường, vòng đời bảo trì/thanh lý, khôi phục tạm ngừng/lý do, hủy FUTURE ban đầu, rollback lệnh nguồn, MANDATORY, bảo vệ phân bổ có căn cứ, phòng không hiệu lực. |
| `backend/src/main/java/com/cambae/backend/m03/internal/bed/persistence/BedAssetStateStore.java` (tạo) | Đọc/khóa phân bổ FUTURE/ACTIVE và cập nhật hủy; không ghi state giường. |
| `backend/src/main/java/com/cambae/backend/m03/internal/bed/service/BedAssetStateService.java` (tạo) | Nhánh trạng thái tài sản độc lập với quyền/transaction của lệnh 019, kiểm điều kiện, hủy ban đầu và phát sự kiện. |
| `specs/003-room-bed-allocation/plan.md` (bổ sung) | Giải thích điểm dừng cũ và quyết định component nội bộ, giới hạn metadata nguồn/chưa adapter. |
| `specs/003-room-bed-allocation/tasks.md` (bổ sung sau kiểm chốt) | Cập nhật phần T038/T042 đã làm; không đánh task trọn vẹn khi thiếu nhánh. |
| `specs/000-shared-foundation/glossary.md` (bổ sung mục 003) | Tên service/store và mã nhật ký phần tài sản. |
| `Working-history.md` (bổ sung) | Ghi nối tiếp quá trình thực tế, không xóa nội dung cũ. |

Không xóa file, không thêm migration, API/fake công khai hoặc thư viện; không đổi giao diện, không chụp màn hình, không commit/push.

**Luồng dữ liệu và quyết định kỹ thuật**

- Tương lai adapter 019 → BedAssetStateService.apply trong transaction nguồn MANDATORY. Service đọc danh sách người có FUTURE, khóa resident_position tăng dần → dòng giường, đọc FUTURE lại FOR UPDATE. Nếu tập người đổi ngoài tập đã khóa, ném CannotAcquireLockException để transaction nguồn thử lại; không lấy khóa ngược giường → người.
- Kiểm ACTIVE cùng trạng thái OCCUPIED/HELD để chặn tài sản; kiểm bảng BedTransitions; READY kiểm phòng Hiệu lực. Bảo trì thường có FUTURE bị chặn; ngoại lệ gắn hư hỏng khi dọn giường/báo hỏng lại hoặc Thanh lý chỉ áp theo FR-013a.
- Với FUTURE ban đầu không mang metadata nguồn: hủy với lý do Giường hỏng, ghi nhật ký M03_FUTURE_ALLOCATION_CANCELLED và chuyển state qua BedStateService cùng transaction. Phát FutureAllocationCancelledForDamagedBed kèm danh sách người cho Hành chính/Trưởng tầng phụ trách. Khi lệnh 019 thất bại sau tác động, test xác nhận cả state/history và trạng thái phân bổ rollback.
- READY khôi phục SUSPENDED và lý do từ lịch sử nếu vào bảo trì từ SUSPENDED; trường hợp còn lại về EMPTY theo bảng. BedBecameEmpty chỉ phát khi không FUTURE và không dấu tài sản.
- FUTURE có basis_type/basis_id/previous_allocation_id được giữ nguyên và trả SOURCE_UNAVAILABLE trước ghi: chưa nối luồng căn cứ/theo lịch nên không tự hủy riêng phân bổ mà bỏ lại request. Đây là giới hạn vận hành của component đang cài, không bổ sung quy tắc nghiệp vụ mới.
- Chưa implements BedAssetCommandPort vì còn callback FR-013b, suitable-empty-bed checker FR-018 và tác động request theo lịch; không thêm phương thức stub. T038/T042 vẫn chưa đủ để đánh [x]. Các nhánh cần M01 tiếp tục giữ chờ B.

**Kết quả chốt phần tài sản bổ sung — 2026-10-24**

- `mvnw.cmd clean verify` nền bản mới **BUILD SUCCESS**, exit **0**, **02:12**; **351 test đơn vị/kiến trúc +504 IT**, cả hai 0 failure/error/skip. `failsafe-summary.xml` completed=504, failures=0, errors=0, skipped=0; Spotless check **789 file** qua. Log `C:/Users/nguye/AppData/Local/Temp/003-p6-assets-final-clean-verify.log`.
- `git diff --check` qua. Reviewer không thấy P1/P2 trong 3 file mới. Không có thay đổi UI/client trong lượt bổ sung nên không lặp frontend test/lint/build; kết quả 113 test/lint/build trước đó đã ghi đúng thời điểm riêng.
- Sau kiểm cập nhật tasks/plan/glossary, **không đánh thêm task [x]**: T038/T042 có thêm nhánh độc lập đã kiểm nhưng vẫn thiếu adapter/FR-013b/suitable checker và kết nối căn cứ/theo lịch. Kết quả 495 IT trước là trạng thái trước bổ sung; kết quả backend mới nhất là 504 IT.
- Thứ tự tiếp tục vẫn T038 (nhánh dấu chờ chuyển người và bộ kiểm tương ứng), T039 (M01), hoàn thiện T040 facade, T042 adapter/FR-013b, T043 (M01), phần còn lại T044; không triển khai vượt sang Phase 7. M01 vẫn chờ B. Không commit/push/đổi nhánh, không thêm API/fake/thư viện và không chụp màn hình.

### 2026-10-24 — Xử lý conflict khi merge origin/develop vào feature/003-manage-bedroom

**Yêu cầu và phạm vi**: Sau khi xem conflict và đề xuất ghép hai phía, chủ dự án xác nhận “ok xác nhận làm đi”. HEAD là `9babe1d`, MERGE_HEAD là `4fbb0d840eeb6756b812bfd90056f212dc14a5be`. Chỉ xử lý năm file conflict, sinh lại client và xác minh bản ghép; không triển khai thêm task Phase 6, không commit/push hoặc chạy `git merge --continue`.

**Trình tự thực hiện và các file**

1. Kiểm tra trạng thái merge, đọc từng hunk và các thay đổi liên quan từ hai phía. Giữ thay đổi M03 tại HEAD cùng các phần M01/M13/M15 mới từ develop.
2. `backend/src/test/java/com/cambae/backend/common/ApiContractIT.java`: ghép đăng ký hợp đồng 000/001/002/003/009; chuyển đăng ký 003 sang constructor Contract bốn tham số của develop, giữ pending của 003 và bật kiểm operation của 003. Giữ cấu hình checkOperations=false của 001 đã có từ develop và backlog tương ứng; không tắt thêm test để xử lý merge. 002 dùng danh sách pending đã hoàn tất từ develop; 009 giữ pending còn lại của develop.
3. `backend/src/test/java/com/cambae/backend/common/scheduling/SchedulerScopeArchTest.java`: giữ Map.ofEntries và các job từ develop, thêm HoldExpiryJob của M03; tổng 16 mục. Map.ofEntries cần thiết vì Map.of không hỗ trợ hơn 10 cặp; giữ nguyên luật kiểm phạm vi scheduler.
4. `docs/team-plan.md`: hợp nhất các dòng phụ thuộc của C với trạng thái mới từ develop, giữ các dòng bổ sung của nhóm. Cập nhật ba dòng chờ M01 vì ResidentApi, CareLevelApi, ResidentStatusApi, StatusTransitionParticipant và các sự kiện liên quan đã hiện diện. FakeResidents/FakeResidentEvents/FakeStatusTransitionParticipant có sẵn; không kết luận mọi fake nguồn đã đủ. Việc nối và test M03 với nguồn mới vẫn còn riêng.
5. `frontend/scripts/gen-api.mjs`: hợp nhất TRANSLATED thành 000/001/002/003/009; giữ bộ lọc x-implementation-phase riêng của 003 để chỉ sinh operation đã duyệt.
6. `frontend/src/api/index.js`: sinh bằng `npm run gen:api`, không sửa tay; xuất đủ api000/api001/api002/api003/api009. Luồng dữ liệu: contracts YAML → gen-api.mjs → client từng feature và index.js → các màn dùng client. api001.js/api009.js được ghi lại bởi generator nhưng không có diff nội dung so với phần develop đã staged.
7. Chạy Spotless, kiểm kiến trúc và hợp đồng, kiểm frontend. Sau đó `git add --` đúng năm file conflict trên để đánh dấu đã giải quyết; `git ls-files -u` rỗng, MERGE_HEAD vẫn tồn tại. Không dùng git add toàn bộ; Working-history.md tiếp tục là file lịch sử chưa tracked có sẵn.

**Xác minh đã hoàn tất ở bước kiểm hẹp**

- `npm run gen:api`: exit 0, sinh 5 client. `node --check scripts/gen-api.mjs` và `node --check src/api/index.js`: exit 0.
- `mvnw.cmd spotless:apply test -Dtest=*ArchTest,ModuleBoundaryTest failsafe:integration-test failsafe:verify -Dit.test=ApiContractIT`: BUILD SUCCESS, exit 0, 25 test kiến trúc +1 ApiContractIT, không failure/error/skip. Spotless kiểm 1196 file, chỉ định dạng lại hai Java conflict. Log: `C:/Users/nguye/AppData/Local/Temp/003-merge-contract-arch.log`.
- `npm test`: 31 file, 226 test qua. `npm run lint` và `npm run build`: exit 0. Build có cảnh báo chunk lớn hơn 500 kB; không có lỗi build. Log lần lượt `003-merge-frontend-test.log`, `003-merge-frontend-lint.log`, `003-merge-frontend-build.log` trong `C:/Users/nguye/AppData/Local/Temp/`.
- Không còn marker conflict trong năm file; `git diff --check` và `git diff --cached --check` qua. Không thay giao diện, không chụp màn hình, không thêm thư viện hoặc sửa hợp đồng API.
- Đã khởi chạy `mvnw.cmd clean verify` nền với Java 25 và MySQL test; kết quả cuối ghi nối tiếp sau khi lệnh kết thúc, không suy từ kiểm hẹp.

**Việc còn lại**: Merge chưa có commit. Kết quả xử lý conflict không đồng nghĩa Phase 6 hoàn tất; thứ tự task còn lại của 003 vẫn T038 → T039 → hoàn thiện T040 → T042 → T043 → hoàn thiện T044 rồi mới T045. M01 đã có trong checkout nên cần đánh giá lại từng điểm nối theo API thực tế, không giữ kết luận cũ rằng toàn bộ M01 chưa tồn tại. Không đánh thêm task [x] trong lượt xử lý conflict.

**Kết quả xác minh cuối bản ghép**

- `mvnw.cmd clean verify` nền: BUILD SUCCESS, exit 0, thời gian 03:58; 392 test đơn vị/kiến trúc +938 test tích hợp, không failure/error/skip. `failsafe-summary.xml` completed=938, failures=0, errors=0, skipped=0. Spotless check 1196 file qua. Log `C:/Users/nguye/AppData/Local/Temp/003-merge-clean-verify.log`.
- Năm file conflict đã được stage đúng phạm vi; `git ls-files -u` rỗng. MERGE_HEAD vẫn là `4fbb0d840eeb6756b812bfd90056f212dc14a5be`, chưa tạo merge commit. Working-history.md được bổ sung nối tiếp và chưa stage.
- Phần xử lý conflict đã xong, không có lỗi build/test còn tồn tại trong các lệnh đã chạy. Giữ backlog 001 về kiểm operation theo team-plan hiện có; Phase 6 của 003 vẫn còn các task đã liệt kê, cần triển khai và test nối M01 trong lượt feature tiếp theo. Cảnh báo bundle frontend lớn và log publication thử lỗi từ FailingChecker không làm các bộ test thất bại; không tự sửa ngoài phạm vi merge.

### 2026-10-24 — Tiếp tục các task cần M01 trong Phase 1–6 sau merge

- Chủ dự án yêu cầu rà Phase 1–6 và thực hiện tiếp các task trước đây bị chặn bởi M01. Đã đọc lại AGENTS/CLAUDE/constitution, tasks, ghi chú plan, backend README; báo các dòng C ở team-plan mục 8. Không tiếp tục merge hoặc commit/push; giữ index bản ghép đã kiểm.
- Bổ sung ma trận phụ thuộc vào plan: Phase 1–2 đã xong nền; Phase 3 T012/T015/T017 cần FR-006; Phase 4 T019/T020/T021/T025/T026/T027/T028 cần FR-018/lệnh; Phase 5 T029–T036 cần nền và khoảng vắng/sự kiện; Phase 6 T038–T044 cần SPI M01/lệnh nền. Không sửa module M01, không tạo fake M01 thay chủ phần đó.
- T012 viết FR_006_RoomPolicyIT trước implementation: lượt đầu lỗi biên dịch fixture (SwitchableUser không có get), đã sửa sang accountId; lượt kế thiếu TEST_DB_URL vì biến User không có trong tiến trình, dùng cấu hình test chủ dự án đã cung cấp cho các tiến trình sau. Đây chưa phải RED nghiệp vụ. Lượt RED thực tế `003-m01-policy-red3.log`: 7 ca, 2 failure/4 error/1 pass, lỗi do StructureService còn trả SOURCE_UNAVAILABLE cho chính sách phòng.
- T015 cài ResidentAllocationFacts (service) và CheckedCareLevel (domain) để đọc ResidentApi/CareLevelApi, chọn mức hiện hành trước; chỉ Đang tiếp nhận thiếu mức mới fallback AllocationResidencySource hợp đồng → lượt đăng ký Q-301. Ánh xạ REGULAR → FREQUENT tường minh. StructureService dùng facts kiểm từng người, chỉ báo vi phạm FR-006; StructureStore đọc danh sách người từ v_current_allocation đã áp đính chính. Không đọc bảng M01 trong production.
- File tạo: backend/src/test/java/com/cambae/backend/m03/FR_006_RoomPolicyIT.java (7 IT); backend/src/main/java/com/cambae/backend/m03/internal/allocation/domain/CheckedCareLevel.java; .../service/ResidentAllocationFacts.java. File sửa: .../internal/structure/service/StructureService.java; .../persistence/StructureStore.java; specs/003-room-bed-allocation/plan.md; Working-history.md. Không xóa file.
- GREEN T012/T015: `mvnw.cmd spotless:apply test -Dtest=*ArchTest,ModuleBoundaryTest failsafe:integration-test failsafe:verify -Dit.test=FR_006_RoomPolicyIT`: 25 ArchTest +7 IT qua, BUILD SUCCESS 51.236 giây, exit 0; log 003-m01-policy-green.log trong Temp. Chưa đánh [x] trước clean verify cuối.
- T019 viết BR_M03_01_AllocationConditionsIT trước, RED 7/7 vì chưa có allocationChecker. T025 thêm AllocationCandidate (domain), AllocationCheckStore (SQL M03), AllocationChecker (service) dùng chung facts và các SPI nguồn lưu trú/cách ly/tài sản. Kiểm đầy đủ FR-018(a)–(i); interval nửa mở, xem view đã áp đính chính. GREEN lần đầu 6/7, ca đổi trạng thái qua JDBC trong cùng transaction còn entity Resident cached từ lần đọc trước. Đã xác định nguyên nhân fixture và clear EntityManager trước lần đọc API thứ hai; không sửa M01 để khớp test. Kết quả kiểm lại ghi nối tiếp khi hoàn tất.

**T025/T026/T020/T027 — các bước nối tiếp trong lượt M01**

- T025 GREEN sau sửa fixture: 25 ArchTest +7 IT, BUILD SUCCESS 44.311 giây, log `003-m01-checker-green2.log`. Chưa nghiệm thu cả T025 vì còn retry/kiểm tích hợp các luồng sau.
- T021/T026: viết FR_023_AllocationCommandsIT trước, RED 5/5 do chưa có allocationCommands. Cài AllocationRecord (domain), AllocationCommandStore (persistence), AllocationCommands (service). Lệnh khóa resident_position rồi giường tăng dần, dùng trạng thái M01 quyết FUTURE/ACTIVE, ghi mức/nguồn, callback AllocationResidencySource.allocated, ghi audit; ACTIVE gỡ without_bed_since, đổi state qua BedStateService và phát BedAllocationStarted. Hủy chỉ FUTURE, giữ bản ghi và lý do, đọc FOR UPDATE sau khóa.
- Lần kiểm đầu bị quyền: fixture Quản lý viện không có CREATE phân bổ theo ma trận; sửa riêng fixture sang OFFICE_STAFF, không nới production. Lần sau phát hiện ScopeApi.inScope chỉ xét phân công, không thay kiểm PermissionApi ba lớp cho Hành chính có phạm vi toàn viện; sửa lệnh dùng PermissionApi với khóa BED_ALLOCATION và Target giường. Hủy dùng UPDATE đúng ma trận. GREEN `003-m01-allocation-green3.log`: 25 ArchTest +5 IT, BUILD SUCCESS 51.194 giây.
- T020 thêm FR_020_AllocationConcurrencyIT: mười transaction thật cạnh tranh một giường. RED: một lệnh thua lọt xuống trigger ALLOCATION_TIME_OVERLAP dưới dạng UncategorizedSQLException vì đã thiết lập snapshot RR trước khi chờ khóa bed. Chuyển riêng lệnh create sang READ_COMMITTED, giữ khóa và trigger DB. GREEN `003-m01-concurrency-green.log`: đúng một thành công, các lệnh còn lại trả lỗi nghiệp vụ, 25 ArchTest +1 IT qua, BUILD SUCCESS 51.409 giây. Không đổi isolation toàn hệ thống hoặc thêm thư viện.
- T027 viết FR_022_AllocationHttpIT trước: RED 4/4 do endpoint 404. Cài sáu operation đã duyệt: suitable-beds, POST/GET bed-allocations, GET detail, POST cancel, GET resident location. Gỡ đúng sáu pending trong ApiContractIT (giữ beds/summary và các operation khác chưa cài), không bỏ so hợp đồng. GREEN `003-m01-http-green.log`: 25 ArchTest +4 HTTP IT +1 ApiContractIT, BUILD SUCCESS 01:01, exit 0. Chưa đủ T027 vì summary và các nhánh quyền/hiển thị còn hoàn thiện.
- HTTP dùng AllocationDetails (domain), AllocationReadStore, AllocationReading; response BedAllocationResponse/SuitableBedResponse/ResidentLocationResponse, request BedAllocationWrite, BedAllocationController/ResidentLocationController. Luồng: request → service quyền/khóa/checker → store M03 + API nguồn → audit/event → đọc view hiện hành → response chỉ trường hợp đồng; tên actor qua AccountApi. Không sửa generated client hay hợp đồng trong bước này.
- Mã điều kiện công khai theo mô tả hợp đồng là ALLOCATION_*; lưu FR-018(a)–(i) ở basis. Không trả module/waivable vì schema 003 chưa có hai trường này. Test checker giữ các tình huống, cập nhật tên mã cho đúng hợp đồng.
- Áp dụng skill requesting-code-review, mời một reviewer chỉ đọc core M03, không chạy Maven/sửa file. Ba finding P2: thiếu deadline AWAITING_READY, hủy nhầm giữ chỗ giường đích FUTURE, FR-006 đọc snapshot cũ trước khi chờ giường. Viết test hồi quy trước: `003-m01-review-red.log` có 14 ca, đúng 3 failure (deadline, HELD thành EMPTY, cập nhật chính sách bỏ sót người vừa commit), 0 error. Đang sửa và chạy GREEN; chưa ghi nhận đã qua khi process chưa kết thúc.
- T035 đã viết FR_034_AllocationSuitabilityIT trước: RED 1/1 thiếu allocationSuitabilityListener (`003-m01-suitability-red.log`); chưa cài listener tại thời điểm ghi đoạn này.
- Không đánh thêm [x], không commit/push, không mở tiếp merge. Các log ở C:/Users/nguye/AppData/Local/Temp/. Không đổi UI nên không chụp màn hình.

### 2026-10-24 — Hoàn thiện nhánh đọc HTTP, participant M01 và web chọn giường

- T027: thêm BedCapacityQueries/BedCapacitySummary, ResidentLocationQueries/ResidentLocationDetails, BedSummaryController/BedSummaryResponse, request BedResidencyType (chỉ LONG_TERM/SHORT_TERM). Summary loại SUSPENDED khỏi usable; người chỉ được xem vị trí không nhận phân bổ/dấu; người chưa từng có phân bổ vẫn nhận withoutBedSince. Test HTTP đỏ 3 nhánh trước sửa; sau sửa 7 HTTP IT +1 ApiContractIT +25 ArchTest xanh, log 003-m01-http-branches-green2.log. Không đổi HTTP contract.
- T035: AllocationSuitabilityListener nhận InitialAssessmentConfirmed, CareLevelChanged, PersonalInfoUpdated sau commit; AllocationSuitabilityService kiểm hiện hành và đặt/gỡ room_unsuitable, không hủy FUTURE. FR_034_AllocationSuitabilityIT đỏ do thiếu listener rồi xanh cùng 25 ArchTest (52.717 giây), log 003-m01-suitability-green.log. Còn sự kiện đổi loại lưu trú của 004, chưa tự tạo thay B.
- T044: PastStartAllocation (domain), PastStartAllocationStore, PastStartAllocationProcessor, FutureAllocationPastStartJob báo FUTURE quá mốc bắt đầu của người ADMISSION_IN_PROGRESS; khóa và kiểm lại, lưu chính start_at đã báo để chống lặp, không tự mở ACTIVE. FR_023_FutureAllocationPastStartIT đỏ 2 ca rồi xanh cùng 25 ArchTest (52.026 giây), log 003-m01-past-start-green.log. Đăng ký phạm vi SchedulerScopeArchTest. Chủ dự án chọn giữ nguyên api-000.yaml/TestClockController; đã thêm bàn giao A vào team-plan mục 8 cho EXPIRE_BED_HOLDS và REPORT_FUTURE_ALLOCATION_PAST_START.
- T039/T043: thêm AllocationStatusParticipant, @Order(20), tham gia 7 lệnh trạng thái trong transaction M01. Lệnh Hoàn tất tiếp nhận kiểm và mở FUTURE tại thời điểm thực tế, có thể đổi giường qua switchAllocationToBedId đã có trong hợp đồng 001; Hủy tiếp nhận hủy FUTURE và giữ chỗ; tạm vắng/chuyển viện giữ hoặc đóng theo nguồn; trở về đổi trạng thái hoặc đánh dấu chưa có giường; kết thúc/qua đời đóng phân bổ, hủy tương lai và gỡ dấu. AllocationCommandStore thêm current read, activate/close/audit helper; clearWithoutBed dọn allocation_reminder. FR_024_AllocationStatusParticipantIT viết đỏ 9 ca trước; sau sửa assertion nguyên nhân gốc của exception bị Spring bọc, bổ sung test audit và dừng nhắc. 10 FR024 +6 FR023 +6 StatusEndpointsIT thật của M01 +25 ArchTest xanh, BUILD SUCCESS 54.664 giây, log 003-m01-status-integration-green.log.
- Rà soát participant phát hiện 4 lỗi cần hồi quy: lấy nhầm giữ chỗ sau snapshot RR; hủy tiếp nhận bỏ sót giữ chỗ sau snapshot; chuyển viện từ TEMPORARILY_ABSENT khi giường đã HELD; DAY_CARE trở về bị đánh dấu chưa có giường. Test FR024 mở rộng chạy đỏ 15 ca: 3 failure +2 error, log 003-m01-status-race-red.log. Không bỏ/xóa test đỏ.
- Đã sửa phần chuyển viện khi đang vắng: BedStateService.refreshAbsentHold và BedStateStore.refreshAbsentHold chỉ đồng bộ hạn, audit M03_BED_HOLD_UPDATED, không thêm chuyển trạng thái HELD→HELD ngoài bảng; không giữ thì dùng RELEASE_WHEN_ABSENT. DAY_CARE trở về không lập dấu/nhắc chưa có giường. Fixture deadline cắt đến microsecond theo DATETIME(6), tránh so nanosecond với DB.
- T025: FR_020_AllocationRetryIT gây lỗi khóa sau lần INSERT đầu để kiểm rollback và thử lại toàn transaction. Đỏ vì PessimisticLockingFailureException thoát ra, log 003-m01-retry-red.log. Cài AllocationRetryConfig trong internal/allocation/config và @Retryable của Spring Framework 7 đã có, chỉ lỗi khóa, tối đa hai lần thử lại, delay kỹ thuật 50ms, advice bao ngoài transaction. Không thêm thư viện. Xanh 1 retry +1 test mười transaction +8 ca FR024 đã sửa +25 ArchTest, log 003-m01-retry-green.log. Hai ca RR còn đỏ được giữ để tiếp tục sửa, không suy rằng cả participant đã xong.
- Chủ dự án chọn giữ M01 cho B, không cho sửa isolation. Hướng xử lý trong M03: V003_04_1__waiting_hold_index.sql thêm JSON waiting_hold_bed_ids trên dòng resident_position hiện có và backfill giữ chỗ đang tồn tại; WaitingHoldIndexStore đọc hiện hành FOR UPDATE sau khóa người, để lấy đủ id giường trước khi khóa tăng dần. BedStateService duy trì chỉ mục cùng transaction của mọi chuyển WAITING_RECORD; BedStateSnapshot bổ sung holdResidentId. AllocationChecker/AllocationCheckStore thêm đường kiểm current/locking read cho SPI sau khóa, không đổi isolation M01, không mở transaction con production. Chỉ mục này là dữ liệu kỹ thuật, không tự sinh bản ghi nghiệp vụ mới. Đang chạy test hồi quy, chưa ghi xanh.
- T028: viết BedAllocationForm.test.jsx (3 ca) và BedAllocationsPanel.test.jsx (2 ca) đỏ do chưa có component; thêm hai component tương ứng trong features/beds. Form tải suitable-beds theo người/thời điểm, không chọn sẵn, bắt buộc lý do, gửi Idempotency-Key, giữ lỗi đủ unmetConditions, bỏ chọn khi tải lại/đổi thời điểm. Panel dùng API thật summary/list, tìm hồ sơ qua client 001, người dùng chọn hồ sơ trước khi mở form. BedsPage thêm tab Phân bổ giường theo quyền BED_ALLOCATION, giữ tab mặc định cũ. Thêm test tích hợp vào BedsPage.test.jsx; lần đầu đỏ thiếu tab, sau sửa 19 test trong 4 file beds xanh (003-m01-ui-green3.log). Chưa chạy kiểm cuối toàn frontend.
- Vướng công cụ: một lần automatic approval review không thực hiện action do hết hạn mức dịch vụ, không phải kết luận action thiếu an toàn; lần tiếp tục đã kiểm file rồi chạy lại phần chưa thực hiện. Không có commit/push/merge tiếp.
- Việc chưa xong theo thứ tự tasks.md: kiểm đủ T012/T015/T017; hoàn tất và nghiệm thu T019–T028 sau clean verify; tiếp T029–T036 (nhập bù T031/T034 vẫn chờ B cài ResidentStatusApi.statusPeriods đang throw UnsupportedOperationException); tiếp T038–T044, giữ các phụ thuộc M02/M04/M07/tài sản và bàn giao A nêu trong team-plan. Không đánh thêm [x] trong đoạn lịch sử này.

**Danh sách file của phần tích hợp M01 đã tạo/sửa trong lượt này** (đường dẫn dưới đây thuộc `backend/src/main/java/com/cambae/backend/m03/` trừ khi ghi đầy đủ):

| File | Mục đích và quan hệ |
| --- | --- |
| `internal/allocation/domain/CheckedCareLevel.java` | Mức và nguồn đã kiểm, truyền từ ResidentAllocationFacts vào lệnh/store. |
| `internal/allocation/domain/AllocationCandidate.java` | Dữ kiện giường/phòng cho checker, có readyDeadline và chủ giữ chỗ. |
| `internal/allocation/domain/AllocationRecord.java` | Dòng vòng đời tối thiểu để khóa và đổi trạng thái phân bổ. |
| `internal/allocation/domain/AllocationDetails.java` | Projection đầy đủ cho HTTP, tách khỏi persistence và response. |
| `internal/allocation/domain/ResidentLocationDetails.java` | Vị trí, ACTIVE/FUTURE và withoutBedSince sau lọc quyền. |
| `internal/allocation/domain/PastStartAllocation.java` | Dữ liệu job báo quá bắt đầu; không tạo phân bổ. |
| `internal/bed/domain/BedCapacitySummary.java` | Số lượng giường theo trạng thái và sức chứa hợp lệ. |
| `internal/allocation/service/ResidentAllocationFacts.java` | Gọi ResidentApi/CareLevelApi thật và nguồn lưu trú fallback; ánh xạ REGULAR→FREQUENT. |
| `internal/allocation/service/AllocationChecker.java` | Một bộ kiểm FR-018 cho suitable/create/SPI, có đường current read sau khóa. |
| `internal/allocation/persistence/AllocationCheckStore.java` | Đọc candidate, mức phòng và chồng khoảng từ view hiện hành. |
| `internal/allocation/service/AllocationCommands.java` | Tạo/hủy phân bổ với quyền thật, khóa, audit/event và callback nguồn. |
| `internal/allocation/persistence/AllocationCommandStore.java` | Ghi create/cancel/activate/close/dấu; current read sau khóa; dọn nhắc khi gỡ dấu. |
| `internal/allocation/service/AllocationReading.java` | Danh sách/detail/suitable theo quyền từng đối tượng; bỏ helper đọc marker không kiểm quyền vì không còn dùng. |
| `internal/allocation/persistence/AllocationReadStore.java` | Projection HTTP từ v_current_allocation; đọc marker nullable đúng cách. |
| `internal/allocation/service/ResidentLocationQueries.java` | Tách người có quyền xem phân bổ với người chỉ được xem vị trí. |
| `internal/bed/service/BedCapacityQueries.java` | Tổng hợp các giường được xem, dùng RoomPolicyApi kiểm emptyMatching. |
| `internal/allocation/service/AllocationSuitabilityListener.java` | Nhận 3 sự kiện M01 sau commit qua Modulith. |
| `internal/allocation/service/AllocationSuitabilityService.java` | Đánh giá lại chính sách (a,b,c), đặt/gỡ dấu và báo khi chuyển sang không phù hợp. |
| `internal/allocation/service/AllocationStatusParticipant.java` | Tác động đồng bộ 7 lệnh của M01; lỗi rollback transaction nguồn. |
| `internal/allocation/persistence/WaitingHoldIndexStore.java` | Đọc và duy trì chỉ mục giữ chỗ trên dòng vị trí đã khóa. |
| `internal/allocation/config/AllocationRetryConfig.java` | Đăng ký retry trước transaction, không thêm thư viện. |
| `internal/allocation/service/PastStartAllocationProcessor.java` | Mỗi phân bổ một transaction, khóa và kiểm lại trước báo. |
| `internal/allocation/persistence/PastStartAllocationStore.java` | Quét/đánh mốc start_at đã báo để chống lặp. |
| `scheduling/FutureAllocationPastStartJob.java` | Cron/startup 15 phút, lọc người đang tiếp nhận qua ResidentApi, không mở ACTIVE. |
| `web/BedAllocationController.java` | REST suitable/list/create/detail/cancel theo hợp đồng đã duyệt. |
| `web/ResidentLocationController.java` | HTTP vị trí, không gọi persistence trực tiếp. |
| `web/BedSummaryController.java` | HTTP beds/summary. |
| `web/request/BedAllocationWrite.java`, `web/request/BedResidencyType.java` | Request ghi và enum HTTP đúng hai loại nội trú của contract. |
| `web/response/BedAllocationResponse.java` | Ánh xạ projection và tên actor qua AccountApi, đúng schema 003. |
| `web/response/SuitableBedResponse.java`, `web/response/ResidentLocationResponse.java`, `web/response/BedSummaryResponse.java` | Response đúng các schema HTTP đã duyệt. |
| `internal/structure/service/StructureService.java`, `internal/structure/persistence/StructureStore.java` | FR-006 kiểm người vi phạm bằng dữ kiện M01 và view đính chính, update READ_COMMITTED. |
| `internal/allocation/service/AllocationLocks.java`, `internal/allocation/persistence/AllocationStore.java` | Khóa vị trí, shared room guard, rồi giường tăng dần; giữ thứ tự tương thích sửa phòng. |
| `internal/bed/service/BedHoldService.java`, `internal/bed/persistence/BedHoldStore.java` | Giữ target FUTURE; hủy theo người bằng lookup hiện hành, không bỏ sót giữ chỗ commit sau snapshot. |
| `internal/bed/service/BedStateService.java`, `internal/bed/persistence/BedStateStore.java`, `internal/bed/domain/BedStateSnapshot.java` | Chỉ mục giữ chỗ cùng transaction, refresh hạn người đã vắng; khóa riêng bed FOR UPDATE, room shared để tránh nâng khóa. |
| `backend/src/main/resources/db/migration/V003_04_1__waiting_hold_index.sql` | Migration mới thêm/backfill chỉ mục, không sửa migration đã merge. |
| `backend/src/test/java/com/cambae/backend/m03/FR_006_RoomPolicyIT.java`, `FR_006_RoomPolicyConcurrencyIT.java` | Chính sách phòng, fallback M01, chặn đổi cha và race người vừa được phân bổ. |
| `backend/src/test/java/com/cambae/backend/m03/BR_M03_01_AllocationConditionsIT.java` | Toàn bộ nhóm điều kiện, khoảng nửa mở và dữ kiện nguồn. |
| `backend/src/test/java/com/cambae/backend/m03/FR_023_AllocationCommandsIT.java` | Tạo FUTURE/ACTIVE, marker, hủy và giữ target. |
| `backend/src/test/java/com/cambae/backend/m03/FR_020_AllocationConcurrencyIT.java`, `FR_020_AllocationRetryIT.java` | Mười lệnh tranh giường; rollback INSERT dở và retry transaction mới. |
| `backend/src/test/java/com/cambae/backend/m03/FR_022_AllocationHttpIT.java` | Bảy nhánh HTTP, contract, quyền và marker. |
| `backend/src/test/java/com/cambae/backend/m03/FR_034_AllocationSuitabilityIT.java` | Nhận sự kiện M01 thật sau commit, đặt/gỡ dấu, giữ FUTURE. |
| `backend/src/test/java/com/cambae/backend/m03/FR_023_FutureAllocationPastStartIT.java` | Quá mốc báo một lần, đổi start_at mới báo lại, không tự mở phân bổ. |
| `backend/src/test/java/com/cambae/backend/m03/FR_024_AllocationStatusParticipantIT.java` | Tác động 7 lệnh, rollback M01/M03, audit, các race RR và thứ tự khóa. |
| `backend/src/test/java/com/cambae/backend/common/ApiContractIT.java` | Gỡ đúng bảy pending Phase 4 khi đã có controller, giữ đối chiếu schema. |
| `backend/src/test/java/com/cambae/backend/common/scheduling/SchedulerScopeArchTest.java` | Đăng ký phạm vi job mới, không nới quyền tạo bản ghi của scheduler. |
| `frontend/src/features/beds/BedAllocationForm.jsx`, `BedAllocationForm.test.jsx` | Form chọn giường và 3 test: không chọn sẵn, request/idempotency, đủ lỗi và tải lại. |
| `frontend/src/features/beds/BedAllocationsPanel.jsx`, `BedAllocationsPanel.test.jsx` | Bảng/summary, tìm và chọn hồ sơ thật; 2 test quyền chỉ xem và mở form. |
| `frontend/src/features/beds/BedsPage.jsx`, `BedsPage.test.jsx` | Tab Phân bổ giường và test tích hợp, giữ đường chuyển giường cũ. |
| `specs/003-room-bed-allocation/plan.md`, `tasks.md` | Quyết định kỹ thuật, tiến độ và điều kiện nghiệm thu; chưa đánh trước kiểm cuối. |
| `specs/000-shared-foundation/glossary.md` | Bổ sung các tên tiếng Anh thuộc 003 đã dùng. |
| `docs/team-plan.md` | Sửa thông tin phụ thuộc statusPeriods còn stub và bàn giao A enum job. |
| `Working-history.md` | Bổ sung lịch sử nối tiếp, không xóa nội dung trước. |

Không xóa file nguồn nào trong lượt tích hợp này. Không sửa source/test M01 hoặc enum job 000 sau lựa chọn của chủ dự án. Các file M01/000 đang staged trong merge là dữ liệu đã có từ merge, không phải thay đổi mới của phần tích hợp này.

**Bằng chứng thêm sau review**:
- `003-m01-status-race-green.log`: 15 FR024 +25 ArchTest xanh; không có FR_015_BedHoldIT trong checkout nên pattern tên này không đóng góp test, không tính thành test đã chạy.
- `003-m01-return-isolation-red.log`: 2 ca, 1 failure do trở về đọc cách ly cũ; `003-m01-room-lock-red.log`: 1 error tái hiện MySQL deadlock giữa giường và phòng.
- Sửa AllocationLocks/AllocationStore để lấy shared guard phòng trước toàn bộ bed locks; BedStore.lock cũng guard trước bed; BedStateStore.lock chỉ FOR UPDATE OF b, đọc trạng thái phòng FOR SHARE để không nâng room thành exclusive. Mapping bed→room hiện không được thay khi có lịch sử; đường đổi phòng giường vẫn đang tạm hoãn ở T017. AllocationStatusParticipant.returned đọc currentCandidate sau khóa để thấy cách ly vừa commit.
- `003-m01-lock-guard-green.log`: 35 IT (1 policy race +7 giữ chỗ +3 expiry +6 state +18 participant) và 25 ArchTest, BUILD SUCCESS. Thêm một ca đồng thời hai người tiếp nhận ở hai giường cùng phòng để kiểm không nâng khóa phòng; ca mới sẽ được chạy trong clean verify, chưa ghi nhận kết quả tại đây.
- `npm test`: 232 test /33 file qua; `npm run lint`: không lỗi; `npm run build`: qua, còn cảnh báo chunk lớn hơn 500 kB đã có. Log 003-m01-frontend-test.log, 003-m01-frontend-lint.log, 003-m01-frontend-build.log. Đã định dạng riêng sáu file frontend sửa bằng Prettier.
- Đã chạy `mvnw.cmd spotless:apply` thành công; đang chạy nền `mvnw.cmd clean verify`, log 003-m01-clean-verify.log. Chưa dùng kết quả kiểm hẹp để đánh [x].

**Sửa tiếp sau clean verify lần hai (không sửa M01)**

- `003-m01-clean-verify-final.log`: 392 unit/kiến trúc qua; 992 IT còn 6 failure +11 error. Ngoài 14 lỗi fixture/contract M01, có 1 failure fixture deadline và 2 error deadlock M03. Không tính lượt này là xanh, không đánh thêm task.
- Đã đọc `SHOW ENGINE INNODB STATUS` bằng kết nối test chỉ đọc; log `003-m01-deadlock-detail2.log` xác nhận vòng S next-key/insert-intention tại `idx_allocation_bed_start` trong trigger khi nhiều UPDATE start_at ở transaction RR. Đây là nguyên nhân khác với scan PRIMARY trên view trước đó. Không sửa trigger/migration đã merge và không thay isolation M01.
- Thêm `backend/src/main/resources/db/migration/V003_04_2__allocation_write_guard.sql`: bảng kỹ thuật `allocation_write_guard`, một dòng khóa id=1, không phải bản ghi nghiệp vụ. `AllocationStore.guardWrites` và `AllocationLocks.lock` lấy khóa trước position/room/bed: transaction explicit READ_COMMITTED giữ S, transaction RR/default giữ X. RC writers vẫn chạy song song; RR source được tuần tự hóa trước khi giữ allocation row/range locks, xử lý cả next-key và supremum gap. Khóa cùng transaction nguồn nên rollback/commit tự giải phóng. Đây là đánh đổi kỹ thuật do giữ nguyên isolation của M01 theo lựa chọn chủ dự án; nếu B về sau dùng RC, các lệnh đó tự dùng S guard. Không thêm thư viện hoặc cấu hình chung.
- Hai file mới cần bổ sung vào danh sách file: `internal/allocation/config/AllocationCurrentReadPool.java` quản lý pool chỉ đọc riêng, dùng cấu hình/tài khoản DataSource chính và đóng khi context hủy; `internal/allocation/persistence/CurrentAllocationOverlapStore.java` dùng JDBC trực tiếp/try-with-resources đọc view RC, không bind connection phụ vào parent. Chỉ dùng trước lần ghi allocation đầu tiên của COMPLETE_ADMISSION, không dùng cho luồng cần nhìn thấy allocation chưa commit trong parent. Mọi lệnh ghi/audit/event giữ trong parent.
- `BR_M03_01_AllocationConditionsIT`: fixture deadline đổi sang một Instant duy nhất đã truncate microsecond, dùng cùng giá trị cho DB và assertion. Lỗi cũ do nhiều lần gọi time.now() cùng với DATETIME(6) làm tròn, không nới điều kiện production.
- `FR_024_AllocationStatusParticipantIT`: thêm ca đính chính seq2 commit sau snapshot (RED bỏ sót vi phạm), ca 5 transaction giữ toàn bộ 5 connection chính, và hai lệnh đổi sang hai giường mới cùng supremum gap. Ca hai tiếp nhận được đồng bộ ở đầu transaction trước khóa M03 thay vì bắt buộc cả hai cùng giữ bed lock, vì RR guard mới chủ động tuần tự hóa; vẫn yêu cầu cả hai hoàn tất và giữ đúng số ACTIVE. Không bỏ, skip hoặc xóa test.
- `003-m01-write-guard-green.log`: 30 IT (7 điều kiện +1 mười luồng phân bổ +22 participant) và 25 ArchTest xanh, BUILD SUCCESS 51.835 giây. Ca 5 connection và supremum replacement đã chạy qua trong lượt này. Chỉnh tiếp id fixture supremum sang vùng cao riêng để tránh va chạm TestUsers ở các test chạy sau; được kiểm lại trong clean verify cuối.
- Reviewer chỉ đọc không thấy blocker thứ tự khóa ở mode guard hiện tại; RC S/RR X không nâng khóa qua các lần gọi lặp, read pool không lấy write guard. Kết luận áp dụng source hiện tại, M04 chưa ghi allocation trước participant. Không coi review thay cho test.
- Đã chạy spotless:apply thành công; đang chạy nền clean verify lần cuối với log `003-m01-clean-verify-final3.log`. Kết quả sẽ ghi nối tiếp, chưa dùng dự đoán 14 lỗi M01 làm kết quả thật.

**Kết quả kiểm cuối đã kết thúc (2026-10-24)**

- Lệnh nền `backend/mvnw.cmd clean verify`: exit 1 / BUILD FAILURE, tổng thời gian 03:41 phút, log `C:/Users/nguye/AppData/Local/Temp/003-m01-clean-verify-final3.log`. Unit/kiến trúc: 392 test qua. Tích hợp: 993 test, 5 failure +9 error, không skip. Không ghi là toàn kho xanh.
- Trong chính lượt clean verify này, M03 có 148 IT /31 lớp, 0 failure/error/skip. Riêng FR_024_AllocationStatusParticipantIT có 22 ca qua, gồm rollback M01/M03, đính chính commit sau snapshot, hai tiếp nhận cùng phòng, 5 transaction chiếm đủ connection pool lệnh, hai replacement vào giường trống cùng supremum gap. Không chỉ suy từ kiểm hẹp.
- 14 lỗi còn lại thuộc đúng bảy lớp M01: BR_M01_01_TemporaryAbsenceOverdueIT (1 error), BR_M01_02_ReassessmentNoticeIT (2 error), CompleteAdmissionConsentIT (2 error), CompleteAdmissionWishWarningsIT (3 error), FR_034a_CompleteAdmissionIT (4 failure), ResidentContractIT (1 failure), StatusHistoryIT (1 error). Các fixture trạng thái đang nạp hai FakeStatusTransitionParticipant nhưng vẫn nạp thêm participant thật M03, không có nguồn/giường M03 cho các ca cũ. Contract test gom theo prefix /residents nên nhận nhầm GET /residents/{residentId}/location của 003 vào tập 001. B cần cập nhật cô lập fixture và bộ lọc contract; không phải lý do để nới điều kiện production. Chủ dự án đã chọn giữ M01 cho B, nên không sửa bảy lớp này và không tắt/skip test.
- Lệnh hẹp cuối trước clean verify: `mvnw.cmd spotless:apply test -Dtest=*ArchTest,ModuleBoundaryTest failsafe:integration-test failsafe:verify -Dit.test=FR_024_AllocationStatusParticipantIT,BR_M03_01_AllocationConditionsIT,FR_020_AllocationConcurrencyIT`: 25 ArchTest +30 IT qua, BUILD SUCCESS, log 003-m01-write-guard-green.log. `spotless:apply` chạy thêm sau chỉnh id fixture trước clean verify cuối và qua.
- Frontend lần cuối: `npm test` 232 test /33 file qua; `npm run lint` và `npm run build` exit 0. Build còn cảnh báo chunk lớn hơn 500 kB. Java thay đổi sau đó không sửa frontend. `git diff --check` các file thuộc lượt này qua; không còn unmerged entry, merge vẫn chưa commit. Không commit, push, đổi nhánh hoặc tiếp tục merge.
- Giữ nguyên các checkbox đã có, không đánh thêm [x] vì clean verify toàn kho còn đỏ. Phase 3–6 chưa được ghi hoàn tất. Phần đã cài được ghi rõ trong plan; không suy rằng T029–T036 đã xong vì listener M01 đã chạy.
- Những việc vẫn chờ: B cập nhật fixture/contract M01 nêu trên và cài ResidentStatusApi.statusPeriods đang stub cho kiểm nhập bù; B (004) cung cấp adapter/sự kiện lưu trú; D (007) adapter lây nhiễm; cổng tài sản/đổi phòng giường còn theo quyết định tạm hoãn trước; A bổ sung enum hai job cho clock test 000 theo lựa chọn chủ dự án. Không tự khai báo thay các chủ module.

- Thứ tự task chưa đánh trong Phase 1–6, lấy trực tiếp tasks.md tại thời điểm kết thúc lượt: T012 → T015 → T017 → T019 → T020 → T021 → T025 → T026 → T027 → T028 → T029 → T030 → T031 → T032 → T033 → T034 → T035 → T036 → T038 → T039 → T040 → T042 → T043 → T044. Task đầu tiên còn phải nghiệm thu là T012; không nhảy qua hoặc đánh thay công việc chưa hoàn tất. Các task chuyển giường T029–T036 vẫn tiếp tục theo phụ thuộc, phần nhập bù T031/T034 chờ statusPeriods của B.


## 2026-10-24 — Spec 003 Phase 7, T045–T048

### Phạm vi, yêu cầu và các phê duyệt thực tế

- HungNX-nguyen là C; đầu lượt đã báo các dòng giao C ở team-plan mục 8. Đọc tasks/ghi chú plan trước, tuân thủ backend/README và frontend/README; tra G-10, FR-019/026/035–037 và ma trận quyền khi cần.
- Chủ dự án duyệt dịch riêng năm HTTP operation Phase 7. Sau đó chủ dự án chọn **2, giữ nguyên contract**, không thêm pendingBackfill cho BedLocationHistoryRow. Không diễn giải lựa chọn này thành duyệt bổ sung trường.
- Giữ quyết định trước: source/test M01 do B xử lý; không sửa isolation M01; không thêm hai giá trị job vào API clock 000; không commit/push/đổi nhánh/tiếp tục merge. Không thêm thư viện hoặc migration. Lượt này không xóa file.
- T045 viết test trước; T046 API dữ liệu và cổng HTTP có quyền; T047 hai provider đính chính; T048 năm operation và UI trong features/beds. Các task chưa được đánh [x] khi kiểm cuối chưa kết thúc hoặc còn yêu cầu chưa đạt.

### Trình tự và chứng cứ RED → GREEN

1. Viết FR_035_LocationHistoryIT trước bản cài. Lượt đầu thiếu TEST_DB_ADMIN_PASSWORD do biến User không được kế thừa; đây là lỗi môi trường, không tính RED nghiệp vụ. Nạp cấu hình test đã được người dùng cung cấp vào riêng process, không ghi giá trị bí mật vào repository.
2. Lượt history-red2 chạy được MySQL, 3 assertion đỏ vì không có bean LocationHistoryApi. Cài truy vấn view, các projection và service. Sửa fixture enum VOID thay VOID_RECORD; không dùng lỗi fixture làm bằng chứng nghiệp vụ. Lượt hẹp tiếp theo 4 history IT +25 ArchTest xanh.
3. Viết provider IT, thấy thiếu M03.BED_ALLOCATION và M03.BED_READINESS_CONFIRMATION. Cài hai provider, giữ nguyên bản gốc, khóa/phát event trong transaction correction. Lượt correction-green 4 IT và ArchTest xanh.
4. Viết HTTP IT, 3 ca RED 404 ở endpoint chưa cài. Cài đúng năm operation đã dịch, đăng ký contract và sinh client. Test bổ sung ready-history ban đầu dùng sai enum fixture PENDING_READY; sửa thành AWAITING_READY. RED nghiệp vụ sau đó tái hiện ghi chú vẫn là Gốc sau khi correction thành công; sửa BedController đọc đúng record type và áp nội dung hiện hành. ApiContractIT đã so khớp xanh.
5. Viết UI test trước component; thiếu component tạo RED. Test tiếp bắt lỗi nút Base UI mặc định không submit; thêm type=submit. Đã kiểm hiển thị giờ Việt Nam, dấu correction/pending, khoảng vắng và xóa dữ liệu cũ khi 403. Tab history-only và form typed có test riêng trước cài.
6. Test biên history-edge-red: API nội bộ chặn điều dưỡng gọi cho 007; pending bị bỏ sót khi transferAt trước from; source M01 stub ném lỗi kỹ thuật. Tách reader API nguồn khỏi quyền HTTP, xét giao khoảng pending và trả SOURCE_UNAVAILABLE thay vì giả khoảng vắng rỗng. history-edge-green có 12 IT xanh, gồm SC-003.
7. Test quyền HTTP dùng spy PermissionApi tái hiện role key còn tồn tại nhưng tài khoản bị từ chối: 200 thay vì403. Sửa kiểm quyền thực tế và hợp các historyFilter hợp lệ; permission-green xanh. Không chỉ dựa vào danh sách quyền vai trò.
8. Final fresh-context review chỉ đọc phát hiện thiếu overlap cùng người khác giường và khoảng vắng HTTP vượt đoạn cùng phòng. Bổ sung RED thực tế cho cả hai. Đồng thời RED tái hiện before event RR lấy giá trị gốc thay amendment vừa commit, và payload generic chấp nhận timestamp không phù hợp cách CAST của view. review-red có 4 failure; sửa rồi review-green có 22 IT (21 Phase 7 +1 contract) và25 ArchTest xanh.
9. Bổ sung RED thu hẹp quyền CREATE trên correction cách A; sửa provider dùng hasPermission và kiểm lại tầng bản gốc cho cách A/C. scoped-final có **23 IT (22 Phase 7 +1 ApiContractIT) và25 ArchTest xanh**, không skip.
10. UI bổ sung RED chặn form ACTIVE và mở form từ CANCELLED; sửa bằng GET chi tiết trước form và nút trên bảng phân bổ. Hai file UI có5 test xanh. Frontend toàn bộ trước hai ca bổ sung có237 test xanh; đang chạy lại đầy đủ sau sửa cuối.
11. Đã khởi chạy nền backend/mvnw.cmd clean verify, log 003-p7-clean-verify-final.log. Chưa ghi kết quả dự đoán thành kết quả thật; kết quả cuối sẽ bổ sung khi process kết thúc.

### File tạo trong lượt Phase 7

Các file dưới đây là delta Phase 7 trên checkout đang có thay đổi Phase 1–6/merge chưa commit; không nhận các thay đổi baseline đó là việc mới của lượt này.

| File | Mục đích |
| --- | --- |
| `backend/src/main/java/com/cambae/backend/m03/internal/history/domain/HistoryStay.java` | Projection vị trí đã áp đính chính, gồm allocationId và khoảng ở. |
| `backend/src/main/java/com/cambae/backend/m03/internal/history/domain/HistoryAbsence.java` | Projection khoảng vắng tối thiểu, phân biệt residentId và loại trạng thái. |
| `backend/src/main/java/com/cambae/backend/m03/internal/history/persistence/LocationHistoryStore.java` | SQL theo người/vị trí/cùng phòng trên v_current_allocation; truy chỉ mục yêu cầu nhập bù và danh mục giường trống. |
| `backend/src/main/java/com/cambae/backend/m03/internal/history/service/LocationHistoryReading.java` | Cài LocationHistoryApi cho module 007/016 đọc dữ liệu nguồn; cache theo người trong mỗi lần đọc. |
| `backend/src/main/java/com/cambae/backend/m03/internal/history/service/LocationHistoryQueries.java` | Cổng đọc HTTP có quyền/phạm vi; lấy tên và khoảng vắng qua API M01, trạng thái yêu cầu qua API 000. |
| `backend/src/main/java/com/cambae/backend/m03/internal/history/service/HistoryCorrections.java` | Facade transaction RC cho hai lệnh HTTP đính chính, kiểm bed/history và chuẩn hóa UTC microsecond. |
| `backend/src/main/java/com/cambae/backend/m03/internal/allocation/domain/AllocationCorrectionOriginal.java` | Projection bản gốc phân bổ bất biến và tầng giường gốc. |
| `backend/src/main/java/com/cambae/backend/m03/internal/allocation/domain/AllocationCorrectionSnapshot.java` | Projection correction đã commit từ public view 000 cho before event của parent RR. |
| `backend/src/main/java/com/cambae/backend/m03/internal/allocation/persistence/AllocationCorrectionStore.java` | Đọc bản gốc; kiểm overlap cả bed/resident bằng view dưới khóa; đọc public v_latest_correction qua pool RC khi cần. |
| `backend/src/main/java/com/cambae/backend/m03/internal/allocation/service/AllocationCorrectionProvider.java` | Provider M03.BED_ALLOCATION: CLOSED/CANCELLED, quyền ghi thực tế, whitelist, kiểm FR-019 và phát BedAllocationCorrected. |
| `backend/src/main/java/com/cambae/backend/m03/internal/bed/domain/ReadinessCorrectionOriginal.java` | Projection xác nhận sẵn sàng gốc, actor và floor_area_id lúc ghi. |
| `backend/src/main/java/com/cambae/backend/m03/internal/bed/persistence/ReadinessCorrectionStore.java` | Đọc lịch sử giường gốc theo id, không UPDATE lịch sử. |
| `backend/src/main/java/com/cambae/backend/m03/internal/bed/service/ReadinessCorrectionProvider.java` | Provider M03.BED_READINESS_CONFIRMATION cho CONFIRM_READY và ba trường note/disinfected/ppeUsed. |
| `backend/src/main/java/com/cambae/backend/m03/web/LocationHistoryController.java` | Ba GET lịch sử và hai POST correction đã duyệt; gom nhóm HTTP và giao khoảng vắng với từng khoảng cùng phòng. |
| `backend/src/main/java/com/cambae/backend/m03/web/request/RecordCorrection.java` | Request type/correctedContent/reason, validation lý do và enum chuẩn 000. |
| `backend/src/main/java/com/cambae/backend/m03/web/response/ResidentLocationHistoryRow.java` | Schema HTTP theo người, có allocationId và hai dấu correction/pending. |
| `backend/src/main/java/com/cambae/backend/m03/web/response/BedLocationHistoryRow.java` | Schema HTTP mỗi giường và danh sách người, giữ nguyên thiếu pendingBackfill theo quyết định chủ dự án. |
| `backend/src/main/java/com/cambae/backend/m03/web/response/BedResidentHistoryRow.java` | Người ở giường và khoảng thời gian, corrected. |
| `backend/src/main/java/com/cambae/backend/m03/web/response/RoommateHistoryRow.java` | Người/phòng, danh sách overlaps/absences và hai dấu. |
| `backend/src/main/java/com/cambae/backend/m03/web/response/HistoryInterval.java` | DTO khoảng nửa mở cho overlaps. |
| `backend/src/main/java/com/cambae/backend/m03/web/response/HistoryAbsenceResponse.java` | DTO khoảng vắng với residentId/type/from/to, không chứa nội dung sức khỏe. |
| `backend/src/test/java/com/cambae/backend/m03/FR_035_LocationHistoryIT.java` | 7 IT API, half-open/correction/void, ngày Việt Nam, M01 spy, pending, người gọi 007 và SC-003. |
| `backend/src/test/java/com/cambae/backend/m03/BR_M03_24_AllocationCorrectionIT.java` | 8 IT provider/event, bất biến, overlap bed/resident, RR before, canonical payload và thu hẹp quyền. |
| `backend/src/test/java/com/cambae/backend/m03/FR_035_LocationHistoryHttpIT.java` | 7 IT hình dạng HTTP, giường trống, lựa chọn query, ready correction, nguồn M01 thiếu, quyền bị từ chối và clip khoảng vắng. |
| `frontend/src/features/beds/LocationHistoryPanel.jsx` | Tab tra cứu ba dạng, chọn hồ sơ, bộ lọc, kết quả/dấu/vắng, xem chuỗi correction và mở form riêng. |
| `frontend/src/features/beds/LocationHistoryPanel.test.jsx` | 3 test tra cứu/giờ Việt Nam, 403 bỏ kết quả cũ, phân biệt khoảng vắng của A/người kia. |
| `frontend/src/features/beds/AllocationCorrectionForm.jsx` | Form typed cho correction phân bổ; đọc trạng thái trước khi hiện, idempotency key, lý do và ngày giờ +07:00. |
| `frontend/src/features/beds/AllocationCorrectionForm.test.jsx` | 2 test payload số/thời gian/idempotency và chặn form cho ACTIVE. |

### File sửa trong lượt Phase 7

| File | Mục đích của phần sửa mới |
| --- | --- |
| `backend/src/main/java/com/cambae/backend/m03/web/BedController.java` | Đọc amendment đúng loại M03.BED_READINESS_CONFIRMATION, áp note/disinfected/ppeUsed và dấu corrected khi trả state-history. |
| `backend/src/test/java/com/cambae/backend/common/ApiContractIT.java` | Chỉ phần đăng ký 003: bỏ đúng năm pending đã có controller; không tắt so operation hay bỏ test. Theo dòng A review/C trong team-plan. |
| `frontend/src/features/beds/BedsPage.jsx` | Thêm tab lịch sử theo quyền VIEW BED_ALLOCATION hoặc CREATE INFECTION_CONTAINMENT, kể cả người chỉ có quyền lịch sử. |
| `frontend/src/features/beds/BedsPage.test.jsx` | Thêm ca mở lịch sử khi chỉ có quyền khoanh vùng, không lộ tab cơ cấu. |
| `frontend/src/features/beds/BedAllocationsPanel.jsx` | Nút và form correction cho CLOSED/CANCELLED khi có quyền ghi, để đính chính cả bản hủy không thuộc lịch sử vị trí. |
| `frontend/src/features/beds/BedAllocationsPanel.test.jsx` | Thêm ca mở correction từ bản CANCELLED. |
| `frontend/src/api/api003.js` | Sinh lại bằng npm run gen:api: thêm năm hàm, không sửa tay; các client khác không có diff nội dung. |
| `specs/003-room-bed-allocation/contracts/api-003.yaml` | Bản dịch năm operation/schema/request Phase 7 đã duyệt; giữ schema giường không thêm pending theo lựa chọn 2. |
| `specs/003-room-bed-allocation/plan.md` | Ghi quyết định phân lớp, nguồn, quyền API/HTTP, SPI callback/khóa/RR, UI và các giới hạn kiểm cuối. |
| `specs/003-room-bed-allocation/tasks.md` | Ghi trạng thái đang kiểm, các phụ thuộc; chưa thay checkbox Phase 7. |
| `specs/000-shared-foundation/glossary.md` | Chỉ mục 9 của 003: bổ sung tên lớp/schema/endpoint/UI Phase 7. |
| `docs/team-plan.md` | Mở rộng bàn giao statusPeriods cho T046/FR-036 và đăng ký năm operation 003 cho A review. |
| `Working-history.md` | Bổ sung lịch sử lượt Phase 7 và kết quả xác minh thực tế, giữ nguyên phần cũ. |

### Quan hệ file và luồng xử lý dữ liệu

- Web gọi client api003 sinh từ hợp đồng. Ba GET đi LocationHistoryController → LocationHistoryQueries (quyền và phạm vi) → LocationHistoryStore → v_current_allocation. Nhãn người lấy ResidentApi.identity; trạng thái vắng lấy ResidentStatusApi.statusPeriods; dấu yêu cầu lấy chỉ mục M03 rồi ApprovalRequestApi.find. Không đọc bảng M01 hoặc approval_request trong production M03. Phản hồi chỉ có nhận dạng/vị trí/thời gian/trạng thái vắng tối thiểu.
- LocationHistoryReading cài API nguồn cho 007/016; module gọi chịu trách nhiệm quyền của chức năng đó như ResidentLocationApi đã có. HTTP M03 không gọi reader không lọc này. Điều dưỡng chỉ truy trực tiếp qua chức năng 007; HTTP M03 yêu cầu BED_ALLOCATION VIEW hoặc INFECTION_CONTAINMENT CREATE và lọc historyFilter/hasPermission.
- Tại thời điểm dùng start<=at<effective_end; ngày dùng BusinessTime [00:00D,00:00D+1), giờ Việt Nam. Roommates tự nối view theo phòng, dùng GREATEST/LEAST; HTTP giao từng khoảng vắng của A/người kia với các overlap được phép trước khi trả. Nguồn thiếu trả503, không tạo lịch sử hoặc khoảng vắng thay thế.
- Hai POST → HistoryCorrections → CorrectionApi.create của000 → provider sở hữu M03. validateContent kiểm whitelist; afterCorrection phân bổ khóa qua AllocationLocks rồi kiểm cả bed và resident trên view, lỗi rollback bản correction/audit và không phát BedAllocationCorrected. Provider ready không đổi máy trạng thái; BedController áp amendment khi đọc lịch sử.
- Form AllocationCorrectionForm lấy chi tiết trạng thái, chỉ hiện cho CLOSED/CANCELLED, gửi số bedId và thời gian +07:00. Facade chuẩn hóa thành UTC tới microsecond. CorrectionHistorySheet chung xem chain qua API000, không sửa component chung hoặc gọi fetch tự viết.

### Quyết định kỹ thuật quan trọng và giới hạn

- SPI validateContent không có id: kiểm phụ thuộc bản gốc trong afterCorrection cùng transaction; phương án này không kiểm được đầy đủ overlap ngay khi tạo yêu cầu cách C, nhưng kiểm lại lúc áp dụng và rollback nếu sai. Không mở rộng SPI củaA chưa được duyệt.
- Giữ guard→position→room→bed theo plan trước. HTTP correction RC đọc view trong parent. Parent RR dùng poolRC cho allocation **khác id** đã commit; payload của chính id vừa sửa tính trong parent. Luồng correction không được dùng như một bước sau các ghi allocation khác chưa commit của cùng parent mà vẫn kỳ vọng connection độc lập thấy các ghi đó.
- Before event RR chọn amendment mới nhất giữa chain thấy trong parent (gồm correction chưa commit của chính parent) và public v_latest_correction trên poolRC (gồm correction commit sau snapshot cũ). Chuỗi đã được000 khóa; trong một chuỗi, id correction tăng theo thứ tự append. Không tự chép thuật toán chọn latest trong truy vấn lịch sử.
- Generic correctedContent của M03 cần thời gian UTC Z tới microsecond trong miền DATETIME, để khớp view đã có. HTTP nhận ISO offset và chuẩn hóa, không sửa migration đã merge hoặc nới API000. Ngưỡng này là giới hạn kiểu dữ liệu kỹ thuật, không phải giá trị CFG nghiệp vụ.
- SC-003 test dữ liệu30ngày/10người/3phòng, năm mốc hỏi và roommates5ngày, xác minh tổng lượt đọc dưới1giây sau làm nóng trong môi trường test; không xem là benchmark production.
- Không chụp màn hình trong lượt này. UI đã kiểm bằng Vitest/jsdom và build/lint; chưa ghi nhận kiểm trực quan đủ ba bề rộng/chế độ tối trên browser. Không giả kết quả kiểm trực quan.

### Lệnh và log kiểm thực tế (thư mục C:/Users/nguye/AppData/Local/Temp/)

- Backend dùng JDK25 và MySQL8.0; process nhận TEST_DB_URL/TEST_DB_ADMIN_USER/TEST_DB_ADMIN_PASSWORD, không lưu bí mật trong kho.
- Cấu trúc lệnh hẹp: `mvnw.cmd [spotless:apply] test -Dtest=*ArchTest,ModuleBoundaryTest failsafe:integration-test failsafe:verify -Dit.test=<lớp của task>`.
- Các log RED/cấu hình/fixture: 003-p7-history-red.log (môi trường); history-red2; correction-red; http-red; http-green1 (fixture enum sai); http-green2 (quyền fixture); readiness-red (ghi chú gốc); history-edge-red; permission-red; review-red; correction-permission-red. Tiền tố chung `003-p7-`, hậu tố `.log`. Không coi các lượt có chữ green trong tên là xanh nếu process trả lỗi.
- Các log xanh đã đọc: correction-green; http-green3 (4 HTTP +1 contract); history-edge-green (12 IT); permission-green (6 HTTP); review-green (22 IT); scoped-final (23 IT, trong đó22 Phase7), đều có25 ArchTest. Tên lớp và tổng số lấy từ report/log thật.
- Frontend: `npm run gen:api`; `npx prettier --write` chỉ file beds sửa mới; `npm test -- <file của task>`; các RED ui-red/ui-tab-red/ui-correction-guard-red; các lượt xanh ui-green2/ui-green3/ui-correction-guard-green. `npm test` trước sửa UI cuối:237 test/35file xanh; lint/build xanh, build còn cảnh báo chunk>500kB. Đang chờ lượt kiểm đầy đủ cuối và clean verify nền.

### Việc còn tồn tại và thứ tự tiếp theo

- B cần cài ResidentStatusApi.statusPeriods thật cho FR-036; M03 đã nối API và kiểm output hợp lệ bằng spy, không đồng nghĩa nguồn đã chạy. Giữ phạm vi source/test M01 choB.
- Chủ dự án giữ nguyên contract lịch sử theo giường: chưa có trường để trả dấu nhập bù FR-027 ở BedLocationHistoryRow, nhất là giường đích trống. Không tự thêm trường, không ghi T048 đã đạt đầy đủ yêu cầu này.
- Lỗi clean verify của M01 ở lượt trước đã bàn giaoB; chưa khẳng định còn bao nhiêu lỗi trong lượt mới trước khi lệnh kết thúc.
- Trong Phase7, thứ tự nghiệm thu vẫn **T045 → T046 → T047 → T048**, chưa đánh [x]. Nếu đủ các điều kiện và kiểm toàn kho xanh, phase kế tiếp bắt đầu **T049 → T050 → T051 → T052** (Phase8 bán trú). Các task cũ chưa đánh trong Phase3–6 vẫn giữ nguyên và xem phần lịch sử lượt trước, không nhảy qua hoặc tự đánh thay.


### Kiểm cuối lần 1 và sửa tác động fixture (đã xác minh)

- Frontend cuối: `npm test` **239 test/35 file xanh**, `npm run lint` và `npm run build` exit0. Các log: 003-p7-frontend-tests-final.log, 003-p7-lint-final2.log, 003-p7-build-final2.log. Build vẫn cảnh báo chunk>500kB; không sửa cấu hình chunk ngoài phạm vi.
- `mvnw.cmd clean verify` nền lần1 kết thúc **exit1/BUILD FAILURE,04:50phút** (003-p7-clean-verify-final.log). Toàn bộ **170 IT M03/34class xanh**; tổng1015IT có6failure+9error, không skip. Trong đó14 lỗi ở7class M01 đã biết từ lượt trước; lỗi thứ15 ở ApprovalLifecycleIT.US5_1_create_isDraft_withRequesterTypeContentDateReason.
- Lỗi mới này do **fixture Phase7 của M03**, không quy choA: FR_027_pendingRequestBeforeQueryStartStillMarksAffectedInterval INSERT approval_request với id từ TestUsers.nextId (~5triệu), làm mãYC-2026-5002837 dài7chữsố thay regex6chữsố của000. AUTO_INCREMENT chịu ảnh hưởng dù bản fixture rollback. Đã sửa riêng testM03: để DB tự sinh id, lấy LAST_INSERT_ID trên connection đang gắn transaction test, rồi dùng id đó cho chỉ mụcM03. Không sửa test/production củaA hoặc nới regex để xanh.
- Đang chạy kiểm hẹp FR_035_LocationHistoryIT + ApprovalLifecycleIT (test bị ảnh hưởng) +ArchTest; sau khi xanh phải chạy clean verify nền lại. Không dùng kết quả kiểm lần1 làm kết quả của source sau sửa fixture.


### Tiếp tục kiểm cuối sau lỗi cơ chế duyệt

- Kiểm fixture sau sửa đã kết thúc: `003-p7-fixture-id-green.log`, exit0/BUILD SUCCESS,48.690giây,14IT (7history+7ApprovalLifecycleIT) và25ArchTest xanh.
- Lần gọi khởi chạy clean verify lần2 trước đó bị automatic approval review từ chối do hết hạn mức duyệt. Lệnh **chưa thực thi**, không tạo log final2; không đổi sang cơ chế khác để bỏ qua duyệt.
- Sau khi chủ dự án yêu cầu tiếp tục, gọi lại qua cùng cơ chế duyệt và đã khởi chạy nền `mvnw.cmd clean verify`; log `C:/Users/nguye/AppData/Local/Temp/003-p7-clean-verify-final2.log`. Chờ process kết thúc trước khi ghi kết quả hoặc đánh checkbox.


### Kết quả kiểm cuối lần 2 — đã kết thúc

- `backend/mvnw.cmd clean verify` chạy nền qua cơ chế duyệt, **exit1/BUILD FAILURE,04:09phút**, log `C:/Users/nguye/AppData/Local/Temp/003-p7-clean-verify-final2.log`.
- **392 test đơn vị/kiến trúc xanh**. Tổng **1015 IT:5failure+9error,0skip**. Toàn bộ **170 IT M03/34class xanh**, gồm22IT Phase7. `ApiContractIT` xanh; `ApprovalLifecycleIT` **7test xanh**, xác nhận lỗi fixture đẩy ID lớn đã hết trong toàn bộ bộ test.
- 14 lỗi còn lại chỉ ở7class M01 dưới đây. Không sửa source/test M01 theo quyết định giữ phần này choB; các nguyên nhân fixture participant/contract đã bàn giao ở team-plan mục8 và lịch sử lượt trước. Không bỏ test hoặc nới production M03 để làm fixture cũ xanh.

| Class M01 | Failure | Error |
| --- | ---: | ---: |
| `BR_M01_01_TemporaryAbsenceOverdueIT` | 0 | 1 |
| `BR_M01_02_ReassessmentNoticeIT` | 0 | 2 |
| `CompleteAdmissionConsentIT` | 0 | 2 |
| `CompleteAdmissionWishWarningsIT` | 0 | 3 |
| `FR_034a_CompleteAdmissionIT` | 4 | 0 |
| `ResidentContractIT` | 1 | 0 |
| `StatusHistoryIT` | 0 | 1 |

- Frontend không đổi sau lượt239test/35file xanh và lint/build exit0; giữ cảnh báo chunk lớn. Kiểm trực quan browser ba bề rộng/chế độ tối chưa thực hiện, không ghi nhận đạt.
- **Chưa nghiệm thu Phase7, không đánh [x] T045–T048**: clean verify toàn kho còn14lỗiM01; nguồn `ResidentStatusApi.statusPeriods` thật vẫn stub và chặn FR-036; HTTP theo giường chưa thể trả dấu pendingBackfill FR-027 do chủ dự án chọn giữ nguyên schema. Phần đã triển khai và giới hạn được ghi riêng, không gọi các phụ thuộc chưa xong là hoàn tất.
- Thứ tự tiếp theo: xử lý các điều kiện nghiệm thu **T045 → T046 → T047 → T048**; chỉ sau đó chuyển **T049 → T050 → T051 → T052** của Phase8 theo tasks.md. Phần M01 cần B xử lý; không tự mở rộng phạm vi được phép.
- `git diff --check` đã qua; không có unmerged entry. Không có diff mới trong M01 source/test, common production, frontend common/components, pom.xml, package.json hoặc CLAUDE.md. Không commit, push, đổi nhánh hay tiếp tục merge. Các file cũ/đã staged vẫn được giữ nguyên; Working-history.md bổ sung nối tiếp, không xóa lịch sử trước.

## 2026-10-24 — Spec 003 Phase 8, T049–T052 (đang triển khai, chưa nghiệm thu)

### Yêu cầu, phạm vi và quyết định đã được duyệt

- HungNX-nguyen (C) yêu cầu làm Phase 8 theo AGENTS.md, CLAUDE.md, docs/team-plan.md; đầu phiên đã báo các dòng tên C trong mục 8. Đã đọc tasks.md và ghi chú triển khai plan.md trước khi tra các mục FR-038–FR-042, research G-11, data-model và contract cần dùng.
- Chủ dự án trả lời “1” để duyệt dịch riêng 9 operation T052 sang tiếng Anh: năm operation `/day-care-rest-areas` và bốn operation `/day-care-sessions`, cùng schema/request/response. Không mở rộng duyệt sang M01, API đồng hồ 000, trường pendingBackfill Phase 7 hoặc thư viện mới.
- Tiếp tục trên checkout hiện có; không commit, push, đổi nhánh, stage, hoàn tất merge hoặc xóa thay đổi cũ. Giữ nội dung lịch sử trước đó.
- Hai đề nghị API mới đã gửi và đang chờ câu trả lời: thêm version trong response khu nghỉ/buổi để UI có If-Match từng mục; bổ sung danh sách residentId có lịch tương lai vào DayCareArrivalCounter (SPI hiện chỉ có boolean). Chưa cài hai thay đổi này. UI hiện cài xem/tạo/đếm; chưa có lệnh sửa/ngừng hiệu lực từ UI. Backend chặn ngừng khu nghỉ bằng boolean, chưa liệt kê người có lịch. Không giả phiên bản 0 hoặc bịa danh sách người.

### Trình tự triển khai và xác minh thực tế

1. Viết `FR_038_DayCareSchemaIT`, chạy RED do thiếu hai bảng. Thêm migration V003_05 với ràng buộc khu nghỉ duy nhất Hiệu lực, vị trí XOR, tên buổi Hiệu lực duy nhất và giờ bắt đầu/kết thúc trong ngày.
2. Lần kiểm schema tiếp theo thất bại vì assertion kỳ vọng DataIntegrityViolationException cho CHECK; MySQL 8 trả UncategorizedSQLException (error3819). Sửa đúng assertion thông báo constraint, giữ nguyên việc kiểm lỗi. Ba test schema xanh trong `003-p8-capacity-red3.log`.
3. Viết BR_M03_03_DayCareCapacityIT. Hai lần chạy đầu testCompile lỗi vì ParameterCode là enum, không phải value object; đã sửa test dùng CFG_M03_01. Lượt RED thực tế `003-p8-capacity-red3.log` có bốn failure do chưa có bean DayCareCapacityApi. Sau đó cài DayCareCapacityService, Store và các ảnh đọc.
4. Lượt capacity đầu sau implementation lỗi MySQL vì tài khoản ứng dụng chưa có UPDATE trên guard để dùng SELECT FOR UPDATE. Bổ sung grant trong migration mới. `003-p8-http-red.log`: bốn capacity test xanh, bốn HTTP test mới RED 404 đúng nguyên nhân chưa có controller.
5. Cài hai entity CatalogBase, service cấu hình, đếm lại qua SPI, controller/request/response. `003-p8-http-green.log`: bốn HTTP test xanh và ArchTests xanh. Kiểm khu nghỉ thứ hai, overlap (cho phép hai khung tiếp giáp), đổi giờ đếm lại/cảnh báo, không hủy lịch, chặn khi còn lịch tương lai, If-Match cũ và quyền đọc/ghi.
6. Viết test ParameterChanged RED do thiếu DayCareParameterListener; thêm listener qua ApplicationModuleListener. `003-p8-parameter-green.log`: một test xanh. Test gọi target listener trong transaction fixture để kiểm điều phối/recount, không tuyên bố đã kiểm toàn bộ chuỗi async/outbox sau commit.
7. Bổ sung mode SCHEDULER vào test: `003-p8-parameter-system-red.log` RED vì audit thiếu căn cứ FR-045. Bổ sung Basis BR-M03-25 vào cảnh báo vượt sức chứa. Test này xanh trong lượt `003-p8-system-concurrency-green.log`; toàn lệnh lượt đó vẫn đỏ do test concurrency bên dưới.
8. Bổ sung kiểm hai transaction tạo buổi chồng giờ đồng thời. Tái hiện lỗi ánh xạ JPA LocalTime qua timezone UTC làm khung 07:00–12:00 vi phạm CHECK. Sửa bằng JdbcTypeCode(SqlTypes.LOCAL_TIME) trên hai trường entity và ResultSet.getObject(LocalTime.class), giữ nguyên giờ buổi, không đổi cấu hình timezone dùng chung.
9. Viết hồi quy REPEATABLE_READ: transaction đã tạo snapshot, transaction khác sửa giờ rồi commit, lock thấy khung mới nhưng occupiedSessions đọc khung cũ. `003-p8-frames-red.log` RED đúng danh sách buổi cũ. Đổi occupiedSessions sang transaction có guard và current locking read.
10. Reviewer độc lập phát hiện thêm thiếu guard vị trí cha. `003-p8-parent-red.log` tái hiện ngừng tầng thành công dù còn khu nghỉ ACTIVE. Thêm khu nghỉ vào activeChildren và đồng bộ guard trước khóa vị trí khi ngừng/xóa AREA/FLOOR; bổ sung test race dời khu nghỉ/ngừng tầng. Reviewer rà lại hai fix không thấy blocker mới; kết quả runtime vẫn phải dựa vào lệnh kiểm tiếp theo.
11. Dịch đúng nhóm contract đã duyệt, giữ các operation phase khác; bỏ đúng chín pending của 003 trong ApiContractIT, sinh lại client bằng npm run gen:api. `003-p8-contract-check.log`: ApiContractIT và ArchTests xanh, không tắt check hợp đồng.
12. UI: test RED thiếu DayCarePanel trước, sau đó cài xem/tạo/đếm/cảnh báo; test tab RED thiếu tab Bán trú trước khi gắn vào BedsPage. Lượt xanh `003-p8-ui-scoped.log`: 12 test/2 file; npm run lint và npm run build qua. Build còn cảnh báo chunk >500 kB. Test đếm ban đầu bắt được Button mặc định type=button nên không submit; sửa type=submit và chạy xanh.

### Danh sách file và mục đích (thay đổi mới của lượt Phase 8)

| File | Loại, mục đích |
| --- | --- |
| backend/src/main/resources/db/migration/V003_05__day_care_capacity.sql | Tạo: hai bảng danh mục, generated unique, CHECK/FK nội bộ, guard kỹ thuật và grant tối thiểu cho app |
| backend/src/main/java/com/cambae/backend/m03/internal/daycare/domain/DayCareRestArea.java | Tạo: entity CatalogBase cho khu nghỉ |
| backend/src/main/java/com/cambae/backend/m03/internal/daycare/domain/DayCareSessionEntry.java | Tạo: entity CatalogBase cho buổi, ánh xạ LocalTime trực tiếp |
| backend/src/main/java/com/cambae/backend/m03/internal/daycare/domain/DayCareAreaState.java | Tạo: ảnh đọc khu nghỉ và ánh xạ LocationRef |
| backend/src/main/java/com/cambae/backend/m03/internal/daycare/domain/DayCareSessionState.java | Tạo: ảnh đọc buổi, version và DTO API |
| backend/src/main/java/com/cambae/backend/m03/internal/daycare/domain/SessionCapacityRow.java | Tạo: kết quả đăng ký/sức chứa/còn lại theo ngày và buổi |
| backend/src/main/java/com/cambae/backend/m03/internal/daycare/domain/DayCareSessionChange.java | Tạo: kết quả đổi buổi kèm danh sách vượt |
| backend/src/main/java/com/cambae/backend/m03/internal/daycare/persistence/DayCareStore.java | Tạo: JPA ghi, JDBC đọc hiện hành và guard transaction |
| backend/src/main/java/com/cambae/backend/m03/internal/daycare/service/DayCareCapacityService.java | Tạo: bản thật DayCareCapacityApi, lock, giao buổi, kiểm tăng đăng ký/sức chứa/khoanh vùng |
| backend/src/main/java/com/cambae/backend/m03/internal/daycare/service/DayCareConfigurationService.java | Tạo: quyền, lệnh danh mục, If-Match, validation, audit, gọi recount |
| backend/src/main/java/com/cambae/backend/m03/internal/daycare/service/DayCareRecounting.java | Tạo: lấy lịch qua SPI004, tính số chỗ, phát cảnh báo và audit |
| backend/src/main/java/com/cambae/backend/m03/internal/daycare/service/DayCareParameterListener.java | Tạo: nghe riêng CFG_M03_01 qua ApplicationModuleListener |
| backend/src/main/java/com/cambae/backend/m03/internal/structure/persistence/StructureStore.java | Sửa: current reference khu nghỉ và guard khi ngừng/xóa vị trí cha |
| backend/src/main/java/com/cambae/backend/m03/internal/structure/service/StructureService.java | Sửa: lấy guard trước khóa AREA/FLOOR ở deactivate/delete |
| backend/src/main/java/com/cambae/backend/m03/web/DayCareRestAreaController.java | Tạo: năm operation khu nghỉ theo hợp đồng dịch |
| backend/src/main/java/com/cambae/backend/m03/web/DayCareSessionController.java | Tạo: bốn operation danh mục buổi theo hợp đồng dịch |
| backend/src/main/java/com/cambae/backend/m03/web/request/DayCareRestAreaWrite.java | Tạo: code/name/floorId/areaId |
| backend/src/main/java/com/cambae/backend/m03/web/request/DayCareSessionWrite.java | Tạo: tên và HH:mm, kiểm định dạng và parse LocalTime |
| backend/src/main/java/com/cambae/backend/m03/web/response/DayCareRestAreaResponse.java | Tạo: chỉ trường contract, sức chứa đọc CFG hiện hành; bỏ trường chưa đặt |
| backend/src/main/java/com/cambae/backend/m03/web/response/DayCareSessionResponse.java | Tạo: khung giờ HH:mm và trạng thái |
| backend/src/main/java/com/cambae/backend/m03/web/response/DayCareSessionChangeResponse.java | Tạo: buổi và cảnh báo sau đổi cấu hình |
| backend/src/main/java/com/cambae/backend/m03/web/response/DayCareSessionCapacity.java | Tạo: response tra cứu số đăng ký |
| backend/src/test/java/com/cambae/backend/m03/FR_038_DayCareSchemaIT.java | Tạo: ràng buộc MySQL thật |
| backend/src/test/java/com/cambae/backend/m03/BR_M03_03_DayCareCapacityIT.java | Tạo: API thật, giao buổi, tăng/giảm số đăng ký, CFG unset và khoanh vùng |
| backend/src/test/java/com/cambae/backend/m03/FR_038_DayCareHttpIT.java | Tạo: validation/quyền/If-Match/recount/chặn lịch tương lai qua HTTP |
| backend/src/test/java/com/cambae/backend/m03/BR_M03_25_DayCareParameterIT.java | Tạo: đổi CFG và căn cứ cảnh báo khi chạy dưới SCHEDULER |
| backend/src/test/java/com/cambae/backend/m03/FR_038_DayCareConcurrencyIT.java | Tạo: hai lệnh tạo buổi, snapshot RR, ngừng vị trí cha và race dời khu |
| backend/src/test/java/com/cambae/backend/common/ApiContractIT.java | Sửa riêng đăng ký003: gỡ đúng chín operation đã cài khỏi pending |
| specs/003-room-bed-allocation/contracts/api-003.yaml | Sửa: dịch 9operation/schema đã duyệt, marker phase8 để generator nhận |
| frontend/src/api/api003.js | Sinh lại bằng npm run gen:api; không sửa tay |
| frontend/src/features/beds/DayCarePanel.jsx | Tạo: cấu hình bán trú, form tạo và bảng tra cứu số đăng ký |
| frontend/src/features/beds/DayCarePanel.test.jsx | Tạo: quyền hiển thị, tạo/cảnh báo, tham số ngày và xóa kết quả cũ khi lỗi |
| frontend/src/features/beds/BedsPage.jsx | Sửa: gắn tab Bán trú, truyền quyền CREATE từ backend |
| frontend/src/features/beds/BedsPage.test.jsx | Sửa: test tab Bán trú từ quyền cấu hình |
| specs/003-room-bed-allocation/plan.md | Bổ sung quyết định kỹ thuật, TDD, giới hạn và việc chờ duyệt |
| specs/000-shared-foundation/glossary.md | Bổ sung tên003 Phase8 và mã lỗi, không sửa thuật ngữ module khác |
| Working-history.md | Bổ sung lịch sử này, không xóa phần cũ |

Không xóa file mã nguồn hiện có; chỉ tạo rồi xóa script tạm `.phase8-contract.py` dùng dịch nhóm contract trong workspace.

### Quan hệ giữa file và luồng dữ liệu

- BedsPage đọc quyền backend, mở DayCarePanel; panel chỉ gọi client api003 sinh từ hợp đồng. Controller nhận request riêng, service kiểm quyền/lý do/If-Match rồi gọi DayCareStore. JPA quản lý version hai CatalogBase; JDBC current read dùng cùng transaction để tránh snapshot cũ. HTTP không serialize entity.
- Các lệnh cấu hình và DayCareCapacityApi dùng một day_care_capacity_guard. Ngừng/xóa vị trí cha lấy cùng guard trước khóa vị trí, nên không lọt race với tạo/dời khu nghỉ. Ràng buộc DB bảo vệ unique/XOR/giờ ngay cả khi bỏ qua validation Java.
- Bên004 phải gọi lock → occupiedSessions → tự đếm before/after → checkCapacity trong cùng transaction. Sức chứa đọc ParameterApi CFG-M03-01; khu nghỉ/tầng/khu vực đối chiếu LocationStructureApi và AllocationInfectionStatus. Chỉ tăng số đăng ký mới bị chặn khi vượt chỗ/khoanh vùng; không chặn giảm hoặc giữ nguyên số lượng chỉ vì đã vượt.
- Đổi buổi hoặc CFG gọi DayCareRecounting lấy countFrom từ DayCareArrivalCounter theo khung hiện hành. Không đọc bảng004, không sinh/hủy lịch. Buổi vượt phát DayCareOverCapacity cho Quản lý viện và ghi audit M03_DAY_CARE_OVER_CAPACITY có căn cứ BR-M03-25. Lịch có báo vắng do nguồn004 loại trừ,003 không suy diễn trạng thái thay004.
- Không có source thật004 thì thao tác cần nguồn trả503 SOURCE_UNAVAILABLE, không mặc định bằng0. Test dùng đúng FakeDayCareArrivals do003 sở hữu từ trước, không tạo fake thứ hai.

### Lệnh kiểm và tình trạng tại lúc ghi mục này

- Backend scoped: `mvnw.cmd test -Dtest=*ArchTest,ModuleBoundaryTest failsafe:integration-test failsafe:verify -Dit.test=<test của task>`; chạy spotless:apply sau sửa Java. Mỗi lượt chỉ có một Maven process vì base test tạo lại schema MySQL. Log ở thư mục Temp của máy, không đưa mật khẩu vào tài liệu/kho mã.
- Các lượt có chữ green trong tên nhưng exit1 đã ghi đúng lỗi ở trên, không coi tên log là kết quả đạt. Lượt hồi quy sau fix reviewer đang chạy `003-p8-review-green.log`, chưa ghi nhận thành công trước khi có kết quả.
- Chưa chạy clean verify cuối cho lượt Phase8 tại mốc ghi này. Baseline lượt Phase7 trước có14lỗiM01, không suy thành kết quả lượt mới. Chưa đánh `[x]` T049–T052.
- Chưa kiểm trực quan browser ba bề rộng/chế độ tối/bàn phím. Không chụp ảnh màn hình hoặc tuyên bố UI đã nghiệm thu.
- Tiếp theo theo tasks.md: hoàn thiện/xác minh T049 → T050 → T051 → T052 (hai điểm contract đang chờ); chỉ sau nghiệm thu mới chuyển Phase9 T053. Các tồn đọng Phase7 và M01 đã ghi ở lịch sử trước vẫn giữ nguyên, không tự sửa phần của B.

### Tiếp tục xác minh Phase8 sau review

- `003-p8-review-green.log` exit0: 16 IT và các ArchTests xanh. Bốn ca concurrency gồm hai lệnh tạo buổi chồng khung, RR snapshot, chặn ngừng tầng còn khu nghỉ và race dời khu/ngừng tầng. HTTP cơ cấu cũ vẫn xanh. Reviewer rà lại hai fix không còn blocker mới.
- Đã bổ sung ghi chú tiến độ trong `specs/003-room-bed-allocation/tasks.md` (giữ `[ ]`), thêm ba dòng bàn giao Phase8 ở `docs/team-plan.md` mục8: API sức chứa đã có trong checkout; B cần cung cấp nguồn004; A/C review contract dịch. Đây là hai file tài liệu bổ sung ngoài bảng file ở trên.
- `npm test -- --run`: 243 test /36file xanh (`003-p8-frontend-final.log`). Lint và build đã qua trước đó; không đổi UI sau các lệnh đó. Còn cảnh báo chunk lớn và thông báo jsdom về navigation trong test toàn kho; không coi đó là lỗi test.
- `mvnw.cmd clean verify` đã khởi chạy nền, log `003-p8-clean-verify-final.log`; chờ kết quả cuối để cập nhật tiếp. Không đánh checkbox khi chỉ có test hẹp xanh.

- Trong clean verify phát hiện failure mới ở M01 `FR_023h_KhongDauNangLucIT.FR_023h_openApi_hasNoCapacityField`: regex `"(resident)?capacity"` quét mọi schema OpenAPI, nên bắt trường `DayCareSessionCapacity.capacity` (sức chứa buổi, không phải năng lực nhận thức). Đã đọc test và assertion để xác nhận. Trường này là bản dịch `ChoTheoBuoi.sucChua` theo contract003 đã được duyệt. Không sửa contract để né regex, không tắt/sửa test của B. Đã thêm dòng bàn giao B trong docs/team-plan.md mục8. Đây là failure mới của lượt Phase8, không gộp nhầm vào14lỗi baseline cũ.

### Kết quả cuối lượt Phase8 — 2026-10-24

- `mvnw.cmd clean verify` chạy nền đã kết thúc exit1 sau04:13phút, log `003-p8-clean-verify-final.log`. 392 test đơn vị/kiến trúc xanh. 1031 IT có6failure+9error,0skip. Đọc XML failsafe: toàn bộ186 IT M03 /39class xanh, trong đó16 IT mới Phase8; ApiContractIT cũng xanh.
- 15 lỗi đều ở8class M01: BR_M01_01_TemporaryAbsenceOverdueIT (1error), BR_M01_02_ReassessmentNoticeIT (2error), CompleteAdmissionConsentIT (2error), CompleteAdmissionWishWarningsIT (3error), FR_023h_KhongDauNangLucIT (1failure mới do regex capacity), FR_034a_CompleteAdmissionIT (4failure), ResidentContractIT (1failure), StatusHistoryIT (1error). Không sửa/tắt test của B. Mới có failure FR023h đã bàn giao riêng;14lỗi còn lại khớp baseline trước.
- Frontend toàn kho243test/36file xanh. Sau đó sửa riêng utility tiêu đề trong DayCarePanel từ tên chưa có sang type-label theo DESIGN; chạy lại12test/2file xanh, lint exit0, build exit0 (`003-p8-ui-final-scoped.log`, `003-p8-lint-final.log`, `003-p8-build-final.log`). Còn cảnh báo chunk lớn. Không có kiểm browser ba bề rộng/chế độ tối/bàn phím, chưa nghiệm thu trực quan.
- `git diff --check` được kiểm lại sau bỏ dòng trắng cuối tasks.md; không commit/push/stage/đổi nhánh. Chưa đánh `[x]` T049–T052 vì full verify đỏ và T052 chưa hoàn thiện sửa/ngừng hiệu lực UI; hai câu hỏi API version response và danh sách residentId tương lai vẫn chưa có câu trả lời. Nguồn DayCareArrivalCounter thật004 vẫn cần B cung cấp; không cho fake vào production.
- Thứ tự tiếp: chốt phạm vi API còn thiếu của T052, hoàn thiện UI và test tương ứng; xác minh lại theo T049 → T050 → T051 → T052, xử lý bàn giao M01/004 và kiểm UI theo DESIGN. Chỉ chuyển T053 Phase9 sau nghiệm thu. Các phần Phase7 còn tồn đọng trong lịch sử cũ được giữ nguyên.

### Xác nhận hai lựa chọn của chủ dự án — 2026-10-24

- Chủ dự án trả lời “2” cho đề nghị version response: giữ contract hiện hành, để phần sửa/ngừng hiệu lực UI chờ. Không thêm trường, endpoint đọc chi tiết hoặc cơ chế phiên bản khác; không giả If-Match=0.
- Chủ dự án trả lời “2” cho đề nghị mở rộng DayCareArrivalCounter: giữ SPI hiện hành, backend chỉ chặn chung khi còn lịch tương lai. Phần liệt kê người có lịch chưa thực hiện; không coi lựa chọn này là cho phép mở rộng SPI hoặc miễn yêu cầu liệt kê của T052.
- File tài liệu cập nhật: plan.md bổ sung quyết định và hệ quả; tasks.md đổi ghi chú từ “chờ quyết định” sang “đã chọn giữ contract/SPI”; docs/team-plan.md mục8 cập nhật trạng thái hai đề nghị; Working-history.md nối thêm mục này. Không sửa mã nguồn, contract, fake hoặc checkbox task trong lượt xác nhận.
- Chỉ kiểm git diff --check cho thay đổi tài liệu; không chạy lại build/test vì không có thay đổi mã. Kết quả kiểm mã gần nhất vẫn là clean verify exit1 với15lỗiM01,186IT M03 xanh, frontend243test/lint/build xanh; không ghi thành kết quả chạy mới.
- Phase8 chưa nghiệm thu. Phần còn lại: T052 sửa/ngừng UI cần cách lấy phiên bản phù hợp được duyệt; liệt kê người có lịch còn thiếu; nguồn004 thật, xử lý bàn giaoM01 và kiểm trực quan UI. Thứ tự task vẫn T049 → T050 → T051 → T052, rồi mới T053 khi đủ điều kiện.

## 2026-10-24 — Tiếp tục riêng T049 và T050 của Spec003

- Yêu cầu: thực hiện tiếp T049–T050 trong phạm vi không cần cài thêm spec người khác. Đã đọc lại tasks.md, ghi chú plan, test và migration thực tế; giữ nguyên các quyết định giữ contract/SPI và phần T051/T052 đang chờ.
- T049 đã có test cho sức chứa trước/sau, giao buổi nửa mở, CFG đổi, khung đổi/recount/cảnh báo, unique khu nghỉ, lịch tương lai và các race. T050 đã có V003_05__day_care_capacity.sql với CHECK/UNIQUE/FK chỉ tới bảng003. Không tạo lại migration hoặc fake, không đổi production chỉ để có thay đổi mới.
- Trình tự: (1) đối chiếu yêu cầu với5class test Phase8; (2) chạy5class cùng ArchTests:16IT+25testkiếntrúc xanh, BUILD SUCCESS trong39.128giây, log003-t049-t050-verify.log; (3) bổ sung hai ca biên; (4) chạy spotless:apply và kiểm hai lớp thay đổi cùng ArchTests.
- File sửa: backend/src/test/java/com/cambae/backend/m03/BR_M03_03_DayCareCapacityIT.java thêm BR_M03_03_weeklySeriesReportsOnlyTheDateThatExceedsCapacity, đưa Map<SessionDate,RegistrationCount> cho ba ngày cách nhau một tuần vào API thật, khẳng định chỉ ngày có4/3đăng ký bị báo; không tạo lịch lặp hoặc đọc bảng004. backend/src/test/java/com/cambae/backend/m03/FR_038_DayCareSchemaIT.java thêm assertion MySQL từ chối khung07:00–07:00. plan.md bổ sung mapping yêu cầu/test và lý do không sửa SQL; tasks.md sẽ ghi kết quả riêng và giữ gate nghiệm thu; Working-history.md nối mục này. Không tạo/xóa file nguồn trong lượt này.
- Quan hệ dữ liệu: test capacity dùng đúng API thật của003, ParameterApi đã có và fake khoanh vùng003; số trước/sau là đầu vào của bên gọi, không triển khai thay nguồn004. Test schema ghi trực tiếp bằng JdbcTemplate trong transaction để chứng minh DB tự chặn khung rỗng bằng CHECK, không chỉ dựa validation service.
- Hai ca bổ sung kiểm hành vi đã có, không phát hiện lỗi production hoặc cần implementation mới; không ghi nhận một vòng RED mới. Bằng chứng test RED trước implementation ban đầu giữ nguyên trong lịch sử Phase8.
- Lần khởi chạy lệnh bổ sung bị automatic permission approval review hết deadline trước khi chạy process; công cụ cho phép thử lại một lần. Đã thử lại cùng lệnh và khởi chạy thành công; không bỏ sandbox hoặc đổi phạm vi để né review. Lệnh bổ sung đang chạy, chưa ghi xanh trước khi có kết quả.
- Kết quả bổ sung:003-t049-t050-extra-verify.log exit0, BUILD SUCCESS trong43.366giây;8IT (5capacity+3schema) và25testkiếntrúc xanh; spotless:apply qua. Chỉ hai file test và tài liệu nêu trên thay đổi trong lượt này; migration đã có và được Flyway áp thành công khi dựng schema test.
- T049–T050 đạt phần kiểm riêng, chưa đánh[x] do quy tắc phải qua clean verify cuốiPhase8. Không chạy lại frontend hoặc toàn kho vì không đổi production/UI;15lỗiM01 của lần clean verify gần nhất vẫn là tồn đọng đã bàn giao, không xem là kết quả chạy mới.
- Task kế tiếp theo tasks.md là T051 (nguồn004 thật cầnB), sau đóT052 (các phần UI/liệt kê/kiểm trực quan còn thiếu theo quyết định giữ contract/SPI). Lượt này không thực hiện hai task đó; không commit/push/stage hoặc thay hợp đồng.

## 2026-10-25 — Bắt đầu Phase9 Spec003 (T053–T056)

- Đã đọc AGENTS/CLAUDE/team-plan, báo các dòng liên quan HungNX-nguyen/C mục8; đọc tasks, plan ghi chú, backend README trước khi tra data-model mục5, researchG08, FR010/024a/028 và BR-M03-21 của tài liệu nghiệp vụ. Giữ phạm vi003 và các quyết định cũ; checkout hiện trên feature/003-manage-bedroom, không tự đổi nhánh/commit/push.
- Chủ dự án duyệt bản dịch5operation HTTP Phase9. Đã dịch đúng nhóm đó, clone RoomIsolationHistoryRow tiếng Anh và dùng lại Room/BedAllocation/BedTransferResult. Đã sinh lại client bằng npm run gen:api. Hai API đọc cho lựa chọn phòng/Bác sĩ đang được hỏi riêng, chưa coi là được duyệt.
- Viết FR_008_RoomIsolationIT trước:3test RED404, sau đó thêm migration V003_06, RoomIsolationFact/Store/Service, RoomIsolationController/Write/HistoryRow. Kiểm quyền Quản lý viện/Bác sĩ đặt-gỡ, Trưởng tầng chỉ xem; lý do bắt buộc, trạng thái lặp409 và lịch sử không cho sửa/xóa. Lượt xanh003-p9-isolation-green.log exit0,3IT+ArchTests,01:17phút.
- Viết BR_M03_21_ControlledTransferIT trước:4test RED404 (chuyển vào phòng cách ly, Bác sĩ không hợp lệ, giới tính không phù hợp, Bác sĩ xác nhận ở lại). Thêm ControlledTransferResult/Store/Service, InfectionControlTransferWrite, BedTransferResult, ControlledTransferController. Lần compile đầu dùng nhầm constructor không tham số của PermissionDeniedException; đã sửa bằng constructor thông báo, lượt kiểm mới đang chạy. Không ghi nhận đây là lỗi nghiệp vụ hoặc kết quả xanh.
- Quan hệ luồng: HTTP→service kiểm quyền/lý do→khóa phân bổ/phòng/giường theo thứ tự→JDBC metadata và lịch sử→BedStateService duy nhất thay trạng thái giường→audit và sự kiện cùng transaction. RoomResponse refresh entity để không trả cờ isolated cũ. AccountApi xác minh tài khoản Bác sĩ, thông báo chọn RecipientGroup.Staff theo staffId của tài khoản đó; không gửi nhầm bác sĩ trực.
- Quyết định kỹ thuật và giới hạn ghi trong plan Phase9. M07 chưa có sự kiện công khai nên chưa cài listener thay chủ D; mobile nhân viên chưa mở nên giữ phần appT056 chưa làm. Chưa đánh[x] hoặc tuyên bố nghiệm thu. File/lệnh/kết quả cuối sẽ bổ sung khi thực hiện thực tế, không xóa lịch sử trước.

### Tiếp tục Phase9 sau các phê duyệt HTTP và review

- Chủ dự án đã duyệt thêm cả ba GET `/room-isolation/rooms`, `/bed-allocations/prescribing-doctors`, `/room-isolation/pending-stays`. Phòng dùng ROOM_ISOLATION.VIEW; danh sách Bác sĩ dùng BED_ALLOCATION.CREATE; danh sách chờ dùng ROOM_ISOLATION.UPDATE cùng điều kiện Bác sĩ Hoạt động. Không sửa quyền GET /rooms, GET /accounts hoặc GET /bed-allocations. Phần “đang hỏi” trong mục trước là trạng thái tại thời điểm ghi, nay được thay bằng phê duyệt này.
- Trình tự thực tế: test3lệnh/lịch sử RED404 → migration/lệnh cách ly → test4chuyển/xác nhận RED404 → service/controller chuyển → phát hiện cập nhật dấu trên CLOSED bị trigger chặn, chuyển clearPending lên trước close → bổ sung3GET sau phê duyệt → test lựa chọn RED404/400 → controller/API đọc → testUI RED thiếu component → RoomIsolationPanel/IsolationWorkspace → nối tab/quyền → testmenu RED thiếu DOCTOR → sửa đúng dòng đăng ký beds → review cuối → test trạng thái Bác sĩ RED → sửa kiểm ACTIVE tại003 → scoped xanh → chạy clean verify nền.
- Các lỗi thiết lập đã xử lý, không bịa thành lỗi production: deadline fixture phải dùng microsecond của DATETIME(6); spy FakeAllocationResidencySource phải lấy target sau proxy để stub không vướng MANDATORY ngoài transaction; account fixture LOCKED cần lock_cause=MANUAL, TEMPORARILY_LOCKED cần locked_until theo CHECK002. Test quyền /accounts ban đầu đoán403 sai: Hành chính được đọc người thân, lọc type=STAFF trả items rỗng; đã sửa riêng test003.

Các file tạo mới (đường dẫn Java dưới `backend/src/main/java/com/cambae/backend/m03/`):

| File | Mục đích thực tế |
| --- | --- |
| `internal/isolation/domain/RoomIsolationFact.java` | Ảnh một lần đặt/gỡ, action/time/actor/reason/incidentId |
| `internal/isolation/domain/IsolationRoom.java` | Room cùng cờ chịu chặn đã xét nguồn khoanh vùng |
| `internal/isolation/domain/PrescribingDoctor.java` | Id và tên Bác sĩ được phép chọn |
| `internal/isolation/domain/ControlledTransferResult.java` | Hai ảnh phân bổ trước/sau chuyển |
| `internal/isolation/persistence/RoomIsolationStore.java` | GuardX→room lock, SQL đổi cờ/version, thêm/đọc lịch sử, xóa dấu chờ ACTIVE khi hết chịu chặn |
| `internal/isolation/persistence/ControlledTransferStore.java` | Đọc hold/dấu chờ bằng khóa, ghi previous/reason/doctor, ghi xác nhận |
| `internal/isolation/service/RoomIsolationService.java` | Lệnh đặt/gỡ, audit/sự kiện, tra phòng/lịch sử đúng quyền |
| `internal/isolation/service/ControlledTransferService.java` | Chuyển ngoại lệ nguyên tử, chỉ miễn cách ly, kiểm Bác sĩ ACTIVE, xác nhận/danh sách chờ/lựa chọn Bác sĩ |
| `web/RoomIsolationController.java` | HTTP đặt/gỡ/lịch sử phòng, map actor từ AccountApi |
| `web/ControlledTransferController.java` | HTTP chuyển kiểm soát và xác nhận ở lại |
| `web/IsolationOptionsController.java` | Ba GET phụ trợ đã duyệt, không gọi API quản trị thay quyền |
| `web/request/RoomIsolationWrite.java` | Reason bắt buộc và incidentId tùy chọn |
| `web/request/InfectionControlTransferWrite.java` | Giường đích, Bác sĩ chỉ định và Reason |
| `web/response/RoomIsolationHistoryRow.java` | JSON nhật sử theo schema đã dịch |
| `web/response/PrescribingDoctorResponse.java` | JSON id/fullName tối thiểu |
| `web/response/BedTransferResult.java` | Phân bổ cũ/mới và warnings theo hợp đồng |

Các file khác tạo mới:

| File | Mục đích thực tế |
| --- | --- |
| `backend/src/main/resources/db/migration/V003_06__room_isolation_history.sql` | Bảng append-only, CHECK lý do, trigger chặn UPDATE/DELETE ngoài retention, grant INSERT ứng dụng; không sửa migration cũ |
| `backend/src/test/java/com/cambae/backend/m03/FR_008_RoomIsolationIT.java` | Quyền, lý do, lệnh lặp, lịch sử, API phòng cho Bác sĩ |
| `backend/src/test/java/com/cambae/backend/m03/BR_M03_21_ControlledTransferIT.java` | Chuyển/doctor/gender, xác nhận, pending list, giữ hạn vắng, gỡ phòng còn vùng khoanh, trạng thái tài khoản |
| `backend/src/test/java/com/cambae/backend/m03/FR_028_ControlledTransferAtomicityIT.java` | Transaction thật rollback sau lỗi004 cuối luồng; admin cũng không sửa/xóa được nhật sử |
| `frontend/src/features/beds/RoomIsolationPanel.jsx` | Chọn phòng, lịch sử, đặt/gỡ có lý do, retry và phản hồi lỗi |
| `frontend/src/features/beds/RoomIsolationPanel.test.jsx` | Xem/đặt/gỡ lỗi, idempotency và giữ nội dung khi lỗi |
| `frontend/src/features/beds/IsolationWorkspace.jsx` | Chọn giường/Bác sĩ từ API, chuyển kiểm soát, xác nhận đúng phân bổ, quyền backend |
| `frontend/src/features/beds/IsolationWorkspace.test.jsx` | Hai luồng thành công, body và danh sách chọn đúng API được duyệt |

Các file sửa:

| File | Mục đích thực tế |
| --- | --- |
| `specs/003-room-bed-allocation/contracts/api-003.yaml` | Dịch5operation, thêm3GET đã duyệt cùng schema tương ứng |
| `backend/src/test/java/com/cambae/backend/common/ApiContractIT.java` | Bỏ đúng5pending Phase9 đã cài để đối chiếu thật; không nới test contract |
| `frontend/src/api/api003.js` | Sinh tự động bằng npm run gen:api; API khác không có diff nội dung |
| `frontend/src/features/beds/BedsPage.jsx` | Tab Cách ly và trang cho người chỉ có quyền cách ly |
| `frontend/src/features/beds/BedsPage.test.jsx` | Quyền Bác sĩ, không tải cấu hình khi không có quyền, menu tới trang |
| `frontend/src/common/layout/navigation.js` | Chỉ dòng beds của003: bổ sung BS theo dòng Cách ly phòng; frontend README cho phép chủ feature sửa dòng đăng ký của mình |
| `specs/000-shared-foundation/glossary.md` | Thêm riêng thuật ngữ003Phase9, class/DTO/màn/mã lỗi đã dùng |
| `specs/003-room-bed-allocation/plan.md` | Quyết định khóa, transaction,3GET, sửa lỗi thực tế, kết quả và phần chờ |
| `specs/003-room-bed-allocation/tasks.md` | Ánh xạ tên logic sang file thật và trạng thái chưa nghiệm thu |
| `docs/team-plan.md` | Mục8 bàn giao nguồn/sự kiện007 choD và review8operation choA/C |
| `Working-history.md` | Nối lịch sử này; giữ nguyên mọi nội dung cũ |

- Luồng dữ liệu: client sinh→controller/request→service kiểm permission/lý do/Bác sĩ→AllocationLocks/Checker đọc M01 qua api và các SPI003→JDBC metadata/RoomIsolationStore hoặc AllocationCommandStore→BedStateService thay hai trạng thái→AuditLogApi và event publisher cùng transaction→response DTO→UI reload. DoctorAssignedToAllocation dùng staffId của tài khoản chỉ định. Chuyển vắng mang nguyên hold ABSENT_RESIDENT và hạn còn lại; không tự lập bản ghi nghiệp vụ từ job.
- Quyết định: ControlledTransferService riêng chỉ phục vụ ngoại lệ T055; không dựng luồng chuyển thường còn thiếu. GuardX trước room bảo vệ các lần bulk xóa dấu chờ và cùng thứ tự khóa writers, đánh đổi tuần tự hóa ngắn khi đặt/gỡ. Chỉ bỏ ALLOCATION_ISOLATION; các lỗi giới tính/mức chăm sóc/trùng lịch/trạng thái vẫn chặn. Không thêm thư viện hoặc API ngoài những gì đã duyệt.
- Review cuối read-only phát hiện hasRole chỉ xét vai trò, activeWithRole chủ ý bao gồm TEMPORARILY_LOCKED cho thông báo009. Test hồi quy4ca RED ở `003-p9-review-red2.log` (12IT,4failure,0error); sửa003 lọc AccountView.status==ACTIVE cả command/list/confirm, giữ nguyên002. Lượt RED đầu bị3lỗi fixture CHECK, đã sửa fixture trước khi chứng minh RED hành vi. Không có finding khác từ reviewer.
- Lệnh kiểm scoped: `mvnw.cmd spotless:apply test -Dtest=*ArchTest,ModuleBoundaryTest failsafe:integration-test failsafe:verify -Dit.test=FR_008_RoomIsolationIT,BR_M03_21_ControlledTransferIT,FR_028_ControlledTransferAtomicityIT,ApiContractIT`. `003-p9-backend-scoped2.log`:15IT+25kiến trúc xanh; sau sửa review `003-p9-review-green.log`:19IT+25kiến trúc xanh, exit0/01:56phút. Test phase mới đã RED trước implementation, không ghi các kiểm fixture thành RED nghiệp vụ.
- Frontend: `npm run gen:api`; scoped3file15test xanh trước bổ sung menu, sau đó `npm test -- --run`:250test/38file xanh (`003-p9-web-full.log`); `npm run lint`, `npm run build` qua, còn cảnh báo chunk>500kB. Không đổi package.json.
- Kiểm trực quan dùng server Vite/HTTP fixture tạm, không ghi backend thật: mở menu Bác sĩ→tab Cách ly→chọn phòng/lịch sử→form xác nhận, dark mode và Tab tới nút lưu; Hành chính→tab Cách ly→form chọn giường/Bác sĩ chỉ định. Đã xem viewport375/768/1280, đo không tràn ngang ở375/1280. Fixture đầu thiếu startAt làm màn chuyển thường lỗi ngày, đã bổ sung đúng trường contract; không coi đây là lỗi API production. Chưa coi kiểm fixture là tích hợp007 thật.
- Kiểm toàn kho đã khởi chạy nền `mvnw.cmd clean verify`, log `C:/Users/nguye/AppData/Local/Temp/003-p9-clean-verify-final.log`; kết quả được bổ sung sau khi process kết thúc. Không đánh[x] trong lúc lệnh còn chạy.

### Kết quả cuối lượt Phase9 — chưa nghiệm thu toàn phase

- `mvnw.cmd clean verify` chạy nền đã kết thúc exit1, BUILD FAILURE,06:25phút.392test đơn vị/kiến trúc xanh.1050IT có6failure,9error,0skip. Đọc XML failsafe xác nhận toàn bộ205IT M03 xanh, gồm18ca mới Phase9 và các ca trở về phòng cách ly của Phase6. Spotless check1308file,0file cần sửa. Đây là kết quả chạy mới, không lấy số liệu Phase8 để thay.
- 15 lỗi đều thuộc M01, cùng các nhóm đã bàn giao B: BR_M01_01_TemporaryAbsenceOverdueIT (1 error), BR_M01_02_ReassessmentNoticeIT (2 error), CompleteAdmissionConsentIT (2 error), CompleteAdmissionWishWarningsIT (3 error), FR_023h_KhongDauNangLucIT (1 failure), FR_034a_CompleteAdmissionIT (4 failure), ResidentContractIT (1 failure), StatusHistoryIT (1 error). Lỗi nguồn lưu trú/điều kiện tiếp nhận xuất hiện khi fixture 001 nạp participant thật 003; test capacity và contract 001 vẫn có phạm vi bắt cả schema/endpoint 003 như đã ghi trước. Không sửa nguồn/test M01 theo lựa chọn của chủ dự án.
- Các file tạm `.phase9-contract.py` và `.phase9-qa.mjs` chỉ dùng dịch contract/kiểm UI, đã xóa; server QA đã dừng, viewport đã reset. Không xóa file nguồn nào. Không tạo PR, commit, push, stage, đổi nhánh hoặc tiếp tục merge.
- Tồn đọng Phase9: T053 thiếu kiểm qua sự kiện007 thật; T054 thiếu listener gỡ khoanh/thêm tiếp xúc và nguồn007 thật doD; T055 phần chuyển/xác nhận đã cài và xanh nhưng chưa đánh theo gate toàn kho; T056 backend/web đã cài, appnhânviên chưa mở theo CLAUDE.md:12 và mobile README. Thiếu nguồn004/007 production vẫn được báo503 ở đường cần nguồn, không đưa fake vào production. Phase8 T051/T052 còn tồn đọng đã ghi riêng, không được ghi hoàn tất bởi lượt Phase9.
- Thứ tự tiếp theo trong tasks.md ở phạm vi đang làm: T053(ca sự kiện khiD bàn giao)→T054(listener dùng API/fake doD công bố)→T055(chốt gate sauB sửa lỗi toàn kho)→T056(nghiệm thu backend/web và giữ appchờ mở). Khi chuyển sangPhase10, task đầu làT057 rồiT058,T059; chưa thực hiện các taskđó. Giữ toàn bộ checkboxPhase9[ ] và cập nhật plan/tasks/team-plan đúng trạng thái.
- Hai quyết định kỹ thuật chính được ghi plan: dùng service riêng cho chuyển ngoại lệ, giữ dùng chung guard/checker/state machine khi chuyển thường được cài sau; dùng guardX trước room, chấp nhận tuần tự hóa ngắn để không đảo thứ tự khóa. Không có finding minor nào được reviewer báo để hoãn. Báo cáo cuối phải phân biệt implementation backend/web đã có với nghiệm thu toàn phase chưa đạt.

## 2026-10-25 — Triển khai Phase 10 Spec 003 (T057–T060)

- Đầu lượt đã đọc AGENTS.md, mục8 team-plan và báo đủ bốn dòng chứa HungNX-nguyen; đọc tasks.md và ghi chú plan trước khi tra G-09/G-14, data/API hiện có, FR-044/045 và nhánh FR-011a. Checkout đầu lượt chỉ có Working-history.md chưa được Git theo dõi; các thay đổi Phase9 trước đó đã có trong checkout. Không tự commit, push, đổi nhánh hoặc tiếp tục merge.
- Chủ dự án duyệt dịch riêng GET `/beds/{id}/readiness-conditions`, POST `/beds/{id}/confirm-readiness` và request/response liên quan. Đồng thời duyệt thiếu khử khuẩn/bảo hộ trả422 thay409 của contract cũ, theo T057 và state machine hiện có. Không thay API khác hoặc hai quyết định giữ nguyên contract/SPI Phase8, enum clock000 trước đó.
- T057: tạo BR_M03_09_BedReadinessIT trước implementation; `003-p10-readiness-red.log`:5IT,4failure404 vì chưa có endpoint; CHECK MySQL đã có nên ca DB xanh ngay, không nhận là RED mới của schema. Cài lớp sẵn sàng và HTTP; lượt `003-p10-readiness-green.log` còn1failure do test kỳ vọng403 cho Trưởng tầng ngoài phạm vi, trong khi002 che đối tượng bằng404. Đã sửa chính test mới003 theo RequiresPermissionAspect, không đổi quyền002. Lượt tiếp theo xác nhận5test này xanh.
- T058: lớp trong internal/bed/{domain,persistence,service}, web/{request,response}. Controller tra Target từ giường, truyền Target vào phương thức có RequiresPermission để quyền nhiệm vụ SHIFT_LEAD được002 xét theo tầng; service kiểm lại tầng và người được giao sau khóa. Nguồn007 được hỏi lại lúc lệnh chạy, không tin bản xem trước. BedStateService vẫn là cổng duy nhất đổi trạng thái; BedTransition bổ sung note với constructor tương thích cho lệnh cũ, BedStateStore INSERT note ngay lần tạo fact, không UPDATE lịch sử append-only. Chỉ Sử dụng lại không có last_closed_allocation_id thì không có người vừa rời để kiểm.
- Nhánh chuyển đang chờ dùng cổng BedAllocationCommandApi.applyScheduledTransfer đã khai báo, cùng transaction xác nhận. Bản thật T032–T033 của chính003 chưa có; khi tồn tại phân bổ đang chờ mà thiếu provider, trả503 và giữ nguyên trạng thái/lịch sử. Đây là phần chưa nghiệm thu thật, không đưa fake vào production và không ghi mock là bản cài chuyển theo lịch. Không triển khai vòng đời yêu cầu004 thay B.
- T059: tạo FR_044_BedRemindersIT trước job; `003-p10-reminders-red.log`:2error đúng vì thiếu bean bedReadinessReminderJob/allocationReminderJob, còn5test sẵn sàng xanh. Sau cài, `003-p10-reminders-green.log`:2IT+25kiến trúc xanh. Hai job chỉ đọc trạng thái/dấu, ghi mốc kỹ thuật, audit và sự kiện; không tạo phân bổ/yêu cầu/bàn giao. Nhắc giường quá hạn một lần mỗi lần vào Chờ sẵn sàng; chu kỳ nhắc dấu đọc CFG-M03-02 và ngưỡng trễ CFG-M03-08 qua ParameterApi. Profiletest không tự chạy, test gọi runNow với TestClockControl.
- T060: dịch contract theo phê duyệt, sinh client bằng npm run gen:api, không sửa file sinh bằng tay. ApiContractIT cần bỏ đúng2pending đã cài; lần kiểm đầu còn pending cũ vì patch nhiều file bị từ chối nguyên khối, đã sửa lại đúng2dòng. Không nới so sánh schema. App nhân viên chưa mở theo CLAUDE.md:12/mobile README nên giữ phần app[ ]; task không yêu cầu web mới, không sửa UI hoặc chụp màn hình.
- Kiểm mở rộng đầu (`003-p10-scoped-review.log`) có2failure: pending contract nêu trên và fixture rollback kỳ vọng IllegalStateException trong khi proxy Repository dịch thành InvalidDataAccessApiUsageException, root cause vẫn IllegalStateException. Đã sửa assertion của chính test mới để kiểm DataAccessException cùng root cause, không sửa production để chiều fixture.
- Review cuối chỉ đọc theo executing-plans phát hiện2điểm: tín hiệu BedBecameEmpty cần chặn khi còn FUTURE hoặc tài sản chờ chuyển người; marker ROOM_UNSUITABLE cần tạo/xóa cùng lệnh đổi dấu, không bắt đầu muộn ở lần job đầu hoặc giữ counter cũ qua gỡ/đặt lại. Đã thêm3ca hồi quy để thấy RED trước sửa; kết quả và file sửa tiếp được ghi sau khi thực hiện thực tế.

### File và luồng xử lý thực tế của Phase 10

File tạo mới, đường dẫn Java production dưới `backend/src/main/java/com/cambae/backend/m03/`:

| File | Mục đích |
| --- | --- |
| `internal/bed/domain/BedReadiness.java` | Ảnh điều kiện tại thời điểm hỏi, cờ khử khuẩn, id người vừa rời và điều kiện chưa thỏa |
| `internal/bed/persistence/BedReadinessStore.java` | Tra giường/tầng/người vừa rời, phân bổ chờ chuyển, quét deadline và đánh dấu đã nhắc; không ghi state |
| `internal/bed/service/BedReadinessService.java` | Kiểm quyền theo Target/tầng và nhiệm vụ, khóa, kiểm lại007, gọi state machine/cổng chuyển đang chờ, trả ảnh giường |
| `internal/bed/service/BedReadinessReminderProcessor.java` | Transaction từng giường, kiểm lại quá hạn sau khóa, marker/audit/sự kiện cùng rollback |
| `internal/allocation/persistence/AllocationReminderStore.java` | Quét dấu thật cùng marker cũ, khóa nguồn và mốc chu kỳ, đánh dấu/xóa counter kỹ thuật |
| `internal/allocation/service/AllocationReminderProcessor.java` | CFG-M03-02/08, kiểm lại dấu, chọn Hành chính/Trưởng tầng theo từng loại dấu, audit/thông báo |
| `scheduling/BedReadinessReminderJob.java` | ScheduledJob REMIND_BED_READINESS, ShedLock, SchedulerRuns, lịch15phút và startup ngoài profiletest |
| `scheduling/AllocationReminderJob.java` | ScheduledJob REMIND_ALLOCATIONS cùng cơ chế chạy, không lập yêu cầu/phân bổ |
| `web/BedReadinessController.java` | Hai HTTP operation đã được duyệt, DTO hóa kết quả, lấy tên người vừa rời qua ResidentApi |
| `web/request/BedReadinessWrite.java` | disinfected, ppeUsed và note tối đa2000ký tự; không thêm lý do bắt buộc ngoài nghiệp vụ |
| `web/response/BedReadinessResponse.java` | allowed, disinfectionRequired, departedResident, unmetConditions theo schema đã dịch |

Các file test mới:

| File | Mục đích |
| --- | --- |
| `backend/src/test/java/com/cambae/backend/m03/BR_M03_09_BedReadinessIT.java` |8ca quyền, xác nhận/note, hỏi lại lây nhiễm, CHECK, nhánh thiếu provider503 và chặn tín hiệu giường nhận người khi còn FUTURE/assetflag |
| `backend/src/test/java/com/cambae/backend/m03/FR_044_BedRemindersIT.java` |4ca đồng hồ giả: nhắc giường một lần, chu kỳ WITHOUT_BED, ba dấu phân bổ/nhóm nhận riêng, mốc tạo/gỡ/đặt lại cùng lệnh |
| `backend/src/test/java/com/cambae/backend/m03/FR_044_ReminderAtomicityIT.java` | Transaction thật kết thúc trước assertion, lỗi sau ghi marker phải rollback rồi retry được |

Các file sửa:

| File | Mục đích |
| --- | --- |
| `backend/src/main/java/com/cambae/backend/m03/internal/bed/domain/BedTransition.java` | Bổ sung note và constructor cũ tương thích để không sửa hàng loạt lệnh ngoài task |
| `backend/src/main/java/com/cambae/backend/m03/internal/bed/persistence/BedStateStore.java` | INSERT note vào fact trạng thái, không sửa lịch sử đã ghi |
| `backend/src/main/java/com/cambae/backend/m03/internal/allocation/persistence/AllocationCommandStore.java` | markUnsuitable tạo/xóa marker ROOM_UNSUITABLE cùng transaction, mốc lấy BusinessTime; không reset khi sự kiện lặp |
| `backend/src/test/java/com/cambae/backend/common/ApiContractIT.java` | Bỏ đúng2pending HTTP Phase10 đã cài; giữ pending chuyển giường thường Phase5 |
| `backend/src/test/java/com/cambae/backend/common/scheduling/SchedulerScopeArchTest.java` | Chỉ thêm2dòng job003 và loại việc nhắc được phép, theo hướng dẫn đăng ký ngay trong test |
| `specs/003-room-bed-allocation/contracts/api-003.yaml` | Dịch2operation/schema và phản hồi422 đã duyệt; dùng lại Bed/ResidentSummary/UnmetCondition/Problem |
| `frontend/src/api/api003.js` | Chỉ client sinh từ contract qua npm run gen:api; không sửa UI hoặc các client khác bằng tay |
| `specs/003-room-bed-allocation/plan.md` | Quyết định, mapping lớp, phê duyệt, TDD, review/fix, dependency và kiểm cuối |
| `specs/003-room-bed-allocation/tasks.md` | Mapping T057–T060 sang file thật và trạng thái chưa nghiệm thu |
| `specs/000-shared-foundation/glossary.md` | Bổ sung riêng tên mã003Phase10, job/mã lỗi/audit đã dùng |
| `docs/team-plan.md` | Bàn giao A review contract/job và C hoàn thiện provider chuyển theo lịch/mốc các dấu tương ứng |
| `Working-history.md` | Bổ sung lịch sử mới này, giữ nguyên lịch sử cũ |

- Không tạo/sửa/xóa migration: các cột deadline, ready_reminded_at, bằng chứng khử khuẩn, note và bảng allocation_reminder đã có. Không xóa file nguồn, không thêm thư viện, không sửa source/testM01 hoặc enum runJobs API000. Không sửa giao diện/mobile.
- Luồng xác nhận: HTTP→service.target đọc tầng thật→RequiresPermission002 xét Target (kể cả nhiệm vụ SHIFT_LEAD)→guardX/room/bed→kiểm lại người được giao và danh sách007 hiện hành→BedStateService CONFIRM_READY→INSERT history có note + audit→nếu có pending thì gọi BedAllocationCommandApi cùng transaction, nếu thiếu trả503/rollback→nếu không có pending dùng BedHoldService.becameEmpty gate FUTURE/assetflag→BedResponse. Thông báo không được phát như giường nhận người mới nếu vẫn bị giữ bởi phân bổ tương lai/tài sản.
- Luồng nhắc: ScheduledJob→SchedulerRuns.start→quét candidate→processor từng đối tượng khóa và đọc lại nguồn→đọc CFG hiện hành→marker + AuditLogApi (Basis có schedulerRunId, plannedAt/executedLate) + event cùng transaction→SchedulerRuns.finish. ROOM_UNSUITABLE có marker từ lệnh nguồn; WITHOUT_BED đã có từ participant. Dấu cũ chưa có marker được tạo ở lần quan sát đầu, không bịa thời điểm đặt; các writer AWAITING_REQUEST/AWAITING_SAME_PRICE tương lai phải tuân thủ data-model mục7 như bàn giao C.
- Quyết định kỹ thuật: dùng Target do service tạo để002 cấp đúng quyền theo nhiệm vụ, kiểm lại sau khóa tránh tin floor do người gọi dựng; guardX bảo vệ nhánh tiếp tục chuyển, đánh đổi tuần tự hóa ngắn; lưu note ngay INSERT qua overload BedTransition để giữ tương thích; dùng lại gate BedHoldService thay viết lại quy tắc giường trống. Dùng SPI hiện có cho nhánh chuyển còn thiếu, không tạo provider giả production. Chi tiết trong plan.
- Bằng chứng fix review: `003-p10-final-review-red.log` có14IT/3failure/0error đúng3ca mới; sau dùng lại gate và sửa marker nguyên tử, `003-p10-final-scoped-green.log` exit0,01:03phút:15IT (8readiness+4reminder+1rollback+1ApiContract+1FR_034 cũ) và25testkiến trúc xanh,0skip; spotless:apply qua. Không có finding minor nào được reviewer báo.
- Lệnh kiểm hẹp cuối: `mvnw.cmd spotless:apply test -Dtest=*ArchTest,ModuleBoundaryTest failsafe:integration-test failsafe:verify -Dit.test=BR_M03_09_BedReadinessIT,FR_044_BedRemindersIT,FR_044_ReminderAtomicityIT,ApiContractIT,FR_034_AllocationSuitabilityIT`. Có sửa markUnsuitable nên thêm đúng hồi quy suitability của task liên quan; không chạy bừa toàn suite sau từng task.
- Frontend sau sinh client: `npm test -- --run`250test/38file xanh (`003-p10-web-test.log`), `npm run lint` qua, `npm run build` qua trong868ms, còn cảnh báo chunk>500kB. Không có thay đổi UI nên không chụp màn hình.
- Đã khởi chạy nền `mvnw.cmd clean verify`, log `C:/Users/nguye/AppData/Local/Temp/003-p10-clean-verify-final.log`; chưa ghi kết quả hoặc đánh[x] trước khi process hoàn tất.

### Kết quả cuối Phase 10 trong lượt này

- Lệnh nền `mvnw.cmd clean verify` đã kết thúc exit1, BUILD FAILURE,04:06phút.392test đơn vị/kiến trúc xanh.1063IT có6failure+9error,0skip. Đối chiếu XML failsafe:218IT M03 xanh (205ca có trước+13ca mới Phase10); ApiContractIT và hồi quy suitability xanh. Spotless check1322file,0file cần sửa. `git diff --check` không báo lỗi khoảng trắng; các cảnh báo chuyển LF/CRLF không phải test failure.
- 15lỗi vẫn ở8lớpM01 đã bàn giaoB: BR_M01_01_TemporaryAbsenceOverdueIT(1error), BR_M01_02_ReassessmentNoticeIT(2error), CompleteAdmissionConsentIT(2error), CompleteAdmissionWishWarningsIT(3error), FR_023h_KhongDauNangLucIT(1failure), FR_034a_CompleteAdmissionIT(4failure), ResidentContractIT(1failure), StatusHistoryIT(1error). Không sửa source/testM01 theo quyết định giữ phầnB; không nới contract003 hoặc CHECK để làm test cũ xanh.
- Đã cập nhật kết quả cuối vào plan/tasks/team-plan. Giữ T057–T060[ ], không ghi toànPhase10 hoàn tất. Backend xác nhận thường, hai job và hai HTTP operation đã cài/kiểm xanh. Nhánh tự chuyển phân bổ đang chờ chỉ nối cổng có sẵn, còn thiếu provider thật T032–T033; hiện503/rollback, không hạ giường thành Trống giả. App nhân viên T060 chưa mở theo CLAUDE.md:12/mobile README. Kiểm với nguồn007/008/004 thật vẫn thuộc T063 khi các module bàn giao.
- Thứ tự tiếp theo: xử lý dependency T032–T033 của003 để có thể bổ sung kiểm chuyển ngay ở T057, hoàn thiện T058, chốt gate cho T059, rồi T060 phần app khi được mở. Nếu chỉ xét Phase10 thì task còn đầu tiên làT057; nếu được yêu cầu mở Phase11 thì bắt đầuT061, sau đóT062/T063/T064/T065 theo tasks.md. Không tự thực hiệnPhase11 hoặc sửa phầnB/D/E trong lượt này.
- Không commit/push/stage/đổi nhánh hoặc tạo PR; Working-history.md vẫn là file hiện có được bổ sung, không xóa lịch sử cũ. Không chụp màn hình vì không đổi UI; không chạy Flutter cho phần nhân viên chưa mở và không ghi đã kiểm mobile.

## 2026-10-25 — Phase 11 Spec 003, T061–T065

### Yêu cầu, phạm vi và thứ tự thực hiện

- Đầu lượt đã đọc AGENTS.md, báo sáu dòng chứa HungNX-nguyen ở mục8 team-plan: DayCareCapacityApi, review HTTP Phase8, nguồn007, review Phase9, review Phase10 và dependency T032–T033. Đọc tasks/ghi chú plan rồi README backend trước khi tra hợp đồng module1.8/1.8a/1.9, data-model11 và quickstart. Checkout đầu lượt chỉ có Working-history.md untracked; không tự đổi nhánh/commit/push.
- Chủ dự án duyệt riêng BedFlagView.bedId từ long sang Long để WITHOUT_BED trả null; giữ tên/trường khác và chữ ký thao tác. Ghi vào module-interfaces, không đặt id0/-1 hoặc tự đổi HTTP contract. Không thêm thư viện.
- T061 viết SC_008_BedReportingIT trước: lượt đầu có3error thiếu bean đúng và2error fixture CONFIRM_READY thiếu disinfection_required theo CHECK. Sửa riêng fixture, chạy lại `003-p11-reporting-red2.log`:5error đều thiếu BedStatusApi/BedStatisticsApi/BedHandoverAlertApi. Sau đó mới cài adapter/store/domain. `003-p11-reporting-green.log`:5IT+25kiến trúc xanh, exit0/50.274giây.
- T062 bổ sung metadata trigger cho hai bảng nhóm3 của003 vào test chung và test app/admin trên hàng thật đã commit. Không ghi các ca này là RED mới: migration/trigger đã có và từng được kiểm ở phase trước. ApiContractIT đã đăng ký003 nên không sửa thêm vô ích; giữ pending endpoint chuyển thường chưa cài.
- T063 đối chiếu lại đúng feature→module:004=m02,007=m05/m06,008=m09,019=m03;019 phầnm11 quaAPI E. Các gói m02/m05/m06/m09 chỉ có package-info, không tìm thấy provider production cài AllocationResidencySource/AllocationInfectionStatus/LocationReferences/BedAssetPort. Đính chính các ghi chú cũ đã gọi nhầm m07(006) hoặc m14(016); không xóa lịch sử cũ và không lấy tên gói nhầm để chứng minh T063. Kết luận thiếu nguồn thật vẫn được kiểm lại bằng đúng gói/interface, không tạo fake production.
- T064 tạo phép đo GET /beds theo tầng với100phòng/300giường, một warmup và10request. Lượt hẹp `003-p11-cross-cutting.log`:7IT+25kiến trúc xanh; mẫu96–256ms, trung vị đúng116.5ms. Log lượt đầu in phần tử giữa trên117ms; đã đổi phép tính trung vị số mẫu chẵn trước lượt cuối. Không nhầm phép đo server này với SC-005 có60người dùng/10lần thao tác Hành chính và trung vị<2phút.
- T065 rà bảng11 bằng class/method thật, thêm kiểm mức12loại thông báo FR-051 vào FR_024_BedEventsTest và assertion producer BedTransferred trong lệnh chuyển kiểm soát. `003-p11-traceability.log`:4unit sự kiện+25kiến trúc,12IT chuyển xanh. Các kiểm phủ hành vi sẵn có, không bịa RED mới, không coi producer là receiver005/011 đã cài. FR-051 thông báo003 được phân biệt với FR-051 lưu giữ000.
- Review cuối read-only theo executing-plans có2finding P2. Thêm2ca vào SC_008_BedReportingIT rồi chạy `003-p11-review-red.log`:7IT/2failure/0error, tái hiện scope đầu dải bị dùng cho cửa sổ về sau và ParameterApi.readAt còn chạy lúc consume Stream. Sửa scope theo người vừa rời của từng cửa sổ, REUSE không có người vừa rời; materialize cả deadline trong transaction trước khi trả Stream. `003-p11-review-green.log`:7IT+25kiến trúc xanh, exit0/48.887giây. Không có finding minor được báo, không yêu cầu re-review.

### File được tạo/sửa và quan hệ dữ liệu

| Thao tác | File | Mục đích |
| --- | --- | --- |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/bed/domain/BedReportingSnapshot.java` | Các record ảnh đọc State/Window/Policy/Flags nằm đúng domain, không khai trong persistence |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/bed/persistence/BedReportingStore.java` | SQL theo lô/window function cho snapshot, interval, cách ly, phạm vi người, dấu phân bổ và WITHOUT_BED |
| Tạo | `backend/src/main/java/com/cambae/backend/m03/internal/bed/service/BedReportingService.java` | Cài ba API dữ liệu đã công bố, lọc ScopeFilter, kiểm nguồn available, deadline lịch sử và materialize Stream |
| Sửa | `backend/src/main/java/com/cambae/backend/m03/api/dto/BedFlagView.java` | bedId nullable đã được duyệt, không bịa giường cho người chưa có giường |
| Tạo | `backend/src/test/java/com/cambae/backend/m03/SC_008_BedReportingIT.java` |7ca API thật003, scope, state tại thời điểm, availability/cách ly, lượt Trống, interval/deadline và2fix review |
| Tạo | `backend/src/test/java/com/cambae/backend/m03/DBR_23_RecordTablesIT.java` | Chứng minh app/admin không sửa/xóa được cả hai bảng lịch sử trên hàng đã commit |
| Tạo | `backend/src/test/java/com/cambae/backend/m03/T064_BedListPerformanceIT.java` | Đo10request danh sách300giường sau warmup, assertion max<1000ms; không mô phỏng60người dùng |
| Sửa | `backend/src/test/java/com/cambae/backend/m15/RecordGroupTriggerIT.java` | Thêm đúng kiểm2bảng003, nhất là room_isolation_history SQL-only không được quét @Entity |
| Sửa | `backend/src/test/java/com/cambae/backend/m03/FR_024_BedEventsTest.java` | Kiểm LOW/MEDIUM/URGENT cho12loại theo FR-051/ma trận đã duyệt |
| Sửa | `backend/src/test/java/com/cambae/backend/m03/BR_M03_21_ControlledTransferIT.java` | Quan sát sự kiện từ lệnh thật, kiểm resident/old/new allocation và tầng, không chỉ tồn tại class sự kiện |
| Tạo | `specs/003-room-bed-allocation/verification.md` | Bảng quy tắc→test,37kịch bản quickstart theo nhóm, phép đo và phần thiếu; phân biệt bằng chứng với nghiệm thu |
| Sửa | `specs/003-room-bed-allocation/contracts/module-interfaces.md` | Ghi thay đổi nullable đã duyệt, không thêm thao tác API |
| Sửa | `specs/003-room-bed-allocation/quickstart.md` | Đính chính endpoint công tắc fake mới là thiết kế chưa cài; Q18 dùng422 đã duyệt Phase10 |
| Sửa | `specs/003-room-bed-allocation/plan.md` | Quyết định kỹ thuật, mapping nguồn đúng, TDD/review/fix/kiểm cuối và giới hạn |
| Sửa | `specs/003-room-bed-allocation/tasks.md` | Ghi triển khai thực tế T061–T065 và phần chưa đủ, giữ checkbox theo gate |
| Sửa | `specs/000-shared-foundation/glossary.md` | Bổ sung riêng thuật ngữ003Phase11, adapter/domain/test và bedId nullable |
| Sửa | `docs/team-plan.md` | Sửa mapping007 của dòng hiện hành, bàn giao API thật choA/E và receiver BedTransferred choC005/D011 |
| Bổ sung | `Working-history.md` | Lịch sử mới này, giữ toàn bộ nội dung trước |

- Không tạo/sửa migration, không xóa file nguồn. Không đổi pom/package/pubspec, source/testM01, APIclock000, frontend/src/api hoặc UI. Test chung củaA chỉ được mở rộng đúng phần003 được T062 yêu cầu; không nới kiểm của module khác. Flutter pub get không tạo diff manifest/lockfile trong Git.
- Luồng dữ liệu:008/016 kiểm quyền chức năng của mình→truyền ScopeFilter vào BedStatusApi/BedStatisticsApi/BedHandoverAlertApi→BedReportingService→BedReportingStore đọc lịch sử trạng thái, room isolation, v_current_allocation và resident_position→record domain→lọc phạm vi/tính deadline qua ParameterApi.readAt, kiểm cordon007 và assetflag019 khi đếm available→DTO API/Stream đã materialize. Không thêm endpoint, không tạo bản ghi nghiệp vụ hoặc mục bàn giao.
- countByState và stateAt dùng cùng snapshot, cùng occurred_at phân xử bằng id và không tính giường trước CREATE_BED. Khoảng dùng nửa mở và cắt vào cửa sổ hỏi; scope theo từng Window, không theo người chiếm giường ở đầu toàn dải. Lượt Trống đi qua hold WAITING_RECORD nhưng bị cắt khi sử dụng/bảo trì/tạm ngừng/ngừng dùng. byFloor chỉ trả dấu chờ bác sĩ đang mở, không đưa giường quá hạn vào mục bàn giao tự sinh.
- Quyết định kỹ thuật: dùng adapter chung và đọc theo lô để ba API không tự tính số liệu khác nhau; trả Stream từ list đã dựng trong transaction; deadline lấy CFG tại lúc vào Chờ sẵn sàng. Không bịa lịch sử cấu hình/tài sản/quan hệ phòng khi schema/SPI chưa cung cấp; verification.md ghi rõ giới hạn quá khứ của các nguồn hiện hành. Việc thiếu dữ liệu đó không được che bằng kết quả snapshot state xanh.

### Lệnh, kết quả cuối và phần chưa giải quyết

- Các lệnh backend dùng Maven wrapper/JDK25/MySQL8 thật và biến môi trường test đã được nạp, không ghi mật khẩu trong tài liệu. Lệnh scoped T061: `mvnw.cmd test -Dtest=*ArchTest,ModuleBoundaryTest failsafe:integration-test failsafe:verify -Dit.test=SC_008_BedReportingIT`; lượt xanh thêm spotless:apply. T062/T064: cùng ArchTests, `-Dit.test=DBR_23_RecordTablesIT,RecordGroupTriggerIT,ApiContractIT,T064_BedListPerformanceIT`. T065: unit FR_024_BedEventsTest cùng ArchTests, IT BR_M03_21_ControlledTransferIT. Sau review chỉ kiểm lại test liên quan cùng ArchTests, rồi chạy clean verify cuối.
- `npm test -- --run`:250test/38file xanh (`003-p11-web-test.log`); `npm run lint` qua; `npm run build` qua trong933ms, cảnh báo chunk>500kB còn. Không đổi UI nên không chụp màn hình.
- Đã chạy Flutter SDK có sẵn `E:/DevFlutter/flutter_windows_3.41.9-stable/flutter/bin/flutter.bat test`. Dependency đã khai báo được giải quyết/tải xong nhưng lệnh exit1 trước khi chạy test: Windows yêu cầu hỗ trợ symlink/Developer Mode. Log `003-p11-flutter-test.log`. Không tự bật cài đặt hệ thống, không ghi test mobile xanh, không cài thư viện mới vào manifest.
- Lệnh nền cuối `mvnw.cmd clean verify`, log `C:/Users/nguye/AppData/Local/Temp/003-p11-clean-verify-final.log`, đã kết thúc exit1/04:09phút.393unit/kiến trúc xanh;1073IT có6failure+9error,0skip; đọc XML xác nhận227IT M03 xanh (218cũ+9mới). Spotless check1328file,0cần sửa. Lượt hiệu năng cuối mẫu67–85ms, trung vị77.5ms,10/10request<1giây. Đây là phép đo server qua MockMvc, không phải SC-005 thực địa hoặc độ trễ trình duyệt/mạng.
- 15lỗi toàn kho vẫn thuộc8lớpM01 đã bàn giaoB: BR_M01_01_TemporaryAbsenceOverdueIT(1error), BR_M01_02_ReassessmentNoticeIT(2error), CompleteAdmissionConsentIT(2error), CompleteAdmissionWishWarningsIT(3error), FR_023h_KhongDauNangLucIT(1failure), FR_034a_CompleteAdmissionIT(4failure), ResidentContractIT(1failure), StatusHistoryIT(1error). Không sửa phầnB hoặc xóa test để xanh; không gọi kiểm toàn kho là thành công.
- Giữ T061–T065[ ]. T061 đã có implementation/test, còn gate và giới hạn lịch sử; T062 kiểm endpoint đã cài và2bảng, chuyển thường vẫn pending ở task gốc; T063 chờ nguồn thật004/007/008/019; T064 còn Flutter/môi trường, demo đủ37kịch bản và SC-00560người dùng/10lần thao tác; T065 mapping đã đối chiếu nhưng các luồng thiếu không được ghi thành đủ kiểm toàn phần. Các dependencyT032–T033, receiver005/011, listener007 và mobile nhân viên chưa mở vẫn giữ đúng trạng thái.
- Thứ tự tiếp theo trong Phase11: T061 chốt dữ liệu/gate→T062→T063 khi có nguồn thật→T064 môi trường/demo/SC-005→T065 đóng khoảng trống sau khi task nguồn được hoàn thiện. Không có Phase12 trong tasks.md. Không tự chuyển sang feature khác, không commit/push/stage/tạoPR/đổi nhánh hoặc sửa phầnB/D/E ngoài các dòng bàn giao tài liệu.

## 2026-10-25 — Rà soát toàn bộ task còn mở của Spec 003

- Yêu cầu: kiểm tra các task còn sót và chỉ ra lý do, không tự triển khai tiếp phase hoặc đổi hợp đồng.
- Trình tự: đọc trạng thái checkbox trong tasks.md; đối chiếu ghi chú plan.md, verification.md, các dòng có HungNX-nguyen ở mục 8 team-plan; kiểm source các nhánh còn thiếu, API/facade/listener, provider của các module liên quan; lập bảng đủ từng task và đối chiếu lại danh sách bằng PowerShell.
- Tạo specs/003-room-bed-allocation/remaining-tasks-audit.md: 45 dòng tương ứng đúng 45 task [ ] trong 65 task; 20 task đã [x], Phase 1–2 không còn task mở; Phase 6 T037/T041 đã [x]. Báo cáo liên kết task với code, test và nguồn dữ liệu cần thiết; phân biệt chưa cài, đã cài/chưa qua gate, chờ nguồn thật, quyết định contract/mobile và nghiệm thu môi trường/demo.
- Sửa Working-history.md bằng cách bổ sung mục này, giữ nguyên lịch sử trước. Không tạo/sửa/xóa file code, contract, migration hoặc checkbox trong lượt này. Các thay đổi Phase 11 đã tồn tại trong working tree không phải thay đổi mới của lượt rà soát.
- Phát hiện quan trọng: ResidentStatusApiImpl.statusPeriods() thực tế ném UnsupportedOperationException, không phải trả danh sách rỗng; BedAllocationCommandApi chưa có bean production; listener phù hợp đã nghe ba sự kiện M01 nhưng chưa có sự kiện đổi loại lưu trú 004. BedService.update đổi phòng giường chưa từng phân bổ trả 503 do chưa nối đồng bộ tài sản. Đây là bằng chứng source cho nguyên nhân, không bịa kết quả test mới.
- Quyết định: không coi 45 checkbox trống là 45 task chưa viết; không gộp tất cả thành phụ thuộc spec khác; 019 thuộc C/m03, 007 thuộc D/m05/m06, 008 thuộc E/m09. Fakes hợp đồng dùng cho test riêng nhưng không thay được nghiệm thu bean thật T063.
- Kiểm tra tài liệu: PowerShell đếm 45 task mở và 45 dòng audit, Compare-Object không có chênh lệch; git diff --check exit 0, chỉ cảnh báo chuẩn hóa LF/CRLF trên một số file đã sửa từ trước. Không chạy lại Maven/npm/Flutter trong lượt chỉ rà soát. Kết quả 227 IT M03 xanh, 15 lỗi M01 và web 250 test xanh được dẫn lại có ghi rõ từ lượt Phase 11 trước, không ghi là chạy mới.
- Chưa giải quyết: chuyển thường/theo lịch, handler nhập bù, nguồn khoảng vắng M01, provider/sự kiện 004/007/008 và phần tài sản 019, các giới hạn contract/UI đã ghi, mobile nhân viên chưa mở, Flutter symlink, demo/SC-005, gate toàn kho. Không tự xin lại hay suy thành duyệt các đề nghị contract đã bị từ chối.
- Thứ tự tasks.md: task mở sớm nhất T012 → T015 → T017 cần chốt gate/nhánh còn thiếu, rồi các task Phase 4; phần implementation trọng tâm có thể tiếp tục theo T029 → T030 → T031 (test trước) → T032 → T033 → T034 → T035 → T036 theo dependency thực tế. Lượt này chỉ báo cáo, chưa bắt đầu các task đó, không commit/push/đổi nhánh.

## 2026-10-26 — Spec 005 Phase 1: khung M04 và bản giả SPI (T001–T002)

### Yêu cầu và trình tự thực hiện

1. Người dùng yêu cầu làm Phase1 theo AGENTS/team-plan cho HungNX-nguyen, test trước, sau task chỉ test task cùng ArchTest; cuối phase clean verify chạy nền rồi mới đánh checkbox; chỉ hỏi nghiệp vụ/đổi hợp đồng/thêm thư viện/commit-push. Bổ sung lịch sử tiếng Việt, không chụp màn hình khi không sửa UI.
2. Đọc AGENTS, team-plan mục1/8, CLAUDE, constitution, backend README; đọc tasks và plan005 trước khi tra hợp đồng module2.1, researchC01/C02/C17, data-model/quickstart, glossary. Plan chưa có Ghi chú triển khai nên tạo mục mới. Chỉ tra FR029 của spec005 khi cần xác định hai việc bàn giao trên checklist; không sửa spec hoặc tài liệu nghiệp vụ.
3. Đầu phiên báo 7 dòng ghi rõ HungNX-nguyen cùng các dòng C liên quan005 trong mục8; các việc 003/provider, BedTransferred và thống kê005 không được coi đã hoàn tất ở Phase1. Git đầu lượt trên feature/003-manage-bedroom, Working-history.md và specs/003-room-bed-allocation/hand-over-task-003.md đã untracked. Giữ nhánh, không stage/commit/push/rebase/merge; không đụng file hand-over của003.
4. T001: viết T001_ModuleSetupTest trước, chạy Maven. Lượt mặc định có lỗi JDK24/release25 và sandbox chặn cache lock .m2; không tính đó là test RED. Tìm JDK25 sẵn có ở E:/JAVA/JDK_GE/INS_25 qua lịch sử cũ, chỉ đặt JAVA_HOME cho process và chạy với quyền dùng cache Maven. T001 RED thật:12failure/0error do12package descriptor chưa có. Sau đó tạo12package-info theo tên tiếng Anh/README. GREEN:40test gồm12T001+28kiến trúc,0failure/error.
5. T002 phụ thuộc SPI chỉ dự kiến khai báo ở T003: tự quyết đưa riêng8SPI và kiểu chỉ SPI dùng lên T002, cập nhật tasks/plan cùng mã; không đánh xong T003 hoặc kéo API/story khác vào Phase1. Thiết kế chỉ có endpoint công tắc tên logic tiếng Việt, chưa có hợp đồng: hỏi đúng phạm vi API. Chủ dự án chọn “1”, duyệt PUT /api/v1/testing/fakes/{module}, body enabled boolean,204;6module006/007/008/011/012/014, chỉ import test/demo.
6. Viết8ca T002_FakeM04Test và2ca T002_FakeSwitchHttpTest trước implementation. Lượt RED thất bại biên dịch vì chưa có SPI/fake. Cài8SPI và8loại fake,12provider bean; controller/công tắc/cấu hình ở test classpath. Lượt xanh đầu còn lỗi tham chiếu Measurement trong test; sửa thành FakeVitalSigns.Measurement (không sửa hành vi để né test). GREEN2:38test gồm10T002+28kiến trúc.
7. Viết T002_FakeSwitchContractTest trước khi thêm operation vào YAML; RED thật1failure/0error vì thiếu path. Sau đó thêm duy nhất operation đã được duyệt; chạy spotless:apply và test T002 cùng kiến trúc:39test gồm11T002+28kiến trúc xanh,0skip.
8. Áp dụng skill using-superpowers, brainstorming với quyền tự quyết kỹ thuật đã có của người dùng, executing-plans/TDD, systematic-debugging cho lỗi môi trường/biên dịch, verification-before-completion và requesting-code-review. Reviewer độc lập chỉ đọc đã đọc cả file untracked, không phát hiện Critical/Important/Minor trong T001/T002, không sửa hoặc chạy full suite. Không tạo commit theo các bước mẫu skill vì người dùng chưa cho phép.
9. Khởi động mvnw.cmd clean verify ở tiến trình nền qua exec session, nạp riêng TEST_DB_* từ backend/.env vào process, không in giá trị/ghi bí mật vào lịch sử. Lệnh đã kết thúc exit1/04:12phút; kết quả chi tiết ghi bên dưới bằng XML/log thật. Đối chiếu thêm skill karpathy-guidelines trong .claude/skills theo CLAUDE và rà phạm vi thay đổi; tạo .specify/feature.json cục bộ đang Git ignore để trỏ feature005.

### Danh sách file và mục đích

Đường dẫn dưới đây tính từ thư mục gốc project. Không xóa file nào; m04/package-info.java đã có từ trước và không sửa.

| Thao tác | File | Mục đích |
| --- | --- | --- |
| Tạo (bỏ qua bởi Git) | `.specify/feature.json` | Chọn `specs/005-daily-care-tasks` cho công cụ Spec Kit trên máy này theo CLAUDE.md; trước lượt này file chưa tồn tại |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/package-info.java` | NamedInterface công khai daily-care |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/careplan/package-info.java` | khung kế hoạch chăm sóc |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/attendance/package-info.java` | khung có mặt/điểm danh bán trú |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/task/package-info.java` | khung lập và vòng đời công việc |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/recording/package-info.java` | khung ghi nhận kết quả/đính chính |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/checklist/package-info.java` | khung checklist/phân công |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/catalog/package-info.java` | khung danh mục |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/rules/package-info.java` | khung đánh giá kết quả |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/overdue/package-info.java` | khung nhắc/xử lý quá hạn |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/timetable/package-info.java` | khung thời khóa biểu |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/status/package-info.java` | khung tham gia lệnh trạng thái |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/scheduling/package-info.java` | khung job đúng tên chuẩn README |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/spi/package-info.java` | NamedInterface daily-care-spi cho module ngoài cài nguồn |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/spi/ShiftStaffPort.java` | 11 phương thức ca, phân công, vai trò/chứng chỉ và bàn giao; các view chỉ phục vụ SPI |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/spi/TaskSourcePort.java` | loại căn cứ, kết quả kiểm và xác nhận phục vụ; giữ default true theo thiết kế |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/spi/VitalSignPort.java` | kết quả đồng bộ SAVED/BLOCKED/AWAITING_REMEASUREMENT, giữ đủ đối số và Optional thời điểm thiết bị |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/spi/MedicationChecklistPort.java` | liều thuốc chỉ đọc, projection DoseView giữ trường 006 mục1.3 |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/spi/AlertReadPort.java` | đọc cảnh báo mở theo người và liên kết theo công việc |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/spi/DayCarePickupPort.java` | kiểm bản ghi đón và đánh dấu dùng, contract lệnh MANDATORY của provider thật |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/spi/QualityCheckPort.java` | kết quả Không đạt kèm shiftId để kiểm làm lại |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/spi/TimetableSourcePort.java` | mục thời khóa biểu theo người/ngày cho các nguồn |
| Tạo | `backend/src/test/java/com/cambae/backend/m04/T001_ModuleSetupTest.java` | 12 ca descriptor của khung package đã biên dịch |
| Tạo | `backend/src/test/java/com/cambae/backend/support/fake/m04/FakeShiftStaff.java` | fixture 008 theo ca/người/thời điểm/chứng chỉ, lọc tầng và ca đã công bố |
| Tạo | `backend/src/test/java/com/cambae/backend/support/fake/m04/FakeTaskSource.java` | fixture riêng 007/011/014, kiểm đúng loại+căn cứ+người+thời điểm, không mặc định căn cứ hợp lệ |
| Tạo | `backend/src/test/java/com/cambae/backend/support/fake/m04/FakeVitalSigns.java` | fixture kết quả đo đặt trước và danh sách Measurement đã gọi |
| Tạo | `backend/src/test/java/com/cambae/backend/support/fake/m04/FakeMedicationChecklist.java` | fixture 006 lọc người và thời điểm dự kiến trong khoảng nửa mở |
| Tạo | `backend/src/test/java/com/cambae/backend/support/fake/m04/FakeAlertReader.java` | fixture 007 lọc người và các id công việc yêu cầu |
| Tạo | `backend/src/test/java/com/cambae/backend/support/fake/m04/FakeDayCarePickups.java` | fixture 012 theo bản ghi/người/thời điểm, chặn dùng lại, lưu attendanceRecordId |
| Tạo | `backend/src/test/java/com/cambae/backend/support/fake/m04/FakeQualityChecks.java` | fixture 014 theo cặp kết quả/công việc gốc |
| Tạo | `backend/src/test/java/com/cambae/backend/support/fake/m04/FakeTimetableSource.java` | fixture riêng 006/012/014 theo người/ngày |
| Tạo | `backend/src/test/java/com/cambae/backend/support/fake/m04/FakeM04Switches.java` | công tắc chung theo 6 module, mặc định bật, tắt gây lỗi rõ nguồn |
| Tạo | `backend/src/test/java/com/cambae/backend/support/fake/m04/FakeM04Controller.java` | route PUT đã duyệt, validation enabled và module, chỉ bean import/test component |
| Tạo | `backend/src/test/java/com/cambae/backend/support/fake/m04/FakeM04Config.java` | cấu hình opt-in test/demo, 12 bean provider cho 8 SPI, công tắc và controller |
| Tạo | `backend/src/test/java/com/cambae/backend/support/fake/m04/T002_FakeM04Test.java` | 8 ca phạm vi dữ liệu, default/reset, kết quả đo, dùng lại bản ghi đón, config/profile, tắt module |
| Tạo | `backend/src/test/java/com/cambae/backend/support/fake/m04/T002_FakeSwitchHttpTest.java` | 2 ca HTTP/body/6module và absence không import/production |
| Tạo | `backend/src/test/java/com/cambae/backend/support/fake/m04/T002_FakeSwitchContractTest.java` | 1 ca parse OpenAPI để kiểm route, module, enabled, 204/400 |
| Bổ sung/sửa | `specs/005-daily-care-tasks/contracts/api-005.yaml` | thêm duy nhất operation công tắc được duyệt; các operation nghiệp vụ chưa dịch giữ nguyên |
| Bổ sung/sửa | `specs/005-daily-care-tasks/contracts/module-interfaces.md` | ghi tên triển khai, projection view, thứ tự đưa SPI lên T002 và giới hạn fake/transaction |
| Bổ sung/sửa | `specs/005-daily-care-tasks/tasks.md` | T001 tên package chuẩn, T002 vị trí fake/công tắc và khai báo SPI trước T003; trạng thái theo gate |
| Bổ sung/sửa | `specs/005-daily-care-tasks/plan.md` | thêm Ghi chú triển khai, quyết định kỹ thuật, TDD và kết quả kiểm cuối |
| Bổ sung/sửa | `specs/005-daily-care-tasks/quickstart.md` | cách import fake, reset, công tắc mới; phân biệt demo bình thường với test classpath opt-in |
| Bổ sung/sửa | `specs/000-shared-foundation/glossary.md` | chỉ mục11 Feature005: package, 8SPI/phương thức, view, enum, lỗi dùng lại và công tắc |
| Bổ sung/sửa | `docs/team-plan.md` | thêm dòng C005 bàn giao khai báo SPI/fake cho D/E/C014, chưa ghi tích hợp nguồn thật |
| Bổ sung/sửa | `Working-history.md` | bổ sung lượt triển khai thực tế này, giữ toàn bộ nội dung cũ |

### Quan hệ giữa file, luồng dữ liệu và quyết định kỹ thuật

- Module nguồn006/007/008/011/012/014 sẽ cài SPI do M04 sở hữu trong api/spi; M04 không import API hay đọc bảng module nguồn để tránh vòng phụ thuộc. Role tái dùng common.Role. Những view chỉ SPI dùng đặt lồng đúng interface theo README; chưa tạo api/dto/events hoặc các tầng triển khai rỗng. Chưa có service nghiệp vụ, migration, job hoặc endpoint nghiệp vụ chăm sóc.
- T001 yêu cầu rõ tạo khung nên có package-info chức năng dù README thường chỉ tạo package khi có lớp. api và api.spi dùng NamedInterface; package chức năng dùng NullMarked để compiler có descriptor/quy ước không-null. Các lớp Phase2/story về sau phải đặt đúng internal/.../service/domain/persistence/config hoặc web/request/response/scheduling, không dùng tên logic tiếng Việt làm đường dẫn mã.
- Test consumer → import FakeM04Config (profile test/demo) → tiêm8SPI:6SPI một nguồn dùng Primary; TaskSourcePort gồm3bean health/meal/activity; TimetableSourcePort gồm3bean medication/pickup/activity. Fixture do test đặt vào từng fake theo khóa đối số; query chỉ lọc/trả dữ liệu đã đặt. Các nguồn nhiều bean cần chọn cấu hình test phù hợp khi có provider thật, không biến fake thành fallback production.
- HTTP request PUT /api/v1/testing/fakes/{module} → FakeM04Controller validation → FakeM04Switches.enabled → toàn bộ fake thuộc module đó kiểm requireEnabled trước đọc/lệnh. Tắt gây IllegalStateException rõ nguồn; bật lại giữ dữ liệu fixture. Body thiếu/null enabled hoặc module lạ trả400; thành công204 không body. TestComponent/TestConfiguration và import tường minh ngăn fake/controller tự quét vào ứng dụng khác; JAR production không chứa src/test.
- Fixture mặc định không có ca/phân công/cảnh báo/liều/kết quả kiểm/thời khóa biểu; qualification mặc định false; căn cứ chưa đặt không hợp lệ. confirmServing giữ default true của hợp đồng thiết kế. FakeVitalSigns không tự chấp nhận chỉ số: phải đặt RecordingResult tương ứng đủ7đối số, lưu Measurement đã gọi. DayCarePickupPort lưu dấu dùng lại và lỗi PICKUP_RECORD_ALREADY_USED; không tự xác minh người đón hoặc quy tắc tự về. reset xóa dữ liệu/dấu gọi của fake, không thay công tắc.
- Ca đã công bố lọc tầng và khoảng nửa mở; shiftContaining loại mốc kết thúc. Liều thuốc lọc người/thời điểm dự kiến, không là căn cứ lập công việc. View SPI là projection tối thiểu ghi rõ trong module-interfaces, không tự nhận là toàn bộ model nguồn. Fake bộ nhớ không mô phỏng transaction rollback, MANDATORY của record/markUsed phải kiểm với provider thật khi tích hợp.
- Không thêm thư viện, đổi pom/package/pubspec/common, test/sourceM01, migration, client sinh tự động hoặc UI. Không chạy npm/Flutter và không chụp màn hình vì phạm vi chỉ khung/SPI backend cùng fixture.

### Lệnh và bằng chứng scoped

Các lệnh dưới chạy từ backend, JDK25 qua JAVA_HOME của process, không đổi cấu hình máy. Log ở C:/Users/nguye/.codex/visualizations/2026/10/26/01a176d7-6a54-7db1-b0ab-d145a335d1e4/.

| Lệnh mvnw.cmd | Log | Kết quả quan sát |
| --- | --- | --- |
| test -Dtest=T001_ModuleSetupTest | 005-t001-red.log | exit1;12failure,0error, thiếu12package descriptor |
| test -Dtest=T001_ModuleSetupTest,*ArchTest,ModuleBoundaryTest | 005-t001-green.log | exit0;40test,0failure/error/skip |
| test -Dtest=T002_FakeM04Test,T002_FakeSwitchHttpTest | 005-t002-red.log | exit1 trước test, compilation báo thiếu SPI/fake |
| spotless:apply test -Dtest=T002_FakeM04Test,T002_FakeSwitchHttpTest,*ArchTest,ModuleBoundaryTest | 005-t002-green.log | exit1 compilation vì tham chiếu Measurement chưa đúng trong test, không ghi xanh |
| cùng lệnh sau sửa tham chiếu | 005-t002-green2.log | exit0;38test,0failure/error/skip |
| test -Dtest=T002_FakeSwitchContractTest | 005-switch-contract-red.log | exit1;1failure/0error, thiếu path trước sửa YAML |
| spotless:apply test -Dtest=T002_FakeM04Test,T002_FakeSwitchHttpTest,T002_FakeSwitchContractTest,*ArchTest,ModuleBoundaryTest | 005-t002-final-scoped.log | exit0;39test,0failure/error/skip |
| git diff --check | kết quả trực tiếp | exit0 ở lượt kiểm trước clean verify |
| clean verify (chạy nền) | 005-p1-clean-verify.log; exit lưu 005-p1-clean-verify.exit | exit1/04:12phút;461unit/kiến trúc xanh;1098IT có6failure+9error,0skip;Spotless1417file sạch |

### Kết quả cuối và vấn đề chưa giải quyết

- Đọc XML surefire/failsafe sau clean verify, không chỉ dựa dòng log: 461 test đơn vị/kiến trúc 0failure/0error/0skip; 1098IT có6failure+9error/0skip. Cả23ca Phase1 mới xanh: T001_ModuleSetupTest12, T002_FakeM04Test8, T002_FakeSwitchHttpTest2, T002_FakeSwitchContractTest1. Không coi kết quả38/39scoped là tổng số test mới; chúng bao gồm28kiểm kiến trúc dùng chung.
- 15 lỗi đều thuộc8lớp M01: BR_M01_01_TemporaryAbsenceOverdueIT(1error), BR_M01_02_ReassessmentNoticeIT(2error), CompleteAdmissionConsentIT(2error), CompleteAdmissionWishWarningsIT(3error), FR_023h_KhongDauNangLucIT(1failure), FR_034a_CompleteAdmissionIT(4failure), ResidentContractIT(1failure), StatusHistoryIT(1error). Nguồn lưu trú/điều kiện trạng thái thiếu, regex capacity của001 bắt dữ liệu003 và contract001 nhận hai endpoint vị trí003 vẫn còn. Các nhóm lỗi này đã có dòng bàn giao B ở team-plan; không sửa source/test của B hoặc thay nguồn production để ép xanh.
- Spotless check1417file:0cần sửa. Kiểm JAR bằng JDK25 jar tf: không có support/fake/m04 hoặc FakeM04Controller. Reviewer độc lập read-only không báo finding Critical/Important/Minor trong phạm vi T001/T002. Không dispatch lại review hoặc chạy lại full suite khi không đổi mã sau đó.
- Cập nhật plan/tasks/team-plan bằng kết quả này; giữ T001–T002[ ] vì clean verify chưa xanh, chưa ghi “xong phase”. Bộ khung và fixture đã cài/test riêng xanh nhưng gate tích hợp toàn kho chưa đạt. Không có thư viện mới, thay đổi UI hoặc bí mật trong lịch sử.

### Việc còn lại và thứ tự tiếp theo

- Gate hiện còn15lỗi tích hợp M01 nêu trên, thuộc B; sau khi nguồn/fixture/contract được B sửa cần chạy lại clean verify mới có thể đánh[x]. Không dùng kết quả003 trước đây làm bằng chứng mới cho005.
- T001/T002 giữ checkbox trống theo gate. Sau gate Phase1, task tiếp theo đúng thứ tự Phase2 là T003 (API công khai còn lại; phần8SPI đã khai báo ởT002, không tạo lại), T004(sự kiện), T005(migration), T006(khóa/job), T007(danh mục), T007a(loại lý do), T008(provider), T009(kiến trúcM04); không tự triển khai Phase2 trong lượt này.
- Còn phải cài và kiểm provider thật của các module nguồn; chưa kiểm đầy đủ luồng005/003/011 hoặc tiếp nhận BedTransferred. API nghiệp vụ005 chưa dịch, cần duyệt hợp đồng trước triển khai endpoint. Chưa commit/push/tạoPR hoặc bàn giao qua merge.

## 2026-10-27 — Spec 005 Phase 2: phần độc lập đã cài, chờ quyết định quyền T007/T007a

### Yêu cầu, phạm vi và trình tự thực hiện

1. Người dùng yêu cầu Phase 2 theo AGENTS/team-plan cho HungNX-nguyen: báo việc đang chờ đầu phiên, đọc tasks/ghi chú plan trước, tự quyết kỹ thuật; chỉ hỏi nghiệp vụ, API, thư viện hoặc Git. Viết test trước, sau task chỉ kiểm task đó cùng ArchTest, cuối phase chạy clean verify nền rồi mới xét checkbox. Bổ sung lịch sử tiếng Việt, không chụp màn hình nếu không đổi UI.
2. Đọc AGENTS, team-plan, tasks005, ghi chú Phase1, README backend và các quy định đã đọc của constitution/CLAUDE. Áp dụng using-superpowers, executing-plans, TDD, verification-before-completion, karpathy-guidelines và review độc lập. Tra hợp đồng module, data-model4/10/11, researchC12/C13/C15/C16 theo task; chỉ tra nguồn nghiệp vụ khi phát hiện thiếu dòng quyền. Không sửa spec hoặc docs/business.
3. Đầu phiên báo 8 dòng có tên HungNX-nguyen. Nhánh hiện tại là feature/005-daily-care-and-hand-over; ban đầu chỉ có Working-history.md và specs/003-room-bed-allocation/hand-over-task-003.md untracked cũ. Giữ nhánh, không sửa file hand-over003, không stage/commit/push/merge/rebase. Không nhận việc nhánh đổi từ lượt trước là thao tác AI của lượt này.
4. T003: viết PublicCareApiTest trước, chạy đỏ do thiếu class; sau đó khai báo 11 API (có TaskTypeApi ở contracts1.2), DTO/enum và MANDATORY cho lệnh. Viết FakePublicApiTest trước fixture/config, thấy compilation đỏ, rồi mới cài. Fixture trả theo interface, method và đủ đối số; thiếu dữ liệu thì báo lỗi. Scoped cuối xanh 31 kiểm, gồm 3 ca mới và 28 kiểm kiến trúc lúc đó. Giữ tám SPI Phase1, chưa có adapter nghiệp vụ story.
5. T004: test khóa/mức và FR019 viết trước, đỏ do thiếu context/event, rồi cài 28 event và khuôn HasNotificationRequests. Scoped ban đầu xanh 30 kiểm. Chưa có producer/listener/outbox story để nhận đã kiểm việc giao sự kiện cuối luồng.
6. T005: viết 3 IT MySQL trước migration, thấy 3 lỗi SQL do chưa có bảng, rồi thêm V005_01__care_foundation.sql tạo đúng 5 bảng. Xanh 3 IT và kiến trúc: đủ 18 nhóm/10 loại kết quả, unique active_name, FK chứng chỉ, window nullable để dùng CFG khi cài story.
7. T006: test transaction/khóa người, rollback marker, chạy lại và chạy bù với Clock viết trước; đỏ do thiếu class. Cài khóa, hai frame job và các điểm đăng ký work, thêm đúng hai dòng vào SchedulerScopeArchTest theo hướng dẫn ngay trong test. Chưa đổi APIclock000. Lượt đầu xanh 3 IT và kiến trúc.
8. T007/T007a HTTP: tạo phase2-api-review.md, hỏi chủ dự án. User chọn phương án 1, duyệt bản dịch và hai operation result-values còn thiếu trong YAML. Viết FR_038_CatalogContractTest, thấy đỏ do thiếu path, rồi mới sửa YAML đúng 12 operation/schema; xanh 1 ca contract cùng 30 kiểm kiến trúc. Lượt chuẩn bị patch đầu không khớp CRLF nên dừng trước khi ghi; sau đó chuẩn hóa trong bộ nhớ và apply thành công. Không coi contract test là endpoint đã cài.
9. Phát hiện planC16 dùng LOAI_CONG_VIEC/CAU_HINH nhưng Function/Phụ lục27 không có dòng này. Tra mục19.3, Phụ lục27 và24.1: không có Q đang mở giải quyết. Đã hỏi chủ dự án chọn kiểm AC-01 trực tiếp kèm 9 method SELF_CHECKED như M13 hay chờ A công bố khóa. Chưa nhận câu trả lời cho điểm quyền; duyệt HTTP không được suy thành duyệt quyền. Chưa sửa Function/SELF_CHECKED/nghiệp vụ, chưa cài service/controller danh mục.
10. Trong lúc chờ, làm T008[P]/T009[P] độc lập. T008 từ 3 ca đỏ thiếu provider, bổ sung dữ liệu thật thành 4 ca đỏ rồi cài provider và thấy xanh. Rà yêu cầu mọi trường thỏa thuận thấy thiếu metadata: thêm test FR002a, thấy 1 failure/5 test trước khi bổ sung HEALTH cho định danh/người ghi/thời điểm; cuối xanh 5 IT. Fixture DDL care_plan/care_task do test tạo rồi dọn trên schema test, không là migration story production.
11. T009: viết test kiểm luật trước, đỏ do thiếu M04ArchitectureRules; cài hai ArchRule và kiểm bytecode cố ý vi phạm, ban đầu xanh 31 kiểm. T007a schema test lần đầu bị compilation T009 đang viết chặn nên không tính đó là RED schema; chạy lại mới thấy 2 lỗi SQL thiếu bảng.
12. T007a chỉ phần schema: sau RED SQL, thêm V005_01_1__non_performance_reason_type.sql. Dùng version số thay tên logic 01a để Flyway nhận được. Seed đúng dòng cố định, CHECK và trigger UPDATE/DELETE; xanh 2 IT. Chưa có service/controller hoặc kiểm quyền/HTTP cho task này.
13. Review độc lập chỉ đọc nêu hai Important: khóa sự kiện thiếu namespace và INSERT IGNORE có nguy cơ nâng shared lock thành deadlock khi dòng đã có. Viết test tái hiện trước sửa: 2 unit failure và 1 IT error Deadlock thật. Lượt RED này dùng maven.test.failure.ignore=true chỉ để chạy tiếp IT sau unit đỏ; XML vẫn đỏ nên BUILD SUCCESS/exit0 không được tính xanh. Sau đó tách test loại ngày xem lại và quan sát 3 failure riêng trước fix.
14. Finding Minor về bỏ sót step scheduler được nâng thành Important: khung job thực tế gọi qua CareMinuteWork/CareDailyWork nên T009 cần kiểm chính đường này. Thêm fixture BadStep ở ngoài package scheduling, thấy 1 failure/2 self-test. Sửa một lượt: hash namespace/mốc có SourceType/reviewDateType; upsert X-lock; predicate bao phủ work. Lượt cuối xanh 37 unit/kiến trúc và 4 IT liên quan; không re-review. Datasource-proxy đã có trong pom được dùng để đồng bộ interleaving MySQL, không thêm thư viện hoặc retry để che lỗi.
15. Chạy spotless:apply khi sửa Java, cuối lượt chạy nền mvnw.cmd clean verify qua runner ngoài kho. Dùng JDK25 E:/JAVA/JDK_GE/INS_25 và nạp riêng TEST_DB_* vào process, không in giá trị. Lệnh cuối không có cờ ignore. Đọc XML/log sau khi kết thúc rồi mới cập nhật trạng thái bên dưới.

### File được tạo/sửa và mục đích

Đường dẫn project tính từ gốc E:/LEARNING02/FPT_LEARNING/FPT_Semester9/SEP490/Elder_Care. Danh sách được lấy lại từ git ls-files --others và diff của chính lượt này; giữ các file cũ. Không xóa file source nào.

| Thao tác | File | Mục đích |
| --- | --- | --- |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/AbsenceTaskApi.java` | Khai báo CongViecVangMatApi, chữ ký tiếng Anh tương đương; chưa có adapter story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/CarePlanApi.java` | Khai báo KeHoachChamSocApi, chữ ký tiếng Anh tương đương; chưa có adapter story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/DayCareAttendanceApi.java` | Khai báo CoMatBanTruApi, chữ ký tiếng Anh tương đương; chưa có adapter story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/HandoverTaskApi.java` | Khai báo CongViecBanGiaoApi, chữ ký tiếng Anh tương đương; chưa có adapter story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/PublicCareResultApi.java` | Khai báo KetQuaChamSocCongApi, chữ ký tiếng Anh tương đương; chưa có adapter story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/ShiftTaskApi.java` | Khai báo CongViecCaApi, chữ ký tiếng Anh tương đương; chưa có adapter story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/TaskSourceApi.java` | Khai báo NguonCongViecApi, chữ ký tiếng Anh tương đương; chưa có adapter story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/TaskStatisticsApi.java` | Khai báo CongViecThongKeApi, chữ ký tiếng Anh tương đương; chưa có adapter story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/TaskTypeApi.java` | Khai báo LoaiCongViecApi, chữ ký tiếng Anh tương đương; chưa có adapter story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/TimetableApi.java` | Khai báo ThoiKhoaBieuApi, chữ ký tiếng Anh tương đương; chưa có adapter story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/VitalSignTaskApi.java` | Khai báo CongViecDoChiSoApi, chữ ký tiếng Anh tương đương; chưa có adapter story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/dto/AttendanceStatus.java` | Kiểu trao đổi AttendanceStatus theo hợp đồng module, không là DTO riêng cho HTTP |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/dto/AttendanceView.java` | Kiểu trao đổi AttendanceView theo hợp đồng module, không là DTO riêng cho HTTP |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/dto/CareEventContext.java` | Metadata sourceType/id/mốc/giờ/nhóm nhận, chép danh sách để sự kiện không đổi theo caller |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/dto/CarePlanItemView.java` | Kiểu trao đổi CarePlanItemView theo hợp đồng module, không là DTO riêng cho HTTP |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/dto/CarePlanStatus.java` | Kiểu trao đổi CarePlanStatus theo hợp đồng module, không là DTO riêng cho HTTP |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/dto/CarePlanView.java` | Kiểu trao đổi CarePlanView theo hợp đồng module, không là DTO riêng cho HTTP |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/dto/CareResultView.java` | Kiểu trao đổi CareResultView theo hợp đồng module, không là DTO riêng cho HTTP |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/dto/CareTimeRange.java` | Kiểu trao đổi CareTimeRange theo hợp đồng module, không là DTO riêng cho HTTP |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/dto/CheckoutType.java` | Kiểu trao đổi CheckoutType theo hợp đồng module, không là DTO riêng cho HTTP |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/dto/EatingDrinkingSummary.java` | Kiểu trao đổi EatingDrinkingSummary theo hợp đồng module, không là DTO riêng cho HTTP |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/dto/FamilyCareResultView.java` | Kiểu trao đổi FamilyCareResultView theo hợp đồng module, không là DTO riêng cho HTTP |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/dto/HandoverConfirmationResult.java` | Kiểu trao đổi HandoverConfirmationResult theo hợp đồng module, không là DTO riêng cho HTTP |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/dto/HandoverTaskView.java` | Kiểu trao đổi HandoverTaskView theo hợp đồng module, không là DTO riêng cho HTTP |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/dto/Importance.java` | Kiểu trao đổi Importance theo hợp đồng module, không là DTO riêng cho HTTP |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/dto/ResultType.java` | Kiểu trao đổi ResultType theo hợp đồng module, không là DTO riêng cho HTTP |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/dto/TaskReportView.java` | Kiểu trao đổi TaskReportView theo hợp đồng module, không là DTO riêng cho HTTP |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/dto/TaskStateChange.java` | Kiểu trao đổi TaskStateChange theo hợp đồng module, không là DTO riêng cho HTTP |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/dto/TaskStateView.java` | Kiểu trao đổi TaskStateView theo hợp đồng module, không là DTO riêng cho HTTP |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/dto/TaskStatus.java` | Kiểu trao đổi TaskStatus theo hợp đồng module, không là DTO riêng cho HTTP |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/dto/TaskTypeRequirements.java` | Kiểu trao đổi TaskTypeRequirements theo hợp đồng module, không là DTO riêng cho HTTP |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/dto/package-info.java` | NamedInterface daily-care-dto |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/AlertClosureRequested.java` | Record sự kiện YeuCauDongCanhBao; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/AlertRequested.java` | Record sự kiện YeuCauCanhBao; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/AttendanceChanged.java` | Record sự kiện TrangThaiCoMatDaDoi; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/AttendanceDetermined.java` | Record sự kiện TrangThaiCoMatDaXacDinh; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/BatchedOverdueTaskReminder.java` | Record sự kiện NhacQuaHanGop; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/BillableCareResultCorrected.java` | Record sự kiện KetQuaTinhPhiDaDinhChinh; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/BillableCareResultRecorded.java` | Record sự kiện KetQuaCoTinhPhi; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/CareEvent.java` | Tính khóa ổn định có namespace và mốc bằng SHA-256, tách khỏi giờ phát lại |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/CareEventNotifications.java` | Dựng NotificationRequest chuẩn chung, tiêu đề không có dữ liệu sức khỏe |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/CareNotice.java` | Giao diện HasNotificationRequests với mức và người cao tuổi của nguồn |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/CarePlanApprovalOverdue.java` | Record sự kiện PhienBanKeHoachQuaHanDuyet; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/CarePlanApprovalWithdrawn.java` | Record sự kiện PhienBanKeHoachDaThuHoi; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/CarePlanChanged.java` | Record sự kiện KeHoachDaDoi; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/CarePlanManagerEscalation.java` | Record sự kiện PhienBanKeHoachBaoQuanLyVien; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/CarePlanReturned.java` | Record sự kiện PhienBanKeHoachDaTraLai; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/CarePlanReviewNeeded.java` | Record sự kiện CanXemXetKeHoach; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/CarePlanSubmitted.java` | Record sự kiện PhienBanKeHoachDaGuiDuyet; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/DayCareCheckedIn.java` | Record sự kiện DaDiemDanhDen; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/DayCarePresentAtDayEnd.java` | Record sự kiện BanTruConCoMatHetNgay; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/MeasurementTaskMissed.java` | Record sự kiện CongViecDoBiBo; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/OverdueTaskNeedsHandling.java` | Record sự kiện CongViecQuaHanCanXuLy; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/OverdueTaskReminder.java` | Record sự kiện NhacQuaHan; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/ProlongedPoorEating.java` | Record sự kiện AnKemKeoDai; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/RiskAgreementReviewReminder.java` | Record sự kiện NhacXemLaiThoaThuan; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/SharedTaskWithoutQualifiedStaff.java` | Record sự kiện CongViecChungKhongAiDuDieuKien; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/ShiftWithoutTasks.java` | Record sự kiện CaChuaCoCongViec; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/SleepResultRecorded.java` | Record sự kiện KetQuaNguDaGhi; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/TaskBecameShared.java` | Record sự kiện CongViecThanhChung; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/TaskPlanningNeeded.java` | Record sự kiện CanLapCongViec; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/TaskSourceChanged.java` | Record sự kiện NguonCongViecDaDoi; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/UnreportedAbsence.java` | Record sự kiện VangKhongBao; payload và khóa chống lặp, chưa nối producer story |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/api/events/package-info.java` | NamedInterface daily-care-events |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/internal/governance/config/CareGovernanceConfiguration.java` | Đăng ký provider phân loại trường,4scope và12retention theo loại |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/internal/governance/domain/CareRetentionRow.java` | Ảnh đọc ứng viên id/người/thời điểm theo đúng domain layer |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/internal/governance/domain/CareRetentionType.java` | Ánh xạ12loại lưu giữ, tách profile và ba alias LOCATION cho CFG khác nhau |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/internal/governance/persistence/CareGovernanceStore.java` | SQL M04 cho scope/candidate/graph tham chiếu/delete bằng kết nối retention; phát hiện bảng story chưa tạo |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/internal/governance/service/CareFieldClassification.java` | Metadata trường HEALTH/INTERNAL_PUBLIC bất biến |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/internal/governance/service/CareRetention.java` | Mốc hồ sơ qua API001 hoặc plannedAt; materialize Stream, thiếu mốc thì giữ dữ liệu |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/internal/governance/service/CareScopeAssignment.java` | Gắn Target từ M04 và vị trí đọc qua API003; thiếu bản ghi không thành toàn viện |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/internal/scheduling/persistence/CareMilestoneStore.java` | Ghi marker ngày duy nhất theo job/mốc |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/internal/scheduling/service/CareDailyWork.java` | Điểm đăng ký các ngày còn thiếu do story trả từ nguồn thật |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/internal/scheduling/service/CareDailyWorkRunner.java` | Marker và tác động work cùng transaction, rollback cho phép thử lại |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/internal/scheduling/service/CareMinuteWork.java` | Điểm đăng ký bước theo phút cho các story sau, không sinh việc |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/internal/status/persistence/CareLockStore.java` | Upsert X-lock và SELECT FOR UPDATE theo ID đã sort/distinct, tránh lock upgrade |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/internal/status/service/CareResidentLock.java` | Lối khóa kỹ thuật chỉ chạy trong transaction MANDATORY của caller |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/scheduling/CareDailyJob.java` | Khung CARE_DAILY, sort/distinct ngày chạy bù và chống mốc tương lai |
| Tạo | `backend/src/main/java/com/cambae/backend/m04/scheduling/CareMinuteJob.java` | Khung job CARE_MINUTE, ShedLock/clock/startup và nhật ký scheduler_run |
| Tạo | `backend/src/main/resources/db/migration/V005_01_1__non_performance_reason_type.sql` | Migration danh mục lý do, seed cố định, CHECK/trigger bảo vệ và grant app; version số hợp lệ |
| Tạo | `backend/src/main/resources/db/migration/V005_01__care_foundation.sql` | Migration5bảng nền/catalog, active_name unique, enum18nhóm/10kếtquả và grant app |
| Tạo | `backend/src/test/java/com/cambae/backend/m04/FR_028a_NonPerformanceReasonTypeIT.java` | 2ca SQL bảo vệ dòng cố định và cho phép sửa/ngừng dòng khác; chưa kiểm HTTP |
| Tạo | `backend/src/test/java/com/cambae/backend/m04/FR_038_CatalogContractTest.java` | Kiểm12operation/field YAML đã duyệt; không thay bằng kiểm runtime endpoint |
| Tạo | `backend/src/test/java/com/cambae/backend/m04/M04ArchTest.java` | 2luật ứng dụng: chỉ qua SPI007/008, job và work không gọi tạo bản ghi |
| Tạo | `backend/src/test/java/com/cambae/backend/m04/M04ArchitectureRules.java` | Luật dùng chung cho bytecode ứng dụng và fixture vi phạm |
| Tạo | `backend/src/test/java/com/cambae/backend/m04/NFR_04_CareEventsTest.java` | 5ca về key/mức/giao lại/namespace/loại ngày xem lại và hai nhóm FR019 |
| Tạo | `backend/src/test/java/com/cambae/backend/m04/NFR_04_CareSchedulingIT.java` | 4ca MySQL khóa mới/có sẵn đồng thời, MANDATORY, rollback marker và chạy bù clock thật được tiêm |
| Tạo | `backend/src/test/java/com/cambae/backend/m04/T003_PublicCareApiTest.java` | Kiểm public interface/số phương thức và MANDATORY của các lệnh hợp đồng |
| Tạo | `backend/src/test/java/com/cambae/backend/m04/T005_FoundationSchemaIT.java` | 3ca MySQL về18nhóm, nullable CFG-window, unique active_name, FK/khóa kỹ thuật |
| Tạo | `backend/src/test/java/com/cambae/backend/m04/T008_CareGovernanceIT.java` | 5ca che trường/metadata/scope thiếu nguồn/lưu giữ với hàng thật và tài khoản retention; fixture bảng story được dọn |
| Tạo | `backend/src/test/java/com/cambae/backend/m04/T009_ArchitectureRulesTest.java` | 2ca biên dịch bytecode xấu trong TempDir để kiểm luật job trực tiếp và qua work |
| Tạo | `backend/src/test/java/com/cambae/backend/support/fake/m04/FakeM04ApiConfig.java` | Cấu hình test/demo opt-in cho11API công bố, không nạp production |
| Tạo | `backend/src/test/java/com/cambae/backend/support/fake/m04/FakeM04ApiFixture.java` | Fixture theo interface/method/đủ đối số, Supplier cho Stream mới; không có cấu hình thì lỗi |
| Tạo | `backend/src/test/java/com/cambae/backend/support/fake/m04/T003_FakePublicApiTest.java` | Kiểm11bean API, phạm vi fixture theo đối số, fail-closed lệnh và reset |
| Tạo | `specs/005-daily-care-tasks/contracts/phase2-api-review.md` | Bản dịch và đề nghị2operation, ghi user đã chọnphươngán1 |
| Tạo | `specs/005-daily-care-tasks/contracts/phase2-permission-review.md` | Bằng chứng thiếu dòng quyền,2phươngán và9method dự kiến; câu hỏi còn chờ |
| Sửa/bổ sung | `backend/src/test/java/com/cambae/backend/common/scheduling/SchedulerScopeArchTest.java` | Chỉ thêm2dòng đăng ký job M04 theo hướng dẫn ngay trong test, chờ A review |
| Sửa/bổ sung | `specs/005-daily-care-tasks/contracts/api-005.yaml` | Bản dịch12operation danh mục được duyệt, thêm2operation result-values, schema lỗi/Reason tiếng Anh riêng danh mục |
| Sửa/bổ sung | `specs/005-daily-care-tasks/contracts/module-interfaces.md` | Ánh xạ khai báo API/event, cách dùng fake, eventcontext và phân biệt khai báo với thực thi |
| Sửa/bổ sung | `specs/005-daily-care-tasks/plan.md` | Ghi chú Phase2, quyết định kỹ thuật/điểm quyền, TDD/review/gate và giới hạn thực tế |
| Sửa/bổ sung | `specs/005-daily-care-tasks/tasks.md` | Ghi tiến độ từng phần/schema thật, giữ checkbox, thứ tự tiếp T007→T007a trướcPhase3 |
| Sửa/bổ sung | `specs/000-shared-foundation/glossary.md` | Chỉ mục11.2 của005: API/method/DTO/event/enum/bảng/cột/tên dự kiến cho provider |
| Sửa/bổ sung | `docs/team-plan.md` | Thêm2dòng: chủ dự án quyết quyền; A review khai báoAPI/event/2job; không đánh nguồn thật hay Phase2 xong |
| Sửa/bổ sung | `Working-history.md` | Bổ sung mục thực hiện thực tế này, giữ nguyên lịch sử cũ |
| Tạo ngoài kho mã | `C:/Users/nguye/.codex/visualizations/2026/10/26/01a176d7-6a54-7db1-b0ab-d145a335d1e4/run-005-p2.ps1` | Runner dùng JDK25/nạp TEST_DB_* riêng process, ghi log và exit để kiểm nền; không sửa cấu hình máy |

### Quan hệ file, luồng dữ liệu và quyết định kỹ thuật

- Consumer module → API005 đã khai báo → provider thật sẽ cài theo story. Hiện có fixture test/demo khi import; không có provider business trả dữ liệu giả trong production. FakeM04ApiFixture dùng Supplier theo đủ đối số, reset xóa cấu hình. DTO ở api/dto; kiểu SPI Phase1 vẫn ở api/spi. Vai trò tái dùng common.Role, default_role dùng VARCHAR để tránh nhân bản enum Java vào DDL.
- Producer story tương lai → record event + CareEventContext → CareEvent.eventKey → CareNotice/NotificationRequest hoặc event dữ liệu cho 007/010/011 → outbox chung. Lượt này mới khai báo/khuôn, chưa kiểm producer rollback hoặc giao cuối luồng. Namespace phân biệt loại nguồn, SourceType và loại ngày xem lại; hash có độ dài cố định, giờ phát lại không làm đổi key. FR019 tách hai nhóm và hai mức, không gửi MEDIUM cho Trưởng tầng.
- Lệnh đang có transaction → CareResidentLock → CareLockStore → care_resident_lock. Distinct/sort ID tăng dần, upsert lấy X-lock ngay rồi SELECT FOR UPDATE; no-op không tăng version vô ích. Dòng kỹ thuật chỉ tạo khi lệnh gọi, không phải job tự lập công việc.
- Scheduler → CareMinuteJob/CareDailyJob → các work đăng ký dần ở story. SchedulerRuns ghi run/count/kết quả. Daily runner claim m04_job_milestone và work cùng transaction; lỗi rollback marker. dueDates do nguồn thật cung cấp, được sort/distinct; không tự đoán ngày khởi đầu. Nhắc/gộp nhắc/chuyển trạng thái còn chờ story, không ghi frame là toàn bộ C12 đã cài.
- GovernanceConfiguration đăng ký phân loại trường cho audit, scope cho002, retention cho000. Store chỉ đọc bảng M04; bảng story chưa có được nhận diện bằng information_schema, không nuốt lỗi SQL khác. Thiếu scope ném InvalidPermissionDataException thay vì Target.UNSPECIFIED/toàn viện; vị trí theo người chỉ đọc qua API003.
- Retention nhận cutoff từ job chung, đọc ResidentApi.finalStatusAt qua API001; thiếu nguồn/mốc thì giữ dữ liệu. Dữ liệu không gắn hồ sơ dùng plannedAt và các loại LOCATION_* với CFG-M15-04. Có 12 provider/dataType cho graph con; delete dùng RetentionDatabase/tài khoản retention, không appDataSource. Thành viên thỏa thuận composite được xóa theo agreement khi root được phép; audit/correction đi kèm do job chung xử lý theo entityType. Story sau phải dùng đúng alias khi ghi audit/correction, chưa có producer để chứng minh toàn luồng.
- Các tên bảng/cột story dự kiến được ghi trong glossary11.2. Migration story phải bổ sung đủ invariant và giữ đồng bộ mapping, không lấy fixture projection làm toàn bộ schema. Chưa đo hiệu năng quét ứng viên retention ở hàng triệu dòng; cần rà phân trang/chỉ mục khi có dữ liệu quy mô thật.
- YAML danh mục đã duyệt đủ 12 operation và schema tiếng Anh riêng. Các GET hiện chưa có version/ETag từng dòng dù PUT dùng If-Match; không tự thêm field, cần chốt trước khi nối UI. Đồng bộ các reference tới schema đã dịch; không coi sửa YAML là endpoint runtime đã chạy.
- Không đổi UI nên không chụp màn hình/chạy npm/Flutter. Không đổi pom/package/pubspec, common production, source/testM01, Function, ma trận quyền, spec/docsbusiness hoặc client sinh tự động. Hai dòng đăng ký Arch job là phần005 theo hướng dẫn test, đã ghi để A review. Không dùng công cụ gửi tin cho thành viên khác.

### Lệnh test/build và kết quả

Log và file .log.exit tại C:/Users/nguye/.codex/visualizations/2026/10/26/01a176d7-6a54-7db1-b0ab-d145a335d1e4/.
Runner gọi Maven wrapper từ backend. Lệnh IT có dạng: test -Dtest=*ArchTest,ModuleBoundaryTest failsafe:integration-test failsafe:verify -Dit.test=<class>. Lượt GREEN có spotless:apply nếu sửa Java. Không ghi giá trị kết nối/mật khẩu.

| Nhóm/lệnh | Log | Kết quả |
| --- | --- | --- |
| T003 API trước/sau | 005-p2-t003-red.log / t003-green.log | Đỏ thiếu class; xanh 30 kiểm (2 API + 28 Arch) |
| T003 fake trước/sau | 005-p2-t003-fake-red.log / t003-final.log | Đỏ thiếu fixture/config; xanh 31 kiểm (3 mới + 28 Arch) |
| T004 event trước/sau | 005-p2-t004-red.log, t004-red2.log / t004-green.log | Đỏ thiếu context/event; xanh 30 kiểm trước review |
| T005 schema | 005-p2-t005-red.log / t005-green.log | Đỏ 3 lỗi SQL; xanh 3 IT cùng kiến trúc |
| T006 frame/lock | 005-p2-t006-red.log / t006-green.log | Đỏ thiếu class; xanh 3 IT cùng kiến trúc trước review |
| T007 hợp đồng HTTP | 005-p2-t007-contract-red.log / t007-contract-green.log | Đỏ 1 failure thiếu path; xanh 1 contract + 30 Arch; chưa cài service/controller |
| T007a SQL | 005-p2-t007a-schema-red.log, schema-red2.log / schema-green.log | Lần1 bị T009 compilation chặn; lần2 đỏ 2 lỗi SQL; xanh 2 IT |
| T008 bootstrap/hàng thật | 005-p2-t008-red.log, red2.log / t008-green.log | Đỏ thiếu provider; xanh 4 IT cùng kiến trúc |
| T008 metadata thỏa thuận | 005-p2-t008-metadata-red.log / t008-final.log | Đỏ 1 failure/5 IT; xanh 5 IT cùng kiến trúc |
| T009 luật | 005-p2-t009-red.log / t009-green.log | Đỏ thiếu rule; xanh 31 kiểm ban đầu |
| Review RED nguồn/deadlock | 005-p2-review-red.log | 2 unit failure + 1 IT error Deadlock; ignore=true chỉ để thu đủ RED, không tính BUILD SUCCESS là xanh |
| Review RED loại khóa/ngày | 005-p2-events-review-red2.log | 5 test/3 failure/0 error, exit1 |
| Review RED step scheduler | 005-p2-t009-work-red.log | 2 self-test/1 failure, exit1 |
| Review GREEN không ignore | 005-p2-review-green.log | 37 unit/kiến trúc + 4 IT xanh, exit0/55.894s |
| clean verify nền, không ignore | 005-p2-clean-verify.log | exit1/04:32 phút; 474 unit/kiến trúc xanh; 1112 IT có 6 failure + 9 error, 0 skip; Spotless 1509 file sạch |
| git diff --check | Output trực tiếp | exit0 trước và sau cập nhật tài liệu |

### Kết quả cuối, vấn đề còn lại và thứ tự tiếp tục

- XML sau clean verify xác nhận 27 kiểm mới đều xanh: 13 unit/kiến trúc (T0032 + fake1 + event5 + contract1 + M04Arch2 + self-test2), 14 IT (schema3 + scheduler4 + governance5 + reasonSQL2). Không đếm Arch chung lặp lại thành test mới.
- Có 15 lỗi trong 8 lớp M01 đã bàn giao B: BR_M01_01_TemporaryAbsenceOverdueIT (1 error), BR_M01_02_ReassessmentNoticeIT (2 error), CompleteAdmissionConsentIT (2 error), CompleteAdmissionWishWarningsIT (3 error), FR_023h_KhongDauNangLucIT (1 failure), FR_034a_CompleteAdmissionIT (4 failure), ResidentContractIT (1 failure), StatusHistoryIT (1 error). Đây là kết quả lượt kiểm mới ngày2026-10-27; không dùng kết quả Phase1 làm bằng chứng mới. Không sửa B hoặc bỏ test để ép gate xanh.
- Phase2 chưa đủ T007/T007a service/controller và gate chưa qua, nên giữ T003–T009/T007a[ ]. Câu hỏi quyền còn chờ user. Thứ tự tiếp theo là T007 → T007a sau quyết định, viết test HTTP/quyền/audit/optimistic lock trước service. T008/T009 đã kiểm phần độc lập; sau khi nối danh mục cần kiểm mapping phát sinh. Sau khi làm đủ và B xử lý gate, chạy lại clean verify rồi mới xét checkbox.
- Phase2 đạt mới tới T010/schema kế hoạch của Phase3; không tự chuyển phase. Provider thật, producer API/event story, nhắc gộp, nguồn004/007/008/011/012 và receiver BedTransferred005/011 chưa được chứng minh toàn luồng ở lượt này.
- Chưa commit/push/tạoPR/merge hoặc đổi nhánh. Giữ nguyên thay đổi để người dùng xem. Nội dung Working-history.md trước mục này được bảo toàn, đây là mục bổ sung.
- Kiểm đọc cuối: cộng XML đúng 10 lớp kiểm mới cho kết quả 27 test/0 lỗi; `git diff --check` exit0. `jar tf backend/target/backend-0.0.1-SNAPSHOT.jar` có hai class job production, không có support/fake/m04. Nhánh vẫn feature/005-daily-care-and-hand-over. Các lệnh này chỉ xác minh artifact/phạm vi, không được tính thêm test mới.

### Tiếp tục ngày 2026-10-27 — đối chiếu điều kiện triển khai danh mục

- Nhận yêu cầu tiếp tục; đọc lại AGENTS.md, docs/team-plan.md mục 8, tasks.md, ghi chú plan.md, phase2-permission-review.md và các mẫu CatalogBase/StructureService cùng test hợp đồng hiện có.
- Kiểm `git status --short`: các thay đổi Phase 2 vẫn ở checkout; chưa có service/controller danh mục. Không ghi nhận thêm task hoàn tất hoặc kết quả test mới; lượt đối chiếu này không chạy Maven.
- Đã gửi lại câu hỏi lựa chọn kiểm AC-01 trực tiếp và đăng ký chín phương thức SELF_CHECKED, hoặc chờ A công bố khóa quyền. Yêu cầu "tiếp tục công việc" chưa chỉ rõ lựa chọn nghiệp vụ này; không suy thành chấp thuận phương án 1 và không sửa Function, SELF_CHECKED hay tài liệu nghiệp vụ.
- File sửa trong lượt này: Working-history.md, chỉ bổ sung lịch sử đối chiếu thực tế. Chưa thay đổi mã nguồn, hợp đồng, checkbox hoặc nhánh. Task kế tiếp vẫn T007, rồi T007a phần service/HTTP sau khi có quyết định quyền; gate clean verify còn theo kết quả đã ghi ở trên.

### Tiếp nhận Phase 3 ngày 2026-10-27 — chuẩn bị hợp đồng và ghi nhận quyết định quyền

- Chủ dự án trả lời phương án 2: chờ A công bố khóa quyền danh mục chính thức. Giữ quyết định này khi tiếp nhận yêu cầu Phase 3; không tự chuyển sang AC-01 trực tiếp hoặc thêm chín phương thức SELF_CHECKED.
- Đọc lại docs/team-plan.md mục 8 và báo mười dòng có tên HungNX-nguyen; đọc tasks.md, ghi chú triển khai plan.md trước, đối chiếu hợp đồng 005, DTO đã công bố, research C-05 và schema công khai của 001 khi phát hiện tham chiếu thiếu. Không tra hoặc sửa tài liệu nghiệp vụ/spec trong lượt chuẩn bị này.
- Kiểm `git status --short` đầu lượt chỉ có Working-history.md và specs/003-room-bed-allocation/hand-over-task-003.md untracked; phần mã Phase 2 đã nằm trong checkout được Git theo dõi. Không ghi nhận đây là commit do công cụ thực hiện. Nhánh kiểm bằng `git branch --show-current` vẫn feature/005-daily-care-and-hand-over; không sửa file bàn giao 003.
- tasks.md yêu cầu hoàn tất nền trước user story. T007/T007a service/HTTP chưa có, Function chưa có khóa danh mục; vì vậy chưa viết test hoặc implementation T010–T017, chưa chạy Maven và chưa đánh checkbox. Chuẩn bị hợp đồng là công việc thực tế đã thực hiện, không ghi thành hoàn tất Phase 3.
- Tạo specs/005-daily-care-tasks/contracts/phase3-api-review.md: bảng dịch 11 operation GET/POST/PUT kế hoạch, trường và enum, mã lỗi; giữ nguyên mã HTTP, header, tính bắt buộc và kiểu dữ liệu. Nêu hai tham chiếu NhanDinhTuQuyet/NguonYKien chưa có đích trong api-005.yaml; đề nghị dùng SelfDeterminationAssessment/OpinionSource đúng schema 001. Đây là đề nghị cần duyệt; chưa áp dụng YAML, chưa sinh client và chưa thay đổi API runtime.
- Sửa specs/005-daily-care-tasks/contracts/phase2-permission-review.md để ghi quyết định phương án 2. Sửa plan.md bằng mục ghi chú mới và tasks.md tại checkpoint để phản ánh phụ thuộc thực tế. Sửa docs/team-plan.md mục 8 chuyển việc cần làm sang A công bố khóa quyền; không đánh Xong dòng danh mục.
- Luồng phụ thuộc: khóa quyền từ A → lệnh service danh mục T007/T007a → nguồn loại công việc hợp lệ cho mục kế hoạch Phase 3; hợp đồng Phase 3 được duyệt → DTO/controller và client web. Chưa có luồng dữ liệu mới được cài ở lượt này.
- Không sửa Java/SQL/UI, không thêm thư viện, không chụp màn hình, không commit/push/đổi nhánh hoặc gửi tin cho thành viên khác. Không có kết quả build/test mới để báo; số liệu kiểm cũ chỉ giữ trong các mục lịch sử.
- Việc còn lại theo thứ tự tasks.md: A công bố quyền → hoàn thiện T007 → T007a → clean verify nền; sau đó T010 → T011 → T012–T017 (gồm T016a). Bản dịch Phase 3 và hai schema bổ sung đang chờ duyệt; gate toàn kho còn theo kết quả Phase 2 đã ghi, chưa chạy lại để kết luận thay đổi.
- Xác minh tài liệu: `git diff --check` exit 0; Git có cảnh báo chuẩn hóa LF/CRLF ở phase2-permission-review.md, không có lỗi whitespace. Đã gửi câu hỏi duyệt bản đề nghị Phase 3; chưa nhận quyết định trong lượt chuẩn bị này.

### Áp dụng phê duyệt hợp đồng Phase 3 ngày 2026-10-27

**Yêu cầu đã nhận:** chủ dự án trả lời "1", duyệt phase3-api-review.md gồm bản dịch 11 operation kế hoạch và bổ sung đúng hai schema công khai của 001. Phê duyệt không thay đổi lựa chọn chờ A công bố quyền danh mục Phase 2.

**Trình tự thực hiện thực tế:**

1. Đọc bản duyệt, YAML hiện tại, test danh mục, runner Maven và schema nguồn 001. Viết FR_003_CarePlanContractTest trước khi sửa YAML, gồm ba ca: operation/header/mã phản hồi; trường/enum/required; schema thỏa thuận và schema dùng chung đồng nhất với 001.
2. Lượt chạy đầu lỗi biên dịch do kiểu Schema raw của thư viện Swagger trả Object khi truy cập lồng; sửa kiểu Schema<?> trong test. Không tính lỗi này là RED hợp đồng.
3. Chạy lại khi YAML chưa đổi: 3 test/3 failure/0 error, exit1, do chưa có đường dẫn và schema tiếng Anh. Sau bằng chứng RED mới áp dụng YAML bằng script PowerShell ngoài kho. Hai lần script dừng trước bước ghi vì nhận diện schema một dòng chưa đúng; sửa script, không ghi hợp đồng dở dang.
4. Dịch 11 operation và các trường/enum theo bản duyệt; giữ mã HTTP/headers/tính bắt buộc/kiểu. Thêm operationId; hai schema OpinionSource/SelfDeterminationAssessment chép nguyên từ 001. Các tham chiếu dùng chung tới CarePlanItem/RiskAgreement được đổi theo tên schema mới, không dịch operation các story khác.
5. Chạy Spotless và test scoped cùng ArchTests. Lượt đầu còn 1 failure vì parser chuẩn hóa thứ tự required; sửa assertion kiểm tập không phụ thuộc thứ tự, không thay trường bắt buộc. Lượt cuối 34 test xanh, 0 failure/error/skip; riêng ba test mới xanh.
6. Reviewer độc lập chỉ đọc đối chiếu baseline/bản duyệt/YAML/test, không có finding Critical/Important/Minor. Kiểm git diff --check exit0; giữ toàn bộ checkbox mở và không tuyên bố endpoint/runtime/Phase 3 hoàn tất.

**Danh sách file tạo/sửa trong lượt áp dụng:**

| File | Thay đổi và mục đích |
| --- | --- |
| backend/src/test/java/com/cambae/backend/m04/FR_003_CarePlanContractTest.java | Tạo ba test hợp đồng trước YAML; bảo vệ header, mã phản hồi, field/enum và tính đồng nhất schema 001 |
| specs/005-daily-care-tasks/contracts/api-005.yaml | Áp dụng bản dịch được duyệt, thêm hai schema đang thiếu đích tham chiếu; chưa cài controller |
| specs/005-daily-care-tasks/contracts/phase3-api-review.md | Đổi trạng thái sang đã duyệt/đã áp dụng, vẫn ghi rõ chưa có runtime |
| specs/005-daily-care-tasks/tasks.md | Cập nhật checkpoint đã duyệt HTTP, giữ T007/T007a và toàn bộ Phase 3 [ ] |
| specs/005-daily-care-tasks/plan.md | Ghi quyết định tách component, bằng chứng RED/GREEN, kết quả review và giới hạn kiểm |
| specs/000-shared-foundation/glossary.md | Bổ sung đúng mục 11.3 của feature 005 cho HTTP/schema/trường/enum/lỗi mới |
| Working-history.md | Bổ sung lịch sử lượt phê duyệt và áp dụng, giữ nguyên lịch sử trước |
| C:/Users/nguye/.codex/visualizations/2026/10/26/01a176d7-6a54-7db1-b0ab-d145a335d1e4/apply-005-p3-contract.ps1 | Script hỗ trợ ngoài kho để dịch phạm vi đã duyệt; không phải mã production |
| C:/Users/nguye/.codex/visualizations/2026/10/26/01a176d7-6a54-7db1-b0ab-d145a335d1e4/005-p3-before.yaml | Bản YAML trước sửa phục vụ đối chiếu review; ngoài kho, không chứa dữ liệu người dùng |

Không xóa file. docs/team-plan.md và phase2-permission-review.md giữ cập nhật của lượt trước, không thay quyết định quyền lần này. hand-over-task-003.md không sửa. Không sửa mã nguồn Java production, SQL, DTO/API Java đã công bố, source/hợp đồng 001 hoặc frontend/src/api.

**Quan hệ file và luồng dữ liệu:** YAML định nghĩa HTTP kế hoạch, các schema CarePlan/CarePlanItem/RiskAgreement/CarePlanComparison dùng schema nguồn của 001. Test mới đọc YAML 005 và 001, so sánh schema và kiểm metadata hợp đồng. Chưa có controller nhận dữ liệu, persistence kế hoạch hoặc client web; đây không phải bằng chứng toàn luồng. Component Importance/ResidentId/CarePlanReason tách khỏi component tiếng Việt còn phục vụ story khác để không mở rộng phạm vi phê duyệt. Các consumer schema dùng chung đổi $ref theo tên mới để không treo tham chiếu.

**Lệnh và kết quả xác minh** (runner ngoài kho dùng JAVA_HOME JDK25, chỉ nạp TEST_DB_* vào process; không in giá trị):

| Lệnh Maven qua run-005-p2.ps1 | Log | Kết quả |
| --- | --- | --- |
| test -Dtest=FR_003_CarePlanContractTest -Dspotless.skip=true | 005-p3-contract-red.log | exit1 do biên dịch test; không tính RED hợp đồng |
| test -Dtest=FR_003_CarePlanContractTest -Dspotless.skip=true | 005-p3-contract-red2.log | exit1, 3 test/3 failure/0 error; RED do chưa dịch YAML |
| spotless:apply test -Dtest=FR_003_CarePlanContractTest,FR_038_CatalogContractTest,T002_FakeContractTest,*ArchTest,ModuleBoundaryTest | 005-p3-contract-green.log | exit1, còn 1 failure assertion thứ tự required; test danh mục/kiến trúc không có lỗi |
| Cùng lệnh scoped trên sau sửa assertion | 005-p3-contract-green2.log | exit0/33.771s; 34 test/0 failure/0 error/0 skip, ba ca mới xanh; Spotless 1510 file sạch, một test được định dạng |
| git diff --check | Output trực tiếp | exit0; chỉ cảnh báo LF/CRLF của Git, không lỗi whitespace |

Log nằm cùng thư mục artifact ngoài kho ở bảng trên. Pattern T002_FakeContractTest trong lệnh không khớp lớp hiện có và không tạo test bổ sung; không tính pattern thành bằng chứng chạy test fixture. Số 34 lấy từ kết quả Surefire thực tế, không cộng lặp XML cũ. Không dùng maven.test.failure.ignore=true trong các lượt này.

**Còn tồn tại và thứ tự tiếp theo:** nền chưa hoàn tất T007/T007a vì chờ A công bố khóa quyền chính thức; giữ lựa chọn 2, không sửa SELF_CHECKED/Function. Sau khóa quyền: T007 → T007a → kiểm cuối nền; sau đó T010 → T011 → T012–T017 (gồm T016a), áp dụng TDD cho implementation. Bản duyệt HTTP đã xử lý, nhưng chưa hoàn tất T016/T016a hoặc Phase 3. Không chạy clean verify mới vì chưa đến nghiệm thu phase; kết quả lỗi M01 ở mục trước là lịch sử, chưa xác nhận lại. Không screenshot, thêm thư viện, commit/push, đổi nhánh hoặc gửi tin tới A.

## Tiếp tục Phase 1–2 của 005 — 2026-10-31

Yêu cầu: hoàn thiện phần nền có thể làm ngay,theo AGENTS/tasks/plan và R-15;test đỏ trước mỗi thay đổi,kiểm task cùng kiến trúc,cuối phase clean verify nền;không đổi API/thêm thư viện/commit/push. Đã đọc lịch sử cũ,giữ nguyên nội dung trước mục này.

Đầu phiên: xác nhận nhánh feature/005-daily-care-and-hand-over,git status chỉ có Working-history.md và hand-over-task-003.md chưa theo dõi;không đổi nhánh hoặc hoàn tất merge. Đọc AGENTS,team-plan8,tasks,ghi chú plan,backendREADME và API chung. Mục8 có4dòng ghi trực tiếp HungNX-nguyen (API/events/job005,támSPI005,DayCareCapacity003,facadechuyển003);dòng sở hữu ở mục1 không tính vào mục8. Các dòng dùng tênC còn áp dụng R-15,cấu trúc và nhóm dữ liệu.

Đã bổ sung kế hoạch kỹ thuật vào specs/005-daily-care-tasks/plan.md;đầu tiên kiểm T006 vì tách khỏi schema. Tạo backend/src/test/java/com/eldercare/backend/care/NFR_04_CareStartupTest.java: công tắc chung tắt ngoài profiletest,ghi đè on-startup=false,và mặc định bật phải chạy hai job. Đang chạy test RED trong log TEMP 005-p2-startup-red-20261031.log;chưa có kết quả nên chưa ghi test xanh hoặc sửa production.

Hai lần gọi runner phụ ngoài kho không chạy được vì thư mục TEMP của sandbox thay theo process và file không còn ở process kế;không tính là RED nghiệp vụ. Đã chuyển sang lệnh Maven trực tiếp,nạp kín biến môi trường từ backend/.env và dùng JDK25;không ghi giá trị bí mật vào log/tài liệu.

### T006 — công tắc job

RED NFR_04_CareStartupTest:3test,2failure,0error do job vẫn chạy khi scheduling.enabled=false hoặc on-startup=false. Sau RED sửa CareMinuteJob.atStartup/CareDailyJob.atStartup dùng Environment với fallback công tắc chung;giữ constructor hiện có và runNow gọi tay. GREEN:44unit/kiến trúc,0failure/error/skip,exit0/01:08phút;Spotless đúng3file. Log TEMP005-p2-startup-{red,green}-20261031.log. Chưa đóngT006 vì còn chuyển khóa/marker.

### T005/R-15 — test migration và thiết kế ID

Tạo T005_CareFoundationMigrationIT: scratchFlyway tới5.2,ghi loại công việc/chứng chỉ,kết quả mã100ký tự,khóa vàmarker cũ;catalog_item cóid2000000001 trùng nguồn005;chạy tới5.95. RED exit1 vì chưa cómigration5.95,không phải lỗi môi trường. Sau RED tạo V005_95__care_foundation_consolidation.sql: chép sáu bảng nền sang ba bảng chung rồi bỏ bảngcũ;không tạo bảng story,không sửa migration cũ. Tạm gỡ @Entity/@Table/@DataGroup của NonPerformanceReasonType để Hibernate không yêu cầu bảng đã bỏ;store/service sẽ đổi trongT007a trước kiểm cuốiphase.

LuồngID: physical catalogid mới lớn hơnmaxid nguồn/đích;attributes.legacy={table,id} giữ ID công khai của dòngchuyển. Adapter sẽ giữ type/id audit cũ,không sửa nhật ký;không cần LegacyRecordTypeSource khi loạiđối tượng giữ nguyên. Mã kết quả100ký tự giữ attributes.code thayvì code40 của bảngchung. Marker dùng SHA256(job+':'+milestone),details giữ nguồn,chống cắt mất dữliệu;đây là thiết kế kỹ thuật,không thêm nghiệpvụ. Kết quảGREEN migration đang chờ,chưa ghi thành công.

### T005/T006/T007a — kiểm adapter sau gộp

Migration GREEN:41kiến trúc và3IT scratch,0failure/error/skip,exit0/01:24phút;log005-p2-migration-green-20261031.log. Đổi fixture HTTP lý do sang catalog_item và fixture khóa/marker sang bảng chung. RED adapter:HTTP create lỗi unknown entity cũ;T006 đọc bảng đã bỏ. Cleanup ban đầu dùng quyền app để DELETE bảngchung bị từchối và che lỗi body;đổi cleanup test qua adminConnection,không đổi quyền production. RED T006 chạy lại:4test có1failure+3error do SQLcare_resident_lock/m04_job_milestone không còn.

Sau RED: CareLockStore gọi ResidentModuleStateApi theo id tăng dần,upsert lấy exclusive lock trước SELECT FOR UPDATE;CareResidentState giữversion nguồn. CareMilestoneStore gọi ReminderMarkers với SHA256 đúng migration,runner giữmarker vàwork cùngtransaction. NonPerformanceReasonType thànhsnapshot không@Entity;ReasonAttributes/LegacyCatalogId/CareCatalogKinds khai nội dung/mãloại;NonPerformanceReasonStore gọi CatalogApi và ánh xạIDcôngkhai cũ;NonPerformanceReasonService điều phối snapshot mới,giữquyền/audit/HTTP. NonPerformanceReasonController/response giữhợpđồng cũ,không thêmendpoint.

GREEN adapter:41ArchTest+25IT (reasonHTTP18,rollbackaudit3,scheduling4),0failure/error/skip,exit0/01:33phút;Spotless phạmvi fileđãđổi. Log005-p2-adapters-green-20261031.log. Testđỏ/green migration không thay thế kiểm cuối phase;T005/T006/T007a vẫn chưa đánh[x].

### T007 — chuẩn bị test

Tạo FR_038_CareCatalogHttpIT trước implementation: lifecycle hai danh mục,támoperation,ETag/staleversion,audit,quyềnQ-391,bắt buộc header,enum sai,trùngtên và TaskTypeApi đọc chứngchỉ mới ngay. Đang chạy REDlog005-p2-t007-red-20261031.log;chưa có kết quả,không ghi test xanh. Tasktiếp theo trong cùngphạmvi là cài T007 sau RED,hoàn thiện T007a testđích mới,T008/T009 và kiểm cuối nền.

### Kiểm nền cuối trước clean verify và review

FR_038_CareCatalogHttpIT12ca và FR_038_CatalogContractTest cùngkiếntrúc đãxanh. Sauđó bổsung FR_038_CareCatalogAtomicityIT6ca: audit lỗi phải rollback cả create/update/deactivate của cảhai danh mục,không cótransactionbao test. T005_FoundationSchemaIT3ca và FR_028a_NonPerformanceReasonTypeIT2ca được chuyển sang service/API sauR-15,giữ enum18nhóm,khungnull,chứngchỉ,duynhấttên,tái sửdụngtênngừng,khóa/marker,nhómluậtvàcờfixed. T008_CareGovernanceIT5ca readiness vẫn xanh;không viết schema/story tươnglai trongphase2.

Lượt005-p2-foundation-final-20261031.log exit0/01:47phút:28IT (T007HTTP12,rollback6,schema3,reason2,governance5),0failure/error/skip;cácunit/fake/ArchTest đãqua,chi tiếtsố lượngghi theo báo cáoMaven cuối. Đây là hồi quy nền,không nghiệmthuT064 toànfeature. SpotlesslầnT007 dùngregexrộng.*care.*vì têneldercare nên quéttoànkho;gitdiffxácnhậnkhôngsửafilengườikhác. Các lầnsaudùngregex.*backend.care.* đểgiớihạnđúngmodule.

Tronglượtđọc cuối,HEADđãđổi sang728a9f94 'update 005 daily care phase 1 and 2',cácfile implementationđãđượcGit theo dõi;đây là thayđổi bênngoài phiêncôngcụ,khôngcó lệnhgitcommit/pushdoAIthựchiện. Khôngđổihoặcghiđè commitđó. Reviewerđộc lậpđọcHEAD^..HEADvàbảntrênđĩa,khôngchạyMaven/sửafile.

ReviewpháthiệnP2:hairequestđồngthờicùngquaprechecktên,UNIQUEnémDataIntegrityViolationExceptionnhưngCareCatalogStorechỉdịchConditionNotMetException,nêncólọtlỗi500. TạoFR_038_CatalogRaceTest2ca trước sửa,RED2failure/0error;sauredthêmánhxạexceptionDBthành409DUPLICATE_NAMEởcreate/update. GREEN43unit/kiếntrúc,0failure/error/skip,exit0/52.657giây,Spotlessđúng2file;log005-p2-race-{red,green}-20261031.log. KhôngđổingưỡngtesthoặchợpđồngAPI.

Đãkhởichạy mvnw.cmd clean verify nền sau bản sửa cuối,logTEMP005-p2-clean-verify-20261031.log;chưa cókếtquả,khôngđánh[x]. Cậpnhật data-model005(glossarymãloại/JSON/ID),glossary000chỉmục11.5,contracts005(bảnthậtTaskTypeApi),tasks005(giữcheckpointmở),team-plan(dòngT007vàAkiểm12operation9route). Khôngsửanghiệpvụ/spec,hợpđồngHTTP,common,modulekhác,UIhoặc thưviện.

### RàsoátPhase3theoyêucầutiếptheo

ChỉđọcT010–T017/T016a,planvàbảnduyệtphase3-api-review,APICarePlan/OpinionSource/FamilyRelationship/Approver,câythưmụcBE/FE. Chưacócareplan/riskagreementruntime hoặc testnghiệpvụPhase3;chỉcó3testhợpđồng. 11operationvàschemaHTTPđãduyệt27/10. FamilyRelationshipApiđãcóbelongsToResident/relationshipnhưngchưatìmthấybeanproduction012;OpinionSourceServicecóbảnthậtvàdùngnguồnquan hệquaSPI,nênnhánhngườithânvẫncầnnguồnthậtđểnghiệmthutoànluồng. Nền005cóT007vàAPIbảngchung000,T010cóthểchuẩnbịtestngay;implementationstoryvẫnchờchốtnền. Khôngcàiphase3tronglượtràsoát.

### Bổ sung kiểm ghép nguồn thật với fake chung

Trước chốt nền,đối chiếu FakeM04Config thấy FakeCareRequirements khôngPrimary (đúng thiết kế ghép với FakeM04ApiConfig cóproxyPrimary),nhưngthêmCareTaskTypeLookup thật cóthểgâyhaiứngviên khi chỉimportcấu hình fake cũ. Dừng JVM14224 của cleanverify đầu đểkiểm,sửa và chạylại bảncuối;log005-p2-clean-verify-20261031.log làlượt bịdừng,không tínhgate đãchạyhết.

Tạo backend/src/test/java/com/eldercare/backend/support/fake/care/T003_RealCatalogCompatibilityTest.java bằng ApplicationContextRunner nạpnguồnthật vàfakechung;RED1error NoUniqueBeanDefinition đúng careTaskTypeLookup/fakeCareRequirements. SauRED đặt @Fallback trên CareTaskTypeLookup (Springhiệncó),giữnormal/Primaryfixture ưu tiên khi test/demo import tườngminh;productionvẫn cóproviderthật duy nhất. KhôngsửaAPI/caller/testcủaE,khôngtạofakethứhai. Đangchạy005-p2-real-fake-green-20261031.log vớiT003* vàArchTest;chưa ghi kếtquả trước khi cólog.

### Danh sách file thực tế của lượt nền31/10

A: tạo mới; M: sửa. Những file thuộc commit728a9f94 vẫn được liệt kê vì đã triển khai trong lượt này; AI không tạo commit đó. Không có file bị xóa trong trạng thái cuối.

| Loại | File | Mục đích |
| --- | --- | --- |
| A | `backend/src/main/java/com/eldercare/backend/care/internal/catalog/domain/CareCatalogKinds.java` | Khai ba loại danh mục M04 và ánh xạ ID công khai. |
| A | `backend/src/main/java/com/eldercare/backend/care/internal/catalog/domain/LegacyCatalogId.java` | Giữ tên bảng và ID trước khi gộp. |
| M | `backend/src/main/java/com/eldercare/backend/care/internal/catalog/domain/NonPerformanceReasonType.java` | Thay entity bảng riêng bằng snapshot danh mục. |
| A | `backend/src/main/java/com/eldercare/backend/care/internal/catalog/domain/ReasonAttributes.java` | Nội dung fixed và legacy của loại lý do. |
| A | `backend/src/main/java/com/eldercare/backend/care/internal/catalog/domain/ResultValueAttributes.java` | Nội dung và kiểm kiểu của giá trị kết quả. |
| A | `backend/src/main/java/com/eldercare/backend/care/internal/catalog/domain/TaskTypeAttributes.java` | Nội dung, enum, khung và chứng chỉ của loại công việc. |
| A | `backend/src/main/java/com/eldercare/backend/care/internal/catalog/persistence/CareCatalogStore.java` | Adapter CatalogApi cho T007; dịch xung đột tên sang409. |
| M | `backend/src/main/java/com/eldercare/backend/care/internal/catalog/persistence/NonPerformanceReasonStore.java` | Adapter CatalogApi cho T007a; giữ ID công khai cũ. |
| A | `backend/src/main/java/com/eldercare/backend/care/internal/catalog/service/CareCatalogService.java` | Tám use case danh mục với quyền, transaction, version và audit. |
| A | `backend/src/main/java/com/eldercare/backend/care/internal/catalog/service/CareTaskTypeLookup.java` | Provider TaskTypeApi đọc chứng chỉ mới; Fallback cho fixture thay thế. |
| M | `backend/src/main/java/com/eldercare/backend/care/internal/catalog/service/NonPerformanceReasonService.java` | Điều phối snapshot/API mới, giữ bảo vệ fixed và audit. |
| M | `backend/src/main/java/com/eldercare/backend/care/internal/scheduling/persistence/CareMilestoneStore.java` | Đổi marker sang ReminderMarkers, hash khóa dài. |
| A | `backend/src/main/java/com/eldercare/backend/care/internal/status/domain/CareResidentState.java` | Nội dung trạng thái kỹ thuật theo người. |
| M | `backend/src/main/java/com/eldercare/backend/care/internal/status/persistence/CareLockStore.java` | Khóa tăng dần qua ResidentModuleStateApi. |
| M | `backend/src/main/java/com/eldercare/backend/care/scheduling/CareDailyJob.java` | Tôn trọng công tắc khởi động và fallback lịch chung. |
| M | `backend/src/main/java/com/eldercare/backend/care/scheduling/CareMinuteJob.java` | Tôn trọng công tắc khởi động và fallback lịch chung. |
| A | `backend/src/main/java/com/eldercare/backend/care/web/CareCatalogController.java` | Cài đúng tám operation T007 đã duyệt. |
| A | `backend/src/main/java/com/eldercare/backend/care/web/request/ResultValueWrite.java` | Body HTTP giá trị kết quả đúng hợp đồng. |
| A | `backend/src/main/java/com/eldercare/backend/care/web/request/TaskTypeWrite.java` | Body HTTP loại công việc đúng hợp đồng. |
| A | `backend/src/main/java/com/eldercare/backend/care/web/response/ResultValueResponse.java` | Ánh xạ sáu trường phản hồi, giấu nội dung lưu trữ. |
| A | `backend/src/main/java/com/eldercare/backend/care/web/response/TaskTypeResponse.java` | Ánh xạ phản hồi loại công việc, không thêm version vào body. |
| A | `backend/src/main/resources/db/migration/V005_95__care_foundation_consolidation.sql` | Chép sáu bảng nền sang bảng chung rồi bỏ bảng cũ. |
| M | `backend/src/test/java/com/eldercare/backend/care/FR_028a_NonPerformanceReasonTypeIT.java` | Kiểm fixed qua lệnh service theo R-15 thay trigger riêng. |
| M | `backend/src/test/java/com/eldercare/backend/care/FR_028a_ReasonCatalogAtomicityIT.java` | Đổi fixture sang bảng chung, giữ ba kiểm rollback audit. |
| M | `backend/src/test/java/com/eldercare/backend/care/FR_028a_ReasonCatalogHttpIT.java` | Đổi fixture/truy vấn sang bảng chung và ID legacy, giữ HTTP/quyền. |
| A | `backend/src/test/java/com/eldercare/backend/care/FR_038_CareCatalogAtomicityIT.java` | Sáu ca rollback audit của các lệnh T007. |
| A | `backend/src/test/java/com/eldercare/backend/care/FR_038_CareCatalogHttpIT.java` | Kiểm tám endpoint, quyền, ETag, dữ liệu và chứng chỉ tức thời. |
| A | `backend/src/test/java/com/eldercare/backend/care/FR_038_CatalogRaceTest.java` | Hai ca lỗi UNIQUE đồng thời phải thành409. |
| M | `backend/src/test/java/com/eldercare/backend/care/NFR_04_CareSchedulingIT.java` | Kiểm khóa/marker trên bảng chung và cleanup bằng tài khoản test quản trị. |
| A | `backend/src/test/java/com/eldercare/backend/care/NFR_04_CareStartupTest.java` | Ba ca công tắc khởi động không phụ thuộc profile test. |
| A | `backend/src/test/java/com/eldercare/backend/care/T005_CareFoundationMigrationIT.java` | ScratchFlyway kiểm chép dữ liệu, va chạm ID và bỏ bảng cũ. |
| M | `backend/src/test/java/com/eldercare/backend/care/T005_FoundationSchemaIT.java` | Giữ các bảo đảm nền qua service/API bảng chung. |
| A | `backend/src/test/java/com/eldercare/backend/support/fake/care/T003_RealCatalogCompatibilityTest.java` | Kiểm chọn đúng fake chung khi provider thật đã có. |
| M | `docs/team-plan.md` | Đổi dòng chờ sang kiểm cuối/review và đăng ký12operation danh mục. |
| M | `specs/000-shared-foundation/glossary.md` | Chỉ bổ sung thuật ngữ phần005 mục11.5. |
| M | `specs/005-daily-care-tasks/contracts/module-interfaces.md` | Ghi provider TaskTypeApi thật, không đổi chữ ký. |
| M | `specs/005-daily-care-tasks/data-model.md` | Ghi lược đồ nền sau gộp, loại/JSON/ID và phạm vi chưa cài. |
| M | `specs/005-daily-care-tasks/plan.md` | Ghi kế hoạch, quyết định kỹ thuật và bằng chứng RED/GREEN. |
| M | `specs/005-daily-care-tasks/tasks.md` | Ghi trạng thái hiện tại và giữ checkpoint chưa nghiệm thu. |
| M | `Working-history.md` | Bổ sung toàn bộ trình tự, luồng dữ liệu, file, kiểm chứng và vấn đề còn lại; giữ lịch sử cũ. |

GREEN ghép bean:49unit/kiến trúc,0failure/error/skip,exit0/51.264giây;Spotless đúng2file. Reviewer đọc lại @Fallback và test,không còn finding hoặc thayđổi chữkýAPI. Đã bắt đầu lại cleanverify trên bảncuối,log005-p2-clean-verify-final-20261031.log;không dùng log bịdừng làm bằng chứng nghiệmthu.

Luồng dữ liệu nền sau sửa: HTTP TaskTypeWrite/ResultValueWrite → CareCatalogController → CareCatalogService (quyền CARE_TASK_CATALOG,transaction,version) → CareCatalogStore → CatalogApi → catalog_item;service ghi AuditLogApi trong cùngtransaction,web response chỉ trả trường hợpđồng. Consumer008 → TaskTypeApi/CareTaskTypeLookup → cùngCatalogStore,đọc chứngchỉ mỗi lần. HTTP lýdo giữ controller/response cũ nhưng service/store dùng snapshot/ReasonAttributes từCatalogApi;fixed được service chặn. Lệnh cần khóa người → CareResidentLock → CareLockStore → ResidentModuleStateApi,khóa tăng dần;CareDailyJob → CareDailyWorkRunner → CareMilestoneStore/ReminderMarkers,marker và tácđộng cùngtransaction nên thấtbại thửlại được. SchedulerRuns vẫn ghi UUID vòng chạy;job không tạo côngviệc. Fake chỉ đăngký khi test/demo import cấu hình riêng;@Fallback giúp fixture cũ thay thế providerthật mà không sửa modulekhác.

### Lệnh kiểm của lượt31/10 (từ backend, JDK25)

Biến TEST_DB_* và DB_PASSWORD/DB_RETENTION_PASSWORD được nạp từ .env vào process,không in giá trị. Log ở C:/Users/nguye/AppData/Local/Temp/.

| Phạm vi | Lệnh Maven (các tùy chọn Spotless chỉ giới hạn file Java đã sửa) | Bằng chứng |
| --- | --- | --- |
| T006 startup RED | `.\mvnw.cmd test -Dtest=NFR_04_CareStartupTest` | startup-red:3test,2failure |
| T006 startup GREEN | `.\mvnw.cmd spotless:apply test -Dtest=NFR_04_CareStartupTest,*ArchTest,ModuleBoundaryTest` | startup-green:44test xanh |
| T005 migration RED/GREEN | `.\mvnw.cmd test-compile failsafe:integration-test failsafe:verify -Dit.test=T005_CareFoundationMigrationIT`;GREEN thêm `test -Dtest=*ArchTest,ModuleBoundaryTest` | migration-red thiếu5.95;green41Arch+3IT |
| T006 adapter RED | `.\mvnw.cmd test-compile failsafe:integration-test failsafe:verify -Dit.test=NFR_04_CareSchedulingIT` | locks-red:4test,1failure+3error |
| T006/T007a GREEN | `.\mvnw.cmd spotless:apply test -Dtest=*ArchTest,ModuleBoundaryTest failsafe:integration-test failsafe:verify -Dit.test=NFR_04_CareSchedulingIT,FR_028a_ReasonCatalogHttpIT,FR_028a_ReasonCatalogAtomicityIT` | adapters-green:41Arch+25IT |
| T007 RED | `.\mvnw.cmd test-compile failsafe:integration-test failsafe:verify -Dit.test=FR_038_CareCatalogHttpIT` | t007-red:12failure404 |
| T007 GREEN | `.\mvnw.cmd spotless:apply test -Dtest=FR_038_CatalogContractTest,*ArchTest,ModuleBoundaryTest failsafe:integration-test failsafe:verify -Dit.test=FR_038_CareCatalogHttpIT` | t007-green:42unit/Arch+12IT |
| Hồi quy nền | `.\mvnw.cmd spotless:apply test -Dtest=T001_ModuleSetupTest,T002*,T003*,NFR_04_CareEventsTest,NFR_04_CareStartupTest,T009*,FR_038_CatalogContractTest,*ArchTest,ModuleBoundaryTest failsafe:integration-test failsafe:verify -Dit.test=FR_038_CareCatalogHttpIT,FR_038_CareCatalogAtomicityIT,T005_FoundationSchemaIT,FR_028a_NonPerformanceReasonTypeIT,T008_CareGovernanceIT` | foundation-final:unit/fake/Arch và28IT xanh |
| Lỗi trùng tên RED/GREEN | `.\mvnw.cmd test -Dtest=FR_038_CatalogRaceTest`;GREEN thêm `spotless:apply` và `*ArchTest,ModuleBoundaryTest` | race-red2failure;green43test xanh |
| Ghép nguồn thật/fake RED/GREEN | `.\mvnw.cmd test -Dtest=T003_RealCatalogCompatibilityTest`;GREEN `spotless:apply test -Dtest=T003*,*ArchTest,ModuleBoundaryTest` | real-fake-red1error;green49test xanh |
| Kiểm cuối phase | `.\mvnw.cmd clean verify` chạy nền | logclean-verify đầu bị dừng để sửa ghép bean;logclean-verify-final mới là bản cuối,đang chờ kết quả |

Không dùng cờ bỏ test hoặc maven.test.failure.ignore;không sửa test người khác để ép xanh. Các mẫu -Dtest không được coi là số test,số lượng phải lấy từ báo cáo thực tế.

### Kết quả rà soát Phase 3 (chỉ đọc, chưa triển khai story)

Phase 3 có9task:T010,T011,T012,T013,T014,T015,T016,T016a,T017. Cây mã hiện có CarePlanApi/DTO/events và FR_003_CarePlanContractTest;chưa có service/persistence/controller kế hoạch hoặc thỏa thuận. Hợp đồng 11operation đã duyệt và áp dụng ngày27/10. Quyền CARE_TASK_CATALOG và API bảngchung000 đã có;ghi chú lịch sử chờ công bố quyền không còn là phụ thuộc hiện hành. Taskstype/catalog đã có bảnthật sau sửa nền,nhưng checkpoint Phase2 chưa đóng khi cleanverify chưa qua.

| Task | Có thể chuẩn bị ngay | Điều kiện khi triển khai |
| --- | --- | --- |
| T010 | Đủ căn cứ cho test chuỗi thời điểm bốn tần suất và so sánh phiên bản | Làm đầu tiên sau checkpoint nền;chưa có test nghiệp vụ này trong kho |
| T011 | Lập các ca lifecycle,khóa,duy nhất hiệu lực,tự duyệt và thỏa thuận | Viết đỏ trước schema/service;dùng MySQL và fake chung đúng chủ khi thiếu nguồn thật |
| T012 | Chốt schema sau R-15 và test constraint/trigger | Số V005_02 đã dùng,phải chọn migration mới;giữ care_plan/care_plan_item,gộp thỏa thuận vào mục theo kind;người tham gia/ý kiến qua RecordEventApi,không tạo lại ba bảng cũ |
| T013 | Thiết kế domain/entity/chuỗi thời điểm | Theo schema T012,đặt internal/careplan/domain,persistence/service đúng README;không chép đường dẫn logic |
| T014 | Đủ hợp đồng và API quyền/duyệt/tham số/nhật ký | Sau T010–T013;cài bảng chuyển,If-Match,sao chép và tự duyệt đúng BR;đồng bộ provider T008 với schema thật |
| T015 | Khung CareDailyJob/runner/marker đã có | Sau lifecycle T014,đổi hiệu lực và hết hiệu lực cùng transaction;không tự tạo kế hoạch/công việc |
| T016 | 11operation HTTP và CarePlanApi đã duyệt/khai báo | Sau service;giữ các chữ ký hiện có và ETag/hình dạng hợp đồng;chưa có runtime |
| T016a | OpinionSourceApi có bảnthật;FamilyRelationshipApi có belongsToResident/relationship | Có thể cài/test với fake chung;chưa tìm thấy bean production FamilyRelationshipApi nên chưa nghiệmthu toànluồng người thân/đại diện |
| T017 | Có thể chuẩn bị cấu trúc/test giao diện theo README/DESIGN | Nối màn kế hoạch sau T016;phần sửa danh mục cần chốt cách lấy version vì GETlist không có version/ETag từng dòng,không tự thêm trường/endpoint |

Thứ tự đề nghị sau chốt nền:T010→T011→T012/T013→T014→T015→T016→T016a→T017. Có thể chuẩn bị test T016a cùng T011;chưa phát sinh phê duyệt nghiệp vụ hoặc thay đổi hợp đồng trong lượt đọc. ShiftStaffPort thật của008 chủ yếu chặn story lập công việc/checklist sau đó,không phải lý do chặn phần lõi kế hoạch Phase3. Chưa viết Java/SQL/UI củaPhase3 hoặc đánh task củaPhase3.

### Kết quả cuối của lượt nền và công việc còn lại

Lệnh clean verify bản cuối đã kết thúc exit1 sau09:50phút,logC:/Users/nguye/AppData/Local/Temp/005-p2-clean-verify-final-20261031.log. Surefire:721test đơn vị/kiếntrúc xanh. Failsafe:1563IT,có7failure+9error,0skip;care56IT/9lớp,roombedallocation258IT/53lớp,staffing251IT/36lớp đều xanh. Spotless:check toàn2250file sạch. Lượt hồi quy nền trước đó có74unit/fake/ArchTest và28IT xanh (số74 được lấy lại từlogfoundation-final,không suy từpattern).

Đối chiếu phương thức lỗi với eldercare-c-allocation-handover-verify-20261031.log:16phươngthức trùng,không cóphươngthức lỗi mới. 15lỗiM01 giữbàn giaoB;ApiContractIT có1failurevìapi005 chưa được A đăng ký,bao gồm9route/12operationdanhmục đãcài. Không sửacommon/ApiContractIT hoặc bỏtest. Ca hiệunăngM03 trước đó không đỏ tronglượt005 (samples204,205,207,209,212,213,213,214,216,267ms;max267ms),không ghi làđãtốiưuhoặcsửa xong vìlượt này khôngsửa đường đọcgiường.

Reviewer đã xác nhậnbản sửa lỗi trùngtên vàFallback,khôngcònfinding trongphạmvi đọc;review khôngthaythế test. Không chạy lại toànkho khi khôngcó mãmới/lỗimới. GiữmọicheckboxPhase1–2 vìgate toànkho chưaqua;không coi migration nền là hoàn tấtT064toànfeature. PENDING_API_ROOT_TYPES của MedicationChecklistItem vẫn còn,vìkhông đổi đườngdẫn kiểuAPI/callerD tronglượt này. T008có5ITreadiness xanh nhưngmapping/provider phảiđồngbộschemaR-15 khi bảngstory được cài;không dùngfixtureDDL đểghi đãtíchhợpnghiệpvụ thật.

Tài liệu cuối đã cập nhật:plan/tasks005 ghi kếtquả và giữcheckpoint;data-model005 ghiánhxạ nền;glossary000 chỉmục11.5;module-interfaces005 ghiTaskTypeApi cónguồnthật;team-plan8giữ dòngchờreview/gate vàAđăngký12operation,khôngxóadòng chưa vào develop;docs/progress.md thêm1dòng mới (khôngsửa lịch sử). Working-history.md giữnội dung cũ vàbổsung tấtcả mục trên. Không chụpảnh,sửaUI,thêmthưviện,đổiAPI hoặc tựcommit/push/merge.

Thứ tự tiếp theo theo tasks.md:chốtPhase2/gate (AđăngkýApiContractIT,BxửlýM01;Cgiữ kiểmhẹp vàboardreview),rồiT010→T011→T012/T013→T014→T015→T016→T016a→T017. BảngràsoátPhase3 ởmục trước chỉlàđánhgiásẵnsàng,chưa làm implementation hoặcđánh[x]. Nhữngphần đủcăn cứ cóthểchuẩnbịtest/thiếtkếngay;khôngbỏcheckpointPhase2. NguồnFamilyRelationshipApi thật vàcáchlấyversioncho UIdanhmục làcácphầncầnphốihợp/chốt trước nghiệmthu cácnhánh tươngứng.

| File tài liệu bổ sung cuối | Mục đích |
| --- | --- |
| `docs/progress.md` | Thêm nhật ký ngày31/10 với kếtquả gate thật và các việc chưa xong. |
| `Working-history.md` | Hoàn tất ghi sốtest,log,review,giới hạn nghiệmthu và kếtquả ràPhase3. |

## Triển khai Phase 3 của 005 theo yêu cầu mới — 2026-10-31

Đã đọc lại AGENTS.md, mục8 team-plan (4dòng ghi trực tiếp HungNX-nguyen), tasks T010–T017/T016a, ghi chú plan, backend/README, C-04/C-05/C-18 và hợp đồng đã duyệt. Giữ các thay đổi nền chưa commit, không đổi nhánh hoặc sửa file bàn giao003. Yêu cầu mới cho tiếp tục phần Phase3 có thể làm; không suy gate16lỗiA/B thành xanh hoặc đánh hoàn tất Phase2. Kế hoạch và quyết định kỹ thuật đã thêm vào plan005.

T010: tạo BR_M04_25_CarePlanItemSequenceTest5ca và FR_008_CarePlanComparisonTest2ca trước production. Lượt đầu đỏ biên dịch do assertionList<?> dùng containsExactly(String);sửa test và chạy lại,RED7ca (thiếu lớp mới;ca invalidinterval báo thiếu lớp thay vì lỗi nghiệp vụ). Sau RED tạo CarePlanItemSequence,CarePlanComparison,CarePlanItemChange trong internal/careplan/domain. Chuỗi EVERY_N_HOURS reset mỗi ngày theo BusinessTime.ZONE,lọc [from,to),lọc ngày của mốc qua đêm,mốc trongca dùng đầu ca đại diện;không sinh côngviệc. So sánh theo key lineage,bỏ id bản sao,đối chiếu từng trường nội dung. GREEN48unit/kiếntrúc,0failure/error/skip,exit0/58.096giây;Spotless5file. LogTEMP005-p3-t010-{red,red2,green}-20261031.log. Chưa đánh[x] trước kiểm cuối phase.

T011 trước T012: tạo BR_M04_19_CarePlanSchemaIT,RED6error vì chưa có care_plan/care_plan_item. Bổ sung2ca riskagreement và record_event ở bảng chung,đang chạy REDlần2;đồng thời sửa fixture để mỗi lần tạo plan tăng revision,không để UNIQUE(resident,revision) che điều đang muốn kiểm là open/effective unique. Một lệnh PowerShell thay fixture lỗi parser trước ghi;dùng apply_patch sửa đúng,không tính lỗi parser là RED nghiệp vụ. Chưa có kết quả cho2ca bổ sung hoặc migration mới.

Quyết định T012: DATETIME của bảng005 lưu ngày/giờ nghiệp vụ theo BusinessTime.ZONE như DBR-25;store sẽ chuyển Instant↔LocalDateTime theo zone đó,để CHECK ngày hiệu lực sau DATE(approved_at) không sai biên ngày. Hai bảng riêng nhómBUSINESS;TASK vàRISK_AGREEMENT là hai dạng mục nhóm2. Ýkiến/ngườithamgia dùng record_event do000 giữ (MULTIPLE cósẵn),thêm guard riêngM04 theo trạngthái cha để khóa ngoàiNháp;cầnA review trigger phầnbảngchung trongPR,không sửa migration000 hoặc JavaM15. Nội dung con cócờactive kỹthuật để thay toàn bộ danh sách nháp qua RecordEventApi.updateDraft/add mà không đòi APIxóa;giữ dòng cũ cho retention và không đưa cờ này vàoHTTP. Chưa cài các quyếtđịnh sau trước khi test đỏ tươngứng.

### Schema T012, thay đổi version đã duyệt và bảng chuyển FR-003

- V005_96__care_plan_revisions.sql: tạo care_plan và care_plan_item; chỉ mục một bản mở/một bản hiệu lực, CHECK ngày duyệt và tần suất, trigger khóa mục và con record_event ngoài Nháp. Không tạo lại các bảng thỏa thuận riêng bị R-15 loại bỏ. T011 schema RED lần 2 có 8 error thiếu bảng; GREEN T012 có 41 ArchTest + 8 IT, 0 failure/error/skip, exit 0/01:01 phút. Log 005-p3-t011-schema-red2-20261031.log và 005-p3-t012-schema-green-20261031.log ở thư mục Temp của người dùng.
- Chủ dự án trả lời phương án 2, duyệt version chỉ đọc cho ba danh mục. Sửa FR_038_CatalogContractTest, FR_038_CareCatalogHttpIT, FR_028a_ReasonCatalogHttpIT trước production; sửa lỗi biên dịch cast Schema, RED thực tế ở lần 2 vì hợp đồng chưa có trường. Sau đó cập nhật contracts/api-005.yaml cùng TaskTypeResponse, ResultValueResponse, NonPerformanceReasonTypeResponse. Trường này lấy từ version hiện hành của CatalogApi, dùng cho If-Match; không lấy id vật lý làm id công khai.
- Thêm FR_003_CarePlanTransitionsTest: kiểm đủ cặp trạng thái/lệnh SUBMIT, APPROVE, RETURN, CANCEL, WITHDRAW và biên ngày. RED 3 failure do chưa có CarePlanTransitions. Sau RED tạo CarePlanTransitions trong internal/careplan/domain; quyền/lý do và điều kiện nội dung sẽ được kiểm ở service, không coi bảng chuyển thuần là hoàn tất T014.
- Lượt kiểm version đầu có lỗi ProfileOpenGuardArchTest không tìm thấy fixture trong lúc Maven thứ hai biên dịch cùng target. Không sửa nguồn B và không ghi là xanh; lượt kiểm tuần tự đang chạy. Các task và checkbox vẫn mở, chưa có kết quả clean verify Phase 3.
### Lát cắt vòng đời, thỏa thuận và job Phase 3

Đã thêm FR_003_CarePlanLifecycleIT trước implementation: RED 4 failure HTTP 404 (005-p3-lifecycle-red-20261031.log). Sau đó tạo CarePlan, CarePlanItem, CarePlanItemData, CarePlanStore, CarePlanService, CarePlanQueries, CarePlanController và các request/response trong web. Luồng: controller chuyển request → service kiểm quyền CARE_PLAN và khóa CareLockStore theo resident → CarePlanStore đọc FOR UPDATE, ghi hai bảng riêng → AuditLogApi và sự kiện trong cùng transaction → response theo hợp đồng. CarePlanQueries cài CarePlanApi, dùng @Fallback để tương thích fake do chủ sở hữu cung cấp khi test/demo import. Không đổi chữ ký CarePlanApi.

- Lượt lifecycle đầu: 44 unit/Arch xanh, 4 IT còn 1 failure vì test mong 403 cho điều dưỡng ngoài phạm vi. Đã đọc RequiresPermissionAspect: không xem được đối tượng do SCOPE trả 404 chung. Sửa kỳ vọng test của 005 theo chính sách này; không sửa aspect của A.
- Bổ sung test tự duyệt bắt buộc Reason, duyệt kiểm lại ngày, và mục tiêu nước trùng. Chủ dự án chọn chỉ một mục có waterTargetMl trong mỗi phiên bản (31/10); ghi ở plan, không tự sửa tài liệu nghiệp vụ. RED mục tiêu trùng trả 200 thay vì 422; sau đó service chặn trước ghi. T008 readiness cũ RED vì tự CREATE care_plan đã tồn tại; đổi fixture sang schema kế hoạch thật, id từ TestUsers.nextId, chỉ kiểm/xóa dòng của fixture; care_task vẫn là DDL fixture cho story chưa cài.
- T015 test RED do chưa có bước CARE_PLAN_EFFECTIVE. Tạo CarePlanActivation và thêm truy vấn/ghi trạng thái trong CarePlanStore. CareDailyJob hiện có gọi bước này qua runner/marker và SchedulerRuns; không thêm job hoặc tự tạo công việc. Hết hiệu lực bản cũ và áp dụng bản mới cùng transaction, audit có Basis và jobRunId từ MDC khi chạy qua job. GREEN 44 unit/Arch + 13 IT (8 vòng đời, 5 governance), exit 0/01:24 phút; log 005-p3-activation-green-20261031.log.
- Bổ sung test thỏa thuận và so sánh: RED 3 failure (hai PUT risk 404, compare 400). Tạo CareRiskAgreement, RiskDecisionMaker, RiskParticipant, RiskOpinion, RiskOpinionEntry, CareRiskKinds trong domain; CareRiskStore trong persistence; CareRiskService trong service; các transport Risk*Write/Risk*Response ở web. CareRiskStore chỉ SQL trên bảng của 005, người tham gia/ý kiến qua RecordEventApi. Khi thay danh sách, cờ active của con cũ bị tắt trước dòng cha; nội dung cũ vẫn giữ. Sao chép giữ gốc sourceAgreementId, nguồn ý kiến/người ghi/thời điểm. Kiểm doctor qua AccountApi, nguồn qua OpinionSourceApi, quan hệ qua FamilyRelationshipApi; thiếu nguồn quan hệ thật thì không tự cho qua.
- So sánh HTTP dùng CarePlanDifference/CarePlanDifferenceEntry trên nội dung nullable của Nháp, không thay khung null bằng CFG mới. CarePlanComparisonResponse/CarePlanItemChangeResponse chỉ xuất added/removed/changed của TASK. Kiểm quyền cả hai phiên bản trước so sánh và chỉ cùng resident. GREEN 44 unit/Arch + 11 IT, exit 0/01:12 phút; log 005-p3-risk-compare-green-20261031.log.
- T014/T016a nhắc: test RED 2 failure do thiếu hai bước. Tạo CarePlanApprovalReminders dùng OverdueReminder/CFG-M15-05,06; CarePlanNotices lấy đúng ApproverApi và phát từng sự kiện chứa một người duyệt để giữ payload outbox nhỏ. CareRiskReview dùng ReminderMarkers theo agreement/type/date để nhắc xem lại thỏa thuận và nhận định một lần mỗi mốc, không sửa thỏa thuận hoặc tạo task. Các truy vấn ngày/bản ghi chờ nằm trong CarePlanStore/CareRiskStore. GREEN 44 unit/Arch + 13 IT, exit 0/01:49 phút; log 005-p3-reminders-green-20261031.log. Chưa dùng các kết quả này để đóng phase hoặc thay cho clean verify.

### Chuẩn bị web T017

Đọc frontend/README, DESIGN, component/permissions/client dùng chung; áp dụng ui-ux-pro-max cho label và quản lý focus (giữ hệ thiết kế hiện có, không thêm TypeScript/thư viện). Test carePlanClient.test.js RED 2 ca thiếu client 005. Generator trước đó chưa đăng ký 005; thêm đúng đăng ký 005 và lọc x-implementation-phase như 003. api-005.yaml đánh dấu 23 operation đã dịch/duyệt (11 kế hoạch Phase3, 12 danh mục Phase2), không sinh client cho phần hợp đồng còn tiếng Việt. Chạy npm run gen:api, tạo api005.js và cập nhật index.js, không sửa tay file sinh. Lệnh Python thêm metadata đã ghi file thành công nhưng print tiếng Việt báo lỗi encoding cp1258; không chạy lại để tránh lặp metadata. Kiểm đủ 23 dòng và generator sinh 9 feature. Test client GREEN 2/2, log 005-p3-web-client-green-20261031.log. Generator thuộc phần nền cần A review đúng hai dòng đăng ký/lọc, sẽ ghi bàn giao mục 8; không sửa package.json hoặc common.

Đã tạo trước CarePlanDetail.test.jsx (4 ca) và CareCatalogPage.test.jsx (3 ca), chạy RED vì chưa có component; log 005-p3-web-forms-red-20261031.log. Chưa ghi giao diện đã hoàn thành hoặc có kiểm hình ảnh. Còn cài các form/list thực, kiểm UI, retention ánh xạ R-15, review và clean verify cuối.

### Danh sách file Phase 3 theo thay đổi thực tế

Các file mới dưới đây có trong working tree; không tính file bàn giao003 hoặc mã nền Phase1–2 đã ghi ở phần trước. Không xóa file production. Hai harness kiểm hình ảnh tạm đã tạo rồi xóa, Vite đã dừng; không đưa fixture vào bản giao.

| File tạo | Mục đích |
| --- | --- |
| `backend/src/main/java/com/eldercare/backend/care/internal/careplan/domain/CarePlan.java` | Snapshot typed của một phiên bản và metadata vòng đời. |
| `backend/src/main/java/com/eldercare/backend/care/internal/careplan/domain/CarePlanComparison.java` | Kết quả/hàm so sánh projection CarePlanItemView của T010. |
| `backend/src/main/java/com/eldercare/backend/care/internal/careplan/domain/CarePlanDifference.java` | So sánh mục kể cả bản Nháp còn khung null theo source lineage. |
| `backend/src/main/java/com/eldercare/backend/care/internal/careplan/domain/CarePlanDifferenceEntry.java` | Một mục đổi trong so sánh nội dung nullable. |
| `backend/src/main/java/com/eldercare/backend/care/internal/careplan/domain/CarePlanItem.java` | Snapshot mục TASK, FK vật lý và gốc lineage. |
| `backend/src/main/java/com/eldercare/backend/care/internal/careplan/domain/CarePlanItemChange.java` | Một thay đổi trước/sau và trường đổi của projection T010. |
| `backend/src/main/java/com/eldercare/backend/care/internal/careplan/domain/CarePlanItemData.java` | Nội dung mục, khung nullable khi Nháp, projection API và các trường so sánh. |
| `backend/src/main/java/com/eldercare/backend/care/internal/careplan/domain/CarePlanItemSequence.java` | Chuỗi mốc theo bốn tần suất, reset mỗi ngày nghiệp vụ, không tạo công việc. |
| `backend/src/main/java/com/eldercare/backend/care/internal/careplan/domain/CarePlanTransitions.java` | Bảng chuyển FR-003 và kiểm biên ngày đề xuất/duyệt/thu hồi. |
| `backend/src/main/java/com/eldercare/backend/care/internal/careplan/domain/CareRiskAgreement.java` | Snapshot thỏa thuận, nhận định/quyết định, con và metadata ghi nhận. |
| `backend/src/main/java/com/eldercare/backend/care/internal/careplan/domain/CareRiskKinds.java` | Hai SharedKind con do M04 sở hữu cho RecordEventApi. |
| `backend/src/main/java/com/eldercare/backend/care/internal/careplan/domain/RiskDecisionMaker.java` | Dữ liệu người quyết định tầng2. |
| `backend/src/main/java/com/eldercare/backend/care/internal/careplan/domain/RiskOpinion.java` | Nguồn/nội dung ý kiến và active; không ghi đè nội dung lịch sử. |
| `backend/src/main/java/com/eldercare/backend/care/internal/careplan/domain/RiskOpinionEntry.java` | Ý kiến kèm actor/thời điểm từ record_event. |
| `backend/src/main/java/com/eldercare/backend/care/internal/careplan/domain/RiskParticipant.java` | Nội dung người tham gia và cờ active kỹ thuật của con. |
| `backend/src/main/java/com/eldercare/backend/care/internal/careplan/persistence/CarePlanStore.java` | SQL riêng care_plan/care_plan_item, current reads, đổi Instant theo zone nghiệp vụ. |
| `backend/src/main/java/com/eldercare/backend/care/internal/careplan/persistence/CareRiskStore.java` | SQL thỏa thuận trên care_plan_item và con qua RecordEventApi; sao chép, truy vấn ngày xem lại. |
| `backend/src/main/java/com/eldercare/backend/care/internal/careplan/service/CarePlanActivation.java` | Bước đổi hiệu lực nguyên tử, guard hồ sơ cuối/ngày hiện hành và báo người phụ trách. |
| `backend/src/main/java/com/eldercare/backend/care/internal/careplan/service/CarePlanApprovalReminders.java` | Bước nhắc duyệt/leo thang qua OverdueReminder, CFG và cờ chống lặp. |
| `backend/src/main/java/com/eldercare/backend/care/internal/careplan/service/CarePlanNotices.java` | Phân giải ApproverApi và chia payload một người duyệt mỗi event. |
| `backend/src/main/java/com/eldercare/backend/care/internal/careplan/service/CarePlanQueries.java` | Provider CarePlanApi thật và đọc HTTP theo phạm vi/role hợp lệ. |
| `backend/src/main/java/com/eldercare/backend/care/internal/careplan/service/CarePlanService.java` | Lệnh phiên bản, quyền/Reason/ETag/khóa/điều kiện, đóng băng khung, audit và sự kiện. |
| `backend/src/main/java/com/eldercare/backend/care/internal/careplan/service/CareRiskReview.java` | Bước nhắc xem lại từng loại ngày qua ReminderMarkers, không sinh công việc. |
| `backend/src/main/java/com/eldercare/backend/care/internal/careplan/service/CareRiskService.java` | Lệnh thay thỏa thuận, kiểm AC-03/nguồn/quan hệ/quyết định, audit cùng transaction. |
| `backend/src/main/java/com/eldercare/backend/care/web/CarePlanController.java` | 11 operation HTTP kế hoạch, ánh xạ request/response, header và đọc cả hai phạm vi khi so sánh. |
| `backend/src/main/java/com/eldercare/backend/care/web/request/CarePlanCreate.java` | Transport nhận đúng trường của hợp đồng kế hoạch/thỏa thuận, chuyển sang domain; không nhận metadata chỉ đọc. |
| `backend/src/main/java/com/eldercare/backend/care/web/request/CarePlanItemWrite.java` | Transport nhận đúng trường của hợp đồng kế hoạch/thỏa thuận, chuyển sang domain; không nhận metadata chỉ đọc. |
| `backend/src/main/java/com/eldercare/backend/care/web/request/CarePlanSubmit.java` | Transport nhận đúng trường của hợp đồng kế hoạch/thỏa thuận, chuyển sang domain; không nhận metadata chỉ đọc. |
| `backend/src/main/java/com/eldercare/backend/care/web/request/CareRiskAgreementWrite.java` | Transport nhận đúng trường của hợp đồng kế hoạch/thỏa thuận, chuyển sang domain; không nhận metadata chỉ đọc. |
| `backend/src/main/java/com/eldercare/backend/care/web/request/RiskDecisionMakerWrite.java` | Transport nhận đúng trường của hợp đồng kế hoạch/thỏa thuận, chuyển sang domain; không nhận metadata chỉ đọc. |
| `backend/src/main/java/com/eldercare/backend/care/web/request/RiskOpinionWrite.java` | Transport nhận đúng trường của hợp đồng kế hoạch/thỏa thuận, chuyển sang domain; không nhận metadata chỉ đọc. |
| `backend/src/main/java/com/eldercare/backend/care/web/request/RiskParticipantWrite.java` | Transport nhận đúng trường của hợp đồng kế hoạch/thỏa thuận, chuyển sang domain; không nhận metadata chỉ đọc. |
| `backend/src/main/java/com/eldercare/backend/care/web/response/CarePlanComparisonResponse.java` | Projection HTTP typed của kế hoạch/thỏa thuận/so sánh theo schema đã duyệt; không lộ cờ hoặc id lưu trữ kỹ thuật. |
| `backend/src/main/java/com/eldercare/backend/care/web/response/CarePlanItemChangeResponse.java` | Projection HTTP typed của kế hoạch/thỏa thuận/so sánh theo schema đã duyệt; không lộ cờ hoặc id lưu trữ kỹ thuật. |
| `backend/src/main/java/com/eldercare/backend/care/web/response/CarePlanItemResponse.java` | Projection HTTP typed của kế hoạch/thỏa thuận/so sánh theo schema đã duyệt; không lộ cờ hoặc id lưu trữ kỹ thuật. |
| `backend/src/main/java/com/eldercare/backend/care/web/response/CarePlanResponse.java` | Projection HTTP typed của kế hoạch/thỏa thuận/so sánh theo schema đã duyệt; không lộ cờ hoặc id lưu trữ kỹ thuật. |
| `backend/src/main/java/com/eldercare/backend/care/web/response/CareRiskAgreementResponse.java` | Projection HTTP typed của kế hoạch/thỏa thuận/so sánh theo schema đã duyệt; không lộ cờ hoặc id lưu trữ kỹ thuật. |
| `backend/src/main/java/com/eldercare/backend/care/web/response/RiskAssessmentResponse.java` | Projection HTTP typed của kế hoạch/thỏa thuận/so sánh theo schema đã duyệt; không lộ cờ hoặc id lưu trữ kỹ thuật. |
| `backend/src/main/java/com/eldercare/backend/care/web/response/RiskDecisionMakerResponse.java` | Projection HTTP typed của kế hoạch/thỏa thuận/so sánh theo schema đã duyệt; không lộ cờ hoặc id lưu trữ kỹ thuật. |
| `backend/src/main/java/com/eldercare/backend/care/web/response/RiskOpinionResponse.java` | Projection HTTP typed của kế hoạch/thỏa thuận/so sánh theo schema đã duyệt; không lộ cờ hoặc id lưu trữ kỹ thuật. |
| `backend/src/main/java/com/eldercare/backend/care/web/response/RiskParticipantResponse.java` | Projection HTTP typed của kế hoạch/thỏa thuận/so sánh theo schema đã duyệt; không lộ cờ hoặc id lưu trữ kỹ thuật. |
| `backend/src/main/resources/db/migration/V005_96__care_plan_revisions.sql` | Migration mới tạo2bảng BUSINESS, CHECK/unique và guard parent trên mục/con record_event. |
| `backend/src/test/java/com/eldercare/backend/care/BR_M04_19_CarePlanSchemaIT.java` | 8 test MySQL về unique, CHECK, DataGroup và khóa con theo trạng thái cha. |
| `backend/src/test/java/com/eldercare/backend/care/BR_M04_25_CarePlanItemSequenceTest.java` | 5 test chuỗi thời điểm, biên ca/ngày và khoảng N giờ. |
| `backend/src/test/java/com/eldercare/backend/care/FR_003_CarePlanConcurrencyIT.java` | 3 test: hai transaction MySQL chống ghi ngược snapshot, danh mục vừa ngừng, trạng thái đổi tại ranh giới khóa. |
| `backend/src/test/java/com/eldercare/backend/care/FR_003_CarePlanLifecycleIT.java` | 15 test HTTP thật: vòng đời/quyền/ETag/audit/thỏa thuận/nguồn/copy/nhắc và guard hiệu lực. |
| `backend/src/test/java/com/eldercare/backend/care/FR_003_CarePlanTransitionsTest.java` | 4 test bảng chuyển đủ trạng thái/lệnh, biên ngày và recheck ngày khi job chờ. |
| `backend/src/test/java/com/eldercare/backend/care/FR_008_CarePlanComparisonTest.java` | 2 test thêm/bớt/đổi và bỏ id bản sao theo lineage. |
| `frontend/src/api/api005.js` | Client sinh từ23operation đã dịch/duyệt, không sửa tay. |
| `frontend/src/features/care-tasks/CareCatalogPage.jsx` | Ba danh mục, quyền từ me/permissions, tạo/sửa/ngừng, version→If-Match và xử lý412. |
| `frontend/src/features/care-tasks/CareCatalogPage.test.jsx` | Test Vitest/Testing Library trước cài tương ứng: API/ETag/Reason/quyền/412 hoặc menu, không giả dữ liệu trong sản phẩm. |
| `frontend/src/features/care-tasks/CareField.jsx` | Label/điều khiển có tên truy cập được, kích thước tương tác và checkbox chỉ dùng trong005. |
| `frontend/src/features/care-tasks/carePlanClient.test.js` | Test Vitest/Testing Library trước cài tương ứng: API/ETag/Reason/quyền/412 hoặc menu, không giả dữ liệu trong sản phẩm. |
| `frontend/src/features/care-tasks/CarePlanDetail.jsx` | Tải chi tiết/ETag, lệnh theo allowedCommands, lỗi/412/Reason, form và nội dung chỉ đọc. |
| `frontend/src/features/care-tasks/CarePlanDetail.test.jsx` | Test Vitest/Testing Library trước cài tương ứng: API/ETag/Reason/quyền/412 hoặc menu, không giả dữ liệu trong sản phẩm. |
| `frontend/src/features/care-tasks/CarePlanItemsForm.jsx` | Form typed các trường mục công việc và bốn tần suất, không tự rải giờ. |
| `frontend/src/features/care-tasks/carePlanLabels.js` | Nhãn tiếng Việt cho enum trong hợp đồng005. |
| `frontend/src/features/care-tasks/carePlanNavigation.test.js` | Test Vitest/Testing Library trước cài tương ứng: API/ETag/Reason/quyền/412 hoặc menu, không giả dữ liệu trong sản phẩm. |
| `frontend/src/features/care-tasks/CarePlanPage.jsx` | Tìm/chọn hồ sơ, danh sách phiên bản, lệnh lập Nháp thủ công, so sánh và DetailLayout. |
| `frontend/src/features/care-tasks/CarePlanPage.test.jsx` | Test Vitest/Testing Library trước cài tương ứng: API/ETag/Reason/quyền/412 hoặc menu, không giả dữ liệu trong sản phẩm. |
| `frontend/src/features/care-tasks/CareRiskAgreementsForm.jsx` | Form nhận định, người quyết định, người tham gia và từng ý kiến có nguồn; metadata ý kiến cũ chỉ đọc. |

| File sửa trong Phase 3 | Mục đích |
| --- | --- |
| `backend/src/main/java/com/eldercare/backend/care/internal/governance/domain/CareRetentionType.java` | Đích RISK_AGREEMENT là care_plan_item; ý kiến là con bảng chung. |
| `backend/src/main/java/com/eldercare/backend/care/internal/governance/persistence/CareGovernanceStore.java` | Retention lọc kind, trả đúng DataRef, xóa đúng loại; không query/xóa SQL record_event. |
| `backend/src/main/java/com/eldercare/backend/care/internal/scheduling/service/CareDailyWorkRunner.java` | Transaction READ_COMMITTED cho bước job sau chờ khóa, marker/tác động vẫn cùng transaction. |
| `backend/src/main/java/com/eldercare/backend/care/web/response/TaskTypeResponse.java`, `ResultValueResponse.java`, `NonPerformanceReasonTypeResponse.java` | Thêm version chỉ đọc đã được chủ dự án duyệt. |
| `backend/src/test/java/com/eldercare/backend/care/FR_038_CatalogContractTest.java`, `FR_038_CareCatalogHttpIT.java`, `FR_028a_ReasonCatalogHttpIT.java` | Test schema/response version và version sau cập nhật. |
| `backend/src/test/java/com/eldercare/backend/care/T008_CareGovernanceIT.java` | Fixture kế hoạch dùng schema thật, id riêng và kiểm tách retention TASK/RISK. |
| `frontend/scripts/gen-api.mjs` | Chỉ đăng ký/lọc phần005 đã duyệt như003; cần A review. |
| `frontend/src/api/index.js` | Sinh lại để xuất client005. Generator viết lại các client khác với nội dung không đổi; không sửa tay các file này. |
| `frontend/src/App.jsx`, `frontend/src/common/layout/navigation.js` | Thêm đúng hai đăng ký màn/menu005 theo CARE_PLAN/CARE_TASK_CATALOG; không thay logic chung. |
| `specs/005-daily-care-tasks/contracts/api-005.yaml` | version readOnly cho3danhmục và23metadata x-implementation-phase, không thêm endpoint. |
| `specs/005-daily-care-tasks/contracts/module-interfaces.md`, `data-model.md`, `plan.md`, `tasks.md` | Provider thật/phạm vi story, ánh xạ vật lý R-15, quyết định/kiểm/việc còn lại; checkbox theo gate. |
| `specs/000-shared-foundation/glossary.md` | Chỉ bổ sung thuật ngữ phần11 của005, không sửa phần module khác. |
| `docs/team-plan.md`, `docs/progress.md`, `Working-history.md` | Bàn giao/gate/nhật ký thực tế; progress chỉ thêm, history giữ nội dung cũ. |

Luồng đọc/ghi: UI → client api005 sinh → CarePlanController → CarePlanService/CareRiskService (aspect quyền, Reason, transaction) → CareLockStore/resident lock → CarePlanStore/CareRiskStore; con qua RecordEventApi, danh mục qua CatalogApi, cấu hình qua ParameterApi. AuditLogApi và event publisher trong transaction ghi; CarePlanQueries chỉ đưa TASK sang CarePlanApi, thỏa thuận đọc riêng cho HTTP. CareDailyJob/SchedulerRuns → CareDailyWorkRunner → các bước hiệu lực/nhắc; ReminderMarkers và cờ reminded_at/escalated_at chống lặp.

Kiểm cuối hiện tại: full frontend361/64file, lint/build exit0. Backend reviewGREEN45unit/Arch+17IT; create-final-raceGREEN41Arch+3IT,0failure/error/skip,exit0/02:21 phút. Clean verify toàn backend đang chạy nền ở005-p3-clean-verify-20261031.log, chưa ghi đạt. Đang chờ quyết định API cho lý do thông báo CarePlanReturned, chưa sửa record công khai đó.

### Bổ sung kiểm cuối Phase 3 ngày 2026-10-31

- `FR_003_CarePlanConcurrencyIT`: thêm ca trạng thái hồ sơ đổi tại ranh giới khóa, chạy RED trước sửa; `CarePlanService.create` lấy khóa resident trước khi kiểm writable. GREEN của nhóm này: 41 ArchTest + 3 IT, exit 0.
- `FR_003_CarePlanLifecycleIT`: thêm sáu ca payload có phần tử null trong mục, giờ, thứ, thỏa thuận, người tham gia và ý kiến. RED có sáu lỗi NullPointerException; sửa `CarePlanController`, `CarePlanItemWrite`, `CareRiskAgreementWrite` để trả lỗi giá trị không hợp lệ 422. Thêm ca kiểm OpenAPI POST tạo kế hoạch khai 201, chạy đỏ trước khi bổ sung `@ApiResponse` và ETag đúng hợp đồng. Nhóm HTTP cuối qua 41 ArchTest + 22 IT, không failure/error/skip, exit 0, 02:18 phút; log `005-p3-http-final-green-20261031.log` trong thư mục Temp của người dùng.
- Tạo `frontend/src/features/care-tasks/CarePlanItemsForm.test.jsx` để tái hiện nhập nhiều giờ: dấu phẩy bị loại khi đang gõ. RED một test; sửa `CarePlanItemsForm.jsx` giữ `timesText` tại form và chỉ chuyển thành mảng khi gửi, bỏ trường kỹ thuật khỏi payload. GREEN một test. File test mới bổ sung cho danh sách file ở trên; không có file sản phẩm bị xóa.
- Sau sửa, chạy toàn frontend: `npm test -- --run` qua 362 test/65 file; `npm run lint` và `npm run build` đều exit 0. Build còn cảnh báo chunk lớn hơn 500 kB; chưa kiểm hình ảnh giao diện vì giới hạn truy cập ứng dụng đã ghi ở phần trước.
- Lượt backend `005-p3-clean-verify-20261031.log` chạy 732 unit test và 1590 IT (7 failure, 9 error); cuối lệnh thất bại Spotless do bổ sung test Java trong lúc verify đang chạy. Đây không phải bằng chứng gate đạt. Đã chạy định dạng rồi khởi động lại `./mvnw clean verify`, log `005-p3-clean-verify-final-20261031.log`; giữ nguyên nguồn Java/JS trong suốt lượt cuối. Chưa ghi kết quả của lượt đang chạy.
- `CarePlanReturned.reason` vẫn là đề xuất đổi API chờ chủ dự án duyệt. Chưa đổi record, chưa ghi nhận thông báo đã mang lý do. Không dùng việc tiếp tục kiểm độc lập làm sự đồng ý thay hợp đồng.

### Kết quả chốt lượt thực hiện Phase 3 — 2026-10-31

`./mvnw clean verify` (Windows ` .\mvnw.cmd clean verify`) chạy nền với JDK25, log `C:/Users/nguye/AppData/Local/Temp/005-p3-clean-verify-final-20261031.log`, kết thúc exit1 sau17:42 phút. 732 unit/ArchTest không lỗi; 1597 IT có7failure+9error, không skip. Đọc XML xác nhận90ITcare,258ITroombedallocation,251ITstaffing không failure/error. Spotless2297file sạch, không cần đổi file nào. So sánh đủ tên phương thức với `005-p2-clean-verify-final-20261031.log`: hai tập cùng16phương thức, không thêm/bớt;15resident do nguồn/kiểm hợp đồng củaB và1common.ApiContractIT do chưa đăng ký005 củaA. Số operation005 cần A review hiện là23 trên19path; không coi chỉ cùng tên lỗi là payload lỗi không đổi hoặc hợp đồng chung đã đạt.

Frontend cuối:362test/65file, lint/build exit0 (các log `005-p3-web-{test,lint,build}-final-20261031.log`). Không sửa Java/JS trong lượt clean verify cuối. Sau đó chỉ bổ sung tài liệu; `git diff --check` được dùng kiểm thay đổi văn bản, không thay cho test nghiệp vụ.

Giữ nguyên tất cả checkbox Phase3 và chưa báo phase hoàn thành: gate A/B còn đỏ; đề xuất `CarePlanReturned.reason` chưa có câu trả lời duyệt; nguồn FamilyRelationshipApi thật chưa được tích hợp kiểm đầu–cuối; kiểm hình ảnh web chưa thực hiện; phần đăng ký generator/menu/trigger bảng chung còn cần A review. Không tạo commit, push hoặc PR; nhánh vẫn `feature/005-daily-care-and-hand-over`, HEAD `728a9f94`.

Thứ tự tiếp theo: T011/T014 chốt và kiểm lý do thông báo Trả lại; T016 A đăng ký/review hợp đồng chung; T016a kiểm nguồn người thân thật; T017 kiểm hình ảnh/review phần web chung. Khi gate được chủ phần A/B giải quyết, chạy lại clean verify và nghiệm thu Phase3. Sau checkpoint, task tiếp theo trong tasks.md là T018 rồi T019 thuộc Phase4; chưa triển khai hai task này trong lượt hiện tại. T057 callback trạng thái cuối giữ ở phase đã quy định, không đánh hoàn thành bằng guard kích hoạt hiện tại.

### Bổ sung theo phê duyệt CarePlanReturned.reason — 2026-10-31

Chủ dự án trả lời phương án1: duyệt thêm String reason, giữ constructor cũ và chỉ đưa lý do vào ContentPart HEALTH; title/shortContent vẫn là nội dung chung. Đọc lại tasks/plan và dùng quy trình TDD. Lần chạy đầu gặp quyền ghi Maven cache ngoài workspace; chạy lại với quyền Maven/MySQL. Test mới ban đầu dùng sai assertion OptionalAssert.doesNotContain nên lỗi biên dịch; sửa assertion trên chuỗi, không tính hai lỗi kỹ thuật này là bằng chứng RED nghiệp vụ. RED đúng hành vi trong `005-p3-return-reason-red-20261031.log`:1test/1failure, thông báo chỉ có GENERAL thay vì HEALTH chứa lý do.

File thay đổi và luồng xử lý:

- `backend/src/main/java/com/eldercare/backend/care/api/events/CarePlanReturned.java`: thêm thành phần reason ở cuối record, giữ constructor ba tham số gán null; sự kiện cũ giữ thông báo chung. Sự kiện có lý do tạo ContentPart HEALTH, giữ level LOW, eventKey và người nhận.
- `backend/src/main/java/com/eldercare/backend/care/api/events/CareEventNotifications.java`: overload nội bộ nhận danh sách ContentPart, dùng chung phần header/title/shortContent an toàn; không thay API công khai của module khác.
- `backend/src/main/java/com/eldercare/backend/care/internal/careplan/service/CarePlanService.java`: lệnh RETURN phát CarePlanReturned với nguyên Reason.value sau cập nhật/audit cùng transaction; không thay lý do hoặc giảm giới hạn HTTP.
- `backend/src/test/java/com/eldercare/backend/care/FR_003_CarePlanLifecycleIT.java`: kiểm HTTP Trả lại sinh HEALTH chính xác, không đưa lý do vào title/shortContent; thêm ca lưu rồi đọc lại payload tối đa trên EVENT_PUBLICATION, dữ liệu test cô lập bằng UUID và rollback transaction.
- `backend/src/test/java/com/eldercare/backend/care/NFR_04_CareEventsTest.java`: kiểm constructor cũ, JSON không có reason, giữ eventKey và roundtrip đầy đủ lý do tối đa. Không thêm fake thứ hai.
- `specs/005-daily-care-tasks/contracts/module-interfaces.md`, `tasks.md`, `plan.md`, `specs/000-shared-foundation/glossary.md` phần11.6, `docs/team-plan.md`, `docs/progress.md`, `Working-history.md`: ghi hợp đồng được duyệt, bàn giao, kiểm và giới hạn thực tế. Không sửa spec/nghiệp vụ, API HTTP, thư viện, migration đã merge hoặc nguồn/schema nền củaA.

Phát hiện giới hạn outbox bằng test: lý do hợp lệ2000dấu ngoặc kép tạo JSON4233ký tự, vượt VARCHAR(4000); log `005-p3-return-reason-payload-red-20261031.log` có7test/1failure,6test còn lại xanh. Đề xuất đổi duy nhất cột SERIALIZED_EVENT sang TEXT NOT NULL; chủ dự án chọn phương án2: chờ A mở rộng outbox trước. Vì vậy không tạo migration thay schema nền. Thay assertion độ dài cố định bằng ca lưu/đọc thật trên MySQL: sau khi A mở rộng, ca này tự kiểm được khả năng chứa và nguyên vẹn nội dung; unit test vẫn kiểm roundtrip. Không tắt/xóa ca biên hoặc cắt lý do để lấy kết quả xanh.

Đã chạy `spotless:apply` chỉ cho care, exit0; lượt kiểm task `-Dtest=NFR_04_CareEventsTest,*ArchTest -Dit.test=FR_003_CarePlanLifecycleIT verify` đang chạy, chưa ghi kết quả. Mã reason hiện là bản cài tại checkout chờ gate lưu payload tối đa, không coi đã sẵn sàng merge/triển khai. Giữ checkbox T011/T014 mở; ưu tiên tiếp theo là A cung cấp migration outbox rồi C chạy lại ca biên và clean verify, sau đó tiếp các việc T016/T016a/T017 đã ghi ở trên.

Kết quả lượt task trên: exit1/01:59phút;47unit/Arch xanh,23IT có1error/0failure/0skip. Ca FR_003_maximumEscapedReturnReasonSurvivesOutboxStorage đỏ với MysqlDataTruncation/DataIntegrityViolationException: Data too long for column SERIALIZED_EVENT.22IT còn lại xanh, bao gồm HTTP Trả lại có HEALTH chứa nguyên lý do và title/shortContent không chứa lý do. Spotless2297file sạch. Không coi đây là lỗi baseline cũ: đây là dependency/ca biên mới của thay đổi reason, đã trình chủ dự án và được chọn chờ A. Không chạy lại clean verify toàn kho trong lúc dependency này chưa thay đổi; phải chạy lại sau khi A cung cấp migration, không lấy kết quả clean verify trước reason làm bằng chứng cho mã mới. Đang kiểm riêng ModuleBoundaryTest vì tên lớp này không khớp glob *ArchTest; kết quả sẽ bổ sung bên dưới.

ModuleBoundaryTest chạy riêng bằng `-Dtest=ModuleBoundaryTest test`:1test xanh,0failure/error/skip,exit0/18.429giây;log005-p3-return-reason-boundary-20261031.log. Tổng hai lượt task/kiến trúc có48unit/Arch/Boundary xanh;23IT vẫn giữ1error outbox đã nêu. `git diff --check` không báo lỗi khoảng trắng; chỉ có cảnh báo chuyển LF/CRLF của Git. Không có thay đổi mã Java sau gate task. Các kết quả frontend trước đây không chạy lại vì lượt bổ sung chỉ sửa backend và tài liệu.

## Phiên Phase 4 Spec 005 — 2026-11-01 (đang triển khai)

Yêu cầu: T018–T026 theo AGENTS/team-plan, test trước, chỉ kiểm task+kiến trúc từng lát cắt, clean verify nền cuối phase trước checkbox; không commit/push. Đã đọc lịch sử cũ và giữ nguyên nội dung. Checkout đầu lượt: feature/005-daily-care-and-hand-over, HEAD df032811; Working-history.md và specs/003-room-bed-allocation/hand-over-task-003.md đã untracked từ trước, không đụng file bàn giao003.

Đầu phiên xác nhận mục8 có7dòng ghi HungNX-nguyen: reviewAPI/event/job005; công bố8SPI005; DayCareCapacityApi003; chờ nguồn nhiễm bệnhD; BedAllocationCommandApi003/dấu giá; reviewgenerator/menu/triggerPhase3; chờA mở rộng outbox reason. Không ghi xong các dependency này.

Trình tự thực tế: đọc tasks/plan/readme và tài liệu liên quan; phát hiện7operationPhase4 còn tiếngViệt và SPI thiếu phép liệt kê; soạn phase4-contract-proposal.md. Chủ dự án duyệt mục1–3 bằng “1”: HTTP/JSON tiếngAnh, rowErrors từ1, sources/members giữ chữ ký cũ. Câu hỏi bổ sung ongoingShifts (mục5) và cho phép sửa ApiClient mobile củaA (mục6) đang chờ, chưa tự áp dụng.

T018: tạo FR_026_CareTaskTransitionsTest trước, chạy đỏ5failure/0error, rồi CareTaskTransitions ở internal/task/domain. Hàm next trả Optional cho cặp trạng thái/sự kiện; không mở lại trạng thái cuối; bàn giao chỉ đóng mứcNORMAL. initialStatus nhận now từ caller, đúng hai biên cònDUE, saucuối mớiOVERDUE. GREEN46test gồm5testmới+Arch/ModuleBoundary. Log C:/Users/nguye/AppData/Local/Temp/005-p4-transitions-{red,green}-20261101.log. Lượt sandbox đầu vướng cacheMaven, không tính RED; Spotless lầnđầu dùng glob sai cú pháp regex, sửa tham số rồi định dạng2file exit0, không đổi plugin.

T019:10IT BR_M04_01_ShiftTaskPlanningIT viết trước code, RED10failure404/0error. Nội dung: lôhợplệ/read/audit, lỗi dòng2 và rollback cảlô, trùngDBR12/cancel/lậplại, trùngtronglô, vắngmặt, bántrúchưađến, chứngchỉ, quyềnCS, lậpDUE/OVERDUE, nguồnkhônggợiý. Test hợpđồng FR_017_ShiftTaskContractTest RED2test trước dịch/SPI, saudịch đãxanh cùngkiếntrúc.

File triển khai mới: CareTaskDraft (lệnh), CareTaskData (snapshot), CareTask (trạngthái), CareTaskHandling (lịchsửchung), CareTaskValidation (kếtquảkiểm), CareTaskTransitions ở care/internal/task/domain; CareTaskStore ở persistence chỉSQLbảngcare_task; CareTaskValidator/CareTaskService/CareTaskQueries/CareTaskSources ở service điềuphối kiểm/khóa/ghi/API nguồn; CareShiftSources làprojectiondomain. V005_97__shift_care_tasks.sql tạo care_task BUSINESS, generatedUNIQUE bỏquaCANCELLED, uniquekiểmtraca/điềudưỡng, CHECKtarget/window, index vàtriggerchặnđổitrạngtháicuối. Không tạo bảnglịchsửđãgộp.

HTTP mới: CareTaskController điềuphối7operation; CareTaskWrite/CareTaskReworkWrite ở web/request chuyểnthànhdraft; CareTaskResponse/CareTaskDetailResponse/CareShiftSourceResponse ở web/response trảđúngprojection. Cácnullablefield chưa códữliệuphaseghi nhận giữnull, khôngbịavalue. FileSPI TaskSourcePort/ShiftStaffPort và fake hiệncó FakeTaskSource/FakeShiftStaff sửađúngphầnduyệt; sources resetcùngfixture, khôngtạofakeproduction. api-005.yaml bổsungbảndịchgiữschema cũchostorychưadịch; frontend/src/api/api005.js sinhlạibằngnpm run gen:api.

Luồng: HTTP→CareTaskWrite.draft→CareTaskService (quyền,khóacưdântăngdần,READ_COMMITTED)→CareTaskValidator (catalog/ResidentApi/ContractApi/ShiftStaffPort/TaskSourcePort/CarePlanQueries)→kiểmtoànbộlô→CareTaskStore.insert; StatusHistoryApi vàAuditLogApi cùngtransaction. CareTaskQueries chỉđọcnguồnđãcó; CareTaskSources callbackMANDATORY khôngtạocôngviệc. FakeM04Config cònkèmfakeCarePlanApi cũ; query005 dùngCarePlanQueries thật trongcùngmodule để khôngnhầmfakevớinguồnthật.

Kiểm lượtđầu GREEN bịlỗicompile AccountView.id khôngtồntại;đãđọcDTO,sửaaccountId. Lượtkế48unit/Archxanh,10IT9xanh/1error dofakeeffectiveOnchưađặt;đãsửainjectionnộibộnhưtrên. Verify cuối lượtđócònbáoSpotless vìbổsungtesttronglúcchạy;khôngcoiBUILDđạt. Log005-p4-planning-green2-20261101.log. HaiITcallbackđãRED2failure/0errorthiếuprovider,trướcCareTaskSources;log005-p4-callbacks-red-20261101.log. Bổsung7testunitvalidator,4ITcallback/kiểmđầuca/triggerchưachạyxácnhận.

Web: tạo ShiftTaskPlanningPage.test.jsx trướccomponent, importfailkhichưacófilekhôngtínhREDhànhvi;scaffoldreturnnullchạyRED3failure/1pass. CàiShiftTaskPlanningPage.jsx theoDESIGN, chỉdòngtựnhập,nguồnđốichiếu,giữdòngkhi409vàIdempotency-Keyqua retrycùngpayload. Lượtđầuhànhvi4testpassnhưng1unhandlederrorvìPermissionDeniedStatethiếulabel;đãđốichiếucomponentchung,sửalabelvàtestđợithôngbáothật. ChưaghiGREENcuối, chưađăngkýmennu, chưaKIỂMUIhoặcmobile.

Thứtựtiếptheo: hoànthiệnT018validator/T019IT;kiểmT020–T023vàT025đếnxanh;T024sauquyếtđịnhmục5;T026web/mobilevàadapter sauquyếtđịnhmục6;chạykiểmcuốifrontend/Flutter;clean verify nềnvớinguồncốđịnh;đọclog/phânbiệtgateA/B/outbox;tài liệuvàcheckboxchỉcậpnậttheobằngchứng. Chưacófilebịxóavàchưađánh[x]Phase4.

### Nhật ký bổ sung Phase 4 và quyết định chờ — 2026-11-01

Các đoạn “đang chờ trả lời”, “chưa đăng ký menu” và số test trung gian phía trên là trạng thái tại thời điểm ghi. Các kết quả và quyết định sau đây cập nhật trạng thái tiếp theo, không xóa lịch sử cũ.

**Yêu cầu và phạm vi:** tiếp tục T018–T026 trên nhánh `feature/005-daily-care-and-hand-over`; không tạo nhánh, commit, push hoặc PR. Chủ dự án duyệt mục 1–3 của `phase4-contract-proposal.md`; chọn hoãn mục 5 (nguồn mọi ca đang diễn ra), mục 6 (sửa mobile common), và chọn chờ 007 thống nhất cảnh báo công việc Bắt buộc gắn vị trí. Không sửa `spec.md`, tài liệu nghiệp vụ, dependency manifest, `common/` backend hoặc `mobile/lib/common/`.

**Trình tự đã thực hiện tiếp:**

1. Hoàn thiện validation và HTTP lô nguyên tử, thêm kiểm hủy/lập lại, nguồn kế hoạch thật, kiểm chuyên môn, trạng thái khi lập muộn và callbacks MANDATORY. `CareTaskWrite.shared`/`CareTaskReworkWrite.shared` dùng `Boolean` tùy chọn, chuẩn hóa `false`; test RED trước sửa vì Jackson từ chối null cho primitive.
2. Thêm `CareTaskNotices` và `CareTaskDueStep`: marker và sự kiện cùng transaction, chạy bù chuyển việc chưa đến hạn tới trạng thái tương ứng tại thời điểm chạy, nhắc ban đầu khi đã quá hạn, không sinh công việc. Chưa cài nhắc mọi ca rỗng, chưa cài bước DUE → OVERDUE của T044/Phase 7.
3. Test review phát hiện thiếu mã lỗi theo dòng cho loại công việc không tồn tại, hủy vẫn được khi job trễ, chi tiết mất lý do hủy. Đã sửa trả lỗi dòng, kiểm thời gian thật khi hủy và đọc `CareTaskClosure`; không thay hợp đồng.
4. Lượt clean verify đầu lỗi `BackendApplicationTests.contextLoads` do thiếu bean `ContractApi` production. Sửa injection sang `ObjectProvider<ContractApi>`, báo thiếu nguồn tường minh khi dùng, không đưa fake vào production. Lượt `005-p4-startup-source-green.log` sau sửa đã qua các test chọn và 28 IT; đây chưa phải gate toàn kho cuối.
5. Test nguồn dùng chung giữa hai người phát hiện trùng eventKey; thêm residentId vào milestone `TaskSourceChanged`, không thay hợp đồng event. RED trước sửa, GREEN cùng kiểm startup.
6. Review chỉ đọc phát hiện lô REWORK bỏ qua kiểm chất lượng, Target dùng tầng do ca cung cấp mà chưa xác thực người thuộc tầng, nguồn ca đêm thiếu kế hoạch ngày sau, danh sách người thực hiện vị trí thiếu thành viên chưa được phân công theo người. Viết test tái hiện: 3 IT đều RED; web 1/6 RED; mobile 1/5 RED. Sau đó mới sửa: originalTaskId bắt buộc cho REWORK; kiểm phân bổ tại plannedAt/phân công thật trước quyền; hợp nhất kế hoạch các ngày giao với ca nửa mở; roster dùng GET /shifts/{id} của 008, lọc người absent/approvedLeave. Reviewer đọc lại xác nhận các sửa khớp đường mã và schema/quyền 008; không chạy test thay người triển khai.
7. Web đăng ký `care-task-planning` vào App/navigation; dùng client sinh, không tự điền dòng từ nguồn, giữ nội dung khi lỗi, lỗi đánh số từ 1, giữ Idempotency-Key khi retry cùng payload. Mobile làm widget/gateway tiêm phụ thuộc; test fixture chỉ ở test. Lỗi xóa dòng làm nội dung dòng sau nhảy sai đã RED rồi sửa key ổn định trên Column. Màn mobile chưa đăng ký vào StaffRoot và chưa có adapter production, đúng quyết định chờ A.
8. Flutter đã tự sinh lại file registrant của các nền tảng, chỉ thay xuống dòng; đã so byte nội dung chuẩn hóa với HEAD rồi phục hồi CRLF. Không thay logic các file sinh. Script phục hồi lần đầu gặp UnicodeEncodeError khi in tiếng Việt sau file đầu; chạy tiếp với output ASCII để hoàn tất.

**Danh sách file mới và quan hệ (đường dẫn dưới đây tính từ gốc project):**

| File | Mục đích |
| --- | --- |
| `backend/src/main/java/com/eldercare/backend/care/internal/task/domain/CareTask.java` | Bản ghi công việc có trạng thái/version và khóa nguồn. |
| Cùng thư mục: `CareTaskDraft.java`, `CareTaskData.java` | Dữ liệu lệnh và snapshot nội dung typed; không lưu dữ liệu tùy ý từ client. |
| Cùng thư mục: `CareTaskValidation.java`, `CareTaskTransitions.java` | Lỗi kiểm từng dòng và bảng chuyển thuần. |
| Cùng thư mục: `CareTaskHandling.java`, `CareTaskClosure.java`, `CareShiftSources.java` | Chi tiết lịch sử chung, thông tin đóng và projection nguồn ca. |
| `backend/src/main/java/com/eldercare/backend/care/internal/task/persistence/CareTaskStore.java` | SQL bảng care_task, khóa dòng, uniqueness, overlap, chuyển trạng thái và đọc closure. |
| `backend/src/main/java/com/eldercare/backend/care/internal/task/service/CareTaskValidator.java` | Kiểm nguồn, hiện diện, khung, vai trò/chứng chỉ và trùng căn cứ; không ghi. |
| Cùng thư mục: `CareTaskService.java`, `CareTaskQueries.java` | Lệnh transaction/quyền/khóa/audit/history và đọc nguồn/chi tiết. |
| Cùng thư mục: `CareTaskSources.java` | Provider TaskSourceApi với propagation MANDATORY; callback nguồn không sinh việc. |
| Cùng thư mục: `CareTaskNotices.java`, `CareTaskDueStep.java` | Nhắc ban đầu/sự kiện và bước chạy trên CareMinuteWork hiện có; không tạo scheduler riêng. |
| `backend/src/main/java/com/eldercare/backend/care/web/CareTaskController.java` | 7 operation đã duyệt; ánh xạ DTO, quyền và che dữ liệu sức khỏe. |
| `backend/src/main/java/com/eldercare/backend/care/web/request/CareTaskWrite.java`, `CareTaskReworkWrite.java` | Payload lập/phát sinh/làm lại, chuyển draft. |
| `backend/src/main/java/com/eldercare/backend/care/web/response/CareTaskResponse.java`, `CareTaskDetailResponse.java`, `CareShiftSourceResponse.java` | Projection HTTP đúng schema; chi tiết bổ sung closure/history. |
| `backend/src/main/resources/db/migration/V005_97__shift_care_tasks.sql` | Bảng BUSINESS care_task, generated UNIQUE, CHECK/index/trigger trạng thái cuối; không thêm bảng lịch sử đã gộp R-15. |
| `backend/src/test/java/com/eldercare/backend/care/FR_026_CareTaskTransitionsTest.java` | Ma trận chuyển và biên thời gian. |
| Cùng thư mục: `FR_017_CareTaskValidatorTest.java`, `FR_017_CareTaskRequestTest.java`, `FR_017_ShiftTaskContractTest.java` | Kiểm validator, optional shared, hợp đồng HTTP/SPI. |
| Cùng thư mục: `BR_M04_01_ShiftTaskPlanningIT.java` | MySQL/HTTP thật cho các kịch bản T019, audit, concurrency, callbacks, nguồn, quyền và job. |
| `frontend/src/features/care-tasks/ShiftTaskPlanningPage.jsx`, `.test.jsx`, `shiftTaskPlanningNavigation.test.js` | Màn web và test tương tác/đăng ký quyền. |
| `mobile/lib/staff/care_tasks/care_task_planning_gateway.dart`, `shift_task_planning_screen.dart` | Hợp đồng nội bộ widget và màn lập thủ công; shiftMembers nhận roster thật từ adapter tương lai. |
| `mobile/test/staff/care_tasks/shift_task_planning_screen_test.dart` | 5 widget test với gateway fixture riêng test. |
| `specs/005-daily-care-tasks/phase4-contract-proposal.md` | Nội dung đã duyệt và các phần chủ dự án chọn hoãn. |

**File đã sửa:** `care/api/spi/{TaskSourcePort,ShiftStaffPort}.java` và `support/fake/care/{FakeTaskSource,FakeShiftStaff}.java` thêm đúng sources/members và fixture/reset tương ứng, giữ chữ ký cũ. `specs/005-daily-care-tasks/contracts/api-005.yaml` dịch 7 operation/schema; `frontend/src/api/api005.js` sinh lại bằng `npm run gen:api`. `frontend/src/App.jsx` và `frontend/src/common/layout/navigation.js` đăng ký màn/menu, bàn giao A review. `specs/005-daily-care-tasks/{tasks,plan,data-model}.md`, `contracts/module-interfaces.md`, glossary 000 mục 11.7 ghi mapping vật lý, quyết định, thuật ngữ và trạng thái. `docs/team-plan.md` thêm các việc chờ E/D/A và review nền; giữ các dòng chưa xong. `Working-history.md` chỉ bổ sung. Chưa xóa file sản phẩm nào; hai file visual harness tạm trong frontend đã tạo rồi xóa sau lần thử UI không thành công, không phải phần bàn giao.

**Luồng dữ liệu hiện tại:** controller → request.draft → service kiểm quyền và phạm vi thật → khóa người tăng dần → validator đọc catalog/nguồn qua API, kế hoạch qua CarePlanQueries nội bộ → gom rowErrors, chỉ insert nếu cả lô hợp lệ → CareTaskStore/care_task + StatusHistoryApi/AuditLogApi cùng transaction. UNIQUE SQL chốt race đồng thời. GET nguồn chỉ đọc dữ liệu tham chiếu; người dùng tự lập. Job và callback nguồn chỉ đổi trạng thái/nhắc các bản ghi đã có. Web gọi client sinh; mobile gateway chưa được nối transport, không coi widget xanh là tích hợp production.

**Kết quả kiểm đã xác nhận trước gate cuối:**

- `005-p4-planning-final-green.log`: 56 unit/Arch/Boundary và 27 IT đều xanh, Spotless 2323 file; exit0. Sau đó có sửa Java nên không dùng làm gate cuối.
- `005-p4-review-green.log`: 58 unit/Arch/Boundary xanh; 31 IT có 30 xanh/1 failure tại biên window_from. Ba lỗi review đều hết. Nguyên nhân ca biên: Instant trong fixture có nano nhưng MySQL lưu DATETIME(6); test phải dùng window_from/window_to đã lưu. Không thay assertion thành khoảng dung sai hoặc sửa đồng hồ production.
- Web: `npm test` 368 test/67 file xanh; `npm run lint` và `npm run build` exit0. Build còn cảnh báo bundle lớn hơn500kB. Log `005-p4-web-final-{tests,lint,build}.log`.
- Mobile: test riêng 5/5 xanh (`005-p4-mobile-roster-green.log`); `flutter analyze --no-pub lib/staff/care_tasks test/staff/care_tasks` không có issue; toàn bộ `flutter test --no-pub` 49/49 xanh (`005-p4-mobile-final-tests.log`). Lệnh analyze đầu tự thử pub đã dừng và chạy lại --no-pub; không thêm dependency. Một lần gọi full test bị automatic approval review không thực thi vì usage limit, không phải kết luận unsafe; sau yêu cầu “tiếp tục” của chủ dự án đã chạy được và có kết quả thật.
- UI thực tế chưa kiểm được: IAB localhost timeout, Chrome accessibility không khả dụng. Không chụp màn hình, không khẳng định đã xem responsive/dark mode.

Các log cuối ở `C:/Users/nguye/.codex/visualizations/2026/10/31/01a19373-e167-7ec3-b1f3-367d283ca782/`; các log cũ TEMP giữ nguyên tên đã ghi. Kết quả clean verify và kiểm biên sau cùng sẽ được bổ sung khi lệnh kết thúc, không ghi trước.

Đính chính phạm vi thử nghiệm job: `005-p4-clock-expiry-red.log` chạy41 Arch/Boundary xanh và1IT có1failure tại assert OVERDUE sau cuối khung. Đã xác nhận đầu khung dùng mốc đã lưu thì DUE đúng; failure này thuộc bước DUE → OVERDUE của T044/Phase7, không thuộc T024 đang triển khai. Hoàn nguyên phần assert thử nghiệm ngoài phase, giữ các assert đầu/cuối khung và lặp không tạo mới của T024. Không viết implementation T044 hoặc coi test khám phá này là task hoàn thành; các test bắt buộc Phase4 về lập muộn/catch-up/nhắc ban đầu giữ nguyên. Test cuối đang chạy lại riêng task+ArchTest, sẽ ghi số và kết quả thực sau khi hoàn tất.

**Xác minh task cuối:** `005-p4-final-scoped.log`: BUILD SUCCESS, exit0 sau01:11phút;58unit/Arch/Boundary +31IT đều0failure/error/skip, Spotless2323file sạch. Review chỉ đọc cuối xác nhận phạm vi T024/T044, test biên và tài liệu nhất quán; không phát hiện finding mới ngoài dependency đã ghi. Đã bắt đầu nền `.\mvnw.cmd clean verify` toàn backend với Java25 và biến DB từ backend/.env, không in thông tin nhạy cảm. Log `005-p4-clean-verify-final.log`; chưa dùng kết quả chưa kết thúc. Không sửa Java/UI sau khi bắt đầu gate cuối.

Lượt clean verify `005-p4-clean-verify-final.log` đã qua751unit/Arch,31ITPhase4 xanh nhưng phát hiện lỗi tích hợp T008: fixture Phase2 tự CREATE TABLE care_task, nay bảng thật đã có. Đã chủ động dừng lượt này bằng Ctrl+C để sửa sớm; đây là lượt bị ngắt, không phải kết quả kiểm toàn kho hoàn chỉnh. Báo cáo lúc đó cũng có ParameterAsOfIT/A, ApiContractIT/A và lỗi outbox reason đã biết; không tự kết luận toàn bộ lỗi là baseline.

Sửa cần thiết khi nối schema story: `backend/src/test/java/com/eldercare/backend/care/T008_CareGovernanceIT.java` dùng care_task thật từ V005_97, TestUsers.nextId cho mọi bản ghi, không CREATE/DROP bảng. Giữ kiểm đồng hồ retention người và vị trí, không lẫn hai loại, scope của vị trí, và bổ sung xóa thật hai task qua RetentionProvider (tài khoản retention) cùng kiểm xóa plan. Cleanup chỉ id do test tạo, không giả định bảng rỗng. Lỗi Table already exists đã tái hiện trước sửa; đang kiểm riêng T008 và ArchTest rồi mới khởi động clean verify mới với nguồn cố định. Không sửa production/provider hoặc test của A/B.

Kiểm sau nối schema T008: `005-p4-governance-green.log` BUILD SUCCESS,exit0/01:01phút;41Arch/Boundary +6IT xanh,0failure/error/skip;Spotless2323file sạch. Reviewer đọc test xác nhận không làm yếu bảo đảm, cleanup cô lập theoid. Khởi động nền cleanverify mới `005-p4-clean-verify-final2.log`, không ghi đè log lượt bị ngắt và không sửa Java sau điểm này.

### Kết quả gate cuối và bàn giao — 2026-11-01

`005-p4-clean-verify-final2.log` đã kết thúc, **exit 1 sau 10 phút 14 giây**. Không có Java/UI thay đổi trong lượt này:

| Phạm vi | Kết quả thực |
| --- | --- |
| Unit/Arch toàn backend | 751 test, 0 failure/error/skip. |
| IT toàn backend | 1.629 test, 8 failure, 10 error, 0 skip. |
| `BR_M04_01_ShiftTaskPlanningIT` | 31/31 xanh trong lượt clean cuối. |
| `T008_CareGovernanceIT` | 6/6 xanh với schema thật; lỗi fixture đã hết. |
| care toàn bộ | 122 IT, 121 xanh, 1 error outbox reason của Phase3 đã biết. |
| roombedallocation / staffing | 258 / 251 IT, đều xanh. |
| Spotless | 2.323 file sạch. |
| Web | 368 test/67 file xanh; lint/build exit0, còn cảnh báo bundle >500kB. |
| Mobile | Toàn bộ49 test xanh;5 test màn Phase4 xanh; analyze phần Phase4 không có issue. |

Đối chiếu **tên từng phương thức**, không chỉ số lỗi, với `C:/Users/nguye/AppData/Local/Temp/005-p3-clean-verify-final-20261031.log`: đủ 16 phương thức baseline còn nguyên, không mất phương thức nào. Hai ca thêm so baseline đó:

- `FR_003_CarePlanLifecycleIT.FR_003_maximumEscapedReturnReasonSurvivesOutboxStorage`: bổ sung sau baseline Phase3, đã biết MySQL không chứa được reason escape trong SERIALIZED_EVENT VARCHAR(4000); chủ dự án chọn chờ A. Không phải lỗi mới phát hiện từ runtime Phase4, nhưng vẫn là gate đỏ hiện tại.
- `ParameterAsOfIT.US4_8_valueAtPastInstant_isTheValueCurrentThen`: expected20M, actual10M; tái hiện ở cả lượt bị ngắt và lượt cuối. Đã thêm dòng bàn giao A ở team-plan mục8; chưa kết luận nguyên nhân hoặc gọi đây là baseline cũ. Không sửa nguồn/test A.

Các lệnh kiểm cuối (chạy ở thư mục tương ứng, Java25 và DB từ biến môi trường, không ghi giá trị nhạy cảm):

```powershell
# backend: bộ task Phase4, BUILD SUCCESS
.\mvnw.cmd '-Dtest=FR_026_CareTaskTransitionsTest,FR_017_CareTaskValidatorTest,FR_017_CareTaskRequestTest,FR_017_ShiftTaskContractTest,*ArchTest,ModuleBoundaryTest' '-Dit.test=BR_M04_01_ShiftTaskPlanningIT' verify
# backend: nối schema thật T008, BUILD SUCCESS
.\mvnw.cmd '-Dtest=*ArchTest,ModuleBoundaryTest' '-Dit.test=T008_CareGovernanceIT' verify
# backend: định dạng phần Java thay đổi và gate toàn kho nền
.\mvnw.cmd '-DspotlessFiles=.*care.*' spotless:apply
.\mvnw.cmd clean verify
# frontend
npm run gen:api
npm test
npm run lint
npm run build
# mobile: SDK hiện có, không thêm dependency
flutter test --no-pub test/staff/care_tasks/shift_task_planning_screen_test.dart
flutter analyze --no-pub lib/staff/care_tasks test/staff/care_tasks
flutter test --no-pub
```

Log cuối đều nằm trong `C:/Users/nguye/.codex/visualizations/2026/10/31/01a19373-e167-7ec3-b1f3-367d283ca782/`. `git diff --check` không báo lỗi khoảng trắng; Git chỉ cảnh báo LF/CRLF trên một số tài liệu/client sinh. Nhánh vẫn `feature/005-daily-care-and-hand-over`, HEAD `df032811`, chưa commit/push/PR; file handover003 ban đầu không sửa. Không có file sản phẩm nào bị xóa.

**Trạng thái và việc tiếp theo theo thứ tự `tasks.md`:**

1. T018 có test đơn vị/ma trận xanh; T019 còn test nhắc ca chưa có công việc, chỉ có thể hoàn thiện khi E cung cấp nguồn đầy đủ đã được chốt. Không tạo test xanh giả từ danh sách tầng suy ra từ task/giường.
2. T020–T023 đã có migration/domain/store/validator/service và IT xanh tại checkout; giữ gate ghép nguồn thật, kiểm lại khi provider E/D được cung cấp. Không coi fake xanh là nghiệm thu production.
3. T024 hoàn thiện nhắc ca rỗng sau nguồn E; thống nhất với 007 cảnh báo MANDATORY gắn vị trí. **Guard503 hiện vẫn có thể rollback toàn transaction chạy bù** khi gặp việc này. Đây là vấn đề chưa giải quyết, không phải hành vi đã nghiệm thu.
4. T025 kiểm lại sources/members và callback với provider production; thay đổi HTTP/SPI mục1–3 đã được duyệt, các thay đổi khác chưa tự áp dụng.
5. T026 chờ A mở transport mảng JSON/rowErrors rồi cài adapter production và đăng ký mobile vào app; review App/navigation với A, kiểm UI thực tế còn thiếu. Không chỉnh mobile common theo quyết định chủ dự án.
6. Sau các phần trên và gate A/B/outbox được xử lý, chạy lại kiểm liên quan và clean verify trước checkbox. T027/Phase5 chưa bắt đầu; DUE→OVERDUE thuộc T044/Phase7, không cài trong lượt Phase4.

Toàn bộ checkbox Phase4 giữ `[ ]`, vì phase chưa đủ điều kiện nghiệm thu. `docs/team-plan.md` giữ các dòng chưa xong và thêm bàn giao E/D/A; `docs/progress.md` chỉ thêm dòng, ghi cả lượt bị ngắt và lượt cuối. `plan.md`, `tasks.md`, hợp đồng module, glossary và đề xuất ghi các quyết định hoãn đã nhận, không để nhầm là còn đợi câu trả lời. Nhật ký cũ giữ nguyên; mọi kết quả trung gian được phân biệt với gate cuối.

Danh sách 18 phương thức đỏ trong lượt clean cuối:

- `ApiContractIT.R_14_generatedApi_matchesContract`
- `BR_M01_01_TemporaryAbsenceOverdueIT.BR_M01_01_afterRecordReturn_noAlert`
- `BR_M01_02_ReassessmentNoticeIT.T048_recordReturnFromHospital_startsTheDeadlineForUnmetNotices_andAddsReturnBasis`
- `BR_M01_02_ReassessmentNoticeIT.T048_recordReturnFromTemporaryAbsence_doesNotNotify`
- `CompleteAdmissionConsentIT.FR_012_withActiveConsent_completesWithoutWarningOrReminder`
- `CompleteAdmissionConsentIT.FR_012_withoutActiveConsent_completesWithWarning_andStartsReminding`
- `CompleteAdmissionWishWarningsIT.US7_11_withoutATreatmentWish_completesWithWarning_andNeverReminds`
- `CompleteAdmissionWishWarningsIT.US7_4_withBothActiveWishes_completesWithoutEitherWarningOrReminder`
- `CompleteAdmissionWishWarningsIT.US7_4_withoutAnEndOfLifeWish_completesWithWarning_andStartsReminding`
- `FR_003_CarePlanLifecycleIT.FR_003_maximumEscapedReturnReasonSurvivesOutboxStorage`
- `FR_023h_KhongDauNangLucIT.FR_023h_openApi_hasNoCapacityField`
- `FR_034a_CompleteAdmissionIT.FR_034a_assessmentOlderThanCfgM0102_blocksAndNamesBothConditionsWhenOthersAreUnmet`
- `FR_034a_CompleteAdmissionIT.FR_034a_atNinetyDays_assessmentIsStillValid`
- `FR_034a_CompleteAdmissionIT.FR_034a_confirmedAssessmentWithinCfgM0102_allowsCompleteAdmission_andMergesOtherConditions`
- `FR_034a_CompleteAdmissionIT.FR_034a_noConfirmedInitialAssessment_blocksCompleteAdmission`
- `ParameterAsOfIT.US4_8_valueAtPastInstant_isTheValueCurrentThen`
- `ResidentContractIT.R_14_implementedResidentEndpoints_matchContract`
- `StatusHistoryIT.FR_046_historyEndpointListsEntriesOldestFirstWithActorName`

## Phiên Phase 5 Spec005 — 2026-11-01

Yêu cầu T027–T031: đọc tasks/plan trước, TDD, kiểm task+ArchTest sau task, clean verify nền cuối; chỉ sửa phần C, không commit/push/đổi nhánh/thư viện. Đã đọc lịch sử cũ và giữ nguyên. Đầu phiên có12dòng chứa HungNX-nguyen ở team-plan mục8 và đã báo đủ12dòng. HEAD hiện tại fe7891ea (`update 005 daily care phase 4`) xuất hiện từ thay đổi bên ngoài, không phải AI commit. Checkout đầu phiên chỉ có Working-history.md và hand-over-task-003.md untracked; giữ cảhai,không sửa handover003.

Đọc tasks Phase5/plan Ghi chú triển khai, data-model5 vàR15, contracts/C-06, backendREADME và các API hiện có. Các gate Phase4/A/B/outbox giữ nguyên; user yêu cầu Phase5 là phạm vi mới, không coi phase trước đã nghiệm thu. Đã tạo phase5-contract-proposal.md cho4HTTP tiếngAnh, DayCareCalendarPort (ngày nghỉ/báo vắng), refreshCalendar callbackMANDATORY thay listener khi004 chưa công bố kiểu event. Đang chờ câu trả lời API trước khi áp dụng. T027 bắt đầu bằng FR_013_AttendanceTransitionsTest, reflection để mã test biên dịch trước implementation; kiểm mọi cặp trạng thái/sự kiện và không tự checkout hết ngày. Chưa ghi RED/GREEN trước khi lệnh chạy kết thúc.

Checkpoint T027 phần thuần: FR_013_AttendanceTransitionsTest RED2failure/0error trước AttendanceTransitions; sau cài GREEN43unit/Arch/Boundary,exit0. Hàmnext chỉ trả chuyển hợp lệ; HOLIDAY_CANCELLED là dòng kỹ thuật biểu diễn không còn buổi, ARRIVE vào ngày nghỉ là buổi phát sinh theoQ140. Không tựcheckout cuối ngày. T027 chưa đủ IT toànluồng nênkhôngđánh[x].

T028 viết FR_012_AttendanceSchemaIT trước migration:4IT có3failure/0error (trùngngười/ngày lọt,UNKNOWN lọt,thiếuindex),1testloạikhác xanh. Lượt verifyđỏ cònSpotless dofiletestmới chưafomat; đãđọcbáocáo xácnhận3failure hànhvi thật,khôngcoiSpotlesslàREDnghiệpvụ. Sau đó mới thêm V005_94__shared_day_care_attendance.sql (số94 còntrống,đúngquyđịnhmởrộngbảngchung):simple_record cóm04_attendance_date từeffective_from,m04_expected_arrival từcontent.expectedArrival,UNIQUEresident/date,CHECK6trạngthái+khóangười/ngày,chỉmụcchongày/vắngkhôngbáo;loạikhácnhậnNULLkhôngbịràngbuộc. Khôngtạobảngcare_day_attendance hoặc care_attendance_record. GREEN41Arch/Boundary+4IT,exit0/01:01phút;log005-p5-schema-green.log. ĐangbổsungtestbuổiphátsinhexpectedArrivalJSONnull trước khichốtschema;chưaghiKQkhi chưa xong.

Review chỉđọc (agentreview_phase4) kiểm4fileđộclập+đềxuất:chưathấyfindingbắtbuộc,bảngFR013/migration/fixtuređúngphạmvi,khôngAPI/fakeproductionchưaduyệt. ReviewkhôngchạytestvàkhôngthaythếITT027toànluồng. ChưacàiT029–T031khiđềxuấtAPIcònchờ;khôngmặcđịnhcalendarngàynghỉ/báovắngfalse.

### Kiểm buổi phát sinh và giới hạn checkpoint Phase 5

Test `FR_013_adHocSessionMayHaveNoExpectedArrival` được bổ sung trước khi sửa biểu thức cột sinh. Lượt `005-p5-ad-hoc-red.log` có 1 IT/1 error: MySQL `Truncated incorrect time value: 'null'`. Đây là lỗi thật khi `expectedArrival` có JSON null của buổi ngoài lịch. Sửa migration chưa commit bằng `NULLIF(JSON_UNQUOTE(...),'null')` trước `CAST AS TIME`; không thay null thành giờ nghiệp vụ giả. Đang chạy đủ 43 unit/Arch/Boundary và 5 IT schema trong `005-p5-checkpoint-green.log`, chưa ghi xanh trước kết quả.

Các file được tạo trong phần độc lập:

| File | Mục đích và quan hệ |
| --- | --- |
| `backend/src/test/java/com/eldercare/backend/care/FR_013_AttendanceTransitionsTest.java` | Test trước cho bảng FR-013, mọi cặp trạng thái/sự kiện; hết ngày không tự checkout. |
| `backend/src/main/java/com/eldercare/backend/care/internal/attendance/domain/AttendanceTransitions.java` | Hàm thuần `next`, sử dụng AttendanceStatus đã công bố; chưa có service lưu điểm danh. |
| `backend/src/test/java/com/eldercare/backend/care/FR_012_AttendanceSchemaIT.java` | MySQL thật: duy nhất người/ngày, trạng thái hợp lệ, hai index, loại khác không bị ảnh hưởng và giờ dự kiến null của buổi phát sinh. |
| `backend/src/main/resources/db/migration/V005_94__shared_day_care_attendance.sql` | Mở rộng simple_record đúng R-15, cột/index m04 và CHECK/UNIQUE riêng kind M04.DAY_CARE_ATTENDANCE. |
| `specs/005-daily-care-tasks/phase5-contract-proposal.md` | Đề xuất cụ thể 4 operation HTTP, calendar SPI và callback cần duyệt; chưa áp dụng. |

File đã sửa: `specs/005-daily-care-tasks/plan.md` ghi thứ tự và lý do lựa chọn kỹ thuật; `tasks.md` ghi checkpoint, giữ T027–T031[ ]; `data-model.md` ghi ánh xạ physical kind/column và phần record_event chưa có runtime; `specs/000-shared-foundation/glossary.md` chỉ thêm mục11.8 của005; `docs/team-plan.md` thêm việc A review migration bảng chung; `Working-history.md` chỉ bổ sung lịch sử. Chưa xóa file nào, chưa sửa UI/hợp đồng API/module khác hoặc thêm thư viện.

Luồng hiện tại chỉ là phần nền: test ma trận → AttendanceTransitions; Flyway → ALTER simple_record → test SQL có transaction rollback và TestUsers id riêng. Chưa có luồng HTTP → service → SimpleRecordApi/RecordEventApi, chưa có calendar provider, job hoặc CorrectionProvider Phase5. Không ghi các phần này đã thực hiện. `co_mat_ban_tru` không có bảng riêng; điểm danh nhóm3 phải qua record_event khi cài T029.

### Kết quả checkpoint trước điểm duyệt API — Phase 5

Lượt `005-p5-checkpoint-green.log` chưa đạt: 43 unit/Arch xanh, 5 IT có 1 error ở giờ thật `08:00:00` sau khi dùng NULLIF. Giữ nguyên test để phát hiện hồi quy; không dùng kết quả 4 IT trước đó làm bằng chứng cuối. Thay biểu thức bằng `CASE WHEN kind=... AND JSON_TYPE(...)= 'STRING' THEN CAST(... AS TIME) ELSE NULL END`, phân nhánh rõ JSON string so với JSON null/missing. Không ghi lý do là một lỗi cụ thể của MySQL khi chưa chứng minh nội bộ; bằng chứng là hai payload thật và kết quả SQL trong test.

**Kiểm cuối phần độc lập:** `005-p5-checkpoint-green2.log` BUILD SUCCESS, exit0 sau47.274giây; **43 unit/Arch/Boundary và5 IT schema,0failure/error/skip**, Spotless2.326file sạch. Đã kiểm cả giờ thật lẫn JSON null, giữ mọi assertion UNIQUE/trạng thái/loại khác/index. `git diff --check` không có lỗi khoảng trắng (chỉ cảnh báo LF/CRLF). Không có Java hoặc migration thay đổi sau lượt kiểm này.

Lệnh đã chạy ở backend với JAVA_HOME=E:/JAVA/JDK_GE/INS_25 và biến DB từ .env, không in giá trị nhạy cảm:

```powershell
.\mvnw.cmd '-Dtest=FR_013_AttendanceTransitionsTest,*ArchTest,ModuleBoundaryTest' test
.\mvnw.cmd '-DspotlessFiles=.*care.*' spotless:apply
.\mvnw.cmd '-Dtest=*ArchTest,ModuleBoundaryTest' '-Dit.test=FR_012_AttendanceSchemaIT' verify
.\mvnw.cmd '-Dtest=*ArchTest,ModuleBoundaryTest' '-Dit.test=FR_012_AttendanceSchemaIT#FR_013_adHocSessionMayHaveNoExpectedArrival' verify
.\mvnw.cmd '-Dtest=FR_013_AttendanceTransitionsTest,*ArchTest,ModuleBoundaryTest' '-Dit.test=FR_012_AttendanceSchemaIT' verify
```

Các lần RED/GREEN và log tách riêng ở `C:/Users/nguye/.codex/visualizations/2026/10/31/01a19373-e167-7ec3-b1f3-367d283ca782/005-p5-*.log`; không ghi đè log đỏ. Không chạy npm/Flutter vì chưa sửa UI, không chụp màn hình. Chưa chạy clean verify cuối Phase5: đây là checkpoint chờ duyệt API, không phải nghiệm thu cuối phase; cần cài/kiểm T029–T031 trước gate cuối theo yêu cầu. Các kết quả toàn kho của Phase4 không đại diện mã hiện tại.

**Vấn đề còn lại và thứ tự tiếp tục đúng tasks.md:**

1. T027: viết IT luồng điểm danh đến/về, bản ghi đón dùng lại, hủy đúng công việc chưa đến hạn, dời/khôi phục giờ về, báo vắng/ngày nghỉ, đính chính và thông báo hết ngày. Hiện mới có ma trận và schema test, chưa hoàn tất T027.
2. T028: migration phần ràng buộc đã kiểm; tiếp tục model/payload shared và kết nối API dùng chung với runtime, bàn giao A review migration trước merge. Không tự coi bảng chung là runtime đã xong.
3. T029: cần duyệt đề xuất API trước khi thêm calendar SPI/callback hoặc dịch HTTP; sau đó cài service và CorrectableRecordProvider, provider DayCareAttendanceApi; TrangThaiCoMatBanTruPort/kiểu event nguồn vẫn cần B công bố. Không mặc định thiếu nguồn là không nghỉ/không báo vắng.
4. T030: chỉ đăng ký bước ngày/phút vào khung CareDailyWork/CareMinuteWork hiện có sau khi đủ nguồn; không tự sinh bản ghi điểm danh từ job.
5. T031: áp bản dịch đã duyệt, test HTTP rồi cài controller/web/mobile; giữ quyết định không sửa mobile common của Phase4, nếu cần transport thì bàn giao A.
6. Sau khi các task đủ điều kiện: chạy clean verify nền với nguồn cố định, đọc kết quả thực và các gate nền còn mở trước checkbox. Chưa bắt đầu T032/Phase6.

Đề xuất API vẫn chưa có câu trả lời tại checkpoint này. Dừng đúng điểm cần duyệt theo AGENTS.md luật5: “Cần đổi thì hỏi chủ dự án.” Không áp dụng đề xuất, không đánh[x]T027–T031, không báo hoàn thành Phase5. Chưa commit/push/PR; HEAD vẫnfe7891ea. Đã thêm tiến độ chỉ-append vào docs/progress.md; giữ nguyên lịch sử cũ và file handover003.

## Tiếp tục Phase5 sau duyệt API — 2026-11-01

Chủ dự án trả lời “1” duyệt toàn bộ phase5-contract-proposal.md:4HTTP cùng trường tiếngAnh,DayCareCalendarPort và callbackMANDATORY DayCareAttendanceApi.refreshCalendar. Giữ chữkýcũ. ĐãviếtFR_012_AttendanceContractTest trước:RED2failure; sauđóthêmSPI/callback,FakeDayCareCalendar duy nhất vàbeantrongFakeM04Config;fakechưađặtngàybáolỗi,khôngcófakeproduction. Dịchđúng4operation/schema/requestđínhchínhriêng vàsinhclientnpmrungen:api(thêm4operation). GiữđịnhdạngYAMLcũngoàicácblockPhase5 đểkhôngđịnhdạnglạitoànfile.

T027 luồng:BR_M04_02_DayCareAttendanceIT viết10testtrướcservice/controller. HaivruncóimportMockMvcthiếu,RoletrùngSpringannotation,đãsửatest vàkhôngtínhlàREDhànhvi. REDthậtở005-p5-flow-red3.log:10IT có8failure404+2errorchưaprovider,43unit/Arch/Boundaryxanh. CàiAttendanceData/AttendanceRecordData/AttendanceKinds/AttendanceDaySource,AttendanceStoređiAPIshared,AttendanceSourcesđiAPI004/001/003vàcalendar005,AttendanceServicequyền/khóa/ghi/audit/events,AttendanceQueriescàiDayCareAttendanceApivàlọclistscope,controller/request/responseđúngtầng. LượtcoređầuArchTestbắtcontrollerđipersistence;đãchuyểntruyvấnsangAttendanceQueries,khôngsửatestkiếntrúc. GREEN005-p5-core-green2.log:43unit/Arch/Boundary+10IT,0failure/error/skip,Spotless2342file,exit0/01:18phút.

Bảngchung:attendanceSimpleRecordparentM01.RESIDENT vàrecord_eventđiểmdanhcũngparentM01.RESIDENT,AttendanceRecordData giữattendanceId đểlọcđúngbuổi. Lýdo:SharedRowRetention000xóatoànchuỗitheohồsơngườikhihếthạn,khôngđểrecord_eventmồcôikhiSimpleRecordgốcbịloạibỏ. KhôngSQLbảngchungtrongproduction005. Ngàyởeffective_from;thờigianghinhậnvàthờigianthựctếgiữriêng,cácbảnghiđiểmdanhđượcconfirmngay,bảnggốckhôngsửatrựctiếp.

T029/T030 thêm5testtrướccácphầntiếp:checkoutchỉhủycôngviệcNOT_YET_DUEcủađúngngườisaugiờvề;daystartkhôngtạobảnghiđiểmdanh;noShowđúngbiênCFG_M02_07,mộtlần;enddaychỉnhắckhôngtựcheckout;đínhchínhgiờkhôngsửagốc. RED005-p5-clock-red.log:15IT có5failure/0error,10cađầuvẫnxanh,41Arch/Boundaryxanh. SauREDđãbắtđầunốihủycôngviệcởAttendanceService/CareTaskStore,typedAttendanceCheckoutResult/DayCareCheckoutResponse;chưaghiGREENkhichưakiểm. Thêmcasengàynghỉsaukhiđãđếnđểkiểmkhoảnglịchđãthựchiệnkhôngbịxóa.

T031web:DayCareAttendancePage.test.jsx RED4/4 trướcmàn;4GREENsaucài.Caerrorfixtuređầugọisaihelperproblem(status,errorCode,extra),đãđốichiếuhelpersửalạitest,khôngsửacommon. Mànnguồnlưutrạngtháitheongày;chỉlệnhngườidùngghigửi;giữform/lỗi/keykhiretry;checkoutgửipickupRecordIdchứkhôngtựchọnloạivề;formđínhchínhgiờAMEND_CONTENT. NavigationtestRED1trướcdòngmenu/đăngkýApp;scoped5testxanh. Skillui-ux-pro-maxqueryformsubmissionerrorfeedbacktrảđúng3mục:loading/success/error,phụchồilỗi,feedbackgầnform;ápcomponent/tokenhiệncó.

Mobile:DayCareAttendanceGateway/Data vàscaffoldscreencho3testwidgetREDtrướckhicài;005-p5-mobile-red.log3failure,005-p5-mobile-green.log3testxanh. Widgetchỉtrựctuyến,khôngtựgửilệnh,giữngườiđãchọn/keykhilỗi,quyềnchỉxemkhônghiệnlệnh. Sourceởmobile/lib/staff/care_tasks,fixturechỉởtest. KhôngsửamobilecommonhoặcStaffRoot/adapterproduction:GETattendancecủahợpđồngtrảmảng,ApiClientAhiệnchỉtrảMap;giữquyếtđịnhchờAcủaPhase4.

ĐãgửicâuhỏinghiệpvụriêngvềVOID(Hủyghinhậnđiểmdanh):FR015chốtđínhchínhgiờnhưngchưachốttrạngthái/côngviệctínhlạikhiVOID,khôngtựsuyđoán. Câuhỏiđangchờ,độc lậpvớiphầnAMEND_CONTENTcóthểtiếptụclàm. ChưanóiPhase5hoànthành,chưađánh[x],chưachạycleanverifycuốiphase.

### Review cuối phần đã cài Phase5 (chưa nghiệm thu)

Review độc lập chỉ đọc đã xác nhận hai P1 phải giữ làm gate:

- Retention alias005 còn trỏ bảng logic cũ không tồn tại. ParentM01.RESIDENT của raw shared rows không đủ để `CorrectionRetention.attachedTo` nhìn thấy từng `M04.ATTENDANCE_RECORD` và kiểm thời hạn bản đính chính. Purge có thể xóa bản gốc trước hạn của đính chính, để lại correction/chain. **Đính chính nhận định trước đó:** parent liên kết người chưa bảo đảm “toàn chuỗi”; đã thêm bàn giao A ở team-plan mục8. Không sửa nguồnA hoặc SQL shared từ005.
- Callback đổi loại lưu trú: sau004 đổi DAY_CARE→nội trú, `refreshCalendar` hiện bị checkResident409, rollback lệnh ngoài; row cũ cũng có thể làm cả lượt minute job rollback. Chưa có quy tắc nghiệp vụ cho trạng thái/công việc/tính phí ngày đổi loại. Đã hỏi chủ dự án chờB hoặc chốt; không tự dùng HOLIDAY_CANCELLED hay bỏ dữ liệu.

Review thêm P2: VOID chỉ bị chặn trong afterCorrection nên cáchC có thể tạo yêu cầu không áp dụng được. Đang có test unit trước sửa để endpoint005 từ chối trước CorrectionApi.create; public generic000 vẫn cần chính sách loại đính chính ở provider sau khi nghiệp vụ chốt. Câu hỏi nghiệp vụVOID vẫn chờ, không coi đó là quyền tự chọn hành vi.

Scope theo R15 đã có RED1 InvalidPermissionData MISSING_RECORD khi provider cũ đọc care_day_attendance; sửa CareGovernanceStore.scope đi AttendanceStore/APIshared, giữ MissingRecord khi thiếu. GREEN005-p5-scoped-green.log:45unit/Arch/Boundary+22IT(17luồng+5schema),0failure/error/skip,Spotless2350file,exit0/01:22phút. Minute/endday refactor dùng cùng snapshot đã khóa resident tăng dần, không query thêm người cóid nhỏ hơn giữa transaction; marker riêng từngattendance/ngày để bù dữ liệu ghi muộn.

NPMfull lầnđầu(005-p5-web-full-tests.log) có3testfail+1unhandlederror:iconUserCheckkhôngcótrongregistryICONS,gâyApp/navcomponentundefined. KhôngsửaAregistryhaytestchung;đổiđúngdòngmenu005sangClipboardCheckđãcósẵn. Kiểmliênquan5file/App/nav/màn:12test xanh,log005-p5-web-icons-green.log. Đây là lỗi của bảncài005,khônggọi làbaseline. Lint/buildlượtđóexit0vớicảnhbáobundlechung. Mobilefull005-p5-mobile-full-tests.log52testxanh;analyze3filePhase5khôngissue.

ThêmtestđổingàytronglúcPOSTpending:RED1/5doôNgàyvẫnsửađược,khirefreshcóthểtrộnngày;đãdisabledtheobusy,saucài6testmàn/menuxanh(005-p5-web-date-green.log). Thêmtestauditdờigiờ:RED1failurevìdata_afterchỉstatus,thiếucurrentExpectedReturn;đangcàidiffbefore/afterđủgiờ,chưaghiGREEN.

## Bàn giao trạng thái Phase 5 sau duyệt và hai quyết định chờ — 2026-11-01

Chủ dự án đã chọn sửa giờ bằng `AMEND_CONTENT` trước, giữ `VOID` chưa hỗ trợ; đồng thời chọn chờ thống nhất với B về ngày chuyển loại lưu trú từ bán trú sang nội trú. Hai câu hỏi đã có câu trả lời, không còn pending approval. Các chữ ký/HTTP tại mục1–3 của phase5-contract-proposal.md đã được duyệt từ trước và đã áp dụng. Không sửa `spec.md` hoặc tài liệu nghiệp vụ để tự định nghĩa phần chưa chốt.

### Trình tự triển khai tiếp và quyết định quan trọng

- Cài luồng cơ bản sau RED10 IT; sửa controller truy cập persistence theo ArchTest bằng cách đi qua AttendanceQueries. Lượt cơ bản đã xanh10IT và43unit/Arch/Boundary.
- Sau RED5 test bổ sung, cài hủy công việc theo giờ về, job ngày/phút và provider đính chính. Test ngày nghỉ sau khi đã đến phát hiện mất giờ lịch ban đầu (RED1); sửa giữ lịch của buổi đã đến. Lượt đó16IT xanh cùng41Arch/Boundary.
- Scope provider T008 đọc bảng attendance logic chưa tồn tại đã RED1; nối sang AttendanceStore/APIshared và giữ lỗi metadata khi thiếu dòng. Sau đó17ITflow+5schema và45unit/Arch/Boundary xanh.
- Job ngày chạy theo thứ tự hiệu lực kế hoạch (`@Order(0)`), xác định attendance (`@Order(10)`), báo hết ngày (`@Order(20)`). Ngày chạy bù lấy từ các khoảng DAY_CARE của ContractApi, không đặt ngày khởi đầu giả. Job phút dùng CFG_M02_07; nhắc hết ngày có marker riêng theoattendance/ngày và được bù cả khi dòng Có mặt được ghi muộn sau mốc ngày của runner. Không tự điểm danh về hoặc tạo công việc. Các khóa người được giữ tăng dần trên snapshot đã đọc, không query thêm người thấp hơn vào giữa transaction.
- Đính chính giữ RecordEvent gốc đã xác nhận; cập nhật projection giờ và phát AttendanceChanged khi 000 thực sự áp dụng. Không tự hủy/khôi phục thêm công việc do sửa giờ về: nguồn hiện chỉ quy định sửa giờ/projection; tự mở lại task đóng trái FR-026. Đây là ruling kỹ thuật/nghiệp vụ còn giới hạn, cần chốt tác động dây chuyền nếu muốn mở rộng, không coi review đã xác nhận toàn luồng đó.
- Review xác nhận retention P1 và callback đổi loại lưu trú P1; đã bàn giao A/B. Review P2 VOID cáchC đã có unit RED1 trước sửa; endpoint005 chặn409 trước CorrectionApi.create. Entry generic000 vẫn cần A nối capability/loại đính chính trước tạo approval, không coi guard provider sau apply là đủ.
- Audit dời giờ đã RED1 vì chỉ logstatus; sửa diff trước/sau chỉ gồm trường thay đổi, giữ null và giờ hiện hành cùng id bản ghi/đăng ký/báo vắng để truy vết. Cập nhật phân loại trường của005 tương ứng. Lượt cuối bộ task trước sửa YAML:46unit/Arch/Boundary +29IT đều xanh, Spotless2.351file,exit0/01:50phút.
- Web có6testmàn/menu xanh sau RED. Toàn web lần đầu lỗi3test+1unhandled vì iconUserCheck không có trong registry. Sửa đúng dòng menu005 sang ClipboardCheck có sẵn, giữ registry/testA nguyên vẹn;12test liên quan xanh, rồi toàn bộ374test/69file xanh,lint/buildexit0. Build còn cảnh báo bundle>500kB. Test đổi ngày trong lúc gửi đã RED1 rồi khóa ô ngày theo busy để không trộn dữ liệu hai ngày.
- Mobile có3widgettest RED trước implementation;3test riêng và52test toàn mobile xanh, analyze3file mới khôngissue. Widget trực tuyến nhận gateway/quyền, giữ input/key khi lỗi, không tạo hàng đợi ngoại tuyến. Chưa có adapter production/đăng ký app vì ApiClientA không đọc JSON array; giữ quyết định chờ A của phase trước. Không sửa mobile common hoặc thêm dependency.
- Đọc lại C-02 phát hiện mô tả giao B cài trực tiếp careSPI/call careAPI từ admission sẽ tạo vòng khi care đã dùng ContractReadApi. Chỉnh cách bàn giao: B công bố API/sự kiện admission; adapter/listener thuộc care đọc nguồn và gọi callback đồng bộ cùng transaction. Giữ chữ ký đã duyệt, chưa viết nguồn/eventB chưa có hợp đồng. Không xem test fake là chứng cứ tích hợp production.

### File thực tế và luồng xử lý

Các đường dẫn trong bảng là tính từ gốc project. Không có file sản phẩm nào bị xóa. Scaffold màn được thay nội dung trong cùng file, không có thao tác xóa file bàn giao.

| File mới | Mục đích |
| --- | --- |
| `backend/src/main/java/com/eldercare/backend/care/api/spi/DayCareCalendarPort.java` | Hợp đồng calendar đã duyệt; thiếu nguồn production vẫn là gate B. |
| `backend/src/main/java/com/eldercare/backend/care/internal/attendance/domain/AttendanceTransitions.java` | Bảng chuyển FR-013 thuần, không tự checkout. |
| Cùng thư mục: `AttendanceData.java`, `AttendanceRecordData.java`, `AttendanceKinds.java` | Snapshot typed, bản gốc nhóm3 và kind của bảng chung. |
| Cùng thư mục: `AttendanceDaySource.java`, `AttendanceCheckoutResult.java` | Nguồn ngày đã đọc và kết quả checkout kèm task IDs đã hủy. |
| `backend/src/main/java/com/eldercare/backend/care/internal/attendance/persistence/AttendanceStore.java` | Adapter SimpleRecordApi/RecordEventApi, không SQL shared trong runtime005. |
| `backend/src/main/java/com/eldercare/backend/care/internal/attendance/service/AttendanceSources.java` | Đọc ResidentApi/ContractApi/ContractReadApi/LocationStructureApi/calendar, thời gian lịch và vị trí có căn cứ. |
| Cùng thư mục: `AttendanceService.java`, `AttendanceQueries.java` | Lệnh/quyền/khóa/audit/events và provider DayCareAttendanceApi @Fallback. |
| Cùng thư mục: `AttendanceDayStartWork.java`, `AttendanceClockWork.java`, `AttendanceDayEndWork.java` | Các bước của khung job005 hiện có, CFG và marker chống trùng. |
| Cùng thư mục: `AttendanceCorrectionProvider.java` | Provider `M04.ATTENDANCE_RECORD`, xác nhận gốc/quyền/scope/nội dung và projection sau đính chính. |
| `backend/src/main/java/com/eldercare/backend/care/web/DayCareAttendanceController.java`, `DayCareAttendanceCorrectionController.java` | Bốn operation đã duyệt; guard VOID sớm, chuyển DTO/service. |
| `backend/src/main/java/com/eldercare/backend/care/web/request/DayCareCheckInWrite.java`, `DayCareCheckOutWrite.java`, `AttendanceCorrectionWrite.java` | Payload đúng trường hợp đồng. |
| `backend/src/main/java/com/eldercare/backend/care/web/response/DayCareAttendanceResponse.java`, `DayCareCheckoutResponse.java` | Projection trạng thái/giờ/loại về/buổi phát sinh và danh sách task hủy. |
| `backend/src/main/resources/db/migration/V005_94__shared_day_care_attendance.sql` | UNIQUE người/ngày, CHECK trạng thái, cột sinh/index riêng005; xử lý JSON null bằng CASE/JSON_TYPE. |
| `backend/src/test/java/com/eldercare/backend/care/FR_013_AttendanceTransitionsTest.java`, `FR_012_AttendanceSchemaIT.java`, `FR_012_AttendanceContractTest.java` | Ma trận, ràng buộc MySQL và hợp đồng/SPI. |
| Cùng thư mục: `BR_M04_02_DayCareAttendanceIT.java`, `FR_015_AttendanceCorrectionControllerTest.java` | 18 IT luồng đã chạy trước sửa YAML cuối và unit guard VOID không tạo approval. |
| `backend/src/test/java/com/eldercare/backend/support/fake/care/FakeDayCareCalendar.java` | Fixture duy nhất của calendarSPI005, mọi ngày phải đặt rõ; không productionfake. |
| `frontend/src/features/care-tasks/DayCareAttendancePage.jsx`, `.test.jsx`, `dayCareAttendanceNavigation.test.js` | Màn web/test ghi thủ công, hiển thị trạng thái, đính chính giờ, quyền và retry. |
| `mobile/lib/staff/care_tasks/day_care_attendance_gateway.dart`, `day_care_attendance_screen.dart` | Gateway nội bộ và widget trực tuyến; chưa adapterproduction. |
| `mobile/test/staff/care_tasks/day_care_attendance_screen_test.dart` | Fixture chỉ test,3test quyền/hiện diện/retry. |
| `specs/005-daily-care-tasks/phase5-contract-proposal.md` | Nội dung đã duyệt, cách ghép theo C-02 và các phần chọn chờ. |

File đã sửa:

- `care/api/DayCareAttendanceApi.java`: thêm refreshCalendarMANDATORY, giữ hàm cũ.
- `care/internal/task/persistence/CareTaskStore.java`: truy vấn task chưa đến hạn từ thời điểm về, trên bảng care_task doC sở hữu.
- `care/internal/careplan/service/CarePlanActivation.java`: thứ tự chạy trước attendance, không thay vòng đời kế hoạch.
- `care/internal/governance/persistence/CareGovernanceStore.java`: scopeattendance đọc sharedmodel; `config/CareGovernanceConfiguration.java`: metadata trường audit mới. Retentionalias cũ chưa được tuyên bố đã nối xong.
- `support/fake/care/FakeM04Config.java`: đăng ký fakecalendar opt-in. `care/T003_PublicCareApiTest.java`: count6→7 và kiểm MANDATORYrefreshCalendar theo thay đổi đã được duyệt, không xóa/bỏ các bảo đảm cũ.
- `frontend/src/App.jsx`, `frontend/src/common/layout/navigation.js`: chỉ đăng ký màn/menu005. `frontend/src/api/api005.js`: sinh thêm4operation, tổng34; không sửa tay.
- `specs/005-daily-care-tasks/{tasks,plan,data-model}.md`, `contracts/{api-005.yaml,module-interfaces.md}`, glossary000mục11.8: ánh xạ/từ ngữ/quyết định/trạng thái đúng thực tế.
- `docs/team-plan.md`: bổ sung/chỉnh các việc chờ nguồn B, adaptermobile A, retention/correction A và review shell/migration; giữ dòng chưa xong. `docs/progress.md`: chỉ thêm tiến độ. `Working-history.md`: giữ phần cũ và bổ sung toàn bộ lượt này.

Luồng hiện tại: HTTP→request→AttendanceService kiểm quyền/vai trò/trạng thái lưu trú→khóa người→khóa attendance→đọc nguồn004/calendar/vị trí→ghi SimpleRecord và RecordEvent đã xác nhận→audit/events cùng transaction. Checkout kiểm nguồn012/markUsed trong transaction nguồn, chuyển LEFT và hủy đúng taskNOT_YET_DUE sau giờ về; task đã tới hạn thực tế được giữ/chuyển theo đồng hồ, không mở lại task đóng. Callback014 đổi/khôi phục giờ theo đúngregistrationId. QueryAPI đọc projection; job chỉ xác định trạng thái từ lịch hoặc chuyển/nhắc, không lập task/điểm danh. CorrectionApi000 áp quyền/quy trìnhA/B/C trước provider cập nhật projection; bản gốc immutable. Nguồn012 thật cần kiểm giao dịch/MySQL của E; fixture bộ nhớ không là bằng chứng rollback markUsed.

### Kết quả build/test và lần xác minh chưa thực thi

| Lượt | Kết quả thực |
| --- | --- |
| `005-p5-final-scoped.log` | BUILD SUCCESS,46unit/Arch/Boundary+29IT(18flow+5schema+6T008),0failure/error/skip;Spotless2351file;exit0/01:50phút. Đây là trước hai chỉnh sửa test hợp đồng/YAML cuối. |
| `005-p5-web-final-tests.log` | 374test/69file xanh; lint/buildfinalexit0, còn cảnh báo bundle. |
| `005-p5-mobile-full-tests.log` / `005-p5-mobile-analyze.log` | 52test xanh;analyze3file mới khôngissue. |
| `005-p5-clean-verify-final.log` | Cleanverify nền đã kết thúc exit1 ở756unit:2failure,0error,0skip;IT chưa chạy. Không có kết quả gate toàn kho xanh hoặc đối chiếu baselineIT ở lượt này. |
| Lệnh `005-p5-yaml-red.log` dự kiến | Không thực thi:automaticapprovalreview hết usage limit. Không có log/testresult mới để ghi xanh. |

Hai failure clean đầu đều của lượt005: T003 đếm6hàmAPI thay vì7đãduyệt; T002 parser thấy componentrequest đínhchính chèn nhầm cấp trong /testing/fakes vì replacekhôngneođầudòng. Đã cập nhật testC T003 và thêm testparser `FR_015_correctionRequestIsARealReusableComponentNotAnOperationChild` trong FR_012_AttendanceContractTest; **test mới này chưa được Maven chạy**. Giữ T002 nguyên vẹn, không tắt test.

SửaYAML sau RED thật T002: dựng lại từ HEAD, thay đúng4path/schema mới và chèn reusable requestBody bằng regexneođầudòng ởcomponents. Kiểm tĩnh Node/yaml/assert đã qua:4path đúng,DayCareAttendanceCorrection làcomponentthật,testing/fakes giữresponses204/400;mọipath/componentcũngoài4operation thay đổi deepEqualvớiHEAD. `npm run gen:api` chạyđược;SHA256api005 trước/sau sửaYAML giốngnhau. Chỉ api005 có12dòng semanticdiff; các clientkhác khôngcócontentdiff. Không dùng kiểm tĩnh này thay Maven/Spotless/cleanverify.

Các lệnh đã thực hiện (ởbackend/frontend/mobile tương ứng):

```powershell
.\mvnw.cmd '-DspotlessFiles=.*care.*' spotless:apply
.\mvnw.cmd '-Dtest=FR_013_AttendanceTransitionsTest,FR_012_AttendanceContractTest,FR_015_AttendanceCorrectionControllerTest,*ArchTest,ModuleBoundaryTest' '-Dit.test=BR_M04_02_DayCareAttendanceIT,FR_012_AttendanceSchemaIT,T008_CareGovernanceIT' verify
.\mvnw.cmd clean verify
npm run gen:api
npm test
npm run lint
npm run build
flutter test --no-pub test/staff/care_tasks/day_care_attendance_screen_test.dart
flutter analyze --no-pub lib/staff/care_tasks/day_care_attendance_gateway.dart lib/staff/care_tasks/day_care_attendance_screen.dart test/staff/care_tasks/day_care_attendance_screen_test.dart
flutter test --no-pub
```

Các lệnh Maven dùng Java25 và DBenv từ backend/.env, không in/lưu bí mật. Sau lỗi usage limit không chạy lại Maven bằng đường khác để né kiểm duyệt. Hai filetestJava sửa cuối chưa chạy Maven/Spotless lại. Log đã có nằm trong `C:/Users/nguye/.codex/visualizations/2026/10/31/01a19373-e167-7ec3-b1f3-367d283ca782/`. Chưa thực hiện kiểm trực quan Phase5 hoặc chụp màn hình; không khẳng định đã nghiệm thu responsive/darkmode. Không commit/push/đổi nhánh/PR;HEAD vẫnfe7891ea;file handover003 ban đầu không sửa.

### Vấn đề còn tồn tại và thứ tự tiếp tục theo tasks.md

0. Khôi phục khả năng duyệt/thực thi Maven của phiên; chạy Spotless và bộ hợp đồng/Arch (gồm T002/T003/parser mới), rồi kiểm task để xác nhận đầu nguồn YAML/test đã sửa. Không đánh[x] dựa vào suy đoán.
1. **T027:** hoàn thiện nghiệm thu test với nguồn thật, sau khi các quyết định/callback còn thiếu được chốt; giữ bộ test đã có, không vô hiệu hóa ca đỏ.
2. **T028:** A/C chốt enumeration/xóa sharedattendance/record/correction theo thời hạn. RootM01 liên kết rawkhôngđủ, không SQLsharedtừ005 hoặc gọi purgecảresident để né thiếuAPI. ReviewmigrationbảngchungcủaA vẫn mở.
3. **T029:** chờ B chốt ngày đổi khỏiDAY_CARE (user đã chọn chờ); B công bố API/sự kiện/TrangThaiCoMatBanTruPort đểC viếtadapter/listener đúngchiều; VOID đã chọn chờ, generic000 cần capabilityguard củaA. AMEND_CONTENT hiện chỉsửagiờ/projection, không tựsuythêm khôi phục/hủy taskkhi chỉnhgiờvề.
4. **T030:** kiểm nguồnproduction, callbackvàchạybù sau cácgate trên; guardđổiloại hiện409cóthểrollbackbatch nênchưađủđiềukiệnnghiệmthu job.
5. **T031:** A mởtransportlistJSON rồiC càiadaptermobile/đăngkýapp;reviewApp/menu vàkiểmUIthựctế. KhôngđổiHTTPtrảmảngthànhwrapper hoặc tựsửamobilecommon trái quyếtđịnhtrước.
6. Chạy **clean verify nền mới** với nguồn cố định, đọc đầyđủunit/IT/Spotless và phân biệt gateA/B/outbox cũ với lỗi mới trướccheckbox. T032/Phase6 chưa bắtđầu.

T027–T031 vẫn `[ ]`; Phase5 là bản cài tại checkout chưa nghiệm thu. Đã ghi rõ kết quả thật, lần lệnh không chạy, quyết định user chọn chờ và vấn đề chưa giải quyết; không ghi việc chưa làm thành đã xong.

## Bổ sung quy tắc ưu tiên listener khi merge — theo yêu cầu chủ dự án

Yêu cầu đã thực hiện: đánh dấu phần tích hợp tạm cho trường hợp listener chưa có kiểu sự kiện nguồn của B; khi merge nếu B/E đã có listener tương ứng thì ưu tiên dùng bản B/E thay cho bản tạm đã làm, giữ chữ ký API cũ và bàn giao B/E review.

Đã bổ sung cùng một ghi chú có nhãn “Ghi chú khi merge — ưu tiên listener của B/E” vào `specs/005-daily-care-tasks/phase5-contract-proposal.md`, `contracts/module-interfaces.md` và `plan.md`; cập nhật đúng dòng chờ tích hợp Phase5 của B/C trong `docs/team-plan.md`. Các tài liệu này liên kết quyết định hợp đồng, hướng triển khai và việc bàn giao khi ghép nhánh. Yêu cầu hợp nhất để mỗi sự kiện chỉ xử lý một lần; phần tạm bị thay thế chỉ bỏ khi đã đối chiếu tương đương, giữ vị trí lớp/chiều phụ thuộc theo C-02. Nếu hợp đồng khác thì nêu chênh lệch để review/duyệt, không tự đổi chữ ký.

Chỉ sửa tài liệu và bổ sung nhật ký này, không thay runtime/API/test hoặc thực hiện merge, commit, push. Không khẳng định B/E hiện đã có listener. Không chạy Maven vì thay đổi chỉ là ghi chú; lần Maven/Spotless/clean verify còn thiếu của Phase5 và các gate A/B trước đó vẫn giữ nguyên, không ghi nhận đã giải quyết. Kiểm `git diff --check` sau cập nhật; kết quả được xác nhận từ lệnh kiểm trong lượt này. Việc tiếp theo vẫn theo tasks.md: kiểm lại hợp đồng T027; xử lý retention T028; ghép nguồn/callback T029 theo ưu tiên mới; kiểm job T030; nối adapter/UI T031, rồi gate cuối trước checkbox. Khi có yêu cầu merge mới áp dụng quy tắc ưu tiên B/E này.

## Phase 6 — bắt đầu triển khai ngày 2026-11-01

Đầu phiên đọc quy định, tasks.md và ghi chú plan.md; báo 18 dòng mục 8 có HungNX-nguyen. HEAD đầu phiên 44a7c201 (Phase 5); chỉ có Working-history.md và specs/003-room-bed-allocation/hand-over-task-003.md untracked. Giữ nguyên lịch sử cũ và file bàn giao 003. Không đổi nhánh/commit/push/merge, không chụp màn hình.

Đã tạo specs/005-daily-care-tasks/phase6-contract-proposal.md với bản dịch sáu operation T039 và trường request/response, giữ HTTP/required/header. Chủ dự án trả lời “1” duyệt; đã cập nhật trạng thái đề nghị. Chưa coi các gate A/B/D/E từ Phase 5 là đã giải quyết.

T032: tạo backend/src/test/java/com/eldercare/backend/care/FR_035_CareResultTimingTest.java trước implementation, sáu ca kiểm thời điểm trong khung, hai biên, một microsecond sau cuối khung, khung dài bằng 0, trước đầu khung và khung đảo. Maven RED exit1: 46 test gồm 2 failure và 4 error do thiếu lớp. Sau đó tạo backend/src/main/java/com/eldercare/backend/care/internal/result/domain/CareResultTiming.java, hàm completedStatus chỉ phụ thuộc khung và performedAt. Trả COMPLETED khi không sau cuối khung, COMPLETED_LATE khi sau; từ chối trước đầu/khung đảo. Service T036 sẽ kiểm task còn mở/quyền/thời điểm ghi; hàm thuần không đọc database hoặc đồng hồ.

Lệnh xác minh (backend, JDK25): .\mvnw.cmd '-Dtest=FR_035_CareResultTimingTest,*ArchTest' test; sau cài chạy .\mvnw.cmd '-DspotlessFiles=.*care.*' spotless:apply rồi cùng test. GREEN 46/46, 0 failure/error/skip, exit0. Log tại C:/Users/nguye/.codex/visualizations/2026/10/31/01a19373-e167-7ec3-b1f3-367d283ca782/005-p6-t032-red.log, 005-p6-t032-green.log và 005-p6-spotless.log. Không dùng kết quả này thay cho clean verify toàn phase.

Đã sửa tasks.md ghi đường dẫn thực tế T032 và trạng thái tiếp nhận; plan.md ghi quyết định kỹ thuật, bằng chứng và gate. Rà data-model/research C-09/C-10/C-14: kết quả gộp care_task theo R-15; id kết quả bằng task, không tạo bảng riêng. Phát hiện snapshot CareTaskData còn thiếu upperLimit/vitalSignType; T036 phải giữ cấu hình lúc lập task theo FR-038.

Đang tạo test T033: backend/src/test/java/com/eldercare/backend/care/FR_034_CareResultRecordingIT.java, kiểm HTTP thật/MySQL, giá trị ML, xác nhận vượt giới hạn, thời điểm, ghi lần hai, lý do Không thực hiện, PERFORMED/ITEM_QUANTITY và sự kiện tính phí. Chưa có kết quả RED/GREEN được xác nhận cho bộ này. Lần gọi đầu nạp ../.env sai đường dẫn (file thật backend/.env); lỗi môi trường này không phải RED nghiệp vụ. Không ghi giá trị biến môi trường vào lịch sử.

T032–T041 còn [ ]; chưa chạy clean verify cuối Phase 6. Thứ tự tiếp theo: T033 hoàn thiện/quan sát RED; T034 test ngoại tuyến; T035 schema R-15; T036 service; T037 handler; T038 checklist/claim; T039 HTTP/API; T040 mobile; T041 web; cuối cùng clean verify nền và chỉ đánh dấu sau kết quả thực tế.

### T033/T035 và bản dịch T039 — checkpoint có kiểm chứng

T033 RED thật ở 005-p6-t033-red2.log: 14 failure/0 error, đều 404 thiếu endpoint. Bổ sung tám ca về EATING/MOVEMENT/SLEEP/MOOD/BEHAVIOR và ba phản hồi của VitalSignPort; 005-p6-t033-types-red.log: 22 failure/0 error do 404. Sau implementation, lượt green đầu dừng testCompile vì fixture Phase5 còn constructor CareTaskData cũ; đã sửa đúng hai đối số snapshot tùy chọn của fixture, không đổi assertion. Lượt green2 có 18/22 qua, ba lỗi fake đo do thời điểm mặc định thay đổi giữa đặt fixture và HTTP, một failure do dùng id vật lý danh mục thay public id legacy. Sửa fixture với performedAt cố định và public id theo R-15. Lượt 005-p6-t033-green3.log exit0: 40 ArchTest và 22 IT ghi nhận đều xanh, 0 failure/error/skip. Spotless care đã chạy qua; đây chưa phải toàn bộ T033/T036 hoặc toàn Phase6 hoàn thành.

T035: tạo FR_036_CareResultSchemaIT.java trước migration. RED có 1 failure/2 error do thiếu cột; ba ca âm ban đầu qua vì unknown column nên chưa chứng minh CHECK. Tạo V005_98__care_task_results.sql: cột kết quả số/mã/JSON, thời điểm thực hiện/ghi/thiết bị, người ghi, nhãn muộn/mất quyền, căn cứ quyền, id chỉ số/bữa, lý do không thực hiện, người/loại đóng, cờ và payload chờ đo lại; index ix_care_task_result(resident_id,result_type,result_performed_at), CHECK kết quả chỉ trên task hoàn thành và performedAt không sau device/recorded, CHECK trạng thái chờ đo lại; trigger DELETE chặn xóa bản gốc ngoài tài khoản retention. Trigger final_update cũ đã chặn UPDATE task đóng nên không sửa migration đã có. Không tạo care_result riêng. Lượt 005-p6-schema-contract-green2.log có 6/6 IT schema xanh, 41 unit/Arch (1 contract + 40 Arch) xanh, exit0. Cột mã VARCHAR(100) giữ độ dài danh mục ResultValueAttributes đã công bố, không cắt mã còn 40 ký tự.

T039 contract: tạo FR_034_CareRecordingContractTest.java, RED 1 failure trước đổi YAML. Áp dụng đúng sáu operation đã duyệt và schema CareResultWrite/Checklist; dùng CareTask/CareTaskDetail/CareTaskResult đã có, bổ sung response CareTaskClosed riêng, giữ schema tiếng Việt ở story chưa dịch. Script kiểm tĩnh bằng yaml/assert chứng minh mọi path, schema và requestBodies ngoài phạm vi giữ nguyên. Lượt đầu parser bắt path history bị dịch placeholder trước tên path nên chưa đổi hết; đã sửa thành /residents/{residentId}/care-results. Lượt green2 nêu trên chứng minh parser qua; không nới assertion hoặc sửa endpoint ngoài đề nghị.

Danh sách mã được tạo: internal/result/domain/CareResultTiming (luật thuần), CareResultInput (nội dung ghi, values theo loại), CareResult (snapshot gốc), PendingCareResult (giữ nội dung và metadata khi chờ 007); internal/result/persistence/CareResultStore (SQL chỉ care_task, đóng và ghi kết quả trong một UPDATE có version, lưu pending, truy vấn bản gốc/lịch sử); internal/result/service/CareResultValidation (giá trị theo loại/danh mục), CareResultService (khóa người/task, quyền, thời điểm, nguồn chỉ số/bữa, lý do, audit qua CareTaskService.record và outbox), CareResultQueries (quyền đọc qua service). Tạo web/request/CareResultWrite, CareNotPerformedWrite; web/response/CareResultResponse; web/CareResultController cho record/not-perform/history. Các đường dẫn này đều dưới backend/src/main/java/com/eldercare/backend/care/.

Sửa CareTaskData thêm upperLimit/vitalSignType; CareTaskValidator chụp hai giá trị khi lập task. Sửa CareTaskController lấy result qua CareResultQueries và ánh xạ theo quyền HEALTH; không import persistence vào web. Sửa BR_M04_02_DayCareAttendanceIT chỉ cập nhật constructor snapshot cho fixture cũ. Spotless cũng định dạng hai test005 FR_012_AttendanceContractTest và T003_PublicCareApiTest còn thay đổi từ Phase5; không đổi kỳ vọng nghiệp vụ của chúng.

Luồng hiện tại: HTTP CareResultWrite → CareResultService → khóa CareLockStore và care_task → CareResultValidation → VitalSignPort hoặc TaskSourcePort khi cần → CareResultStore cập nhật đồng thời trạng thái/kết quả → CareTaskService.record ghi history/audit → ApplicationEventPublisher phát AlertClosureRequested, BillableCareResultRecorded, SleepResultRecorded trong transaction → controller đọc chi tiết/result qua service, che HEALTH. BLOCKED trả 422 không đóng; AWAITING_REMEASUREMENT trả 202, giữ task mở và pending_result; callback xác nhận chưa cài ở checkpoint này. Không thực hiện yêu cầu reasonTypeId ACTIVE, note tùy chọn, không tạo kết quả gốc.

Chủ dự án đã chọn chờ thống nhất E/012 cho công thức summary. Đã thêm dòng mục8 team-plan, ghi plan/proposal; chưa cài công thức hoặc trả Optional.empty giả. Đang đề nghị riêng mục4 proposal về content của hai phong bì ngoại tuyến T037 vì hợp đồng /sync chỉ mô tả object tùy loại; chưa áp dụng payload chưa duyệt. Các việc vẫn còn theo tasks.md: hoàn thiện T033 (quyền, phục vụ bữa, đồng thời, phát sinh), T034 ngoại tuyến, hoàn thiện T036 callback/audit/phát sinh, T037 handler, T038 checklist/claim, hoàn thiện T039 API, T040/T041 UI, kiểm cuối nền. Chưa đánh [x], chưa clean verify cuối phase.

### Hoàn thiện trực tuyến và tiếp nhận các quyết định hoãn

Chủ dự án chọn chờ A thống nhất payload ngoại tuyến sau khi không duyệt mục4 đề nghị của C; không tạo OfflineHandler, không thay /sync hoặc app enqueue theo payload chưa duyệt. Chủ dự án cũng chọn chờ D/007 cho nhánh Ghi nhận phát sinh đo chỉ số do xung đột Hoàn thành ngay/Chờ đo lại và response202 chưa có trong hợp đồng. Đã thêm hai dòng mục8 team-plan; ghi đề nghị và gate tại plan/proposal. Công thức summary vẫn chờ E theo quyết định trước.

Tạo CareResultTestBase.java để dùng chung fixture nguồn có sẵn cho các bộ IT005; tách phần fixture khỏi FR_034_CareResultRecordingIT, giữ 22 assertion nghiệp vụ. Tạo FR_025_CareResultCompletionIT.java (9 ca) trước bổ sung phần phát sinh/callback/audit: RED 5 failure +1 error (thiếu route, bean callback, audit kết quả và quyền lead bị phân loại theo ROLE). Cài AdHocCareRecordingService.java, AdHocCareRecording.java, AdHocCareRecordingWrite.java; nối POST /ad-hoc-care-recordings trong CareResultController. Lệnh kiểm người có mặt/lịch sử vắng, bán trú theo điểm danh, nguồn ca đã công bố, tên trong ca, vai trò/chứng chỉ; 200 chỉ hiển thị công việc cùng loại khi chưa xác nhận đã xem, 201 mới ghi. Task DUE chỉ là bước kỹ thuật chưa commit cho INSERT rồi UPDATE kết quả; không ghi history/outbox trung gian, giao dịch chỉ công bố Hoàn thành với window null. Nhánh MEASURED_VALUE phát sinh trả503 trước tạo theo gate đã chốt.

Sửa CareResultService kiểm nhiệm vụ thực tế qua DutyApi khi người phụ trách ca còn có quyền ROLE thường; không suy thiếu nhiệm vụ từ PermissionResult.basis ưu tiên ROLE. Thêm audit riêng M04.CARE_RESULT gồm giá trị thực và metadata, cùng transaction với task. Tạo CareVitalSignTasks.java cài API MANDATORY hiện có, đóng pending cùng id bản ghi chỉ số; không chép giá trị đo đầu thành giá trị đo đã xác nhận (numericValue bỏ trống, tham chiếu nguồn007). Callback khác thời điểm/người đo gốc hiện503 vì cần thống nhất metadata nguồn, chưa coi nhánh này đầy đủ. 005-p6-completion-green.log exit0: 9/9 IT và 40 ArchTest xanh. Test audit còn xác minh data_after.values.numericValue=200; không chỉ kiểm có dòng.

T038: tạo FR_030_CareChecklistIT.java trước mã, RED 7 failure do404. Tạo internal/checklist/domain/CareChecklist.java, ChecklistRiskAgreement.java; persistence/CareChecklistStore.java; service/CareChecklistService.java; web/CareChecklistController.java, response/CareChecklistResponse.java. CareChecklistStore đọc toàn bộ header thỏa thuận của các người trong một truy vấn; response chứa đúng trường RiskAgreement bắt buộc, không trả danh sách người tham gia/ý kiến giả rỗng. CareChecklistService đọc task theo ca/nhân viên/việc chung, lọc quyền từng dòng, vai trò/chứng chỉ hiện hành theo danh mục, nguồn vị trí và lưu ý gọi theo lô. Lấy người trong ScopeApi.filter cùng phân công008 để không bỏ liều thuốc/cảnh báo của người chưa có task chăm sóc. Điều dưỡng đọc MedicationChecklistPort/AlertReadPort/handoverTasks; thiếu provider thật báo503. Claim khóa người/task, chỉ người có tên trong ca, vai trò và chứng chỉ hiện hành; không lấy chứng chỉ snapshot cũ khi nhận việc. Audit/history nhận việc ghi người nhận, không tự tạo task.

Tạo V005_99__care_checklist_carryover.sql thêm carryover/index cho đọc việc tồn; producer dấu thuộc Phase7/lệnh bàn giao chưa được làm sớm. Sửa CareTask.java thêm awaitingRemeasurement/carryover, CareTaskStore row/query/claim, constructor CareTaskValidator/AdHocCareRecordingService, CareTaskResponse hiển thị hai trường thực tế. ETag checklist là SHA256 trên response đã lọc quyền; không dùng thời gian cố định làm version. 005-p6-checklist-green.log: 7/7 IT +40 Arch xanh, exit0. Fixture chứng chỉ kiểm danh mục hiện hành và phân công một đối tượng cư dân đúng AssignmentView.

T039 PublicCareResultApi: tạo FR_049_PublicCareResultsTest.java trước provider, RED4 ClassNotFound. Tạo PublicCareResults.java: đọc các kết quả, overlay CorrectionApi.current theo lô, bỏ bản đã VOID, chỉ trả numericValue/codeValue/jsonValue, không ghi chú nội bộ hoặc cờ xác nhận. 012 vẫn tự kiểm bản đồng ý/quyền và ghi nhật ký xem theo hợp đồng. summary ném503 rõ gate công thức chờ E. 005-p6-public-api-green.log: 44/44 (4 test +40 Arch), exit0, Spotless care qua.

Đọc README web/mobile, DESIGN.md và skill UI để chuẩn bị màn. Giữ công thức dòng thời gian/TaskBlock/DetailLayout đã duyệt; không tự thêm layout hay màu mới. Phát hiện CareTask chỉ có tên loại, chưa có resultType snapshot cho form; lập mục5 đề nghị thêm duy nhất resultType chỉ đọc và hỏi duyệt, chưa sửa schema/response trường này khi chưa nhận duyệt. Không ghép tên loại với danh mục hiện tại để đoán kiểu form (FR038). Chưa đổi UI hoặc chụp screenshot tại checkpoint này.

Bổ sung kiểm thực: FR_036_CareResultConcurrencyIT (hai request hai transaction độc lập, một200/một409, chỉ một kết quả); fixture commit có dọn task/audit/history của đúng id bằng retentionConnection test. Ca đồng thời xanh ngay với khóa hiện có; không nhận đã thấy RED cho riêng ca này. FR_035_CareResultRegressionIT bổ sung số nguyên có đuôi .000, trễ việc Quan trọng cần lý do, thực hiện trong khung đóng Quá hạn thành Hoàn thành, chưa xác nhận phục vụ409. FR_036_CareResultGovernanceIT kiểm phạm vi và graph hai định danh logic cùng dòng R15. 005-p6-result-regressions-red.log: regression4 ca có1failure (200.000 bị422), governance2 ca có1failure+1error (care_result cũ chưa map); concurrency1 xanh.

Sau RED đã sửa kiểm scale bằng stripTrailingZeros, map scope M04.CARE_RESULT vào care_task có result_performed_at, CareRetentionType CARE_RESULT/LOCATION_CARE_RESULT trỏ care_task. Graph TASK↔CARE_RESULT (cùng id) buộc kiểm attachment của cả hai trước xóa; vòng được ExpiredDataPurge.seen xử lý sẵn. Result delete là no-op, TASK mới xóa vật lý; root bắt đầu ở định danh nào cũng kéo đủ chuỗi. Không sửa common hoặc nguồn A. Đang chạy 005-p6-result-regressions-green.log, chưa ghi kết quả khi chưa đọc xong. Các gate điểm danh/shared của Phase5 chưa được sửa theo thay đổi này.

### Tiếp tục Phase 6 ngày 01/11/2026 — kiểm thực bổ sung và giao diện

Đã đọc log kết thúc `005-p6-result-regressions-green.log`: các kiểm regression/retention/concurrency đã xanh (7 IT cùng 40 ArchTest). Bổ sung RED thực cho ràng buộc không lưu payload kết quả dở dang, việc tồn trước đầu khung, timezone retention và lý do công khai/khóa ngoại vật lý; sau sửa, `005-p6-integrity-green-actions-red.log` có Schema7, Governance3, Regression5 xanh, còn hai ca mới thiếu allowedCommands RED. Sửa CareTaskController/CareChecklistService/CareResultService cung cấp quyền record/notPerform/claim theo đúng kiểm backend; `005-p6-actions-green.log` kết thúc BUILD SUCCESS, 10 ChecklistIT cùng 40 ArchTest xanh. V005_100 thêm FK lý do vào catalog_item; service lưu catalogId vật lý và audit id công khai đã chọn. V005_98 siết CHECK payload; CareGovernanceStore dùng BusinessTime.ZONE cho planned_at.

Web: tạo CareChecklistPage.jsx, CareChecklistDetail.jsx, careChecklistView.js và CareChecklistPage.test.jsx; App.jsx chuyển tuyến care-tasks sang trang API thật. Dùng TaskBlock/DetailLayout/ChoiceNote/CareNote hiện có, sắp Quá hạn trước, lọc việc, đọc chi tiết mới, nhận việc, Không thực hiện chọn danh mục và giữ ghi chú/idempotency key khi lỗi. `005-p6-web-green2.log`: 3/3 xanh sau RED và sửa label trạng thái không quyền; `005-p6-web-lint.log` và `005-p6-web-build.log` qua, build có cảnh báo bundle lớn. Chưa kiểm trực quan bằng trình duyệt tại checkpoint này.

Mobile: tạo care_checklist_cache.dart/.g.dart, care_checklist_gateway.dart, care_checklist_repository.dart và care_checklist_cache_test.dart trong feature staff/care_tasks. Luồng gateway ApiClient -> repository -> cache Drift riêng theo accountId/shiftId; chỉ dùng cache khi ApiUnreachable, 403/404 xóa snapshot, không phục hồi nội dung đã mất quyền. Không ghi phong bì offline chưa duyệt. Test RED thiếu component; lần đầu sau sinh mã RED do trùng tên CareChecklistSnapshot với model Drift. Đổi DTO thành CachedCareChecklist, giữ model sinh tự động; `005-p6-mobile-cache-green2.log`: 4/4 xanh. Build_runner đã vô tình loại file sinh common/offline/offline_database.g.dart khi chạy build-filter; đã phục hồi nguyên nội dung HEAD bằng git show, không giữ thay đổi common. Không sửa pubspec hoặc thêm thư viện.

Chủ dự án duyệt mục5 proposal: CareTask.resultType chỉ đọc từ snapshot, không đổi request/API/SPI. Test schema mới đã RED thực với field null (42 test,1failure); hai lượt trước lỗi biên dịch test do Schema raw/generic đã được sửa trước khi ghi nhận RED nghiệp vụ. Đã thêm resultType vào CareTaskResponse/YAML, cập nhật proposal đã duyệt và chạy gen:api. Đang kiểm GREEN schema/HTTP cùng ArchTest, chưa kết luận khi chưa đọc log cuối.

Form web: tạo CareResultForm.jsx/CareChecklistRecording.test.jsx, sửa CareChecklistDetail để gửi record và phân biệt 202 Chờ đo lại. Bảy test đầu RED vì chưa có form, sau cài 7/7 xanh cùng 3 test checklist (`005-p6-web-record-green.log`:10/10). Các ô nhập dựa snapshot, không ghép tên danh mục; giữ input khi lỗi, chỉ hỏi xác nhận vượt giới hạn sau CONFIRMATION_REQUIRED. Thêm hai ca danh mục ngủ/tâm trạng RED vì chưa lọc đúng loại và gửi query không có trong hợp đồng; đã sửa dùng GET /result-values không query và lọc type/status tại client, đang kiểm tiếp.

Chủ dự án chọn chờ E/010 cho quantity của BillableCareResultRecorded, chọn tiếp tục chờ D/007 cho callback khác thời điểm/người đo. Quyết định kỹ thuật: chặn lệnh billable trước lưu thay vì công bố thành công với số lượng tự suy đoán hoặc bỏ sự kiện; đang viết/kiểm test trước thay đổi này. Không tự chuyển ml/phút/giá trị đo thành số lượng dịch vụ.

Chưa chạy clean verify cuối Phase6, chưa đánh [x]. Task kế tiếp theo thứ tự tasks.md: hoàn thiện kiểm T033/T036 theo gate tính phí mới, phần T034/T037 vẫn chờ A; tiếp tục T038/T039 kiểm tích hợp, T040 mobile và T041 web, cập nhật tài liệu, review, kiểm nền cuối. Các gate summary E/012, ad-hoc đo chỉ số/callback D/007, nguồn thực và transport A từ phase trước vẫn còn.

### Kết quả tiếp tục — snapshot, chờ đo lại, web/mobile

`005-p6-billing-gate-red2.log`: schema/40 Arch xanh, Checklist10/10 xanh (xác minh HTTP resultType=ML), Recording22 còn đúng1failure503 kỳ vọng/200 thực. `005-p6-input-null-red.log`:1error NullPointerException. `005-p6-record-safety-red.log`:25IT có3failure đúng nhánh billable, legacy ML thiếu cấu hình, notPerform khi đang pending. Sau sửa, `005-p6-record-safety-green.log` BUILD SUCCESS:35IT (25 Recording+10 Checklist) cùng43 unit/kiến trúc xanh. Không tính lượt chồng Maven lỗi FakeResidents$Entry là lỗi mã nghiệp vụ; đã chạy lại tuần tự.

Sửa CareResultInput dùng unmodifiable LinkedHashMap: JSON null giữ được để validation xử lý, map được copy và không sửa được. CareTaskData thêm Boolean resultConfigurationCaptured nội bộ; CareTaskValidator/AdHocCareRecordingService đặt true khi lập mới. Cập nhật constructor fixture BR_M04_02_DayCareAttendanceIT, không đổi kỳ vọng nghiệp vụ. ML cũ thiếu upperLimit và dấu đã chụp trả503, cấu hình mới ghi rõ không giới hạn vẫn được ghi. CareResultService chặn billable và chặn notPerform khi đang chờ xác nhận đo lại. Loại bỏ công thức quantity tự suy đoán, callback billable cũng chặn theo quyết định chờ E/010.

Tạo FR_049_CareResultHistoryScopeTest: không có from/to đã RED vì BedAllocationApi.openAt nhận null. Sửa CareResultController inject BusinessTime, dùng now khi cả hai biên trống, không thay chữ ký HTTP. Test GREEN đang nằm trong lượt scoped tổng hợp, chưa ghi kết quả trước khi đọc log.

Mobile: tạo CareChecklistScreen (accountId/shiftId được shell cấp) và care_checklist_screen_test.dart; RED thiếu màn, GREEN7/7 gồm4cache+3widget. Hiện Quá hạn trước, lựa chọn trên thẻ, lưu ý chăm sóc, nhãn Chưa cập nhật khi đọc cache; mất mạng không gửi lệnh chưa có payload được duyệt. Form Không thực hiện yêu cầu chọn loại lý do, ghi chú tùy chọn, giữ nội dung khi lỗi. Thêm test retry RED vì key không phải UUID; dùng Random.secure tạo UUIDv4, giữ key cho cùng nội dung. Thêm test record ML RED thiếu nút; tạo mobile CareResultForm và mở từ checklist, CareChecklistGateway.record/ApiCareChecklistGateway.record dùng ApiClient hiện có. Form có ML/ITEM_QUANTITY/MEASURED_VALUE/EATING/MOVEMENT/PERFORMED, xác nhận vượt giới hạn theo lỗi server, ghi chú/lý do. Không báo hoàn thành chỉ từ HTTP: gửi xong tải lại trạng thái checklist (202 của client common không có status riêng). `005-p6-mobile-record-green.log`:9/9 xanh. SLEEP/MOOD/BEHAVIOR còn thiếu nguồn danh mục mảng, nhập giờ thực hiện/phát sinh/sync/bản bị từ chối chưa đầy đủ; không coi T040 hoàn thành.

Tạo FR_034_CareResultInputTest.java/FR_049_CareResultHistoryScopeTest.java cho hai lỗi nêu trên. Tạo frontend CareChecklistRecording.test.jsx/CareResultForm.jsx; 7ca đầu RED->GREEN, bổ sung2ca danh mục RED->GREEN; cùng3ca checklist=12/12 (`005-p6-web-catalog-green.log`). `005-p6-web-lint2.log` qua. Đã cập nhật plan/tasks/data-model/module-interfaces/glossary000 mục11.9, proposal mục5 đã duyệt/mục6 quyết định chờ, thêm ba dòng team-plan chờ E010, D007 callback, A review shell/cache/FK/client.

Đã thử kiểm trực quan bằng skill computer-use với fixture chỉ dùng kiểm thử, không có tuyến production. Tạo tạm frontend/.phase6-preview.html và src/test/phase6-preview.jsx, chạy Vite127.0.0.1:5175; công cụ Windows tự dừng vì không xác định chắc URL trình duyệt. Không chụp được màn ứng dụng để nghiệm thu, không tiếp tục nhập UI; đã xóa đúng hai file tạm và dừng Vite. Không ghi nhận kiểm trực quan đạt. Đang chạy scoped backend tổng hợp, full web/mobile test và analyzer; reviewer độc lập chỉ đọc thay đổi Phase6, chưa clean verify cuối.

### Danh mục file Phase 6 và vai trò (bổ sung theo thay đổi thực tế)

Các đường dẫn backend bên dưới tính từ `backend/src/main/java/com/eldercare/backend/care/`:

- `internal/result/domain/CareResultTiming.java`: luật thời điểm đóng; `CareResultInput.java`: nội dung lệnh bất biến; `CareResult.java`: projection bản ghi; `PendingCareResult.java`: metadata lần đo chờ; `AdHocCareRecording.java`: kết quả nhánh phát sinh hoặc danh sách việc cùng loại.
- `internal/result/persistence/CareResultStore.java`: đọc/ghi kết quả trên dòng care_task, version và pending; `internal/result/service/CareResultValidation.java`: hình dạng/giới hạn/mã theo loại.
- `internal/result/service/CareResultService.java`: khóa, quyền, kiểm thời gian/nguồn, ghi trạng thái/kết quả, history/audit/outbox; `AdHocCareRecordingService.java`: kiểm hiện diện/ca/chứng chỉ và tạo bản phát sinh theo lệnh người dùng.
- `internal/result/service/CareResultQueries.java`: truy vấn qua quyền checklist; `PublicCareResults.java`: projection cho012/correction overlay; `CareVitalSignTasks.java`: callback xác nhận nguồn007 theo API đã có.
- `internal/checklist/domain/CareChecklist.java`: aggregate nguồn đọc; `ChecklistRiskAgreement.java`: header thỏa thuận; `internal/checklist/persistence/CareChecklistStore.java`: truy vấn header theo lô; `internal/checklist/service/CareChecklistService.java`: lọc checklist và nhận việc.
- `web/CareResultController.java`: bốn operation kết quả/phát sinh/lịch sử; `web/CareChecklistController.java`: GET checklist và claim, ETag và lọc trường.
- `web/request/CareResultWrite.java`, `CareNotPerformedWrite.java`, `AdHocCareRecordingWrite.java`: ánh xạ đúng body đã dịch/duyệt; `web/response/CareResultResponse.java`, `CareChecklistResponse.java`: kết quả và nguồn checklist theo hợp đồng.
- Sửa `internal/task/domain/CareTask.java`, `CareTaskData.java`: metadata đang chờ/việc tồn/snapshot kết quả; `internal/task/persistence/CareTaskStore.java`: row/query checklist/claim; `internal/task/service/CareTaskValidator.java`: chụp cấu hình khi lập.
- Sửa `web/CareTaskController.java` và `web/response/CareTaskResponse.java`: kết quả thật/allowedCommands/resultType snapshot và metadata checklist; sửa `internal/governance/domain/CareRetentionType.java`, `internal/governance/persistence/CareGovernanceStore.java`: scope/graph/xóa đúng R15 và timezone.

Migration thực ở `backend/src/main/resources/db/migration/`: `V005_98__care_task_results.sql` (cột/ràng buộc kết quả), `V005_99__care_checklist_carryover.sql` (dấu việc tồn), `V005_100__care_result_reason_reference.sql` (FK lý do vật lý). Không có migration cũ bị sửa hoặc file sản phẩm bị xóa.

Test thực ở `backend/src/test/java/com/eldercare/backend/care/`:

- `CareResultTestBase.java`: fixture chung có nguồn giả do chủ SPI cung cấp; `FR_035_CareResultTimingTest.java`: biên thời điểm; `FR_034_CareRecordingContractTest.java`: sáu operation/field và readOnly snapshot.
- `FR_036_CareResultSchemaIT.java`: CHECK/trigger/migration; `FR_034_CareResultRecordingIT.java`: các loại/quyền/nguồn/pending/lý do/snapshot/gate tính phí; `FR_025_CareResultCompletionIT.java`: phát sinh, duty, audit, callback.
- `FR_030_CareChecklistIT.java`: checklist/thỏa thuận/claim/chứng chỉ/quyền/việc tồn/allowedCommands; `FR_036_CareResultConcurrencyIT.java`: hai transaction cạnh tranh; `FR_035_CareResultRegressionIT.java`: số nguyên đuôi thập phân, giờ/lý do/bữa/FK audit.
- `FR_036_CareResultGovernanceIT.java`: scope/graph/timezone; `FR_049_PublicCareResultsTest.java`: correction overlay/VOID/projection; `FR_034_CareResultInputTest.java`: JSON null/copy/bất biến; `FR_049_CareResultHistoryScopeTest.java`: truy vấn lịch sử không gửi null sang nguồn vị trí.
- Sửa fixture constructor `BR_M04_02_DayCareAttendanceIT.java`; định dạng/cập nhật test hợp đồng Phase5 `FR_012_AttendanceContractTest.java`, `T003_PublicCareApiTest.java` cho thay đổi đã được duyệt trước; không sửa test nguồn người khác để hợp mã mình.

Web `frontend/src/features/care-tasks/`: `CareChecklistPage.jsx` đọc/lọc/chọn/refresh checklist, `careChecklistView.js` ánh xạ response sang thẻ, `CareChecklistDetail.jsx` lệnh có idempotency/error, `CareResultForm.jsx` form theo snapshot và danh mục. `CareChecklistPage.test.jsx` kiểm checklist/quyền/Không thực hiện; `CareChecklistRecording.test.jsx` kiểm body theo loại/422/202/danh mục. `frontend/src/App.jsx` đổi đăng ký tuyến; `frontend/src/api/api005.js` chỉ sinh bằng gen:api từ YAML.

Mobile `mobile/lib/staff/care_tasks/`: `care_checklist_cache.dart`/`.g.dart` là DB cache riêng feature và mã Drift sinh; `care_checklist_gateway.dart` gọi ApiClient có sẵn; `care_checklist_repository.dart` quản lý cache/offline/403; `care_checklist_screen.dart` render nguồn/các lệnh online; `care_result_form.dart` nhập kết quả và giữ retry. Test tương ứng `mobile/test/staff/care_tasks/care_checklist_cache_test.dart`, `care_checklist_screen_test.dart`. Không giữ sửa common, không thêm package.

Tài liệu: `contracts/api-005.yaml` là bản dịch/field đã duyệt; `phase6-contract-proposal.md` lưu đề nghị và quyết định; `contracts/module-interfaces.md` ghi provider/gate; `data-model.md` ghi ánh xạ vật lý; `plan.md` ghi lựa chọn kỹ thuật; `tasks.md` ghi phạm vi và còn dở, checkbox chưa đánh; `specs/000-shared-foundation/glossary.md` bổ sung mục11.9; `docs/team-plan.md` ghi dependency/review; `Working-history.md` nối tiếp lịch sử và bằng chứng. `docs/progress.md` sẽ chỉ bổ sung kết quả kiểm cuối khi có bằng chứng.

Kiểm tổng thể trước review: `005-p6-web-all-tests.log`71 file/386test xanh; `005-p6-web-build2.log` build qua (cảnh báo bundle lớn); `005-p6-mobile-all-tests.log`61test xanh; `005-p6-mobile-analyze3.log` No issues found. Review cuối và scoped backend còn đang chờ tại checkpoint này.

### Review độc lập và quyết định xử lý (01/11/2026)

Reviewer chỉ đọc, không sửa/chạy DB. Chỉ ra sáu vấn đề: P1 target quyền history phụ thuộc from/to; P1 mobile giữ dữ liệu tài khoản/ca cũ và nhận response cũ; P2 UI bỏ thuốc/cảnh báo/bàn giao; P2 mở detail mất roomBed/careNotes; P2 retry web ca cũ ghi đè ca mới; P2 claim việc tồn dùng ca gốc. Không có minor riêng; các gate A/D/E, correction Phase8 và graph module khác được reviewer để ngoài kết luận và được giữ là chưa nghiệm thu.

Đã tái hiện RED: `005-p6-review-scope-red.log` history có from/to gọi vị trí quá khứ thay vì now; `005-p6-review-web-red.log`3ca mới đỏ,3ca cũ xanh; `005-p6-review-mobile-red.log`2ca mới đỏ,5ca cũ xanh. Sửa target quyền lịch sử luôn lấy vị trí hiện tại bằng BusinessTime.now, from/to chỉ lọc bản ghi; mobile xử lý didUpdateWidget, xóa state khi đổi account/shift/repository, dùng generation bỏ response cũ. Web effect/retry/refresh dùng generation chung; detail giữ projection roomBed/careNotes được checklist cung cấp, nhưng xóa careNotes nếu detail che content (không phục hồi trường sức khỏe bị che). Tạo `frontend/src/features/care-tasks/CareChecklistSources.jsx` hiển thị dữ liệu điều dưỡng riêng, không biến liều thuốc thành task hoặc nút ghi thuốc của M04. Mobile hiển thị phần nguồn tương ứng. `005-p6-review-web-green.log`:15/15 xanh; `005-p6-review-mobile-green.log`:11/11 xanh.

Claim việc tồn: đã hỏi vì research C10 yêu cầu ca trùng giờ dự kiến nhưng Q93 có người ca sau nhận; chủ dự án chọn2, để nhánh này chờ E/008 ở Phase7. Không tự áp luật lấy ca hiện tại. Đang bổ sung test trước để checklist không đưa allowedCommand claim mà service sẽ từ chối, dùng cùng canClaim/canRecord như detail. Không coi việc hiển thị tồn là đã hoàn tất nhận việc tồn.

Lượt scoped tổng hợp trước review (`005-p6-scoped-final.log`) dừng ở unit:60test có1failure do test Phase5 của C so thứ tự schema.required [type,reason] trong khi parser trả [reason,type]. Hai phần tử đều đúng, thứ tự required không mang nghĩa trong OpenAPI. Đã sửa containsExactlyInAnyOrder, không đổi YAML/hợp đồng hoặc nới số trường. Không ghi nhận lượt này đã chạy các IT. Tiếp tục kiểm lại toàn bộ sau sửa review, không mở thêm lượt reviewer.

### Chốt sửa review trước kiểm nền

`005-p6-review-commands-red.log`:11ChecklistIT có1failure do vẫn đưa claim khi service từ chối; unit scope/Arch ở lượt này xanh. Đã sửa CareChecklistController gọi cùng CareResultService.canRecord và CareChecklistService.canClaim như detail, quyền vẫn lọc qua PermissionApi.allowedCommands. Nhánh nhận việc tồn ca sau vẫn chờ E/008 theo lựa chọn2 của chủ dự án; đã thêm dòng team-plan/plan, không đổi quy tắc ca gốc.

Bổ sung test runtime OpenAPI `FR_038_runtimeSnapshotResultTypeHasTheApprovedEnum`: RED vì Java String không sinh enum dù YAML có enum đã duyệt. CareTaskResponse dùng ResultType hiện có và Schema.READ_ONLY, không thêm trường hoặc đổi API/SPI; YAML/request giữ nguyên. Đang kiểm GREEN trong lượt scoped-reviewed. FR_012_AttendanceContractTest kiểm required không phụ thuộc thứ tự, không sửa hợp đồng.

Web15/15 test riêng sau review xanh (`005-p6-web-review-final.log`), lint sạch (`005-p6-web-lint-clean.log`); full test lượt sau sửa có388/389 xanh, một test StaffPage của008 timeout5000ms khi chạy đồng thời nhiều tiến trình. Chạy riêng StaffPage nguyên trạng6/6 xanh (`005-p6-staff-page-recheck.log`); đang kiểm toàn bộ lại với --maxWorkers=2, không tăng timeout hoặc sửa test của008. Mobile cuối63/63 xanh và analyzer care_tasks No issues found (`005-p6-mobile-all-tests-final.log`, `005-p6-mobile-analyze-final.log`). Build web qua, còn cảnh báo bundle lớn đã có.

Đã chạy spotless giới hạn care và khởi chạy tiến trình nền57837: kiểm tất cả test Phase6, T008 governance, hai contract test Phase5, ModuleBoundaryTest và toàn bộ *ArchTest; chỉ nếu xanh mới chạy tiếp .\mvnw.cmd clean verify. Log tách `005-p6-scoped-reviewed.log` và `005-p6-clean-verify.log`. Tại checkpoint này chưa đọc kết quả cuối, không đánh[x] hoặc ghi nhận clean verify thành công. Không sửa mã Java trong lúc kiểm cuối.

File thêm do review: CareChecklistSources.jsx đọc riêng medicationDoses/openAlerts/handoverTasks; hai bộ test web/mobile bổ sung case không bỏ nguồn, giữ projection và loại response cũ. Những file khác giữ danh mục đã ghi; không có thêm thư viện hoặc mã nguồn người khác.

### Kiểm tổng hợp sau review đã xanh; bắt đầu clean verify

`005-p6-scoped-reviewed.log` kết thúc BUILD SUCCESS:61test đơn vị/kiến trúc,67IT,0failure/0error/0skip. Có đủ bảy bộ IT Phase6 cùng T008_CareGovernanceIT (6ca), các test runtime/YAML/JSON/history/Public API, hai test hợp đồng Phase5, ModuleBoundaryTest và40ArchTest. Runtime enum/readOnly và allowedCommands sau sửa đều được xác minh xanh trong lượt này; không chỉ dựa đọc diff.

`005-p6-web-all-tests-recheck.log`:71file/389test xanh, exit0 với --maxWorkers=2; StaffPage không cần sửa. Web lint sau sửa alias ref sạch,15test own UI cuối xanh; build qua. `005-p6-mobile-all-tests-final.log`:63/63 xanh, analyzer No issues found. Các log RED trước đó được giữ để chứng minh trình tự, không ghi đè/xóa lịch sử lỗi.

Tiến trình nền đã chuyển sang .\mvnw.cmd clean verify toàn backend (`005-p6-clean-verify.log`, bắt đầu khoảng23:00 ngày01/11/2026). Tại checkpoint này đang chạy unit/kiến trúc, chưa có kết luận toàn build. Giữ toàn bộ task Phase6 chưa đánh[x] vì còn gate nghiệp vụ/nguồn và chưa qua kiểm cuối.

Checkpoint kiểm nền23:03 ngày01/11/2026: clean verify đã qua772/772 test đơn vị/kiến trúc,0failure/0error/0skip, chuyển sang failsafe integration-test. Chưa có kết quả toàn integration hoặc Spotless cuối; vẫn không đánh[x].

Trong lượt clean verify Phase6, ParameterAsOfIT.US4_8_valueAtPastInstant_isTheValueCurrentThen tái hiện đúng expected20M/actual10M (line77), cùng trường hợp ở team-plan mục8 dòng233 từ Phase4. Chỉ ghi nhận đúng ca đã đối chiếu; không kết luận các lỗi khác nếu chưa có log. Không sửa nguồn/test A. Build vẫn chạy tiếp integration-test tại checkpoint này.

Trong clean verify tiếp tục tái hiện FR_003_CarePlanLifecycleIT.FR_003_maximumEscapedReturnReasonSurvivesOutboxStorage: JSON reason hợp lệ vượt EVENT_PUBLICATION.SERIALIZED_EVENT VARCHAR4000, DataIntegrityViolationException (testline149). Đã đối chiếu plan mục gate reason và team-plan dòng228: đây là dependency được chủ dự án chọn chờ A từ Phase3, không tự mở rộng bảng chung/cắt reason. ApiContractIT cũng đỏ ở kiểm đường dẫn runtime chưa đăng ký trong các hợp đồng của test chung; test hiện chưa có registration005, các endpoint mới Phase6 cần A review cùng phần đã bàn giao. Chưa tổng kết build khi integration vẫn chạy.

### Kết quả cuối lượt Phase 6 — 01/11/2026

Backend clean verify chạy nền kết thúc exit1 sau20:54phút (23:20:43):772unit/kiến trúc xanh;1713IT có8failure+10error,0skip. Spotless.check xác nhận2387file sạch. Scoped sau review61unit/kiến trúc và67IT xanh. Care toàn kho206IT:0failure,1error là outbox reason đã chờA từ Phase3; không có test Phase6 đỏ.

Đối chiếu toàn bộ18tên phương thức lỗi với danh sách Working-history trước Phase5: đều đã được ghi nhận từ Phase4, không có tên lỗi mới trong lượt này. Đây là đối chiếu tên và báo cáo hiện tại, không thay bằng chứng root cause hoặc tuyên bố toàn backend xanh. Ba ca đã kiểm nguyên nhân trực tiếp: ParameterAsOf expected20M/actual10M; reason CarePlanReturned vượt SERIALIZED_EVENT; ApiContractIT chưa đăng ký đầy đủ đường dẫn005. Các ca còn lại thuộc resident như danh sách sau:

- `ParameterAsOfIT.US4_8_valueAtPastInstant_isTheValueCurrentThen` — đã ghi trước Phase5.
- `FR_003_CarePlanLifecycleIT.FR_003_maximumEscapedReturnReasonSurvivesOutboxStorage` — đã ghi trước Phase5.
- `ApiContractIT.R_14_generatedApi_matchesContract` — đã ghi trước Phase5.
- `BR_M01_01_TemporaryAbsenceOverdueIT.BR_M01_01_afterRecordReturn_noAlert` — đã ghi trước Phase5.
- `BR_M01_02_ReassessmentNoticeIT.T048_recordReturnFromHospital_startsTheDeadlineForUnmetNotices_andAddsReturnBasis` — đã ghi trước Phase5.
- `BR_M01_02_ReassessmentNoticeIT.T048_recordReturnFromTemporaryAbsence_doesNotNotify` — đã ghi trước Phase5.
- `CompleteAdmissionConsentIT.FR_012_withoutActiveConsent_completesWithWarning_andStartsReminding` — đã ghi trước Phase5.
- `CompleteAdmissionConsentIT.FR_012_withActiveConsent_completesWithoutWarningOrReminder` — đã ghi trước Phase5.
- `CompleteAdmissionWishWarningsIT.US7_4_withBothActiveWishes_completesWithoutEitherWarningOrReminder` — đã ghi trước Phase5.
- `CompleteAdmissionWishWarningsIT.US7_4_withoutAnEndOfLifeWish_completesWithWarning_andStartsReminding` — đã ghi trước Phase5.
- `CompleteAdmissionWishWarningsIT.US7_11_withoutATreatmentWish_completesWithWarning_andNeverReminds` — đã ghi trước Phase5.
- `FR_023h_KhongDauNangLucIT.FR_023h_openApi_hasNoCapacityField` — đã ghi trước Phase5.
- `FR_034a_CompleteAdmissionIT.FR_034a_atNinetyDays_assessmentIsStillValid` — đã ghi trước Phase5.
- `FR_034a_CompleteAdmissionIT.FR_034a_noConfirmedInitialAssessment_blocksCompleteAdmission` — đã ghi trước Phase5.
- `FR_034a_CompleteAdmissionIT.FR_034a_assessmentOlderThanCfgM0102_blocksAndNamesBothConditionsWhenOthersAreUnmet` — đã ghi trước Phase5.
- `FR_034a_CompleteAdmissionIT.FR_034a_confirmedAssessmentWithinCfgM0102_allowsCompleteAdmission_andMergesOtherConditions` — đã ghi trước Phase5.
- `ResidentContractIT.R_14_implementedResidentEndpoints_matchContract` — đã ghi trước Phase5.
- `StatusHistoryIT.FR_046_historyEndpointListsEntriesOldestFirstWithActorName` — đã ghi trước Phase5.

Bằng chứng metadata: `005-p6-verification-summary.json` và log `005-p6-clean-verify.log` tại thư mục artifacts đã ghi trong các checkpoint. Không bỏ/tắt test, không sửa nguồn A/B, không chạy lại toàn build khi các dependency này chưa thay đổi.

Web cuối:389/389test,71file xanh;15test own UI sau sửa cuối xanh; lint sạch; build đã chạy lại và qua (`005-p6-web-build-reviewed.log`). Một timeout StaffPage đã tái kiểm6/6 nguyên trạng và full389/389 với2worker; không tăng timeout. Mobile cuối:64/64test xanh (`005-p6-mobile-all-tests-reviewed.log`), scoped12/12 sau cú pháp null-aware tương đương, analyzer No issues found (`005-p6-mobile-final-analyze.log`).

Bổ sung độc lập T040 cuối lượt: thời điểm thực hiện nhập dd/MM/yyyy HH:mm theo giờ Việt Nam; test RED vì thiếu ô, sau cài GREEN12/12. Dùng common formatDateTime đối chiếu ngày hợp lệ, gửi ISO UTC qua performedAt đã được duyệt; trống dùng thời điểm server. Giữ giá trị nhập/idempotency signature khi lỗi. Không thay API/common/package. Kiểm tự động sau bổ sung đã chạy; không nhận review độc lập trước đó là đã rà phần bổ sung này.

Tài liệu đã cập nhật đầy đủ: plan/tasks/data-model, contracts/api-005.yaml và module-interfaces, proposal các quyết định đã duyệt/chờ, glossary11.9, team-plan mục8 và progress. Working-history giữ nguyên lịch sử cũ, chỉ nối tiếp file/luồng/quyết định/lệnh/kết quả. Hash nội dung chuẩn hóa của mobile/lib/common/offline/offline_database.g.dart trùng HEAD (9767d2f4ce60fd4e477005db4e2fcf1c0809a66f); không có diff nội dung common sau phục hồi. Hai file preview kiểm trực quan đã xóa; hand-over-task-003.md có sẵn không sửa. Chưa commit/push/PR, không đổi nhánh.

Phase6 chưa đóng, giữ toàn bộ checkbox: các gate được chủ dự án chọn chờ vẫn còn. Thứ tự tiếp theo theo tasks.md: T033 kiểm phát sự kiện tính phí sau E010 chốt quantity; T034 test offline sau A chốt content; T036 hoàn tất billable/callback và phát sinh đo sau D/E; T037 hai handler sau A; T038 nhận việc tồn ca sau sau E008/Phase7 chốt; T039 summary sau E012; T040 nguồn danh mục mảng/SLEEP-MOOD-BEHAVIOR, phát sinh, sync và bản từ chối/shell/cache logout sau A; T041 nghiệm thu trực quan khi có công cụ phù hợp. T032/T035 và phần online còn lại đã có bằng chứng nhưng không dùng để đóng toàn phase. Các gate cũ outbox/nguồn B-D-E/retention/VOID giữ nguyên, khi merge ưu tiên listener nguồn B/E như yêu cầu. Sau khi dependency được giải quyết cần scoped+ArchTest rồi clean verify cuối lại trước đánh[x]; chỉ sang T042 Phase7 theo thứ tự khi được tiếp nhận phạm vi đó.

## Tiếp nhận Phase 7 Spec 005 — 02/11/2026

Yêu cầu: thực thi T042–T046, test trước/quan sát RED, sau mỗi lát cắt chỉ kiểm test liên quan và ArchTest, cuối chạy nền clean verify trước khi đánh checkbox; giữ nguyên quyết định chờ nghiệp vụ/API và không commit/push/đổi nhánh. Working-history.md đã tồn tại; phiên này chỉ bổ sung lịch sử, không xóa nội dung cũ. Checkout ban đầu ở feature/005-daily-care-and-hand-over; git status có Working-history.md và specs/003-room-bed-allocation/hand-over-task-003.md chưa tracked. Không sửa file bàn giao003.

Đã đọc AGENTS.md, docs/team-plan.md, CLAUDE.md, constitution, backend/README.md, tasks.md và plan.md/Ghi chú triển khai005; tra data-model/contracts/research C-10/C-12, FR-043–047a/052 và BR-M04-05/06/07/29, Q85/Q93 trong nghiệp vụ khi cần. Đã báo các nhóm dòng HungNX-nguyen trong mục8. Gate nhận việc tồn ca sau đã được chủ dự án chọn chờ E008 từ Phase6 vẫn giữ. Không tự đổi ca/giờ kiểm nhận việc.

Phát hiện hiện trạng: CareTaskDueStep chỉ quét NOT_YET_DUE; chưa chuyển DUE hết khung. CareTaskNotices đã có nhắc/IMPORTANT/MANDATORY ban đầu qua ReminderMarkers nhưng chưa leo thang NORMAL hoặc gộp chạy bù/lịch sử nhắc. ShiftTaskApi/TaskHandoverApi/HandoverTaskApi mới có interface và fake, chưa có production. DTO TaskHandoverView là chữ ký tương thích consumer008; HandoverTaskView là projection đầy đủ005. Không đổi chữ ký hai interface.

T042/T043: tạo CareOverdueTestBase (fixture riêng từ mẫu HTTP/MySQL005, id TestUsers.nextId; spy lọc truy vấn clock theo floor của fixture để cô lập dữ liệu test sót), BR_M04_05_OverdueReminderIT (biên khung, ba mức, một lần, CFG_M04_12, không sinh task), FR_047_TaskHandoverIT (đọc floor, current state, callback production, đóng NORMAL/idempotent, chuyển IMPORTANT giữ plannedAt). Chưa có mã production mới. Lượt Maven đầu thất bại cache .m2 lock trong sandbox, không tính RED chức năng; chạy lại với quyền cache/MySQL. Lượt005-p7-red2.log dừng compile do CareAssignmentView import trùng DTO/SPI; sửa bằng tên qualified và chạy005-p7-red3.log, tại lúc ghi chưa kết thúc. Không coi lỗi compile là test RED chức năng.

Chủ dự án trả lời “1”, duyệt dịch T046 nguyên nghĩa: GET /cong-viec/qua-han -> GET /care-tasks/overdue; query tangKhuVucId -> floorAreaId; response mảng CareTask/phạm vi quyền giữ nguyên. Không thêm endpoint/trường/thư viện khác. Log phiên này đặt tại C:/Users/nguye/.codex/visualizations/2026/11/02/01a19a62-9c6c-7bb3-8b5d-5b150f6ba35e/.

### T044/T045: triển khai và kiểm hồi quy

RED chức năng đầu005-p7-red3.log:40Arch xanh;6IT có3failure (DUE chưa chuyển OVERDUE, chưa leo thang) và3error (chưa có bean careHandoverQueries/careTaskHandover). Sau cài,6IT xanh trong005-p7-green.log; lệnh tổng thể vẫn exit1 do ba file test mới bổ sung giữa lượt kiểm chưa định dạng, không báo BUILD SUCCESS. Lỗi Spotless bộ lọc dùng glob thay regex trong005-p7-format.log đã sửa sang -DspotlessFiles=.*care.*.

Cài CareTaskClockProcessor dưới internal/task/service, mở rộng CareTaskStore.waiting/overdueAt/carry/overdue và tái sử dụng CareTaskDueStep. Tách điều phối khỏi transaction xử lý; dòng thường xử lý riêng, các công việc chạy bù cùng nhóm người nhận xử lý nguyên tử để marker và event gộp không lệch nhau. CareTaskNotices lưu lịch sử REMINDER/IMPORTANT_ESCALATION/ALERT_REQUESTED/NORMAL_ESCALATION; mốc NORMAL lấy overdue_at thực tế + ParameterApi.CFG_M04_12. Cờ carryover dùng ENUM/index đã có V005_99, không tạo migration mới. CareTaskHandling bổ sung previousPerformerId/performerId/handoverId, giữ constructor cũ; CareTaskService.record có overload ghi căn cứ M09.HANDOVER cho callback. Không đổi interface/DTO công khai.

CareHandoverQueries cài HandoverTaskApi đầy đủ005 (Optional resident, reminderCount, alertIds), lưu ghi vào bàn giao bằng StatusHistoryApi; CareTaskHandoverProjection giữ chữ ký TaskHandoverApi của008. Projection008 có residentId primitive, không biểu diễn được task gắn vị trí: guard503 rõ ràng, không dùng id0. CareTaskHandover cài onHandoverConfirmed MANDATORY: xét lại trạng thái dưới khóa, đóng NORMAL OVERDUE có trong taskIds của bàn giao, chuyển các việc còn mở, giữ plannedAt/status, giữ người đã nhận lúc chờ, marker chống replay khi nextShiftId=null. Các callback vắng/đổi phân công còn thiếu từ T038 trả503, chưa được nghiệm thu; không no-op và không coi toàn ShiftTaskApi đã hoàn chỉnh.

RED lượt005-p7-next-red.log:4IT có3failure (chưa có tồn, chưa gộp, route chưa tồn tại bị400 từ /care-tasks/{id}) và1ca giữ người đã nhận xanh;40Arch xanh. Sau bổ sung,005-p7-scoped-green.log BUILD SUCCESS/01:35phút:10IT/40Arch xanh. Sau đó thêm FR_047_HandoverRegressionIT tái hiện5lỗi:4failure+1error (replay chưa có ca sau đóng nhầm, ghi lại cùng mục bị unique, batch gộp nhầm người nhận hiện hành, thiếu thông báo thành chung, ambiguous fake/real). Sửa bằng marker cùngtransaction, phân nhóm lại sau khóa, thông báo TaskBecameShared/SharedTaskWithoutQualifiedStaff và @Primary cho fakeCareRequirements trong FakeM04Config doC sở hữu. 005-p7-review-green.log BUILD SUCCESS/01:47phút:15IT/40Arch xanh.

Review độc lập theo requesting-code-review (chỉ đọc, không test DB) phát hiện2P1: task đã carryover FROM_PREVIOUS_SHIFT không được đưa lại thành chung ở ca thứ3; bàn giao chọn role nhưng bỏ qua chứng chỉ hiện hành. FR_047_ReviewFindingsIT được viết rồi chạy005-p7-independent-review-red.log:2failure đúng hai finding,40Arch xanh. Sau đó sửa điều kiện pending để chỉ loại AWAITING_HANDOVER_CONFIRMATION, đọc CareCatalogStore yêu cầu chứng chỉ hiện hành và qualified tại ngày xác nhận theo BusinessTime.ZONE, dùng roleMatches thống nhất quy tắc điều dưỡng. Không ai đủ điều kiện thì việc chung và cảnh báo nguồn thiếu người. Fixture của ca chuyển thành công bổ sung qualified=true cho nhân viên mới (trước test chưa dựng điều kiện này), không bỏ assertion giao người. Reviewer không kết luận các dependency đã chọn chờ là hoàn tất, không review lại sau sửa; GREEN hồi quy là bằng chứng sửa.

Lượt005-p7-final-scoped.log:41Arch/ModuleBoundary xanh;48IT có1failure và47xanh, gồm17ITPhase7 xanh và30/31ITPhase4 xanh. Ca đỏ BR_M04_26_clockStepMovesDueTasksWithoutCreatingNewOnes còn kỳ vọng DUE ở đúng window_to vì lúc viết Phase4 chỉ có T024; planPhase4 ghi T044 chưa cài. FR043/C12/T044 hiện yêu cầu window_to<=now chuyển OVERDUE. Cập nhật testC của chính feature để kiểm DUE ởto-1microsecond rồi OVERDUE ởto, giữ kiểm không sinh task và chạy lại không tạo thêm. Không sửa test người khác hoặc bỏ assertion. Đang kiểm lại, chưa ghi xanh hoặc đánh[x].

### Đối chiếu nguyên văn các dòng có HungNX-nguyen đầu phiên

Có 26 dòng ghi trực tiếp tên HungNX-nguyen trong mục8 trước khi bổ sung bàn giaoPhase7:

| Người / trạng thái | Việc | Phụ thuộc | Căn cứ | Tình trạng |
| --- | --- | --- | --- | --- |
| A review, C (005, HungNX-nguyen) bàn giao | Review11API/DTO và28event005 mới, fake API riêng; hai dòng đăng ký CareMinuteJob/CareDailyJob trong SchedulerScopeArchTest theo hướng dẫn test. Không đổi APIclock000/runJobs | D006/007/011, E008/012/015/016 và C014 dùng hợp đồng; API có khai báo/fake nhưng chưa có adapter story | 005 T003/T004/T006; glossary11.2; module-interfaces1; planPhase2 | Khai báo/khung/test đã cài tại checkout;27kiểm mới xanh,474unit/kiến trúc xanh trong clean verify cuối2026-10-27;1112IT còn15lỗi/8lớpM01 đã bàn giaoB. Quyền CARE_TASK_CATALOG đã được A công bố; còn cài T007 và review A; T007a đã có mã/test23/23 xanh trong lượt kiểm cuối 2026-10-28; chưa nghiệm thu/commit/push/merge Ghép30/10: ShiftTaskApi giữ đủ chữ ký, haiCarejob đổi String runId và FQN đăng ký; chờ A review SchedulerScopeArchTest. |
| C (005, HungNX-nguyen) | Công bố tám SPI 005: ShiftStaffPort, TaskSourcePort, VitalSignPort, MedicationChecklistPort, AlertReadPort, DayCarePickupPort, QualityCheckPort, TimetableSourcePort; fake duy nhất do 005 sở hữu tại support/fake/care, import FakeM04Config ở test/demo | D (006/007/011), E (008/012), C (014): cài nguồn thật theo contracts mục 2.1; API còn lại/sự kiện chưa nằm trong Phase1 | 005 T002; plan Ghi chú triển khai; glossary11; module-interfaces2.1 | Đã cài ở checkout (2026-10-26); scoped T002 39/39 xanh, 23 ca Phase1 xanh trong clean verify cuối; gate còn 15 lỗi/8 lớp M01 đã bàn giao B (log 005-p1-clean-verify.log). Giữ T001–T002[ ]; chưa nghiệm thu/commit/push/merge, chưa ghi nguồn thật đã tích hợp |
| C (003, HungNX-nguyen) | Bản thật DayCareCapacityApi Phase8: lock → occupiedSessions → checkCapacity cùng transaction; chỗ ở CFG-M03-01, không tự sinh lịch. Gọi port sau khi cấu hình khu nghỉ/buổi | B (004 đăng ký lịch đến), C (005) | 003 T049–T052; plan ghi chú Phase8; test DayCareCapacity/Concurrency | Đã cài trong checkout; scoped xanh, chưa nghiệm thu phase/merge |
| D (007) | Cung cấp bản thật SPI `roombedallocation.api.spi.AllocationInfectionStatus` và kiểu sự kiện công khai/fake cho gỡ khoanh vùng, thêm người vào danh sách tiếp xúc; thống nhất đúng hợp đồng module trước khi003 viết listener. 007 thuộc incident/health theo mục1; checkout hai gói này chỉ có package-info (đính chính tênm07 ở ghi chú trước) | C (003, HungNX-nguyen): T053–T054/FR-024a gỡ dấu chờ và cập nhật khi thêm tiếp xúc | 003 researchG-08, plan Phase9/11; SPI/fake003 đã có | Chờ D;003 không khai hộ kiểu incident/health, không đưa fake vào production |
| C (003, HungNX-nguyen) | Cài facade BedAllocationCommandApi/T033 cho chuyển theo lịch và callback sau chuyển gấp; còn nhánh T032 chuyển từ phân bổ đang có dấu giá (chủ dự án giữ SPI, tạm503/bảo toàn dấu và reminder). Nối nguồn giá hợp đồng trước khi giải quyết nhánh này; writer gỡ/đổi AWAITING_REQUEST/AWAITING_SAME_PRICE phải giữ đúng marker trong transaction | 003 T032/T033/T057–T059; B (004) tích hợp lịch | 003 researchG-07/G-09; plan T029/T032 2026-10-29 | T02915ca/bộ hẹp43IT xanh, clean verify cuối680unit/252IT M03 xanh; T032 một phần, T033 chưa provider, T058 pending vẫn503/rollback; không đổi API hoặc đưa fake vào production |
| A review; C (005, HungNX-nguyen) bàn giao | Review hai dòng đăng ký/lọc 005 trong frontend/scripts/gen-api.mjs (chỉ sinh 23 operation đã dịch), hai đăng ký màn/menu care-plans/care-catalogs và guard riêng M04.RISK_PARTICIPANT/M04.RISK_OPINION trên record_event trong V005_96. API bảng chung và source A giữ nguyên | Web T017 dùng client sinh; trigger khóa con theo trạng thái care_plan, không đổi loại khác trên bảng chung | frontend/README; V005_96; plan Phase3; Working-history.md | Có bản cài/test trong checkout; chờ review chủ phần nền trước merge, chưa commit/push/PR |
| A (000 outbox), C (005, HungNX-nguyen) bàn giao/retest | Mở rộng EVENT_PUBLICATION.SERIALIZED_EVENT để chứa đầy đủ JSON CarePlanReturned với reason hợp lệ tối đa2000ký tự. Fixture2000dấu ngoặc kép serialize4233ký tự, vượt VARCHAR(4000); worst-case escape có thể lớn hơn. Chủ dự án duyệt event reason nhưng chọn chờ A mở rộng outbox, không duyệt C sửa schema. Không cắt lý do/giảm hợp đồng; C giữ ca hồi quy MySQL và retest sau khi A cung cấp migration | 005 T011/T014 FR-003: lệnh Trả lại và thông báo HEALTH không được rollback vì payload hợp lệ; không merge phần reason trước khi test lưu tối đa xanh | 000 V000_05; 005 module-interfaces; NFR_04_CareEventsTest và FR_003_CarePlanLifecycleIT; Working-history.md | Chờ A; C chưa tạo migration, chưa nghiệm thu reason; các lỗi gate nền A/B giữ nguyên |
| E (008), C (005, HungNX-nguyen) bàn giao/retest | Cài ShiftStaffPort thật, bổ sung members(shiftId) đã được chủ dự án duyệt; chốt nguồn liệt kê đầy đủ ca đang diễn ra cho nhắc ca chưa có công việc, kể cả tầng/khu chưa có giường. Không thêm ongoingShifts trong lượt C theo lựa chọn hoãn của chủ dự án | 005 T022–T025; T024 nhắc ca rỗng chưa cài | 005 phase4-contract-proposal.md mục3/5/7; contracts/module-interfaces.md | Chờ E; fake/test không đại diện nguồn thật |
| D (007/011), C (014/005, HungNX-nguyen) bàn giao | Review và cài TaskSourcePort.sources(residentId,from,to) đã duyệt, giữ chữ ký cũ; mỗi nguồn chỉ trả managedTypes của mình trong [from,to). Thống nhất cảnh báo MANDATORY gắn vị trí với 007 vì AlertRequested bắt buộc residentId; chủ dự án chọn chờ, không gán id giả | 005 T024/T025; mandatory location overdue hiện503, có thể rollback toàn lượt chạy bù | 005 phase4-contract-proposal.md mục3/7; CareTaskNotices/CareTaskDueStep | Chưa nghiệm thu nguồn thật hoặc cảnh báo vị trí |
| A (mobile nền), C (005, HungNX-nguyen) bàn giao/retest | Cung cấp transport hỗ trợ JSON array request/response, Idempotency-Key và giữ properties.rowErrors. Chủ dự án không cho C sửa common; giữ get/post cũ tương thích. Sau đó C cài adapter production/đăng ký ShiftTaskPlanningScreen | 005 T026/T031; widget/gateway lập lô và điểm danh đã có, chưa nối app | 005 phase4-contract-proposal.md mục6/7; mobile/lib/staff/care_tasks | Chờ A; không sửa common, không dùng gateway fixture trong production |
| A review; C (005, HungNX-nguyen) bàn giao | Review đăng ký màn/menu care-task-planning trong App.jsx/navigation.js, client005 sinh thêm7operation Phase4; dùng GET /shifts/{id}008 hiện có cho roster vị trí. Không đổi transport web dùng chung | 005 T026 web; shared shell A sở hữu | 005 plan Phase4; frontend/src/features/care-tasks/ShiftTaskPlanningPage.jsx | Có bản cài tại checkout; chờ review trước merge, chưa commit/push/PR |
| A (parameter), C (005, HungNX-nguyen) bàn giao log | Xác minh ParameterAsOfIT.US4_8_valueAtPastInstant_isTheValueCurrentThen: expected20M nhưngactual10M. Tái hiện ở hai lượt cleanverify Phase4 của C; không có trong16phươngthức baselinePhase3 được đối chiếu trước đó. C không sửa nguồn/testA, chưa kết luận nguyên nhân | Gate cleanverify toànbackend; không quy mọi lỗi mới cho baseline | backend/target/failsafe-reports; log005-p4-clean-verify-final{,2}.log; Working-history.md | Chờ A xác minh;31ITPhase4 và6ITT008 xanh ở lượt mới |
| A review; C (005, HungNX-nguyen) bàn giao | Review migration V005_94__shared_day_care_attendance.sql mở rộng simple_record theo R-15: cột/index m04, UNIQUE người/ngày và CHECK riêng M04.DAY_CARE_ATTENDANCE. Không sửa common/API shared hoặc tạo bảng attendance riêng | 005 T028, cần giữ ràng buộc schema khi cài T029 | 005 data-model/plan Phase5; FR_012_AttendanceSchemaIT | Bản cài checkpoint tại checkout; chờ review chủ bảng chung trước merge, chưa commit/push/PR |
| B (004), C (005, HungNX-nguyen) chờ tích hợp Phase5 | Công bố API lịch/ngày nghỉ/báo vắng và kiểu sự kiện004 cùng TrangThaiCoMatBanTruPort đã mô tả trong contracts. SPI DayCareCalendarPort/callback005 đã được chủ dự án duyệt; adapter/listener thuộc care sử dụng API/eventadmission để giữ chiều005→004, B không importcare càiSPI hoặc gọi callback trực tiếp gây vòng | 005 T029/T030, nguồn production và trạng thái có mặt cho004 | phase5-contract-proposal.md mục4; researchC-02; contracts/module-interfaces.md | Chờ B cung cấp hợp đồng/types/nguồn; fakecare không đại diện tích hợp thật Khi merge, nếu B/E đã có listener/kiểu sự kiện nguồn tương ứng thì ưu tiên bản B/E thay đường tích hợp tạm005; giữ chữ ký cũ, hợp nhất tránh chạy trùng và bàn giao B/E review. |
| A (000 shared/retention), C (005, HungNX-nguyen) bàn giao Phase5 | Nối retention cho M04.DAY_CARE_ATTENDANCE/M04.ATTENDANCE_RECORD đã gộp vào simple_record/record_event. Provider005 cũ còn trỏ care_day_attendance/care_attendance_record không tồn tại; SharedRowRetention xóa raw theo M01.RESIDENT nhưng CorrectionRetention chỉ kiểm attachment của DataRef được enumerate. Cần enumerate chuỗi điểm danh/đính chính và kiểm hạn trước khi xóa, API xóa đúng loại/dòng shared khi cần; không để correction/chain mồ côi hoặc mất bản gốc khi correction chưa hết hạn | 005 T028/T029 và gate R-15/FR-050; chưa nghiệm thu retention Phase5 | AttendanceStore, CareRetentionType, CareGovernanceStore; ExpiredDataPurge.chain/CorrectionRetention/SharedRowRetention; Working-history.md | Review read-only xác nhận P1; C không SQL bảng chung hoặc gọi purge cả hồ sơ để né API thiếu. Chờ A thống nhất adapter/API; không claim parentM01.RESIDENT đã bảo đảm toàn chuỗi |
| B (004), C (005, HungNX-nguyen) bàn giao Phase5 | Chốt hành vi ngày đổi LOAI_LUU_TRU từ DAY_CARE sang nội trú khi attendance đã có: trạng thái cũ còn được cung cấp/tính phí không, có hủy trạng thái trước đến không, xử lý Có mặt/Đã về thế nào. refreshCalendar hiện checkResident sẽ409 sau đổi, có thể rollback lệnh004 hoặc cả AttendanceClockWork batch; không tự dùng HOLIDAY_CANCELLED cho đổi loại lưu trú | 005 T029 callback đã duyệt, nhánh LOAI_LUU_TRU chưa nghiệm thu | phase5-contract-proposal.md; AttendanceSources.checkResident/AttendanceService.refresh; Working-history.md | Chủ dự án đã chọn chờ thống nhất với B; giữ gate P1 rõ ràng, không coi callback đã đủ mọi nhánh |
| A (000 correction), C (005, HungNX-nguyen) bàn giao Phase5 | Cung cấp cách từ chối loại đính chính module chưa hỗ trợ trước khi tạo approval cáchC ở entrypoint generic000. Endpoint005 đã chặn VOID409 trước CorrectionApi.create, provider chặn khi apply; generic000 vẫn có thể tạo request VOID rồi thất bại khi duyệt. Chủ dự án chọn sửa giờ AMEND_CONTENT trước, VOID chờ chốt nghiệp vụ; mọi thay đổi SPI chung phải được duyệt trước áp dụng | 005 T029/FR-015; không để sinh yêu cầu không thể áp dụng | AttendanceCorrectionProvider/DayCareAttendanceCorrectionController; CorrectionService.create; Working-history.md | Chờ A review contract/capability; không sửa nguồnA trong lượtC |
| A review; C (005, HungNX-nguyen) bàn giao Phase5 web | Review hai dòng App/menu day-care-attendance, client005 sinh thêm4operation đã duyệt. Dùng ClipboardCheck có sẵn trong registry, không thêm icon vào common. Nguồn mobile list còn cần transportA | 005 T031, phần shell/generatorA sở hữu | DayCareAttendancePage; navigation.js/App.jsx; api005 sinh từcontract | Web374test/69file,lint/build xanh trước sửa YAML cuối; api005 SHA256 giữ nguyên sau sửa/regen; chưa commit/push/PR |
| E (012), C (005, HungNX-nguyen) chờ công thức Phase6 | Chốt quy đổi mức ăn thành averageEatingRatio và mẫu số ăn/nước trung bình (theo ngày hay lần ghi) cho PublicCareResultApi.summary; nghiệp vụ hiện chỉ liệt kê năm mức ăn. Chủ dự án đã chọn chờ thống nhất với E, không tự gán tỷ lệ | 005 T039; 012 FR-052 tổng hợp bản tin | 005 phase6-contract-proposal.md; module-interfaces 1.7; plan Phase6 | Chưa cài công thức, không trả số giả hoặc coi Optional.empty là không có dữ liệu khi thực tế thiếu quy tắc |
| A (002 offline), C (005, HungNX-nguyen) chờ hợp đồng Phase6 | Chốt content hai recordType M04.KET_QUA_CONG_VIEC/M04.GHI_NHAN_PHAT_SINH trong /sync. Chủ dự án không duyệt mục4 đề nghị của C và chọn chờ A thống nhất; không đổi envelope/OfflineHandler, không tạo handler theo payload chưa duyệt | 005 T034/T037, T040 ghi ngoại tuyến | 005 phase6-contract-proposal.md mục4; C-14; plan Phase6 | Chờ A; phần trực tuyến tiếp tục, không coi hàng đợi đã tích hợp 005 |
| D (007), C (005, HungNX-nguyen) chờ nghiệp vụ Phase6 | Thống nhất Ghi nhận phát sinh đo chỉ số khi nguồn trả AWAITING_REMEASUREMENT: FR025 yêu cầu task Hoàn thành ngay nhưng FR041 cấm đóng trước xác nhận; endpoint phát sinh chưa có202. Chủ dự án đã chọn chờ D/007 | 005 T036/T039/T040 nhánh đo phát sinh | 005 phase6-contract-proposal.md mục5; VitalSignPort | Nhánh này trả503 trước tạo task; không thêm trạng thái hoặc HTTP202 chưa duyệt |
| E (010), C (005, HungNX-nguyen) chờ quantity Phase6 | Thống nhất BillableCareResultRecorded.quantity theo loại kết quả; chủ dự án chọn chờ E/010, không tự quy đổi ml/phút/giá trị đo thành đơn vị dịch vụ | 005 T033/T036 FR-051, 010 consumer | 005 plan Phase6; phase6-contract-proposal.md | Lệnh billable chặn trước lưu, chưa nghiệm thu phát sự kiện tính phí |
| D (007), C (005, HungNX-nguyen) chờ callback Phase6 | Thống nhất metadata callback đo lại khác người/giờ gốc cho VitalSignTaskApi.confirmed; chủ dự án chọn tiếp tục chờ D/007, giữ chữ ký cũ | 005 T036/T039, 007 callback đo lại | 005 contracts/module-interfaces.md; plan Phase6 | Callback cùng người/giờ gốc có test; khác metadata trả503, không chép giá trị đo đầu thành giá trị đã xác nhận |
| A review, C (005, HungNX-nguyen) bàn giao Phase6 | Review App.jsx chuyển care-tasks sang API thật, client api005 sinh lại, FK V005_100 tới catalog_item và nối CareChecklistScreen(repository,accountId,shiftId) vào shell; xử lý cache.clearAccount khi đăng xuất/xóa quyền; transport mảng và Problem properties vẫn chờ A như Phase4 | 005 T035/T040/T041 | 005 plan; mobile staff/care_tasks | Không sửa common, pubspec hoặc tự nối ca giả; full mobile/offline chưa hoàn tất |
| E (008), C (005, HungNX-nguyen) chờ claim việc tồn Phase7 | Chốt ca/giờ kiểm người nhận đối với task có carryover: C10 yêu cầu trùng giờ dự kiến, Q93 có người ca sau nhận. Chủ dự án chọn chờ E/008, không tự chuyển sang ca hiện tại | 005 T038, Phase7 bàn giao; 008 Q93 | 005 plan/Working-history review Phase6 | Hiển thị tồn đã có; nhận việc tồn ca sau chưa nghiệm thu; allowedCommands phải theo kiểm lệnh thực |
| A (000 kiểm hợp đồng), C (005, HungNX-nguyen) bàn giao Phase6 | Đăng ký/so phần endpoint005 đã dịch trong ApiContractIT chung, giữ pending cho phase chưa cài. Sáu operationPhase6 và CareTask.resultType đã được chủ dự án duyệt; runtime có các đường dẫn mới nhưng CONTRACTS chưa có005 | Gate R-14/clean verify; 005 T039 | api-005.yaml; phase6-contract-proposal.md; common/ApiContractIT | C không sửa test common của A; lỗi đường dẫn tái hiện ở clean verify Phase6, không tắt so hợp đồng hoặc bỏ endpoint thật |

### Quyết định payload gộp và kiểm cuối đang thực hiện

Chủ dự án đã chọn“1”: chờA chốt payload gộp an toàn, không chia nhiều thông báo cùng người nhận. FR_052_OverdueBatchCapacityTest viết trước,005-p7-capacity-red.log có1failure đúng thiếu guard,40Arch xanh. Sau RED thay chia lô32 bằng chặn nhóm>32 trước transaction xử lý; dùng MAX_GROUPED_TASKS kỹ thuật, trả503 và vẫn chạy các nhóm khác. Không tự nới outbox hoặc đổi event. Nhóm lớn chưa được xử lý là gate công khai, không gọi FR052/T044 hoàn tất. Clean verify đầu bị ngắt chủ động ởcompile khi nhận quyết định; sẽ chạy lượt mới sau guardGREEN, không ghi lượt ngắt xanh/đỏtoànkho.

Xác minh trước thay guard:005-p7-final-scoped2.log BUILD SUCCESS/01:32phút,48IT (17Phase7+31Phase4) và41Arch/ModuleBoundary,0failure/error/skip. Web392test/72file xanh ởlượtđầu; lint banđầu phát hiện effect setState trực tiếp, đã sửa bằng state gắn requestKey + hủy callback cũ; scoped3test,lint/build sau sửa đềuexit0. Mobile68fulltest xanh; analyzer banđầu có1info thiếublockcatch, đã sửa và retest4widget xanh/analyzer No issues found ở005-p7-mobile-analyze3.log. Không dùng kết quả trước thay guard thay gate nguồn mới.

Đã sửa báo cáo đếm đầu phiên:26dòng cóHungNX-nguyen (không phải21 đã nói banđầu), nguyênvăn lưu mục trên. Thêm4dòng bàn giaoPhase7 choA/E/D vàpayload; không xóa bất kỳ việc cũ chưa giải quyết. Thêm glossary11.10,ghi chú tasks/plan/contracts,giữmọicheckboxPhase7[ ].

### Danh sách file thực tế của Phase 7 và mục đích

Các đường dẫn sau tính từ gốc kho. File đã tồn tại được sửa đúng phần005; file mới chỉ thuộc care/005. Không xóa file sản phẩm. Hai file preview test tạm đã được tạo rồi xóa để thử kiểm trực quan: frontend/phase7-preview.html và frontend/src/features/care-tasks/Phase7Preview.test.jsx; Vite5177 đã dừng, không đưa fixture vào bản build. File specs/003-room-bed-allocation/hand-over-task-003.md có sẵn đầu phiên không sửa.

| File | Thay đổi và mục đích |
| --- | --- |
| backend/src/main/java/com/eldercare/backend/care/internal/task/service/CareTaskClockProcessor.java | Mới; xử lý trạng thái/mốc NORMAL/pending dưới transaction, khóa lại trước ghi, đọc chứng chỉ hiện hành cho thông báo thiếu người |
| backend/src/main/java/com/eldercare/backend/care/internal/task/service/CareTaskDueStep.java | Điều phối quét trạng thái mở, xử lý dòng hoặc nhóm chạy bù, tiếp tục nhóm khác sau lỗi, chặn nhóm vượt dung lượng trước xử lý |
| backend/src/main/java/com/eldercare/backend/care/internal/task/service/CareTaskNotices.java | Nhắc/gộp/leo thang/thành chung, marker cùng transaction, lịch sử số lần nhắc, MAX_GROUPED_TASKS=32 bảo vệ payload |
| backend/src/main/java/com/eldercare/backend/care/internal/task/persistence/CareTaskStore.java | SQL care_task: waiting mở rộng, overdue_at, chuyển ca/người/dấu tồn, danh sách quá hạn lọc ScopeFilter |
| backend/src/main/java/com/eldercare/backend/care/internal/task/domain/CareTaskHandling.java | Thêm người trước/sau và handoverId vào details lịch sử, constructor content cũ còn tương thích |
| backend/src/main/java/com/eldercare/backend/care/internal/task/service/CareTaskService.java | Overload record ghi căn cứ M09.HANDOVER và details người thực hiện; caller cũ giữ nguyên |
| backend/src/main/java/com/eldercare/backend/care/internal/overdue/service/CareHandoverQueries.java | Mới; HandoverTaskApi, projection đầy đủ005, currentStates, reminderCount/alertIds, lịch sử thêm vào bàn giao chống lặp |
| backend/src/main/java/com/eldercare/backend/care/internal/overdue/service/CareTaskHandoverProjection.java | Mới; TaskHandoverApi tương thích008, giữ chữ ký và guard loại task vị trí chưa biểu diễn được |
| backend/src/main/java/com/eldercare/backend/care/internal/overdue/service/CareTaskHandover.java | Mới; callback xác nhận MANDATORY, khóa/ngăn replay, đóng theo điều kiện hoặc chuyển/giữ người đã nhận; các callbackT038 chưa cài vẫn503 |
| backend/src/main/java/com/eldercare/backend/care/internal/overdue/service/CareOverdueQueries.java | Mới; kiểm OVERDUE_TASK_HANDLING.VIEW, ScopeApi và quyền từng task; lọc người trạng thái cuối |
| backend/src/main/java/com/eldercare/backend/care/web/CareOverdueController.java | Mới; GET /care-tasks/overdue?floorAreaId, che content bằng FieldVisibilityApi, đếm nhắc thật, trả mảngCareTask |
| backend/src/main/java/com/eldercare/backend/care/web/response/CareTaskResponse.java | Overload of nhận reminderCount thật; giữ constructor và trường hợp đồng hiện có |
| backend/src/test/java/com/eldercare/backend/care/CareOverdueTestBase.java | Mới; fixture HTTP/MySQL riêng005, TestUsers.nextId và spy giới hạn floor khi kiểm job, không giả định database rỗng |
| backend/src/test/java/com/eldercare/backend/care/BR_M04_05_OverdueReminderIT.java | Mới;3ca biên cửa sổ/một lần/3mức/CFGmốc thực tế/không sinh task |
| backend/src/test/java/com/eldercare/backend/care/FR_047_TaskHandoverIT.java | Mới;3ca danh sách/currentstate/đóng/idempotency/chuyển giữplannedAt và người hợp lệ |
| backend/src/test/java/com/eldercare/backend/care/FR_047a_PendingHandoverIT.java | Mới;2ca tồn chờ xác nhận và giữ việc đã nhận |
| backend/src/test/java/com/eldercare/backend/care/FR_052_GroupedOverdueReminderIT.java | Mới;1ca chạy bù gộp và không nhắc lại |
| backend/src/test/java/com/eldercare/backend/care/FR_045_OverdueTasksIT.java | Mới;1ca HTTP danh sách/quyền phạm vi tầng |
| backend/src/test/java/com/eldercare/backend/care/FR_047_HandoverRegressionIT.java | Mới;5ca wiringfake/replaykhôngcasau/ghi mục lặp/người nhận batch/thông báo thành chung |
| backend/src/test/java/com/eldercare/backend/care/FR_047_ReviewFindingsIT.java | Mới;2ca P1 reviewer, tồn qua3ca và chứng chỉ mới không đủ |
| backend/src/test/java/com/eldercare/backend/care/FR_052_OverdueBatchCapacityTest.java | Mới;1unit kiểm chặn nhóm lớn không chia và vẫn xử lý nhóm khác |
| backend/src/test/java/com/eldercare/backend/care/BR_M04_01_ShiftTaskPlanningIT.java | Tăng kiểm biên job ởto-1microsecond/to theo T044, giữ kiểm không sinh task |
| backend/src/test/java/com/eldercare/backend/support/fake/care/FakeM04Config.java | @Primary cho fakeCareRequirements opt-in để không ambiguous với bean production mới; không tạo fake thứhai |
| frontend/src/features/care-tasks/OverdueTasksPage.jsx | Mới; danh sách API thật, tải/lỗi/rỗng/quyền, TaskBlock/DetailLayout đọc, linkchecklist, requestKey không giữ response phạm vi cũ |
| frontend/src/features/care-tasks/OverdueTasksPage.test.jsx | Mới;3test API/tảilại/rỗng/403 và menu theo khóa quyền |
| frontend/src/App.jsx | Chỉ thêm import và đăng ký overdue-care-tasks |
| frontend/src/common/layout/navigation.js | Chỉ thêm mục005 cùng căn cứ Phụlục27, dùng icon ListChecks đãcó |
| frontend/src/api/api005.js | Sinh lại qua npm run gen:api; thêm getCareTasksOverdue, không sửa tay |
| mobile/lib/staff/care_tasks/overdue_tasks_screen.dart | Mới; danh sách chỉ đọc với loader tiêm vào/accountId/canView/floorAreaId; chống response cũ khi đổi tài khoản, trạng thái tải/lỗi/thửlại, dùng theme/formatDateTime chung |
| mobile/test/staff/care_tasks/overdue_tasks_screen_test.dart | Mới;4widgettest hiển thị/quyền/đổi account/mấtmạng thửlại; fixture ởtest, không nối app |
| specs/005-daily-care-tasks/contracts/api-005.yaml | Dịch một GET/query đãđượcduyệt, thêm x-implementation-phase:7, response CareTask giữ nguyên hình dạng |
| specs/005-daily-care-tasks/contracts/module-interfaces.md | Ghi bản cài API/callback và dependency/giới hạn, không đổi chữ ký công khai |
| specs/005-daily-care-tasks/tasks.md | Ghi ánh xạ tên thực tế và trạng thái chưa nghiệm thu, không đánh checkbox |
| specs/005-daily-care-tasks/plan.md | Ghi kế hoạch, quyết định kỹ thuật, hai finding reviewer, thay test biên, quyết định chờ payloadA, lượt cleanverify bịngắt |
| specs/000-shared-foundation/glossary.md | Chỉ thêm mục11.10 của005, tên lớp/màn/endpoint/dấu tồn |
| docs/team-plan.md | Chỉ nối4dòng bàn giaoPhase7 choA/E/D; các dòng cũ giữ nguyên |
| Working-history.md | File đãcó đầu phiên, chỉ nối lịch sử/RED-GREEN/file/luồng/quyết định và gate; không xóa nội dung cũ |

Tệp sinh tự động ngoàiapi005 do npm/Flutter chạy có biến động xuống dòng trong workingtree nhưng git diff nội dung không thay đổi; không sửa semantic các module khác, common/API nền, pom.xml/package.json/pubspec.yaml hoặc migration. Phép chuẩn hóa team-plan từng biến literal `npm thành xuống dòng; phát hiện bằng git diff --check, khôi phục nguyên nội dung HEAD (đầuphiên file sạch) rồi chỉ nối lại4dòng mới. git diff --numstat team-plan sau sửa là4thêm/0xóa, diff --check sạch. Không dùng gitrestore/đổi nhánh hoặc sửa file nền để che lỗi.

### Quan hệ giữa các file và luồng dữ liệu

1. CareMinuteJob lấy BusinessTime.now, gọi CareTaskDueStep. CareTaskStore.waiting đọc task mở đã đến mốc. Dòng thường gọi CareTaskClockProcessor.advance; nhóm chạy bù cùng floor/người nhận gọi advanceBatch sau kiểm dung lượng. Processor khóa cư dân tăng dần rồi task, đọc lại trạng thái, bỏ người trạng thái cuối, chuyển DUE/OVERDUE trong care_task; CareTaskService.record ghi StatusHistoryApi/AuditLogApi. CareTaskNotices ghi ReminderMarkers, history và phát event cùng transaction; outbox chuyển tới009/007. Lúc chạy thật lưu overdue_at, cửa sổ dự kiến giữ nguyên; NORMAL lấy deadline từ ParameterApi khi đánh giá.
2. Pending bàn giao dùng ShiftStaffPort.shift/shiftContaining/handoverConfirmed/onShift. Khi ca sau đãbắtđầu, người ca trước không còn ca và bàn giao chưa xác nhận, task thành chung và dấu AWAITING_HANDOVER_CONFIRMATION; không đổi plannedAt/status. Task đã FROM_PREVIOUS_SHIFT vẫn được xét lại ởca kếtiếp. Không tự tạo ca/task/mục bàn giao. Nhận việc ở ca sau vẫn là gate đãchờE, không ngầm đổi giờkiểmclaim.
3. 008 đọc nguồn qua HandoverTaskApi đầy đủ hoặc TaskHandoverApi tương thích. AlertReadPort cung cấp alertIds, StatusHistoryApi cung cấp sốnhắc/trạng thái mới. Actor tựghi mục008; recordedInHandover ghi lịch sử thamchiếu, không sinh mục. Khi008 gọi onHandoverConfirmed, transactionMANDATORY giữ nguyên lệnh ngoài, khóa cư dân/task, marker theo bàn giao/task ngăn replay; NORMALđủđiềukiện đóngNOT_PERFORMED với quy tắc hệthống, việc còn lại chuyển shift_id/người/dấutồn. Chứng chỉ đọc catalog hiện hành, qualified tại ngàyxácnhận; không ai đủ thì chung và thông báo. NextShiftId null không tạo idgiả, giữ cagốc và marker.
4. Web gọi getCareTasksOverdue sinh từapi005 -> CareOverdueController -> CareOverdueQueries -> ScopeApi/PermissionApi và CareTaskStore. Mỗi row kiểm quyền lại, content che bằng FieldVisibilityApi, reminderCount từ history. CareTaskResponse -> OverdueTasksPage -> adapter careChecklistView -> TaskBlock/DetailLayout. Chi tiết chỉđọc; xửlý qua checklist sẵn có, không thêm endpoint ngoài duyệt. result gắnrequestKey giúp dữliệu cũ không hiện khi tải lại/đổiphạmvi.
5. OverdueTasksScreen mobile nhận loader từ shell tươnglai; ởphiênnày chỉwidgettest cónguồn giả. Không có adapterproduction vì ApiClient chung chưa trả JSONarray; không tựfetch hoặc sửa common đểnénguồn thiếu. accountId/floorAreaId/canView/load thay đổi sẽ xóa rows, tănggeneration; response cũ không phục hồi dữliệu tài khoản khác.

### Các việc còn tồn tại và thứ tự tiếp theo theo tasks.md

Phase7 vẫn chưa hoàn thành. Trong phạmviPhase7, tiếp T042: bổ sung và nghiệm thu chạy bù nhóm lớn sauA chốt payload; T043: kiểm008 bằngnguồnShiftStaffPort thật/projection vịtrí và nhánh đãnhận theo quy tắcE; T044: gỡ gate payload/cảnhbáovịtrí và nối metadata MeasurementTaskMissed hếtca sauD007; T045: nghiệmthu consumer008/nguồnthật và các callbackT038 cònthiếu trướcghép; T046: transportmảng/đăngkýapp từA, nghiệmthu trựcquan/kịchbản quyền và các lệnhxửlý ởchecklist. Sau đó kiểmriêng+Arch, cleanverify mới và xửlýgateA/B trước đánh[x]. Chưa bắtđầu T047Phase8. Các việc còn mở củaPhase3–6 theo ledger trước vẫn giữnguyên (outboxreason, retention/correctionchain,VOID,nguồnB,offline/quantity/summary/đochỉsố,claimtồn).

### Full gate phát hiện xung đột primary và bản sửa

005-p7-clean-verify-final.log dừng exit1 sau02:04phút:773unit,0failure/1error/0skip, chưaIT. Lỗi T003_MergedApiCompatibilityTest.T003_bothFakeConfigurationsResolveApisWithoutAmbiguousPrimaryBeans mới do@Primary củaFakeCareRequirements cócùngCarePlanApi với fakeCarePlanApi. Không quy lỗi này cho18baseline. Đối chiếu CarePlanQueries/AttendanceQueries cho thấy pattern production@Fallback đã có. Giữtestcũnguyên, gỡ@Primaryvừathêm trongFakeM04Config và đặt@Fallback ởCareTaskHandover/CareTaskHandoverProjection/CareHandoverQueries. FakeM04Config bảncuối khôngdiffHEAD; dòngfileởbảngtrên mô tả thayđổi trunggian đãhoànnguyên. Khôngcófake mới hoặc productionfake. TiếpT003compatibility+capacity+Arch/Boundary+5ITwiring trướcchạycleanverifylạitoànkho.

Các lệnhphụ sandbox Java24/flutter banđầu bịtreo đãđượcgửiCtrl+C đểdừng; Maven/Flutterxácminh dùngJDK25/cacheSDKđượcduyệt,khôngđổiJAVA_HOME toànmáy. .specify/feature.json đọc lạiđãtrỏ005 từtrước,khôngcầnsửa. Bảncàicuối giữtấtcảgiớihạn/decisionchờA/E/D đãghi;khôngđánhcheckboxvìbuildcòncầnkiểmlạivàdependencychưaxong.

### Gate wiring sau sửa và trạng thái kiểm cuối

005-p7-wiring-green.log BUILD SUCCESS/01:43phút:46unit/Arch/ModuleBoundary (4compatibility+1capacity+41kiếntrúc/boundary) và5IT hồi quy,0failure/error/skip;Spotless2402file sạch. Bản cuối @Fallback đã kiểm cả import hai configfake cùng lúc lẫn beanproduction được lấy theo tên. Không giữ thay đổi FakeM04Config.

Đã khởi chạy nền .\mvnw.cmd clean verify mới005-p7-clean-verify-final2.log sau gateGREEN, giữnguồn cốđịnh. Đã chuẩn bị script chỉđọc XML và so18phươngthứcbaseline từPhase6 vào artifact summarize-phase7.ps1, chỉchạy khiMaven kếtthúc. Không dùng tên lỗi giốngnhau để kếtluận nguyênnhân giốngnhau; sẽ ghi rõ lỗi mới nếu có. Chưa ghi kếtquả cleanverify khi đangchạy.

Webcuối sau effectrequestKey:005-p7-web-all-final.log có392/392test/72file xanh, không skip;005-p7-web-lint2.log exit0 và005-p7-web-build2.log exit0. Mobile68/68fulltest xanh;4/4widgettest cuối/analyzer No issues found sau sửa blockcatch (005-p7-mobile-final-scoped2.log,005-p7-mobile-analyze3.log). BUILD kíchthướcchunk lớn chỉ là warning Vite, không hạ cảnhbáo hoặc chỉnhcấuhình nền. Trựcquan chưađạt vìcôngcụUI dừng/localhosttimeout nhưđãghi; khôngchụpảnhngoàiphạmgiao diện.

Bổ sung nhỏ T046 sau reviewUI bằngmã: danh sáchquáhạn cóthểtrảviệcquanh nhiều ngày nên chỉHH:mmkhôngđủphânbiệt. Thêm assertionngàydựkiến đầyđủ dùngformatDateTime chung;005-p7-web-date-red.log có1failure/2pass trước cài, sau đó005-p7-web-date-green.log3/3xanh. Thêm dòngDựkiến dd/MM/yyyyHH:mm từplannedAt đãcótronghợpđồng,khôngthêmfield/API. Sau sửa chạyfullweb lại005-p7-web-all-final2.log:392/392test/72filexanh;lint3/build3exit0. Backendcleanverifyđangchạyđộc lập,mãbackendgiữcốđịnh;773unitđãxanh,chưakếtluậnIT/toànkho.

Bổ sung specs/005-daily-care-tasks/data-model.md: ánh xạ marker/history thay các cột mốcnhắc logic, cách tính reminderCount, details người trước/sau/căn cứbàngiao, giữ timestamps/carryover schema đãcó. Khôngtạomigration hoặcchỉnhbảngchung. Đây là đồngbộ môhìnhvậtlývới bảnsửathựctế, khôngđổi nghiệpvụ.

## Kết quả cuối lượt Phase 7 — 02/11/2026

Lệnh cuối toàn backend `.\mvnw.cmd clean verify` chạy nền, log `005-p7-clean-verify-final2.log`, kết thúc **exit1 sau12:05phút lúc11:56:04 giờ Việt Nam**. Mã backend giữ cố định từ trước lệnh đến khi kết thúc. Kết quả: **773unit/Arch xanh;1730IT có9failure+36error,0skip;Spotless2402file sạch**. Không có test bị tắt/bỏ/giảmassertion.

Care toàn kho **223IT:0failure+1error**; lỗi duy nhất là `FR_003_CarePlanLifecycleIT.FR_003_maximumEscapedReturnReasonSurvivesOutboxStorage`, outbox reason đãchờA từPhase3. **17ITPhase7 đều xanh**, `FR_052_OverdueBatchCapacityTest` cũng xanh trong773unit. T003_MergedApiCompatibilityTest trướcđỏ do@Primary đã xanh ởfullrun sau sửa@Fallback. Hai findingreview độc lập cótestRED→GREEN và đều qua ởfullrun; không nhận reviewtĩnh banđầu làm bằngchứng đãreview lại bản sửa.

Đã đọc XML toàn bộ lỗi và lưu `005-p7-final-verification-summary.json` trước chạydiagnostic. So với18tênphươngthứcbaselinePhase6: **cả18vẫnlỗi;thêm27phươngthức củaAssignmentIT**, khôngphải18lỗicũđơnlẻ. AssignmentIT có31test/2failure+25error (4ca còn lại xanh). Không quy27ca này cho baseline hoặc nói toàn backend xanh.

Diagnostic không sửa nguồn/test E: chạy `.\mvnw.cmd '-Dit.test=AssignmentIT#FR_025_draftDoesNotGrantScopeAndPublishedAssignmentDoes' failsafe:integration-test failsafe:verify` với cùngJDK25/DBenv. Log `005-p7-assignment-diagnostic.log` exit1/29.241giây,1test/1error PermissionDenied. Stacktrace dừng ở RequiresPermissionAspect trước AssignmentService.create, chưa tới callback care. Đọc AssignmentIT.setup thấy tạo current bằng TestUsers.staff trước rồi mới clock.setTo(2026-11-01T01:00Z); TestUsers.assignRole ghi effective_from=BusinessTime.now−1ngày, PermissionService đọc roles.current theo đồng hồ hiện hành. Ngày02/11 lúcchạy có thể khiến quyền tạo trước clockset còn ởtươnglai so với01/11. Đây là suy luận nguyênnhân từsource và lỗiquyền đãtáihiệnriêng; cần E xácnhận/sửa fixture, không tựsửaTestUsers/common hoặc testE. XML AssignmentIT fullrun được giữ riêng ở `005-p7-full-AssignmentIT.xml`; diagnostic ghi đè report class tại target bằng1case nên tổngfullrun phải đọc từlogfinal2/JSONsnapshot đãlưu, không cộng lại reporttarget sau diagnostic.

27tên phươngthức mới so baseline:
- `AssignmentIT.BR_M09_16_locationReferencesIncludeDraftAssignmentsAndCurrentFloorManager`
- `AssignmentIT.BR_M09_17_noShiftDoesNotLockAccountOrEraseFloorManagerHistory`
- `AssignmentIT.BR_M15_05_temporaryLoginLockDoesNotCancelTheEmployeesEligibilityForAssignments`
- `AssignmentIT.FR_016_invalidDraftCanBeExplicitlyRemovedDuringPublication`
- `AssignmentIT.FR_024_floorManagerAbsenceAndSubstitutionAreEvaluatedAtTheCurrentTime`
- `AssignmentIT.FR_024_floorManagerIntervalsAreExclusiveAndTemporaryReplacementStartsNextDay`
- `AssignmentIT.FR_024_twoConcurrentFloorManagerCommandsOnlyOneSucceeds`
- `AssignmentIT.FR_025_deletingDraftShiftAlsoDeletesItsDraftAssignments`
- `AssignmentIT.FR_025_draftDoesNotGrantScopeAndPublishedAssignmentDoes`
- `AssignmentIT.FR_025_publicationActivatesDraftsAndSnapshotsAllAssignments`
- `AssignmentIT.FR_025_removingDraftMemberAlsoDeletesTheirDraftAssignments`
- `AssignmentIT.FR_026_assignmentRecipientsFollowRoomMovesWithoutRecreatingAssignments`
- `AssignmentIT.FR_026_finalStatusEndsDirectAssignmentsAndPreservesTheirHistoricalScope`
- `AssignmentIT.FR_027_cancelRequiresReasonAndCannotBeRepeated`
- `AssignmentIT.FR_027_earlyEndIsAtomicWithTaskEffectAndStopsScopeImmediately`
- `AssignmentIT.FR_028_CareSpiReturnsOnlyTheMostSpecificPrimaryForEachRole`
- `AssignmentIT.FR_028_concurrentPrimaryCommandsOnlyOneSucceeds`
- `AssignmentIT.FR_028_duplicatePrimaryIsRejectedButSupportIsUnlimited`
- `AssignmentIT.FR_028_previewAlsoDetectsOverlappingPrimaryInAnotherShift`
- `AssignmentIT.FR_028a_nurseCanDoCaregiverWorkButCaregiverCannotDoNursing`
- `AssignmentIT.FR_029_remindsOnceOnPublicationAndOnceAtShiftStart`
- `AssignmentIT.FR_030_assignedNurseUsesTheConfiguredMedicationLicenseAndFeedsAllThreeSpis`
- `AssignmentIT.FR_030_missingCertificateOrCertificateExpiringDuringIntervalIsRejected`
- `AssignmentIT.FR_031_unrestrictedPrimaryPreviewWarnsBeforeAnyWrite`
- `AssignmentIT.FR_032_copyRechecksEveryRowAndReturnsSkipReasons`
- `AssignmentIT.FR_037a_newPrimaryCannotOverwriteTheIntervalOfAnEndedAssignment`
- `AssignmentIT.SC_005_twoHundredCredentialAndAssignmentChecksNeverAcceptMissingRequirements`

Webcuối **392/392test/72file**, lint/build exit0 sau bổ sung ngày dựkiến đầyđủ (005-p7-web-all-final2.log,005-p7-web-lint3.log,005-p7-web-build3.log). Mobile **68/68fulltest**,4/4scoped cuối, analyzer No issues found. Không có adapterproduction/shellmobile mới, khôngclaim offline hoặc nguồn thật đãtíchhợp. Trựcquan chưađượcxácminh bằngảnh: IABlocalhosttimeout, Computer UseWindows dừng vì khôngxácđịnhchắcURLChrome; đãdừngUI,xóafixurepreviewtạm,dừngVite5177.

Tất cả log/JSON/script chẩnđoán ở `C:/Users/nguye/.codex/visualizations/2026/11/02/01a19a62-9c6c-7bb3-8b5d-5b150f6ba35e/`. Các thư mục backend/target,frontend/dist,mobile/.dart_tool là đầu ra tựđộng của lệnhkiểm, không phải mãsảnphẩm mới. Gitdiff nội dung chỉ chạm phầnC/005 và dòngshell/menu đãđượcquyđịnhchophép; không đổi migration/commonAPI/thưviện/spec/nghiệpvụ. git diff --check sạch. Không commit/push/PR,không đổi nhánh.

**Chưa đóng Phase7,giữT042–T046[ ].** Ngoài gatefullverify18cũ+27Assignment mới, cònpayloadgộplớn chờA,ShiftStaffPort/projection008/claimtồn chờE,cảnhbáovịtrí+metadatađobịbỏ chờD,mobiletransport/shell chờA,callbackT038chưacài vàtrựcquanchưađạt. TiếpđúngthứtựT042→T043→T044→T045→T046 nhưmụcviệccònởtrên;sauđógatecleanverifyvàreviewchủphầnnền trướccheckbox. Chưa bắtđầuT047Phase8.

Checkout cuối: nhánh feature/005-daily-care-and-hand-over, HEAD 8a145b58. Working-history.md cósẵnđầuphiênvàđượcgiữnguyênnộidungcũ,chỉnốilịch sửphiênnày.


## Tiếp nhận Phase 8 Spec005 — 02/11/2026

Yêu cầu thực hiện T047–T049, giữ nguyên kết quả/gatePhase7, testRED trước code, kiểm riêng+Arch sau task, cuối clean verify nền; không UI/screenshot/thưviện/commit/push. Đọc lại AGENTS/team-plan/tasks/plan/backendREADME; tra C11, FR039/040/042 và nghiệpvụ BRM0408/09/11 khi cần. Checkout đầu lượt chỉ có Working-history.md và hand-over-task-003.md untracked; các thay đổi Phase7 đã có trongHEAD, không tự commit lượt trước. Nhánh hiện tại vẫn feature/005-daily-care-and-hand-over. Working-history đã tồn tại, chỉ nối thêm.

Đầu phiên đã báo đủ31dòng trực tiếp cóHungNX-nguyen tại team-plan mục8:101,102,181,184,188,227–252. Nội dung31dòng được lưu nguyên văn ngay sau đoạn này. Không xóa gate cũ chưa đạt.

Chủ dự án chọn“1”: VOID bữa ăn được bỏ qua, không cắt chuỗi; chỉnh mô tảT048 cho đúng quyết định này, còn mood thiếu ngày vẫn cắtchuỗi. Đã hỏi riêng EATING phát sinh thiếumealId để không bịa nguồn011; phần nước/mood/bữa cóMEAL tiếp tục độc lập. T047 tạo3testclass reflection trước production để RED do thiếu rule thật; dùng mã kết quả tiếngAnh hiện có ATE_ALL/MOST/SOME/NOT_EATEN/SKIPPED.

| Người | Việc | Phụ thuộc | Căn cứ | Trạng thái |
| --- | --- | --- | --- | --- |
| A review, C (005, HungNX-nguyen) bàn giao | Review11API/DTO và28event005 mới, fake API riêng; hai dòng đăng ký CareMinuteJob/CareDailyJob trong SchedulerScopeArchTest theo hướng dẫn test. Không đổi APIclock000/runJobs | D006/007/011, E008/012/015/016 và C014 dùng hợp đồng; API có khai báo/fake nhưng chưa có adapter story | 005 T003/T004/T006; glossary11.2; module-interfaces1; planPhase2 | Khai báo/khung/test đã cài tại checkout;27kiểm mới xanh,474unit/kiến trúc xanh trong clean verify cuối2026-10-27;1112IT còn15lỗi/8lớpM01 đã bàn giaoB. Quyền CARE_TASK_CATALOG đã được A công bố; còn cài T007 và review A; T007a đã có mã/test23/23 xanh trong lượt kiểm cuối 2026-10-28; chưa nghiệm thu/commit/push/merge Ghép30/10: ShiftTaskApi giữ đủ chữ ký, haiCarejob đổi String runId và FQN đăng ký; chờ A review SchedulerScopeArchTest. |
| C (005, HungNX-nguyen) | Công bố tám SPI 005: ShiftStaffPort, TaskSourcePort, VitalSignPort, MedicationChecklistPort, AlertReadPort, DayCarePickupPort, QualityCheckPort, TimetableSourcePort; fake duy nhất do 005 sở hữu tại support/fake/care, import FakeM04Config ở test/demo | D (006/007/011), E (008/012), C (014): cài nguồn thật theo contracts mục 2.1; API còn lại/sự kiện chưa nằm trong Phase1 | 005 T002; plan Ghi chú triển khai; glossary11; module-interfaces2.1 | Đã cài ở checkout (2026-10-26); scoped T002 39/39 xanh, 23 ca Phase1 xanh trong clean verify cuối; gate còn 15 lỗi/8 lớp M01 đã bàn giao B (log 005-p1-clean-verify.log). Giữ T001–T002[ ]; chưa nghiệm thu/commit/push/merge, chưa ghi nguồn thật đã tích hợp |
| C (003, HungNX-nguyen) | Bản thật DayCareCapacityApi Phase8: lock → occupiedSessions → checkCapacity cùng transaction; chỗ ở CFG-M03-01, không tự sinh lịch. Gọi port sau khi cấu hình khu nghỉ/buổi | B (004 đăng ký lịch đến), C (005) | 003 T049–T052; plan ghi chú Phase8; test DayCareCapacity/Concurrency | Đã cài trong checkout; scoped xanh, chưa nghiệm thu phase/merge |
| D (007) | Cung cấp bản thật SPI `roombedallocation.api.spi.AllocationInfectionStatus` và kiểu sự kiện công khai/fake cho gỡ khoanh vùng, thêm người vào danh sách tiếp xúc; thống nhất đúng hợp đồng module trước khi003 viết listener. 007 thuộc incident/health theo mục1; checkout hai gói này chỉ có package-info (đính chính tênm07 ở ghi chú trước) | C (003, HungNX-nguyen): T053–T054/FR-024a gỡ dấu chờ và cập nhật khi thêm tiếp xúc | 003 researchG-08, plan Phase9/11; SPI/fake003 đã có | Chờ D;003 không khai hộ kiểu incident/health, không đưa fake vào production |
| C (003, HungNX-nguyen) | Cài facade BedAllocationCommandApi/T033 cho chuyển theo lịch và callback sau chuyển gấp; còn nhánh T032 chuyển từ phân bổ đang có dấu giá (chủ dự án giữ SPI, tạm503/bảo toàn dấu và reminder). Nối nguồn giá hợp đồng trước khi giải quyết nhánh này; writer gỡ/đổi AWAITING_REQUEST/AWAITING_SAME_PRICE phải giữ đúng marker trong transaction | 003 T032/T033/T057–T059; B (004) tích hợp lịch | 003 researchG-07/G-09; plan T029/T032 2026-10-29 | T02915ca/bộ hẹp43IT xanh, clean verify cuối680unit/252IT M03 xanh; T032 một phần, T033 chưa provider, T058 pending vẫn503/rollback; không đổi API hoặc đưa fake vào production |
| A review; C (005, HungNX-nguyen) bàn giao | Review hai dòng đăng ký/lọc 005 trong frontend/scripts/gen-api.mjs (chỉ sinh 23 operation đã dịch), hai đăng ký màn/menu care-plans/care-catalogs và guard riêng M04.RISK_PARTICIPANT/M04.RISK_OPINION trên record_event trong V005_96. API bảng chung và source A giữ nguyên | Web T017 dùng client sinh; trigger khóa con theo trạng thái care_plan, không đổi loại khác trên bảng chung | frontend/README; V005_96; plan Phase3; Working-history.md | Có bản cài/test trong checkout; chờ review chủ phần nền trước merge, chưa commit/push/PR |
| A (000 outbox), C (005, HungNX-nguyen) bàn giao/retest | Mở rộng EVENT_PUBLICATION.SERIALIZED_EVENT để chứa đầy đủ JSON CarePlanReturned với reason hợp lệ tối đa2000ký tự. Fixture2000dấu ngoặc kép serialize4233ký tự, vượt VARCHAR(4000); worst-case escape có thể lớn hơn. Chủ dự án duyệt event reason nhưng chọn chờ A mở rộng outbox, không duyệt C sửa schema. Không cắt lý do/giảm hợp đồng; C giữ ca hồi quy MySQL và retest sau khi A cung cấp migration | 005 T011/T014 FR-003: lệnh Trả lại và thông báo HEALTH không được rollback vì payload hợp lệ; không merge phần reason trước khi test lưu tối đa xanh | 000 V000_05; 005 module-interfaces; NFR_04_CareEventsTest và FR_003_CarePlanLifecycleIT; Working-history.md | Chờ A; C chưa tạo migration, chưa nghiệm thu reason; các lỗi gate nền A/B giữ nguyên |
| E (008), C (005, HungNX-nguyen) bàn giao/retest | Cài ShiftStaffPort thật, bổ sung members(shiftId) đã được chủ dự án duyệt; chốt nguồn liệt kê đầy đủ ca đang diễn ra cho nhắc ca chưa có công việc, kể cả tầng/khu chưa có giường. Không thêm ongoingShifts trong lượt C theo lựa chọn hoãn của chủ dự án | 005 T022–T025; T024 nhắc ca rỗng chưa cài | 005 phase4-contract-proposal.md mục3/5/7; contracts/module-interfaces.md | Chờ E; fake/test không đại diện nguồn thật |
| D (007/011), C (014/005, HungNX-nguyen) bàn giao | Review và cài TaskSourcePort.sources(residentId,from,to) đã duyệt, giữ chữ ký cũ; mỗi nguồn chỉ trả managedTypes của mình trong [from,to). Thống nhất cảnh báo MANDATORY gắn vị trí với 007 vì AlertRequested bắt buộc residentId; chủ dự án chọn chờ, không gán id giả | 005 T024/T025; mandatory location overdue hiện503, có thể rollback toàn lượt chạy bù | 005 phase4-contract-proposal.md mục3/7; CareTaskNotices/CareTaskDueStep | Chưa nghiệm thu nguồn thật hoặc cảnh báo vị trí |
| A (mobile nền), C (005, HungNX-nguyen) bàn giao/retest | Cung cấp transport hỗ trợ JSON array request/response, Idempotency-Key và giữ properties.rowErrors. Chủ dự án không cho C sửa common; giữ get/post cũ tương thích. Sau đó C cài adapter production/đăng ký ShiftTaskPlanningScreen | 005 T026/T031; widget/gateway lập lô và điểm danh đã có, chưa nối app | 005 phase4-contract-proposal.md mục6/7; mobile/lib/staff/care_tasks | Chờ A; không sửa common, không dùng gateway fixture trong production |
| A review; C (005, HungNX-nguyen) bàn giao | Review đăng ký màn/menu care-task-planning trong App.jsx/navigation.js, client005 sinh thêm7operation Phase4; dùng GET /shifts/{id}008 hiện có cho roster vị trí. Không đổi transport web dùng chung | 005 T026 web; shared shell A sở hữu | 005 plan Phase4; frontend/src/features/care-tasks/ShiftTaskPlanningPage.jsx | Có bản cài tại checkout; chờ review trước merge, chưa commit/push/PR |
| A (parameter), C (005, HungNX-nguyen) bàn giao log | Xác minh ParameterAsOfIT.US4_8_valueAtPastInstant_isTheValueCurrentThen: expected20M nhưngactual10M. Tái hiện ở hai lượt cleanverify Phase4 của C; không có trong16phươngthức baselinePhase3 được đối chiếu trước đó. C không sửa nguồn/testA, chưa kết luận nguyên nhân | Gate cleanverify toànbackend; không quy mọi lỗi mới cho baseline | backend/target/failsafe-reports; log005-p4-clean-verify-final{,2}.log; Working-history.md | Chờ A xác minh;31ITPhase4 và6ITT008 xanh ở lượt mới |
| A review; C (005, HungNX-nguyen) bàn giao | Review migration V005_94__shared_day_care_attendance.sql mở rộng simple_record theo R-15: cột/index m04, UNIQUE người/ngày và CHECK riêng M04.DAY_CARE_ATTENDANCE. Không sửa common/API shared hoặc tạo bảng attendance riêng | 005 T028, cần giữ ràng buộc schema khi cài T029 | 005 data-model/plan Phase5; FR_012_AttendanceSchemaIT | Bản cài checkpoint tại checkout; chờ review chủ bảng chung trước merge, chưa commit/push/PR |
| B (004), C (005, HungNX-nguyen) chờ tích hợp Phase5 | Công bố API lịch/ngày nghỉ/báo vắng và kiểu sự kiện004 cùng TrangThaiCoMatBanTruPort đã mô tả trong contracts. SPI DayCareCalendarPort/callback005 đã được chủ dự án duyệt; adapter/listener thuộc care sử dụng API/eventadmission để giữ chiều005→004, B không importcare càiSPI hoặc gọi callback trực tiếp gây vòng | 005 T029/T030, nguồn production và trạng thái có mặt cho004 | phase5-contract-proposal.md mục4; researchC-02; contracts/module-interfaces.md | Chờ B cung cấp hợp đồng/types/nguồn; fakecare không đại diện tích hợp thật Khi merge, nếu B/E đã có listener/kiểu sự kiện nguồn tương ứng thì ưu tiên bản B/E thay đường tích hợp tạm005; giữ chữ ký cũ, hợp nhất tránh chạy trùng và bàn giao B/E review. |
| A (000 shared/retention), C (005, HungNX-nguyen) bàn giao Phase5 | Nối retention cho M04.DAY_CARE_ATTENDANCE/M04.ATTENDANCE_RECORD đã gộp vào simple_record/record_event. Provider005 cũ còn trỏ care_day_attendance/care_attendance_record không tồn tại; SharedRowRetention xóa raw theo M01.RESIDENT nhưng CorrectionRetention chỉ kiểm attachment của DataRef được enumerate. Cần enumerate chuỗi điểm danh/đính chính và kiểm hạn trước khi xóa, API xóa đúng loại/dòng shared khi cần; không để correction/chain mồ côi hoặc mất bản gốc khi correction chưa hết hạn | 005 T028/T029 và gate R-15/FR-050; chưa nghiệm thu retention Phase5 | AttendanceStore, CareRetentionType, CareGovernanceStore; ExpiredDataPurge.chain/CorrectionRetention/SharedRowRetention; Working-history.md | Review read-only xác nhận P1; C không SQL bảng chung hoặc gọi purge cả hồ sơ để né API thiếu. Chờ A thống nhất adapter/API; không claim parentM01.RESIDENT đã bảo đảm toàn chuỗi |
| B (004), C (005, HungNX-nguyen) bàn giao Phase5 | Chốt hành vi ngày đổi LOAI_LUU_TRU từ DAY_CARE sang nội trú khi attendance đã có: trạng thái cũ còn được cung cấp/tính phí không, có hủy trạng thái trước đến không, xử lý Có mặt/Đã về thế nào. refreshCalendar hiện checkResident sẽ409 sau đổi, có thể rollback lệnh004 hoặc cả AttendanceClockWork batch; không tự dùng HOLIDAY_CANCELLED cho đổi loại lưu trú | 005 T029 callback đã duyệt, nhánh LOAI_LUU_TRU chưa nghiệm thu | phase5-contract-proposal.md; AttendanceSources.checkResident/AttendanceService.refresh; Working-history.md | Chủ dự án đã chọn chờ thống nhất với B; giữ gate P1 rõ ràng, không coi callback đã đủ mọi nhánh |
| A (000 correction), C (005, HungNX-nguyen) bàn giao Phase5 | Cung cấp cách từ chối loại đính chính module chưa hỗ trợ trước khi tạo approval cáchC ở entrypoint generic000. Endpoint005 đã chặn VOID409 trước CorrectionApi.create, provider chặn khi apply; generic000 vẫn có thể tạo request VOID rồi thất bại khi duyệt. Chủ dự án chọn sửa giờ AMEND_CONTENT trước, VOID chờ chốt nghiệp vụ; mọi thay đổi SPI chung phải được duyệt trước áp dụng | 005 T029/FR-015; không để sinh yêu cầu không thể áp dụng | AttendanceCorrectionProvider/DayCareAttendanceCorrectionController; CorrectionService.create; Working-history.md | Chờ A review contract/capability; không sửa nguồnA trong lượtC |
| A review; C (005, HungNX-nguyen) bàn giao Phase5 web | Review hai dòng App/menu day-care-attendance, client005 sinh thêm4operation đã duyệt. Dùng ClipboardCheck có sẵn trong registry, không thêm icon vào common. Nguồn mobile list còn cần transportA | 005 T031, phần shell/generatorA sở hữu | DayCareAttendancePage; navigation.js/App.jsx; api005 sinh từcontract | Web374test/69file,lint/build xanh trước sửa YAML cuối; api005 SHA256 giữ nguyên sau sửa/regen; chưa commit/push/PR |
| E (012), C (005, HungNX-nguyen) chờ công thức Phase6 | Chốt quy đổi mức ăn thành averageEatingRatio và mẫu số ăn/nước trung bình (theo ngày hay lần ghi) cho PublicCareResultApi.summary; nghiệp vụ hiện chỉ liệt kê năm mức ăn. Chủ dự án đã chọn chờ thống nhất với E, không tự gán tỷ lệ | 005 T039; 012 FR-052 tổng hợp bản tin | 005 phase6-contract-proposal.md; module-interfaces 1.7; plan Phase6 | Chưa cài công thức, không trả số giả hoặc coi Optional.empty là không có dữ liệu khi thực tế thiếu quy tắc |
| A (002 offline), C (005, HungNX-nguyen) chờ hợp đồng Phase6 | Chốt content hai recordType M04.KET_QUA_CONG_VIEC/M04.GHI_NHAN_PHAT_SINH trong /sync. Chủ dự án không duyệt mục4 đề nghị của C và chọn chờ A thống nhất; không đổi envelope/OfflineHandler, không tạo handler theo payload chưa duyệt | 005 T034/T037, T040 ghi ngoại tuyến | 005 phase6-contract-proposal.md mục4; C-14; plan Phase6 | Chờ A; phần trực tuyến tiếp tục, không coi hàng đợi đã tích hợp 005 |
| D (007), C (005, HungNX-nguyen) chờ nghiệp vụ Phase6 | Thống nhất Ghi nhận phát sinh đo chỉ số khi nguồn trả AWAITING_REMEASUREMENT: FR025 yêu cầu task Hoàn thành ngay nhưng FR041 cấm đóng trước xác nhận; endpoint phát sinh chưa có202. Chủ dự án đã chọn chờ D/007 | 005 T036/T039/T040 nhánh đo phát sinh | 005 phase6-contract-proposal.md mục5; VitalSignPort | Nhánh này trả503 trước tạo task; không thêm trạng thái hoặc HTTP202 chưa duyệt |
| E (010), C (005, HungNX-nguyen) chờ quantity Phase6 | Thống nhất BillableCareResultRecorded.quantity theo loại kết quả; chủ dự án chọn chờ E/010, không tự quy đổi ml/phút/giá trị đo thành đơn vị dịch vụ | 005 T033/T036 FR-051, 010 consumer | 005 plan Phase6; phase6-contract-proposal.md | Lệnh billable chặn trước lưu, chưa nghiệm thu phát sự kiện tính phí |
| D (007), C (005, HungNX-nguyen) chờ callback Phase6 | Thống nhất metadata callback đo lại khác người/giờ gốc cho VitalSignTaskApi.confirmed; chủ dự án chọn tiếp tục chờ D/007, giữ chữ ký cũ | 005 T036/T039, 007 callback đo lại | 005 contracts/module-interfaces.md; plan Phase6 | Callback cùng người/giờ gốc có test; khác metadata trả503, không chép giá trị đo đầu thành giá trị đã xác nhận |
| A review, C (005, HungNX-nguyen) bàn giao Phase6 | Review App.jsx chuyển care-tasks sang API thật, client api005 sinh lại, FK V005_100 tới catalog_item và nối CareChecklistScreen(repository,accountId,shiftId) vào shell; xử lý cache.clearAccount khi đăng xuất/xóa quyền; transport mảng và Problem properties vẫn chờ A như Phase4 | 005 T035/T040/T041 | 005 plan; mobile staff/care_tasks | Không sửa common, pubspec hoặc tự nối ca giả; full mobile/offline chưa hoàn tất |
| E (008), C (005, HungNX-nguyen) chờ claim việc tồn Phase7 | Chốt ca/giờ kiểm người nhận đối với task có carryover: C10 yêu cầu trùng giờ dự kiến, Q93 có người ca sau nhận. Chủ dự án chọn chờ E/008, không tự chuyển sang ca hiện tại | 005 T038, Phase7 bàn giao; 008 Q93 | 005 plan/Working-history review Phase6 | Hiển thị tồn đã có; nhận việc tồn ca sau chưa nghiệm thu; allowedCommands phải theo kiểm lệnh thực |
| A (000 kiểm hợp đồng), C (005, HungNX-nguyen) bàn giao Phase6 | Đăng ký/so phần endpoint005 đã dịch trong ApiContractIT chung, giữ pending cho phase chưa cài. Sáu operationPhase6 và CareTask.resultType đã được chủ dự án duyệt; runtime có các đường dẫn mới nhưng CONTRACTS chưa có005 | Gate R-14/clean verify; 005 T039 | api-005.yaml; phase6-contract-proposal.md; common/ApiContractIT | C không sửa test common của A; lỗi đường dẫn tái hiện ở clean verify Phase6, không tắt so hợp đồng hoặc bỏ endpoint thật |
| A review; C (005, HungNX-nguyen) bàn giao Phase7 | Review đăng ký OverdueTasksPage/menu overdue-care-tasks và client005 sinh GET /care-tasks/overdue?floorAreaId đã duyệt; ApiContractIT chung cần nhận thêm endpointPhase7. Mobile OverdueTasksScreen chỉ widget/loader, chờ transport mảng/shellA, không nối fixture vào production | 005 T046, R-14; phần nềnA sở hữu | 005 plan Phase7; Working-history.md | Bản cài tại checkout; chưa commit/push/PR; chưa nghiệm thu hình ảnh vì Computer Use dừng do không xác định chắc URL |
| E (008), C (005, HungNX-nguyen) bàn giao Phase7 | Review CareTaskHandover/Queries/Projection với bean thật ShiftStaffPort; TaskHandoverView tương thích008 có residentId primitive không biểu diễn task vị trí, cần thống nhất dùng HandoverTaskApi Optional đã có/adapter mà không tự đổi chữ ký cũ. Năm callback vắng/đổi phân công T038 hiện503, không coi toàn ShiftTaskApi đã cài; claim tồn vẫn chờ quyết định dòngPhase6/7 ở trên | 005 T043/T045/FR046–047a và008 handover | 005 contracts/module-interfaces; plan Phase7; FR_047_*IT | C cài luồng bàn giao có hồi quy, chưa nghiệm thu tích hợp thật hoặc nguồn vị trí đủ |
| A (000 outbox/009), C (005, HungNX-nguyen) chờ payload gộp Phase7 | Chủ dự án chọn chờA chốt payload gộp an toàn cho FR052: một nhóm người nhận nhiều task không được chia nhiều thông báo tráiFR052; outbox4000 và quy định không đưa danh sáchid dài. Guard kỹ thuật giữ nhóm lớn chờ nguồn/hợp đồng, không tự mở rộng schema hoặc đổi event/API | 005 T042/T044 chạy bù, NFR04/FR052 | AGENTS.md outbox; planPhase7; FR_052_OverdueBatchCapacityTest | ChờA; không nghiệm thu gộp không giới hạn hoặc đánh[x]T044 |
| D (007), C (005, HungNX-nguyen) bàn giao metadata đo bị bỏ Phase7 | Cung cấp/thống nhất nguồn metadata measurementScheduleId/vitalSignIds cho MeasurementTaskMissed khi task MEASUREMENT còn OVERDUE lúc hết ca. SPI đọc nguồn005 hiện chỉ SourceView(type,id,at,description), không suy id nguồn hoặc tự khai kiểuD; MANDATORY vị trí vẫn chờ dòngD cũ | 005 T044/FR044; 007 FR039b/Q75 | 005 contracts1.11; VitalSignPort/TaskSourcePort; Working-history.md | Chưa phát payload suy đoán, chưa nghiệm thu nhánh bỏ lỡ lần đo hết ca |
| E (008), C (005, HungNX-nguyen) bàn giao log Phase7 | Xác minh27ca AssignmentIT mới đỏ so18baselinePhase6: full31test có2failure+25error; chạy riêng FR_025_draftDoesNotGrantScopeAndPublishedAssignmentDoes vẫn PermissionDenied trước callbackcare. Fixture setup tạo TestUsers.staff rồi lùi clock về2026-11-01T01:00Z, trong khi quyềnTestUsers effective_from=now−1ngày; ngày02/11 có thể làm quyền nằm tươnglai. C không sửa testE/TestUsers hoặc giảm assertion | Gate cleanverifyPhase7; không quy27ca vào baseline | 005-p7-clean-verify-final2.log;005-p7-assignment-diagnostic.log;005-p7-full-AssignmentIT.xml;Working-history.md | ChờE xácnhận timefixture/nguyênnhân;17ITPhase7 xanh, chưa đónggate toànkho |

### Triển khai T047–T049 và review Phase 8 — 02/11/2026

Phạm vi thực hiện: ba quy tắc từ kết quả (FR-039, FR-040, FR-042), projection hiện hành, điểm nối ghi nhận/đính chính và bước kiểm tra nước theo phút. Giữ nguyên chữ ký HTTP/SPI/event và thư viện; không sửa UI, không chụp màn hình, không tạo migration, không commit/push/đổi nhánh. Các gate Phase 7 vẫn còn.

Quyết định nghiệp vụ đã nhận: (1) kết quả ăn VOID bị bỏ qua, không ngắt chuỗi bữa; ngày thiếu tâm trạng/hành vi vẫn ngắt chuỗi ngày; (2) giữ gate EATING phát sinh chưa có mealId thật, chờ D/011, tiếp tục bữa MEAL/nước/tâm trạng. Không lấy taskId làm mealId. tasks.md sửa câu chữ T048 theo quyết định (1), không sửa spec.md hoặc tài liệu nghiệp vụ.

Trình tự thực tế:

1. Đọc tasks.md/plan.md và các nguồn/API cần dùng; kiểm ownership, README tầng; báo 31 dòng mục 8 có HungNX-nguyen ở đầu phiên, chép bằng chứng trên đây. Giữ checkout sẵn có; Working-history.md và hand-over-task-003.md đã tồn tại dạng untracked khi bắt đầu.
2. T047: viết ba test hàm thuần, chạy RED thật do thiếu ba class. Sau đó cài ba hàm domain, kiểm scoped GREEN 43 test (3 unit mới + 40 ArchTest), BUILD SUCCESS 52.155 giây. Các hàm dùng ngưỡng làm tham số, không cố định CFG nghiệp vụ.
3. T048: viết FR_039_CareResultRulesIT trên MySQL. Lần đầu lỗi fixture permissionBasis ROLE không thuộc enum, chưa coi là RED nghiệp vụ; sửa fixture thành ASSIGNED. Lần RED đúng: 6 IT có 1 failure (lệnh ghi bữa thứ ba chưa phát event) và 5 error thiếu bean careResultRules. ArchTest xanh.
4. T049: cài CurrentCareResults/CareResultRules/CareWaterCheckStep; nối CareResultService.recordedEvents; đọc CorrectionApi.current và CFG lúc chạy. Bộ ban đầu GREEN 43 unit/Arch + 6 IT, BUILD SUCCESS 1:24 phút.
5. Dùng skill requesting-code-review cho một agent chỉ đọc; agent không sửa file hoặc chạy Maven. Review phát hiện gate EATING lịch sử quá rộng, eventKey chuỗi thay đổi bị trùng, chạy bù bỏ sót người kết thúc lưu trú sau mốc, và projection đính chính không kiểm giá trị hợp lệ. Viết test tái hiện trước khi sửa các lỗi có RED; bổ sung kiểm bữa ghi muộn, hook CorrectionMade, tâm trạng ba ngày, projection sai, tỷ lệ hiện diện và mốc nước.
6. Log review-red: 9 IT có 2 error, trong đó 1 fixture thử UPDATE task đã đóng bị trigger TASK_CLOSED chặn đúng. Sửa helper fixture cho phép truyền plannedAt ngay lúc INSERT, không gỡ trigger hoặc sửa bản ghi kết quả đã đóng. Error còn lại là gate lịch sử EATING thật.
7. Log review-red2: test chạy bù RED do không gọi water cho resident đã kết thúc lưu trú. Sau sửa tập ứng viên sang mọi trạng thái, lịch sử hiện diện vẫn quyết định tại mốc. Log review-red3: 41 unit/Arch xanh; 11 IT có 2 failure (eventKey, mã ăn đính chính lạ) + 1 error gate lịch sử. Sửa gate chỉ trên chuỗi kém cần phát; bữa tốt ngắt chuỗi không đòi mealId; validate mã ăn; đưa digest chuỗi vào context milestone.
8. Log projection-red: 13 IT có 1 failure do numericValue đính chính âm được nhận; bổ sung kiểm số nguyên dương trước marker. Lần này Spotless còn lỗi vì test mới được bổ sung trong khi lượt kiểm đang chạy; đã định dạng lại ở lượt scoped-final. Bổ sung kiểm mã tâm trạng hiện hành tồn tại trong danh mục. Không coi lỗi format là RED nghiệp vụ.
9. Review lại chỉ đọc xác nhận không còn P1/P2 cụ thể ngoài các gate đã ghi. Scoped-final và clean verify cuối được ghi kết quả ở mục kế tiếp sau khi lệnh kết thúc; không ghi trước kết quả hoặc đánh [x] sớm.

Danh sách file mã nguồn tạo mới (đường dẫn tính từ thư mục gốc):

| File | Mục đích |
| --- | --- |
| backend/src/main/java/com/eldercare/backend/care/internal/rules/domain/WaterIntakeRule.java | floor mục tiêu ml theo phút hiện diện và so sánh dưới tỷ lệ, bằng ngưỡng không cảnh báo |
| backend/src/main/java/com/eldercare/backend/care/internal/rules/domain/PoorMealStreak.java | đếm liên tiếp SOME/NOT_EATEN/SKIPPED từ bữa mới nhất |
| backend/src/main/java/com/eldercare/backend/care/internal/rules/domain/NegativeMoodStreak.java | đếm ngày dương lịch tiêu cực liên tiếp, ngày thiếu ngắt chuỗi |
| backend/src/main/java/com/eldercare/backend/care/internal/rules/service/CurrentCareResults.java | đọc gốc + đính chính theo lô, bỏ VOID, kèm metadata task và sắp plannedAt/id giảm dần |
| backend/src/main/java/com/eldercare/backend/care/internal/rules/service/CareResultRules.java | điều phối quy tắc, khóa resident, CFG, marker, AlertRequested/ProlongedPoorEating; lắng nghe CorrectionMade |
| backend/src/main/java/com/eldercare/backend/care/internal/rules/service/CareWaterCheckStep.java | đăng ký CareMinuteWork hiện có; từng resident một transaction, tiếp tục người khác khi một người lỗi, gom lỗi cuối lượt |
| backend/src/test/java/com/eldercare/backend/care/BR_M04_08_WaterIntakeRuleTest.java | test T047 nước, làm tròn và biên tỷ lệ |
| backend/src/test/java/com/eldercare/backend/care/BR_M04_09_PoorMealStreakTest.java | test T047 chuỗi mã ăn |
| backend/src/test/java/com/eldercare/backend/care/BR_M04_11_NegativeMoodStreakTest.java | test T047 liên tiếp ngày, thiếu ngày và một ngày một lần |
| backend/src/test/java/com/eldercare/backend/care/FR_039_CareResultRulesIT.java | test MySQL projection hiện hành, VOID, marker, lệnh HTTP, hook correction, ghi muộn, eventKey và validation |
| backend/src/test/java/com/eldercare/backend/care/FR_052_WaterCatchUpTest.java | test ứng viên chạy bù có người đã kết thúc lưu trú sau mốc |

File sửa/bổ sung tài liệu:

| File | Thay đổi/mục đích |
| --- | --- |
| backend/src/main/java/com/eldercare/backend/care/internal/result/service/CareResultService.java | inject CareResultRules và gọi onResult sau khi ghi thành công, cùng transaction |
| specs/005-daily-care-tasks/tasks.md | T048 bỏ VOID ăn thay vì ngắt chuỗi theo quyết định đã nhận; chưa đánh checkbox |
| specs/005-daily-care-tasks/plan.md | ghi quyết định nghiệp vụ, tầng thực tế, CFG/projection/transaction/marker/chạy bù, giới hạn và bằng chứng kiểm |
| specs/005-daily-care-tasks/data-model.md | ánh xạ vật lý Phase8 vào care_task/reminder_marker, không thêm schema |
| specs/000-shared-foundation/glossary.md | chỉ thêm mục 11.11 của 005: tên class, loại cảnh báo và marker |
| docs/team-plan.md | thêm ba dòng chờ D/011 mealId, A/D payload chuỗi dài, D review consumer; không xóa gate cũ chưa xong |
| docs/progress.md | sẽ thêm dòng tiến độ sau kết quả kiểm cuối, giữ mọi dòng cũ |
| Working-history.md | bổ sung lịch sử tiếng Việt này, giữ nội dung các phase cũ |

Không xóa file. Không sửa file untracked specs/003-room-bed-allocation/hand-over-task-003.md của phần công việc trước.

Luồng dữ liệu và quyết định kỹ thuật:

- Lệnh ghi nhận → CareResultStore.complete trên care_task → CareResultService.recordedEvents → CareResultRules.onResult. Hook CorrectionMade chỉ chạy khi correction đã áp dụng; không đăng ký listener cho yêu cầu chưa được duyệt. M04.CARE_RESULT provider tạo/duyệt correction là T051 Phase9, chưa có trong Phase8.
- CareResultRules khóa resident qua CareLockStore → CurrentCareResults → CareResultStore.history + CorrectionApi.current + CareTaskStore.get → bỏ VOID → quy tắc thuần → marker và publication cùng transaction. Projection map theo convention đang có của PublicCareResults; không tự đặt payload correction mới. Dùng lại persistence hiện có, không tạo SQL adapter trùng; truy vấn metadata theo từng task chưa được nghiệm thu hiệu năng lịch sử lớn.
- Ăn: xếp plannedAt giảm dần, không dùng thời điểm ghi muộn thay thứ tự bữa. Mã tốt ATE_ALL/MOST ngắt chuỗi; thiếu/VOID bỏ. Chuỗi đủ CFG-M04-04 mới cần mealIds thật. Chuỗi cần phát chứa EATING chưa liên kết hoặc trên32 bữa giữ503 trước marker/event. Không cắt id hoặc chia payload làm thay đổi nghĩa; guard32 là giới hạn kỹ thuật chờ A/D, không phải quy tắc nghiệp vụ. SHA-256 thành phần chuỗi dùng cho marker và milestone để chống lặp đúng cả khi đính chính mở rộng chuỗi.
- Tâm trạng: danh mục MOOD (kể cả giá trị đã ngừng dùng) quyết định nhóm NEGATIVE; BEHAVIOR đọc jsonValue.occurred. Đếm ngày có ít nhất một kết quả tiêu cực/bất thường, dừng ở ngày thiếu; mốc cuối là ngày performedAt của kết quả kích hoạt. CFG-M04-05 đọc mỗi lần; marker và milestone cùng ngày:độ dài. Không tạo hoạt động hoặc công việc.
- Nước: CareMinuteWork → CareWaterCheckStep → water mỗi resident, chỉ chạy bù ngày hiện tại sau giờ CFG-M04-03. Plan hiệu lực cung cấp waterTargetMl và itemId thật. Nội trú đọc các khoảng IN_RESIDENCE, hợp nhất khoảng chồng nhau; bán trú đọc arrivedAt/leftAt. Chỉ người có mặt tại mốc được xét; tính phút từ00:00, floor mục tiêu điều chỉnh, cộng ML hiện hành tới mốc (gồm phát sinh), cảnh báo LOW nếu dưới tỷ lệ. Marker resident/ngày lưu cả khi không thiếu, chưa tới mốc hoặc dữ liệu lỗi không tiêu thụ marker. Ứng viên gồm trạng thái cuối để không bỏ người rời viện sau mốc.
- Người nhận dùng AssignedNurse và Substitution.Default trong context, không đưa danh sách người nhận dài. Chỉ phát payload sẵn có AlertRequested/ProlongedPoorEating; chưa nghiệm thu consumer007/011 thật. Tên mới ở glossary; không sửa API công khai.

Lệnh kiểm (thư mục backend, JDK25 JAVA_HOME=E:/JAVA/JDK_GE/INS_25; nạp .env vào Process, không ghi giá trị bí mật):

- `./mvnw.cmd -Dtest=BR_M04_08_WaterIntakeRuleTest,BR_M04_09_PoorMealStreakTest,BR_M04_11_NegativeMoodStreakTest,*ArchTest test`: RED rồi GREEN nêu trên (log t047-red/t047-green).
- `./mvnw.cmd -Dtest=*ArchTest -Dit.test=FR_039_CareResultRulesIT verify`: RED T048 nêu trên; sau cài có lượt GREEN ban đầu. Các lượt review chỉ thêm test của quy tắc Phase8 và ArchTest, không mở rộng chạy test module khác riêng lẻ.
- `./mvnw.cmd -DspotlessFiles=.*care.* spotless:apply`: chạy sau sửa Java; mẫu là regex Maven, không dùng glob; kiểm git status không có sửa Java ngoài care.
- Scoped cuối: `./mvnw.cmd -Dtest=BR_M04_08_WaterIntakeRuleTest,BR_M04_09_PoorMealStreakTest,BR_M04_11_NegativeMoodStreakTest,FR_052_WaterCatchUpTest,*ArchTest -Dit.test=FR_039_CareResultRulesIT verify`. Kết quả cuối ghi bên dưới.

Log lưu ngoài kho tại C:/Users/nguye/.codex/visualizations/2026/11/02/01a19a62-9c6c-7bb3-8b5d-5b150f6ba35e/005-p8-*.log. Không thêm log chứa cấu hình môi trường vào repo.

### Kết quả scoped cuối Phase 8 và kiểm toàn kho đang chạy

`005-p8-scoped-final.log`: BUILD SUCCESS, exit0, 1:36 phút; 44 unit/ArchTest và 14 FR_039_CareResultRulesIT đều xanh, 0 failure/error/skip; Spotless2413 file sạch. Ca unknownCorrectedMoodCode được thêm trước validation và xanh trong scoped cuối; không ghi nhận ca này đã đỏ riêng (lượt projection-red trước đó mới chạy13 ca, RED numericValue âm). Ca nước kiểm số nguyên dương bao gồm âm và phần lẻ sau sửa; marker vẫn cho phép thử lại với projection đúng.

Đã khởi chạy `./mvnw.cmd clean verify` ở nền trong backend; log005-p8-clean-verify-final.log, không bật cửa sổ hoặc in .env. Unit/kiến trúc toàn kho đã kết thúc777 test xanh. Trong lượt full đang chạy,14 IT Phase8 xanh; care237IT có0failure/1error outbox reason đã chờA từ trước. Có2 tên lỗi mới ParameterChangeIT.US4_7_restoreDefault_isAnOrdinaryChange_markedAsRestore và US4_9_staleIfMatch_isRejectedWithCurrentValue_thenSavesAfterReload so với summaryPhase7: DuplicateKeyException trên parameter_history.uq_parameter_history, CFG-M05-01 có trùng effective_from tới microsecond trong từng ca. Chỉ ghi bằng chứng, chưa kết luận nguyên nhân hoặc quy cho Phase8; không sửa common/parameter/testA. Kết quả tổng cuối được bổ sung sau khi lệnh kết thúc.

### Kết quả cuối lượt Phase 8 — 02/11/2026

Lệnh nền `./mvnw.cmd clean verify` đã kết thúc exit1, BUILD FAILURE, tổng11:48phút (không phải đang chờ). Kết quả từ XML/log:

| Nhóm | Test | Failure | Error | Skip |
| --- | ---: | ---: | ---: | ---: |
| Unit/kiến trúc toàn backend | 777 | 0 | 0 | 0 |
| Integration toàn backend | 1744 | 9 | 38 | 0 |
| Riêng care integration | 237 | 0 | 1 | 0 |
| Riêng test mới Phase8 (4unit+14IT) | 18 | 0 | 0 | 0 |

Spotless2413file sạch. Scoped trước full có44unit/Arch+14IT xanh, BUILD SUCCESS1:36phút; full cũng kiểm ModuleBoundaryTest và toàn bộ ArchTest. `git diff --check` sau thay đổi không có lỗi whitespace (chỉ cảnh báo LF→CRLF của Git). Không chạy npm/Flutter hoặc chụp màn vì không đổi UI.

Dùng script ngoài repo005-p8-summarize.ps1 đọc XML, lưu005-p8-final-verification-summary.json cùng thư mục log đã nêu. So với005-p7-final-verification-summary.json: cả45tên lỗi Phase7 còn nguyên; thêm đúng2tên ParameterChangeIT nêu trên, không có tên lỗi care mới. Care1error là reason outbox cũ đã chờA. Không gọi2lỗi mới là baseline hoặc khẳng định do/không do thay đổi Phase8 khi chưa điều tra. team-plan thêm dòng thứ4 bàn giaoA rà hai ca trùng khóa parameter_history; chưa chạy riêng testA, không sửa test/common củaA.

Đã cập nhật plan ghi chú triển khai/kết quả/gates, data-model ánh xạ vật lý, glossary11.11, tasks sửaT048 theo duyệt và checkpoint giữ[ ], team-plan4dòng mới, progress thêm1dòng cuối. Mọi dòng progress và history cũ được giữ. Không xóa hoặc đổi trạng thái dòng chờ nào khi chưa đủ bằng chứng xong. Không thêm file nguồn ngoài care hoặc sửa contracts/api, package/pom, CLAUDE, common, business/spec.

Các vấn đề chưa giải quyết và thứ tự tiếp theo theo tasks.md:

1. T047: ba hàm/test đã có RED→GREEN, vẫn giữ checkbox mở theo gate cuối phase; không cần cài thêm hàm thuần nếu không có thay đổi nghiệp vụ.
2. T048: MySQL/projection/current/VOID/marker đã xanh; cần giữ phạm vi bằng chứng chính xác. Luồng tạo/duyệt correction thật cần providerT051, không được lấy spyCurrentValue thay nghiệm thu lifecycle. Gate fullverify chung còn đỏ.
3. T049: nhánh MEAL có nguồn thật, nước và tâm trạng đã có producer/hook; chờ D/011 chốt mealId cho phát sinh, A/D payload chuỗi dài hơn32 và D review consumer007/011 thật. Chưa nghiệm thu giới hạn không hạn chế, chưa đóngPhase8. Guard nguồn/payload trả503 trước marker/event, không tự bỏ bữa hoặc giảid. Gate Phase7 và lỗi toàn kho cũ giữ nguyên;2ParameterChangeIT mới bàn giaoA.
4. Sau khi xử lý gate và xác minh/đóng T047→T048→T049: task kế tiếp là T050 (test đính chính/trigger/cáchA-B/tính phí) rồi T051 (CareResult correction provider/endpoint và callback), thuộcPhase9. Chưa viết/cài/chạy các taskPhase9 trong lượt này; hợp đồng API chưa dịch/duyệt phải theo quy định hỏi trước khi thay đổi.

Không commit, push, merge, đổi nhánh hoặc tạo PR. Thư mục làm việc vẫn feature/005-daily-care-and-hand-over; file hand-over-task-003.md untracked được giữ nguyên. Đây là checkpoint có mã/test Phase8, không tuyên bố toàn phase đã hoàn tất.

## Tiếp nhận và thực thi Phase 9 Spec 005 — 02/11/2026

Yêu cầu: T050→T051 theo AGENTS.md/team-plan/backendREADME; đọc tasks/plan trước, TDD thật, chỉ test task + ArchTest/ModuleBoundaryTest sau task; clean verify nền cuối trước checkbox; không commit/push/đổi nhánh, không chụp màn khi không đổi UI; bổ sung lịch sử tiếng Việt và giữ mọi nội dung cũ.

Đầu phiên HEADbd646d1e, nhánh feature/005-daily-care-and-hand-over; chỉ Working-history.md và specs/003-room-bed-allocation/hand-over-task-003.md untracked. Mã Phase8 đã ở HEAD do thay đổi ngoài lượt này; không lặp lại cài Phase8. Đã đọc lại luật và báo35dòng có HungNX-nguyen trong mục8 ở commentary:101,102,181,184,188,227–256; bảng tóm tắt từng dòng đã trình bày cho chủ dự án. Không tự xóa dòng chờ chưa có bằng chứng hoàn thành.

Chủ dự án đã chọn1/duyệt hai nội dung:

- Dịch endpoint T051 thành POST /care-results/{id}/correct, body type/correctedContent/reason, type AMEND_CONTENT hoặc VOID, reason chuỗi1–2000ký tự, correctedContent object bắt buộc khi sửa nội dung; giữ Idempotency-Key và201khôngbody. Không áp dụng thay đổi trước khi nhận duyệt.
- VOID kết quả ngủ giữ gate chờ D/007 vì SleepResultRecorded chỉ có valueGroup, chưa có tín hiệu hủy; không tự dùng nhóm rỗng/null. AMEND_CONTENT ngủ và loại khác tiếp tục. Gate quantity tính phí E/010 từPhase6 vẫn giữ, không hỏi lại hoặc suy numericValue thành newQuantity.

Trình tự thực hiện tới checkpoint này:

1. Đọc tasksPhase9/plan notes trước, rồi data-model/contracts/research C09 và nguồn CorrectionApi/CorrectableRecordProvider/CorrectionService hiện có. Kiểm ownership care/spec005 và glossary11. Không tra/sửa spec hoặc nghiệp vụ khi nguồn triển khai đã đủ.
2. Viết BR_M04_14_CareResultCorrectionIT trước production; dùng CareResultTestBase, MySQL và CorrectionApi thật, chỉ nguồn liên module dùng fake chung đã có. 13IT RED thật:12failure do endpoint404 và1error do chưa có provider M04.CARE_RESULT.41Arch/ModuleBoundary xanh. Lượt này còn lỗi Spotless vì bổ sung test contract trong khi verify đang chạy; không dùng format làm RED nghiệp vụ.
3. Viết FR_037_CareResultCorrectionContractTest, chạy RED1failure thiếu đường dẫn đã dịch,41Arch/Boundary xanh; sau đó mới sửa YAML hợp đồng được duyệt. Không sửa signature sự kiện/API Java công khai.
4. Cài CareResultCorrectionProvider/Service, controller/request theo đúng tầng. Provider không phụ thuộc CorrectionApi/CareResultRules để tránh vòng constructor của CorrectionService; lắng nghe CorrectionMade có sẵn tính lạiF sau callback. Type-specific validation trong callback cóoriginalRecordId, rollback khi sai. AMEND ngủ phát SleepResultRecorded có mốc CORRECTION:<id> và ngày plannedAt. Không UPDATE care_task hay tạo schema mới.
5. Lượt t051-green thực tế BUILD FAILURE:42unit/Arch/Boundary xanh,14IT có1failure. Nguyên nhân assertion DELETE chờ trigger nhưng eldercare_app bị SQLSyntaxErrorException DELETE command denied trước trigger; sửa assertion của test mới để kiểm đúng lớp bảo vệ quyền, không cấp thêm quyền, gỡ trigger, bỏ assertion hoặc sửa test của người khác. UPDATE vẫn kiểm TASK_CLOSED; kiểm trạng thái/bản gốc và projection giữ nguyên.
6. Review độc lập theo skill requesting-code-review (agent chỉ đọc, không sửa/chạy Maven) tìmP1: generic000CorrectionController dùng CorrectionApi.create mặc định REPEATABLE_READ, khác endpointcare được bọc READ_COMMITTED. Snapshot có thể hình thành trước khóa resident rồi bỏ sót correction đồng thời khi tínhF. Đang viết/chạy test gate generic trước khi thêm guard; thêm kiểm hai lệnh care cùng resident chạy song song, dùng marker trongDB để xác nhận quy tắc sau cảhai commit. Không mở rộng quyền sửa common củaA.
7. Chạy npm run gen:api sau bản dịch hợp đồng. Generator hiện có sinh lại mọi file, diff nội dung chỉ frontend/src/api/api005.js; không sửa tay file sinh, generator/common/package. Chưa thay màn hình hoặc điều hướng; không có ảnh cần chụp. Kết quả kiểm frontend/cuối phase bổ sung khi lệnh hoàn tất.

Lệnh/log đã có:005-p9-format*.log,005-p9-t050-red.log,005-p9-contract-red.log,005-p9-t051-green.log (tên green nhưng kết quả đỏ như trên),005-p9-isolation-red.log đang chạy. Tất cả ở C:/Users/nguye/.codex/visualizations/2026/11/02/01a19a62-9c6c-7bb3-8b5d-5b150f6ba35e/. JDK25 JAVA_HOME=E:/JAVA/JDK_GE/INS_25, nạp .env vào Process, không in bí mật. Test scoped dùng -Dtest=FR_037_CareResultCorrectionContractTest,*ArchTest,ModuleBoundaryTest -Dit.test=BR_M04_14_CareResultCorrectionIT verify; format dùng -DspotlessFiles=.*care.* spotless:apply. Lượt đầu chạy wrapper với -f backend/pom.xml từroot; các lượt tiếp ởbackend. Chưa ghi kết quả xanh hoặc đánh[x] trước khi xác minh.

### Bổ sung kiểm chứng và sửa review Phase 9

- Web codegen đãexit0; npm test392test/72file xanh (73.24giây),npm run lint vànpm run build exit0 (build4.67giây). Build có cảnh báo chunk lớn sẵn có, không thay cấu hình/dep để giấu cảnh báo. Không có UI thay đổi nên không chụp ảnh.
- Isolation RED:42unit/Arch/Boundary xanh,16IT có1failure ở generic expected503 nhưngactual201. Thêm guard tạm tạiisConfirmed rồi scoped16IT xanh. Tự rà đường đọc, viết ca chain trongouterRR transaction; chạy RED thật1error vì bịguard503. Chuyển guard và khóa sangallows (chỉ lệnh tạo), giữmetadata đọc thuần; scoped-final2 BUILD SUCCESS1:59phút,42unit/Arch/Boundary+17IT xanh.
- Kiểm callback luồngapply chung: đọc CorrectionRequestHandler thấyapply gọicallback mà không quaallows. Viết2unit trước bổ sung: callback cáchC vàcallbackRR đều RED do không ném lỗi. Sau đó thêm denyC403 vàrequireWriteIsolation ởcallback, cùnghelper vớiallows. scoped-final3 BUILD SUCCESS2:14phút,44unit/Arch/Boundary+17IT xanh,0skip. Reviewer đọc lại xác nhận không có P1/P2 cụ thể còn lại trong phạm vi đã rà; generic ghi vẫn làgate, không coi đã hỗ trợ.
- Rà tiếp đườngGETgeneric khi đăngkýprovider: thêm testngười ngoài tầng xemchain. RED thật1failure expected403/actual200: commonCorrectionApi.chain đọc nội dung quaoriginalContent mà chưa có kiểmVIEW củacare. Viết tiếp testHealthVisibility PRESENCE_ONLY trước nối bảo vệ nội dung; lượtchain-health-red đangchạy. Không thay luật quyền hoặc sửa common; dùng checker/queries hiện có củacare để tránh lộ raworiginal/current/corrections quađườnggeneric.

File tạo mới trong lượtPhase9 và mục đích:

| Đường dẫn tính từ gốc project | Mục đích |
| --- | --- |
| backend/src/main/java/com/eldercare/backend/care/internal/result/service/CareResultCorrectionProvider.java | SPI nhóm3 M04.CARE_RESULT, metadata bản gốc, A/B theo quyền/tầng task, validation theo snapshot, guardnguồn/isolation và callbackngủ; không sửa bản gốc |
| backend/src/main/java/com/eldercare/backend/care/internal/result/service/CareResultCorrectionService.java | bọc CorrectionApi.create bằng transactionREAD_COMMITTED, không chép nghiệp vụ000 |
| backend/src/main/java/com/eldercare/backend/care/web/CareResultCorrectionController.java | endpoint đãduyệt, kiểmtype/reason, nhậnIdempotency-Key, trả201khôngbody |
| backend/src/main/java/com/eldercare/backend/care/web/request/CareResultCorrectionWrite.java | requestrecord type/correctedContent/reason, Reason dùng validationchung |
| backend/src/test/java/com/eldercare/backend/care/BR_M04_14_CareResultCorrectionIT.java | MySQL/CorrectionApithật, lifecycleA/B/VOID/quyềntầng, bảogốcbấtbiến, F/sleepcallback, rollback, chạyđồngthời, gatevàquyềnxemchain |
| backend/src/test/java/com/eldercare/backend/care/FR_037_CareResultCorrectionContractTest.java | bản dịch YAML duyệt, đúngpath/3field/2enum/201khôngbody |
| backend/src/test/java/com/eldercare/backend/care/BR_M04_14_CareResultCorrectionProviderTest.java | unitguardcallbackC và isolation; khônggiả làm testlifecycleapproval thật |

File sửa/bổ sung trong lượtPhase9:

| File | Mục đích |
| --- | --- |
| specs/005-daily-care-tasks/contracts/api-005.yaml | dịchduyệt1operation/requestbody, x-implementation-phase9, giữnghĩa/trường/phảnhồi |
| frontend/src/api/api005.js | scriptgen:api thêmhàm postCareResultsByIdCorrect, khôngsửatay |
| specs/005-daily-care-tasks/contracts/module-interfaces.md | ghi provider/type hiệncó, giátrịthaythế, callback/gates; khôngđổi chữký Java |
| specs/005-daily-care-tasks/plan.md | ghi quyếtđịnh/duyệt/gate/isolation/validation/luồng/kiểm chứng |
| specs/005-daily-care-tasks/data-model.md | ánhxạ correction/correction_chain/audit_log chung vàcare_task bấtbiến, khôngthêmschema |
| specs/000-shared-foundation/glossary.md | chỉthêmmục11.12 của005: tênprovider/service/controller/request/endpoint/client |
| docs/team-plan.md | thêmchờA isolation/luồnggeneric vàD tínhiệuVOIDngủ; cậpnhậtphầncònchờ sau kếtquảcuối |
| specs/005-daily-care-tasks/tasks.md | checkpointT050/T051 saukiểmcuối, giữ[]khi gate chưaqua |
| docs/progress.md | thêmkếtquảcuốiphase saukhi cleanverify kếtthúc, khôngsửa dòngcũ |
| Working-history.md | bổsung cáclượttriểnkhai/RED/GREEN/file/luồng/lệnh/gates trongtiếngViệt, khôngxóa nộidungcũ |

Không xóa file. File untracked specs/003-room-bed-allocation/hand-over-task-003.md giữ nguyên. Không sửa backend/common, pom.xml,package.json,pubspec.yaml,CLAUDE.md, business hoặc spec.md. Clientgenerator ghi lại các file nhưnggitdiffnộidung chỉapi005; khôngcoi file khác là thayđổi củaPhase9.

Luồng chính: HTTPcorrect → CareResultCorrectionService(RC) → CorrectionApi.create → provider.isConfirmed/validateContent/metadata/canRecord/allows → khóa resident/task → correction_chain/correction/audit_log của000 → provider.afterCorrection(kiểm typeddata,gate/callbackngủ) → CorrectionMade → CareResultRules/CurrentCareResults/CorrectionApi.current → marker+AlertRequested/ProlongedPoorEating. Toàn bộ trongtransaction; lỗitypeddata/gatengủ rollbackcảchuỗivàaudit, kiểmthậtkhôngoutertesttransaction. Trạngthái,metadata,values gốc trêncare_task khôngđổi; current/chain/PublicCareResults dùngprojection. CáchA gắnrecordedBy; cáchB doScopeApi000 kiểmngườiphụtráchca/trưởngtầng vàtầngtask; provider kiểmCONFIRMhiệnhành. Đo chỉsố trả403 đểđínhchính007, billablegiữ503 trước lưu vìquantitychưa thốngnhất. VOID khôngmở lại hoặc sinh côngviệc; bữaVOID bỏkhỏicáchđếmF theoquyếtđịnhPhase8. Khóa chuỗi/ngàyF giữ theoPhase8, khôngthêm bảngđếm.

### Kết quả scoped cuối và bắt đầu clean verify Phase 9

Hai test quyền xem generic chain đã chạy RED đúng nguyên nhân: ngoài tầng và HEALTH=PRESENCE_ONLY đều nhận200 thay vì403. Sau đó originalContent được nối FieldVisibilityApi (phải FULL) và CareResultQueries.original (TASK_CHECKLIST/VIEW), dùng Target M04.CARE_RESULT/tầng task gốc. Không chỉ kiểm scope rồi trả raw values; toàn bộ chain chỉ được trả sau kiểm quyền/nhóm HEALTH. Provider nhận thêm query/CurrentUser/visibility; unit constructor được cập nhật cho đúng dependency. Reviewer chỉ đọc xác nhận đường đọc không có vòng constructor hoặc P1/P2 cụ thể còn lại.

Lượt005-p9-scoped-final4.log: BUILD SUCCESS, exit0,2:19phút;44unit/ArchTest/ModuleBoundaryTest +19IT đều xanh,0failure/error/skip;Spotless2420file sạch. Trong44 có3unit riêngPhase9 (1contract +2callback), còn41kiểm kiến trúc/ranh giới.19IT dùng CorrectionApi/current/chain thật; chỉ ca HEALTH=PRESENCE_ONLY spy kết quả checker hiện có, không spy lifecycle correction. Các ca A/B, VOID được hỗ trợ, projection/bản gốc, callbackF/ngủ, rollback, hai lệnh song song, readchain trongouterRR và hai ca quyền xem đều qua. Không coi test guard billable/VOIDngủ/generic là nghiệm thu producer/luồng còn bị chặn.

Đã bắt đầu `./mvnw.cmd clean verify` ở nền sau lượt scoped cuối xanh, log005-p9-clean-verify-final.log. Không đánh[x] trước kết quả. Script005-p9-summarize.ps1 ngoài repo sẽ đọc XML sau khi xong, so tên lỗi với005-p8-final-verification-summary.json (47tên lỗi/9failure+38error trướcPhase9) và lưu005-p9-final-verification-summary.json. Chưa ghi kết quả tổng của lệnh đang chạy.

Các quyết định cuối: metadata provider là truy vấn thuần, guard/lệnh khóa chỉ ởallows/callback; entrycare READ_COMMITTED, genericghi DEFAULT/RR trả503 trước khóa/ghi và chờA. Callback không cho cáchC dù đườngapply chung bỏ quaallows; cần A review trước lập request loại module không hỗ trợ. VIEW/HEALTH dùng quy tắc hiện có, không tạo role/quyền mới. Không thêm dịch vụ tầng trung gian để né boundary; service nhỏ có mục đích transactionisolation cụ thể vì không sửa phần nềnA.

### Tiếp tục và hoàn tất điểm nối đọc — 03/11/2026

Tiếp tục theo yêu cầu “tiếp tục”, giữ nguyên nhánh và các thay đổi đang có. Lượt scoped-final5 đã kết thúc ngày02/11: BUILD SUCCESS/exit0,2:13phút,44unit/Arch/Boundary và26IT (20ITPhase9 +6T008_CareGovernanceIT),0skip;Spotless2420file sạch. Không báo trước kết quả khi lượt kiểm còn chạy.

Lượt clean verify đầu005-p9-clean-verify-final.log kết thúc exit1/13:11phút:780unit xanh,1763IT9failure+38error,care256IT0failure+3error;22test riêngPhase9 lúc đó xanh. Đối chiếu JSON vớiPhase8:2tên mới là T008_CareGovernanceIT.FR_050_retentionDoesNotQueryUncreatedStoryTablesAndSeparatesUnlinkedTasks và FR_051_profileAndUnlinkedCandidatesUseDifferentClocksAndRetentionConnection. Cảhai báo FakeResidents chưa có resident5000671 do fixturePhase9 chạyNOT_SUPPORTED để kiểm rollback/đồng thời đã commitcare_task, trong khi test governance dùng fake ởcontext khác. Đây là hồi quy từfixture mới, không gọi làbaseline và không sửa test governance/nuốt lỗi nguồn.

Sửa ở BR_M04_14_CareResultCorrectionIT: AfterEach chỉ chạy khi không có testtransaction; dọn đúng residentId fixture duy nhất. App dọn publication kỹ thuật gắn event/resident/task đó; retentionConnection dọn correction/chain/audit/status_history/marker/care_task/state của đúng fixture. Không dọn bảng toàn cục, sửa quyềnDB, thay dữ liệu resident khác, hoặc sửa CareRetention. Test@Transactional vẫn để frameworkrollback như cũ. Hai test governance bị ảnh hưởng đã chạy cùng suite mới và xanh.

Trong lượt full đầu,2ca ParameterChangeIT từng đỏ ởPhase8 không tái hiện; chưa có sửa nguồnA nên không xem là đã giải quyết. Summary đầu lưu005-p9-first-verification-summary.json; logfull đầu giữ nguyên. Kết quả final phải dùng lượt full mới sau các thay đổi tiếp theo.

Điểm nối API đọc: phát hiện CareResultResponse.of đang cố địnhcorrected/recordingCancelled=false và dùngvalues gốc. Viết testAPI trước; RED thật expected200 nhưngactual2000 tại $.result.currentValue.numericValue. Sau đó:

- CareResultController.history đọc CorrectionApi.current theo lô sau kiểm quyền truy vấn cũ.
- CareTaskController.get đọc current cho kết quả đã có.
- CareResultResponse thêm overload factory nhậnCurrentValue, ánh xạcurrentValue/corrected/recordingCancelled theo trường đã có; giữ metadata/người ghi/trạng thái gốc và HEALTHmask. VớiVOID cócontentnull, giữfallback bản gốc kèm cờhủy giống CorrectionApi.chain; quy tắc/tổng hợp vẫn bỏVOID. Projection malformed trả503, không âm thầm báo giá trị gốc là giá trị hiện hành. Không đổi schema/endpoint/fieldAPI hoặc sửaUI.

Ba file hiện có sửa thêm và mục đích: backend/src/main/java/com/eldercare/backend/care/web/CareResultController.java (lịch sử hiện hành theo lô), backend/src/main/java/com/eldercare/backend/care/web/CareTaskController.java (chi tiết kết quả hiện hành), backend/src/main/java/com/eldercare/backend/care/web/response/CareResultResponse.java (mappingcờ/giá trị vàmask). TestIT có thêmcase mapping/VOID; không thêmfile hoặc dependency cho điểm nối này. Reviewer chỉ đọc kiểm lại projection và cleanup, không có P1/P2 cụ thể mới.

Sau scoped cuối xanh, đã khởi chạy `./mvnw.cmd clean verify` nền lần2 (005-p9-clean-verify-final2.log) ngày03/11 trên mã cuối. Không dùng lượt đầu để đánh[x] cho nguồn đã sửa sau đó. Kết quả tổng lần2 sẽ ghi sau khi tiến trình kết thúc; các gate quantityE/010, VOID ngủD/007, genericghiA và consumer thật vẫn giữ.

### Sửa fixture phụ thuộc giờ chạy và kiểm lại — 03/11/2026

Clean verify lần2 (005-p9-clean-verify-final2.log) kết thúc exit1/15:37phút:780unit xanh,1764IT9failure+40error/0skip.23test riêngPhase9 xanh;6governance xanh, đã hết2hồi quy fixture commit của lượt đầu. Care257IT có5error:1outbox cũ và4ca nước FR_039_CareResultRulesIT mới đỏ khi chạy sau nửađêm. Tên lỗi mới và đối chiếu lưu005-p9-second-verification-summary.json; không gộp các lỗi mới vào baseline.

Root cause được đọc từ SQL exception: ck_care_result_time bị vi phạm. Helper save của testPhase8 mô phỏng kết quả lúc15:00 hômnay, nhưng lấy recordedAt bằng giờ thực chạy khoảng00:xx; performedAt lớn hơnrecordedAt. Sửa duy nhất fixture helper ở backend/src/test/java/com/eldercare/backend/care/FR_039_CareResultRulesIT.java: capturedrecordedAt=max(time.now(),performedAt) theo mốc mô phỏng. Không sửa production, nới constraint, bỏ assertion, tắt test hoặc giả lập rằng lệnh HTTP cho phép ghi tương lai. Case này tạo fixture qua store để gọi quy tắc với thời điểm mô phỏng; kiểm chống ghi tương lai của lệnh production giữ nguyên.

Scoped-final6 BUILD SUCCESS/exit0,2:03phút:44unit/Arch/Boundary +40IT (14Phase8 +20Phase9 +6governance) xanh,0skip;Spotless2420file sạch. Đây là kiểm bổ sung các test liên quan trực tiếp tới hồi quy quan sát được, không chạy riêng test moduleA/E ngoài phạm vi. Sau đó khởi chạy clean verify nền lần3 trên mã cuối, log005-p9-clean-verify-final3.log; đang chờ tổng cuối trước checkpoint. Web không đổi sau lần kiểm392test/lint/build xanh nên không lặp lại.

### Kiểm bảo toàn định danh publication trong cleanup

Clean verify lần3 (005-p9-clean-verify-final3.log) đã kết thúc exit1/14:07phút:780unit xanh,1764IT9failure+36error/0skip;care257IT0failure+1error outbox cũ;23test riêngPhase9 xanh. SoPhase8 không có tên lỗi mới;2ParameterChangeIT không tái hiện trong lượt này.14IT quy tắcPhase8 và6governance đều xanh. Snapshot lưu005-p9-third-verification-summary.json trước khi kiểm tiếp.

Siết nốt an toàn cleanup fixture: ID số có thể trùng giữa task/plan hoặc giữa các recordType. Viết testDB tạo2publication COMPLETED thuộc thực thể khác loại nhưng cùng sốID, rồi gọi cleanup. RED thật expected2 nhưngactual0, chứng minh bộ lọc cũ thiếu loại định danh. Sau đó thêm kiểm context.sourceType thuộc M04.TASK/M04.CARE_RESULT trước so sourceId, và CorrectionMade.recordType=M04.CARE_RESULT trước so originalRecordId. Điều kiện residentId fixture vẫn riêng biệt; phần còn lại dùng đúng loại+ID. Test đối chứng dọn hai UUID fixture trong finally, không để publication giả tồn tại ngoài ca test. Không đổi mã production hoặc xóa event thực thể khác để cho test xanh.

Scoped-final7 BUILD SUCCESS/exit0,2:13phút:44unit/Arch/Boundary +41IT (21Phase9 +14Phase8 +6governance) xanh/0skip,Spotless2420sạch. Reviewer chỉ đọc xác nhận bộ lọc theo loại+ID bảo toàn publication thực thể khác. Do fixture vừa thay đổi, đã chạy clean verify nền lần4 (005-p9-clean-verify-final4.log), kết quả cuối ghi sau khi hoàn tất; không dùng kết quả lần3 để tuyên bố bản cuối đã qua gate.

## Kết quả cuối lượt Phase 9 — 03/11/2026

`./mvnw.cmd clean verify` nền lần4 đã kết thúc exit1, BUILD FAILURE,14:24phút; không còn tiến trình kiểm đang chờ. Bản mã cuối dùng log005-p9-clean-verify-final4.log, không lấy các lượt bị sửa sau đó làm bằng chứng cuối. XML được tổng hợp bằng005-p9-summarize.ps1 và lưu005-p9-final-verification-summary.json trong thư mục artifact đã nêu.

| Nhóm kiểm cuối | Test | Failure | Error | Skip |
| --- | ---: | ---: | ---: | ---: |
| Unit/kiến trúc toàn backend | 780 | 0 | 0 | 0 |
| Integration toàn backend | 1765 | 9 | 36 | 0 |
| Integration riêng care | 258 | 0 | 1 | 0 |
| Riêng Phase9 (3unit+21IT) | 24 | 0 | 0 | 0 |

Spotless2420file sạch.14ITPhase8 và6T008governance đều xanh trong lượt full; các hồi quy fixture mới đã xử lý. So005-p8-final-verification-summary.json:không có tên lỗi mới;45tên lỗi cũ còn nguyên. Hai ca ParameterChangeIT củaPhase8 không tái hiện trong lượt cuối; vì không sửa nguồnA nên chưa coi là đã giải quyết và vẫn giữ dòngreview. Care1error là FR_003_maximumEscapedReturnReasonSurvivesOutboxStorage đã chờ A mở rộng outbox; không cắt lý do hoặc giảm hợp đồng. Các lỗi AssignmentIT/timefixture và các lỗiM01/APIContract đã bàn giao trước vẫn thuộc phần nguồn của chủ tương ứng; không sửa test người khác, bỏ qua assertion hay đổi common để lấy kết quả xanh.

Scoped cuối005-p9-scoped-final7.log:exit0/2:13phút,44unit/Arch/ModuleBoundary+41IT (21Phase9,14Phase8,6governance) xanh/0skip. Web392test/72file,lint/buildexit0,gen:api exit0; diff nội dung client chỉapi005. Không sửa mobile/UI, không chụp ảnh. git diff --check không có lỗi whitespace, chỉ cảnh báo chuyển LF→CRLF của Git. Nhánh vẫn feature/005-daily-care-and-hand-over, HEADbd646d1e; không commit/push/merge/đổi nhánh hoặc tạoPR.

Tài liệu đã cập nhật: tasks.md giữ T050/T051[ ] và checkpoint; plan.md ghi quyết định/duyệt/đường dẫn thực/giới hạn/test; data-model.md ghi bảng chung/bản gốc bất biến; contracts/api-005.yaml và module-interfaces.md ghi bản dịch đã duyệt/cách dùng/gates; glossary11.12 chỉ phần005; team-plan thêm3dòng chờ A/D/E và cập nhật phần chờAPI/consumer/ParameterChange; progress.md chỉ thêm dòng mới. Không xóa dòng tiến độ/history cũ, không sửa AGENTS.md/CLAUDE.md/business/spec hoặc dependency. Không xóa file; hand-over-task-003.md untracked của công việc khác được giữ nguyên.

Các phần thực hiện và phần chưa nghiệm thu:

1. T050: test gốc bất biến/triggerUPDATE/quyềnDELETE, A/B theo tầng, quyền/HEALTH khi xemchain, VOID được hỗ trợ, chuyển đo chỉsố sang007, projection/bản gốc, callbackF/ngủAMEND, rollback thật, concurrency và giới hạn nguồn đã có RED→GREEN. Chưa có nghiệm thu sự kiện billable với newQuantity thật; test503 chỉ chứng minh guard, không thay acceptance của nhánh tính phí.
2. T051: provider M04.CARE_RESULT, serviceRC, POST /care-results/{id}/correct, mapping API đọc và callback đã cài. Chờ E/010 chốt newQuantity để phát BillableCareResultCorrected; chờ D/007 cách biểu diễn VOIDngủ/consumer, A thống nhất genericghi/isolation/thu hẹp trước approval. Gate mealId phát sinh/payload dài và các nguồn thực từPhase8 còn nguyên. Không tự phát số lượng/nhóm giả, giảmealId, nới outbox hoặc mở genericghi thiếu bảo đảm. T050/T051 chưa đóng toàn phần.
3. Theo thứ tự tasks.md: chốt phần còn lại T050 → T051 và xác minh lại; task mới tiếp theo là T052 (test bốn nguồn báo xem xét kế hoạch) → T053 (listener), thuộcPhase10. Chưa cài/chạyPhase10 trong lượt này.

Kết luận checkpoint: nhánh đính chính A/B được hỗ trợ đã có mã và kiểm chứng; toànPhase9 vẫn mở do gate nguồn/tích hợp và fullverify còn lỗi cũ. Bản ghi lịch sử này phản ánh các thay đổi và kết quả thực tế, không ghi nhận nhánh bị chặn là đã hoàn thành.


## 03/11/2026 — Gỡ conflict merge develop theo xác nhận của chủ dự án

### Yêu cầu và trình tự thực hiện

1. Rà merge sau khi chủ dự án ghép code; chỉ đọc ở lượt đầu và báo phương án. HEAD `e35a46a2` (005 Phase9 một phần), MERGE_HEAD `698932cd` (PR #96/004); phát hiện docs/team-plan.md có 7 khối, docs/progress.md có 1 khối unmerged. Source không có marker nhưng còn tham chiếu package API cũ.
2. Sau xác nhận “xác nhận công việc”, gộp tài liệu và cập nhật import ở phần C sở hữu. Đã đọc luật chung, README backend, tasks/plan005 và lịch sử cũ; không commit, không đổi nhánh, không khởi tạo merge khác.
3. Giữ lịch sử hai nhánh nguyên nội dung; đối chiếu stage2/stage3 của progress.md: không mất dòng không rỗng. team-plan giữ các gate care, gộp việc trùng, giữ cập nhật 004/016 mới và tiến độ 003/005 hiện có; không chọn toàn bộ ours/theirs. Loại dòng chờ nguồn lưu trú/B fixture đã được develop cập nhật hoàn thành, giữ việc nối listener ResidenceTypeChanged ở C; nguồn thật có khai báo không được ghi thành đã nối tích hợp.
4. Compile đã đỏ trước sửa với JDK25 vì CorrectionType/CurrentValue/CorrectionMade/ParameterCode/AuditAction/AuditLogEntry không còn ở package cũ. Sau sửa care, compile tiếp tục đỏ tại BedTransferService do AuditAction/AuditLogEntry/Basis qua wildcard cũ; sửa cùng nguyên nhân trong module003. testCompile tiếp tục chỉ ra ParameterCode fully qualified ở BR_M04_05_OverdueReminderIT/FR_003_CarePlanLifecycleIT và tham chiếu đính chính fully qualified trong BR_M04_14_CareResultCorrectionIT; đã đổi đúng FQN. Chỉ sửa import/FQN, không sửa assertion hoặc hành vi.
5. Chạy Spotless cho care và roombedallocation, rồi scoped verify gồm test care, BR_M03_07_BedTransferIT, FR_035_LocationHistoryHttpIT, các ArchTest và ModuleBoundaryTest. Kết quả cuối sẽ được ghi khi lệnh kết thúc; chưa đánh checkbox task hoặc tuyên bố phase hoàn tất.

### Danh sách file sửa và mục đích

- `backend/src/main/java/com/eldercare/backend/care/internal/attendance/service/AttendanceClockWork.java`: đọc ParameterCode từ api.dto khi xử lý mốc điểm danh.
- `backend/src/main/java/com/eldercare/backend/care/internal/attendance/service/AttendanceCorrectionProvider.java`: dùng DTO correction hiện hành trong SPI đính chính điểm danh.
- `backend/src/main/java/com/eldercare/backend/care/internal/attendance/service/AttendanceService.java`: dùng AuditAction/AuditLogEntry từ api.dto cho lệnh điểm danh.
- `backend/src/main/java/com/eldercare/backend/care/internal/careplan/service/CarePlanActivation.java`: dùng DTO nhật ký khi kích hoạt kế hoạch.
- `backend/src/main/java/com/eldercare/backend/care/internal/careplan/service/CarePlanApprovalReminders.java`: dùng DTO nhật ký, Basis và ParameterCode trong nhắc duyệt kế hoạch.
- `backend/src/main/java/com/eldercare/backend/care/internal/careplan/service/CarePlanService.java`: dùng DTO nhật ký cho lệnh kế hoạch.
- `backend/src/main/java/com/eldercare/backend/care/internal/careplan/service/CareRiskService.java`: dùng DTO nhật ký cho họp nguy cơ.
- `backend/src/main/java/com/eldercare/backend/care/internal/catalog/service/CareCatalogService.java`: dùng DTO nhật ký cho danh mục chăm sóc.
- `backend/src/main/java/com/eldercare/backend/care/internal/catalog/service/NonPerformanceReasonService.java`: chuyển import AuditAction/AuditLogEntry sang api.dto.
- `backend/src/main/java/com/eldercare/backend/care/internal/checklist/service/CareChecklistService.java`: dùng DTO nhật ký cho checklist.
- `backend/src/main/java/com/eldercare/backend/care/internal/result/service/CareResultCorrectionProvider.java`: dùng CorrectionType/CorrectionView/CreationMethod của api.dto.
- `backend/src/main/java/com/eldercare/backend/care/internal/result/service/CareResultCorrectionService.java`: dùng CorrectionType mới tại transaction service.
- `backend/src/main/java/com/eldercare/backend/care/internal/result/service/CareResultService.java`: dùng DTO nhật ký khi ghi nhận kết quả.
- `backend/src/main/java/com/eldercare/backend/care/internal/rules/service/CareResultRules.java`: nghe CorrectionMade tại api.events và đọc ParameterCode tại api.dto.
- `backend/src/main/java/com/eldercare/backend/care/internal/task/service/CareTaskClockProcessor.java`: dùng ParameterCode tại api.dto khi xử lý thời gian.
- `backend/src/main/java/com/eldercare/backend/care/internal/task/service/CareTaskService.java`: dùng DTO nhật ký cho lệnh công việc.
- `backend/src/main/java/com/eldercare/backend/care/internal/task/service/CareTaskValidator.java`: dùng ParameterCode tại api.dto khi kiểm công việc.
- `backend/src/main/java/com/eldercare/backend/care/web/DayCareAttendanceCorrectionController.java`: dùng CorrectionType tại api.dto cho endpoint điểm danh.
- `backend/src/main/java/com/eldercare/backend/care/web/request/AttendanceCorrectionWrite.java`: kiểu type của request tham chiếu DTO mới.
- `backend/src/main/java/com/eldercare/backend/care/web/request/CareResultCorrectionWrite.java`: kiểu type của request tham chiếu DTO mới.
- `backend/src/main/java/com/eldercare/backend/care/web/response/CareResultResponse.java`: dùng CurrentValue tại api.dto khi dựng response.
- `backend/src/main/java/com/eldercare/backend/roombedallocation/internal/allocation/service/BedTransferService.java`: dùng AuditAction/AuditLogEntry/Basis tại api.dto khi chuyển giường.
- `backend/src/test/java/com/eldercare/backend/care/BR_M04_02_DayCareAttendanceIT.java`: cập nhật import/FQN DTO nền trong test hồi quy hiện có; giữ nguyên kịch bản và assertion.
- `backend/src/test/java/com/eldercare/backend/care/BR_M04_05_OverdueReminderIT.java`: cập nhật import/FQN DTO nền trong test hồi quy hiện có; giữ nguyên kịch bản và assertion.
- `backend/src/test/java/com/eldercare/backend/care/BR_M04_14_CareResultCorrectionIT.java`: cập nhật import/FQN DTO nền trong test hồi quy hiện có; giữ nguyên kịch bản và assertion.
- `backend/src/test/java/com/eldercare/backend/care/BR_M04_14_CareResultCorrectionProviderTest.java`: cập nhật import/FQN DTO nền trong test hồi quy hiện có; giữ nguyên kịch bản và assertion.
- `backend/src/test/java/com/eldercare/backend/care/FR_003_CarePlanLifecycleIT.java`: cập nhật import/FQN DTO nền trong test hồi quy hiện có; giữ nguyên kịch bản và assertion.
- `backend/src/test/java/com/eldercare/backend/care/FR_015_AttendanceCorrectionControllerTest.java`: cập nhật import/FQN DTO nền trong test hồi quy hiện có; giữ nguyên kịch bản và assertion.
- `backend/src/test/java/com/eldercare/backend/care/FR_025_CareResultCompletionIT.java`: cập nhật import/FQN DTO nền trong test hồi quy hiện có; giữ nguyên kịch bản và assertion.
- `backend/src/test/java/com/eldercare/backend/care/FR_039_CareResultRulesIT.java`: cập nhật import/FQN DTO nền trong test hồi quy hiện có; giữ nguyên kịch bản và assertion.
- `backend/src/test/java/com/eldercare/backend/care/FR_049_PublicCareResultsTest.java`: cập nhật import/FQN DTO nền trong test hồi quy hiện có; giữ nguyên kịch bản và assertion.
- `docs/team-plan.md`: gỡ 7 khối conflict, gộp dòng chờ trùng và bảo toàn các bàn giao còn hiệu lực.
- `docs/progress.md`: gỡ 1 khối conflict, giữ nhật ký cả hai nhánh; bổ sung kết quả hòa merge sau xác minh.
- `specs/005-daily-care-tasks/plan.md`: ghi nguyên nhân, quyết định package và phạm vi sửa; không đổi nghiệp vụ/tasks.
- `Working-history.md`: bổ sung lịch sử này vào cuối, giữ nguyên nội dung cũ.

Không tạo/xóa class, interface, migration, endpoint, DTO hoặc test mới. Các file hàng nghìn dòng đã staged sẵn từ merge thuộc các thành viên khác không được tính là thay đổi do lượt sửa này. Các bản stage gốc và log/script kiểm nằm ngoài kho mã ở `C:/Users/nguye/.codex/visualizations/2026/11/02/01a19a62-9c6c-7bb3-8b5d-5b150f6ba35e/`.

### Quan hệ file, luồng xử lý và quyết định kỹ thuật

Controller/request tiếp tục dùng CorrectionApi → provider005 → callback/CorrectionMade → quy tắc F; chỉ đường import enum/DTO/event đổi theo T110. DTO phản hồi CareResultResponse vẫn đọc CurrentValue của CorrectionApi, không đổi field HTTP. Các service điểm danh/kế hoạch/công việc/chuyển giường vẫn ghi qua AuditLogApi và đọc CFG qua ParameterApi; AuditAction/AuditLogEntry/Basis/ParameterCode được import từ api.dto. Test gọi đúng các kiểu công khai mới; không tạo bản giả thứ hai, không đưa fake vào production.

Dùng compiler và bộ test đã có làm bằng chứng RED→kiểm sau sửa cho việc hòa import, không viết test phản chiếu dòng import. Không đổi schema outbox, quantity billable, VOID ngủ, isolation chung, công thức ăn hoặc nguồn ca vì các quyết định đó vẫn cần chủ module/chủ dự án. Không sửa common, pom.xml, nguồn/test A/B/D/E, UI hoặc generated client; không chụp màn hình.

Các lỗi lệnh/môi trường đã gặp: Maven đầu bị chặn lock trong cache .m2; lần compile tiếp dùng JDK24 mặc định nên báo release version25 not supported. Đã chạy với JAVA_HOME=E:/JAVA/JDK_GE/INS_25 và quyền cache được duyệt, không đổi cấu hình Java toàn máy. Một lệnh Spotless có regex chứa dấu | bị wrapper Windows tách lệnh (exit255); đã chạy lại hai pattern riêng. Lỗi này không phải lỗi mã nguồn hoặc kết quả test.

### Vấn đề còn mở và việc tiếp theo

Giữ T050 → T051 [ ]: newQuantity chờ E010, tín hiệu VOID ngủ chờ D007, generic correction/isolation chờ A; gate mealId/payload dài và các nguồn tích hợp trước đó giữ nguyên. Sau các phần còn lại T050/T051 mới tới T052 (test xem xét kế hoạch) → T053 (listener), Phase10 chưa bắt đầu. Việc hòa merge không được dùng để đóng các task đang thiếu nghiệm thu.


### Xác minh scoped và trạng thái Git sau gỡ conflict

`merge-resolution-scoped.log` kết thúc exit1:127 test đơn vị/kiến trúc xanh,281 IT có0failure/1error/0skip.258 IT care có đúng lỗi cũ `FR_003_CarePlanLifecycleIT.FR_003_maximumEscapedReturnReasonSurvivesOutboxStorage` (Data too long cho EVENT_PUBLICATION.SERIALIZED_EVENT); không sửa schema/test để làm xanh. `BR_M03_07_BedTransferIT`15ca và `FR_035_LocationHistoryHttpIT`8ca xanh. Source và test đã qua compile với JDK25. Các log compile RED trước sửa, lỗi môi trường và scoped được giữ ngoài checkout.

Web sau merge: `npm test -- --run`458test/77file xanh, `npm run lint` và `npm run build` exit0; build chỉ cảnh báo chunk lớn hơn500kB. Không sửa frontend hoặc UI. Reviewer độc lập theo skill requesting-code-review không phát hiện P1/P2 trong31file Java và tài liệu gộp, không sửa file/chạy test song song. `git diff --check` qua; kiểm marker trong source/docs/specs không còn kết quả. Đã `git add` đúng35file của lượt xử lý (31Java+4tài liệu), `git ls-files -u` rỗng; HEAD vẫn `e35a46a2`, MERGE_HEAD vẫn `698932cd`, không commit/push hoặc hoàn tất merge. File untracked `specs/003-room-bed-allocation/hand-over-task-003.md` có sẵn vẫn giữ ngoài index.

Đã khởi chạy nền `mvnw.cmd -o clean verify` với JDK25 và biến môi trường MySQL từ backend/.env (không in bí mật);956unit/kiến trúc xanh, IT đang chạy. Chưa ghi kết quả cuối hoặc đóng gate toàn kho trước khi tiến trình kết thúc.


### Quan sát trong clean verify sau hòa merge (chưa phải kết quả cuối)

- Khi log đọc trước đó còn ở FR_029_ContractOverdueIT, đã lấy thread dump bằng JDK25 jcmd cho JVM test17848. Stack thực tế đã ở FR_043_MinorChangeIT → ApprovalRequestService → ApproverService → RoleCatalog/CatalogItemStore; tiến trình vẫn làm việc, không dừng hoặc khởi chạy lại bộ test. Không kết luận nguyên nhân chậm từ một stack duy nhất.
- ParameterAsOfIT xanh1/1 trong full, A đã sửa trên develop tại25f492a8. Xóa dòng chờ C báo ParameterAsOfIT cũ, ghi kết quả tại đây và progress cuối; các dòng tham số khác chưa có kết luận giữ nguyên.
- Hai test đỏ cũ đã kiểm đúng nội dung: ApiContractIT chưa đăng ký api-005 (danh sách đường dẫn dư thuộc005), và outbox reason hợp lệ vượt VARCHAR4000. Không chỉ dựa vào tên test để quy baseline.
- Quan sát mới:4 error Awaitility timeout tại notification.NFR_03_DeliveryDispatcherIT (ba điều kiện10giây và một5giây),2 failure reporting.SC_008_ReadOnlyIT.Q29_onlySuccessfulExportWrites_oneAuditRowOneFile/Q29_viewingChangesNothing. Phân tích message reporting cho thấy checksum chỉ khác family_medication_batch. Chưa xác định nguyên nhân hoặc nguồn ghi, không tự gán là lỗi production hay do import. Thêm2dòng bàn giao A009 và A016/D006 ở team-plan, không sửa nguồn/test của họ, không tăng timeout hoặc bỏ bảng khỏi kiểm tra.
- Các chuỗi EVENT_TYPE CorrectionMade trong cleanup test Phase9 được đổi sang api.events cùng FQN lớp mới để tiếp tục lọc đúng loại publication; giữ điều kiện recordType/sourceType và test bảo toàn publication khác loại. Đây là cập nhật tên kiểu sự kiện sau T110, không mở rộng phạm vi xóa.


Bổ sung đối chiếu nội dung ResidentContractIT: trước merge, log Phase9 tại ResidentContractIT.java:122 đỏ ở so operation (danh sách endpoint); sau merge, test đi tới so schema và báo SelfDeterminationAssessment thiếu at (expected at/canSelfDetermine/reviewDate/supportMethod, actual ba trường còn lại). Vì vậy tuy tên phương thức có trong danh sách đỏ cũ, nguyên nhân hiện tại khác; ghi dòng bàn giao B riêng, không gọi đây là cùng lỗi baseline đã biết. B cần đối chiếu response/DTO với hợp đồng; C không đổi API hoặc nguồn resident. So sánh tên phương thức trong summary chỉ giúp phát hiện tên mới, không thay đối chiếu nội dung lỗi.


### Kết quả cuối hòa merge — 03/11/2026

`merge-resolution-clean-verify.log` đã kết thúc, Maven exit1/BUILD FAILURE,44:36phút. Đọc XML sau clean (không trộn báo cáo scoped):956unit/kiến trúc xanh/0skip;2504IT có6failure+30error/2skip. Hai skip thuộc SC_001_UrgentConcurrencyIT, không thêm điều kiện bỏ qua test trong lượt sửa. Spotless2959file sạch,0cần sửa. Care258IT có0failure/1error outbox reason cũ;24test riêngPhase9 (3unit+21IT) xanh;23IT003 liênquan (15BedTransfer+8LocationHistoryHttp) xanh. Web458test/77file,lint/build exit0; chỉ cảnh báo Vite chunk lớn. Không sửa mobile/UI hoặc chụp màn hình.

36phươngthức đỏ chia theo lớp:

| Lớp | Failure | Error | Nội dung/chủ phần |
| --- | ---: | ---: | --- |
| AssignmentIT | 2 | 25 | Gate fixture giờ/quyền đã bàn giao E008; không sửa test E |
| ApiContractIT | 1 | 0 | Test chung chưa đăng ký api-005, danh sách đường dẫn dư đều thuộc005; chờ A000 |
| FR_003_CarePlanLifecycleIT | 0 | 1 | maximumEscapedReturnReasonSurvivesOutboxStorage: reason hợp lệ vượt SERIALIZED_EVENT VARCHAR4000; chờ A000 |
| ResidentContractIT | 1 | 0 | SelfDeterminationAssessment thiếu at; cùng tên đỏ cũ nhưng nguyên nhân hiện tại khác phần so operation trước merge; chờ B001 |
| NFR_03_DeliveryDispatcherIT | 0 | 4 | Awaitility timeout4ca đã ghi đầy đủ tại team-plan; tên mới so mốc Phase9 trước merge, chờ A009 |
| SC_008_ReadOnlyIT | 2 | 0 | Hai kiểm checksum khác duy nhất family_medication_batch; tên mới so mốc Phase9 trước merge, chờ A016/D006 xác minh |

`merge-resolution-final-summary.json` lưu tổng từng nhóm/lớp, full method name, message rút gọn và đối chiếu với005-p9-final-verification-summary.json. Có6tên đỏ mới (4notification+2reporting),30tên cũ còn;15tên cũ không còn đỏ. Một trong30tên cũ là ResidentContractIT đã đổi nguyên nhân; không gọi toàn bộ30trườnghợp là cùng nguyên nhân baseline. Không suy rằng6lỗi mới do import khi chưa có bằng chứng nguyên nhân; không sửa nguồn A/B/D/E, tăng timeout, bỏ assertion hoặc schema để làm xanh.

Lệnh xác minh đã thực thi (backend, JDK25 và biến .env được nạp vào Process):

- `.\mvnw.cmd -o -DskipTests compile`: RED trước sửa import (merge-review-jdk25-compile.log).
- `.\mvnw.cmd -o -DskipTests test-compile`: hai lượt RED tiếp theo đã chỉ ra BedTransferService và các FQN trong test; không ghi là xanh trước khi sửa đủ.
- `.\mvnw.cmd -o -DspotlessFiles=.*care.* spotless:apply` và pattern `.*roombedallocation.*`: định dạng phần C; không đổi Java khác.
- `.\mvnw.cmd -o -Dtest=com.eldercare.backend.care.*Test,*ArchTest,ModuleBoundaryTest -Dit.test=com.eldercare.backend.care.*IT,BR_M03_07_BedTransferIT,FR_035_LocationHistoryHttpIT verify`: scoped127unit/Arch xanh;281IT0failure/1error outbox cũ/0skip, exit1.
- `.\mvnw.cmd -o clean verify`: chạy nền một lượt, kết quả956unit/2504IT/6failure/30error/2skip ở trên, exit1; không chạy lại test A/B/D/E riêng lẻ.
- Frontend: `npm test -- --run`, `npm run lint`, `npm run build`: cả ba exit0,458test/77file.

Đã gỡ đủ8khối conflict, cập nhật import/FQN31file Java (22main+9test) và4tài liệu; không tạo/xóa class, đổi hợp đồng, thêm thư viện hoặc migration. Progress.md giữ mọi dòng không rỗng từ cả hai phía trước merge. Git được đánh dấu resolve bằng add đúng35file của lượt này; incoming staged có sẵn vẫn giữ, hand-over-task-003.md có sẵn vẫn untracked. Không commit/push/đổi nhánh hoặc kết thúc merge: feature/005-daily-care-and-hand-over,HEAD=e35a46a2,MERGE_HEAD=698932cd.

Chưa nghiệm thu toànkho/phase hoặc đánh thêm[x]. Các gate T050→T051 (quantity E010, VOID ngủ D007, generic correction A và nguồn/payload trước đó) giữ nguyên; task mới tiếp theo theo tasks.md vẫn T052→T053 sau khi đủ điều kiện. Ba dòng bàn giao quan sát sau merge đã thêm vào team-plan; dòng ParameterAsOfIT cũ được xóa vì A đã sửa trên develop25f492a8 và full hiện tại xanh1/1, kết quả giữ tại progress/history. File lịch sử này chỉ bổ sung, không xóa nội dung cũ.
