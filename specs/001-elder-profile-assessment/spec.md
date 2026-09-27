# Feature Specification: Hồ sơ và đánh giá người cao tuổi

**Feature Branch**: `001-elder-profile-assessment`

**Created**: 2026-09-25

**Status**: Draft

**Input**: User description: "Quản lý hồ sơ người cao tuổi của viện dưỡng lão theo docs/nghiep-vu.md Module 01 (mục 5): hồ sơ cá nhân; bản đồng ý chia sẻ dữ liệu; hồ sơ sức khỏe ban đầu với dị ứng, bệnh nền lưu thành từng mục chỉ loại trừ chứ không xóa; đánh giá đầu vào và đánh giá lại bằng thang điểm, tự quy đổi ra mức chăm sóc đề xuất và cờ nguy cơ; trạng thái người cao tuổi chỉ thay đổi qua các lệnh nghiệp vụ theo bảng 5.5 với điều kiện ở 5.6. Loại hình lưu trú và mức chăm sóc là hai thuộc tính độc lập."

## Clarifications

### Session 2026-09-25

- Q: Khi một người đã Kết thúc lưu trú hoặc Hủy tiếp nhận quay lại đăng ký lần nữa, hệ thống tạo hồ sơ mới hay dùng lại hồ sơ cũ? → A: Tạo hồ sơ mới liên kết với hồ sơ cũ; CCCD chỉ phải duy nhất trong các hồ sơ chưa ở trạng thái cuối (đề xuất Q-12).
- Q: Có bắt buộc phải có bản đồng ý chia sẻ dữ liệu đang Hiệu lực thì mới được Hoàn tất tiếp nhận không? → A: Không chặn; hệ thống cảnh báo khi Hoàn tất tiếp nhận và nhắc hành chính định kỳ (CFG-M01-06 đề xuất, mặc định 1 ngày) cho tới khi có bản đồng ý Hiệu lực (Q-03).
- Q: Khi chấp nhận một lần đánh giá, bác sĩ có được bỏ cờ nguy cơ hệ thống đề xuất, hoặc gắn thêm cờ không có trong đề xuất, không? → A: Được gắn thêm cờ (bắt buộc lý do); không được bỏ cờ đề xuất (đề xuất Q-13).
- Q: Sau khi hồ sơ đã Kết thúc lưu trú hoặc Qua đời, người đồng ý (hoặc người đại diện hợp pháp) có được yêu cầu rút lại bản đồng ý chia sẻ dữ liệu không? → A: Có; "Rút lại đồng ý" là ngoại lệ duy nhất cho phép trên hồ sơ trạng thái cuối, do hành chính thực hiện, bắt buộc lý do và bằng chứng (Q-03).
- Q: Quản lý viện có phải duyệt trước khi hành chính hoặc trưởng tầng thực hiện "Cho tạm vắng" hay "Ghi nhận trở về" không? → A: Không; hai lệnh thực hiện trực tiếp. Quyền D của Quản lý viện ở dòng "Tạm vắng, trở về" (4.4) chỉ áp dụng cho quyết định giữ hay giải phóng giường khi vắng quá ngưỡng (BR-M02-07, feature 004).
- Q: Khi hồ sơ đã ở trạng thái cuối mà phát hiện một bản ghi đã xác nhận bị sai, có được tạo bản đính chính không? → A: Được, nhưng mỗi bản đính chính phải qua yêu cầu phê duyệt loại "Đính chính" và chỉ có hiệu lực khi Quản lý viện duyệt.
- Q: Khi đang có một yêu cầu đổi mức chăm sóc chưa áp dụng mà bác sĩ chấp nhận một lần đánh giá lại mới, yêu cầu cũ được xử lý thế nào? → A: Hệ thống chuyển yêu cầu cũ sang trạng thái kết thúc mới "Được thay thế", trỏ tới lần đánh giá và yêu cầu mới; đề xuất bổ sung trạng thái này vào vòng đời chung của feature 000.
- Q: Ai được thực hiện lệnh "Ghi nhận qua đời": chỉ Bác sĩ, hay cả Hành chính như Permission Matrix đang ghi? → A: Bác sĩ khi mất tại viện (Đang lưu trú); khi mất ngoài viện (Tạm vắng, Hoạt động bên ngoài, Điều trị tại bệnh viện), Hành chính cũng được ghi nhận, bắt buộc kèm giấy tờ bằng chứng.
- Q: Dị ứng nên được ghi bằng cách chọn từ danh mục hay gõ văn bản tự do? → A: Chọn từ danh mục dị nguyên là chính; được ghi loại "khác" bằng văn bản, nhưng mục đó mang dấu "không kiểm tra tự động" và người ghi được cảnh báo.
- Q: Nếu hồ sơ nằm danh sách chờ lâu, đánh giá đầu vào đã xác nhận có còn dùng được để Hoàn tất tiếp nhận không? → A: Không, nếu lần đánh giá Đã xác nhận gần nhất đã quá CFG-M01-02 (mặc định 90 ngày) thì chặn Hoàn tất tiếp nhận; phải có một lần đánh giá lại trước.

### Cập nhật 2026-09-26 (đồng bộ với spec 009)

Spec 009 đã chốt khi clarify: chỉ thông báo mức Khẩn cấp được gửi người thân trong giờ yên tĩnh CFG-M13-02 (Q-97), và mọi thông báo phải được module nguồn nêu mức (Q-108, spec 009 FR-043a). Thông báo cho người liên hệ chính khi Chuyển viện và khi Ghi nhận qua đời vì vậy được nêu rõ là mức Khẩn cấp (bảng trạng thái, User Story 4 kịch bản 11), kéo theo xác nhận đã nhận và yêu cầu gọi điện nếu chưa xác nhận (spec 009 FR-026 → FR-030a). Các thông báo khác của spec này (FR-036a) chưa được nêu mức; spec 009 tạm gửi ở mức Nhẹ kèm dấu "thiếu mức" (spec 009 Điểm báo lại 14).

### Cập nhật 2026-09-26 (đồng bộ với spec 012)

Spec 012 đã chốt khi clarify: điều kiện "người đón" của lệnh Cho tạm vắng là người thuộc danh sách được phép đón tại thời điểm đón hoặc có ngoại lệ đón Hiệu lực (BR-M10-03, feature 012 FR-021, FR-024); dòng "Đang lưu trú → Cho tạm vắng" của bảng trạng thái được sửa cho khớp. Cờ nguy cơ đi lạc do spec này quản lý làm dấu "được tự về" của người bán trú mất tác dụng ngay khi được gắn (feature 012 FR-021, Q-125); spec này không đổi hành vi. Dòng "Đang tiếp nhận → Hoàn tất tiếp nhận" thêm điều kiện đủ DBR-02 do feature 012 cung cấp (012 FR-003).

### Cập nhật 2026-09-27 (đồng bộ với spec 011)

Spec 011 cần thêm hai sự kiện từ spec này: mục dị ứng chuyển Đã loại trừ (để tính lại suất đặc biệt, đối chiếu lại đồ ăn gia đình, đóng nguồn cảnh báo xung đột), và mục dị ứng loại "khác" mới (để đưa người cao tuổi vào danh sách cần đối chiếu khi phục vụ). FR-020 được bổ sung tương ứng.

### Cập nhật 2026-09-27 (đồng bộ với spec 013)

Spec 013 và tài liệu nguồn (BR-M12-03, BR-M12-06, Q-148, Q-155) đã chốt:
- Tiền mặt, trang sức chỉ được giao cho người cao tuổi tự giữ khi không có cờ nguy cơ đi lạc; khi cờ đi lạc được gắn trong lúc người cao tuổi đang tự giữ đồ đó, feature 013 báo thu lại. Spec này cung cấp cho feature 013 cờ đi lạc hiện hành và sự kiện gắn, gỡ cờ (FR-038 được bổ sung).
- Điều kiện đồ gửi của lệnh Kết thúc lưu trú (bảng trạng thái) là "không còn đồ gửi ở Đang giữ, Đang được sử dụng hoặc Hư hỏng"; đồ Thất lạc không tính ở điều kiện này nhưng sự cố của nó thuộc điều kiện sự cố mở.
- Thao tác trên đồ gửi sau khi hồ sơ ở trạng thái cuối (thu lại, trả, báo thất lạc, xử lý đồ không người nhận) thuộc ngoại lệ (3) của BR-M01-05, không sửa hồ sơ người cao tuổi.

### Cập nhật 2026-09-27 (đồng bộ với spec 014)

Spec 014 và tài liệu nguồn (5.6, 8.9, BR-M04-04, Q-166, Q-170, Q-171) đã chốt:
- Chuyển Đang lưu trú → Hoạt động bên ngoài do trưởng đoàn, hoặc trưởng tầng thay, điểm danh rời viện; chỉ từ Đang lưu trú. Công việc Chưa đến hạn trong khoảng đi bị **Hủy** theo BR-M04-04 (không "tạm dừng"), sinh lại khi trở về (bảng trạng thái, hai dòng chuyến đi được sửa).
- Chuyển Hoạt động bên ngoài → Đang lưu trú còn xảy ra khi trưởng tầng Hủy ghi nhận bản ghi rời viện của người bị ghi đi nhầm (feature 014 FR-031a); căn cứ ghi "đính chính điểm danh".
- Hoạt động bên ngoài không chuyển sang Tạm vắng; người thân không đón thẳng từ điểm đến (Q-166). Người thiếu khi về giữ Hoạt động bên ngoài tới lệnh phù hợp (không đổi).

## Phạm vi

**Trong phạm vi** (Module 01, mục 5; UC-01 → UC-08):

1. Hồ sơ cá nhân người cao tuổi: tạo, cập nhật qua lệnh, tra cứu theo quyền (5.1, UC-01, UC-02, DBR-01).
2. Bản đồng ý xử lý và chia sẻ dữ liệu: ghi nhận, rút lại (5.1, UC-03, BR-M01-08, DBR-03).
3. Hồ sơ sức khỏe ban đầu; dị ứng, bệnh nền, tiền sử bệnh lưu thành từng mục, chỉ loại trừ, không xóa (5.2, UC-04, BR-M01-07, DBR-04).
4. Đánh giá đầu vào và đánh giá lại bằng thang điểm; quy đổi tự động ra mức chăm sóc đề xuất, cờ nguy cơ, hoạt động mẫu; yêu cầu đánh giá lại tự động (5.3, 5.4, UC-05 → UC-07, BR-M01-02, 03, 09, 10, DBR-05).
5. Vòng đời trạng thái người cao tuổi: tập trạng thái, chuyển hợp lệ, lệnh nghiệp vụ, điều kiện chặn, lịch sử (5.5, 5.6, BR-M01-01, 04, 05, 06).
6. Tính độc lập giữa loại hình lưu trú và mức chăm sóc (1.3, mục 4).

**Ngoài phạm vi** (spec này chỉ **dùng** kết quả hoặc **kích hoạt** feature sở hữu):

- Quy tắc dùng chung (phân loại dữ liệu, đính chính, yêu cầu phê duyệt, nhật ký, tham số): feature 000 — spec này kế thừa, không lặp lại.
- Tài khoản, phân quyền, phạm vi dữ liệu (feature 002, BR-M15-01).
- Giường, phân bổ, giữ chỗ (feature 003); hợp đồng, đặt cọc, danh sách chờ, yêu cầu thay đổi lưu trú, chính sách phí vắng, nội dung chi tiết của tạm vắng/kết thúc lưu trú/qua đời (feature 004).
- Kế hoạch chăm sóc, công việc, lịch cá nhân (feature 005); đơn thuốc, liều, đối chiếu thuốc (feature 006); sự cố, cảnh báo, thẻ thông tin khẩn cấp (feature 007); thông báo (feature 009); chi phí (feature 010); chế độ ăn, suất ăn (feature 011); người thân, quyền của người thân, cổng người thân, đón người cao tuổi (feature 012); đồ gửi (feature 013); hoạt động ngoài viện (feature 014).
- Tác động tự động khi chuyển trạng thái (sinh/hủy công việc, liều, suất ăn, giữ giường…) do feature sở hữu thực hiện; spec này chỉ quy định **khi nào** tác động được kích hoạt.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Hành chính tạo hồ sơ người cao tuổi và nhân viên tra cứu theo quyền (Priority: P1)

Khi một gia đình đăng ký, nhân viên hành chính tạo hồ sơ cá nhân cho người cao tuổi thay cho sổ giấy. Hồ sơ mới ở trạng thái Đang tiếp nhận. Nhân viên các vai trò khác tra cứu hồ sơ trong phạm vi được phân công và chỉ thấy phần thông tin vai trò mình được xem.

**Why this priority**: Mọi nghiệp vụ khác (đánh giá, hợp đồng, chăm sóc, thuốc, chi phí) đều gắn với người cao tuổi (mục 1.3). Không có hồ sơ thì không feature nào chạy được.

**Independent Test**: Tạo một hồ sơ với đầy đủ thông tin, tạo hồ sơ thứ hai trùng CCCD, rồi đăng nhập lần lượt bằng từng vai trò trong Permission Matrix để tra cứu và kiểm tra phần hiển thị.

**Acceptance Scenarios**:

1. **Given** nhân viên hành chính có quyền tạo hồ sơ, **When** nhập họ tên, ngày sinh, giới tính, giấy tờ định danh, ảnh, địa chỉ, thông tin liên hệ, thông tin đặc biệt và lưu, **Then** hệ thống cấp một mã hồ sơ duy nhất, hồ sơ ở trạng thái Đang tiếp nhận, lịch sử trạng thái có bản ghi đầu tiên kèm người tạo và thời điểm.
2. **Given** đã có hồ sơ chưa ở trạng thái cuối mang CCCD X, **When** hành chính tạo hồ sơ mới với cùng CCCD X, **Then** hệ thống từ chối và chỉ ra hồ sơ đang mang CCCD đó (DBR-01).
3. **Given** hồ sơ H1 mang CCCD X đang ở Kết thúc lưu trú (hoặc Hủy tiếp nhận), **When** hành chính tạo hồ sơ mới với cùng CCCD X, **Then** hệ thống tạo hồ sơ H2 ở Đang tiếp nhận với mã hồ sơ mới, liên kết H2 với H1; H1 giữ nguyên chỉ đọc; người được xem H2 xem được H1 qua liên kết theo quyền của họ trên H1 (FR-004).
4. **Given** hồ sơ mới không có CCCD và đã có hồ sơ trùng họ tên và ngày sinh, **When** lưu, **Then** hệ thống cảnh báo khả năng trùng và yêu cầu hành chính xác nhận trước khi tạo.
5. **Given** hồ sơ đang ở trạng thái không phải trạng thái cuối, **When** hành chính thực hiện lệnh "Cập nhật thông tin cá nhân" kèm lý do, **Then** thông tin mới được áp dụng, giá trị trước/sau được ghi nhật ký; **When** thiếu lý do, **Then** hệ thống từ chối.
6. **Given** nhân viên chăm sóc được phân công cho người cao tuổi A trong ca hiện tại, **When** tra cứu hồ sơ A, **Then** thấy hồ sơ; **When** tra cứu hồ sơ B ngoài phạm vi, **Then** hệ thống từ chối (BR-M15-01).
7. **Given** nhân viên bếp hoặc nhân viên vệ sinh, **When** tra cứu hồ sơ người cao tuổi, **Then** hệ thống từ chối (Permission Matrix dòng "Hồ sơ người cao tuổi").
8. **Given** dinh dưỡng viên tra cứu hồ sơ trong phạm vi, **When** mở phần sức khỏe, **Then** chỉ thấy dị ứng và bệnh lý liên quan chế độ ăn, không thấy toàn bộ hồ sơ sức khỏe (19.3).
9. **Given** người thân của người cao tuổi A, **When** xem hồ sơ A, **Then** thấy thông tin cá nhân; phần sức khỏe chỉ hiện khi có bản đồng ý đang hiệu lực bao gồm người thân đó (User Story 5); **When** xem hồ sơ người cao tuổi khác, **Then** hệ thống từ chối.

---

### User Story 2 - Bác sĩ, điều dưỡng ghi hồ sơ sức khỏe ban đầu; dị ứng và bệnh nền là từng mục chỉ loại trừ (Priority: P1)

Khi tiếp nhận, bác sĩ hoặc điều dưỡng ghi hồ sơ sức khỏe ban đầu. Dị ứng, bệnh nền, tiền sử bệnh được ghi thành từng mục riêng có mức độ, nguồn, người ghi, ngày ghi. Khi một mục không còn đúng, người có quyền loại trừ mục đó kèm lý do; mục không bao giờ bị xóa hay sửa nội dung.

**Why this priority**: Dị ứng là thông tin an toàn: nó được dùng để kiểm tra đơn thuốc (BR-M07-06), chế độ ăn (BR-M08-02) và hiển thị trên thẻ thông tin khẩn cấp (9.5). Mất hoặc sửa âm thầm một mục dị ứng có thể gây hại trực tiếp cho người cao tuổi.

**Independent Test**: Ghi 2 mục dị ứng và 1 bệnh nền, loại trừ 1 mục dị ứng, rồi thử xóa và sửa nội dung; kiểm tra danh sách hiện hành, danh sách đã loại trừ và nhật ký.

**Acceptance Scenarios**:

1. **Given** hồ sơ đang Đang tiếp nhận hoặc Đang lưu trú, **When** bác sĩ hoặc điều dưỡng ghi một mục dị ứng với nội dung, mức độ, nguồn thông tin, **Then** hệ thống lưu mục ở trạng thái Hiệu lực kèm người ghi và ngày ghi.
2. **Given** một mục dị ứng mới vừa được ghi, **When** lưu thành công, **Then** hệ thống kích hoạt kiểm tra lại mọi đơn thuốc đang hiệu lực (BR-M07-06) và chế độ ăn, thực đơn đang phân bổ (BR-M08-02) của người cao tuổi đó; mỗi xung đột tạo một cảnh báo.
3. **Given** một mục đang Hiệu lực, **When** bác sĩ hoặc điều dưỡng chọn "Loại trừ" kèm lý do, **Then** mục chuyển Đã loại trừ, vẫn hiển thị trong lịch sử kèm lý do, người loại trừ, thời điểm; **When** thiếu lý do, **Then** hệ thống từ chối.
4. **Given** bất kỳ mục nào, **When** bất kỳ ai (kể cả quản lý viện) cố xóa hoặc sửa nội dung mục, **Then** hệ thống không có thao tác đó (DBR-04).
5. **Given** mục dị ứng ghi sai nội dung, **When** cần sửa, **Then** người dùng loại trừ mục sai với lý do "ghi nhầm" và ghi một mục mới đúng; cả hai mục cùng hiện trong lịch sử.
6. **Given** nhân viên chăm sóc, trưởng tầng hoặc dinh dưỡng viên, **When** cố ghi hoặc loại trừ một mục, **Then** hệ thống từ chối (Permission Matrix dòng "Dị ứng, bệnh nền": chỉ BS, ĐD là T).
7. **Given** người cao tuổi đã có một mục dị ứng Hiệu lực với cùng dị nguyên trong danh mục, **When** ghi thêm mục trùng, **Then** hệ thống cảnh báo trùng và yêu cầu xác nhận.
8. **Given** dị nguyên "Penicillin" có trong danh mục và người cao tuổi đang có đơn thuốc chứa amoxicillin, **When** điều dưỡng chọn "Penicillin" từ danh mục và lưu, **Then** việc kiểm tra theo FR-020 phát hiện xung đột và tạo cảnh báo; **When** thay vào đó điều dưỡng ghi dị ứng loại "khác" với văn bản "phấn hoa cây X" (chưa có trong danh mục), **Then** mục được lưu, hệ thống cảnh báo ngay rằng mục này không được kiểm tra tự động, và mục mang dấu "không kiểm tra tự động" trên hồ sơ và thẻ thông tin khẩn cấp (FR-018a).

---

### User Story 3 - Bác sĩ đánh giá đầu vào bằng thang điểm; hệ thống đề xuất mức chăm sóc và cờ nguy cơ (Priority: P1)

Bác sĩ (có điều dưỡng hỗ trợ nhập điểm khi cần) chấm từng thang điểm được cấu hình. Hệ thống tự tính tổng điểm, áp bảng quy đổi CFG-M01-05 để đề xuất mức chăm sóc, cờ nguy cơ và hoạt động chăm sóc mẫu. Bác sĩ chấp nhận hoặc điều chỉnh; điều chỉnh khác đề xuất bắt buộc có lý do. Kết quả được chấp nhận là căn cứ lập kế hoạch chăm sóc.

**Why this priority**: 5.6 chặn Hoàn tất tiếp nhận khi chưa có đánh giá đầu vào hoàn tất; mức chăm sóc là đầu vào của hợp đồng, phân bổ giường (BR-M03-01), tỷ lệ phục vụ (BR-M09-02) và kế hoạch chăm sóc.

**Independent Test**: Nhập bộ điểm cho từng dòng của bảng 5.3 (ví dụ Barthel 15, Braden 10, Morse 50, MMSE 25), kiểm tra đề xuất; chấp nhận một lần, điều chỉnh một lần có và không có lý do.

**Acceptance Scenarios**:

1. **Given** hồ sơ ở Đang tiếp nhận, **When** bác sĩ hoặc điều dưỡng tạo lần đánh giá đầu vào và nhập điểm từng mục của các thang, **Then** hệ thống tự tính tổng điểm từng thang và phân loại theo CFG-M01-05; lần đánh giá ở trạng thái Nháp.
2. **Given** Barthel = 15, Braden = 10, Morse = 50, MMSE = 25 và CFG-M01-05 đang ở giá trị mặc định, **When** hệ thống quy đổi, **Then** đề xuất mức "Chăm sóc đặc biệt", cờ nguy cơ loét và cờ nguy cơ ngã, không có cờ đi lạc, và các hoạt động mẫu "xoay trở 2 giờ/lần", "kiểm tra da mỗi ca", "hỗ trợ khi di chuyển", "kiểm tra an toàn phòng mỗi ca".
3. **Given** lần đánh giá Nháp còn thiếu điểm của một thang bắt buộc, **When** bác sĩ chấp nhận, **Then** hệ thống từ chối và chỉ ra thang còn thiếu.
4. **Given** đề xuất mức "Chăm sóc thường xuyên", **When** bác sĩ chấp nhận đúng đề xuất, **Then** lần đánh giá chuyển Đã xác nhận, mức chăm sóc hiện hành của người cao tuổi là "Chăm sóc thường xuyên", cờ nguy cơ được gắn, và các hoạt động mẫu được chép vào bản nháp phiên bản kế hoạch chăm sóc mới (BR-M01-09, feature 005).
5. **Given** đề xuất mức "Chăm sóc thường xuyên", **When** bác sĩ chọn "Phục hồi sau tai biến" mà không nhập lý do, **Then** hệ thống từ chối (DBR-05); **When** có lý do, **Then** lần đánh giá lưu cả mức đề xuất, mức được chấp nhận và lý do.
6. **Given** kết quả quy đổi đề xuất cờ nguy cơ ngã, **When** bác sĩ chấp nhận nhưng bỏ cờ nguy cơ ngã, **Then** hệ thống không cho bỏ; **When** bác sĩ gắn thêm cờ nguy cơ đi lạc (MMSE không đề xuất) mà không có lý do, **Then** hệ thống từ chối; khi có lý do, cờ đi lạc được gắn với nguồn "bác sĩ gắn thêm" (FR-030).
7. **Given** điều dưỡng đã nhập đủ điểm, **When** điều dưỡng cố chấp nhận kết quả, **Then** hệ thống từ chối; chỉ bác sĩ được chấp nhận (Permission Matrix: BS T, D; ĐD T).
8. **Given** lần đánh giá đã Đã xác nhận, **When** bất kỳ ai cố sửa điểm, **Then** hệ thống không có thao tác sửa; sai sót xử lý bằng đính chính theo feature 000 (xem Edge Cases).
9. **Given** người cao tuổi có loại hình lưu trú "Bán trú" theo hợp đồng, **When** đánh giá cho mức "Chăm sóc đặc biệt", **Then** hệ thống chấp nhận tổ hợp này; mức chăm sóc thay đổi không làm thay đổi loại hình lưu trú và ngược lại (FR-060).

---

### User Story 4 - Trạng thái người cao tuổi chỉ thay đổi qua lệnh nghiệp vụ có điều kiện (Priority: P1)

Không ai "sửa trạng thái". Mỗi chuyển trạng thái là một lệnh nghiệp vụ riêng (Hoàn tất tiếp nhận, Hủy tiếp nhận, Cho tạm vắng, Ghi nhận trở về, Chuyển viện, Kết thúc lưu trú, Ghi nhận qua đời, và hai chuyển do điểm danh chuyến đi ngoài viện). Mỗi lệnh chỉ chạy khi trạng thái hiện tại cho phép theo bảng 5.5 và điều kiện ở 5.6 được thỏa; nếu không, hệ thống từ chối và nêu điều kiện chưa thỏa. Mọi lần chuyển được ghi lịch sử.

**Why this priority**: Trạng thái người cao tuổi quyết định công việc nào được sinh, liều thuốc nào được phát, suất ăn nào được nấu, giường nào được giữ. Chuyển sai trạng thái kéo theo sai ở hầu hết module khác.

**Independent Test**: Đi hết các chuyển hợp lệ trong bảng trạng thái (mục C), thử mọi cặp trạng thái không có trong bảng, và thử từng lệnh khi thiếu từng điều kiện chặn.

**Acceptance Scenarios**:

1. **Given** hồ sơ Đang tiếp nhận có đánh giá đầu vào Đã xác nhận, hợp đồng Hiệu lực, đặt cọc đạt, loại hình nội trú và đã có giường, **When** hành chính thực hiện "Hoàn tất tiếp nhận", **Then** trạng thái chuyển Đang lưu trú, lịch sử ghi trạng thái từ/đến, lệnh, người, thời điểm, lý do, và các feature 005, 006, 011 được kích hoạt sinh lịch cá nhân, lịch thuốc, suất ăn từ ngày hiệu lực.
2. **Given** hồ sơ Đang tiếp nhận chưa có đánh giá đầu vào Đã xác nhận, **When** thực hiện "Hoàn tất tiếp nhận", **Then** hệ thống từ chối, nêu rõ "chưa hoàn tất đánh giá đầu vào", trạng thái giữ nguyên; tương tự khi thiếu hợp đồng Hiệu lực, đặt cọc chưa đạt, hoặc nội trú chưa có giường — hệ thống liệt kê tất cả điều kiện chưa thỏa trong một lần. **Given** hồ sơ Đang tiếp nhận có đánh giá đầu vào được chấp nhận cách đây 120 ngày (CFG-M01-02 mặc định \[90 ngày\]) và đủ các điều kiện khác, **When** thực hiện "Hoàn tất tiếp nhận", **Then** hệ thống từ chối với "đánh giá đã quá hạn, cần đánh giá lại"; **When** bác sĩ chấp nhận một lần đánh giá lại rồi thực hiện lại lệnh, **Then** lệnh thành công và mức chăm sóc hiện hành là mức của lần đánh giá lại (FR-034a, FR-035).
3. **Given** hồ sơ Bán trú đủ các điều kiện khác nhưng không có giường, **When** thực hiện "Hoàn tất tiếp nhận", **Then** hệ thống chấp nhận (điều kiện giường chỉ áp dụng cho nội trú, 3.3).
4. **Given** hồ sơ Đang tiếp nhận, **When** hành chính thực hiện "Hủy tiếp nhận" kèm lý do, **Then** trạng thái chuyển Hủy tiếp nhận, giữ chỗ giường nếu có bị hủy (feature 003), hồ sơ chờ liên quan bị đóng (feature 004).
5. **Given** hồ sơ Hoạt động bên ngoài, **When** bất kỳ ai thực hiện "Kết thúc lưu trú", **Then** hệ thống từ chối vì chuyển này không có trong bảng 5.5.
6. **Given** hồ sơ Đang lưu trú, **When** trưởng đoàn điểm danh rời viện cho chuyến đi ngoài viện có người cao tuổi đó (feature 014), **Then** hệ thống tự chuyển trạng thái sang Hoạt động bên ngoài, người thực hiện ghi là người điểm danh, căn cứ là chuyến đi (BR-M04-16).
7. **Given** hồ sơ Điều trị tại bệnh viện, **When** thực hiện "Ghi nhận trở về", **Then** trạng thái chuyển Đang lưu trú; toàn bộ lịch thuốc cũ bị tạm dừng và yêu cầu đối chiếu thuốc được tạo (BR-M07-09, feature 006); một yêu cầu đánh giá lại được tạo (User Story 6).
8. **Given** hồ sơ Đang lưu trú và người đón thuộc danh sách được phép đón, **When** trưởng tầng thực hiện "Cho tạm vắng" kèm lý do, **Then** trạng thái chuyển Tạm vắng ngay, không tạo yêu cầu phê duyệt nào (FR-047a).
9. **Given** hồ sơ Tạm vắng có thời điểm dự kiến trở lại T, **When** đến T + CFG-M01-01 (mặc định \[2 giờ\]) mà chưa có "Ghi nhận trở về", **Then** hệ thống cảnh báo trưởng tầng và hành chính (BR-M01-01), trạng thái giữ nguyên Tạm vắng.
10. **Given** hồ sơ Đang lưu trú còn đồ gửi đang giữ, **When** thực hiện "Kết thúc lưu trú", **Then** hệ thống từ chối và nêu điều kiện chưa thỏa; **When** quản lý viện đã duyệt ngoại lệ cho lần kết thúc này, **Then** lệnh được thực hiện, lịch sử ghi tham chiếu yêu cầu ngoại lệ.
11. **Given** hồ sơ Tạm vắng (người cao tuổi mất tại nhà), **When** hành chính thực hiện "Ghi nhận qua đời" kèm bản scan giấy báo tử, **Then** trạng thái chuyển Qua đời, người liên hệ chính được thông báo ở mức Khẩn cấp (feature 009, Q-97), hồ sơ chuyển chỉ đọc; **When** thiếu giấy tờ bằng chứng, **Then** hệ thống từ chối; **Given** hồ sơ Đang lưu trú, **When** hành chính thực hiện "Ghi nhận qua đời", **Then** hệ thống từ chối vì người cao tuổi mất tại viện phải do bác sĩ xác nhận (FR-047b).
12. **Given** hồ sơ ở Kết thúc lưu trú, Qua đời hoặc Hủy tiếp nhận, **When** bất kỳ ai cố cập nhật thông tin cá nhân, ghi mục sức khỏe, tạo đánh giá hay thực hiện lệnh chuyển trạng thái, **Then** hệ thống chặn; hồ sơ vẫn tra cứu được theo quyền (BR-M01-05); **When** hành chính thực hiện "Rút lại đồng ý" kèm lý do và bằng chứng trên hồ sơ Kết thúc lưu trú có bản đồng ý Hiệu lực, **Then** lệnh được thực hiện và người thân trong phạm vi mất quyền xem sức khỏe ngay (FR-014a); **When** bác sĩ lập bản đính chính điểm Barthel của lần đánh giá lại cuối cùng trên hồ sơ Qua đời, **Then** hệ thống tạo yêu cầu phê duyệt loại "Đính chính" ở Chờ duyệt, bản gốc chưa bị đánh dấu; bản đính chính chỉ được lưu khi Quản lý viện duyệt, và không tạo yêu cầu đánh giá lại (FR-049a).
13. **Given** hai người thực hiện hai lệnh khác nhau trên cùng một hồ sơ gần như đồng thời (ví dụ "Cho tạm vắng" và "Chuyển viện"), **When** cả hai gửi, **Then** chỉ lệnh đến trước được áp dụng; lệnh sau được kiểm tra lại trên trạng thái mới và bị từ chối nếu không còn hợp lệ.

---

### User Story 5 - Hành chính ghi nhận và thu hồi bản đồng ý chia sẻ dữ liệu (Priority: P2)

Khi tiếp nhận, hành chính ghi nhận bản đồng ý xử lý và chia sẻ dữ liệu của người cao tuổi (hoặc của người đại diện hợp pháp khi người cao tuổi không đủ khả năng): phạm vi chia sẻ thông tin sức khỏe cho những người thân nào, sử dụng hình ảnh, nhận thông báo; kèm bản ký được scan. Khi đồng ý bị rút lại, quyền xem sức khỏe tương ứng của người thân tắt ngay.

**Why this priority**: Đây là căn cứ pháp lý để cổng người thân hiển thị thông tin sức khỏe (BR-M01-08, DBR-03). Chăm sóc tại viện vẫn chạy được khi chưa có cổng người thân, nên xếp sau nhóm P1.

**Independent Test**: Ghi nhận một bản đồng ý bao gồm người thân X nhưng không gồm Y, kiểm tra X xem được sức khỏe còn Y không; rút lại bản đồng ý và kiểm tra X mất quyền ngay.

**Acceptance Scenarios**:

1. **Given** hồ sơ chưa ở trạng thái cuối, **When** hành chính ghi nhận bản đồng ý với người đồng ý, phạm vi (danh sách người thân được xem sức khỏe, sử dụng hình ảnh, nhận thông báo), thời điểm, bản scan có chữ ký, **Then** bản đồng ý ở trạng thái Hiệu lực.
2. **Given** thiếu bản scan bằng chứng, **When** lưu, **Then** hệ thống từ chối.
3. **Given** người đồng ý là người đại diện, **When** lưu, **Then** hệ thống yêu cầu người đó đang là người đại diện của người cao tuổi (feature 012) và yêu cầu ghi lý do người cao tuổi không tự đồng ý.
4. **Given** bản đồng ý Hiệu lực bao gồm người thân X, **When** X mở phần sức khỏe trên cổng người thân, **Then** thấy thông tin; **Given** người thân Y không có trong phạm vi, **When** Y mở, **Then** không thấy (BR-M01-08).
5. **Given** bản đồng ý Hiệu lực, **When** hành chính ghi nhận "Rút lại đồng ý" kèm lý do và bằng chứng yêu cầu rút lại, **Then** bản đồng ý chuyển Đã rút lại, quyền xem sức khỏe của mọi người thân thuộc phạm vi đó bị tắt ngay ở lần truy cập tiếp theo (liên kết 14.1).
6. **Given** bản đồng ý Hiệu lực, **When** cần đổi phạm vi (thêm/bớt người thân), **Then** hành chính ghi nhận bản đồng ý mới; bản cũ chuyển Đã rút lại với lý do "được thay thế" và tham chiếu bản mới; người thân bị bớt mất quyền ngay.
7. **Given** hồ sơ Đang tiếp nhận đủ mọi điều kiện ở 5.6 nhưng chưa có bản đồng ý Hiệu lực, **When** hành chính thực hiện "Hoàn tất tiếp nhận", **Then** lệnh được thực hiện, hệ thống hiển thị cảnh báo "chưa có bản đồng ý chia sẻ dữ liệu", và từ đó nhắc hành chính mỗi CFG-M01-06 (mặc định \[1 ngày\]); **When** bản đồng ý được ghi nhận, **Then** việc nhắc dừng.
8. **Given** người cao tuổi Đang lưu trú chưa có bản đồng ý Hiệu lực, **When** bất kỳ người thân nào mở phần sức khỏe trên cổng người thân, **Then** không thấy thông tin sức khỏe.
9. **Given** trưởng tầng, bác sĩ hoặc điều dưỡng, **When** cố ghi nhận hoặc rút lại bản đồng ý, **Then** hệ thống từ chối (Permission Matrix: chỉ HC là T).

---

### User Story 6 - Đánh giá lại theo định kỳ, sau sự cố, sau nằm viện; mức chăm sóc thay đổi thì tạo yêu cầu thay đổi lưu trú (Priority: P2)

Hệ thống tự tạo yêu cầu đánh giá lại khi đến hạn định kỳ, sau sự cố ngã hoặc sự cố mức trung bình trở lên, và khi người cao tuổi trở về từ bệnh viện. Bác sĩ, điều dưỡng cũng tạo được yêu cầu khi tình trạng thay đổi đáng kể hoặc có yêu cầu chuyên môn. Đánh giá lại dùng cùng thang điểm và quy đổi như đánh giá đầu vào. Nếu mức chăm sóc được chấp nhận khác mức hiện tại, hệ thống tạo yêu cầu thay đổi lưu trú chờ duyệt; cờ nguy cơ chỉ được gỡ qua đánh giá lại.

**Why this priority**: Giữ mức chăm sóc và cờ nguy cơ khớp với tình trạng thực tế. Cần đánh giá đầu vào (User Story 3) có trước.

**Independent Test**: Dùng đồng hồ giả lập (NFR-13) đưa thời gian tới hạn CFG-M01-02, kiểm tra yêu cầu được tạo; hoàn thành đánh giá lại với mức khác mức hiện tại và kiểm tra yêu cầu thay đổi lưu trú; để một yêu cầu quá CFG-M01-03 và kiểm tra cảnh báo.

**Acceptance Scenarios**:

1. **Given** lần đánh giá Đã xác nhận gần nhất của người cao tuổi Đang lưu trú cách hiện tại CFG-M01-02 (mặc định \[90 ngày\]), **When** Bộ lập lịch hệ thống chạy, **Then** một yêu cầu đánh giá lại lý do "định kỳ" được tạo với hạn hoàn thành CFG-M01-03 (mặc định \[48 giờ\]) (BR-M01-02, UC-07).
2. **Given** feature 007 ghi nhận một sự cố ngã hoặc sự cố mức trung bình trở lên cho người cao tuổi, **When** sự cố được ghi, **Then** một yêu cầu đánh giá lại lý do "sau sự cố" được tạo, tham chiếu sự cố.
3. **Given** người cao tuổi đã có một yêu cầu đánh giá lại đang mở, **When** phát sinh thêm một căn cứ (ví dụ sự cố ngã), **Then** hệ thống không tạo yêu cầu thứ hai mà bổ sung căn cứ mới vào yêu cầu đang mở; hạn hoàn thành giữ theo hạn sớm hơn.
4. **Given** yêu cầu đánh giá lại quá hạn CFG-M01-03 chưa hoàn thành, **When** Bộ lập lịch kiểm tra, **Then** hệ thống cảnh báo bác sĩ; yêu cầu vẫn mở tới khi có đánh giá lại Đã xác nhận.
5. **Given** mức chăm sóc hiện hành là "Chăm sóc cơ bản", **When** bác sĩ chấp nhận đánh giá lại với mức "Chăm sóc đặc biệt", **Then** hệ thống tạo yêu cầu thay đổi lưu trú loại "đổi mức chăm sóc" ở trạng thái Chờ duyệt (BR-M01-03, feature 004); mức chăm sóc hiện hành và kế hoạch chăm sóc cũ giữ nguyên tới ngày hiệu lực của thay đổi.
6. **Given** đã có yêu cầu đổi mức sang "Chăm sóc đặc biệt" đang Chờ duyệt, **When** bác sĩ chấp nhận đánh giá lại mới với mức "Chăm sóc thường xuyên", **Then** yêu cầu cũ chuyển "Được thay thế" (không tạo tác động, trỏ tới lần đánh giá và yêu cầu mới), một yêu cầu mới sang "Chăm sóc thường xuyên" được tạo ở Chờ duyệt; **When** thay vào đó mức mới bằng mức hiện hành, **Then** yêu cầu cũ chuyển "Được thay thế" và không có yêu cầu mới (FR-036a).
7. **Given** người cao tuổi đang mang cờ nguy cơ ngã, **When** đánh giá lại có Morse cho kết quả không còn nguy cơ theo CFG-M01-05 và bác sĩ chấp nhận, **Then** cờ nguy cơ ngã được gỡ, ghi lần đánh giá gỡ cờ (BR-M01-10).
8. **Given** người cao tuổi đang mang cờ nguy cơ ngã, **When** bất kỳ ai cố gỡ cờ trực tiếp, không qua đánh giá lại, **Then** hệ thống không có thao tác đó.
9. **Given** bác sĩ chấp nhận đánh giá lại, **When** lưu, **Then** yêu cầu đánh giá lại đang mở được đóng với tham chiếu lần đánh giá, và feature 005 được kích hoạt tạo yêu cầu xem xét kế hoạch chăm sóc (BR-M04-20).

---

### Edge Cases

- **Đánh giá đã xác nhận bị nhập sai điểm**: sửa bằng đính chính theo feature 000. Đính chính không tự đổi mức chăm sóc hay cờ nguy cơ đã áp dụng; nếu quy đổi theo điểm đã đính chính khác kết quả đã chấp nhận, hệ thống tạo yêu cầu đánh giá lại với lý do "đính chính đánh giá".
- **Đánh giá lại không có thang tương ứng với một cờ đang gắn** (ví dụ không chấm Morse): cờ nguy cơ ngã giữ nguyên; chỉ thang tương ứng cho kết quả không còn nguy cơ mới gỡ được cờ.
- **Nhiều thang cùng đề xuất mức chăm sóc**: mức đề xuất là mức cao nhất trong các đề xuất, theo thứ tự mức trong CFG-M01-05.
- **Người cao tuổi đang Điều trị tại bệnh viện khi đến hạn đánh giá định kỳ**: yêu cầu định kỳ vẫn được tạo, nhưng hạn hoàn thành chỉ bắt đầu tính từ khi Ghi nhận trở về; yêu cầu này gộp với yêu cầu "trở về từ bệnh viện".
- **Tạm vắng dài vượt dự kiến**: BR-M01-01 cảnh báo đúng một lần mỗi mốc dự kiến trở lại; nếu thời điểm dự kiến được gia hạn (feature 004), mốc cảnh báo tính lại theo thời điểm mới.
- **Hoạt động bên ngoài không trở về đúng kế hoạch**: trạng thái giữ nguyên Hoạt động bên ngoài; feature 014/007 tạo sự cố khẩn cấp (BR-M04-17); trạng thái sau đó chuyển bằng lệnh phù hợp (Ghi nhận trở về, Chuyển viện, Ghi nhận qua đời).
- **Mức chăm sóc đang có yêu cầu thay đổi chờ duyệt thì có đánh giá lại mới cho mức khác nữa**: yêu cầu cũ chưa áp dụng được hệ thống chuyển sang "Được thay thế" và yêu cầu mới được tạo; nếu mức mới trùng mức hiện hành thì yêu cầu cũ vẫn chuyển "Được thay thế" và không có yêu cầu mới (FR-036a).
- **Loại trừ mục dị ứng**: không kích hoạt tự động bỏ cảnh báo đã tạo; cảnh báo đã tạo xử lý theo feature 007.
- **Rút lại đồng ý khi người thân đang mở trang sức khỏe**: dữ liệu sức khỏe không được trả thêm từ lần truy cập kế tiếp; không có khoảng trễ chờ đăng nhập lại.
- **Hồ sơ ở trạng thái cuối cần rút lại đồng ý** (người đại diện yêu cầu sau khi kết thúc lưu trú hoặc sau khi người cao tuổi qua đời): hành chính thực hiện "Rút lại đồng ý" như ngoại lệ của BR-M01-05 (FR-014a); ngoài đính chính có Quản lý viện duyệt (FR-049a), mọi thao tác ghi khác vẫn bị chặn. Không được ghi nhận bản đồng ý mới trên hồ sơ trạng thái cuối.
- **Người cao tuổi từng Hủy tiếp nhận hoặc Kết thúc lưu trú rồi đăng ký lại**: tạo hồ sơ mới liên kết hồ sơ cũ (FR-004); mục dị ứng cũ chỉ là tham khảo, bác sĩ/điều dưỡng phải ghi lại thành mục của hồ sơ mới để có hiệu lực.
- **Người từng lưu trú có nhiều hồ sơ cũ** (quay lại nhiều lần): hồ sơ mới liên kết với hồ sơ gần nhất; chuỗi liên kết cho xem được toàn bộ các lần trước.

## Requirements *(mandatory)*

### Functional Requirements

Mọi yêu cầu dưới đây kế thừa feature 000: nhóm dữ liệu (mục 1.5), lý do bắt buộc cho lệnh nhóm 2, đính chính cho nhóm 3, nhật ký cho mọi thay đổi, tham số theo mã CFG. Phân nhóm dữ liệu của feature này:

| Dữ liệu | Nhóm (1.5) | Thao tác được phép |
| --- | --- | --- |
| Danh mục dị nguyên | 1 – Danh mục | Tạo, sửa, Ngừng hiệu lực (người có quyền cấu hình danh mục thuốc/món ăn, theo feature 006, 011) |
| Thang điểm và mục chấm | 1 – Danh mục | Tạo, sửa, Ngừng hiệu lực (Quản lý viện, sau khi bác sĩ cơ sở thống nhất) |
| Người cao tuổi (thông tin cá nhân, trạng thái, loại hình lưu trú, mức chăm sóc) | 2 | Lệnh nghiệp vụ |
| Bản đồng ý chia sẻ dữ liệu | 2 | Ghi nhận, Rút lại |
| Mục sức khỏe (dị ứng, bệnh nền, tiền sử bệnh) | 2 | Ghi nhận, Loại trừ |
| Cờ nguy cơ | 2 | Chỉ gắn/gỡ qua đánh giá đã xác nhận |
| Yêu cầu đánh giá lại | 2 | Tạo (hệ thống hoặc BS/ĐD), bổ sung căn cứ, đóng khi có đánh giá |
| Hồ sơ sức khỏe ban đầu | 3 (sau xác nhận) | Nháp sửa được; sau xác nhận chỉ đính chính |
| Lần đánh giá, kết quả thang điểm | 3 (sau xác nhận) | Nháp sửa được; sau xác nhận chỉ đính chính |
| Lịch sử trạng thái | 3 | Chỉ ghi thêm |

#### A. Hồ sơ cá nhân

- **FR-001**: Hệ thống MUST cho phép nhân viên hành chính tạo hồ sơ người cao tuổi gồm: họ tên, ngày sinh, giới tính, giấy tờ định danh (CCCD hoặc giấy tờ khác), ảnh, địa chỉ, thông tin liên hệ, thông tin đặc biệt. Họ tên, ngày sinh, giới tính là bắt buộc. Thông tin người thân không thuộc hồ sơ này (Module 10, feature 012). *(Nguồn: 5.1, UC-01, Permission Matrix "Hồ sơ người cao tuổi": HC T)*
- **FR-002**: Hồ sơ mới MUST ở trạng thái Đang tiếp nhận và MUST có bản ghi lịch sử trạng thái đầu tiên. *(Nguồn: 5.5, BR-M01-04)*
- **FR-003**: Mã hồ sơ MUST duy nhất và do hệ thống cấp; CCCD, nếu có, MUST duy nhất trong các hồ sơ chưa ở trạng thái cuối (Kết thúc lưu trú, Qua đời, Hủy tiếp nhận); mỗi hồ sơ MUST có đúng một trạng thái hiện tại. *(Nguồn: DBR-01; Clarification 2026-09-25)*
- **FR-004**: Khi một người đã có hồ sơ ở trạng thái Kết thúc lưu trú hoặc Hủy tiếp nhận đăng ký tiếp nhận lại, hệ thống MUST tạo hồ sơ mới (mã hồ sơ mới, trạng thái Đang tiếp nhận) và liên kết với hồ sơ cũ; hồ sơ cũ MUST giữ nguyên chỉ đọc (BR-M01-05). Người được xem hồ sơ mới MUST xem được hồ sơ cũ qua liên kết, trong giới hạn quyền của họ trên hồ sơ cũ. Hồ sơ mới MUST NOT kế thừa tự động trạng thái, bản đồng ý, đánh giá hay cờ nguy cơ của hồ sơ cũ; mục sức khỏe Hiệu lực của hồ sơ cũ MAY được hiển thị làm tham khảo khi ghi mục sức khỏe cho hồ sơ mới. Hồ sơ Qua đời MUST NOT được dùng làm căn cứ tạo hồ sơ mới. *(Nguồn: DBR-01, BR-M01-05, 5.5, 6.2; Clarification 2026-09-25, đề xuất Q-12)*
- **FR-005**: Khi hồ sơ mới không có CCCD và trùng họ tên và ngày sinh với một hồ sơ đã có, hệ thống MUST cảnh báo và yêu cầu hành chính xác nhận trước khi tạo.
- **FR-006**: Thông tin cá nhân MUST chỉ thay đổi qua lệnh "Cập nhật thông tin cá nhân" do hành chính thực hiện, kèm lý do; nhật ký ghi giá trị trước/sau. Lệnh MUST bị chặn khi hồ sơ ở trạng thái cuối. *(Nguồn: 1.5 nhóm 2, BR-M01-05)*
- **FR-007**: Tra cứu hồ sơ MUST tuân theo Permission Matrix dòng "Hồ sơ người cao tuổi": Quản lý viện, bác sĩ xem (X); trưởng tầng, điều dưỡng, nhân viên chăm sóc, dinh dưỡng viên xem trong phạm vi được phân công (P); hành chính thực hiện (T); nhân viên bếp, nhân viên vệ sinh không có quyền; người thân chỉ xem hồ sơ của người cao tuổi mình có quan hệ. Quyền hiệu lực MUST là giao của vai trò, phạm vi dữ liệu và điều kiện pháp lý. *(Nguồn: UC-02, 4.4, BR-M15-01)*
- **FR-008**: Trong hồ sơ, phần sức khỏe MUST lọc thêm theo vai trò: dinh dưỡng viên chỉ thấy dị ứng và bệnh lý liên quan chế độ ăn; nhân viên chăm sóc xem dị ứng, bệnh nền trong phạm vi (P) nhưng không xem kết quả đánh giá; người thân chỉ thấy dị ứng, bệnh nền khi thỏa FR-022. *(Nguồn: 19.3, 4.4 dòng "Dị ứng, bệnh nền" và "Đánh giá, quy đổi mức chăm sóc")*
- **FR-009**: Hồ sơ MUST hiển thị trạng thái hiện tại, loại hình lưu trú hiện hành, mức chăm sóc hiện hành và các cờ nguy cơ đang gắn cho mọi người dùng được xem hồ sơ (trừ người thân với cờ nguy cơ, vốn là kết quả đánh giá).

#### B. Bản đồng ý chia sẻ dữ liệu

- **FR-010**: Hệ thống MUST cho phép hành chính ghi nhận bản đồng ý gồm: người đồng ý (người cao tuổi, hoặc người đại diện hợp pháp khi người cao tuổi không đủ khả năng); phạm vi (danh sách người thân được xem thông tin sức khỏe; đồng ý sử dụng hình ảnh; đồng ý nhận thông báo); thời điểm; bằng chứng là bản ký được scan (bắt buộc); trạng thái Hiệu lực / Đã rút lại. *(Nguồn: 5.1, UC-03, 4.4 dòng "Bản đồng ý chia sẻ dữ liệu": HC T, QL X, NT X)*
- **FR-011**: Khi người đồng ý là người đại diện, người đó MUST đang là người đại diện của người cao tuổi (14.1, feature 012) và bản đồng ý MUST ghi lý do người cao tuổi không tự đồng ý.
- **FR-012**: Bản đồng ý MUST NOT là điều kiện chặn của "Hoàn tất tiếp nhận". Khi Hoàn tất tiếp nhận mà người cao tuổi chưa có bản đồng ý Hiệu lực, hệ thống MUST cảnh báo người thực hiện (lệnh vẫn được thực hiện) và MUST nhắc hành chính theo chu kỳ CFG-M01-06 (đề xuất, mặc định \[1 ngày\]) cho tới khi có bản đồng ý Hiệu lực hoặc hồ sơ chuyển trạng thái cuối. Trong thời gian chưa có bản đồng ý Hiệu lực, không người thân nào xem được thông tin sức khỏe (FR-022). Mẫu bản đồng ý và các mục phạm vi bắt buộc theo căn cứ pháp lý được chốt ở Q-03 (mặc định: Nghị định 13/2023/NĐ-CP, cần đối chiếu văn bản mới nhất). *(Nguồn: 5.1, 5.6, Q-03; Clarification 2026-09-25)*
- **FR-013**: Mỗi người cao tuổi MUST có tối đa một bản đồng ý Hiệu lực tại một thời điểm. Ghi nhận bản mới khi đã có bản Hiệu lực MUST chuyển bản cũ sang Đã rút lại với lý do "được thay thế" và tham chiếu bản mới, trong cùng một lần thực hiện.
- **FR-014**: Lệnh "Rút lại đồng ý" MUST do hành chính thực hiện, kèm lý do và bằng chứng yêu cầu rút lại của người đồng ý (hoặc người đại diện hợp pháp). Bản đồng ý Đã rút lại MUST NOT được kích hoạt lại; muốn đồng ý lại thì ghi nhận bản mới.
- **FR-014a**: Lệnh "Rút lại đồng ý" MUST được thực hiện được cả khi hồ sơ ở trạng thái cuối (Kết thúc lưu trú, Qua đời, Hủy tiếp nhận), với cùng điều kiện ở FR-014; đây là một trong hai ngoại lệ của BR-M01-05 cho thao tác của người dùng (cùng đính chính có duyệt, FR-049a). Ghi nhận bản đồng ý mới MUST bị chặn trên hồ sơ trạng thái cuối. *(Nguồn: 5.1 căn cứ bảo vệ dữ liệu cá nhân, BR-M01-05; Clarification 2026-09-25, Q-03)*
- **FR-015**: Ngay khi bản đồng ý chuyển Đã rút lại (kể cả do được thay thế), mọi người thân không còn thuộc phạm vi của một bản đồng ý Hiệu lực MUST mất quyền xem thông tin sức khỏe từ lần truy cập kế tiếp. *(Nguồn: BR-M01-08, 14.1)*
- **FR-016**: Bản đồng ý MUST NOT bị sửa nội dung hay xóa. *(Nguồn: 1.5 nhóm 2)*

#### C. Hồ sơ sức khỏe ban đầu và mục sức khỏe

- **FR-017**: Hệ thống MUST cho phép bác sĩ hoặc điều dưỡng ghi hồ sơ sức khỏe ban đầu gồm: nhóm máu nếu có; khả năng vận động; khả năng tự chăm sóc; khả năng nhận thức; nhu cầu chăm sóc; nhu cầu dinh dưỡng; thuốc đang sử dụng khi tiếp nhận; nhu cầu phục hồi chức năng; thông tin chăm sóc cuối đời nếu có. Hồ sơ sức khỏe ban đầu sửa được khi còn Nháp; sau khi bác sĩ xác nhận, chỉ sửa bằng đính chính (feature 000). Thuốc dùng trong thời gian lưu trú thuộc feature 006. *(Nguồn: 5.2)*
- **FR-018**: Dị ứng, bệnh nền và tiền sử bệnh MUST được lưu thành từng mục riêng, mỗi mục gồm: loại (dị ứng / bệnh nền / tiền sử bệnh); nội dung; mức độ (bắt buộc với dị ứng); nguồn thông tin; người ghi nhận; ngày ghi nhận; trạng thái Hiệu lực / Đã loại trừ; lý do loại trừ. *(Nguồn: 5.2)*
- **FR-018a**: Nội dung của mục dị ứng MUST được chọn từ danh mục dị nguyên (hoạt chất hoặc nhóm thuốc, thành phần thực phẩm, dị nguyên khác), dùng chung với danh mục thuốc (feature 006) và thành phần món ăn (feature 011). Khi dị nguyên chưa có trong danh mục, người ghi MAY ghi loại "khác" bằng văn bản; mục đó MUST được đánh dấu "không kiểm tra tự động", hệ thống MUST cảnh báo người ghi ngay khi lưu rằng xung đột với thuốc và chế độ ăn sẽ không được phát hiện tự động, và dấu hiệu này MUST hiển thị cùng mục ở mọi nơi mục được xem (hồ sơ, thẻ thông tin khẩn cấp). *(Nguồn: BR-M01-07, BR-M07-06, BR-M08-02; Clarification 2026-09-25)*
- **FR-019**: Chỉ bác sĩ và điều dưỡng MUST được ghi nhận hoặc loại trừ mục sức khỏe; lệnh "Loại trừ" MUST có lý do; mục MUST NOT bị xóa hay sửa nội dung bởi bất kỳ ai. Sai nội dung được xử lý bằng loại trừ mục sai và ghi mục mới. *(Nguồn: BR-M01-07, DBR-04, UC-04, 4.4: BS T, ĐD T)*
- **FR-020**: Khi một mục dị ứng mới được ghi nhận, hệ thống MUST kích hoạt kiểm tra lại toàn bộ đơn thuốc đang hiệu lực (BR-M07-06, feature 006) và chế độ ăn, thực đơn đang phân bổ (BR-M08-02, feature 011) của người cao tuổi đó; mỗi xung đột tạo cảnh báo (feature 007). Việc kiểm tra MUST so khớp theo mục danh mục dị nguyên; mục loại "khác" không tham gia kiểm tra tự động (FR-018a). Khi một mục dị ứng chuyển Đã loại trừ, hệ thống MUST báo feature 011 để tính lại suất đặc biệt, đối chiếu lại đồ ăn gia đình và báo feature 007 nguồn của cảnh báo xung đột tương ứng đã được xử lý (feature 011 FR-021). Mục dị ứng loại "khác" mới được ghi MUST được báo cho feature 011 để đưa người đó vào danh sách cần đối chiếu khi phục vụ (feature 011 FR-036a). *(Nguồn: BR-M01-07; đồng bộ spec 011)*
- **FR-021**: Khi ghi một mục dị ứng cùng mục danh mục dị nguyên, hoặc một mục (bệnh nền, tiền sử, dị ứng "khác") có loại và nội dung trùng một mục đang Hiệu lực, hệ thống MUST cảnh báo trùng và yêu cầu xác nhận.
- **FR-022**: Người thân MUST chỉ xem được dị ứng, bệnh nền và thông tin sức khỏe khác khi có bản đồng ý Hiệu lực có người thân đó trong phạm vi; khi xem, chỉ thấy mục đang Hiệu lực. *(Nguồn: BR-M01-08, DBR-03, 4.4 chú thích ¹)*
- **FR-023**: Các mục dị ứng Hiệu lực, bệnh nền Hiệu lực và thông tin chăm sóc cuối đời MUST sẵn có cho thẻ thông tin khẩn cấp (9.5, feature 007).

#### D. Đánh giá đầu vào, đánh giá lại và quy đổi

- **FR-024**: Hệ thống MUST quản lý danh mục thang điểm cấu hình được; khuyến nghị mặc định: Barthel (tự chăm sóc), MMSE (nhận thức), Morse (nguy cơ ngã), Braden (nguy cơ loét tì đè). Mỗi thang có các mục chấm, khoảng điểm và cờ "bắt buộc trong mọi lần đánh giá". *(Nguồn: 5.3)*
- **FR-025**: Mỗi lần đánh giá MUST ghi: loại (đầu vào / đánh giá lại), lý do hoặc căn cứ, thời điểm, người thực hiện, điểm từng mục, tổng điểm và phân loại từng thang, mức chăm sóc đề xuất, cờ nguy cơ đề xuất, hoạt động mẫu đề xuất, mức chăm sóc được chấp nhận, lý do nếu khác đề xuất, người chấp nhận. *(Nguồn: 5.3, 3.2 DANH_GIA, KET_QUA_THANG_DIEM)*
- **FR-026**: Bác sĩ và điều dưỡng MUST được tạo lần đánh giá và nhập điểm (Nháp); chỉ bác sĩ MUST được chấp nhận hoặc điều chỉnh kết quả. *(Nguồn: 5.3, UC-05, UC-06, 4.4: BS T, D; ĐD T)*
- **FR-027**: Hệ thống MUST tự tính tổng điểm từng thang từ điểm các mục và áp bảng quy đổi CFG-M01-05 (mặc định theo bảng 5.3; giá trị ngưỡng do bác sĩ cơ sở chốt ở Q-02) để đề xuất: mức chăm sóc, cờ nguy cơ (ngã, loét, đi lạc) và hoạt động chăm sóc mẫu. Với dòng Barthel 21–60 ("thường xuyên trở lên"), đề xuất mặc định là "Chăm sóc thường xuyên". Khi nhiều thang cùng đề xuất mức chăm sóc, lấy mức cao nhất. *(Nguồn: 5.3, BR-M01-09, CFG-M01-05)*
- **FR-028**: Bác sĩ MUST NOT chấp nhận lần đánh giá khi còn thiếu điểm của bất kỳ thang bắt buộc nào.
- **FR-029**: Kết quả quy đổi MUST chỉ là đề xuất. Bác sĩ chấp nhận hoặc chọn mức chăm sóc khác; khi mức được chấp nhận khác mức đề xuất, lý do MUST bắt buộc. *(Nguồn: BR-M01-09, DBR-05)*
- **FR-030**: Khi bác sĩ chấp nhận, mọi cờ nguy cơ đề xuất MUST được gắn cho người cao tuổi; bác sĩ MUST NOT bỏ cờ đề xuất. Bác sĩ MAY gắn thêm cờ nguy cơ không có trong đề xuất, mỗi cờ gắn thêm MUST có lý do; cờ lưu nguồn gắn "theo quy đổi" hoặc "bác sĩ gắn thêm". Cờ đã đang gắn từ trước không bị ảnh hưởng bởi việc chấp nhận, trừ trường hợp gỡ theo FR-037. *(Nguồn: BR-M01-09, BR-M01-10; Clarification 2026-09-25, đề xuất Q-13)*
- **FR-031**: Khi bác sĩ chấp nhận, các hoạt động mẫu đề xuất MUST được chép vào bản nháp phiên bản kế hoạch chăm sóc mới của người cao tuổi (8.1, feature 005). *(Nguồn: BR-M01-09)*
- **FR-032**: Lần đánh giá sửa được khi còn Nháp; sau khi bác sĩ chấp nhận, lần đánh giá và kết quả thang điểm là ghi nhận đã xác nhận, chỉ sửa bằng đính chính (feature 000). Đính chính MUST NOT tự thay đổi mức chăm sóc hay cờ nguy cơ đã áp dụng; nếu quy đổi theo nội dung đã đính chính khác kết quả đã chấp nhận, hệ thống MUST tạo yêu cầu đánh giá lại với căn cứ "đính chính đánh giá". *(Nguồn: 1.5 nhóm 3, 3.2 DANH_GIA nhóm 3)*
- **FR-033**: Với đánh giá đầu vào, khi bác sĩ chấp nhận, mức chăm sóc được chấp nhận MUST trở thành mức chăm sóc hiện hành của người cao tuổi. Đánh giá đầu vào cũng MUST cho phép bác sĩ ghi loại hình lưu trú đề xuất; loại hình lưu trú chính thức chỉ được xác định bởi hợp đồng (6.3, feature 004). *(Nguồn: 5.3)*
- **FR-034**: Người cao tuổi MUST có tối đa một lần đánh giá đầu vào Đã xác nhận. *(Suy ra từ 5.3; đánh giá tiếp theo là đánh giá lại)*
- **FR-034a**: Kết quả đánh giá còn hiệu lực cho việc Hoàn tất tiếp nhận MUST là lần đánh giá Đã xác nhận gần nhất (đánh giá đầu vào hoặc đánh giá lại) có thời điểm chấp nhận cách thời điểm thực hiện lệnh không quá CFG-M01-02 (mặc định \[90 ngày\]). Nếu quá hạn, lệnh "Hoàn tất tiếp nhận" MUST bị từ chối với điều kiện chưa thỏa "đánh giá đã quá hạn, cần đánh giá lại". *(Nguồn: 5.6 "đánh giá đầu vào hoàn tất", BR-M01-02, CFG-M01-02; Clarification 2026-09-25)*
- **FR-035**: Đánh giá lại MUST dùng cùng danh mục thang điểm, cùng quy đổi và cùng quy tắc chấp nhận như đánh giá đầu vào (FR-025 → FR-032), và chỉ thực hiện được khi người cao tuổi ở trạng thái Đang lưu trú, Tạm vắng, Hoạt động bên ngoài, hoặc Đang tiếp nhận khi đã có đánh giá đầu vào Đã xác nhận. Với đánh giá lại ở trạng thái Đang tiếp nhận, mức chăm sóc được chấp nhận MUST thay mức chăm sóc hiện hành ngay như FR-033 (chưa có lưu trú nên không tạo yêu cầu thay đổi lưu trú); nếu đã có hợp đồng mang mức chăm sóc khác, việc điều chỉnh hợp đồng thuộc feature 004. *(Nguồn: 5.4, UC-06; Clarification 2026-09-25)*
- **FR-036**: Khi đánh giá lại được chấp nhận với mức chăm sóc khác mức hiện hành, hệ thống MUST tạo yêu cầu thay đổi lưu trú loại "đổi mức chăm sóc" ở trạng thái Chờ duyệt (vòng đời feature 000, loại yêu cầu thuộc feature 004); mức chăm sóc hiện hành và kế hoạch chăm sóc cũ MUST giữ nguyên tới ngày hiệu lực của thay đổi. *(Nguồn: BR-M01-03, 5.4)*
- **FR-036a**: Khi bác sĩ chấp nhận một lần đánh giá lại mà người cao tuổi đang có yêu cầu đổi mức chăm sóc ở Nháp, Chờ duyệt hoặc Đã duyệt (chờ hiệu lực), hệ thống MUST chuyển yêu cầu cũ sang trạng thái kết thúc "Được thay thế", trong cùng lần thực hiện với việc chấp nhận, ghi tham chiếu tới lần đánh giá mới và tới yêu cầu mới (nếu có), người thực hiện là hệ thống; yêu cầu Được thay thế MUST NOT tạo tác động nào. Sau đó: nếu mức mới khác mức hiện hành thì tạo yêu cầu mới theo FR-036; nếu mức mới trùng mức hiện hành thì không tạo yêu cầu mới. Tại mọi thời điểm, mỗi người cao tuổi MUST có tối đa một yêu cầu đổi mức chăm sóc chưa kết thúc. Người yêu cầu và người duyệt của yêu cầu cũ MUST được báo (feature 009). *(Nguồn: BR-M01-03; Clarification 2026-09-25; trạng thái "Được thay thế" là đề xuất bổ sung vào vòng đời chung của feature 000)*
- **FR-037**: Cờ nguy cơ MUST chỉ được gỡ qua một lần đánh giá lại Đã xác nhận: cờ gắn theo quy đổi được gỡ khi thang tương ứng (ngã – Morse, loét – Braden, đi lạc – MMSE, theo CFG-M01-05) cho kết quả không còn nguy cơ; cờ do bác sĩ gắn thêm được gỡ khi bác sĩ xác nhận không còn nguy cơ kèm lý do và thang tương ứng, nếu được chấm trong lần đó, cũng không cho kết quả nguy cơ. Cờ lưu lần đánh giá gắn và lần đánh giá gỡ. Không có thao tác gỡ cờ trực tiếp. *(Nguồn: BR-M01-10, 3.2 CO_NGUY_CO)*
- **FR-038**: Cờ nguy cơ đang gắn MUST sẵn có cho thẻ người cao tuổi trong checklist ca (feature 005) và thẻ thông tin khẩn cấp (feature 007); cờ đi lạc MUST kèm yêu cầu "bắt buộc người đi kèm khi rời khu vực". Cờ đi lạc hiện hành và sự kiện gắn, gỡ cờ đi lạc MUST được cung cấp cho feature 013 để chặn giao tiền mặt, trang sức cho người cao tuổi tự giữ và báo thu lại (feature 013 FR-015a, BR-M12-06; đồng bộ spec 013). *(Nguồn: BR-M01-10, 5.3)*

#### E. Yêu cầu đánh giá lại

- **FR-039**: Bộ lập lịch hệ thống MUST tạo yêu cầu đánh giá lại cho người cao tuổi Đang lưu trú, Tạm vắng, Hoạt động bên ngoài hoặc Điều trị tại bệnh viện khi lần đánh giá Đã xác nhận gần nhất đã qua CFG-M01-02 (mặc định \[90 ngày\]). *(Nguồn: BR-M01-02, UC-07)*
- **FR-040**: Hệ thống MUST tạo yêu cầu đánh giá lại khi: feature 007 ghi nhận sự cố ngã hoặc sự cố mức trung bình trở lên; lệnh "Ghi nhận trở về" từ Điều trị tại bệnh viện được thực hiện; FR-032 phát hiện kết quả sau đính chính khác. Bác sĩ và điều dưỡng MUST tạo được yêu cầu thủ công với căn cứ "tình trạng thay đổi đáng kể" hoặc "yêu cầu chuyên môn". *(Nguồn: BR-M01-02, 5.4, 5.6)*
- **FR-041**: Mỗi yêu cầu MUST có hạn hoàn thành CFG-M01-03 (mặc định \[48 giờ\]) tính từ lúc tạo; riêng người cao tuổi đang Điều trị tại bệnh viện, hạn tính từ lúc Ghi nhận trở về. Quá hạn mà chưa có đánh giá lại Đã xác nhận, hệ thống MUST cảnh báo bác sĩ; yêu cầu MUST NOT tự đóng. *(Nguồn: BR-M01-02)*
- **FR-042**: Mỗi người cao tuổi MUST có tối đa một yêu cầu đánh giá lại đang mở; căn cứ mới phát sinh MUST được bổ sung vào yêu cầu đang mở, hạn hoàn thành lấy hạn sớm hơn.
- **FR-043**: Khi đánh giá lại được bác sĩ chấp nhận, yêu cầu đang mở MUST được đóng với tham chiếu lần đánh giá, và feature 005 MUST được kích hoạt tạo yêu cầu xem xét kế hoạch chăm sóc (BR-M04-20). Yêu cầu đang mở MUST bị hệ thống đóng với lý do tương ứng khi người cao tuổi chuyển sang trạng thái cuối.

#### F. Trạng thái người cao tuổi và lệnh nghiệp vụ

- **FR-044**: Hệ thống MUST chỉ cho phép các chuyển trạng thái trong bảng dưới đây; MUST NOT có thao tác "sửa trạng thái". *(Nguồn: 5.5, BR-M01-04, BR-M01-06)*
- **FR-045**: Mỗi lệnh MUST kiểm tra toàn bộ điều kiện chặn trước khi chuyển; nếu có điều kiện chưa thỏa, hệ thống MUST từ chối, liệt kê mọi điều kiện chưa thỏa trong một lần, và giữ nguyên trạng thái. *(Nguồn: 5.6, BR-M01-06)*
- **FR-046**: Mỗi lần chuyển trạng thái MUST ghi lịch sử: trạng thái từ, trạng thái đến, lệnh, thời điểm, người thực hiện (hoặc Bộ lập lịch hệ thống), lý do, căn cứ (ví dụ sự cố, chuyến đi, yêu cầu ngoại lệ). Lý do MUST bắt buộc với mọi lệnh do người dùng thực hiện. *(Nguồn: BR-M01-04, 3.2 LICH_SU_TRANG_THAI)*
- **FR-047**: Khi chuyển trạng thái thành công, hệ thống MUST kích hoạt các tác động ở cột "Tác động" cho feature sở hữu, trong cùng một lần thực hiện với việc chuyển trạng thái: hoặc chuyển trạng thái và kích hoạt tác động cùng thành công, hoặc không có gì thay đổi. *(Nguồn: 5.6; nguyên tắc "hoặc toàn bộ, hoặc không" của feature 000)*
- **FR-047a**: "Cho tạm vắng" và "Ghi nhận trở về" MUST được hành chính hoặc trưởng tầng thực hiện trực tiếp, không qua yêu cầu phê duyệt. Quyền duyệt (D) của Quản lý viện ở dòng "Tạm vắng, trở về" của Permission Matrix MUST chỉ áp dụng cho quyết định giữ tiếp hay giải phóng giường khi thời gian vắng vượt ngưỡng giữ giường (BR-M02-07, feature 004). *(Nguồn: 5.6, 4.4; Clarification 2026-09-25)*
- **FR-047b**: Lệnh "Ghi nhận qua đời" MUST do bác sĩ thực hiện khi người cao tuổi đang ở trạng thái Đang lưu trú (mất tại viện). Khi trạng thái hiện tại là Tạm vắng, Hoạt động bên ngoài hoặc Điều trị tại bệnh viện (mất ngoài viện), bác sĩ hoặc hành chính MAY thực hiện; nếu hành chính thực hiện, lệnh MUST kèm bản scan giấy tờ bằng chứng (giấy báo tử hoặc giấy tờ của cơ sở y tế), thiếu bằng chứng thì bị từ chối. *(Nguồn: 5.6 "có người xác nhận", 6.8, UC-18, 4.4 dòng "Kết thúc lưu trú, qua đời": BS T, HC T; Clarification 2026-09-25)*
- **FR-048**: Khi hai lệnh được gửi gần như đồng thời trên cùng một người cao tuổi, chỉ lệnh đầu tiên MUST được áp dụng; lệnh sau MUST được kiểm tra lại trên trạng thái mới.
- **FR-049**: Hồ sơ ở trạng thái cuối (Kết thúc lưu trú, Qua đời, Hủy tiếp nhận) MUST chỉ được xem theo quyền; mọi lệnh và mọi thao tác ghi trên hồ sơ, bản đồng ý, mục sức khỏe, đánh giá, cờ nguy cơ của người đó MUST bị chặn, trừ hai ngoại lệ do người dùng thực hiện — lệnh "Rút lại đồng ý" (FR-014a) và đính chính theo FR-049a — và các thao tác của Bộ lập lịch hệ thống được quy định ở feature 000 (lưu giữ) và 5.6 (khóa tài khoản người thân). *(Nguồn: BR-M01-05, 5.5)*
- **FR-049a**: Bản ghi đã xác nhận thuộc hồ sơ ở trạng thái cuối (lần đánh giá, kết quả thang điểm, hồ sơ sức khỏe ban đầu, lịch sử trạng thái) MUST chỉ được đính chính dưới dạng yêu cầu phê duyệt loại "Đính chính" (vòng đời feature 000), do người được đính chính theo feature 000 FR-026 lập; bản đính chính MUST chỉ được lưu và có hiệu lực khi Quản lý viện duyệt, kể cả với bản ghi gắn tầng mà khi hồ sơ còn hoạt động được đính chính trực tiếp. Đính chính trên hồ sơ trạng thái cuối MUST NOT kích hoạt yêu cầu đánh giá lại (FR-032) hay bất kỳ tác động nào lên trạng thái, mức chăm sóc, cờ nguy cơ. *(Nguồn: BR-M01-05, 1.3, feature 000 FR-023 → FR-026; Clarification 2026-09-25)*
- **FR-050**: Khi người cao tuổi Tạm vắng quá thời điểm dự kiến trở lại cộng CFG-M01-01 (mặc định \[2 giờ\]) mà chưa có "Ghi nhận trở về", hệ thống MUST cảnh báo trưởng tầng và hành chính một lần cho mỗi mốc dự kiến trở lại. *(Nguồn: BR-M01-01)*

**Bảng trạng thái người cao tuổi** *(5.5, 5.6, BR-M01-06; quyền theo 4.4)*:

| Trạng thái hiện tại | Lệnh | Trạng thái kế tiếp | Ai thực hiện | Điều kiện (chặn nếu không đạt) | Tác động (feature thực hiện) |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Tạo hồ sơ | Đang tiếp nhận | Hành chính | FR-001, FR-003 | — |
| Đang tiếp nhận | Hoàn tất tiếp nhận | Đang lưu trú | Hành chính | Đánh giá đầu vào Đã xác nhận và kết quả đánh giá gần nhất chưa quá CFG-M01-02 (001, FR-034a); hợp đồng Hiệu lực và đặt cọc đạt (004); nếu nội trú: đã có phân bổ giường (003); đủ DBR-02 — đúng một người liên hệ chính và ít nhất một người đại diện (012 FR-003). Bản đồng ý không chặn (FR-012) | Sinh lịch cá nhân, lịch thuốc, suất ăn từ ngày hiệu lực (005, 006, 011); nếu chưa có bản đồng ý Hiệu lực: cảnh báo và bắt đầu nhắc theo CFG-M01-06 (001) |
| Đang tiếp nhận | Hủy tiếp nhận | Hủy tiếp nhận | Hành chính | Có lý do | Hủy giữ chỗ giường nếu có (003); đóng hồ sơ chờ liên quan (004) |
| Đang lưu trú | Cho tạm vắng | Tạm vắng | Hành chính, Trưởng tầng | Có bản ghi đón hợp lệ: người đón thuộc danh sách được phép đón hoặc có ngoại lệ đón Hiệu lực (14.3, BR-M10-03, 012 FR-026) | Hủy công việc, suất ăn trong thời gian vắng (005, 011); liều chuyển "Mang theo" hoặc "Tạm dừng" (006); giường Giữ chỗ hoặc Trống theo chính sách (003); áp chính sách phí vắng BR-M02-06 (004, 010) |
| Đang lưu trú | Điểm danh rời viện (chuyến đi) | Hoạt động bên ngoài | Hệ thống, khi trưởng đoàn (hoặc trưởng tầng thay) điểm danh rời viện (014 FR-040) | Người cao tuổi có trong danh sách chuyến đi, được đánh giá Đạt, đang Đang lưu trú (8.9) | Hủy công việc Chưa đến hạn trong khoảng đi (005 FR-020, BR-M04-04); liều chuyển "Mang theo" (006) (BR-M04-16) |
| Đang lưu trú, Tạm vắng, Hoạt động bên ngoài | Chuyển viện | Điều trị tại bệnh viện | Điều dưỡng, Bác sĩ | Có sự cố hoặc chỉ định chuyển viện (007) | Như Cho tạm vắng; thông báo người liên hệ chính mức Khẩn cấp (009, Q-97) |
| Tạm vắng | Ghi nhận trở về | Đang lưu trú | Hành chính, Trưởng tầng | — | Khôi phục sinh công việc, liều, suất ăn từ thời điểm trở về (005, 006, 011) |
| Hoạt động bên ngoài | Điểm danh về (chuyến đi), hoặc Hủy ghi nhận điểm danh rời viện (014 FR-031a) | Đang lưu trú | Hệ thống, khi trưởng đoàn hoặc trưởng tầng điểm danh về, hoặc trưởng tầng đính chính (014) | Người cao tuổi được điểm danh về, hoặc bản ghi rời viện bị hủy ghi nhận khi chuyến chưa về | Sinh lại công việc từ lúc trở về (005 FR-020); tính lại liều Mang theo (006) |
| Điều trị tại bệnh viện | Ghi nhận trở về | Đang lưu trú | Hành chính, Trưởng tầng | — | Tạm dừng toàn bộ lịch thuốc cũ, tạo yêu cầu đối chiếu thuốc BR-M07-09 (006); tạo yêu cầu đánh giá lại (001, FR-040) |
| Đang lưu trú, Tạm vắng, Điều trị tại bệnh viện | Kết thúc lưu trú | Kết thúc lưu trú | Hành chính, Bác sĩ; Quản lý viện duyệt ngoại lệ | Không còn đồ gửi ở Đang giữ, Đang được sử dụng hoặc Hư hỏng (013, Q-148); không còn thuốc gửi đang giữ (006); chi phí đã chốt (010); không còn cảnh báo/sự cố mở (007) — trừ khi có yêu cầu ngoại lệ đã được Quản lý viện duyệt (vòng đời 000) | Hủy mọi lịch tương lai (005, 006, 011); giải phóng giường (003); khóa tài khoản người thân sau CFG-M01-04, mặc định \[30 ngày\] (012) |
| Đang lưu trú, Tạm vắng, Hoạt động bên ngoài, Điều trị tại bệnh viện | Ghi nhận qua đời | Qua đời | Bác sĩ; Hành chính chỉ khi trạng thái hiện tại là Tạm vắng, Hoạt động bên ngoài hoặc Điều trị tại bệnh viện (FR-047b) | Có người xác nhận: bác sĩ thực hiện lệnh, hoặc giấy tờ bằng chứng (giấy báo tử, giấy tờ của cơ sở y tế) khi hành chính thực hiện | Như Kết thúc lưu trú; thông báo người liên hệ chính mức Khẩn cấp (009, Q-97); hồ sơ chỉ đọc |

Kết thúc lưu trú, Qua đời, Hủy tiếp nhận là trạng thái cuối; không có lệnh nào đưa hồ sơ ra khỏi các trạng thái này. Nội dung chi tiết của từng lệnh (ví dụ thời gian dự kiến trở lại, người bàn giao, thông tin qua đời ở 6.8) thuộc feature 004; spec này quy định tập trạng thái, chuyển hợp lệ, điều kiện chặn và lịch sử.

#### G. Loại hình lưu trú và mức chăm sóc

- **FR-051**: Loại hình lưu trú và mức chăm sóc MUST là hai thuộc tính độc lập của người cao tuổi: mọi tổ hợp giữa ba loại hình lưu trú (3.1–3.3) và các mức chăm sóc trong danh mục (mục 4) MUST được chấp nhận; thay đổi thuộc tính này MUST NOT tự động thay đổi thuộc tính kia. *(Nguồn: 1.3, mục 4)*
- **FR-052**: Loại hình lưu trú hiện hành MUST lấy từ hợp đồng Hiệu lực (kể cả phụ lục đã áp dụng, BR-M02-09); mức chăm sóc hiện hành MUST lấy từ đánh giá đầu vào đã chấp nhận (FR-033), sau đó chỉ thay đổi khi một yêu cầu thay đổi lưu trú đổi mức chăm sóc được áp dụng (FR-036, feature 004). Không có thao tác sửa trực tiếp hai thuộc tính này. *(Nguồn: 5.3, 6.3, 6.6, BR-M01-03)*
- **FR-053**: Lịch sử của từng thuộc tính MUST cho biết giá trị hiện hành tại một ngày bất kỳ và căn cứ thay đổi (lần đánh giá, hợp đồng, yêu cầu thay đổi lưu trú).

### Key Entities *(include if feature involves data)*

- **Người cao tuổi (NGUOI_CAO_TUOI)** – nhóm 2: mã hồ sơ, hồ sơ trước (liên kết tới hồ sơ ở trạng thái cuối của cùng người, nếu có), họ tên, ngày sinh, giới tính, giấy tờ định danh, ảnh, địa chỉ, liên hệ, thông tin đặc biệt, trạng thái hiện tại, loại hình lưu trú hiện hành, mức chăm sóc hiện hành.
- **Lịch sử trạng thái (LICH_SU_TRANG_THAI)** – nhóm 3: trạng thái từ, đến, lệnh, thời điểm, người thực hiện, lý do, căn cứ.
- **Bản đồng ý (BAN_DONG_Y)** – nhóm 2: người đồng ý, là người đại diện hay không, lý do đại diện, phạm vi (người thân được xem sức khỏe, hình ảnh, thông báo), thời điểm, bằng chứng, trạng thái, bản thay thế.
- **Hồ sơ sức khỏe ban đầu** – nhóm 3 sau xác nhận: các thông tin ở FR-017, người ghi, người xác nhận.
- **Danh mục dị nguyên** – nhóm 1: tên, nhóm (thuốc/hoạt chất, thực phẩm, khác), liên kết tới hoạt chất (feature 006) hoặc thành phần món ăn (feature 011).
- **Mục sức khỏe (MUC_SUC_KHOE)** – nhóm 2: loại, nội dung (với dị ứng: mục danh mục dị nguyên, hoặc văn bản kèm dấu "không kiểm tra tự động"), mức độ, nguồn, người ghi, ngày ghi, trạng thái, lý do và người loại trừ.
- **Thang điểm** – nhóm 1: tên, mục chấm, khoảng điểm, bắt buộc hay không; bảng quy đổi nằm ở CFG-M01-05.
- **Lần đánh giá (DANH_GIA)** – nhóm 3 sau xác nhận: loại, căn cứ, thời điểm, người thực hiện, mức đề xuất, mức chấp nhận, lý do khác đề xuất, loại hình lưu trú đề xuất, người chấp nhận.
- **Kết quả thang điểm (KET_QUA_THANG_DIEM)** – nhóm 3: thang, điểm từng mục, tổng điểm, phân loại.
- **Cờ nguy cơ (CO_NGUY_CO)** – nhóm 2: loại (ngã, loét, đi lạc), nguồn gắn (theo quy đổi / bác sĩ gắn thêm), lý do gắn thêm, lần đánh giá gắn, lần đánh giá gỡ, lý do gỡ.
- **Yêu cầu đánh giá lại** – nhóm 2: người cao tuổi, danh sách căn cứ, thời điểm tạo, hạn hoàn thành, trạng thái (Đang mở / Đã hoàn thành / Đã đóng), lần đánh giá hoàn thành.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% lần chuyển trạng thái người cao tuổi trong đợt kiểm thử thuộc bảng 5.5 và có bản ghi lịch sử đủ trạng thái từ/đến, lệnh, người, thời điểm, lý do; 0 chuyển trạng thái ngoài bảng được chấp nhận.
- **SC-002**: Với mỗi lệnh có điều kiện chặn, 100% lần thực hiện khi thiếu một hoặc nhiều điều kiện bị từ chối và người dùng thấy đầy đủ danh sách điều kiện chưa thỏa ngay trong lần từ chối đầu tiên.
- **SC-003**: 0 mục dị ứng, bệnh nền, tiền sử bệnh bị mất hoặc đổi nội dung sau khi ghi, trong mọi kịch bản kiểm thử kể cả với tài khoản quản lý viện.
- **SC-004**: Với mọi bộ điểm kiểm thử phủ đủ các ngưỡng của CFG-M01-05, mức chăm sóc và cờ nguy cơ đề xuất khớp 100% với bảng quy đổi đang hiệu lực.
- **SC-005**: Bác sĩ hoàn tất một lần đánh giá đầu vào với 4 thang khuyến nghị (từ lúc mở đến lúc chấp nhận, không kể thời gian khám) trong không quá 10 phút.
- **SC-006**: Hành chính tạo xong một hồ sơ người cao tuổi đầy đủ thông tin trong không quá 5 phút, không cần sổ giấy.
- **SC-007**: 100% yêu cầu đánh giá lại được tạo đúng lúc cho các căn cứ ở BR-M01-02 trong đợt kiểm thử với đồng hồ giả lập; 100% yêu cầu quá hạn CFG-M01-03 có cảnh báo bác sĩ.
- **SC-008**: Sau khi bản đồng ý bị rút lại, 0 lần người thân ngoài phạm vi còn xem được thông tin sức khỏe ở lần truy cập kế tiếp.
- **SC-009**: 100% lần thêm dị ứng mới chọn từ danh mục kích hoạt kiểm tra đơn thuốc và chế độ ăn; mọi xung đột có sẵn trong dữ liệu kiểm thử đều sinh cảnh báo; 100% mục dị ứng loại "khác" mang dấu "không kiểm tra tự động" ở mọi nơi hiển thị.
- **SC-010**: Mọi tổ hợp loại hình lưu trú × mức chăm sóc trong danh mục đều ghi nhận được cho một người cao tuổi; đổi một thuộc tính không làm thay đổi thuộc tính còn lại trong 100% ca kiểm thử.

## Assumptions

- Số feature `001` theo bản đồ feature ở mục 4.2 `docs/phan-tich-yeu-cau.md` (UC-01 → UC-08) và trùng số tuần tự kế tiếp.
- Quy tắc dùng chung (nhóm dữ liệu, đính chính, yêu cầu phê duyệt, nhật ký, tham số) theo feature 000 và không lặp lại ở đây.
- Họ tên, ngày sinh, giới tính là trường bắt buộc tối thiểu của hồ sơ cá nhân; giới tính cần cho ràng buộc giới tính của phòng (BR-M02-01, BR-M03-01).
- Với dòng Barthel 21–60 ("mức chăm sóc thường xuyên trở lên"), hệ thống đề xuất mức thấp nhất thỏa điều kiện là "Chăm sóc thường xuyên"; bác sĩ nâng mức nếu cần (có lý do). Giá trị ngưỡng là tham số CFG-M01-05, Q-02 chỉ chốt giá trị nên không ảnh hưởng hành vi.
- Các mức chăm sóc dạng chuyên biệt ("Hỗ trợ vận động", "Phục hồi sau tai biến", "Phục hồi chức năng khác", "Theo dõi sức khỏe thường xuyên") không có trong bảng quy đổi mặc định; chúng chỉ được bác sĩ chọn thủ công khi điều chỉnh, kèm lý do.
- Mọi lần đánh giá (đầu vào và đánh giá lại) phải có đủ các thang được đánh dấu bắt buộc; đánh giá lại có thể chấm thêm thang không bắt buộc.
- Mỗi người cao tuổi có tối đa một bản đồng ý Hiệu lực; đổi phạm vi bằng bản mới thay thế (mục 5.1 chỉ có hai trạng thái Hiệu lực / Đã rút lại).
- Yêu cầu đánh giá lại có tối đa một yêu cầu mở mỗi người; căn cứ mới được gộp.
- Hồ sơ sức khỏe ban đầu được bác sĩ xác nhận; sau xác nhận là nhóm 3. Tình trạng thay đổi sau đó được phản ánh qua đánh giá lại và mục sức khỏe, không sửa hồ sơ ban đầu.
- Nội dung chi tiết của lệnh Ghi nhận qua đời (thời điểm, địa điểm, người phát hiện, nguyên nhân) thuộc feature 004.
- Ghi nhận trở về từ Tạm vắng không có điều kiện chặn (5.6 không nêu); việc khôi phục công việc, liều, suất ăn do feature sở hữu quy định.
- Hai chuyển do điểm danh chuyến đi (vào/ra Hoạt động bên ngoài) do hệ thống thực hiện khi trưởng đoàn điểm danh ở feature 014, người thực hiện ghi là người điểm danh.
- Việc ghi nhật ký lượt người thân xem hồ sơ sức khỏe (NFR-08) thuộc feature 012.

## Điểm cần báo lại về tài liệu nguồn

Ngày 2026-09-25, các điểm đã chốt đã được đưa vào `docs/nghiep-vu.md` và `docs/phan-tich-yeu-cau.md` theo yêu cầu. Danh sách dưới đây ghi nơi đã phản ánh và các điểm còn mở.

**Đã phản ánh vào tài liệu nguồn**

1. Hai chuyển trạng thái vào/ra Hoạt động bên ngoài do điểm danh chuyến đi kích hoạt; Cho tạm vắng / Ghi nhận trở về không qua duyệt: BR-M01-06.
2. Người từng lưu trú quay lại có hồ sơ mới liên kết hồ sơ cũ, CCCD duy nhất trong các hồ sơ chưa ở trạng thái cuối (Q-12): 5.1, DBR-01, mục 24.2.
3. Bản đồng ý không chặn tiếp nhận, nhắc theo CFG-M01-06: 5.1, dòng "Đang tiếp nhận → Đang lưu trú" của 5.6, Phụ lục 25.
4. Ngoại lệ của BR-M01-05 (rút lại đồng ý; đính chính có Quản lý viện duyệt): BR-M01-05.
5. Cờ nguy cơ khi chấp nhận đánh giá — được gắn thêm, không được bỏ (Q-13): BR-M01-09, mục 24.2.
6. Quyền D của Quản lý viện ở dòng "Tạm vắng, trở về" chỉ cho quyết định giữ/giải phóng giường: chú thích ⁸ của Permission Matrix 4.4.
7. Trạng thái kết thúc "Được thay thế" của yêu cầu phê duyệt: 6.6 và BR-M01-03.
8. Thời hạn hiệu lực của đánh giá khi Hoàn tất tiếp nhận theo CFG-M01-02: dòng "Đang tiếp nhận → Đang lưu trú" của 5.6; cột "Dùng tại" của CFG-M01-02.
9. Người xác nhận qua đời (bác sĩ khi mất tại viện; bác sĩ hoặc hành chính kèm bằng chứng khi mất ngoài viện): dòng "→ Qua đời" của 5.6.

**Còn mở**

1. **Bảng 5.3, dòng Barthel 21–60** ghi "mức chăm sóc thường xuyên trở lên" — không phải một mức cụ thể; spec giả định đề xuất "Chăm sóc thường xuyên" (đã ghi tạm vào cột Mặc định của Q-02, mục 24.1). Chờ bác sĩ của cơ sở chốt Q-02.
2. **Q-03** (căn cứ pháp lý và mẫu bản đồng ý) vẫn mở.
