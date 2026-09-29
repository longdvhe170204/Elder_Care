# Feature Specification: Nền tảng quy tắc nghiệp vụ dùng chung

**Feature Branch**: `000-shared-foundation`

**Created**: 2026-09-25

**Status**: Draft

**Input**: User description: "Mô tả các quy tắc nghiệp vụ dùng chung cho mọi module của hệ thống quản lý viện dưỡng lão, theo docs/nghiep-vu.md mục 1.3–1.5, 19.4 và Phụ lục 25: (1) phân loại dữ liệu thành danh mục, nghiệp vụ có trạng thái và ghi nhận đã xác nhận, cùng thao tác được phép với từng nhóm; (2) tham số cấu hình do quản lý viện thay đổi, có hiệu lực ngay và lưu lịch sử; (3) yêu cầu phê duyệt dùng chung: tạo, duyệt, từ chối, áp dụng đúng một lần; (4) bản ghi đã xác nhận chỉ được đính chính bằng bản ghi mới có lý do; (5) nhật ký thay đổi ghi người thực hiện, thời điểm, giá trị trước/sau và lý do. Chưa bao gồm nghiệp vụ của module cụ thể nào."

## Clarifications

### Session 2026-09-25

- Q: Người vừa lập yêu cầu vừa có quyền duyệt loại yêu cầu đó có được tự duyệt không? → A: Có, nhưng bắt buộc ghi lý do và nhật ký đánh dấu "tự duyệt" (đề xuất Q-10).
- Q: Yêu cầu đã duyệt nhưng đến ngày hiệu lực điều kiện áp dụng không còn thỏa thì chuyển trạng thái nào? → A: Chuyển trạng thái kết thúc mới "Áp dụng không thành", ghi lý do, báo người duyệt và người yêu cầu; muốn tiếp tục thì lập yêu cầu mới (đề xuất Q-11).
- Q: Hồ sơ và nhật ký lưu giữ tối thiểu bao lâu, cố định hay cấu hình? → A: Hai tham số mới — CFG-M15-03 (hồ sơ, mặc định 10 năm sau khi kết thúc lưu trú) và CFG-M15-04 (nhật ký, mặc định 10 năm); nhật ký của một hồ sơ không bị xóa trước khi hồ sơ đó hết hạn lưu (Q-04).
- Q: Ngoài Quản lý viện, ai được xem lịch sử thay đổi của đối tượng? → A: Quản lý viện xem toàn bộ nhật ký; nhân viên xem lịch sử của đối tượng trong phạm vi họ đang được xem; người thân không xem nhật ký, chỉ thấy giá trị hiện hành (đề xuất bổ sung dòng "Nhật ký" vào Permission Matrix 4.4).
- Q: Ai được tạo bản đính chính cho bản ghi đã xác nhận? → A: Người đã ghi bản gốc, hoặc người phụ trách ca / trưởng tầng của phạm vi đó; module có thể thu hẹp thêm.
- Q: Khi dữ liệu và nhật ký hết thời hạn lưu giữ, có được loại bỏ không? → A: Chỉ Bộ lập lịch hệ thống được loại bỏ khi hết thời hạn; mỗi đợt loại bỏ ghi một bản nhật ký tóm tắt (loại dữ liệu, số lượng, khoảng thời gian, căn cứ); không người dùng nào được xóa.
- Q: Với bản ghi đã xác nhận không gắn tầng (chi phí đã chốt, bàn giao đồ gửi, bản ghi của Hành chính), ngoài người ghi gốc ai được đính chính? → A: Quản lý viện; trưởng tầng không được đính chính loại bản ghi này. *(Đã được thay bằng câu trả lời về khớp Permission Matrix bên dưới.)*
- Q: Nếu Bộ lập lịch không chạy đúng ngày hiệu lực của yêu cầu đã duyệt thì áp dụng thế nào? → A: Áp dụng bù ngay khi chạy lại; ngày hiệu lực giữ đúng ngày đã duyệt; nhật ký ghi cả ngày hiệu lực và thời điểm áp dụng thực tế, đánh dấu "áp dụng bù".
- Q: Giá trị trước/sau trong nhật ký có bị ẩn theo quyền xem từng trường của nhân viên không? → A: Có; trường người xem không được xem chỉ hiện "đã thay đổi", không hiện giá trị.
- Q: Yêu cầu nằm ở Chờ duyệt quá lâu thì xử lý thế nào? → A: Quá CFG-M15-05 (đề xuất, mặc định 48 giờ) thì nhắc người duyệt; quá gấp đôi thời hạn thì báo Quản lý viện; không tự hủy. *(Mốc báo Quản lý viện được thay bằng CFG-M15-06 ở câu trả lời bên dưới.)*
- Q: Permission Matrix chỉ cho Quản lý viện X/D với chi phí, đồ gửi; Quản lý viện có được trực tiếp đính chính bản ghi không gắn tầng không? → A: Không. Nhân viên khác cùng vai trò với người ghi gốc lập bản đính chính dưới dạng yêu cầu phê duyệt; bản đính chính chỉ có hiệu lực khi Quản lý viện duyệt.
- Q: Yêu cầu được duyệt sau khi ngày hiệu lực mong muốn đã qua thì tác động tính từ ngày nào? → A: Từ ngày được duyệt (ngày hiệu lực thực tế); ghi cả ngày mong muốn và ngày thực tế; muốn tính lùi thì module sở hữu dùng khoản điều chỉnh.
- Q: Mốc báo Quản lý viện khi yêu cầu chưa được duyệt là tham số riêng hay cố định gấp đôi CFG-M15-05? → A: Tham số riêng CFG-M15-06 (đề xuất), mặc định 96 giờ kể từ lúc gửi duyệt, phải lớn hơn CFG-M15-05.
- Q: Bản ghi hết hạn lưu nhưng còn được bản ghi chưa hết hạn tham chiếu thì xử lý thế nào? → A: Hoãn loại bỏ tới khi mọi bản ghi tham chiếu cũng hết hạn; cả chuỗi (gốc, đính chính, nhật ký liên quan) loại bỏ cùng một đợt.
- Q: Nếu áp dụng yêu cầu đã duyệt bị lỗi giữa chừng thì xử lý thế nào? → A: Hoặc toàn bộ, hoặc không; lỗi thì bỏ mọi tác động, yêu cầu giữ Đã duyệt (chờ hiệu lực) và được thử lại ở lần chạy kế tiếp như áp dụng bù.

### Cập nhật 2026-09-26 (đồng bộ với spec 012)

Spec 012 đã chốt khi clarify (Q-118): yêu cầu thay đổi quyền người thân và danh sách được phép đón (BR-M10-07), cùng yêu cầu ngoại lệ đón (BR-M10-03) và dấu "được tự về" (Q-125), dùng vòng đời riêng "Chờ xác nhận → Hiệu lực / Từ chối / Hủy", không đi qua vòng đời chung của spec này. FR-031 được bổ sung ngoại lệ này; Điểm báo lại "Còn mở" 1 được giải quyết. Sau checklist consistency của 012: ngoại lệ gồm thêm yêu cầu đặt/thôi người đại diện (Q-127) và phiếu đăng ký người thân (Q-126); nhắc yêu cầu chờ lâu của 012 dùng tham số riêng CFG-M10-11, không dùng CFG-M15-05/06 của spec này (Q-130).

### Cập nhật 2026-09-27 (đồng bộ với spec 010)

Theo 15.6, DBR-17 và mục 1.5 đã làm rõ ở tài liệu nguồn, chi phí đã chốt không có bản đính chính. Sai sót của chi phí đã chốt chỉ được xử lý bằng khoản điều chỉnh (UC-63: Hành chính lập, Quản lý viện duyệt; feature 010 mục F). Sửa theo đó:
- **FR-026 (c):** bỏ "chi phí đã chốt" khỏi ví dụ; ghi chi phí là ngoại lệ theo FR-030.
- **FR-031:** đề nghị mua hộ (feature 010) là đối tượng có vòng đời riêng, không phải một loại yêu cầu phê duyệt dùng chung; nó mượn FR-031a và FR-040.

### Cập nhật 2026-09-27 (đồng bộ với spec 015)

Spec 015 (Q-179, Q-182, Q-184) dùng yêu cầu phê duyệt dùng chung cho yêu cầu đổi ca và yêu cầu nghỉ đột xuất, với các sai khác vòng đời được khai báo là ngoại lệ ở FR-031: bước "Chờ người nhận đồng ý", trạng thái "Người nhận từ chối", "Đã áp dụng một phần", "Hết hiệu lực", và chuyển dạng "theo ngày" → "theo ca". Lý do bắt buộc, nhật ký, tự duyệt (Q-10), chống quyết định đồng thời và nhắc theo FR-031a giữ nguyên.

### Cập nhật 2026-09-28 (đồng bộ với spec 002, 016; rà chéo)

Spec 002 (FR-015, ghi nhật ký đăng nhập) và spec 016 (FR-062, ghi lại mỗi lần xuất báo cáo; Q-193, Q-198) cần ghi nhật ký cho hành động **không thay đổi dữ liệu**. Mục E trước đây chỉ nói về thay đổi. Spec này bổ sung:
- FR-043a mới: nhật ký cho hành động không thay đổi dữ liệu, do spec module sở hữu yêu cầu; lần xuất báo cáo là một bản ghi nhật ký, không phải dữ liệu nghiệp vụ riêng.
- FR-044: lý do bắt buộc với lần xuất báo cáo có định danh người cao tuổi (mục đích xuất, Q-198).
- FR-047: áp cho việc xuất; không file nào được giao cho người dùng mà thiếu bản ghi nhật ký.
- Tên mục E đổi thành "Nhật ký hệ thống".

## Phạm vi

**Trong phạm vi**: các quy tắc mà mọi module (001–016) kế thừa, không lặp lại trong spec của module:

1. Phân loại dữ liệu thành 3 nhóm và thao tác được phép với từng nhóm (mục 1.5).
2. Tham số cấu hình (Phụ lục 25, BR-M15-04, DBR-24, UC-69).
3. Yêu cầu phê duyệt dùng chung (YEU_CAU_PHE_DUYET, UC-14).
4. Đính chính bản ghi đã xác nhận (mục 1.3, 1.5, BR-M15-03, DBR-23).
5. Nhật ký hệ thống: thay đổi dữ liệu và các hành động không thay đổi dữ liệu mà spec module yêu cầu ghi (mục 19.4, DBR-23, DBR-25).

**Ngoài phạm vi**:

- Nghiệp vụ riêng của từng module: loại yêu cầu phê duyệt cụ thể (thay đổi lưu trú, đổi ca, người được phép đón…), nội dung lệnh nghiệp vụ cụ thể, khoản điều chỉnh chi phí (Module 11, DBR-17), tác động dây chuyền của một bản đính chính (ví dụ sinh lại chi phí).
- Tài khoản, đăng nhập, gán vai trò và phạm vi dữ liệu (feature 002, UC-68, UC-70). Spec này chỉ **dùng** kết quả kiểm tra quyền theo BR-M15-01.
- Thông báo gửi cho người duyệt/người yêu cầu (feature 009).
- Ghi nhật ký lượt xem hồ sơ sức khỏe của người thân (NFR-08, feature 012).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Mỗi loại dữ liệu chỉ cho phép đúng thao tác của nhóm mình (Priority: P1)

Mọi loại dữ liệu trong hệ thống thuộc đúng một trong ba nhóm ở mục 1.5. Người dùng chỉ thấy và chỉ thực hiện được thao tác mà nhóm đó cho phép: danh mục được tạo, sửa, ngừng hiệu lực; nghiệp vụ có trạng thái chỉ thay đổi qua lệnh nghiệp vụ có điều kiện hoặc phiên bản mới có ngày hiệu lực; ghi nhận đã xác nhận chỉ được ghi thêm.

**Why this priority**: Đây là khung mà mọi module khác dựa vào. Nếu không có, các module sẽ quay về kiểu Thêm/Sửa/Xóa tùy ý, làm mất tính toàn vẹn của hồ sơ chăm sóc (nguyên tắc III của constitution).

**Independent Test**: Chọn một loại dữ liệu đại diện cho mỗi nhóm (ví dụ: dịch vụ – nhóm 1; hợp đồng – nhóm 2; liều thuốc đã xác nhận – nhóm 3), thử lần lượt các thao tác tạo, sửa, xóa, ngừng hiệu lực, đổi trạng thái trực tiếp và kiểm tra hệ thống chỉ chấp nhận đúng thao tác của nhóm.

**Acceptance Scenarios**:

1. **Given** một mục danh mục (nhóm 1) chưa từng được bản ghi nào tham chiếu, **When** người có quyền cấu hình yêu cầu xóa, **Then** mục đó bị xóa và việc xóa được ghi nhật ký.
2. **Given** một mục danh mục đã được ít nhất một bản ghi lịch sử tham chiếu, **When** người có quyền yêu cầu xóa, **Then** hệ thống từ chối xóa, chỉ cho phép "Ngừng hiệu lực"; sau khi ngừng, mục đó không còn được chọn cho nghiệp vụ mới nhưng vẫn hiển thị đúng trong các bản ghi cũ.
3. **Given** một đối tượng nhóm 2 đang ở một trạng thái, **When** người dùng cố thay đổi trực tiếp trường trạng thái hoặc sửa nội dung đối tượng mà không qua lệnh nghiệp vụ, **Then** hệ thống không cung cấp thao tác đó.
4. **Given** một lệnh nghiệp vụ của nhóm 2 có điều kiện chưa được thỏa, **When** người dùng thực hiện lệnh, **Then** hệ thống từ chối, nêu điều kiện chưa thỏa, và đối tượng giữ nguyên trạng thái.
5. **Given** một lệnh nghiệp vụ hợp lệ, **When** người dùng thực hiện lệnh mà không nhập lý do, **Then** hệ thống yêu cầu lý do; khi có lý do, lệnh được thực hiện và lưu người thực hiện, thời điểm, lý do.
6. **Given** một đối tượng nhóm 2 được thay đổi bằng phiên bản mới có ngày hiệu lực, **When** người dùng tạo phiên bản có khoảng hiệu lực chồng lên một phiên bản khác của cùng đối tượng, **Then** hệ thống từ chối.
7. **Given** một bản ghi nhóm 3 đã xác nhận, **When** bất kỳ ai, kể cả quản lý viện, yêu cầu sửa hoặc xóa, **Then** hệ thống không có thao tác đó; chỉ có thao tác đính chính (User Story 2).

---

### User Story 2 - Sai sót trên bản ghi đã xác nhận được sửa bằng bản đính chính (Priority: P1)

Khi nhân viên phát hiện một bản ghi đã xác nhận bị sai (ví dụ ghi nhầm chỉ số, ghi nhầm người cao tuổi), họ tạo một bản đính chính có lý do. Bản gốc vẫn giữ nguyên; người xem thấy rõ bản gốc đã được đính chính và thấy giá trị đang có hiệu lực.

**Why this priority**: Mục 1.3 và BR-M15-03 yêu cầu bản ghi quan trọng không bị sửa/xóa, nhưng nhân viên vẫn phải có cách sửa sai. Không có đính chính thì nhóm 3 không dùng được trong thực tế.

**Independent Test**: Tạo một bản ghi nhóm 3 đại diện, xác nhận nó, tạo bản đính chính, rồi kiểm tra: bản gốc không đổi, bản đính chính trỏ về bản gốc, giá trị hiện hành là giá trị đính chính, toàn bộ chuỗi xem lại được.

**Acceptance Scenarios**:

1. **Given** một bản ghi nhóm 3 đã xác nhận, **When** người đã ghi bản gốc (hoặc người phụ trách ca / trưởng tầng của phạm vi đó) tạo bản đính chính kèm lý do và nội dung đúng, **Then** hệ thống lưu bản đính chính trỏ đúng bản ghi gốc, bản gốc giữ nguyên nội dung, và nhật ký ghi lại lần đính chính.
2. **Given** người dùng tạo bản đính chính, **When** lý do để trống, **Then** hệ thống từ chối.
3. **Given** một bản ghi đã xác nhận do nhân viên A ghi, **When** nhân viên B cùng vai trò nhưng không phải người phụ trách ca hay trưởng tầng của phạm vi đó cố tạo bản đính chính, **Then** hệ thống từ chối.
4. **Given** một bản ghi đã xác nhận không gắn tầng (ví dụ bàn giao đồ gửi do Hành chính ghi) và người ghi gốc vắng mặt, **When** trưởng tầng cố tạo bản đính chính, **Then** hệ thống từ chối; **When** một nhân viên Hành chính khác lập bản đính chính kèm lý do, **Then** hệ thống tạo yêu cầu phê duyệt loại "Đính chính" ở Chờ duyệt, bản gốc chưa bị đánh dấu đính chính; **When** Quản lý viện duyệt, **Then** bản đính chính được lưu và có hiệu lực; **When** Quản lý viện từ chối, **Then** không có bản đính chính nào được lưu.
5. **Given** một bản ghi gốc đã có bản đính chính, **When** người dùng xem bản ghi, **Then** hệ thống hiển thị bản ghi đã được đính chính, giá trị hiện hành theo bản đính chính mới nhất, và cho xem toàn bộ các lần đính chính theo thứ tự thời gian.
6. **Given** một bản ghi gốc đã có một bản đính chính, **When** người dùng phát hiện bản đính chính cũng sai và tạo thêm một bản đính chính, **Then** bản đính chính mới cũng trỏ về bản ghi gốc, trở thành giá trị hiện hành, còn bản đính chính trước vẫn được giữ.
7. **Given** bản ghi bị ghi nhầm hoàn toàn (ví dụ ghi cho sai người cao tuổi), **When** người dùng tạo bản đính chính loại "Hủy ghi nhận" kèm lý do, **Then** bản gốc được đánh dấu không còn hiệu lực trong mọi thống kê và tính toán về sau nhưng vẫn giữ nguyên và xem lại được.
8. **Given** một bản ghi chưa xác nhận (bản nháp) của nhóm 3, **When** người dùng muốn thay đổi nội dung, **Then** việc chỉnh sửa trước khi xác nhận theo quy định của module sở hữu; quy tắc đính chính chỉ áp dụng sau khi bản ghi được xác nhận.

---

### User Story 3 - Mọi thay đổi quan trọng đều truy vết được qua nhật ký (Priority: P1)

Quản lý viện (hoặc người có quyền xem đối tượng) tra cứu được ai đã làm gì, lúc nào, trên đối tượng nào, giá trị trước và sau, lý do. Nhật ký không ai sửa hay xóa được.

**Why this priority**: Mục 1.3 ("mọi thay đổi quan trọng phải có lịch sử và người thực hiện"), 19.4 và DBR-23 là điều kiện để mọi quy tắc khác kiểm chứng được. Các User Story 1, 2, 4, 5 đều ghi vào nhật ký.

**Independent Test**: Thực hiện một thay đổi trên mỗi nhóm dữ liệu, một lần đổi tham số, một lần đính chính; tra cứu nhật ký theo đối tượng, theo người, theo khoảng thời gian và kiểm tra đủ các trường ở mục 19.4.

**Acceptance Scenarios**:

1. **Given** một thay đổi bất kỳ trên dữ liệu nhóm 2 hoặc nhóm 3, **When** thay đổi được lưu, **Then** nhật ký có một bản ghi gồm: người thực hiện, thời điểm, hành động, đối tượng, dữ liệu trước, dữ liệu sau, lý do (nếu hành động yêu cầu lý do).
2. **Given** một thay đổi do hệ thống tự thực hiện theo lịch (ví dụ áp dụng yêu cầu đã duyệt đến ngày hiệu lực), **When** thay đổi được lưu, **Then** nhật ký ghi người thực hiện là "Bộ lập lịch hệ thống" (AC-11) và tham chiếu tới căn cứ (ví dụ mã yêu cầu phê duyệt).
3. **Given** một bản ghi nhật ký đã tồn tại, **When** bất kỳ ai, kể cả quản lý viện, cố sửa hoặc xóa, **Then** hệ thống không có thao tác đó.
4. **Given** một nhóm bản ghi nhật ký đã quá CFG-M15-04 và hồ sơ liên quan đã quá CFG-M15-03, **When** Bộ lập lịch hệ thống thực hiện đợt loại bỏ, **Then** các bản ghi đó bị loại bỏ và nhật ký có một bản ghi tóm tắt gồm loại dữ liệu, số lượng, khoảng thời gian, căn cứ; bản ghi nhật ký của hồ sơ còn trong thời hạn CFG-M15-03 vẫn được giữ.
5. **Given** một liều thuốc đã quá CFG-M15-03 nhưng còn một khoản chi phí chưa hết hạn trỏ tới nó, **When** Bộ lập lịch thực hiện đợt loại bỏ, **Then** liều thuốc và các bản đính chính, nhật ký liên quan được giữ lại; chúng chỉ bị loại bỏ, cùng một đợt, khi khoản chi phí cũng hết hạn.
6. **Given** nhật ký không ghi được vì bất kỳ lý do gì, **When** người dùng thực hiện thay đổi, **Then** thay đổi không được lưu và người dùng nhận thông báo lỗi; không tồn tại thay đổi nhóm 2/3 nào mà không có nhật ký tương ứng.
7. **Given** một nhân viên đang có quyền xem một đối tượng trong phạm vi được phân công, **When** mở lịch sử thay đổi của đối tượng đó, **Then** thấy các bản ghi nhật ký của đối tượng; với đối tượng ngoài phạm vi hiện tại, hệ thống từ chối.
8. **Given** Dinh dưỡng viên xem lịch sử thay đổi của hồ sơ một người cao tuổi, trong đó có lần sửa cả dị ứng và một trường sức khỏe ngoài phạm vi của Dinh dưỡng viên, **When** mở bản ghi nhật ký đó, **Then** thấy giá trị trước/sau của dị ứng, còn trường sức khỏe kia chỉ hiện "đã thay đổi".
9. **Given** người thân được cấp quyền xem một dữ liệu đã được đính chính, **When** xem dữ liệu đó trên cổng người thân, **Then** chỉ thấy giá trị hiện hành, không thấy nhật ký hay giá trị trước.
10. **Given** quản lý viện cần điều tra, **When** tra cứu nhật ký theo đối tượng, người thực hiện, loại hành động hoặc khoảng thời gian, **Then** hệ thống trả về các bản ghi phù hợp, sắp xếp theo thời điểm.
11. **Given** Quản lý viện xuất một báo cáo có định danh người cao tuổi với mục đích "họp giao ban" (feature 016), **When** lần xuất hoàn tất, **Then** có đúng một bản ghi nhật ký hành động "Xuất báo cáo" với lý do là mục đích xuất, dữ liệu trước/sau để trống và phần chi tiết của lần xuất; không có bản ghi nghiệp vụ nào khác được tạo; **When** việc lưu bản ghi nhật ký thất bại, **Then** file không được giao (FR-043a, FR-044, FR-047); **When** một nhân viên chỉ xem báo cáo trên hệ thống, **Then** không có bản ghi nhật ký nào được tạo.

---

### User Story 4 - Quản lý viện thay đổi tham số cấu hình, có hiệu lực ngay và lưu lịch sử (Priority: P2)

Quản lý viện xem danh sách tham số ở Phụ lục 25 cùng giá trị hiện hành và mặc định, đổi giá trị kèm lý do. Giá trị mới áp dụng ngay cho các lần kiểm tra quy tắc tiếp theo, không cần triển khai lại hệ thống. Lịch sử giá trị được giữ để biết tại một thời điểm quá khứ quy tắc đã dùng giá trị nào.

**Why this priority**: Nguyên tắc V của constitution và NFR-12 yêu cầu ngưỡng, thời hạn, tỷ lệ là tham số. Các module có thể chạy với giá trị mặc định trước khi màn hình cấu hình hoàn thiện, nên ưu tiên sau nhóm P1.

**Independent Test**: Đổi giá trị một tham số thời hạn (ví dụ CFG-M05-01, mặc định 15 phút), kích hoạt quy tắc dùng tham số đó trước và sau khi đổi, kiểm tra quy tắc dùng đúng giá trị tương ứng; tra cứu lịch sử và giá trị tại một thời điểm quá khứ.

**Acceptance Scenarios**:

1. **Given** quản lý viện đang xem danh sách tham số, **When** mở một tham số, **Then** thấy mã CFG, mô tả, giá trị hiện hành, giá trị mặc định, quy tắc sử dụng (cột "Dùng tại" của Phụ lục 25) và lịch sử thay đổi.
2. **Given** quản lý viện nhập giá trị mới hợp lệ và lý do, **When** lưu, **Then** giá trị mới thành giá trị hiện hành duy nhất của mã đó (DBR-24), giá trị cũ vào lịch sử, nhật ký ghi giá trị trước/sau, người, thời điểm, lý do.
3. **Given** giá trị mới vừa được lưu, **When** hệ thống kiểm tra quy tắc dùng tham số đó ở bất kỳ thời điểm nào sau thời điểm lưu, **Then** hệ thống dùng giá trị mới.
4. **Given** một kết quả đã sinh trước khi đổi tham số (ví dụ cảnh báo đã tạo, công việc đã sinh), **When** tham số thay đổi, **Then** kết quả đó không bị tính lại.
5. **Given** giá trị nhập sai kiểu hoặc ngoài khoảng hợp lệ của tham số, **When** lưu, **Then** hệ thống từ chối và giữ nguyên giá trị hiện hành.
6. **Given** một người dùng không phải quản lý viện, **When** cố thay đổi tham số, **Then** hệ thống từ chối (BR-M15-04).
7. **Given** quản lý viện chọn "Khôi phục mặc định", **When** xác nhận kèm lý do, **Then** việc khôi phục được ghi như một lần thay đổi mới trong lịch sử và nhật ký.
8. **Given** cần xác định một quy tắc đã dùng giá trị nào, **When** truy vấn giá trị của một mã tham số tại một thời điểm quá khứ, **Then** hệ thống trả về giá trị hiện hành tại thời điểm đó.

---

### User Story 5 - Yêu cầu phê duyệt dùng chung: tạo, duyệt, từ chối, áp dụng đúng một lần (Priority: P2)

Nhiều nghiệp vụ cần người có thẩm quyền duyệt trước khi thay đổi có hiệu lực. Người yêu cầu lập yêu cầu, gửi duyệt; người duyệt duyệt hoặc từ chối có lý do; yêu cầu đã duyệt được áp dụng đúng một lần vào ngày hiệu lực. Module cụ thể chỉ định nghĩa loại yêu cầu, nội dung, người được duyệt và tác động khi áp dụng; vòng đời chung do spec này quy định.

**Why this priority**: UC-14 phục vụ feature 004 và các module có duyệt khác. Vòng đời chung tránh mỗi module tự định nghĩa lại, nhưng có thể làm sau khi nhật ký và phân loại dữ liệu đã có.

**Independent Test**: Dùng một loại yêu cầu mẫu, đi hết các nhánh: Nháp → Chờ duyệt → Đã duyệt → Đã áp dụng; Chờ duyệt → Từ chối; Nháp → Hủy; kiểm tra chạy lại bước áp dụng không tạo tác động lần hai.

**Acceptance Scenarios**:

1. **Given** người dùng có quyền tạo loại yêu cầu đó, **When** lập yêu cầu, **Then** yêu cầu ở trạng thái Nháp, ghi người yêu cầu, loại, nội dung, ngày hiệu lực mong muốn, lý do.
2. **Given** yêu cầu ở Nháp, **When** người yêu cầu gửi duyệt, **Then** yêu cầu chuyển Chờ duyệt và nội dung bị khóa, không sửa được nữa.
3. **Given** yêu cầu ở Chờ duyệt, **When** người có quyền duyệt loại yêu cầu đó trong phạm vi dữ liệu của mình duyệt, **Then** yêu cầu chuyển Đã duyệt (chờ hiệu lực), ghi người duyệt, thời điểm, ý kiến.
4. **Given** yêu cầu ở Chờ duyệt đã quá CFG-M15-05 chưa có quyết định, **When** Bộ lập lịch kiểm tra, **Then** người có quyền duyệt được nhắc; **When** quá CFG-M15-06 vẫn chưa có quyết định, **Then** Quản lý viện được báo, và yêu cầu vẫn ở Chờ duyệt.
5. **Given** yêu cầu ở Chờ duyệt, **When** người duyệt từ chối mà không có lý do, **Then** hệ thống không cho từ chối; khi có lý do, yêu cầu chuyển Từ chối và không tạo tác động nào.
6. **Given** yêu cầu có ngày hiệu lực mong muốn là hôm nay hoặc đã qua, **When** được duyệt, **Then** hệ thống áp dụng ngay, chuyển Đã áp dụng, ngày hiệu lực thực tế là ngày duyệt; nếu ngày mong muốn đã qua thì tác động không tính lùi và yêu cầu lưu cả ngày mong muốn lẫn ngày thực tế.
7. **Given** yêu cầu Đã duyệt có ngày hiệu lực trong tương lai, **When** đến ngày hiệu lực, **Then** Bộ lập lịch hệ thống áp dụng yêu cầu, chuyển Đã áp dụng và ghi tham chiếu yêu cầu vào kết quả áp dụng.
8. **Given** yêu cầu Đã duyệt (chờ hiệu lực), **When** đến thời điểm áp dụng mà điều kiện áp dụng do module quy định không còn thỏa, **Then** yêu cầu chuyển Áp dụng không thành kèm lý do, không có tác động nào lên dữ liệu, người duyệt và người yêu cầu được báo.
9. **Given** yêu cầu Đã duyệt có ngày hiệu lực D nhưng Bộ lập lịch không chạy vào ngày D, **When** Bộ lập lịch chạy lại vào ngày sau đó, **Then** yêu cầu được áp dụng bù, tác động tính từ ngày D, nhật ký ghi ngày D và thời điểm áp dụng thực tế kèm đánh dấu "áp dụng bù".
10. **Given** yêu cầu Đã duyệt mà lần áp dụng bị lỗi sau khi một phần tác động đã được ghi, **When** lỗi xảy ra, **Then** mọi tác động của lần đó bị bỏ, yêu cầu vẫn ở Đã duyệt (chờ hiệu lực), nhật ký ghi lần lỗi và nguyên nhân, và lần chạy kế tiếp áp dụng lại toàn bộ.
11. **Given** yêu cầu đã ở Đã áp dụng, **When** bước áp dụng bị kích hoạt lại (chạy lại lịch, thao tác lặp), **Then** không có tác động nào được tạo thêm.
12. **Given** hai người duyệt cùng thao tác trên một yêu cầu Chờ duyệt gần như đồng thời, **When** cả hai gửi quyết định, **Then** chỉ quyết định đến trước được ghi nhận; người sau nhận thông báo yêu cầu đã được xử lý.
13. **Given** người yêu cầu cũng có quyền duyệt loại yêu cầu đó, **When** tự duyệt yêu cầu của chính mình mà không nhập lý do, **Then** hệ thống không cho duyệt; khi có lý do, yêu cầu chuyển Đã duyệt (chờ hiệu lực) và nhật ký đánh dấu "tự duyệt".
14. **Given** người dùng không có quyền duyệt loại yêu cầu đó hoặc yêu cầu nằm ngoài phạm vi dữ liệu của họ, **When** cố duyệt/từ chối, **Then** hệ thống từ chối (BR-M15-01).

---

### Edge Cases

- Mục danh mục đang Ngừng hiệu lực nhưng vẫn được một đối tượng nhóm 2 đang hiệu lực tham chiếu (ví dụ dịch vụ trong hợp đồng hiện hành): đối tượng nhóm 2 giữ nguyên tham chiếu; chỉ nghiệp vụ mới không chọn được mục đó.
- Hai quản lý viện cùng mở và đổi một tham số: người lưu sau phải thấy giá trị đã bị đổi và xác nhận lại trên giá trị mới; không được ghi đè âm thầm.
- Tham số dạng bảng (ví dụ CFG-M02-05, CFG-M01-05, CFG-M09-07): thay đổi một dòng được ghi lịch sử như thay đổi cả tham số, có đủ giá trị trước/sau.
- Tham số đổi đúng lúc một quy tắc đang được kiểm tra: mỗi lần kiểm tra dùng trọn vẹn một giá trị (trước hoặc sau), không trộn.
- Đính chính một bản ghi đã bị "Hủy ghi nhận": không cho phép đính chính nội dung nữa; nếu hủy nhầm thì phải ghi nhận lại thành bản ghi mới.
- Bản ghi thuộc danh sách bảo vệ (BR-M15-03) trong một kỳ chi phí đã chốt: đính chính vẫn được ghi, còn tác động tới chi phí do Module 11 xử lý bằng khoản điều chỉnh (DBR-17).
- Người yêu cầu muốn đổi nội dung sau khi đã gửi duyệt: phải hủy yêu cầu và lập yêu cầu mới.
- Yêu cầu Đã duyệt (chờ hiệu lực) nhưng đến ngày hiệu lực thì điều kiện áp dụng không còn thỏa (ví dụ giường đã có người): yêu cầu chuyển "Áp dụng không thành", không tạo tác động, người liên quan được báo; muốn tiếp tục phải lập yêu cầu mới (FR-038).
- Thay đổi do hệ thống tự thực hiện (sinh công việc, áp dụng yêu cầu, leo thang): nhật ký vẫn phải có người thực hiện là Bộ lập lịch hệ thống, không để trống.
- Nhật ký đã quá CFG-M15-04 nhưng hồ sơ liên quan vẫn còn trong thời hạn CFG-M15-03: nhật ký phải được giữ tiếp (FR-050).
- Dữ liệu đã hết thời hạn lưu giữ: chỉ Bộ lập lịch hệ thống loại bỏ, kèm nhật ký tóm tắt không bị loại bỏ; người dùng, kể cả Quản lý viện, không có thao tác xóa (FR-048, FR-051).
- Bản ghi đã hết hạn lưu nhưng còn được bản ghi chưa hết hạn tham chiếu (ví dụ liều thuốc được chi phí trỏ tới): được giữ tới khi cả chuỗi hết hạn, rồi loại bỏ cùng một đợt (FR-051a).
- Bản ghi ghi nhận ngoại tuyến được đồng bộ sau (Q-01): nhật ký lưu cả thời điểm trên thiết bị và thời điểm đồng bộ (DBR-25).

## Requirements *(mandatory)*

### Functional Requirements

#### A. Phân loại dữ liệu và thao tác được phép

- **FR-001**: Mỗi loại dữ liệu nghiệp vụ MUST thuộc đúng một trong ba nhóm: (1) Danh mục, (2) Nghiệp vụ có trạng thái, (3) Ghi nhận đã xác nhận. Spec của từng module MUST khai báo nhóm của mỗi loại dữ liệu nó sở hữu. *(Nguồn: mục 1.5; constitution III)*
- **FR-002**: Với nhóm 1, hệ thống MUST cho phép người có quyền cấu hình tạo, sửa và Ngừng hiệu lực; MUST chỉ cho xóa khi mục đó chưa từng được bản ghi nào tham chiếu. *(Nguồn: mục 1.5)*
- **FR-003**: Mục danh mục Ngừng hiệu lực MUST không được chọn cho nghiệp vụ mới, MUST vẫn hiển thị đúng trong mọi bản ghi đã tham chiếu nó, và MAY được kích hoạt lại bởi người có quyền cấu hình. *(Nguồn: mục 1.5)*
- **FR-004**: Với thuộc tính danh mục ảnh hưởng tới lịch sử đã phát sinh (ví dụ đơn giá), thay đổi MUST được thực hiện bằng phiên bản mới có ngày hiệu lực, không sửa phiên bản đã áp dụng; module sở hữu chỉ định thuộc tính nào theo phiên bản. *(Nguồn: mục 1.5, 6.4, DBR-08)*
- **FR-005**: Với nhóm 2, hệ thống MUST NOT cung cấp thao tác sửa trực tiếp nội dung hay trạng thái; mọi thay đổi MUST đi qua một lệnh nghiệp vụ có điều kiện hoặc tạo phiên bản mới có ngày hiệu lực. *(Nguồn: mục 1.5, 2.4 "Lệnh nghiệp vụ"; constitution III)*
- **FR-006**: Khi điều kiện của lệnh nghiệp vụ không thỏa, hệ thống MUST từ chối lệnh, nêu rõ điều kiện chưa thỏa và giữ nguyên đối tượng. *(Nguồn: mục 1.5)*
- **FR-007**: Mỗi lệnh nghiệp vụ nhóm 2 MUST lưu người thực hiện, thời điểm và lý do; lý do MUST bắt buộc. *(Nguồn: mục 1.5)*
- **FR-008**: Các phiên bản của cùng một đối tượng nhóm 2 MUST NOT chồng khoảng hiệu lực; tại một ngày bất kỳ, đối tượng có tối đa một phiên bản hiệu lực. *(Nguồn: mục 2.4 "Phiên bản"; khái quát từ DBR-08, DBR-11)*
- **FR-009**: Với nhóm 3, sau khi bản ghi được xác nhận, hệ thống MUST NOT cung cấp thao tác sửa hay xóa cho bất kỳ vai trò nào, kể cả quản lý viện; chỉ cho phép ghi thêm bản ghi mới và đính chính (mục C). *(Nguồn: mục 1.3, 1.5, 19.4; constitution III)*
- **FR-010**: Các bản ghi thuộc danh sách bảo vệ — liều thuốc đã xác nhận, sự cố khẩn cấp, chi phí đã chốt, bàn giao đã xác nhận, bàn giao đồ gửi — MUST được xử lý theo FR-009, và module sở hữu MUST NOT nới lỏng quy tắc này. *(Nguồn: BR-M15-03, 19.4)*

#### B. Tham số cấu hình

- **FR-011**: Hệ thống MUST quản lý đúng tập tham số ở Phụ lục 25 (cùng CFG-M15-03, CFG-M15-04 đề xuất ở FR-050; CFG-M15-05 và CFG-M15-06 đề xuất ở FR-031a), mỗi tham số có mã CFG, mô tả, kiểu giá trị, khoảng hợp lệ, giá trị mặc định và danh sách quy tắc sử dụng. Mã tham số MUST NOT được tạo hay xóa qua thao tác nghiệp vụ; mã mới chỉ xuất hiện khi tài liệu nghiệp vụ bổ sung. *(Nguồn: Phụ lục 25, mục 1.4)*
- **FR-012**: Mỗi mã tham số MUST có đúng một giá trị hiện hành tại mọi thời điểm; mọi giá trị trước đó MUST được lưu lịch sử. *(Nguồn: DBR-24)*
- **FR-013**: Chỉ Quản lý viện MUST được thay đổi giá trị tham số; mọi vai trò khác bị từ chối. *(Nguồn: BR-M15-04, UC-69, Permission Matrix dòng "Tài khoản, phân quyền, tham số")*
- **FR-014**: Mỗi lần thay đổi tham số MUST kèm lý do và MUST được ghi nhật ký với giá trị trước, giá trị sau, người thực hiện, thời điểm. *(Nguồn: BR-M15-04, 19.4)*
- **FR-015**: Hệ thống MUST kiểm tra giá trị mới theo kiểu và khoảng hợp lệ của tham số; giá trị không hợp lệ MUST bị từ chối và giá trị hiện hành giữ nguyên.
- **FR-016**: Giá trị mới MUST có hiệu lực ngay từ thời điểm lưu cho mọi lần kiểm tra quy tắc sau đó, không cần triển khai lại hệ thống. *(Nguồn: NFR-12, Phụ lục 25)*
- **FR-017**: Việc đổi tham số MUST NOT tính lại các kết quả đã sinh trước thời điểm đổi (công việc, cảnh báo, liều, chi phí…), trừ khi spec module quy định khác cho một tham số cụ thể.
- **FR-018**: Mỗi lần kiểm tra quy tắc MUST dùng trọn vẹn một giá trị của tham số (giá trị hiện hành tại thời điểm kiểm tra).
- **FR-019**: Hệ thống MUST cho phép tra cứu giá trị hiện hành của một mã tham số tại một thời điểm quá khứ bất kỳ.
- **FR-020**: "Khôi phục mặc định" MUST được ghi như một lần thay đổi giá trị bình thường (có lý do, có lịch sử, có nhật ký).
- **FR-021**: Khi hai người cùng đổi một tham số, lần lưu dựa trên giá trị đã cũ MUST bị từ chối và người đó MUST được hiển thị giá trị hiện hành mới trước khi lưu lại.
- **FR-022**: Với tham số dạng bảng (ví dụ CFG-M01-05, CFG-M02-05, CFG-M02-08, CFG-M09-07), thay đổi bất kỳ phần nào MUST được ghi như một lần thay đổi của cả tham số, có đủ nội dung trước/sau.

#### C. Đính chính bản ghi đã xác nhận

- **FR-023**: Hệ thống MUST cho phép tạo bản đính chính cho một bản ghi nhóm 3 đã xác nhận; bản đính chính MUST trỏ đúng một bản ghi gốc. *(Nguồn: mục 1.3, 2.4 "Đính chính", DBR-23)*
- **FR-024**: Bản đính chính MUST có lý do bắt buộc, người thực hiện, thời điểm và nội dung đúng thay cho nội dung sai. *(Nguồn: mục 1.3, BR-M15-03)*
- **FR-025**: Việc tạo bản đính chính MUST NOT thay đổi nội dung của bản ghi gốc. *(Nguồn: mục 1.5)*
- **FR-026**: Bản đính chính MUST chỉ được tạo theo một trong ba cách:
  - (a) Người đã ghi bản gốc, nếu hiện vẫn có quyền ghi nhận loại bản ghi đó, tạo trực tiếp.
  - (b) Với bản ghi gắn tầng/khu vực: người phụ trách ca hoặc trưởng tầng của tầng/khu vực đó, trong phạm vi dữ liệu hiện tại, tạo trực tiếp (BR-M15-01, mục 2.4).
  - (c) Với bản ghi không gắn tầng/khu vực (ví dụ bàn giao đồ gửi, bản ghi do Hành chính ghi; chi phí đã chốt là ngoại lệ, không có bản đính chính mà xử lý bằng khoản điều chỉnh theo FR-030 và feature 010): một nhân viên khác cùng vai trò với người ghi gốc và có quyền ghi nhận loại bản ghi đó lập bản đính chính dưới dạng yêu cầu phê duyệt loại "Đính chính" (mục D); bản đính chính MUST chỉ được lưu và có hiệu lực khi yêu cầu được Quản lý viện duyệt và chuyển Đã áp dụng. Trưởng tầng và người phụ trách ca MUST NOT đính chính loại bản ghi này (mục 19.3). Cách này giữ đúng Permission Matrix 4.4: nhân viên thực hiện (T), Quản lý viện duyệt (D).

  Bản ghi do Bộ lập lịch hệ thống tạo được đính chính theo (b) nếu gắn tầng/khu vực, theo (c) nếu không. Ngoài ba cách trên, nhân viên khác, kể cả cùng vai trò, MUST NOT đính chính bản ghi của người khác. Module sở hữu MAY thu hẹp thêm (ví dụ chỉ vai trò chuyên môn). *(Clarification 2026-09-25)*
- **FR-027**: Khi một bản ghi gốc có nhiều bản đính chính, tất cả MUST trỏ về cùng bản ghi gốc; giá trị hiện hành MUST là nội dung của bản đính chính mới nhất.
- **FR-028**: Khi xem bản ghi gốc, hệ thống MUST cho thấy bản ghi đã được đính chính, giá trị hiện hành và danh sách các lần đính chính theo thời gian.
- **FR-029**: Hệ thống MUST hỗ trợ bản đính chính loại "Hủy ghi nhận" cho bản ghi ghi nhầm hoàn toàn; bản ghi bị hủy MUST bị loại khỏi thống kê và tính toán từ đó về sau, nhưng MUST vẫn giữ nguyên và xem lại được. Bản ghi đã Hủy ghi nhận MUST NOT được đính chính nội dung thêm.
- **FR-030**: Mọi báo cáo, thống kê và quy tắc tự động đọc dữ liệu nhóm 3 MUST dùng giá trị hiện hành (sau đính chính); tác động dây chuyền cụ thể (ví dụ khoản điều chỉnh chi phí) do spec module sở hữu quy định. *(Nguồn: mục 1.3, DBR-17)*

#### D. Yêu cầu phê duyệt dùng chung

- **FR-031**: Hệ thống MUST cung cấp một vòng đời yêu cầu phê duyệt dùng chung cho mọi loại yêu cầu, theo bảng trạng thái dưới đây. Module sở hữu một loại yêu cầu MUST định nghĩa: nội dung yêu cầu, vai trò được tạo, vai trò được duyệt (theo cột D của Permission Matrix), điều kiện và tác động khi áp dụng. Ngoại lệ: các yêu cầu thuộc BR-M10-03, BR-M10-07 của feature 012 (thay đổi quyền người thân, danh sách được phép đón, dấu "được tự về", đặt thêm hoặc thôi người đại diện khi đã có người đại diện, ngoại lệ đón; quyền trên phiếu đăng ký người thân được tạo thẳng ở Hiệu lực) dùng vòng đời riêng "Chờ xác nhận → Hiệu lực / Từ chối / Hủy" do feature 012 định nghĩa (feature 012 FR-020, FR-024), nhưng vẫn theo quy tắc lý do bắt buộc, nhật ký và chống quyết định đồng thời của spec này. Đề nghị mua hộ của feature 010 (BR-M11-07, UC-79) cũng không phải một loại yêu cầu phê duyệt dùng chung. Nó có vòng đời riêng "Nháp → Được phép mua / Chờ đồng ý → Từ chối / Hủy / Đã mua", người quyết định là người đại diện hoặc Quản lý viện, và mượn FR-031a (nhắc theo CFG-M15-05, CFG-M15-06) cùng FR-040 của spec này. Hai loại yêu cầu của feature 015 dùng vòng đời chung với các sai khác sau, do feature 015 định nghĩa (feature 015 FR-022, FR-031): yêu cầu đổi ca không có Nháp, có thêm "Chờ người nhận đồng ý" trước "Chờ duyệt" và trạng thái cuối "Người nhận từ chối"; yêu cầu nghỉ đột xuất có trạng thái cuối "Đã áp dụng một phần" khi người duyệt duyệt một phần, và dạng "theo ngày" tự chuyển thành dạng "theo ca" khi lịch được công bố; cả hai có trạng thái cuối "Hết hiệu lực" do hệ thống đặt khi ca liên quan bắt đầu hoặc thay đổi (không phải vì quá hạn duyệt, nên không trái FR-031a). *(Nguồn: mục 1.5, 3.2 YEU_CAU_PHE_DUYET, 6.6, UC-14; đồng bộ spec 012, Q-118; đồng bộ spec 010)*
- **FR-031a**: Khi yêu cầu ở Chờ duyệt quá thời hạn CFG-M15-05 (đề xuất, mặc định \[48 giờ\]) kể từ lúc gửi duyệt, Bộ lập lịch hệ thống MUST nhắc những người có quyền duyệt loại yêu cầu đó trong phạm vi; quá thời hạn CFG-M15-06 (đề xuất, mặc định \[96 giờ\] kể từ lúc gửi duyệt, MUST lớn hơn CFG-M15-05) mà vẫn chưa có quyết định, hệ thống MUST báo Quản lý viện (nếu người duyệt không phải Quản lý viện). Yêu cầu MUST NOT tự chuyển Hủy hay Từ chối vì quá hạn. Module sở hữu MAY quy định thời hạn ngắn hơn cho loại yêu cầu của mình. *(Clarification 2026-09-25)*
- **FR-032**: Yêu cầu MUST lưu: loại, nội dung, người yêu cầu, thời điểm tạo, ngày hiệu lực mong muốn, ngày hiệu lực thực tế, lý do, người duyệt, thời điểm quyết định, ý kiến/lý do quyết định, trạng thái. *(Nguồn: mục 3.2 YEU_CAU_PHE_DUYET)*
- **FR-033**: Nội dung yêu cầu MUST chỉ sửa được ở trạng thái Nháp; từ Chờ duyệt trở đi nội dung bị khóa.
- **FR-034**: Chỉ người có quyền duyệt loại yêu cầu đó, và yêu cầu nằm trong phạm vi dữ liệu của họ, MUST được duyệt hoặc từ chối. *(Nguồn: BR-M15-01, 19.3)*
- **FR-035**: Người yêu cầu có quyền duyệt loại yêu cầu đó MAY tự duyệt yêu cầu của chính mình; khi tự duyệt, lý do MUST bắt buộc và bản ghi nhật ký MUST được đánh dấu "tự duyệt" để tra cứu riêng. *(Clarification 2026-09-25; đề xuất Q-10)*
- **FR-036**: Từ chối MUST có lý do; yêu cầu bị từ chối MUST NOT tạo bất kỳ tác động nào lên dữ liệu.
- **FR-037**: Yêu cầu Đã duyệt MUST được áp dụng đúng một lần: ngay khi duyệt nếu ngày hiệu lực mong muốn là hôm nay hoặc đã qua; do Bộ lập lịch hệ thống thực hiện vào ngày hiệu lực mong muốn nếu ngày đó ở tương lai. Ngày hiệu lực thực tế MUST là ngày hiệu lực mong muốn nếu yêu cầu được duyệt trước hoặc đúng ngày đó, và là ngày duyệt nếu được duyệt muộn hơn; tác động MUST NOT tính lùi trước ngày hiệu lực thực tế. Nếu cần tính lùi, module sở hữu MUST xử lý bằng khoản điều chỉnh hoặc bản đính chính, không sửa bản ghi đã xác nhận. Kích hoạt áp dụng lặp lại MUST NOT tạo thêm tác động. *(Nguồn: BR-M02-04, NFR-04, mục 1.5; Clarification 2026-09-25)*
- **FR-037a**: Nếu Bộ lập lịch không áp dụng được vào ngày hiệu lực (gián đoạn, bảo trì), yêu cầu MUST được áp dụng bù ngay lần chạy kế tiếp; tác động MUST tính từ ngày hiệu lực thực tế (FR-037), không từ ngày Bộ lập lịch chạy bù. Nhật ký MUST ghi cả ngày hiệu lực và thời điểm áp dụng thực tế, đánh dấu "áp dụng bù". Điều kiện áp dụng (FR-038) được kiểm tra tại thời điểm áp dụng bù. *(Nguồn: BR-M02-04; Clarification 2026-09-25)*
- **FR-037b**: Việc áp dụng một yêu cầu MUST theo nguyên tắc "hoặc toàn bộ, hoặc không": nếu có lỗi khi đang áp dụng, mọi tác động của lần áp dụng đó MUST bị bỏ, yêu cầu MUST giữ trạng thái Đã duyệt (chờ hiệu lực) và được áp dụng lại ở lần chạy kế tiếp của Bộ lập lịch theo FR-037a. Mỗi lần áp dụng lỗi MUST được ghi nhật ký kèm nguyên nhân. Lỗi kỹ thuật khác với trường hợp điều kiện áp dụng không thỏa (FR-038). *(Nguồn: FR-047; Clarification 2026-09-25)*
- **FR-038**: Khi đến thời điểm áp dụng mà điều kiện áp dụng do module quy định không còn thỏa, hệ thống MUST chuyển yêu cầu sang trạng thái kết thúc "Áp dụng không thành", ghi điều kiện không thỏa làm lý do, không tạo bất kỳ tác động nào, và báo cho người duyệt và người yêu cầu. Muốn tiếp tục thì MUST lập yêu cầu mới. *(Clarification 2026-09-25; đề xuất Q-11)*
- **FR-039**: Kết quả áp dụng (ví dụ phụ lục hợp đồng, phiên bản mới) MUST tham chiếu tới yêu cầu phê duyệt đã tạo ra nó. *(Nguồn: DBR-07)*
- **FR-040**: Khi có nhiều quyết định gửi gần như đồng thời trên cùng một yêu cầu, chỉ quyết định đầu tiên MUST được ghi nhận; các quyết định sau MUST bị từ chối kèm thông báo yêu cầu đã được xử lý.
- **FR-041**: Mỗi lần chuyển trạng thái của yêu cầu MUST được ghi nhật ký (mục E).

**Bảng trạng thái yêu cầu phê duyệt** *(khái quát từ 6.6)*:

| Trạng thái hiện tại | Lệnh | Trạng thái kế tiếp | Ai thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Lập yêu cầu | Nháp | Vai trò được tạo loại yêu cầu | Có quyền tạo loại đó | Lưu nội dung, người yêu cầu, lý do |
| Nháp | Gửi duyệt | Chờ duyệt | Người yêu cầu | Nội dung đủ theo module | Khóa nội dung |
| Nháp | Hủy | Hủy | Người yêu cầu | — | Không tác động |
| Chờ duyệt | Hủy | Hủy | Người yêu cầu | Chưa có quyết định; có lý do | Không tác động |
| Chờ duyệt | Duyệt | Đã duyệt (chờ hiệu lực) | Người có quyền duyệt loại đó trong phạm vi (kể cả chính người yêu cầu) | FR-034; nếu tự duyệt thì bắt buộc lý do (FR-035) | Ghi người duyệt, thời điểm, ý kiến; đánh dấu "tự duyệt" nếu có |
| Chờ duyệt | Từ chối | Từ chối | Người có quyền duyệt loại đó trong phạm vi | Có lý do | Không tác động |
| Đã duyệt (chờ hiệu lực) | Áp dụng | Đã áp dụng | Hệ thống (ngay khi duyệt, hoặc Bộ lập lịch vào ngày hiệu lực hay lần chạy bù kế tiếp) | Đến hoặc đã qua ngày hiệu lực; điều kiện module thỏa | Tác động do module quy định, đúng một lần, tính từ ngày hiệu lực (FR-037a) |
| Đã duyệt (chờ hiệu lực) | Hủy | Hủy | Người có quyền duyệt loại đó | Chưa áp dụng; có lý do | Không tác động |
| Đã duyệt (chờ hiệu lực) | Áp dụng | Áp dụng không thành | Hệ thống | Đến thời điểm áp dụng; điều kiện module không thỏa | Không tác động; ghi lý do; báo người duyệt và người yêu cầu (FR-038) |
| Đã duyệt (chờ hiệu lực) | Áp dụng | Đã duyệt (chờ hiệu lực) | Hệ thống | Lỗi khi đang áp dụng | Bỏ mọi tác động của lần đó; ghi nhật ký lỗi; thử lại lần chạy kế tiếp (FR-037b) |

Đã áp dụng, Áp dụng không thành, Từ chối, Hủy là trạng thái kết thúc; không có lệnh nào đưa yêu cầu ra khỏi các trạng thái này.

#### E. Nhật ký hệ thống

- **FR-042**: Mọi thay đổi trên dữ liệu nhóm 2 và nhóm 3 (bao gồm lệnh nghiệp vụ, phiên bản mới, xác nhận, đính chính, chuyển trạng thái yêu cầu phê duyệt) MUST có bản ghi nhật ký tương ứng. *(Nguồn: DBR-23, mục 1.3)*
- **FR-043**: Thay đổi dữ liệu nhóm 1 (tạo, sửa, ngừng hiệu lực, kích hoạt lại, xóa), thay đổi tham số và thay đổi phân quyền MUST được ghi nhật ký. *(Nguồn: mục 1.3, BR-M15-04)*
- **FR-043a**: Spec module MAY yêu cầu ghi nhật ký cho hành động **không thay đổi dữ liệu**; hiện gồm: đăng nhập thành công và thất bại (feature 002 FR-015) và xuất báo cáo ra file (feature 016 FR-062). Bản ghi loại này dùng cùng khung FR-044 → FR-051: "dữ liệu trước/sau" để trống; phần chi tiết riêng (ví dụ bộ lọc, khoảng, phạm vi, số dòng, dấu "có định danh" của lần xuất) do spec sở hữu quy định; lý do bắt buộc hay không theo FR-044. Lần xuất báo cáo là một bản ghi nhật ký, không phải dữ liệu nghiệp vụ nhóm 1, 2, 3 riêng, nên không sinh thêm bản ghi nhật ký thứ hai theo FR-042. Việc xem dữ liệu, dashboard, báo cáo trên hệ thống MUST NOT được ghi nhật ký, trừ lần người thân xem hồ sơ sức khỏe (NFR-08, feature 012). *(Nguồn: 19.4; NFR-08; đồng bộ spec 002, 016; Q-193)*
- **FR-044**: Mỗi bản ghi nhật ký MUST gồm: người thực hiện; thời điểm; hành động; đối tượng (loại và định danh); dữ liệu trước; dữ liệu sau; lý do (bắt buộc với lệnh nhóm 2, đính chính, từ chối, hủy, tự duyệt, thay đổi tham số, và xuất báo cáo có thông tin định danh người cao tuổi — lý do là mục đích xuất, feature 016 FR-061, Q-198; tùy chọn với hành động khác). *(Nguồn: 19.4; Q-198)*
- **FR-045**: Với thay đổi do hệ thống tự thực hiện, người thực hiện MUST là "Bộ lập lịch hệ thống" (AC-11) và nhật ký MUST tham chiếu căn cứ (quy tắc hoặc yêu cầu phê duyệt đã dẫn tới thay đổi).
- **FR-046**: Thời điểm trong nhật ký MUST theo múi giờ Asia/Ho_Chi_Minh và lấy theo đồng hồ của hệ thống, không theo thiết bị; với bản ghi ngoại tuyến, nhật ký MUST lưu cả thời điểm trên thiết bị và thời điểm đồng bộ. *(Nguồn: DBR-25, NFR-09)*
- **FR-047**: Thay đổi và bản ghi nhật ký tương ứng MUST cùng được lưu hoặc cùng không được lưu; không có thay đổi nào được lưu mà thiếu nhật ký. Với lần xuất báo cáo (FR-043a), file MUST NOT được giao cho người dùng nếu bản ghi nhật ký của lần xuất chưa được lưu.
- **FR-048**: Nhật ký MUST chỉ ghi thêm; MUST NOT có thao tác sửa hay xóa cho bất kỳ vai trò người dùng nào, kể cả Quản lý viện. Ngoại lệ duy nhất là việc loại bỏ theo thời hạn lưu giữ ở FR-051.
- **FR-049**: Quản lý viện MUST tra cứu được nhật ký toàn viện theo đối tượng, người thực hiện, loại hành động và khoảng thời gian. Nhân viên (AC-02 → AC-09) MUST chỉ xem được lịch sử thay đổi của đối tượng mà họ hiện có quyền xem, trong phạm vi dữ liệu hiện tại của họ (BR-M15-01); trong mỗi bản ghi nhật ký, giá trị trước/sau của một trường MUST chỉ hiện khi người xem hiện có quyền xem trường đó, nếu không thì chỉ hiện "đã thay đổi" (mục 19.3). Người thân (AC-10) MUST NOT xem nhật ký; họ chỉ thấy giá trị hiện hành của dữ liệu được cấp quyền (sau đính chính). *(Clarification 2026-09-25)*
- **FR-050**: Hồ sơ người cao tuổi, sức khỏe, thuốc, sự cố MUST được lưu giữ tối thiểu theo CFG-M15-03 (mặc định \[10 năm\] sau khi kết thúc lưu trú); nhật ký MUST được lưu giữ tối thiểu theo CFG-M15-04 (mặc định \[10 năm\]). Bản ghi nhật ký của một hồ sơ MUST NOT bị loại bỏ trước khi chính hồ sơ đó hết thời hạn lưu giữ, bất kể giá trị CFG-M15-04. *(Nguồn: NFR-07; Clarification 2026-09-25, Q-04; hai mã CFG là đề xuất bổ sung Phụ lục 25)*
- **FR-051**: Việc thay đổi CFG-M15-03 hoặc CFG-M15-04 MUST tuân theo mục B như mọi tham số khác. Dữ liệu và nhật ký đã hết thời hạn lưu giữ MUST chỉ được loại bỏ bởi Bộ lập lịch hệ thống (AC-11), không có thao tác loại bỏ cho người dùng. Mỗi đợt loại bỏ MUST thỏa FR-050 theo giá trị tham số hiện hành tại thời điểm loại bỏ và MUST ghi một bản nhật ký tóm tắt gồm: loại dữ liệu, số lượng bản ghi, khoảng thời gian của dữ liệu bị loại bỏ, căn cứ (mã CFG và giá trị áp dụng), thời điểm thực hiện. Bản nhật ký tóm tắt này MUST NOT bị loại bỏ. *(Clarification 2026-09-25)*
- **FR-051a**: Bản ghi đã hết thời hạn lưu giữ nhưng còn được một bản ghi khác chưa hết hạn tham chiếu tới (ví dụ chi phí trỏ về liều thuốc, bản đính chính trỏ về bản gốc, yêu cầu phê duyệt trỏ về kết quả áp dụng) MUST được giữ lại cho tới khi mọi bản ghi tham chiếu tới nó cũng hết hạn. Khi đó cả chuỗi (bản ghi gốc, các bản đính chính, nhật ký liên quan) MUST được loại bỏ trong cùng một đợt; không đợt loại bỏ nào được để lại bản ghi trỏ tới bản ghi đã bị loại bỏ. *(Nguồn: mục 1.3, DBR-15, DBR-23; Clarification 2026-09-25)*


**Danh mục loại yêu cầu phê duyệt** *(tổng hợp 2026-09-29, checklist cross-feature CHK028)*. Bảng chỉ để tra cứu; khi khác nhau, spec sở hữu là căn cứ.

| Loại | Spec sở hữu (căn cứ) | Người lập | Người duyệt | Vòng đời |
| --- | --- | --- | --- | --- |
| Thay đổi lưu trú (gồm đổi mức chăm sóc, gia hạn, đổi phòng/giường khác giá) | 004 FR-037, FR-039, FR-043; 001; 003 FR-030 | Hành chính; Bác sĩ (chỉ đổi mức chăm sóc); Hệ thống (đánh giá lại, chuyển giường gấp, Q-52) | Quản lý viện | Chung (FR-031) |
| Điều chỉnh điểm ưu tiên danh sách chờ | 004 FR-011 | Hành chính | Quản lý viện | Chung |
| Duyệt điều khoản hợp đồng khác chuẩn | 004 FR-025a | Hành chính | Quản lý viện | Chung |
| Quyết định giữ giường khi vắng | 004 (bảng phân nhóm dữ liệu) | Hệ thống | Quản lý viện | Chung |
| Ngoại lệ điều kiện kết thúc lưu trú | 004 User Story 7, FR-063 → FR-065 | Hành chính | Quản lý viện | Chung |
| Nhập bù phân bổ giường lùi quá CFG-M03-07 | 003 FR-027 (Q-43) | Hành chính, Trưởng tầng | Quản lý viện | Chung |
| Xử lý đồ không người nhận | 013 FR-027a (Q-152) | Hành chính | Quản lý viện (không tự duyệt) | Chung |
| Yêu cầu đổi ca; yêu cầu nghỉ đột xuất | 015 (bảng FR-022, FR-031) | Nhân viên; người lập thay | Trưởng tầng (tầng được giao); Quản lý viện (ca toàn viện, Q-177) | Chung, có ngoại lệ khai báo ở 015 |
| Hoàn tiền, hoàn cọc, điều chỉnh, giao dịch đảo | 017 FR-013 | Kế toán | Quản lý viện | Chung, trạng thái Chờ duyệt → Đã xác nhận / Từ chối / Đã hủy (Q-229) |
| Thay đổi quyền người thân, danh sách được phép đón, ngoại lệ đón, dấu "được tự về" | 012 FR-020, FR-024 (Q-118, Q-125) | Người đại diện, Hành chính | Theo 012 | Riêng: Chờ xác nhận → Hiệu lực / Từ chối / Hủy |
| Đề nghị mua hộ | 010 (BR-M11-07, UC-79) | Theo 010 | Người đại diện hoặc Quản lý viện, theo 010 | Riêng (FR-031 ghi rõ không dùng vòng đời chung) |
| Xác nhận người nhận khác; đồng ý cho tự giữ | 013 FR-017b | Theo 013 | Theo 013 | Riêng |
| Phiếu kiểm kê kho; chốt quỹ ngày | 019 FR-010 (Q-233); 017 (Q-226) | Nhân viên bếp; Kế toán | Quản lý viện | Riêng (bảng trạng thái của spec sở hữu) |
### Key Entities *(include if feature involves data)*

- **Tham số (THAM_SO)** – nhóm 1: mã CFG, mô tả, kiểu giá trị, khoảng hợp lệ, giá trị mặc định, giá trị hiện hành, danh sách quy tắc sử dụng. Mỗi mã có đúng một giá trị hiện hành (DBR-24).
- **Lịch sử giá trị tham số**: mã CFG, giá trị, thời điểm bắt đầu hiệu lực, thời điểm kết thúc, người đổi, lý do. Cho phép xác định giá trị tại một thời điểm quá khứ.
- **Yêu cầu phê duyệt (YEU_CAU_PHE_DUYET)** – nhóm 2: loại, nội dung, người yêu cầu, ngày hiệu lực mong muốn, ngày hiệu lực thực tế, lý do, người duyệt, thời điểm và lý do quyết định, trạng thái; liên kết tới kết quả áp dụng.
- **Đính chính (DINH_CHINH)** – nhóm 3: bản ghi gốc (đúng một), loại (sửa nội dung / Hủy ghi nhận), nội dung đúng, lý do, người thực hiện, thời điểm.
- **Nhật ký (NHAT_KY)** – nhóm 3: người thực hiện (người dùng hoặc Bộ lập lịch hệ thống), thời điểm (và thời điểm thiết bị nếu ngoại tuyến), hành động, đối tượng, dữ liệu trước, dữ liệu sau, lý do, căn cứ, phần chi tiết riêng của hành động (FR-043a). Hành động gồm thay đổi dữ liệu và hành động không thay đổi dữ liệu (đăng nhập, xuất báo cáo).
- **Nhóm dữ liệu** (khái niệm): thuộc tính của mỗi loại dữ liệu, quyết định thao tác được phép theo mục A.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% thay đổi trên dữ liệu nhóm 2 và nhóm 3 trong một đợt kiểm thử có bản ghi nhật ký tương ứng đủ các trường ở mục 19.4 (kiểm bằng đối chiếu toàn bộ thay đổi với nhật ký).
- **SC-002**: 0 bản ghi nhóm 3 đã xác nhận bị thay đổi nội dung hoặc biến mất sau khi tạo, trong mọi kịch bản kiểm thử, kể cả với tài khoản quản lý viện.
- **SC-003**: 0 yêu cầu phê duyệt tạo tác động nhiều hơn một lần khi bước áp dụng được kích hoạt lặp lại ít nhất 3 lần.
- **SC-004**: Sau khi quản lý viện lưu giá trị tham số mới, 100% lần kiểm tra quy tắc bắt đầu từ thời điểm lưu dùng giá trị mới, không cần khởi động lại hay triển khai lại hệ thống.
- **SC-005**: Với bất kỳ mã tham số và thời điểm quá khứ nào trong đợt kiểm thử, giá trị tra cứu được khớp 100% với giá trị đã hiện hành tại thời điểm đó.
- **SC-006**: Quản lý viện tìm được toàn bộ lịch sử thay đổi của một đối tượng bất kỳ (ai, lúc nào, trước/sau, lý do) trong không quá 1 phút.
- **SC-007**: Nhân viên tạo xong một bản đính chính cho bản ghi sai trong không quá 1 phút, và người xem sau đó nhận ra bản ghi đã được đính chính ngay trên màn hình xem bản ghi, không cần tra nhật ký.
- **SC-008**: 100% lệnh nghiệp vụ nhóm 2, bản đính chính, lần từ chối yêu cầu và lần đổi tham số thiếu lý do bị hệ thống từ chối.

## Assumptions

- Thư mục feature dùng số `000` theo bản đồ feature trong tài liệu (UC-14, UC-69, Q-04 đều ghi feature 000) và theo `.specify/feature.json` đã có, thay vì số tiếp theo tự sinh.
- Quyền và phạm vi dữ liệu được kiểm tra theo BR-M15-01 do feature 002 cung cấp; spec này chỉ quy định kết quả kiểm tra phải được áp dụng.
- Trước khi xác nhận, bản ghi nhóm 3 ở dạng nháp có được chỉnh sửa hay không do module sở hữu quy định (ví dụ bản nháp bàn giao, BR-M09-06).
- Mục danh mục Ngừng hiệu lực có thể được kích hoạt lại; mục 1.5 không cấm điều này.
- Lý do bắt buộc khi đổi tham số (mục 19.4 chỉ ghi "lý do nếu có"); chọn bắt buộc vì tham số ảnh hưởng tới nhiều quy tắc chăm sóc và an toàn.
- Mọi bản đính chính trỏ về bản ghi gốc ban đầu (không trỏ về bản đính chính trước) để thỏa DBR-23 "mỗi bản đính chính trỏ đúng một bản ghi gốc"; loại "Hủy ghi nhận" được suy ra từ mục 1.3 (sửa sai bằng bản ghi mới) cho trường hợp ghi nhầm đối tượng.
- Yêu cầu Đã duyệt nhưng chưa áp dụng được hủy bởi người có quyền duyệt kèm lý do; 6.6 có trạng thái Hủy nhưng không nói rõ hủy từ trạng thái nào.
- Người yêu cầu được hủy yêu cầu ở Nháp hoặc Chờ duyệt; không được hủy sau khi đã có quyết định.
- Thông báo cho người duyệt khi có yêu cầu mới, và cho người yêu cầu khi có quyết định, thuộc feature 009.

## Điểm cần báo lại về tài liệu nguồn

Ngày 2026-09-25, các điểm đã chốt đã được đưa vào `docs/nghiep-vu.md` và `docs/phan-tich-yeu-cau.md` theo yêu cầu. Danh sách dưới đây ghi nơi đã phản ánh và các điểm còn mở.

**Đã phản ánh vào tài liệu nguồn**

1. Thời hạn lưu giữ hồ sơ và nhật ký (Q-04): NFR-07 sửa nhật ký thành \[10 năm\], gắn CFG-M15-03, CFG-M15-04; Q-04 chuyển sang mục 24.2; Phụ lục 25 có hai tham số mới.
2. Quyền xem nhật ký: thêm dòng "Nhật ký thay đổi" vào Permission Matrix 4.4; quyền xem ghi ở 19.4.
3. Lý do bắt buộc với lệnh nhóm 2, đính chính, từ chối, hủy, đổi tham số: 1.5 và 19.4 (thay cho "lý do nếu có").
4. Tự duyệt (Q-10) và trạng thái "Áp dụng không thành" (Q-11): 6.6 (vòng đời yêu cầu phê duyệt chung) và mục 24.2.
5. Tham số CFG-M15-05, CFG-M15-06 (nhắc người duyệt, báo Quản lý viện): 6.6 và Phụ lục 25.
6. Quy tắc đính chính (ai được tạo, loại yêu cầu "Đính chính" cho bản ghi không gắn tầng, "Hủy ghi nhận") và loại bỏ theo chuỗi tham chiếu: 1.5, NFR-07; Permission Matrix 4.4 có dòng "Đính chính bản ghi không gắn tầng" (chú thích ¹³).

**Còn mở**

1. **[Đã xử lý: Q-118, 24.2]** **Vòng đời yêu cầu phê duyệt và BR-M10-07**: đã chốt ở spec 012 (Q-118) — yêu cầu của BR-M10-07 dùng vòng đời riêng "Chờ xác nhận → Hiệu lực / Từ chối / Hủy", là ngoại lệ của FR-031. Cần ghi ngoại lệ vào 6.6 và thêm trạng thái Hủy vào BR-M10-07.
