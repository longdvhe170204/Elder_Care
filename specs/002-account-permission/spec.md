# Feature Specification: Tài khoản và phân quyền

**Feature Branch**: `002-account-permission`

**Created**: 2026-09-25

**Status**: Draft

**Input**: User description: "Quản lý tài khoản và phân quyền theo docs/nghiep-vu.md Module 15 (mục 19) và mục 2.3–2.4: tài khoản nhân viên và người thân; vai trò hệ thống; quyền theo thao tác (xem, tạo, sửa, xác nhận, duyệt, chốt, nghiệp vụ chuyên môn); phạm vi dữ liệu theo khu vực, tầng, phòng, nhóm người cao tuổi được phân công; quyền chuyên môn phụ thuộc giấy phép còn hiệu lực. Quyền hiệu lực = vai trò ∩ phạm vi được phân công ∩ điều kiện pháp lý."

## Clarifications

### Session 2026-09-25

- Q: Nhân viên được phân công theo ca có còn xem được người cao tuổi mình phụ trách sau khi ca kết thúc không? → A: Có, trong giờ ca cộng một khoảng trước và sau ca theo tham số mới CFG-M15-07 (mặc định 2 giờ, bằng CFG-M04-06); ngoài khoảng đó thì mất phạm vi (đề xuất Q-14).
- Q: Ngoài quyền Duyệt kế hoạch chăm sóc, Quản lý viện có được điều chỉnh quyền của vai trò/tài khoản khác với Permission Matrix 4.4 không? → A: Chỉ được thu hẹp so với ma trận (theo vai trò hoặc từng tài khoản); ngoại lệ duy nhất được thêm vượt ma trận là quyền Duyệt kế hoạch chăm sóc cho điều dưỡng (đề xuất Q-15).
- Q: Ai tạo, kích hoạt lại và cấp lại mật khẩu cho tài khoản người thân? → A: Nhân viên hành chính thực hiện; Quản lý viện cũng được thực hiện (đề xuất Q-16).
- Q: Nhân viên phát hiện sự cố của người cao tuổi ngoài phạm vi phân công có được ghi nhận sự cố/kích hoạt khẩn cấp không? → A: Được; hai thao tác này miễn kiểm tra phạm vi, người ghi chỉ thấy thông tin nhận dạng (họ tên, ảnh, phòng), không thấy thông tin sức khỏe.
- Q: Điều dưỡng giữ nhiệm vụ Người phụ trách ca có được xử lý việc quá hạn (UC-27) không? → A: Được; nhiệm vụ Người phụ trách ca mang quyền UC-27, chỉ trong tầng/khu vực và thời gian của ca; là ngoại lệ duy nhất của quy tắc "nhiệm vụ không tạo quyền". *(Đã được mở rộng bằng câu trả lời về đính chính bên dưới.)*
- Q: Quyền "T"/"D" trong Permission Matrix áp dụng toàn viện hay chỉ trong phạm vi phân công? → A: Theo vai trò: Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc, Nhân viên vệ sinh trong phạm vi phân công; Quản lý viện, Bác sĩ, Hành chính, Dinh dưỡng viên, Nhân viên bếp toàn viện (19.3).
- Q: Spec 000 (FR-026 b) cho Người phụ trách ca tạo bản đính chính trực tiếp; spec 002 có chấp nhận đây là quyền thứ hai phát sinh từ nhiệm vụ không? → A: Có; thêm FR-019b, giới hạn trong tầng/khu vực và khoảng thời gian của ca (FR-032), giữ nguyên spec 000.
- Q: Bản ghi nhận ngoại tuyến đồng bộ sau khi người ghi đã mất quyền có được chấp nhận không? → A: Kiểm tra ba lớp quyền theo thời điểm ghi trên thiết bị; hợp lệ thì chấp nhận; nếu lúc đồng bộ đã mất quyền thì gắn nhãn "đồng bộ sau khi mất quyền" và báo trưởng tầng.
- Q: Khi không xác định được quyền do dữ liệu nguồn thiếu hoặc lỗi, hệ thống từ chối hay cho phép? → A: Từ chối mặc định; riêng ghi nhận sự cố và kích hoạt khẩn cấp (FR-044a) vẫn được thực hiện; hệ thống báo Quản lý viện về lỗi dữ liệu.
- Q: Hành chính và Trưởng tầng được xem thông tin sức khỏe đến mức nào? → A: Hành chính chỉ xem mức chăm sóc và cờ nguy cơ (không dị ứng, bệnh nền, chỉ số, thuốc, đánh giá); Trưởng tầng xem đầy đủ hồ sơ sức khỏe trong phạm vi.
- Q: Bác sĩ và Hành chính (ký hiệu "P" ở dòng "Dashboard, báo cáo" nhưng thường không có phân công theo tầng) xem dashboard, báo cáo toàn viện hay theo phân công? → A: Toàn viện, vẫn áp giới hạn trường của FR-022 (Hành chính không thấy chỉ số sức khỏe).
- Q: Khi thiết bị ngoại tuyến quá lâu hoặc đồng hồ thiết bị sai lệch rõ, có còn tin thời điểm trên thiết bị để kiểm tra quyền không? → A: Tham số mới CFG-M15-08 (thời gian ngoại tuyến tối đa, mặc định 24 giờ); bản ghi vượt giới hạn hoặc có thời điểm thiết bị bất hợp lý không được tự động chấp nhận mà chuyển "chờ xem lại" cho trưởng tầng.
- Q: Tài khoản nhiều vai trò mà hai vai trò cùng đem lại một quyền với phạm vi khác nhau thì áp phạm vi nào? → A: Phạm vi rộng hơn (hợp các phạm vi), nhất quán với FR-017; muốn hẹp hơn thì Quản lý viện thu hẹp theo tài khoản (FR-025).
- Q: Trước khi cấp lại mật khẩu hoặc kích hoạt lại tài khoản người thân, xác minh danh tính thế nào; người thân có tự đặt lại mật khẩu được không? → A: Xác minh trực tiếp tại viện bằng giấy tờ tùy thân hoặc gọi lại số điện thoại đã đăng ký trong hồ sơ người thân; ghi cách xác minh; giai đoạn đầu không có tự đặt lại mật khẩu.
- Q: Tài khoản nhân viên bị Khóa tạm giữa ca thì ai được mở khóa sớm? → A: Ngoài Quản lý viện, Trưởng tầng hoặc Người phụ trách ca được mở khóa sớm cho nhân viên cùng tầng/khu vực và cùng ca, sau khi xác minh trực tiếp; bắt buộc lý do, ghi nhật ký.

### Cập nhật 2026-09-26 (làm rõ quyền của nhân viên bếp ở 19.3)

Tài liệu nguồn làm rõ ở 19.3: với suất đặc biệt trên phiếu bữa ăn (12.5), nhân viên bếp xem họ tên, phòng, chế độ ăn, món thay thế và kết cấu thức ăn; không xem dị ứng, bệnh lý hay thông tin sức khỏe khác. Spec cập nhật: User Story 1 kịch bản 6, FR-022.

### Cập nhật 2026-09-26 (đồng bộ với spec 008)

Spec 008 đã chốt khi clarify (Q-80): Người phụ trách ca được "Ghi nhận vắng ca" cho nhân viên trong ca mình phụ trách. Đây là ngoại lệ quyền theo nhiệm vụ thứ tư, thêm FR-019d và sửa FR-019 cho khớp. Khi tầng chưa có Trưởng tầng được giao, spec 008 (Q-90, thay phần làm thay của Q-86) không cho Quản lý viện làm thay các lệnh bàn giao (giữ đúng quyền "X" ở 4.4 và Q-15); các lệnh đó chuyển cho Người phụ trách ca đang diễn ra của tầng, trong phạm vi ca, như một phần của quyền "T" ở dòng "Bàn giao ca" mà vai trò Điều dưỡng/Trưởng tầng đã có. Theo spec 008 Q-92, đối tượng phân công chỉ gồm khu vực, tầng, phòng, người cao tuổi; FR-028 bỏ "nhóm người cao tuổi".

## Phạm vi

**Trong phạm vi**:

1. Tài khoản nhân viên và tài khoản người thân: tạo, liên kết, trạng thái, khóa/mở khóa (mục 19.1, UC-68).
2. Đăng nhập và bảo vệ tài khoản (UC-70, BR-M15-05).
3. Vai trò hệ thống (bảng 2.3) và quyền theo loại thao tác (mục 19.2), giới hạn bởi Permission Matrix 4.4 và mục 19.3.
4. Phạm vi dữ liệu theo khu vực, tầng, phòng, nhóm người cao tuổi, suy ra từ phân công (BR-M15-02).
5. Điều kiện pháp lý cho nghiệp vụ chuyên môn (BR-M06-05, mục 10.4, 13.4).
6. Quy tắc tính quyền hiệu lực áp dụng cho mọi lần xem và mọi thao tác (BR-M15-01).

**Ngoài phạm vi**:

- Hồ sơ nhân viên, giấy phép hành nghề, đào tạo và trạng thái làm việc (feature 008, UC-49). Spec này chỉ **dùng** trạng thái làm việc và hiệu lực giấy phép.
- Lập lịch ca và phân công (feature 008, UC-50, UC-51). Spec này chỉ **dùng** kết quả phân công để xác định phạm vi dữ liệu.
- Giấy phép hoạt động của cơ sở (mục 10.4, Module 06). Spec này chỉ **dùng** hiệu lực và phạm vi của giấy phép cơ sở.
- Hồ sơ người thân, quan hệ, quyền theo từng người thân và danh sách được phép đón (feature 012, UC-55, BR-M10-07); bản đồng ý chia sẻ dữ liệu (feature 001, UC-03). Spec này chỉ **dùng** các quyền đó và bản đồng ý khi tính quyền hiệu lực của người thân.
- Nhật ký lượt xem hồ sơ sức khỏe của người thân (NFR-08, feature 012).
- Nội dung, kênh gửi thông báo và cảnh báo (feature 009).
- Vòng đời nhật ký, tham số cấu hình, yêu cầu phê duyệt dùng chung (feature 000). Spec này kế thừa các quy tắc đó.
- Quyền cụ thể của từng nghiệp vụ (ai được lập đơn thuốc, ai được chốt kỳ…): do spec module sở hữu quy định theo Permission Matrix 4.4; spec này chỉ quy định cơ chế.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Mọi lần xem và thao tác đều bị giới hạn bởi quyền hiệu lực (Priority: P1)

Mỗi khi một người dùng xem dữ liệu hoặc thực hiện một thao tác, hệ thống tính quyền hiệu lực của họ bằng giao của ba lớp: quyền của vai trò, phạm vi dữ liệu được phân công và điều kiện pháp lý còn hiệu lực. Chỉ khi cả ba lớp cùng cho phép thì hệ thống mới hiển thị dữ liệu hoặc chấp nhận thao tác.

**Why this priority**: BR-M15-01 là nền của toàn bộ hệ thống; mọi module khác (001, 005, 006, 007…) đều dẫn chiếu quy tắc này. Thiếu nó, dữ liệu sức khỏe người cao tuổi lộ ra ngoài phạm vi chuyên môn, vi phạm nguyên tắc mục 1.3.

**Independent Test**: Chuẩn bị một viện có hai tầng, mỗi vai trò có một tài khoản được phân công vào tầng 1. Với mỗi dòng của Permission Matrix 4.4, thử xem và thực hiện thao tác trên một người cao tuổi ở tầng 1 và một người ở tầng 2; đối chiếu kết quả với ma trận và mục 19.3.

**Acceptance Scenarios**:

1. **Given** vai trò của người dùng không có quyền một thao tác theo Permission Matrix 4.4 (ô "—"), **When** người dùng thực hiện thao tác đó trên bất kỳ đối tượng nào, **Then** hệ thống từ chối, và thao tác không có trong danh sách thao tác mà người dùng nhìn thấy.
2. **Given** vai trò có quyền "P" với một chức năng và người dùng được phân công tầng 1, **When** xem dữ liệu của người cao tuổi ở tầng 1, **Then** được xem; **When** xem dữ liệu của người cao tuổi ở tầng 2, **Then** hệ thống từ chối và không tiết lộ nội dung.
3. **Given** vai trò có quyền "X" với một chức năng (ví dụ Quản lý viện với hồ sơ người cao tuổi), **When** xem dữ liệu ở bất kỳ tầng nào, **Then** được xem, không phụ thuộc phân công.
4. **Given** bác sĩ có vai trò và phạm vi hợp lệ nhưng giấy phép hành nghề đã hết hạn, **When** thực hiện nghiệp vụ chuyên môn cần giấy phép (ví dụ kê đơn nội bộ), **Then** hệ thống từ chối và nêu lý do là điều kiện pháp lý không còn hiệu lực; bác sĩ vẫn xem được dữ liệu theo quyền "X" của vai trò.
5. **Given** dinh dưỡng viên xem hồ sơ một người cao tuổi trong phạm vi, **When** mở phần sức khỏe, **Then** chỉ thấy dị ứng và bệnh lý liên quan chế độ ăn, không thấy phần sức khỏe khác (mục 19.3).
6. **Given** nhân viên bếp, **When** tìm cách xem hồ sơ sức khỏe của bất kỳ người cao tuổi nào, **Then** hệ thống từ chối; nhân viên bếp chỉ thấy số suất và yêu cầu đặc biệt theo chế độ ăn, và với suất đặc biệt trên phiếu bữa ăn thì thấy họ tên, phòng, chế độ ăn, món thay thế, kết cấu thức ăn, không thấy dị ứng hay bệnh lý (mục 19.3, 12.5).
7. **Given** danh sách người cao tuổi hoặc báo cáo tổng hợp, **When** người dùng có quyền "P" mở danh sách, **Then** danh sách chỉ gồm người cao tuổi trong phạm vi của họ, và số liệu tổng không tính người ngoài phạm vi.
8. **Given** người dùng đã mở một màn hình khi còn quyền, **When** quyền bị thu hồi rồi người dùng gửi thao tác, **Then** hệ thống kiểm tra lại tại thời điểm gửi và từ chối.
9. **Given** nhân viên bếp không có phân công nào bao gồm người cao tuổi A, **When** ghi nhận sự cố ngã cho A, **Then** hệ thống cho chọn A qua họ tên, ảnh, phòng/giường, không hiển thị thông tin sức khỏe của A, lưu sự cố và ghi nhật ký đánh dấu "ngoài phạm vi"; **When** sau đó nhân viên bếp mở hồ sơ A, **Then** hệ thống từ chối.
10. **Given** nhân viên chăm sóc (quyền "T" với "Checklist, ghi nhận công việc") được phân công tầng 1, **When** ghi nhận công việc cho người cao tuổi ở tầng 2, **Then** hệ thống từ chối; **Given** nhân viên hành chính (quyền "T" với "Tạm vắng, trở về"), **When** cho tạm vắng người cao tuổi ở bất kỳ tầng nào, **Then** thao tác được xét tiếp theo điều kiện nghiệp vụ, không bị chặn vì phạm vi (FR-034).
11. **Given** nhân viên hành chính mở hồ sơ người cao tuổi A ở bất kỳ tầng nào, **When** xem phần sức khỏe, **Then** chỉ thấy mức chăm sóc và cờ nguy cơ, không thấy dị ứng, bệnh nền, chỉ số, thuốc hay kết quả đánh giá; **Given** trưởng tầng được phân công tầng của A, **When** xem phần sức khỏe của A, **Then** thấy đầy đủ (FR-022).
12. **Given** nhân viên hành chính và bác sĩ không có phân công theo tầng, **When** mở dashboard, **Then** cả hai thấy số liệu toàn viện; bác sĩ thấy chỉ tiêu sức khỏe, nhân viên hành chính chỉ thấy chỉ tiêu không thuộc sức khỏe (số người, giường trống, tạm vắng, chi phí); **Given** trưởng tầng được phân công tầng 1, **When** mở dashboard, **Then** chỉ thấy số liệu tầng 1 (FR-035).

---

### User Story 2 - Quản lý viện cấp và thu hồi tài khoản nhân viên (Priority: P1)

Khi có nhân viên mới, quản lý viện tạo tài khoản liên kết với hồ sơ nhân viên và gán một hoặc nhiều vai trò hệ thống. Khi nhân viên nghỉ việc, tài khoản bị khóa ngay. Quản lý viện có thể khóa hoặc mở khóa tài khoản có lý do. Tài khoản không bao giờ bị xóa vì lịch sử còn tham chiếu người thực hiện.

**Why this priority**: Không có tài khoản thì không ai dùng được hệ thống; không khóa kịp thời thì người đã nghỉ việc vẫn truy cập được hồ sơ sức khỏe (mục 19.1, BR-M09-04).

**Independent Test**: Tạo tài khoản cho một hồ sơ nhân viên, gán vai trò Điều dưỡng, đăng nhập thử; sau đó chuyển nhân viên sang Nghỉ việc ở feature 008 và kiểm tra tài khoản bị khóa, phiên đang mở bị chấm dứt, lịch sử vẫn hiển thị đúng tên người thực hiện.

**Acceptance Scenarios**:

1. **Given** một hồ sơ nhân viên đang làm việc chưa có tài khoản, **When** quản lý viện tạo tài khoản, gán ít nhất một vai trò nhân viên (AC-01 → AC-09), **Then** tài khoản ở trạng thái Hoạt động, liên kết đúng một hồ sơ nhân viên, và nhật ký ghi lần tạo.
2. **Given** một hồ sơ nhân viên đã có tài khoản, **When** quản lý viện tạo thêm tài khoản thứ hai cho cùng hồ sơ, **Then** hệ thống từ chối.
3. **Given** quản lý viện tạo tài khoản, **When** không gán vai trò nào, hoặc gán vai trò Người thân cho tài khoản nhân viên, **Then** hệ thống từ chối.
4. **Given** một tài khoản nhân viên đang Hoạt động, **When** nhân viên chuyển trạng thái Nghỉ việc, **Then** tài khoản chuyển Đã khóa ngay, mọi phiên đăng nhập đang mở bị chấm dứt, và nhật ký ghi người thực hiện là Bộ lập lịch hệ thống kèm căn cứ BR-M09-04.
5. **Given** quản lý viện khóa một tài khoản, **When** không nhập lý do, **Then** hệ thống từ chối; khi có lý do, tài khoản chuyển Đã khóa và phiên đang mở bị chấm dứt.
6. **Given** tài khoản Đã khóa vì nhân viên Nghỉ việc, **When** quản lý viện mở khóa trong khi hồ sơ nhân viên vẫn ở Nghỉ việc, **Then** hệ thống từ chối.
7. **Given** một tài khoản đã từng thực hiện thao tác, **When** bất kỳ ai, kể cả quản lý viện, yêu cầu xóa tài khoản, **Then** hệ thống không có thao tác xóa; các bản ghi cũ vẫn hiển thị đúng người thực hiện.
8. **Given** quản lý viện gán thêm hoặc gỡ một vai trò, **When** lưu kèm lý do, **Then** quyền mới có hiệu lực ngay ở thao tác kế tiếp của người dùng đó và nhật ký ghi vai trò trước/sau (BR-M15-04).
9. **Given** viện chỉ còn một tài khoản Hoạt động có vai trò Quản lý viện, **When** tài khoản đó bị gỡ vai trò Quản lý viện hoặc bị khóa bởi thao tác người dùng, **Then** hệ thống từ chối.

---

### User Story 3 - Đăng nhập an toàn cho nhân viên và người thân (Priority: P1)

Nhân viên và người thân đăng nhập bằng tài khoản của mình. Hệ thống khóa tạm khi đăng nhập sai nhiều lần liên tiếp, chuyển tài khoản người thân lâu không dùng sang Không hoạt động, và chỉ cho đăng nhập khi tài khoản đang Hoạt động.

**Why this priority**: UC-70 là cửa vào duy nhất của mọi use case khác; BR-M15-05 bảo vệ dữ liệu sức khỏe khỏi dò mật khẩu.

**Independent Test**: Với một tài khoản nhân viên và một tài khoản người thân, thử đăng nhập đúng, sai liên tiếp tới ngưỡng, chờ hết thời gian khóa, và giả lập đồng hồ vượt CFG-M15-02 với tài khoản người thân.

**Acceptance Scenarios**:

1. **Given** tài khoản Hoạt động, **When** đăng nhập đúng, **Then** người dùng vào hệ thống, bộ đếm đăng nhập sai về 0, và thời điểm đăng nhập được lưu.
2. **Given** tài khoản Hoạt động, **When** đăng nhập sai liên tiếp đủ số lần theo CFG-M15-01 (mặc định \[5 lần\]), **Then** tài khoản chuyển Khóa tạm trong thời gian theo CFG-M15-01 (mặc định \[15 phút\]); mọi lần đăng nhập trong thời gian đó bị từ chối kể cả khi đúng mật khẩu.
3. **Given** tài khoản Khóa tạm, **When** hết thời gian khóa, **Then** tài khoản tự trở về Hoạt động với bộ đếm về 0.
4. **Given** tài khoản Khóa tạm, **When** quản lý viện mở khóa sớm kèm lý do, **Then** tài khoản trở về Hoạt động và nhật ký ghi lại.
5. **Given** tài khoản điều dưỡng D ở tầng 2 bị Khóa tạm giữa ca đêm, **When** Người phụ trách ca đêm tầng 2 mở khóa sớm kèm lý do sau khi gặp trực tiếp D, **Then** tài khoản trở về Hoạt động và nhật ký ghi căn cứ nhiệm vụ; **When** Người phụ trách ca tầng 1 cố mở khóa cho D, hoặc cố mở khóa một tài khoản người thân, **Then** hệ thống từ chối (FR-019c).
6. **Given** tài khoản người thân không đăng nhập quá CFG-M15-02 (mặc định \[180 ngày\]), **When** Bộ lập lịch kiểm tra, **Then** tài khoản chuyển Không hoạt động; lần đăng nhập sau đó bị từ chối với hướng dẫn liên hệ viện để kích hoạt lại.
7. **Given** tài khoản nhân viên không đăng nhập quá CFG-M15-02, **When** Bộ lập lịch kiểm tra, **Then** tài khoản không bị chuyển Không hoạt động (BR-M15-05 chỉ áp dụng cho người thân).
8. **Given** tài khoản Đã khóa hoặc Không hoạt động, **When** đăng nhập đúng mật khẩu, **Then** hệ thống từ chối; thông báo không tiết lộ tài khoản có tồn tại hay mật khẩu có đúng hay không ngoài thông tin cần thiết để liên hệ viện.
9. **Given** tài khoản vừa được tạo hoặc vừa được cấp lại mật khẩu, **When** người dùng đăng nhập lần đầu, **Then** hệ thống buộc đổi mật khẩu trước khi dùng chức năng khác.

---

### User Story 4 - Phạm vi dữ liệu tự cập nhật theo phân công (Priority: P2)

Phạm vi dữ liệu của nhân viên không được cấp tay mà suy ra từ phân công: khu vực, tầng, phòng, người cao tuổi hoặc nhóm người cao tuổi được giao. Khi phân công thay đổi (chuyển tầng, đổi ca), phạm vi thay đổi ngay mà quản lý viện không phải cấp lại quyền.

**Why this priority**: BR-M15-02 giúp phân quyền theo kịp vận hành hằng ngày (đổi ca, chuyển tầng, nhân viên hỗ trợ chung). Có thể chạy trước với phạm vi toàn viện cho vai trò "X", nên xếp sau P1.

**Independent Test**: Phân công một nhân viên chăm sóc cho người cao tuổi A ở tầng 1; kiểm tra thấy A, không thấy B ở tầng 2; đổi phân công sang tầng 2 và kiểm tra ngay thao tác kế tiếp thấy B, không thấy A; chuyển A sang phòng thuộc tầng 2 và kiểm tra phạm vi đi theo A.

**Acceptance Scenarios**:

1. **Given** nhân viên được phân công theo tầng, **When** xem danh sách người cao tuổi, **Then** thấy mọi người cao tuổi đang có phân bổ giường mở ở các phòng thuộc tầng đó.
2. **Given** nhân viên được phân công theo khu vực, **When** xem dữ liệu, **Then** phạm vi bao gồm mọi tầng, phòng và người cao tuổi thuộc khu vực đó.
3. **Given** nhân viên được phân công cho một nhóm người cao tuổi cụ thể, **When** xem dữ liệu, **Then** phạm vi chỉ gồm những người trong nhóm, không mở rộng ra cả phòng hay tầng.
4. **Given** phân công của nhân viên đổi từ tầng 1 sang tầng 2, **When** nhân viên thực hiện thao tác kế tiếp, **Then** phạm vi đã là tầng 2, không cần quản lý viện thao tác gì.
5. **Given** người cao tuổi A chuyển giường từ tầng 1 sang tầng 2, **When** lệnh chuyển giường hoàn tất, **Then** A rời khỏi phạm vi của nhân viên chỉ được phân công tầng 1 và vào phạm vi của nhân viên được phân công tầng 2 (mục 7.4).
6. **Given** nhân viên có nhiều phân công cùng hiệu lực (ví dụ phụ trách chính phòng 101, hỗ trợ chung tầng 2), **When** xem dữ liệu, **Then** phạm vi là hợp của các phân công.
7. **Given** nhân viên không có phân công nào đang hiệu lực, **When** mở chức năng có quyền "P", **Then** không thấy người cao tuổi nào.
8. **Given** nhân viên chăm sóc được phân công người cao tuổi A trong ca kết thúc lúc 18:00 và CFG-M15-07 là \[2 giờ\], **When** xem hoặc ghi nhận muộn cho A lúc 19:30, **Then** được chấp nhận; **When** xem A lúc 20:30 mà không có phân công khác bao gồm A, **Then** hệ thống từ chối.
9. **Given** ca bắt đầu lúc 07:00 và CFG-M15-07 là \[2 giờ\], **When** nhân viên xem người cao tuổi của ca đó lúc 05:30, **Then** được xem (chuẩn bị nhận ca); **When** xem lúc 04:30, **Then** hệ thống từ chối.
10. **Given** điều dưỡng N (vai trò không có quyền UC-27) được chỉ định Người phụ trách ca đêm tầng 2, **When** N phân lại một công việc quá hạn ở tầng 2 trong ca, **Then** được chấp nhận và nhật ký ghi căn cứ nhiệm vụ; **When** N xử lý việc quá hạn ở tầng 1, hoặc ở tầng 2 sau khi hết khoảng thời gian của ca theo FR-032, **Then** hệ thống từ chối.
11. **Given** điều dưỡng N là Người phụ trách ca đêm tầng 2, **When** N tạo bản đính chính kèm lý do cho một chỉ số do nhân viên chăm sóc khác ghi ở tầng 2 trong ca, **Then** được chấp nhận và nhật ký ghi căn cứ nhiệm vụ (FR-019b); **When** N đính chính một bàn giao đồ gửi (không gắn tầng), **Then** hệ thống từ chối.
12. **Given** nhân viên chăm sóc ghi nhận ngoại tuyến cho người cao tuổi A lúc 17:00 trong ca của mình, và tài khoản bị khóa lúc 18:30, **When** thiết bị đồng bộ lúc 19:00, **Then** bản ghi được chấp nhận, gắn nhãn "đồng bộ sau khi mất quyền" và trưởng tầng được báo; **Given** bản ghi ngoại tuyến có thời điểm trên thiết bị sau 18:30, **When** đồng bộ, **Then** hệ thống từ chối (FR-041a).
13. **Given** phân công của nhân viên trỏ tới một tầng đã ngừng hiệu lực nên không xác định được phạm vi, **When** nhân viên xem hồ sơ người cao tuổi hoặc ghi nhận công việc, **Then** hệ thống từ chối và Quản lý viện được báo lỗi dữ liệu; **When** nhân viên đó ghi nhận sự cố cho một người cao tuổi, **Then** được thực hiện theo FR-044a (FR-041b).
14. **Given** CFG-M15-08 là \[24 giờ\] và thiết bị kết nối lần cuối lúc 07:00 ngày D, **When** đồng bộ lúc 09:00 ngày D+1 các bản ghi ngoại tuyến, **Then** các bản ghi chuyển "chờ xem lại" và trưởng tầng được báo; **When** trưởng tầng từ chối một bản ghi mà không nhập lý do, **Then** hệ thống không cho từ chối; **Given** một bản ghi có thời điểm thiết bị muộn hơn thời điểm đồng bộ, **When** đồng bộ trong giới hạn, **Then** bản ghi đó cũng chuyển "chờ xem lại" (FR-041c).
15. **Given** tài khoản có hai vai trò Nhân viên hành chính và Điều dưỡng, được phân công điều dưỡng ở tầng 1, **When** ghi nhận tạm vắng cho người cao tuổi ở tầng 3, **Then** được chấp nhận vì vai trò Hành chính cho phạm vi toàn viện; **Given** Quản lý viện đã thu hẹp quyền đó của tài khoản xuống theo phân công, **When** thực hiện lại, **Then** hệ thống từ chối (FR-034).

---

### User Story 5 - Nghiệp vụ chuyên môn chỉ bật khi giấy phép còn hiệu lực (Priority: P2)

Một số thao tác là nghiệp vụ chuyên môn (chẩn đoán, kê đơn, phát thuốc…) và chỉ được thực hiện khi điều kiện pháp lý thỏa: cơ sở có phạm vi hoạt động phù hợp còn hiệu lực và người thực hiện có giấy phép/đào tạo còn hiệu lực đúng phạm vi. Khi một điều kiện hết hạn, quyền tự tắt; khi được gia hạn, quyền tự bật lại.

**Why this priority**: BR-M06-05 và mục 10.4 cấm mặc định "có bác sĩ = được khám chữa bệnh". Rủi ro pháp lý cao, nhưng chỉ áp dụng cho một tập thao tác hẹp nên xếp P2.

**Independent Test**: Cho một bác sĩ có giấy phép hết hạn vào ngày D; giả lập đồng hồ ngày D và D+1, thử kê đơn nội bộ; lặp lại với giấy phép cơ sở hết hạn trong khi giấy phép bác sĩ còn hạn; gia hạn và thử lại.

**Acceptance Scenarios**:

1. **Given** cơ sở có phạm vi khám chữa bệnh còn hiệu lực và bác sĩ có giấy phép còn hiệu lực đúng phạm vi, **When** bác sĩ kê đơn nội bộ, **Then** thao tác được chấp nhận.
2. **Given** giấy phép hành nghề của bác sĩ hết hạn vào ngày D, **When** bác sĩ kê đơn nội bộ trong ngày D, **Then** được chấp nhận; **When** kê đơn vào ngày D+1, **Then** hệ thống từ chối, nêu giấy phép đã hết hạn, và chỉ còn cho phép nhập đơn từ cơ sở bên ngoài nếu vai trò có quyền đó (Permission Matrix 4.4, chú thích ³).
3. **Given** giấy phép bác sĩ còn hiệu lực nhưng phạm vi hoạt động khám chữa bệnh của cơ sở đã hết hạn hoặc không có, **When** bác sĩ chẩn đoán hoặc kê đơn nội bộ, **Then** hệ thống từ chối (BR-M06-05).
4. **Given** giấy phép có phạm vi hành nghề không bao gồm một nghiệp vụ chuyên môn, **When** người hành nghề thực hiện nghiệp vụ đó, **Then** hệ thống từ chối dù giấy phép còn hạn.
5. **Given** điều dưỡng có giấy phép hết hạn, **When** xác nhận liều thuốc, **Then** hệ thống từ chối (mục 13.4); điều dưỡng vẫn thực hiện được các thao tác không yêu cầu giấy phép.
6. **Given** giấy phép hết hạn đã được cập nhật gia hạn ở hồ sơ nhân viên (feature 008), **When** người hành nghề thực hiện lại nghiệp vụ chuyên môn, **Then** được chấp nhận mà quản lý viện không cần cấp lại quyền.
7. **Given** giấy phép cơ sở hoặc giấy phép người hành nghề sẽ hết hạn trong vòng CFG-M06-02 (mặc định \[60 ngày\]), **When** Bộ lập lịch kiểm tra hằng ngày, **Then** quản lý viện nhận cảnh báo nêu giấy phép, người liên quan, ngày hết hạn và các nghiệp vụ chuyên môn sẽ bị tắt.
8. **Given** một bản ghi chuyên môn được xác nhận khi giấy phép còn hiệu lực, **When** giấy phép sau đó hết hạn, **Then** bản ghi đó vẫn hợp lệ và không bị ảnh hưởng.

---

### User Story 6 - Người thân chỉ xem được đúng những gì được cấp (Priority: P2)

Người thân đăng nhập cổng người thân và chỉ thấy người cao tuổi mà mình có quan hệ; trong đó chỉ thấy các loại thông tin được cấp riêng cho mình (xem sức khỏe, xem chi phí…), và thông tin sức khỏe chỉ khi có bản đồng ý còn hiệu lực bao gồm mình. Sau khi người cao tuổi kết thúc lưu trú, tài khoản người thân bị khóa sau một thời gian cấu hình.

**Why this priority**: 600 tài khoản người thân (NFR-01) là nhóm người dùng lớn nhất và nằm ngoài viện; lộ dữ liệu sức khỏe cho người không được đồng ý là rủi ro pháp lý (mục 5.1, BR-M01-08). Xếp P2 vì phụ thuộc feature 012 và 001.

**Independent Test**: Tạo hai người thân của cùng một người cao tuổi, chỉ một người nằm trong bản đồng ý; kiểm tra phần sức khỏe hiện với người này, ẩn với người kia; rút lại đồng ý và kiểm tra lại; kết thúc lưu trú và giả lập đồng hồ vượt CFG-M01-04.

**Acceptance Scenarios**:

1. **Given** người thân có quan hệ với người cao tuổi A, không có quan hệ với B, **When** đăng nhập cổng, **Then** chỉ thấy A.
2. **Given** người thân có quyền xem chi phí nhưng không có quyền xem sức khỏe, **When** xem thông tin của A, **Then** thấy chi phí, không thấy thông tin sức khỏe.
3. **Given** người thân được cấp quyền xem sức khỏe, **When** bản đồng ý chia sẻ dữ liệu bao gồm người đó bị rút lại, **Then** ở lần xem kế tiếp người thân không còn thấy thông tin sức khỏe (BR-M01-08, DBR-03).
4. **Given** người thân không phải người đại diện, **When** gửi yêu cầu thay đổi dịch vụ, **Then** hệ thống từ chối (BR-M10-01).
5. **Given** mọi người cao tuổi có quan hệ với một người thân đều đã Kết thúc lưu trú hoặc Qua đời, **When** đã qua CFG-M01-04 (mặc định \[30 ngày\]) kể từ thời điểm muộn nhất trong các thời điểm kết thúc đó, **Then** tài khoản người thân chuyển Đã khóa (mục 5.6).
6. **Given** người thân có quan hệ với hai người cao tuổi, một người đã Kết thúc lưu trú quá CFG-M01-04 và một người còn Đang lưu trú, **When** Bộ lập lịch kiểm tra, **Then** tài khoản vẫn Hoạt động, nhưng người thân không còn thấy người cao tuổi đã kết thúc quá hạn.
7. **Given** người thân có quan hệ với người cao tuổi đã Kết thúc lưu trú nhưng chưa quá CFG-M01-04, **When** đăng nhập, **Then** vẫn xem được thông tin được cấp (ví dụ chi phí đã chốt) ở chế độ chỉ đọc.
8. **Given** một hồ sơ người thân chưa có tài khoản và có quan hệ với người cao tuổi Đang lưu trú, **When** nhân viên hành chính tạo tài khoản, **Then** tài khoản ở Hoạt động với vai trò Người thân và buộc đổi mật khẩu lần đầu; **When** một điều dưỡng hoặc trưởng tầng cố tạo tài khoản đó, **Then** hệ thống từ chối.
9. **Given** tài khoản người thân Không hoạt động, **When** nhân viên hành chính kích hoạt lại kèm lý do và ghi cách xác minh "gọi lại số đã đăng ký", **Then** tài khoản về Hoạt động; **When** kích hoạt lại mà không ghi cách xác minh, hoặc xác minh bằng số điện thoại người yêu cầu vừa đọc, **Then** hệ thống từ chối (FR-052); **When** nhân viên hành chính cố gán thêm vai trò khác cho tài khoản người thân hoặc thao tác trên tài khoản nhân viên, **Then** hệ thống từ chối.

---

### User Story 7 - Quản lý viện điều chỉnh quyền của vai trò trong giới hạn cho phép (Priority: P3)

Quản lý viện xem được quyền của từng vai trò (theo Permission Matrix 4.4) và điều chỉnh trong giới hạn cho phép, ví dụ gán quyền Duyệt kế hoạch chăm sóc cho điều dưỡng có đủ chuyên môn (mục 2.4). Mọi thay đổi có lý do, có hiệu lực ngay và được ghi nhật ký trước/sau.

**Why this priority**: Hệ thống chạy được với ma trận mặc định; điều chỉnh chỉ cần khi viện có mô hình vận hành khác (mục 1.3 "hỗ trợ nhiều mô hình vận hành").

**Independent Test**: Gán quyền Duyệt kế hoạch chăm sóc cho một điều dưỡng; kiểm tra điều dưỡng đó duyệt được còn điều dưỡng khác không; thu hồi và kiểm tra lại; tra nhật ký.

**Acceptance Scenarios**:

1. **Given** quản lý viện mở cấu hình quyền, **When** chọn một vai trò, **Then** thấy danh sách chức năng, loại thao tác được phép (FR-020) và phạm vi (X hoặc P) của vai trò đó, khớp Permission Matrix 4.4.
2. **Given** quản lý viện gán quyền Duyệt kế hoạch chăm sóc cho điều dưỡng N kèm lý do, **When** lưu, **Then** N duyệt được kế hoạch chăm sóc trong phạm vi dữ liệu của N; điều dưỡng khác không có quyền này (Q-07).
3. **Given** vai trò Nhân viên chăm sóc không có quyền với "Đơn thuốc" theo Permission Matrix 4.4, **When** quản lý viện cố thêm quyền xem đơn thuốc cho vai trò đó hoặc cho một tài khoản nhân viên chăm sóc, **Then** hệ thống từ chối vì vượt quyền tối đa của vai trò.
4. **Given** vai trò Hành chính có quyền "T" với "Chi phí, khoản điều chỉnh", **When** quản lý viện bỏ quyền tạo khoản điều chỉnh của một tài khoản Hành chính cụ thể kèm lý do, **Then** thay đổi được lưu, tài khoản đó không tạo được khoản điều chỉnh từ thao tác kế tiếp, các tài khoản Hành chính khác không bị ảnh hưởng.
5. **Given** một quyền đã bị thu hẹp, **When** quản lý viện khôi phục quyền đó về mức của ma trận kèm lý do, **Then** quyền có hiệu lực lại và nhật ký ghi trước/sau.
6. **Given** một vai trò khác Quản lý viện, **When** người dùng cố thay đổi quyền của bất kỳ vai trò hay tài khoản nào, **Then** hệ thống từ chối (BR-M15-04).
7. **Given** một lần thay đổi quyền đã lưu, **When** quản lý viện tra nhật ký, **Then** thấy người thực hiện, thời điểm, vai trò/tài khoản bị ảnh hưởng, quyền trước, quyền sau và lý do.

---

### Edge Cases

- Nhân viên có nhiều vai trò (ví dụ Trưởng tầng kiêm Điều dưỡng): quyền của vai trò là hợp quyền các vai trò; phạm vi và điều kiện pháp lý vẫn áp dụng cho từng thao tác (FR-017). Hai vai trò cùng cho một quyền với phạm vi khác nhau (ví dụ Hành chính toàn viện, Điều dưỡng theo phân công) thì áp phạm vi rộng hơn, trừ khi quyền đã bị thu hẹp theo tài khoản (FR-034, FR-025).
- Nhiệm vụ (Người phụ trách ca, Bác sĩ trực, Điều dưỡng phụ trách, Trưởng đoàn) không phải vai trò: không tạo thêm quyền thao tác, chỉ xác định phạm vi và người nhận nhắc việc/thông báo (mục 2.4, FR-019); ngoại lệ: Người phụ trách ca được xử lý việc quá hạn (FR-019a), tạo bản đính chính (FR-019b) , mở khóa sớm tài khoản nhân viên bị Khóa tạm (FR-019c) và ghi nhận vắng ca (FR-019d, spec 008 Q-80) trong tầng/khu vực và thời gian của ca.
- Phân công thay đổi ngay khi người dùng đang mở một màn hình: dữ liệu đã hiển thị không bị thu hồi trên màn hình, nhưng mọi lần tải lại và mọi thao tác gửi đi được kiểm tra theo phạm vi mới (FR-041).
- Người cao tuổi Tạm vắng, Điều trị tại bệnh viện hoặc bán trú không có giường cố định: không có phân bổ giường đang mở thì chỉ thuộc phạm vi của nhân viên được phân công trực tiếp cho người đó hoặc nhóm chứa người đó, cùng các vai trò có quyền "X" (FR-030).
- Người cao tuổi ở trạng thái cuối (Kết thúc lưu trú, Qua đời, Hủy tiếp nhận): hồ sơ chỉ đọc (BR-M01-05); vẫn tra cứu được bởi vai trò có quyền "X" và theo FR-030 với vai trò có quyền "P".
- Giấy phép hết hạn giữa lúc người hành nghề đang soạn một thao tác chuyên môn: kiểm tra tại thời điểm gửi, không tại thời điểm mở (FR-041).
- Bản ghi ngoại tuyến ghi lúc còn quyền nhưng đồng bộ sau khi tài khoản bị khóa, hết ca hoặc giấy phép hết hạn: chấp nhận theo thời điểm trên thiết bị, gắn nhãn "đồng bộ sau khi mất quyền" và báo trưởng tầng; ghi lúc đã mất quyền thì từ chối (FR-041a).
- Thiết bị ngoại tuyến quá CFG-M15-08, hoặc thời điểm trên thiết bị muộn hơn lúc đồng bộ / sớm hơn lần kết nối gần nhất: bản ghi chuyển "chờ xem lại", trưởng tầng chấp nhận hoặc từ chối có lý do (FR-041c).
- Dữ liệu nguồn để tính quyền bị thiếu hoặc lỗi (ví dụ giấy phép không có ngày hết hạn, phân công trỏ tới tầng không tồn tại): từ chối mặc định, trừ sự cố/khẩn cấp; Quản lý viện được báo (FR-041b).
- Kế hoạch chăm sóc đang Chờ duyệt mà người duyệt duy nhất trong phạm vi mất quyền (giấy phép hết hạn, chuyển tầng): yêu cầu giữ nguyên trạng thái chờ và được nhắc theo quy tắc của feature 000 (FR-031a của 000), không tự chuyển cho người khác.
- Một người vừa là nhân viên vừa là người thân của một người cao tuổi trong viện: dùng hai tài khoản riêng; quyền của tài khoản nhân viên không cộng vào tài khoản người thân và ngược lại (FR-003).
- Tài khoản quản lý viện cuối cùng: không được khóa hay gỡ vai trò Quản lý viện bởi thao tác người dùng (FR-012).
- Nhân viên nghỉ việc rồi được tuyển lại: hồ sơ nhân viên ở feature 008 trở lại trạng thái làm việc; quản lý viện mở khóa tài khoản cũ, không tạo tài khoản mới, để lịch sử liền mạch (FR-011).
- Nhiều người thân dùng chung một số điện thoại hoặc email: mỗi người thân vẫn có tài khoản riêng; thông tin liên hệ trùng không được dùng làm tên đăng nhập chung (FR-001).
- Ghi nhận sự cố và kích hoạt khẩn cấp cho người cao tuổi ngoài phạm vi (ví dụ nhân viên bếp thấy người ngã ở hành lang): được phép, chỉ thấy thông tin nhận dạng, có nhật ký đánh dấu "ngoài phạm vi" (FR-044a).

## Requirements *(mandatory)*

### Functional Requirements

#### A. Tài khoản

- **FR-001**: Hệ thống MUST quản lý tài khoản với các thông tin: tên đăng nhập duy nhất, loại tài khoản (nhân viên / người thân), liên kết, vai trò, trạng thái, thời điểm đăng nhập gần nhất, số lần đăng nhập sai liên tiếp. *(Nguồn: 19.1, 3.2 TAI_KHOAN)*
- **FR-002**: Tài khoản nhân viên MUST liên kết đúng một hồ sơ nhân viên; mỗi hồ sơ nhân viên MUST có tối đa một tài khoản. Tài khoản người thân MUST liên kết đúng một hồ sơ người thân; mỗi hồ sơ người thân MUST có tối đa một tài khoản. *(Nguồn: 19.1; ERD miền D NHAN_VIEN ||--o| TAI_KHOAN, NGUOI_THAN ||--o| TAI_KHOAN)*
- **FR-003**: Một tài khoản MUST NOT đồng thời liên kết cả hồ sơ nhân viên và hồ sơ người thân. Tài khoản nhân viên MUST chỉ được gán vai trò trong AC-01 → AC-09; tài khoản người thân MUST chỉ có vai trò Người thân (AC-10). *(Nguồn: 2.3, 4.1)*
- **FR-004**: Chỉ Quản lý viện MUST được tạo tài khoản nhân viên. Khi tạo, tài khoản MUST có ít nhất một vai trò, và hồ sơ nhân viên liên kết MUST không ở trạng thái Nghỉ việc. *(Nguồn: UC-68, Permission Matrix dòng "Tài khoản, phân quyền, tham số")*
- **FR-005**: Tài khoản MUST NOT bị xóa bởi bất kỳ vai trò nào; tài khoản không còn dùng được khóa (FR-009). Mọi bản ghi lịch sử MUST tiếp tục hiển thị đúng người thực hiện. *(Nguồn: 1.3, 1.5 "không xóa đối tượng đã được lịch sử tham chiếu")*
- **FR-006**: Khi tạo tài khoản hoặc cấp lại mật khẩu, người dùng MUST đổi mật khẩu ở lần đăng nhập đầu tiên trước khi dùng chức năng khác. Cấp lại mật khẩu cho tài khoản nhân viên MUST do Quản lý viện thực hiện; cho tài khoản người thân MUST do Nhân viên hành chính hoặc Quản lý viện thực hiện (FR-051); mỗi lần cấp lại MUST được ghi nhật ký. *(Giả định, xem Assumptions; Clarification 2026-09-25)*

#### B. Trạng thái tài khoản và đăng nhập

- **FR-007**: Tài khoản MUST có một trong bốn trạng thái: Hoạt động, Khóa tạm, Không hoạt động, Đã khóa, và chỉ chuyển trạng thái theo bảng dưới đây. Chỉ tài khoản Hoạt động MUST được đăng nhập. *(Nguồn: 19.1, BR-M15-05, BR-M09-04, 5.6)*
- **FR-008**: Khi đăng nhập sai liên tiếp đủ số lần theo CFG-M15-01 (mặc định \[5 lần\]), tài khoản MUST chuyển Khóa tạm trong thời gian theo CFG-M15-01 (mặc định \[15 phút\]) rồi tự trở về Hoạt động. Đăng nhập đúng MUST đưa bộ đếm sai về 0. *(Nguồn: BR-M15-05)*
- **FR-009**: Khi hồ sơ nhân viên chuyển trạng thái Nghỉ việc, tài khoản liên kết MUST chuyển Đã khóa ngay trong cùng lần thay đổi đó. *(Nguồn: BR-M09-04, 19.1)*
- **FR-010**: Tài khoản người thân không đăng nhập quá CFG-M15-02 (mặc định \[180 ngày\]) MUST chuyển Không hoạt động. Quy tắc này MUST NOT áp dụng cho tài khoản nhân viên. *(Nguồn: BR-M15-05)*
- **FR-011**: Quản lý viện MUST được khóa và mở khóa tài khoản kèm lý do bắt buộc. Mở khóa MUST bị từ chối khi nguyên nhân khóa tự động còn tồn tại (hồ sơ nhân viên vẫn Nghỉ việc; người thân không còn người cao tuổi nào chưa quá CFG-M01-04 sau kết thúc). *(Nguồn: 19.1, 1.5)*
- **FR-012**: Hệ thống MUST luôn có ít nhất một tài khoản Hoạt động có vai trò Quản lý viện; thao tác người dùng làm vi phạm điều này MUST bị từ chối.
- **FR-013**: Khi tài khoản rời trạng thái Hoạt động, mọi phiên đăng nhập đang mở của tài khoản MUST bị chấm dứt ngay. *(Nguồn: 19.1 "phải bị khóa khi nhân viên không còn được phép sử dụng hệ thống")*
- **FR-014**: Thông báo từ chối đăng nhập MUST NOT tiết lộ tên đăng nhập có tồn tại hay không; MUST cho biết tài khoản đang bị khóa tạm hoặc cần liên hệ viện khi đăng nhập đúng nhưng tài khoản không ở Hoạt động.
- **FR-015**: Mỗi lần chuyển trạng thái tài khoản và mỗi lần đăng nhập (thành công hay thất bại) MUST được ghi nhật ký với thời điểm và kết quả; thay đổi do hệ thống thực hiện ghi người thực hiện là Bộ lập lịch hệ thống kèm căn cứ. *(Nguồn: 19.4, FR-045 của 000)*

**Bảng trạng thái tài khoản**:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái kế tiếp | Ai thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Tạo tài khoản | Hoạt động | Quản lý viện (nhân viên); Nhân viên hành chính hoặc Quản lý viện (người thân) | FR-002 → FR-004; FR-050 với người thân | Buộc đổi mật khẩu lần đầu; ghi nhật ký |
| Hoạt động | Đăng nhập sai đủ ngưỡng | Khóa tạm | Hệ thống | Sai liên tiếp đủ CFG-M15-01 | Từ chối mọi đăng nhập trong thời gian khóa |
| Khóa tạm | Hết thời gian khóa | Hoạt động | Hệ thống | Đủ thời gian theo CFG-M15-01 | Bộ đếm sai về 0 |
| Khóa tạm | Mở khóa sớm | Hoạt động | Quản lý viện; hoặc Trưởng tầng / Người phụ trách ca với tài khoản nhân viên cùng tầng/khu vực và cùng ca (FR-019c) | Có lý do; với Trưởng tầng / Người phụ trách ca: đã xác minh trực tiếp | Bộ đếm sai về 0; ghi nhật ký |
| Hoạt động | Hết hạn không sử dụng | Không hoạt động | Bộ lập lịch | Chỉ tài khoản người thân; không đăng nhập quá CFG-M15-02 | Chấm dứt phiên |
| Không hoạt động | Kích hoạt lại | Hoạt động | Nhân viên hành chính hoặc Quản lý viện | Có lý do; người thân còn ít nhất một quan hệ với người cao tuổi chưa quá CFG-M01-04 sau kết thúc | Buộc đổi mật khẩu; ghi nhật ký |
| Hoạt động / Khóa tạm / Không hoạt động | Khóa tài khoản | Đã khóa | Quản lý viện | Có lý do; FR-012 | Chấm dứt phiên |
| Hoạt động / Khóa tạm / Không hoạt động | Nhân viên Nghỉ việc | Đã khóa | Hệ thống | Hồ sơ nhân viên chuyển Nghỉ việc (BR-M09-04) | Chấm dứt phiên ngay |
| Hoạt động / Khóa tạm / Không hoạt động | Hết hạn sau kết thúc lưu trú | Đã khóa | Bộ lập lịch | Tài khoản người thân; mọi người cao tuổi có quan hệ đã ở trạng thái cuối quá CFG-M01-04 (5.6) | Chấm dứt phiên |
| Đã khóa | Mở khóa | Hoạt động | Quản lý viện | Có lý do; nguyên nhân khóa tự động không còn (FR-011) | Buộc đổi mật khẩu; ghi nhật ký |

Không có trạng thái kết thúc: tài khoản Đã khóa được giữ vĩnh viễn để lịch sử tham chiếu (FR-005).

#### C. Vai trò và quyền theo thao tác

- **FR-016**: Hệ thống MUST có đúng 10 vai trò hệ thống theo bảng 2.3 (AC-01 → AC-10). Vai trò MUST NOT được tạo mới hay xóa qua thao tác người dùng. *(Nguồn: 2.3, 2.4, 4.1; constitution VI)*
- **FR-017**: Một tài khoản nhân viên MAY có nhiều vai trò; quyền của vai trò cho tài khoản đó MUST là hợp các quyền của những vai trò được gán. *(Nguồn: ERD miền D TAI_KHOAN }o--|{ VAI_TRO)*
- **FR-018**: Quyền tối đa của mỗi vai trò MUST khớp Permission Matrix 4.4; khi 4.4 khác mục 19.3, mục 19.3 là căn cứ. *(Nguồn: 4.4, 19.3)*
- **FR-019**: Nhiệm vụ (Người phụ trách ca, Bác sĩ trực, Điều dưỡng phụ trách, Trưởng đoàn) MUST NOT tạo thêm vai trò hay quyền thao tác; nhiệm vụ chỉ góp vào phạm vi dữ liệu (mục D) và xác định người nhận nhắc việc/thông báo theo quy tắc của module sở hữu. Ngoại lệ chỉ gồm FR-019a, FR-019b, FR-019c và FR-019d. *(Nguồn: 2.4; spec 008 Q-80)*
- **FR-019a**: Nhân viên đang giữ nhiệm vụ Người phụ trách ca MUST có quyền "Xử lý việc quá hạn" (UC-27) như Trưởng tầng, dù vai trò của họ không có quyền này, nhưng MUST chỉ với công việc thuộc tầng/khu vực của ca đó và chỉ trong khoảng thời gian phạm vi của ca theo FR-032. Quyền này MUST mất khi nhiệm vụ kết thúc hoặc được chuyển cho người khác, và MUST NOT mở rộng sang quyền khác của Trưởng tầng (kiểm tra chất lượng, duyệt đổi ca…). Mỗi lần dùng quyền này, nhật ký MUST ghi căn cứ là nhiệm vụ Người phụ trách ca. *(Nguồn: 2.4, 8.7, UC-27; Clarification 2026-09-25)*
- **FR-019b**: Nhân viên đang giữ nhiệm vụ Người phụ trách ca MUST được tạo trực tiếp bản đính chính cho bản ghi nhóm 3 đã xác nhận gắn với tầng/khu vực của ca đó, theo spec 000 FR-026 (b), dù vai trò của họ không có quyền này; MUST chỉ trong khoảng thời gian phạm vi của ca theo FR-032 và MUST mất khi nhiệm vụ kết thúc hoặc được chuyển. Quyền này MUST NOT áp dụng cho bản ghi không gắn tầng/khu vực (spec 000 FR-026 c). Nhật ký MUST ghi căn cứ là nhiệm vụ Người phụ trách ca. *(Nguồn: 2.4, spec 000 FR-026; Clarification 2026-09-25)*
- **FR-019c**: Trưởng tầng, và nhân viên đang giữ nhiệm vụ Người phụ trách ca, MUST được mở khóa sớm tài khoản nhân viên đang Khóa tạm (FR-008) khi chủ tài khoản thuộc cùng tầng/khu vực và có ca trùng với ca đang diễn ra; MUST xác minh trực tiếp người đó và nhập lý do. Quyền này MUST NOT áp dụng cho tài khoản Đã khóa, Không hoạt động, tài khoản người thân, hay tài khoản ngoài tầng/ca. Mỗi lần mở khóa MUST được ghi nhật ký kèm căn cứ (vai trò Trưởng tầng hoặc nhiệm vụ Người phụ trách ca). *(Nguồn: BR-M15-05, 2.4; Clarification 2026-09-25)*
- **FR-019d**: Nhân viên đang giữ nhiệm vụ Người phụ trách ca MUST được "Ghi nhận vắng ca" (spec 008 FR-020) cho nhân viên có tên trong ca mình đang phụ trách, dù vai trò của họ không có quyền này; MUST chỉ trong khoảng thời gian phạm vi của ca theo FR-032 và MUST mất khi nhiệm vụ kết thúc hoặc được chuyển. Quyền này MUST NOT mở rộng sang các lệnh khác trên lịch ca (bổ sung nhân viên, chuyển người phụ trách ca, hủy ca). Nhật ký MUST ghi căn cứ là nhiệm vụ Người phụ trách ca. *(Nguồn: 2.4, BR-M04-13; spec 008 FR-020, Q-80, Q-81)*
- **FR-020**: Mỗi quyền MUST là một cặp (chức năng nghiệp vụ, loại thao tác), với loại thao tác thuộc tập của mục 19.2 và được hiểu theo bảng dưới đây. Ký hiệu của Permission Matrix 4.4 được quy đổi sang loại thao tác theo cùng bảng. *(Nguồn: 19.2, 4.4)*
- **FR-021**: Với dữ liệu nhóm 2 và nhóm 3 (mục 1.5), quyền "Sửa" MUST chỉ áp dụng cho bản nháp chưa gửi/chưa xác nhận; quyền thay đổi đối tượng đã có hiệu lực MUST là quyền thực hiện từng lệnh nghiệp vụ cụ thể, không có quyền "sửa trạng thái". *(Nguồn: 1.5; constitution III; FR-005 của 000)*
- **FR-022**: Quyền "Xem" MUST có thể giới hạn tới mức nhóm trường thông tin của một đối tượng: dinh dưỡng viên chỉ xem dị ứng và bệnh lý liên quan chế độ ăn; nhân viên bếp chỉ xem số suất và yêu cầu đặc biệt theo chế độ ăn, với suất đặc biệt trên phiếu bữa ăn thì xem họ tên, phòng, chế độ ăn, món thay thế và kết cấu thức ăn, MUST NOT xem dị ứng, bệnh lý hay hồ sơ sức khỏe; nhân viên vệ sinh chỉ xem công việc vệ sinh phòng/khu vực được phân công; nhân viên hành chính chỉ xem mức chăm sóc và cờ nguy cơ, MUST NOT xem dị ứng, bệnh nền, tiền sử, chỉ số, thuốc, kết quả đánh giá; trưởng tầng xem đầy đủ hồ sơ sức khỏe của người cao tuổi trong phạm vi. Giới hạn này áp dụng cả với nhật ký (spec 000 FR-049). *(Nguồn: 1.3, 13.3, 19.3; Clarification 2026-09-25)*
- **FR-023**: Trưởng tầng MUST NOT có quyền duyệt thay đổi lưu trú hay chi phí, và MUST NOT mặc định có quyền thay đổi y lệnh hoặc kê đơn. *(Nguồn: 19.3, 13.3)*
- **FR-024**: Quyền Duyệt kế hoạch chăm sóc MUST mặc định thuộc vai trò Bác sĩ; Quản lý viện MAY gán thêm quyền này cho từng tài khoản Điều dưỡng cụ thể kèm lý do. *(Nguồn: 2.4; Q-07)*
- **FR-025**: Ngoài FR-024, Quản lý viện MUST chỉ được thu hẹp quyền so với Permission Matrix 4.4, theo vai trò hoặc theo từng tài khoản: bỏ một loại thao tác, hoặc hạ phạm vi từ toàn viện ("X") xuống theo phân công ("P"). Mọi điều chỉnh làm quyền vượt ô tương ứng của ma trận (thêm chức năng, thêm loại thao tác, mở rộng phạm vi) MUST bị từ chối. Quyền đã thu hẹp MAY được khôi phục tới đúng mức của ma trận. Việc thu hẹp MUST NOT làm vi phạm FR-012 (luôn còn Quản lý viện có quyền cấu hình tài khoản và phân quyền). *(Nguồn: 4.4 "quyền tối đa của vai trò", 2.4; constitution VI; Clarification 2026-09-25, đề xuất Q-15)*
- **FR-026**: Chỉ Quản lý viện MUST được gán/gỡ vai trò và thay đổi quyền; mỗi thay đổi MUST có lý do và được ghi nhật ký với quyền trước/sau. *(Nguồn: BR-M15-04)*
- **FR-027**: Thay đổi vai trò hoặc quyền MUST có hiệu lực từ thao tác kế tiếp của mọi người dùng bị ảnh hưởng, kể cả người đang đăng nhập. *(Nguồn: BR-M15-02 áp dụng tương tự)*

**Bảng loại thao tác** *(mục 19.2, quy đổi ký hiệu 4.4)*:

| Loại thao tác | Ý nghĩa | Nhóm dữ liệu áp dụng (mục 1.5) | Ký hiệu 4.4 tương ứng |
| --- | --- | --- | --- |
| Xem | Đọc dữ liệu, danh sách, lịch sử trong phạm vi | Cả ba nhóm | X (toàn viện), P (trong phạm vi được phân công) |
| Tạo | Tạo đối tượng mới, lập bản nháp, ghi thêm bản ghi | Cả ba nhóm | C (nhóm 1), T |
| Sửa | Sửa danh mục; sửa bản nháp trước khi gửi/xác nhận | Nhóm 1; bản nháp nhóm 2/3 | C, T |
| Xác nhận | Xác nhận một ghi nhận làm nó trở thành bản ghi nhóm 3; xác nhận bàn giao, xác nhận liều | Nhóm 3 | T |
| Duyệt | Duyệt hoặc từ chối yêu cầu phê duyệt (feature 000) | Nhóm 2 | D |
| Chốt | Khóa một kỳ hoặc tập dữ liệu để các bản ghi trong đó chuyển nhóm 3 (ví dụ chốt kỳ chi phí) | Nhóm 2 → 3 | D (dòng "Chốt kỳ, xuất kế toán") |
| Thực hiện nghiệp vụ chuyên môn | Thao tác thuộc danh mục nghiệp vụ chuyên môn (mục E); ngoài quyền vai trò còn cần điều kiện pháp lý | Nhóm 2, 3 | T³, T⁴ và T trên các dòng chuyên môn |

Lệnh nghiệp vụ của nhóm 2 (ví dụ Chuyển giường, Ngừng đơn) được phân quyền như một quyền "Tạo" hoặc "Thực hiện nghiệp vụ chuyên môn" riêng cho từng lệnh, do spec module sở hữu khai báo.

#### D. Phạm vi dữ liệu

- **FR-028**: Với quyền ký hiệu "P", phạm vi dữ liệu của nhân viên MUST được suy ra từ phân công đang hiệu lực (feature 008), không được cấp tay. Phân công MAY theo khu vực, tầng, phòng hoặc người cao tuổi (spec 008 Q-92: không có đối tượng "nhóm người cao tuổi"). *(Nguồn: 19.2, 13.4, BR-M15-02)*
- **FR-029**: Phạm vi theo cấp MUST bao hàm cấp dưới: khu vực bao gồm mọi tầng, phòng, giường trong khu vực; tầng bao gồm mọi phòng trong tầng; phòng bao gồm mọi người cao tuổi đang có phân bổ giường mở tại phòng đó. Phân công theo người cao tuổi hoặc nhóm MUST chỉ bao gồm đúng những người được nêu. *(Nguồn: 2.2, 7.3)*
- **FR-030**: Vị trí của người cao tuổi khi tính phạm vi MUST là phân bổ giường đang mở tại thời điểm kiểm tra; người cao tuổi không có phân bổ giường đang mở chỉ thuộc phạm vi qua phân công theo người cao tuổi hoặc nhóm. *(Nguồn: 7.3, 7.4)*
- **FR-031**: Phạm vi của một nhân viên MUST là hợp của mọi phân công đang hiệu lực của họ, kể cả phạm vi phát sinh từ nhiệm vụ (ví dụ người phụ trách ca có phạm vi là tầng/khu vực của ca đó; trưởng đoàn có phạm vi là người tham gia chuyến đi trong thời gian chuyến đi). *(Nguồn: 2.4, 8.4, 13.4)*
- **FR-032**: Phân công theo ca MUST tạo phạm vi từ giờ bắt đầu ca trừ CFG-M15-07 tới giờ kết thúc ca cộng CFG-M15-07 (đề xuất, mặc định \[2 giờ\], bằng ngưỡng ghi nhận muộn CFG-M04-06); ngoài khoảng này phân công đó MUST NOT góp vào phạm vi. Phân công không gắn ca (ví dụ trưởng tầng được giao tầng) MUST tạo phạm vi trong suốt thời gian phân công còn hiệu lực. *(Nguồn: 2.4, BR-M04-12, BR-M15-02; Clarification 2026-09-25, đề xuất Q-14)*
- **FR-033**: Khi phân công thay đổi (chuyển tầng, đổi ca, đổi người phụ trách) hoặc người cao tuổi chuyển giường, phạm vi MUST được cập nhật ngay và áp dụng từ thao tác kế tiếp, không cần Quản lý viện thao tác. *(Nguồn: BR-M15-02, 7.4)*
- **FR-034**: Phạm vi của quyền ký hiệu "T" và "D" MUST theo vai trò: với Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc, Nhân viên vệ sinh, phạm vi MUST là phạm vi theo phân công (mục D, như "P"); với Quản lý viện, Bác sĩ, Nhân viên hành chính, Dinh dưỡng viên, Nhân viên bếp, phạm vi MUST là toàn viện. Quyền "X" và "C" MUST có phạm vi toàn viện. Khi một tài khoản có nhiều vai trò, phạm vi của mỗi quyền MUST lấy theo vai trò đem lại quyền đó (FR-017); nếu nhiều vai trò cùng đem lại một quyền, phạm vi MUST là hợp các phạm vi (toàn viện nếu có một vai trò toàn viện), trừ khi Quản lý viện đã thu hẹp quyền đó theo tài khoản (FR-025). Giới hạn trường của FR-022 MUST xét theo từng cặp (nhóm trường, phạm vi) của từng vai trò rồi mới hợp lại; ví dụ tài khoản Hành chính kiêm Điều dưỡng xem đầy đủ sức khỏe chỉ trong phạm vi phân công điều dưỡng, còn ngoài phạm vi đó chỉ thấy mức chăm sóc và cờ nguy cơ. Spec module sở hữu MAY thu hẹp thêm; ngoại lệ về phạm vi chỉ gồm FR-019a, FR-019b và FR-044a. *(Nguồn: 4.4 ký hiệu, 19.3; Clarification 2026-09-25)*
- **FR-035**: Danh sách, tìm kiếm, báo cáo và số liệu tổng hợp MUST chỉ gồm dữ liệu trong phạm vi của người xem; hệ thống MUST NOT tiết lộ sự tồn tại hay số lượng bản ghi ngoài phạm vi. Riêng với dashboard và báo cáo (UC-67), ký hiệu "P" của Bác sĩ và Nhân viên hành chính MUST được hiểu là toàn viện, nhất quán với FR-034; số liệu MUST chỉ gồm các chỉ tiêu dựng từ trường mà vai trò đó được xem theo FR-022 (Hành chính không thấy chỉ tiêu sức khỏe như chỉ số, cảnh báo sức khỏe, sự cố y tế, thuốc). Trưởng tầng và Điều dưỡng giữ "P" theo phân công. *(Nguồn: BR-M15-01; UC-67 dòng "Dashboard, báo cáo"; Clarification 2026-09-25)*

#### E. Điều kiện pháp lý

- **FR-036**: Hệ thống MUST có danh mục nghiệp vụ chuyên môn; mỗi mục MUST khai báo: loại giấy phép/đào tạo cá nhân yêu cầu, phạm vi hành nghề yêu cầu, và có yêu cầu phạm vi hoạt động của cơ sở hay không. Danh mục mặc định MUST gồm ít nhất: chẩn đoán và kê đơn nội bộ (bác sĩ có giấy phép đúng phạm vi và cơ sở có phạm vi khám chữa bệnh); phát thuốc và xác nhận liều (điều dưỡng có giấy phép còn hiệu lực). *(Nguồn: 10.4, 13.4, BR-M06-05)*
- **FR-037**: Một thao tác thuộc danh mục nghiệp vụ chuyên môn MUST chỉ được chấp nhận khi, tại thời điểm thực hiện: (a) người thực hiện có giấy phép/đào tạo yêu cầu còn hiệu lực; (b) phạm vi hành nghề trên giấy phép bao gồm nghiệp vụ đó; (c) nếu mục yêu cầu, cơ sở có phạm vi hoạt động phù hợp còn hiệu lực. *(Nguồn: BR-M06-05, BR-M09-01, 10.4)*
- **FR-038**: Giấy phép MUST được coi là còn hiệu lực đến hết ngày hết hạn ghi trên giấy phép, theo múi giờ Asia/Ho_Chi_Minh. Khi hết hạn, quyền chuyên môn tương ứng MUST tự tắt; khi hồ sơ giấy phép được cập nhật gia hạn, quyền MUST tự bật lại, không cần Quản lý viện cấp lại. *(Nguồn: BR-M06-05, NFR-09)*
- **FR-039**: Hệ thống MUST cảnh báo Quản lý viện khi giấy phép hoạt động của cơ sở hoặc giấy phép của người hành nghề sẽ hết hạn trong vòng CFG-M06-02 (mặc định \[60 ngày\]); cảnh báo MUST nêu các nghiệp vụ chuyên môn sẽ bị tắt và người bị ảnh hưởng. Cảnh báo đào tạo bắt buộc (CFG-M09-03) thuộc feature 008. *(Nguồn: BR-M06-05, BR-M09-05)*
- **FR-040**: Điều kiện pháp lý MUST chỉ giới hạn thao tác chuyên môn; MUST NOT giới hạn quyền xem hoặc thao tác không thuộc danh mục nghiệp vụ chuyên môn của cùng người dùng. Bản ghi đã xác nhận khi điều kiện còn hiệu lực MUST giữ nguyên giá trị sau khi điều kiện hết hiệu lực. *(Nguồn: BR-M06-05 chỉ nêu "quyền chẩn đoán và kê đơn")*

#### F. Kiểm tra quyền hiệu lực

- **FR-041**: Mọi lần xem và mọi thao tác của người dùng MUST được kiểm tra đủ ba lớp — quyền vai trò (mục C), phạm vi dữ liệu (mục D), điều kiện pháp lý (mục E) — tại thời điểm hệ thống nhận yêu cầu; thiếu bất kỳ lớp nào thì từ chối. *(Nguồn: BR-M15-01)*
- **FR-041a**: Với bản ghi nhận ngoại tuyến được đồng bộ sau (Q-01, 8.6), khi thời điểm trên thiết bị hợp lý theo FR-041c, ba lớp quyền MUST được kiểm tra theo trạng thái tại thời điểm ghi trên thiết bị (trạng thái tài khoản, vai trò, phân công và khoảng thời gian FR-032, giấy phép), nhất quán với BR-M04-12. Bản ghi không hợp lệ tại thời điểm đó MUST bị từ chối khi đồng bộ. Bản ghi hợp lệ tại thời điểm đó MUST được chấp nhận; nếu tại thời điểm đồng bộ người ghi đã mất một trong ba lớp, bản ghi MUST được gắn nhãn "đồng bộ sau khi mất quyền" và hệ thống MUST báo trưởng tầng của tầng/khu vực liên quan để xem lại. Thao tác bắt buộc trực tuyến theo Q-01 (kích hoạt khẩn cấp, xác nhận liều thuốc có kiểm soát đặc biệt) không thuộc quy tắc này. *(Nguồn: 8.6, BR-M04-12, DBR-25, Q-01; Clarification 2026-09-25)*
- **FR-041c**: Thời điểm trên thiết bị MUST chỉ được dùng cho FR-041a khi hợp lý: (a) không muộn hơn thời điểm đồng bộ; (b) không sớm hơn lần kết nối gần nhất của thiết bị với hệ thống trước khi ngoại tuyến; (c) khoảng từ lần kết nối gần nhất tới lúc đồng bộ không vượt CFG-M15-08 (đề xuất, mặc định \[24 giờ\]). Bản ghi không thỏa MUST NOT được tự động chấp nhận hay từ chối, mà MUST chuyển trạng thái "chờ xem lại" và báo trưởng tầng của tầng/khu vực liên quan. Trưởng tầng MUST chấp nhận (bản ghi có hiệu lực từ thời điểm chấp nhận, giữ thời điểm thiết bị để tham khảo) hoặc từ chối kèm lý do (bản ghi được lưu nhưng không có hiệu lực); mỗi quyết định MUST được ghi nhật ký. Bản ghi gắn nhãn "đồng bộ sau khi mất quyền" (FR-041a) đã có hiệu lực, trưởng tầng xử lý sai sót nếu có bằng đính chính theo spec 000. *(Nguồn: 8.6, DBR-25, NFR-09, Q-01; Clarification 2026-09-25)*
- **FR-041b**: Khi không xác định được một trong ba lớp quyền vì dữ liệu nguồn thiếu, mâu thuẫn hoặc không đọc được (phân công, giấy phép, bản đồng ý, quan hệ người thân…), hệ thống MUST từ chối lần xem hoặc thao tác đó như khi lớp đó không thỏa. Ngoại lệ duy nhất: ghi nhận sự cố và kích hoạt khẩn cấp theo FR-044a MUST vẫn được thực hiện, với giới hạn hiển thị của FR-044a. Mỗi lần từ chối vì lý do này MUST được ghi nhật ký kèm dữ liệu nguồn bị lỗi, và hệ thống MUST báo Quản lý viện; các lần cùng lỗi dữ liệu MUST được gộp thành một báo cáo cho tới khi lỗi được khắc phục. *(Nguồn: BR-M15-01, NFR-08; Clarification 2026-09-25)*
- **FR-042**: Thao tác mà người dùng không có quyền hiệu lực MUST không xuất hiện trong danh sách thao tác của họ; nếu vẫn được gửi tới, hệ thống MUST từ chối. *(Nguồn: BR-M15-01)*
- **FR-043**: Khi từ chối, hệ thống MUST nêu lớp nào không thỏa (không có quyền / ngoài phạm vi / điều kiện pháp lý hết hiệu lực kèm tên giấy phép), trừ trường hợp nêu lý do sẽ tiết lộ dữ liệu ngoài phạm vi.
- **FR-044**: Thao tác do Bộ lập lịch hệ thống (AC-11) thực hiện MUST NOT bị giới hạn bởi phạm vi dữ liệu hay điều kiện pháp lý của người dùng, nhưng MUST chỉ thực hiện các tác động mà quy tắc nghiệp vụ nguồn quy định. *(Nguồn: 4.1 AC-11)*
- **FR-044a**: Ghi nhận sự cố (UC-35) và kích hoạt khẩn cấp (UC-36) MUST được miễn lớp phạm vi dữ liệu (ngoại lệ phạm vi còn lại là quyền từ nhiệm vụ ở FR-019a, FR-019b): mọi tài khoản nhân viên có quyền vai trò "T" với "Sự cố, khẩn cấp" MUST thực hiện được cho bất kỳ người cao tuổi nào chưa ở trạng thái cuối, không phụ thuộc phân công. Để chọn đúng người, người ghi nhận MUST chỉ thấy thông tin nhận dạng (họ tên, ảnh, phòng/giường hiện tại) của người ngoài phạm vi; MUST NOT thấy thông tin sức khỏe hay các phần hồ sơ khác. Sau khi tạo, việc xem và xử lý tiếp sự cố đó theo phạm vi thông thường, trừ việc xem lại chính bản ghi mình đã tạo. Mỗi lần dùng ngoại lệ MUST được ghi nhật ký kèm đánh dấu "ngoài phạm vi". *(Nguồn: 4.4 dòng "Sự cố, khẩn cấp", UC-35, UC-36, BR-M15-01; Clarification 2026-09-25)*

#### G. Tài khoản và quyền hiệu lực của người thân

- **FR-045**: Với tài khoản người thân, ba lớp của BR-M15-01 MUST được hiểu là: (1) vai trò — quyền tối đa của cột "NT" trong Permission Matrix 4.4; (2) phạm vi — những người cao tuổi có quan hệ người thân còn hiệu lực với người đó; (3) điều kiện — quyền được cấp riêng cho người thân đó (xem sức khỏe, xem chi phí, nhận thông báo khẩn, đăng ký thăm, được phép đón, được yêu cầu thay đổi dịch vụ) và bản đồng ý chia sẻ dữ liệu còn hiệu lực bao gồm người đó cho phần sức khỏe. *(Nguồn: 14.1, BR-M10-01, BR-M01-08, DBR-03, 4.4 chú thích ¹ ²)*
- **FR-046**: Thông tin sức khỏe MUST chỉ hiện với người thân khi đồng thời có quyền xem sức khỏe (14.1) và bản đồng ý còn hiệu lực bao gồm người đó; khi đồng ý bị rút lại, phần này MUST ẩn từ lần xem kế tiếp. *(Nguồn: BR-M01-08, DBR-03)*
- **FR-047**: Chỉ người thân là người đại diện MUST được gửi yêu cầu thay đổi dịch vụ và xác nhận thay đổi danh sách người được phép đón. *(Nguồn: BR-M10-01, 19.3, 4.4 chú thích ²)*
- **FR-048**: Người thân MUST NOT xem nhật ký thay đổi; chỉ thấy giá trị hiện hành của dữ liệu được cấp. *(Nguồn: FR-049 của 000)*
- **FR-049**: Sau khi người cao tuổi chuyển Kết thúc lưu trú hoặc Qua đời, người thân MUST còn xem được thông tin được cấp của người đó ở chế độ chỉ đọc trong CFG-M01-04 (mặc định \[30 ngày\]); sau thời hạn đó người cao tuổi đó MUST ra khỏi phạm vi của người thân. Khi mọi người cao tuổi có quan hệ đều đã ra khỏi phạm vi theo quy tắc này, tài khoản MUST chuyển Đã khóa. *(Nguồn: 5.6)*
- **FR-050**: Tài khoản người thân MUST chỉ được tạo cho hồ sơ người thân có ít nhất một quan hệ với người cao tuổi chưa ở trạng thái cuối. *(Nguồn: 14.1)*
- **FR-051**: Nhân viên hành chính và Quản lý viện MUST được tạo, kích hoạt lại và cấp lại mật khẩu cho tài khoản người thân, không cần yêu cầu phê duyệt; tài khoản tạo ra MUST chỉ có vai trò Người thân. Nhân viên hành chính MUST NOT tạo hay thay đổi tài khoản nhân viên, gán vai trò khác, hay thay đổi quyền (FR-026). Khóa và mở khóa tài khoản Đã khóa vẫn do Quản lý viện (FR-011). Mỗi thao tác MUST có lý do (trừ tạo mới) và được ghi nhật ký. *(Nguồn: UC-55, NFR-01; Clarification 2026-09-25, đề xuất Q-16)*
- **FR-052**: Trước khi tạo, cấp lại mật khẩu hoặc kích hoạt lại tài khoản người thân, người thực hiện MUST xác minh danh tính bằng một trong hai cách: (a) người thân có mặt tại viện, đối chiếu giấy tờ tùy thân với hồ sơ người thân; (b) gọi lại số điện thoại đã đăng ký trong hồ sơ người thân (không dùng số do người yêu cầu cung cấp lúc yêu cầu). Cách xác minh MUST được ghi vào nhật ký của thao tác; thao tác không có cách xác minh MUST bị từ chối. Mật khẩu tạm MUST chỉ giao trực tiếp cho người đã được xác minh. Giai đoạn đầu MUST NOT có chức năng người thân tự đặt lại mật khẩu; bổ sung sau khi chốt Q-06 (nhà cung cấp SMS). *(Nguồn: 5.1, 14.1, Q-06; Clarification 2026-09-25)*

### Key Entities *(include if feature involves data)*

- **Tài khoản (TAI_KHOAN)** – nhóm 1 theo 3.2, có trạng thái theo bảng ở mục B: tên đăng nhập, loại (nhân viên / người thân), liên kết đúng một hồ sơ nhân viên hoặc một hồ sơ người thân, trạng thái, thời điểm đăng nhập gần nhất, số lần đăng nhập sai liên tiếp, cờ buộc đổi mật khẩu. Không xóa.
- **Vai trò (VAI_TRO)** – nhóm 1, cố định 10 vai trò theo bảng 2.3; mỗi vai trò có tập quyền tối đa theo Permission Matrix 4.4.
- **Quyền**: cặp (chức năng nghiệp vụ, loại thao tác) kèm phạm vi mặc định (toàn viện hoặc theo phân công); gắn với vai trò, hoặc với tài khoản khi được gán thêm (FR-024, FR-025).
- **Gán vai trò**: tài khoản, vai trò, người gán, thời điểm, lý do; lịch sử gán/gỡ được giữ.
- **Phạm vi dữ liệu** (khái niệm, không lưu tay): suy ra từ Phân công (PHAN_CONG, feature 008) và phân bổ giường (PHAN_BO_GIUONG, feature 003) tại thời điểm kiểm tra.
- **Danh mục nghiệp vụ chuyên môn** – nhóm 1: nghiệp vụ, loại giấy phép/đào tạo cá nhân yêu cầu, phạm vi hành nghề yêu cầu, có yêu cầu phạm vi hoạt động của cơ sở.
- **Giấy phép** (dùng, không sở hữu): CHUNG_CHI của nhân viên (feature 008) và giấy phép hoạt động của cơ sở (mục 10.4): loại, số, phạm vi, ngày cấp, ngày hết hạn.
- **Quan hệ người thân, quyền theo người thân, bản đồng ý** (dùng, không sở hữu): QUAN_HE_NGUOI_THAN, BAN_DONG_Y (feature 012, 001).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Trong bộ kiểm thử gồm mọi ô của Permission Matrix 4.4 (10 vai trò × mọi chức năng), 100% kết quả cho/từ chối khớp ma trận và mục 19.3, cho cả người cao tuổi trong và ngoài phạm vi.
- **SC-002**: 0 lần xem hoặc thao tác nào thành công trên dữ liệu ngoài phạm vi (trừ ghi nhận sự cố/kích hoạt khẩn cấp theo FR-044a, khi đó 0 lần lộ thông tin sức khỏe), hoặc thao tác chuyên môn nào thành công khi thiếu điều kiện pháp lý, trong toàn bộ kịch bản kiểm thử.
- **SC-003**: Sau khi phân công hoặc quyền thay đổi, thao tác đầu tiên của người dùng bị ảnh hưởng đã theo quyền mới trong 100% lần thử; quản lý viện không phải thực hiện thêm bước cấp quyền nào.
- **SC-004**: 0 lần đăng nhập thành công bằng tài khoản của nhân viên đã chuyển Nghỉ việc, tính từ thời điểm chuyển trạng thái.
- **SC-005**: Quản lý viện tạo xong một tài khoản nhân viên có vai trò trong không quá 3 phút.
- **SC-006**: Vào ngày sau ngày hết hạn giấy phép, 100% thao tác chuyên môn phụ thuộc giấy phép đó bị từ chối; vào ngày sau khi cập nhật gia hạn, 100% được chấp nhận lại mà không cần cấp quyền.
- **SC-007**: 100% thay đổi vai trò, quyền và trạng thái tài khoản có bản ghi nhật ký đủ người thực hiện, thời điểm, giá trị trước/sau và lý do.
- **SC-008**: Người dùng bị từ chối hiểu được lý do (không có quyền / ngoài phạm vi / giấy phép hết hạn) mà không cần hỏi quản lý viện, trong ít nhất 90% lần thử với người dùng thật.
- **SC-009**: Trong bộ kiểm thử bản ghi ngoại tuyến, 100% bản ghi được xử lý đúng một trong ba kết quả theo FR-041a, FR-041c (chấp nhận, chấp nhận kèm nhãn "đồng bộ sau khi mất quyền", "chờ xem lại") hoặc bị từ chối, và 0 bản ghi có thời điểm thiết bị bất hợp lý được tự động chấp nhận.
- **SC-010**: 0 lần nhân viên hành chính thấy trường sức khỏe bị cấm theo FR-022, kể cả qua dashboard, báo cáo và nhật ký; 100% trường hợp thiếu dữ liệu nguồn để tính quyền bị từ chối, trừ ghi nhận sự cố/kích hoạt khẩn cấp (FR-041b).
- **SC-011**: 100% lần cấp lại mật khẩu, kích hoạt lại tài khoản người thân và mở khóa sớm bởi trưởng tầng/người phụ trách ca có ghi cách xác minh và lý do trong nhật ký (FR-052, FR-019c).

## Assumptions

- Thư mục feature dùng số `002` theo bản đồ feature ở 4.2 (UC-68, UC-70 ghi feature 002), trùng với số tuần tự kế tiếp.
- Một tài khoản chỉ phục vụ một loại người dùng; người vừa là nhân viên vừa là người thân có hai tài khoản riêng, để quyền nhân viên không lọt sang cổng người thân và ngược lại (FR-003).
- Đăng nhập bằng tên đăng nhập và mật khẩu; buộc đổi mật khẩu lần đầu và cấp lại mật khẩu là thực hành chuẩn, tài liệu nguồn không nêu (FR-006). Chính sách độ mạnh mật khẩu và thời gian tự đăng xuất để giai đoạn thiết kế, và nếu cần giá trị thì bổ sung mã CFG.
- Tài khoản người thân Không hoạt động không tự trở lại Hoạt động khi đăng nhập đúng, mà phải được kích hoạt lại (bảng mục B); nếu không, BR-M15-05 không có tác dụng.
- Danh mục nghiệp vụ chuyên môn mặc định chỉ gồm những thao tác mà tài liệu nêu rõ cần giấy phép (chẩn đoán, kê đơn nội bộ, phát thuốc/xác nhận liều); các thao tác khác (đánh giá đầu vào, thiết lập ngưỡng, duyệt kế hoạch chăm sóc) không bị giới hạn bởi giấy phép trừ khi Quản lý viện bổ sung vào danh mục.
- Giấy phép còn hiệu lực đến hết ngày hết hạn (FR-038).
- Nhật ký đăng nhập (thành công và thất bại) được ghi để phục vụ điều tra an ninh (FR-015), dù mục 19.4 chỉ liệt kê "phân quyền".
- Trạng thái làm việc của nhân viên ngoài Nghỉ việc (nghỉ phép, tạm nghỉ) không khóa tài khoản; nhân viên không có phân công thì phạm vi "P" rỗng (US4 kịch bản 7).
- Hiển thị dữ liệu đã tải trên màn hình không bị thu hồi khi quyền đổi; mọi lần tải lại và thao tác gửi đi được kiểm tra lại (FR-041).

## Điểm cần báo lại về tài liệu nguồn

Ngày 2026-09-25, các điểm đã chốt đã được đưa vào `docs/nghiep-vu.md` và `docs/phan-tich-yeu-cau.md` theo yêu cầu. Danh sách dưới đây ghi nơi đã phản ánh và các điểm còn mở.

**Đã phản ánh vào tài liệu nguồn**

1. TAI_KHOAN có vòng đời trạng thái: cột Nhóm ở 3.2 ghi "1 (vai trò); 2 (trạng thái tài khoản)".
2. Hành chính (và Quản lý viện) quản lý tài khoản người thân, xác minh danh tính, chưa có tự đặt lại mật khẩu (Q-16): 19.1, chú thích ¹² của Permission Matrix 4.4, mục 24.2.
3. Phạm vi theo ca ± CFG-M15-07 (Q-14): BR-M15-02, Phụ lục 25, mục 24.2. Thời gian ngoại tuyến tối đa CFG-M15-08 và trạng thái "chờ xem lại": cột Mặc định của Q-01, Phụ lục 25.
4. Quản lý viện chỉ được thu hẹp quyền, ngoại lệ là quyền Duyệt kế hoạch chăm sóc (Q-15); hợp phạm vi khi nhiều vai trò: 19.2, đoạn bổ sung dưới Permission Matrix 4.4, mục 24.2.
5. Ghi nhận sự cố / kích hoạt khẩn cấp ngoài phạm vi: BR-M15-01, chú thích ¹⁰ của 4.4.
6. Hai tham số cảnh báo giấy phép: Phụ lục 25 — CFG-M06-02 cho giấy phép hành nghề, CFG-M09-03 cho chứng chỉ đào tạo.
7. Người phụ trách ca xử lý việc quá hạn, đính chính, mở khóa sớm: 2.4, chú thích ⁹ của 4.4, BR-M15-05.
8. Nhật ký ghi cả đăng nhập: 19.4.
9. Hành chính chỉ xem mức chăm sóc và cờ nguy cơ; phạm vi T/D theo vai trò; từ chối mặc định khi thiếu dữ liệu nguồn; dashboard của Bác sĩ, Hành chính là toàn viện: 19.3, chú thích ⁵ và ¹¹ của 4.4.

**Còn mở**

1. **Việc cần phản ánh ở spec khác** (không phải tài liệu nguồn): ngoại lệ ghi nhận sự cố ngoài phạm vi (FR-044a) cần có trong spec feature 007; quyền xử lý việc quá hạn của Người phụ trách ca (FR-019a) cần có trong spec feature 005.
2. **Q-01** (ghi nhận khi mất kết nối) và **Q-06** (nhà cung cấp SMS, ảnh hưởng tự đặt lại mật khẩu) vẫn mở.
3. **Đồng bộ với spec 008 (2026-09-26)**: FR-019d (Người phụ trách ca ghi nhận vắng ca, spec 008 Q-80) cần được phản ánh vào định nghĩa "Người phụ trách ca" ở mục 2.4 và chú thích ⁹ của Permission Matrix 4.4.
