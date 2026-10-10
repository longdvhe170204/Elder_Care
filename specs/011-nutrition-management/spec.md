# Feature Specification: Quản lý dinh dưỡng và suất ăn

**Feature Branch**: `011-nutrition-management`

**Created**: 2026-09-27

**Status**: Draft

**Input**: User description: "Quản lý dinh dưỡng theo docs/nghiep-vu.md Module 08 (mục 12): chế độ ăn gán cho người cao tuổi có luồng duyệt với chế độ bệnh lý; món ăn gắn thành phần gây dị ứng; thực đơn tuần có vòng đời và kiểm tra độ phủ, lặp món, dị ứng trước khi công bố; hệ thống chốt số suất trước mỗi bữa theo người thực tế có mặt; đồ ăn gia đình mang vào được đối chiếu với dị ứng và hạn chế."

## Clarifications

### Session 2026-09-27

- Q: Ai là người duyệt để thực đơn tuần chuyển từ Chờ duyệt sang Công bố? → A: Dinh dưỡng viên tự công bố sau khi thực đơn qua kiểm tra tự động; không có người duyệt thủ công. Chờ duyệt là trạng thái "đã qua kiểm tra, chờ công bố"; quyền D của Bác sĩ ở dòng "Chế độ ăn, thực đơn" chỉ áp cho UC-45 (đề xuất Q-142).
- Q: Những vai trò nào được kiểm đếm phiếu bữa ăn tại tầng rồi xác nhận Đã nhận hoặc báo Có sai lệch? → A: Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc (và Người phụ trách ca) có phạm vi phân công tại tầng/khu của phiếu trong ca đang diễn ra (đề xuất Q-143).
- Q: Có đưa lưu mẫu thức ăn vào hệ thống không, và nếu có thì việc chưa ghi lưu mẫu có chặn bếp giao phiếu bữa ăn không? → A: Có, bằng bản ghi đơn giản; bữa chưa có bản ghi lưu mẫu thì phiếu không chuyển Đã giao được (Q-39).
- Q: Khi chốt suất, người đang Tạm vắng (hoặc đi hoạt động ngoài viện) có giờ dự kiến trở về trước giờ bữa có được tính suất sẵn không? → A: Có, tính sẵn với dấu "dự kiến trở về"; tới giờ bữa dự kiến mà chưa trở về thì hệ thống tạo phát sinh "−1" (đề xuất Q-144).
- Q: Nếu người cao tuổi đang dùng chế độ ăn liên quan điều trị và dinh dưỡng viên chỉ đổi kết cấu thức ăn hoặc hạn chế thực phẩm riêng, bản gán mới có cần bác sĩ duyệt không? → A: Cần duyệt khi đổi, thêm hoặc bỏ chế độ ăn liên quan điều trị, khi chuyển sang kết cấu cứng hơn, hoặc bỏ hạn chế trong lúc dùng chế độ đó; thêm hạn chế hoặc chuyển sang kết cấu mềm hơn thì dinh dưỡng viên áp dụng ngay và bác sĩ được báo (đề xuất Q-145).
- Q: Mỗi bữa, bản ghi lưu mẫu thức ăn phải gồm những món nào thì mới cho bếp giao phiếu? → A: Mọi món được nấu trong bữa, kể cả món thay thế, mỗi món một mẫu; món không lưu được thì ghi lý do và không chặn giao (đề xuất Q-146; người dùng xác nhận phương án B).
- Q: Khi chốt suất ngoài giờ hành chính mà có suất "thiếu món thay thế", ai xử lý khi dinh dưỡng viên không có mặt? → A: Hệ thống tự dùng "món an toàn" do dinh dưỡng viên khai sẵn cho mỗi chế độ ăn nếu không xung đột với người nhận; nếu vẫn xung đột thì báo điều dưỡng phụ trách (đề xuất Q-147; người dùng xác nhận phương án B).

### Cập nhật 2026-09-27 (đồng bộ với spec 014)

Spec 014 và tài liệu nguồn (8.9, 3.4, Q-144, Q-170, Q-176) đã chốt:
- Feature 014 gửi chuyến đi đã lên lịch cùng danh sách người tham gia ở mọi lần thay đổi (đăng ký, hủy, "Không đi", đi sau), giờ về dự kiến và gia hạn, thời điểm rời/về thực tế và đính chính (bảng giao tiếp được sửa). Giả định ở Assumptions về "chuyến ngoài viện có thời điểm dự kiến trở lại" nay đã được spec 014 xác nhận (FR-034).
- Suất ăn của bán trú dùng giờ về theo ngày do feature 005 quản lý, có thể dời vì đồng ý về muộn.

### Cập nhật 2026-09-27 (đồng bộ với spec 016)

Spec 016 định nghĩa "phiếu giao trễ" (Đã giao sau giờ bữa + CFG-M08-04 hoặc chưa giao khi tới mốc đó) và "phiếu có sai lệch" (đã từng Có sai lệch; phần bổ sung không tính là phiếu riêng) cho báo cáo 18.2 (feature 016 FR-034). Số suất theo bữa, chế độ ăn gửi Module 14 thuộc giai đoạn sau (Q-194); bảng giao tiếp được ghi rõ.

### Cập nhật 2026-10-06 (đồng bộ Q-280 → Q-288, quyết định của chủ dự án)

- **Q-283**: bỏ vai trò Dinh dưỡng viên; Bác sĩ lập danh mục chế độ ăn, món ăn, bản gán chế độ ăn, thực đơn, đồ ăn gia đình và xử lý yêu cầu xem lại chế độ ăn. Bản gán do Bác sĩ lập nên **không còn bước duyệt riêng** (BR-M08-03): bỏ trạng thái Chờ duyệt, Từ chối và lệnh Gửi duyệt, Duyệt, Từ chối của bản gán; các trường hợp FR-013 (a) → (d) bắt buộc ghi lý do chuyên môn khi Áp dụng; không còn thông báo cho bác sĩ đã lập bản gán trước đó. Thực đơn giữ vòng đời cũ (Chờ duyệt nghĩa là "đã qua kiểm tra, chờ công bố"), Bác sĩ tự công bố. Mọi chỗ ghi "dinh dưỡng viên" trong thân spec đổi thành "bác sĩ"; User Story 1, 3, mục B, bảng trạng thái bản gán, FR-063, bảng thông báo FR-065, Key Entities, SC-004 và Assumptions viết lại.
- **Q-284**: kho nguyên liệu nấu ăn do bộ phận Bếp tự quản lý (12.7, 13.1a), viết ở feature 019; đề nghị nhập thay bằng yêu cầu mua sắm (15.10). Spec này vẫn không quản lý kho.
- **Q-282**: bỏ "nhân viên vệ sinh" khỏi câu loại trừ quyền ở FR-063.
- **(Bổ sung 2026-10-06, rà soát nhất quán)** Trạng thái "Chờ duyệt" của thực đơn đổi tên thành "Chờ công bố", lệnh "Gửi duyệt" thành "Gửi kiểm tra" (12.2, BR-M08-06), vì Module 08 không có người duyệt. Nghĩa và quy tắc không đổi.

### Cập nhật 2026-10-06, lượt 2 (đồng bộ Q-306, Q-307: hệ thống không tự chốt suất, không tạo yêu cầu hay sự cố)

- **Q-306**: Chốt suất là lệnh của Nhân viên bếp, thực hiện được từ mốc giờ bữa dự kiến trừ CFG-M08-01. Khi chốt, hệ thống tính sẵn số suất và danh sách người được tính, chọn món thay thế theo BR-M08-02. Lệnh Chốt suất lập phiếu bữa ăn và suất ăn người thân. Tới mốc mà bữa chưa chốt thì hệ thống nhắc Nhân viên bếp. Bỏ Bộ lập lịch chốt suất và dấu "chốt bù"; giữ dấu "chốt sau giờ bữa". Tầng/khu chưa có phiếu mà nhận phát sinh "+" thì phiếu được lập khi Bếp xác nhận phát sinh đó. Phát sinh sau chốt vẫn được gửi bếp như cũ, vì đó là tác động kèm lệnh của actor. Lịch bữa cung cấp cho feature 005 là căn cứ để Người phụ trách ca lập công việc "hỗ trợ ăn" (feature 005 FR-017).
- **Q-307**: bỏ yêu cầu xem lại chế độ ăn và CFG-M08-02. Khi có ăn kém kéo dài hoặc sụt cân, hệ thống chỉ báo bác sĩ; bác sĩ giữ nguyên hoặc lập bản gán chế độ ăn mới, không có lệnh kết thúc yêu cầu (BR-M08-05). Phục vụ suất có thành phần gây dị ứng: hệ thống chặn và yêu cầu feature 007 tạo cảnh báo mức Trung bình cho điều dưỡng phụ trách, không tự tạo sự cố. Người cao tuổi đã ăn thì nhân viên ghi nhận sự cố ở feature 007, mức theo triệu chứng, nguồn "ăn uống" (BR-M08-14).
- Giữ nguyên các nội dung tài liệu nguồn còn quy định: hệ thống chọn món thay thế và tự dùng món an toàn (BR-M08-02, FR-037a); phát sinh "−1" chỉ để ghi nhận khi người dự kiến trở về chưa về (Q-144, FR-040a). Xem điểm báo lại cuối spec.
- Viết lại: Phạm vi mục 9; User Story 4 (mô tả, kịch bản 1, 7); User Story 6 (mô tả, kịch bản 3, 4); User Story 8; User Story 9 kịch bản 3; Edge Cases (chốt sau giờ bữa); bảng nhóm dữ liệu; FR-010, FR-033, FR-038, FR-042 → FR-044, FR-049, FR-061, FR-063; mục I (FR-058, FR-059, bỏ bảng trạng thái); bảng phiếu; bảng FR-065; bảng giao tiếp; Key Entities; SC-001, 005, 006, 010; Assumptions.

### Cập nhật 2026-10-06, lượt 3 (đồng bộ Q-308: Nhân viên bếp lập món ăn, thực đơn; Bác sĩ xác nhận, duyệt)

- **Q-308**: Bác sĩ vẫn xác định yêu cầu dinh dưỡng của từng người qua bản gán chế độ ăn (mục B), quản lý danh mục chế độ ăn, thành phần thực phẩm và chọn món an toàn. Nhân viên bếp tạo, sửa món ăn và gửi xác nhận; Bác sĩ xác nhận thành phần và chế độ ăn phù hợp, hoặc trả lại kèm lý do. Chỉ món đã xác nhận mới dùng được trong thực đơn. Sửa món đã xác nhận cần Bác sĩ xác nhận lại; trong lúc chờ, món giữ nội dung đã xác nhận.
- Nhân viên bếp lập thực đơn tuần, rồi tự xác nhận đã kiểm tra khả năng chế biến, nguyên liệu, suất ăn, kèm ghi chú. Hệ thống hiển thị tồn kho nguyên liệu và số người dùng từng chế độ ăn, không đối chiếu theo định lượng (Q-220). Sau đó Bếp gửi kiểm tra. Bác sĩ duyệt bằng Công bố, hoặc Trả lại về Nháp kèm lý do. Bếp vẫn Rút lại và Hủy được. Nhân viên bếp đổi món sau công bố, Bác sĩ được báo. Nhắc thực đơn tuần kế tiếp gửi Nhân viên bếp, và gửi Bác sĩ khi thực đơn đang Chờ công bố.
- Viết lại: Phạm vi mục 1, 4; User Story 2, 3; Edge Cases (chế độ ăn chưa có món, món ngừng hiệu lực); bảng nhóm dữ liệu; mục A (FR-002 → FR-004a, thêm bảng trạng thái món ăn); FR-020; mục D (FR-024, FR-025, FR-025a mới, FR-027 → FR-029, FR-031, bảng thực đơn); FR-063; bảng FR-065; Key Entities; SC-003, SC-009, SC-014 mới; Assumptions.

### Cập nhật 2026-10-10, lượt 2 (đồng bộ Q-333)

- **Q-333**: áp món an toàn tại mốc CFG-M08-05 và phát sinh "−1" tại giờ bữa là chuyển trạng thái theo giờ, ghi rõ trong Q-307. Sửa FR-037a, FR-040a.

## Phạm vi

**Trong phạm vi** (Module 08, mục 12; UC-45 → UC-48, UC-75 → UC-78):

1. Danh mục chế độ ăn (Bác sĩ), món ăn gắn thành phần gây dị ứng và chế độ ăn phù hợp (Nhân viên bếp lập, Bác sĩ xác nhận), bữa ăn, loại đồ ăn bị cấm theo chính sách cơ sở (12.1, 12.4, 1.5 nhóm 1; **sửa, 2026-10-06, Q-308**).
2. Chế độ ăn gán cho người cao tuổi: vòng đời Đề xuất → (Chờ hiệu lực) → Hiệu lực → Ngừng do Bác sĩ lập và áp dụng, thay đổi liên quan điều trị bắt buộc lý do chuyên môn (sửa, 2026-10-06, Q-283: không còn Chờ công bố); kết cấu thức ăn và hạn chế thực phẩm của từng người (12.1, BR-M08-03, UC-45).
3. Đối chiếu món ăn với dị ứng và hạn chế thực phẩm: khi gán chế độ ăn, khi lập thực đơn, khi thêm dị ứng mới, khi chốt suất (BR-M08-02, BR-M01-07).
4. Thực đơn tuần: Nhân viên bếp lập, kiểm tra khả năng chế biến, nguyên liệu, suất ăn rồi gửi kiểm tra; hệ thống kiểm tra độ phủ, lặp món, dị ứng; Bác sĩ công bố hoặc trả lại; vòng đời Nháp → Chờ công bố → Công bố → Đã áp dụng; Nhân viên bếp đổi món sau công bố (12.2, BR-M08-06 → 08, UC-46; **sửa, 2026-10-06, Q-308**).
5. Chốt số suất trước mỗi bữa theo người dự kiến có mặt, tính cả người thân ở lại có đăng ký ăn; thay đổi sau chốt thành phát sinh (BR-M08-01, BR-M08-13, UC-47).
6. Phiếu bữa ăn theo tầng/khu, suất đặc biệt có tên, nhãn suất đặc biệt, chuẩn bị, giao, nhận, báo sai lệch (12.5, BR-M08-09 → 12, UC-47, UC-75, UC-76).
7. Xác nhận phục vụ suất đặc biệt đúng người, đúng suất (BR-M08-14, UC-77).
8. Đồ ăn gia đình mang vào: ghi nhận, đối chiếu tự động, xác nhận khi nghi vấn (12.4, BR-M08-04, UC-48).
9. Báo bác sĩ xem lại chế độ ăn khi ăn kém kéo dài hoặc sụt cân; bác sĩ giữ nguyên hoặc lập bản gán mới (BR-M08-05; **sửa, 2026-10-06, Q-307**).
10. Lưu mẫu thức ăn (12.5, BR-M08-15, UC-78); bắt buộc trước khi giao phiếu (Q-39, FR-060).
11. Các thông báo của nghiệp vụ dinh dưỡng, gửi qua feature 009 (FR-065).

**Ngoài phạm vi** (spec này **nhận** dữ liệu hoặc **cung cấp** dữ liệu cho feature sở hữu):

- Quy tắc dùng chung (nhóm dữ liệu, lý do bắt buộc, đính chính, nhật ký, tham số, "hoặc toàn bộ, hoặc không", Bộ lập lịch chạy lại không tạo trùng, nhắc yêu cầu chờ lâu FR-031a): feature 000. Spec này kế thừa và không lặp lại.
- Mục dị ứng, bệnh nền, nhu cầu dinh dưỡng trong hồ sơ sức khỏe, danh mục dị nguyên dùng chung: feature 001. Spec này **dùng** dị ứng Hiệu lực và thêm thành phần thực phẩm vào danh mục dị nguyên (feature 001 FR-018a).
- Trạng thái người cao tuổi, lượt vắng, Điều trị tại bệnh viện, kết thúc lưu trú, qua đời: feature 001, 004. Giường và tầng của người nội trú, khu bán trú: feature 003. Trạng thái có mặt bán trú theo ngày, công việc "hỗ trợ ăn" và kết quả ăn uống, cảnh báo ăn kém kéo dài: feature 005. Chuyến hoạt động ngoài viện: feature 014. Lượt người thân ở lại có đăng ký ăn: feature 012.
- Tạo cảnh báo và sự cố (xung đột dị ứng, phục vụ sai suất ăn), cảnh báo sụt cân: feature 007. Gửi thông báo: feature 009. Khoản chi phí suất ăn của người thân ở lại: feature 010. Đơn giá suất ăn ngoài hợp đồng: danh mục của feature 004.
- Báo cáo số phiếu giao trễ, có sai lệch (18.2) và dashboard: feature báo cáo (Module 14). Spec này chỉ cung cấp dữ liệu.
- *(Đã chỉnh sửa 2026-09-28, Q-210)* Kho nguyên liệu nấu ăn (nhập, xuất theo lô, kiểm kê, 12.7, BR-M08-17 → 20): thuộc Module 08 nhưng được viết ở feature 019; **(sửa, 2026-10-06, Q-284)** kho do bộ phận Bếp tự quản lý, đề nghị nhập thay bằng yêu cầu mua sắm (15.10). Không thuộc spec này. Spec này không tự trừ kho theo thực đơn hay số suất (Q-220). Mua nguyên liệu ngoài kho và định lượng dinh dưỡng từng món: ngoài hệ thống ở giai đoạn này.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Bác sĩ gán chế độ ăn; thay đổi liên quan điều trị bắt buộc lý do chuyên môn (Priority: P1) *(viết lại 2026-10-06, Q-283)*

Bác sĩ lập đề xuất chế độ ăn cho người cao tuổi: chọn chế độ ăn từ danh mục, kết cấu thức ăn (thường / mềm / xay nhuyễn), các hạn chế thực phẩm riêng, ghi chú nhu cầu dinh dưỡng, thời điểm hiệu lực và lý do, rồi Áp dụng. Không có bước duyệt riêng vì người lập đã là Bác sĩ. Việc chuyển sang, đổi hoặc bỏ một chế độ ăn được đánh dấu "liên quan điều trị" (ví dụ tiểu đường, suy thận), cũng như chuyển sang kết cấu cứng hơn hoặc bỏ hạn chế khi đang dùng chế độ đó, bắt buộc ghi lý do chuyên môn. Mỗi người cao tuổi có đúng một chế độ ăn Hiệu lực tại một thời điểm.

**Why this priority**: Chế độ ăn là căn cứ của mọi bước phía sau: độ phủ thực đơn, số suất theo chế độ ăn, suất đặc biệt, đối chiếu đồ ăn gia đình. Quy tắc duyệt chuyên môn (12.2, BR-M08-03) là yêu cầu an toàn.

**Independent Test**: Với A có chế độ "Thường" Hiệu lực, lập và áp dụng "Tiểu đường" (liên quan điều trị) kết cấu "mềm" khi thiếu lý do chuyên môn (bị chặn) rồi có lý do; lập chế độ "Ăn chay" (không liên quan điều trị) cho B và kiểm tra hiệu lực ngay; lập bản gán có thời điểm hiệu lực tương lai.

**Acceptance Scenarios**:

1. **Given** A có chế độ "Thường" Hiệu lực, **When** bác sĩ lập đề xuất "Tiểu đường" kết cấu "mềm" và Áp dụng mà không ghi lý do chuyên môn, **Then** hệ thống chặn (FR-013 (a)); A vẫn được tính suất theo chế độ "Thường".
2. **Given** đề xuất "Tiểu đường" của A ở Đề xuất, **When** bác sĩ Áp dụng kèm lý do chuyên môn với thời điểm hiệu lực là thời điểm áp dụng, **Then** trong cùng một lệnh bản gán chuyển Hiệu lực và chế độ "Thường" chuyển Ngừng; lịch sử ghi người lập, thời điểm, lý do (BR-M08-03).
3. **Given** đề xuất "Suy thận" của C ở Đề xuất, **When** bác sĩ Hủy mà không nhập lý do, **Then** hệ thống chặn; **When** có lý do, **Then** bản gán chuyển Đã hủy, chế độ ăn hiện hành của C giữ nguyên. *(Sửa 2026-10-06, Q-283: không còn lệnh Từ chối.)*
4. **Given** B có chế độ "Thường" Hiệu lực, **When** bác sĩ lập và áp dụng chế độ "Ăn chay" (không liên quan điều trị), **Then** chế độ "Ăn chay" Hiệu lực ngay, không bắt buộc lý do chuyên môn, chế độ cũ chuyển Ngừng.
5. **Given** đề xuất của A có thời điểm hiệu lực 00:00 ngày 05/11 và được áp dụng ngày 03/11, **When** áp dụng, **Then** bản gán chuyển Chờ hiệu lực; tới 00:00 ngày 05/11 Bộ lập lịch chuyển nó Hiệu lực và chuyển chế độ cũ Ngừng trong cùng một lệnh.
6. **Given** A đang có một bản gán ở Đề xuất hoặc Chờ hiệu lực, **When** bác sĩ lập thêm một đề xuất khác cho A, **Then** hệ thống chặn và yêu cầu hủy bản gán đang chờ trước (FR-012).
7. **Given** bất kỳ ai, **When** tìm cách sửa chế độ ăn, kết cấu hay hạn chế thực phẩm của một bản gán Hiệu lực, **Then** không có thao tác sửa; chỉ có lệnh lập đề xuất mới thay thế (1.5 nhóm 2).
8. **Given** D vừa Hoàn tất tiếp nhận và chưa có chế độ ăn nào Hiệu lực, **When** tới thời điểm chốt bữa trưa, **Then** D được tính suất theo chế độ ăn mặc định của viện, suất mang dấu "chưa gán chế độ ăn", và bác sĩ được thông báo (FR-014).
9. **Given** A đang dùng "Tiểu đường" kết cấu "mềm" Hiệu lực, **When** bác sĩ lập bản gán "Tiểu đường" kết cấu "xay nhuyễn" thêm hạn chế "thịt bò", **Then** bản gán áp dụng được mà không bắt buộc lý do chuyên môn; **When** sau đó bác sĩ lập bản gán chuyển A về kết cấu "thường", hoặc bỏ hạn chế "thịt bò", **Then** lệnh Áp dụng bắt buộc lý do chuyên môn (FR-013 (c), (d), Q-145). Không có thông báo cho bác sĩ đã lập bản gán trước (sửa, Q-283).

---

### User Story 2 - Bếp lập món ăn gắn thành phần gây dị ứng, Bác sĩ xác nhận; xung đột với dị ứng và hạn chế được phát hiện ở mọi điểm (Priority: P1)

Nhân viên bếp tạo món ăn: các thành phần chọn từ danh mục dị nguyên dùng chung (feature 001), các chế độ ăn phù hợp và các kết cấu có thể chế biến, rồi gửi Bác sĩ xác nhận. Bác sĩ xác nhận thành phần và chế độ ăn phù hợp, hoặc trả lại kèm lý do; chỉ món đã xác nhận mới dùng được trong thực đơn (Q-308). Khi gán chế độ ăn, khi thêm một dị ứng mới cho người cao tuổi, hoặc khi thực đơn đã công bố có món xung đột với dị ứng hay hạn chế của người dùng chế độ ăn đó, hệ thống cảnh báo và cho biết ai cần món thay thế.

**Why this priority**: Xung đột dị ứng qua bữa ăn là rủi ro an toàn trực tiếp (BR-M08-02, BR-M01-07). Các kiểm tra ở User Story 3, 4, 6, 7 đều dựa trên thành phần của món.

**Independent Test**: Bếp tạo món "Cá kho" có thành phần "cá", món "Đậu phụ sốt cà" làm món thay thế, Bác sĩ xác nhận một món và trả lại một món; Bếp sửa thành phần của một món đã xác nhận; gán chế độ ăn cho E có dị ứng "cá"; thêm dị ứng "tôm" cho F khi thực đơn tuần đã công bố có món "Canh chua tôm" không có món thay thế.

**Acceptance Scenarios**:

1. **Given** nhân viên bếp tạo món "Cá kho" mà không chọn thành phần nào, **When** gửi xác nhận, **Then** hệ thống chặn; mỗi món MUST có ít nhất một thành phần hoặc được đánh dấu rõ "không có thành phần gây dị ứng trong danh mục" (FR-004).
2. **Given** món "Cá kho" đã được dùng trong một thực đơn đã công bố, **When** nhân viên bếp tìm cách xóa món, **Then** không có thao tác xóa; chỉ có "Ngừng hiệu lực", và món đã ngừng không chọn được cho thực đơn mới (1.5 nhóm 1).
3. **Given** thực đơn tuần 03/11 → 09/11 đã công bố, chế độ "Thường" có món "Cá kho" ở bữa trưa thứ 4 với món thay thế "Đậu phụ sốt cà", **When** bác sĩ gán chế độ "Thường" cho E có dị ứng "cá" Hiệu lực, **Then** hệ thống thông báo E sẽ nhận món thay thế ở bữa đó; không chặn (BR-M08-02).
4. **Given** thực đơn đã công bố có món "Canh chua tôm" ở chế độ "Thường" không có món thay thế phù hợp với F, **When** điều dưỡng ghi thêm dị ứng "tôm" cho F (feature 001), **Then** hệ thống yêu cầu feature 007 tạo cảnh báo "xung đột dị ứng với thực đơn" cho F, nêu bữa và món, và thông báo bác sĩ (BR-M01-07, FR-021).
5. **Given** G có dị ứng loại "khác" ghi bằng văn bản ("một số loại nấm"), **When** chốt suất, **Then** G có trong danh sách cần đối chiếu khi phục vụ của bữa, chỉ nhân viên tại tầng thấy; nếu G không có lý do nào khác thì suất của G vẫn là suất thường trên phiếu, và bếp không thấy dấu hay nội dung nào về dị ứng của G (feature 001 FR-018a, FR-036a, 19.3).
6. **Given** H có hạn chế thực phẩm "thịt bò" trong bản gán chế độ ăn Hiệu lực, **When** thực đơn có món "Phở bò" ở bữa sáng của chế độ H dùng, **Then** hệ thống xử lý như một xung đột dị ứng: H cần món thay thế ở bữa đó.
7. **(Bổ sung, 2026-10-06, Q-308)** **Given** nhân viên bếp K gửi xác nhận món "Bún riêu" với thành phần "cua, cà chua" và chế độ ăn phù hợp "Thường, Mềm", **When** Bác sĩ thấy thiếu thành phần "tôm" và trả lại mà không nhập lý do, **Then** hệ thống chặn; **When** có lý do, **Then** món về Nháp, K được báo. **When** K bổ sung "tôm", gửi lại và Bác sĩ xác nhận, **Then** món Hiệu lực và chọn được cho thực đơn. **When** K chọn một món còn ở Nháp hoặc Chờ xác nhận khi lập thực đơn, **Then** hệ thống chặn (FR-004a).
8. **(Bổ sung, 2026-10-06, Q-308)** **Given** món "Cháo gà" Hiệu lực đang có trong thực đơn đã công bố, **When** K sửa thành phần thêm "nấm" và gửi xác nhận, **Then** món vẫn dùng nội dung đã xác nhận (không có "nấm") cho kiểm tra, chốt suất và phục vụ tới khi Bác sĩ xác nhận bản sửa; **When** Bác sĩ xác nhận, **Then** nội dung mới có hiệu lực từ thời điểm đó và hệ thống kiểm tra lại các thực đơn chưa hết tuần có món này (FR-004). **When** Bác sĩ trả lại bản sửa, **Then** món giữ nội dung cũ.

---

### User Story 3 - Nhân viên bếp lập thực đơn tuần, Bác sĩ duyệt; hệ thống kiểm tra độ phủ, lặp món, dị ứng trước khi công bố (Priority: P1)

Nhân viên bếp lập thực đơn cho một tuần theo các yêu cầu dinh dưỡng do Bác sĩ xác định: với mỗi ngày, mỗi bữa, mỗi chế độ ăn, chọn các món đã được Bác sĩ xác nhận và món thay thế cho từng món. Trước khi gửi, Bếp kiểm tra khả năng chế biến, nguyên liệu, suất ăn: hệ thống hiển thị tồn kho nguyên liệu và số người đang dùng từng chế độ ăn; Bếp xác nhận đã kiểm tra kèm ghi chú. Khi gửi kiểm tra, hệ thống kiểm tra tự động: chế độ ăn nào đang có người dùng mà thiếu món ở bữa nào thì chặn và chỉ rõ; món lặp trong CFG-M08-03 ngày liên tiếp hoặc món chứa thành phần gây dị ứng của người thuộc chế độ ăn mà chưa có món thay thế thì cảnh báo. Thực đơn qua kiểm tra chuyển Chờ công bố và Bác sĩ được báo. Bác sĩ duyệt về chuyên môn: Công bố, hoặc Trả lại kèm lý do (Q-308). Tới ngày đầu tuần thì thực đơn chuyển Đã áp dụng. Sau khi công bố, Nhân viên bếp chỉ đổi món bằng lệnh có lý do; Bác sĩ được thông báo.

**Why this priority**: Thực đơn công bố là nguồn món cho chốt suất và phiếu bữa ăn. Kiểm tra trước công bố (BR-M08-06, BR-M08-07) được nêu trực tiếp trong mô tả feature.

**Independent Test**: Có 4 chế độ ăn đang có người dùng. Lập thực đơn tuần thiếu bữa tối thứ 6 của chế độ "Tiểu đường", có món "Cháo gà" ở bữa sáng 3 ngày liên tiếp, và món chứa "cá" không có món thay thế trong khi một người dùng chế độ đó dị ứng cá. Bếp gửi kiểm tra, sửa, gửi lại; Bác sĩ trả lại một lần rồi công bố; cho tới ngày đầu tuần; Bếp đổi một món sau thời điểm chốt.

**Acceptance Scenarios**:

1. **Given** chế độ "Tiểu đường" có 12 người đang dùng và thực đơn tuần 10/11 → 16/11 thiếu món bữa tối thứ 6 của chế độ này, **When** nhân viên bếp gửi kiểm tra, **Then** hệ thống chặn và liệt kê "Tiểu đường – thứ 6 14/11 – bữa tối"; thực đơn vẫn ở Nháp (BR-M08-06).
2. **Given** chế độ "Cắt nhỏ ít muối" không có người nào dùng trong tuần, **When** gửi kiểm tra thực đơn không có món cho chế độ này, **Then** không bị chặn vì chế độ không có người dùng (BR-M08-06).
3. **Given** CFG-M08-03 mặc định \[3 ngày\] và món "Cháo gà" ở bữa sáng chế độ "Mềm" các ngày 10/11, 11/11, 12/11, **When** gửi kiểm tra, **Then** hệ thống cảnh báo lặp món, nêu món, chế độ ăn và các ngày; không chặn. Nhân viên bếp phải xác nhận đã xem cảnh báo để gửi tiếp; Bác sĩ thấy các cảnh báo này khi duyệt (BR-M08-07).
4. **Given** thực đơn tuần trước có "Cháo gà" ở bữa sáng chế độ "Mềm" ngày 08/11 và 09/11, **When** thực đơn tuần 10/11 có món này ngày 10/11, **Then** cảnh báo lặp món được tính qua ranh giới tuần (FR-026).
5. **Given** E dị ứng "cá" dùng chế độ "Thường", thực đơn có "Cá kho" ở bữa trưa 12/11 chưa có món thay thế, **When** gửi kiểm tra, **Then** hệ thống cảnh báo "món chứa thành phần gây dị ứng của người dùng chế độ ăn, chưa có món thay thế", nêu số người bị ảnh hưởng; nhân viên bếp chỉ thấy số người, Bác sĩ thấy tên người khi duyệt (BR-M08-07, 19.3).
6. **Given** thực đơn Chờ công bố, **When** nhân viên bếp rút lại, **Then** thực đơn về Nháp để sửa. **When** Bác sĩ trả lại kèm lý do "bữa tối chế độ Tiểu đường nhiều tinh bột", **Then** thực đơn về Nháp và Bếp được báo lý do; trả lại không có lý do thì bị chặn. **When** Bác sĩ công bố, **Then** hệ thống chạy lại kiểm tra độ phủ tại thời điểm công bố; đạt thì thực đơn chuyển Công bố và Bếp được thông báo, không cần Quản lý viện duyệt (FR-029, Q-142, Q-308). **When** trong lúc chờ có người mới dùng chế độ "Tiểu đường" làm chế độ này thiếu món ở một bữa, **Then** lệnh công bố bị chặn và nêu bữa thiếu. **When** nhân viên bếp tìm cách công bố, **Then** hệ thống từ chối.
7. **Given** thực đơn tuần 10/11 đã Công bố, **When** Bộ lập lịch tới 00:00 ngày 10/11, **Then** thực đơn chuyển Đã áp dụng.
8. **Given** thực đơn đã công bố, **When** nhân viên bếp đổi món bữa trưa 12/11 của chế độ "Thường" từ "Cá kho" sang "Gà luộc" không nhập lý do, **Then** hệ thống chặn; **When** có lý do "thiếu nguyên liệu", **Then** món được đổi, lịch sử lưu món trước/sau, Bác sĩ được thông báo; "Gà luộc" phải là món đã xác nhận; nếu lúc đổi đã qua thời điểm chốt bữa trưa 12/11 thì thay đổi được đánh dấu phát sinh (BR-M08-08).
9. **Given** thực đơn đã công bố, **When** một lệnh đổi món làm chế độ "Tiểu đường" không còn món ở bữa tối 14/11, **Then** hệ thống chặn lệnh (FR-032).
10. **Given** thực đơn tuần 10/11 đã có một bản Công bố, **When** nhân viên bếp tạo thêm một thực đơn cho cùng tuần, **Then** hệ thống chặn (FR-023).
11. **(Bổ sung, 2026-10-06, Q-308)** **Given** thực đơn tuần 10/11 ở Nháp, **When** nhân viên bếp gửi kiểm tra mà chưa xác nhận đã kiểm tra khả năng chế biến, nguyên liệu, suất ăn, **Then** hệ thống chặn. **When** Bếp xem tồn kho nguyên liệu (feature 019) và số người dùng từng chế độ ăn trong tuần (ví dụ Thường 40, Tiểu đường 12, Mềm 8), xác nhận kèm ghi chú "thiếu cá tuần đầu, đã có yêu cầu mua sắm", **Then** việc gửi tiếp tục với các kiểm tra tự động; ghi chú được lưu cùng thực đơn để Bác sĩ xem. Hệ thống không đối chiếu nhu cầu nguyên liệu theo định lượng món (Q-220, FR-025a).
12. **(Bổ sung, 2026-10-06, Q-308)** **Given** tới 2 ngày trước ngày đầu tuần (CFG-M08-07) mà tuần kế tiếp chưa có thực đơn Công bố, **When** thực đơn còn Nháp hoặc chưa có, **Then** nhân viên bếp được nhắc và Quản lý viện được báo; **When** thực đơn đang Chờ công bố, **Then** Bác sĩ cũng được nhắc duyệt (FR-027).

---

### User Story 4 - Nhân viên bếp chốt số suất trước mỗi bữa theo người thực tế dự kiến có mặt (Priority: P1)

Từ mốc CFG-M08-01 (mặc định \[2 giờ\]) trước mỗi bữa, Nhân viên bếp thực hiện lệnh Chốt suất; hệ thống tính sẵn số suất theo từng chế độ ăn và từng tầng/khu (Q-306): người nội trú đang ở viện trừ người Tạm vắng, Điều trị tại bệnh viện, Hoạt động bên ngoài; người bán trú có lịch và chưa báo vắng; cộng người thân ở lại có đăng ký ăn. Thay đổi xảy ra sau thời điểm chốt (người trở về, người rời viện, đổi chế độ ăn, đổi món, thêm dị ứng) được gửi cho bếp dưới dạng phát sinh, và bếp phải xác nhận đã nhận. Tới mốc mà bữa chưa chốt, hệ thống nhắc Nhân viên bếp; hệ thống không tự chốt.

**Why this priority**: Số suất đúng là mục tiêu vận hành chính của Module 08 (BR-M08-01). Suất thiếu thì người cao tuổi không có ăn; suất dư thì lãng phí.

**Independent Test**: Với cơ sở giả lập 3 tầng và khu bán trú: 40 người nội trú (2 Tạm vắng, 1 Điều trị tại bệnh viện, 3 đi hoạt động ngoài viện qua bữa trưa), 8 người bán trú có lịch (1 Vắng có báo), 1 người thân ở lại có đăng ký ăn. Nhân viên bếp chốt bữa trưa, đối chiếu số suất hệ thống tính với tính tay; sau chốt cho 1 người trở về và đổi chế độ ăn của 1 người.

**Acceptance Scenarios**:

1. **Given** giờ bữa trưa dự kiến 11:00 (CFG-M08-04) và CFG-M08-01 mặc định \[2 giờ\], **When** Nhân viên bếp chốt bữa trưa lúc 08:30, **Then** hệ thống chặn vì chưa tới mốc. **When** tới 09:00 bữa chưa chốt, **Then** Nhân viên bếp được nhắc. **When** Nhân viên bếp chốt lúc 09:05, **Then** số suất theo từng chế độ ăn và từng tầng/khu, danh sách người được tính được lưu với thời điểm chốt 09:05, và lệnh lập phiếu bữa ăn cho từng tầng/khu (BR-M08-01, BR-M08-09, Q-306).
2. **Given** Tầng 2 có 15 người nội trú, trong đó A Tạm vắng, B Điều trị tại bệnh viện, C có chuyến hoạt động ngoài viện 08:00 → 14:00 đã lên lịch, **When** chốt bữa trưa, **Then** Tầng 2 có 12 suất; A, B, C không được tính (BR-M08-01).
3. **Given** người bán trú K có lịch đến thứ 4, trạng thái có mặt lúc 09:00 là "Chưa đến", và L đã "Vắng có báo", **When** chốt bữa trưa thứ 4, **Then** K được tính vào phiếu khu bán trú, L không được tính (3.4, BR-M08-01).
4. **Given** người thân M của A (Tầng 2) có lượt ở lại Đang ở lại, đăng ký ăn, **When** chốt bữa trưa, **Then** Tầng 2 có thêm 1 suất theo chế độ ăn mặc định của viện, ghi "người thân ở lại của A", và một suất ăn người thân được ghi làm bản ghi nguồn chi phí cho feature 010 (BR-M08-09, BR-M10-04).
5. **Given** bữa trưa đã chốt lúc 09:00, **When** A được ghi nhận trở về lúc 10:15, **Then** hệ thống tạo phát sinh "+1 suất chế độ Thường, Tầng 2" gắn bữa trưa, gửi bếp; bếp được nhắc cho tới khi xác nhận đã nhận (BR-M08-01, BR-M08-13).
6. **Given** phát sinh của bữa trưa 11:00 chưa được bếp xác nhận, CFG-M08-05 mặc định \[30 phút\], **When** tới 10:30, **Then** bếp và bác sĩ được nhắc (BR-M08-13).
7. **Given** bữa trưa ngày 12/11 đã được chốt, **When** một nhân viên bếp khác chốt lại cùng bữa, **Then** hệ thống chặn; không có thêm lần chốt, suất hay phiếu (FR-033).
8. **Given** chế độ "Tiểu đường" của N được bác sĩ áp dụng, hiệu lực lúc 09:30, sau khi bữa trưa đã chốt, **When** lệnh áp dụng thành công, **Then** hệ thống tạo phát sinh "−1 Thường, +1 Tiểu đường" cho tầng của N ở bữa trưa.
9. **Given** hết giờ bữa trưa dự kiến, **When** người cao tuổi P trở về lúc 11:40, **Then** hệ thống không tự tạo phát sinh cho bữa trưa; người nhận tại tầng lập được "yêu cầu suất bổ sung" có lý do, gửi bếp như một phát sinh (FR-040).
10. **Given** V Tạm vắng về nhà với giờ dự kiến trở về 10:30 và bữa trưa chốt lúc 09:00, **When** chốt, **Then** V được tính suất ở Tầng 2 với dấu "dự kiến trở về". **When** V được Ghi nhận trở về lúc 10:20, **Then** dấu được gỡ, không có phát sinh. **When** (tình huống khác) tới 11:00 V vẫn chưa trở về, **Then** hệ thống tạo phát sinh "−1" chỉ để ghi nhận cho Tầng 2 với nguồn "chưa trở về đúng dự kiến", suất của V là "suất giữ" tại tầng; **When** V về lúc 11:15 (trong ngưỡng giao trễ), **Then** người nhận tại tầng ghi "phục vụ suất giữ" và hệ thống tạo phát sinh "+1" không cần bếp chuẩn bị (FR-040a).
11. **Given** người bán trú K được tính suất bữa trưa khi đang "Chưa đến", **When** lúc 10:00 trạng thái có mặt của K chuyển Vắng không báo, **Then** hệ thống tạo phát sinh "−1" cho phiếu khu bán trú (FR-039).
12. **Given** bữa trưa đã chốt, W (Tầng 2) có suất đặc biệt "xay nhuyễn", **When** W được chuyển giường sang Tầng 3 lúc 10:00, **Then** hệ thống tạo phát sinh "−1" cho Tầng 2 và "+1" cho Tầng 3; suất đặc biệt của W chuyển sang phiếu Tầng 3 (FR-039, Edge Cases).
13. **Given** bữa trưa đã chốt, **When** Quản lý viện khai báo hôm nay là ngày khu bán trú nghỉ (Q-140) lúc 09:30, **Then** hệ thống tạo phát sinh "−" cho mọi suất bán trú có lịch của phiếu khu bán trú (FR-039).
14. **Given** chế độ "Thường" có món an toàn "Cháo thịt bằm", Y dị ứng "cá", bữa sáng 06:30 có "Cháo cá" không có món thay thế phù hợp với Y, chốt lúc 04:30 (ngoài CFG-M13-06), **When** chốt, **Then** suất của Y dùng "Cháo thịt bằm", mang dấu "món an toàn", không bị chặn chuẩn bị, và bác sĩ nhận thông báo Nhẹ. **When** (tình huống khác) Y cũng dị ứng thịt heo nên món an toàn xung đột, **Then** suất giữ dấu "thiếu món thay thế" và điều dưỡng phụ trách Y nhận thông báo Trung bình (FR-037a, Q-147).
15. **Given** bữa trưa 11:00 chốt lúc 09:00 trong giờ hành chính, suất của Z mang dấu "thiếu món thay thế", **When** tới 10:30 (CFG-M08-05 trước giờ bữa) bác sĩ vẫn chưa chỉ định món, **Then** hệ thống tự dùng món an toàn nếu không xung đột với Z và tạo phát sinh cho bếp (FR-037a (ii)).

---

### User Story 5 - Bếp chuẩn bị, dán nhãn và giao phiếu bữa ăn; tầng kiểm đếm và nhận (Priority: P2)

Sau thời điểm chốt, mỗi tầng/khu có một phiếu bữa ăn gồm số suất theo từng chế độ ăn và danh sách suất đặc biệt có tên (họ tên, phòng, chế độ ăn, món thay thế, kết cấu). Bếp đánh dấu chuẩn bị từng suất đặc biệt, dán nhãn, và chỉ giao được khi mọi suất đặc biệt đã chuẩn bị. Người nhận tại tầng kiểm đếm rồi xác nhận Đã nhận, hoặc báo Có sai lệch để bếp xử lý. Quá giờ bữa mà chưa giao thì trưởng tầng và bếp được cảnh báo.

**Why this priority**: Là đầu ra của chốt suất tới bếp và tầng (12.5). Cần chốt suất (User Story 4) trước.

**Independent Test**: Chốt bữa trưa Tầng 2 có 14 suất (12 Thường, 2 Tiểu đường) và 3 suất đặc biệt. Thử giao khi còn 1 suất đặc biệt chưa chuẩn bị; giao; tầng báo thiếu 1 suất; bếp bổ sung; tầng nhận. Để phiếu Tầng 3 quá giờ.

**Acceptance Scenarios**:

1. **Given** chốt bữa trưa xong, **When** bếp mở phiếu Tầng 2, **Then** bếp thấy số suất theo chế độ ăn và 3 suất đặc biệt với họ tên, phòng, chế độ ăn, món thay thế, kết cấu; không thấy dị ứng, bệnh lý hay lý do của món thay thế (12.5, 19.3).
2. **Given** phiếu Tầng 2 Đã chốt, 1 trong 3 suất đặc biệt chưa được đánh dấu chuẩn bị, **When** bếp chuyển phiếu sang Đã giao, **Then** hệ thống chặn và nêu suất còn thiếu (BR-M08-10).
3. **Given** mọi suất đặc biệt đã chuẩn bị, **When** bếp chuyển phiếu Đã chuẩn bị rồi Đã giao, **Then** hệ thống ghi người giao và thời điểm giao (BR-M08-10).
4. **Given** phiếu Tầng 2 Đã giao, **When** người nhận tại tầng báo "thiếu 1 suất Tiểu đường", **Then** phiếu chuyển Có sai lệch, nội dung được lưu và bếp được thông báo; **When** bếp ghi cách xử lý "đã bổ sung 1 suất", **Then** phiếu về Đã giao; **When** tầng xác nhận, **Then** phiếu Đã nhận và lịch sử sai lệch còn nguyên (BR-M08-11).
5. **Given** giờ bữa trưa 11:00, CFG-M08-04 ngưỡng giao trễ mặc định \[30 phút\], phiếu Tầng 3 vẫn Đã chuẩn bị lúc 11:30, **When** tới 11:30, **Then** trưởng tầng Tầng 3 và bếp nhận cảnh báo mức Nhẹ (BR-M08-12); nếu Tầng 3 tạm chưa có Trưởng tầng, Người phụ trách ca của Tầng 3 nhận thay (FR-048a, 2.4).
6. **Given** phiếu Tầng 2 đã Đã nhận, **When** người thân ở lại của A bắt đầu ở lại lúc 10:40 (trước giờ bữa), **Then** phát sinh "+1" được bếp xác nhận rồi giao như một phần bổ sung của phiếu, có người và thời điểm giao; người nhận tại tầng xác nhận nhận phần bổ sung; phiếu vẫn Đã nhận (FR-048b).
7. **Given** bữa trưa ngày 12/11 có 3 phiếu và 2 phát sinh, **When** đối chiếu, **Then** tổng suất trên các phiếu đúng bằng số suất đã chốt cộng phát sinh (DBR-27).

---

### User Story 6 - Nhân viên chăm sóc xác nhận phục vụ suất đặc biệt đúng người, đúng suất (Priority: P2)

Khi phục vụ một suất đặc biệt, nhân viên chăm sóc xác nhận suất trên nhãn đúng là của người đang được phục vụ trước khi ghi kết quả ăn uống (feature 005). Nếu suất sai người, hệ thống chặn. Nếu suất có thành phần gây dị ứng với người nhận, hệ thống chặn và tạo cảnh báo mức Trung bình cho điều dưỡng phụ trách; hệ thống không tự tạo sự cố. Nếu người cao tuổi đã ăn, nhân viên ghi nhận sự cố ở feature 007, mức theo triệu chứng, nguồn "ăn uống" (BR-M08-14, Q-307).

**Why this priority**: Là lớp chặn cuối cùng trước khi người cao tuổi ăn phải thức ăn gây dị ứng (BR-M08-14). Cần phiếu và suất đặc biệt (User Story 5) trước.

**Independent Test**: Phục vụ suất đặc biệt của E cho E; phục vụ suất của E cho G; phục vụ cho F một suất chứa "tôm" khi F vừa được ghi dị ứng tôm sau thời điểm chốt; khai "đã ăn" cho một trường hợp.

**Acceptance Scenarios**:

1. **Given** suất đặc biệt của E trên phiếu Tầng 2, **When** nhân viên chăm sóc xác nhận phục vụ suất đó cho E, **Then** xác nhận được lưu và nhân viên được ghi kết quả ăn uống của E cho bữa đó (BR-M08-14, 8.6).
2. **Given** suất đặc biệt có nhãn của E, **When** nhân viên xác nhận phục vụ suất đó cho G và suất không chứa thành phần gây dị ứng của G, **Then** hệ thống chặn, báo "suất không đúng người", không tạo sự cố.
3. **Given** suất đặc biệt của F có món chứa "tôm" và F có dị ứng "tôm" Hiệu lực, **When** nhân viên xác nhận phục vụ và khai "chưa ăn", **Then** hệ thống chặn ghi nhận và yêu cầu feature 007 tạo cảnh báo mức Trung bình "phục vụ suất có thành phần gây dị ứng" cho điều dưỡng phụ trách F; không có sự cố nào được tạo (BR-M08-14, Q-307).
4. **Given** như kịch bản 3 nhưng nhân viên khai "đã ăn" và ghi triệu chứng "nổi mẩn", **When** lưu, **Then** cảnh báo được tạo như kịch bản 3, và nhân viên MUST ghi nhận sự cố loại "phục vụ sai suất ăn", nguồn "ăn uống", mức theo triệu chứng (mặc định Trung bình; chọn thấp hơn phải có lý do) ở feature 007 trước khi hoàn tất xác nhận; xác nhận phục vụ trỏ về sự cố đó (BR-M08-14, feature 007 FR-042).
5. **Given** H có hạn chế riêng "thịt bò" (không phải dị ứng), suất của H có món chứa thịt bò do phục vụ nhầm suất, **When** nhân viên xác nhận phục vụ, **Then** hệ thống chặn với lý do "suất trái hạn chế" và không tạo sự cố (FR-049).
6. **Given** A có suất đặc biệt ở bữa trưa, **When** nhân viên ghi kết quả ăn uống bữa trưa của A ở feature 005 mà chưa có xác nhận phục vụ, **Then** hệ thống chặn và yêu cầu xác nhận trước (FR-050).

---

### User Story 7 - Đồ ăn gia đình mang vào được đối chiếu với dị ứng, hạn chế và chính sách của viện (Priority: P2)

Khi người thân mang đồ ăn vào, điều dưỡng hoặc bác sĩ ghi nhận: người gửi, người cao tuổi, loại đồ ăn, thành phần chính, thời gian, số lượng, tình trạng, ghi chú. Hệ thống đối chiếu ngay với dị ứng Hiệu lực, hạn chế thực phẩm, hạn chế của chế độ ăn và danh mục loại đồ ăn bị cấm. Vi phạm thì Không sử dụng. Còn nghi vấn thì chỉ cho dùng khi điều dưỡng hoặc bác sĩ xác nhận.

**Why this priority**: Đồ ăn ngoài là nguồn rủi ro dị ứng và vi phạm chế độ ăn điều trị mà thực đơn không kiểm soát được (12.4, BR-M08-04). Độc lập với thực đơn và chốt suất.

**Independent Test**: Ghi 4 lần gửi đồ ăn: bánh có "đậu phộng" cho người dị ứng đậu phộng; "rượu thuốc" (loại bị cấm); hộp cơm thành phần "không rõ"; trái cây cho người không có hạn chế. Kiểm tra trạng thái của từng lần và xác nhận nghi vấn.

**Acceptance Scenarios**:

1. **Given** Q dị ứng "đậu phộng", **When** điều dưỡng ghi nhận "bánh quy" có thành phần "đậu phộng" do con gái Q gửi, **Then** đồ ăn chuyển Không sử dụng, lý do "trùng dị ứng Hiệu lực", và điều dưỡng được hướng dẫn trả lại hoặc hủy bỏ (BR-M08-04).
2. **Given** "đồ uống có cồn" thuộc danh mục loại đồ ăn bị cấm, **When** ghi nhận "rượu thuốc", **Then** đồ ăn chuyển Không sử dụng, lý do "vi phạm chính sách của viện" (12.4).
3. **Given** R dùng chế độ "Tiểu đường" có hạn chế "đường", **When** ghi nhận "chè đậu xanh" có thành phần "đường", **Then** đồ ăn chuyển Không sử dụng, lý do "trái hạn chế của chế độ ăn".
4. **Given** S không có dị ứng hay hạn chế, **When** ghi nhận "hộp cơm" với thành phần "không rõ", **Then** đồ ăn chuyển Cần xác nhận; **When** bác sĩ xác nhận cho dùng kèm ghi chú, **Then** đồ ăn chuyển Được sử dụng, lưu người và thời điểm xác nhận (BR-M08-04).
5. **Given** đồ ăn của Q ở Không sử dụng do trùng dị ứng, **When** bất kỳ ai tìm cách chuyển nó sang Được sử dụng, **Then** không có lệnh nào cho phép (BR-M08-04).
6. **Given** T không có dị ứng, "sữa chua" của T đang Được sử dụng, **When** bác sĩ ghi thêm dị ứng "sữa" cho T, **Then** hệ thống chuyển đồ ăn đó sang Không sử dụng, lý do "dị ứng mới", và thông báo điều dưỡng phụ trách T (FR-056).
7. **Given** U có kết cấu "xay nhuyễn", **When** ghi nhận "táo" không vi phạm dị ứng, hạn chế hay chính sách, **Then** đồ ăn chuyển Cần xác nhận với lý do "kết cấu thức ăn của người nhận khác thường" (FR-054).
8. **Given** R dùng chế độ "Tiểu đường" (liên quan điều trị), **When** ghi nhận "bánh mì nguyên cám" không có thành phần trùng dị ứng hay hạn chế, **Then** đồ ăn chuyển Cần xác nhận với lý do "người nhận dùng chế độ ăn liên quan điều trị" (FR-054).
9. **Given** "súp gà tự nấu" của S được bác sĩ xác nhận cho dùng với hạn dùng 18:00 hôm nay, **When** tới 18:00, **Then** hệ thống chuyển đồ ăn sang Không sử dụng, lý do "hết hạn dùng", và điều dưỡng phụ trách S nhận thông báo Nhẹ (FR-055).

---

### User Story 8 - Bác sĩ được báo xem lại chế độ ăn khi ăn kém kéo dài hoặc sụt cân (Priority: P3)

Khi feature 005 phát hiện ăn kém kéo dài (BR-M04-09) hoặc feature 007 phát hiện sụt cân (BR-M05-04), hệ thống báo bác sĩ xem lại chế độ ăn. Hệ thống không tạo yêu cầu và không theo dõi hạn xử lý. Bác sĩ quyết định giữ nguyên, không cần lệnh, hoặc lập bản gán chế độ ăn mới (BR-M08-05, Q-307).

**Why this priority**: Khép vòng theo dõi dinh dưỡng (BR-M08-05). Dựa trên dữ liệu của feature 005, 007 và luồng gán chế độ ăn (User Story 1).

**Independent Test**: Cho B có 3 bữa kém liên tiếp, rồi có cảnh báo sụt cân; kiểm tra thông báo tới bác sĩ; bác sĩ lập đề xuất chế độ ăn mới có tham chiếu thông báo.

**Acceptance Scenarios**:

1. **Given** feature 005 báo B có CFG-M04-04 bữa ăn kém liên tiếp, **When** nhận sự kiện, **Then** bác sĩ nhận thông báo mức Nhẹ "xem lại chế độ ăn" kèm nguồn; không có yêu cầu nào được tạo (BR-M08-05, Q-307).
2. **Given** B vừa có thông báo ăn kém kéo dài, **When** feature 007 báo sụt cân cho B, **Then** bác sĩ nhận thêm một thông báo với nguồn "sụt cân" (FR-058).
3. **Given** bác sĩ xem thông báo và quyết định giữ nguyên chế độ ăn, **When** không thực hiện lệnh nào, **Then** hệ thống không nhắc lại và không báo Quản lý viện (Q-307).
4. **Given** bác sĩ quyết định đổi chế độ ăn của B, **When** bác sĩ lập đề xuất chế độ ăn mới và chọn tham chiếu thông báo đã nhận, **Then** bản gán mới lưu nguồn "sau thông báo ăn kém kéo dài / sụt cân" (FR-010, FR-059).

---

### User Story 9 - Bếp ghi nhận lưu mẫu thức ăn mỗi bữa (Priority: P3)

Mỗi bữa, bếp ghi nhận món đã lưu mẫu, thời điểm lưu, người lưu và thời điểm hủy mẫu. Phiếu bữa ăn chỉ chuyển Đã giao khi bữa đã có bản ghi lưu mẫu. Bếp được nhắc hủy mẫu sau CFG-M08-06 (mặc định \[24 giờ\]). Phạm vi đã chốt theo Q-39 (FR-060).

**Why this priority**: Là bằng chứng kiểm thực khi có ngộ độc thực phẩm (12.5, Q-39); ít ảnh hưởng tới luồng chính ngoài điều kiện giao phiếu.

**Independent Test**: Thử giao phiếu khi bữa chưa có bản ghi lưu mẫu; ghi lưu mẫu; giao; để quá CFG-M08-06; ghi hủy mẫu.

**Acceptance Scenarios**:

1. **Given** bữa trưa 12/11 chưa có bản ghi lưu mẫu, **When** bếp chuyển phiếu Tầng 2 sang Đã giao, **Then** hệ thống chặn (BR-M08-15).
2. **Given** bếp ghi lưu mẫu "Cá kho, Canh rau" lúc 10:40, **When** bếp giao phiếu, **Then** không bị chặn; **When** tới 10:40 ngày hôm sau (CFG-M08-06) mà chưa ghi hủy mẫu, **Then** bếp được nhắc mức Nhẹ.
3. **Given** bữa trưa 12/11 có 5 món được nấu (gồm món thay thế "Đậu phụ sốt cà" và món an toàn "Cháo thịt bằm"), **When** bếp mở bản ghi lưu mẫu, **Then** danh sách món cần lưu mẫu (tính từ thực đơn và kết quả chốt) có đủ 5 món. **When** bếp ghi "Canh rau" không lưu mẫu mà không có lý do, **Then** hệ thống chặn; **When** có lý do "hết hộp lưu mẫu", **Then** bản ghi được lưu, phiếu giao được, và bác sĩ, Quản lý viện nhận thông báo Nhẹ (FR-061, Q-146).

---

### Edge Cases

- **Chế độ ăn của một người chưa có món trong thực đơn đã công bố** (người mới dùng một chế độ ăn sau khi thực đơn đã công bố): khi gán, bác sĩ được cảnh báo và nhân viên bếp được báo để đổi món bổ sung (FR-032, **sửa, 2026-10-06, Q-308**). Nếu tới lúc chốt vẫn thiếu, suất được chốt theo chế độ ăn đó với dấu "chưa có món trong thực đơn"; bác sĩ và bếp được thông báo mức Trung bình (FR-038).
- **Tuần chưa có thực đơn công bố khi tới thời điểm chốt**: việc chốt vẫn chạy (số suất theo chế độ ăn), phiếu mang dấu "chưa có thực đơn công bố", không xác định được món thay thế; bác sĩ, bếp và Quản lý viện được thông báo mức Trung bình. Trước đó hệ thống đã nhắc theo CFG-M08-07 (FR-027); món an toàn được dùng theo FR-037a.
- **Không có món thay thế phù hợp** cho một người ở một bữa khi chốt: suất đặc biệt mang dấu "thiếu món thay thế"; bác sĩ được thông báo mức Trung bình để chỉ định món thay thế cho người đó (FR-036). Suất này không giao được cho tới khi có món thay thế hoặc bác sĩ xác nhận món trong suất không xung đột. Ngoài giờ hành chính của bác sĩ, hệ thống tự dùng món an toàn của chế độ ăn nếu không xung đột; nếu vẫn xung đột, điều dưỡng phụ trách được báo để xử lý tại tầng (FR-037a).
- **Dị ứng mới hoặc dị ứng Đã loại trừ sau thời điểm chốt**: hệ thống tính lại suất đặc biệt của người đó cho các bữa đã chốt chưa phục vụ và tạo phát sinh; xác nhận phục vụ (FR-049) dùng dị ứng hiện hành tại lúc phục vụ, nên vẫn chặn được nếu bếp chưa kịp đổi.
- **Chuyển giường sang tầng khác sau thời điểm chốt**: phát sinh "−1" ở phiếu tầng cũ, "+1" ở phiếu tầng mới; nếu người đó có suất đặc biệt thì suất chuyển theo.
- **Người bán trú đến ngoài lịch** (buổi phát sinh, Q-140): nếu điểm danh Có mặt trước thời điểm chốt thì được tính; nếu sau thì tạo phát sinh.
- **Ngày khu bán trú nghỉ** (Q-140): người bán trú có lịch trùng ngày nghỉ không được tính suất; nếu ngày nghỉ được khai báo sau thời điểm chốt thì tạo phát sinh "−" (User Story 4 kịch bản 13).
- **Hoạt động bên ngoài có bữa ăn ngoài viện**: chuyến đi đã lên lịch trùng giờ bữa làm người tham gia không được tính suất; nếu chuyến bị hủy sau chốt thì tạo phát sinh "+".
- **Qua đời hoặc Kết thúc lưu trú sau chốt**: phát sinh "−1"; bản gán chế độ ăn Hiệu lực chuyển Ngừng, đề xuất đang chờ chuyển Đã hủy (FR-015).
- **Người thân ở lại kết thúc sớm hoặc lượt ở lại bị hủy sau chốt**: phát sinh "−1"; suất ăn người thân của các bữa chưa tới giờ chuyển Đã hủy để feature 010 hủy khoản chi phí (FR-043).
- **Bếp chốt sau giờ bữa dự kiến** (**sửa, 2026-10-06, Q-306**): lệnh Chốt suất vẫn được thực hiện để có số liệu và bản ghi nguồn chi phí; phiếu được lập ở Đã chốt kèm dấu "chốt sau giờ bữa", đi đủ vòng đời phiếu (chuẩn bị, giao, nhận), và bác sĩ, bếp được thông báo mức Trung bình ngay khi chốt; thông báo giao trễ FR-048a không áp cho phiếu này. Hệ thống không chốt thay bếp.
- **Món bị Ngừng hiệu lực trong danh mục khi đang nằm trong thực đơn đã công bố**: thực đơn giữ nguyên món; nhân viên bếp được thông báo để đổi món nếu cần (Q-308). Món đã ngừng không chọn được cho thực đơn mới hay lệnh đổi món.
- **Dị ứng loại "khác" không kiểm tra tự động** (feature 001 FR-018a): không tham gia so khớp tự động; người đó luôn nằm trong danh sách cần đối chiếu khi phục vụ (FR-036a), không hiện với bếp; đồ ăn gia đình của người đó luôn Cần xác nhận.
- **Hai lệnh gần như đồng thời trên cùng một đối tượng** (hai bác sĩ cùng áp dụng hoặc hủy một đề xuất chế độ ăn, cùng xác nhận một món hoặc cùng công bố, trả lại một thực đơn, cùng xác nhận đồ ăn nghi vấn, cùng chuyển trạng thái phiếu): chỉ lệnh đầu tiên được ghi nhận; lệnh sau bị từ chối kèm thông báo đối tượng đã đổi trạng thái (theo feature 000 FR-040).

## Requirements *(mandatory)*

### Functional Requirements

Mọi yêu cầu dưới đây kế thừa feature 000: nhóm dữ liệu (mục 1.5), lý do bắt buộc cho lệnh nhóm 2, nhật ký cho mọi thay đổi, tham số theo mã CFG, "hoặc toàn bộ, hoặc không" khi một lệnh có nhiều tác động, Bộ lập lịch chạy lại không tạo trùng, và mọi mốc thời gian theo DBR-25. Phân nhóm dữ liệu của feature này:

| Dữ liệu | Nhóm (1.5) | Thao tác được phép |
| --- | --- | --- |
| Danh mục chế độ ăn, loại đồ ăn bị cấm; thành phần thực phẩm trong danh mục dị nguyên | 1 – Danh mục | Tạo, sửa, Ngừng hiệu lực; không xóa khi đã được tham chiếu |
| Danh mục món ăn | 1 – Danh mục có xác nhận | **(Sửa, 2026-10-06, Q-308)** Chỉ qua lệnh ở bảng trạng thái món ăn (mục A); không xóa khi đã được tham chiếu |
| Danh mục bữa ăn | 1 – Danh mục | Tạo, sửa, Ngừng hiệu lực (Quản lý viện) |
| Giờ bữa dự kiến, ngưỡng giao trễ | 1 – Tham số CFG-M08-04 | Cấu hình (Quản lý viện, feature 000) |
| Bản gán chế độ ăn của người cao tuổi | 2 | Chỉ qua lệnh ở bảng mục B; không sửa nội dung, thay đổi bằng bản gán mới |
| Thực đơn tuần | 2 | Nội dung sửa được khi Nháp; sau đó chỉ qua lệnh ở bảng mục D; đổi món sau công bố bằng lệnh Đổi món (FR-031) |
| Lần chốt suất của một bữa, suất ăn theo chế độ ăn, danh sách người được tính | 3 | Chỉ ghi thêm; thay đổi sau chốt là bản ghi phát sinh |
| Phát sinh sau chốt | 3 | Chỉ ghi thêm; bếp ghi thêm xác nhận đã nhận |
| Suất ăn người thân (bản ghi nguồn chi phí) | 2 | Lập kèm lệnh Chốt suất hoặc phát sinh "+"; hủy kèm phát sinh "−" (FR-043) |
| Phiếu bữa ăn, suất đặc biệt | 2 | Chỉ qua lệnh ở bảng mục F |
| Sai lệch phiếu | 3 | Chỉ ghi thêm; bếp ghi thêm cách xử lý |
| Xác nhận phục vụ suất đặc biệt | 3 | Chỉ ghi thêm; đính chính theo feature 000 |
| Đồ ăn gia đình mang vào | 2 | Chỉ qua lệnh ở bảng mục H |
| Bản ghi lưu mẫu thức ăn | 3 | Chỉ ghi thêm |
| Tham số CFG-M08-01, 03 → 07 | 1 – Tham số | Cấu hình (Quản lý viện, feature 000) |

**Thuật ngữ của spec** (dùng thống nhất ở mọi FR, bảng, kịch bản, thông báo; đã ghi vào 2.4 của tài liệu nguồn):

- **Bản gán chế độ ăn**: chế độ ăn, kết cấu, hạn chế riêng áp cho một người cao tuổi trong một khoảng thời gian (mục B).
- **Món an toàn**: món do bác sĩ khai sẵn cho mỗi chế độ ăn, dùng khi thiếu món thay thế hoặc thiếu món (FR-037a).
- **Suất đặc biệt**: suất có tên người vì cần món thay thế, kết cấu khác thường hoặc món chỉ định riêng (FR-036).
- **Danh sách cần đối chiếu khi phục vụ**: người có dị ứng không kiểm tra tự động, chỉ nhân viên tại tầng thấy (FR-036a).
- **Phát sinh**: thay đổi sau thời điểm chốt; **cần giao** (bếp xác nhận, chuẩn bị, giao) hoặc **chỉ để ghi nhận** (FR-039, FR-040a).
- **Suất giữ**: suất của người chưa trở về đúng dự kiến, giữ tại tầng tới hết ngưỡng giao trễ (FR-040a).
- **Phần bổ sung**: suất của phát sinh cần giao được giao sau khi phiếu đã Đã giao (FR-048b).
- **Yêu cầu suất bổ sung**: yêu cầu của tầng sau giờ bữa cho người đang có mặt (FR-040).
- **Người nhận tại tầng**: người được kiểm đếm, nhận phiếu và phần bổ sung (FR-048).

#### A. Danh mục

- **FR-001**: Bác sĩ MUST quản lý được danh mục chế độ ăn, mỗi chế độ ăn gồm: tên, mô tả, dấu "liên quan điều trị", danh sách thành phần hoặc nhóm thực phẩm bị hạn chế của chế độ ăn (chọn từ danh mục dị nguyên, ví dụ "đường" với chế độ tiểu đường), món an toàn (FR-037a), trạng thái. Danh mục MUST có đúng một chế độ ăn được đánh dấu **mặc định của viện**; chế độ ăn mặc định MUST NOT có dấu "liên quan điều trị" và MUST NOT bị Ngừng hiệu lực. Món an toàn là trường bắt buộc: chế độ ăn MUST NOT được tạo hay lưu khi chưa có món an toàn thỏa FR-005. Chế độ ăn đã có trước khi có quy tắc này mà thiếu món an toàn MUST được bác sĩ bổ sung trước khi dùng cho bản gán mới, và FR-037a coi trường hợp thiếu như "món an toàn cũng xung đột". *(Nguồn: 12.1, 12.2, BR-M08-03; Q-147)*
- **FR-002**: Chế độ ăn đang có bản gán Hiệu lực, Chờ hiệu lực hoặc đang có món trong thực đơn chưa hết tuần MUST NOT bị Ngừng hiệu lực. Thay đổi dấu "liên quan điều trị" hoặc danh sách hạn chế của một chế độ ăn MUST chỉ áp cho bản gán và kiểm tra lập sau thời điểm sửa, và MUST được ghi nhật ký. Khi danh sách hạn chế của một chế độ ăn được sửa, hệ thống MUST kiểm tra lại FR-005 với các thực đơn chưa hết tuần; món vi phạm MUST được báo cho bác sĩ và nhân viên bếp để Bếp Đổi món (FR-031, **sửa, 2026-10-06, Q-308**), thực đơn không tự đổi. *(Nguồn: 1.5 nhóm 1; suy ra từ FR-005)*
- **FR-003**: Bác sĩ MUST thêm được thành phần thực phẩm vào danh mục dị nguyên dùng chung với nhóm "thực phẩm" (feature 001 FR-018a); thành phần đã được một mục dị ứng, món ăn hay hạn chế tham chiếu MUST NOT bị xóa, chỉ Ngừng hiệu lực. *(Nguồn: feature 001 FR-018a, 1.5 nhóm 1)*
- **FR-004**: **(Sửa, 2026-10-06, Q-308)** Nhân viên bếp MUST tạo, sửa được món ăn, mỗi món gồm: tên, các thành phần (chọn từ danh mục dị nguyên; thành phần chưa có trong danh mục thì ghi đề nghị trong ghi chú để Bác sĩ thêm theo FR-003), các chế độ ăn phù hợp, các kết cấu có thể chế biến (thường / mềm / xay nhuyễn), ghi chú, trạng thái. Mỗi món MUST có ít nhất một thành phần, hoặc dấu rõ ràng "không có thành phần nào thuộc danh mục dị nguyên". Món đã được một thực đơn tham chiếu MUST NOT bị xóa, chỉ Ngừng hiệu lực; món đã ngừng MUST NOT chọn được cho thực đơn mới hoặc lệnh đổi món. Món đang là món an toàn của một chế độ ăn còn hiệu lực MUST NOT bị Ngừng hiệu lực cho tới khi chế độ ăn đó được khai món an toàn khác. Nội dung mới của một món (khi Bác sĩ xác nhận bản sửa, FR-004a) MUST chỉ áp cho lần kiểm tra và lần chốt sau thời điểm xác nhận, và MUST kích hoạt kiểm tra lại các thực đơn chưa hết tuần có món đó (FR-020) và FR-005 với chế độ ăn nhận món đó làm món an toàn. *(Nguồn: 12.1, 1.5 nhóm 1, Q-308)*
- **FR-004a**: **(Mới, 2026-10-06, Q-308)** Món MUST qua xác nhận của Bác sĩ trước khi dùng: chỉ món ở trạng thái Hiệu lực mới chọn được cho thực đơn (món, món thay thế), lệnh Đổi món, chỉ định món thay thế riêng (FR-037) và món an toàn (FR-001). Bác sĩ MUST xác nhận hoặc trả lại (lý do bắt buộc) món Chờ xác nhận, sau khi xem thành phần, chế độ ăn phù hợp và kết cấu. Sửa thành phần, chế độ ăn phù hợp hoặc kết cấu của món Hiệu lực MUST tạo một **bản sửa chờ xác nhận**; trong lúc chờ, mọi kiểm tra, chốt suất và phục vụ MUST dùng nội dung đã xác nhận. Sửa tên hoặc ghi chú không cần xác nhận lại. Mỗi món MUST có tối đa một bản sửa chờ xác nhận. *(Nguồn: 12.1, Q-308)*

**Bảng trạng thái món ăn** *(12.1, Q-308; quyền theo Phụ lục 27 dòng "Món ăn, thực đơn")*:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái kế tiếp | Ai thực hiện | Điều kiện (chặn nếu không đạt) | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Tạo món | Nháp | Nhân viên bếp | — | — |
| Nháp | Gửi xác nhận | Chờ xác nhận | Nhân viên bếp | Có thành phần hoặc dấu "không có thành phần trong danh mục"; có ít nhất một chế độ ăn phù hợp | Báo Bác sĩ |
| Chờ xác nhận | Xác nhận | Hiệu lực | Bác sĩ | — | Báo người gửi |
| Chờ xác nhận | Trả lại | Nháp | Bác sĩ | Lý do | Báo người gửi |
| Chờ xác nhận | Rút lại | Nháp | Nhân viên bếp | — | — |
| Hiệu lực | Gửi bản sửa | Hiệu lực (có bản sửa chờ xác nhận) | Nhân viên bếp | Chưa có bản sửa chờ; như dòng Gửi xác nhận | Báo Bác sĩ; nội dung đã xác nhận vẫn dùng |
| Hiệu lực (có bản sửa chờ xác nhận) | Xác nhận bản sửa | Hiệu lực | Bác sĩ | — | Nội dung mới áp từ thời điểm xác nhận; kiểm tra lại theo FR-004; báo người gửi |
| Hiệu lực (có bản sửa chờ xác nhận) | Trả lại bản sửa; rút bản sửa | Hiệu lực | Bác sĩ (trả lại, có lý do); Nhân viên bếp (rút) | — | Bỏ bản sửa; báo người gửi khi trả lại |
| Hiệu lực | Ngừng hiệu lực | Ngừng hiệu lực | Nhân viên bếp; Bác sĩ | Lý do; món không là món an toàn của chế độ ăn còn hiệu lực | Thực đơn đã công bố giữ món; báo Bếp để đổi món nếu cần (Edge Cases) |
| Nháp | Hủy | Đã hủy | Nhân viên bếp | Chưa từng Hiệu lực | — |

Ngừng hiệu lực và Đã hủy là trạng thái cuối. Món đã có trước Q-308 được coi là Hiệu lực.
- **FR-005**: Một món MUST chỉ được xếp vào thực đơn của chế độ ăn nằm trong danh sách "chế độ ăn phù hợp" của món, và MUST NOT chứa thành phần nằm trong danh sách hạn chế của chế độ ăn đó. *(Nguồn: 12.1, BR-M08-02)*
- **FR-006**: Quản lý viện MUST quản lý được **danh mục bữa ăn** (nhóm 1): tên bữa, loại lưu trú được phục vụ (nội trú, bán trú), trạng thái. Giờ bữa dự kiến của từng bữa và ngưỡng giao trễ là tham số CFG-M08-04 (mặc định \[Theo cơ sở / 30 phút\]). Bữa đã có lần chốt MUST NOT bị xóa, chỉ Ngừng hiệu lực; đổi giờ bữa MUST chỉ áp cho các bữa chưa chốt. *(Nguồn: CFG-M08-04, BR-M08-12, 1.5 nhóm 1)*
- **FR-007**: Quản lý viện MUST quản lý được danh mục **loại đồ ăn bị cấm** theo chính sách của cơ sở (ví dụ đồ uống có cồn, thực phẩm sống, thực phẩm không nhãn), dùng cho đối chiếu đồ ăn gia đình (mục H). *(Nguồn: 12.4 "vi phạm chính sách của cơ sở")*

#### B. Chế độ ăn gán cho người cao tuổi

- **FR-010**: Mỗi bản gán chế độ ăn MUST gồm: người cao tuổi, chế độ ăn, kết cấu thức ăn (thường / mềm / xay nhuyễn), danh sách hạn chế thực phẩm riêng (chọn từ danh mục dị nguyên, ví dụ kiêng thịt bò), ghi chú nhu cầu dinh dưỡng, thời điểm hiệu lực mong muốn, lý do, lý do chuyên môn (bắt buộc trong các trường hợp FR-013), người lập, nguồn (lập mới / sau thông báo ăn kém kéo dài hoặc sụt cân, FR-059; **sửa, 2026-10-06, Q-307**), trạng thái. Nội dung MUST NOT sửa được sau khi rời trạng thái Đề xuất; thay đổi MUST bằng một bản gán mới. *(Nguồn: 12.1, 1.5 nhóm 2, UC-45)*
- **FR-011**: Mỗi người cao tuổi MUST có tối đa một bản gán Hiệu lực tại một thời điểm. Khi bản gán mới chuyển Hiệu lực, bản gán Hiệu lực cũ MUST chuyển Ngừng trong cùng một lệnh. *(Nguồn: 12.1, ERD "áp dụng", BR-M08-03)*
- **FR-012**: Mỗi người cao tuổi MUST có tối đa một bản gán ở trạng thái Đề xuất hoặc Chờ hiệu lực. Muốn lập bản gán khác, bác sĩ MUST hủy bản đang chờ trước. *(Suy ra từ BR-M08-03; sửa 2026-10-06, Q-283)*
- **FR-013**: **(Viết lại, 2026-10-06, Q-283)** Bản gán do Bác sĩ lập và áp dụng; không có bước duyệt riêng, không có trạng thái Chờ duyệt hay Từ chối. Bản gán mới, so với bản gán Hiệu lực hiện có (hoặc với chế độ ăn mặc định nếu chưa có), thỏa ít nhất một trường hợp sau thì lệnh Áp dụng MUST bắt buộc **lý do chuyên môn**: (a) chế độ ăn mới mang dấu "liên quan điều trị" và khác chế độ ăn hiện có; (b) chế độ ăn hiện có mang dấu "liên quan điều trị" và chế độ ăn mới khác nó; (c) chế độ ăn giữ nguyên, mang dấu "liên quan điều trị", và kết cấu chuyển sang mức cứng hơn — ba mức kết cấu là danh sách cố định theo 12.5, thứ tự từ mềm tới cứng: xay nhuyễn → mềm → thường; (d) chế độ ăn giữ nguyên, mang dấu "liên quan điều trị", và danh sách hạn chế riêng mới thiếu ít nhất một hạn chế của bản gán hiện có (thay một hạn chế bằng hạn chế khác cũng tính là bỏ). Mọi bản gán khác, gồm cả thêm hạn chế riêng hoặc chuyển sang kết cấu mềm hơn khi đang dùng chế độ ăn liên quan điều trị, được áp dụng không cần lý do chuyên môn. Hệ thống MUST NOT gửi thông báo cho bác sĩ đã lập bản gán trước đó. *(Nguồn: BR-M08-03, 12.1; Q-145, Q-283)*
- **FR-014**: Khi người cao tuổi Đang lưu trú không có bản gán Hiệu lực, hệ thống MUST dùng chế độ ăn mặc định của viện (FR-001), kết cấu "thường", không có hạn chế riêng, và MUST gắn dấu "chưa gán chế độ ăn" cho các suất của người đó; bác sĩ MUST được thông báo khi người đó Hoàn tất tiếp nhận và ở mỗi lần chốt còn thiếu. Việc thiếu bản gán MUST NOT chặn Hoàn tất tiếp nhận. *(Suy ra từ 5.6 "sinh … suất ăn từ ngày hiệu lực"; điều kiện của feature 001 không gồm chế độ ăn)*
- **FR-015**: Khi người cao tuổi chuyển Kết thúc lưu trú hoặc Qua đời, bản gán Hiệu lực MUST chuyển Ngừng và bản gán Đề xuất, Chờ hiệu lực MUST chuyển Đã hủy, với lý do theo sự kiện nguồn. Trạng thái Tạm vắng, Điều trị tại bệnh viện, Hoạt động bên ngoài MUST NOT làm đổi bản gán. *(Nguồn: 5.6)*
- **FR-016**: Khi lập bản gán, hệ thống MUST đối chiếu theo FR-020 với thực đơn của chế độ ăn mới trong tuần hiện tại và các tuần đã công bố; mỗi xung đột MUST được hiển thị cho bác sĩ cùng việc đã có hay chưa có món thay thế phù hợp. Xung đột chưa có món thay thế MUST yêu cầu bác sĩ xác nhận đã xem để tiếp tục; không chặn. Nếu chế độ ăn mới không có món ở bữa nào trong thực đơn đã công bố, hệ thống MUST cảnh báo tương tự (FR-038). *(Nguồn: BR-M08-02 "khi phân bổ")*
- **FR-017**: Thời điểm hiệu lực thực tế MUST là thời điểm hiệu lực mong muốn nếu lệnh áp dụng xảy ra trước đó, và là thời điểm của lệnh nếu xảy ra muộn hơn; tác động MUST NOT tính lùi. Nếu thời điểm hiệu lực thực tế rơi sau thời điểm chốt của một bữa chưa tới giờ, hệ thống MUST tạo phát sinh cho bữa đó (FR-039). *(Theo feature 000 FR-037; BR-M08-01)*
- **FR-018**: *(Bỏ, 2026-10-06, Q-283)* Không còn bản gán Chờ duyệt nên không có nhắc theo feature 000 FR-031a.

**Bảng trạng thái bản gán chế độ ăn** *(12.1 "(Bổ sung)", BR-M08-03; viết lại 2026-10-06, Q-283; quyền theo Phụ lục 27 dòng "Chế độ ăn")*:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái kế tiếp | Ai thực hiện | Điều kiện (chặn nếu không đạt) | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Lập đề xuất | Đề xuất | Bác sĩ | FR-012; người cao tuổi chưa ở trạng thái cuối; chế độ ăn còn hiệu lực | Chạy đối chiếu FR-016 |
| Đề xuất | Áp dụng | Hiệu lực hoặc Chờ hiệu lực | Bác sĩ | Có lý do chuyên môn khi thuộc FR-013 (a)–(d); đã xác nhận các cảnh báo của FR-016 | Theo FR-011, FR-017; Chờ hiệu lực nếu thời điểm hiệu lực ở tương lai |
| Đề xuất | Hủy | Đã hủy | Bác sĩ | Lý do | — |
| Chờ hiệu lực | Tới thời điểm hiệu lực | Hiệu lực | Hệ thống (Bộ lập lịch) | — | Bản gán cũ chuyển Ngừng trong cùng lệnh (FR-011); phát sinh nếu cần (FR-017) |
| Chờ hiệu lực | Hủy | Đã hủy | Bác sĩ | Lý do | Bản gán Hiệu lực hiện có giữ nguyên |
| Hiệu lực | Có bản gán mới Hiệu lực | Ngừng | Hệ thống | — | FR-011 |
| Hiệu lực | Kết thúc lưu trú, Qua đời | Ngừng | Hệ thống | — | FR-015 |
| Đề xuất, Chờ hiệu lực | Kết thúc lưu trú, Qua đời | Đã hủy | Hệ thống | — | FR-015 |

Ngừng, Đã hủy là trạng thái cuối. *(Trạng thái Chờ duyệt, Từ chối và lệnh Gửi duyệt, Duyệt, Từ chối của bản gán bỏ, 2026-10-06, Q-283.)* Không có lệnh Ngừng trực tiếp cho bản gán Hiệu lực; đổi chế độ ăn luôn bằng bản gán mới, nên người Đang lưu trú không rơi vào tình trạng không có chế độ ăn (FR-014 chỉ áp cho người chưa từng được gán).

#### C. Đối chiếu với dị ứng và hạn chế

- **FR-020**: **Xung đột** của một món với một người cao tuổi MUST được xác định khi món có ít nhất một thành phần trùng với: (a) mục dị ứng Hiệu lực của người đó (feature 001), so khớp theo mục danh mục dị nguyên; (b) hạn chế thực phẩm riêng trong bản gán Hiệu lực; (c) danh sách hạn chế của chế độ ăn đang dùng. Mục dị ứng loại "khác" (không kiểm tra tự động, feature 001 FR-018a) MUST NOT tham gia so khớp. Việc đối chiếu MUST chạy khi: lập và duyệt bản gán (FR-016); xác nhận món hoặc bản sửa (FR-004a); gửi kiểm tra và công bố thực đơn (FR-025, FR-029); đổi món (FR-031); thêm hoặc loại trừ một mục dị ứng (FR-021); sửa thành phần món (FR-004); chốt suất (FR-035); xác nhận phục vụ (FR-049); ghi nhận đồ ăn gia đình (FR-053). *(Nguồn: BR-M08-02, BR-M08-04, BR-M08-07, BR-M01-07)*
- **FR-021**: Khi feature 001 báo một mục dị ứng mới được ghi nhận (feature 001 FR-020), hệ thống MUST đối chiếu với thực đơn của chế độ ăn đang dùng trong các tuần chưa hết (Công bố, Đã áp dụng) và với đồ ăn gia đình Được sử dụng của người đó. Mỗi bữa có món xung đột mà không có món thay thế phù hợp MUST dẫn tới yêu cầu feature 007 tạo cảnh báo "xung đột dị ứng với chế độ ăn, thực đơn" mức Trung bình (gộp theo BR-M05-02), nêu bữa và món (mức Trung bình theo nguyên tắc feature 009 FR-043b: cần xử lý trong ngày); bác sĩ MUST được thông báo. Các bữa đã chốt chưa phục vụ MUST được tính lại suất đặc biệt và tạo phát sinh (FR-039). Khi một mục dị ứng chuyển Đã loại trừ, hệ thống MUST tính lại suất đặc biệt từ lần chốt kế tiếp và báo feature 007 nguồn của cảnh báo xung đột tương ứng đã được xử lý. *(Nguồn: BR-M01-07, BR-M08-02; feature 007 FR-026, FR-032)*
- **FR-022**: Kết quả đối chiếu hiển thị cho bác sĩ MUST nêu người, bữa, món và thành phần xung đột; kết quả gửi tới bếp MUST chỉ gồm các trường của FR-046, không nêu dị ứng, hạn chế hay thành phần xung đột. *(Nguồn: 19.3, 12.5)*

#### D. Thực đơn tuần

- **FR-023**: Mỗi thực đơn MUST gắn đúng một tuần (thứ 2 → chủ nhật). Mỗi tuần MUST có tối đa một thực đơn không ở trạng thái Đã hủy. Thực đơn MUST NOT được tạo cho tuần đã kết thúc. *(Nguồn: 12.2 "(Bổ sung)", THUC_DON)*
- **FR-024**: Nội dung thực đơn MUST gồm, với mỗi ngày, mỗi bữa (FR-006), mỗi chế độ ăn: danh sách món; và với mỗi món: danh sách món thay thế theo thứ tự ưu tiên. Món và món thay thế MUST là món Hiệu lực (FR-004a) và thỏa FR-005 với chế độ ăn đó. Nhân viên bếp MUST tạo, sửa được nội dung khi thực đơn ở Nháp, và MUST sao chép được nội dung từ một thực đơn trước làm bản nháp; món không còn Hiệu lực trong bản sao MUST được đánh dấu để thay (**sửa, 2026-10-06, Q-308**). *(Nguồn: 12.2, BR-M08-02)*
- **FR-025**: Khi gửi kiểm tra, hệ thống MUST chạy trong một lần và hiển thị đầy đủ: (a) **kiểm tra độ phủ** (BR-M08-06): mỗi chế độ ăn **đang có người sử dụng** MUST có ít nhất một món ở mọi bữa của mọi ngày trong tuần mà bữa đó phục vụ loại lưu trú của ít nhất một người dùng chế độ ăn (FR-006; chế độ ăn mặc định xét với mọi bữa vì người thân ở lại dùng nó); thiếu thì MUST chặn và liệt kê từng cặp (chế độ ăn, ngày, bữa) bị thiếu; (b) **cảnh báo lặp món** (FR-026); (c) **cảnh báo dị ứng** (BR-M08-07): món có xung đột (FR-020) với ít nhất một người đang sử dụng chế độ ăn đó mà không có món thay thế nào phù hợp với người đó, nêu món, bữa, số người và tên người (tên chỉ hiển thị cho người có quyền xem dị ứng; nhân viên bếp chỉ thấy số người, 19.3). Cảnh báo (b), (c) MUST NOT chặn nhưng MUST được nhân viên bếp xác nhận đã xem trước khi thực đơn chuyển Chờ công bố (**sửa, 2026-10-06, Q-308**); danh sách cảnh báo đã xác nhận MUST được lưu cùng thực đơn để Bác sĩ xem khi duyệt và Quản lý viện xem (FR-029). *(Nguồn: BR-M08-06, BR-M08-07, 12.2)*
- **FR-025a**: **(Mới, 2026-10-06, Q-308)** Trước khi gửi kiểm tra, hệ thống MUST hiển thị cho nhân viên bếp: tồn kho hiện hành của các nguyên liệu (feature 019, 12.7) và số người đang dùng từng chế độ ăn trong tuần của thực đơn (theo FR-027, không kèm tên người, không kèm dị ứng). Lệnh Gửi kiểm tra MUST bắt buộc nhân viên bếp xác nhận "đã kiểm tra khả năng chế biến, nguyên liệu, suất ăn" và MAY kèm ghi chú; xác nhận, ghi chú, người xác nhận và thời điểm MUST được lưu cùng thực đơn và hiển thị cho Bác sĩ khi duyệt. Hệ thống MUST NOT tự đối chiếu nhu cầu nguyên liệu theo định lượng món hay tự trừ kho (Q-220). *(Nguồn: 12.2, Q-220, Q-308)*
- **FR-026**: **Lặp món** MUST được xác định khi cùng một món xuất hiện ở cùng một chế độ ăn trong CFG-M08-03 (mặc định \[3 ngày\]) ngày liên tiếp trở lên, không phân biệt bữa; món xuất hiện ở nhiều bữa trong cùng một ngày tính là một ngày; "cùng một món" là cùng một mục trong danh mục món ăn, các món khác mục không được gom dù tên giống nhau; chuỗi ngày MUST được tính qua ranh giới tuần với thực đơn Công bố hoặc Đã áp dụng của tuần liền trước và liền sau. Món thay thế MUST NOT tính vào lặp món. *(Nguồn: BR-M08-07, CFG-M08-03; cách tính "không phân biệt bữa" và "qua ranh giới tuần" là giả định)*
- **FR-027**: "Chế độ ăn đang có người sử dụng" của một tuần MUST gồm: chế độ ăn mặc định của viện (luôn có người dùng vì người thân ở lại và người chưa gán, FR-014); và mọi chế độ ăn có ít nhất một bản gán Hiệu lực hoặc Chờ hiệu lực, của người cao tuổi chưa ở trạng thái cuối, có hiệu lực giao với tuần đó, tại thời điểm kiểm tra. Khi tới thời điểm CFG-M08-07 (mặc định \[2 ngày trước ngày đầu tuần\], BR-M08-16) mà tuần kế tiếp chưa có thực đơn Công bố, nhân viên bếp MUST được nhắc mức Trung bình, Bác sĩ MUST được nhắc mức Trung bình nếu thực đơn đang Chờ công bố, và Quản lý viện MUST được báo mức Nhẹ (**sửa, 2026-10-06, Q-308**). *(Nguồn: BR-M08-06, BR-M08-16, CFG-M08-07 ở Phụ lục 25)*
- **FR-028**: Khi thực đơn chuyển Công bố, nhân viên bếp (người gửi và Bếp đang trong ca) MUST được thông báo; thực đơn Công bố hoặc Đã áp dụng MUST được cung cấp cho feature 012 dưới dạng **thực đơn chung**: món theo ngày và bữa của chế độ ăn mặc định và danh sách tên chế độ ăn, không kèm người dùng hay món thay thế theo người. *(Nguồn: 12.3, feature 012 FR-046)*
- **FR-029**: **(Viết lại, 2026-10-06, Q-308)** Khi thực đơn chuyển Chờ công bố, Bác sĩ MUST được thông báo. Bác sĩ duyệt về chuyên môn, có xem nội dung, các cảnh báo đã xác nhận (FR-025) và xác nhận kiểm tra của Bếp (FR-025a), bằng một trong hai lệnh: **Công bố**, hoặc **Trả lại** về Nháp kèm lý do bắt buộc, nhân viên bếp được báo lý do. Nhân viên bếp MUST NOT công bố. Khi công bố, hệ thống MUST chạy lại kiểm tra độ phủ và cảnh báo (FR-025) với dữ liệu tại thời điểm công bố; thiếu độ phủ thì chặn; cảnh báo mới phát sinh so với lần gửi kiểm tra MUST được Bác sĩ xác nhận đã xem. Quản lý viện MUST xem được thực đơn cùng các cảnh báo đã xác nhận; Quản lý viện không duyệt. Thực đơn Chờ công bố không dùng nhắc của feature 000 FR-031a; việc chậm duyệt được nhắc theo FR-027 (CFG-M08-07). *(Nguồn: 12.2, UC-46, Phụ lục 27; Q-142, Q-308)*
- **FR-030**: Khi tuần của thực đơn bắt đầu (00:00 ngày thứ 2), Bộ lập lịch MUST chuyển thực đơn Công bố sang Đã áp dụng. Thực đơn được công bố khi tuần đã bắt đầu MUST chuyển thẳng Đã áp dụng. *(Nguồn: 12.2 "(Bổ sung)")*
- **FR-031**: **(Sửa, 2026-10-06, Q-308)** Thực đơn Công bố hoặc Đã áp dụng MUST chỉ thay đổi qua lệnh **Đổi món** của nhân viên bếp, chỉ dùng món Hiệu lực (FR-004a), với các trường: ngày, bữa, chế độ ăn, món trước, món sau (hoặc thêm, bớt món; thêm, bớt món thay thế), lý do bắt buộc. Lệnh MUST NOT áp cho bữa đã qua giờ bữa dự kiến. Lệnh MUST được áp dụng ngay, không cần duyệt, lưu lịch sử trước/sau, và Bác sĩ MUST được thông báo; nếu lệnh xảy ra sau thời điểm chốt của bữa đó, thay đổi MUST được đánh dấu phát sinh (FR-039). *(Nguồn: BR-M08-08)*
- **FR-032**: Lệnh Đổi món MUST chạy lại kiểm tra độ phủ cho bữa bị đổi và MUST bị chặn nếu làm một chế độ ăn đang có người sử dụng mất món ở bữa đó; MUST chạy lại cảnh báo lặp món và dị ứng như FR-025 (b), (c). Lệnh Đổi món MUST dùng được để bổ sung món cho chế độ ăn chưa có món ở một bữa (Edge Cases). *(Nguồn: BR-M08-06, BR-M08-07, BR-M08-08)*

**Bảng trạng thái thực đơn tuần** *(12.2, BR-M08-06 → 08; Q-308; quyền theo Phụ lục 27 dòng "Món ăn, thực đơn")*:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái kế tiếp | Ai thực hiện | Điều kiện (chặn nếu không đạt) | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Tạo thực đơn | Nháp | Nhân viên bếp | FR-023 | — |
| Nháp | Gửi kiểm tra | Chờ công bố | Nhân viên bếp | Mọi món Hiệu lực (FR-024); đã xác nhận kiểm tra chế biến, nguyên liệu, suất ăn (FR-025a); kiểm tra độ phủ đạt; đã xác nhận mọi cảnh báo (FR-025) | Báo Bác sĩ (FR-029) |
| Nháp | Hủy | Đã hủy | Nhân viên bếp | Lý do | — |
| Chờ công bố | Rút lại | Nháp | Nhân viên bếp | — | Báo Bác sĩ |
| Chờ công bố | Trả lại | Nháp | Bác sĩ | Lý do | Báo nhân viên bếp kèm lý do |
| Chờ công bố | Công bố | Công bố, hoặc Đã áp dụng nếu tuần đã bắt đầu | Bác sĩ | Kiểm tra độ phủ chạy lại đạt; đã xác nhận cảnh báo mới (FR-029) | Thông báo nhân viên bếp; cung cấp thực đơn chung cho feature 012 (FR-028) |
| Công bố | Tới 00:00 thứ 2 của tuần | Đã áp dụng | Hệ thống (Bộ lập lịch) | — | — |
| Công bố, Đã áp dụng | Đổi món | Giữ nguyên | Nhân viên bếp | FR-031, FR-032 | Lịch sử thay đổi; thông báo Bác sĩ; phát sinh nếu sau chốt |

Đã hủy là trạng thái cuối. Thực đơn Đã áp dụng giữ nguyên trạng thái sau khi tuần kết thúc và chỉ còn để tra cứu. Thực đơn Công bố hay Đã áp dụng MUST NOT bị hủy.

#### E. Chốt suất và phát sinh

- **FR-033**: **(Viết lại, 2026-10-06, Q-306)** Với mỗi bữa trong danh mục (FR-006), Nhân viên bếp MUST thực hiện được lệnh **Chốt suất** từ thời điểm giờ bữa dự kiến trừ CFG-M08-01 (mặc định \[2 giờ\]); lệnh trước mốc đó MUST bị chặn. Khi bếp mở lệnh, hệ thống MUST tính sẵn danh sách người được tính và số suất (FR-034, FR-035) theo dữ liệu tại thời điểm đó; thời điểm thực hiện lệnh là **thời điểm chốt** dùng cho FR-039. Mỗi bữa MUST chỉ được chốt một lần; lệnh chốt thứ hai MUST bị chặn (feature 000 FR-040). Tới mốc mà bữa chưa chốt, hệ thống MUST nhắc mọi Nhân viên bếp đang trong ca; hệ thống MUST NOT tự chốt. Bếp chốt sau giờ bữa dự kiến thì lần chốt mang dấu "chốt sau giờ bữa" (Edge Cases). *(Nguồn: BR-M08-01, CFG-M08-01, Q-306)*
- **FR-034**: Danh sách người được tính suất của một bữa MUST gồm, theo dữ liệu tại thời điểm chốt: (a) người cao tuổi nội trú (dài hạn, ngắn ngày) ở trạng thái Đang lưu trú, trừ người có lượt vắng hoặc chuyến hoạt động ngoài viện đã lên lịch trùng giờ bữa dự kiến; người đang Điều trị tại bệnh viện không được tính; người đang Tạm vắng hoặc Hoạt động bên ngoài không được tính, **trừ khi** lượt vắng hoặc chuyến đi có thời điểm dự kiến trở lại (feature 004 FR-048, feature 014) không muộn hơn giờ bữa dự kiến: khi đó người đó được tính và suất mang dấu **"dự kiến trở về"** (FR-040a); lượt vắng hoặc chuyến đi không có thời điểm dự kiến trở lại thì người đó không được tính; (b) người bán trú có lịch đến hôm đó bao trùm giờ bữa dự kiến, bữa đó phục vụ bán trú (FR-006), ngày đó không phải ngày khu bán trú nghỉ, và trạng thái có mặt là Chưa đến hoặc Có mặt; cộng người bán trú đã điểm danh Có mặt ở buổi phát sinh ngoài lịch (Q-140); (c) người thân có lượt ở lại **Đang ở lại**, có đăng ký ăn, khoảng ở lại bao trùm giờ bữa dự kiến (feature 012 FR-042 và kịch bản "Bắt đầu ở lại" của feature 012: feature 011 tính người thân vào số suất khi lượt chuyển Đang ở lại); lượt Đã xác nhận chưa bắt đầu không được tính, và nếu lượt bắt đầu sau thời điểm chốt nhưng trước giờ bữa thì tạo phát sinh "+1" (FR-039). Mỗi người MUST được gắn chế độ ăn, kết cấu và tầng/khu: người nội trú theo giường đang phân bổ (feature 003); người bán trú theo khu bán trú; người thân ở lại theo tầng của người cao tuổi được gắn, với chế độ ăn mặc định của viện. *(Nguồn: BR-M08-01, BR-M08-09, 3.4, 5.5)*
- **FR-035**: Kết quả chốt MUST gồm: số suất theo từng (tầng/khu, chế độ ăn), danh sách người được tính, và với mỗi người: có phải suất đặc biệt không và nội dung suất đặc biệt (FR-036). Kết quả chốt MUST truy ngược được từ mỗi con số về danh sách người. *(Nguồn: BR-M08-01, SUAT_AN, 1.3)*
- **FR-036**: Suất của một người cao tuổi là **suất đặc biệt** khi có ít nhất một: (a) một món ở bữa đó của chế độ ăn người đó dùng có xung đột (FR-020) với người đó; khi đó hệ thống MUST chọn món thay thế đầu tiên trong danh sách ưu tiên (FR-024) không xung đột với người đó; không có món nào phù hợp thì suất mang dấu **"thiếu món thay thế"** và bác sĩ MUST được thông báo mức Trung bình; (b) kết cấu khác "thường"; (c) bác sĩ đã chỉ định món thay thế riêng cho người đó ở bữa đó (FR-037). Suất của người thân ở lại MUST NOT là suất đặc biệt. *(Nguồn: 12.5, BR-M08-02; định nghĩa là giả định vì tài liệu nguồn chưa nêu)*
- **FR-036a**: Người cao tuổi có mục dị ứng loại "khác" (không kiểm tra tự động, feature 001 FR-018a) MUST có mặt trong **danh sách cần đối chiếu khi phục vụ** của bữa, dù suất của họ là suất thường hay suất đặc biệt. Danh sách này MUST chỉ hiển thị cho nhân viên chăm sóc, điều dưỡng, trưởng tầng trong phạm vi (người có quyền xem dị ứng theo feature 002), MUST NOT xuất hiện trên phiếu, nhãn hay bất kỳ nội dung nào bếp thấy (19.3). Người trong danh sách MUST được xác nhận phục vụ như suất đặc biệt (FR-049), kèm nhắc hỏi lại dị ứng ghi trong hồ sơ. *(Nguồn: feature 001 FR-018a, 19.3, BR-M08-14)*
- **FR-037**: Bác sĩ MUST chỉ định được món thay thế riêng cho một người ở một bữa chưa qua giờ bữa dự kiến, kèm lý do; món chỉ định MUST thỏa FR-005 và MUST NOT xung đột với người đó. Chỉ định sau thời điểm chốt MUST tạo phát sinh. Suất mang dấu "thiếu món thay thế" MUST NOT được đánh dấu chuẩn bị cho tới khi có chỉ định, hoặc bác sĩ xác nhận kèm lý do rằng món gốc được chế biến không có thành phần xung đột. *(Nguồn: BR-M08-02 "bếp nhận danh sách người cần món thay thế")*
- **FR-037a**: Mỗi chế độ ăn MUST có một **món an toàn** do bác sĩ khai (FR-001), là món trong danh mục thỏa FR-005 với chế độ ăn đó. Món an toàn áp dụng khi suất của một người mang dấu "thiếu món thay thế", hoặc chế độ ăn của người đó không có món ở bữa (dấu "chưa có món trong thực đơn", hay tuần chưa có thực đơn công bố, FR-038), tại một trong hai thời điểm: (i) lúc chốt suất hoặc tạo phát sinh, nếu thời điểm đó nằm ngoài giờ hành chính của bác sĩ (CFG-M13-06, feature 009 FR-016a); (ii) lúc còn CFG-M08-05 (mặc định \[30 phút\]) trước giờ bữa dự kiến mà bác sĩ vẫn chưa xử lý, kể cả trong giờ hành chính. Việc áp món an toàn ở thời điểm (ii) là chuyển trạng thái theo giờ được Q-307 cho phép (Q-333). Khi đó hệ thống MUST tự dùng món an toàn của chế độ ăn làm món thay thế nếu món đó không xung đột với người nhận (FR-020); suất mang dấu "món an toàn" thay cho "thiếu món thay thế", không bị chặn chuẩn bị, và bác sĩ MUST được thông báo mức Nhẹ để xem lại. Nếu món an toàn cũng xung đột, suất giữ dấu "thiếu món thay thế" và điều dưỡng phụ trách người đó MUST được thông báo mức Trung bình để xử lý tại tầng (ví dụ dùng đồ ăn khác phù hợp đã có); bác sĩ vẫn được thông báo theo FR-036. Trước mốc (ii) trong giờ hành chính, việc xử lý theo FR-037. Món an toàn được tự dùng là một thay đổi nội dung suất nên tạo phát sinh nếu xảy ra sau thời điểm chốt (FR-039). *(Nguồn: BR-M08-02; Clarification 2026-09-27, đề xuất Q-147; mốc (ii) và trường hợp không có món là suy ra từ Q-147, để suất không bị chặn tới giờ bữa)*
- **FR-038**: Nếu chế độ ăn của một người không có món ở bữa đó trong thực đơn, hoặc tuần chưa có thực đơn Công bố, lệnh Chốt suất MUST NOT bị chặn; suất liên quan mang dấu "chưa có món trong thực đơn" hoặc phiếu mang dấu "chưa có thực đơn công bố"; bác sĩ, bếp (và Quản lý viện với trường hợp chưa có thực đơn) MUST được thông báo mức Trung bình. *(Suy ra từ BR-M08-01, BR-M08-06)*
- **FR-039**: Mọi thay đổi xảy ra sau thời điểm chốt và trước giờ bữa dự kiến làm đổi danh sách người được tính, chế độ ăn, kết cấu, tầng/khu hay nội dung suất đặc biệt của một người, hoặc làm đổi món của bữa (FR-031), MUST tạo một bản ghi **phát sinh** gắn bữa, gồm: tầng/khu, chế độ ăn, số suất thay đổi (+/−), người liên quan (nếu là suất đặc biệt), loại thay đổi, sự kiện nguồn, thời điểm. Phát sinh MUST được gửi tới bếp và MUST có trạng thái "bếp đã xác nhận" (người, thời điểm). Các sự kiện nguồn MUST gồm ít nhất: Ghi nhận trở về, Cho tạm vắng, chuyển Điều trị tại bệnh viện, điểm danh rời viện hoặc trở về của chuyến ngoài viện, hủy chuyến, thay đổi trạng thái có mặt bán trú, Hoàn tất tiếp nhận, Kết thúc lưu trú, Qua đời, chuyển giường, bản gán chế độ ăn chuyển Hiệu lực, thêm hoặc loại trừ mục dị ứng, bắt đầu, kết thúc sớm hay hủy lượt người thân ở lại, Đổi món, chỉ định món thay thế, sửa thành phần một món có trong bữa (FR-004), khai báo ngày khu bán trú nghỉ (Q-140), đổi thời điểm dự kiến trở lại của lượt vắng hay chuyến đi. Khi một thay đổi ảnh hưởng nhiều bữa (ví dụ bữa phụ gần bữa chính), hệ thống MUST tạo một phát sinh riêng cho mỗi bữa đã qua thời điểm chốt mà chưa tới giờ bữa dự kiến; bữa chưa tới thời điểm chốt nhận thay đổi qua lần chốt của nó. Phát sinh và sai lệch chưa xử lý MUST không phụ thuộc ca bếp: mọi nhân viên bếp đang trong ca đều xử lý được, vì ca bếp là ca toàn viện không có bàn giao (feature 008). *(Nguồn: BR-M08-01, BR-M08-08, BR-M08-13, SUAT_AN, feature 008)*
- **FR-040**: Sau giờ bữa dự kiến, hệ thống MUST NOT tự tạo phát sinh cho bữa đó. Người nhận tại tầng (FR-048) MUST lập được **yêu cầu suất bổ sung** cho một người đang có mặt, kèm lý do, cho tới hết ngưỡng giao trễ của bữa (CFG-M08-04); yêu cầu này MUST được ghi và gửi bếp như một phát sinh. *(Suy ra từ BR-M08-01; giả định)*
- **FR-040a**: Với suất mang dấu "dự kiến trở về" (FR-034 (a)): nếu người đó được Ghi nhận trở về hoặc điểm danh trở về trước giờ bữa dự kiến, dấu được gỡ và không tạo phát sinh; nếu tới giờ bữa dự kiến vẫn chưa trở về, hệ thống MUST tạo phát sinh "−1" (chuyển trạng thái theo giờ được Q-307 cho phép, Q-333) cho tầng/khu và chế độ ăn của người đó (ngoại lệ của FR-040), gắn sự kiện nguồn "chưa trở về đúng dự kiến", và suất của người đó MUST được đánh dấu **"suất giữ"** trên phiếu. Phát sinh này chỉ để ghi nhận số suất: không cần bếp xác nhận, không áp FR-041 và không chặn giao phiếu (FR-047). Suất giữ được giữ tại tầng tới hết ngưỡng giao trễ CFG-M08-04: nếu người đó trở về trong khoảng này, người nhận tại tầng ghi "phục vụ suất giữ", hệ thống tạo phát sinh "+1" không cần bếp chuẩn bị; hết khoảng này, suất giữ được người nhận tại tầng ghi hủy bỏ. Người đó trở về muộn hơn thì dùng yêu cầu suất bổ sung theo FR-040. Giờ dự kiến trở về thay đổi sau thời điểm chốt MUST được xét lại theo FR-039. *(Nguồn: BR-M08-01 "số người dự kiến có mặt trong bữa"; Clarification 2026-09-27)*
- **FR-041**: Phát sinh chưa được bếp xác nhận khi còn CFG-M08-05 (mặc định \[30 phút\]) trước giờ bữa dự kiến MUST được nhắc cho bếp và bác sĩ; phát sinh tạo sau mốc đó MUST được nhắc ngay khi tạo. *(Nguồn: BR-M08-13, CFG-M08-05)*
- **FR-042**: Hệ thống MUST cung cấp cho feature 005 **lịch bữa** (feature 005 FR-017 gọi là "lịch ăn") của từng người cao tuổi Đang lưu trú (bữa, giờ dự kiến, có suất đặc biệt hay không), làm căn cứ để Người phụ trách ca lập công việc "hỗ trợ ăn" (**sửa, 2026-10-06, Q-306**); và MUST báo feature 005 khi một bữa của người đó bị bỏ vì vắng mặt, để công việc tương ứng chuyển Hủy (feature 005 FR-017, FR-022). *(Nguồn: 5.6 "hủy … suất ăn", 8.3)*
- **FR-043**: Với mỗi người thân ở lại được tính ở một bữa, lệnh Chốt suất MUST ghi một **suất ăn người thân** gồm: người thân, lượt ở lại, người cao tuổi được gắn, bữa, ngày của bữa, trạng thái (Đã ghi / Đã hủy). Suất ăn người thân MUST là bản ghi nguồn chi phí cho feature 010 (DBR-15). Suất được thêm bằng phát sinh "+" MUST ghi thêm suất ăn người thân. Khi phát sinh "−" của người thân xảy ra trước giờ bữa dự kiến, suất ăn người thân MUST chuyển Đã hủy và feature 010 MUST được báo. Sau giờ bữa dự kiến, suất ăn người thân MUST NOT bị hủy vì người thân không ăn. Suất ăn người thân Đã hủy MUST được giữ lại, không bị xóa, để truy vết nguồn chi phí. *(Nguồn: BR-M10-04, BR-M08-09, feature 010 FR-005, feature 012 FR-042 (c))*

#### F. Phiếu bữa ăn

- **FR-044**: **(Sửa, 2026-10-06, Q-306)** Lệnh Chốt suất MUST lập đúng một phiếu bữa ăn cho mỗi tầng/khu có ít nhất một suất; phiếu gồm số suất theo từng chế độ ăn và danh sách suất đặc biệt. Khi một phát sinh "+" thuộc một tầng/khu chưa có phiếu của bữa đó, lệnh bếp xác nhận phát sinh đó MUST lập phiếu cho tầng/khu đó. Tổng suất trên các phiếu của một bữa MUST bằng số suất đã chốt cộng các phát sinh **cần giao** (DBR-27), tức là số suất bếp đã chuẩn bị và giao. Phát sinh **chỉ để ghi nhận** (FR-040a: "−1" khi tạo suất giữ, "+1" khi phục vụ suất giữ) MUST NOT làm đổi tổng này, vì suất giữ vẫn nằm ở tầng; chúng chỉ làm đổi **số suất được phục vụ** của bữa, được tính riêng bằng số suất đã chốt cộng mọi phát sinh. Tầng/khu không có suất nào thì không có phiếu; đây là cách hiểu "mỗi (bữa, tầng/khu) có đúng một phiếu" của DBR-27 cho tầng/khu có suất (điểm báo lại 5). *(Nguồn: BR-M08-09, DBR-27)*
- **FR-045**: Mỗi suất đặc biệt MUST có trạng thái "đã chuẩn bị" do bếp đánh dấu, và MUST có **nhãn** gồm họ tên, phòng, chế độ ăn, kết cấu và món thay thế (nếu có) để nhân viên chăm sóc đối chiếu khi phục vụ. *(Nguồn: 12.5 "Nhãn suất đặc biệt", BR-M08-10)*
- **FR-046**: Nội dung phiếu và nhãn hiển thị cho bếp MUST chỉ gồm: tầng/khu, bữa, số suất theo chế độ ăn, và với suất đặc biệt: họ tên, phòng, chế độ ăn, món thay thế, kết cấu. Các dấu bếp thấy MUST là danh sách đóng sau: ở mức phiếu — "chưa có thực đơn công bố", "chốt sau giờ bữa"; ở mức suất hoặc chế độ ăn — "chưa gán chế độ ăn" (FR-014), "thiếu món thay thế" (FR-036), "món an toàn" (FR-037a), "chưa có món trong thực đơn" (FR-038), "dự kiến trở về", "suất giữ" (FR-040a). Phiếu MUST NOT chứa dị ứng, bệnh lý, thành phần xung đột, lý do của món thay thế, danh sách cần đối chiếu khi phục vụ (FR-036a) hay thông tin sức khỏe khác. *(Nguồn: 12.5, 19.3, feature 002 FR-022)*
- **FR-047**: Bếp MUST chuyển phiếu sang Đã giao chỉ khi mọi suất đặc biệt trên phiếu đã được đánh dấu chuẩn bị, mọi phát sinh của tầng/khu đó cho bữa đó đã được bếp xác nhận, và bữa đã có bản ghi lưu mẫu (FR-061, Q-39); khi giao, hệ thống MUST ghi người giao và thời điểm giao. Phát sinh chỉ để ghi nhận số suất (FR-040a) không thuộc điều kiện này. *(Nguồn: BR-M08-10, BR-M08-15; điều kiện về phát sinh suy ra từ BR-M08-13 "phải được bếp xác nhận đã nhận")*
- **FR-048**: **Người nhận tại tầng** là Trưởng tầng, Điều dưỡng hoặc Nhân viên chăm sóc có phạm vi phân công tại tầng/khu của phiếu trong ca đang diễn ra (feature 002 FR-034, giờ ca ± CFG-M15-07); Người phụ trách ca của tầng/khu đó cũng là người nhận. Bất kỳ người nào trong nhóm này MUST kiểm đếm rồi xác nhận Đã nhận, hoặc báo Có sai lệch kèm nội dung bắt buộc; mỗi phiếu chỉ có một lần xác nhận Đã nhận, lệnh sau bị từ chối (feature 000 FR-040). Nhân viên ngoài nhóm hoặc ngoài phạm vi MUST NOT nhận phiếu. Với phiếu của khu bán trú (khu nghỉ chung của viện, feature 003, Q-45), người nhận là nhân viên thuộc nhóm trên có phân công trong ca tại tầng/khu vực mà khu nghỉ bán trú gắn vào (feature 003 FR-038); nếu tầng/khu vực đó không có ai được phân công trong ca, thông báo ở FR-048a gửi Quản lý viện thay Trưởng tầng. Mọi sai lệch và cách bếp xử lý MUST được lưu lịch sử. *(Nguồn: BR-M08-11, UC-76, 4.4 chú thích ¹⁵; Clarification 2026-09-27, đề xuất Q-143)*
- **FR-048a**: Quá giờ bữa dự kiến cộng ngưỡng giao trễ CFG-M08-04 (mặc định \[30 phút\]) mà phiếu chưa ở Đã giao, Đã nhận hoặc Có sai lệch, hệ thống MUST gửi thông báo mức Nhẹ cho Trưởng tầng của tầng đó (hoặc Người phụ trách ca nếu tầng chưa có Trưởng tầng, 2.4) và bếp, một lần cho mỗi phiếu. Phiếu của lần chốt sau giờ bữa (Edge Cases) không dùng thông báo này vì đã được báo mức Trung bình khi chốt. *(Nguồn: BR-M08-12; người nhận thay theo 2.4 "Người phụ trách ca", Q-90)*

**Bảng trạng thái phiếu bữa ăn** *(12.5, BR-M08-09 → 12; quyền theo 4.4 dòng "Chuẩn bị, giao phiếu, lưu mẫu", "Nhận phiếu, báo sai lệch")*:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái kế tiếp | Ai thực hiện | Điều kiện (chặn nếu không đạt) | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Chốt suất (FR-033) | Đã chốt | Nhân viên bếp | FR-044 | Phiếu được lập cùng lệnh |
| — | Xác nhận phát sinh "+" của tầng/khu chưa có phiếu | Đã chốt | Nhân viên bếp | FR-044 | Phiếu được lập cùng lệnh |
| Đã chốt | Đánh dấu chuẩn bị xong | Đã chuẩn bị | Nhân viên bếp | Mọi suất đặc biệt đã chuẩn bị; không còn suất "thiếu món thay thế" chưa xử lý (FR-037) | — |
| Đã chuẩn bị | Có phát sinh làm thêm hoặc đổi suất đặc biệt | Đã chốt | Hệ thống | — | Bếp chuẩn bị lại phần thay đổi |
| Đã chuẩn bị | Giao | Đã giao | Nhân viên bếp | FR-047 | Ghi người giao, thời điểm giao |
| Đã giao | Xác nhận nhận | Đã nhận | Người nhận tại tầng (FR-048) | Phạm vi tầng/khu và ca | Ghi người nhận, thời điểm nhận |
| Đã giao | Báo sai lệch | Có sai lệch | Người nhận tại tầng (FR-048) | Nội dung sai lệch | Lưu sai lệch; thông báo bếp mức Trung bình |
| Có sai lệch | Ghi xử lý sai lệch | Đã giao | Nhân viên bếp | Cách xử lý (bổ sung suất / đổi suất / khác kèm mô tả) | Lưu người, thời điểm xử lý; thông báo người đã báo sai lệch |
| Đã giao, Có sai lệch, Đã nhận | Có phát sinh hoặc yêu cầu suất bổ sung (FR-040) | Giữ nguyên | Hệ thống | — | Phát sinh được giao và nhận riêng (FR-048b) |

Đã nhận là trạng thái cuối của phiếu.

- **FR-048b**: Phát sinh cần giao thêm (phát sinh "+" hoặc đổi suất) tới sau khi phiếu đã Đã giao MUST được giao như một **phần bổ sung** của phiếu, không đưa phiếu quay lại trạng thái trước. Mỗi phần bổ sung MUST có: phát sinh nguồn, bếp đã xác nhận (FR-039), người và thời điểm giao, người và thời điểm nhận. Người nhận tại tầng MUST xác nhận nhận được phần bổ sung, hoặc báo sai lệch trên phần bổ sung; sai lệch này được bếp xử lý và lưu như sai lệch của phiếu. Sau khi phiếu Đã nhận, thiếu suất phát hiện thêm MUST được xử lý bằng yêu cầu suất bổ sung (FR-040); suất sai đã phục vụ MUST được xử lý theo FR-049 hoặc ghi sự cố theo feature 007; phiếu không mở lại. *(Nguồn: BR-M08-10, BR-M08-11, BR-M08-13 áp cho phần bổ sung; luồng chính 12.5 coi Đã nhận là trạng thái cuối)*

#### G. Phục vụ suất đặc biệt

- **FR-049**: Trước khi ghi kết quả ăn uống cho một người có suất đặc biệt ở một bữa, nhân viên chăm sóc MUST xác nhận phục vụ: chọn suất theo nhãn và người đang được phục vụ. Hệ thống MUST so: (a) suất có đúng là của người đó; (b) món trong suất (món thay thế nếu có, món gốc nếu không) có xung đột với dị ứng Hiệu lực và hạn chế **hiện hành** của người được phục vụ tại lúc phục vụ. Nếu (a) sai mà (b) không có xung đột, hệ thống MUST chặn với lý do "suất không đúng người", không tạo sự cố. **(Sửa, 2026-10-06, Q-307)** Nếu (b) có xung đột **với dị ứng Hiệu lực**, hệ thống MUST chặn, yêu cầu feature 007 tạo cảnh báo mức Trung bình "phục vụ suất có thành phần gây dị ứng" cho điều dưỡng phụ trách người đó, và yêu cầu người ghi khai người cao tuổi đã ăn hay chưa. Hệ thống MUST NOT tự tạo sự cố. Khai "đã ăn" → người ghi MUST ghi nhận sự cố loại "phục vụ sai suất ăn", nguồn phát sinh "ăn uống", mức theo triệu chứng ở feature 007 trước khi hoàn tất xác nhận; xác nhận phục vụ trỏ về sự cố đó. Mức mặc định của loại này là Trung bình; chọn mức thấp hơn (kể cả khi đã ăn mà chưa có triệu chứng) MUST ghi lý do theo feature 007 FR-042. Khai "chưa ăn" → không có sự cố; người ghi MAY ghi sự cố thủ công theo feature 007. Nếu (b) chỉ xung đột với **hạn chế** (hạn chế riêng hoặc hạn chế của chế độ ăn), hệ thống MUST chặn với lý do "suất trái hạn chế", không tạo sự cố; người ghi MAY ghi sự cố thủ công theo feature 007. Người thuộc danh sách cần đối chiếu khi phục vụ (FR-036a) MUST được nhắc hỏi lại dị ứng ghi trong hồ sơ trước khi xác nhận. *(Nguồn: BR-M08-14, 9.1, feature 007 FR-040, FR-042, FR-043, Q-307)*
- **FR-050**: Feature 005 MUST NOT cho ghi kết quả ăn uống của một bữa có suất đặc biệt, hoặc của người thuộc danh sách cần đối chiếu khi phục vụ (FR-036a), khi chưa có xác nhận phục vụ thành công của bữa đó cho người đó. Người có suất thường không thuộc danh sách đó không cần xác nhận phục vụ. Yêu cầu này cần được đưa vào feature 005 (điểm báo lại 10). *(Nguồn: BR-M08-14 "trước khi ghi nhận kết quả ăn uống (8.6)")*
- **FR-051**: Xác nhận phục vụ là ghi nhận nhóm 3 gắn tầng/khu; sai sót MUST xử lý bằng đính chính theo feature 000. *(Nguồn: 1.5)*

#### H. Đồ ăn gia đình mang vào

- **FR-052**: Điều dưỡng hoặc bác sĩ MUST ghi nhận được đồ ăn gia đình mang vào, gồm: người gửi (chọn người thân có quan hệ Hiệu lực, hoặc ghi họ tên và quan hệ nếu không có trong danh sách), người cao tuổi, loại đồ ăn, thành phần chính (chọn từ danh mục dị nguyên, MAY chọn "không rõ"), thời gian nhận, số lượng, tình trạng (nguyên niêm phong / đã mở / tự chế biến; hạn dùng nếu có), ghi chú. *(Nguồn: 12.4, UC-48, DO_AN_GIA_DINH)*
- **FR-053**: Ngay khi ghi nhận, hệ thống MUST đối chiếu và gán trạng thái: **Không sử dụng** nếu loại đồ ăn thuộc danh mục bị cấm (FR-007), hạn dùng đã qua (hạn dùng in trên đồ ăn, hoặc hạn dùng do người xác nhận ghi với đồ ăn tự chế biến, FR-055), hoặc thành phần xung đột (FR-020) với người cao tuổi — lý do ghi rõ loại vi phạm; ngược lại **Cần xác nhận** nếu có điểm nghi vấn (FR-054); ngược lại **Được sử dụng**. *(Nguồn: BR-M08-04, 12.4)*
- **FR-054**: Điểm nghi vấn MUST gồm ít nhất: thành phần "không rõ" hoặc có thành phần loại "khác"; người cao tuổi có mục dị ứng loại "khác"; kết cấu của người cao tuổi khác "thường"; tình trạng "đã mở" hoặc "tự chế biến"; người gửi không có trong danh sách người thân có quan hệ Hiệu lực; người cao tuổi đang dùng chế độ ăn liên quan điều trị (vì hạn chế của chế độ ăn điều trị như muối, chất béo không luôn thể hiện được bằng thành phần). *(Suy ra từ BR-M08-04 "còn nghi vấn", 12.4 "không phù hợp với các hạn chế"; danh sách là giả định)*
- **FR-055**: Đồ ăn Cần xác nhận MUST chỉ chuyển Được sử dụng khi điều dưỡng hoặc bác sĩ xác nhận, kèm ghi chú; người xác nhận MUST thấy toàn bộ điểm nghi vấn. Với đồ ăn không có hạn dùng in sẵn (tự chế biến, đã mở), người xác nhận MUST ghi hạn dùng. Khi đồ ăn Được sử dụng tới hạn dùng, hệ thống MUST chuyển nó sang Không sử dụng với lý do "hết hạn dùng" và thông báo điều dưỡng phụ trách mức Nhẹ. Đồ ăn Không sử dụng MUST NOT chuyển sang Được sử dụng bằng bất kỳ lệnh nào; nếu cần, người ghi phải ghi nhận lại như một lần gửi mới sau khi thông tin được làm rõ. *(Nguồn: BR-M08-04)*
- **FR-056**: Khi một mục dị ứng mới được ghi nhận hoặc bản gán chế độ ăn mới chuyển Hiệu lực, hệ thống MUST đối chiếu lại đồ ăn Được sử dụng của người đó; nếu có xung đột, đồ ăn MUST chuyển Không sử dụng với lý do tương ứng và điều dưỡng phụ trách MUST được thông báo mức Trung bình. *(Suy ra từ BR-M08-04, BR-M01-07)*
- **FR-057**: Khi đồ ăn chuyển Không sử dụng, nếu người gửi là người thân có tài khoản và có quan hệ Hiệu lực với người cao tuổi (người gửi chỉ ghi họ tên tự do thì không gửi), hệ thống MUST thông báo người đó mức Nhẹ với nội dung loại "chung" (đồ ăn không được sử dụng theo quy định của viện, cách nhận lại); lý do liên quan dị ứng, chế độ ăn MUST là phần loại "sức khỏe" (feature 009 FR-013). *(Suy ra; feature 009 FR-001, FR-013)*

**Bảng trạng thái đồ ăn gia đình mang vào** *(12.4, BR-M08-04; quyền theo UC-48)*:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái kế tiếp | Ai thực hiện | Điều kiện (chặn nếu không đạt) | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Ghi nhận | Không sử dụng / Cần xác nhận / Được sử dụng | Điều dưỡng, Bác sĩ (hệ thống gán trạng thái theo FR-053) | Đủ trường bắt buộc FR-052 | FR-057 nếu Không sử dụng |
| Cần xác nhận | Xác nhận cho dùng | Được sử dụng | Điều dưỡng, Bác sĩ | Ghi chú | Lưu người, thời điểm xác nhận |
| Cần xác nhận | Không cho dùng | Không sử dụng | Điều dưỡng, Bác sĩ | Lý do | FR-057 |
| Được sử dụng | Dị ứng hoặc chế độ ăn mới gây xung đột | Không sử dụng | Hệ thống | FR-056 | Thông báo điều dưỡng phụ trách |
| Được sử dụng | Tới hạn dùng | Không sử dụng | Hệ thống | FR-055 | Thông báo điều dưỡng phụ trách |
| Không sử dụng | Ghi xử lý | Đã kết thúc | Điều dưỡng, Bác sĩ | Cách xử lý: trả lại người gửi (người nhận lại) / hủy bỏ | — |
| Được sử dụng | Kết thúc | Đã kết thúc | Điều dưỡng, Bác sĩ | Cách kết thúc: đã dùng hết / hủy bỏ (hỏng, hết hạn) / trả lại | — |

Đã kết thúc là trạng thái cuối và giữ lý do, cách xử lý.

#### I. Báo xem lại chế độ ăn

- **FR-058**: **(Viết lại, 2026-10-06, Q-307)** Khi feature 005 báo ăn kém kéo dài (BR-M04-09, feature 005 FR-040) hoặc feature 007 báo sụt cân (BR-M05-04, feature 007 FR-039a) cho một người cao tuổi, hệ thống MUST thông báo bác sĩ mức Nhẹ "xem lại chế độ ăn", kèm người cao tuổi, nguồn (ăn kém kéo dài / sụt cân, bản ghi nguồn) và chế độ ăn hiện hành. Hệ thống MUST NOT tạo yêu cầu, MUST NOT đặt hạn xử lý hay nhắc khi bác sĩ chưa có lệnh. Thông báo này là thông báo duy nhất tới bác sĩ cho sự kiện ăn kém kéo dài; nhắc riêng của feature 005 FR-040 không dùng. *(Nguồn: BR-M08-05, Q-307)*
- **FR-059**: Bác sĩ quyết định giữ nguyên chế độ ăn (không cần lệnh) hoặc lập bản gán chế độ ăn mới theo mục B. Khi lập bản gán mới, bác sĩ MAY chọn tham chiếu thông báo đã nhận; bản gán khi đó lưu nguồn "sau thông báo ăn kém kéo dài / sụt cân" (FR-010). *(Nguồn: BR-M08-05, BR-M08-03, Q-307)*

#### J. Lưu mẫu thức ăn

- **FR-060**: Lưu mẫu thức ăn MUST được quản lý trong hệ thống bằng bản ghi đơn giản (FR-061), mỗi bữa một bản ghi; việc thiếu bản ghi lưu mẫu MUST chặn giao phiếu của bữa đó (FR-047). Hệ thống chỉ ghi nhận, không thay quy định kiểm thực ba bước và lưu mẫu của Bộ Y tế; văn bản hiện hành cần được đối chiếu khi triển khai (12.5). *(Nguồn: 12.5, BR-M08-15; Clarification 2026-09-27, Q-39)*
- **FR-061**: Nhân viên bếp MUST ghi được bản ghi lưu mẫu cho mỗi bữa: danh sách món được nấu trong bữa (danh sách món cần lưu mẫu do hệ thống tính từ thực đơn và kết quả chốt, gồm món của mọi chế độ ăn, món thay thế được dùng, món an toàn được tự dùng (FR-037a), món chỉ định riêng và món thêm qua Đổi món, mỗi món một lần dù xuất hiện ở nhiều chế độ ăn; khi tuần chưa có thực đơn công bố, danh sách chỉ gồm món an toàn được dùng và bếp MUST ghi thêm được các món thực tế đã nấu), với mỗi món: đã lưu mẫu hay không, và lý do bắt buộc nếu không lưu; thời điểm lưu, người lưu; và ghi thêm thời điểm hủy mẫu, người hủy. Phiếu của bữa MUST NOT chuyển Đã giao khi bữa chưa có bản ghi lưu mẫu (FR-047); món không lưu mẫu có lý do MUST NOT chặn giao, nhưng MUST hiển thị cho Quản lý viện và bác sĩ. Phát sinh sau khi đã ghi lưu mẫu làm có thêm món mới (Đổi món, chỉ định món thay thế, món an toàn) MUST cho bếp ghi thêm món đó vào bản ghi. Điều kiện chặn giao của FR-047 vẫn áp cả khi tuần chưa có thực đơn công bố. *(Nguồn: BR-M08-15, UC-78; Clarification 2026-09-27, đề xuất Q-146)*
- **FR-062**: Khi đã quá thời điểm lưu + CFG-M08-06 (mặc định \[24 giờ\]) mà mẫu chưa có thời điểm hủy, bếp MUST được nhắc mức Nhẹ. *(Nguồn: BR-M08-15, CFG-M08-06)*

#### K. Quyền

- **FR-063**: Quyền theo Permission Matrix 4.4 và phạm vi theo vai trò (feature 002 FR-034). Đây là tập lệnh đầy đủ và MUST khớp cột "Ai thực hiện" của các bảng trạng thái:
  - **Bác sĩ** (toàn viện) **(sửa, 2026-10-06, Q-283, Q-308)**: dòng "Chế độ ăn" (T): danh mục chế độ ăn, món an toàn, thành phần thực phẩm; lập, áp dụng, hủy bản gán (không có bước duyệt riêng, FR-013). Dòng "Món ăn, thực đơn" (D): xác nhận, trả lại món và bản sửa, Ngừng hiệu lực món (FR-004a); công bố, trả lại thực đơn (FR-029). Chỉ định món thay thế riêng; ghi nhận, xác nhận, không cho dùng, ghi xử lý và kết thúc đồ ăn gia đình (UC-48); lập bản gán mới sau thông báo xem lại chế độ ăn (FR-059). Xem phiếu, phát sinh, sai lệch, xác nhận phục vụ (X). Bác sĩ xem đầy đủ hồ sơ sức khỏe (19.3). Bác sĩ MUST NOT lập, sửa, gửi thực đơn hay Đổi món.
  - **Điều dưỡng**: xem chế độ ăn, thực đơn (X); ghi nhận, xác nhận, không cho dùng, ghi xử lý và kết thúc đồ ăn gia đình trong phạm vi phân công (UC-48); nhận phiếu, báo sai lệch, lập yêu cầu suất bổ sung trong phạm vi (người nhận tại tầng, FR-048); xem xác nhận phục vụ (X).
  - **Nhân viên chăm sóc**: xác nhận phục vụ suất đặc biệt trong phạm vi phân công (UC-77, T); nhận phiếu, báo sai lệch, lập yêu cầu suất bổ sung (người nhận tại tầng, FR-048).
  - **Trưởng tầng**: xem phiếu, suất đã chốt, phát sinh, xác nhận phục vụ trong phạm vi (X); nhận phiếu, báo sai lệch, lập yêu cầu suất bổ sung (người nhận tại tầng, FR-048). Trưởng tầng MUST NOT xem bản gán chế độ ăn hay thực đơn qua chức năng của feature này (Phụ lục 27 dòng "Chế độ ăn" và "Món ăn, thực đơn": —); tên chế độ ăn, kết cấu, món thay thế mà trưởng tầng thấy trên phiếu thuộc dòng "Suất ăn đã chốt, phiếu bữa ăn" (TT X), không phải hai dòng trên; trưởng tầng thấy thông tin sức khỏe của người cao tuổi trong phạm vi qua hồ sơ (feature 002).
  - **Người phụ trách ca**: nhận phiếu, báo sai lệch, lập yêu cầu suất bổ sung trong tầng/khu và thời gian của ca (FR-048); luôn là Trưởng tầng hoặc Điều dưỡng (2.4).
  - **Nhân viên bếp** (toàn viện) **(sửa, 2026-10-06, Q-308)**: dòng "Món ăn, thực đơn" (T): tạo, sửa, gửi xác nhận, rút lại, hủy, Ngừng hiệu lực món ăn (FR-004, FR-004a); tạo, sửa, sao chép, gửi kiểm tra (kèm xác nhận kiểm tra chế biến, nguyên liệu, suất ăn, FR-025a), rút lại, hủy thực đơn; Đổi món (FR-031). Xem danh mục chế độ ăn và số người dùng từng chế độ ăn, không kèm tên người (X); xem phiếu và phát sinh với giới hạn FR-046 (X¹⁴); Chốt suất (FR-033), đánh dấu chuẩn bị, giao phiếu, ghi xử lý sai lệch, xác nhận phát sinh, ghi lưu mẫu (T). MUST NOT xem bản gán chế độ ăn, dị ứng hay tên người trong cảnh báo dị ứng của thực đơn (FR-025), ngoài những gì hiện trên phiếu; MUST NOT công bố thực đơn hay xác nhận món.
  - **Quản lý viện**: xem toàn bộ (X); cấu hình danh mục bữa, loại đồ ăn bị cấm và tham số (C).
  - **Người thân**: xem thực đơn chung; xem chế độ ăn riêng của người cao tuổi khi có quyền xem sức khỏe có tác dụng (feature 012 FR-046).
  - **Hệ thống / Bộ lập lịch** (**sửa, 2026-10-06, Q-306, Q-307**): tính sẵn số suất khi bếp chốt; nhắc chốt; tạo phát sinh kèm lệnh của actor gây thay đổi; tự dùng món an toàn (FR-037a), tạo phát sinh chỉ để ghi nhận cho suất giữ (FR-040a), tự chuyển đồ ăn gia đình tới hạn dùng sang Không sử dụng (FR-055), và các chuyển trạng thái khác do "Hệ thống" thực hiện ở các bảng trên. Hệ thống MUST NOT chốt suất, lập phiếu ngoài lệnh của bếp, tạo yêu cầu xem lại chế độ ăn hay tạo sự cố.

  Nhân viên hành chính MUST NOT có quyền trong feature này. *(Nguồn: Phụ lục 27 dòng "Chế độ ăn", "Món ăn, thực đơn" (Q-308), "Suất ăn đã chốt, phiếu bữa ăn", "Chuẩn bị, giao phiếu, lưu mẫu", "Nhận phiếu, báo sai lệch", "Xác nhận phục vụ suất đặc biệt"; UC-45 → 48, UC-75 → 78; 19.3)*
- **FR-064**: Mọi thay đổi trên bản gán chế độ ăn, món ăn (gồm xác nhận, trả lại, bản sửa), thực đơn, đồ ăn gia đình và mọi lần chốt, phát sinh MUST được ghi nhật ký (feature 000); giá trị trước/sau có dị ứng, hạn chế MUST chịu giới hạn trường theo vai trò người xem (feature 002 FR-022). *(Nguồn: 19.4, DBR-23)*

#### L. Thông báo

- **FR-065**: Feature này MUST yêu cầu feature 009 gửi đúng các thông báo ở bảng dưới, theo khuôn chung của feature 009 FR-001 và nguyên tắc xếp mức FR-043b. Thông báo tới bếp MUST NOT chứa dị ứng, bệnh lý hay thành phần xung đột (FR-046). Mọi "báo", "thông báo", "nhắc" trong FR và bảng trạng thái của spec này MUST có dòng tương ứng ở bảng dưới. *(Nguồn: 17, BR-M13-01, feature 009)*

| Sự kiện | Người nhận | Mức | Căn cứ |
| --- | --- | --- | --- |
| Người Đang lưu trú chưa gán chế độ ăn | Bác sĩ | Nhẹ | FR-014 |
| Dị ứng mới xung đột với thực đơn, chưa có món thay thế (kèm cảnh báo do feature 007 tạo) | Bác sĩ | Trung bình | FR-021 |
| Chế độ ăn sửa danh sách hạn chế làm món trong thực đơn chưa hết tuần vi phạm FR-005; món bị Ngừng hiệu lực khi đang có trong thực đơn đã công bố | Bác sĩ; nhân viên bếp | Nhẹ | FR-002, Edge Cases |
| Món hoặc bản sửa món được gửi xác nhận | Bác sĩ | Nhẹ | FR-004a |
| Món hoặc bản sửa được xác nhận, bị trả lại (kèm lý do) | Nhân viên bếp người gửi | Nhẹ | FR-004a |
| Thực đơn gửi kiểm tra, chờ duyệt; nhân viên bếp rút lại | Bác sĩ | Nhẹ | FR-029 |
| Thực đơn bị trả lại (kèm lý do) | Nhân viên bếp người gửi | Trung bình | FR-029 |
| Bản gán tự Ngừng hoặc tự Hủy do Kết thúc lưu trú, Qua đời | (không gửi; ghi nhật ký) | — | FR-015 |
| Thực đơn Công bố | Nhân viên bếp | Nhẹ | FR-028 |
| Đổi món | Bác sĩ | Nhẹ; Trung bình nếu Đổi món cho bữa trong ngày | FR-031 |
| Tuần kế tiếp chưa có thực đơn Công bố tại CFG-M08-07 | Nhân viên bếp; Bác sĩ (khi thực đơn đang Chờ công bố); Quản lý viện | Trung bình; Trung bình; Nhẹ | FR-027 |
| Tới mốc chốt (giờ bữa − CFG-M08-01) mà bữa chưa chốt | Nhân viên bếp đang trong ca | Trung bình | FR-033 |
| Chốt suất có suất "thiếu món thay thế", "chưa có món trong thực đơn" hoặc tuần chưa có thực đơn; chốt sau giờ bữa | Bác sĩ, nhân viên bếp; Quản lý viện (chưa có thực đơn) | Trung bình | FR-036, FR-038, Edge Cases |
| Suất được tự dùng món an toàn (ngoài giờ hành chính, hoặc tới mốc CFG-M08-05) | Bác sĩ; nhân viên bếp qua phát sinh nếu sau chốt | Nhẹ; Trung bình (phát sinh) | FR-037a, FR-039 |
| Món an toàn cũng xung đột, hoặc chế độ ăn thiếu món an toàn | Điều dưỡng phụ trách người đó | Trung bình | FR-037a, FR-001 |
| Phần bổ sung, suất giữ, phục vụ hoặc hủy bỏ suất giữ | (không gửi riêng; hiện trên phiếu cho người nhận tại tầng và bếp) | — | FR-040a, FR-048b |
| Bữa có món không lưu mẫu (kèm lý do) | Bác sĩ, Quản lý viện | Nhẹ | FR-061 |
| Phát sinh mới sau chốt; yêu cầu suất bổ sung | Nhân viên bếp | Trung bình | FR-039, FR-040 |
| Phát sinh chưa được bếp xác nhận tại CFG-M08-05 trước giờ bữa | Nhân viên bếp, Bác sĩ | Trung bình | FR-041, BR-M08-13 |
| Phiếu chưa giao quá giờ bữa + ngưỡng CFG-M08-04 | Trưởng tầng (hoặc Người phụ trách ca; Quản lý viện với khu bán trú không có người được phân công), nhân viên bếp | Nhẹ | FR-048a, BR-M08-12 |
| Phiếu hoặc phần bổ sung Có sai lệch; sai lệch đã được xử lý | Nhân viên bếp; người đã báo sai lệch | Trung bình; Nhẹ | Bảng phiếu, FR-048b |
| Phục vụ suất có thành phần gây dị ứng (cảnh báo do feature 007 tạo và gửi) | Điều dưỡng phụ trách | Trung bình | FR-049 |
| Đồ ăn gia đình Không sử dụng | Người gửi là người thân có tài khoản | Nhẹ | FR-057 |
| Đồ ăn đang dùng chuyển Không sử dụng do dị ứng, chế độ ăn mới | Điều dưỡng phụ trách | Trung bình | FR-056 |
| Đồ ăn đang dùng tới hạn dùng | Điều dưỡng phụ trách | Nhẹ | FR-055 |
| Ăn kém kéo dài hoặc sụt cân: xem lại chế độ ăn | Bác sĩ | Nhẹ | FR-058 |
| Mẫu thức ăn quá CFG-M08-06 chưa hủy | Nhân viên bếp | Nhẹ | FR-062 |

### Giao tiếp với feature khác

| Hướng | Feature | Nội dung | Dữ liệu tối thiểu |
| --- | --- | --- | --- |
| Nhận | 001 | Mục dị ứng Hiệu lực và mục chuyển Đã loại trừ; nhu cầu dinh dưỡng trong hồ sơ sức khỏe; trạng thái người cao tuổi, Hoàn tất tiếp nhận, Kết thúc lưu trú, Qua đời | Người cao tuổi, mục danh mục dị nguyên, loại "khác", thời điểm |
| Cung cấp | 001 | Thành phần thực phẩm trong danh mục dị nguyên | Tên, nhóm "thực phẩm" |
| Nhận | 003 | Giường và tầng/khu của người nội trú theo thời điểm; khu bán trú; chuyển giường | Người cao tuổi, phòng, tầng/khu, thời điểm |
| Nhận | 004 | Lượt vắng (đang diễn ra, đã lên lịch), Điều trị tại bệnh viện, ghi nhận trở về; ngày khu bán trú nghỉ; danh mục có đơn giá "suất ăn ngoài hợp đồng" | Người cao tuổi, khoảng thời gian |
| Nhận | 005 | Trạng thái có mặt bán trú theo ngày, kể cả buổi phát sinh; sự kiện ăn kém kéo dài | Người cao tuổi, ngày, trạng thái, bản ghi nguồn |
| Cung cấp | 005 | Lịch bữa từng người (căn cứ để Người phụ trách ca lập công việc "hỗ trợ ăn"), bữa bị bỏ vì vắng mặt; điều kiện "đã xác nhận phục vụ suất đặc biệt" trước khi ghi kết quả ăn uống | Người cao tuổi, bữa, giờ dự kiến, có suất đặc biệt, xác nhận phục vụ |
| Gửi | 007 | Yêu cầu tạo cảnh báo xung đột dị ứng với chế độ ăn, thực đơn; báo nguồn đã xử lý; yêu cầu tạo cảnh báo phục vụ suất có thành phần gây dị ứng; sự cố "phục vụ sai suất ăn" do nhân viên ghi nhận ở feature 007 được tham chiếu từ xác nhận phục vụ | Người cao tuổi, loại, mức, bữa, món, bản ghi nguồn, thời điểm |
| Nhận | 007 | Sự kiện cảnh báo sụt cân | Người cao tuổi, bản ghi nguồn |
| Nhận | 008 | Ca đang diễn ra, phân công theo tầng/khu (người nhận tại tầng, điều dưỡng phụ trách, Người phụ trách ca) | Nhân viên, tầng/khu, ca |
| Gửi | 009 | Các thông báo ở bảng FR-065 | Nguồn, mức, nhóm người nhận, loại thông tin |
| Gửi | 010 | Suất ăn người thân (ghi, hủy) làm bản ghi nguồn chi phí | Suất, người thân, lượt ở lại, người cao tuổi được gắn, ngày bữa, trạng thái |
| Nhận | 012 | Lượt người thân ở lại có đăng ký ăn (xác nhận, bắt đầu, kết thúc, hủy); người thân có quan hệ Hiệu lực (người gửi đồ ăn) | Lượt, người thân, người cao tuổi, khoảng thời gian, có đăng ký ăn |
| Cung cấp | 012 | Thực đơn chung (FR-028); chế độ ăn riêng (loại thông tin "sức khỏe") | Tuần, ngày, bữa, món; người cao tuổi, chế độ ăn, kết cấu |
| Nhận | 014 | Chuyến hoạt động ngoài viện đã lên lịch, người tham gia ở mọi lần thay đổi, giờ về dự kiến và gia hạn, điểm danh rời viện và trở về, đính chính thời điểm hoặc Hủy ghi nhận (feature 014 FR-034, FR-031a), hủy chuyến; giờ về theo ngày của bán trú lấy từ feature 005 (Q-176) | Chuyến, người tham gia, khoảng thời gian |
| Gửi | Module 14 (feature 016) | Dữ liệu cho báo cáo: số phiếu giao trễ, có sai lệch (18.2, định nghĩa ở feature 016 FR-034); số suất theo bữa, chế độ ăn (giai đoạn sau, Q-194) | Phiếu, bữa, tầng/khu, thời điểm giao, sai lệch |

### Key Entities *(include if feature involves data)*

- **Chế độ ăn (CHE_DO_AN)** – nhóm 1 (danh mục, như 3.2); "chế độ ăn" mà 1.5 xếp vào nhóm 2 là **bản gán chế độ ăn** bên dưới: tên, mô tả, dấu "liên quan điều trị", dấu "mặc định của viện", danh sách thành phần hạn chế, món an toàn (FR-037a), trạng thái.
- **Món ăn (MON_AN)** – nhóm 1 có xác nhận: tên, thành phần (dị nguyên), chế độ ăn phù hợp, kết cấu có thể chế biến, ghi chú, người tạo, người xác nhận và thời điểm, lý do trả lại, bản sửa chờ xác nhận (nếu có), lịch sử nội dung đã xác nhận, trạng thái (Nháp / Chờ xác nhận / Hiệu lực / Ngừng hiệu lực / Đã hủy) (Q-308).
- **Loại đồ ăn bị cấm** – nhóm 1: tên, mô tả, trạng thái.
- **Bản gán chế độ ăn** – nhóm 2: người cao tuổi, chế độ ăn, kết cấu, hạn chế riêng, ghi chú nhu cầu dinh dưỡng, thời điểm hiệu lực mong muốn và thực tế, lý do, lý do chuyên môn, người lập, cảnh báo đã xác nhận, nguồn (lập mới / sau thông báo xem lại), trạng thái (Đề xuất / Chờ hiệu lực / Hiệu lực / Ngừng / Đã hủy; Chờ duyệt, Từ chối bỏ, Q-283). Là quan hệ "áp dụng" giữa NGUOI_CAO_TUOI và CHE_DO_AN trong ERD.
- **Danh mục bữa ăn** – nhóm 1: tên bữa, loại lưu trú được phục vụ, trạng thái; giờ bữa và ngưỡng giao trễ theo CFG-M08-04.
- **Thực đơn tuần (THUC_DON)** – nhóm 2: tuần, nội dung (ngày, bữa, chế độ ăn, món, món thay thế theo thứ tự), cảnh báo đã xác nhận, người lập (Nhân viên bếp), xác nhận kiểm tra chế biến, nguyên liệu, suất ăn kèm ghi chú, người xác nhận và thời điểm (FR-025a), người công bố (Bác sĩ), các lần trả lại kèm lý do, trạng thái, lịch sử Đổi món (người đổi, lý do).
- **Lần chốt suất** – nhóm 3: bữa, ngày, thời điểm chốt, người chốt, dấu "chốt sau giờ bữa", danh sách người được tính (người, chế độ ăn, kết cấu, tầng/khu, suất đặc biệt).
- **Suất ăn (SUAT_AN)** – nhóm 3: bữa, tầng/khu, chế độ ăn, số lượng đã chốt.
- **Phát sinh sau chốt** – nhóm 3: bữa, tầng/khu, chế độ ăn, số suất +/−, người liên quan, loại thay đổi, sự kiện nguồn, thời điểm, chỉ để ghi nhận hay cần giao (FR-040a), bếp đã xác nhận (người, thời điểm); với phần bổ sung (FR-048b): người và thời điểm giao, người và thời điểm nhận.
- **Danh sách cần đối chiếu khi phục vụ** – dẫn xuất theo bữa từ dị ứng loại "khác" (FR-036a); không lưu riêng, không hiển thị cho bếp.
- **Suất ăn người thân** – nhóm 2: người thân, lượt ở lại, người cao tuổi được gắn, bữa, ngày, trạng thái. Là nguồn chi phí (DBR-15).
- **Phiếu bữa ăn (PHIEU_BUA_AN)** – nhóm 2: bữa, tầng/khu, số suất theo chế độ ăn, trạng thái, người và thời điểm giao, người và thời điểm nhận.
- **Suất đặc biệt (SUAT_DAC_BIET)** – nhóm 2: phiếu, người cao tuổi, phòng, chế độ ăn, món thay thế, kết cấu, các dấu, đã chuẩn bị, xác nhận phục vụ.
- **Sai lệch phiếu (SAI_LECH_PHIEU)** – nhóm 3: phiếu, nội dung, người báo, thời điểm, cách xử lý, người xử lý, thời điểm xử lý.
- **Xác nhận phục vụ** – nhóm 3: suất đặc biệt, người được phục vụ, người xác nhận, thời điểm, kết quả (đúng / chặn sai người / chặn xung đột), khai đã ăn hay chưa, cảnh báo được tạo, sự cố do người ghi ghi nhận (nếu đã ăn).
- **Đồ ăn gia đình (DO_AN_GIA_DINH)** – nhóm 2: người gửi, người cao tuổi, loại, thành phần, thời gian, số lượng, tình trạng, ghi chú, kết quả đối chiếu, người xác nhận, cách xử lý, trạng thái.
- **Lưu mẫu thức ăn (LUU_MAU_THUC_AN)** – nhóm 3 (Q-39, Q-146): bữa, danh sách món được nấu, với mỗi món đã lưu mẫu hay không và lý do, thời điểm lưu, người lưu, thời điểm hủy, người hủy.
- Dùng từ feature khác: **Mục dị ứng (MUC_SUC_KHOE)**, **Danh mục dị nguyên** (feature 001); **Giường, tầng/khu** (feature 003); **Lượt vắng** (feature 004); **Có mặt bán trú**, **Công việc hỗ trợ ăn** (feature 005); **Cảnh báo, Sự cố** (feature 007); **Lượt ở lại** (feature 012); **Chuyến hoạt động ngoài viện** (feature 014).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Với bộ kiểm thử 7 ngày giả lập cho 60 người cao tuổi (có vắng, nằm viện, chuyến ngoài viện, bán trú có và không có mặt, người thân ở lại, đổi chế độ ăn, chuyển tầng), 100% bữa có số suất tính sẵn khi bếp chốt, theo từng tầng/khu và chế độ ăn, khớp tính tay; 100% lệnh chốt lần hai cho cùng bữa bị chặn; 0 bữa được chốt mà không có lệnh của Nhân viên bếp; 100% bữa chưa chốt tại mốc được nhắc.
- **SC-002**: 100% thay đổi sau thời điểm chốt và trước giờ bữa trong bộ kiểm thử tới bếp dưới dạng phát sinh trong vòng 5 phút, kể cả việc tính lại suất đặc biệt khi dị ứng mới được ghi (FR-021); tổng suất trên phiếu bằng số đã chốt cộng phát sinh ở 100% bữa (DBR-27); 100% phát sinh chưa xác nhận tại mốc CFG-M08-05 được nhắc.
- **SC-003**: 0 thực đơn chuyển Công bố khi còn một chế độ ăn đang có người sử dụng thiếu món ở một bữa; 0 thực đơn công bố không qua lệnh Công bố của Bác sĩ; 0 thực đơn hay lệnh Đổi món dùng món chưa được Bác sĩ xác nhận (Q-308); 100% trường hợp lặp món và món gây dị ứng chưa có món thay thế đã cài sẵn trong bộ kiểm thử được cảnh báo ở lần gửi kiểm tra đầu tiên.
- **SC-004**: 0 bản gán thuộc các trường hợp FR-013 (a)–(d) chuyển Hiệu lực mà không có lý do chuyên môn; tại mọi thời điểm của bộ kiểm thử, mỗi người Đang lưu trú có đúng một chế độ ăn được dùng để tính suất (bản gán Hiệu lực hoặc mặc định có dấu). *(Sửa 2026-10-06, Q-283)*
- **SC-005**: 0 lần suất đặc biệt chứa thành phần gây dị ứng hiện hành của người nhận được ghi nhận phục vụ thành công; 100% lần bị chặn do xung đột tạo đúng một cảnh báo mức Trung bình; 0 sự cố do hệ thống tự tạo; 100% lần khai "đã ăn" có sự cố do người ghi ghi nhận với nguồn "ăn uống" (BR-M08-14, Q-307).
- **SC-006**: Với cơ sở 300 người cao tuổi, lệnh Chốt suất một bữa (tính số suất và lập phiếu của mọi tầng/khu) xong trong không quá 1 phút.
- **SC-007**: 100% đồ ăn gia đình trong bộ kiểm thử có thành phần trùng dị ứng, hạn chế, hoặc thuộc loại bị cấm ở trạng thái Không sử dụng; 0 đồ ăn có điểm nghi vấn chuyển Được sử dụng mà không có xác nhận của điều dưỡng hoặc bác sĩ.
- **SC-008**: 0 lần dị ứng, bệnh lý, thành phần xung đột, hay việc một người có dị ứng không kiểm tra tự động (FR-036a), xuất hiện với nhân viên bếp, kể cả trên phiếu, nhãn, phát sinh và thông báo; 0 lần bác sĩ thấy thông tin sức khỏe ngoài dị ứng, bệnh lý liên quan chế độ ăn, kết quả ăn uống và lượng nước.
- **SC-009**: Nhân viên bếp nhận đầy đủ mọi lỗi độ phủ và cảnh báo của một thực đơn tuần trong một lần gửi kiểm tra; trong đợt thử, một thực đơn 6 chế độ ăn × 3 bữa × 7 ngày được nhân viên bếp lập (từ bản sao tuần trước), sửa và gửi kiểm tra thành công trong không quá 60 phút làm việc, và Bác sĩ duyệt xong trong không quá 15 phút. Đây là mục tiêu nghiệm thu, không phải tham số.
- **SC-010**: 100% sự kiện ăn kém kéo dài và sụt cân trong bộ kiểm thử có đúng một thông báo tới bác sĩ; 0 yêu cầu xem lại chế độ ăn được tạo (Q-307).
- **SC-011**: 100% suất của người thân ở lại có đăng ký ăn tạo đúng một bản ghi nguồn cho feature 010; 100% suất bị hủy trước giờ bữa được báo cho feature 010.
- **SC-012**: Trong bộ kiểm thử có suất "thiếu món thay thế" hoặc "chưa có món trong thực đơn" ở bữa chốt ngoài giờ hành chính và ở bữa bác sĩ không xử lý tới mốc CFG-M08-05, 0 suất còn bị chặn chuẩn bị tới giờ bữa khi món an toàn không xung đột với người nhận; 100% suất có món an toàn xung đột được báo cho điều dưỡng phụ trách trước giờ bữa (FR-037a).
- **SC-013**: 100% bản ghi lưu mẫu trong bộ kiểm thử có đủ mọi món được nấu của bữa, gồm món thay thế và món an toàn, mỗi món có trạng thái đã lưu hoặc lý do không lưu (FR-061); 100% phần bổ sung (FR-048b) có người và thời điểm giao, người và thời điểm nhận hoặc sai lệch được báo; tổng suất trên phiếu khớp DBR-27 và số suất được phục vụ khớp tính tay ở 100% bữa có suất giữ (FR-044, FR-040a).
- **SC-014**: **(Mới, 2026-10-06, Q-308)** 0 lần nhân viên bếp thấy tên người hoặc dị ứng trong cảnh báo của thực đơn, số người theo chế độ ăn hay thông báo; 100% thực đơn Chờ công bố có xác nhận kiểm tra chế biến, nguyên liệu, suất ăn của Bếp; 100% lần trả lại món hoặc thực đơn có lý do.

## Assumptions

- Số feature `011` theo cột Feature của UC-45 → UC-48, UC-75 → UC-78 ở mục 4.2 `docs/phan-tich-yeu-cau.md` và theo cách các spec 001, 004, 005, 007, 010, 012 đã gọi Module 08.
- Mỗi người cao tuổi dùng một chế độ ăn tại một thời điểm (ERD: NGUOI_CAO_TUOI }o--|| CHE_DO_AN). Kết cấu thức ăn và hạn chế thực phẩm riêng là thuộc tính của bản gán, không phải chế độ ăn riêng, nên thay đổi kết cấu hay hạn chế cũng tạo bản gán mới; bản gán đó chỉ bắt buộc lý do chuyên môn trong các trường hợp của FR-013 (sửa, Q-283).
- "Hạn chế thực phẩm" (12.1) được ghi trong bản gán chế độ ăn vì mục 5.2 và feature 001 chỉ quản lý dị ứng, bệnh nền, tiền sử; nhu cầu dinh dưỡng vẫn ở hồ sơ sức khỏe (5.2) và bản gán chỉ có ghi chú.
- Thực đơn là của toàn viện, theo chế độ ăn; "nhóm người cao tuổi" ở 12.2 và "nhóm được phân bổ" ở BR-M08-07 được hiểu là những người dùng cùng một chế độ ăn. Tuần tính từ thứ 2 đến chủ nhật.
- Đổi món sau công bố (BR-M08-08) do nhân viên bếp thực hiện, có hiệu lực ngay khi có lý do, không cần Bác sĩ duyệt lại, vì đổi món thường do thiếu nguyên liệu trong ngày; an toàn được bảo đảm vì chỉ dùng món đã được Bác sĩ xác nhận và kiểm tra tự động chạy lại; Bác sĩ được báo và cùng Quản lý viện xem được lịch sử đổi món (**sửa, 2026-10-06, Q-308**).
- **(Bổ sung, 2026-10-06, Q-308)** "Kiểm tra khả năng chế biến, nguyên liệu, suất ăn" là xác nhận của nhân viên bếp dựa trên số liệu hệ thống hiển thị; hệ thống không có định lượng nguyên liệu theo món nên không tự đánh giá đủ hay thiếu (Q-220).
- Cảnh báo lặp món và dị ứng khi gửi kiểm tra thực đơn không chặn công bố, đúng chữ "cảnh báo" của BR-M08-07; an toàn được bảo đảm tiếp ở chốt suất (FR-036, FR-037) và xác nhận phục vụ (FR-049).
- Suất đặc biệt được định nghĩa ở FR-036 vì 12.5 chỉ nêu nội dung, không nêu tiêu chí. Người có mục dị ứng loại "khác" không được đưa thành suất đặc biệt (để bếp không suy ra việc có dị ứng, 19.3) mà nằm trong danh sách cần đối chiếu khi phục vụ (FR-036a) chỉ nhân viên tại tầng thấy.
- Người được tính suất xác định theo dữ liệu tại thời điểm Nhân viên bếp thực hiện lệnh Chốt suất (**sửa, 2026-10-06, Q-306**). Người Tạm vắng hoặc Hoạt động bên ngoài có giờ dự kiến trở về không muộn hơn giờ bữa được tính sẵn, ưu tiên việc người cao tuổi có suất ăn; suất dư khi người đó về muộn được ghi bằng phát sinh "−1" và giữ tại tầng tới hết ngưỡng giao trễ (FR-040a). Lượt vắng Tạm vắng luôn có thời điểm dự kiến trở lại (feature 004 FR-048); chuyến ngoài viện (feature 014, chưa viết) được giả định có thời điểm này. Người thân ở lại chỉ được tính khi lượt Đang ở lại, theo feature 012, nên không có trường hợp tính phí suất cho người thân không đến.
- Người thân ở lại dùng chế độ ăn mặc định của viện; dị ứng của người thân không được hệ thống quản lý.
- Suất ăn người thân đã qua giờ bữa được tính phí dù người thân không ăn, vì bếp đã chuẩn bị theo đăng ký.
- Thông báo "cảnh báo nhẹ" của BR-M08-12 là thông báo mức Nhẹ qua feature 009, không phải cảnh báo sức khỏe của feature 007, vì không gắn một người cao tuổi.
- Danh mục bữa ăn và loại lưu trú được phục vụ là danh mục nhóm 1; giờ bữa dự kiến và ngưỡng giao trễ là CFG-M08-04; cả hai do Quản lý viện cấu hình. Danh mục loại đồ ăn bị cấm do Quản lý viện quản lý như cấu hình nghiệp vụ (2.3).
- Thời hạn lưu: xác nhận phục vụ, đồ ăn gia đình, bản gán chế độ ăn gắn người cao tuổi nên lưu theo NFR-07 (CFG-M15-03); lần chốt, phiếu, phát sinh, sai lệch, lưu mẫu lưu như nhật ký hệ thống (CFG-M15-04), đủ để truy vết khi có ngộ độc thực phẩm.
- Tham số mới duy nhất của spec là CFG-M08-07 (nhắc công bố thực đơn tuần kế tiếp, mặc định \[2 ngày trước ngày đầu tuần\]), đã được bổ sung vào Phụ lục 25 cùng quy tắc BR-M08-16.

## Điểm cần báo lại về tài liệu nguồn

**Trạng thái (2026-09-27):** theo yêu cầu của người dùng, `docs/nghiep-vu.md` đã được cập nhật cho các quyết định Q-39, Q-142 → Q-147 (nằm ở mục 24.2; Q-39 được bỏ khỏi 24.1; 24.3 không còn dòng nào) và cho điểm 3, 4, 5, 6, 7, 9 dưới đây, tại 2.4 (thuật ngữ mới), 12.1, 12.2, 12.5, 19.3 (giới hạn quyền của dòng "Chế độ ăn, thực đơn" và người nhận tại tầng), BR-M08-01, 02, 03, 11, 15, BR-M08-16 mới và CFG-M08-07 ở Phụ lục 25. **Không sửa theo quyết định của người dùng (2026-09-27):** `docs/phan-tich-yeu-cau.md` giữ nguyên; điểm 2, 8 và phần của điểm 1 thuộc tài liệu này được giữ ở đây làm ghi chú lệch đã biết; 19.3 của tài liệu nghiệp vụ và spec này là căn cứ cho các điểm đó. **Còn mở:** chỉ phần feature 014 (chưa viết) ở điểm 10.

1. **Quyết định đã chốt khi clarify ngày 2026-09-27** — *đã phản ánh vào `docs/nghiep-vu.md` (thân tài liệu và mục 24.2; Q-39 đã bỏ khỏi 24.1)*; các ghi chú "cần …" dưới đây chỉ còn đúng với `docs/phan-tich-yeu-cau.md` và các spec khác:
   - Q-39 (đã chốt 2026-09-27): có lưu mẫu bằng bản ghi đơn giản, chặn giao phiếu khi chưa lưu mẫu (FR-060). Cần chuyển Q-39 từ 24.1 sang 24.3 và bỏ chữ "cần xác nhận" ở 12.5, BR-M08-15, UC-78, LUU_MAU_THUC_AN.
   - Q-142 (đã chốt 2026-09-27): dinh dưỡng viên tự công bố thực đơn sau kiểm tra tự động (FR-029). Cần ghi vào 12.2, chú thích dòng "Chế độ ăn, thực đơn" của 4.4 rằng quyền D của Bác sĩ chỉ áp cho UC-45, và thêm Q-142 vào mục 24.3.
   - Q-143 (đã chốt 2026-09-27): người nhận tại tầng là Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc trong phạm vi phân công (FR-048). Cần bỏ chữ "tạm" ở chú thích ¹⁵ của 4.4, ghi vai trò vào BR-M08-11 và thêm Q-143 vào mục 24.3.
   - Đề xuất Q-144 (đã chốt 2026-09-27): người Tạm vắng, Hoạt động bên ngoài có giờ dự kiến trở về không muộn hơn giờ bữa được tính suất sẵn; chưa về tới giờ bữa thì phát sinh "−1" và suất được giữ tại tầng (FR-034, FR-040a). BR-M08-01 hiện trừ cả Tạm vắng và Hoạt động bên ngoài không điều kiện; cần sửa BR-M08-01 cho cả hai trạng thái; feature 004 (lượt vắng) và 014 (chuyến ngoài viện) cần cung cấp giờ dự kiến trở về.
   - Đề xuất Q-145 (đã chốt 2026-09-27): phạm vi bác sĩ duyệt bản gán chế độ ăn (FR-013 (a)–(d)). Cần làm rõ BR-M08-03 theo phạm vi này.
   - Đề xuất Q-146 (đã chốt 2026-09-27): bản ghi lưu mẫu gồm mọi món được nấu trong bữa, mỗi món một mẫu; món không lưu được ghi lý do, không chặn giao (FR-061). Cần bổ sung vào 12.5 và BR-M08-15.
   - Đề xuất Q-147 (đã chốt 2026-09-27): mỗi chế độ ăn có món an toàn; ngoài giờ hành chính của dinh dưỡng viên, suất "thiếu món thay thế" tự dùng món an toàn nếu không xung đột, nếu vẫn xung đột thì báo điều dưỡng phụ trách (FR-037a). Cần bổ sung vào 12.1 (thuộc tính chế độ ăn) và BR-M08-02.
2. **UC-48 "Đối chiếu đồ ăn gia đình"** không có dòng trong Permission Matrix 4.4. Spec dùng actor của UC-48 (Điều dưỡng, Dinh dưỡng viên: T). Cần thêm dòng vào 4.4.
3. *(Đã phản ánh vào 12.1; phần ERD, 3.2 giữ nguyên theo quyết định của người dùng.)* **Vòng đời chế độ ăn (12.1)** chỉ có Đề xuất → Chờ duyệt → Hiệu lực → Ngừng. Spec thêm Chờ hiệu lực, Từ chối, Đã hủy và đường Đề xuất → Hiệu lực cho chế độ ăn không liên quan điều trị. ERD và 3.2 cần thực thể **bản gán chế độ ăn** (nhóm 2), vì 1.5 xếp "chế độ ăn" vào nhóm 2 còn 3.2 xếp CHE_DO_AN vào nhóm 1.
4. *(Đã phản ánh vào 12.2, BR-M08-16, Phụ lục 25.)* **Vòng đời thực đơn (12.2)** chưa có Rút lại, Đã hủy, đường Chờ duyệt → Đã áp dụng khi công bố muộn trong tuần, và chưa nêu ai công bố (đã chốt Q-142: dinh dưỡng viên). Theo Q-142, "Chờ duyệt" có nghĩa "đã qua kiểm tra, chờ công bố"; nên cân nhắc đổi tên trạng thái ở 12.2 cho khỏi hiểu nhầm. Chưa có quy định khi tuần bắt đầu mà chưa có thực đơn; spec đề xuất CFG-M08-07, cần bổ sung vào Phụ lục 25.
5. *(Đã phản ánh vào 12.5.)* **Phiếu bữa ăn (12.5)**: "phiếu quay lại bếp xử lý" chưa nêu trạng thái đích; spec cho Có sai lệch → Đã giao khi bếp ghi xử lý, và Đã chuẩn bị → Đã chốt khi có phát sinh. DBR-27 ("mỗi (bữa, tầng/khu) có đúng một phiếu") được hiểu là cho tầng/khu có ít nhất một suất.
6. *(Đã phản ánh vào 12.5; phần ERD giữ nguyên.)* **Suất đặc biệt, món thay thế**: 12.5 chưa nêu tiêu chí suất đặc biệt (FR-036); ERD chưa có quan hệ món thay thế trong thực đơn và chỉ định món thay thế theo người.
7. *(Đã phản ánh vào 12.1; phần ERD giữ nguyên.)* **Hạn chế thực phẩm**: 12.1 quản lý "hạn chế thực phẩm" nhưng 5.2 và ERD không có chỗ lưu; spec đặt vào bản gán chế độ ăn và danh sách hạn chế của chế độ ăn.
8. **Thực thể còn thiếu trong ERD miền E**: lần chốt suất, phát sinh sau chốt (hiện là thuộc tính của SUAT_AN), suất ăn người thân (đã có trong DBR-15 nhưng chưa có thực thể), xác nhận phục vụ, yêu cầu xem lại chế độ ăn, loại đồ ăn bị cấm.
9. *(Đã phản ánh vào BR-M08-01.)* **Thời điểm sau giờ bữa**: BR-M08-01 chưa nêu phát sinh có áp dụng sau giờ bữa không; spec dừng phát sinh tự động ở giờ bữa và thêm yêu cầu suất bổ sung (FR-040).
10. *(Đã đồng bộ ngày 2026-09-27: mỗi spec 001, 003, 004, 005, 007, 008, 009, 010, 012 có mục "Cập nhật 2026-09-27 (đồng bộ với spec 011)"; 003, 008 chỉ ghi chú vì khu nghỉ bán trú đã gắn tầng/khu vực theo feature 003 FR-038; 000 không cần sửa vì FR-011 quản lý mọi tham số của Phụ lục 25, gồm CFG-M08-07. Còn chờ: 014, chưa viết.)* **Yêu cầu đồng bộ ở feature đã viết**:
    - **005**: FR-040 đang tự nhắc dinh dưỡng viên khi ăn kém kéo dài; cần gửi sự kiện cho feature này để tạo yêu cầu xem lại (FR-058) và bỏ nhắc trùng. Ghi kết quả ăn uống của bữa có suất đặc biệt cần xác nhận phục vụ trước (FR-050). Nhận lịch bữa và bữa bị bỏ (FR-042).
    - **007**: FR-039a cần gửi sự kiện sụt cân cho feature này (BR-M08-05). Nhận yêu cầu tạo sự cố nguồn "ăn uống" với mức theo triệu chứng (FR-049), ngoài loại "phục vụ sai suất ăn" đã có.
    - **009**: thêm các dòng của feature 011 vào bảng mức ở FR-043b theo bảng FR-065.
    - **010**: bảng nguồn đã có "suất ăn của người thân ở lại"; cần nhận thêm sự kiện hủy suất (FR-043).
    - **012**: FR-052 ghi tỷ lệ ăn và lượng nước lấy từ "feature 005, 011"; nguồn đúng là feature 005, feature này chỉ cung cấp thực đơn chung và chế độ ăn riêng.
    - **001**: FR-020 đã kích hoạt kiểm tra lại chế độ ăn, thực đơn; cần thêm sự kiện khi mục dị ứng chuyển Đã loại trừ (FR-021).
    - **014** (chưa viết): cần cung cấp chuyến ngoài viện đã lên lịch, người tham gia, khoảng thời gian và thời điểm dự kiến trở lại, để trừ khỏi hoặc tính sẵn vào số suất (Q-144).
    - **007**: FR-040, FR-042 liệt kê "phục vụ sai suất ăn" vừa là nguồn phát sinh vừa là loại sự cố; spec này dùng loại "phục vụ sai suất ăn" cho cả hai trường hợp, nguồn "phục vụ sai suất ăn" khi chưa ăn và "ăn uống" khi đã ăn (FR-049). Nên ghi rõ ở feature 007.
    - **003, 008**: khu bán trú cần là một tầng/khu nhận được phân công trong ca, để có người nhận phiếu khu bán trú (FR-048).
    - **000**: FR-011 chưa có CFG-M08-07.
11. *(Đã phản ánh ngày 2026-09-27 vào 12.4, BR-M08-04, BR-M08-05, BR-M08-08; BR-M04-09 được làm rõ để khỏi nhắc trùng.)* **Quy tắc đã chốt trong spec nhưng chưa vào tài liệu nguồn**:
    - 12.4, BR-M08-04: điểm nghi vấn đầy đủ của đồ ăn gia đình, gồm người dùng chế độ ăn liên quan điều trị (FR-054); hạn dùng do người xác nhận ghi và việc tự chuyển Không sử dụng khi tới hạn dùng (FR-055); danh mục loại đồ ăn bị cấm do Quản lý viện quản lý (FR-007).
    - BR-M08-08: lệnh Đổi món có hiệu lực ngay khi có lý do, không cần duyệt (FR-031); không áp cho bữa đã qua giờ bữa; bị chặn nếu làm mất độ phủ (FR-032).
    - BR-M08-05: hạn xử lý yêu cầu xem lại không dừng khi người cao tuổi vắng mặt (FR-059).

**(2026-09-28, góp ý nghiệp vụ Q-207 → Q-222) Tài liệu nguồn đã thay đổi, spec cần rà lại.** *(2026-09-29: mọi điểm dưới đây đã xử lý, xem nhãn "Đã xử lý" từng điểm.)* Các điểm dưới đây đã có trong `docs/nghiep-vu.md` và `docs/luong-nghiep-vu.md`; spec **chưa** được sửa theo, và cần chạy `/speckit-clarify` hoặc cập nhật FR tương ứng.

1. **[Đã xử lý 2026-09-28: mục Ngoài phạm vi; nội dung chờ spec kho nguyên liệu]** **Kho nguyên liệu nấu ăn (Q-210, Q-220; 12.7, BR-M08-17 → 20, UC-91, 92, DBR-31).** Chức năng mới thuộc Module 08: danh mục nguyên liệu, phiếu nhập (có kiểm tra đầu vào), phiếu xuất theo lô, kiểm kê. Quản lý viện quản lý; Nhân viên bếp lập phiếu xuất cho bữa và đề nghị nhập; Dinh dưỡng viên xem tồn. Không tự trừ kho theo định lượng món. Đề xuất viết spec riêng (cùng tài sản 7.7) thay vì mở rộng spec này.

**(2026-10-05) Thay đổi do chủ dự án chốt Q-280 → Q-286.** *(Đã xử lý 2026-10-06: spec đã sửa theo các quyết định dưới đây, xem mục "Cập nhật 2026-10-06" ở Clarifications)*

- **Q-283** – gộp Dinh dưỡng viên vào Bác sĩ; bản gán chế độ ăn do Bác sĩ lập nên không còn bước duyệt riêng (BR-M08-03); cột DDV của Phụ lục 27 gộp vào cột BS. Ảnh hưởng: User Story 1, User Story 3, User Story 8, FR-001 → FR-004, FR-012 → FR-016, FR-021 → FR-041, FR-052, FR-055, FR-059, FR-061, SC-007 → SC-009, SC-012 (mọi chỗ ghi Dinh dưỡng viên); bảng trạng thái bản gán bỏ Chờ duyệt, Từ chối (BR-M08-03).
- **Q-284** – kho, tài sản do bộ phận tự quản lý qua Phụ trách bộ phận (13.1a); Quản lý viện chỉ duyệt chi phí qua yêu cầu mua sắm và duyệt thanh lý; bộ phận tự mua, Kế toán đối chiếu chứng từ (15.10, BR-M11-17 → 20, CFG-M11-07 → 09, UC-94, UC-95, DBR-36, BF-18). Ảnh hưởng: kho nguyên liệu do bộ phận Bếp quản lý (12.7).

**(2026-10-06) Thay đổi do chủ dự án chốt Q-306 (hệ thống không tự sinh dữ liệu, công việc).** *(Đã xử lý 2026-10-06: spec đã viết lại, xem mục "Cập nhật 2026-10-06, lượt 2" ở Clarifications.)*

- **Q-306** – Chốt suất là lệnh của Nhân viên bếp từ mốc CFG-M08-01; hệ thống tính sẵn số suất và chọn món thay thế; lệnh lập phiếu bữa ăn; quá mốc chưa chốt thì nhắc Bếp (12.5, BR-M08-01, 09). Tầng chưa có phiếu nhận phát sinh "+" thì phiếu được lập khi Bếp xác nhận nhận phát sinh. Phát sinh sau chốt vẫn gửi bếp như cũ. Nguyên tắc chung: mọi bản ghi do actor lập bằng lệnh; hệ thống chỉ gợi ý từ dữ liệu đã có, tự chuyển trạng thái theo thời gian, cảnh báo, leo thang, nhắc, kiểm tra và thực hiện tác động kèm theo lệnh (1.3, 24.2). FR cần rà (dò theo từ khóa, chưa đối chiếu từng FR): FR-040, FR-042, FR-044.

**(2026-10-06) Thay đổi do chủ dự án chốt Q-307 (bỏ gợi ý, đề xuất, yêu cầu và sự cố do hệ thống tự tạo).** *(Đã xử lý 2026-10-06: spec đã viết lại, xem mục "Cập nhật 2026-10-06, lượt 2" ở Clarifications. Còn mở: hệ thống vẫn tự chọn món thay thế, tự dùng món an toàn (BR-M08-02) và tạo phát sinh "−1" chỉ để ghi nhận cho suất giữ (Q-144) theo tài liệu nguồn; nếu áp Q-307 cho cả các việc này thì cần sửa BR-M08-02, Q-144 trước.)*

- **Q-307** – Bỏ yêu cầu bác sĩ xem lại chế độ ăn và CFG-M08-02: hệ thống chỉ báo bác sĩ, bác sĩ quyết định giữ nguyên hoặc lập bản gán mới (BR-M08-05, BR-M04-09). Phục vụ suất có thành phần dị ứng: chặn và tạo cảnh báo Trung bình, không tự tạo sự cố (BR-M08-14). Giữ: cảnh báo do hệ thống tạo và leo thang, nhắc, thông báo; bản ghi kèm lệnh của actor; số liệu tính để hiển thị (24.2).

**(2026-10-06) Thay đổi do chủ dự án chốt Q-308 (Nhân viên bếp lập món ăn, thực đơn; Bác sĩ duyệt).** *(Đã xử lý 2026-10-06: spec đã viết lại, xem mục "Cập nhật 2026-10-06, lượt 3" ở Clarifications.)*

- **Q-308** – Bác sĩ xác định yêu cầu dinh dưỡng (bản gán chế độ ăn), danh mục chế độ ăn và món an toàn. Nhân viên bếp tạo món ăn, Bác sĩ xác nhận thành phần và chế độ ăn phù hợp trước khi món được dùng. Nhân viên bếp lập thực đơn tuần, tự xác nhận đã kiểm tra khả năng chế biến, nguyên liệu, suất ăn (hệ thống hiển thị tồn kho và số người theo chế độ ăn, không đối chiếu định lượng, Q-220), gửi kiểm tra; Bác sĩ Công bố hoặc Trả lại kèm lý do. Nhân viên bếp đổi món sau công bố, Bác sĩ được báo. Nhắc thực đơn tuần kế tiếp gửi Nhân viên bếp (12.1, 12.2, BR-M08-08, 16, 19.3, Phụ lục 26, 27, BF-03; sửa Q-142, Q-283). FR cần rà: mục A (FR-001 → FR-005), mục D (FR-023 → FR-032), FR-063, bảng FR-065, User Story 2, 3.

**(2026-10-10) Plan 011, điểm báo lại 1 → 3.** *(Đã xử lý 2026-10-10: Q-333; sửa FR-037a, FR-040a. Dẫn chiếu "4.4" tra qua bảng ánh xạ ở Phụ lục 26, không phải lỗi.)*
