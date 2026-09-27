# Feature Specification: Quản lý hoạt động và chất lượng chăm sóc

**Feature Branch**: `014-activity-care-quality`

**Created**: 2026-09-27

**Status**: Draft

**Input**: User description: "Quản lý hoạt động và chất lượng chăm sóc theo docs/nghiep-vu.md Module 04 mục 8.8–8.10 và BR-M04-15 đến BR-M04-18, BR-M04-21 đến BR-M04-23: hoạt động và hoạt động định kỳ tự sinh buổi; đăng ký có giới hạn và điều kiện sức khỏe; hoạt động ngoài viện với điểm danh rời/về, thuốc mang theo, sự cố khi thiếu người; theo dõi tinh thần và cảnh báo nguy cơ cô lập; kiểm tra chất lượng ngẫu nhiên công việc đã hoàn thành."

## Clarifications

### Session 2026-09-27

- Q: Ai được làm trưởng đoàn, người đi cùng và ai đánh giá khả năng tham gia chuyến đi? → A: Trưởng đoàn là Trưởng tầng hoặc Nhân viên chăm sóc; người đi cùng là nhân viên bất kỳ có ca chồng thời gian chuyến đi, kể cả Điều dưỡng; Trưởng tầng đánh giá khả năng tham gia dựa trên thông tin hệ thống hiển thị. Điều dưỡng đi cùng không có quyền lệnh của chuyến ngoài "Báo thiếu người" (đề xuất Q-161; người dùng chọn phương án A). Áp dụng tại FR-035, FR-036, FR-046, FR-067, bảng trạng thái người tham gia.
- Q: Cờ "không đủ điều kiện" (BR-M04-15) có phạm vi hay là cờ chung; điều dưỡng có được gắn tạm không? → A: Có phạm vi (mọi hoạt động / mọi hoạt động nhóm / ngoài viện / theo loại hoạt động), do Bác sĩ gắn, và là "chỉ định hạn chế" của BR-M04-22. Điều dưỡng được gắn chỉ định tạm, hiệu lực tối đa CFG-M04-14 (đề xuất, mặc định \[24 giờ\]); Bác sĩ xác nhận thành chính thức hoặc gỡ; quá hạn không xác nhận thì hết hiệu lực (đề xuất Q-162; người dùng chọn phương án C). Áp dụng tại FR-023 → FR-023c, bảng trạng thái chỉ định hạn chế.
- Q: Kiểm tra chất lượng chọn mẫu lúc nào và hạn kiểm tra bao lâu? → A: Chọn rải trong ca: mỗi công việc đủ điều kiện được xét chọn ngẫu nhiên ngay khi hoàn thành; hạn kiểm tra là hết chính ca đó; công việc làm lại thuộc ca đó (đề xuất Q-163; người dùng chọn phương án B). Tại mốc CFG-M09-04 trước khi kết ca, hệ thống chọn bổ sung nếu số đã chọn chưa đạt tỷ lệ CFG-M04-11 (FR-059a). Áp dụng tại FR-059 → FR-064.

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

Trưởng tầng hoặc nhân viên chăm sóc đăng ký người cao tuổi vào buổi hoạt động. Hệ thống chặn đăng ký khi buổi đã đủ số lượng tối đa, khi người cao tuổi có chỉ định hạn chế do bác sĩ gắn bao trùm hoạt động đó, hoặc khi người cao tuổi hay địa điểm của buổi đang trong khu bị khoanh vùng. Hệ thống gợi ý người tham gia theo sở thích, mức chăm sóc và điều kiện sức khỏe.

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

---

### User Story 3 - Điểm danh buổi hoạt động và tạo chi phí khi có thu phí (Priority: P1)

Người phụ trách buổi (trưởng tầng hoặc nhân viên chăm sóc) điểm danh từng người đăng ký: có mặt hoặc vắng kèm lý do; với người có mặt ghi mức độ tham gia, giao tiếp và tình trạng sau hoạt động. Người không đăng ký trước được thêm tại lúc điểm danh nếu đủ điều kiện. Điểm danh có mặt ở hoạt động có thu phí tự tạo chi phí nháp. Bản ghi điểm danh không sửa, không xóa; sai sót xử lý bằng đính chính.

**Why this priority**: Điểm danh là căn cứ duy nhất cho chi phí hoạt động (BR-M04-18, DBR-15), truy vết tiếp xúc khi có lây nhiễm (BR-M05-10), tỷ lệ tham gia (18.2) và phát hiện nguy cơ cô lập (BR-M04-22).

**Independent Test**: Buổi "Tập đàn" có thu phí với 4 người đăng ký; điểm danh 3 có mặt, 1 vắng; thêm 1 người tại chỗ; kiểm tra 4 chi phí nháp; đính chính một người từ có mặt sang vắng; kiểm tra feature 010 nhận sự kiện hủy.

**Acceptance Scenarios**:

1. **Given** buổi "Tập đàn" 09:00 ngày 07/10 có thu phí, dịch vụ "Lớp đàn" trong danh mục, 4 người đăng ký A, B, C, D, **When** S điểm danh A, B, C có mặt (mức độ tham gia, giao tiếp, tình trạng sau) và D vắng lý do "từ chối", rồi hoàn tất điểm danh, **Then** buổi chuyển Đã điểm danh; feature 010 nhận ba lượt điểm danh có mặt và tạo ba chi phí nháp trỏ về từng lượt (BR-M04-18, BR-M11-01, DBR-15); D không có chi phí.
2. **Given** E không đăng ký trước nhưng muốn tham gia và buổi còn chỗ, **When** S thêm E lúc điểm danh, **Then** hệ thống kiểm tra như đăng ký (FR-015) rồi ghi E có mặt, dấu "tham gia không đăng ký trước"; E có chi phí nháp.
3. **Given** buổi đã đủ 20 người có mặt, **When** S thêm người thứ 21 lúc điểm danh, **Then** hệ thống chặn (BR-M04-15).
4. **Given** S đã ghi C có mặt nhưng thực tế C vắng, **When** S tạo bản đính chính kèm lý do, **Then** bản gốc giữ nguyên, hiện dấu "đã đính chính"; feature 010 nhận sự kiện hủy lượt điểm danh để hủy chi phí nháp hoặc tạo khoản điều chỉnh nếu đã chốt (FR-031).
5. **Given** buổi kết thúc lúc 10:00, **When** tới 10:00 + CFG-M04-06 (mặc định \[2 giờ\]) mà chưa hoàn tất điểm danh, **Then** người phụ trách buổi và T được nhắc; điểm danh ghi sau mốc này gắn nhãn "ghi nhận muộn" (FR-030).
6. **Given** A Đang lưu trú có đăng ký buổi 15:00 nhưng chuyển Tạm vắng lúc 13:00, **When** tới giờ điểm danh, **Then** A được hiển thị sẵn "Vắng – đang vắng mặt"; người điểm danh không cần ghi thêm (FR-028).
7. **Given** nhân viên vệ sinh, bác sĩ hoặc điều dưỡng, **When** tìm cách điểm danh, **Then** hệ thống không cho phép (4.4 dòng "Hoạt động, ngoài viện").

---

### User Story 4 - Chuẩn bị và điểm danh rời viện cho hoạt động ngoài viện (Priority: P1)

Với buổi thuộc hoạt động ngoài viện (chuyến đi), trước khi đi trưởng tầng phân công trưởng đoàn (Trưởng tầng hoặc Nhân viên chăm sóc) và người đi cùng (nhân viên bất kỳ có ca chồng thời gian chuyến, kể cả Điều dưỡng), đánh giá khả năng tham gia của từng người đăng ký, và điều dưỡng phụ trách chuẩn bị thuốc cần mang. Khi xuất phát, trưởng đoàn điểm danh rời viện; người tham gia chuyển sang Hoạt động bên ngoài, liều thuốc trong khoảng đi chuyển Mang theo, công việc trong khoảng đi bị hủy.

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
8. **Given** T muốn phân công điều dưỡng D1 làm trưởng đoàn, **When** lưu, **Then** hệ thống chặn vì trưởng đoàn phải là Trưởng tầng hoặc Nhân viên chăm sóc; **When** T phân công D1 làm người đi cùng, D1 có ca 06:00–14:00, **Then** được chấp nhận; **When** T phân công nhân viên chăm sóc S3 không có ca chồng thời gian chuyến, **Then** hệ thống chặn (FR-035, Q-161). **When** S1 (trưởng đoàn) tìm cách ghi đánh giá khả năng tham gia, **Then** hệ thống chặn vì chỉ Trưởng tầng đánh giá (FR-036).

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
- **Bán trú**: chỉ đăng ký được buổi nằm trong khung có mặt theo lịch (3.3); ngày không điểm danh đến thì lúc điểm danh buổi hiển thị "Vắng – không đến" (FR-016, FR-028). Bán trú không đi được hoạt động ngoài viện có giờ về sau giờ về theo lịch, trừ khi hành chính ghi nhận đồng ý của người đại diện (Assumptions).
- **Buổi bị dời giờ**: đăng ký được kiểm tra lại; người không còn đủ điều kiện (trùng giờ, ngoài khung bán trú, khoanh vùng) bị hủy đăng ký kèm lý do và được báo; feature 005 nhận thời điểm mới (FR-012).
- **Số lượng tối đa bị giảm dưới số đã đăng ký**: không tự hủy đăng ký; hệ thống chặn giảm hoặc yêu cầu trưởng tầng chọn đăng ký cần hủy (FR-003).
- **Hai người cùng đăng ký chỗ cuối**: chỉ lần lưu đầu tiên được chấp nhận; lần sau bị chặn vì đủ số lượng (theo feature 000 FR-021).
- **Chỉ định hạn chế gắn trong lúc buổi đang diễn ra**: không hủy điểm danh đã ghi; người điểm danh được hiển thị chỉ định mới (FR-025).
- **Khoanh vùng được đặt khi chuyến ngoài viện đang đi**: người đang đi thuộc vùng vẫn được điểm danh về; khi về, hệ thống báo trưởng đoàn và trưởng tầng rằng người đó trở về vùng khoanh (feature 007 quyết định cách ly tiếp theo).
- **Người đi cùng hoặc trưởng đoàn vắng đột xuất trước giờ rời**: trưởng tầng phân công lại; không có trưởng đoàn thì không điểm danh rời viện được (FR-035).
- **Trưởng đoàn không điểm danh về (mất kết nối, quên)**: cảnh báo quá giờ về theo BR-M04-17; trưởng tầng được điểm danh về thay khi đoàn đã ở viện (FR-044).
- **Người đi trở về sớm một mình** (ví dụ mệt, có nhân viên đưa về): trưởng đoàn hoặc trưởng tầng điểm danh về riêng người đó; chuyến vẫn Đang đi (FR-044).
- **Người tham gia qua đời trong chuyến**: chuyển trạng thái qua lệnh Ghi nhận qua đời của feature 004; người đó hiển thị "Rời đoàn – qua đời", không tạo sự cố thiếu người (FR-045).
- **Mất kết nối khi điểm danh rời viện hoặc về**: điểm danh được ghi tạm và đồng bộ sau (Q-01); với "Báo thiếu người" và kết thúc điểm danh về có người thiếu, việc tạo sự cố khẩn cấp theo ngoại lệ bắt buộc trực tuyến của 8.6: thiết bị phải hướng dẫn gọi điện cho trưởng tầng ngay (Assumptions).
- **Điểm danh có mặt ở hoạt động có thu phí rồi đính chính sau khi kỳ chi phí đã chốt**: feature 010 xử lý bằng khoản điều chỉnh (DBR-17).
- **Mẫu kiểm tra chất lượng khi ca có ít công việc**: có ít nhất 1 công việc đủ điều kiện thì mốc bổ sung bảo đảm tối thiểu 1 mục (làm tròn lên, FR-059a); không có công việc nào thì danh sách của ca trống.
- **Công việc hoàn thành trong 30 phút cuối ca được chọn**: trưởng tầng chỉ còn ít thời gian; mục không kịp kiểm tra thành "Quá hạn kiểm tra" (FR-063). Đây là hệ quả đã chấp nhận của Q-163.
- **Ca không có trưởng tầng trực** (ca đêm, hoặc tầng tạm chưa có trưởng tầng được giao): danh sách vẫn được lập; trưởng tầng được giao ghi được trong thời gian ca dù không có tên trong ca; không có ai ghi thì các mục thành "Quá hạn kiểm tra" và Quản lý viện được báo (FR-063, FR-066; Q-84, Q-90; điểm báo lại 8).
- **Chỉ định tạm của điều dưỡng hết hạn khi bác sĩ chưa xem**: chỉ định Hết hiệu lực; nếu người cao tuổi vẫn cần hạn chế, điều dưỡng báo bác sĩ để gắn chỉ định chính thức; điều dưỡng không gắn chỉ định tạm liên tiếp để kéo dài được khi chỉ định tạm trước đó vừa hết hiệu lực chưa quá CFG-M04-14 (FR-023a, Assumptions).
- **Công việc làm lại cũng Không đạt**: công việc làm lại là công việc bình thường, có thể được chọn ở mẫu của ca sau; không tự sinh vòng lặp làm lại.
- **Cảnh báo cô lập với người mới vào ở**: chuỗi chỉ tính từ ngày Hoàn tất tiếp nhận; người vào ở dưới CFG-M04-10 ngày không bị cảnh báo (FR-054).

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
- **Ngày tính**: ngày người cao tuổi ở Đang lưu trú (bán trú: ngày có điểm danh đến) trọn ngày và không có chỉ định hạn chế bao trùm hoạt động nhóm (BR-M04-22).
- **Danh sách kiểm tra chất lượng**: tập công việc được hệ thống chọn ngẫu nhiên trong một ca của một tầng để trưởng tầng kiểm tra (BR-M04-23).

Nguồn ghi ở cuối từng FR là mã quy tắc, use case hoặc quyết định cụ thể.

#### A. Danh mục loại hoạt động và hoạt động

- **FR-001**: Hệ thống MUST quản lý danh mục loại hoạt động (nhóm 1) gồm tối thiểu các loại của 8.8: giải trí, vận động, phục hồi, giao lưu, xem TV, vui chơi, đi dạo, ngoài viện. Mỗi loại có dấu "hoạt động nhóm" (mặc định bật cho mọi loại trừ "xem TV" và "đi dạo"), dấu "ngoài viện" (chỉ loại "ngoài viện") và các nhóm sở thích liên quan (FR-051). Quản lý viện tạo, sửa, ngừng hiệu lực loại; loại đã được tham chiếu MUST NOT bị xóa. *(Nguồn: 8.8, 1.5 nhóm 1)*
- **FR-002**: Trưởng tầng MUST khai báo được hoạt động (nhóm 1) gồm: tên, loại, phạm vi tổ chức (một tầng/khu vực hoặc toàn viện), địa điểm mặc định (khu vực trong viện, hoặc điểm đến với chuyến đi), người phụ trách mặc định, thời lượng, số lượng tối đa, đối tượng (mức chăm sóc, loại hình lưu trú, tầng/khu vực được mời), có thu phí hay không và dịch vụ tương ứng trong danh mục dịch vụ của feature 004 khi có thu phí. Trưởng tầng chỉ khai báo hoạt động cho tầng mình phụ trách hoặc toàn viện. *(Nguồn: 8.8 "Quản lý", UC-29, 4.4)*
- **FR-003**: Sửa hoạt động MUST áp cho buổi được sinh sau thời điểm sửa và buổi chưa diễn ra, không áp cho buổi đã có điểm danh. Giảm số lượng tối đa dưới số đăng ký hiện có của một buổi chưa diễn ra MUST bị chặn cho buổi đó, trừ khi trưởng tầng chọn các đăng ký cần hủy kèm lý do. *(Suy ra từ BR-M04-21, BR-M04-15)*
- **FR-004**: Ngừng hiệu lực hoạt động MUST hủy mọi buổi chưa có điểm danh từ ngày ngừng, với lý do "ngừng hoạt động"; hoạt động đã có buổi MUST NOT bị xóa. *(Nguồn: 1.5 nhóm 1)*

#### B. Hoạt động định kỳ và sinh buổi

- **FR-005**: Hoạt động MAY có mẫu lặp: các ngày trong tuần (hoặc chu kỳ theo ngày), giờ bắt đầu, giờ kết thúc, ngày bắt đầu, ngày kết thúc (nếu có), và danh sách người tham gia thường xuyên. *(Nguồn: 8.8 "(Bổ sung) Hoạt động định kỳ")*
- **FR-006**: Bộ lập lịch MUST sinh buổi từ mẫu lặp cho mọi ngày trong CFG-M04-09 (mặc định \[7 ngày\]) tính từ ngày chạy; mỗi lần chạy chỉ sinh các buổi còn thiếu. *(Nguồn: BR-M04-21, CFG-M04-09, UC-29)*
- **FR-007**: Buổi MUST là duy nhất theo (hoạt động, thời điểm bắt đầu); chạy lại hoặc chạy trễ MUST NOT tạo buổi trùng. Buổi đã bị hủy thủ công MUST NOT được sinh lại khi Bộ lập lịch chạy lần sau. *(Suy ra từ DBR-12, NFR-04)*
- **FR-008**: Khi sinh buổi, hệ thống MUST đăng ký sẵn mỗi người tham gia thường xuyên đạt các điều kiện chặn của FR-015 tại thời điểm sinh; người không đạt MUST không được đăng ký, và trưởng tầng của hoạt động MUST nhận danh sách người thường xuyên không được đăng ký kèm lý do (gộp một lần mỗi lần chạy). *(Nguồn: 8.8, BR-M04-15)*
- **FR-009**: Thay đổi mẫu lặp (giờ, ngày trong tuần, ngày kết thúc) MUST có lý do và ngày áp dụng; hệ thống MUST hủy các buổi chưa diễn ra và chưa có điểm danh từ ngày áp dụng mà không còn khớp mẫu mới, với lý do "đổi mẫu", rồi sinh buổi theo mẫu mới trong CFG-M04-09. Buổi khớp cả mẫu cũ và mẫu mới (cùng thời điểm bắt đầu) MUST được giữ cùng đăng ký. Buổi đã có điểm danh MUST giữ nguyên. Người có đăng ký trên buổi bị hủy mà không phải người thường xuyên MUST được báo qua người đã đăng ký cho họ. *(Nguồn: BR-M04-21)*
- **FR-010**: Thêm người vào danh sách thường xuyên MUST đăng ký họ vào các buổi đã sinh chưa diễn ra (theo FR-015); bỏ người khỏi danh sách MUST hủy các đăng ký sẵn của họ ở buổi chưa diễn ra, giữ đăng ký thêm tay. *(Suy ra từ 8.8)*

#### C. Buổi hoạt động

- **FR-011**: Trưởng tầng MUST tạo được buổi lẻ (không từ mẫu) cho hoạt động đang hiệu lực, với thời điểm trong tương lai. *(Nguồn: UC-29)*
- **FR-012**: Lệnh "Dời buổi" (đổi thời điểm, địa điểm hoặc người phụ trách) MUST có lý do, chỉ áp cho buổi chưa bắt đầu; hệ thống MUST kiểm tra lại các điều kiện chặn của FR-015 cho mọi đăng ký, hủy đăng ký không còn đạt kèm lý do, và báo feature 005 (công việc "hoạt động"), feature 006 và 011 (với chuyến đi) thời điểm mới. *(Suy ra từ BR-M04-15, BR-M04-21)*
- **FR-013**: Lệnh "Hủy buổi" MUST có lý do và chỉ áp cho buổi chưa có điểm danh; mọi đăng ký của buổi chuyển Đã hủy; feature 005 nhận sự kiện để hủy công việc "hoạt động" Chưa đến hạn (feature 005 FR-008); với chuyến đi, feature 011 nhận sự kiện hủy chuyến. *(Nguồn: UC-29; feature 005, 011 bảng giao tiếp)*
- **FR-014**: Buổi có thời điểm kết thúc đã qua mà chưa có điểm danh nào vẫn ở Đã lên lịch cho tới khi được điểm danh hoặc hủy; sau CFG-M04-06 kể từ thời điểm kết thúc, hủy buổi như vậy MUST ghi lý do "buổi không diễn ra". *(Suy ra từ 8.8 "điểm danh")*

**Bảng trạng thái buổi hoạt động trong viện** *(8.8, BR-M04-21; quyền theo 4.4 dòng "Hoạt động, ngoài viện")*:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái đích | Ai được thực hiện | Điều kiện (chặn nếu không đạt) | Tác động tự động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Sinh từ mẫu | Đã lên lịch | Hệ thống (Bộ lập lịch) | Trong CFG-M04-09; chưa có buổi cùng (hoạt động, thời điểm bắt đầu) (FR-007) | Đăng ký sẵn người thường xuyên (FR-008) |
| (chưa có) | Tạo buổi lẻ | Đã lên lịch | Trưởng tầng | Hoạt động hiệu lực; thời điểm tương lai (FR-011) | — |
| Đã lên lịch | Dời buổi | Đã lên lịch | Trưởng tầng | Buổi chưa bắt đầu; có lý do (FR-012) | Kiểm tra lại đăng ký; báo feature 005 |
| Đã lên lịch | Hủy buổi | Đã hủy | Trưởng tầng (lý do); Hệ thống (đổi mẫu FR-009, ngừng hoạt động FR-004, khoanh vùng FR-021) | Chưa có điểm danh (FR-013) | Hủy mọi đăng ký; báo feature 005; báo người phụ trách và người được đăng ký qua người đăng ký |
| Đã lên lịch | Hoàn tất điểm danh | Đã điểm danh | Trưởng tầng, Nhân viên chăm sóc (FR-027) | Từ thời điểm bắt đầu; mọi đăng ký còn hiệu lực có kết quả (FR-029) | Chi phí nháp cho lượt có mặt ở hoạt động có thu phí (FR-031) |
| Đã hủy | — | — | — | Trạng thái cuối | — |
| Đã điểm danh | — | — | — | Trạng thái cuối; sai sót xử lý bằng đính chính điểm danh (FR-031) | — |

Chuyến đi có bảng trạng thái riêng ở mục H.

#### D. Đăng ký tham gia

- **FR-015**: Lệnh "Đăng ký" MUST bị chặn khi có ít nhất một điều kiện sau tại thời điểm lưu: (a) số đăng ký còn hiệu lực của buổi đã bằng số lượng tối đa; (b) người cao tuổi có chỉ định hạn chế hoạt động Hiệu lực bao trùm buổi (FR-024); (c) người cao tuổi có giường (hoặc khu nghỉ bán trú) thuộc vùng Đang khoanh vùng, hoặc địa điểm của buổi thuộc vùng Đang khoanh vùng (feature 007 FR-065); (d) người cao tuổi ở trạng thái Đang tiếp nhận hoặc trạng thái cuối; (e) buổi không ở Đã lên lịch hoặc đã bắt đầu (trừ thêm tại lúc điểm danh, FR-028); (f) các điều kiện của FR-016. *(Nguồn: BR-M04-15, BR-M05-11, 5.5)*
- **FR-016**: Đăng ký MUST bị chặn khi người cao tuổi đã có đăng ký còn hiệu lực ở buổi khác chồng thời gian, hoặc là người bán trú mà buổi nằm ngoài khung có mặt theo lịch của ngày đó. *(Suy ra từ 8.2, 3.3)*
- **FR-017**: Người thực hiện đăng ký, hủy đăng ký MUST là Trưởng tầng hoặc Nhân viên chăm sóc, với người cao tuổi trong phạm vi dữ liệu của mình (feature 002). Hủy đăng ký MUST có lý do (ví dụ người cao tuổi không muốn tham gia) và chỉ trước thời điểm bắt đầu. *(Nguồn: 4.4, UC-29; feature 000 lý do bắt buộc)*
- **FR-018**: Hệ thống MUST hiển thị cảnh báo, không chặn, khi người được đăng ký: có cờ nguy cơ ngã hoặc đi lạc (feature 001); có mức chăm sóc, loại hình lưu trú hoặc tầng ngoài đối tượng của hoạt động; mang dấu "nghi nhiễm" (feature 007 FR-060); có cảnh báo hoặc sự cố mức Trung bình trở lên đang mở. Người đăng ký MUST xác nhận; đăng ký lưu dấu "đã xác nhận cảnh báo" và danh sách cảnh báo đã hiển thị. Thông tin sức khỏe chi tiết chỉ hiển thị cho người có quyền xem (19.3). *(Suy ra từ 8.8 "cờ điều kiện sức khỏe", BR-M01-09)*
- **FR-019**: Mỗi đăng ký MUST lưu: buổi, người cao tuổi, nguồn (thường xuyên / thêm tay / thêm lúc điểm danh), người đăng ký, thời điểm, trạng thái (Đã đăng ký / Đã hủy / Đã điểm danh), lý do hủy, cảnh báo đã xác nhận. Đăng ký chỉ đổi trạng thái qua lệnh Hủy đăng ký, hủy tự động (FR-021, FR-022, FR-025) hoặc điểm danh. *(Nguồn: 1.5 nhóm 2)*
- **FR-020**: Hệ thống MUST gợi ý người tham gia cho một buổi còn chỗ, gồm người cao tuổi trong phạm vi người xem, đạt các điều kiện chặn của FR-015, xếp hạng theo: sở thích khớp nhóm sở thích của loại hoạt động (FR-051); mức chăm sóc thuộc đối tượng; số ngày tính kể từ lần gần nhất có mặt ở hoạt động nhóm (nhiều hơn xếp trước). Gợi ý chỉ để chọn; đăng ký vẫn qua FR-015. *(Nguồn: 8.8 "Hệ thống gợi ý người tham gia", 8.10)*
- **FR-021**: Khi một vùng chuyển Đang khoanh vùng (feature 007), trong cùng một lần hệ thống MUST: hủy mọi buổi trong viện chưa có điểm danh có địa điểm trong vùng; hủy mọi đăng ký chưa diễn ra của người cao tuổi có giường trong vùng; lý do "khoanh vùng"; báo người phụ trách buổi và trưởng tầng. Khi vùng được gỡ, buổi và đăng ký đã hủy MUST NOT tự khôi phục. Chuyến đi đang diễn ra không bị ảnh hưởng (Edge Cases). *(Suy ra từ BR-M05-11, BR-M05-12; theo cách xử lý lượt thăm của Q-120)*
- **FR-022**: Khi người cao tuổi chuyển trạng thái cuối, mọi đăng ký chưa diễn ra MUST chuyển Đã hủy với lý do trạng thái cuối, và người đó MUST bị gỡ khỏi mọi danh sách người tham gia thường xuyên. *(Suy ra từ 5.6 "Hủy mọi lịch tương lai")*

#### E. Chỉ định hạn chế hoạt động

- **FR-023**: Bác sĩ MUST gắn được chỉ định hạn chế hoạt động (nhóm 2) cho người cao tuổi gồm: phạm vi, ngày giờ bắt đầu, ngày giờ kết thúc (hoặc tới khi gỡ), lý do, bác sĩ gắn. Phạm vi MUST chọn được ít nhất: mọi hoạt động; mọi hoạt động nhóm; hoạt động ngoài viện; theo loại hoạt động cụ thể. Gỡ hoặc rút ngắn chỉ định MUST là lệnh có lý do của Bác sĩ; chỉ định không bị sửa hay xóa. Chỉ định hạn chế là cờ "không đủ điều kiện" của BR-M04-15 và là "chỉ định hạn chế" của BR-M04-22. *(Nguồn: BR-M04-15, BR-M04-22, 2.3 dòng Bác sĩ; Clarification 2026-09-27, Q-162)*
- **FR-023a**: Điều dưỡng MUST gắn được **chỉ định tạm** cho người cao tuổi trong phạm vi dữ liệu của mình, với cùng các trường và phạm vi như FR-023; thời điểm kết thúc của chỉ định tạm MUST NOT muộn hơn thời điểm gắn cộng CFG-M04-14 (đề xuất, mặc định \[24 giờ\]). Chỉ định tạm có tác động chặn và hủy đăng ký như chỉ định chính thức (FR-015 (b), FR-025, FR-037) và được tính là chỉ định hạn chế ở FR-054. *(Clarification 2026-09-27, Q-162)*
- **FR-023b**: Bác sĩ MUST "Xác nhận" được chỉ định tạm (thành chính thức, MAY đổi phạm vi và thời gian, bắt buộc lý do khi đổi) hoặc "Gỡ" chỉ định tạm kèm lý do. Khi chỉ định tạm được gắn, bác sĩ trực (2.4) MUST được báo. Tới thời điểm kết thúc của chỉ định tạm mà chưa được xác nhận, chỉ định MUST tự chuyển Hết hiệu lực với lý do "chỉ định tạm không được xác nhận", và Điều dưỡng đã gắn, trưởng tầng MUST được báo. Đăng ký đã bị hủy do chỉ định tạm MUST NOT tự khôi phục khi chỉ định tạm bị gỡ hoặc hết hiệu lực. *(Clarification 2026-09-27, Q-162)*
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
- **FR-024**: Một chỉ định bao trùm một buổi khi khoảng hiệu lực giao thời gian của buổi và phạm vi khớp: "mọi hoạt động" khớp mọi buổi; "mọi hoạt động nhóm" khớp buổi của loại có dấu hoạt động nhóm; "hoạt động ngoài viện" khớp chuyến đi; "theo loại" khớp buổi của loại đó. Người không có quyền xem sức khỏe (19.3) chỉ thấy phạm vi và thời gian của chỉ định, không thấy lý do. *(Suy ra từ BR-M04-15, 19.3)*
- **FR-025**: Khi chỉ định được gắn hoặc mở rộng, hệ thống MUST hủy các đăng ký chưa diễn ra bị chỉ định bao trùm với lý do "chỉ định hạn chế" và báo trưởng tầng, người phụ trách buổi. Điểm danh đã ghi MUST NOT bị ảnh hưởng. Với chuyến đi đã rời viện, hệ thống MUST báo trưởng đoàn và trưởng tầng, không tự chuyển trạng thái người cao tuổi. *(Suy ra từ BR-M04-15)*
- **FR-026**: Hồ sơ người cao tuổi MUST hiển thị chỉ định hạn chế Hiệu lực và lịch sử chỉ định cho người có quyền xem hồ sơ trong phạm vi. *(Nguồn: 1.3, 1.5 nhóm 2)*

#### F. Điểm danh buổi trong viện

- **FR-027**: Điểm danh MUST do Trưởng tầng hoặc Nhân viên chăm sóc thực hiện: người phụ trách của buổi, hoặc người có người cao tuổi được điểm danh trong phạm vi dữ liệu (feature 002). *(Nguồn: 4.4, UC-29)*
- **FR-028**: Điểm danh MUST ghi được từ thời điểm bắt đầu của buổi. Với mỗi người, kết quả là Có mặt hoặc Vắng kèm lý do (từ chối, sức khỏe, đang vắng mặt, không đến (bán trú), khác). Người đang Tạm vắng, Điều trị tại bệnh viện, Hoạt động bên ngoài, hoặc bán trú chưa điểm danh đến, MUST được hiển thị sẵn kết quả "Vắng – đang vắng mặt" / "Vắng – không đến". Người chưa đăng ký MUST thêm được lúc điểm danh nếu đạt FR-015 (a) → (d), với nguồn "thêm lúc điểm danh". *(Nguồn: 8.8 "người tham gia; điểm danh")*
- **FR-029**: Với người Có mặt, điểm danh MUST ghi mức độ tham gia (tích cực / bình thường / thụ động / bỏ giữa chừng) và MAY ghi mức giao tiếp (chủ động / khi được hỏi / ít giao tiếp), tình trạng sau hoạt động và ghi chú. "Hoàn tất điểm danh" MUST chỉ được thực hiện khi mọi đăng ký còn hiệu lực đã có kết quả. *(Nguồn: 8.8 "kết quả", 8.10 "giao tiếp; mức độ tham gia"; ERD DIEM_DANH)*
- **FR-030**: Điểm danh ghi sau thời điểm kết thúc của buổi quá CFG-M04-06 (mặc định \[2 giờ\]) MUST gắn nhãn "ghi nhận muộn"; bản ghi ngoại tuyến so theo thời điểm ghi trên thiết bị. Tới thời điểm kết thúc cộng CFG-M04-06 mà buổi chưa Hoàn tất điểm danh, người phụ trách và trưởng tầng MUST được nhắc một lần. *(Suy ra từ BR-M04-12, CFG-M04-06, DBR-25)*
- **FR-031**: Điểm danh là nhóm 3: MUST NOT sửa, xóa; sai sót xử lý bằng đính chính theo feature 000 (bản ghi gắn tầng của buổi; buổi toàn viện gắn tầng của người cao tuổi). Khi đính chính làm một lượt Có mặt ở hoạt động có thu phí thành Vắng hoặc Hủy ghi nhận, feature 010 MUST nhận sự kiện hủy lượt điểm danh; đính chính thêm lượt Có mặt MUST tạo lượt mới cho feature 010. *(Nguồn: 1.5 nhóm 3, BR-M04-14 tương tự, DBR-15, DBR-23)*
- **FR-032**: Mỗi lượt điểm danh Có mặt ở buổi của hoạt động có thu phí MUST được cung cấp cho feature 010 làm bản ghi nguồn (một lượt), để tạo chi phí ở trạng thái Nháp với dịch vụ của hoạt động; lượt Vắng MUST NOT tạo chi phí. Với chuyến đi, lượt có mặt là điểm danh rời viện (FR-041). *(Nguồn: BR-M04-18, BR-M11-01, DBR-15; feature 010 bảng nguồn)*
- **FR-033**: Điểm danh Có mặt MUST được cung cấp cho feature 007 để lập danh sách tiếp xúc (người cùng tham gia một buổi trong CFG-M05-07) và cho feature 012 (số hoạt động đã tham gia trong bản tin, loại thông tin "chung"). *(Nguồn: BR-M05-10, feature 012 FR-052)*

#### G. Hoạt động ngoài viện – chuẩn bị

- **FR-034**: Chuyến đi MUST có thêm: điểm đến, thời điểm rời dự kiến, thời điểm về dự kiến, phương tiện (mô tả), trưởng đoàn, danh sách người đi cùng. Thời điểm về dự kiến MUST được cung cấp cho feature 006 (khoảng đi, feature 006 FR-048) và 011 (tính suất, Q-144) ngay khi chuyến được lên lịch, và mỗi lần thay đổi. *(Nguồn: 8.9; feature 006 FR-048, feature 011 FR-034)*
- **FR-035**: Trưởng tầng MUST phân công trưởng đoàn và người đi cùng trước khi điểm danh rời viện; không có trưởng đoàn thì điểm danh rời viện MUST bị chặn. Trưởng đoàn MUST có vai trò Trưởng tầng hoặc Nhân viên chăm sóc; người đi cùng MAY có bất kỳ vai trò nhân viên nào, kể cả Điều dưỡng. Trưởng đoàn và người đi cùng MUST có tài khoản Hoạt động và có tên trong ca chồng thời gian chuyến đi (feature 008); không đạt thì phân công bị chặn. Việc phân công MUST cho trưởng đoàn và người đi cùng phạm vi dữ liệu là người tham gia chuyến trong thời gian chuyến (feature 002 FR-031); người đi cùng không có vai trò Trưởng tầng hoặc Nhân viên chăm sóc chỉ được thực hiện "Báo thiếu người" (FR-046) trong các lệnh của chuyến. Khi giờ về được gia hạn vượt ca của một người đi cùng, trưởng tầng MUST được báo để phân công bổ sung; phân công hiện có không bị hủy. *(Nguồn: 8.9 "phân công người đi cùng", 2.4 "Trưởng đoàn", 4.4; Clarification 2026-09-27, Q-161)*
- **FR-036**: Trước khi điểm danh rời viện, mỗi người đăng ký MUST có kết quả đánh giá khả năng tham gia: "Đạt" hoặc "Không đạt" kèm lý do. Khi đánh giá, hệ thống MUST hiển thị: chỉ định hạn chế, cờ nguy cơ, dấu nghi nhiễm, cảnh báo và sự cố đang mở, chỉ số vượt ngưỡng trong 24 giờ gần nhất (chỉ với người có quyền xem sức khỏe), liều thuốc trong khoảng đi. Người có cờ nguy cơ đi lạc được đánh giá Đạt MUST có một người đi cùng được chỉ định kèm riêng. Người Không đạt chuyển "Không đi", đăng ký kết thúc với lý do. Người thực hiện đánh giá MUST là Trưởng tầng (tầng của người cao tuổi, hoặc tầng tổ chức với chuyến toàn viện); trưởng đoàn không có vai trò Trưởng tầng không được đánh giá. *(Nguồn: 8.9 "đánh giá khả năng tham gia"; Clarification 2026-09-27, Q-161)*
- **FR-037**: Chỉ định hạn chế phạm vi "hoạt động ngoài viện" hoặc "mọi hoạt động" gắn sau khi người đã được đánh giá Đạt MUST đưa người đó về "Không đi" nếu chưa rời viện, và báo trưởng đoàn, trưởng tầng. *(Suy ra từ BR-M04-15)*
- **FR-038**: Với mỗi người được đánh giá Đạt, hệ thống MUST lấy từ feature 006 danh sách liều trong khoảng đi dự kiến. Điểm danh rời viện của người đó MUST bị chặn khi còn liều trong khoảng đi mà chưa có lần "Giao thuốc mang theo" (feature 006 FR-048a) và chưa có quyết định "không mang thuốc" của Điều dưỡng kèm lý do (liều khi đó theo feature 006). Danh sách thuốc cần mang MUST hiển thị cho điều dưỡng phụ trách từ khi người được đánh giá Đạt. *(Nguồn: 8.9 "kiểm tra thuốc cần mang", BR-M04-16; feature 006 User Story 2 kịch bản 3)*
- **FR-039**: Danh sách chuyến MUST hiển thị cho trưởng đoàn, người đi cùng và trưởng tầng: người tham gia và trạng thái của từng người, người đi cùng được chỉ định kèm riêng, số liên lạc người liên hệ chính (chỉ với trưởng đoàn và trưởng tầng), thuốc mang theo đã giao (theo quyền xem thuốc của feature 006), cờ nguy cơ, và thẻ thông tin khẩn cấp theo quyền của feature 007. *(Nguồn: 8.9 "Trong chuyến đi: theo dõi danh sách", BR-M05-13)*

#### H. Rời viện, trong chuyến và trở về

- **FR-040**: Lệnh "Điểm danh rời viện" MUST do trưởng đoàn thực hiện (trưởng tầng thay được khi trưởng đoàn không có mặt, kèm lý do), cho từng người hoặc nhiều người trong một lần, không sớm hơn thời điểm rời dự kiến trừ CFG-M04-13 (đề xuất, mặc định \[1 giờ\]). Người được điểm danh MUST đã được đánh giá Đạt và đạt FR-038. *(Nguồn: BR-M04-16, UC-30)*
- **FR-041**: Khi một người được điểm danh rời viện, trong cùng một lần hệ thống MUST: (a) ghi bản ghi điểm danh rời viện (nhóm 3) với thời điểm thực tế; (b) yêu cầu feature 001 chuyển người đó sang Hoạt động bên ngoài, người thực hiện là người điểm danh, căn cứ là chuyến đi; (c) cung cấp cho feature 006 thời điểm rời thực tế và giờ về dự kiến để liều trong khoảng chuyển Mang theo (BR-M07-03); (d) cung cấp cho feature 005 để hủy công việc Chưa đến hạn trong khoảng đi (feature 005 FR-020); (e) cung cấp cho feature 010 lượt có mặt nếu có thu phí (FR-032); (f) cung cấp cho feature 011 thời điểm rời thực tế. Người đăng ký không được điểm danh rời viện khi chuyến bắt đầu (người Không đi, hoặc không có mặt lúc xuất phát) MUST có kết quả "Vắng" kèm lý do. *(Nguồn: BR-M04-16, 5.6 dòng "Đang lưu trú → Hoạt động bên ngoài")*
- **FR-042**: Chuyến có ít nhất một người đã rời viện MUST chuyển Đang đi. Người đăng ký chưa rời viện MAY được điểm danh rời viện muộn (đi sau) tới thời điểm về dự kiến. *(Suy ra từ 8.9)*
- **FR-043**: Trưởng đoàn hoặc trưởng tầng MUST "Gia hạn giờ về" được khi chuyến Đang đi, kèm lý do; giờ về dự kiến mới được lưu cùng lịch sử, mốc quá giờ tính lại, feature 006 và 011 nhận giờ mới. *(Suy ra từ BR-M04-17; theo cách của feature 004 gia hạn dự kiến trở lại)*
- **FR-044**: Lệnh "Điểm danh về" MUST do trưởng đoàn hoặc trưởng tầng thực hiện, cho từng người hoặc nhiều người, khi người đó đã có mặt tại viện. Khi một người được điểm danh về, trong cùng một lần hệ thống MUST: ghi bản ghi điểm danh về với thời điểm thực tế; yêu cầu feature 001 chuyển người đó về Đang lưu trú; cung cấp cho feature 005, 006, 011 thời điểm về thực tế (sinh lại công việc, tính lại liều Mang theo chưa tới giờ theo Q-60, suất ăn). Người về sớm một mình được điểm danh về riêng; chuyến vẫn Đang đi. *(Nguồn: 8.9 "Khi trở về: điểm danh; cập nhật trạng thái", BR-M04-16)*
- **FR-045**: Người tham gia chuyển Điều trị tại bệnh viện (chuyển viện, feature 004/007) hoặc Qua đời (feature 004) trong khi chuyến Đang đi MUST được đánh dấu "Rời đoàn" kèm trạng thái mới, không cần điểm danh về và không bị tính là thiếu người. Hệ thống MUST NOT có lệnh cho người tham gia rời đoàn để về với người thân, vì 5.5 không có chuyển Hoạt động bên ngoài → Tạm vắng; người đó phải được điểm danh về trước. *(Nguồn: 5.5)*
- **FR-046**: Trong khi chuyến Đang đi, trưởng đoàn hoặc bất kỳ người đi cùng nào (kể cả Điều dưỡng, vì ghi sự cố là quyền của mọi nhân viên theo 4.4 dòng "Sự cố, khẩn cấp") MUST "Báo thiếu người" được cho một người đã rời viện, kèm địa điểm và thời điểm thấy lần cuối, mô tả; hệ thống MUST ngay trong lần đó chuyển người đó sang "Thiếu khi về" và yêu cầu feature 007 tạo sự cố khẩn cấp theo FR-048. *(Suy ra từ BR-M04-17, BR-M05-06)*
- **FR-047**: Lệnh "Kết thúc điểm danh về" MUST do trưởng đoàn hoặc trưởng tầng thực hiện; mọi người đã rời viện chưa được điểm danh về và không "Rời đoàn" MUST chuyển "Thiếu khi về", và với mỗi người chưa có sự cố thiếu người từ FR-046, hệ thống MUST yêu cầu feature 007 tạo sự cố khẩn cấp theo FR-048. Trạng thái người cao tuổi MUST giữ Hoạt động bên ngoài cho tới lệnh phù hợp (điểm danh về muộn, chuyển viện, ghi nhận qua đời) (5.5). Người "Thiếu khi về" MUST được điểm danh về muộn khi đã có mặt tại viện; feature 007 nhận diễn biến "đã tìm thấy, đã trở về" vào sự cố; sự cố không tự đóng. *(Nguồn: BR-M04-17, 5.5 "Thay đổi so với bản trước")*
- **FR-048**: Sự cố thiếu người MUST có: loại "đi lạc hoặc không trở về", mức Khẩn cấp, nguồn "hoạt động ngoài viện", người cao tuổi, chuyến đi, trưởng đoàn, địa điểm và thời điểm thấy lần cuối, người phát hiện (người thực hiện lệnh). Mỗi người trong một chuyến MUST có tối đa một sự cố thiếu người đang mở. Thông báo khẩn cấp theo feature 007 (BR-M05-06). *(Nguồn: BR-M04-17, feature 007 FR-040, FR-042)*
- **FR-049**: Tới thời điểm về dự kiến cộng CFG-M04-07 (mặc định \[30 phút\]) mà chuyến chưa Kết thúc điểm danh về, hệ thống MUST báo trưởng đoàn và trưởng tầng, một lần cho mỗi mốc giờ về dự kiến; chuyến gắn dấu "quá giờ về"; trạng thái người tham gia không đổi. *(Nguồn: BR-M04-17, CFG-M04-07)*
- **FR-050**: Chuyến MUST chuyển Đã về khi Kết thúc điểm danh về được thực hiện; chuyến ở Đã về vẫn nhận điểm danh về muộn cho người "Thiếu khi về". *(Suy ra từ 8.9)*

**Bảng trạng thái chuyến đi** *(8.9, BR-M04-16, BR-M04-17)*:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái đích | Ai được thực hiện | Điều kiện (chặn nếu không đạt) | Tác động tự động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Sinh từ mẫu / Tạo buổi lẻ | Đã lên lịch | Hệ thống / Trưởng tầng | Như buổi trong viện; có điểm đến, giờ rời, giờ về dự kiến (FR-034) | Cung cấp khoảng đi cho feature 006, 011 |
| Đã lên lịch | Dời buổi | Đã lên lịch | Trưởng tầng | Chưa có người rời viện; có lý do | Như buổi trong viện; báo feature 006, 011 |
| Đã lên lịch | Hủy buổi | Đã hủy | Trưởng tầng (lý do); Hệ thống (đổi mẫu, ngừng hoạt động) | Chưa có người rời viện | Hủy đăng ký; báo feature 005, 006, 011 |
| Đã lên lịch | Điểm danh rời viện (người đầu tiên) | Đang đi | Trưởng đoàn; Trưởng tầng thay (lý do) | Có trưởng đoàn (FR-035); người đi đã Đạt (FR-036), đạt FR-038 | FR-041 cho từng người |
| Đang đi | Điểm danh rời viện (đi sau) | Đang đi | Như trên | Như trên; trước giờ về dự kiến | FR-041 |
| Đang đi | Gia hạn giờ về | Đang đi | Trưởng đoàn, Trưởng tầng | Có lý do | Tính lại mốc quá giờ; báo feature 006, 011 |
| Đang đi | Tới giờ về dự kiến + CFG-M04-07 | Đang đi (dấu "quá giờ về") | Hệ thống | Chưa Kết thúc điểm danh về | Báo trưởng đoàn, trưởng tầng (FR-049) |
| Đang đi | Điểm danh về (từng phần) | Đang đi | Trưởng đoàn, Trưởng tầng | Người đã rời viện, có mặt tại viện | FR-044 |
| Đang đi | Báo thiếu người | Đang đi | Trưởng đoàn, mọi người đi cùng (Q-161) | Người đã rời viện, chưa về | Sự cố khẩn cấp (FR-046, FR-048) |
| Đang đi | Kết thúc điểm danh về | Đã về | Trưởng đoàn, Trưởng tầng | — | Người chưa về, không rời đoàn → Thiếu khi về, sự cố khẩn cấp (FR-047) |
| Đã về | Điểm danh về muộn | Đã về | Trưởng đoàn, Trưởng tầng | Người ở "Thiếu khi về", có mặt tại viện | FR-044; diễn biến vào sự cố (FR-047) |
| Đã hủy | — | — | — | Trạng thái cuối | — |

**Bảng trạng thái người tham gia chuyến đi**:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái đích | Ai được thực hiện | Điều kiện | Tác động tự động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Đăng ký | Đã đăng ký | Trưởng tầng, Nhân viên chăm sóc | FR-015, FR-016 | — |
| Đã đăng ký | Đánh giá khả năng: Đạt | Được đi | Trưởng tầng (Q-161) | FR-036 | Hiển thị thuốc cần mang cho điều dưỡng phụ trách (FR-038) |
| Đã đăng ký; Được đi | Đánh giá: Không đạt; chỉ định hạn chế mới (FR-037); hủy đăng ký | Không đi | Trưởng tầng (Q-161); Hệ thống; Trưởng tầng, Nhân viên chăm sóc | Có lý do; chưa rời viện | Kết thúc đăng ký với lý do |
| Được đi | Điểm danh rời viện | Đã rời viện | Trưởng đoàn; Trưởng tầng thay | FR-038, FR-040 | FR-041 |
| Đã rời viện | Điểm danh về | Đã về | Trưởng đoàn, Trưởng tầng | Có mặt tại viện | FR-044 |
| Đã rời viện | Báo thiếu người; Kết thúc điểm danh về khi chưa về | Thiếu khi về | Trưởng đoàn, người đi cùng; Trưởng tầng | — | Sự cố khẩn cấp (FR-048) |
| Đã rời viện; Thiếu khi về | Chuyển viện / Ghi nhận qua đời (feature 004, 007) | Rời đoàn | Hệ thống, theo lệnh của feature nguồn | — | Không tính thiếu người (FR-045) |
| Thiếu khi về | Điểm danh về muộn | Đã về | Trưởng đoàn, Trưởng tầng | Có mặt tại viện | FR-044; diễn biến vào sự cố (FR-047) |
| Không đi; Đã về; Rời đoàn | — | — | — | Trạng thái cuối | — |

#### I. Sở thích, theo dõi tinh thần và nguy cơ cô lập

- **FR-051**: Hệ thống MUST quản lý danh mục nhóm sở thích (nhóm 1, ví dụ âm nhạc, cờ, làm vườn, tôn giáo, đọc sách, thể dục) liên kết với loại hoạt động, và danh sách sở thích của từng người cao tuổi gồm: nhóm sở thích, mô tả, mức ưa thích (thích / không thích), nguồn (người cao tuổi tự nói, người thân cho biết, nhân viên quan sát), người ghi, thời điểm. Thêm, bỏ sở thích MUST lưu lịch sử người thực hiện, thời điểm; bỏ sở thích MUST có lý do. Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc ghi được sở thích cho người cao tuổi trong phạm vi (xem Assumptions). *(Nguồn: 8.10 "sở thích", 1.3)*
- **FR-052**: Hoạt động mà người cao tuổi có sở thích "không thích" khớp nhóm của loại hoạt động MUST được hiển thị cảnh báo khi đăng ký và bị loại khỏi gợi ý (FR-020, FR-057). *(Suy ra từ 8.10)*
- **FR-053**: Hệ thống MUST cho Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc (trong phạm vi) xem hồ sơ tinh thần của một người cao tuổi theo khoảng thời gian, tổng hợp: số buổi hoạt động nhóm có mặt theo tuần; mức độ tham gia và giao tiếp theo buổi (FR-029); tâm trạng và hành vi bất thường theo ngày (feature 005, chỉ đọc); sở thích; các cảnh báo cô lập, tâm trạng đã có và trạng thái của chúng (feature 007). Hồ sơ tinh thần chỉ để xem; dữ liệu MUST được ghi ở nguồn. *(Nguồn: 8.10 "Ghi nhận: tâm trạng; giao tiếp; mức độ tham gia; hành vi bất thường; sở thích")*
- **FR-054**: Mỗi ngày, Bộ lập lịch MUST xét cho mỗi người cao tuổi ở Đang lưu trú (bán trú: có hợp đồng hiệu lực) chuỗi ngày tính liên tiếp gần nhất không có lượt điểm danh Có mặt ở hoạt động nhóm (kể cả chuyến đi). Ngày không phải ngày tính (vắng mặt một phần hay trọn ngày, có chỉ định hạn chế bao trùm hoạt động nhóm, bán trú không đến) MUST được bỏ qua, không làm đứt chuỗi. Chuỗi bắt đầu không sớm hơn ngày Hoàn tất tiếp nhận. *(Nguồn: BR-M04-22 "không tính thời gian vắng mặt hoặc có chỉ định hạn chế", CFG-M04-10)*
- **FR-055**: Khi chuỗi ở FR-054 đạt CFG-M04-10 (mặc định \[7 ngày\]) ngày tính, hệ thống MUST yêu cầu feature 007 tạo cảnh báo mức Nhẹ loại "nguy cơ cô lập" cho người cao tuổi, người nhận là trưởng tầng của tầng người đó, kèm danh sách hoạt động gợi ý (FR-057). *(Nguồn: BR-M04-22, UC-29)*
- **FR-056**: Nếu đã có cảnh báo "nguy cơ cô lập" đang mở cho người đó, cảnh báo mới MUST được gộp vào theo DBR-18 (feature 007). Khi người đó có lượt Có mặt ở hoạt động nhóm, chuỗi MUST đếm lại từ đầu và feature 007 MUST nhận diễn biến "đã tham gia hoạt động nhóm" cho cảnh báo đang mở; cảnh báo không tự đóng. *(Nguồn: BR-M05-02, DBR-18)*
- **FR-057**: Danh sách hoạt động gợi ý cho một người MUST gồm các buổi trong CFG-M04-09 tới, còn chỗ, người đó đạt các điều kiện chặn của FR-015, xếp theo mức khớp sở thích "thích", loại trừ sở thích "không thích"; nếu không có buổi khớp sở thích thì gồm các buổi hoạt động nhóm đạt điều kiện, có ghi chú "không khớp sở thích". Danh sách này MUST được cung cấp cho cảnh báo cô lập (FR-055) và cho cảnh báo tâm trạng tiêu cực kéo dài khi feature 005 yêu cầu (BR-M04-11, feature 005 FR-042). *(Nguồn: BR-M04-11, BR-M04-22, 8.10 "đề xuất hoạt động phù hợp")*
- **FR-058**: Danh sách người cao tuổi có cảnh báo nguy cơ cô lập đang mở và số ngày tính của chuỗi MUST được cung cấp cho báo cáo chăm sóc (18.2). *(Nguồn: 18.2 "(Bổ sung)")*

#### J. Kiểm tra chất lượng ngẫu nhiên

- **FR-059**: Với mỗi ca có phạm vi tầng/khu vực (feature 008), hệ thống MUST lập một danh sách kiểm tra chất lượng giao trưởng tầng được giao của tầng. Ngay khi một công việc đủ điều kiện (FR-060) được hoàn thành trong ca, hệ thống MUST xét chọn nó ngẫu nhiên với xác suất bằng CFG-M04-11 (mặc định \[5%\]); công việc được chọn vào danh sách ngay. Trưởng tầng MUST được báo khi danh sách của ca có mục đầu tiên; các mục sau chỉ hiện trong danh sách. Người thực hiện công việc MUST NOT được báo công việc của mình được chọn. *(Nguồn: BR-M04-23, CFG-M04-11, UC-31; Clarification 2026-09-27, Q-163)*
- **FR-059a**: Tại mốc CFG-M09-04 (mặc định \[30 phút\]) trước khi kết ca, nếu số mục đã chọn nhỏ hơn CFG-M04-11 nhân số công việc đủ điều kiện đã hoàn thành trong ca (làm tròn lên, tối thiểu 1 khi có công việc đủ điều kiện), hệ thống MUST chọn ngẫu nhiên bổ sung trong số công việc đủ điều kiện chưa được chọn cho đủ số đó. Công việc hoàn thành sau mốc này vẫn được xét chọn theo FR-059. *(Clarification 2026-09-27, Q-163; mốc dùng lại CFG-M09-04 là thời điểm lập bản nháp bàn giao)*
- **FR-060**: Công việc đủ điều kiện là công việc gắn tầng/khu vực của ca, ở Hoàn thành hoặc Hoàn thành trễ, có thời điểm hoàn thành trong ca, gồm công việc chăm sóc (feature 005, kể cả "Ghi nhận phát sinh" Q-36) và công việc vệ sinh (feature 003, 7.5); không gồm liều thuốc (Module 07), công việc do chính trưởng tầng được giao thực hiện, công việc đã có bản đính chính "Hủy ghi nhận", và công việc làm lại do kiểm tra chất lượng sinh ra trong cùng ca. Mỗi công việc MUST được chọn tối đa một lần. Phép chọn MUST ngẫu nhiên, mỗi công việc đủ điều kiện có cùng khả năng được chọn bất kể loại, người thực hiện hay thời điểm hoàn thành, và không để nhân viên biết trước công việc nào sẽ được chọn. *(Nguồn: BR-M04-23 "(Bổ sung, vệ sinh)"; 8.3 "(Làm rõ)")*
- **FR-061**: Với mỗi công việc trong danh sách, trưởng tầng MUST ghi kết quả Đạt hoặc Không đạt, kèm ghi chú (bắt buộc với Không đạt) và MAY kèm ảnh. Kết quả kiểm tra là nhóm 3: không sửa, không xóa; sai sót xử lý bằng đính chính theo feature 000. Công việc gốc và kết quả ghi nhận của nó MUST NOT bị sửa. *(Nguồn: BR-M04-23 "Bản ghi gốc của công việc không bị sửa", 1.5 nhóm 3)*
- **FR-062**: Trưởng tầng MUST ghi được "Không kiểm tra được" kèm lý do (ví dụ người cao tuổi đã chuyển viện, công việc không còn dấu vết kiểm tra được). Mục có công việc bị "Hủy ghi nhận" sau khi được chọn MUST tự đóng với lý do đó. Hai kết quả này MUST NOT tính vào tỷ lệ Đạt. *(Suy ra từ BR-M04-23)*
- **FR-063**: Hạn kiểm tra của mọi mục là giờ kết thúc của ca có danh sách. Mục chưa có kết quả khi hết ca MUST tự đóng với kết quả "Quá hạn kiểm tra", được tính riêng trong báo cáo, và Quản lý viện MUST được báo một lần cho mỗi danh sách có mục quá hạn. Trưởng tầng được giao ghi được kết quả trong thời gian của ca, kể cả khi không có tên trong ca đó. *(Suy ra từ BR-M04-23, 18.2; Clarification 2026-09-27, Q-163)*
- **FR-064**: Khi kết quả Không đạt được lưu, trong cùng một lần hệ thống MUST yêu cầu feature 005 (công việc chăm sóc) hoặc feature 003 (công việc vệ sinh) sinh một công việc làm lại: cùng loại, cùng người cao tuổi hoặc phòng/khu vực, mức quan trọng bằng công việc gốc, thời điểm dự kiến trong chính ca của danh sách (kết quả luôn được ghi trong ca đó, FR-063), giao cho người thực hiện công việc gốc nếu người đó còn tên và không vắng trong ca, nếu không thì thành công việc chung của tầng; khi ghi kết quả sát giờ kết ca, công việc làm lại chưa đóng đi vào bản nháp bàn giao theo feature 005 (BR-M04-07); công việc làm lại trỏ về công việc gốc và kết quả kiểm tra. Người thực hiện công việc gốc MUST được báo. *(Nguồn: BR-M04-23 "tạo công việc làm lại cho ca hiện tại")*
- **FR-065**: Kết quả kiểm tra MUST được cung cấp cho báo cáo chất lượng (18.2): tỷ lệ Đạt theo nhân viên thực hiện công việc gốc và theo tầng, theo khoảng thời gian; số mục Không kiểm tra được và Quá hạn kiểm tra. Tỷ lệ Đạt = số Đạt ÷ (số Đạt + số Không đạt). *(Nguồn: BR-M04-23, 18.2)*
- **FR-066**: Chỉ Trưởng tầng được giao của tầng (kể cả trưởng tầng tạm có thời hạn, Q-84) MUST ghi được kết quả kiểm tra; Người phụ trách ca không có quyền này. Quản lý viện xem được mọi danh sách và kết quả. *(Nguồn: 4.4 dòng "Kiểm tra chất lượng", 19.3 "(Bổ sung)")*

#### K. Quyền, hiển thị và dữ liệu cung cấp

- **FR-067**: Quyền của spec MUST theo 4.4: Quản lý viện xem mọi hoạt động, buổi, điểm danh, chuyến đi, kết quả kiểm tra; Trưởng tầng thực hiện mọi lệnh của spec trong tầng mình và với hoạt động toàn viện; Nhân viên chăm sóc đăng ký, hủy đăng ký, điểm danh, ghi sở thích trong phạm vi; Bác sĩ gắn, xác nhận, gỡ chỉ định hạn chế (FR-023, FR-023b); Điều dưỡng gắn chỉ định tạm (FR-023a), ghi quyết định "không mang thuốc" (FR-038), ghi sở thích, và khi là người đi cùng thì "Báo thiếu người" (FR-046); trưởng đoàn là Trưởng tầng hoặc Nhân viên chăm sóc, đánh giá khả năng tham gia chỉ do Trưởng tầng (FR-035, FR-036, Q-161); Người thân chỉ xem. Nhiệm vụ Trưởng đoàn MUST NOT tạo thêm quyền ngoài các lệnh của chuyến mình dẫn (feature 002 FR-019). *(Nguồn: 4.4, 2.4; feature 002 FR-019, FR-031)*
- **FR-068**: Trên cổng người thân, người thân có quan hệ Hiệu lực MUST thấy (loại thông tin "chung", feature 012 FR-046): các buổi người cao tuổi đã đăng ký, chuyến đi (điểm đến, giờ rời, giờ về dự kiến), số buổi đã tham gia; MUST NOT thấy chỉ định hạn chế, mức độ tham gia, giao tiếp, tình trạng sau hoạt động, cảnh báo cô lập hay kết quả kiểm tra chất lượng. *(Nguồn: 4.4 cột NT "X"; feature 012 FR-046, FR-052)*
- **FR-069**: Mọi lệnh ở spec này MUST được ghi nhật ký với người thực hiện, thời điểm, lý do (khi bắt buộc), theo feature 000; mọi mốc thời gian theo DBR-25. *(Nguồn: DBR-23, DBR-25, 19.4)*
- **FR-070**: Spec MUST cung cấp cho Module 14: số buổi, số lượt đăng ký, số lượt có mặt và tỷ lệ tham gia theo hoạt động, theo tầng, theo khoảng thời gian; chuyến đi quá giờ về và sự cố thiếu người. *(Nguồn: 18.2 "hoạt động; tỷ lệ tham gia", 18.5)*

#### L. Thông báo

- **FR-071**: Feature này MUST yêu cầu feature 009 gửi đúng các thông báo ở bảng dưới, theo khuôn chung của feature 009 FR-001, FR-043a và nguyên tắc xếp mức FR-043b. Mọi "báo", "nhắc" trong FR và bảng trạng thái của spec này MUST có dòng tương ứng. Quy tắc chung cho mọi dòng: *(Nguồn: 17, BR-M13-01, feature 009)*
  - **Khóa sự kiện** (feature 009 FR-005): (loại sự kiện, buổi) cho sự kiện của buổi; (loại sự kiện, buổi, người cao tuổi) cho sự kiện của một đăng ký hoặc người tham gia; (loại sự kiện, danh sách kiểm tra) cho kiểm tra chất lượng; với nhắc theo mốc thêm mốc thời gian.
  - **Loại thông tin**: mọi thông báo tới nhân viên là "chung", trừ nội dung có lý do chỉ định hạn chế (loại "sức khỏe", chỉ hiện với người có quyền). Thông báo tới người thân chỉ là "chung".
  - **Nội dung rút gọn cho kênh ngoài ứng dụng** (feature 009 FR-019): loại sự kiện, tên hoạt động hoặc chuyến, thời điểm; không nêu lý do sức khỏe.
  - Cảnh báo "nguy cơ cô lập" và sự cố thiếu người được thông báo qua feature 007, không lặp ở bảng này.

| Sự kiện | Người nhận | Mức | Căn cứ |
| --- | --- | --- | --- |
| Người thường xuyên không được đăng ký sẵn khi sinh buổi | Trưởng tầng của hoạt động | Nhẹ | FR-008 |
| Buổi bị hủy hoặc dời; đăng ký bị hủy tự động (đổi mẫu, dời buổi, khoanh vùng, chỉ định hạn chế, trạng thái cuối) | Người phụ trách buổi; người đã đăng ký cho người cao tuổi; Trưởng tầng (với khoanh vùng và chỉ định hạn chế) | Nhẹ | FR-009, FR-012, FR-013, FR-021, FR-022, FR-025 |
| Buổi chưa hoàn tất điểm danh sau thời điểm kết thúc + CFG-M04-06 | Người phụ trách buổi; Trưởng tầng | Nhẹ | FR-030 |
| Người được đánh giá Đạt có liều trong khoảng đi, cần chuẩn bị thuốc mang theo | Điều dưỡng phụ trách người cao tuổi | Trung bình | FR-038 |
| Người đã được đánh giá Đạt bị chỉ định hạn chế mới; chỉ định mới cho người đang đi | Trưởng đoàn; Trưởng tầng | Trung bình | FR-025, FR-037 |
| Điều dưỡng gắn chỉ định tạm, cần bác sĩ xác nhận | Bác sĩ trực | Trung bình | FR-023b |
| Chỉ định tạm hết hiệu lực do không được xác nhận | Điều dưỡng đã gắn; Trưởng tầng | Nhẹ | FR-023b |
| Giờ về được gia hạn vượt ca của người đi cùng | Trưởng tầng | Trung bình | FR-035 |
| Chuyến quá giờ về dự kiến + CFG-M04-07 | Trưởng đoàn; Trưởng tầng | Trung bình | BR-M04-17, FR-049 |
| Người đi trở về vùng đang khoanh vùng | Trưởng đoàn; Trưởng tầng | Trung bình | Edge Cases |
| Danh sách kiểm tra chất lượng của ca có mục đầu tiên | Trưởng tầng được giao của tầng | Nhẹ | FR-059 |
| Kết quả kiểm tra Không đạt, có công việc làm lại | Người thực hiện công việc gốc | Trung bình | FR-064 |
| Danh sách kiểm tra có mục Quá hạn kiểm tra | Quản lý viện | Nhẹ | FR-063 |

### Truy vết quy tắc

| Quy tắc / quyết định | Kịch bản chấp nhận | FR |
| --- | --- | --- |
| 8.8 (hoạt động, quản lý, gợi ý) | US1 kịch bản 1, 5, 6, 7; US2 kịch bản 8 | FR-001 → FR-004, FR-011, FR-020 |
| BR-M04-21, CFG-M04-09 (hoạt động định kỳ) | US1 kịch bản 1 → 4 | FR-005 → FR-010 |
| BR-M04-15 (giới hạn, chỉ định hạn chế, khoanh vùng) | US2 kịch bản 1 → 6, 9, 11, 12; US3 kịch bản 3 | FR-015 → FR-025 |
| BR-M05-11, BR-M05-12 (khoanh vùng) | US2 kịch bản 5, 6; US4 kịch bản 7 | FR-015 (c), FR-021 |
| BR-M04-18, BR-M11-01, DBR-15 (chi phí) | US3 kịch bản 1, 2, 4; US4 kịch bản 5 | FR-031, FR-032 |
| 8.8 (điểm danh), BR-M04-12 (ghi nhận muộn) | US3 kịch bản 1 → 7 | FR-027 → FR-031 |
| 8.9, UC-30 (chuẩn bị chuyến đi) | US4 kịch bản 1 → 3, 6 | FR-034 → FR-039 |
| BR-M04-16, BR-M07-03, 5.6 (rời viện) | US4 kịch bản 4, 5 | FR-040 → FR-042 |
| BR-M04-17, 5.5 (quá giờ, thiếu người) | US5 kịch bản 1 → 8 | FR-043 → FR-050 |
| 8.10, BR-M04-11 (gợi ý), BR-M04-22, CFG-M04-10 | US6 kịch bản 1 → 7 | FR-051 → FR-058 |
| BR-M04-23, CFG-M04-11, UC-31 | US7 kịch bản 1 → 10 | FR-059 → FR-066 |
| Q-161 (trưởng đoàn, người đi cùng, người đánh giá) | US4 kịch bản 2, 8; US5 kịch bản 5 | FR-035, FR-036, FR-046, FR-067 |
| Q-162 (chỉ định hạn chế có phạm vi, chỉ định tạm) | US2 kịch bản 3, 4, 11, 12; US6 kịch bản 5 | FR-023 → FR-025, bảng trạng thái chỉ định |
| Q-163 (chọn rải trong ca, hạn hết ca) | US7 kịch bản 1 → 3, 5 | FR-059, FR-059a, FR-063, FR-064 |
| BR-M05-10 (truy vết tiếp xúc) | — (kiểm thử cùng feature 007) | FR-033 |
| 4.4, 19.3 (quyền) | US1 kịch bản 7; US2 kịch bản 10; US3 kịch bản 7; US7 kịch bản 8 | FR-017, FR-027, FR-066 → FR-068 |
| 1.5 nhóm 3, DBR-23 (không sửa, đính chính, nhật ký) | US3 kịch bản 4; US7 kịch bản 2, 3 | FR-031, FR-061, FR-069 |

### Giao tiếp với feature khác

| Hướng | Feature | Nội dung | Dữ liệu tối thiểu |
| --- | --- | --- | --- |
| Nhận | 000 | Đính chính, nhật ký, lý do bắt buộc, tham số CFG-M04-06, CFG-M04-07, CFG-M04-09, CFG-M04-10, CFG-M04-11, CFG-M04-13, CFG-M04-14 (đề xuất), CFG-M05-07, CFG-M09-04 | — |
| Nhận, Gửi | 001 | Nhận: trạng thái, cờ nguy cơ, mức chăm sóc, loại hình lưu trú, ngày Hoàn tất tiếp nhận, sự kiện trạng thái cuối. Gửi: yêu cầu chuyển Đang lưu trú ↔ Hoạt động bên ngoài khi điểm danh rời/về (feature 001 bảng trạng thái) | Người cao tuổi, trạng thái, người thực hiện, căn cứ (chuyến), thời điểm |
| Nhận | 002 | Phạm vi dữ liệu; tài khoản Hoạt động; phạm vi do nhiệm vụ Trưởng đoàn (feature 002 FR-031) | Nhân viên, người cao tuổi, chuyến |
| Nhận, Gửi | 003 | Nhận: tầng/khu vực, giường, khu nghỉ bán trú của người cao tuổi; công việc vệ sinh Hoàn thành. Gửi: yêu cầu sinh công việc vệ sinh làm lại (FR-064) | Người cao tuổi, tầng, phòng, công việc |
| Nhận | 004 | Lượt vắng; Chuyển viện; Ghi nhận qua đời; danh mục dịch vụ cho hoạt động có thu phí; lịch có mặt bán trú | Người cao tuổi, khoảng vắng, dịch vụ |
| Gửi, Nhận | 005 | Gửi: đăng ký, hủy đăng ký, buổi bị dời/hủy (sinh, hủy công việc "hoạt động"); thời điểm rời/về chuyến đi; danh sách hoạt động gợi ý (FR-057); yêu cầu sinh công việc làm lại (FR-064). Nhận: công việc Hoàn thành cho mẫu kiểm tra; tâm trạng, hành vi bất thường cho hồ sơ tinh thần; yêu cầu gợi ý khi tâm trạng tiêu cực kéo dài; điểm danh đến/về bán trú | Người cao tuổi, buổi, thời điểm, công việc, kết quả |
| Gửi, Nhận | 006 | Gửi: khoảng đi dự kiến, giờ về dự kiến và gia hạn, thời điểm rời/về thực tế (feature 006 FR-048). Nhận: danh sách liều trong khoảng đi, lần giao thuốc mang theo, quyết định không mang thuốc (FR-038) | Người cao tuổi, chuyến, thời điểm, liều, lần giao |
| Gửi, Nhận | 007 | Gửi: yêu cầu tạo sự cố khẩn cấp thiếu người (FR-048), diễn biến "đã tìm thấy, đã trở về"; yêu cầu tạo cảnh báo "nguy cơ cô lập" và diễn biến "đã tham gia" (FR-055, FR-056); điểm danh cho danh sách tiếp xúc (FR-033). Nhận: trạng thái và sự kiện khoanh vùng (feature 007 FR-065); dấu nghi nhiễm; cảnh báo, sự cố đang mở; chuyển viện từ sự cố | Người cao tuổi, chuyến/buổi, loại, mức, địa điểm, thời điểm |
| Nhận, Gửi | 008 | Nhận: ca, trưởng tầng được giao, người trong ca (FR-035, FR-059, FR-064). Gửi: người đang Hoạt động bên ngoài, chuyến quá giờ về vào bản nháp bàn giao (feature 008 FR-039 (e)) | Ca, tầng, nhân viên, người cao tuổi |
| Gửi | 009 | Các thông báo ở bảng FR-071 | Nguồn, mức, nhóm người nhận, loại thông tin |
| Gửi | 010 | Lượt điểm danh có mặt ở hoạt động có thu phí (buổi trong viện: điểm danh; chuyến đi: điểm danh rời viện), sự kiện hủy lượt (đính chính) (FR-031, FR-032); dấu "có thu phí" và dịch vụ của hoạt động | Buổi, người cao tuổi, dịch vụ, ngày của buổi |
| Gửi | 011 | Chuyến đã lên lịch, người tham gia, giờ rời, giờ về dự kiến và gia hạn, thời điểm rời/về thực tế, hủy chuyến (Q-144) | Chuyến, người tham gia, khoảng thời gian |
| Gửi | 012 | Buổi đã đăng ký, chuyến đi, số buổi đã tham gia cho cổng và bản tin (FR-068) | Người cao tuổi, buổi, số lượt |
| Gửi | Module 14 | Tỷ lệ tham gia, danh sách nguy cơ cô lập, tỷ lệ Đạt kiểm tra chất lượng, chuyến quá giờ (FR-058, FR-065, FR-070) | — |

### Key Entities *(include if feature involves data)*

- **Loại hoạt động** – nhóm 1: tên, dấu "hoạt động nhóm", dấu "ngoài viện", nhóm sở thích liên quan, trạng thái.
- **Hoạt động (HOAT_DONG)** – nhóm 1: tên, loại, phạm vi tổ chức, địa điểm mặc định, người phụ trách mặc định, thời lượng, số lượng tối đa, đối tượng, có thu phí, dịch vụ, mẫu lặp (ngày, giờ, ngày bắt đầu, ngày kết thúc), người tham gia thường xuyên, lịch sử thay đổi mẫu (lý do, ngày áp dụng), trạng thái.
- **Buổi hoạt động (BUOI_HOAT_DONG)** – nhóm 2: hoạt động, thời điểm bắt đầu, kết thúc, địa điểm, người phụ trách, nguồn (mẫu / lẻ), trạng thái, lý do hủy hoặc dời. Với chuyến đi thêm: điểm đến, giờ rời dự kiến, giờ về dự kiến và lịch sử gia hạn, phương tiện, trưởng đoàn, người đi cùng, dấu "quá giờ về".
- **Đăng ký** – nhóm 2: buổi, người cao tuổi, nguồn, người đăng ký, thời điểm, trạng thái, lý do hủy, cảnh báo đã xác nhận. Với chuyến đi thêm: kết quả đánh giá khả năng tham gia (người đánh giá, thời điểm, lý do), người đi cùng kèm riêng, trạng thái người tham gia.
- **Điểm danh (DIEM_DANH)** – nhóm 3: buổi, người cao tuổi, loại (buổi / rời viện / về), kết quả, lý do vắng, mức độ tham gia, mức giao tiếp, tình trạng sau, thời điểm thực tế, người ghi, nhãn "ghi nhận muộn", thời điểm trên thiết bị và đồng bộ.
- **Chỉ định hạn chế hoạt động** – nhóm 2: người cao tuổi, phạm vi, thời gian hiệu lực, lý do (thông tin sức khỏe), loại (chính thức / tạm), người gắn (bác sĩ hoặc điều dưỡng), trạng thái (Tạm / Hiệu lực / Đã gỡ / Hết hiệu lực), bác sĩ xác nhận và thời điểm, lịch sử gỡ, rút ngắn, mở rộng.
- **Nhóm sở thích** – nhóm 1; **Sở thích của người cao tuổi** – nhóm 2 có lịch sử: nhóm, mô tả, mức ưa thích, nguồn, người ghi, thời điểm, lý do bỏ.
- **Danh sách kiểm tra chất lượng** – nhóm 2: ca, tầng, hạn (giờ kết thúc ca), trưởng tầng được giao, các công việc được chọn kèm thời điểm và cách chọn (khi hoàn thành / bổ sung ở mốc CFG-M09-04). **Kết quả kiểm tra** – nhóm 3: công việc, kết quả (Đạt / Không đạt / Không kiểm tra được / Quá hạn kiểm tra), ghi chú, ảnh, người kiểm tra, thời điểm, công việc làm lại.
- Dùng từ feature khác: **Người cao tuổi**, **cờ nguy cơ** (001); **Công việc** (005, 003); **Liều thuốc, lần giao thuốc mang theo** (006); **Cảnh báo, Sự cố, vùng khoanh** (007); **Ca, trưởng tầng** (008); **Dịch vụ** (004); **Chi phí** (010).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Trong bộ kiểm thử 4 tuần với 10 hoạt động định kỳ, 100% buổi theo mẫu có mặt trước thời điểm diễn ra ít nhất CFG-M04-09 trừ 1 ngày; 0 buổi trùng (hoạt động, thời điểm bắt đầu); 0 buổi đã điểm danh bị thay đổi do đổi mẫu.
- **SC-002**: 0 đăng ký hoặc lượt thêm lúc điểm danh thành công khi buổi đã đủ số lượng tối đa, khi người cao tuổi có chỉ định hạn chế bao trùm, hoặc khi người hay địa điểm thuộc vùng Đang khoanh vùng.
- **SC-003**: 100% lượt có mặt ở hoạt động có thu phí có đúng một chi phí nháp trỏ về lượt đó; 0 chi phí cho lượt vắng; 100% đính chính hủy lượt có mặt được feature 010 nhận.
- **SC-004**: 0 người cao tuổi được điểm danh rời viện mà chưa có đánh giá Đạt, chưa có trưởng đoàn, hoặc còn liều trong khoảng đi chưa có lần giao thuốc hoặc quyết định không mang thuốc.
- **SC-005**: 100% người được điểm danh rời viện chuyển Hoạt động bên ngoài và 100% người được điểm danh về chuyển Đang lưu trú trong cùng thao tác; 0 người ở Hoạt động bên ngoài quá giờ về dự kiến + CFG-M04-07 mà không có thông báo tới trưởng đoàn và trưởng tầng.
- **SC-006**: 100% người thiếu khi kết thúc điểm danh về hoặc được báo thiếu có đúng một sự cố khẩn cấp đang mở, được tạo trong vòng 1 phút sau thao tác; 0 sự cố thiếu người cho người đã Rời đoàn.
- **SC-007**: Trong 30 kịch bản kiểm thử quy tắc cô lập (đúng ngưỡng, dưới ngưỡng, có vắng mặt, có chỉ định hạn chế, bán trú, người mới vào ở, đã có cảnh báo mở), 100% cảnh báo được tạo đúng ngày và 0 cảnh báo thừa.
- **SC-008**: Với mỗi ca có ít nhất một công việc đủ điều kiện hoàn thành trước mốc CFG-M09-04, số mục được chọn tới mốc đó không nhỏ hơn CFG-M04-11 nhân số công việc đủ điều kiện (làm tròn lên); qua 1.000 ca mô phỏng trên cùng tập công việc, tần suất chọn của mỗi công việc lệch không quá 20% so với kỳ vọng, không phụ thuộc thời điểm hoàn thành; 100% kết quả Không đạt có công việc làm lại trong cùng ca; 100% mục chưa có kết quả lúc hết ca thành "Quá hạn kiểm tra"; 0 công việc gốc bị sửa.
- **SC-011**: 100% chỉ định tạm có thời điểm kết thúc không muộn hơn CFG-M04-14 kể từ lúc gắn; 100% chỉ định tạm không được xác nhận chuyển Hết hiệu lực đúng thời điểm kết thúc; 100% chỉ định tạm có thông báo tới bác sĩ trực trong vòng 5 phút.
- **SC-009**: Nhân viên chăm sóc hoàn tất điểm danh một buổi 20 người trong không quá 3 phút; trưởng đoàn điểm danh rời viện hoặc về cho 15 người trong không quá 2 phút.
- **SC-010**: 0 lần người thân thấy chỉ định hạn chế, mức độ tham gia, cảnh báo cô lập hay kết quả kiểm tra chất lượng trên cổng.

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
- Bán trú chỉ đi được chuyến có giờ về trong khung có mặt theo lịch; đi ngoài khung cần được xử lý như buổi phát sinh của feature 005/010 và đồng ý của người đại diện, chưa được tài liệu nêu; spec chặn theo FR-016 (điểm báo lại 7).
- Mức của thông báo quá giờ về là Trung bình theo feature 009 FR-043b (cần hành động trong ca, chưa phải nguy cơ trực tiếp); khi có người thiếu, mức Khẩn cấp đi qua sự cố của feature 007.
- Với "Báo thiếu người" và kết thúc điểm danh về có người thiếu khi thiết bị mất kết nối, việc tạo sự cố khẩn cấp theo ngoại lệ bắt buộc trực tuyến của 8.6 và Q-01; thiết bị hiển thị hướng dẫn gọi điện cho trưởng tầng ngay.
- Kiểm tra chất lượng lấy mẫu theo ca có phạm vi tầng/khu vực; công việc vệ sinh gắn phòng/khu vực của tầng. Chọn theo xác suất ngay khi hoàn thành (Q-163) cho số mục dao động quanh tỷ lệ; mốc bổ sung CFG-M09-04 bảo đảm không dưới tỷ lệ, không cắt bớt khi vượt. Công việc do chính trưởng tầng thực hiện bị loại khỏi mẫu để tránh tự kiểm tra; tài liệu chưa nêu ai kiểm tra các công việc này (điểm báo lại 8).
- Mọi dữ liệu của spec gắn người cao tuổi được lưu theo CFG-M15-03 (NFR-07).

## Điểm cần báo lại về tài liệu nguồn

1. **5.6 dòng "Đang lưu trú → Hoạt động bên ngoài"** ghi "Tạm dừng công việc trong khoảng thời gian đi", còn **BR-M04-04** ghi công việc chuyển Hủy. Feature 005 FR-020 đã theo BR-M04-04; feature 001 bảng trạng thái ghi "Tạm dừng … tiếp tục công việc đã tạm dừng". Spec này theo BR-M04-04 và feature 005. Nên sửa 5.6 và bảng trạng thái của feature 001 cho khớp.
2. **Permission Matrix 4.4, dòng "Hoạt động, ngoài viện"**: Điều dưỡng và Bác sĩ là "—", nhưng BR-M04-15 cho bác sĩ gắn cờ "không đủ điều kiện", 8.9 yêu cầu "đánh giá khả năng tham gia" và "kiểm tra thuốc cần mang", Q-54 chỉ cho Điều dưỡng ghi liều Mang theo. Đã chốt khi clarify: Q-161 (trưởng đoàn là Trưởng tầng hoặc Nhân viên chăm sóc; người đi cùng là nhân viên bất kỳ có ca; Trưởng tầng đánh giá) và Q-162 (Bác sĩ gắn chỉ định có phạm vi; Điều dưỡng gắn tạm tối đa CFG-M04-14, Bác sĩ xác nhận hoặc gỡ). Cần bổ sung chú thích cho dòng này: BS "T" chỉ với chỉ định hạn chế; ĐD "T" chỉ với chỉ định tạm, quyết định không mang thuốc và "Báo thiếu người" khi đi cùng. Cần thêm Q-161 → Q-163 vào mục 24.2 và sửa BR-M04-15 cho có chỉ định tạm.
3. **BR-M04-16** ghi liều Mang theo "giao cho người đi cùng ghi nhận"; Q-54 đã chốt chỉ Điều dưỡng ghi nhận (không có điều dưỡng đi cùng thì ghi theo báo lại). Nên sửa câu chữ BR-M04-16 theo Q-54.
4. **Sở thích (8.10)** chưa có dòng trong 4.4 và chưa có thực thể trong ERD. Đề xuất thêm dòng "Sở thích, hồ sơ tinh thần" (TT, ĐD, CS: T; QL: X) và thực thể SO_THICH, CHI_DINH_HAN_CHE, DANH_SACH_KIEM_TRA, KET_QUA_KIEM_TRA vào ERD miền D.
5. **BR-M04-15 và BR-M05-11** không nêu số phận của buổi, đăng ký đã có khi khu bị khoanh vùng. Spec đặt mặc định: hủy tự động, gỡ vùng không khôi phục, theo cách Q-120 cho lượt thăm (FR-021); đề xuất chốt thành một quyết định Q mới khi clarify.
6. **Tham số đề xuất** (cần bổ sung vào Phụ lục 25 nếu được chốt): CFG-M04-13 thời gian sớm nhất được điểm danh rời viện trước giờ rời dự kiến, mặc định \[1 giờ\] (FR-040); CFG-M04-14 thời hạn tối đa của chỉ định tạm, mặc định \[24 giờ\] (FR-023a, Q-162). Spec dùng lại CFG-M09-04 cho mốc chọn bổ sung kiểm tra chất lượng (FR-059a) và CFG-M04-06 cho nhãn "ghi nhận muộn" và nhắc điểm danh buổi.
7. **Bán trú tham gia chuyến đi** vượt khung có mặt theo lịch: tài liệu không nêu; spec chặn (FR-016).
8. **BR-M04-23** không nêu ai kiểm tra công việc do chính trưởng tầng thực hiện và xử lý khi quá hạn kiểm tra. Spec loại các công việc đó khỏi mẫu (FR-060) và đặt kết quả "Quá hạn kiểm tra" (FR-063). Theo Q-163 (hạn là hết ca), ca không có trưởng tầng trực (thường là ca đêm) sẽ có nhiều mục quá hạn trừ khi trưởng tầng được giao ghi từ xa; cơ sở cần xem lại khi vận hành, có thể bổ sung quyền cho Người phụ trách ca (hiện 4.4 và FR-066 không cho).
9. **5.5** không có chuyển Hoạt động bên ngoài → Tạm vắng, nên người thân không đón thẳng người cao tuổi từ điểm tham quan được (FR-045). Nếu cơ sở cần, phải bổ sung chuyển này vào 5.5 và quy trình đón 14.3.
10. **Feature 005 bảng kết quả 8.6** chưa có "giao tiếp"; spec ghi giao tiếp lúc điểm danh (FR-029). Nếu cơ sở muốn ghi giao tiếp hằng ngày ngoài hoạt động, cần thêm kết quả vào feature 005.
11. **Feature 007** cần: nguồn "hoạt động ngoài viện" đã có ở FR-040; thêm loại cảnh báo "nguy cơ cô lập" (mức Nhẹ, khóa gộp theo người cao tuổi) vào khóa loại FR-028; nhận diễn biến "đã tìm thấy, đã trở về" cho sự cố thiếu người và "đã tham gia hoạt động nhóm" cho cảnh báo cô lập.
12. **Feature 009** cần thêm dòng "014 – Mọi thông báo của feature 014, theo bảng FR-071 của spec 014" vào bảng mức FR-043b.
13. **Thuật ngữ 2.4**: đề xuất thêm "Buổi", "Chuyến đi", "Hoạt động nhóm", "Chỉ định hạn chế hoạt động", "Chỉ định tạm", "Ngày tính", "Danh sách kiểm tra chất lượng", "Người đi cùng".
14. **Quyết định chốt ngày 2026-09-27 (Clarifications), cần đưa vào mục 24.2**:

    | Mã | Vấn đề | Quyết định | Căn cứ |
    | --- | --- | --- | --- |
    | Q-161 | Ai làm trưởng đoàn, người đi cùng, ai đánh giá khả năng tham gia chuyến đi | Trưởng đoàn: Trưởng tầng hoặc Nhân viên chăm sóc; người đi cùng: nhân viên bất kỳ có ca chồng thời gian chuyến, kể cả Điều dưỡng; Trưởng tầng đánh giá | FR-035, FR-036, FR-046 |
    | Q-162 | Cờ "không đủ điều kiện" có phạm vi không; điều dưỡng có gắn tạm không | Có phạm vi (mọi hoạt động / hoạt động nhóm / ngoài viện / theo loại), Bác sĩ gắn, là "chỉ định hạn chế" của BR-M04-22; Điều dưỡng gắn tạm tối đa CFG-M04-14 \[24 giờ\], Bác sĩ xác nhận hoặc gỡ, quá hạn thì hết hiệu lực | FR-023 → FR-023c |
    | Q-163 | Kiểm tra chất lượng chọn mẫu lúc nào, hạn bao lâu | Chọn ngẫu nhiên ngay khi công việc hoàn thành theo CFG-M04-11, bổ sung ở mốc CFG-M09-04 trước khi kết ca; hạn là hết ca; làm lại trong cùng ca | FR-059, FR-059a, FR-063, FR-064 |
