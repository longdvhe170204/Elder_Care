# Feature Specification: Chỉ số sức khỏe, cảnh báo và sự cố

**Feature Branch**: `007-health-alert-incident`

**Created**: 2026-09-26

**Status**: Draft

**Input**: User description: "Quản lý chỉ số sức khỏe, cảnh báo và sự cố theo docs/business/nghiep-vu.md Module 05 và Module 06 (mục 9, 10): lịch đo; ngưỡng cá nhân và mặc định với hai mức cảnh báo/nguy hiểm; quy tắc theo xu hướng; vòng đời cảnh báo có thời hạn tiếp nhận, leo thang và gộp trùng; sự cố ba mức; quy trình khẩn cấp với thẻ thông tin khẩn cấp và đối chiếu nguyện vọng cuối đời; sự cố ngã kéo theo theo dõi sau ngã; sự cố lây nhiễm với truy vết tiếp xúc tự động và khoanh vùng khu vực; phạm vi y tế được phép theo giấy phép."

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

### Session 2026-09-30 (rà soát vận hành)

- Q: Cảnh báo đã leo thang tới cấp 2 mà vẫn chưa ai tiếp nhận thì sao? → A: Không thêm cấp (trên Quản lý viện không còn cấp nào trong viện), nhưng nhắc lại mức Trung bình cho Quản lý viện và Trưởng tầng của tầng người cao tuổi, mỗi khoảng bằng thời hạn tiếp nhận của mức cảnh báo (CFG-M05-01 với mức Trung bình; hết ca với mức Nhẹ), tới khi có người tiếp nhận (Q-248).

### Session 2026-10-01 (/speckit-clarify, checklist cross-feature)

- Q: Lần nhắc lại cảnh báo "đã leo thang tối đa" có được tính là một lần leo thang không, và mức Nhẹ nhắc thế nào? → A: Không tính; không đổi cấp, không vào lịch sử leo thang hay chỉ tiêu số lần leo thang, chỉ ghi lịch sử nhắc. Mức Nhẹ nhắc khi hết mỗi ca của tầng, không thay bàn giao; mỗi lần nhắc có khóa sự kiện riêng theo số thứ tự (Q-266).

### Session 2026-10-01 (đánh giá checklist business-rules)

Các điểm dưới đây do nhóm phân tích quyết định khi đánh giá checklist business-rules; không đổi quy tắc ở tài liệu nguồn, trừ các đề xuất nêu ở mục "Điểm cần báo lại".

- Q: Trưởng tầng có xem được ngưỡng và lịch đo không (Phụ lục 27 ghi "—", 19.3 ghi xem đầy đủ hồ sơ sức khỏe trong phạm vi)? → A: Xem được trong phạm vi, không thiết lập; 19.3 là căn cứ khi khác Phụ lục 27 (CHK024).
- Q: Sự cố "cơ sở vật chất" không gắn người cao tuổi thì báo ai, ai tiếp nhận? → A: Người phụ trách ca và Trưởng tầng của tầng/khu vực chứa địa điểm; địa điểm chọn từ cơ cấu của feature 003 (CHK004).
- Q: Cảnh báo, sự cố của người cao tuổi đang vắng mặt do ai phụ trách? → A: Điều dưỡng phụ trách theo tầng của giường đang giữ; không còn giường thì Người phụ trách ca của tầng theo phân bổ gần nhất; hạn và leo thang chạy như thường (CHK006, CHK039).
- Q: Người trong vùng khoanh vùng có được Cho tạm vắng, đón về, chuyển viện không? → A: Có; hệ thống không chặn mà cảnh báo người thực hiện rằng người đó thuộc vùng đang khoanh vùng. Hoạt động ngoài viện bị chặn như hoạt động chung (CHK008).
- Q: Cảnh báo Nhẹ tạo sát giờ hết ca thì hạn tiếp nhận là khi nào? → A: Tạo sau thời điểm lập bản nháp bàn giao của ca (CFG-M09-04) thì hạn là hết ca kế tiếp; cảnh báo vẫn vào bàn giao (CHK020).
- Q: "Biện pháp hồi sức" gồm những gì? → A: Các loại diễn biến mang dấu "biện pháp hồi sức" trong danh mục loại diễn biến do Quản lý viện cấu hình; mặc định đề xuất gồm ép tim ngoài lồng ngực, hỗ trợ thông khí, sốc điện, đặt nội khí quản, thuốc cấp cứu (CHK021).
- Q: Ai được "Hủy ghi nhận" hoặc đổi loại một sự cố từng ở mức Khẩn cấp? → A: Chỉ Điều dưỡng (trong phạm vi) hoặc Bác sĩ; người ghi gốc không có quyền xử lý chỉ đề nghị bằng diễn biến (CHK053).
- Q: Hủy sự cố lây nhiễm khi vùng khoanh vùng còn chỉ gắn với nó? → A: Chặn, như chặn đóng; phải gỡ vùng hoặc gắn vùng với sự cố khác trước (CHK054).
- Q: Người nghi nhiễm là nhân viên hoặc người thân? → A: Ngoài phạm vi. Sự cố "lây nhiễm nghi ngờ" luôn gắn một người cao tuổi (Assumptions); hệ thống không truy vết từ một nhân viên hay người thân nghi nhiễm (chủ dự án chốt 2026-10-01, CHK037).
- Q: Hành chính thấy gì của danh sách tiếp xúc để liên hệ người thân? → A: Chỉ danh sách người thân cần liên hệ (họ tên, quan hệ, số điện thoại) và nội dung cần báo; không thấy người nghi nhiễm hay người cao tuổi tiếp xúc (CHK062).
- Q: Bộ lập lịch ngừng một thời gian thì leo thang chạy bù thế nào? → A: Mỗi cảnh báo, sự cố quá hạn lên đúng một cấp khi chạy lại, hạn mới tính từ lúc chạy lại; không dồn nhiều cấp (CHK035).
- Q: "Số liên hệ của viện" trong thông báo lấy từ đâu? → A: Thông tin cơ sở do Quản lý viện cấu hình cùng giấy phép cơ sở (CHK057).

### Cập nhật 2026-10-06 (đồng bộ Q-280 → Q-288, quyết định của chủ dự án)

- **Q-281**: không còn cờ nguy cơ; thẻ thông tin khẩn cấp hiển thị phần **lưu ý chăm sóc** của lần đánh giá Đã xác nhận gần nhất thay cho cờ nguy cơ (feature 001 FR-038). Phạm vi, User Story khẩn cấp, FR-048, bảng giao tiếp, Assumptions đã sửa.
- **Q-282**: hệ thống không sinh công việc khử khuẩn khi khoanh vùng hay gỡ khoanh vùng (BR-M03-11 không còn áp dụng); viện tự tổ chức khử khuẩn ngoài hệ thống. Dấu "nghi nhiễm", "tiếp xúc" vẫn được feature 003 dùng để bắt buộc ghi "đã khử khuẩn" khi Xác nhận giường sẵn sàng (feature 003 FR-045). FR-065, bảng trạng thái vùng khoanh, kịch bản khoanh vùng đã sửa.
- **Q-283**: yêu cầu xem lại chế độ ăn khi sụt cân do Bác sĩ xử lý (FR-039a); cột "DDV, Bếp, VS" của bảng quyền FR-080 thành "Bếp".

### Cập nhật 2026-10-06, lượt 3 (rà tham chiếu tới tài liệu nguồn)

- FR-080, dòng "Ngưỡng, lịch đo": Phụ lục 27 nay ghi Trưởng tầng "P" (Q-279), khớp 19.3; ô của Trưởng tầng bỏ ghi chú mâu thuẫn. Không đổi quyền.
- FR-060 (c): bỏ "công việc vệ sinh phòng của người đó" khỏi nguồn truy vết tiếp xúc, vì hệ thống không còn công việc vệ sinh (Q-282).

### Cập nhật 2026-10-06, lượt 4 (đồng bộ Q-306, Q-307: hệ thống không tự sinh, tự tạo, tự gợi ý)

- **Q-307, sự cố**: hệ thống không tự tạo sự cố. Phục vụ suất có thành phần gây dị ứng (feature 011), đồ gửi thất lạc, hư hỏng (feature 013), thiếu người khi về (feature 014) chỉ tạo cảnh báo hoặc thông báo; nhân viên tự ghi nhận sự cố. "Báo thiếu người" của feature 014 là lệnh ghi nhận sự cố (FR-040, bảng giao tiếp).
- **Q-307, theo dõi sau ngã và tiếp xúc**: bỏ lịch đo tự động nguồn "theo dõi sau ngã", "theo dõi tiếp xúc". Sự cố ngã chỉ báo bác sĩ cần đánh giá lại, báo điều dưỡng xem xét kế hoạch và báo Người phụ trách ca lập công việc theo dõi trong khoảng theo dõi (FR-051, FR-052, User Story 6). Danh sách tiếp xúc do Điều dưỡng hoặc Bác sĩ tự lập (trạng thái Nháp), hệ thống không đề xuất; người tiếp xúc được Người phụ trách ca lập công việc đo nhiệt độ (FR-060 → FR-062, User Story 8).
- **Q-307, nâng mức và gọi điện**: bỏ "đề xuất nâng mức": hệ thống báo số lần lặp, người tiếp nhận quyết định (FR-038a). Xác nhận lại nguyện vọng không còn là yêu cầu gọi điện: hệ thống báo người gọi, người gọi ghi kết quả (FR-050b). Hạ mức khỏi Khẩn cấp dừng nhắc gọi điện (FR-044).
- **Q-306**: lịch đo là căn cứ để Người phụ trách ca lập công việc đo, không sinh công việc (FR-004, bảng FR-006). Sự cố mức Trung bình trở lên chỉ báo bác sĩ cần đánh giá lại (FR-043). Lựa chọn chăm sóc giảm nhẹ chỉ báo điều dưỡng xem xét kế hoạch (FR-050d). Cảnh báo, sự cố đang mở do người bàn giao ghi vào bàn giao (FR-037).

### Cập nhật 2026-10-10, lượt 2 (đồng bộ Q-306, Q-307, Q-311, Q-330 và giao tiếp với 013, 014 sau rà soát điểm báo lại của plan)

- Bỏ dòng "Hệ thống tạo sự cố" ở bảng FR-045 và dòng "Nguồn đồ gửi bị Hủy ghi nhận → Đã hủy"; FR-046b chỉ báo người xử lý (Q-307, theo 013 FR-011a).
- Key Entities "Lịch đo" chỉ còn nguồn chỉ định bác sĩ (FR-003). Bảng FR-006 dòng "Ngừng lịch đo" ghi tác động theo 005 FR-022.
- Bảng FR-083: lựa chọn "đưa về nhà" chỉ báo người thực hiện; dòng 001 là căn cứ báo đánh giá lại; thêm dòng cung cấp dấu nghi nhiễm và diễn biến cho 014.
- **Q-330**: nhắc lại thiếu ngưỡng khi ngưỡng bị Ngừng. Sửa FR-025. FR-017 bỏ chữ "đề xuất".
- **Q-311**: thêm FR-085 (mức thông báo); thêm FR-086 (diễn biến cảnh báo từ module khác).

### Cập nhật 2026-10-11 (đồng bộ Q-337 → Q-345 sau đối chiếu tài liệu gốc DOCX)

Chủ dự án chốt Q-337 → Q-345 (docs/business/nghiep-vu.md 24.2) sau khi đối chiếu với tài liệu gốc DOCX "nghiep-vu-quan-ly-vien-duong-lao.docx" (lưu ngoài kho mã từ 2026-10-15; bản cũ ở commit a2ff357); các Q đã chốt trước được giữ. Thay đổi ở spec này:

- **Q-339**: thêm lệnh "Giao người xử lý" cho sự cố (tài liệu gốc 20.1 "Phân công người xử lý"): Trưởng tầng, Bác sĩ, Quản lý viện giao sự cố Mới cho Điều dưỡng, Trưởng tầng hoặc Bác sĩ có quyền (chuyển Đang xử lý, dừng leo thang); giao lại khi Đang xử lý, Đang theo dõi (FR-045a, bảng FR-045, FR-041, bảng quyền, bảng giao tiếp 009, User Story 4 kịch bản 11, Key Entities). Quản lý viện vẫn không xử lý, không đóng (Q-71).
- **Q-307**: FR-039a không còn "tạo (hoặc gộp nguồn vào) yêu cầu Bác sĩ xem lại chế độ ăn"; feature 011 chỉ thông báo Bác sĩ (011 FR-058, SC-010).

### Cập nhật 2026-10-18 (đồng bộ Q-352, Q-354, nguyên tắc nhân văn)

Chủ dự án chốt nguyên tắc "quyền riêng tư và phạm vi thông báo" (docs/business/nghiep-vu.md 9.5, 14.5, 24.2). Thay đổi ở spec này:

- **Q-354**: thông tin về người cao tuổi gửi người thân ngoài phạm vi bản đồng ý chỉ trong các trường hợp liệt kê ở 9.5; spec này phát sinh ba trường hợp (khẩn cấp tới người liên hệ chính, danh sách tiếp xúc, xác nhận lại nguyện vọng khi nguy kịch) và phần chuyển viện của quy trình khẩn cấp; mỗi lần gửi ghi trường hợp áp dụng (FR-047, FR-047c).
- **Q-352**: sự cố cung cấp cho bản tin và thông báo người thân tách phần chung và phần sức khỏe (FR-082).

### Cập nhật 2026-10-18, lượt 2 (đồng bộ Q-357 → Q-364)

Ngày 2026-10-18 chủ dự án ủy quyền chốt các điểm báo lại của lượt cập nhật nguyên tắc nhân văn; quyết định ghi ở docs/business/nghiep-vu.md 24.2 (Q-357 → Q-364). Thay đổi ở spec này:

- **Q-358**: mức sự cố thuộc phần chung; mô tả, xử lý ban đầu, diễn biến thuộc phần sức khỏe (FR-082).
- Sửa dẫn chiếu ở FR-047: trường hợp không có Bác sĩ trực áp FR-047b (lần sửa trước ghi nhầm FR-047c).

### Cập nhật 2026-10-18, lượt 3 (đồng bộ Q-369, nút SOS của người cao tuổi)

Chủ dự án chốt Q-365 → Q-371 (docs/business/nghiep-vu.md 24.2; Q-367 theo mặc định). Thay đổi ở spec này:

- **Q-369**: người cao tuổi tạo cảnh báo Khẩn cấp bằng nút SOS (FR-026 nguồn (d), FR-047d); khóa loại theo người cao tuổi để bấm lặp lại được gộp (FR-028); người nhận như FR-047a, thêm trưởng đoàn và người đi cùng khi ở Hoạt động bên ngoài, không báo người liên hệ chính (FR-047e); chọn nhanh nơi đang ở, trạng thái cho người cao tuổi (FR-047f); không ai tắt được nút (FR-047g).
- Bảng quyền FR-080 thêm cột NCT; FR-081 cung cấp nguồn cảnh báo cho báo cáo tách riêng SOS; FR-083 thêm giao tiếp với 002, 005, 012, 014. Thêm User Story 12, SC-017, một edge case.
- FR-047c đã dùng cho Q-354, nên các FR mới của Q-369 mang mã FR-047d → FR-047g.
- *(2026-10-19, Q-372)* Cách kích hoạt nút SOS đổi từ giữ 3 giây sang chạm một lần, đếm ngược 3 giây có nút Hủy: sửa FR-047d, mô tả và kịch bản 1, 2, 5 của User Story 12, SC-017.

### Cập nhật 2026-10-22 (đồng bộ Q-373 → Q-376: vị trí GPS, nút hỗ trợ khẩn cấp, mốc tới nơi, gọi 115)

Chủ dự án chốt Q-373 → Q-376 (docs/business/nghiep-vu.md 24.2; BR-M05-28 → 31). Thay đổi ở spec này:

- **Q-373**: lệnh SOS gửi ngay, tọa độ GPS gửi bổ sung kèm sai số, gửi lại mỗi CFG-M05-09 tới khi có người tới nơi; trong viện tọa độ chỉ tham khảo, ngoài viện là vị trí chính (FR-047h). Danh sách nơi chọn nhanh thêm hành lang, phòng sinh hoạt chung (FR-047f).
- **Q-374**: mốc "Đã tới nơi" của cảnh báo từ nút SOS, nút hỗ trợ khẩn cấp (FR-047j, bảng FR-029); báo cáo thời gian tới nơi (FR-081).
- **Q-375**: nút hỗ trợ khẩn cấp của nhân viên tạo cảnh báo Khẩn cấp "Nhân viên yêu cầu hỗ trợ", được không gắn người cao tuổi (FR-047i, FR-026 nguồn (e), FR-027, FR-028); chuyển sự cố phải chọn người cao tuổi (bảng FR-029). Thêm User Story 13, UC-99, dòng quyền FR-080.
- **Q-376**: nút "Gọi 115" ghi mốc gọi cấp cứu (FR-047k, FR-048b).
- Thêm SC-018 → SC-020, hai edge case; FR-083 thêm giao tiếp với 008, 012, 016 và tham số CFG-M11-11.
- **Q-383, Q-384** (cùng ngày): lệnh Chuyển viện hiện nguyện vọng khi điều trị, ghi người đi kèm, in phiếu tóm tắt chuyển viện (PDF) và đính kèm giấy tờ của bệnh viện (FR-049, FR-050, FR-050h); User Story 5 kịch bản 8; SC-021.

## Phạm vi

**Trong phạm vi** (Module 05 mục 9.1 → 9.7; Module 06 mục 10.1 → 10.5; UC-16 phần khởi phát từ sự cố, UC-32 → UC-38):

1. Danh mục chỉ số (huyết áp, nhịp tim, nhiệt độ, SpO2, đường huyết, cân nặng, chỉ số khác) với khoảng hợp lệ vật lý (10.1, CFG-M06-03).
2. Lịch đo riêng của từng người cao tuổi do bác sĩ thiết lập; lịch đo là căn cứ để Người phụ trách ca lập công việc đo chỉ số ở feature 005 (10.1, BR-M04-01). Không có lịch đo tự động (Q-307).
3. Lưu bản ghi chỉ số, chặn giá trị phi vật lý, đo lại xác nhận giá trị nguy hiểm, so ngưỡng, đính chính (UC-32, BR-M04-10, BR-M06-03, 04, 06).
4. Ngưỡng mặc định của cơ sở và ngưỡng cá nhân theo phiên bản, hai mức cảnh báo và nguy hiểm, mỗi mức có cận dưới và cận trên; nhắc thiết lập ngưỡng cá nhân (10.3, UC-33, BR-M06-01, 02).
5. Quy tắc theo xu hướng: sụt cân, bỏ lỡ lần đo theo lịch, mất ngủ nhiều đêm liên tiếp; tham số dùng chung với quy tắc từ chối thuốc (BR-M05-04).
6. Cảnh báo: tạo từ mọi nguồn, gộp trùng, thời hạn tiếp nhận theo mức, leo thang, báo cảnh báo lặp lại để người tiếp nhận quyết định nâng mức, xử lý, đóng, chuyển thành sự cố, chuyển người phụ trách sau bàn giao (9.1, 9.3, 9.4, UC-34, UC-38, BR-M05-01 → 03, 05, DBR-18).
7. Sự cố ba mức Nhẹ, Trung bình, Khẩn cấp: ghi nhận, tiếp nhận, diễn biến, theo dõi, đóng, đính chính (9.2 → 9.4, UC-35, BR-M05-08, 09).
8. Quy trình khẩn cấp: thẻ thông tin khẩn cấp, đối chiếu nguyện vọng cuối đời trước biện pháp hồi sức, thông báo đồng thời, chuyển viện kèm bản tóm tắt chuyển viện (9.5, UC-36, UC-16, BR-M05-06, 13, 14, DBR-19).
9. Sự cố ngã: báo bác sĩ cần đánh giá lại, báo điều dưỡng xem xét kế hoạch chăm sóc, báo Người phụ trách ca lập công việc theo dõi sau ngã (BR-M05-07; sửa 2026-10-06, Q-306, Q-307).
10. Sự cố lây nhiễm: danh sách tiếp xúc do Điều dưỡng hoặc Bác sĩ lập và xác nhận (không đề xuất, Q-307), theo dõi người tiếp xúc; khoanh vùng và gỡ khoanh vùng (9.6, UC-37, BR-M05-10 → 12).
11. Phạm vi y tế: giấy phép hoạt động của cơ sở làm căn cứ cho BR-M06-05; ghi nhận khám và điều trị theo phạm vi được phép (10.2, 10.4).
12. Nguy kịch: dấu nguy kịch, cảnh báo "nguy kịch – thực hiện nguyện vọng cuối đời", xác nhận lại nguyện vọng với gia đình, thực hiện lựa chọn (9.5, UC-83, UC-84, BR-M05-15, 16, Q-213, Q-218; bổ sung 2026-09-28).
13. *(Bổ sung 2026-10-18, Q-369)* Nút SOS của người cao tuổi: tạo cảnh báo Khẩn cấp từ thiết bị đã gắn, điều kiện gửi, người nhận, trạng thái hiển thị cho người cao tuổi (9.4, BR-M05-27, UC-98).
14. *(Bổ sung 2026-10-22, Q-373 → Q-376)* Vị trí GPS kèm nút SOS; nút hỗ trợ khẩn cấp của nhân viên; mốc "Đã tới nơi"; nút "Gọi 115" ghi mốc gọi cấp cứu (9.4, 9.5, BR-M05-28 → 31, UC-99).

**Ngoài phạm vi** (spec này chỉ **cung cấp** dữ liệu hoặc **được kích hoạt** bởi feature sở hữu):

- Quy tắc dùng chung (nhóm dữ liệu, lý do bắt buộc, đính chính, nhật ký, tham số, "hoặc toàn bộ, hoặc không", yêu cầu phê duyệt): feature 000 — spec này kế thừa, không lặp lại.
- Nguyện vọng chăm sóc cuối đời, dị ứng, bệnh nền, lưu ý chăm sóc của lần đánh giá Đã xác nhận gần nhất (sửa, 2026-10-06, Q-281), nhắc đánh giá lại, tập trạng thái người cao tuổi và lệnh Chuyển viện (điều kiện ở 5.6): feature 001. Nội dung lệnh Chuyển viện và lượt vắng loại Bệnh viện: feature 004.
- Kiểm tra quyền, phạm vi dữ liệu, ngoại lệ ghi nhận sự cố ngoài phạm vi (feature 002 FR-044a), danh mục nghiệp vụ chuyên môn, kiểm tra giấy phép khi thực hiện (feature 002 FR-036 → 038) và cảnh báo giấy phép sắp hết hạn (feature 002 FR-039): feature 002. Spec này sở hữu **dữ liệu** giấy phép hoạt động của cơ sở.
- Giấy phép hành nghề, đào tạo của nhân viên: feature 008 (UC-49).
- Lịch sử vị trí, trạng thái cách ly phòng, chặn phân bổ giường trong vùng khoanh, xác nhận đã khử khuẩn khi Xác nhận giường sẵn sàng: feature 003 (FR-008, FR-009, FR-036, FR-045). *(Sửa 2026-10-06, Q-282: không còn công việc khử khuẩn.)*
- Lập công việc đo chỉ số (căn cứ lịch đo, theo dõi sau ngã, theo dõi tiếp xúc), ghi kết quả công việc, phát hiện uống nước thiếu, ăn kém kéo dài, tâm trạng tiêu cực, công việc Bắt buộc quá hạn, báo xem xét kế hoạch chăm sóc: feature 005. Spec này nhận yêu cầu tạo và đóng cảnh báo.
- Phát hiện liều Bỏ lỡ, phản ứng thuốc, từ chối thuốc liên tiếp, xung đột dị ứng với đơn, chưa đối chiếu thuốc; danh sách thuốc đang dùng: feature 006. Spec này nhận yêu cầu tạo và đóng cảnh báo.
- Bàn giao ca, phân công, ca trực, nhiệm vụ Bác sĩ trực, Người phụ trách ca, Điều dưỡng phụ trách: feature 008.
- Kênh gửi, xác nhận đã xem, nhắc gọi điện khi thông báo khẩn cấp chưa được xác nhận, giờ yên tĩnh (BR-M13-01 → 05): feature 009. Spec này xác định **sự kiện** và **người nhận**.
- Xung đột dị ứng với chế độ ăn, phục vụ sai suất ăn (BR-M08-02, BR-M08-14): feature 011 phát hiện và yêu cầu spec này tạo cảnh báo; sự cố do nhân viên ghi nhận (Q-307).
- Hồ sơ người thân, người liên hệ chính, quyền của người thân, lượt thăm, chặn đăng ký thăm: feature 012 (bản tin bỏ theo Q-396).
- Hoạt động, điểm danh, hoạt động ngoài viện, chặn đăng ký hoạt động trong vùng khoanh, phát hiện thiếu người khi trở về (yêu cầu cảnh báo Khẩn cấp; lệnh "Báo thiếu người" ghi nhận sự cố): feature 014.
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

Bác sĩ thiết lập ngưỡng mặc định của cơ sở cho từng chỉ số và ngưỡng cá nhân cho từng người cao tuổi. Mỗi ngưỡng có hai mức (cảnh báo, nguy hiểm), mỗi mức có cận dưới và cận trên, có ngày giờ hiệu lực và được lưu thành phiên bản; thay đổi ngưỡng tạo phiên bản mới, không sửa phiên bản đang dùng. Bác sĩ cũng đặt lịch đo riêng (chỉ số, tần suất, thời gian) làm căn cứ để Người phụ trách ca lập công việc đo. Người lưu trú quá CFG-M06-01 mà chưa có ngưỡng cá nhân thì bác sĩ được nhắc.

**Why this priority**: Không có ngưỡng thì không có cảnh báo chỉ số; không có lịch đo thì không có công việc đo (10.1, 10.3).

**Independent Test**: Tạo ngưỡng mặc định nhiệt độ; tạo ngưỡng cá nhân cho A có hiệu lực từ ngày mai; ghi một giá trị hôm nay và một giá trị ngày mai; kiểm tra hôm nay so theo ngưỡng mặc định, ngày mai theo ngưỡng cá nhân, và phiên bản cũ vẫn xem được.

**Acceptance Scenarios**:

1. **Given** bác sĩ B, **When** B lưu ngưỡng cá nhân nhiệt độ cho A với cận dưới nguy hiểm 35,0, cận dưới cảnh báo 36,0, cận trên cảnh báo 37,8, cận trên nguy hiểm 39,0, hiệu lực từ 08:00 ngày 01/10, **Then** phiên bản được lưu với người thiết lập B, thời điểm, trạng thái Chờ hiệu lực; tới 08:00 ngày 01/10 phiên bản chuyển Hiệu lực (10.3).
2. **Given** B nhập cận dưới cảnh báo 34,5 thấp hơn cận dưới nguy hiểm 35,0, **When** lưu, **Then** hệ thống chặn vì khoảng cảnh báo phải nằm trong khoảng nguy hiểm (FR-021).
3. **Given** A đang có phiên bản ngưỡng nhiệt độ số 1 Hiệu lực, **When** B tạo phiên bản số 2 hiệu lực ngay, kèm lý do, **Then** phiên bản 1 chuyển Hết hiệu lực tại cùng thời điểm, phiên bản 2 Hiệu lực; bản ghi chỉ số đã so theo phiên bản 1 giữ nguyên kết quả so (FR-023).
4. **Given** A Đang lưu trú từ 8 ngày trước (CFG-M06-01, mặc định \[7 ngày\]) và chưa có phiên bản ngưỡng cá nhân Hiệu lực nào, **When** Bộ lập lịch kiểm tra hằng ngày, **Then** các Bác sĩ nhận nhắc "chưa có ngưỡng cá nhân" cho A và A xuất hiện trong danh sách "chưa có ngưỡng cá nhân" (BR-M06-02).
5. **Given** B đặt lịch đo huyết áp cho A lúc 07:00 và 17:00 hằng ngày từ 01/10, **When** lưu, **Then** lịch đo Hiệu lực từ 01/10 và là căn cứ để Người phụ trách ca lập công việc "đo huyết áp" mức Bắt buộc cho các mốc đó (feature 005 FR-017, 10.1, BR-M04-01).
6. **Given** lịch đo huyết áp của A đang Hiệu lực, **When** B đổi sang 3 lần/ngày từ ngày mai kèm lý do, **Then** lịch cũ chuyển Đã ngừng từ thời điểm đó, lịch mới trỏ về lịch cũ; Người phụ trách ca và Trưởng tầng được báo để điều chỉnh công việc đã lập (feature 005 FR-022).
7. **Given** điều dưỡng D, **When** D thử tạo ngưỡng cá nhân cho A, **Then** hệ thống chặn (4.4 dòng "Ngưỡng cảnh báo": ĐD X).
8. **(Bổ sung, CHK033)** **Given** B đã tạo phiên bản ngưỡng cá nhân nhiệt độ của A ở Chờ hiệu lực từ ngày mai, **When** B "Hủy phiên bản" mà không nhập lý do, **Then** hệ thống chặn; **When** B hủy kèm lý do, **Then** phiên bản chuyển Đã hủy, phiên bản đang Hiệu lực không đổi và ngày mai vẫn được dùng để so (bảng FR-022).

---

### User Story 3 - Tiếp nhận, leo thang và gộp cảnh báo (Priority: P1)

Mọi cảnh báo (từ chỉ số, xu hướng, công việc quá hạn, thuốc, ăn uống, nhân viên phát hiện) đi qua một vòng đời chung: Mới → Đã tiếp nhận → Đang xử lý → Đã đóng hoặc Chuyển sự cố. Mỗi cảnh báo có hạn tiếp nhận theo mức; quá hạn thì Bộ lập lịch leo thang lên cấp trên và ghi lịch sử. Cùng người cao tuổi đã có cảnh báo cùng loại đang mở thì cảnh báo mới được gộp vào, tăng số lần, không tạo cảnh báo mới. Cảnh báo Trung bình lặp lại nhiều lần trong 24 giờ thì hệ thống báo số lần lặp để người tiếp nhận quyết định có nâng lên Khẩn cấp hay không. Cảnh báo đang mở khi hết ca đi vào bàn giao và chuyển người phụ trách khi ca sau xác nhận.

**Why this priority**: Cảnh báo không ai tiếp nhận là rủi ro an toàn lớn nhất; gộp trùng tránh "ngập cảnh báo" làm nhân viên bỏ qua tín hiệu thật (BR-M05-01, 02).

**Independent Test**: Dùng đồng hồ giả lập; tạo cảnh báo Trung bình cho A không ai tiếp nhận; kiểm tra leo thang ở phút 15, 30, 45 lên đúng cấp; tạo thêm hai lần cùng loại trong 24 giờ và kiểm tra số lần gộp và thông báo lặp lại.

**Acceptance Scenarios**:

1. **Given** cảnh báo Trung bình "huyết áp vượt ngưỡng cảnh báo" của A tạo lúc 09:00, điều dưỡng phụ trách là D, **When** tới 09:15 (CFG-M05-01, mặc định \[15 phút\]) chưa ai tiếp nhận, **Then** cảnh báo chuyển Leo thang cấp 1, Trưởng tầng và Bác sĩ trực được thông báo, hạn tiếp nhận mới là 09:30; lịch sử ghi lần leo thang (BR-M05-01, UC-38).
2. **Given** như trên, **When** tới 09:30 vẫn chưa tiếp nhận, **Then** cảnh báo leo thang cấp 2, Quản lý viện được thông báo; **When** tới 09:45, **Then** không leo thang thêm, cảnh báo giữ dấu "đã leo thang tối đa" trên dashboard, và (bổ sung, Q-248) Quản lý viện và Trưởng tầng nhận nhắc lại mức Trung bình, lặp lại mỗi 15 phút tới khi có người tiếp nhận (FR-034); **When** Quản lý viện Q tiếp nhận rồi giao cho điều dưỡng D2 cùng tầng kèm ghi chú, **Then** người phụ trách là D2, D2 được thông báo; Q không đóng được cảnh báo (FR-036, Q-71).
3. **Given** cảnh báo ở Leo thang cấp 1, **When** Trưởng tầng T tiếp nhận lúc 09:20, **Then** cảnh báo chuyển Đã tiếp nhận, người phụ trách là T, việc leo thang dừng; thời gian từ tạo đến tiếp nhận (20 phút) được lưu cho báo cáo (18.3).
4. **Given** A có cảnh báo "huyết áp vượt ngưỡng cảnh báo" đang Đã tiếp nhận, **When** lần đo 11:00 lại vượt ngưỡng cảnh báo, **Then** không tạo cảnh báo mới; cảnh báo cũ tăng số lần lên 2 và thêm bản ghi chỉ số vào danh sách nguồn (BR-M05-02, DBR-18).
5. **Given** A có cảnh báo Trung bình "huyết áp" đang mở, **When** lần đo mới vượt ngưỡng nguy hiểm và được xác nhận, **Then** cảnh báo được gộp, mức nâng lên Khẩn cấp, người nhận theo mức Khẩn cấp của FR-047a (Điều dưỡng phụ trách, Người phụ trách ca, Bác sĩ trực, Trưởng tầng; chưa có người liên hệ chính và Quản lý viện) được thông báo ngay (FR-030).
6. **Given** cảnh báo Trung bình "huyết áp" của A đã có 3 lần trong 24 giờ (CFG-M05-02, mặc định \[3 lần / 24 giờ\]), **When** lần thứ 3 được gộp, **Then** người phụ trách và Bác sĩ trực được báo số lần lặp; hệ thống không đề xuất; **When** D quyết định nâng lên Khẩn cấp, **Then** D dùng lệnh Nâng mức kèm lý do (BR-M05-03; sửa 2026-10-06, Q-307).
7. **Given** cảnh báo của A đang Đang xử lý, **When** D đóng mà không ghi kết quả xử lý, **Then** hệ thống chặn; **When** D ghi kết quả "đã cho nghỉ, đo lại 11:30 bình thường", **Then** cảnh báo Đã đóng (9.4).
8. **Given** cảnh báo của A đã Đã đóng, **When** lần đo mới vượt ngưỡng, **Then** hệ thống tạo cảnh báo mới (không gộp vào cảnh báo đã đóng).
9. **Given** hai lần đo vượt ngưỡng của A được lưu gần như đồng thời từ hai thiết bị, **When** cả hai gửi, **Then** chỉ có một cảnh báo đang mở với số lần 2 (DBR-18).
10. **Given** cảnh báo của A đang Đã tiếp nhận bởi D ở ca ngày, **When** ca đêm xác nhận bàn giao (feature 008), **Then** người phụ trách chuyển sang điều dưỡng phụ trách A ở ca đêm; trạng thái giữ nguyên (BR-M05-05, BR-M09-08). Nếu tới giờ bắt đầu ca đêm mà bàn giao chưa được xác nhận, cảnh báo được chuyển tạm sang Người phụ trách ca đêm với dấu "tạm nhận, chờ xác nhận bàn giao" cho tới lúc xác nhận (FR-037, feature 008 Q-78).
11. **Given** công việc Bắt buộc "xoay trở" của A có cảnh báo quá hạn, **When** feature 005 báo công việc được ghi Hoàn thành với thời điểm thực hiện trong khung, **Then** cảnh báo chuyển Đã đóng, người đóng là Hệ thống, kết quả "nguồn đã được xử lý: đã thực hiện đúng khung" (FR-032).
12. **Given** nhân viên chăm sóc S, **When** S thử tiếp nhận cảnh báo của A, **Then** hệ thống chặn; S chỉ xem được cảnh báo của người mình được phân công (4.4 dòng "Xử lý cảnh báo": CS X).
13. **(Bổ sung, CHK065, Q-76)** **Given** cảnh báo "bỏ lỡ liều" của A đã ở Chuyển sự cố và sự cố liên kết đang Đang xử lý bởi D, **When** feature 006 báo liều đó được đính chính thành Đã dùng, **Then** cảnh báo giữ Chuyển sự cố, sự cố có thêm diễn biến "nguồn đã được xử lý: liều được đính chính Đã dùng" do Hệ thống ghi, D được thông báo, và sự cố không tự đóng (FR-032).

---

### User Story 4 - Ghi nhận và xử lý sự cố ba mức (Priority: P1)

Bất kỳ nhân viên nào phát hiện sự cố đều ghi nhận được, kể cả với người cao tuổi ngoài phạm vi phân công. Sự cố có người cao tuổi, thời gian, địa điểm, hoạt động đang diễn ra, người phát hiện, mô tả, mức độ, xử lý ban đầu và người được thông báo. Sự cố đi qua các bước Ghi nhận → Đánh giá → Xử lý → Thông báo → Theo dõi → Đóng; mọi bổ sung là bản ghi diễn biến ghi thêm, không sửa nội dung đã ghi. Sự cố chỉ đóng khi có kết quả xử lý; mức Trung bình trở lên cần điều dưỡng hoặc bác sĩ đóng.

**Why this priority**: Sự cố là hồ sơ pháp lý và chuyên môn của viện; cần ghi đủ, đúng lúc, không bị sửa (9.2, 1.5 nhóm 3).

**Independent Test**: Nhân viên bếp ghi sự cố Nhẹ "bỏ bữa" cho người cao tuổi ngoài phạm vi; điều dưỡng ghi sự cố Trung bình "từ chối thuốc"; kiểm tra phạm vi hiển thị, thông báo theo mức, và điều kiện đóng.

**Acceptance Scenarios**:

1. **Given** nhân viên bếp K không được phân công cho A, **When** K ghi nhận sự cố cho A, **Then** K chọn được A và chỉ thấy họ tên, ảnh, phòng/giường, không thấy thông tin sức khỏe; nhật ký đánh dấu "ngoài phạm vi" (feature 002 FR-044a).
2. **Given** S ghi sự cố "khó ngủ" mức Nhẹ cho A, **When** lưu, **Then** sự cố ở trạng thái Mới, điều dưỡng phụ trách A được thông báo trong ứng dụng, hạn tiếp nhận là hết ca hiện tại (9.3, BR-M13-01).
3. **Given** D ghi sự cố "từ chối thuốc" mức Trung bình cho A, **When** lưu, **Then** thông báo gửi qua ứng dụng và tin nhắn cho điều dưỡng phụ trách và Người phụ trách ca, hạn tiếp nhận CFG-M05-01; bác sĩ được báo cần đánh giá lại, căn cứ "sau sự cố" (feature 001 FR-040, BR-M01-02).
4. **Given** sự cố Trung bình của A đang Đang xử lý, **When** Trưởng tầng T (không phải điều dưỡng) đóng sự cố, **Then** hệ thống chặn; **When** D đóng kèm kết quả xử lý, **Then** sự cố Đã đóng (BR-M05-09).
5. **Given** sự cố Nhẹ đang Đang theo dõi, **When** T đóng kèm kết quả, **Then** được phép.
6. **Given** sự cố đã ghi, **When** người ghi muốn sửa mô tả, **Then** không có thao tác sửa; người ghi tạo bản ghi diễn biến bổ sung hoặc bản đính chính có lý do, bản gốc giữ nguyên (1.5 nhóm 3).
7. **Given** D xử lý sự cố Trung bình và thấy tình trạng xấu đi, **When** D nâng mức lên Khẩn cấp kèm lý do, **Then** quy trình khẩn cấp được kích hoạt (User Story 5).
8. **Given** cảnh báo Trung bình của A đang Đang xử lý, **When** D chọn "Chuyển sự cố", **Then** hệ thống tạo sự cố liên kết cảnh báo, mức mặc định bằng mức cảnh báo, và cảnh báo chuyển Chuyển sự cố (9.4).
9. **Given** người thân N của A có bản đồng ý chia sẻ dữ liệu đang hiệu lực bao gồm N, **When** N xem cổng người thân, **Then** N thấy sự cố của A theo quyền; người thân không có bản đồng ý thì không thấy (4.4 chú thích ¹).
10. **(Bổ sung, CHK033)** **Given** sự cố Trung bình của A đang Đang xử lý, đã có một diễn biến đánh giá và một diễn biến xử lý, **When** D "Chuyển theo dõi", **Then** sự cố chuyển Đang theo dõi; **When** tình trạng tái diễn và D "Mở lại xử lý" mà không nhập lý do, **Then** hệ thống chặn; **When** có lý do, **Then** sự cố về Đang xử lý và lịch sử ghi lần mở lại (bảng FR-045).
11. **(Bổ sung, 2026-10-11, Q-339)** **Given** sự cố Trung bình của A vừa được nhân viên chăm sóc ghi nhận, đang Mới, **When** Trưởng tầng T chọn "Giao người xử lý" cho điều dưỡng D (có quyền với A) kèm ghi chú, **Then** sự cố chuyển Đang xử lý với D là người xử lý, leo thang dừng, D nhận thông báo mức Trung bình và lịch sử ghi lần phân công; **When** T giao cho nhân viên chăm sóc, hoặc không ghi chú, **Then** hệ thống chặn; **When** Quản lý viện giao lại sự cố đang Đang xử lý cho Bác sĩ B, **Then** người xử lý đổi sang B, trạng thái giữ nguyên, Quản lý viện vẫn không đóng được sự cố.

---

### User Story 5 - Quy trình khẩn cấp: thẻ thông tin khẩn cấp, đối chiếu nguyện vọng, chuyển viện (Priority: P1)

Khi một sự cố Khẩn cấp được tạo (trực tiếp, nâng mức, hoặc từ cảnh báo), hệ thống hiển thị ngay cho người xử lý thẻ thông tin khẩn cấp: nguyện vọng chăm sóc cuối đời, dị ứng đang hiệu lực, thuốc đang dùng, bệnh nền, lưu ý chăm sóc (sửa, Q-281), người liên hệ chính và số điện thoại. Đồng thời hệ thống thông báo song song cho bác sĩ trực, trưởng tầng, quản lý và người liên hệ chính. Nếu người cao tuổi có nguyện vọng cuối đời đã ghi nhận, người xử lý phải xác nhận "đã đối chiếu nguyện vọng" trước khi ghi biện pháp hồi sức. Nếu cần chuyển viện, lệnh Chuyển viện được thực hiện từ sự cố và hệ thống tạo bản tóm tắt chuyển viện.

**Why this priority**: Vài phút đầu của cấp cứu quyết định kết quả; thông tin sai hoặc thiếu (dị ứng, nguyện vọng không hồi sức) gây hại không thể đảo ngược (9.5, BR-M05-13).

**Independent Test**: Cho A có nguyện vọng "không đặt nội khí quản", dị ứng Penicillin, hai thuốc đang dùng; điều dưỡng kích hoạt khẩn cấp; kiểm tra nội dung thẻ, bốn người nhận thông báo, việc chặn ghi biện pháp hồi sức trước khi xác nhận, và bản tóm tắt khi chuyển viện.

**Acceptance Scenarios**:

1. **Given** A có nguyện vọng cuối đời đã ghi nhận, dị ứng Penicillin Hiệu lực, đơn Amlodipin và Metformin đang dùng, bệnh nền tăng huyết áp, lưu ý chăm sóc "nguy cơ ngã cao, hỗ trợ khi di chuyển", người liên hệ chính là con gái M, **When** D tạo sự cố Khẩn cấp "khó thở" cho A, **Then** thẻ thông tin khẩn cấp hiển thị ngay đủ các mục trên kèm họ tên, ảnh, phòng/giường, và nội dung thẻ tại thời điểm đó được lưu cùng sự cố (9.5, BR-M05-13).
2. **Given** như trên, **When** sự cố được tạo, **Then** trong cùng lúc hệ thống phát thông báo khẩn cấp song song cho Bác sĩ trực, Trưởng tầng của tầng A, Quản lý viện, người liên hệ chính M, điều dưỡng phụ trách A và Người phụ trách ca (BR-M05-06, BR-M13-01); người nhận chưa xác nhận sau CFG-M13-01 (mặc định \[5 phút\]) được xử lý theo feature 009 (BR-M13-02).
3. **Given** sự cố Khẩn cấp của A vừa tạo, **When** D ghi biện pháp hồi sức "ép tim ngoài lồng ngực" khi chưa xác nhận đối chiếu nguyện vọng, **Then** hệ thống chặn và hiển thị lại nguyện vọng; **When** D xác nhận "đã đối chiếu nguyện vọng", **Then** hệ thống lưu người xác nhận, thời điểm, và cho ghi biện pháp hồi sức (BR-M05-13, DBR-19).
4. **Given** người cao tuổi C không có nguyện vọng cuối đời đã ghi nhận, **When** D kích hoạt khẩn cấp cho C, **Then** thẻ ghi rõ "chưa ghi nhận nguyện vọng cuối đời" và biện pháp hồi sức ghi được ngay, không cần xác nhận.
5. **Given** sự cố Khẩn cấp của A, **When** D ghi các hành động: 10:02 gọi bác sĩ trực, 10:04 gọi cấp cứu 115, 10:20 xe cấp cứu đến, **Then** mỗi hành động là một bản ghi diễn biến có thời điểm và người ghi; hồ sơ sự cố có thời gian phát hiện, người xử lý, thời gian gọi hỗ trợ/cấp cứu, người được thông báo (9.5).
6. **Given** sự cố Khẩn cấp của A Đang xử lý và A Đang lưu trú, **When** bác sĩ B chọn "Chuyển viện" tới Bệnh viện X, **Then** trong cùng một lệnh: lệnh Chuyển viện của feature 001/004 được thực hiện (A chuyển Điều trị tại bệnh viện, căn cứ là sự cố này) và bản tóm tắt chuyển viện được tạo gồm dị ứng, thuốc đang dùng, chỉ số gần nhất của từng loại kèm thời điểm, và diễn biến sự cố (BR-M05-14).
7. **Given** A đã ở Điều trị tại bệnh viện, **When** B chọn "Chuyển viện" từ một sự cố khác, **Then** lệnh bị từ chối vì trạng thái không cho phép (5.5) và không tạo bản tóm tắt ("hoặc toàn bộ, hoặc không").
8. *(Bổ sung 2026-10-22, Q-383, Q-384)* **Given** A có phiếu nguyện vọng khi điều trị: bệnh viện ưu tiên X, cần hỏi trước, người cần báo R, người đi kèm "Gia đình", **When** bác sĩ B mở lệnh Chuyển viện cho sự cố Trung bình của A, **Then** màn hiện các thông tin này; B chọn X, ghi người đi kèm, chưa liên lạc được R nên nhập lý do; lệnh thành công, phiếu tóm tắt PDF có nguyện vọng khi điều trị, dòng "không thay giấy chuyển cơ sở khám bệnh, chữa bệnh"; in lần hai có dấu "Bản in lại" (FR-049, FR-050h).
8. **Given** sự cố Khẩn cấp đã ghi, **When** bất kỳ ai thử sửa hay xóa nội dung, **Then** không có thao tác đó; bổ sung hay sửa sai bằng bản đính chính có lý do (BR-M05-08, BR-M15-03).
9. **Given** thiết bị của D mất kết nối, **When** D kích hoạt khẩn cấp, **Then** hệ thống không lưu tạm yêu cầu này trên thiết bị và báo rõ "không gửi được – gọi trực tiếp theo quy trình của cơ sở" (8.6, Q-01).
10. **(Bổ sung, CHK034)** **Given** sự cố của A từng ở mức Khẩn cấp và đã được Bác sĩ hạ xuống Trung bình kèm lý do, **When** bất kỳ ai thử sửa hay xóa nội dung, diễn biến hoặc xác nhận đối chiếu nguyện vọng của sự cố, **Then** không có thao tác đó; sự cố vẫn thuộc danh sách bảo vệ và chỉ nhận bản đính chính có lý do (FR-046, DBR-19).
11. **(Bổ sung, CHK065, Q-74)** **Given** ca đêm không có ai giữ nhiệm vụ Bác sĩ trực, **When** D tạo sự cố Khẩn cấp cho A, **Then** thông báo khẩn cấp gửi song song cho mọi Bác sĩ đang hoạt động của cơ sở cùng các người nhận khác của FR-047, sự cố mang dấu "không có Bác sĩ trực tại thời điểm", và Quản lý viện được báo (FR-047b).

---

### User Story 6 - Sự cố ngã và theo dõi sau ngã (Priority: P2)

Khi sự cố loại Ngã được ghi nhận, trong cùng lần ghi hệ thống báo bác sĩ cần đánh giá lại, báo điều dưỡng phụ trách xem xét kế hoạch chăm sóc và báo Người phụ trách ca lập công việc theo dõi sau ngã: đo sinh hiệu mỗi CFG-M05-06 trong khoảng theo dõi. Hệ thống không tạo yêu cầu, lịch đo hay công việc nào (sửa 2026-10-06, Q-306, Q-307).

**Why this priority**: Người cao tuổi sau ngã có nguy cơ biến chứng muộn (chảy máu nội sọ, gãy xương ẩn); theo dõi có lịch là biện pháp tối thiểu (BR-M05-07).

**Independent Test**: Ghi sự cố ngã cho A lúc 10:00; kiểm tra ba thông báo (bác sĩ, điều dưỡng phụ trách, Người phụ trách ca), A hiện "đang theo dõi sau ngã" tới 10:00 sau 72 giờ, và không có yêu cầu, lịch đo hay công việc nào được tự tạo; thử lập công việc theo dõi ngoài khoảng theo dõi và kiểm tra bị chặn.

**Acceptance Scenarios**:

1. **Given** A không đang theo dõi sau ngã, **When** S ghi sự cố "ngã trong phòng tắm" lúc 10:00, **Then** trong cùng lần ghi: bác sĩ được báo cần đánh giá lại, căn cứ "sau sự cố ngã" (feature 001 FR-040, BR-M01-02); điều dưỡng phụ trách được báo xem xét kế hoạch chăm sóc (feature 005 FR-011, BR-M04-20); Người phụ trách ca của tầng được báo lập công việc đo các chỉ số nhóm "sinh hiệu" mỗi CFG-M05-06 (mặc định \[4 giờ\] trong \[72 giờ\]) từ 10:00, lần đo đầu 14:00; A hiện "đang theo dõi sau ngã" tới 10:00 sau 72 giờ (BR-M05-07).
2. **Given** loại Ngã có mức mặc định Khẩn cấp trong danh mục loại sự cố, **When** S ghi sự cố ngã mà chọn mức Nhẹ, **Then** hệ thống bắt buộc lý do chọn mức thấp hơn mặc định (FR-042).
3. **Given** A đang theo dõi sau ngã còn 30 giờ, **When** A ngã lần nữa, **Then** khoảng theo dõi tính lại từ lần ngã này và Người phụ trách ca được báo; mỗi người tối đa một khoảng theo dõi sau ngã đang diễn ra (FR-052).
4. **Given** A đang theo dõi sau ngã, **When** một lần đo theo dõi vượt ngưỡng, **Then** cảnh báo được tạo như User Story 1.
5. **Given** A được chuyển viện trong thời gian theo dõi, **When** A vắng mặt, **Then** công việc đo trong khoảng vắng chuyển Hủy (feature 005 FR-020); khoảng theo dõi không tự kéo dài.
6. **Given** bác sĩ B đánh giá A ổn định sau 24 giờ, **When** B thực hiện "Kết thúc theo dõi sau ngã" kèm lý do, **Then** A hết "đang theo dõi sau ngã" và công việc theo dõi Chưa đến hạn có căn cứ là sự cố này chuyển Hủy.
7. **(Bổ sung, CHK065, Q-73)** **Given** sự cố ngã của A ghi nhầm người (đúng ra là của B), các thông báo đã gửi và A đang theo dõi sau ngã, **When** D đính chính "Hủy ghi nhận" kèm lý do, **Then** sự cố chuyển Đã hủy và vẫn xem được; khoảng theo dõi không tự bị thu hồi; Điều dưỡng phụ trách A và các Bác sĩ nhận danh sách tác động còn hiệu lực kèm lệnh tương ứng (Kết thúc theo dõi sau ngã); thông báo đã gửi không bị thu hồi (FR-046a).

---

### User Story 7 - Cảnh báo theo xu hướng (Priority: P2)

Ngoài so ngưỡng từng lần đo, hệ thống theo dõi xu hướng: cân nặng giảm nhiều trong một khoảng ngày, bỏ lỡ lần đo theo lịch, mất ngủ nhiều đêm liên tiếp. Mỗi quy tắc bật/tắt được, có tham số và mức cảnh báo cấu hình được.

**Why this priority**: Nhiều vấn đề (suy dinh dưỡng, trầm cảm) không vượt ngưỡng ở lần đo nào nhưng lộ ra qua xu hướng (BR-M05-04).

**Independent Test**: Nhập chuỗi cân nặng 50,0 → 48,5 → 47,4 kg trong 25 ngày; ghi "mất ngủ" 3 đêm liên tiếp; để một công việc đo theo lịch kết thúc Không thực hiện; kiểm tra ba cảnh báo đúng loại và mức.

**Acceptance Scenarios**:

1. **Given** cân nặng cao nhất của A trong CFG-M05-03 (mặc định \[5% trong 30 ngày\]) trước lần đo là 50,0 kg, **When** ghi 47,4 kg (giảm 5,2%), **Then** hệ thống tạo cảnh báo "sụt cân" theo mức cấu hình của quy tắc (mặc định Trung bình), nêu hai lần đo được so (BR-M05-04).
2. **Given** chất lượng ngủ của A được ghi "mất ngủ" (feature 005) hai đêm liên tiếp, **When** đêm thứ ba cũng ghi "mất ngủ" (CFG-M05-05, mặc định \[3\]), **Then** hệ thống tạo cảnh báo "mất ngủ kéo dài" (mặc định Nhẹ); đêm không có ghi nhận ngủ làm đứt chuỗi.
3. **Given** công việc đo huyết áp 17:00 của A lập theo lịch đo, **When** công việc kết thúc Không thực hiện, hoặc tới hết ca vẫn Quá hạn, **Then** hệ thống tạo hoặc gộp đúng một cảnh báo "bỏ lỡ lần đo theo lịch: huyết áp"; nếu công việc là Bắt buộc, yêu cầu cảnh báo quá hạn của feature 005 lúc trước đã tạo chính cảnh báo này ở mức Trung bình, không có cảnh báo "công việc quá hạn" riêng; công việc bị Hủy do vắng mặt không tính (BR-M05-04, FR-039b, Q-75).
4. **Given** feature 006 báo A từ chối cùng một thuốc CFG-M05-04 (mặc định \[2\]) lần liên tiếp, **When** nhận yêu cầu, **Then** hệ thống tạo hoặc gộp cảnh báo "từ chối thuốc" mức Trung bình (BR-M07-13, feature 006 FR-032).
5. **Given** Quản lý viện tắt quy tắc "mất ngủ kéo dài", **When** A có 3 đêm mất ngủ, **Then** không tạo cảnh báo; việc tắt được ghi nhật ký kèm lý do (BR-M15-04).

---

### User Story 8 - Sự cố lây nhiễm: truy vết tiếp xúc và khoanh vùng (Priority: P2)

Khi phát hiện dấu hiệu nghi ngờ lây nhiễm, nhân viên ghi sự cố loại "lây nhiễm nghi ngờ". Điều dưỡng hoặc Bác sĩ lập danh sách tiếp xúc, tra cứu dữ liệu sẵn có theo các nhóm (người cùng phòng, người cùng tham gia hoạt động, nhân viên được phân công chăm sóc, người thân đã đến thăm) rồi xác nhận; hệ thống không tự đề xuất. Người phụ trách ca được báo lập công việc đo nhiệt độ cho người cao tuổi tiếp xúc (sửa 2026-10-06, Q-307). Bác sĩ hoặc quản lý khoanh vùng khu vực; hệ thống chặn đăng ký thăm mới, hoạt động chung và phân bổ giường mới trong vùng cho tới khi gỡ.

**Why this priority**: Ổ dịch trong viện dưỡng lão lan nhanh và có tỷ lệ tử vong cao; truy vết thủ công bằng sổ giấy chậm và sót (9.6).

**Independent Test**: Dựng dữ liệu 5 ngày: A ở P201 cùng B; A tham gia 2 buổi hoạt động cùng C, E; S chăm sóc A; người thân N thăm A. Ghi sự cố lây nhiễm cho A; kiểm tra người lập tra cứu được đúng 6 người theo các nhóm trong khoảng thời gian và không có danh sách nào được hệ thống tự lập; khoanh vùng tầng 2 và thử đăng ký thăm, đăng ký hoạt động, phân bổ giường.

**Acceptance Scenarios**:

1. **Given** trong CFG-M05-07 (mặc định \[5 ngày\]) trước thời điểm phát hiện, A ở chung P201 với B (feature 003), cùng điểm danh hai buổi hoạt động với C và E (feature 014), được S chăm sóc (feature 008), được người thân N thăm (feature 012), **When** D ghi sự cố "sốt, ho – nghi cúm" loại lây nhiễm nghi ngờ cho A, **Then** A mang dấu "nghi nhiễm", D và các Bác sĩ được báo cần lập danh sách tiếp xúc; hệ thống không tự lập danh sách; **When** D lập danh sách (Nháp) và tra cứu theo các nhóm, **Then** D thấy B, C, E, S, N kèm nguồn và khoảng thời gian tiếp xúc; người chỉ tiếp xúc trước khoảng này không có trong kết quả tra cứu (BR-M05-10).
2. **Given** A Tạm vắng từ ngày thứ 3 đến ngày thứ 4 của khoảng truy vết, **When** D tra cứu, **Then** các khoảng A vắng mặt được đánh dấu để người lập xem xét (feature 003 FR-036).
3. **Given** danh sách Nháp, **When** điều dưỡng D (phụ trách A) thêm B, C, F (F với lý do "ăn chung bàn"), S, N và không thêm E (ngồi bàn khác, cách xa), rồi xác nhận, **Then** danh sách Đã xác nhận gồm B, C, F, S, N; B, C, F mang dấu "tiếp xúc" và Người phụ trách ca của tầng họ được báo lập công việc đo nhiệt độ theo CFG-M05-08 (mặc định \[2 lần/ngày trong 7 ngày\]); S và Quản lý viện được thông báo; N được thông báo qua feature 009 nếu có kênh nhận, nếu không thì N nằm trong danh sách để Hành chính liên hệ trực tiếp với cùng nội dung (FR-062) (BR-M05-10, 11).
4. **Given** bác sĩ B khoanh vùng Tầng 2 với lý do và gắn sự cố của A, **When** khoanh vùng có hiệu lực, **Then** hệ thống chặn đăng ký thăm mới cho người cao tuổi ở Tầng 2 (BR-M10-02), chặn đăng ký hoạt động chung (BR-M04-15), chặn phân bổ giường mới trong Tầng 2 (feature 003 FR-009) (sửa, 2026-10-06, Q-282: không sinh công việc khử khuẩn); Trưởng tầng, Hành chính và điều dưỡng của Tầng 2 được thông báo; dashboard hiển thị khu đang khoanh vùng (18.5) (BR-M05-11).
5. **Given** Tầng 2 đang khoanh vùng, **When** người thân đăng ký thăm A, hành chính phân bổ giường mới ở P203, hoặc trưởng tầng đăng ký A vào buổi thể dục chung, **Then** cả ba bị chặn và nêu lý do "khu đang khoanh vùng".
6. **Given** Tầng 2 đang khoanh vùng, **When** Trưởng tầng T thử gỡ khoanh vùng, **Then** hệ thống chặn (4.4 dòng "Khoanh vùng lây nhiễm": TT X); **When** bác sĩ B gỡ kèm lý do, **Then** vùng chuyển Đã gỡ, các chặn tự động bỏ (trừ phòng đang cách ly riêng) (BR-M05-12). *(Sửa 2026-10-06, Q-282: không sinh khử khuẩn kết thúc.)*
7. **Given** Tầng 2 đang khoanh vùng gắn sự cố của A và không vùng nào khác gắn sự cố này, **When** D đóng sự cố của A, **Then** hệ thống chặn cho tới khi vùng được gỡ (FR-066).
8. **Given** B trong danh sách tiếp xúc Đã xác nhận, **When** B bị loại khỏi danh sách với lý do, **Then** dấu "tiếp xúc" của B được gỡ và công việc đo nhiệt độ Chưa đến hạn có căn cứ là danh sách này chuyển Hủy.
9. **(Bổ sung, CHK033; sửa 2026-10-06, Q-307)** **Given** danh sách tiếp xúc của sự cố của A còn ở Nháp, D đã thêm F, **When** feature 012 ghi nhận thêm một lượt thăm A trong khoảng truy vết, **Then** danh sách không tự đổi; D tra cứu lại và tự thêm người thân của lượt thăm đó nếu thấy cần (bảng FR-061).

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

Bác sĩ nhận định người cao tuổi nguy kịch và ghi dấu nguy kịch (không có Bác sĩ trực thì Điều dưỡng ghi dấu tạm). Hệ thống tạo ngay cảnh báo Khẩn cấp hiển thị nguyện vọng cuối đời, báo đội ngũ và gia đình, và báo Bác sĩ trực, Điều dưỡng phụ trách gọi xác nhận lại nguyện vọng (không tạo yêu cầu gọi điện, Q-307). Sau khi có kết quả, người xử lý thực hiện lựa chọn: chuyển bệnh viện điều trị tích cực, đưa về nhà, hoặc ở lại viện chăm sóc giảm nhẹ.

**Why this priority**: Góp ý nghiệp vụ Q-213: quyết định lúc nguy kịch có ý nghĩa pháp lý và không đảo ngược được; hệ thống phải bảo đảm nhân viên thấy nguyện vọng và đã hỏi lại gia đình.

**Independent Test**: Cho A có nguyện vọng "đưa về nhà", B chưa có nguyện vọng. Ghi dấu nguy kịch cho cả hai, thử đóng cảnh báo khi chưa xác nhận lại, ghi các kết quả xác nhận khác nhau và kiểm tra lệnh được thực hiện.

**Acceptance Scenarios**:

1. **Given** A có nguyện vọng Hiệu lực "đưa về nhà", **When** Bác sĩ BS1 ghi dấu nguy kịch cho A kèm nhận định, **Then** trong cùng lần: có đúng một cảnh báo Khẩn cấp "nguy kịch – thực hiện nguyện vọng cuối đời" hiển thị nguyện vọng; Bác sĩ trực, Điều dưỡng phụ trách, Trưởng tầng, Quản lý viện được báo; người liên hệ chính và người đại diện nhận thông báo Khẩn cấp không có nội dung nguyện vọng; Bác sĩ trực và Điều dưỡng phụ trách được báo gọi xác nhận lại, bắt đầu từ người đại diện; không có yêu cầu gọi điện nào được tạo (FR-050b).
2. **Given** 23:00 không có Bác sĩ trực, **When** điều dưỡng D ghi dấu tạm cho A, **Then** cảnh báo được tạo như kịch bản 1 và mọi Bác sĩ đang hoạt động được báo để xác nhận dấu (FR-050a, Q-218).
3. **Given** cảnh báo nguy kịch của A đang xử lý và chưa có kết quả xác nhận lại, **When** D tìm cách đóng cảnh báo, **Then** hệ thống chặn (FR-050d).
4. **Given** D gọi được người đại diện P và P muốn đổi sang "chuyển bệnh viện điều trị tích cực", **When** D ghi kết quả "nguyện vọng mới" với người trả lời P, **Then** feature 001 ghi phiên bản nguyện vọng mới, phiên bản cũ Được thay thế; **When** D ghi lựa chọn "chuyển bệnh viện", **Then** lệnh Chuyển viện được thực hiện kèm bản tóm tắt; dấu nguy kịch tự gỡ; cảnh báo đóng được (FR-050c, FR-050d, FR-049).
5. **Given** giữ nguyện vọng "đưa về nhà", **When** D ghi lựa chọn, **Then** feature 004 nhận yêu cầu Cho tạm vắng lý do "về nhà theo nguyện vọng cuối đời" để Hành chính hoặc Trưởng tầng thực hiện qua quy trình đón (FR-050d, Q-218).
6. **Given** B chưa có nguyện vọng Hiệu lực và không liên lạc được ai trong thứ tự gọi, **When** BS1 ghi "không liên lạc được" và quyết định "ở lại viện chăm sóc giảm nhẹ" kèm lý do, **Then** điều dưỡng phụ trách được báo xem xét kế hoạch chăm sóc (feature 005 FR-011); khi thiếu lý do, hệ thống chặn (FR-050c, FR-050d, BR-M05-16).
7. **Given** A có dấu nguy kịch, **When** tình trạng ổn định và BS1 gỡ dấu có lý do, **Then** dấu chuyển Đã gỡ; ghi dấu lại sau đó tạo cảnh báo mới (bảng trạng thái dấu nguy kịch).

---

### User Story 12 - Người cao tuổi bấm nút SOS (Priority: P1) *(bổ sung 2026-10-18, Q-369)*

Người cao tuổi có tài khoản chạm nút SOS trên điện thoại khi cần giúp. Hệ thống tạo cảnh báo Khẩn cấp và báo ngay nhân viên lâm sàng của tầng; nhân viên tới nơi rồi quyết định đóng, ghi sự cố hay kích hoạt khẩn cấp.

**Why this priority**: Là đường gọi trợ giúp trực tiếp của người cao tuổi; báo chậm hoặc mất tín hiệu có thể gây hại ngay.

**Independent Test**: Với người cao tuổi A ở Tầng 2 có tài khoản và thiết bị đã gắn, Điều dưỡng phụ trách D, Người phụ trách ca P, Bác sĩ trực B, Trưởng tầng T, người liên hệ chính R, Quản lý viện Q, bấm SOS trong các trạng thái khác nhau của A bằng đồng hồ giả lập.

**Acceptance Scenarios**:

1. **Given** A Đang lưu trú, ở viện, **When** A chạm nút SOS lúc 14:00 và không bấm Hủy trong 3 giây đếm ngược, **Then** cảnh báo Khẩn cấp loại "người cao tuổi yêu cầu trợ giúp" được tạo ở Mới với phòng, giường của A; D, P, B, T nhận đồng thời mọi kênh; R và Q không nhận; ứng dụng của A hiện "Đã gửi" (FR-047d, FR-047e).
2. **Given** A vừa chạm nút SOS, **When** A bấm Hủy trước khi hết 3 giây đếm ngược, **Then** không có lệnh nào gửi tới hệ thống, không có cảnh báo nào được tạo; **When** A chạm nút lần nữa trong lúc đếm ngược, **Then** lệnh được gửi ngay (Q-372).
3. **Given** cảnh báo SOS của A đang Mới, **When** A bấm thêm hai lần, **Then** không có cảnh báo mới; cảnh báo cũ có số lần 3 (FR-028, FR-030).
4. **Given** cảnh báo SOS của A đang Mới, **When** D tiếp nhận, **Then** ứng dụng của A hiện "D đang đến" (FR-047f).
5. **Given** A đang Tạm vắng, **When** A chạm nút SOS và hết đếm ngược, **Then** lệnh bị từ chối, không tạo cảnh báo; ứng dụng hiện số liên hệ của viện và 115 (FR-047d).
6. **Given** A ở Hoạt động bên ngoài trong chuyến do trưởng đoàn L dẫn, có người đi cùng M, **When** A bấm SOS, **Then** L và M nhận thêm thông báo cùng D, P, B, T (FR-047e).
7. **Given** D tới nơi và A chỉ cần nước uống, **When** D đóng cảnh báo với kết quả "hỗ trợ uống nước", **Then** cảnh báo Đã đóng; không có sự cố; R không được báo.
8. **Given** D tới nơi và A đã ngã, **When** D chọn "Kích hoạt khẩn cấp từ cảnh báo", **Then** cảnh báo chuyển sự cố, quy trình khẩn cấp chạy và R được báo theo FR-047.
9. **Given** cảnh báo SOS của A quá hạn tiếp nhận mức Khẩn cấp mà chưa ai tiếp nhận, **When** Bộ lập lịch kiểm tra, **Then** cảnh báo leo thang theo BR-M05-01 như mọi cảnh báo khác.
10. **Given** A bấm SOS 15 lần trong một tuần, **When** T tìm thao tác tắt nút SOS của A, **Then** không có thao tác này; D ghi được lưu ý chăm sóc (FR-047g).
11. *(bổ sung 2026-10-22, Q-373)* **Given** A đã cho ứng dụng quyền vị trí, đang ở viện, **When** A bấm SOS và máy có tọa độ sau 12 giây, **Then** cảnh báo được tạo ngay với phòng, giường (không chờ tọa độ); 12 giây sau cảnh báo có thêm tọa độ kèm sai số, thời điểm đo, nhãn "tham khảo"; ứng dụng gửi lại vị trí mỗi 30 giây tới khi D ghi đã tới nơi (FR-047h).
12. *(bổ sung 2026-10-22, Q-373)* **Given** A ở Hoạt động bên ngoài và đã cho quyền vị trí, **When** A bấm SOS, **Then** người nhận thấy tọa độ là vị trí chính trên bản đồ kèm nút chỉ đường; R không thấy vị trí (FR-047h).
13. *(bổ sung 2026-10-22, Q-374)* **Given** D đã tiếp nhận cảnh báo SOS của A lúc 14:01, **When** D ghi "Đã tới nơi" lúc 14:03, **Then** ứng dụng của A hiện "D đã tới"; ứng dụng dừng gửi lại vị trí; báo cáo có thời gian tới nơi 3 phút (FR-047j, FR-081).

---

### User Story 13 - Nhân viên bấm nút hỗ trợ khẩn cấp, gọi 115 (Priority: P1) *(bổ sung 2026-10-22, Q-375, Q-376)*

Nhân viên đang trong ca cần người tới giúp ngay (người cao tuổi ngừng tim, ngã, kích động) chạm nút hỗ trợ khẩn cấp trên app nhân viên. Hệ thống tạo cảnh báo Khẩn cấp, báo Người phụ trách ca, Bác sĩ trực và nhân viên trong ca cùng tầng. Người xử lý gọi 115 từ màn cảnh báo hoặc sự cố, hệ thống ghi mốc gọi cấp cứu.

**Why this priority**: Lúc khẩn cấp nhân viên không có thời gian ghi sự cố đủ trường; báo chậm làm chậm cấp cứu.

**Independent Test**: Với Tầng 2 có Nhân viên chăm sóc C, Điều dưỡng D2 cùng ca, Người phụ trách ca P, Bác sĩ trực B, Nhân viên bếp K (không có tầng), người cao tuổi A, người liên hệ chính R, dùng đồng hồ giả lập.

**Acceptance Scenarios**:

1. **Given** C đang trong ca ở Tầng 2, **When** C chạm nút hỗ trợ khẩn cấp và không Hủy trong 3 giây, **Then** cảnh báo Khẩn cấp "Nhân viên yêu cầu hỗ trợ" được tạo ngay, không gắn người cao tuổi, gắn Tầng 2; P, B, D2 nhận đồng thời mọi kênh; R không nhận (FR-047i).
2. **Given** cảnh báo vừa tạo, **When** C chọn loại "Ngã", nơi "Nhà vệ sinh" và người cao tuổi A, **Then** cảnh báo được ghi thêm các lựa chọn; người nhận như Q-67 của A (Điều dưỡng phụ trách, Trưởng tầng) được báo thêm (FR-047i).
3. **Given** K không có tầng trong ca, **When** K bấm nút mà chưa chọn tầng, **Then** Người phụ trách ca của mọi tầng đang có ca (gồm P) và B được báo, nhân viên khác của các tầng không được báo; **When** K chọn Tầng 3, **Then** nhân viên trong ca Tầng 3 được báo thêm (FR-047i).
4. **Given** C không trong ca, **When** C bấm nút, **Then** lệnh bị từ chối và app hiện số liên hệ của viện, số 115 (FR-047i).
5. **Given** D2 tiếp nhận rồi ghi "Đã tới nơi", **When** C xem app, **Then** C thấy "D2 đã tới" (FR-047j).
6. **Given** cảnh báo chưa chọn người cao tuổi, **When** D2 chọn "Kích hoạt khẩn cấp từ cảnh báo", **Then** hệ thống yêu cầu chọn người cao tuổi; chọn A thì sự cố Khẩn cấp của A được tạo và quy trình khẩn cấp chạy (bảng FR-029).
7. **Given** sự cố Khẩn cấp của A đang xử lý, **When** D2 bấm "Gọi 115" rồi chọn "Đã gọi được", **Then** trình quay số mở với 115; sự cố có diễn biến gọi cấp cứu kèm thời điểm bấm và người gọi; màn hiện địa chỉ viện, Tầng 2, nhà vệ sinh, họ tên, tuổi A (FR-047k).
8. **Given** sự việc là nhân viên bị thương, không liên quan người cao tuổi, **When** D2 đóng cảnh báo với kết quả, **Then** cảnh báo Đã đóng, không có sự cố.

---

### Edge Cases

- **(Bổ sung 2026-10-18, Q-369) Bấm SOS đúng lúc chuyến đi điểm danh về**: trạng thái của người cao tuổi được xét tại thời điểm hệ thống nhận lệnh; người nhận là nhóm ứng với trạng thái đó (FR-047e). Lệnh gửi lúc mất kết nối không được xếp hàng gửi sau: ứng dụng báo "chưa gửi được" và hiện số liên hệ của viện.
- **(Bổ sung 2026-10-22, Q-373) Tọa độ không cập nhật**: máy hết pin hoặc mất GPS sau lần gửi đầu; quá hai chu kỳ CFG-M05-09 không có tọa độ mới thì tọa độ cuối mang dấu "có thể đã cũ"; cảnh báo không đổi trạng thái hay người nhận.
- **(Bổ sung 2026-10-22, Q-375) Hai nhân viên cùng bấm cho một sự việc**: mỗi người bấm tạo cảnh báo riêng (khóa theo người bấm); người tiếp nhận đóng cảnh báo trùng với kết quả "trùng với cảnh báo của <người bấm>".

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
- **Sự cố không gắn người cao tuổi** (hư hỏng cơ sở vật chất; sửa, Q-282: không còn công việc vệ sinh): được phép với loại "cơ sở vật chất", bắt buộc địa điểm; không có thẻ khẩn cấp, không kích hoạt các tác động theo người cao tuổi.
- **Người tiếp xúc đã có lịch đo nhiệt độ do bác sĩ đặt**: Người phụ trách ca vẫn được báo lập công việc theo dõi tiếp xúc; khi lập, hệ thống chặn công việc trùng căn cứ và thời điểm (feature 005 FR-021), nhân viên ghi từng công việc đã lập.
- **Người tiếp xúc thuộc nhiều sự cố lây nhiễm**: mỗi lần xác nhận gắn khoảng theo dõi với sự cố đó; các khoảng trùng thời gian của cùng người cao tuổi được tính gộp thành một khoảng kéo tới thời điểm kết thúc muộn nhất.
- **Hai vùng khoanh vùng chồng nhau**: chặn còn hiệu lực khi còn ít nhất một vùng đang khoanh vùng bao phòng đó; gỡ một vùng không bỏ chặn của vùng kia.
- **Lượt thăm đã được duyệt trước khi khoanh vùng**: feature 012 tự chuyển Hủy với lý do "khu đang khoanh vùng" và báo người đăng ký; gỡ vùng không khôi phục lượt; Hành chính và Trưởng tầng nhận danh sách các lượt bị hủy (feature 012 FR-037, Q-120).
- **Giấy phép cơ sở hết hạn giữa chừng khi bác sĩ đang ghi chẩn đoán tại viện**: kiểm tra tại thời điểm lưu; sau 23:59 ngày hết hạn thì bị chặn (feature 002 FR-038).
- **Người cao tuổi vừa là người nghi nhiễm của một sự cố vừa là người tiếp xúc của sự cố khác** (CHK038): người đó mang cả hai dấu; khoảng theo dõi tiếp xúc vẫn tính theo FR-062; feature 003 bắt buộc ghi đã khử khuẩn khi Xác nhận giường sẵn sàng nếu có ít nhất một trong hai dấu (sửa, Q-282).
- **Đổi khoảng hợp lệ vật lý hoặc ngưỡng khi một bản ghi đang "chờ đo lại"** (CHK041): lần đo lại được kiểm theo khoảng hợp lệ vật lý hiện hành lúc ghi (FR-002) và so theo ngưỡng Hiệu lực tại thời điểm đo của chính nó (FR-024); bản ghi đầu giữ kết quả so đã lưu.
- **Cân nặng đầu tiên sau tiếp nhận hoặc sau thời gian không có lần đo nào trong khoảng của CFG-M05-03** (CHK042): không có cân nặng nào để so nên quy tắc sụt cân không chạy cho lần đo đó (FR-039a).
- **Đổi cơ cấu khi đang khoanh vùng** (CHK043): vùng tính theo cơ cấu hiện hành, nên phòng mới tạo trong tầng đang khoanh vùng chịu chặn ngay; việc Ngừng hiệu lực tầng, khu vực theo điều kiện của feature 003 FR-005.
- **Bộ lập lịch ngừng chạy một thời gian** (CHK035): xem FR-084.

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
| Sự cố: nội dung ghi nhận, diễn biến, lịch sử phân công người xử lý (FR-045a), xác nhận đối chiếu nguyện vọng, bản chụp thẻ thông tin khẩn cấp | 3 | Chỉ ghi thêm; sai sót xử lý bằng đính chính; sự cố Khẩn cấp thuộc danh sách bảo vệ (BR-M05-08, BR-M15-03) |
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
- **FR-003**: Bác sĩ MUST tạo được lịch đo riêng cho một người cao tuổi, gồm: chỉ số (một hoặc nhiều), tần suất (các mốc giờ trong ngày, hoặc mỗi N giờ từ một mốc, hoặc theo ngày trong tuần kèm mốc giờ), thời điểm bắt đầu, thời điểm kết thúc (nếu có), mức quan trọng (mặc định Bắt buộc), ghi chú. Lịch đo chỉ có nguồn "chỉ định bác sĩ"; không có lịch đo do hệ thống tạo (**sửa, 2026-10-06, Q-307:** bỏ nguồn "theo dõi sau ngã", "theo dõi tiếp xúc", xem FR-051, FR-062). *(Nguồn: 10.1, BR-M04-06 "đo chỉ số theo chỉ định" là Bắt buộc, Q-307)*
- **FR-004**: **(Sửa, 2026-10-06, Q-306)** Lịch đo ở Hiệu lực MUST là căn cứ để Người phụ trách ca lập công việc "đo chỉ số" ở feature 005 (BR-M04-01, feature 005 FR-017); mỗi mốc đo có tối đa một công việc theo (lịch đo, thời điểm dự kiến) (DBR-12, feature 005 FR-021). Spec này và feature 005 MUST NOT tự sinh công việc. *(Nguồn: 10.1, BR-M04-01, DBR-12, Q-306)*
- **FR-005**: Lịch đo MUST NOT sửa được chỉ số, tần suất, mốc giờ hay thời điểm bắt đầu khi đã Hiệu lực hoặc Chờ hiệu lực; thay đổi MUST dùng lệnh "Thay đổi lịch đo" tạo lịch mới trỏ về lịch cũ, lịch cũ chuyển Đã ngừng tại thời điểm hiệu lực của lịch mới, trong cùng một lệnh. *(Nguồn: 1.5 nhóm 2, BR-M04-03)*
- **FR-006**: Vòng đời lịch đo MUST theo bảng dưới; không có chuyển nào khác. *(Nguồn: 10.1, BR-M05-07, BR-M05-11, constitution IV)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái mới | Người thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Tạo lịch đo | Hiệu lực (bắt đầu không sau thời điểm lệnh) hoặc Chờ hiệu lực | Bác sĩ | Người cao tuổi chưa ở trạng thái cuối; đủ trường FR-003 | Căn cứ để Người phụ trách ca lập công việc (FR-004) |
| Chờ hiệu lực | Tới thời điểm bắt đầu | Hiệu lực | Bộ lập lịch | — | — |
| Hiệu lực, Chờ hiệu lực | Thay đổi lịch đo | Đã ngừng | Bác sĩ | Bắt buộc lý do; lịch mới hợp lệ; cùng một lệnh (FR-005) | Feature 005 báo Người phụ trách ca, Trưởng tầng để điều chỉnh công việc đã lập (feature 005 FR-022) |
| Hiệu lực, Chờ hiệu lực | Ngừng lịch đo | Đã ngừng | Bác sĩ | Bắt buộc lý do | Công việc đo Chưa đến hạn của lịch chuyển Hủy kèm lệnh (feature 005 FR-022; sửa, 2026-10-10) |
| Hiệu lực | Tới thời điểm kết thúc | Hết hạn | Bộ lập lịch | — | Không còn là căn cứ lập công việc sau thời điểm kết thúc |
| Hiệu lực, Chờ hiệu lực | Người cao tuổi chuyển trạng thái cuối | Đã ngừng | Hệ thống | — | Lý do là trạng thái cuối |

Đã ngừng và Hết hạn là trạng thái cuối. Người cao tuổi vắng mặt không làm đổi trạng thái lịch đo; feature 005 chặn lập và hủy công việc trong khoảng vắng (feature 005 FR-017, FR-020).

#### B. Ghi nhận chỉ số và so ngưỡng

- **FR-010**: Nhân viên chăm sóc và Điều dưỡng MUST ghi được chỉ số cho người cao tuổi trong phạm vi phân công, từ công việc đo (feature 005) hoặc đo ngoài lịch. Mỗi bản ghi MUST gồm: người cao tuổi, chỉ số, giá trị từng thành phần, đơn vị, thời điểm đo, người đo, công việc nguồn (nếu có), thời điểm ghi trên thiết bị và thời điểm đồng bộ (với bản ghi ngoại tuyến), ghi chú, ngưỡng đã áp dụng (loại và phiên bản), kết quả so. *(Nguồn: 10.1, UC-32, CHI_SO, 4.4 dòng "Chỉ số sức khỏe", DBR-25)*
- **FR-011**: Giá trị nằm ngoài khoảng hợp lệ vật lý của thành phần (CFG-M06-03) MUST bị chặn, không lưu, kèm thông báo khoảng hợp lệ; công việc nguồn giữ nguyên trạng thái. *(Nguồn: BR-M06-04, feature 005 FR-034)*
- **FR-012**: Thời điểm đo MUST mặc định là thời điểm ghi; người đo MAY chọn thời điểm sớm hơn nhưng MUST NOT sau thời điểm ghi. Nhãn "ghi nhận muộn" và cách xét thời điểm của bản ghi ngoại tuyến theo feature 005 FR-035 (BR-M04-12). *(Nguồn: BR-M04-12, Q-01 theo mặc định)*
- **FR-013**: Ngay khi bản ghi được lưu (hoặc đồng bộ), hệ thống MUST so từng thành phần với ngưỡng áp dụng theo FR-024 và xếp bản ghi theo thành phần nặng nhất: Bình thường, Cảnh báo, Nguy hiểm, hoặc "chưa có ngưỡng". *(Nguồn: BR-M04-10, BR-M06-01, 03)*
- **FR-014**: Kết quả Cảnh báo MUST tạo (hoặc gộp) cảnh báo mức Trung bình; kết quả Nguy hiểm đã xác nhận (FR-015 → 017) MUST tạo (hoặc gộp) cảnh báo mức Khẩn cấp. Loại cảnh báo là "vượt ngưỡng: <chỉ số>". *(Nguồn: BR-M06-03)*
- **FR-015**: Bản ghi có kết quả Nguy hiểm MUST chuyển "chờ đo lại" và hệ thống MUST yêu cầu người đo đo lại, trừ khi người đo chọn "xử lý ngay" lúc ghi. Với "xử lý ngay", bản ghi MUST được xác nhận ngay và cảnh báo Khẩn cấp MUST được tạo. *(Nguồn: BR-M06-04)*
- **FR-016**: Lần đo lại MUST trỏ về bản ghi "chờ đo lại" và được so ngưỡng như một lần đo mới; lần đo lại là bản ghi được xác nhận và dùng để tạo cảnh báo (Nguy hiểm → Khẩn cấp, Cảnh báo → Trung bình, Bình thường → không tạo). Lần đo lại MUST NOT yêu cầu đo lại thêm. Bản ghi đầu MUST được giữ với nhãn "đã đo lại". Người đo lại MAY khác người đo đầu, nhưng MUST có quyền ghi chỉ số cho người cao tuổi đó. *(Nguồn: BR-M06-04, BR-M06-06)*
- **FR-017**: Bản ghi "chờ đo lại" quá CFG-M06-04 (mặc định \[10 phút\]) mà chưa có lần đo lại MUST được xác nhận với nhãn "chưa đo lại" và cảnh báo Khẩn cấp MUST được tạo từ bản ghi đó. *(Suy ra từ BR-M06-04; CFG-M06-04 ở Phụ lục 25)*
- **FR-018**: Công việc đo nguồn MUST chỉ đóng Hoàn thành khi có bản ghi đã xác nhận (lần đo thường, lần đo lại, "xử lý ngay" hoặc hết hạn đo lại); hệ thống MUST báo feature 005 bản ghi đã xác nhận để công việc trỏ tới. *(Nguồn: feature 005 FR-034 dòng "Đo chỉ số")*
- **FR-019**: Bản ghi chỉ số MUST NOT sửa hay xóa. Đính chính MUST có lý do, trỏ đúng một bản gốc (DBR-23), do người ghi gốc, Trưởng tầng hoặc Người phụ trách ca của tầng trong thời gian ca tạo (1.5, feature 000); giá trị đính chính MUST qua FR-011. Giá trị hiện hành sau đính chính MUST được dùng cho các lần đánh giá xu hướng kể từ thời điểm đính chính. Cảnh báo đã tạo từ bản gốc MUST NOT tự đóng; người phụ trách cảnh báo MUST được thông báo "nguồn đã được đính chính". Nếu giá trị đính chính vượt ngưỡng mà bản gốc không vượt, hệ thống MUST tạo (hoặc gộp) cảnh báo theo giá trị đính chính. Bản ghi đang "chờ đo lại" chưa được xác nhận nên MUST NOT đính chính được; ghi sai thì người đo ghi lần đo lại (FR-016), hoặc bản ghi được xác nhận khi hết CFG-M06-04 rồi mới đính chính (CHK040). *(Nguồn: BR-M06-06, feature 005 FR-037)*

#### C. Ngưỡng cảnh báo

- **FR-020**: Mỗi phiên bản ngưỡng MUST gồm: phạm vi (mặc định của cơ sở, hoặc cá nhân của một người cao tuổi), chỉ số, và với từng thành phần: cận dưới nguy hiểm, cận dưới cảnh báo, cận trên cảnh báo, cận trên nguy hiểm (mỗi cận MAY để trống, nghĩa là không so phía đó); thời điểm hiệu lực; người thiết lập; lý do (bắt buộc khi thay phiên bản trước); trạng thái. Ngưỡng cá nhân của một chỉ số MUST thay toàn bộ ngưỡng mặc định của chỉ số đó. *(Nguồn: 10.3, NGUONG_CANH_BAO, BR-M06-01)*
- **FR-021**: Các cận có giá trị MUST thỏa: nằm trong khoảng hợp lệ vật lý; cận dưới nguy hiểm < cận dưới cảnh báo < cận trên cảnh báo < cận trên nguy hiểm (bỏ qua cận để trống). Vi phạm thì chặn. *(Nguồn: 10.3 "hai mức"; suy ra từ BR-M06-01, BR-M06-03: mức nguy hiểm phải nặng hơn mức cảnh báo; xem Điểm cần báo lại 17)*
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
- **FR-025**: Mỗi ngày Bộ lập lịch MUST tìm người cao tuổi đã Đang lưu trú (tính từ Hoàn tất tiếp nhận) quá CFG-M06-01 (mặc định \[7 ngày\]) mà không có phiên bản ngưỡng cá nhân Hiệu lực hay Chờ hiệu lực nào; với mỗi người, hệ thống MUST gửi nhắc tới các Bác sĩ một lần khi vừa đạt mốc và MUST giữ người đó trong danh sách "chưa có ngưỡng cá nhân" cho tới khi có ngưỡng. Khi ngưỡng cá nhân bị Ngừng mà không còn phiên bản Hiệu lực hay Chờ hiệu lực, người đó lại vào danh sách và các Bác sĩ được nhắc lại một lần (Q-330). "Các Bác sĩ" là mọi tài khoản đang Hoạt động có vai trò Bác sĩ (phạm vi toàn viện, feature 002 FR-034), không giới hạn theo Bác sĩ trực hay bác sĩ đã đánh giá (CHK014). *(Nguồn: BR-M06-02, CFG-M06-01; cách nhắc là giả định)*

#### D. Cảnh báo: tạo, gộp, vòng đời, leo thang

Thuật ngữ (CHK015): "người phụ trách" của một cảnh báo hay sự cố (viết thường) là người đang chịu trách nhiệm xử lý mục đó, đổi theo lệnh Tiếp nhận, Giao người phụ trách và theo bàn giao. "Người phụ trách ca" (viết hoa, mục 2.4) là nhiệm vụ của một nhân viên trong một ca tại một tầng. Hai khái niệm khác nhau; Người phụ trách ca chỉ là người phụ trách của một cảnh báo khi được gán theo FR-031 hoặc FR-037.

- **FR-026**: Cảnh báo MUST được tạo từ: (a) spec này — vượt ngưỡng (FR-014), quy tắc xu hướng (mục E); (b) yêu cầu của feature khác — công việc Bắt buộc quá hạn, uống nước thiếu, ăn kém kéo dài, tâm trạng tiêu cực (feature 005); liều Bỏ lỡ, phản ứng thuốc, từ chối thuốc liên tiếp, xung đột dị ứng với đơn, chưa đối chiếu thuốc, lần dùng PRN vượt giới hạn, "nghi dùng trùng liều" mức Trung bình khi hai bản ghi ngoại tuyến cùng một liều đều là Đã dùng (feature 006 FR-029, Q-277); xung đột dị ứng với chế độ ăn, thực đơn (feature 011); (c) Trưởng tầng, Bác sĩ, Điều dưỡng tạo thủ công (ví dụ hành vi bất thường), bắt buộc mô tả; (d) **(bổ sung 2026-10-18, Q-369)** người cao tuổi bấm nút SOS (FR-047d); (e) **(bổ sung 2026-10-22, Q-375)** nhân viên bấm nút hỗ trợ khẩn cấp (FR-047i). Yêu cầu từ feature khác MUST nêu người cao tuổi, loại, mức, bản ghi nguồn, thời điểm. *(Nguồn: 9.1, 4.4 dòng "Xử lý cảnh báo", feature 005 FR-044, 006 FR-052)*
- **FR-027**: Mỗi cảnh báo MUST gồm: người cao tuổi, loại, khóa loại (FR-028), mức (Nhẹ / Trung bình / Khẩn cấp), danh sách bản ghi nguồn, số lần (bắt đầu 1), thời điểm lần đầu và lần gần nhất, trạng thái, cấp leo thang hiện tại, người phụ trách, hạn tiếp nhận, người tiếp nhận và thời điểm, kết quả xử lý, người đóng (người hoặc Hệ thống), sự cố được chuyển thành (nếu có). *(Nguồn: 9.4, CANH_BAO)* **(Bổ sung 2026-10-18, Q-369)** Cảnh báo từ nút SOS MUST ghi thêm nguồn "người cao tuổi", phòng và giường lúc bấm, nơi đang ở nếu người cao tuổi chọn (FR-047f). **(Bổ sung 2026-10-22, Q-373 → Q-375)** Cảnh báo từ nút SOS hoặc nút hỗ trợ khẩn cấp MUST ghi thêm các vị trí GPS đã nhận (FR-047h) và mốc đã tới nơi (FR-047j); cảnh báo từ nút hỗ trợ khẩn cấp MUST ghi người bấm, tầng, loại sự việc, nơi, và người cao tuổi MAY trống (9.2).
- **FR-028**: "Cùng loại" (BR-M05-02, DBR-18) MUST xác định theo khóa loại gồm người cao tuổi, nguồn và đối tượng: vượt ngưỡng → chỉ số; xu hướng → quy tắc; công việc quá hạn → loại công việc (trừ công việc đo từ lịch đo, dùng khóa "bỏ lỡ lần đo theo lịch: <chỉ số>" theo FR-039b); thuốc → loại cảnh báo thuốc và đơn thuốc (xung đột dị ứng, từ chối liên tiếp: theo thuốc); uống nước, ăn kém, tâm trạng, chưa đối chiếu thuốc, nguy cơ cô lập (feature 014) → loại cảnh báo; thủ công → danh mục loại do người tạo chọn. **(Bổ sung 2026-09-28)** Cảnh báo "nguy kịch – thực hiện nguyện vọng cuối đời" MUST NOT gộp theo BR-M05-02: mỗi lần ghi dấu nguy kịch tạo đúng một cảnh báo riêng (FR-050b, BR-M05-15). **(Bổ sung 2026-10-18, Q-369)** Cảnh báo từ nút SOS dùng khóa "người cao tuổi yêu cầu trợ giúp" theo người cao tuổi: bấm lại khi cảnh báo này đang mở thì gộp. **(Bổ sung 2026-10-22, Q-375)** Cảnh báo từ nút hỗ trợ khẩn cấp dùng khóa "nhân viên yêu cầu hỗ trợ" theo người bấm. *(Nguồn: BR-M05-02, BR-M05-15, BR-M05-29; cách xác định khóa là giả định, xem Điểm cần báo lại 5)*
- **FR-029**: Vòng đời cảnh báo MUST theo bảng dưới; không có chuyển nào khác. "Đang mở" là Mới, Leo thang, Đã tiếp nhận, Đang xử lý. *(Nguồn: 9.4, BR-M05-01 → 03, 05, UC-34, UC-38, DBR-18, constitution IV)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái mới | Người thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Tạo cảnh báo | Mới | Hệ thống; Trưởng tầng, Bác sĩ, Điều dưỡng (thủ công); người cao tuổi (nút SOS, FR-047d); nhân viên trong ca (nút hỗ trợ khẩn cấp, FR-047i) | Người cao tuổi chưa ở trạng thái cuối; không có cảnh báo đang mở cùng khóa loại (nếu có thì "Phát sinh cùng loại") | Người phụ trách và hạn tiếp nhận theo FR-031; thông báo theo mức (FR-035) |
| Đang mở | Phát sinh cùng loại | Giữ nguyên | Hệ thống | — | Gộp (FR-030) |
| Mới, Leo thang | Quá hạn tiếp nhận | Leo thang | Bộ lập lịch | Chưa ở cấp cao nhất (FR-033) | Lên cấp kế tiếp; hạn mới; ghi lịch sử (BR-M05-01) |
| Mới, Leo thang | Tiếp nhận | Đã tiếp nhận | Điều dưỡng, Trưởng tầng, Bác sĩ (FR-036); Quản lý viện chỉ khi cảnh báo ở Leo thang cấp 2 | — | Người phụ trách là người tiếp nhận; dừng leo thang; lưu thời gian đến tiếp nhận. Lệnh MAY được gọi từ thao tác "Xác nhận và tiếp nhận" trên thông báo Khẩn cấp (feature 009 FR-026a, Q-102): khi đó xác nhận thông báo và lệnh tiếp nhận là hai bản ghi riêng; lệnh tiếp nhận bị từ chối (đã có người tiếp nhận) không làm mất xác nhận |
| Đã tiếp nhận | Giao người phụ trách | Đã tiếp nhận | Quản lý viện (khi là người phụ trách) | Người được giao là Điều dưỡng, Trưởng tầng hoặc Bác sĩ có quyền với người cao tuổi đó; bắt buộc ghi chú | Người phụ trách mới được thông báo; ghi lịch sử |
| Đã tiếp nhận | Bắt đầu xử lý | Đang xử lý | Người phụ trách, hoặc người có quyền FR-036 | — | — |
| Đã tiếp nhận, Đang xử lý | Ghi đã tới nơi | Giữ nguyên | Người phụ trách | Cảnh báo từ nút SOS hoặc nút hỗ trợ khẩn cấp; chưa ghi mốc này | Dừng gửi lại vị trí; trạng thái "đã tới" cho người bấm (FR-047j) |
| Đang xử lý | Đóng cảnh báo | Đã đóng | Điều dưỡng, Trưởng tầng, Bác sĩ | Bắt buộc kết quả xử lý (9.4); cảnh báo từ công việc Bắt buộc quá hạn: bắt buộc lý do (BR-M04-06) | — |
| Đang xử lý | Chuyển sự cố | Chuyển sự cố | Điều dưỡng, Trưởng tầng, Bác sĩ | Cảnh báo không gắn người cao tuổi: bắt buộc chọn người cao tuổi (FR-047i) | Tạo sự cố liên kết (FR-040) |
| Mới, Leo thang, Đã tiếp nhận | Kích hoạt khẩn cấp từ cảnh báo | Chuyển sự cố | Điều dưỡng, Trưởng tầng, Bác sĩ | Cùng một lệnh ghi lần lượt Tiếp nhận, Bắt đầu xử lý, Chuyển sự cố; cảnh báo không gắn người cao tuổi: bắt buộc chọn người cao tuổi (FR-047i) | Tạo sự cố Khẩn cấp liên kết; quy trình khẩn cấp (mục G) |
| Đang mở | Mọi nguồn đã được xử lý (yêu cầu đóng từ feature 005, 006, 011) | Đã đóng | Hệ thống | FR-032 | Kết quả "nguồn đã được xử lý: <lý do từ feature nguồn>" |
| Đang mở | Nâng mức / Hạ mức | Giữ nguyên | Điều dưỡng, Trưởng tầng, Bác sĩ; hạ từ Khẩn cấp: chỉ Bác sĩ | Bắt buộc lý do | Nếu Mới hoặc Leo thang: hạn tiếp nhận tính lại theo mức mới từ thời điểm đổi; thông báo theo mức mới khi nâng |
| Đang mở | Bàn giao ca được xác nhận | Giữ nguyên | Hệ thống | Người phụ trách thuộc ca vừa bàn giao | Chuyển người phụ trách (FR-037) |
| Đang mở | Người cao tuổi chuyển trạng thái cuối | Giữ nguyên | Hệ thống | — | Dừng leo thang và nhắc; liệt kê để đóng (FR-038) |

- **FR-030**: Khi có phát sinh cùng khóa loại với một cảnh báo đang mở, hệ thống MUST gộp vào cảnh báo đó trong cùng một lần: tăng số lần, thêm bản ghi nguồn, cập nhật thời điểm gần nhất; MUST NOT tạo cảnh báo mới. Nếu mức của phát sinh cao hơn mức cảnh báo, mức cảnh báo MUST nâng lên mức đó và áp tác động "Nâng mức". Hai phát sinh cùng khóa loại đến gần như đồng thời MUST cho kết quả một cảnh báo đang mở. Phát sinh cùng loại khi không có cảnh báo đang mở (cảnh báo cũ Đã đóng hoặc Chuyển sự cố) MUST tạo cảnh báo mới. *(Nguồn: BR-M05-02, DBR-18)*
- **FR-031**: Hạn tiếp nhận theo mức MUST là: Nhẹ — hết ca hiện tại của tầng người cao tuổi; Trung bình — CFG-M05-01 (mặc định \[15 phút\]) từ thời điểm tạo, nâng mức hoặc leo thang; Khẩn cấp — ngay lập tức: mọi người nhận ở FR-035 được thông báo đồng thời, không leo thang theo cấp, và thông báo chưa được xác nhận xử lý theo BR-M13-02 (CFG-M13-01, mặc định \[5 phút\], feature 009). Người phụ trách ban đầu MUST là Điều dưỡng phụ trách người cao tuổi trong ca (nếu không có thì Người phụ trách ca của tầng). Với người cao tuổi đang Tạm vắng, Điều trị tại bệnh viện hoặc Hoạt động bên ngoài, "tầng" là tầng của giường đang giữ; nếu người đó không còn phân bổ giường thì là tầng của phân bổ gần nhất. Hạn tiếp nhận và leo thang của cảnh báo, sự cố đang mở MUST NOT dừng vì người cao tuổi vắng mặt (CHK006, CHK039). Cảnh báo mức Nhẹ được tạo sau mốc lập bàn giao của ca (CFG-M09-04 trước giờ kết ca) MUST có hạn tiếp nhận là hết ca kế tiếp, và vẫn có trong bàn giao (CHK020). Lệnh "Giao người phụ trách" chỉ dành cho Quản lý viện vì Quản lý viện không được xử lý (Q-71); Trưởng tầng, Điều dưỡng, Bác sĩ nhận một cảnh báo từ người khác bằng lệnh "Bắt đầu xử lý" hoặc tiếp nhận trực tiếp theo FR-036, không cần lệnh giao (CHK012). *(Nguồn: 9.3, BR-M05-01, BR-M13-02; người phụ trách ban đầu: xem Điểm cần báo lại 1)*
- **FR-032**: Khi feature nguồn yêu cầu đóng cảnh báo vì nguồn đã được xử lý (công việc hoàn thành đúng khung, liều được đính chính Đã dùng, phiếu đối chiếu được xác nhận, trạng thái cuối…), hệ thống MUST ghi việc xử lý của bản ghi nguồn đó vào lịch sử cảnh báo; nếu mọi bản ghi nguồn của cảnh báo đã được xử lý, cảnh báo MUST chuyển Đã đóng với người đóng là Hệ thống. Đính chính bản ghi chỉ số MUST NOT được coi là nguồn đã được xử lý (FR-019). Nếu cảnh báo đã ở Chuyển sự cố, cảnh báo MUST giữ nguyên trạng thái; hệ thống MUST thêm vào sự cố liên kết một diễn biến "nguồn đã được xử lý: <lý do từ feature nguồn>" (người ghi là Hệ thống) và MUST thông báo người xử lý sự cố; sự cố MUST NOT tự đóng mà được đóng theo bảng FR-045 (BR-M05-09) (Clarification 2026-09-26 lượt 2, đề xuất Q-76). Nếu sự cố liên kết đã ở Đã đóng hoặc Đã hủy, diễn biến đó vẫn được ghi thêm vào sự cố nhưng hệ thống MUST NOT gửi thông báo (CHK055). *(Nguồn: feature 005 FR-010, FR-035; feature 006 FR-029, FR-030, FR-036, FR-047)*
- **FR-033**: Chuỗi leo thang MUST là: cấp 0 — người phụ trách ban đầu (FR-031); cấp 1 — Trưởng tầng của tầng người cao tuổi và Bác sĩ trực (một trong hai tiếp nhận là đủ); cấp 2 — Quản lý viện. Với mức Trung bình, mỗi cấp chờ CFG-M05-01 trước khi lên cấp kế tiếp. Với mức Nhẹ, cảnh báo chưa tiếp nhận khi hết ca MUST được người bàn giao ghi vào bàn giao (BR-M05-05) và lên một cấp. Cấp không có người nhận trong ca MUST được bỏ qua, lên thẳng cấp kế tiếp; vì vậy yêu cầu thông báo leo thang gửi feature 009 MUST khai cách thay "không thay" cho các nhóm người nhận của cấp (feature 009 FR-010, Q-110). Nhân viên thực hiện công việc nguồn (nếu có) MUST được thông báo từ cấp 0 nhưng không là người tiếp nhận. Mỗi lần leo thang MUST ghi lịch sử: thời điểm, cấp cũ, cấp mới, người được thông báo. Khi người phụ trách ban đầu ở cấp 0 cũng chính là Trưởng tầng của tầng đó, thông báo cấp 1 vẫn được gửi cho người đó cùng Bác sĩ trực, để lịch sử leo thang đủ các cấp (CHK027). *(Nguồn: BR-M05-01, UC-38, 18.3 "số lần leo thang"; xem Điểm cần báo lại 1, 2)*
- **FR-034**: Cảnh báo đã ở cấp 2 mà vẫn chưa tiếp nhận MUST NOT leo thang thêm; cảnh báo MUST mang dấu "đã leo thang tối đa" và hiển thị nổi bật trên dashboard của Quản lý viện và Trưởng tầng (18.5) cho tới khi được tiếp nhận. **(Bổ sung, 2026-09-30, Q-248)** Trong thời gian mang dấu, hệ thống MUST yêu cầu feature 009 nhắc lại mức Trung bình cho Quản lý viện và Trưởng tầng của tầng người cao tuổi, mỗi khoảng bằng thời hạn tiếp nhận của mức cảnh báo (CFG-M05-01 với mức Trung bình; hết ca của tầng với mức Nhẹ), và dừng nhắc khi cảnh báo được tiếp nhận hoặc tự đóng. **(Làm rõ, 2026-10-01, Q-266)** Mỗi lần nhắc MUST dùng khóa sự kiện (cảnh báo, "nhắc leo thang tối đa", số thứ tự lần nhắc) theo feature 009 FR-005. Lần nhắc MUST NOT đổi cấp leo thang, MUST NOT ghi vào lịch sử leo thang và MUST NOT tính vào số lần leo thang cung cấp cho feature 016 (FR-081); hệ thống ghi lịch sử lần nhắc riêng. Với mức Nhẹ, lần nhắc gửi khi hết mỗi ca của tầng và không thay việc ghi cảnh báo vào bàn giao (BR-M05-05); người nhận thay khi tầng không có Trưởng tầng theo feature 009 FR-010. *(Suy ra từ BR-M05-01; Q-248)*
- **FR-035**: Người nhận thông báo khi tạo cảnh báo MUST theo mức: Nhẹ — người phụ trách ban đầu; Trung bình — người phụ trách ban đầu và nhân viên thực hiện công việc nguồn; Khẩn cấp — theo FR-047a (người liên hệ chính và Quản lý viện chỉ được báo khi có sự cố Khẩn cấp, FR-047). Người nhận MUST xác định theo phân công hiện tại (BR-M13-04); kênh gửi theo feature 009 (BR-M13-01). *(Nguồn: BR-M13-01, 04)*
- **FR-036**: Tiếp nhận, bắt đầu xử lý, đóng, chuyển sự cố, nâng/hạ mức MUST chỉ do Điều dưỡng, Trưởng tầng (trong phạm vi phân công, feature 002 FR-034) hoặc Bác sĩ (toàn viện) thực hiện; Người phụ trách ca có cùng quyền trong tầng và thời gian ca (4.4 chú thích ⁹, feature 002 FR-019a). Nhân viên chăm sóc chỉ xem cảnh báo của người được phân công; Quản lý viện xem mọi cảnh báo; MUST tiếp nhận được cảnh báo đang ở Leo thang cấp 2 và giao người phụ trách (bảng FR-029), nhưng MUST NOT bắt đầu xử lý, đóng, chuyển sự cố hay nâng/hạ mức. *(Nguồn: 4.4, UC-34, BR-M05-01; Clarification 2026-09-26, đề xuất Q-71)*
- **FR-037**: Mọi cảnh báo và sự cố đang mở khi kết thúc ca MUST hiện trong danh sách người lập bàn giao xem để ghi vào bàn giao; spec này không lập sẵn mục bàn giao (feature 008 BR-M09-06; sửa 2026-10-06, Q-307). Khi ca sau xác nhận bàn giao, người phụ trách của mỗi cảnh báo, sự cố mà người phụ trách thuộc ca trước MUST chuyển sang Điều dưỡng phụ trách người cao tuổi trong ca mới (nếu không có thì Người phụ trách ca mới); trạng thái, cấp leo thang và hạn tiếp nhận giữ nguyên. "Thuộc ca trước" nghĩa là người phụ trách có tên trong ca vừa bàn giao của tầng đó; cảnh báo, sự cố có người phụ trách là Bác sĩ (kể cả Bác sĩ trực) hoặc Quản lý viện MUST NOT bị chuyển theo bàn giao của tầng (CHK016). Trước khi xác nhận, người phụ trách chính thức không đổi; ngoại lệ: từ giờ bắt đầu ca sau, nếu bàn giao chưa Đã xác nhận, cảnh báo và sự cố mà người phụ trách thuộc ca trước và không có tên trong ca nào đang diễn ra MUST được chuyển tạm sang Người phụ trách ca sau (không có thì Trưởng tầng của phạm vi), mang dấu "tạm nhận, chờ xác nhận bàn giao", trạng thái, cấp leo thang và hạn tiếp nhận giữ nguyên; người tạm nhận có đủ quyền FR-036 với các mục đó và thay Điều dưỡng phụ trách ở FR-033 (bậc đầu của chuỗi leo thang) và FR-035 (người nhận thông báo), các bậc sau giữ nguyên (feature 008 Q-94); khi bàn giao được xác nhận, quy tắc chuyển ở trên áp cho cả các mục tạm nhận và dấu được gỡ (feature 008 FR-044a). *(Nguồn: BR-M05-05, BR-M09-08; feature 008 Q-78)*
- **FR-038**: Khi người cao tuổi chuyển trạng thái cuối, hệ thống MUST dừng leo thang và nhắc với mọi cảnh báo, sự cố đang mở của người đó, và MUST cung cấp danh sách này cho điều kiện Kết thúc lưu trú và danh sách việc sau qua đời (feature 004 FR-063 (d), FR-071). Cảnh báo có yêu cầu đóng từ feature nguồn tự đóng theo FR-032; cảnh báo, sự cố còn lại MUST được đóng bởi người có quyền, với kết quả xử lý. Sự cố "đang mở" là sự cố ở Mới, Đang xử lý hoặc Đang theo dõi; sự cố Đã đóng và Đã hủy không còn là sự cố mở đối với điều kiện Kết thúc lưu trú và danh sách việc sau qua đời (CHK061). *(Nguồn: 5.6 "→ Kết thúc lưu trú", 6.8)*
- **FR-038a**: **(Sửa, 2026-10-06, Q-307)** Khi số lần phát sinh mức Trung bình của cùng khóa loại trong 24 giờ trượt (tính cả các lần đã gộp) đạt CFG-M05-02 (mặc định \[3 lần / 24 giờ\]), hệ thống MUST báo người phụ trách và Bác sĩ trực số lần lặp; hệ thống MUST NOT đề xuất nâng mức. Người có quyền FR-036 quyết định có nâng lên Khẩn cấp hay không bằng lệnh Nâng mức (bảng FR-029, bắt buộc lý do). Thông báo được gửi lại ở mỗi lần phát sinh tiếp theo khi điều kiện còn đúng. *(Nguồn: BR-M05-03, CFG-M05-02, Q-307)*

#### E. Quy tắc theo xu hướng

- **FR-039**: Hệ thống MUST có danh mục quy tắc xu hướng, mỗi quy tắc có: trạng thái bật/tắt, tham số (mã CFG), mức cảnh báo tạo ra. Quản lý viện MUST bật/tắt và đổi mức; mọi thay đổi ghi nhật ký kèm giá trị trước/sau và lý do (BR-M15-04). Danh mục mặc định: *(Nguồn: BR-M05-04 "cấu hình được")*

| Quy tắc | Tham số | Mức mặc định | Bên phát hiện |
| --- | --- | --- | --- |
| Sụt cân | CFG-M05-03 (mặc định \[5% trong 30 ngày\]) | Trung bình | Spec này (FR-039a) |
| Bỏ lỡ lần đo theo lịch | — | Nhẹ (Trung bình khi công việc Bắt buộc, FR-039b) | Spec này (FR-039b) |
| Mất ngủ kéo dài | CFG-M05-05 (mặc định \[3\] đêm) | Nhẹ | Spec này, từ kết quả ngủ của feature 005 (FR-039c) |
| Từ chối cùng một thuốc | CFG-M05-04 (mặc định \[2\] lần) | Trung bình | feature 006 FR-032 phát hiện, spec này tạo cảnh báo |

- **FR-039a**: Khi một cân nặng được xác nhận, hệ thống MUST so với cân nặng hiện hành cao nhất trong khoảng ngày của CFG-M05-03 trước thời điểm đo; nếu mức giảm (tính trên giá trị cao nhất đó) đạt tỷ lệ của CFG-M05-03, hệ thống MUST tạo (hoặc gộp) cảnh báo "sụt cân" nêu hai lần đo được so (khi trong khoảng đó không có cân nặng nào trước lần đo, ví dụ lần cân đầu sau tiếp nhận, quy tắc không chạy; CHK042), và MUST gửi sự kiện "sụt cân" cho feature 011 để feature 011 thông báo Bác sĩ xem lại chế độ ăn; không tạo yêu cầu (sửa, 2026-10-06, Q-283; sửa, 2026-10-11, Q-307) (BR-M08-05, feature 011 FR-058). *(Nguồn: BR-M05-04, BR-M08-05; cách chọn mốc so là giả định)*
- **FR-039b**: Mọi tín hiệu bỏ lỡ của một công việc đo sinh từ lịch đo MUST dồn vào một cảnh báo khóa loại "bỏ lỡ lần đo theo lịch: <chỉ số>": (a) yêu cầu tạo cảnh báo công việc Bắt buộc quá hạn từ feature 005 cho công việc có nguồn là lịch đo MUST được ghi với khóa này, không phải khóa "công việc quá hạn"; (b) công việc kết thúc Không thực hiện (không tính đóng do trạng thái cuối), hoặc tới hết ca vẫn Quá hạn, MUST tạo hoặc gộp vào cảnh báo này. Mức của cảnh báo là mức cao nhất trong các nguồn đã gộp (Trung bình với công việc Bắt buộc, mức cấu hình của quy tắc với công việc khác). Công việc Hủy (vắng mặt, lịch bị ngừng) MUST NOT tính. Một lần đo bị bỏ MUST NOT sinh hai cảnh báo khác khóa loại. Yêu cầu đóng "đã thực hiện đúng khung" của feature 005 cho một công việc đo MUST được áp vào cảnh báo khóa này theo bản ghi nguồn (FR-032), vì việc đóng xét theo bản ghi nguồn chứ không theo khóa loại; câu "cảnh báo quá hạn gắn công việc" ở feature 005 FR-044 (c) với công việc đo từ lịch đo chỉ chính cảnh báo này (CHK064). *(Nguồn: BR-M05-04, BR-M04-06; Clarification 2026-09-26 lượt 2, đề xuất Q-75)*
- **FR-039c**: Chất lượng ngủ thuộc nhóm "mất ngủ" trong danh mục chất lượng ngủ của feature 005 được ghi CFG-M05-05 đêm liên tiếp MUST tạo (hoặc gộp) cảnh báo "mất ngủ kéo dài"; đêm không có ghi nhận ngủ làm đứt chuỗi; đêm vắng mặt không tính và không làm đứt chuỗi. Một "đêm" được xác định bằng ngày dương lịch của thời điểm dự kiến của công việc ghi nhận ngủ ở feature 005; mỗi ngày tối đa một đêm, lấy kết quả ngủ hiện hành mới nhất của ngày đó (CHK045). *(Nguồn: BR-M05-04, feature 005 FR-034 dòng "Ngủ")*

#### F. Sự cố

- **FR-040**: Mỗi sự cố MUST gồm: người cao tuổi (MAY trống chỉ với loại "cơ sở vật chất"), loại (danh mục loại sự cố), nguồn phát sinh (chăm sóc, ăn uống, thuốc, vận động, hoạt động, đi lại, điều trị, hoạt động ngoài viện, phục vụ sai suất ăn, đồ gửi), thời điểm xảy ra (nếu biết), thời điểm phát hiện, địa điểm, hoạt động đang diễn ra, người phát hiện, mô tả, mức độ, xử lý ban đầu, người được thông báo (hệ thống điền từ nhật ký thông báo của feature 009), cảnh báo nguồn (nếu chuyển từ cảnh báo). Sự cố tạo từ cảnh báo MUST lấy mức mặc định bằng mức cảnh báo. **(Sửa, 2026-10-06, Q-307)** Hệ thống MUST NOT tự tạo sự cố; mọi sự cố do nhân viên ghi nhận. Khi phục vụ suất có thành phần gây dị ứng mà người cao tuổi đã ăn (feature 011 chỉ chặn và yêu cầu cảnh báo, BR-M08-14), nhân viên ghi sự cố loại "phục vụ sai suất ăn", nguồn "ăn uống", mức theo triệu chứng (FR-042). Khi đồ gửi chuyển Thất lạc hoặc Hư hỏng (feature 013 chỉ thông báo, BR-M12-02), người ghi lệnh đồ gửi quyết định ghi sự cố loại "đồ gửi thất lạc" hoặc "đồ gửi hư hỏng", nguồn "đồ gửi", mức mặc định Trung bình, có tham chiếu đồ gửi và bản ghi bàn giao; địa điểm là nơi xảy ra. Sự cố loại "cơ sở vật chất" MUST có địa điểm chọn từ cơ cấu tầng/khu vực của feature 003; thông báo theo FR-043 (a) và việc tiếp nhận dùng Người phụ trách ca và Trưởng tầng của tầng/khu vực đó thay cho "điều dưỡng phụ trách" (CHK004). *(Nguồn: 9.1, 9.2, SU_CO; BR-M08-14, đồng bộ spec 011; BR-M12-02, đồng bộ spec 013)*
- **FR-041**: Mọi tài khoản nhân viên có quyền "T" ở dòng "Sự cố, khẩn cấp" MUST ghi nhận được sự cố và kích hoạt khẩn cấp cho bất kỳ người cao tuổi nào chưa ở trạng thái cuối, theo ngoại lệ phạm vi của feature 002 FR-044a (chỉ thấy thông tin nhận dạng với người ngoài phạm vi; nhật ký đánh dấu "ngoài phạm vi"). Quản lý viện MUST ghi nhận được sự cố và kích hoạt khẩn cấp như nhân viên khác (4.1, AC-00), nhưng MUST NOT tiếp nhận xử lý, ghi diễn biến chuyên môn, đổi mức hay đóng sự cố (Clarification 2026-09-26, đề xuất Q-71); Quản lý viện MAY giao người xử lý theo FR-045a (bổ sung, 2026-10-11, Q-339). Nếu người cao tuổi đã có sự cố Khẩn cấp đang mở, hệ thống MUST hiển thị sự cố đó và cho người ghi chọn tham gia xử lý sự cố đó hoặc tạo sự cố mới; MUST NOT tự gộp sự cố. "Tham gia xử lý" nghĩa là: không tạo sự cố mới; người đó được ghi vào danh sách người xử lý của sự cố (FR-048b); ghi được diễn biến theo dòng "Ghi diễn biến" của bảng FR-045; và chỉ thấy thẻ thông tin khẩn cấp khi có quyền xem sức khỏe của người cao tuổi đó (FR-048) (CHK010). *(Nguồn: 4.4 dòng "Sự cố, khẩn cấp" chú thích ¹⁰, UC-35, UC-36, BR-M15-01; xem Điểm cần báo lại 2)*
- **FR-042**: Danh mục loại sự cố MUST có mức mặc định cho từng loại (ví dụ Ngã — Khẩn cấp, theo ví dụ 9.3); người ghi chọn mức thấp hơn mức mặc định MUST ghi lý do. Với loại Ngã, tác động của FR-051 MUST áp dụng bất kể mức được chọn (Clarification 2026-09-26, đề xuất Q-70). Danh mục MUST gồm ít nhất: ngã, lây nhiễm nghi ngờ, thuốc, ăn uống, phục vụ sai suất ăn, khó thở / mất ý thức, đi lạc hoặc không trở về (mặc định Khẩn cấp, đồng bộ spec 014), hành vi, chấn thương khác, cơ sở vật chất, đồ gửi thất lạc (mặc định Trung bình), đồ gửi hư hỏng (mặc định Trung bình), khác. Mỗi loại MUST có dấu "không thuộc sức khỏe" do Quản lý viện đặt, mặc định bật cho cơ sở vật chất, đồ gửi thất lạc, đồ gửi hư hỏng; mỗi lần đổi dấu MUST ghi nhật ký với giá trị trước/sau. Dấu chỉ dùng để quyết định Hành chính có thấy số liệu của loại đó trên báo cáo, dashboard hay không (feature 016 FR-042a). *(Nguồn: 9.1, 9.2, 9.3; BR-M12-02, đồng bộ spec 013; Q-202, đồng bộ spec 016)*
- **FR-043**: Khi sự cố được tạo, trong cùng lần ghi hệ thống MUST: (a) gửi thông báo theo mức — Nhẹ: điều dưỡng phụ trách; Trung bình: điều dưỡng phụ trách và Người phụ trách ca; Khẩn cấp: theo FR-047 — kênh theo BR-M13-01; (b) với mức Trung bình trở lên hoặc loại Ngã, báo bác sĩ cần đánh giá lại, căn cứ "sau sự cố" (feature 001 FR-040, BR-M01-02; không tạo yêu cầu, Q-307), trừ sự cố loại "đồ gửi thất lạc", "đồ gửi hư hỏng" ở mọi mức (đồng bộ spec 013 FR-022); (c) với loại Ngã, áp FR-051; (d) với loại lây nhiễm nghi ngờ, áp mục I; (e) với mức Khẩn cấp, kích hoạt quy trình khẩn cấp (mục G). *(Nguồn: 9.3, BR-M01-02, BR-M05-06, 07, 10)*
- **FR-044**: Sự cố MUST có hạn tiếp nhận và chuỗi leo thang như cảnh báo cùng mức (FR-031, FR-033); lệnh "Tiếp nhận xử lý" dừng leo thang. Cảnh báo có trạng thái riêng "Leo thang" (theo sơ đồ 9.4), còn sự cố quá hạn tiếp nhận giữ "Mới" và chỉ tăng cấp leo thang, vì 9.4 không có trạng thái leo thang cho sự cố; khác biệt này có chủ ý, và báo cáo đếm số lần leo thang của cả hai (FR-081) (CHK022). Khi sự cố hạ mức khỏi Khẩn cấp (bảng FR-045): hệ thống MUST yêu cầu feature 009 dừng nhắc gọi điện chưa có kết quả của thông báo khẩn cấp đó (BR-M13-02, BR-M05-22); sự cố được xếp theo mức hiện hành trên dashboard; yêu cầu đối chiếu nguyện vọng (FR-048a) và việc bảo vệ theo FR-046 vẫn giữ nguyên (CHK003). *(Nguồn: 9.3 cột "Thời hạn tiếp nhận")*
- **FR-045**: Vòng đời xử lý sự cố MUST theo bảng dưới (các bước 9.4: Ghi nhận → Đánh giá → Xử lý → Thông báo → Theo dõi → Đóng); không có chuyển nào khác. *(Nguồn: 9.4, BR-M05-09, UC-35, constitution IV)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái mới | Người thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Ghi nhận sự cố | Mới | Nhân viên theo FR-041 | FR-040 | FR-043 |
| — | Chuyển sự cố từ cảnh báo / Kích hoạt khẩn cấp từ cảnh báo | Đang xử lý | Điều dưỡng, Trưởng tầng, Bác sĩ | FR-029 | Người xử lý là người thực hiện; FR-043 |
| Mới | Tiếp nhận xử lý | Đang xử lý | Điều dưỡng, Trưởng tầng, Bác sĩ (như FR-036) | — | Dừng leo thang; MAY gọi từ "Xác nhận và tiếp nhận" trên thông báo Khẩn cấp (feature 009 FR-026a, Q-102) |
| Mới | Giao người xử lý *(bổ sung, 2026-10-11, Q-339)* | Đang xử lý | Trưởng tầng, Bác sĩ (trong phạm vi); Quản lý viện | FR-045a | Người xử lý là người được giao; dừng leo thang; FR-045a |
| Đang xử lý, Đang theo dõi | Giao người xử lý khác *(bổ sung, 2026-10-11, Q-339)* | Giữ nguyên | Người xử lý hiện tại; Trưởng tầng, Bác sĩ (trong phạm vi); Quản lý viện | FR-045a | Đổi người xử lý; FR-045a |
| Mới | Quá hạn tiếp nhận | Mới (cấp leo thang tăng) | Bộ lập lịch | FR-044 | Ghi lịch sử |
| Mới, Đang xử lý, Đang theo dõi | Ghi diễn biến (đánh giá, hành động, thông báo, theo dõi) | Giữ nguyên | Người đã ghi nhận sự cố; nhân viên có quyền "T" trong phạm vi | — | Bản ghi thêm, có thời điểm và người ghi |
| Mới, Đang xử lý, Đang theo dõi | Nâng mức / Hạ mức | Giữ nguyên | Điều dưỡng, Trưởng tầng, Bác sĩ; hạ từ Khẩn cấp: chỉ Bác sĩ | Bắt buộc lý do | Nâng lên Khẩn cấp: mục G; nâng lên Trung bình: FR-043 (b); thông báo đã gửi không thu hồi |
| Đang xử lý | Chuyển theo dõi | Đang theo dõi | Điều dưỡng, Trưởng tầng, Bác sĩ | Có ít nhất một diễn biến đánh giá và một diễn biến xử lý | — |
| Đang theo dõi | Mở lại xử lý | Đang xử lý | Điều dưỡng, Trưởng tầng, Bác sĩ | Bắt buộc lý do | — |
| Mới, Đang xử lý | Chuyển viện | Giữ nguyên | Điều dưỡng, Bác sĩ | FR-049 | FR-049 |
| Đang xử lý, Đang theo dõi | Đóng sự cố | Đã đóng | Nhẹ: Điều dưỡng, Trưởng tầng, Bác sĩ; Trung bình, Khẩn cấp: Điều dưỡng hoặc Bác sĩ | Bắt buộc kết quả xử lý (BR-M05-09); sự cố lây nhiễm: FR-066 | Dừng nhắc; gỡ khỏi danh sách đang mở |
| Mới, Đang xử lý, Đang theo dõi, Đã đóng | Đính chính "Hủy ghi nhận" | Đã hủy | Người được đính chính theo FR-046 | Bắt buộc lý do | FR-046a |
| Mới, Đang xử lý, Đang theo dõi, Đã đóng | Đính chính đổi loại sự cố | Giữ nguyên | Người được đính chính theo FR-046 | Bắt buộc lý do | FR-046a |

- **FR-045a**: **(Bổ sung, 2026-10-11, Q-339)** Lệnh "Giao người xử lý" MUST chọn người được giao là Điều dưỡng, Trưởng tầng hoặc Bác sĩ có quyền xử lý sự cố với người cao tuổi đó (phạm vi theo feature 002; sự cố "cơ sở vật chất" không gắn người cao tuổi theo tầng/khu vực của địa điểm) và MUST có ghi chú; người được giao khác người xử lý hiện tại. Hệ thống MUST ghi lịch sử phân công (người giao, người được giao, thời điểm, ghi chú) như bản ghi nhóm 3 và MUST yêu cầu feature 009 báo người được giao theo mức hiện hành của sự cố (kênh theo BR-M13-01). Giao từ Mới dừng leo thang như "Tiếp nhận xử lý" (FR-044). Lệnh không đổi mức, không ghi diễn biến chuyên môn. *(Nguồn: 9.4, Q-339, Q-71)*
- **FR-046**: Đã đóng là trạng thái cuối (trừ đính chính ở bảng FR-045). Nội dung ghi nhận và diễn biến của sự cố MUST NOT sửa hay xóa; đính chính theo feature 000 (người ghi gốc, Trưởng tầng hoặc Người phụ trách ca của tầng). Sự cố Khẩn cấp (kể cả sự cố từng ở mức Khẩn cấp) thuộc danh sách bảo vệ; mọi đính chính MUST có lý do và được ghi nhật ký. Sự cố đã đóng MUST vẫn nhận được bản đính chính. Với sự cố đang hoặc từng ở mức Khẩn cấp, đính chính "Hủy ghi nhận" và đính chính đổi loại MUST chỉ do Điều dưỡng (trong phạm vi) hoặc Bác sĩ thực hiện; người ghi gốc không có quyền xử lý sự cố (ví dụ nhân viên ngoài phạm vi) đề nghị bằng một diễn biến, không tự hủy được. Đây là thu hẹp cho phép theo feature 000 FR-026 (CHK053). *(Nguồn: BR-M05-08, BR-M15-03, DBR-19, DBR-23)*
- **FR-046a**: Khi sự cố bị "Hủy ghi nhận" hoặc đổi loại, hệ thống MUST NOT tự thu hồi tác động đã tạo (khoảng theo dõi sau ngã, danh sách tiếp xúc, dấu nghi nhiễm/tiếp xúc, vùng khoanh vùng); thông báo đã gửi (cần đánh giá lại, xem xét kế hoạch) không thu hồi; hệ thống MUST gửi cho Điều dưỡng phụ trách và các Bác sĩ danh sách từng tác động còn hiệu lực kèm lệnh tương ứng (kết thúc theo dõi sau ngã, loại người khỏi danh sách tiếp xúc, gỡ khoanh vùng) để người có quyền tự quyết định. Đổi loại **sang** Ngã MUST tạo ngay các tác động của FR-051 tại thời điểm đính chính; đổi loại sang "lây nhiễm nghi ngờ" MUST áp mục I từ thời điểm đính chính; đổi loại **khỏi** Ngã hoặc lây nhiễm xử lý như "Hủy ghi nhận" với các tác động của loại cũ. Thông báo đã gửi MUST NOT bị thu hồi. Đã hủy là trạng thái cuối; sự cố Đã hủy vẫn xem được và không tính vào báo cáo. Khi đổi loại sang "lây nhiễm nghi ngờ", khoảng truy vết CFG-M05-07 của FR-060 MUST tính lùi từ thời điểm phát hiện ghi trong sự cố, không từ thời điểm đính chính (CHK059). Sự cố Đã hủy MUST NOT có trong bản tin kỳ sau; nếu sự cố đã được nêu trong một bản tin đã gửi, hệ thống MUST cung cấp cho feature 012 sự cố bị hủy và bản tin liên quan để Hành chính báo lại người đã nhận (CHK056). *(Nguồn: 1.5 "Hủy ghi nhận", BR-M05-07, BR-M05-10; Clarification 2026-09-26 lượt 2, đề xuất Q-73)*
- **FR-046b**: **(Sửa, 2026-10-10, Q-307)** Khi feature 013 báo bản ghi bàn giao làm căn cứ cho sự cố loại "đồ gửi thất lạc", "đồ gửi hư hỏng" bị "Hủy ghi nhận" (feature 013 FR-011a), hệ thống MUST báo người xử lý sự cố kèm tham chiếu bản đính chính ở feature 013; sự cố MUST NOT tự chuyển Đã hủy, người xử lý xử lý bằng lệnh theo FR-045, FR-046. Thông báo đã gửi không bị thu hồi (FR-046a); feature 013 báo lại người đã nhận. Lệnh "Đóng sự cố" với hai loại này theo FR-045; diễn biến "đã tìm thấy" do feature 013 ghi không tự đóng sự cố. *(Nguồn: đồng bộ spec 013, Q-160)*

#### G. Quy trình khẩn cấp và chuyển viện

- **FR-047**: Quy trình khẩn cấp MUST được kích hoạt khi một sự cố ở mức Khẩn cấp được tạo (từ bất kỳ nguồn nào) hoặc được nâng lên Khẩn cấp. Khi kích hoạt, trong cùng lúc hệ thống MUST: (a) phát thông báo khẩn cấp song song (không tuần tự) cho Bác sĩ trực, Trưởng tầng của tầng người cao tuổi, Quản lý viện, người liên hệ chính (BR-M05-06), điều dưỡng phụ trách và Người phụ trách ca; (b) hiển thị thẻ thông tin khẩn cấp (FR-048); (c) lưu bản chụp nội dung thẻ tại thời điểm kích hoạt vào sự cố. Thông báo cho người liên hệ chính MUST chỉ gồm "có tình huống khẩn cấp liên quan [họ tên người cao tuổi], đề nghị liên hệ viện", thời điểm và số liên hệ của viện, trừ khi người đó nằm trong bản đồng ý chia sẻ dữ liệu đang hiệu lực — tức thuộc danh sách người thân được xem thông tin sức khỏe của bản đồng ý (feature 001 FR-010), không phải mục "đồng ý nhận thông báo" (CHK060) — (khi đó MAY kèm loại sự cố và mô tả ngắn); MUST NOT chứa chẩn đoán, chỉ số, thuốc hay nguyện vọng cuối đời. Nếu tại thời điểm kích hoạt không có Bác sĩ trực, hệ thống MUST áp FR-047b. Kích hoạt khẩn cấp MUST thực hiện trực tuyến, MUST NOT lưu tạm trên thiết bị (8.6, Q-01). *(Nguồn: 9.5, BR-M05-06, BR-M05-13, BR-M13-01, NFR-03, DBR-03; Clarification 2026-09-26 lượt 2, đề xuất Q-72)* **(Bổ sung 2026-10-18, Q-354)** Thông báo cho người liên hệ chính ngoài phạm vi bản đồng ý là trường hợp "khẩn cấp tới người liên hệ chính" của Q-354 và MUST mang mã trường hợp đó (FR-047c).
- **FR-047a**: Cảnh báo mức Khẩn cấp (vượt ngưỡng nguy hiểm, nâng mức, gộp) MUST NOT tự tạo sự cố hay kích hoạt quy trình khẩn cấp. Khi cảnh báo được tạo hoặc nâng lên Khẩn cấp, hệ thống MUST thông báo đồng thời (mọi kênh, song song, BR-M13-01) cho Điều dưỡng phụ trách, Người phụ trách ca, Bác sĩ trực và Trưởng tầng của tầng người cao tuổi; MUST NOT thông báo người liên hệ chính hay Quản lý viện ở bước này. Nếu không có Bác sĩ trực, hệ thống MUST áp FR-047b. Quy trình khẩn cấp (FR-047) chỉ bắt đầu khi người có quyền chọn "Kích hoạt khẩn cấp từ cảnh báo" (bảng FR-029). *(Nguồn: 9.1, 9.3, BR-M06-03, BR-M05-06; Clarification 2026-09-26, đề xuất Q-67)*
- **FR-047b**: Khi thông báo Khẩn cấp (sự cố theo FR-047 hoặc cảnh báo theo FR-047a) cần gửi Bác sĩ trực mà trong ca hiện tại không có bác sĩ nào giữ nhiệm vụ Bác sĩ trực (2.4), hệ thống MUST gửi song song cho mọi Bác sĩ đang hoạt động của cơ sở (mọi tài khoản ở trạng thái Hoạt động có vai trò Bác sĩ; không xét ca hay hiệu lực giấy phép, vì đây là thông báo chứ không phải nghiệp vụ chuyên môn; CHK058), MUST gắn dấu "không có Bác sĩ trực tại thời điểm" vào sự cố hoặc cảnh báo, và MUST báo Quản lý viện (kể cả với cảnh báo, như ngoại lệ của FR-047a). Quy tắc này không áp cho leo thang cấp 1 của cảnh báo Trung bình (FR-033 bỏ qua người nhận vắng). Một thời điểm có thể có nhiều Bác sĩ trực (feature 008 FR-023): khi đó mọi Bác sĩ trực đều nhận thông báo dành cho "Bác sĩ trực" ở FR-033, FR-047, FR-047a, một người tiếp nhận là đủ; "không có Bác sĩ trực" nghĩa là không có ai mang dấu Bác sĩ trực tại thời điểm đó (làm rõ 2026-10-01, spec 008 checklist consistency CHK016). *(Nguồn: BR-M05-06, 2.4 "Bác sĩ trực"; Clarification 2026-09-26 lượt 2, đề xuất Q-74)*
- **FR-047c**: **(Bổ sung 2026-10-18, Q-354)** Mỗi thông báo hoặc yêu cầu gọi điện tới người thân do spec này phát sinh mà nội dung vượt phạm vi bản đồng ý của người nhận (feature 001 FR-010) MUST thuộc một trường hợp của Q-354 và MUST mang mã trường hợp để feature 009 lưu cùng bản ghi gửi (BR-M10-06): "khẩn cấp tới người liên hệ chính" (FR-047, nội dung tối thiểu); "danh sách tiếp xúc" (FR-062, không nêu danh tính hay tình trạng người nghi nhiễm); "xác nhận lại nguyện vọng khi nguy kịch" (FR-050b, FR-050c); "chuyển viện" (FR-049). Spec này MUST NOT gửi người thân nội dung vượt phạm vi đồng ý ngoài các trường hợp trên. Căn cứ: Luật Bảo vệ dữ liệu cá nhân Điều 19 khoản 1 điểm a. *(Nguồn: 9.5, Q-354)*
- **FR-047d** *(bổ sung 2026-10-18, Q-369)*: Khi người cao tuổi chạm nút SOS trên thiết bị đã gắn (feature 002 FR-071) mà không bấm Hủy trong 3 giây đếm ngược, hoặc chạm lần nữa trong lúc đếm (Q-372), hệ thống MUST tạo cảnh báo mức Khẩn cấp loại "người cao tuổi yêu cầu trợ giúp", không cần phiên đăng nhập. Lệnh MUST bị từ chối khi: người cao tuổi ở Tạm vắng hoặc Điều trị tại bệnh viện; người bán trú không ở trạng thái có mặt Có mặt trong ngày (feature 005); thiết bị đã gỡ hoặc tài khoản Đã khóa. Khi bị từ chối hoặc mất kết nối, ứng dụng hiện số liên hệ của viện (BR-M05-24) và số cấp cứu 115 (feature 012 FR-085). *(Nguồn: BR-M05-27, 9.1, UC-98)*
- **FR-047e** *(bổ sung 2026-10-18, Q-369)*: Cảnh báo từ nút SOS MUST báo người nhận như FR-047a; khi người cao tuổi ở Hoạt động bên ngoài, MUST báo thêm trưởng đoàn và người đi cùng của chuyến (feature 014). MUST NOT báo người liên hệ chính hay Quản lý viện ở bước này, MUST NOT tự tạo sự cố; cảnh báo được xử lý theo FR-029 (đóng kèm kết quả, chuyển sự cố hoặc kích hoạt khẩn cấp từ cảnh báo). *(Nguồn: 9.4 "Nút SOS", Q-67)*
- **FR-047f** *(bổ sung 2026-10-18, Q-369)*: Sau khi gửi, người cao tuổi MAY chọn nhanh nơi đang ở (phòng mình, nhà vệ sinh, hành lang, phòng sinh hoạt chung, sân, khác; sửa 2026-10-22, Q-373); lựa chọn được ghi thêm vào cảnh báo, không bắt buộc và không làm chậm việc gửi. Spec MUST cung cấp cho kênh người cao tuổi trạng thái "Đã gửi" và họ tên người đã tiếp nhận. *(Nguồn: 9.4 "Nút SOS")*
- **FR-047g** *(bổ sung 2026-10-18, Q-369)*: MUST NOT có lệnh tắt hay hạn chế nút SOS của một người cao tuổi. Người cao tuổi bấm lặp lại nhiều lần thì Điều dưỡng ghi lưu ý chăm sóc (feature 001), không chặn nút. *(Nguồn: BR-M05-27)*
- **FR-047h** *(bổ sung 2026-10-22, Q-373)*: Lệnh SOS và lệnh nút hỗ trợ khẩn cấp MUST được gửi và xử lý mà không chờ vị trí. Khi ứng dụng có quyền vị trí (chỉ xin khi đang dùng ứng dụng; MUST NOT theo dõi nền) và bắt được GPS, hệ thống MUST nhận thêm vị trí (vĩ độ, kinh độ, sai số theo mét, thời điểm đo) gắn vào cảnh báo; trong khi cảnh báo đang mở mà chưa ghi đã tới nơi, ứng dụng gửi lại mỗi CFG-M05-09 (mặc định \[30 giây\]) và hệ thống MUST lưu từng lần. Người nhận MUST thấy tọa độ mới nhất kèm sai số, thời điểm đo; tọa độ quá hai chu kỳ CFG-M05-09 không cập nhật MUST mang dấu "có thể đã cũ". Khi người cao tuổi ở Hoạt động bên ngoài, tọa độ là vị trí chính, hiển thị trên bản đồ có nút chỉ đường (mở ứng dụng bản đồ của máy); ở viện, tọa độ mang nhãn "tham khảo" và phòng, giường, nơi tự chọn là vị trí chính. Vị trí MUST NOT đổi người nhận, MUST NOT hiện cho người thân, và được lưu, loại bỏ cùng cảnh báo. *(Nguồn: BR-M05-28, CFG-M05-09)*
- **FR-047i** *(bổ sung 2026-10-22, Q-375)*: Nhân viên có tài khoản đang trong ca (vai trò không xếp ca: trong giờ hành chính CFG-M13-06) MUST bấm được nút hỗ trợ khẩn cấp trên app nhân viên, kích hoạt như FR-047d (đếm ngược 3 giây, Hủy, chạm lần hai gửi ngay); người không trong ca bị từ chối và app hiện số liên hệ của viện, số 115. Hệ thống MUST tạo cảnh báo Khẩn cấp loại "nhân viên yêu cầu hỗ trợ", nguồn nhân viên, không cần người cao tuổi; MUST NOT tự tạo sự cố (Q-67). Sau khi gửi, người bấm MAY chọn loại sự việc (Ngừng tim hoặc ngất, Ngã, Kích động hoặc bạo lực, Khác), tầng, nơi (phòng, nhà vệ sinh, hành lang, phòng sinh hoạt chung, sân, khác), người cao tuổi; vị trí theo FR-047h. Người nhận MUST là: Bác sĩ trực (không có thì FR-047b); Người phụ trách ca và nhân viên có ca đang diễn ra của tầng người bấm (tầng theo phân công ca của feature 008; người không có tầng thì tầng đã chọn); chưa xác định được tầng thì Người phụ trách ca của mọi tầng đang có ca (feature 009 FR-007 (a)); khi người cao tuổi được chọn, thêm người nhận FR-047a của người đó. MUST NOT báo người liên hệ chính. Người phụ trách ban đầu (FR-031) là Người phụ trách ca của tầng (chưa xác định tầng: người tiếp nhận đầu tiên). Chuyển sự cố hoặc kích hoạt khẩn cấp MUST bắt buộc chọn người cao tuổi; sự việc không liên quan người cao tuổi thì đóng kèm kết quả. Cháy không thuộc nút này. *(Nguồn: BR-M05-29, 9.2, UC-99)*
- **FR-047j** *(bổ sung 2026-10-22, Q-374)*: Với cảnh báo từ nút SOS hoặc nút hỗ trợ khẩn cấp, người phụ trách MUST ghi được "Đã tới nơi" một lần, thời điểm do hệ thống ghi, trạng thái giữ nguyên (bảng FR-029). Kênh người cao tuổi và app của người bấm MUST hiện "[họ tên] đang tới" sau tiếp nhận và "[họ tên] đã tới" sau mốc này. Cảnh báo đóng mà chưa ghi MUST mang dấu "không ghi mốc tới nơi". *(Nguồn: BR-M05-30)*
- **FR-047k** *(bổ sung 2026-10-22, Q-376)*: Màn cảnh báo và màn sự cố mức Khẩn cấp MUST có nút "Gọi 115" cho người có quyền xử lý (FR-036, dòng "Sự cố, khẩn cấp" của FR-080). Bấm thì app mở trình quay số với 115; hệ thống MUST ghi lượt gọi (người bấm, thời điểm bấm) và kết quả "Đã gọi được / Không gọi được" do người bấm chọn. Lượt "Đã gọi được" MUST là thời điểm gọi cấp cứu của FR-048b và được thêm làm diễn biến của sự cố liên kết nếu có. Màn MUST hiện thông tin để đọc cho tổng đài: tên, địa chỉ, điện thoại của viện (CFG-M11-11); tầng, phòng hoặc nơi đã chọn; họ tên, tuổi, giới tính người cao tuổi; dị ứng, bệnh nền chỉ cho người có quyền xem sức khỏe. Gọi bằng máy khác thì người xử lý ghi thời điểm gọi cấp cứu bằng tay (FR-048b). Hệ thống MUST NOT kết nối tổng đài. *(Nguồn: BR-M05-31, 9.5)*
- **FR-048**: Thẻ thông tin khẩn cấp MUST gồm: họ tên, ảnh, ngày sinh, phòng/giường; nguyện vọng cuối đời theo phiên bản Hiệu lực của feature 001 FR-023a (lựa chọn khi nguy kịch: chuyển bệnh viện điều trị tích cực / đưa về nhà / ở lại viện chăm sóc giảm nhẹ; người cần liên hệ trước; ghi chú; người ký, thời điểm), hoặc "chưa ghi nhận nguyện vọng cuối đời"; dấu nguy kịch nếu đang có (FR-050a); dị ứng Hiệu lực kèm mức độ; thuốc đang dùng (feature 006 FR-050); bệnh nền Hiệu lực; phần lưu ý chăm sóc của lần đánh giá Đã xác nhận gần nhất (feature 001 FR-038, BR-M01-10; sửa, 2026-10-06, Q-281: thay cờ nguy cơ); người liên hệ chính và số điện thoại. Thẻ MUST hiển thị cho người kích hoạt và người xử lý có quyền xem thông tin sức khỏe của người cao tuổi đó (theo feature 002); người kích hoạt ngoài phạm vi MUST chỉ thấy thông tin nhận dạng và thông điệp "đã thông báo người xử lý" (feature 002 FR-044a). *(Nguồn: 9.5, BR-M05-13, BR-M01-10, feature 001 FR-023, FR-038; xem Điểm cần báo lại 6)*
- **FR-048a**: Nếu người cao tuổi có nguyện vọng cuối đời đã ghi nhận, hệ thống MUST chặn ghi diễn biến loại "biện pháp hồi sức" cho tới khi sự cố có xác nhận "đã đối chiếu nguyện vọng" (người xác nhận, thời điểm) của người đã được hiển thị thẻ; lúc xác nhận, hệ thống MUST hiển thị lại nội dung nguyện vọng. Không có nguyện vọng đã ghi nhận thì MUST NOT yêu cầu xác nhận. "Biện pháp hồi sức" là các loại diễn biến mang dấu "biện pháp hồi sức" trong danh mục loại diễn biến do Quản lý viện cấu hình (nhóm 1); giá trị khởi tạo đề xuất: ép tim ngoài lồng ngực, hỗ trợ thông khí, sốc điện, đặt nội khí quản, dùng thuốc cấp cứu. Danh sách khởi tạo cần Bác sĩ của cơ sở đối chiếu (CHK021). *(Nguồn: BR-M05-13, DBR-19)*
- **FR-048b**: Hồ sơ sự cố Khẩn cấp MUST cho ghi được và hiển thị được: thời điểm phát hiện, người xử lý (mọi người đã ghi diễn biến hoặc tham gia), hành động đã thực hiện (theo thời gian), thời điểm gọi hỗ trợ và thời điểm gọi cấp cứu (từ lượt "Gọi 115" Đã gọi được, FR-047k, hoặc ghi tay), kết quả, người được thông báo kèm thời điểm gửi và thời điểm đã xem (từ feature 009). *(Nguồn: 9.5)*
- **FR-049**: Điều dưỡng hoặc Bác sĩ MUST thực hiện được "Chuyển viện" từ một sự cố Mới hoặc Đang xử lý, kèm cơ sở tiếp nhận và thời điểm; **(bổ sung 2026-10-22, Q-383, Q-384)** kèm người đi kèm, và MAY đính kèm ảnh hoặc bản scan giấy tờ của bệnh viện. Màn lệnh MUST hiện phiếu nguyện vọng khi điều trị Hiệu lực (feature 001 FR-023j); người thực hiện tự chọn cơ sở tiếp nhận, hệ thống MUST NOT tự chọn hay gợi ý (Q-306). Phiếu ghi "cần hỏi trước" mà người thực hiện chuyển khi chưa liên lạc được người cần báo thì lệnh MUST bắt buộc lý do; cấp cứu không cần lý do này. Trong cùng một lệnh hệ thống MUST: thực hiện lệnh Chuyển viện của feature 001/004 với căn cứ là sự cố này (điều kiện 5.6); và tạo bản tóm tắt chuyển viện (FR-050). Nếu lệnh Chuyển viện bị từ chối (ví dụ trạng thái hiện tại không cho phép), không phần nào được áp dụng và sự cố ghi lại lần thử kèm lý do bị từ chối. *(Nguồn: BR-M05-14, 5.6, UC-16, feature 000)*
- **FR-050**: Bản tóm tắt chuyển viện MUST gồm: thông tin nhận dạng, dị ứng Hiệu lực, thuốc đang dùng (feature 006 FR-050), giá trị hiện hành gần nhất của từng chỉ số kèm thời điểm đo, diễn biến sự cố tới thời điểm chuyển, nguyện vọng cuối đời (nếu có), **nguyện vọng khi điều trị (nếu có; bổ sung 2026-10-22, Q-384)**, người liên hệ chính, cơ sở tiếp nhận, người lập, người đi kèm, thời điểm. Bản tóm tắt MUST in được và xem được bởi Bác sĩ, Điều dưỡng, Trưởng tầng trong phạm vi và Quản lý viện. *(Nguồn: BR-M05-14; nguyện vọng cuối đời là bổ sung, xem Điểm cần báo lại 8)*
- **FR-050h** *(bổ sung 2026-10-22, Q-383)*: Trong cùng lệnh Chuyển viện, hệ thống MUST dựng **phiếu tóm tắt chuyển viện** dạng PDF từ bản tóm tắt (FR-050), có thông tin viện (CFG-M11-11), ô ký tay của người lập và dòng "Phiếu thông tin bàn giao, không thay giấy chuyển cơ sở khám bệnh, chữa bệnh". File MUST được lưu cùng lệnh, MUST NOT sửa hay xóa; tải lại trả đúng file đã lưu; in lại mang dấu "Bản in lại"; mỗi lần in MUST ghi nhật ký (19.4). Người in là người xem được bản tóm tắt (FR-050). Hệ thống MUST NOT lập giấy chuyển cơ sở khám bệnh, chữa bệnh theo mẫu Bộ Y tế. *(Nguồn: BR-M05-32, Q-383)*

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

- **FR-050b**: Khi dấu nguy kịch (kể cả dấu tạm) được ghi, trong cùng một lần hệ thống MUST: (a) tạo cảnh báo mức Khẩn cấp loại "nguy kịch – thực hiện nguyện vọng cuối đời", không gộp (FR-028), hiển thị phiên bản nguyện vọng Hiệu lực (feature 001 FR-023a) hoặc dấu "chưa có nguyện vọng"; (b) thông báo đồng thời Bác sĩ trực (không có thì theo FR-047b), Điều dưỡng phụ trách, Trưởng tầng, Quản lý viện; (c) thông báo người liên hệ chính và mọi người đại diện ở mức Khẩn cấp, nội dung tối thiểu như FR-047 với người không thuộc bản đồng ý (Q-72), MUST NOT chứa nội dung nguyện vọng; (d) báo Bác sĩ trực và Điều dưỡng phụ trách gọi **xác nhận lại nguyện vọng**, bắt đầu từ người đại diện, theo thứ tự gọi của BR-M13-02; hệ thống MUST NOT tạo yêu cầu gọi điện; người gọi ghi kết quả theo FR-050c, và việc nhắc khi chưa có kết quả theo feature 009 (**sửa, 2026-10-06, Q-307**). Cảnh báo này không tự kích hoạt quy trình khẩn cấp (FR-047a); nếu có sự cố khẩn cấp cùng lúc thì FR-047, FR-048a vẫn áp dụng. *(Nguồn: BR-M05-15, BR-M05-16, BR-M13-02, 9.5)*
- **FR-050c**: Bác sĩ hoặc Điều dưỡng MUST ghi được **kết quả xác nhận lại**: "giữ nguyện vọng", "nguyện vọng mới" (hệ thống yêu cầu feature 001 ghi phiên bản mới theo FR-023b, người trả lời và người ký theo FR-023c), hoặc "không liên lạc được". Mỗi kết quả ghi: người được liên hệ, kênh (gọi điện, trực tiếp), thời điểm, người ghi. Khi "không liên lạc được" với mọi người trong thứ tự gọi: có nguyện vọng Hiệu lực thì làm theo nguyện vọng đó; chưa có thì Bác sĩ quyết định theo chuyên môn và bắt buộc lý do. *(Nguồn: BR-M05-16)*
- **FR-050d**: Người xử lý MUST ghi **lựa chọn được thực hiện** và thực hiện lệnh tương ứng: "chuyển bệnh viện điều trị tích cực" → Chuyển viện (FR-049, kèm bản tóm tắt FR-050); "đưa về nhà" → Cho tạm vắng ở feature 004 với lý do "về nhà theo nguyện vọng cuối đời" (Hành chính hoặc Trưởng tầng thực hiện qua quy trình đón; Hành chính lập hồ sơ kết thúc lưu trú khi gia đình quyết định, Q-218); "ở lại viện chăm sóc giảm nhẹ" → điều dưỡng phụ trách được báo xem xét kế hoạch chăm sóc (feature 005 FR-011, BR-M04-20; không tạo yêu cầu, Q-306). Cảnh báo nguy kịch MUST chỉ được đóng khi đã có kết quả xác nhận lại (FR-050c) và lựa chọn đã ghi; đóng bắt buộc kết quả. *(Nguồn: 9.5, BR-M05-15, UC-84)*
- **FR-050e**: Cảnh báo nguy kịch theo vòng đời FR-029 (tiếp nhận, xử lý, đóng), với điều kiện đóng bổ sung của FR-050d. Leo thang theo mức Khẩn cấp (mọi người nhận được báo đồng thời, không leo thang theo cấp). *(Nguồn: 9.3, 9.4)*
- **FR-050f**: Thẻ thông tin khẩn cấp và danh sách người cao tuổi của Bác sĩ, Điều dưỡng, Trưởng tầng MUST hiển thị dấu nguy kịch đang mở. Nội dung nguyện vọng chỉ hiện với người có quyền xem sức khỏe (feature 001 FR-023e). *(Nguồn: 9.5, 19.3)*
- **FR-050g**: Mọi kết quả xác nhận lại và lựa chọn thực hiện là bản ghi nhóm 3, gắn với cảnh báo nguy kịch; sai sót xử lý bằng đính chính. *(Nguồn: BR-M05-16, 1.5)*

#### H. Sự cố ngã

- **FR-051**: **(Viết lại, 2026-10-06, Q-306, Q-307)** Khi sự cố loại Ngã được ghi nhận, trong cùng lần ghi hệ thống MUST: báo bác sĩ cần đánh giá lại, căn cứ "sau sự cố ngã" (feature 001 FR-040, BR-M01-02); báo điều dưỡng phụ trách xem xét kế hoạch chăm sóc (feature 005 FR-011, BR-M04-20); báo Người phụ trách ca của tầng người cao tuổi lập công việc theo dõi sau ngã: đo các chỉ số nhóm "sinh hiệu" (FR-001) mỗi khoảng của CFG-M05-06 (mặc định \[4 giờ\] trong \[72 giờ\]) (feature 005 FR-017). Hệ thống MUST NOT tạo yêu cầu, lịch đo hay công việc. Người cao tuổi MUST mang trạng thái tính được "đang theo dõi sau ngã" từ mốc tới mốc cộng thời gian theo dõi, hiển thị trên thẻ người cao tuổi; công việc theo dõi lập với căn cứ là sự cố ngã và MUST có thời điểm dự kiến trong khoảng theo dõi. Mốc được đọc như sau (CHK013): nếu sự cố có thời điểm xảy ra thì mốc là thời điểm xảy ra, nhưng không sớm hơn (thời điểm ghi nhận trừ một khoảng đo); nếu không có thì mốc là thời điểm ghi nhận. Lần đo đầu là mốc cộng một khoảng đo; khoảng theo dõi kết thúc tại mốc cộng thời gian theo dõi. Ví dụ với khoảng 4 giờ: ngã 02:00, ghi nhận 09:00 thì mốc là 05:00 và lần đo đầu là 09:00; ngã 08:00, ghi nhận 09:00 thì mốc là 08:00 và lần đo đầu là 12:00. *(Nguồn: BR-M05-07, CFG-M05-06, Q-306, Q-307)*
- **FR-052**: Mỗi người cao tuổi MUST có tối đa một khoảng theo dõi sau ngã đang diễn ra; lần ngã mới trong khi đang theo dõi MUST tính lại khoảng theo dõi từ lần ngã mới theo FR-051 và báo lại Người phụ trách ca. Bác sĩ MUST kết thúc theo dõi sớm được bằng lệnh "Kết thúc theo dõi sau ngã" kèm lý do; trong cùng lệnh, công việc theo dõi Chưa đến hạn có căn cứ là sự cố đó chuyển Hủy (feature 005). *(Suy ra từ BR-M05-07; sửa 2026-10-06, Q-307)*

#### I. Sự cố lây nhiễm, truy vết và khoanh vùng

- **FR-060**: Khi sự cố loại "lây nhiễm nghi ngờ" được ghi nhận, người cao tuổi của sự cố MUST mang dấu "nghi nhiễm" cho tới khi sự cố đóng, và hệ thống MUST báo Điều dưỡng phụ trách và các Bác sĩ cần lập danh sách tiếp xúc. **(Sửa, 2026-10-06, Q-307)** Hệ thống MUST NOT tự lập hay đề xuất danh sách; người lập (Điều dưỡng trong phạm vi hoặc Bác sĩ) tra cứu được dữ liệu theo các nhóm cần xét, trong CFG-M05-07 (mặc định \[5 ngày\]) trước thời điểm phát hiện: (a) người cao tuổi có phân bổ cùng phòng chồng thời gian (feature 003 FR-035); (b) người cao tuổi cùng điểm danh tham gia một buổi hoạt động (feature 014); (c) nhân viên được phân công chăm sóc người đó (feature 008); (d) người thân có lượt thăm người đó ở trạng thái Đã vào hoặc Đã ra, và người đi cùng thực tế vào của các lượt đó (chỉ có họ tên, quan hệ, số điện thoại nếu có; feature 012 FR-038). Mỗi người được thêm vào danh sách MUST kèm nhóm, nguồn và khoảng thời gian tiếp xúc; các khoảng người nghi nhiễm vắng mặt MUST được đánh dấu (feature 003 FR-036). Danh sách tiếp xúc MUST được lập, xác nhận, bổ sung hoặc loại người bởi Điều dưỡng (khi người nghi nhiễm thuộc phạm vi phân công) hoặc Bác sĩ (toàn viện); Quản lý viện và Trưởng tầng chỉ xem. Ở mục (c), "nhân viên được phân công chăm sóc" gồm: nhân viên có phân công chăm sóc người đó (phụ trách chính hoặc hỗ trợ) trong một ca thuộc khoảng truy vết, và nhân viên đã ghi kết quả một công việc hoặc một liều cho người đó trong khoảng (kể cả công việc chung của tầng) (CHK018). *(Nguồn: BR-M05-10, CFG-M05-07, 9.6; Clarification 2026-09-26, đề xuất Q-68; khoảng thời gian dùng chung cho mọi nguồn là giả định, xem Điểm cần báo lại 10)*
- **FR-061**: Vòng đời danh sách tiếp xúc MUST theo bảng dưới; không có chuyển nào khác. "Người xác nhận" là Điều dưỡng (trong phạm vi) hoặc Bác sĩ (FR-060). Khi feature 003 báo một bản đính chính phân bổ làm đổi phòng hoặc khoảng thời gian trong khoảng đã dùng để lập danh sách (feature 003 FR-026, Q-274), hệ thống MUST báo người xác nhận của danh sách và các Bác sĩ để xem lại; danh sách MUST NOT tự đổi, việc thêm hay loại người theo bảng dưới. *(Nguồn: BR-M05-10, 11, constitution IV; 7.5 "(Bổ sung, Q-274)")*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái mới | Người thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Lập danh sách tiếp xúc | Nháp | Người xác nhận | Sự cố lây nhiễm nghi ngờ chưa đóng; sự cố chưa có danh sách | — |
| Nháp | Thêm, bỏ người | Nháp | Người xác nhận | Người thêm có nhóm, nguồn, khoảng tiếp xúc | Lưu lịch sử |
| Nháp | Xác nhận danh sách | Đã xác nhận | Người xác nhận | Có ít nhất một người hoặc ghi "không có người tiếp xúc" kèm lý do | FR-062 |
| Đã xác nhận | Bổ sung người tiếp xúc | Đã xác nhận | Người xác nhận | Bắt buộc lý do | FR-062 cho người mới |
| Đã xác nhận | Loại người khỏi danh sách | Đã xác nhận | Người xác nhận | Bắt buộc lý do | Gỡ dấu "tiếp xúc"; công việc đo nhiệt độ Chưa đến hạn có căn cứ là danh sách này chuyển Hủy (feature 005) |
| Nháp, Đã xác nhận | Sự cố lây nhiễm được đóng | Đã kết thúc | Hệ thống | — | Dấu "tiếp xúc" và khoảng theo dõi giữ tới hết thời gian theo dõi |

- **FR-062**: Khi danh sách được xác nhận hoặc bổ sung, với mỗi người mới: người cao tuổi MUST mang dấu "tiếp xúc" kèm khoảng theo dõi theo CFG-M05-08 (mặc định \[2 lần/ngày trong 7 ngày\]) từ thời điểm xác nhận (khoảng trùng thời gian của cùng người cao tuổi được tính gộp tới thời điểm kết thúc muộn nhất), và Người phụ trách ca của tầng người đó MUST được báo lập công việc đo nhiệt độ (feature 005 FR-017); hệ thống MUST NOT tự tạo lịch đo hay công việc (**sửa, 2026-10-06, Q-307**); Quản lý viện MUST được thông báo đầy đủ; nhân viên và người thân trong danh sách MUST được thông báo (qua feature 009; người thân gửi theo nhóm "người thân được nêu trong bản ghi nguồn", chỉ nhận phần loại "chung", feature 009 FR-007 (d), Q-112) với nội dung chỉ gồm "có thể đã tiếp xúc tại [tầng/khu vực, khoảng thời gian], đề nghị làm theo hướng dẫn của viện" và số liên hệ, MUST NOT nêu danh tính, phòng hay tình trạng của người nghi nhiễm. Người thân không có tài khoản hay kênh nhận thông báo được liệt kê cho Hành chính liên hệ trực tiếp với cùng nội dung. Hành chính MUST chỉ thấy phần này của danh sách: họ tên, quan hệ, số điện thoại của người thân cần liên hệ và nội dung cần báo; MUST NOT thấy người nghi nhiễm, người cao tuổi tiếp xúc hay nhân viên tiếp xúc (CHK062). Người thân đến thăm một người duy nhất có thể tự suy ra người nghi nhiễm từ nội dung "tầng/khu vực, khoảng thời gian"; rủi ro này được chấp nhận một cách tường minh, vì việc báo cho người đã tiếp xúc cần cho sức khỏe của chính họ, và nội dung đã ở mức tối thiểu (CHK067). Dấu "nghi nhiễm" và "tiếp xúc" MUST sẵn có cho feature 003 (bắt buộc ghi đã khử khuẩn khi Xác nhận giường sẵn sàng, feature 003 FR-045; sửa, 2026-10-06, Q-282). *(Nguồn: BR-M05-11, CFG-M05-08, 9.6 "thực hiện thông báo theo quy định", DBR-03, 19.3; Clarification 2026-09-26 lượt 2, đề xuất Q-72)*
- **FR-063**: Bác sĩ hoặc Quản lý viện MUST thực hiện được "Khoanh vùng" một vùng gồm một hoặc nhiều tầng, hoặc toàn bộ một khu vực (cấu trúc 2.2), kèm lý do và gắn ít nhất một sự cố lây nhiễm chưa đóng. Vùng bao mọi phòng và khu vực chung thuộc các tầng/khu vực đã chọn. Khoanh vùng MUST NOT chọn từng phòng riêng lẻ; cô lập một phòng dùng lệnh "Đặt cách ly phòng" của feature 003 (FR-008). *(Nguồn: 9.6, UC-37, BR-M05-11, 4.4 dòng "Khoanh vùng lây nhiễm"; Clarification 2026-09-26, đề xuất Q-69)*
- **FR-064**: Vòng đời khoanh vùng MUST theo bảng dưới; không có chuyển nào khác. *(Nguồn: BR-M05-11, 12, KHOANH_VUNG, constitution IV)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái mới | Người thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Khoanh vùng | Đang khoanh vùng | Bác sĩ, Quản lý viện | FR-063 | FR-065; thông báo Trưởng tầng, Hành chính, điều dưỡng có ca trong vùng; dashboard (18.5) |
| Đang khoanh vùng | Gỡ khoanh vùng | Đã gỡ | Bác sĩ, Quản lý viện | Bắt buộc lý do | Bỏ các chặn của FR-065 với phần không còn vùng nào khác bao (BR-M05-12) (sửa, Q-282: không còn khử khuẩn do hệ thống sinh) |

- **FR-065**: Trong thời gian một vùng Đang khoanh vùng, hệ thống MUST cung cấp trạng thái "đang chịu khoanh vùng" cho mọi phòng, khu vực chung và người cao tuổi có giường trong vùng, để: chặn đăng ký thăm mới cho người cao tuổi trong vùng (feature 012, BR-M10-02); chặn đăng ký mới vào hoạt động chung của người cao tuổi trong vùng và buổi hoạt động diễn ra trong vùng, đồng thời để feature 014 tự hủy buổi và đăng ký đã có trong vùng (feature 014 FR-021, BR-M04-15, BR-M05-11, Q-164); chặn phân bổ giường mới và đăng ký lịch đến khu nghỉ bán trú trong vùng (feature 003 FR-009, FR-042). *(Sửa 2026-10-06, Q-282: không sinh công việc khử khuẩn.)* Lượt thăm đã duyệt trong vùng được feature 012 tự hủy (feature 012 FR-037, Q-120); danh sách lượt bị hủy và đăng ký hoạt động đã có trong thời gian khoanh vùng MUST được liệt kê cho Hành chính và Trưởng tầng. Hoạt động ngoài viện là một loại hoạt động nên cũng bị chặn đăng ký theo quy tắc trên. Lệnh Cho tạm vắng, quy trình đón và Chuyển viện với người cao tuổi trong vùng MUST NOT bị chặn (gia đình có quyền đón, và chuyển viện là việc y tế); hệ thống MUST cảnh báo người thực hiện rằng người đó thuộc vùng đang khoanh vùng trước khi xác nhận lệnh (CHK008). *(Nguồn: BR-M05-11, 9.6 "hạn chế hoạt động/thăm nom theo chính sách")*
- **FR-066**: Sự cố lây nhiễm MUST NOT đóng được khi còn vùng Đang khoanh vùng chỉ gắn với sự cố đó. Cùng điều kiện này MUST chặn đính chính "Hủy ghi nhận" và đính chính đổi loại khỏi "lây nhiễm nghi ngờ": người thực hiện phải gỡ vùng, hoặc gắn vùng với một sự cố lây nhiễm khác chưa đóng, trước (CHK054). Kết quả xử lý của sự cố lây nhiễm MUST chọn một trong: xác nhận nhiễm, loại trừ, không xác định. Việc đã báo cơ quan y tế (nếu có) MUST ghi được như một diễn biến (thời điểm, người báo, nơi nhận). *(Suy ra từ BR-M05-12, 9.6)*

#### J. Phạm vi y tế và khám, điều trị

- **FR-070**: Quản lý viện MUST lưu được giấy phép hoạt động của cơ sở gồm: số, cơ quan cấp, ngày cấp, ngày hết hạn, phạm vi hoạt động (có hay không có phạm vi khám bệnh, chữa bệnh; danh sách phạm vi chuyên môn được phép), bản scan. Cùng nơi này, Quản lý viện MUST cấu hình được "số liên hệ của viện" dùng trong thông báo khẩn cấp và thông báo tiếp xúc (FR-047, FR-062, feature 009); mỗi lần đổi được ghi nhật ký (CHK057). *(Nguồn: 10.4 "(Bổ sung)", 4.4 dòng "Tài khoản, phân quyền, tham số": QL C)*
- **FR-071**: Cơ sở MUST được coi là có phạm vi khám bệnh, chữa bệnh cho một phạm vi chuyên môn khi và chỉ khi có giấy phép còn hiệu lực (tới hết ngày hết hạn, feature 002 FR-038) bao gồm phạm vi đó. Hệ thống MUST NOT có công tắc riêng khác với giấy phép; MUST NOT coi "có bác sĩ" là có phạm vi khám chữa bệnh. Dữ liệu này MUST sẵn có cho feature 002 FR-037 (c). *(Nguồn: 10.4, BR-M06-05)*
- **FR-072**: Bác sĩ MUST ghi nhận được khám, đánh giá, chẩn đoán, chỉ định, điều trị, kết quả cho người cao tuổi, mỗi bản ghi gồm: loại, nội dung, cơ sở thực hiện (viện hoặc cơ sở bên ngoài), người thực hiện, thời điểm, đính kèm. Điều dưỡng MUST ghi được bản ghi có cơ sở thực hiện bên ngoài (bắt buộc tên cơ sở, người thực hiện, ngày) và bản ghi loại khám, kết quả tại viện. Điều dưỡng ghi bản ghi tại viện với tư cách người ghi lại: bản ghi loại "khám" tại viện do Điều dưỡng ghi MUST có người thực hiện là một Bác sĩ, và Điều dưỡng MUST NOT được ghi mình là người thực hiện khám; bản ghi loại "kết quả" là kết quả nhận về (ví dụ xét nghiệm). Điều dưỡng không tự khám, chẩn đoán hay chỉ định (10.2) (CHK019). *(Nguồn: 10.2; quyền là giả định, xem Điểm cần báo lại 9)*
- **FR-073**: Bản ghi chẩn đoán, chỉ định, điều trị với cơ sở thực hiện là viện MUST chỉ được lưu khi, tại thời điểm lưu, thao tác thỏa kiểm tra nghiệp vụ chuyên môn "chẩn đoán và kê đơn nội bộ" (feature 002 FR-036, FR-037): cơ sở có phạm vi khám chữa bệnh phù hợp còn hiệu lực và bác sĩ có giấy phép còn hiệu lực đúng phạm vi. *(Nguồn: BR-M06-05, 10.2 "phạm vi thực hiện tại viện phụ thuộc...")*
- **FR-074**: Bản ghi khám và điều trị MUST NOT sửa hay xóa; đính chính theo feature 000. *(Nguồn: 1.5 nhóm 3)*
- **FR-075**: Giấy phép cơ sở đã lưu MUST NOT sửa hay xóa; gia hạn hoặc thay đổi phạm vi MUST tạo bản giấy phép mới; sai sót nhập liệu MUST xử lý bằng bản mới có lý do, bản cũ đánh dấu "được thay thế do sai sót" và không còn được dùng để xét hiệu lực. Mọi thao tác MUST được ghi nhật ký. Cảnh báo giấy phép sắp hết hạn theo feature 002 FR-039 (CFG-M06-02, mặc định \[60 ngày\]). *(Nguồn: 10.4, BR-M06-05, 19.4 "đặc biệt quan trọng với sức khỏe")*

#### K. Quyền xem, dữ liệu cung cấp và giao tiếp

- **FR-080**: Quyền MUST theo 4.4 (quyền tối đa, còn bị giới hạn bởi phạm vi và điều kiện pháp lý, BR-M15-01): *(Nguồn: 4.4, 19.3, BR-M01-08, DBR-03)*

| Chức năng | QL | TT | BS | ĐD | CS | Bếp (sửa, Q-282, Q-283) | HC | NT | NCT |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Chỉ số (UC-32) | Xem | Xem trong phạm vi | Xem | Ghi, xem trong phạm vi | Ghi, xem trong phạm vi | — | — | Xem khi có bản đồng ý (¹) | — |
| Ngưỡng, lịch đo (UC-33) | Xem | Xem trong phạm vi (19.3; Phụ lục 27 "P", Q-279) | Thiết lập | Xem | — | — | — | — | — |
| Cảnh báo (UC-34, UC-38) | Xem; tiếp nhận và giao khi Leo thang cấp 2 | Xử lý trong phạm vi | Xử lý | Xử lý trong phạm vi | Xem trong phạm vi | — | — | — | — |
| Sự cố, khẩn cấp (UC-35, UC-36) | Xem, ghi, kích hoạt khẩn cấp, giao người xử lý (Q-339) | Ghi (¹⁰), xử lý, giao người xử lý trong phạm vi | Ghi, xử lý, giao người xử lý | Ghi, xử lý trong phạm vi | Ghi (¹⁰) | Ghi (¹⁰) | Ghi (¹⁰), chỉ thông tin nhận dạng | Xem khi có bản đồng ý (¹) | — |
| Khoanh vùng (UC-37) | Thực hiện | Xem | Thực hiện | Xem | — | — | Xem | — | — |
| Danh sách tiếp xúc | Xem | Xem trong phạm vi | Xác nhận | Xác nhận trong phạm vi | — | — | Chỉ danh sách người thân cần liên hệ (FR-062) | — | — |
| Giấy phép cơ sở | Cấu hình | — | Xem | — | — | — | — | — | — |
| Khám và điều trị | Xem | Xem trong phạm vi | Ghi (FR-073) | Ghi (FR-072), xem trong phạm vi | — | — | — | Xem khi có bản đồng ý (¹) | — |
| Dấu nguy kịch, xác nhận lại nguyện vọng (UC-83, UC-84; bổ sung 2026-09-28) | Xem | Xem trong phạm vi | Ghi, gỡ dấu; xác nhận lại; thực hiện lựa chọn | Dấu tạm khi không có Bác sĩ trực (Q-218); xác nhận lại, thực hiện lựa chọn trong phạm vi | — | — | — | Nhận thông báo; xem khi có bản đồng ý (¹) | — |
| Nút SOS (UC-98; bổ sung 2026-10-18, Q-369) | — | — | — | — | — | — | — | — | Bấm, trên thiết bị đã gắn (FR-047d); cảnh báo tạo ra xử lý theo dòng "Cảnh báo" |
| Nút hỗ trợ khẩn cấp (UC-99; bổ sung 2026-10-22, Q-375; Kế toán cũng bấm được theo Phụ lục 27) | Bấm (⁴⁹) | Bấm (⁴⁹) | Bấm (⁴⁹) | Bấm (⁴⁹) | Bấm (⁴⁹) | Bấm (⁴⁹) | Bấm (⁴⁹) | — | — |

- **FR-081**: Hệ thống MUST cung cấp cho báo cáo sức khỏe và dashboard (18.3, 18.5, feature 016): *(bổ sung 2026-09-29)* người cao tuổi đang mang dấu nguy kịch (Đang mở, Tạm) kèm tầng; chỉ số theo thời gian; số cảnh báo, sự cố theo mức, loại, tầng; thời gian trung bình từ tạo cảnh báo đến tiếp nhận theo mức (mốc tạo là lần tạo đầu tiên, không đặt lại khi gộp; mốc tiếp nhận với cảnh báo kích hoạt khẩn cấp là thời điểm kích hoạt, với cảnh báo Quản lý viện tiếp nhận ở cấp 2 là thời điểm Quản lý viện tiếp nhận); số lần leo thang của cảnh báo; **số lần leo thang của sự cố và thời điểm sự cố được tiếp nhận xử lý (chuyển Đang xử lý), theo mức (Q-199)**; trường hợp đang theo dõi (khoảng theo dõi sau ngã, theo dõi tiếp xúc đang diễn ra); khu đang khoanh vùng; cảnh báo "đã leo thang tối đa"; dấu "không thuộc sức khỏe" của từng loại sự cố (FR-042). Định nghĩa là căn cứ cho feature 016: **(Bổ sung 2026-10-18, Q-369)** Mỗi cảnh báo MUST kèm nguồn, để báo cáo tách riêng cảnh báo từ nút SOS (feature 016 FR-041). **(Bổ sung 2026-10-22, Q-374, Q-375)** Nguồn gồm cả nút hỗ trợ khẩn cấp; cảnh báo từ hai nút này MUST kèm mốc đã tới nơi và dấu "không ghi mốc tới nơi" để báo cáo thời gian từ tạo đến tới nơi (18.3).
  - **lần đo** là bản ghi chỉ số đã xác nhận; bản ghi "chờ đo lại" đã được thay bằng lần đo lại không được tính (tính lần đo lại); bản ghi "chờ đo lại" tự xác nhận sau CFG-M06-04 được tính;
  - **vượt ngưỡng** chia hai mức, cảnh báo và nguy hiểm, theo ngưỡng đang hiệu lực lúc đo (ngưỡng cá nhân nếu có, không thì ngưỡng mặc định); chỉ số nhiều thành phần (ví dụ huyết áp) tính một lần ở mức cao nhất của các thành phần;
  - **mức dùng để xếp:** số cảnh báo, sự cố theo mức dùng mức hiện hành; thời gian từ tạo đến tiếp nhận theo mức dùng mức tại thời điểm tiếp nhận, vì hạn tiếp nhận và leo thang chạy theo mức lúc đó.

  *(Nguồn: 18.3, 18.5; FR-016, FR-033, FR-044; Q-199, Q-202, đồng bộ spec 016)*
- **FR-082**: **(Sửa 2026-10-29, Q-396: bỏ phần cung cấp cho bản tin định kỳ)** **(Bổ sung 2026-10-18, Q-352)** Mỗi sự cố cung cấp cho cổng và cho thông báo người thân MUST tách hai phần: phần chung (có sự cố, loại sự cố, mức sự cố, thời điểm, viện đã xử lý; Q-358) và phần sức khỏe (tổn thương, khám, chỉ số, điều trị, thuốc, nguyên nhân y tế, cùng mô tả, xử lý ban đầu, diễn biến; Q-358); feature 012 và 009 chỉ đưa phần sức khỏe tới người thân có quyền xem sức khỏe theo bản đồng ý (BR-M01-08). Sự cố liên quan bệnh truyền nhiễm áp Q-72.
- **FR-083**: Bảng tổng hợp giao tiếp với feature khác:

| Chiều | Feature | Sự kiện / dữ liệu |
| --- | --- | --- |
| Nhận | 001 | Nguyện vọng cuối đời (phiên bản Hiệu lực, feature 001 FR-023a), nguyện vọng khi điều trị (feature 001 FR-023j; bổ sung 2026-10-22), dị ứng, bệnh nền, lưu ý chăm sóc (FR-048; sửa, Q-281); trạng thái người cao tuổi; kết quả lệnh Chuyển viện |
| Cung cấp | 001 | *(bổ sung 2026-09-28)* Yêu cầu ghi phiên bản nguyện vọng mới khi kết quả xác nhận lại là "nguyện vọng mới" (FR-050c, feature 001 FR-023b) |
| Cung cấp | 004, 005 | *(bổ sung 2026-09-28)* Lựa chọn "đưa về nhà": Hành chính, Trưởng tầng được báo để thực hiện Cho tạm vắng lý do "về nhà theo nguyện vọng cuối đời" (feature 004; không tạo yêu cầu, Q-307); lựa chọn "chăm sóc giảm nhẹ": báo điều dưỡng xem xét kế hoạch chăm sóc (feature 005, Q-306) (FR-050d) |
| Cung cấp | 009 | *(bổ sung 2026-09-28)* Thông báo nguy kịch và thông báo gọi xác nhận lại nguyện vọng theo thứ tự gọi của BR-M13-02, không tạo yêu cầu gọi điện (FR-050b, Q-307) |
| Cung cấp | 001 | Căn cứ báo bác sĩ cần đánh giá lại sau sự cố ngã hoặc Trung bình trở lên (FR-043, FR-051; không tạo yêu cầu, Q-307); căn cứ sự cố cho lệnh Chuyển viện (FR-049) |
| Nhận | 002 | Quyền, phạm vi, ngoại lệ FR-044a, kiểm tra nghiệp vụ chuyên môn |
| Cung cấp | 002 | Giấy phép cơ sở và phạm vi (FR-071) |
| Nhận | 003 | Người cùng phòng và khoảng thời gian; khoảng vắng mặt (FR-060) |
| Cung cấp | 003 | Vùng khoanh vùng và gỡ; dấu nghi nhiễm, tiếp xúc (FR-062, FR-065) |
| Nhận | 004 | Chuyển viện, trở về, trạng thái cuối |
| Cung cấp | 004 | Cảnh báo, sự cố đang mở cho điều kiện Kết thúc lưu trú và danh sách việc sau qua đời (FR-038) |
| Nhận | 005 | Yêu cầu tạo/đóng cảnh báo; giá trị đo từ công việc; kết quả ngủ; công việc đo kết thúc Không thực hiện hoặc Quá hạn cuối ca |
| Cung cấp | 005 | Lịch đo (căn cứ lập công việc); sự cố ngã và khoảng theo dõi sau ngã; danh sách tiếp xúc và khoảng theo dõi tiếp xúc; lựa chọn chăm sóc giảm nhẹ (báo xem xét kế hoạch); bản ghi đã xác nhận / yêu cầu đo lại cho công việc đo (FR-018); cảnh báo đang mở cho checklist điều dưỡng |
| Nhận | 006 | Yêu cầu tạo/đóng cảnh báo thuốc; thuốc đang dùng |
| Nhận, Cung cấp | 008 | Nhận phân công, ca, Bác sĩ trực, Người phụ trách ca, xác nhận bàn giao; cung cấp danh sách cảnh báo, sự cố đang mở, chỉ số vượt ngưỡng trong ca để người lập bàn giao ghi (BR-M09-06, Q-307) |
| Cung cấp | 009 | Sự kiện thông báo theo mức và người nhận (FR-033, FR-035, FR-043, FR-045a, FR-047, FR-062, FR-064); nhận lại thời điểm gửi, đã xem |
| Nhận, Cung cấp | 011 | Nhận yêu cầu tạo cảnh báo xung đột dị ứng với chế độ ăn, thực đơn (mức Trung bình) và báo nguồn đã xử lý; nhận yêu cầu tạo cảnh báo Trung bình khi phục vụ suất có thành phần gây dị ứng bị chặn; sự cố "phục vụ sai suất ăn" do nhân viên ghi nhận (FR-040, Q-307). Gửi sự kiện "sụt cân" (FR-039a) |
| Nhận, Cung cấp | 013 | Sự cố loại "đồ gửi thất lạc" / "đồ gửi hư hỏng" do người ghi lệnh đồ gửi ghi nhận (nguồn "đồ gửi", FR-040, FR-042, Q-307), liên kết sự cố với đồ gửi, diễn biến "đã tìm thấy", yêu cầu chuyển Đã hủy (FR-046b); cung cấp sự cố đã ghi để liên kết và trạng thái mở/đóng cho căn cứ điều kiện đồ gửi (feature 013 FR-021, FR-025) |
| Nhận, Cung cấp | 012 | Nhận lượt thăm (FR-060) và người liên hệ chính; cung cấp trạng thái khoanh vùng, sự cố theo quyền (FR-082) |
| Cung cấp | 014 | *(bổ sung 2026-10-10)* Sự kiện dấu "nghi nhiễm" được gắn hoặc gỡ (FR-060) cho feature 014 FR-021a; ghi diễn biến vào cảnh báo theo yêu cầu kèm lệnh của feature 014 (FR-086) |
| Nhận, Cung cấp | 014 | Nhận điểm danh hoạt động; nhận yêu cầu tạo cảnh báo Khẩn cấp thiếu người khi về; lệnh "Báo thiếu người" của feature 014 ghi nhận sự cố "đi lạc hoặc không trở về" (Q-307); cung cấp trạng thái khoanh vùng |
| Cung cấp | 016 | FR-081 (chỉ số đã xác nhận và mức vượt ngưỡng lúc đo; cảnh báo, sự cố với mức hiện hành, mức lúc tiếp nhận, thời điểm tạo, tiếp nhận, các lần leo thang; theo dõi; khoanh vùng; dấu loại sự cố) |
| Nhận | 002 | *(bổ sung 2026-10-18, Q-370)* Người cao tuổi xác định từ thiết bị đã gắn cho lệnh nút SOS (feature 002 FR-071) |
| Nhận | 005 | *(bổ sung 2026-10-18)* Trạng thái có mặt trong ngày của người bán trú cho điều kiện gửi SOS (FR-047d) |
| Nhận | 014 | *(bổ sung 2026-10-18)* Chuyến đang diễn ra của người cao tuổi, trưởng đoàn và người đi cùng (FR-047e) |
| Cung cấp | 012 | *(bổ sung 2026-10-18)* Trạng thái cảnh báo SOS (Đã gửi, người đã tiếp nhận) và lý do từ chối cho kênh người cao tuổi (FR-047d, FR-047f; feature 012 FR-085) |
| Nhận | 008 | *(bổ sung 2026-10-22, Q-375)* Nhân viên đang trong ca theo tầng, Người phụ trách ca, Bác sĩ trực tại thời điểm bấm nút hỗ trợ khẩn cấp; giờ hành chính CFG-M13-06 cho vai trò không xếp ca (FR-047i) |
| Cung cấp | 012 | *(bổ sung 2026-10-22, Q-374)* Trạng thái "đang tới", "đã tới" cho kênh người cao tuổi (FR-047j) |
| Cung cấp | 016 | *(bổ sung 2026-10-22, Q-374, Q-375)* Nguồn nút hỗ trợ khẩn cấp, mốc đã tới nơi, dấu "không ghi mốc tới nơi" (FR-081) |
| Nhận | 000 | *(bổ sung 2026-10-22, Q-376)* Tham số CFG-M11-11 (thông tin viện) cho màn gọi 115 (FR-047k) |

- **FR-084** *(bổ sung, CHK035)*: Khi Bộ lập lịch ngừng chạy một thời gian rồi chạy lại, hệ thống MUST xử lý bù: (a) mỗi cảnh báo, sự cố đã quá hạn tiếp nhận lên đúng một cấp leo thang và hạn tiếp nhận mới tính từ lúc chạy lại; MUST NOT lên nhiều cấp trong một lần, để mỗi cấp có đủ thời gian tiếp nhận; (b) bản ghi "chờ đo lại" đã quá CFG-M06-04 được xác nhận với nhãn "chưa đo lại" và tạo cảnh báo ngay; (c) lịch đo, phiên bản ngưỡng tới thời điểm hiệu lực hoặc hết hạn được chuyển trạng thái với thời điểm hiệu lực giữ nguyên; (d) lần nhắc thiếu ngưỡng cá nhân (FR-025) và lần nhắc "đã leo thang tối đa" (FR-034) bị lỡ được gửi một lần. Mỗi việc làm bù ghi thời điểm thực hiện thực tế kèm thời điểm dự kiến. Mọi quy tắc theo thời gian của spec này (hạn tiếp nhận, leo thang, chờ đo lại, 24 giờ trượt của FR-038a, chuỗi đêm của FR-039c, khoảng truy vết) kiểm thử được bằng đồng hồ giả lập (NFR-13) (CHK045). *(Nguồn: NFR-04, NFR-13; feature 000 FR-037a)*
- **FR-085**: *(Bổ sung, 2026-10-10, Q-311)* Mức của các thông báo spec này chưa nêu mức (feature 009 FR-043a, FR-043b): **Trung bình** — báo lặp lại (BR-M05-03), cần lập công việc theo dõi sau ngã, cần lập danh sách tiếp xúc, cần lập công việc đo nhiệt độ, thông báo tiếp xúc, khoanh vùng, cần xác nhận dấu tạm, lựa chọn "đưa về nhà" (FR-050d), tạm nhận bàn giao; **Nhẹ** — nguồn đã được đính chính, nguồn đã xử lý trên sự cố liên kết, tác động còn hiệu lực, danh sách tiếp xúc cần xem lại, gỡ khoanh vùng, người phụ trách đổi, nguồn đồ gửi bị Hủy ghi nhận (FR-046b). *(Nguồn: Q-311, 17.1)*
- **FR-086**: *(Bổ sung, 2026-10-10)* Khi lệnh của module khác yêu cầu ghi diễn biến vào một cảnh báo đang mở (feature 014 FR-047: người Thiếu khi về đã trở về khi chưa có sự cố; FR-056: cô lập), hệ thống MUST ghi diễn biến vào cảnh báo đó kèm lệnh nguồn, người thực hiện và thời điểm; diễn biến không tự đóng cảnh báo. *(Nguồn: đồng bộ spec 014, BR-M04-17)*

**Truy vết use case** *(constitution I; bổ sung 2026-10-01, CHK052)*: mục B (FR-010 → FR-019) — UC-32; mục A phần lịch đo và mục C (FR-003 → FR-006, FR-020 → FR-025) — UC-33; mục D, E (FR-026 → FR-039c) — UC-34, UC-38; mục F, H (FR-040 → FR-046b, FR-051, FR-052) — UC-35; mục G (FR-047 → FR-050) — UC-36, và UC-16 cho Chuyển viện (actor Điều dưỡng, Bác sĩ); mục G1 (FR-050a → FR-050g) — UC-83, UC-84; FR-047d → FR-047h, FR-047j — UC-98; FR-047i — UC-99 (bổ sung 2026-10-22); mục I (FR-060 → FR-066) — UC-37. Mục J (FR-070 → FR-075) và mục K (FR-080 → FR-084) không có UC riêng: mục J theo 10.2, 10.4 và BR-M06-05; mục K là quyền, dữ liệu cung cấp và việc theo lịch.

### Key Entities *(include if feature involves data)*

- **Chỉ số (danh mục)** – nhóm 1: tên, đơn vị, thành phần, khoảng hợp lệ vật lý từng thành phần (CFG-M06-03), thuộc nhóm "sinh hiệu", trạng thái hiệu lực.
- **Lịch đo** – nhóm 2: người cao tuổi, chỉ số, tần suất và mốc giờ, bắt đầu, kết thúc, mức quan trọng, nguồn (chỉ định bác sĩ, FR-003; sửa 2026-10-10, Q-306), "thay thế cho", trạng thái (Chờ hiệu lực / Hiệu lực / Đã ngừng / Hết hạn), người tạo, lý do.
- **Phiên bản ngưỡng (NGUONG_CANH_BAO)** – nhóm 2: phạm vi (cơ sở / cá nhân), người cao tuổi, chỉ số, bốn cận cho từng thành phần, thời điểm hiệu lực, trạng thái (Chờ hiệu lực / Hiệu lực / Hết hiệu lực / Đã hủy), người thiết lập, lý do.
- **Bản ghi chỉ số (CHI_SO)** – nhóm 3: người cao tuổi, chỉ số, giá trị từng thành phần, thời điểm đo, người đo, công việc nguồn, thời điểm ghi trên thiết bị và đồng bộ, ngưỡng áp dụng và kết quả so, trạng thái xác nhận (chờ đo lại / đã xác nhận / đã đo lại), nhãn (xử lý ngay, chưa đo lại, ghi nhận muộn, ghi ngoại tuyến), bản ghi đo lại, các bản đính chính.
- **Cảnh báo (CANH_BAO)** – nhóm 2: người cao tuổi, loại, khóa loại, mức, bản ghi nguồn, số lần, thời điểm đầu và gần nhất, trạng thái (Mới / Leo thang / Đã tiếp nhận / Đang xử lý / Đã đóng / Chuyển sự cố), cấp leo thang, người phụ trách, hạn tiếp nhận, người và thời điểm tiếp nhận, kết quả, người đóng, sự cố liên kết, các lần báo lặp lại (FR-038a); lịch sử (nhóm 3). *(Bổ sung 2026-10-22)* Nguồn (hệ thống, nhân viên tạo thủ công, người cao tuổi, nhân viên bấm nút hỗ trợ khẩn cấp), người bấm, tầng, loại sự việc, nơi đã chọn, mốc đã tới nơi; người cao tuổi MAY trống chỉ với loại "nhân viên yêu cầu hỗ trợ".
- **Quy tắc xu hướng (danh mục)** – nhóm 1: tên, bật/tắt, mã tham số, mức cảnh báo.
- **Vị trí kèm cảnh báo** *(bổ sung 2026-10-22, Q-373)* – nhóm 3: cảnh báo, vĩ độ, kinh độ, sai số (mét), thời điểm đo, thời điểm nhận.
- **Lượt gọi 115** *(bổ sung 2026-10-22, Q-376)* – nhóm 3: cảnh báo hoặc sự cố, người bấm, thời điểm bấm, kết quả (Đã gọi được / Không gọi được).
- **Sự cố (SU_CO)** – nhóm 3 cho nội dung: người cao tuổi, loại, nguồn phát sinh, thời điểm xảy ra và phát hiện, địa điểm, hoạt động, người phát hiện, mô tả, mức (và lịch sử đổi mức), xử lý ban đầu, cảnh báo nguồn, diễn biến (đánh giá, hành động, biện pháp hồi sức, thông báo, theo dõi), xác nhận đã đối chiếu nguyện vọng, bản chụp thẻ thông tin khẩn cấp, kết quả xử lý, người đóng; trạng thái xử lý (Mới / Đang xử lý / Đang theo dõi / Đã đóng / Đã hủy), cấp leo thang, người xử lý hiện tại và lịch sử phân công (FR-045a).
- **Dấu nguy kịch (DAU_NGUY_KICH)** *(bổ sung 2026-09-28)* – nhóm 2: người cao tuổi, loại (chính thức / tạm), nhận định, người ghi, thời điểm, Bác sĩ xác nhận, trạng thái (Tạm / Đang mở / Đã gỡ), lý do gỡ, cảnh báo nguy kịch liên kết.
- **Kết quả xác nhận lại nguyện vọng, lựa chọn thực hiện** *(bổ sung 2026-09-28)* – nhóm 3: cảnh báo nguy kịch, người được liên hệ, kênh, thời điểm, kết quả (giữ / mới / không liên lạc được), phiên bản nguyện vọng mới nếu có, lựa chọn thực hiện, lệnh đã thực hiện, người ghi.
- **Bản tóm tắt chuyển viện** – nhóm 3: sự cố, người cao tuổi, cơ sở tiếp nhận, dị ứng, thuốc đang dùng, chỉ số gần nhất, diễn biến, nguyện vọng cuối đời, người lập, thời điểm.
- **Phiếu tóm tắt chuyển viện** *(bổ sung 2026-10-22, Q-383)* – nhóm 3: bản tóm tắt, file PDF, các lần in (người, thời điểm); giấy tờ của bệnh viện đính kèm lệnh Chuyển viện.
- **Danh sách tiếp xúc** – nhóm 2: sự cố lây nhiễm, trạng thái (Nháp / Đã xác nhận / Đã kết thúc), từng người (loại: người cao tuổi / nhân viên / người thân; nhóm; nguồn; khoảng tiếp xúc; khoảng theo dõi; bị loại và lý do), người lập, người xác nhận, thời điểm. *(Sửa 2026-10-06, Q-307: không còn đề xuất.)*
- **Khoanh vùng (KHOANH_VUNG)** – nhóm 2: vùng (danh sách tầng, hoặc một khu vực), sự cố gắn, bắt đầu, kết thúc, người khoanh, người gỡ, lý do, trạng thái (Đang khoanh vùng / Đã gỡ).
- **Giấy phép hoạt động của cơ sở** – nhóm 1 có lịch sử: số, cơ quan cấp, ngày cấp, ngày hết hạn, phạm vi, bản scan, bản được thay thế.
- **Bản ghi khám và điều trị** – nhóm 3: người cao tuổi, loại, nội dung, cơ sở thực hiện, người thực hiện, thời điểm, đính kèm, các bản đính chính.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Với bộ kiểm thử 300 giá trị đo (6 chỉ số, có giá trị bằng cận, ngoài khoảng vật lý, ngưỡng cá nhân và mặc định, phiên bản ngưỡng đổi giữa ngày), 100% giá trị được xếp đúng mức so với bảng tính tay; 0 giá trị phi vật lý được lưu; 100% giá trị Nguy hiểm có yêu cầu đo lại hoặc nhãn "xử lý ngay".
- **SC-002**: Người đo thấy kết quả so ngưỡng (bình thường, cảnh báo, nguy hiểm, yêu cầu đo lại) ngay khi lưu, không quá 2 giây; cảnh báo tương ứng xuất hiện với người phụ trách không quá 10 giây sau khi lưu. Hai mốc này đo ở quy mô NFR-01 (60 người dùng đồng thời), kể cả khi nhiều bản ghi ngoại tuyến được đồng bộ cùng lúc vào giờ giao ca; với bản ghi đồng bộ, mốc tính từ lúc hệ thống nhận bản ghi (CHK044).
- **SC-003**: Trong kiểm thử đồng thời (10 phát sinh cùng loại cho cùng người cao tuổi gửi cùng lúc), 0 trường hợp có hai cảnh báo cùng khóa loại đang mở; số lần của cảnh báo bằng số phát sinh.
- **SC-004**: Với đồng hồ giả lập, 100% cảnh báo Trung bình chưa tiếp nhận được leo thang không muộn hơn 1 phút sau mỗi hạn, đúng cấp, và 100% lần leo thang có bản ghi lịch sử; 0 cảnh báo đã tiếp nhận bị leo thang.
- **SC-005**: Khi kích hoạt khẩn cấp, thẻ thông tin khẩn cấp hiển thị cho người xử lý không quá 3 giây; thông báo tới mọi người nhận của FR-047 được gửi song song và đến thiết bị trong không quá 10 giây (NFR-03).
- **SC-006**: 0 biện pháp hồi sức được ghi cho người cao tuổi có nguyện vọng cuối đời đã ghi nhận mà thiếu xác nhận "đã đối chiếu nguyện vọng"; 0 lần sửa hay xóa trực tiếp sự cố Khẩn cấp hoặc bản ghi chỉ số.
- **SC-007** *(sửa 2026-10-06, Q-306, Q-307)*: 100% sự cố ngã gửi đủ ba thông báo (bác sĩ cần đánh giá lại, điều dưỡng xem xét kế hoạch, Người phụ trách ca lập công việc theo dõi) trong cùng lần ghi; 0 yêu cầu, lịch đo hay công việc do hệ thống tạo; 0 người cao tuổi có hai khoảng theo dõi sau ngã cùng diễn ra.
- **SC-008** *(sửa 2026-10-06, Q-307)*: Với bộ dữ liệu truy vết dựng sẵn (phòng, điểm danh, phân công, lượt thăm trong 10 ngày), kết quả tra cứu theo các nhóm khớp 100% danh sách tính tay trong CFG-M05-07 và 0 người ngoài khoảng; 0 danh sách tiếp xúc do hệ thống tự lập.
- **SC-009**: Trong thời gian khoanh vùng, 0 đăng ký thăm mới, đăng ký hoạt động chung mới, phân bổ giường mới trong vùng thành công; sau khi gỡ, 100% các chặn được bỏ ngay (trừ phòng đang cách ly riêng hoặc thuộc vùng khác còn hiệu lực).
- **SC-010**: 0 bản ghi chẩn đoán, chỉ định, điều trị tại viện được lưu khi cơ sở hoặc bác sĩ thiếu giấy phép đúng phạm vi còn hiệu lực.
- **SC-011**: Nhân viên ghi xong một sự cố Nhẹ hoặc Trung bình trong không quá 90 giây và kích hoạt khẩn cấp trong không quá 15 giây tính từ lúc mở chức năng. Các con số này là mục tiêu nghiệm thu, không phải tham số cấu hình.
- **SC-012**: 0 lần người ghi sự cố ngoài phạm vi thấy thông tin sức khỏe của người cao tuổi; 100% lần như vậy có nhật ký "ngoài phạm vi".
- **SC-013**: Báo cáo thời gian trung bình từ tạo cảnh báo đến tiếp nhận và số lần leo thang khớp 100% số tính tay trên bộ kiểm thử 50 cảnh báo.
- **SC-014**: Trong bộ kiểm thử thông báo khẩn cấp và thông báo tiếp xúc, 0 thông báo gửi người không có bản đồng ý chia sẻ dữ liệu chứa thông tin sức khỏe, và 0 thông báo tiếp xúc nêu danh tính người nghi nhiễm (FR-047, FR-062, Q-72).
- **SC-015** *(bổ sung 2026-09-28)*: 100% lần ghi dấu nguy kịch tạo đúng một cảnh báo nguy kịch và một thông báo gọi xác nhận lại trong cùng lần, không có yêu cầu gọi điện nào được tạo; thông báo tới Bác sĩ trực, Điều dưỡng phụ trách, Trưởng tầng, Quản lý viện, người đại diện đến thiết bị trong không quá 10 giây (NFR-03); 0 cảnh báo nguy kịch được đóng khi thiếu kết quả xác nhận lại hoặc lựa chọn thực hiện; 0 thông báo tới người thân chứa nội dung nguyện vọng.
- **SC-016** *(bổ sung, CHK030, CHK031, CHK066)*: 0 lần người liên hệ chính được báo chỉ vì một cảnh báo Khẩn cấp chưa chuyển sự cố, và 0 lần Quản lý viện được báo ở bước đó trừ trường hợp FR-047b và FR-050b (Q-67). 0 lần Quản lý viện tiếp nhận được cảnh báo chưa ở Leo thang cấp 2, và 0 lần Quản lý viện bắt đầu xử lý, đóng hay đổi mức cảnh báo, sự cố (Q-71). 0 lần Điều dưỡng xác nhận danh sách tiếp xúc của người nghi nhiễm ngoài phạm vi phân công (Q-68). 100% thông báo Khẩn cấp phát ra khi không có Bác sĩ trực đến mọi tài khoản Bác sĩ đang Hoạt động và mục nguồn mang dấu "không có Bác sĩ trực tại thời điểm" (Q-74). 0 lần đo bị bỏ sinh hai cảnh báo khác khóa loại (Q-75).
- **SC-017** *(bổ sung 2026-10-18, Q-369)*: 100% lần gửi SOS hợp lệ (hết đếm ngược hoặc chạm lần hai) tạo hoặc gộp vào đúng một cảnh báo SOS đang mở của người cao tuổi đó, và 0 lần người liên hệ chính được báo chỉ vì cảnh báo SOS chưa chuyển sự cố.
- **SC-018** *(bổ sung 2026-10-22, Q-373)*: 100% lệnh SOS hợp lệ tạo cảnh báo mà không chờ vị trí (thời gian tạo như khi tắt GPS); tọa độ mới hiện với người nhận không quá 10 giây sau khi hệ thống nhận; 0 lần người thân thấy vị trí.
- **SC-019** *(bổ sung 2026-10-22, Q-375)*: 100% lần bấm nút hỗ trợ khẩn cấp hợp lệ tạo hoặc gộp đúng một cảnh báo đang mở của người bấm; thông báo tới mọi người nhận FR-047i đến thiết bị không quá 10 giây (NFR-03); 0 lần người liên hệ chính được báo vì cảnh báo này khi chưa chuyển sự cố.
- **SC-020** *(bổ sung 2026-10-22, Q-374, Q-376)*: Thời gian từ tạo đến tới nơi của báo cáo khớp 100% số tính tay trên bộ kiểm thử 30 cảnh báo SOS và nút hỗ trợ khẩn cấp; 100% lượt "Gọi 115" Đã gọi được có mốc gọi cấp cứu trên sự cố liên kết.
- **SC-021** *(bổ sung 2026-10-22, Q-383)*: 100% lệnh Chuyển viện thành công có phiếu tóm tắt PDF khớp bản tóm tắt; phiếu dựng xong trong không quá 3 giây; 0 phiếu thiếu dòng "không thay giấy chuyển cơ sở khám bệnh, chữa bệnh".
- Các con số thời gian ở SC-002, SC-004, SC-005, SC-008 là mục tiêu nghiệm thu, không phải tham số cấu hình, như đã ghi ở SC-011 (CHK032).

## Assumptions

- Số feature `007` theo bản đồ feature ở mục 4.2 `docs/phan-tich-yeu-cau.md` (UC-16 phần sự cố, UC-32 → UC-38) và trùng số tuần tự kế tiếp.
- Quy tắc dùng chung theo feature 000; hồ sơ sức khỏe, nguyện vọng cuối đời, lưu ý chăm sóc, lệnh Chuyển viện theo feature 001/004; quyền và giấy phép theo feature 002; lịch sử vị trí và khoanh vùng phòng theo feature 003; lập công việc theo feature 005; thuốc theo feature 006. Spec này không lặp lại các quy tắc đó.
- Q-01 được dùng theo mặc định: chỉ số ghi được ngoại tuyến và đồng bộ sau; kích hoạt khẩn cấp bắt buộc trực tuyến (8.6).
- Lịch đo do Bác sĩ thiết lập theo dòng "Ngưỡng cảnh báo" (UC-33) vì Permission Matrix không có dòng riêng; ngưỡng mặc định của cơ sở cũng do Bác sĩ thiết lập (chuyên môn), không coi là tham số cấu hình của Quản lý viện.
- Người phụ trách ban đầu của cảnh báo là Điều dưỡng phụ trách, không phải nhân viên chăm sóc, vì nhân viên chăm sóc chỉ có quyền xem cảnh báo (4.4); chuỗi leo thang của BR-M05-01 được rút thành ba cấp (FR-033).
- Mỗi cấp leo thang của cảnh báo Trung bình chờ lại CFG-M05-01; cảnh báo Khẩn cấp không leo thang theo cấp vì mọi cấp đã được thông báo đồng thời, việc chưa xác nhận xử lý theo BR-M13-02.
- Cảnh báo Nhẹ có hạn "trong ca" được hiểu là tới hết ca hiện tại của tầng người cao tuổi.
- Sự cố dùng cùng thời hạn tiếp nhận và chuỗi leo thang với cảnh báo cùng mức (cột "Thời hạn tiếp nhận" của 9.3 áp cho cả hai).
- Mức độ của các quy tắc xu hướng không có trong tài liệu nguồn; mặc định ở FR-039 là đề xuất và cấu hình được.
- "Sụt cân ≥ 5% trong 30 ngày" được so với cân nặng cao nhất trong 30 ngày trước lần đo.
- CFG-M05-07 \[5 ngày\] được dùng cho mọi nguồn truy vết (cùng phòng, hoạt động, phân công, thăm), không chỉ hoạt động chung.
- Người tiếp xúc là nhân viên và người thân được thông báo, không có công việc đo nhiệt độ (công việc chỉ gắn người cao tuổi, 8.3).
- Mỗi sự cố gắn tối đa một người cao tuổi; ổ dịch nhiều người được thể hiện bằng nhiều sự cố lây nhiễm gắn cùng một vùng khoanh vùng. Trường hợp người nghi nhiễm là nhân viên hoặc người thân nằm ngoài phạm vi: hệ thống không có sự cố lây nhiễm không gắn người cao tuổi và không truy vết tự động từ người đó; viện xử lý ngoài hệ thống, và ghi sự cố lây nhiễm cho từng người cao tuổi có dấu hiệu (chủ dự án chốt 2026-10-01, CHK037).
- Hệ thống không gửi báo cáo cho cơ quan y tế; chỉ ghi nhận việc đã báo (FR-066).
- Bản ghi chỉ số, cảnh báo, sự cố, danh sách tiếp xúc, bản ghi khám và điều trị là dữ liệu gắn hồ sơ người cao tuổi, lưu giữ theo CFG-M15-03 (NFR-07, feature 000 FR-050); sự cố không gắn người cao tuổi lưu theo CFG-M15-04 (CHK011).
- Lần hiển thị thẻ thông tin khẩn cấp khi kích hoạt được lưu cùng sự cố (bản chụp thẻ và người đã được hiển thị, FR-047, FR-048a); các lần nhân viên xem lại thẻ hoặc bản tóm tắt chuyển viện sau đó không được ghi nhật ký, theo Q-03 đã chốt (feature 000 FR-043a; điểm cần tư vấn pháp lý đối chiếu ở mục 24.4) (CHK046).
- Ngưỡng mặc định của cơ sở là dữ liệu nhóm 2 theo phiên bản do Bác sĩ thiết lập (Phụ lục 27 dòng "Ngưỡng cảnh báo": BS T), không phải tham số CFG của Phụ lục 25, nên không thuộc BR-M15-04 (CHK047).
- Spec này cần danh mục chất lượng ngủ của feature 005 có nhóm "mất ngủ" (FR-039c); feature 005 FR-034 đã được ghi thêm yêu cầu này ngày 2026-10-01 (CHK048).
- Q-67 → Q-76 và CFG-M06-04 được ghi "đề xuất" ở phần Clarifications và một số FR vì khi đó chưa có trong tài liệu nguồn; nay đã có ở mục 24.2 và Phụ lục 25. Q-01 đã chốt theo mặc định ngày 2026-10-01.

## Điểm cần báo lại về tài liệu nguồn

*(Câu mở đầu lúc lập spec: "Chưa sửa `docs/`".)* **(2026-09-29, checklist cross-feature CHK026)** Tình trạng hiện tại của từng điểm ghi ở các đoạn có ngày bên dưới; điểm không được nhắc ở đoạn nào vẫn là "còn chờ chủ tài liệu xác nhận" (xem CHK027).

1. **Chuỗi leo thang BR-M05-01 bắt đầu từ "nhân viên"**, nhưng Permission Matrix 4.4 chỉ cho Nhân viên chăm sóc quyền X ở dòng "Xử lý cảnh báo" và UC-34 có actor là Điều dưỡng. Spec bắt đầu chuỗi từ Điều dưỡng phụ trách, nhân viên thực hiện công việc nguồn chỉ được thông báo (FR-031, FR-033). Cần sửa BR-M05-01. **Đã xử lý (2026-09-28):** BR-M05-01 trong docs/business/nghiep-vu.md đã được bổ sung đoạn "Làm rõ, spec 007" theo FR-031, FR-033.
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

**(2026-09-27)** Các quyết định Q-67 → Q-76 của spec này đã được phản ánh vào `docs/business/nghiep-vu.md` (9.3 → 9.6, BR-M05-02) và `docs/phan-tich-yeu-cau.md` (dòng "Danh sách tiếp xúc", chú thích ²³, ²⁴ của 4.4), và nằm ở mục 24.2.

**(2026-09-28, checklist cross-feature CHK018, CHK024, CHK030)** Đã phản ánh vào `docs/business/nghiep-vu.md`: điểm 3 (CFG-M06-04 ở Phụ lục 25 và BR-M06-04); điểm 4, 7 (bảng bổ sung vòng đời cảnh báo và bảng vòng đời sự cố ở 9.4); điểm 10 (BR-M05-10 và mô tả CFG-M05-07: khoảng truy vết áp cho mọi nguồn).

**(2026-09-28, góp ý nghiệp vụ Q-207 → Q-222) Tài liệu nguồn đã thay đổi, spec cần rà lại.** *(2026-09-29: mọi điểm dưới đây đã xử lý, xem nhãn "Đã xử lý" từng điểm.)* Các điểm dưới đây đã có trong `docs/business/nghiep-vu.md` và `docs/business/luong-nghiep-vu.md`; spec **chưa** được sửa theo, và cần chạy `/speckit-clarify` hoặc cập nhật FR tương ứng.

1. **[Đã xử lý 2026-09-28: Phạm vi 12, User Story 11, mục G1 FR-050a → FR-050g, FR-028, FR-080, FR-083, Key Entities, SC-015]** **Dấu nguy kịch và cảnh báo "nguy kịch – thực hiện nguyện vọng cuối đời" (Q-213, Q-218; 9.5, BR-M05-15, 16, UC-83, 84, BF-17, DBR-34).** Chức năng mới: Bác sĩ ghi, gỡ dấu (Điều dưỡng ghi dấu tạm khi không có Bác sĩ trực). Hệ thống tạo cảnh báo Khẩn cấp không gộp, báo gia đình, tạo yêu cầu xác nhận lại nguyện vọng theo thứ tự gọi của BR-M13-02, rồi thực hiện lựa chọn (chuyển viện / tạm vắng về nhà / chăm sóc giảm nhẹ). Cảnh báo chỉ đóng khi đã có kết quả xác nhận và lựa chọn. Đề xuất thêm một User Story.
2. **[Đã xử lý 2026-09-28: FR-048, FR-083]** **Nguyện vọng cuối đời có cấu trúc (Q-213, Q-216; 5.2).** Thẻ thông tin khẩn cấp (User Story 5) hiển thị phiên bản nguyện vọng Hiệu lực với ba lựa chọn đã chuẩn hóa, không còn là văn bản tự do.
3. **[Không cần sửa]** **Chỉ số thuộc nhóm "Chăm sóc" (Q-207; 1.6).** Không đổi ranh giới spec.

**(2026-09-30)** 15. **Rà soát vận hành (B3.2)** – cảnh báo "đã leo thang tối đa" được nhắc lại theo chu kỳ tới khi có người tiếp nhận (Q-248). *(Đã xử lý 2026-09-30: 9.4, BR-M05-01, 17, Q-248 ở 24.2, BF-06; FR-034, User Story 3 kịch bản 2.)*

**(2026-10-01)** 16. **Checklist cross-feature CHK080, CHK081, CHK082** – lần nhắc lại không là leo thang; khóa sự kiện theo số thứ tự; mức Nhẹ nhắc khi hết ca (Q-266, làm rõ Q-248). *(Đã xử lý 2026-10-01: 9.4, Q-266 ở 24.2; FR-034.)*

**(2026-10-01)** 17. **Đánh giá checklist business-rules** – 68 mục được đối chiếu; phần lớn đã được clarify lượt 2 và các lượt đồng bộ đáp ứng. **Một mâu thuẫn trong tài liệu nguồn cần chủ tài liệu chốt:** Phụ lục 27 dòng "Ngưỡng cảnh báo" ghi Trưởng tầng "—", trong khi 19.3 ghi Trưởng tầng xem đầy đủ hồ sơ sức khỏe trong phạm vi; spec theo 19.3 (FR-080), đề nghị sửa ô của Phụ lục 27 thành "P". Một FR suy ra chưa có trong danh sách điểm 12: thứ tự bốn cận của ngưỡng (FR-021). Chín điểm là đề xuất mới chưa có trong tài liệu nguồn, cần ghi vào 9.3 → 9.6, 10.2, 10.4 khi rà lại nguồn: (a) người nhận và người tiếp nhận của sự cố "cơ sở vật chất" (FR-040); (b) người nghi nhiễm là nhân viên hoặc người thân nằm ngoài phạm vi, sự cố lây nhiễm luôn gắn một người cao tuổi (Assumptions; chủ dự án chốt 2026-10-01); (c) người phụ trách cảnh báo khi người cao tuổi vắng mặt; hạn của cảnh báo Nhẹ tạo sát giờ hết ca (FR-031); (d) danh mục loại diễn biến có dấu "biện pháp hồi sức" (FR-048a; cần Bác sĩ đối chiếu, đề nghị thêm vào 24.4); (e) chỉ Điều dưỡng, Bác sĩ hủy hoặc đổi loại sự cố từng Khẩn cấp; chặn hủy sự cố lây nhiễm khi vùng còn chỉ gắn nó (FR-046, FR-066); (f) hạ mức khỏi Khẩn cấp thì dừng yêu cầu gọi điện (FR-044; cần đồng bộ sang spec 009); (g) không chặn tạm vắng, đón, chuyển viện với người trong vùng khoanh vùng, chỉ cảnh báo (FR-065); (h) Hành chính chỉ thấy danh sách người thân cần liên hệ; "số liên hệ của viện" do Quản lý viện cấu hình (FR-062, FR-070); (i) chạy bù của Bộ lập lịch, mỗi lần chỉ lên một cấp leo thang (FR-084). Cần đồng bộ sang spec khác: báo feature 012 khi sự cố đã nêu trong bản tin bị hủy (FR-046a). *(Đã xử lý 2026-10-01: Q-278 ở 24.2, các quy tắc mang mã BR-M05-17 → BR-M05-26; cuối mục 9.6; Phụ lục 27 dòng "Ngưỡng cảnh báo" nay ghi Trưởng tầng "P" (Q-279), FR-080 đã ghi theo ngày 2026-10-06; danh sách biện pháp hồi sức đã thêm vào 24.4; đã đồng bộ spec 009 FR-029a và spec 012 FR-056.)*

**(2026-10-05) Thay đổi do chủ dự án chốt Q-280 → Q-286.** *(Đã xử lý 2026-10-06: spec đã sửa theo các quyết định dưới đây, xem mục "Cập nhật 2026-10-06" ở Clarifications)*

- **Q-281** – bỏ cờ nguy cơ (ngã, loét, đi lạc); phần "lưu ý chăm sóc" của nhận xét đánh giá gần nhất hiện trên checklist và thẻ thông tin khẩn cấp; bỏ mọi điều kiện dựa vào cờ đi lạc (BR-M01-10). Ảnh hưởng: FR-048 (cờ nguy cơ trên thẻ thông tin khẩn cấp).
- **Q-282** – bỏ vai trò Nhân viên vệ sinh và nghiệp vụ vệ sinh (7.5, BR-M03-08, 11, 12, 14, UC-71 → 73); giường kết thúc phân bổ chuyển "Chờ sẵn sàng", Trưởng tầng hoặc Người phụ trách ca "Xác nhận giường sẵn sàng" (nghi nhiễm thì bắt buộc xác nhận đã khử khuẩn) (BR-M03-09, 10, UC-74); nhân viên không dùng hệ thống chỉ có hồ sơ nhân viên. Ảnh hưởng: FR-060, FR-062, FR-065 (khử khuẩn khoanh vùng không còn là công việc hệ thống sinh).
- **Q-283** – gộp Dinh dưỡng viên vào Bác sĩ; bản gán chế độ ăn do Bác sĩ lập nên không còn bước duyệt riêng (BR-M08-03); cột DDV của Phụ lục 27 gộp vào cột BS. Ảnh hưởng: FR-039a (Dinh dưỡng viên).

**(2026-10-06) Thay đổi do chủ dự án chốt Q-306 (hệ thống không tự sinh dữ liệu, công việc).** *(Đã xử lý 2026-10-06: spec đã viết lại, xem mục "Cập nhật 2026-10-06, lượt 4" ở Clarifications.)*

- **Q-306** – Sự cố ngã vẫn tự tạo yêu cầu đánh giá lại; công việc theo dõi sau ngã là gợi ý để Người phụ trách ca lập, và Người phụ trách ca được báo (BR-M05-07). Người tiếp xúc được gợi ý công việc đo nhiệt độ (BR-M05-10). Lựa chọn "ở lại viện chăm sóc giảm nhẹ" chỉ báo điều dưỡng phụ trách xem xét kế hoạch (9.5, BR-M04-20). Nguyên tắc chung: mọi bản ghi do actor lập bằng lệnh; hệ thống chỉ gợi ý từ dữ liệu đã có, tự chuyển trạng thái theo thời gian, cảnh báo, leo thang, nhắc, kiểm tra và thực hiện tác động kèm theo lệnh (1.3, 24.2). FR cần rà (dò theo từ khóa, chưa đối chiếu từng FR): FR-004, FR-046a, FR-047a, FR-050d, FR-051, FR-065.

**(2026-10-06) Thay đổi do chủ dự án chốt Q-307 (bỏ gợi ý, đề xuất, yêu cầu và sự cố do hệ thống tự tạo).** *(Đã xử lý 2026-10-06: spec đã viết lại, xem mục "Cập nhật 2026-10-06, lượt 4" ở Clarifications.)*

- **Q-307** – Bỏ đề xuất nâng mức: hệ thống báo số lần lặp, người tiếp nhận quyết định (BR-M05-03, CFG-M05-02). Sự cố ngã chỉ kéo theo thông báo (đánh giá lại, lập công việc theo dõi, xem xét kế hoạch), không tạo yêu cầu (BR-M05-07). Danh sách tiếp xúc do Điều dưỡng tự lập, hệ thống không đề xuất (BR-M05-10). Lệnh Chuyển viện do Điều dưỡng hoặc Bác sĩ thực hiện, hệ thống không tự thực hiện (BR-M05-14). Hệ thống không tự tạo sự cố: 9.4 bỏ nguồn "hệ thống tạo"; thiếu người khi về, phục vụ suất dị ứng chỉ tạo cảnh báo, nhân viên ghi nhận sự cố. Xác nhận lại nguyện vọng là việc gọi theo thứ tự, không phải yêu cầu (9.5, BR-M05-16, 22). Giữ: cảnh báo do hệ thống tạo và leo thang, nhắc, thông báo; bản ghi kèm lệnh của actor; số liệu tính để hiển thị (24.2).

**(2026-10-10) Plan 007, điểm báo lại 1 → 7; plan 013 điểm 1; plan 014 điểm 2, 3.** *(Đã xử lý 2026-10-10: sửa bảng FR-006, FR-045, bảng trạng thái sự cố, FR-017, FR-025, FR-046b, bảng FR-083, Key Entities; thêm FR-085, FR-086 (Q-307, Q-311, Q-330).)*

**(2026-10-11) Đối chiếu tài liệu gốc DOCX và rà soát bổ sung (báo cáo audit 2026-10-11, không lưu trong kho mã).** *(Đã xử lý 2026-10-11: Q-337 → Q-345; sửa FR-039a, FR-041, bảng FR-045, FR-045a, bảng nhóm dữ liệu, bảng quyền, bảng giao tiếp, User Story 4 kịch bản 11, Key Entities; xem mục "Cập nhật 2026-10-11" ở Clarifications.)*


**(2026-10-18) Đồng bộ Q-352, Q-354.** *(Đã xử lý 2026-10-18: Clarifications "Cập nhật 2026-10-18", FR-047, FR-047c, FR-082.)* Còn báo lại:

1. Ranh giới "phần chung / phần sức khỏe" (Q-352) là danh sách trường theo loại; khi danh mục loại sự cố có loại tự nêu bệnh (ngoài lây nhiễm), cần chủ dự án xác nhận loại đó thuộc phần nào.
2. Plan, data-model, contracts, tasks chưa cập nhật theo FR-047c, FR-082; cần /speckit-plan và /speckit-tasks. *(Đã xử lý 2026-10-18: plan, research, contracts, quickstart, tasks đã cập nhật.)*

**(2026-10-18) Đồng bộ Q-357 → Q-364.** *(Đã xử lý 2026-10-18: Clarifications "Cập nhật 2026-10-18, lượt 2".)* Đã xử lý điểm 1 của lượt 2026-10-18 theo Q-358 (tên loại sự cố thuộc phần chung, trừ lây nhiễm theo Q-72); sửa dẫn chiếu FR-047b ở FR-047.

**(2026-10-22) Đồng bộ Q-373 → Q-376.** *(Đã xử lý 2026-10-22: Clarifications "Cập nhật 2026-10-22", Phạm vi 14, User Story 12 kịch bản 11 → 13, User Story 13, FR-026 → FR-029, FR-047f, FR-047h → FR-047k, FR-048b, FR-080, FR-081, FR-083, Key Entities, SC-018 → SC-020.)* Còn báo lại:

1. Plan, data-model, contracts, quickstart, tasks chưa cập nhật theo các FR trên; cần /speckit-plan và /speckit-tasks trước khi triển khai Phase của User Story 12, 13.
2. Phần mobile (quyền vị trí, gửi GPS, nút của nhân viên, trình quay số) chờ chủ dự án mở `mobile/`; DESIGN.md mục 15 cần màn nút hỗ trợ khẩn cấp và trạng thái "đã tới".
3. Căn cứ xử lý dữ liệu vị trí và thời hạn lưu cần tư vấn pháp lý (24.4, Q-373); kết quả khác thì mở Q mới.
4. *(Q-383, Q-384)* Plan, contracts, tasks chưa cập nhật cho FR-049, FR-050, FR-050h; nếu viện có giấy phép khám chữa bệnh, việc lập giấy chuyển cơ sở theo mẫu Bộ Y tế chờ đối chiếu pháp lý (24.4).
