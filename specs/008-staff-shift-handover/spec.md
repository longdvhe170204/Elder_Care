# Feature Specification: Nhân viên, ca trực, phân công và bàn giao ca

**Feature Branch**: `008-staff-shift-handover`

**Created**: 2026-09-26

**Status**: Draft

**Input**: User description: "Quản lý nhân viên, ca trực, phân công và bàn giao ca theo docs/nghiep-vu.md Module 09 mục 13.1–13.5: hồ sơ nhân viên với giấy phép và đào tạo có thời hạn; ca trực cấu hình được, có người phụ trách ca; phân công theo tầng, phòng, người cao tuổi; không phân nhân viên thiếu chứng chỉ vào việc yêu cầu; cảnh báo tỷ lệ phục vụ theo trọng số chăm sóc; bàn giao ca do hệ thống tự lập bản nháp từ việc tồn, liều bất thường, chỉ số vượt ngưỡng, cảnh báo mở; ca sau xác nhận thì trách nhiệm chuyển sang."

## Clarifications

### Session 2026-09-26

- Q: Ranh giới 008/015 về lịch ca (UC-50 ghi cho cả hai)? → A: 008 sở hữu lịch ca (tạo tay, công bố, Ghi nhận vắng ca, Bổ sung nhân viên vào ca, Chuyển người phụ trách ca, Hủy ca); 015 sở hữu sinh lịch từ mẫu xoay ca (BR-M09-09), đổi ca (UC-54, BR-M09-10), nghỉ đột xuất có duyệt và phủ tối thiểu, gợi ý người thay (BR-M09-11) (Phạm vi, FR-020, đề xuất Q-77).
- Q: Ai giữ trách nhiệm với cảnh báo, sự cố đang mở khi ca sau đã bắt đầu mà bàn giao chưa được xác nhận (hoặc đang Có ý kiến)? → A: Từ giờ bắt đầu ca sau, Người phụ trách ca sau **tạm nhận** trách nhiệm cảnh báo, sự cố đang mở mà người phụ trách thuộc ca trước và không còn ca; công việc tồn chỉ chuyển khi xác nhận. Khi xác nhận, trách nhiệm chuyển chính thức theo feature 007 FR-037 (FR-044, FR-044a, đề xuất Q-78).
- Q: Tỷ lệ phục vụ tính những vai trò nào, ngưỡng hiểu theo chiều nào? → A: Tính Điều dưỡng và Nhân viên chăm sóc (Trưởng tầng không tính); ngưỡng là trọng số tối đa trên mỗi nhân viên, cấu hình riêng cho từng mẫu ca (ví dụ ca ngày, ca đêm) (FR-034, đề xuất Q-79).

### Session 2026-09-26 (lượt 2, /speckit-clarify)

- Q: Ngoài Trưởng tầng, Người phụ trách ca có được "Ghi nhận vắng ca" cho nhân viên cùng ca không? → A: Được, chỉ với ca mình đang phụ trách và trong thời gian phạm vi ca; đây là ngoại lệ quyền theo nhiệm vụ thứ tư (đề xuất feature 002 FR-019d) (FR-020, FR-048, đề xuất Q-80).
- Q: "Ghi nhận vắng ca" được dùng từ lúc nào? → A: Chỉ từ giờ bắt đầu ca trừ CFG-M15-07 (mặc định \[2 giờ\]) tới giờ kết thúc ca; vắng biết trước phải đi qua yêu cầu nghỉ đột xuất có duyệt của feature 015 (FR-020, Edge Cases, đề xuất Q-81).
- Q: Phân công lập trên ca của lịch còn Nháp có hiệu lực ngay không? → A: Không; được soạn ở trạng thái Nháp, không tạo phạm vi dữ liệu, không gán công việc hay liều; khi công bố lịch, hệ thống kiểm tra lại FR-028, FR-030 và phân công chỉ có hiệu lực từ lúc công bố (FR-016, FR-025, FR-027, đề xuất Q-82).
- Q: Khi hoàn tất bàn giao, có bắt buộc ghi nhận định cho từng mục nghiêm trọng không? → A: Có; mỗi mục nghiêm trọng (cảnh báo, sự cố mức Khẩn cấp hoặc Trung bình; liều Bỏ lỡ, Từ chối; công việc Bắt buộc quá hạn) MUST có ghi chú, cộng nhận định chung; mục khác tùy chọn (FR-041, FR-042, đề xuất Q-83).
- Q: Một tầng/khu vực có được giao cho nhiều Trưởng tầng cùng lúc không? → A: Không; tối đa một Trưởng tầng được giao tại một thời điểm, một người được giao nhiều tầng; thay tạm bằng giao có thời hạn (FR-024, đề xuất Q-84).

### Session 2026-09-26 (lượt 3, sau checklist business-rules)

- Q: Trong khoảng từ giờ bắt đầu ca sau tới lúc xác nhận bàn giao, ai giữ công việc tồn và liều Mang theo chờ ghi nhận của người ca trước đã hết ca? → A: Tạm thành công việc chung / liều chung của tầng từ giờ bắt đầu ca sau; khi xác nhận bàn giao, áp feature 005 FR-047 như bình thường (FR-044, FR-044a, đề xuất Q-85).
- Q: Khi tầng chưa có Trưởng tầng được giao, ai làm thay các lệnh bàn giao dành cho "Trưởng tầng của phạm vi"? → A: Lịch của tầng chưa có Trưởng tầng được giao không công bố được; nếu thiếu giữa chừng, Quản lý viện thực hiện thay (hoàn tất, bổ sung, xác nhận, gửi ý kiến, tạm nhận), bắt buộc lý do, nhật ký đánh dấu "thay Trưởng tầng" (FR-016, FR-024, FR-048, đề xuất Q-86). *(Phần "Quản lý viện thực hiện thay" đã được thay bằng Q-90 ở lượt 4; phần chặn công bố lịch giữ nguyên.)*
- Q: Khi bàn giao đang Có ý kiến và tới giờ kết thúc ca, ca trước có đóng không? → A: Không; Có ý kiến coi như chưa lập xong, ca vào Chờ bàn giao và chỉ đóng khi bàn giao Đã lập hoặc Đã xác nhận; bàn giao chuyển Có ý kiến sau khi ca đã đóng thì ca giữ Đã đóng nhưng Trưởng tầng được nhắc (FR-022, FR-043, đề xuất Q-87).
- Q: Khi còn Bản nháp, mục bàn giao tự lập được coi là "đã xử lý xong" khi nào? → A: Khi nguồn ở trạng thái kết thúc (công việc đã đóng; liều đã được ghi hoặc đính chính khỏi Trễ, Bỏ lỡ, Từ chối, Mang theo chờ ghi nhận; cảnh báo, sự cố đã đóng; yêu cầu vệ sinh Gấp Hoàn thành); chỉ số vượt ngưỡng và biến động người cao tuổi trong ca luôn được giữ (FR-040, đề xuất Q-88).
- Q: Hai mốc "24 giờ" viết cứng ở FR-036 và FR-038 có chuyển thành tham số cấu hình không? → A: Có, hai tham số riêng: CFG-M09-08 báo Quản lý viện khi ca dưới ngưỡng phục vụ sắp bắt đầu, mặc định \[24 giờ\]; CFG-M09-09 khoảng tối đa tìm ca sau để nhận bàn giao, mặc định \[24 giờ\] (FR-036, FR-038, Edge Cases, đề xuất Q-89).

### Session 2026-09-26 (lượt 4, sau checklist consistency)

- Q: Khi tầng tạm thời chưa có Trưởng tầng, các lệnh bàn giao và tạm nhận của Trưởng tầng giao cho ai để không vượt Permission Matrix? → A: Bỏ quyền làm thay của Quản lý viện; Quản lý viện được nhắc ngay để giao Trưởng tầng tạm bằng giao có thời hạn (FR-024); trong lúc chờ, Người phụ trách ca đang diễn ra của tầng thực hiện các lệnh đó trong phạm vi ca của mình (FR-024, FR-048, Edge Cases, đề xuất Q-90, thay phần làm thay của Q-86).
- Q: Một lần "Ghi nhận vắng ca" chỉ ảnh hưởng ca đó hay cả các ca tương lai? → A: Chỉ ca đó; ca tương lai của nhân viên giữ nguyên; nghỉ nhiều ca đi qua yêu cầu nghỉ đột xuất của feature 015; "các ca tương lai" ở feature 005 FR-032 chỉ áp cho Nghỉ việc (FR-020, đề xuất Q-91).
- Q: Phân công có loại đối tượng "nhóm người cao tuổi" như feature 002 FR-028 ghi không? → A: Không; đối tượng phân công chỉ gồm tầng/khu vực, phòng, người cao tuổi; feature 002 FR-028 bỏ "nhóm người cao tuổi"; mục 19.2 tài liệu nguồn cần sửa tương ứng (FR-025, đề xuất Q-92).
- Q: Công việc Thường quá hạn đã chuyển tạm thành việc chung và đã có nhân viên ca sau nhận có bị tự đóng khi bàn giao được xác nhận (feature 005 FR-047) không? → A: Không; chỉ công việc vẫn là việc chung chưa ai nhận mới bị tự đóng như FR-047 (FR-044a, feature 005 FR-047a, đề xuất Q-93).
- Q: Trong lúc cảnh báo ở trạng thái "tạm nhận", chuỗi leo thang và thông báo gửi cho ai? → A: Người tạm nhận thay Điều dưỡng phụ trách ở mọi chỗ feature 007 dùng (người nhận thông báo, bậc đầu của chuỗi leo thang); các bậc sau giữ nguyên (FR-044a, feature 007 FR-037, đề xuất Q-94).

### Cập nhật 2026-09-27 (đồng bộ với spec 011)

Spec 011 dùng phân công và ca của spec này để xác định người nhận phiếu bữa ăn tại tầng (Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc, Người phụ trách ca có phạm vi tại tầng/khu vực của phiếu trong ca, Q-143). Phiếu của khu bán trú do nhân viên phân công tại tầng/khu vực mà khu nghỉ bán trú gắn vào nhận (feature 003 FR-038); không cần loại đối tượng phân công mới. Ca bếp là ca toàn viện không có bàn giao: phát sinh và sai lệch chưa xử lý do bất kỳ nhân viên bếp nào đang trong ca xử lý (feature 011 FR-039). Không có yêu cầu nào thay đổi của spec này.

## Phạm vi

**Trong phạm vi** (Module 09 mục 13.1 → 13.5; BR-M09-01 → 08; UC-49, UC-50 phần lập và công bố lịch ca, UC-51, UC-52, UC-53; DBR-20, DBR-21):

1. Hồ sơ nhân viên và trạng thái làm việc; lệnh Cho nghỉ việc và các tác động (13.1, UC-49, BR-M09-04).
2. Danh mục loại giấy phép/đào tạo; giấy phép hành nghề có phạm vi hành nghề; đào tạo (kể cả đào tạo sơ cứu) có thời hạn; gia hạn; cảnh báo đào tạo bắt buộc sắp hết hạn (13.1, BR-M09-05, CFG-M09-03).
3. Mẫu ca cấu hình được (tên ca, giờ bắt đầu, giờ kết thúc, phạm vi); lịch ca theo tháng ở trạng thái Nháp → Đã công bố; ca cụ thể có nhân viên, người phụ trách ca và Bác sĩ trực (13.2, UC-50, BR-M09-03).
4. Các lệnh trên ca đã công bố do feature này sở hữu: Chuyển người phụ trách ca, Bổ sung nhân viên vào ca, Ghi nhận vắng ca, Hủy ca.
5. Giao Trưởng tầng phụ trách tầng/khu vực (13.3; phân công không gắn ca).
6. Phân công trong ca theo tầng, phòng, người cao tuổi, loại công việc; nhân viên chính và nhân viên hỗ trợ; kiểm tra giấy phép/đào tạo; sao chép phân công (13.4, UC-51, BR-M09-01, DBR-21).
7. Tỷ lệ phục vụ theo trọng số chăm sóc và cảnh báo ca dưới ngưỡng phục vụ (mục 4, BR-M09-02, CFG-M09-01).
8. Bàn giao ca: bản nháp tự lập, hoàn tất, có ý kiến, xác nhận, chuyển trách nhiệm, nhắc, đóng ca, đính chính (13.5, UC-52, UC-53, BR-M09-06 → 08, DBR-20).

**Ngoài phạm vi** (spec này chỉ **cung cấp** dữ liệu hoặc **được kích hoạt** bởi feature sở hữu):

- Quy tắc dùng chung (nhóm dữ liệu, lý do bắt buộc, đính chính, nhật ký, tham số, "hoặc toàn bộ, hoặc không", yêu cầu phê duyệt, tự duyệt Q-10): feature 000 — spec này kế thừa, không lặp lại.
- Tài khoản, khóa tài khoản khi nhân viên Nghỉ việc (feature 002 FR-009), kiểm tra quyền, phạm vi dữ liệu suy ra từ phân công (feature 002 FR-028, FR-032), quyền của Người phụ trách ca (feature 002 FR-019a → c), danh mục nghiệp vụ chuyên môn và kiểm tra giấy phép tại lúc thực hiện (feature 002 FR-036 → 038), cảnh báo giấy phép hành nghề sắp hết hạn theo CFG-M06-02 (feature 002 FR-039). Spec này sở hữu **dữ liệu** nhân viên, giấy phép, đào tạo, ca và phân công.
- Cơ cấu khu vực, tầng, phòng, phân bổ giường; chặn ngừng hiệu lực tầng còn phân công (feature 003 FR-005); công việc vệ sinh và yêu cầu vệ sinh Gấp chưa xong (feature 003 FR-049, FR-050).
- Mức chăm sóc của người cao tuổi: feature 001. Tiếp nhận, tạm vắng, trở về: feature 004.
- Sinh công việc, gán người thực hiện theo phân công (feature 005 FR-024), checklist, nhận việc, công việc chung của tầng, danh mục loại công việc có vai trò và chứng chỉ yêu cầu (feature 005 FR-038), công việc chuyển thành công việc chung khi vắng ca hoặc nghỉ việc (feature 005 FR-032), cung cấp công việc chưa đóng cho bản nháp (feature 005 FR-046) và chuyển công việc tồn sang ca mới (feature 005 FR-047).
- Liều thuốc, liều chung của tầng (feature 006 FR-026a), cung cấp liều Trễ, Bỏ lỡ, Từ chối, Mang theo chờ ghi nhận cho bản nháp (feature 006 FR-028).
- Cảnh báo, sự cố, chỉ số vượt ngưỡng; chuyển người phụ trách cảnh báo, sự cố sau khi bàn giao được xác nhận (feature 007 FR-037); xử lý khi ca không có Bác sĩ trực (feature 007 FR-047b).
- Kênh gửi và xác nhận đã xem thông báo: feature 009. Spec này xác định **sự kiện** và **người nhận**.
- Sinh lịch ca từ mẫu xoay ca (BR-M09-09), yêu cầu đổi ca (UC-54, BR-M09-10), nghỉ đột xuất có duyệt, yêu cầu phủ tối thiểu theo vai trò và gợi ý người thay (BR-M09-11, CFG-M09-06, CFG-M09-07): feature 015. Lịch Nháp do 015 sinh được công bố và điều chỉnh theo spec này; kết quả đổi ca, nghỉ đột xuất đã duyệt được áp vào ca bằng các kiểm tra FR-013, FR-014, FR-030 (Clarification 2026-09-26, đề xuất Q-77).
- Báo cáo, dashboard (18.2, 18.5): feature 016. Spec này cung cấp dữ liệu.
- Chấm công, tính lương, quản lý nhân sự chuyên sâu (1.2): không thuộc hệ thống.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Bàn giao ca tự lập và ca sau xác nhận (Priority: P1)

Trước khi kết thúc ca, hệ thống tự lập bản nháp bàn giao cho mỗi ca của tầng/khu vực từ dữ liệu đã có: công việc chưa hoàn thành, liều thuốc Trễ, Bỏ lỡ, Từ chối, chỉ số vượt ngưỡng trong ca, cảnh báo và sự cố đang mở, người mới nhập, trở về hoặc tạm vắng trong ca. Người phụ trách ca chỉ bổ sung ghi chú, nhận định và vấn đề cần theo dõi rồi hoàn tất. Người phụ trách ca sau đọc, xác nhận hoặc gửi ý kiến khi thấy thiếu sót; khi xác nhận, công việc tồn chuyển sang checklist ca mới và trách nhiệm với cảnh báo đang mở chuyển sang ca mới.

**Why this priority**: Bàn giao hiện còn làm bằng giấy (mục 21); bỏ sót một liều Bỏ lỡ hay một cảnh báo đang mở khi đổi ca là rủi ro trực tiếp cho người cao tuổi. Nguyên tắc 1.3 yêu cầu công việc chưa hoàn thành và cảnh báo chưa xử lý phải được đưa vào bàn giao.

**Independent Test**: Cho ca ngày tầng 2 (07:00–18:00) có một công việc Quá hạn, một liều Bỏ lỡ, một chỉ số vượt ngưỡng, một cảnh báo đang mở và một người trở về trong ca; tới 17:30 kiểm tra bản nháp có đủ năm mục; người phụ trách ca hoàn tất; người phụ trách ca đêm xác nhận; kiểm tra công việc tồn xuất hiện trong checklist ca đêm, cảnh báo đổi người phụ trách và bàn giao không còn sửa được.

**Acceptance Scenarios**:

1. **Given** ca ngày tầng 2 kết thúc 18:00, người phụ trách ca là điều dưỡng D, **When** tới 17:30 (CFG-M09-04, mặc định \[30 phút\] trước kết ca), **Then** hệ thống tạo bản nháp bàn giao gồm: công việc chưa hoàn thành (feature 005 FR-046), liều Trễ, Bỏ lỡ, Từ chối và liều Mang theo chờ ghi nhận (feature 006 FR-028), chỉ số vượt ngưỡng trong ca, cảnh báo và sự cố đang mở (feature 007 FR-037), người mới nhập, trở về, tạm vắng trong ca (feature 004), yêu cầu vệ sinh Gấp chưa xong (feature 003 FR-049); mỗi mục gắn người cao tuổi hoặc phòng và liên kết tới bản ghi nguồn; D được thông báo (BR-M09-06).
2. **Given** bản nháp đã tạo lúc 17:30, **When** lúc 17:40 một công việc trong bản nháp được Hoàn thành và một cảnh báo mới được tạo, **Then** bản nháp tự cập nhật: công việc đã hoàn thành rời khỏi danh sách tồn, cảnh báo mới được thêm; ghi chú D đã viết cho các mục còn lại được giữ; **When** cảnh báo gắn với chỉ số vượt ngưỡng của A được đóng lúc 17:45, **Then** cảnh báo rời danh sách nhưng mục chỉ số vượt ngưỡng của A vẫn được giữ (FR-040, Q-88).
3. **Given** bản nháp, **When** D cố xóa một mục do hệ thống lập, **Then** hệ thống từ chối; D chỉ thêm được ghi chú cho từng mục, nhận định theo người cao tuổi, vấn đề cần theo dõi và nhận định chung (BR-M09-06).
4. **Given** D đã nhập nhận định chung và ghi chú cho mọi mục nghiêm trọng trừ liều Bỏ lỡ của B, **When** D chọn "Hoàn tất bàn giao", **Then** hệ thống từ chối và chỉ ra liều Bỏ lỡ của B còn thiếu ghi chú; công việc Thường chưa có ghi chú không bị chặn (FR-041, Q-83); **Given** D bổ sung ghi chú đó, **When** D chọn "Hoàn tất bàn giao" lúc 17:50, **Then** bàn giao chuyển Đã lập, nội dung tại thời điểm đó được chốt; ca chuyển Đã đóng khi tới 18:00.
5. **Given** bàn giao Đã lập lúc 17:50, **When** lúc 17:55 một cảnh báo mới được tạo cho người cao tuổi A ở tầng 2, **Then** hệ thống ghi thêm cảnh báo vào phần "Phát sinh sau khi lập" của bàn giao, không sửa phần đã chốt, và phần này hiển thị nổi bật cho người xác nhận.
6. **Given** ca ngày tầng 2 tới 18:00 mà bàn giao vẫn ở Bản nháp, **When** tới 18:00, **Then** ca chuyển Chờ bàn giao, không đóng được; D và Trưởng tầng T được nhắc (BR-M09-07, DBR-20).
7. **Given** bàn giao Đã lập, người phụ trách ca đêm là điều dưỡng E, **When** E chọn "Xác nhận", **Then** bàn giao chuyển Đã xác nhận; trong cùng một lần: công việc chưa đóng chuyển sang checklist ca đêm theo feature 005 FR-047, người phụ trách cảnh báo và sự cố đang mở chuyển theo feature 007 FR-037, và nhật ký ghi người xác nhận, thời điểm (BR-M09-08).
8. **Given** bàn giao Đã lập, **When** E chọn "Gửi ý kiến" với nội dung "thiếu tình trạng của B sau ngã chiều nay", **Then** bàn giao chuyển Có ý kiến, D và T được thông báo; trách nhiệm chưa chuyển; nếu lúc đó chưa tới 18:00 thì tới 18:00 ca ngày vào Chờ bàn giao thay vì Đã đóng (Q-87); **When** D bổ sung nhận định cho B và gửi lại, **Then** bàn giao trở lại Đã lập với ý kiến và phần bổ sung được lưu kèm; **When** E xác nhận, **Then** bàn giao Đã xác nhận.
9. **Given** ca đêm bắt đầu 18:00 và bàn giao chưa Đã xác nhận, **When** tới 18:30 (CFG-M09-05, mặc định \[30 phút\] từ đầu ca), **Then** Trưởng tầng T được nhắc (BR-M09-07).
10. **Given** bàn giao Đã xác nhận, **When** bất kỳ ai cố sửa hay xóa nội dung, **Then** hệ thống từ chối; sai sót chỉ được xử lý bằng bản đính chính trỏ về bàn giao gốc (BR-M09-08, BR-M15-03, DBR-20).
11. **Given** ca ngày tầng 2 đã có một bàn giao, **When** hệ thống chạy lại bước lập bản nháp, **Then** không có bàn giao thứ hai cho cùng ca (DBR-20).
12. **Given** bàn giao ca ngày đang Có ý kiến, cảnh báo Trung bình của A có người phụ trách là D (ca ngày, không có ca đêm), **When** tới 18:00 ca đêm bắt đầu, **Then** cảnh báo chuyển tạm sang E (người phụ trách ca đêm) với dấu "tạm nhận, chờ xác nhận bàn giao", trạng thái và hạn tiếp nhận giữ nguyên, E được thông báo; công việc xoay trở Quá hạn của A do S (ca ngày) giữ thành công việc chung của tầng với dấu "tồn, chờ xác nhận bàn giao" và nhân viên ca đêm M nhận được (Q-85); **When** E xác nhận bàn giao, **Then** cảnh báo chuyển chính thức theo feature 007 FR-037 và dấu "tạm nhận" được gỡ (FR-044a, Q-78). Nếu ca đêm bắt đầu mà không có người phụ trách (E vắng, chưa chỉ định người mới), Trưởng tầng T tạm nhận thay E với cùng dấu và thông báo.

---

### User Story 2 - Lập, công bố lịch ca và chỉ định người phụ trách ca (Priority: P1)

Quản lý viện cấu hình các mẫu ca (ví dụ Ca ngày 07:00–18:00, Ca đêm 18:00–07:00 hôm sau). Trưởng tầng lập lịch ca tháng cho tầng mình ở trạng thái Nháp: tạo ca theo mẫu, xếp nhân viên, chỉ định người phụ trách ca. Hệ thống chặn xếp một nhân viên vào hai ca chồng giờ, cảnh báo khi giờ làm liên tục vượt ngưỡng, và không cho công bố khi còn ca thiếu người phụ trách. Sau khi công bố, nhân viên xem được lịch của mình; thay đổi chỉ qua lệnh có lý do.

**Why this priority**: Ca và người phụ trách ca là nền cho phạm vi dữ liệu (feature 002 FR-032), người nhận nhắc việc, công việc chung của tầng và bàn giao; không có ca đã công bố thì các feature 005, 006, 007 không xác định được ai làm gì.

**Independent Test**: Cấu hình hai mẫu ca, lập lịch tháng 10 cho tầng 2, xếp nhân viên sao cho có một trường hợp chồng giờ và một trường hợp liên tục 22 giờ, để trống người phụ trách một ca; kiểm tra lần lượt bị chặn, bị cảnh báo, không công bố được; sửa lại rồi công bố và kiểm tra nhân viên thấy lịch.

**Acceptance Scenarios**:

1. **Given** Quản lý viện tạo mẫu "Ca đêm" 18:00–07:00, **When** lưu, **Then** hệ thống chấp nhận ca qua ngày (giờ kết thúc thuộc ngày hôm sau) và mẫu dùng được cho mọi tầng (13.2).
2. **Given** lịch tháng 10 tầng 2 ở Nháp, S đã có Ca ngày 07:00–18:00 ngày 05/10 ở tầng 2, **When** T xếp S vào ca 12:00–20:00 ngày 05/10 ở tầng 3, **Then** hệ thống chặn và nêu ca bị chồng (BR-M09-03, DBR-21).
3. **Given** S có Ca ngày 07:00–18:00 ngày 05/10, **When** T xếp S vào Ca đêm 18:00–07:00 cùng ngày, **Then** hệ thống cho xếp nhưng cảnh báo "làm liên tục 24 giờ, vượt CFG-M09-02 (mặc định \[16 giờ\])"; T phải xác nhận đã xem cảnh báo, kèm lý do (BR-M09-03).
4. **Given** một ca của lịch Nháp chưa có người phụ trách, **When** T chọn "Công bố lịch", **Then** hệ thống từ chối và liệt kê các ca thiếu người phụ trách (BR-M09-03).
5. **Given** T chỉ định nhân viên chăm sóc S làm người phụ trách ca, **When** lưu, **Then** hệ thống từ chối vì người phụ trách ca phải có vai trò Trưởng tầng hoặc Điều dưỡng và có tên trong ca.
6. **Given** lịch Nháp hợp lệ, **When** T công bố, **Then** lịch chuyển Đã công bố, mỗi nhân viên có ca được thông báo lịch của mình, phân công Nháp chuyển Tương lai hoặc Hiệu lực, và tỷ lệ phục vụ của từng ca được tính lại (BR-M09-02).
7. **Given** lịch đã công bố, **When** T cố xóa S khỏi một ca, **Then** hệ thống từ chối; thay đổi chỉ qua các lệnh Ghi nhận vắng ca, Bổ sung nhân viên vào ca, Chuyển người phụ trách ca, Hủy ca, hoặc yêu cầu đổi ca của feature 015 (13.2).
8. **Given** ca đêm ngày 05/10 đã công bố, người phụ trách là E, **When** T chuyển người phụ trách sang điều dưỡng F cùng ca kèm lý do, **Then** nhiệm vụ Người phụ trách ca chuyển sang F từ thời điểm lệnh; quyền của E theo feature 002 FR-019a mất ngay; nhật ký ghi người trước và sau.
9. **Given** T soạn phân công E làm Điều dưỡng phụ trách trong lịch Nháp tháng 10 khi giấy phép của E còn hiệu lực, **When** lịch chưa công bố, **Then** phân công ở Nháp, E không có phạm vi dữ liệu theo phân công này và không được gán công việc, liều; **When** giấy phép của E hết hạn rồi T công bố, **Then** hệ thống từ chối công bố, liệt kê phân công của E kèm lý do "giấy phép hết hạn"; T loại bỏ phân công đó và công bố được (FR-016, FR-025, Q-82).

---

### User Story 3 - Phân công theo tầng, phòng, người cao tuổi và chặn nhân viên thiếu chứng chỉ (Priority: P1)

Trưởng tầng phân công nhân viên trong ca: nhân viên chính và nhân viên hỗ trợ theo tầng, phòng, người cao tuổi, có thể giới hạn theo loại công việc. Hệ thống chặn phân công vào nhiệm vụ đòi hỏi giấy phép hoặc đào tạo mà nhân viên không có hoặc đã hết hạn, và báo trước các công việc sẽ không giao được cho nhân viên đó.

**Why this priority**: Phân công quyết định người thực hiện từng công việc và liều (feature 005 FR-024, feature 006) và phạm vi dữ liệu (feature 002 FR-028). Phân một người không có giấy phép vào phát thuốc là vi phạm pháp lý (BR-M09-01, DBR-21).

**Independent Test**: Cho tầng 2 có 25 người cao tuổi ở 8 phòng; phân S chính theo phòng 201–204, phân D làm Điều dưỡng phụ trách theo tầng; cho giấy phép của điều dưỡng E hết hạn hôm qua; kiểm tra E không được phân làm Điều dưỡng phụ trách, người cao tuổi ở phòng 201 có S là nhân viên chăm sóc chính, và phân công theo người cao tuổi thắng phân công theo phòng.

**Acceptance Scenarios**:

1. **Given** giấy phép hành nghề của điều dưỡng E hết hạn ngày 04/10, **When** T phân E làm Điều dưỡng phụ trách người cao tuổi A trong ca ngày 05/10, **Then** hệ thống chặn và nêu giấy phép đã hết hạn (BR-M09-01, DBR-21).
2. **Given** giấy phép của E hết hạn ngày 05/10 (còn hiệu lực tới hết ngày 05/10), **When** T phân E làm Điều dưỡng phụ trách trong Ca đêm 05/10 18:00 – 06/10 07:00, **Then** hệ thống chặn vì giấy phép không còn hiệu lực trong toàn bộ thời gian ca.
3. **Given** loại công việc "thay băng" yêu cầu đào tạo "chăm sóc vết thương" và S không có đào tạo này, **When** T phân S làm nhân viên chăm sóc chính cho A, **Then** hệ thống cho phân công, đồng thời cảnh báo "công việc thay băng của A sẽ không giao cho S" và các công việc đó thành công việc chung của tầng theo feature 005.
4. **Given** S chính theo phòng 201 và M chính theo người cao tuổi A (ở phòng 201), cùng vai trò Nhân viên chăm sóc, **When** hệ thống xác định nhân viên chính của A trong ca, **Then** kết quả là M (đối tượng cụ thể hơn thắng); người khác trong phòng 201 có S.
5. **Given** S đã là nhân viên chăm sóc chính theo người cao tuổi A trong ca ngày, **When** T phân thêm M làm nhân viên chăm sóc chính cho A cùng ca, **Then** hệ thống từ chối; T chỉ được phân M làm nhân viên hỗ trợ hoặc kết thúc phân công của S trước.
6. **Given** S không có tên trong ca ngày tầng 2, **When** T phân S cho phòng 201 trong ca đó, **Then** hệ thống từ chối.
7. **Given** phân công ca ngày thứ Hai đã lập, **When** T chọn "Sao chép phân công" sang ca ngày thứ Ba, **Then** hệ thống sao chép các phân công của nhân viên có tên trong ca thứ Ba, kiểm tra lại giấy phép/đào tạo, và liệt kê các phân công không sao chép được kèm lý do.
8. **Given** người cao tuổi mới được phân bổ giường ở phòng 201 lúc 10:00, **When** hệ thống xác định người phụ trách của người đó, **Then** S (chính theo phòng 201) là nhân viên chăm sóc chính mà không cần phân công lại.
9. **Given** giấy phép của D hết hạn lúc hết ngày 10/10 mà D đã được phân làm Điều dưỡng phụ trách trong các ca 11/10 → 15/10 của lịch đã công bố, **When** hết ngày 10/10, **Then** các phân công đó bị hệ thống kết thúc với lý do "giấy phép hết hạn", liều của người cao tuổi liên quan thành liều chung của tầng (feature 006 FR-026a), và T được thông báo danh sách ca bị ảnh hưởng.

---

### User Story 4 - Hồ sơ nhân viên, giấy phép, đào tạo và cảnh báo hết hạn (Priority: P2)

Quản lý viện lập hồ sơ nhân viên (họ tên, chức danh, chuyên môn, trạng thái làm việc) và ghi giấy phép hành nghề, phạm vi hành nghề, các đào tạo (kể cả đào tạo sơ cứu) cùng ngày hết hạn. Hệ thống cảnh báo trước khi giấy phép hoặc đào tạo bắt buộc hết hạn; gia hạn bằng cách ghi bản mới, lịch sử được giữ. Khi nhân viên nghỉ việc, tài khoản bị khóa ngay và công việc, ca, phân công tương lai của họ được giải phóng.

**Why this priority**: Hồ sơ và chứng chỉ là dữ liệu đầu vào của BR-M09-01 và BR-M06-05; nhưng có thể nhập ban đầu một lần trước khi vận hành, nên xếp sau ba luồng vận hành hằng ngày.

**Independent Test**: Tạo hồ sơ điều dưỡng D với giấy phép hết hạn sau 59 ngày và đào tạo sơ cứu hết hạn sau 61 ngày; kiểm tra cảnh báo giấy phép (feature 002) có mặt và cảnh báo đào tạo xuất hiện sau một ngày; gia hạn đào tạo và kiểm tra bản cũ vẫn xem được; cho D nghỉ việc và kiểm tra tài khoản, ca, phân công.

**Acceptance Scenarios**:

1. **Given** Quản lý viện tạo hồ sơ nhân viên với họ tên, chức danh "Điều dưỡng", chuyên môn, **When** lưu, **Then** hồ sơ ở trạng thái Đang làm việc và có thể liên kết tài khoản (feature 002).
2. **Given** đào tạo sơ cứu của S là loại bắt buộc và hết hạn ngày 30/11, **When** tới ngày 01/10 (CFG-M09-03, mặc định \[60 ngày\] trước), **Then** Quản lý viện và S được cảnh báo; Trưởng tầng của tầng S đang được xếp ca được thông báo (BR-M09-05).
3. **Given** đào tạo sơ cứu của S đã có bản mới hết hạn 30/11/2028, **When** Quản lý viện ghi bản gia hạn, **Then** bản cũ chuyển "Đã thay thế" và vẫn xem được trong lịch sử; cảnh báo sắp hết hạn của bản cũ được đóng; phân công bị chặn trước đó nay thực hiện được mà không cần cấp lại quyền.
4. **Given** giấy phép hành nghề của D, **When** Quản lý viện ghi nhận giấy phép bị thu hồi kèm lý do và ngày hiệu lực, **Then** từ ngày đó giấy phép được coi là không còn hiệu lực cho mọi kiểm tra (BR-M09-01, feature 002 FR-037).
5. **Given** S đang có ca ngày mai và là nhân viên chăm sóc chính cho 4 người, **When** Quản lý viện thực hiện "Cho nghỉ việc" với S kèm lý do, hiệu lực ngay, **Then** trong cùng một lần: S chuyển Nghỉ việc; tài khoản của S chuyển Đã khóa (feature 002 FR-009); S được rút khỏi mọi ca từ thời điểm hiệu lực; phân công của S kết thúc; công việc tương lai của S chuyển thành công việc chung của tầng (feature 005 FR-032); Trưởng tầng liên quan được thông báo (BR-M09-04).
6. **Given** E là người phụ trách ca đêm tối nay, **When** E được cho nghỉ việc, **Then** ca đêm được gắn dấu "thiếu người phụ trách", Trưởng tầng và Quản lý viện được cảnh báo ngay; cho tới khi có người phụ trách mới, Trưởng tầng nhận các nhắc việc dành cho Người phụ trách ca và là người hoàn tất bàn giao của ca đó (FR-004, FR-019, FR-041).
7. **Given** hồ sơ nhân viên đã được ca, phân công, bàn giao tham chiếu, **When** Quản lý viện cố xóa hồ sơ, **Then** hệ thống từ chối; hồ sơ chỉ chuyển Nghỉ việc (1.5).

---

### User Story 5 - Cảnh báo tỷ lệ phục vụ theo trọng số chăm sóc (Priority: P2)

Khi lập hoặc thay đổi ca, hệ thống tính tỷ lệ phục vụ của từng ca theo trọng số mức chăm sóc của người cao tuổi dự kiến có mặt, và cảnh báo ca dưới ngưỡng phục vụ cấu hình. Quy tắc chỉ cảnh báo, không chặn.

**Why this priority**: Giúp Trưởng tầng và Quản lý viện thấy khu thiếu người trước khi vào ca (BR-M09-02), nhưng không chặn vận hành; có thể bổ sung sau khi các luồng P1 chạy.

**Independent Test**: Cho tầng 2 có 20 người mức cơ bản (trọng số 1) và 6 người mức đặc biệt (trọng số 2,5), tổng trọng số 35; ngưỡng tối đa 8 trọng số/nhân viên; xếp 4 nhân viên được tính; kiểm tra tỷ lệ 8,75 và cảnh báo; bổ sung một nhân viên và kiểm tra cảnh báo biến mất.

**Acceptance Scenarios**:

1. **Given** ca ngày tầng 2 có tổng trọng số 35 và 4 nhân viên được tính, ngưỡng CFG-M09-01 là 8, **When** T lưu ca, **Then** hệ thống hiển thị tỷ lệ 8,75 và cảnh báo "dưới ngưỡng phục vụ"; T vẫn công bố được lịch (BR-M09-02).
2. **Given** ca đã công bố đang đạt ngưỡng, **When** một nhân viên được ghi nhận vắng ca làm tỷ lệ vượt ngưỡng, **Then** Trưởng tầng được cảnh báo ngay.
3. **Given** người cao tuổi A của tầng 2 Tạm vắng trong suốt ca đêm, **When** hệ thống tính tỷ lệ phục vụ ca đêm, **Then** trọng số của A không được cộng.
4. **Given** mức chăm sóc của A đổi từ cơ bản sang đặc biệt, **When** thay đổi có hiệu lực, **Then** tỷ lệ phục vụ của các ca tương lai có A được tính lại và cảnh báo phát sinh nếu vượt ngưỡng.

---

### User Story 6 - Điều chỉnh ca đã công bố: vắng ca, bổ sung nhân viên, giao trưởng tầng (Priority: P3)

Trong vận hành, Trưởng tầng hoặc Người phụ trách ca ghi nhận nhân viên vắng ca; Trưởng tầng bổ sung nhân viên vào ca; Quản lý viện giao Trưởng tầng phụ trách tầng. Mỗi lệnh có lý do, và hệ thống giải phóng công việc, liều, nhiệm vụ của người vắng.

**Why this priority**: Cần cho vận hành thực tế nhưng các trường hợp này ít hơn luồng hằng ngày; đổi ca và nghỉ đột xuất có duyệt thuộc feature 015.

**Independent Test**: Ghi nhận S vắng ca ngày lúc 07:20; kiểm tra công việc của S thành công việc chung, liều của người do S phụ trách thành liều chung, phân công của S kết thúc, tỷ lệ phục vụ được tính lại; bổ sung M vào ca và phân M thay.

**Acceptance Scenarios**:

1. **Given** S có tên trong ca ngày tầng 2 đang diễn ra, **When** người phụ trách ca D ghi nhận S vắng ca kèm lý do, **Then** trong cùng một lần: S được đánh dấu vắng trong ca (vẫn giữ trong lịch sử ca); phân công của S trong ca kết thúc; công việc chưa đóng của S thành công việc chung của tầng (feature 005 FR-032); T được thông báo; tỷ lệ phục vụ được tính lại (BR-M04-13).
2. **Given** D là người phụ trách ca, **When** T ghi nhận D vắng ca, **Then** hệ thống yêu cầu chỉ định người phụ trách ca mới trong cùng lệnh; nếu không có người đủ điều kiện, ca gắn dấu "thiếu người phụ trách" và T nhận các nhắc việc dành cho người phụ trách ca cho tới khi có người mới.
3. **Given** ca ngày đang diễn ra, **When** T bổ sung M vào ca kèm lý do, **Then** hệ thống kiểm tra chồng giờ và giờ làm liên tục như khi lập lịch; M được thông báo và có phạm vi dữ liệu theo ca từ thời điểm bổ sung (feature 002 FR-032).
4. **Given** Quản lý viện giao T phụ trách tầng 2 từ ngày 01/10, **When** lưu, **Then** T có phạm vi tầng 2 suốt thời gian được giao, không phụ thuộc ca (feature 002 FR-032) và nhận thông báo dành cho Trưởng tầng của tầng 2.
5. **Given** ca đêm ngày 20/10 đã công bố, chưa bắt đầu, **When** Quản lý viện hủy ca kèm lý do, **Then** ca chuyển Đã hủy, phân công của ca bị hủy, nhân viên được thông báo; ca đã bắt đầu thì không hủy được.
6. **Given** T đang được giao tầng 2 không thời hạn, **When** Quản lý viện giao U phụ trách tầng 2 từ 10/10 đến 20/10, **Then** hệ thống từ chối vì chồng thời gian; **When** Quản lý viện kết thúc giao của T vào 09/10, giao U từ 10/10 đến 20/10 và giao lại T từ 21/10, kèm lý do, **Then** mỗi thời điểm tầng 2 có đúng một Trưởng tầng (FR-024, Q-84).

---

### Edge Cases

- **Nhân viên báo trước sẽ vắng một ca của tuần sau**: không dùng "Ghi nhận vắng ca" (bị từ chối vì ngoài khoảng giờ bắt đầu − CFG-M15-07); phải lập yêu cầu nghỉ đột xuất có duyệt ở feature 015 (Q-81).
- **Ca ngắn hơn CFG-M09-04**: bản nháp bàn giao được tạo ngay khi ca bắt đầu.
- **Không có ca sau cùng tầng/khu vực** (ví dụ tầng đóng ban đêm): bàn giao được gửi tới ca kế tiếp gần nhất của cùng tầng/khu vực; nếu trong CFG-M09-09 (mặc định \[24 giờ\]) sau kết thúc ca không có ca nào, bàn giao gửi cho Trưởng tầng được giao tầng đó xác nhận.
- **Ca sau không có người phụ trách** (thiếu vì nghỉ việc, vắng): Trưởng tầng của tầng xác nhận thay.
- **Người bàn giao hết phạm vi ca** (quá giờ kết thúc + CFG-M15-07, mặc định \[2 giờ\]) mà bàn giao còn Bản nháp hoặc Có ý kiến: chỉ Trưởng tầng của tầng hoàn tất hoặc bổ sung được.
- **Ca sau chưa xác nhận, hoặc bàn giao đang Có ý kiến kéo dài**: từ giờ bắt đầu ca sau, Người phụ trách ca sau tạm nhận cảnh báo, sự cố đang mở mà người phụ trách thuộc ca trước và không còn ca (FR-044a); công việc chưa đóng và liều Mang theo chờ ghi nhận của người ca trước tạm thành công việc chung, liều chung của tầng (FR-044a, Q-85); Trưởng tầng được nhắc theo CFG-M09-05 (FR-043).
- **Ca sau không có người phụ trách lúc bắt đầu**: Trưởng tầng của phạm vi tạm nhận thay (FR-044a).
- **Trưởng tầng nghỉ việc giữa tháng, tầng chưa có người thay**: Quản lý viện được nhắc ngay để giao Trưởng tầng tạm; trong lúc chờ, Người phụ trách ca đang diễn ra của tầng thực hiện các lệnh bàn giao và tạm nhận dành cho Trưởng tầng, trong phạm vi ca của mình (FR-024, Q-90); lịch tháng sau của tầng không công bố được khi chưa giao Trưởng tầng (FR-016).
- **Mục trong bản nháp được xử lý sau khi Đã lập** (ví dụ liều Bỏ lỡ được đính chính Đã dùng): phần đã chốt giữ nguyên; lúc xác nhận, hệ thống hiển thị trạng thái hiện tại bên cạnh trạng thái lúc lập; việc chuyển trách nhiệm dùng trạng thái hiện tại.
- **Ca không yêu cầu bàn giao** (ca phạm vi toàn viện như Bác sĩ trực, bếp): ca đóng khi tới giờ kết thúc, không có bàn giao.
- **Nhân viên có hai vai trò** (ví dụ Trưởng tầng kiêm Điều dưỡng): được tính một lần trong tỷ lệ phục vụ; điều kiện người phụ trách ca xét theo bất kỳ vai trò nào.
- **Nhân viên được phân công ở tầng khác tầng của ca**: bị chặn; phạm vi phân công phải nằm trong phạm vi của ca.
- **Giấy phép không có ngày hết hạn** (loại được khai báo không thời hạn): luôn còn hiệu lực cho tới khi bị thu hồi hoặc thay thế.
- **Sửa sai ngày hết hạn đã nhập**: sửa được có lý do, ghi nhật ký; kiểm tra từ lúc sửa dùng giá trị mới; phân công và thao tác đã xảy ra không bị xét lại.
- **Nhân viên Nghỉ việc được tuyển lại**: hồ sơ trở lại Đang làm việc bằng lệnh "Nhận lại làm việc"; tài khoản không tự mở (feature 002).
- **Nghỉ việc có ngày hiệu lực tương lai**: tác động xảy ra tại thời điểm hiệu lực; trước đó nhân viên vẫn xếp ca bình thường, nhưng không xếp được vào ca bắt đầu sau thời điểm hiệu lực.
- **Người cao tuổi chuyển giường sang tầng khác giữa ca**: nhân viên chính được xác định lại theo phân công của tầng mới (feature 005 FR-032); bản nháp bàn giao của tầng cũ giữ các mục đã phát sinh ở tầng cũ.
- **Người cao tuổi chuyển trạng thái cuối giữa ca** (Kết thúc lưu trú, Qua đời, Hủy tiếp nhận): phân công theo người cao tuổi đó kết thúc tại thời điểm chuyển; trọng số của người đó thôi được tính cho phần còn lại của ca và các ca sau (FR-035); các mục bàn giao đã phát sinh trong ca vẫn được giữ, cùng biến động "trạng thái cuối" như mục loại (e).
- **Hai lần chạy tính tỷ lệ cùng lúc** (ví dụ vắng ca và đổi mức chăm sóc đồng thời): kết quả cuối phản ánh cả hai thay đổi; không phát cảnh báo trùng cho cùng ca và cùng mức vượt.

## Requirements *(mandatory)*

### Functional Requirements

Mọi yêu cầu dưới đây kế thừa feature 000: nhóm dữ liệu (mục 1.5), lý do bắt buộc cho lệnh nhóm 2, đính chính cho bản ghi đã xác nhận, nhật ký cho mọi thay đổi, tham số theo mã CFG, "hoặc toàn bộ, hoặc không" khi một lệnh có nhiều tác động, mốc thời gian theo Asia/Ho_Chi_Minh (DBR-25), danh sách bản ghi bảo vệ (BR-M15-03). Phân nhóm dữ liệu của feature này:

| Dữ liệu | Nhóm (1.5) | Thao tác được phép |
| --- | --- | --- |
| Hồ sơ nhân viên (thông tin mô tả) | 1 – Danh mục | Tạo, sửa (Quản lý viện); không xóa, chỉ Nghỉ việc |
| Trạng thái làm việc của nhân viên | 2 | Chỉ qua lệnh ở bảng FR-003 |
| Danh mục loại giấy phép/đào tạo | 1 – Danh mục | Tạo, sửa, Ngừng hiệu lực (Quản lý viện) |
| Giấy phép, đào tạo của nhân viên | 1 – Danh mục có lịch sử | Thêm bản mới, gia hạn bằng bản thay thế, ghi nhận thu hồi, sửa sai có lý do (FR-006); không xóa |
| Mẫu ca | 1 – Danh mục | Tạo, sửa, Ngừng hiệu lực (Quản lý viện); sửa không ảnh hưởng ca đã tạo |
| Lịch ca, ca, nhân viên trong ca, người phụ trách ca | 2 | Tự do khi lịch ở Nháp; sau công bố chỉ qua lệnh ở FR-017 → FR-021 |
| Giao trưởng tầng, phân công trong ca | 2 | Chỉ qua lệnh ở bảng FR-027 |
| Bàn giao | 3 từ khi Đã lập | Bản nháp: bổ sung ghi chú; Đã lập, Có ý kiến: chỉ ghi thêm; Đã xác nhận: chỉ đính chính (bảng FR-042) |

#### A. Hồ sơ nhân viên, giấy phép và đào tạo

- **FR-001**: Quản lý viện MUST tạo và sửa được hồ sơ nhân viên gồm: họ tên, chức danh, chuyên môn, ngày bắt đầu làm việc, thông tin liên hệ, trạng thái làm việc, tài khoản liên kết (feature 002). Hồ sơ đã được ca, phân công, công việc, bàn giao hay bất kỳ bản ghi nào tham chiếu MUST NOT bị xóa. *(Nguồn: 13.1, UC-49, 1.5)*
- **FR-002**: Vai trò dùng để xếp ca, phân công và tính tỷ lệ phục vụ MUST là vai trò hệ thống của tài khoản liên kết (feature 002); chức danh chỉ mang tính mô tả. Nhân viên MUST chỉ được xếp vào ca khi ở trạng thái Đang làm việc và có tài khoản liên kết không ở trạng thái Đã khóa. *(Nguồn: 2.4, 13.1)*
- **FR-003**: Trạng thái làm việc MUST chỉ thay đổi theo bảng sau:

| Từ | Lệnh | Đến | Người thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Tạo hồ sơ | Đang làm việc | Quản lý viện | Có họ tên, chức danh | — |
| Đang làm việc | Cho nghỉ việc | Nghỉ việc | Quản lý viện | Lý do; thời điểm hiệu lực không sớm hơn hiện tại | Tại thời điểm hiệu lực, trong cùng một lần: FR-004 |
| Nghỉ việc | Nhận lại làm việc | Đang làm việc | Quản lý viện | Lý do | Không tự mở tài khoản (feature 002); lịch sử cũ giữ nguyên |

- **FR-004**: Khi nhân viên chuyển Nghỉ việc, hệ thống MUST trong cùng một lần: (a) yêu cầu feature 002 khóa tài khoản liên kết (feature 002 FR-009); (b) rút nhân viên khỏi mọi ca có thời điểm kết thúc sau thời điểm hiệu lực, giữ dấu "rút do nghỉ việc" trong lịch sử ca; (c) kết thúc mọi phân công và giao trưởng tầng của nhân viên từ thời điểm hiệu lực; (d) yêu cầu feature 005 chuyển công việc chưa đóng và công việc tương lai của nhân viên thành công việc chung của tầng (feature 005 FR-032); (e) với mỗi ca mà nhân viên là người phụ trách, gắn dấu "thiếu người phụ trách" và cảnh báo ngay Trưởng tầng của tầng đó và Quản lý viện; (f) với bàn giao ở Bản nháp hoặc Có ý kiến mà nhân viên là người bàn giao, chuyển quyền hoàn tất cho Trưởng tầng của tầng; (g) với bàn giao ở Đã lập hoặc Có ý kiến mà nhân viên là người xác nhận (người phụ trách ca sau), người xác nhận chuyển theo (e): Trưởng tầng của phạm vi xác nhận hoặc gửi ý kiến cho tới khi có người phụ trách ca mới; (h) tính lại tỷ lệ phục vụ của các ca bị ảnh hưởng (FR-034). *(Nguồn: BR-M09-04, 19.1)*
- **FR-005**: Quản lý viện MUST cấu hình được danh mục loại giấy phép/đào tạo, mỗi loại gồm: tên, nhóm (giấy phép hành nghề / đào tạo), có thời hạn hay không, là loại bắt buộc với vai trò nào (nếu có), có phạm vi hành nghề hay không. Danh mục mặc định MUST có ít nhất: giấy phép hành nghề bác sĩ, giấy phép hành nghề điều dưỡng, đào tạo sơ cứu. Loại giấy phép/đào tạo yêu cầu cho từng loại công việc do danh mục loại công việc khai báo (feature 005 FR-038) và cho từng nghiệp vụ chuyên môn do feature 002 FR-036 khai báo; spec này cung cấp danh mục loại để hai nơi đó tham chiếu. *(Nguồn: 13.1, 13.4)*
- **FR-006**: Quản lý viện MUST ghi được cho nhân viên các giấy phép, đào tạo với: loại, số, cơ quan hoặc đơn vị cấp, phạm vi hành nghề (với giấy phép), ngày cấp, ngày hết hạn (bắt buộc nếu loại có thời hạn), bản scan. Các lệnh MUST gồm: "Thêm"; "Gia hạn" — tạo bản mới trỏ về bản cũ, bản cũ chuyển Đã thay thế; "Ghi nhận thu hồi" — kèm lý do và ngày hiệu lực; "Sửa sai" — kèm lý do, giữ giá trị trước/sau trong nhật ký. Không có thao tác xóa. *(Nguồn: 13.1, BR-M09-01)*
- **FR-007**: Tình trạng hiệu lực của một giấy phép/đào tạo MUST được suy ra như sau: Còn hiệu lực — đã tới ngày cấp, chưa qua hết ngày hết hạn (theo feature 002 FR-038), chưa thu hồi, chưa bị thay thế; Hết hạn — đã qua hết ngày hết hạn; Đã thu hồi — từ ngày hiệu lực thu hồi; Đã thay thế — khi có bản gia hạn. "Nhân viên có giấy phép/đào tạo X còn hiệu lực trong khoảng [t1, t2]" MUST đúng khi có một bản loại X Còn hiệu lực tại mọi thời điểm trong khoảng đó. *(Nguồn: BR-M09-01, BR-M06-05, NFR-09)*
- **FR-008**: Khi giấy phép/đào tạo thuộc loại bắt buộc với vai trò của nhân viên, hoặc được nhiệm vụ của một phân công hiện tại hay tương lai của nhân viên yêu cầu (FR-030), sẽ hết hạn trong vòng CFG-M09-03 (mặc định \[60 ngày\]) mà chưa có bản gia hạn, hệ thống MUST cảnh báo Quản lý viện và nhân viên đó (chỉ nêu loại, ngày hết hạn), và thông báo Trưởng tầng của các tầng mà nhân viên có ca sau ngày hết hạn. Cảnh báo MUST được đóng khi có bản gia hạn. Cảnh báo giấy phép hành nghề dùng CFG-M06-02 và thuộc feature 002 FR-039; spec này cung cấp dữ liệu. *(Nguồn: BR-M09-05, CFG-M09-03, CFG-M06-02)*
- **FR-009**: Khi một giấy phép/đào tạo của nhân viên chuyển Hết hạn hoặc Đã thu hồi mà không có bản thay thế Còn hiệu lực, hệ thống MUST trong cùng một lần: kết thúc mọi phân công hiện tại và tương lai của nhân viên có nhiệm vụ yêu cầu loại đó (FR-030), với lý do "giấy phép/đào tạo hết hiệu lực"; và thông báo Trưởng tầng của các tầng liên quan danh sách ca và người cao tuổi bị ảnh hưởng. Nhân viên vẫn có tên trong ca. *(Nguồn: BR-M09-01, DBR-21)*
- **FR-010**: Khi đào tạo bắt buộc với vai trò của nhân viên không còn hiệu lực, hệ thống MUST vẫn cho xếp ca nhưng MUST cảnh báo người xếp ca, trừ khi loại đó còn được yêu cầu bởi nhiệm vụ phân công (khi đó áp FR-030). *(Nguồn: 13.1, BR-M09-01)*

#### B. Mẫu ca và lịch ca

- **FR-011**: Quản lý viện MUST cấu hình được mẫu ca gồm: tên ca, giờ bắt đầu, giờ kết thúc (được phép qua ngày), loại phạm vi (tầng / khu vực / toàn viện), có yêu cầu bàn giao hay không (mặc định: có với phạm vi tầng hoặc khu vực, không với toàn viện). Giờ ca MUST NOT được cố định trong sản phẩm; cấu hình tham chiếu theo khảo sát là Ca ngày 07:00–18:00 và Ca đêm 18:00–07:00 hôm sau (Q-09). Sửa hoặc Ngừng hiệu lực mẫu MUST NOT làm thay đổi ca đã tạo từ mẫu; mẫu đã Ngừng hiệu lực MUST NOT được dùng để tạo ca mới. *(Nguồn: 13.2, Q-09, 1.5)*
- **FR-012**: Lịch ca MUST được lập theo tháng cho một phạm vi (một tầng, một khu vực, hoặc toàn viện) và có trạng thái Nháp → Đã công bố. Trong lịch Nháp, người lập MUST tạo, sửa, xóa được ca và xếp, gỡ nhân viên tự do; mỗi ca gồm: ngày, mẫu ca (giờ bắt đầu và kết thúc lấy từ mẫu, được chỉnh riêng cho từng ca), phạm vi cụ thể, danh sách nhân viên, người phụ trách ca, Bác sĩ trực (với ca toàn viện có Bác sĩ). Ca kéo qua hai tháng MUST thuộc lịch của tháng chứa giờ bắt đầu ca. *(Nguồn: 13.2, UC-50)*
- **FR-013**: Hệ thống MUST chặn xếp một nhân viên vào hai ca có khoảng thời gian chồng nhau, bất kể phạm vi, ở mọi trạng thái lịch và mọi lệnh thêm nhân viên vào ca. Hai ca chỉ tiếp giáp (ca sau bắt đầu đúng lúc ca trước kết thúc) không bị coi là chồng. *(Nguồn: BR-M09-03, DBR-21)*
- **FR-014**: Hệ thống MUST cảnh báo khi xếp một nhân viên làm cho chuỗi ca liên tục của họ vượt CFG-M09-02 (mặc định \[16 giờ\]); chuỗi liên tục là các ca tiếp giáp nhau không có khoảng nghỉ. Người xếp MUST xác nhận đã xem cảnh báo kèm lý do để tiếp tục; hệ thống MUST NOT chặn. *(Nguồn: BR-M09-03, CFG-M09-02)*
- **FR-015**: Người phụ trách ca MUST là nhân viên có tên trong ca và có vai trò Trưởng tầng hoặc Điều dưỡng. Mỗi ca thuộc mẫu có yêu cầu bàn giao MUST có đúng một người phụ trách tại mỗi thời điểm, trừ khi đang gắn dấu "thiếu người phụ trách" (FR-004, FR-019). *(Nguồn: BR-M09-03, 2.4, 4.4 dòng "Bàn giao ca")*
- **FR-016**: Lệnh "Công bố lịch" MUST do Trưởng tầng được giao tầng/khu vực của lịch hoặc Quản lý viện thực hiện, và MUST bị từ chối khi còn ca có yêu cầu bàn giao mà thiếu người phụ trách, hoặc khi tầng/khu vực của lịch chưa có Trưởng tầng được giao cho toàn bộ tháng của lịch (FR-024, Q-86), hoặc khi còn phân công Nháp không đạt kiểm tra lại FR-028, FR-030 tại thời điểm công bố (ví dụ giấy phép đã hết hạn sau khi soạn); hệ thống MUST liệt kê các phân công này kèm lý do và cho người công bố loại bỏ chúng trong một thao tác. Trước khi công bố, hệ thống MUST hiển thị tổng hợp các cảnh báo không chặn: giờ làm liên tục (FR-014), tỷ lệ phục vụ dưới ngưỡng (FR-034), đào tạo bắt buộc hết hiệu lực (FR-010). Khi công bố, mỗi nhân viên có ca MUST được thông báo lịch của mình. Người lập lịch tự công bố MUST được chấp nhận theo Q-10: bắt buộc lý do và nhật ký đánh dấu "tự duyệt". Lịch phạm vi toàn viện MUST chỉ do Quản lý viện lập và công bố. *(Nguồn: 13.2, BR-M09-09 phần duyệt công bố, 17, Q-10)*
- **FR-017**: Sau khi công bố, ca và danh sách nhân viên MUST NOT được sửa trực tiếp; chỉ thay đổi qua các lệnh FR-018 → FR-021 của spec này hoặc yêu cầu đổi ca, nghỉ đột xuất của feature 015. Mọi lệnh MUST có lý do và thông báo cho nhân viên bị ảnh hưởng. *(Nguồn: 13.2)*
- **FR-018**: Trưởng tầng (tầng của ca) hoặc Quản lý viện MUST thực hiện được "Bổ sung nhân viên vào ca" với ca Đã công bố hoặc Đang diễn ra, áp FR-002, FR-013, FR-014. *(Nguồn: 13.2)*
- **FR-019**: Trưởng tầng (tầng của ca) hoặc Quản lý viện MUST thực hiện được "Chuyển người phụ trách ca" sang nhân viên thỏa FR-015; nhiệm vụ và quyền theo nhiệm vụ (feature 002 FR-019a → d) MUST chuyển từ thời điểm lệnh. Khi ca đang gắn dấu "thiếu người phụ trách", Trưởng tầng của tầng MUST nhận các nhắc việc và thông báo dành cho Người phụ trách ca cho tới khi có người mới. *(Nguồn: 2.4, BR-M09-03)*
- **FR-020**: Trưởng tầng (tầng của ca) hoặc Người phụ trách ca (chỉ với ca mình đang phụ trách, trong khoảng phạm vi của ca theo feature 002 FR-032; quyền mất khi nhiệm vụ chuyển cho người khác; nhật ký ghi căn cứ là nhiệm vụ Người phụ trách ca) MUST thực hiện được "Ghi nhận vắng ca" với nhân viên có tên trong ca Đã công bố hoặc Đang diễn ra, chỉ trong khoảng từ giờ bắt đầu ca trừ CFG-M15-07 (mặc định \[2 giờ\]) tới giờ kết thúc ca; ngoài khoảng này lệnh MUST bị từ chối và người dùng được hướng sang yêu cầu nghỉ đột xuất của feature 015. Lệnh MUST chỉ tác động lên ca được ghi nhận; ca, phân công và công việc của nhân viên ở các ca khác MUST giữ nguyên (Q-91). Trong cùng một lần, hệ thống MUST: đánh dấu nhân viên vắng (giữ trong lịch sử ca); kết thúc phân công của họ trong ca; yêu cầu feature 005 chuyển công việc chưa đóng của họ trong ca đó thành công việc chung (feature 005 FR-032); nếu họ là người phụ trách ca thì yêu cầu chỉ định người mới trong cùng lệnh, không có người đủ điều kiện thì gắn dấu "thiếu người phụ trách"; thông báo Trưởng tầng; tính lại tỷ lệ phục vụ. *(Nguồn: BR-M04-13, 8.4, 2.4, 13.2; Clarification 2026-09-26, đề xuất Q-77, Q-80, Q-81)*
- **FR-021**: Quản lý viện MUST thực hiện được "Hủy ca" với ca Đã công bố chưa bắt đầu, kèm lý do; phân công của ca MUST bị hủy và nhân viên được thông báo. Ca đã bắt đầu MUST NOT bị hủy. *(Nguồn: 13.2)*
- **FR-022**: Trạng thái ca MUST chỉ thay đổi theo bảng sau:

| Từ | Sự kiện / lệnh | Đến | Người thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Tạo ca trong lịch Nháp | Nháp | Trưởng tầng, Quản lý viện | Lịch ở Nháp | — |
| Nháp | Công bố lịch | Đã công bố | Trưởng tầng, Quản lý viện | FR-016 | Thông báo nhân viên |
| Đã công bố | Tới giờ bắt đầu | Đang diễn ra | Hệ thống | — | Phạm vi dữ liệu theo ca (feature 002 FR-032) |
| Đã công bố | Hủy ca | Đã hủy | Quản lý viện | Chưa tới giờ bắt đầu; lý do | Hủy phân công; thông báo |
| Đang diễn ra | Tới giờ kết thúc | Đã đóng | Hệ thống | Mẫu ca không yêu cầu bàn giao, hoặc bàn giao đã ở Đã lập / Đã xác nhận | — |
| Đang diễn ra | Tới giờ kết thúc | Chờ bàn giao | Hệ thống | Bàn giao còn Bản nháp hoặc Có ý kiến | Nhắc người phụ trách ca và Trưởng tầng |
| Chờ bàn giao | Bàn giao chuyển Đã lập (hoàn tất, hoặc bổ sung và gửi lại) | Đã đóng | Hệ thống | — | — |

  Ca MUST NOT chuyển Đã đóng khi chưa lập xong bàn giao; bàn giao ở Có ý kiến được coi là chưa lập xong (BR-M09-07, DBR-20). Khi bàn giao chuyển Có ý kiến sau khi ca đã Đã đóng, ca MUST giữ Đã đóng, và Trưởng tầng MUST được nhắc cùng người bàn giao. *(Nguồn: 13.2, 13.5, BR-M09-07; Clarification 2026-09-26, đề xuất Q-87)*

- **FR-023**: Bác sĩ trực tại một thời điểm MUST là Bác sĩ có tên trong một ca phạm vi toàn viện đang diễn ra tại thời điểm đó, được đánh dấu "Bác sĩ trực"; một thời điểm MAY có nhiều Bác sĩ trực, khi đó mọi người đều nhận thông báo dành cho Bác sĩ trực. Spec này cung cấp danh sách Bác sĩ trực theo thời điểm cho feature 007 và 009; trường hợp không có Bác sĩ trực do feature 007 FR-047b xử lý. *(Nguồn: 2.4)*

#### C. Giao trưởng tầng và phân công

- **FR-024**: Quản lý viện MUST giao được một nhân viên có vai trò Trưởng tầng phụ trách một hoặc nhiều tầng/khu vực, với ngày bắt đầu và (tùy chọn) ngày kết thúc. Giao trưởng tầng là phân công không gắn ca, tạo phạm vi trong suốt thời gian hiệu lực (feature 002 FR-032) và xác định người nhận thông báo "Trưởng tầng" của tầng đó (BR-M13-04). Mỗi tầng/khu vực MUST có tối đa một giao trưởng tầng hiệu lực tại một thời điểm; hệ thống MUST từ chối giao có khoảng thời gian chồng với giao đang có của cùng tầng. Thay tạm MUST thực hiện bằng cách kết thúc (hoặc rút ngắn) giao hiện tại và lập giao có thời hạn cho người thay, kèm lý do. Khi một tầng không có Trưởng tầng được giao (ví dụ Trưởng tầng nghỉ việc giữa kỳ), hệ thống MUST nhắc ngay Quản lý viện giao Trưởng tầng tạm (giao có thời hạn theo đoạn trên), nhắc lại mỗi đầu ca cho tới khi có người được giao; Quản lý viện MUST NOT thực hiện thay các lệnh bàn giao (quyền của Quản lý viện ở dòng "Bàn giao ca" vẫn là xem, 4.4, Q-15). Trong lúc chờ, mọi lệnh và nhắc việc mà spec giao cho "Trưởng tầng của phạm vi" (hoàn tất, bổ sung và gửi lại, xác nhận, gửi ý kiến theo FR-041, FR-042; nhận nhắc thay người phụ trách ca theo FR-019; tạm nhận theo FR-044a) MUST chuyển cho Người phụ trách ca đang diễn ra của tầng đó, chỉ trong khoảng phạm vi ca của họ, nhật ký ghi căn cứ "thay Trưởng tầng – tầng chưa có Trưởng tầng". Nếu tại thời điểm đó tầng cũng không có ca đang diễn ra có người phụ trách, lệnh chờ tới ca kế tiếp có người phụ trách. *(Nguồn: 13.3, 2.3, 4.4, Q-15; Clarification 2026-09-26, đề xuất Q-84, Q-86, Q-90)*
- **FR-025**: Trưởng tầng (tầng của ca, trong phạm vi) và Quản lý viện MUST lập được phân công trong một ca, mỗi phân công gồm: nhân viên, ca, đối tượng (tầng/khu vực, phòng, hoặc người cao tuổi; không có đối tượng "nhóm người cao tuổi", Q-92), loại công việc giới hạn (tùy chọn), vai trò phân công (chính / hỗ trợ), nhiệm vụ đặc biệt (Điều dưỡng phụ trách, nếu có), thời điểm bắt đầu và kết thúc (mặc định là giờ ca). Nhân viên MUST có tên trong ca và không bị đánh dấu vắng; đối tượng MUST nằm trong phạm vi của ca. Phân công lập trên ca của lịch Nháp MUST ở trạng thái Nháp: sửa, xóa tự do như lịch Nháp, MUST NOT tạo phạm vi dữ liệu (feature 002) và MUST NOT được dùng để gán công việc, liều (feature 005, 006) cho tới khi lịch công bố. Khi ca Nháp bị xóa hoặc nhân viên bị gỡ khỏi ca Nháp, các phân công Nháp tương ứng MUST bị xóa cùng. *(Nguồn: 13.4, 8.4, UC-51, 13.2; Clarification 2026-09-26, đề xuất Q-82)*
- **FR-026**: Phân công theo tầng/khu vực hoặc phòng MUST áp tự động cho mọi người cao tuổi đang ở đó tại từng thời điểm (theo phân bổ giường, feature 003), không cần lập lại khi có người mới đến, chuyển đi. Phân công nhân viên vệ sinh MUST theo phòng hoặc tầng/khu vực (feature 003 FR-050). *(Nguồn: 13.4, 8.4)*
- **FR-027**: Trạng thái phân công MUST chỉ thay đổi theo bảng sau:

| Từ | Sự kiện / lệnh | Đến | Người thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Lập phân công trên ca của lịch Nháp | Nháp | Trưởng tầng, Quản lý viện; Hệ thống khi sao chép | FR-025, FR-028, FR-030 | Không có tác động ngoài lịch Nháp |
| Nháp | Công bố lịch | Tương lai hoặc Hiệu lực | Trưởng tầng, Quản lý viện | Kiểm tra lại FR-028, FR-030 đạt (FR-016) | Cập nhật người thực hiện của công việc, liều (feature 005, 006); phạm vi dữ liệu (feature 002) |
| (chưa có) | Lập phân công trên ca đã công bố | Tương lai hoặc Hiệu lực | Trưởng tầng, Quản lý viện; Hệ thống khi sao chép | FR-025, FR-028, FR-030 | Cập nhật người thực hiện của công việc, liều (feature 005, 006) |
| Tương lai | Tới thời điểm bắt đầu | Hiệu lực | Hệ thống | — | Phạm vi dữ liệu (feature 002) |
| Tương lai | Hủy phân công | Đã hủy | Trưởng tầng, Quản lý viện, Hệ thống (FR-004, FR-009, FR-021, và FR-020 khi Trưởng tầng hoặc Người phụ trách ca ghi nhận vắng ca trước giờ bắt đầu ca) | Lý do | — |
| Hiệu lực | Kết thúc phân công | Đã kết thúc | Trưởng tầng, Người phụ trách ca (trong ca), Quản lý viện, Hệ thống (FR-004, FR-009, FR-020) | Lý do | Công việc, liều chưa đóng được gán lại theo phân công còn lại hoặc thành chung (feature 005, 006) |
| Hiệu lực | Tới thời điểm kết thúc | Đã kết thúc | Hệ thống | — | — |

  Đổi nhân viên của một phân công MUST được thực hiện bằng kết thúc phân công cũ và lập phân công mới. *(Nguồn: 13.4, 1.5)*

- **FR-028**: Với mỗi người cao tuổi, mỗi vai trò và mỗi thời điểm trong ca, MUST có tối đa một nhân viên chính được xác định như sau: phân công theo người cao tuổi thắng theo phòng, theo phòng thắng theo tầng/khu vực; cùng mức đối tượng, phân công có giới hạn loại công việc thắng phân công không giới hạn đối với loại công việc đó. Hệ thống MUST từ chối lập hai phân công chính cùng mức đối tượng, cùng vai trò, cùng loại công việc giới hạn cho cùng một đối tượng có khoảng thời gian chồng nhau. Phân công hỗ trợ không giới hạn số lượng. Khi hai lệnh lập phân công vi phạm quy tắc này được gửi gần như đồng thời, lệnh được ghi nhận sau MUST bị từ chối. Kết quả xác định này là căn cứ cho người thực hiện công việc (feature 005 FR-024) và Điều dưỡng phụ trách (2.4). *(Nguồn: 13.4, 8.4, 2.4)*
- **FR-029**: Người cao tuổi đang có mặt trong phạm vi ca mà không có Điều dưỡng phụ trách hoặc không có nhân viên chăm sóc chính tại giờ bắt đầu ca MUST được liệt kê cho Trưởng tầng và người phụ trách ca từ thời điểm công bố lịch và nhắc lại tại giờ bắt đầu ca; công việc, liều của họ là công việc chung, liều chung của tầng (feature 005, feature 006 FR-026a). *(Nguồn: 8.4, BR-M04-13)*
- **FR-030**: Hệ thống MUST chặn lập phân công khi nhiệm vụ của phân công yêu cầu một loại giấy phép/đào tạo mà nhân viên không có bản còn hiệu lực trong toàn bộ khoảng thời gian phân công (FR-007). Nhiệm vụ yêu cầu gồm: Điều dưỡng phụ trách — các loại mà feature 002 FR-036 khai báo cho "phát thuốc và xác nhận liều"; phân công có giới hạn loại công việc — các loại mà loại công việc đó khai báo (feature 005 FR-038). *(Nguồn: BR-M09-01, 13.4, DBR-21)*
- **FR-031**: Với phân công chính không giới hạn loại công việc, hệ thống MUST cho lập phân công nhưng MUST liệt kê trước khi lưu các loại công việc của đối tượng mà nhân viên không đủ giấy phép/đào tạo; các công việc đó MUST NOT được giao cho nhân viên này và thành công việc chung của tầng (feature 005 FR-024, FR-031). *(Nguồn: BR-M09-01, 13.4)*
- **FR-032**: Trưởng tầng MUST thực hiện được "Sao chép phân công" từ một ca nguồn sang một ca đích cùng phạm vi; hệ thống MUST chỉ sao chép phân công của nhân viên có tên trong ca đích và không vắng, MUST kiểm tra lại FR-028, FR-030 cho ca đích, và MUST liệt kê các phân công không sao chép được kèm lý do. *(Nguồn: 13.4, 1.3 "hệ thống chủ động sinh… nhân viên chủ yếu xác nhận")*
- **FR-033**: Mỗi nhân viên MUST xem được ca và phân công của chính mình (quyền "P" ở dòng "Lịch ca, phân công"); Trưởng tầng xem và lập được ca, phân công của tầng được giao; Quản lý viện xem và lập được toàn viện. *(Nguồn: 4.4, UC-50, UC-51)*

#### D. Tỷ lệ phục vụ

- **FR-034**: Với mỗi ca phạm vi tầng/khu vực, hệ thống MUST tính tỷ lệ phục vụ = tổng trọng số chăm sóc của người cao tuổi dự kiến có mặt trong ca ÷ số nhân viên được tính có tên trong ca và không vắng, trong đó trọng số theo mức chăm sóc hiện hành của từng người (mục 4, CFG-M09-01). Người dự kiến có mặt MUST gồm người nội trú có phân bổ giường trong phạm vi và không vắng suốt ca (Tạm vắng, Điều trị tại bệnh viện, Hoạt động bên ngoài), và người bán trú có ngày có mặt trùng ca (3.4); người vắng một phần ca được tính đủ trọng số. Nhân viên được tính MUST là nhân viên có vai trò Điều dưỡng hoặc Nhân viên chăm sóc (mỗi người tính một lần dù có nhiều vai trò); Trưởng tầng không kiêm một trong hai vai trò này không được tính. Ngưỡng MUST là trọng số tối đa trên mỗi nhân viên, cấu hình riêng cho từng mẫu ca trong CFG-M09-01 (mặc định: theo cơ sở); ca có tỷ lệ lớn hơn ngưỡng là "dưới ngưỡng phục vụ". Ca không có nhân viên được tính mà có người cao tuổi dự kiến có mặt MUST được coi là dưới ngưỡng. *(Nguồn: BR-M09-02, mục 4, CFG-M09-01; Clarification 2026-09-26, đề xuất Q-79)*
- **FR-035**: Tỷ lệ phục vụ MUST được tính lại khi: lưu ca trong lịch Nháp; công bố lịch; ghi nhận vắng ca, bổ sung nhân viên, nghỉ việc; thay đổi mức chăm sóc có hiệu lực (feature 001); phân bổ giường, tạm vắng, trở về làm đổi người dự kiến có mặt (feature 003, 004). *(Nguồn: BR-M09-02)*
- **FR-036**: Khi tỷ lệ phục vụ của một ca vượt ngưỡng (dưới ngưỡng phục vụ), hệ thống MUST hiển thị cảnh báo cho người lập lịch khi lịch còn Nháp, và MUST cảnh báo Trưởng tầng của tầng (và Quản lý viện khi ca bắt đầu trong vòng CFG-M09-08, đề xuất, mặc định \[24 giờ\]) khi lịch đã công bố. Cảnh báo MUST NOT chặn lưu hay công bố. Một ca MUST NOT nhận hai cảnh báo liên tiếp cho cùng tình trạng vượt ngưỡng; cảnh báo mới chỉ phát khi ca trở lại đạt ngưỡng rồi vượt lại. *(Nguồn: BR-M09-02; Clarification 2026-09-26, đề xuất Q-89, CFG-M09-08)*
- **FR-037**: Tỷ lệ phục vụ, số nhân viên được tính, tổng trọng số và tình trạng đạt/không đạt của mỗi ca MUST được lưu theo lần tính để cung cấp cho báo cáo (feature 016). *(Nguồn: 18.2, 18.5)*

#### E. Bàn giao ca

- **FR-038**: Mỗi ca thuộc mẫu có yêu cầu bàn giao MUST có đúng một bàn giao; "ca sau" của bàn giao là ca có yêu cầu bàn giao cùng phạm vi, có giờ bắt đầu sớm nhất trong các ca bắt đầu sau giờ bắt đầu của ca trước và không muộn hơn giờ kết thúc của ca trước; nếu không có thì là ca cùng phạm vi bắt đầu sớm nhất sau giờ kết thúc, trong vòng CFG-M09-09 (đề xuất, mặc định \[24 giờ\]); nếu vẫn không có thì người xác nhận là Trưởng tầng được giao phạm vi đó. *(Nguồn: 13.5, DBR-20; Clarification 2026-09-26, đề xuất Q-89, CFG-M09-09)*
- **FR-039**: Tại thời điểm giờ kết thúc ca trừ CFG-M09-04 (mặc định \[30 phút\]), hoặc tại giờ bắt đầu nếu ca ngắn hơn CFG-M09-04, hệ thống MUST tự tạo bản nháp bàn giao (người bàn giao là người phụ trách ca) gồm các mục tự lập, mỗi mục có nguồn, người cao tuổi hoặc phòng, mô tả, trạng thái, liên kết tới bản ghi nguồn:
  (a) công việc chưa hoàn thành (feature 005 FR-046);
  (b) liều Trễ, Bỏ lỡ, Từ chối trong ca và liều Mang theo chờ ghi nhận (feature 006 FR-028);
  (c) chỉ số vượt ngưỡng cảnh báo hoặc nguy hiểm đo trong ca (feature 007);
  (d) cảnh báo và sự cố đang mở của người cao tuổi trong phạm vi (feature 007 FR-037);
  (e) người cao tuổi mới nhập, trở về hoặc bắt đầu vắng (Tạm vắng, Điều trị tại bệnh viện, Hoạt động bên ngoài) trong ca, và người đang vắng mà đã quá giờ dự kiến trở về (feature 004); người vắng từ trước ca và chưa quá giờ dự kiến trở về MUST NOT được lặp lại ở mỗi bàn giao;
  (f) yêu cầu vệ sinh Gấp chưa hoàn thành (feature 003 FR-049).
  Chạy lại bước tạo MUST NOT tạo bàn giao thứ hai. Nếu mốc tạo bị lỡ do hệ thống gián đoạn, bản nháp MUST được tạo ngay khi hệ thống hoạt động lại; việc ca chuyển Chờ bàn giao tại giờ kết thúc (FR-022) không phụ thuộc bản nháp đã được tạo hay chưa. Người bàn giao MUST được thông báo. *(Nguồn: BR-M09-06, BR-M04-07, 13.5, DBR-20, NFR-04)*
- **FR-040**: Khi bàn giao ở Bản nháp, các mục tự lập MUST được cập nhật theo dữ liệu nguồn (thêm mục mới phát sinh, bỏ mục đã được xử lý xong), giữ nguyên ghi chú đã gắn cho mục còn lại. Mục "đã được xử lý xong" MUST được xác định theo loại: (a) công việc — đã đóng (Hoàn thành, Hoàn thành trễ, Không thực hiện, Hủy); (b) liều — không còn ở Trễ, Bỏ lỡ, Từ chối hay Mang theo chờ ghi nhận nhờ được ghi hoặc đính chính; (d) cảnh báo, sự cố — đã đóng (hoặc Đã hủy với sự cố); (f) yêu cầu vệ sinh Gấp — Hoàn thành. Mục loại (c) chỉ số vượt ngưỡng và (e) biến động người cao tuổi MUST luôn được giữ tới khi chốt (Q-88); mục bị bỏ có ghi chú MUST được giữ ở phần "Đã xử lý trong ca" cùng ghi chú đó. Người bàn giao và Điều dưỡng có tên trong ca MUST chỉ bổ sung được: ghi chú cho từng mục, nhận định theo người cao tuổi (tình trạng người cao tuổi), vấn đề cần theo dõi (mục thêm tay) và nhận định chung; MUST NOT xóa hoặc sửa mục tự lập. *(Nguồn: BR-M09-06, 13.5; Clarification 2026-09-26, đề xuất Q-88)*
- **FR-041**: Lệnh "Hoàn tất bàn giao" MUST do người phụ trách ca thực hiện (hoặc Trưởng tầng của phạm vi khi người phụ trách ca đã ra khỏi phạm vi ca, vắng hay nghỉ việc), yêu cầu có nhận định chung và có ghi chú cho mọi mục nghiêm trọng tại thời điểm hoàn tất, và MUST chốt nội dung tại thời điểm hoàn tất. Mục nghiêm trọng gồm: cảnh báo, sự cố mức Khẩn cấp hoặc Trung bình; liều Bỏ lỡ, Từ chối; công việc mức Bắt buộc đang Quá hạn. Khi thiếu, hệ thống MUST từ chối và liệt kê các mục còn thiếu ghi chú; ghi chú cho mục khác là tùy chọn. Mục phát sinh sau khi chốt và trước khi Đã xác nhận MUST được hệ thống ghi thêm vào phần "Phát sinh sau khi lập", không sửa phần đã chốt; các mục này không bắt buộc ghi chú, nhưng người bàn giao hoặc Trưởng tầng MAY ghi thêm ghi chú cho chúng (ghi thêm, không sửa). *(Nguồn: BR-M09-06, BR-M09-07, 1.5 nhóm 3; Clarification 2026-09-26, đề xuất Q-83)*
- **FR-042**: Trạng thái bàn giao MUST chỉ thay đổi theo bảng sau:

| Từ | Lệnh / sự kiện | Đến | Người thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Tới thời điểm FR-039 | Bản nháp | Hệ thống | Ca có yêu cầu bàn giao, chưa có bàn giao | Thông báo người bàn giao |
| Bản nháp | Hoàn tất bàn giao | Đã lập | Người phụ trách ca; Trưởng tầng (FR-041) | Có nhận định chung; mọi mục nghiêm trọng có ghi chú (FR-041) | Chốt nội dung; ca Đã đóng nếu đã qua giờ kết thúc (FR-022); thông báo người xác nhận |
| Đã lập | Xác nhận | Đã xác nhận | Người phụ trách ca sau; Trưởng tầng của phạm vi | Người xác nhận đang trong phạm vi ca sau (feature 002 FR-032) | FR-044 |
| Đã lập | Gửi ý kiến | Có ý kiến | Người phụ trách ca sau; Trưởng tầng của phạm vi | Nội dung ý kiến bắt buộc | Thông báo người bàn giao và Trưởng tầng |
| Có ý kiến | Bổ sung và gửi lại | Đã lập | Người bàn giao; Trưởng tầng (FR-041) | Có phản hồi cho từng ý kiến | Ý kiến và phần bổ sung được ghi thêm; ca Đã đóng nếu đang Chờ bàn giao (FR-022); thông báo người xác nhận |

  Không có chuyển nào ra khỏi Đã xác nhận. Khi hai lệnh trên cùng một bàn giao được gửi gần như đồng thời (ví dụ Xác nhận và Gửi ý kiến), chỉ lệnh được ghi nhận trước có hiệu lực; lệnh sau MUST bị từ chối vì trạng thái đã đổi. *(Nguồn: 13.5, BR-M09-08, DBR-20)*

- **FR-043**: Nếu tới giờ bắt đầu ca sau cộng CFG-M09-05 (mặc định \[30 phút\]) mà bàn giao chưa Đã xác nhận, hệ thống MUST nhắc Trưởng tầng của phạm vi và người phụ trách ca sau. Nếu tới giờ kết thúc ca mà bàn giao còn Bản nháp hoặc Có ý kiến, hệ thống MUST nhắc người phụ trách ca và Trưởng tầng (FR-022, Q-87). *(Nguồn: BR-M09-07, CFG-M09-05)*
- **FR-044**: Khi bàn giao chuyển Đã xác nhận, hệ thống MUST trong cùng một lần: (a) yêu cầu feature 005 chuyển công việc chưa đóng sang checklist ca mới (feature 005 FR-047); (b) yêu cầu feature 007 chuyển người phụ trách cảnh báo, sự cố đang mở (feature 007 FR-037); (c) yêu cầu feature 006 hiển thị liều Mang theo chờ ghi nhận cho Điều dưỡng phụ trách của ca mới (feature 006 FR-028); (d) khóa nội dung bàn giao; (e) ghi nhật ký người xác nhận, thời điểm, căn cứ quyền (nhiệm vụ Người phụ trách ca hoặc vai trò Trưởng tầng). Nếu một yêu cầu thất bại, toàn bộ lệnh MUST bị hủy và bàn giao giữ Đã lập. Trước khi xác nhận, công việc tồn MUST NOT chuyển sang checklist của nhân viên ca mới; từ giờ bắt đầu ca sau, cảnh báo, sự cố, công việc và liều chỉ chuyển tạm theo FR-044a. *(Nguồn: BR-M09-08, BR-M05-05, 000 "hoặc toàn bộ, hoặc không")*
- **FR-044a**: Tại giờ bắt đầu ca sau, nếu bàn giao chưa Đã xác nhận, hệ thống MUST chuyển tạm người phụ trách của mỗi cảnh báo, sự cố đang mở trong phạm vi mà người phụ trách hiện tại thuộc ca trước và không có tên trong ca nào đang diễn ra, sang Người phụ trách ca sau (không có thì Trưởng tầng của phạm vi); mỗi mục MUST mang dấu "tạm nhận, chờ xác nhận bàn giao"; trạng thái, cấp leo thang và hạn tiếp nhận giữ nguyên; trong lúc tạm nhận, người tạm nhận MUST thay Điều dưỡng phụ trách ở mọi quy tắc của feature 007 dùng tới vai trò này (người nhận thông báo theo FR-035, bậc đầu của chuỗi leo thang theo FR-033), các bậc sau giữ nguyên (Q-94); người tạm nhận MUST được thông báo danh sách; người tạm nhận MAY khác người nhận chính thức sau khi xác nhận (Điều dưỡng phụ trách theo feature 007 FR-037), và người nhận chính thức MUST được thông báo khi việc chuyển chính thức xảy ra. Cảnh báo, sự cố phát sinh sau thời điểm này theo quy tắc của feature 007. Cùng thời điểm, hệ thống MUST yêu cầu feature 005 chuyển công việc chưa đóng và feature 006 chuyển liều Mang theo chờ ghi nhận mà người được giao thuộc ca trước và không có tên trong ca nào đang diễn ra thành công việc chung, liều chung của tầng (feature 005 FR-031, FR-032; feature 006 FR-026a), giữ trạng thái, thời điểm dự kiến, lịch sử và mang dấu "tồn, chờ xác nhận bàn giao"; nhân viên đủ điều kiện của ca sau nhận việc hoặc được phân lại như công việc chung. Khi bàn giao Đã xác nhận, FR-044 (a) → (c) MUST áp cho cả các mục đang tạm chuyển (mục đã được ai nhận hoặc đóng giữ kết quả đó; công việc Thường Quá hạn đã được nhận hoặc phân lại trong lúc chờ MUST NOT bị tự đóng theo feature 005 FR-047, chỉ công việc vẫn là việc chung chưa ai nhận mới bị tự đóng, Q-93) và các dấu tạm được gỡ. Mỗi lần chuyển tạm MUST ghi nhật ký. *(Nguồn: BR-M09-08, BR-M05-05, BR-M04-13, 1.3; Clarification 2026-09-26, đề xuất Q-78, Q-85)*
- **FR-045**: Khi mở bàn giao để xác nhận, người xác nhận MUST thấy mỗi mục với trạng thái lúc chốt và trạng thái hiện tại (nếu khác), phần "Phát sinh sau khi lập" và các ý kiến trước đó. *(Nguồn: 13.5, BR-M09-08)*
- **FR-046**: Bàn giao đã xác nhận MUST NOT sửa hay xóa; sai sót MUST xử lý bằng bản đính chính trỏ về bàn giao gốc, do người bàn giao, người xác nhận, Trưởng tầng hoặc người phụ trách ca của phạm vi đó tạo (feature 000). Bản đính chính MUST NOT làm thay đổi các tác động đã thực hiện ở FR-044. *(Nguồn: BR-M09-08, BR-M15-03, 1.5, DBR-20)*
- **FR-047**: Nội dung bàn giao MUST hiển thị theo quyền của người xem: trường sức khỏe áp giới hạn trường của feature 002 (FR-022); Nhân viên chăm sóc chỉ thấy các mục và nhận định gắn với người cao tuổi trong phạm vi phân công, kể cả trong phần "Phát sinh sau khi lập", và thấy nhận định chung (19.3). Mọi lần xem, hoàn tất, xác nhận, gửi ý kiến MUST ghi nhật ký (19.4 xếp bàn giao vào nhóm "đặc biệt quan trọng"; bàn giao chứa thông tin sức khỏe của nhiều người cao tuổi). Bàn giao MUST được lưu giữ tới khi hết thời hạn lưu của mọi người cao tuổi có mục trong bàn giao (CFG-M15-03, mặc định \[10 năm sau kết thúc lưu trú\]; NFR-07 "giữ tới khi cả chuỗi hết hạn"); hồ sơ nhân viên, giấy phép, ca, phân công MUST được lưu ít nhất bằng thời hạn của các bản ghi tham chiếu tới chúng. *(Nguồn: 19.3, 19.4 "đặc biệt quan trọng với… bàn giao")*

#### F. Quyền và giao tiếp với feature khác

- **FR-048**: Quyền của feature này MUST theo Permission Matrix 4.4 và mục 19.3 (19.3 là căn cứ khi khác nhau):

| Chức năng | Quản lý viện | Trưởng tầng | Bác sĩ | Điều dưỡng | Nhân viên chăm sóc | Dinh dưỡng viên, Bếp, Vệ sinh, Hành chính | Người thân |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Hồ sơ nhân viên, giấy phép, đào tạo (UC-49) | Thực hiện | Xem (giới hạn trường, FR-052) | — | — | — | — | — |
| Mẫu ca, danh mục loại giấy phép/đào tạo | Cấu hình | — | — | — | — | — | — |
| Lịch ca, phân công (UC-50, UC-51) | Thực hiện, duyệt công bố (toàn viện) | Thực hiện, duyệt công bố (tầng được giao) | Xem của mình | Xem của mình | Xem của mình | Xem của mình | — |
| Giao trưởng tầng | Thực hiện | Xem | — | — | — | — | — |
| Ghi nhận vắng ca | Xem | Thực hiện | — | Thực hiện khi là Người phụ trách ca | — | — | — |
| Bàn giao ca (UC-52, UC-53) | Xem (khi tầng chưa có Trưởng tầng: nhận nhắc để giao Trưởng tầng tạm, FR-024, Q-90) | Hoàn tất, xác nhận, gửi ý kiến (tầng được giao) | Xem | Bổ sung ghi chú; hoàn tất, xác nhận, gửi ý kiến khi là Người phụ trách ca; thêm các lệnh của Trưởng tầng khi tầng chưa có Trưởng tầng (FR-024, Q-90) | Xem trong phạm vi phân công | — | — |

  *(Nguồn: 4.4, 19.3, feature 002 FR-019, FR-034)*

- **FR-049**: Bảng tổng hợp giao tiếp với feature khác:

| Chiều | Feature | Sự kiện / dữ liệu |
| --- | --- | --- |
| Cung cấp | 002 | Trạng thái làm việc, Nghỉ việc (khóa tài khoản); giấy phép, đào tạo và hiệu lực; ca, phân công, giao trưởng tầng, Người phụ trách ca theo thời điểm (phạm vi dữ liệu, FR-019a → c) |
| Nhận | 002 | Tài khoản liên kết và vai trò; danh mục nghiệp vụ chuyên môn và giấy phép yêu cầu |
| Nhận | 001 | Mức chăm sóc hiện hành |
| Nhận, Cung cấp | 003 | Nhận cơ cấu tầng/khu vực/phòng, phân bổ giường, yêu cầu vệ sinh Gấp chưa xong; cung cấp phân công vệ sinh và danh sách phân công còn hiệu lực khi ngừng hiệu lực tầng |
| Nhận | 004 | Tiếp nhận, tạm vắng, trở về trong ca |
| Nhận, Cung cấp | 005 | Nhận danh mục loại công việc và chứng chỉ yêu cầu, công việc chưa đóng cho bản nháp; cung cấp nhân viên chính theo vai trò (FR-028), vắng ca, nghỉ việc, bàn giao đã xác nhận |
| Nhận, Cung cấp | 006 | Nhận liều Trễ, Bỏ lỡ, Từ chối, Mang theo chờ ghi nhận; cung cấp Điều dưỡng phụ trách theo ca, bàn giao đã xác nhận |
| Nhận, Cung cấp | 007 | Nhận chỉ số vượt ngưỡng, cảnh báo và sự cố đang mở; cung cấp Bác sĩ trực, Người phụ trách ca, Điều dưỡng phụ trách, chuyển tạm người phụ trách (FR-044a), bàn giao đã xác nhận |
| Cung cấp | 009 | Sự kiện thông báo, mức độ và người nhận (FR-004, FR-008, FR-009, FR-016, FR-017, FR-036, FR-039, FR-042, FR-043; mức độ và giới hạn nội dung theo FR-058, FR-059) |
| Nhận, Cung cấp | 015 | Nhận kết quả đổi ca, nghỉ đột xuất đã duyệt, lịch Nháp sinh từ mẫu xoay ca; cung cấp kiểm tra BR-M09-01, BR-M09-03 (FR-013, FR-014, FR-030) (Q-77) |
| Cung cấp | 016 | Tỷ lệ phục vụ theo ca (FR-037); bàn giao đúng hạn / trễ; nhân viên có giấy phép, đào tạo sắp hết hạn; các lần dùng quyền thay Trưởng tầng (FR-056) |

#### G. Quyền riêng tư, nhật ký và thông báo

- **FR-050**: Nội dung sức khỏe trong bàn giao gồm: giá trị chỉ số và mức vượt ngưỡng, tên thuốc, liều và lý do Từ chối/Bỏ lỡ, nội dung cảnh báo và sự cố, nhận định theo người cao tuổi, ghi chú của mục, vấn đề cần theo dõi, ý kiến bàn giao và phần bổ sung. Các trường này MUST được xem theo giới hạn trường của feature 002 FR-022: Trưởng tầng, Điều dưỡng (trong phạm vi), Bác sĩ và Quản lý viện (toàn viện) xem đầy đủ; Nhân viên chăm sóc xem theo FR-047; vai trò khác không xem bàn giao. Ý kiến bàn giao và phần bổ sung MUST theo cùng giới hạn trường và cùng quy tắc chỉ ghi thêm như mục bàn giao. *(Nguồn: 19.3, 4.4 dòng "Bàn giao ca", feature 002 FR-022, FR-034)*
- **FR-051**: Vấn đề cần theo dõi thêm tay MUST gắn với một người cao tuổi hoặc một phòng trong phạm vi ca; nhận định chung MUST NOT là chỗ ghi thông tin sức khỏe của một người cụ thể — khi người bàn giao nhập nhận định chung, hệ thống MUST nhắc ghi thông tin theo từng người vào nhận định theo người cao tuổi. Nhân viên được Bổ sung vào ca (FR-018) MUST xem được bàn giao mà ca đó đã nhận, theo phạm vi phân công của họ, từ thời điểm bổ sung. *(Nguồn: 19.3, feature 002 FR-032)*
- **FR-052**: Dữ liệu cá nhân của nhân viên MUST được xem theo nhóm trường: (a) nhóm công khai trong viện — họ tên, chức danh, chuyên môn, trạng thái làm việc, loại và tình trạng hiệu lực của giấy phép/đào tạo; (b) nhóm hạn chế — thông tin liên hệ, số giấy phép, bản scan, lý do Cho nghỉ việc, lý do thu hồi, lý do sửa sai. Quản lý viện xem cả hai nhóm; Trưởng tầng xem nhóm (a) toàn viện và thông tin liên hệ của nhân viên có ca trong tầng mình trong tháng; nhân viên khác chỉ xem nhóm (a) của người cùng ca. *(Nguồn: 4.4 dòng "Hồ sơ nhân viên, chứng chỉ", 19.3, feature 002 FR-022)*
- **FR-053**: Lý do "Ghi nhận vắng ca" MUST chỉ hiển thị cho người ghi, Trưởng tầng của tầng và Quản lý viện. Thông báo về vắng ca, nghỉ việc, thu hồi giấy phép, kết thúc phân công do giấy phép/đào tạo hết hiệu lực MUST NOT nêu lý do; chỉ nêu sự kiện và tác động (ca, phân công, người cao tuổi bị ảnh hưởng). Cảnh báo giấy phép/đào tạo sắp hết hạn gửi Trưởng tầng (FR-008) MUST chỉ nêu tên nhân viên, loại, ngày hết hạn và ca bị ảnh hưởng. Cảnh báo giờ làm liên tục (FR-014) MUST chỉ hiển thị cho người xếp lịch, Trưởng tầng của tầng, Quản lý viện và chính nhân viên. *(Nguồn: 19.3, 19.4)*
- **FR-054**: Nhân viên xem được ca và phân công của chính mình, danh sách người cùng ca và người phụ trách của ca mình (FR-033); MUST NOT xem lịch ca, phân công của người khác ngoài các ca mình có tên. Thông báo khi công bố lịch (FR-016) MUST chỉ chứa ca của chính người nhận. *(Nguồn: 4.4 "P")*
- **FR-055**: Mọi lệnh làm thay đổi dữ liệu nhóm 2 của spec này MUST ghi nhật ký gồm người thực hiện, thời điểm, trước/sau, căn cứ quyền và lý do (feature 000). Lý do MUST bắt buộc với mọi lệnh thay đổi hoặc chấm dứt đối tượng đang có (kết thúc, hủy, chuyển, thu hồi, sửa sai, rút, cho nghỉ việc, ghi nhận vắng, công bố của người tự lập); lập mới phân công và giao trưởng tầng MUST ghi nhật ký, lý do tùy chọn. Thay đổi trong lịch Nháp và phân công Nháp không ghi nhật ký từng lần vì chưa có hiệu lực; khi công bố, hệ thống MUST lưu toàn bộ lịch và phân công tại thời điểm công bố làm bản gốc có nhật ký. Thay đổi do hệ thống (tự lập, tự cập nhật bản nháp bàn giao, chuyển tạm, kết thúc phân công do giấy phép, rút ca do nghỉ việc, chuyển trạng thái ca) MUST ghi người thực hiện "Hệ thống" và sự kiện kích hoạt. *(Nguồn: 1.5, 19.4, feature 000, DBR-23)*
- **FR-056**: Mỗi lần dùng quyền theo nhiệm vụ hoặc quyền thay thế MUST ghi căn cứ theo một trong các giá trị: "vai trò", "nhiệm vụ Người phụ trách ca", "thay Trưởng tầng – tầng chưa có Trưởng tầng", "tạm nhận – bàn giao chưa xác nhận". Quản lý viện MUST tra cứu được danh sách các lần dùng căn cứ khác "vai trò" theo tầng và thời gian. Nhật ký xem bàn giao (FR-047) MUST chỉ Quản lý viện xem được; nhân viên xem lịch sử bàn giao trong phạm vi theo 19.4 nhưng MUST NOT thấy danh sách người đã xem. *(Nguồn: 19.4, feature 002 FR-019a → d, Q-90)*
- **FR-057**: Bàn giao đã xác nhận, gồm phần đã chốt, phần "Phát sinh sau khi lập", ý kiến, phản hồi và ghi chú, MUST thuộc danh sách bản ghi bảo vệ (BR-M15-03). Bản đính chính bàn giao MUST có lý do và MUST được thông báo cho người bàn giao, người xác nhận và Người phụ trách ca hiện tại của phạm vi. Hết thời hạn lưu (FR-047) được xét cho cả bàn giao: bàn giao MUST NOT bị loại bỏ từng phần. Hồ sơ nhân viên, giấy phép, đào tạo, ca, phân công MUST được lưu không ngắn hơn CFG-M15-04 (mặc định \[10 năm\]) và không ngắn hơn thời hạn của bản ghi tham chiếu tới chúng. *(Nguồn: BR-M15-03, NFR-07, CFG-M15-03, CFG-M15-04, feature 000)*
- **FR-058**: Mức độ của các thông báo spec này phát ra (để feature 009 chọn kênh theo BR-M13-01) MUST là: Nhẹ — công bố lịch, thay đổi ca, đào tạo/giấy phép sắp hết hạn, tỷ lệ phục vụ dưới ngưỡng khi lịch còn Nháp; Trung bình — bản nháp bàn giao đã tạo, bàn giao chưa lập khi hết ca, ca sau chưa xác nhận sau CFG-M09-05, ý kiến bàn giao, tạm nhận, ca thiếu người phụ trách, tầng chưa có Trưởng tầng, vắng ca, nghỉ việc, giấy phép hết hiệu lực làm kết thúc phân công, tỷ lệ phục vụ dưới ngưỡng khi lịch đã công bố. Spec này không phát thông báo mức Khẩn cấp. *(Nguồn: BR-M13-01, 17)*
- **FR-059**: Thông báo gửi qua kênh ngoài ứng dụng (tin nhắn, BR-M13-01) MUST NOT chứa thông tin sức khỏe hay họ tên người cao tuổi; chỉ nêu loại sự kiện, tầng/khu vực, ca và yêu cầu mở ứng dụng (ví dụ "Bàn giao ca ngày tầng 2 chưa được xác nhận"). *(Nguồn: 19.3, NFR-08, BR-M13-01)*
- **FR-060**: Hoàn tất bàn giao, gửi ý kiến, bổ sung và xác nhận MUST chỉ thực hiện khi trực tuyến, để nội dung chốt và việc chuyển trách nhiệm là duy nhất (Q-01). Bàn giao MAY được xem ngoại tuyến; bản lưu trên thiết bị MUST chỉ gồm bàn giao trong phạm vi ca hiện tại của người dùng và MUST bị xóa khỏi thiết bị khi hết phạm vi ca (feature 002 FR-032). *(Nguồn: Q-01, 8.6, NFR-08)*

### Key Entities *(include if feature involves data)*

- **Nhân viên (NHAN_VIEN)** – nhóm 1 cho thông tin mô tả, nhóm 2 cho trạng thái: họ tên, chức danh, chuyên môn, ngày bắt đầu, liên hệ, trạng thái làm việc (Đang làm việc / Nghỉ việc) và lịch sử chuyển, tài khoản liên kết.
- **Loại giấy phép/đào tạo** – nhóm 1: tên, nhóm, có thời hạn, bắt buộc với vai trò, có phạm vi hành nghề, trạng thái hiệu lực.
- **Giấy phép/đào tạo của nhân viên (CHUNG_CHI)** – nhóm 1 có lịch sử: nhân viên, loại, số, đơn vị cấp, phạm vi hành nghề, ngày cấp, ngày hết hạn, bản scan, bản được thay thế, thu hồi (ngày, lý do); tình trạng hiệu lực suy ra (FR-007).
- **Mẫu ca** – nhóm 1: tên, giờ bắt đầu, giờ kết thúc, loại phạm vi, có yêu cầu bàn giao, trạng thái hiệu lực.
- **Lịch ca tháng** – nhóm 2: tháng, phạm vi, trạng thái (Nháp / Đã công bố), người công bố, thời điểm.
- **Ca (CA_TRUC)** – nhóm 2: lịch, mẫu, ngày, giờ bắt đầu, giờ kết thúc, phạm vi, người phụ trách ca (và lịch sử chuyển), dấu "thiếu người phụ trách", trạng thái (Nháp / Đã công bố / Đang diễn ra / Chờ bàn giao / Đã đóng / Đã hủy).
- **Nhân viên trong ca** – nhóm 2: ca, nhân viên, đánh dấu Bác sĩ trực, vắng (thời điểm, lý do, người ghi), rút do nghỉ việc, bổ sung sau công bố (lý do).
- **Giao trưởng tầng** – nhóm 2: nhân viên, tầng/khu vực, bắt đầu, kết thúc, người giao.
- **Phân công (PHAN_CONG)** – nhóm 2: ca, nhân viên, đối tượng (tầng/khu vực, phòng, người cao tuổi), loại công việc giới hạn, vai trò (chính / hỗ trợ), nhiệm vụ Điều dưỡng phụ trách, bắt đầu, kết thúc, trạng thái (Nháp / Tương lai / Hiệu lực / Đã kết thúc / Đã hủy), lý do kết thúc, nguồn (lập tay / sao chép).
- **Lần tính tỷ lệ phục vụ** – nhóm 3: ca, thời điểm, sự kiện kích hoạt, tổng trọng số, số nhân viên được tính, tỷ lệ, ngưỡng áp dụng, đạt/không đạt.
- **Bàn giao (BAN_GIAO)** – nhóm 3 từ khi Đã lập: ca trước, ca sau, người bàn giao, người xác nhận, trạng thái (Bản nháp / Đã lập / Có ý kiến / Đã xác nhận), thời điểm lập bản nháp, hoàn tất, xác nhận; nhận định chung; các bản đính chính.
- **Mục bàn giao** – nhóm 3 từ khi Đã lập: bàn giao, loại nguồn (công việc / liều / chỉ số / cảnh báo / sự cố / biến động người cao tuổi / vệ sinh Gấp / thêm tay), bản ghi nguồn, người cao tuổi hoặc phòng, trạng thái lúc chốt, ghi chú, phần (đã chốt / phát sinh sau khi lập / đã xử lý trong ca).
- **Ý kiến bàn giao** – nhóm 3: bàn giao, người gửi, nội dung, thời điểm, phản hồi của người bàn giao.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Với bộ kiểm thử một ca có đủ sáu loại mục nguồn (FR-039), 100% mục nguồn chưa xử lý có mặt trong bản nháp bàn giao và 0 mục đã xử lý xong trước thời điểm hoàn tất còn nằm trong danh sách tồn; bản nháp của một ca sẵn sàng không muộn hơn 1 phút sau thời điểm CFG-M09-04; khi mọi tầng cùng kết ca, bản nháp của cả viện hoàn tất trong giới hạn của NFR-04 (≤ 5 phút) và bản nháp đầu tiên sẵn sàng trong 1 phút.
- **SC-002**: Người phụ trách ca hoàn tất bàn giao cho một tầng khoảng 30 người cao tuổi trong không quá 10 phút (so với bàn giao giấy hiện tại), đo trong thử nghiệm với điều dưỡng thực tế.
- **SC-003**: Sau một tháng vận hành thử, ít nhất 90% bàn giao được ca sau xác nhận trong CFG-M09-05 kể từ đầu ca; 100% bàn giao quá hạn có nhắc gửi tới Trưởng tầng.
- **SC-004**: Khi bàn giao được xác nhận, 100% công việc tồn xuất hiện trong checklist ca mới và 100% cảnh báo, sự cố đang mở có người phụ trách mới trong không quá 1 phút; 0 trường hợp chuyển một phần (hoặc tất cả, hoặc không).
- **SC-005**: Với bộ kiểm thử 200 lần xếp ca và phân công (có giấy phép hết hạn đúng ngày, ca qua đêm, thu hồi, gia hạn), 0 phân công được lập cho nhân viên thiếu giấy phép/đào tạo yêu cầu và 0 nhân viên có hai ca chồng giờ; 100% trường hợp giờ làm liên tục vượt CFG-M09-02 có cảnh báo.
- **SC-006**: 100% đào tạo bắt buộc và giấy phép sắp hết hạn được cảnh báo đúng ngày theo CFG-M09-03 / CFG-M06-02 (kiểm bằng đồng hồ giả lập, NFR-13). Mọi quy tắc theo thời gian của spec này (CFG-M09-02 → 05, CFG-M09-08, CFG-M09-09, CFG-M15-07 ở FR-020, hiệu lực giấy phép tới hết ngày, ca qua đêm, ca qua tháng) đều kiểm được bằng đồng hồ giả lập.
- **SC-007**: Tỷ lệ phục vụ của mọi ca trong bộ kiểm thử khớp 100% với bảng tính tay; cảnh báo xuất hiện với Trưởng tầng trong không quá 1 phút sau sự kiện làm tỷ lệ vượt ngưỡng.
- **SC-008**: Khi nhân viên chuyển Nghỉ việc, 100% tài khoản bị khóa, ca và phân công tương lai được giải phóng trong cùng lệnh; 0 công việc tương lai còn giao cho người đã nghỉ việc.
- **SC-009**: 0 lần sửa hay xóa trực tiếp bàn giao đã xác nhận; mọi thay đổi sau xác nhận đều là bản đính chính có nhật ký.
- **SC-010**: Trưởng tầng lập xong phân công cho một ca của tầng khoảng 30 người trong không quá 5 phút khi dùng sao chép phân công từ ca trước.
- **SC-011**: Với bộ kiểm thử phân công và giao trưởng tầng (có phân công chồng mức đối tượng, lệnh gửi đồng thời, giao trưởng tầng thay tạm), tại mọi thời điểm 0 người cao tuổi có hai nhân viên chính cùng vai trò và 0 tầng có hai Trưởng tầng được giao (FR-024, FR-028).
- **SC-012**: Với bộ kiểm thử mọi loại thông báo của FR-058 gửi qua kênh ngoài ứng dụng, 0 thông báo chứa họ tên người cao tuổi, thông tin sức khỏe hay lý do vắng ca, nghỉ việc, thu hồi giấy phép; 100% lệnh thay đổi dữ liệu nhóm 2 của spec có bản ghi nhật ký với căn cứ quyền (FR-055, FR-056, FR-059).

## Assumptions

- Số feature `008` theo bản đồ feature ở mục 4.2 `docs/phan-tich-yeu-cau.md` (UC-49 → UC-53) và trùng số tuần tự kế tiếp.
- Quy tắc dùng chung theo feature 000; tài khoản, quyền, phạm vi dữ liệu theo feature 002; cơ cấu vị trí và phân bổ giường theo feature 003; công việc theo feature 005; liều theo feature 006; cảnh báo và sự cố theo feature 007. Spec này không lặp lại các quy tắc đó.
- Q-09 được dùng theo mặc định: cấu hình tham chiếu hai ca ngày/đêm; mẫu xoay ca thực tế thuộc feature 015.
- Q-01 được dùng theo mặc định: bàn giao được xem ngoại tuyến nhưng hoàn tất và xác nhận cần trực tuyến, để nội dung chốt và việc chuyển trách nhiệm là duy nhất (đã thành yêu cầu FR-060).
- Quy tắc riêng tư và nhật ký ở FR-050 → FR-060 là mặc định suy ra từ 19.3, 19.4, BR-M13-01, BR-M15-03, feature 000 và 002, không phải quyết định mới; chủ tài liệu cần xác nhận các nhóm trường dữ liệu nhân viên (FR-052) và danh sách mức độ thông báo (FR-058).
- Trạng thái làm việc chỉ gồm Đang làm việc và Nghỉ việc (13.1 chỉ nêu Nghỉ việc); nghỉ dài ngày được thể hiện qua lịch ca (không xếp ca).
- Người phụ trách ca phải có vai trò Trưởng tầng hoặc Điều dưỡng vì chỉ hai vai trò này có quyền "T" ở dòng "Bàn giao ca" (4.4) và 2.4 ghi "thường là trưởng tầng hoặc điều dưỡng".
- Phạm vi của ca là một tầng, một khu vực, hoặc toàn viện (13.2 ghi "khu vực"; 13.4 và 8.4 phân công theo tầng). Ca toàn viện dùng cho Bác sĩ trực và các vai trò phục vụ toàn viện, mặc định không yêu cầu bàn giao.
- Người cao tuổi có tối đa một nhân viên chính cho mỗi vai trò tại mỗi thời điểm (2.4 dùng số ít cho "Điều dưỡng phụ trách"; 8.4 cho phép nhiều nhân viên cùng chăm sóc qua vai trò hỗ trợ).
- Giờ làm liên tục chỉ nối các ca tiếp giáp, không có khoảng nghỉ; không có tham số khoảng nghỉ tối thiểu.
- Cảnh báo đào tạo sắp hết hạn gửi Quản lý viện, chính nhân viên và Trưởng tầng liên quan; nội dung gửi nhân viên chỉ gồm loại và ngày hết hạn (không cần quyền xem hồ sơ).
- Lệnh "Ghi nhận thu hồi" giấy phép được thêm vì giấy phép có thể bị đình chỉ trước ngày hết hạn; nếu không có lệnh này, BR-M09-01 và BR-M06-05 sẽ coi giấy phép bị đình chỉ là còn hiệu lực.
- Bàn giao là bản ghi nhóm 3 từ khi Đã lập; ở Bản nháp chỉ ghi chú và nhận định được thay đổi (feature 000 cho phép module quy định bản nháp).

## Điểm cần báo lại về tài liệu nguồn

Chưa sửa `docs/`. Các điểm dưới đây cần chủ tài liệu xác nhận.

1. **BR-M09-08 chuyển trách nhiệm cảnh báo cho "người phụ trách ca mới"**, còn feature 007 FR-037 (theo BR-M05-05) chuyển cho Điều dưỡng phụ trách người cao tuổi trong ca mới, nếu không có mới tới Người phụ trách ca. Spec này theo feature 007 (FR-044). Cần sửa câu chữ BR-M09-08.
2. **BR-M09-06 không liệt kê** liều Mang theo chờ ghi nhận (feature 006 FR-028) và yêu cầu vệ sinh Gấp chưa xong (feature 003 FR-049), nhưng hai feature đó đã dựa vào bàn giao. Spec thêm vào (FR-039 (b), (f)).
3. **DBR-20 "Mỗi ca kết thúc có đúng một bàn giao"** không phân biệt ca toàn viện (Bác sĩ trực, bếp, hành chính). Spec thêm thuộc tính "có yêu cầu bàn giao" cho mẫu ca (FR-011) và chỉ áp DBR-20 cho ca có yêu cầu.
4. **Permission Matrix 4.4 cho Nhân viên chăm sóc quyền "X" ở dòng "Bàn giao ca"**, theo feature 002 FR-034 là toàn viện, mâu thuẫn với 19.3 ("nhân viên chăm sóc chỉ xem/thực hiện trên phạm vi được phân công"). Spec theo 19.3: xem trong phạm vi phân công (FR-047, FR-048). Cần đổi thành "P".
5. **Permission Matrix không có dòng** cho: mẫu ca, danh mục loại giấy phép/đào tạo, giao trưởng tầng, ghi nhận vắng ca. Spec giao mẫu ca, danh mục, giao trưởng tầng cho Quản lý viện; ghi nhận vắng ca cho Trưởng tầng và Người phụ trách ca (FR-048). Đã chốt khi clarify (Q-80): Người phụ trách ca ghi nhận vắng ca được trong ca mình phụ trách; feature 002 cần thêm ngoại lệ FR-019d vào FR-019 và mục 2.4 cần bổ sung quyền này vào định nghĩa "Người phụ trách ca".
6. **13.2 không nêu vòng đời của ca** (chỉ lịch Nháp → Đã công bố) và BR-M09-07 nói "không thể đóng ca" mà không định nghĩa đóng ca. Spec thêm bảng trạng thái ca với Chờ bàn giao, Đã đóng (FR-022).
7. **13.5 và BR-M09-08 không nêu thời điểm và người xác nhận thay** khi ca sau không có người phụ trách hoặc không có ca sau; spec giao cho Trưởng tầng (FR-038, Edge Cases).
8. **CFG-M09-01 gộp "trọng số chăm sóc theo mức" và "ngưỡng tỷ lệ phục vụ"** trong một tham số, không nêu công thức và vai trò được tính (Q-79). Nên tách làm hai tham số.
9. **Các bổ sung khác của spec không có trong tài liệu nguồn**, cần chủ tài liệu xác nhận: lệnh Ghi nhận thu hồi giấy phép (FR-006); lệnh Nhận lại làm việc (FR-003); lệnh Bổ sung nhân viên vào ca, Chuyển người phụ trách ca, Hủy ca sau công bố (FR-018, FR-019, FR-021) — 13.2 chỉ nêu "đổi ca và nghỉ đột xuất đi qua yêu cầu có duyệt"; quy tắc xác định nhân viên chính theo mức đối tượng (FR-028); phần "Phát sinh sau khi lập" của bàn giao (FR-041); sao chép phân công (FR-032); phân công Nháp (FR-025, Q-82); chuyển tạm cảnh báo, sự cố, công việc, liều khi bàn giao chưa xác nhận (FR-044a, Q-78, Q-85); trạng thái ca Chờ bàn giao (FR-022); ca kéo qua hai tháng thuộc lịch tháng chứa giờ bắt đầu (FR-012); lịch toàn viện chỉ do Quản lý viện lập (FR-016); nhiều Bác sĩ trực cùng lúc (FR-023).
10. **Các quyết định clarify (2026-09-26)** cần phản ánh vào tài liệu nguồn và thêm vào mục 24.2: Q-77 ranh giới 008/015 (sửa cột Feature của UC-50 trong 4.2 cho rõ phần của mỗi feature); Q-78 Người phụ trách ca sau tạm nhận cảnh báo, sự cố đang mở từ giờ bắt đầu ca khi bàn giao chưa xác nhận (bổ sung BR-M09-08, BR-M05-05); Q-79 tỷ lệ phục vụ tính Điều dưỡng và Nhân viên chăm sóc, ngưỡng là trọng số tối đa trên mỗi nhân viên theo mẫu ca (bổ sung BR-M09-02, CFG-M09-01).
11. **Việc đã phản ánh ở spec khác (2026-09-26)**: feature 007 FR-037 và kịch bản User Story 3 số 10 đã thêm ngoại lệ chuyển tạm (Q-78); feature 002 đã thêm FR-019d và sửa FR-019 (Q-80); feature 005 đã thêm FR-047a (Q-85); feature 006 FR-026a đã thêm liều chuyển tạm (Q-85). Mỗi spec có mục "Cập nhật 2026-09-26 (đồng bộ với spec 008)" ở phần Clarifications.
12. **Các quyết định clarify lượt 2 (2026-09-26, /speckit-clarify)** cần phản ánh vào tài liệu nguồn và thêm vào mục 24.2: Q-80 Người phụ trách ca ghi nhận vắng ca trong ca mình phụ trách (bổ sung định nghĩa "Người phụ trách ca" ở 2.4, chú thích ⁹ của 4.4; feature 002 thêm FR-019d); Q-81 ghi nhận vắng ca chỉ từ giờ bắt đầu ca − CFG-M15-07 tới hết ca, vắng biết trước đi qua yêu cầu nghỉ đột xuất (bổ sung 13.2); Q-82 phân công trên lịch Nháp chỉ có hiệu lực khi công bố (bổ sung 13.2, 13.4); Q-83 bắt buộc ghi chú cho mục nghiêm trọng khi hoàn tất bàn giao (bổ sung BR-M09-06); Q-84 mỗi tầng tối đa một Trưởng tầng được giao tại một thời điểm, tầng chưa có Trưởng tầng thì báo Quản lý viện (bổ sung 13.3).
13. **Các quyết định clarify lượt 3 (2026-09-26, sau checklist business-rules)** cần phản ánh vào tài liệu nguồn và thêm vào mục 24.2: Q-85 công việc và liều của người ca trước tạm thành việc chung, liều chung của tầng từ giờ bắt đầu ca sau khi bàn giao chưa xác nhận (bổ sung BR-M09-08; feature 005 FR-047, feature 006 FR-026a cần nhận thêm nguồn kích hoạt này); Q-86 lịch của tầng chưa có Trưởng tầng không công bố được, thiếu giữa chừng thì Quản lý viện thay các lệnh bàn giao (bổ sung 13.3; dòng "Bàn giao ca" của 4.4 cần thêm chú thích cho quyền của Quản lý viện); Q-87 bàn giao Có ý kiến coi như chưa lập xong, ca không đóng (bổ sung BR-M09-07); Q-88 định nghĩa "đã xử lý xong" cho mục bàn giao (bổ sung BR-M09-06); Q-89 hai tham số mới CFG-M09-08 và CFG-M09-09, mặc định \[24 giờ\], cần thêm vào Phụ lục 25.
14. **Thu hẹp so với Permission Matrix 4.4**: "Hủy ca" chỉ giao cho Quản lý viện (FR-021), dù 4.4 cho Trưởng tầng quyền T, D ở dòng "Lịch ca, phân công", vì hủy ca đã công bố làm mất ca của nhiều nhân viên; theo Q-15 đây là thu hẹp được phép. Spec cũng chưa yêu cầu Người phụ trách ca có giấy phép hành nghề còn hiệu lực (FR-015); nếu cơ sở muốn yêu cầu, cần quyết định bổ sung.
15. **Đã phản ánh vào `docs/nghiep-vu.md` (2026-09-26)**: các quyết định Q-77 → Q-89 (mục 24.2), hai tham số CFG-M09-08, CFG-M09-09 và chú giải CFG-M09-01 (Phụ lục 25), các bổ sung ở 2.4 (Người phụ trách ca), 13.1 → 13.5, BR-M05-05, BR-M09-02, BR-M09-06, BR-M09-07, BR-M09-08 (điểm 1, 2, 6, 7, 8, 10, 12, 13). Còn chờ sửa `docs/phan-tich-yeu-cau.md`: cột Feature của UC-50 ở 4.2 (Q-77); Permission Matrix 4.4 — Nhân viên chăm sóc "P" ở dòng "Bàn giao ca" (điểm 4), các dòng mới cho mẫu ca, loại giấy phép/đào tạo, giao trưởng tầng, ghi nhận vắng ca (điểm 5), chú thích ⁹ thêm quyền ghi nhận vắng ca (Q-80), quyền thay Trưởng tầng của Quản lý viện ở dòng "Bàn giao ca" (Q-86); thực thể CA_TRUC, PHAN_CONG, BAN_GIAO ở 3.2 (trạng thái mới).
16. **Các quyết định clarify lượt 4 (2026-09-26, sau checklist consistency)** cần phản ánh vào tài liệu nguồn và thêm vào mục 24.2: Q-90 khi tầng chưa có Trưởng tầng, Quản lý viện không làm thay mà được nhắc giao Trưởng tầng tạm; Người phụ trách ca đang diễn ra của tầng làm các lệnh bàn giao, tạm nhận trong phạm vi ca — **thay phần làm thay của Q-86 đã ghi vào `docs/nghiep-vu.md` ở mục 13.3 và dòng Q-86 của 24.2, hai chỗ này hiện đã lỗi thời**; Q-91 một lần vắng ca chỉ tác động ca đó (bổ sung 13.2); Q-92 đối tượng phân công không có "nhóm người cao tuổi" (sửa 19.2 "nhóm người cao tuổi" và 13.4); Q-93 công việc Thường quá hạn đã được nhận trong lúc chờ xác nhận không bị tự đóng (bổ sung BR-M09-08); Q-94 người tạm nhận thay Điều dưỡng phụ trách trong thông báo và bậc đầu leo thang (bổ sung BR-M05-01, BR-M05-05). Spec 002 (FR-028, mục cập nhật), 005 (FR-032, FR-047a), 007 (FR-037) đã được đồng bộ.
17. **Bổ sung quyền riêng tư và nhật ký (2026-09-26, sau checklist privacy-audit)**: FR-050 → FR-060 là mặc định suy ra, cần chủ tài liệu xác nhận và phản ánh: 19.3 thêm giới hạn trường cho dữ liệu cá nhân nhân viên (nhóm công khai / hạn chế, FR-052) và cho lý do vắng ca, nghỉ việc (FR-053); dòng "Hồ sơ nhân viên, chứng chỉ" của 4.4 ghi Trưởng tầng "X" cần chú thích giới hạn trường; 17 và BR-M13-01 cần danh sách mức độ cho thông báo của Module 09 (FR-058) và quy tắc không đưa thông tin sức khỏe vào tin nhắn (FR-059, áp chung cho các module); DBR-23 ("mọi thay đổi dữ liệu nhóm 2 có nhật ký") cần ngoại lệ cho lịch Nháp và phân công Nháp chưa có hiệu lực (FR-055).
18. **(2026-09-27)** Q-90 → Q-94 đã được phản ánh vào tài liệu nguồn (2.4 dòng "Người phụ trách ca", 13.2, 13.3, 13.4, 13.5, 19.2) và nằm ở mục 24.2; dòng Q-86 ở 24.2 được ghi chú là đã bị Q-90 thay một phần.
