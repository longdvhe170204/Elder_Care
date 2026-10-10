# Feature Specification: Khai báo tạm trú, lưu trú

**Feature Branch**: `018-residence-registration`

**Created**: 2026-09-28

**Status**: Draft

**Input**: User description: "Khai báo tạm trú, thông báo lưu trú cho người cao tuổi nội trú theo docs/nghiep-vu.md mục 6.10 (Module 02, BR-M02-11 → BR-M02-14), 6.8 (khai báo xóa tạm trú khi kết thúc lưu trú, qua đời), mục 23 (cơ quan công an, không tích hợp), BF-01 bước 15a, BF-10 bước 6b; quyết định Q-208, Q-215 (chốt 2026-09-29); UC-80, UC-81; DBR-33; CFG-M02-11 → CFG-M02-13. Hành chính ghi nhận việc khai báo với công an; hệ thống xác định loại khai báo, nhắc hạn, gia hạn và khai báo xóa; không nộp hồ sơ thay, không kết nối Cổng dịch vụ công."

## Clarifications

### Session 2026-09-29

- Q: Mốc đăng ký tạm trú tính thế nào? → A: Thời gian lưu trú dự kiến từ CFG-M02-11 (30 ngày) trở lên, tính tổng từ ngày bắt đầu hợp đồng đầu tiên tới ngày kết thúc dự kiến mới nhất, gồm cả các phụ lục gia hạn (Q-215).
- Q: Gia hạn hợp đồng ngắn ngày mà tổng thời gian vẫn dưới ngưỡng thì có khai báo mới không? → A: Có. Hệ thống tạo khai báo "Thông báo lưu trú" mới ở "Cần khai báo" khi phụ lục gia hạn được áp dụng; khai báo mới Đã xác nhận thì khai báo cũ chuyển Được thay thế (Q-215).
- Q: Người thường trú cùng xã/phường với viện có phải đăng ký tạm trú không? → A: Không; người này luôn dùng loại Thông báo lưu trú, bất kể thời gian lưu trú; Hành chính xác nhận "thường trú cùng xã/phường với viện" khi khai báo còn "Cần khai báo" (Q-215).
- Q: "Hợp đồng đầu tiên" khi cộng dồn là hợp đồng nào nếu có hợp đồng mới trong cùng hồ sơ? → A: Hợp đồng đầu của chuỗi hợp đồng nối tiếp không gián đoạn trong cùng hồ sơ (hợp đồng mới bắt đầu ngay ngày sau khi hợp đồng trước kết thúc); có khoảng trống, hoặc hồ sơ mới theo Q-12, thì tính lại từ đầu (Q-241).

### Cập nhật 2026-10-06 (đồng bộ Q-280 → Q-288, quyết định của chủ dự án)

- **Q-286**: bán trú không qua đêm; người bán trú cần ở lại qua đêm được ghi bằng nội trú ngắn ngày (hợp đồng hoặc phụ lục riêng, feature 004 FR-019). Người đó đã Hoàn tất tiếp nhận từ trước nên FR-001 không tự kích hoạt; thêm FR-001a để khoảng nội trú này cũng có khai báo như người vào ở nội trú (6.10).

### Cập nhật 2026-10-06, lượt 2 (đồng bộ Q-306: khai báo do Hành chính lập khi được nhắc)

- **Q-306**: hệ thống không tự tạo khai báo. Khi Hoàn tất tiếp nhận nội trú, khi phần nội trú có hiệu lực, khi gia hạn hợp đồng mà người đó chỉ có thông báo lưu trú, và khi khai báo Hết hạn mà chưa có khai báo đang xử lý, hệ thống nhắc Hành chính lập khai báo. Lệnh "Lập khai báo" tạo khai báo ở "Cần khai báo"; loại khai báo được xác định theo quy tắc khi lập (BR-M02-11, 14). Hạn khai báo tính từ mốc phát sinh nghĩa vụ, không từ lúc lập, nên lập muộn không kéo dài hạn. Bỏ "việc khai báo xóa tạm trú" do hệ thống tạo: tình trạng "cần khai báo xóa" là số liệu tính, hệ thống nhắc Hành chính tới khi khai báo chuyển Đã xóa (BR-M02-13).
- Viết lại: Phạm vi mục 1, 4, 5; User Story 1, 3 (kịch bản 3, 4, 6), 4; Edge Cases (Hủy tiếp nhận); bảng nhóm dữ liệu; FR-001, FR-001a, FR-002, FR-005 (dòng lập khai báo), FR-006 → FR-008, FR-010, FR-011; Key Entities; SC-001, SC-002.

### Cập nhật 2026-10-10, lượt 2 (đồng bộ Q-336)

- **Q-336**: mốc nghĩa vụ khi bán trú chuyển nội trú là ngày hiệu lực của phụ lục hoặc ngày bắt đầu hợp đồng nội trú mới. Sửa FR-001a, FR-002.

## Phạm vi

**Trong phạm vi** (Module 02, mục 6.10; UC-80, UC-81; nhóm chức năng "Lưu trú và tạm trú", 1.6):

1. Nhắc Hành chính lập khai báo khi người nội trú Hoàn tất tiếp nhận; Hành chính lập khai báo ở "Cần khai báo", loại khai báo theo thời gian lưu trú dự kiến (BR-M02-11, CFG-M02-11, Q-215; **sửa, 2026-10-06, Q-306**).
2. Hành chính ghi đã nộp, kết quả được chấp nhận hoặc bị từ chối, kèm bằng chứng (6.10, UC-80).
3. Nhắc khai báo quá hạn; nhắc gia hạn đăng ký tạm trú trước ngày hết hạn; chuyển Hết hạn (BR-M02-11, BR-M02-12, CFG-M02-12, CFG-M02-13, UC-81).
4. Nhắc lập khai báo mới khi gia hạn hợp đồng ngắn ngày, gồm chuyển từ thông báo lưu trú sang đăng ký tạm trú khi vượt ngưỡng (BR-M02-14).
5. Nhắc khai báo xóa tạm trú khi Kết thúc lưu trú hoặc Qua đời (BR-M02-13).

**Ngoài phạm vi**:

- Quy tắc dùng chung (nhóm dữ liệu, lý do bắt buộc, đính chính, nhật ký, tham số, "hoặc toàn bộ, hoặc không", Bộ lập lịch chạy lại không tạo trùng): feature 000.
- Hợp đồng, thời hạn hợp đồng, gia hạn bằng phụ lục, Hoàn tất tiếp nhận, Kết thúc lưu trú, Ghi nhận qua đời: feature 001, 004. Spec này **nhận** sự kiện.
- Nộp hồ sơ trên Cổng dịch vụ công hoặc tại cơ quan công an, khai tử: làm ngoài hệ thống (23). Spec này chỉ ghi kết quả.
- Gửi thông báo: feature 009. Dashboard "khai báo tạm trú cần làm" (18.5): feature 016.
- Khai báo tạm vắng của người cao tuổi với nơi thường trú: không thuộc hệ thống.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Hành chính được nhắc và lập khai báo khi người nội trú vào ở (Priority: P1)

Khi người cao tuổi nội trú Hoàn tất tiếp nhận, hệ thống nhắc Hành chính lập khai báo; hệ thống không tự tạo khai báo (Q-306). Hành chính lập khai báo ở "Cần khai báo". Loại khai báo được xác định khi lập theo thời gian lưu trú dự kiến của hợp đồng: từ 30 ngày trở lên là đăng ký tạm trú, dưới 30 ngày là thông báo lưu trú. Người bán trú không cần khai báo. Hành chính thấy danh sách người cần khai báo kèm hạn, gồm cả người chưa được lập khai báo.

**Why this priority**: Góp ý nghiệp vụ Q-208: viện phải khai báo cư trú cho người vào ở; thiếu khai báo là rủi ro pháp lý cho viện.

**Independent Test**: Hoàn tất tiếp nhận ba người: nội trú dài hạn, nội trú ngắn ngày 14 ngày, bán trú. Kiểm tra lời nhắc, lập khai báo cho từng người nội trú và kiểm tra loại khai báo.

**Acceptance Scenarios**:

1. **Given** A nội trú dài hạn có hợp đồng từ 01/10/2026 đến 30/09/2027, **When** Hoàn tất tiếp nhận ngày 01/10, **Then** Hành chính được nhắc lập khai báo cho A, hạn khai báo 02/10 (CFG-M02-12 mặc định \[1 ngày\]); chưa có khai báo nào. **When** Hành chính lập khai báo cho A, **Then** có một khai báo "Đăng ký tạm trú" ở "Cần khai báo", hạn khai báo vẫn là 02/10 (BR-M02-11, Q-215, Q-306).
2. **Given** B nội trú ngắn ngày 14 ngày, CFG-M02-11 mặc định \[30 ngày\], **When** Hoàn tất tiếp nhận và Hành chính lập khai báo, **Then** khai báo là "Thông báo lưu trú" ở "Cần khai báo".
3. **Given** C bán trú, **When** Hoàn tất tiếp nhận, **Then** không có lời nhắc; lệnh lập khai báo cho C bị chặn.
4. **Given** A cần khai báo với hạn 02/10 (đã hoặc chưa được lập khai báo), **When** hết ngày 02/10 mà chưa ghi đã nộp, **Then** Hành chính được nhắc mỗi ngày và Quản lý viện được báo (BR-M02-11).
5. **Given** khai báo chưa có, **When** hành chính thực hiện Hoàn tất tiếp nhận, **Then** lệnh không bị chặn vì khai báo (Q-215).
6. **Given** F nội trú dài hạn, thường trú cùng phường với viện, có khai báo "Đăng ký tạm trú" ở "Cần khai báo", **When** Hành chính xác nhận "thường trú cùng xã/phường với viện" kèm ghi chú căn cứ, **Then** loại khai báo chuyển "Thông báo lưu trú", hạn khai báo giữ nguyên; khi F được gia hạn hợp đồng, khai báo mới vẫn là "Thông báo lưu trú" (Q-215).

---

### User Story 2 - Hành chính ghi nộp và kết quả khai báo (Priority: P1)

Sau khi khai báo trên Cổng dịch vụ công hoặc tại công an, Hành chính ghi đã nộp (ngày, kênh, cơ quan, mã hồ sơ), rồi ghi kết quả: được chấp nhận (kèm bằng chứng, với đăng ký tạm trú ghi ngày hết hạn) hoặc bị từ chối (kèm lý do, khai báo quay về "Cần khai báo").

**Why this priority**: Đây là thao tác chính của UC-80; không có nó hệ thống không biết việc khai báo đã xong.

**Independent Test**: Ghi nộp rồi xác nhận một khai báo; ghi nộp rồi từ chối một khai báo khác; thử ghi kết quả khi chưa ghi nộp.

**Acceptance Scenarios**:

1. **Given** khai báo của A ở "Cần khai báo", **When** Hành chính ghi đã nộp ngày 02/10, kênh "Cổng dịch vụ công", cơ quan "Công an phường X", mã hồ sơ, **Then** khai báo chuyển "Đã nộp"; việc nhắc quá hạn dừng.
2. **Given** khai báo "Đã nộp", **When** Hành chính ghi được chấp nhận kèm ảnh kết quả và ngày hết hạn tạm trú 30/09/2027, **Then** khai báo chuyển "Đã xác nhận" (6.10).
3. **Given** khai báo "Đã nộp", **When** Hành chính ghi bị từ chối kèm lý do "thiếu giấy tờ", **Then** khai báo về "Cần khai báo", hạn khai báo mới là ngày ghi từ chối cộng CFG-M02-12.
4. **Given** khai báo "Cần khai báo", **When** Hành chính ghi được chấp nhận mà chưa ghi đã nộp, **Then** hệ thống chặn.
5. **Given** khai báo "Đăng ký tạm trú" "Đã nộp", **When** Hành chính ghi được chấp nhận mà thiếu ngày hết hạn hoặc thiếu bằng chứng, **Then** hệ thống chặn.
6. **Given** Điều dưỡng, Trưởng tầng hoặc Kế toán, **When** tìm cách ghi khai báo, **Then** hệ thống từ chối; Quản lý viện chỉ xem (Phụ lục 27).

---

### User Story 3 - Gia hạn đăng ký tạm trú và chuyển loại khi gia hạn hợp đồng (Priority: P2)

Trước ngày hết hạn tạm trú 30 ngày, nếu người cao tuổi vẫn lưu trú, Hành chính được nhắc lập khai báo gia hạn. Khai báo gia hạn được xác nhận thì thay khai báo cũ. Qua ngày hết hạn mà chưa có khai báo gia hạn được xác nhận, khai báo cũ chuyển Hết hạn và Quản lý viện được báo. Người ngắn ngày được gia hạn hợp đồng vượt ngưỡng thì phải chuyển sang đăng ký tạm trú.

**Why this priority**: Giữ khai báo còn hiệu lực trong suốt thời gian lưu trú dài; xảy ra ít hơn User Story 1, 2.

**Independent Test**: Dùng đồng hồ giả lập với khai báo hết hạn 30/09/2027; gia hạn một người, để một người quá hạn; gia hạn hợp đồng ngắn ngày từ 14 lên 60 ngày.

**Acceptance Scenarios**:

1. **Given** khai báo của A "Đã xác nhận" hết hạn 30/09/2027, A vẫn Đang lưu trú, **When** tới 31/08/2027 (CFG-M02-13 mặc định \[30 ngày\]), **Then** Hành chính được nhắc lập khai báo gia hạn (BR-M02-12).
2. **Given** Hành chính lập khai báo gia hạn cho A, **When** khai báo gia hạn được ghi "Đã xác nhận" với ngày hết hạn mới, **Then** khai báo cũ chuyển "Được thay thế"; A có đúng một khai báo Đã xác nhận (DBR-33).
3. **Given** tới hết 30/09/2027 mà khai báo gia hạn của D chưa được xác nhận và chưa có khai báo đang xử lý, **When** qua ngày hết hạn, **Then** khai báo cũ chuyển "Hết hạn", Hành chính được nhắc lập khai báo mới, Quản lý viện được báo; hệ thống không tự tạo khai báo (BR-M02-12, Q-306).
4. **Given** B nội trú ngắn ngày 14 ngày có thông báo lưu trú "Đã xác nhận", **When** phụ lục gia hạn hợp đồng làm tổng thời gian lưu trú thành 60 ngày được áp dụng, **Then** Hành chính được nhắc lập khai báo; khi Hành chính lập, khai báo là "Đăng ký tạm trú" ở "Cần khai báo" (BR-M02-14, Q-306).
5. **Given** khai báo gia hạn của A đang "Đã nộp", **When** Hành chính lập thêm một khai báo khác cho A, **Then** hệ thống chặn vì đã có khai báo đang xử lý (DBR-33).
6. **Given** E nội trú ngắn ngày 14 ngày có thông báo lưu trú "Đã xác nhận", **When** phụ lục gia hạn thêm 7 ngày (tổng 21 ngày, dưới CFG-M02-11) được áp dụng, **Then** Hành chính được nhắc lập khai báo "Thông báo lưu trú" cho ngày kết thúc mới; khi khai báo mới Đã xác nhận, thông báo lưu trú cũ chuyển "Được thay thế" (Q-215).

---

### User Story 4 - Khai báo xóa khi kết thúc lưu trú hoặc qua đời (Priority: P2)

Khi người cao tuổi Kết thúc lưu trú hoặc Qua đời mà còn khai báo "Đã xác nhận", hệ thống nhắc Hành chính khai báo xóa tạm trú; người cao tuổi ở tình trạng "cần khai báo xóa" tới khi khai báo chuyển Đã xóa (Q-306). Việc này không chặn Kết thúc lưu trú và được làm cả khi hồ sơ đã ở trạng thái cuối. Khai báo đang xử lý bị hủy.

**Why this priority**: Kết thúc đúng nghĩa vụ khai báo; không ảnh hưởng tới luồng kết thúc lưu trú.

**Independent Test**: Kết thúc lưu trú cho A có khai báo Đã xác nhận, và cho B có khai báo đang "Đã nộp"; ghi đã khai báo xóa cho A.

**Acceptance Scenarios**:

1. **Given** A có khai báo "Đã xác nhận", **When** lệnh Kết thúc lưu trú của A thành công, **Then** Hành chính được nhắc khai báo xóa tạm trú cho A; lệnh Kết thúc lưu trú không bị chặn vì khai báo (BR-M02-13, 6.8).
2. **Given** A cần khai báo xóa, **When** Hành chính ghi đã khai báo xóa kèm bằng chứng, **Then** khai báo chuyển "Đã xóa"; việc nhắc dừng; thao tác được phép dù hồ sơ A ở trạng thái cuối (ngoại lệ (3) của BR-M01-05).
3. **Given** B qua đời khi khai báo đang "Đã nộp", **When** lệnh Ghi nhận qua đời thành công, **Then** khai báo "Đã nộp" chuyển "Đã hủy"; không có nhắc khai báo xóa (vì chưa có khai báo Đã xác nhận).
4. **Given** khai báo của A chưa chuyển Đã xóa, **When** mỗi ngày trôi qua, **Then** Hành chính được nhắc mỗi CFG-M02-09 (mặc định \[1 ngày\]).

---

### Edge Cases

- Người nội trú đang Tạm vắng hoặc Điều trị tại bệnh viện: khai báo giữ nguyên, việc nhắc vẫn chạy, vì người đó vẫn cư trú tại viện.
- Hợp đồng dài hạn không ghi ngày kết thúc cụ thể: coi là từ CFG-M02-11 trở lên, loại "Đăng ký tạm trú".
- Hủy tiếp nhận: không có nhắc và không lập được khai báo, vì nghĩa vụ khai báo chỉ phát sinh khi Hoàn tất tiếp nhận.
- Người quay lại với hồ sơ mới (Q-12): khai báo của hồ sơ cũ không chuyển sang; hồ sơ mới có khai báo mới. Mốc cộng dồn cũng tính lại từ hợp đồng đầu của hồ sơ mới (Q-241).
- Kết quả ghi nhầm: sửa bằng đính chính (feature 000); khai báo không bị xóa.
- Người thường trú cùng xã/phường với viện có hợp đồng không ghi ngày kết thúc: Thông báo lưu trú không có ngày hết hạn riêng, không có nhắc gia hạn (FR-006 chỉ áp cho Đăng ký tạm trú); khai báo lại chỉ khi có phụ lục gia hạn (FR-007) (Q-215).

## Requirements *(mandatory)*

### Functional Requirements

Mọi yêu cầu dưới đây kế thừa feature 000. Phân nhóm dữ liệu:

| Dữ liệu | Nhóm (1.5) | Thao tác được phép |
| --- | --- | --- |
| Khai báo tạm trú, lưu trú | 2 | Chỉ qua lệnh ở bảng trạng thái (FR-005); nội dung đã ghi (nộp, kết quả, xóa) là nhóm 3, sai sót bằng đính chính |
| Tình trạng "cần khai báo", "cần khai báo xóa" của người cao tuổi | Số liệu tính | **(Sửa, 2026-10-06, Q-306)** Tính từ trạng thái người cao tuổi, hợp đồng và khai báo; không có lệnh; dùng cho nhắc và dashboard |
| Tham số CFG-M02-11, 12, 13 | 1 – Tham số | Cấu hình (Quản lý viện, feature 000) |

- **FR-001**: **(Sửa, 2026-10-06, Q-306)** Khi lệnh Hoàn tất tiếp nhận của người **nội trú** thành công (feature 001, 004), hệ thống MUST nhắc Hành chính lập khai báo và MUST NOT tự tạo khai báo. Hành chính MUST lập được bằng lệnh **"Lập khai báo"** đúng một khai báo ở "Cần khai báo" cho người nội trú đã Hoàn tất tiếp nhận mà chưa có khai báo đang xử lý (FR-004). Loại khai báo MUST được xác định khi lập là "Đăng ký tạm trú" nếu tổng thời gian lưu trú dự kiến (từ ngày bắt đầu hợp đồng đầu tiên của chuỗi hợp đồng nối tiếp không gián đoạn trong cùng hồ sơ tới ngày kết thúc dự kiến mới nhất, gồm cả các phụ lục gia hạn; có khoảng trống giữa hai hợp đồng thì tính lại từ hợp đồng sau, Q-241) từ CFG-M02-11 (mặc định \[30 ngày\]) trở lên, hoặc hợp đồng không có ngày kết thúc; ngược lại là "Thông báo lưu trú". Người bán trú MUST NOT có khai báo; lệnh lập cho người bán trú MUST bị chặn. Khi khai báo còn "Cần khai báo", Hành chính MUST xác nhận được "thường trú cùng xã/phường với viện" (đối chiếu địa chỉ thường trú trên hồ sơ, feature 001 FR-001, Q-244; có ghi chú căn cứ); khi đó loại MUST chuyển thành "Thông báo lưu trú", và mọi khai báo sau của cùng hồ sơ (FR-006, FR-007) MUST dùng loại này, bất kể thời gian lưu trú. Xác nhận nhầm được gỡ bằng đính chính khi khai báo còn "Cần khai báo". *(Nguồn: 6.10, BR-M02-11; Q-215)*
- **FR-001a** *(bổ sung, 2026-10-06, Q-286)*: Khi người đang lưu trú bán trú được chuyển sang nội trú (gồm nội trú ngắn ngày để ở lại qua đêm) bằng hợp đồng mới hoặc phụ lục đổi loại lưu trú được áp dụng (feature 004), hệ thống MUST nhắc Hành chính lập khai báo như FR-001 (**sửa, 2026-10-06, Q-306**), với tổng thời gian lưu trú dự kiến tính từ ngày hiệu lực của phần nội trú (ngày hiệu lực của phụ lục đổi loại lưu trú, hoặc ngày bắt đầu hợp đồng nội trú mới; Q-336); các quy tắc FR-002 → FR-007 áp như với người vào ở nội trú. *(Nguồn: 6.10, BR-M02-11, 3.3; Q-286, Q-289)*
- **FR-002**: **(Sửa, 2026-10-06, Q-306)** Hạn khai báo MUST là mốc phát sinh nghĩa vụ cộng CFG-M02-12 (mặc định \[1 ngày\]); mốc là thời điểm Hoàn tất tiếp nhận (FR-001), ngày phần nội trú có hiệu lực, kể cả khi ngày đó ở tương lai so với lúc phụ lục hoặc hợp đồng được áp dụng (FR-001a, Q-336), thời điểm phụ lục gia hạn được áp dụng (FR-007), hoặc ngày khai báo chuyển Hết hạn (FR-006). Hạn không phụ thuộc thời điểm Hành chính lập khai báo. Khi quá hạn mà chưa ghi đã nộp, dù khai báo đã được lập hay chưa, Hành chính MUST được nhắc mỗi ngày và Quản lý viện MUST được báo một lần vào ngày quá hạn đầu tiên. Khai báo MUST NOT là điều kiện chặn Hoàn tất tiếp nhận. *(Nguồn: BR-M02-11, CFG-M02-12; Q-215, Q-306)*
- **FR-003**: Khai báo MUST ghi: người cao tuổi, loại, xác nhận "thường trú cùng xã/phường với viện" (có hoặc không, ghi chú căn cứ), hạn khai báo, ngày nộp, kênh nộp (Cổng dịch vụ công / trực tiếp), cơ quan tiếp nhận, mã hồ sơ, kết quả, ngày hết hạn (bắt buộc khi đăng ký tạm trú được chấp nhận), bằng chứng (bắt buộc khi ghi chấp nhận và khi ghi đã khai báo xóa), lý do từ chối, người thực hiện từng lệnh, thời điểm, khai báo được thay thế. *(Nguồn: 6.10)*
- **FR-004**: Với mỗi người cao tuổi, tại một thời điểm MUST có tối đa một khai báo "Đã xác nhận" và tối đa một khai báo đang xử lý ("Cần khai báo" hoặc "Đã nộp"). *(Nguồn: DBR-33)*
- **FR-005**: Vòng đời khai báo MUST theo bảng dưới; không có chuyển nào khác. *(Nguồn: 6.10, BR-M02-11 → 14, constitution IV)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái kế tiếp | Ai thực hiện | Điều kiện (chặn nếu không đạt) | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Lập khai báo (sau nhắc: Hoàn tất tiếp nhận nội trú, FR-001; phần nội trú có hiệu lực, FR-001a; phụ lục gia hạn khi người đó chỉ có Thông báo lưu trú, FR-007; Hết hạn mà chưa có khai báo đang xử lý, FR-006) | Cần khai báo | Hành chính | Nghĩa vụ khai báo đã phát sinh; FR-004 | Xác định loại (FR-001, FR-007); hạn khai báo theo mốc (FR-002) |
| (chưa có) | Lập khai báo gia hạn | Cần khai báo | Hành chính | Có khai báo Đăng ký tạm trú Đã xác nhận; FR-004 | Tính hạn khai báo |
| Cần khai báo | Xác nhận thường trú cùng xã/phường với viện (FR-001) | Cần khai báo | Hành chính | Có ghi chú căn cứ | Loại chuyển Thông báo lưu trú; hạn khai báo giữ nguyên |
| Cần khai báo | Ghi đã nộp | Đã nộp | Hành chính | Có ngày nộp, kênh, cơ quan | Dừng nhắc quá hạn |
| Đã nộp | Ghi được chấp nhận | Đã xác nhận | Hành chính | Có bằng chứng; đăng ký tạm trú có ngày hết hạn | Nếu là gia hạn hoặc chuyển loại: khai báo Đã xác nhận cũ → Được thay thế |
| Đã nộp | Ghi bị từ chối | Cần khai báo | Hành chính | Có lý do | Hạn khai báo mới = thời điểm ghi + CFG-M02-12 |
| Đã xác nhận | Qua ngày hết hạn mà chưa có khai báo gia hạn Đã xác nhận | Hết hạn | Bộ lập lịch | — | Báo Quản lý viện; FR-006 |
| Đã xác nhận, Hết hạn | Ghi đã khai báo xóa (FR-008) | Đã xóa | Hành chính | Có bằng chứng; người cao tuổi đã Kết thúc lưu trú hoặc Qua đời | Dừng nhắc khai báo xóa (FR-008) |
| Đã xác nhận | Khai báo mới cùng người được Đã xác nhận | Được thay thế | Hệ thống | — | — |
| Cần khai báo, Đã nộp | Người cao tuổi chuyển trạng thái cuối | Đã hủy | Hệ thống | — | Dừng nhắc |

Đã xóa, Được thay thế và Đã hủy là trạng thái cuối. Hết hạn chỉ còn chuyển sang Đã xóa.

- **FR-006**: Khi khai báo Đăng ký tạm trú "Đã xác nhận" còn CFG-M02-13 (mặc định \[30 ngày\]) tới ngày hết hạn và người cao tuổi chưa ở trạng thái cuối, Hành chính MUST được nhắc lập khai báo gia hạn, nhắc lại mỗi ngày tới khi có khai báo gia hạn đang xử lý. Qua ngày hết hạn mà chưa có khai báo gia hạn Đã xác nhận, khai báo MUST chuyển Hết hạn và Quản lý viện MUST được báo; nếu chưa có khai báo đang xử lý thì Hành chính MUST được nhắc lập khai báo loại Đăng ký tạm trú (**sửa, 2026-10-06, Q-306:** hệ thống không tự tạo). *(Nguồn: BR-M02-12, CFG-M02-13)*
- **FR-007**: **(Sửa, 2026-10-06, Q-306)** Khi một phụ lục gia hạn hợp đồng nội trú (ngắn ngày, hoặc của người thường trú cùng xã/phường với viện) được áp dụng (feature 004) trong khi người đó chỉ có khai báo loại Thông báo lưu trú, hệ thống MUST nhắc Hành chính lập khai báo mới và MUST NOT tự tạo. Loại khai báo khi lập MUST là Đăng ký tạm trú nếu tổng thời gian lưu trú dự kiến mới (cách tính ở FR-001) từ CFG-M02-11 trở lên và người đó chưa được xác nhận thường trú cùng xã/phường với viện; ngược lại là Thông báo lưu trú cho ngày kết thúc dự kiến mới. Nếu đã có khai báo đang xử lý thì lệnh lập thêm MUST bị chặn (FR-004); Hành chính được nhắc khai theo ngày kết thúc mới. Khi khai báo mới được Đã xác nhận, thông báo lưu trú cũ chuyển Được thay thế. *(Nguồn: BR-M02-14; gia hạn dưới ngưỡng: Q-215, Clarification 2026-09-29)*
- **FR-008**: **(Sửa, 2026-10-06, Q-306)** Khi người cao tuổi Kết thúc lưu trú hoặc Qua đời mà có khai báo "Đã xác nhận" hoặc "Hết hạn", người đó ở tình trạng **"cần khai báo xóa"** (số liệu tính, không phải bản ghi) và hệ thống MUST nhắc Hành chính mỗi CFG-M02-09 (mặc định \[1 ngày\]) tới khi khai báo chuyển Đã xóa. Việc này MUST NOT là điều kiện của Kết thúc lưu trú (6.8) và MUST NOT là mục của danh sách việc sau qua đời; thao tác ghi đã khai báo xóa MUST được phép khi hồ sơ ở trạng thái cuối (ngoại lệ (3) của BR-M01-05). *(Nguồn: BR-M02-13, 6.8)*
- **FR-009**: Quyền MUST theo Phụ lục 27 dòng "Khai báo tạm trú, lưu trú": Hành chính thực hiện mọi lệnh; Quản lý viện xem toàn viện; vai trò khác và người thân không có quyền. *(Nguồn: Phụ lục 27, 19.3)*
- **FR-010**: Feature này MUST yêu cầu feature 009 gửi các thông báo: nhắc lập khai báo khi nghĩa vụ phát sinh (FR-001, FR-001a, FR-006, FR-007) (Hành chính, Nhẹ); quá hạn khai báo (Hành chính, Nhẹ, nhắc mỗi ngày; Quản lý viện, Nhẹ); nhắc gia hạn (Hành chính, Nhẹ); Hết hạn (Hành chính, Quản lý viện, Nhẹ); nhắc khai báo xóa (Hành chính, Nhẹ). *(Nguồn: 17 "khai báo tạm trú quá hạn ... Nhẹ")*
- **FR-011**: Feature này MUST cung cấp cho feature 016 số người cần khai báo (chưa lập khai báo, hoặc khai báo ở Cần khai báo), số quá hạn, số khai báo Hết hạn, số người cần khai báo xóa, cho dashboard (18.5). *(Nguồn: 18.5)*

#### Giao tiếp với feature khác

| Hướng | Feature | Nội dung | Dữ liệu tối thiểu |
| --- | --- | --- | --- |
| Nhận | 001, 004 | Hoàn tất tiếp nhận; loại lưu trú, ngày bắt đầu, ngày kết thúc hợp đồng hiệu lực; phụ lục gia hạn được áp dụng; Kết thúc lưu trú; Qua đời; trạng thái người cao tuổi | Người cao tuổi, hợp đồng, ngày, trạng thái |
| Gửi | 009 | Các thông báo ở FR-010 | Nguồn, mức, người nhận |
| Gửi | 016 | Số liệu ở FR-011 | Người cao tuổi, trạng thái, hạn |

### Key Entities *(include if feature involves data)*

- **Khai báo tạm trú (KHAI_BAO_TAM_TRU)** – nhóm 2: các trường ở FR-003, trạng thái, khai báo được thay thế, lịch sử lệnh.
- **Tình trạng "cần khai báo", "cần khai báo xóa"** – số liệu tính theo người cao tuổi (**sửa, 2026-10-06, Q-306:** thay thực thể "Việc khai báo xóa tạm trú"): mốc phát sinh nghĩa vụ, hạn, khai báo liên quan.
- Dùng từ feature khác: **Hợp đồng**, **Phụ lục** (004); **Người cao tuổi** và trạng thái (001).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% người nội trú Hoàn tất tiếp nhận trong đợt thử có lời nhắc lập khai báo; 100% khai báo do Hành chính lập có loại khớp tính tay theo CFG-M02-11; 0 khai báo do hệ thống tự tạo; 0 người bán trú có khai báo.
- **SC-002**: Với đồng hồ giả lập, 100% nghĩa vụ khai báo quá hạn (đã hoặc chưa lập khai báo) và 100% khai báo tới mốc CFG-M02-13 được nhắc đúng ngày; 100% khai báo quá ngày hết hạn chuyển Hết hạn trong lần chạy đầu tiên sau đó.
- **SC-003**: 0 người cao tuổi có hai khai báo Đã xác nhận hoặc hai khai báo đang xử lý cùng lúc.
- **SC-004**: 0 lệnh Hoàn tất tiếp nhận hoặc Kết thúc lưu trú bị chặn vì khai báo.
- **SC-005**: Hành chính ghi xong một lần nộp và một kết quả khai báo trong không quá 2 phút mỗi thao tác.

## Assumptions

- Số feature `018` là số kế tiếp sau spec 017; người dùng chọn tách khai báo tạm trú thành spec riêng (2026-09-28).
- Mốc 30 ngày (cộng dồn cả gia hạn), khai báo lại khi gia hạn dưới ngưỡng, người thường trú cùng xã/phường dùng Thông báo lưu trú: chốt ở Clarification 2026-09-29 (Q-215). Hạn 1 ngày và việc không chặn Hoàn tất tiếp nhận: chốt theo mặc định đề xuất cùng Q-215. Vẫn nên nhờ tư vấn pháp lý đối chiếu Luật Cư trú 2020 (Luật số 68/2020/QH14, sửa đổi bởi Luật số 118/2025/QH15 từ 01/7/2026) và văn bản hướng dẫn trước khi vận hành.
- Thông báo lưu trú có hiệu lực tới ngày kết thúc dự kiến, không có ngày hết hạn riêng; khi hợp đồng ngắn ngày được gia hạn, kể cả khi vẫn dưới ngưỡng, viện thông báo lưu trú lại (FR-007; Clarification 2026-09-29, Q-215).
- Người đang Tạm vắng hoặc Điều trị tại bệnh viện vẫn giữ khai báo.
- Quản lý viện được báo khi quá hạn khai báo: một lần vào ngày quá hạn đầu tiên, để không nhắc lặp tới người duyệt.

## Điểm cần báo lại về tài liệu nguồn

1. **Gia hạn hợp đồng ngắn ngày mà vẫn dưới ngưỡng (FR-007, Assumptions)** – 6.10 và BR-M02-14 chỉ nêu trường hợp vượt ngưỡng. Người dùng chốt ở Clarification 2026-09-29: tạo khai báo Thông báo lưu trú mới cho ngày kết thúc mới. *(Đã xử lý 2026-09-29: 6.10, BR-M02-11, BR-M02-14, BF-01 bước 15a; Q-215 chuyển sang 24.2.)*
2. **Báo Quản lý viện khi quá hạn khai báo (FR-002)** – BR-M02-11 ghi "báo Quản lý viện" nhưng không nêu tần suất; spec chọn một lần vào ngày quá hạn đầu tiên. *(Đã xử lý 2026-09-29: ghi vào BR-M02-11.)*
3. **Khai báo Hết hạn khi người cao tuổi kết thúc lưu trú (FR-008)** – 6.10 cho Đã xác nhận, Hết hạn → Đã xóa; spec tạo việc khai báo xóa cho cả khai báo Hết hạn. Khớp bảng 6.10.

**(2026-10-05) Thay đổi do chủ dự án chốt Q-280 → Q-286.** *(Đã xử lý 2026-10-06: spec đã sửa theo quyết định dưới đây, xem mục "Cập nhật 2026-10-06" ở Clarifications. Điểm báo lại về FR-001a đã xử lý 2026-10-06: chủ dự án chốt Q-289, đã ghi vào 24.2, BR-M02-11, 6.10, 24.4, BF-08)*

- **Q-286** – bán trú không qua đêm; người cần ở lại qua đêm phải thuê giường bằng nội trú ngắn ngày; giá bán trú theo dịch vụ đăng ký (3.3). Ảnh hưởng: người bán trú ở lại qua đêm được ghi là nội trú ngắn ngày, nên áp quy tắc thông báo lưu trú.

**(2026-10-06) Cập nhật căn cứ pháp lý (rà bảng 24.4).** Chỉ đổi trích dẫn, không đổi quy tắc; nội dung cần tư vấn pháp lý xác nhận ở 24.4.

- **Q-215** – Luật Cư trú 2020 đã được sửa đổi bởi Luật số 118/2025/QH15, hiệu lực từ 01/7/2026: mở rộng nghĩa vụ thông báo lưu trú qua đêm cho "cơ sở lưu trú khác", thẩm quyền đăng ký cư trú chuyển về Công an cấp xã; ngưỡng 30 ngày đăng ký tạm trú không đổi theo các nguồn đã tra. Tài liệu nguồn (6.10, Q-215, 24.4) và Assumptions của spec này đã đổi trích dẫn. Cần tư vấn pháp lý xác nhận viện có thuộc "cơ sở lưu trú khác" không và hạn thông báo (CFG-M02-12).

**(2026-10-06) Thay đổi do chủ dự án chốt Q-306 (hệ thống không tự sinh dữ liệu, công việc).** *(Đã xử lý 2026-10-06: spec đã viết lại, xem mục "Cập nhật 2026-10-06, lượt 2" ở Clarifications.)*

- **Q-306** – Khai báo tạm trú, lưu trú, khai báo gia hạn và khai báo xóa do Hành chính lập khi được nhắc, không còn tự tạo khi Hoàn tất tiếp nhận nội trú, khi phần nội trú có hiệu lực hay khi qua ngày hết hạn (BR-M02-11, 13, 14; bảng 6.10; Q-289 sửa). Nguyên tắc chung: mọi bản ghi do actor lập bằng lệnh; hệ thống chỉ gợi ý từ dữ liệu đã có, tự chuyển trạng thái theo thời gian, cảnh báo, leo thang, nhắc, kiểm tra và thực hiện tác động kèm theo lệnh (1.3, 24.2). FR cần rà (dò theo từ khóa, chưa đối chiếu từng FR): FR-001, FR-001a, FR-002, FR-004, FR-006, FR-007, FR-011.

**(2026-10-10) Plan 018, điểm báo lại 1, 2.** *(Đã xử lý 2026-10-10: Q-336; sửa FR-001a, FR-002. Điểm 1 (Luật Cư trú 118/2025/QH15) vẫn chờ tư vấn pháp lý ở 24.4.)*
