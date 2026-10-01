# Feature Specification: Quản lý đồ gửi của người cao tuổi

**Feature Branch**: `013-personal-belongings`

**Created**: 2026-09-27

**Status**: Draft

**Input**: User description: "Quản lý đồ gửi của người cao tuổi theo docs/nghiep-vu.md Module 12 (mục 16): tiếp nhận có vị trí lưu giữ và hình ảnh với đồ có giá trị; mọi lần bàn giao ghi ai giao, ai nhận, khi nào, tình trạng; trạng thái đồ gửi; thất lạc hoặc hư hỏng tự tạo sự cố; chặn kết thúc lưu trú khi còn đồ chưa trả; chỉ trả cho người có quyền."

## Clarifications

### Session 2026-09-27

- Q: Thất lạc và Hư hỏng có phải trạng thái cuối không; đồ tìm thấy hoặc đồ hỏng có được trả lại, và có chặn kết thúc lưu trú không? → A: Không phải trạng thái cuối. Thất lạc → Đang giữ khi tìm thấy; Hư hỏng → Đã trả (trả đồ hỏng cho gia đình). Đồ Hư hỏng chặn kết thúc lưu trú như đồ Đang giữ; đồ Thất lạc không chặn (đề xuất Q-148; người dùng chọn phương án A).
- Q: Người cao tuổi có được là người nhận khi trả đồ gửi không? → A: Được, nhưng luôn cần xác nhận người nhận khác (người đại diện xác nhận, như mọi người nhận khác) (đề xuất Q-149; người dùng chọn phương án B). *(Sửa ở Session 2026-09-30: người cao tuổi không có cờ nguy cơ đi lạc tự nhận lại đồ không phải tiền mặt, trang sức thì không cần xác nhận, Q-253.)*
- Q: Ngoài người đại diện, Quản lý viện có được duyệt cho người nhận khác không? → A: Có, khi không liên hệ được người đại diện: yêu cầu đã Chờ xác nhận quá CFG-M10-08 mà người đại diện chưa phản hồi, hoặc người cao tuổi không có người đại diện Hiệu lực; bắt buộc lý do và bằng chứng (đề xuất Q-150; người dùng chọn phương án B). *Ghi chú khi rà checklist business-rules: thời gian chờ này được tách thành tham số riêng CFG-M12-05, cùng mặc định \[4 giờ\]; CFG-M10-08 chỉ còn dùng cho thời hạn hiệu lực của xác nhận.*
- Q: Trên cổng người thân, ai được xem danh sách đồ gửi, lịch sử bàn giao và ảnh? → A: Chỉ người đại diện và người thân có quyền "được phép đón" đang hiệu lực, tức những người có quyền nhận; người thân khác không thấy đồ gửi (đề xuất Q-151).
- Q: Đồ gửi không có ai nhận sau khi người cao tuổi đã kết thúc lưu trú hoặc qua đời thì viện xử lý thế nào? → A: Sau CFG-M12-03 (đề xuất, mặc định \[90 ngày\]) kể từ khi hồ sơ ở trạng thái cuối, Hành chính lập đề nghị "Xử lý đồ không người nhận" (thanh lý, tiêu hủy hoặc chuyển cơ quan có thẩm quyền) kèm biên bản; Quản lý viện duyệt thì đồ chuyển trạng thái cuối "Đã xử lý" (đề xuất Q-152).
- Q: Ai được thực hiện lệnh trả đồ gửi cho người thân: chỉ Hành chính, hay cả Điều dưỡng tại tầng? → A: Hành chính trả mọi loại đồ; Điều dưỡng chỉ trả đồ không có giá trị (ngoài CFG-M12-01) cho người có quyền nhận; trả theo xác nhận người nhận khác chỉ do Hành chính (đề xuất Q-153).
- Q: Viện có cần kiểm kê định kỳ đồ gửi đang giữ trên hệ thống không? → A: Có, theo chu kỳ CFG-M12-04 (đề xuất, mặc định \[hằng tháng, hạn hoàn thành 3 ngày\]), chỉ cho đồ có giá trị ở trạng thái Đang giữ, theo từng vị trí lưu giữ; hệ thống sinh phiếu kiểm kê cho Hành chính; đồ không tìm thấy phải ghi Báo thất lạc, lệch tình trạng thì ghi nhận tình trạng mới hoặc Ghi hư hỏng (đề xuất Q-154).
- Q: Đồ có giá trị có được giao cho người cao tuổi tự giữ và dùng trong phòng không? → A: Điện thoại giao tự do; tiền mặt và trang sức chỉ giao khi người đại diện đã đồng ý cho đúng đồ đó (qua cổng hoặc bản ký) và người cao tuổi không có cờ nguy cơ đi lạc (đề xuất Q-155).
- Q: Các mặc định spec tự đặt khi rà checklist (thang tình trạng, bằng chứng liên hệ, người giữ vắng mặt và đồ khi hết ca, tiền mặt không người nhận, giới hạn đính chính) có được giữ không? → A: Giữ nguyên toàn bộ theo mặc định đề xuất; chốt thành Q-156 (danh mục 4 mức tình trạng, FR-001a), Q-157 (CFG-M12-06, CFG-M12-07), Q-158 (FR-014, FR-014a), Q-159 (tiền mặt chỉ chuyển cơ quan có thẩm quyền, FR-027a), Q-160 (FR-011a) và đưa vào mục 24.2.

### Cập nhật 2026-09-27 (đồng bộ với spec 016)

Theo Q-194, báo cáo đồ gửi (số đồ thất lạc, hư hỏng, đồ chưa trả) thuộc giai đoạn sau; bảng giao tiếp được ghi rõ. Ở giai đoạn này, Hành chính thấy số liệu sự cố đồ gửi thất lạc, hư hỏng trên báo cáo của feature 016 qua dấu "không thuộc sức khỏe" của danh mục loại sự cố (feature 007 FR-042, Q-202).

### Session 2026-09-30 (rà soát vận hành)

- Q: Người cao tuổi còn minh mẫn tự nhận lại đồ của chính mình có cần người đại diện xác nhận không? → A: Không, khi người cao tuổi không có cờ nguy cơ đi lạc và đồ không thuộc loại "cần đồng ý khi giao sử dụng" (tiền mặt, trang sức; các đồ này vẫn theo FR-015a). Người trả vẫn theo Q-153. Thay quyết định trước đó (Q-149) (Q-253).

### Session 2026-10-01 (/speckit-clarify, checklist cross-feature)

- Q: Với người cao tuổi tự nhận lại đồ theo Q-253, Điều dưỡng tại tầng có được trả đồ không có giá trị thẳng cho người cao tuổi không? → A: Có. Người cao tuổi thuộc ngoại lệ của FR-016 được coi là người có quyền nhận với đồ của chính mình; Điều dưỡng (trong phạm vi) trả đồ không có giá trị, Hành chính trả mọi đồ (Q-258).

## Phạm vi

**Trong phạm vi** (Module 12, mục 16; UC-65; DBR-22):

1. Danh mục loại đồ (16.1) với dấu "đồ có giá trị" theo CFG-M12-01, và danh mục vị trí lưu giữ (1.5 nhóm 1).
2. Tiếp nhận đồ gửi: người cao tuổi, vật phẩm, số lượng, tình trạng, người giao, người nhận, thời điểm, vị trí lưu giữ, hình ảnh (16.2, BR-M12-05).
3. Bàn giao đồ gửi: mọi lần đồ đổi người giữ, đổi vị trí hoặc đổi trạng thái đều sinh một bản ghi bàn giao không sửa, không xóa (16.3, BR-M12-01, DBR-22).
4. Trạng thái đồ gửi: Đang giữ, Đang được người cao tuổi sử dụng, Đã trả, Thất lạc, Hư hỏng (16.4 "(Bổ sung)").
5. Thất lạc hoặc hư hỏng: tự yêu cầu feature 007 tạo sự cố mức Trung bình và thông báo người liên hệ chính (BR-M12-02).
6. Trả lại đồ gửi chỉ cho người có quyền; người nhận khác cần người đại diện xác nhận (16.4, BR-M12-04).
7. Cung cấp điều kiện "không còn đồ gửi chưa trả" cho kết thúc lưu trú và mục "xử lý đồ gửi" cho danh sách việc sau qua đời (BR-M12-03, 5.6, 6.8); xử lý đồ không người nhận sau khi hồ sơ ở trạng thái cuối (Q-152).
8. Kiểm kê định kỳ đồ có giá trị đang giữ theo vị trí lưu giữ (Q-154).
9. Xem đồ gửi trên cổng người thân; các thông báo của nghiệp vụ đồ gửi, gửi qua feature 009.

**Ngoài phạm vi** (spec này **nhận** dữ liệu hoặc **cung cấp** dữ liệu cho feature sở hữu):

- Quy tắc dùng chung (nhóm dữ liệu, lý do bắt buộc, đính chính, yêu cầu phê duyệt, nhật ký, tham số): feature 000. Spec này kế thừa và không lặp lại.
- **Thuốc gia đình gửi** (16.1 có liệt kê): tiếp nhận, đối chiếu, sử dụng, hoàn trả theo 11.4 do feature 006 sở hữu, vì Hành chính không được xem thông tin thuốc (19.3; feature 006 điểm báo lại 6). Spec này không nhận thuốc làm đồ gửi.
- Đồ ăn gia đình mang vào: feature 011 (12.4).
- Trạng thái người cao tuổi, hồ sơ kết thúc lưu trú, ngoại lệ điều kiện kết thúc, danh sách việc sau qua đời: feature 001, 004. Giường và tầng của người cao tuổi: feature 003.
- Người thân, người đại diện, người liên hệ chính, danh sách được phép đón, cổng người thân: feature 012. Tài khoản và phạm vi dữ liệu: feature 002.
- Tạo, xử lý, đóng sự cố: feature 007. Gửi thông báo: feature 009.
- Bồi thường đồ thất lạc, hư hỏng: không sinh chi phí hay khoản điều chỉnh trong hệ thống; xử lý ngoài hệ thống (1.2).
- Quản lý kho tổng thể, tài sản của viện (xe lăn, đồ dùng của viện): ngoài hệ thống (1.2, dòng đầu mục 16).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Hành chính tiếp nhận đồ gửi, có vị trí lưu giữ và hình ảnh với đồ có giá trị (Priority: P1)

Khi người cao tuổi nhập viện hoặc người thân mang đồ vào trong thời gian lưu trú, nhân viên hành chính (hoặc điều dưỡng tại tầng) ghi nhận từng đồ gửi: loại đồ, mô tả, số lượng, tình trạng, người giao (người thân hoặc người cao tuổi), người nhận (chính nhân viên đó), thời điểm, vị trí lưu giữ. Đồ thuộc danh mục có giá trị (CFG-M12-01, mặc định điện thoại, trang sức, tiền mặt) bắt buộc có ít nhất một hình ảnh. Đồ mà người cao tuổi dùng hằng ngày (kính, quần áo, xe lăn riêng) có thể được giao ngay cho người cao tuổi sử dụng trong cùng lần tiếp nhận.

**Why this priority**: Tiếp nhận là điểm bắt đầu của mọi đồ gửi (DBR-22). Thiếu vị trí lưu giữ hoặc hình ảnh thì không truy vết được khi mất, hỏng; đây là rủi ro tranh chấp lớn nhất của module.

**Independent Test**: Với A Đang lưu trú, tiếp nhận 1 điện thoại không ảnh (bị chặn), rồi có ảnh; tiếp nhận 2.000.000 đồng tiền mặt; tiếp nhận 5 bộ quần áo và giao ngay cho A sử dụng; thử tiếp nhận cho người đã Kết thúc lưu trú.

**Acceptance Scenarios**:

1. **Given** A Đang lưu trú, người thân R mang điện thoại tới, **When** hành chính H ghi nhận đồ gửi loại "Điện thoại", mô tả, số lượng 1, tình trạng "màn hình trầy nhẹ", người giao R, vị trí "Tủ khóa hành chính 1", không đính kèm ảnh, **Then** hệ thống chặn vì loại "Điện thoại" thuộc danh mục đồ có giá trị (BR-M12-05); **When** H đính kèm ảnh, **Then** đồ gửi được tạo ở trạng thái Đang giữ, kèm bản ghi bàn giao loại "Tiếp nhận" (người giao R, người nhận H, thời điểm, số lượng, tình trạng, vị trí, ảnh).
2. **Given** A Đang lưu trú, **When** H tiếp nhận tiền mặt, **Then** hệ thống bắt buộc số tiền (đồng) và ảnh; đồ gửi ở Đang giữ tại vị trí đã chọn.
3. **Given** A Đang lưu trú, **When** điều dưỡng D tại tầng 2 tiếp nhận "5 bộ quần áo" do R mang tới và chọn "giao ngay cho người cao tuổi sử dụng", **Then** trong cùng một lệnh hệ thống tạo đồ gửi với hai bản ghi bàn giao: "Tiếp nhận" (R → D) và "Giao sử dụng" (D → A); trạng thái hiện tại là Đang được người cao tuổi sử dụng; không cần ảnh vì "Quần áo" không thuộc CFG-M12-01.
4. **Given** B đang ở Đang tiếp nhận vào ngày nhập viện, **When** H tiếp nhận đồ gửi của B, **Then** được chấp nhận.
5. **Given** C đã Kết thúc lưu trú, **When** H tìm cách tiếp nhận đồ gửi mới cho C, **Then** hệ thống chặn (FR-006).
6. **Given** H chọn vị trí lưu giữ đã Ngừng hiệu lực, **When** lưu, **Then** hệ thống chặn và chỉ cho chọn vị trí đang hiệu lực.
7. **Given** nhân viên chăm sóc S hoặc bác sĩ, **When** tìm cách tiếp nhận đồ gửi, **Then** hệ thống không cho phép (4.4 dòng "Đồ gửi": chỉ Hành chính, Điều dưỡng có T).

---

### User Story 2 - Mọi lần bàn giao ghi ai giao, ai nhận, khi nào, vật gì, số lượng, tình trạng (Priority: P1)

Trong thời gian lưu trú, đồ gửi có thể được giao cho người cao tuổi sử dụng, thu lại để cất giữ, chuyển cho nhân viên khác giữ hoặc chuyển vị trí lưu giữ. Mỗi lần như vậy là một lệnh bàn giao, sinh một bản ghi bàn giao có đủ: người giao, người nhận, thời điểm, vật, số lượng, tình trạng (và vị trí khi đồ được cất giữ). Bản ghi bàn giao không sửa, không xóa; sai sót được đính chính theo feature 000. Trạng thái hiện tại của đồ gửi luôn là trạng thái của bản ghi bàn giao gần nhất.

**Why this priority**: Là nội dung cốt lõi của 16.3 và BR-M12-01; lịch sử bàn giao là căn cứ duy nhất khi có tranh chấp mất, hỏng.

**Independent Test**: Với điện thoại của A đang Đang giữ, giao cho A sử dụng, thu lại, chuyển cho điều dưỡng khác giữ tại kho tầng; ghi một lần bàn giao sai số lượng rồi đính chính; kiểm tra lịch sử và trạng thái hiện tại sau mỗi bước.

**Acceptance Scenarios**:

1. **Given** điện thoại của A Đang giữ tại "Tủ khóa hành chính 1", **When** H giao cho A sử dụng, **Then** bản ghi bàn giao "Giao sử dụng" (H → A) được tạo với thời điểm, số lượng, tình trạng; trạng thái chuyển Đang được người cao tuổi sử dụng; vị trí lưu giữ để trống.
2. **Given** điện thoại của A đang được A sử dụng, **When** điều dưỡng D thu lại điện thoại lúc 21:00 và cất vào "Tủ tầng 2", **Then** bản ghi "Thu lại" (A → D) được tạo, bắt buộc vị trí và tình trạng; trạng thái về Đang giữ.
3. **Given** tình trạng lần bàn giao trước là "màn hình trầy nhẹ", **When** D ghi tình trạng lần này "nứt góc màn hình", **Then** hệ thống hiển thị chênh lệch tình trạng so với lần trước và yêu cầu D xác nhận ghi nhận như tình trạng mới hoặc chuyển sang lệnh "Ghi hư hỏng" (FR-015).
4. **Given** đồ gửi Đang giữ tại "Tủ tầng 2" do D giữ, **When** H nhận lại và chuyển về "Tủ khóa hành chính 1", **Then** bản ghi "Chuyển giữ" (D → H) được tạo với vị trí mới; trạng thái vẫn Đang giữ.
5. **Given** bất kỳ người dùng nào, kể cả Quản lý viện, **When** tìm cách sửa hoặc xóa một bản ghi bàn giao, **Then** không có thao tác đó (BR-M12-01, BR-M15-03).
6. **Given** H ghi nhầm số lượng 2 thay vì 1 ở bản ghi tiếp nhận và H vẫn còn quyền, **When** H tạo bản đính chính kèm lý do, **Then** bản đính chính được lưu, bản gốc giữ nguyên và hiện dấu "đã đính chính"; **Given** H đã nghỉ việc, **When** nhân viên hành chính H2 lập bản đính chính, **Then** bản đính chính chỉ có hiệu lực khi Quản lý viện duyệt (feature 000 FR-026 (c)); **When** trưởng tầng tìm cách đính chính, **Then** bị chặn.
7. **Given** 5 bộ quần áo của A đang Đang giữ, **When** D giao 2 bộ cho A sử dụng, **Then** hệ thống tách thành hai đồ gửi: 2 bộ Đang được người cao tuổi sử dụng (bản ghi "Giao sử dụng" số lượng 2) và 3 bộ còn Đang giữ; cả hai trỏ về cùng lần tiếp nhận (FR-012).
8. **Given** A muốn giữ 500.000 đồng tiền mặt trong phòng, **When** H thực hiện "Giao sử dụng" khi chưa có đồng ý của người đại diện, **Then** hệ thống chặn; **When** người đại diện R đồng ý cho tự giữ khoản tiền đó qua cổng, **Then** H giao được; **Given** sau đó A được gắn cờ nguy cơ đi lạc, **Then** Hành chính và điều dưỡng phụ trách được báo để thu lại, và mọi lần Giao sử dụng tiếp theo bị chặn. **When** H giao điện thoại cho A, **Then** không cần đồng ý (FR-015a, Q-155).
9. **Given** D ghi nhầm "Báo thất lạc" cho kính của A (kính thực ra đang ở phòng A), sự cố đã được tạo và người liên hệ chính đã được báo, **When** bản đính chính "Hủy ghi nhận" có hiệu lực, **Then** kính trở về Đang được người cao tuổi sử dụng theo bản ghi liền trước; feature 007 nhận yêu cầu chuyển sự cố Đã hủy; người liên hệ chính được báo đính chính; **When** ai đó tìm cách Hủy ghi nhận bản ghi "Giao sử dụng" trước đó (không phải bản ghi gần nhất), **Then** hệ thống chặn (FR-011a).
10. **Given** H và D cùng mở điện thoại của A đang Đang giữ, **When** H lưu lệnh Trả trước rồi D lưu lệnh Giao sử dụng, **Then** lệnh của D bị từ chối và D thấy trạng thái Đã trả (FR-011b).

---

### User Story 3 - Đồ gửi chỉ được trả cho người có quyền (Priority: P1)

Khi người thân nhận lại đồ gửi (mang về nhà, khi kết thúc lưu trú hoặc sau qua đời), nhân viên hành chính kiểm tra người nhận (điều dưỡng tại tầng chỉ trả được đồ không có giá trị cho người có quyền nhận, Q-153): người đại diện hoặc người thân có quyền "được phép đón" đang hiệu lực thì trả được ngay sau khi xác nhận danh tính. Người nhận khác, kể cả chính người cao tuổi (trừ người cao tuổi không có cờ nguy cơ đi lạc nhận lại đồ không phải tiền mặt, trang sức, Q-253), chỉ nhận được khi người đại diện đã xác nhận cho đúng người nhận và đúng đồ gửi đó; khi không liên hệ được người đại diện, Quản lý viện duyệt thay kèm lý do và bằng chứng (Q-149, Q-150). Đồ có giá trị bắt buộc có ảnh khi trả. Người nhận ký xác nhận; đồ chuyển Đã trả.

**Why this priority**: Trả nhầm người là rủi ro an toàn tài sản và tranh chấp trực tiếp (BR-M12-04); là bước bắt buộc trước khi kết thúc lưu trú.

**Independent Test**: Trả điện thoại của A cho người đại diện R; trả cho người thân N không có quyền đón (bị chặn); người đại diện R xác nhận cho N qua cổng rồi trả; trả cho người lạ X kèm bản ký của R; thử trả đồ có giá trị không có ảnh.

**Acceptance Scenarios**:

1. **Given** R là người đại diện của A, **When** H chọn trả điện thoại cho R, xác nhận danh tính R, chụp ảnh điện thoại, ghi tình trạng và R ký nhận, **Then** bản ghi "Trả" (H → R) được tạo, đồ chuyển Đã trả.
2. **Given** M là người thân của A có quyền "được phép đón" Hiệu lực, **When** H trả kính của A cho M, **Then** được chấp nhận như kịch bản 1; không cần ảnh vì "Kính" không thuộc CFG-M12-01.
3. **Given** N là người thân của A nhưng không có quyền đón và không phải người đại diện, **When** H chọn trả đồ cho N, **Then** hệ thống chặn và cho H gửi yêu cầu xác nhận tới người đại diện của A.
4. **Given** yêu cầu xác nhận cho N nhận "Điện thoại" của A, **When** người đại diện R xác nhận qua cổng người thân, **Then** H trả được cho N đúng đồ gửi đó, một lần, trong thời hạn hiệu lực của xác nhận; bản ghi "Trả" tham chiếu xác nhận của R. **When** H dùng xác nhận đó để trả thêm "Trang sức" cho N, **Then** hệ thống chặn vì trang sức không nằm trong xác nhận.
5. **Given** X không phải người thân của A, **When** H ghi nhận xác nhận của người đại diện R bằng bản ký (scan đính kèm), **Then** H trả được cho X; bản ghi lưu danh tính X và bản ký.
6. **Given** R đang là người đại diện khi yêu cầu xác nhận được tạo nhưng R thôi làm người đại diện trước khi xác nhận, **When** R tìm cách xác nhận, **Then** hệ thống từ chối vì R không còn là người đại diện.
7. **Given** trả tiền mặt 2.000.000 đồng, **When** H không đính kèm ảnh, **Then** hệ thống chặn (BR-M12-05).
8. **Given** A kết thúc lưu trú với 4 đồ gửi còn Đang giữ, **When** H chọn trả cả 4 đồ cho R trong một lần, **Then** hệ thống tạo 4 bản ghi "Trả" riêng với cùng người nhận, thời điểm và một chữ ký; mỗi đồ có ảnh nếu thuộc CFG-M12-01.
9. **Given** đồ gửi đang Đang được người cao tuổi sử dụng, **When** R đến nhận, **Then** H trả trực tiếp được; bản ghi "Trả" ghi H là người bàn giao và ghi chú "thu từ người cao tuổi" (FR-018).
10. **Given** A được xuất viện về nhà và muốn tự mang đồ đi mà không có người thân đến, **When** H chọn trả cho chính A, **Then** hệ thống chặn và cho H lập yêu cầu xác nhận người nhận khác với người nhận là A; **When** người đại diện R xác nhận, **Then** H trả được cho A, bản ghi "Trả" ghi người nhận là người cao tuổi và tham chiếu xác nhận của R (Q-149). **(Sửa, 2026-09-30, Q-253)** Kịch bản này áp khi A có cờ nguy cơ đi lạc hoặc đồ là tiền mặt, trang sức. **When** A không có cờ đi lạc và đồ là quần áo, kính, **Then** H trả thẳng cho A, không cần xác nhận người nhận khác.
11. **Given** yêu cầu xác nhận cho X nhận "Điện thoại" của A đã Chờ xác nhận quá CFG-M12-05 (mặc định \[4 giờ\]) mà R không phản hồi, **When** H ghi lý do "không liên hệ được người đại diện" kèm bằng chứng các lần liên hệ, **Then** Quản lý viện duyệt được yêu cầu (bắt buộc lý do); xác nhận chuyển Hiệu lực và R được báo. **When** Quản lý viện tìm cách duyệt trước khi quá CFG-M12-05 trong khi A còn người đại diện Hiệu lực, **Then** hệ thống chặn (FR-017a, Q-150).
12. **Given** người cao tuổi E không còn người đại diện Hiệu lực (ví dụ người đại diện duy nhất đã thôi), **When** H lập yêu cầu xác nhận người nhận khác, **Then** Quản lý viện duyệt được ngay, kèm lý do và bằng chứng (FR-017a).
13. **Given** tối chủ nhật, người thân M có quyền đón đến lấy kính và 2 bộ quần áo của A, **When** điều dưỡng D (trong phạm vi) thực hiện lệnh Trả, **Then** được chấp nhận; **When** D trả thêm điện thoại của A trong cùng lần, **Then** cả lần trả bị chặn vì điện thoại là đồ có giá trị và chỉ Hành chính được trả; **When** D chọn trả kính cho người thân N không có quyền đón, **Then** hệ thống chặn và không cho D lập yêu cầu xác nhận người nhận khác (FR-016a, Q-153).

---

### User Story 4 - Thất lạc hoặc hư hỏng tự tạo sự cố và báo người liên hệ chính (Priority: P1)

Khi phát hiện đồ gửi bị mất hoặc bị hỏng trong thời gian viện giữ hoặc khi người cao tuổi đang sử dụng, nhân viên có quyền ghi "Báo thất lạc" hoặc "Ghi hư hỏng" kèm mô tả, thời điểm phát hiện, lý do. Hệ thống trong cùng một lần: chuyển trạng thái đồ gửi, yêu cầu feature 007 tạo sự cố mức Trung bình gắn người cao tuổi và đồ gửi, và yêu cầu feature 009 thông báo người liên hệ chính.

**Why this priority**: BR-M12-02 là quy tắc tự động của module; bảo đảm mọi mất mát, hư hỏng đều được xử lý và gia đình được biết kịp thời.

**Independent Test**: Ghi thất lạc cho điện thoại của A; ghi hư hỏng cho kính của B có ảnh; kiểm tra mỗi lần tạo đúng một sự cố mức Trung bình và một thông báo tới người liên hệ chính; thử ghi nhận tình trạng xấu lúc tiếp nhận (không tạo sự cố).

**Acceptance Scenarios**:

1. **Given** điện thoại của A Đang giữ tại "Tủ tầng 2", **When** D ghi "Báo thất lạc" với thời điểm phát hiện và mô tả, **Then** trong cùng một lần: đồ chuyển Thất lạc với bản ghi bàn giao "Báo thất lạc" (người giao là người giữ theo bản ghi gần nhất, người ghi nhận là D); feature 007 nhận yêu cầu tạo sự cố loại "đồ gửi thất lạc", mức Trung bình, gắn A và đồ gửi; người liên hệ chính của A nhận thông báo mức Trung bình.
2. **Given** kính của B đang được B sử dụng, **When** D ghi "Ghi hư hỏng" kèm mô tả và ảnh, **Then** đồ chuyển Hư hỏng; bản ghi bàn giao ghi người đang giữ đồ hỏng (D) và vị trí cất; sự cố loại "đồ gửi hư hỏng" mức Trung bình được tạo; người liên hệ chính được báo.
3. **Given** H tiếp nhận một điện thoại đã vỡ màn hình từ trước, **When** ghi tình trạng "vỡ màn hình" lúc tiếp nhận, **Then** đồ gửi ở Đang giữ, không tạo sự cố, vì hư hỏng có trước khi viện nhận.
4. **Given** đồ gửi đã Thất lạc, **When** D tìm cách ghi "Báo thất lạc" lần nữa, **Then** hệ thống chặn; không tạo sự cố trùng.
5. **Given** điện thoại của A đã Thất lạc và sự cố đang mở, **When** D tìm thấy điện thoại, **Then** D ghi lệnh "Tìm thấy" kèm vị trí lưu giữ và tình trạng; đồ chuyển Đang giữ; feature 007 nhận diễn biến "đã tìm thấy" cho sự cố (sự cố vẫn do người xử lý đóng theo feature 007); người liên hệ chính được báo mức Nhẹ (Q-148). Nếu đồ tìm thấy đã hỏng, D ghi tiếp "Ghi hư hỏng" và một sự cố mới được tạo.
6. **Given** kính của B ở Hư hỏng, cất tại "Tủ khóa hành chính 1", **When** H trả kính cho người đại diện của B, **Then** được chấp nhận như lệnh Trả thông thường; đồ chuyển Đã trả, bản ghi ghi tình trạng hỏng và ý kiến của người nhận (nếu có) (Q-148).
7. **Given** sự cố thất lạc được tạo, **When** feature 007 xử lý sự cố, **Then** sự cố không kích hoạt yêu cầu đánh giá lại người cao tuổi "sau sự cố" (FR-022).
8. **Given** đầu tháng, "Tủ khóa hành chính 1" có 6 đồ có giá trị Đang giữ và 4 bộ quần áo, **When** Bộ lập lịch chạy theo CFG-M12-04, **Then** hệ thống sinh một phiếu kiểm kê cho vị trí đó gồm đúng 6 đồ có giá trị (không gồm quần áo) và giao Hành chính. **When** H đếm được 5 đồ, không thấy nhẫn vàng của C, và hoàn thành phiếu, **Then** hệ thống bắt buộc ghi "Báo thất lạc" cho nhẫn trong cùng lần hoàn thành, sự cố và thông báo tạo theo FR-021; 5 đồ còn lại ghi "Đúng" mà không sinh bản ghi bàn giao. **When** phiếu chưa hoàn thành sau hạn 3 ngày, **Then** Hành chính được nhắc mỗi ngày và Quản lý viện được báo một lần (FR-028a → FR-028c, Q-154).
9. **Given** 5 bộ quần áo của A ở Thất lạc, **When** D tìm thấy 3 bộ, **Then** hệ thống tách: 3 bộ chuyển Đang giữ qua lệnh "Tìm thấy", 2 bộ vẫn Thất lạc; sự cố nhận diễn biến "đã tìm thấy 3/5" và vẫn mở (FR-012, Q-148).
10. **Given** kính của B ở Hư hỏng tại "Tủ khóa hành chính 1", **When** H không còn thấy kính ở tủ và ghi "Báo thất lạc", **Then** đồ chuyển Thất lạc và một sự cố "đồ gửi thất lạc" mới được tạo, độc lập với sự cố hư hỏng trước đó.
11. **Given** phiếu kiểm kê tháng 10 của "Tủ khóa hành chính 1" đang Chờ kiểm kê với 6 đồ, **When** H trả điện thoại của A trước khi kiểm, **Then** điện thoại tự bị loại khỏi phiếu với ghi chú "đã Trả"; **Given** mọi đồ trong phiếu đã rời vị trí trước khi kiểm, **When** H hủy phiếu kèm lý do, **Then** phiếu chuyển Đã hủy (FR-028b).
12. **Given** H kiểm kê "Tủ khóa hành chính 1" thấy một đồng hồ không có trong phiếu và không khớp đồ gửi nào, **When** hoàn thành phiếu, **Then** đồng hồ được ghi ở phần "đồ không rõ chủ" kèm ảnh, không tạo đồ gửi, Quản lý viện được báo; **Given** chiếc nhẫn đã ghi Thất lạc tháng trước được thấy ở tủ, **Then** H ghi "Tìm thấy" cho nhẫn đó (FR-028b).

---

### User Story 5 - Kết thúc lưu trú bị chặn khi còn đồ gửi chưa trả (Priority: P2)

Khi hồ sơ kết thúc lưu trú được lập (feature 004), hệ thống cung cấp điều kiện "không còn đồ gửi chưa trả" với danh sách đồ còn Đang giữ, Đang được người cao tuổi sử dụng hoặc Hư hỏng làm căn cứ (đồ Thất lạc không chặn, Q-148), và cập nhật điều kiện mỗi khi đồ gửi đổi trạng thái. Người đại diện được báo danh sách đồ cần nhận. Khi người cao tuổi qua đời, mục "xử lý đồ gửi" của danh sách việc sau qua đời được cập nhật theo cùng quy tắc. Đồ còn lại sau khi Quản lý viện duyệt ngoại lệ vẫn trả được và được nhắc định kỳ.

**Why this priority**: Thực thi BR-M12-03; phụ thuộc feature 004 đã viết. Đặt P2 vì cần tiếp nhận và trả (User Story 1, 3) chạy trước.

**Independent Test**: Lập hồ sơ kết thúc cho A còn 2 đồ Đang giữ, 1 đồ Đang được sử dụng; kiểm tra điều kiện Chưa đạt kèm 3 đồ; trả dần; kiểm tra điều kiện Đạt khi đồ cuối được trả. Với B, được duyệt ngoại lệ khi còn 1 đồ; kiểm tra nhắc sau kết thúc và việc trả sau trạng thái cuối.

**Acceptance Scenarios**:

1. **Given** A có 2 đồ Đang giữ và 1 đồ Đang được người cao tuổi sử dụng, **When** hành chính lập hồ sơ kết thúc lưu trú, **Then** điều kiện (b) của feature 004 FR-063 là Chưa đạt với căn cứ "còn 3 đồ gửi: Điện thoại (Đang giữ), Tiền mặt (Đang giữ), Kính (Đang sử dụng)"; người đại diện của A nhận thông báo danh sách đồ cần nhận.
2. **Given** điều kiện (b) Chưa đạt, **When** hành chính thực hiện "Kết thúc lưu trú", **Then** lệnh bị từ chối và nêu điều kiện (b) cùng các điều kiện chưa đạt khác (feature 004 FR-065).
3. **Given** H lần lượt trả 3 đồ cho R, **When** đồ cuối chuyển Đã trả, **Then** điều kiện (b) tự chuyển Đạt, không cần thao tác thủ công.
4. **Given** A còn 1 kính Hư hỏng và 1 điện thoại Thất lạc (sự cố đang mở), mọi đồ khác Đã trả, **When** hệ thống cập nhật điều kiện (b), **Then** điều kiện Chưa đạt với căn cứ "còn 1 đồ gửi: Kính (Hư hỏng)"; điện thoại Thất lạc không được tính vào điều kiện (b) (sự cố của nó thuộc điều kiện (d) của feature 004) (Q-148). **When** H trả kính hỏng cho R, **Then** điều kiện (b) chuyển Đạt.
5. **Given** B còn 1 bộ quần áo cũ Đang giữ và Quản lý viện duyệt ngoại lệ điều kiện (b), **When** B Kết thúc lưu trú, **Then** đồ vẫn ở Đang giữ; hệ thống nhắc hành chính theo chu kỳ CFG-M12-02 cho tới khi đồ được trả; H vẫn trả được đồ dù hồ sơ B ở trạng thái cuối (FR-027).
6. **Given** C qua đời khi còn đồ Đang giữ, **When** lệnh "Ghi nhận qua đời" chạy, **Then** lệnh không bị chặn; mục "xử lý đồ gửi" của danh sách việc sau qua đời là Chưa hoàn thành kèm danh sách đồ; khi đồ cuối được trả cho người có quyền, mục tự chuyển Hoàn thành; C không còn đồ nào thì mục là Không áp dụng.
7. **Given** đồ đang Đang được người cao tuổi sử dụng khi C qua đời, **When** D thu lại để trả gia đình, **Then** bản ghi "Thu lại" ghi người giao là "người cao tuổi (đã qua đời)" và người nhận là D.
8. **Given** B Kết thúc lưu trú ngày 01/03 với ngoại lệ, còn 1 bộ quần áo cũ Đang giữ và không ai đến nhận, **When** H lập đề nghị "Xử lý đồ không người nhận" ngày 15/03, **Then** hệ thống chặn vì chưa đủ CFG-M12-03 (\[90 ngày\]); **When** H lập đề nghị ngày 01/06 với hình thức Thanh lý, bằng chứng liên hệ và biên bản, và Quản lý viện duyệt kèm lý do, **Then** đồ chuyển Đã xử lý với bản ghi "Xử lý" tham chiếu đề nghị, và người đại diện của B được báo (FR-027a, Q-152).
9. **Given** đề nghị xử lý của B đang Chờ duyệt, **When** người đại diện R đến nhận đồ và H trả cho R, **Then** lệnh Trả được thực hiện; khi Quản lý viện duyệt, đồ đó bị loại khỏi phần áp dụng và đề nghị chuyển Áp dụng không thành nếu không còn đồ nào (feature 000 FR-038).
10. **Given** E đang ở Đang tiếp nhận và đã gửi 1 điện thoại, **When** hồ sơ E chuyển Hủy tiếp nhận, **Then** lệnh không bị chặn; điện thoại vẫn Đang giữ; Hành chính được nhắc theo CFG-M12-02; H trả được điện thoại cho người có quyền nhận dù hồ sơ ở trạng thái cuối; nếu không ai nhận, đề nghị xử lý được lập từ ngày thứ 90 sau Hủy tiếp nhận (FR-027, FR-027a).
11. **Given** mục "xử lý đồ gửi" của C (đã qua đời) đã Hoàn thành vì đồ còn lại là 1 điện thoại Thất lạc, và hồ sơ lưu trú đã Đã đóng, **When** D tìm thấy điện thoại, **Then** mục và hồ sơ không mở lại; điện thoại Đang giữ được nhắc theo CFG-M12-02 và CFG-M12-03 tính từ thời điểm Tìm thấy (FR-026a).

---

### User Story 6 - Người thân xem đồ gửi và lịch sử bàn giao trên cổng (Priority: P3)

Người có quyền nhận (người đại diện, người thân có quyền "được phép đón" đang hiệu lực) xem trên cổng danh sách đồ gửi của người cao tuổi, trạng thái hiện tại, lịch sử bàn giao (người giao, người nhận, thời điểm, số lượng, tình trạng) và ảnh; người thân khác không thấy đồ gửi (Q-151). Người đại diện còn thấy và xử lý các yêu cầu xác nhận người nhận khác (User Story 3).

**Why this priority**: Tăng minh bạch, giảm tranh chấp (4.4 dòng "Đồ gửi": NT X). Không chặn luồng vận hành.

**Independent Test**: Với A có 3 đồ gửi, đăng nhập cổng bằng người đại diện R, người thân M có quyền đón và người thân N không có quyền đón; kiểm tra R, M thấy danh sách và lịch sử, N không thấy; chỉ R thấy yêu cầu xác nhận; tắt quyền đón của M và kiểm tra M mất quyền xem ngay.

**Acceptance Scenarios**:

1. **Given** A có 3 đồ gửi, **When** người thân M có quyền "được phép đón" đang hiệu lực mở cổng, **Then** M thấy 3 đồ, trạng thái, lịch sử bàn giao và ảnh; M không có thao tác thay đổi.
2. **Given** N là người thân có quan hệ Hiệu lực nhưng không phải người đại diện và không có quyền đón, **When** N mở cổng, **Then** N không thấy mục đồ gửi; **When** quyền đón của M chuyển tắt, **Then** M không còn thấy đồ gửi từ thời điểm đó (Q-151).
3. **Given** có yêu cầu xác nhận người nhận khác đang chờ, **When** R (người đại diện) mở cổng, **Then** R thấy yêu cầu với người nhận, đồ gửi, người lập và có thao tác Xác nhận / Từ chối (kèm lý do).
4. **Given** trưởng tầng T của tầng 2, **When** T xem đồ gửi, **Then** T xem được đồ gửi của người cao tuổi có giường ở tầng 2, không có thao tác ghi (4.4 TT X).

---

### Edge Cases

- **Hủy tiếp nhận khi còn đồ gửi**: lệnh Hủy tiếp nhận (feature 001) không bị chặn; đồ gửi còn lại được nhắc hành chính theo CFG-M12-02 như sau kết thúc lưu trú có ngoại lệ (FR-027).
- **Người cao tuổi chuyển tầng** (feature 003): đồ Đang giữ ở vị trí gắn tầng cũ được liệt kê cho hành chính và điều dưỡng phụ trách để chuyển giữ; trạng thái không tự đổi (FR-028).
- **Người cao tuổi Tạm vắng, Hoạt động bên ngoài, Điều trị tại bệnh viện mang đồ theo**: đồ vẫn ở Đang được người cao tuổi sử dụng; không cần bàn giao thêm. Nếu người thân mang đồ về nhà thì là lần "Trả" cho người có quyền.
- **Số lượng thực tế khi bàn giao ít hơn số lượng ghi**: người nhận không được ghi số lượng nhỏ hơn mà không xử lý phần thiếu; hệ thống tách phần thiếu thành đồ gửi riêng và yêu cầu "Báo thất lạc" cho phần đó trong cùng lệnh (FR-013).
- **Vị trí lưu giữ bị ngừng hiệu lực khi còn đồ**: bị chặn cho tới khi mọi đồ ở vị trí đó được chuyển giữ (FR-002).
- **Loại đồ được thêm vào CFG-M12-01 sau khi đồ đã được tiếp nhận không có ảnh**: đồ cũ không bị chặn hồi tố; ảnh bắt buộc từ lần trả kế tiếp (FR-009).
- **Người thân có quyền đón bị gỡ quyền trong lúc đang ở quầy nhận đồ**: quyền được kiểm tra tại thời điểm lưu lệnh "Trả"; mất quyền thì bị chặn (feature 012 BR-M10-07 "bỏ người được phép đón có hiệu lực ngay").
- **Tiền mặt**: số lượng là số tiền (đồng); trả một phần tiền mặt được xử lý như tách đồ gửi, tới đơn vị đồng (FR-012). Ngoại tệ, vàng miếng, giấy tờ có giá ghi là "tài sản khác" (FR-003).
- **Đồ mất khi người cao tuổi Hoạt động bên ngoài**: sự cố vẫn có nguồn "đồ gửi", địa điểm ghi nơi xảy ra (FR-021).
- **Đồ gửi hỏng được phát hiện khi vệ sinh phòng**: nhân viên vệ sinh không có quyền với đồ gửi và không thấy thông tin người cao tuổi ngoài phòng; hư hỏng được ghi ở kết quả vệ sinh (feature 003 FR-048) hoặc bằng sự cố ở feature 007, rồi Điều dưỡng hoặc Hành chính ghi "Ghi hư hỏng" và liên kết sự cố đó (FR-021). Hư hỏng thiết bị của viện chỉ đi theo feature 003. Việc dùng tiền gửi để mua hộ không thuộc spec này (đề nghị mua hộ, feature 010).
- **Đồ không ai nhận sau thời gian dài**: hệ thống nhắc theo CFG-M12-02; từ khi đủ CFG-M12-03 kể từ lúc hồ sơ ở trạng thái cuối, Hành chính lập được đề nghị "Xử lý đồ không người nhận"; đồ chỉ chuyển Đã xử lý khi Quản lý viện duyệt (FR-027a, Q-152). Nếu người có quyền nhận đến nhận trong lúc đề nghị Chờ duyệt, lệnh Trả vẫn thực hiện được và đề nghị chuyển Áp dụng không thành cho đồ đó (feature 000 FR-038).
- **Ghi nhận ngoại tuyến** (Q-01): tiếp nhận, bàn giao, trả đồ gửi phải trực tuyến, vì quyền của người nhận và xác nhận của người đại diện cần kiểm tra tức thời (FR-031).

## Requirements *(mandatory)*

### Functional Requirements

**Thuật ngữ của spec** (dùng thống nhất ở mọi FR, bảng, kịch bản, thông báo):

- **Đồ gửi**: một vật phẩm (hoặc một nhóm cùng loại có số lượng) của người cao tuổi do viện ghi nhận giữ hộ hoặc theo dõi.
- **Bản ghi bàn giao**: bản ghi nhóm 3 của một lần đồ gửi đổi người giữ, vị trí hoặc trạng thái; gồm cả lần tiếp nhận.
- **Người giữ**: người nhận ở bản ghi bàn giao gần nhất (nhân viên hoặc người cao tuổi).
- **Người có quyền nhận**: người đại diện, hoặc người thân có quyền "được phép đón" đang hiệu lực, của người cao tuổi tại thời điểm trả (BR-M12-04, 14.1).
- **Xác nhận người nhận khác**: xác nhận của người đại diện cho một người nhận cụ thể nhận các đồ gửi cụ thể (BR-M12-04).
- **Đồ có giá trị**: đồ thuộc loại đồ nằm trong CFG-M12-01.
- **Đang được người cao tuổi sử dụng**: trạng thái đồ do chính người cao tuổi giữ và dùng; trong căn cứ, danh sách và kịch bản có thể viết gọn là "Đang sử dụng".
- **Đồng ý cho tự giữ**: đồng ý của người đại diện cho người cao tuổi tự giữ một đồ gửi thuộc loại "cần đồng ý khi giao sử dụng" (FR-015a).
- **Đồ không rõ chủ**: đồ phát hiện khi kiểm kê mà không khớp đồ gửi nào (FR-028b); không phải đồ gửi.

Mọi FR của spec thuộc UC-65 "Tiếp nhận, bàn giao, trả đồ gửi"; nguồn ghi ở cuối từng FR là mã quy tắc hoặc quyết định cụ thể.

#### A. Danh mục

- **FR-001**: Hệ thống MUST quản lý danh mục loại đồ (nhóm 1) gồm tối thiểu các loại của 16.1 trừ thuốc gia đình gửi: điện thoại, kính, quần áo, xe lăn, giấy tờ, đồ dùng cá nhân, trang sức, tiền mặt, tài sản khác. Mỗi loại có dấu "đồ có giá trị" đọc từ CFG-M12-01, dấu "có số tiền" (tiền mặt) và dấu "cần đồng ý khi giao sử dụng" (mặc định bật cho tiền mặt, trang sức; FR-015a, Q-155). Quản lý viện tạo, sửa, ngừng hiệu lực loại đồ; loại đã được đồ gửi tham chiếu MUST NOT bị xóa. Việc bật, tắt dấu "cần đồng ý khi giao sử dụng" MUST có lý do và được lưu lịch sử, vì dấu này quyết định quyền giao tài sản. *(Nguồn: 16.1, 1.5 nhóm 1, CFG-M12-01)*
- **FR-001a**: Hệ thống MUST quản lý danh mục mức tình trạng (nhóm 1) dùng ở FR-015b, có thứ tự từ tốt tới xấu và mức cuối nghĩa là "không dùng được"; mặc định 4 mức: Tốt / Có dấu hiệu sử dụng / Hư hỏng một phần / Không dùng được. Quản lý viện cấu hình; mức đã được bản ghi tham chiếu chỉ ngừng hiệu lực, không xóa. *(Suy ra từ 16.2, 16.3 "tình trạng"; Constitution V; Q-156)*
- **FR-002**: Hệ thống MUST quản lý danh mục vị trí lưu giữ (nhóm 1): tên, mô tả, tầng/khu vực (nếu vị trí đặt tại tầng), trạng thái. Hành chính và Quản lý viện tạo, sửa; vị trí còn đồ Đang giữ hoặc Hư hỏng MUST NOT được ngừng hiệu lực. *(Nguồn: 16.2 "vị trí lưu giữ", 1.5 nhóm 1)*

#### B. Tiếp nhận

- **FR-003**: Lệnh "Tiếp nhận đồ gửi" MUST ghi: người cao tuổi, loại đồ, mô tả, số lượng (với tiền mặt: số tiền bằng đồng; ngoại tệ, vàng miếng, giấy tờ có giá ghi là "tài sản khác" với số lượng theo đơn vị đếm và mô tả mệnh giá), mức tình trạng (FR-015b) kèm mô tả, người giao (người thân có quan hệ Hiệu lực, người khác ghi họ tên và giấy tờ, hoặc chính người cao tuổi), người nhận (người thực hiện lệnh), thời điểm, vị trí lưu giữ đang hiệu lực, hình ảnh (nếu có), ghi chú. *(Nguồn: 16.2)*
- **FR-004**: Tiếp nhận MUST tạo đồ gửi ở trạng thái Đang giữ và đúng một bản ghi bàn giao loại "Tiếp nhận" trong cùng một lần; mỗi đồ gửi MUST có đúng một bản ghi "Tiếp nhận", trừ đồ gửi sinh ra do tách (FR-012) trỏ về bản ghi tiếp nhận của đồ gốc. *(Nguồn: DBR-22)*
- **FR-005**: Lệnh tiếp nhận MAY kèm lựa chọn "giao ngay cho người cao tuổi sử dụng"; khi đó hệ thống MUST tạo thêm bản ghi "Giao sử dụng" ngay sau bản ghi "Tiếp nhận" trong cùng một lần, và vị trí lưu giữ không bắt buộc. Với loại đồ có dấu "cần đồng ý khi giao sử dụng" (FR-015a), lựa chọn này MUST chỉ được dùng khi người giao là người đại diện và người đó ký đồng ý cho tự giữ ngay trong lệnh tiếp nhận (bản ký gắn với đồ gửi được tạo); các trường hợp khác đồ MUST vào Đang giữ trước, rồi Giao sử dụng sau khi có đồng ý. *(Suy ra từ 16.4 "(Bổ sung)"; Q-155)*
- **FR-006**: Tiếp nhận MUST chỉ được thực hiện khi người cao tuổi ở trạng thái Đang tiếp nhận, Đang lưu trú, Tạm vắng, Hoạt động bên ngoài hoặc Điều trị tại bệnh viện. *(Suy ra từ 5.5, BR-M01-05)*
- **FR-007**: Người thực hiện tiếp nhận MUST là Hành chính hoặc Điều dưỡng; Điều dưỡng chỉ tiếp nhận cho người cao tuổi trong phạm vi dữ liệu của mình (feature 002). *(Nguồn: 4.4 dòng "Đồ gửi", UC-65)*

#### C. Hình ảnh đồ có giá trị

- **FR-008**: Với đồ có giá trị, lệnh "Tiếp nhận", "Trả" và "Tìm thấy" MUST có ít nhất một hình ảnh; lệnh "Ghi hư hỏng" MUST có ít nhất một hình ảnh với mọi loại đồ. Thiếu ảnh thì lệnh bị chặn. *(Nguồn: BR-M12-05; lệnh Ghi hư hỏng: suy ra từ BR-M12-02, cần bằng chứng cho sự cố)*
- **FR-009**: Dấu "đồ có giá trị" MUST được xét theo CFG-M12-01 tại thời điểm thực hiện lệnh; thay đổi CFG-M12-01 MUST NOT làm không hợp lệ bản ghi cũ. *(Suy ra từ BR-M12-05, feature 000 FR-011)*
- **FR-010**: Hình ảnh gắn bản ghi bàn giao MUST không xóa, không thay được; ảnh bổ sung chỉ được thêm bằng bản đính chính. *(Nguồn: 1.5 nhóm 3)*

#### D. Bàn giao và trạng thái

- **FR-011**: Mỗi lệnh ở bảng trạng thái dưới đây MUST tạo đúng một bản ghi bàn giao gồm: đồ gửi, loại bàn giao, người giao, người nhận, thời điểm, số lượng, tình trạng, vị trí lưu giữ (bắt buộc khi trạng thái đích là Đang giữ hoặc Hư hỏng), hình ảnh, lý do (bắt buộc với Chuyển giữ, Báo thất lạc, Ghi hư hỏng), người ghi nhận. Bản ghi bàn giao MUST NOT sửa, xóa; sai sót xử lý bằng đính chính theo feature 000 FR-026 (c) (bản ghi không gắn tầng). Trạng thái hiện tại của đồ gửi MUST luôn bằng trạng thái đích của bản ghi bàn giao hiện hành gần nhất (sau đính chính). *(Nguồn: 16.3, BR-M12-01, DBR-22, BR-M15-03, 1.5)*
- **FR-011a**: Bản đính chính MUST NOT đổi loại bàn giao hay trạng thái đích của bản ghi gốc; chỉ sửa được các trường nội dung (số lượng, tình trạng, vị trí, người giao, người nhận, thời điểm, ảnh bổ sung). Bản đính chính loại "Hủy ghi nhận" MUST chỉ áp cho bản ghi bàn giao gần nhất của đồ gửi (không áp cho bản ghi "Tiếp nhận" khi đồ đã có bản ghi sau). Khi "Hủy ghi nhận" có hiệu lực, trong cùng một lần: trạng thái, người giữ, vị trí của đồ gửi MUST được tính lại theo bản ghi hiện hành liền trước; nếu bản ghi bị hủy đã tạo sự cố, hệ thống MUST yêu cầu feature 007 chuyển sự cố đó Đã hủy với lý do "ghi nhầm đồ gửi"; điều kiện (b) và mục "xử lý đồ gửi" MUST được tính lại (FR-025, FR-026); người đã nhận thông báo về sự kiện bị hủy MUST được báo đính chính. Hủy ghi nhận "Tiếp nhận" của đồ chưa có bản ghi sau làm đồ gửi bị loại khỏi mọi danh sách, bản gốc vẫn giữ. Mọi bản ghi bàn giao, kể cả do Điều dưỡng ghi tại tầng, là bản ghi không gắn tầng theo 1.5 và được đính chính theo feature 000 FR-026 (c); các giới hạn ở FR-011a là thu hẹp mà feature 000 FR-026 cho phép module sở hữu đặt thêm. *(Suy ra từ feature 000 FR-026 → FR-030; feature 007 Clarification "sự cố hủy"; Q-160)*
- **FR-011b**: Mỗi lệnh MUST được xét trên trạng thái và phiên bản hiện hành của đồ gửi tại thời điểm lưu; lệnh được lập dựa trên trạng thái đã cũ (ví dụ hai nhân viên cùng thao tác trên một đồ) MUST bị từ chối và người thực hiện MUST được hiển thị trạng thái mới. *(Suy ra từ feature 000 FR-021)*
- **FR-012**: Khi lệnh bàn giao áp cho số lượng nhỏ hơn số lượng hiện có, hệ thống MUST tách đồ gửi: phần được bàn giao thành đồ gửi mới mang trạng thái đích, phần còn lại giữ trạng thái cũ; cả hai trỏ về đồ gốc và lần tiếp nhận gốc. Tổng số lượng của các đồ tách từ cùng một lần tiếp nhận MUST bằng số lượng tiếp nhận (sau đính chính). *(Suy ra từ 16.3 "Số lượng")*
- **FR-013**: Khi người nhận đếm được số lượng ít hơn số lượng hiện có của đồ gửi, lệnh bàn giao MUST tách phần thiếu và ghi "Báo thất lạc" cho phần đó trong cùng lần; hệ thống MUST NOT cho lưu số lượng nhỏ hơn mà không xử lý phần thiếu. *(Suy ra từ BR-M12-01, BR-M12-02)*
- **FR-014**: Lệnh bàn giao MUST bị chặn khi người giao ghi trên lệnh không phải người giữ hiện tại, trừ các trường hợp: lệnh Trả từ Đang được người cao tuổi sử dụng (FR-018); lệnh Báo thất lạc; lệnh Tìm thấy (người giao để trống, Q-148); và lệnh do Hành chính thực hiện với lý do "người giữ vắng mặt". "Người giữ vắng mặt" MUST chỉ được chọn khi người giữ là nhân viên và tại thời điểm lệnh không có ca đang diễn ra, hoặc tài khoản không ở trạng thái Hoạt động (feature 002, 008); khi đó lý do bắt buộc và người giữ được báo. *(Suy ra từ 16.3 "Ai giao → Ai nhận")*
- **FR-014a**: Khi ca của một nhân viên kết thúc mà nhân viên đó còn là người giữ đồ có giá trị ở Đang giữ, hệ thống MUST đưa danh sách các đồ đó vào bản nháp bàn giao ca của tầng/khu vực (feature 008) như một mục "đồ gửi đang giữ" và nhắc nhân viên Chuyển giữ cho Hành chính hoặc nhân viên ca sau. Người giữ không tự đổi khi bàn giao ca được xác nhận; chỉ đổi qua lệnh Chuyển giữ. *(Suy ra từ 1.3 "bàn giao ca", 16.3; Q-158)*
- **FR-015**: Khi mức tình trạng ghi ở lệnh thấp hơn mức của bản ghi gần nhất, hệ thống MUST hiển thị chênh lệch và yêu cầu người ghi chọn: ghi nhận tình trạng mới (không tạo sự cố) hoặc thực hiện "Ghi hư hỏng". Mức "Không dùng được" MUST chỉ được ghi qua lệnh "Ghi hư hỏng", trừ ở lệnh Tiếp nhận (hỏng từ trước khi viện nhận). *(Suy ra từ BR-M12-02)*
- **FR-015b**: Mọi bản ghi bàn giao và kết quả kiểm kê MUST ghi tình trạng bằng một mức trong danh mục mức tình trạng (FR-001a), kèm mô tả tự do. "Không dùng được" ở FR-015 là mức cuối của danh mục. *(Suy ra từ 16.2, 16.3 "tình trạng")*
- **FR-015a**: Lệnh "Giao sử dụng" (kể cả "Tiếp nhận và giao ngay") cho đồ thuộc loại có dấu "cần đồng ý khi giao sử dụng" MUST bị chặn trừ khi: (a) có **đồng ý cho tự giữ** Hiệu lực của người đại diện cho đúng đồ gửi đó, ghi qua cổng người thân hoặc bằng bản ký scan do Hành chính ghi nhận; và (b) người cao tuổi không có cờ nguy cơ đi lạc tại thời điểm lệnh. Đồng ý cho tự giữ có hiệu lực cho mọi lần Giao sử dụng đồ đó tới khi người đại diện rút lại, đồ chuyển Đã trả, Thất lạc, Hư hỏng hoặc Đã xử lý; người xác nhận MUST là người đại diện tại thời điểm đồng ý. Đồng ý vẫn hiệu lực khi người đã đồng ý thôi làm người đại diện; mọi người đại diện hiện tại MUST rút được đồng ý đó. Khi Quản lý viện bật dấu "cần đồng ý khi giao sử dụng" cho một loại đồ, dấu áp cho lần Giao sử dụng sau; đồ loại đó đang Đang được người cao tuổi sử dụng mà chưa có đồng ý MUST được liệt kê và báo như trường hợp rút đồng ý dưới đây. Khi đồng ý bị rút, hoặc người cao tuổi được gắn cờ nguy cơ đi lạc, trong lúc đồ đang Đang được người cao tuổi sử dụng, hệ thống MUST báo Hành chính và điều dưỡng phụ trách để "Thu lại"; trạng thái không tự đổi. Điện thoại và các loại đồ có giá trị khác không có dấu này giao sử dụng không cần đồng ý. *(Clarification 2026-09-27, Q-155)*

**Bảng trạng thái đồ gửi** *(16.4 "(Bổ sung)", BR-M12-01 → 04; quyền theo 4.4 dòng "Đồ gửi")*:

| Trạng thái hiện tại | Lệnh | Trạng thái đích | Ai được thực hiện | Điều kiện (chặn nếu không đạt) | Tác động tự động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Tiếp nhận | Đang giữ | Hành chính, Điều dưỡng (trong phạm vi) | FR-003, FR-006, FR-008 | Bản ghi "Tiếp nhận"; báo người đại diện nếu là đồ có giá trị |
| (chưa có) | Tiếp nhận và giao ngay | Đang được người cao tuổi sử dụng | Hành chính, Điều dưỡng (trong phạm vi) | Như trên; với tiền mặt, trang sức: người giao là người đại diện và ký đồng ý cho tự giữ trong lệnh (FR-005, FR-015a) | Hai bản ghi "Tiếp nhận", "Giao sử dụng" |
| Đang giữ | Giao sử dụng | Đang được người cao tuổi sử dụng | Hành chính, Điều dưỡng (trong phạm vi) | Người cao tuổi chưa ở trạng thái cuối; với tiền mặt, trang sức: có đồng ý cho tự giữ, không có cờ nguy cơ đi lạc (FR-015a) | Bản ghi (người giữ → người cao tuổi) |
| Đang được người cao tuổi sử dụng | Thu lại | Đang giữ | Hành chính, Điều dưỡng (trong phạm vi) | Có vị trí lưu giữ | Bản ghi (người cao tuổi → người thực hiện) |
| Đang giữ | Chuyển giữ | Đang giữ | Hành chính, Điều dưỡng (trong phạm vi) | Có vị trí mới hoặc người giữ mới; có lý do | Bản ghi (người giữ cũ → người giữ mới) |
| Đang giữ; Đang được người cao tuổi sử dụng; Hư hỏng (Q-148) | Trả | Đã trả | Hành chính; Điều dưỡng (trong phạm vi) chỉ với đồ không có giá trị và người nhận là người có quyền nhận hoặc chính người cao tuổi thuộc ngoại lệ FR-016 (FR-016a, Q-153, Q-258) | Người nhận theo FR-016 → FR-018; ảnh theo FR-008; người nhận ký xác nhận | Cập nhật điều kiện kết thúc, danh sách việc sau qua đời (FR-025, FR-026); báo người đại diện |
| Đang giữ; Đang được người cao tuổi sử dụng | Báo thất lạc | Thất lạc | Hành chính, Điều dưỡng (trong phạm vi) | Có mô tả, thời điểm phát hiện, lý do | Yêu cầu feature 007 tạo sự cố (FR-021); báo người liên hệ chính |
| Đang giữ; Đang được người cao tuổi sử dụng | Ghi hư hỏng | Hư hỏng | Hành chính, Điều dưỡng (trong phạm vi) | Có mô tả, ảnh, vị trí cất đồ hỏng, lý do | Yêu cầu feature 007 tạo sự cố (FR-021); báo người liên hệ chính |
| Thất lạc | Tìm thấy | Đang giữ | Hành chính, Điều dưỡng (trong phạm vi) | Có vị trí lưu giữ, tình trạng, số lượng tìm thấy (Q-148) | Bản ghi (người ghi nhận là người nhận); yêu cầu feature 007 ghi diễn biến "đã tìm thấy" vào sự cố; báo người liên hệ chính; cập nhật điều kiện (b) (FR-025) |
| Hư hỏng | Chuyển giữ | Hư hỏng | Hành chính, Điều dưỡng (trong phạm vi) | Có vị trí mới hoặc người giữ mới; có lý do | Bản ghi (người giữ cũ → người giữ mới) |
| Hư hỏng | Báo thất lạc | Thất lạc | Hành chính, Điều dưỡng (trong phạm vi) | Như lệnh Báo thất lạc | Như lệnh Báo thất lạc (sự cố mới) |
| Đang giữ; Hư hỏng | Xử lý đồ không người nhận (áp dụng đề nghị đã duyệt) | Đã xử lý | Hành chính lập đề nghị; Quản lý viện duyệt | FR-027a (Q-152) | Bản ghi (người giữ → hình thức xử lý, đơn vị tiếp nhận nếu có); cập nhật mục "xử lý đồ gửi" (FR-026); báo người đại diện |
| Đã trả | — | — | — | Trạng thái cuối | — |
| Đã xử lý | — | — | — | Trạng thái cuối | — |

Thất lạc và Hư hỏng không phải trạng thái cuối (Q-148). Đồ Thất lạc tìm thấy một phần được xử lý bằng tách đồ gửi (FR-012): phần tìm thấy chuyển Đang giữ, phần còn lại giữ Thất lạc.

"Trong phạm vi" với Điều dưỡng: người cao tuổi thuộc phạm vi dữ liệu của điều dưỡng theo feature 002. Các lệnh trên MUST thực hiện được cả khi hồ sơ người cao tuổi ở trạng thái cuối, trừ Tiếp nhận và Giao sử dụng (FR-027).

#### E. Trả lại cho người có quyền

- **FR-016**: Lệnh "Trả" MUST kiểm tra người nhận tại thời điểm lưu: người có quyền nhận thì được trả sau khi nhân viên xác minh danh tính theo cùng quy tắc của quy trình đón (giấy tờ tùy thân xuất trình khớp loại và số đã đăng ký ở hồ sơ người thân, feature 012 FR-001; không khớp thì chặn và ghi lần chặn; nếu hồ sơ người thân chưa có giấy tờ, như với người đại diện không có quyền đón, nhân viên MUST ghi loại, số giấy tờ xuất trình và đối chiếu họ tên, số điện thoại với hồ sơ, và Hành chính được nhắc bổ sung giấy tờ vào hồ sơ người thân); người khác, kể cả chính người cao tuổi (Q-149), chỉ được trả khi có xác nhận người nhận khác còn hiệu lực, khớp người nhận và bao gồm đồ gửi đang trả. **(Sửa, 2026-09-30, Q-253)** Ngoại lệ: người nhận là chính người cao tuổi thì MUST trả được không cần xác nhận người nhận khác khi, tại thời điểm lưu, người cao tuổi không có cờ nguy cơ đi lạc (feature 001) và đồ không thuộc loại "cần đồng ý khi giao sử dụng"; nếu không thỏa thì áp quy tắc chung ở trên. *(Nguồn: 16.4, BR-M12-04, 14.3; Clarification 2026-09-27; Q-253)*
- **FR-016a**: Hành chính MUST thực hiện được lệnh "Trả" cho mọi đồ gửi. Điều dưỡng (trong phạm vi) MUST chỉ thực hiện được lệnh "Trả" khi mọi đồ trong lần trả không phải đồ có giá trị (CFG-M12-01 tại thời điểm lệnh) và người nhận là người có quyền nhận, hoặc (**bổ sung, 2026-10-01, Q-258**) là chính người cao tuổi thuộc ngoại lệ của FR-016 (Q-253); trả theo xác nhận người nhận khác (kể cả cho chính người cao tuổi khi không thuộc ngoại lệ đó) và lập yêu cầu xác nhận người nhận khác chỉ do Hành chính thực hiện. Lần trả có lẫn đồ có giá trị do Điều dưỡng thực hiện MUST bị chặn toàn bộ, không trả một phần. *(Clarification 2026-09-27, Q-153; 14.3, Q-121)*
- **FR-017**: Xác nhận người nhận khác MUST gồm: người cao tuổi, người nhận (họ tên, giấy tờ), danh sách đồ gửi, người lập (Hành chính, Q-153), người xác nhận (người đại diện, hoặc Quản lý viện theo FR-017a), cách xác nhận (qua cổng người thân, bản ký scan do Hành chính ghi nhận, hoặc duyệt của Quản lý viện), thời điểm. Xác nhận có trạng thái Chờ xác nhận → Hiệu lực / Từ chối (bắt buộc lý do) / Hủy / Đã dùng / Hết hạn; hiệu lực trong CFG-M10-08 kể từ khi xác nhận và dùng cho đúng một lần trả (có thể nhiều đồ trong danh sách). Người đại diện xác nhận MUST là người đại diện tại thời điểm xác nhận. Người đại diện đã từ chối thì Quản lý viện MUST NOT duyệt thay yêu cầu đó. Khi tài khoản cổng của người đại diện đã bị khóa (ví dụ sau kết thúc lưu trú hoặc qua đời, feature 001 CFG-M01-04), xác nhận MUST ghi được bằng bản ký scan; quan hệ người đại diện vẫn được xét theo hồ sơ người thân, không theo trạng thái tài khoản. *(Nguồn: BR-M12-04; thời hạn: suy ra từ 14.3 "Ngoại lệ đón")*
- **FR-017a**: Quản lý viện MAY duyệt yêu cầu xác nhận người nhận khác thay người đại diện chỉ khi: (a) yêu cầu đã Chờ xác nhận quá CFG-M12-05 (đề xuất, mặc định \[4 giờ\]) mà không người đại diện nào phản hồi, và Hành chính đã ghi lý do "không liên hệ được người đại diện" kèm bằng chứng liên hệ tối thiểu theo CFG-M12-06 (đề xuất, mặc định \[với mỗi người đại diện Hiệu lực, 2 lần qua 2 kênh\]; kênh là gọi điện, tin nhắn, thông báo cổng; Q-157), mỗi lần có thời điểm và kết quả; hoặc (b) người cao tuổi không có người đại diện Hiệu lực, tức người đại diện duy nhất đã qua đời, đã thôi hoặc quan hệ đã kết thúc mà chưa có người thay (trường hợp tạm thời vi phạm 14.1 "ít nhất một người đại diện"; Hành chính MUST ghi căn cứ). Duyệt MUST có lý do; mọi người đại diện Hiệu lực (nếu có) MUST được báo. Quản lý viện MUST NOT tự lập rồi tự duyệt yêu cầu. *(Clarification 2026-09-27, Q-150; tương tự BR-M10-03)*
- **FR-017b**: Xác nhận người nhận khác (FR-017) và đồng ý cho tự giữ (FR-015a) có vòng đời riêng theo hai bảng dưới, không đi qua vòng đời phê duyệt chung của feature 000 FR-031, giống cách feature 012 FR-020 làm với yêu cầu BR-M10-07; tên trạng thái chung (Chờ xác nhận, Hiệu lực, Từ chối, Hủy) có cùng nghĩa với feature 012 FR-020. Cả hai là nhóm 2: chỉ đổi qua lệnh trong bảng, nội dung (người nhận, danh sách đồ) khóa từ khi lập. *(Suy ra từ BR-M12-04, Q-155; feature 012 FR-020)*

**Bảng trạng thái xác nhận người nhận khác** *(BR-M12-04, Q-149, Q-150)*:

| Trạng thái hiện tại | Lệnh | Trạng thái đích | Ai được thực hiện | Điều kiện (chặn nếu không đạt) | Tác động tự động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Lập yêu cầu | Chờ xác nhận | Hành chính | Người nhận không phải người có quyền nhận; danh sách đồ ở Đang giữ, Đang sử dụng hoặc Hư hỏng | Báo người đại diện (FR-032) |
| (chưa có) | Lập kèm bản ký của người đại diện | Hiệu lực | Hành chính | Như trên; bản ký scan của người là người đại diện tại thời điểm ký | Bắt đầu tính CFG-M10-08 |
| Chờ xác nhận | Xác nhận | Hiệu lực | Người đại diện (qua cổng) | Là người đại diện tại thời điểm xác nhận | Báo người lập; bắt đầu tính CFG-M10-08 |
| Chờ xác nhận | Từ chối | Từ chối | Người đại diện | Có lý do | Báo người lập; Quản lý viện không duyệt thay được |
| Chờ xác nhận | Duyệt thay | Hiệu lực | Quản lý viện | FR-017a (a) hoặc (b); có lý do; không phải người lập | Báo mọi người đại diện Hiệu lực |
| Chờ xác nhận, Hiệu lực | Hủy | Hủy | Hành chính | Có lý do | — |
| Hiệu lực | Dùng cho lần Trả | Đã dùng | Hệ thống (khi lệnh Trả lưu) | Lệnh Trả khớp người nhận, đồ thuộc danh sách | — |
| Hiệu lực | Hết CFG-M10-08 | Hết hạn | Hệ thống | Chưa dùng | Báo người lập |

Từ chối, Hủy, Đã dùng, Hết hạn là trạng thái cuối. Đồ trong danh sách rời Đang giữ, Đang sử dụng, Hư hỏng trước khi dùng thì bị loại khỏi danh sách; hết đồ thì yêu cầu tự chuyển Hủy với lý do "không còn đồ".

**Bảng trạng thái đồng ý cho tự giữ** *(Q-155)*:

| Trạng thái hiện tại | Lệnh | Trạng thái đích | Ai được thực hiện | Điều kiện (chặn nếu không đạt) | Tác động tự động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Ghi đồng ý | Hiệu lực | Người đại diện (qua cổng); Hành chính (ghi bản ký); Hành chính, Điều dưỡng khi người đại diện ký trong lệnh tiếp nhận (FR-005) | Đồ thuộc loại "cần đồng ý khi giao sử dụng"; người ký là người đại diện tại thời điểm ký | Báo người đại diện khác, Hành chính |
| Hiệu lực | Rút đồng ý | Đã rút | Mọi người đại diện hiện tại (qua cổng hoặc bản ký do Hành chính ghi) | Có lý do | Nếu đồ đang Đang sử dụng: báo thu lại (FR-015a) |
| Hiệu lực | Đồ chuyển Đã trả, Thất lạc, Hư hỏng, Đã xử lý | Hết hiệu lực | Hệ thống | — | — |

Đã rút và Hết hiệu lực là trạng thái cuối; đồ được tìm thấy sau Thất lạc cần đồng ý mới.
- **FR-018**: Lệnh "Trả" từ trạng thái Đang được người cao tuổi sử dụng MUST ghi người bàn giao là nhân viên thực hiện và ghi chú "thu từ người cao tuổi". Khi người nhận là chính người cao tuổi, lệnh "Trả" MUST có xác nhận người nhận khác theo FR-017 hoặc FR-017a, trừ ngoại lệ của FR-016 (Q-253); bản ghi ghi người nhận là người cao tuổi. Việc đưa đồ cho người cao tuổi dùng trong viện vẫn là lệnh "Giao sử dụng", không phải "Trả". *(Nguồn: 16.4 "người bàn giao"; Clarification 2026-09-27, Q-149)*
- **FR-019**: Lệnh "Trả" MUST có xác nhận của người nhận (chữ ký điện tử hoặc bản ký scan). Trả nhiều đồ cho cùng người nhận trong một lần MUST tạo mỗi đồ một bản ghi, dùng chung người nhận, thời điểm và chữ ký. Khi người nhận là người cao tuổi (FR-018), người cao tuổi ký nếu còn khả năng; nếu không, xác nhận người nhận khác của người đại diện thay cho chữ ký và nhân viên ghi lý do. Lệnh Giao sử dụng và Thu lại, nơi người cao tuổi là người nhận hoặc người giao, MUST NOT yêu cầu chữ ký của người cao tuổi; nhân viên thực hiện là người chứng. *(Nguồn: 16.4; suy ra từ 16.3)*
- **FR-020**: Lệnh "Trả" MUST ghi tình trạng lúc trả và MUST hiển thị cho người nhận tình trạng lúc tiếp nhận để đối chiếu; người nhận MAY ghi ý kiến không đồng ý về tình trạng, và ý kiến đó được lưu trong bản ghi. *(Suy ra từ 16.4 "tình trạng")*

#### F. Thất lạc, hư hỏng và sự cố

- **FR-021**: Khi đồ gửi chuyển Thất lạc hoặc Hư hỏng, trong cùng một lần hệ thống MUST yêu cầu feature 007 tạo đúng một sự cố: người cao tuổi, loại "đồ gửi thất lạc" hoặc "đồ gửi hư hỏng", nguồn phát sinh "đồ gửi", mức Trung bình, thời điểm phát hiện, mô tả, người phát hiện, tham chiếu đồ gửi và bản ghi bàn giao; và yêu cầu feature 009 thông báo người liên hệ chính (FR-032). Nếu yêu cầu tạo sự cố không thành, lệnh MUST không được lưu (feature 000, "hoặc toàn bộ, hoặc không"). Nếu nhân viên đã ghi sự cố về việc mất, hỏng này trực tiếp ở feature 007 (ví dụ khi mất kết nối, hoặc do Nhân viên chăm sóc phát hiện), lệnh MUST cho chọn liên kết sự cố đó thay vì tạo sự cố mới; khi đó chỉ gửi thông báo người liên hệ chính nếu sự cố chưa gửi. Sự cố luôn có nguồn phát sinh "đồ gửi", kể cả khi đồ mất lúc người cao tuổi Hoạt động bên ngoài; địa điểm ghi nơi xảy ra. *(Nguồn: BR-M12-02)*
- **FR-022**: Sự cố do FR-021 tạo MUST NOT kích hoạt yêu cầu đánh giá lại người cao tuổi "sau sự cố" của feature 007 FR-043 (b), vì không liên quan tình trạng sức khỏe; cần feature 007 bổ sung ngoại lệ này (điểm báo lại 3). *(Suy ra từ BR-M01-02 "sau sự cố" và mục đích BR-M12-02)*
- **FR-023**: Người ghi MAY chọn mức sự cố khác mức Trung bình theo quy tắc chọn mức của feature 007 FR-042 (có lý do); mức mặc định MUST là Trung bình. *(Nguồn: BR-M12-02, feature 007 FR-042)*
- **FR-024**: Mỗi lần đồ gửi chuyển vào Thất lạc hoặc Hư hỏng MUST tạo tối đa một sự cố; tách phần thiếu của nhiều đồ trong cùng một lệnh bàn giao MUST tạo một sự cố cho mỗi đồ gửi. *(Suy ra từ BR-M12-02)*

#### G. Kết thúc lưu trú, qua đời, hủy tiếp nhận

- **FR-025**: Hệ thống MUST cung cấp cho feature 004 điều kiện (b) của FR-063: Đạt khi người cao tuổi không còn đồ gửi ở Đang giữ, Đang được người cao tuổi sử dụng hoặc Hư hỏng; Chưa đạt kèm danh sách đồ (loại, mô tả, số lượng, trạng thái, vị trí). Đồ Thất lạc MUST NOT làm điều kiện (b) Chưa đạt (sự cố của nó được xét ở điều kiện (d) khi còn mở); khi đồ Thất lạc được tìm thấy, điều kiện MUST được xét lại. Căn cứ của điều kiện (b) MUST liệt kê riêng đồ Thất lạc có sự cố còn mở kèm ghi chú "chặn qua điều kiện (d)", vì sự cố đó vẫn chặn kết thúc lưu trú cho tới khi đóng hoặc được ngoại lệ (feature 004 FR-063 (d), FR-064); đây là hệ quả có chủ đích của Q-148. Điều kiện MUST được cập nhật sau mỗi lệnh làm đổi trạng thái đồ gửi (mục tiêu thời gian ở SC-006). Duyệt ngoại lệ do feature 004 FR-064 thực hiện. *(Nguồn: BR-M12-03, 5.6, 6.8; Clarification 2026-09-27, Q-148)*
- **FR-026**: Hệ thống MUST cập nhật mục "xử lý đồ gửi" của danh sách việc sau qua đời (feature 004 FR-071) theo cùng quy tắc của FR-025: Không áp dụng khi người cao tuổi chưa từng có đồ gửi chưa trả tại thời điểm qua đời, Hoàn thành khi mọi đồ ở Đã trả, Thất lạc hoặc Đã xử lý (Q-152). *(Nguồn: 6.8)*
- **FR-026a**: Khi một đồ Thất lạc được Tìm thấy sau khi mục "xử lý đồ gửi" đã Hoàn thành hoặc hồ sơ lưu trú đã "Đã đóng" (feature 004 FR-072), mục và hồ sơ MUST NOT mở lại; đồ tìm thấy MUST được theo dõi bằng nhắc CFG-M12-02 (FR-027) và xử lý bằng Trả hoặc FR-027a, với CFG-M12-03 tính từ thời điểm Tìm thấy. *(Suy ra từ feature 004 FR-072, Q-148)*
- **FR-027**: Khi hồ sơ người cao tuổi ở trạng thái cuối (Kết thúc lưu trú có ngoại lệ điều kiện (b), Qua đời, Hủy tiếp nhận) mà còn đồ ở Đang giữ, Đang được người cao tuổi sử dụng hoặc Hư hỏng, hệ thống MUST cho phép các lệnh Thu lại, Chuyển giữ, Trả, Báo thất lạc, Ghi hư hỏng, và MUST nhắc Hành chính theo chu kỳ CFG-M12-02 (đề xuất, mặc định \[7 ngày\]) cho tới khi hết đồ; với người đã qua đời, nhắc của feature 004 FR-072 (CFG-M02-09) thay thế nhắc này. Đây là ngoại lệ (3) của BR-M01-05 (việc kết thúc trên đối tượng của module khác). *(Nguồn: feature 004 FR-072; CFG-M12-02 đề xuất)*
- **FR-027a**: Khi hồ sơ người cao tuổi đã ở trạng thái cuối từ CFG-M12-03 (đề xuất, mặc định \[90 ngày\]) trở lên, Hành chính MAY lập đề nghị "Xử lý đồ không người nhận" (loại yêu cầu phê duyệt dùng chung, feature 000 FR-031) cho đồ ở Đang giữ hoặc Hư hỏng, gồm: danh sách đồ, hình thức xử lý (Thanh lý, Tiêu hủy, Chuyển cơ quan có thẩm quyền), đơn vị tiếp nhận (nếu có), lý do, bằng chứng liên hệ, biên bản (bản scan). Bằng chứng liên hệ tối thiểu theo CFG-M12-07 (đề xuất, mặc định \[2 lần, cách nhau không dưới CFG-M12-02]; Q-157), tới mọi người đại diện và người liên hệ chính còn quan hệ trong hồ sơ, mỗi lần có thời điểm, kênh, kết quả. Tiền mặt MUST chỉ có hình thức "Chuyển cơ quan có thẩm quyền" (Q-159); với Thanh lý, số tiền thu được MUST được ghi trong biên bản và xử lý ngoài hệ thống (không sinh chi phí hay khoản điều chỉnh). Đồ Đang được người cao tuổi sử dụng MUST được Thu lại trước khi đưa vào đề nghị; đồ Thất lạc không thuộc đề nghị và không được nhắc theo CFG-M12-02 (sự cố của nó theo feature 007). Đồ có giá trị MUST có ảnh trong đề nghị. Trước CFG-M12-03, hệ thống MUST chặn lập đề nghị. Chỉ Quản lý viện duyệt (bắt buộc lý do; không tự duyệt đề nghị do mình lập, là thu hẹp feature 000 FR-035 vì đây là quyết định về tài sản của người khác); khi áp dụng, mỗi đồ MUST chuyển Đã xử lý với một bản ghi bàn giao loại "Xử lý" tham chiếu đề nghị. Điều kiện áp dụng (feature 000 FR-038): đồ vẫn ở Đang giữ hoặc Hư hỏng; đồ đã được trả hoặc đổi trạng thái trong lúc chờ MUST bị loại khỏi phần áp dụng. Đề nghị được lập hay chưa, CFG-M12-02 vẫn nhắc cho tới khi đồ hết ở Đang giữ, Đang được người cao tuổi sử dụng, Hư hỏng. *(Clarification 2026-09-27, Q-152)*

#### H. Chuyển tầng

- **FR-028**: Khi feature 003 chuyển người cao tuổi sang giường ở tầng/khu vực khác, hệ thống MUST liệt kê đồ Đang giữ tại vị trí gắn tầng/khu vực cũ và thông báo Hành chính và điều dưỡng phụ trách tầng mới; trạng thái và vị trí đồ MUST NOT tự đổi. *(Suy ra từ 16.2 "vị trí lưu giữ", 7.4)*

#### H2. Kiểm kê định kỳ

- **FR-028a**: Theo chu kỳ CFG-M12-04 (đề xuất, mặc định \[hằng tháng, hạn hoàn thành 3 ngày\]), Bộ lập lịch hệ thống MUST sinh cho mỗi vị trí lưu giữ đang có đồ có giá trị ở trạng thái Đang giữ đúng một phiếu kiểm kê, gồm danh sách các đồ đó tại thời điểm sinh (loại, mô tả, số lượng hoặc số tiền, tình trạng gần nhất, ảnh); phiếu giao Hành chính. Chạy lại cho cùng kỳ MUST NOT tạo phiếu trùng. Đồ Đang được người cao tuổi sử dụng, Hư hỏng và đồ không có giá trị MUST NOT nằm trong phiếu. *(Clarification 2026-09-27, Q-154)*
- **FR-028b**: Phiếu kiểm kê có trạng thái Chờ kiểm kê → Hoàn thành (hoặc Đã hủy khi mọi đồ trong phiếu đã rời vị trí trước khi kiểm, có lý do). Hành chính ghi kết quả từng đồ: Đúng; Lệch tình trạng (ghi tình trạng mới, hoặc chuyển Ghi hư hỏng theo FR-015); Không tìm thấy. Hoàn thành phiếu MUST có kết quả cho mọi đồ còn Đang giữ tại vị trí đó; đồ Không tìm thấy MUST được ghi "Báo thất lạc" (FR-021) trong cùng lần hoàn thành; đồ đã đổi trạng thái hoặc vị trí sau khi phiếu sinh được tự loại khỏi phiếu với ghi chú. Kết quả Đúng MUST NOT sinh bản ghi bàn giao; phiếu đã Hoàn thành là nhóm 3. Khi phát hiện **thừa**: đồ có trong hệ thống nhưng đang ở trạng thái Thất lạc thì ghi lệnh "Tìm thấy"; số lượng thực tế nhiều hơn ghi nhận thì lập đính chính số lượng cho bản ghi tiếp nhận (FR-011a); đồ không xác định được chủ thì ghi vào phần "đồ không rõ chủ" của phiếu kèm ảnh, không tạo đồ gửi, và Quản lý viện được báo. *(Clarification 2026-09-27, Q-154)*
- **FR-028c**: Phiếu chưa Hoàn thành khi hết hạn MUST được nhắc Hành chính mỗi ngày cho tới khi Hoàn thành (khóa sự kiện theo phiếu và ngày, feature 009 FR-005), và Quản lý viện được báo một lần khi hết hạn; phiếu vẫn làm được sau hạn và mang dấu "trễ hạn". *(Clarification 2026-09-27, Q-154)*

**Bảng trạng thái phiếu kiểm kê** *(Q-154)*:

| Trạng thái hiện tại | Lệnh | Trạng thái đích | Ai được thực hiện | Điều kiện (chặn nếu không đạt) | Tác động tự động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Sinh phiếu theo CFG-M12-04 | Chờ kiểm kê | Bộ lập lịch hệ thống | Vị trí có đồ có giá trị Đang giữ; chưa có phiếu cùng kỳ | Báo Hành chính |
| Chờ kiểm kê | Hoàn thành | Hoàn thành | Hành chính | Mọi đồ còn trong phiếu có kết quả; đồ Không tìm thấy có Báo thất lạc trong cùng lần (FR-028b) | Tạo bản ghi Báo thất lạc / Ghi hư hỏng / Tìm thấy kèm theo; báo Quản lý viện nếu có đồ không rõ chủ; gắn "trễ hạn" nếu quá hạn |
| Chờ kiểm kê | Hủy phiếu | Đã hủy | Hành chính | Mọi đồ trong phiếu đã rời vị trí; có lý do | — |
| Chờ kiểm kê | Hết hạn | Chờ kiểm kê (dấu "trễ hạn") | Hệ thống | — | Nhắc theo FR-028c |

Hoàn thành và Đã hủy là trạng thái cuối. Đề nghị "Xử lý đồ không người nhận" (FR-027a) là một loại yêu cầu phê duyệt nên dùng bảng trạng thái chung của feature 000 FR-031 (Nháp → Chờ duyệt → Đã duyệt → Đã áp dụng / Áp dụng không thành; Từ chối; Hủy); FR-027a nêu nội dung, người lập, người duyệt, điều kiện áp dụng và tác động theo yêu cầu của feature 000 FR-031.

#### I. Quyền và hiển thị

- **FR-029**: Quyền theo 4.4 dòng "Đồ gửi": Hành chính, Điều dưỡng (trong phạm vi) thực hiện các lệnh ở bảng trạng thái, trừ giới hạn của Điều dưỡng với lệnh "Trả" và xác nhận người nhận khác (FR-016a) lệnh lập đề nghị xử lý (chỉ Hành chính, FR-027a), và phiếu kiểm kê (chỉ Hành chính, FR-028a); Quản lý viện, Trưởng tầng (người cao tuổi của tầng mình) chỉ xem; Bác sĩ, Nhân viên chăm sóc, Dinh dưỡng viên, Nhân viên bếp, Nhân viên vệ sinh không xem. Quản lý viện duyệt yêu cầu đính chính bản ghi bàn giao (feature 000 FR-026 (c)), duyệt thay xác nhận người nhận khác theo FR-017a, và duyệt đề nghị xử lý đồ không người nhận theo FR-027a. *(Nguồn: 4.4, 19.3; Q-150, Q-152)*
- **FR-030**: Cổng người thân MUST hiển thị danh sách đồ gửi, trạng thái, lịch sử bàn giao và ảnh chỉ cho người có quyền nhận của người cao tuổi đó (người đại diện, người thân có quyền "được phép đón" đang hiệu lực), xét tại thời điểm xem; người thân khác MUST NOT thấy đồ gửi. Thông báo gửi người thân của spec này MUST chỉ nêu loại đồ và sự kiện, không nêu mô tả, số tiền hay ảnh, để người liên hệ chính không có quyền nhận vẫn nhận thông báo BR-M12-02 mà không thấy chi tiết tài sản; người đại diện MUST ghi hoặc rút được đồng ý cho tự giữ (FR-015a), và MUST xác nhận hoặc từ chối được yêu cầu xác nhận người nhận khác trên cổng. Người thân MUST NOT thực hiện lệnh bàn giao. *(Nguồn: 4.4 NT X, BR-M12-04; Clarification 2026-09-27, Q-151)*
- **FR-031**: Mọi lệnh ở bảng trạng thái và mọi xác nhận người nhận khác MUST thực hiện trực tuyến; hệ thống MUST NOT cho ghi tạm ngoại tuyến các lệnh này. *(Suy ra từ Q-01 "khẩn cấp bắt buộc trực tuyến" và BR-M12-04 cần kiểm tra quyền tức thời)*
- **FR-031a**: Mọi lệnh, xác nhận và đính chính của spec này MUST được ghi nhật ký (feature 000); nhật ký đồ gửi thuộc nhóm "đặc biệt quan trọng" của 19.4. Bản ghi bàn giao thuộc danh sách bảo vệ của BR-M15-03. Phiếu kiểm kê đã Hoàn thành là nhóm 3 và được đối xử như bản ghi bàn giao (không sửa, xóa; chỉ đính chính theo FR-011a); xác nhận người nhận khác và đồng ý cho tự giữ là nhóm 2, chỉ đổi qua lệnh ở FR-017b. *(Nguồn: 19.4, DBR-23, BR-M15-03)*

#### J. Thông báo

- **FR-032**: Feature này MUST yêu cầu feature 009 gửi đúng các thông báo ở bảng dưới, theo khuôn chung của feature 009 FR-001, FR-043a và nguyên tắc xếp mức FR-043b. Mọi "báo", "thông báo", "nhắc" trong FR và bảng trạng thái của spec này MUST có dòng tương ứng ở bảng dưới. Các quy tắc chung cho mọi dòng, thay cho các cột mà feature 009 FR-043a yêu cầu: *(Nguồn: 17, BR-M13-01, feature 009)*
  - **Người nhận là người thân** được xác định bằng nhóm người nhận của từng dòng (người đại diện, người liên hệ chính), không bằng loại thông tin. Nội dung mang loại thông tin "chung" và chỉ nêu loại đồ và sự kiện (FR-030, Q-151), nên người liên hệ chính không có quyền nhận vẫn nhận được mà không thấy chi tiết tài sản; điều này không mở quyền xem đồ gửi trên cổng.
  - **Sau khi người cao tuổi qua đời**: mọi thông báo tới người thân của spec này MUST chỉ gửi người đại diện và MUST được đánh dấu thuộc danh sách việc sau qua đời (feature 009 FR-015, feature 004 FR-073); dòng nào ghi "người liên hệ chính" thì thay bằng người đại diện.
  - **Khóa sự kiện** (feature 009 FR-005): (loại sự kiện, đồ gửi) cho sự kiện của một đồ; (loại sự kiện, yêu cầu / đề nghị / phiếu) cho sự kiện của các đối tượng đó; với nhắc lặp thêm ngày nhắc.
  - **Nội dung rút gọn cho kênh ngoài ứng dụng** (feature 009 FR-019): loại sự kiện và loại đồ; không nêu mô tả, số tiền, ảnh, vị trí lưu giữ.

| Sự kiện | Người nhận | Mức | Căn cứ |
| --- | --- | --- | --- |
| Đồ gửi chuyển Thất lạc hoặc Hư hỏng | Người liên hệ chính (nhân viên được báo qua sự cố của feature 007); sau qua đời theo quy tắc chung ở trên. Không còn người nhận hợp lệ khi xét lại (feature 009 FR-025): Hành chính, để liên hệ trực tiếp | Trung bình | BR-M12-02, FR-021 |
| Bản ghi bàn giao bị Hủy ghi nhận mà sự kiện đã được báo (thất lạc, hư hỏng, trả, tìm thấy) | Những người đã nhận thông báo gốc | Nhẹ | FR-011a |
| Phiếu kiểm kê có đồ không rõ chủ | Quản lý viện | Nhẹ | FR-028b |
| Đồ Thất lạc được tìm thấy | Người liên hệ chính | Nhẹ | Bảng trạng thái |
| Tiếp nhận đồ có giá trị | Người đại diện | Nhẹ | Bảng trạng thái |
| Đồ gửi được trả | Người đại diện (trừ khi chính người đó nhận) | Nhẹ | FR-019 |
| Yêu cầu xác nhận người nhận khác mới | Người đại diện | Trung bình | FR-017 |
| Xác nhận người nhận khác được xác nhận, từ chối hoặc hết hạn | Người lập yêu cầu | Nhẹ | FR-017 |
| Yêu cầu xác nhận người nhận khác đủ điều kiện để Quản lý viện duyệt thay (FR-017a (a) hoặc (b)) | Quản lý viện | Trung bình | FR-017a |
| Quản lý viện duyệt thay xác nhận người nhận khác | Mọi người đại diện Hiệu lực (nếu có) | Trung bình | FR-017a |
| Hồ sơ kết thúc lưu trú được lập khi còn đồ chưa trả | Người đại diện | Nhẹ | FR-025 |
| Còn đồ chưa trả sau trạng thái cuối, theo chu kỳ CFG-M12-02 | Hành chính | Nhẹ | FR-027 |
| Đồ đủ CFG-M12-03, được lập đề nghị xử lý | Hành chính | Nhẹ | FR-027a |
| Đề nghị "Xử lý đồ không người nhận" Chờ duyệt (nhắc theo feature 000 FR-031a) | Quản lý viện | Nhẹ; theo feature 000 FR-031a | FR-027a |
| Đề nghị được duyệt, từ chối hoặc áp dụng không thành | Hành chính (người lập) | Nhẹ | FR-027a |
| Đồ gửi chuyển Đã xử lý | Người đại diện (nếu còn quan hệ Hiệu lực) | Nhẹ | FR-027a |
| Lệnh bàn giao do Hành chính thực hiện thay người giữ vắng mặt | Người giữ cũ | Nhẹ | FR-014 |
| Người cao tuổi chuyển tầng còn đồ ở vị trí tầng cũ | Hành chính; điều dưỡng phụ trách tầng mới | Nhẹ | FR-028 |
| Phiếu kiểm kê mới | Hành chính | Nhẹ | FR-028a |
| Đồng ý cho tự giữ bị rút, người cao tuổi có cờ nguy cơ đi lạc, hoặc dấu "cần đồng ý khi giao sử dụng" được bật khi đồ loại đó đang Đang được người cao tuổi sử dụng mà chưa có đồng ý | Hành chính; điều dưỡng phụ trách | Trung bình | FR-015a |
| Hồ sơ người thân chưa có giấy tờ khi nhận đồ | Hành chính | Nhẹ | FR-016 |
| Nhân viên hết ca còn là người giữ đồ có giá trị | Nhân viên đó; Người phụ trách ca | Trung bình | FR-014a |
| Yêu cầu xác nhận người nhận khác tự Hủy vì không còn đồ | Người lập yêu cầu | Nhẹ | FR-017b |
| Đồng ý cho tự giữ được ghi hoặc rút | Người đại diện khác (nếu có); Hành chính | Nhẹ | FR-015a |
| Phiếu kiểm kê quá hạn chưa hoàn thành (Hành chính: mỗi ngày; Quản lý viện: một lần) | Hành chính; Quản lý viện | Trung bình; Nhẹ | FR-028c |

### Truy vết quy tắc

| Quy tắc / quyết định | Kịch bản chấp nhận | FR |
| --- | --- | --- |
| 16.2, BR-M12-05 (tiếp nhận, ảnh) | US1 kịch bản 1 → 7 | FR-001 → FR-010 |
| 16.3, BR-M12-01, DBR-22 (bàn giao) | US2 kịch bản 1 → 7, 9, 10 | FR-011 → FR-015b, FR-001a |
| BR-M15-03, 19.4, 1.5 nhóm 3 (danh sách bảo vệ, nhật ký, đính chính) | US2 kịch bản 5, 6, 9 | FR-010, FR-011, FR-011a, FR-031a |
| 1.3 (bàn giao ca), 16.3 | — (kiểm thử cùng feature 008) | FR-014, FR-014a |
| Đồng thời thao tác | US2 kịch bản 10 | FR-011b |
| BR-M12-02 (thất lạc, hư hỏng, sự cố) | US4 kịch bản 1 → 4, 7, 10 | FR-021 → FR-024 |
| BR-M12-03, 5.6, 6.8 (kết thúc lưu trú, qua đời) | US5 kịch bản 1 → 7, 11 | FR-025 → FR-027 |
| BR-M12-04, 16.4 (trả cho người có quyền) | US3 kịch bản 1 → 9 | FR-016 → FR-020 |
| Q-148 (Thất lạc, Hư hỏng không phải trạng thái cuối) | US4 kịch bản 5, 6, 9; US5 kịch bản 4, 11 | Bảng trạng thái, FR-025, FR-026, FR-026a |
| Q-149 (người cao tuổi tự nhận) | US3 kịch bản 10 | FR-016, FR-018, FR-019 |
| Q-253 (người cao tuổi tự nhận không cần xác nhận) | US3 kịch bản 10 (phần bổ sung) | FR-016, FR-018, SC-004 |
| Q-150 (Quản lý viện duyệt thay) | US3 kịch bản 11, 12 | FR-017, FR-017a, FR-017b |
| Q-151 (hiển thị trên cổng) | US6 kịch bản 1, 2 | FR-030, FR-032 |
| Q-152 (xử lý đồ không người nhận) | US5 kịch bản 8 → 10 | FR-027, FR-027a |
| Q-153 (Điều dưỡng trả đồ) | US3 kịch bản 13 | FR-016a, FR-029 |
| Q-154 (kiểm kê) | US4 kịch bản 8, 11, 12 | FR-028a → FR-028c |
| Q-155 (đồng ý cho tự giữ) | US2 kịch bản 8 | FR-005, FR-015a, FR-017b |

### Giao tiếp với feature khác

| Hướng | Feature | Nội dung | Dữ liệu tối thiểu |
| --- | --- | --- | --- |
| Nhận | 000 | Đính chính (FR-026 (c)), yêu cầu phê duyệt loại "Đính chính" và loại "Xử lý đồ không người nhận", nhật ký, tham số CFG-M12-01 → CFG-M12-07, CFG-M10-08 | — |
| Nhận | 001 | Trạng thái người cao tuổi; sự kiện Hủy tiếp nhận, Kết thúc lưu trú, Qua đời; cờ nguy cơ đi lạc và sự kiện gắn cờ (FR-015a; FR-016 ngoại lệ Q-253) | Người cao tuổi, trạng thái, cờ, thời điểm |
| Nhận | 002 | Phạm vi dữ liệu của Điều dưỡng, Trưởng tầng | Nhân viên, người cao tuổi |
| Nhận | 003 | Tầng/khu vực của người cao tuổi; sự kiện chuyển giường sang tầng khác | Người cao tuổi, tầng cũ, tầng mới, thời điểm |
| Cung cấp | 004 | Điều kiện (b) của FR-063 và mục "xử lý đồ gửi" của FR-071 | Người cao tuổi, trạng thái điều kiện, danh sách đồ làm căn cứ |
| Gửi, Nhận | 007 | Gửi: yêu cầu tạo sự cố loại "đồ gửi thất lạc" / "đồ gửi hư hỏng", nguồn "đồ gửi"; diễn biến "đã tìm thấy"; yêu cầu chuyển sự cố Đã hủy khi bản ghi bàn giao bị Hủy ghi nhận (FR-011a). Nhận: sự cố đã ghi trực tiếp để liên kết (FR-021); trạng thái mở/đóng của sự cố cho căn cứ điều kiện (b) (FR-025) | Người cao tuổi, loại, mức, đồ gửi, bản ghi bàn giao, thời điểm |
| Gửi | 009 | Các thông báo ở bảng FR-032 | Nguồn, mức, nhóm người nhận, loại thông tin |
| Nhận | 012 | Người thân có quan hệ Hiệu lực, người đại diện, quyền "được phép đón", người liên hệ chính tại thời điểm lệnh | Người thân, người cao tuổi, quyền, thời điểm |
| Cung cấp | 012 | Đồ gửi, lịch sử bàn giao, ảnh cho cổng, chỉ hiển thị cho người có quyền nhận (FR-030, Q-151); yêu cầu xác nhận người nhận khác; thao tác ghi, rút đồng ý cho tự giữ (FR-015a) | Người cao tuổi, đồ gửi, bản ghi, yêu cầu, đồng ý |
| Nhận | 002, 008 | Trạng thái tài khoản nhân viên, ca đang diễn ra của người giữ (FR-014); sự kiện kết thúc ca | Nhân viên, ca, trạng thái tài khoản |
| Gửi | 008 | Mục "đồ gửi đang giữ" cho bản nháp bàn giao ca (FR-014a) | Nhân viên, tầng/khu vực, ca, danh sách đồ có giá trị (loại, người cao tuổi, vị trí) |
| Gửi (giai đoạn sau, Q-194) | Module 14 (feature 016) | Số đồ thất lạc, hư hỏng theo kỳ; đồ chưa trả sau kết thúc lưu trú. Ở giai đoạn này feature 016 chỉ đếm sự cố đồ gửi qua feature 007 (Q-202) | Đồ gửi, trạng thái, thời điểm |

### Key Entities *(include if feature involves data)*

- **Loại đồ** – nhóm 1: tên, dấu "có số tiền", dấu "cần đồng ý khi giao sử dụng", trạng thái; dấu "đồ có giá trị" lấy từ CFG-M12-01.
- **Vị trí lưu giữ** – nhóm 1: tên, mô tả, tầng/khu vực (nếu có), trạng thái.
- **Mức tình trạng** – nhóm 1: tên, thứ tự, dấu "không dùng được", trạng thái (FR-001a).
- **Đồ gửi (DO_GUI)** – nhóm 2: người cao tuổi, loại đồ, mô tả, số lượng hoặc số tiền, đồ gốc (khi tách), lần tiếp nhận gốc, trạng thái hiện tại, người giữ hiện tại, vị trí hiện tại. Trạng thái và người giữ chỉ đổi qua lệnh ở bảng trạng thái.
- **Bản ghi bàn giao đồ gửi (BAN_GIAO_DO_GUI)** – nhóm 3: đồ gửi, loại bàn giao (Tiếp nhận, Giao sử dụng, Thu lại, Chuyển giữ, Trả, Báo thất lạc, Ghi hư hỏng, Tìm thấy, Xử lý), trạng thái đích, người giao, người nhận, thời điểm, số lượng, mức tình trạng (FR-015b) và mô tả, vị trí, hình ảnh, lý do, người ghi nhận, danh tính người nhận và chữ ký (lệnh Trả), ý kiến người nhận, xác nhận người nhận khác (nếu có), sự cố được tạo (nếu có), đề nghị xử lý (lệnh Xử lý).
- **Phiếu kiểm kê đồ gửi** – nhóm 2 khi Chờ kiểm kê, nhóm 3 khi Hoàn thành: kỳ, vị trí lưu giữ, danh sách đồ, kết quả từng đồ, tình trạng mới, đồ bị loại và lý do, người kiểm kê, thời điểm hoàn thành, dấu "trễ hạn", các bản ghi Báo thất lạc / Ghi hư hỏng phát sinh.
- **Đề nghị xử lý đồ không người nhận** – nhóm 2 (một loại yêu cầu phê duyệt của feature 000): người cao tuổi, danh sách đồ, hình thức xử lý, đơn vị tiếp nhận, lý do, bằng chứng liên hệ, biên bản, ảnh, người lập, người duyệt, trạng thái theo vòng đời chung, đồ đã áp dụng và đồ bị loại.
- **Đồng ý cho tự giữ** – nhóm 2: người cao tuổi, đồ gửi, người đại diện đồng ý, cách ghi (cổng hoặc bản ký), thời điểm, trạng thái (Hiệu lực / Đã rút / Hết hiệu lực), người và thời điểm rút (Q-155).
- **Xác nhận người nhận khác** – nhóm 2: người cao tuổi, người nhận, danh sách đồ gửi, người lập, người đại diện xác nhận, cách xác nhận, bản ký, thời điểm, hạn hiệu lực, trạng thái, lần trả đã dùng.
- Dùng từ feature khác: **Người cao tuổi** (001), **Người thân, quyền người thân** (012), **Sự cố** (007), **Hồ sơ kết thúc lưu trú, danh sách việc sau qua đời** (004), **Tầng/khu vực** (003).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% đồ gửi trong bộ kiểm thử có đúng một lần tiếp nhận (trực tiếp hoặc qua đồ gốc) và trạng thái hiện tại bằng trạng thái đích của bản ghi bàn giao gần nhất (DBR-22); tổng số lượng các đồ tách từ cùng một lần tiếp nhận bằng số lượng tiếp nhận.
- **SC-002**: 0 bản ghi bàn giao thiếu người giao (trừ Tìm thấy), người nhận (trừ Báo thất lạc), thời điểm, số lượng hoặc tình trạng; 0 bản ghi bàn giao bị sửa hoặc xóa.
- **SC-003**: 0 lần tiếp nhận hoặc trả đồ có giá trị (theo CFG-M12-01 tại thời điểm lệnh) mà không có ảnh.
- **SC-004**: 0 lần trả thành công cho người không phải người có quyền nhận (kể cả chính người cao tuổi, trừ ngoại lệ FR-016 của Q-253) mà không có xác nhận người nhận khác còn hiệu lực bao gồm đồ đó; 0 lần Quản lý viện duyệt thay ngoài hai trường hợp của FR-017a.
- **SC-005**: 100% lần chuyển Thất lạc hoặc Hư hỏng tạo đúng một sự cố mức Trung bình (trừ khi người ghi chọn mức khác có lý do) và một thông báo tới người liên hệ chính trong vòng 5 phút.
- **SC-006**: 0 lệnh "Kết thúc lưu trú" thành công khi còn đồ Đang giữ, Đang được người cao tuổi sử dụng hoặc Hư hỏng mà không có ngoại lệ được duyệt; điều kiện (b) chuyển Đạt trong vòng 1 phút sau khi đồ cuối được trả.
- **SC-007**: Với người giao hoặc người nhận đã có hồ sơ người thân trong hệ thống và ảnh chụp tại chỗ, Hành chính tiếp nhận một đồ có giá trị trong không quá 2 phút và trả 5 đồ cho người có quyền nhận trong một lần trong không quá 5 phút, tính từ lúc chọn người cao tuổi tới lúc lưu. Đây là mục tiêu nghiệm thu, không phải tham số.
- **SC-008**: 100% đồ gửi chưa ở trạng thái cuối trong bộ kiểm thử có đủ: trạng thái, người giữ (trừ Thất lạc), vị trí (khi Đang giữ hoặc Hư hỏng) và thời điểm bản ghi bàn giao gần nhất, tra cứu được cùng lúc cho một đồ gửi mà không cần mở từng bản ghi bàn giao.
- **SC-009**: Trong bộ kiểm thử 3 kỳ kiểm kê, 100% vị trí có đồ có giá trị Đang giữ có đúng một phiếu mỗi kỳ; 0 phiếu Hoàn thành còn đồ Không tìm thấy chưa chuyển Thất lạc; 100% phiếu quá hạn được nhắc.
- **SC-010**: 100% đồ Đã xử lý có đề nghị được Quản lý viện duyệt, biên bản và bằng chứng liên hệ đạt mức tối thiểu của FR-027a; 0 đề nghị được lập trước CFG-M12-03; 0 tiền mặt có hình thức Thanh lý hoặc Tiêu hủy (Q-152).
- **SC-011**: 0 lần Giao sử dụng tiền mặt, trang sức mà không có đồng ý cho tự giữ Hiệu lực, hoặc khi người cao tuổi có cờ nguy cơ đi lạc; 100% lần rút đồng ý hoặc gắn cờ khi đồ đang được sử dụng tạo thông báo thu lại trong vòng 5 phút (Q-155).
- **SC-012**: 0 lần người thân không phải người đại diện và không có quyền "được phép đón" đang hiệu lực thấy đồ gửi trên cổng; 0 thông báo gửi người thân chứa mô tả, số tiền hay ảnh của đồ gửi (Q-151).

## Assumptions

- Số feature `013` theo cột Feature của UC-65 ở mục 4.2 `docs/phan-tich-yeu-cau.md` và theo cách spec 001, 004 đã gọi Module 12.
- Xe lăn trong 16.1 là xe lăn riêng của người cao tuổi; xe lăn của viện là tài sản của viện, ngoài hệ thống.
- Đồ người cao tuổi dùng hằng ngày (kính, quần áo) ở Đang được người cao tuổi sử dụng trong suốt thời gian dùng; việc đưa, cất hằng ngày trong phòng không cần bàn giao. Chỉ khi viện cất giữ lại thì mới ghi "Thu lại". Vì vậy Nhân viên chăm sóc không cần quyền ghi đồ gửi, đúng 4.4. Giả định này vẫn đúng sau Q-154, Q-155: tiền mặt, trang sức chỉ được người cao tuổi tự giữ khi có đồng ý và do Hành chính, Điều dưỡng giao; kiểm kê do Hành chính làm. Nhân viên chăm sóc phát hiện mất, hỏng thì ghi sự cố ở feature 007 (mọi nhân viên đều ghi được), và Điều dưỡng hoặc Hành chính liên kết sự cố đó khi ghi lệnh đồ gửi (FR-021).
- Người giao ở lệnh Tiếp nhận có thể không có tài khoản (người thân chưa có quan hệ trong hệ thống, người vận chuyển); hệ thống ghi họ tên và giấy tờ.
- Trong lệnh Báo thất lạc, trường "người nhận" để trống vì không có người nhận thực tế; người ghi nhận thay vào. Tương tự, lệnh Tìm thấy để trống "người giao" và người ghi nhận là người nhận. Đây là cách hiểu BR-M12-01 cho trường hợp này (điểm báo lại 2).
- Thời hạn hiệu lực của xác nhận người nhận khác dùng lại CFG-M10-08 (hiệu lực ngoại lệ đón) vì cùng nghĩa "thời hạn dùng một lần của một sự cho phép người đại diện cấp". Thời gian chờ trước khi Quản lý viện được duyệt thay có nghĩa khác nên dùng tham số riêng CFG-M12-05, cùng mặc định \[4 giờ\] như lựa chọn Q-150.
- Tham số mới của spec (cần bổ sung vào Phụ lục 25, điểm báo lại 8):
  - CFG-M12-02: chu kỳ nhắc đồ chưa trả sau khi hồ sơ ở trạng thái cuối, mặc định \[7 ngày\];
  - CFG-M12-03: thời gian tối thiểu từ khi hồ sơ ở trạng thái cuối tới khi được lập đề nghị xử lý đồ không người nhận, mặc định \[90 ngày\] (Q-152);
  - CFG-M12-04: chu kỳ và hạn kiểm kê đồ có giá trị, mặc định \[hằng tháng, hạn 3 ngày\] (Q-154);
  - CFG-M12-05: thời gian chờ người đại diện phản hồi trước khi Quản lý viện được duyệt thay xác nhận người nhận khác, mặc định \[4 giờ\] (Q-150);
  - CFG-M12-06: bằng chứng liên hệ tối thiểu trước khi Quản lý viện duyệt thay, mặc định \[mỗi người đại diện 2 lần qua 2 kênh\] (FR-017a, Q-157);
  - CFG-M12-07: bằng chứng liên hệ tối thiểu trước khi lập đề nghị xử lý đồ, mặc định \[2 lần, cách nhau không dưới CFG-M12-02\] (FR-027a, Q-157).
- Các mặc định spec tự đặt để yêu cầu kiểm thử được (thang tình trạng, bằng chứng liên hệ, người giữ vắng mặt và đồ khi hết ca, tiền mặt không người nhận, giới hạn đính chính) đã được chốt ngày 2026-09-27 thành Q-156 → Q-160 (điểm báo lại 11).
- Căn cứ pháp lý cho việc thanh lý, tiêu hủy tài sản của người đã mất hoặc người đã rời viện do cơ sở tự đối chiếu; hệ thống chỉ ghi quyết định, biên bản và người duyệt.
- Kiểm kê định kỳ chỉ áp cho đồ có giá trị Đang giữ (Q-154); đồ thường và đồ Hư hỏng được đối chiếu ở các lần bàn giao. Vị trí gắn tầng vẫn do Hành chính kiểm kê vì phiếu thuộc trách nhiệm quản lý đồ gửi của Hành chính (2.3).
- Thời hạn lưu: đồ gửi và bản ghi bàn giao gắn người cao tuổi nên lưu theo CFG-M15-03 (NFR-07).

## Điểm cần báo lại về tài liệu nguồn

**Trạng thái (2026-09-27):** theo yêu cầu của người dùng, `docs/nghiep-vu.md` đã được đồng bộ: Q-148 → Q-155 ở mục 24.2; Q-156 → Q-160 ở mục 24.2 (chốt theo mặc định đề xuất); thuật ngữ mới ở 2.4; 5.6 và 6.8 (tập trạng thái chặn, mục "xử lý đồ gửi"); nguồn "đồ gửi" ở 9.1; đồ gửi trên cổng ở 14.6; phần bổ sung của 16.1 → 16.4; BR-M12-01 → 05 được làm rõ hoặc chỉnh sửa, BR-M12-06 → 09 mới; mục 16.6 mới (kiểm kê, đồ không người nhận, đồ khi hết ca); quyền đồ gửi ở 19.3; CFG-M12-02 → 07 ở Phụ lục 25 và CFG-M10-08 thêm nơi dùng 16.4. Như vậy điểm 1, 2, 4, 7, 8, 13 và phần tài liệu nghiệp vụ của điểm 3 (9.1), 6 (14.6), 10 (19.3) đã được phản ánh. **Không sửa:** `docs/phan-tich-yeu-cau.md` (4.4, UC-65, ERD, DBR-22 ở điểm 10, 12, 14), theo CLAUDE.md; 19.3 là căn cứ khi 4.4 khác. Các spec 001, 004, 007, 008, 009, 012 đã được đồng bộ (điểm 16). **Còn mở:** không còn điểm nào thuộc tài liệu nghiệp vụ; các lệch với `docs/phan-tich-yeu-cau.md` được giữ làm ghi chú đã biết.

1. **Vòng đời đồ gửi (16.4 "(Bổ sung)")** chỉ nêu Đang giữ ↔ Đang được sử dụng → Đã trả; hoặc Thất lạc / Hư hỏng. Đã chốt khi clarify (Q-148): Thất lạc → Đang giữ (Tìm thấy); Hư hỏng → Đã trả; Hư hỏng chặn kết thúc lưu trú, Thất lạc không chặn. Cần bổ sung vào 16.4, sửa BR-M12-03 thành "còn đồ gửi ở trạng thái Đang giữ, Đang được sử dụng hoặc Hư hỏng", và thêm Q-148 vào mục 24.2. Ngoài ra 16.4 chưa có lệnh Chuyển giữ và việc tách đồ khi bàn giao một phần (FR-012). Cùng theo Q-148, 5.6 (dòng "→ Kết thúc lưu trú": "không còn đồ gửi/thuốc gửi đang giữ") và 6.8 ("không còn đồ gửi") cần ghi rõ tập trạng thái chặn là Đang giữ, Đang được sử dụng, Hư hỏng.
2. **BR-M12-01** yêu cầu mọi lần chuyển trạng thái có người nhận; với Thất lạc không có người nhận thực tế. Spec dùng người ghi nhận thay (Assumptions).
3. **Feature 007** cần: thêm nguồn phát sinh "đồ gửi" vào FR-040; thêm loại "đồ gửi thất lạc", "đồ gửi hư hỏng" (mức mặc định Trung bình) vào danh mục FR-042; loại trừ hai loại này khỏi FR-043 (b) (không tạo yêu cầu đánh giá lại "sau sự cố") (FR-022). Mục 9.1 "Nguồn phát sinh" cũng chưa có "đồ gửi".
4. **16.1** liệt kê thuốc gia đình gửi là một loại đồ gửi nhưng quy trình thuộc 11.4 và feature 006 (feature 006 điểm báo lại 6). Nên ghi ở 16.1 rằng thuốc gia đình gửi không đi qua quy trình đồ gửi. 16.1 cũng chưa có "trang sức", "tiền mặt" dù CFG-M12-01 dùng hai loại này.
5. **Feature 004 FR-063 (b)** ghi "không còn đồ gửi đang giữ"; theo BR-M12-03 cần là "không còn đồ gửi Đang giữ, Đang được người cao tuổi sử dụng hoặc Hư hỏng" (Q-148); tương tự cho mục "xử lý đồ gửi" của FR-071 (Hoàn thành khi mọi đồ ở Đã trả, Thất lạc, Đã xử lý). FR-072 (hồ sơ "Đã đóng") khớp với FR-026a: feature 004 không cần nhận sự kiện "tìm thấy sau khi đóng", vì hồ sơ không mở lại.
6. **Feature 012 FR-046** chưa có đồ gửi. Theo Q-151, đồ gửi trên cổng chỉ hiển thị cho người đại diện và người có quyền "được phép đón". Đề xuất thêm **một quy tắc hiển thị riêng** cho mục đồ gửi ở FR-046 (không thêm loại thông tin thứ năm), vì thông báo đồ gửi vẫn dùng loại "chung" của feature 009 FR-013 với người nhận xác định theo nhóm (FR-032); feature 012 FR-001 cũng nên cho ghi giấy tờ tùy thân của người đại diện để xác minh khi nhận đồ (FR-016) (và chú thích giới hạn cột NT của dòng "Đồ gửi" ở 4.4); và cần thêm thao tác xác nhận người nhận khác, ghi và rút đồng ý cho tự giữ cho người đại diện (FR-030, FR-015a). **Feature 009 FR-043b** cần thêm các dòng của bảng FR-032.
7. **Đồ không có người nhận** sau kết thúc lưu trú hoặc qua đời: tài liệu chưa có quy tắc. Đã chốt khi clarify (Q-152): đề nghị "Xử lý đồ không người nhận" sau CFG-M12-03, Quản lý viện duyệt, trạng thái cuối mới "Đã xử lý" (FR-027a). Cần bổ sung vào 16.4 (trạng thái Đã xử lý), thêm một quy tắc BR-M12 mới, và thêm loại yêu cầu này vào 4.4 (HC T, QL D).
8. **Phụ lục 25** cần thêm CFG-M12-02 → CFG-M12-07 (đề xuất). Mục 24.2 cần thêm Q-148 → Q-155 (đã chốt 2026-09-27); Q-156 → Q-160 đã chốt ngày 2026-09-27 (điểm 11). Quy tắc đồng ý cho tự giữ tiền mặt, trang sức (Q-155, FR-015a) cần bổ sung vào BR-M12-05 hoặc một BR-M12 mới; feature 001 cần cung cấp sự kiện gắn cờ nguy cơ đi lạc cho feature này. Module 12 chưa có kiểm kê; cần bổ sung vào mục 16 và một quy tắc BR-M12 mới (Q-154).
   - **BR-M12-04** cần bổ sung theo Q-149, Q-150: người cao tuổi nhận lại đồ cũng là "người nhận khác", cần người đại diện xác nhận; khi không liên hệ được người đại diện (quá CFG-M12-05 hoặc không có người đại diện Hiệu lực), Quản lý viện duyệt thay có lý do và bằng chứng (FR-017a).
9. **Bồi thường** đồ thất lạc, hư hỏng do lỗi của viện: tài liệu không nêu; spec để ngoài hệ thống. Nếu cơ sở muốn ghi nhận bồi thường thành khoản điều chỉnh chi phí, cần bổ sung vào Module 11.
10. **Permission Matrix 4.4, dòng "Đồ gửi"**: Trưởng tầng X được hiểu là trong phạm vi tầng; Điều dưỡng T được hiểu là trong phạm vi dữ liệu và, với lệnh Trả, chỉ đồ không có giá trị cho người có quyền nhận (Q-153); Quản lý viện chỉ có X nhưng theo Q-150, Q-152 được duyệt thay xác nhận người nhận khác và duyệt đề nghị xử lý đồ (thực chất là D). Người thân có X không chú thích, nhưng theo Q-151 chỉ người đại diện và người có quyền đón được xem, và theo BR-M12-04, Q-155 người đại diện có thao tác xác nhận, đồng ý (tương đương T có chú thích ²). UC-65 chỉ ghi actor Hành chính trong khi 4.4 cho Điều dưỡng T. Chú thích đề xuất cho dòng "Đồ gửi" nếu chủ tài liệu muốn cập nhật: QL "X, D²³"; ĐD "T²⁴"; NT "X²⁵, T²"; với ²³ = duyệt thay xác nhận người nhận khác (FR-017a) và duyệt đề nghị xử lý đồ (FR-027a); ²⁴ = Trả chỉ đồ không có giá trị cho người có quyền nhận (Q-153); ²⁵ = chỉ người đại diện và người có quyền "được phép đón" (Q-151). Ghi chú lệch đã biết, không sửa `docs/phan-tich-yeu-cau.md`.
11. **Quyết định chốt ngày 2026-09-27 (theo mặc định đề xuất), đã đưa vào mục 24.2**:

    | Mã | Vấn đề | Quyết định | Căn cứ |
    | --- | --- | --- | --- |
    | Q-156 | Thang mức tình trạng đồ gửi | Danh mục 4 mức: Tốt / Có dấu hiệu sử dụng / Hư hỏng một phần / Không dùng được | FR-001a, FR-015, FR-015b |
    | Q-157 | Bằng chứng liên hệ tối thiểu trước khi Quản lý viện duyệt thay hoặc lập đề nghị xử lý đồ | CFG-M12-06 \[mỗi người đại diện 2 lần qua 2 kênh\]; CFG-M12-07 \[2 lần, cách nhau không dưới CFG-M12-02\] | FR-017a, FR-027a |
    | Q-158 | Khi nào Hành chính được bàn giao thay người giữ; đồ có giá trị do nhân viên giữ khi hết ca | Người giữ không có ca đang diễn ra hoặc tài khoản không Hoạt động; đồ có giá trị còn giữ lúc hết ca vào bàn giao ca và được nhắc chuyển giữ | FR-014, FR-014a |
    | Q-159 | Hình thức xử lý tiền mặt không người nhận | Chỉ "Chuyển cơ quan có thẩm quyền"; tiền thanh lý đồ ghi biên bản, xử lý ngoài hệ thống | FR-027a |
    | Q-160 | Giới hạn đính chính bản ghi bàn giao | Không đổi loại bàn giao, trạng thái đích; "Hủy ghi nhận" chỉ áp cho bản ghi gần nhất | FR-011a |

    Các điểm đi kèm cũng đã được phản ánh: xe lăn trong 16.1 là xe lăn riêng (16.1 bổ sung); CFG-M12-05 tách khỏi CFG-M10-08 (Phụ lục 25); feature 007 nhận yêu cầu chuyển sự cố Đã hủy và cho liên kết sự cố có sẵn (feature 007 FR-046b, đồng bộ spec 013).
12. **DBR-22** ("mỗi đồ gửi có ít nhất một lần bàn giao (lần tiếp nhận)"): đồ sinh ra do tách (FR-012) không có bản ghi Tiếp nhận riêng mà trỏ về lần tiếp nhận của đồ gốc (FR-004). Spec hiểu DBR-22 là "mỗi đồ gửi truy được về đúng một lần tiếp nhận"; nên sửa câu chữ DBR-22 và thêm quan hệ đồ gốc ↔ đồ tách.
13. **Thuật ngữ 2.4**: đề xuất thêm "Người giữ", "Người có quyền nhận", "Xác nhận người nhận khác", "Đồng ý cho tự giữ", "Đồ có giá trị", "Đồ không rõ chủ" (mục "Thuật ngữ của spec").
14. **ERD miền E** chỉ có DO_GUI, BAN_GIAO_DO_GUI. Thiếu: loại đồ, vị trí lưu giữ, mức tình trạng, xác nhận người nhận khác, đồng ý cho tự giữ, phiếu kiểm kê, đề nghị xử lý đồ (một loại yêu cầu phê duyệt), quan hệ đồ gốc ↔ đồ tách, và quan hệ bản ghi bàn giao ↔ sự cố.
15. **BR-M01-05**: *(Đã kiểm tra khi đồng bộ, không cần sửa.)* BR-M01-05 đã có ngoại lệ (3) "hoàn thành danh sách việc sau qua đời và các việc kết thúc trên đối tượng của module khác (đồ gửi, thuốc gửi, chi phí, sự cố)", khớp FR-027, FR-026a. Ghi chú trước đây ("chỉ hai ngoại lệ") là nhầm.
16. **Đồng bộ các spec khác** — *đã đồng bộ ngày 2026-09-27: mỗi spec 001, 004, 007, 008, 009, 012 có mục "Cập nhật 2026-09-27 (đồng bộ với spec 013)" và các FR được sửa: 001 FR-038, bảng trạng thái (Kết thúc lưu trú); 004 FR-063 (b), FR-071, FR-072; 007 FR-040, FR-042, FR-043 (b), FR-046b mới, bảng FR-045, bảng giao tiếp; 008 FR-039 (g), FR-040, bảng giao tiếp; 009 dòng 013 ở bảng FR-043b; 012 FR-046b mới, Ngoài phạm vi, bảng giao tiếp. 008 FR-039 (g) theo Q-158 đã chốt.*
    - **001**: cung cấp sự kiện gắn, gỡ cờ nguy cơ đi lạc cho feature 013 (FR-015a); thêm dòng vào bảng giao tiếp.
    - **004**: FR-063 (b), FR-071 theo điểm 5.
    - **007**: theo điểm 3 và 11 (nguồn "đồ gửi", hai loại sự cố, ngoại lệ FR-043 (b), hủy sự cố khi Hủy ghi nhận, liên kết sự cố có sẵn).
    - **008**: nhận mục "đồ gửi đang giữ" vào bản nháp bàn giao ca (FR-014a).
    - **009**: thêm các dòng FR-032 vào bảng mức FR-043b.
    - **012**: theo điểm 6 (quy tắc hiển thị riêng, thao tác của người đại diện, giấy tờ của người đại diện).

**(2026-09-30)** 17. **Rà soát vận hành (B12.2)** – người cao tuổi không có cờ nguy cơ đi lạc tự nhận lại đồ không thuộc loại "cần đồng ý khi giao sử dụng" thì không cần xác nhận người nhận khác (Q-253, sửa Q-149). *(Đã xử lý 2026-09-30: 2.4, 16.4, BR-M12-04, Q-253 ở 24.2, BF-14; FR-016, FR-018, User Story 3 kịch bản 10, SC-004.)*

**(2026-10-01)** 18. **Checklist cross-feature CHK095** – Điều dưỡng trả được đồ không có giá trị thẳng cho người cao tuổi thuộc ngoại lệ Q-253 (Q-258). *(Đã xử lý 2026-10-01: BR-M12-04, Q-258 ở 24.2; FR-016a, bảng trạng thái đồ gửi.)*
