# Feature Specification: Chăm sóc hằng ngày

**Feature Branch**: `005-daily-care-tasks`

**Created**: 2026-09-25

**Status**: Draft

**Input**: User description: "Quản lý chăm sóc hằng ngày theo docs/nghiep-vu.md Module 04 mục 8.1–8.7: kế hoạch chăm sóc theo phiên bản có ngày hiệu lực và phải được duyệt; hệ thống tự sinh công việc theo ca từ kế hoạch, lịch đo, lịch ăn, hoạt động; mỗi công việc có khung thời gian, mức quan trọng và vòng đời trạng thái; checklist theo ca cho từng nhân viên; ghi nhận kết quả theo loại công việc; công việc quá hạn được nhắc, leo thang và đưa vào bàn giao; kết quả ghi nhận kích hoạt quy tắc (uống nước thiếu, ăn kém kéo dài). Bán trú chỉ sinh công việc khi có mặt."

## Clarifications

### Session 2026-09-25

- Q: Nếu công việc đã Quá hạn nhưng bản ghi (ghi muộn hoặc đồng bộ sau mất kết nối) có thời điểm thực hiện trong khung thời gian, công việc là Hoàn thành hay Hoàn thành trễ? → A: Hoàn thành, xét theo thời điểm thực hiện; gắn nhãn "ghi nhận muộn" nếu chênh quá CFG-M04-06; nhắc và thông báo đã gửi giữ trong lịch sử, cảnh báo đang mở được đóng qua feature 007 (bổ sung chuyển Quá hạn → Hoàn thành, đề xuất đưa vào 8.3 và Q-01).
- Q: Mục tiêu lượng nước mỗi ngày lấy từ đâu, và áp thế nào cho bán trú hoặc người vắng một phần ngày? → A: Lấy từ mục tiêu ml/ngày của mục kế hoạch "hỗ trợ uống nước" trong phiên bản Hiệu lực; người bán trú và người vắng một phần ngày so với mục tiêu tính theo tỷ lệ số giờ có mặt từ đầu ngày tới mốc kiểm tra; không có mục tiêu thì không kiểm tra (đề xuất Q-30).
- Q: Với công việc Thường quá hạn mà người thực hiện chưa xử lý, khi nào báo người phụ trách ca? → A: Sau khoảng chờ tính từ lúc Quá hạn theo tham số mới CFG-M04-12, mặc định \[30 phút\] (đề xuất Q-31).
- Q: Công việc Quá hạn qua nhiều ca mà không ai đóng có được hệ thống tự đóng không? → A: Chỉ công việc Thường: tự chuyển Không thực hiện (lý do "hệ thống đóng sau bàn giao") ngay khi bàn giao đầu tiên chứa nó ở trạng thái Quá hạn được ca sau xác nhận; công việc Quan trọng, Bắt buộc không tự đóng (đề xuất Q-32).
- Q: Bác sĩ và Dinh dưỡng viên có được xem kết quả ghi nhận chăm sóc không? → A: Chỉ xem, không ghi: Bác sĩ xem mọi kết quả ghi nhận; Dinh dưỡng viên chỉ xem kết quả ăn uống và lượng nước; đề xuất bổ sung quyền "X" có giới hạn vào Permission Matrix 4.4 (báo lại, chưa sửa tài liệu nguồn; đề xuất Q-33).
- Q: Phiên bản kế hoạch được duyệt với ngày hiệu lực là hôm nay có được áp dụng ngay trong ngày không? → A: Không; ngày hiệu lực sớm nhất là ngày hôm sau ngày duyệt, DBR-11 giữ nguyên; việc gấp trong ngày dùng công việc phát sinh (FR-025) (đề xuất Q-34).
- Q: Người phụ trách ca (không phải trưởng tầng) được ghi nhận thay công việc nào? → A: Chỉ công việc Quá hạn trong tầng và thời gian ca (đúng spec 002 FR-019a); công việc chưa Quá hạn thì chỉ phân lại hoặc nhận việc về mình rồi ghi như người được giao. Hệ quả: hủy công việc Chưa đến hạn chỉ do Trưởng tầng (8.3) (đề xuất Q-35).
- Q: Công việc phát sinh do nhân viên tự tạo để ghi việc vừa làm xong đi qua trạng thái nào? → A: Dùng lệnh "Ghi nhận phát sinh": tạo và ghi kết quả trong một lệnh, vào thẳng Hoàn thành; thời điểm thực hiện không sau thời điểm ghi; nhãn ghi muộn áp như thường (đề xuất Q-36).
- Q: Trạng thái Quá hạn dùng để tự đóng công việc Thường sau bàn giao được xét vào lúc nào? → A: Tại thời điểm ca sau xác nhận bàn giao; công việc phải vừa có trong nội dung bàn giao vừa đang Quá hạn lúc xác nhận; việc đã đóng trước đó giữ kết quả của người đóng (đề xuất Q-37, làm rõ Q-32).
- Q: Khi người cao tuổi chuyển trạng thái cuối, công việc Đến hạn/Quá hạn có được hệ thống tự đóng không? → A: Có, chỉ với trạng thái cuối (Kết thúc lưu trú, Qua đời, Hủy tiếp nhận): tự chuyển Không thực hiện với lý do trạng thái cuối và đóng cảnh báo quá hạn liên quan; vắng mặt tạm thời vẫn đóng tay (FR-027) (đề xuất Q-38).

### Cập nhật 2026-09-26 (bổ sung nghiệp vụ vệ sinh vào tài liệu nguồn)

Tài liệu nguồn thêm mục 7.5 (vệ sinh phòng và khu vực, Module 03), thêm "lịch vệ sinh (BR-M03-08)" vào nguồn sinh công việc của BR-M04-01, làm rõ ở 8.3 rằng "vệ sinh phòng" là công việc vệ sinh của Module 03, và mở rộng BR-M04-23 cho công việc vệ sinh. Spec cập nhật: Phạm vi mục 4, Ngoài phạm vi, FR-017, FR-024. Nguồn sinh, loại và tác động lên giường của công việc vệ sinh do feature 003 (mục H) quy định; spec này cung cấp vòng đời, checklist và ghi nhận.

### Cập nhật 2026-09-26 (đồng bộ với spec 008)

Spec 008 đã chốt khi clarify: (Q-85) từ giờ bắt đầu ca sau mà bàn giao chưa được xác nhận, công việc chưa đóng của người ca trước đã hết ca tạm thành công việc chung của tầng (thêm FR-047a); (Q-81) "vắng ca" ở FR-032 chỉ được ghi nhận từ giờ bắt đầu ca trừ CFG-M15-07 tới hết ca, vắng biết trước đi qua yêu cầu nghỉ đột xuất của feature 015; (Q-91) một lần vắng ca chỉ tác động lên ca đó, "các ca tương lai" ở FR-032 chỉ áp cho Nghỉ việc (FR-032 được sửa); (Q-93) công việc Thường Quá hạn đã được nhận trong lúc chờ xác nhận không bị tự đóng theo FR-047 (FR-047a được bổ sung).

### Cập nhật 2026-09-26 (đồng bộ với spec 009)

Spec 009 đã chốt mức cho các thông báo của spec này (spec 009 FR-043b): nhắc công việc quá hạn cho người thực hiện là Nhẹ (FR-043, Q-117); báo leo thang công việc Thường, Quan trọng quá hạn (FR-044), vắng ca/nghỉ việc (FR-032), kế hoạch Chờ duyệt quá hạn (FR-005) là Trung bình; yêu cầu xem xét kế hoạch (FR-011) và thông báo tới Dinh dưỡng viên khi ăn kém kéo dài nay do feature 011 gửi khi tạo yêu cầu xem lại chế độ ăn (mức Nhẹ, feature 011 FR-058). Người bán trú còn "Có mặt" lúc hết ngày (FR-019) được báo Trung bình cho Người phụ trách ca đang diễn ra của tầng và Nhẹ cho Trưởng tầng (Q-115); FR-019 được sửa người nhận cho khớp. Leo thang của FR-044 là yêu cầu gửi spec 009 với cách thay mặc định (spec 009 FR-010).

### Cập nhật 2026-09-26 (đồng bộ với spec 012)

Spec 012 đã chốt khi clarify: (1) Q-121 — quy trình đón khi điểm danh về do Hành chính hoặc Nhân viên chăm sóc thực hiện, khớp bảng trạng thái có mặt của spec này. (2) Q-125 — người bán trú có dấu "được tự về" có tác dụng được điểm danh về không cần người đón; feature 012 vẫn tạo bản ghi đón loại "Tự về" với nhân viên tiễn. Dòng "Có mặt → Điểm danh về" của bảng trạng thái có mặt và User Story 2 kịch bản 3 được sửa cho khớp.

### Cập nhật 2026-09-27 (đồng bộ với spec 010)

Spec 010 và tài liệu nguồn 3.4 đã chốt Q-140:
- Người bán trú được điểm danh đến cả vào ngày không có lịch (buổi phát sinh).
- Buổi có lịch trùng ngày khu bán trú nghỉ (feature 004 FR-060a) không chuyển Vắng không báo.

Bảng trạng thái có mặt (FR-013) thêm hai dòng tương ứng, và FR-016 nêu rõ các trạng thái gửi cho feature 010. Theo FR-025, ngày phát sinh chi phí của kết quả công việc là thời điểm thực hiện.

### Cập nhật 2026-09-27 (đồng bộ với spec 011)

Spec 011 và tài liệu nguồn (BR-M04-09 làm rõ, BR-M08-05, BR-M08-14) đã chốt:
- Khi ăn kém kéo dài, spec này gửi sự kiện cho feature 011 để tạo yêu cầu xem lại chế độ ăn; không gửi nhắc riêng tới Dinh dưỡng viên (FR-040).
- Kết quả ăn uống của bữa có suất đặc biệt, hoặc của người thuộc danh sách cần đối chiếu khi phục vụ, chỉ ghi được sau khi feature 011 ghi nhận xác nhận phục vụ (FR-040a mới).
- Nhận lịch bữa và bữa bị bỏ vì vắng mặt từ feature 011 (bảng giao tiếp).

### Cập nhật 2026-09-27 (đồng bộ với spec 014)

Spec 014 và tài liệu nguồn (3.4, 8.8, BR-M04-04, BR-M04-23, Q-163, Q-169, Q-170, Q-176) đã chốt:
- Spec này sở hữu giờ về dự kiến theo ngày của bán trú; giờ này dời khi có đồng ý về muộn vì hoạt động và là nguồn cho feature 003, 010, 011 (FR-018a mới).
- Đính chính thời điểm rời/về hoặc Hủy ghi nhận điểm danh chuyến đi làm công việc được tính lại theo FR-020 (FR-020a mới).
- Kết quả kiểm tra chất lượng Không đạt sinh công việc làm lại trong chính ca của danh sách kiểm tra (FR-026a mới).
- Feature 014 báo buổi kết thúc lúc hết ngày (đăng ký "Không ghi nhận") để xử lý công việc "hoạt động" liên quan theo vòng đời thường. Công việc trong khoảng Hoạt động bên ngoài vẫn bị Hủy theo FR-020; 5.6 đã được sửa cho khớp.

### Cập nhật 2026-09-27 (đồng bộ với spec 015)

Spec 015 thêm yêu cầu nghỉ đột xuất có duyệt và đổi ca. FR-032 nhận thêm nguồn kích hoạt "nghỉ có duyệt" (chỉ các ca được duyệt nghỉ, như vắng ca theo Q-91); phân công không chuyển được khi đổi ca đi theo quy tắc kết thúc phân công của feature 008 FR-027. Trưởng tầng được thông báo như các trường hợp khác của FR-032.

### Cập nhật 2026-09-28 (đồng bộ với spec 016, rà chéo)

Spec 016 (báo cáo và dashboard) dùng dữ liệu công việc của spec này; bảng giao tiếp có thêm dòng "Gửi 016". Spec 016 FR-030, FR-031 chia công việc đóng bởi "Hệ thống" theo đúng danh mục quy tắc của FR-028 (FR-010, FR-047, hủy tự động); công việc Không thực hiện do hệ thống đóng không vào mẫu số tỷ lệ đúng hạn; công việc từng Quá hạn nhưng được ghi Hoàn thành theo thời điểm thực hiện vẫn tính là "đã từng quá hạn" nhưng đúng hạn. Không có thay đổi về hành vi của spec này.

### Session 2026-10-01 (đánh giá checklist business-rules)

Các điểm dưới đây do nhóm phân tích quyết định khi đánh giá checklist business-rules; không đổi quy tắc ở tài liệu nguồn, trừ các đề xuất nêu ở mục "Điểm cần báo lại".

- Q: "Kiểm tra đầu ca" và "giám sát" của điều dưỡng (8.5) là gì? → A: Hai loại công việc trong danh mục, sinh một lần mỗi ca cho điều dưỡng có phân công trong ca, gắn tầng/khu vực, khung bằng cả ca, kết quả "Đã thực hiện" kèm ghi chú (CHK003).
- Q: Công việc Quá hạn được nhắc một lần hay lặp lại? → A: Một lần khi chuyển Quá hạn; sau đó là các bước leo thang của FR-044 (CHK004).
- Q: Đổi mức chăm sóc bằng phụ lục hợp đồng có tạo yêu cầu xem xét kế hoạch không? → A: Có, là nguồn thứ ba của FR-011, cho khớp feature 004 FR-042; cờ nguy cơ mới chỉ phát sinh từ đánh giá nên đã nằm trong nguồn "đánh giá lại" (CHK007).
- Q: CFG-M04-01 "00:00, hoặc trước mỗi ca 1 giờ" hiểu thế nào? → A: Hai chế độ loại trừ nhau do cơ sở chọn; mặc định là 00:00, sinh cho cả ngày (CHK010).
- Q: Người cao tuổi bỏ bữa thì ghi thế nào? → A: Ghi kết quả "Bỏ bữa" (công việc Hoàn thành, tính vào chuỗi ăn kém). "Không thực hiện" chỉ dùng khi không hỗ trợ hay quan sát được bữa đó; khi ấy bữa không có kết quả, không tính và không làm đứt chuỗi (CHK014).
- Q: Yêu cầu xem xét kế hoạch giao cho điều dưỡng nào khi qua nhiều ca? → A: Giao theo nhiệm vụ Điều dưỡng phụ trách: điều dưỡng đang có người cao tuổi đó trong phạm vi ở ca hiện tại đều thấy và xử lý được (CHK022).
- Q: Phiên bản kế hoạch mới có hiệu lực khi công việc cùng loại của phiên bản cũ còn Đến hạn hoặc Quá hạn? → A: Không sinh công việc mới có thời điểm dự kiến rơi vào khung của công việc cũ chưa đóng đó (CHK039).
- Q: Đổi phân công chăm sóc giữa ca? → A: Công việc chưa đóng của người cao tuổi trong ca được gán lại cho người mới; công việc đã đóng giữ nguyên (CHK041).
- Q: Kết quả bị "Hủy ghi nhận" thì công việc ở trạng thái nào? → A: Giữ trạng thái đóng; kết quả không còn được tính vào chuỗi hay tổng; cần làm lại thì tạo công việc phát sinh (CHK038).
- Q: Ghi muộn hoặc đồng bộ ngoại tuyến cho công việc đã bị hệ thống đóng vì trạng thái cuối? → A: Chấp nhận dưới dạng "Ghi nhận phát sinh" trỏ về công việc gốc, nếu thời điểm thực hiện trước thời điểm chuyển trạng thái cuối (CHK056).
- Q: Bộ lập lịch bỏ lỡ lần chạy? → A: Chạy bù khi hoạt động lại; công việc được sinh bù, trạng thái xét theo đồng hồ lúc chạy; nhắc gộp một lần mỗi người (CHK034).
- Q: Mất kết nối thì checklist và công việc thay đổi trong lúc đó xử lý thế nào? → A: Thiết bị hiện checklist đã tải kèm dấu "chưa cập nhật"; khi đồng bộ, bản ghi cho công việc đã đóng hoặc đã Hủy xử lý như FR-036 (CHK044).

## Phạm vi

**Trong phạm vi** (Module 04, mục 8.1 → 8.7; UC-22 → UC-28):

1. Kế hoạch chăm sóc theo phiên bản: lập, gửi duyệt, duyệt, có hiệu lực theo ngày, hết hiệu lực; mỗi mục kế hoạch có khung thời gian, mức quan trọng, có tính phí (8.1, UC-22, UC-23, BR-M04-19, DBR-11).
2. Yêu cầu xem xét kế hoạch chăm sóc sau đánh giá lại hoặc sự cố ngã (BR-M04-20).
3. Danh mục loại công việc: vai trò thực hiện, chứng chỉ yêu cầu, loại kết quả cần ghi, mức quan trọng và khung thời gian mặc định (8.3, 8.6, 13.4).
4. Sinh công việc theo ngày/ca từ kế hoạch chăm sóc, lịch đo, lịch ăn, hoạt động đã đăng ký, lịch vệ sinh (BR-M03-08); hủy và sinh lại khi nguồn thay đổi hoặc người cao tuổi vắng mặt (UC-24, BR-M04-01 → 04, DBR-12).
5. Trạng thái có mặt theo ngày của bán trú và điểm danh đến/về làm căn cứ sinh công việc (3.4, UC-28, BR-M04-02).
6. Vòng đời công việc: Chưa đến hạn → Đến hạn → Hoàn thành / Quá hạn → Hoàn thành trễ / Không thực hiện / Hủy (8.3).
7. Checklist theo ca cho từng nhân viên (8.5, UC-25) và công việc chung của tầng (8.4, BR-M04-13).
8. Ghi nhận kết quả theo loại công việc, ghi nhận muộn, đính chính (8.6, UC-26, BR-M04-12, 14).
9. Công việc quá hạn: nhắc, báo người phụ trách ca, cảnh báo theo mức quan trọng, xử lý và đưa vào bàn giao (8.7, UC-27, BR-M04-05 → 07).
10. Quy tắc kích hoạt từ kết quả: uống nước thiếu, ăn kém kéo dài, chỉ số (chuyển sang so ngưỡng), tâm trạng tiêu cực kéo dài (BR-M04-08 → 11).
11. Thời khóa biểu cá nhân theo ngày, tổng hợp từ công việc và lịch của các module khác (8.2) – chỉ xem.

**Ngoài phạm vi** (spec này chỉ **cung cấp** dữ liệu hoặc **được kích hoạt** bởi feature sở hữu):

- Quy tắc dùng chung (nhóm dữ liệu, lý do bắt buộc, đính chính, vòng đời yêu cầu phê duyệt, tự duyệt, nhật ký, tham số, "hoặc toàn bộ, hoặc không"): feature 000 — spec này kế thừa, không lặp lại.
- Tập trạng thái người cao tuổi (Đang lưu trú, Tạm vắng, Điều trị tại bệnh viện, Hoạt động bên ngoài, trạng thái cuối) và đánh giá lại: feature 001. Hoạt động mẫu từ đánh giá được chép vào bản nháp kế hoạch (feature 001 FR-031).
- Quyền, phạm vi dữ liệu theo phân công, quyền của Người phụ trách ca (FR-019a, FR-019b) và kiểm tra quyền của bản ghi ngoại tuyến: feature 002.
- Lịch đến của bán trú trong hợp đồng, ghi nhận báo vắng và tính phí theo trạng thái có mặt: feature 004. Chuyển giường kích hoạt gán lại công việc: feature 003 FR-031.
- Lịch vệ sinh, nguồn sinh và loại công việc vệ sinh (định kỳ, trả giường, khử khuẩn, đột xuất), kết quả theo hạng mục, hư hỏng và tác động lên trạng thái giường (7.5, BR-M03-08 → 14): feature 003 mục H. Spec này áp vòng đời công việc (8.3), checklist, quá hạn và ghi nhận cho công việc vệ sinh như mọi công việc khác, và báo cho feature 003 khi công việc vệ sinh trả giường/khử khuẩn Hoàn thành.
- Đơn thuốc, liều thuốc và trạng thái liều (Module 07): feature 006 — liều chỉ được **hiển thị** chung trong checklist, không sinh công việc trùng.
- Lịch đo, ngưỡng, lưu và so ngưỡng giá trị chỉ số (Module 06); cảnh báo, leo thang cảnh báo, sự cố (Module 05): feature 007.
- Hồ sơ nhân viên, chứng chỉ, lịch ca, phân công chăm sóc, bàn giao ca (Module 09): feature 008. Spec này cung cấp danh sách công việc chưa đóng cho bản nháp bàn giao và nhận sự kiện "bàn giao đã xác nhận".
- Gửi thông báo (Module 13): feature 009. Sinh chi phí nháp từ công việc có tính phí và vật phẩm đã dùng: feature 010.
- Chế độ ăn, thực đơn, lịch bữa, chốt suất ăn (Module 08): feature 011.
- Người thân, quy trình đón khi điểm danh về (14.3), lịch thăm: feature 012.
- Hoạt động, hoạt động ngoài viện, sở thích và theo dõi tinh thần (8.8 → 8.10), kiểm tra chất lượng ngẫu nhiên (BR-M04-23): feature 014.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Điều dưỡng lập phiên bản kế hoạch chăm sóc, bác sĩ duyệt, có hiệu lực từ một ngày (Priority: P1)

Điều dưỡng lập bản nháp kế hoạch chăm sóc cho người cao tuổi (từ hoạt động mẫu của đánh giá hoặc sao chép phiên bản đang hiệu lực), khai báo từng mục: loại công việc, nội dung, tần suất, mốc giờ, khung thời gian cho phép, vai trò thực hiện, kết quả cần ghi, mức quan trọng, có tính phí. Điều dưỡng gửi duyệt kèm ngày hiệu lực đề xuất; người có quyền Duyệt kế hoạch chăm sóc duyệt hoặc trả lại. Phiên bản đã duyệt có hiệu lực từ ngày hiệu lực; phiên bản trước tự hết hiệu lực. Phiên bản đang hiệu lực không sửa được.

**Why this priority**: Kế hoạch chăm sóc là nguồn chính sinh công việc (BR-M04-01); không có kế hoạch hiệu lực thì các story còn lại không có dữ liệu đầu vào.

**Independent Test**: Lập bản nháp 3 mục, gửi duyệt, bác sĩ duyệt với ngày hiệu lực ngày mai; kiểm tra hôm nay phiên bản cũ vẫn hiệu lực, ngày mai phiên bản mới hiệu lực và phiên bản cũ Hết hiệu lực; thử sửa phiên bản đang hiệu lực và kiểm tra bị chặn.

**Acceptance Scenarios**:

1. **Given** bác sĩ chấp nhận đánh giá đầu vào của A với các hoạt động mẫu (feature 001 FR-031), **When** hệ thống lưu, **Then** A có một phiên bản kế hoạch ở trạng thái Nháp chứa các hoạt động mẫu; mục nào thiếu khung thời gian thì nhận mặc định CFG-M04-02 (mặc định \[±15 phút\]).
2. **Given** bản nháp của A có mục "xoay trở mỗi 2 giờ, Bắt buộc" và "hỗ trợ uống nước 3 giờ/lần, mục tiêu 1.500 ml/ngày, Quan trọng", **When** điều dưỡng gửi duyệt với ngày hiệu lực 01/10, **Then** phiên bản chuyển Chờ duyệt và người có quyền Duyệt kế hoạch chăm sóc trong phạm vi nhận thông báo.
3. **Given** phiên bản Chờ duyệt, **When** bác sĩ duyệt ngày 28/09, **Then** phiên bản chuyển Chờ hiệu lực, ghi người lập, người duyệt, thời điểm duyệt, lý do thay đổi; **When** tới 00:00 ngày 01/10, **Then** phiên bản chuyển Hiệu lực, phiên bản trước chuyển Hết hiệu lực với ngày kết thúc 30/09 (DBR-11), và việc sinh lại công việc theo BR-M04-03 được kích hoạt.
4. **Given** phiên bản Chờ duyệt, **When** bác sĩ trả lại kèm lý do, **Then** phiên bản về Nháp, người lập nhận thông báo kèm lý do; **When** bác sĩ trả lại mà không nhập lý do, **Then** hệ thống chặn.
5. **Given** phiên bản đang Hiệu lực, **When** bất kỳ ai sửa một mục của phiên bản đó, **Then** hệ thống chặn và gợi ý tạo phiên bản mới (BR-M04-19).
6. **Given** điều dưỡng N được Quản lý viện gán quyền Duyệt kế hoạch chăm sóc (Q-15), **When** N duyệt phiên bản do chính N lập, **Then** hệ thống cho phép, bắt buộc lý do và nhật ký đánh dấu "tự duyệt" (Q-10); **Given** điều dưỡng M không có quyền này, **When** M duyệt, **Then** hệ thống chặn.
7. **Given** phiên bản Chờ duyệt có ngày hiệu lực đề xuất 26/09, **When** bác sĩ duyệt ngày 26/09, **Then** hệ thống chặn và yêu cầu chọn ngày hiệu lực từ 27/09 trở đi, vì phiên bản mới không có hiệu lực trong ngày duyệt (FR-003, DBR-11).
8. **Given** phiên bản Chờ duyệt của A, **When** người lập hủy phiên bản kèm lý do, **Then** phiên bản chuyển Đã hủy và A có thể lập phiên bản mới; **Given** phiên bản Chờ hiệu lực từ 01/10, **When** bác sĩ thu hồi duyệt ngày 29/09 kèm lý do, **Then** phiên bản chuyển Đã hủy, phiên bản đang Hiệu lực vẫn áp sau 01/10 và người lập được thông báo; **When** bác sĩ thu hồi ngày 01/10, **Then** hệ thống chặn vì đã tới ngày hiệu lực (bảng FR-003).
9. **(Bổ sung, CHK033)** **Given** A có một phiên bản Hiệu lực và một phiên bản Chờ hiệu lực từ 05/10, **When** A chuyển Kết thúc lưu trú ngày 02/10, **Then** trong cùng lệnh phiên bản Hiệu lực chuyển Hết hiệu lực, phiên bản Chờ hiệu lực chuyển Đã hủy với lý do là trạng thái cuối, và không có công việc nào được sinh thêm (bảng FR-003, FR-010).

---

### User Story 2 - Hệ thống tự sinh công việc theo ca, chỉ cho người đang hoặc sẽ có mặt (Priority: P1)

Vào thời điểm CFG-M04-01, hệ thống sinh công việc cho ngày/ca tới từ phiên bản kế hoạch đang hiệu lực, lịch đo, lịch ăn và hoạt động đã đăng ký. Mỗi công việc có thời điểm dự kiến, khung thời gian cho phép, mức quan trọng, vai trò thực hiện và người thực hiện theo phân công. Công việc không bị sinh trùng khi chạy lại. Khi kế hoạch thay đổi hoặc người cao tuổi vắng mặt, chỉ công việc Chưa đến hạn bị hủy/sinh lại; công việc đã thực hiện không đổi.

**Why this priority**: "Hệ thống chủ động sinh công việc" (1.3) là giá trị cốt lõi của module; checklist và ghi nhận đều dựa trên công việc được sinh.

**Independent Test**: Với 3 người cao tuổi (nội trú có kế hoạch, nội trú Tạm vắng, bán trú), chạy sinh công việc cho một ngày; so số công việc với số tính tay; chạy lại lần hai và kiểm tra không có bản trùng.

**Acceptance Scenarios**:

1. **Given** A Đang lưu trú có mục "xoay trở mỗi 2 giờ từ 06:00" và ca ngày 07:00–18:00, **When** tới thời điểm sinh công việc CFG-M04-01 (mặc định \[00:00 hoặc trước ca 1 giờ\]), **Then** hệ thống sinh các công việc xoay trở lúc 08:00, 10:00, 12:00, 14:00, 16:00 cho ca ngày, mỗi công việc có khung ±15 phút, mức Bắt buộc, người thực hiện theo phân công chăm sóc của A trong ca.
2. **Given** công việc đã được sinh cho ca, **When** việc sinh chạy lại lần nữa cho cùng ca, **Then** không có công việc mới trùng (mục kế hoạch, thời điểm dự kiến) (DBR-12).
3. **Given** B đang Tạm vắng lúc sinh công việc, **When** sinh công việc, **Then** B không có công việc; **When** B được Ghi nhận trở về lúc 14:20, **Then** hệ thống sinh công việc cho B có thời điểm dự kiến từ 14:20 tới hết ca hiện tại và ca kế tiếp đã tới thời điểm sinh.
4. **Given** C Đang lưu trú có công việc Chưa đến hạn lúc 15:00 và 17:00, **When** C chuyển Điều trị tại bệnh viện lúc 14:00, **Then** hai công việc chuyển Hủy với lý do "vắng mặt" (BR-M04-04).
5. **Given** phiên bản mới của kế hoạch A có hiệu lực từ 01/10 và công việc ngày 01/10 đã được sinh từ phiên bản cũ lúc 00:00, **When** phiên bản mới có hiệu lực, **Then** chỉ công việc ở Chưa đến hạn từ ngày hiệu lực bị Hủy (lý do "thay đổi kế hoạch") và được sinh lại theo phiên bản mới; công việc đã Hoàn thành, Không thực hiện hoặc đang Đến hạn/Quá hạn giữ nguyên (BR-M04-03).
6. **Given** bác sĩ đặt lịch đo huyết áp 2 lần/ngày cho A (feature 007) và lịch ăn 3 bữa (feature 011), **When** sinh công việc, **Then** có công việc "đo chỉ số" và "hỗ trợ ăn" tương ứng; liều thuốc của A không tạo công việc mà được hiển thị từ feature 006.
7. **Given** A được đăng ký buổi thể dục 08:00 thứ 2 (feature 014), **When** sinh công việc ngày thứ 2, **Then** có công việc "hoạt động" gắn buổi đó; **When** đăng ký bị hủy trước giờ diễn ra, **Then** công việc đang Chưa đến hạn chuyển Hủy.
8. **Given** A có công việc Đến hạn lúc 09:00, một công việc Bắt buộc Quá hạn lúc 08:00 kèm cảnh báo trung bình đang mở, và bản nháp bàn giao ca đêm chưa lập, **When** A được ghi nhận qua đời lúc 09:10 (feature 004), **Then** trong cùng lệnh công việc Chưa đến hạn chuyển Hủy, hai công việc kia chuyển Không thực hiện với lý do trạng thái cuối và người đóng là Hệ thống, feature 007 nhận yêu cầu đóng cảnh báo, không có nhắc hay leo thang tiếp, và các công việc này không xuất hiện trong bàn giao hay checklist ca sau (FR-010, FR-028).

---

### User Story 3 - Người bán trú chỉ có công việc trong thời gian có mặt (Priority: P1)

Mỗi ngày có lịch đến, người bán trú có trạng thái có mặt. Công việc chỉ được sinh khi điểm danh đến và chỉ cho khoảng từ lúc đến tới giờ về dự kiến; khi điểm danh về, công việc sau giờ về bị hủy. Quá giờ đến dự kiến mà chưa điểm danh và không có báo vắng thì chuyển Vắng không báo.

**Why this priority**: 3.3 và BR-M04-02 là quy tắc bắt buộc; sinh công việc cho người không có mặt tạo việc ảo, báo quá hạn sai và chi phí sai.

**Independent Test**: Người bán trú E có lịch 07:30–16:30; kiểm tra lúc 00:00 không có công việc; điểm danh đến 08:10, kiểm tra công việc được sinh từ 08:10; điểm danh về 15:00, kiểm tra công việc sau 15:00 bị Hủy.

**Acceptance Scenarios**:

1. **Given** E (bán trú) có lịch đến hôm nay 07:30–16:30, **When** tới thời điểm sinh công việc, **Then** E có trạng thái có mặt "Chưa đến" và không có công việc nào (BR-M04-02).
2. **Given** E Chưa đến, **When** nhân viên chăm sóc điểm danh đến lúc 08:10, **Then** trạng thái có mặt chuyển "Có mặt", ghi giờ đến và người điểm danh; hệ thống sinh công việc của E có thời điểm dự kiến từ 08:10 tới 16:30; công việc có khung đã kết thúc trước 08:10 không được sinh.
3. **Given** E Có mặt, còn công việc Chưa đến hạn lúc 15:30, **When** E được điểm danh về lúc 15:00 qua quy trình đón hoặc "Tự về" (feature 012), **Then** trạng thái có mặt chuyển "Đã về" và công việc lúc 15:30 chuyển Hủy (lý do "đã về"); công việc đang Đến hạn hoặc Quá hạn vẫn giữ để nhân viên đóng.
4. **Given** E Chưa đến và không có báo vắng, **When** quá giờ đến dự kiến CFG-M02-07 (mặc định \[2 giờ\]), **Then** trạng thái chuyển "Vắng không báo" và trưởng tầng được thông báo; **When** E đến lúc 10:00 sau đó, **Then** điểm danh đến vẫn được thực hiện, trạng thái chuyển "Có mặt" và công việc được sinh từ 10:00.
5. **Given** hành chính đã ghi nhận báo vắng cho E trước CFG-M02-06 (feature 004), **When** tới ngày đó, **Then** trạng thái có mặt là "Vắng có báo" và không có công việc nào.

---

### User Story 4 - Nhân viên nhận checklist theo ca và ghi nhận kết quả theo loại công việc (Priority: P1)

Đầu ca, mỗi nhân viên thấy checklist gồm công việc chăm sóc, công việc chuyên môn, công việc phát sinh, công việc tồn từ ca trước, và công việc chung của tầng; điều dưỡng thấy thêm liều thuốc, nhận bàn giao, xử lý cảnh báo, lập bàn giao. Nhân viên mở công việc, thực hiện, nhập kết quả đúng loại (ml, mức ăn, giá trị đo, mức vận động…) và hoàn thành.

**Why this priority**: Đây là thao tác hằng ngày của phần lớn nhân viên (BF-02 bước 3); ghi nhận là đầu vào của mọi quy tắc cảnh báo, chi phí và bàn giao.

**Independent Test**: Đăng nhập với một nhân viên chăm sóc được phân công 5 người trong ca ngày; kiểm tra checklist hiển thị đúng các công việc của 5 người đó cùng công việc chung của tầng; ghi nhận một công việc uống nước 200 ml và một công việc ăn "một phần"; kiểm tra trạng thái và dữ liệu lưu.

**Acceptance Scenarios**:

1. **Given** nhân viên chăm sóc S được phân công A, B, C trong ca ngày tầng 2, **When** S mở checklist, **Then** S thấy công việc của A, B, C trong ca, công việc chung của tầng 2 mà vai trò của S thực hiện được, công việc tồn từ ca trước được đánh dấu "tồn"; mỗi công việc hiện người cao tuổi, phòng/giường, thời điểm dự kiến, khung thời gian, mức quan trọng, trạng thái và cờ nguy cơ của người cao tuổi (feature 001 FR-038).
2. **Given** điều dưỡng D phụ trách tầng 2 trong ca, **When** D mở checklist, **Then** D thấy thêm liều thuốc của người được phân công (feature 006), việc nhận bàn giao đầu ca, cảnh báo đang mở cần xử lý (feature 007) và việc lập bàn giao cuối ca (feature 008).
3. **Given** công việc "hỗ trợ uống nước" của A Đến hạn, **When** S ghi nhận 200 ml với thời điểm thực hiện trong khung, **Then** công việc chuyển Hoàn thành, lưu người thực hiện, thời điểm thực hiện, thời điểm ghi, người cao tuổi, công việc, kết quả, ghi chú (8.6).
4. **Given** công việc "hỗ trợ ăn" bữa trưa của B, **When** S chọn kết quả, **Then** chỉ chọn được một trong: Ăn hết / Phần lớn / Một phần / Không ăn / Bỏ bữa; **When** S nhập giá trị không thuộc danh sách, **Then** hệ thống chặn.
5. **Given** công việc "thay tã" có tính phí của C, **When** S ghi nhận số lượng 2, **Then** công việc Hoàn thành và feature 010 nhận yêu cầu tạo chi phí nháp trỏ về kết quả ghi nhận này (DBR-15).
6. **Given** công việc "đo chỉ số" huyết áp của A, **When** S ghi giá trị đo, **Then** giá trị được lưu thành bản ghi chỉ số ở feature 007 và được so ngưỡng ngay (BR-M04-10); công việc Hoàn thành và trỏ tới bản ghi chỉ số đó.
7. **Given** công việc của A được giao cho S, **When** nhân viên T không được phân công A và không phải trưởng tầng ghi nhận công việc đó, **Then** hệ thống chặn (BR-M04-12); **Given** P giữ nhiệm vụ Người phụ trách ca tầng 2 (không phải trưởng tầng), **When** P ghi nhận thay công việc Đến hạn của S, **Then** hệ thống chặn và gợi ý phân lại hoặc nhận việc; **When** công việc đó đã Quá hạn, **Then** P ghi nhận thay được và bản ghi lưu căn cứ là nhiệm vụ Người phụ trách ca (FR-033).
8. **Given** S thực hiện lúc 09:00 nhưng tới 11:30 mới ghi nhận, **When** lưu, **Then** bản ghi được gắn nhãn "ghi nhận muộn" vì chênh quá CFG-M04-06 (mặc định \[2 giờ\]) (BR-M04-12).
9. **Given** B có các bữa và lượng nước đã ghi, **When** dinh dưỡng viên mở lịch sử ghi nhận của B, **Then** chỉ thấy kết quả ăn uống và lượng nước, không có thao tác ghi; **When** bác sĩ mở, **Then** thấy mọi kết quả ghi nhận của B, không có thao tác ghi hay đính chính (FR-049).
10. **Given** công việc của B Đến hạn và B từ chối vận động, **When** S chọn "Không thực hiện" mà không nhập lý do, **Then** hệ thống chặn; **When** S nhập lý do "người cao tuổi từ chối", **Then** công việc chuyển Không thực hiện.
11. **Given** A vừa uống thêm 150 ml nước ngoài lịch lúc 13:10, **When** S dùng "Ghi nhận phát sinh" loại uống nước với thời điểm thực hiện 13:10, **Then** một công việc phát sinh giao cho S được tạo ở trạng thái Hoàn thành cùng kết quả 150 ml, và lượng này được cộng vào tổng lượng nước của A cho quy tắc FR-039; **When** S chọn thời điểm thực hiện sau thời điểm ghi, **Then** hệ thống chặn (FR-025).

---

### User Story 5 - Công việc quá hạn được nhắc, leo thang theo mức quan trọng và đưa vào bàn giao (Priority: P1)

Hết khung thời gian mà chưa đóng, công việc chuyển Quá hạn và người thực hiện được nhắc. Theo mức quan trọng: công việc Thường được báo người phụ trách ca; công việc Quan trọng được báo trưởng tầng; công việc Bắt buộc tạo cảnh báo mức trung bình. Trưởng tầng hoặc người phụ trách ca xử lý: phân lại, ghi nhận thay, đóng Không thực hiện có lý do. Công việc chưa đóng khi hết ca được đưa vào bản nháp bàn giao và chuyển sang checklist ca sau khi bàn giao được xác nhận.

**Why this priority**: 1.3 bắt buộc công việc chưa hoàn thành phải vào bàn giao; bỏ sót xoay trở hay đo chỉ số có thể gây hại trực tiếp cho người cao tuổi.

**Independent Test**: Tạo 3 công việc (Thường, Quan trọng, Bắt buộc) đến hạn 10:00 với khung ±15 phút; không ghi nhận; kiểm tra lúc 10:15 cả ba Quá hạn, người thực hiện được nhắc, trưởng tầng được báo, cảnh báo trung bình được tạo cho việc Bắt buộc; kết thúc ca và kiểm tra cả ba có trong bản nháp bàn giao.

**Acceptance Scenarios**:

1. **Given** công việc Thường của A lúc 10:00, khung ±15 phút, Đến hạn, **When** tới 10:15 chưa đóng, **Then** công việc chuyển Quá hạn và người thực hiện nhận nhắc (BR-M04-05); **When** tới 10:45 (CFG-M04-12, mặc định \[30 phút\] sau khi Quá hạn) mà vẫn chưa đóng, **Then** người phụ trách ca nhận thông báo (8.7, FR-044); **Given** công việc được đóng lúc 10:30, **Then** người phụ trách ca không nhận thông báo.
2. **Given** công việc Quan trọng "hỗ trợ uống nước" của A Quá hạn, **When** chuyển Quá hạn, **Then** trưởng tầng của tầng A nhận thông báo (BR-M04-06); khi đóng công việc này (Hoàn thành trễ hoặc Không thực hiện) bắt buộc lý do (8.7).
3. **Given** công việc Bắt buộc "xoay trở" của A Quá hạn, **When** chuyển Quá hạn, **Then** hệ thống yêu cầu feature 007 tạo cảnh báo mức trung bình gắn công việc này (gộp theo BR-M05-02 nếu A đã có cảnh báo cùng loại đang mở); khi đóng công việc bắt buộc lý do (BR-M04-06).
4. **Given** công việc Quá hạn của A, **When** người phụ trách ca P (không phải trưởng tầng) phân lại cho nhân viên chăm sóc S2 cùng tầng trong ca, **Then** hệ thống cho phép vì P giữ nhiệm vụ Người phụ trách ca của tầng đó trong thời gian ca (feature 002 FR-019a), nhật ký ghi căn cứ là nhiệm vụ; **When** P làm việc này với công việc tầng khác, **Then** hệ thống chặn.
5. **Given** ca ngày kết thúc 18:00 và còn công việc Đến hạn, Quá hạn của A, **When** tới thời điểm lập bản nháp bàn giao CFG-M09-04 (mặc định \[30 phút\] trước kết ca), **Then** các công việc chưa đóng được đưa vào bản nháp bàn giao kèm trạng thái, mức quan trọng, người thực hiện, số lần đã nhắc (BR-M04-07, BR-M09-06).
6. **Given** bàn giao ca ngày được ca đêm xác nhận (feature 008), **When** xác nhận, **Then** công việc chưa đóng của ca ngày xuất hiện trong checklist ca đêm của nhân viên được phân công người cao tuổi tương ứng với dấu "tồn từ ca trước"; nếu không có người được phân công thì thành công việc chung của tầng (BR-M09-08). Riêng công việc Thường đang Quá hạn trong bàn giao được hệ thống chuyển Không thực hiện với lý do "hệ thống đóng sau bàn giao" và không vào checklist ca đêm; công việc Quan trọng, Bắt buộc Quá hạn vẫn chuyển sang ca đêm (FR-047).
7. **Given** công việc Quá hạn, **When** người thực hiện ghi nhận Hoàn thành với thời điểm thực hiện sau khung, **Then** công việc chuyển Hoàn thành trễ; lịch sử nhắc và thông báo đã gửi vẫn được giữ.
8. **Given** công việc Bắt buộc lúc 10:00 (khung ±15 phút) đã Quá hạn và có cảnh báo trung bình vì thiết bị của S mất kết nối, **When** bản ghi của S được đồng bộ lúc 10:40 với thời điểm trên thiết bị 10:05, **Then** công việc chuyển Hoàn thành (không phải Hoàn thành trễ), lịch sử nhắc được giữ, và feature 007 nhận yêu cầu đóng cảnh báo với lý do "đã thực hiện đúng khung" (FR-035).
9. **Given** công việc Chưa đến hạn của A lúc 15:00, **When** Trưởng tầng hủy kèm lý do, **Then** công việc chuyển Hủy; **When** người phụ trách ca P (không phải trưởng tầng) hủy công việc đó, **Then** hệ thống chặn (bảng FR-026, 8.3).
10. **Given** công việc Thường của B lúc 17:00 có trong bản nháp bàn giao ở trạng thái Đến hạn và chuyển Quá hạn lúc 17:15, **When** người thực hiện ghi Hoàn thành trễ lúc 17:40 trước khi ca đêm xác nhận lúc 18:10, **Then** công việc giữ kết quả Hoàn thành trễ và không bị hệ thống đóng; **Given** người thực hiện không ghi, **When** ca đêm xác nhận lúc 18:10, **Then** công việc chuyển Không thực hiện với lý do "hệ thống đóng sau bàn giao", người đóng là Hệ thống (FR-028, FR-047).

---

### User Story 6 - Kết quả ghi nhận kích hoạt quy tắc: uống nước thiếu, ăn kém kéo dài (Priority: P2)

Hệ thống tự đánh giá kết quả đã ghi: đến mốc kiểm tra CFG-M04-03, người có tổng lượng nước trong ngày dưới ngưỡng so với mục tiêu thì có cảnh báo nhẹ và thêm công việc "hỗ trợ uống nước"; người có CFG-M04-04 bữa ăn kém liên tiếp thì điều dưỡng được cảnh báo và feature 011 tạo yêu cầu dinh dưỡng viên xem lại chế độ ăn; tâm trạng tiêu cực kéo dài tạo cảnh báo nhẹ.

**Why this priority**: Đây là phần hệ thống "chủ động" phát hiện nguy cơ từ dữ liệu ghi hằng ngày (BF-02 bước 5), nhưng phụ thuộc story 4 đã có dữ liệu ghi nhận.

**Independent Test**: Với A có mục tiêu 1.500 ml, ghi tổng 800 ml trước 16:00; kiểm tra lúc 16:00 có cảnh báo nhẹ và công việc bổ sung. Với B, ghi 3 bữa liên tiếp "một phần", "bỏ bữa", "không ăn"; kiểm tra điều dưỡng có cảnh báo và feature 011 nhận sự kiện "ăn kém kéo dài".

**Acceptance Scenarios**:

1. **Given** A có mục tiêu lượng nước (nguồn theo FR-039) là 1.500 ml và tổng đã ghi trong ngày là 800 ml (53%), **When** tới mốc CFG-M04-03 (mặc định \[16:00 / 60% mục tiêu\]), **Then** hệ thống yêu cầu feature 007 tạo cảnh báo nhẹ "uống nước thiếu" và sinh thêm một công việc phát sinh "hỗ trợ uống nước" cho A trong ca hiện tại (BR-M04-08).
2. **Given** tổng lượng nước của A là 1.000 ml (67%), **When** tới mốc kiểm tra, **Then** không có cảnh báo và không có công việc bổ sung.
3. **Given** người bán trú E có mục tiêu 1.200 ml, điểm danh đến lúc 08:00 và đã uống 300 ml, **When** tới mốc 16:00, **Then** mục tiêu điều chỉnh là 1.200 × 8 ÷ 16 = 600 ml, ngưỡng 60% là 360 ml, nên E bị cảnh báo; **Given** E đã uống 400 ml, **Then** không có cảnh báo (FR-039).
4. **Given** F (nội trú) không có mục kế hoạch "hỗ trợ uống nước" có mục tiêu, **When** tới mốc kiểm tra, **Then** F không bị kiểm tra.
5. **Given** B có 3 bữa liên tiếp gần nhất ghi "Một phần", "Bỏ bữa", "Không ăn" (kể cả khi 3 bữa trải qua 2 ngày), **When** bữa thứ 3 được ghi, **Then** điều dưỡng phụ trách B nhận cảnh báo "ăn kém kéo dài", và feature 011 nhận sự kiện để tạo yêu cầu xem lại chế độ ăn, có thông báo tới dinh dưỡng viên (BR-M04-09, BR-M08-05, CFG-M04-04 mặc định \[3\]).
6. **Given** B có 2 bữa kém rồi bữa thứ 3 ghi "Phần lớn", **When** ghi, **Then** chuỗi đếm về 0, không có cảnh báo.
7. **Given** B có 2 bữa kém, sau đó B Tạm vắng qua 1 bữa (công việc bị Hủy), **When** bữa kế tiếp sau khi trở về ghi "Không ăn", **Then** bữa vắng không tính và không làm đứt chuỗi; chuỗi là 3 và cảnh báo được tạo.
8. **Given** B đã có cảnh báo "ăn kém kéo dài" đang mở, **When** thêm một bữa kém, **Then** cảnh báo được gộp theo BR-M05-02 (tăng số lần), không tạo cảnh báo mới.
9. **Given** C có tâm trạng ghi nhận "tiêu cực" 3 ngày liên tiếp, **When** ghi ngày thứ 3, **Then** hệ thống tạo cảnh báo nhẹ và đề xuất hoạt động theo sở thích đã ghi nhận (BR-M04-11, CFG-M04-05 mặc định \[3\]; danh sách gợi ý do feature 014 cung cấp).

---

### User Story 7 - Đính chính kết quả đã hoàn thành, không sửa trực tiếp (Priority: P2)

Kết quả đã ghi không sửa hay xóa. Khi ghi sai, người ghi gốc, trưởng tầng hoặc người phụ trách ca của phạm vi đó tạo bản đính chính có lý do; bản gốc vẫn giữ và xem lại được.

**Why this priority**: Toàn vẹn hồ sơ chăm sóc (constitution III, 1.5); sai sót ghi nhận ảnh hưởng tới quy tắc cảnh báo và chi phí.

**Independent Test**: Ghi uống nước 2.000 ml (nhầm, đúng là 200 ml); thử sửa trực tiếp và kiểm tra bị chặn; tạo đính chính 200 ml có lý do; kiểm tra tổng lượng nước dùng giá trị đính chính và bản gốc vẫn xem được.

**Acceptance Scenarios**:

1. **Given** công việc Hoàn thành của A với kết quả 2.000 ml, **When** S sửa trực tiếp kết quả, **Then** hệ thống chặn (BR-M04-14).
2. **Given** kết quả trên, **When** S (người ghi gốc) tạo đính chính 200 ml với lý do "nhập thừa số 0", **Then** bản đính chính trỏ bản gốc (DBR-23), giá trị hiện hành là 200 ml, bản gốc vẫn hiển thị trong lịch sử.
3. **Given** kết quả đính chính làm tổng lượng nước của A dưới ngưỡng trước mốc kiểm tra, **When** tới mốc kiểm tra, **Then** quy tắc BR-M04-08 dùng giá trị hiện hành sau đính chính.
4. **Given** công việc "thay tã" đã sinh chi phí nháp số lượng 2, **When** trưởng tầng đính chính số lượng thành 1, **Then** feature 010 nhận sự kiện đính chính để điều chỉnh chi phí nháp hoặc tạo khoản điều chỉnh nếu kỳ đã chốt (DBR-17).
5. **Given** nhân viên chăm sóc T không phải người ghi gốc, không phải trưởng tầng hay người phụ trách ca của tầng, **When** T tạo đính chính, **Then** hệ thống chặn.
6. **(Bổ sung, CHK038)** **Given** B có 3 bữa kém liên tiếp và cảnh báo "ăn kém kéo dài" đang mở, **When** kết quả "Không ăn" của bữa thứ ba được đính chính "Hủy ghi nhận" vì ghi nhầm người, **Then** công việc của bữa đó giữ trạng thái Hoàn thành, kết quả mang dấu "đã hủy ghi nhận" và bản gốc vẫn xem được; từ lúc đó bữa này được coi là không có kết quả nên chuỗi còn 2; cảnh báo đã tạo không tự rút lại (feature 007) (FR-037, FR-040).

---

### User Story 8 - Yêu cầu xem xét kế hoạch sau đánh giá lại hoặc sự cố ngã (Priority: P2)

Khi bác sĩ chấp nhận đánh giá lại hoặc có sự cố ngã, hệ thống tạo yêu cầu xem xét kế hoạch chăm sóc cho điều dưỡng phụ trách với hạn CFG-M04-08. Điều dưỡng xử lý bằng cách gửi duyệt phiên bản mới hoặc ghi "không cần thay đổi" có lý do.

**Why this priority**: Giữ kế hoạch phù hợp với tình trạng mới (BR-M04-20), nhưng không chặn vận hành hằng ngày.

**Independent Test**: Chấp nhận một đánh giá lại cho A; kiểm tra có yêu cầu xem xét với hạn 48 giờ; gửi duyệt phiên bản mới; kiểm tra yêu cầu được đóng.

**Acceptance Scenarios**:

1. **Given** bác sĩ chấp nhận đánh giá lại của A (feature 001 FR-043), **When** lưu, **Then** hệ thống tạo yêu cầu xem xét kế hoạch cho điều dưỡng phụ trách A, hạn CFG-M04-08 (mặc định \[48 giờ\]) (BR-M04-20).
2. **Given** có sự cố ngã của A (feature 007, BR-M05-07), **When** sự cố được ghi nhận, **Then** hệ thống tạo yêu cầu xem xét kế hoạch tương tự; nếu A đã có yêu cầu xem xét đang mở thì gộp nguồn vào yêu cầu đó, không tạo yêu cầu mới.
3. **Given** yêu cầu xem xét đang mở, **When** điều dưỡng gửi duyệt một phiên bản kế hoạch mới của A, **Then** yêu cầu chuyển Đã xử lý và trỏ tới phiên bản đó; **When** điều dưỡng chọn "không cần thay đổi" kèm lý do, **Then** yêu cầu chuyển Đã xử lý với lý do đó.
4. **Given** yêu cầu xem xét quá hạn mà chưa xử lý, **When** tới hạn, **Then** trưởng tầng của A nhận thông báo và yêu cầu được gắn dấu "quá hạn".
5. **Given** yêu cầu xem xét của A đang Mở, **When** A chuyển trạng thái cuối, **Then** yêu cầu chuyển Đã hủy với lý do trạng thái cuối; **Given** yêu cầu đã Đã xử lý bằng một phiên bản gửi duyệt, **When** phiên bản đó bị Trả lại, **Then** yêu cầu không mở lại (bảng FR-011).

---

### User Story 9 - Trưởng tầng phân lại công việc chung và xem thời khóa biểu cá nhân (Priority: P3)

Công việc không có người thực hiện (nhân viên vắng ca, nghỉ việc, chưa phân công) trở thành công việc chung của tầng; trưởng tầng phân lại hoặc nhân viên đủ điều kiện tự nhận. Trưởng tầng và điều dưỡng xem thời khóa biểu trong ngày của từng người cao tuổi, tổng hợp từ công việc, liều thuốc, buổi hoạt động và lịch thăm.

**Why this priority**: Bảo đảm không công việc nào mất người chịu trách nhiệm (BR-M04-13) và hỗ trợ điều phối; không chặn các luồng P1.

**Independent Test**: Đánh dấu nhân viên S vắng ca; kiểm tra công việc của S chuyển thành công việc chung của tầng; trưởng tầng phân lại cho S2; kiểm tra S2 thấy công việc trong checklist.

**Acceptance Scenarios**:

1. **Given** S được phân công ca ngày tầng 2 và có 20 công việc, **When** S được ghi nhận vắng ca (feature 008), **Then** các công việc chưa đóng của S chuyển thành công việc chung của tầng 2 và trưởng tầng nhận thông báo (BR-M04-13).
2. **Given** công việc chung "phát thuốc hỗ trợ" yêu cầu điều dưỡng có giấy phép còn hiệu lực, **When** trưởng tầng phân cho nhân viên chăm sóc hoặc điều dưỡng có giấy phép hết hạn, **Then** hệ thống chặn (BR-M09-01).
3. **Given** công việc chung của tầng 2, **When** nhân viên chăm sóc S2 đủ điều kiện chọn "Nhận việc", **Then** công việc được giao cho S2 và biến khỏi danh sách chung của người khác.
4. **Given** A được chuyển giường từ tầng 2 sang tầng 3 (feature 003 FR-031), **When** chuyển thành công, **Then** công việc chưa thực hiện của A được gán theo phân công tầng 3; công việc đã đóng giữ nguyên.
5. **Given** A có công việc, liều thuốc, buổi hoạt động và lịch thăm trong ngày, **When** điều dưỡng xem thời khóa biểu của A, **Then** các mục được sắp theo thời gian, ghi rõ nguồn và trạng thái; không có thao tác sửa trên thời khóa biểu.

---

### Edge Cases

- **Cần thay đổi kế hoạch gấp trong ngày**: ngày hiệu lực sớm nhất là ngày hôm sau ngày duyệt (FR-003), nên phiên bản đang hiệu lực vẫn áp tới hết hôm nay; việc cần làm ngay trong ngày được điều dưỡng hoặc trưởng tầng tạo thành công việc phát sinh (FR-025). Người cao tuổi mới vào ở chưa có kế hoạch hiệu lực thì hưởng FR-009 cho tới ngày hiệu lực đầu tiên.
- **Người cao tuổi chưa có kế hoạch hiệu lực**: vẫn sinh công việc từ lịch đo, lịch ăn, hoạt động; trưởng tầng và điều dưỡng phụ trách thấy dấu "chưa có kế hoạch chăm sóc hiệu lực" trên thẻ người cao tuổi (FR-009).
- **Người cao tuổi chuyển trạng thái cuối** (Kết thúc lưu trú, Qua đời, Hủy tiếp nhận): mọi công việc Chưa đến hạn chuyển Hủy, công việc Đến hạn và Quá hạn được hệ thống chuyển Không thực hiện (lý do trạng thái cuối) và cảnh báo quá hạn liên quan được đóng (FR-010); phiên bản Hiệu lực chuyển Hết hiệu lực, phiên bản Nháp/Chờ duyệt/Chờ hiệu lực chuyển Đã hủy; không sinh công việc mới (feature 004 SC-008).
- **Vắng khi công việc đang Đến hạn**: công việc đã Đến hạn hoặc Quá hạn lúc người cao tuổi rời đi không bị hủy tự động (8.3 chỉ cho Hủy từ Chưa đến hạn); người thực hiện hoặc trưởng tầng đóng Không thực hiện với lý do "vắng mặt" (FR-027).
- **Hoạt động bên ngoài**: như vắng mặt (BR-M04-04); liều thuốc "Mang theo" do feature 006/014 xử lý.
- **Bán trú điểm danh đến muộn hơn giờ về dự kiến hoặc sau khi Vắng không báo**: vẫn điểm danh được; công việc sinh trong khoảng từ giờ đến tới giờ về dự kiến, nếu khoảng này rỗng thì không có công việc (FR-018).
- **Bán trú quên điểm danh về**: cuối ngày vẫn "Có mặt" thì trưởng tầng được thông báo; trạng thái không tự chuyển (FR-019).
- **Nguồn của công việc bị gỡ** (lịch đo bị ngừng, buổi hoạt động bị hủy, bữa bị hủy): công việc Chưa đến hạn tương ứng chuyển Hủy với lý do nguồn; công việc đã đóng giữ nguyên (FR-008).
- **Công việc Quá hạn kéo dài qua nhiều ca**: công việc Thường được hệ thống đóng Không thực hiện sau lần bàn giao đầu tiên được xác nhận; công việc Quan trọng, Bắt buộc không tự đóng và tiếp tục vào bàn giao mỗi ca cho tới khi được đóng (FR-047).
- **Công việc Thường vẫn Đến hạn lúc ca sau xác nhận bàn giao, rồi chuyển Quá hạn trong ca sau**: công việc vào checklist ca sau; nó chỉ bị hệ thống đóng khi lần bàn giao kế tiếp có chứa nó được xác nhận mà nó đang Quá hạn (FR-047).
- **Công việc Thường đang Đến hạn lúc lập bản nháp bàn giao, chuyển Quá hạn trước khi ca sau xác nhận**: công việc đã có trong bàn giao và đang Quá hạn lúc xác nhận nên bị hệ thống đóng Không thực hiện; nếu người thực hiện đóng trước lúc xác nhận thì giữ kết quả đó (FR-047).
- **Nhân viên nghỉ việc** (BR-M09-04): công việc tương lai của họ thành công việc chung của tầng như khi vắng ca.
- **Hai nhân viên cùng ghi nhận một công việc gần như đồng thời**: chỉ lần ghi đầu tiên đóng công việc; lần sau bị từ chối và người ghi được báo công việc đã đóng bởi ai.
- **Ghi nhận ngoại tuyến**: xem FR-035; quyền được kiểm tra theo thời điểm trên thiết bị (feature 002 FR-041a); bản ghi ngoại tuyến quá CFG-M15-08 chuyển "chờ xem lại" cho trưởng tầng.
- **Quản lý viện đổi CFG-M04-03 trong ngày**: giá trị mới áp từ lần kiểm tra kế tiếp; lần kiểm tra đã chạy không tính lại (feature 000).
- **Kết quả thay tã/vật phẩm đã tính phí nhưng công việc sau đó bị đính chính "Hủy ghi nhận"**: feature 010 nhận sự kiện để hủy chi phí nháp hoặc tạo khoản điều chỉnh.
- **Bán trú chưa được điểm danh về khi đã sang ngày hôm sau** (CHK036): trạng thái có mặt của ngày trước vẫn là "Có mặt" cho tới khi có lệnh Điểm danh về, ghi giờ về thực tế (có thể thuộc ngày sau); ngày mới có trạng thái có mặt riêng theo lịch của nó. Lịch đến mỗi ngày của một người bán trú là một khoảng liên tục từ giờ đến tới giờ về (feature 003 FR-039), nên mỗi ngày có đúng một trạng thái có mặt, kể cả khi khoảng đó phủ nhiều buổi.
- **Công việc chung không có ai trong ca đủ vai trò và chứng chỉ để nhận** (CHK037): công việc vẫn là công việc chung và theo vòng đời thường; ngay khi công việc trở thành công việc chung mà ca không có nhân viên nào đủ điều kiện, Trưởng tầng được thông báo mức Trung bình để xếp người (BR-M04-13, BR-M09-01).
- **Bàn giao ở trạng thái "Có ý kiến"** (CHK035): được coi là chưa xác nhận (feature 008 Q-87); công việc chưa đóng xử lý theo FR-047a và chỉ áp FR-047 khi bàn giao Đã xác nhận.
- **Bộ lập lịch bỏ lỡ lần chạy** (CHK034): xem FR-052.
- **Thiết bị mất kết nối** (CHK044): checklist đã tải vẫn xem và ghi tạm được; xem FR-035.

## Requirements *(mandatory)*

### Functional Requirements

Mọi yêu cầu dưới đây kế thừa feature 000: nhóm dữ liệu (mục 1.5), lý do bắt buộc cho lệnh nhóm 2, đính chính cho bản ghi đã xác nhận, nhật ký cho mọi thay đổi, tham số theo mã CFG, vòng đời yêu cầu phê duyệt và tự duyệt (Q-10), "hoặc toàn bộ, hoặc không" khi một lệnh có nhiều tác động, mốc thời gian theo Asia/Ho_Chi_Minh (DBR-25). Phân nhóm dữ liệu của feature này:

| Dữ liệu | Nhóm (1.5) | Thao tác được phép |
| --- | --- | --- |
| Loại công việc | 1 – Danh mục | Tạo, sửa, Ngừng hiệu lực (Quản lý viện); không xóa khi đã được tham chiếu |
| Phiên bản kế hoạch chăm sóc, mục kế hoạch | 2 – theo phiên bản | Sửa mục chỉ ở Nháp; còn lại qua lệnh ở bảng mục A |
| Yêu cầu xem xét kế hoạch | 2 | Hệ thống tạo; điều dưỡng xử lý qua lệnh ở mục A |
| Công việc | 2 | Chỉ qua lệnh ở bảng mục C; không "sửa trạng thái" |
| Trạng thái có mặt bán trú theo ngày | 2 | Chỉ qua lệnh ở bảng mục B |
| Kết quả ghi nhận, lần nhắc/thông báo quá hạn, lần phân lại công việc | 3 | Chỉ ghi thêm; kết quả sai xử lý bằng đính chính |

#### A. Kế hoạch chăm sóc theo phiên bản

- **FR-001**: Mỗi phiên bản kế hoạch chăm sóc MUST gồm: người cao tuổi, số phiên bản, ngày hiệu lực, ngày hết hiệu lực (khi có), trạng thái, người lập, người duyệt, thời điểm duyệt, lý do thay đổi so với phiên bản trước, danh sách mục kế hoạch. *(Nguồn: 8.1, KE_HOACH_CHAM_SOC, UC-22)*
- **FR-002**: Mỗi mục kế hoạch MUST gồm: loại công việc (mục E), nội dung, tần suất (số lần/ngày, mỗi N giờ, theo ngày trong tuần, hoặc một lần mỗi ca), mốc giờ bắt đầu hoặc "trong ca", khung thời gian cho phép (mặc định lấy từ loại công việc, nếu không có thì CFG-M04-02, mặc định \[±15 phút\]), vai trò thực hiện, kết quả cần ghi, mức quan trọng (Thường / Quan trọng / Bắt buộc), có tính phí hay không, và mục tiêu định lượng khi loại công việc có (ví dụ lượng nước ml/ngày). Mục có mốc "trong ca" MUST có khung bằng toàn bộ ca. Với tần suất "mỗi N giờ", chuỗi thời điểm dự kiến của mỗi ngày bắt đầu lại từ mốc giờ bắt đầu và lặp mỗi N giờ tới hết ngày đó (24:00), không nối sang ngày sau; mỗi công việc thuộc ca chứa thời điểm dự kiến của nó, nên chuỗi đi qua ranh giới hai ca được chia theo ca. Khi mục có "theo ngày trong tuần", chỉ các thời điểm dự kiến rơi vào ngày được chọn mới sinh công việc (CHK011). *(Nguồn: UC-22, 8.1, MUC_KE_HOACH)*
- **FR-003**: Vòng đời phiên bản kế hoạch MUST theo bảng dưới; mọi lệnh MUST lưu người thực hiện, thời điểm và lý do khi bảng yêu cầu. *(Nguồn: UC-22, UC-23, 8.1, BR-M04-19, 1.5, constitution IV)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái mới | Người thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Lập phiên bản | Nháp | Điều dưỡng; Hệ thống khi bác sĩ chấp nhận đánh giá (feature 001 FR-031) | Người cao tuổi chưa ở trạng thái cuối; không có phiên bản Nháp, Chờ duyệt hoặc Chờ hiệu lực khác | Nội dung khởi đầu là bản sao phiên bản Hiệu lực (nếu có) hoặc hoạt động mẫu từ đánh giá |
| Nháp | Cập nhật mục | Nháp | Điều dưỡng trong phạm vi | — | Lưu lịch sử thay đổi của bản nháp |
| Nháp | Gửi duyệt | Chờ duyệt | Điều dưỡng | Có ít nhất một mục; mọi mục đủ trường FR-002; ngày hiệu lực đề xuất muộn hơn hôm nay; có lý do thay đổi khi đã có phiên bản trước | Thông báo người có quyền Duyệt kế hoạch chăm sóc trong phạm vi; đóng yêu cầu xem xét đang mở (FR-011) |
| Chờ duyệt | Duyệt | Chờ hiệu lực | Người có quyền Duyệt kế hoạch chăm sóc (FR-005) | Ngày hiệu lực muộn hơn ngày duyệt (sớm nhất là ngày hôm sau; đề xuất Q-34) | Ghi người duyệt, thời điểm duyệt |
| Chờ duyệt | Trả lại | Nháp | Người có quyền duyệt | Bắt buộc lý do | Thông báo người lập kèm lý do |
| Nháp, Chờ duyệt | Hủy phiên bản | Đã hủy | Người lập, người có quyền duyệt | Bắt buộc lý do | — |
| Chờ hiệu lực | Thu hồi duyệt | Đã hủy | Người có quyền duyệt | Trước ngày hiệu lực; bắt buộc lý do | Thông báo người lập |
| Chờ hiệu lực | Tới ngày hiệu lực | Hiệu lực | Bộ lập lịch | Người cao tuổi chưa ở trạng thái cuối | Áp FR-006 |
| Hiệu lực | Phiên bản mới có hiệu lực | Hết hiệu lực | Hệ thống | — | Ngày hết hiệu lực = ngày liền trước ngày hiệu lực của phiên bản mới |
| Hiệu lực | Người cao tuổi chuyển trạng thái cuối | Hết hiệu lực | Hệ thống | — | Hủy công việc Chưa đến hạn (FR-010) |
| Nháp, Chờ duyệt, Chờ hiệu lực | Người cao tuổi chuyển trạng thái cuối | Đã hủy | Hệ thống | — | Lý do: trạng thái cuối của người cao tuổi |

- **FR-004**: Phiên bản Hiệu lực, Hết hiệu lực và Đã hủy MUST NOT sửa được; mọi thay đổi nội dung MUST qua một phiên bản mới. Mỗi người cao tuổi MUST có tối đa một phiên bản Hiệu lực tại một ngày (DBR-11) và tối đa một phiên bản ở Nháp, Chờ duyệt hoặc Chờ hiệu lực. Người duyệt MUST NOT sửa nội dung hay ngày hiệu lực khi duyệt; nếu ngày hiệu lực đề xuất không còn hợp lệ (không muộn hơn ngày duyệt) thì người duyệt MUST dùng lệnh Trả lại để người lập chọn ngày mới. *(Nguồn: BR-M04-19, DBR-11, UC-22, UC-23)*
- **FR-005**: Quyền Duyệt kế hoạch chăm sóc MUST mặc định thuộc Bác sĩ; Điều dưỡng chỉ duyệt được khi được Quản lý viện gán quyền này (feature 002 FR-024, Q-15, Q-07 theo mặc định). Người lập có quyền duyệt MAY tự duyệt, bắt buộc lý do và nhật ký đánh dấu "tự duyệt" (Q-10). Phiên bản Chờ duyệt MUST được nhắc người duyệt theo CFG-M15-05 và báo Quản lý viện theo CFG-M15-06 như yêu cầu phê duyệt của feature 000. *(Nguồn: UC-23, 2.4, 4.4 dòng "Kế hoạch chăm sóc": BS D, ĐD T, TT X, QL X, CS P)*
- **FR-006**: Khi một phiên bản chuyển Hiệu lực, trong cùng một lần hệ thống MUST: chuyển phiên bản trước sang Hết hiệu lực; hủy các công việc sinh từ phiên bản trước đang ở Chưa đến hạn có thời điểm dự kiến từ thời điểm hiệu lực; sinh lại công việc từ phiên bản mới cho các ngày/ca đã tới thời điểm sinh (FR-022). Khi sinh lại, nếu người cao tuổi còn một công việc cùng loại từ phiên bản trước đang Đến hạn hoặc Quá hạn, hệ thống MUST NOT sinh công việc mới có thời điểm dự kiến rơi vào khung thời gian của công việc đó, để một việc không xuất hiện hai lần quanh thời điểm đổi phiên bản (CHK039). *(Nguồn: UC-23, UC-24, BR-M04-03, BR-M04-19)*
- **FR-007**: Nhân viên chăm sóc MUST xem được phiên bản kế hoạch Hiệu lực của người cao tuổi trong phạm vi phân công; Trưởng tầng và Quản lý viện MUST xem được mọi phiên bản và lịch sử; Hành chính, Dinh dưỡng viên, Nhân viên bếp, Nhân viên vệ sinh và Người thân MUST NOT xem kế hoạch. *(Nguồn: UC-22, 4.4 dòng "Kế hoạch chăm sóc")*
- **FR-008**: Hệ thống MUST cho xem lịch sử phiên bản của một người cao tuổi, và so sánh hai phiên bản (mục thêm, bớt, đổi). *(Nguồn: UC-22, 8.1 "lưu lịch sử khi thay đổi")*
- **FR-009**: Người cao tuổi Đang lưu trú chưa có phiên bản Hiệu lực MUST được đánh dấu "chưa có kế hoạch chăm sóc hiệu lực" trên thẻ người cao tuổi cho Trưởng tầng và Điều dưỡng phụ trách. *(Nguồn: 8.1, UC-22)*
- **FR-010**: Khi người cao tuổi chuyển trạng thái cuối (Kết thúc lưu trú, Qua đời, Hủy tiếp nhận), trong cùng một lần với lệnh đó hệ thống MUST: áp các dòng tương ứng của bảng FR-003; hủy mọi công việc Chưa đến hạn của người đó; chuyển mọi công việc Đến hạn và Quá hạn của người đó sang Không thực hiện với lý do là trạng thái cuối, không bắt nhân viên đóng tay; yêu cầu feature 007 đóng các cảnh báo quá hạn gắn các công việc này; và không đưa các công việc này vào bàn giao. Nếu bản nháp bàn giao của ca đã được lập trước thời điểm đó (FR-046) và chưa được xác nhận, các công việc này MUST được cập nhật trong bản nháp thành đã đóng với lý do trạng thái cuối, không còn là công việc tồn, và MUST NOT chuyển sang checklist ca sau. *(Nguồn: 5.6, 1.3, feature 004 SC-008, UC-24; Clarification 2026-09-25, đề xuất Q-38)*
- **FR-011**: Khi bác sĩ chấp nhận đánh giá lại (feature 001 FR-043) hoặc có sự cố ngã (feature 007, BR-M05-07), hệ thống MUST tạo yêu cầu xem xét kế hoạch chăm sóc cho Điều dưỡng phụ trách người cao tuổi đó, hạn CFG-M04-08 (mặc định \[48 giờ\]); nếu người đó đã có yêu cầu đang mở thì MUST gộp nguồn vào yêu cầu đó. Nguồn thứ ba **(bổ sung 2026-10-01, CHK007)**: khi một phụ lục đổi mức chăm sóc được áp dụng (feature 004 FR-042), hệ thống MUST tạo hoặc gộp yêu cầu như trên; cờ nguy cơ mới chỉ phát sinh từ một lần đánh giá được chấp nhận nên đã nằm trong nguồn thứ nhất. "Điều dưỡng phụ trách" là nhiệm vụ theo ca (2.4), không phải một người cố định: yêu cầu MUST hiện cho mọi điều dưỡng đang có người cao tuổi đó trong phạm vi phân công của ca hiện tại, và bất kỳ ai trong số đó xử lý được; yêu cầu còn Mở khi hết ca MUST có trong bản nháp bàn giao (feature 008) (CHK022). Vòng đời yêu cầu xem xét MUST theo bảng dưới. *(Nguồn: BR-M04-20, CFG-M04-08, UC-22, constitution IV)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái mới | Người thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Đánh giá lại được chấp nhận, có sự cố ngã, hoặc phụ lục đổi mức chăm sóc được áp dụng | Mở | Hệ thống | Người cao tuổi chưa ở trạng thái cuối; chưa có yêu cầu Mở | Giao cho Điều dưỡng phụ trách; hạn = thời điểm tạo + CFG-M04-08; thông báo người được giao |
| Mở | Nguồn mới (đánh giá lại, sự cố ngã hoặc phụ lục đổi mức chăm sóc khác) | Mở | Hệ thống | — | Gộp nguồn vào yêu cầu; hạn giữ nguyên |
| Mở | Tới hạn mà chưa xử lý | Mở (gắn dấu "quá hạn") | Bộ lập lịch | — | Thông báo Trưởng tầng của người cao tuổi |
| Mở | Gửi duyệt phiên bản kế hoạch mới (bảng FR-003) | Đã xử lý | Điều dưỡng | Phiên bản được gửi duyệt sau thời điểm tạo yêu cầu | Yêu cầu trỏ tới phiên bản đó; gỡ dấu "quá hạn" nếu có |
| Mở | Ghi "không cần thay đổi" | Đã xử lý | Điều dưỡng | Bắt buộc lý do | Lưu lý do và người xử lý |
| Mở | Người cao tuổi chuyển trạng thái cuối | Đã hủy | Hệ thống | — | Lý do: trạng thái cuối của người cao tuổi |

Đã xử lý và Đã hủy là trạng thái cuối; nếu phiên bản gửi duyệt sau đó bị Trả lại hoặc Hủy, yêu cầu MUST NOT mở lại, và nguồn mới sau đó MUST tạo yêu cầu mới.

#### B. Trạng thái có mặt của bán trú và điểm danh

- **FR-012**: Mỗi ngày có lịch đến theo hợp đồng bán trú đang hiệu lực (feature 004), hệ thống MUST tạo trạng thái có mặt "Chưa đến" cho người đó, kèm giờ đến và giờ về dự kiến. *(Nguồn: UC-28, 3.4, CO_MAT_BAN_TRU)*
- **FR-013**: Trạng thái có mặt MUST theo bảng dưới. *(Nguồn: 3.4, BR-M04-02, UC-28)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái mới | Người thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Tạo theo lịch đến | Chưa đến | Bộ lập lịch | Có lịch đến trong ngày; chưa có báo vắng; ngày đó không phải ngày khu bán trú nghỉ (feature 004 FR-060a) | — |
| — | Điểm danh đến vào ngày không có lịch (buổi phát sinh) | Có mặt | Hành chính, Nhân viên chăm sóc, Điều dưỡng (Q-214) | Người cao tuổi Đang lưu trú; ngày không có lịch hoặc trùng ngày khu nghỉ | Ghi giờ đến, đánh dấu "buổi phát sinh"; sinh công việc (FR-018); feature 010 tính phí buổi phát sinh (feature 010 FR-005b, Q-140) |
| Chưa đến, Vắng không báo | Khai báo ngày khu bán trú nghỉ (feature 004) | (không còn trạng thái có mặt cho buổi đó) | Hệ thống | Buổi chưa có điểm danh đến | Hủy trạng thái có mặt của buổi; không thông báo Trưởng tầng; feature 010 không tính phí buổi (Q-140) |
| — hoặc Chưa đến | Ghi nhận báo vắng | Vắng có báo | Hành chính (feature 004) | Báo trước giờ đến dự kiến ít nhất CFG-M02-06 (mặc định \[24 giờ\]) | Không sinh công việc |
| Chưa đến | Quá giờ đến dự kiến CFG-M02-07 (mặc định \[2 giờ\]) | Vắng không báo | Bộ lập lịch | Chưa điểm danh đến, không có báo vắng hợp lệ | Thông báo Trưởng tầng |
| Chưa đến, Vắng không báo | Điểm danh đến | Có mặt | Hành chính, Nhân viên chăm sóc, Điều dưỡng (Q-214) | Người cao tuổi Đang lưu trú | Ghi giờ đến, người điểm danh; sinh công việc (FR-018) |
| Có mặt | Điểm danh về | Đã về | Hành chính, Nhân viên chăm sóc, Điều dưỡng (Q-214) | Có bản ghi đón hợp lệ của feature 012 (FR-026 của 012): người đón thuộc danh sách hoặc có ngoại lệ đón Hiệu lực, hoặc loại "Tự về" khi dấu "được tự về" có tác dụng (Q-125) | Ghi giờ về; hủy công việc Chưa đến hạn sau giờ về (FR-019) |

- **FR-014**: Báo vắng được ghi muộn hơn CFG-M02-06 MUST NOT tạo trạng thái "Vắng có báo"; trạng thái vẫn đi theo bảng FR-013 và bản ghi báo vắng muộn MUST được lưu kèm để feature 004 áp phí. *(Nguồn: UC-28, 3.4)*
- **FR-015**: Điểm danh đến/về MUST NOT sửa trực tiếp; giờ đến/về ghi sai MUST được xử lý bằng đính chính theo feature 000. *(Nguồn: UC-28, 1.5)*
- **FR-016**: Trạng thái có mặt MUST được cung cấp cho feature 004/010 (tính phí buổi), feature 011 (chốt suất ăn) và cho việc sinh công việc. Mỗi lần trạng thái được xác định lần đầu hoặc thay đổi, kể cả buổi phát sinh ngoài lịch và việc hủy buổi do ngày khu nghỉ, MUST được gửi cho feature 010 (feature 010 FR-005b). *(Nguồn: UC-28, 3.4; đồng bộ spec 010, Q-140)*

#### C. Sinh công việc và vòng đời công việc

- **FR-017**: Vào thời điểm CFG-M04-01 (mặc định \[00:00, hoặc trước mỗi ca 1 giờ\]), Bộ lập lịch MUST sinh công việc cho ngày/ca tới từ: các mục của phiên bản kế hoạch Hiệu lực tại thời điểm dự kiến của công việc; lịch đo (feature 007); lịch ăn (feature 011); buổi hoạt động mà người cao tuổi đã đăng ký (feature 014); và các nguồn tự động khác (theo dõi sau ngã BR-M05-07, công việc bổ sung BR-M04-08); lịch vệ sinh của phòng/khu vực theo feature 003 FR-044 (BR-M03-08). Liều thuốc MUST NOT sinh công việc. Công việc gắn người cao tuổi chỉ sinh cho người Đang lưu trú và không đang vắng mặt tại thời điểm dự kiến; không sinh cho người bán trú lúc này. Công việc vệ sinh gắn phòng/khu vực không phụ thuộc sự có mặt của người cao tuổi; điều kiện không sinh theo feature 003 FR-044. CFG-M04-01 có hai chế độ loại trừ nhau do cơ sở chọn: (a) một lần mỗi ngày tại giờ cấu hình, sinh cho cả ngày — mặc định, lúc \[00:00\]; (b) trước giờ bắt đầu mỗi ca một khoảng cấu hình, mặc định \[1 giờ\], sinh cho ca đó (CHK010). Phiên bản kế hoạch MAY ở Hiệu lực khi người cao tuổi còn Đang tiếp nhận; khi đó không có công việc nào được sinh, và khi "Hoàn tất tiếp nhận" được thực hiện, hệ thống MUST sinh công việc có thời điểm dự kiến từ thời điểm đó cho các ngày/ca đã tới thời điểm sinh, như khi trở về ở FR-020 (CHK008). *(Nguồn: BR-M04-01, BR-M03-08, 8.3 "Làm rõ", UC-24, 5.6)*
- **FR-018**: Với bán trú, công việc MUST được sinh khi điểm danh đến, gồm các công việc có khung thời gian chưa kết thúc tính từ giờ đến tới giờ về dự kiến của ngày đó. *(Nguồn: UC-24, UC-28, BR-M04-02, 3.3)*
- **FR-018a**: **(Đồng bộ spec 014, Q-167, Q-176)** Giờ về dự kiến theo ngày của bán trú thuộc trạng thái có mặt theo ngày (3.4) do spec này quản lý. Khi feature 014 báo một đăng ký có đồng ý về muộn, giờ về dự kiến của ngày đó MUST dời tới giờ kết thúc buổi (hoặc giờ về dự kiến của chuyến, kể cả gia hạn), và trở lại theo lịch khi đăng ký bị hủy trước giờ bắt đầu. Công việc được sinh, hủy theo giờ về mới (FR-018, BR-M04-02); cảnh báo "hết ngày vẫn Có mặt" (FR-019) xét theo giờ về mới; feature 003, 010, 011 lấy giờ về theo ngày từ spec này. *(Nguồn: 3.4 "(Bổ sung, spec 014)")*
- **FR-019**: Khi điểm danh về, công việc Chưa đến hạn có thời điểm dự kiến sau giờ về MUST chuyển Hủy với lý do "đã về". Nếu hết ngày dương lịch (24:00, DBR-25) mà người bán trú vẫn "Có mặt", hệ thống MUST thông báo mức Trung bình cho Người phụ trách ca đang diễn ra của tầng để kiểm tra ngay và mức Nhẹ cho Trưởng tầng (feature 009 FR-043b, Q-115), và MUST NOT tự chuyển trạng thái; nếu người kiểm tra xác minh người cao tuổi không còn ở viện mà không rõ ở đâu, họ ghi sự cố đi lạc theo feature 007. *(Nguồn: UC-24, UC-28, BR-M04-02)*
- **FR-020**: Khi người cao tuổi chuyển sang Tạm vắng, Điều trị tại bệnh viện hoặc Hoạt động bên ngoài, công việc Chưa đến hạn của người đó có thời điểm dự kiến từ thời điểm rời đi MUST chuyển Hủy với lý do "vắng mặt". Khi người đó trở về Đang lưu trú, hệ thống MUST sinh công việc có thời điểm dự kiến từ lúc trở về cho các ngày/ca đã tới thời điểm sinh. *(Nguồn: UC-24, BR-M04-04)*
- **FR-020a**: **(Đồng bộ spec 014, Q-170)** Khi feature 014 đính chính thời điểm rời hoặc về thực tế của chuyến đi, hoặc Hủy ghi nhận bản ghi rời viện/về, hệ thống MUST tính lại theo FR-020: công việc đã bị Hủy vì vắng mặt mà nay nằm ngoài khoảng đi đúng được sinh lại nếu thời điểm dự kiến chưa qua; công việc đã sinh lại mà nay nằm trong khoảng đi đúng và còn Chưa đến hạn chuyển Hủy lý do "vắng mặt". Công việc đã đóng không bị sửa. *(Nguồn: BR-M04-04; feature 014 FR-031a)*
- **FR-021**: Mỗi công việc MUST là duy nhất theo (nguồn sinh, thời điểm dự kiến) — với công việc từ kế hoạch là (mục kế hoạch, thời điểm dự kiến); với nguồn khác, nguồn sinh là bản ghi nguồn cụ thể (một lần đo trong lịch đo, một bữa, một buổi hoạt động, một lần kích hoạt quy tắc cho một người cao tuổi trong một ngày); công việc tạo tay (phát sinh, "Ghi nhận phát sinh") không chịu khóa này. Khóa chỉ xét trên công việc chưa ở trạng thái Hủy: công việc đã Hủy vì vắng mặt không cản việc sinh lại công việc cùng (nguồn sinh, thời điểm dự kiến) khi người cao tuổi trở về trước thời điểm đó, kể cả khi vắng và về nhiều lần trong một ca (CHK040). Với lịch theo dõi sau ngã (BR-M05-07), nguồn sinh là lịch theo dõi do feature 007 cung cấp; việc gộp hai lịch chồng thời gian thuộc feature 007 (CHK062). Chạy lại việc sinh MUST NOT tạo trùng. *(Nguồn: UC-24, DBR-12, NFR-04)*
- **FR-022**: Khi nguồn của công việc thay đổi (phiên bản kế hoạch mới, lịch đo đổi/ngừng, bữa bị hủy, đăng ký hoạt động bị hủy), hệ thống MUST chỉ hủy và sinh lại công việc ở Chưa đến hạn tính từ thời điểm thay đổi có hiệu lực; công việc ở trạng thái khác MUST NOT bị sửa. *(Nguồn: UC-24, BR-M04-03)*
- **FR-023**: Mỗi công việc MUST có: người cao tuổi, loại công việc, nguồn sinh (mục kế hoạch, lịch đo, bữa, buổi hoạt động, phát sinh, quy tắc), thời điểm dự kiến, khung thời gian cho phép, mức quan trọng, vai trò thực hiện, người thực hiện hoặc "công việc chung của tầng/khu vực", ca, tầng/khu vực, có tính phí, trạng thái, lý do (khi Không thực hiện hoặc Hủy). Công việc từ lịch đo theo chỉ định MUST có mức Bắt buộc. *(Nguồn: UC-24, CONG_VIEC, 8.1, BR-M04-06)*
- **FR-024**: Người thực hiện MUST lấy theo phân công chăm sóc hiện hành của ca (feature 008): nhân viên phụ trách chính của người cao tuổi có vai trò khớp vai trò thực hiện; nếu không có thì công việc là công việc chung của tầng/khu vực. Công việc vệ sinh phòng MUST gắn với phòng/khu vực, không gắn người cao tuổi; người thực hiện lấy theo phân công nhân viên vệ sinh theo khu vực trong ca, chưa có người nhận thì là công việc chung của khu (feature 003 FR-050). Khi phân công chăm sóc của một người cao tuổi đổi giữa ca (feature 008), công việc chưa đóng của người đó trong ca (Chưa đến hạn, Đến hạn, Quá hạn) MUST được gán lại cho người được phân công mới, hoặc thành công việc chung nếu không còn ai; công việc đã đóng giữ nguyên người thực hiện (CHK041). *(Nguồn: UC-24, 8.3, 8.4, 7.5, 13.4)*
- **FR-025**: Trưởng tầng và Điều dưỡng MUST tạo được công việc phát sinh cho người cao tuổi trong phạm vi, với các trường của FR-023; mọi nhân viên có quyền "T" ở dòng "Checklist, ghi nhận công việc" (TT, ĐD, CS, VS) MUST dùng được lệnh "Ghi nhận phát sinh" cho người cao tuổi trong phạm vi của mình: tạo công việc phát sinh giao cho chính mình và ghi kết quả trong cùng một lệnh; công việc vào thẳng Hoàn thành, không qua Chưa đến hạn hay Đến hạn, không có khung thời gian và không bị nhắc hay quá hạn. Thời điểm thực hiện MUST NOT sau thời điểm ghi, MUST nằm trong khoảng người cao tuổi có mặt (không trong khoảng vắng mặt; với bán trú, không trước giờ điểm danh đến) và trong phạm vi dữ liệu của ca người ghi (feature 002 FR-032); nhãn "ghi nhận muộn" áp như FR-035. Loại công việc được chọn MUST có vai trò thực hiện khớp vai trò của người ghi và người ghi MUST đủ chứng chỉ yêu cầu (BR-M09-01), như điều kiện "Nhận việc" ở FR-031. Nếu người cao tuổi đang có công việc cùng loại ở Đến hạn hoặc Quá hạn, hệ thống MUST hiển thị công việc đó và gợi ý ghi nhận vào nó thay vì tạo bản ghi phát sinh; nếu người ghi vẫn chọn ghi phát sinh, hai bản ghi được coi là hai lần thực hiện riêng. Kết quả được dùng cho quy tắc mục F và chi phí nháp như mọi kết quả khác (ví dụ lượng nước uống ngoài lịch, vật phẩm dùng thêm). *(Nguồn: 8.3 "công việc phát sinh", 8.5, UC-26; Clarification 2026-09-25, đề xuất Q-36)*
- **FR-026**: Vòng đời công việc MUST theo bảng dưới; không có chuyển nào khác. *(Nguồn: UC-24, UC-26, UC-27, 8.3, BR-M04-05, 06, constitution IV)*
- **FR-026a**: **(Đồng bộ spec 014, BR-M04-23, Q-163)** Khi feature 014 ghi kết quả kiểm tra chất lượng Không đạt cho một công việc chăm sóc, hệ thống MUST sinh công việc làm lại cùng loại, cùng người cao tuổi, mức quan trọng bằng công việc gốc, thời điểm dự kiến trong chính ca của danh sách kiểm tra, giao người thực hiện gốc nếu còn tên và không vắng trong ca, nếu không thì thành công việc chung của tầng; công việc làm lại trỏ về công việc gốc và kết quả kiểm tra, và theo vòng đời công việc thông thường (kể cả vào bản nháp bàn giao, BR-M04-07). Kết quả gốc không bị sửa. *(Nguồn: BR-M04-23; feature 014 FR-064)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái mới | Người thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Sinh / tạo phát sinh | Chưa đến hạn | Bộ lập lịch; Trưởng tầng, Điều dưỡng (FR-025) | FR-017 → FR-025 | — |
| — | Ghi nhận phát sinh | Hoàn thành | Người ghi (TT, ĐD, CS, VS trong phạm vi) | Kết quả hợp lệ theo loại (mục D); thời điểm thực hiện không sau thời điểm ghi (FR-025) | Kích hoạt quy tắc mục F; chi phí nháp nếu có tính phí |
| Chưa đến hạn | Tới đầu khung thời gian | Đến hạn | Bộ lập lịch | — | Công việc hiển thị "đang tới hạn" trong checklist |
| Chưa đến hạn | Hủy tự động | Hủy | Hệ thống | Vắng mặt, đã về, thay đổi kế hoạch/nguồn, trạng thái cuối | Lý do theo nguyên nhân |
| Chưa đến hạn | Hủy công việc | Hủy | Trưởng tầng (8.3) | Bắt buộc lý do | — |
| Đến hạn | Ghi nhận hoàn thành | Hoàn thành | Người được phân công, Trưởng tầng (người phụ trách ca chỉ ghi được sau khi nhận việc về mình, FR-033) | Kết quả hợp lệ theo loại (mục D); thời điểm thực hiện trong khung | Kích hoạt quy tắc mục F; chi phí nháp nếu có tính phí |
| Đến hạn | Ghi nhận không thực hiện | Không thực hiện | Như trên | Bắt buộc lý do | Không có kết quả, nên không tham gia quy tắc mục F. Người cao tuổi bỏ bữa được ghi bằng kết quả "Bỏ bữa" của lệnh Ghi nhận hoàn thành, không bằng lệnh này (sửa 2026-10-01, CHK014) |
| Đến hạn | Hết khung thời gian | Quá hạn | Bộ lập lịch | Chưa đóng | Nhắc và leo thang (mục G) |
| Quá hạn | Ghi nhận hoàn thành | Hoàn thành | Người được phân công, Trưởng tầng, người phụ trách ca | Kết quả hợp lệ; thời điểm thực hiện trong khung (ghi muộn hoặc đồng bộ sau mất kết nối, FR-035) | Như Hoàn thành; giữ lịch sử nhắc; đóng cảnh báo liên quan theo feature 007 |
| Quá hạn | Ghi nhận hoàn thành | Hoàn thành trễ | Người được phân công, Trưởng tầng, người phụ trách ca | Kết quả hợp lệ; thời điểm thực hiện sau khung; bắt buộc lý do với công việc Quan trọng, Bắt buộc | Như Hoàn thành; đóng cảnh báo liên quan theo feature 007 |
| Quá hạn | Ghi nhận không thực hiện | Không thực hiện | Như trên | Bắt buộc lý do | Như trên |
| Quá hạn | Ca sau xác nhận bàn giao có chứa công việc | Không thực hiện | Hệ thống | Chỉ công việc mức Thường; có trong nội dung bàn giao và đang Quá hạn tại thời điểm xác nhận (FR-047) | Lý do "hệ thống đóng sau bàn giao"; không chuyển sang checklist ca mới |
| Đến hạn, Quá hạn | Người cao tuổi chuyển trạng thái cuối | Không thực hiện | Hệ thống | Kết thúc lưu trú, Qua đời, Hủy tiếp nhận (FR-010) | Lý do là trạng thái cuối của người cao tuổi; yêu cầu feature 007 đóng cảnh báo quá hạn gắn công việc; không nhắc, không leo thang, không vào bàn giao |

- **FR-027**: Công việc đã Đến hạn hoặc Quá hạn MUST NOT bị hủy (kể cả tự động khi vắng mặt hoặc đã về). Khi người cao tuổi rời đi tạm thời (Tạm vắng, Điều trị tại bệnh viện, Hoạt động bên ngoài, bán trú đã về), người thực hiện hoặc Trưởng tầng MUST đóng các công việc này bằng "Không thực hiện" với lý do (ví dụ "vắng mặt"); người phụ trách ca chỉ đóng được công việc đã Quá hạn, trong phạm vi ở FR-033. Ngoại lệ duy nhất là trạng thái cuối (FR-010). Trong lúc chờ đóng, việc nhắc và leo thang của mục G vẫn chạy cho các công việc này, kể cả cảnh báo trung bình của công việc Bắt buộc; đây là chủ ý, để việc đã tới hạn trước lúc người cao tuổi rời đi không bị bỏ sót mà không có lý do được ghi (CHK060). *(Nguồn: 8.3, UC-26, UC-27; Clarification 2026-09-25, đề xuất Q-38)*
- **FR-028**: Hoàn thành, Hoàn thành trễ, Không thực hiện và Hủy là trạng thái cuối; công việc ở trạng thái cuối MUST NOT chuyển sang trạng thái khác. Mỗi lần đóng MUST ghi người đóng là nhân viên (kèm căn cứ quyền) hoặc "Hệ thống" (kèm quy tắc: FR-010, FR-047, hủy tự động), để báo cáo chất lượng theo nhân viên (18.2) tách được công việc do hệ thống đóng khỏi công việc do nhân viên đóng. *(Nguồn: 8.3, BR-M04-14, UC-26)*

#### D. Checklist theo ca, ghi nhận kết quả, đính chính

- **FR-029**: Mỗi nhân viên MUST có checklist cho ca được phân công, hiển thị từ đầu phạm vi dữ liệu của ca (feature 002 FR-032), gồm: công việc giao cho họ trong ca (chăm sóc, chuyên môn, phát sinh), công việc tồn từ ca trước (có dấu "tồn"), và công việc chung của tầng/khu vực mà vai trò và chứng chỉ của họ thực hiện được. Điều dưỡng MUST có thêm: liều thuốc của người được phân công (feature 006), việc nhận bàn giao đầu ca, kiểm tra đầu ca, cảnh báo đang mở cần xử lý (feature 007), việc lập bàn giao cuối ca (feature 008). "Kiểm tra đầu ca" và "giám sát" (8.5) là hai loại công việc của danh mục (FR-038), nhóm chuyên môn, vai trò thực hiện Điều dưỡng: hệ thống MUST sinh mỗi loại một lần mỗi ca cho mỗi điều dưỡng có phân công trong ca, gắn tầng/khu vực của ca (không gắn một người cao tuổi), khung bằng toàn bộ ca, mức quan trọng theo danh mục, kết quả "Đã thực hiện" kèm ghi chú; nội dung cần kiểm tra do cơ sở ghi ở phần nội dung của loại công việc (CHK003). *(Nguồn: 8.5, UC-25)*
- **FR-030**: Mỗi dòng checklist MUST hiển thị: người cao tuổi (họ tên, ảnh, phòng/giường), cờ nguy cơ đang gắn (feature 001 FR-038), loại công việc, nội dung, thời điểm dự kiến, khung thời gian, mức quan trọng, trạng thái, nguồn; sắp theo thời điểm dự kiến, công việc Quá hạn lên đầu. Dòng công việc gắn phòng, giường hoặc khu vực (công việc vệ sinh, kiểm tra đầu ca, giám sát) MUST hiển thị phòng/giường/khu vực thay cho người cao tuổi; checklist của Nhân viên vệ sinh MUST NOT hiển thị họ tên, ảnh hay cờ nguy cơ của người cao tuổi (feature 002 FR-022, 19.3) (CHK042). *(Nguồn: UC-25, 8.5)*
- **FR-031**: Nhân viên MUST nhận được công việc chung của tầng qua lệnh "Nhận việc" khi vai trò và chứng chỉ phù hợp (BR-M09-01); Trưởng tầng và người phụ trách ca của tầng MUST phân lại được mọi công việc chưa đóng trong tầng cho nhân viên đủ điều kiện có ca trùng thời điểm dự kiến. Mỗi lần nhận/phân lại MUST được lưu (người, thời điểm, người nhận trước và sau). *(Nguồn: UC-25, UC-27, 8.4, BR-M04-13, BR-M09-01, feature 002 FR-019a)*
- **FR-032**: Khi nhân viên được ghi nhận vắng ca (feature 008), công việc chưa đóng của họ trong ca đó MUST chuyển thành công việc chung của tầng/khu vực; công việc ở các ca khác giữ nguyên (feature 008 Q-91). Quy tắc này áp tương tự khi nhân viên nghỉ có duyệt cho một ca (feature 015 FR-029): chỉ công việc chưa đóng của họ trong các ca được duyệt nghỉ thành công việc chung. Khi đổi ca làm một phân công không chuyển được cho người nhận (feature 015 FR-024 (b)), công việc của phân công đó được gán lại theo phân công còn lại hoặc thành công việc chung như khi kết thúc phân công (feature 008 FR-027). Khi nhân viên chuyển Nghỉ việc (feature 008), công việc chưa đóng của họ trong ca hiện tại và các ca tương lai MUST chuyển thành công việc chung. Trong cả hai trường hợp Trưởng tầng MUST được thông báo. Khi người cao tuổi chuyển giường sang khu khác (feature 003 FR-031), công việc chưa đóng MUST được gán lại theo phân công của khu mới. *(Nguồn: UC-25, BR-M04-13, BR-M09-04, BR-M03-04)*
- **FR-033**: Chỉ nhân viên được giao công việc hoặc Trưởng tầng MUST ghi nhận được công việc đó (BR-M04-12). Người phụ trách ca (không phải Trưởng tầng) MUST ghi nhận thay được chỉ với công việc Quá hạn thuộc tầng/khu vực của ca và chỉ trong khoảng phạm vi của ca theo feature 002 FR-032 (giờ ca ± CFG-M15-07) — cùng phạm vi này áp cho việc đóng (FR-027), phân lại (FR-031) và xử lý việc quá hạn (FR-045) (feature 002 FR-019a); với công việc chưa Quá hạn, người phụ trách ca chỉ phân lại (FR-031) hoặc nhận việc về mình rồi ghi nhận như người được giao. Vai trò của người ghi MUST có quyền "T" ở dòng "Checklist, ghi nhận công việc" (4.4: TT, ĐD, CS, VS). Mỗi bản ghi MUST lưu: người thực hiện, thời điểm thực hiện, thời điểm ghi, người cao tuổi, công việc, kết quả, ghi chú, và căn cứ quyền (được giao, vai trò Trưởng tầng, hoặc nhiệm vụ Người phụ trách ca). *(Nguồn: BR-M04-12, 8.6, UC-26, UC-27; Clarification 2026-09-25)*
- **FR-034**: Kết quả MUST đúng loại kết quả của loại công việc; tối thiểu gồm các loại sau, và danh mục loại công việc MUST khai báo được loại nào áp dụng. *(Nguồn: UC-26, 8.6)*

| Loại công việc | Kết quả | Ràng buộc |
| --- | --- | --- |
| Ăn uống (hỗ trợ ăn) | Ăn hết / Phần lớn / Một phần / Không ăn / Bỏ bữa | Chọn một; gắn với bữa |
| Uống nước | Số ml | Số nguyên dương; vượt giới hạn trên khai báo ở loại công việc (FR-038) thì yêu cầu xác nhận lại |
| Đo chỉ số | Giá trị đo | Theo khoảng hợp lệ CFG-M06-03; lưu ở feature 007 (BR-M04-10, BR-M06-04). Giá trị bị chặn thì công việc giữ nguyên trạng thái; giá trị cần đo lại để xác nhận thì công việc chỉ đóng khi feature 007 lưu giá trị đã xác nhận hoặc người đo chọn "xử lý ngay" |
| Vận động | Tự làm / Cần hỗ trợ / Không thể | Chọn một |
| Ngủ | Thời gian ngủ, chất lượng | Chất lượng chọn từ danh mục; danh mục có nhóm "mất ngủ" dùng cho quy tắc mất ngủ kéo dài của feature 007 FR-039c (bổ sung 2026-10-01) |
| Tâm trạng | Mức độ / trạng thái | Chọn từ danh mục; có nhóm "tiêu cực" dùng cho BR-M04-11 |
| Hành vi bất thường | Có / Không; loại hành vi chọn từ danh mục; mô tả | Dùng cho BR-M04-11 (8.10) |
| Thay tã/bỉm và vật phẩm | Số lượng vật phẩm đã dùng | Số nguyên ≥ 0; là căn cứ chi phí nháp |
| Công việc khác | Đã thực hiện | Ghi chú tùy chọn |
| Uống thuốc | Không ghi ở đây | Liều thuốc xác nhận ở feature 006 |

- **FR-035**: Thời điểm thực hiện MUST mặc định là thời điểm ghi; người ghi MAY chọn thời điểm sớm hơn nhưng MUST NOT sau thời điểm ghi và MUST NOT trước đầu khung thời gian. Bản ghi có thời điểm ghi muộn hơn thời điểm thực hiện quá CFG-M04-06 (mặc định \[2 giờ\]) MUST được gắn nhãn "ghi nhận muộn"; với bản ghi đồng bộ sau khi mất kết nối, "thời điểm ghi" là thời điểm ghi trên thiết bị (BR-M04-12, Q-01 theo mặc định, feature 002 FR-041a). Trạng thái đóng MUST xét theo thời điểm thực hiện, không theo thời điểm ghi: khi công việc đã chuyển Quá hạn nhưng bản ghi (ghi muộn hoặc đồng bộ sau mất kết nối) có thời điểm thực hiện nằm trong khung thời gian, công việc MUST chuyển Hoàn thành (không phải Hoàn thành trễ), không bắt buộc lý do trễ; các lần nhắc, thông báo quá hạn đã gửi MUST được giữ trong lịch sử xử lý công việc, và cảnh báo quá hạn đang mở gắn công việc MUST được yêu cầu đóng qua feature 007 với lý do "đã thực hiện đúng khung". Khi mất kết nối, thiết bị MUST vẫn cho xem checklist đã tải lần gần nhất, kèm dấu "chưa cập nhật từ <thời điểm>", và cho ghi tạm; công việc sinh, hủy hay được người khác đóng trong lúc ngoại tuyến chỉ hiện sau khi đồng bộ. Khi đồng bộ, bản ghi cho một công việc đã bị Hủy hoặc đã được đóng MUST được xử lý như FR-036: từ chối đóng lần hai, báo người ghi và giữ nội dung để chuyển thành đính chính hoặc "Ghi nhận phát sinh" (CHK044). Với công việc đã bị hệ thống đóng Không thực hiện vì người cao tuổi chuyển trạng thái cuối (FR-010), bản ghi ghi muộn hoặc đồng bộ sau có thời điểm thực hiện trước thời điểm chuyển trạng thái cuối MUST được chấp nhận dưới dạng "Ghi nhận phát sinh" trỏ về công việc gốc, trong giới hạn quyền của feature 002 FR-041a; bản ghi có thời điểm thực hiện sau thời điểm đó MUST bị từ chối (CHK056). CFG-M04-06 (ngưỡng gắn nhãn) và CFG-M15-07 (khoảng phạm vi sau ca, feature 002 FR-032) là hai tham số độc lập: người được giao chỉ ghi được khi còn trong phạm vi ca, nên khi CFG-M15-07 nhỏ hơn hoặc bằng CFG-M04-06 thì bản ghi "ghi nhận muộn" của người đã hết ca chỉ còn do Trưởng tầng ghi thay hoặc đến từ đồng bộ ngoại tuyến; Quản lý viện đổi một tham số cần xét tham số kia (CHK024). *(Nguồn: UC-26, BR-M04-12, 8.6, Q-01; Clarification 2026-09-25)*
- **FR-036**: Khi hai lần ghi nhận cùng một công việc tới gần như đồng thời, chỉ lần được hệ thống nhận trước (theo thời điểm hệ thống nhận bản ghi, kể cả với bản ghi ngoại tuyến có thời điểm trên thiết bị sớm hơn) MUST đóng công việc; lần sau MUST bị từ chối, người ghi MUST được báo ai đã đóng công việc, và nội dung bị từ chối MUST được giữ để người ghi chuyển thành đính chính hoặc "Ghi nhận phát sinh" nếu đó là một lần thực hiện khác. *(Nguồn: 8.6, UC-26)*
- **FR-037**: Kết quả đã ghi MUST NOT sửa hay xóa; sai sót MUST được xử lý bằng bản đính chính có lý do, trỏ đúng một bản gốc (DBR-23), do người ghi gốc, Trưởng tầng hoặc người phụ trách ca của tầng trong thời gian ca tạo (1.5, feature 002 FR-019b). Bản đính chính loại "Hủy ghi nhận" MUST giữ bản gốc xem được; công việc của kết quả bị hủy MUST giữ nguyên trạng thái đóng (FR-028), kết quả mang dấu "đã hủy ghi nhận" và từ đó được coi là không có kết quả trong các quy tắc mục F (không cộng vào tổng lượng nước, không tính và không làm đứt chuỗi bữa ăn); nếu việc đó vẫn cần làm thì tạo công việc phát sinh (CHK038). Giá trị hiện hành sau đính chính MUST được dùng cho các lần đánh giá quy tắc mục F kể từ thời điểm đính chính; cảnh báo đã tạo không tự rút lại (xử lý ở feature 007). Mọi đính chính trên kết quả có tính phí MUST phát sự kiện cho feature 010. *(Nguồn: UC-26, BR-M04-14, 1.5)*

#### E. Danh mục loại công việc

- **FR-038**: Quản lý viện MUST cấu hình được danh mục loại công việc, mỗi loại gồm: tên, nhóm (vệ sinh, tắm, thay quần áo, hỗ trợ ăn, hỗ trợ uống nước, hỗ trợ đi vệ sinh, vận động, phục hồi, đo chỉ số, hoạt động, vệ sinh phòng, phát sinh, …), loại kết quả (FR-034), vai trò thực hiện mặc định, chứng chỉ/đào tạo yêu cầu, mức quan trọng mặc định, khung thời gian mặc định (nếu trống dùng CFG-M04-02), có tính phí mặc định và dịch vụ/vật phẩm tương ứng, giới hạn trên hợp lý của giá trị số (ví dụ ml mỗi lần uống). Loại đã được tham chiếu MUST chỉ Ngừng hiệu lực, không xóa; loại Ngừng hiệu lực MUST NOT chọn được cho mục kế hoạch mới. Sửa một loại công việc MUST chỉ áp cho mục kế hoạch lập sau thời điểm sửa và cho công việc phát sinh tạo sau đó; mục của phiên bản kế hoạch đã gửi duyệt hoặc đang Hiệu lực giữ giá trị riêng (FR-004), còn công việc đã sinh giữ giá trị lúc sinh; riêng yêu cầu chứng chỉ mới MUST áp ngay cho lần giao, nhận hay phân lại kế tiếp (BR-M09-01). *(Nguồn: 8.3, 8.6, 13.4, 1.5 nhóm 1; mục 4.2 chưa có UC riêng, xem điểm báo lại 12)*

#### F. Quy tắc kích hoạt từ kết quả

- **FR-039**: Mục tiêu lượng nước MUST là mục tiêu ml/ngày của mục kế hoạch loại "hỗ trợ uống nước" trong phiên bản kế hoạch Hiệu lực của ngày đó; người cao tuổi không có mục tiêu này MUST NOT bị kiểm tra. Mục tiêu dùng để so MUST là mục tiêu điều chỉnh = mục tiêu × (số giờ người đó có mặt từ 00:00 tới mốc kiểm tra) ÷ (số giờ từ 00:00 tới mốc kiểm tra); với người có mặt suốt khoảng này, mục tiêu điều chỉnh bằng mục tiêu. Thời gian có mặt của bán trú tính từ giờ điểm danh đến; của nội trú loại trừ khoảng Tạm vắng, Điều trị tại bệnh viện, Hoạt động bên ngoài. Tại mốc giờ của CFG-M04-03 (mặc định \[16:00 / 60% mục tiêu\]) mỗi ngày, với mỗi người cao tuổi có mục tiêu và đang có mặt tại mốc đó, nếu tổng lượng nước hiện hành (sau đính chính) ghi trong ngày dưới tỷ lệ ngưỡng của mục tiêu điều chỉnh thì hệ thống MUST yêu cầu feature 007 tạo cảnh báo nhẹ "uống nước thiếu" (gộp theo BR-M05-02) và sinh thêm một công việc phát sinh "hỗ trợ uống nước" mức Quan trọng, thời điểm dự kiến là mốc kiểm tra, khung thời gian theo loại công việc (hoặc CFG-M04-02), cho người thực hiện theo FR-024; mỗi người cao tuổi có tối đa một công việc bổ sung như vậy mỗi ngày (FR-021). Số giờ có mặt tính tới phút, không làm tròn; mục tiêu điều chỉnh làm tròn xuống tới ml. Với người bán trú, công thức chia đều mục tiêu ngày cho khoảng từ 00:00 tới mốc kiểm tra là chủ ý của Q-30 (so theo tỷ lệ thời gian có mặt); cơ sở thấy ngưỡng quá thấp với bán trú thì đặt mục tiêu ml/ngày của mục kế hoạch cho phù hợp, không đổi công thức (CHK015). *(Nguồn: UC-26, BR-M04-08, CFG-M04-03; Clarification 2026-09-25, đề xuất Q-30)*
- **FR-040**: Khi một bữa được ghi, hệ thống MUST đếm số bữa liên tiếp gần nhất có kết quả "Một phần", "Không ăn" hoặc "Bỏ bữa" (tính qua các ngày; bữa bị Hủy vì vắng mặt hoặc không có kết quả không tính và không làm đứt chuỗi; một bữa có kết quả khác đặt chuỗi về 0). Khi chuỗi đạt CFG-M04-04 (mặc định \[3\]), hệ thống MUST yêu cầu feature 007 tạo cảnh báo nhẹ "ăn kém kéo dài" cho Điều dưỡng phụ trách (gộp theo BR-M05-02) và gửi sự kiện "ăn kém kéo dài" kèm các bữa trong chuỗi cho feature 011 để tạo (hoặc gộp nguồn vào) yêu cầu xem lại chế độ ăn (feature 011 FR-058, BR-M08-05). Spec này MUST NOT gửi nhắc riêng tới Dinh dưỡng viên; thông báo tới Dinh dưỡng viên do feature 011 gửi. *(Nguồn: UC-26, BR-M04-09 (làm rõ, spec 011), BR-M08-05, CFG-M04-04, 9.3 mức Nhẹ)*
- **FR-040a**: Với người cao tuổi có suất đặc biệt ở một bữa, hoặc thuộc danh sách cần đối chiếu khi phục vụ của bữa đó (feature 011 FR-036a), công việc "hỗ trợ ăn" của bữa MUST NOT ghi được kết quả khi chưa có xác nhận phục vụ thành công do feature 011 ghi nhận (feature 011 FR-049, FR-050). Công việc của bữa bị feature 011 báo là bỏ vì vắng mặt MUST chuyển Hủy theo FR-022. *(Nguồn: BR-M08-14 "trước khi ghi nhận kết quả ăn uống (8.6)"; đồng bộ spec 011)*
- **FR-041**: Kết quả đo chỉ số MUST được chuyển ngay cho feature 007 để so ngưỡng; spec này không tạo cảnh báo chỉ số. *(Nguồn: UC-26, BR-M04-10)*
- **FR-042**: Khi tâm trạng thuộc nhóm "tiêu cực" hoặc hành vi bất thường được ghi CFG-M04-05 (mặc định \[3\]) ngày liên tiếp, hệ thống MUST yêu cầu feature 007 tạo cảnh báo nhẹ và đính kèm danh sách hoạt động gợi ý theo sở thích do feature 014 cung cấp. Ngày không có ghi nhận tâm trạng làm đứt chuỗi. Cách đếm này khác chuỗi bữa ăn ở FR-040 một cách có chủ ý: bữa ăn là sự kiện có lịch, bữa thiếu kết quả là thiếu dữ liệu nên được bỏ qua; còn BR-M04-11 đòi tâm trạng tiêu cực được ghi "N ngày liên tiếp", nên ngày không có ghi nhận không được tính là ngày tiêu cực (CHK023). *(Nguồn: UC-26, BR-M04-11, CFG-M04-05)*

#### G. Công việc quá hạn, leo thang và bàn giao

- **FR-043**: Khi công việc chuyển Quá hạn, hệ thống MUST gửi nhắc mức Nhẹ (feature 009 FR-043b, Q-117) cho người thực hiện (hoặc mọi nhân viên đủ điều kiện của tầng nếu là công việc chung); các thông báo leo thang ở FR-044 (a), (b) ở mức Trung bình. Lần nhắc này MUST chỉ gửi một lần cho mỗi công việc, tại lúc chuyển Quá hạn; spec này không nhắc lặp theo chu kỳ. Các bước sau đó là leo thang theo FR-044 và, với công việc Bắt buộc, leo thang của cảnh báo theo feature 007. "Số lần đã nhắc" ở FR-046 đếm lần nhắc này cùng các lần báo leo thang đã gửi (CHK004). *(Nguồn: UC-27, BR-M04-05, 8.7)*
- **FR-044**: Ngoài FR-043, theo mức quan trọng: (a) Thường — nếu vẫn chưa đóng sau CFG-M04-12 (đề xuất, mặc định \[30 phút\]) tính từ lúc chuyển Quá hạn thì báo người phụ trách ca của tầng (Clarification 2026-09-25, đề xuất Q-31); (b) Quan trọng — thông báo ngay cho Trưởng tầng và người phụ trách ca của tầng; (c) Bắt buộc — yêu cầu feature 007 tạo cảnh báo mức trung bình gắn công việc (leo thang tiếp theo BR-M05-01). Khi đóng công việc Quan trọng hoặc Bắt buộc đã Quá hạn, lý do MUST bắt buộc. Người nhận MUST được xác định theo phân công hiện tại (BR-M13-04). *(Nguồn: UC-27, BR-M04-06, 8.7)*
- **FR-045**: Trưởng tầng và người phụ trách ca của tầng (trong thời gian ca, feature 002 FR-019a) MUST xử lý được công việc Quá hạn trong tầng: phân lại (FR-031), ghi nhận thay, đóng Không thực hiện có lý do. Mỗi lần xử lý MUST lưu người, căn cứ quyền (vai trò hoặc nhiệm vụ), thời điểm, lý do. Quản lý viện MUST xem được danh sách công việc Quá hạn toàn viện. *(Nguồn: UC-27, 4.4 dòng "Xử lý việc quá hạn": QL X, TT T⁹)*
- **FR-046**: Công việc chưa đóng (Đến hạn, Quá hạn) và công việc Chưa đến hạn có thời điểm dự kiến trong ca mà chưa thực hiện tại thời điểm lập bản nháp bàn giao (CFG-M09-04, mặc định \[30 phút\] trước kết ca) MUST được cung cấp cho bản nháp bàn giao (feature 008) kèm: người cao tuổi, loại, thời điểm dự kiến, trạng thái, mức quan trọng, người thực hiện, số lần đã nhắc, cảnh báo liên quan. *(Nguồn: UC-27, UC-52, BR-M04-07, BR-M09-06)*
- **FR-047**: Khi bàn giao được ca sau xác nhận (feature 008), công việc chưa đóng MUST chuyển sang checklist ca mới: giao cho nhân viên ca mới được phân công người cao tuổi đó (khớp vai trò), nếu không có thì thành công việc chung của tầng; công việc giữ nguyên trạng thái, thời điểm dự kiến và lịch sử, có dấu "tồn từ ca trước". Ngoại lệ: công việc mức Thường đang Quá hạn MUST được hệ thống tự chuyển Không thực hiện với lý do "hệ thống đóng sau bàn giao" tại thời điểm ca sau xác nhận bàn giao, nếu công việc vừa có trong nội dung bàn giao đó (ở bất kỳ trạng thái chưa đóng nào lúc lập) vừa đang Quá hạn tại thời điểm xác nhận; công việc đã được đóng trước lúc xác nhận giữ kết quả của người đóng. Công việc bị hệ thống đóng như vậy MUST NOT chuyển sang checklist ca mới; nội dung bàn giao đã xác nhận vẫn giữ công việc đó. Công việc Quan trọng và Bắt buộc Quá hạn MUST NOT tự đóng dù qua bao nhiêu ca, và tiếp tục chuyển sang ca sau cho tới khi có người đóng kèm lý do. *(Nguồn: BR-M09-08, 8.5, 8.7, 1.3, UC-27; Clarification 2026-09-25, đề xuất Q-32, Q-37)*
- **FR-047a**: Từ giờ bắt đầu ca sau, nếu bàn giao chưa được xác nhận (feature 008 FR-044a), công việc chưa đóng mà người được giao thuộc ca trước và không có tên trong ca nào đang diễn ra MUST tạm thành công việc chung của tầng/khu vực, giữ trạng thái, thời điểm dự kiến, lịch sử và mang dấu "tồn, chờ xác nhận bàn giao"; nhân viên đủ điều kiện của ca sau nhận việc hoặc được phân lại theo FR-031. Khi bàn giao được xác nhận, FR-047 áp cho các công việc này; công việc đã được nhận hoặc đã đóng trong lúc chờ giữ kết quả đó. Ngoại lệ tự đóng công việc Thường Quá hạn của FR-047 MUST chỉ áp cho công việc vẫn là công việc chung chưa ai nhận tại thời điểm xác nhận; công việc đã được nhân viên ca sau nhận hoặc được phân lại MUST NOT bị tự đóng (feature 008 Q-93). *(Nguồn: BR-M09-08, BR-M04-13; feature 008 FR-044a, Q-85)*

#### H. Thời khóa biểu cá nhân

- **FR-048**: Trưởng tầng và Điều dưỡng MUST xem được thời khóa biểu theo ngày của người cao tuổi trong phạm vi, tổng hợp: công việc (mọi nguồn), liều thuốc (feature 006), buổi hoạt động (feature 014), lịch thăm (feature 012), lịch đến/về với bán trú; mỗi mục ghi nguồn và trạng thái. Thời khóa biểu chỉ để xem; thay đổi MUST được thực hiện ở nguồn. *(Nguồn: 8.2; mục 4.2 chưa có UC riêng, xem điểm báo lại 12)*

#### I. Quyền

- **FR-049**: Quyền của feature này MUST khớp Permission Matrix 4.4 và phạm vi theo vai trò (feature 002 FR-034): "Kế hoạch chăm sóc" (UC-22, 23) QL X, TT X, BS D, ĐD T, CS P; "Checklist, ghi nhận công việc" (UC-25, 26) QL X, TT T, ĐD T, CS T, VS T; "Xử lý việc quá hạn" (UC-27) QL X, TT T và người phụ trách ca (⁹). Điểm danh bán trú (UC-28) do Hành chính, Nhân viên chăm sóc và (**bổ sung 2026-09-28, Q-214**) Điều dưỡng thực hiện; Trưởng tầng xem được. **(Q-214)** Điều dưỡng có mọi quyền thực hiện của Nhân viên chăm sóc ở spec này (Phụ lục 27 ²⁷): loại công việc khai báo vai trò yêu cầu là Nhân viên chăm sóc (FR-038) cũng được phân cho và ghi nhận bởi Điều dưỡng; chiều ngược lại không áp dụng. Ngoài ma trận hiện hành, Bác sĩ MUST xem được (không ghi, không đính chính) mọi kết quả ghi nhận của người cao tuổi trong phạm vi toàn viện, và Dinh dưỡng viên MUST xem được (không ghi) chỉ kết quả ăn uống và lượng nước; hai vai trò này MUST NOT thấy checklist của nhân viên hay thao tác trên công việc. *(Nguồn: 4.4, 4.2 UC-28; Clarification 2026-09-25 — đề xuất bổ sung ma trận 4.4, xem điểm báo lại 7)*

#### J. Trao đổi với feature khác

- **FR-050**: Các sự kiện spec này gửi đi và nhận vào MUST gồm tối thiểu dữ liệu ở bảng dưới; mỗi sự kiện gửi đi MUST được xử lý "hoặc toàn bộ, hoặc không" cùng lệnh gây ra nó (feature 000). *(Nguồn: 1.3, mục Phạm vi, UC-24 → UC-27)*

| Chiều | Feature | Sự kiện | Dữ liệu tối thiểu |
| --- | --- | --- | --- |
| Gửi | 007 | Tạo/gộp cảnh báo (công việc Bắt buộc quá hạn, uống nước thiếu, ăn kém kéo dài, tâm trạng) | Người cao tuổi, loại cảnh báo, mức, công việc/kết quả nguồn, thời điểm |
| Gửi | 007 | Yêu cầu đóng cảnh báo gắn công việc | Cảnh báo, công việc, lý do (đã thực hiện đúng khung, đã đóng, trạng thái cuối) |
| Gửi | 007 | Giá trị đo chỉ số | Người cao tuổi, loại chỉ số, giá trị, thời điểm đo, người đo, công việc |
| Gửi | 008 | Công việc chưa đóng cho bản nháp bàn giao; cập nhật khi đóng | Như FR-046 |
| Gửi | 009 | Nhắc và thông báo (FR-019, FR-043, FR-044, FR-011, FR-032) | Nguồn kích hoạt, người nhận theo phân công hiện tại, mức (BR-M13-04, 05) |
| Gửi | 010 | Kết quả có tính phí; đính chính hoặc hủy ghi nhận trên kết quả đó | Kết quả ghi nhận (nguồn, DBR-15), người cao tuổi, loại công việc, dịch vụ/vật phẩm, số lượng, thời điểm thực hiện |
| Gửi | 004, 010, 011 | Trạng thái có mặt bán trú theo ngày | Như FR-016 |
| Gửi | 016 | Dữ liệu cho báo cáo chăm sóc và dashboard (18.2, 18.5): công việc theo trạng thái, mức quan trọng, loại, tầng/khu, ca; người đóng và căn cứ đóng (FR-028); nhãn "ghi nhận muộn" (BR-M04-12); bản ghi ngoại tuyến và trạng thái "chờ xem lại"; trạng thái có mặt bán trú theo ngày (FR-016) | Công việc, người cao tuổi, tầng/khu, thời điểm dự kiến, thời điểm thực hiện (kể cả thời điểm trên thiết bị), trạng thái và lịch sử trạng thái, người thực hiện, người đóng, quy tắc đóng, nhãn |
| Nhận | 001 | Đánh giá lại được chấp nhận; hoạt động mẫu; chuyển trạng thái người cao tuổi | Người cao tuổi, thời điểm, trạng thái mới |
| Nhận | 003 | Chuyển giường thành công | Người cao tuổi, khu cũ, khu mới, thời điểm |
| Nhận | 004 | Lịch đến bán trú; báo vắng | Người cao tuổi, ngày, giờ đến/về dự kiến, thời điểm báo |
| Nhận | 006 | Liều thuốc hiển thị trong checklist | Người cao tuổi, liều, thời điểm dự kiến, trạng thái |
| Nhận | 007 | Lịch đo; sự cố ngã; kết quả so ngưỡng hoặc yêu cầu đo lại | Người cao tuổi, lịch/sự cố, thời điểm |
| Nhận | 008 | Phân công chăm sóc; vắng ca, nghỉ việc; bàn giao đã xác nhận | Ca, tầng/khu vực, nhân viên, người cao tuổi |
| Nhận | 011, 014 | Lịch bữa (feature 011 FR-042) và bữa bị bỏ vì vắng mặt; kết quả xác nhận phục vụ suất đặc biệt (feature 011 FR-049); đăng ký buổi hoạt động và hủy đăng ký; danh sách hoạt động gợi ý | Người cao tuổi, bữa/buổi, thời điểm, có suất đặc biệt hoặc cần đối chiếu, xác nhận phục vụ |
| Gửi | 011 | Sự kiện "ăn kém kéo dài" (FR-040) | Người cao tuổi, các bữa trong chuỗi, thời điểm |

- **FR-051**: Spec này quyết định công việc nào **có tính phí** (mục kế hoạch hoặc mặc định của loại công việc) và **số lượng** đã dùng; đơn giá, việc khoản đó có thuộc gói hợp đồng hay không (số tiền 0) và thời điểm chốt do feature 010 quyết định theo DBR-15, DBR-16. *(Nguồn: 8.1 "có tính phí", BR-M11-01, DBR-15, DBR-16)*

#### K. Việc theo lịch của Bộ lập lịch

- **FR-052** *(bổ sung, CHK034)*: Khi Bộ lập lịch bỏ lỡ một hoặc nhiều lần chạy, lần chạy kế tiếp MUST xử lý bù: (a) sinh công việc cho mọi ngày/ca đã tới thời điểm sinh mà chưa được sinh, không tạo trùng (FR-021); công việc có khung thời gian đã qua lúc chạy bù được sinh và chuyển thẳng theo đồng hồ lúc đó (Đến hạn hoặc Quá hạn), để việc phải làm không mất dấu; (b) chuyển Đến hạn, Quá hạn cho mọi công việc đã tới mốc; thời điểm chuyển ghi là thời điểm chạy thực tế, kèm thời điểm dự kiến; các lần nhắc bị dồn lại được gộp thành một lần cho mỗi người nhận; (c) chuyển phiên bản kế hoạch Chờ hiệu lực sang Hiệu lực với ngày hiệu lực giữ nguyên, rồi áp FR-006; (d) chuyển "Vắng không báo" cho buổi bán trú đã quá CFG-M02-07; (e) chạy mốc kiểm tra nước CFG-M04-03 một lần nếu còn trong ngày đó, và bỏ qua nếu đã sang ngày khác; (f) gắn dấu "quá hạn" cho yêu cầu xem xét kế hoạch đã quá CFG-M04-08. Tham số dùng khi chạy bù là giá trị hiện hành tại lúc chạy bù (feature 000 FR-018). *(Nguồn: NFR-04, NFR-13, BR-M04-01; feature 000 FR-037a)*

### Key Entities *(include if feature involves data)*

- **Phiên bản kế hoạch chăm sóc (KE_HOACH_CHAM_SOC)** – nhóm 2: người cao tuổi, số phiên bản, ngày hiệu lực, ngày hết hiệu lực, trạng thái (Nháp / Chờ duyệt / Chờ hiệu lực / Hiệu lực / Hết hiệu lực / Đã hủy), người lập, người duyệt, thời điểm duyệt, tự duyệt, lý do thay đổi.
- **Mục kế hoạch (MUC_KE_HOACH)** – nhóm 2 (theo phiên bản): phiên bản, loại công việc, nội dung, tần suất, mốc giờ, khung thời gian cho phép, vai trò thực hiện, kết quả cần ghi, mức quan trọng, có tính phí, mục tiêu định lượng (ví dụ ml nước/ngày, dùng cho FR-039).
- **Yêu cầu xem xét kế hoạch** – nhóm 2: người cao tuổi, các nguồn (đánh giá lại, sự cố ngã), người được giao, hạn, trạng thái, dấu quá hạn, cách xử lý (phiên bản mới hoặc "không cần thay đổi" kèm lý do).
- **Loại công việc** – nhóm 1: tên, nhóm, loại kết quả, vai trò, chứng chỉ yêu cầu, mức quan trọng mặc định, khung mặc định, tính phí mặc định, dịch vụ/vật phẩm, giới hạn trên của giá trị số, trạng thái hiệu lực.
- **Trạng thái có mặt bán trú (CO_MAT_BAN_TRU)** – nhóm 2: người cao tuổi, ngày, giờ đến/về dự kiến, trạng thái, giờ đến, giờ về, người điểm danh, báo vắng muộn.
- **Công việc (CONG_VIEC)** – nhóm 2: người cao tuổi hoặc phòng/khu vực, loại, nguồn sinh, thời điểm dự kiến, khung cho phép, mức quan trọng, vai trò, người thực hiện hoặc chung của tầng, ca, tầng/khu vực, có tính phí, trạng thái, lý do, người đóng (nhân viên hoặc Hệ thống kèm quy tắc, FR-028), dấu "tồn từ ca trước"; nguồn sinh gồm cả "Ghi nhận phát sinh" (FR-025).
- **Kết quả ghi nhận (KET_QUA_GHI_NHAN)** – nhóm 3: công việc, giá trị kết quả, người thực hiện, thời điểm thực hiện, thời điểm ghi (trên thiết bị) và thời điểm đồng bộ, nhãn ghi nhận muộn, ghi chú, liên kết bản ghi chỉ số (với đo chỉ số), căn cứ quyền của người ghi (được giao, vai trò Trưởng tầng, nhiệm vụ Người phụ trách ca – FR-033), các bản đính chính.
- **Lịch sử xử lý công việc** – nhóm 3: công việc, loại sự kiện (nhắc, báo người phụ trách ca theo CFG-M04-12, báo trưởng tầng, tạo cảnh báo, yêu cầu đóng cảnh báo, nhận việc, phân lại, đưa vào bàn giao, chuyển ca, hệ thống đóng sau bàn giao, hệ thống đóng do trạng thái cuối), người, căn cứ quyền, thời điểm, lý do.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Với bộ kiểm thử 20 người cao tuổi (nội trú có/không có kế hoạch, Tạm vắng, bán trú có mặt/vắng) trong 7 ngày giả lập, 100% công việc được sinh khớp danh sách tính tay (loại, thời điểm, khung, mức quan trọng, người thực hiện); 0 công việc trùng khi chạy lại việc sinh 3 lần.
- **SC-002**: 0 công việc được sinh cho người cao tuổi đang vắng mặt hoặc cho người bán trú không ở trạng thái "Có mặt" tại thời điểm dự kiến.
- **SC-003**: Sau mỗi lần đổi phiên bản kế hoạch trong bộ kiểm thử, 100% công việc Chưa đến hạn từ ngày hiệu lực được sinh lại theo phiên bản mới và 0 công việc đã ở trạng thái khác bị thay đổi.
- **SC-004**: 0 thao tác sửa thành công trên phiên bản kế hoạch Hiệu lực hoặc kết quả đã ghi; 100% phiên bản Hiệu lực có người duyệt có quyền Duyệt kế hoạch chăm sóc tại thời điểm duyệt.
- **SC-005**: 100% công việc hết khung thời gian mà chưa đóng chuyển Quá hạn và người thực hiện nhận nhắc trong vòng 1 phút sau khi hết khung (đo từ thời điểm hết khung theo đồng hồ hệ thống tới lúc lần nhắc được tạo, với dữ liệu ở quy mô của SC-008; là mục tiêu nghiệm thu; CHK028); 100% công việc Bắt buộc quá hạn có cảnh báo mức trung bình; 100% công việc Thường còn Quá hạn sau CFG-M04-12 được báo người phụ trách ca, và 0 lần báo cho công việc đã đóng trước mốc đó.
- **SC-006**: 100% công việc chưa đóng lúc lập bản nháp bàn giao có mặt trong bản nháp, và sau khi bàn giao được xác nhận, 100% công việc Quan trọng, Bắt buộc và công việc chưa Quá hạn xuất hiện trong checklist ca sau, 100% công việc Thường Quá hạn được đóng Không thực hiện với lý do hệ thống; 0 công việc chưa đóng "biến mất" giữa hai ca mà không có bản ghi đóng.
- **SC-007**: Trong 30 kịch bản kiểm thử quy tắc nước uống, ăn kém và tâm trạng (gồm đúng ngưỡng, dưới ngưỡng, qua ngày, có vắng mặt, có đính chính, có "Ghi nhận phát sinh"), 100% cảnh báo được tạo đúng và 0 cảnh báo thừa.
- **SC-008**: Với dữ liệu của một cơ sở 100 người cao tuổi và một nhân viên chăm sóc được phân công 15 người trong ca, nhân viên thấy toàn bộ công việc của ca trong không quá 3 giây tính từ lúc mở checklist; ghi nhận một kết quả thường gặp (uống nước, bữa ăn, vận động) trong không quá 15 giây, đo từ lúc chạm vào công việc tới lúc lưu xong. Các con số này là mục tiêu nghiệm thu, không phải tham số cấu hình.
- **SC-009**: 100% người bán trú được điểm danh đến có công việc sẵn sàng trong checklist trong vòng 1 phút sau điểm danh (đo từ lúc lệnh điểm danh đến được lưu tới lúc công việc hiện trong checklist của nhân viên được phân công, với dữ liệu ở quy mô của SC-008; là mục tiêu nghiệm thu; CHK028).
- **SC-010**: 0 phiên bản kế hoạch có ngày hiệu lực bằng hoặc sớm hơn ngày duyệt; 100% đánh giá lại được chấp nhận và sự cố ngã tạo (hoặc gộp vào) yêu cầu xem xét kế hoạch, và 100% yêu cầu còn Mở khi tới hạn CFG-M04-08 được báo Trưởng tầng.
- **SC-011**: 0 lần ghi nhận thành công bởi người không được giao, không phải Trưởng tầng, hoặc là người phụ trách ca trên công việc chưa Quá hạn; 100% bản ghi có thời điểm ghi muộn hơn thời điểm thực hiện quá CFG-M04-06 mang nhãn "ghi nhận muộn".
- **SC-012**: Khi người cao tuổi chuyển trạng thái cuối, 100% công việc chưa đóng của người đó được đóng trong cùng lệnh (Hủy hoặc Không thực hiện do Hệ thống), 0 cảnh báo quá hạn gắn các công việc đó còn mở, và 0 công việc đó xuất hiện trong checklist ca sau.

## Assumptions

- Số feature `005` theo bản đồ feature ở mục 4.2 `docs/phan-tich-yeu-cau.md` (UC-22 → UC-28) và trùng số tuần tự kế tiếp.
- Quy tắc dùng chung theo feature 000; trạng thái người cao tuổi theo feature 001; quyền và phạm vi theo feature 002; lịch đến bán trú, báo vắng và tính phí theo feature 004. Spec này không lặp lại các quy tắc đó.
- Q-07 được dùng theo mặc định: quyền Duyệt kế hoạch chăm sóc thuộc Bác sĩ, Quản lý viện được gán thêm cho điều dưỡng; mặc định này nhất quán với Q-15 đã chốt.
- Q-01 được dùng theo mặc định cho phần đã chốt ở feature 002 (cho ghi tạm, đồng bộ sau, kiểm tra quyền theo thời điểm thiết bị); trạng thái công việc khi đồng bộ muộn xét theo thời điểm thực hiện (FR-035, Clarification 2026-09-25).
- Vòng đời phiên bản kế hoạch có thêm Chờ duyệt, Chờ hiệu lực và Đã hủy ngoài ba trạng thái Nháp → Hiệu lực → Hết hiệu lực ở 8.1, để thể hiện bước duyệt của BR-M04-19; ngày hiệu lực sớm nhất là ngày hôm sau ngày duyệt (đề xuất Q-34).
- Mỗi người cao tuổi chỉ có một phiên bản đang soạn/chờ tại một thời điểm, để tránh hai thay đổi song song. Yêu cầu xem xét kế hoạch được coi là Đã xử lý ngay khi phiên bản mới được gửi duyệt; nếu phiên bản đó sau này bị Trả lại hoặc Hủy, yêu cầu không mở lại (FR-011) — nếu cơ sở muốn theo dõi tới khi phiên bản được duyệt, cần chốt lại điểm này.
- Công việc "Đến hạn" bắt đầu tại đầu khung thời gian (thời điểm dự kiến trừ khung) và "Quá hạn" tại cuối khung; không cho ghi nhận khi công việc còn Chưa đến hạn.
- Công việc bị hủy do vắng mặt chỉ gồm công việc Chưa đến hạn, đúng với sơ đồ 8.3; công việc đã Đến hạn/Quá hạn khi vắng tạm thời phải được đóng thủ công (FR-027). Ngoại lệ là người cao tuổi chuyển trạng thái cuối: hệ thống tự đóng (FR-010, đề xuất Q-38).
- Công việc cho người Tạm vắng được sinh khi trở về cho phần còn lại của ngày/ca, thay vì sinh trước theo "dự kiến có mặt".
- "Cảnh báo trưởng tầng" với công việc Quan trọng (BR-M04-06) được hiểu là thông báo, không tạo bản ghi cảnh báo; công việc Bắt buộc mới tạo bản ghi cảnh báo (CANH_BAO). Lý do bắt buộc khi đóng áp cho cả Quan trọng và Bắt buộc (8.7).
- Chuỗi bữa ăn kém được đếm qua các ngày; cảnh báo ăn kém là mức Nhẹ (9.3 xếp "bỏ bữa" vào mức Nhẹ).
- Danh mục loại công việc do Quản lý viện cấu hình (quyền C), vì Permission Matrix không có dòng riêng.
- Thời khóa biểu cá nhân chỉ để xem; không có quyền riêng ngoài phạm vi dữ liệu.
- Tham số mới đề xuất: CFG-M04-12 (thời gian từ lúc công việc Thường Quá hạn tới lúc báo người phụ trách ca, mặc định 30 phút). **(Cập nhật 2026-10-01)** Q-30 → Q-38 và CFG-M04-12 đã có ở mục 24.2 và Phụ lục 25, không còn là đề xuất; các chỗ còn ghi "đề xuất" trong spec là ghi nhận thời điểm clarify (CHK049).
- Q-01 và Q-07 đã được chốt theo đúng giá trị mặc định ngày 2026-10-01 (mục 24.2); các câu "theo mặc định Q-01", "theo mặc định Q-07" trong spec vẫn đúng và không cần nhãn [NEEDS CLARIFICATION] (Q-204) (CHK047).

## Điểm cần báo lại về tài liệu nguồn

*(Câu mở đầu lúc lập spec: "Chưa sửa `docs/`".)* **(2026-09-29, checklist cross-feature CHK026)** Tình trạng hiện tại của từng điểm ghi ở các đoạn có ngày bên dưới; điểm không được nhắc ở đoạn nào vẫn là "còn chờ chủ tài liệu xác nhận" (xem CHK027).

1. **UC-28 (Điểm danh bán trú đến/về) không có dòng trong Permission Matrix 4.4**; spec lấy actor của UC-28 (Hành chính, Nhân viên chăm sóc) làm người thực hiện, Trưởng tầng xem (FR-049).
2. **Vòng đời phiên bản kế hoạch ở 8.1** chỉ có Nháp → Hiệu lực → Hết hiệu lực, không có bước chờ duyệt, trả lại, thu hồi dù BR-M04-19 yêu cầu duyệt; spec bổ sung Chờ duyệt, Chờ hiệu lực, Đã hủy (FR-003).
3. **Sơ đồ trạng thái 8.3** lệch với bảng FR-026 ở năm điểm, cần cập nhật đồng bộ: (a) Quá hạn → Hoàn thành khi thời điểm thực hiện nằm trong khung (FR-035, Q-01); (b) — → Hoàn thành qua lệnh "Ghi nhận phát sinh" (FR-025, Q-36); (c) Quá hạn → Không thực hiện do Hệ thống khi ca sau xác nhận bàn giao, chỉ với công việc Thường (FR-047, Q-32, Q-37); (d) Đến hạn/Quá hạn → Không thực hiện do Hệ thống khi người cao tuổi chuyển trạng thái cuối (FR-010, Q-38); (e) Hủy thủ công chỉ do Trưởng tầng (Q-35). Ngoài ra BR-M04-02, 04 nói "công việc ... tự động chuyển Hủy"; spec hiểu là chỉ công việc Chưa đến hạn, công việc Đến hạn/Quá hạn khi vắng tạm thời được đóng tay (FR-027).
4. **"Mục tiêu" lượng nước ở BR-M04-08** chưa có nguồn trong tài liệu; đã chốt khi clarify: lấy từ mục kế hoạch và tính theo tỷ lệ giờ có mặt (FR-039). Cần phản ánh vào BR-M04-08 và thêm Q-30 vào mục 24.2.
5. **Mốc báo người phụ trách ca với công việc Thường quá hạn** (8.7 "Nhắc người thực hiện → Báo người phụ trách ca") chưa có tham số; đã chốt khi clarify: CFG-M04-12, mặc định 30 phút từ lúc Quá hạn (FR-044). Cần thêm CFG-M04-12 vào Phụ lục 25 và Q-31 vào mục 24.2.
6. **BR-M04-06 "cảnh báo trưởng tầng"** với công việc Quan trọng không nói rõ là thông báo hay bản ghi cảnh báo; spec hiểu là thông báo.
7. **Bác sĩ và Dinh dưỡng viên không có quyền xem ở dòng "Checklist, ghi nhận công việc"** (4.4), trong khi BR-M04-09 nhắc dinh dưỡng viên về bữa ăn kém. Đã chốt khi clarify (FR-049): Bác sĩ xem mọi kết quả ghi nhận, Dinh dưỡng viên chỉ xem kết quả ăn uống và lượng nước, cả hai không ghi. Theo constitution VI, cần bổ sung vào Permission Matrix 4.4 (ví dụ dòng mới "Kết quả ghi nhận chăm sóc": BS X, DDV X kèm chú thích giới hạn) trước khi coi spec khớp ma trận (đề xuất Q-33).
8. **Báo vắng muộn hơn CFG-M02-06** của bán trú không có trạng thái có mặt tương ứng ở 3.4; spec giữ trạng thái theo điểm danh và lưu bản ghi báo muộn (FR-014).
9. **BR-M04-11 (tâm trạng)** thuộc 8.10 nhưng được kích hoạt từ kết quả ghi nhận ở 8.6; spec đặt phần phát hiện ở đây, phần gợi ý hoạt động ở feature 014.
10. **Tự đóng công việc Thường quá hạn sau bàn giao** (đã chốt khi clarify, FR-047) chưa có trong 8.7 và BR-M09-08; cần phản ánh vào hai mục này và thêm Q-32 vào mục 24.2.
11. **Các quyết định clarify lượt 2** cần phản ánh vào tài liệu nguồn và thêm vào mục 24.2: Q-33 quyền xem kết quả ghi nhận của Bác sĩ, Dinh dưỡng viên (Permission Matrix 4.4, xem điểm 7); Q-34 ngày hiệu lực kế hoạch sớm nhất là ngày hôm sau ngày duyệt (8.1, BR-M04-19); Q-35 người phụ trách ca chỉ ghi nhận thay công việc Quá hạn, hủy công việc Chưa đến hạn chỉ do Trưởng tầng (BR-M04-12, 8.3); Q-36 lệnh "Ghi nhận phát sinh" (8.3, 8.6); Q-37 mốc xét tự đóng công việc Thường là lúc ca sau xác nhận bàn giao (BR-M09-08); Q-38 hệ thống tự đóng công việc Đến hạn/Quá hạn khi người cao tuổi chuyển trạng thái cuối (8.3, BR-M04-04).
12. **Mục 4.2 chưa có use case** cho cấu hình danh mục loại công việc (FR-038) và xem thời khóa biểu cá nhân (FR-048, 8.2); cần bổ sung UC hoặc ghi rõ hai chức năng này thuộc UC nào.
13. **Các bổ sung của spec không có trong tài liệu nguồn**, cần chủ tài liệu xác nhận: mỗi người cao tuổi tối đa một phiên bản kế hoạch đang soạn/chờ (FR-004); người duyệt không sửa ngày hiệu lực mà phải Trả lại (FR-004); bảng trạng thái yêu cầu xem xét kế hoạch và việc yêu cầu không mở lại khi phiên bản bị Trả lại (FR-011); điều kiện "đang có mặt tại mốc kiểm tra" và tối đa một công việc bổ sung nước mỗi ngày (FR-039); giới hạn thời điểm, vai trò và gợi ý tránh trùng của "Ghi nhận phát sinh" (FR-025); cập nhật bản nháp bàn giao khi người cao tuổi chuyển trạng thái cuối (FR-010); sửa loại công việc chỉ áp cho mục và công việc tạo sau (FR-038).

**(2026-09-27)** Các quyết định Q-30 → Q-38 của spec này đã được phản ánh vào `docs/nghiep-vu.md` (8.1, 8.3, 8.7, 19.3; CFG-M04-12 ở Phụ lục 25) và `docs/phan-tich-yeu-cau.md` (chú thích ²² của 4.4, quyền xem của Bác sĩ, Dinh dưỡng viên), và nằm ở mục 24.2.

**(2026-09-28, checklist cross-feature CHK013, CHK014)** Điểm 2 và 3 đã được phản ánh vào `docs/nghiep-vu.md`: bảng vòng đời phiên bản kế hoạch ở 8.1 (theo FR-003, FR-004) và bảng trạng thái công việc ở 8.3 (theo FR-026). Hai bảng là căn cứ khi khác sơ đồ hoặc câu gốc.

**(2026-09-28, góp ý nghiệp vụ Q-207 → Q-222) Tài liệu nguồn đã thay đổi, spec cần rà lại.** *(2026-09-29: mọi điểm dưới đây đã xử lý, xem nhãn "Đã xử lý" từng điểm.)* Các điểm dưới đây đã có trong `docs/nghiep-vu.md` và `docs/luong-nghiep-vu.md`; spec **chưa** được sửa theo, và cần chạy `/speckit-clarify` hoặc cập nhật FR tương ứng.

1. **[Không cần sửa: FR-041 đã coi đo chỉ số là công việc, chuyển kết quả cho feature 007]** **Nhóm chức năng "Chăm sóc" (Q-207; 1.6, 10.1).** Đo chỉ số theo lịch là công việc chăm sóc, ghi trong checklist (UC-26); quy tắc ngưỡng vẫn thuộc feature 007. Không đổi ranh giới spec; chỉ cần kiểm tra lại cách diễn đạt nếu spec đang coi đo chỉ số là việc riêng.
2. **[Đã xử lý 2026-09-28: FR-049, bảng trạng thái có mặt bán trú]** **Điều dưỡng làm việc của Nhân viên chăm sóc (Q-214; 2.4, 19.3, Phụ lục 27 ²⁷).** Điều dưỡng được điểm danh bán trú đến/về (UC-28) và được phân mọi loại công việc chăm sóc. Rà FR-049 và bảng quyền của spec.

**(2026-10-01) Đánh giá checklist business-rules** – 70 mục còn mở được đối chiếu (CHK002 đã đạt từ 2026-09-26). Phần lớn đã được các lượt clarify và đồng bộ trước đó đáp ứng. Spec được sửa ở một chỗ tự mâu thuẫn: dòng "Ghi nhận không thực hiện" của bảng FR-026 từng ghi bữa "bỏ bữa" kích hoạt quy tắc, trái FR-040; nay "Bỏ bữa" là kết quả của Ghi nhận hoàn thành. Bảy điểm là đề xuất mới chưa có trong tài liệu nguồn, cần ghi vào 8.1 → 8.7 khi rà lại nguồn: (a) phụ lục đổi mức chăm sóc là nguồn thứ ba của yêu cầu xem xét kế hoạch (FR-011; cần thêm vào BR-M04-20, cho khớp spec 004 FR-042); (b) "kiểm tra đầu ca" và "giám sát" là hai loại công việc sinh mỗi ca cho điều dưỡng (FR-029); (c) hai chế độ của CFG-M04-01 và cách tính chuỗi "mỗi N giờ" (FR-017, FR-002); (d) chạy bù của Bộ lập lịch (FR-052); (e) xem checklist khi mất kết nối và xử lý bản ghi cho công việc đã đóng, đã Hủy hoặc đã bị hệ thống đóng vì trạng thái cuối (FR-035); (f) gán lại công việc khi đổi phân công giữa ca, không sinh trùng khi đổi phiên bản kế hoạch, báo Trưởng tầng khi ca không có ai đủ điều kiện (FR-024, FR-006, Edge Cases); (g) nhắc quá hạn chỉ một lần (FR-043).
