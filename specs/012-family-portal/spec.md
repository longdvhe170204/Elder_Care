# Feature Specification: Người thân và cổng thông tin gia đình

**Feature Branch**: `012-family-portal`

**Created**: 2026-09-26

**Status**: Draft

**Input**: User description: "Quản lý người thân và cổng thông tin gia đình theo docs/nghiep-vu.md Module 10 (mục 14): quyền theo từng người thân gắn với bản đồng ý chia sẻ dữ liệu; danh sách người được phép đón thay đổi qua xác nhận/duyệt; đăng ký thăm có kiểm tra khung giờ, sức chứa, khoanh vùng; quy trình đón; người thân ở lại; bản tin tuần tự tổng hợp và được điều dưỡng duyệt; phản hồi, khiếu nại có hạn xử lý và leo thang."

## Clarifications

### Session 2026-09-26

- Q: Yêu cầu thay đổi quyền và danh sách được phép đón dùng vòng đời riêng của BR-M10-07 hay là một loại của vòng đời yêu cầu phê duyệt chung (feature 000)? → A: Vòng đời riêng theo BR-M10-07, thêm trạng thái Hủy: Chờ xác nhận → Hiệu lực / Từ chối / Hủy. Xác nhận của người đại diện (qua cổng hoặc bản ký) và duyệt của Quản lý viện đều đưa yêu cầu sang Hiệu lực ngay, không qua Nháp hay "chờ hiệu lực"; nhắc khi chờ lâu theo CFG-M10-11 (ban đầu dùng lại CFG-M15-05/06, tách riêng theo Q-130). Đây là ngoại lệ của vòng đời chung, đã ghi vào spec 000 FR-031 (FR-015, FR-019, FR-020, đề xuất Q-118).
- Q: Lượt thăm đạt mọi kiểm tra của BR-M10-02 có tự được duyệt không? → A: Có; lượt được tạo thẳng ở Đã duyệt và giữ chỗ trong khung; đăng ký không đạt bị từ chối ngay kèm lý do và không tạo lượt. Hành chính vẫn hủy được lượt kèm lý do (FR-031, FR-039, đề xuất Q-119).
- Q: Lượt thăm đã duyệt của người cao tuổi trong vùng vừa bị khoanh vùng xử lý thế nào? → A: Tự chuyển Hủy với lý do "khu đang khoanh vùng", trả chỗ và báo người đăng ký (mức Trung bình); không có trạng thái tạm hoãn; gỡ vùng không khôi phục lượt đã hủy, gia đình đăng ký lại. Danh sách lượt bị hủy vẫn được liệt kê cho Hành chính và Trưởng tầng (FR-037, đề xuất Q-120).

### Session 2026-09-26 (lượt 2, /speckit-clarify)

- Q: Ai được thực hiện quy trình đón (kiểm tra danh sách, xác minh giấy tờ, ghi bàn giao) khi người cao tuổi rời viện? → A: Người có quyền thực hiện lệnh nguồn: Hành chính, Trưởng tầng (Cho tạm vắng); Hành chính, Nhân viên chăm sóc (Điểm danh về bán trú); Hành chính (Kết thúc lưu trú). Permission Matrix 4.4 dòng "Đăng ký thăm, đón" cần sửa cho khớp (FR-022, FR-067, Điểm báo lại 3, đề xuất Q-121).
- Q: Phản hồi được giao cho ai và ai đặt mức ưu tiên? → A: Giao tự động theo nhóm nội dung: chăm sóc, sinh hoạt, ăn uống, sức khỏe, thuốc → Trưởng tầng được giao của tầng (không có thì Quản lý viện); chi phí, hợp đồng, khác → Hành chính. Khiếu nại mặc định Cao, Góp ý và Hỏi đáp mặc định Thường; người phụ trách đổi được mức kèm lý do, hạn tính lại từ thời điểm gửi (FR-059, FR-062, Điểm báo lại 7, đề xuất Q-122).
- Q: Người thân ở lại có cần ai cho phép, và mỗi lượt tính phí bao nhiêu ngày? → A: Hành chính đăng ký, Trưởng tầng được giao của tầng xác nhận thì mới bắt đầu được; phí tính theo số đêm ở lại (một đêm được tính khi lượt ở qua mốc 00:00), ở trong ngày không qua đêm thì không tính phí ngày (FR-040, FR-043, FR-045, User Story 8, Điểm báo lại 8, đề xuất Q-123).
- Q: Bản tin tuần không được điều dưỡng nào duyệt (kể cả sau khi đã nhắc Trưởng tầng lúc quá 48 giờ) thì xử lý thế nào? → A: Quá CFG-M10-12 (mặc định \[96 giờ\]) kể từ lúc sinh, Trưởng tầng được giao của tầng được duyệt thay, vẫn bắt buộc phần giải thích sự cố; tới khi bản nháp kỳ sau được sinh mà vẫn chưa duyệt thì bản cũ chuyển Không gửi và báo Quản lý viện (FR-053, FR-055, FR-057, FR-073, User Story 7, Điểm báo lại 9, đề xuất Q-124).
- Q: Người bán trú về nhà mà không có người thân đến đón có được phép không? → A: Được, khi người cao tuổi bán trú có dấu "được tự về" đang bật; dấu bật/tắt qua yêu cầu BR-M10-07 (người đại diện xác nhận hoặc Quản lý viện duyệt); không bật được khi người cao tuổi có cờ nguy cơ đi lạc. Khi tự về vẫn tạo bản ghi đón loại "Tự về" với thời điểm và nhân viên tiễn; "viện đưa về" chưa hỗ trợ (FR-013, FR-021, FR-026, User Story 2, Điểm báo lại 4, đề xuất Q-125).

### Session 2026-09-26 (lượt 3, sau checklist business-rules)

- Q: Quyền của người thân khi lập quan hệ lần đầu, lúc chưa có người đại diện nào xác nhận được, bật qua đâu? → A: Qua "phiếu đăng ký người thân" có chữ ký của người đại diện (người ký hợp đồng), Hành chính ghi kèm bản scan; mỗi quyền ghi trên phiếu có hiệu lực ngay theo FR-015 (b), kể cả quyền của chính người đại diện; không có phiếu thì mọi quyền giữ tắt và chỉ bật qua yêu cầu BR-M10-07 do Quản lý viện duyệt (FR-008, User Story 1 kịch bản 9, đề xuất Q-126).
- Q: Đặt hoặc thôi người đại diện có cần xác nhận hay duyệt không? → A: Người đại diện đầu tiên do Hành chính đặt kèm bằng chứng, không cần duyệt. Khi người cao tuổi đã có người đại diện, thêm hoặc thôi người đại diện đi qua yêu cầu cùng vòng đời BR-M10-07, có hiệu lực khi một người đại diện hiện có khác người bị tác động xác nhận hoặc Quản lý viện duyệt (FR-005, FR-013, User Story 1 kịch bản 10, đề xuất Q-127).
- Q: Lượt người thân ở lại có giới hạn gì về người cùng phòng và số người? → A: Với phòng có từ hai giường đang sử dụng, Trưởng tầng khi xác nhận MUST ghi đã hỏi ý kiến người cùng phòng (hoặc người đại diện của họ) và kết quả; mỗi người cao tuổi có tối đa CFG-M10-10 (đề xuất, mặc định \[1\]) lượt Đã xác nhận hoặc Đang ở lại chồng thời gian; một người thân không có hai lượt chồng thời gian (FR-040, FR-045, User Story 8 kịch bản 6, đề xuất Q-128).
- Q: BR-M10-09 xét "sự cố mức trung bình trở lên" theo mức nào khi mức sự cố thay đổi? → A: Mức cao nhất mà sự cố từng có tính tới thời điểm duyệt, với sự cố có thời điểm xảy ra trong kỳ; sự cố ở trạng thái Đã hủy (feature 007) không tính (FR-053, User Story 7 kịch bản 8, đề xuất Q-129).

### Session 2026-09-26 (lượt 4, sau checklist consistency)

- Q: Mốc nhắc yêu cầu thay đổi quyền chờ lâu và mốc Trưởng tầng duyệt thay bản tin có dùng chung CFG-M15-05/06 của yêu cầu phê duyệt (feature 000) không? → A: Không; tách thành tham số riêng: CFG-M10-11 nhắc và báo khi yêu cầu BR-M10-07 Chờ xác nhận lâu (đề xuất, mặc định \[48 giờ / 96 giờ\]); CFG-M10-12 mốc Trưởng tầng được duyệt thay bản tin (đề xuất, mặc định \[96 giờ\]). Các quyết định Q-118 và Q-124 ở trên được hiểu theo tham số mới (FR-019, FR-053, FR-055, FR-057, FR-067, đề xuất Q-130).

### Cập nhật 2026-09-27 (đồng bộ với spec 010)

Spec 010 và tài liệu nguồn (BR-M10-10, UC-79, 4.4 dòng "Đề nghị mua hộ", chú thích ¹⁶) đã chốt các điểm dưới đây. Sửa theo:
- **Q-133:** che tên thuốc trên bảng chi phí khi người thân không có quyền xem sức khỏe (FR-046).
- **Q-136:** thành phần chi phí tạm tính trong bản tin (FR-052).
- **BR-M11-07, Q-137:** người đại diện đồng ý hoặc từ chối đề nghị mua hộ trên cổng, và thấy phần mua hộ vượt số đã đồng ý (FR-046a mới).
- **FR-067** thêm dòng "Đề nghị mua hộ"; bảng giao tiếp với feature 010 được sửa.

Thông báo về mua hộ do feature 010 phát ra (feature 010 FR-044), không nằm trong FR-069.

### Cập nhật 2026-09-27 (đồng bộ với spec 011)

Spec 011 đã chốt:
- Người thân ở lại chỉ được tính suất khi lượt chuyển Đang ở lại; lượt bắt đầu sau thời điểm chốt tạo phát sinh "+1"; kết thúc sớm hoặc hủy trước giờ bữa thì suất ăn người thân bị hủy (feature 011 FR-034 (c), FR-043).
- Tỷ lệ ăn và lượng nước cho bản tin lấy từ feature 005, không từ feature 011 (FR-052 được sửa). Feature 011 cung cấp thực đơn chung (loại "chung") và chế độ ăn riêng (loại "sức khỏe") cho cổng (FR-046).
- Người thân gửi đồ ăn có tài khoản và quan hệ Hiệu lực được báo khi đồ ăn Không sử dụng; lý do liên quan dị ứng, chế độ ăn thuộc loại "sức khỏe" (feature 011 FR-057).

### Cập nhật 2026-09-27 (đồng bộ với spec 013)

Spec 013 và tài liệu nguồn (14.6, BR-M12-04, BR-M12-06, BR-M12-09, Q-149 → Q-151, Q-155) đã chốt:
- Mục **đồ gửi** trên cổng (danh sách, trạng thái, lịch sử bàn giao, ảnh) chỉ hiển thị cho người đại diện và người thân có quyền "được phép đón" đang hiệu lực, xét tại thời điểm xem. Đây là quy tắc hiển thị riêng (FR-046b mới), không phải loại thông tin thứ năm ở FR-046; thông báo đồ gửi vẫn mang loại "chung" của feature 009 với người nhận xác định theo nhóm.
- Người đại diện có thêm hai thao tác trên cổng: xác nhận hoặc từ chối **xác nhận người nhận khác** (kể cả khi người nhận là chính người cao tuổi), và ghi hoặc rút **đồng ý cho tự giữ** tiền mặt, trang sức. Hai thao tác này thuộc vòng đời của feature 013 (FR-017b), không đi qua vòng đời "Chờ xác nhận" của FR-020.
- Việc trả đồ gửi dựa trên danh sách được phép đón, người đại diện và giấy tờ tùy thân ở hồ sơ người thân (FR-001, FR-007). Người đại diện không có quyền đón chưa có giấy tờ thì feature 013 cho ghi giấy tờ xuất trình và nhắc Hành chính bổ sung vào hồ sơ; spec này không đổi FR-001.
- Tắt quyền "được phép đón" (FR-017) có hiệu lực ngay cả với việc xem và nhận đồ gửi.

### Cập nhật 2026-09-27 (đồng bộ với spec 014)

Spec 014 và tài liệu nguồn (3.4, 8.8, 19.3, Q-167, Q-173, Q-176) đã chốt:
- Người đại diện ghi, rút "đồng ý về muộn" cho người bán trú vào một buổi hoạt động cụ thể qua cổng (FR-046c mới); rút được tới giờ bắt đầu buổi.
- "Số hoạt động đã tham gia" trong bản tin (FR-052) là số lượt Có mặt trong kỳ, gồm hoạt động cá nhân và chuyến đi (feature 014 FR-033).
- Mục hoạt động trên cổng (loại "chung") không gồm chỉ định hạn chế, mức độ tham gia, cảnh báo cô lập hay kết quả kiểm tra chất lượng.

### Cập nhật 2026-09-27 (đồng bộ với spec 016)

Theo Q-194, báo cáo thăm, phản hồi, bản tin thuộc giai đoạn sau; dòng "Cung cấp 016" của bảng giao tiếp được ghi rõ. Người thân không có dashboard, báo cáo của feature 016 (4.4 dòng "Dashboard, báo cáo").

## Phạm vi

**Trong phạm vi** (Module 10 mục 14.1 → 14.8; BR-M10-01 → 09, BR-M10-10 phần cổng; DBR-02, DBR-03; UC-55 → UC-60; NFR-08 phần lượt xem của người thân; CFG-M10-01 → 03, CFG-M10-04 → 12 (đề xuất); thực thể NGUOI_THAN, QUAN_HE_NGUOI_THAN, LUOT_THAM, PHAN_HOI, BAN_TIN):

1. Hồ sơ người thân, quan hệ với người cao tuổi, vai trò người đại diện (đặt người đại diện đầu tiên; thêm hoặc thôi người đại diện qua xác nhận, Q-127) và người liên hệ chính; phiếu đăng ký người thân làm căn cứ quyền ban đầu (Q-126) (14.1, DBR-02, UC-55).
2. Sáu quyền theo từng người thân, trong đó quyền xem sức khỏe gắn với bản đồng ý chia sẻ dữ liệu (14.1, BR-M01-08, DBR-03).
3. Yêu cầu thay đổi danh sách được phép đón, quyền của người thân, dấu "được tự về" của người bán trú và người đại diện, có xác nhận của người đại diện hoặc duyệt của Quản lý viện (14.1, BR-M10-07, Q-118, Q-125, Q-127).
4. Quy trình đón người cao tuổi: kiểm tra danh sách, xác minh danh tính, ghi thời điểm, ghi người bàn giao; ngoại lệ cho người ngoài danh sách; bán trú "tự về" (14.3, BR-M10-03, UC-58, Q-125).
5. Đăng ký thăm, kiểm tra khung giờ, sức chứa, khoanh vùng, trạng thái người cao tuổi; ghi nhận vào/ra (14.2, BR-M10-02, UC-57).
6. Người thân ở lại chăm sóc (14.4, BR-M10-04).
7. Cổng thông tin người thân: hiển thị theo quyền từng người thân; ghi nhật ký lượt xem sức khỏe (14.5, 14.6, BR-M10-01, NFR-08, UC-56).
8. Bản tin tuần: tự tổng hợp, điều dưỡng nhận xét và duyệt, gửi, theo dõi đã xem (14.5, BR-M10-06, 08, 09, UC-60).
9. Phản hồi, khiếu nại: hạn xử lý theo mức ưu tiên, leo thang, người thân xác nhận đóng hoặc mở lại (14.7, BR-M10-05, UC-59).

**Ngoài phạm vi** (spec này chỉ **nhận** hoặc **cung cấp** dữ liệu):

- Tài khoản người thân (tạo, kích hoạt lại, cấp lại mật khẩu, khóa sau CFG-M01-04), cách tính quyền hiệu lực ba lớp: feature 002 (FR-048, FR-049). Spec này sở hữu dữ liệu mà 002 dùng: quan hệ còn hiệu lực và các quyền được cấp.
- Bản đồng ý chia sẻ dữ liệu (ghi nhận, rút lại, phạm vi): feature 001 (FR-010 → FR-016). Spec này chỉ đọc bản đồng ý.
- Lệnh Cho tạm vắng, Ghi nhận trở về, Kết thúc lưu trú và điều kiện "bàn giao người cao tuổi": feature 004 (FR-051, FR-066). Điểm danh về của bán trú: feature 005. Spec này cung cấp bản ghi đón làm căn cứ cho các lệnh đó.
- Khoanh vùng lây nhiễm và danh sách tiếp xúc: feature 007 (FR-062, FR-067). Spec này nhận trạng thái "đang chịu khoanh vùng" và cung cấp lượt thăm cho truy vết.
- Sinh khoản chi phí, chốt kỳ, bảng chi phí: feature 010. Chốt suất ăn: feature 011. Spec này cung cấp bản ghi người thân ở lại làm nguồn.
- Gửi thông báo, giờ yên tĩnh, lọc nội dung theo loại thông tin, ghi thời điểm gửi và xem: feature 009 (FR-013, FR-014, FR-022, FR-034). Spec này nêu sự kiện, mức, người nhận, loại thông tin (feature 009 FR-043a).
- Yêu cầu thay đổi dịch vụ của người đại diện (nội dung, duyệt, áp dụng): feature 004 (FR-039). Spec này chỉ bảo đảm chỉ người đại diện có quyền này mới gửi được (BR-M10-01).
- Dữ liệu nguồn của bản tin (ăn uống, nước, hoạt động, cân nặng, chỉ số, sự cố, chi phí tạm tính): feature 005, 007, 010, 011, 014.
- Camera (14.6, mục 23): ngoài phạm vi giai đoạn đầu.
- Đồ gửi (tiếp nhận, bàn giao, trả, xác nhận người nhận khác, đồng ý cho tự giữ): feature 013. Spec này chỉ hiển thị mục đồ gửi và nhận quyết định của người đại diện trên cổng (FR-046b).
- Quy tắc dùng chung (nhóm dữ liệu, nhật ký, tham số, đính chính, yêu cầu phê duyệt): feature 000, kế thừa, không lặp lại.

## User Scenarios & Testing *(mandatory)*

Nhân vật dùng chung: người cao tuổi A (Nội trú dài hạn, Đang lưu trú, phòng P201 Tầng 2); R là người đại diện của A; M là người liên hệ chính của A; N là người thân khác của A; X là người chưa có trong danh sách được phép đón; E là người cao tuổi bán trú; H là nhân viên Hành chính; D là Điều dưỡng có phạm vi với A; T là Trưởng tầng Tầng 2; Q là Quản lý viện.

### User Story 1 - Hồ sơ người thân, vai trò và quyền theo từng người thân (Priority: P1)

Hành chính lập hồ sơ người thân cho người cao tuổi: họ tên, quan hệ, số liên hệ, giấy tờ tùy thân, vai trò người đại diện hoặc người liên hệ chính, và sáu quyền cấp riêng cho từng người. Hệ thống bảo đảm mỗi người cao tuổi luôn có đúng một người liên hệ chính và ít nhất một người đại diện, và quyền xem sức khỏe chỉ bật được khi bản đồng ý chia sẻ dữ liệu bao gồm người thân đó.

**Why this priority**: Mọi chức năng khác của module (đón, thăm, cổng, bản tin, thông báo khẩn của feature 009, ký hợp đồng của feature 004) dựa trên dữ liệu này. Cấp sai quyền xem sức khỏe là rủi ro pháp lý (5.1, BR-M01-08).

**Independent Test**: Lập ba người thân R, M, N cho A với vai trò và quyền khác nhau; thử bỏ người liên hệ chính, bỏ người đại diện cuối cùng, bật quyền xem sức khỏe cho N khi bản đồng ý không gồm N; kiểm tra các lệnh bị chặn đúng và dữ liệu cung cấp cho feature 002, 009 đúng.

**Acceptance Scenarios**:

1. **Given** A Đang tiếp nhận chưa có người thân, **When** H lập người thân R (con trai, số điện thoại, CCCD) với vai trò người đại diện và người liên hệ chính, **Then** quan hệ R–A ở trạng thái Hiệu lực; R có quyền "nhận thông báo khẩn" bật và không tắt được (FR-009); R có quyền "được yêu cầu thay đổi dịch vụ" bật (FR-010).
2. **Given** R đang là người liên hệ chính của A, **When** H thực hiện "Đổi người liên hệ chính" sang M kèm lý do, **Then** trong cùng một lần M trở thành người liên hệ chính, R thôi vai trò này; A luôn có đúng một người liên hệ chính (DBR-02); **When** H cố bỏ vai trò người liên hệ chính của M mà không chỉ định người thay, **Then** hệ thống từ chối.
3. **Given** R là người đại diện duy nhất của A Đang lưu trú, **When** H cố thôi vai trò người đại diện của R hoặc kết thúc quan hệ R–A, **Then** hệ thống từ chối vì vi phạm DBR-02; **When** H trước tiên lập yêu cầu đặt N làm người đại diện và yêu cầu này Hiệu lực (R xác nhận hoặc Q duyệt, FR-005), rồi lập yêu cầu thôi vai trò của R, **Then** yêu cầu thôi chỉ Hiệu lực khi N (người đại diện hiện có khác R) xác nhận hoặc Q duyệt, và A luôn còn ít nhất một người đại diện.
4. **Given** bản đồng ý Hiệu lực của A gồm M, không gồm N, **When** có yêu cầu bật quyền xem sức khỏe cho N, **Then** yêu cầu bị từ chối với lý do "bản đồng ý chia sẻ dữ liệu không bao gồm người thân này" (14.1, DBR-03); **When** yêu cầu bật cho M, **Then** yêu cầu được tạo theo BR-M10-07 (User Story 3).
5. **Given** M có quyền xem sức khỏe Hiệu lực, **When** bản đồng ý của A bị rút lại (feature 001), **Then** quyền của M vẫn ghi là bật nhưng không có tác dụng: M không thấy thông tin sức khỏe từ lần truy cập kế tiếp (feature 002 FR-046); danh sách quyền của M hiển thị "không có tác dụng – chưa có bản đồng ý bao gồm người thân này".
6. **Given** N là người thân không phải người đại diện, **When** có yêu cầu bật quyền "được yêu cầu thay đổi dịch vụ" cho N, **Then** hệ thống từ chối (BR-M10-01, FR-010).
7. **Given** người thân K đã có hồ sơ vì là con của người cao tuổi B, **When** H lập quan hệ K–A (K cũng là cháu của A), **Then** hệ thống dùng lại hồ sơ K, tạo quan hệ K–A riêng với vai trò và quyền riêng; quyền của K với B không đổi.
8. **Given** Điều dưỡng D hoặc Trưởng tầng T, **When** cố lập người thân hoặc đổi quyền, **Then** hệ thống từ chối (Permission Matrix dòng "Người thân và quyền": chỉ HC T, QL D, NT T²).
9. **Given** A Đang tiếp nhận, R vừa được đặt làm người đại diện đầu tiên kèm hợp đồng đã ký, **When** H ghi "phiếu đăng ký người thân" do R ký (bản scan) với R được phép đón, xem chi phí; M được phép đón, đăng ký thăm, **Then** các quyền này Hiệu lực ngay với cách xác nhận "bản ký" (Q-126); **When** H lập quan hệ N mà không có phiếu, **Then** mọi quyền của N tắt và chỉ bật qua yêu cầu BR-M10-07.
10. **Given** R là người đại diện của A, **When** H thực hiện "Đặt người đại diện" cho N kèm giấy ủy quyền, **Then** hệ thống tạo yêu cầu ở Chờ xác nhận, N chưa là người đại diện và chưa xác nhận được yêu cầu nào; **When** R xác nhận (hoặc Q duyệt), **Then** N trở thành người đại diện; **When** N (sau khi là người đại diện) gửi yêu cầu thôi vai trò của R, **Then** yêu cầu chờ Q duyệt vì không còn người đại diện nào khác R để xác nhận (Q-127).

---

### User Story 2 - Quy trình đón người cao tuổi (Priority: P1)

Khi người cao tuổi rời viện cùng người thân (tạm vắng, đi chơi với gia đình, bán trú về, kết thúc lưu trú), nhân viên kiểm tra người đón có trong danh sách được phép đón, xác minh danh tính bằng giấy tờ tùy thân, ghi thời điểm và nhân viên bàn giao. Người ngoài danh sách bị chặn; ngoại lệ chỉ được khi người đại diện xác nhận qua cổng hoặc Quản lý viện duyệt.

**Why this priority**: Giao người cao tuổi cho người không được phép là sự cố an toàn nghiêm trọng, đặc biệt với người có suy giảm nhận thức. Feature 004 và 005 đều chờ bản ghi đón làm điều kiện.

**Independent Test**: Với danh sách được phép đón của A gồm R, M; thử đón bởi R có giấy tờ khớp, bởi M có giấy tờ không khớp, bởi X ngoài danh sách (có và không có ngoại lệ); kiểm tra kết quả và bản ghi đón.

**Acceptance Scenarios**:

1. **Given** R thuộc danh sách được phép đón của A, số CCCD đã đăng ký, **When** H chọn "Đón" cho A loại "Tạm vắng – về nhà", chọn R, ghi "đã đối chiếu CCCD – khớp", nhân viên bàn giao T, **Then** bản ghi đón được lưu với thời điểm theo thời gian hệ thống (NFR-09) và căn cứ "trong danh sách"; feature 004 dùng bản ghi này cho lệnh "Cho tạm vắng" (feature 004 FR-048).
2. **Given** M thuộc danh sách, **When** giấy tờ M xuất trình không khớp số đã đăng ký, **Then** lượt đón bị chặn với lý do "xác minh danh tính không đạt"; lần chặn được ghi (người, thời điểm, lý do); không có bản ghi đón nào được tạo.
3. **Given** X không thuộc danh sách, **When** H cố ghi đón bởi X, **Then** hệ thống chặn (BR-M10-03) và cho H lập "yêu cầu ngoại lệ đón" với họ tên, số giấy tờ, quan hệ của X và lý do; R (người đại diện) nhận thông báo mức Trung bình để xác nhận qua cổng, Q nhận yêu cầu duyệt.
4. **Given** yêu cầu ngoại lệ đón X đang chờ, **When** R xác nhận trên cổng, **Then** ngoại lệ có hiệu lực cho đúng một lượt đón A bởi X trong CFG-M10-08 (đề xuất, mặc định \[4 giờ\]); H ghi đón bởi X với căn cứ "ngoại lệ – người đại diện xác nhận"; X **không** được thêm vào danh sách được phép đón.
5. **Given** như trên nhưng R không phản hồi, **When** Q duyệt ngoại lệ kèm lý do, **Then** kết quả như kịch bản 4 với căn cứ "ngoại lệ – Quản lý viện duyệt"; **When** R hoặc Q từ chối, **Then** ngoại lệ chuyển Từ chối, lượt đón vẫn bị chặn.
6. **Given** ngoại lệ đã có hiệu lực lúc 09:00, **When** tới 13:00 (hết CFG-M10-08) X mới đến, **Then** ngoại lệ đã Hết hạn, lượt đón bị chặn như kịch bản 3.
7. **Given** E (bán trú) Có mặt, **When** nhân viên chăm sóc điểm danh về cho E (feature 005) với người đón thuộc danh sách, xác minh đạt, **Then** bản ghi đón loại "Bán trú về" được tạo và feature 005 chuyển E sang "Đã về".
8. **Given** A đang lập hồ sơ kết thúc lưu trú, **When** H ghi đón loại "Kết thúc lưu trú" bởi R, **Then** bản ghi đón được dùng làm điều kiện "bàn giao người cao tuổi" (feature 004 FR-064 (e)).
9. **Given** R vừa bị bỏ khỏi danh sách được phép đón lúc 08:00 (User Story 3 kịch bản 3), **When** R đến đón lúc 08:30, **Then** lượt đón bị chặn như người ngoài danh sách.
10. **Given** E (bán trú, không có cờ nguy cơ đi lạc) có dấu "được tự về" đang bật do R xác nhận, **When** 16:00 E ra về một mình và nhân viên chăm sóc chọn "Tự về" khi điểm danh về, **Then** bản ghi đón loại "Tự về" được tạo với thời điểm, nhân viên tiễn và căn cứ "được tự về", không cần người đón; feature 005 chuyển E sang "Đã về". **Given** bác sĩ gắn cờ nguy cơ đi lạc cho E (feature 001), **When** nhân viên chọn "Tự về" hôm sau, **Then** hệ thống chặn vì dấu "được tự về" không còn tác dụng, E phải có người đón; **When** R gửi yêu cầu bật dấu cho E đang có cờ đi lạc, **Then** yêu cầu bị từ chối (Q-125).
11. **Given** M thuộc danh sách, lúc 09:00 H đã ghi bản ghi đón A bởi M nhưng lệnh Cho tạm vắng chưa thực hiện, **When** lúc 09:20 M bị bỏ khỏi danh sách được phép đón, **Then** bản ghi đón đó chuyển Không dùng ngay và không còn là căn cứ; **When** Điều dưỡng D cố ghi đón, **Then** hệ thống từ chối vì D không có quyền lệnh nguồn (FR-022).

---

### User Story 3 - Thay đổi danh sách được phép đón và quyền của người thân qua xác nhận/duyệt (Priority: P1)

Danh sách người được phép đón và các quyền của người thân là dữ liệu kiểm soát: mọi thay đổi đi qua yêu cầu ở trạng thái Chờ xác nhận, chỉ có hiệu lực khi người đại diện xác nhận (qua cổng hoặc bản ký) hoặc Quản lý viện duyệt. Trong lúc chờ, quyền cũ vẫn áp dụng; riêng yêu cầu **bỏ** người được phép đón có hiệu lực ngay.

**Why this priority**: Đây là lớp bảo vệ trước việc thêm người đón hoặc mở quyền xem trái ý gia đình; cùng User Story 2 tạo thành luồng an toàn cho người cao tuổi.

**Independent Test**: Tạo các yêu cầu thêm X vào danh sách đón, bật quyền xem chi phí cho N, bỏ R khỏi danh sách đón; cho xác nhận, từ chối, hủy; kiểm tra quyền áp dụng trong lúc chờ và sau khi có kết quả.

**Acceptance Scenarios**:

1. **Given** H lập yêu cầu "Thêm X vào danh sách được phép đón của A" với họ tên, số giấy tờ, quan hệ của X và lý do, **When** yêu cầu được lưu, **Then** yêu cầu ở Chờ xác nhận; X chưa được phép đón; R nhận thông báo để xác nhận; Q thấy yêu cầu trong danh sách chờ duyệt.
2. **Given** yêu cầu ở kịch bản 1, **When** R xác nhận trên cổng, **Then** yêu cầu chuyển Hiệu lực, X vào danh sách được phép đón từ thời điểm đó, lịch sử ghi người xác nhận, cách xác nhận "qua cổng", thời điểm; **When** thay vào đó H ghi nhận bản ký của R kèm bản scan, **Then** kết quả tương tự với cách xác nhận "bản ký".
3. **Given** M và N trong danh sách được phép đón, **When** lúc 08:00 H lập tại quầy yêu cầu bỏ N khỏi danh sách theo đề nghị qua điện thoại của M, **Then** N bị bỏ khỏi danh sách ngay lúc 08:00, yêu cầu ở Chờ xác nhận (BR-M10-07); **When** R (người đại diện) từ chối kèm lý do, **Then** N được khôi phục vào danh sách từ thời điểm từ chối, lịch sử giữ khoảng thời gian N không được phép đón; **When** M tự gửi yêu cầu bỏ N trên cổng, **Then** hệ thống từ chối vì M không phải người đại diện và chỉ được bỏ chính mình (FR-014).
4. **Given** yêu cầu bật quyền xem chi phí cho N đang Chờ xác nhận, **When** N đăng nhập cổng, **Then** N vẫn không thấy chi phí (quyền cũ áp dụng); **When** Q duyệt kèm lý do, **Then** N thấy chi phí từ lần truy cập kế tiếp.
5. **Given** yêu cầu đang Chờ xác nhận, **When** người lập hủy kèm lý do, **Then** yêu cầu chuyển Hủy, không tạo tác động; với yêu cầu bỏ người đón đã có hiệu lực tạm, người đó được khôi phục.
6. **Given** N (không phải người đại diện), **When** N cố xác nhận một yêu cầu, **Then** hệ thống từ chối; **When** N gửi yêu cầu thêm người đón qua cổng, **Then** hệ thống từ chối; **When** N gửi yêu cầu bỏ chính N khỏi danh sách đón, **Then** yêu cầu được tạo và có hiệu lực ngay (FR-017).
7. **Given** R là người đại diện gửi qua cổng yêu cầu thêm X, **When** yêu cầu được lưu, **Then** yêu cầu được coi là đã có xác nhận của người đại diện và chuyển Hiệu lực ngay; H nhận thông báo Nhẹ để đối chiếu thông tin của X.
8. **Given** yêu cầu ở Chờ xác nhận quá mốc nhắc của CFG-M10-11 (mặc định \[48 giờ\]), **When** Bộ lập lịch kiểm tra, **Then** người đại diện và Q được nhắc; quá mốc báo của CFG-M10-11 (mặc định \[96 giờ\]) thì Q được báo; yêu cầu không tự Từ chối.


---

### User Story 4 - Đăng ký thăm và ghi nhận vào/ra (Priority: P2)

Người thân có quyền đăng ký thăm chọn ngày, khung giờ và số người đi cùng trên cổng. Hệ thống kiểm tra khung giờ thăm, sức chứa của khung, khu có đang khoanh vùng không và trạng thái người cao tuổi. Khi đến, Hành chính ghi giờ vào, giờ ra; lượt thăm là dữ liệu truy vết tiếp xúc cho feature 007.

**Why this priority**: Giảm tắc nghẽn giờ thăm và là căn cứ truy vết lây nhiễm; nhưng chăm sóc và an toàn (P1) vẫn chạy được khi thăm được ghi thủ công.

**Independent Test**: Với CFG-M10-04 khung 09:00–11:00 sức chứa 10 người, đăng ký lần lượt tới khi đầy; đăng ký ngoài khung; đăng ký khi Tầng 2 khoanh vùng; đăng ký khi A đang Tạm vắng; ghi vào/ra và kiểm tra lượt Không đến.

**Acceptance Scenarios**:

1. **Given** M có quyền đăng ký thăm, khung 09:00–11:00 ngày 05/11 còn 4 chỗ, **When** M đăng ký thăm A khung đó cho 3 người (M và hai người đi cùng ghi họ tên), **Then** lượt thăm được tạo ở Đã duyệt ngay (Q-119), số chỗ còn lại của khung là 1; M nhận kết quả trên cổng.
2. **Given** như trên, **When** N đăng ký cùng khung cho 2 người, **Then** bị từ chối với lý do "khung giờ đã đủ sức chứa" và hệ thống gợi ý các khung còn chỗ.
3. **Given** khung thăm theo CFG-M10-04 là 09:00–11:00 và 15:00–17:00, **When** M đăng ký 13:00, **Then** bị từ chối "ngoài khung giờ thăm".
4. **Given** Tầng 2 đang khoanh vùng (feature 007 FR-065), **When** M đăng ký thăm A, **Then** bị từ chối "khu đang khoanh vùng" (BR-M10-02, BR-M05-11).
5. **Given** A đang Tạm vắng hoặc Điều trị tại bệnh viện, **When** M đăng ký thăm, **Then** bị từ chối "người cao tuổi không có mặt tại viện".
6. **Given** lượt thăm của M ngày 05/11 đã duyệt, **When** ngày 03/11 A chuyển Điều trị tại bệnh viện, **Then** lượt thăm chuyển Hủy với lý do "người cao tuổi không có mặt tại viện" và M nhận thông báo (FR-036).
7. **Given** lượt thăm đã duyệt của M cho A ngày 05/11, **When** ngày 04/11 Tầng 2 bị khoanh vùng, **Then** lượt thăm tự chuyển Hủy với lý do "khu đang khoanh vùng", chỗ được trả cho khung, M nhận thông báo mức Trung bình, lượt xuất hiện trong danh sách lượt bị hủy do khoanh vùng của Hành chính và T (Q-120); **When** ngày 05/11 lúc 08:00 vùng được gỡ, **Then** lượt vẫn Hủy, M phải đăng ký lại.
8. **Given** lượt thăm Đã duyệt, **When** M đến lúc 09:10, H ghi "Vào" và xác nhận danh sách người thực tế vào (tối đa số đã đăng ký), **Then** lượt chuyển Đã vào với giờ vào; **When** 10:20 H ghi "Ra", **Then** lượt chuyển Đã ra với giờ ra.
9. **Given** lượt Đã duyệt khung 09:00–11:00, **When** tới 11:00 chưa ghi Vào, **Then** Bộ lập lịch chuyển lượt sang Không đến và trả chỗ; **Given** lượt Đã vào, **When** quá 11:00 + CFG-M10-07 (đề xuất, mặc định \[30 phút\]) chưa ghi Ra, **Then** H được nhắc ghi giờ ra.
10. **Given** M đến viện không đăng ký trước, khung hiện tại còn chỗ, **When** H lập lượt thăm tại quầy cho M, **Then** hệ thống áp đủ các kiểm tra như đăng ký qua cổng; nếu đạt, H ghi Vào ngay.
11. **Given** lượt thăm của M Đã duyệt, **When** lúc H ghi Vào, Tầng 2 vừa bị khoanh vùng hoặc A vừa chuyển viện, **Then** ghi Vào bị chặn với lý do tương ứng.
12. **Given** khung 15:00–17:00 còn đúng 2 chỗ, **When** M và N gần như cùng lúc đăng ký mỗi người 2 chỗ, **Then** đăng ký được hệ thống ghi nhận trước được Đã duyệt, đăng ký còn lại bị từ chối "khung giờ đã đủ sức chứa" (FR-030 (b)); không lượt nào làm khung vượt sức chứa.
13. **Given** M muốn đổi lượt Đã duyệt từ khung sáng sang khung chiều, **When** M thao tác, **Then** M phải hủy lượt cũ rồi đăng ký lượt mới; không có lệnh đổi khung (FR-035); **When** Điều dưỡng D cố ghi Vào cho một lượt, **Then** hệ thống từ chối (FR-067).

---

### User Story 5 - Cổng thông tin hiển thị theo quyền từng người thân (Priority: P2)

Người thân đăng nhập cổng và chỉ thấy người cao tuổi mình có quan hệ còn hiệu lực. Mỗi phần thông tin hiển thị theo quyền riêng: phần chung (trạng thái lưu trú, lịch sinh hoạt, hoạt động, lượt vắng, lượt thăm), phần sức khỏe (chỉ khi có quyền và có bản đồng ý bao gồm mình), phần chi phí, phần hợp đồng và dịch vụ (người đại diện). Mỗi lần người thân xem thông tin sức khỏe được ghi nhật ký.

**Why this priority**: Là giá trị chính của cổng với gia đình và là nơi lộ dữ liệu dễ xảy ra nhất; phụ thuộc User Story 1.

**Independent Test**: Với R (đại diện, xem chi phí), M (xem sức khỏe, trong bản đồng ý), N (không quyền riêng), mở từng phần trên cổng; kiểm tra phần hiện, phần ẩn và nhật ký lượt xem sức khỏe.

**Acceptance Scenarios**:

1. **Given** M có quyền xem sức khỏe và thuộc bản đồng ý Hiệu lực của A, **When** M mở phần sức khỏe, **Then** M thấy dị ứng, bệnh nền đang Hiệu lực, chỉ số, cân nặng, thuốc đang dùng, sự cố trong phạm vi được chia sẻ; nhật ký ghi M, A, phần đã xem, thời điểm (NFR-08).
2. **Given** N không có quyền xem sức khỏe, **When** N mở cổng, **Then** N không thấy mục sức khỏe; lịch sinh hoạt của N chỉ gồm hoạt động, lượt thăm, lịch đến/về bán trú, không gồm công việc chăm sóc hay giờ thuốc (FR-046).
3. **Given** R có quyền xem chi phí, **When** R mở phần chi phí, **Then** R thấy bảng chi phí do feature 010 cung cấp; **When** M (không có quyền xem chi phí) mở cổng, **Then** M không thấy mục chi phí.
4. **Given** R là người đại diện, **When** R mở phần hợp đồng, **Then** R thấy hợp đồng, phụ lục, trạng thái đặt cọc, không có thao tác sửa (feature 004 chú thích ⁷); M và N không thấy phần này.
5. **Given** N có quan hệ với A, **When** N cố mở trang của người cao tuổi B mà N không có quan hệ, **Then** hệ thống từ chối.
6. **Given** R gửi yêu cầu thay đổi dịch vụ trên cổng, **When** R là người đại diện có quyền "được yêu cầu thay đổi dịch vụ", **Then** yêu cầu được chuyển cho feature 004 (FR-039); **When** N cố gửi, **Then** thao tác không hiển thị với N và yêu cầu trực tiếp bị từ chối (BR-M10-01).
7. **Given** quan hệ của N với A đã kết thúc, **When** N đăng nhập, **Then** N không còn thấy A.

---

### User Story 6 - Phản hồi, khiếu nại có hạn xử lý và leo thang (Priority: P2)

Người thân gửi phản hồi hoặc khiếu nại trên cổng (hoặc Hành chính ghi thay khi gia đình phản ánh trực tiếp). Hệ thống đặt người phụ trách theo loại, hạn xử lý theo mức ưu tiên, leo thang lên Quản lý viện khi quá hạn. Sau khi được phản hồi, người thân xác nhận đóng hoặc mở lại; không phản hồi trong CFG-M10-02 thì tự đóng.

**Why this priority**: Là kênh giữ lòng tin của gia đình và là bằng chứng xử lý khiếu nại; không chặn chăm sóc hằng ngày.

**Independent Test**: Gửi hai phản hồi mức Cao và Thường bằng đồng hồ giả lập; để một cái quá hạn; phản hồi rồi cho người thân mở lại, rồi để tự đóng.

**Acceptance Scenarios**:

1. **Given** M gửi khiếu nại "phòng P201 không được dọn hai ngày" loại Khiếu nại, **When** lưu, **Then** phản hồi ở Mới, mức ưu tiên mặc định Cao (FR-062), hạn xử lý = thời điểm gửi + 24 giờ (CFG-M10-01), người phụ trách là T (Trưởng tầng Tầng 2) theo nhóm nội dung "chăm sóc, sinh hoạt"; T nhận thông báo.
2. **Given** phản hồi ở Mới, **When** T nhận xử lý, **Then** chuyển Đang xử lý; **When** T ghi hướng xử lý và nội dung trả lời gửi người thân, **Then** chuyển Đã phản hồi, M nhận thông báo.
3. **Given** phản hồi Cao gửi 09:00 ngày 01/11 chưa chuyển Đã phản hồi, **When** tới 09:00 ngày 02/11, **Then** phản hồi được gắn dấu "quá hạn", Q nhận thông báo leo thang mức Trung bình (BR-M10-05); người phụ trách không đổi trừ khi Q giao lại.
4. **Given** phản hồi Đã phản hồi, **When** M chọn "Chưa hài lòng – mở lại" kèm lý do, **Then** phản hồi chuyển Mở lại, hạn xử lý mới tính từ thời điểm mở lại theo mức ưu tiên; **When** M chọn "Đồng ý đóng", **Then** chuyển Đóng với thời điểm đóng và cách đóng "người thân xác nhận".
5. **Given** phản hồi Đã phản hồi từ ngày 01/11, **When** tới ngày 08/11 (CFG-M10-02 \[7 ngày\]) M không thao tác, **Then** chuyển Đóng với cách đóng "tự đóng".
6. **Given** R đến quầy phản ánh về hóa đơn, **When** H ghi phản hồi thay R, chọn người gửi là R, **Then** phản hồi có nguồn "ghi tại quầy", người phụ trách là Hành chính theo nhóm "chi phí, hợp đồng"; R thấy phản hồi này trên cổng.
7. **Given** phản hồi Đóng, **When** người thân hoặc nhân viên cố sửa nội dung hay kết quả, **Then** hệ thống từ chối; nội dung đã gửi và phản hồi đã trả lời là ghi nhận không sửa.
8. **Given** N (không có quyền xem sức khỏe) gửi phản hồi nhóm "sức khỏe, thuốc" hỏi vì sao A đổi thuốc, **When** T soạn trả lời, **Then** T được cảnh báo "người gửi không có quyền xem sức khỏe"; nội dung trả lời gửi N chỉ ở loại chung (ví dụ mời liên hệ trực tiếp hoặc đề nghị người đại diện cấp quyền); N không thấy phần sức khỏe nào (FR-058).
9. **Given** phản hồi Thường của M quá hạn lúc 10:00 ngày 04/11 và Q đã nhận leo thang, **When** tới các ngày sau vẫn chưa Đã phản hồi, **Then** Q không nhận thêm leo thang, phản hồi vẫn nằm trong danh sách phản hồi quá hạn của Q; **When** phản hồi được trả lời, M mở lại và hạn mới lại bị quá, **Then** Q nhận một leo thang mới (FR-063); **When** nhân viên chăm sóc cố trả lời phản hồi, **Then** hệ thống từ chối (FR-067).

---

### User Story 7 - Bản tin tuần tự tổng hợp, điều dưỡng duyệt và gửi (Priority: P3)

Mỗi thứ 2 (CFG-M10-03), hệ thống sinh bản nháp bản tin của tuần trước cho từng người cao tuổi từ dữ liệu đã ghi. Điều dưỡng thêm nhận xét và duyệt trong 48 giờ; nếu trong tuần có sự cố mức Trung bình trở lên thì phải có phần giải thích. Bản tin đã gửi không sửa; người thân chỉ thấy phần thuộc quyền của mình.

**Why this priority**: Tăng niềm tin và giảm cuộc gọi hỏi thăm, nhưng phụ thuộc dữ liệu của nhiều feature và không ảnh hưởng an toàn.

**Independent Test**: Dựng dữ liệu một tuần cho A (ăn uống, nước, 3 hoạt động, cân nặng, một sự cố Trung bình, chi phí tạm tính); chạy Bộ lập lịch thứ 2; thử duyệt thiếu giải thích; duyệt và gửi; kiểm tra nội dung R, M, N thấy.

**Acceptance Scenarios**:

1. **Given** thứ 2 ngày 09/11 lúc sinh bản tin, **When** Bộ lập lịch chạy, **Then** mỗi người cao tuổi Đang lưu trú, Tạm vắng hoặc Điều trị tại bệnh viện có một bản nháp kỳ 02/11 → 08/11 gồm: tỷ lệ ăn trung bình, lượng nước trung bình, số hoạt động đã tham gia, cân nặng và xu hướng, các chỉ số chính, sự cố trong kỳ, chi phí tạm tính (14.5); chạy lại không tạo bản nháp trùng.
2. **Given** bản nháp của A có sự cố "ngã" mức Trung bình ngày 04/11, **When** D duyệt mà chưa nhập phần giải thích, **Then** hệ thống từ chối (BR-M10-09); **When** D nhập giải thích và nhận xét rồi duyệt, **Then** bản tin chuyển Đã duyệt và được gửi qua feature 009.
3. **Given** bản tin gửi tới R, M, N, **When** mỗi người mở, **Then** M thấy phần chung và sức khỏe (có quyền và có bản đồng ý), R thấy phần chung và chi phí, N chỉ thấy phần chung (feature 009 FR-013, Q-105); thời điểm gửi và thời điểm xem của từng người được ghi (BR-M10-06).
4. **Given** bản nháp sinh 00:00 ngày 09/11 chưa được duyệt, **When** tới 00:00 ngày 11/11 (48 giờ, CFG-M10-03), **Then** T được nhắc mức Trung bình; bản nháp vẫn chờ duyệt và T chưa duyệt thay được; **When** tới 00:00 ngày 13/11 (96 giờ, CFG-M10-12) vẫn chưa duyệt, **Then** T được báo "có thể duyệt thay" mức Trung bình; T nhập nhận xét, phần giải thích sự cố (nếu kỳ có sự cố Trung bình trở lên) và duyệt; bản tin được gửi, người duyệt ghi là T kèm dấu "Trưởng tầng duyệt thay" (Q-124).
5. **Given** bản nháp kỳ 02/11 → 08/11 vẫn chưa duyệt, **When** bản nháp kỳ kế tiếp được sinh ngày 16/11, **Then** bản cũ chuyển Không gửi với lý do "quá kỳ" và Q được báo (FR-073).
6. **Given** bản tin đã gửi, **When** D phát hiện cân nặng sai, **Then** D không sửa được bản đã gửi; D lập bản tin đính chính trỏ về bản gốc, duyệt và gửi; bản gốc vẫn xem được.
7. **Given** A qua đời ngày 05/11, **When** Bộ lập lịch chạy ngày 09/11, **Then** không có bản nháp cho A (feature 004 FR-073).
8. **Given** sự cố "té trong phòng tắm" của A ngày 03/11 được ghi mức Nhẹ, **When** ngày 10/11 (trước khi duyệt) bác sĩ nâng lên Trung bình ở feature 007, **Then** bản nháp kỳ 02/11 → 08/11 bắt buộc có phần giải thích trước khi duyệt; **Given** một sự cố khác trong kỳ đã chuyển Đã hủy, **Then** sự cố đó không tính (Q-129).
9. **Given** B ở bệnh viện suốt 02/11 → 08/11, **When** Bộ lập lịch chạy ngày 09/11, **Then** không có bản nháp cho B, kỳ đó ghi "bỏ qua – vắng cả kỳ" (FR-051).

---

### User Story 8 - Người thân ở lại chăm sóc (Priority: P3)

Khi người thân được phép ở lại với người cao tuổi (ví dụ sau phẫu thuật, giai đoạn cuối), Hành chính đăng ký người ở lại, thời gian, vị trí, lý do, có đăng ký ăn hay không; Trưởng tầng của tầng xác nhận trước khi người thân bắt đầu ở lại. Mỗi đêm ở lại tự tạo chi phí theo đơn giá và người ở lại được tính vào số suất ăn nếu có đăng ký ăn.

**Why this priority**: Khảo sát xác nhận có tình huống này (mục 21), nhưng tần suất thấp và không ảnh hưởng an toàn.

**Independent Test**: Đăng ký M ở lại từ 18:00 ngày 01/11 tới 08:00 ngày 03/11 có đăng ký ăn; thử bắt đầu khi Trưởng tầng chưa xác nhận; kiểm tra chi phí 2 đêm theo đơn giá và số suất ăn được feature 011 tính; thử ở lại khi phòng đang khoanh vùng.

**Acceptance Scenarios**:

1. **Given** M có quan hệ Hiệu lực với A, **When** H đăng ký "M ở lại tại P201 từ 18:00 ngày 01/11 tới 08:00 ngày 03/11, lý do chăm sóc sau mổ, có đăng ký ăn", **Then** lượt ở lại ở trạng thái Chờ xác nhận và T nhận thông báo; **When** H cố ghi "Bắt đầu ở lại", **Then** hệ thống từ chối "chưa được Trưởng tầng xác nhận"; **When** T xác nhận, **Then** lượt chuyển Đã xác nhận; **When** T từ chối kèm lý do (ví dụ người ở cùng phòng không đồng ý), **Then** lượt chuyển Từ chối và H được báo.
2. **Given** lượt ở lại Đã xác nhận, **When** M đến và H ghi "Bắt đầu ở lại" lúc 18:00 ngày 01/11, **Then** lượt chuyển Đang ở lại; feature 011 tính M vào số suất các bữa trong khoảng ở lại (BR-M08-01); feature 010 nhận yêu cầu tạo một khoản chi phí cho mỗi đêm ở lại theo phiên bản đơn giá dịch vụ "Người thân ở lại" hiệu lực tại ngày bắt đầu của đêm đó (BR-M10-04, Q-25).
3. **Given** M Đang ở lại từ 18:00 ngày 01/11, **When** H ghi "Kết thúc ở lại" lúc 08:00 ngày 03/11, **Then** lượt chuyển Đã kết thúc; số đêm tính phí là 2 (đêm 01→02/11 và 02→03/11) theo FR-043; không còn suất ăn cho M sau thời điểm kết thúc; **Given** người thân K ở lại từ 08:00 tới 20:00 cùng ngày, **When** kết thúc, **Then** số đêm tính phí là 0, K vẫn được tính suất ăn các bữa trong khoảng ở lại nếu có đăng ký ăn.
4. **Given** Tầng 2 đang khoanh vùng, **When** H đăng ký hoặc bắt đầu lượt ở lại tại P201, **Then** hệ thống từ chối "khu đang khoanh vùng".
5. **Given** M Đang ở lại, **When** A chuyển Điều trị tại bệnh viện, **Then** H và T được nhắc kết thúc lượt ở lại; lượt không tự kết thúc.
6. **Given** A ở phòng hai giường cùng người cao tuổi C, M đang có lượt Đã xác nhận cho A, **When** H đăng ký thêm lượt ở lại chồng thời gian cho N, **Then** hệ thống từ chối vì vượt CFG-M10-10 (mặc định \[1\]); **When** T xác nhận lượt của M, **Then** T phải ghi đã hỏi ý kiến C (hoặc người đại diện của C) và kết quả; thiếu thì không xác nhận được (Q-128).

---

### Edge Cases

- **Người thân của nhiều người cao tuổi**: một hồ sơ người thân, nhiều quan hệ; vai trò, quyền, danh sách đón tính riêng cho từng quan hệ (FR-002).
- **Người đón không phải người nhà** (tài xế, người giúp việc): phải có hồ sơ người thân với quan hệ "Khác" và chỉ có quyền "được phép đón" qua BR-M10-07; không có quyền xem nào khác trừ khi được cấp.
- **Người đại diện là người duy nhất trong danh sách đón và bị bỏ khỏi danh sách**: lệnh bỏ vẫn có hiệu lực ngay; danh sách rỗng được phép; mọi lượt đón sau đó đi qua ngoại lệ (FR-025).
- **Người đại diện vừa là người lập vừa là người xác nhận**: yêu cầu thay đổi quyền do người đại diện gửi qua cổng được coi là đã có xác nhận (User Story 3 kịch bản 7), kể cả khi yêu cầu tác động tới chính người đại diện đó (ví dụ bật quyền xem chi phí cho chính mình); khi A có hai người đại diện, hệ thống MUST NOT đòi người thứ hai xác nhận (FR-016). Ngoại lệ: yêu cầu đặt thêm hoặc thôi người đại diện chỉ được xác nhận bởi người đại diện hiện có khác người bị tác động, hoặc Quản lý viện duyệt (FR-005, Q-127).
- **Không có người đại diện có tài khoản cổng**: xác nhận bằng bản ký do Hành chính ghi, hoặc Quản lý viện duyệt.
- **Yêu cầu thay đổi trùng**: đã có yêu cầu Chờ xác nhận cho cùng (người thân, quyền) thì yêu cầu mới bị từ chối, trừ yêu cầu bỏ người đón (FR-018).
- **Bản đồng ý bị rút khi yêu cầu bật quyền xem sức khỏe đang Chờ xác nhận**: khi xác nhận hoặc duyệt, hệ thống kiểm tra lại bản đồng ý; không còn thì yêu cầu chuyển Từ chối với lý do "bản đồng ý không còn bao gồm người thân" (FR-013).
- **Lượt thăm cho người bán trú**: chỉ đăng ký được vào ngày E có lịch đến và khung giờ nằm trong giờ có mặt dự kiến; E báo vắng thì lượt thăm chuyển Hủy như kịch bản 6 của User Story 4.
- **Tham số khung giờ hoặc sức chứa thay đổi khi đã có lượt thăm**: lượt đã duyệt giữ nguyên, kể cả khi vượt sức chứa mới; chỉ đăng ký mới áp giá trị mới (FR-032).
- **Người thăm là người trong danh sách tiếp xúc** (feature 007): không chặn đăng ký ở giai đoạn đầu; chỉ khu khoanh vùng bị chặn (BR-M05-11).
- **Người cao tuổi qua đời khi đang có lượt thăm Đã duyệt, lượt ở lại Đang ở lại, bản tin Chờ duyệt, phản hồi đang mở**: lượt thăm tương lai chuyển Hủy; lượt ở lại được nhắc kết thúc; bản tin chuyển Không gửi; phản hồi vẫn xử lý tiếp (FR-075). Bàn giao thi hài cho gia đình không đi qua quy trình đón (FR-021) mà theo danh sách việc sau qua đời của feature 004.
- **Kết thúc lưu trú**: quan hệ người thân giữ Hiệu lực tới khi tài khoản bị khóa theo CFG-M01-04 (feature 002) để người thân xem chi phí kỳ cuối; không nhận đăng ký thăm, lượt ở lại hay bản tin mới.
- **Phản hồi về nhân viên cụ thể là người phụ trách mặc định** (ví dụ khiếu nại về chính Trưởng tầng): người tiếp nhận MAY giao lại cho người khác có quyền; Quản lý viện luôn xem được mọi phản hồi (FR-063).
- **Phản hồi được gửi khi quyền hoặc quan hệ của người gửi sau đó kết thúc**: phản hồi vẫn được xử lý; người gửi không còn xem được trên cổng.
- **Nhân viên ghi đón ngoại tuyến**: không cho phép; kiểm tra danh sách đón và ngoại lệ bắt buộc trực tuyến vì danh sách có thể vừa bị bỏ người (Q-01 mặc định, FR-028).

## Requirements *(mandatory)*

### Phân loại dữ liệu (mục 1.5)

| Đối tượng | Nhóm | Thao tác được phép |
| --- | --- | --- |
| Hồ sơ người thân (NGUOI_THAN) | 1 | Tạo, sửa thông tin liên hệ (có nhật ký trước/sau); không xóa khi đã có quan hệ |
| Quan hệ người thân (QUAN_HE_NGUOI_THAN) gồm vai trò | 2 | Lập quan hệ, Đổi người liên hệ chính, Đặt/Thôi người đại diện, Kết thúc quan hệ |
| Quyền theo người thân, danh sách được phép đón | 2 | Chỉ thay đổi qua yêu cầu BR-M10-07 |
| Yêu cầu thay đổi quyền, yêu cầu ngoại lệ đón | 2 | Lập, Xác nhận, Duyệt, Từ chối, Hủy |
| Bản ghi đón, lần chặn đón | 3 | Chỉ ghi thêm; "Đã dùng" (lệnh nguồn đã dùng) và "Không dùng" (FR-017, FR-027) là sự kiện ghi thêm gắn vào bản ghi, trạng thái suy ra từ sự kiện gần nhất; đính chính theo feature 000 |
| Lượt thăm (LUOT_THAM) | 2 | Đăng ký, Hủy, Vào, Ra; do Hệ thống: Không đến, Hủy tự động (FR-034, FR-036, FR-037); giờ vào/ra đã ghi chỉ đính chính |
| Lượt ở lại | 2 | Đăng ký, Xác nhận, Từ chối, Bắt đầu, Gia hạn, Kết thúc, Hủy |
| Phản hồi (PHAN_HOI) | 2 cho trạng thái; 3 cho nội dung, trả lời | Lệnh theo bảng FR-066 |
| Bản tin (BAN_TIN) | 2 khi nháp; 3 khi đã gửi | Nhận xét, duyệt khi nháp; đã gửi chỉ đính chính bằng bản mới |
| Nhật ký lượt xem sức khỏe | 3 | Chỉ ghi thêm |
| Phiếu đăng ký người thân | 3 | Chỉ ghi thêm; sai sót xử lý bằng phiếu mới hoặc yêu cầu BR-M10-07 |
| Dấu "được tự về" | 2 | Chỉ thay đổi qua yêu cầu BR-M10-07 (FR-013) |

### Functional Requirements

**Quy ước trong spec** (áp cho mọi FR, kịch bản và bảng):

- "Trưởng tầng" khi gắn với một người cao tuổi hoặc một tầng (kể cả các cách viết "Trưởng tầng của tầng", "Trưởng tầng được giao") nghĩa là Trưởng tầng đang được giao tầng của người cao tuổi đó (feature 008, Q-84). Khi tầng không có Trưởng tầng được giao, việc xác nhận, duyệt, nhận xử lý dành cho Trưởng tầng MUST chuyển cho Quản lý viện, và thông báo gửi Trưởng tầng MUST gửi Quản lý viện.
- "Quyền đang bật" là giá trị hiện hành của quyền trên quan hệ; "quyền có tác dụng" là quyền đang bật và thỏa điều kiện kèm theo (bản đồng ý với xem sức khỏe, FR-011; không có cờ nguy cơ đi lạc với dấu "được tự về", FR-021); "yêu cầu Hiệu lực" là trạng thái của yêu cầu thay đổi (FR-020); "quan hệ Hiệu lực" tương đương "quan hệ còn hiệu lực" của feature 002.
- "Điều dưỡng phụ trách" của BR-M10-08 được hiểu, với bản tin tuần, là Điều dưỡng có phạm vi dữ liệu với người cao tuổi tại thời điểm thao tác (FR-053), vì Điều dưỡng phụ trách ở 2.4 được gán theo từng ca.
- Mốc 00:00 khi đếm đêm ở lại (FR-043) và kỳ bản tin 7 ngày liền trước ngày sinh (FR-051) là định nghĩa nghiệp vụ, không phải tham số; lịch sinh và hạn duyệt bản tin là tham số CFG-M10-03.


#### A. Hồ sơ người thân và quan hệ

- **FR-001**: Hành chính MUST lập được hồ sơ người thân gồm: họ tên (bắt buộc), số điện thoại (bắt buộc), loại và số giấy tờ tùy thân, địa chỉ. Người có quyền "được phép đón" MUST có loại và số giấy tờ tùy thân (FR-007). Số điện thoại MAY trùng giữa các hồ sơ (feature 002 Edge Case). *(Nguồn: 14.1, 3.2 NGUOI_THAN, UC-55)*
- **FR-002**: Mỗi hồ sơ người thân MAY có quan hệ với nhiều người cao tuổi; mỗi quan hệ gồm: quan hệ (vợ/chồng, con, cháu, anh chị em, khác), là người đại diện, là người liên hệ chính, sáu quyền (FR-007), trạng thái Hiệu lực / Đã kết thúc, thứ tự liên hệ (FR-012). Vai trò và quyền MUST được tính riêng cho từng quan hệ. *(Nguồn: 14.1, 3.2 QUAN_HE_NGUOI_THAN)*
- **FR-003**: Mỗi người cao tuổi ở trạng thái Đang lưu trú, Tạm vắng hoặc Điều trị tại bệnh viện MUST có đúng một người liên hệ chính và ít nhất một người đại diện trong các quan hệ Hiệu lực. Mọi lệnh làm vi phạm điều này MUST bị từ chối. Với người cao tuổi Đang tiếp nhận, hệ thống MUST cho lưu khi chưa đủ; feature 004 dùng điều kiện "có người đại diện" khi lập hợp đồng (feature 004 FR-021), và hệ thống MUST cung cấp điều kiện "đủ DBR-02" để feature 001 chặn lệnh Hoàn tất tiếp nhận khi chưa đạt. *(Nguồn: DBR-02, 14.1)*
- **FR-004**: Lệnh "Đổi người liên hệ chính" MUST chuyển vai trò sang người thân mới và bỏ vai trò của người cũ trong cùng một lần thực hiện; bắt buộc lý do. *(Nguồn: DBR-02, 1.5 nhóm 2)*
- **FR-005**: Lệnh "Đặt người đại diện" và "Thôi người đại diện" MUST bắt buộc lý do và bằng chứng (văn bản ủy quyền, giấy tờ quan hệ hoặc hợp đồng đã ký). Khi người cao tuổi chưa có người đại diện, Hành chính MUST đặt được người đại diện đầu tiên trực tiếp. Khi đã có người đại diện, mọi lệnh đặt thêm hoặc thôi người đại diện MUST đi qua yêu cầu theo vòng đời FR-020, có hiệu lực khi một người đại diện hiện có khác người bị tác động xác nhận hoặc Quản lý viện duyệt; người được đề nghị làm người đại diện MUST NOT xác nhận yêu cầu nào trước khi yêu cầu đặt mình có hiệu lực. Khi yêu cầu đặt một người làm người đại diện còn Chờ xác nhận, người đó MUST NOT được coi là người đại diện hợp lệ ở bất kỳ feature nào (ký hợp đồng, phụ lục ở feature 004 FR-021; người đồng ý ở feature 001 FR-011). "Thôi người đại diện" MUST bị từ chối nếu làm người cao tuổi không còn người đại diện (FR-003) hoặc nếu người đó là người đồng ý trên bản đồng ý Hiệu lực (feature 001 FR-011) mà chưa có bản đồng ý thay thế. *(Nguồn: 2.4 "Người đại diện", DBR-02, BR-M10-07; Clarification 2026-09-26 lượt 3, đề xuất Q-127)*
- **FR-006**: Lệnh "Kết thúc quan hệ" MUST bắt buộc lý do, MUST bị từ chối nếu vi phạm FR-003, và khi thành công MUST: bỏ người đó khỏi danh sách được phép đón, hủy các lượt thăm tương lai do người đó đăng ký, hủy các yêu cầu Chờ xác nhận của người đó. Quan hệ đã kết thúc MUST NOT kích hoạt lại; muốn lập lại thì lập quan hệ mới. *(Nguồn: 1.5 nhóm 2, feature 002 FR-045)*

#### B. Quyền theo người thân

- **FR-007**: Mỗi quan hệ MUST có sáu quyền bật/tắt: xem sức khỏe; xem chi phí; nhận thông báo khẩn; đăng ký thăm; được phép đón; được yêu cầu thay đổi dịch vụ. "Được phép đón" tạo nên danh sách người được phép đón của người cao tuổi. Quyền "được phép đón" MUST chỉ bật được khi hồ sơ người thân có loại và số giấy tờ tùy thân, để xác minh ở FR-021. *(Nguồn: 14.1, 14.3)*
- **FR-008**: Khi lập quan hệ, mọi quyền MUST mặc định tắt, trừ các quyền do FR-009, FR-010 quy định. Hành chính MUST ghi nhận được "phiếu đăng ký người thân" do người đại diện ký (bản scan bắt buộc), liệt kê quyền của từng người thân kể cả của chính người đại diện; mỗi quyền trên phiếu MUST tạo một yêu cầu Hiệu lực ngay với cách xác nhận "bản ký" (FR-015 (b)) và vẫn qua các kiểm tra FR-011, FR-007. Quyền không có trên phiếu MUST chỉ bật qua yêu cầu BR-M10-07; khi chưa có người đại diện, yêu cầu đó chỉ Hiệu lực khi Quản lý viện duyệt. *(Nguồn: 14.1, BR-M10-07; Clarification 2026-09-26 lượt 3, đề xuất Q-126)*
- **FR-009**: Quyền "nhận thông báo khẩn" của người liên hệ chính MUST luôn bật và MUST NOT tắt được trong thời gian người đó là người liên hệ chính; khi thôi vai trò, quyền giữ giá trị bật cho tới khi có yêu cầu tắt. *(Nguồn: 2.4 "Người liên hệ chính", feature 009 FR-014, Q-96)*
- **FR-010**: Quyền "được yêu cầu thay đổi dịch vụ" MUST chỉ bật được cho người đại diện, MUST tự bật khi người thân được đặt làm người đại diện và tự tắt khi thôi vai trò. *(Nguồn: BR-M10-01, 19.3)*
- **FR-011**: Quyền "xem sức khỏe" MUST chỉ bật được khi bản đồng ý Hiệu lực của người cao tuổi bao gồm người thân đó (feature 001). Khi bản đồng ý bị rút lại hoặc thay bằng bản không gồm người đó, quyền MUST giữ nguyên giá trị nhưng MUST không có tác dụng (feature 002 FR-046) và MUST được hiển thị "không có tác dụng" cho Hành chính và người thân. *(Nguồn: 14.1, BR-M01-08, DBR-03)*
- **FR-012**: Hệ thống MUST cung cấp cho feature 009 thứ tự liên hệ giữa các người thân cùng nhóm (người đại diện; người có quyền nhận thông báo khẩn) để lập chuỗi gọi: mặc định theo thứ tự Hành chính sắp khi lập quan hệ; Hành chính MAY đổi thứ tự kèm lý do, không cần xác nhận BR-M10-07. *(Nguồn: feature 009 FR-029a, Điểm báo lại 17)*

#### C. Yêu cầu thay đổi quyền và danh sách được phép đón (BR-M10-07)

- **FR-013**: Mọi thay đổi quyền ở FR-007 (trừ các trường hợp tự động ở FR-009, FR-010, FR-006) MUST đi qua yêu cầu thay đổi gồm: người cao tuổi, người thân bị tác động, quyền, giá trị mới (bật/tắt), lý do, người lập, nguồn (quầy / cổng), thông tin người mới khi thêm người đón (họ tên, số giấy tờ, quan hệ). Lệnh đặt thêm hoặc thôi người đại diện khi đã có người đại diện (FR-005) MUST cũng dùng yêu cầu này. Yêu cầu bật "được phép đón" MUST có số giấy tờ tùy thân của người được thêm. Yêu cầu bật "xem sức khỏe" MUST kiểm tra FR-011 cả lúc lập và lúc xác nhận/duyệt. Dấu "được tự về" của người cao tuổi bán trú (FR-021) MUST cũng được bật/tắt bằng yêu cầu này (người thân bị tác động để trống); yêu cầu bật MUST bị từ chối nếu người cao tuổi có cờ nguy cơ đi lạc, kiểm tra cả lúc lập và lúc xác nhận/duyệt; yêu cầu tắt MUST có hiệu lực ngay như yêu cầu tắt "được phép đón" (FR-017). *(Nguồn: 14.1, BR-M10-07; Clarification 2026-09-26 lượt 2, đề xuất Q-125)*
- **FR-014**: Người lập được yêu cầu: Hành chính (theo đề nghị của gia đình tại quầy), người đại diện (qua cổng), người thân bất kỳ chỉ với yêu cầu bỏ chính mình khỏi danh sách đón hoặc tắt quyền của chính mình. *(Nguồn: 4.4 dòng "Người thân và quyền": HC T, NT T²)*
- **FR-015**: Yêu cầu MUST chuyển Hiệu lực khi có một trong: (a) người đại diện xác nhận qua cổng; (b) Hành chính ghi nhận bản ký của người đại diện kèm bản scan; (c) Quản lý viện duyệt kèm lý do. Yêu cầu do người đại diện lập qua cổng MUST được coi là đã có xác nhận (a). Từ chối MUST do người đại diện hoặc Quản lý viện thực hiện, bắt buộc lý do. *(Nguồn: 14.1, BR-M10-07, 19.3, 4.4 QL D)*
- **FR-016**: Xác nhận của một người đại diện MUST đủ; khi người cao tuổi có nhiều người đại diện, hệ thống MUST NOT đòi thêm xác nhận. *(Suy ra từ 14.1; xem Điểm báo lại 6)*
- **FR-017**: Trong lúc yêu cầu Chờ xác nhận, quyền cũ MUST tiếp tục áp dụng; riêng yêu cầu tắt "được phép đón" MUST có hiệu lực ngay khi lập. Nếu yêu cầu đó bị Từ chối hoặc Hủy, người thân MUST được khôi phục vào danh sách từ thời điểm có kết quả, không hồi tố. Khi "được phép đón" của một người bị tắt, mọi bản ghi đón chưa dùng (FR-027) có người đó là người đón MUST chuyển Không dùng ngay. *(Nguồn: BR-M10-07)*
- **FR-018**: Mỗi (quan hệ, quyền) MUST có tối đa một yêu cầu Chờ xác nhận; yêu cầu mới trùng MUST bị từ chối, trừ yêu cầu tắt "được phép đón", khi đó yêu cầu bật đang chờ cho cùng người MUST tự chuyển Hủy với lý do "được thay bằng yêu cầu bỏ". Khi các yêu cầu cho cùng (quan hệ, quyền) nối tiếp nhau có hiệu lực, giá trị hiện hành MUST là kết quả của yêu cầu có thời điểm Hiệu lực muộn nhất; yêu cầu tắt "được phép đón" luôn áp ngay theo FR-017, kể cả khi người đại diện khác vừa xác nhận yêu cầu bật trước đó. *(Đề xuất, xem Điểm báo lại 6)*
- **FR-019**: Yêu cầu Chờ xác nhận quá mốc nhắc của CFG-M10-11 (đề xuất, mặc định \[48 giờ\]) MUST được nhắc tới người đại diện và Quản lý viện; quá mốc báo của CFG-M10-11 (mặc định \[96 giờ\]) MUST báo Quản lý viện; yêu cầu MUST NOT tự Từ chối. Tham số này tách riêng khỏi CFG-M15-05/06 của feature 000 (Q-130). *(Nguồn: feature 000 FR-031a áp tương tự; Clarification 2026-09-26 lượt 4, đề xuất Q-130)*
- **FR-020**: Bảng trạng thái yêu cầu thay đổi quyền, áp cho mọi loại: thay đổi quyền của người thân (gồm danh sách được phép đón), dấu "được tự về" (FR-013), đặt thêm hoặc thôi người đại diện khi đã có người đại diện (FR-005); quyền trên phiếu đăng ký người thân được tạo thẳng ở Hiệu lực (FR-008). Đây là vòng đời riêng theo BR-M10-07, không dùng vòng đời yêu cầu phê duyệt chung của feature 000 (không có Nháp, không có "chờ hiệu lực"); nhật ký, lý do bắt buộc và bảo vệ chống quyết định đồng thời vẫn theo feature 000. *(Nguồn: BR-M10-07; Clarification 2026-09-26, đề xuất Q-118)*

| Từ | Lệnh | Đến | Ai | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Lập yêu cầu | Chờ xác nhận | Hành chính, người thân theo FR-014 | FR-013, FR-018 | Yêu cầu tắt "được phép đón": bỏ khỏi danh sách ngay (FR-017); báo người đại diện và Quản lý viện |
| (chưa có) | Lập yêu cầu (người đại diện qua cổng, hoặc kèm bản ký) | Hiệu lực | Người đại diện; Hành chính | FR-013, FR-015 (a)(b) | Áp quyền mới; báo Hành chính (Nhẹ) |
| Chờ xác nhận | Xác nhận | Hiệu lực | Người đại diện; với yêu cầu đặt/thôi người đại diện: người đại diện hiện có khác người bị tác động (FR-005) | FR-013 kiểm tra lại | Áp quyền mới từ thời điểm xác nhận |
| Chờ xác nhận | Duyệt | Hiệu lực | Quản lý viện | Có lý do; FR-013 kiểm tra lại | Áp quyền mới |
| Chờ xác nhận | Từ chối | Từ chối | Người đại diện, Quản lý viện | Có lý do | Không áp; khôi phục người đón nếu là yêu cầu bỏ (FR-017) |
| Chờ xác nhận | Hủy | Hủy | Người lập | Có lý do | Như Từ chối |

#### D. Quy trình đón (14.3, BR-M10-03)

- **FR-021**: Quy trình đón MUST được thực hiện trước khi người cao tuổi rời viện cùng người thân trong các trường hợp: Tạm vắng (mọi loại vắng trừ "Bệnh viện"), Đi chơi với gia đình, Bán trú về, Kết thúc lưu trú có người thân nhận. Quy trình gồm: (1) chọn người đón; (2) kiểm tra người đón thuộc danh sách được phép đón tại thời điểm đón hoặc có ngoại lệ Hiệu lực; (3) xác minh danh tính bằng cách đối chiếu giấy tờ tùy thân xuất trình với số đã đăng ký, ghi kết quả; (4) ghi thời điểm; (5) ghi nhân viên bàn giao. Ngoại lệ duy nhất: người cao tuổi bán trú có dấu "được tự về" có tác dụng MAY điểm danh về mà không có người đón; dấu chỉ có tác dụng khi đang bật và người cao tuổi không có cờ nguy cơ đi lạc (5.3) tại thời điểm về — cờ được gắn sau khi bật thì dấu mất tác dụng ngay, không cần yêu cầu tắt. "Viện đưa về" chưa được hỗ trợ ở giai đoạn đầu. Quy trình đón MUST NOT áp cho người cao tuổi Qua đời; bàn giao thi hài theo danh sách việc sau qua đời của feature 004 (6.8). *(Nguồn: 14.3, UC-58; Clarification 2026-09-26 lượt 2, đề xuất Q-125)*
- **FR-022**: Người thực hiện quy trình đón (FR-021) và lập yêu cầu ngoại lệ đón (FR-024) MUST là người có quyền thực hiện lệnh nguồn mà lượt đón phục vụ, trong phạm vi dữ liệu của họ (BR-M15-01): Hành chính, Trưởng tầng (Cho tạm vắng, gồm Đi chơi với gia đình; feature 004 FR-049); Hành chính, Nhân viên chăm sóc (Điểm danh về của bán trú, feature 005); Hành chính (Kết thúc lưu trú, feature 004). Vai trò khác MUST bị từ chối. *(Nguồn: 14.3, feature 004, 005; Clarification 2026-09-26 lượt 2, đề xuất Q-121)*
- **FR-023**: Khi bước (2) hoặc (3) không đạt, lượt đón MUST bị chặn và hệ thống MUST ghi "lần chặn đón" gồm người cao tuổi, người đón được khai, lý do, người thực hiện, thời điểm; Trưởng tầng của tầng người cao tuổi MUST nhận thông báo Nhẹ. *(Nguồn: BR-M10-03, 1.3)*
- **FR-024**: Khi người đón không thuộc danh sách, người thực hiện MUST lập được "yêu cầu ngoại lệ đón" gồm họ tên, số giấy tờ, quan hệ, lý do. Ngoại lệ MUST chuyển Hiệu lực khi người đại diện xác nhận qua cổng hoặc Quản lý viện duyệt kèm lý do; MUST có hiệu lực cho đúng một lượt đón của người cao tuổi đó bởi người đó, trong CFG-M10-08 (đề xuất, mặc định \[4 giờ\]) kể từ lúc Hiệu lực; hết hạn chưa dùng thì chuyển Hết hạn. Ngoại lệ MUST NOT thêm người vào danh sách được phép đón. Người đại diện hoặc Quản lý viện MUST hủy được ngoại lệ Hiệu lực chưa dùng, bắt buộc lý do. Thông báo cho Quản lý viện MUST gửi ngay; thông báo cho người đại diện chịu giờ yên tĩnh của feature 009 (mức Trung bình), nên trong giờ yên tĩnh ngoại lệ trên thực tế đi qua Quản lý viện duyệt. Trạng thái: Chờ xác nhận → Hiệu lực → Đã dùng / Hết hạn / Hủy; Chờ xác nhận → Từ chối / Hủy. *(Nguồn: BR-M10-03, 19.3 "Quản lý viện là người duyệt ... người đón ngoài danh sách")*
- **FR-025**: Hệ thống MUST cho phép danh sách được phép đón rỗng; khi đó mọi lượt đón đi qua FR-024. *(Suy ra từ BR-M10-07)*
- **FR-026**: Mỗi lượt đón thành công MUST tạo một bản ghi đón (nhóm 3) gồm: người cao tuổi, loại đón, người đón (để trống với loại "Tự về"), căn cứ (trong danh sách / ngoại lệ do người đại diện xác nhận / ngoại lệ do Quản lý viện duyệt / được tự về), kết quả xác minh danh tính, thời điểm, nhân viên bàn giao (nhân viên tiễn với loại "Tự về"), người ghi. Bản ghi đón MUST được feature 004 và 005 dùng làm căn cứ của lệnh nguồn; lệnh nguồn không có bản ghi đón hợp lệ (cho các trường hợp ở FR-021) MUST bị chặn. *(Nguồn: 14.3, feature 004 FR-048, FR-064, feature 005)*
- **FR-027**: Bản ghi đón chưa được lệnh nguồn dùng trong CFG-M10-09 (đề xuất, mặc định \[2 giờ\]) kể từ lúc ghi MUST chuyển "Không dùng" và không còn là căn cứ; người cao tuổi chưa rời viện. *(Đề xuất)*
- **FR-028**: Quy trình đón và yêu cầu ngoại lệ MUST thực hiện trực tuyến; không ghi tạm để đồng bộ sau. *(Nguồn: Q-01 mặc định cho thao tác an toàn)*

#### E. Thăm nom (14.2, BR-M10-02)

- **FR-029**: Người thân có quyền "đăng ký thăm" với người cao tuổi MUST đăng ký được qua cổng; Hành chính MUST đăng ký được thay tại quầy cho người thân có quan hệ Hiệu lực. Lượt thăm gồm: người cao tuổi, người đăng ký, ngày, khung giờ, danh sách người đi cùng (họ tên, quan hệ), số người, nguồn. Số người MUST NOT vượt CFG-M10-06 (đề xuất, mặc định \[3 người\]). Số điện thoại của người đi cùng MAY ghi, không bắt buộc; khi người đi cùng không có số liên hệ, người đăng ký lượt thăm là đầu mối liên hệ nếu họ vào danh sách tiếp xúc của feature 007 (feature 009 FR-021). *(Nguồn: 14.2, UC-57, 3.2 LUOT_THAM)*
- **FR-030**: Khi đăng ký, hệ thống MUST kiểm tra và từ chối kèm lý do cụ thể nếu vi phạm một trong: (a) khung giờ thuộc khung thăm của CFG-M10-04 (đề xuất, mặc định \[09:00–11:00 và 15:00–17:00 hằng ngày\]); (b) tổng số người (người đăng ký cộng người đi cùng) của các lượt Đã duyệt, Đã vào trong khung cộng số người mới không vượt sức chứa khung ở CFG-M10-04 (đề xuất, mặc định \[20 người mỗi khung\]), tính chung cho khu tiếp khách của toàn viện; khi nhiều đăng ký tranh cùng chỗ, đăng ký được hệ thống ghi nhận trước giữ chỗ; (c) không đang chịu khoanh vùng (feature 007 FR-065, BR-M05-11): phòng của người cao tuổi nội trú, khu nghỉ bán trú với người bán trú, và khu tiếp khách — khu tiếp khách nằm trong vùng thì mọi đăng ký bị chặn; (d) người cao tuổi đang Đang lưu trú, không ở Tạm vắng, Điều trị tại bệnh viện, không trong lượt vắng dự kiến trùng ngày thăm, và với bán trú có lịch đến ngày đó và khung thăm nằm trọn trong giờ có mặt dự kiến; (e) thời điểm đăng ký trong khoảng CFG-M10-05 (đề xuất, mặc định trước ít nhất \[2 giờ\], tối đa \[14 ngày\]); không áp với đăng ký tại quầy cho khung hiện tại. *(Nguồn: BR-M10-02, BF-03 bước 2)*
- **FR-031**: Lượt thăm đạt FR-030 MUST được tạo ở trạng thái Đã duyệt ngay và giữ chỗ trong khung; đăng ký không đạt MUST bị từ chối ngay kèm lý do cụ thể và MUST NOT tạo lượt thăm. Không có bước Hành chính duyệt tay. *(Nguồn: 14.2, BF-03 bước 2; Clarification 2026-09-26, đề xuất Q-119)*
- **FR-032**: Thay đổi CFG-M10-04 MUST chỉ áp cho đăng ký mới; lượt đã có giữ nguyên. *(Nguồn: NFR-12)*
- **FR-033**: Khi ghi "Vào", hệ thống MUST kiểm tra lại FR-030 (c) và (d); không đạt thì chặn. Hành chính MUST ghi danh sách người thực tế vào (không vượt số đã đăng ký; người không có trong danh sách đi cùng MUST được thêm tên trước khi vào). *(Nguồn: 14.2 "lịch sử vào/ra", BR-M05-10 truy vết)*
- **FR-034**: Lượt Đã duyệt chưa ghi Vào khi hết khung MUST được Bộ lập lịch chuyển Không đến. Lượt Đã vào chưa ghi Ra sau giờ kết thúc khung + CFG-M10-07 (đề xuất, mặc định \[30 phút\]) MUST nhắc Hành chính; Hành chính MAY ghi giờ ra thực tế sớm hơn thời điểm ghi, không muộn hơn. *(Nguồn: 14.2)*
- **FR-035**: Người đăng ký MUST hủy được lượt Đã duyệt trước giờ bắt đầu khung; Hành chính MUST hủy được kèm lý do. Hủy MUST trả chỗ cho khung. Đổi khung giờ MUST thực hiện bằng hủy lượt cũ và đăng ký lượt mới; không có lệnh đổi khung. Tắt quyền "đăng ký thăm" của một người MUST NOT hủy các lượt Đã duyệt đã có của người đó; quyền chỉ áp cho đăng ký mới. *(Nguồn: 14.2)*
- **FR-036**: Khi người cao tuổi chuyển Tạm vắng, Điều trị tại bệnh viện, trạng thái cuối, hoặc bán trú báo vắng, mọi lượt thăm Đã duyệt trùng thời gian vắng MUST tự chuyển Hủy với lý do tương ứng và người đăng ký MUST nhận thông báo. *(Suy ra từ BR-M10-02 (d))*
- **FR-037**: Khi một vùng bắt đầu khoanh vùng (feature 007 FR-065), mọi lượt thăm Đã duyệt của người cao tuổi có giường trong vùng với khung chưa kết thúc MUST tự chuyển Hủy với lý do "khu đang khoanh vùng", trả chỗ, và người đăng ký MUST nhận thông báo mức Trung bình. Gỡ khoanh vùng MUST NOT khôi phục lượt đã hủy. Danh sách các lượt bị hủy MUST được liệt kê cho Hành chính và Trưởng tầng. Lượt đang Đã vào lúc khoanh vùng không bị hủy; Hành chính được báo để mời khách ra và ghi giờ ra. *(Nguồn: BR-M05-11, feature 007 Edge Cases "feature 012 quyết định"; Clarification 2026-09-26, đề xuất Q-120)*
- **FR-038**: Hệ thống MUST cung cấp cho feature 007 các lượt thăm Đã vào hoặc Đã ra để lập danh sách tiếp xúc, gồm hai loại người: người đăng ký (người thân có hồ sơ) và người đi cùng thực tế vào (chỉ có họ tên, quan hệ, số điện thoại nếu có — không có hồ sơ người thân), kèm giờ vào, giờ ra. *(Nguồn: feature 007 FR-060 (d), FR-029)*
- **FR-039**: Bảng trạng thái lượt thăm:

| Từ | Lệnh | Đến | Ai | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Đăng ký | Đã duyệt | Người thân có quyền đăng ký thăm; Hành chính | FR-029, FR-030 (không đạt thì từ chối, không tạo lượt) | Giữ chỗ trong khung (FR-031) |
| Đã duyệt | Hủy | Hủy | Người đăng ký, Hành chính, Hệ thống (FR-036, FR-037) | Trước giờ bắt đầu khung; Hành chính có lý do | Trả chỗ; báo người đăng ký nếu không phải họ hủy |
| Đã duyệt | Vào | Đã vào | Hành chính | FR-033 | Ghi giờ vào, người thực tế |
| Đã duyệt | (Bộ lập lịch) Hết khung | Không đến | Hệ thống | Hết khung chưa Vào | Trả chỗ |
| Đã vào | Ra | Đã ra | Hành chính | Giờ ra ≥ giờ vào | Ghi giờ ra |

#### F. Người thân ở lại (14.4, BR-M10-04)

- **FR-040**: Hành chính MUST đăng ký được lượt ở lại gồm: người thân (quan hệ Hiệu lực), người cao tuổi, thời gian bắt đầu và kết thúc dự kiến, vị trí (phòng của người cao tuổi hoặc vị trí khác do cơ sở khai báo), lý do, có đăng ký ăn hay không. Lượt mới ở trạng thái Chờ xác nhận và Trưởng tầng được giao của tầng người cao tuổi MUST nhận thông báo; tầng không có Trưởng tầng được giao thì Quản lý viện xác nhận. Chỉ lượt Đã xác nhận MUST được Bắt đầu ở lại. Mỗi người cao tuổi MUST có tối đa CFG-M10-10 (đề xuất, mặc định \[1\]) lượt Đã xác nhận hoặc Đang ở lại chồng thời gian; một người thân MUST NOT có hai lượt Chờ xác nhận, Đã xác nhận hoặc Đang ở lại chồng thời gian. Khi phòng của người cao tuổi có từ hai giường đang sử dụng, lệnh Xác nhận MUST bắt buộc ghi đã hỏi ý kiến người cùng phòng (hoặc người đại diện của họ) và kết quả. *(Nguồn: 14.4 "được phép ở lại"; Clarification 2026-09-26 lượt 3, đề xuất Q-128; Clarification 2026-09-26 lượt 2, đề xuất Q-123)*
- **FR-041**: Đăng ký và Bắt đầu ở lại MUST bị chặn khi vị trí hoặc phòng của người cao tuổi đang chịu khoanh vùng hoặc cách ly (feature 003, 007), hoặc người cao tuổi không Đang lưu trú. *(Suy ra từ BR-M05-11; xem Điểm báo lại 8)*
- **FR-042**: Khi lượt chuyển Đang ở lại, hệ thống MUST: (a) cung cấp cho feature 011 người ở lại có đăng ký ăn trong khoảng ở lại để tính vào số suất (BR-M08-01); (b) yêu cầu feature 010 tạo một khoản chi phí cho mỗi đêm ở lại (FR-043) theo phiên bản đơn giá dịch vụ "Người thân ở lại" hiệu lực tại ngày bắt đầu của đêm đó, nguồn là lượt ở lại (DBR-15, Q-25); (c) suất ăn của người ở lại có đăng ký ăn MUST được tính chi phí theo đơn giá suất ăn ngoài hợp đồng, nguồn là suất ăn do feature 011 ghi nhận, không phải lượt ở lại (Điểm báo lại 14). *(Nguồn: BR-M10-04)*
- **FR-043**: Đơn vị tính phí của lượt ở lại MUST là đêm: số đêm = số lần lượt ở lại đi qua mốc 00:00 giữa thời điểm bắt đầu thực tế và thời điểm kết thúc thực tế (lượt đang mở tính tới thời điểm hiện tại); lượt không qua mốc 00:00 nào có 0 đêm và không tạo chi phí ngày. Suất ăn tính riêng theo FR-042 (a), không phụ thuộc số đêm. *(Nguồn: BR-M10-04 "đơn giá mỗi ngày"; Clarification 2026-09-26 lượt 2, đề xuất Q-123)*
- **FR-044**: Khi người cao tuổi chuyển Tạm vắng, Điều trị tại bệnh viện hoặc trạng thái cuối trong lúc có lượt Đang ở lại, Hành chính và Trưởng tầng MUST được nhắc kết thúc lượt; lượt MUST NOT tự kết thúc. *(Đề xuất)*
- **FR-045**: Bảng trạng thái lượt ở lại:

| Từ | Lệnh | Đến | Ai | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Đăng ký ở lại | Chờ xác nhận | Hành chính | FR-040, FR-041 | Báo Trưởng tầng được giao của tầng |
| Chờ xác nhận | Xác nhận | Đã xác nhận | Trưởng tầng được giao của tầng (không có thì Quản lý viện) | FR-041 kiểm tra lại | Báo Hành chính |
| Chờ xác nhận | Từ chối | Từ chối | Trưởng tầng được giao của tầng (không có thì Quản lý viện) | Có lý do | Báo Hành chính |
| Đã xác nhận | Bắt đầu ở lại | Đang ở lại | Hành chính, Trưởng tầng | FR-041 kiểm tra lại | FR-042 |
| Chờ xác nhận, Đã xác nhận | Hủy | Hủy | Hành chính | Có lý do | — |
| Đang ở lại | Gia hạn | Đang ở lại | Hành chính | Có lý do | Lưu lịch sử dự kiến kết thúc |
| Đang ở lại | Kết thúc ở lại | Đã kết thúc | Hành chính, Trưởng tầng | Thời điểm kết thúc ≥ bắt đầu | Dừng suất ăn và chi phí sau ngày kết thúc |

#### G. Cổng thông tin người thân (14.5, 14.6, BR-M10-01)

- **FR-046**: Cổng MUST chỉ hiển thị người cao tuổi mà người thân có quan hệ Hiệu lực (feature 002 FR-045) và chia thông tin thành bốn loại, hiển thị theo quyền: (1) **chung**, mọi người thân có quan hệ: họ tên, ảnh, phòng, trạng thái lưu trú, lịch sinh hoạt gồm buổi hoạt động, lượt thăm, lịch đến/về bán trú, lượt vắng dự kiến, thực đơn chung của viện (không gồm chế độ ăn riêng); hoạt động đã tham gia; lượt vắng; lượt thăm và phản hồi của chính mình; bản tin; thông báo; (2) **sức khỏe**, khi có quyền xem sức khỏe có tác dụng (FR-011): dị ứng và bệnh nền Hiệu lực, chỉ số, cân nặng, thuốc đang dùng, sự cố theo phạm vi được chia sẻ, kết quả công việc chăm sóc, tỷ lệ ăn và lượng nước, chế độ ăn riêng, lịch khám bên ngoài; (3) **chi phí**, khi có quyền xem chi phí: bảng chi phí đã chốt và chi phí tạm tính của kỳ đang mở do feature 010 cung cấp (feature 010 FR-037, FR-038). Tên thuốc trên khoản chi phí chỉ hiện khi người thân có thêm quyền xem sức khỏe có tác dụng; nếu không, chỉ hiện "Thuốc" và mã vật phẩm (Q-133, BR-M10-10); (4) **hợp đồng, dịch vụ**, chỉ người đại diện: hợp đồng, phụ lục, đặt cọc, tiến trình kết thúc lưu trú (feature 004). Phân loại này MUST trùng với loại thông tin của feature 009 FR-013. *(Nguồn: 14.5, 14.6, BR-M10-01, 4.4 chú thích ¹ ² ⁷)*
- **FR-046b**: Ngoài bốn loại ở FR-046, cổng MUST hiển thị mục **đồ gửi** do feature 013 cung cấp (danh sách đồ gửi, trạng thái, lịch sử bàn giao, ảnh) chỉ cho người đại diện và người thân có quyền "được phép đón" đang hiệu lực của người cao tuổi đó, xét tại thời điểm xem; người thân khác MUST NOT thấy mục này. Cổng MUST cho người đại diện: xác nhận hoặc từ chối (kèm lý do) yêu cầu xác nhận người nhận khác; ghi hoặc rút (kèm lý do) đồng ý cho tự giữ tiền mặt, trang sức. Vòng đời, điều kiện và thông báo của hai thao tác này do feature 013 định nghĩa (feature 013 FR-015a, FR-017, FR-017b, FR-030). *(Nguồn: 14.6, BR-M12-04, BR-M12-06, BR-M12-09; đồng bộ spec 013)*
- **FR-046a**: Cổng MUST cho người đại diện có quan hệ Hiệu lực xem và quyết định (Đồng ý / Từ chối kèm lý do) các đề nghị mua hộ ở trạng thái Chờ đồng ý của người cao tuổi mình đại diện. Đề nghị hiển thị mục cần mua, số lượng, số tiền dự kiến, lý do, người yêu cầu. Với thuốc, chỉ hiện tên thuốc khi người đại diện có quyền xem sức khỏe có tác dụng (Q-133). Vòng đời, việc nhắc và quy tắc "quyết định đầu tiên được ghi nhận" do feature 010 định nghĩa (feature 010 FR-023, FR-023a). Yêu cầu này không đi qua vòng đời "Chờ xác nhận" của FR-020. Người đại diện cũng MUST xem được, trên phần chi phí, khoản mua hộ đã duyệt có phần vượt số đã đồng ý, kèm lý do của Quản lý viện (Q-137). Người thân chưa lập đề nghị mua hộ qua cổng ở giai đoạn này. *(Nguồn: BR-M11-07, BR-M10-10, UC-79; đồng bộ spec 010)*
- **FR-046c**: **(Đồng bộ spec 014, Q-167, Q-176)** Người đại diện MUST ghi được "đồng ý về muộn" cho người bán trú vào một buổi hoạt động hoặc chuyến đi cụ thể kết thúc sau giờ về theo lịch, và rút được đồng ý trước giờ bắt đầu của buổi; bản ghi gửi cho feature 014 (FR-016a). Người đại diện được báo khi giờ về dự kiến của ngày được dời. Mục hoạt động trên cổng (loại "chung") gồm buổi đã đăng ký, chuyến đi (điểm đến, giờ rời, giờ về dự kiến) và số hoạt động đã tham gia theo feature 014 FR-033; không gồm chỉ định hạn chế, mức độ tham gia, cảnh báo cô lập (feature 014 FR-068). *(Nguồn: 3.4, 14.6; feature 014 FR-016a, FR-068)*
- **FR-047**: Người thân MUST chỉ thấy giá trị hiện hành, không thấy nhật ký hay giá trị trước đính chính (feature 000). *(Nguồn: 19.4)*
- **FR-048**: Mỗi lần người thân mở một phần thuộc loại sức khỏe MUST được ghi nhật ký gồm: người thân, người cao tuổi, phần đã xem, nguồn (cổng / bản tin / thông báo), thời điểm. Với bản tin và thông báo gửi qua feature 009, lần xem được ghi khi người thân mở bản tin hoặc thông báo có phần sức khỏe mà người đó được thấy (feature 009 FR-034), mỗi lần mở một bản ghi. Quản lý viện MUST tra cứu được nhật ký này; người thân MUST NOT xem nhật ký. *(Nguồn: NFR-08, 19.4)*
- **FR-049**: Chỉ người đại diện có quyền "được yêu cầu thay đổi dịch vụ" MUST thấy và dùng được thao tác gửi yêu cầu thay đổi dịch vụ; yêu cầu MUST được chuyển cho feature 004 (FR-039). *(Nguồn: BR-M10-01)*
- **FR-050**: Người thân MUST thực hiện được trên cổng: đăng ký, hủy lượt thăm (khi có quyền); gửi và theo dõi phản hồi; xác nhận/từ chối yêu cầu thay đổi quyền và ngoại lệ đón (chỉ người đại diện); gửi yêu cầu thay đổi quyền theo FR-014; xem và xác nhận thông báo (feature 009). *(Nguồn: 14.6)*

#### H. Bản tin tuần (14.5, BR-M10-06, 08, 09)

- **FR-051**: Theo lịch CFG-M10-03 (mặc định \[thứ 2 hằng tuần\]), Bộ lập lịch MUST sinh một bản nháp bản tin cho mỗi người cao tuổi ở Đang lưu trú, Tạm vắng hoặc Điều trị tại bệnh viện tại thời điểm sinh, kỳ là 7 ngày liền trước ngày sinh. Người cao tuổi không có mặt tại viện ngày nào trong kỳ (Tạm vắng hoặc Điều trị tại bệnh viện cả kỳ) MUST NOT có bản nháp; hệ thống ghi "bỏ qua – vắng cả kỳ" cho (người cao tuổi, kỳ) đó. Bản nháp là duy nhất theo (người cao tuổi, kỳ); chạy lại MUST NOT tạo trùng. *(Nguồn: BR-M10-08, NFR-04)*
- **FR-052**: Bản nháp MUST tự tổng hợp từ dữ liệu đã ghi, không soạn tay, gồm các phần theo loại thông tin: **chung** — số hoạt động đã tham gia (feature 014), số ngày vắng trong kỳ; **sức khỏe** — tỷ lệ ăn trung bình, lượng nước trung bình (feature 005), cân nặng và xu hướng, các chỉ số chính, sự cố trong kỳ kèm mức (feature 007 FR-082); **chi phí** — chi phí tạm tính của kỳ bản tin (feature 010 FR-038, Q-136): tổng theo loại của các khoản chưa hủy, chưa chốt, trừ khoản nhập tay và mua hộ chưa được duyệt, không có chi tiết từng khoản và không có tên thuốc, kèm nhãn "tạm tính, chưa chốt". Phần nào thiếu dữ liệu MUST hiển thị "không có dữ liệu trong kỳ", không để trống. *(Nguồn: 14.5)*
- **FR-053**: Điều dưỡng có phạm vi dữ liệu với người cao tuổi tại thời điểm thao tác MUST nhập được nhận xét (phần chung và phần sức khỏe) và duyệt bản nháp. Khi bản nháp đã Chờ duyệt quá CFG-M10-12 (đề xuất, mặc định [96 giờ]) kể từ lúc sinh, Trưởng tầng được giao của tầng người cao tuổi MUST cũng nhận xét và duyệt thay được; bản tin ghi dấu "Trưởng tầng duyệt thay". Nếu trong kỳ có sự cố mức Trung bình hoặc Khẩn cấp, bản nháp MUST có phần giải thích (loại sức khỏe) của người duyệt trước khi được duyệt, kể cả khi duyệt thay. "Sự cố mức Trung bình hoặc Khẩn cấp" MUST xét theo mức cao nhất mà sự cố từng có tính tới thời điểm duyệt, với sự cố có thời điểm xảy ra trong kỳ; sự cố Đã hủy (feature 007) không tính (đề xuất Q-129). *(Nguồn: BR-M10-08, BR-M10-09, UC-60, 4.4 dòng "Bản tin định kỳ": ĐD T; Clarification 2026-09-26 lượt 2, đề xuất Q-124)*
- **FR-054**: Khi duyệt, bản tin MUST được gửi qua feature 009 tới mọi người thân có quan hệ Hiệu lực với nội dung chia phần theo loại thông tin; mức thông báo Nhẹ; chịu giờ yên tĩnh (feature 009 FR-022). Thời điểm gửi và thời điểm xem của từng người MUST được ghi (BR-M10-06, feature 009 FR-034). *(Nguồn: BR-M10-06, BR-M10-08)*
- **FR-055**: Bản nháp chưa duyệt sau 48 giờ (CFG-M10-03) kể từ lúc sinh MUST nhắc Trưởng tầng của tầng người cao tuổi (mức Trung bình); sau CFG-M10-12 MUST báo Trưởng tầng rằng bản nháp đã có thể được duyệt thay (mức Trung bình). *(Nguồn: BR-M10-08; Clarification 2026-09-26 lượt 2, đề xuất Q-124)*
- **FR-056**: Bản tin đã gửi MUST NOT sửa. Sai sót MUST xử lý bằng bản tin đính chính trỏ về bản gốc, gồm nội dung đúng và lý do đính chính; người lập và duyệt theo FR-053 (Điều dưỡng; Trưởng tầng được giao nếu bản gốc do Trưởng tầng duyệt thay), BR-M10-09 áp như bản thường. Bản đính chính MUST gửi qua feature 009 tới người thân có quan hệ Hiệu lực tại thời điểm gửi đính chính, lọc theo quyền hiện hành; bản gốc vẫn xem được, có dấu "đã được đính chính". *(Nguồn: BR-M10-08, 1.5 nhóm 3)*
- **FR-057**: Bảng trạng thái bản tin:

| Từ | Lệnh | Đến | Ai | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Sinh bản nháp | Chờ duyệt | Bộ lập lịch | FR-051 | — |
| Chờ duyệt | Nhận xét | Chờ duyệt | Điều dưỡng; Trưởng tầng sau mốc báo của CFG-M10-11 | FR-053 | Lưu nhận xét, người, thời điểm |
| Chờ duyệt | Duyệt | Đã gửi | Điều dưỡng; Trưởng tầng được giao sau CFG-M10-12 (duyệt thay) | FR-053, BR-M10-09 | Gửi qua 009 (FR-054); ghi dấu "duyệt thay" nếu có |
| Chờ duyệt | (Bộ lập lịch) Sinh kỳ kế tiếp | Không gửi | Hệ thống | Bản nháp kỳ sau được sinh | Báo Quản lý viện (FR-073) |
| Chờ duyệt | (Hệ thống) Người cao tuổi qua đời | Không gửi | Hệ thống | feature 004 FR-073 | — |

#### I. Phản hồi, khiếu nại (14.7, BR-M10-05)

- **FR-058**: Người thân có quan hệ Hiệu lực MUST gửi được phản hồi qua cổng; Hành chính MUST ghi được thay người thân. Phản hồi gồm: người gửi, người cao tuổi, loại (Góp ý / Khiếu nại / Hỏi đáp), nhóm nội dung (chăm sóc, sinh hoạt / sức khỏe, thuốc / ăn uống / chi phí, hợp đồng / khác), nội dung, tệp đính kèm, nguồn (cổng / quầy), thời điểm gửi. Nội dung trả lời của phản hồi nhóm "sức khỏe, thuốc" MUST được xếp loại sức khỏe; khi người gửi không có quyền xem sức khỏe có tác dụng (FR-011), người phụ trách MUST được cảnh báo lúc soạn trả lời và phần trả lời hiển thị cho người gửi MUST chỉ ở loại chung. *(Nguồn: 14.7, UC-59, DBR-03)*
- **FR-059**: Người phụ trách mặc định MUST theo nhóm nội dung: chăm sóc, sinh hoạt, ăn uống → Trưởng tầng được giao của tầng người cao tuổi; sức khỏe, thuốc → Trưởng tầng được giao (Điều dưỡng MAY được giao lại); chi phí, hợp đồng, khác → Hành chính (bất kỳ nhân viên Hành chính nào nhận trước). Tầng không có Trưởng tầng được giao thì giao Quản lý viện. *(Nguồn: 19.3 "Trưởng tầng tham gia xử lý phản hồi của người thân trong tầng", 4.4 dòng "Phản hồi, khiếu nại"; Clarification 2026-09-26 lượt 2, đề xuất Q-122)*
- **FR-060**: Trưởng tầng, Hành chính và Quản lý viện MUST giao lại được người phụ trách cho người có quyền T ở dòng "Phản hồi, khiếu nại" (TT, ĐD, HC trong phạm vi), bắt buộc lý do. *(Nguồn: 4.4)*
- **FR-061**: Hạn xử lý MUST = thời điểm gửi (hoặc thời điểm Mở lại) + CFG-M10-01 theo mức ưu tiên (mặc định Cao \[24 giờ\], Thường \[72 giờ\]). Phản hồi được coi là đã xử lý đúng hạn khi chuyển Đã phản hồi trước hạn. *(Nguồn: BR-M10-05, CFG-M10-01)*
- **FR-062**: Mức ưu tiên mặc định: Khiếu nại → Cao; Góp ý, Hỏi đáp → Thường. Người phụ trách MAY nâng hoặc hạ mức kèm lý do; hạn tính lại từ thời điểm gửi (hoặc Mở lại gần nhất) theo mức mới; nếu hạn mới đã qua thì phản hồi lập tức được coi là quá hạn (FR-063). *(Nguồn: 14.7, BR-M10-05; Clarification 2026-09-26 lượt 2, đề xuất Q-122)*
- **FR-063**: Khi phản hồi quá hạn mà chưa Đã phản hồi, Bộ lập lịch MUST gắn dấu "quá hạn" và gửi thông báo leo thang mức Trung bình cho Quản lý viện (BR-M10-05), đúng một lần cho mỗi hạn; không có nhắc lặp sau đó, nhưng phản hồi MUST nằm trong danh sách phản hồi quá hạn của Quản lý viện tới khi Đã phản hồi. Phản hồi Mở lại có hạn mới (FR-061) và MUST leo thang lại nếu hạn mới bị quá. Quản lý viện MUST xem được mọi phản hồi và giao lại người phụ trách. *(Nguồn: BR-M10-05, 4.4 QL D)*
- **FR-064**: Phản hồi ở Đã phản hồi quá CFG-M10-02 (mặc định \[7 ngày\]) mà người gửi không xác nhận hay mở lại MUST tự chuyển Đóng với cách đóng "tự đóng". *(Nguồn: 14.7)*
- **FR-065**: Nội dung phản hồi, nội dung trả lời, hướng xử lý và kết quả là ghi nhận không sửa; bổ sung bằng ghi chú mới. *(Nguồn: 1.5)*
- **FR-066**: Bảng trạng thái phản hồi:

| Từ | Lệnh | Đến | Ai | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Gửi | Mới | Người thân; Hành chính ghi thay | FR-058 | Đặt người phụ trách, mức, hạn; báo người phụ trách |
| Mới, Mở lại | Nhận xử lý | Đang xử lý | Người phụ trách | — | — |
| Đang xử lý | Trả lời | Đã phản hồi | Người phụ trách | Có hướng xử lý và nội dung trả lời | Báo người gửi |
| Đã phản hồi | Đồng ý đóng | Đóng | Người gửi | — | Ghi thời điểm đóng, cách đóng |
| Đã phản hồi | (Bộ lập lịch) Hết CFG-M10-02 | Đóng | Hệ thống | — | Cách đóng "tự đóng" |
| Đã phản hồi | Mở lại | Mở lại | Người gửi | Có lý do | Hạn mới (FR-061); báo người phụ trách |

#### J. Quyền, thông báo, nhật ký và giao tiếp

- **FR-067**: Quyền thực hiện theo vai trò (khớp Permission Matrix 4.4; ô ghi "đề xuất" chưa có trong ma trận, xem Điểm báo lại 2):

| Chức năng | QL | TT | ĐD | CS | HC | NT đại diện | NT khác |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Hồ sơ, quan hệ, vai trò người thân | X, D (đặt/thôi người đại diện khi đã có người đại diện, FR-005) | — | — | — | T (gồm phiếu đăng ký người thân, FR-008) | X (của mình), xác nhận đặt/thôi người đại diện khác | X (của mình) |
| Yêu cầu thay đổi quyền, danh sách đón, dấu "được tự về" | D | — | — | — | T | T, xác nhận | T (chỉ với chính mình, FR-014) |
| Quy trình đón, ngoại lệ đón | D (ngoại lệ) | T (Cho tạm vắng) | — | T (Điểm danh về, gồm "Tự về") | T (mọi loại, gồm "Tự về") | Xác nhận ngoại lệ | — |
| Đăng ký thăm | X | X | — | — | T | T (nếu có quyền) | T (nếu có quyền) |
| Vào/ra lượt thăm | X | X | — | — | T | — | — |
| Người thân ở lại | X, xác nhận khi tầng không có Trưởng tầng | T (xác nhận, từ chối, bắt đầu, kết thúc) | — | — | T (đăng ký, bắt đầu, gia hạn, kết thúc, hủy) | X | X |
| Phản hồi | D | T | T | — | T | T | T |
| Bản tin | X | X, duyệt thay sau CFG-M10-12 (Q-124, Q-130) | T | — | — | X | X |
| Nhật ký lượt xem sức khỏe | X | — | — | — | — | — | — |
| Đề nghị mua hộ (feature 010, UC-79) | D | — | — | — | T (lập, gửi, ghi đã mua) | Đồng ý / từ chối (FR-046a) | — |

- **FR-068**: Mọi lệnh trên dữ liệu nhóm 2 của spec MUST ghi nhật ký với người thực hiện, thời điểm, lý do, giá trị trước/sau (feature 000); lịch sử danh sách được phép đón MUST cho biết tại một thời điểm bất kỳ ai được phép đón. *(Nguồn: 14.1 "được ghi lịch sử", 19.4)*
- **FR-069**: Thông báo do spec phát ra (feature 009 FR-043a), khóa sự kiện là (bản ghi nguồn, loại sự kiện) và thêm số thứ tự lần nhắc với nhắc lặp:

| Sự kiện | Mức | Người nhận | Loại thông tin |
| --- | --- | --- | --- |
| Yêu cầu thay đổi quyền cần xác nhận/duyệt | Trung bình | Người đại diện; Quản lý viện | chung |
| Yêu cầu thay đổi quyền có kết quả | Nhẹ | Người lập; người thân bị tác động; Hành chính | chung |
| Yêu cầu đặt/thôi người đại diện cần xác nhận | Trung bình | Người đại diện hiện có khác người bị tác động; Quản lý viện | chung |
| Yêu cầu thay đổi quyền Chờ xác nhận quá mốc nhắc / mốc báo của CFG-M10-11 | Trung bình | Người đại diện, Quản lý viện / Quản lý viện | chung |
| Yêu cầu ngoại lệ đón cần xác nhận/duyệt | Trung bình | Người đại diện; Quản lý viện | chung |
| Lần chặn đón | Nhẹ | Trưởng tầng của tầng | — (nội bộ) |
| Lượt thăm bị hủy bởi Hành chính hoặc do người cao tuổi vắng | Nhẹ | Người đăng ký | chung (thông báo về lượt của chính người nhận, không phụ thuộc quyền đăng ký thăm hiện hành) |
| Lượt thăm bị hủy do khoanh vùng | Trung bình | Người đăng ký | chung (như trên) |
| Khách đang trong vùng lúc bắt đầu khoanh vùng | Trung bình | Hành chính | — (nội bộ) |
| Nhắc ghi giờ ra | Nhẹ | Hành chính | — (nội bộ) |
| Lượt ở lại cần xác nhận | Trung bình | Trưởng tầng được giao của tầng (không có thì Quản lý viện) | — (nội bộ) |
| Lượt ở lại được xác nhận hoặc từ chối | Nhẹ | Hành chính; người thân ở lại | chung |
| Nhắc kết thúc lượt ở lại | Trung bình | Hành chính, Trưởng tầng | — (nội bộ) |
| Phản hồi mới, được giao lại, mở lại | Trung bình | Người phụ trách | — (nội bộ) |
| Phản hồi đã được trả lời | Nhẹ | Người gửi | chung (nội dung trả lời thuộc nhóm nội dung phản hồi; phản hồi nhóm sức khỏe, thuốc xếp sức khỏe) |
| Phản hồi quá hạn (leo thang) | Trung bình | Quản lý viện | — (nội bộ) |
| Bản tin | Nhẹ | Mọi người thân có quan hệ Hiệu lực | chung, sức khỏe, chi phí (FR-052) |
| Bản tin đính chính | Nhẹ | Người thân có quan hệ Hiệu lực lúc gửi đính chính | chung, sức khỏe, chi phí (theo phần được đính chính, FR-056) |
| Bản tin quá hạn duyệt | Trung bình | Trưởng tầng | — (nội bộ) |
| Bản tin có thể được Trưởng tầng duyệt thay | Trung bình | Trưởng tầng | — (nội bộ) |
| Bản tin Không gửi vì quá kỳ | Nhẹ | Quản lý viện | — (nội bộ) |

- **FR-070**: Nội dung rút gọn cho tin nhắn MUST NOT chứa thông tin sức khỏe (feature 009 FR-019). *(Nguồn: feature 009)*
- **FR-071**: Giao tiếp với các feature khác:

| Chiều | Feature | Nội dung |
| --- | --- | --- |
| Cung cấp | 001 | Người đại diện hợp lệ để làm người đồng ý (FR-011 của 001) |
| Nhận | 001 | Bản đồng ý Hiệu lực và phạm vi người thân; trạng thái người cao tuổi |
| Cung cấp | 002 | Quan hệ Hiệu lực, quyền từng người thân (FR-045, FR-046 của 002); dấu "được tự về" gắn với người cao tuổi và vai trò xác nhận của người đại diện không phải quyền xem, 002 không cần dùng |
| Cung cấp | 004 | Người đại diện (ký hợp đồng); bản ghi đón (Cho tạm vắng, bàn giao khi kết thúc) |
| Nhận | 004 | Lượt vắng, kết thúc lưu trú, qua đời; yêu cầu thay đổi dịch vụ của người đại diện (FR-039 của 004) |
| Cung cấp | 005 | Bản ghi đón cho Điểm danh về; lượt thăm cho thời khóa biểu (FR-048 của 005) |
| Nhận | 005, 014 | Ăn uống, nước (005), hoạt động (014) cho bản tin |
| Nhận | 011 | Thực đơn chung (loại "chung") và chế độ ăn riêng của người cao tuổi (loại "sức khỏe") cho cổng (FR-046) |
| Nhận, Cung cấp | 007 | Nhận trạng thái khoanh vùng, dữ liệu bản tin (FR-082 của 007); cung cấp lượt thăm (FR-060 của 007), người liên hệ chính |
| Cung cấp | 009 | Người liên hệ chính, người đại diện, quyền, thứ tự liên hệ, số liên hệ; yêu cầu thông báo (FR-069) |
| Cung cấp | 010 | Lượt ở lại làm nguồn chi phí (mỗi mốc 00:00 là một đêm); quyết định của người đại diện về đề nghị mua hộ (FR-046a); nhận chi phí tạm tính, bảng chi phí đã chốt (có che tên thuốc theo Q-133) và đề nghị mua hộ Chờ đồng ý |
| Cung cấp | 011 | Người ở lại có đăng ký ăn, khi lượt chuyển Đang ở lại, kết thúc sớm hoặc bị hủy (feature 011 FR-034 (c), FR-043) |
| Nhận, Cung cấp | 013 | Nhận đồ gửi, lịch sử bàn giao, ảnh, yêu cầu xác nhận người nhận khác, đồng ý cho tự giữ cho cổng (FR-046b); cung cấp người đại diện, người có quyền "được phép đón", người liên hệ chính, giấy tờ tùy thân, và quyết định của người đại diện trên cổng (feature 013 FR-016, FR-017, FR-015a) |
| Cung cấp (giai đoạn sau, Q-194) | 016 | Dữ liệu thăm, phản hồi, bản tin cho báo cáo |

- **FR-072**: Mọi quy tắc theo thời gian của spec (hạn phản hồi, tự đóng, sinh bản tin, hạn duyệt, Không đến, hạn ngoại lệ đón) MUST kiểm thử được bằng đồng hồ giả lập và dùng múi giờ Asia/Ho_Chi_Minh. *(Nguồn: NFR-09, NFR-13)*
- **FR-073**: Khi bản tin chuyển Không gửi vì quá kỳ, Quản lý viện MUST được báo (mức Nhẹ) kèm người cao tuổi, kỳ và các lần nhắc đã gửi. *(Nguồn: Clarification 2026-09-26 lượt 2, đề xuất Q-124)*
- **FR-074**: Mọi tham số CFG-M10-01 → 12 MUST thay đổi được bởi Quản lý viện, có nhật ký (BR-M15-04). *(Nguồn: NFR-12)*
- **FR-075**: Khi người cao tuổi chuyển trạng thái cuối: lượt thăm tương lai MUST chuyển Hủy; bản tin Chờ duyệt MUST chuyển Không gửi nếu Qua đời, còn với Kết thúc lưu trú hoặc Hủy tiếp nhận thì vẫn được duyệt và gửi cho kỳ trước thời điểm kết thúc, và không có bản nháp cho kỳ sau (FR-051); yêu cầu thay đổi quyền và ngoại lệ đón Chờ xác nhận MUST chuyển Hủy với lý do trạng thái cuối; phản hồi đang mở MUST tiếp tục xử lý; quan hệ MUST giữ Hiệu lực cho tới khi tài khoản bị khóa theo CFG-M01-04 (feature 002). *(Nguồn: 5.6, feature 004 FR-073)*

### Truy vết quy tắc → kịch bản chấp nhận

| Quy tắc | Kịch bản | FR chính |
| --- | --- | --- |
| BR-M10-01 | US5 kịch bản 1 → 7; US1 kịch bản 6 | FR-046, FR-049 |
| BR-M10-02 | US4 kịch bản 1 → 7, 11, 12 | FR-030, FR-036, FR-037 |
| BR-M10-03 | US2 kịch bản 3 → 6, 9, 11 | FR-021, FR-023, FR-024 |
| BR-M10-04 | US8 kịch bản 2, 3 | FR-042, FR-043 |
| BR-M10-05 | US6 kịch bản 1, 3, 9 | FR-061, FR-063 |
| BR-M10-06 | US7 kịch bản 3 (bản tin); thông báo sự cố theo feature 009 | FR-054, feature 009 FR-034 |
| BR-M10-07 | US3 kịch bản 1 → 8; US1 kịch bản 9, 10 | FR-013 → FR-020 |
| BR-M10-08 | US7 kịch bản 1, 4 → 6, 9 | FR-051, FR-055, FR-056, FR-057 |
| BR-M10-09 | US7 kịch bản 2, 8 | FR-053 |
| DBR-02 | US1 kịch bản 2, 3 | FR-003 → FR-006 |
| DBR-03 | US1 kịch bản 4, 5; US5 kịch bản 1; US6 kịch bản 8 | FR-011, FR-058 |
| Q-123 đếm đêm ở lại | US8 kịch bản 3 | FR-043 |
| Q-124, Q-130 duyệt thay bản tin | US7 kịch bản 4 | FR-053, FR-055 |
| Q-125 dấu "được tự về" | US2 kịch bản 10 | FR-013, FR-021 |
| Q-126 phiếu đăng ký người thân | US1 kịch bản 9 | FR-008 |
| Q-127 đặt/thôi người đại diện | US1 kịch bản 3, 10 | FR-005 |
| Q-128 giới hạn lượt ở lại | US8 kịch bản 6 | FR-040 |

### Key Entities *(include if feature involves data)*

- **Người thân (NGUOI_THAN)** – nhóm 1: họ tên, số điện thoại, loại và số giấy tờ tùy thân, địa chỉ; liên kết tối đa một tài khoản (feature 002).
- **Quan hệ người thân (QUAN_HE_NGUOI_THAN)** – nhóm 2: người thân, người cao tuổi, quan hệ, là người đại diện, là người liên hệ chính, sáu quyền, thứ tự liên hệ, trạng thái, lịch sử.
- **Phiếu đăng ký người thân** – nhóm 3: người cao tuổi, người đại diện ký, danh sách người thân và quyền của từng người, bản scan, người ghi, thời điểm, các yêu cầu Hiệu lực đã tạo (FR-008).
- **Yêu cầu thay đổi quyền** – nhóm 2: quan hệ (hoặc người cao tuổi, với dấu "được tự về"), quyền, giá trị mới, thông tin người mới (khi thêm người đón), người lập, nguồn, lý do, trạng thái, người xác nhận/duyệt, cách xác nhận (cổng / bản ký / Quản lý viện duyệt), bằng chứng.
- **Yêu cầu ngoại lệ đón** – nhóm 2: người cao tuổi, người đón (họ tên, số giấy tờ, quan hệ), lý do, người lập, người xác nhận/duyệt, thời điểm hiệu lực, hạn (CFG-M10-08), trạng thái (Chờ xác nhận / Hiệu lực / Đã dùng / Hết hạn / Từ chối / Hủy), bản ghi đón đã dùng ngoại lệ.
- **Dấu "được tự về"** – nhóm 2: người cao tuổi bán trú, giá trị bật/tắt, yêu cầu đã bật/tắt; tác dụng suy ra từ cờ nguy cơ đi lạc hiện hành (feature 001).
- **Bản ghi đón** – nhóm 3: người cao tuổi, loại đón (Tạm vắng, Đi chơi với gia đình, Bán trú về, Tự về, Kết thúc lưu trú), người đón, căn cứ, kết quả xác minh, thời điểm, nhân viên bàn giao hoặc tiễn, người ghi, lệnh nguồn đã dùng.
- **Lần chặn đón** – nhóm 3: người cao tuổi, người đón được khai, lý do, người thực hiện, thời điểm.
- **Lượt thăm (LUOT_THAM)** – nhóm 2: người cao tuổi, người đăng ký, ngày, khung, người đi cùng, số người, nguồn, trạng thái, giờ vào, giờ ra, người thực tế vào.
- **Khung giờ thăm** – không phải danh mục: là giá trị có cấu trúc của tham số CFG-M10-04 (danh sách khung gồm giờ bắt đầu, giờ kết thúc, sức chứa), một giá trị hiện hành có lịch sử (DBR-24), thay đổi theo BR-M15-04.
- **Lượt ở lại** – nhóm 2: người thân, người cao tuổi, vị trí, lý do, bắt đầu và kết thúc (dự kiến, thực tế), có đăng ký ăn, người xác nhận hoặc từ chối và lý do, số đêm tính phí, trạng thái.
- **Phản hồi (PHAN_HOI)** – nhóm 2 cho trạng thái, nhóm 3 cho nội dung: người gửi, người cao tuổi, loại, nhóm nội dung, nội dung, đính kèm, nguồn, mức ưu tiên, hạn, người phụ trách và lịch sử giao, hướng xử lý, trả lời, dấu quá hạn, thời điểm và cách đóng.
- **Bản tin (BAN_TIN)** – nhóm 2 khi Chờ duyệt, nhóm 3 khi Đã gửi: người cao tuổi, kỳ, các phần nội dung theo loại thông tin, nhận xét, giải thích sự cố, người duyệt, thời điểm gửi, bản gốc (khi là bản đính chính).
- **Nhật ký lượt xem sức khỏe** – nhóm 3: người thân, người cao tuổi, phần đã xem, thời điểm.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Trong bộ kiểm thử 100 lượt đón (trong danh sách, ngoài danh sách, giấy tờ không khớp, ngoại lệ còn/hết hạn, người vừa bị bỏ khỏi danh sách), 0 lượt người cao tuổi rời viện với người không thuộc danh sách và không có ngoại lệ Hiệu lực; 100% lượt đón thành công có đủ người đón, căn cứ, kết quả xác minh, thời điểm, nhân viên bàn giao.
- **SC-002**: 0 người thân thấy thông tin sức khỏe khi không đồng thời có quyền xem sức khỏe và bản đồng ý Hiệu lực bao gồm mình; 100% lượt người thân xem phần sức khỏe có bản ghi nhật ký (NFR-08).
- **SC-003**: Yêu cầu bỏ người được phép đón có hiệu lực trong không quá 1 phút kể từ lúc lập; 0 thay đổi quyền khác có hiệu lực khi chưa có xác nhận của người đại diện hoặc duyệt của Quản lý viện.
- **SC-004**: Với đồng hồ giả lập, 0 lượt thăm được chấp nhận ngoài khung giờ, vượt sức chứa (kể cả khi 20 đăng ký gửi đồng thời cho chỗ cuối), trong khu khoanh vùng hoặc khi người cao tuổi không có mặt; người thân đã có tài khoản và đã đăng nhập hoàn tất một lượt đăng ký thăm (chọn ngày trong CFG-M10-05, chọn khung, nhập tối đa 2 người đi cùng) trên điện thoại trong không quá 2 phút, đo trên ít nhất 10 người thân dùng lần đầu.
- **SC-005**: 100% phản hồi quá hạn được báo Quản lý viện trong không quá 5 phút sau hạn; sau ba tháng vận hành, ít nhất 90% hạn xử lý của phản hồi mức Cao được đáp ứng (chuyển Đã phản hồi trước hạn), tính mỗi hạn là một lượt (lần gửi và mỗi lần Mở lại là các lượt riêng), mức xét là mức tại thời điểm hạn đó được tính (sau khi người phụ trách đổi mức, Q-122).
- **SC-006**: 100% người cao tuổi đủ điều kiện có bản nháp bản tin mỗi kỳ, chạy lại tạo 0 bản trùng; 0 bản tin có sự cố mức Trung bình trở lên được gửi mà thiếu giải thích; điều dưỡng hoàn tất nhận xét và duyệt một bản tin trong không quá 3 phút.
- **SC-007**: 0 bản tin, thông báo gửi người thân chứa phần thuộc loại mà người đó không có quyền (feature 009 FR-013).
- **SC-008**: Với bộ dữ liệu người thân ở lại 30 ngày (gồm lượt qua đêm, lượt trong ngày, lượt qua mốc đổi đơn giá), số đêm tính phí và số suất ăn khớp 100% bảng tính tay; 0 lượt được Bắt đầu khi chưa được xác nhận.
- **SC-009**: 100% lượt thăm Đã vào/Đã ra có giờ vào và danh sách người thực tế; feature 007 lấy được lượt thăm của một người cao tuổi trong CFG-M05-07 không quá 1 phút sau khi ghi sự cố lây nhiễm.
- **SC-010**: Tại mọi thời điểm, 100% người cao tuổi Đang lưu trú, Tạm vắng, Điều trị tại bệnh viện có đúng một người liên hệ chính và ít nhất một người đại diện (DBR-02).

## Assumptions

- Số feature `012` theo cột Feature của UC-55 → UC-60 ở mục 4.2 `docs/phan-tich-yeu-cau.md` và theo cách các spec 000 → 009 gọi Module 10; không theo số tuần tự kế tiếp (010, 011 đã dành cho chi phí và dinh dưỡng).
- Q-01 theo mặc định: quy trình đón bắt buộc trực tuyến; các thao tác khác của spec do người thân thực hiện trên cổng vốn trực tuyến.
- Q-03 vẫn mở: spec dùng bản đồng ý theo feature 001, không phụ thuộc mẫu cụ thể.
- Q-06 theo mặc định: người thân không có tài khoản cổng không xác nhận được qua cổng; khi đó dùng bản ký hoặc Quản lý viện duyệt.
- Khung giờ thăm và sức chứa là một cấu hình chung cho toàn viện (một khu tiếp khách), tương tự quyết định Q-45 của feature 003 về khu nghỉ bán trú; sức chứa tính theo số người, không theo số lượt; điều này đã đưa vào FR-030 (b).
- Người đón không phải người nhà cũng được lập hồ sơ người thân với quan hệ "Khác" để có số giấy tờ đối chiếu; spec không có danh sách người đón tách riêng.
- Người bán trú tự về chỉ khi có dấu "được tự về" (Q-125); "viện đưa về" chưa được hỗ trợ ở giai đoạn đầu.
- Kỳ bản tin là 7 ngày liền trước ngày sinh; bản nháp được sinh đầu ngày theo lịch CFG-M10-03; "48 giờ" tính từ thời điểm sinh.
- Tỷ lệ ăn, lượng nước và kết quả công việc chăm sóc xếp loại "sức khỏe" (hạn chế nhất); số hoạt động xếp loại "chung", theo ví dụ ở feature 009 User Story 4 kịch bản 5a.
- Dịch vụ "Người thân ở lại" là một dịch vụ ngoài hợp đồng trong danh mục 6.4, đơn giá theo phiên bản hiệu lực tại ngày phát sinh (Q-25).
- Leo thang phản hồi chỉ một cấp (lên Quản lý viện) như BR-M10-05; không có cấp leo thang tiếp theo.
- Người thân không xem nhật ký thay đổi (feature 000, 19.4); lịch sử danh sách đón chỉ hiển thị cho nhân viên.

## Điểm cần báo lại về tài liệu nguồn

Chưa sửa `docs/`. Các điểm dưới đây cần chủ tài liệu xác nhận.

1. **Vòng đời yêu cầu BR-M10-07 và vòng đời phê duyệt chung (6.6, feature 000)**: đã chốt khi clarify (Q-118, FR-020): vòng đời riêng Chờ xác nhận → Hiệu lực / Từ chối / Hủy. Cần bổ sung trạng thái Hủy vào BR-M10-07, ghi ngoại lệ vào 6.6 (spec 000 đã đồng bộ ngày 2026-09-26: FR-031, Điểm báo lại 1) và thêm Q-118 vào mục 24.2.
2. **Permission Matrix 4.4 thiếu dòng hoặc chưa đủ ô**: người thân ở lại (spec đề xuất HC T, TT T và xác nhận (Q-123), QL X và xác nhận khi tầng không có Trưởng tầng); ghi vào/ra lượt thăm (tách khỏi dòng "Đăng ký thăm, đón"); yêu cầu ngoại lệ đón (QL D, NT đại diện xác nhận); nhật ký lượt xem sức khỏe (QL X). Dòng "Người thân và quyền" ghi T² (chỉ người đại diện), nhưng spec cho mọi người thân tự bỏ mình khỏi danh sách đón hoặc tắt quyền của mình (FR-014, FR-067) — vượt ô ma trận, cần sửa chú thích ². **Tổng hợp các ô vượt ma trận** (chỉ có hiệu lực khi 4.4 được sửa, vì 19.2/Q-15 không cho thêm quyền vượt ma trận): TT, CS thực hiện quy trình đón (Q-121, Điểm 3); TT duyệt thay bản tin (Q-124, Điểm 9); NT khác tự bỏ mình khỏi danh sách đón (FR-014); QL xác nhận lượt ở lại khi tầng không có Trưởng tầng (Q-123); QL duyệt đặt/thôi người đại diện (Q-127). Cần bổ sung vào 4.4 và use case vào 4.2 (người thân ở lại chưa có UC).
3. **Dòng "Đăng ký thăm, đón" (4.4) chỉ cho HC T**, trong khi feature 004 FR-049 cho Trưởng tầng "Cho tạm vắng" và feature 005 cho Nhân viên chăm sóc "Điểm danh về" — hai lệnh này bắt buộc qua quy trình đón. Đã chốt khi clarify (Q-121, FR-022): người có quyền lệnh nguồn thực hiện quy trình đón. Cần tách 4.4 thành hai dòng "Đăng ký thăm" (HC T, NT T) và "Quy trình đón" (HC T, TT T, CS T, QL D cho ngoại lệ, NT đại diện xác nhận ngoại lệ), và thêm Q-121 vào mục 24.2.
4. **14.3 áp quy trình đón cho "bán trú về"** mà không nêu trường hợp tự về hoặc viện đưa về. Đã chốt khi clarify (Q-125): cho tự về khi có dấu "được tự về" bật qua BR-M10-07, không bật được khi có cờ nguy cơ đi lạc, vẫn ghi bản ghi đón loại "Tự về" (FR-013, FR-021, FR-026). Cần bổ sung 14.1 (dấu "được tự về"), 14.3, BR-M10-03, BR-M10-07 và thêm Q-125 vào mục 24.2; "viện đưa về" chưa có quy tắc.
5. **Đề xuất tham số mới cần thêm vào Phụ lục 25**: CFG-M10-04 khung giờ thăm và sức chứa mỗi khung \[09:00–11:00 và 15:00–17:00; 20 người mỗi khung\]; CFG-M10-05 thời hạn đăng ký trước \[tối thiểu 2 giờ, tối đa 14 ngày\]; CFG-M10-06 số người tối đa mỗi lượt thăm \[3\]; CFG-M10-07 nhắc ghi giờ ra sau khi hết khung \[30 phút\]; CFG-M10-08 hiệu lực của ngoại lệ đón \[4 giờ\]; CFG-M10-09 thời gian bản ghi đón chưa dùng còn làm căn cứ \[2 giờ\]; CFG-M10-10 số lượt người thân ở lại tối đa cùng lúc cho một người cao tuổi \[1\]; CFG-M10-11 nhắc/báo yêu cầu thay đổi quyền Chờ xác nhận lâu \[48 giờ / 96 giờ\]; CFG-M10-12 mốc Trưởng tầng được duyệt thay bản tin \[96 giờ\] (Q-130).
6. **Các bổ sung không có trong tài liệu nguồn** (đề xuất, cần xác nhận): xác nhận của một người đại diện là đủ (FR-016); yêu cầu do người đại diện gửi qua cổng coi như đã xác nhận (FR-015); tối đa một yêu cầu chờ cho mỗi (quan hệ, quyền) (FR-018); người thân tự bỏ mình khỏi danh sách đón hoặc tắt quyền của mình (FR-014); nhắc yêu cầu chờ lâu theo tham số riêng CFG-M10-11 (FR-019, Q-130); quyền "nhận thông báo khẩn" của người liên hệ chính luôn bật (FR-009, theo Q-96 của 009); quyền "được yêu cầu thay đổi dịch vụ" tự bật/tắt theo vai trò người đại diện (FR-010); thứ tự liên hệ (FR-012, theo 009 Điểm báo lại 17); bản ghi đón chưa dùng hết hiệu lực và bị vô hiệu khi người đón bị bỏ khỏi danh sách (FR-027, FR-017); giá trị hiện hành theo yêu cầu có hiệu lực muộn nhất (FR-018); hủy ngoại lệ đón chưa dùng (FR-024); ghi nhật ký lượt xem sức khỏe cả qua bản tin, thông báo (FR-048); không sinh bản tin khi vắng cả kỳ (FR-051); bản tin đính chính gửi theo quan hệ và quyền hiện hành (FR-056); giới hạn trả lời phản hồi sức khỏe (FR-058); leo thang phản hồi một lần cho mỗi hạn (FR-063); không có lệnh đổi khung thăm (FR-035); danh sách được phép đón rỗng (FR-025); tự hủy lượt thăm khi người cao tuổi vắng (FR-036); tắt quyền đăng ký thăm không hủy lượt đã duyệt (FR-035); thông báo về lượt thăm của chính người nhận xếp loại chung (FR-069); bản tin Chờ duyệt vẫn gửi khi Kết thúc lưu trú (FR-075); quy ước thuật ngữ Trưởng tầng và việc chuyển Quản lý viện khi tầng không có Trưởng tầng (Quy ước trong spec).
7. **14.7 không nêu cách đặt mức ưu tiên và người phụ trách**. Đã chốt khi clarify (Q-122): người phụ trách theo nhóm nội dung (FR-059); Khiếu nại mặc định Cao, còn lại Thường, người phụ trách đổi được mức kèm lý do (FR-062); mở lại tính hạn mới (FR-061, đề xuất). Cần bổ sung 14.7 và thêm Q-122 vào mục 24.2.
8. **14.4 và BR-M10-04 không nêu**: ai cho phép ở lại và cách đếm ngày tính phí — đã chốt khi clarify (Q-123): Trưởng tầng được giao của tầng xác nhận (FR-040, FR-045), tính theo đêm qua mốc 00:00 (FR-043); BR-M10-04 ghi "đơn giá mỗi ngày" cần sửa thành "mỗi đêm", bổ sung 14.4 và thêm Q-123 vào mục 24.2. Còn là đề xuất: chặn ở lại trong khu khoanh vùng/cách ly (FR-041), xử lý khi người cao tuổi rời viện trong lúc có người ở lại (FR-044). BR-M05-11 chỉ chặn "đăng ký thăm mới", chưa nêu người thân ở lại.
9. **BR-M10-08 chỉ nêu nhắc Trưởng tầng khi quá hạn duyệt**, không nêu bản nháp không bao giờ được duyệt. Đã chốt khi clarify (Q-124): sau CFG-M10-12 Trưởng tầng được giao duyệt thay (FR-053); tới kỳ kế tiếp vẫn chưa duyệt thì Không gửi và báo Quản lý viện (FR-057, FR-073). Việc Trưởng tầng duyệt vượt ô "X" của dòng "Bản tin định kỳ" trong Permission Matrix 4.4 — theo 19.2 (Q-15) quản lý không được thêm quyền vượt ma trận, nên cần sửa 4.4 (TT: T sau CFG-M10-12), bổ sung BR-M10-08 và thêm Q-124 vào mục 24.2. Cũng chưa nêu "Điều dưỡng phụ trách" với bản tin tuần là ai; spec dùng Điều dưỡng có phạm vi dữ liệu với người cao tuổi lúc thao tác (FR-053), vì Điều dưỡng phụ trách (2.4) được gán theo ca.
10. **Trạng thái lượt thăm (14.2)**: đã chốt khi clarify (Q-119, Q-120): lượt hợp lệ tự Đã duyệt nên trạng thái Đăng ký và Từ chối của 14.2 không còn được dùng cho lượt thăm (đăng ký không đạt bị từ chối ngay, không tạo lượt); lượt Đã duyệt trong vùng vừa khoanh vùng tự Hủy. Cần sửa 14.2 thành "Đã duyệt → Đã vào → Đã ra / Không đến / Hủy", bổ sung BR-M05-11 ("lượt đã duyệt trong vùng tự hủy"), thêm Q-119, Q-120 vào mục 24.2. Spec 007 đã đồng bộ ngày 2026-09-26 (Edge Case "Lượt thăm đã được duyệt trước khi khoanh vùng", FR-065).
11. **LUOT_THAM (3.2)** chưa có người đi cùng, số người, người thực tế vào — cần cho sức chứa và truy vết (FR-029, FR-033). **ERD khái niệm chưa có** yêu cầu thay đổi quyền, yêu cầu ngoại lệ đón, bản ghi đón, lượt ở lại, nhật ký lượt xem sức khỏe.
12. **Không có DBR cho Module 10 ngoài DBR-02, DBR-03**. Đề xuất: mỗi (quan hệ, quyền) tối đa một yêu cầu Chờ xác nhận; mỗi (người cao tuổi, kỳ) tối đa một bản tin không phải đính chính; mỗi ngoại lệ đón dùng tối đa một lần; tổng số người trong một khung thăm không vượt sức chứa tại thời điểm đăng ký.
13. **Đồng bộ các spec khác (2026-09-26)**: spec 000 (FR-031 ngoại lệ vòng đời, Q-118), 001 (bảng trạng thái: dòng Cho tạm vắng — bản ghi đón hoặc ngoại lệ; dòng Hoàn tất tiếp nhận — đủ DBR-02, sau checklist business-rules), 004 (FR-048: người đón có bản ghi đón hợp lệ), 005 (bảng trạng thái có mặt dòng Điểm danh về, User Story 2 kịch bản 3: thêm "Tự về", Q-125), 007 (Edge Case lượt thăm khi khoanh vùng, FR-065, Q-120), 009 (Điểm báo lại 17 đã giải quyết). Mỗi spec có mục "Cập nhật 2026-09-26 (đồng bộ với spec 012)", riêng 009 chỉ sửa Điểm báo lại 17.
14. **Yêu cầu đối với feature chưa viết**, cần đưa vào khi làm các spec đó: **010** — nhận lượt ở lại làm nguồn chi phí theo đêm (FR-042, FR-043), tính chi phí suất ăn của người ở lại theo đơn giá ngoài hợp đồng (FR-042 (c)), cung cấp chi phí tạm tính theo kỳ cho bản tin và bảng chi phí cho cổng (FR-046, FR-052) — đã đưa vào spec 010 ngày 2026-09-27 (feature 010 FR-005, FR-037, FR-038); **011** — nhận người ở lại có đăng ký ăn để tính suất (BR-M08-01), ghi suất ăn của họ làm nguồn chi phí, cung cấp thực đơn chung và chế độ ăn riêng (FR-046) — đã đưa vào spec 011 ngày 2026-09-27 (feature 011 FR-028, FR-034 (c), FR-043); tỷ lệ ăn và lượng nước cho bản tin (FR-052) do feature 005 cung cấp, không phải feature 011; **014** — cung cấp số hoạt động đã tham gia theo kỳ và buổi hoạt động cho lịch sinh hoạt (FR-046, FR-052).
15. **Mục 14.5 tách "tình trạng chăm sóc" khỏi "thông tin sức khỏe phù hợp"**, nhưng spec xếp kết quả công việc chăm sóc, tỷ lệ ăn, lượng nước vào loại sức khỏe (FR-046), vì chúng phản ánh tình trạng sức khỏe và chịu DBR-03 theo 19.3. Hệ quả: người thân không có bản đồng ý chỉ thấy phần chung. Chủ tài liệu cần xác nhận cách hiểu này, hoặc liệt kê rõ những mục "tình trạng chăm sóc" được xem mà không cần đồng ý.
16. **Các quyết định clarify lượt 3 (2026-09-26, sau checklist business-rules)** cần phản ánh vào tài liệu nguồn và thêm vào mục 24.2: Q-126 phiếu đăng ký người thân do người đại diện ký làm căn cứ bật quyền ban đầu (bổ sung 14.1, BR-M10-07); Q-127 thêm hoặc thôi người đại diện khi đã có người đại diện đi qua BR-M10-07 (bổ sung 2.4, 14.1); Q-128 giới hạn lượt ở lại và ý kiến người cùng phòng (bổ sung 14.4, tham số mới CFG-M10-10); Q-129 cách xét mức sự cố cho BR-M10-09 (bổ sung BR-M10-09).
17. **Tổng hợp mã Q đề xuất cần thêm vào mục 24.2** (2026-09-26): Q-118 vòng đời riêng BR-M10-07 (Điểm 1); Q-119, Q-120 lượt thăm tự duyệt và tự hủy khi khoanh vùng (Điểm 10); Q-121 người thực hiện quy trình đón (Điểm 3); Q-122 người phụ trách và mức ưu tiên phản hồi (Điểm 7); Q-123 xác nhận lượt ở lại và đếm đêm (Điểm 8); Q-124 duyệt thay bản tin (Điểm 9); Q-125 bán trú tự về (Điểm 4); Q-126 → Q-129 (Điểm 16); Q-130 tham số riêng CFG-M10-11, CFG-M10-12 (Điểm 5). Mỗi mã có câu hỏi và trả lời ở mục Clarifications. **(2026-09-27)** Q-118 → Q-130 đã được phản ánh vào tài liệu nguồn (14.1 → 14.4, 14.7, BR-M10-04, 07, 08, 09, BR-M05-11, 6.6, CFG-M10-04 → 12 ở Phụ lục 25; chú thích ¹⁸ → ²¹ và các dòng "Quy trình đón, ngoại lệ đón", "Người thân ở lại" của 4.4) và nằm ở mục 24.2. Các điểm 1, 3, 4, 5, 7, 8, 9, 10, 16 được giải quyết; điểm 2 (các ô ma trận ngoài Q-121, Q-123, Q-124, Q-127), 6, 11, 12, 15 vẫn mở.
