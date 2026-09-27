# Feature Specification: Báo cáo và dashboard

**Feature Branch**: `016-reports-dashboard`

**Created**: 2026-09-27

**Status**: Draft

**Input**: User description: "Xây dựng báo cáo và dashboard theo docs/nghiep-vu.md Module 14 (mục 18): báo cáo người cao tuổi, chăm sóc, sức khỏe, thuốc, chi phí phát sinh và chất lượng; dashboard theo vai trò với số liệu trong ngày; mọi số liệu tuân theo phạm vi dữ liệu của người xem. Chỉ đọc dữ liệu, không tạo dữ liệu nghiệp vụ."

## Clarifications

### Session 2026-09-27

- Q: Khi báo cáo một khoảng thời gian đã qua, "Tầng được giao" và "Phạm vi phân công" tính thế nào? → A: Tách theo vai trò: trưởng tầng theo tầng/khu đang được giao tại lúc xem, thấy mọi bản ghi gắn tầng/khu đó kể cả trước khi được giao; điều dưỡng chỉ thấy bản ghi thuộc phân công của mình trong các ca mình đã làm (giờ ca ± CFG-M15-07) (đề xuất Q-192).
- Q: Có cho xuất báo cáo ra file không? → A: Có; mọi người có quyền xem một báo cáo được xuất báo cáo đó, đúng phạm vi và giới hạn trường, mỗi lần xuất ghi lại (đề xuất Q-193).
- Q: Có thêm nhóm báo cáo ngoài 18.1 → 18.5 mà các spec khác đã hứa cung cấp dữ liệu không? → A: Thêm một nhóm "nhân sự và ca trực" từ dữ liệu của feature 008 và 015; số liệu thăm, phản hồi (012), đồ gửi (013), thông báo (009) để giai đoạn sau (đề xuất Q-194).
- Q: Danh sách chi tiết của trưởng tầng hiện gì với bản ghi của người cao tuổi không còn trong phạm vi hiện hành? → A: Họ tên, phòng/giường tại thời điểm phát sinh và nội dung của chính bản ghi (loại, trạng thái, thời điểm, người thực hiện, kết quả), vẫn áp giới hạn trường; không mở được bản ghi đầy đủ hay hồ sơ hiện tại (FR-012, Q-196).
- Q: File xuất có được chứa danh sách chi tiết có họ tên người cao tuổi không? → A: Chỉ Quản lý viện xuất được danh sách có định danh người cao tuổi; vai trò khác chỉ xuất số liệu tổng hợp, không có dòng hay cột định danh (FR-061, FR-062; gộp vào Q-193).
- Q: Một lần xem hoặc xuất báo cáo được chọn khoảng thời gian dài tối đa bao nhiêu? → A: Tối đa CFG-M14-01 (tham số mới, mặc định \[12 tháng\]); dài hơn thì từ chối, người xem xem từng đoạn (FR-060, Q-203).
- Q: Hành chính có thấy số liệu sự cố thuộc loại không liên quan sức khỏe không? → A: Có, chỉ với loại sự cố mang dấu "không thuộc sức khỏe" trên danh mục loại sự cố (mặc định đồ gửi thất lạc, đồ gửi hư hỏng, cơ sở vật chất); chỉ số lượng và danh sách không có mô tả, diễn biến (FR-010, FR-042a, Q-202).
- Q (checklist permissions): Người phụ trách ca thấy gì trên dashboard, báo cáo? → A: Trong giờ ca ± CFG-M15-07, thấy cả tầng/khu của ca với các nhóm chỉ tiêu của Điều dưỡng; báo cáo khoảng đã qua gồm cả tầng/khu trong các ca mình phụ trách; khi tầng tạm chưa có trưởng tầng (Q-90) không được thêm nhóm chỉ dành cho trưởng tầng (FR-012, đề xuất Q-195).
- Q (checklist permissions): Người xem có quyền "X" toàn viện ở dòng nguồn có mở được bản ghi của người ngoài phạm vi từ danh sách chi tiết không? → A: Có, khi quyền hiện hành ở feature sở hữu cho phép (FR-009); ngoài trường hợp đó giữ giới hạn của FR-012 (đề xuất Q-196).
- Q (checklist permissions): File do vai trò khác Quản lý viện xuất có được chứa tên nhân viên không? → A: Được, trong phạm vi quyền xem; riêng số giờ làm theo từng nhân viên chỉ Quản lý viện xuất (FR-061, FR-073, đề xuất Q-197).
- Q (checklist permissions): Quản lý viện có phải nêu mục đích khi xuất danh sách có định danh người cao tuổi không? → A: Có, bắt buộc; mục đích lưu vào bản ghi lần xuất (FR-061, FR-062, đề xuất Q-198).
- Q (checklist consistency): Có đếm leo thang của sự cố không? → A: Có; đếm số lần leo thang và thời gian tạo → tiếp nhận của sự cố theo mức, tính riêng với cảnh báo (FR-041, FR-042, đề xuất Q-199).
- Q (checklist consistency): Báo cáo tỷ lệ phục vụ của ca đã qua lấy lần tính nào? → A: Giá trị của lần tính cuối trước khi ca kết thúc, kèm dấu "từng không đạt" nếu có lần tính nào trong ca không đạt; "số ca không đạt" đếm theo dấu này (FR-070, đề xuất Q-200).
- Q (checklist consistency): Bản ghi ngoại tuyến đang "chờ xem lại" có được đếm không? → A: Có, theo giá trị đang ghi; chỉ tiêu có bản ghi loại này nêu kèm "N bản ghi chờ xem lại"; đính chính sau xem lại được phản ánh theo FR-007 (đề xuất Q-201).

## Phạm vi

**Trong phạm vi** (Module 14, mục 18; UC-67):

1. Báo cáo người cao tuổi: số lượng, loại lưu trú, mức chăm sóc, trạng thái, phân bố phòng/giường, danh sách chờ (18.1).
2. Báo cáo chăm sóc: công việc theo trạng thái, nhân viên, ca, tầng; ghi nhận muộn; vệ sinh; phiếu bữa ăn; hoạt động và tỷ lệ tham gia; nguy cơ cô lập (18.2).
3. Báo cáo chất lượng: tỷ lệ Đạt của kiểm tra chất lượng ngẫu nhiên theo nhân viên và theo tầng (18.2, BR-M04-23).
4. Báo cáo sức khỏe và thuốc: chỉ số, cảnh báo, sự cố, trường hợp cần theo dõi, tình trạng dùng thuốc, yêu cầu đánh giá lại và đối chiếu thuốc quá hạn (18.3).
5. Báo cáo chi phí phát sinh của người cao tuổi (18.4).
6. Dashboard theo vai trò với số liệu trong ngày và ca hiện tại (18.5).
7. Áp phạm vi dữ liệu và giới hạn trường của người xem cho mọi số liệu, danh sách và lần mở chi tiết (18.5 "(Bổ sung)", 19.3, BR-M15-01).
8. Mở từ một số liệu xuống danh sách bản ghi tạo nên số liệu đó (chỉ xem).
9. Báo cáo nhân sự và ca trực: tỷ lệ phục vụ theo ca, ca thiếu phủ, mục "thiếu ca", yêu cầu đổi ca, nghỉ đột xuất, số giờ làm trong tháng (18.2 "theo ca", 18.5; feature 008 FR-037, feature 015 FR-042; Q-194).
10. Xuất báo cáo ra file theo đúng phạm vi và giới hạn trường, có ghi lại lần xuất (Q-193).

**Ngoài phạm vi**:

- Mọi dữ liệu nghiệp vụ và mọi quy tắc sinh ra chúng: feature sở hữu (000 → 015). Spec này chỉ đọc, không tính lại quy tắc nghiệp vụ (ví dụ không tự xác định công việc Quá hạn, không tự sinh cảnh báo).
- Gửi thông báo, nhắc hạn, leo thang: feature 009 và feature nguồn. Dashboard chỉ hiển thị.
- Phạm vi dữ liệu, giới hạn trường, quyền ba lớp: feature 002. Spec này **dùng** kết quả tính quyền, không định nghĩa lại.
- Báo cáo tài chính của viện, thu tiền, công nợ: hệ thống kế toán (1.2, 18.4). File chi phí cho kế toán: feature 010 (UC-64).
- Báo cáo sinh lịch ca của từng lần sinh: feature 015 (FR-012 của 015). Bàn giao ca: feature 008.
- Số liệu thăm, phản hồi (feature 012), đồ gửi thất lạc, hư hỏng (feature 013), thống kê thông báo (feature 009): giai đoạn sau (Q-194).
- Số suất theo bữa, theo chế độ ăn (feature 011 bảng giao tiếp): mục 18 không nêu; giai đoạn sau. Spec này chỉ dùng số phiếu giao trễ, có sai lệch của feature 011.
- Cổng người thân: người thân không có quyền ở dòng "Dashboard, báo cáo" (4.4). Số liệu cho người thân thuộc feature 012.
- Dashboard trên màn hình dùng chung (ví dụ quầy điều dưỡng), thời gian hết phiên đăng nhập, khóa màn hình: feature 002. Spec này không đặt thêm yêu cầu.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Quản lý viện và trưởng tầng nắm tình hình trong ngày trên dashboard (Priority: P1)

Khi mở dashboard, Quản lý viện thấy tình hình toàn viện trong ngày: số người đang lưu trú, tình trạng giường, công việc hôm nay và quá hạn, cảnh báo và sự cố đang mở, hoạt động hôm nay, tình trạng chốt chi phí, giấy phép sắp hết hạn, tỷ lệ phục vụ theo khu trong ca hiện tại, khu đang khoanh vùng, bàn giao chưa xác nhận. Trưởng tầng thấy cùng các chỉ tiêu nhưng chỉ cho tầng mình được giao và không có chi phí. Mỗi con số mở được xuống danh sách bản ghi.

**Why this priority**: Đây là nhu cầu dùng hằng ngày của hai actor chính của UC-67 và là mục 18.5. Dashboard giúp phát hiện việc tồn, cảnh báo chưa tiếp nhận ngay trong ca.

**Independent Test**: Dựng dữ liệu một ngày cho 2 tầng: tầng 1 có 40 công việc hôm nay (3 Quá hạn, 1 Bắt buộc), 2 cảnh báo đang mở (1 mang dấu "đã leo thang tối đa"), 1 sự cố Khẩn cấp; tầng 2 có 30 công việc, 1 khu đang khoanh vùng. Mở dashboard bằng Quản lý viện và bằng trưởng tầng tầng 1. So từng con số với số đếm tay.

**Acceptance Scenarios**:

1. **Given** dữ liệu như trên lúc 10:00, **When** Quản lý viện mở dashboard, **Then** "Công việc hôm nay" hiện 70, chia theo trạng thái; "Công việc quá hạn" hiện 3, trong đó 1 Bắt buộc; "Cảnh báo đang mở" hiện 2, cảnh báo "đã leo thang tối đa" được đặt nổi bật (feature 007 FR-034); "Khu đang khoanh vùng" nêu tầng 2 (18.5).
2. **Given** trưởng tầng T được giao tầng 1, **When** T mở dashboard, **Then** mọi con số chỉ gồm tầng 1 (40 công việc, 3 quá hạn, 2 cảnh báo, 1 sự cố); không có chỉ tiêu chi phí, không có tổng toàn viện, không có con số nào cho biết tầng 2 có bao nhiêu bản ghi (18.5 "(Bổ sung)", feature 002 FR-035).
3. **Given** Quản lý viện đang xem dashboard, **When** một công việc ở tầng 1 chuyển Quá hạn lúc 10:05, **Then** con số "Công việc quá hạn" tăng lên 4 trong thời hạn ở SC-002 mà không cần thao tác gì khác, và dashboard ghi thời điểm cập nhật số liệu.
4. **Given** ca ngày tầng 1 có tỷ lệ phục vụ tính theo feature 008 không đạt ngưỡng CFG-M09-01, **When** Quản lý viện mở dashboard, **Then** chỉ tiêu "Tỷ lệ phục vụ ca hiện tại" của tầng 1 hiện giá trị do feature 008 tính và nhãn "không đạt"; spec này không tính lại (BR-M09-02, Q-79).
5. **Given** con số "Công việc quá hạn" là 3, **When** trưởng tầng T chọn con số đó, **Then** T thấy danh sách đúng 3 công việc (người cao tuổi, công việc, mức quan trọng, người được giao, thời điểm dự kiến) và mở được từng công việc ở chế độ chỉ xem của feature 005.
6. **Given** giấy phép hoạt động của cơ sở hết hạn sau 45 ngày và CFG-M06-02 là \[60 ngày\], **When** Quản lý viện mở dashboard, **Then** mục "Giấy phép sắp hết hạn" nêu giấy phép đó và số ngày còn lại; **When** trưởng tầng mở dashboard, **Then** không thấy giấy phép của cơ sở, chỉ thấy giấy phép hành nghề, chứng chỉ đào tạo sắp hết hạn của nhân viên có phân công tại tầng mình (CFG-M06-02, CFG-M09-03).

---

### User Story 2 - Mọi số liệu tuân theo phạm vi dữ liệu và giới hạn trường của người xem (Priority: P1)

Cùng một báo cáo, mỗi vai trò chỉ thấy số liệu dựng từ dữ liệu mình được xem. Quản lý viện xem toàn viện. Bác sĩ và Hành chính xem toàn viện nhưng Hành chính không thấy chỉ tiêu sức khỏe và không thấy tên thuốc. Trưởng tầng và Điều dưỡng chỉ thấy phạm vi được phân công. Nhân viên chăm sóc, dinh dưỡng viên, nhân viên bếp, nhân viên vệ sinh và người thân không có dashboard, báo cáo.

**Why this priority**: Báo cáo tổng hợp là kênh dễ rò rỉ dữ liệu nhất vì gom nhiều bản ghi. 18.5 và feature 002 FR-035 đòi hỏi điều này cho mọi số liệu. Sai phạm vi là lỗi bảo mật, không phải lỗi hiển thị.

**Independent Test**: Dùng bộ tài khoản kiểm thử quyền gồm 11 tài khoản: Quản lý viện; trưởng tầng tầng 1; trưởng tầng giao tạm tầng 3 có thời hạn; trưởng tầng đã thôi giao tầng 4; bác sĩ; điều dưỡng có ca tầng 1 đang diễn ra; điều dưỡng giữ nhiệm vụ Người phụ trách ca tầng 1; hành chính; nhân viên chăm sóc; tài khoản hành chính kiêm điều dưỡng tầng 2; tài khoản bác sĩ kiêm trưởng tầng tầng 2. Mở cùng báo cáo sức khỏe, chăm sóc, chi phí, nhân sự tháng 10 và dashboard. So danh sách chỉ tiêu và từng con số với FR-010, FR-012, FR-015.

**Acceptance Scenarios**:

1. **Given** hành chính H, **When** H mở dashboard, **Then** H thấy số người đang lưu trú, tình trạng giường, tạm vắng, danh sách chờ, khu đang khoanh vùng, tình trạng chốt chi phí và sự cố thuộc loại mang dấu "không thuộc sức khỏe" (ví dụ đồ gửi thất lạc); H không thấy chỉ tiêu cảnh báo, sự cố loại khác, chỉ số, thuốc, công việc chăm sóc (feature 002 FR-035, User Story 1 kịch bản 12 của 002).
2. **Given** báo cáo chi phí tháng 10 có khoản loại Thuốc, **When** Quản lý viện mở báo cáo "theo thuốc", **Then** thấy tên thuốc; **When** hành chính H mở cùng báo cáo, **Then** chỉ thấy mã vật phẩm, số lượng, đơn giá, thành tiền (Q-133, feature 002 FR-022).
3. **Given** bác sĩ B không có phân công theo tầng, **When** B mở báo cáo sức khỏe tháng 10, **Then** B thấy số liệu toàn viện (chú thích ¹¹ của 4.4); **When** B mở báo cáo chi phí, **Then** hệ thống từ chối vì bác sĩ không có quyền ở dòng "Chi phí, khoản điều chỉnh".
4. **Given** nhân viên chăm sóc C, **When** C tìm mở dashboard hoặc bất kỳ báo cáo nào, **Then** hệ thống từ chối (4.4 dòng "Dashboard, báo cáo", cột CS "—").
5. **Given** điều dưỡng D có ca tầng 1 đang diễn ra và được phân công 8 người cao tuổi, **When** D mở dashboard, **Then** mọi con số chỉ gồm 8 người đó, công việc chung và liều chung của tầng 1 trong ca (Q-61, Q-85); **When** D mở dashboard lúc ngoài giờ ca cộng CFG-M15-07 (mặc định \[2 giờ\]) và không có phân công nào khác, **Then** dashboard báo "hiện không có phạm vi phân công" và không hiện con số nào (BR-M15-02, Q-14).
6. **Given** tài khoản K có vai trò Hành chính và Điều dưỡng, đang có ca tầng 2, **When** K mở báo cáo sức khỏe, **Then** chỉ tiêu sức khỏe chỉ gồm người cao tuổi thuộc phân công điều dưỡng ở tầng 2; **When** K mở báo cáo người cao tuổi, **Then** thấy toàn viện theo quyền Hành chính (feature 002 FR-034).
7. **Given** trưởng tầng T được giao tầng 1, **When** T xem báo cáo chăm sóc tháng 10 và lọc theo "toàn viện" hoặc "tầng 2", **Then** hệ thống không cho chọn các giá trị lọc ngoài phạm vi; mọi tổng chỉ gồm tầng 1.
8. **Given** không xác định được phạm vi của điều dưỡng D vì dữ liệu phân công bị lỗi, **When** D mở dashboard, **Then** hệ thống từ chối và báo Quản lý viện theo feature 002 FR-041b.
9. **Given** tháng 10 có 3 sự cố "đồ gửi thất lạc" (loại mang dấu "không thuộc sức khỏe") và 2 sự cố ngã, **When** hành chính H xem báo cáo sự cố, **Then** H thấy 3 sự cố đồ gửi theo mức, trạng thái, và danh sách có người cao tuổi, loại, mức, trạng thái, thời điểm nhưng không có mô tả, diễn biến; tổng không nhắc tới 2 sự cố ngã (FR-010, FR-042a).
10. **Given** điều dưỡng P giữ nhiệm vụ Người phụ trách ca ngày tầng 1 (07:00–19:00) và được phân công 6 người, **When** P mở dashboard lúc 10:00, **Then** mọi chỉ tiêu nhóm Điều dưỡng gồm cả tầng 1 (không chỉ 6 người); P không thấy nhóm hoạt động, kiểm tra chất lượng, nhân sự, chỉ tiêu theo nhân viên; **When** P xem báo cáo công việc tháng 10 vào ngày 05/11, **Then** số liệu gồm cả tầng 1 trong giờ ca ± CFG-M15-07 của các ca P phụ trách và chỉ người được phân công ở các ca P không phụ trách (FR-012, Q-195).
11. **Given** tầng 3 tạm chưa có trưởng tầng được giao và P là Người phụ trách ca đang diễn ra (Q-90), **When** P mở dashboard, **Then** P thấy tầng 3 như kịch bản 10 và không thấy thêm nhóm nào dành cho trưởng tầng (FR-012).
12. **Given** các ô "—" của FR-010, **When** từng vai trò mở nhóm chỉ tiêu tương ứng, **Then** hệ thống từ chối trong mọi trường hợp sau: trưởng tầng với danh sách chờ, chi phí, phiếu đối chiếu thuốc; bác sĩ với phòng/giường, hoạt động, vệ sinh, phiếu bữa ăn, tỷ lệ phục vụ, nhân sự; điều dưỡng với hoạt động, vệ sinh, phiếu bữa ăn, tỷ lệ phục vụ, chỉ tiêu theo nhân viên; hành chính với công việc, chỉ số, cảnh báo, liều, sự cố không mang dấu (FR-010).
13. **Given** Quản lý viện thu hẹp quyền "Dashboard, báo cáo" của tài khoản trưởng tầng T chỉ còn dashboard (feature 002 FR-025), **When** T mở báo cáo hoặc xuất file, **Then** hệ thống từ chối; dashboard vẫn mở được (FR-010a).

---

### User Story 3 - Báo cáo người cao tuổi (Priority: P2)

Quản lý viện, hành chính và các vai trò có quyền xem hồ sơ trong phạm vi xem số người cao tuổi theo trạng thái, loại lưu trú, mức chăm sóc tại một ngày; biến động trong một khoảng (tiếp nhận, kết thúc lưu trú, qua đời, hủy tiếp nhận); phân bố theo tầng, phòng và tình trạng giường; và danh sách chờ.

**Why this priority**: Là số liệu nền cho mọi báo cáo khác và cho quyết định nhận người mới. Không cần các báo cáo khác để dùng được.

**Independent Test**: Dựng 120 hồ sơ với trạng thái, loại lưu trú, mức chăm sóc khác nhau, trong đó có 5 người đổi mức chăm sóc và 3 người chuyển tầng trong tháng 10. Xem báo cáo tại 01/10 và 31/10, và biến động tháng 10. So với số đếm tay.

**Acceptance Scenarios**:

1. **Given** A đổi mức chăm sóc từ "Cơ bản" sang "Thường xuyên" có hiệu lực 15/10, **When** xem báo cáo tại ngày 10/10, **Then** A được đếm ở "Cơ bản"; **When** xem tại 20/10, **Then** A được đếm ở "Thường xuyên" (FR-022).
2. **Given** trong tháng 10 có 4 người Hoàn tất tiếp nhận, 2 người Kết thúc lưu trú, 1 người Qua đời, 1 hồ sơ Hủy tiếp nhận, **When** xem biến động tháng 10, **Then** mỗi loại có đúng số đó; người ở trạng thái cuối vẫn được đếm trong các ngày trước khi chuyển (5.5).
3. **Given** tầng 1 có 30 giường: 23 Đang sử dụng, 1 Đang giữ chỗ (hồ sơ chờ), 1 Đang giữ chỗ (người vắng), 1 Chờ vệ sinh, 1 Đang bảo trì, 1 Không sử dụng, 2 Trống, **When** trưởng tầng tầng 1 xem phân bố giường hiện tại, **Then** thấy đúng từng số theo bảng trạng thái giường của feature 003, "tổng giường có thể dùng" là 29, và số khớp dashboard tình trạng giường tại cùng thời điểm (feature 003 SC-008).
4. **Given** danh sách chờ có 12 hồ sơ, **When** hành chính xem báo cáo danh sách chờ, **Then** thấy số hồ sơ theo trạng thái hồ sơ chờ, theo loại lưu trú, mức chăm sóc dự kiến và thời gian đã chờ; **When** trưởng tầng xem, **Then** không có phần danh sách chờ (4.4 dòng "Danh sách chờ": TT "—").
5. **Given** người bán trú có lịch ngày 14/10, **When** xem báo cáo ngày 14/10, **Then** số bán trú được chia theo trạng thái có mặt theo ngày (3.4) do feature 005 xác định.

---

### User Story 4 - Báo cáo chăm sóc và chất lượng (Priority: P2)

Quản lý viện và trưởng tầng xem kết quả chăm sóc trong một khoảng: công việc hoàn thành, chưa hoàn thành, quá hạn theo nhân viên, ca, tầng; tỷ lệ hoàn thành đúng hạn công việc Bắt buộc; số ghi nhận muộn; vệ sinh đúng hạn và thời gian giường Chờ vệ sinh; phiếu bữa ăn giao trễ hoặc có sai lệch; hoạt động, tỷ lệ tham gia, lượt "Không ghi nhận", chuyến đi quá giờ về, sự cố thiếu người; danh sách người có nguy cơ cô lập; tỷ lệ Đạt của kiểm tra chất lượng. Bác sĩ và điều dưỡng xem phần công việc chăm sóc (không có phần theo nhân viên).

**Why this priority**: Là công cụ giám sát chất lượng chăm sóc (18.2, BR-M04-23). Cần dữ liệu một thời gian mới có giá trị, nên sau dashboard.

**Independent Test**: Dựng dữ liệu tháng 10 cho tầng 1: 1.200 công việc (60 Bắt buộc, trong đó 54 Hoàn thành, 4 Hoàn thành trễ, 2 Không thực hiện), 15 ghi nhận muộn, 40 mục kiểm tra chất lượng (30 Đạt, 5 Không đạt, 3 Không kiểm tra được, 2 Quá hạn kiểm tra), 6 buổi hoạt động. So từng chỉ tiêu với tính tay.

**Acceptance Scenarios**:

1. **Given** dữ liệu trên, **When** trưởng tầng xem "Tỷ lệ hoàn thành đúng hạn công việc Bắt buộc", **Then** kết quả là 54 ÷ (54 + 4 + 2) = 90% (2 công việc Không thực hiện do nhân viên ghi); công việc Hủy, công việc chưa ở trạng thái cuối và công việc Không thực hiện do hệ thống đóng không vào mẫu số, loại cuối được nêu thành số riêng (FR-031).
2. **Given** 40 mục kiểm tra như trên, **When** xem tỷ lệ Đạt của tầng 1, **Then** kết quả là 30 ÷ (30 + 5) ≈ 85,7%; 3 mục "Không kiểm tra được" và 2 mục "Quá hạn kiểm tra" hiện thành hai số riêng, không vào mẫu số (feature 014 FR-065).
3. **Given** trưởng tầng T tự thực hiện 12 công việc trong tháng, **When** xem tỷ lệ Đạt theo nhân viên, **Then** dòng của T ghi "ngoài phạm vi kiểm tra", không hiện 0% hay ô trống như một kết quả (Q-168).
4. **Given** buổi hoạt động có 10 lượt Có mặt, 2 lượt Vắng lý do "từ chối", 1 lượt "Vắng – đang vắng mặt", 1 lượt "Không ghi nhận", **When** xem tỷ lệ tham gia, **Then** kết quả là 10 ÷ (10 + 2) ≈ 83,3%; lượt "Không ghi nhận" được đếm riêng theo hoạt động và tầng (Q-169, Q-173, feature 014 FR-070).
5. **Given** bác sĩ B, **When** B mở báo cáo chăm sóc, **Then** B thấy công việc theo trạng thái, ca, tầng; không thấy phần theo nhân viên, hoạt động, kiểm tra chất lượng, vệ sinh, phiếu bữa ăn (FR-010).
6. **Given** một kết quả công việc bị đính chính "Hủy ghi nhận" sau khi báo cáo tháng đã được xem lần đầu, **When** xem lại báo cáo, **Then** số liệu dùng giá trị hiện hành sau đính chính (feature 000 FR-030) và bản ghi gốc vẫn mở được từ danh sách chi tiết.

---

### User Story 5 - Báo cáo sức khỏe và thuốc (Priority: P2)

Quản lý viện, bác sĩ, trưởng tầng và điều dưỡng (trong phạm vi) xem: số lần đo và số lần vượt ngưỡng theo loại chỉ số; cảnh báo theo mức, loại, tầng; thời gian trung bình từ khi tạo cảnh báo đến khi tiếp nhận, theo mức; số lần leo thang; sự cố theo mức, loại, tầng; trường hợp cần theo dõi; tỷ lệ liều Bỏ lỡ, Từ chối theo người cao tuổi và theo ca; yêu cầu đánh giá lại quá hạn; phiếu đối chiếu thuốc quá hạn.

**Why this priority**: Phục vụ giám sát y tế và an toàn thuốc (18.3). Có thể làm độc lập với báo cáo chăm sóc.

**Independent Test**: Dựng tháng 10 với 50 cảnh báo (thời điểm tạo và tiếp nhận biết trước), 8 lần leo thang, 900 liều đã đến hạn (20 Bỏ lỡ, 15 Từ chối), 3 phiếu đối chiếu quá hạn. So với tính tay (feature 007 SC-013).

**Acceptance Scenarios**:

1. **Given** 10 cảnh báo mức Trung bình có thời gian từ tạo đến tiếp nhận lần lượt đã biết, **When** xem thời gian trung bình theo mức, **Then** kết quả bằng trung bình cộng của 10 khoảng; cảnh báo chưa được tiếp nhận không vào phép tính và được đếm riêng là "chưa tiếp nhận" (FR-041).
2. **Given** 900 liều có trạng thái cuối Đã dùng, Từ chối, Không thực hiện hoặc Bỏ lỡ, trong đó 20 Bỏ lỡ và 15 Từ chối, **When** xem tỷ lệ, **Then** tỷ lệ Bỏ lỡ là 20 ÷ 900 ≈ 2,2% và Từ chối là 15 ÷ 900 ≈ 1,7%; liều Đã hủy và liều Tạm dừng không vào mẫu số (FR-043).
3. **Given** trưởng tầng T, **When** T xem báo cáo sức khỏe tầng 1, **Then** T thấy tỷ lệ liều nhưng không thấy phần phiếu đối chiếu thuốc (4.4 dòng "Đối chiếu thuốc": TT "—").
4. **Given** hành chính H, **When** H mở báo cáo sức khỏe, **Then** hệ thống từ chối (feature 002 FR-035).
5. **Given** A đang có lịch theo dõi sau ngã và B đang theo dõi tiếp xúc Hiệu lực, **When** xem "trường hợp cần theo dõi" hôm nay, **Then** A và B có trong danh sách kèm loại theo dõi và ngày kết thúc dự kiến (feature 007 FR-081).

---

### User Story 6 - Báo cáo chi phí phát sinh (Priority: P3)

Quản lý viện và hành chính xem chi phí phát sinh của người cao tuổi theo người, theo kỳ (tháng), theo loại, theo dịch vụ, theo hoạt động, theo thuốc, theo vật phẩm; tỷ lệ chi phí tự sinh so với nhập tay; số khoản điều chỉnh sau chốt. Người xem chọn xem số đã chốt hoặc số tạm tính gồm cả khoản chưa chốt.

**Why this priority**: Dữ liệu chi phí có ở feature 010 và đã có bảng chi phí cho từng người; báo cáo tổng hợp là bổ trợ, nên xếp sau.

**Independent Test**: Chốt bảng tháng 10 của 3 người, để 1 người chưa chốt; có 2 khoản nhập tay, 1 khoản điều chỉnh sau chốt của tháng 9 nằm trong bảng tháng 10. Xem báo cáo tháng 10 ở hai chế độ. Đối chiếu với các bảng chi phí và file kế toán tháng 10.

**Acceptance Scenarios**:

1. **Given** dữ liệu trên, **When** Quản lý viện xem chế độ "đã chốt" tháng 10, **Then** báo cáo chỉ gồm 3 bảng đã chốt, tổng khớp tổng của 3 bảng và dòng tổng kiểm soát của file kế toán tháng 10 (feature 010 FR-039, FR-041); báo cáo nêu 1 người có bảng chưa chốt.
2. **Given** như trên, **When** chuyển sang chế độ "tạm tính", **Then** báo cáo cộng thêm khoản chưa hủy của bảng chưa chốt, trừ khoản nhập tay và mua hộ chưa Đã duyệt, gắn nhãn "tạm tính, chưa chốt"; khoản thiếu đơn giá không được cộng và được đếm riêng (feature 010 FR-038, Q-136).
3. **Given** khoản điều chỉnh của tháng 9 nằm trong bảng tháng 10, **When** xem báo cáo tháng 10, **Then** khoản được tính vào tháng 10, có cột kỳ gốc "tháng 9", và được đếm ở chỉ tiêu "số khoản điều chỉnh sau chốt"; tổng tháng 9 đã chốt không đổi (DBR-17, FR-052).
4. **Given** tháng 10 có 1.000 khoản, 980 do hệ thống tự sinh và 20 nhập tay, **When** xem "tỷ lệ chi phí tự sinh so với nhập tay", **Then** báo cáo nêu 98% theo số khoản và tỷ lệ tương ứng theo số tiền (18.4).
5. **Given** trưởng tầng, bác sĩ hoặc điều dưỡng, **When** mở báo cáo chi phí, **Then** hệ thống từ chối (4.4 dòng "Chi phí, khoản điều chỉnh").

---

### User Story 7 - Số liệu nhất quán và truy về được bản ghi nguồn (Priority: P3)

Mọi con số trên dashboard và báo cáo mở được xuống danh sách bản ghi tạo nên nó, và từ đó mở bản ghi ở chế độ chỉ xem của feature sở hữu. Cùng một chỉ tiêu, cùng phạm vi, cùng thời điểm thì dashboard và báo cáo cho cùng một số.

**Why this priority**: Giúp người xem tin số liệu và kiểm tra được; cần các báo cáo ở trên có trước.

**Independent Test**: Với 20 chỉ tiêu ngẫu nhiên, so số trên dashboard, số trên báo cáo cùng ngày và số dòng của danh sách chi tiết.

**Acceptance Scenarios**:

1. **Given** dashboard hiện "Cảnh báo đang mở: 5" lúc 10:00, **When** Quản lý viện mở báo cáo sức khỏe hôm nay lúc 10:00 cho cùng phạm vi, **Then** số cảnh báo đang mở cũng là 5, và danh sách chi tiết có đúng 5 dòng.
2. **Given** A ở tầng 1 tới 15/10 rồi chuyển sang tầng 2, **When** trưởng tầng T của tầng 1 mở danh sách công việc tháng 10 của tầng 1, **Then** danh sách có các công việc của A phát sinh tới 15/10 (gắn tầng 1, FR-011), mỗi dòng hiện họ tên A, phòng/giường lúc phát sinh và nội dung công việc (loại, trạng thái, thời điểm, người thực hiện, kết quả); T không mở được hồ sơ hiện tại của A, và không mở được công việc đầy đủ vì quyền "T" của trưởng tầng ở dòng "Checklist, ghi nhận công việc" chỉ theo phân công (FR-012, Q-196); **When** T mở danh sách liều Bỏ lỡ tháng 10 của tầng 1 và chọn một liều của A phát sinh trước 15/10, **Then** T mở được liều đó ở chế độ chỉ xem vì trưởng tầng có quyền "X" toàn viện ở dòng "Phát thuốc, thuốc khi cần"; **When** trưởng tầng của tầng 2 xem cùng tháng, **Then** chỉ thấy công việc của A từ 15/10.
3. **Given** bất kỳ thao tác xem nào trên dashboard hoặc báo cáo, **When** thao tác hoàn tất, **Then** không có bản ghi nghiệp vụ nào được tạo, sửa hay đổi trạng thái, và không có thông báo nào được gửi (FR-001).
4. **Given** trưởng tầng T nhận giao tầng 3 từ 01/11, **When** T xem báo cáo chăm sóc tháng 10 của tầng 3 vào ngày 05/11, **Then** T thấy số liệu tháng 10 của tầng 3 dù lúc đó chưa được giao (Q-192); **When** T xem tầng 1 mà T đã thôi giao từ 31/10, **Then** hệ thống từ chối.
5. **Given** điều dưỡng D làm ca ngày tầng 1 các ngày 03/10 và 04/10 và được phân công 8 người, **When** D xem báo cáo liều tháng 10 ngày 06/11, **Then** số liệu chỉ gồm liều của 8 người đó trong khoảng giờ ca ± CFG-M15-07 (mặc định \[2 giờ\]) của hai ca đó và liều chung của tầng trong hai ca; không gồm ngày khác (Q-192).

---

### User Story 8 - Báo cáo nhân sự và ca trực (Priority: P3)

Quản lý viện và trưởng tầng xem theo tháng: tỷ lệ phục vụ và tình trạng đạt của từng ca; số ca thiếu phủ và tổng thời gian ca chạy thiếu phủ theo tầng, vai trò; số mục "thiếu ca" đã công bố; yêu cầu đổi ca, nghỉ đột xuất theo trạng thái cuối và thời gian từ lập tới quyết định; phân bố số giờ làm trong tháng theo nhân viên.

**Why this priority**: Là số liệu feature 008 và 015 đã lưu cho feature này; giúp Quản lý viện điều chỉnh nhân lực. Không chặn các báo cáo khác.

**Independent Test**: Dựng tháng 11 cho tầng 2 với 60 ca, 4 ca thiếu phủ (tổng 20 giờ), 1 mục "thiếu ca" đã công bố, 6 yêu cầu đổi ca (4 Đã áp dụng, 1 Từ chối, 1 Hết hiệu lực), 3 yêu cầu nghỉ. So với tính tay.

**Acceptance Scenarios**:

1. **Given** dữ liệu trên, **When** Quản lý viện xem báo cáo ca trực tháng 11 tầng 2, **Then** thấy 4 ca thiếu phủ, 20 giờ thiếu phủ chia theo vai trò của dòng phủ không đạt, 1 mục "thiếu ca", yêu cầu đổi ca 4 / 1 / 1 theo trạng thái cuối (feature 015 FR-042).
2. **Given** trưởng tầng T của tầng 2, **When** T xem cùng báo cáo, **Then** chỉ thấy tầng 2 và nhân viên có phân công tại tầng 2; không thấy ca toàn viện.
3. **Given** yêu cầu nghỉ của nhân viên N có lý do "việc gia đình", **When** Quản lý viện hoặc T xem báo cáo, **Then** báo cáo chỉ đếm yêu cầu theo trạng thái; lý do nghỉ, lý do đổi ca MUST NOT hiện trong báo cáo hay danh sách chi tiết (19.3 "(Bổ sung, spec 015)").
4. **Given** điều dưỡng D, bác sĩ B hoặc hành chính H, **When** mở báo cáo ca trực, **Then** hệ thống từ chối (FR-010).

---

### User Story 9 - Xuất báo cáo ra file (Priority: P3)

Người có quyền xem một báo cáo xuất được báo cáo đó ra file với đúng phạm vi, bộ lọc và giới hạn trường như trên màn hình. Chỉ Quản lý viện xuất được danh sách có định danh người cao tuổi; vai trò khác chỉ xuất số liệu tổng hợp. Mỗi lần xuất được ghi lại.

**Why this priority**: Phục vụ họp và báo cáo lên trên; cần các báo cáo có trước.

**Independent Test**: Xuất cùng báo cáo chi phí tháng 10 bằng Quản lý viện và hành chính; xuất báo cáo sức khỏe bằng trưởng tầng. So file với màn hình và kiểm tra bản ghi lần xuất.

**Acceptance Scenarios**:

1. **Given** hành chính H đang xem báo cáo chi phí tháng 10 "theo thuốc" có khoản Thuốc, **When** H xuất file, **Then** file có cùng dòng tổng hợp, cùng tổng với màn hình, khoản Thuốc chỉ có mã vật phẩm, không có dòng hay cột định danh người cao tuổi, và file ghi "tính đến" thời điểm lấy số (FR-061, Q-133); **When** H chọn xuất báo cáo "chi phí theo người cao tuổi", **Then** hệ thống từ chối và nêu lý do "chỉ Quản lý viện xuất được danh sách có định danh".
2. **Given** trưởng tầng T xuất báo cáo sức khỏe tháng 10 tầng 1, **When** xuất xong, **Then** file chỉ có số liệu tổng hợp (theo mức, loại, ca), không có tên người cao tuổi; hệ thống ghi lần xuất: T, thời điểm, báo cáo, bộ lọc, khoảng, phạm vi, số dòng (FR-062).
3. **Given** file đã xuất ngày 01/11, **When** một kết quả trong tháng 10 bị đính chính ngày 03/11, **Then** file cũ không bị hệ thống sửa; xuất lại cho số mới.
4. **Given** nhân viên chăm sóc C, **When** C tìm xuất bất kỳ báo cáo nào, **Then** hệ thống từ chối.
5. **Given** Quản lý viện xem danh sách liều Bỏ lỡ tháng 10 có họ tên người cao tuổi, **When** xuất file mà không nhập mục đích, **Then** hệ thống từ chối; **When** nhập mục đích "họp giao ban an toàn thuốc" rồi xuất, **Then** file có đầy đủ danh sách như màn hình và lần xuất ghi dấu "có định danh" cùng mục đích (FR-061, FR-062, Q-198).
6. **Given** trưởng tầng T xem báo cáo tỷ lệ Đạt theo nhân viên và báo cáo giờ làm tháng 10 của tầng 1, **When** T xuất báo cáo tỷ lệ Đạt, **Then** file có tên nhân viên; **When** T xuất báo cáo giờ làm theo nhân viên, **Then** hệ thống từ chối vì chỉ Quản lý viện được xuất chỉ tiêu này (FR-061, FR-073, Q-197).

---

### Edge Cases

- **Mẫu số bằng 0**: tỷ lệ hiển thị "—" kèm "không có dữ liệu", không hiển thị 0% hay 100% (FR-008).
- **Ca qua nửa đêm**: "hôm nay" tính theo ngày dương lịch của thời điểm dự kiến (công việc, liều) hoặc thời điểm phát sinh (cảnh báo, sự cố); "ca hiện tại" tính theo ca đang diễn ra, có thể bắt đầu từ hôm qua (FR-006).
- **Bản ghi ngoại tuyến đồng bộ muộn**: số liệu của ngày, ca đã qua thay đổi sau khi đồng bộ. Báo cáo luôn dùng giá trị hiện hành và ghi "tính đến" thời điểm lấy số (FR-007).
- **Đính chính sau khi đã xem hoặc đã xuất**: báo cáo xem lại cho số mới; file đã xuất trước đó không bị sửa (FR-007, FR-061).
- **Người cao tuổi chuyển tầng trong kỳ**: bản ghi được gắn tầng/khu tại thời điểm phát sinh (FR-011); trưởng tầng của tầng cũ vẫn thấy bản ghi phát sinh ở tầng mình, nhưng không mở được hồ sơ hiện tại của người đó (FR-012, Q-192).
- **Điều dưỡng xem báo cáo khoảng không có ca nào của mình**: báo cáo báo "không có ca trong khoảng đã chọn", không hiện số 0 như số liệu thật (FR-012).
- **Trưởng tầng thay tạm có thời hạn**: phạm vi tầng chỉ có hiệu lực trong thời hạn được giao (13.3, Q-84).
- **Tầng tạm chưa có trưởng tầng**: không có ai xem được các nhóm chỉ tiêu chỉ dành cho trưởng tầng của tầng đó; Người phụ trách ca đang diễn ra thấy tầng đó với nhóm chỉ tiêu của Điều dưỡng (FR-012, Q-90, Q-195); Quản lý viện vẫn thấy qua dashboard toàn viện.
- **Mất quyền trong lúc xuất**: quyền được xét lúc bắt đầu và lúc hoàn tất lần xuất (FR-017); nếu mất quyền giữa chừng, lần xuất thất bại, không tạo file và không tạo bản ghi lần xuất thành công.
- **Công việc, liều chung của tầng**: được tính cho tầng, không tính cho nhân viên nào; hiện riêng dòng "việc chung" trong báo cáo theo nhân viên (Q-61, Q-85).
- **Nhân viên nhiều vai trò**: chỉ tiêu theo từng cặp (nhóm trường, phạm vi) rồi hợp lại (feature 002 FR-034); không cộng trùng một bản ghi.
- **Dữ liệu đã bị loại bỏ theo NFR-07**: báo cáo cho khoảng có dữ liệu đã hết hạn lưu giữ nêu rõ "dữ liệu trước ngày X đã được loại bỏ theo quy định lưu trữ"; không hiển thị số thiếu như số đúng.
- **Khoảng thời gian chứa hôm nay**: phần hôm nay là số tạm thời; báo cáo gắn nhãn "ngày chưa kết thúc".
- **Chỉ tiêu nguồn chưa có dữ liệu** (ví dụ tầng chưa cấu hình ngưỡng tỷ lệ phục vụ CFG-M09-01): hiện "chưa cấu hình", không hiện "đạt" hay "không đạt".
- **Tài khoản mất phạm vi trong khi đang xem**: lần làm mới kế tiếp áp phạm vi mới ngay (BR-M15-02); số liệu cũ trên màn hình không được mở chi tiết nữa.

## Requirements *(mandatory)*

### Functional Requirements

#### A. Nguyên tắc chung

- **FR-001**: Feature này MUST chỉ đọc. Mọi thao tác trên dashboard và báo cáo MUST NOT tạo, sửa, đổi trạng thái hay xóa dữ liệu nghiệp vụ của nhóm 1, 2, 3 (mục 1.5); MUST NOT gửi thông báo, nhắc hạn hay leo thang; MUST NOT kích hoạt quy tắc của feature khác. Bản ghi duy nhất feature này tạo là bản ghi lần xuất (FR-062), không phải dữ liệu nghiệp vụ. *(Nguồn: mô tả feature; 1.5; 18; Q-193)*
- **FR-002**: Mỗi chỉ tiêu MUST lấy trạng thái, phân loại và kết quả đúng như feature sở hữu đã xác định (ví dụ trạng thái Quá hạn của công việc do feature 005 xác định, tỷ lệ phục vụ do feature 008 tính). Feature này MUST NOT tự suy ra lại trạng thái hay kết quả từ dữ liệu thô. Khi spec này nhắc lại một công thức hay định nghĩa của feature nguồn (ví dụ tỷ lệ tham gia của feature 014, tỷ lệ phục vụ của feature 008), định nghĩa của feature nguồn là căn cứ; khi feature nguồn đổi, spec này MUST được cập nhật theo. *(Nguồn: 1.3; bảng giao tiếp; constitution I)*
- **FR-003**: Mỗi chỉ tiêu MUST có định nghĩa được ghi trong spec này (FR-020 → FR-073): đối tượng được đếm, điều kiện, mốc thời gian dùng để xếp vào khoảng, và với tỷ lệ là tử số, mẫu số. Người xem MUST xem được định nghĩa của chỉ tiêu ngay tại chỗ hiển thị. *(Nguồn: constitution IV, VIII)*
- **FR-004**: Báo cáo MUST gồm các nhóm ở mục 18.1 → 18.5 và nhóm "nhân sự và ca trực" (mục I). Số liệu thăm, phản hồi (feature 012), đồ gửi (feature 013), thống kê thông báo (feature 009) MUST NOT thuộc phạm vi giai đoạn này. *(Nguồn: 18; feature 008 FR-037, feature 015 FR-042; Clarification 2026-09-27, đề xuất Q-194)*
- **FR-005**: Mọi mốc thời gian MUST theo múi giờ Asia/Ho_Chi_Minh; khoảng ngày là ngày dương lịch từ 00:00 đến hết 23:59:59, gồm cả ngày đầu và ngày cuối; ngày hiển thị dạng dd/MM/yyyy, tiền dạng VND. *(Nguồn: NFR-09, NFR-11, DBR-25)*
- **FR-006**: "Hôm nay" MUST là ngày dương lịch hiện tại theo đồng hồ hệ thống. Công việc, liều được xếp vào ngày theo thời điểm dự kiến; cảnh báo, sự cố, chỉ số theo thời điểm tạo hoặc thời điểm đo; chi phí theo ngày phát sinh (feature 010). Với bản ghi ngoại tuyến, mốc xếp khoảng là thời điểm thực hiện hoặc thời điểm đo ghi trên thiết bị, không phải thời điểm đồng bộ (DBR-25; cùng cách feature 005 xét Hoàn thành). "Ca hiện tại" MUST là ca đang diễn ra của tầng/khu vực theo lịch ca đã công bố (feature 008). Khi chia "theo ca", một bản ghi thuộc ca của tầng/khu (FR-011) có khoảng giờ chứa mốc xếp của bản ghi (với liều: thời điểm dự kiến), kể cả liều chung của tầng (Q-61). *(Nguồn: 18.5; NFR-09; DBR-25)*
- **FR-007**: Mọi số liệu MUST dùng giá trị hiện hành sau đính chính; bản ghi đã "Hủy ghi nhận" MUST NOT được đếm như bản ghi có hiệu lực. Mỗi dashboard và báo cáo MUST ghi "tính đến" thời điểm lấy số liệu. Báo cáo có khoảng chứa hôm nay MUST gắn nhãn "ngày chưa kết thúc". Bản ghi ngoại tuyến đang "chờ xem lại" (Q-01, Q-62) MUST được đếm theo giá trị đang ghi; chỉ tiêu có bản ghi loại này MUST nêu kèm "N bản ghi chờ xem lại" và danh sách chi tiết đánh dấu các bản ghi đó; đính chính sau khi xem lại được phản ánh như mọi đính chính khác. *(Nguồn: feature 000 FR-030; 1.5; Clarification 2026-09-27, đề xuất Q-201)*
- **FR-008**: Khi mẫu số của một tỷ lệ bằng 0, hệ thống MUST hiện "—" kèm "không có dữ liệu". Khi dữ liệu cấu hình mà chỉ tiêu cần chưa có, hệ thống MUST hiện "chưa cấu hình". Tỷ lệ MUST hiển thị kèm tử số và mẫu số. *(Suy ra từ yêu cầu kiểm tra được, constitution IV)*
- **FR-009**: Mỗi con số trên dashboard và báo cáo MUST mở được danh sách bản ghi tạo nên nó, và danh sách MUST có đúng số dòng bằng con số (trừ chỉ tiêu là trung bình hoặc tổng tiền, khi đó danh sách gồm các bản ghi được đưa vào phép tính). Từ mỗi dòng, người xem MUST mở được bản ghi ở chế độ chỉ xem của feature sở hữu, nếu có quyền xem bản ghi đó. *(Nguồn: 1.3 "truy xuất lịch sử"; 15.4 với chi phí)*

#### B. Phạm vi dữ liệu và quyền

- **FR-010**: Nhóm chỉ tiêu một vai trò được thấy MUST xác định theo ba quy tắc:
  1. **Có hay không**: vai trò có quyền ở dòng "Dashboard, báo cáo" của 4.4 **và** có một trong các quyền X, P, T, D, C ở **dòng nguồn chính** của nhóm chỉ tiêu. Mỗi nhóm có đúng một dòng nguồn chính, là dòng quản lý đối tượng được thống kê, ghi **in đậm** ở cột cuối của bảng dưới; các dòng khác trong cột chỉ để tham chiếu và MUST NOT mở thêm quyền.
  2. **Phạm vi**: lấy theo ký hiệu của vai trò ở dòng "Dashboard, báo cáo" (P: theo phân công, tính theo FR-012; P¹¹ và X: toàn viện), kể cả khi dòng nguồn chính cho quyền toàn viện (ví dụ Điều dưỡng "X" ở dòng khoanh vùng vẫn chỉ thấy tầng/khu của phân công).
  3. **Ngoại lệ**: "P" ở dòng "Lịch ca, phân công" chỉ là quyền xem lịch của chính người xem (13.2), không mở ra chỉ tiêu tổng hợp của ca (tỷ lệ phục vụ, nhân sự).

  Kết quả là bảng dưới đây, MUST được áp đúng; ô "—" nghĩa là nhóm chỉ tiêu không hiện và mọi yêu cầu mở bị từ chối. Nhân viên chăm sóc, dinh dưỡng viên, nhân viên bếp, nhân viên vệ sinh và người thân MUST NOT có dashboard và báo cáo. Người phụ trách ca dùng cột ĐD với phạm vi ở FR-012. *(Nguồn: 4.4 dòng "Dashboard, báo cáo" và các dòng nguồn chính; 19.3; feature 002 FR-035; BR-M15-01)*

| Nhóm chỉ tiêu | QL | TT | BS | ĐD | HC | Dòng nguồn ở 4.4 (nguồn chính in đậm) |
| --- | --- | --- | --- | --- | --- | --- |
| Người cao tuổi: số lượng, trạng thái, loại lưu trú, mức chăm sóc, biến động, tạm vắng | Toàn viện | Tầng được giao | Toàn viện | Phạm vi phân công | Toàn viện | **Hồ sơ người cao tuổi**; Tạm vắng, trở về |
| Danh sách chờ | Toàn viện | — | — | — | Toàn viện | **Danh sách chờ, điểm ưu tiên** |
| Phòng, giường theo trạng thái | Toàn viện | Tầng được giao | — | — | Toàn viện | **Cấu hình phòng, giường**; Phân bổ, chuyển giường |
| Công việc chăm sóc (trạng thái, ca, tầng, ghi nhận muộn) | Toàn viện | Tầng được giao | Toàn viện | Phạm vi phân công | — | **Checklist, ghi nhận công việc**; Xử lý việc quá hạn |
| Chỉ tiêu theo từng nhân viên (công việc, kiểm tra chất lượng) | Toàn viện | Nhân viên có phân công tại tầng | — | — | — | **Hồ sơ nhân viên, chứng chỉ**; Kiểm tra chất lượng |
| Vệ sinh phòng, khu vực | Toàn viện | Tầng được giao | — | — | — | **Lịch vệ sinh**; Yêu cầu vệ sinh đột xuất |
| Phiếu bữa ăn giao trễ, có sai lệch | Toàn viện | Tầng được giao | — | — | — | **Suất ăn đã chốt, phiếu bữa ăn**; Nhận phiếu, báo sai lệch |
| Hoạt động, tỷ lệ tham gia, chuyến đi, nguy cơ cô lập | Toàn viện | Tầng được giao | — | — | — | **Hoạt động, ngoài viện** |
| Kiểm tra chất lượng | Toàn viện | Tầng được giao | — | — | — | **Kiểm tra chất lượng** |
| Chỉ số, cảnh báo, sự cố, trường hợp cần theo dõi | Toàn viện | Tầng được giao | Toàn viện | Phạm vi phân công | — | **Chỉ số sức khỏe**; Xử lý cảnh báo; Sự cố, khẩn cấp; Danh sách tiếp xúc |
| Sự cố thuộc loại mang dấu "không thuộc sức khỏe" (FR-042a) | (như dòng trên) | (như dòng trên) | (như dòng trên) | (như dòng trên) | Toàn viện, không có mô tả, diễn biến | **Sự cố, khẩn cấp** (HC "T¹⁰"); 19.3 giới hạn trường của Hành chính |
| Yêu cầu đánh giá lại quá hạn | Toàn viện | Tầng được giao | Toàn viện | Phạm vi phân công | — | **Đánh giá, quy đổi mức chăm sóc** |
| Liều thuốc (tỷ lệ Bỏ lỡ, Từ chối) | Toàn viện | Tầng được giao | Toàn viện | Phạm vi phân công | — | **Phát thuốc, thuốc khi cần** |
| Phiếu đối chiếu thuốc quá hạn | Toàn viện | — | Toàn viện | Phạm vi phân công | — | **Đối chiếu thuốc, thuốc gia đình gửi** |
| Khu đang khoanh vùng | Toàn viện | Tầng được giao | Toàn viện | Tầng/khu của phân công | Toàn viện | **Khoanh vùng lây nhiễm** |
| Bàn giao chưa xác nhận | Toàn viện | Tầng được giao | Toàn viện | Tầng/khu của phân công | — | **Bàn giao ca** |
| Tỷ lệ phục vụ ca hiện tại | Toàn viện | Tầng được giao | — | — | — | **Lịch ca, phân công** (BS, ĐD, HC "P" là lịch của chính mình, quy tắc 3) |
| Giấy phép cơ sở sắp hết hạn | Toàn viện | — | — | — | — | Không có dòng ở 4.4; giấy phép hoạt động của cơ sở (10.4), cảnh báo chỉ tới Quản lý viện (feature 002 FR-039) |
| Giấy phép hành nghề, chứng chỉ đào tạo nhân viên sắp hết hạn | Toàn viện | Nhân viên có phân công tại tầng | — | — | — | **Hồ sơ nhân viên, chứng chỉ** |
| Chi phí phát sinh | Toàn viện | — | — | — | Toàn viện | **Chi phí, khoản điều chỉnh**; Chốt kỳ, xuất kế toán |
| Nhân sự và ca trực (mục I) | Toàn viện | Tầng được giao; nhân viên có phân công tại tầng | — | — | — | **Lịch ca, phân công** (QL, TT "T, D"; vai trò khác "P" là lịch của chính mình, quy tắc 3); Yêu cầu đổi ca; Hồ sơ nhân viên, chứng chỉ |

- **FR-010a**: Quản lý viện MUST thu hẹp được quyền ở FR-010 theo vai trò hoặc theo tài khoản, theo từng nhóm chỉ tiêu và riêng giữa dashboard và báo cáo, qua cơ chế thu hẹp của feature 002 FR-025; MUST NOT mở rộng quá bảng FR-010 (Q-15). Quyền xuất đi theo quyền xem báo cáo, không có quyền xuất riêng. *(Nguồn: 19.2 "(Bổ sung, Q-15)"; feature 002 FR-025)*
- **FR-011**: Mỗi bản ghi MUST được xếp vào tầng/khu vực mà nó gắn tại thời điểm phát sinh (công việc: tầng/khu khi sinh hoặc khi chuyển thành việc chung; cảnh báo, sự cố: tầng/khu của người cao tuổi lúc tạo; liều: tầng của người cao tuổi tại thời điểm dự kiến). Báo cáo "theo tầng" MUST dùng cách xếp này. Trong spec này, "tầng/khu" là một tầng hoặc một khu vực theo cấu trúc cơ sở (2.2) và đối tượng phân công (13.4); "khu" trong tỷ lệ phục vụ là phạm vi tính của feature 008 (BR-M09-02); khu vực khoanh vùng theo Q-69. *(Suy ra từ 1.3 "gắn với người cao tuổi, thời gian"; 2.2, 13.4)*
- **FR-012**: Phạm vi khi báo cáo MUST tính như sau:
  - **Dashboard** (trong ngày, ca hiện tại): phạm vi hiện hành theo feature 002, kể cả khoảng CFG-M15-07 (mặc định \[2 giờ\]) trước và sau ca.
  - **Trưởng tầng, báo cáo mọi khoảng**: các tầng/khu mà người xem đang được giao tại lúc xem (13.3, kể cả giao tạm có thời hạn còn hiệu lực); số liệu gồm mọi bản ghi gắn tầng/khu đó theo FR-011, kể cả bản ghi phát sinh trước khi được giao. Tầng đã thôi giao không còn xem được.
  - **Điều dưỡng, báo cáo mọi khoảng**: chỉ các bản ghi thuộc phân công của chính người xem (người cao tuổi được phân công, việc chung và liều chung của tầng/khu trong ca, Q-61, Q-85) có thời điểm (theo FR-006) nằm trong giờ ca ± CFG-M15-07 của các ca người xem thuộc về, trong khoảng được chọn. "Thuộc về" tính theo khoảng thời gian người xem thật sự có tên trong ca (từ lúc được bổ sung tới lúc bị gỡ, feature 008, 015); ca bị hủy, ca người xem bị ghi vắng hoặc có nghỉ có duyệt (Q-184) MUST NOT được tính.
  - **Người phụ trách ca** (điều dưỡng giữ nhiệm vụ này, 2.4): trong giờ ca ± CFG-M15-07 của ca mình phụ trách, dashboard dùng nhóm chỉ tiêu của cột ĐD ở FR-010 với phạm vi là **cả tầng/khu của ca**; báo cáo khoảng đã qua gồm cả tầng/khu trong giờ các ca mình phụ trách, và theo quy tắc Điều dưỡng ở trên cho các ca khác. Khi tầng tạm chưa có trưởng tầng (Q-90), Người phụ trách ca MUST NOT được thêm nhóm chỉ tiêu nào chỉ dành cho trưởng tầng. *(Clarification 2026-09-27, đề xuất Q-195)*
  - Chỉ tiêu "Khu đang khoanh vùng" và "Bàn giao chưa xác nhận" chỉ có trên dashboard, theo phạm vi hiện hành; không có báo cáo theo khoảng cho hai chỉ tiêu này.
  - Với bản ghi của người cao tuổi không thuộc phạm vi hiện hành của người xem (đã chuyển tầng, ở trạng thái cuối, hoặc bản ghi phát sinh trước khi được giao), danh sách chi tiết MUST chỉ hiện: họ tên, phòng/giường tại thời điểm phát sinh, và nội dung của chính bản ghi đó (loại, trạng thái, thời điểm, người thực hiện, kết quả). "Kết quả" gồm giá trị đo, tên thuốc và các trường sức khỏe khác của bản ghi chỉ khi vai trò của người xem được xem nhóm trường đó (FR-014). Từ danh sách, người xem MUST NOT mở được hồ sơ hiện tại của người đó, và chỉ mở được bản ghi đầy đủ khi quyền hiện hành ở feature sở hữu cho phép (ví dụ quyền "X" toàn viện của trưởng tầng ở dòng "Phát thuốc, thuốc khi cần", feature 002 FR-034). Với người đang trong phạm vi, việc mở bản ghi theo FR-009. *(Clarification 2026-09-27, đề xuất Q-196)*

  *(Nguồn: 18.5 "(Bổ sung)"; BR-M15-02; Q-14; Clarification 2026-09-27, đề xuất Q-192)*
- **FR-013**: Số liệu tổng MUST chỉ gồm bản ghi trong phạm vi của người xem. Hệ thống MUST NOT hiện, trong bộ lọc, chú thích, tổng toàn viện hay thông báo lỗi, thông tin cho biết sự tồn tại hay số lượng bản ghi ngoài phạm vi. Bộ lọc MUST chỉ cho chọn giá trị (tầng, khu, nhân viên, người cao tuổi) thuộc phạm vi. *(Nguồn: feature 002 FR-035)*
- **FR-014**: Giới hạn trường (feature 002 FR-022) MUST áp cho mọi nhãn, cột, danh sách chi tiết và file xuất: Hành chính không thấy chỉ tiêu dựng từ dị ứng, bệnh nền, tiền sử, chỉ số, thuốc, kết quả đánh giá; với chi phí loại Thuốc, Hành chính chỉ thấy mã vật phẩm, số lượng, đơn giá, thành tiền (Q-133). Lý do của chỉ định hạn chế hoạt động là thông tin sức khỏe, chỉ hiện cho người có quyền xem sức khỏe (19.3). Trạng thái người cao tuổi theo 5.5, kể cả "Điều trị tại bệnh viện", là trạng thái lưu trú và hiện được cho Hành chính; lý do, chẩn đoán, nơi điều trị là thông tin sức khỏe, Hành chính MUST NOT thấy. Bác sĩ không có giới hạn trường riêng trên dashboard, báo cáo; phần "vẫn áp giới hạn trường" của chú thích ¹¹ (4.4) chỉ tác động Hành chính (feature 002 FR-035). *(Nguồn: 19.3; feature 002 FR-022, FR-035; Q-133)*
- **FR-015**: Tài khoản nhiều vai trò MUST được áp phạm vi và giới hạn trường theo từng vai trò rồi hợp lại (feature 002 FR-034); một bản ghi MUST NOT được đếm hai lần. *(Nguồn: 19.2; feature 002 FR-034)*
- **FR-016**: Khi không xác định được phạm vi của người xem vì dữ liệu nguồn thiếu hoặc lỗi, hệ thống MUST từ chối mở dashboard, báo cáo và báo Quản lý viện theo feature 002 FR-041b. *(Nguồn: 19.3 "(Bổ sung, spec 002)")*
- **FR-017**: Quyền và phạm vi MUST được xét lại mỗi lần lấy số liệu và mỗi lần mở danh sách chi tiết; thay đổi phân công có hiệu lực từ lần kế tiếp (BR-M15-02). *(Nguồn: BR-M15-02; feature 002 FR-033)*

#### C. Dashboard theo vai trò

- **FR-020**: Dashboard MUST hiển thị các chỉ tiêu ở bảng dưới, cho vai trò được phép theo FR-010 và trong phạm vi của FR-012. Vai trò không có chỉ tiêu nào trong phạm vi hiện hành MUST thấy "hiện không có phạm vi phân công". *(Nguồn: 18.5)*

| Chỉ tiêu | Định nghĩa | Nguồn |
| --- | --- | --- |
| Người đang lưu trú (gồm vắng tạm thời) | Số hồ sơ ở trạng thái Đang lưu trú, Tạm vắng, Hoạt động bên ngoài, Điều trị tại bệnh viện lúc xem, luôn hiện kèm con số của từng trạng thái 5.5 và loại lưu trú; người bán trú có lịch hôm nay chia thêm theo trạng thái có mặt theo ngày (3.4) | Feature 001, 004, 005 |
| Tình trạng phòng/giường | Số giường theo từng trạng thái của bảng trạng thái giường feature 003, tách riêng: Trống, Đang sử dụng, Đang giữ chỗ (hồ sơ chờ), Đang giữ chỗ (người vắng), Chờ vệ sinh, Đang bảo trì, Không sử dụng; "tổng giường có thể dùng" không tính giường Không sử dụng; số phòng đang cách ly | Feature 003 |
| Công việc hôm nay | Số công việc có thời điểm dự kiến trong hôm nay, chia theo trạng thái 8.3 và mức quan trọng; công việc vệ sinh tính riêng | Feature 005, 003 |
| Công việc quá hạn | Số công việc đang ở Quá hạn lúc xem (mọi ngày dự kiến), chia theo mức quan trọng; nêu riêng số đã có trong bàn giao | Feature 005 |
| Cảnh báo | Số cảnh báo đang mở (Mới, Leo thang, Đã tiếp nhận, Đang xử lý) chia theo mức và trạng thái; cảnh báo mang dấu "đã leo thang tối đa" đặt nổi bật tới khi được tiếp nhận | Feature 007 FR-034, FR-081 |
| Sự cố | Số sự cố tạo hôm nay và số sự cố chưa đóng (trạng thái Mới, Đang xử lý, Đang theo dõi; không tính Đã hủy), chia theo mức và loại; sự cố Mới đã leo thang đặt nổi bật tới khi được tiếp nhận | Feature 007 |
| Hoạt động | Số buổi hôm nay theo trạng thái buổi; số lượt đăng ký, có mặt; chuyến đi đang diễn ra và chuyến quá giờ về | Feature 014 |
| Chi phí | Kỳ đang mở và kỳ vừa kết thúc: số bảng chi phí theo trạng thái, hạn chốt CFG-M11-01, số khoản mang dấu "thiếu đơn giá", số bảng quá hạn chốt | Feature 010 |
| Giấy phép sắp hết hạn | Giấy phép của cơ sở hết hạn trong CFG-M06-02 (mặc định \[60 ngày\]) hoặc đã hết hạn | Giấy phép hoạt động của cơ sở (10.4); cùng mốc với cảnh báo của feature 002 FR-039 |
| Giấy phép hành nghề/đào tạo sắp hết hạn | Giấy phép hành nghề hết hạn trong CFG-M06-02 (mặc định \[60 ngày\]); chứng chỉ đào tạo hết hạn trong CFG-M09-03 (mặc định \[60 ngày\]); kèm số ngày còn lại | Dữ liệu giấy phép, chứng chỉ: feature 008; cùng mốc với cảnh báo giấy phép hành nghề của feature 002 FR-039 và cảnh báo đào tạo của feature 008 |
| Tỷ lệ phục vụ ca hiện tại | Giá trị và tình trạng đạt/không đạt theo khu do feature 008 tính cho ca đang diễn ra (BR-M09-02, Q-79, CFG-M09-01) | Feature 008 FR-037 |
| Khu đang khoanh vùng | Tầng/khu vực đang khoanh vùng và thời điểm bắt đầu, theo phạm vi của dòng ở FR-010; "số người cao tuổi trong vùng" chỉ đếm người trong phạm vi của người xem, nhãn "trong phạm vi của bạn" với vai trò có phạm vi "P" (FR-013); Quản lý viện, Bác sĩ, Hành chính thấy số toàn viện | Feature 007 |
| Bàn giao chưa xác nhận | Bàn giao ở trạng thái Đã lập hoặc Có ý kiến, cộng các ca đang Chờ bàn giao mà bàn giao còn Bản nháp; mỗi mục kèm trạng thái và thời gian đã chờ từ giờ kết thúc ca trước | Feature 008 |

- **FR-021**: Số liệu dashboard MUST được cập nhật mà người xem không phải thao tác, trong thời hạn ở SC-002, và MUST ghi thời điểm cập nhật gần nhất. *(Nguồn: 18.5 "số liệu trong ngày"; Assumptions)*

#### D. Báo cáo người cao tuổi (18.1)

- **FR-022**: Báo cáo tại một ngày MUST đếm người cao tuổi theo trạng thái 5.5, loại lưu trú và mức chăm sóc **có hiệu lực tại ngày đó** (theo lịch sử trạng thái và phiên bản của feature 001, 004), không theo giá trị hiện tại. Hồ sơ ở trạng thái cuối MUST vẫn được đếm cho các ngày trước khi chuyển. Loại lưu trú lấy theo hợp đồng Hiệu lực tại ngày đó, kể cả hợp đồng mang dấu "quá hạn hợp đồng" (Q-20); người Đang tiếp nhận chưa có hợp đồng Hiệu lực xếp theo hợp đồng đã ký chờ hiệu lực, nếu không có thì vào nhóm "chưa xác định". *(Nguồn: 18.1; 5.5; 1.5 "phiên bản"; Q-20, Q-22)*
- **FR-023**: Báo cáo biến động trong một khoảng MUST đếm số lần Hoàn tất tiếp nhận, Kết thúc lưu trú, Qua đời, Hủy tiếp nhận, bắt đầu và kết thúc Tạm vắng, Điều trị tại bệnh viện, đổi loại lưu trú, đổi mức chăm sóc, theo thời điểm hiệu lực của lệnh. *(Nguồn: 18.1 "trạng thái"; 5.5, 6.6 → 6.8)*
- **FR-024**: Báo cáo phân bố phòng MUST nêu số người theo tầng, phòng và số giường theo từng trạng thái của bảng trạng thái giường feature 003 (tách hai loại Đang giữ chỗ; có Đang bảo trì, Không sử dụng như FR-020) tại thời điểm xem hoặc tại một ngày đã qua. *(Nguồn: 18.1 "phân bố phòng"; 7.2; feature 003)*
- **FR-025**: Báo cáo danh sách chờ MUST nêu số hồ sơ theo trạng thái hồ sơ chờ (feature 004), theo loại lưu trú và mức chăm sóc dự kiến; thời gian chờ trung bình và dài nhất của hồ sơ đang chờ; số hồ sơ rời danh sách trong khoảng theo lý do (tiếp nhận, từ chối, hủy chờ). *(Nguồn: 18.1 "danh sách chờ"; 6.2)*

#### E. Báo cáo chăm sóc (18.2)

- **FR-030**: Báo cáo công việc MUST đếm theo trạng thái 8.3 (Chưa đến hạn, Đến hạn, Quá hạn, Hoàn thành, Hoàn thành trễ, Không thực hiện, Hủy), cho phép chia theo nhân viên thực hiện, ca, tầng, loại công việc, mức quan trọng. "Hoàn thành" của 18.2 là Hoàn thành cộng Hoàn thành trễ; "chưa hoàn thành" là Không thực hiện cộng công việc chưa ở trạng thái cuối; "quá hạn" là công việc đã từng ở Quá hạn trong khoảng, kể cả công việc sau đó được ghi Hoàn thành vì thời điểm thực hiện nằm trong khung (bản ghi muộn, đồng bộ sau mất kết nối, feature 005). Công việc chung của tầng MUST hiện ở dòng riêng "việc chung" khi chia theo nhân viên. Công việc đóng bởi "Hệ thống" MUST tách khỏi công việc đóng bởi nhân viên, và chia theo đúng danh mục quy tắc đóng mà feature 005 FR-028 ghi (tự đóng sau bàn giao Q-32, đóng do trạng thái cuối Q-38, hủy tự động). *(Nguồn: 18.2; 8.3; 8.7; feature 005 FR-028)*
- **FR-031**: Tỷ lệ hoàn thành đúng hạn công việc Bắt buộc = số Hoàn thành ÷ (Hoàn thành + Hoàn thành trễ + Không thực hiện), chỉ tính công việc Bắt buộc có thời điểm dự kiến trong khoảng; công việc Hủy, công việc chưa ở trạng thái cuối, và công việc Không thực hiện do "Hệ thống" đóng (FR-030) không vào mẫu số; loại cuối được đếm riêng bên cạnh tỷ lệ. Công việc từng Quá hạn nhưng có trạng thái cuối Hoàn thành được tính là đúng hạn. *(Nguồn: 18.2 "(Bổ sung)"; feature 005)*
- **FR-032**: Số ghi nhận muộn MUST đếm kết quả mang nhãn "ghi nhận muộn" (BR-M04-12, CFG-M04-06 mặc định \[2 giờ\]), theo nhân viên, ca, tầng; điểm danh buổi hoạt động mang nhãn này tính riêng. *(Nguồn: 18.2; BR-M04-12)*
- **FR-033**: Báo cáo vệ sinh MUST nêu tỷ lệ công việc vệ sinh đúng hạn theo khu = số Hoàn thành ÷ (Hoàn thành + Hoàn thành trễ + Không thực hiện), với cùng loại trừ như FR-031 (công việc Hủy, chưa ở trạng thái cuối, Không thực hiện do "Hệ thống" đóng), và thời gian giường ở trạng thái Chờ vệ sinh (trung bình, dài nhất, số lần vượt hạn CFG-M03-04, mặc định \[4 giờ\]). *(Nguồn: 18.2 "(Bổ sung, vệ sinh)"; BR-M03-09)*
- **FR-034**: Báo cáo phiếu bữa ăn MUST nêu, theo tầng/khu và bữa:
  - **số phiếu giao trễ**: phiếu có thời điểm Đã giao sau giờ bữa + CFG-M08-04 (mặc định \[30 phút\]), hoặc chưa Đã giao khi tới mốc đó (BR-M08-12);
  - **số phiếu có sai lệch**: phiếu đã từng ở trạng thái Có sai lệch (BR-M08-11), kể cả khi bếp đã xử lý sau đó; phần bổ sung (12.5) không tính là phiếu riêng.

  *(Nguồn: 18.2; BR-M08-11, BR-M08-12; feature 011 bảng giao tiếp)*
- **FR-035**: Báo cáo hoạt động MUST nêu số buổi, số lượt đăng ký, số lượt Có mặt, số lượt "Không ghi nhận", tỷ lệ tham gia theo hoạt động, theo tầng; tỷ lệ tham gia = số lượt Có mặt ÷ (số lượt Có mặt + số lượt Vắng có lý do "từ chối", "sức khỏe" hoặc "khác"); lượt "Vắng – đang vắng mặt", "Vắng – không đến", "Không ghi nhận" và đăng ký Đã hủy không vào mẫu số. Báo cáo MUST nêu số chuyến quá giờ về (quá giờ về dự kiến + CFG-M04-07, mặc định \[30 phút\], mà chưa điểm danh về, BR-M04-17), trong đó số chuyến tới mốc giờ về + 2 × CFG-M04-07 (Q-172) tính riêng, và số sự cố thiếu người. *(Nguồn: 18.2; BR-M04-17; Q-169, Q-172, Q-173; feature 014 FR-070)*
- **FR-036**: Báo cáo MUST nêu danh sách người cao tuổi có cảnh báo nguy cơ cô lập đang mở và số ngày tính của chuỗi (BR-M04-22, CFG-M04-10, mặc định \[7 ngày\]). *(Nguồn: 18.2; feature 014 FR-058)*
- **FR-037**: Báo cáo chất lượng MUST nêu tỷ lệ Đạt = số Đạt ÷ (số Đạt + số Không đạt), theo nhân viên thực hiện công việc gốc và theo tầng; số mục "Không kiểm tra được" và "Quá hạn kiểm tra" tính riêng. Công việc do trưởng tầng được giao thực hiện MUST ghi "ngoài phạm vi kiểm tra" và MUST NOT hiện tỷ lệ Đạt cho trưởng tầng (Q-168). *(Nguồn: 18.2; BR-M04-23; feature 014 FR-065)*

#### F. Báo cáo sức khỏe và thuốc (18.3)

- **FR-040**: Báo cáo chỉ số MUST nêu số lần đo và số lần vượt ngưỡng theo loại chỉ số, theo tầng; và cho một người cao tuổi, chuỗi giá trị theo thời gian. *(Nguồn: 18.3 "chỉ số"; feature 007 FR-081)*
- **FR-041**: Báo cáo cảnh báo MUST nêu số cảnh báo theo mức, loại, tầng; thời gian trung bình từ khi tạo đến khi tiếp nhận theo mức, chỉ tính cảnh báo đã được tiếp nhận; số cảnh báo chưa tiếp nhận tính riêng; số lần leo thang theo cấp. Cảnh báo được gộp theo DBR-18 được đếm là một, và mốc "tạo" là lần tạo đầu tiên (các lần gộp không đặt lại mốc). Mốc "tiếp nhận" là: thời điểm chuyển Đã tiếp nhận; với cảnh báo đi thẳng sang Chuyển sự cố bằng "Kích hoạt khẩn cấp từ cảnh báo", là thời điểm kích hoạt; với cảnh báo Quản lý viện tiếp nhận ở Leo thang cấp 2 rồi giao (Q-71), là thời điểm Quản lý viện tiếp nhận. Cách tính MUST giống cách tính của feature 007 SC-013. *(Nguồn: 18.3 "(Bổ sung)"; BR-M05-01, BR-M05-02; DBR-18; Q-67, Q-71)*
- **FR-042a**: Hành chính MUST thấy chỉ tiêu sự cố (dashboard và báo cáo, toàn viện) chỉ với các loại sự cố mang dấu "không thuộc sức khỏe" trên danh mục loại sự cố của feature 007 (mặc định: đồ gửi thất lạc, đồ gửi hư hỏng, cơ sở vật chất). Hành chính chỉ thấy số lượng theo loại, mức, trạng thái và danh sách gồm người cao tuổi, loại, mức, trạng thái, thời điểm; MUST NOT thấy mô tả, xử lý ban đầu, diễn biến. Khi một sự cố đổi loại sang loại không mang dấu (Q-73), sự cố đó MUST rời khỏi số liệu của Hành chính từ lần lấy số kế tiếp. Dấu do Quản lý viện đặt trên danh mục loại sự cố (nhóm 1, feature 007); mỗi lần đổi dấu MUST được ghi nhật ký với giá trị trước/sau (19.4). Số liệu của Hành chính, kể cả cho khoảng đã qua, MUST luôn tính theo dấu hiện hành, giống cách hiển thị theo quyền hiện hành ở Q-99. *(Nguồn: 19.3; feature 002 FR-035 "sự cố y tế"; Clarification 2026-09-27, Q-202)*
- **FR-042**: Báo cáo sự cố MUST nêu số sự cố theo mức, loại, tầng; mức dùng để đếm là mức hiện hành; sự cố Đã hủy không được đếm trong số sự cố mà được nêu riêng; "sự cố chưa đóng" là sự cố ở Mới, Đang xử lý, Đang theo dõi. Báo cáo MUST nêu, tính riêng với cảnh báo: số lần leo thang của sự cố (sự cố Mới quá hạn tiếp nhận, feature 007) theo cấp và theo mức, và thời gian trung bình từ khi tạo đến khi tiếp nhận xử lý (chuyển Đang xử lý) theo mức; sự cố chưa được tiếp nhận tính riêng. *(Clarification 2026-09-27, đề xuất Q-199)* Danh sách "trường hợp cần theo dõi" MUST gồm lịch theo dõi sau ngã và theo dõi tiếp xúc đang Hiệu lực tại ngày xem. *(Nguồn: 18.3; Q-73; feature 007 FR-081)*
- **FR-043**: Tỷ lệ liều Bỏ lỡ và tỷ lệ liều Từ chối = số liều ở trạng thái đó ÷ số liều có trạng thái cuối Đã dùng, Từ chối, Không thực hiện hoặc Bỏ lỡ, có thời điểm dự kiến trong khoảng; liều Đã hủy và liều kết thúc ở Tạm dừng không vào mẫu số. Liều Mang theo đã được ghi Đã dùng hoặc Không thực hiện có vào mẫu số. Liều Không thực hiện do người cao tuổi chuyển trạng thái cuối (feature 006 FR-047) không vào mẫu số và được đếm riêng. Tỷ lệ MUST chia được theo người cao tuổi và theo ca (ca xác định theo FR-006). Liều "dùng trễ" MUST được đếm riêng. *(Nguồn: 18.3; feature 006 FR-021, FR-022, FR-047, FR-051)*
- **FR-044**: Báo cáo MUST nêu yêu cầu đánh giá lại đang quá hạn CFG-M01-03 (mặc định \[48 giờ\]) và phiếu đối chiếu thuốc đang quá hạn CFG-M07-05 (mặc định \[4 giờ\]), mỗi loại có số lượng và danh sách. Tình trạng "quá hạn" MUST lấy từ feature sở hữu (FR-002): phiếu đối chiếu từ feature 006 FR-051; yêu cầu đánh giá lại từ trạng thái hoặc dấu quá hạn do feature 001 xác định. *(Nguồn: 18.3 "(Bổ sung)"; BR-M01-02, BR-M07-09; feature 001, 006 FR-051)*

#### G. Báo cáo chi phí phát sinh (18.4)

- **FR-050**: Báo cáo chi phí MUST tổng hợp khoản chi phí của feature 010 theo người cao tuổi, theo kỳ (tháng dương lịch, Q-132), theo loại chi phí (danh sách chuẩn ở feature 010 FR-014a), theo dịch vụ, hoạt động, thuốc, vật phẩm; kèm số lượng và thành tiền. Báo cáo MUST ghi rõ "chi phí phát sinh của người cao tuổi, không phải báo cáo tài chính của viện" và MUST NOT có số đã thu, công nợ hay đặt cọc. *(Nguồn: 18.4; 1.2; feature 010 FR-042)*
- **FR-051**: Người xem MUST chọn một trong hai chế độ: (a) **đã chốt** — chỉ khoản thuộc bảng chi phí Đã chốt, là chế độ mặc định; (b) **tạm tính** — thêm các khoản chưa hủy của bảng chưa chốt theo đúng thành phần của feature 010 FR-038 (Q-136), nhãn "tạm tính, chưa chốt". Ở cả hai chế độ, báo cáo MUST nêu số người cao tuổi có bảng chưa chốt trong kỳ và số khoản "thiếu đơn giá" không được cộng. *(Nguồn: 15.6; feature 010 FR-038)*
- **FR-052**: Khoản điều chỉnh MUST được tính vào kỳ của bảng chứa nó, có cột kỳ gốc, để số của mỗi kỳ khớp bảng đã chốt và file kế toán; số của kỳ gốc đã chốt MUST NOT thay đổi. Báo cáo MUST nêu "số khoản điều chỉnh sau chốt" theo kỳ chứa và theo kỳ gốc. *(Nguồn: 18.4 "(Bổ sung)"; DBR-17; feature 010 FR-027, FR-028)*
- **FR-053**: Tỷ lệ chi phí tự sinh so với nhập tay MUST được nêu cả theo số khoản và theo số tiền; "tự sinh" là khoản có bản ghi nguồn, kể cả khoản loại Mua hộ (nguồn là đề nghị mua hộ, DBR-15); "nhập tay" chỉ là khoản loại "Khoản khác (nhập tay)"; khoản điều chỉnh tính theo nguồn sinh của nó ở feature 010. *(Nguồn: 18.4 "(Bổ sung)"; DBR-15)*
- **FR-054**: Tên thuốc, hàm lượng trong báo cáo "theo thuốc" MUST chỉ hiện với người có quyền xem thuốc của người cao tuổi đó; người khác thấy mã vật phẩm (Q-133). *(Nguồn: Q-133; 19.3)*
- **FR-055**: Ở chế độ "đã chốt", tổng của một kỳ MUST khớp tổng các bảng Đã chốt của kỳ đó và dòng tổng kiểm soát của file kế toán cùng phạm vi (feature 010 FR-041). *(Nguồn: 23; feature 010 FR-039, FR-041)*

#### H. Lọc, xuất và nhật ký

- **FR-060**: Báo cáo MUST cho chọn khoảng thời gian (hôm nay, ca hiện tại, tuần, tháng, khoảng tùy chọn) và lọc theo tầng/khu vực, loại lưu trú, mức chăm sóc, người cao tuổi, nhân viên, ca, trong giới hạn của FR-013. Khoảng tùy chọn MUST NOT dài hơn CFG-M14-01 (mặc định \[12 tháng\]); khi người xem chọn khoảng dài hơn, hệ thống MUST từ chối và nêu giới hạn hiện hành, không tự cắt khoảng. Giới hạn áp cho cả xem và xuất. *(Nguồn: 18.2 "theo nhân viên; theo ca; theo tầng"; Clarification 2026-09-27, tham số mới CFG-M14-01, Q-203)*
- **FR-061**: Mọi người có quyền xem một báo cáo MUST xuất được báo cáo đó ra file; vai trò không có quyền xem báo cáo MUST NOT xuất. Nội dung được xuất phụ thuộc vai trò:
  - **Thông tin định danh người cao tuổi** là: họ tên, mã người cao tuổi, phòng/giường, ngày sinh, số CCCD, ảnh (5.1), và mọi dòng tổng hợp mà nhóm chỉ có đúng một người cao tuổi.
  - **Quản lý viện**: xuất được số liệu tổng hợp và danh sách chi tiết có định danh người cao tuổi. Trước mỗi lần xuất có định danh, Quản lý viện MUST nhập mục đích xuất; không có mục đích thì không xuất được. *(Clarification 2026-09-27, đề xuất Q-198)*
  - **Vai trò khác**: chỉ xuất số liệu tổng hợp; file MUST NOT có dòng hay cột nào chứa thông tin định danh người cao tuổi.
  - **Định danh nhân viên** (họ tên nhân viên trong chỉ tiêu theo nhân viên, ghi nhận muộn, kiểm tra chất lượng, nhân sự) được xuất theo đúng quyền xem của người xuất; riêng số giờ làm theo từng nhân viên (FR-073) chỉ Quản lý viện được xuất. *(Clarification 2026-09-27, đề xuất Q-197)* Vì vậy các báo cáo theo từng người cao tuổi (ví dụ chi phí theo người, tỷ lệ liều theo người, danh sách nguy cơ cô lập, trường hợp cần theo dõi) không xuất được với các vai trò này; họ vẫn xem được trên hệ thống. Dữ liệu chi phí theo người cho kế toán đi qua file của feature 010 (UC-64).

  File MUST có đúng phạm vi (FR-012, FR-013), bộ lọc, giới hạn trường (FR-014) và cùng số liệu như màn hình tại thời điểm xuất; MUST ghi người xuất, "tính đến" thời điểm lấy số, bộ lọc và dòng tổng. Hệ thống MUST NOT sửa file đã xuất khi dữ liệu nguồn đổi sau đó; muốn số mới thì xuất lại. Dashboard không có lệnh xuất; muốn xuất thì dùng báo cáo tương ứng. Định dạng file do giai đoạn thiết kế chọn. *(Nguồn: Clarification 2026-09-27, đề xuất Q-193; 19.3; NFR-08)*
- **FR-062**: Mỗi lần xuất MUST được ghi lại thành bản ghi chỉ ghi thêm: người xuất, vai trò, thời điểm, báo cáo, bộ lọc, khoảng, phạm vi, chế độ (với chi phí), số dòng, dấu "có định danh" và mục đích xuất khi file có thông tin định danh người cao tuổi. Quản lý viện xem được mọi lần xuất; người xuất xem được lần xuất của mình. Bản ghi lần xuất MUST được lưu giữ theo CFG-M15-04 (mặc định \[10 năm\]) như nhật ký hệ thống và chỉ được loại bỏ theo NFR-07. Việc xem dashboard và báo cáo trên hệ thống (không xuất) MUST NOT tạo bản ghi nghiệp vụ. *(Nguồn: 19.4 "người thực hiện; thời gian; hành động; đối tượng"; NFR-07; Q-193, Q-198)*

#### I. Báo cáo nhân sự và ca trực

- **FR-070**: Báo cáo MUST nêu, cho từng ca đã diễn ra trong khoảng: tỷ lệ phục vụ theo khu, số nhân viên được tính, tổng trọng số và tình trạng đạt/không đạt theo lần tính cuối cùng trước khi ca kết thúc do feature 008 lưu (BR-M09-02, Q-79, CFG-M09-01), kèm dấu "từng không đạt" nếu có bất kỳ lần tính nào trong thời gian ca cho kết quả không đạt; tổng hợp theo tầng, mẫu ca, tháng: số ca không đạt, đếm theo dấu "từng không đạt". *(Nguồn: 18.2 "theo ca", 18.5; feature 008 FR-037; Clarification 2026-09-27, đề xuất Q-200)*
- **FR-071**: Báo cáo MUST nêu theo tầng, vai trò của dòng phủ, tháng: số ca thiếu phủ (CFG-M09-07, mặc định \[≥ 1 điều dưỡng / tầng / ca\]), tổng thời gian ca chạy thiếu phủ, số mục "thiếu ca" đã công bố (Q-189). Ca toàn viện được nêu riêng, chỉ với Quản lý viện. *(Nguồn: BR-M09-11; feature 015 FR-042)*
- **FR-072**: Báo cáo MUST nêu số yêu cầu đổi ca, yêu cầu nghỉ đột xuất theo trạng thái cuối (của feature 015) và thời gian trung bình từ lập tới quyết định; số lời mời nhận ca thay và số được nhận. Lý do nghỉ, lý do đổi ca, danh sách gợi ý người thay MUST NOT hiện trong báo cáo, danh sách chi tiết hay file xuất; số liệu MUST NOT được chia theo loại nghỉ hay lý do nghỉ, để không lộ gián tiếp thông tin sức khỏe của nhân viên. *(Nguồn: BR-M09-10, BR-M09-11; 19.3 "(Bổ sung, spec 015)"; feature 015 FR-042)*
- **FR-073**: Báo cáo MUST nêu phân bố số giờ làm trong tháng theo nhân viên: giờ đã làm và giờ đã xếp còn lại tách riêng (Q-180); số lần vượt số giờ làm liên tục CFG-M09-02 (mặc định \[16 giờ\]). Chỉ Quản lý viện và trưởng tầng được giao xem được chỉ tiêu này. Căn cứ: 19.3 "(Bổ sung, spec 015)" chỉ cho người nhận cảnh báo thiếu phủ và Quản lý viện xem số giờ làm của người khác, và trưởng tầng được giao là người nhận cố định cảnh báo thiếu phủ của tầng mình (Q-178, BR-M09-11). Người phụ trách ca không phải trưởng tầng MUST NOT xem. Trưởng tầng chỉ thấy nhân viên có phân công tại tầng mình; giờ ở ca của tầng khác của cùng nhân viên chỉ hiện tổng, không hiện chi tiết ca; giờ ở ca toàn viện chỉ Quản lý viện xem chi tiết. Chỉ Quản lý viện được xuất chỉ tiêu này ra file (FR-061). *(Nguồn: BR-M09-03, BR-M09-11; Q-178, Q-180; 19.3; feature 015 FR-042)*

### Giao tiếp với feature khác

Feature này chỉ **nhận** dữ liệu; không gửi yêu cầu nào tới feature khác.

| Hướng | Feature | Nội dung | Dữ liệu tối thiểu |
| --- | --- | --- | --- |
| Nhận | 000 | Giá trị hiện hành sau đính chính; tham số hiện hành | Bản ghi, bản đính chính, giá trị tham số |
| Nhận | 002 | Phạm vi hiện hành và giới hạn trường của người xem; kết quả "không xác định được quyền" | Tài khoản, vai trò, phạm vi, nhóm trường được xem |
| Nhận | 001 | Trạng thái người cao tuổi và lịch sử chuyển; mức chăm sóc theo phiên bản; yêu cầu đánh giá lại, hạn và tình trạng quá hạn do 001 xác định (FR-044) | Người cao tuổi, trạng thái, thời điểm, mức, hạn |
| Nhận | 003 | Trạng thái giường, phòng cách ly theo thời gian; công việc vệ sinh; thời gian Chờ vệ sinh | Giường, phòng, tầng, trạng thái, thời điểm |
| Nhận | 004 | Loại lưu trú theo hợp đồng hiệu lực; danh sách chờ; lượt tạm vắng; điều trị tại bệnh viện | Hồ sơ chờ, trạng thái, loại lưu trú, thời điểm |
| Nhận | 005 | Công việc, trạng thái, mức quan trọng, người thực hiện, người đóng, nhãn ghi nhận muộn; trạng thái có mặt bán trú theo ngày | Công việc, tầng/khu, thời điểm dự kiến, trạng thái |
| Nhận | 006 | Liều và trạng thái cuối; phiếu đối chiếu quá hạn (feature 006 FR-051) | Liều, người cao tuổi, ca, trạng thái |
| Nhận | 007 | Chỉ số, cảnh báo, sự cố, leo thang của cảnh báo và của sự cố, thời điểm tiếp nhận, theo dõi, khoanh vùng (feature 007 FR-081); dấu "không thuộc sức khỏe" của từng loại sự cố trên danh mục loại sự cố (FR-042a) | Như FR-081 của 007; loại sự cố, dấu |
| Nhận | 008 | Ca, lịch đã công bố, tỷ lệ phục vụ theo lần tính (feature 008 FR-037), bàn giao chưa xác nhận, giấy phép và chứng chỉ nhân viên; **lịch sử** để tính phạm vi theo thời gian (FR-012): phân công theo ca, giao trưởng tầng (kể cả giao tạm có thời hạn), bổ sung và gỡ nhân viên khỏi ca, vắng ca, người phụ trách ca | Ca, tầng/khu, nhân viên, vai trò trong ca, thời điểm bắt đầu/kết thúc, tỷ lệ, tình trạng |
| Nhận | 010 | Khoản chi phí, loại, nguồn sinh, bảng, kỳ, điều chỉnh sau chốt (bảng giao tiếp của 010) | Như bảng giao tiếp của 010 |
| Nhận | 011 | Phiếu bữa ăn giao trễ, có sai lệch (FR-034); số suất theo bữa, chế độ ăn không dùng ở giai đoạn này | Phiếu, bữa, giờ bữa, tầng/khu, thời điểm Đã giao, lịch sử trạng thái Có sai lệch |
| Nhận | 014 | Tỷ lệ tham gia, "Không ghi nhận", nguy cơ cô lập, kiểm tra chất lượng, chuyến quá giờ, sự cố thiếu người (feature 014 FR-058, FR-065, FR-070) | Như các FR đó |
| Nhận | 015 | Lần tính phủ, ca thiếu phủ, mục "thiếu ca" đã công bố, yêu cầu đổi ca, nghỉ đột xuất, lời mời, giờ làm (feature 015 FR-042) | Ca, tầng/khu, vai trò, trạng thái, thời điểm lập, quyết định |
| — | 009, 012, 013 | Không nhận ở giai đoạn này (Q-194); số liệu các spec này đã hứa cung cấp để giai đoạn sau | — |

### Key Entities *(include if feature involves data)*

Feature này không sở hữu dữ liệu nghiệp vụ nhóm 1, 2, 3. Các khái niệm dùng trong spec:

- **Chỉ tiêu** – định nghĩa cố định trong spec (FR-020 → FR-073): tên, đối tượng đếm, điều kiện, mốc thời gian xếp khoảng, tử số, mẫu số, nhóm chỉ tiêu (để áp FR-010), feature nguồn. Không phải danh mục cấu hình.
- **Phạm vi xem** – kết quả do feature 002 tính cho người xem tại thời điểm lấy số liệu; không lưu.
- **Lần xuất báo cáo** – nhóm 3, chỉ ghi thêm (FR-062): người xuất, vai trò, thời điểm, báo cáo, bộ lọc, khoảng, phạm vi, chế độ, số dòng, dấu "có định danh", mục đích xuất (bắt buộc khi có định danh). Không có sửa, xóa, đính chính; lưu giữ theo CFG-M15-04.
- Dùng từ feature khác: **Người cao tuổi**, **Giường**, **Hồ sơ chờ**, **Công việc**, **Liều thuốc**, **Chỉ số**, **Cảnh báo**, **Sự cố**, **Ca trực**, **Bàn giao**, **Buổi hoạt động**, **Kiểm tra chất lượng**, **Phiếu bữa ăn**, **Khoản chi phí**, **Bảng chi phí**.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Trên bộ kiểm thử có số liệu tính tay cho mọi chỉ tiêu ở FR-020 → FR-073, 100% chỉ tiêu cho kết quả khớp tính tay, kể cả trường hợp mẫu số bằng 0.
- **SC-002**: Với quy mô NFR-01 (300 người cao tuổi, 60 người dùng đồng thời), dashboard mở xong trong không quá 3 giây ở 95% lần mở; thay đổi ở dữ liệu nguồn xuất hiện trên dashboard đang mở trong không quá 1 phút.
- **SC-003**: Báo cáo một tháng cho toàn viện hiện kết quả trong không quá 10 giây, báo cáo có độ dài bằng CFG-M14-01 (mặc định \[12 tháng\]) trong không quá 30 giây, ở 95% lần xem; 100% yêu cầu khoảng dài hơn CFG-M14-01 bị từ chối.
- **SC-004**: Với bộ 11 tài khoản kiểm thử quyền ở User Story 2 (gồm Người phụ trách ca, trưởng tầng giao tạm, trưởng tầng đã thôi giao, tài khoản bác sĩ kiêm trưởng tầng, tài khoản hành chính kiêm điều dưỡng), 0 chỉ tiêu, dòng chi tiết, giá trị lọc hay tổng nào lộ bản ghi ngoài phạm vi hoặc trường bị giới hạn; 100% yêu cầu mở báo cáo của vai trò "—" bị từ chối.
- **SC-005**: 0 lần tên thuốc hoặc hàm lượng xuất hiện với Hành chính trên báo cáo chi phí, danh sách chi tiết hoặc file xuất (Q-133).
- **SC-006**: Với 20 chỉ tiêu chọn ngẫu nhiên, 100% cặp (dashboard, báo cáo) cùng phạm vi, cùng thời điểm cho cùng số; 100% danh sách chi tiết có số dòng khớp con số.
- **SC-007**: Ở chế độ "đã chốt", tổng chi phí của mỗi kỳ khớp 100% với tổng các bảng Đã chốt và dòng tổng kiểm soát của file kế toán cùng phạm vi.
- **SC-008**: Sau khi chạy toàn bộ bộ kiểm thử báo cáo và dashboard, số bản ghi nghiệp vụ và số thông báo trong hệ thống không đổi (0 bản ghi tạo, sửa, đổi trạng thái); chỉ số bản ghi lần xuất tăng đúng bằng số lần xuất.
- **SC-009**: Trong đợt dùng thử, Quản lý viện và trưởng tầng trả lời được 5 câu hỏi tình hình trong ngày (số người đang lưu trú, việc quá hạn, cảnh báo chưa tiếp nhận, giường trống, bàn giao chưa xác nhận) chỉ từ dashboard, không phải mở màn hình nghiệp vụ, với tỷ lệ trả lời đúng ít nhất 90%.
- **SC-010**: 100% file xuất trong bộ kiểm thử có cùng số liệu và cùng tổng với màn hình tại thời điểm xuất; 0 file chứa bản ghi ngoài phạm vi, trường bị giới hạn, tên thuốc với Hành chính, hay lý do nghỉ, lý do đổi ca; 0 file do vai trò khác Quản lý viện xuất chứa thông tin định danh người cao tuổi.
- **SC-011**: Với bộ kiểm thử phạm vi theo thời gian (trưởng tầng nhận giao, thôi giao giữa kỳ; điều dưỡng có 2 ca trong tháng; người cao tuổi chuyển tầng), 100% số liệu khớp quy tắc FR-012, kể cả Người phụ trách ca và ca có bổ sung, gỡ, vắng, nghỉ có duyệt.
- **SC-012**: 0 lần Hành chính thấy sự cố thuộc loại không mang dấu "không thuộc sức khỏe", hoặc thấy mô tả, diễn biến của sự cố mang dấu, trên dashboard, báo cáo, danh sách chi tiết hay file xuất, kể cả sau khi sự cố đổi loại hoặc dấu của loại bị đổi (FR-042a).
- **SC-013**: 100% lần xuất có thông tin định danh người cao tuổi do Quản lý viện thực hiện và có mục đích xuất; 0 file giờ làm theo nhân viên do vai trò khác Quản lý viện xuất (FR-061, FR-073).
- **SC-014**: Số giường theo từng trạng thái trên dashboard và báo cáo phân bố giường khớp 100% với trạng thái giường của feature 003 tại cùng thời điểm (tương ứng feature 003 SC-008).

## Assumptions

- Số feature `016` theo cột Feature của UC-67 ở mục 4.2 `docs/phan-tich-yeu-cau.md` và theo cách các spec 003, 007, 008, 009, 012, 015 đã gọi feature báo cáo.
- Điều dưỡng có dashboard và báo cáo theo phạm vi phân công vì 4.4 ghi "P" ở cột ĐD, dù UC-67 chỉ nêu Quản lý viện và Trưởng tầng. UC-67 nêu actor chính; ma trận là quyền tối đa (Q-15).
- Nhóm chỉ tiêu một vai trò được thấy là giao của quyền "Dashboard, báo cáo" và quyền ở **một dòng nguồn chính** (FR-010). Chọn một dòng nguồn chính thay vì hợp mọi dòng liên quan để quyền không bị mở rộng qua dòng thao tác (ví dụ Điều dưỡng "T" ở dòng "Nhận phiếu, báo sai lệch" chỉ để nhận phiếu, không để xem thống kê phiếu cả tầng). Cách hiểu này khớp feature 002 FR-035 và User Story 1 kịch bản 12 của 002 (Hành chính thấy số người, giường trống, tạm vắng, chi phí).
- Hành chính thấy "Khu đang khoanh vùng" vì dòng "Khoanh vùng lây nhiễm" cho HC "X", feature 007 đã thông báo khoanh vùng cho Hành chính (bảng trạng thái khoanh vùng của 007), và nội dung chỉ gồm khu, thời điểm, số người; không gồm lý do khoanh vùng hay sự cố gắn kèm.
- Không che nhóm nhỏ ngoài ngưỡng "đúng một người" ở FR-061: quy mô viện tối đa 300 người, người xuất đã có quyền xem số liệu đó trên hệ thống, và dữ liệu định danh đầy đủ chỉ Quản lý viện xuất được.
- Không ghi nhật ký lần xem dashboard, báo cáo của nhân viên (FR-062): NFR-08 chỉ đòi ghi lần xem hồ sơ sức khỏe của người thân; lần xuất đã được ghi. Nếu Q-03 (căn cứ theo Nghị định 13/2023/NĐ-CP) đòi ghi truy cập dữ liệu sức khỏe của nhân viên, yêu cầu này phải được bổ sung.
- Dashboard cập nhật trong vòng 1 phút là đủ cho nhu cầu "số liệu trong ngày"; thông báo tức thời (khẩn cấp) do feature 009 đảm nhận, không qua dashboard.
- Báo cáo được tính từ dữ liệu nguồn tại lúc xem; không lưu bản chụp số liệu. Dữ liệu lịch sử cần cho báo cáo tại một ngày đã qua (lịch sử trạng thái, phiên bản) đã được các feature nguồn lưu theo 1.5 và 19.4.
- Chế độ mặc định của báo cáo chi phí là "đã chốt" để khớp số gửi người thân và kế toán; khoản điều chỉnh xếp theo kỳ chứa nó để không làm đổi số của kỳ đã chốt (DBR-17).
- Chỉ đề xuất một tham số mới: CFG-M14-01 "Độ dài tối đa của khoảng thời gian một lần xem, xuất báo cáo", mặc định \[12 tháng\] (FR-060). Các mốc "sắp hết hạn" và "quá hạn" dùng CFG hiện có.
- Hiệu năng ở SC-002, SC-003 là mục tiêu nghiệm thu cho quy mô NFR-01, không phải tham số.

## Điểm cần báo lại về tài liệu nguồn

**Trạng thái (2026-09-27):** theo yêu cầu của người dùng, các điểm dưới đây đã được đưa vào tài liệu nguồn và spec liên quan, trừ phần "Còn mở".

**Đã phản ánh**
- `docs/nghiep-vu.md`: 9.2 (dấu "không thuộc sức khỏe", Q-202); 13.4 (lưu lịch sử phân công); 18.2 → 18.5 (làm rõ định nghĩa); 18.6 mới (báo cáo nhân sự và ca trực); 18.7 mới (quy tắc chung của báo cáo); 19.3 (quyền dashboard, báo cáo; chú thích ¹¹; người nhận cảnh báo thiếu phủ); 19.4 (bản ghi lần xuất); 24.2 (Q-192 → Q-203); 24.3; Phụ lục 25 (CFG-M14-01).
- Spec 001 FR-041 (dấu "quá hạn" của yêu cầu đánh giá lại); spec 007 FR-042, FR-081; spec 008 FR-037, FR-037a mới, bảng giao tiếp; spec 009, 011, 012, 013 (bảng giao tiếp "giai đoạn sau"). Mỗi spec có mục "Cập nhật 2026-09-27 (đồng bộ với spec 016)".
- Bốn quyết định của phiên clarify được gắn mã: danh sách chi tiết ngoài phạm vi → Q-196; xuất có định danh → Q-193; sự cố "không thuộc sức khỏe" → Q-202; CFG-M14-01 → Q-203.

**Còn mở**
- Điểm 1, 8: tab Phân tích yêu cầu (UC-67 actor, chú thích ¹¹ của 4.4) không được sửa theo quyết định của người dùng; 19.3 là căn cứ khi 4.4 khác.
- Q-03 (Nghị định 13/2023) còn mở ở 24.1; nếu đòi ghi nhật ký truy cập dữ liệu sức khỏe của nhân viên, FR-062 phải bổ sung.

Nội dung gốc của từng điểm được giữ dưới đây để truy vết.

1. **UC-67** nêu actor là Quản lý viện, Trưởng tầng; dòng "Dashboard, báo cáo" của 4.4 còn cho Bác sĩ, Điều dưỡng, Hành chính quyền "P". Spec theo ma trận. Có thể bổ sung actor cho UC-67 khi sửa tab Phân tích yêu cầu (người dùng đã quyết định không sửa tab này).
2. **Mục 18** cần bổ sung nhóm "Báo cáo nhân sự và ca trực" (FR-070 → FR-073, Q-194), phạm vi theo thời gian của báo cáo (FR-012, Q-192) và việc xuất báo cáo có ghi lại (FR-061, FR-062, Q-193).
3. **Bảng giao tiếp của spec 009, 012, 013** ghi "cung cấp cho feature 016"; theo Q-194 cần ghi thêm "giai đoạn sau".
4. **Spec 007** (danh mục loại sự cố, FR-042 của 007) cần thêm dấu "không thuộc sức khỏe" cho mỗi loại, do Quản lý viện đặt, mặc định bật cho đồ gửi thất lạc, đồ gửi hư hỏng, cơ sở vật chất (FR-042a của spec này). 19.3 cần ghi Hành chính được xem số liệu sự cố của các loại này.
5. **Spec 001** cần xác nhận có trạng thái hoặc dấu "quá hạn" cho yêu cầu đánh giá lại (CFG-M01-03), để FR-044 lấy thẳng thay vì tự tính (FR-002). **Spec 007** FR-081 cần thêm leo thang của sự cố và thời điểm tiếp nhận xử lý sự cố (Q-199). **Spec 011** bảng giao tiếp: "số suất theo bữa, chế độ ăn" gửi Module 14 cần ghi "giai đoạn sau".
6. **Phụ lục 25** cần thêm CFG-M14-01 "Độ dài tối đa của khoảng thời gian một lần xem, xuất báo cáo", mặc định \[12 tháng\], dùng tại 18 (FR-060).
7. **Mục 19.3 "(Bổ sung, spec 015)"** nên ghi rõ "người nhận cảnh báo thiếu phủ" gồm trưởng tầng được giao của tầng (người nhận cố định, Q-178), để khớp FR-073.
8. **Chú thích ¹¹ của 4.4** ghi "P¹¹" cho cả Bác sĩ và Hành chính với câu "vẫn áp giới hạn trường của ⁵", trong khi ⁵ là giới hạn của Hành chính. Nên ghi rõ giới hạn trường chỉ áp cho Hành chính (FR-014).
9. **Spec 008** cần xác nhận lưu đủ lịch sử phân công theo ca, giao trưởng tầng (kể cả giao tạm), bổ sung và gỡ nhân viên khỏi ca, người phụ trách ca theo thời gian, để feature này tính phạm vi theo FR-012.
10. **Đề xuất ghi vào mục 24.2** (chưa ghi vào tài liệu nguồn), chốt ngày 2026-09-27:

   | Mã | Vấn đề | Quyết định | Cần phản ánh tại |
   | --- | --- | --- | --- |
   | Q-192 | Phạm vi của trưởng tầng, điều dưỡng khi báo cáo khoảng thời gian đã qua | Trưởng tầng theo tầng đang được giao, gồm cả bản ghi trước khi được giao; điều dưỡng theo phân công trong các ca đã làm (giờ ca ± CFG-M15-07) | 18.5, 19.3 |
   | Q-193 | Có xuất báo cáo ra file không | Có; ai có quyền xem báo cáo thì xuất được, đúng phạm vi và giới hạn trường; mỗi lần xuất được ghi lại | 18, 19.4 |
   | Q-194 | Nhóm báo cáo ngoài 18.1 → 18.5 | Thêm "nhân sự và ca trực" từ dữ liệu feature 008, 015; thăm, phản hồi, đồ gửi, thông báo để giai đoạn sau | 18 |
   | Q-195 | Người phụ trách ca thấy gì trên dashboard, báo cáo | Cả tầng/khu của ca, nhóm chỉ tiêu của Điều dưỡng, trong giờ ca ± CFG-M15-07; không thêm nhóm của trưởng tầng khi tầng chưa có trưởng tầng | 2.4, 18.5 |
   | Q-196 | Mở bản ghi của người ngoài phạm vi từ danh sách chi tiết | Không mở hồ sơ hiện tại; mở bản ghi đầy đủ chỉ khi quyền hiện hành ở feature sở hữu cho phép | 18.5, 19.3 |
   | Q-197 | Tên nhân viên trong file xuất | Được theo quyền xem; số giờ làm theo nhân viên chỉ Quản lý viện xuất | 18, 19.3 |
   | Q-198 | Mục đích khi xuất danh sách có định danh người cao tuổi | Quản lý viện bắt buộc nhập mục đích; lưu vào bản ghi lần xuất | 18, 19.4 |
   | Q-199 | Có đếm leo thang của sự cố trong báo cáo sức khỏe | Có; số lần leo thang và thời gian tạo → tiếp nhận của sự cố theo mức, tính riêng với cảnh báo | 18.3 |
   | Q-200 | Tỷ lệ phục vụ của ca đã qua lấy lần tính nào | Lần tính cuối trước khi ca kết thúc, kèm dấu "từng không đạt"; số ca không đạt đếm theo dấu | 18.2, 18.5 |
   | Q-201 | Bản ghi ngoại tuyến "chờ xem lại" có được đếm | Có, theo giá trị đang ghi, nêu kèm số bản ghi chờ xem lại | 18, Q-01 |

   Bốn quyết định của phiên clarify (danh sách chi tiết của người ngoài phạm vi, xuất có định danh, sự cố "không thuộc sức khỏe", CFG-M14-01) chưa có mã Q riêng; khi ghi vào tài liệu nguồn, có thể gộp vào Q-192, Q-193 hoặc cấp mã mới (phản ánh tại 18, 19.3, 19.4, Phụ lục 25).
