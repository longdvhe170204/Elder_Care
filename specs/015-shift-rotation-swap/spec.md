# Feature Specification: Đổi ca và nghỉ đột xuất (sửa 2026-10-29, Q-397; tên cũ: Lịch ca xoay vòng, phủ ca tối thiểu, đổi ca và nghỉ đột xuất)

**Feature Branch**: `015-shift-rotation-swap`

**Created**: 2026-09-27

**Status**: Draft

**Input**: User description: "Mở rộng quản lý ca theo docs/business/nghiep-vu.md mục 13.2 phần lịch ca xoay vòng và BR-M09-09 đến BR-M09-11: sinh lịch ca tháng từ mẫu xoay ca; yêu cầu phủ ca tối thiểu theo vai trò; công bố lịch; đổi ca cần người nhận đồng ý và trưởng tầng duyệt; nghỉ đột xuất gợi ý người thay thế."

## Clarifications

### Session 2026-09-27

- Q: Ai duyệt yêu cầu đổi ca, nghỉ đột xuất của ca phạm vi toàn viện (Bác sĩ trực, bếp, vệ sinh, hành chính), khi 4.4 chỉ cho Quản lý viện quyền "X" ở dòng "Yêu cầu đổi ca"? → A: Quản lý viện duyệt; đây là ngoại lệ được ghi cho dòng "Yêu cầu đổi ca" của 4.4 và cho Q-15. Quản lý viện cũng lập thay được yêu cầu nghỉ đột xuất cho ca toàn viện (đề xuất Q-177; người dùng chọn phương án A). Áp dụng tại FR-023, FR-026, FR-028, FR-040.
- Q: Lịch còn ca thiếu phủ tối thiểu có công bố được không? → A: Được. Hệ thống chỉ cảnh báo; người công bố phải xác nhận đã xem kèm lý do, và mỗi ca thiếu phủ mở cảnh báo thiếu phủ cho trưởng tầng từ lúc công bố, như cách BR-M09-02 xử lý tỷ lệ phục vụ (đề xuất Q-178; người dùng chọn phương án A). Áp dụng tại FR-017, FR-032, FR-036, User Story 2 kịch bản 5.
- Q: Đổi ca gồm những dạng nào? → A: Đổi hai chiều (X ↔ Y) và nhận thay một chiều (B nhận X, không trả ca), cả hai chỉ trong cùng phạm vi; đổi giữa hai phạm vi không hỗ trợ, trưởng tầng dùng "Bổ sung nhân viên vào ca" của feature 008 (đề xuất Q-179; người dùng chọn phương án B). Áp dụng tại FR-019 → FR-022, FR-024, User Story 3.

### Session 2026-09-27 (lượt 2, /speckit-clarify)

- Q: Khi xếp thứ tự người thay, "số giờ đã làm trong tháng" tính những ca nào? → A: Ca đã kết thúc cộng ca đã xếp còn lại trong tháng chứa ca cần thay (tổng tải dự kiến của tháng), hiển thị riêng hai phần (đề xuất Q-180; người dùng chọn phương án B). Áp dụng tại FR-034, User Story 5 kịch bản 2.
- Q: Nhân viên nhiều vai trò được tính cho mấy dòng yêu cầu phủ tối thiểu? → A: Chỉ một dòng; hệ thống tự phân nhân viên vào các dòng sao cho số dòng đạt là lớn nhất (đề xuất Q-181; người dùng chọn phương án A). Áp dụng tại FR-006, User Story 2 kịch bản 3.
- Q: Trưởng tầng có được duyệt một phần yêu cầu nghỉ đột xuất nhiều ca không? → A: Được; người duyệt chọn các ca được duyệt, các ca còn lại bị từ chối kèm lý do, yêu cầu chuyển "Đã áp dụng một phần" (đề xuất Q-182; người dùng chọn phương án B). Áp dụng tại FR-028, FR-031, FR-046, User Story 4 kịch bản 9.
- Q: Người nhận lời mời nhận ca thay được thêm thẳng vào ca hay chờ trưởng tầng xác nhận? → A: Thêm thẳng: người nhận đầu tiên được bổ sung vào ca ngay sau khi hệ thống kiểm tra lại, không cần xác nhận thêm; người gửi lời mời được báo (đề xuất Q-183; người dùng chọn phương án A). Áp dụng tại FR-038, User Story 5 kịch bản 4.
- Q: Nhân viên biết trước sẽ nghỉ vào ngày thuộc tháng chưa có lịch hoặc lịch còn Nháp có lập yêu cầu nghỉ được không? → A: Có, bằng yêu cầu nghỉ đột xuất dạng "theo ngày"; khi được duyệt, hệ thống không xếp nhân viên vào các ngày đó lúc sinh, sinh lại, và gỡ khỏi lịch Nháp nếu đã sinh (đề xuất Q-184; người dùng chọn phương án B). Áp dụng tại Phạm vi, FR-010, FR-013, FR-026a, FR-031, User Story 4 kịch bản 10 → 12, Key Entities, Assumptions.

### Session 2026-09-27 (lượt 3, sau checklist consistency)

- Q: Khi đổi ca hoặc nghỉ gỡ Trưởng đoàn hoặc người đi cùng của một chuyến đi (spec 014 FR-035) khỏi ca, xử lý thế nào? → A: Cho duyệt nhưng cảnh báo trước khi duyệt; khi áp dụng, hệ thống tự gỡ nhiệm vụ đó và báo trưởng tầng phân công lại; spec 014 vẫn chặn điểm danh rời viện khi chưa có trưởng đoàn (đề xuất Q-185; người dùng chọn phương án A). Áp dụng tại FR-021 (h), FR-024 (g), FR-027, FR-029 (h).
- Q: Lời mời nhận ca thay gửi nhân viên đang ngoài ca có được gửi tin nhắn không? → A: Không; giữ quy tắc Q-98 của spec 009: người ngoài ca chỉ nhận thông báo Trung bình trong ứng dụng; spec ghi rõ hệ quả, trưởng tầng liên hệ ngoài hệ thống nếu gấp (đề xuất Q-186; người dùng chọn phương án A). Áp dụng tại FR-044, Assumptions.
- Q: Khi người rời ca là Người phụ trách ca và không có ai đủ điều kiện thay, đổi ca và nghỉ đột xuất xử lý có khác nhau không? → A: Giữ khác biệt: đổi ca là tự nguyện nên chặn duyệt; nghỉ là bắt buộc nên cho duyệt và gắn dấu "thiếu người phụ trách" (đề xuất Q-187; người dùng chọn phương án A). Áp dụng tại FR-024 (c), FR-029 (d).
- Q: Phủ tối thiểu có tính giấy phép, đào tạo không? → A: Có; nhân viên chỉ được tính cho một dòng khi có đủ giấy phép, đào tạo bắt buộc với vai trò đó còn hiệu lực trong toàn ca, như điều kiện ứng viên thay; giấy phép, đào tạo hết hiệu lực hoặc bị thu hồi là sự kiện tính lại phủ (đề xuất Q-188; người dùng chọn phương án A). Áp dụng tại FR-006, FR-007, FR-032, User Story 2 kịch bản 7.

Các mục khác của checklist consistency được sửa theo đề xuất ghi trong checklist, không cần quyết định mới: định nghĩa "nghỉ có duyệt" (Requirements), một lý do cho lệnh công bố (FR-017), dòng mặc định CFG-M09-07 (FR-005), chọn lại người phụ trách khi gỡ khỏi lịch Nháp (FR-026a), người phụ trách đổi theo lệnh "Chuyển người phụ trách ca" (FR-024, FR-029), dấu Bác sĩ trực (FR-024 (f), FR-029 (g)), tên trạng thái "Từ chối" (FR-022), mốc "giờ kết thúc ca" (FR-035, FR-036, FR-039), nhật ký lời mời (FR-038), thực thể và trường hiệu lực (Key Entities), SC-004, điểm báo lại 12 → 16. Các spec 000, 002, 005, 008, 014 được đồng bộ bằng mục "Cập nhật 2026-09-27 (đồng bộ với spec 015)".

### Session 2026-09-27 (lượt 4, sau checklist business-rules)

- Q: Ngày và mẫu ca có dòng phủ tối thiểu áp dụng nhưng không có ai được xếp khi sinh lịch thì xử lý thế nào? → A: Không tạo ca rỗng; báo cáo sinh lịch và lệnh công bố liệt kê mục "thiếu ca" (ngày, mẫu ca, phạm vi); công bố vẫn được, xác nhận bằng cùng một lý do như ca thiếu phủ (đề xuất Q-189; người dùng chọn phương án A). Áp dụng tại FR-006a, FR-012, FR-016, FR-017, User Story 2 kịch bản 8.
- Q: Có được đổi mẫu xoay ca hoặc ngày gốc của một nhóm đã có thành viên không? → A: Không; muốn đổi thì lập nhóm mới và chuyển thành viên bằng ngày kết thúc, ngày bắt đầu tham gia; báo cáo sinh lịch vẫn cảnh báo giờ liên tục ở chỗ nối (đề xuất Q-190; người dùng chọn phương án A). Áp dụng tại FR-002, FR-003, Edge Cases.
- Q: Khi đổi ca mà người nhường không được tính cho dòng phủ nào trong ca, người nhận cần vai trò gì? → A: Nếu người nhường được tính cho một dòng phủ thì người nhận phải có vai trò của dòng đó; nếu không, người nhận phải có ít nhất một vai trò hệ thống chung với người nhường (đề xuất Q-191; người dùng chọn phương án A). Áp dụng tại FR-019, User Story 3 kịch bản 13.

Các mục khác của checklist business-rules được sửa theo đề xuất ghi trong checklist: FR-003 (ngừng hiệu lực nhóm), FR-005 (dòng toàn viện), FR-006 (phân dòng duy nhất), FR-008 (đổi tham số, sinh lỗi), FR-025 (nhắc ngay), FR-026a (tách theo nhóm, ngày nghỉ, rời ngày), FR-031 (nghỉ việc), FR-033, FR-034 (cùng phạm vi, ca qua tháng), FR-037 (lời mời), FR-039 (người gửi mất quyền), FR-043 (ngoại tuyến), FR-046 (thời hạn lưu), các kịch bản mới và SC-002, SC-003, SC-006, SC-007.

### Cập nhật 2026-10-06 (đồng bộ Q-280 → Q-288, quyết định của chủ dự án)

- **Q-282, Q-283**: bỏ vai trò Nhân viên vệ sinh và Dinh dưỡng viên; nhân viên không có tài khoản không có trong lịch ca, nên không là ứng viên thay và không tính vào phủ tối thiểu (13.1). Kịch bản 13 của User Story nhận thay đổi sang ví dụ Hành chính; bảng quyền bỏ hai vai trò.

### Cập nhật 2026-10-06, lượt 2 (đồng bộ Q-306, Q-307: lịch ca do người lập lịch lập bằng lệnh; bỏ gợi ý người thay)

- **Q-306**: bỏ tự sinh lịch Nháp. Trưởng tầng (tầng được giao) hoặc Quản lý viện (lịch toàn viện) lập lịch tháng bằng lệnh "Lập lịch từ mẫu xoay ca"; việc xếp nhân viên theo mẫu là tác động của lệnh. CFG-M09-06 đổi nghĩa thành mốc nhắc: tới mốc mà phạm vi chưa có lịch thì nhắc người lập lịch. Không ghi đè lịch đã có. Lịch Nháp lập lại được, mất các sửa tay sau khi xác nhận kèm lý do. Bỏ "sinh lỗi thì thử lại lúc 00:00" (13.2, BR-M09-09, Q-189).
- **Q-307**: người phụ trách ca do người lập lịch chọn; bỏ danh sách ưu tiên người phụ trách và việc hệ thống tự chỉ định (BR-M09-09). Bỏ danh sách gợi ý người thay: hệ thống cảnh báo thiếu phủ; Trưởng tầng tự chọn nhân viên đang không có ca để mời. Hệ thống kiểm tra điều kiện người được mời và hiển thị số giờ đã làm, đã xếp trong tháng, không xếp hạng (BR-M09-11, 19.3).
- Thuật ngữ "sinh lịch", "Sinh lại", "báo cáo sinh lịch" đổi thành "lập lịch", "Lập lại", "báo cáo lập lịch".
- Viết lại: Phạm vi mục 4, 8; User Story 1, 5; User Story 2 (lý do, kịch bản 6, 8); User Story 4 kịch bản 10; Edge Cases; bảng nhóm dữ liệu; FR-002, FR-006b, FR-008, FR-011 → FR-014, FR-026a, FR-027, FR-032 → FR-035, FR-037; bảng cảnh báo thiếu phủ; FR-040, FR-043 → FR-046; Key Entities; SC-001, 002, 005; Assumptions.

### Cập nhật 2026-10-29 (đồng bộ Q-397: bỏ xoay ca, phủ tối thiểu, lời mời nhận ca)

- Chủ dự án bỏ mẫu xoay ca, nhóm xoay ca, lập lịch từ mẫu, yêu cầu phủ tối thiểu (CFG-M09-07), thiếu ca, cảnh báo thiếu phủ, lời mời nhận ca thay; CFG-M09-06, CFG-M09-10 bỏ. Lịch ca lập tay ở feature 008. Ca thiếu người do Trưởng tầng tự bổ sung nhân viên vào ca (feature 008).
- Spec này chỉ còn yêu cầu đổi ca (mục D) và nghỉ đột xuất (mục E). Bỏ User Story 1, 2, 5; mục A (FR-001 → FR-007), B (FR-008 → FR-015), FR-016, FR-017, mục F (FR-032 → FR-039); bỏ dòng tương ứng ở ma trận quyền FR-040.
- Trong mục D, E: bỏ mọi kiểm tra, hiển thị, tính lại phủ tối thiểu; tỷ lệ phục vụ (feature 008 FR-034) vẫn hiển thị. Đổi ca: B có ít nhất một vai trò hệ thống chung với A (Q-191 phần còn lại). Nghỉ "theo ngày" mở cho mọi nhân viên, chọn phạm vi (tầng/khu vực hoặc toàn viện) khi lập để xác định người duyệt (chủ dự án xác nhận 2026-10-29).
- Mã FR, US giữ để không đánh lại số. Chỗ còn sót nhắc phủ tối thiểu, nhóm xoay ca, lời mời trong spec này không còn hiệu lực.

## Phạm vi

**Trong phạm vi** (Module 09 mục 13.2; BR-M09-10; UC-54; ranh giới theo Q-77, Q-397):

1. *(Bỏ theo Q-397, 2026-10-29)* Mẫu xoay ca.
2. *(Bỏ theo Q-397, 2026-10-29)* Nhóm xoay ca.
3. *(Bỏ theo Q-397, 2026-10-29)* Yêu cầu phủ ca tối thiểu theo vai trò cho từng mẫu ca và phạm vi.
4. *(Bỏ theo Q-397, 2026-10-29)* Lệnh "Lập lịch từ mẫu xoay ca" lập bản nháp lịch ca tháng.
5. *(Bỏ theo Q-397, 2026-10-29)* Kiểm tra phủ tối thiểu khi công bố lịch và nhắc công bố.
6. Yêu cầu đổi ca giữa hai nhân viên cùng phạm vi, gồm đổi hai chiều và nhận thay một chiều (Q-179): người nhận ca đồng ý, trưởng tầng duyệt, kiểm tra BR-M09-01 và BR-M09-03 trước khi duyệt, áp dụng vào lịch đã công bố (UC-54, BR-M09-10).
7. Yêu cầu nghỉ đột xuất có duyệt, dạng "theo ca" cho một hoặc nhiều ca đã công bố và dạng "theo ngày" cho ngày thuộc tháng chưa công bố lịch (13.2, Q-81, Q-91, Q-184).
8. *(Bỏ theo Q-397, 2026-10-29)* Cảnh báo ca thiếu phủ tối thiểu.
9. Các thông báo của nghiệp vụ trên (gửi qua feature 009) và dữ liệu cho báo cáo nhân sự (feature 016).

**Ngoài phạm vi** (spec này **nhận** dữ liệu hoặc **gọi lệnh** của feature sở hữu):

- Quy tắc dùng chung (nhóm dữ liệu, lý do bắt buộc, yêu cầu phê duyệt, nhật ký, tham số): feature 000. Spec này kế thừa, không lặp lại.
- Hồ sơ nhân viên, giấy phép, đào tạo; mẫu ca; lịch ca lập tay, sửa lịch Nháp, lệnh "Công bố lịch"; trạng thái ca; các lệnh Ghi nhận vắng ca, Bổ sung nhân viên vào ca, Chuyển người phụ trách ca, Hủy ca; giao trưởng tầng; phân công; tỷ lệ phục vụ; bàn giao ca: feature 008. Lịch Nháp do lệnh của spec này lập ra thuộc feature 008 từ lúc lệnh hoàn tất; kết quả đổi ca, nghỉ đột xuất đã duyệt được áp vào ca bằng các kiểm tra FR-013, FR-014, FR-015, FR-028, FR-030 của feature 008 (Q-77).
- Tài khoản, vai trò hệ thống, phạm vi dữ liệu: feature 002. Chuyển công việc, liều thành chung: feature 005, 006. Gửi thông báo: feature 009. Báo cáo, dashboard: feature 016 (Module 14).
- Nghỉ phép năm, chấm công, tính lương, làm thêm giờ: không thuộc hệ thống ở giai đoạn này. Nghỉ biết trước cho tháng chưa công bố lịch đi qua yêu cầu nghỉ đột xuất dạng "theo ngày" (FR-026a, Q-184); trưởng tầng vẫn sửa tay được lịch Nháp theo feature 008.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Lập lịch từ mẫu xoay ca *(Bỏ theo Q-397, 2026-10-29)*

---

### User Story 2 - Phủ ca tối thiểu và công bố lịch *(Bỏ theo Q-397, 2026-10-29)*

Công bố lịch theo feature 008 FR-016.

---

### User Story 3 - Nhân viên đổi ca, người nhận đồng ý và trưởng tầng duyệt (Priority: P2)

Điều dưỡng A có ca ngày 12/11 muốn đổi với ca ngày 15/11 của điều dưỡng B cùng tầng. A lập yêu cầu, chọn B và ca của B. B đồng ý. Trưởng tầng xem kết quả kiểm tra (chồng giờ, giờ làm liên tục, giấy phép cho các phân công sẽ chuyển, người phụ trách ca, phủ tối thiểu) rồi duyệt. Hệ thống đổi tên hai người trong hai ca, chuyển phân công và thông báo các bên.

**Why this priority**: Đổi ca xảy ra hằng tuần; nếu làm ngoài hệ thống thì lịch, phân công và phạm vi dữ liệu sai lệch với thực tế. Cần sau khi đã có lịch công bố (User Story 1, 2).

**Independent Test**: Trên lịch đã công bố, lập một yêu cầu đổi hợp lệ, một yêu cầu gây chồng giờ, một yêu cầu mà người nhận thiếu giấy phép cho phân công Điều dưỡng phụ trách; đi hết các nhánh đồng ý, người nhận từ chối, duyệt, từ chối, hết hiệu lực.

**Acceptance Scenarios**:

1. **Given** A có ca X (ngày 12/11) và B có ca Y (ngày 15/11), cùng tầng 2, cùng Đã công bố chưa bắt đầu, **When** A lập yêu cầu đổi X ↔ Y với B kèm lý do, **Then** yêu cầu ở "Chờ người nhận đồng ý" và B được thông báo; lịch chưa thay đổi.
2. **Given** yêu cầu ở "Chờ người nhận đồng ý", **When** B từ chối, **Then** yêu cầu chuyển "Người nhận từ chối", A được thông báo, lịch giữ nguyên; **When** B đồng ý, **Then** yêu cầu chuyển "Chờ duyệt" và trưởng tầng được giao của tầng 2 được thông báo (BR-M09-10).
3. **Given** sau khi đổi, A sẽ có ca Y chồng giờ với một ca toàn viện A đang có, **When** trưởng tầng duyệt, **Then** hệ thống từ chối lệnh duyệt và nêu ca chồng giờ (BR-M09-03).
4. **Given** sau khi đổi, B sẽ làm liên tục 22 giờ (vượt CFG-M09-02 mặc định \[16 giờ\]), **When** trưởng tầng duyệt, **Then** hệ thống hiển thị cảnh báo và chỉ cho duyệt khi trưởng tầng xác nhận đã xem kèm lý do (BR-M09-03, feature 008 FR-014).
5. **Given** A là Điều dưỡng phụ trách phòng 201 trong ca X, và giấy phép hành nghề của B hết hạn trước ngày 12/11, **When** trưởng tầng xem yêu cầu, **Then** hệ thống liệt kê "phân công Điều dưỡng phụ trách phòng 201 không chuyển được cho B – thiếu giấy phép còn hiệu lực" (BR-M09-01); **When** trưởng tầng vẫn duyệt, **Then** phân công đó không được lập cho B, công việc và liều liên quan thành chung của tầng và trưởng tầng được nhắc phân công lại.
6. **Given** yêu cầu hợp lệ ở Chờ duyệt, **When** trưởng tầng duyệt, **Then** trong cùng một lần: A rời X và vào Y, B rời Y và vào X; phân công Tương lai của A trong X được hủy và lập lại tương ứng cho B (và ngược lại) nếu đạt feature 008 FR-028, FR-030; tỷ lệ phục vụ của X, Y được tính lại; A, B và người phụ trách của X, Y được thông báo; yêu cầu chuyển "Đã áp dụng".
7. **Given** A là người phụ trách ca X và B có vai trò Điều dưỡng, **When** yêu cầu được duyệt, **Then** B thành người phụ trách ca X; **Given** B chỉ có vai trò Nhân viên chăm sóc (không thỏa feature 008 FR-015), **When** trưởng tầng duyệt mà không chọn người phụ trách khác có tên trong X, **Then** hệ thống không cho duyệt.
8. **Given** yêu cầu còn ở "Chờ duyệt" khi tới giờ bắt đầu của ca sớm hơn trong X, Y, **When** hệ thống kiểm tra, **Then** yêu cầu chuyển "Hết hiệu lực" và A, B được thông báo; trước đó CFG-M09-11 (đề xuất, mặc định \[24 giờ\]) người đang phải hành động đã được nhắc.
9. **Given** ca X đã nằm trong một yêu cầu đổi ca hoặc nghỉ đột xuất đang mở của A, **When** A lập yêu cầu mới có X, **Then** hệ thống từ chối.
10. **Given** A và B khác vai trò (A là Điều dưỡng, B chỉ là Nhân viên chăm sóc), **When** A lập yêu cầu đổi với B, **Then** hệ thống từ chối vì B không có vai trò mà A được tính trong phủ ca X.
11. **Given** A có ca X và B không có ca nào chồng giờ với X, **When** A lập yêu cầu "nhận thay" cho B (không chọn ca Y) và B đồng ý, trưởng tầng duyệt, **Then** A rời X, B vào X, phân công của A trong X chuyển cho B theo FR-024; A không nhận ca nào để bù, và số giờ đã xếp trong tháng của A giảm, của B tăng tương ứng (Q-179).
12. **Given** ca X thuộc tầng 2 và ca Y của B thuộc tầng 3, **When** A lập yêu cầu đổi X ↔ Y, **Then** hệ thống từ chối vì hai ca khác phạm vi; trưởng tầng dùng "Bổ sung nhân viên vào ca" của feature 008 nếu cần điều người giữa hai tầng (Q-179).
13. **Given** ca bếp toàn viện X, A chỉ có vai trò Nhân viên bếp, B1 là Nhân viên bếp, B2 chỉ là Hành chính (sửa, 2026-10-06, Q-282: trước đây là Nhân viên vệ sinh), **When** A lập yêu cầu nhận thay cho B1, **Then** yêu cầu được lập và chờ Quản lý viện duyệt (Q-177); **When** A lập yêu cầu cho B2, **Then** hệ thống từ chối vì A và B2 không có vai trò hệ thống chung (Q-191).
14. **Given** yêu cầu ở "Chờ duyệt", **When** B rút đồng ý, **Then** yêu cầu chuyển "Người nhận từ chối", A và trưởng tầng được báo, lịch giữ nguyên.
15. **Given** B vừa hết ca 07:00 và X bắt đầu 07:00 cùng ngày, **When** A lập yêu cầu nhận thay cho B, **Then** B và người duyệt thấy cảnh báo "vượt giờ làm liên tục"; trưởng tầng chỉ duyệt được khi xác nhận kèm lý do (FR-021 (b)).
16. **Given** X và Y cùng ngày, cùng giờ, thuộc hai nhóm của tầng 2, **When** A lập yêu cầu đổi X ↔ Y với B, **Then** hệ thống kiểm tra trạng thái sau đổi (A chỉ còn Y, B chỉ còn X), không có chồng giờ nên yêu cầu được lập.
17. **Given** A là Bác sĩ mang dấu "Bác sĩ trực" trong ca toàn viện X, **When** Quản lý viện duyệt yêu cầu nhận thay cho Bác sĩ B, **Then** B vào X và mang dấu "Bác sĩ trực", A rời X (FR-024 (f)).

---

### User Story 4 - Nghỉ đột xuất có duyệt (Priority: P2)

Nhân viên chăm sóc C báo ốm cho ba ca ngày 20 → 22/11. C (hoặc trưởng tầng lập thay khi C gọi điện báo) lập yêu cầu nghỉ đột xuất chọn ba ca, kèm lý do. Trước khi duyệt, trưởng tầng thấy tác động với từng ca: phủ tối thiểu, tỷ lệ phục vụ, người phụ trách ca, phân công bị ảnh hưởng và vai trò sẽ thiếu; trưởng tầng tự tra cứu người thay nếu cần (Q-307). Khi duyệt, C được rút khỏi ba ca, công việc của C thành công việc chung.

**Why this priority**: 13.2 và Q-81 yêu cầu vắng biết trước phải đi qua nghỉ đột xuất có duyệt; feature 008 đã hướng người dùng sang yêu cầu này khi ngoài khoảng "Ghi nhận vắng ca".

**Independent Test**: Lập yêu cầu nghỉ ba ca, trong đó một ca làm tầng thiếu Điều dưỡng; kiểm tra màn hình tác động trước duyệt, duyệt, rồi kiểm tra lịch, phân công, công việc chung.

**Acceptance Scenarios**:

1. **Given** C có ba ca Đã công bố chưa bắt đầu ngày 20 → 22/11, **When** C lập yêu cầu nghỉ đột xuất cho ba ca kèm lý do, **Then** yêu cầu ở "Chờ duyệt" và trưởng tầng được giao của tầng được thông báo (không kèm lý do).
2. **Given** C gọi điện báo ốm, **When** trưởng tầng lập yêu cầu thay cho C, **Then** yêu cầu ghi người lập là trưởng tầng, người nghỉ là C, dấu "lập thay", và C được thông báo; trưởng tầng duyệt được yêu cầu này theo Q-10 (lý do bắt buộc, nhật ký "tự duyệt").
3. **Given** yêu cầu ở Chờ duyệt, **When** trưởng tầng mở yêu cầu, **Then** với mỗi ca hệ thống hiển thị: tỷ lệ phục vụ trước/sau (feature 008 FR-034), C có phải người phụ trách ca không, phân công của C trong ca; không có danh sách gợi ý người thay (BR-M09-11, Q-307).
4. **Given** trưởng tầng duyệt, **When** yêu cầu được áp dụng, **Then** trong cùng một lần với mỗi ca: C được đánh dấu "nghỉ có duyệt" (giữ trong lịch sử ca, không tính tỷ lệ phục vụ); phân công Tương lai của C trong ca bị hủy; công việc, liều của C trong ca thành chung của tầng (feature 005 FR-032, feature 006); ca, phân công của C ngoài yêu cầu giữ nguyên (Q-91).
5. **Given** C là người phụ trách ca ngày 21/11, **When** trưởng tầng duyệt, **Then** hệ thống yêu cầu chọn người phụ trách mới trong ca trong cùng lệnh; nếu không có ai thỏa feature 008 FR-015, ca được gắn dấu "thiếu người phụ trách" (feature 008 FR-019).
6. **Given** trưởng tầng từ chối kèm lý do, **When** lệnh được ghi, **Then** yêu cầu chuyển "Từ chối", C được thông báo, lịch giữ nguyên.
7. **Given** yêu cầu còn Chờ duyệt khi ca 20/11 tới giờ bắt đầu, **When** hệ thống kiểm tra, **Then** ca 20/11 được tách khỏi yêu cầu, trưởng tầng được nhắc dùng "Ghi nhận vắng ca" (feature 008 FR-020) cho ca này, hai ca còn lại vẫn chờ duyệt; khi mọi ca của yêu cầu đã bắt đầu, yêu cầu chuyển "Hết hiệu lực".
8. **Given** yêu cầu ở Chờ duyệt, **When** C hủy yêu cầu, **Then** yêu cầu chuyển "Đã hủy"; **Given** yêu cầu đã áp dụng, **When** C muốn đi làm lại ca 22/11, **Then** trưởng tầng dùng "Bổ sung nhân viên vào ca" (feature 008 FR-018), yêu cầu nghỉ không bị sửa.
9. **Given** yêu cầu nghỉ ba ca 20 → 22/11 ở Chờ duyệt và ca 21/11 không có người thay, **When** trưởng tầng duyệt ca 20/11 và 22/11, từ chối ca 21/11 kèm lý do, **Then** yêu cầu chuyển "Đã áp dụng một phần"; FR-029 chỉ áp cho ca 20/11 và 22/11; C vẫn có tên trong ca 21/11 và được thông báo kết quả từng ca (Q-182). **When** trưởng tầng chọn duyệt một phần mà không nhập lý do cho ca bị từ chối, **Then** hệ thống không ghi lệnh.
10. **Given** ngày 05/10, lịch tháng 11 của tầng 2 chưa được lập, **When** C lập yêu cầu nghỉ "theo ngày" phạm vi tầng 2 cho 10 → 12/11 và trưởng tầng duyệt, **Then** các ngày được ghi "nghỉ có duyệt"; khi trưởng tầng lập lịch tháng 11 từ mẫu, C không được xếp vào ca nào bắt đầu trong 10 → 12/11 và báo cáo lập lịch ghi "nghỉ có duyệt" (Q-184).
11. **Given** lịch tháng 11 đã ở Nháp và C có ca Nháp ngày 11/11, **When** yêu cầu nghỉ "theo ngày" cho 11/11 được duyệt, **Then** C được gỡ khỏi ca Nháp đó cùng phân công Nháp, phủ tối thiểu được tính lại; **When** trưởng tầng sửa tay để xếp lại C vào ca ngày 11/11, **Then** hệ thống cảnh báo và chỉ lưu khi trưởng tầng xác nhận kèm lý do.
12. **Given** yêu cầu "theo ngày" cho 20/11 còn Chờ duyệt, **When** lịch tháng 11 được công bố, **Then** lệnh công bố hiển thị yêu cầu này nhưng không bị chặn, và sau khi công bố yêu cầu chuyển thành dạng "theo ca" gồm ca Đã công bố của C ngày 20/11, vẫn ở Chờ duyệt.
13. **Given** C là Trưởng đoàn của chuyến đi 09:00 → 16:00 ngày 21/11, **When** trưởng tầng mở yêu cầu nghỉ ca ngày 21/11 của C, **Then** hệ thống cảnh báo C là Trưởng đoàn; **When** trưởng tầng duyệt, **Then** nhiệm vụ Trưởng đoàn của C bị gỡ, trưởng tầng phụ trách chuyến đi được báo phân công lại, và điểm danh rời viện của chuyến vẫn bị chặn tới khi có trưởng đoàn mới (Q-185, feature 014 FR-035).
14. **Given** Bác sĩ trực duy nhất của ca toàn viện đêm 21/11 được duyệt nghỉ, **When** yêu cầu được áp dụng, **Then** dấu "Bác sĩ trực" mất cùng việc nghỉ; thông báo Khẩn cấp cần Bác sĩ trực trong ca đó đi theo feature 007 FR-047b, và nếu có dòng phủ cho Bác sĩ thì ca mở cảnh báo thiếu phủ cho Quản lý viện.

---

### User Story 5 - Cảnh báo thiếu phủ, mời người thay *(Bỏ theo Q-397, 2026-10-29)*

Ca thiếu người: Trưởng tầng tự bổ sung nhân viên vào ca (feature 008).

---

### Edge Cases

- **Tháng có 28, 29, 30, 31 ngày; ca qua đêm cuối tháng**: vị trí trong chu kỳ tính từ ngày gốc, không phụ thuộc độ dài tháng; ca đêm bắt đầu ngày cuối tháng thuộc lịch tháng đó (feature 008 FR-012), lịch tháng sau không lập lại.
- **Nhân viên vào nhóm giữa tháng hoặc rời nhóm**: chỉ được xếp các ngày nằm trong khoảng hiệu lực thành viên; thay đổi thành viên không sửa lịch đã lập, chỉ áp cho lần lập sau hoặc khi trưởng tầng "Lập lại" lịch Nháp.
- **Tầng mất trưởng tầng được giao khi có yêu cầu Chờ duyệt**: yêu cầu giữ nguyên Chờ duyệt; Quản lý viện được nhắc giao trưởng tầng tạm (feature 008 FR-024); người phụ trách ca không duyệt thay vì không có quyền D ở dòng "Yêu cầu đổi ca" (4.4).
- **Ca trong yêu cầu bị thay đổi bởi lệnh khác trước khi duyệt** (Hủy ca, người nhường bị ghi nhận vắng hoặc nghỉ việc, người nhận nghỉ việc): yêu cầu chuyển "Hết hiệu lực" ngay, các bên được báo.
- **Hai lệnh gần đồng thời trên cùng yêu cầu** (ví dụ A hủy trong lúc trưởng tầng duyệt, hoặc hai yêu cầu cùng dùng một ca được đồng ý cùng lúc): chỉ lệnh đến trước được ghi (feature 000); lệnh sau bị từ chối và người thực hiện được báo yêu cầu đã được xử lý. Mỗi tầng chỉ có một trưởng tầng được giao tại một thời điểm (Q-84) nên không có hai người duyệt cùng cấp.
- **Người nhận đồng ý rồi đổi ý**: được rút đồng ý khi yêu cầu còn "Chờ duyệt"; yêu cầu chuyển "Người nhận từ chối".
- **Nhận thay làm người nhường có ít giờ hơn trong tháng**: được phép; hệ thống không cân lại giờ. Số giờ đã xếp của hai bên hiển thị cho trưởng tầng khi duyệt (FR-021 (g)).
- **Nghỉ đột xuất trong khoảng CFG-M15-07 trước giờ ca**: vẫn lập được cho ca chưa bắt đầu; nếu ca bắt đầu trước khi duyệt, ca được tách khỏi yêu cầu và xử lý bằng "Ghi nhận vắng ca" (feature 008 FR-020, Q-81).
- **Mất kết nối**: lập, đồng ý, duyệt chỉ thực hiện trực tuyến (FR-043).

## Requirements *(mandatory)*

### Functional Requirements

Mọi yêu cầu dưới đây kế thừa feature 000 (nhóm dữ liệu mục 1.5, lý do bắt buộc cho lệnh nhóm 2, nhật ký, tham số theo mã CFG, "hoặc toàn bộ, hoặc không", mốc thời gian theo Asia/Ho_Chi_Minh DBR-25, chống quyết định đồng thời) và dùng các định nghĩa lịch, ca, mẫu ca, phân công của feature 008. Phân nhóm dữ liệu của feature này:

| Dữ liệu | Nhóm (1.5) | Thao tác được phép |
| --- | --- | --- |
| Yêu cầu đổi ca, yêu cầu nghỉ đột xuất | 2 | Chỉ qua lệnh ở bảng FR-022, FR-031 |

Thuật ngữ dùng trong spec: **Nghỉ có duyệt** là tình trạng của một nhân viên với một ca (dạng "theo ca") hoặc một ngày (dạng "theo ngày") thuộc yêu cầu nghỉ đột xuất đã được duyệt cho ca hoặc ngày đó; người nghỉ có duyệt không được tính phủ, không được tính tỷ lệ phục vụ, không được mời thay và không được xếp khi lập lịch từ mẫu. **Thiếu phủ** là tình trạng của ca khi có dòng yêu cầu phủ tối thiểu không đạt (FR-006). **Người nhường** và **người nhận** là hai bên của yêu cầu đổi ca; **nhận thay** là dạng đổi ca một chiều (FR-019).

#### A. Mẫu xoay ca, nhóm xoay ca và yêu cầu phủ tối thiểu *(Bỏ theo Q-397, 2026-10-29)*

- **FR-001 → FR-007**: *(Bỏ theo Q-397, 2026-10-29)*

#### B. Lập bản nháp lịch ca tháng từ mẫu xoay ca *(Bỏ theo Q-397, 2026-10-29)*

- **FR-008 → FR-015**: *(Bỏ theo Q-397, 2026-10-29)*

#### C. Công bố lịch

- **FR-016, FR-017**: *(Bỏ theo Q-397, 2026-10-29)* Công bố lịch chỉ theo feature 008 FR-016.
- **FR-018**: Sau khi công bố, lịch MUST chỉ thay đổi qua các lệnh của feature 008 (FR-017 → FR-021) hoặc yêu cầu đổi ca, nghỉ đột xuất của spec này.

#### D. Yêu cầu đổi ca

- **FR-019**: Nhân viên có ca Đã công bố chưa bắt đầu (người nhường, A; ca X) MUST lập được yêu cầu đổi ca kèm lý do, chọn đồng nghiệp B (người nhận) và một trong hai dạng: "Đổi hai chiều" — chọn thêm ca Y của B, A nhận Y; hoặc "Nhận thay" — B nhận X, A không nhận ca nào. Điều kiện chung: X (và Y với đổi hai chiều) Đã công bố, chưa bắt đầu; Y cùng phạm vi với X; A ≠ B; B đang Đang làm việc và có tài khoản không Đã khóa; B không có tên trong X; B có ít nhất một vai trò hệ thống chung với A (Q-191; sửa 2026-10-29, Q-397); X (và Y) không nằm trong yêu cầu đổi ca hoặc nghỉ đột xuất nào khác đang mở. Riêng đổi hai chiều: A không có tên trong Y và A thỏa điều kiện vai trò tương ứng với B trong Y. Hai ca khác phạm vi MUST NOT được đổi; điều người giữa hai phạm vi dùng "Bổ sung nhân viên vào ca" của feature 008 FR-018. *(Nguồn: UC-54, BR-M09-10, 13.2; Clarification 2026-09-27, đề xuất Q-179, Q-191)*
- **FR-020**: Với đổi hai chiều, khi A chọn ca Y, hệ thống MUST chỉ cho A thấy các ca của B thỏa FR-019 và chỉ các trường ngày, giờ bắt đầu, giờ kết thúc, mẫu ca; A MUST NOT thấy các ca khác, phân công hay người cùng ca của B. *(Nguồn: feature 008 FR-054, 19.3)*
- **FR-021**: Khi lập, khi B đồng ý và ngay trước khi duyệt, hệ thống MUST kiểm tra trạng thái sau khi đổi và hiển thị cho người đang thao tác (B chỉ thấy phần liên quan đến chính mình): (a) chồng giờ của A hoặc B với ca khác (BR-M09-03, feature 008 FR-013) — chặn; (b) chuỗi giờ làm liên tục của A hoặc B vượt CFG-M09-02 (feature 008 FR-014) — cảnh báo; (c) các phân công Tương lai của A trong X (và của B trong Y) mà người kia không đạt feature 008 FR-030 (BR-M09-01) — liệt kê "không chuyển được" kèm loại giấy phép/đào tạo thiếu; (d) người phụ trách ca X, Y sau khi đổi (feature 008 FR-015); (f) tỷ lệ phục vụ của X, Y trước/sau (feature 008 FR-034); (g) số giờ đã làm và đã xếp trong tháng của A và B trước/sau (FR-034), chỉ hiển thị cho người duyệt; (h) A hoặc B đang là Trưởng đoàn hoặc người đi cùng của một chuyến đi chồng thời gian với ca mình rời (feature 014 FR-035) — cảnh báo, không chặn (Q-185). Với nhận thay, các mục liên quan đến Y và việc A vào Y không áp dụng. Kiểm tra lúc duyệt MUST dùng dữ liệu tại thời điểm duyệt, không dùng kết quả lúc lập. *(Nguồn: BR-M09-10, BR-M09-01, BR-M09-03, BR-M09-02)*
- **FR-022**: Trạng thái yêu cầu đổi ca MUST chỉ thay đổi theo bảng sau:

| Từ | Sự kiện / lệnh | Đến | Người thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Lập yêu cầu | Chờ người nhận đồng ý | Người nhường (A) | FR-019; không vi phạm FR-021 (a); lý do | Thông báo B |
| Chờ người nhận đồng ý | Đồng ý | Chờ duyệt | Người nhận (B) | FR-021 (a) không vi phạm | Thông báo người duyệt (FR-023) và A |
| Chờ người nhận đồng ý | Từ chối | Người nhận từ chối | Người nhận (B) | Lý do tùy chọn | Thông báo A |
| Chờ duyệt | Rút đồng ý | Người nhận từ chối | Người nhận (B) | — | Thông báo A và người duyệt |
| Chờ người nhận đồng ý, Chờ duyệt | Hủy yêu cầu | Đã hủy | Người nhường (A) | — | Thông báo B (và người duyệt nếu đã ở Chờ duyệt) |
| Chờ duyệt | Duyệt | Đã áp dụng | Người duyệt (FR-023) | FR-021 (a) không vi phạm; xác nhận kèm lý do nếu có FR-021 (b); FR-021 (d) được giải quyết | FR-024, trong cùng một lần |
| Chờ duyệt | Từ chối | Từ chối | Người duyệt (FR-023) | Lý do bắt buộc | Thông báo A, B |
| Chờ người nhận đồng ý, Chờ duyệt | Tới giờ bắt đầu của ca sớm hơn trong X, Y (nhận thay: của X); hoặc X, Y bị hủy; hoặc A, B không còn thỏa FR-019 do lệnh khác (vắng ca, nghỉ đột xuất, nghỉ việc, bị rút) | Hết hiệu lực | Hệ thống | — | Thông báo A, B (và người duyệt nếu đã ở Chờ duyệt) |

  Yêu cầu đổi ca là một loại yêu cầu phê duyệt dùng chung của feature 000 (FR-031) với các sai khác được khai báo là ngoại lệ ở feature 000 FR-031: không có Nháp (lập là gửi ngay); thêm bước "Chờ người nhận đồng ý" trước "Chờ duyệt" và trạng thái cuối "Người nhận từ chối"; thêm trạng thái cuối "Hết hiệu lực" do hệ thống đặt khi ca bắt đầu hoặc thay đổi, không phải vì quá hạn duyệt (feature 000 FR-031a không bị vi phạm); Đã duyệt và Đã áp dụng xảy ra cùng lúc vì hiệu lực là ngay khi duyệt. "Từ chối" chỉ dùng cho quyết định của người duyệt. Người duyệt tự duyệt khi là A hoặc B MUST theo Q-10 (lý do bắt buộc, nhật ký "tự duyệt"). *(Nguồn: BR-M09-10, UC-54, feature 000 FR-031, Q-10)*

- **FR-023**: Người duyệt yêu cầu đổi ca MUST là trưởng tầng được giao của tầng/khu vực của X và Y tại thời điểm duyệt (dòng "Yêu cầu đổi ca" cột Trưởng tầng "D", 4.4). Khi tầng/khu vực tạm chưa có trưởng tầng được giao, yêu cầu MUST giữ Chờ duyệt và Quản lý viện được nhắc giao trưởng tầng tạm (feature 008 FR-024). Với ca phạm vi toàn viện, người duyệt MUST là Quản lý viện; đây là ngoại lệ của dòng "Yêu cầu đổi ca" (4.4, Quản lý viện "X") và của Q-15, chỉ áp cho ca toàn viện. *(Nguồn: BR-M09-10, 4.4, 13.3, Q-15; Clarification 2026-09-27, đề xuất Q-177)*
- **FR-024**: Khi yêu cầu được duyệt, hệ thống MUST trong cùng một lần: (a) gỡ A khỏi X và thêm B vào X; với đổi hai chiều, thêm gỡ B khỏi Y và thêm A vào Y; lịch sử ca ghi "đổi ca theo yêu cầu" hoặc "nhận thay theo yêu cầu" kèm mã yêu cầu; các mục (b) → (e) liên quan đến Y chỉ áp cho đổi hai chiều; (b) với mỗi phân công Tương lai của A trong X: hủy phân công (feature 008 FR-027) và lập phân công tương ứng (cùng đối tượng, loại công việc giới hạn, vai trò chính/hỗ trợ, nhiệm vụ Điều dưỡng phụ trách, thời gian) cho B nếu đạt feature 008 FR-028, FR-030; không đạt thì không lập, công việc và liều liên quan theo feature 005, 006 thành chung của tầng, và trưởng tầng được nhắc phân công lại; làm tương tự cho phân công của B trong Y; (c) nếu A là người phụ trách X: B thành người phụ trách khi thỏa feature 008 FR-015, nếu không người duyệt MUST chọn trong lệnh duyệt một người khác có tên trong X thỏa FR-015, nếu không có ai thì lệnh duyệt MUST bị từ chối — khác với nghỉ đột xuất (FR-029 (d)) vì đổi ca là tự nguyện và có thể chờ tới khi có người phụ trách, còn nghỉ là bắt buộc (Q-187); làm tương tự cho Y; việc đổi người phụ trách MUST có hiệu lực như lệnh "Chuyển người phụ trách ca" của feature 008 FR-019, nên nhiệm vụ, quyền theo nhiệm vụ, vai trò người bàn giao, người xác nhận bàn giao và người tạm nhận (feature 008 FR-038, FR-044a) chuyển theo; (d) tính lại tỷ lệ phục vụ (feature 008 FR-035) của X, Y; (e) thông báo A, B, người phụ trách X, Y; (f) với ca toàn viện, nếu A mang dấu "Bác sĩ trực" trong X (feature 008 FR-023), dấu này chuyển sang B (B có vai trò Bác sĩ theo FR-019); làm tương tự cho Y; (g) nếu A (hoặc B) là Trưởng đoàn hoặc người đi cùng của chuyến đi chồng thời gian với ca mình rời, gỡ nhiệm vụ đó (feature 014 FR-035) và báo trưởng tầng phụ trách chuyến đi phân công lại; nhiệm vụ không tự chuyển cho người nhận (Q-185). *(Nguồn: BR-M09-10, BR-M09-01, BR-M09-03, 13.4, 2.4; Q-185, Q-187)*
- **FR-025**: Với yêu cầu đổi ca ở Chờ người nhận đồng ý hoặc Chờ duyệt, khi ca sớm hơn trong X, Y còn CFG-M09-11 (đề xuất, mặc định \[24 giờ\]) tới giờ bắt đầu, hệ thống MUST nhắc người đang phải hành động (B hoặc người duyệt) và báo A; nếu yêu cầu được lập hoặc chuyển sang bước mới khi đã ở trong khoảng này, nhắc MUST được gửi ngay tại thời điểm đó. Nhắc theo CFG-M15-05, CFG-M15-06 của feature 000 FR-031a vẫn áp dụng nếu tới trước. *(Nguồn: feature 000 FR-031a)*

#### E. Yêu cầu nghỉ đột xuất

- **FR-026**: Nhân viên MUST lập được yêu cầu nghỉ đột xuất cho chính mình, chọn một hoặc nhiều ca Đã công bố chưa bắt đầu của mình (MAY chọn qua khoảng ngày, hệ thống liệt kê các ca trong khoảng), kèm lý do. Trưởng tầng được giao MUST lập được yêu cầu thay cho nhân viên có ca trong tầng mình (ví dụ khi nhân viên gọi điện báo ốm); Quản lý viện MUST lập thay được cho ca phạm vi toàn viện (Q-177). Yêu cầu lập thay ghi người lập, người nghỉ và dấu "lập thay"; nhân viên được thông báo. Mỗi yêu cầu MUST chỉ gồm ca cùng một phạm vi; chọn ca thuộc nhiều phạm vi thì hệ thống tách thành nhiều yêu cầu. Ca đã nằm trong yêu cầu đổi ca hoặc nghỉ đột xuất khác đang mở MUST NOT được chọn. Dạng này gọi là yêu cầu nghỉ "theo ca". *(Nguồn: 13.2, Q-81)*
- **FR-026a**: **(Sửa 2026-10-29, Q-397)** Nhân viên MUST lập được yêu cầu nghỉ đột xuất dạng "theo ngày" cho một hoặc nhiều ngày trong tương lai thuộc tháng mà lịch của phạm vi đã chọn chưa công bố (chưa có lịch, hoặc lịch còn Nháp), kèm lý do và phạm vi (tầng/khu vực hoặc toàn viện); người lập thay và người duyệt như FR-026, FR-028 theo phạm vi đó (toàn viện: Quản lý viện). Khi duyệt: nếu nhân viên đã có tên trong ca Nháp bắt đầu trong các ngày đó, hệ thống MUST gỡ nhân viên khỏi các ca đó (feature 008); nếu nhân viên là người phụ trách của ca Nháp đó, ca MUST để trống người phụ trách và người lập lịch được báo để chọn lại (lịch không công bố được cho tới khi có, feature 008 FR-016). Xếp nhân viên vào ngày đã nghỉ có duyệt thì feature 008 cảnh báo (`APPROVED_LEAVE_DAY`). Lịch công bố khi yêu cầu còn chờ thì yêu cầu chuyển dạng "theo ca" (Q-184).
- **FR-027**: Trước khi duyệt, hệ thống MUST hiển thị cho người duyệt, với từng ca của yêu cầu: tỷ lệ phục vụ trước/sau (feature 008 FR-034), nhân viên có phải người phụ trách ca không, các phân công của nhân viên trong ca, nhân viên có phải Trưởng đoàn hoặc người đi cùng của chuyến đi chồng thời gian với ca không (Q-185), và vai trò sẽ thiếu khi ca sẽ thiếu phủ; người duyệt MAY tra cứu người thay theo FR-037 (**sửa, 2026-10-06, Q-307:** không có danh sách gợi ý). Việc ca sẽ thiếu phủ MUST NOT chặn duyệt. *(Nguồn: BR-M09-11, BR-M09-02)*
- **FR-028**: Người duyệt yêu cầu nghỉ đột xuất MUST như FR-023 (trưởng tầng được giao của phạm vi; ca toàn viện: Quản lý viện, Q-177). Người duyệt MUST quyết định được cho từng ca còn lại trong yêu cầu: duyệt tất cả, từ chối tất cả (lý do bắt buộc), hoặc duyệt một phần — chọn ít nhất một ca được duyệt và các ca còn lại bị từ chối, lý do bắt buộc cho phần bị từ chối. Tác động FR-029 MUST chỉ áp cho các ca được duyệt; các ca bị từ chối giữ nguyên trong lịch. Kết quả từng ca MUST được lưu trong yêu cầu và thông báo cho nhân viên (và người lập thay). *(Nguồn: 13.2, 4.4; Clarification 2026-09-27, đề xuất Q-182)*
- **FR-029**: Khi yêu cầu được duyệt, hệ thống MUST trong cùng một lần, với mỗi ca của yêu cầu: (a) đánh dấu nhân viên "nghỉ có duyệt" (giữ tên trong lịch sử ca, không tính tỷ lệ phục vụ); (b) hủy phân công Tương lai của nhân viên trong ca (feature 008 FR-027, người thực hiện "Hệ thống"); (c) yêu cầu feature 005 chuyển công việc chưa đóng của nhân viên trong ca đó thành công việc chung (feature 005 FR-032) và feature 006 chuyển liều tương ứng thành liều chung; (d) nếu nhân viên là người phụ trách ca: người duyệt chọn người phụ trách mới thỏa feature 008 FR-015 trong cùng lệnh, có hiệu lực như lệnh "Chuyển người phụ trách ca" của feature 008 FR-019; không có ai thì lệnh duyệt vẫn được thực hiện và ca được gắn dấu "thiếu người phụ trách" (feature 008 FR-019), vì nghỉ là bắt buộc (Q-187); (e) tính lại tỷ lệ phục vụ; (f) thông báo nhân viên, người lập thay (nếu có), người phụ trách ca; (g) nếu nhân viên mang dấu "Bác sĩ trực" trong ca, dấu mất cùng với việc nghỉ; khi ca không còn Bác sĩ trực, thông báo dành cho Bác sĩ trực theo feature 007 FR-047b; (h) nếu nhân viên là Trưởng đoàn hoặc người đi cùng của chuyến đi chồng thời gian với ca, gỡ nhiệm vụ đó (feature 014 FR-035) và báo trưởng tầng phụ trách chuyến đi phân công lại (Q-185). Ca, phân công và công việc của nhân viên ngoài các ca của yêu cầu MUST giữ nguyên (Q-91). *(Nguồn: 13.2, Q-91, BR-M04-13, BR-M09-04 phần phạm vi, 2.4; Q-185, Q-187)*
- **FR-030**: Khi một ca của yêu cầu tới giờ bắt đầu mà yêu cầu chưa được quyết định, hệ thống MUST tách ca đó khỏi yêu cầu (ghi chú trong yêu cầu), nhắc trưởng tầng dùng "Ghi nhận vắng ca" (feature 008 FR-020) nếu nhân viên thực tế vắng; yêu cầu MUST chuyển "Hết hiệu lực" khi không còn ca nào. Nhắc theo FR-025 (CFG-M09-11) áp tương tự cho yêu cầu nghỉ đột xuất, tính theo ca sớm nhất còn lại. *(Nguồn: Q-81)*
- **FR-031**: Trạng thái yêu cầu nghỉ đột xuất MUST chỉ thay đổi theo bảng sau:

| Từ | Sự kiện / lệnh | Đến | Người thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Lập yêu cầu | Chờ duyệt | Nhân viên; Trưởng tầng (lập thay); Quản lý viện (lập thay, ca toàn viện) | FR-026; lý do | Thông báo người duyệt; báo nhân viên nếu lập thay |
| Chờ duyệt | Duyệt tất cả | Đã áp dụng | Người duyệt (FR-028) | Còn ít nhất một ca chưa bắt đầu; người phụ trách ca được xử lý theo FR-029 (d) | FR-029 cho mọi ca, trong cùng một lần |
| Chờ duyệt | Duyệt một phần | Đã áp dụng một phần | Người duyệt (FR-028) | Ít nhất một ca được duyệt và ít nhất một ca bị từ chối; lý do cho phần bị từ chối; FR-029 (d) cho ca được duyệt | FR-029 chỉ cho ca được duyệt, trong cùng một lần; thông báo kết quả từng ca |
| Chờ duyệt | Từ chối tất cả | Từ chối | Người duyệt (FR-028) | Lý do bắt buộc | Thông báo nhân viên (và người lập thay) |
| Chờ duyệt | Hủy yêu cầu | Đã hủy | Nhân viên; người lập thay | — | Thông báo người duyệt |
| Chờ duyệt | Mọi ca đã tới giờ bắt đầu, bị hủy, hoặc nhân viên đã bị gỡ khỏi các ca bởi lệnh khác; với dạng "theo ngày": mọi ngày đã rời yêu cầu, hoặc nhân viên chuyển Nghỉ việc | Hết hiệu lực | Hệ thống | — | Thông báo nhân viên và người duyệt |
| Chờ duyệt (dạng "theo ngày") | Lịch của tháng được công bố | Chờ duyệt (dạng "theo ca") | Hệ thống | — | Các ngày còn chờ chuyển thành ca Đã công bố tương ứng (FR-026a); báo người duyệt |

  Yêu cầu Đã áp dụng hoặc Đã áp dụng một phần MUST NOT bị sửa hay rút; ca bị từ chối trong yêu cầu duyệt một phần MAY được đưa vào một yêu cầu mới; nhân viên đi làm lại một ca đã nghỉ thì trưởng tầng dùng "Bổ sung nhân viên vào ca" (feature 008 FR-018). Người duyệt đồng thời là người lập thay MUST theo Q-10. *(Nguồn: 13.2, feature 000 FR-031, Q-10)*

#### F. Thiếu phủ và mời người thay *(Bỏ theo Q-397, 2026-10-29)*

- **FR-032 → FR-039**: *(Bỏ theo Q-397, 2026-10-29)* Ca thiếu người: Trưởng tầng tự bổ sung nhân viên vào ca (feature 008 FR-018).

#### G. Quyền, giao tiếp, quyền riêng tư và nhật ký

- **FR-040**: Quyền của feature này MUST theo Permission Matrix 4.4 và mục 19.3 (19.3 là căn cứ khi khác nhau):

| Chức năng | Quản lý viện | Trưởng tầng | Bác sĩ, Điều dưỡng, Nhân viên chăm sóc, Bếp, Hành chính (sửa, Q-282, Q-283) | Người thân |
| --- | --- | --- | --- | --- |
| Yêu cầu đổi ca (UC-54) | Xem; duyệt ca toàn viện (ngoại lệ Q-177) | Duyệt (tầng được giao) | Lập, đồng ý / từ chối khi là người nhận | — |
| Yêu cầu nghỉ đột xuất | Xem; duyệt, lập thay cho ca toàn viện (ngoại lệ Q-177) | Duyệt; lập thay (tầng được giao) | Lập cho chính mình | — |

  *(Nguồn: 4.4 dòng "Lịch ca, phân công", "Yêu cầu đổi ca"; 13.3; Q-15; Clarification 2026-09-27, đề xuất Q-177)*

- **FR-041**: Bảng tổng hợp giao tiếp với feature khác:

| Chiều | Feature | Sự kiện / dữ liệu |
| --- | --- | --- |
| Nhận, Gọi | 008 | Nhận mẫu ca, trạng thái làm việc, giấy phép/đào tạo và hiệu lực, giao trưởng tầng, ca, phân công, vắng ca, tỷ lệ phục vụ; gọi tạo lịch Nháp, kiểm tra FR-013 → FR-015, FR-028, FR-030, lệnh Bổ sung nhân viên vào ca, hủy/lập phân công, gắn dấu thiếu người phụ trách, cung cấp dấu "nghỉ có duyệt" cho FR-034 của 008 |
| Nhận | 002 | Vai trò hệ thống, trạng thái tài khoản liên kết |
| Gọi | 005, 006 | Chuyển công việc, liều của nhân viên trong ca nghỉ hoặc phân công không chuyển được thành chung |
| Cung cấp | 009 | Sự kiện thông báo, mức độ, người nhận (FR-044) |
| Cung cấp | 016 | Số yêu cầu đổi ca, nghỉ đột xuất theo kết quả và nhân viên, số giờ làm theo tháng |

- **FR-042**: **(Sửa 2026-10-29, Q-397)** Yêu cầu đổi ca và yêu cầu nghỉ đột xuất MUST được lưu đủ để feature 016 tính: số yêu cầu theo trạng thái cuối; thời gian từ lập tới quyết định; phân bố số giờ làm trong tháng theo nhân viên. *(Nguồn: 18.5)*
- **FR-043**: Lập, đồng ý, từ chối, duyệt, hủy yêu cầu MUST chỉ thực hiện khi trực tuyến, để quyết định là duy nhất (Q-01). Nhân viên MAY xem ngoại tuyến yêu cầu và lời mời của chính mình; bản lưu trên thiết bị MUST NOT chứa lý do của người khác, kết quả tra cứu người thay hay số giờ làm của người khác (feature 008 FR-060). *(Nguồn: Q-01, NFR-08)*
- **FR-044**: Mức độ thông báo (để feature 009 chọn kênh theo BR-M13-01) MUST là: Nhẹ — nhắc chọn lại người phụ trách ca Nháp (FR-026a), yêu cầu đổi ca mới, người nhận đồng ý/từ chối, kết quả duyệt, yêu cầu hết hiệu lực; Trung bình — nhắc yêu cầu sắp tới giờ ca (FR-025), yêu cầu nghỉ đột xuất mới. Spec này không phát thông báo mức Khẩn cấp. Theo feature 009 FR-016a (Q-98), nhân viên đang ngoài ca chỉ nhận thông báo Trung bình trong ứng dụng; vì vậy lời mời nhận ca thay, yêu cầu đổi ca gửi người nhận và nhắc gửi người nhận đang ngoài ca MUST NOT được gửi qua tin nhắn. Spec không thêm ngoại lệ cho quy tắc này; khi cần lấp ca gấp, người gửi lời mời liên hệ ngoài hệ thống rồi dùng "Bổ sung nhân viên vào ca" (FR-037) (Q-186). *(Nguồn: BR-M13-01, 17, feature 009 FR-016a; Q-186)*
- **FR-045**: Lý do nghỉ đột xuất và lý do đổi ca MUST chỉ hiển thị cho người lập, nhân viên liên quan (người nghỉ; A và B), người duyệt và Quản lý viện; lý do từ chối của người nhận chỉ hiển thị cho A. Thông báo gửi người khác (người phụ trách ca, người được mời) và mọi thông báo qua kênh ngoài ứng dụng MUST NOT nêu lý do, chỉ nêu sự kiện, ca và tác động. Kết quả tra cứu người thay (số giờ đã làm, giờ đã xếp, cảnh báo giờ làm liên tục) MUST chỉ hiển thị cho người nhận cảnh báo ở FR-032 và Quản lý viện; lời mời gửi nhân viên MUST NOT cho biết ai khác được mời. *(Nguồn: 19.3, feature 008 FR-053, FR-054, FR-059)*
- **FR-046**: Mọi lệnh làm thay đổi dữ liệu nhóm 2 và thay đổi danh mục của spec MUST ghi nhật ký gồm người thực hiện, thời điểm, trước/sau, căn cứ quyền và lý do (feature 000); thay đổi do hệ thống (hết hiệu lực, tách ca, mở/đóng cảnh báo) MUST ghi người thực hiện "Hệ thống" và sự kiện kích hoạt; lệnh tự duyệt MUST mang dấu "tự duyệt" (Q-10); lệnh lập thay MUST mang dấu "lập thay". Yêu cầu đã ở trạng thái cuối (Đã áp dụng, Đã áp dụng một phần, Từ chối, Người nhận từ chối, Đã hủy, Hết hiệu lực) MUST NOT bị sửa. Mẫu xoay ca, nhóm xoay ca, lần lập lịch, lần tính phủ, yêu cầu đổi ca, yêu cầu nghỉ đột xuất, cảnh báo thiếu phủ, lời mời MUST được lưu không ngắn hơn CFG-M15-04 (mặc định \[10 năm\]) và không ngắn hơn ca liên quan (feature 008 FR-057). *(Nguồn: 1.5, 19.4, feature 000, Q-10)*

### Key Entities *(include if feature involves data)*

- **Yêu cầu đổi ca** – nhóm 2, loại yêu cầu phê duyệt dùng chung (feature 000): dạng (đổi hai chiều / nhận thay), người nhường, ca X, người nhận, ca Y, lý do, kết quả kiểm tra tại từng bước, người duyệt, người phụ trách được chọn khi duyệt, trạng thái (bảng FR-022), các mốc thời gian. Theo feature 000 FR-032: ngày hiệu lực mong muốn là giờ bắt đầu của ca sớm hơn trong X, Y; ngày hiệu lực thực tế là thời điểm duyệt.
- **Yêu cầu nghỉ đột xuất** – nhóm 2, loại yêu cầu phê duyệt dùng chung: dạng (theo ca / theo ngày), nhân viên nghỉ, người lập, dấu lập thay, các ca (và ca đã tách) hoặc các ngày, với kết quả từng ca hoặc từng ngày (được duyệt / bị từ chối kèm lý do), ghi chú "xếp lại dù đã duyệt nghỉ" (FR-026a), lý do, người duyệt, trạng thái (bảng FR-031). Ngày hiệu lực mong muốn là giờ bắt đầu ca sớm nhất (dạng theo ca) hoặc ngày đầu tiên (dạng theo ngày); ngày hiệu lực thực tế là thời điểm duyệt.
- **Nhân viên trong ca** (thực thể của feature 008, spec này thêm thuộc tính): dấu "nghỉ có duyệt" (mã yêu cầu nghỉ), căn cứ vào ca "đổi ca theo yêu cầu" / "nhận thay theo yêu cầu" / "nhận lời mời" (mã yêu cầu hoặc lời mời), căn cứ rời ca theo yêu cầu đổi ca.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: *(Bỏ theo Q-397, 2026-10-29)*
- **SC-002**: *(Bỏ theo Q-397, 2026-10-29)*
- **SC-003**: *(Bỏ theo Q-397, 2026-10-29)*
- **SC-004**: Với bộ kiểm thử 200 yêu cầu đổi ca và nghỉ đột xuất (có chồng giờ, giờ làm liên tục, giấy phép hết hạn đúng ngày ca, người phụ trách ca, Bác sĩ trực, Trưởng đoàn chuyến đi, lệnh đồng thời, ca bị hủy khi đang chờ, đổi hai chiều và nhận thay, duyệt một phần, nghỉ dạng "theo ngày" trước và sau khi lịch công bố), 0 thay đổi lịch được áp dụng khi thiếu đồng ý của người nhận hoặc thiếu duyệt, 0 nhân viên có hai ca chồng giờ, 0 phân công được lập cho người thiếu giấy phép/đào tạo, và 100% yêu cầu áp dụng theo "hoặc toàn bộ, hoặc không".
- **SC-005**: *(Bỏ theo Q-397, 2026-10-29)*
- **SC-006**: Nhân viên lập xong một yêu cầu đổi ca hai chiều hoặc nghỉ đột xuất ba ca, tính từ lúc mở chức năng tới lúc gửi, trong không quá 2 phút (trung vị, ít nhất 10 nhân viên thực tế, lần dùng đầu tiên sau hướng dẫn); sau một tháng vận hành thử, ít nhất 90% yêu cầu được quyết định trước giờ bắt đầu ca liên quan.
- **SC-007**: *(Bỏ theo Q-397, 2026-10-29)*
- **SC-008**: Với bộ kiểm thử mọi loại thông báo của FR-044 gửi qua kênh ngoài ứng dụng, 0 thông báo chứa lý do nghỉ, lý do đổi ca hay tên người được mời khác; 100% lệnh của spec có nhật ký với căn cứ quyền. Mọi quy tắc theo thời gian (CFG-M09-06, CFG-M09-10, CFG-M09-11, hết hiệu lực khi tới giờ ca, tách ca) kiểm được bằng đồng hồ giả lập.

## Assumptions

- Số feature `015` theo bản đồ feature ở mục 4.2 `docs/phan-tich-yeu-cau.md` (UC-50 phần lập lịch từ mẫu, UC-54) và ranh giới đã chốt Q-77.
- Q-09 (đã chốt theo mặc định ngày 2026-10-01, xem 24.2) được dùng: cấu hình tham chiếu hai ca ngày/đêm; spec không giả định mẫu xoay ca cụ thể, mọi chu kỳ đều cấu hình được (FR-001).
- "Nhân viên phù hợp đang không có ca" (BR-M09-11) hiểu là không có ca chồng giờ với ca cần thay; nhân viên có ca ở ngày đó nhưng không chồng giờ vẫn mời được, kèm cảnh báo giờ làm liên tục.
- Yêu cầu đổi ca chỉ áp cho ca Đã công bố chưa bắt đầu; yêu cầu nghỉ đột xuất áp cho ca Đã công bố chưa bắt đầu (dạng "theo ca") hoặc cho ngày thuộc tháng chưa công bố lịch (dạng "theo ngày", Q-184); ca đang diễn ra dùng "Ghi nhận vắng ca" của feature 008 (Q-81).
- Hai tham số mới được đề xuất: CFG-M09-10 nhắc công bố lịch trước đầu tháng, mặc định \[3 ngày\]; CFG-M09-11 nhắc yêu cầu đổi ca, nghỉ đột xuất chưa quyết định trước giờ ca, mặc định \[24 giờ\].
- Quy tắc riêng tư ở FR-045 là mặc định suy ra từ 19.3 và feature 008 FR-053, FR-054, không phải quyết định mới.
- Lời mời nhận ca thay tới người đang ngoài ca chỉ qua hộp thông báo trong ứng dụng (Q-98, Q-186); mục tiêu lấp ca ở SC-007 được đo với ràng buộc này.

## Điểm cần báo lại về tài liệu nguồn

Chưa sửa `docs/`. Các điểm dưới đây cần chủ tài liệu xác nhận.

1. **Permission Matrix 4.4 dòng "Yêu cầu đổi ca"** chỉ cho Quản lý viện quyền "X", trong khi ca toàn viện (Bác sĩ trực, bếp, vệ sinh, hành chính) không có trưởng tầng. Đã chốt Q-177: Quản lý viện duyệt và lập thay cho ca toàn viện. Q-15 cần thêm ngoại lệ thứ hai (hiện chỉ có quyền Duyệt kế hoạch chăm sóc cho điều dưỡng); 19.3 cần ghi quyền này.
2. **4.4 và danh sách UC không có dòng cho nghỉ đột xuất**, mẫu xoay ca, nhóm xoay ca, yêu cầu phủ tối thiểu, lời mời nhận ca thay. Spec giao theo dòng "Yêu cầu đổi ca" và "Lịch ca, phân công" (FR-040); cần thêm dòng và UC cho nghỉ đột xuất. Riêng quyền "lập thay" yêu cầu nghỉ của Trưởng tầng (tầng được giao) và Quản lý viện (ca toàn viện) là "T", vượt cột "D" nếu suy từ dòng "Yêu cầu đổi ca"; dòng mới cho nghỉ đột xuất cần ghi rõ quyền này.
3. **CFG-M09-07 gộp "mẫu xoay ca" (danh mục có cấu trúc) và "yêu cầu phủ tối thiểu" (tham số)**. Nên tách: mẫu xoay ca là danh mục nhóm 1 (FR-001), CFG-M09-07 chỉ còn yêu cầu phủ tối thiểu.
4. **BR-M09-11 chỉ nêu nghỉ đột xuất và đổi ca** làm ca thiếu phủ. Spec mở rộng cảnh báo cho cả ghi nhận vắng ca, nghỉ việc, thay đổi cấu hình (FR-032), vì các sự kiện này cũng làm ca thiếu điều dưỡng.
5. **BR-M09-09 không nêu hành vi khi lịch sinh ra thiếu phủ lúc công bố**; đã chốt Q-178 (chỉ cảnh báo, xác nhận kèm lý do, mở cảnh báo thiếu phủ khi công bố), cần bổ sung vào 13.2 và BR-M09-11. BR-M09-09 cũng không nêu việc gì xảy ra khi lịch đã có trước mốc sinh (FR-014), sinh lại (FR-013), cách xác định người phụ trách ca khi sinh (FR-011).
6. **BR-M09-10 không nêu** phạm vi đổi ca — đã chốt Q-179 (đổi hai chiều và nhận thay một chiều, chỉ cùng phạm vi), cần bổ sung vào BR-M09-10; việc chuyển phân công và người phụ trách ca khi đổi (FR-024), hạn quyết định (FR-022 hàng Hết hiệu lực), và việc người yêu cầu cần thấy ca của đồng nghiệp — ngoại lệ hẹp của feature 008 FR-054 (FR-020).
7. **Vòng đời hai loại yêu cầu khác vòng đời chung của feature 000 FR-031**: yêu cầu đổi ca không có Nháp, có "Chờ người nhận đồng ý", "Người nhận từ chối", "Hết hiệu lực"; yêu cầu nghỉ đột xuất có "Đã áp dụng một phần" (Q-182), "Hết hiệu lực" và chuyển dạng "theo ngày" → "theo ca" (Q-184). Đã thêm ngoại lệ vào feature 000 FR-031 (mục "Cập nhật 2026-09-27 (đồng bộ với spec 015)"); 1.5 và 3.2 YEU_CAU_PHE_DUYET của tài liệu nguồn cần ghi các trạng thái này.
8. **Tham số mới đề xuất** CFG-M09-10, CFG-M09-11 cần thêm vào Phụ lục 25.
9. **ERD 3.2 không có thực thể** cho mẫu xoay ca, nhóm xoay ca, yêu cầu phủ tối thiểu, cảnh báo thiếu phủ, lời mời nhận ca thay; yêu cầu đổi ca và nghỉ đột xuất dùng YEU_CAU_PHE_DUYET. Theo quy ước hiện hành, tab Phân tích yêu cầu không sửa; 19.3 và thân tài liệu là căn cứ.
10. **Đã đồng bộ spec khác (2026-09-27)**, mỗi spec có mục "Cập nhật 2026-09-27 (đồng bộ với spec 015)": feature 008 — FR-012 (cảnh báo nghỉ có duyệt trên lịch Nháp), FR-016 (kiểm tra phủ, một lý do, yêu cầu nghỉ theo ngày), FR-027 (nguồn hủy phân công), FR-034 (loại người nghỉ có duyệt), FR-035 (sự kiện tính lại), FR-049 (dòng 015), FR-054 (ngoại lệ FR-020), FR-055 (ngoại lệ nhật ký), FR-056 (căn cứ "vai trò – lời mời đã gửi"), Key Entities "Nhân viên trong ca"; feature 000 — ngoại lệ vòng đời ở FR-031; feature 002 — ngoại lệ quyền Q-177 so với Q-15; feature 005 — FR-032 thêm nguồn "nghỉ có duyệt"; feature 014 — Trưởng đoàn, người đi cùng bị gỡ khi đổi ca, nghỉ (Q-185).
11. **Các quyết định clarify (2026-09-27)** Q-177 → Q-188 chưa được phản ánh vào `docs/business/nghiep-vu.md`; cần thêm vào bảng 24.3 (hoặc 24.2 sau khi phản ánh vào 13.2, BR-M09-09 → 11, 19.3, Q-15, BR-M13-01 cho Q-186 và 8.9 cho Q-185).
12. **13.2 cần bổ sung câu cho Q-184**: hiện viết "sau khi công bố, đổi ca và nghỉ đột xuất đi qua yêu cầu có duyệt", ngụ ý trước công bố chỉ sửa lịch trực tiếp. Đề xuất thêm: "Trước khi lịch công bố, nghỉ biết trước đi qua yêu cầu nghỉ đột xuất dạng theo ngày; khi được duyệt, hệ thống không xếp nhân viên vào các ngày đó."
13. **BR-M09-11 "nhân viên phù hợp đang không có ca"**: spec hiểu là không có ca chồng giờ với ca cần thay (FR-033); nhân viên có ca khác trong ngày nhưng không chồng giờ vẫn là ứng viên, kèm đánh dấu giờ làm liên tục. Cần chủ tài liệu xác nhận.
14. **BR-M09-11 và CFG-M09-07 không nêu phủ tối thiểu có xét giấy phép**; đã chốt Q-188 (nhân viên chỉ được tính khi có giấy phép, đào tạo bắt buộc với vai trò còn hiệu lực trong toàn ca). Cần bổ sung vào BR-M09-11 hoặc chú giải CFG-M09-07.
15. **Thuật ngữ mới cần bổ sung vào 2.4**: Mẫu xoay ca, Nhóm xoay ca, Phủ tối thiểu, Thiếu phủ, Nghỉ có duyệt (dạng theo ca / theo ngày), Người nhường, Người nhận (ca), Nhận thay, Lời mời nhận ca thay.
16. **Spec 009 không cần sửa** theo Q-186: lời mời và yêu cầu gửi người ngoài ca theo đúng FR-016a (chỉ trong ứng dụng); 17 và BR-M13-01 nên ghi mức độ của các thông báo Module 09 do spec này phát (FR-044).
17. **Quyết định clarify lượt 4 (2026-09-27)**: Q-189 mục "thiếu ca" khi sinh lịch (bổ sung BR-M09-09, BR-M09-11), Q-190 không sửa mẫu xoay ca, ngày gốc của nhóm đã có thành viên (bổ sung 13.2), Q-191 điều kiện vai trò khi đổi ca (bổ sung BR-M09-10) cần phản ánh vào tài liệu nguồn cùng Q-177 → Q-188 (điểm 11).
18. **Đã phản ánh vào `docs/business/nghiep-vu.md` (2026-09-27, theo yêu cầu người dùng)**: Q-177 → Q-191 ở mục 24.2 (và ghi chú 24.3); thuật ngữ mới ở 2.4 (Mẫu xoay ca, Nhóm xoay ca, Phủ tối thiểu, Thiếu ca, Nghỉ có duyệt, Người nhường, người nhận ca, Lời mời nhận ca thay); phần "(Làm rõ, spec 015)" ở 13.2, BR-M09-09, BR-M09-10, BR-M09-11; BR-M13-01 (Q-186); 19.3 và dòng Q-15 (Q-177); Phụ lục 25: chú giải lại CFG-M09-07, thêm CFG-M09-10, CFG-M09-11 (điểm 3, 5, 6, 8, 11 → 15, 17). Còn chờ, theo quy ước không sửa `docs/phan-tich-yeu-cau.md`: UC và dòng Permission Matrix cho nghỉ đột xuất, mẫu xoay ca, nhóm xoay ca, lời mời (điểm 2), ngoại lệ Q-177 ở dòng "Yêu cầu đổi ca" của 4.4 (điểm 1), vòng đời YEU_CAU_PHE_DUYET ở 3.2 (điểm 7), thực thể mới ở ERD (điểm 9); 19.3 là căn cứ khi 4.4 khác.

**(2026-09-28, góp ý nghiệp vụ Q-207 → Q-222) Tài liệu nguồn đã thay đổi, spec cần rà lại.** *(2026-09-29: mọi điểm dưới đây đã xử lý, xem nhãn "Đã xử lý" từng điểm.)* Các điểm dưới đây đã có trong `docs/business/nghiep-vu.md` và `docs/business/luong-nghiep-vu.md`; spec **chưa** được sửa theo, và cần chạy `/speckit-clarify` hoặc cập nhật FR tương ứng.

1. **[Đã xử lý 2026-09-28: FR-006b]** **Phủ tối thiểu không tính Điều dưỡng cho dòng Nhân viên chăm sóc (Q-221, Q-214).** FR-005, FR-007 giữ nguyên cách tính theo vai trò hệ thống; cần ghi rõ để không hiểu nhầm rằng Q-214 cho Điều dưỡng lấp dòng Nhân viên chăm sóc. Đổi ca giữa Điều dưỡng và Nhân viên chăm sóc vẫn theo Q-191.

**(2026-10-05) Thay đổi do chủ dự án chốt Q-280 → Q-286.** *(Đã xử lý 2026-10-06: spec đã sửa theo các quyết định dưới đây, xem mục "Cập nhật 2026-10-06" ở Clarifications)*

- **Q-282** – bỏ vai trò Nhân viên vệ sinh và nghiệp vụ vệ sinh (7.5, BR-M03-08, 11, 12, 14, UC-71 → 73); giường kết thúc phân bổ chuyển "Chờ sẵn sàng", Trưởng tầng hoặc Người phụ trách ca "Xác nhận giường sẵn sàng" (nghi nhiễm thì bắt buộc xác nhận đã khử khuẩn) (BR-M03-09, 10, UC-74); nhân viên không dùng hệ thống chỉ có hồ sơ nhân viên. Ảnh hưởng: phủ tối thiểu và ứng viên thay không còn vai trò Nhân viên vệ sinh.
- **Q-283** – gộp Dinh dưỡng viên vào Bác sĩ; bản gán chế độ ăn do Bác sĩ lập nên không còn bước duyệt riêng (BR-M08-03); cột DDV của Phụ lục 27 gộp vào cột BS. Ảnh hưởng: phủ tối thiểu không còn vai trò Dinh dưỡng viên.

**(2026-10-06) Thay đổi do chủ dự án chốt Q-306 (hệ thống không tự sinh dữ liệu, công việc).** *(Đã xử lý 2026-10-06: spec đã viết lại, xem mục "Cập nhật 2026-10-06, lượt 2" ở Clarifications.)*

- **Q-306** – Lịch ca tháng do Trưởng tầng (tầng được giao) hoặc Quản lý viện lập từ mẫu xoay ca bằng lệnh; tới CFG-M09-06 trước đầu tháng chưa có lịch thì nhắc; bỏ "sinh lỗi thì thử lại lúc 00:00" (13.2, BR-M09-09, Q-189 sửa, CFG-M09-06 đổi nghĩa). Nguyên tắc chung: mọi bản ghi do actor lập bằng lệnh; hệ thống chỉ gợi ý từ dữ liệu đã có, tự chuyển trạng thái theo thời gian, cảnh báo, leo thang, nhắc, kiểm tra và thực hiện tác động kèm theo lệnh (1.3, 24.2). FR cần rà (dò theo từ khóa, chưa đối chiếu từng FR): FR-003, FR-006a, FR-007, FR-008, FR-010, FR-012, FR-013, FR-026a, FR-044, FR-046.

**(2026-10-06) Thay đổi do chủ dự án chốt Q-307 (bỏ gợi ý, đề xuất, yêu cầu và sự cố do hệ thống tự tạo).** *(Đã xử lý 2026-10-06: spec đã viết lại, xem mục "Cập nhật 2026-10-06, lượt 2" ở Clarifications.)*

- **Q-307** – Bỏ gợi ý người thay ca: hệ thống cảnh báo thiếu phủ, Trưởng tầng tự chọn người đang không có ca để mời; số giờ đã làm trong tháng vẫn hiển thị (BR-M09-11, 13.2, 19.3). Người phụ trách ca do người lập lịch chọn (BR-M09-09). Giữ: cảnh báo do hệ thống tạo và leo thang, nhắc, thông báo; bản ghi kèm lệnh của actor; số liệu tính để hiển thị (24.2).

**(2026-10-10) Plan 015, điểm báo lại 1.** *(Đã xử lý 2026-10-10: không cần sửa: dẫn chiếu "4.4", mục 4.2 tra qua bảng ánh xạ ở Phụ lục 26; điểm báo lại 1 nhắc "vệ sinh" là lịch sử trước Q-282.)*
