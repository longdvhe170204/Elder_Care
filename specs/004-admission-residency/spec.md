# Feature Specification: Tiếp nhận và lưu trú

**Feature Branch**: `004-admission-residency`

**Created**: 2026-09-25

**Status**: Draft

**Input**: User description: "Quản lý luồng tiếp nhận và lưu trú theo docs/nghiep-vu.md Module 02 (mục 6): đăng ký tiếp nhận; danh sách chờ với điểm ưu tiên tự tính và đề xuất người phù hợp khi có giường trống; hợp đồng khóa sau khi có hiệu lực, thay đổi qua yêu cầu thay đổi thành phụ lục có ngày hiệu lực; đặt cọc chỉ ghi nhận trạng thái; tạm vắng với bảng chính sách phí và giữ giường cấu hình được; kết thúc lưu trú và qua đời với danh sách điều kiện bắt buộc."

## Clarifications

### Session 2026-09-25

- Q: Khi kết thúc lưu trú, chi phí kỳ cuối phải được chốt trước khi thực hiện lệnh "Kết thúc lưu trú", hay chính lệnh đó tự chốt? → A: Hai bước — lập hồ sơ kết thúc với ngày dự kiến thì hệ thống dừng sinh chi phí từ ngày đó và tạo nháp kỳ cuối; hành chính kiểm tra, quản lý duyệt và chốt; chỉ sau đó mới thực hiện được lệnh Kết thúc lưu trú; đổi ngày kết thúc thì kỳ cuối được tính lại (đề xuất Q-21).
- Q: Khi hợp đồng đã qua ngày kết thúc nhưng người cao tuổi vẫn ở viện và chưa có quyết định gia hạn hay kết thúc lưu trú, hợp đồng ở trạng thái nào và phí tính theo căn cứ nào? → A: Hợp đồng vẫn Hiệu lực, gắn dấu "quá hạn hợp đồng", phí tính theo điều khoản cũ; hành chính được nhắc hằng ngày, quá CFG-M02-10 (đề xuất, mặc định 7 ngày) thì báo Quản lý viện; dấu gỡ khi có phụ lục gia hạn hoặc kết thúc lưu trú (đề xuất Q-20).
- Q: Nếu ngày bắt đầu hợp đồng khác ngày người cao tuổi thực sự vào ở ("Hoàn tất tiếp nhận"), phí lưu trú bắt đầu tính từ ngày nào? → A: Nội trú tính từ ngày bắt đầu hợp đồng kể cả khi vào muộn; "Hoàn tất tiếp nhận" không được sớm hơn ngày bắt đầu hợp đồng (vào sớm thì phải có phụ lục đổi ngày); bán trú tính theo buổi có lịch từ ngày bắt đầu (đề xuất Q-22).
- Q: Khi hành chính soạn hợp đồng có giá riêng hoặc chính sách phí vắng khác bảng chuẩn của cơ sở, có cần Quản lý viện duyệt trước khi gửi gia đình ký không? → A: Chỉ hợp đồng có điều khoản khác chuẩn (giá khác phiên bản đơn giá hiện hành, dòng ghi đè chính sách vắng hoặc giữ giường) mới cần Quản lý viện duyệt trước khi "Gửi ký"; hợp đồng khớp chuẩn gửi ký trực tiếp (đề xuất Q-23).
- Q: Khi người cao tuổi đang tạm vắng rồi phải nhập viện, số ngày vắng dùng để tra bảng phí được đếm lại từ ngày 1 hay đếm tiếp từ ngày đầu tiên rời viện? → A: Đếm lại từ ngày 1 theo dòng "Bệnh viện" kể từ lúc nhập viện; lượt vắng trước đóng, lượt vắng loại Bệnh viện mở; giường giữ liên tục (đề xuất Q-24).
- Q: Khi viện tạo bảng giá mới, giá trong các hợp đồng đang có hiệu lực có tự động đổi theo không, hay giữ nguyên giá lúc ký cho tới khi có phụ lục? → A: Giữ nguyên — hợp đồng ghi lại đơn giá của mọi khoản phí và dịch vụ trong hợp đồng khi ký; bảng giá mới chỉ áp cho hợp đồng ký sau; tăng giá cho hợp đồng đang hiệu lực phải qua yêu cầu thay đổi thành phụ lục; dịch vụ phát sinh ngoài hợp đồng tính theo bảng giá hiệu lực tại ngày phát sinh (đề xuất Q-25).
- Q: Khi đã qua ngày kết thúc dự kiến mà lệnh "Kết thúc lưu trú" chưa thực hiện được và người cao tuổi vẫn ở viện, hệ thống xử lý chi phí và ngày kết thúc thế nào? → A: Từ ngày liền sau ngày kết thúc dự kiến, hồ sơ kết thúc mang dấu "quá ngày dự kiến"; hệ thống sinh bù và tiếp tục sinh chi phí hằng ngày; điều kiện "chi phí đã chốt" về Chưa đạt; hành chính được nhắc hằng ngày đặt ngày kết thúc mới; có ngày mới thì kỳ cuối tính lại theo FR-066 (đề xuất Q-26).
- Q: Nếu người cao tuổi bị Hủy tiếp nhận sau ngày bắt đầu hợp đồng (đã có vài ngày tính phí "chưa vào ở"), phí của những ngày đó được giữ lại hay hủy bỏ? → A: Giữ phí từ ngày bắt đầu hợp đồng tới hết ngày Hủy tiếp nhận; hợp đồng Chấm dứt, phí dừng sau ngày hủy; miễn giảm chỉ qua khoản điều chỉnh có duyệt (đề xuất Q-27).
- Q: Với hợp đồng nội trú, có bắt buộc người cao tuổi phải có giường được phân bổ (kể cả phân bổ trước cho ngày vào) bắt đầu không muộn hơn ngày bắt đầu hợp đồng không? → A: Có — chặn "Ghi nhận đã ký" nếu chưa có phân bổ giường (đang hiệu lực hoặc tương lai) bắt đầu không muộn hơn ngày bắt đầu hợp đồng; nếu phân bổ đó bị hủy khi hợp đồng đã Hiệu lực thì cảnh báo và nhắc hành chính phân bổ lại (đề xuất Q-28).
- Q: Khi hồ sơ chờ chuyển Từ chối hoặc Hủy chờ (gia đình không còn muốn vào viện), hồ sơ người cao tuổi đang ở trạng thái Đang tiếp nhận và lượt đăng ký được xử lý thế nào? → A: Trong cùng một lần: lượt đăng ký chuyển Đã hủy và người cao tuổi được Hủy tiếp nhận với cùng lý do; hợp đồng Nháp/Chờ ký chuyển Đã hủy, phân bổ tương lai và giữ chỗ giường bị hủy (đề xuất Q-29).

### Cập nhật 2026-09-26 (bổ sung nghiệp vụ vệ sinh vào tài liệu nguồn)

Tài liệu nguồn thêm trạng thái giường Chờ vệ sinh (7.2) và BR-M03-09, sửa BR-M03-06: giường vừa kết thúc một phân bổ (kết thúc lưu trú, qua đời, tạm vắng không giữ giường, giải phóng khi vắng, chuyển giường) chuyển Chờ vệ sinh, chỉ về Trống khi vệ sinh trả giường Hoàn thành; giường giữ tạm cho hồ sơ chờ hết hạn hoặc bị hủy giữ vẫn về thẳng Trống. Spec cập nhật: Phạm vi, User Story 2 kịch bản 10, FR-012, FR-054, FR-070.

## Phạm vi

**Trong phạm vi** (Module 02, mục 6; UC-08 phần điều kiện, UC-09 → UC-18):

1. Đăng ký tiếp nhận và danh sách điều kiện tiếp nhận theo quy trình 6.1 (UC-09; điều kiện của "Hoàn tất tiếp nhận" ở 5.6, UC-08).
2. Danh sách chờ: hồ sơ chờ có vòng đời, điểm ưu tiên tự tính và tính lại mỗi ngày, điều chỉnh điểm thủ công có duyệt, đề xuất người phù hợp khi có giường trống, giữ chỗ tạm (6.2, UC-10, BR-M02-01, 02, 03, 10).
3. Hợp đồng lưu trú: vòng đời Nháp → Chờ ký → Hiệu lực → Kết thúc / Chấm dứt; khóa khi Hiệu lực; không chồng thời gian; nhắc hạn (6.3, UC-11, BR-M02-05, 08, 09, DBR-06).
4. Đặt cọc: chỉ ghi nhận khoản cần đặt cọc và trạng thái đáp ứng (6.5, UC-12).
5. Danh mục dịch vụ và phiên bản đơn giá (6.4, DBR-08).
6. Yêu cầu thay đổi lưu trú và phụ lục hợp đồng có ngày hiệu lực; áp dụng tự động vào ngày hiệu lực (6.6, UC-13, UC-14, BR-M02-04, DBR-07).
7. Tạm vắng và điều trị tại bệnh viện: nội dung lượt vắng, bảng chính sách phí khi vắng cấu hình được và ghi đè theo hợp đồng, giữ giường, quyết định khi vượt ngưỡng giữ giường (6.7, UC-15, UC-16 phần nội dung lượt vắng, BR-M02-06, 07, CFG-M02-05).
8. Kết thúc lưu trú với danh sách điều kiện bắt buộc (6.8, 5.6, UC-17).
9. Ghi nhận qua đời: nội dung ghi nhận và danh sách việc bắt buộc sau qua đời (6.8, UC-18).

**Ngoài phạm vi** (spec này chỉ **cung cấp** dữ liệu/điều kiện hoặc **được kích hoạt** bởi feature sở hữu):

- Quy tắc dùng chung (nhóm dữ liệu, lý do bắt buộc, đính chính, vòng đời yêu cầu phê duyệt, nhật ký, tham số, "hoặc toàn bộ, hoặc không"): feature 000 — spec này kế thừa, không lặp lại.
- Hồ sơ người cao tuổi, đánh giá, tập trạng thái người cao tuổi và điều kiện chặn của từng lệnh chuyển trạng thái (5.5, 5.6): feature 001. Spec này quy định **nội dung** của lệnh Cho tạm vắng, Chuyển viện, Ghi nhận trở về, Kết thúc lưu trú, Ghi nhận qua đời và **cung cấp** các điều kiện hợp đồng, đặt cọc, danh sách kết thúc cho feature 001 kiểm tra.
- Phòng, giường, trạng thái giường, bản ghi phân bổ, giữ chỗ giường, chuyển giường (7.x): feature 003. Spec này kích hoạt giữ chỗ/giải phóng và nhận sự kiện "giường chuyển Trống". Giường vừa kết thúc phân bổ đi qua Chờ vệ sinh và chỉ phát sự kiện "giường chuyển Trống" khi vệ sinh trả giường Hoàn thành (feature 003 FR-011a).
- Người thân, người đại diện, người liên hệ chính, danh sách được phép đón (14.1, 14.3): feature 012.
- Sinh và tính chi phí, chốt kỳ, kỳ cuối (Module 11): feature 010. Spec này cung cấp hệ số phí vắng, đơn giá, nội dung hợp đồng hiệu lực tại một ngày.
- Đồ gửi (Module 12): feature 013; thuốc gia đình gửi (11.4): feature 006; cảnh báo, sự cố (Module 05): feature 007; công việc, kế hoạch chăm sóc, điểm danh bán trú (Module 04): feature 005; suất ăn: feature 011; gửi thông báo: feature 009.
- Thu tiền, hoàn cọc, hạch toán: hệ thống kế toán (1.2, 15.1).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Hành chính đăng ký tiếp nhận và theo dõi danh sách điều kiện tiếp nhận (Priority: P1)

Khi gia đình liên hệ gửi người cao tuổi, nhân viên hành chính lập một lượt đăng ký tiếp nhận gắn với hồ sơ người cao tuổi ở trạng thái Đang tiếp nhận (feature 001). Lượt đăng ký ghi ngày đăng ký, người liên hệ, nhu cầu, loại lưu trú mong muốn, mức chăm sóc dự kiến, ngày mong muốn vào ở. Hệ thống hiển thị tiến trình theo các bước của 6.1 và một danh sách điều kiện tiếp nhận (đánh giá đầu vào, hợp đồng Hiệu lực, đặt cọc đạt, giường với nội trú) với trạng thái từng điều kiện, để hành chính biết còn thiếu gì trước khi "Hoàn tất tiếp nhận". Nếu chưa có giường phù hợp, hành chính chuyển lượt đăng ký vào danh sách chờ.

**Why this priority**: Là điểm vào của toàn bộ hành trình lưu trú (BF-01 bước 1, 3, 5); không có nó thì không có hợp đồng, danh sách chờ hay tiếp nhận.

**Independent Test**: Tạo 3 lượt đăng ký: một người đủ mọi điều kiện, một người thiếu đặt cọc, một người nội trú chưa có giường; kiểm tra danh sách điều kiện của từng người và kết quả khi thực hiện "Hoàn tất tiếp nhận".

**Acceptance Scenarios**:

1. **Given** hồ sơ người cao tuổi A ở trạng thái Đang tiếp nhận, **When** hành chính lập lượt đăng ký tiếp nhận cho A với ngày đăng ký 01/10, loại lưu trú mong muốn "Nội trú dài hạn", mức chăm sóc dự kiến "Chăm sóc thường xuyên", ngày mong muốn vào 15/10, **Then** lượt đăng ký được lưu ở trạng thái Đang xử lý, gắn với A, và danh sách điều kiện tiếp nhận hiển thị bốn điều kiện đều "Chưa đạt".
2. **Given** A đã có một lượt đăng ký Đang xử lý, **When** hành chính lập thêm lượt đăng ký thứ hai cho A, **Then** hệ thống chặn (mỗi hồ sơ chỉ có một lượt đăng ký đang mở).
3. **Given** A có đánh giá đầu vào Đã xác nhận, hợp đồng Hiệu lực, đặt cọc Đã đáp ứng, và phân bổ giường (feature 003), **When** hành chính xem danh sách điều kiện, **Then** cả bốn điều kiện hiển thị "Đạt" kèm căn cứ (lần đánh giá, số hợp đồng, thời điểm xác nhận cọc, giường).
4. **Given** A thiếu đặt cọc, **When** hành chính thực hiện "Hoàn tất tiếp nhận" (feature 001), **Then** hệ thống từ chối và liệt kê "đặt cọc chưa đáp ứng" cùng mọi điều kiện chưa đạt khác trong một lần.
5. **Given** người cao tuổi B có loại lưu trú "Bán trú", **When** xem danh sách điều kiện, **Then** điều kiện "giường" không áp dụng (3.3).
6. **Given** A không có giường phù hợp, **When** hành chính chọn "Đưa vào danh sách chờ", **Then** hệ thống tạo hồ sơ chờ từ lượt đăng ký (ngày đăng ký, nhu cầu, loại lưu trú, mức chăm sóc lấy từ lượt đăng ký) (User Story 2).
7. **Given** A Hoàn tất tiếp nhận thành công, **When** kiểm tra lượt đăng ký, **Then** lượt đăng ký chuyển Đã tiếp nhận; **Given** A bị Hủy tiếp nhận (feature 001), **Then** lượt đăng ký chuyển Đã hủy và hồ sơ chờ liên quan chuyển Hủy chờ.
8. **Given** trưởng tầng, bác sĩ hoặc điều dưỡng, **When** cố lập lượt đăng ký tiếp nhận, **Then** hệ thống từ chối (Permission Matrix dòng "Tiếp nhận, hợp đồng, đặt cọc": HC T, QL X).

---

### User Story 2 - Danh sách chờ với điểm ưu tiên tự tính và đề xuất người phù hợp khi có giường trống (Priority: P1)

Người chưa thể tiếp nhận được đưa vào danh sách chờ. Hệ thống tự tính điểm ưu tiên: điểm theo mức độ cần chăm sóc + điểm thời gian chờ + điểm tình huống đặc biệt, theo bảng CFG-M02-08, và tính lại mỗi ngày. Hành chính có thể cộng/trừ điểm thủ công kèm lý do, chỉ có hiệu lực khi Quản lý viện duyệt. Khi một giường chuyển Trống, hệ thống lọc hồ sơ chờ phù hợp và đề xuất một số người đầu cho hành chính; khi hành chính liên hệ gia đình, giường được giữ chỗ tạm trong thời hạn cấu hình.

**Why this priority**: Là cơ chế bảo đảm công bằng giữa các gia đình (BR-M02-10) và lấp giường trống nhanh; được nêu trực tiếp trong mô tả feature.

**Independent Test**: Tạo 6 hồ sơ chờ với mức chăm sóc, ngày đăng ký, tình huống đặc biệt, giới tính khác nhau; dùng đồng hồ giả lập cho thời gian chờ trôi qua; giải phóng một giường và kiểm tra danh sách đề xuất, thứ tự, giữ chỗ, hết hạn giữ chỗ.

**Acceptance Scenarios**:

1. **Given** CFG-M02-08 cho "Chăm sóc đặc biệt" 5 điểm, "sống một mình" 2 điểm, mỗi 7 ngày chờ +1 điểm, **When** hồ sơ chờ của C (Chăm sóc đặc biệt, sống một mình) đã chờ 15 ngày, **Then** điểm ưu tiên của C là 5 + 2 + 2 = 9 và hệ thống hiển thị chi tiết từng thành phần điểm.
2. **Given** hồ sơ chờ của C có điểm 9 vào ngày D, **When** Bộ lập lịch tính lại vào ngày D + 7, **Then** điểm của C là 10 (BR-M02-10); **When** Quản lý viện đổi bảng CFG-M02-08, **Then** điểm của mọi hồ sơ Đang chờ được tính lại theo bảng mới ngay lần tính kế tiếp.
3. **Given** hành chính đề nghị cộng 3 điểm cho C kèm lý do "vừa ra viện, không người chăm", **When** chưa được duyệt, **Then** điểm của C chưa đổi và đề nghị ở Chờ duyệt; **When** Quản lý viện duyệt, **Then** điểm của C tăng 3, lịch sử điều chỉnh lưu người đề nghị, người duyệt, lý do, thời điểm; **When** thiếu lý do, **Then** hệ thống từ chối lập đề nghị.
4. **Given** giường P201-G1 (phòng nữ, cho phép "Chăm sóc thường xuyên" và "Chăm sóc đặc biệt", phục vụ nội trú dài hạn) vừa chuyển Trống, và danh sách chờ có: C (nữ, đặc biệt, 9 điểm), E (nữ, thường xuyên, 9 điểm, đăng ký sớm hơn C), F (nam, đặc biệt, 12 điểm), H (nữ, cơ bản, 11 điểm), K (nữ, thường xuyên, 6 điểm), **When** hệ thống chạy BR-M02-01 với CFG-M02-01 = 3, **Then** hệ thống đề xuất cho hành chính theo thứ tự E, C, K (F bị loại vì giới tính, H bị loại vì mức chăm sóc; E đứng trước C vì hòa điểm và đăng ký sớm hơn).
5. **Given** hành chính chọn E trong đề xuất và ghi nhận "Đã liên hệ" với G1, **When** lưu, **Then** hồ sơ chờ của E chuyển Đã liên hệ và G1 chuyển Đang giữ chỗ (hồ sơ chờ) với hạn = thời điểm liên hệ + CFG-M02-02 (mặc định \[48 giờ\]) (BR-M02-02, feature 003).
6. **Given** hồ sơ E Đã liên hệ, **When** quá hạn giữ chỗ mà chưa có phản hồi, **Then** G1 về Trống, hồ sơ E về Đang chờ (giữ nguyên ngày đăng ký và điểm), và hệ thống đề xuất người tiếp theo cho G1, không đề xuất lại E cho chính G1.
7. **Given** hồ sơ E Đã liên hệ, **When** gia đình đồng ý và hành chính phân bổ G1 cho E (feature 003), **Then** hồ sơ E chuyển Đã tiếp nhận, giữ chỗ kết thúc và lượt đăng ký của E tiếp tục tới bước hợp đồng.
8. **Given** hồ sơ chờ của K không có cập nhật nào trong CFG-M02-03 (mặc định \[30 ngày\]), **When** Bộ lập lịch kiểm tra, **Then** hành chính được nhắc xác nhận gia đình còn nhu cầu (BR-M02-03); **When** hành chính ghi nhận "Còn nhu cầu", **Then** mốc 30 ngày tính lại từ lúc ghi nhận.
9. **Given** trưởng tầng, bác sĩ hoặc người thân, **When** cố xem hoặc thao tác danh sách chờ, **Then** hệ thống từ chối (Permission Matrix dòng "Danh sách chờ, điểm ưu tiên": QL D, HC T).
10. **Given** người cao tuổi M kết thúc lưu trú ở P201-G2 và danh sách chờ có hồ sơ phù hợp G2, **When** lệnh Kết thúc lưu trú hoàn tất, **Then** G2 chuyển Chờ vệ sinh và hệ thống chưa đề xuất hồ sơ chờ nào cho G2; **When** công việc vệ sinh trả giường của G2 Hoàn thành và G2 chuyển Trống, **Then** hệ thống chạy BR-M02-01 cho G2 (BR-M03-06, BR-M03-09).

---

### User Story 3 - Hợp đồng lưu trú bị khóa khi có hiệu lực, không chồng thời gian (Priority: P1)

Hành chính lập hợp đồng lưu trú ở trạng thái Nháp với loại lưu trú, thời hạn, mức chăm sóc, dịch vụ, phòng/giường (nếu có), người đại diện, chính sách chi phí, điều kiện đặt cọc, chính sách giữ giường và chính sách khi tạm vắng (có thể ghi đè bảng của cơ sở), điều kiện chấm dứt. Hợp đồng chỉ sửa được khi Nháp; gửi ký thì khóa nội dung; khi ghi nhận đã ký, hợp đồng Hiệu lực và không còn thao tác sửa. Nội dung hợp đồng tại một ngày bất kỳ = hợp đồng gốc + các phụ lục đã áp dụng có ngày hiệu lực không muộn hơn ngày đó.

**Why this priority**: Hợp đồng Hiệu lực là điều kiện tiếp nhận (5.6) và là căn cứ của mọi chi phí (1.3); nếu sửa được hợp đồng hiệu lực thì mất căn cứ đối chiếu với gia đình.

**Independent Test**: Lập hợp đồng Nháp, sửa, gửi ký, thử sửa, ghi nhận đã ký, thử sửa lại; tạo hợp đồng thứ hai chồng thời gian; áp dụng hai phụ lục và hỏi nội dung hợp đồng tại ba ngày khác nhau.

**Acceptance Scenarios**:

1. **Given** người cao tuổi A Đang tiếp nhận có người đại diện (feature 012), **When** hành chính lập hợp đồng "Nội trú dài hạn" từ 15/10/2026 đến 14/10/2027, mức "Chăm sóc thường xuyên", gói dịch vụ, giá tháng theo phiên bản đơn giá hiện hành, khoản đặt cọc, chính sách phí vắng theo bảng cơ sở, **Then** hợp đồng ở trạng thái Nháp với số hợp đồng duy nhất.
2. **Given** hợp đồng Nháp, **When** hành chính sửa giá tháng, **Then** được lưu và ghi nhật ký; **When** hành chính "Gửi ký", **Then** hợp đồng chuyển Chờ ký và mọi thao tác sửa nội dung bị chặn; **When** cần sửa, **Then** hành chính "Trả về nháp" kèm lý do.
3. **Given** hợp đồng Chờ ký, **When** hành chính "Ghi nhận đã ký" kèm ngày ký, người đại diện đã ký và bản scan hợp đồng đã ký, **Then** hợp đồng chuyển Hiệu lực; **When** thiếu người đại diện ký hoặc bản scan, **Then** hệ thống từ chối; **When** hợp đồng nội trú bắt đầu 15/10 mà A chỉ có phân bổ giường tương lai từ 17/10 (hoặc chưa có phân bổ), **Then** hệ thống từ chối "chưa có giường từ ngày bắt đầu hợp đồng" (FR-021).
4. **Given** hợp đồng Hiệu lực, **When** bất kỳ ai (kể cả Quản lý viện) cố sửa bất kỳ nội dung nào, **Then** hệ thống không có thao tác đó và hướng tới lập yêu cầu thay đổi lưu trú (BR-M02-09).
5. **Given** A đã có hợp đồng Hiệu lực từ 15/10/2026 đến 14/10/2027, **When** hành chính "Ghi nhận đã ký" cho một hợp đồng khác của A có thời gian từ 01/06/2027, **Then** hệ thống chặn vì chồng thời gian (BR-M02-08, DBR-06).
6. **Given** hợp đồng gốc giá tháng 12.000.000 đồng; phụ lục PL1 đổi mức chăm sóc và giá tháng thành 15.000.000 đồng, hiệu lực 01/12, đã áp dụng; phụ lục PL2 thêm dịch vụ phục hồi, hiệu lực 01/01, đã duyệt nhưng chưa tới ngày, **When** hỏi nội dung hợp đồng tại 30/11, 01/12 và 01/01, **Then** lần lượt trả giá 12.000.000 đồng; giá 15.000.000 đồng và mức mới; và (sau khi PL2 được áp dụng vào 01/01) giá 15.000.000 đồng kèm dịch vụ phục hồi (BR-M02-09).
7. **Given** hợp đồng dài hạn kết thúc 14/10/2027 và CFG-M02-04 = \[30 ngày\], **When** Bộ lập lịch chạy ngày 14/09/2027, **Then** hành chính được nhắc hợp đồng sắp hết hạn (BR-M02-05).
8. **Given** hợp đồng ngắn ngày của B kết thúc 20/10 và tới ngày 20/10 chưa có yêu cầu gia hạn hay kết thúc lưu trú, **When** Bộ lập lịch chạy, **Then** hành chính được nhắc tạo "Kết thúc lưu trú" hoặc yêu cầu thay đổi lưu trú; hợp đồng không tự gia hạn (BR-M02-05); **When** tới ngày 21/10 vẫn chưa có quyết định, **Then** hợp đồng vẫn Hiệu lực với ngày kết thúc 20/10, mang dấu "quá hạn hợp đồng", phí ngày 21/10 tính theo điều khoản cũ, hành chính được nhắc mỗi ngày; **When** quá hạn vượt CFG-M02-10 (mặc định \[7 ngày\]), **Then** Quản lý viện được báo; **When** phụ lục gia hạn được áp dụng, **Then** dấu được gỡ (FR-029).
9. **Given** hợp đồng Nháp của A có giá tháng 10.000.000 đồng trong khi phiên bản đơn giá hiện hành là 12.000.000 đồng, **When** hành chính "Gửi ký", **Then** hệ thống chặn và yêu cầu gửi Quản lý viện duyệt điều khoản khác chuẩn; **When** Quản lý viện duyệt, **Then** hành chính gửi ký được; **When** hành chính trả hợp đồng về sửa giá thành 11.000.000 đồng sau khi đã duyệt, **Then** phải duyệt lại; **Given** hợp đồng khớp đơn giá và bảng chính sách chuẩn, **When** gửi ký, **Then** không cần duyệt (FR-025a).
10. **Given** người đại diện của A đăng nhập cổng người thân, **When** xem hợp đồng, **Then** thấy hợp đồng gốc, các phụ lục và trạng thái đặt cọc, không có thao tác sửa (Permission Matrix: NT X).

---

### User Story 4 - Hành chính ghi nhận trạng thái đặt cọc, không thu tiền (Priority: P1)

Hệ thống không thu tiền. Hợp đồng xác định khoản cần đặt cọc (có thể là "không yêu cầu"). Khi kế toán hoặc thủ quỹ đã nhận tiền, hành chính ghi nhận đặt cọc "Đã đáp ứng" kèm thời điểm, người xác nhận, nguồn xác nhận. Trạng thái này là một điều kiện của "Hoàn tất tiếp nhận".

**Why this priority**: Là điều kiện tiếp nhận ở 5.6; ranh giới "không thu tiền" (1.2, 6.5) cần được thể hiện rõ để tránh phát sinh nghiệp vụ kế toán.

**Independent Test**: Với một hợp đồng có yêu cầu cọc và một hợp đồng không yêu cầu cọc, kiểm tra điều kiện đặt cọc trước và sau khi xác nhận; đính chính một lần xác nhận sai.

**Acceptance Scenarios**:

1. **Given** hợp đồng Hiệu lực của A có khoản cần đặt cọc 24.000.000 đồng, **When** chưa có xác nhận, **Then** đặt cọc ở trạng thái "Chưa đáp ứng" và điều kiện "đặt cọc đạt" của A là "Chưa đạt".
2. **Given** kế toán báo đã nhận cọc, **When** hành chính "Xác nhận đặt cọc" kèm thời điểm xác nhận, nguồn xác nhận "Phiếu thu số PT-1023" và bằng chứng (tùy chọn), **Then** đặt cọc chuyển "Đã đáp ứng", ghi người xác nhận; hệ thống không ghi nhận số tiền thực nhận, phương thức thanh toán hay tài khoản.
3. **Given** hợp đồng của B ghi "không yêu cầu đặt cọc", **When** hợp đồng Hiệu lực, **Then** điều kiện "đặt cọc đạt" của B là "Đạt (không yêu cầu)".
4. **Given** đặt cọc của A đã xác nhận nhầm, **When** hành chính đính chính kèm lý do (feature 000), **Then** bản xác nhận gốc giữ nguyên, trạng thái hiện hành về "Chưa đáp ứng"; nếu A đã Đang lưu trú, trạng thái lưu trú không bị ảnh hưởng và Quản lý viện được báo.
5. **Given** điều dưỡng hoặc trưởng tầng, **When** cố xác nhận đặt cọc, **Then** hệ thống từ chối.

---

### User Story 5 - Thay đổi lưu trú qua yêu cầu có duyệt, trở thành phụ lục có ngày hiệu lực (Priority: P2)

Khi cần đổi loại lưu trú, mức chăm sóc, dịch vụ, thời gian (gia hạn, rút ngắn) hoặc phòng/giường của người đang có hợp đồng Hiệu lực, người có quyền lập yêu cầu thay đổi lưu trú ghi giá trị trước, giá trị sau, ngày hiệu lực mong muốn, lý do. Quản lý viện duyệt. Khi được duyệt, yêu cầu tạo phụ lục gắn với hợp đồng gốc; vào ngày hiệu lực, hệ thống tự áp dụng: đổi mức chăm sóc, đổi đơn giá, chuyển giường. Chi phí trước ngày hiệu lực tính theo giá trị cũ.

**Why this priority**: Là con đường duy nhất để thay đổi hợp đồng đã khóa; cần hợp đồng (User Story 3) có trước.

**Independent Test**: Lập ba yêu cầu (đổi mức chăm sóc do đánh giá lại, gia hạn hợp đồng ngắn ngày, đổi giường sang phòng giá khác) với ngày hiệu lực tương lai; duyệt, dùng đồng hồ giả lập tới ngày hiệu lực, kiểm tra phụ lục, mức chăm sóc, giường, đơn giá và chi phí trước/sau ngày hiệu lực.

**Acceptance Scenarios**:

1. **Given** bác sĩ chấp nhận đánh giá lại của A với mức "Chăm sóc đặc biệt" khác mức hiện hành (feature 001), **When** hệ thống tạo yêu cầu thay đổi lưu trú loại "Đổi mức chăm sóc" ở Chờ duyệt (BR-M01-03), **Then** yêu cầu ghi giá trị trước/sau, người yêu cầu là bác sĩ, và hành chính bổ sung được giá tháng mới và ngày hiệu lực mong muốn trước khi Quản lý viện duyệt.
2. **Given** yêu cầu đổi mức chăm sóc của A hiệu lực 01/12 đã được Quản lý viện duyệt ngày 20/11, **When** duyệt, **Then** yêu cầu chuyển Đã duyệt (chờ hiệu lực) và phụ lục PL1 được tạo ở trạng thái Chờ hiệu lực, gắn hợp đồng gốc và yêu cầu (DBR-07); **When** tới 01/12, **Then** Bộ lập lịch áp dụng: mức chăm sóc hiện hành đổi (feature 001), giá mới áp dụng từ 01/12, feature 005 được kích hoạt xem xét kế hoạch chăm sóc; PL1 chuyển Đã áp dụng; chi phí ngày 30/11 vẫn theo giá cũ (BR-M02-04).
3. **Given** yêu cầu đổi giường của A sang P305-G2 hiệu lực 05/12, **When** Quản lý viện duyệt, **Then** hệ thống kiểm tra điều kiện phân bổ cho G2 (feature 003) và tạo phân bổ tương lai từ 05/12; **When** tới 05/12 mà G2 thuộc phòng vừa cách ly, **Then** yêu cầu chuyển "Áp dụng không thành" (feature 000), phụ lục chuyển Không áp dụng, A giữ giường cũ, người duyệt và người yêu cầu được báo.
4. **Given** hợp đồng ngắn ngày của B kết thúc 20/10, **When** hành chính lập yêu cầu "Gia hạn thời gian" đến 20/11 và Quản lý viện duyệt với ngày hiệu lực 21/10, **Then** phụ lục đổi ngày kết thúc hợp đồng thành 20/11; ràng buộc không chồng thời gian (BR-M02-08) được kiểm tra với ngày kết thúc mới.
5. **Given** người đại diện của A, **When** gửi yêu cầu thêm dịch vụ phục hồi qua cổng người thân, **Then** yêu cầu được tạo ở Nháp, hành chính được báo để hoàn thiện và gửi duyệt; người đại diện không trực tiếp thay đổi hợp đồng (Permission Matrix: NT T²); người thân không phải người đại diện không có thao tác này.
6. **Given** yêu cầu đổi giường sang phòng cùng đơn giá và yêu cầu đổi loại lưu trú, **When** mỗi yêu cầu được gửi, **Then** cả hai đều phải được Quản lý viện duyệt trước khi áp dụng (FR-043).
7. **Given** A đã có một yêu cầu "Đổi mức chăm sóc" Đã duyệt (chờ hiệu lực), **When** Quản lý viện duyệt một yêu cầu "Đổi mức chăm sóc" khác của A, **Then** hệ thống chặn và yêu cầu hủy yêu cầu trước (FR-046).
8. **Given** hành chính lập yêu cầu với ngày hiệu lực mong muốn sớm hơn ngày bắt đầu hợp đồng, **When** gửi duyệt, **Then** hệ thống chặn (DBR-07).

---

### User Story 6 - Tạm vắng áp bảng chính sách phí và giữ giường cấu hình được (Priority: P2)

Khi người cao tuổi rời viện (về nhà, đi chơi với gia đình, đi khám trong ngày, lý do khác) hoặc chuyển viện, hệ thống ghi lượt vắng: thời gian rời, thời gian dự kiến trở lại, lý do, người đón, người bàn giao, tình trạng giường, chính sách tính phí áp dụng. Mỗi ngày vắng, hệ thống tra bảng chính sách phí khi vắng (cấu hình của cơ sở, có thể ghi đè theo hợp đồng) theo loại lưu trú, loại vắng và số ngày đã vắng để xác định hệ số phí và việc giữ giường. Tạm vắng không mặc định là miễn phí. Khi vắng vượt ngưỡng giữ giường, hệ thống tạo yêu cầu để Quản lý viện chọn giữ tiếp (có phí) hoặc giải phóng giường.

**Why this priority**: Là nghiệp vụ đã xác thực từ khảo sát (21: "Tạm vắng có chính sách tính phí khác nhau") và ảnh hưởng trực tiếp tới chi phí và giường trống; cần hợp đồng và giường có trước.

**Independent Test**: Với bảng chính sách mặc định (6.7), cho một người nội trú dài hạn về nhà 5 ngày, một người nằm viện 35 ngày, một người ngắn ngày có hợp đồng ghi đè; kiểm tra hệ số phí từng ngày, trạng thái giường, yêu cầu giữ/giải phóng ở ngày 31.

**Acceptance Scenarios**:

1. **Given** A (Nội trú dài hạn) Đang lưu trú ở G1, **When** hành chính "Cho tạm vắng" loại "Về nhà" với người đón thuộc danh sách được phép đón (feature 012), người bàn giao, thời điểm rời 10:00 ngày 01/11, dự kiến trở lại 18:00 ngày 05/11, **Then** lượt vắng được tạo với các thông tin trên, chính sách áp dụng "Nội trú dài hạn – Về nhà, đi chơi – 100% – Giữ giường", G1 chuyển Đang giữ chỗ (người vắng) (feature 003), A chuyển Tạm vắng (feature 001).
2. **Given** A vắng từ 01/11 đến 05/11, **When** Bộ lập lịch xử lý mỗi ngày vắng, **Then** mỗi ngày có hệ số phí 100% và giữ giường, được cung cấp cho feature 010 để sinh chi phí nháp tham chiếu lượt vắng (BR-M02-06, DBR-15).
3. **Given** A cần ở nhà thêm, **When** hành chính "Gia hạn dự kiến trở lại" tới 18:00 ngày 07/11 kèm lý do, **Then** thời điểm dự kiến mới được lưu cùng lịch sử, mốc cảnh báo quá hạn (BR-M01-01, feature 001) tính lại theo thời điểm mới.
4. **Given** C (Nội trú dài hạn) chuyển viện ngày 01/11 (feature 001), **When** Bộ lập lịch xử lý ngày vắng thứ 1–7, 8–30 và 31, **Then** hệ số lần lượt 100%, 70%, và từ ngày 31 hệ thống tạo yêu cầu "Quyết định giữ giường khi vắng" cho Quản lý viện (BR-M02-07) vì dòng chính sách là "Quản lý quyết định".
5. **Given** yêu cầu quyết định giữ giường của C, **When** Quản lý viện chọn "Giữ tiếp" với hệ số 50% và hạn xem xét lại, **Then** giường tiếp tục Đang giữ chỗ, các ngày từ 31 dùng hệ số 50%; **When** Quản lý viện chọn "Giải phóng", **Then** feature 003 đóng phân bổ và giường chuyển Trống (kích hoạt BR-M02-01), các ngày từ ngày giải phóng dùng hệ số Quản lý viện ghi trong quyết định.
6. **Given** yêu cầu quyết định giữ giường của C chưa được duyệt, **When** tới các ngày vắng từ 31, **Then** giường vẫn giữ, chi phí nháp dùng hệ số của dòng trước đó (70%) và đánh dấu "chờ quyết định"; khi có quyết định, chi phí nháp của các ngày này được tính lại (BR-M11-05, feature 010).
7. **Given** hợp đồng ngắn ngày của D ghi đè chính sách "Mọi loại vắng: 50%, không giữ giường", **When** D được cho tạm vắng, **Then** lượt vắng áp dụng chính sách của hợp đồng thay cho bảng cơ sở, hệ số 50%, feature 003 đóng phân bổ và giường chuyển Trống tại thời điểm rời viện.
8. **Given** A được "Ghi nhận trở về" lúc 16:00 ngày 05/11, **When** lưu, **Then** lượt vắng ghi thời điểm về, G1 chuyển lại Đang sử dụng, và ngày 05/11 vẫn tính là ngày vắng (FR-058).
9. **Given** người bán trú E báo vắng trước CFG-M02-06 (mặc định \[24 giờ\]) cho buổi 06/10, **When** hành chính ghi nhận báo vắng, **Then** trạng thái có mặt ngày 06/10 là "Vắng có báo" và hệ số 0%; **Given** E không báo và quá giờ đến dự kiến CFG-M02-07 (mặc định \[2 giờ\]) chưa điểm danh, **Then** trạng thái "Vắng không báo" (feature 005) và hệ số 50% theo bảng (3.4, 6.7).
10. **Given** Quản lý viện sửa bảng chính sách phí khi vắng CFG-M02-05, **When** lưu, **Then** các ngày vắng từ thời điểm lưu dùng bảng mới; các ngày đã tính trước đó không thay đổi (feature 000).

---

### User Story 7 - Kết thúc lưu trú chỉ hoàn tất khi đủ danh sách điều kiện bắt buộc (Priority: P2)

Khi người cao tuổi hết hợp đồng, xuất viện, chuyển cơ sở hoặc chấm dứt hợp đồng, hành chính lập hồ sơ kết thúc lưu trú với trường hợp, ngày kết thúc, lý do. Hệ thống hiển thị danh sách điều kiện bắt buộc: chốt chi phí, hoàn trả đồ gửi, hoàn trả thuốc gia đình gửi, không còn cảnh báo/sự cố mở, bàn giao người cao tuổi; hệ thống tự kiểm tra từng điều kiện. Chỉ khi mọi điều kiện đạt (hoặc có ngoại lệ Quản lý viện duyệt) thì lệnh "Kết thúc lưu trú" mới được thực hiện; khi đó hợp đồng được xử lý, giường giải phóng, lịch tương lai hủy.

**Why this priority**: Là điểm kết thúc của hành trình lưu trú (BF-01 bước 9); thiếu kiểm tra sẽ để sót đồ gửi, thuốc, chi phí chưa chốt.

**Independent Test**: Lập hồ sơ kết thúc cho một người còn 1 đồ gửi, 1 thuốc gửi, 1 cảnh báo mở; kiểm tra lệnh bị chặn với đủ lý do; lần lượt xử lý từng điều kiện; thử một ngoại lệ có duyệt.

**Acceptance Scenarios**:

1. **Given** A Đang lưu trú, **When** hành chính lập hồ sơ kết thúc lưu trú trường hợp "Xuất viện", ngày kết thúc 30/11, lý do, **Then** hệ thống tạo danh sách điều kiện kết thúc với trạng thái từng điều kiện và căn cứ (ví dụ "còn 1 đồ gửi: Điện thoại").
2. **Given** A còn 1 đồ gửi đang giữ và 1 cảnh báo mở, **When** hành chính thực hiện "Kết thúc lưu trú", **Then** hệ thống từ chối và liệt kê cả hai điều kiện chưa đạt trong một lần (5.6).
3. **Given** cảnh báo mở của A là cảnh báo theo dõi không thể đóng trước ngày đi, **When** hành chính lập yêu cầu ngoại lệ cho điều kiện "không còn cảnh báo/sự cố mở" kèm lý do và Quản lý viện duyệt, **Then** điều kiện đó hiển thị "Đạt (ngoại lệ)" kèm người duyệt; các điều kiện khác vẫn phải đạt.
4. **Given** mọi điều kiện đã đạt, **When** hành chính thực hiện "Kết thúc lưu trú" với thời điểm hiệu lực, **Then** trong cùng một lần: A chuyển Kết thúc lưu trú (feature 001), hợp đồng chuyển Kết thúc (nếu ngày kết thúc lưu trú không sớm hơn ngày kết thúc hợp đồng) hoặc Chấm dứt (nếu sớm hơn), phân bổ giường đóng và giường chuyển Trống (feature 003), mọi lịch tương lai bị hủy (sinh chi phí tự động đã dừng từ khi lập hồ sơ kết thúc, FR-066), tài khoản người thân khóa sau CFG-M01-04 (feature 012).
5. **Given** hồ sơ kết thúc lưu trú của A với ngày kết thúc dự kiến 30/11 vừa được lập, **When** hệ thống xử lý, **Then** chi phí tự động của A không sinh cho các ngày từ 01/12 và bảng kỳ cuối nháp tính đến 30/11 được tạo; điều kiện "chi phí đã chốt" là Chưa đạt cho tới khi Quản lý viện chốt kỳ cuối; **When** hành chính dời ngày kết thúc dự kiến sang 05/12 sau khi kỳ cuối đã chốt, **Then** điều kiện trở về Chưa đạt, sinh chi phí tiếp tục tới 05/12, và chênh lệch được xử lý bằng khoản điều chỉnh (FR-066).
6. **Given** ngày kết thúc dự kiến của A là 30/11 và tới hết 30/11 vẫn còn đồ gửi chưa trả nên lệnh chưa thực hiện được, **When** Bộ lập lịch chạy ngày 01/12, **Then** hồ sơ kết thúc mang dấu "quá ngày dự kiến", chi phí ngày 01/12 được sinh, điều kiện "chi phí đã chốt" về Chưa đạt, hành chính được nhắc đặt ngày mới; **When** hành chính đặt ngày kết thúc mới 03/12, **Then** sinh chi phí dừng sau 03/12 và kỳ cuối được tính lại tới 03/12 (FR-066a).
7. **Given** điều kiện "bàn giao người cao tuổi", **When** hành chính ghi nhận người nhận (người thân hoặc cơ sở tiếp nhận), thời điểm, nhân viên bàn giao, **Then** điều kiện đạt.
8. **Given** người đại diện của A, **When** xem cổng người thân, **Then** thấy tiến trình kết thúc lưu trú và các việc còn lại liên quan gia đình (nhận đồ gửi, thuốc gửi), không có thao tác thay đổi.

---

### User Story 8 - Ghi nhận qua đời và hoàn thành danh sách việc bắt buộc sau qua đời (Priority: P2)

Khi người cao tuổi qua đời, người có thẩm quyền (feature 001 FR-047b) ghi nhận thời điểm, địa điểm, người phát hiện, người xác nhận, thông tin nguyên nhân nếu đã xác định. Lệnh không bị chặn bởi đồ gửi, chi phí hay sự cố. Hệ thống ngay lập tức: chấm dứt hợp đồng, giải phóng giường, hủy lịch tương lai, dừng sinh chi phí, thông báo người liên hệ chính; đồng thời mở một danh sách việc bắt buộc sau qua đời (xử lý đồ gửi, hoàn trả thuốc gửi, chốt chi phí, xử lý cảnh báo/sự cố mở) và nhắc hành chính tới khi hoàn thành để đóng hồ sơ lưu trú.

**Why this priority**: Là sự kiện nhạy cảm, cần thông tin chính xác và không được để gia đình nhận các thông báo, chi phí sai sau thời điểm mất.

**Independent Test**: Ghi nhận qua đời cho một người còn đồ gửi, thuốc gửi, chi phí chưa chốt; kiểm tra các tác động tức thời, danh sách việc, nhắc việc và việc đóng hồ sơ.

**Acceptance Scenarios**:

1. **Given** A Đang lưu trú, **When** bác sĩ "Ghi nhận qua đời" với thời điểm 03:20 ngày 12/11, địa điểm "Phòng P201", người phát hiện (nhân viên chăm sóc trực), người xác nhận (bác sĩ), nguyên nhân "chưa xác định", **Then** A chuyển Qua đời (feature 001); hợp đồng chuyển Chấm dứt với lý do "Qua đời"; giường giải phóng (feature 003); lịch công việc, liều thuốc, suất ăn sau 03:20 bị hủy; người liên hệ chính được thông báo (feature 009); sinh chi phí tự động dừng từ thời điểm qua đời.
2. **Given** A còn đồ gửi, thuốc gửi và chi phí kỳ chưa chốt, **When** lệnh được thực hiện, **Then** lệnh không bị chặn và danh sách việc sau qua đời được tạo với ba mục "Chưa hoàn thành" kèm căn cứ.
3. **Given** danh sách việc sau qua đời còn mục chưa hoàn thành, **When** mỗi chu kỳ CFG-M02-09 (đề xuất, mặc định \[1 ngày\]) trôi qua, **Then** hành chính được nhắc; **When** mọi mục hoàn thành, **Then** hồ sơ lưu trú của A chuyển "Đã đóng" và việc nhắc dừng.
4. **Given** nguyên nhân tử vong được xác định sau đó, **When** bác sĩ bổ sung, **Then** thông tin được ghi dưới dạng đính chính bản ghi qua đời (feature 000), bản gốc giữ nguyên.
5. **Given** A đang Điều trị tại bệnh viện và mất tại bệnh viện, **When** hành chính ghi nhận qua đời kèm bản scan giấy báo tử (feature 001 FR-047b), **Then** địa điểm ghi tên bệnh viện, lượt vắng đang mở được đóng tại thời điểm qua đời.
6. **Given** điều dưỡng, **When** cố ghi nhận qua đời, **Then** hệ thống từ chối (Permission Matrix dòng "Kết thúc lưu trú, qua đời": BS T, HC T).

---

### User Story 9 - Quản lý viện quản lý danh mục dịch vụ và phiên bản đơn giá (Priority: P3)

Dịch vụ (chăm sóc, phục hồi, hoạt động, đưa đi khám, tiêm, dịch vụ khác) có tên, đơn vị tính, trạng thái. Đơn giá theo phiên bản có ngày hiệu lực; đổi giá thì tạo phiên bản mới, không sửa phiên bản đã áp dụng. Hợp đồng ghi lại đơn giá khi ký và giữ nguyên tới khi có phụ lục; dịch vụ phát sinh ngoài hợp đồng lấy đơn giá hiệu lực tại ngày phát sinh.

**Why this priority**: Cần cho lập hợp đồng và chi phí, nhưng là danh mục ít thay đổi; có thể khởi tạo trước bằng dữ liệu mẫu.

**Independent Test**: Tạo dịch vụ, tạo hai phiên bản đơn giá liên tiếp, thử tạo phiên bản chồng khoảng, thử sửa phiên bản đã áp dụng.

**Acceptance Scenarios**:

1. **Given** dịch vụ "Đưa đi khám" đơn giá 300.000 đồng/lần từ 01/01, **When** Quản lý viện tạo phiên bản 350.000 đồng từ 01/12, **Then** phiên bản cũ tự kết thúc hiệu lực ngày 30/11; chi phí phát sinh ngày 30/11 dùng 300.000 đồng, ngày 01/12 dùng 350.000 đồng (BR-M11-02).
2. **Given** phiên bản đơn giá đã được một chi phí hoặc hợp đồng tham chiếu, **When** Quản lý viện cố sửa đơn giá hay ngày hiệu lực, **Then** hệ thống chặn; chỉ được tạo phiên bản mới.
3. **Given** hai phiên bản của cùng dịch vụ, **When** tạo phiên bản có khoảng hiệu lực chồng nhau, **Then** hệ thống chặn (DBR-08).
4. **Given** hợp đồng Hiệu lực của A ký với giá tháng 12.000.000 đồng và dịch vụ phục hồi 200.000 đồng/buổi trong hợp đồng, **When** Quản lý viện tạo phiên bản giá tháng 13.000.000 đồng và giá phục hồi 250.000 đồng từ 01/01, **Then** chi phí của A từ 01/01 vẫn tính 12.000.000 đồng và 200.000 đồng/buổi; một lần "Đưa đi khám" (không thuộc hợp đồng của A) ngày 02/01 tính theo phiên bản hiệu lực ngày 02/01; muốn áp giá mới cho A thì phải có phụ lục (FR-036).
5. **Given** dịch vụ đã được lịch sử tham chiếu, **When** Quản lý viện muốn bỏ, **Then** chỉ có "Ngừng hiệu lực"; dịch vụ ngừng hiệu lực không chọn được cho hợp đồng mới hoặc yêu cầu thay đổi mới.

---

### Edge Cases

- **Đánh giá đầu vào quá hạn khi đang chờ lâu**: người trên danh sách chờ quá CFG-M01-02 kể từ lần đánh giá gần nhất thì điều kiện "đánh giá" trong danh sách điều kiện tiếp nhận hiển thị "Chưa đạt – cần đánh giá lại" (feature 001 FR-034a); điểm ưu tiên vẫn tính theo mức chăm sóc hiện hành.
- **Mức chăm sóc thay đổi khi người đó đang trên danh sách chờ hoặc đang có hợp đồng Nháp/Chờ ký** (đánh giá lại ở trạng thái Đang tiếp nhận, feature 001 FR-035): hồ sơ chờ nhận mức mới và điểm tính lại ngay; hợp đồng Nháp được hành chính sửa; hợp đồng Chờ ký phải "Trả về nháp"; hệ thống cảnh báo khi mức trên hợp đồng khác mức hiện hành.
- **Người cao tuổi vào ở muộn hoặc muốn vào sớm so với ngày bắt đầu hợp đồng**: vào muộn thì phí nội trú vẫn tính từ ngày bắt đầu, các ngày đó mang dấu "chưa vào ở" (FR-074); vào sớm thì lệnh Hoàn tất tiếp nhận bị chặn cho tới khi phụ lục đổi ngày bắt đầu được áp dụng (FR-075).
- **Hai giường trống cùng lúc cùng đề xuất một người**: một hồ sơ chờ chỉ ở Đã liên hệ với đúng một giường; khi E đã Đã liên hệ với G1, E bị loại khỏi đề xuất cho G2.
- **Giường trống có phân bổ tương lai** (feature 003): không kích hoạt đề xuất.
- **Gia đình từ chối giường được đề xuất nhưng vẫn muốn chờ**: hành chính ghi nhận "Từ chối giường này" kèm lý do; hồ sơ về Đang chờ, giữ ngày đăng ký và điểm, giường được trả về Trống và đề xuất người tiếp theo. Chỉ khi gia đình không tiếp tục chờ thì hồ sơ chuyển Từ chối, kéo theo Hủy tiếp nhận hồ sơ người cao tuổi (FR-002); nếu gia đình quay lại sau đó thì lập hồ sơ mới liên kết hồ sơ cũ (feature 001 FR-004).
- **Người trên danh sách chờ đăng ký bán trú**: BR-M02-01 chỉ kích hoạt theo giường; hồ sơ chờ bán trú được hành chính xét thủ công khi khu nghỉ còn chỗ (feature 003 FR-039).
- **Tạm vắng chuyển thành nằm viện** (Tạm vắng → Điều trị tại bệnh viện): lượt vắng về nhà đóng tại thời điểm chuyển viện, lượt vắng loại "Bệnh viện" mở từ thời điểm đó, đếm ngày vắng của dòng "Bệnh viện" bắt đầu lại từ 1; giữ giường nối tiếp không gián đoạn.
- **Loại vắng không có dòng trong bảng chính sách** (ví dụ "Đi khám trong ngày", "Lý do khác" với nội trú dài hạn trong bảng ví dụ): hệ thống áp hệ số 100% và giữ giường, đánh dấu "không có dòng chính sách" để Quản lý viện bổ sung bảng (1.3 "tạm vắng không mặc định đồng nghĩa miễn phí").
- **Vắng trong ngày không qua đêm** (đi khám 8:00–11:00): tính là một ngày vắng theo FR-058; hệ số theo dòng chính sách tương ứng.
- **Phụ lục có ngày hiệu lực rơi vào giữa một lượt vắng**: hệ số vắng áp trên giá mới từ ngày hiệu lực của phụ lục.
- **Yêu cầu thay đổi lưu trú Đã duyệt khi người cao tuổi kết thúc lưu trú hoặc qua đời trước ngày hiệu lực**: yêu cầu chuyển "Áp dụng không thành" (feature 000 FR-038), phụ lục chuyển Không áp dụng.
- **Người cao tuổi vẫn ở viện sau ngày kết thúc dự kiến** vì còn điều kiện chưa đạt: hồ sơ kết thúc mang dấu "quá ngày dự kiến", chi phí được sinh bù và tiếp tục, hành chính phải đặt ngày kết thúc mới trước khi thực hiện lệnh (FR-066a).
- **Kết thúc lưu trú khi đang Tạm vắng hoặc Điều trị tại bệnh viện**: lượt vắng đóng tại thời điểm kết thúc; điều kiện "bàn giao người cao tuổi" ghi nhận người đã đón là người nhận.
- **Qua đời trong lúc đang có hồ sơ kết thúc lưu trú chưa hoàn tất**: hồ sơ kết thúc lưu trú chuyển Đã hủy với lý do "Qua đời"; các điều kiện chưa đạt chuyển sang danh sách việc sau qua đời.
- **Ghi nhận qua đời với thời điểm trong quá khứ** (phát hiện muộn): chi phí nháp, công việc, liều thuốc sau thời điểm qua đời bị hủy theo feature sở hữu; bản ghi đã xác nhận sau thời điểm đó được đánh dấu để rà soát, không tự xóa.
- **Hai lệnh gần như đồng thời trên cùng một hợp đồng hoặc hồ sơ chờ** (ví dụ duyệt hai yêu cầu, hoặc hai hành chính cùng "Đã liên hệ"): chỉ lệnh đến trước được áp dụng; lệnh sau được kiểm tra lại trên trạng thái mới.
- **Hợp đồng đã ký nhưng người cao tuổi Hủy tiếp nhận trước khi vào ở**: hợp đồng Hiệu lực chuyển Chấm dứt với lý do "Hủy tiếp nhận"; nếu đã qua ngày bắt đầu hợp đồng thì phí "chưa vào ở" tới hết ngày hủy được giữ, miễn giảm chỉ qua khoản điều chỉnh có duyệt (FR-075a); đặt cọc giữ nguyên trạng thái ghi nhận, việc hoàn cọc do kế toán xử lý.

## Requirements *(mandatory)*

### Functional Requirements

Mọi yêu cầu dưới đây kế thừa feature 000: nhóm dữ liệu (mục 1.5), lý do bắt buộc cho lệnh nhóm 2, đính chính cho bản ghi đã xác nhận, nhật ký cho mọi thay đổi, tham số theo mã CFG, vòng đời yêu cầu phê duyệt, nguyên tắc "hoặc toàn bộ, hoặc không" khi một lệnh có nhiều tác động. Phân nhóm dữ liệu của feature này:

| Dữ liệu | Nhóm (1.5) | Thao tác được phép |
| --- | --- | --- |
| Dịch vụ | 1 – Danh mục | Tạo, sửa, Ngừng hiệu lực (Quản lý viện) |
| Phiên bản đơn giá | 1 – Danh mục, theo phiên bản | Tạo phiên bản mới; không sửa phiên bản đã được tham chiếu (6.4, DBR-08) |
| Bảng chính sách phí khi vắng của cơ sở | 1 – Tham số CFG-M02-05 | Cấu hình (Quản lý viện, feature 000) |
| Bảng điểm ưu tiên | 1 – Tham số CFG-M02-08 | Cấu hình (Quản lý viện, feature 000) |
| Lượt đăng ký tiếp nhận | 2 | Lập, cập nhật thông tin khi Đang xử lý; chuyển trạng thái qua lệnh (mục A) |
| Hồ sơ chờ | 2 | Chỉ qua lệnh ở bảng mục B |
| Đề nghị điều chỉnh điểm ưu tiên | 2 – Yêu cầu phê duyệt (feature 000) | Lập, gửi, duyệt, từ chối, hủy |
| Hợp đồng | 2 | Sửa chỉ ở Nháp; còn lại qua lệnh ở bảng mục C; không sửa khi Hiệu lực |
| Đặt cọc | 2; lần xác nhận là nhóm 3 | Xác nhận đặt cọc; sai sót xử lý bằng đính chính |
| Yêu cầu thay đổi lưu trú | 2 – Yêu cầu phê duyệt (feature 000) | Theo bảng mục F |
| Phụ lục hợp đồng | 2 | Chỉ phát sinh từ yêu cầu đã duyệt; không sửa |
| Lượt vắng | 2 khi đang mở; bất biến khi đã đóng | Mở, gia hạn dự kiến trở lại, đóng qua lệnh; bản đã đóng chỉ đính chính |
| Quyết định giữ giường khi vắng | 2 – Yêu cầu phê duyệt (feature 000) | Hệ thống tạo; Quản lý viện quyết định |
| Hồ sơ kết thúc lưu trú, danh sách việc sau qua đời | 2 | Theo bảng mục H, I |
| Bản ghi qua đời, bàn giao người cao tuổi, lịch sử điểm ưu tiên | 3 | Chỉ ghi thêm; đính chính |

#### A. Đăng ký tiếp nhận và danh sách điều kiện tiếp nhận

- **FR-001**: Hành chính MUST lập được lượt đăng ký tiếp nhận cho một hồ sơ người cao tuổi ở trạng thái Đang tiếp nhận (feature 001), gồm: ngày đăng ký, người liên hệ, nhu cầu, loại lưu trú mong muốn, mức chăm sóc dự kiến, ngày mong muốn vào ở, ghi chú. Mỗi hồ sơ người cao tuổi MUST có tối đa một lượt đăng ký ở trạng thái Đang xử lý hoặc Đang chờ. *(Nguồn: 6.1, UC-09)*
- **FR-002**: Lượt đăng ký MUST có trạng thái: Đang xử lý → Đang chờ (khi có hồ sơ chờ) → Đã tiếp nhận / Đã hủy; Đang chờ → Đang xử lý khi hồ sơ chờ chuyển Đã tiếp nhận. Lượt đăng ký MUST chuyển Đã tiếp nhận khi người cao tuổi Hoàn tất tiếp nhận và Đã hủy khi người cao tuổi Hủy tiếp nhận (feature 001). Khi hành chính chuyển hồ sơ chờ sang Từ chối hoặc Hủy chờ, hệ thống MUST, trong cùng một lần với lệnh đó: chuyển lượt đăng ký sang Đã hủy và thực hiện lệnh "Hủy tiếp nhận" (feature 001) cho người cao tuổi với cùng lý do và người thực hiện, kéo theo các tác động của lệnh đó (hợp đồng Nháp/Chờ ký chuyển Đã hủy; phân bổ tương lai và giữ chỗ giường bị hủy, feature 003). Nếu "Hủy tiếp nhận" không thực hiện được (ví dụ người cao tuổi không còn ở Đang tiếp nhận), lệnh trên hồ sơ chờ MUST bị từ chối. Khi hồ sơ chờ chuyển Hủy chờ do chính lệnh Hủy tiếp nhận từ feature 001, hệ thống MUST NOT kích hoạt lại Hủy tiếp nhận. *(Nguồn: 6.1, 6.2, 5.6; Clarification 2026-09-25, đề xuất Q-29)*
- **FR-003**: Hệ thống MUST hiển thị tiến trình của lượt đăng ký theo các bước 6.1 (Đăng ký → Thu thập thông tin → Đánh giá → Xác định lưu trú → Xác định chăm sóc → Xác định dịch vụ → Kiểm tra điều kiện → Tiếp nhận), mỗi bước suy ra từ dữ liệu đã có (hồ sơ, lần đánh giá, hợp đồng), không nhập tay trạng thái bước. *(Nguồn: 6.1, 1.3 "hệ thống chủ động")*
- **FR-004**: Hệ thống MUST cung cấp danh sách điều kiện tiếp nhận cho mỗi người Đang tiếp nhận, mỗi điều kiện có trạng thái Đạt / Chưa đạt / Không áp dụng và căn cứ: (a) đánh giá đầu vào Đã xác nhận và chưa quá CFG-M01-02 (feature 001); (b) hợp đồng Hiệu lực và thời điểm tiếp nhận không sớm hơn ngày bắt đầu hợp đồng (mục C, FR-075); (c) đặt cọc Đã đáp ứng hoặc hợp đồng không yêu cầu cọc (mục D); (d) với nội trú: có phân bổ giường (feature 003). Feature 001 MUST dùng đúng danh sách này khi kiểm tra lệnh "Hoàn tất tiếp nhận". *(Nguồn: 5.6, UC-08)*
- **FR-005**: Hệ thống MUST cảnh báo trên danh sách điều kiện khi mức chăm sóc hoặc loại lưu trú trên hợp đồng khác mức chăm sóc hiện hành hoặc loại lưu trú của lượt đăng ký/phân bổ giường.
- **FR-006**: Chỉ Hành chính MUST lập và cập nhật lượt đăng ký; Quản lý viện MUST xem được; người đại diện MUST xem được tiến trình của người mình đại diện qua cổng người thân. *(Nguồn: 4.4 dòng "Tiếp nhận, hợp đồng, đặt cọc": QL X, HC T, NT X)*

#### B. Danh sách chờ và điểm ưu tiên

- **FR-007**: Hành chính MUST tạo được hồ sơ chờ từ lượt đăng ký Đang xử lý, gồm: ngày đăng ký (lấy từ lượt đăng ký), nhu cầu, loại lưu trú, mức chăm sóc, tình huống đặc biệt (chọn từ danh mục của CFG-M02-08), trạng thái. Mỗi người cao tuổi MUST có tối đa một hồ sơ chờ chưa ở trạng thái cuối. *(Nguồn: 6.2, 3.2 HO_SO_CHO)*
- **FR-008**: Mức chăm sóc của hồ sơ chờ MUST lấy từ mức chăm sóc hiện hành của người cao tuổi nếu đã có đánh giá được chấp nhận (feature 001); nếu chưa, lấy mức chăm sóc dự kiến của lượt đăng ký và hiển thị "chưa đánh giá". Khi mức chăm sóc hiện hành thay đổi, hồ sơ chờ MUST nhận mức mới. *(Nguồn: 6.2)*
- **FR-009**: Điểm ưu tiên MUST do hệ thống tính, không nhập tay: điểm = điểm theo mức chăm sóc + điểm thời gian chờ (mỗi chu kỳ ngày chờ trọn vẹn được cộng điểm theo CFG-M02-08, mặc định \[mỗi 7 ngày +1\]) + tổng điểm các tình huống đặc biệt + tổng các điều chỉnh thủ công đã duyệt. Bảng điểm mức chăm sóc, danh mục tình huống đặc biệt và điểm của từng tình huống là cấu hình CFG-M02-08. Hệ thống MUST hiển thị chi tiết từng thành phần điểm. *(Nguồn: 6.2, BR-M02-10, CFG-M02-08)*
- **FR-010**: Bộ lập lịch hệ thống MUST tính lại điểm của mọi hồ sơ Đang chờ và Đã liên hệ mỗi ngày; điểm cũng MUST được tính lại ngay khi mức chăm sóc, tình huống đặc biệt hay điều chỉnh thủ công của hồ sơ thay đổi. Mỗi lần điểm thay đổi MUST lưu lịch sử (điểm cũ, điểm mới, nguyên nhân). *(Nguồn: BR-M02-10)*
- **FR-011**: Hành chính MUST lập được đề nghị điều chỉnh điểm (cộng hoặc trừ một số điểm), bắt buộc lý do; đề nghị là một yêu cầu phê duyệt (feature 000) do Quản lý viện duyệt; điểm chỉ thay đổi khi đề nghị được duyệt. Lịch sử điều chỉnh MUST lưu người đề nghị, người duyệt, lý do, thời điểm và MUST xem được bởi Quản lý viện và Hành chính. *(Nguồn: 6.2, BR-M02-10, 4.4 dòng "Danh sách chờ, điểm ưu tiên": QL D, HC T)*
- **FR-012**: Khi một giường chuyển Trống và không có phân bổ tương lai (feature 003 FR-014; giường Chờ vệ sinh chưa được tính là Trống), hệ thống MUST lọc hồ sơ chờ ở trạng thái Đang chờ thỏa đồng thời: loại lưu trú thuộc loại hình được phục vụ của loại phòng; mức chăm sóc thuộc danh sách mức được phép của phòng; giới tính phù hợp chính sách giới tính của phòng; phòng không đang chịu chặn cách ly; hồ sơ chưa từng được liên hệ và từ chối/hết hạn với chính giường này trong lượt trống hiện tại. Kết quả MUST sắp theo điểm ưu tiên giảm dần, hòa điểm thì theo ngày đăng ký sớm hơn, hòa tiếp thì theo thời điểm tạo hồ sơ chờ sớm hơn. *(Nguồn: BR-M02-01, BR-M02-10, feature 003 FR-018)*
- **FR-013**: Hệ thống MUST đề xuất CFG-M02-01 (mặc định \[3\]) hồ sơ đầu tiên của danh sách đã lọc cho hành chính, kèm điểm và lý do phù hợp, và gửi thông báo cho hành chính (feature 009). Nếu không có hồ sơ phù hợp, hệ thống MUST ghi nhận "không có người phù hợp" cho giường đó. Hành chính MAY xem toàn bộ danh sách đã lọc, không chỉ số người được đề xuất. *(Nguồn: BR-M02-01, CFG-M02-01, UC-10)*
- **FR-014**: Hành chính MUST thực hiện được lệnh "Ghi nhận đã liên hệ" cho một hồ sơ Đang chờ với một giường Trống cụ thể mà hồ sơ đó thỏa FR-012; hồ sơ chuyển Đã liên hệ, giường chuyển Đang giữ chỗ (hồ sơ chờ) với hạn = thời điểm liên hệ + CFG-M02-02 (mặc định \[48 giờ\]) (feature 003). Mỗi hồ sơ chờ MUST giữ tối đa một giường tại một thời điểm. Nếu hành chính chọn hồ sơ không nằm trong số được đề xuất, lý do MUST bắt buộc và được lưu để bảo đảm công bằng. *(Nguồn: BR-M02-02, BR-M02-10)*
- **FR-015**: Khi quá hạn giữ chỗ mà hồ sơ vẫn Đã liên hệ, Bộ lập lịch hệ thống MUST chuyển hồ sơ về Đang chờ (giữ nguyên ngày đăng ký và điểm), trả giường về Trống (feature 003) và chạy lại FR-012, FR-013 cho giường đó. *(Nguồn: BR-M02-02, BR-M03-06)*
- **FR-016**: Hồ sơ chờ MUST chuyển Đã tiếp nhận khi người cao tuổi được phân bổ giường (feature 003), bất kể giường đó có phải giường đang giữ hay không; nếu hồ sơ đang giữ một giường khác, giữ chỗ đó MUST kết thúc và giường về Trống. *(Nguồn: 6.2)*
- **FR-017**: Khi hồ sơ chờ không có cập nhật nào (lệnh, thay đổi thông tin, xác nhận còn nhu cầu) trong CFG-M02-03 (mặc định \[30 ngày\]), hệ thống MUST nhắc hành chính xác nhận gia đình còn nhu cầu; lệnh "Xác nhận còn nhu cầu" MUST đặt lại mốc tính. Hệ thống MUST NOT tự hủy hồ sơ chờ vì quá hạn. *(Nguồn: BR-M02-03, CFG-M02-03)*
- **FR-018**: Chỉ Hành chính MUST thao tác và Quản lý viện MUST xem, duyệt danh sách chờ; các vai trò khác, kể cả người thân, MUST NOT xem danh sách chờ. *(Nguồn: 4.4)*

**Bảng trạng thái hồ sơ chờ** *(6.2, BR-M02-01, 02, 03; quyền theo 4.4)*:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái kế tiếp | Ai thực hiện | Điều kiện (chặn nếu không đạt) | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Đưa vào danh sách chờ | Đang chờ | Hành chính | Lượt đăng ký Đang xử lý; người cao tuổi Đang tiếp nhận; chưa có hồ sơ chờ chưa ở trạng thái cuối (FR-007) | Lượt đăng ký chuyển Đang chờ; tính điểm lần đầu |
| Đang chờ | Ghi nhận đã liên hệ (kèm giường) | Đã liên hệ | Hành chính | Giường Trống, không có phân bổ tương lai; hồ sơ thỏa FR-012 với giường đó | Giường Đang giữ chỗ (hồ sơ chờ), hạn CFG-M02-02 (003) |
| Đã liên hệ | Hết hạn giữ chỗ | Đang chờ | Bộ lập lịch | Quá hạn giữ chỗ | Giường về Trống; đề xuất người tiếp theo (FR-015) |
| Đã liên hệ | Từ chối giường này | Đang chờ | Hành chính | Có lý do | Giường về Trống; đề xuất người tiếp theo |
| Đang chờ, Đã liên hệ | Phân bổ giường cho người này (003) | Đã tiếp nhận | Hệ thống, khi hành chính phân bổ | — | Kết thúc giữ chỗ; lượt đăng ký về Đang xử lý |
| Đang chờ, Đã liên hệ | Ghi nhận từ chối (gia đình không tiếp tục chờ) | Từ chối | Hành chính | Có lý do; người cao tuổi đang ở Đang tiếp nhận | Giường đang giữ (nếu có) về Trống; lượt đăng ký Đã hủy; Hủy tiếp nhận (001) cùng lý do (FR-002) |
| Đang chờ, Đã liên hệ | Hủy chờ | Hủy chờ | Hành chính; hệ thống khi người cao tuổi Hủy tiếp nhận (001) | Có lý do | Như trên; khi do hệ thống kích hoạt từ Hủy tiếp nhận thì không gọi lại Hủy tiếp nhận |
| Đang chờ | Xác nhận còn nhu cầu | Đang chờ | Hành chính | — | Đặt lại mốc CFG-M02-03 |

Đã tiếp nhận, Từ chối, Hủy chờ là trạng thái cuối.

#### C. Hợp đồng lưu trú

- **FR-019**: Hợp đồng MUST gồm: số hợp đồng (duy nhất), người cao tuổi, loại lưu trú, ngày bắt đầu, ngày kết thúc (bắt buộc với nội trú ngắn ngày; với dài hạn và bán trú là thời hạn hợp đồng), mức chăm sóc, danh sách dịch vụ đăng ký (đánh dấu dịch vụ thuộc gói), phòng/giường hoặc loại phòng (với nội trú), lịch đến theo buổi (với bán trú), người đại diện ký, chính sách chi phí (giá theo tháng / ngày / buổi và phiên bản đơn giá áp dụng), khoản cần đặt cọc hoặc "không yêu cầu", chính sách giữ giường và chính sách phí khi vắng (theo bảng cơ sở hoặc các dòng ghi đè), điều kiện chấm dứt. *(Nguồn: 6.3, 3.1–3.3, 15.7)*
- **FR-020**: Hợp đồng MUST có vòng đời theo bảng dưới đây; MUST NOT có thao tác "sửa trạng thái". Nội dung hợp đồng MUST chỉ sửa được ở trạng thái Nháp. *(Nguồn: 6.3, BR-M02-09, 1.5)*
- **FR-021**: Lệnh "Ghi nhận đã ký" MUST bắt buộc: ngày ký, người đại diện đã ký (phải là người đại diện hợp lệ của người cao tuổi theo feature 012, DBR-02), bản scan hợp đồng đã ký. Với hợp đồng nội trú, lệnh MUST bị chặn nếu người cao tuổi chưa có phân bổ giường (đang hiệu lực hoặc tương lai, feature 003) bắt đầu không muộn hơn ngày bắt đầu hợp đồng. Khi hợp đồng đã Hiệu lực mà phân bổ giường đó bị hủy hoặc đóng trước khi người cao tuổi vào ở, hệ thống MUST cảnh báo trên danh sách điều kiện tiếp nhận và nhắc hành chính mỗi ngày phân bổ lại cho tới khi có phân bổ hợp lệ; phí vẫn tính theo FR-074. *(Nguồn: 6.3, 2.4 "Người đại diện", FR-074; Clarification 2026-09-25, đề xuất Q-28)*
- **FR-022**: Hệ thống MUST NOT cho một người cao tuổi có hai hợp đồng Hiệu lực có khoảng thời gian [ngày bắt đầu, ngày kết thúc] chồng nhau, xét cả ngày kết thúc sau khi áp dụng phụ lục gia hạn; kiểm tra MUST thực hiện ở lệnh "Ghi nhận đã ký" và khi duyệt yêu cầu đổi thời gian. *(Nguồn: BR-M02-08, DBR-06)*
- **FR-023**: Hợp đồng Hiệu lực MUST NOT có bất kỳ thao tác sửa nào; mọi thay đổi MUST đi qua yêu cầu thay đổi lưu trú (mục F) và trở thành phụ lục. *(Nguồn: 6.3, BR-M02-09, DBR-06)*
- **FR-024**: Hệ thống MUST trả lời được "nội dung hợp đồng hiệu lực của người cao tuổi A tại ngày D" = hợp đồng gốc + các phụ lục Đã áp dụng có ngày hiệu lực không muộn hơn D, áp theo thứ tự ngày hiệu lực. Feature 001 (loại lưu trú hiện hành), 003 (giường), 005 (dịch vụ), 010 (đơn giá, chính sách vắng) MUST dùng kết quả này. *(Nguồn: BR-M02-09, 1.3)*
- **FR-025**: Hệ thống MUST cảnh báo khi lập hoặc gửi ký hợp đồng mà mức chăm sóc trên hợp đồng khác mức chăm sóc hiện hành (feature 001), hoặc dịch vụ/đơn giá không còn hiệu lực tại ngày bắt đầu.
- **FR-025a**: Hệ thống MUST tự xác định các điều khoản khác chuẩn của hợp đồng Nháp: (a) giá khác phiên bản đơn giá hiện hành tại ngày bắt đầu hợp đồng (FR-036); (b) có dòng ghi đè chính sách phí vắng hoặc chính sách giữ giường khác bảng CFG-M02-05 (FR-051). Hợp đồng có ít nhất một điều khoản khác chuẩn MUST được Quản lý viện duyệt qua yêu cầu phê duyệt loại "Duyệt điều khoản hợp đồng" (vòng đời feature 000; nội dung liệt kê từng điều khoản khác chuẩn kèm giá trị chuẩn và giá trị đề xuất, lý do bắt buộc) trước khi "Gửi ký". Trong khi yêu cầu ở Chờ duyệt, nội dung hợp đồng MUST bị khóa; mọi sửa nội dung sau khi được duyệt MUST làm yêu cầu đã duyệt mất hiệu lực và phải duyệt lại nếu vẫn còn điều khoản khác chuẩn. Hợp đồng không có điều khoản khác chuẩn MUST gửi ký được trực tiếp. Hợp đồng Hiệu lực MUST lưu tham chiếu tới yêu cầu đã duyệt các điều khoản khác chuẩn. *(Nguồn: 6.3, 6.7, 6.6 "thay đổi ảnh hưởng chi phí cần phê duyệt"; Clarification 2026-09-25, đề xuất Q-23)*
- **FR-026**: Với hợp đồng dài hạn, Bộ lập lịch hệ thống MUST nhắc hành chính trước ngày kết thúc CFG-M02-04 (mặc định \[30 ngày\]). Với hợp đồng ngắn ngày, vào ngày kết thúc mà chưa có hồ sơ kết thúc lưu trú hoặc yêu cầu thay đổi thời gian đang mở, hệ thống MUST nhắc hành chính tạo một trong hai; hệ thống MUST NOT tự gia hạn bất kỳ hợp đồng nào. *(Nguồn: BR-M02-05, 3.2, CFG-M02-04)*
- **FR-027**: Hợp đồng MUST chuyển Kết thúc khi lệnh "Kết thúc lưu trú" có thời điểm hiệu lực không sớm hơn ngày kết thúc hợp đồng; MUST chuyển Chấm dứt khi lưu trú kết thúc sớm hơn ngày kết thúc hợp đồng, khi người cao tuổi qua đời, hoặc khi người cao tuổi Hủy tiếp nhận sau khi hợp đồng đã Hiệu lực; lý do chấm dứt MUST được lưu. *(Nguồn: 6.3, 6.8)*
- **FR-028**: Hành chính MUST thực hiện mọi lệnh trên hợp đồng; Quản lý viện MUST xem được và duyệt điều khoản khác chuẩn (FR-025a); người đại diện MUST xem được hợp đồng, phụ lục, trạng thái đặt cọc của người mình đại diện qua cổng; các vai trò khác MUST NOT xem nội dung tài chính của hợp đồng. *(Nguồn: 4.4 dòng "Tiếp nhận, hợp đồng, đặt cọc")*
- **FR-029**: Khi hợp đồng (dài hạn hoặc ngắn ngày) qua ngày kết thúc mà người cao tuổi vẫn Đang lưu trú, Tạm vắng hoặc Điều trị tại bệnh viện và chưa có phụ lục gia hạn hay lệnh Kết thúc lưu trú, hợp đồng MUST giữ trạng thái Hiệu lực với ngày kết thúc không đổi (không coi là tự gia hạn) và MUST được gắn dấu "quá hạn hợp đồng" từ ngày liền sau ngày kết thúc. Trong thời gian quá hạn: nội dung hợp đồng hiệu lực (FR-024) và căn cứ tính phí MUST là điều khoản cuối cùng trước ngày kết thúc; hệ thống MUST nhắc hành chính mỗi ngày; khi số ngày quá hạn vượt CFG-M02-10 (đề xuất, mặc định \[7 ngày\]), hệ thống MUST báo Quản lý viện, một lần cho mỗi hợp đồng. Dấu "quá hạn hợp đồng" MUST hiển thị trên hồ sơ người cao tuổi, danh sách hợp đồng và cổng người thân của người đại diện, và MUST được gỡ khi một phụ lục gia hạn được áp dụng (các ngày quá hạn trước ngày hiệu lực của phụ lục vẫn tính theo điều khoản cũ, FR-044) hoặc khi lưu trú kết thúc (hợp đồng chuyển Kết thúc theo FR-027). Dấu này MUST NOT chặn các lệnh và yêu cầu thay đổi khác. *(Nguồn: BR-M02-05; Clarification 2026-09-25, đề xuất Q-20)*

**Bảng trạng thái hợp đồng** *(6.3, BR-M02-05, 08, 09; quyền theo 4.4)*:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái kế tiếp | Ai thực hiện | Điều kiện (chặn nếu không đạt) | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Lập hợp đồng | Nháp | Hành chính | Người cao tuổi Đang tiếp nhận hoặc đang lưu trú (cho hợp đồng kế tiếp); có người đại diện (012) | Cấp số hợp đồng |
| Nháp | Sửa nội dung | Nháp | Hành chính | — | Ghi nhật ký trước/sau |
| Nháp | Gửi duyệt điều khoản khác chuẩn | Nháp (đang chờ duyệt điều khoản) | Hành chính | Có ít nhất một điều khoản khác chuẩn (FR-025a); có lý do | Khóa nội dung; tạo yêu cầu cho Quản lý viện |
| Nháp (đang chờ duyệt điều khoản) | Quản lý viện duyệt / từ chối | Nháp | Quản lý viện | Feature 000 | Duyệt: cho phép Gửi ký; từ chối: mở khóa để sửa |
| Nháp | Gửi ký | Chờ ký | Hành chính | Đủ nội dung bắt buộc FR-019; nếu có điều khoản khác chuẩn thì đã được duyệt và không sửa sau khi duyệt (FR-025a) | Khóa nội dung; cảnh báo theo FR-025 |
| Chờ ký | Trả về nháp | Nháp | Hành chính | Có lý do | Mở khóa nội dung |
| Chờ ký | Ghi nhận đã ký | Hiệu lực | Hành chính | FR-021 (với nội trú: có phân bổ giường bắt đầu không muộn hơn ngày bắt đầu hợp đồng); không chồng thời gian (FR-022) | Điều kiện (b) của FR-004 đạt; khoản đặt cọc bắt đầu theo dõi (mục D) |
| Nháp, Chờ ký | Hủy hợp đồng | Đã hủy | Hành chính; hệ thống khi người cao tuổi Hủy tiếp nhận (001) | Có lý do | — |
| Hiệu lực | Áp dụng phụ lục (mục F) | Hiệu lực | Bộ lập lịch / hệ thống | Yêu cầu Đã duyệt tới ngày hiệu lực | Nội dung hiệu lực đổi từ ngày hiệu lực (FR-024) |
| Hiệu lực | Kết thúc lưu trú với thời điểm không sớm hơn ngày kết thúc hợp đồng | Kết thúc | Hệ thống, khi lệnh của 001 thực hiện | Mục H | — |
| Hiệu lực | Kết thúc lưu trú sớm hơn ngày kết thúc / Ghi nhận qua đời / Hủy tiếp nhận | Chấm dứt | Hệ thống, khi lệnh của 001 thực hiện | Mục H, I | Lưu lý do chấm dứt |
| Hiệu lực | Qua ngày kết thúc, chưa có phụ lục gia hạn hay Kết thúc lưu trú | Hiệu lực (dấu "quá hạn hợp đồng") | Bộ lập lịch | — | Phí theo điều khoản cũ; nhắc hành chính hằng ngày; quá CFG-M02-10 báo Quản lý viện (FR-029) |

Kết thúc, Chấm dứt, Đã hủy là trạng thái cuối. Người cao tuổi đã Đang lưu trú MAY có hợp đồng kế tiếp ở Nháp/Chờ ký với ngày bắt đầu sau ngày kết thúc của hợp đồng hiện tại (tái ký), chịu FR-022.

#### D. Đặt cọc

- **FR-030**: Hệ thống MUST NOT thực hiện nghiệp vụ thu tiền, hoàn tiền hay hạch toán đặt cọc. Hệ thống MUST chỉ ghi nhận: khoản cần đặt cọc (từ hợp đồng), trạng thái Chưa đáp ứng / Đã đáp ứng / Không yêu cầu, thời điểm xác nhận, người xác nhận, nguồn xác nhận (ví dụ số phiếu thu của kế toán), bằng chứng đính kèm (tùy chọn). *(Nguồn: 6.5, 1.2)*
- **FR-031**: Khi hợp đồng Hiệu lực có khoản cần đặt cọc, trạng thái đặt cọc MUST bắt đầu ở Chưa đáp ứng; hợp đồng ghi "không yêu cầu" thì trạng thái MUST là Không yêu cầu và được coi là đạt. *(Nguồn: 6.5, 5.6)*
- **FR-032**: Lệnh "Xác nhận đặt cọc" MUST do Hành chính thực hiện, bắt buộc nguồn xác nhận; trạng thái chuyển Đã đáp ứng. Lần xác nhận là bản ghi đã xác nhận (nhóm 3); sai sót MUST xử lý bằng đính chính (feature 000), và nếu đính chính làm trạng thái về Chưa đáp ứng khi người cao tuổi đã Đang lưu trú thì hệ thống MUST báo Quản lý viện, không thay đổi trạng thái lưu trú. *(Nguồn: 6.5, 1.5)*
- **FR-033**: Trạng thái đặt cọc MUST là điều kiện (c) của danh sách điều kiện tiếp nhận (FR-004). *(Nguồn: 5.6, 6.5)*

#### E. Dịch vụ và đơn giá

- **FR-034**: Quản lý viện MUST quản lý được danh mục dịch vụ (loại: chăm sóc, phục hồi, hoạt động, đưa đi khám, tiêm, dịch vụ khác; tên; đơn vị tính; trạng thái) và vật phẩm/phí lưu trú có đơn giá (giá tháng/ngày theo loại phòng hoặc mức chăm sóc, giá buổi bán trú). Dịch vụ đã được tham chiếu MUST chỉ Ngừng hiệu lực, không xóa. *(Nguồn: 6.4, 15.2, 1.5 nhóm 1)*
- **FR-035**: Đơn giá MUST được quản lý bằng phiên bản có ngày hiệu lực từ; tạo phiên bản mới MUST tự đặt ngày hiệu lực đến của phiên bản trước là ngày liền trước. Các phiên bản của cùng một dịch vụ MUST NOT chồng khoảng hiệu lực. Phiên bản đã được hợp đồng hoặc chi phí tham chiếu MUST NOT bị sửa. *(Nguồn: 6.4, DBR-08)*
- **FR-036**: Khi hợp đồng chuyển Hiệu lực, hợp đồng MUST ghi lại đơn giá của mọi khoản trong hợp đồng (phí lưu trú theo tháng/ngày/buổi và từng dịch vụ đăng ký), lấy từ giá riêng đã duyệt (FR-025a) hoặc phiên bản đơn giá hiệu lực tại ngày bắt đầu hợp đồng. Các khoản trong hợp đồng MUST được tính theo đơn giá đã ghi của nội dung hợp đồng hiệu lực tại ngày phát sinh (FR-024), không theo phiên bản đơn giá mới tạo sau khi ký; đơn giá này chỉ thay đổi qua phụ lục (mục F). Dịch vụ, vật phẩm phát sinh không thuộc hợp đồng MUST tính theo phiên bản đơn giá hiệu lực tại ngày phát sinh (BR-M11-02). Tạo phiên bản đơn giá mới MUST NOT tự tạo thay đổi nào cho hợp đồng đang hiệu lực; hệ thống MUST cho Quản lý viện xem danh sách hợp đồng đang hiệu lực có đơn giá khác phiên bản mới. *(Nguồn: 6.3, 6.4, BR-M02-09, BR-M11-02; Clarification 2026-09-25, đề xuất Q-25)*

#### F. Yêu cầu thay đổi lưu trú và phụ lục hợp đồng

- **FR-037**: Yêu cầu thay đổi lưu trú là một loại yêu cầu phê duyệt (vòng đời feature 000: Nháp → Chờ duyệt → Đã duyệt (chờ hiệu lực) → Đã áp dụng / Từ chối / Hủy / Áp dụng không thành). Mỗi yêu cầu MUST gắn với một hợp đồng Hiệu lực và thuộc một hoặc nhiều loại: đổi loại lưu trú, đổi mức chăm sóc, đổi dịch vụ (thêm/bớt), đổi thời gian (gia hạn/rút ngắn), đổi phòng/giường. *(Nguồn: 6.6, UC-13)*
- **FR-038**: Yêu cầu MUST lưu: giá trị trước (lấy tự động từ nội dung hợp đồng hiệu lực), giá trị sau, ảnh hưởng chi phí (đơn giá trước/sau do hệ thống tính), người yêu cầu, người duyệt, thời điểm, ngày hiệu lực mong muốn và thực tế, lý do. *(Nguồn: 6.6)*
- **FR-039**: Hành chính và Bác sĩ MUST lập được yêu cầu (bác sĩ chỉ với loại đổi mức chăm sóc); hệ thống MUST tự lập yêu cầu đổi mức chăm sóc ở Chờ duyệt khi đánh giá lại cho mức khác mức hiện hành (BR-M01-03, feature 001 FR-036), và hành chính MUST bổ sung được giá trị chi phí, ngày hiệu lực cho yêu cầu này trước khi duyệt. Người đại diện MUST gửi được yêu cầu qua cổng người thân; yêu cầu này được tạo ở Nháp và hành chính được báo để hoàn thiện, gửi duyệt. *(Nguồn: UC-13, 4.4 dòng "Yêu cầu thay đổi lưu trú": BS T, HC T, NT T²)*
- **FR-040**: Ngày hiệu lực mong muốn MUST NOT sớm hơn ngày bắt đầu hợp đồng gốc. *(Nguồn: DBR-07)*
- **FR-041**: Khi duyệt, hệ thống MUST kiểm tra điều kiện của từng loại thay đổi: đổi phòng/giường — điều kiện phân bổ của feature 003 FR-018 cho giường mới và khoảng thời gian từ ngày hiệu lực; đổi thời gian — FR-022; đổi dịch vụ — dịch vụ Hiệu lực tại ngày hiệu lực; đổi loại lưu trú sang bán trú — kèm lịch đến và kiểm tra sức chứa khu nghỉ (feature 003 FR-039); đổi loại lưu trú từ bán trú sang nội trú — kèm giường mới. Vi phạm bất kỳ điều kiện nào thì MUST chặn duyệt và liệt kê mọi điều kiện vi phạm. *(Nguồn: 6.6, BR-M03-01)*
- **FR-042**: Khi yêu cầu được duyệt, hệ thống MUST tạo phụ lục hợp đồng ở trạng thái Chờ hiệu lực, gắn với hợp đồng gốc và yêu cầu (DBR-07); với đổi phòng/giường, feature 003 MUST tạo phân bổ tương lai. Khi yêu cầu được áp dụng (feature 000 FR-037), phụ lục MUST chuyển Đã áp dụng và các tác động MUST xảy ra trong cùng một lần: đổi mức chăm sóc hiện hành (feature 001 FR-052) và kích hoạt xem xét kế hoạch chăm sóc (feature 005); đổi đơn giá cho chi phí từ ngày hiệu lực (feature 010); chuyển giường (feature 003 FR-029); đổi ngày kết thúc hợp đồng; đổi dịch vụ đăng ký. Khi yêu cầu chuyển Áp dụng không thành, phụ lục MUST chuyển Không áp dụng. *(Nguồn: BR-M02-04, DBR-07, feature 000 FR-037 → FR-039)*
- **FR-043**: Mọi yêu cầu thay đổi lưu trú MUST được Quản lý viện duyệt trước khi áp dụng, kể cả khi không ảnh hưởng chi phí, vì mọi thay đổi đều tạo phụ lục và DBR-07 yêu cầu phụ lục gắn với yêu cầu đã duyệt. Quy tắc tự duyệt theo feature 000 FR-035. *(Nguồn: 6.6, DBR-07, 4.4 QL D)*
- **FR-044**: Chi phí của các ngày trước ngày hiệu lực thực tế MUST tính theo giá trị cũ; hệ thống MUST NOT tính lùi. *(Nguồn: BR-M02-04, feature 000 FR-037)*
- **FR-045**: Điều kiện áp dụng tại ngày hiệu lực MUST gồm: hợp đồng gốc vẫn Hiệu lực; người cao tuổi chưa ở trạng thái cuối; các điều kiện ở FR-041 còn thỏa. Không thỏa thì yêu cầu chuyển Áp dụng không thành (feature 000 FR-038). *(Nguồn: feature 000 FR-038)*
- **FR-046**: Hệ thống MUST chặn duyệt một yêu cầu nếu người cao tuổi đã có một yêu cầu khác Đã duyệt (chờ hiệu lực) thay đổi cùng thuộc tính (cùng loại thay đổi); người dùng phải hủy yêu cầu trước hoặc gộp nội dung. *(Suy ra từ BR-M02-09)*
- **FR-047**: Phụ lục MUST NOT bị sửa hay xóa; sai sót MUST xử lý bằng một yêu cầu thay đổi mới. *(Nguồn: 1.5, BR-M02-09)*

#### G. Tạm vắng và chính sách phí khi vắng

- **FR-048**: Mỗi lần người cao tuổi chuyển sang Tạm vắng hoặc Điều trị tại bệnh viện (feature 001), hệ thống MUST mở một lượt vắng gồm: loại vắng (Về nhà, Đi chơi với gia đình, Đi khám trong ngày, Lý do khác, Bệnh viện), thời điểm rời viện, thời điểm dự kiến trở lại (bắt buộc với Tạm vắng; với Bệnh viện MAY để trống), lý do, người đón (MUST thuộc danh sách được phép đón, feature 012; với Bệnh viện ghi người/đơn vị đưa đi), người bàn giao (nhân viên), tình trạng giường (giữ/không giữ), chính sách áp dụng (nguồn: bảng cơ sở hoặc hợp đồng; dòng chính sách). *(Nguồn: 6.7, 3.2 LUOT_VANG, 5.6)*
- **FR-049**: Hành chính và Trưởng tầng MUST thực hiện được "Cho tạm vắng", "Gia hạn dự kiến trở lại" (bắt buộc lý do; lưu lịch sử các mốc) và "Ghi nhận trở về" trực tiếp, không qua phê duyệt (feature 001 FR-047a); Điều dưỡng MUST xem được lượt vắng; người thân MUST xem được lượt vắng của người mình liên quan. *(Nguồn: UC-15, 4.4 dòng "Tạm vắng, trở về": QL D, TT T, ĐD X, HC T, NT X)*
- **FR-050**: Bảng chính sách phí khi vắng CFG-M02-05 MUST có các dòng gồm: loại lưu trú, loại vắng, khoảng ngày vắng (từ ngày thứ … đến ngày thứ …, hoặc không giới hạn), hệ số phí lưu trú (phần trăm, "Theo hợp đồng" hoặc "Quản lý quyết định"), giữ giường (Có / Không / "Theo hợp đồng" / "Quản lý quyết định"). Giá trị mặc định MUST là bảng ví dụ 6.7 (Q-08). Hệ thống MUST chặn lưu bảng có hai dòng cùng loại lưu trú, loại vắng mà khoảng ngày chồng nhau. *(Nguồn: 6.7, CFG-M02-05, Q-08)*
- **FR-051**: Hợp đồng MAY ghi đè bảng chính sách bằng các dòng riêng cùng cấu trúc FR-050; khi tra, dòng của hợp đồng hiệu lực tại ngày vắng MUST được ưu tiên hơn dòng của bảng cơ sở. Dòng bảng cơ sở mang giá trị "Theo hợp đồng" MUST lấy từ dòng ghi đè của hợp đồng; nếu hợp đồng không có dòng tương ứng, hệ thống MUST áp FR-053. *(Nguồn: 6.7 "có thể được ghi đè theo từng hợp đồng")*
- **FR-052**: Với mỗi ngày vắng, Bộ lập lịch hệ thống MUST tra chính sách theo loại lưu trú hiện hành, loại vắng, số thứ tự ngày vắng để xác định hệ số phí và giữ giường, lưu kết quả vào lượt vắng theo ngày và cung cấp cho feature 010 để sinh chi phí nháp tham chiếu lượt vắng. Bảng dùng cho một ngày MUST là giá trị tham số hiện hành tại thời điểm tính ngày đó; các ngày đã tính MUST NOT đổi khi bảng thay đổi sau đó. *(Nguồn: BR-M02-06, 15.2, feature 000)*
- **FR-053**: Khi không tìm được dòng chính sách phù hợp, hệ thống MUST áp hệ số 100% và giữ giường, đánh dấu "không có dòng chính sách" trên ngày vắng và cảnh báo Quản lý viện. *(Nguồn: 6.7 "tạm vắng không mặc định đồng nghĩa với miễn phí")*
- **FR-054**: Khi lượt vắng bắt đầu, nếu dòng chính sách của ngày thứ nhất có giữ giường "Có", feature 003 MUST chuyển giường sang Đang giữ chỗ (người vắng); nếu "Không", feature 003 MUST đóng phân bổ tại thời điểm rời viện và giường chuyển Chờ vệ sinh (feature 003 FR-011a). *(Nguồn: 5.6, 6.7, feature 003 FR-024, BR-M03-09)*
- **FR-055**: Ngưỡng giữ giường là ngày vắng đầu tiên mà dòng chính sách áp dụng có giữ giường "Không" hoặc "Quản lý quyết định" trong khi giường đang được giữ. Khi tới ngưỡng, hệ thống MUST tạo yêu cầu "Quyết định giữ giường khi vắng" (yêu cầu phê duyệt, feature 000) cho Quản lý viện, gồm lượt vắng, số ngày đã vắng, dòng chính sách, hai lựa chọn: "Giữ tiếp" (bắt buộc hệ số phí và ngày xem xét lại) hoặc "Giải phóng" (bắt buộc hệ số phí cho các ngày còn lại của lượt vắng, mặc định 0%). *(Nguồn: BR-M02-07, 6.7)*
- **FR-056**: Khi yêu cầu FR-055 chưa có quyết định, giường MUST tiếp tục được giữ và các ngày vắng MUST dùng hệ số của dòng chính sách liền trước, đánh dấu "chờ quyết định"; khi có quyết định, hệ thống MUST tính lại hệ số các ngày đã đánh dấu và feature 010 MUST tính lại chi phí nháp tương ứng (BR-M11-05). Chọn "Giải phóng" thì feature 003 MUST đóng phân bổ và giường chuyển Trống ngay khi quyết định; chọn "Giữ tiếp" thì tới ngày xem xét lại hệ thống MUST tạo yêu cầu quyết định mới. *(Nguồn: BR-M02-07, BR-M11-05)*
- **FR-057**: Khi người đang Tạm vắng chuyển sang Điều trị tại bệnh viện, lượt vắng hiện tại MUST đóng tại thời điểm chuyển và lượt vắng loại Bệnh viện MUST mở từ cùng thời điểm; số thứ tự ngày vắng của lượt mới bắt đầu lại từ 1; nếu giường đang giữ thì tiếp tục giữ không gián đoạn. Ngày chuyển viện MUST thuộc lượt vắng loại Bệnh viện (ngày vắng thứ nhất của lượt mới) và MUST NOT được tính thêm lần nữa cho lượt vắng trước; ngưỡng giữ giường (FR-055) của lượt mới MUST tính theo số ngày của lượt mới. *(Nguồn: 5.5, 6.7; Clarification 2026-09-25, đề xuất Q-24)*
- **FR-058**: Ngày vắng là mỗi ngày dương lịch có ít nhất một phần thời gian vắng, tính từ ngày rời viện tới ngày trở về, cả hai đầu. Ngày rời viện là ngày vắng thứ nhất. *(Suy ra từ BR-M02-06; múi giờ theo DBR-25)*
- **FR-059**: "Ghi nhận trở về" MUST đóng lượt vắng với thời điểm về; nếu giường đang giữ, feature 003 MUST chuyển giường về Đang sử dụng; nếu giường đã được giải phóng, hệ thống MUST cảnh báo cần phân bổ giường mới và mở danh sách giường phù hợp (feature 003 FR-022). *(Nguồn: 5.6, feature 003 FR-024)*
- **FR-060**: Với bán trú, hệ số phí của một buổi có lịch đến MUST tra theo trạng thái có mặt (3.4): Vắng có báo (gia đình báo trước CFG-M02-06, mặc định \[24 giờ\]) và Vắng không báo (quá giờ đến dự kiến CFG-M02-07, mặc định \[2 giờ\] mà chưa điểm danh) theo dòng bán trú của CFG-M02-05 (mặc định 0% và 50%). Hành chính MUST ghi nhận được báo vắng bán trú kèm thời điểm gia đình báo; hệ thống MUST tự xác định "có báo trước đủ thời hạn" hay không. Điểm danh và trạng thái có mặt thuộc feature 005. *(Nguồn: 3.4, 6.7, CFG-M02-06, CFG-M02-07)*
- **FR-061**: Lượt vắng đã đóng MUST NOT bị sửa; sai sót về thời điểm MUST xử lý bằng đính chính (feature 000), và đính chính MUST kích hoạt tính lại hệ số ngày vắng và chi phí theo BR-M11-05. *(Nguồn: 1.5, BR-M11-05)*

#### H. Kết thúc lưu trú

- **FR-062**: Hành chính MUST lập được hồ sơ kết thúc lưu trú cho người cao tuổi đang ở Đang lưu trú, Tạm vắng hoặc Điều trị tại bệnh viện, gồm: trường hợp (Hết hợp đồng, Xuất viện, Chuyển cơ sở, Chấm dứt hợp đồng), ngày kết thúc dự kiến, lý do, người yêu cầu, nơi chuyển đến (với Chuyển cơ sở). Mỗi người MUST có tối đa một hồ sơ kết thúc lưu trú chưa ở trạng thái cuối. Bác sĩ MUST lập được hồ sơ với trường hợp Xuất viện hoặc Chuyển cơ sở vì lý do y tế. *(Nguồn: 6.8, UC-17, 4.4 dòng "Kết thúc lưu trú, qua đời": BS T, HC T)*
- **FR-063**: Hệ thống MUST tạo và tự kiểm tra danh sách điều kiện kết thúc, mỗi điều kiện có trạng thái Đạt / Chưa đạt / Đạt (ngoại lệ) và căn cứ:
  (a) chi phí đã chốt đến ngày kết thúc (feature 010) — xem FR-066;
  (b) không còn đồ gửi đang giữ (feature 013);
  (c) không còn thuốc gia đình gửi đang giữ (feature 006);
  (d) không còn cảnh báo hoặc sự cố đang mở (feature 007);
  (e) đã ghi nhận bàn giao người cao tuổi: người nhận (người thân hoặc cơ sở tiếp nhận), thời điểm, nhân viên bàn giao;
  (f) hợp đồng được xử lý và (g) giường được giải phóng — do hệ thống thực hiện khi lệnh "Kết thúc lưu trú" chạy, hiển thị "Tự động".
  *(Nguồn: 6.8, 5.6)*
- **FR-064**: Với mỗi điều kiện (a) → (d), hành chính MAY lập yêu cầu ngoại lệ (yêu cầu phê duyệt, feature 000) kèm lý do; điều kiện chỉ chuyển Đạt (ngoại lệ) khi Quản lý viện duyệt. Điều kiện (e) MUST NOT có ngoại lệ. *(Nguồn: 5.6 "trừ khi quản lý duyệt ngoại lệ")*
- **FR-065**: Lệnh "Kết thúc lưu trú" (feature 001) MUST chỉ thực hiện được khi mọi điều kiện (a) → (e) Đạt hoặc Đạt (ngoại lệ); nếu không, MUST từ chối và liệt kê mọi điều kiện chưa đạt. Khi thành công, trong cùng một lần: hồ sơ kết thúc chuyển Hoàn tất; hợp đồng chuyển Kết thúc hoặc Chấm dứt (FR-027); lượt vắng đang mở (nếu có) đóng; hồ sơ chờ, yêu cầu thay đổi đang mở chuyển Hủy / Áp dụng không thành; feature 003 đóng phân bổ; các tác động còn lại theo feature 001 (hủy lịch tương lai, khóa tài khoản người thân sau CFG-M01-04). *(Nguồn: 5.6, 6.8, BR-M11-08)*
- **FR-066**: Chốt chi phí kỳ cuối MUST diễn ra trước lệnh "Kết thúc lưu trú", theo hai bước: (1) khi hồ sơ kết thúc lưu trú được lập, hệ thống MUST dừng sinh chi phí tự động từ ngày sau ngày kết thúc dự kiến và yêu cầu feature 010 tạo bảng kỳ cuối nháp tính đến ngày kết thúc dự kiến; (2) hành chính kiểm tra, Quản lý viện duyệt và chốt kỳ cuối theo vòng đời chi phí của feature 010; điều kiện (a) chỉ Đạt khi kỳ cuối đã chốt. Khi ngày kết thúc dự kiến thay đổi (FR-067), việc dừng sinh chi phí MUST dời theo ngày mới, kỳ cuối nháp MUST được tính lại; nếu kỳ cuối đã chốt thì điều kiện (a) trở về Chưa đạt và phần chênh lệch được xử lý bằng khoản điều chỉnh (feature 010). Lệnh "Kết thúc lưu trú" MUST có thời điểm hiệu lực trong ngày kết thúc dự kiến; muốn kết thúc ngày khác thì phải đổi ngày kết thúc dự kiến trước. Nếu hồ sơ kết thúc bị hủy, sinh chi phí MUST tiếp tục từ ngày đã dừng và các ngày bị bỏ qua MUST được sinh bù. *(Nguồn: 5.6, BR-M11-08, 15.7; Clarification 2026-09-25, đề xuất Q-21)*
- **FR-066a**: Khi hết ngày kết thúc dự kiến mà lệnh "Kết thúc lưu trú" chưa được thực hiện và hồ sơ kết thúc vẫn Đang chuẩn bị, Bộ lập lịch hệ thống MUST, từ ngày liền sau: gắn dấu "quá ngày dự kiến" cho hồ sơ kết thúc; sinh bù chi phí tự động cho các ngày từ ngày liền sau ngày kết thúc dự kiến và tiếp tục sinh hằng ngày như khi chưa có hồ sơ kết thúc; chuyển điều kiện (a) về Chưa đạt (kỳ cuối đã chốt, nếu có, được xử lý bằng khoản điều chỉnh ở feature 010); nhắc hành chính mỗi ngày đặt ngày kết thúc dự kiến mới. Lệnh "Kết thúc lưu trú" MUST bị chặn cho tới khi có ngày kết thúc dự kiến mới không sớm hơn hôm nay; khi ngày mới được đặt, dấu được gỡ và FR-066 áp dụng lại với ngày mới. *(Clarification 2026-09-25, đề xuất Q-26)*
- **FR-067**: Hồ sơ kết thúc lưu trú MUST có trạng thái: Đang chuẩn bị → Hoàn tất / Đã hủy (hủy bắt buộc lý do; tự hủy khi người cao tuổi qua đời, các điều kiện chưa đạt chuyển sang danh sách việc sau qua đời). Trong khi Đang chuẩn bị, ngày kết thúc dự kiến MAY được đổi kèm lý do.
- **FR-068**: Người đại diện MUST xem được tiến trình kết thúc lưu trú và các điều kiện liên quan gia đình (nhận đồ gửi, thuốc gửi, chi phí kỳ cuối) qua cổng người thân. *(Nguồn: 4.4 NT X)*

#### I. Qua đời

- **FR-069**: Lệnh "Ghi nhận qua đời" (quyền và điều kiện theo feature 001 FR-047b) MUST ghi: thời điểm, địa điểm, người phát hiện, người xác nhận, thông tin nguyên nhân nếu đã xác định, bằng chứng (bắt buộc khi hành chính thực hiện). Bản ghi qua đời là nhóm 3; bổ sung nguyên nhân hay sửa sai MUST bằng đính chính. *(Nguồn: 6.8, UC-18)*
- **FR-070**: Lệnh "Ghi nhận qua đời" MUST NOT bị chặn bởi đồ gửi, thuốc gửi, chi phí, cảnh báo hay sự cố. Khi thành công, trong cùng một lần: hợp đồng chuyển Chấm dứt với lý do "Qua đời"; lượt vắng đang mở đóng tại thời điểm qua đời; hồ sơ kết thúc lưu trú đang mở chuyển Đã hủy; hồ sơ chờ, yêu cầu thay đổi đang mở chuyển Hủy / Áp dụng không thành; feature 003 đóng phân bổ và giải phóng giường (giường chuyển Chờ vệ sinh, FR-011a của feature 003); sinh chi phí tự động dừng từ thời điểm qua đời (feature 010); các tác động còn lại theo feature 001 (hủy lịch tương lai, thông báo người liên hệ chính, hồ sơ chỉ đọc). *(Nguồn: 5.6, 6.8)*
- **FR-071**: Khi ghi nhận qua đời, hệ thống MUST tạo danh sách việc bắt buộc sau qua đời gồm: xử lý đồ gửi (feature 013); hoàn trả thuốc gia đình gửi (feature 006); chốt chi phí kỳ cuối (feature 010); xử lý cảnh báo/sự cố đang mở (feature 007). Mỗi mục có trạng thái Chưa hoàn thành / Hoàn thành / Không áp dụng, được hệ thống tự cập nhật từ feature sở hữu. *(Nguồn: 6.8)*
- **FR-072**: Khi danh sách việc sau qua đời còn mục Chưa hoàn thành, hệ thống MUST nhắc hành chính theo chu kỳ CFG-M02-09 (đề xuất, mặc định \[1 ngày\]); khi mọi mục Hoàn thành hoặc Không áp dụng, hồ sơ lưu trú MUST chuyển "Đã đóng". Các thao tác hoàn thành những mục này MUST được phép dù hồ sơ người cao tuổi ở trạng thái cuối, vì chúng thuộc đối tượng của feature sở hữu, không sửa hồ sơ người cao tuổi. *(Nguồn: 6.8, BF-01 "đóng hồ sơ"; feature 001 FR-049)*
- **FR-073**: Nhắc việc, thông báo định kỳ và bản tin cho người thân về người đã qua đời MUST dừng từ thời điểm qua đời, trừ thông báo liên quan tới danh sách việc sau qua đời gửi người đại diện. *(Suy ra từ 6.8)*

#### J. Căn cứ tính phí lưu trú

- **FR-074**: Với nội trú, phí lưu trú định kỳ theo hợp đồng MUST được tính từ ngày bắt đầu hợp đồng, kể cả khi người cao tuổi Hoàn tất tiếp nhận muộn hơn ngày đó; các ngày từ ngày bắt đầu tới trước ngày Hoàn tất tiếp nhận MUST được đánh dấu "chưa vào ở" trên chi phí nháp để hành chính và gia đình đối chiếu. Với bán trú, phí buổi MUST tính cho các buổi có lịch đến từ ngày bắt đầu hợp đồng theo trạng thái có mặt và FR-060. Phí MUST dừng sau ngày kết thúc lưu trú (FR-066) hoặc từ thời điểm qua đời (FR-070). *(Nguồn: 6.3, 1.3, 15.7, BR-M11-08; Clarification 2026-09-25, đề xuất Q-22)*
- **FR-075**: Lệnh "Hoàn tất tiếp nhận" (feature 001) MUST NOT có thời điểm hiệu lực sớm hơn ngày bắt đầu của hợp đồng Hiệu lực; muốn vào ở sớm hơn thì phải có phụ lục đổi thời gian (mục F) được áp dụng trước. Điều kiện này MUST là một phần của điều kiện (b) trong FR-004. *(Clarification 2026-09-25, đề xuất Q-22)*
- **FR-075a**: Khi người cao tuổi bị Hủy tiếp nhận sau ngày bắt đầu của hợp đồng Hiệu lực, phí "chưa vào ở" từ ngày bắt đầu hợp đồng tới hết ngày Hủy tiếp nhận MUST được giữ nguyên; hợp đồng chuyển Chấm dứt (FR-027) và phí MUST dừng sau ngày Hủy tiếp nhận. Hệ thống MUST NOT tự hủy hay giảm các khoản này; miễn giảm MUST được thực hiện bằng khoản điều chỉnh có lý do và được duyệt theo feature 010. Hủy tiếp nhận trước hoặc đúng ngày bắt đầu hợp đồng thì không phát sinh phí lưu trú. *(Nguồn: FR-074, 15.6; Clarification 2026-09-25, đề xuất Q-27)*
- **FR-076**: Khi người cao tuổi nội trú chưa Hoàn tất tiếp nhận sau ngày bắt đầu hợp đồng, hệ thống MUST cảnh báo hành chính mỗi ngày trên danh sách điều kiện tiếp nhận rằng phí đang được tính cho ngày chưa vào ở. *(Suy ra từ FR-074)*

### Key Entities *(include if feature involves data)*

- **Lượt đăng ký tiếp nhận** – nhóm 2: người cao tuổi, ngày đăng ký, người liên hệ, nhu cầu, loại lưu trú mong muốn, mức chăm sóc dự kiến, ngày mong muốn vào, trạng thái.
- **Hồ sơ chờ (HO_SO_CHO)** – nhóm 2: lượt đăng ký, ngày đăng ký, loại lưu trú, mức chăm sóc, tình huống đặc biệt, điểm ưu tiên hiện hành, giường đang giữ, hạn giữ chỗ, mốc cập nhật gần nhất, trạng thái.
- **Lịch sử điểm ưu tiên** – nhóm 3: hồ sơ chờ, điểm cũ, điểm mới, thành phần, nguyên nhân, thời điểm; điều chỉnh thủ công kèm người đề nghị, người duyệt, lý do.
- **Hợp đồng (HOP_DONG)** – nhóm 2: số hợp đồng, người cao tuổi, loại lưu trú, ngày bắt đầu, ngày kết thúc, mức chăm sóc, dịch vụ đăng ký, phòng/giường hoặc loại phòng, lịch đến (bán trú), người đại diện ký, chính sách chi phí, đơn giá đã ghi khi ký của từng khoản (FR-036), khoản đặt cọc, chính sách giữ giường và dòng ghi đè chính sách vắng, điều kiện chấm dứt, ngày ký, bản scan, tham chiếu yêu cầu duyệt điều khoản khác chuẩn (FR-025a), dấu "quá hạn hợp đồng" (FR-029), trạng thái, lý do chấm dứt.
- **Phụ lục hợp đồng (PHU_LUC_HOP_DONG)** – nhóm 2: hợp đồng gốc, yêu cầu thay đổi, nội dung trước/sau, ngày hiệu lực, trạng thái (Chờ hiệu lực / Đã áp dụng / Không áp dụng).
- **Đặt cọc** – nhóm 2 (lần xác nhận nhóm 3): hợp đồng, khoản cần đặt cọc, trạng thái, thời điểm xác nhận, người xác nhận, nguồn xác nhận, bằng chứng.
- **Dịch vụ (DICH_VU)** – nhóm 1: loại, tên, đơn vị tính, có trong gói, trạng thái.
- **Phiên bản đơn giá (PHIEN_BAN_DON_GIA)** – nhóm 1: dịch vụ/phí, đơn giá, hiệu lực từ, hiệu lực đến.
- **Yêu cầu thay đổi lưu trú** – nhóm 2 (YEU_CAU_PHE_DUYET, feature 000): hợp đồng, loại thay đổi, giá trị trước/sau, ảnh hưởng chi phí, ngày hiệu lực, lý do, người yêu cầu, người duyệt, trạng thái.
- **Lượt vắng (LUOT_VANG)** – nhóm 2, bất biến khi đóng: người cao tuổi, loại vắng, rời lúc, dự kiến về (và lịch sử gia hạn), về lúc, người đón, người bàn giao, tình trạng giường, nguồn chính sách, kết quả theo ngày (số thứ tự ngày, hệ số, giữ giường, cờ "chờ quyết định"/"không có dòng chính sách").
- **Quyết định giữ giường khi vắng** – nhóm 2 (YEU_CAU_PHE_DUYET): lượt vắng, ngày ngưỡng, lựa chọn, hệ số, ngày xem xét lại, người quyết định.
- **Hồ sơ kết thúc lưu trú** – nhóm 2: người cao tuổi, trường hợp, ngày kết thúc dự kiến/thực tế, lý do, nơi chuyển đến, danh sách điều kiện và trạng thái, ngoại lệ đã duyệt, bàn giao người cao tuổi, trạng thái.
- **Bản ghi qua đời** – nhóm 3: người cao tuổi, thời điểm, địa điểm, người phát hiện, người xác nhận, nguyên nhân, bằng chứng.
- **Danh sách việc sau qua đời** – nhóm 2: bản ghi qua đời, các mục và trạng thái, thời điểm đóng hồ sơ lưu trú.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 0 trường hợp một người cao tuổi có hai hợp đồng Hiệu lực chồng thời gian, và 0 thao tác sửa thành công trên hợp đồng Hiệu lực hoặc phụ lục, trong mọi kịch bản kiểm thử.
- **SC-002**: Với bộ kiểm thử gồm hợp đồng có tới 5 phụ lục, 100% câu hỏi "nội dung hợp đồng tại ngày D" trả đúng giá trị tính tay theo BR-M02-09, và 100% chi phí trước ngày hiệu lực của phụ lục dùng giá trị cũ.
- **SC-003**: 100% lần giường chuyển Trống (không có phân bổ tương lai) tạo đề xuất hồ sơ chờ cho hành chính trong vòng 5 phút; thứ tự đề xuất khớp 100% với thứ tự tính tay theo điểm, ngày đăng ký.
- **SC-004**: 100% hồ sơ chờ có điểm ưu tiên được tính lại mỗi ngày trong đợt kiểm thử 30 ngày với đồng hồ giả lập; 100% điều chỉnh điểm thủ công có người duyệt và lý do; 0 điều chỉnh có hiệu lực trước khi được duyệt.
- **SC-005**: 100% giường giữ chỗ cho hồ sơ chờ quá hạn CFG-M02-02 được trả về Trống và đề xuất người tiếp theo trong đợt kiểm thử.
- **SC-006**: Với bảng chính sách mặc định và bộ 20 lượt vắng kiểm thử phủ mọi dòng của bảng (kể cả ghi đè theo hợp đồng và loại vắng không có dòng), 100% ngày vắng có hệ số phí và trạng thái giữ giường khớp bảng; 100% lượt vắng vượt ngưỡng giữ giường tạo yêu cầu quyết định cho Quản lý viện.
- **SC-007**: 0 lệnh "Kết thúc lưu trú" thành công khi còn đồ gửi, thuốc gửi, cảnh báo/sự cố mở hoặc chưa bàn giao người cao tuổi mà không có ngoại lệ được duyệt.
- **SC-008**: 100% lần ghi nhận qua đời chấm dứt hợp đồng, giải phóng giường và dừng sinh chi phí ngay trong cùng lệnh; 0 chi phí tự sinh, công việc hay thông báo định kỳ nào phát sinh cho người đã qua đời sau thời điểm qua đời; 100% danh sách việc sau qua đời được nhắc cho tới khi đóng hồ sơ.
- **SC-009**: Hành chính xác định được người cao tuổi còn thiếu điều kiện tiếp nhận nào trong không quá 30 giây, không cần mở từng module khác.
- **SC-010**: Hành chính hoàn tất một lệnh "Cho tạm vắng" (gồm chọn người đón, người bàn giao, dự kiến trở lại) trong không quá 2 phút.

## Assumptions

- Số feature `004` theo bản đồ feature ở mục 4.2 `docs/phan-tich-yeu-cau.md` (UC-09 → UC-18) và trùng số tuần tự kế tiếp.
- Quy tắc dùng chung (nhóm dữ liệu, đính chính, vòng đời yêu cầu phê duyệt, nhật ký, tham số) theo feature 000; tập trạng thái người cao tuổi và điều kiện chặn theo feature 001; giường và phân bổ theo feature 003. Spec này không lặp lại các quy tắc đó.
- "Lượt đăng ký tiếp nhận" là một thực thể gắn với hồ sơ người cao tuổi Đang tiếp nhận (hồ sơ do feature 001 tạo trước); ERD 3.2 chưa có thực thể này (xem điểm báo lại 1).
- Danh sách chờ và đề xuất tự động (BR-M02-01) áp dụng cho nội trú; hồ sơ chờ bán trú được xét thủ công theo sức chứa khu nghỉ.
- Hồ sơ chờ chuyển Đã tiếp nhận khi người đó được phân bổ giường (không đợi Hoàn tất tiếp nhận), vì từ lúc đó người đó không còn cần giường trống.
- "Từ chối" ở 6.2 được hiểu là gia đình không tiếp tục chờ (trạng thái cuối); từ chối một giường cụ thể nhưng vẫn chờ thì hồ sơ về Đang chờ.
- Hợp đồng có thêm trạng thái Đã hủy (từ Nháp hoặc Chờ ký) và lệnh "Trả về nháp" (Chờ ký → Nháp), không có ở 6.3.
- Kết thúc = lưu trú kết thúc tại/sau ngày kết thúc hợp đồng; Chấm dứt = kết thúc trước hạn, qua đời, hoặc hủy tiếp nhận sau khi hợp đồng Hiệu lực.
- Gia hạn hợp đồng dài hạn hoặc ngắn ngày được thực hiện bằng yêu cầu thay đổi "đổi thời gian" thành phụ lục; tái ký một hợp đồng mới là cách thay thế, chịu ràng buộc không chồng thời gian.
- Mọi yêu cầu thay đổi lưu trú đều cần Quản lý viện duyệt (FR-043), không chỉ thay đổi ảnh hưởng chi phí hoặc loại lưu trú.
- Hệ thống không theo dõi hoàn cọc, khấu trừ cọc hay số tiền thực nhận; việc này thuộc kế toán.
- Hành chính soạn dòng ghi đè chính sách vắng và giá riêng trong hợp đồng Nháp; các điều khoản khác chuẩn này cần Quản lý viện duyệt trước khi gửi ký (FR-025a).
- Danh mục dịch vụ, đơn giá do Quản lý viện cấu hình (quyền C), vì Permission Matrix không có dòng riêng.
- Với loại vắng "Đi khám trong ngày" và "Lý do khác" không có dòng trong bảng ví dụ, hệ thống áp 100% và giữ giường (FR-053) cho tới khi cơ sở cấu hình.
- Ngày vắng tính theo ngày dương lịch, múi giờ Asia/Ho_Chi_Minh (DBR-25); ngày rời và ngày về đều là ngày vắng.
- Ghi nhận qua đời không bị chặn bởi điều kiện nào ngoài "có người xác nhận" (5.6); các việc còn lại được theo dõi bằng danh sách việc sau qua đời.
- "Bàn giao người cao tuổi" (6.8) là điều kiện bắt buộc không có ngoại lệ, vì liên quan an toàn người cao tuổi.
- Tham số mới đề xuất: CFG-M02-09 (chu kỳ nhắc danh sách việc sau qua đời, mặc định 1 ngày); CFG-M02-10 (số ngày quá hạn hợp đồng trước khi báo Quản lý viện, mặc định 7 ngày).

## Điểm cần báo lại về tài liệu nguồn

Ngày 2026-09-25, các điểm đã chốt đã được đưa vào `docs/nghiep-vu.md` và `docs/phan-tich-yeu-cau.md` theo yêu cầu. Danh sách dưới đây ghi nơi đã phản ánh và các điểm còn mở.

**Đã phản ánh vào tài liệu nguồn**

1. Thực thể lượt đăng ký tiếp nhận, đặt cọc, hồ sơ kết thúc lưu trú, bản ghi qua đời: ERD miền B và mô tả thực thể ở 3.2.
2. Nghĩa của "Từ chối" và "Hủy chờ", hồ sơ chờ Đã tiếp nhận khi được phân bổ giường (Q-29); BR-M02-01 không đề xuất khi giường có phân bổ tương lai hoặc phòng cách ly: 6.2, BR-M02-01.
3. Vòng đời hợp đồng (Trả về nháp, Đã hủy, Kết thúc khác Chấm dứt): 6.3.
4. Hợp đồng quá hạn vẫn Hiệu lực, gắn dấu, báo Quản lý viện sau CFG-M02-10 (Q-20): BR-M02-05, Phụ lục 25.
5. Chốt chi phí kỳ cuối trước lệnh Kết thúc lưu trú; quá ngày kết thúc dự kiến thì sinh bù (Q-21, Q-26): BR-M11-08, 15.7, 6.8, dòng "→ Kết thúc lưu trú" của 5.6.
6. Mốc tính phí từ ngày bắt đầu hợp đồng (Q-22), giữ phí khi Hủy tiếp nhận muộn (Q-27), phải có giường từ ngày bắt đầu (Q-28): 6.3, dòng "Đang tiếp nhận → Đang lưu trú" của 5.6.
7. Quản lý viện duyệt điều khoản hợp đồng khác chuẩn (Q-23): 6.3, chú thích ⁶ của Permission Matrix 4.4; chỉ người đại diện xem hợp đồng: chú thích ⁷.
8. Mọi thay đổi lưu trú đều cần duyệt: 6.6.
9. Quy tắc tra bảng phí vắng (ngày vắng, loại vắng không có dòng, ngưỡng giữ giường) và đếm lại ngày khi nằm viện (Q-24): 6.7.
10. Hệ số phí khi giữ tiếp, khi giải phóng và khi chờ quyết định: BR-M02-07.
11. Dịch vụ và đơn giá do Quản lý viện cấu hình: dòng "Dịch vụ, đơn giá" của Permission Matrix 4.4.
12. Danh sách việc sau qua đời là ngoại lệ của BR-M01-05: BR-M01-05 (ngoại lệ 3) và 6.8.
13. Hợp đồng giữ đơn giá lúc ký (Q-25): 6.3, BR-M11-02, DBR-16, quan hệ HOP_DONG – CHI_PHI trong ERD miền E.
14. Mã Q-20 → Q-29 ở mục 24.2; tham số CFG-M02-09, CFG-M02-10 ở Phụ lục 25.

**Còn mở**

1. **Q-08 (giá trị bảng chính sách phí khi vắng)** vẫn mở ở mục 24.1; spec dùng bảng ví dụ 6.7 làm mặc định (FR-050). Quản lý viện cần chốt giá trị, gồm dòng cho "Đi khám trong ngày", "Lý do khác" và thời hạn giữ giường của dòng "Về nhà, đi chơi".
2. **Mã yêu cầu phê duyệt riêng của feature này** ("Duyệt điều khoản hợp đồng", "Quyết định giữ giường khi vắng", đề nghị điều chỉnh điểm, ngoại lệ kết thúc) chưa được liệt kê tập trung trong tài liệu nguồn; có thể bổ sung một bảng loại yêu cầu phê duyệt ở 6.6 khi các module khác cũng có đủ loại yêu cầu.
