# Feature Specification: Quản lý hoạt động và chất lượng chăm sóc

**Feature Branch**: `014-activity-care-quality`

**Created**: 2026-09-27

**Status**: Draft

**Input**: User description: "Quản lý hoạt động và chất lượng chăm sóc theo docs/nghiep-vu.md Module 04 mục 8.8–8.10 và BR-M04-15 đến BR-M04-18, BR-M04-21 đến BR-M04-23: hoạt động và hoạt động định kỳ tự sinh buổi; đăng ký có giới hạn và điều kiện sức khỏe; hoạt động ngoài viện với điểm danh rời/về, thuốc mang theo, sự cố khi thiếu người; theo dõi tinh thần và cảnh báo nguy cơ cô lập; kiểm tra chất lượng ngẫu nhiên công việc đã hoàn thành."

## Clarifications

### Session 2026-09-27

- Q: Ai được làm trưởng đoàn, người đi cùng và ai đánh giá khả năng tham gia chuyến đi? → A: Trưởng đoàn là Trưởng tầng hoặc Nhân viên chăm sóc; người đi cùng là nhân viên bất kỳ có ca chồng thời gian chuyến đi, kể cả Điều dưỡng; Trưởng tầng đánh giá khả năng tham gia dựa trên thông tin hệ thống hiển thị. Điều dưỡng đi cùng không có quyền lệnh của chuyến ngoài "Báo thiếu người" (đề xuất Q-161; người dùng chọn phương án A). Áp dụng tại FR-035, FR-036, FR-046, FR-067, bảng trạng thái người tham gia. *(Đã được thay một phần bởi Q-214 ngày 2026-09-28: Điều dưỡng được làm trưởng đoàn và có mọi lệnh của Nhân viên chăm sóc; xem Cập nhật 2026-09-28 bên dưới.)*
- Q: Cờ "không đủ điều kiện" (BR-M04-15) có phạm vi hay là cờ chung; điều dưỡng có được gắn tạm không? → A: Có phạm vi (mọi hoạt động / mọi hoạt động nhóm / ngoài viện / theo loại hoạt động), do Bác sĩ gắn, và là "chỉ định hạn chế" của BR-M04-22. Điều dưỡng được gắn chỉ định tạm, hiệu lực tối đa CFG-M04-14 (đề xuất, mặc định \[24 giờ\]); Bác sĩ xác nhận thành chính thức hoặc gỡ; quá hạn không xác nhận thì hết hiệu lực (đề xuất Q-162; người dùng chọn phương án C). Áp dụng tại FR-023 → FR-023c, bảng trạng thái chỉ định hạn chế.
- Q: Kiểm tra chất lượng chọn mẫu lúc nào và hạn kiểm tra bao lâu? → A: Chọn rải trong ca: mỗi công việc đủ điều kiện được xét chọn ngẫu nhiên ngay khi hoàn thành; hạn kiểm tra là hết chính ca đó; công việc làm lại thuộc ca đó (đề xuất Q-163; người dùng chọn phương án B). Tại mốc CFG-M09-04 trước khi kết ca, hệ thống chọn bổ sung nếu số đã chọn chưa đạt tỷ lệ CFG-M04-11 (FR-059a). Áp dụng tại FR-059 → FR-064.
- Q: Khi một khu bị khoanh vùng, buổi đã lên lịch trong khu và đăng ký sẵn có của người trong khu xử lý thế nào? → A: Tự hủy buổi trong viện có địa điểm trong vùng và mọi đăng ký chưa diễn ra của người có giường trong vùng, lý do "khoanh vùng", báo người phụ trách buổi và trưởng tầng; gỡ vùng không tự khôi phục, như Q-120 với lượt thăm (đề xuất Q-164; người dùng chọn phương án A). Áp dụng tại FR-021.
- Q: Người mang dấu "nghi nhiễm" có được đăng ký và tham gia hoạt động nhóm hoặc chuyến đi không? → A: Không. Hệ thống chặn đăng ký và thêm lúc điểm danh ở hoạt động nhóm và chuyến đi, tự hủy đăng ký nhóm đã có khi dấu được gắn; hoạt động cá nhân vẫn được, kèm cảnh báo; ngày mang dấu không tính cho cảnh báo cô lập (đề xuất Q-165; người dùng chọn phương án B). Áp dụng tại FR-015 (g), FR-018, FR-021a, FR-028, FR-054, thuật ngữ "Ngày tính".
- Q: Người thân có được đón thẳng người cao tuổi từ điểm đến của chuyến đi về nhà không? → A: Không. Người cao tuổi phải được điểm danh về viện trước, rồi mới Cho tạm vắng theo quy trình đón (14.3); giữ nguyên bảng 5.5 (đề xuất Q-166; người dùng chọn phương án A). Áp dụng tại FR-045.
- Q: Người bán trú có được đăng ký buổi hoặc chuyến đi kết thúc sau giờ về theo lịch không? → A: Được, khi Hành chính ghi nhận đồng ý về muộn của người đại diện cho đúng buổi đó; giờ về dự kiến của ngày tự dời tới giờ kết thúc buổi; feature 003, 005, 010, 011 nhận giờ mới (đề xuất Q-167; người dùng chọn phương án B). Áp dụng tại FR-016, FR-016a, Edge Cases "Bán trú".
- Q: Công việc do chính trưởng tầng thực hiện có vào mẫu kiểm tra chất lượng không; nếu có thì ai kiểm tra? → A: Không. Loại khỏi mẫu, không kiểm tra; báo cáo chất lượng ghi rõ công việc của trưởng tầng không thuộc phạm vi kiểm tra; không mở rộng quyền kiểm tra ngoài 4.4 (đề xuất Q-168; người dùng chọn phương án A). Áp dụng tại FR-060, FR-065.
- Q: Các mặc định spec tự đặt khi rà checklist business-rules, consistency (buổi thiếu điểm danh, đính chính điểm danh rời/về, thay đổi chuyến sau chuẩn bị, an toàn chuyến, tham gia và phí, phạm vi kiểm tra chất lượng, địa điểm và quyền trên hoạt động toàn viện, giờ về theo ngày của bán trú) có được giữ không? → A: Giữ nguyên toàn bộ theo mặc định đề xuất; chốt thành Q-169 (FR-014a), Q-170 (FR-031a), Q-171 (FR-015 (e), FR-038, FR-038a, FR-040), Q-172 (FR-035, FR-035a, FR-036, FR-049a), Q-173 (FR-029, FR-033, FR-054, FR-070), Q-174 (FR-059, FR-060, FR-064), Q-175 (FR-002a, FR-002b, FR-036), Q-176 (FR-016a) và đưa vào điểm báo lại 17.

### Cập nhật 2026-09-27 (đồng bộ với spec 015)

Spec 015 đã chốt (Q-185): đổi ca, nghỉ đột xuất vẫn được duyệt khi người rời ca là trưởng đoàn hoặc người đi cùng của chuyến đi chồng thời gian; người duyệt được cảnh báo trước, và khi áp dụng thì nhiệm vụ bị gỡ, Trưởng tầng phụ trách chuyến được báo để phân công lại. FR-035 của spec này được bổ sung tương ứng; chặn điểm danh rời viện khi chưa có trưởng đoàn giữ nguyên.

### Cập nhật 2026-09-28 (đồng bộ với spec 016, rà chéo)

Spec 016 cần cách xếp "theo tầng" cho hoạt động toàn viện và dữ liệu để tính mốc quá giờ về (CFG-M04-07, 2 × CFG-M04-07 theo Q-172). FR-070 được bổ sung: buổi xếp theo tầng địa điểm; lượt tham gia, tỷ lệ tham gia, sự cố thiếu người xếp theo tầng của người cao tuổi lúc buổi bắt đầu; chuyến đi xếp theo phạm vi của hoạt động. Dòng giao tiếp "Gửi Module 14" có đủ dữ liệu tối thiểu.

### Cập nhật 2026-09-28 (đồng bộ với góp ý nghiệp vụ Q-214)

Tài liệu nguồn (2.4, 8.8, 8.9, 19.3, Phụ lục 27 chú thích ²⁷, BF-15) đã chốt Q-214: Điều dưỡng có mọi quyền thực hiện của Nhân viên chăm sóc; chiều ngược lại không. Q-214 thay phần "trưởng đoàn là Trưởng tầng hoặc Nhân viên chăm sóc; Điều dưỡng đi cùng chỉ Báo thiếu người" của Q-161 và phần "người phụ trách buổi" của Q-175. Spec này được sửa:
- Điều dưỡng được phụ trách buổi (FR-002a), đăng ký, hủy đăng ký (FR-017), điểm danh (FR-027), làm trưởng đoàn và nhận chuyển trưởng đoàn (FR-035, FR-035a), trong phạm vi dữ liệu của mình.
- Giới hạn "chỉ Báo thiếu người" nay chỉ áp cho người đi cùng không có vai trò Trưởng tầng, Nhân viên chăm sóc hoặc Điều dưỡng (FR-035, FR-067).
- Các bảng trạng thái buổi, đăng ký, người tham gia chuyến, trưởng đoàn thêm Điều dưỡng ở cột người thực hiện; kịch bản User Story 1 (đoạn đăng ký), User Story 2 #7, User Story 3 #8, #10 được viết lại.
- Không đổi: đánh giá khả năng tham gia vẫn chỉ do Trưởng tầng (FR-036); ghi kết quả kiểm tra chất lượng vẫn chỉ do Trưởng tầng được giao (BR-M04-23); liều Mang theo vẫn chỉ do Điều dưỡng ghi (Q-54).

### Cập nhật 2026-09-29 (đồng bộ với spec 019)

Tài liệu nguồn (7.7, BR-M03-16, DBR-32, BF-15) đã chốt Q-231 và Q-236 về lịch xe của chuyến đi dùng xe của viện:
- Lịch xe chuyển Đang dùng khi người đầu tiên được điểm danh rời viện, Đã hoàn thành khi Kết thúc điểm danh về, Đã hủy khi chuyến bị hủy, dời theo khi chuyến dời (Q-231). Bảng trạng thái chuyến đi ghi các sự kiện gửi feature 019.
- Chuyến đang đi được gia hạn giờ về thì giờ về của lịch xe dời theo; chồng lịch kế tiếp của cùng xe không chặn gia hạn, Quản lý viện và người đặt lịch kế tiếp được báo (Q-236).
- FR-034 thêm phân biệt phương tiện "xe của viện" để áp FR-040; bảng giao tiếp thêm dòng feature 019.
- (Checklist cross-feature CHK047, CHK048) Dời chuyến bị chặn nếu lịch xe ở giờ mới chồng lịch khác của cùng xe (Q-239). Hủy ghi nhận bản ghi rời viện cuối cùng đưa chuyến về Đã lên lịch và lịch xe về Đã đặt (Q-240; FR-031a, bảng trạng thái chuyến đi).

## Phạm vi

**Trong phạm vi** (Module 04 mục 8.8, 8.9, 8.10; UC-29, UC-30, UC-31; BR-M04-15 → 18, BR-M04-21 → 23; phần gợi ý hoạt động của BR-M04-11):

1. Danh mục loại hoạt động và hoạt động: tên, loại, địa điểm, người phụ trách, số lượng tối đa, đối tượng, có thu phí (8.8).
2. Hoạt động định kỳ: mẫu lặp, tự sinh buổi trước CFG-M04-09, đăng ký sẵn người tham gia thường xuyên, thay đổi mẫu chỉ ảnh hưởng buổi chưa diễn ra (8.8 "(Bổ sung)", BR-M04-21).
3. Buổi hoạt động: tạo lẻ, dời, hủy; vòng đời buổi.
4. Đăng ký tham gia có giới hạn: chặn khi đủ số lượng, khi có chỉ định hạn chế của bác sĩ, khi khu đang bị khoanh vùng (BR-M04-15, BR-M05-11); gợi ý người tham gia.
5. Chỉ định hạn chế hoạt động (cờ "không đủ điều kiện" do bác sĩ gắn; điều dưỡng gắn tạm chờ bác sĩ xác nhận) (BR-M04-15, BR-M04-22, Q-162).
6. Điểm danh buổi: có mặt, vắng, mức độ tham gia, tình trạng sau; điểm danh hoạt động có thu phí tạo chi phí nháp (8.8, BR-M04-18).
7. Hoạt động ngoài viện: đánh giá khả năng tham gia, phân công trưởng đoàn và người đi cùng, kiểm tra thuốc cần mang, điểm danh rời viện, theo dõi danh sách, điểm danh về, quá giờ về, thiếu người thành sự cố khẩn cấp (8.9, BR-M04-16, BR-M04-17).
8. Theo dõi tinh thần: sở thích, mức độ tham gia, giao tiếp; tổng hợp tinh thần theo người; gợi ý hoạt động; cảnh báo nguy cơ cô lập (8.10, BR-M04-11 phần gợi ý, BR-M04-22).
9. Kiểm tra chất lượng ngẫu nhiên công việc đã Hoàn thành, kể cả công việc vệ sinh; công việc làm lại khi Không đạt (BR-M04-23).
10. Các thông báo của nghiệp vụ trên, gửi qua feature 009; dữ liệu cho báo cáo chăm sóc 18.2.

**Ngoài phạm vi** (spec này **nhận** dữ liệu hoặc **cung cấp** dữ liệu cho feature sở hữu):

- Quy tắc dùng chung (nhóm dữ liệu, lý do bắt buộc, đính chính, yêu cầu phê duyệt, nhật ký, tham số): feature 000. Spec này kế thừa và không lặp lại.
- Trạng thái người cao tuổi và bảng chuyển 5.5, cờ nguy cơ, mức chăm sóc: feature 001. Spec này chỉ kích hoạt chuyển Đang lưu trú ↔ Hoạt động bên ngoài qua điểm danh (feature 001 bảng trạng thái).
- Lượt vắng, Tạm vắng, Chuyển viện, Ghi nhận qua đời: feature 004 (và 007 với chuyển viện từ sự cố khẩn cấp).
- Sinh, hủy, sinh lại công việc "hoạt động" và công việc trong khoảng vắng; ghi nhận tâm trạng, hành vi bất thường và phát hiện chuỗi tâm trạng tiêu cực (BR-M04-04, BR-M04-11 phần phát hiện): feature 005.
- Liều Mang theo, lần giao thuốc mang theo, ghi nhận liều khi đi, nhận lại thuốc (BR-M07-03, Q-54, Q-58, Q-60, Q-65): feature 006. Spec này chỉ cung cấp khoảng đi, thời điểm rời/về và chặn rời viện khi chưa xử lý thuốc (FR-038).
- Tạo, xử lý, đóng cảnh báo và sự cố; khoanh vùng, danh sách tiếp xúc: feature 007.
- Ca trực, phân công, bàn giao ca: feature 008. Tài khoản, phạm vi dữ liệu, nhiệm vụ Trưởng đoàn: feature 002.
- Gửi thông báo: feature 009. Tạo, tính, chốt chi phí: feature 010. Suất ăn khi đi ngoài viện: feature 011. Cổng người thân và bản tin: feature 012.
- Báo cáo, dashboard: Module 14; spec này chỉ cung cấp dữ liệu nguồn.
- Kế hoạch phục hồi chức năng chuyên sâu, trị liệu có chỉ định y khoa: công việc "phục hồi" trong kế hoạch chăm sóc (feature 005), không phải hoạt động.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Trưởng tầng khai báo hoạt động định kỳ, hệ thống tự sinh buổi (Priority: P1)

Trưởng tầng khai báo một hoạt động với tên, loại, địa điểm, người phụ trách, số lượng tối đa, đối tượng, có thu phí hay không. Với hoạt động lặp lại, trưởng tầng khai báo mẫu lặp (ví dụ thể dục 08:00 thứ 2 đến thứ 6) và danh sách người tham gia thường xuyên. Hệ thống tự sinh từng buổi trước CFG-M04-09 và đăng ký sẵn người tham gia thường xuyên đủ điều kiện. Khi mẫu thay đổi, chỉ các buổi chưa diễn ra được sinh lại; buổi đã điểm danh giữ nguyên.

**Why this priority**: Hoạt động định kỳ chiếm phần lớn đời sống sinh hoạt hằng ngày; không tự sinh buổi thì nhân viên phải lập lịch tay mỗi tuần và công việc "hoạt động" của feature 005 không có nguồn.

**Independent Test**: Khai báo "Thể dục buổi sáng" 08:00 thứ 2 → thứ 6, tối đa 20 người, 3 người tham gia thường xuyên; chạy Bộ lập lịch; kiểm tra buổi được sinh đủ 7 ngày tới và có đăng ký sẵn; đổi giờ mẫu sang 08:30 từ thứ 4; kiểm tra buổi đã điểm danh không đổi.

**Acceptance Scenarios**:

1. **Given** hôm nay thứ 2 ngày 05/10/2026, trưởng tầng T của tầng 2 khai báo hoạt động "Thể dục buổi sáng" loại Vận động, địa điểm "Sân tầng 2", người phụ trách S, tối đa 20 người, không thu phí, mẫu lặp 08:00–08:45 thứ 2 → thứ 6, người tham gia thường xuyên A, B, C, **When** lưu, **Then** hệ thống sinh các buổi từ 05/10 tới hết ngày 11/10 (CFG-M04-09 mặc định \[7 ngày\]), mỗi buổi có đăng ký sẵn của A, B, C (BR-M04-21, 8.8).
2. **Given** các buổi đã sinh tới 11/10, **When** Bộ lập lịch chạy ngày 06/10, **Then** hệ thống sinh thêm buổi của ngày 12/10; chạy lại cùng ngày không tạo buổi trùng (FR-007).
3. **Given** C có chỉ định hạn chế "không tham gia hoạt động vận động" từ 07/10, **When** Bộ lập lịch sinh buổi ngày 07/10 trở đi, **Then** C không được đăng ký sẵn; T nhận danh sách "người thường xuyên không được đăng ký" kèm lý do (FR-008).
4. **Given** buổi thứ 2 ngày 05/10 đã được điểm danh, **When** T đổi mẫu thành 08:30–09:15 áp dụng từ 05/10 kèm lý do, **Then** buổi 05/10 giữ nguyên 08:00; các buổi chưa diễn ra (06/10 → 12/10) bị hủy với lý do "đổi mẫu" và được sinh lại lúc 08:30, người tham gia thường xuyên được đăng ký sẵn lại; đăng ký thêm tay trên các buổi bị hủy được báo cho người đã đăng ký (BR-M04-21, FR-009).
5. **Given** T ngừng hiệu lực hoạt động "Thể dục buổi sáng" từ 10/10, **When** lưu, **Then** các buổi từ 10/10 chưa có điểm danh chuyển Đã hủy; buổi đã diễn ra và lịch sử điểm danh giữ nguyên; hoạt động không bị xóa (1.5 nhóm 1).
6. **Given** T tạo một buổi lẻ "Giao lưu văn nghệ" 15:00 ngày 09/10 cho toàn viện, **When** lưu, **Then** buổi ở Đã lên lịch, chưa có đăng ký; hiển thị trong lịch hoạt động của mọi tầng.
7. **Given** nhân viên chăm sóc S, **When** S tìm cách tạo hoặc sửa hoạt động, mẫu lặp, **Then** hệ thống không cho phép (4.4 dòng "Hoạt động, ngoài viện": Nhân viên chăm sóc chỉ điểm danh, đăng ký).

---

### User Story 2 - Đăng ký có giới hạn số lượng và điều kiện sức khỏe (Priority: P1)

Trưởng tầng, nhân viên chăm sóc hoặc điều dưỡng (Q-214) đăng ký người cao tuổi vào buổi hoạt động. Hệ thống chặn đăng ký khi buổi đã đủ số lượng tối đa, khi người cao tuổi có chỉ định hạn chế do bác sĩ gắn bao trùm hoạt động đó, hoặc khi người cao tuổi hay địa điểm của buổi đang trong khu bị khoanh vùng. Hệ thống gợi ý người tham gia theo sở thích, mức chăm sóc và điều kiện sức khỏe.

**Why this priority**: BR-M04-15 là quy tắc an toàn: đưa người có chỉ định hạn chế vào hoạt động hoặc tụ tập trong vùng khoanh là rủi ro trực tiếp cho sức khỏe và kiểm soát lây nhiễm.

**Independent Test**: Buổi tối đa 2 người; đăng ký A, B rồi C (bị chặn); bác sĩ gắn chỉ định hạn chế cho D rồi đăng ký D (bị chặn); khoanh vùng tầng 3 rồi đăng ký E ở tầng 3 (bị chặn); kiểm tra đăng ký đã có của người bị gắn chỉ định hoặc bị khoanh vùng bị hủy và được báo.

**Acceptance Scenarios**:

1. **Given** buổi "Đố vui" 15:00 ngày 06/10 tối đa 2 người, đã có A, B đăng ký, **When** S đăng ký C, **Then** hệ thống chặn vì đủ số lượng tối đa (BR-M04-15).
2. **Given** A hủy đăng ký trước giờ bắt đầu, **When** S đăng ký C, **Then** được chấp nhận; feature 005 nhận sự kiện hủy đăng ký của A và đăng ký của C để hủy, sinh công việc "hoạt động" (feature 005 FR-017).
3. **Given** bác sĩ gắn cho D chỉ định hạn chế phạm vi "mọi hoạt động nhóm" từ 06/10 tới 20/10 kèm lý do "sau phẫu thuật", **When** S đăng ký D vào "Đố vui" 06/10, **Then** hệ thống chặn và hiển thị chỉ định (không hiển thị chẩn đoán với người không có quyền xem sức khỏe) (BR-M04-15, FR-024).
4. **Given** D đã có đăng ký ở ba buổi trong khoảng 06/10 → 20/10, **When** bác sĩ gắn chỉ định hạn chế trên, **Then** ba đăng ký đó tự chuyển Đã hủy với lý do "chỉ định hạn chế"; T và người phụ trách của từng buổi được báo (FR-025).
5. **Given** tầng 3 đang bị khoanh vùng (feature 007), **When** S đăng ký E (giường ở tầng 3) vào buổi ở "Sân chung tầng 1", **Then** hệ thống chặn; **When** T đăng ký F (tầng 2) vào buổi có địa điểm thuộc tầng 3, **Then** hệ thống cũng chặn (BR-M04-15, BR-M05-11).
6. **Given** E có đăng ký buổi 10/10 và buổi "Hát karaoke" diễn ra tại tầng 3 ngày 11/10, **When** tầng 3 bị khoanh vùng ngày 08/10, **Then** đăng ký của E chuyển Đã hủy và buổi 11/10 chuyển Đã hủy, lý do "khoanh vùng"; người phụ trách, T được báo; **When** vùng được gỡ ngày 09/10, **Then** đăng ký và buổi không tự khôi phục (FR-021, theo cách của Q-120).
7. **Given** G có cờ nguy cơ ngã và mức chăm sóc cao hơn đối tượng của buổi "Đi bộ nhanh", **When** S đăng ký G, **Then** hệ thống hiển thị cảnh báo và yêu cầu S xác nhận; đăng ký vẫn được lưu kèm dấu "đã xác nhận cảnh báo" (FR-018).
8. **Given** T mở buổi "Vẽ tranh" 09:00 ngày 07/10 còn 6 chỗ, **When** T chọn "Gợi ý người tham gia", **Then** hệ thống liệt kê người trong phạm vi của T có sở thích khớp loại hoạt động, mức chăm sóc thuộc đối tượng, không có chỉ định hạn chế bao trùm, không trùng giờ đăng ký khác, sắp theo người lâu chưa tham gia hoạt động nhóm nhất (FR-020).
9. **Given** A đã đăng ký "Đố vui" 15:00–16:00 ngày 06/10, **When** S đăng ký A vào "Xem phim" 15:30 cùng ngày, **Then** hệ thống chặn vì trùng giờ (FR-016).
10. **Given** người thân của A trên cổng, **When** xem lịch sinh hoạt của A, **Then** thấy các buổi A đã đăng ký; không có thao tác đăng ký (4.4 cột NT: X).
11. **Given** 21:00, H sốt 38,5 °C và không có bác sĩ tại viện, **When** điều dưỡng D1 gắn chỉ định tạm phạm vi "mọi hoạt động" cho H tới 21:00 hôm sau kèm lý do, **Then** chỉ định có hiệu lực ngay, đăng ký của H trong khoảng đó bị hủy như kịch bản 4, bác sĩ trực được báo; **When** D1 đặt thời điểm kết thúc sau 48 giờ, **Then** hệ thống chặn vì vượt CFG-M04-14 (mặc định \[24 giờ\]) (FR-023a, Q-162).
12. **Given** chỉ định tạm của H, **When** bác sĩ xác nhận và đổi phạm vi thành "hoạt động ngoài viện" tới 30/10 kèm lý do, **Then** chỉ định chuyển Hiệu lực với phạm vi mới; **When** thay vào đó tới 21:00 hôm sau không bác sĩ nào xác nhận, **Then** chỉ định tự Hết hiệu lực, D1 và trưởng tầng được báo, đăng ký đã hủy không tự khôi phục (FR-023b).
13. **Given** K có đăng ký "Hát karaoke" (hoạt động nhóm) ngày 08/10 và "Xem TV" (cá nhân) ngày 09/10, **When** feature 007 gắn dấu "nghi nhiễm" cho K ngày 07/10, **Then** đăng ký "Hát karaoke" tự hủy lý do "nghi nhiễm", đăng ký "Xem TV" giữ nguyên; **When** S đăng ký K vào một buổi hoạt động nhóm hoặc chuyến đi, hoặc thêm K lúc điểm danh buổi nhóm, **Then** hệ thống chặn; **When** S đăng ký K vào "Đi dạo" cá nhân, **Then** hệ thống cảnh báo và cho lưu sau khi S xác nhận; các ngày K mang dấu không tính cho cảnh báo cô lập (FR-015 (g), FR-018, FR-021a, FR-054, Q-165).
14. **Given** L là người bán trú có lịch 07:30–16:30 thứ 2 → thứ 6, **When** S đăng ký L vào chuyến "Tham quan chùa" 13:00–18:00 thứ 6 ngày 09/10, **Then** hệ thống chặn và hướng dẫn cần đồng ý về muộn của người đại diện; **When** Hành chính ghi nhận đồng ý của người đại diện R cho đúng chuyến đó qua bản ký, rồi S đăng ký lại, **Then** đăng ký được lưu và giờ về dự kiến ngày 09/10 của L thành 18:00, feature 005, 010, 011 nhận giờ mới; **When** chuyến gia hạn giờ về tới 18:30, **Then** giờ về dự kiến của L cũng thành 18:30; **When** S đăng ký L vào buổi 07:00 cùng ngày (trước giờ đến), **Then** hệ thống chặn (FR-016, FR-016a, Q-167).

---

### User Story 3 - Điểm danh buổi hoạt động và tạo chi phí khi có thu phí (Priority: P1)

Người phụ trách buổi (trưởng tầng, nhân viên chăm sóc hoặc điều dưỡng, Q-214) điểm danh từng người đăng ký: có mặt hoặc vắng kèm lý do; với người có mặt ghi mức độ tham gia, giao tiếp và tình trạng sau hoạt động. Người không đăng ký trước được thêm tại lúc điểm danh nếu đủ điều kiện. Điểm danh có mặt ở hoạt động có thu phí tự tạo chi phí nháp. Bản ghi điểm danh không sửa, không xóa; sai sót xử lý bằng đính chính.

**Why this priority**: Điểm danh là căn cứ duy nhất cho chi phí hoạt động (BR-M04-18, DBR-15), truy vết tiếp xúc khi có lây nhiễm (BR-M05-10), tỷ lệ tham gia (18.2) và phát hiện nguy cơ cô lập (BR-M04-22).

**Independent Test**: Buổi "Tập đàn" có thu phí với 4 người đăng ký; điểm danh 3 có mặt, 1 vắng; thêm 1 người tại chỗ; kiểm tra 4 chi phí nháp; đính chính một người từ có mặt sang vắng; kiểm tra feature 010 nhận sự kiện hủy.

**Acceptance Scenarios**:

1. **Given** buổi "Tập đàn" 09:00 ngày 07/10 có thu phí, dịch vụ "Lớp đàn" trong danh mục, 4 người đăng ký A, B, C, D, **When** S điểm danh A, B, C có mặt (mức độ tham gia, giao tiếp, tình trạng sau) và D vắng lý do "từ chối", rồi hoàn tất điểm danh, **Then** buổi chuyển Đã điểm danh; feature 010 nhận ba lượt điểm danh có mặt và tạo ba chi phí nháp trỏ về từng lượt (BR-M04-18, BR-M11-01, DBR-15); D không có chi phí.
2. **Given** E không đăng ký trước nhưng muốn tham gia và buổi còn chỗ, **When** S thêm E lúc điểm danh, **Then** hệ thống kiểm tra như đăng ký (FR-015) rồi ghi E có mặt, dấu "tham gia không đăng ký trước"; E có chi phí nháp.
3. **Given** buổi đã đủ 20 người có mặt, **When** S thêm người thứ 21 lúc điểm danh, **Then** hệ thống chặn (BR-M04-15).
4. **Given** S đã ghi C có mặt nhưng thực tế C vắng, **When** S tạo bản đính chính kèm lý do, **Then** bản gốc giữ nguyên, hiện dấu "đã đính chính"; feature 010 nhận sự kiện hủy lượt điểm danh để hủy chi phí nháp hoặc tạo khoản điều chỉnh nếu đã chốt (FR-031).
5. **Given** buổi kết thúc lúc 10:00, **When** tới 10:00 + CFG-M04-06 (mặc định \[2 giờ\]) mà chưa hoàn tất điểm danh, **Then** người phụ trách buổi và T được nhắc; điểm danh ghi sau mốc này gắn nhãn "ghi nhận muộn" (FR-030).
6. **Given** A Đang lưu trú có đăng ký buổi 15:00 nhưng chuyển Tạm vắng lúc 13:00, **When** tới giờ điểm danh, **Then** A được hiển thị sẵn "Vắng – đang vắng mặt"; người điểm danh không cần ghi thêm (FR-028).
7. **Given** nhân viên vệ sinh hoặc bác sĩ, **When** tìm cách điểm danh, **Then** hệ thống không cho phép (Phụ lục 27 dòng "Hoạt động, ngoài viện"). **Given** điều dưỡng D1 có người cao tuổi của buổi trong phạm vi phân công, **When** D1 điểm danh, **Then** được phép (Q-214, Phụ lục 27 ²⁷).
8. **Given** A và B cùng được ghi Có mặt ở buổi "Đố vui" ngày 06/10, C được ghi Vắng, **When** ngày 09/10 feature 007 lập danh sách tiếp xúc cho A (nghi nhiễm) trong CFG-M05-07 (mặc định \[5 ngày\]), **Then** spec này cung cấp B (cùng có mặt ở buổi, kèm buổi và thời điểm), không cung cấp C (FR-033, BR-M05-10).
9. **Given** buổi "Vẽ tranh" ngày 07/10 có 5 đăng ký, S ghi Có mặt cho 3 người rồi quên 2 người, **When** hết ngày 07/10, **Then** 2 đăng ký còn lại nhận kết quả "Không ghi nhận", buổi chuyển Đã điểm danh với dấu "điểm danh không đủ", trưởng tầng được báo; ngày 07/10 không là ngày tính cho cảnh báo cô lập của 2 người đó; **Given** buổi "Cờ tướng" cùng ngày không có lượt điểm danh nào, **When** hết ngày, **Then** buổi chuyển "Không điểm danh" và mọi đăng ký nhận "Không ghi nhận"; S sửa được bằng đính chính, gắn nhãn "ghi nhận muộn" (FR-014a).

---

### User Story 4 - Chuẩn bị và điểm danh rời viện cho hoạt động ngoài viện (Priority: P1)

Với buổi thuộc hoạt động ngoài viện (chuyến đi), trước khi đi trưởng tầng phân công trưởng đoàn (Trưởng tầng, Nhân viên chăm sóc hoặc Điều dưỡng, Q-214) và người đi cùng (nhân viên bất kỳ có ca chồng thời gian chuyến), đánh giá khả năng tham gia của từng người đăng ký, và điều dưỡng phụ trách chuẩn bị thuốc cần mang. Khi xuất phát, trưởng đoàn điểm danh rời viện; người tham gia chuyển sang Hoạt động bên ngoài, liều thuốc trong khoảng đi chuyển Mang theo, công việc trong khoảng đi bị hủy.

**Why this priority**: Rời viện là lúc rủi ro an toàn cao nhất: người không đủ khả năng đi, thiếu thuốc hoặc không có người chịu trách nhiệm đều dẫn tới sự cố nghiêm trọng (8.9, BR-M04-16).

**Independent Test**: Chuyến "Tham quan chùa" 07:00–13:00 với 4 người đăng ký; phân công trưởng đoàn và một người đi cùng; đánh giá 3 người đạt, 1 không đạt; A có hai liều trong khoảng đi; thử điểm danh rời viện khi chưa giao thuốc cho A (bị chặn); giao thuốc rồi điểm danh; kiểm tra trạng thái, liều, công việc.

**Acceptance Scenarios**:

1. **Given** chuyến "Tham quan chùa" ngày 10/10, rời 07:00, về dự kiến 13:00, 4 người đăng ký A, B, C, D, **When** T chọn điểm danh rời viện khi chưa có trưởng đoàn, **Then** hệ thống chặn (FR-035).
2. **Given** T phân công S1 làm trưởng đoàn và S2 đi cùng, **When** T đánh giá khả năng tham gia: A, B, C "Đạt", D "Không đạt" lý do "huyết áp cao sáng nay", **Then** D chuyển "Không đi", đăng ký của D kết thúc với lý do; D không bị chuyển trạng thái (FR-036, Q-161).
3. **Given** A có liều 08:00 và 12:00 trong khoảng đi (feature 006), chưa có lần "Giao thuốc mang theo" và chưa có quyết định "không mang thuốc", **When** S1 điểm danh A rời viện, **Then** hệ thống chặn riêng A và hiển thị danh sách thuốc cần mang của A cho điều dưỡng phụ trách chuẩn bị (FR-038, 8.9).
4. **Given** điều dưỡng D1 đã ghi lần giao thuốc mang theo của A cho S1 (feature 006 FR-048a), **When** S1 điểm danh A, B, C rời viện lúc 07:05, **Then** trong cùng một lần: A, B, C chuyển Hoạt động bên ngoài (người thực hiện ghi là S1, căn cứ là chuyến đi, feature 001); liều 08:00 và 12:00 của A chuyển Mang theo (feature 006, BR-M04-16); công việc Chưa đến hạn của A, B, C trong khoảng 07:05 → 13:00 chuyển Hủy lý do "vắng mặt" (feature 005 FR-020, BR-M04-04); feature 011 nhận thời điểm rời thực tế.
5. **Given** chuyến có thu phí, **When** S1 điểm danh A, B, C rời viện, **Then** feature 010 nhận ba lượt điểm danh có mặt để tạo chi phí nháp; D "Không đi" không có chi phí (BR-M04-18).
6. **Given** C có cờ nguy cơ đi lạc, **When** T đánh giá C "Đạt", **Then** hệ thống yêu cầu chỉ định một người đi cùng kèm riêng cho C và ghi vào danh sách chuyến (FR-036).
7. **Given** chuyến 10/10 có B đăng ký, **When** tầng của B bị khoanh vùng ngày 09/10, **Then** đăng ký của B tự hủy theo FR-021; chuyến vẫn diễn ra với người còn lại vì địa điểm ngoài viện không thuộc vùng khoanh.
8. **Given** T muốn phân công nhân viên hành chính H1 làm trưởng đoàn, **When** lưu, **Then** hệ thống chặn vì trưởng đoàn phải là Trưởng tầng, Nhân viên chăm sóc hoặc Điều dưỡng; **When** T phân công điều dưỡng D1 (có ca 06:00–14:00) làm trưởng đoàn, **Then** được chấp nhận (Q-214); **When** T phân công H1 làm người đi cùng, H1 có ca chồng thời gian chuyến, **Then** được chấp nhận và H1 chỉ có lệnh "Báo thiếu người" trong chuyến; **When** T phân công nhân viên chăm sóc S3 không có ca chồng thời gian chuyến, **Then** hệ thống chặn (FR-035, Q-161). **When** S1 (trưởng đoàn) tìm cách ghi đánh giá khả năng tham gia, **Then** hệ thống chặn vì chỉ Trưởng tầng đánh giá (FR-036).

---

### User Story 5 - Theo dõi chuyến đi, điểm danh về, quá giờ và thiếu người (Priority: P1)

Trong chuyến đi, trưởng đoàn xem danh sách người đang đi, người đi cùng và thuốc mang theo. Khi trở về, trưởng đoàn điểm danh về; người được điểm danh chuyển lại Đang lưu trú. Quá giờ về dự kiến CFG-M04-07 mà chưa kết thúc điểm danh về thì trưởng đoàn và trưởng tầng được cảnh báo. Khi kết thúc điểm danh về mà còn người không có mặt, hoặc trưởng đoàn phát hiện mất người trong chuyến, hệ thống tạo sự cố khẩn cấp.

**Why this priority**: BR-M04-17 là quy tắc an toàn khẩn cấp; người cao tuổi thất lạc ngoài viện là sự cố nghiêm trọng nhất của module.

**Independent Test**: Chuyến có A, B, C rời 07:05, về dự kiến 13:00; để quá 13:30 chưa điểm danh về (cảnh báo); gia hạn giờ về; điểm danh về A, B và kết thúc điểm danh khi C không có mặt (sự cố khẩn cấp); tìm thấy C và điểm danh về muộn.

**Acceptance Scenarios**:

1. **Given** chuyến về dự kiến 13:00, **When** tới 13:30 (CFG-M04-07 mặc định \[30 phút\]) chưa kết thúc điểm danh về, **Then** hệ thống báo trưởng đoàn S1 và trưởng tầng T mức Trung bình; trạng thái người tham gia giữ nguyên Hoạt động bên ngoài (BR-M04-17).
2. **Given** S1 biết đoàn bị kẹt xe, **When** S1 "Gia hạn giờ về" tới 14:00 kèm lý do, **Then** mốc cảnh báo tính lại là 14:30; feature 006, 011 nhận giờ về dự kiến mới (FR-043).
3. **Given** đoàn về lúc 13:50, **When** S1 điểm danh về A, B, **Then** A, B chuyển Đang lưu trú; feature 005 sinh lại công việc từ lúc trở về (feature 005 FR-020); feature 006 tính lại liều Mang theo chưa tới giờ (Q-60).
4. **Given** C không có mặt khi đoàn về, **When** S1 kết thúc điểm danh về, **Then** C chuyển "Thiếu khi về"; trong cùng một lần hệ thống yêu cầu feature 007 tạo sự cố khẩn cấp loại "đi lạc hoặc không trở về", nguồn "hoạt động ngoài viện", gắn C và chuyến, kèm địa điểm, thời điểm thấy C lần cuối do S1 ghi; trạng thái C giữ nguyên Hoạt động bên ngoài (BR-M04-17, 5.5).
5. **Given** giữa chuyến lúc 10:00 S1 phát hiện không thấy C, **When** S1 chọn "Báo thiếu người" cho C, **Then** sự cố khẩn cấp được tạo ngay như kịch bản 4, không chờ tới lúc về; C chuyển "Thiếu khi về" (FR-046).
6. **Given** C đang "Thiếu khi về" và sự cố đang mở, **When** C được tìm thấy và đưa về viện lúc 16:00, S1 hoặc T điểm danh về muộn cho C, **Then** C chuyển Đang lưu trú; feature 007 nhận diễn biến "đã tìm thấy, đã trở về" cho sự cố; sự cố vẫn do người xử lý đóng theo feature 007 (FR-047).
7. **Given** B ngã trong chuyến và được chuyển thẳng tới bệnh viện qua sự cố khẩn cấp (feature 007 BR-M05-14), **When** S1 kết thúc điểm danh về, **Then** B hiển thị "Rời đoàn – chuyển viện", không bị tính là thiếu người và không tạo sự cố thiếu người (FR-045).
8. **Given** người thân của A đến điểm tham quan và muốn đón A về nhà luôn, **When** S1 tìm cách ghi A rời đoàn để về với người thân, **Then** hệ thống không có lệnh đó; A phải được điểm danh về viện trước, rồi Cho tạm vắng theo quy trình đón của feature 004, 012 (5.5 không có chuyển Hoạt động bên ngoài → Tạm vắng; FR-045).
9. **Given** chuyến Đang đi, giờ về dự kiến 13:00, **When** tới 13:30 chưa Kết thúc điểm danh về, **Then** S1 và T được báo (FR-049); **When** tới 14:00 (giờ về dự kiến + 2 × CFG-M04-07) vẫn chưa kết thúc và chưa gia hạn, **Then** Quản lý viện được báo mức Trung bình (FR-049a).
10. **Given** giữa chuyến trưởng đoàn S1 phải đưa B tới bệnh viện, người đi cùng còn lại là nhân viên hành chính H1 và điều dưỡng D1, **When** S1 hoặc T thực hiện "Chuyển trưởng đoàn" cho H1, **Then** hệ thống chặn vì H1 không có vai trò Trưởng tầng, Nhân viên chăm sóc hoặc Điều dưỡng; **When** chuyển cho D1, **Then** D1 thành trưởng đoàn từ thời điểm đó, S1 được ghi là người đi cùng, lịch sử lưu lại (FR-035a, Q-214). **Given** ca của D1 kết thúc lúc 14:00 khi chuyến chưa về, **When** tới 14:00, **Then** D1 vẫn là trưởng đoàn tới khi chuyến Đã về; T được báo (FR-035).
11. **Given** H bị điểm danh rời viện nhầm (thực tế H ở lại viện), **When** T tạo đính chính "Hủy ghi nhận" cho bản ghi rời viện của H kèm lý do trong lúc chuyến chưa Đã về, **Then** H chuyển về Đang lưu trú với căn cứ "đính chính điểm danh", feature 005, 006, 010, 011 nhận sự kiện để sinh lại công việc, tính lại liều, hủy chi phí nháp, tính lại suất (FR-031a).

---

### User Story 6 - Theo dõi tinh thần, sở thích và cảnh báo nguy cơ cô lập (Priority: P2)

Nhân viên ghi nhận sở thích của người cao tuổi. Hệ thống tổng hợp tinh thần theo người từ tâm trạng, hành vi bất thường (feature 005) và mức độ tham gia, giao tiếp khi điểm danh. Người không tham gia hoạt động nhóm nào trong CFG-M04-10, không tính thời gian vắng mặt hoặc có chỉ định hạn chế, được cảnh báo nhẹ "nguy cơ cô lập" cho trưởng tầng kèm danh sách hoạt động gợi ý. Danh sách gợi ý cũng được cung cấp cho cảnh báo tâm trạng tiêu cực kéo dài của feature 005.

**Why this priority**: Phát hiện sớm cô lập và suy giảm tinh thần là giá trị chăm sóc quan trọng, nhưng phụ thuộc vào dữ liệu điểm danh của User Story 3.

**Independent Test**: Ghi sở thích "âm nhạc", "cờ tướng" cho A; để A 7 ngày không có điểm danh có mặt ở hoạt động nhóm; kiểm tra một cảnh báo nhẹ với gợi ý; cho B vắng 3 ngày trong 7 ngày và kiểm tra không cảnh báo sớm.

**Acceptance Scenarios**:

1. **Given** A có sở thích "âm nhạc", "cờ tướng", **When** nhân viên chăm sóc S ghi thêm "làm vườn" kèm nguồn "người cao tuổi tự nói", **Then** sở thích được thêm với người ghi, thời điểm; lịch sử sở thích xem được (FR-051).
2. **Given** A Đang lưu trú, không có chỉ định hạn chế, lần cuối có mặt ở hoạt động nhóm ngày 28/09, **When** Bộ lập lịch kiểm tra cuối ngày 05/10, **Then** A đủ 7 ngày tính (CFG-M04-10 mặc định \[7 ngày\]) không tham gia; hệ thống yêu cầu feature 007 tạo cảnh báo nhẹ "nguy cơ cô lập" cho A, gửi trưởng tầng, kèm danh sách hoạt động gợi ý trong 7 ngày tới theo sở thích (BR-M04-22, FR-055).
3. **Given** B không tham gia hoạt động nhóm từ 28/09 nhưng Tạm vắng từ 01/10 tới 03/10, **When** kiểm tra cuối ngày 05/10, **Then** chỉ có 5 ngày tính; không cảnh báo; nếu B vẫn không tham gia thì cảnh báo vào cuối ngày 07/10 (FR-054).
4. **Given** A đã có cảnh báo "nguy cơ cô lập" đang mở, **When** A tiếp tục không tham gia, **Then** hệ thống không tạo cảnh báo mới mà gộp vào cảnh báo cũ theo DBR-18; **When** A có mặt ở một hoạt động nhóm, **Then** chuỗi đếm lại từ đầu và feature 007 nhận diễn biến "đã tham gia hoạt động nhóm" cho cảnh báo đang mở (FR-056).
5. **Given** C có chỉ định hạn chế "mọi hoạt động nhóm" từ 01/10 tới 20/10, **When** kiểm tra hằng ngày, **Then** các ngày có chỉ định không được tính; C không bị cảnh báo cô lập trong thời gian đó (BR-M04-22).
6. **Given** feature 005 phát hiện E có tâm trạng tiêu cực 3 ngày liên tiếp, **When** feature 005 yêu cầu danh sách gợi ý, **Then** spec này trả các buổi trong 7 ngày tới khớp sở thích của E, E đủ điều kiện đăng ký và còn chỗ (BR-M04-11, feature 005 FR-042).
7. **Given** trưởng tầng T mở hồ sơ tinh thần của A, **When** xem 30 ngày gần nhất, **Then** thấy: số buổi hoạt động nhóm có mặt theo tuần, mức độ tham gia và giao tiếp theo buổi, tâm trạng và hành vi bất thường theo ngày (feature 005), các cảnh báo cô lập, tâm trạng đã có (FR-053).

---

### User Story 7 - Kiểm tra chất lượng ngẫu nhiên công việc đã hoàn thành (Priority: P2)

Trong mỗi ca, ngay khi một công việc trong tầng (kể cả công việc vệ sinh) được hoàn thành, hệ thống xét chọn ngẫu nhiên công việc đó theo tỷ lệ CFG-M04-11 để trưởng tầng kiểm tra lại ngay trong ca, ghi Đạt hoặc Không đạt kèm ghi chú. Gần cuối ca, hệ thống chọn bổ sung nếu số đã chọn chưa đạt tỷ lệ. Mục chưa kiểm tra khi hết ca thành Quá hạn kiểm tra. Kết quả Không đạt tạo công việc làm lại cho ca hiện tại và được ghi vào báo cáo chất lượng theo nhân viên. Bản ghi gốc của công việc không bị sửa.

**Why this priority**: Kiểm tra ngẫu nhiên giúp giám sát chất lượng chăm sóc mà không phải kiểm tra toàn bộ; phụ thuộc dữ liệu công việc của feature 005 đã có.

**Independent Test**: Ca ngày 06:00–18:00 tầng 2 có 200 công việc hoàn thành rải trong ca; kiểm tra công việc được chọn ngay khi hoàn thành, tới mốc 17:30 số được chọn được bổ sung đủ 10; T ghi 9 Đạt, 1 Không đạt; kiểm tra công việc làm lại được sinh trong ca, bản ghi gốc không đổi; để một mục qua 18:00 và kiểm tra Quá hạn kiểm tra.

**Acceptance Scenarios**:

1. **Given** ca ngày 06/10 của tầng 2 (06:00–18:00), **When** nhân viên chăm sóc S hoàn thành công việc "Thay ga giường" của A lúc 09:10, **Then** hệ thống xét chọn công việc đó ngẫu nhiên theo tỷ lệ CFG-M04-11 (mặc định \[5%\]); nếu được chọn, công việc vào danh sách kiểm tra của ca, T được báo, S không được báo (BR-M04-23, FR-059, Q-163). Công việc vệ sinh được xét như công việc chăm sóc.
2. **Given** tới 17:30 (CFG-M09-04 mặc định \[30 phút\] trước khi kết ca) ca có 200 công việc đủ điều kiện, trong đó 7 đã được chọn, **When** hệ thống kiểm tra mốc bổ sung, **Then** hệ thống chọn ngẫu nhiên thêm 3 trong 193 công việc chưa được chọn để đạt 10 (5%, làm tròn lên); nếu đã được chọn từ 10 trở lên thì không chọn thêm. Công việc hoàn thành sau 17:30 vẫn được xét chọn như kịch bản 1 (FR-059a).
3. **Given** mục "Vệ sinh cá nhân" của C được chọn lúc 16:00 và chưa có kết quả, **When** ca kết thúc lúc 18:00, **Then** mục tự đóng với kết quả "Quá hạn kiểm tra", không tính vào tỷ lệ Đạt, Quản lý viện được báo (FR-063).
4. **Given** T kiểm tra công việc "Thay ga giường" của A do S thực hiện, **When** T ghi Đạt, **Then** kết quả kiểm tra được lưu cùng người kiểm tra, thời điểm; công việc gốc không đổi.
5. **Given** T kiểm tra công việc "Vệ sinh cá nhân" của B do S thực hiện, **When** T ghi Không đạt kèm ghi chú "chưa vệ sinh răng miệng", **Then** trong cùng một lần: hệ thống sinh công việc làm lại cùng loại cho B, mức quan trọng bằng công việc gốc, thời điểm dự kiến trong chính ca đó, giao cho S nếu S còn tên trong ca, nếu không thì thành công việc chung của tầng (feature 005); kết quả gốc của công việc không bị sửa; S được báo; kết quả Không đạt được tính vào tỷ lệ Đạt của S (BR-M04-23, 18.2).
6. **Given** công việc vệ sinh phòng P205 do nhân viên vệ sinh V thực hiện bị T ghi Không đạt, **When** lưu, **Then** công việc vệ sinh làm lại được sinh cho P205 theo quy tắc của feature 003 cho công việc vệ sinh; V được báo (BR-M04-23 "(Bổ sung, vệ sinh)").
7. **Given** công việc được chọn là của C, nhưng C đã chuyển viện trước khi T kiểm tra, **When** T ghi "Không kiểm tra được" kèm lý do, **Then** mục được đóng, không tính vào tỷ lệ Đạt hay Không đạt (FR-062).
8. **Given** T hoàn thành một công việc do chính T được giao, **When** hệ thống xét chọn, **Then** công việc đó không được xét (FR-060).
9. **Given** công việc đã được chọn kiểm tra nhưng sau đó bị đính chính "Hủy ghi nhận", **When** T mở danh sách, **Then** mục đó tự đóng với lý do "công việc đã hủy ghi nhận", không tính vào tỷ lệ (FR-062).
10. **Given** nhân viên chăm sóc, điều dưỡng hay người phụ trách ca không phải trưởng tầng, **When** tìm cách ghi kết quả kiểm tra, **Then** hệ thống không cho phép (4.4 dòng "Kiểm tra chất lượng": chỉ Trưởng tầng T).

---

### Edge Cases

- **Người thường xuyên đổi trạng thái**: người Tạm vắng, Điều trị tại bệnh viện, Hoạt động bên ngoài có đăng ký trùng khoảng vắng không bị hủy đăng ký tự động (có thể về kịp); lúc điểm danh được hiển thị sẵn "Vắng – đang vắng mặt" (FR-028). Người chuyển trạng thái cuối: mọi đăng ký tương lai tự hủy, bị gỡ khỏi danh sách người tham gia thường xuyên (FR-022).
- **Bán trú**: không đăng ký được buổi bắt đầu trước giờ đến theo lịch hoặc vào ngày không có lịch; buổi kết thúc sau giờ về theo lịch chỉ đăng ký được khi có đồng ý về muộn của người đại diện, và giờ về dự kiến của ngày đó tự dời (FR-016, FR-016a, Q-167). Ngày không điểm danh đến thì lúc điểm danh buổi hiển thị "Vắng – không đến" (FR-028).
- **Buổi bị dời giờ**: đăng ký được kiểm tra lại; người không còn đủ điều kiện (trùng giờ, ngoài khung bán trú, khoanh vùng) bị hủy đăng ký kèm lý do và được báo; feature 005 nhận thời điểm mới (FR-012).
- **Số lượng tối đa bị giảm dưới số đã đăng ký**: không tự hủy đăng ký; hệ thống chặn giảm hoặc yêu cầu trưởng tầng chọn đăng ký cần hủy (FR-003).
- **Hai người cùng đăng ký chỗ cuối**: chỉ lần lưu đầu tiên được chấp nhận; lần sau bị chặn vì đủ số lượng (theo feature 000 FR-021).
- **Chỉ định hạn chế gắn trong lúc buổi đang diễn ra**: không hủy điểm danh đã ghi; người điểm danh được hiển thị chỉ định mới (FR-025).
- **Khoanh vùng được đặt khi chuyến ngoài viện đang đi**: người đang đi thuộc vùng vẫn được điểm danh về; khi về, hệ thống báo trưởng đoàn và trưởng tầng rằng người đó trở về vùng khoanh (feature 007 quyết định cách ly tiếp theo).
- **Người đi cùng hoặc trưởng đoàn vắng đột xuất trước giờ rời**: trưởng tầng phân công lại; không có trưởng đoàn thì không điểm danh rời viện được (FR-035).
- **Trưởng đoàn không điểm danh về (mất kết nối, quên)**: cảnh báo quá giờ về theo BR-M04-17; trưởng tầng được điểm danh về thay khi đoàn đã ở viện (FR-044).
- **Người đi trở về sớm một mình** (ví dụ mệt, có nhân viên đưa về): trưởng đoàn hoặc trưởng tầng điểm danh về riêng người đó; chuyến vẫn Đang đi (FR-044).
- **Người tham gia qua đời trong chuyến**: chuyển trạng thái qua lệnh Ghi nhận qua đời của feature 004; người đó hiển thị "Rời đoàn – qua đời", không tạo sự cố thiếu người (FR-045).
- **Mất kết nối khi điểm danh rời viện hoặc về**: điểm danh được ghi tạm và đồng bộ sau (Q-01); với "Báo thiếu người" và kết thúc điểm danh về có người thiếu, việc tạo sự cố khẩn cấp theo ngoại lệ bắt buộc trực tuyến của 8.6: thiết bị phải hướng dẫn gọi điện cho trưởng tầng ngay và trưởng tầng thực hiện lệnh thay (FR-046a).
- **Điểm danh có mặt ở hoạt động có thu phí rồi đính chính sau khi kỳ chi phí đã chốt**: feature 010 xử lý bằng khoản điều chỉnh (DBR-17).
- **Mẫu kiểm tra chất lượng khi ca có ít công việc**: có ít nhất 1 công việc đủ điều kiện thì mốc bổ sung bảo đảm tối thiểu 1 mục (làm tròn lên, FR-059a); không có công việc nào thì danh sách của ca trống.
- **Công việc hoàn thành trong 30 phút cuối ca được chọn**: trưởng tầng chỉ còn ít thời gian; mục không kịp kiểm tra thành "Quá hạn kiểm tra" (FR-063). Đây là hệ quả đã chấp nhận của Q-163.
- **Ca không có trưởng tầng trực** (ca đêm, hoặc tầng tạm chưa có trưởng tầng được giao): danh sách vẫn được lập; trưởng tầng được giao ghi được trong thời gian ca dù không có tên trong ca; không có ai ghi thì các mục thành "Quá hạn kiểm tra" và Quản lý viện được báo (FR-063, FR-066; Q-84, Q-90; điểm báo lại 8).
- **Chỉ định tạm của điều dưỡng hết hạn khi bác sĩ chưa xem**: chỉ định Hết hiệu lực; nếu người cao tuổi vẫn cần hạn chế, điều dưỡng báo bác sĩ để gắn chỉ định chính thức; điều dưỡng không gắn chỉ định tạm liên tiếp để kéo dài được khi chỉ định tạm trước đó vừa hết hiệu lực chưa quá CFG-M04-14 (FR-023a, Assumptions).
- **Công việc làm lại cũng Không đạt**: công việc làm lại là công việc bình thường, có thể được chọn ở mẫu của ca sau; không tự sinh vòng lặp làm lại.
- **Cảnh báo cô lập với người mới vào ở**: chuỗi chỉ tính từ ngày Hoàn tất tiếp nhận; người vào ở dưới CFG-M04-10 ngày không bị cảnh báo (FR-054).
- **Mẫu lặp có ngày kết thúc, ngày viện nghỉ hoạt động**: không sinh buổi sau ngày kết thúc; ngày viện không tổ chức hoạt động (ví dụ lễ) được xử lý bằng Hủy buổi có lý do, spec không có lịch ngày nghỉ riêng; đổi mẫu không được có ngày áp dụng trước hôm nay (FR-005, FR-009).
- **Chuyến đi bị hủy hoặc người Không đi sau khi thuốc đã được giao mang theo**: điều dưỡng phụ trách được báo để ghi Nhận lại theo feature 006 (Q-58, FR-038a).
- **Chuyến đi được dời sang ngày khác sau khi đã đánh giá**: mọi đánh giá quay lại chưa đánh giá, danh sách liều được lấy lại (FR-038a).
- **Người đại diện muốn rút đồng ý về muộn khi buổi đã bắt đầu**: đồng ý đã ở "Đã dùng", không rút được; người đại diện liên hệ viện (FR-016a).

## Requirements *(mandatory)*

### Functional Requirements

**Thuật ngữ của spec** (dùng thống nhất ở mọi FR, bảng, kịch bản, thông báo):

- **Hoạt động**: một hoạt động được khai báo (danh mục, nhóm 1), có thể có mẫu lặp.
- **Buổi**: một lần diễn ra cụ thể của hoạt động, có ngày giờ, địa điểm (nhóm 2).
- **Chuyến đi**: buổi của hoạt động thuộc loại "ngoài viện" (8.9).
- **Hoạt động nhóm**: hoạt động thuộc loại có dấu "hoạt động nhóm" (FR-001); là căn cứ của BR-M04-22.
- **Đăng ký**: việc ghi một người cao tuổi vào một buổi trước khi buổi diễn ra (nhóm 2).
- **Điểm danh**: bản ghi kết quả tham gia của một người ở một buổi (nhóm 3); với chuyến đi gồm điểm danh rời viện và điểm danh về.
- **Chỉ định hạn chế hoạt động**: cờ "không đủ điều kiện" do bác sĩ gắn ở BR-M04-15, có phạm vi và thời gian hiệu lực; cũng là "chỉ định hạn chế" ở BR-M04-22.
- **Trưởng đoàn**: nhân viên được chỉ định dẫn một chuyến đi (2.4); **người đi cùng**: nhân viên khác được phân công đi theo chuyến.
- **Ngày tính**: ngày dương lịch (00:00–24:00) mà người cao tuổi ở Đang lưu trú trong suốt ngày, không có khoảng vắng nào dù ngắn (bán trú: ngày có điểm danh đến), không có chỉ định hạn chế bao trùm hoạt động nhóm vào bất kỳ lúc nào trong ngày, không mang dấu "nghi nhiễm", và không có đăng ký hoạt động nhóm mang kết quả "Không ghi nhận" (BR-M04-22, Q-165, Q-169, Q-173).
- **Không ghi nhận**: kết quả hệ thống gán cho đăng ký chưa có kết quả điểm danh khi hết ngày của buổi; không phải Có mặt, không phải Vắng (FR-014a).
- **Danh sách kiểm tra chất lượng**: tập công việc được hệ thống chọn ngẫu nhiên trong một ca của một tầng để trưởng tầng kiểm tra (BR-M04-23).

Nguồn ghi ở cuối từng FR là mã quy tắc, use case hoặc quyết định cụ thể.

#### A. Danh mục loại hoạt động và hoạt động

- **FR-001**: Hệ thống MUST quản lý danh mục loại hoạt động (nhóm 1) gồm tối thiểu các loại của 8.8: giải trí, vận động, phục hồi, giao lưu, xem TV, vui chơi, đi dạo, ngoài viện. Mỗi loại có dấu "hoạt động nhóm" (mặc định bật cho mọi loại trừ "xem TV" và "đi dạo"), dấu "ngoài viện" (chỉ loại "ngoài viện") và các nhóm sở thích liên quan (FR-051). Quản lý viện tạo, sửa, ngừng hiệu lực loại; loại đã được tham chiếu MUST NOT bị xóa. *(Nguồn: 8.8, 1.5 nhóm 1)*
- **FR-002**: Trưởng tầng MUST khai báo được hoạt động (nhóm 1) gồm: tên, loại, phạm vi tổ chức (một tầng/khu vực hoặc toàn viện), địa điểm mặc định (khu vực trong viện, hoặc điểm đến với chuyến đi), người phụ trách mặc định, thời lượng, số lượng tối đa, đối tượng (mức chăm sóc, loại hình lưu trú, tầng/khu vực được mời), có thu phí hay không và dịch vụ tương ứng trong danh mục dịch vụ của feature 004 khi có thu phí, dấu "hoạt động nhóm" (mặc định theo loại, trưởng tầng đổi được cho từng hoạt động kèm lý do). Trưởng tầng chỉ khai báo hoạt động cho tầng mình phụ trách hoặc toàn viện. *(Nguồn: 8.8 "Quản lý", UC-29, 4.4)*
- **FR-002a**: Địa điểm của buổi trong viện MUST chọn từ phòng hoặc khu vực trong cấu trúc của feature 003 (2.2), nên luôn gắn một tầng/khu vực; điểm đến của chuyến đi là mô tả ngoài viện, không gắn tầng. Người phụ trách (mặc định và của từng buổi) MUST có vai trò Trưởng tầng, Nhân viên chăm sóc hoặc Điều dưỡng. *(Suy ra từ 2.2, 4.4; Q-175; Điều dưỡng: Q-214)*
- **FR-002b**: Hoạt động, mẫu lặp và buổi của hoạt động theo tầng chỉ do trưởng tầng của tầng đó sửa, dời, hủy, ngừng hiệu lực. Với hoạt động toàn viện, các lệnh này MUST do trưởng tầng đã tạo hoạt động hoặc trưởng tầng của tầng/khu vực nơi đặt địa điểm của buổi thực hiện. *(Suy ra từ 4.4, FR-002; Q-175)*
- **FR-003**: Sửa hoạt động MUST áp cho buổi được sinh sau thời điểm sửa và buổi chưa diễn ra, không áp cho buổi đã có điểm danh. Giảm số lượng tối đa dưới số đăng ký hiện có của một buổi chưa diễn ra MUST bị chặn cho buổi đó, trừ khi trưởng tầng chọn các đăng ký cần hủy kèm lý do. *(Suy ra từ BR-M04-21, BR-M04-15)*
- **FR-004**: Ngừng hiệu lực hoạt động MUST hủy mọi buổi chưa có điểm danh từ ngày ngừng, với lý do "ngừng hoạt động"; hoạt động đã có buổi MUST NOT bị xóa. *(Nguồn: 1.5 nhóm 1)*

#### B. Hoạt động định kỳ và sinh buổi

- **FR-005**: Hoạt động MAY có mẫu lặp: các ngày trong tuần (hoặc chu kỳ theo ngày), giờ bắt đầu, giờ kết thúc, ngày bắt đầu, ngày kết thúc (nếu có), và danh sách người tham gia thường xuyên. Bộ lập lịch MUST NOT sinh buổi sau ngày kết thúc. Spec không có lịch ngày nghỉ của viện; buổi rơi vào ngày không tổ chức được xử lý bằng Hủy buổi (FR-013). *(Nguồn: 8.8 "(Bổ sung) Hoạt động định kỳ")*
- **FR-006**: Bộ lập lịch MUST sinh buổi từ mẫu lặp cho mọi ngày trong CFG-M04-09 (mặc định \[7 ngày\]) tính từ ngày chạy; mỗi lần chạy chỉ sinh các buổi còn thiếu. *(Nguồn: BR-M04-21, CFG-M04-09, UC-29)*
- **FR-007**: Buổi MUST là duy nhất theo (hoạt động, thời điểm bắt đầu); chạy lại hoặc chạy trễ MUST NOT tạo buổi trùng. Buổi đã bị hủy thủ công MUST NOT được sinh lại khi Bộ lập lịch chạy lần sau. *(Suy ra từ DBR-12, NFR-04)*
- **FR-008**: Khi sinh buổi, hệ thống MUST đăng ký sẵn mỗi người tham gia thường xuyên đạt các điều kiện chặn của FR-015 tại thời điểm sinh; người không đạt MUST không được đăng ký, và trưởng tầng của hoạt động MUST nhận danh sách người thường xuyên không được đăng ký kèm lý do (gộp một lần mỗi lần chạy). *(Nguồn: 8.8, BR-M04-15)*
- **FR-009**: Thay đổi mẫu lặp (giờ, ngày trong tuần, ngày kết thúc) MUST có lý do và ngày áp dụng không sớm hơn hôm nay; hệ thống MUST hủy các buổi chưa diễn ra và chưa có điểm danh từ ngày áp dụng mà không còn khớp mẫu mới, với lý do "đổi mẫu", rồi sinh buổi theo mẫu mới trong CFG-M04-09. Buổi khớp cả mẫu cũ và mẫu mới (cùng thời điểm bắt đầu) MUST được giữ cùng đăng ký. Buổi đã có điểm danh MUST giữ nguyên. Người có đăng ký trên buổi bị hủy mà không phải người thường xuyên MUST được báo qua người đã đăng ký cho họ. *(Nguồn: BR-M04-21)*
- **FR-010**: Thêm người vào danh sách thường xuyên MUST đăng ký họ vào các buổi đã sinh chưa diễn ra (theo FR-015); bỏ người khỏi danh sách MUST hủy các đăng ký sẵn của họ ở buổi chưa diễn ra, giữ đăng ký thêm tay. *(Suy ra từ 8.8)*

#### C. Buổi hoạt động

- **FR-011**: Trưởng tầng MUST tạo được buổi lẻ (không từ mẫu) cho hoạt động đang hiệu lực, với thời điểm trong tương lai. *(Nguồn: UC-29)*
- **FR-012**: Lệnh "Dời buổi" (đổi thời điểm, địa điểm hoặc người phụ trách) MUST có lý do, chỉ áp cho buổi chưa bắt đầu; hệ thống MUST kiểm tra lại các điều kiện chặn của FR-015 cho mọi đăng ký, hủy đăng ký không còn đạt kèm lý do, và báo feature 005 (công việc "hoạt động"), feature 006 và 011 (với chuyến đi) thời điểm mới. *(Suy ra từ BR-M04-15, BR-M04-21)*
- **FR-013**: Lệnh "Hủy buổi" MUST có lý do và chỉ áp cho buổi chưa có điểm danh; mọi đăng ký của buổi chuyển Đã hủy; feature 005 nhận sự kiện để hủy công việc "hoạt động" Chưa đến hạn (feature 005 FR-008); với chuyến đi, feature 011 nhận sự kiện hủy chuyến. *(Nguồn: UC-29; feature 005, 011 bảng giao tiếp)*
- **FR-014**: Buổi có thời điểm kết thúc đã qua mà chưa Hoàn tất điểm danh vẫn ở Đã lên lịch tới hết ngày của buổi; trong thời gian đó trưởng tầng MAY hủy buổi với lý do "buổi không diễn ra" nếu chưa có lượt điểm danh nào. *(Suy ra từ 8.8 "điểm danh")*
- **FR-014a**: Khi hết ngày của buổi (24:00), với buổi trong viện còn ở Đã lên lịch, hệ thống MUST: gán kết quả "Không ghi nhận" cho mọi đăng ký còn hiệu lực chưa có kết quả; chuyển buổi sang Đã điểm danh kèm dấu "điểm danh không đủ" nếu đã có ít nhất một lượt điểm danh, hoặc sang "Không điểm danh" nếu chưa có lượt nào; báo người phụ trách buổi và trưởng tầng. "Không ghi nhận" không tạo chi phí, không là lượt tham gia, và làm ngày đó không là ngày tính của người cao tuổi (thuật ngữ "Ngày tính"). Người điểm danh sửa được "Không ghi nhận" thành Có mặt hoặc Vắng bằng đính chính (FR-031), gắn nhãn "ghi nhận muộn"; đính chính thành Có mặt ở hoạt động có thu phí tạo lượt cho feature 010. Feature 005 nhận sự kiện buổi kết thúc cho công việc "hoạt động". *(Suy ra từ 8.8, FR-030; Q-169)*

**Bảng trạng thái buổi hoạt động trong viện** *(8.8, BR-M04-21; quyền theo 4.4 dòng "Hoạt động, ngoài viện")*:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái đích | Ai được thực hiện | Điều kiện (chặn nếu không đạt) | Tác động tự động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Sinh từ mẫu | Đã lên lịch | Hệ thống (Bộ lập lịch) | Trong CFG-M04-09; chưa có buổi cùng (hoạt động, thời điểm bắt đầu) (FR-007) | Đăng ký sẵn người thường xuyên (FR-008) |
| (chưa có) | Tạo buổi lẻ | Đã lên lịch | Trưởng tầng | Hoạt động hiệu lực; thời điểm tương lai (FR-011) | — |
| Đã lên lịch | Dời buổi | Đã lên lịch | Trưởng tầng | Buổi chưa bắt đầu; có lý do (FR-012) | Kiểm tra lại đăng ký; báo feature 005 |
| Đã lên lịch | Hủy buổi | Đã hủy | Trưởng tầng (lý do); Hệ thống (đổi mẫu FR-009, ngừng hoạt động FR-004, khoanh vùng FR-021) | Chưa có điểm danh (FR-013) | Hủy mọi đăng ký; báo feature 005; báo người phụ trách và người được đăng ký qua người đăng ký |
| Đã lên lịch | Hoàn tất điểm danh | Đã điểm danh | Trưởng tầng, Nhân viên chăm sóc, Điều dưỡng (FR-027) | Từ thời điểm bắt đầu; mọi đăng ký còn hiệu lực có kết quả (FR-029) | Chi phí nháp cho lượt có mặt ở hoạt động có thu phí (FR-032) |
| Đã lên lịch | Hết ngày của buổi, đã có lượt điểm danh | Đã điểm danh (dấu "điểm danh không đủ") | Hệ thống | Chưa Hoàn tất điểm danh (FR-014a) | Đăng ký chưa có kết quả nhận "Không ghi nhận"; báo người phụ trách, trưởng tầng |
| Đã lên lịch | Hết ngày của buổi, chưa có lượt nào | Không điểm danh | Hệ thống | (FR-014a) | Như trên |
| Đã hủy; Không điểm danh | — | — | — | Trạng thái cuối; "Không điểm danh" sửa bằng đính chính từng đăng ký (FR-031) | — |
| Đã điểm danh | — | — | — | Trạng thái cuối; sai sót xử lý bằng đính chính điểm danh (FR-031) | — |

Chuyến đi có bảng trạng thái riêng ở mục H.

#### D. Đăng ký tham gia

- **FR-015**: Lệnh "Đăng ký" MUST bị chặn khi có ít nhất một điều kiện sau tại thời điểm lưu: (a) số đăng ký còn hiệu lực của buổi đã bằng số lượng tối đa; (b) người cao tuổi có chỉ định hạn chế hoạt động Hiệu lực bao trùm buổi (FR-024); (c) người cao tuổi có giường (hoặc khu nghỉ bán trú) thuộc vùng Đang khoanh vùng, hoặc địa điểm của buổi thuộc vùng Đang khoanh vùng (feature 007 FR-065); (d) người cao tuổi ở trạng thái Đang tiếp nhận hoặc trạng thái cuối; (e) buổi không ở Đã lên lịch hoặc đã bắt đầu (trừ thêm tại lúc điểm danh buổi trong viện, FR-028); với chuyến đi, đăng ký chỉ được tới khi người đầu tiên được điểm danh rời viện, sau đó không đăng ký thêm và "đi sau" (FR-042) chỉ áp cho người đã đăng ký (Q-171); (f) các điều kiện của FR-016; (g) người cao tuổi mang dấu "nghi nhiễm" (feature 007 FR-060) và buổi thuộc hoạt động nhóm hoặc là chuyến đi; hoạt động cá nhân không bị chặn. *(Nguồn: BR-M04-15, BR-M05-11, 5.5; (g): Clarification 2026-09-27, Q-165)*
- **FR-016**: Đăng ký MUST bị chặn khi người cao tuổi đã có đăng ký còn hiệu lực ở buổi khác chồng thời gian, hoặc là người bán trú mà buổi bắt đầu trước giờ đến theo lịch hay rơi vào ngày không có lịch đến. Buổi kết thúc sau giờ về theo lịch MUST xử lý theo FR-016a. *(Suy ra từ 8.2, 3.3; Clarification 2026-09-27, Q-167)*
- **FR-016a**: Người bán trú MUST đăng ký được buổi (kể cả chuyến đi) kết thúc sau giờ về theo lịch của ngày đó chỉ khi Hành chính đã ghi nhận **đồng ý về muộn** của người đại diện cho đúng buổi đó (qua cổng người thân hoặc bản ký scan), gồm người đại diện, buổi, giờ về dự kiến mới, người ghi nhận, thời điểm. Khi đăng ký được lưu, giờ về dự kiến của ngày đó MUST tự dời tới giờ kết thúc của buổi (với chuyến đi: giờ về dự kiến của chuyến, và theo mọi lần gia hạn FR-043). Spec này gửi giờ về dự kiến mới cho feature 005, nơi quản lý có mặt theo ngày của bán trú (3.4); feature 003 (chỗ khu nghỉ, Q-47), 010 (tính phí buổi) và 011 (suất ăn) lấy giờ về theo ngày từ feature 005, không nhận riêng từ spec này. Spec không tạo chi phí cho phần giờ thêm; feature 010 tính theo giờ về theo ngày với quy tắc buổi hiện có (Q-138, Q-140). Người đại diện rút được đồng ý trước giờ bắt đầu của buổi; khi đó đăng ký bị hủy và giờ về dự kiến trở lại theo lịch. Từ giờ bắt đầu của buổi, đồng ý chuyển "Đã dùng" và không rút được. *(Clarification 2026-09-27, Q-167; chủ sở hữu giờ về theo ngày và "Đã dùng": Q-176)*
- **FR-017**: Người thực hiện đăng ký, hủy đăng ký MUST là Trưởng tầng, Nhân viên chăm sóc hoặc Điều dưỡng (Q-214), với người cao tuổi trong phạm vi dữ liệu của mình (feature 002). Hủy đăng ký MUST có lý do (ví dụ người cao tuổi không muốn tham gia) và chỉ trước thời điểm bắt đầu. *(Nguồn: 4.4, UC-29; feature 000 lý do bắt buộc)*
- **FR-018**: Hệ thống MUST hiển thị cảnh báo, không chặn, khi người được đăng ký: có cờ nguy cơ ngã hoặc đi lạc (feature 001); có mức chăm sóc, loại hình lưu trú hoặc tầng ngoài đối tượng của hoạt động; mang dấu "nghi nhiễm" khi đăng ký hoạt động cá nhân (với hoạt động nhóm và chuyến đi thì bị chặn theo FR-015 (g)); có cảnh báo hoặc sự cố mức Trung bình trở lên đang mở. Người đăng ký MUST xác nhận; đăng ký lưu dấu "đã xác nhận cảnh báo" và danh sách cảnh báo đã hiển thị. Thông tin sức khỏe chi tiết chỉ hiển thị cho người có quyền xem (19.3). *(Suy ra từ 8.8 "cờ điều kiện sức khỏe", BR-M01-09)*
- **FR-019**: Mỗi đăng ký MUST lưu: buổi, người cao tuổi, nguồn (thường xuyên / thêm tay / thêm lúc điểm danh), người đăng ký, thời điểm, trạng thái (Đã đăng ký / Đã hủy / Đã điểm danh), lý do hủy, cảnh báo đã xác nhận. Đăng ký chỉ đổi trạng thái qua lệnh Hủy đăng ký, hủy tự động (FR-009, FR-012, FR-016a, FR-021, FR-021a, FR-022, FR-025), điểm danh hoặc "Không ghi nhận" (FR-014a), theo bảng trạng thái đăng ký. *(Nguồn: 1.5 nhóm 2)*
- **FR-020**: Hệ thống MUST gợi ý người tham gia cho một buổi còn chỗ, gồm người cao tuổi trong phạm vi người xem, đạt các điều kiện chặn của FR-015, loại trừ người có sở thích "không thích" khớp (FR-052). Thứ tự xếp hạng: (1) người thuộc đối tượng của hoạt động xếp trước người ngoài đối tượng; (2) trong cùng nhóm, người có sở thích "thích" khớp nhóm sở thích của loại hoạt động (FR-051) xếp trước; (3) trong cùng mức, người có nhiều ngày tính hơn kể từ lần gần nhất có mặt ở hoạt động nhóm xếp trước; (4) còn bằng nhau thì theo họ tên. Gợi ý chỉ để chọn; đăng ký vẫn qua FR-015. *(Nguồn: 8.8 "Hệ thống gợi ý người tham gia", 8.10)*
- **FR-021**: Khi một vùng chuyển Đang khoanh vùng (feature 007), trong cùng một lần hệ thống MUST: hủy mọi buổi trong viện chưa có điểm danh có địa điểm trong vùng; hủy mọi đăng ký chưa diễn ra của người cao tuổi có giường trong vùng; lý do "khoanh vùng"; báo người phụ trách buổi và trưởng tầng. Khi vùng được gỡ, buổi và đăng ký đã hủy MUST NOT tự khôi phục. Chuyến đi đang diễn ra không bị ảnh hưởng (Edge Cases). *(Suy ra từ BR-M05-11, BR-M05-12; theo cách xử lý lượt thăm của Q-120; Clarification 2026-09-27, Q-164)*
- **FR-021a**: Khi người cao tuổi được gắn dấu "nghi nhiễm" (feature 007 FR-060), trong cùng một lần hệ thống MUST hủy mọi đăng ký chưa diễn ra của người đó ở hoạt động nhóm và chuyến đi, lý do "nghi nhiễm", báo người phụ trách buổi và trưởng tầng; người đó được bỏ qua khi đăng ký sẵn từ mẫu (FR-008). Người đã Được đi ở chuyến chưa rời viện MUST chuyển "Không đi" với lý do "nghi nhiễm", như FR-037. Nếu người đó đang trong chuyến đi đã rời viện, trưởng đoàn và trưởng tầng MUST được báo; trạng thái không tự đổi. Khi dấu được gỡ (sự cố đóng), đăng ký đã hủy MUST NOT tự khôi phục; người thường xuyên được đăng ký sẵn lại từ lần sinh buổi kế tiếp. *(Clarification 2026-09-27, Q-165)*
- **FR-022**: Khi người cao tuổi chuyển trạng thái cuối, mọi đăng ký chưa diễn ra MUST chuyển Đã hủy với lý do trạng thái cuối, và người đó MUST bị gỡ khỏi mọi danh sách người tham gia thường xuyên. *(Suy ra từ 5.6 "Hủy mọi lịch tương lai")*

**Bảng trạng thái đăng ký buổi trong viện** *(chuyến đi dùng bảng trạng thái người tham gia ở mục H)*:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái đích | Ai được thực hiện | Điều kiện (chặn nếu không đạt) | Tác động tự động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Đăng ký; đăng ký sẵn từ mẫu; thêm lúc điểm danh | Đã đăng ký (thêm lúc điểm danh: Đã điểm danh ngay) | Trưởng tầng, Nhân viên chăm sóc, Điều dưỡng; Hệ thống (FR-008) | FR-015, FR-016, FR-016a | Báo feature 005 sinh công việc "hoạt động" |
| Đã đăng ký | Hủy đăng ký | Đã hủy | Trưởng tầng, Nhân viên chăm sóc, Điều dưỡng | Trước giờ bắt đầu; có lý do (FR-017) | Báo feature 005 |
| Đã đăng ký | Hủy tự động (đổi mẫu, dời buổi, khoanh vùng, nghi nhiễm, chỉ định hạn chế, trạng thái cuối, rút đồng ý về muộn) | Đã hủy | Hệ thống | FR-009, FR-012, FR-021, FR-021a, FR-022, FR-025, FR-016a | Báo theo FR-071 |
| Đã đăng ký | Ghi kết quả điểm danh | Đã điểm danh (Có mặt / Vắng) | Trưởng tầng, Nhân viên chăm sóc, Điều dưỡng (FR-027) | Từ giờ bắt đầu | Chi phí nháp nếu Có mặt ở hoạt động có thu phí (FR-032) |
| Đã đăng ký | Hết ngày của buổi | Đã điểm danh (Không ghi nhận) | Hệ thống | FR-014a | Báo người phụ trách, trưởng tầng |
| Đã hủy; Đã điểm danh | — | — | — | Trạng thái cuối; kết quả sửa bằng đính chính (FR-031) | — |

**Bảng trạng thái đồng ý về muộn của bán trú** *(FR-016a, Q-167, Q-176)*:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái đích | Ai được thực hiện | Điều kiện (chặn nếu không đạt) | Tác động tự động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Ghi nhận đồng ý | Hiệu lực | Hành chính (bản ký) hoặc người đại diện qua cổng (feature 012) | Người đồng ý là người đại diện tại thời điểm đồng ý; đúng một buổi | Cho phép đăng ký buổi (FR-016a) |
| Hiệu lực | Rút đồng ý | Đã rút | Người đại diện (qua cổng) hoặc Hành chính ghi theo yêu cầu | Trước giờ bắt đầu của buổi | Hủy đăng ký; giờ về dự kiến trở lại theo lịch; báo theo FR-071 |
| Hiệu lực | Tới giờ bắt đầu của buổi | Đã dùng | Hệ thống | Đăng ký còn hiệu lực | — |
| Hiệu lực | Đăng ký hoặc buổi bị hủy | Hết hiệu lực | Hệ thống | — | Giờ về dự kiến trở lại theo lịch |
| Đã rút; Đã dùng; Hết hiệu lực | — | — | — | Trạng thái cuối | — |

#### E. Chỉ định hạn chế hoạt động

- **FR-023**: Bác sĩ MUST gắn được chỉ định hạn chế hoạt động (nhóm 2) cho người cao tuổi gồm: phạm vi, ngày giờ bắt đầu, ngày giờ kết thúc (hoặc tới khi gỡ), lý do, bác sĩ gắn. Phạm vi MUST chọn được ít nhất: mọi hoạt động; mọi hoạt động nhóm; hoạt động ngoài viện; theo loại hoạt động cụ thể. Gỡ hoặc rút ngắn chỉ định MUST là lệnh có lý do của Bác sĩ; chỉ định không bị sửa hay xóa. Chỉ định hạn chế là cờ "không đủ điều kiện" của BR-M04-15 và là "chỉ định hạn chế" của BR-M04-22. *(Nguồn: BR-M04-15, BR-M04-22, 2.3 dòng Bác sĩ; Clarification 2026-09-27, Q-162)*
- **FR-023a**: Điều dưỡng MUST gắn được **chỉ định tạm** cho người cao tuổi trong phạm vi dữ liệu của mình, với cùng các trường và phạm vi như FR-023; thời điểm kết thúc của chỉ định tạm MUST NOT muộn hơn thời điểm gắn cộng CFG-M04-14 (đề xuất, mặc định \[24 giờ\]). Chỉ định tạm có tác động chặn và hủy đăng ký như chỉ định chính thức (FR-015 (b), FR-025, FR-037) và được tính là chỉ định hạn chế ở FR-054. *(Clarification 2026-09-27, Q-162)*
- **FR-023b**: Bác sĩ MUST "Xác nhận" được chỉ định tạm (thành chính thức, MAY đổi phạm vi và thời gian, bắt buộc lý do khi đổi) hoặc "Gỡ" chỉ định tạm kèm lý do. Khi chỉ định tạm được gắn, bác sĩ trực (2.4) MUST được báo; không có bác sĩ trực thì mọi Bác sĩ có tài khoản Hoạt động được báo song song, theo cách Q-74. Tới thời điểm kết thúc của chỉ định tạm mà chưa được xác nhận, chỉ định MUST tự chuyển Hết hiệu lực với lý do "chỉ định tạm không được xác nhận", và Điều dưỡng đã gắn, trưởng tầng MUST được báo. Đăng ký đã bị hủy do chỉ định tạm MUST NOT tự khôi phục khi chỉ định tạm bị gỡ hoặc hết hiệu lực. *(Clarification 2026-09-27, Q-162)*
- **FR-023c**: Mỗi người cao tuổi MAY có nhiều chỉ định cùng lúc; chỉ định Điều dưỡng gắn khi đã có chỉ định chính thức cùng phạm vi bao trùm khoảng đó MUST bị chặn vì không cần thiết. Điều dưỡng MUST NOT gắn chỉ định tạm cùng phạm vi cho cùng người trong CFG-M04-14 kể từ khi chỉ định tạm trước Hết hiệu lực vì không được xác nhận; khi đó hệ thống hướng dẫn báo bác sĩ. *(Suy ra từ FR-023, FR-023a; Q-162)*

**Bảng trạng thái chỉ định hạn chế hoạt động** *(BR-M04-15, BR-M04-22, Q-162)*:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái đích | Ai được thực hiện | Điều kiện (chặn nếu không đạt) | Tác động tự động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Gắn chỉ định | Hiệu lực | Bác sĩ | Có phạm vi, thời gian bắt đầu, lý do (FR-023) | Hủy đăng ký bị bao trùm, báo (FR-025, FR-037) |
| (chưa có) | Gắn chỉ định tạm | Tạm | Điều dưỡng (trong phạm vi) | Như trên; kết thúc không muộn hơn CFG-M04-14 kể từ lúc gắn (FR-023a, FR-023c) | Như trên; báo bác sĩ trực (FR-023b) |
| Tạm | Xác nhận | Hiệu lực | Bác sĩ | Lý do nếu đổi phạm vi hoặc thời gian | Áp phạm vi, thời gian mới; hủy thêm đăng ký nếu mở rộng |
| Tạm | Gỡ | Đã gỡ | Bác sĩ | Có lý do | Không khôi phục đăng ký đã hủy |
| Tạm | Tới thời điểm kết thúc | Hết hiệu lực | Hệ thống | Chưa được xác nhận | Báo Điều dưỡng đã gắn, trưởng tầng (FR-023b) |
| Hiệu lực | Rút ngắn / mở rộng | Hiệu lực | Bác sĩ | Có lý do | Mở rộng: hủy thêm đăng ký bị bao trùm (FR-025) |
| Hiệu lực | Gỡ | Đã gỡ | Bác sĩ | Có lý do | Không khôi phục đăng ký đã hủy |
| Hiệu lực | Tới thời điểm kết thúc | Hết hiệu lực | Hệ thống | Có thời điểm kết thúc | — |
| Đã gỡ; Hết hiệu lực | — | — | — | Trạng thái cuối | — |

- **FR-024**: Một chỉ định bao trùm một buổi khi khoảng hiệu lực giao thời gian của buổi và phạm vi khớp: "mọi hoạt động" khớp mọi buổi; "mọi hoạt động nhóm" khớp buổi của loại có dấu hoạt động nhóm; "hoạt động ngoài viện" khớp chuyến đi; "theo loại" khớp buổi của loại đó. Người không có quyền xem sức khỏe (19.3) chỉ thấy phạm vi và thời gian của chỉ định, không thấy lý do; giới hạn này MUST áp ở mọi nơi chỉ định xuất hiện: thông báo chặn khi đăng ký, đánh giá khả năng tham gia, danh sách chuyến, thông báo của FR-071, hồ sơ tinh thần, hồ sơ người cao tuổi. Người thân không thấy chỉ định (FR-068). *(Suy ra từ BR-M04-15, 19.3)*
- **FR-025**: Khi chỉ định được gắn hoặc mở rộng, hệ thống MUST hủy các đăng ký chưa diễn ra bị chỉ định bao trùm với lý do "chỉ định hạn chế" và báo trưởng tầng, người phụ trách buổi. Điểm danh đã ghi MUST NOT bị ảnh hưởng. Với chuyến đi đã rời viện, hệ thống MUST báo trưởng đoàn và trưởng tầng, không tự chuyển trạng thái người cao tuổi. *(Suy ra từ BR-M04-15)*
- **FR-026**: Hồ sơ người cao tuổi MUST hiển thị chỉ định hạn chế Hiệu lực và lịch sử chỉ định cho người có quyền xem hồ sơ trong phạm vi. *(Nguồn: 1.3, 1.5 nhóm 2)*

#### F. Điểm danh buổi trong viện

- **FR-027**: Điểm danh MUST do Trưởng tầng, Nhân viên chăm sóc hoặc Điều dưỡng (Q-214) thực hiện: người phụ trách của buổi, hoặc người có người cao tuổi được điểm danh trong phạm vi dữ liệu (feature 002). *(Nguồn: 4.4, UC-29)*
- **FR-028**: Điểm danh MUST ghi được từ thời điểm bắt đầu của buổi. Với mỗi người, kết quả là Có mặt hoặc Vắng kèm lý do (từ chối, sức khỏe, đang vắng mặt, không đến (bán trú), khác). Người đang Tạm vắng, Điều trị tại bệnh viện, Hoạt động bên ngoài, hoặc bán trú chưa điểm danh đến, MUST được hiển thị sẵn kết quả "Vắng – đang vắng mặt" / "Vắng – không đến". Người chưa đăng ký MUST thêm được lúc điểm danh nếu đạt FR-015 (a) → (d) và (g), với nguồn "thêm lúc điểm danh". *(Nguồn: 8.8 "người tham gia; điểm danh")*
- **FR-029**: Với người Có mặt, điểm danh MUST ghi mức độ tham gia (tích cực / bình thường / thụ động / bỏ giữa chừng) và MAY ghi mức giao tiếp (chủ động / khi được hỏi / ít giao tiếp), tình trạng sau hoạt động và ghi chú. "Hoàn tất điểm danh" MUST chỉ được thực hiện khi mọi đăng ký còn hiệu lực đã có kết quả. Lượt Có mặt với mức "bỏ giữa chừng" vẫn là Có mặt: tạo chi phí ở hoạt động có thu phí (FR-032), được tính là tham gia hoạt động nhóm (FR-054) và là lượt tiếp xúc (FR-033). *(Nguồn: 8.8 "kết quả", 8.10 "giao tiếp; mức độ tham gia"; ERD DIEM_DANH; "bỏ giữa chừng": Q-173)*
- **FR-030**: Điểm danh ghi sau thời điểm kết thúc của buổi quá CFG-M04-06 (mặc định \[2 giờ\]) MUST gắn nhãn "ghi nhận muộn"; bản ghi ngoại tuyến so theo thời điểm ghi trên thiết bị. Tới thời điểm kết thúc cộng CFG-M04-06 mà buổi chưa Hoàn tất điểm danh, người phụ trách và trưởng tầng MUST được nhắc một lần. *(Suy ra từ BR-M04-12, CFG-M04-06, DBR-25)*
- **FR-031**: Điểm danh là nhóm 3: MUST NOT sửa, xóa; sai sót xử lý bằng đính chính theo feature 000 (bản ghi gắn tầng của buổi; buổi toàn viện gắn tầng của người cao tuổi). Khi đính chính làm một lượt Có mặt ở hoạt động có thu phí thành Vắng hoặc Hủy ghi nhận, feature 010 MUST nhận sự kiện hủy lượt điểm danh; đính chính thêm lượt Có mặt MUST tạo lượt mới cho feature 010. *(Nguồn: 1.5 nhóm 3, BR-M04-14 tương tự, DBR-15, DBR-23)*
- **FR-031a**: Bản ghi điểm danh rời viện và điểm danh về (chuyến đi) MUST chỉ được đính chính bởi Trưởng tầng của chuyến, kèm lý do; bản đính chính MUST NOT đổi người cao tuổi hay loại bản ghi. (a) Đính chính thời điểm thực tế: hệ thống gửi thời điểm mới cho feature 001 (lịch sử trạng thái), 005, 006, 011 để tính lại theo thời điểm đúng. (b) "Hủy ghi nhận" bản ghi rời viện (người bị ghi đi nhầm): chỉ khi người đó chưa có bản ghi về và chuyến chưa Đã về; hệ thống yêu cầu feature 001 chuyển người đó về Đang lưu trú với căn cứ "đính chính điểm danh", gửi sự kiện hủy cho feature 005 (sinh lại công việc), 006 (tính lại liều), 010 (hủy lượt có mặt), 011 (suất ăn); người tham gia trở về "Được đi" nếu chuyến còn cho đi sau, ngược lại "Không đi". Nếu sau lần Hủy ghi nhận chuyến không còn bản ghi rời viện nào, chuyến MUST quay về Đã lên lịch, đăng ký mở lại theo quy tắc trước khi đi (Q-171), và feature 019 được báo để lịch xe quay về Đã đặt (Q-240). (c) "Hủy ghi nhận" bản ghi về (ghi về nhầm người chưa về): chỉ khi chuyến chưa Đã về; người đó trở lại "Đã rời viện" và Hoạt động bên ngoài; feature 005, 006, 011 nhận sự kiện. *(Suy ra từ 1.5 nhóm 3, feature 000 đính chính; Q-170)*
- **FR-032**: Mỗi lượt điểm danh Có mặt ở buổi của hoạt động có thu phí MUST được cung cấp cho feature 010 làm bản ghi nguồn (một lượt), để tạo chi phí ở trạng thái Nháp với dịch vụ của hoạt động; lượt Vắng và "Không ghi nhận" MUST NOT tạo chi phí. Với chuyến đi, lượt có mặt là bản ghi điểm danh rời viện (FR-041): khoản chi phí trỏ về bản ghi đó, và "ngày của buổi" của feature 010 là ngày của thời điểm rời viện thực tế, kể cả khi chuyến về sau nửa đêm. *(Nguồn: BR-M04-18, BR-M11-01, DBR-15; feature 010 bảng nguồn)*
- **FR-033**: Điểm danh Có mặt MUST được cung cấp cho feature 007 để lập danh sách tiếp xúc (người cùng có mặt ở một buổi trong CFG-M05-07, kèm buổi và thời điểm) và cho feature 012. "Số hoạt động đã tham gia" cho bản tin và cổng (loại thông tin "chung") MUST là số lượt Có mặt của người cao tuổi trong kỳ, gồm hoạt động nhóm, hoạt động cá nhân và chuyến đi (tính theo điểm danh rời viện). *(Nguồn: BR-M05-10, feature 012 FR-052; định nghĩa số lượt: Q-173)*

#### G. Hoạt động ngoài viện – chuẩn bị

- **FR-034**: Chuyến đi MUST có thêm: điểm đến, thời điểm rời dự kiến, thời điểm về dự kiến, phương tiện (xe của viện hoặc khác, kèm mô tả; FR-040), trưởng đoàn, danh sách người đi cùng. Khoảng đi dự kiến (giờ rời, giờ về) và danh sách người tham gia MUST được cung cấp cho feature 006 (khoảng đi, feature 006 FR-048) và 011 (tính suất, Q-144) khi chuyến được lên lịch và mỗi lần một trong các thông tin đó thay đổi: dời, gia hạn, đăng ký, hủy đăng ký, chuyển "Không đi", đi sau, hủy chuyến. *(Nguồn: 8.9; feature 006 FR-048, feature 011 FR-034)*
- **FR-035**: Trưởng tầng MUST phân công trưởng đoàn và người đi cùng trước khi điểm danh rời viện; không có trưởng đoàn thì điểm danh rời viện MUST bị chặn. Trưởng đoàn MUST có vai trò Trưởng tầng, Nhân viên chăm sóc hoặc Điều dưỡng (Q-214); người đi cùng MAY có bất kỳ vai trò nhân viên nào. Trưởng đoàn và người đi cùng MUST có tài khoản Hoạt động và có tên trong ca chồng thời gian chuyến đi (feature 008); không đạt thì phân công bị chặn. Khi đổi ca hoặc nghỉ đột xuất đã duyệt làm trưởng đoàn hoặc người đi cùng rời ca đó (feature 015 FR-024 (g), FR-029 (h), Q-185), nhiệm vụ của họ bị gỡ và Trưởng tầng phụ trách chuyến được báo để phân công lại; nhiệm vụ không tự chuyển cho người nhận ca, và điểm danh rời viện vẫn bị chặn khi chưa có trưởng đoàn. Việc phân công MUST cho trưởng đoàn và người đi cùng phạm vi dữ liệu là người tham gia chuyến trong thời gian chuyến (feature 002 FR-031); người đi cùng không có vai trò Trưởng tầng, Nhân viên chăm sóc hoặc Điều dưỡng chỉ được thực hiện "Báo thiếu người" (FR-046) trong các lệnh của chuyến (Q-214). Khi giờ về được gia hạn vượt ca của trưởng đoàn hoặc một người đi cùng, trưởng tầng MUST được báo để phân công bổ sung; phân công hiện có không bị hủy. Trưởng đoàn và người đi cùng giữ nhiệm vụ tới khi chuyến Đã về, kể cả khi ca của họ đã kết thúc; bàn giao ca không chuyển nhiệm vụ này. *(Nguồn: 8.9 "phân công người đi cùng", 2.4 "Trưởng đoàn", 4.4; Clarification 2026-09-27, Q-161; giữ nhiệm vụ qua hết ca: Q-172; Điều dưỡng: Q-214)*
- **FR-035a**: Trong khi chuyến Đang đi, trưởng đoàn hoặc trưởng tầng MUST "Chuyển trưởng đoàn" được cho một người đi cùng có vai trò Trưởng tầng, Nhân viên chăm sóc hoặc Điều dưỡng (Q-214), kèm lý do; người trưởng đoàn cũ trở thành người đi cùng (hoặc rời chuyến nếu được ghi rời), lịch sử lưu lại. Nếu không còn người đi cùng đủ điều kiện, trưởng tầng MUST phân công bổ sung người đủ điều kiện; trong lúc chờ, trưởng tầng là người chịu trách nhiệm chuyến (dấu "trưởng đoàn tạm từ xa") và Quản lý viện được báo. *(Suy ra từ 2.4 "Trưởng đoàn", Q-161; Q-172)*
- **FR-036**: Trước khi điểm danh rời viện, mỗi người đăng ký MUST có kết quả đánh giá khả năng tham gia: "Đạt" hoặc "Không đạt" kèm lý do. Khi đánh giá, hệ thống MUST hiển thị: chỉ định hạn chế, cờ nguy cơ, dấu nghi nhiễm, cảnh báo và sự cố đang mở (gồm cảnh báo vượt ngưỡng chỉ số, chi tiết chỉ với người có quyền xem sức khỏe), liều thuốc trong khoảng đi. Người có cờ nguy cơ đi lạc được đánh giá Đạt MUST có một người đi cùng được chỉ định kèm riêng; mỗi người đi cùng kèm riêng tối đa một người. Người Không đạt chuyển "Không đi", đăng ký kết thúc với lý do. Người thực hiện đánh giá MUST là Trưởng tầng của tầng người cao tuổi đang ở, kể cả với chuyến toàn viện (mỗi người do trưởng tầng của mình đánh giá; trưởng tầng tổ chức xem được tổng hợp); trưởng đoàn không có vai trò Trưởng tầng không được đánh giá. *(Nguồn: 8.9 "đánh giá khả năng tham gia"; Clarification 2026-09-27, Q-161; chuyến toàn viện, kèm riêng một người: Q-172, Q-175)*
- **FR-037**: Chỉ định hạn chế phạm vi "hoạt động ngoài viện" hoặc "mọi hoạt động" gắn sau khi người đã được đánh giá Đạt MUST đưa người đó về "Không đi" nếu chưa rời viện, và báo trưởng đoàn, trưởng tầng. *(Suy ra từ BR-M04-15)*
- **FR-038**: Với mỗi người được đánh giá Đạt, hệ thống MUST lấy từ feature 006 danh sách liều trong khoảng đi dự kiến. Điểm danh rời viện của người đó MUST bị chặn khi còn liều trong khoảng đi mà chưa có lần "Giao thuốc mang theo" (feature 006 FR-048a) và chưa có quyết định "không mang thuốc". Quyết định "không mang thuốc" là lệnh của feature 006, do Điều dưỡng ghi với người cao tuổi, chuyến, lý do; liều trong khoảng đi khi đó chuyển Tạm dừng như Tạm vắng không mang thuốc của feature 006; spec này chỉ đọc kết quả để xét điều kiện. Danh sách thuốc cần mang MUST hiển thị cho điều dưỡng phụ trách từ khi người được đánh giá Đạt. *(Nguồn: 8.9 "kiểm tra thuốc cần mang", BR-M04-16; feature 006 User Story 2 kịch bản 3; chủ sở hữu lệnh: Q-171)*
- **FR-038a**: Khi chuyến bị hủy, hoặc một người đã có lần giao thuốc mang theo chuyển "Không đi", hệ thống MUST báo feature 006 và điều dưỡng phụ trách để ghi Nhận lại thuốc (Q-58). Khi chuyến bị dời: nếu ngày không đổi, đánh giá khả năng giữ nguyên nhưng danh sách liều trong khoảng đi MUST được lấy lại và FR-038 xét lại lúc rời viện; nếu ngày đổi, mọi đánh giá MUST quay lại chưa đánh giá (người tham gia về "Đã đăng ký"), và điều dưỡng phụ trách được báo nếu đã có lần giao thuốc. *(Suy ra từ 8.9, Q-58; Q-171)*
- **FR-039**: Danh sách chuyến MUST hiển thị cho trưởng đoàn, người đi cùng và trưởng tầng: người tham gia và trạng thái của từng người, người đi cùng được chỉ định kèm riêng, số liên lạc người liên hệ chính (chỉ với trưởng đoàn và trưởng tầng), thuốc mang theo đã giao (theo quyền xem thuốc của feature 006), cờ nguy cơ, và thẻ thông tin khẩn cấp theo quyền của feature 007. *(Nguồn: 8.9 "Trong chuyến đi: theo dõi danh sách", BR-M05-13)*

#### H. Rời viện, trong chuyến và trở về

- **FR-040**: Lệnh "Điểm danh rời viện" MUST do trưởng đoàn thực hiện (trưởng tầng thay được khi trưởng đoàn không có mặt, kèm lý do), cho từng người hoặc nhiều người trong một lần, không sớm hơn thời điểm rời dự kiến trừ CFG-M04-13 (đề xuất, mặc định \[1 giờ\]). Người được điểm danh MUST đã được đánh giá Đạt, đạt FR-038, và đang ở Đang lưu trú tại thời điểm lệnh (bán trú: đã điểm danh đến trong ngày). Người Được đi nhưng đang Tạm vắng, Điều trị tại bệnh viện hoặc chưa đến MUST bị chặn và được ghi "Vắng – đang vắng mặt" khi chuyến bắt đầu. **(Bổ sung 2026-09-28, Q-220)** Chuyến có phương tiện là xe của viện MUST có lịch xe Đã đặt bao trùm giờ rời dự kiến (feature 019 FR-019) trước khi điểm danh rời viện; không có thì lệnh bị chặn với lý do "chưa có lịch xe". Với chuyến có phương tiện là xe của viện (FR-034), và chỉ với chuyến đó, spec này báo feature 019 khi chuyến được lên lịch, dời, hủy, có người đầu tiên rời viện, gia hạn giờ về, Hủy ghi nhận bản ghi rời viện cuối cùng và khi kết thúc điểm danh về; mọi tác động "báo feature 019" trong bảng trạng thái chuyến đi chỉ áp cho chuyến này (feature 019 FR-018; Q-231, Q-236). Gia hạn giờ về MUST NOT bị chặn vì lịch xe chồng với lịch kế tiếp của cùng xe. *(Nguồn: BR-M04-16, UC-30, 5.5 (chỉ Đang lưu trú → Hoạt động bên ngoài); Q-171; lịch xe: 7.7, BR-M03-16, Q-220, Q-231, Q-236)*
- **FR-041**: Khi một người được điểm danh rời viện, trong cùng một lần hệ thống MUST: (a) ghi bản ghi điểm danh rời viện (nhóm 3) với thời điểm thực tế; (b) yêu cầu feature 001 chuyển người đó sang Hoạt động bên ngoài, người thực hiện là người điểm danh, căn cứ là chuyến đi; (c) cung cấp cho feature 006 thời điểm rời thực tế và giờ về dự kiến để liều trong khoảng chuyển Mang theo (BR-M07-03); liều Mang theo do Điều dưỡng ghi nhận theo feature 006 FR-027 (Q-54), không do người đi cùng khác ghi; (d) cung cấp cho feature 005 để hủy công việc Chưa đến hạn trong khoảng đi (feature 005 FR-020); (e) cung cấp cho feature 010 lượt có mặt nếu có thu phí (FR-032); (f) cung cấp cho feature 011 thời điểm rời thực tế. Người đăng ký không được điểm danh rời viện khi chuyến bắt đầu (người Không đi, hoặc không có mặt lúc xuất phát) MUST có kết quả "Vắng" kèm lý do. *(Nguồn: BR-M04-16, 5.6 dòng "Đang lưu trú → Hoạt động bên ngoài")*
- **FR-042**: Chuyến có ít nhất một người đã rời viện MUST chuyển Đang đi. Người đăng ký chưa rời viện MAY được điểm danh rời viện muộn (đi sau) tới thời điểm về dự kiến. *(Suy ra từ 8.9)*
- **FR-043**: Trưởng đoàn hoặc trưởng tầng MUST "Gia hạn giờ về" được khi chuyến Đang đi, kèm lý do; giờ về dự kiến mới được lưu cùng lịch sử, mốc quá giờ tính lại, feature 006 và 011 nhận giờ mới. *(Suy ra từ BR-M04-17; theo cách của feature 004 gia hạn dự kiến trở lại)*
- **FR-044**: Lệnh "Điểm danh về" MUST do trưởng đoàn hoặc trưởng tầng thực hiện, cho từng người hoặc nhiều người, khi người đó đã có mặt tại viện. Việc người đó có mặt tại viện do người thực hiện lệnh xác nhận bằng chính lệnh và chịu trách nhiệm; hệ thống không kiểm tra vị trí. Khi một người được điểm danh về, trong cùng một lần hệ thống MUST: ghi bản ghi điểm danh về với thời điểm thực tế; yêu cầu feature 001 chuyển người đó về Đang lưu trú; cung cấp cho feature 005, 006, 011 thời điểm về thực tế (sinh lại công việc, tính lại liều Mang theo chưa tới giờ theo Q-60, suất ăn). Người về sớm một mình được điểm danh về riêng; chuyến vẫn Đang đi. *(Nguồn: 8.9 "Khi trở về: điểm danh; cập nhật trạng thái", BR-M04-16)*
- **FR-045**: Người tham gia chuyển Điều trị tại bệnh viện (chuyển viện, feature 004/007) hoặc Qua đời (feature 004) trong khi chuyến Đang đi MUST được đánh dấu "Rời đoàn" kèm trạng thái mới, không cần điểm danh về và không bị tính là thiếu người. Hệ thống MUST NOT có lệnh cho người tham gia rời đoàn để về với người thân, vì 5.5 không có chuyển Hoạt động bên ngoài → Tạm vắng; người đó phải được điểm danh về trước, rồi mới Cho tạm vắng theo quy trình đón của feature 004, 012 (14.3). *(Nguồn: 5.5; Clarification 2026-09-27, Q-166)*
- **FR-046**: Trong khi chuyến Đang đi, trưởng đoàn, bất kỳ người đi cùng nào (kể cả Điều dưỡng, vì ghi sự cố là quyền của mọi nhân viên theo 4.4 dòng "Sự cố, khẩn cấp") hoặc trưởng tầng (khi nhận tin báo qua điện thoại) MUST "Báo thiếu người" được cho một người đã rời viện, kèm địa điểm và thời điểm thấy lần cuối, mô tả; hệ thống MUST ngay trong lần đó chuyển người đó sang "Thiếu khi về" và yêu cầu feature 007 tạo sự cố khẩn cấp theo FR-048. *(Suy ra từ BR-M04-17, BR-M05-06)*
- **FR-046a**: "Báo thiếu người" và "Kết thúc điểm danh về" có người thiếu MUST là lệnh bắt buộc trực tuyến theo ngoại lệ của 8.6 (tạo sự cố khẩn cấp). Khi thiết bị mất kết nối, lệnh MUST NOT được ghi tạm như thành công; thiết bị hiển thị ngay hướng dẫn gọi trưởng tầng, và trưởng tầng thực hiện lệnh thay theo FR-046, FR-047. Điểm danh rời viện và điểm danh về thông thường MAY ghi tạm và đồng bộ sau (Q-01), so theo thời điểm ghi trên thiết bị (DBR-25). *(Nguồn: 8.6 "(Bổ sung)", Q-01, BR-M04-17)*
- **FR-047**: Lệnh "Kết thúc điểm danh về" MUST do trưởng đoàn hoặc trưởng tầng thực hiện; mọi người đã rời viện chưa được điểm danh về và không "Rời đoàn" MUST chuyển "Thiếu khi về", và với mỗi người chưa có sự cố thiếu người từ FR-046, hệ thống MUST yêu cầu feature 007 tạo sự cố khẩn cấp theo FR-048. Trạng thái người cao tuổi MUST giữ Hoạt động bên ngoài cho tới lệnh phù hợp (điểm danh về muộn, chuyển viện, ghi nhận qua đời) (5.5). Người "Thiếu khi về" MUST được điểm danh về muộn khi đã có mặt tại viện; feature 007 nhận diễn biến "đã tìm thấy, đã trở về" vào sự cố; sự cố không tự đóng. *(Nguồn: BR-M04-17, 5.5 "Thay đổi so với bản trước")*
- **FR-048**: Sự cố thiếu người MUST có: loại "đi lạc hoặc không trở về", mức Khẩn cấp, nguồn "hoạt động ngoài viện", người cao tuổi, chuyến đi, trưởng đoàn, địa điểm và thời điểm thấy lần cuối, người phát hiện (người thực hiện lệnh). Mỗi người trong một chuyến MUST có tối đa một sự cố thiếu người đang mở. Thông báo khẩn cấp theo feature 007 (BR-M05-06). *(Nguồn: BR-M04-17, feature 007 FR-040, FR-042)*
- **FR-049**: Tới thời điểm về dự kiến cộng CFG-M04-07 (mặc định \[30 phút\]) mà chuyến chưa Kết thúc điểm danh về, hệ thống MUST báo trưởng đoàn và trưởng tầng, một lần cho mỗi mốc giờ về dự kiến; chuyến gắn dấu "quá giờ về"; trạng thái người tham gia không đổi. *(Nguồn: BR-M04-17, CFG-M04-07)*
- **FR-049a**: Tới thời điểm về dự kiến cộng hai lần CFG-M04-07 mà chuyến vẫn chưa Kết thúc điểm danh về và giờ về chưa được gia hạn, hệ thống MUST báo Quản lý viện, một lần cho mỗi mốc giờ về dự kiến. *(Suy ra từ BR-M04-17, cách leo thang của BR-M05-01; Q-172)*
- **FR-050**: Chuyến MUST chuyển Đã về khi Kết thúc điểm danh về được thực hiện; chuyến ở Đã về vẫn nhận điểm danh về muộn cho người "Thiếu khi về". *(Suy ra từ 8.9)*

**Bảng trạng thái chuyến đi** *(8.9, BR-M04-16, BR-M04-17)*:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái đích | Ai được thực hiện | Điều kiện (chặn nếu không đạt) | Tác động tự động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Sinh từ mẫu / Tạo buổi lẻ | Đã lên lịch | Hệ thống / Trưởng tầng | Như buổi trong viện; có điểm đến, giờ rời, giờ về dự kiến (FR-034) | Cung cấp khoảng đi cho feature 006, 011 |
| Đã lên lịch | Dời buổi | Đã lên lịch | Trưởng tầng | Chưa có người rời viện; có lý do; chuyến dùng xe của viện: lịch xe dời được sang giờ mới, không chồng lịch khác của cùng xe (feature 019 FR-017), nếu không thì chặn với lý do "xe đã có lịch khác" (Q-239) | Như buổi trong viện; báo feature 006, 011, 019 (lịch xe dời theo, Q-231); đánh giá và danh sách liều theo FR-038a |
| Đã lên lịch | Hủy buổi | Đã hủy | Trưởng tầng (lý do); Hệ thống (đổi mẫu, ngừng hoạt động) | Chưa có người rời viện | Hủy đăng ký; báo feature 005, 006, 011, 019 (lịch xe Đã hủy, Q-231); báo điều dưỡng phụ trách để Nhận lại thuốc đã giao (FR-038a) |
| Đã lên lịch | Điểm danh rời viện (người đầu tiên) | Đang đi | Trưởng đoàn; Trưởng tầng thay (lý do) | Có trưởng đoàn (FR-035); người đi đã Đạt (FR-036), đạt FR-038 | FR-041 cho từng người; báo feature 019 (lịch xe Đang dùng, Q-231) |
| Đang đi | Điểm danh rời viện (đi sau) | Đang đi | Như trên | Như trên; trước giờ về dự kiến | FR-041 |
| Đang đi | Hủy ghi nhận bản ghi rời viện cuối cùng còn lại (FR-031a (b)) | Đã lên lịch | Trưởng tầng | Chuyến không còn bản ghi rời viện nào; có lý do | Đăng ký mở lại; báo feature 005, 006, 011, 019 (lịch xe về Đã đặt, Q-240) |
| Đang đi | Gia hạn giờ về | Đang đi | Trưởng đoàn, Trưởng tầng | Có lý do | Tính lại mốc quá giờ; báo feature 006, 011, 019 (giờ về của lịch xe dời theo; chồng lịch kế tiếp không chặn gia hạn, Q-236) |
| Đang đi | Tới giờ về dự kiến + CFG-M04-07 | Đang đi (dấu "quá giờ về") | Hệ thống | Chưa Kết thúc điểm danh về | Báo trưởng đoàn, trưởng tầng (FR-049) |
| Đang đi | Tới giờ về dự kiến + 2 × CFG-M04-07 | Đang đi | Hệ thống | Chưa Kết thúc điểm danh về, chưa gia hạn | Báo Quản lý viện (FR-049a) |
| Đang đi | Chuyển trưởng đoàn | Đang đi | Trưởng đoàn, Trưởng tầng | Người nhận là người đi cùng có vai trò Trưởng tầng, Nhân viên chăm sóc hoặc Điều dưỡng; có lý do (FR-035a) | Lưu lịch sử trưởng đoàn |
| Đang đi | Điểm danh về (từng phần) | Đang đi | Trưởng đoàn, Trưởng tầng | Người đã rời viện, có mặt tại viện | FR-044 |
| Đang đi | Báo thiếu người | Đang đi | Trưởng đoàn, mọi người đi cùng, Trưởng tầng (FR-046) | Người đã rời viện, chưa về; trực tuyến (FR-046a) | Sự cố khẩn cấp (FR-046, FR-048) |
| Đang đi | Kết thúc điểm danh về | Đã về | Trưởng đoàn, Trưởng tầng | — | Người chưa về, không rời đoàn → Thiếu khi về, sự cố khẩn cấp (FR-047); báo feature 019 (lịch xe Đã hoàn thành, Q-231) |
| Đã về | Điểm danh về muộn | Đã về | Trưởng đoàn, Trưởng tầng | Người ở "Thiếu khi về", có mặt tại viện | FR-044; diễn biến vào sự cố (FR-047) |
| Đã hủy | — | — | — | Trạng thái cuối | — |

**Bảng trạng thái người tham gia chuyến đi**:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái đích | Ai được thực hiện | Điều kiện | Tác động tự động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Đăng ký | Đã đăng ký | Trưởng tầng, Nhân viên chăm sóc, Điều dưỡng | FR-015, FR-016 | — |
| Đã đăng ký | Đánh giá khả năng: Đạt | Được đi | Trưởng tầng (Q-161) | FR-036 | Hiển thị thuốc cần mang cho điều dưỡng phụ trách (FR-038) |
| Đã đăng ký; Được đi | Đánh giá: Không đạt; chỉ định hạn chế mới (FR-037); dấu nghi nhiễm (FR-021a); hủy đăng ký | Không đi | Trưởng tầng (Q-161); Hệ thống; Trưởng tầng, Nhân viên chăm sóc, Điều dưỡng | Có lý do; chưa rời viện | Kết thúc đăng ký với lý do; báo feature 006 nếu đã giao thuốc (FR-038a) |
| Được đi | Điểm danh rời viện | Đã rời viện | Trưởng đoàn; Trưởng tầng thay | FR-038, FR-040 | FR-041 |
| Đã rời viện | Điểm danh về | Đã về | Trưởng đoàn, Trưởng tầng | Có mặt tại viện | FR-044 |
| Đã rời viện | Báo thiếu người | Thiếu khi về | Trưởng đoàn, mọi người đi cùng, Trưởng tầng (FR-046) | Trực tuyến (FR-046a) | Sự cố khẩn cấp (FR-048) |
| Đã rời viện | Kết thúc điểm danh về khi chưa về | Thiếu khi về | Trưởng đoàn, Trưởng tầng (FR-047) | Trực tuyến (FR-046a) | Sự cố khẩn cấp (FR-048) |
| Đã rời viện | Hủy ghi nhận điểm danh rời viện (FR-031a (b)) | Được đi hoặc Không đi | Trưởng tầng | Chưa có bản ghi về; chuyến chưa Đã về; có lý do | Người cao tuổi về Đang lưu trú; báo feature 005, 006, 010, 011 |
| Đã về | Hủy ghi nhận điểm danh về (FR-031a (c)) | Đã rời viện | Trưởng tầng | Chuyến chưa Đã về; có lý do | Người cao tuổi về Hoạt động bên ngoài; báo feature 005, 006, 011 |
| Được đi | Chuyến bị dời sang ngày khác (FR-038a) | Đã đăng ký | Hệ thống | — | Báo điều dưỡng phụ trách nếu đã giao thuốc |
| Đã rời viện; Thiếu khi về | Chuyển viện / Ghi nhận qua đời (feature 004, 007) | Rời đoàn | Hệ thống, theo lệnh của feature nguồn | — | Không tính thiếu người (FR-045) |
| Thiếu khi về | Điểm danh về muộn | Đã về | Trưởng đoàn, Trưởng tầng | Có mặt tại viện | FR-044; diễn biến vào sự cố (FR-047) |
| Không đi; Đã về; Rời đoàn | — | — | — | Trạng thái cuối | — |

#### I. Sở thích, theo dõi tinh thần và nguy cơ cô lập

- **FR-051**: Hệ thống MUST quản lý danh mục nhóm sở thích (nhóm 1, ví dụ âm nhạc, cờ, làm vườn, tôn giáo, đọc sách, thể dục) liên kết với loại hoạt động, và danh sách sở thích của từng người cao tuổi gồm: nhóm sở thích, mô tả, mức ưa thích (thích / không thích), nguồn (người cao tuổi tự nói, người thân cho biết, nhân viên quan sát), người ghi, thời điểm. Thêm, bỏ sở thích MUST lưu lịch sử người thực hiện, thời điểm; bỏ sở thích MUST có lý do. Mỗi người cao tuổi có tối đa một mục hiện hành cho mỗi nhóm sở thích: ghi mục mới cho nhóm đã có (ví dụ "không thích" sau "thích") MUST hiển thị mục cũ và yêu cầu người ghi xác nhận; khi xác nhận, mục cũ chuyển "đã thay" trong lịch sử. Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc ghi được sở thích cho người cao tuổi trong phạm vi (xem Assumptions). *(Nguồn: 8.10 "sở thích", 1.3)*
- **FR-052**: Hoạt động mà người cao tuổi có sở thích "không thích" khớp nhóm của loại hoạt động MUST được hiển thị cảnh báo khi đăng ký và bị loại khỏi gợi ý (FR-020, FR-057). *(Suy ra từ 8.10)*

**Bảng trạng thái mục sở thích** *(FR-051)*:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái đích | Ai được thực hiện | Điều kiện (chặn nếu không đạt) | Tác động tự động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Ghi sở thích | Hiện hành | Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc (trong phạm vi) | Nhóm sở thích đang hiệu lực | Nếu nhóm đã có mục Hiện hành: sau khi người ghi xác nhận, mục cũ → Đã thay |
| Hiện hành | Ghi mục mới cùng nhóm | Đã thay | Như trên | Người ghi xác nhận | Mục mới thành Hiện hành |
| Hiện hành | Bỏ sở thích | Đã bỏ | Như trên | Có lý do | — |
| Đã thay; Đã bỏ | — | — | — | Trạng thái cuối; xem được trong lịch sử | — |
- **FR-053**: Hệ thống MUST cho Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc (trong phạm vi) xem hồ sơ tinh thần của một người cao tuổi theo khoảng thời gian, tổng hợp: số buổi hoạt động nhóm có mặt theo tuần; mức độ tham gia và giao tiếp theo buổi (FR-029); tâm trạng và hành vi bất thường theo ngày (feature 005, chỉ đọc); sở thích; các cảnh báo cô lập, tâm trạng đã có và trạng thái của chúng (feature 007). Hồ sơ tinh thần chỉ để xem; dữ liệu MUST được ghi ở nguồn. *(Nguồn: 8.10 "Ghi nhận: tâm trạng; giao tiếp; mức độ tham gia; hành vi bất thường; sở thích")*
- **FR-054**: Lúc 00:00 mỗi ngày, Bộ lập lịch MUST xét cho ngày vừa kết thúc, với mỗi người cao tuổi ở Đang lưu trú (bán trú: có hợp đồng hiệu lực), chuỗi ngày tính liên tiếp gần nhất không có lượt điểm danh Có mặt ở hoạt động nhóm (kể cả chuyến đi, kể cả mức "bỏ giữa chừng"). Ngày không phải ngày tính theo định nghĩa ở mục Thuật ngữ (có khoảng vắng dù ngắn, có chỉ định hạn chế bao trùm hoạt động nhóm, mang dấu "nghi nhiễm", có đăng ký hoạt động nhóm "Không ghi nhận", bán trú không đến) MUST được bỏ qua, không làm đứt chuỗi. Một ngày có lượt Có mặt ở hoạt động nhóm luôn làm chuỗi đếm lại, kể cả khi ngày đó không phải ngày tính. Chuỗi bắt đầu không sớm hơn ngày Hoàn tất tiếp nhận. *(Nguồn: BR-M04-22 "không tính thời gian vắng mặt hoặc có chỉ định hạn chế", CFG-M04-10; nghi nhiễm: Q-165; vắng một phần ngày, "Không ghi nhận": Q-169, Q-173)*
- **FR-055**: Khi chuỗi ở FR-054 đạt CFG-M04-10 (mặc định \[7 ngày\]) ngày tính, hệ thống MUST yêu cầu feature 007 tạo cảnh báo mức Nhẹ loại "nguy cơ cô lập" cho người cao tuổi, người nhận là trưởng tầng của tầng người đó, kèm danh sách hoạt động gợi ý (FR-057). *(Nguồn: BR-M04-22, UC-29)*
- **FR-056**: Nếu đã có cảnh báo "nguy cơ cô lập" đang mở cho người đó, cảnh báo mới MUST được gộp vào theo DBR-18 (feature 007). Khi người đó có lượt Có mặt ở hoạt động nhóm, chuỗi MUST đếm lại từ đầu và feature 007 MUST nhận diễn biến "đã tham gia hoạt động nhóm" cho cảnh báo đang mở; cảnh báo không tự đóng. *(Nguồn: BR-M05-02, DBR-18)*
- **FR-057**: Danh sách hoạt động gợi ý cho một người MUST gồm các buổi trong CFG-M04-09 tới, còn chỗ, người đó đạt các điều kiện chặn của FR-015, loại trừ sở thích "không thích", xếp theo thứ tự: buổi khớp sở thích "thích" trước, trong cùng mức thì buổi sớm hơn trước; nếu không có buổi khớp sở thích thì gồm các buổi hoạt động nhóm đạt điều kiện, có ghi chú "không khớp sở thích". Danh sách này MUST được cung cấp cho cảnh báo cô lập (FR-055) và cho cảnh báo tâm trạng tiêu cực kéo dài khi feature 005 yêu cầu (BR-M04-11, feature 005 FR-042). *(Nguồn: BR-M04-11, BR-M04-22, 8.10 "đề xuất hoạt động phù hợp")*
- **FR-058**: Danh sách người cao tuổi có cảnh báo nguy cơ cô lập đang mở và số ngày tính của chuỗi MUST được cung cấp cho báo cáo chăm sóc (18.2). *(Nguồn: 18.2 "(Bổ sung)")*

#### J. Kiểm tra chất lượng ngẫu nhiên

- **FR-059**: Với mỗi ca có phạm vi tầng/khu vực (feature 008), hệ thống MUST lập một danh sách kiểm tra chất lượng giao trưởng tầng được giao của tầng. Ngay khi một công việc đủ điều kiện (FR-060) được hoàn thành trong ca, hệ thống MUST xét chọn nó ngẫu nhiên với xác suất bằng CFG-M04-11 (mặc định \[5%\]); công việc được chọn vào danh sách ngay. Trưởng tầng MUST được báo khi danh sách của ca có mục đầu tiên; các mục sau chỉ hiện trong danh sách. Người thực hiện công việc MUST NOT được báo công việc của mình được chọn. Công việc ghi nhận ngoại tuyến được xét chọn lúc đồng bộ nếu thời điểm hoàn thành trên thiết bị thuộc ca và ca còn đang diễn ra; đồng bộ sau khi ca đã kết thúc thì không được xét cho ca nào. *(Nguồn: BR-M04-23, CFG-M04-11, UC-31; Clarification 2026-09-27, Q-163; ngoại tuyến: Q-174)*
- **FR-059a**: Tại mốc CFG-M09-04 (mặc định \[30 phút\]) trước khi kết ca, nếu số mục đã chọn nhỏ hơn CFG-M04-11 nhân số công việc đủ điều kiện đã hoàn thành trong ca (làm tròn lên, tối thiểu 1 khi có công việc đủ điều kiện), hệ thống MUST chọn ngẫu nhiên bổ sung trong số công việc đủ điều kiện chưa được chọn cho đủ số đó. Công việc hoàn thành sau mốc này vẫn được xét chọn theo FR-059. *(Clarification 2026-09-27, Q-163; mốc dùng lại CFG-M09-04 là thời điểm lập bản nháp bàn giao)*
- **FR-060**: Công việc đủ điều kiện là công việc gắn tầng/khu vực của ca, ở Hoàn thành hoặc Hoàn thành trễ, có thời điểm hoàn thành trong ca, gồm công việc chăm sóc (feature 005, kể cả "Ghi nhận phát sinh" Q-36) và công việc vệ sinh (feature 003, 7.5) của phòng hoặc khu vực chung gắn tầng/khu vực đó; không gồm công việc vệ sinh của khu vực chung không gắn tầng/khu vực nào, liều thuốc (Module 07), công việc do chính trưởng tầng được giao thực hiện, công việc đã có bản đính chính "Hủy ghi nhận", và công việc làm lại do kiểm tra chất lượng sinh ra trong cùng ca. Mỗi công việc MUST được chọn tối đa một lần. Phép chọn MUST ngẫu nhiên, mỗi công việc đủ điều kiện có cùng khả năng được chọn bất kể loại, người thực hiện hay thời điểm hoàn thành, và không để nhân viên biết trước công việc nào sẽ được chọn. *(Nguồn: BR-M04-23 "(Bổ sung, vệ sinh)"; 8.3 "(Làm rõ)"; loại công việc của trưởng tầng: Clarification 2026-09-27, Q-168; khu vực chung không gắn tầng: Q-174)*
- **FR-061**: Với mỗi công việc trong danh sách, trưởng tầng MUST ghi kết quả Đạt hoặc Không đạt, kèm ghi chú (bắt buộc với Không đạt) và MAY kèm ảnh. Kết quả kiểm tra là nhóm 3: không sửa, không xóa; sai sót xử lý bằng đính chính theo feature 000. Công việc gốc và kết quả ghi nhận của nó MUST NOT bị sửa. *(Nguồn: BR-M04-23 "Bản ghi gốc của công việc không bị sửa", 1.5 nhóm 3)*
- **FR-062**: Trưởng tầng MUST ghi được "Không kiểm tra được" kèm lý do (ví dụ người cao tuổi đã chuyển viện, công việc không còn dấu vết kiểm tra được). Mục có công việc bị "Hủy ghi nhận" sau khi được chọn MUST tự đóng với lý do đó. Hai kết quả này MUST NOT tính vào tỷ lệ Đạt. *(Suy ra từ BR-M04-23)*
- **FR-063**: Hạn kiểm tra của mọi mục là giờ kết thúc của ca có danh sách. Mục chưa có kết quả khi hết ca MUST tự đóng với kết quả "Quá hạn kiểm tra", được tính riêng trong báo cáo, và Quản lý viện MUST được báo một lần cho mỗi danh sách có mục quá hạn. Trưởng tầng được giao ghi được kết quả trong thời gian của ca, kể cả khi không có tên trong ca đó. *(Suy ra từ BR-M04-23, 18.2; Clarification 2026-09-27, Q-163)*
- **FR-064**: Khi kết quả Không đạt được lưu, trong cùng một lần hệ thống MUST yêu cầu feature 005 (công việc chăm sóc) hoặc feature 003 (công việc vệ sinh) sinh một công việc làm lại: cùng loại, cùng người cao tuổi hoặc phòng/khu vực, mức quan trọng bằng công việc gốc, thời điểm dự kiến trong chính ca của danh sách (kết quả luôn được ghi trong ca đó, FR-063), giao cho người thực hiện công việc gốc nếu người đó còn tên và không vắng trong ca, nếu không thì thành công việc chung của tầng; khi ghi kết quả sát giờ kết ca, công việc làm lại chưa đóng đi vào bản nháp bàn giao theo feature 005 (BR-M04-07); công việc làm lại trỏ về công việc gốc và kết quả kiểm tra. Người thực hiện công việc gốc MUST được báo. Kết quả Không đạt MUST NOT tự đổi trạng thái giường hay phòng; với công việc vệ sinh trả giường hoặc khử khuẩn mà giường đã chuyển Trống (DBR-26), hệ thống chỉ sinh công việc làm lại và báo trưởng tầng nếu giường đã có phân bổ mới, việc xử lý tiếp theo do feature 003 quyết định. *(Nguồn: BR-M04-23 "tạo công việc làm lại cho ca hiện tại"; giường: Q-174)*
- **FR-065**: Kết quả kiểm tra MUST được cung cấp cho báo cáo chất lượng (18.2): tỷ lệ Đạt theo nhân viên thực hiện công việc gốc và theo tầng, theo khoảng thời gian; số mục Không kiểm tra được và Quá hạn kiểm tra. Tỷ lệ Đạt = số Đạt ÷ (số Đạt + số Không đạt). Báo cáo MUST ghi rõ công việc do trưởng tầng được giao thực hiện không thuộc phạm vi kiểm tra (Q-168), không hiển thị tỷ lệ Đạt 0% hay trống như một kết quả của trưởng tầng. *(Nguồn: BR-M04-23, 18.2)*
- **FR-066**: Chỉ Trưởng tầng được giao của tầng (kể cả trưởng tầng tạm có thời hạn, Q-84) MUST ghi được kết quả kiểm tra; Người phụ trách ca không có quyền này. Quản lý viện xem được mọi danh sách và kết quả. *(Nguồn: 4.4 dòng "Kiểm tra chất lượng", 19.3 "(Bổ sung)")*

**Bảng trạng thái mục kiểm tra chất lượng** *(BR-M04-23, Q-163, Q-168)*:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái đích | Ai được thực hiện | Điều kiện (chặn nếu không đạt) | Tác động tự động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Xét chọn khi hoàn thành; chọn bổ sung ở mốc CFG-M09-04 | Chờ kiểm tra | Hệ thống | Công việc đủ điều kiện (FR-060) | Báo trưởng tầng nếu là mục đầu tiên của ca (FR-059) |
| Chờ kiểm tra | Ghi Đạt | Đạt | Trưởng tầng được giao (FR-066) | Trong thời gian ca | — |
| Chờ kiểm tra | Ghi Không đạt | Không đạt | Trưởng tầng được giao | Trong thời gian ca; có ghi chú | Sinh công việc làm lại; báo người thực hiện (FR-064) |
| Chờ kiểm tra | Ghi Không kiểm tra được | Không kiểm tra được | Trưởng tầng được giao | Có lý do (FR-062) | — |
| Chờ kiểm tra | Công việc bị Hủy ghi nhận | Không kiểm tra được | Hệ thống | — | Lý do "công việc đã hủy ghi nhận" |
| Chờ kiểm tra | Hết ca | Quá hạn kiểm tra | Hệ thống | Chưa có kết quả (FR-063) | Báo Quản lý viện một lần mỗi danh sách |
| Đạt; Không đạt; Không kiểm tra được; Quá hạn kiểm tra | — | — | — | Trạng thái cuối; sửa bằng đính chính (FR-061) | — |

#### K. Quyền, hiển thị và dữ liệu cung cấp

- **FR-067**: Quyền của spec MUST theo 4.4: Quản lý viện xem mọi hoạt động, buổi, điểm danh, chuyến đi, kết quả kiểm tra; Trưởng tầng thực hiện mọi lệnh của spec trong tầng mình và với hoạt động toàn viện; Nhân viên chăm sóc đăng ký, hủy đăng ký, điểm danh, phụ trách buổi, làm trưởng đoàn, ghi sở thích trong phạm vi; Bác sĩ gắn, xác nhận, gỡ chỉ định hạn chế (FR-023, FR-023b); Hành chính ghi nhận đồng ý về muộn của người đại diện cho bán trú (FR-016a, Q-167); Điều dưỡng có mọi lệnh của Nhân viên chăm sóc ở spec này (Q-214, Phụ lục 27 ²⁷), cộng với gắn chỉ định tạm (FR-023a); quyết định "không mang thuốc" là lệnh của feature 006 (FR-038); trưởng đoàn là Trưởng tầng, Nhân viên chăm sóc hoặc Điều dưỡng, đánh giá khả năng tham gia chỉ do Trưởng tầng (FR-035, FR-036, Q-161, Q-214); người đi cùng thuộc vai trò khác chỉ "Báo thiếu người" (FR-046); lệnh trên hoạt động toàn viện theo FR-002b; Người thân chỉ xem. Nhiệm vụ Trưởng đoàn MUST NOT tạo thêm quyền ngoài các lệnh của chuyến mình dẫn (feature 002 FR-019). *(Nguồn: 4.4, 2.4; feature 002 FR-019, FR-031)*
- **FR-068**: Trên cổng người thân, người thân có quan hệ Hiệu lực MUST thấy (loại thông tin "chung", feature 012 FR-046): các buổi người cao tuổi đã đăng ký, chuyến đi (điểm đến, giờ rời, giờ về dự kiến), số buổi đã tham gia; MUST NOT thấy chỉ định hạn chế, mức độ tham gia, giao tiếp, tình trạng sau hoạt động, cảnh báo cô lập hay kết quả kiểm tra chất lượng. *(Nguồn: 4.4 cột NT "X"; feature 012 FR-046, FR-052)*
- **FR-069**: Mọi lệnh ở spec này MUST được ghi nhật ký với người thực hiện, thời điểm, lý do (khi bắt buộc), theo feature 000; mọi mốc thời gian theo DBR-25. *(Nguồn: DBR-23, DBR-25, 19.4)*
- **FR-070**: Spec MUST cung cấp cho Module 14: số buổi, số lượt đăng ký, số lượt có mặt, số lượt "Không ghi nhận" và tỷ lệ tham gia theo hoạt động, theo tầng, theo khoảng thời gian; chuyến đi quá giờ về và sự cố thiếu người. Tỷ lệ tham gia = số lượt Có mặt ÷ (số lượt Có mặt + số lượt Vắng có lý do "từ chối", "sức khỏe" hoặc "khác"); lượt "Vắng – đang vắng mặt", "Vắng – không đến", "Không ghi nhận" và đăng ký Đã hủy không vào mẫu số. "Theo tầng" MUST hiểu như sau: buổi trong viện xếp theo tầng của địa điểm (Q-175); lượt đăng ký, lượt điểm danh, tỷ lệ tham gia và sự cố thiếu người xếp theo tầng của người cao tuổi tại thời điểm buổi bắt đầu; chuyến đi xếp theo phạm vi của hoạt động (một tầng hoặc toàn viện). Để feature 016 tính "chuyến quá giờ về", MUST cung cấp giờ về dự kiến, các lần gia hạn và thời điểm điểm danh về của từng người; và nhãn "ghi nhận muộn" của điểm danh buổi (CFG-M04-06). *(Nguồn: 18.2 "hoạt động; tỷ lệ tham gia", 18.5; công thức: Q-173; xếp tầng: đồng bộ spec 016)*

#### L. Thông báo

- **FR-071**: Feature này MUST yêu cầu feature 009 gửi đúng các thông báo ở bảng dưới, theo khuôn chung của feature 009 FR-001, FR-043a và nguyên tắc xếp mức FR-043b. Mọi "báo", "nhắc" trong FR và bảng trạng thái của spec này MUST có dòng tương ứng. Quy tắc chung cho mọi dòng: *(Nguồn: 17, BR-M13-01, feature 009)*
  - **Khóa sự kiện** (feature 009 FR-005): (loại sự kiện, buổi) cho sự kiện của buổi; (loại sự kiện, buổi, người cao tuổi) cho sự kiện của một đăng ký hoặc người tham gia; (loại sự kiện, danh sách kiểm tra) cho kiểm tra chất lượng; với nhắc theo mốc thêm mốc thời gian.
  - **Loại thông tin**: mọi thông báo tới nhân viên là "chung", trừ nội dung có lý do chỉ định hạn chế (loại "sức khỏe", chỉ hiện với người có quyền). Thông báo tới người thân chỉ là "chung".
  - **Nội dung rút gọn cho kênh ngoài ứng dụng** (feature 009 FR-019): loại sự kiện, tên hoạt động hoặc chuyến, thời điểm; không nêu lý do sức khỏe.
  - **Cách thay khi nhóm người nhận trống** (feature 009 FR-010): Trưởng tầng trống → Người phụ trách ca đang diễn ra của tầng; trưởng đoàn trống → Trưởng tầng; người phụ trách buổi trống → Trưởng tầng; Bác sĩ trực trống → mọi Bác sĩ Hoạt động (FR-023b); các nhóm khác "không thay".
  - **Người thân**: chỉ nhận dòng về đồng ý về muộn; nội dung chỉ nêu tên buổi, ngày, giờ về dự kiến, không nêu thông tin sức khỏe, nên không cần bản đồng ý chia sẻ dữ liệu (feature 009 FR-014).
  - Cảnh báo "nguy cơ cô lập" và sự cố thiếu người được thông báo qua feature 007, không lặp ở bảng này.

| Sự kiện | Người nhận | Mức | Căn cứ |
| --- | --- | --- | --- |
| Người thường xuyên không được đăng ký sẵn khi sinh buổi | Trưởng tầng của hoạt động | Nhẹ | FR-008 |
| Buổi bị hủy hoặc dời; đăng ký bị hủy tự động (đổi mẫu, dời buổi, khoanh vùng, nghi nhiễm, chỉ định hạn chế, trạng thái cuối) | Người phụ trách buổi; người đã đăng ký cho người cao tuổi (với đăng ký nguồn "thường xuyên": trưởng tầng của hoạt động); Trưởng tầng (với khoanh vùng, nghi nhiễm và chỉ định hạn chế) | Nhẹ | FR-009, FR-012, FR-013, FR-021, FR-021a, FR-022, FR-025 |
| Buổi chưa hoàn tất điểm danh sau thời điểm kết thúc + CFG-M04-06 | Người phụ trách buổi; Trưởng tầng | Nhẹ | FR-030 |
| Hết ngày, buổi có đăng ký "Không ghi nhận" hoặc "Không điểm danh" | Người phụ trách buổi; Trưởng tầng | Trung bình | FR-014a |
| Chuyến bị hủy, dời sang ngày khác, hoặc người chuyển "Không đi" sau khi đã giao thuốc mang theo | Điều dưỡng phụ trách người cao tuổi | Trung bình | FR-038a |
| Chuyến quá giờ về dự kiến + 2 × CFG-M04-07, chưa gia hạn | Quản lý viện | Trung bình | FR-049a |
| Chuyển trưởng đoàn; chuyến không còn trưởng đoàn đủ điều kiện (trưởng tầng tạm chịu trách nhiệm) | Trưởng đoàn mới, trưởng tầng; Quản lý viện (khi không còn người đủ điều kiện) | Trung bình | FR-035a |
| Người được đánh giá Đạt có liều trong khoảng đi, cần chuẩn bị thuốc mang theo | Điều dưỡng phụ trách người cao tuổi | Trung bình | FR-038 |
| Người đã được đánh giá Đạt bị chỉ định hạn chế mới; chỉ định mới cho người đang đi | Trưởng đoàn; Trưởng tầng | Trung bình | FR-025, FR-037 |
| Điều dưỡng gắn chỉ định tạm, cần bác sĩ xác nhận | Bác sĩ trực (không có thì mọi Bác sĩ Hoạt động) | Trung bình | FR-023b |
| Chỉ định tạm hết hiệu lực do không được xác nhận | Điều dưỡng đã gắn; Trưởng tầng | Nhẹ | FR-023b |
| Giờ về được gia hạn vượt ca của trưởng đoàn hoặc người đi cùng | Trưởng tầng | Trung bình | FR-035 |
| Người đại diện rút đồng ý về muộn, đăng ký của người bán trú bị hủy; giờ về dự kiến của bán trú được dời theo đồng ý | Người đã đăng ký cho người cao tuổi; Hành chính; người đại diện (với lần dời giờ về) | Nhẹ | FR-016a |
| Chuyến quá giờ về dự kiến + CFG-M04-07 | Trưởng đoàn; Trưởng tầng | Trung bình | BR-M04-17, FR-049 |
| Người đi trở về vùng đang khoanh vùng; người đang đi được gắn dấu nghi nhiễm | Trưởng đoàn; Trưởng tầng | Trung bình | Edge Cases, FR-021a |
| Danh sách kiểm tra chất lượng của ca có mục đầu tiên | Trưởng tầng được giao của tầng | Nhẹ | FR-059 |
| Kết quả kiểm tra Không đạt, có công việc làm lại | Người thực hiện công việc gốc | Trung bình | FR-064 |
| Danh sách kiểm tra có mục Quá hạn kiểm tra | Quản lý viện | Nhẹ | FR-063 |

### Truy vết quy tắc

| Quy tắc / quyết định | Kịch bản chấp nhận | FR |
| --- | --- | --- |
| 8.8 (hoạt động, quản lý, gợi ý) | US1 kịch bản 1, 5, 6, 7; US2 kịch bản 8 | FR-001 → FR-004, FR-011, FR-020 |
| BR-M04-21, CFG-M04-09 (hoạt động định kỳ) | US1 kịch bản 1 → 4 | FR-005 → FR-010 |
| BR-M04-15 (giới hạn, chỉ định hạn chế, khoanh vùng) | US2 kịch bản 1 → 6, 9, 11, 12; US3 kịch bản 3 | FR-015 → FR-025 |
| BR-M05-11, BR-M05-12 (khoanh vùng), Q-164 | US2 kịch bản 5, 6; US4 kịch bản 7 | FR-002a, FR-015 (c), FR-021 |
| Q-167 (bán trú về muộn) | US2 kịch bản 14 | FR-016, FR-016a |
| Q-165, feature 007 FR-060 (dấu nghi nhiễm) | US2 kịch bản 13 | FR-015 (g), FR-018, FR-021a, FR-054 |
| BR-M04-18, BR-M11-01, DBR-15 (chi phí) | US3 kịch bản 1, 2, 4; US4 kịch bản 5 | FR-031, FR-032 |
| 8.8 (điểm danh), BR-M04-12 (ghi nhận muộn) | US3 kịch bản 1 → 7 | FR-027 → FR-031 |
| Q-169 (buổi thiếu điểm danh, "Không ghi nhận") | US3 kịch bản 9 | FR-014, FR-014a, FR-054 |
| Q-170 (đính chính điểm danh rời/về) | US5 kịch bản 11 | FR-031a |
| Q-171 (thay đổi chuyến sau chuẩn bị; không mang thuốc) | Edge Cases (chuyến bị hủy, dời) | FR-015 (e), FR-038, FR-038a, FR-040 |
| Q-172 (an toàn chuyến: leo thang, chuyển trưởng đoàn, qua hết ca, kèm riêng) | US5 kịch bản 9, 10 | FR-035, FR-035a, FR-036, FR-049a |
| Q-173 (bỏ giữa chừng, tỷ lệ tham gia, ngày tính) | US3 kịch bản 9; US6 kịch bản 3 | FR-029, FR-033, FR-054, FR-070 |
| Q-174 (kiểm tra chất lượng: khu vực chung, ngoại tuyến, giường) | — (kiểm thử cùng feature 003, 005) | FR-059, FR-060, FR-064 |
| Q-175 (địa điểm, người phụ trách, quyền trên hoạt động toàn viện, đánh giá chuyến toàn viện) | US1 kịch bản 7; US2 kịch bản 5 | FR-002a, FR-002b, FR-036 |
| Q-176 (giờ về theo ngày của bán trú, đồng ý "Đã dùng") | US2 kịch bản 14 | FR-016a, bảng trạng thái đồng ý về muộn |
| 8.9, UC-30 (chuẩn bị chuyến đi) | US4 kịch bản 1 → 3, 6 | FR-034 → FR-039 |
| BR-M04-16, BR-M07-03, 5.6 (rời viện) | US4 kịch bản 4, 5 | FR-040 → FR-042 |
| BR-M04-17, 5.5 (quá giờ, thiếu người) | US5 kịch bản 1 → 8 | FR-043 → FR-050 (Q-166: US5 kịch bản 8, FR-045) |
| 8.10, BR-M04-11 (gợi ý), BR-M04-22, CFG-M04-10 | US6 kịch bản 1 → 7 | FR-051 → FR-058 |
| BR-M04-23, CFG-M04-11, UC-31 | US7 kịch bản 1 → 10 | FR-059 → FR-066 |
| Q-161 (trưởng đoàn, người đi cùng, người đánh giá) | US4 kịch bản 2, 8; US5 kịch bản 5 | FR-035, FR-036, FR-046, FR-067 |
| Q-162 (chỉ định hạn chế có phạm vi, chỉ định tạm) | US2 kịch bản 3, 4, 11, 12; US6 kịch bản 5 | FR-023 → FR-025, bảng trạng thái chỉ định |
| Q-163 (chọn rải trong ca, hạn hết ca) | US7 kịch bản 1 → 3, 5 | FR-059, FR-059a, FR-063, FR-064 |
| Q-168 (loại công việc của trưởng tầng) | US7 kịch bản 8 | FR-060, FR-065 |
| BR-M05-10 (truy vết tiếp xúc) | US3 kịch bản 8 | FR-033 |
| 4.4, 19.3 (quyền) | US1 kịch bản 7; US2 kịch bản 10; US3 kịch bản 7; US7 kịch bản 8 | FR-017, FR-027, FR-066 → FR-068 |
| 1.5 nhóm 3, DBR-23 (không sửa, đính chính, nhật ký) | US3 kịch bản 4; US7 kịch bản 2, 3 | FR-031, FR-061, FR-069 |

### Giao tiếp với feature khác

| Hướng | Feature | Nội dung | Dữ liệu tối thiểu |
| --- | --- | --- | --- |
| Nhận | 000 | Đính chính, nhật ký, lý do bắt buộc, tham số CFG-M04-06, CFG-M04-07, CFG-M04-09, CFG-M04-10, CFG-M04-11, CFG-M04-13, CFG-M04-14 (đề xuất), CFG-M05-07, CFG-M09-04 | — |
| Nhận, Gửi | 001 | Nhận: trạng thái, cờ nguy cơ, mức chăm sóc, loại hình lưu trú, ngày Hoàn tất tiếp nhận, sự kiện trạng thái cuối. Gửi: yêu cầu chuyển Đang lưu trú ↔ Hoạt động bên ngoài khi điểm danh rời/về (feature 001 bảng trạng thái) | Người cao tuổi, trạng thái, người thực hiện, căn cứ (chuyến), thời điểm |
| Nhận | 002 | Phạm vi dữ liệu; tài khoản Hoạt động; phạm vi do nhiệm vụ Trưởng đoàn (feature 002 FR-031) | Nhân viên, người cao tuổi, chuyến |
| Nhận, Gửi | 003 | Nhận: tầng/khu vực, giường, khu nghỉ bán trú của người cao tuổi; công việc vệ sinh Hoàn thành. Gửi: yêu cầu sinh công việc vệ sinh làm lại (FR-064) | Người cao tuổi, tầng, phòng, công việc |
| Nhận | 004 | Lượt vắng; Chuyển viện; Ghi nhận qua đời; danh mục dịch vụ cho hoạt động có thu phí; lịch bán trú theo hợp đồng | Người cao tuổi, khoảng vắng, dịch vụ |
| Gửi, Nhận | 005 | Gửi: đăng ký, hủy đăng ký, buổi bị dời/hủy, buổi kết thúc (FR-014a) (sinh, hủy công việc "hoạt động"); thời điểm rời/về chuyến đi và đính chính (FR-031a); giờ về dự kiến mới của bán trú theo đồng ý về muộn (FR-016a, Q-176); danh sách hoạt động gợi ý (FR-057); yêu cầu sinh công việc làm lại (FR-064). Nhận: công việc Hoàn thành cho mẫu kiểm tra; tâm trạng, hành vi bất thường cho hồ sơ tinh thần; yêu cầu gợi ý khi tâm trạng tiêu cực kéo dài; điểm danh đến/về bán trú | Người cao tuổi, buổi, thời điểm, công việc, kết quả |
| Gửi, Nhận | 006 | Gửi: khoảng đi dự kiến, người tham gia, giờ về dự kiến và gia hạn, thời điểm rời/về thực tế và đính chính (feature 006 FR-048, FR-031a); hủy chuyến, người "Không đi", dời chuyến để Nhận lại thuốc (FR-038a). Nhận: danh sách liều trong khoảng đi, lần giao thuốc mang theo, quyết định "không mang thuốc" (lệnh của feature 006, FR-038) | Người cao tuổi, chuyến, thời điểm, liều, lần giao |
| Gửi, Nhận | 007 | Gửi: yêu cầu tạo sự cố khẩn cấp thiếu người (FR-048), diễn biến "đã tìm thấy, đã trở về"; yêu cầu tạo cảnh báo "nguy cơ cô lập" và diễn biến "đã tham gia" (FR-055, FR-056); điểm danh cho danh sách tiếp xúc (FR-033). Nhận: trạng thái và sự kiện khoanh vùng (feature 007 FR-065); dấu nghi nhiễm và sự kiện gắn, gỡ dấu (FR-021a); cảnh báo, sự cố đang mở; chuyển viện từ sự cố | Người cao tuổi, chuyến/buổi, loại, mức, địa điểm, thời điểm |
| Nhận, Gửi | 008 | Nhận: ca, trưởng tầng được giao, người trong ca (FR-035, FR-059, FR-064). Gửi: người đang Hoạt động bên ngoài, chuyến quá giờ về vào bản nháp bàn giao (feature 008 FR-039 (e)) | Ca, tầng, nhân viên, người cao tuổi |
| Gửi | 009 | Các thông báo ở bảng FR-071 | Nguồn, mức, nhóm người nhận, loại thông tin |
| Gửi | 010 | Lượt điểm danh có mặt ở hoạt động có thu phí (buổi trong viện: điểm danh; chuyến đi: điểm danh rời viện, ngày là ngày rời thực tế), sự kiện hủy lượt (đính chính, FR-031, FR-031a) (FR-032); dấu "có thu phí" và dịch vụ của hoạt động | Buổi, người cao tuổi, dịch vụ, bản ghi điểm danh, ngày của buổi |
| Gửi | 011 | Chuyến đã lên lịch, người tham gia ở mọi lần thay đổi, giờ rời, giờ về dự kiến và gia hạn, thời điểm rời/về thực tế và đính chính, hủy chuyến (FR-034, Q-144) | Chuyến, người tham gia, khoảng thời gian |
| Gửi, Nhận | 019 | Gửi: chuyến dùng xe của viện được lên lịch, dời, hủy, người đầu tiên rời viện, gia hạn giờ về, Kết thúc điểm danh về (Q-231, Q-236). Nhận: có lịch xe Đã đặt bao trùm giờ rời dự kiến (FR-040) | Chuyến, xe, giờ rời, giờ về dự kiến, thời điểm thực tế |
| Gửi, Nhận | 012 | Gửi: buổi đã đăng ký, chuyến đi, số hoạt động đã tham gia cho cổng và bản tin (FR-033, FR-068). Nhận: đồng ý về muộn và rút đồng ý của người đại diện qua cổng (FR-016a) | Người cao tuổi, buổi, số lượt, người đại diện, đồng ý |
| Gửi | Module 14 (feature 016) | Tỷ lệ tham gia, danh sách nguy cơ cô lập, tỷ lệ Đạt kiểm tra chất lượng, chuyến quá giờ (FR-058, FR-065, FR-070) | Buổi (hoạt động, phạm vi, tầng địa điểm, trạng thái); đăng ký và kết quả điểm danh (người cao tuổi, tầng lúc buổi bắt đầu, lý do vắng, nhãn "ghi nhận muộn"); chuyến đi (giờ về dự kiến, các lần gia hạn, thời điểm điểm danh về từng người); sự cố thiếu người; mục kiểm tra chất lượng (công việc gốc, người thực hiện, tầng, kết quả, dấu "ngoài phạm vi kiểm tra"); cảnh báo cô lập (người cao tuổi, số ngày tính) |

### Key Entities *(include if feature involves data)*

- **Loại hoạt động** – nhóm 1: tên, dấu "hoạt động nhóm" mặc định, dấu "ngoài viện", nhóm sở thích liên quan, trạng thái.
- **Hoạt động (HOAT_DONG)** – nhóm 1: tên, loại, dấu "hoạt động nhóm", trưởng tầng tạo, phạm vi tổ chức, địa điểm mặc định (phòng/khu vực của feature 003), người phụ trách mặc định, thời lượng, số lượng tối đa, đối tượng, có thu phí, dịch vụ, mẫu lặp (ngày, giờ, ngày bắt đầu, ngày kết thúc), người tham gia thường xuyên, lịch sử thay đổi mẫu (lý do, ngày áp dụng), trạng thái.
- **Buổi hoạt động (BUOI_HOAT_DONG)** – nhóm 2: hoạt động, thời điểm bắt đầu, kết thúc, địa điểm, người phụ trách, nguồn (mẫu / lẻ), trạng thái (Đã lên lịch / Đã điểm danh / Không điểm danh / Đã hủy), dấu "điểm danh không đủ", lý do hủy hoặc dời. Với chuyến đi thêm: điểm đến, giờ rời dự kiến, giờ về dự kiến và lịch sử gia hạn, phương tiện, trưởng đoàn và lịch sử chuyển trưởng đoàn, dấu "trưởng đoàn tạm từ xa", người đi cùng, dấu "quá giờ về".
- **Đăng ký** – nhóm 2: buổi, người cao tuổi, nguồn, người đăng ký, thời điểm, trạng thái, lý do hủy, cảnh báo đã xác nhận. Với chuyến đi thêm: kết quả đánh giá khả năng tham gia (người đánh giá, thời điểm, lý do), người đi cùng kèm riêng, trạng thái người tham gia.
- **Đồng ý về muộn của bán trú** – nhóm 2: người cao tuổi, buổi, người đại diện đồng ý, cách ghi (cổng hoặc bản ký), giờ về dự kiến mới, người ghi nhận, thời điểm, trạng thái (Hiệu lực / Đã rút / Đã dùng), người và thời điểm rút (FR-016a, Q-167).
- **Điểm danh (DIEM_DANH)** – nhóm 3: buổi, người cao tuổi, loại (buổi / rời viện / về), kết quả (Có mặt / Vắng / Không ghi nhận), lý do vắng, mức độ tham gia, mức giao tiếp, tình trạng sau, thời điểm thực tế, người ghi, nhãn "ghi nhận muộn", thời điểm trên thiết bị và đồng bộ.
- **Chỉ định hạn chế hoạt động** – nhóm 2: người cao tuổi, phạm vi, thời gian hiệu lực, lý do (thông tin sức khỏe), loại (chính thức / tạm), người gắn (bác sĩ hoặc điều dưỡng), trạng thái (Tạm / Hiệu lực / Đã gỡ / Hết hiệu lực), bác sĩ xác nhận và thời điểm, lịch sử gỡ, rút ngắn, mở rộng.
- **Nhóm sở thích** – nhóm 1; **Sở thích của người cao tuổi** – nhóm 2 có lịch sử, tối đa một mục hiện hành mỗi nhóm: nhóm, mô tả, mức ưa thích, nguồn, người ghi, thời điểm, trạng thái (Hiện hành / Đã bỏ / Đã thay), lý do bỏ.
- **Danh sách kiểm tra chất lượng** – nhóm 2: ca, tầng, hạn (giờ kết thúc ca), trưởng tầng được giao, các công việc được chọn kèm thời điểm và cách chọn (khi hoàn thành / bổ sung ở mốc CFG-M09-04). **Kết quả kiểm tra** – nhóm 3: công việc, kết quả (Đạt / Không đạt / Không kiểm tra được / Quá hạn kiểm tra), ghi chú, ảnh, người kiểm tra, thời điểm, công việc làm lại.
- Dùng từ feature khác: **Người cao tuổi**, **cờ nguy cơ** (001); **Công việc** (005, 003); **Liều thuốc, lần giao thuốc mang theo** (006); **Cảnh báo, Sự cố, vùng khoanh** (007); **Ca, trưởng tầng** (008); **Dịch vụ** (004); **Chi phí** (010).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Trong bộ kiểm thử 4 tuần với 10 hoạt động định kỳ và Bộ lập lịch chạy mỗi ngày, sau mỗi lần chạy 100% buổi theo mẫu có ngày diễn ra trong CFG-M04-09 tính từ ngày chạy đều đã tồn tại; 0 buổi trùng (hoạt động, thời điểm bắt đầu); 0 buổi sau ngày kết thúc của mẫu; 0 buổi đã điểm danh bị thay đổi do đổi mẫu.
- **SC-002**: 0 đăng ký hoặc lượt thêm lúc điểm danh thành công khi buổi đã đủ số lượng tối đa, khi người cao tuổi có chỉ định hạn chế bao trùm, khi người hay địa điểm thuộc vùng Đang khoanh vùng, hoặc khi người mang dấu "nghi nhiễm" và buổi là hoạt động nhóm hay chuyến đi.
- **SC-003**: 100% lượt có mặt ở hoạt động có thu phí có đúng một chi phí nháp trỏ về lượt đó; 0 chi phí cho lượt vắng; 100% đính chính hủy lượt có mặt được feature 010 nhận.
- **SC-004**: 0 người cao tuổi được điểm danh rời viện mà chưa có đánh giá Đạt, chưa có trưởng đoàn, hoặc còn liều trong khoảng đi chưa có lần giao thuốc hoặc quyết định không mang thuốc.
- **SC-005**: 100% người được điểm danh rời viện chuyển Hoạt động bên ngoài và 100% người được điểm danh về chuyển Đang lưu trú trong cùng thao tác; 0 người ở Hoạt động bên ngoài quá giờ về dự kiến + CFG-M04-07 mà không có thông báo tới trưởng đoàn và trưởng tầng.
- **SC-006**: 100% người thiếu khi kết thúc điểm danh về hoặc được báo thiếu có đúng một sự cố khẩn cấp đang mở, được tạo trong vòng 1 phút sau thao tác; 0 sự cố thiếu người cho người đã Rời đoàn.
- **SC-007**: Trong 30 kịch bản kiểm thử quy tắc cô lập (đúng ngưỡng, dưới ngưỡng, có vắng mặt, có chỉ định hạn chế, bán trú, người mới vào ở, đã có cảnh báo mở), 100% cảnh báo được tạo đúng ngày và 0 cảnh báo thừa.
- **SC-008**: Với mỗi ca có ít nhất một công việc đủ điều kiện hoàn thành trước mốc CFG-M09-04, số mục được chọn tới mốc đó không nhỏ hơn CFG-M04-11 nhân số công việc đủ điều kiện (làm tròn lên); qua 1.000 ca mô phỏng trên cùng tập 200 công việc hoàn thành rải đều trong ca, tỷ lệ số ca mà mỗi công việc được chọn nằm trong khoảng CFG-M04-11 ± 20% giá trị đó (với mặc định: 4%–6%), và tỷ lệ này không khác nhau quá 20% giữa công việc hoàn thành ở nửa đầu và nửa sau ca; 100% kết quả Không đạt có công việc làm lại trong cùng ca; 100% mục chưa có kết quả lúc hết ca thành "Quá hạn kiểm tra"; 0 công việc gốc bị sửa.
- **SC-009**: Khi có kết nối, với buổi 20 người đã đăng ký sẵn và tối đa 2 người thêm tại chỗ, nhân viên chăm sóc hoàn tất điểm danh trong không quá 3 phút; trưởng đoàn điểm danh rời viện hoặc về cho 15 người đã được đánh giá Đạt trong không quá 2 phút. Đây là mục tiêu nghiệm thu, đo từ lúc mở danh sách tới lúc lưu.
- **SC-010**: 0 lần người thân thấy chỉ định hạn chế, mức độ tham gia, cảnh báo cô lập hay kết quả kiểm tra chất lượng trên cổng.
- **SC-011**: 100% chỉ định tạm có thời điểm kết thúc không muộn hơn CFG-M04-14 kể từ lúc gắn; 100% chỉ định tạm không được xác nhận chuyển Hết hiệu lực đúng thời điểm kết thúc; 100% chỉ định tạm có thông báo tới bác sĩ trực (hoặc mọi Bác sĩ Hoạt động khi không có bác sĩ trực) trong vòng 5 phút.
- **SC-012**: Ngay sau khi một chỉ định hạn chế (chính thức hoặc tạm) được gắn hoặc mở rộng, 0 đăng ký chưa diễn ra bị chỉ định bao trùm còn ở Đã đăng ký; 0 người thấy lý do chỉ định mà không có quyền xem sức khỏe.
- **SC-013**: 0 người bán trú có đăng ký buổi kết thúc sau giờ về theo lịch mà không có đồng ý về muộn Hiệu lực hoặc Đã dùng; 100% đăng ký như vậy làm giờ về dự kiến của ngày được dời trong cùng thao tác.
- **SC-014**: 0 công việc do trưởng tầng được giao thực hiện có trong danh sách kiểm tra chất lượng; 0 báo cáo chất lượng hiển thị tỷ lệ Đạt cho trưởng tầng như một kết quả kiểm tra.
- **SC-015**: 100% buổi trong viện qua hết ngày mà chưa Hoàn tất điểm danh được chuyển Đã điểm danh (dấu "điểm danh không đủ") hoặc Không điểm danh trong vòng 5 phút sau 24:00; 0 ngày có đăng ký hoạt động nhóm "Không ghi nhận" được tính là ngày tính.

## Assumptions

- Số feature `014` theo cột Feature của UC-29, UC-30, UC-31 ở mục 4.2 `docs/phan-tich-yeu-cau.md`, và theo cách spec 001, 005, 006, 007, 010, 011, 012 đã gọi feature 014.
- Khi người cao tuổi chuyển Hoạt động bên ngoài, công việc trong khoảng đi bị **Hủy** theo BR-M04-04 và feature 005 FR-020 (không "tạm dừng" như câu chữ 5.6); khi trở về, feature 005 sinh lại công việc. Xem điểm báo lại 1.
- Buổi hoạt động và chuyến đi dùng chung một thực thể buổi; chuyến đi là buổi của loại "ngoài viện" và có thêm các trường, lệnh ở mục G, H. Đây là cách hiểu ERD (HOAT_DONG, BUOI_HOAT_DONG) vốn không có thực thể chuyến riêng.
- Không có danh sách chờ khi buổi đủ chỗ; người muốn tham gia được đăng ký khi có người hủy. Tài liệu không yêu cầu danh sách chờ.
- Người cao tuổi không dùng hệ thống; nhân viên đăng ký thay theo mong muốn của người cao tuổi; người thân chỉ xem (4.4 cột NT).
- Sở thích được Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc ghi. 4.4 không có dòng cho sở thích; spec coi đây là một phần của theo dõi chăm sóc hằng ngày, gần với quyền của dòng "Checklist, ghi nhận công việc" (điểm báo lại 4).
- "Giao tiếp" và "mức độ tham gia" (8.10) được ghi lúc điểm danh; tâm trạng và hành vi bất thường được ghi ở feature 005. Spec không tạo phiếu ghi tinh thần riêng để tránh ghi trùng.
- Tham số mới của spec (cần bổ sung vào Phụ lục 25, điểm báo lại 6): CFG-M04-13, thời gian sớm nhất được điểm danh rời viện trước giờ rời dự kiến, mặc định \[1 giờ\] (FR-040), cho phù hợp thực tế xuất phát sớm; CFG-M04-14, thời hạn tối đa của chỉ định tạm do điều dưỡng gắn, mặc định \[24 giờ\] (FR-023a, Q-162). Hạn kiểm tra chất lượng là giờ kết thúc ca (Q-163) nên không cần tham số riêng; mốc chọn bổ sung dùng lại CFG-M09-04.
- Điều dưỡng không được gắn chỉ định tạm mới cùng phạm vi cho cùng người trong CFG-M04-14 sau khi chỉ định tạm trước Hết hiệu lực vì không được xác nhận, để chỉ định tạm không thay được chỉ định của bác sĩ (Edge Cases).
- Đồng ý về muộn của bán trú (FR-016a) do Hành chính ghi nhận vì Hành chính quản lý người thân và lịch bán trú (2.3); 4.4 dòng "Hoạt động, ngoài viện" chưa có quyền cho Hành chính (điểm báo lại 7). Phí của phần giờ thêm do feature 010 tính theo quy tắc buổi hiện có (Q-138, Q-140), spec này không đặt quy tắc phí riêng.
- Mức của thông báo quá giờ về là Trung bình theo feature 009 FR-043b (cần hành động trong ca, chưa phải nguy cơ trực tiếp); khi có người thiếu, mức Khẩn cấp đi qua sự cố của feature 007.
- Cách đếm ngày của cảnh báo cô lập (ngày tính, bỏ qua ngày có vắng mặt, FR-054) cố ý khác cách đếm tâm trạng tiêu cực của feature 005 FR-042 (ngày không có ghi nhận làm đứt chuỗi): BR-M04-22 nêu rõ "không tính thời gian vắng mặt", còn BR-M04-11 đếm "ngày liên tiếp" có ghi nhận.
- Kiểm tra chất lượng lấy mẫu theo ca có phạm vi tầng/khu vực; công việc vệ sinh gắn phòng/khu vực của tầng. Chọn theo xác suất ngay khi hoàn thành (Q-163) cho số mục dao động quanh tỷ lệ; mốc bổ sung CFG-M09-04 bảo đảm không dưới tỷ lệ, không cắt bớt khi vượt. Công việc do chính trưởng tầng thực hiện bị loại khỏi mẫu, không có người kiểm tra thay (Q-168).
- Mọi dữ liệu của spec gắn người cao tuổi được lưu theo CFG-M15-03 (NFR-07).

## Điểm cần báo lại về tài liệu nguồn

**Trạng thái (2026-09-27):** theo yêu cầu của người dùng, `docs/nghiep-vu.md` đã được đồng bộ: Q-161 → Q-176 ở mục 24.2 (24.3 không còn dòng nào); thuật ngữ mới ở 2.4; 3.4 (về muộn vì hoạt động); dòng "Đang lưu trú → Hoạt động bên ngoài" của 5.6 (Hủy thay cho Tạm dừng); phần bổ sung của 8.8, 8.9, 8.10; BR-M04-15 → 18, 21 → 23, BR-M05-11, BR-M07-03 được làm rõ; 18.2; 19.3 (quyền của spec này, là căn cứ khi 4.4 khác); CFG-M04-13, CFG-M04-14 mới và phần dùng lại CFG-M04-06, CFG-M09-04 ở Phụ lục 25. **Không sửa theo CLAUDE.md:** `docs/phan-tich-yeu-cau.md` (điểm 2, 4, 15 phần 4.4, UC-30, ERD, DBR chỉ để báo lại). Các spec khác đã được đồng bộ cùng ngày (điểm 16).

1. **5.6 dòng "Đang lưu trú → Hoạt động bên ngoài"** ghi "Tạm dừng công việc trong khoảng thời gian đi", còn **BR-M04-04** ghi công việc chuyển Hủy. Feature 005 FR-020 đã theo BR-M04-04; feature 001 bảng trạng thái ghi "Tạm dừng … tiếp tục công việc đã tạm dừng". Spec này theo BR-M04-04 và feature 005. Nên sửa 5.6 và bảng trạng thái của feature 001 cho khớp.
2. **Permission Matrix 4.4, dòng "Hoạt động, ngoài viện"**: Điều dưỡng và Bác sĩ là "—", nhưng BR-M04-15 cho bác sĩ gắn cờ "không đủ điều kiện", 8.9 yêu cầu "đánh giá khả năng tham gia" và "kiểm tra thuốc cần mang", Q-54 chỉ cho Điều dưỡng ghi liều Mang theo. Đã chốt khi clarify: Q-161 (trưởng đoàn là Trưởng tầng hoặc Nhân viên chăm sóc; người đi cùng là nhân viên bất kỳ có ca; Trưởng tầng đánh giá) và Q-162 (Bác sĩ gắn chỉ định có phạm vi; Điều dưỡng gắn tạm tối đa CFG-M04-14, Bác sĩ xác nhận hoặc gỡ). Cần bổ sung chú thích cho dòng này: BS "T" chỉ với chỉ định hạn chế; ĐD "T" chỉ với chỉ định tạm, ghi sở thích và "Báo thiếu người" khi đi cùng (quyết định "không mang thuốc" là lệnh của feature 006, Q-171). Cần thêm Q-161 → Q-163 vào mục 24.2 và sửa BR-M04-15 cho có chỉ định tạm. *(Cập nhật 2026-09-28: Phụ lục 27 đã cho ĐD "T²⁷" ở dòng "Hoạt động, ngoài viện" theo Q-214; phần "ĐD chỉ Báo thiếu người khi đi cùng" không còn áp.)*
3. **BR-M04-16** ghi liều Mang theo "giao cho người đi cùng ghi nhận"; Q-54 đã chốt chỉ Điều dưỡng ghi nhận (không có điều dưỡng đi cùng thì ghi theo báo lại). Nên sửa câu chữ BR-M04-16 theo Q-54.
4. **Sở thích (8.10)** chưa có dòng trong 4.4 và chưa có thực thể trong ERD. Đề xuất thêm dòng "Sở thích, hồ sơ tinh thần" (TT, ĐD, CS: T; QL: X) và các thực thể DANG_KY_HOAT_DONG, DONG_Y_VE_MUON, SO_THICH, CHI_DINH_HAN_CHE, DANH_SACH_KIEM_TRA, KET_QUA_KIEM_TRA vào ERD miền D, cùng một DBR mới theo cách DBR-12: "Buổi hoạt động là duy nhất theo (hoạt động, thời điểm bắt đầu); mỗi người cao tuổi có tối đa một đăng ký còn hiệu lực cho mỗi buổi" (FR-007, FR-016). Ghi chú: `docs/phan-tich-yeu-cau.md` không sửa theo CLAUDE.md; điểm này chỉ để báo lại.
5. **BR-M04-15 và BR-M05-11** không nêu số phận của buổi, đăng ký đã có khi khu bị khoanh vùng. Đã chốt khi clarify (Q-164): hủy tự động, gỡ vùng không khôi phục, theo cách Q-120 cho lượt thăm (FR-021). Q-165 thêm chặn hoạt động nhóm, chuyến đi với người mang dấu "nghi nhiễm" (FR-015 (g), FR-021a). Cần bổ sung cả hai vào BR-M05-11 (hoặc BR-M04-15) và mục 24.2.
6. **Tham số đề xuất** (cần bổ sung vào Phụ lục 25 nếu được chốt): CFG-M04-13 thời gian sớm nhất được điểm danh rời viện trước giờ rời dự kiến, mặc định \[1 giờ\] (FR-040); CFG-M04-14 thời hạn tối đa của chỉ định tạm, mặc định \[24 giờ\] (FR-023a, Q-162). Spec dùng lại hai tham số ngoài nghĩa gốc, cần ghi thêm cột "Dùng tại" ở Phụ lục 25: CFG-M09-04 (gốc: lập bản nháp bàn giao trước khi kết ca) làm mốc chọn bổ sung kiểm tra chất lượng (FR-059a), vì cùng nghĩa "mốc cuối ca"; CFG-M04-06 (gốc: ngưỡng ghi nhận muộn của công việc, BR-M04-12) làm ngưỡng "ghi nhận muộn" và mốc nhắc của điểm danh buổi (FR-030). Nếu cơ sở muốn tách, cần tham số riêng.
7. **Bán trú tham gia buổi, chuyến đi vượt giờ về theo lịch**: tài liệu không nêu. Đã chốt khi clarify (Q-167): được khi Hành chính ghi nhận đồng ý về muộn của người đại diện cho đúng buổi; giờ về dự kiến của ngày tự dời (FR-016a). Cần bổ sung vào 3.3 hoặc 3.4, thêm quyền T cho Hành chính (ghi đồng ý về muộn) vào chú thích dòng "Hoạt động, ngoài viện" của 4.4, thêm Q-167 vào mục 24.2; feature 005, 010, 011, 003 cần nhận giờ về dự kiến mới; feature 012 cần cho người đại diện đồng ý qua cổng.
8. **BR-M04-23** không nêu ai kiểm tra công việc do chính trưởng tầng thực hiện và xử lý khi quá hạn kiểm tra. Đã chốt khi clarify (Q-168): loại các công việc đó khỏi mẫu, không kiểm tra (FR-060); nên ghi vào BR-M04-23 và 18.2. Spec đặt kết quả "Quá hạn kiểm tra" cho mục chưa kiểm tra lúc hết ca (FR-063). Câu chữ "mỗi ca chọn ngẫu nhiên [5%]" của BR-M04-23 cần sửa theo Q-163: xét chọn ngẫu nhiên từng công việc khi hoàn thành với tỷ lệ CFG-M04-11, bổ sung ở mốc CFG-M09-04 trước khi kết ca. Theo Q-163 (hạn là hết ca), ca không có trưởng tầng trực (thường là ca đêm) sẽ có nhiều mục quá hạn trừ khi trưởng tầng được giao ghi từ xa; cơ sở cần xem lại khi vận hành, có thể bổ sung quyền cho Người phụ trách ca (hiện 4.4 và FR-066 không cho).
9. **5.5** không có chuyển Hoạt động bên ngoài → Tạm vắng. Đã chốt khi clarify (Q-166): giữ nguyên 5.5, người thân không đón thẳng người cao tuổi từ điểm đến của chuyến đi; người cao tuổi phải được điểm danh về viện trước (FR-045). Nên ghi rõ ở 8.9 và thêm Q-166 vào mục 24.2.
10. **Feature 005 bảng kết quả 8.6** chưa có "giao tiếp"; spec ghi giao tiếp lúc điểm danh (FR-029). Nếu cơ sở muốn ghi giao tiếp hằng ngày ngoài hoạt động, cần thêm kết quả vào feature 005.
11. **Feature 007** cần: nguồn "hoạt động ngoài viện" đã có ở FR-040; đặt mức mặc định Khẩn cấp cho loại sự cố "đi lạc hoặc không trở về" ở FR-042 (BR-M04-17); thêm loại cảnh báo "nguy cơ cô lập" (mức Nhẹ, khóa gộp theo người cao tuổi) vào khóa loại FR-028, với người nhận là trưởng tầng của tầng người cao tuổi thay cho điều dưỡng phụ trách (BR-M04-22); nhận diễn biến "đã tìm thấy, đã trở về" cho sự cố thiếu người và "đã tham gia hoạt động nhóm" cho cảnh báo cô lập; FR-065 bổ sung hủy tự động buổi và đăng ký khi khoanh vùng (Q-164); FR-060 cung cấp sự kiện gắn, gỡ dấu "nghi nhiễm" cho spec này (Q-165).
12. **Feature 009** cần thêm dòng "014 – Mọi thông báo của feature 014, theo bảng FR-071 của spec 014" vào bảng mức FR-043b.
13. **Thuật ngữ 2.4**: đề xuất thêm "Buổi", "Chuyến đi", "Hoạt động nhóm", "Chỉ định hạn chế hoạt động", "Chỉ định tạm", "Đồng ý về muộn", "Ngày tính", "Không ghi nhận", "Danh sách kiểm tra chất lượng", "Người đi cùng".
14. **Quyết định chốt ngày 2026-09-27 (Clarifications, Q-161 → Q-168), cần đưa vào mục 24.2**:

    | Mã | Vấn đề | Quyết định | Căn cứ |
    | --- | --- | --- | --- |
    | Q-161 | Ai làm trưởng đoàn, người đi cùng, ai đánh giá khả năng tham gia chuyến đi | Trưởng đoàn: Trưởng tầng hoặc Nhân viên chăm sóc; người đi cùng: nhân viên bất kỳ có ca chồng thời gian chuyến, kể cả Điều dưỡng; Trưởng tầng đánh giá | FR-035, FR-036, FR-046 |
    | Q-162 | Cờ "không đủ điều kiện" có phạm vi không; điều dưỡng có gắn tạm không | Có phạm vi (mọi hoạt động / hoạt động nhóm / ngoài viện / theo loại), Bác sĩ gắn, là "chỉ định hạn chế" của BR-M04-22; Điều dưỡng gắn tạm tối đa CFG-M04-14 \[24 giờ\], Bác sĩ xác nhận hoặc gỡ, quá hạn thì hết hiệu lực | FR-023 → FR-023c |
    | Q-163 | Kiểm tra chất lượng chọn mẫu lúc nào, hạn bao lâu | Chọn ngẫu nhiên ngay khi công việc hoàn thành theo CFG-M04-11, bổ sung ở mốc CFG-M09-04 trước khi kết ca; hạn là hết ca; làm lại trong cùng ca | FR-059, FR-059a, FR-063, FR-064 |
    | Q-164 | Buổi và đăng ký đã có khi khu bị khoanh vùng | Tự hủy buổi trong vùng và đăng ký của người trong vùng, lý do "khoanh vùng"; gỡ vùng không khôi phục | FR-021 |
    | Q-165 | Người mang dấu "nghi nhiễm" có tham gia hoạt động nhóm, chuyến đi không | Chặn đăng ký, điểm danh có mặt ở hoạt động nhóm và chuyến đi; tự hủy đăng ký nhóm khi gắn dấu; hoạt động cá nhân vẫn được; ngày mang dấu không tính cho cảnh báo cô lập | FR-015 (g), FR-021a, FR-054 |
    | Q-166 | Người thân có đón thẳng người cao tuổi từ điểm đến của chuyến đi không | Không; điểm danh về viện trước rồi Cho tạm vắng theo 14.3; giữ nguyên 5.5 | FR-045 |
    | Q-167 | Người bán trú có đăng ký buổi, chuyến đi kết thúc sau giờ về theo lịch không | Được khi Hành chính ghi nhận đồng ý về muộn của người đại diện cho đúng buổi; giờ về dự kiến của ngày tự dời, feature 003, 005, 010, 011 nhận giờ mới | FR-016, FR-016a |
    | Q-168 | Công việc do chính trưởng tầng thực hiện có vào mẫu kiểm tra chất lượng không | Không; loại khỏi mẫu, không kiểm tra; báo cáo ghi rõ ngoài phạm vi | FR-060, FR-065 |

15. **Sai khác khác với tài liệu nguồn**:
    - **UC-30** chỉ có actor Trưởng tầng; theo Q-161, Nhân viên chăm sóc làm trưởng đoàn cũng điểm danh rời/về, chuyển trưởng đoàn. Đề xuất thêm Nhân viên chăm sóc (vai trò trưởng đoàn) vào UC-30 (chỉ báo lại, không sửa `docs/phan-tich-yeu-cau.md`).
    - **BR-M04-17** viết "cảnh báo trưởng đoàn và trưởng tầng" khi quá giờ về. Spec dùng thông báo của feature 009 (FR-049), không tạo cảnh báo của feature 007, vì chuyến quá giờ chưa phải tình trạng của một người cao tuổi cần xử lý như cảnh báo; khi có người thiếu, sự cố khẩn cấp đi qua feature 007. Đề xuất sửa câu chữ thành "báo trưởng đoàn và trưởng tầng".
16. **Đồng bộ các spec đã viết** — *đã đồng bộ ngày 2026-09-27: mỗi spec 001, 002, 003, 005, 006, 007, 008, 009, 010, 011, 012 có mục "Cập nhật 2026-09-27 (đồng bộ với spec 014)" và các chỗ được sửa: 001 bảng trạng thái (hai dòng chuyến đi); 002 FR-031; 003 FR-049a mới; 005 FR-018a, FR-020a, FR-026a mới; 006 FR-048b mới; 007 FR-028, FR-042, FR-065; 008 FR-039 (e); 009 dòng 014 ở bảng FR-043b; 010 bảng nguồn và bảng giao tiếp; 011 bảng giao tiếp; 012 FR-046c mới.* Nội dung đã đồng bộ:
    - **001**: bảng trạng thái, chuyển vào/ra Hoạt động bên ngoài: người thực hiện gồm trưởng đoàn hoặc trưởng tầng thay (FR-040, FR-044); tác động "Hủy công việc … sinh lại khi về" thay cho "Tạm dừng … tiếp tục" (điểm 1); nhận chuyển về Đang lưu trú do đính chính điểm danh (FR-031a).
    - **002**: FR-031 thêm phạm vi của người đi cùng (người tham gia chuyến trong thời gian chuyến, FR-035); FR-019 ghi ngoại lệ "Báo thiếu người" của người đi cùng là quyền ghi sự cố vốn có, không phải quyền do nhiệm vụ.
    - **003**: nhận yêu cầu sinh công việc vệ sinh làm lại (FR-064); lấy giờ về theo ngày của bán trú từ feature 005 cho chỗ khu nghỉ (Q-47, Q-176).
    - **005**: nhận yêu cầu sinh công việc làm lại (FR-064); nhận giờ về dự kiến mới của bán trú theo đồng ý về muộn và cung cấp giờ về theo ngày cho 003, 010, 011 (FR-016a, Q-176); nhận sự kiện buổi kết thúc (FR-014a) và đính chính điểm danh rời/về (FR-031a).
    - **006**: thêm lệnh "không mang thuốc" cho chuyến đi (FR-038, Q-171); nhận hủy chuyến, người Không đi, dời chuyến để nhắc Nhận lại (FR-038a); nhận đính chính thời điểm rời/về (FR-031a).
    - **007**: theo điểm 11.
    - **008**: bản nháp bàn giao gồm chuyến đang đi và chuyến quá giờ về (FR-039 (e)); trưởng đoàn giữ nhiệm vụ qua hết ca (FR-035).
    - **009**: thêm dòng 014 vào bảng FR-043b (điểm 12).
    - **010**: với chuyến đi, nguồn là bản ghi điểm danh rời viện, ngày là ngày rời thực tế (FR-032); nhận hủy lượt do đính chính rời viện (FR-031a); lấy giờ về theo ngày của bán trú từ feature 005.
    - **011**: nhận danh sách người tham gia ở mọi lần thay đổi và đính chính rời/về (FR-034, FR-031a); lấy giờ về theo ngày của bán trú từ feature 005.
    - **012**: cho người đại diện ghi, rút đồng ý về muộn qua cổng (FR-016a); định nghĩa "số hoạt động đã tham gia" theo FR-033.
17. **Quyết định chốt ngày 2026-09-27 theo mặc định đề xuất (người dùng đồng ý tất cả sau khi rà checklist), cần đưa vào mục 24.2 cùng Q-161 → Q-168**:

    | Mã | Vấn đề | Quyết định | Căn cứ |
    | --- | --- | --- | --- |
    | Q-169 | Buổi hết ngày mà chưa điểm danh đủ | Đăng ký chưa có kết quả nhận "Không ghi nhận"; buổi sang Đã điểm danh (dấu "điểm danh không đủ") hoặc Không điểm danh; ngày đó không là ngày tính; sửa bằng đính chính | FR-014, FR-014a |
    | Q-170 | Đính chính điểm danh rời/về của chuyến đi | Chỉ Trưởng tầng; không đổi người, loại; sửa thời điểm, hoặc Hủy ghi nhận trước khi chuyến Đã về, kéo theo chuyển trạng thái và sự kiện cho 005, 006, 010, 011 | FR-031a |
    | Q-171 | Thay đổi chuyến sau khi chuẩn bị | Đăng ký đóng khi người đầu tiên rời viện; rời viện chỉ từ Đang lưu trú; "không mang thuốc" là lệnh của feature 006; hủy chuyến, Không đi báo Nhận lại thuốc; dời sang ngày khác thì đánh giá lại | FR-015 (e), FR-038, FR-038a, FR-040 |
    | Q-172 | An toàn chuyến | Quá giờ về + 2 × CFG-M04-07 báo Quản lý viện; chuyển trưởng đoàn giữa chuyến cho người đi cùng là TT/CS, không có thì trưởng tầng tạm chịu trách nhiệm; nhiệm vụ giữ qua hết ca; mỗi người kèm riêng tối đa một người | FR-035, FR-035a, FR-036, FR-049a |
    | Q-173 | Tham gia, phí và ngày tính | "Bỏ giữa chừng" vẫn là Có mặt, tính phí, tính tham gia; tỷ lệ tham gia bỏ vắng do không có mặt ở viện khỏi mẫu số; ngày có vắng một phần không là ngày tính | FR-029, FR-033, FR-054, FR-070 |
    | Q-174 | Kiểm tra chất lượng: phạm vi và ngoại tuyến | Loại vệ sinh khu vực chung không gắn tầng; công việc ngoại tuyến xét khi đồng bộ nếu ca còn diễn ra; Không đạt không đổi trạng thái giường | FR-059, FR-060, FR-064 |
    | Q-175 | Địa điểm, người phụ trách, quyền trên hoạt động toàn viện | Địa điểm là phòng/khu vực gắn tầng; người phụ trách là TT hoặc CS; hoạt động toàn viện do trưởng tầng tạo hoặc trưởng tầng nơi đặt địa điểm quản lý; chuyến toàn viện: mỗi người do trưởng tầng của mình đánh giá | FR-002a, FR-002b, FR-036 |
    | Q-176 | Ai sở hữu giờ về theo ngày của bán trú; rút đồng ý về muộn | Feature 005 (có mặt theo ngày, 3.4) sở hữu; 003, 010, 011 lấy từ 005; đồng ý "Đã dùng" từ giờ bắt đầu, không rút sau đó | FR-016a |


**(2026-09-28, góp ý nghiệp vụ Q-207 → Q-222) Tài liệu nguồn đã thay đổi, spec cần rà lại.** *(2026-09-29: mọi điểm dưới đây đã xử lý, xem nhãn "Đã xử lý" từng điểm.)* Các điểm dưới đây đã có trong `docs/nghiep-vu.md` và `docs/luong-nghiep-vu.md`; spec **chưa** được sửa theo, và cần chạy `/speckit-clarify` hoặc cập nhật FR tương ứng.

1. **[Đã xử lý 2026-09-28, xem "Cập nhật 2026-09-28 (Q-214)"]** **Điều dưỡng được làm Trưởng đoàn và làm việc của Nhân viên chăm sóc (Q-214; 2.4, 8.8, 8.9, 19.3, Phụ lục 27 ²⁷).** Điều dưỡng được đăng ký, điểm danh, làm người phụ trách buổi, làm Trưởng đoàn. Giới hạn "chỉ Báo thiếu người" (FR-035, FR-046, FR-067) chỉ còn áp cho người đi cùng không có vai trò Trưởng tầng, Nhân viên chăm sóc hoặc Điều dưỡng. Tài liệu nguồn 8.8, 8.9, 19.3, 24.2 (Q-214) và BF-15 được sửa cùng ngày để khớp.
2. **[Đã xử lý 2026-09-28: FR-040; lịch xe ở spec 019]** **Lịch xe đưa đón (Q-209, Q-220; 7.7, BR-M03-16, UC-90).** Chuyến đi dùng xe của viện phải có lịch xe trước khi điểm danh rời viện (FR-040).
