# Feature Specification: Chỉ số sức khỏe, cảnh báo và sự cố

**Feature Branch**: `007-health-alert-incident`

**Created**: 2026-09-26

**Status**: Draft

**Input**: User description: "Quản lý chỉ số sức khỏe, cảnh báo và sự cố theo docs/nghiep-vu.md Module 05 và Module 06 (mục 9, 10): lịch đo; ngưỡng cá nhân và mặc định với hai mức cảnh báo/nguy hiểm; quy tắc theo xu hướng; vòng đời cảnh báo có thời hạn tiếp nhận, leo thang và gộp trùng; sự cố ba mức; quy trình khẩn cấp với thẻ thông tin khẩn cấp và đối chiếu nguyện vọng cuối đời; sự cố ngã kéo theo theo dõi sau ngã; sự cố lây nhiễm với truy vết tiếp xúc tự động và khoanh vùng khu vực; phạm vi y tế được phép theo giấy phép."

## Clarifications

### Session 2026-09-26

- Q: Cảnh báo mức Khẩn cấp (vượt ngưỡng nguy hiểm, nâng mức, gộp) có tự động tạo sự cố khẩn cấp và chạy quy trình khẩn cấp (gồm báo người liên hệ chính) không? → A: Không. Hệ thống báo đồng thời cho Điều dưỡng phụ trách, Người phụ trách ca, Bác sĩ trực và Trưởng tầng; người tiếp nhận chọn "Kích hoạt khẩn cấp từ cảnh báo" khi cần; người liên hệ chính chỉ được báo khi có sự cố Khẩn cấp (FR-047a, đề xuất Q-67).
- Q: Ai được xác nhận, bổ sung, loại người khỏi danh sách tiếp xúc? → A: Điều dưỡng (trong phạm vi phân công) và Bác sĩ; Quản lý viện chỉ xem. Permission Matrix 4.4 cần thêm dòng "Danh sách tiếp xúc" (FR-060, FR-061, FR-080, đề xuất Q-68).
- Q: "Khu" khi khoanh vùng lây nhiễm là đơn vị nào? → A: Một hoặc nhiều tầng, hoặc toàn bộ một khu vực (2.2); một phòng riêng dùng "Đặt cách ly phòng" của feature 003 (FR-063, đề xuất Q-69).
- Q: Sự cố ngã có bắt buộc luôn ở mức Khẩn cấp không? → A: Không bắt buộc; mức mặc định là Khẩn cấp, người ghi được chọn mức thấp hơn kèm lý do; các tác động của BR-M05-07 áp cho mọi sự cố ngã bất kể mức (FR-042, đề xuất Q-70).
- Q: Quản lý viện có được tiếp nhận cảnh báo leo thang tới mình và ghi nhận sự cố không? → A: Được tiếp nhận cảnh báo ở Leo thang cấp 2 rồi giao người phụ trách; được ghi nhận sự cố và kích hoạt khẩn cấp; không xử lý, không đóng cảnh báo hay sự cố (FR-029, FR-036, FR-041, FR-080, đề xuất Q-71).

### Session 2026-09-26 (lượt 2, sau checklist business-rules)

- Q: Thông báo gửi người ngoài nhóm chăm sóc (người liên hệ chính khi khẩn cấp; nhân viên, người thân trong danh sách tiếp xúc) chưa chắc có bản đồng ý chia sẻ dữ liệu; nội dung được nêu những gì? → A: Người liên hệ chính không có bản đồng ý bao gồm mình chỉ nhận "có tình huống khẩn cấp liên quan [họ tên người cao tuổi], đề nghị liên hệ viện" và thời điểm; chi tiết sức khỏe chỉ khi có bản đồng ý. Thông báo cho người tiếp xúc chỉ nêu "có thể đã tiếp xúc tại [khu vực, khoảng thời gian], đề nghị làm theo hướng dẫn", không nêu danh tính hay tình trạng của người nghi nhiễm (FR-047, FR-062, đề xuất Q-72).
- Q: Khi sự cố bị hủy vì ghi nhầm hoặc bị đính chính đổi loại, các tác động tự động đã tạo xử lý thế nào? → A: Sự cố hủy chuyển Đã hủy (trạng thái cuối); tác động không tự thu hồi, hệ thống gửi Điều dưỡng phụ trách và Bác sĩ danh sách để đóng bằng lệnh riêng; đổi loại sang Ngã (hoặc lây nhiễm) tạo ngay tác động của loại mới; đổi loại khỏi Ngã xử lý như hủy (FR-045, FR-046a, đề xuất Q-73).
- Q: Khi có khẩn cấp mà ca hiện tại không có Bác sĩ trực, ai nhận thông báo thay? → A: Mọi Bác sĩ đang hoạt động của cơ sở nhận song song; sự cố hoặc cảnh báo gắn dấu "không có Bác sĩ trực tại thời điểm" và Quản lý viện được báo (FR-047b, đề xuất Q-74).
- Q: Một công việc đo Bắt buộc từ lịch đo bị bỏ sinh một hay hai cảnh báo? → A: Một: yêu cầu cảnh báo quá hạn của feature 005 cho công việc đo từ lịch đo được ghi với khóa "bỏ lỡ lần đo theo lịch: <chỉ số>"; Không thực hiện hoặc Quá hạn cuối ca gộp vào; mức là mức cao nhất của các nguồn (FR-028, FR-039b, đề xuất Q-75).
- Q: Khi nguồn của một cảnh báo đã Chuyển sự cố được xử lý (ví dụ liều được đính chính Đã dùng), sự cố xử lý thế nào? → A: Cảnh báo giữ Chuyển sự cố; hệ thống thêm diễn biến "nguồn đã được xử lý" vào sự cố và báo người xử lý; sự cố không tự đóng, người có quyền đóng kèm kết quả (FR-032, đề xuất Q-76).

### Cập nhật 2026-09-26 (đồng bộ với spec 008)

Spec 008 đã chốt khi clarify (Q-78): từ giờ bắt đầu ca sau, nếu bàn giao chưa được xác nhận, Người phụ trách ca sau tạm nhận cảnh báo và sự cố đang mở của người ca trước đã hết ca. FR-037 và kịch bản 10 của User Story 3 được sửa cho khớp; việc chuyển chính thức khi xác nhận giữ nguyên. Theo Q-94, trong lúc tạm nhận, người tạm nhận thay Điều dưỡng phụ trách ở FR-033 và FR-035. Theo Q-90, khi tầng chưa có Trưởng tầng, Người phụ trách ca của tầng (không phải Quản lý viện) là người tạm nhận thay Trưởng tầng, nên không mâu thuẫn với Q-71.

### Cập nhật 2026-09-26 (đồng bộ với spec 009)

Spec 009 đã chốt khi clarify, và spec này được sửa cho khớp: (1) Q-102 — "Xác nhận đã nhận" thông báo Khẩn cấp không phải là tiếp nhận cảnh báo/sự cố; thao tác gộp "Xác nhận và tiếp nhận" gọi lệnh Tiếp nhận (bảng FR-029) hoặc Tiếp nhận xử lý (bảng FR-045) như hai bản ghi riêng. (2) Q-110 — yêu cầu thông báo leo thang khai "không thay" để giữ quy tắc bỏ qua cấp không có người (FR-033). (3) Q-112 — người thân trong danh sách tiếp xúc nhận theo nhóm "người thân được nêu trong bản ghi nguồn", chỉ phần loại "chung" (FR-062). (4) Spec 009 FR-011: khi một yêu cầu Khẩn cấp không còn người nhận nào, mọi Quản lý viện và Trưởng tầng đang hoạt động nhận thay; điều này áp cả với cảnh báo Khẩn cấp (FR-047a) như một lưới an toàn, cùng tinh thần với FR-047b. (5) Q-109 — Bác sĩ trực có tài khoản đang Khóa tạm vẫn nhận thông báo Khẩn cấp. Dữ liệu "người được thông báo" (FR-040, FR-048b) lấy từ spec 009 FR-035, gồm cả xác nhận qua cuộc gọi, xác nhận muộn và "Không liên lạc được" (Q-100, Q-104).

### Cập nhật 2026-09-26 (đồng bộ với spec 012)

Spec 012 đã chốt khi clarify (Q-120): lượt thăm Đã duyệt của người cao tuổi trong vùng vừa khoanh vùng được feature 012 tự chuyển Hủy (lý do "khu đang khoanh vùng") và báo người đăng ký; gỡ vùng không khôi phục lượt. Edge Case "Lượt thăm đã được duyệt trước khi khoanh vùng" và FR-065 được sửa cho khớp. Sau checklist consistency của 012: FR-060 (d) gồm cả người đi cùng thực tế vào của lượt thăm (feature 012 FR-038).

### Cập nhật 2026-09-27 (đồng bộ với spec 011)

Spec 011 và tài liệu nguồn (BR-M08-05, BR-M08-14) đã chốt:
- Khi có cảnh báo sụt cân, spec này gửi sự kiện cho feature 011 để tạo yêu cầu dinh dưỡng viên xem lại chế độ ăn (FR-039a).
- Sự cố do phục vụ suất có thành phần gây dị ứng luôn có loại "phục vụ sai suất ăn"; nguồn "phục vụ sai suất ăn" khi chưa ăn (mức Trung bình), "ăn uống" khi đã ăn (mức theo triệu chứng, chọn thấp hơn mặc định phải có lý do theo FR-042) (FR-040).
- Cảnh báo "xung đột dị ứng với chế độ ăn, thực đơn" do feature 011 yêu cầu có mức Trung bình.

### Cập nhật 2026-09-27 (đồng bộ với spec 013)

Spec 013 và tài liệu nguồn (9.1, BR-M12-02, Q-148; Q-160) đã chốt:
- Nguồn phát sinh sự cố có thêm "đồ gửi"; danh mục loại sự cố có thêm "đồ gửi thất lạc" và "đồ gửi hư hỏng", mức mặc định Trung bình (FR-040, FR-042). Sự cố loại này luôn có nguồn "đồ gửi", kể cả khi đồ mất lúc người cao tuổi đang ở ngoài viện.
- Sự cố loại "đồ gửi thất lạc", "đồ gửi hư hỏng" **không** tạo yêu cầu đánh giá lại "sau sự cố" (FR-043 (b) được sửa), vì không liên quan tình trạng sức khỏe. Các tác động khác của FR-043 giữ nguyên.
- Feature 013 được liên kết một sự cố đã ghi trực tiếp (ví dụ do Nhân viên chăm sóc phát hiện) thay vì yêu cầu tạo sự cố mới; khi đồ thất lạc được tìm thấy, feature 013 ghi diễn biến "đã tìm thấy" vào sự cố, sự cố vẫn đóng theo FR-045.
- Khi bản ghi bàn giao đồ gửi đã tạo sự cố bị "Hủy ghi nhận" (feature 013 FR-011a), feature 013 yêu cầu chuyển sự cố đó Đã hủy với lý do "ghi nhầm đồ gửi" (FR-046b mới).
- Sự cố đồ gửi còn mở vẫn chặn Kết thúc lưu trú qua điều kiện (d) của feature 004, dù đồ Thất lạc không tính vào điều kiện đồ gửi (Q-148).

### Cập nhật 2026-09-27 (đồng bộ với spec 014)

Spec 014 và tài liệu nguồn (BR-M04-15, BR-M04-17, BR-M04-22, BR-M05-11, Q-164, Q-165, Q-172) đã chốt:
- Loại sự cố "đi lạc hoặc không trở về" có mức mặc định Khẩn cấp (FR-042 được sửa). Feature 014 yêu cầu tạo sự cố loại này, nguồn "hoạt động ngoài viện", khi "Báo thiếu người" trong chuyến hoặc khi kết thúc điểm danh về còn người thiếu, và ghi diễn biến "đã tìm thấy, đã trở về" khi người đó được điểm danh về muộn; sự cố không tự đóng.
- Cảnh báo "nguy cơ cô lập" (mức Nhẹ) gộp theo người cao tuổi (FR-028 được sửa); người nhận là trưởng tầng của tầng người đó thay cho điều dưỡng phụ trách (BR-M04-22); feature 014 ghi diễn biến "đã tham gia hoạt động nhóm" khi người đó có mặt ở một hoạt động nhóm.
- Khi khoanh vùng, feature 014 tự hủy buổi trong vùng và đăng ký của người trong vùng, không khôi phục khi gỡ (FR-065 được sửa, Q-164).
- Spec này cung cấp cho feature 014 sự kiện gắn, gỡ dấu "nghi nhiễm" (FR-060): người mang dấu không được đăng ký, điểm danh có mặt ở hoạt động nhóm và chuyến đi; đăng ký nhóm đã có tự hủy (Q-165).

### Cập nhật 2026-09-27 (đồng bộ với spec 016)

Spec 016 và tài liệu nguồn (9.2, 18.3, 19.3, Q-199, Q-202) đã chốt:
- Mỗi loại trong danh mục loại sự cố có dấu "không thuộc sức khỏe" do Quản lý viện đặt, mặc định bật cho đồ gửi thất lạc, đồ gửi hư hỏng, cơ sở vật chất; Hành chính chỉ thấy số liệu sự cố của các loại mang dấu (FR-042 được sửa).
- Báo cáo sức khỏe đếm cả leo thang và thời gian tiếp nhận xử lý của sự cố, tính riêng với cảnh báo; mốc tạo, tiếp nhận của cảnh báo theo 18.3 (FR-081 được sửa).

### Cập nhật 2026-09-28 (đồng bộ với spec 016, rà chéo)

FR-081 được bổ sung định nghĩa "lần đo" (bản ghi đã xác nhận; bản ghi "chờ đo lại" được thay thì tính lần đo lại), "vượt ngưỡng" hai mức theo ngưỡng hiệu lực lúc đo, và mức dùng để xếp (số lượng theo mức hiện hành; thời gian tiếp nhận theo mức lúc tiếp nhận). Đây là căn cứ cho feature 016 FR-040 → FR-042. Không có thay đổi về hành vi của spec này.

### Cập nhật 2026-09-28 (đồng bộ với góp ý nghiệp vụ Q-213, Q-216, Q-218)

Tài liệu nguồn (5.2, 9.5, BR-M05-15, 16, UC-83, 84, Phụ lục 27 dòng "Dấu nguy kịch, xác nhận lại nguyện vọng", DBR-34, BF-17) đã chốt quy trình khi người cao tuổi nguy kịch. Spec này thêm: Phạm vi 12; User Story 11; mục G1 (FR-050a → FR-050g); bảng trạng thái dấu nguy kịch; dòng phân nhóm dữ liệu; dòng quyền ở FR-080; dòng giao tiếp ở FR-083; Key Entities; SC-015. FR-028 (khóa loại "nguy kịch" không gộp) và FR-048 (thẻ hiển thị phiếu nguyện vọng có cấu trúc của feature 001 FR-023a) được sửa.

## Phạm vi

**Trong phạm vi** (Module 05 mục 9.1 → 9.7; Module 06 mục 10.1 → 10.5; UC-16 phần khởi phát từ sự cố, UC-32 → UC-38):

1. Danh mục chỉ số (huyết áp, nhịp tim, nhiệt độ, SpO2, đường huyết, cân nặng, chỉ số khác) với khoảng hợp lệ vật lý (10.1, CFG-M06-03).
2. Lịch đo riêng của từng người cao tuổi do bác sĩ thiết lập, và lịch đo tự động của theo dõi sau ngã, theo dõi người tiếp xúc; lịch đo là nguồn sinh công việc đo chỉ số ở feature 005 (10.1, BR-M04-01, BR-M05-07, BR-M05-11).
3. Lưu bản ghi chỉ số, chặn giá trị phi vật lý, đo lại xác nhận giá trị nguy hiểm, so ngưỡng, đính chính (UC-32, BR-M04-10, BR-M06-03, 04, 06).
4. Ngưỡng mặc định của cơ sở và ngưỡng cá nhân theo phiên bản, hai mức cảnh báo và nguy hiểm, mỗi mức có cận dưới và cận trên; nhắc thiết lập ngưỡng cá nhân (10.3, UC-33, BR-M06-01, 02).
5. Quy tắc theo xu hướng: sụt cân, bỏ lỡ lần đo theo lịch, mất ngủ nhiều đêm liên tiếp; tham số dùng chung với quy tắc từ chối thuốc (BR-M05-04).
6. Cảnh báo: tạo từ mọi nguồn, gộp trùng, thời hạn tiếp nhận theo mức, leo thang, đề xuất nâng mức, xử lý, đóng, chuyển thành sự cố, chuyển người phụ trách sau bàn giao (9.1, 9.3, 9.4, UC-34, UC-38, BR-M05-01 → 03, 05, DBR-18).
7. Sự cố ba mức Nhẹ, Trung bình, Khẩn cấp: ghi nhận, tiếp nhận, diễn biến, theo dõi, đóng, đính chính (9.2 → 9.4, UC-35, BR-M05-08, 09).
8. Quy trình khẩn cấp: thẻ thông tin khẩn cấp, đối chiếu nguyện vọng cuối đời trước biện pháp hồi sức, thông báo đồng thời, chuyển viện kèm bản tóm tắt chuyển viện (9.5, UC-36, UC-16, BR-M05-06, 13, 14, DBR-19).
9. Sự cố ngã kéo theo yêu cầu đánh giá lại, yêu cầu xem xét kế hoạch chăm sóc và lịch theo dõi sau ngã (BR-M05-07).
10. Sự cố lây nhiễm: danh sách tiếp xúc tự đề xuất, xác nhận, theo dõi người tiếp xúc; khoanh vùng và gỡ khoanh vùng (9.6, UC-37, BR-M05-10 → 12).
11. Phạm vi y tế: giấy phép hoạt động của cơ sở làm căn cứ cho BR-M06-05; ghi nhận khám và điều trị theo phạm vi được phép (10.2, 10.4).
12. Nguy kịch: dấu nguy kịch, cảnh báo "nguy kịch – thực hiện nguyện vọng cuối đời", xác nhận lại nguyện vọng với gia đình, thực hiện lựa chọn (9.5, UC-83, UC-84, BR-M05-15, 16, Q-213, Q-218; bổ sung 2026-09-28).

**Ngoài phạm vi** (spec này chỉ **cung cấp** dữ liệu hoặc **được kích hoạt** bởi feature sở hữu):

- Quy tắc dùng chung (nhóm dữ liệu, lý do bắt buộc, đính chính, nhật ký, tham số, "hoặc toàn bộ, hoặc không", yêu cầu phê duyệt): feature 000 — spec này kế thừa, không lặp lại.
- Nguyện vọng chăm sóc cuối đời, dị ứng, bệnh nền, cờ nguy cơ, yêu cầu đánh giá lại, tập trạng thái người cao tuổi và lệnh Chuyển viện (điều kiện ở 5.6): feature 001. Nội dung lệnh Chuyển viện và lượt vắng loại Bệnh viện: feature 004.
- Kiểm tra quyền, phạm vi dữ liệu, ngoại lệ ghi nhận sự cố ngoài phạm vi (feature 002 FR-044a), danh mục nghiệp vụ chuyên môn, kiểm tra giấy phép khi thực hiện (feature 002 FR-036 → 038) và cảnh báo giấy phép sắp hết hạn (feature 002 FR-039): feature 002. Spec này sở hữu **dữ liệu** giấy phép hoạt động của cơ sở.
- Giấy phép hành nghề, đào tạo của nhân viên: feature 008 (UC-49).
- Lịch sử vị trí, trạng thái cách ly phòng, chặn phân bổ giường trong vùng khoanh, công việc khử khuẩn: feature 003 (FR-008, FR-009, FR-036, FR-045, FR-046).
- Sinh công việc đo chỉ số từ lịch đo, ghi kết quả công việc, phát hiện uống nước thiếu, ăn kém kéo dài, tâm trạng tiêu cực, công việc Bắt buộc quá hạn, yêu cầu xem xét kế hoạch chăm sóc: feature 005. Spec này nhận yêu cầu tạo và đóng cảnh báo.
- Phát hiện liều Bỏ lỡ, phản ứng thuốc, từ chối thuốc liên tiếp, xung đột dị ứng với đơn, chưa đối chiếu thuốc; danh sách thuốc đang dùng: feature 006. Spec này nhận yêu cầu tạo và đóng cảnh báo.
- Bàn giao ca, phân công, ca trực, nhiệm vụ Bác sĩ trực, Người phụ trách ca, Điều dưỡng phụ trách: feature 008.
- Kênh gửi, xác nhận đã xem, nhắc gọi điện khi thông báo khẩn cấp chưa được xác nhận, giờ yên tĩnh (BR-M13-01 → 05): feature 009. Spec này xác định **sự kiện** và **người nhận**.
- Xung đột dị ứng với chế độ ăn, phục vụ sai suất ăn (BR-M08-02, BR-M08-14): feature 011 phát hiện và yêu cầu spec này tạo cảnh báo hoặc sự cố.
- Hồ sơ người thân, người liên hệ chính, quyền của người thân, lượt thăm, chặn đăng ký thăm, bản tin định kỳ: feature 012.
- Hoạt động, điểm danh, hoạt động ngoài viện, chặn đăng ký hoạt động trong vùng khoanh, phát hiện thiếu người khi trở về: feature 014.
- Báo cáo, dashboard: feature 016. Spec này cung cấp dữ liệu (18.3, 18.5).
- Bệnh án điện tử chuyên sâu (1.2) và báo cáo bắt buộc gửi cơ quan y tế ngoài hệ thống: không thuộc hệ thống; spec chỉ ghi nhận việc đã báo.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ghi nhận chỉ số, so ngưỡng hai mức và tạo cảnh báo (Priority: P1)

Nhân viên chăm sóc hoặc điều dưỡng đo chỉ số theo công việc trong checklist (hoặc đo ngoài lịch khi thấy cần) và ghi giá trị. Hệ thống chặn giá trị phi vật lý, lưu bản ghi với người đo và thời điểm, rồi so ngay với ngưỡng đang áp dụng cho người cao tuổi đó: vượt ngưỡng cảnh báo thì tạo cảnh báo mức Trung bình, vượt ngưỡng nguy hiểm thì yêu cầu đo lại để xác nhận (trừ khi người đo chọn "xử lý ngay") rồi tạo cảnh báo mức Khẩn cấp.

**Why this priority**: Đây là nguồn cảnh báo sức khỏe chính của hệ thống (10.3, BR-M04-10); bỏ sót một giá trị nguy hiểm có thể gây hại trực tiếp cho người cao tuổi.

**Independent Test**: Cho người cao tuổi A có ngưỡng mặc định SpO2 (cảnh báo dưới 94, nguy hiểm dưới 90); lần lượt ghi 101, 96, 92, 88 rồi đo lại 87; kiểm tra 101 bị chặn, 96 không tạo cảnh báo, 92 tạo cảnh báo Trung bình, 88 chuyển "chờ đo lại" và lần đo lại 87 tạo cảnh báo Khẩn cấp.

**Acceptance Scenarios**:

1. **Given** công việc "đo SpO2" của A Đến hạn và khoảng hợp lệ vật lý của SpO2 là 50–100 (CFG-M06-03), **When** nhân viên chăm sóc S ghi 101, **Then** hệ thống chặn, nêu khoảng hợp lệ, và công việc giữ nguyên trạng thái (BR-M06-04).
2. **Given** A không có ngưỡng cá nhân SpO2 và ngưỡng mặc định là cảnh báo dưới 94, nguy hiểm dưới 90, **When** S ghi 92, **Then** bản ghi được lưu với người đo S, thời điểm đo, công việc nguồn; hệ thống tạo cảnh báo Trung bình "SpO2 vượt ngưỡng cảnh báo" cho điều dưỡng phụ trách A với hạn tiếp nhận CFG-M05-01 (mặc định \[15 phút\]) (BR-M06-03).
3. **Given** như trên, **When** S ghi 88, **Then** bản ghi ở trạng thái "chờ đo lại", hệ thống yêu cầu S đo lại và chưa tạo cảnh báo; **When** S ghi lần đo lại 87, **Then** lần đo lại được xác nhận và hệ thống tạo cảnh báo Khẩn cấp; công việc đo đóng Hoàn thành trỏ tới bản ghi đã xác nhận (BR-M06-04, feature 005 FR-034).
4. **Given** S ghi 88 và chọn "xử lý ngay", **When** lưu, **Then** không yêu cầu đo lại; bản ghi được xác nhận ngay và cảnh báo Khẩn cấp được tạo (BR-M06-04).
5. **Given** bản ghi 88 đang "chờ đo lại", **When** qua CFG-M06-04 (đề xuất, mặc định \[10 phút\]) mà chưa có lần đo lại, **Then** hệ thống xác nhận bản ghi với nhãn "chưa đo lại" và tạo cảnh báo Khẩn cấp (FR-017).
6. **Given** lần đo đầu 88 "chờ đo lại", **When** lần đo lại là 95, **Then** lần đo lại được xác nhận, không tạo cảnh báo; lần đo đầu vẫn được lưu với nhãn "đã đo lại", xem được trong lịch sử.
7. **Given** A có ngưỡng cá nhân SpO2 (cảnh báo dưới 90, nguy hiểm dưới 85) đang hiệu lực, **When** S ghi 88, **Then** hệ thống so theo ngưỡng cá nhân: tạo cảnh báo Trung bình, không yêu cầu đo lại (BR-M06-01).
8. **Given** huyết áp của A có ngưỡng riêng cho tâm thu và tâm trương, **When** S ghi 150/95 mà tâm thu vượt ngưỡng cảnh báo còn tâm trương vượt ngưỡng nguy hiểm, **Then** hệ thống xếp theo thành phần nặng nhất (nguy hiểm) và yêu cầu đo lại.
9. **Given** nhân viên hành chính H, **When** H mở danh sách chỉ số của A, **Then** hệ thống chặn (19.3, 4.4 dòng "Chỉ số sức khỏe": HC —).
10. **Given** lần đo lại SpO2 87 của A được xác nhận và cảnh báo Khẩn cấp được tạo, **When** cảnh báo được tạo, **Then** Điều dưỡng phụ trách, Người phụ trách ca, Bác sĩ trực và Trưởng tầng được báo đồng thời; không có sự cố nào được tạo và người liên hệ chính chưa được báo; **When** D chọn "Kích hoạt khẩn cấp từ cảnh báo", **Then** sự cố Khẩn cấp được tạo và quy trình khẩn cấp bắt đầu (FR-047a, Q-67).

---

### User Story 2 - Bác sĩ thiết lập ngưỡng và lịch đo (Priority: P1)

Bác sĩ thiết lập ngưỡng mặc định của cơ sở cho từng chỉ số và ngưỡng cá nhân cho từng người cao tuổi. Mỗi ngưỡng có hai mức (cảnh báo, nguy hiểm), mỗi mức có cận dưới và cận trên, có ngày giờ hiệu lực và được lưu thành phiên bản; thay đổi ngưỡng tạo phiên bản mới, không sửa phiên bản đang dùng. Bác sĩ cũng đặt lịch đo riêng (chỉ số, tần suất, thời gian) để hệ thống sinh công việc đo. Người lưu trú quá CFG-M06-01 mà chưa có ngưỡng cá nhân thì bác sĩ được nhắc.

**Why this priority**: Không có ngưỡng thì không có cảnh báo chỉ số; không có lịch đo thì không có công việc đo (10.1, 10.3).

**Independent Test**: Tạo ngưỡng mặc định nhiệt độ; tạo ngưỡng cá nhân cho A có hiệu lực từ ngày mai; ghi một giá trị hôm nay và một giá trị ngày mai; kiểm tra hôm nay so theo ngưỡng mặc định, ngày mai theo ngưỡng cá nhân, và phiên bản cũ vẫn xem được.

**Acceptance Scenarios**:

1. **Given** bác sĩ B, **When** B lưu ngưỡng cá nhân nhiệt độ cho A với cận dưới nguy hiểm 35,0, cận dưới cảnh báo 36,0, cận trên cảnh báo 37,8, cận trên nguy hiểm 39,0, hiệu lực từ 08:00 ngày 01/10, **Then** phiên bản được lưu với người thiết lập B, thời điểm, trạng thái Chờ hiệu lực; tới 08:00 ngày 01/10 phiên bản chuyển Hiệu lực (10.3).
2. **Given** B nhập cận dưới cảnh báo 34,5 thấp hơn cận dưới nguy hiểm 35,0, **When** lưu, **Then** hệ thống chặn vì khoảng cảnh báo phải nằm trong khoảng nguy hiểm (FR-021).
3. **Given** A đang có phiên bản ngưỡng nhiệt độ số 1 Hiệu lực, **When** B tạo phiên bản số 2 hiệu lực ngay, kèm lý do, **Then** phiên bản 1 chuyển Hết hiệu lực tại cùng thời điểm, phiên bản 2 Hiệu lực; bản ghi chỉ số đã so theo phiên bản 1 giữ nguyên kết quả so (FR-023).
4. **Given** A Đang lưu trú từ 8 ngày trước (CFG-M06-01, mặc định \[7 ngày\]) và chưa có phiên bản ngưỡng cá nhân Hiệu lực nào, **When** Bộ lập lịch kiểm tra hằng ngày, **Then** các Bác sĩ nhận nhắc "chưa có ngưỡng cá nhân" cho A và A xuất hiện trong danh sách "chưa có ngưỡng cá nhân" (BR-M06-02).
5. **Given** B đặt lịch đo huyết áp cho A lúc 07:00 và 17:00 hằng ngày từ 01/10, **When** lưu, **Then** lịch đo Hiệu lực từ 01/10 và feature 005 sinh công việc "đo huyết áp" mức Bắt buộc cho các mốc đó (10.1, BR-M04-01).
6. **Given** lịch đo huyết áp của A đang Hiệu lực, **When** B đổi sang 3 lần/ngày từ ngày mai kèm lý do, **Then** lịch cũ chuyển Đã ngừng từ thời điểm đó, lịch mới trỏ về lịch cũ; feature 005 chỉ hủy và sinh lại công việc Chưa đến hạn (feature 005 FR-022).
7. **Given** điều dưỡng D, **When** D thử tạo ngưỡng cá nhân cho A, **Then** hệ thống chặn (4.4 dòng "Ngưỡng cảnh báo": ĐD X).

---

### User Story 3 - Tiếp nhận, leo thang và gộp cảnh báo (Priority: P1)

Mọi cảnh báo (từ chỉ số, xu hướng, công việc quá hạn, thuốc, ăn uống, nhân viên phát hiện) đi qua một vòng đời chung: Mới → Đã tiếp nhận → Đang xử lý → Đã đóng hoặc Chuyển sự cố. Mỗi cảnh báo có hạn tiếp nhận theo mức; quá hạn thì Bộ lập lịch leo thang lên cấp trên và ghi lịch sử. Cùng người cao tuổi đã có cảnh báo cùng loại đang mở thì cảnh báo mới được gộp vào, tăng số lần, không tạo cảnh báo mới. Cảnh báo Trung bình lặp lại nhiều lần trong 24 giờ thì hệ thống đề xuất nâng lên Khẩn cấp. Cảnh báo đang mở khi hết ca đi vào bàn giao và chuyển người phụ trách khi ca sau xác nhận.

**Why this priority**: Cảnh báo không ai tiếp nhận là rủi ro an toàn lớn nhất; gộp trùng tránh "ngập cảnh báo" làm nhân viên bỏ qua tín hiệu thật (BR-M05-01, 02).

**Independent Test**: Dùng đồng hồ giả lập; tạo cảnh báo Trung bình cho A không ai tiếp nhận; kiểm tra leo thang ở phút 15, 30, 45 lên đúng cấp; tạo thêm hai lần cùng loại trong 24 giờ và kiểm tra số lần gộp, đề xuất nâng mức.

**Acceptance Scenarios**:

1. **Given** cảnh báo Trung bình "huyết áp vượt ngưỡng cảnh báo" của A tạo lúc 09:00, điều dưỡng phụ trách là D, **When** tới 09:15 (CFG-M05-01, mặc định \[15 phút\]) chưa ai tiếp nhận, **Then** cảnh báo chuyển Leo thang cấp 1, Trưởng tầng và Bác sĩ trực được thông báo, hạn tiếp nhận mới là 09:30; lịch sử ghi lần leo thang (BR-M05-01, UC-38).
2. **Given** như trên, **When** tới 09:30 vẫn chưa tiếp nhận, **Then** cảnh báo leo thang cấp 2, Quản lý viện được thông báo; **When** tới 09:45, **Then** không leo thang thêm, cảnh báo giữ dấu "đã leo thang tối đa" trên dashboard (FR-034); **When** Quản lý viện Q tiếp nhận rồi giao cho điều dưỡng D2 cùng tầng kèm ghi chú, **Then** người phụ trách là D2, D2 được thông báo; Q không đóng được cảnh báo (FR-036, Q-71).
3. **Given** cảnh báo ở Leo thang cấp 1, **When** Trưởng tầng T tiếp nhận lúc 09:20, **Then** cảnh báo chuyển Đã tiếp nhận, người phụ trách là T, việc leo thang dừng; thời gian từ tạo đến tiếp nhận (20 phút) được lưu cho báo cáo (18.3).
4. **Given** A có cảnh báo "huyết áp vượt ngưỡng cảnh báo" đang Đã tiếp nhận, **When** lần đo 11:00 lại vượt ngưỡng cảnh báo, **Then** không tạo cảnh báo mới; cảnh báo cũ tăng số lần lên 2 và thêm bản ghi chỉ số vào danh sách nguồn (BR-M05-02, DBR-18).
5. **Given** A có cảnh báo Trung bình "huyết áp" đang mở, **When** lần đo mới vượt ngưỡng nguy hiểm và được xác nhận, **Then** cảnh báo được gộp, mức nâng lên Khẩn cấp, người nhận theo mức Khẩn cấp được thông báo ngay (FR-030).
6. **Given** cảnh báo Trung bình "huyết áp" của A đã có 3 lần trong 24 giờ (CFG-M05-02, mặc định \[3 lần / 24 giờ\]), **When** lần thứ 3 được gộp, **Then** hệ thống đề xuất nâng lên Khẩn cấp cho người phụ trách và Bác sĩ trực; **When** D từ chối đề xuất, **Then** hệ thống bắt buộc lý do (BR-M05-03).
7. **Given** cảnh báo của A đang Đang xử lý, **When** D đóng mà không ghi kết quả xử lý, **Then** hệ thống chặn; **When** D ghi kết quả "đã cho nghỉ, đo lại 11:30 bình thường", **Then** cảnh báo Đã đóng (9.4).
8. **Given** cảnh báo của A đã Đã đóng, **When** lần đo mới vượt ngưỡng, **Then** hệ thống tạo cảnh báo mới (không gộp vào cảnh báo đã đóng).
9. **Given** hai lần đo vượt ngưỡng của A được lưu gần như đồng thời từ hai thiết bị, **When** cả hai gửi, **Then** chỉ có một cảnh báo đang mở với số lần 2 (DBR-18).
10. **Given** cảnh báo của A đang Đã tiếp nhận bởi D ở ca ngày, **When** ca đêm xác nhận bàn giao (feature 008), **Then** người phụ trách chuyển sang điều dưỡng phụ trách A ở ca đêm; trạng thái giữ nguyên (BR-M05-05, BR-M09-08). Nếu tới giờ bắt đầu ca đêm mà bàn giao chưa được xác nhận, cảnh báo được chuyển tạm sang Người phụ trách ca đêm với dấu "tạm nhận, chờ xác nhận bàn giao" cho tới lúc xác nhận (FR-037, feature 008 Q-78).
11. **Given** công việc Bắt buộc "xoay trở" của A có cảnh báo quá hạn, **When** feature 005 báo công việc được ghi Hoàn thành với thời điểm thực hiện trong khung, **Then** cảnh báo chuyển Đã đóng, người đóng là Hệ thống, kết quả "nguồn đã được xử lý: đã thực hiện đúng khung" (FR-032).
12. **Given** nhân viên chăm sóc S, **When** S thử tiếp nhận cảnh báo của A, **Then** hệ thống chặn; S chỉ xem được cảnh báo của người mình được phân công (4.4 dòng "Xử lý cảnh báo": CS X).

---

### User Story 4 - Ghi nhận và xử lý sự cố ba mức (Priority: P1)

Bất kỳ nhân viên nào phát hiện sự cố đều ghi nhận được, kể cả với người cao tuổi ngoài phạm vi phân công. Sự cố có người cao tuổi, thời gian, địa điểm, hoạt động đang diễn ra, người phát hiện, mô tả, mức độ, xử lý ban đầu và người được thông báo. Sự cố đi qua các bước Ghi nhận → Đánh giá → Xử lý → Thông báo → Theo dõi → Đóng; mọi bổ sung là bản ghi diễn biến ghi thêm, không sửa nội dung đã ghi. Sự cố chỉ đóng khi có kết quả xử lý; mức Trung bình trở lên cần điều dưỡng hoặc bác sĩ đóng.

**Why this priority**: Sự cố là hồ sơ pháp lý và chuyên môn của viện; cần ghi đủ, đúng lúc, không bị sửa (9.2, 1.5 nhóm 3).

**Independent Test**: Nhân viên bếp ghi sự cố Nhẹ "bỏ bữa" cho người cao tuổi ngoài phạm vi; điều dưỡng ghi sự cố Trung bình "từ chối thuốc"; kiểm tra phạm vi hiển thị, thông báo theo mức, và điều kiện đóng.

**Acceptance Scenarios**:

1. **Given** nhân viên bếp K không được phân công cho A, **When** K ghi nhận sự cố cho A, **Then** K chọn được A và chỉ thấy họ tên, ảnh, phòng/giường, không thấy thông tin sức khỏe; nhật ký đánh dấu "ngoài phạm vi" (feature 002 FR-044a).
2. **Given** S ghi sự cố "khó ngủ" mức Nhẹ cho A, **When** lưu, **Then** sự cố ở trạng thái Mới, điều dưỡng phụ trách A được thông báo trong ứng dụng, hạn tiếp nhận là hết ca hiện tại (9.3, BR-M13-01).
3. **Given** D ghi sự cố "từ chối thuốc" mức Trung bình cho A, **When** lưu, **Then** thông báo gửi qua ứng dụng và tin nhắn cho điều dưỡng phụ trách và Người phụ trách ca, hạn tiếp nhận CFG-M05-01; feature 001 tạo yêu cầu đánh giá lại lý do "sau sự cố" (BR-M01-02).
4. **Given** sự cố Trung bình của A đang Đang xử lý, **When** Trưởng tầng T (không phải điều dưỡng) đóng sự cố, **Then** hệ thống chặn; **When** D đóng kèm kết quả xử lý, **Then** sự cố Đã đóng (BR-M05-09).
5. **Given** sự cố Nhẹ đang Đang theo dõi, **When** T đóng kèm kết quả, **Then** được phép.
6. **Given** sự cố đã ghi, **When** người ghi muốn sửa mô tả, **Then** không có thao tác sửa; người ghi tạo bản ghi diễn biến bổ sung hoặc bản đính chính có lý do, bản gốc giữ nguyên (1.5 nhóm 3).
7. **Given** D xử lý sự cố Trung bình và thấy tình trạng xấu đi, **When** D nâng mức lên Khẩn cấp kèm lý do, **Then** quy trình khẩn cấp được kích hoạt (User Story 5).
8. **Given** cảnh báo Trung bình của A đang Đang xử lý, **When** D chọn "Chuyển sự cố", **Then** hệ thống tạo sự cố liên kết cảnh báo, mức mặc định bằng mức cảnh báo, và cảnh báo chuyển Chuyển sự cố (9.4).
9. **Given** người thân N của A có bản đồng ý chia sẻ dữ liệu đang hiệu lực bao gồm N, **When** N xem cổng người thân, **Then** N thấy sự cố của A theo quyền; người thân không có bản đồng ý thì không thấy (4.4 chú thích ¹).

---

### User Story 5 - Quy trình khẩn cấp: thẻ thông tin khẩn cấp, đối chiếu nguyện vọng, chuyển viện (Priority: P1)

Khi một sự cố Khẩn cấp được tạo (trực tiếp, nâng mức, hoặc từ cảnh báo), hệ thống hiển thị ngay cho người xử lý thẻ thông tin khẩn cấp: nguyện vọng chăm sóc cuối đời, dị ứng đang hiệu lực, thuốc đang dùng, bệnh nền, cờ nguy cơ, người liên hệ chính và số điện thoại. Đồng thời hệ thống thông báo song song cho bác sĩ trực, trưởng tầng, quản lý và người liên hệ chính. Nếu người cao tuổi có nguyện vọng cuối đời đã ghi nhận, người xử lý phải xác nhận "đã đối chiếu nguyện vọng" trước khi ghi biện pháp hồi sức. Nếu cần chuyển viện, lệnh Chuyển viện được thực hiện từ sự cố và hệ thống tạo bản tóm tắt chuyển viện.

**Why this priority**: Vài phút đầu của cấp cứu quyết định kết quả; thông tin sai hoặc thiếu (dị ứng, nguyện vọng không hồi sức) gây hại không thể đảo ngược (9.5, BR-M05-13).

**Independent Test**: Cho A có nguyện vọng "không đặt nội khí quản", dị ứng Penicillin, hai thuốc đang dùng; điều dưỡng kích hoạt khẩn cấp; kiểm tra nội dung thẻ, bốn người nhận thông báo, việc chặn ghi biện pháp hồi sức trước khi xác nhận, và bản tóm tắt khi chuyển viện.

**Acceptance Scenarios**:

1. **Given** A có nguyện vọng cuối đời đã ghi nhận, dị ứng Penicillin Hiệu lực, đơn Amlodipin và Metformin đang dùng, bệnh nền tăng huyết áp, cờ nguy cơ ngã, người liên hệ chính là con gái M, **When** D tạo sự cố Khẩn cấp "khó thở" cho A, **Then** thẻ thông tin khẩn cấp hiển thị ngay đủ các mục trên kèm họ tên, ảnh, phòng/giường, và nội dung thẻ tại thời điểm đó được lưu cùng sự cố (9.5, BR-M05-13).
2. **Given** như trên, **When** sự cố được tạo, **Then** trong cùng lúc hệ thống phát thông báo khẩn cấp song song cho Bác sĩ trực, Trưởng tầng của tầng A, Quản lý viện, người liên hệ chính M, điều dưỡng phụ trách A và Người phụ trách ca (BR-M05-06, BR-M13-01); người nhận chưa xác nhận sau CFG-M13-01 (mặc định \[5 phút\]) được xử lý theo feature 009 (BR-M13-02).
3. **Given** sự cố Khẩn cấp của A vừa tạo, **When** D ghi biện pháp hồi sức "ép tim ngoài lồng ngực" khi chưa xác nhận đối chiếu nguyện vọng, **Then** hệ thống chặn và hiển thị lại nguyện vọng; **When** D xác nhận "đã đối chiếu nguyện vọng", **Then** hệ thống lưu người xác nhận, thời điểm, và cho ghi biện pháp hồi sức (BR-M05-13, DBR-19).
4. **Given** người cao tuổi C không có nguyện vọng cuối đời đã ghi nhận, **When** D kích hoạt khẩn cấp cho C, **Then** thẻ ghi rõ "chưa ghi nhận nguyện vọng cuối đời" và biện pháp hồi sức ghi được ngay, không cần xác nhận.
5. **Given** sự cố Khẩn cấp của A, **When** D ghi các hành động: 10:02 gọi bác sĩ trực, 10:04 gọi cấp cứu 115, 10:20 xe cấp cứu đến, **Then** mỗi hành động là một bản ghi diễn biến có thời điểm và người ghi; hồ sơ sự cố có thời gian phát hiện, người xử lý, thời gian gọi hỗ trợ/cấp cứu, người được thông báo (9.5).
6. **Given** sự cố Khẩn cấp của A Đang xử lý và A Đang lưu trú, **When** bác sĩ B chọn "Chuyển viện" tới Bệnh viện X, **Then** trong cùng một lệnh: lệnh Chuyển viện của feature 001/004 được thực hiện (A chuyển Điều trị tại bệnh viện, căn cứ là sự cố này) và bản tóm tắt chuyển viện được tạo gồm dị ứng, thuốc đang dùng, chỉ số gần nhất của từng loại kèm thời điểm, và diễn biến sự cố (BR-M05-14).
7. **Given** A đã ở Điều trị tại bệnh viện, **When** B chọn "Chuyển viện" từ một sự cố khác, **Then** lệnh bị từ chối vì trạng thái không cho phép (5.5) và không tạo bản tóm tắt ("hoặc toàn bộ, hoặc không").
8. **Given** sự cố Khẩn cấp đã ghi, **When** bất kỳ ai thử sửa hay xóa nội dung, **Then** không có thao tác đó; bổ sung hay sửa sai bằng bản đính chính có lý do (BR-M05-08, BR-M15-03).
9. **Given** thiết bị của D mất kết nối, **When** D kích hoạt khẩn cấp, **Then** hệ thống không lưu tạm yêu cầu này trên thiết bị và báo rõ "không gửi được – gọi trực tiếp theo quy trình của cơ sở" (8.6, Q-01).

---

### User Story 6 - Sự cố ngã và theo dõi sau ngã (Priority: P2)

Khi sự cố loại Ngã được ghi nhận, trong cùng lần ghi hệ thống tạo yêu cầu đánh giá lại, yêu cầu xem xét kế hoạch chăm sóc và lịch theo dõi sau ngã: đo sinh hiệu mỗi CFG-M05-06 trong khoảng thời gian cấu hình.

**Why this priority**: Người cao tuổi sau ngã có nguy cơ biến chứng muộn (chảy máu nội sọ, gãy xương ẩn); theo dõi có lịch là biện pháp tối thiểu (BR-M05-07).

**Independent Test**: Ghi sự cố ngã cho A lúc 10:00; kiểm tra một yêu cầu đánh giá lại, một yêu cầu xem xét kế hoạch và lịch theo dõi sinh 18 lần đo sinh hiệu (72 giờ ÷ 4 giờ) bắt đầu 14:00.

**Acceptance Scenarios**:

1. **Given** A không có lịch theo dõi sau ngã, **When** S ghi sự cố "ngã trong phòng tắm" lúc 10:00, **Then** trong cùng lần ghi: feature 001 tạo yêu cầu đánh giá lại lý do "sau sự cố ngã" (BR-M01-02); feature 005 tạo yêu cầu xem xét kế hoạch chăm sóc hạn CFG-M04-08 (BR-M04-20); hệ thống tạo lịch theo dõi sau ngã với các chỉ số thuộc nhóm "sinh hiệu", mỗi CFG-M05-06 (mặc định \[4 giờ\] trong \[72 giờ\]) từ 10:00, lần đo đầu 14:00, kết thúc 10:00 sau 72 giờ (BR-M05-07).
2. **Given** loại Ngã có mức mặc định Khẩn cấp trong danh mục loại sự cố, **When** S ghi sự cố ngã mà chọn mức Nhẹ, **Then** hệ thống bắt buộc lý do chọn mức thấp hơn mặc định (FR-042).
3. **Given** A đang có lịch theo dõi sau ngã còn 30 giờ, **When** A ngã lần nữa, **Then** lịch cũ chuyển Đã ngừng (lý do "thay bằng lịch theo dõi mới") và lịch mới bắt đầu từ lần ngã này; không có hai lịch theo dõi sau ngã cùng Hiệu lực (FR-052).
4. **Given** lịch theo dõi sau ngã của A, **When** một lần đo trong lịch vượt ngưỡng, **Then** cảnh báo được tạo như User Story 1.
5. **Given** A được chuyển viện trong thời gian theo dõi, **When** A vắng mặt, **Then** công việc đo trong khoảng vắng chuyển Hủy (feature 005 FR-020); lịch theo dõi không tự kéo dài.
6. **Given** bác sĩ B đánh giá A ổn định sau 24 giờ, **When** B ngừng lịch theo dõi sau ngã kèm lý do, **Then** lịch chuyển Đã ngừng và công việc Chưa đến hạn chuyển Hủy.

---

### User Story 7 - Cảnh báo theo xu hướng (Priority: P2)

Ngoài so ngưỡng từng lần đo, hệ thống theo dõi xu hướng: cân nặng giảm nhiều trong một khoảng ngày, bỏ lỡ lần đo theo lịch, mất ngủ nhiều đêm liên tiếp. Mỗi quy tắc bật/tắt được, có tham số và mức cảnh báo cấu hình được.

**Why this priority**: Nhiều vấn đề (suy dinh dưỡng, trầm cảm) không vượt ngưỡng ở lần đo nào nhưng lộ ra qua xu hướng (BR-M05-04).

**Independent Test**: Nhập chuỗi cân nặng 50,0 → 48,5 → 47,4 kg trong 25 ngày; ghi "mất ngủ" 3 đêm liên tiếp; để một công việc đo theo lịch kết thúc Không thực hiện; kiểm tra ba cảnh báo đúng loại và mức.

**Acceptance Scenarios**:

1. **Given** cân nặng cao nhất của A trong CFG-M05-03 (mặc định \[5% trong 30 ngày\]) trước lần đo là 50,0 kg, **When** ghi 47,4 kg (giảm 5,2%), **Then** hệ thống tạo cảnh báo "sụt cân" theo mức cấu hình của quy tắc (mặc định Trung bình), nêu hai lần đo được so (BR-M05-04).
2. **Given** chất lượng ngủ của A được ghi "mất ngủ" (feature 005) hai đêm liên tiếp, **When** đêm thứ ba cũng ghi "mất ngủ" (CFG-M05-05, mặc định \[3\]), **Then** hệ thống tạo cảnh báo "mất ngủ kéo dài" (mặc định Nhẹ); đêm không có ghi nhận ngủ làm đứt chuỗi.
3. **Given** công việc đo huyết áp 17:00 của A sinh từ lịch đo, **When** công việc kết thúc Không thực hiện, hoặc tới hết ca vẫn Quá hạn, **Then** hệ thống tạo hoặc gộp đúng một cảnh báo "bỏ lỡ lần đo theo lịch: huyết áp"; nếu công việc là Bắt buộc, yêu cầu cảnh báo quá hạn của feature 005 lúc trước đã tạo chính cảnh báo này ở mức Trung bình, không có cảnh báo "công việc quá hạn" riêng; công việc bị Hủy do vắng mặt không tính (BR-M05-04, FR-039b, Q-75).
4. **Given** feature 006 báo A từ chối cùng một thuốc CFG-M05-04 (mặc định \[2\]) lần liên tiếp, **When** nhận yêu cầu, **Then** hệ thống tạo hoặc gộp cảnh báo "từ chối thuốc" mức Trung bình (BR-M07-13, feature 006 FR-032).
5. **Given** Quản lý viện tắt quy tắc "mất ngủ kéo dài", **When** A có 3 đêm mất ngủ, **Then** không tạo cảnh báo; việc tắt được ghi nhật ký kèm lý do (BR-M15-04).

---

### User Story 8 - Sự cố lây nhiễm: truy vết tiếp xúc và khoanh vùng (Priority: P2)

Khi phát hiện dấu hiệu nghi ngờ lây nhiễm, nhân viên ghi sự cố loại "lây nhiễm nghi ngờ". Hệ thống tự đề xuất danh sách tiếp xúc từ dữ liệu sẵn có (người cùng phòng, người cùng tham gia hoạt động, nhân viên được phân công chăm sóc, người thân đã đến thăm); người có thẩm quyền xác nhận danh sách cuối cùng. Người cao tuổi tiếp xúc được sinh lịch đo nhiệt độ. Bác sĩ hoặc quản lý khoanh vùng khu vực; hệ thống chặn đăng ký thăm mới, hoạt động chung và phân bổ giường mới trong vùng cho tới khi gỡ.

**Why this priority**: Ổ dịch trong viện dưỡng lão lan nhanh và có tỷ lệ tử vong cao; truy vết thủ công bằng sổ giấy chậm và sót (9.6).

**Independent Test**: Dựng dữ liệu 5 ngày: A ở P201 cùng B; A tham gia 2 buổi hoạt động cùng C, E; S chăm sóc A; người thân N thăm A. Ghi sự cố lây nhiễm cho A; kiểm tra danh sách đề xuất đúng 6 người, loại người ngoài khoảng thời gian; khoanh vùng tầng 2 và thử đăng ký thăm, đăng ký hoạt động, phân bổ giường.

**Acceptance Scenarios**:

1. **Given** trong CFG-M05-07 (mặc định \[5 ngày\]) trước thời điểm phát hiện, A ở chung P201 với B (feature 003), cùng điểm danh hai buổi hoạt động với C và E (feature 014), được S chăm sóc (feature 008), được người thân N thăm (feature 012), **When** D ghi sự cố "sốt, ho – nghi cúm" loại lây nhiễm nghi ngờ cho A, **Then** hệ thống lập danh sách tiếp xúc Đề xuất gồm B, C, E (người cao tuổi), S (nhân viên), N (người thân), mỗi người kèm nguồn và khoảng thời gian tiếp xúc; người chỉ tiếp xúc trước khoảng này không có trong danh sách (BR-M05-10).
2. **Given** A Tạm vắng từ ngày thứ 3 đến ngày thứ 4 của khoảng truy vết, **When** lập đề xuất, **Then** các khoảng A vắng mặt được đánh dấu để người xác nhận xem xét (feature 003 FR-036).
3. **Given** danh sách Đề xuất, **When** điều dưỡng D (phụ trách A) loại E với lý do "ngồi bàn khác, cách xa" và thêm F thủ công với lý do "ăn chung bàn", rồi xác nhận, **Then** danh sách Đã xác nhận gồm B, C, F, S, N; B, C, F được sinh lịch đo nhiệt độ CFG-M05-08 (mặc định \[2 lần/ngày trong 7 ngày\]); S và Quản lý viện được thông báo; N được thông báo qua feature 009 (BR-M05-10, 11).
4. **Given** bác sĩ B khoanh vùng Tầng 2 với lý do và gắn sự cố của A, **When** khoanh vùng có hiệu lực, **Then** hệ thống chặn đăng ký thăm mới cho người cao tuổi ở Tầng 2 (BR-M10-02), chặn đăng ký hoạt động chung (BR-M04-15), chặn phân bổ giường mới trong Tầng 2 (feature 003 FR-009), feature 003 sinh công việc khử khuẩn (BR-M03-11); Trưởng tầng, Hành chính và điều dưỡng của Tầng 2 được thông báo; dashboard hiển thị khu đang khoanh vùng (18.5) (BR-M05-11).
5. **Given** Tầng 2 đang khoanh vùng, **When** người thân đăng ký thăm A, hành chính phân bổ giường mới ở P203, hoặc trưởng tầng đăng ký A vào buổi thể dục chung, **Then** cả ba bị chặn và nêu lý do "khu đang khoanh vùng".
6. **Given** Tầng 2 đang khoanh vùng, **When** Trưởng tầng T thử gỡ khoanh vùng, **Then** hệ thống chặn (4.4 dòng "Khoanh vùng lây nhiễm": TT X); **When** bác sĩ B gỡ kèm lý do, **Then** vùng chuyển Đã gỡ, các chặn tự động bỏ (trừ phòng đang cách ly riêng), feature 003 sinh khử khuẩn kết thúc (BR-M05-12).
7. **Given** Tầng 2 đang khoanh vùng gắn sự cố của A và không vùng nào khác gắn sự cố này, **When** D đóng sự cố của A, **Then** hệ thống chặn cho tới khi vùng được gỡ (FR-066).
8. **Given** B trong danh sách tiếp xúc Đã xác nhận, **When** B bị loại khỏi danh sách với lý do, **Then** lịch đo nhiệt độ của B chuyển Đã ngừng; dấu "tiếp xúc" của B được gỡ.

---

### User Story 9 - Phạm vi y tế theo giấy phép và ghi nhận khám, điều trị (Priority: P3)

Quản lý viện lưu giấy phép hoạt động của cơ sở (số, cơ quan cấp, ngày cấp, ngày hết hạn, phạm vi). Việc cơ sở có hay không có phạm vi khám bệnh, chữa bệnh được suy ra từ giấy phép đang hiệu lực, không phải một công tắc riêng. Bác sĩ ghi nhận khám, đánh giá, chẩn đoán, chỉ định, điều trị, kết quả; chẩn đoán, chỉ định và điều trị tại viện chỉ được ghi khi cơ sở và bác sĩ đều có giấy phép đúng phạm vi còn hiệu lực. Khám và điều trị ở cơ sở bên ngoài được ghi kèm thông tin cơ sở thực hiện.

**Why this priority**: Nguyên tắc "có bác sĩ không có nghĩa là cơ sở được khám chữa bệnh" (10.4) là yêu cầu pháp lý; phần kiểm tra quyền đã có ở feature 002, spec này cung cấp dữ liệu giấy phép và áp vào ghi nhận khám, điều trị.

**Independent Test**: Lưu giấy phép cơ sở không có phạm vi khám chữa bệnh; bác sĩ có giấy phép hợp lệ thử ghi chẩn đoán tại viện (bị chặn) và ghi kết quả khám tại Bệnh viện X (được phép); thêm giấy phép có phạm vi khám chữa bệnh và thử lại.

**Acceptance Scenarios**:

1. **Given** Quản lý viện Q, **When** Q lưu giấy phép số 123/GP, cơ quan cấp Sở Y tế, ngày cấp 01/01/2025, hết hạn 31/12/2027, phạm vi "chăm sóc người cao tuổi; khám bệnh, chữa bệnh chuyên khoa nội", kèm bản scan, **Then** giấy phép Hiệu lực và cơ sở được coi là có phạm vi khám chữa bệnh chuyên khoa nội tới hết ngày 31/12/2027 (10.4, feature 002 FR-038).
2. **Given** giấy phép cơ sở không có phạm vi khám chữa bệnh, **When** bác sĩ B ghi chẩn đoán "viêm phổi" với cơ sở thực hiện là viện, **Then** hệ thống chặn (BR-M06-05, feature 002 FR-037); **When** B ghi chẩn đoán của Bệnh viện X kèm tên cơ sở, người thực hiện, ngày, **Then** được phép.
3. **Given** giấy phép cơ sở còn 59 ngày tới hạn, **When** Bộ lập lịch kiểm tra, **Then** Quản lý viện nhận cảnh báo theo feature 002 FR-039 (CFG-M06-02, mặc định \[60 ngày\]).
4. **Given** giấy phép cũ hết hạn và Q lưu giấy phép gia hạn, **When** lưu, **Then** giấy phép cũ giữ nguyên trong lịch sử, giấy phép mới Hiệu lực; quyền chuyên môn tự bật lại (feature 002 FR-038).
5. **Given** bản ghi khám đã lưu, **When** người ghi phát hiện sai, **Then** chỉ tạo được bản đính chính có lý do; bản gốc giữ nguyên (1.5 nhóm 3).

---

### User Story 10 - Đính chính chỉ số và tra lịch sử sức khỏe (Priority: P3)

Bản ghi chỉ số và sự cố không bao giờ bị sửa hay xóa. Khi ghi sai, người có quyền tạo bản đính chính có lý do; giá trị cũ vẫn được giữ và xem được. Bác sĩ, điều dưỡng xem được lịch sử chỉ số theo thời gian (kèm ngưỡng áp dụng tại từng lần đo), lịch sử cảnh báo, sự cố của người cao tuổi.

**Why this priority**: Toàn vẹn hồ sơ (1.3, BR-M06-06); lịch sử là căn cứ cho khám, đánh giá lại và bản tóm tắt chuyển viện.

**Independent Test**: Ghi nhầm huyết áp 180/100 (đúng là 130/80), đính chính; kiểm tra giá trị hiện hành, giá trị gốc vẫn hiển thị, cảnh báo đã tạo không tự đóng và người phụ trách được báo.

**Acceptance Scenarios**:

1. **Given** S ghi nhầm huyết áp A là 180/100 và cảnh báo đã được tạo, **When** S tạo đính chính 130/80 kèm lý do "nhập nhầm của người khác", **Then** giá trị hiện hành là 130/80, bản gốc 180/100 vẫn xem được với dấu "đã đính chính"; cảnh báo không tự đóng, người phụ trách cảnh báo được thông báo "nguồn đã được đính chính" để đóng với kết quả phù hợp (BR-M06-06, FR-019).
2. **Given** người không phải người ghi gốc, Trưởng tầng hay Người phụ trách ca của tầng, **When** người đó tạo đính chính, **Then** hệ thống chặn (1.5, feature 000).
3. **Given** bác sĩ B mở lịch sử huyết áp của A trong 30 ngày, **When** xem, **Then** mỗi lần đo hiển thị giá trị hiện hành, giá trị gốc nếu đã đính chính, người đo, thời điểm, ngưỡng áp dụng (cá nhân hay mặc định, phiên bản) và kết quả so.

---

### User Story 11 - Người cao tuổi nguy kịch: báo gia đình, xác nhận lại và thực hiện nguyện vọng cuối đời (Priority: P1) *(bổ sung 2026-09-28)*

Bác sĩ nhận định người cao tuổi nguy kịch và ghi dấu nguy kịch (không có Bác sĩ trực thì Điều dưỡng ghi dấu tạm). Hệ thống tạo ngay cảnh báo Khẩn cấp hiển thị nguyện vọng cuối đời, báo đội ngũ và gia đình, và tạo yêu cầu gọi điện để xác nhận lại nguyện vọng. Sau khi có kết quả, người xử lý thực hiện lựa chọn: chuyển bệnh viện điều trị tích cực, đưa về nhà, hoặc ở lại viện chăm sóc giảm nhẹ.

**Why this priority**: Góp ý nghiệp vụ Q-213: quyết định lúc nguy kịch có ý nghĩa pháp lý và không đảo ngược được; hệ thống phải bảo đảm nhân viên thấy nguyện vọng và đã hỏi lại gia đình.

**Independent Test**: Cho A có nguyện vọng "đưa về nhà", B chưa có nguyện vọng. Ghi dấu nguy kịch cho cả hai, thử đóng cảnh báo khi chưa xác nhận lại, ghi các kết quả xác nhận khác nhau và kiểm tra lệnh được thực hiện.

**Acceptance Scenarios**:

1. **Given** A có nguyện vọng Hiệu lực "đưa về nhà", **When** Bác sĩ BS1 ghi dấu nguy kịch cho A kèm nhận định, **Then** trong cùng lần: có đúng một cảnh báo Khẩn cấp "nguy kịch – thực hiện nguyện vọng cuối đời" hiển thị nguyện vọng; Bác sĩ trực, Điều dưỡng phụ trách, Trưởng tầng, Quản lý viện được báo; người liên hệ chính và người đại diện nhận thông báo Khẩn cấp không có nội dung nguyện vọng; có một yêu cầu gọi điện xác nhận lại bắt đầu từ người đại diện (FR-050b).
2. **Given** 23:00 không có Bác sĩ trực, **When** điều dưỡng D ghi dấu tạm cho A, **Then** cảnh báo được tạo như kịch bản 1 và mọi Bác sĩ đang hoạt động được báo để xác nhận dấu (FR-050a, Q-218).
3. **Given** cảnh báo nguy kịch của A đang xử lý và chưa có kết quả xác nhận lại, **When** D tìm cách đóng cảnh báo, **Then** hệ thống chặn (FR-050d).
4. **Given** D gọi được người đại diện P và P muốn đổi sang "chuyển bệnh viện điều trị tích cực", **When** D ghi kết quả "nguyện vọng mới" với người trả lời P, **Then** feature 001 ghi phiên bản nguyện vọng mới, phiên bản cũ Được thay thế; **When** D ghi lựa chọn "chuyển bệnh viện", **Then** lệnh Chuyển viện được thực hiện kèm bản tóm tắt; dấu nguy kịch tự gỡ; cảnh báo đóng được (FR-050c, FR-050d, FR-049).
5. **Given** giữ nguyện vọng "đưa về nhà", **When** D ghi lựa chọn, **Then** feature 004 nhận yêu cầu Cho tạm vắng lý do "về nhà theo nguyện vọng cuối đời" để Hành chính hoặc Trưởng tầng thực hiện qua quy trình đón (FR-050d, Q-218).
6. **Given** B chưa có nguyện vọng Hiệu lực và không liên lạc được ai trong thứ tự gọi, **When** BS1 ghi "không liên lạc được" và quyết định "ở lại viện chăm sóc giảm nhẹ" kèm lý do, **Then** feature 005 nhận yêu cầu xem xét kế hoạch chăm sóc; khi thiếu lý do, hệ thống chặn (FR-050c, FR-050d, BR-M05-16).
7. **Given** A có dấu nguy kịch, **When** tình trạng ổn định và BS1 gỡ dấu có lý do, **Then** dấu chuyển Đã gỡ; ghi dấu lại sau đó tạo cảnh báo mới (bảng trạng thái dấu nguy kịch).

---

### Edge Cases

- **Người ghi sự cố ngoài phạm vi và thẻ thông tin khẩn cấp**: nhân viên ngoài phạm vi (ví dụ nhân viên bếp) kích hoạt được khẩn cấp nhưng chỉ thấy thông tin nhận dạng (feature 002 FR-044a); thẻ thông tin khẩn cấp chỉ hiển thị cho người có quyền xem sức khỏe của người cao tuổi đó (người xử lý được thông báo). Vì vậy người ngoài phạm vi không ghi được biện pháp hồi sức khi người cao tuổi có nguyện vọng cuối đời; họ ghi hành động khác (gọi hỗ trợ, sơ cứu không phải hồi sức) và người xử lý có quyền ghi phần còn lại (FR-048, FR-048a).
- **Giá trị nguy hiểm ghi ngoại tuyến**: bản ghi được lưu tạm và so ngưỡng khi đồng bộ; cảnh báo Khẩn cấp tạo lúc đồng bộ kèm nhãn "ghi ngoại tuyến"; việc đo lại xác nhận xét theo thời điểm trên thiết bị. Vì kích hoạt khẩn cấp bắt buộc trực tuyến (8.6), khi mất kết nối người đo phải báo trực tiếp theo quy trình của cơ sở.
- **Lần đo lại cũng không nằm trong khoảng hợp lệ vật lý**: bị chặn như lần đo mới; bản ghi đầu vẫn "chờ đo lại" tới khi có lần đo lại hợp lệ hoặc hết CFG-M06-04.
- **Không có ngưỡng nào cho chỉ số** (không có ngưỡng cá nhân và chưa có ngưỡng mặc định): bản ghi được lưu với kết quả so "chưa có ngưỡng", không tạo cảnh báo; danh sách "chưa có ngưỡng" hiện cho Bác sĩ.
- **Ngưỡng cá nhân chỉ khai báo một phía** (ví dụ chỉ cận trên nhiệt độ): phía để trống không so; ngưỡng cá nhân thay toàn bộ ngưỡng mặc định của chỉ số đó, không trộn từng cận (FR-020).
- **Giá trị đúng bằng cận ngưỡng**: không coi là vượt (FR-024).
- **Cảnh báo gộp có nguồn đã được xử lý và nguồn chưa**: cảnh báo chỉ tự đóng khi mọi nguồn đã được xử lý; nguồn đã xử lý được ghi vào lịch sử cảnh báo (FR-032).
- **Cảnh báo Nhẹ không được tiếp nhận trong ca**: vào bàn giao và leo thang một cấp khi hết ca (FR-033).
- **Người nhận của một cấp leo thang không có** (không có Bác sĩ trực trong ca): thông báo gửi cho những người còn lại của cấp đó; nếu cả cấp không có ai, leo thang thẳng lên cấp kế tiếp.
- **Người cao tuổi chuyển trạng thái cuối khi còn cảnh báo, sự cố mở**: cảnh báo có nguồn được feature 005, 006 đóng thì tự đóng; cảnh báo, sự cố còn lại dừng leo thang và nhắc, được liệt kê để đóng thủ công với kết quả (điều kiện kết thúc lưu trú và danh sách việc sau qua đời của feature 004 FR-063, FR-071); lịch đo, ngưỡng cá nhân, theo dõi tiếp xúc chuyển Đã ngừng / Hết hiệu lực.
- **Sự cố xảy ra khi người cao tuổi Hoạt động bên ngoài**: nguồn phát sinh "hoạt động ngoài viện", địa điểm ngoài viện; người đi cùng ghi nhận được theo ngoại lệ phạm vi; Chuyển viện hợp lệ từ Hoạt động bên ngoài (5.5).
- **Hai người kích hoạt khẩn cấp cho cùng người cao tuổi gần như đồng thời**: nếu người đó đã có sự cố Khẩn cấp đang mở, hệ thống hiển thị sự cố đang mở và hỏi người sau gộp thành người xử lý của sự cố đó hay tạo sự cố riêng (ví dụ hai lần ngã khác nhau); không tự gộp sự cố (FR-041).
- **Sự cố không gắn người cao tuổi** (hư hỏng cơ sở vật chất phát hiện ngoài công việc vệ sinh, feature 003): được phép với loại "cơ sở vật chất", bắt buộc địa điểm; không có thẻ khẩn cấp, không kích hoạt các tác động theo người cao tuổi.
- **Người tiếp xúc đã có lịch đo nhiệt độ do bác sĩ đặt**: lịch theo dõi tiếp xúc vẫn được tạo riêng; hai lịch có thể sinh công việc gần giờ nhau, nhân viên ghi cả hai.
- **Người tiếp xúc thuộc nhiều sự cố lây nhiễm**: mỗi lần xác nhận tạo lịch theo dõi riêng gắn sự cố đó; lịch trùng thời gian của cùng người cao tuổi được gộp thành một lịch kéo tới thời điểm kết thúc muộn nhất.
- **Hai vùng khoanh vùng chồng nhau**: chặn còn hiệu lực khi còn ít nhất một vùng đang khoanh vùng bao phòng đó; gỡ một vùng không bỏ chặn của vùng kia.
- **Lượt thăm đã được duyệt trước khi khoanh vùng**: feature 012 tự chuyển Hủy với lý do "khu đang khoanh vùng" và báo người đăng ký; gỡ vùng không khôi phục lượt; Hành chính và Trưởng tầng nhận danh sách các lượt bị hủy (feature 012 FR-037, Q-120).
- **Giấy phép cơ sở hết hạn giữa chừng khi bác sĩ đang ghi chẩn đoán tại viện**: kiểm tra tại thời điểm lưu; sau 23:59 ngày hết hạn thì bị chặn (feature 002 FR-038).

## Requirements *(mandatory)*

### Functional Requirements

Mọi yêu cầu dưới đây kế thừa feature 000: nhóm dữ liệu (mục 1.5), lý do bắt buộc cho lệnh nhóm 2, đính chính cho bản ghi đã xác nhận, nhật ký cho mọi thay đổi, tham số theo mã CFG, "hoặc toàn bộ, hoặc không" khi một lệnh có nhiều tác động, mốc thời gian theo Asia/Ho_Chi_Minh (DBR-25), danh sách bản ghi bảo vệ (BR-M15-03). Phân nhóm dữ liệu của feature này:

| Dữ liệu | Nhóm (1.5) | Thao tác được phép |
| --- | --- | --- |
| Danh mục chỉ số (loại, đơn vị, thành phần, khoảng hợp lệ vật lý, nhóm "sinh hiệu") | 1 – Danh mục | Tạo, sửa, Ngừng hiệu lực (Quản lý viện); không xóa khi đã được tham chiếu |
| Danh mục loại sự cố, danh mục quy tắc xu hướng | 1 – Danh mục | Tạo, sửa, Ngừng hiệu lực (Quản lý viện) |
| Giấy phép hoạt động của cơ sở | 1 – Danh mục có lịch sử | Tạo bản mới; không sửa bản đã lưu, không xóa (FR-075) |
| Lịch đo | 2 | Chỉ qua lệnh ở bảng FR-006 |
| Phiên bản ngưỡng (mặc định, cá nhân) | 2 | Chỉ qua lệnh ở bảng FR-022 |
| Bản ghi chỉ số | 3 | Chỉ ghi thêm; sai sót xử lý bằng đính chính (BR-M06-06) |
| Cảnh báo | 2 | Chỉ qua lệnh và sự kiện ở bảng FR-029 |
| Lịch sử cảnh báo (tạo, gộp, leo thang, tiếp nhận, đổi mức, chuyển người phụ trách, kết quả) | 3 | Chỉ ghi thêm |
| Sự cố: nội dung ghi nhận, diễn biến, xác nhận đối chiếu nguyện vọng, bản chụp thẻ thông tin khẩn cấp | 3 | Chỉ ghi thêm; sai sót xử lý bằng đính chính; sự cố Khẩn cấp thuộc danh sách bảo vệ (BR-M05-08, BR-M15-03) |
| Trạng thái xử lý của sự cố | 2 – do lệnh điều khiển | Chỉ qua lệnh ở bảng FR-045 |
| Bản tóm tắt chuyển viện | 3 | Chỉ ghi thêm |
| Danh sách tiếp xúc | 2 | Chỉ qua lệnh ở bảng FR-061 |
| Khoanh vùng | 2 | Chỉ qua lệnh ở bảng FR-064 |
| Bản ghi khám và điều trị | 3 | Chỉ ghi thêm; sai sót xử lý bằng đính chính |
| Dấu nguy kịch *(bổ sung 2026-09-28)* | 2 | Chỉ qua lệnh ở bảng trạng thái dấu nguy kịch (FR-050a) |
| Kết quả xác nhận lại nguyện vọng, quyết định thực hiện lựa chọn | 3 | Chỉ ghi thêm (BR-M05-16) |

#### A. Danh mục chỉ số và lịch đo

- **FR-001**: Hệ thống MUST có danh mục chỉ số gồm ít nhất huyết áp (hai thành phần tâm thu, tâm trương), nhịp tim, nhiệt độ, SpO2, đường huyết, cân nặng; Quản lý viện MUST thêm được chỉ số khác. Mỗi chỉ số MUST có: tên, đơn vị, các thành phần (nếu nhiều hơn một), khoảng hợp lệ vật lý của từng thành phần (CFG-M06-03, mặc định theo danh mục chỉ số), thuộc nhóm "sinh hiệu" hay không, trạng thái hiệu lực. *(Nguồn: 10.1, BR-M06-04, CFG-M06-03, 1.5 nhóm 1)*
- **FR-002**: Chỉ Quản lý viện MUST cấu hình được danh mục chỉ số; đổi khoảng hợp lệ vật lý MUST được ghi nhật ký kèm giá trị trước/sau và chỉ áp cho lần ghi sau đó. *(Nguồn: BR-M15-04; giả định về quyền, xem Điểm cần báo lại 9)*
- **FR-003**: Bác sĩ MUST tạo được lịch đo riêng cho một người cao tuổi, gồm: chỉ số (một hoặc nhiều), tần suất (các mốc giờ trong ngày, hoặc mỗi N giờ từ một mốc, hoặc theo ngày trong tuần kèm mốc giờ), thời điểm bắt đầu, thời điểm kết thúc (nếu có), mức quan trọng (mặc định Bắt buộc), ghi chú. Lịch đo MUST ghi nguồn: "chỉ định bác sĩ", "theo dõi sau ngã" (FR-051) hoặc "theo dõi tiếp xúc" (FR-062), và sự cố nguồn với hai loại sau. *(Nguồn: 10.1, BR-M04-06 "đo chỉ số theo chỉ định" là Bắt buộc, BR-M05-07, BR-M05-11)*
- **FR-004**: Lịch đo ở Hiệu lực MUST là nguồn sinh công việc "đo chỉ số" của feature 005 (BR-M04-01); mỗi mốc đo MUST sinh đúng một công việc theo (lịch đo, thời điểm dự kiến) (DBR-12, feature 005 FR-021). Spec này MUST NOT tự sinh công việc. *(Nguồn: 10.1, BR-M04-01, DBR-12)*
- **FR-005**: Lịch đo MUST NOT sửa được chỉ số, tần suất, mốc giờ hay thời điểm bắt đầu khi đã Hiệu lực hoặc Chờ hiệu lực; thay đổi MUST dùng lệnh "Thay đổi lịch đo" tạo lịch mới trỏ về lịch cũ, lịch cũ chuyển Đã ngừng tại thời điểm hiệu lực của lịch mới, trong cùng một lệnh. *(Nguồn: 1.5 nhóm 2, BR-M04-03)*
- **FR-006**: Vòng đời lịch đo MUST theo bảng dưới; không có chuyển nào khác. *(Nguồn: 10.1, BR-M05-07, BR-M05-11, constitution IV)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái mới | Người thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Tạo lịch đo | Hiệu lực (bắt đầu không sau thời điểm lệnh) hoặc Chờ hiệu lực | Bác sĩ | Người cao tuổi chưa ở trạng thái cuối; đủ trường FR-003 | feature 005 sinh công việc (FR-004) |
| — | Sự cố ngã được ghi nhận | Hiệu lực (nguồn "theo dõi sau ngã") | Hệ thống | FR-051 | Như trên |
| — | Danh sách tiếp xúc xác nhận hoặc bổ sung người cao tuổi | Hiệu lực (nguồn "theo dõi tiếp xúc") | Hệ thống | FR-062 | Như trên |
| Chờ hiệu lực | Tới thời điểm bắt đầu | Hiệu lực | Bộ lập lịch | — | — |
| Hiệu lực, Chờ hiệu lực | Thay đổi lịch đo | Đã ngừng | Bác sĩ | Bắt buộc lý do; lịch mới hợp lệ; cùng một lệnh (FR-005) | feature 005 hủy và sinh lại công việc Chưa đến hạn (feature 005 FR-022) |
| Hiệu lực, Chờ hiệu lực | Ngừng lịch đo | Đã ngừng | Bác sĩ | Bắt buộc lý do | Như trên |
| Hiệu lực | Tới thời điểm kết thúc | Hết hạn | Bộ lập lịch | — | Không sinh công việc sau thời điểm kết thúc |
| Hiệu lực | Sự cố ngã mới / lịch theo dõi tiếp xúc trùng (FR-052, FR-062) | Đã ngừng | Hệ thống | Lịch cùng nguồn tự động của cùng người cao tuổi | Lịch mới thay thế |
| Hiệu lực, Chờ hiệu lực | Người tiếp xúc bị loại khỏi danh sách | Đã ngừng | Hệ thống | Lịch nguồn "theo dõi tiếp xúc" của người đó (FR-061) | Như "Ngừng lịch đo" |
| Hiệu lực, Chờ hiệu lực | Người cao tuổi chuyển trạng thái cuối | Đã ngừng | Hệ thống | — | Lý do là trạng thái cuối |

Đã ngừng và Hết hạn là trạng thái cuối. Người cao tuổi vắng mặt không làm đổi trạng thái lịch đo; feature 005 không sinh hoặc hủy công việc trong khoảng vắng (feature 005 FR-017, FR-020).

#### B. Ghi nhận chỉ số và so ngưỡng

- **FR-010**: Nhân viên chăm sóc và Điều dưỡng MUST ghi được chỉ số cho người cao tuổi trong phạm vi phân công, từ công việc đo (feature 005) hoặc đo ngoài lịch. Mỗi bản ghi MUST gồm: người cao tuổi, chỉ số, giá trị từng thành phần, đơn vị, thời điểm đo, người đo, công việc nguồn (nếu có), thời điểm ghi trên thiết bị và thời điểm đồng bộ (với bản ghi ngoại tuyến), ghi chú, ngưỡng đã áp dụng (loại và phiên bản), kết quả so. *(Nguồn: 10.1, UC-32, CHI_SO, 4.4 dòng "Chỉ số sức khỏe", DBR-25)*
- **FR-011**: Giá trị nằm ngoài khoảng hợp lệ vật lý của thành phần (CFG-M06-03) MUST bị chặn, không lưu, kèm thông báo khoảng hợp lệ; công việc nguồn giữ nguyên trạng thái. *(Nguồn: BR-M06-04, feature 005 FR-034)*
- **FR-012**: Thời điểm đo MUST mặc định là thời điểm ghi; người đo MAY chọn thời điểm sớm hơn nhưng MUST NOT sau thời điểm ghi. Nhãn "ghi nhận muộn" và cách xét thời điểm của bản ghi ngoại tuyến theo feature 005 FR-035 (BR-M04-12). *(Nguồn: BR-M04-12, Q-01 theo mặc định)*
- **FR-013**: Ngay khi bản ghi được lưu (hoặc đồng bộ), hệ thống MUST so từng thành phần với ngưỡng áp dụng theo FR-024 và xếp bản ghi theo thành phần nặng nhất: Bình thường, Cảnh báo, Nguy hiểm, hoặc "chưa có ngưỡng". *(Nguồn: BR-M04-10, BR-M06-01, 03)*
- **FR-014**: Kết quả Cảnh báo MUST tạo (hoặc gộp) cảnh báo mức Trung bình; kết quả Nguy hiểm đã xác nhận (FR-015 → 017) MUST tạo (hoặc gộp) cảnh báo mức Khẩn cấp. Loại cảnh báo là "vượt ngưỡng: <chỉ số>". *(Nguồn: BR-M06-03)*
- **FR-015**: Bản ghi có kết quả Nguy hiểm MUST chuyển "chờ đo lại" và hệ thống MUST yêu cầu người đo đo lại, trừ khi người đo chọn "xử lý ngay" lúc ghi. Với "xử lý ngay", bản ghi MUST được xác nhận ngay và cảnh báo Khẩn cấp MUST được tạo. *(Nguồn: BR-M06-04)*
- **FR-016**: Lần đo lại MUST trỏ về bản ghi "chờ đo lại" và được so ngưỡng như một lần đo mới; lần đo lại là bản ghi được xác nhận và dùng để tạo cảnh báo (Nguy hiểm → Khẩn cấp, Cảnh báo → Trung bình, Bình thường → không tạo). Lần đo lại MUST NOT yêu cầu đo lại thêm. Bản ghi đầu MUST được giữ với nhãn "đã đo lại". Người đo lại MAY khác người đo đầu, nhưng MUST có quyền ghi chỉ số cho người cao tuổi đó. *(Nguồn: BR-M06-04, BR-M06-06)*
- **FR-017**: Bản ghi "chờ đo lại" quá CFG-M06-04 (đề xuất, mặc định \[10 phút\]) mà chưa có lần đo lại MUST được xác nhận với nhãn "chưa đo lại" và cảnh báo Khẩn cấp MUST được tạo từ bản ghi đó. *(Suy ra từ BR-M06-04; tham số mới, xem Điểm cần báo lại 3)*
- **FR-018**: Công việc đo nguồn MUST chỉ đóng Hoàn thành khi có bản ghi đã xác nhận (lần đo thường, lần đo lại, "xử lý ngay" hoặc hết hạn đo lại); hệ thống MUST báo feature 005 bản ghi đã xác nhận để công việc trỏ tới. *(Nguồn: feature 005 FR-034 dòng "Đo chỉ số")*
- **FR-019**: Bản ghi chỉ số MUST NOT sửa hay xóa. Đính chính MUST có lý do, trỏ đúng một bản gốc (DBR-23), do người ghi gốc, Trưởng tầng hoặc Người phụ trách ca của tầng trong thời gian ca tạo (1.5, feature 000); giá trị đính chính MUST qua FR-011. Giá trị hiện hành sau đính chính MUST được dùng cho các lần đánh giá xu hướng kể từ thời điểm đính chính. Cảnh báo đã tạo từ bản gốc MUST NOT tự đóng; người phụ trách cảnh báo MUST được thông báo "nguồn đã được đính chính". Nếu giá trị đính chính vượt ngưỡng mà bản gốc không vượt, hệ thống MUST tạo (hoặc gộp) cảnh báo theo giá trị đính chính. *(Nguồn: BR-M06-06, feature 005 FR-037)*

#### C. Ngưỡng cảnh báo

- **FR-020**: Mỗi phiên bản ngưỡng MUST gồm: phạm vi (mặc định của cơ sở, hoặc cá nhân của một người cao tuổi), chỉ số, và với từng thành phần: cận dưới nguy hiểm, cận dưới cảnh báo, cận trên cảnh báo, cận trên nguy hiểm (mỗi cận MAY để trống, nghĩa là không so phía đó); thời điểm hiệu lực; người thiết lập; lý do (bắt buộc khi thay phiên bản trước); trạng thái. Ngưỡng cá nhân của một chỉ số MUST thay toàn bộ ngưỡng mặc định của chỉ số đó. *(Nguồn: 10.3, NGUONG_CANH_BAO, BR-M06-01)*
- **FR-021**: Các cận có giá trị MUST thỏa: nằm trong khoảng hợp lệ vật lý; cận dưới nguy hiểm < cận dưới cảnh báo < cận trên cảnh báo < cận trên nguy hiểm (bỏ qua cận để trống). Vi phạm thì chặn. *(Nguồn: 10.3 "hai mức"; suy ra)*
- **FR-022**: Vòng đời phiên bản ngưỡng MUST theo bảng dưới; không có chuyển nào khác. Tại một thời điểm, mỗi (phạm vi, chỉ số) MUST có tối đa một phiên bản Hiệu lực. *(Nguồn: 10.3, 2.4 "Phiên bản", UC-33, constitution IV)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái mới | Người thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Thiết lập ngưỡng (mặc định hoặc cá nhân) | Hiệu lực (thời điểm hiệu lực là thời điểm lệnh) hoặc Chờ hiệu lực | Bác sĩ | FR-020, FR-021; thời điểm hiệu lực không trước thời điểm lệnh; với ngưỡng cá nhân: người cao tuổi chưa ở trạng thái cuối | Phiên bản đang Hiệu lực của cùng (phạm vi, chỉ số) chuyển Hết hiệu lực tại thời điểm hiệu lực; phiên bản Chờ hiệu lực cũ của cùng (phạm vi, chỉ số) chuyển Đã hủy |
| Chờ hiệu lực | Tới thời điểm hiệu lực | Hiệu lực | Bộ lập lịch | — | Như trên |
| Chờ hiệu lực | Hủy phiên bản | Đã hủy | Bác sĩ | Bắt buộc lý do | — |
| Hiệu lực | Phiên bản mới có hiệu lực | Hết hiệu lực | Hệ thống | — | — |
| Hiệu lực | Ngừng ngưỡng cá nhân | Hết hiệu lực | Bác sĩ | Chỉ với ngưỡng cá nhân; bắt buộc lý do | Từ thời điểm đó so theo ngưỡng mặc định |
| Hiệu lực, Chờ hiệu lực | Người cao tuổi chuyển trạng thái cuối | Hết hiệu lực / Đã hủy | Hệ thống | Ngưỡng cá nhân | — |

- **FR-023**: Hết hiệu lực và Đã hủy là trạng thái cuối. Phiên bản ngưỡng MUST NOT sửa được; thay đổi ngưỡng MUST NOT làm thay đổi kết quả so của bản ghi đã lưu. *(Nguồn: 10.3 "được lưu lịch sử", 1.5)*
- **FR-024**: Ngưỡng áp dụng cho một bản ghi MUST là phiên bản ngưỡng cá nhân Hiệu lực tại thời điểm đo; nếu không có thì phiên bản ngưỡng mặc định Hiệu lực tại thời điểm đo; nếu không có thì "chưa có ngưỡng". Một thành phần "vượt ngưỡng nguy hiểm" khi giá trị nhỏ hơn cận dưới nguy hiểm hoặc lớn hơn cận trên nguy hiểm; "vượt ngưỡng cảnh báo" khi không vượt ngưỡng nguy hiểm và nhỏ hơn cận dưới cảnh báo hoặc lớn hơn cận trên cảnh báo; giá trị bằng cận không coi là vượt. *(Nguồn: BR-M06-01, BR-M06-03)*
- **FR-025**: Mỗi ngày Bộ lập lịch MUST tìm người cao tuổi đã Đang lưu trú (tính từ Hoàn tất tiếp nhận) quá CFG-M06-01 (mặc định \[7 ngày\]) mà không có phiên bản ngưỡng cá nhân Hiệu lực hay Chờ hiệu lực nào; với mỗi người, hệ thống MUST gửi nhắc tới các Bác sĩ một lần khi vừa đạt mốc và MUST giữ người đó trong danh sách "chưa có ngưỡng cá nhân" cho tới khi có ngưỡng. *(Nguồn: BR-M06-02, CFG-M06-01; cách nhắc là giả định)*

#### D. Cảnh báo: tạo, gộp, vòng đời, leo thang

- **FR-026**: Cảnh báo MUST được tạo từ: (a) spec này — vượt ngưỡng (FR-014), quy tắc xu hướng (mục E); (b) yêu cầu của feature khác — công việc Bắt buộc quá hạn, uống nước thiếu, ăn kém kéo dài, tâm trạng tiêu cực (feature 005); liều Bỏ lỡ, phản ứng thuốc, từ chối thuốc liên tiếp, xung đột dị ứng với đơn, chưa đối chiếu thuốc, lần dùng PRN vượt giới hạn (feature 006); xung đột dị ứng với chế độ ăn, thực đơn (feature 011); (c) Trưởng tầng, Bác sĩ, Điều dưỡng tạo thủ công (ví dụ hành vi bất thường), bắt buộc mô tả. Yêu cầu từ feature khác MUST nêu người cao tuổi, loại, mức, bản ghi nguồn, thời điểm. *(Nguồn: 9.1, 4.4 dòng "Xử lý cảnh báo", feature 005 FR-044, 006 FR-052)*
- **FR-027**: Mỗi cảnh báo MUST gồm: người cao tuổi, loại, khóa loại (FR-028), mức (Nhẹ / Trung bình / Khẩn cấp), danh sách bản ghi nguồn, số lần (bắt đầu 1), thời điểm lần đầu và lần gần nhất, trạng thái, cấp leo thang hiện tại, người phụ trách, hạn tiếp nhận, người tiếp nhận và thời điểm, kết quả xử lý, người đóng (người hoặc Hệ thống), sự cố được chuyển thành (nếu có). *(Nguồn: 9.4, CANH_BAO)*
- **FR-028**: "Cùng loại" (BR-M05-02, DBR-18) MUST xác định theo khóa loại gồm người cao tuổi, nguồn và đối tượng: vượt ngưỡng → chỉ số; xu hướng → quy tắc; công việc quá hạn → loại công việc (trừ công việc đo từ lịch đo, dùng khóa "bỏ lỡ lần đo theo lịch: <chỉ số>" theo FR-039b); thuốc → loại cảnh báo thuốc và đơn thuốc (xung đột dị ứng, từ chối liên tiếp: theo thuốc); uống nước, ăn kém, tâm trạng, chưa đối chiếu thuốc, nguy cơ cô lập (feature 014) → loại cảnh báo; thủ công → danh mục loại do người tạo chọn. **(Bổ sung 2026-09-28)** Cảnh báo "nguy kịch – thực hiện nguyện vọng cuối đời" MUST NOT gộp theo BR-M05-02: mỗi lần ghi dấu nguy kịch tạo đúng một cảnh báo riêng (FR-050b, BR-M05-15). *(Nguồn: BR-M05-02, BR-M05-15; cách xác định khóa là giả định, xem Điểm cần báo lại 5)*
- **FR-029**: Vòng đời cảnh báo MUST theo bảng dưới; không có chuyển nào khác. "Đang mở" là Mới, Leo thang, Đã tiếp nhận, Đang xử lý. *(Nguồn: 9.4, BR-M05-01 → 03, 05, UC-34, UC-38, DBR-18, constitution IV)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái mới | Người thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Tạo cảnh báo | Mới | Hệ thống; Trưởng tầng, Bác sĩ, Điều dưỡng (thủ công) | Người cao tuổi chưa ở trạng thái cuối; không có cảnh báo đang mở cùng khóa loại (nếu có thì "Phát sinh cùng loại") | Người phụ trách và hạn tiếp nhận theo FR-031; thông báo theo mức (FR-035) |
| Đang mở | Phát sinh cùng loại | Giữ nguyên | Hệ thống | — | Gộp (FR-030) |
| Mới, Leo thang | Quá hạn tiếp nhận | Leo thang | Bộ lập lịch | Chưa ở cấp cao nhất (FR-033) | Lên cấp kế tiếp; hạn mới; ghi lịch sử (BR-M05-01) |
| Mới, Leo thang | Tiếp nhận | Đã tiếp nhận | Điều dưỡng, Trưởng tầng, Bác sĩ (FR-036); Quản lý viện chỉ khi cảnh báo ở Leo thang cấp 2 | — | Người phụ trách là người tiếp nhận; dừng leo thang; lưu thời gian đến tiếp nhận. Lệnh MAY được gọi từ thao tác "Xác nhận và tiếp nhận" trên thông báo Khẩn cấp (feature 009 FR-026a, Q-102): khi đó xác nhận thông báo và lệnh tiếp nhận là hai bản ghi riêng; lệnh tiếp nhận bị từ chối (đã có người tiếp nhận) không làm mất xác nhận |
| Đã tiếp nhận | Giao người phụ trách | Đã tiếp nhận | Quản lý viện (khi là người phụ trách) | Người được giao là Điều dưỡng, Trưởng tầng hoặc Bác sĩ có quyền với người cao tuổi đó; bắt buộc ghi chú | Người phụ trách mới được thông báo; ghi lịch sử |
| Đã tiếp nhận | Bắt đầu xử lý | Đang xử lý | Người phụ trách, hoặc người có quyền FR-036 | — | — |
| Đang xử lý | Đóng cảnh báo | Đã đóng | Điều dưỡng, Trưởng tầng, Bác sĩ | Bắt buộc kết quả xử lý (9.4); cảnh báo từ công việc Bắt buộc quá hạn: bắt buộc lý do (BR-M04-06) | — |
| Đang xử lý | Chuyển sự cố | Chuyển sự cố | Điều dưỡng, Trưởng tầng, Bác sĩ | — | Tạo sự cố liên kết (FR-040) |
| Mới, Leo thang, Đã tiếp nhận | Kích hoạt khẩn cấp từ cảnh báo | Chuyển sự cố | Điều dưỡng, Trưởng tầng, Bác sĩ | Cùng một lệnh ghi lần lượt Tiếp nhận, Bắt đầu xử lý, Chuyển sự cố | Tạo sự cố Khẩn cấp liên kết; quy trình khẩn cấp (mục G) |
| Đang mở | Mọi nguồn đã được xử lý (yêu cầu đóng từ feature 005, 006, 011) | Đã đóng | Hệ thống | FR-032 | Kết quả "nguồn đã được xử lý: <lý do từ feature nguồn>" |
| Đang mở | Nâng mức / Chấp nhận đề xuất nâng mức / Hạ mức | Giữ nguyên | Điều dưỡng, Trưởng tầng, Bác sĩ; hạ từ Khẩn cấp: chỉ Bác sĩ | Bắt buộc lý do (trừ chấp nhận đề xuất) | Nếu Mới hoặc Leo thang: hạn tiếp nhận tính lại theo mức mới từ thời điểm đổi; thông báo theo mức mới khi nâng |
| Đang mở | Bàn giao ca được xác nhận | Giữ nguyên | Hệ thống | Người phụ trách thuộc ca vừa bàn giao | Chuyển người phụ trách (FR-037) |
| Đang mở | Người cao tuổi chuyển trạng thái cuối | Giữ nguyên | Hệ thống | — | Dừng leo thang và nhắc; liệt kê để đóng (FR-038) |

- **FR-030**: Khi có phát sinh cùng khóa loại với một cảnh báo đang mở, hệ thống MUST gộp vào cảnh báo đó trong cùng một lần: tăng số lần, thêm bản ghi nguồn, cập nhật thời điểm gần nhất; MUST NOT tạo cảnh báo mới. Nếu mức của phát sinh cao hơn mức cảnh báo, mức cảnh báo MUST nâng lên mức đó và áp tác động "Nâng mức". Hai phát sinh cùng khóa loại đến gần như đồng thời MUST cho kết quả một cảnh báo đang mở. Phát sinh cùng loại khi không có cảnh báo đang mở (cảnh báo cũ Đã đóng hoặc Chuyển sự cố) MUST tạo cảnh báo mới. *(Nguồn: BR-M05-02, DBR-18)*
- **FR-031**: Hạn tiếp nhận theo mức MUST là: Nhẹ — hết ca hiện tại của tầng người cao tuổi; Trung bình — CFG-M05-01 (mặc định \[15 phút\]) từ thời điểm tạo, nâng mức hoặc leo thang; Khẩn cấp — ngay lập tức: mọi người nhận ở FR-035 được thông báo đồng thời, không leo thang theo cấp, và thông báo chưa được xác nhận xử lý theo BR-M13-02 (CFG-M13-01, mặc định \[5 phút\], feature 009). Người phụ trách ban đầu MUST là Điều dưỡng phụ trách người cao tuổi trong ca (nếu không có thì Người phụ trách ca của tầng). *(Nguồn: 9.3, BR-M05-01, BR-M13-02; người phụ trách ban đầu: xem Điểm cần báo lại 1)*
- **FR-032**: Khi feature nguồn yêu cầu đóng cảnh báo vì nguồn đã được xử lý (công việc hoàn thành đúng khung, liều được đính chính Đã dùng, phiếu đối chiếu được xác nhận, trạng thái cuối…), hệ thống MUST ghi việc xử lý của bản ghi nguồn đó vào lịch sử cảnh báo; nếu mọi bản ghi nguồn của cảnh báo đã được xử lý, cảnh báo MUST chuyển Đã đóng với người đóng là Hệ thống. Đính chính bản ghi chỉ số MUST NOT được coi là nguồn đã được xử lý (FR-019). Nếu cảnh báo đã ở Chuyển sự cố, cảnh báo MUST giữ nguyên trạng thái; hệ thống MUST thêm vào sự cố liên kết một diễn biến "nguồn đã được xử lý: <lý do từ feature nguồn>" (người ghi là Hệ thống) và MUST thông báo người xử lý sự cố; sự cố MUST NOT tự đóng mà được đóng theo bảng FR-045 (BR-M05-09) (Clarification 2026-09-26 lượt 2, đề xuất Q-76). *(Nguồn: feature 005 FR-010, FR-035; feature 006 FR-029, FR-030, FR-036, FR-047)*
- **FR-033**: Chuỗi leo thang MUST là: cấp 0 — người phụ trách ban đầu (FR-031); cấp 1 — Trưởng tầng của tầng người cao tuổi và Bác sĩ trực (một trong hai tiếp nhận là đủ); cấp 2 — Quản lý viện. Với mức Trung bình, mỗi cấp chờ CFG-M05-01 trước khi lên cấp kế tiếp. Với mức Nhẹ, cảnh báo chưa tiếp nhận khi hết ca MUST vào bản nháp bàn giao (BR-M05-05) và lên một cấp. Cấp không có người nhận trong ca MUST được bỏ qua, lên thẳng cấp kế tiếp; vì vậy yêu cầu thông báo leo thang gửi feature 009 MUST khai cách thay "không thay" cho các nhóm người nhận của cấp (feature 009 FR-010, Q-110). Nhân viên thực hiện công việc nguồn (nếu có) MUST được thông báo từ cấp 0 nhưng không là người tiếp nhận. Mỗi lần leo thang MUST ghi lịch sử: thời điểm, cấp cũ, cấp mới, người được thông báo. *(Nguồn: BR-M05-01, UC-38, 18.3 "số lần leo thang"; xem Điểm cần báo lại 1, 2)*
- **FR-034**: Cảnh báo đã ở cấp 2 mà vẫn chưa tiếp nhận MUST NOT leo thang thêm; cảnh báo MUST mang dấu "đã leo thang tối đa" và hiển thị nổi bật trên dashboard của Quản lý viện và Trưởng tầng (18.5) cho tới khi được tiếp nhận. *(Suy ra từ BR-M05-01)*
- **FR-035**: Người nhận thông báo khi tạo cảnh báo MUST theo mức: Nhẹ — người phụ trách ban đầu; Trung bình — người phụ trách ban đầu và nhân viên thực hiện công việc nguồn; Khẩn cấp — theo FR-047a (người liên hệ chính và Quản lý viện chỉ được báo khi có sự cố Khẩn cấp, FR-047). Người nhận MUST xác định theo phân công hiện tại (BR-M13-04); kênh gửi theo feature 009 (BR-M13-01). *(Nguồn: BR-M13-01, 04)*
- **FR-036**: Tiếp nhận, bắt đầu xử lý, đóng, chuyển sự cố, nâng/hạ mức MUST chỉ do Điều dưỡng, Trưởng tầng (trong phạm vi phân công, feature 002 FR-034) hoặc Bác sĩ (toàn viện) thực hiện; Người phụ trách ca có cùng quyền trong tầng và thời gian ca (4.4 chú thích ⁹, feature 002 FR-019a). Nhân viên chăm sóc chỉ xem cảnh báo của người được phân công; Quản lý viện xem mọi cảnh báo; MUST tiếp nhận được cảnh báo đang ở Leo thang cấp 2 và giao người phụ trách (bảng FR-029), nhưng MUST NOT bắt đầu xử lý, đóng, chuyển sự cố hay nâng/hạ mức. *(Nguồn: 4.4, UC-34, BR-M05-01; Clarification 2026-09-26, đề xuất Q-71)*
- **FR-037**: Mọi cảnh báo và sự cố đang mở khi kết thúc ca MUST có trong bản nháp bàn giao (feature 008 BR-M09-06). Khi ca sau xác nhận bàn giao, người phụ trách của mỗi cảnh báo, sự cố mà người phụ trách thuộc ca trước MUST chuyển sang Điều dưỡng phụ trách người cao tuổi trong ca mới (nếu không có thì Người phụ trách ca mới); trạng thái, cấp leo thang và hạn tiếp nhận giữ nguyên. Trước khi xác nhận, người phụ trách chính thức không đổi; ngoại lệ: từ giờ bắt đầu ca sau, nếu bàn giao chưa Đã xác nhận, cảnh báo và sự cố mà người phụ trách thuộc ca trước và không có tên trong ca nào đang diễn ra MUST được chuyển tạm sang Người phụ trách ca sau (không có thì Trưởng tầng của phạm vi), mang dấu "tạm nhận, chờ xác nhận bàn giao", trạng thái, cấp leo thang và hạn tiếp nhận giữ nguyên; người tạm nhận có đủ quyền FR-036 với các mục đó và thay Điều dưỡng phụ trách ở FR-033 (bậc đầu của chuỗi leo thang) và FR-035 (người nhận thông báo), các bậc sau giữ nguyên (feature 008 Q-94); khi bàn giao được xác nhận, quy tắc chuyển ở trên áp cho cả các mục tạm nhận và dấu được gỡ (feature 008 FR-044a). *(Nguồn: BR-M05-05, BR-M09-08; feature 008 Q-78)*
- **FR-038**: Khi người cao tuổi chuyển trạng thái cuối, hệ thống MUST dừng leo thang và nhắc với mọi cảnh báo, sự cố đang mở của người đó, và MUST cung cấp danh sách này cho điều kiện Kết thúc lưu trú và danh sách việc sau qua đời (feature 004 FR-063 (d), FR-071). Cảnh báo có yêu cầu đóng từ feature nguồn tự đóng theo FR-032; cảnh báo, sự cố còn lại MUST được đóng bởi người có quyền, với kết quả xử lý. *(Nguồn: 5.6 "→ Kết thúc lưu trú", 6.8)*
- **FR-038a**: Khi số lần phát sinh mức Trung bình của cùng khóa loại trong 24 giờ trượt (tính cả các lần đã gộp) đạt CFG-M05-02 (mặc định \[3 lần / 24 giờ\]), hệ thống MUST đề xuất nâng cảnh báo lên Khẩn cấp cho người phụ trách và Bác sĩ trực. Người có quyền FR-036 chấp nhận (cảnh báo nâng lên Khẩn cấp) hoặc từ chối (bắt buộc lý do); đề xuất MUST được lặp lại ở mỗi lần phát sinh tiếp theo khi điều kiện còn đúng. *(Nguồn: BR-M05-03, CFG-M05-02)*

#### E. Quy tắc theo xu hướng

- **FR-039**: Hệ thống MUST có danh mục quy tắc xu hướng, mỗi quy tắc có: trạng thái bật/tắt, tham số (mã CFG), mức cảnh báo tạo ra. Quản lý viện MUST bật/tắt và đổi mức; mọi thay đổi ghi nhật ký kèm giá trị trước/sau và lý do (BR-M15-04). Danh mục mặc định: *(Nguồn: BR-M05-04 "cấu hình được")*

| Quy tắc | Tham số | Mức mặc định | Bên phát hiện |
| --- | --- | --- | --- |
| Sụt cân | CFG-M05-03 (mặc định \[5% trong 30 ngày\]) | Trung bình | Spec này (FR-039a) |
| Bỏ lỡ lần đo theo lịch | — | Nhẹ (Trung bình khi công việc Bắt buộc, FR-039b) | Spec này (FR-039b) |
| Mất ngủ kéo dài | CFG-M05-05 (mặc định \[3\] đêm) | Nhẹ | Spec này, từ kết quả ngủ của feature 005 (FR-039c) |
| Từ chối cùng một thuốc | CFG-M05-04 (mặc định \[2\] lần) | Trung bình | feature 006 FR-032 phát hiện, spec này tạo cảnh báo |

- **FR-039a**: Khi một cân nặng được xác nhận, hệ thống MUST so với cân nặng hiện hành cao nhất trong khoảng ngày của CFG-M05-03 trước thời điểm đo; nếu mức giảm (tính trên giá trị cao nhất đó) đạt tỷ lệ của CFG-M05-03, hệ thống MUST tạo (hoặc gộp) cảnh báo "sụt cân" nêu hai lần đo được so, và MUST gửi sự kiện "sụt cân" cho feature 011 để tạo (hoặc gộp nguồn vào) yêu cầu dinh dưỡng viên xem lại chế độ ăn (BR-M08-05, feature 011 FR-058). *(Nguồn: BR-M05-04, BR-M08-05; cách chọn mốc so là giả định)*
- **FR-039b**: Mọi tín hiệu bỏ lỡ của một công việc đo sinh từ lịch đo MUST dồn vào một cảnh báo khóa loại "bỏ lỡ lần đo theo lịch: <chỉ số>": (a) yêu cầu tạo cảnh báo công việc Bắt buộc quá hạn từ feature 005 cho công việc có nguồn là lịch đo MUST được ghi với khóa này, không phải khóa "công việc quá hạn"; (b) công việc kết thúc Không thực hiện (không tính đóng do trạng thái cuối), hoặc tới hết ca vẫn Quá hạn, MUST tạo hoặc gộp vào cảnh báo này. Mức của cảnh báo là mức cao nhất trong các nguồn đã gộp (Trung bình với công việc Bắt buộc, mức cấu hình của quy tắc với công việc khác). Công việc Hủy (vắng mặt, lịch bị ngừng) MUST NOT tính. Một lần đo bị bỏ MUST NOT sinh hai cảnh báo khác khóa loại. *(Nguồn: BR-M05-04, BR-M04-06; Clarification 2026-09-26 lượt 2, đề xuất Q-75)*
- **FR-039c**: Chất lượng ngủ thuộc nhóm "mất ngủ" trong danh mục chất lượng ngủ của feature 005 được ghi CFG-M05-05 đêm liên tiếp MUST tạo (hoặc gộp) cảnh báo "mất ngủ kéo dài"; đêm không có ghi nhận ngủ làm đứt chuỗi; đêm vắng mặt không tính và không làm đứt chuỗi. *(Nguồn: BR-M05-04, feature 005 FR-034 dòng "Ngủ")*

#### F. Sự cố

- **FR-040**: Mỗi sự cố MUST gồm: người cao tuổi (MAY trống chỉ với loại "cơ sở vật chất"), loại (danh mục loại sự cố), nguồn phát sinh (chăm sóc, ăn uống, thuốc, vận động, hoạt động, đi lại, điều trị, hoạt động ngoài viện, phục vụ sai suất ăn, đồ gửi), thời điểm xảy ra (nếu biết), thời điểm phát hiện, địa điểm, hoạt động đang diễn ra, người phát hiện, mô tả, mức độ, xử lý ban đầu, người được thông báo (hệ thống điền từ nhật ký thông báo của feature 009), cảnh báo nguồn (nếu chuyển từ cảnh báo). Sự cố tạo từ cảnh báo MUST lấy mức mặc định bằng mức cảnh báo. Với sự cố do feature 011 yêu cầu khi phục vụ suất có thành phần gây dị ứng (BR-M08-14): loại luôn là "phục vụ sai suất ăn"; nguồn phát sinh là "phục vụ sai suất ăn" khi người cao tuổi chưa ăn (mức Trung bình) và "ăn uống" khi đã ăn (mức do người ghi chọn theo triệu chứng, theo FR-042). Với sự cố do feature 013 yêu cầu (BR-M12-02): loại "đồ gửi thất lạc" hoặc "đồ gửi hư hỏng", nguồn "đồ gửi", mức mặc định Trung bình, có tham chiếu đồ gửi và bản ghi bàn giao; địa điểm là nơi xảy ra. *(Nguồn: 9.1, 9.2, SU_CO; BR-M08-14, đồng bộ spec 011; BR-M12-02, đồng bộ spec 013)*
- **FR-041**: Mọi tài khoản nhân viên có quyền "T" ở dòng "Sự cố, khẩn cấp" MUST ghi nhận được sự cố và kích hoạt khẩn cấp cho bất kỳ người cao tuổi nào chưa ở trạng thái cuối, theo ngoại lệ phạm vi của feature 002 FR-044a (chỉ thấy thông tin nhận dạng với người ngoài phạm vi; nhật ký đánh dấu "ngoài phạm vi"). Quản lý viện MUST ghi nhận được sự cố và kích hoạt khẩn cấp như nhân viên khác (4.1, AC-00), nhưng MUST NOT tiếp nhận xử lý, ghi diễn biến chuyên môn, đổi mức hay đóng sự cố (Clarification 2026-09-26, đề xuất Q-71). Nếu người cao tuổi đã có sự cố Khẩn cấp đang mở, hệ thống MUST hiển thị sự cố đó và cho người ghi chọn tham gia xử lý sự cố đó hoặc tạo sự cố mới; MUST NOT tự gộp sự cố. *(Nguồn: 4.4 dòng "Sự cố, khẩn cấp" chú thích ¹⁰, UC-35, UC-36, BR-M15-01; xem Điểm cần báo lại 2)*
- **FR-042**: Danh mục loại sự cố MUST có mức mặc định cho từng loại (ví dụ Ngã — Khẩn cấp, theo ví dụ 9.3); người ghi chọn mức thấp hơn mức mặc định MUST ghi lý do. Với loại Ngã, tác động của FR-051 MUST áp dụng bất kể mức được chọn (Clarification 2026-09-26, đề xuất Q-70). Danh mục MUST gồm ít nhất: ngã, lây nhiễm nghi ngờ, thuốc, ăn uống, phục vụ sai suất ăn, khó thở / mất ý thức, đi lạc hoặc không trở về (mặc định Khẩn cấp, đồng bộ spec 014), hành vi, chấn thương khác, cơ sở vật chất, đồ gửi thất lạc (mặc định Trung bình), đồ gửi hư hỏng (mặc định Trung bình), khác. Mỗi loại MUST có dấu "không thuộc sức khỏe" do Quản lý viện đặt, mặc định bật cho cơ sở vật chất, đồ gửi thất lạc, đồ gửi hư hỏng; mỗi lần đổi dấu MUST ghi nhật ký với giá trị trước/sau. Dấu chỉ dùng để quyết định Hành chính có thấy số liệu của loại đó trên báo cáo, dashboard hay không (feature 016 FR-042a). *(Nguồn: 9.1, 9.2, 9.3; BR-M12-02, đồng bộ spec 013; Q-202, đồng bộ spec 016)*
- **FR-043**: Khi sự cố được tạo, trong cùng lần ghi hệ thống MUST: (a) gửi thông báo theo mức — Nhẹ: điều dưỡng phụ trách; Trung bình: điều dưỡng phụ trách và Người phụ trách ca; Khẩn cấp: theo FR-047 — kênh theo BR-M13-01; (b) với mức Trung bình trở lên hoặc loại Ngã, yêu cầu feature 001 tạo yêu cầu đánh giá lại lý do "sau sự cố" (BR-M01-02), trừ sự cố loại "đồ gửi thất lạc", "đồ gửi hư hỏng" ở mọi mức (đồng bộ spec 013 FR-022); (c) với loại Ngã, áp FR-051; (d) với loại lây nhiễm nghi ngờ, áp mục I; (e) với mức Khẩn cấp, kích hoạt quy trình khẩn cấp (mục G). *(Nguồn: 9.3, BR-M01-02, BR-M05-06, 07, 10)*
- **FR-044**: Sự cố MUST có hạn tiếp nhận và chuỗi leo thang như cảnh báo cùng mức (FR-031, FR-033); lệnh "Tiếp nhận xử lý" dừng leo thang. *(Nguồn: 9.3 cột "Thời hạn tiếp nhận")*
- **FR-045**: Vòng đời xử lý sự cố MUST theo bảng dưới (các bước 9.4: Ghi nhận → Đánh giá → Xử lý → Thông báo → Theo dõi → Đóng); không có chuyển nào khác. *(Nguồn: 9.4, BR-M05-09, UC-35, constitution IV)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái mới | Người thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Ghi nhận sự cố | Mới | Nhân viên theo FR-041 | FR-040 | FR-043 |
| — | Chuyển sự cố từ cảnh báo / Kích hoạt khẩn cấp từ cảnh báo | Đang xử lý | Điều dưỡng, Trưởng tầng, Bác sĩ | FR-029 | Người xử lý là người thực hiện; FR-043 |
| — | Hệ thống tạo (thiếu người khi trở về từ hoạt động ngoài viện, phục vụ suất có thành phần gây dị ứng) | Mới | Hệ thống | Yêu cầu từ feature 014 (BR-M04-17), feature 011 (BR-M08-14) | FR-043 |
| Mới | Tiếp nhận xử lý | Đang xử lý | Điều dưỡng, Trưởng tầng, Bác sĩ (như FR-036) | — | Dừng leo thang; MAY gọi từ "Xác nhận và tiếp nhận" trên thông báo Khẩn cấp (feature 009 FR-026a, Q-102) |
| Mới | Quá hạn tiếp nhận | Mới (cấp leo thang tăng) | Bộ lập lịch | FR-044 | Ghi lịch sử |
| Mới, Đang xử lý, Đang theo dõi | Ghi diễn biến (đánh giá, hành động, thông báo, theo dõi) | Giữ nguyên | Người đã ghi nhận sự cố; nhân viên có quyền "T" trong phạm vi | — | Bản ghi thêm, có thời điểm và người ghi |
| Mới, Đang xử lý, Đang theo dõi | Nâng mức / Hạ mức | Giữ nguyên | Điều dưỡng, Trưởng tầng, Bác sĩ; hạ từ Khẩn cấp: chỉ Bác sĩ | Bắt buộc lý do | Nâng lên Khẩn cấp: mục G; nâng lên Trung bình: FR-043 (b); thông báo đã gửi không thu hồi |
| Đang xử lý | Chuyển theo dõi | Đang theo dõi | Điều dưỡng, Trưởng tầng, Bác sĩ | Có ít nhất một diễn biến đánh giá và một diễn biến xử lý | — |
| Đang theo dõi | Mở lại xử lý | Đang xử lý | Điều dưỡng, Trưởng tầng, Bác sĩ | Bắt buộc lý do | — |
| Mới, Đang xử lý | Chuyển viện | Giữ nguyên | Điều dưỡng, Bác sĩ | FR-049 | FR-049 |
| Đang xử lý, Đang theo dõi | Đóng sự cố | Đã đóng | Nhẹ: Điều dưỡng, Trưởng tầng, Bác sĩ; Trung bình, Khẩn cấp: Điều dưỡng hoặc Bác sĩ | Bắt buộc kết quả xử lý (BR-M05-09); sự cố lây nhiễm: FR-066 | Dừng nhắc; gỡ khỏi danh sách đang mở |
| Mới, Đang xử lý, Đang theo dõi, Đã đóng | Đính chính "Hủy ghi nhận" | Đã hủy | Người được đính chính theo FR-046 | Bắt buộc lý do | FR-046a |
| Mới, Đang xử lý, Đang theo dõi, Đã đóng | Đính chính đổi loại sự cố | Giữ nguyên | Người được đính chính theo FR-046 | Bắt buộc lý do | FR-046a |
| Mới, Đang xử lý, Đang theo dõi, Đã đóng | Nguồn đồ gửi bị Hủy ghi nhận | Đã hủy | Hệ thống (theo yêu cầu của feature 013) | Sự cố loại "đồ gửi thất lạc" / "đồ gửi hư hỏng" tạo từ bản ghi bị hủy | FR-046b |

- **FR-046**: Đã đóng là trạng thái cuối (trừ đính chính ở bảng FR-045). Nội dung ghi nhận và diễn biến của sự cố MUST NOT sửa hay xóa; đính chính theo feature 000 (người ghi gốc, Trưởng tầng hoặc Người phụ trách ca của tầng). Sự cố Khẩn cấp (kể cả sự cố từng ở mức Khẩn cấp) thuộc danh sách bảo vệ; mọi đính chính MUST có lý do và được ghi nhật ký. Sự cố đã đóng MUST vẫn nhận được bản đính chính. *(Nguồn: BR-M05-08, BR-M15-03, DBR-19, DBR-23)*
- **FR-046a**: Khi sự cố bị "Hủy ghi nhận" hoặc đổi loại, hệ thống MUST NOT tự thu hồi tác động đã tạo (yêu cầu đánh giá lại, yêu cầu xem xét kế hoạch, lịch theo dõi sau ngã, danh sách tiếp xúc, dấu nghi nhiễm/tiếp xúc, vùng khoanh vùng); hệ thống MUST gửi cho Điều dưỡng phụ trách và các Bác sĩ danh sách từng tác động còn hiệu lực kèm lệnh đóng tương ứng (ngừng lịch đo, loại người khỏi danh sách tiếp xúc, gỡ khoanh vùng, đóng yêu cầu ở feature 001, 005) để người có quyền tự quyết định. Đổi loại **sang** Ngã MUST tạo ngay các tác động của FR-051 tại thời điểm đính chính; đổi loại sang "lây nhiễm nghi ngờ" MUST áp mục I từ thời điểm đính chính; đổi loại **khỏi** Ngã hoặc lây nhiễm xử lý như "Hủy ghi nhận" với các tác động của loại cũ. Thông báo đã gửi MUST NOT bị thu hồi. Đã hủy là trạng thái cuối; sự cố Đã hủy vẫn xem được và không tính vào báo cáo. *(Nguồn: 1.5 "Hủy ghi nhận", BR-M05-07, BR-M05-10; Clarification 2026-09-26 lượt 2, đề xuất Q-73)*
- **FR-046b**: Sự cố loại "đồ gửi thất lạc", "đồ gửi hư hỏng" MUST chuyển Đã hủy khi feature 013 báo bản ghi bàn giao đã tạo nó bị "Hủy ghi nhận" (feature 013 FR-011a), với lý do "ghi nhầm đồ gửi" và tham chiếu bản đính chính ở feature 013; không cần lệnh đính chính riêng ở feature này. Thông báo đã gửi không bị thu hồi (FR-046a); feature 013 báo lại người đã nhận. Lệnh "Đóng sự cố" với hai loại này theo FR-045; diễn biến "đã tìm thấy" do feature 013 ghi không tự đóng sự cố. *(Nguồn: đồng bộ spec 013, Q-160)*

#### G. Quy trình khẩn cấp và chuyển viện

- **FR-047**: Quy trình khẩn cấp MUST được kích hoạt khi một sự cố ở mức Khẩn cấp được tạo (từ bất kỳ nguồn nào) hoặc được nâng lên Khẩn cấp. Khi kích hoạt, trong cùng lúc hệ thống MUST: (a) phát thông báo khẩn cấp song song (không tuần tự) cho Bác sĩ trực, Trưởng tầng của tầng người cao tuổi, Quản lý viện, người liên hệ chính (BR-M05-06), điều dưỡng phụ trách và Người phụ trách ca; (b) hiển thị thẻ thông tin khẩn cấp (FR-048); (c) lưu bản chụp nội dung thẻ tại thời điểm kích hoạt vào sự cố. Thông báo cho người liên hệ chính MUST chỉ gồm "có tình huống khẩn cấp liên quan [họ tên người cao tuổi], đề nghị liên hệ viện", thời điểm và số liên hệ của viện, trừ khi người đó nằm trong bản đồng ý chia sẻ dữ liệu đang hiệu lực (khi đó MAY kèm loại sự cố và mô tả ngắn); MUST NOT chứa chẩn đoán, chỉ số, thuốc hay nguyện vọng cuối đời. Nếu tại thời điểm kích hoạt không có Bác sĩ trực, hệ thống MUST áp FR-047b. Kích hoạt khẩn cấp MUST thực hiện trực tuyến, MUST NOT lưu tạm trên thiết bị (8.6, Q-01). *(Nguồn: 9.5, BR-M05-06, BR-M05-13, BR-M13-01, NFR-03, DBR-03; Clarification 2026-09-26 lượt 2, đề xuất Q-72)*
- **FR-047a**: Cảnh báo mức Khẩn cấp (vượt ngưỡng nguy hiểm, nâng mức, gộp) MUST NOT tự tạo sự cố hay kích hoạt quy trình khẩn cấp. Khi cảnh báo được tạo hoặc nâng lên Khẩn cấp, hệ thống MUST thông báo đồng thời (mọi kênh, song song, BR-M13-01) cho Điều dưỡng phụ trách, Người phụ trách ca, Bác sĩ trực và Trưởng tầng của tầng người cao tuổi; MUST NOT thông báo người liên hệ chính hay Quản lý viện ở bước này. Nếu không có Bác sĩ trực, hệ thống MUST áp FR-047b. Quy trình khẩn cấp (FR-047) chỉ bắt đầu khi người có quyền chọn "Kích hoạt khẩn cấp từ cảnh báo" (bảng FR-029). *(Nguồn: 9.1, 9.3, BR-M06-03, BR-M05-06; Clarification 2026-09-26, đề xuất Q-67)*
- **FR-047b**: Khi thông báo Khẩn cấp (sự cố theo FR-047 hoặc cảnh báo theo FR-047a) cần gửi Bác sĩ trực mà trong ca hiện tại không có bác sĩ nào giữ nhiệm vụ Bác sĩ trực (2.4), hệ thống MUST gửi song song cho mọi Bác sĩ đang hoạt động của cơ sở, MUST gắn dấu "không có Bác sĩ trực tại thời điểm" vào sự cố hoặc cảnh báo, và MUST báo Quản lý viện (kể cả với cảnh báo, như ngoại lệ của FR-047a). Quy tắc này không áp cho leo thang cấp 1 của cảnh báo Trung bình (FR-033 bỏ qua người nhận vắng). *(Nguồn: BR-M05-06, 2.4 "Bác sĩ trực"; Clarification 2026-09-26 lượt 2, đề xuất Q-74)*
- **FR-048**: Thẻ thông tin khẩn cấp MUST gồm: họ tên, ảnh, ngày sinh, phòng/giường; nguyện vọng cuối đời theo phiên bản Hiệu lực của feature 001 FR-023a (lựa chọn khi nguy kịch: chuyển bệnh viện điều trị tích cực / đưa về nhà / ở lại viện chăm sóc giảm nhẹ; người cần liên hệ trước; ghi chú; người ký, thời điểm), hoặc "chưa ghi nhận nguyện vọng cuối đời"; dấu nguy kịch nếu đang có (FR-050a); dị ứng Hiệu lực kèm mức độ; thuốc đang dùng (feature 006 FR-050); bệnh nền Hiệu lực; cờ nguy cơ (BR-M01-10); người liên hệ chính và số điện thoại. Thẻ MUST hiển thị cho người kích hoạt và người xử lý có quyền xem thông tin sức khỏe của người cao tuổi đó (theo feature 002); người kích hoạt ngoài phạm vi MUST chỉ thấy thông tin nhận dạng và thông điệp "đã thông báo người xử lý" (feature 002 FR-044a). *(Nguồn: 9.5, BR-M05-13, BR-M01-10, feature 001 FR-023, FR-038; xem Điểm cần báo lại 6)*
- **FR-048a**: Nếu người cao tuổi có nguyện vọng cuối đời đã ghi nhận, hệ thống MUST chặn ghi diễn biến loại "biện pháp hồi sức" cho tới khi sự cố có xác nhận "đã đối chiếu nguyện vọng" (người xác nhận, thời điểm) của người đã được hiển thị thẻ; lúc xác nhận, hệ thống MUST hiển thị lại nội dung nguyện vọng. Không có nguyện vọng đã ghi nhận thì MUST NOT yêu cầu xác nhận. *(Nguồn: BR-M05-13, DBR-19)*
- **FR-048b**: Hồ sơ sự cố Khẩn cấp MUST cho ghi được và hiển thị được: thời điểm phát hiện, người xử lý (mọi người đã ghi diễn biến hoặc tham gia), hành động đã thực hiện (theo thời gian), thời điểm gọi hỗ trợ và thời điểm gọi cấp cứu, kết quả, người được thông báo kèm thời điểm gửi và thời điểm đã xem (từ feature 009). *(Nguồn: 9.5)*
- **FR-049**: Điều dưỡng hoặc Bác sĩ MUST thực hiện được "Chuyển viện" từ một sự cố Mới hoặc Đang xử lý, kèm cơ sở tiếp nhận và thời điểm. Trong cùng một lệnh hệ thống MUST: thực hiện lệnh Chuyển viện của feature 001/004 với căn cứ là sự cố này (điều kiện 5.6); và tạo bản tóm tắt chuyển viện (FR-050). Nếu lệnh Chuyển viện bị từ chối (ví dụ trạng thái hiện tại không cho phép), không phần nào được áp dụng và sự cố ghi lại lần thử kèm lý do bị từ chối. *(Nguồn: BR-M05-14, 5.6, UC-16, feature 000)*
- **FR-050**: Bản tóm tắt chuyển viện MUST gồm: thông tin nhận dạng, dị ứng Hiệu lực, thuốc đang dùng (feature 006 FR-050), giá trị hiện hành gần nhất của từng chỉ số kèm thời điểm đo, diễn biến sự cố tới thời điểm chuyển, nguyện vọng cuối đời (nếu có), cơ sở tiếp nhận, người lập, thời điểm. Bản tóm tắt MUST in được và xem được bởi Bác sĩ, Điều dưỡng, Trưởng tầng trong phạm vi và Quản lý viện. *(Nguồn: BR-M05-14; nguyện vọng cuối đời là bổ sung, xem Điểm cần báo lại 8)*

#### G1. Nguy kịch và thực hiện nguyện vọng cuối đời *(bổ sung 2026-09-28)*

- **FR-050a**: Bác sĩ MUST ghi được **dấu nguy kịch** cho người cao tuổi đang Đang lưu trú, Tạm vắng hoặc Hoạt động bên ngoài, bắt buộc nhận định. Khi trong ca không có Bác sĩ trực, Điều dưỡng MUST ghi được **dấu tạm**, bắt buộc nhận định; hệ thống báo mọi Bác sĩ đang hoạt động để xác nhận thành dấu chính thức hoặc gỡ. Mỗi người cao tuổi có tối đa một dấu nguy kịch đang mở (DBR-34). Dấu nguy kịch không phải trạng thái ở 5.5. *(Nguồn: 9.5, UC-83, Q-218)*

**Bảng trạng thái dấu nguy kịch** *(9.5, BR-M05-15, Q-218)*:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái mới | Người thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Ghi dấu nguy kịch | Đang mở | Bác sĩ | Có nhận định; chưa có dấu đang mở | Tạo cảnh báo nguy kịch (FR-050b) |
| — | Ghi dấu tạm | Tạm | Điều dưỡng | Không có Bác sĩ trực trong ca; có nhận định | Tạo cảnh báo nguy kịch (FR-050b); báo mọi Bác sĩ đang hoạt động xác nhận |
| Tạm | Xác nhận | Đang mở | Bác sĩ | — | Ghi người xác nhận, thời điểm |
| Đang mở, Tạm | Gỡ dấu | Đã gỡ | Bác sĩ | Bắt buộc lý do | Ghi lịch sử; cảnh báo nguy kịch còn mở vẫn phải đóng theo FR-050d |
| Đang mở, Tạm | Người cao tuổi chuyển Điều trị tại bệnh viện hoặc trạng thái cuối | Đã gỡ | Hệ thống | — | Lý do "tự gỡ theo trạng thái" |

Đã gỡ là trạng thái cuối; ghi dấu mới tạo dấu và cảnh báo mới.

- **FR-050b**: Khi dấu nguy kịch (kể cả dấu tạm) được ghi, trong cùng một lần hệ thống MUST: (a) tạo cảnh báo mức Khẩn cấp loại "nguy kịch – thực hiện nguyện vọng cuối đời", không gộp (FR-028), hiển thị phiên bản nguyện vọng Hiệu lực (feature 001 FR-023a) hoặc dấu "chưa có nguyện vọng"; (b) thông báo đồng thời Bác sĩ trực (không có thì theo FR-047b), Điều dưỡng phụ trách, Trưởng tầng, Quản lý viện; (c) thông báo người liên hệ chính và mọi người đại diện ở mức Khẩn cấp, nội dung tối thiểu như FR-047 với người không thuộc bản đồng ý (Q-72), MUST NOT chứa nội dung nguyện vọng; (d) tạo **yêu cầu xác nhận lại nguyện vọng** qua cơ chế yêu cầu gọi điện của feature 009, bắt đầu từ người đại diện, theo thứ tự gọi của BR-M13-02. Cảnh báo này không tự kích hoạt quy trình khẩn cấp (FR-047a); nếu có sự cố khẩn cấp cùng lúc thì FR-047, FR-048a vẫn áp dụng. *(Nguồn: BR-M05-15, BR-M05-16, BR-M13-02, 9.5)*
- **FR-050c**: Bác sĩ hoặc Điều dưỡng MUST ghi được **kết quả xác nhận lại**: "giữ nguyện vọng", "nguyện vọng mới" (hệ thống yêu cầu feature 001 ghi phiên bản mới theo FR-023b, người trả lời và người ký theo FR-023c), hoặc "không liên lạc được". Mỗi kết quả ghi: người được liên hệ, kênh (gọi điện, trực tiếp), thời điểm, người ghi. Khi "không liên lạc được" với mọi người trong thứ tự gọi: có nguyện vọng Hiệu lực thì làm theo nguyện vọng đó; chưa có thì Bác sĩ quyết định theo chuyên môn và bắt buộc lý do. *(Nguồn: BR-M05-16)*
- **FR-050d**: Người xử lý MUST ghi **lựa chọn được thực hiện** và thực hiện lệnh tương ứng: "chuyển bệnh viện điều trị tích cực" → Chuyển viện (FR-049, kèm bản tóm tắt FR-050); "đưa về nhà" → yêu cầu feature 004 Cho tạm vắng với lý do "về nhà theo nguyện vọng cuối đời" (Hành chính hoặc Trưởng tầng thực hiện qua quy trình đón; Hành chính lập hồ sơ kết thúc lưu trú khi gia đình quyết định, Q-218); "ở lại viện chăm sóc giảm nhẹ" → yêu cầu feature 005 tạo yêu cầu xem xét kế hoạch chăm sóc (BR-M04-20). Cảnh báo nguy kịch MUST chỉ được đóng khi đã có kết quả xác nhận lại (FR-050c) và lựa chọn đã ghi; đóng bắt buộc kết quả. *(Nguồn: 9.5, BR-M05-15, UC-84)*
- **FR-050e**: Cảnh báo nguy kịch theo vòng đời FR-029 (tiếp nhận, xử lý, đóng), với điều kiện đóng bổ sung của FR-050d. Leo thang theo mức Khẩn cấp (mọi người nhận được báo đồng thời, không leo thang theo cấp). *(Nguồn: 9.3, 9.4)*
- **FR-050f**: Thẻ thông tin khẩn cấp và danh sách người cao tuổi của Bác sĩ, Điều dưỡng, Trưởng tầng MUST hiển thị dấu nguy kịch đang mở. Nội dung nguyện vọng chỉ hiện với người có quyền xem sức khỏe (feature 001 FR-023e). *(Nguồn: 9.5, 19.3)*
- **FR-050g**: Mọi kết quả xác nhận lại và lựa chọn thực hiện là bản ghi nhóm 3, gắn với cảnh báo nguy kịch; sai sót xử lý bằng đính chính. *(Nguồn: BR-M05-16, 1.5)*

#### H. Sự cố ngã

- **FR-051**: Khi sự cố loại Ngã được ghi nhận, trong cùng lần ghi hệ thống MUST: yêu cầu feature 001 tạo yêu cầu đánh giá lại lý do "sau sự cố ngã" (BR-M01-02); yêu cầu feature 005 tạo yêu cầu xem xét kế hoạch chăm sóc (BR-M04-20, feature 005 FR-011); tạo lịch đo nguồn "theo dõi sau ngã" gồm các chỉ số thuộc nhóm "sinh hiệu" (FR-001), mỗi khoảng của CFG-M05-06 (mặc định \[4 giờ\] trong \[72 giờ\]), bắt đầu từ thời điểm xảy ra (nếu có, không sớm hơn thời điểm ghi nhận trừ khoảng của CFG-M05-06) hoặc thời điểm ghi nhận, lần đo đầu tiên sau một khoảng. Nếu một phần thất bại thì không phần nào được áp dụng. *(Nguồn: BR-M05-07, CFG-M05-06, feature 000)*
- **FR-052**: Mỗi người cao tuổi MUST có tối đa một lịch "theo dõi sau ngã" Hiệu lực; lần ngã mới trong khi lịch cũ còn Hiệu lực MUST chuyển lịch cũ Đã ngừng (lý do "thay bằng lịch theo dõi mới") và tạo lịch mới theo FR-051. Bác sĩ MUST ngừng được lịch theo dõi sau ngã sớm, kèm lý do. *(Suy ra từ BR-M05-07)*

#### I. Sự cố lây nhiễm, truy vết và khoanh vùng

- **FR-060**: Khi sự cố loại "lây nhiễm nghi ngờ" được ghi nhận, người cao tuổi của sự cố MUST mang dấu "nghi nhiễm" cho tới khi sự cố đóng, và hệ thống MUST lập danh sách tiếp xúc ở trạng thái Đề xuất gồm, trong CFG-M05-07 (mặc định \[5 ngày\]) trước thời điểm phát hiện: (a) người cao tuổi có phân bổ cùng phòng chồng thời gian (feature 003 FR-035); (b) người cao tuổi cùng điểm danh tham gia một buổi hoạt động (feature 014); (c) nhân viên được phân công chăm sóc người đó (feature 008); (d) người thân có lượt thăm người đó ở trạng thái Đã vào hoặc Đã ra, và người đi cùng thực tế vào của các lượt đó (chỉ có họ tên, quan hệ, số điện thoại nếu có; feature 012 FR-038). Mỗi người trong đề xuất MUST kèm nguồn và khoảng thời gian tiếp xúc; các khoảng người nghi nhiễm vắng mặt MUST được đánh dấu (feature 003 FR-036). Danh sách tiếp xúc cuối cùng MUST được xác nhận, bổ sung hoặc loại người bởi Điều dưỡng (khi người nghi nhiễm thuộc phạm vi phân công) hoặc Bác sĩ (toàn viện); Quản lý viện và Trưởng tầng chỉ xem. *(Nguồn: BR-M05-10, CFG-M05-07, 9.6; Clarification 2026-09-26, đề xuất Q-68; khoảng thời gian dùng chung cho mọi nguồn là giả định, xem Điểm cần báo lại 10)*
- **FR-061**: Vòng đời danh sách tiếp xúc MUST theo bảng dưới; không có chuyển nào khác. "Người xác nhận" là Điều dưỡng (trong phạm vi) hoặc Bác sĩ (FR-060). *(Nguồn: BR-M05-10, 11, constitution IV)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái mới | Người thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Sự cố lây nhiễm nghi ngờ được ghi nhận | Đề xuất | Hệ thống | FR-060 | Người xác nhận được thông báo |
| Đề xuất | Làm mới đề xuất | Đề xuất | Người xác nhận | — | Tính lại (a)–(d); giữ các người đã thêm thủ công và các người đã loại kèm lý do |
| Đề xuất | Xác nhận danh sách | Đã xác nhận | Người xác nhận | Mỗi người bị loại khỏi đề xuất có lý do; mỗi người thêm thủ công có lý do và nguồn | FR-062 |
| Đã xác nhận | Bổ sung người tiếp xúc | Đã xác nhận | Người xác nhận | Bắt buộc lý do | FR-062 cho người mới |
| Đã xác nhận | Loại người khỏi danh sách | Đã xác nhận | Người xác nhận | Bắt buộc lý do | Lịch theo dõi tiếp xúc của người đó Đã ngừng; gỡ dấu "tiếp xúc" |
| Đề xuất, Đã xác nhận | Sự cố lây nhiễm được đóng | Đã kết thúc | Hệ thống | — | Dấu "tiếp xúc" giữ tới hết thời gian theo dõi; lịch theo dõi tiếp tục tới khi hết hạn, trừ khi Bác sĩ ngừng |

- **FR-062**: Khi danh sách được xác nhận hoặc bổ sung, với mỗi người mới: người cao tuổi MUST mang dấu "tiếp xúc" và được tạo lịch đo nhiệt độ nguồn "theo dõi tiếp xúc" theo CFG-M05-08 (mặc định \[2 lần/ngày trong 7 ngày\]) từ thời điểm xác nhận (lịch trùng thời gian của cùng người cao tuổi được gộp thành một lịch kéo tới thời điểm kết thúc muộn nhất); Quản lý viện MUST được thông báo đầy đủ; nhân viên và người thân trong danh sách MUST được thông báo (qua feature 009; người thân gửi theo nhóm "người thân được nêu trong bản ghi nguồn", chỉ nhận phần loại "chung", feature 009 FR-007 (d), Q-112) với nội dung chỉ gồm "có thể đã tiếp xúc tại [tầng/khu vực, khoảng thời gian], đề nghị làm theo hướng dẫn của viện" và số liên hệ, MUST NOT nêu danh tính, phòng hay tình trạng của người nghi nhiễm. Người thân không có tài khoản hay kênh nhận thông báo được liệt kê cho Hành chính liên hệ trực tiếp với cùng nội dung. Dấu "nghi nhiễm" và "tiếp xúc" MUST sẵn có cho feature 003 (khử khuẩn thay vệ sinh trả giường, feature 003 FR-045). *(Nguồn: BR-M05-11, CFG-M05-08, 9.6 "thực hiện thông báo theo quy định", DBR-03, 19.3; Clarification 2026-09-26 lượt 2, đề xuất Q-72)*
- **FR-063**: Bác sĩ hoặc Quản lý viện MUST thực hiện được "Khoanh vùng" một vùng gồm một hoặc nhiều tầng, hoặc toàn bộ một khu vực (cấu trúc 2.2), kèm lý do và gắn ít nhất một sự cố lây nhiễm chưa đóng. Vùng bao mọi phòng và khu vực chung thuộc các tầng/khu vực đã chọn. Khoanh vùng MUST NOT chọn từng phòng riêng lẻ; cô lập một phòng dùng lệnh "Đặt cách ly phòng" của feature 003 (FR-008). *(Nguồn: 9.6, UC-37, BR-M05-11, 4.4 dòng "Khoanh vùng lây nhiễm"; Clarification 2026-09-26, đề xuất Q-69)*
- **FR-064**: Vòng đời khoanh vùng MUST theo bảng dưới; không có chuyển nào khác. *(Nguồn: BR-M05-11, 12, KHOANH_VUNG, constitution IV)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái mới | Người thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Khoanh vùng | Đang khoanh vùng | Bác sĩ, Quản lý viện | FR-063 | FR-065; thông báo Trưởng tầng, Hành chính, điều dưỡng có ca trong vùng; dashboard (18.5) |
| Đang khoanh vùng | Gỡ khoanh vùng | Đã gỡ | Bác sĩ, Quản lý viện | Bắt buộc lý do | Bỏ các chặn của FR-065 với phần không còn vùng nào khác bao (BR-M05-12); feature 003 hủy khử khuẩn định kỳ và sinh khử khuẩn kết thúc (feature 003 FR-046) |

- **FR-065**: Trong thời gian một vùng Đang khoanh vùng, hệ thống MUST cung cấp trạng thái "đang chịu khoanh vùng" cho mọi phòng, khu vực chung và người cao tuổi có giường trong vùng, để: chặn đăng ký thăm mới cho người cao tuổi trong vùng (feature 012, BR-M10-02); chặn đăng ký mới vào hoạt động chung của người cao tuổi trong vùng và buổi hoạt động diễn ra trong vùng, đồng thời để feature 014 tự hủy buổi và đăng ký đã có trong vùng (feature 014 FR-021, BR-M04-15, BR-M05-11, Q-164); chặn phân bổ giường mới và đăng ký lịch đến khu nghỉ bán trú trong vùng (feature 003 FR-009, FR-042); sinh công việc khử khuẩn (feature 003 FR-046). Lượt thăm đã duyệt trong vùng được feature 012 tự hủy (feature 012 FR-037, Q-120); danh sách lượt bị hủy và đăng ký hoạt động đã có trong thời gian khoanh vùng MUST được liệt kê cho Hành chính và Trưởng tầng. *(Nguồn: BR-M05-11, 9.6 "hạn chế hoạt động/thăm nom theo chính sách")*
- **FR-066**: Sự cố lây nhiễm MUST NOT đóng được khi còn vùng Đang khoanh vùng chỉ gắn với sự cố đó. Kết quả xử lý của sự cố lây nhiễm MUST chọn một trong: xác nhận nhiễm, loại trừ, không xác định. Việc đã báo cơ quan y tế (nếu có) MUST ghi được như một diễn biến (thời điểm, người báo, nơi nhận). *(Suy ra từ BR-M05-12, 9.6)*

#### J. Phạm vi y tế và khám, điều trị

- **FR-070**: Quản lý viện MUST lưu được giấy phép hoạt động của cơ sở gồm: số, cơ quan cấp, ngày cấp, ngày hết hạn, phạm vi hoạt động (có hay không có phạm vi khám bệnh, chữa bệnh; danh sách phạm vi chuyên môn được phép), bản scan. *(Nguồn: 10.4 "(Bổ sung)", 4.4 dòng "Tài khoản, phân quyền, tham số": QL C)*
- **FR-071**: Cơ sở MUST được coi là có phạm vi khám bệnh, chữa bệnh cho một phạm vi chuyên môn khi và chỉ khi có giấy phép còn hiệu lực (tới hết ngày hết hạn, feature 002 FR-038) bao gồm phạm vi đó. Hệ thống MUST NOT có công tắc riêng khác với giấy phép; MUST NOT coi "có bác sĩ" là có phạm vi khám chữa bệnh. Dữ liệu này MUST sẵn có cho feature 002 FR-037 (c). *(Nguồn: 10.4, BR-M06-05)*
- **FR-072**: Bác sĩ MUST ghi nhận được khám, đánh giá, chẩn đoán, chỉ định, điều trị, kết quả cho người cao tuổi, mỗi bản ghi gồm: loại, nội dung, cơ sở thực hiện (viện hoặc cơ sở bên ngoài), người thực hiện, thời điểm, đính kèm. Điều dưỡng MUST ghi được bản ghi có cơ sở thực hiện bên ngoài (bắt buộc tên cơ sở, người thực hiện, ngày) và bản ghi loại khám, kết quả tại viện. *(Nguồn: 10.2; quyền là giả định, xem Điểm cần báo lại 9)*
- **FR-073**: Bản ghi chẩn đoán, chỉ định, điều trị với cơ sở thực hiện là viện MUST chỉ được lưu khi, tại thời điểm lưu, thao tác thỏa kiểm tra nghiệp vụ chuyên môn "chẩn đoán và kê đơn nội bộ" (feature 002 FR-036, FR-037): cơ sở có phạm vi khám chữa bệnh phù hợp còn hiệu lực và bác sĩ có giấy phép còn hiệu lực đúng phạm vi. *(Nguồn: BR-M06-05, 10.2 "phạm vi thực hiện tại viện phụ thuộc...")*
- **FR-074**: Bản ghi khám và điều trị MUST NOT sửa hay xóa; đính chính theo feature 000. *(Nguồn: 1.5 nhóm 3)*
- **FR-075**: Giấy phép cơ sở đã lưu MUST NOT sửa hay xóa; gia hạn hoặc thay đổi phạm vi MUST tạo bản giấy phép mới; sai sót nhập liệu MUST xử lý bằng bản mới có lý do, bản cũ đánh dấu "được thay thế do sai sót" và không còn được dùng để xét hiệu lực. Mọi thao tác MUST được ghi nhật ký. Cảnh báo giấy phép sắp hết hạn theo feature 002 FR-039 (CFG-M06-02, mặc định \[60 ngày\]). *(Nguồn: 10.4, BR-M06-05, 19.4 "đặc biệt quan trọng với sức khỏe")*

#### K. Quyền xem, dữ liệu cung cấp và giao tiếp

- **FR-080**: Quyền MUST theo 4.4 (quyền tối đa, còn bị giới hạn bởi phạm vi và điều kiện pháp lý, BR-M15-01): *(Nguồn: 4.4, 19.3, BR-M01-08, DBR-03)*

| Chức năng | QL | TT | BS | ĐD | CS | DDV, Bếp, VS | HC | NT |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Chỉ số (UC-32) | Xem | Xem trong phạm vi | Xem | Ghi, xem trong phạm vi | Ghi, xem trong phạm vi | — | — | Xem khi có bản đồng ý (¹) |
| Ngưỡng, lịch đo (UC-33) | Xem | — | Thiết lập | Xem | — | — | — | — |
| Cảnh báo (UC-34, UC-38) | Xem; tiếp nhận và giao khi Leo thang cấp 2 | Xử lý trong phạm vi | Xử lý | Xử lý trong phạm vi | Xem trong phạm vi | — | — | — |
| Sự cố, khẩn cấp (UC-35, UC-36) | Xem, ghi, kích hoạt khẩn cấp | Ghi (¹⁰), xử lý trong phạm vi | Ghi, xử lý | Ghi, xử lý trong phạm vi | Ghi (¹⁰) | Ghi (¹⁰) | Ghi (¹⁰), chỉ thông tin nhận dạng | Xem khi có bản đồng ý (¹) |
| Khoanh vùng (UC-37) | Thực hiện | Xem | Thực hiện | Xem | — | — | Xem | — |
| Danh sách tiếp xúc | Xem | Xem trong phạm vi | Xác nhận | Xác nhận trong phạm vi | — | — | — | — |
| Giấy phép cơ sở | Cấu hình | — | Xem | — | — | — | — | — |
| Khám và điều trị | Xem | Xem trong phạm vi | Ghi (FR-073) | Ghi (FR-072), xem trong phạm vi | — | — | — | Xem khi có bản đồng ý (¹) |
| Dấu nguy kịch, xác nhận lại nguyện vọng (UC-83, UC-84; bổ sung 2026-09-28) | Xem | Xem trong phạm vi | Ghi, gỡ dấu; xác nhận lại; thực hiện lựa chọn | Dấu tạm khi không có Bác sĩ trực (Q-218); xác nhận lại, thực hiện lựa chọn trong phạm vi | — | — | — | Nhận thông báo; xem khi có bản đồng ý (¹) |

- **FR-081**: Hệ thống MUST cung cấp cho báo cáo sức khỏe và dashboard (18.3, 18.5, feature 016): *(bổ sung 2026-09-29)* người cao tuổi đang mang dấu nguy kịch (Đang mở, Tạm) kèm tầng; chỉ số theo thời gian; số cảnh báo, sự cố theo mức, loại, tầng; thời gian trung bình từ tạo cảnh báo đến tiếp nhận theo mức (mốc tạo là lần tạo đầu tiên, không đặt lại khi gộp; mốc tiếp nhận với cảnh báo kích hoạt khẩn cấp là thời điểm kích hoạt, với cảnh báo Quản lý viện tiếp nhận ở cấp 2 là thời điểm Quản lý viện tiếp nhận); số lần leo thang của cảnh báo; **số lần leo thang của sự cố và thời điểm sự cố được tiếp nhận xử lý (chuyển Đang xử lý), theo mức (Q-199)**; trường hợp đang theo dõi (lịch theo dõi sau ngã, theo dõi tiếp xúc Hiệu lực); khu đang khoanh vùng; cảnh báo "đã leo thang tối đa"; dấu "không thuộc sức khỏe" của từng loại sự cố (FR-042). Định nghĩa là căn cứ cho feature 016:
  - **lần đo** là bản ghi chỉ số đã xác nhận; bản ghi "chờ đo lại" đã được thay bằng lần đo lại không được tính (tính lần đo lại); bản ghi "chờ đo lại" tự xác nhận sau CFG-M06-04 được tính;
  - **vượt ngưỡng** chia hai mức, cảnh báo và nguy hiểm, theo ngưỡng đang hiệu lực lúc đo (ngưỡng cá nhân nếu có, không thì ngưỡng mặc định); chỉ số nhiều thành phần (ví dụ huyết áp) tính một lần ở mức cao nhất của các thành phần;
  - **mức dùng để xếp:** số cảnh báo, sự cố theo mức dùng mức hiện hành; thời gian từ tạo đến tiếp nhận theo mức dùng mức tại thời điểm tiếp nhận, vì hạn tiếp nhận và leo thang chạy theo mức lúc đó.

  *(Nguồn: 18.3, 18.5; FR-016, FR-033, FR-044; Q-199, Q-202, đồng bộ spec 016)*
- **FR-082**: Hệ thống MUST cung cấp cho bản tin định kỳ (feature 012): cân nặng và xu hướng, các chỉ số chính, sự cố trong kỳ kèm mức (BR-M10-09 yêu cầu giải thích khi có sự cố Trung bình trở lên). *(Nguồn: 14.5, BR-M10-09)*
- **FR-083**: Bảng tổng hợp giao tiếp với feature khác:

| Chiều | Feature | Sự kiện / dữ liệu |
| --- | --- | --- |
| Nhận | 001 | Nguyện vọng cuối đời (phiên bản Hiệu lực, feature 001 FR-023a), dị ứng, bệnh nền, cờ nguy cơ (FR-048); trạng thái người cao tuổi; kết quả lệnh Chuyển viện |
| Cung cấp | 001 | *(bổ sung 2026-09-28)* Yêu cầu ghi phiên bản nguyện vọng mới khi kết quả xác nhận lại là "nguyện vọng mới" (FR-050c, feature 001 FR-023b) |
| Cung cấp | 004, 005 | *(bổ sung 2026-09-28)* Lựa chọn "đưa về nhà": yêu cầu Cho tạm vắng lý do "về nhà theo nguyện vọng cuối đời" (feature 004); lựa chọn "chăm sóc giảm nhẹ": yêu cầu xem xét kế hoạch chăm sóc (feature 005) (FR-050d) |
| Cung cấp | 009 | *(bổ sung 2026-09-28)* Thông báo nguy kịch và yêu cầu xác nhận lại nguyện vọng theo cơ chế yêu cầu gọi điện (FR-050b) |
| Cung cấp | 001 | Yêu cầu đánh giá lại sau sự cố ngã hoặc Trung bình trở lên (FR-043, FR-051); căn cứ sự cố cho lệnh Chuyển viện (FR-049) |
| Nhận | 002 | Quyền, phạm vi, ngoại lệ FR-044a, kiểm tra nghiệp vụ chuyên môn |
| Cung cấp | 002 | Giấy phép cơ sở và phạm vi (FR-071) |
| Nhận | 003 | Người cùng phòng và khoảng thời gian; khoảng vắng mặt (FR-060) |
| Cung cấp | 003 | Vùng khoanh vùng và gỡ; dấu nghi nhiễm, tiếp xúc (FR-062, FR-065) |
| Nhận | 004 | Chuyển viện, trở về, trạng thái cuối |
| Cung cấp | 004 | Cảnh báo, sự cố đang mở cho điều kiện Kết thúc lưu trú và danh sách việc sau qua đời (FR-038) |
| Nhận | 005 | Yêu cầu tạo/đóng cảnh báo; giá trị đo từ công việc; kết quả ngủ; công việc đo kết thúc Không thực hiện hoặc Quá hạn cuối ca |
| Cung cấp | 005 | Lịch đo (mọi nguồn); bản ghi đã xác nhận / yêu cầu đo lại cho công việc đo (FR-018); sự cố ngã (yêu cầu xem xét kế hoạch); cảnh báo đang mở cho checklist điều dưỡng |
| Nhận | 006 | Yêu cầu tạo/đóng cảnh báo thuốc; thuốc đang dùng |
| Nhận, Cung cấp | 008 | Nhận phân công, ca, Bác sĩ trực, Người phụ trách ca, xác nhận bàn giao; cung cấp cảnh báo, sự cố đang mở cho bản nháp bàn giao, chỉ số vượt ngưỡng trong ca (BR-M09-06) |
| Cung cấp | 009 | Sự kiện thông báo theo mức và người nhận (FR-033, FR-035, FR-043, FR-047, FR-062, FR-064); nhận lại thời điểm gửi, đã xem |
| Nhận, Cung cấp | 011 | Nhận yêu cầu tạo cảnh báo xung đột dị ứng với chế độ ăn, thực đơn (mức Trung bình) và báo nguồn đã xử lý; yêu cầu tạo sự cố loại "phục vụ sai suất ăn" (nguồn "phục vụ sai suất ăn" hoặc "ăn uống", FR-040). Gửi sự kiện "sụt cân" (FR-039a) |
| Nhận, Cung cấp | 013 | Nhận yêu cầu tạo sự cố loại "đồ gửi thất lạc" / "đồ gửi hư hỏng" (nguồn "đồ gửi", FR-040, FR-042), liên kết sự cố đã có, diễn biến "đã tìm thấy", yêu cầu chuyển Đã hủy (FR-046b); cung cấp sự cố đã ghi để liên kết và trạng thái mở/đóng cho căn cứ điều kiện đồ gửi (feature 013 FR-021, FR-025) |
| Nhận, Cung cấp | 012 | Nhận lượt thăm (FR-060) và người liên hệ chính; cung cấp trạng thái khoanh vùng, sự cố theo quyền, dữ liệu bản tin (FR-082) |
| Nhận, Cung cấp | 014 | Nhận điểm danh hoạt động, yêu cầu tạo sự cố thiếu người khi trở về; cung cấp trạng thái khoanh vùng |
| Cung cấp | 016 | FR-081 (chỉ số đã xác nhận và mức vượt ngưỡng lúc đo; cảnh báo, sự cố với mức hiện hành, mức lúc tiếp nhận, thời điểm tạo, tiếp nhận, các lần leo thang; theo dõi; khoanh vùng; dấu loại sự cố) |

### Key Entities *(include if feature involves data)*

- **Chỉ số (danh mục)** – nhóm 1: tên, đơn vị, thành phần, khoảng hợp lệ vật lý từng thành phần (CFG-M06-03), thuộc nhóm "sinh hiệu", trạng thái hiệu lực.
- **Lịch đo** – nhóm 2: người cao tuổi, chỉ số, tần suất và mốc giờ, bắt đầu, kết thúc, mức quan trọng, nguồn (chỉ định bác sĩ / theo dõi sau ngã / theo dõi tiếp xúc), sự cố nguồn, "thay thế cho", trạng thái (Chờ hiệu lực / Hiệu lực / Đã ngừng / Hết hạn), người tạo, lý do.
- **Phiên bản ngưỡng (NGUONG_CANH_BAO)** – nhóm 2: phạm vi (cơ sở / cá nhân), người cao tuổi, chỉ số, bốn cận cho từng thành phần, thời điểm hiệu lực, trạng thái (Chờ hiệu lực / Hiệu lực / Hết hiệu lực / Đã hủy), người thiết lập, lý do.
- **Bản ghi chỉ số (CHI_SO)** – nhóm 3: người cao tuổi, chỉ số, giá trị từng thành phần, thời điểm đo, người đo, công việc nguồn, thời điểm ghi trên thiết bị và đồng bộ, ngưỡng áp dụng và kết quả so, trạng thái xác nhận (chờ đo lại / đã xác nhận / đã đo lại), nhãn (xử lý ngay, chưa đo lại, ghi nhận muộn, ghi ngoại tuyến), bản ghi đo lại, các bản đính chính.
- **Cảnh báo (CANH_BAO)** – nhóm 2: người cao tuổi, loại, khóa loại, mức, bản ghi nguồn, số lần, thời điểm đầu và gần nhất, trạng thái (Mới / Leo thang / Đã tiếp nhận / Đang xử lý / Đã đóng / Chuyển sự cố), cấp leo thang, người phụ trách, hạn tiếp nhận, người và thời điểm tiếp nhận, kết quả, người đóng, sự cố liên kết, đề xuất nâng mức và quyết định; lịch sử (nhóm 3).
- **Quy tắc xu hướng (danh mục)** – nhóm 1: tên, bật/tắt, mã tham số, mức cảnh báo.
- **Sự cố (SU_CO)** – nhóm 3 cho nội dung: người cao tuổi, loại, nguồn phát sinh, thời điểm xảy ra và phát hiện, địa điểm, hoạt động, người phát hiện, mô tả, mức (và lịch sử đổi mức), xử lý ban đầu, cảnh báo nguồn, diễn biến (đánh giá, hành động, biện pháp hồi sức, thông báo, theo dõi), xác nhận đã đối chiếu nguyện vọng, bản chụp thẻ thông tin khẩn cấp, kết quả xử lý, người đóng; trạng thái xử lý (Mới / Đang xử lý / Đang theo dõi / Đã đóng / Đã hủy), cấp leo thang.
- **Dấu nguy kịch (DAU_NGUY_KICH)** *(bổ sung 2026-09-28)* – nhóm 2: người cao tuổi, loại (chính thức / tạm), nhận định, người ghi, thời điểm, Bác sĩ xác nhận, trạng thái (Tạm / Đang mở / Đã gỡ), lý do gỡ, cảnh báo nguy kịch liên kết.
- **Kết quả xác nhận lại nguyện vọng, lựa chọn thực hiện** *(bổ sung 2026-09-28)* – nhóm 3: cảnh báo nguy kịch, người được liên hệ, kênh, thời điểm, kết quả (giữ / mới / không liên lạc được), phiên bản nguyện vọng mới nếu có, lựa chọn thực hiện, lệnh đã thực hiện, người ghi.
- **Bản tóm tắt chuyển viện** – nhóm 3: sự cố, người cao tuổi, cơ sở tiếp nhận, dị ứng, thuốc đang dùng, chỉ số gần nhất, diễn biến, nguyện vọng cuối đời, người lập, thời điểm.
- **Danh sách tiếp xúc** – nhóm 2: sự cố lây nhiễm, trạng thái (Đề xuất / Đã xác nhận / Đã kết thúc), từng người (loại: người cao tuổi / nhân viên / người thân; nguồn; khoảng tiếp xúc; thêm thủ công hay đề xuất; bị loại và lý do), người xác nhận, thời điểm.
- **Khoanh vùng (KHOANH_VUNG)** – nhóm 2: vùng (danh sách tầng, hoặc một khu vực), sự cố gắn, bắt đầu, kết thúc, người khoanh, người gỡ, lý do, trạng thái (Đang khoanh vùng / Đã gỡ).
- **Giấy phép hoạt động của cơ sở** – nhóm 1 có lịch sử: số, cơ quan cấp, ngày cấp, ngày hết hạn, phạm vi, bản scan, bản được thay thế.
- **Bản ghi khám và điều trị** – nhóm 3: người cao tuổi, loại, nội dung, cơ sở thực hiện, người thực hiện, thời điểm, đính kèm, các bản đính chính.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Với bộ kiểm thử 300 giá trị đo (6 chỉ số, có giá trị bằng cận, ngoài khoảng vật lý, ngưỡng cá nhân và mặc định, phiên bản ngưỡng đổi giữa ngày), 100% giá trị được xếp đúng mức so với bảng tính tay; 0 giá trị phi vật lý được lưu; 100% giá trị Nguy hiểm có yêu cầu đo lại hoặc nhãn "xử lý ngay".
- **SC-002**: Người đo thấy kết quả so ngưỡng (bình thường, cảnh báo, nguy hiểm, yêu cầu đo lại) ngay khi lưu, không quá 2 giây; cảnh báo tương ứng xuất hiện với người phụ trách không quá 10 giây sau khi lưu.
- **SC-003**: Trong kiểm thử đồng thời (10 phát sinh cùng loại cho cùng người cao tuổi gửi cùng lúc), 0 trường hợp có hai cảnh báo cùng khóa loại đang mở; số lần của cảnh báo bằng số phát sinh.
- **SC-004**: Với đồng hồ giả lập, 100% cảnh báo Trung bình chưa tiếp nhận được leo thang không muộn hơn 1 phút sau mỗi hạn, đúng cấp, và 100% lần leo thang có bản ghi lịch sử; 0 cảnh báo đã tiếp nhận bị leo thang.
- **SC-005**: Khi kích hoạt khẩn cấp, thẻ thông tin khẩn cấp hiển thị cho người xử lý không quá 3 giây; thông báo tới mọi người nhận của FR-047 được gửi song song và đến thiết bị trong không quá 10 giây (NFR-03).
- **SC-006**: 0 biện pháp hồi sức được ghi cho người cao tuổi có nguyện vọng cuối đời đã ghi nhận mà thiếu xác nhận "đã đối chiếu nguyện vọng"; 0 lần sửa hay xóa trực tiếp sự cố Khẩn cấp hoặc bản ghi chỉ số.
- **SC-007**: 100% sự cố ngã tạo đủ ba tác động (yêu cầu đánh giá lại, yêu cầu xem xét kế hoạch, lịch theo dõi sau ngã với số lần đo bằng thời gian theo dõi chia khoảng đo) trong cùng lần ghi; 0 người cao tuổi có hai lịch theo dõi sau ngã cùng Hiệu lực.
- **SC-008**: Với bộ dữ liệu truy vết dựng sẵn (phòng, điểm danh, phân công, lượt thăm trong 10 ngày), danh sách tiếp xúc đề xuất khớp 100% danh sách tính tay trong CFG-M05-07 và 0 người ngoài khoảng; đề xuất có sẵn không quá 1 phút sau khi ghi sự cố.
- **SC-009**: Trong thời gian khoanh vùng, 0 đăng ký thăm mới, đăng ký hoạt động chung mới, phân bổ giường mới trong vùng thành công; sau khi gỡ, 100% các chặn được bỏ ngay (trừ phòng đang cách ly riêng hoặc thuộc vùng khác còn hiệu lực).
- **SC-010**: 0 bản ghi chẩn đoán, chỉ định, điều trị tại viện được lưu khi cơ sở hoặc bác sĩ thiếu giấy phép đúng phạm vi còn hiệu lực.
- **SC-011**: Nhân viên ghi xong một sự cố Nhẹ hoặc Trung bình trong không quá 90 giây và kích hoạt khẩn cấp trong không quá 15 giây tính từ lúc mở chức năng. Các con số này là mục tiêu nghiệm thu, không phải tham số cấu hình.
- **SC-012**: 0 lần người ghi sự cố ngoài phạm vi thấy thông tin sức khỏe của người cao tuổi; 100% lần như vậy có nhật ký "ngoài phạm vi".
- **SC-013**: Báo cáo thời gian trung bình từ tạo cảnh báo đến tiếp nhận và số lần leo thang khớp 100% số tính tay trên bộ kiểm thử 50 cảnh báo.
- **SC-014**: Trong bộ kiểm thử thông báo khẩn cấp và thông báo tiếp xúc, 0 thông báo gửi người không có bản đồng ý chia sẻ dữ liệu chứa thông tin sức khỏe, và 0 thông báo tiếp xúc nêu danh tính người nghi nhiễm (FR-047, FR-062, Q-72).
- **SC-015** *(bổ sung 2026-09-28)*: 100% lần ghi dấu nguy kịch tạo đúng một cảnh báo nguy kịch và một yêu cầu xác nhận lại trong cùng lần; thông báo tới Bác sĩ trực, Điều dưỡng phụ trách, Trưởng tầng, Quản lý viện, người đại diện đến thiết bị trong không quá 10 giây (NFR-03); 0 cảnh báo nguy kịch được đóng khi thiếu kết quả xác nhận lại hoặc lựa chọn thực hiện; 0 thông báo tới người thân chứa nội dung nguyện vọng.

## Assumptions

- Số feature `007` theo bản đồ feature ở mục 4.2 `docs/phan-tich-yeu-cau.md` (UC-16 phần sự cố, UC-32 → UC-38) và trùng số tuần tự kế tiếp.
- Quy tắc dùng chung theo feature 000; hồ sơ sức khỏe, nguyện vọng cuối đời, cờ nguy cơ, lệnh Chuyển viện theo feature 001/004; quyền và giấy phép theo feature 002; lịch sử vị trí và khoanh vùng phòng theo feature 003; sinh công việc theo feature 005; thuốc theo feature 006. Spec này không lặp lại các quy tắc đó.
- Q-01 được dùng theo mặc định: chỉ số ghi được ngoại tuyến và đồng bộ sau; kích hoạt khẩn cấp bắt buộc trực tuyến (8.6).
- Lịch đo do Bác sĩ thiết lập theo dòng "Ngưỡng cảnh báo" (UC-33) vì Permission Matrix không có dòng riêng; ngưỡng mặc định của cơ sở cũng do Bác sĩ thiết lập (chuyên môn), không coi là tham số cấu hình của Quản lý viện.
- Người phụ trách ban đầu của cảnh báo là Điều dưỡng phụ trách, không phải nhân viên chăm sóc, vì nhân viên chăm sóc chỉ có quyền xem cảnh báo (4.4); chuỗi leo thang của BR-M05-01 được rút thành ba cấp (FR-033).
- Mỗi cấp leo thang của cảnh báo Trung bình chờ lại CFG-M05-01; cảnh báo Khẩn cấp không leo thang theo cấp vì mọi cấp đã được thông báo đồng thời, việc chưa xác nhận xử lý theo BR-M13-02.
- Cảnh báo Nhẹ có hạn "trong ca" được hiểu là tới hết ca hiện tại của tầng người cao tuổi.
- Sự cố dùng cùng thời hạn tiếp nhận và chuỗi leo thang với cảnh báo cùng mức (cột "Thời hạn tiếp nhận" của 9.3 áp cho cả hai).
- Mức độ của các quy tắc xu hướng không có trong tài liệu nguồn; mặc định ở FR-039 là đề xuất và cấu hình được.
- "Sụt cân ≥ 5% trong 30 ngày" được so với cân nặng cao nhất trong 30 ngày trước lần đo.
- CFG-M05-07 \[5 ngày\] được dùng cho mọi nguồn truy vết (cùng phòng, hoạt động, phân công, thăm), không chỉ hoạt động chung.
- Người tiếp xúc là nhân viên và người thân được thông báo, không được sinh công việc đo nhiệt độ (công việc chỉ gắn người cao tuổi, 8.3).
- Mỗi sự cố gắn tối đa một người cao tuổi; ổ dịch nhiều người được thể hiện bằng nhiều sự cố lây nhiễm gắn cùng một vùng khoanh vùng.
- Hệ thống không gửi báo cáo cho cơ quan y tế; chỉ ghi nhận việc đã báo (FR-066).

## Điểm cần báo lại về tài liệu nguồn

*(Câu mở đầu lúc lập spec: "Chưa sửa `docs/`".)* **(2026-09-29, checklist cross-feature CHK026)** Tình trạng hiện tại của từng điểm ghi ở các đoạn có ngày bên dưới; điểm không được nhắc ở đoạn nào vẫn là "còn chờ chủ tài liệu xác nhận" (xem CHK027).

1. **Chuỗi leo thang BR-M05-01 bắt đầu từ "nhân viên"**, nhưng Permission Matrix 4.4 chỉ cho Nhân viên chăm sóc quyền X ở dòng "Xử lý cảnh báo" và UC-34 có actor là Điều dưỡng. Spec bắt đầu chuỗi từ Điều dưỡng phụ trách, nhân viên thực hiện công việc nguồn chỉ được thông báo (FR-031, FR-033). Cần sửa BR-M05-01. **Đã xử lý (2026-09-28):** BR-M05-01 trong docs/nghiep-vu.md đã được bổ sung đoạn "Làm rõ, spec 007" theo FR-031, FR-033.
2. **Cấp cuối của chuỗi leo thang là "quản lý"**, nhưng 4.4 cho Quản lý viện quyền X ở dòng "Xử lý cảnh báo" và X ở dòng "Sự cố, khẩn cấp"; trong khi 4.1 xếp Quản lý viện (AC-01) là con của AC-00 "Nhân viên" có quyền "ghi nhận sự cố". Đã chốt khi clarify (FR-029, FR-036, FR-041): Quản lý viện tiếp nhận được cảnh báo ở Leo thang cấp 2 và giao người phụ trách; ghi nhận được sự cố, kích hoạt được khẩn cấp; không xử lý, không đóng. Cần sửa hai dòng "Xử lý cảnh báo", "Sự cố, khẩn cấp" của 4.4 và thêm Q-71 vào mục 24.2.
3. **Không có tham số cho thời gian chờ đo lại** giá trị nguy hiểm (BR-M06-04); spec đề xuất CFG-M06-04 \[10 phút\] (FR-017) cần thêm vào Phụ lục 25.
4. **Sơ đồ vòng đời cảnh báo 9.4** không có: chuyển tự đóng khi nguồn đã được xử lý (feature 005, 006 đã dựa vào); Leo thang → Leo thang (lên cấp tiếp); lệnh nâng/hạ mức; chuyển người phụ trách sau bàn giao; lệnh "Kích hoạt khẩn cấp từ cảnh báo" đi thẳng từ Mới/Leo thang. Spec thêm các chuyển này (FR-029).
5. **"Cùng loại" trong BR-M05-02 / DBR-18** chưa được định nghĩa; spec định nghĩa khóa loại theo nguồn và đối tượng (FR-028).
6. **Thẻ thông tin khẩn cấp "hiển thị ngay cho người xử lý" (9.5)** và ngoại lệ ghi sự cố ngoài phạm vi (feature 002 FR-044a, 19.3) cần thống nhất: spec chỉ hiện thẻ cho người có quyền xem sức khỏe, người ngoài phạm vi thấy thông tin nhận dạng (FR-048).
7. **Vòng đời sự cố** chỉ có dạng các bước ở 9.4, không có bảng trạng thái; SU_CO được xếp nhóm 3 nhưng có trạng thái đóng (BR-M05-09). Spec tách nội dung (nhóm 3) và trạng thái xử lý (điều khiển bằng lệnh) (FR-045); cần bổ sung vào 9.4.
8. **Bản tóm tắt chuyển viện (BR-M05-14)** không nêu nguyện vọng cuối đời; spec thêm (FR-050).
9. **Permission Matrix không có dòng** cho: danh mục chỉ số, lịch đo, khám và điều trị (10.2), giấy phép cơ sở. Spec giao: danh mục chỉ số, danh mục loại sự cố, quy tắc xu hướng, giấy phép cơ sở cho Quản lý viện; lịch đo cho Bác sĩ; khám và điều trị cho Bác sĩ, Điều dưỡng (FR-002, FR-003, FR-070, FR-072).
10. **Khoảng thời gian truy vết người cùng phòng, nhân viên phân công, người thân thăm** không có trong BR-M05-10 (CFG-M05-07 chỉ gắn với hoạt động chung); feature 003 đã báo lại điểm này. Spec dùng CFG-M05-07 cho mọi nguồn (FR-060).
11. **Mức mặc định của ngã**: 9.3 ghi "Ngã" là ví dụ mức Khẩn cấp; đã chốt khi clarify (FR-042): mặc định Khẩn cấp, được chọn mức thấp hơn kèm lý do, tác động BR-M05-07 áp cho mọi mức. Cần ghi vào 9.3 và thêm Q-70 vào mục 24.2.
12. **Các bổ sung khác của spec không có trong tài liệu nguồn**, cần chủ tài liệu xác nhận: sự cố không gắn người cao tuổi cho loại "cơ sở vật chất" (feature 003 đã dựa vào); cảnh báo "đã leo thang tối đa" (FR-034); không cho đóng sự cố lây nhiễm khi vùng còn hiệu lực (FR-066); lịch theo dõi sau ngã mới thay lịch cũ (FR-052); cách nhắc thiết lập ngưỡng cá nhân (FR-025); phạm vi cơ sở suy ra từ giấy phép thay vì công tắc riêng (FR-071); danh mục quy tắc xu hướng có mức cấu hình (FR-039).
13. **Các quyết định clarify (2026-09-26)** cần phản ánh vào tài liệu nguồn và thêm vào mục 24.2: Q-67 cảnh báo Khẩn cấp không tự tạo sự cố, chỉ báo đồng thời nhân viên lâm sàng; người liên hệ chính chỉ được báo khi có sự cố Khẩn cấp (FR-047a; bổ sung 9.1, 9.3, BR-M06-03); Q-68 Điều dưỡng (trong phạm vi) và Bác sĩ xác nhận danh sách tiếp xúc (FR-060; thêm dòng "Danh sách tiếp xúc" vào 4.4); Q-69 đơn vị khoanh vùng là tầng hoặc khu vực, phòng riêng dùng cách ly phòng (FR-063; sửa "khu" trong BR-M05-11 và BR-M05-12); Q-70 (điểm 11); Q-71 (điểm 2).
14. **Các quyết định clarify lượt 2 (2026-09-26, sau checklist business-rules)** cần phản ánh vào tài liệu nguồn và thêm vào mục 24.2: Q-72 giới hạn nội dung thông báo cho người liên hệ chính không có bản đồng ý và cho người tiếp xúc (FR-047, FR-062; bổ sung BR-M05-06, BR-M05-11, 9.6); Q-73 hủy hoặc đổi loại sự cố không tự thu hồi tác động, sự cố hủy chuyển Đã hủy (FR-045, FR-046a; bổ sung 9.4); Q-74 không có Bác sĩ trực thì báo mọi Bác sĩ và Quản lý viện (FR-047b; bổ sung BR-M05-06, 2.4); Q-75 một lần đo bị bỏ chỉ sinh một cảnh báo "bỏ lỡ lần đo theo lịch" (FR-039b; bổ sung BR-M05-04, BR-M04-06); Q-76 cảnh báo đã chuyển sự cố mà nguồn được xử lý thì ghi diễn biến vào sự cố, không tự đóng (FR-032; bổ sung 9.4).

**(2026-09-27)** Các quyết định Q-67 → Q-76 của spec này đã được phản ánh vào `docs/nghiep-vu.md` (9.3 → 9.6, BR-M05-02) và `docs/phan-tich-yeu-cau.md` (dòng "Danh sách tiếp xúc", chú thích ²³, ²⁴ của 4.4), và nằm ở mục 24.2.

**(2026-09-28, checklist cross-feature CHK018, CHK024, CHK030)** Đã phản ánh vào `docs/nghiep-vu.md`: điểm 3 (CFG-M06-04 ở Phụ lục 25 và BR-M06-04); điểm 4, 7 (bảng bổ sung vòng đời cảnh báo và bảng vòng đời sự cố ở 9.4); điểm 10 (BR-M05-10 và mô tả CFG-M05-07: khoảng truy vết áp cho mọi nguồn).

**(2026-09-28, góp ý nghiệp vụ Q-207 → Q-222) Tài liệu nguồn đã thay đổi, spec cần rà lại.** *(2026-09-29: mọi điểm dưới đây đã xử lý, xem nhãn "Đã xử lý" từng điểm.)* Các điểm dưới đây đã có trong `docs/nghiep-vu.md` và `docs/luong-nghiep-vu.md`; spec **chưa** được sửa theo, và cần chạy `/speckit-clarify` hoặc cập nhật FR tương ứng.

1. **[Đã xử lý 2026-09-28: Phạm vi 12, User Story 11, mục G1 FR-050a → FR-050g, FR-028, FR-080, FR-083, Key Entities, SC-015]** **Dấu nguy kịch và cảnh báo "nguy kịch – thực hiện nguyện vọng cuối đời" (Q-213, Q-218; 9.5, BR-M05-15, 16, UC-83, 84, BF-17, DBR-34).** Chức năng mới: Bác sĩ ghi, gỡ dấu (Điều dưỡng ghi dấu tạm khi không có Bác sĩ trực). Hệ thống tạo cảnh báo Khẩn cấp không gộp, báo gia đình, tạo yêu cầu xác nhận lại nguyện vọng theo thứ tự gọi của BR-M13-02, rồi thực hiện lựa chọn (chuyển viện / tạm vắng về nhà / chăm sóc giảm nhẹ). Cảnh báo chỉ đóng khi đã có kết quả xác nhận và lựa chọn. Đề xuất thêm một User Story.
2. **[Đã xử lý 2026-09-28: FR-048, FR-083]** **Nguyện vọng cuối đời có cấu trúc (Q-213, Q-216; 5.2).** Thẻ thông tin khẩn cấp (User Story 5) hiển thị phiên bản nguyện vọng Hiệu lực với ba lựa chọn đã chuẩn hóa, không còn là văn bản tự do.
3. **[Không cần sửa]** **Chỉ số thuộc nhóm "Chăm sóc" (Q-207; 1.6).** Không đổi ranh giới spec.
