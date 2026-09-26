# Feature Specification: Quản lý thuốc

**Feature Branch**: `006-medication-management`

**Created**: 2026-09-26

**Status**: Draft

**Input**: User description: "Quản lý đơn thuốc, liều và thực hiện thuốc theo docs/nghiep-vu.md Module 07 (mục 11): đơn thuốc không sửa liều, đổi liều bằng đơn thay thế; kiểm tra trùng hoạt chất và dị ứng; hệ thống sinh từng liều với cửa sổ thời gian; vòng đời liều (đúng giờ, trễ, bỏ lỡ, từ chối, tạm dừng, mang theo); thuốc khi cần có giới hạn khoảng cách và số lần; đối chiếu thuốc bắt buộc khi tiếp nhận và khi trở về từ bệnh viện; thuốc gia đình gửi phải đối chiếu trước khi dùng và được theo dõi số lượng. Không quản lý kho thuốc tổng thể."

## Clarifications

### Session 2026-09-26

- Q: Khi người cao tuổi đi hoạt động ngoài viện hoặc Tạm vắng có mang thuốc, ai ghi nhận liều Mang theo? → A: Chỉ Điều dưỡng (FR-026). Điều dưỡng đi cùng thì ghi tại chỗ; không có điều dưỡng đi cùng thì Điều dưỡng phụ trách ghi sau khi người cao tuổi trở về, theo báo lại của nhân viên đi cùng hoặc người thân, gắn căn cứ "ghi theo báo lại" và lưu người báo lại. Permission Matrix 4.4 giữ nguyên (đề xuất Q-54).
- Q: Ai được xác nhận phiếu đối chiếu thuốc, và người lập phiếu có được tự xác nhận không? → A: Bác sĩ hoặc Điều dưỡng lập, một Bác sĩ hoặc Điều dưỡng khác xác nhận (quy tắc hai người, không tự xác nhận), áp như nhau khi cơ sở có hay không có phạm vi khám chữa bệnh; phiếu có dòng tạo đơn nội bộ chỉ Bác sĩ có quyền kê đơn (FR-005) xác nhận (đề xuất Q-55).
- Q: Số lần tối đa mỗi ngày của thuốc khi cần (PRN) tính theo ngày dương lịch hay 24 giờ trượt? → A: 24 giờ trượt, đếm các lần Đã dùng trong 24 giờ liền trước thời điểm dùng mới (đề xuất Q-56).
- Q: Với người mới tiếp nhận, phiếu đối chiếu có được lập và xác nhận trước khi Hoàn tất tiếp nhận không? → A: Có; phiếu "tiếp nhận" được lập, gửi, xác nhận khi hồ sơ còn Đang tiếp nhận; đơn tạo ra chờ và có hiệu lực ngay tại lệnh Hoàn tất tiếp nhận; nếu tới lúc đó phiếu chưa xác nhận thì áp quy tắc cũ (không có liều, hạn CFG-M07-05 tính từ Hoàn tất tiếp nhận) (đề xuất Q-57).
- Q: Khi thuốc được giao mang theo lúc Tạm vắng hoặc đi hoạt động ngoài viện, số lượng thuốc gia đình gửi và chi phí thuốc viện được tính lúc giao hay theo từng liều? → A: Lúc giao: điều dưỡng ghi "Giao thuốc mang theo" (số lượng từng đơn), trừ số lượng thuốc gia đình gửi và tạo chi phí nháp thuốc viện ngay; khi trở về ghi "Nhận lại" để cộng lại / giảm chi phí; liều ghi sau đó không trừ hay tính phí lần nữa (đề xuất Q-58).

### Session 2026-09-26 (lượt 2, sau checklist business-rules)

- Sửa lỗi, không phải quyết định mới: bảng FR-021 bỏ việc khôi phục liều khi trở về từ Điều trị tại bệnh viện, để khớp BR-M07-09 và FR-034 (checklist CHK018).
- Q: Người thân không có bản đồng ý chia sẻ dữ liệu có được xem phiếu đối chiếu đã xác nhận và nhận thông báo nêu tên thuốc sắp hết không? → A: Thuốc gia đình gửi (tên, số lượng còn, hạn dùng, trạng thái) và thông báo sắp hết/hết hạn dùng không cần bản đồng ý; phiếu đối chiếu chỉ xem được khi có bản đồng ý đang hiệu lực bao gồm người thân đó (đề xuất Q-59).
- Q: Người cao tuổi trở về sớm, liều Mang theo chưa tới giờ đã thuộc một lần giao thuốc (đã trừ số lượng, đã tính phí); khi điều dưỡng cho dùng liều đó ở viện thì tính thế nào? → A: Liều thôi thuộc lần giao; điều dưỡng ghi "Nhận lại" gồm cả thuốc của liều chưa dùng, liều dùng ở viện trừ số lượng và tính phí như bình thường; nếu lần giao còn mở khi xác nhận liều thì hệ thống nhắc ghi nhận lại, không chặn (đề xuất Q-60).
- Q: Khi người cao tuổi không có điều dưỡng được phân công trong ca, ai thấy và ai được xác nhận liều của người đó? → A: Liều thành "liều chung của tầng": mọi Điều dưỡng có ca tại tầng thấy và người có giấy phép còn hiệu lực xác nhận được; Trưởng tầng và Người phụ trách ca được thông báo (đề xuất Q-61).
- Q: Khi mất kết nối đúng lúc cần phát thuốc kiểm soát đặc biệt (bắt buộc xác nhận trực tuyến) thì xử lý thế nào? → A: Vẫn cho dùng, ghi tạm ngoài hệ thống; ghi trực tuyến ngay khi có kết nối với thời điểm dùng thực tế, nhãn "ghi sau mất kết nối", luôn chuyển "chờ xem lại" cho một Điều dưỡng khác người ghi (Người phụ trách ca nếu là Điều dưỡng) (xem Q-66); trạng thái liều theo thời điểm dùng thực tế (đề xuất Q-62).
- Q: Ngoài bắt buộc xác nhận trực tuyến, hệ thống có cần kiểm soát pháp lý riêng cho thuốc gây nghiện, hướng thần (người chứng kiến, đếm số lượng còn) không? → A: Không bổ sung ở spec này; giữ phạm vi hiện tại (trực tuyến và xem lại khi mất kết nối) và đưa thành quyết định còn mở cho Quản lý viện và tư vấn pháp lý (đề xuất Q-63, mục 24.1).
- Q: Người cao tuổi qua đời hoặc kết thúc lưu trú khi lần giao thuốc mang theo còn Chờ nhận lại thì xử lý thế nào? → A: Như thuốc gia đình gửi: lần giao chưa Đã nhận lại chặn Kết thúc lưu trú (trừ ngoại lệ được duyệt) và là một mục của danh sách việc sau qua đời; Điều dưỡng ghi Nhận lại, kể cả số lượng 0 với lý do "người thân giữ lại" (đề xuất Q-64).
- Q: Khi người cao tuổi trở về sớm, điều dưỡng có được dùng viên thuốc đã giao mang theo cho liều ở viện không? → A: Chỉ sau khi đã ghi "Nhận lại" viên đó vào lô; liều ở viện luôn trừ lô và tính phí như bình thường (đề xuất Q-65).
- Q: Ai xem lại bản ghi thuốc kiểm soát đặc biệt "ghi sau mất kết nối"? → A: Chỉ Điều dưỡng khác người ghi: Điều dưỡng giữ nhiệm vụ Người phụ trách ca, nếu không có thì Điều dưỡng khác có ca tại tầng; người xem lại tự đính chính khi cần; Trưởng tầng chỉ được thông báo (đề xuất Q-66).

### Cập nhật 2026-09-26 (đồng bộ với spec 008)

Spec 008 đã chốt khi clarify (Q-85): từ giờ bắt đầu ca sau mà bàn giao chưa được xác nhận, liều chưa đóng và liều Mang theo chờ ghi nhận của điều dưỡng ca trước đã hết ca trở thành liều chung của tầng. FR-026a được bổ sung cho khớp.

### Cập nhật 2026-09-26 (đồng bộ với spec 009)

Spec 009 đã chốt mức cho các thông báo của spec này (spec 009 FR-043b): đơn thuốc sắp hết hạn (FR-017) là Nhẹ; nhắc liều Trễ, người cao tuổi chưa có điều dưỡng phụ trách (FR-026a), liều thuốc kiểm soát đặc biệt "chờ xem lại" (FR-029), phiếu đối chiếu cần hoàn thành là Trung bình; thuốc gia đình gửi sắp hết gửi người thân (FR-046) là Trung bình với nội dung chia phần "chung" và "sức khỏe" (Q-116); FR-046 được sửa cho khớp. Thông báo "người cao tuổi chưa có điều dưỡng phụ trách" của FR-026a gửi spec 009 với cách thay "không thay" vì spec này đã tự xử lý bằng liều chung (spec 009 FR-010, Q-110).

### Cập nhật 2026-09-27 (đồng bộ với spec 010)

Spec 010 và tài liệu nguồn (6.4, 11.4, 15.2) đã chốt hai điểm, và các FR của spec này được sửa theo:
- **Q-139, FR-040 và FR-033:** thuốc mua hộ được tiếp nhận như thuốc gia đình gửi, có tham chiếu tới đề nghị mua hộ; liều của nó không sinh chi phí.
- **FR-003:** đơn giá thuốc nguồn viện nằm trong danh mục vật phẩm có phiên bản đơn giá của feature 004 (FR-034, FR-035).

## Phạm vi

**Trong phạm vi** (Module 07, mục 11.1 → 11.6; UC-39 → UC-44):

1. Danh mục thuốc làm căn cứ cho đơn: tên, hoạt chất, hàm lượng, dạng, đường dùng, đơn vị, cửa sổ thời gian riêng, đánh dấu kiểm soát đặc biệt (11.1, 11.2).
2. Đơn thuốc định kỳ và thuốc khi cần (PRN): nhập đơn nội bộ hoặc đơn từ cơ sở kê bên ngoài, kiểm tra trùng hoạt chất và dị ứng, vòng đời đơn, ngừng đơn, đơn thay thế khi đổi liều/đổi thuốc, gia hạn, hết hạn (11.1, UC-39, UC-40, BR-M07-05 → 08, DBR-14).
3. Sinh liều cụ thể từ đơn định kỳ với cửa sổ thời gian; vòng đời liều gồm Chưa đến giờ, Đến giờ, Trễ, Bỏ lỡ, Đã dùng, Từ chối, Không thực hiện, Tạm dừng, Mang theo (11.2, BR-M07-01 → 03, DBR-13).
4. Phát thuốc và xác nhận liều, ghi phản ứng, cảnh báo khi bất thường, đính chính (11.3, UC-41, BR-M07-12 → 14).
5. Dùng thuốc khi cần với khoảng cách tối thiểu và số lần tối đa mỗi ngày (UC-42, BR-M07-04).
6. Đối chiếu thuốc bắt buộc khi tiếp nhận và khi trở về từ bệnh viện (11.5, UC-43, BR-M07-09).
7. Thuốc gia đình gửi: tiếp nhận, đối chiếu, gắn với đơn, theo dõi số lượng, nhắc người thân khi sắp hết, chặn thuốc hết hạn dùng, hoàn trả, hủy theo yêu cầu gia đình (11.4, UC-44, BR-M07-10, 11).

**Ngoài phạm vi** (spec này chỉ **cung cấp** dữ liệu hoặc **được kích hoạt** bởi feature sở hữu):

- Kho thuốc tổng thể của viện (nhập, xuất, tồn kho thuốc viện cung cấp, mua thuốc): không thuộc hệ thống (1.2, mục 11). Số lượng chỉ được theo dõi với thuốc gia đình gửi.
- Quy tắc dùng chung (nhóm dữ liệu, lý do bắt buộc, đính chính, nhật ký, tham số, "hoặc toàn bộ, hoặc không"): feature 000 — spec này kế thừa, không lặp lại.
- Mục dị ứng, danh mục dị nguyên, trạng thái người cao tuổi (Đang lưu trú, Tạm vắng, Điều trị tại bệnh viện, Hoạt động bên ngoài, trạng thái cuối), thuốc đang sử dụng khi tiếp nhận ghi trong hồ sơ sức khỏe ban đầu: feature 001. Khi có dị ứng mới, feature 001 FR-020 kích hoạt kiểm tra đơn thuốc ở spec này.
- Quyền, phạm vi dữ liệu, danh mục nghiệp vụ chuyên môn và điều kiện giấy phép (kê đơn nội bộ, phát thuốc/xác nhận liều), kiểm tra quyền của bản ghi ngoại tuyến: feature 002.
- Lệnh Cho tạm vắng, Ghi nhận trở về, Hoàn tất tiếp nhận, Kết thúc lưu trú, Ghi nhận qua đời và điều kiện "không còn thuốc gia đình gửi đang giữ": feature 004 (và feature 001 cho bảng trạng thái).
- Checklist theo ca: liều thuốc được **hiển thị** trong checklist của điều dưỡng (feature 005 FR-029), không sinh công việc.
- Cảnh báo, gộp cảnh báo, leo thang, sự cố, thẻ thông tin khẩn cấp và bản tóm tắt chuyển viện: feature 007. Spec này yêu cầu tạo cảnh báo và cung cấp "thuốc đang dùng".
- Phân công điều dưỡng phụ trách theo ca, bàn giao ca: feature 008. Spec này cung cấp liều Trễ, Bỏ lỡ, Từ chối cho bản nháp bàn giao (BR-M09-06).
- Gửi thông báo (Module 13): feature 009. Chi phí nháp từ liều nguồn viện và đơn giá thuốc: feature 010.
- Hồ sơ người thân, cổng người thân, danh sách được phép đón: feature 012. Đồ gửi không phải thuốc: feature 013.
- Hoạt động ngoài viện, điểm danh rời viện/trở về của chuyến đi: feature 014.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Nhập đơn thuốc, hệ thống kiểm tra trùng hoạt chất và dị ứng (Priority: P1)

Bác sĩ có quyền kê đơn hợp lệ kê đơn nội bộ; bác sĩ hoặc điều dưỡng nhập đơn đã được kê tại cơ sở y tế bên ngoài kèm thông tin cơ sở kê. Mỗi đơn có thuốc (chọn từ danh mục), hoạt chất, liều, đường dùng, loại đơn (định kỳ hoặc khi cần), tần suất và mốc giờ (với đơn định kỳ) hoặc khoảng cách tối thiểu và số lần tối đa mỗi ngày (với PRN), ngày bắt đầu, ngày kết thúc nếu có, nguồn thuốc, người kê, cơ sở kê, ghi chú. Trước khi lưu, hệ thống kiểm tra trùng hoạt chất với các đơn đang dùng và đối chiếu với dị ứng trong hồ sơ; có vấn đề thì cảnh báo và chỉ cho tiếp tục khi người nhập xác nhận lý do.

**Why this priority**: Đơn thuốc là nguồn duy nhất sinh liều (11.2); sai hoạt chất hoặc bỏ qua dị ứng có thể gây hại trực tiếp cho người cao tuổi.

**Independent Test**: Với người cao tuổi A có dị ứng Penicillin đang hiệu lực và đơn Amlodipin 5 mg đang hiệu lực, lần lượt nhập đơn Amoxicillin (xung đột dị ứng), đơn thuốc phối hợp chứa amlodipin (trùng hoạt chất) và đơn Metformin (không vấn đề); kiểm tra hai đơn đầu bị cảnh báo và chỉ lưu được khi có lý do, đơn thứ ba lưu ngay.

**Acceptance Scenarios**:

1. **Given** cơ sở có phạm vi khám chữa bệnh còn hiệu lực và bác sĩ B có giấy phép còn hiệu lực đúng phạm vi, **When** B kê đơn nội bộ Metformin 500 mg, uống, 2 lần/ngày lúc 07:00 và 19:00, từ 01/10, nguồn viện cung cấp cho A, **Then** đơn được lưu với người kê là B, cơ sở kê là viện, và chuyển Hiệu lực vào 01/10 (BR-M07-05).
2. **Given** giấy phép của bác sĩ B đã hết hạn hôm qua, **When** B kê đơn nội bộ, **Then** hệ thống từ chối (BR-M06-05, feature 002 FR-037); **When** B nhập đơn đã được kê tại Bệnh viện X kèm tên cơ sở kê, người kê, ngày kê, **Then** hệ thống cho phép.
3. **Given** cơ sở không có phạm vi khám chữa bệnh, **When** điều dưỡng D nhập đơn từ Bệnh viện X mà bỏ trống cơ sở kê, **Then** hệ thống chặn (BR-M07-05); **When** D nhập đủ thông tin cơ sở kê, người kê, ngày kê, **Then** đơn được lưu.
4. **Given** A có mục dị ứng "Penicillin" đang Hiệu lực, **When** người nhập lưu đơn Amoxicillin, **Then** hệ thống cảnh báo xung đột dị ứng nêu rõ mục dị ứng và mức độ; **When** người nhập không ghi lý do, **Then** đơn không được lưu; **When** người nhập ghi lý do "đã test âm tính, bác sĩ chuyên khoa chỉ định", **Then** đơn được lưu kèm lý do xác nhận và cảnh báo được lưu lịch sử (BR-M07-06).
5. **Given** A đang có đơn Amlodipin 5 mg Hiệu lực, **When** người nhập lưu đơn thuốc phối hợp amlodipin + valsartan, **Then** hệ thống cảnh báo trùng hoạt chất "amlodipin" và nêu đơn đang có; chỉ lưu được khi có lý do (BR-M07-06).
6. **Given** A có mục dị ứng loại "khác" (ghi tự do, không chọn từ danh mục, feature 001 FR-018a), **When** nhập đơn mới, **Then** hệ thống hiển thị mục đó kèm dấu "không kiểm tra tự động" để người nhập tự xem xét, không chặn.
7. **Given** A có đơn Amoxicillin Hiệu lực, **When** điều dưỡng ghi thêm dị ứng "Penicillin" (feature 001), **Then** hệ thống kiểm tra lại mọi đơn Hiệu lực, Tạm dừng, Chờ hiệu lực của A và yêu cầu feature 007 tạo cảnh báo cho đơn Amoxicillin; đơn không tự ngừng (FR-012).
8. **Given** nhân viên chăm sóc S, **When** S mở danh sách đơn thuốc của A, **Then** hệ thống chặn vì vai trò không có quyền ở dòng "Đơn thuốc" (4.4).

---

### User Story 2 - Hệ thống sinh liều, điều dưỡng phát thuốc và xác nhận từng liều trong cửa sổ thời gian (Priority: P1)

Mỗi ngày hệ thống sinh liều cụ thể cho CFG-M07-03 ngày tới từ các đơn định kỳ đang hiệu lực. Mỗi liều có thời điểm dự kiến và cửa sổ thời gian cho phép. Điều dưỡng phụ trách thấy liều trong checklist, phát thuốc và xác nhận Đã dùng, Từ chối hoặc Không thực hiện; mỗi liều chỉ xác nhận một lần. Quá cửa sổ mà chưa xác nhận thì liều chuyển Trễ và điều dưỡng được nhắc; quá thêm CFG-M07-02 thì chuyển Bỏ lỡ và có cảnh báo.

**Why this priority**: Phát thuốc đúng giờ là nghiệp vụ an toàn hằng ngày của điều dưỡng (BF-02 bước 4); không có liều thì không kiểm soát được việc dùng thuốc.

**Independent Test**: Tạo đơn định kỳ 08:00 và 20:00 cho A; kiểm tra liều được sinh cho 2 ngày tới, không trùng khi chạy lại; xác nhận liều 08:00 lúc 08:10; để liều 20:00 không xác nhận và kiểm tra 20:30 chuyển Trễ, 21:00 chuyển Bỏ lỡ kèm cảnh báo trung bình.

**Acceptance Scenarios**:

1. **Given** đơn định kỳ Amlodipin 5 mg lúc 08:00 hằng ngày của A Hiệu lực từ 01/10, **When** Bộ lập lịch chạy ngày 01/10, **Then** A có liều 08:00 ngày 01/10, 02/10 (CFG-M07-03, mặc định \[2 ngày\]); **When** chạy ngày 02/10, **Then** liều 03/10 được sinh thêm và không có liều trùng (đơn, thời điểm dự kiến) (BR-M07-01, DBR-13).
2. **Given** Amlodipin không khai báo cửa sổ riêng, **When** liều 08:00 được sinh, **Then** cửa sổ là 07:30–08:30 theo CFG-M07-01 (mặc định \[±30 phút\]); **Given** danh mục khai báo Insulin nhanh có cửa sổ riêng ±15 phút, **Then** liều Insulin 11:30 có cửa sổ 11:15–11:45 (11.2).
3. **Given** liều 08:00 của A Chưa đến giờ, **When** tới 07:30, **Then** liều chuyển Đến giờ và nổi lên trong checklist của điều dưỡng phụ trách A trong ca (feature 005 FR-029).
4. **Given** liều Đến giờ, **When** điều dưỡng D có giấy phép còn hiệu lực, được phân công A, xác nhận Đã dùng lúc 08:10, **Then** liều chuyển Đã dùng, lưu người xác nhận, thời điểm dùng, thời điểm ghi, phản ứng (nếu có), ghi chú; liều nguồn viện cung cấp tạo chi phí nháp ở feature 010 (BR-M07-14).
5. **Given** liều đã Đã dùng, **When** D hoặc người khác xác nhận lại liều đó, **Then** hệ thống chặn và báo liều đã được xác nhận bởi ai, lúc nào (BR-M07-12, DBR-13).
6. **Given** liều 20:00 Đến giờ không được xác nhận, **When** tới 20:30, **Then** liều chuyển Trễ và D được nhắc; **When** tới 21:00 (CFG-M07-02, mặc định \[30 phút\]) vẫn chưa xác nhận, **Then** liều chuyển Bỏ lỡ và feature 007 nhận yêu cầu tạo cảnh báo mức trung bình (BR-M07-02).
7. **Given** liều Trễ lúc 20:40, **When** D xác nhận Đã dùng với thời điểm dùng 20:40, **Then** liều chuyển Đã dùng kèm nhãn "dùng trễ".
8. **Given** A đã Từ chối liều Amlodipin 08:00 hôm qua, **When** D ghi A Từ chối liều 08:00 hôm nay kèm lý do, **Then** feature 007 nhận yêu cầu tạo cảnh báo mức trung bình trở lên do từ chối CFG-M05-04 (mặc định \[2\]) lần liên tiếp cùng một thuốc (BR-M07-13); **When** D ghi Từ chối mà không có lý do, **Then** hệ thống chặn.
9. **Given** D xác nhận Đã dùng và ghi phản ứng "nổi mẩn đỏ sau 15 phút", **When** lưu, **Then** feature 007 nhận yêu cầu tạo cảnh báo mức trung bình trở lên (BR-M07-13).
10. **Given** nhân viên chăm sóc S hoặc trưởng tầng T, **When** xác nhận một liều, **Then** hệ thống chặn (4.4 dòng "Phát thuốc, thuốc khi cần": chỉ ĐD thực hiện); **Given** điều dưỡng D2 có giấy phép hết hạn, **When** D2 xác nhận liều, **Then** hệ thống chặn (feature 002 FR-036).
11. **Given** điều dưỡng được phân công A trong ca ngày vắng ca và chưa có phân công thay, **When** liều 08:00 của A chuyển Đến giờ, **Then** liều hiển thị là "liều chung của tầng" cho mọi điều dưỡng có ca ngày tại tầng của A, Trưởng tầng và Người phụ trách ca nhận thông báo "A chưa có điều dưỡng phụ trách"; **When** điều dưỡng D4 có ca tại tầng xác nhận Đã dùng, **Then** liều chuyển Đã dùng với căn cứ "liều chung của tầng"; **When** điều dưỡng D5 có ca ở tầng khác xác nhận, **Then** hệ thống chặn (FR-026a).
12. **Given** liều Morphin 22:00 của A (thuốc kiểm soát đặc biệt) Đến giờ và thiết bị của điều dưỡng D mất kết nối, **When** D ghi Đã dùng trên thiết bị, **Then** hệ thống không lưu tạm bản ghi này; D cho A dùng lúc 22:05 và ghi tạm ngoài hệ thống; **When** có kết nối lại lúc 22:50 (liều đã Trễ) và D ghi Đã dùng với thời điểm dùng 22:05, **Then** liều chuyển Đã dùng không nhãn trễ, mang nhãn "ghi sau mất kết nối", chuyển "chờ xem lại" cho Người phụ trách ca, và feature 007 nhận yêu cầu đóng cảnh báo nếu đã có (FR-029, Q-62).

---

### User Story 3 - Đổi liều bằng đơn thay thế, ngừng đơn, gia hạn và hết hạn (Priority: P1)

Đơn đang hiệu lực không sửa thuốc, liều hay tần suất. Đổi liều hoặc đổi thuốc là ngừng đơn cũ và tạo đơn thay thế liên kết "thay thế cho" trong cùng một lệnh. Các liều chưa dùng từ thời điểm hiệu lực của thay đổi bị hủy và sinh lại theo đơn mới; liều đã dùng giữ nguyên. Đơn có ngày kết thúc được nhắc trước để bác sĩ gia hạn hoặc ngừng; tới ngày kết thúc thì tự Hết hạn.

**Why this priority**: 11.1 và DBR-14 là quy tắc toàn vẹn cốt lõi của module; sửa đè liều sẽ mất dấu vết điều trị và gây phát sai liều.

**Independent Test**: Với đơn Amlodipin 5 mg lúc 08:00, thử sửa liều thành 10 mg và kiểm tra bị chặn; dùng "Đổi liều" từ 14:00 hôm nay; kiểm tra đơn cũ Đã ngừng, đơn mới Hiệu lực trỏ về đơn cũ, liều 08:00 hôm nay đã dùng giữ nguyên, liều 08:00 ngày mai theo đơn cũ bị Đã hủy và có liều 10 mg mới.

**Acceptance Scenarios**:

1. **Given** đơn Amlodipin 5 mg Hiệu lực, **When** bất kỳ ai sửa liều, thuốc, tần suất hay mốc giờ của đơn, **Then** hệ thống chặn và gợi ý lệnh "Đổi liều / đổi thuốc" (11.1, DBR-14).
2. **Given** đơn trên, **When** người có quyền sửa ghi chú hướng dẫn "uống sau ăn", **Then** hệ thống cho phép và lưu lịch sử giá trị trước/sau (11.1).
3. **Given** đơn trên có liều 08:00 hôm nay Đã dùng và liều 08:00 ngày mai, ngày kia Chưa đến giờ, **When** bác sĩ "Đổi liều" thành 10 mg hiệu lực từ 14:00 hôm nay kèm lý do, **Then** trong cùng một lệnh: đơn cũ chuyển Đã ngừng với lý do "thay thế", đơn mới Hiệu lực có liên kết "thay thế cho" đơn cũ, hai liều Chưa đến giờ của đơn cũ chuyển Đã hủy, liều 10 mg được sinh cho các ngày trong CFG-M07-03, liều đã dùng giữ nguyên (BR-M07-07, DBR-14).
4. **Given** đổi liều như trên, **When** hệ thống kiểm tra đơn thay thế, **Then** kiểm tra trùng hoạt chất bỏ qua chính đơn bị thay thế nhưng vẫn áp với các đơn khác và với dị ứng (BR-M07-06).
5. **Given** đơn Hiệu lực có liều đang Đến giờ lúc bác sĩ ngừng đơn, **When** ngừng có hiệu lực ngay, **Then** liều đang Đến giờ chuyển Đã hủy cùng các liều Chưa đến giờ sau đó; liều Trễ tiếp tục vòng đời để điều dưỡng xác nhận hoặc chuyển Bỏ lỡ (FR-021).
6. **Given** bác sĩ ngừng đơn mà không nhập lý do, **When** lưu, **Then** hệ thống chặn (UC-40, feature 000).
7. **Given** đơn có ngày kết thúc 10/10, **When** tới 07/10 (CFG-M07-04, mặc định \[3 ngày\] trước), **Then** người kê (với đơn nội bộ) hoặc các bác sĩ của cơ sở và điều dưỡng phụ trách (với đơn bên ngoài) nhận nhắc gia hạn hay ngừng (BR-M07-08); **When** bác sĩ "Gia hạn" tới 20/10 kèm lý do, **Then** ngày kết thúc mới là 20/10, lịch sử giữ ngày cũ, liều được sinh tiếp.
8. **Given** không ai xử lý, **When** hết ngày 10/10, **Then** đơn chuyển Hết hạn và không còn liều nào sau thời điểm kết thúc (BR-M07-08).

---

### User Story 4 - Đối chiếu thuốc bắt buộc khi tiếp nhận và khi trở về từ bệnh viện (Priority: P1)

Khi người cao tuổi được tiếp nhận hoặc trở về từ Điều trị tại bệnh viện, mọi đơn cũ chuyển Tạm dừng và hệ thống tạo phiếu đối chiếu. Người thực hiện xem từng thuốc trong danh sách hiện có và trong đơn ra viện hoặc thuốc đang dùng khi tiếp nhận, chọn Tiếp tục, Ngừng, Thay đổi liều hoặc Thêm mới; người xác nhận xác nhận phiếu. Lịch thuốc chỉ chạy lại khi phiếu được xác nhận; quá CFG-M07-05 chưa đối chiếu thì có cảnh báo.

**Why this priority**: Sai lệch thuốc khi chuyển tiếp chăm sóc (nhập viện, ra viện) là nguồn sai sót thuốc phổ biến nhất; BR-M07-09 bắt buộc bước này trước khi tiếp tục phát thuốc.

**Independent Test**: A có 3 đơn Hiệu lực, chuyển Điều trị tại bệnh viện rồi Ghi nhận trở về với đơn ra viện gồm 2 thuốc; kiểm tra 3 đơn cũ Tạm dừng, có phiếu đối chiếu 5 dòng; chọn quyết định cho từng dòng, xác nhận; kiểm tra đơn và liều sau xác nhận đúng quyết định; lặp lại mà không xác nhận trong 4 giờ và kiểm tra có cảnh báo.

**Acceptance Scenarios**:

1. **Given** A có đơn Amlodipin, Metformin, Aspirin Hiệu lực và đang Điều trị tại bệnh viện, **When** hành chính Ghi nhận trở về lúc 10:00 (feature 004), **Then** trong cùng lệnh ba đơn chuyển Tạm dừng (lý do "chờ đối chiếu"), mọi liều chưa xác nhận của ba đơn từ 10:00 ở trạng thái Tạm dừng, và phiếu đối chiếu được tạo ở trạng thái Mở với lý do "trở về từ bệnh viện", chứa ba dòng từ danh sách hiện có (BR-M07-09, 11.5).
2. **Given** phiếu Mở, **When** người thực hiện nhập đơn ra viện gồm "Amlodipin 10 mg" và "Clopidogrel 75 mg" rồi chọn: Amlodipin cũ – Thay đổi liều (theo đơn ra viện 10 mg); Metformin – Tiếp tục; Aspirin – Ngừng (lý do "thay bằng Clopidogrel"); Clopidogrel – Thêm mới, **Then** hệ thống kiểm tra trùng hoạt chất và dị ứng cho dòng Thay đổi liều và Thêm mới ngay trên phiếu (BR-M07-06); **When** mọi dòng đã có quyết định, **Then** người thực hiện gửi được phiếu sang Chờ xác nhận.
3. **Given** phiếu Chờ xác nhận, **When** người xác nhận hợp lệ (FR-038) xác nhận lúc 12:00, **Then** trong cùng lệnh: Metformin chuyển Hiệu lực và liều từ 12:00 được khôi phục; Aspirin chuyển Đã ngừng; Amlodipin 5 mg chuyển Đã ngừng và đơn Amlodipin 10 mg thay thế được tạo Hiệu lực (DBR-14); đơn Clopidogrel được tạo Hiệu lực; liều của các đơn Hiệu lực được sinh từ 12:00; liều Tạm dừng có cửa sổ đã qua trước 12:00 giữ Tạm dừng, không bị tính Bỏ lỡ (BR-M07-09, FR-021).
4. **Given** phiếu còn một dòng chưa có quyết định, **When** gửi xác nhận, **Then** hệ thống chặn và chỉ ra dòng còn thiếu.
5. **Given** phiếu tạo lúc 10:00 chưa được xác nhận, **When** tới 14:00 (CFG-M07-05, mặc định \[4 giờ\]), **Then** feature 007 nhận yêu cầu tạo cảnh báo mức trung bình "chưa đối chiếu thuốc" (BR-M07-09); phiếu vẫn Mở và vẫn xử lý được.
6. **Given** hồ sơ B Đang tiếp nhận có "thuốc đang sử dụng khi tiếp nhận" gồm 2 thuốc ghi trong hồ sơ sức khỏe ban đầu (feature 001 FR-017), và chưa ai lập phiếu trước, **When** hành chính Hoàn tất tiếp nhận, **Then** phiếu đối chiếu lý do "tiếp nhận" được tạo với 2 dòng từ danh sách đó; B chưa có liều nào cho tới khi phiếu được xác nhận.
7. **Given** phiếu Chờ xác nhận, **When** người xác nhận trả lại kèm lý do, **Then** phiếu về Mở và người thực hiện được thông báo; **When** trả lại không có lý do, **Then** hệ thống chặn.
8. **Given** phiếu đã Đã xác nhận, **When** bất kỳ ai sửa quyết định trên phiếu, **Then** hệ thống chặn; thay đổi sau đó phải qua lệnh đơn thuốc thông thường (ngừng, đổi liều, thêm đơn) hoặc bản đính chính của phiếu (1.5).
9. **Given** điều dưỡng D ghi quyết định và gửi xác nhận phiếu chỉ gồm dòng Tiếp tục, Ngừng và một dòng Thêm mới từ đơn ra viện của Bệnh viện X, **When** D tự xác nhận, **Then** hệ thống chặn (quy tắc hai người); **When** điều dưỡng D3 không ghi quyết định nào trên phiếu xác nhận, **Then** phiếu chuyển Đã xác nhận; **Given** phiếu có dòng Thay đổi liều tạo đơn nội bộ, **When** D3 xác nhận, **Then** hệ thống chặn và yêu cầu Bác sĩ đáp ứng FR-005 (FR-038).
10. **Given** hồ sơ C Đang tiếp nhận, **When** điều dưỡng D lập trước phiếu "tiếp nhận" lúc 09:00 với Insulin – Tiếp tục (theo đơn bệnh viện, có cơ sở kê) và bác sĩ B xác nhận lúc 09:30, **Then** đơn Insulin ở Chờ hiệu lực dấu "chờ tiếp nhận", chưa có liều nào và không có cảnh báo quá hạn; **When** hành chính Hoàn tất tiếp nhận lúc 14:00, **Then** trong cùng lệnh đơn Insulin chuyển Hiệu lực từ 14:00, liều từ 14:00 được sinh, không có đơn nào bị tạm dừng và không tạo phiếu mới (FR-034a); **Given** phiếu lập trước vẫn Mở lúc Hoàn tất tiếp nhận 14:00, **Then** phiếu đó được dùng tiếp với hạn 18:00 (CFG-M07-05).

---

### User Story 5 - Liều khi người cao tuổi vắng mặt: Tạm dừng hoặc Mang theo (Priority: P2)

Khi người cao tuổi Tạm vắng hoặc Điều trị tại bệnh viện, liều trong thời gian vắng chuyển Tạm dừng. Khi đi hoạt động ngoài viện hoặc Tạm vắng có mang thuốc, liều trong khoảng đi chuyển Mang theo và được ghi nhận Đã dùng hoặc Không thực hiện. Khi trở về, liều Tạm dừng còn ở tương lai được khôi phục.

**Why this priority**: Tránh báo Trễ, Bỏ lỡ và cảnh báo sai khi người cao tuổi không ở viện (BR-M07-03), đồng thời giữ dấu vết thuốc đã dùng bên ngoài.

**Independent Test**: A có liều 08:00, 12:00, 20:00; cho A Tạm vắng không mang thuốc 09:00–15:00 và kiểm tra liều 12:00 Tạm dừng, không có nhắc hay cảnh báo; cho A đi chuyến ngoài viện 07:00–13:00 hôm sau và kiểm tra liều 08:00, 12:00 Mang theo, ghi nhận được Đã dùng.

**Acceptance Scenarios**:

1. **Given** A có liều 12:00 Chưa đến giờ, **When** A được Cho tạm vắng lúc 09:00 và lệnh ghi "không mang thuốc" (feature 004), **Then** liều 12:00 và các liều sau đó trong khoảng vắng dự kiến chuyển Tạm dừng; không có nhắc, không Trễ, không Bỏ lỡ (BR-M07-03).
2. **Given** A Tạm vắng, **When** A được Ghi nhận trở về lúc 15:00, **Then** liều Tạm dừng có thời điểm dự kiến sau 15:00 chuyển Chưa đến giờ (hoặc Đến giờ nếu 15:00 đã trong cửa sổ); liều 12:00 giữ Tạm dừng và không vào bàn giao như liều bỏ lỡ.
3. **Given** A có liều 08:00 và 12:00 ngày 05/10, **When** trưởng đoàn điểm danh A rời viện lúc 07:00 cho chuyến đi dự kiến về 13:00 (feature 014, BR-M04-16), **Then** hai liều chuyển Mang theo và danh sách thuốc cần mang của A được hiển thị cho điều dưỡng phụ trách chuẩn bị (8.9).
4. **Given** liều 08:00 Mang theo và điều dưỡng D2 đi cùng chuyến, **When** D2 ghi Đã dùng lúc 08:15, **Then** liều chuyển Đã dùng với căn cứ "mang theo"; **Given** chuyến đi không có điều dưỡng, **When** nhân viên chăm sóc S (người đi cùng) ghi liều, **Then** hệ thống chặn; **When** A trở về và điều dưỡng phụ trách D ghi Đã dùng lúc 08:15 theo báo lại của S, **Then** liều chuyển Đã dùng với căn cứ "ghi theo báo lại", lưu S là người báo lại và D là người ghi (FR-027).
5. **Given** chuyến đi về lúc 11:00 và liều 12:00 Mang theo chưa ghi nhận, **When** trưởng đoàn điểm danh A trở về, **Then** liều 12:00 chuyển Chưa đến giờ (hoặc Đến giờ nếu đã trong cửa sổ) và quay lại checklist điều dưỡng (FR-021).
6. **Given** chuyến đi về lúc 13:30 và liều 12:00 Mang theo chưa ghi nhận, **When** A trở về, **Then** liều giữ Mang theo, được đánh dấu "chờ ghi nhận sau khi trở về" và xuất hiện trong checklist của điều dưỡng phụ trách và bản nháp bàn giao cho tới khi được ghi Đã dùng hoặc Không thực hiện (FR-028).
7. **Given** A đang Điều trị tại bệnh viện, **When** Bộ lập lịch sinh liều cho ngày mai, **Then** liều được sinh ở trạng thái Tạm dừng; không có liều nào ở Đến giờ trong suốt thời gian A nằm viện.
8. **Given** A có đơn Donepezil 5 mg 1 viên lúc 20:00 nguồn gia đình gửi (lô còn 20 viên) và đơn Amlodipin nguồn viện lúc 08:00, **When** A được Cho tạm vắng 3 ngày cùng con gái có mang thuốc và điều dưỡng D ghi "Giao thuốc mang theo" 3 viên Donepezil và 3 viên Amlodipin cho con gái, **Then** trong cùng lệnh lô Donepezil còn 17 viên và feature 010 nhận yêu cầu tạo chi phí nháp 3 viên Amlodipin trỏ về lần giao (FR-048a); **When** A trở về sau 2 ngày, D ghi Đã dùng 2 liều mỗi thuốc theo báo lại và "Nhận lại" 1 viên mỗi thuốc, **Then** các liều Đã dùng không trừ số lượng hay tạo chi phí thêm, lô Donepezil còn 18 viên, feature 010 nhận yêu cầu giảm chi phí nháp 1 viên Amlodipin.
9. **Given** A được Cho tạm vắng 07:00–19:00 cùng con gái có mang thuốc, liều Amlodipin 08:00 và 20:00 của hôm đó Mang theo và thuộc lần giao 2 viên (đã tính phí 2 viên), **When** A trở về lúc 15:00, **Then** lần giao chuyển Chờ nhận lại, liều 20:00 chuyển Chưa đến giờ và thôi thuộc lần giao; **When** điều dưỡng D ghi Đã dùng liều 08:00 theo báo lại và "Nhận lại" 1 viên, **Then** chi phí nháp của lần giao giảm còn 1 viên; **When** D xác nhận Đã dùng liều 20:00 ở viện, **Then** feature 010 nhận yêu cầu tạo chi phí nháp 1 viên trỏ về liều 20:00; **Given** D xác nhận liều 20:00 trước khi ghi "Nhận lại", **Then** hệ thống nhắc ghi nhận lại nhưng không chặn (FR-048a, Q-60).

---

### User Story 6 - Dùng thuốc khi cần (PRN) có giới hạn khoảng cách và số lần (Priority: P2)

Đơn PRN không sinh lịch. Khi người cao tuổi cần, điều dưỡng ghi một lần dùng kèm lý do. Hệ thống chặn nếu chưa đủ khoảng cách tối thiểu từ lần dùng trước hoặc đã đạt số lần tối đa trong 24 giờ liền trước.

**Why this priority**: Thuốc khi cần (giảm đau, an thần, hạ sốt) có nguy cơ quá liều nếu không kiểm soát khoảng cách và tổng số lần (BR-M07-04).

**Independent Test**: Đơn PRN Paracetamol 500 mg, khoảng cách tối thiểu 4 giờ, tối đa 4 lần/ngày; ghi lần dùng lúc 08:00; thử lúc 11:00 và kiểm tra bị chặn; ghi lúc 12:00, 16:00, 20:00; thử lúc 23:59 và kiểm tra bị chặn do đạt 4 lần.

**Acceptance Scenarios**:

1. **Given** đơn PRN Paracetamol 500 mg của A Hiệu lực, **When** Bộ lập lịch sinh liều, **Then** không có liều nào được sinh từ đơn này (BR-M07-04).
2. **Given** A đau đầu, **When** điều dưỡng D ghi lần dùng lúc 08:00 mà không nhập lý do, **Then** hệ thống chặn; **When** nhập lý do "đau đầu, mức 5/10", **Then** lần dùng được lưu ở trạng thái Đã dùng với người dùng, thời điểm, liều, lý do.
3. **Given** lần dùng gần nhất lúc 08:00 và khoảng cách tối thiểu 4 giờ, **When** D ghi lần dùng lúc 11:00, **Then** hệ thống chặn và hiển thị thời điểm sớm nhất được dùng (12:00).
4. **Given** A đã có 4 lần dùng Paracetamol PRN lúc 08:00, 12:00, 16:00, 20:00 và số lần tối đa là 4, **When** D ghi lần thứ 5 lúc 00:30 hôm sau, **Then** hệ thống chặn dù đã sang ngày mới, vì 4 lần đều nằm trong 24 giờ liền trước; hệ thống hiển thị thời điểm sớm nhất được dùng là 08:00 và gợi ý báo bác sĩ (BR-M07-04, FR-025); **When** D ghi lúc 08:05 hôm sau, **Then** lần dùng được lưu vì lần 08:00 hôm trước đã ra khỏi khoảng 24 giờ.
5. **Given** đơn PRN nguồn viện cung cấp, **When** lần dùng được lưu, **Then** feature 010 nhận yêu cầu tạo chi phí nháp trỏ về lần dùng (BR-M07-14).
6. **Given** đơn PRN đang Tạm dừng hoặc A đang Tạm vắng, **When** D ghi lần dùng, **Then** hệ thống chặn.

---

### User Story 7 - Thuốc gia đình gửi: đối chiếu trước khi dùng và theo dõi số lượng (Priority: P2)

Điều dưỡng tiếp nhận thuốc gia đình gửi với tên thuốc, hàm lượng, số lượng, hạn dùng, bao bì, đơn thuốc/toa, người giao, người nhận. Thuốc ở trạng thái Chờ đối chiếu, không được dùng cho tới khi được đối chiếu với đơn hiện hành và xác nhận "Được sử dụng" (gắn với đơn) hoặc "Chỉ giữ hộ". Mỗi liều đã dùng từ nguồn này trừ số lượng; khi còn dưới CFG-M07-06 ngày dùng thì người thân được thông báo; thuốc hết hạn dùng bị chặn. Thuốc được hoàn trả hoặc hủy theo yêu cầu gia đình.

**Why this priority**: Thuốc không rõ nguồn gốc hoặc không có căn cứ là rủi ro an toàn (11.4); theo dõi số lượng giúp không bị gián đoạn thuốc và là điều kiện để kết thúc lưu trú (feature 004).

**Independent Test**: Tiếp nhận 30 viên Donepezil 5 mg có toa, hạn dùng còn 1 năm; kiểm tra Chờ đối chiếu và không gắn được vào lịch; đối chiếu với đơn Donepezil 5 mg 1 viên/ngày nguồn gia đình gửi; xác nhận 26 liều; kiểm tra số lượng còn 4 và người thân nhận thông báo; tiếp nhận một hộp không có toa và kiểm tra chỉ giữ hộ.

**Acceptance Scenarios**:

1. **Given** con gái của A mang tới 30 viên Donepezil 5 mg có toa của Bệnh viện X, hạn dùng 09/2027, **When** điều dưỡng D ghi nhận tiếp nhận đủ trường 11.4, **Then** thuốc ở trạng thái Chờ đối chiếu, số lượng còn 30, và không đơn nào gắn được với thuốc này cho tới khi đối chiếu (BR-M07-10).
2. **Given** thuốc Chờ đối chiếu và A có đơn Donepezil 5 mg 1 viên/ngày lúc 20:00 nguồn "gia đình gửi" Hiệu lực, **When** D đối chiếu và xác nhận "Được sử dụng" gắn với đơn đó, **Then** thuốc chuyển Được sử dụng và liều của đơn lấy nguồn từ thuốc này (11.4).
3. **Given** thuốc Được sử dụng còn 30 viên, **When** D xác nhận Đã dùng liều 20:00 (1 viên), **Then** số lượng còn 29 và biến động số lượng được lưu trỏ về liều (BR-M07-11).
4. **Given** số lượng còn 5 viên và đơn dùng 1 viên/ngày, **When** một liều nữa được xác nhận làm còn 4 viên (dưới CFG-M07-06, mặc định \[5 ngày\] dùng), **Then** feature 009 nhận yêu cầu thông báo người liên hệ chính và người đại diện của A một lần cho lần vượt ngưỡng này (BR-M07-11).
5. **Given** thuốc Được sử dụng có hạn dùng 30/09, **When** tới 01/10, **Then** thuốc chuyển Chỉ giữ hộ với lý do "hết hạn dùng", các liều dùng nguồn này bị chặn xác nhận Đã dùng, điều dưỡng phụ trách, người liên hệ chính và người đại diện của A được thông báo (BR-M07-11, FR-046).
6. **Given** một hộp thuốc không có toa, không rõ người kê, **When** D đối chiếu, **Then** D chỉ chọn được "Chỉ giữ hộ" kèm lý do "không có căn cứ sử dụng"; thuốc không được gắn vào lịch dùng (11.4, BR-M07-10).
7. **Given** thuốc Chỉ giữ hộ còn 10 viên, **When** D ghi "Hoàn trả" cho người thân thuộc danh sách người thân của A với số lượng 10, người nhận, người giao, thời điểm, **Then** thuốc chuyển Đã hoàn trả và không còn được tính là "thuốc gia đình gửi đang giữ" cho điều kiện kết thúc lưu trú (feature 004 FR-063 (c)).
8. **Given** liều Donepezil 20:00 Đến giờ và thuốc gia đình gửi gắn với đơn đã hết (số lượng 0), **When** D xác nhận Đã dùng, **Then** hệ thống chặn và gợi ý ghi Không thực hiện với lý do "hết thuốc gia đình gửi"; điều dưỡng phụ trách và bác sĩ được thông báo.
9. **Given** một người thân của A, dù có hay không có bản đồng ý chia sẻ dữ liệu, **When** người đó xem cổng người thân, **Then** người thân thấy danh sách thuốc gia đình gửi của A, trạng thái và số lượng còn (4.4 dòng "Đối chiếu thuốc, thuốc gia đình gửi": NT X), nhưng chỉ thấy đơn thuốc và phiếu đối chiếu đã xác nhận khi có bản đồng ý chia sẻ dữ liệu đang hiệu lực bao gồm người thân đó (4.4 chú thích ¹, DBR-03, FR-049).

---

### User Story 8 - Đính chính liều đã xác nhận và tra lịch sử thuốc (Priority: P3)

Liều đã xác nhận không sửa hay xóa. Khi xác nhận nhầm, người có quyền tạo bản đính chính có lý do; bản gốc vẫn giữ. Bác sĩ, điều dưỡng, trưởng tầng và quản lý viện tra được lịch sử đơn thuốc (chuỗi thay thế) và lịch sử liều của một người cao tuổi theo phạm vi.

**Why this priority**: Toàn vẹn hồ sơ thuốc (BR-M15-03, constitution III); không chặn vận hành hằng ngày nhưng cần cho truy vết khi có sự cố.

**Independent Test**: Xác nhận nhầm liều của A là Đã dùng; thử sửa trực tiếp và kiểm tra bị chặn; tạo đính chính "Không thực hiện" có lý do; kiểm tra trạng thái hiện hành, bản gốc còn xem được, chi phí nháp được điều chỉnh và số lượng thuốc gia đình gửi được hoàn lại.

**Acceptance Scenarios**:

1. **Given** liều 08:00 của A đã Đã dùng, **When** D sửa trực tiếp trạng thái hoặc thời điểm, **Then** hệ thống chặn (BR-M07-12, BR-M15-03).
2. **Given** liều trên nguồn gia đình gửi, **When** D (người xác nhận gốc) tạo đính chính "Không thực hiện" với lý do "xác nhận nhầm người cao tuổi", **Then** bản đính chính trỏ bản gốc (DBR-23), trạng thái hiện hành của liều là Không thực hiện, số lượng thuốc gia đình gửi được cộng lại 1 kèm biến động trỏ về bản đính chính, bản gốc vẫn hiển thị trong lịch sử.
3. **Given** liều nguồn viện đã tạo chi phí nháp, **When** liều bị đính chính khỏi Đã dùng, **Then** feature 010 nhận sự kiện để hủy chi phí nháp hoặc tạo khoản điều chỉnh nếu kỳ đã chốt (DBR-17).
4. **Given** liều Bỏ lỡ do hệ thống chuyển, nhưng D thực tế đã cho A uống lúc 08:20 mà quên ghi, **When** D (điều dưỡng phụ trách A trong ca của liều) tạo đính chính "Đã dùng lúc 08:20" có lý do, **Then** trạng thái hiện hành là Đã dùng kèm nhãn "dùng trễ" hoặc đúng giờ theo thời điểm dùng, và feature 007 nhận yêu cầu đóng cảnh báo bỏ lỡ liên quan.
5. **Given** nhân viên chăm sóc hoặc trưởng tầng, **When** tạo đính chính cho liều, **Then** hệ thống chặn (FR-030).
6. **Given** đơn Amlodipin 10 mg thay thế cho 5 mg, 5 mg thay thế cho 2,5 mg, **When** bác sĩ mở lịch sử đơn của A, **Then** thấy chuỗi thay thế theo thời gian, người kê, lý do từng lần.

---

### Edge Cases

- **Đổi liều có hiệu lực trong tương lai** (ví dụ từ 07:00 ngày mai): đơn cũ giữ Hiệu lực tới thời điểm đó rồi chuyển Đã ngừng; đơn thay thế Chờ hiệu lực; liều đơn cũ từ thời điểm hiệu lực chuyển Đã hủy và liều đơn mới được sinh ngay khi lệnh được lưu (FR-016, FR-018).
- **Đổi liều khi đơn đang Tạm dừng chờ đối chiếu**: không dùng lệnh đổi liều thông thường; quyết định đi qua phiếu đối chiếu (FR-037).
- **Hai điều dưỡng cùng xác nhận một liều gần như đồng thời**: chỉ lần đầu được chấp nhận; lần sau bị từ chối và được báo liều đã xác nhận bởi ai (DBR-13).
- **Liều Đến giờ lúc người cao tuổi rời viện**: liều không tự chuyển Tạm dừng (sơ đồ 11.2 chỉ cho Chưa đến giờ → Tạm dừng); điều dưỡng xác nhận Đã dùng trước khi đi hoặc ghi Không thực hiện có lý do "vắng mặt".
- **Người cao tuổi chuyển lại bệnh viện khi phiếu đối chiếu chưa xác nhận**: phiếu cũ chuyển Đã hủy (lý do "chuyển viện lại"); khi trở về, phiếu mới được tạo, danh sách hiện có vẫn là các đơn đang Tạm dừng (FR-035).
- **Người cao tuổi chuyển trạng thái cuối** (Kết thúc lưu trú, Qua đời, Hủy tiếp nhận): đơn chưa kết thúc chuyển Đã ngừng, liều chưa xác nhận chuyển Đã hủy hoặc Không thực hiện (Hệ thống), phiếu đối chiếu đang mở chuyển Đã hủy; thuốc gia đình gửi giữ nguyên để hoàn trả (FR-047). Ghi nhận qua đời với thời điểm trong quá khứ: liều đã xác nhận sau thời điểm qua đời được đánh dấu "cần rà soát", không tự xóa (feature 004).
- **Đơn định kỳ có ngày kết thúc trước lần sinh kế tiếp**: không sinh liều sau thời điểm kết thúc; liều cuối cùng là liều có thời điểm dự kiến không muộn hơn cuối ngày kết thúc.
- **Mốc giờ của đơn rơi đúng lúc chuyển ngày hoặc đổi cấu hình cửa sổ**: cửa sổ được chốt khi sinh liều; đổi CFG-M07-01 hay cửa sổ riêng của thuốc chỉ áp cho liều sinh sau đó (feature 000).
- **Thuốc gia đình gửi gắn với đơn bị đổi liều**: nếu đơn thay thế cùng thuốc và nguồn gia đình gửi, liên kết chuyển sang đơn thay thế trong cùng lệnh; nếu không, thuốc chuyển Chỉ giữ hộ (FR-043).
- **Hai lô thuốc gia đình gửi cho cùng một đơn**: khi xác nhận liều, hệ thống đề xuất lô có hạn dùng sớm nhất còn số lượng; điều dưỡng có thể chọn lô khác (FR-044).
- **Số lượng thuốc gia đình gửi thực tế khác số trên hệ thống** (rơi vỡ, đếm lại): điều chỉnh bằng lệnh "Kiểm kê điều chỉnh" có lý do, lưu biến động; không sửa trực tiếp (FR-045).
- **Thuốc gia đình gửi hết hạn dùng khi đang giữ hộ**: vẫn giữ hộ cho tới khi hoàn trả hoặc hủy theo yêu cầu gia đình.
- **Thuốc PRN dùng trong khi Hoạt động bên ngoài hoặc Tạm vắng có mang thuốc**: người ghi theo FR-027. Điều dưỡng đi cùng ghi tại chỗ và bị chặn bởi giới hạn khoảng cách, số lần như ở viện; lần dùng "ghi theo báo lại" là việc đã xảy ra nên vẫn được lưu kể cả khi vượt giới hạn, nhưng gắn dấu "vượt giới hạn" và feature 007 nhận yêu cầu tạo cảnh báo mức trung bình (FR-027).
- **Tạm vắng cùng người thân có mang thuốc mà người thân không báo lại khi trở về**: điều dưỡng phụ trách ghi Không thực hiện với lý do "không có thông tin từ người thân"; liều không bị tính Bỏ lỡ.
- **Xác nhận liều ngoại tuyến**: được phép theo Q-01 (mặc định), trừ thuốc có đánh dấu kiểm soát đặc biệt bắt buộc trực tuyến (8.6) — mất kết nối thì vẫn cho dùng và ghi trực tuyến ngay khi có mạng với nhãn "ghi sau mất kết nối", luôn chờ xem lại (FR-029); trạng thái liều xét theo thời điểm dùng ghi trên thiết bị; bản ghi quá CFG-M15-08 chuyển "chờ xem lại" (feature 002).
- **Đơn có ngày bắt đầu trước hôm nay khi nhập đơn bên ngoài**: đơn Hiệu lực ngay khi lưu; không sinh liều cho quá khứ.

## Requirements *(mandatory)*

### Functional Requirements

Mọi yêu cầu dưới đây kế thừa feature 000: nhóm dữ liệu (mục 1.5), lý do bắt buộc cho lệnh nhóm 2, đính chính cho bản ghi đã xác nhận, nhật ký cho mọi thay đổi, tham số theo mã CFG, "hoặc toàn bộ, hoặc không" khi một lệnh có nhiều tác động, mốc thời gian theo Asia/Ho_Chi_Minh (DBR-25), danh sách bản ghi bảo vệ (BR-M15-03, feature 000 FR-010). Phân nhóm dữ liệu của feature này:

| Dữ liệu | Nhóm (1.5) | Thao tác được phép |
| --- | --- | --- |
| Danh mục thuốc | 1 – Danh mục | Tạo, sửa, Ngừng hiệu lực; không xóa khi đã được đơn tham chiếu |
| Đơn thuốc | 2 | Chỉ qua lệnh ở bảng FR-011; chỉ sửa được ghi chú hướng dẫn (FR-014) |
| Liều thuốc (trước khi xác nhận) | 2 – do hệ thống điều khiển | Chỉ qua sự kiện và lệnh ở bảng FR-021 |
| Kết quả xác nhận liều, lần dùng PRN | 3 | Chỉ ghi thêm; sai sót xử lý bằng đính chính (thuộc danh sách bảo vệ BR-M15-03) |
| Phiếu đối chiếu thuốc | 2 khi Mở, Chờ xác nhận; 3 khi Đã xác nhận | Chỉ qua lệnh ở bảng FR-035; phiếu Đã xác nhận chỉ đính chính |
| Thuốc gia đình gửi | 2 | Chỉ qua lệnh ở bảng FR-041 |
| Biến động số lượng thuốc gia đình gửi, lần tiếp nhận, hoàn trả, hủy | 3 | Chỉ ghi thêm |
| Lần giao thuốc mang theo, lần nhận lại | 3 | Chỉ ghi thêm; sai sót xử lý bằng đính chính |

#### A. Danh mục thuốc

- **FR-001**: Hệ thống MUST có danh mục thuốc, mỗi thuốc gồm: tên, một hoặc nhiều hoạt chất (liên kết danh mục dị nguyên của feature 001 FR-018a), hàm lượng, dạng bào chế, đường dùng mặc định, đơn vị dùng, cửa sổ thời gian riêng (nếu có, thay cho CFG-M07-01), đánh dấu "kiểm soát đặc biệt", trạng thái hiệu lực. *(Nguồn: 11.1, 11.2, 8.6, 1.5 nhóm 1)*
- **FR-002**: Chỉ Quản lý viện MUST cấu hình được danh mục thuốc; thuốc đã có đơn tham chiếu MUST NOT bị xóa, chỉ Ngừng hiệu lực; thuốc Ngừng hiệu lực MUST NOT được chọn cho đơn mới nhưng đơn đang có vẫn giữ nguyên. Sửa hoạt chất hoặc cửa sổ riêng MUST chỉ áp cho đơn tạo sau và liều sinh sau. *(Nguồn: 1.5 nhóm 1; giả định về quyền, xem Điểm cần báo lại 7)*
- **FR-003**: Đơn giá thuốc nguồn viện cung cấp MUST NOT được quản lý ở spec này. Thuốc nguồn viện có mục trong danh mục vật phẩm với phiên bản đơn giá do Quản lý viện quản lý (feature 004 FR-034, FR-035); feature 010 lấy đơn giá theo DBR-16. Mỗi thuốc nguồn viện trong danh mục thuốc của spec này MUST liên kết được tới mục vật phẩm tương ứng, để khoản chi phí mang mã vật phẩm (feature 010 FR-019). *(Nguồn: 6.4, 15.2, BR-M11-01; đồng bộ spec 010)*

#### B. Đơn thuốc

- **FR-004**: Mỗi đơn thuốc MUST gồm: người cao tuổi, thuốc (từ danh mục), hoạt chất, liều (số lượng và đơn vị dùng), đường dùng, loại đơn (định kỳ / khi cần – PRN), ngày bắt đầu, ngày kết thúc (nếu có), nguồn thuốc (viện cung cấp / gia đình gửi), người kê, cơ sở kê, ngày kê, ghi chú hướng dẫn, trạng thái, liên kết "thay thế cho" (nếu có). Với đơn định kỳ MUST có thêm tần suất (các mốc giờ trong ngày, hoặc mỗi N giờ từ một mốc, hoặc theo ngày trong tuần kèm mốc giờ). Với đơn PRN MUST có thêm khoảng cách tối thiểu giữa hai lần và số lần tối đa mỗi ngày. *(Nguồn: 11.1, DON_THUOC, UC-39)*
- **FR-005**: Đơn nội bộ (cơ sở kê là viện) MUST chỉ được tạo bởi Bác sĩ đáp ứng BR-M06-05 tại thời điểm tạo (cơ sở có phạm vi khám chữa bệnh còn hiệu lực và bác sĩ có giấy phép còn hiệu lực đúng phạm vi, feature 002 FR-036, FR-037). *(Nguồn: BR-M07-05, BR-M06-05, 4.4 chú thích ³)*
- **FR-006**: Đơn từ cơ sở kê bên ngoài MUST được nhập bởi Bác sĩ hoặc Điều dưỡng và MUST có tên cơ sở kê, người kê, ngày kê; MAY đính kèm ảnh đơn/toa. Khi cơ sở không có phạm vi khám chữa bệnh còn hiệu lực, mọi đơn MUST là đơn bên ngoài. *(Nguồn: BR-M07-05, 4.4 chú thích ³ ⁴)*
- **FR-007**: Trước khi lưu một đơn mới (kể cả đơn thay thế và đơn tạo từ phiếu đối chiếu), hệ thống MUST kiểm tra: (a) trùng hoạt chất với mọi đơn Hiệu lực, Tạm dừng, Chờ hiệu lực của cùng người cao tuổi, trừ đơn đang được thay thế; (b) xung đột giữa hoạt chất của thuốc với các mục dị ứng đang Hiệu lực chọn từ danh mục dị nguyên. Có vấn đề thì hệ thống MUST hiển thị từng vấn đề (đơn trùng, mục dị ứng và mức độ) và MUST chỉ lưu khi người nhập ghi lý do xác nhận cho từng vấn đề; lý do và danh sách vấn đề MUST được lưu cùng đơn. *(Nguồn: BR-M07-06, BR-M01-07)*
- **FR-008**: Mục dị ứng loại "khác" (không chọn từ danh mục) MUST được hiển thị cho người nhập kèm dấu "không kiểm tra tự động" mỗi khi nhập đơn mới; hệ thống MUST NOT chặn vì mục này. *(Nguồn: feature 001 FR-018a, BR-M07-06)*
- **FR-009**: Đơn nguồn "gia đình gửi" MUST được lưu được khi chưa có thuốc gia đình gửi Được sử dụng; khi đó đơn MUST mang dấu "chưa có thuốc" và liều của đơn MUST NOT xác nhận Đã dùng được cho tới khi có thuốc gia đình gửi Được sử dụng gắn với đơn (FR-041). *(Nguồn: 11.4, BR-M07-10)*
- **FR-010**: Với đơn PRN, khoảng cách tối thiểu MUST lớn hơn 0 và số lần tối đa mỗi ngày MUST ít nhất là 1; đơn PRN MUST NOT sinh liều. *(Nguồn: 11.1, BR-M07-04)*
- **FR-011**: Vòng đời đơn thuốc MUST theo bảng dưới; không có chuyển nào khác. *(Nguồn: 11.1, UC-39, UC-40, BR-M07-05 → 09, DBR-14, constitution IV)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái mới | Người thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Nhập đơn | Hiệu lực (ngày bắt đầu không sau hôm nay) hoặc Chờ hiệu lực | Bác sĩ (FR-005, FR-006); Điều dưỡng chỉ đơn bên ngoài (FR-006) | Người cao tuổi Đang lưu trú, Tạm vắng hoặc Điều trị tại bệnh viện và không có phiếu đối chiếu đang Mở/Chờ xác nhận (FR-037); đủ trường FR-004; qua kiểm tra FR-007 | Sinh liều (FR-018) |
| Chờ hiệu lực | Tới ngày giờ bắt đầu | Hiệu lực | Bộ lập lịch | Không mang dấu "chờ tiếp nhận" | — |
| — | Phiếu "tiếp nhận" lập trước được xác nhận (FR-034a) | Chờ hiệu lực (dấu "chờ tiếp nhận") | Hệ thống | Hồ sơ Đang tiếp nhận | Không sinh liều |
| Chờ hiệu lực (dấu "chờ tiếp nhận") | Hoàn tất tiếp nhận | Hiệu lực | Hệ thống | — | Ngày giờ bắt đầu = thời điểm lệnh; sinh liều (FR-018) |
| Hiệu lực | Tạm dừng đơn | Tạm dừng | Bác sĩ; Điều dưỡng chỉ khi đơn bên ngoài và có căn cứ từ cơ sở kê | Bắt buộc lý do | Liều Chưa đến giờ từ thời điểm tạm dừng chuyển Tạm dừng |
| Hiệu lực | Tiếp nhận hoặc trở về từ bệnh viện (BR-M07-09) | Tạm dừng | Hệ thống | — | Lý do "chờ đối chiếu"; gắn vào phiếu đối chiếu (FR-034) |
| Tạm dừng | Tiếp tục đơn | Hiệu lực | Như "Tạm dừng đơn" | Đơn không đang chờ đối chiếu; bắt buộc lý do | Liều Tạm dừng có thời điểm dự kiến từ lúc tiếp tục trở lại Chưa đến giờ (FR-021) |
| Tạm dừng | Phiếu đối chiếu được xác nhận với quyết định Tiếp tục | Hiệu lực | Hệ thống | — | Như trên |
| Hiệu lực, Tạm dừng, Chờ hiệu lực | Ngừng đơn | Đã ngừng | Bác sĩ; Điều dưỡng chỉ khi đơn bên ngoài và có căn cứ từ cơ sở kê | Bắt buộc lý do; thời điểm ngừng không trước thời điểm lệnh | Hủy liều từ thời điểm ngừng (BR-M07-07) |
| Hiệu lực, Tạm dừng, Chờ hiệu lực | Đổi liều / đổi thuốc (FR-016) hoặc quyết định Thay đổi liều trên phiếu đối chiếu | Đã ngừng | Như "Ngừng đơn" (với đơn thay thế là đơn nội bộ: FR-005) | Đơn thay thế qua FR-007; cùng một lệnh | Lý do "thay thế"; đơn thay thế trỏ về đơn này (DBR-14) |
| Tạm dừng | Phiếu đối chiếu được xác nhận với quyết định Ngừng | Đã ngừng | Hệ thống | — | Như "Ngừng đơn" |
| Hiệu lực, Tạm dừng | Hết ngày kết thúc | Hết hạn | Bộ lập lịch | Không được gia hạn | Không còn liều sau thời điểm kết thúc (BR-M07-08) |
| Hiệu lực, Tạm dừng, Chờ hiệu lực | Người cao tuổi chuyển trạng thái cuối | Đã ngừng | Hệ thống | — | Lý do là trạng thái cuối; FR-047 |

- **FR-012**: Khi feature 001 ghi thêm một mục dị ứng từ danh mục (feature 001 FR-020), hệ thống MUST kiểm tra mọi đơn Hiệu lực, Tạm dừng, Chờ hiệu lực của người cao tuổi đó và MUST yêu cầu feature 007 tạo một cảnh báo cho mỗi đơn xung đột; đơn MUST NOT tự ngừng. *(Nguồn: BR-M01-07, BR-M07-06)*
- **FR-013**: Đã ngừng và Hết hạn là trạng thái cuối; đơn ở trạng thái cuối MUST NOT chuyển sang trạng thái khác. Mọi lệnh ở bảng FR-011 MUST lưu người thực hiện, thời điểm, lý do (khi bảng yêu cầu) và căn cứ (đơn nội bộ, đơn bên ngoài kèm cơ sở kê). *(Nguồn: 11.1 "mọi thay đổi phải lưu lịch sử", 1.5)*
- **FR-014**: Đơn ở Hiệu lực, Tạm dừng, Chờ hiệu lực MUST NOT sửa được thuốc, hoạt chất, liều, đường dùng, tần suất, mốc giờ, loại đơn, khoảng cách tối thiểu, số lần tối đa, nguồn thuốc hay ngày bắt đầu; chỉ ghi chú hướng dẫn MUST sửa được, bởi người có quyền nhập đơn đó, và MUST lưu lịch sử giá trị trước/sau. *(Nguồn: 11.1, DBR-14)*
- **FR-015**: Lệnh "Gia hạn" MUST cho Bác sĩ (với đơn nội bộ: đáp ứng FR-005; với đơn bên ngoài: có căn cứ từ cơ sở kê, Điều dưỡng cũng được thực hiện) đặt ngày kết thúc mới muộn hơn ngày kết thúc hiện tại, hoặc bỏ ngày kết thúc, kèm lý do; liều được sinh tiếp theo ngày kết thúc mới. Rút ngắn ngày kết thúc MUST dùng lệnh Ngừng đơn với thời điểm ngừng trong tương lai. *(Nguồn: BR-M07-08; giả định: ngày kết thúc không thuộc nhóm trường bị khóa ở 11.1)*
- **FR-016**: Lệnh "Đổi liều / đổi thuốc" MUST tạo trong cùng một lần: đơn thay thế với thời điểm hiệu lực do người kê chọn (không trước thời điểm lệnh), liên kết "thay thế cho" đơn cũ; đơn cũ chuyển Đã ngừng tại đúng thời điểm hiệu lực của đơn thay thế. Nếu một phần thất bại (ví dụ đơn thay thế không qua FR-007 vì thiếu lý do) thì không phần nào được áp dụng. *(Nguồn: 11.1, DBR-14, BR-M07-07, feature 000)*
- **FR-016a**: Mỗi đơn MUST bị thay thế bởi tối đa một đơn; lịch sử đơn của người cao tuổi MUST hiển thị được chuỗi thay thế theo thời gian với người kê và lý do từng lần. *(Nguồn: DBR-14, UC-39)*
- **FR-017**: Người kê (đơn nội bộ) hoặc các Bác sĩ của cơ sở và Điều dưỡng phụ trách người cao tuổi (đơn bên ngoài) MUST được nhắc khi đơn còn CFG-M07-04 (mặc định \[3 ngày\]) tới ngày kết thúc; hết ngày kết thúc mà không được gia hạn thì đơn MUST tự chuyển Hết hạn. *(Nguồn: BR-M07-08, CFG-M07-04)*

#### C. Sinh liều và vòng đời liều

- **FR-018**: Mỗi ngày Bộ lập lịch MUST sinh liều cho CFG-M07-03 (mặc định \[2 ngày\]) tới từ mọi đơn định kỳ ở Hiệu lực, Chờ hiệu lực (trừ đơn mang dấu "chờ tiếp nhận", FR-034a) hoặc Tạm dừng, chỉ cho các thời điểm dự kiến nằm trong khoảng [ngày giờ bắt đầu, cuối ngày kết thúc] của đơn. Khi đơn mới được lưu, được gia hạn, được tiếp tục hoặc được tạo từ phiếu đối chiếu, liều MUST được sinh ngay cho khoảng đó mà không chờ lần chạy kế tiếp. Không sinh liều cho thời điểm đã qua. *(Nguồn: BR-M07-01, CFG-M07-03)*
- **FR-019**: Mỗi liều MUST là duy nhất theo (đơn thuốc, thời điểm dự kiến); chạy lại việc sinh MUST NOT tạo trùng. *(Nguồn: DBR-13, NFR-04)*
- **FR-020**: Mỗi liều MUST có: người cao tuổi, đơn thuốc, thuốc, liều, đường dùng, hướng dẫn, thời điểm dự kiến, cửa sổ thời gian (cửa sổ riêng của thuốc nếu có, nếu không thì CFG-M07-01, mặc định \[±30 phút\]; chốt tại lúc sinh), nguồn thuốc, trạng thái, điều dưỡng phụ trách theo phân công của ca (feature 008), và sau khi xác nhận: người xác nhận, thời điểm dùng, thời điểm ghi (trên thiết bị và đồng bộ), phản ứng, lý do, ghi chú, nhãn "dùng trễ", lô thuốc gia đình gửi (nếu có). *(Nguồn: 11.2, 11.3, LIEU_THUOC, CFG-M07-01)*
- **FR-021**: Vòng đời liều MUST theo bảng dưới; không có chuyển nào khác. *(Nguồn: 11.2 sơ đồ, 11.3, BR-M07-02, 03, 07, 09, 12, BR-M04-16, constitution IV)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái mới | Người thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Sinh liều | Chưa đến giờ | Bộ lập lịch | FR-018; tại thời điểm dự kiến người cao tuổi không trong khoảng vắng đã biết và đơn Hiệu lực hoặc Chờ hiệu lực | — |
| — | Sinh liều | Tạm dừng | Bộ lập lịch | Đơn Tạm dừng, hoặc người cao tuổi đang Tạm vắng không mang thuốc hay Điều trị tại bệnh viện | — |
| Chưa đến giờ | Tới đầu cửa sổ | Đến giờ | Bộ lập lịch | — | Liều nổi lên trong checklist điều dưỡng phụ trách (feature 005 FR-029) |
| Chưa đến giờ | Người cao tuổi Tạm vắng không mang thuốc, Điều trị tại bệnh viện; đơn Tạm dừng | Tạm dừng | Hệ thống | Thời điểm dự kiến từ lúc rời đi / tạm dừng | Không nhắc, không cảnh báo (BR-M07-03) |
| Chưa đến giờ | Điểm danh rời viện của chuyến đi; Tạm vắng có mang thuốc | Mang theo | Hệ thống | Thời điểm dự kiến trong khoảng đi dự kiến | Hiển thị trong danh sách thuốc cần mang (8.9) (BR-M07-03, BR-M04-16) |
| Tạm dừng | Trở về từ Tạm vắng; đơn Tiếp tục; phiếu đối chiếu xác nhận Tiếp tục | Chưa đến giờ, hoặc Đến giờ nếu thời điểm đó đã trong cửa sổ | Hệ thống | Cuối cửa sổ chưa qua; đơn của liều đang Hiệu lực. Trở về từ Điều trị tại bệnh viện không khôi phục liều: liều giữ Tạm dừng tới khi phiếu đối chiếu được xác nhận (FR-034, BR-M07-09) | — |
| Mang theo | Người cao tuổi trở về trước cuối cửa sổ, liều chưa được ghi nhận | Chưa đến giờ, hoặc Đến giờ nếu đã trong cửa sổ | Hệ thống | — | Liều quay lại checklist điều dưỡng và thôi thuộc lần giao thuốc mang theo (nếu có); khi xác nhận ở viện thì trừ số lượng và tạo chi phí như bình thường (FR-048a) |
| Chưa đến giờ, Đến giờ, Tạm dừng, Mang theo (chưa ghi nhận) | Ngừng đơn, đổi liều, Hết hạn, quyết định Ngừng/Thay đổi liều trên phiếu | Đã hủy | Hệ thống | Thời điểm dự kiến từ thời điểm hiệu lực của thay đổi | Lý do theo nguyên nhân; liều thay thế được sinh (BR-M07-07) |
| Chưa đến giờ, Tạm dừng, Mang theo (chưa ghi nhận) | Người cao tuổi chuyển trạng thái cuối | Đã hủy | Hệ thống | — | FR-047 |
| Đến giờ | Xác nhận Đã dùng | Đã dùng | Điều dưỡng (FR-026) | Thời điểm dùng trong cửa sổ; FR-044 nếu nguồn gia đình gửi; FR-009 | Chi phí nháp nếu nguồn viện (BR-M07-14); trừ số lượng nếu nguồn gia đình gửi (BR-M07-11); FR-031 nếu có phản ứng |
| Đến giờ | Ghi Từ chối | Từ chối | Điều dưỡng (FR-026) | Bắt buộc lý do | Kiểm tra BR-M07-13 (FR-032) |
| Đến giờ | Ghi Không thực hiện | Không thực hiện | Điều dưỡng (FR-026) | Bắt buộc lý do | — |
| Đến giờ | Hết cửa sổ | Trễ | Bộ lập lịch | Chưa xác nhận | Nhắc điều dưỡng phụ trách (BR-M07-02) |
| Trễ | Xác nhận Đã dùng | Đã dùng (nhãn "dùng trễ") | Điều dưỡng (FR-026) | Thời điểm dùng sau cửa sổ và không sau thời điểm ghi | Như Đã dùng |
| Trễ | Ghi Từ chối / Không thực hiện | Từ chối / Không thực hiện | Điều dưỡng (FR-026) | Bắt buộc lý do | Như trên (bổ sung so với sơ đồ 11.2, xem Điểm cần báo lại 2) |
| Trễ | Quá CFG-M07-02 (mặc định \[30 phút\]) từ lúc Trễ | Bỏ lỡ | Bộ lập lịch | Chưa xác nhận | Cảnh báo mức trung bình (feature 007) (BR-M07-02) |
| Mang theo | Ghi nhận Đã dùng | Đã dùng | Điều dưỡng theo FR-027 | Thời điểm dùng trong khoảng đi; với "ghi theo báo lại": có người báo lại | Như Đã dùng; căn cứ "mang theo" hoặc "ghi theo báo lại" |
| Mang theo | Ghi nhận Không thực hiện | Không thực hiện | Điều dưỡng theo FR-027 | Bắt buộc lý do | — |
| Đến giờ, Trễ | Người cao tuổi chuyển trạng thái cuối | Không thực hiện | Hệ thống | — | Lý do là trạng thái cuối; không nhắc, không cảnh báo, không vào bàn giao (FR-047) |

- **FR-022**: Đã dùng, Từ chối, Không thực hiện, Bỏ lỡ, Đã hủy là trạng thái cuối. Liều Tạm dừng có cuối cửa sổ đã qua mà vẫn Tạm dừng MUST được coi là kết thúc ở trạng thái Tạm dừng: không tính Bỏ lỡ, không cảnh báo, không vào bàn giao như liều bị bỏ, và MUST NOT chuyển trạng thái nữa. Mỗi lần đóng liều MUST ghi người đóng là nhân viên (kèm căn cứ quyền) hoặc "Hệ thống" (kèm quy tắc). *(Nguồn: 11.2, BR-M07-03, BR-M07-12)*
- **FR-023**: Liều đã ở Đến giờ hoặc Trễ lúc người cao tuổi rời viện tạm thời MUST NOT tự chuyển Tạm dừng hay Mang theo; điều dưỡng MUST đóng liều bằng xác nhận Đã dùng hoặc ghi Không thực hiện có lý do "vắng mặt", nếu không liều đi tiếp tới Bỏ lỡ. *(Nguồn: 11.2 sơ đồ: chỉ Chưa đến giờ → Tạm dừng / Mang theo)*

#### D. Thuốc khi cần (PRN)

- **FR-024**: Điều dưỡng MUST ghi được một lần dùng PRN cho đơn PRN Hiệu lực của người cao tuổi đang có mặt tại viện, với: thời điểm dùng, liều (không vượt liều của đơn), lý do (bắt buộc), phản ứng, ghi chú, lô thuốc gia đình gửi (nếu nguồn gia đình gửi). Lần dùng được lưu ở trạng thái Đã dùng và thuộc nhóm 3. *(Nguồn: BR-M07-04, UC-42, 11.3)*
- **FR-025**: Hệ thống MUST chặn lần dùng PRN khi: (a) khoảng cách từ lần dùng Đã dùng gần nhất của cùng đơn (kể cả đơn mà đơn này thay thế) nhỏ hơn khoảng cách tối thiểu; hoặc (b) số lần Đã dùng của cùng đơn (kể cả đơn mà đơn này thay thế) trong 24 giờ liền trước thời điểm dùng mới đã bằng số lần tối đa. "Số lần tối đa mỗi ngày" MUST được hiểu là số lần tối đa trong 24 giờ trượt, không phải theo ngày dương lịch; lần dùng đã bị đính chính khỏi Đã dùng không được đếm. Khi chặn, hệ thống MUST hiển thị thời điểm sớm nhất được dùng (thời điểm muộn hơn trong hai mốc: lần gần nhất + khoảng cách tối thiểu; lần cũ nhất trong 24 giờ + 24 giờ) và gợi ý báo bác sĩ; hệ thống MUST NOT có thao tác vượt chặn. *(Nguồn: BR-M07-04; Clarification 2026-09-26, đề xuất Q-56)*
- **FR-025a**: Lần dùng PRN nguồn viện cung cấp MUST kích hoạt chi phí nháp như liều Đã dùng (BR-M07-14); lần dùng nguồn gia đình gửi MUST trừ số lượng như FR-044. Lần dùng PRN ghi sai MUST được xử lý bằng đính chính như FR-030. *(Nguồn: BR-M07-11, BR-M07-14, BR-M07-12)*

#### E. Phát thuốc, xác nhận, cảnh báo và đính chính

- **FR-026**: Xác nhận liều và ghi lần dùng PRN MUST chỉ được thực hiện bởi Điều dưỡng: (a) có giấy phép còn hiệu lực theo danh mục nghiệp vụ chuyên môn "phát thuốc và xác nhận liều" (feature 002 FR-036, 13.4); (b) được phân công người cao tuổi đó trong ca, hoặc giữ nhiệm vụ Người phụ trách ca của tầng/khu vực đó, hoặc — khi liều là "liều chung của tầng" — có ca tại tầng/khu vực đó, trong khoảng phạm vi của ca (feature 002 FR-032). Trưởng tầng, Bác sĩ, Quản lý viện MUST chỉ xem.
- **FR-026a**: Khi người cao tuổi không có điều dưỡng được phân công trong ca (chưa phân công, điều dưỡng vắng ca hoặc nghỉ việc, feature 008), liều của người đó trong ca MUST trở thành "liều chung của tầng": hiển thị cho mọi Điều dưỡng có ca tại tầng/khu vực đó, và bất kỳ ai trong số họ đáp ứng FR-026 (a) MUST xác nhận được, với căn cứ "liều chung của tầng". Nhắc Trễ của liều chung MUST gửi cho các Điều dưỡng đó và Người phụ trách ca. Trưởng tầng và Người phụ trách ca MUST được thông báo một lần mỗi ca về người cao tuổi chưa có điều dưỡng phụ trách. Khi có phân công mới trong ca, liều chưa đóng MUST chuyển về điều dưỡng được phân công. Từ giờ bắt đầu ca sau, nếu bàn giao chưa được xác nhận (feature 008 FR-044a), liều Mang theo chờ ghi nhận và liều chưa đóng mà điều dưỡng được giao thuộc ca trước và không có tên trong ca nào đang diễn ra MUST cũng trở thành liều chung của tầng theo quy tắc này, mang dấu "tồn, chờ xác nhận bàn giao"; khi bàn giao được xác nhận, FR-028 áp như bình thường. *(Nguồn: 13.4, BR-M07-02, feature 005 FR-024; Clarification 2026-09-26, đề xuất Q-61; feature 008 Q-85)* *(Nguồn: 4.4 dòng "Phát thuốc, thuốc khi cần": ĐD T, TT X, BS X, QL X; BR-M07-12, 13.4)*
- **FR-027**: Liều Mang theo và lần dùng PRN trong khi Hoạt động bên ngoài hoặc Tạm vắng có mang thuốc MUST chỉ được ghi nhận bởi Điều dưỡng đáp ứng FR-026 (a): (a) Điều dưỡng đi cùng chuyến ghi tại chỗ với căn cứ "mang theo"; (b) nếu không có Điều dưỡng đi cùng (kể cả Tạm vắng cùng người thân), Điều dưỡng phụ trách người cao tuổi trong ca ghi sau khi người cao tuổi trở về, với căn cứ "ghi theo báo lại" và bắt buộc ghi người báo lại (nhân viên đi cùng, hoặc người thân kèm quan hệ). Nhân viên không phải Điều dưỡng MUST NOT ghi liều, kể cả khi là người đi cùng hay trưởng đoàn. Liều Mang theo chưa được ghi nhận khi người cao tuổi đã trở về sau cuối cửa sổ MUST được đánh dấu "chờ ghi nhận sau khi trở về". Lần dùng PRN "ghi theo báo lại" MUST được lưu kể cả khi vượt khoảng cách tối thiểu hoặc số lần tối đa (FR-025 chỉ chặn việc dùng tại chỗ), nhưng MUST gắn dấu "vượt giới hạn" và yêu cầu feature 007 tạo cảnh báo mức trung bình. *(Nguồn: BR-M07-03, BR-M04-16, 8.9, 4.4; Clarification 2026-09-26, đề xuất Q-54)*
- **FR-028**: Liều Trễ, Bỏ lỡ, Từ chối và liều Mang theo "chờ ghi nhận sau khi trở về" trong ca MUST được cung cấp cho bản nháp bàn giao (feature 008, BR-M09-06); liều Mang theo chờ ghi nhận MUST hiển thị trong checklist của điều dưỡng phụ trách cho tới khi được ghi. *(Nguồn: BR-M09-06, 13.5)*
- **FR-029**: Xác nhận liều của thuốc có đánh dấu "kiểm soát đặc biệt" MUST thực hiện trực tuyến, và MUST NOT được lưu tạm trên thiết bị để đồng bộ sau. Khi mất kết nối lúc cần phát thuốc loại này, điều dưỡng vẫn cho dùng, ghi tạm ngoài hệ thống, và MUST ghi liều trực tuyến ngay khi có kết nối với thời điểm dùng thực tế; bản ghi này MUST mang nhãn "ghi sau mất kết nối", MUST luôn chuyển "chờ xem lại" (bất kể thời gian mất kết nối) cho Điều dưỡng giữ nhiệm vụ Người phụ trách ca của tầng đó, hoặc nếu người đó là người ghi hay không phải Điều dưỡng thì cho một Điều dưỡng khác có ca tại tầng (người xem lại MUST khác người ghi và đáp ứng FR-026 (a)); Trưởng tầng chỉ được thông báo, và trạng thái liều xét theo thời điểm dùng thực tế như bản ghi đồng bộ muộn (liều đã Trễ/Bỏ lỡ nhưng thời điểm dùng trong cửa sổ thì chuyển Đã dùng không nhãn trễ, feature 007 nhận yêu cầu đóng cảnh báo liên quan). Việc xem lại chỉ gắn kết quả (chấp nhận / cần đính chính), không thay bản ghi; khi kết quả là cần đính chính, người xem lại hoặc người ghi gốc MUST tạo bản đính chính theo FR-030. *(Nguồn: 8.6; Clarification 2026-09-26, đề xuất Q-62, Q-66)* Với thuốc khác: các xác nhận liều khác MAY ghi ngoại tuyến theo Q-01 (mặc định) và feature 002 FR-041a; trạng thái liều khi đồng bộ muộn MUST xét theo thời điểm dùng ghi trên thiết bị (liều đã Trễ/Bỏ lỡ do mất kết nối nhưng thời điểm dùng trong cửa sổ thì chuyển Đã dùng không nhãn trễ, và feature 007 nhận yêu cầu đóng cảnh báo liên quan). *(Nguồn: 8.6, Q-01, DBR-25)*
- **FR-030**: Liều đã xác nhận và lần dùng PRN MUST NOT sửa hay xóa (BR-M07-12, BR-M15-03). Đính chính MUST có lý do và chỉ do Điều dưỡng đáp ứng FR-026 (a) tạo, là: người xác nhận gốc; hoặc Điều dưỡng giữ nhiệm vụ Người phụ trách ca của tầng/khu vực đó; hoặc, với liều do Hệ thống đóng (Bỏ lỡ, Không thực hiện do trạng thái cuối), Điều dưỡng phụ trách người cao tuổi trong ca của liều. Spec này thu hẹp quy tắc đính chính của feature 000: Trưởng tầng không là Điều dưỡng MUST NOT đính chính liều. Đính chính làm thay đổi trạng thái Đã dùng MUST kích hoạt: sự kiện chi phí cho feature 010; biến động số lượng thuốc gia đình gửi tương ứng; yêu cầu đóng hoặc tạo cảnh báo liên quan ở feature 007. *(Nguồn: BR-M07-12, BR-M15-03, 1.5, DBR-23)*
- **FR-031**: Khi xác nhận liều hoặc lần dùng PRN có ghi phản ứng sau dùng, hệ thống MUST yêu cầu feature 007 tạo cảnh báo mức trung bình trở lên gắn liều đó. *(Nguồn: BR-M07-13, 11.3)*
- **FR-032**: Khi một thuốc bị ghi Từ chối CFG-M05-04 (mặc định \[2\]) lần liên tiếp (tính trên các liều đã đóng của các đơn cùng thuốc của người cao tuổi, theo thứ tự thời điểm dự kiến; liều Tạm dừng, Đã hủy không tính và không làm đứt chuỗi), hệ thống MUST yêu cầu feature 007 tạo cảnh báo mức trung bình trở lên; gộp với cảnh báo cùng loại đang mở theo BR-M05-02. *(Nguồn: BR-M07-13, BR-M05-04, CFG-M05-04)*
- **FR-033**: Liều nguồn viện cung cấp được xác nhận Đã dùng MUST yêu cầu feature 010 tạo chi phí nháp trỏ về liều đó (DBR-15), trừ liều thuộc một lần "Giao thuốc mang theo" đã được tính phí khi giao (FR-048a). Liều nguồn "gia đình gửi", kể cả thuốc mua hộ, MUST NOT yêu cầu tạo chi phí (Q-139). Spec này cung cấp thuốc và số lượng, feature 010 quyết định đơn giá và việc khoản đó thuộc gói (DBR-16). *(Nguồn: BR-M07-14, BR-M11-01, 15.2)*

#### F. Đối chiếu thuốc

- **FR-034a**: Khi hồ sơ người cao tuổi ở Đang tiếp nhận, Bác sĩ hoặc Điều dưỡng MUST lập được trước phiếu đối chiếu lý do "tiếp nhận" (có sẵn một dòng cho mỗi thuốc trong "thuốc đang sử dụng khi tiếp nhận" của hồ sơ sức khỏe ban đầu, feature 001 FR-017), và phiếu MAY được gửi, xác nhận trước khi Hoàn tất tiếp nhận theo FR-038. Mỗi hồ sơ MUST có tối đa một phiếu lý do "tiếp nhận" chưa Đã hủy. Đơn tạo từ phiếu xác nhận trước MUST ở Chờ hiệu lực với dấu "chờ tiếp nhận", MUST NOT sinh liều, và chuyển Hiệu lực ngay tại lệnh Hoàn tất tiếp nhận (ngày giờ bắt đầu = thời điểm lệnh); liều được sinh từ thời điểm đó (FR-018). Hạn CFG-M07-05 và cảnh báo quá hạn MUST chỉ tính từ thời điểm Hoàn tất tiếp nhận. Nếu hồ sơ chuyển Hủy tiếp nhận, phiếu lập trước còn Mở hoặc Chờ xác nhận MUST chuyển Đã hủy và các đơn "chờ tiếp nhận" MUST chuyển Đã ngừng (FR-047); phiếu đã Đã xác nhận giữ nguyên làm lịch sử. *(Nguồn: BR-M07-09, 11.5, 5.6; Clarification 2026-09-26, đề xuất Q-57)*
- **FR-034**: Khi người cao tuổi được Hoàn tất tiếp nhận hoặc Ghi nhận trở về từ Điều trị tại bệnh viện (feature 004), trong cùng một lệnh hệ thống MUST (với tiếp nhận: chỉ khi chưa có phiếu "tiếp nhận" Đã xác nhận theo FR-034a; nếu đã có thì chỉ chuyển các đơn "chờ tiếp nhận" sang Hiệu lực, không tạm dừng, không tạo phiếu mới; nếu đã có phiếu lập trước còn Mở hoặc Chờ xác nhận thì dùng phiếu đó thay vì tạo phiếu mới): chuyển mọi đơn Hiệu lực, Chờ hiệu lực của người đó sang Tạm dừng với lý do "chờ đối chiếu" (đơn đã Tạm dừng thì giữ và cũng gắn vào phiếu); chuyển liều chưa xác nhận của các đơn đó từ thời điểm lệnh sang Tạm dừng; tạo phiếu đối chiếu ở trạng thái Mở với lý do (tiếp nhận / trở về từ bệnh viện), thời điểm tạo, và một dòng cho mỗi đơn gắn vào phiếu. Với tiếp nhận, phiếu MUST có sẵn một dòng cho mỗi thuốc trong "thuốc đang sử dụng khi tiếp nhận" của hồ sơ sức khỏe ban đầu (feature 001 FR-017). *(Nguồn: BR-M07-09, 11.5, 5.6)*
- **FR-035**: Vòng đời phiếu đối chiếu MUST theo bảng dưới. *(Nguồn: 11.5, BR-M07-09, UC-43, PHIEU_DOI_CHIEU, constitution IV)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái mới | Người thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Lập trước khi hồ sơ Đang tiếp nhận | Mở | Bác sĩ, Điều dưỡng | FR-034a | Chưa có hạn |
| — | Tiếp nhận (chưa có phiếu "tiếp nhận") hoặc trở về từ bệnh viện | Mở | Hệ thống | FR-034 | Thông báo Điều dưỡng phụ trách và các Bác sĩ của cơ sở; hạn = thời điểm tạo + CFG-M07-05 |
| Mở, Chờ xác nhận (lập trước) | Hoàn tất tiếp nhận | Giữ nguyên | Hệ thống | — | Hạn = thời điểm Hoàn tất tiếp nhận + CFG-M07-05; thông báo như dòng trên |
| Mở | Thêm dòng từ đơn ra viện / thuốc đang dùng; ghi quyết định từng dòng | Mở | Người thực hiện (FR-038) | Dòng Thay đổi liều, Thêm mới qua FR-007 và đủ trường FR-004 | Lưu lịch sử thay đổi của phiếu |
| Mở | Gửi xác nhận | Chờ xác nhận | Người thực hiện | Mọi dòng có quyết định; dòng Ngừng có lý do | Thông báo người xác nhận |
| Chờ xác nhận | Trả lại | Mở | Người xác nhận | Bắt buộc lý do | Thông báo người thực hiện |
| Chờ xác nhận | Xác nhận | Đã xác nhận | Người xác nhận (FR-038) | Khác người gửi và người đã ghi quyết định (quy tắc hai người); người xác nhận đáp ứng FR-005 cho mọi dòng tạo đơn nội bộ; với dòng tạo đơn bên ngoài phải có cơ sở kê (FR-006) | Áp FR-036 trong cùng một lệnh |
| Mở, Chờ xác nhận | Tới hạn CFG-M07-05 (mặc định \[4 giờ\]) mà chưa Đã xác nhận | Giữ nguyên (gắn dấu "quá hạn") | Bộ lập lịch | Phiếu đã có hạn (không áp khi hồ sơ còn Đang tiếp nhận) | Cảnh báo mức trung bình (feature 007) (BR-M07-09) |
| Mở, Chờ xác nhận | Người cao tuổi chuyển lại Điều trị tại bệnh viện | Đã hủy | Hệ thống | — | Lý do "chuyển viện lại"; các đơn giữ Tạm dừng |
| Mở, Chờ xác nhận | Người cao tuổi chuyển trạng thái cuối | Đã hủy | Hệ thống | — | FR-047 |

- **FR-036**: Khi phiếu được xác nhận, trong cùng một lần hệ thống MUST áp quyết định từng dòng: **Tiếp tục** – đơn Tạm dừng chuyển Hiệu lực; **Ngừng** – đơn chuyển Đã ngừng với lý do trên dòng; **Thay đổi liều** – đơn cũ chuyển Đã ngừng và đơn thay thế Hiệu lực trỏ về đơn cũ (DBR-14); **Thêm mới** – đơn mới Hiệu lực. Dòng từ đơn ra viện hoặc thuốc đang dùng khi tiếp nhận mà được chọn "Ngừng" (không dùng tiếp) MUST được lưu làm căn cứ, không tạo đơn. Sau đó liều MUST được sinh hoặc khôi phục từ thời điểm xác nhận (FR-018, FR-021); đồng thời gỡ dấu "quá hạn" và yêu cầu feature 007 đóng cảnh báo "chưa đối chiếu thuốc". Nếu một phần thất bại thì không phần nào được áp dụng. *(Nguồn: 11.5, BR-M07-09, DBR-14, feature 000)*
- **FR-037**: Khi người cao tuổi có phiếu đối chiếu Mở hoặc Chờ xác nhận, lệnh Nhập đơn, Tiếp tục đơn, Đổi liều ngoài phiếu MUST bị chặn và hệ thống MUST gợi ý ghi trên phiếu; lệnh Ngừng đơn vẫn được phép (và dòng tương ứng trên phiếu được đánh dấu "đã ngừng ngoài phiếu"). *(Nguồn: BR-M07-09 "lịch thuốc chỉ chạy lại khi phiếu được xác nhận")*
- **FR-038**: Mỗi phiếu MUST lưu người thực hiện, người xác nhận, thời điểm tạo, thời điểm gửi, thời điểm xác nhận và lịch sử. Người thực hiện (người ghi quyết định và gửi xác nhận) và người xác nhận MUST là Bác sĩ hoặc Điều dưỡng trong phạm vi (4.4 dòng "Đối chiếu thuốc": BS T, ĐD T), theo quy tắc hai người: người xác nhận MUST khác người gửi xác nhận và khác mọi người đã ghi quyết định trên phiếu; không có tự xác nhận. Quy tắc này áp như nhau khi cơ sở có hay không có phạm vi khám chữa bệnh. Ngoài ra, phiếu có dòng tạo đơn nội bộ (Thay đổi liều hoặc Thêm mới với cơ sở kê là viện) MUST chỉ được xác nhận bởi Bác sĩ đáp ứng FR-005; phiếu chỉ có dòng Tiếp tục, Ngừng hoặc dòng tạo đơn bên ngoài có cơ sở kê (FR-006) MAY do Điều dưỡng xác nhận. *(Nguồn: 11.5, UC-43, BR-M07-05; Clarification 2026-09-26, đề xuất Q-55)*
- **FR-039**: Phiếu Đã xác nhận MUST NOT sửa; sai sót trên phiếu MUST xử lý bằng bản đính chính có lý do trỏ về phiếu, và thay đổi thuốc thực tế sau đó MUST qua lệnh đơn thuốc ở bảng FR-011. *(Nguồn: 1.5 nhóm 3, PHIEU_DOI_CHIEU)*

#### G. Thuốc gia đình gửi

- **FR-040**: Điều dưỡng MUST ghi nhận được việc tiếp nhận thuốc gia đình gửi với: người cao tuổi, tên thuốc (chọn từ danh mục nếu có, nếu không ghi tự do kèm hoạt chất nếu biết), hàm lượng, số lượng và đơn vị, hạn dùng, tình trạng bao bì, có đơn/toa hay không (kèm ảnh nếu có), người giao (người thân trong danh sách người thân của người cao tuổi, feature 012, hoặc ghi rõ họ tên và quan hệ), người nhận, thời điểm. Thuốc đã hết hạn dùng lúc tiếp nhận MUST được ghi nhận nhưng MUST NOT chuyển Được sử dụng. Thuốc mua hộ (feature 010, đề nghị mua hộ ở trạng thái Đã mua) MUST được tiếp nhận theo FR này, với nguồn "gia đình gửi", người giao là nhân viên đã mua, và tham chiếu tới đề nghị mua hộ. Liều dùng thuốc đó MUST NOT sinh chi phí (FR-033), vì thuốc đã được tính ở khoản mua hộ. *(Nguồn: 11.4, UC-44, THUOC_GIA_DINH_GUI; đồng bộ spec 010, Q-139)*
- **FR-041**: Vòng đời thuốc gia đình gửi MUST theo bảng dưới. *(Nguồn: 11.4, BR-M07-10, 11, UC-44, constitution IV)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái mới | Người thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Tiếp nhận | Chờ đối chiếu | Điều dưỡng | FR-040; người cao tuổi chưa ở trạng thái cuối | Lưu lần tiếp nhận và số lượng ban đầu |
| Chờ đối chiếu, Chỉ giữ hộ | Xác nhận sử dụng (gắn đơn) | Được sử dụng | Bác sĩ, Điều dưỡng | Có đơn Hiệu lực, Tạm dừng hoặc Chờ hiệu lực nguồn "gia đình gửi" cùng thuốc (hoặc cùng hoạt chất và hàm lượng); có đơn/toa hoặc đơn của viện làm căn cứ; chưa hết hạn dùng; bao bì còn nhận diện được | Liều của đơn lấy nguồn từ thuốc này; gỡ dấu "chưa có thuốc" của đơn (FR-009) |
| Chờ đối chiếu | Chỉ giữ hộ | Chỉ giữ hộ | Bác sĩ, Điều dưỡng | Bắt buộc lý do (không rõ nguồn gốc, không có căn cứ, không có đơn tương ứng, hết hạn dùng…) | Không gắn vào lịch dùng (BR-M07-10) |
| Được sử dụng | Ngừng sử dụng | Chỉ giữ hộ | Bác sĩ, Điều dưỡng | Bắt buộc lý do | Gỡ liên kết với đơn |
| Được sử dụng | Đơn gắn chuyển Đã ngừng/Hết hạn mà không có đơn thay thế cùng thuốc nguồn gia đình gửi | Chỉ giữ hộ | Hệ thống | — | Lý do theo đơn; thông báo Điều dưỡng phụ trách |
| Được sử dụng | Qua ngày hạn dùng | Chỉ giữ hộ | Bộ lập lịch | — | Lý do "hết hạn dùng"; thông báo Điều dưỡng phụ trách, người liên hệ chính và người đại diện (cùng tập người nhận với FR-046) (BR-M07-11) |
| Được sử dụng | Số lượng còn về 0 | Đã dùng hết | Hệ thống | — | Thông báo Điều dưỡng phụ trách (bổ sung, xem Điểm cần báo lại 5) |
| Chờ đối chiếu, Chỉ giữ hộ | Hoàn trả | Đã hoàn trả | Điều dưỡng | Người nhận thuộc danh sách người thân của người cao tuổi (feature 012) hoặc người được người đại diện chỉ định; ghi số lượng trả, người giao, người nhận, thời điểm | Không còn là "thuốc gia đình gửi đang giữ" (feature 004) |
| Chờ đối chiếu, Chỉ giữ hộ | Hủy theo yêu cầu gia đình | Đã hủy theo yêu cầu gia đình | Điều dưỡng | Có yêu cầu của người đại diện (ghi cách nhận yêu cầu); ghi số lượng hủy, người chứng kiến, lý do | Như trên |

- **FR-042**: Đã hoàn trả, Đã hủy theo yêu cầu gia đình và Đã dùng hết là trạng thái cuối. Thuốc ở Chờ đối chiếu hoặc Chỉ giữ hộ MUST NOT được gắn vào lịch dùng, và liều của đơn MUST NOT lấy nguồn từ thuốc đó. *(Nguồn: BR-M07-10, 11.4)*
- **FR-043**: Khi đơn gắn với thuốc gia đình gửi được thay thế (đổi liều) bởi đơn cùng thuốc và nguồn "gia đình gửi", liên kết MUST chuyển sang đơn thay thế trong cùng lệnh, thuốc giữ Được sử dụng. Một đơn MAY gắn với nhiều lô thuốc gia đình gửi Được sử dụng. *(Nguồn: 11.4, DBR-14)*
- **FR-044**: Khi xác nhận Đã dùng một liều hoặc lần dùng PRN nguồn gia đình gửi, hệ thống MUST: đề xuất lô Được sử dụng có hạn dùng sớm nhất còn đủ số lượng (điều dưỡng MAY chọn lô khác đang Được sử dụng); chặn xác nhận nếu không có lô nào còn đủ số lượng hoặc chưa hết hạn dùng; trừ số lượng của lô bằng liều đã dùng và lưu biến động số lượng trỏ về liều. Liều thuộc một lần "Giao thuốc mang theo" không áp yêu cầu này vì số lượng đã trừ khi giao (FR-048a). *(Nguồn: BR-M07-11)*
- **FR-045**: Số lượng thuốc gia đình gửi MUST NOT sửa trực tiếp; mọi thay đổi MUST là biến động có nguồn: tiếp nhận, liều đã dùng, đính chính liều, giao thuốc mang theo, nhận lại thuốc mang theo (FR-048a), hoàn trả, hủy theo yêu cầu gia đình, hoặc lệnh "Kiểm kê điều chỉnh" do Điều dưỡng thực hiện có lý do (rơi vỡ, đếm lại). Người thân gửi thêm cùng thuốc MUST được ghi như một lần tiếp nhận mới (lô mới). *(Nguồn: BR-M07-11, 1.5)*
- **FR-046**: Sau mỗi biến động làm giảm số lượng, hệ thống MUST tính số ngày dùng còn lại của mỗi đơn gắn thuốc gia đình gửi = tổng số lượng còn của các lô Được sử dụng gắn đơn ÷ lượng dùng mỗi ngày của đơn (với đơn PRN: liều × số lần tối đa mỗi ngày). Khi số ngày còn lại chuyển từ không nhỏ hơn sang nhỏ hơn CFG-M07-06 (mặc định \[5 ngày\] dùng), hệ thống MUST yêu cầu feature 009 thông báo mức Trung bình (feature 009 FR-043b, Q-116; nội dung chia phần "chung" gồm việc thuốc sắp hết và số ngày còn lại, phần "sức khỏe" gồm tên thuốc, hàm lượng) cho người liên hệ chính và người đại diện một lần cho lần vượt ngưỡng đó; thông báo lại chỉ khi số ngày còn lại đã trở về không nhỏ hơn ngưỡng (do tiếp nhận thêm) rồi lại giảm dưới ngưỡng. *(Nguồn: BR-M07-11, CFG-M07-06)*

#### H. Tác động từ trạng thái người cao tuổi, quyền xem và dữ liệu cung cấp

- **FR-047**: Khi người cao tuổi chuyển trạng thái cuối (Kết thúc lưu trú, Qua đời, Hủy tiếp nhận), trong cùng lệnh đó hệ thống MUST: chuyển mọi đơn Hiệu lực, Tạm dừng, Chờ hiệu lực sang Đã ngừng; chuyển liều Chưa đến giờ, Tạm dừng, Mang theo chưa ghi nhận sang Đã hủy; chuyển liều Đến giờ, Trễ sang Không thực hiện (người đóng là Hệ thống, lý do trạng thái cuối) và không nhắc, không cảnh báo, không đưa vào bàn giao; chuyển phiếu đối chiếu Mở hoặc Chờ xác nhận sang Đã hủy; yêu cầu feature 007 đóng cảnh báo bỏ lỡ liều và chưa đối chiếu thuốc đang mở. Thuốc gia đình gửi và lần giao thuốc mang theo chưa Đã nhận lại MUST giữ nguyên trạng thái để được hoàn trả, hủy hoặc nhận lại (feature 004 FR-063 (c), FR-071). *(Nguồn: 5.6, 6.8, feature 004 FR-070, FR-071)*
- **FR-048**: Khi người cao tuổi chuyển Tạm vắng, feature 004 MUST cho biết lệnh "có mang thuốc" hay "không mang thuốc" và khoảng vắng dự kiến; khi chuyển Hoạt động bên ngoài, feature 014 cho biết khoảng đi dự kiến. Spec này áp bảng FR-021 theo các thông tin đó; khi người cao tuổi trở về sớm hoặc muộn hơn dự kiến, liều MUST được tính lại theo thời điểm trở về thực tế. *(Nguồn: BR-M07-03, BR-M04-16, 5.6)*
- **FR-048a**: Khi người cao tuổi rời viện có mang thuốc, Điều dưỡng MUST ghi được lần "Giao thuốc mang theo" gồm: từng đơn (định kỳ có liều Mang theo, hoặc PRN), số lượng giao, lô thuốc gia đình gửi (với nguồn gia đình gửi, theo điều kiện chọn lô của FR-044), người nhận (nhân viên đi cùng, trưởng đoàn hoặc người thân kèm quan hệ), người giao, thời điểm. Trong cùng lệnh: lô thuốc gia đình gửi bị trừ số lượng giao (biến động "giao mang theo"); với nguồn viện cung cấp, feature 010 nhận yêu cầu tạo chi phí nháp theo số lượng giao, trỏ về lần giao. Khi người cao tuổi trở về, Điều dưỡng MUST ghi được lần "Nhận lại thuốc mang theo" với số lượng trả lại từng đơn (không vượt số lượng đã giao); thuốc gia đình gửi được cộng lại vào đúng lô (biến động "nhận lại"), và với nguồn viện, feature 010 nhận yêu cầu giảm chi phí nháp hoặc tạo khoản điều chỉnh nếu kỳ đã chốt. Liều Mang theo và lần dùng PRN thuộc một lần giao MUST NOT trừ số lượng hay tạo chi phí thêm khi được ghi Đã dùng (FR-033, FR-044 không áp). Nếu người cao tuổi rời viện mà không có lần giao nào, liều Mang theo được ghi Đã dùng trừ số lượng và tạo chi phí theo từng liều như bình thường. Lần giao đang mở (chưa ghi nhận lại) MUST xuất hiện trong checklist của điều dưỡng phụ trách sau khi người cao tuổi trở về và trong bản nháp bàn giao cho tới khi được ghi nhận lại (kể cả nhận lại số lượng 0). Lần giao chỉ bao các liều Mang theo và lần dùng PRN có thời điểm dùng trong khoảng vắng: liều quay về Chưa đến giờ / Đến giờ khi người cao tuổi trở về sớm (bảng FR-021) MUST thôi thuộc lần giao, được trừ số lượng và tạo chi phí theo FR-033, FR-044 khi xác nhận ở viện, và thuốc của liều đó MUST được tính trong số lượng nhận lại; thuốc đã giao chỉ được dùng lại cho liều ở viện sau khi đã được ghi "Nhận lại" vào lô (Q-65), liều ở viện luôn trừ lô và tính phí như bình thường; nếu lần giao còn mở khi điều dưỡng xác nhận liều đó, hệ thống MUST nhắc ghi "Nhận lại thuốc mang theo" trước nhưng MUST NOT chặn. Vòng đời lần giao MUST theo bảng dưới. *(Nguồn: BR-M07-11, BR-M07-14, 8.9 "kiểm tra thuốc cần mang"; Clarification 2026-09-26, đề xuất Q-58, Q-60)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái mới | Người thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Giao thuốc mang theo | Đang mang theo | Điều dưỡng (FR-026 (a)) | Người cao tuổi đang rời viện hoặc sắp rời viện có mang thuốc; lô gia đình gửi đủ số lượng, chưa hết hạn dùng | Trừ số lượng lô; chi phí nháp nguồn viện theo số lượng giao |
| Đang mang theo | Người cao tuổi trở về | Chờ nhận lại | Hệ thống | — | Hiện trong checklist điều dưỡng phụ trách và bản nháp bàn giao |
| Đang mang theo, Chờ nhận lại | Nhận lại thuốc mang theo | Đã nhận lại | Điều dưỡng (FR-026 (a)) | Số lượng nhận lại từng đơn không vượt số lượng giao | Cộng lại vào lô; giảm chi phí nháp hoặc khoản điều chỉnh (feature 010) |
| Đang mang theo, Chờ nhận lại | Người cao tuổi chuyển trạng thái cuối | Chờ nhận lại (giữ nguyên nếu đã Chờ nhận lại) | Hệ thống | — | Lần giao chưa Đã nhận lại là điều kiện chặn Kết thúc lưu trú (trừ ngoại lệ được Quản lý viện duyệt) và là một mục của danh sách việc sau qua đời, như thuốc gia đình gửi (FR-047); Điều dưỡng ghi Nhận lại, kể cả số lượng 0 với lý do "người thân giữ lại" (Q-64) |

Đã nhận lại là trạng thái cuối; sai sót của lần giao hoặc lần nhận lại MUST được xử lý bằng đính chính có lý do, do người ghi gốc hoặc Điều dưỡng giữ nhiệm vụ Người phụ trách ca tạo, kèm biến động số lượng và sự kiện chi phí tương ứng.
- **FR-049**: Quyền xem MUST theo 4.4: Bác sĩ, Quản lý viện xem mọi đơn, liều, phiếu đối chiếu, thuốc gia đình gửi; Điều dưỡng xem trong phạm vi phân công (theo feature 002); Trưởng tầng xem liều và trạng thái liều trong phạm vi (dòng "Phát thuốc": X) nhưng MUST NOT xem hay thao tác trên đơn thuốc và phiếu đối chiếu (dòng "Đơn thuốc", "Đối chiếu thuốc": —); Nhân viên chăm sóc, Dinh dưỡng viên, Nhân viên bếp, Nhân viên vệ sinh, Nhân viên hành chính MUST NOT xem thông tin thuốc (19.3); Người thân xem đơn thuốc chỉ khi có bản đồng ý chia sẻ dữ liệu đang hiệu lực bao gồm người thân đó (chú thích ¹), luôn xem được thuốc gia đình gửi của người cao tuổi (tên thuốc, trạng thái, số lượng còn, hạn dùng) không cần bản đồng ý, xem phiếu đối chiếu đã xác nhận chỉ khi có bản đồng ý đang hiệu lực bao gồm người thân đó (như đơn thuốc), và không xem liều. *(Nguồn: 4.4, 19.3, BR-M01-08, DBR-03; Clarification 2026-09-26, đề xuất Q-59)*
- **FR-050**: Hệ thống MUST cung cấp danh sách "thuốc đang dùng" (đơn Hiệu lực và Tạm dừng, kèm liều và mốc giờ) cho thẻ thông tin khẩn cấp và bản tóm tắt chuyển viện (feature 007, 9.5, BR-M05-14), và liều của người được phân công cho checklist điều dưỡng (feature 005 FR-029). *(Nguồn: 9.5, BR-M05-14, 8.3 "Làm rõ")*
- **FR-051**: Hệ thống MUST cung cấp cho báo cáo sức khỏe (18.3): tỷ lệ liều Bỏ lỡ, Từ chối theo người cao tuổi và theo ca; số phiếu đối chiếu quá hạn. *(Nguồn: 18.3)*
- **FR-052**: Bảng tổng hợp giao tiếp với feature khác:

| Chiều | Feature | Sự kiện / dữ liệu |
| --- | --- | --- |
| Nhận | 001 | Dị ứng mới (FR-012); thuốc đang dùng khi tiếp nhận; trạng thái người cao tuổi |
| Nhận | 002 | Giấy phép, phạm vi dữ liệu, nhiệm vụ Người phụ trách ca, kiểm tra quyền ngoại tuyến |
| Nhận | 004 | Hoàn tất tiếp nhận, Cho tạm vắng (có/không mang thuốc), Ghi nhận trở về (từ Tạm vắng / từ bệnh viện), trạng thái cuối |
| Nhận | 008, 014 | Phân công điều dưỡng theo ca; điểm danh rời viện/trở về của chuyến đi |
| Cung cấp | 004 | Còn thuốc gia đình gửi đang giữ (Chờ đối chiếu, Được sử dụng, Chỉ giữ hộ) hoặc lần giao thuốc mang theo chưa Đã nhận lại |
| Cung cấp | 005, 008 | Liều cho checklist; liều Trễ, Bỏ lỡ, Từ chối, Mang theo chờ ghi nhận và lần giao thuốc Chờ nhận lại cho checklist và bàn giao |
| Cung cấp | 007 | Yêu cầu tạo/đóng cảnh báo: Bỏ lỡ, phản ứng, từ chối liên tiếp, xung đột dị ứng, chưa đối chiếu, lần dùng PRN "ghi theo báo lại" vượt giới hạn (FR-027); yêu cầu đóng cảnh báo khi bản ghi đồng bộ muộn hoặc "ghi sau mất kết nối" có thời điểm dùng trong cửa sổ (FR-029); thuốc đang dùng |
| Cung cấp | 009 | Nhắc hết hạn đơn, nhắc Trễ, thông báo thuốc gia đình gửi sắp hết/hết hạn dùng, thông báo phiếu đối chiếu, thông báo "người cao tuổi chưa có điều dưỡng phụ trách" và nhắc Trễ của liều chung của tầng (FR-026a), bản ghi thuốc kiểm soát đặc biệt "chờ xem lại" (FR-029) |
| Cung cấp | 010 | Liều và lần dùng PRN nguồn viện Đã dùng (ngoài lần giao mang theo; không gồm liều gia đình gửi, kể cả thuốc mua hộ); lần giao và nhận lại thuốc mang theo nguồn viện; đính chính liên quan |
| Nhận | 010 | Đề nghị mua hộ thuốc đã Đã mua, để tiếp nhận với nguồn "gia đình gửi" (FR-040, Q-139) |

### Key Entities *(include if feature involves data)*

- **Thuốc (danh mục)** – nhóm 1: tên, hoạt chất (liên kết danh mục dị nguyên), hàm lượng, dạng, đường dùng mặc định, đơn vị dùng, cửa sổ thời gian riêng, kiểm soát đặc biệt, trạng thái hiệu lực.
- **Đơn thuốc (DON_THUOC)** – nhóm 2: người cao tuổi, thuốc, hoạt chất, liều, đường dùng, loại (định kỳ / PRN), tần suất và mốc giờ hoặc khoảng cách tối thiểu và số lần tối đa mỗi ngày, ngày bắt đầu, ngày kết thúc, nguồn thuốc, người kê, cơ sở kê, ngày kê, ghi chú hướng dẫn, trạng thái (Chờ hiệu lực / Hiệu lực / Tạm dừng / Đã ngừng / Hết hạn), "thay thế cho", vấn đề kiểm tra và lý do xác nhận, dấu "chưa có thuốc".
- **Liều thuốc (LIEU_THUOC)** – nhóm 3 sau khi xác nhận: đơn thuốc, thời điểm dự kiến, cửa sổ, trạng thái, điều dưỡng phụ trách, người xác nhận hoặc Hệ thống, thời điểm dùng, thời điểm ghi và đồng bộ, phản ứng, lý do, nhãn dùng trễ, nhãn "ghi sau mất kết nối" và kết quả xem lại (chấp nhận / cần đính chính, người xem lại, thời điểm) (FR-029), lần giao thuốc mang theo mà liều thuộc về (nếu có, FR-048a), căn cứ (được phân công, Người phụ trách ca, liều chung của tầng, mang theo, ghi theo báo lại), người báo lại, lô thuốc gia đình gửi, các bản đính chính. Duy nhất theo (đơn, thời điểm dự kiến).
- **Lần dùng PRN** – nhóm 3: đơn PRN, thời điểm dùng, liều, lý do, phản ứng, người dùng, lô thuốc gia đình gửi, các bản đính chính.
- **Phiếu đối chiếu (PHIEU_DOI_CHIEU)** – nhóm 2 khi Mở/Chờ xác nhận, nhóm 3 khi Đã xác nhận: người cao tuổi, lý do (tiếp nhận / trở về từ bệnh viện), trạng thái, hạn, dấu quá hạn, người thực hiện, người xác nhận, các thời điểm; mỗi dòng gồm nguồn (đơn hiện có / đơn ra viện / thuốc đang dùng khi tiếp nhận), quyết định (Tiếp tục / Ngừng / Thay đổi liều / Thêm mới), lý do, đơn được tạo hoặc bị ngừng.
- **Thuốc gia đình gửi (THUOC_GIA_DINH_GUI)** – nhóm 2: người cao tuổi, tên thuốc, hoạt chất, hàm lượng, số lượng còn, đơn vị, hạn dùng, bao bì, có toa, người giao, người nhận, trạng thái (Chờ đối chiếu / Được sử dụng / Chỉ giữ hộ / Đã dùng hết / Đã hoàn trả / Đã hủy theo yêu cầu gia đình), đơn được gắn.
- **Lần giao thuốc mang theo** – nhóm 3: trạng thái (Đang mang theo / Chờ nhận lại / Đã nhận lại), người cao tuổi, lượt vắng hoặc chuyến đi, từng đơn và số lượng giao, lô thuốc gia đình gửi, người giao, người nhận, thời điểm; lần nhận lại: số lượng trả lại từng đơn, người nhận lại, thời điểm.
- **Biến động số lượng thuốc gia đình gửi** – nhóm 3: lô, loại (tiếp nhận, dùng, đính chính, giao mang theo, nhận lại, kiểm kê điều chỉnh, hoàn trả, hủy), số lượng, bản ghi nguồn, người, thời điểm, lý do.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Với bộ kiểm thử 20 người cao tuổi và 60 đơn định kỳ (nhiều tần suất, có ngày kết thúc, có đổi liều, có vắng mặt) trong 7 ngày giả lập, 100% liều được sinh khớp danh sách tính tay (thời điểm, cửa sổ, trạng thái ban đầu); 0 liều trùng khi chạy lại việc sinh 3 lần.
- **SC-002**: 0 đơn Hiệu lực bị thay đổi thuốc, liều, tần suất hay mốc giờ; 100% lần đổi liều tạo đơn thay thế trỏ về đơn cũ và đơn cũ Đã ngừng trong cùng lệnh; 0 liều Đã dùng bị thay đổi do đổi liều hay ngừng đơn.
- **SC-003**: Trong bộ 30 đơn kiểm thử có trùng hoạt chất hoặc xung đột dị ứng (theo danh mục), 100% bị cảnh báo trước khi lưu và 0 đơn được lưu mà không có lý do xác nhận; 0 cảnh báo sai với 30 đơn không có vấn đề.
- **SC-004**: 100% liều hết cửa sổ mà chưa xác nhận chuyển Trễ và điều dưỡng phụ trách nhận nhắc trong vòng 1 phút; 100% liều Trễ quá CFG-M07-02 chuyển Bỏ lỡ kèm cảnh báo mức trung bình; 0 liều Tạm dừng bị tính Bỏ lỡ hay sinh cảnh báo.
- **SC-005**: 100% lần tiếp nhận và trở về từ bệnh viện tạo phiếu đối chiếu; 0 liều ở trạng thái Đến giờ của người cao tuổi có phiếu chưa xác nhận; 100% phiếu chưa xác nhận sau CFG-M07-05 có cảnh báo.
- **SC-006**: 0 lần dùng PRN được lưu khi chưa đủ khoảng cách tối thiểu hoặc đã đạt số lần tối đa; 100% lần dùng PRN có lý do.
- **SC-007**: 0 liều được xác nhận Đã dùng từ thuốc gia đình gửi ở Chờ đối chiếu, Chỉ giữ hộ hoặc đã hết hạn dùng; số lượng còn trên hệ thống khớp số tính tay (ban đầu trừ liều đã dùng ngoài lần giao, trừ số lượng giao mang theo, cộng số lượng nhận lại, cộng/trừ đính chính và kiểm kê) ở 100% lô trong bộ kiểm thử; 100% lần vượt ngưỡng CFG-M07-06 tạo đúng một thông báo cho người thân.
- **SC-008**: 0 lần xác nhận liều thành công bởi người không phải điều dưỡng có giấy phép còn hiệu lực và được phân công (hoặc giữ nhiệm vụ Người phụ trách ca); 0 lần sửa hay xóa trực tiếp liều đã xác nhận.
- **SC-009**: Điều dưỡng phụ trách 15 người cao tuổi trong một vòng phát thuốc xác nhận một liều Đã dùng trong không quá 10 giây tính từ lúc chạm vào liều tới lúc lưu xong, và thấy toàn bộ liều của vòng trong không quá 3 giây tính từ lúc mở danh sách. Các con số này là mục tiêu nghiệm thu, không phải tham số cấu hình.
- **SC-010**: Khi người cao tuổi chuyển trạng thái cuối, 100% đơn chưa kết thúc, liều chưa xác nhận và phiếu đối chiếu đang mở của người đó được đóng trong cùng lệnh; 0 liều của người đó xuất hiện trong checklist ca sau.
- **SC-011**: Trong bộ kiểm thử có người cao tuổi không có điều dưỡng được phân công trong ca, 0 liều bị Bỏ lỡ mà không hiển thị cho điều dưỡng nào có ca tại tầng; 100% trường hợp như vậy có thông báo cho Trưởng tầng và Người phụ trách ca trong ca đó (FR-026a).
- **SC-012**: 0 liều thuốc kiểm soát đặc biệt được lưu tạm trên thiết bị để đồng bộ sau; 100% bản ghi "ghi sau mất kết nối" có kết quả xem lại trước khi bản nháp bàn giao của ca kế tiếp được xác nhận (FR-029).
- **SC-013**: 0 lần người thân không có bản đồng ý chia sẻ dữ liệu đang hiệu lực xem được đơn thuốc hoặc phiếu đối chiếu; 100% người thân trong phạm vi FR-049 xem được thuốc gia đình gửi của người cao tuổi (FR-049).

## Assumptions

- Số feature `006` theo bản đồ feature ở mục 4.2 `docs/phan-tich-yeu-cau.md` (UC-39 → UC-44) và trùng số tuần tự kế tiếp.
- Quy tắc dùng chung theo feature 000; mục dị ứng và trạng thái người cao tuổi theo feature 001; quyền, giấy phép và phạm vi theo feature 002; lệnh tiếp nhận, tạm vắng, trở về, kết thúc lưu trú theo feature 004. Spec này không lặp lại các quy tắc đó.
- Q-01 được dùng theo mặc định (cho ghi tạm, đồng bộ sau), với ngoại lệ bắt buộc trực tuyến của 8.6 cho thuốc kiểm soát đặc biệt (FR-029).
- Liều được tính theo đơn vị dùng của thuốc (viên, ml, ống…); số lượng thuốc gia đình gửi ghi theo cùng đơn vị để trừ trực tiếp.
- "Đến giờ" bắt đầu tại đầu cửa sổ, "Trễ" tại cuối cửa sổ; CFG-M07-02 tính từ lúc liều chuyển Trễ. Không cho xác nhận khi liều còn Chưa đến giờ.
- Liều trong khoảng vắng đã biết được sinh ở trạng thái Tạm dừng thay vì không sinh, để BR-M07-03 ("liều trong thời gian vắng chuyển Tạm dừng") kiểm tra được; liều Tạm dừng mà cửa sổ đã qua được coi là kết thúc (FR-022).
- Trạng thái đơn "Chờ hiệu lực" được thêm cho đơn có ngày bắt đầu trong tương lai và đơn thay thế có hiệu lực trong tương lai; tài liệu nguồn chỉ nêu Hiệu lực → Tạm dừng → Hiệu lực / Đã ngừng / Hết hạn.
- Kiểm tra trùng hoạt chất gồm cả đơn Tạm dừng và Chờ hiệu lực, vì chúng sẽ được dùng lại; kiểm tra dị ứng chỉ tự động với mục dị ứng chọn từ danh mục (feature 001 FR-018a).
- Gia hạn đơn (đổi ngày kết thúc) không thuộc nhóm trường "liều, thuốc, tần suất" bị khóa ở 11.1 nên được làm bằng lệnh riêng thay vì đơn thay thế.
- Danh mục thuốc do Quản lý viện cấu hình (quyền C), vì Permission Matrix không có dòng riêng; cách làm này giống danh mục loại công việc ở feature 005.
- Người nhận thông báo thuốc gia đình gửi sắp hết là người liên hệ chính và người đại diện; thông báo này (và thông báo thuốc gia đình gửi hết hạn dùng) chỉ nêu thuốc gia đình gửi và không đòi bản đồng ý chia sẻ dữ liệu (Clarification 2026-09-26, Q-59).
- Không có thao tác vượt chặn cho giới hạn PRN; trường hợp cần thêm liều phải qua bác sĩ (đơn mới hoặc đơn thay thế).
- Thuốc gây nghiện, hướng thần chỉ được kiểm soát bằng đánh dấu "kiểm soát đặc biệt" (bắt buộc trực tuyến, xem lại khi mất kết nối, FR-029). Spec chưa có người chứng kiến liều, sổ theo dõi hay đếm số lượng còn cho nhóm thuốc này; đây là quyết định còn mở Q-63 (Quản lý viện và tư vấn pháp lý). Nếu Q-63 chốt cần các kiểm soát đó, FR-001, FR-021, FR-029 và phạm vi "không quản lý kho" phải xem lại.

## Điểm cần báo lại về tài liệu nguồn

Chưa sửa `docs/`. Các điểm dưới đây cần chủ tài liệu xác nhận.

1. **Sơ đồ vòng đời liều 11.2 không có trạng thái Hủy**, trong khi BR-M07-07 nói liều "bị hủy và sinh lại" khi ngừng hoặc đổi liều; spec thêm trạng thái Đã hủy (FR-021). Sơ đồ cũng không có chuyển nào khi người cao tuổi chuyển trạng thái cuối.
2. **Sơ đồ 11.2 chỉ cho Trễ → Đã dùng / Bỏ lỡ**; spec thêm Trễ → Từ chối / Không thực hiện, vì điều dưỡng tới muộn vẫn có thể gặp người cao tuổi từ chối, và thêm Mang theo → Chưa đến giờ / Đến giờ khi trở về trước cuối cửa sổ. Sơ đồ cũng không có chuyển ra khỏi Tạm dừng khi cửa sổ đã qua; spec coi đó là kết thúc (FR-022).
3. **Vòng đời đơn thuốc 11.1** không nêu ai được Tạm dừng / Tiếp tục đơn thủ công, không có trạng thái cho đơn có ngày bắt đầu trong tương lai; spec thêm Chờ hiệu lực và lệnh Tạm dừng / Tiếp tục / Gia hạn (FR-011, FR-015).
4. **Người ghi nhận liều Mang theo** (BR-M04-16 "giao cho người đi cùng") mâu thuẫn với Permission Matrix 4.4, nơi chỉ Điều dưỡng có quyền "T" ở dòng "Phát thuốc, thuốc khi cần"; Tạm vắng có mang thuốc thì người đi cùng là người thân, không phải người dùng hệ thống. Đã chốt khi clarify (FR-027): chỉ Điều dưỡng ghi; không có điều dưỡng đi cùng thì ghi sau khi trở về theo báo lại. Cần sửa BR-M04-16 ("giao cho người đi cùng ghi nhận" → người đi cùng không phải điều dưỡng thì báo lại, điều dưỡng ghi nhận) và thêm Q-54 vào mục 24.2; việc lưu lần dùng PRN "ghi theo báo lại" dù vượt giới hạn là bổ sung cho BR-M07-04.
5. **Vòng đời thuốc gia đình gửi 11.4** không có trạng thái khi thuốc được dùng hết, không có chuyển từ Được sử dụng về Chỉ giữ hộ (đơn ngừng, hết hạn dùng) và không nói có hoàn trả thẳng từ Được sử dụng không; spec thêm Đã dùng hết, lệnh Ngừng sử dụng và chỉ cho hoàn trả từ Chờ đối chiếu / Chỉ giữ hộ (FR-041).
6. **Thuốc gia đình gửi xuất hiện ở hai nơi**: 16.1 (đồ gửi, Hành chính có quyền "T" ở dòng "Đồ gửi") và 11.4 / UC-44 (Điều dưỡng). Spec giao việc tiếp nhận cho Điều dưỡng vì Hành chính không được xem thông tin thuốc (19.3); cần ghi rõ ở 16.1 rằng thuốc gia đình gửi không đi qua quy trình đồ gửi của Hành chính.
7. **Permission Matrix không có dòng cho danh mục thuốc**; spec giao Quản lý viện (C) (FR-002).
8. **Người xác nhận phiếu đối chiếu** (11.5 chỉ nêu "người thực hiện, người xác nhận") chưa rõ vai trò và có tách người không. Đã chốt khi clarify (FR-038): Bác sĩ hoặc Điều dưỡng, quy tắc hai người, không tự xác nhận; dòng tạo đơn nội bộ cần Bác sĩ có quyền kê đơn. Cần ghi vào 11.5 và thêm Q-55 vào mục 24.2.
9. **"Số lần tối đa mỗi ngày" của PRN** (11.1, BR-M07-04) chưa rõ là ngày dương lịch hay 24 giờ trượt. Đã chốt khi clarify (FR-025): 24 giờ trượt. Cần sửa BR-M07-04 ("số lần tối đa trong 24 giờ") và thêm Q-56 vào mục 24.2.
10. **Trưởng tầng có "X" ở dòng "Phát thuốc" nhưng "—" ở dòng "Đơn thuốc"**, trong khi 13.3 nói trưởng tầng "theo dõi thuốc"; spec cho trưởng tầng xem liều, không xem đơn (FR-049). Theo feature 000, trưởng tầng được đính chính bản ghi gắn tầng; spec thu hẹp: chỉ điều dưỡng có giấy phép đính chính liều (FR-030).
11. **Lệnh Cho tạm vắng (feature 004) chưa có thông tin "có mang thuốc"**, là đầu vào để chọn Tạm dừng hay Mang theo (BR-M07-03, FR-048); cần bổ sung vào 6.7 và feature 004.
12. **Các bổ sung khác của spec không có trong tài liệu nguồn**, cần chủ tài liệu xác nhận: chặn nhập đơn/đổi liều ngoài phiếu khi đang có phiếu đối chiếu mở (FR-037); phiếu đối chiếu bị hủy khi người cao tuổi chuyển viện lại (FR-035); cách tính số ngày dùng còn lại và chỉ thông báo một lần mỗi lần vượt ngưỡng (FR-046); đề xuất lô hạn dùng sớm nhất (FR-044); lệnh Kiểm kê điều chỉnh (FR-045); cách đếm "từ chối liên tiếp" qua các đơn cùng thuốc và bỏ qua liều Tạm dừng / Đã hủy (FR-032).
13. **Lập phiếu đối chiếu trước khi Hoàn tất tiếp nhận** (đã chốt khi clarify, FR-034a): BR-M07-09 nói phiếu được tạo "khi tiếp nhận" và mọi đơn cũ chuyển Tạm dừng; spec cho lập, xác nhận phiếu "tiếp nhận" khi hồ sơ còn Đang tiếp nhận, đơn "chờ tiếp nhận" có hiệu lực tại lệnh Hoàn tất tiếp nhận, hạn CFG-M07-05 tính từ lệnh đó. Cần ghi vào 11.5, BR-M07-09, dòng "Đang tiếp nhận → Đang lưu trú" của 5.6 (tác động "sinh lịch thuốc"), và thêm Q-57 vào mục 24.2.
14. **Giao và nhận lại thuốc mang theo** (đã chốt khi clarify, FR-048a): BR-M07-11 trừ số lượng "theo mỗi liều đã dùng" và BR-M07-14 tạo chi phí khi liều "được xác nhận Đã dùng"; spec đổi thời điểm trừ và tính phí sang lúc giao thuốc mang theo, cộng lại / giảm chi phí khi nhận lại. Cần ghi ngoại lệ này vào BR-M07-11, BR-M07-14, dòng "Liều thuốc nguồn viện xác nhận Đã dùng" của bảng 15.2, mục 8.9 ("kiểm tra thuốc cần mang"), và thêm Q-58 vào mục 24.2.
15. **Quyền xem của người thân với phiếu đối chiếu** (đã chốt khi clarify, FR-049): dòng "Đối chiếu thuốc, thuốc gia đình gửi" của 4.4 cho người thân "X" không kèm chú thích ¹, trong khi DBR-03 chỉ cho người thân xem sức khỏe khi có bản đồng ý. Spec giữ thuốc gia đình gửi không cần bản đồng ý, còn phiếu đối chiếu cần bản đồng ý. Cần tách dòng này trong 4.4 (hoặc thêm chú thích ¹ cho phần "Đối chiếu thuốc") và thêm Q-59 vào mục 24.2.
16. **Các quyết định clarify lượt 2 (2026-09-26)** cần phản ánh vào tài liệu nguồn và thêm vào mục 24.2: Q-60 liều Mang theo trở về sớm thôi thuộc lần giao và vòng đời lần giao thuốc mang theo (FR-021, FR-048a; bổ sung cho 8.9, BR-M07-11, BR-M07-14); Q-61 "liều chung của tầng" khi người cao tuổi không có điều dưỡng phụ trách (FR-026a; bổ sung cho 11.3, 13.4); Q-62 thuốc kiểm soát đặc biệt khi mất kết nối được ghi ngay khi có mạng, luôn chờ xem lại (FR-029; bổ sung cho 8.6 và Q-01). Ngoài ra bảng trạng thái liều FR-021 đã được sửa để trở về từ bệnh viện không khôi phục liều (khớp BR-M07-09).
17. **Kiểm soát pháp lý cho thuốc gây nghiện, hướng thần** chưa có trong tài liệu nguồn (8.6 chỉ nêu "xác nhận liều thuốc có kiểm soát đặc biệt" bắt buộc trực tuyến). Đề xuất thêm Q-63 vào mục 24.1 (quyết định còn mở): có cần người chứng kiến liều, sổ theo dõi, đếm số lượng còn cho nhóm thuốc này không; mặc định đề xuất: không bổ sung kiểm soát ngoài bắt buộc trực tuyến và xem lại khi mất kết nối (FR-029); người quyết định: Quản lý viện và tư vấn pháp lý; feature ảnh hưởng: 006.
18. **Các quyết định sau checklist business-rules lượt 2 (2026-09-26)** cần phản ánh vào tài liệu nguồn và thêm vào mục 24.2: Q-64 lần giao thuốc mang theo chưa nhận lại chặn Kết thúc lưu trú và vào danh sách việc sau qua đời (cần bổ sung vào 6.8, dòng "→ Kết thúc lưu trú" của 5.6 và feature 004 FR-063, FR-071); Q-65 thuốc đã giao chỉ được dùng lại sau khi ghi Nhận lại (8.9); Q-66 người xem lại bản ghi thuốc kiểm soát đặc biệt ghi sau mất kết nối là Điều dưỡng khác người ghi, Trưởng tầng chỉ được thông báo (8.6, Q-01).

**(2026-09-27)** Các quyết định Q-54 → Q-62, Q-64 → Q-66 của spec này đã được phản ánh vào `docs/nghiep-vu.md` (11.2 → 11.5) và nằm ở mục 24.2; Q-63 được ghi vào 24.1 (quyết định còn mở).
