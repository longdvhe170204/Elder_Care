# Feature Specification: Số dư và thu chi của người cao tuổi

**Feature Branch**: `017-resident-balance-ledger`

**Created**: 2026-09-28

**Status**: Draft (viết lại theo ranh giới Q-385, 2026-10-22)

**Input**: User description: "Quản lý số dư và thu chi của người cao tuổi theo docs/business/nghiep-vu.md mục 15.9, 15.9a (Module 11, BR-M11-10 → BR-M11-16, BR-M11-21 → 23), 6.5 (đặt cọc do Kế toán thu), 6.8 (quyết toán số dư và tiền cọc khi kết thúc lưu trú, Q-222), luồng BF-16 ở docs/business/luong-nghiep-vu.md; quyết định Q-211, Q-212, Q-219, Q-222, Q-377 → Q-379, Q-381, Q-385; UC-12, UC-85 → UC-88, UC-100, UC-101; actor Kế toán (AC-14); DBR-28 → DBR-30, DBR-37, DBR-38; CFG-M11-04, CFG-M11-05, CFG-M11-10, CFG-M11-11. Gồm: sổ số dư và sổ tiền cọc; mã nộp tiền, mã QR VietQR; Kế toán ghi nhận tiền nộp (tiền mặt, chuyển khoản); tự trừ số dư khi bảng chi phí Đã chốt (sự kiện từ feature 010); hoàn tiền, điều chỉnh có Quản lý viện duyệt; báo sắp hết tiền/còn nợ cho người đại diện và Kế toán; quyết toán khi kết thúc lưu trú và qua đời (Xác nhận quyết toán, bảng quyết toán PDF); phiếu báo phí; xuất sao kê, báo cáo thu chi ra Excel và PDF; người thân xem số dư, tải chứng từ trên cổng."

## Clarifications

### Session 2026-09-28

- Q: Khi tính "số ngày còn đủ tiền" cho người bán trú, "phí một ngày" được tính thế nào? → A: Hợp đồng giá tháng: giá tháng ÷ số ngày của tháng hiện tại; hợp đồng giá buổi: giá buổi × số buổi có lịch trong 7 ngày tới ÷ 7 (chốt theo Mặc định Q-223).
- Q: Số dư khác 0 xuất hiện sau khi hồ sơ đã ở trạng thái cuối (bảng bổ sung chốt muộn, chuyển khoản tới muộn, số dư dương chưa hoàn) được xử lý thế nào? → A: Vẫn ghi giao dịch bình thường; nhắc Kế toán mỗi CFG-M02-09 tới khi số dư về 0; hồ sơ không mở lại (chốt theo Mặc định Q-224).
- Q: Kế toán có được thu tiền cọc khi hợp đồng còn "Chờ ký" không? → A: Có. Sổ số dư, sổ tiền cọc và mã nộp tiền được mở khi hợp đồng đầu tiên chuyển Chờ ký; hợp đồng bị hủy trước khi Hiệu lực (hoặc Hủy tiếp nhận) thì Kế toán lập hoàn cọc, Quản lý viện duyệt.
- ~~Q: Phiếu thu tiền mặt của Kế toán có cần bước kiểm soát thứ hai không? → A: số phiếu liên tục, chốt quỹ ngày.~~ *(Bỏ 2026-10-22, Q-385: quỹ tiền mặt, số phiếu thu chi thuộc phần mềm kế toán.)*

### Session 2026-09-30 (rà soát vận hành)

- Q: Gia đình đã nộp đủ sau khi bị báo "sắp hết tiền", "còn nợ" có được báo không? → A: Có. Khi một giao dịch nộp tiền làm tình trạng về Bình thường, hệ thống tính lại ngay lúc ghi, dừng nhắc và báo người đại diện mức Nhẹ "đã ghi nhận khoản nộp, số dư đã đủ" (người không có quyền xem chi phí chỉ nhận phần "chung"). Các biến động tốt lên khác vẫn không báo ngay. Sửa một phần Q-228 (Q-252).

### Session 2026-10-01 (/speckit-clarify, checklist cross-feature)

- Q: Gia đình nộp tiền nhưng chưa đủ để tình trạng về Bình thường thì người đại diện có được báo không? → A: Có. Mọi giao dịch nộp tiền của người đang ở Sắp hết tiền hoặc Còn nợ đều gửi người đại diện thông báo mức Nhẹ "đã ghi nhận khoản nộp" kèm tình trạng sau khi nộp; chưa đủ thì chu kỳ nhắc giữ nguyên; người đang Bình thường nộp tiền thì không gửi (Q-257).
- Q: Thông báo "đã ghi nhận khoản nộp" xử lý thế nào với chuyển khoản, với người đang "dừng nhắc", và khi khoản nộp bị điều chỉnh giảm? → A: Chuyển khoản tính lúc Kế toán ghi nhận; vẫn gửi khi đang "dừng nhắc"; điều chỉnh giảm khoản nộp được duyệt thì tính lại ngay, tình trạng xấu đi báo như một lần chuyển mới, không gửi thông báo thu hồi (Q-268, sửa theo Q-385).

### Cập nhật 2026-10-06 (đồng bộ Q-280 → Q-288, Q-292 → Q-299)

- **Q-285**: không dùng cổng thanh toán; VietQR chỉ là ảnh chứa số tài khoản và nội dung. Không đổi yêu cầu.
- **Q-284, Q-293**: Kế toán đối chiếu chứng từ yêu cầu mua sắm (15.10, feature 019), không lập yêu cầu mua sắm; việc này không ghi vào sổ số dư. Thêm vào Ngoài phạm vi.

### Cập nhật 2026-10-10, lượt 2 (đồng bộ Q-334, Q-335)

- **Q-334**: số ngày còn đủ tiền chỉ tính trên khoản đã lập (FR-016).
- **Q-335**: tài khoản thu của viện là tham số CFG-M11-10 (FR-003).
- Giao dịch thanh toán được tạo kèm lệnh chốt bảng (Q-306).

### Cập nhật 2026-10-22 (đồng bộ Q-377 → Q-379, Q-381, Q-385; viết lại spec)

Chủ dự án chốt Q-377 → Q-379, Q-381 rồi thu gọn phần tài chính theo **Q-385**: hệ thống tính tiền theo dịch vụ chăm sóc và theo dõi người cao tuổi còn nợ hay còn dư; quỹ tiền mặt, đối soát ngân hàng, phiếu thu chi, hóa đơn, hạch toán thuộc phần mềm kế toán bên ngoài. Spec được viết lại; mã FR, SC cũ giữ nguyên để đối chiếu plan, tasks, mã bị bỏ ghi "(Bỏ, Q-385)":

- **Bỏ**: chốt quỹ ngày (FR-005a cũ, UC-93, Q-226); nhập sao kê, tự khớp, trạng thái dòng sao kê (FR-008, FR-009 cũ, User Story 3 cũ); cặp điều chỉnh chuyển tiền (FR-009a, Q-227); giao dịch đảo (sai sót bằng điều chỉnh có duyệt, FR-011); dãy số và in phiếu thu, phiếu chi (FR-005, FR-033); vòng đời biên bản quyết toán và biên bản bổ sung (FR-035, FR-036); xuất sổ tiền cọc riêng.
- **Viết lại**: Kế toán ghi nhận chuyển khoản bằng tay, cảnh báo trùng mã giao dịch ngân hàng (FR-007, User Story 3); lệnh **Xác nhận quyết toán** dựng **bảng quyết toán** PDF có phiên bản (FR-022, FR-034, User Story 7, 10).
- **Q-386** (cùng ngày): báo cáo thu chi thu gọn còn tổng nộp, tổng đã trừ theo bảng chi phí, tổng hoàn, danh sách người còn nợ (FR-025 (b), User Story 8); sao kê giữ nguyên.
- **Q-387** (sau plan): người qua đời mà gia đình còn nợ thì mục quyết toán được ngoại lệ như hồ sơ kết thúc lưu trú (feature 004 FR-071a); Kế toán vẫn xác nhận quyết toán (FR-023, FR-034).
- **Giữ**: sổ số dư, sổ tiền cọc, mã nộp tiền, VietQR, trừ số dư khi chốt bảng, báo sắp hết tiền, hoàn tiền và điều chỉnh có duyệt, phiếu báo phí (PDF, Excel; Q-378), sao kê và báo cáo thu chi (Excel, PDF; Q-381), chứng từ ghi "không thay thế hóa đơn" (Q-377).

## Phạm vi

**Trong phạm vi** (Module 11, mục 15.9, 15.9a; 6.5; 6.8 phần quyết toán; UC-12, UC-85 → UC-88, UC-100, UC-101; BF-16):

1. Sổ số dư và sổ tiền cọc của từng người cao tuổi; mã nộp tiền; thông tin nộp tiền (số tài khoản của viện, mã QR theo chuẩn VietQR) cung cấp cho cổng người thân (15.9, DBR-28, DBR-29).
2. Kế toán ghi nhận tiền nộp và tiền cọc (tiền mặt; chuyển khoản khi tiền về tài khoản của viện); xác định trạng thái đặt cọc cho feature 004 (6.5, UC-12, UC-85, UC-86, BR-M11-12, BR-M11-16).
3. Giao dịch "thanh toán bảng chi phí" được tạo kèm lệnh chốt bảng chi phí (BR-M11-11, DBR-30; Q-306).
4. Hoàn tiền, cấn trừ tiền cọc, điều chỉnh; Quản lý viện duyệt hoàn tiền và điều chỉnh. Hoàn cọc khi hợp đồng bị hủy trước khi Hiệu lực hoặc Hủy tiếp nhận (BR-M11-10, BR-M11-14).
5. Tính số ngày còn đủ tiền hằng ngày; báo "sắp hết tiền" và "còn nợ" (BR-M11-13, UC-87, CFG-M11-04, CFG-M11-05).
6. Điều kiện "số dư và tiền cọc đã quyết toán" khi kết thúc lưu trú và mục tương ứng trong danh sách việc sau qua đời; lệnh Xác nhận quyết toán và bảng quyết toán PDF (6.8, Q-222, BR-M11-22, UC-101).
7. Phiếu báo phí cho mỗi bảng chi phí Đã chốt (PDF, Excel; 15.9a, BR-M11-21, UC-100).
8. Xuất sao kê số dư (gồm giao dịch tiền cọc), báo cáo thu chi ra Excel hoặc PDF (UC-88, 18.4, Q-381).
9. Dấu "số dư không đủ" cho đề nghị mua hộ (BR-M11-15).

**Ngoài phạm vi** (spec này **nhận** sự kiện hoặc **cung cấp** dữ liệu cho feature sở hữu):

- Quy tắc dùng chung (nhóm dữ liệu, lý do bắt buộc, vòng đời yêu cầu phê duyệt, nhật ký, tham số, "hoặc toàn bộ, hoặc không", Bộ lập lịch chạy lại không tạo trùng): feature 000.
- Hợp đồng, khoản cần đặt cọc, hồ sơ kết thúc lưu trú, danh sách việc sau qua đời, yêu cầu ngoại lệ kết thúc lưu trú: feature 004.
- Khoản chi phí, bảng chi phí, chốt bảng, chi phí tạm tính (Q-136), đề nghị mua hộ, file kế toán (UC-64): feature 010.
- Hiển thị trên cổng người thân và quyền xem chi phí của từng người thân: feature 012. Gửi thông báo: feature 009. Báo cáo, dashboard chung: feature 016.
- **Phần mềm kế toán bên ngoài (Q-385)**: hạch toán, báo cáo tài chính của viện, hóa đơn, phiếu thu, phiếu chi và số chứng từ, quỹ tiền mặt (chốt quỹ, thiếu thừa quỹ), đối soát sao kê ngân hàng. Hệ thống không nhập sao kê, không kết nối ngân hàng, không thanh toán trực tuyến (1.2, 15.1, 23).
- Đối chiếu chứng từ của yêu cầu mua sắm do Kế toán thực hiện (15.10, BR-M11-19): feature 019; không tạo giao dịch trong sổ số dư. Kế toán không lập yêu cầu mua sắm (Q-293).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Mở sổ số dư, cấp mã nộp tiền, người thân xem số dư và thông tin nộp tiền (Priority: P1)

Khi hợp đồng đầu tiên của một người cao tuổi chuyển Chờ ký, hệ thống mở sổ số dư và sổ tiền cọc, số dư ban đầu bằng 0, và cấp một mã nộp tiền không đổi. Người thân có quyền xem chi phí mở cổng thấy số dư, lịch sử giao dịch, số tài khoản của viện, mã nộp tiền và mã QR có sẵn nội dung chuyển khoản. Kế toán, Hành chính, Quản lý viện thấy số dư trên hồ sơ người cao tuổi. Nhân viên chăm sóc, Điều dưỡng, Bác sĩ không thấy số dư.

**Why this priority**: Không có sổ và mã nộp tiền thì không thể ghi thu hay trừ tiền. Đây là yêu cầu trực tiếp của góp ý "trong hồ sơ nên có bao nhiêu tiền" (Q-211).

**Independent Test**: Cho hợp đồng của A chuyển Chờ ký. Kiểm tra sổ, mã nộp tiền, và hiển thị theo từng vai trò.

**Acceptance Scenarios**:

1. **Given** A chưa có hợp đồng nào từng ở Chờ ký, **When** hợp đồng đầu tiên của A chuyển Chờ ký (feature 004), **Then** A có đúng một sổ số dư (số dư 0) và một sổ tiền cọc (số dư cọc 0), và một mã nộp tiền duy nhất (DBR-28, DBR-29).
2. **Given** A đã có sổ, **When** hợp đồng Chờ ký bị Trả về nháp rồi gửi ký lại, hợp đồng chuyển Hiệu lực, hoặc A có phụ lục, hợp đồng mới (gia hạn), **Then** không có sổ mới; mã nộp tiền không đổi.
3. **Given** người thân R của A có quyền xem chi phí, **When** R mở cổng, **Then** R thấy số dư, lịch sử giao dịch, số tài khoản thu của viện, mã nộp tiền và mã QR có nội dung chuyển khoản chứa mã nộp tiền (15.9, 23).
4. **Given** người thân S của A không có quyền xem chi phí, **When** S mở cổng, **Then** S không thấy số dư hay thông tin nộp tiền.
5. **Given** Điều dưỡng D mở hồ sơ A, **When** hồ sơ hiển thị, **Then** không có số dư (19.3).
6. **Given** A từng kết thúc lưu trú và quay lại với hồ sơ mới (Q-12), **When** hợp đồng đầu tiên của hồ sơ mới chuyển Chờ ký, **Then** hồ sơ mới có sổ mới và mã nộp tiền mới; sổ cũ giữ nguyên chỉ đọc.

---

### User Story 2 - Kế toán ghi nhận tiền nộp và tiền cọc (Priority: P1)

Gia đình nộp tiền mặt tại quầy hoặc chuyển khoản. Kế toán ghi nhận khoản nộp vào số dư hoặc vào sổ tiền cọc; giao dịch có hiệu lực ngay. Hệ thống không cấp số phiếu thu và không quản lý quỹ tiền mặt (Q-385); số chứng từ của phần mềm kế toán ghi được ở ghi chú. Khi tổng tiền cọc đã thu không nhỏ hơn khoản cần đặt cọc của hợp đồng, đặt cọc tự chuyển "Đã đáp ứng", làm điều kiện Hoàn tất tiếp nhận của feature 004.

**Why this priority**: Thu cọc là điều kiện tiếp nhận (5.6); tiền nộp là đầu vào của số dư (Q-211, Q-212).

**Independent Test**: Hợp đồng của A cần cọc 20.000.000 đồng. Thu hai lần 10.000.000 đồng (tiền mặt, chuyển khoản). Kiểm tra trạng thái đặt cọc và việc Hành chính không ghi được.

**Acceptance Scenarios**:

1. **Given** hợp đồng của A đang Chờ ký, cần cọc 20.000.000 đồng, **When** Kế toán ghi thu cọc tiền mặt 10.000.000 đồng, **Then** sổ cọc là 10.000.000 đồng, đặt cọc vẫn "Chưa đáp ứng" (6.5).
2. **Given** tiếp theo, **When** Kế toán ghi nhận chuyển khoản 10.000.000 đồng vào sổ cọc với mã giao dịch ngân hàng NH001 (User Story 3), **Then** sổ cọc là 20.000.000 đồng, đặt cọc chuyển "Đã đáp ứng", feature 004 nhận trạng thái mới.
3. **Given** hợp đồng không yêu cầu đặt cọc, **When** hợp đồng chuyển Chờ ký, **Then** đặt cọc ở "Không yêu cầu", được coi là đạt, không cần giao dịch nào (feature 004 FR-031).
4. **Given** Kế toán ghi nộp tiền mặt 5.000.000 đồng vào số dư của A, ghi chú "PT 0123 phần mềm kế toán", **When** lưu, **Then** giao dịch "Nộp tiền mặt" Đã xác nhận, số dư tăng 5.000.000 đồng; giao dịch không sửa, không xóa được; hệ thống không cấp số phiếu, không in phiếu (BR-M11-10, BR-M11-16).
5. **Given** Hành chính H, **When** H tìm cách ghi thu tiền, **Then** hệ thống chặn; H chỉ xem được số dư và trạng thái đặt cọc (Phụ lục 27 ²⁶).
6. **Given** số tiền 0 hoặc âm, **When** lưu, **Then** hệ thống chặn.
7. **Given** A đã nộp cọc 20.000.000 đồng khi hợp đồng Chờ ký, **When** hợp đồng chuyển Đã hủy (hoặc A Hủy tiếp nhận) trước khi Hiệu lực, **Then** Kế toán được nhắc lập hoàn cọc cho người đại diện; hoàn cọc chỉ có hiệu lực khi Quản lý viện duyệt; nhắc mỗi CFG-M02-09 mặc định \[1 ngày\] tới khi số dư cọc về 0 (FR-015a).

---

### User Story 3 - Kế toán ghi nhận chuyển khoản (Priority: P1) *(viết lại 2026-10-22, Q-385)*

Khi thấy tiền chuyển về tài khoản của viện (trên ứng dụng ngân hàng hoặc phần mềm kế toán), Kế toán ghi nhận khoản nộp: người cao tuổi (nhận ra theo mã nộp tiền trong nội dung), sổ, số tiền, ngày nhận, mã giao dịch ngân hàng, người nộp. Hệ thống không nhập file sao kê và không tự khớp; đối soát với ngân hàng làm ở phần mềm kế toán.

**Why this priority**: Chuyển khoản là kênh nộp tiền chính; không ghi nhận thì số dư sai (Q-211).

**Independent Test**: Ghi nhận ba khoản chuyển khoản, trong đó một khoản gộp cọc và tiền nộp, một khoản trùng mã giao dịch.

**Acceptance Scenarios**:

1. **Given** gia đình A chuyển 3.000.000 đồng, nội dung có mã nộp tiền của A, mã giao dịch NH001, **When** Kế toán tìm A theo mã nộp tiền và ghi nhận vào số dư, **Then** có giao dịch "Nộp chuyển khoản" Đã xác nhận 3.000.000 đồng, ghi mã NH001; số dư A tăng (BR-M11-12).
2. **Given** NH002 25.000.000 đồng, gia đình báo gồm 20.000.000 đồng tiền cọc và 5.000.000 đồng tiền nộp, **When** Kế toán ghi hai giao dịch cùng mã NH002 (sổ cọc 20.000.000 đồng, số dư 5.000.000 đồng), **Then** cả hai Đã xác nhận ngay; không cần duyệt.
3. **Given** NH001 đã được ghi nhận, **When** Kế toán ghi một khoản khác với mã NH001, **Then** hệ thống cảnh báo "mã giao dịch đã dùng" kèm giao dịch trước; Kế toán vẫn lưu được nếu xác nhận (DBR-29).
4. **Given** khoản nộp đã ghi sai người (ghi cho A thay vì B), **When** Kế toán lập điều chỉnh giảm cho A trỏ giao dịch sai và ghi nhận lại cho B, Quản lý viện duyệt điều chỉnh, **Then** số dư A giảm, giao dịch gốc giữ nguyên và mang dấu "đã điều chỉnh" (FR-011).
5. **Given** C đã ở trạng thái cuối, **When** Kế toán ghi nhận chuyển khoản tới muộn cho C, **Then** giao dịch vẫn được ghi (tiền đã về viện) và Kế toán được nhắc quyết toán phần mới (FR-024).

---

### User Story 4 - Chốt bảng chi phí tự trừ số dư (Priority: P1)

Khi Quản lý viện chốt bảng chi phí của một người (feature 010), hệ thống tạo ngay một giao dịch "thanh toán bảng chi phí" bằng tổng bảng. Bảng bổ sung có tổng âm làm tăng số dư. Số dư được phép âm, và khi âm được hiển thị là "còn nợ".

**Why this priority**: Đây là nối kết giữa chi phí và tiền (Q-211).

**Independent Test**: A có số dư 10.000.000 đồng. Chốt bảng tháng 10 tổng 9.300.000 đồng, rồi bảng tháng 11 tổng 9.000.000 đồng, rồi bảng bổ sung −200.000 đồng.

**Acceptance Scenarios**:

1. **Given** số dư A là 10.000.000 đồng, **When** bảng tháng 10 của A (9.300.000 đồng) Đã chốt, **Then** có đúng một giao dịch "Thanh toán bảng chi phí" −9.300.000 đồng trỏ về bảng đó; số dư 700.000 đồng (BR-M11-11, DBR-30).
2. **Given** tiếp theo, **When** bảng tháng 11 (9.000.000 đồng) Đã chốt, **Then** số dư −8.300.000 đồng, hiển thị "còn nợ 8.300.000 đồng"; việc chốt không bị chặn.
3. **Given** bảng bổ sung của A có tổng −200.000 đồng, **When** bảng Đã chốt, **Then** giao dịch làm số dư tăng 200.000 đồng.
4. **Given** bảng có tổng 0 đồng (mọi khoản thuộc gói), **When** Đã chốt, **Then** vẫn có một giao dịch 0 đồng để truy xuất (DBR-30).
5. **Given** sự kiện chốt bảng được gửi lại hai lần (lỗi truyền), **When** xử lý, **Then** chỉ có một giao dịch cho bảng đó (DBR-30).

---

### User Story 5 - Báo "sắp hết tiền" và "còn nợ" (Priority: P1)

Mỗi ngày, Bộ lập lịch tính số ngày còn đủ tiền của từng người có hợp đồng Hiệu lực. Khi dưới ngưỡng hoặc số dư âm, người đại diện được báo mức Trung bình, Kế toán được báo mức Nhẹ kèm danh sách, và việc báo được lặp lại theo chu kỳ tới khi hết tình trạng. Còn nợ quá lâu thì Quản lý viện được báo. Tình trạng này không chặn chăm sóc, cấp thuốc hay suất ăn.

**Why this priority**: Trả lời câu hỏi "ai thông báo khi các cụ sắp hết tiền" (Q-211): hệ thống tự báo, Kế toán theo dõi.

**Independent Test**: Dùng đồng hồ giả lập, cho A có số dư giảm dần qua ngưỡng CFG-M11-04 rồi âm; kiểm tra ngày gửi, người nhận, mức, và việc dừng nhắc khi gia đình nộp tiền.

**Acceptance Scenarios**:

1. **Given** hợp đồng của A giá tháng 9.000.000 đồng, tháng hiện tại 30 ngày (phí ngày 300.000 đồng), số dư 6.000.000 đồng, tổng chi phí chưa chốt 2.400.000 đồng, CFG-M11-04 mặc định \[15 ngày\], **When** Bộ lập lịch chạy, **Then** số ngày còn đủ tiền là 12; tình trạng "Sắp hết tiền"; người đại diện của A nhận thông báo mức Trung bình loại "chi phí"; Kế toán nhận mức Nhẹ (BR-M11-13).
2. **Given** A đang "Sắp hết tiền" và đã được báo ngày 01/11, CFG-M11-05 mặc định \[7 ngày\], **When** Bộ lập lịch chạy các ngày 02/11 → 07/11 và A vẫn dưới ngưỡng, **Then** không có thông báo mới; ngày 08/11 có thông báo nhắc lại.
3. **Given** A "Sắp hết tiền", **When** gia đình nộp 10.000.000 đồng và lần tính lại cho số ngày còn đủ tiền là 45, **Then** tình trạng về "Bình thường", không nhắc nữa; người đại diện nhận "đã ghi nhận khoản nộp, số dư đã đủ" (Q-252).
4. **Given** số dư A âm từ ngày 01/11, **When** Bộ lập lịch chạy ngày 08/11 (quá CFG-M11-05), **Then** Quản lý viện được báo thêm, cùng với nhắc lại cho người đại diện và Kế toán.
5. **Given** A "Còn nợ", **When** điều dưỡng phát thuốc, nhân viên ghi công việc, bếp chốt suất, **Then** không có thao tác nào bị chặn vì số dư (Q-219).
6. **Given** A có hai người đại diện P1 (có quyền xem chi phí) và P2 (không có), **When** A chuyển "Sắp hết tiền", **Then** cả hai nhận thông báo; P1 thấy số dư, tổng chi phí chưa chốt, thông tin nộp tiền; P2 chỉ thấy phần "chung" đề nghị liên hệ Kế toán (FR-018, Q-105).
7. **Given** A đã "Còn nợ" từ ngày 01/11, **When** ngày 05/11 gia đình nộp một phần, hết nợ nhưng số ngày còn đủ tiền là 4, **Then** tình trạng chuyển "Sắp hết tiền"; người đại diện nhận "đã ghi nhận khoản nộp" kèm tình trạng; lần nhắc theo chu kỳ vẫn là ngày 08/11 (Q-257).

---

### User Story 6 - Hoàn tiền và điều chỉnh có Quản lý viện duyệt (Priority: P2)

Kế toán lập hoàn tiền (trả số dư dương hoặc tiền cọc cho người đại diện) hoặc điều chỉnh (tăng hoặc giảm, có lý do; dùng cả để sửa một giao dịch ghi sai). Các giao dịch này ở "Chờ duyệt" và chỉ tác động số dư khi Quản lý viện duyệt. Không có giao dịch đảo (Q-385).

**Why this priority**: Cần cho sai sót và kết thúc lưu trú, nhưng không nằm trong luồng thu hằng ngày.

**Independent Test**: A có số dư 2.000.000 đồng. Lập hoàn 3.000.000 đồng (bị chặn), hoàn 2.000.000 đồng (duyệt), điều chỉnh sửa một khoản nộp ghi nhầm.

**Acceptance Scenarios**:

1. **Given** số dư A 2.000.000 đồng, **When** Kế toán lập hoàn 3.000.000 đồng, **Then** hệ thống chặn vì vượt số dư (BR-M11-14).
2. **Given** Kế toán lập hoàn 2.000.000 đồng cho người đại diện P, phương thức chuyển khoản, **When** lưu, **Then** giao dịch ở Chờ duyệt, số dư chưa đổi, Quản lý viện được báo. **When** Quản lý viện duyệt, **Then** giao dịch Đã xác nhận, số dư 0.
3. **Given** hoàn tiền Chờ duyệt 2.000.000 đồng, **When** trước lúc duyệt một bảng chi phí 500.000 đồng được chốt (số dư còn 1.500.000 đồng), **Then** lệnh duyệt bị chặn vì vượt số dư hiện có; Kế toán được báo để lập lại.
4. **Given** khoản nộp tiền mặt 5.000.000 đồng ghi nhầm cho A, **When** Kế toán lập điều chỉnh −5.000.000 đồng cho A trỏ giao dịch đó, lý do "ghi nhầm người", và Quản lý viện duyệt, **Then** số dư A giảm 5.000.000 đồng; giao dịch gốc giữ nguyên, mang dấu "đã điều chỉnh" (BR-M11-10).
5. **Given** giao dịch "Thanh toán bảng chi phí", **When** Kế toán lập điều chỉnh trỏ giao dịch đó, **Then** hệ thống chặn; sai sót của bảng đã chốt đi qua khoản điều chỉnh của feature 010 (DBR-17, DBR-30).
6. **Given** Quản lý viện từ chối một điều chỉnh kèm lý do, **When** lưu, **Then** giao dịch chuyển Từ chối, số dư không đổi, Kế toán được báo.
7. **Given** A đang lưu trú, số dư 5.000.000 đồng, tổng chi phí chưa chốt 4.000.000 đồng, **When** gia đình xin rút 2.000.000 đồng và Kế toán lập hoàn tiền 2.000.000 đồng, **Then** hệ thống chặn vì vượt 1.000.000 đồng được phép; hoàn 1.000.000 đồng thì được lập (FR-014).
8. **Given** hoàn tiền Chờ duyệt ghi người nhận P, **When** trước lúc duyệt P thôi là người đại diện (feature 012), **Then** lệnh duyệt bị chặn; Kế toán được báo để lập lại với người nhận khác (FR-014).

---

### User Story 7 - Quyết toán số dư và tiền cọc khi kết thúc lưu trú hoặc qua đời (Priority: P2)

Khi hồ sơ kết thúc lưu trú được lập, điều kiện "số dư và tiền cọc đã quyết toán" xuất hiện ở trạng thái Chưa đạt. Sau khi bảng kỳ cuối đã chốt và trừ vào số dư, Kế toán cấn trừ tiền cọc vào số dư âm hoặc hoàn cọc, hoàn số dư dương, hoặc ghi thu phần còn nợ, rồi thực hiện lệnh **Xác nhận quyết toán**. Khi qua đời, việc này là một mục trong danh sách việc sau qua đời.

**Why this priority**: Là điều kiện kết thúc lưu trú đã chốt (Q-222), nhưng chỉ xảy ra ở cuối vòng đời.

**Independent Test**: A kết thúc lưu trú với số dư −1.500.000 đồng và cọc 20.000.000 đồng. Kiểm tra trình tự cấn trừ, hoàn, xác nhận và trạng thái điều kiện.

**Acceptance Scenarios**:

1. **Given** hồ sơ kết thúc của A được lập, bảng kỳ cuối chưa chốt, **When** feature 004 xem danh sách điều kiện, **Then** điều kiện "số dư và tiền cọc đã quyết toán" là Chưa đạt, kèm lý do "bảng kỳ cuối chưa chốt" (6.8).
2. **Given** bảng kỳ cuối Đã chốt, số dư −1.500.000 đồng, cọc 20.000.000 đồng, **When** Kế toán cấn trừ 1.500.000 đồng từ cọc, **Then** có hai giao dịch liên kết: sổ cọc −1.500.000 đồng, số dư +1.500.000 đồng; số dư 0, cọc 18.500.000 đồng.
3. **Given** tiếp theo, **When** Kế toán lập hoàn cọc 18.500.000 đồng cho người đại diện và Quản lý viện duyệt, **Then** cọc 0; các ý tiền đã đạt nhưng điều kiện vẫn Chưa đạt, lý do "chưa xác nhận quyết toán"; **When** Kế toán thực hiện Xác nhận quyết toán, **Then** bảng quyết toán phiên bản 1 được dựng, điều kiện chuyển Đạt; feature 004 nhận trạng thái mới (FR-022, FR-034).
4. **Given** A còn nợ 3.000.000 đồng và cọc 0, gia đình hẹn nộp sau, **When** Quản lý viện duyệt ngoại lệ ở feature 004, không còn giao dịch Chờ duyệt và Kế toán xác nhận quyết toán, **Then** bảng quyết toán ghi còn nợ 3.000.000 đồng; điều kiện là "Đạt (ngoại lệ)"; chưa xác nhận thì vẫn Chưa đạt; số dư vẫn âm và tiếp tục được báo "còn nợ" (FR-020).
5. **Given** A qua đời, **When** feature 004 mở danh sách việc sau qua đời, **Then** có mục "quyết toán số dư và tiền cọc" do Kế toán thực hiện, Chưa hoàn thành cho tới khi đạt như trên (gồm xác nhận quyết toán), hoặc Không áp dụng nếu sổ cọc và số dư chưa từng có giao dịch khác 0 đồng.
6. **Given** điều kiện đã Đạt và lệnh Kết thúc lưu trú đã thực hiện, **When** sau đó một bảng bổ sung của A được chốt, **Then** giao dịch vẫn được tạo, số dư khác 0, Kế toán được nhắc quyết toán phần mới và xác nhận lại (bảng quyết toán phiên bản 2); hồ sơ không mở lại (Q-224).

---

### User Story 8 - Xuất sao kê và báo cáo thu chi (Priority: P2)

Kế toán xuất ra Excel hoặc PDF (cùng số liệu, Q-381): sao kê số dư của một hoặc nhiều người trong một khoảng (số dư đầu kỳ, từng giao dịch gồm giao dịch tiền cọc, số dư cuối kỳ); báo cáo thu chi theo khoảng (thu gọn, Q-386: tổng tiền gia đình nộp, tổng đã trừ theo bảng chi phí, tổng tiền hoàn, danh sách người còn nợ). Người thân có quyền xem chi phí tải được sao kê của người cao tuổi trên cổng.

**Why this priority**: Yêu cầu trực tiếp của góp ý "xuất bản ghi excel sao kê, thu chi" (Q-212), nhưng không chặn nghiệp vụ thu hằng ngày.

**Independent Test**: Với dữ liệu tháng 10 của 3 người, xuất sao kê, báo cáo thu chi; so số liệu với tính tay.

**Acceptance Scenarios**:

1. **Given** A có số dư 01/10 là 1.000.000 đồng, trong tháng nộp 10.000.000 đồng, thanh toán bảng −9.300.000 đồng, **When** Kế toán xuất sao kê A từ 01/10 tới 31/10, **Then** file có số dư đầu kỳ 1.000.000 đồng, hai giao dịch, số dư cuối kỳ 1.700.000 đồng (UC-88).
2. **Given** tháng 10 gia đình nộp tổng 55.000.000 đồng, các bảng chi phí chốt trừ 48.000.000 đồng, hoàn 2.000.000 đồng, cuối tháng B còn nợ 1.500.000 đồng, **When** xuất báo cáo thu chi tháng 10, **Then** báo cáo có ba tổng 55.000.000, 48.000.000, 2.000.000 đồng và danh sách người còn nợ gồm B 1.500.000 đồng (Q-386).
3. **Given** giao dịch Chờ duyệt hoặc Từ chối, **When** xuất, **Then** không có trong sao kê và báo cáo; chỉ giao dịch Đã xác nhận được tính.
4. **Given** Hành chính H, **When** H tìm cách xuất báo cáo thu chi, **Then** hệ thống chặn (Phụ lục 27, dòng "Sao kê, báo cáo thu chi").
5. **Given** mỗi lần xuất, **When** xong, **Then** lần xuất được ghi (người xuất, thời điểm, loại, định dạng, khoảng, phạm vi, số dòng); file đã xuất không đổi khi dữ liệu sau đó thay đổi.
6. **Given** khoảng xuất dài hơn CFG-M14-01 mặc định \[12 tháng\], **When** xuất, **Then** hệ thống từ chối (18.7).
7. **Given** sao kê A tháng 10 đã xuất ra Excel, **When** Kế toán xuất cùng khoảng ra PDF, **Then** PDF có thông tin viện (CFG-M11-11), cùng số dư đầu kỳ, từng giao dịch, số dư cuối kỳ như bản Excel (FR-025).

---

### User Story 9 - Phiếu báo phí hằng tháng và khi ra viện (Priority: P2) *(bổ sung 2026-10-22, Q-377, Q-378)*

Khi bảng chi phí của một người cao tuổi Đã chốt, Kế toán phát hành phiếu báo phí (PDF, Excel) gửi gia đình: chi tiết khoản, tổng, số dư trước và sau khi trừ, số còn nợ kèm thông tin nộp tiền. Người thân có quyền xem chi phí được báo và tải phiếu trên cổng.

**Why this priority**: Gia đình cần chứng từ để đối chiếu và nộp tiền; không chặn thu chi hằng ngày.

**Independent Test**: Bảng tháng 10 của A Đã chốt tổng 15.200.000 đồng, số dư trước 10.000.000 đồng; người đại diện R có quyền xem chi phí, người thân N không có.

**Acceptance Scenarios**:

1. **Given** bảng tháng 10 của A Đã chốt, **When** Kế toán phát hành phiếu báo phí, **Then** phiếu có mã duy nhất, thông tin viện (CFG-M11-11), từng khoản, tổng 15.200.000 đồng, số dư trước 10.000.000 đồng, sau −5.200.000 đồng, số còn nợ kèm mã nộp tiền, tài khoản thu và mã QR; dòng "Chứng từ này không thay thế hóa đơn" (FR-031, FR-032).
2. **Given** phiếu đã phát hành, **When** Kế toán phát hành lần nữa cho cùng bảng, **Then** bị từ chối; tải lại trả đúng file đã lưu (FR-031, FR-037).
3. **Given** bảng có khoản thuốc Amlodipin, **When** xem phiếu, **Then** khoản ghi "Thuốc" và mã vật phẩm, không có tên thuốc (FR-037, Q-133).
4. **Given** phiếu vừa phát hành, **When** feature 009 gửi thông báo, **Then** R nhận thông báo Nhẹ và tải được phiếu trên cổng; N không nhận và không thấy phiếu (FR-030, FR-038).
5. **Given** bảng tháng 10 Đã chốt, sau đó có bảng bổ sung tháng 11 chứa khoản điều chỉnh cho tháng 10, **When** bảng bổ sung chốt, **Then** Kế toán phát hành phiếu riêng cho bảng bổ sung; phiếu tháng 10 không đổi (FR-031).

---

### User Story 10 - Xác nhận quyết toán và bảng quyết toán (Priority: P2) *(viết lại 2026-10-22, Q-379, Q-385)*

Khi phần tiền của điều kiện quyết toán đã đạt (hoặc có ngoại lệ), Kế toán thực hiện lệnh Xác nhận quyết toán; hệ thống dựng bảng quyết toán (PDF) tổng hợp kỳ cuối, tiền cọc, số dư và các khoản hoàn, thu. Bản có chữ ký của gia đình đính kèm sau nếu có. Số liệu đổi sau khi xác nhận thì xác nhận lại, tạo phiên bản bảng mới.

**Why this priority**: Là chứng từ cuối cùng giao gia đình khi ra viện.

**Independent Test**: A nội trú dài hạn, kết thúc ngày 20/10; bảng kỳ cuối 9.000.000 đồng; số dư trước quyết toán 4.000.000 đồng; cọc 20.000.000 đồng.

**Acceptance Scenarios**:

1. **Given** bảng kỳ cuối Đã chốt và trừ, cọc đã cấn trừ 5.000.000 đồng và hoàn 15.000.000 đồng, số dư 0, **When** Kế toán xác nhận quyết toán, **Then** bảng quyết toán phiên bản 1 (PDF) ghi tổng kỳ cuối theo loại, số dư trước quyết toán, cọc đã thu, đã cấn trừ, đã hoàn, số còn phải hoàn 0, người xác nhận, thời điểm (FR-034).
2. **Given** bảng kỳ cuối chưa chốt, **When** Kế toán xác nhận quyết toán, **Then** bị từ chối, lý do "bảng kỳ cuối chưa chốt" (FR-034).
3. **Given** bảng quyết toán đã dựng, **When** Kế toán đính kèm bản scan có chữ ký của người đại diện, **Then** bản scan gắn với phiên bản hiện hành; điều kiện quyết toán không đổi (FR-034).
4. **Given** đã xác nhận, hồ sơ kết thúc chưa Hoàn tất, **When** một khoản nộp tới muộn được ghi nhận làm số dư dương, **Then** điều kiện về Chưa đạt; Kế toán lập hoàn, rồi xác nhận lại; bảng phiên bản 2 thay phiên bản 1 làm hiện hành, phiên bản 1 vẫn tra được (FR-034, DBR-38).
5. **Given** A qua đời, **When** mục "quyết toán số dư và tiền cọc" của danh sách việc sau qua đời được xét, **Then** mục chỉ Hoàn thành khi đã xác nhận quyết toán và các ý tiền đạt, hoặc gia đình còn nợ mà Quản lý viện đã duyệt ngoại lệ (feature 004 FR-071a, Q-387) và Kế toán đã xác nhận quyết toán với số còn nợ (FR-023).

---

### Edge Cases

- Gia đình chuyển khoản trước khi có sổ (chưa có hợp đồng Chờ ký): Kế toán chưa ghi nhận được; ghi khi sổ được mở (FR-001). Hệ thống không giữ danh sách chờ.
- Hai khoản chuyển khoản cùng ngày cùng số tiền: mỗi khoản có mã giao dịch ngân hàng riêng nên được ghi riêng; nhập trùng mã thì có cảnh báo (DBR-29).
- Một lần chuyển khoản gồm cả tiền cọc và tiền nộp, hoặc cho hai người cao tuổi: ghi thành nhiều giao dịch cùng mã giao dịch ngân hàng (FR-007).
- Ghi nhầm người hoặc nhầm sổ: sửa bằng điều chỉnh có duyệt trỏ giao dịch sai (FR-011).
- Người cao tuổi đang Tạm vắng hoặc Điều trị tại bệnh viện: vẫn tính số ngày còn đủ tiền (phí lưu trú vẫn sinh theo 6.7).
- Gia đình xin rút bớt tiền khi người cao tuổi đang lưu trú: được, bằng hoàn tiền có duyệt, trong giới hạn FR-014.
- Phí lưu trú một ngày bằng 0 (hợp đồng không có phí lưu trú): số ngày còn đủ tiền không xác định; chỉ báo khi số dư âm.

## Requirements *(mandatory)*

### Functional Requirements

Mọi yêu cầu dưới đây kế thừa feature 000. Phân nhóm dữ liệu của feature này:

| Dữ liệu | Nhóm (1.5) | Thao tác được phép |
| --- | --- | --- |
| Sổ số dư, sổ tiền cọc | 2 – bản ghi dẫn xuất | Mở do hệ thống; số dư chỉ thay đổi qua giao dịch Đã xác nhận |
| Mã nộp tiền | 1 | Do hệ thống cấp, không đổi, không cấp lại cho người khác |
| Giao dịch số dư, giao dịch tiền cọc | 2 khi Chờ duyệt → 3 khi Đã xác nhận | Theo bảng mục C; giao dịch Đã xác nhận chỉ bị bù bằng giao dịch điều chỉnh |
| Tình trạng số dư (Bình thường / Sắp hết tiền / Còn nợ) | 2 – bản ghi dẫn xuất | Theo bảng chuyển tình trạng số dư |
| Lịch sử tình trạng và lần báo | 3 | Chỉ ghi thêm |
| Lần xuất sao kê, báo cáo | 3 | Chỉ ghi thêm |
| Phiếu báo phí, bảng quyết toán (các phiên bản) | 3 | Chỉ ghi thêm; không sửa, không xóa |
| Tham số CFG-M11-04, CFG-M11-05, CFG-M11-10, CFG-M11-11 | 1 – Tham số | Cấu hình (Quản lý viện, feature 000) |

#### A. Sổ, mã nộp tiền, hiển thị

- **FR-001**: Khi hợp đồng đầu tiên của một hồ sơ người cao tuổi chuyển Chờ ký, hệ thống MUST mở đúng một sổ số dư và một sổ tiền cọc với số dư 0, và cấp một mã nộp tiền duy nhất, không đổi, không dùng lại cho hồ sơ khác. Hồ sơ mới của người quay lại (Q-12) MUST có sổ và mã mới. *(Nguồn: 15.9, DBR-28, DBR-29, UC-85)*
- **FR-002**: Số dư MUST luôn bằng tổng các giao dịch số dư Đã xác nhận; số dư cọc MUST luôn bằng tổng các giao dịch tiền cọc Đã xác nhận. Số dư được phép âm; số dư âm MUST hiển thị là "còn nợ" kèm số tiền. *(Nguồn: BR-M11-10, BR-M11-11, DBR-28)*
- **FR-003**: Hệ thống MUST cung cấp cho feature 012: số dư, số dư cọc, lịch sử giao dịch Đã xác nhận, số tài khoản thu của viện (CFG-M11-10, Q-335), mã nộp tiền, mã QR theo chuẩn VietQR có nội dung chuyển khoản chứa mã nộp tiền (không kèm số tiền), sao kê tải được. Mọi thông tin này thuộc loại thông tin **"chi phí"** của cổng (feature 012 FR-046); chỉ người thân có quyền xem chi phí được thấy. *(Nguồn: 15.9, 23, 19.3, Phụ lục 27 dòng "Số dư, thu chi")*
- **FR-004**: Số dư MUST hiển thị trên hồ sơ người cao tuổi cho Kế toán, Hành chính, Quản lý viện; MUST NOT hiển thị cho vai trò khác. Giao dịch loại thanh toán bảng chi phí chỉ hiện tổng bảng, không hiện tên thuốc (Q-133). *(Nguồn: 15.9, 19.3)*

#### B. Ghi nhận tiền nộp

- **FR-005** *(sửa 2026-10-22, Q-385)*: Kế toán MUST ghi được khoản nộp tiền mặt vào số dư hoặc vào sổ tiền cọc: số tiền (lớn hơn 0), người nộp, thời điểm thu, ghi chú (ghi được số chứng từ của phần mềm kế toán). Giao dịch Đã xác nhận ngay. Hệ thống MUST NOT cấp số phiếu thu, phiếu chi và MUST NOT in phiếu. *(Nguồn: 15.9, BR-M11-16, UC-85, 6.5)*
- **FR-005a**: *(Bỏ 2026-10-22, Q-385: không còn chốt quỹ ngày; quỹ tiền mặt thuộc phần mềm kế toán.)*
- **FR-006**: Trạng thái đặt cọc là **dẫn xuất**, không có lệnh "xác nhận" riêng. Với hợp đồng đang Chờ ký hoặc Hiệu lực: "Không yêu cầu" khi hợp đồng ghi không yêu cầu đặt cọc (được coi là đạt); "Đã đáp ứng" khi số dư cọc của hồ sơ không nhỏ hơn khoản cần đặt cọc; ngược lại "Chưa đáp ứng". Trạng thái được tính lại mỗi khi sổ cọc hoặc khoản cần đặt cọc đổi (kể cả phụ lục đổi khoản cọc, điều chỉnh một lần thu cọc). Mỗi lần trạng thái đổi, hệ thống MUST báo feature 004 kèm thời điểm, giao dịch làm thay đổi và Kế toán lập giao dịch đó (nguồn xác nhận). Feature 004 dùng trạng thái này cho điều kiện (c) của danh sách điều kiện tiếp nhận (feature 004 FR-004, FR-033). *(Nguồn: 6.5, 5.6, UC-12, Q-212)*
- **FR-007** *(viết lại 2026-10-22, Q-385)*: Kế toán MUST ghi nhận được khoản nộp chuyển khoản vào số dư hoặc vào sổ tiền cọc: hồ sơ (tìm được theo mã nộp tiền), số tiền (lớn hơn 0), ngày nhận, mã giao dịch ngân hàng (bắt buộc), người nộp, ghi chú. Giao dịch Đã xác nhận ngay. Một lần chuyển khoản gộp nhiều khoản MAY được ghi thành nhiều giao dịch cùng mã giao dịch ngân hàng (khác sổ hoặc khác hồ sơ). Khi mã giao dịch ngân hàng đã có ở giao dịch khác, hệ thống MUST cảnh báo kèm giao dịch đó và chỉ lưu khi Kế toán xác nhận lại; MUST NOT chặn. Hệ thống MUST NOT nhập file sao kê hay tự khớp. *(Nguồn: BR-M11-12, DBR-29, UC-86, Q-227, Q-385)*
- **FR-008**, **FR-009**: *(Bỏ 2026-10-22, Q-385: không còn khớp tự động và trạng thái dòng sao kê.)*
- **FR-009a**: *(Bỏ 2026-10-22, Q-385: không còn cặp điều chỉnh chuyển tiền; thay bằng ghi nhiều giao dịch cùng mã ở FR-007.)*

#### C. Giao dịch và phê duyệt

- **FR-010** *(sửa 2026-10-22, Q-385)*: Các loại giao dịch MUST theo bảng 15.9. Sổ số dư: Nộp tiền mặt, Nộp chuyển khoản, Thanh toán bảng chi phí, Hoàn tiền, Cấn trừ tiền cọc (tăng), Điều chỉnh. Sổ tiền cọc: Thu cọc (tiền mặt, chuyển khoản), Hoàn cọc, Cấn trừ tiền cọc (giảm), Điều chỉnh. "Cấn trừ tiền cọc" luôn là một cặp hai giao dịch cùng tên, liên kết với nhau (FR-015). Mỗi giao dịch MUST ghi: hồ sơ; sổ; loại; số tiền có dấu; thời điểm; phương thức; tham chiếu (mã giao dịch ngân hàng, bảng chi phí, giao dịch được điều chỉnh, giao dịch liên kết); người lập; người duyệt nếu có; lý do nếu có; ghi chú; trạng thái. *(Nguồn: 15.9)*
- **FR-011** *(sửa 2026-10-22, Q-385)*: Giao dịch Đã xác nhận MUST NOT sửa, xóa hay hủy. Sai sót MUST xử lý bằng giao dịch **Điều chỉnh** có lý do; khi sửa một giao dịch cụ thể thì điều chỉnh MUST trỏ giao dịch đó và giao dịch gốc mang dấu "đã điều chỉnh". Giao dịch "Thanh toán bảng chi phí" MUST NOT bị điều chỉnh; sai sót của bảng đi qua khoản điều chỉnh của feature 010. Không có giao dịch đảo. *(Nguồn: BR-M11-10, DBR-17, DBR-28, DBR-30)*
- **FR-012**: Khi feature 010 báo một bảng chi phí (thường hoặc bổ sung) chuyển Đã chốt, hệ thống MUST tạo ngay đúng một giao dịch "Thanh toán bảng chi phí" Đã xác nhận, số tiền bằng tổng bảng với dấu ngược (tổng dương làm giảm số dư, tổng âm làm tăng, tổng 0 vẫn tạo giao dịch 0 đồng). Sự kiện nhận lặp (cùng mã bảng) MUST NOT tạo giao dịch thứ hai. *(Nguồn: BR-M11-11, DBR-30)*
- **FR-013** *(sửa 2026-10-22, Q-385)*: Hoàn tiền, Hoàn cọc, Điều chỉnh do Kế toán lập MUST ở Chờ duyệt và chỉ tác động sổ khi Quản lý viện duyệt; dùng vòng đời yêu cầu phê duyệt chung (6.6) với các trạng thái Chờ duyệt → Đã xác nhận / Từ chối / Đã hủy. Kế toán không có quyền duyệt. *(Nguồn: BR-M11-10, BR-M11-14, Phụ lục 27 ³²)*
- **FR-014**: Hoàn tiền và hoàn cọc MUST ghi người nhận (một người đại diện có quan hệ Hiệu lực tại lúc lập; hồ sơ không còn người đại diện thì ghi người nhận khác và Quản lý viện duyệt kèm lý do), phương thức, ghi chú. Giới hạn số tiền: hoàn cọc không vượt số dư cọc; hoàn tiền không vượt số dư, và khi hồ sơ chưa ở trạng thái cuối thì không vượt (số dư − tổng chi phí chưa chốt, FR-016). Tại lúc duyệt, giới hạn số tiền và người nhận MUST được kiểm tra lại; người nhận không còn là người đại diện Hiệu lực thì lệnh duyệt bị chặn và Kế toán lập lại. *(Nguồn: BR-M11-14)*
- **FR-015**: Cấn trừ tiền cọc do Kế toán thực hiện, không cần duyệt, chỉ khi hồ sơ có hồ sơ kết thúc lưu trú đang chuẩn bị, hoặc danh sách việc sau qua đời đang mở, và bảng kỳ cuối đã chốt. Số tiền MUST NOT vượt số dư cọc và MUST NOT vượt phần còn nợ. Lệnh MUST tạo hai giao dịch liên kết trong cùng một lần: sổ cọc giảm, số dư tăng cùng số tiền. *(Nguồn: 15.9, 6.5, 6.8, Q-219, Q-222)*
- **FR-015a**: Khi hợp đồng có tiền cọc đã thu chuyển Đã hủy trước khi Hiệu lực, hoặc hồ sơ chuyển Hủy tiếp nhận, mà số dư cọc lớn hơn 0, Kế toán MUST được nhắc lập hoàn cọc (FR-013, FR-014) mỗi CFG-M02-09 mặc định \[1 ngày\] tới khi số dư cọc về 0. Hủy tiếp nhận MUST NOT bị chặn vì tiền cọc chưa hoàn. Hợp đồng Chờ ký được Trả về nháp thì tiền cọc giữ nguyên. *(Nguồn: 6.3, 6.5, 5.6)*

**Bảng trạng thái giao dịch** *(BR-M11-10, BR-M11-11, BR-M11-14, 6.6; sửa 2026-10-22, Q-385)*:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái kế tiếp | Ai thực hiện | Điều kiện (chặn nếu không đạt) | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Ghi nộp tiền mặt (Nộp tiền mặt, Thu cọc tiền mặt) | Đã xác nhận | Kế toán | FR-005 | Cập nhật sổ; cập nhật đặt cọc (FR-006) |
| (chưa có) | Ghi nhận chuyển khoản (Nộp chuyển khoản, Thu cọc chuyển khoản) | Đã xác nhận | Kế toán | FR-007 | Cập nhật sổ; cập nhật đặt cọc |
| (chưa có) | Bảng chi phí Đã chốt (Thanh toán bảng chi phí) | Đã xác nhận | Hệ thống | FR-012 | Cập nhật số dư |
| (chưa có) | Cấn trừ tiền cọc (cặp) | Đã xác nhận | Kế toán | FR-015 | Sổ cọc giảm, số dư tăng |
| (chưa có) | Lập hoàn tiền / hoàn cọc / điều chỉnh | Chờ duyệt | Kế toán | Có lý do (điều chỉnh); FR-011, FR-014 | Báo Quản lý viện |
| Chờ duyệt | Duyệt | Đã xác nhận | Quản lý viện | Kiểm tra lại FR-011, FR-014 tại lúc duyệt | Cập nhật số dư hoặc số dư cọc; giao dịch được điều chỉnh mang dấu "đã điều chỉnh" |
| Chờ duyệt | Từ chối | Từ chối | Quản lý viện | Có lý do | Báo Kế toán |
| Chờ duyệt | Hủy | Đã hủy | Kế toán | Có lý do | — |

Quá hạn duyệt được nhắc theo CFG-M15-05 mặc định \[48 giờ\], CFG-M15-06 mặc định \[96 giờ\] (6.6). Đã xác nhận, Từ chối, Đã hủy là trạng thái cuối.

#### D. Báo sắp hết tiền và còn nợ

- **FR-016**: **(Sửa, 2026-10-10, Q-334)** Hằng ngày, Bộ lập lịch MUST tính cho mỗi hồ sơ có hợp đồng Hiệu lực và chưa ở trạng thái cuối: **số ngày còn đủ tiền** = (số dư − **tổng chi phí chưa chốt**) ÷ phí lưu trú một ngày, làm tròn xuống. "Tổng chi phí chưa chốt" chỉ gồm các khoản Hành chính đã lập ở feature 010 (Q-306); hệ thống không ước tính phí của ngày chưa lập khoản (Q-334). Tổng chi phí chưa chốt là tổng các khoản của mọi bảng chi phí chưa Đã chốt của hồ sơ, lấy theo thành phần của Q-136, do feature 010 cung cấp (feature 010 FR-038a). Với hợp đồng tính theo tháng, phí lưu trú một ngày là phí ngày cơ sở của tháng hiện tại (feature 010 FR-009); với hợp đồng tính theo ngày là đơn giá ngày; với hợp đồng bán trú giá tháng là giá tháng ÷ số ngày của tháng hiện tại, với hợp đồng bán trú giá buổi là giá buổi × số buổi có lịch trong 7 ngày tới ÷ 7 (Q-223). *(Nguồn: BR-M11-13, Q-136)*
- **FR-017**: Tình trạng số dư MUST là: "Còn nợ" khi số dư − tổng chi phí chưa chốt < 0; "Sắp hết tiền" khi không còn nợ và số ngày còn đủ tiền < CFG-M11-04 mặc định \[15 ngày\]; "Bình thường" trong trường hợp còn lại. Khi phí lưu trú một ngày bằng 0, chỉ xét "Còn nợ". *(Nguồn: BR-M11-13, CFG-M11-04)*
- **FR-018**: Khi tình trạng chuyển sang "Sắp hết tiền" hoặc "Còn nợ", và mỗi CFG-M11-05 mặc định \[7 ngày\] sau lần báo trước trong khi tình trạng vẫn không phải Bình thường, hệ thống MUST gửi qua feature 009: mọi người đại diện có quan hệ Hiệu lực, mức Trung bình; Kế toán, mức Nhẹ, kèm danh sách. Nội dung gửi người thân chia phần theo loại thông tin (Q-105): phần "chung" là "có thông báo về tài chính của [họ tên], vui lòng liên hệ Kế toán"; phần "chi phí" gồm tình trạng, số dư, tổng chi phí chưa chốt, thông tin nộp tiền. Người đại diện không có quyền xem chi phí chỉ nhận phần "chung" (BR-M13-04). Chiều xấu đi là một lần chuyển mới: báo ngay và đặt lại chu kỳ CFG-M11-05. Chiều tốt lên ("Còn nợ" → "Sắp hết tiền") không báo ngay và giữ chu kỳ cũ. **(Q-252)** Giao dịch nộp tiền đưa tình trạng về Bình thường thì báo ngay theo bảng chuyển tình trạng số dư. **(Q-257)** Giao dịch nộp tiền chưa đưa tình trạng về Bình thường cũng MUST được báo "đã ghi nhận khoản nộp" kèm tình trạng sau khi nộp; thông báo này MUST NOT thay hay đặt lại lần nhắc theo chu kỳ. **(Q-268, sửa theo Q-385)** Với chuyển khoản, thời điểm tính là lúc Kế toán ghi nhận. Thông báo "đã ghi nhận khoản nộp" MUST vẫn gửi khi người cao tuổi đang ở chế độ "dừng nhắc". Khi một điều chỉnh giảm khoản nộp được duyệt, hệ thống MUST tính lại tình trạng ngay; nếu tình trạng xấu đi thì áp các dòng "báo ngay" của bảng chuyển tình trạng số dư; MUST NOT gửi thông báo thu hồi riêng. *(Nguồn: BR-M11-13, BR-M13-04, Q-105, Q-211, Q-252)*
- **FR-019**: Khi hồ sơ ở "Còn nợ" liên tục từ CFG-M11-05 trở lên, Quản lý viện MUST được báo thêm ở mỗi lần nhắc lại. *(Nguồn: BR-M11-13)*
- **FR-020**: Tình trạng "Sắp hết tiền" hay "Còn nợ" MUST NOT chặn bất kỳ lệnh chăm sóc, thuốc, suất ăn, hoạt động nào. Hồ sơ đã kết thúc lưu trú mà số dư còn âm (ngoại lệ ở FR-022) vẫn được báo "còn nợ" cho người đại diện và Kế toán theo FR-018 cho tới khi số dư bằng 0 hoặc Quản lý viện ghi "dừng nhắc" kèm lý do. *(Nguồn: Q-219)*
- **FR-021**: Khi đề nghị mua hộ được gửi (feature 010) mà số dư − tổng chi phí chưa chốt nhỏ hơn số tiền dự kiến, hệ thống MUST cung cấp cho feature 010 dấu "số dư không đủ" cho đề nghị; dấu không chặn. Dấu MUST được tính lại mỗi khi số dư hoặc tổng chi phí chưa chốt của hồ sơ đổi, cho tới khi đề nghị được đồng ý, từ chối hoặc hủy. *(Nguồn: BR-M11-15, Q-219)*

**Bảng chuyển tình trạng số dư** *(BR-M11-13; chỉ Bộ lập lịch đổi tình trạng, trừ "dừng nhắc" và tính lại khi ghi giao dịch nộp tiền)*:

| Tình trạng hiện tại | Sự kiện | Tình trạng kế tiếp | Tác động |
| --- | --- | --- | --- |
| Bình thường | Số ngày còn đủ tiền < CFG-M11-04, không nợ | Sắp hết tiền | Báo ngay theo FR-018; bắt đầu chu kỳ CFG-M11-05 |
| Bình thường, Sắp hết tiền | Số dư − tổng chi phí chưa chốt < 0 | Còn nợ | Báo ngay; đặt lại chu kỳ; bắt đầu đếm cho FR-019 |
| Còn nợ | Hết nợ nhưng số ngày còn đủ tiền < CFG-M11-04 | Sắp hết tiền | Không báo ngay; giữ chu kỳ |
| Sắp hết tiền, Còn nợ | Số ngày còn đủ tiền ≥ CFG-M11-04 và không nợ | Bình thường | Dừng nhắc |
| Sắp hết tiền, Còn nợ | (Q-252) Ghi một giao dịch nộp tiền mà sau khi tính lại ngay, số ngày còn đủ tiền ≥ CFG-M11-04 và không nợ | Bình thường | Dừng nhắc; báo người đại diện "đã ghi nhận khoản nộp, số dư đã đủ" (FR-030) |
| Sắp hết tiền, Còn nợ | (Q-257) Ghi một giao dịch nộp tiền mà sau khi tính lại ngay, tình trạng vẫn là Sắp hết tiền hoặc Còn nợ | Tình trạng tính được | Báo người đại diện "đã ghi nhận khoản nộp" kèm tình trạng sau khi nộp (FR-030); chu kỳ giữ nguyên |
| Sắp hết tiền, Còn nợ | Chưa đổi và đã qua CFG-M11-05 kể từ lần báo trước | Giữ nguyên | Nhắc lại theo FR-018, FR-019 |
| Còn nợ (hồ sơ trạng thái cuối) | Quản lý viện ghi "dừng nhắc", có lý do | Giữ nguyên, mang dấu "dừng nhắc" | Không nhắc nữa (FR-020) |

Hồ sơ không có hợp đồng Hiệu lực và chưa ở trạng thái cuối không được tính.

#### E. Quyết toán

- **FR-022** *(sửa 2026-10-22, Q-379, Q-385)*: Hệ thống MUST cung cấp cho feature 004 điều kiện "số dư và tiền cọc đã quyết toán" của hồ sơ kết thúc lưu trú. Điều kiện Đạt khi đủ hai phần: (1) ba ý về tiền: bảng kỳ cuối Đã chốt và đã có giao dịch thanh toán; số dư cọc bằng 0; số dư bằng 0; (2) không còn giao dịch Chờ duyệt, và Kế toán đã **xác nhận quyết toán** (FR-034) mà sau đó số liệu không đổi. Bảng bổ sung chưa chốt **không** làm điều kiện Chưa đạt (Q-134). Ngược lại Chưa đạt kèm lý do. Ngoại lệ do Quản lý viện duyệt ở feature 004 cho kết quả "Đạt (ngoại lệ)", chỉ thay ba ý về tiền của phần (1), không thay phần (2) (sửa 2026-10-22 theo BR-M11-22). Mỗi lần kết quả đổi, feature 004 MUST được báo. *(Nguồn: 6.8, 5.6, Q-222, Q-379)*
- **FR-023**: Khi người cao tuổi qua đời, mục "quyết toán số dư và tiền cọc" của danh sách việc sau qua đời MUST có kết quả theo cùng điều kiện FR-022 (kể cả ý xác nhận quyết toán): Hoàn thành khi Đạt hoặc Đạt (ngoại lệ) (ngoại lệ do Quản lý viện duyệt ở feature 004 FR-071a, chỉ thay ba ý về tiền; bổ sung 2026-10-22, Q-387); Không áp dụng khi sổ chưa từng có giao dịch nào khác 0 đồng. Kế toán thực hiện mục này. *(Nguồn: 6.8, Q-222, Q-387)*
- **FR-024**: Giao dịch làm số dư khác 0 sau khi hồ sơ đã ở trạng thái cuối (bảng bổ sung chốt muộn, chuyển khoản tới muộn) MUST được ghi bình thường; Kế toán MUST được nhắc mỗi CFG-M02-09 mặc định \[1 ngày\] tới khi số dư về 0 và đã xác nhận quyết toán lại (FR-034); hồ sơ không mở lại (Q-224). *(Nguồn: BR-M01-05, 6.8)*

#### F. Xuất dữ liệu

- **FR-025** *(sửa 2026-10-22, Q-381, Q-385)*: Kế toán MUST xuất được ra Excel và PDF (hai định dạng cùng số liệu; PDF có thông tin viện CFG-M11-11): (a) sao kê số dư của một hoặc nhiều hồ sơ trong một khoảng, gồm số dư đầu kỳ, từng giao dịch Đã xác nhận (thời điểm, sổ, loại, số tiền, phương thức, tham chiếu), số dư cuối kỳ, kèm số dư cọc đầu và cuối kỳ; (b) báo cáo thu chi theo khoảng (thu gọn, Q-386): tổng tiền gia đình nộp (số dư và cọc), tổng đã trừ theo bảng chi phí, tổng tiền hoàn (số dư và cọc), danh sách hồ sơ còn nợ tại cuối khoảng kèm số còn nợ. Quản lý viện xem được các nội dung này. Khoảng xuất MUST NOT dài hơn CFG-M14-01 mặc định \[12 tháng\]. *(Nguồn: 15.9, 18.4, 18.7, UC-88)*
- **FR-026**: Người thân có quyền xem chi phí MUST tải được sao kê (a) dạng Excel hoặc PDF của người cao tuổi đó qua cổng (feature 012), trong khoảng không dài hơn CFG-M14-01. *(Nguồn: 15.9, Q-381)*
- **FR-027**: Mỗi lần xuất MUST được ghi (người xuất, vai trò, thời điểm, loại, định dạng, khoảng, phạm vi, số dòng). File đã xuất không đổi khi dữ liệu nguồn đổi. *(Nguồn: 19.4, 18.7)*

#### G. Quyền

- **FR-028** *(sửa 2026-10-22, Q-385)*: Quyền MUST theo Phụ lục 27 và 19.3: Kế toán thực hiện mọi lệnh của mục B, C (lập), E, F, J (phát hành phiếu báo phí, xác nhận quyết toán, đính kèm bản ký); Quản lý viện duyệt hoặc từ chối hoàn tiền, hoàn cọc, điều chỉnh, ghi "dừng nhắc", xem toàn bộ kể cả chứng từ (Phụ lục 27 dòng "Phiếu báo phí", "Bảng quyết toán"); Hành chính chỉ xem số dư, số dư cọc và trạng thái đặt cọc; người thân có quyền xem chi phí xem theo FR-003, FR-026, FR-038; vai trò khác không có quyền. Kế toán xem hồ sơ người cao tuổi ở mức định danh, hợp đồng, bảng chi phí, số dư, không xem sức khỏe, khoản thuốc chỉ thấy mã vật phẩm (Q-133). *(Nguồn: 19.3, Phụ lục 27 ²⁶, ²⁹, ³²)*
- **FR-029** *(sửa 2026-10-22, Q-385)*: Giao dịch, lần xuất, phiếu báo phí, bảng quyết toán MUST được lưu giữ theo thời hạn của chi phí đã chốt (NFR-07, CFG-M15-03 mặc định \[10 năm sau khi kết thúc lưu trú\]). Mọi lệnh của spec này được ghi nhật ký (19.4); lần người thân xem số dư không bắt buộc ghi nhật ký, vì NFR-08 chỉ đòi với hồ sơ sức khỏe. *(Nguồn: 19.3, 19.4, NFR-07, NFR-08)*

#### I. Thông báo

- **FR-030** *(sửa 2026-10-22, Q-385)*: Feature này MUST yêu cầu feature 009 gửi đúng các thông báo ở bảng dưới, theo khuôn chung của feature 009. Thông báo tới người thân chia phần theo loại thông tin (Q-105). *(Nguồn: 17, BR-M13-01, BR-M13-04, Q-211)*

| Sự kiện | Người nhận | Mức | Căn cứ |
| --- | --- | --- | --- |
| Tình trạng chuyển Sắp hết tiền hoặc Còn nợ; nhắc lại mỗi CFG-M11-05 | Mọi người đại diện (phần "chung"; phần "chi phí" nếu có quyền) | Trung bình | FR-018 |
| Như trên | Kế toán (kèm danh sách) | Nhẹ | FR-018 |
| Giao dịch nộp tiền đưa tình trạng Sắp hết tiền, Còn nợ về Bình thường | Mọi người đại diện (phần "chung": "đã ghi nhận khoản nộp, số dư đã đủ"; phần "chi phí" nếu có quyền) | Nhẹ | Bảng chuyển tình trạng số dư, Q-252 |
| Giao dịch nộp tiền của người đang Sắp hết tiền, Còn nợ mà tình trạng chưa về Bình thường | Mọi người đại diện (phần "chung": "đã ghi nhận khoản nộp, [tình trạng sau khi nộp]"; phần "chi phí" nếu có quyền) | Nhẹ | Bảng chuyển tình trạng số dư, Q-257 |
| Còn nợ liên tục từ CFG-M11-05 | Quản lý viện | Nhẹ | FR-019 |
| Giao dịch cần duyệt được lập | Quản lý viện | Nhẹ | FR-013 |
| Giao dịch bị từ chối; lệnh duyệt bị chặn vì người nhận hoặc giới hạn số tiền | Kế toán | Nhẹ | FR-013, FR-014 |
| Hợp đồng bị hủy hoặc Hủy tiếp nhận khi còn tiền cọc; nhắc mỗi CFG-M02-09 | Kế toán | Nhẹ | FR-015a |
| Số dư khác 0 sau khi hồ sơ ở trạng thái cuối; nhắc mỗi CFG-M02-09 | Kế toán | Nhẹ | FR-024 |
| *(Q-378)* Phiếu báo phí được phát hành | Người thân có quyền xem chi phí (phần "chi phí") | Nhẹ | FR-031 |
| *(Q-379)* Bảng quyết toán được dựng (mỗi lần xác nhận quyết toán) | Người thân có quyền xem chi phí (phần "chi phí") | Nhẹ | FR-034 |

#### J. Chứng từ cho gia đình *(bổ sung 2026-10-22, Q-377 → Q-379; sửa theo Q-385)*

- **FR-031**: Khi một bảng chi phí (thường, bổ sung hoặc kỳ cuối) Đã chốt và đã có giao dịch thanh toán (FR-012), Kế toán MUST phát hành được phiếu báo phí cho bảng đó; mỗi bảng tối đa một phiếu, lệnh phát hành lần hai bị từ chối. Phiếu MUST mang mã duy nhất (DBR-37). Kế toán MUST có danh sách bảng Đã chốt chưa phát hành phiếu. *(Nguồn: BR-M11-21, DBR-37, UC-100)*
- **FR-032**: Phiếu báo phí MUST gồm: thông tin viện (CFG-M11-11); mã phiếu, ngày phát hành, người phát hành; người cao tuổi, người đại diện, kỳ, loại bảng; từng khoản Đã chốt (ngày phát sinh, loại, mô tả, số lượng, đơn giá, thành tiền) và tổng theo loại; tổng bảng; số dư ngay trước và ngay sau giao dịch thanh toán của bảng; khi số dư sau âm: số còn nợ, mã nộp tiền, tài khoản thu và mã QR VietQR (CFG-M11-10, FR-003). Phiếu có bản PDF và Excel. *(Nguồn: BR-M11-21)*
- **FR-033**: *(Bỏ 2026-10-22, Q-385: không in phiếu thu, phiếu chi.)*
- **FR-034** *(viết lại 2026-10-22, Q-379, Q-385)*: Kế toán MUST thực hiện được lệnh **Xác nhận quyết toán** khi: hồ sơ có hồ sơ kết thúc lưu trú Đang chuẩn bị hoặc danh sách việc sau qua đời đang mở, hoặc hồ sơ đã ở trạng thái cuối mà có giao dịch mới sau lần xác nhận trước (FR-024); ba ý về tiền của FR-022 đã đạt hoặc đã có ngoại lệ được duyệt (feature 004 FR-064 cho hồ sơ kết thúc lưu trú, FR-071a cho danh sách việc sau qua đời, Q-387); không còn giao dịch Chờ duyệt (ý này không có ngoại lệ, BR-M11-22; sửa 2026-10-22). Ngược lại lệnh bị từ chối kèm lý do. Lệnh MUST dựng **bảng quyết toán** (PDF) gồm: thông tin viện; người cao tuổi, người đại diện, loại lưu trú, hợp đồng, ngày bắt đầu, ngày kết thúc dự kiến, trường hợp kết thúc (feature 004); tổng bảng kỳ cuối theo loại; số dư trước quyết toán; tiền cọc đã thu, đã cấn trừ, đã hoàn; các giao dịch nộp, hoàn trong quyết toán; số còn nợ hoặc còn phải hoàn (khác 0 chỉ khi có ngoại lệ); người xác nhận, thời điểm, số phiên bản. Mỗi lần xác nhận tạo một phiên bản mới làm phiên bản hiện hành; phiên bản cũ giữ để tra cứu, không sửa, không xóa (DBR-38). Sau khi xác nhận mà có giao dịch Đã xác nhận mới của hồ sơ, ý "đã xác nhận quyết toán" của FR-022 MUST về Chưa đạt tới khi xác nhận lại. Kế toán MAY đính kèm bản scan có chữ ký của người đại diện vào phiên bản hiện hành; việc này không đổi điều kiện. *(Nguồn: BR-M11-22, 6.8, UC-101, DBR-38)*
- **FR-035**, **FR-036**: *(Bỏ 2026-10-22, Q-385: không còn vòng đời biên bản và biên bản bổ sung; thay bằng phiên bản ở FR-034.)*
- **FR-037**: Với mọi chứng từ của mục này: file MUST dựng từ số liệu tại lúc phát hành và được lưu cùng chứng từ; tải lại MUST trả đúng file đã lưu dù dữ liệu nguồn đổi; chứng từ MUST ghi dòng "Chứng từ này không thay thế hóa đơn"; khoản thuốc chỉ ghi "Thuốc" và mã vật phẩm (Q-133); chứng từ MUST NOT sửa hay xóa; mỗi lần phát hành, tải MUST ghi nhật ký (19.4) như FR-027; chứng từ lưu giữ theo FR-029. *(Nguồn: BR-M11-23, 1.2, Q-377)*
- **FR-038**: Hệ thống MUST cung cấp cho feature 012 danh sách phiếu báo phí và phiên bản hiện hành của bảng quyết toán của người cao tuổi kèm file để người thân có quyền xem chi phí tải trên cổng. *(Nguồn: 15.9)*

#### H. Giao tiếp với feature khác

| Hướng | Feature | Nội dung | Dữ liệu tối thiểu |
| --- | --- | --- | --- |
| Nhận | 004 | Hợp đồng chuyển Chờ ký, Trả về nháp, Hiệu lực, Đã hủy; Hủy tiếp nhận; khoản cần đặt cọc (hoặc "không yêu cầu") của hợp đồng Chờ ký hoặc hiệu lực, kể cả khi phụ lục đổi khoản cọc; hồ sơ kết thúc lưu trú lập, hủy; qua đời; danh sách việc sau qua đời; ngoại lệ quyết toán được duyệt; thông tin hồ sơ kết thúc cho bảng quyết toán (loại lưu trú, hợp đồng, ngày bắt đầu, ngày kết thúc dự kiến, trường hợp kết thúc) | Hồ sơ, hợp đồng, số tiền cọc, trạng thái |
| Gửi | 004 | Trạng thái đặt cọc (FR-006); điều kiện quyết toán (FR-022, kể cả ý xác nhận quyết toán); kết quả mục danh sách việc sau qua đời (FR-023) | Hồ sơ, trạng thái, lý do, giao dịch nguồn |
| Nhận | 010 | Bảng chi phí Đã chốt (feature 010 bảng giao tiếp "Gửi 017"); chi tiết khoản Đã chốt cho phiếu báo phí; tổng chi phí chưa chốt (feature 010 FR-038a); phí ngày cơ sở (feature 010 FR-009); đề nghị mua hộ được gửi, được quyết định, bị hủy | Mã bảng (duy nhất), loại bảng, kỳ, hồ sơ, tổng có dấu, khoản; tổng chưa chốt; đề nghị, số tiền dự kiến |
| Gửi | 010 | Dấu "số dư không đủ" cho đề nghị mua hộ (FR-021) | Đề nghị, dấu |
| Gửi | 012 | Số dư, giao dịch, thông tin nộp tiền, sao kê (FR-003, FR-026); phiếu báo phí, bảng quyết toán kèm file (FR-038) | Theo FR-003; chứng từ: loại, mã, ngày, kỳ, phiên bản, file |
| Gửi | 009 | Các thông báo ở bảng FR-030 | Nguồn, mức, nhóm người nhận, loại thông tin ("chung", "chi phí") |
| Gửi | 016 | Số hồ sơ Sắp hết tiền, Còn nợ cho dashboard (18.5) | Hồ sơ, tình trạng, số dư |
| Nhận | 000 | Tham số CFG-M11-11 (thông tin viện trên chứng từ) | Tên, địa chỉ, mã số thuế, điện thoại, logo |

### Key Entities *(include if feature involves data)*

- **Sổ số dư (SO_DU)** – nhóm 2 dẫn xuất: hồ sơ người cao tuổi, mã nộp tiền, số dư, tình trạng số dư, ngày mở.
- **Sổ tiền cọc** – nhóm 2 dẫn xuất: hồ sơ, số dư cọc.
- **Giao dịch số dư / giao dịch tiền cọc (GIAO_DICH_SO_DU)** – nhóm 2 → 3: các trường ở FR-010; mã giao dịch ngân hàng với chuyển khoản (FR-007); giao dịch được điều chỉnh (FR-011); giao dịch liên kết của cặp cấn trừ tiền cọc (FR-015).
- **Lịch sử tình trạng số dư** – nhóm 3: hồ sơ, ngày, số ngày còn đủ tiền, tình trạng, lần báo.
- **Lần xuất** – nhóm 3: theo FR-027.
- **Phiếu báo phí (PHIEU_BAO_PHI)** – nhóm 3: mã phiếu, bảng chi phí, hồ sơ, kỳ, loại bảng, ngày phát hành, người phát hành, số dư trước và sau, file PDF, file Excel.
- **Bảng quyết toán (BANG_QUYET_TOAN)** – nhóm 3, có phiên bản: hồ sơ, hồ sơ kết thúc hoặc danh sách việc sau qua đời, số phiên bản, số liệu ở FR-034, người xác nhận, thời điểm, file PDF, bản scan chữ ký (nếu có), dấu hiện hành.
- Dùng từ feature khác: **Hợp đồng**, **Hồ sơ kết thúc lưu trú**, **Danh sách việc sau qua đời** (004); **Bảng chi phí (BANG_CHI_PHI)**, **Đề nghị mua hộ** (010); **Quan hệ người thân** (012).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Với bộ kiểm thử 3 tháng của 50 hồ sơ, 100% số dư và số dư cọc bằng tổng giao dịch Đã xác nhận tính tay; 0 giao dịch Đã xác nhận bị sửa, xóa.
- **SC-002**: 100% bảng chi phí Đã chốt có đúng một giao dịch thanh toán với số tiền bằng tổng bảng; 0 giao dịch trùng khi sự kiện chốt được gửi lặp.
- **SC-003** *(sửa 2026-10-22, Q-385)*: 100% lần ghi nhận chuyển khoản có mã giao dịch ngân hàng đã dùng hiện cảnh báo kèm giao dịch trước; Kế toán ghi nhận xong một khoản chuyển khoản trong không quá 1 phút kể từ lúc tìm người cao tuổi theo mã nộp tiền.
- **SC-004**: *(Bỏ 2026-10-22, Q-385: không còn khớp tự động sao kê.)*
- **SC-005**: 100% hồ sơ dưới ngưỡng CFG-M11-04 hoặc còn nợ được báo trong lần chạy đầu tiên của Bộ lập lịch sau khi chuyển tình trạng; 0 lần báo lặp sớm hơn CFG-M11-05.
- **SC-006**: 0 lệnh chăm sóc, thuốc, suất ăn bị chặn vì số dư.
- **SC-007**: 0 hoàn tiền, điều chỉnh tác động số dư khi chưa được Quản lý viện duyệt; 0 hoàn tiền vượt số dư tại lúc duyệt.
- **SC-008**: 100% lệnh Kết thúc lưu trú thành công có điều kiện quyết toán Đạt hoặc Đạt (ngoại lệ).
- **SC-009**: File sao kê và báo cáo thu chi (Excel và PDF) khớp 100% với giao dịch Đã xác nhận trong khoảng (số dư đầu kỳ + tổng giao dịch = số dư cuối kỳ).
- **SC-010**: 0 lần số dư, thông tin nộp tiền hoặc chứng từ hiển thị cho người thân không có quyền xem chi phí, hoặc cho Nhân viên chăm sóc, Điều dưỡng, Bác sĩ.
- **SC-011**: *(Bỏ 2026-10-22, Q-385: không còn chốt quỹ ngày và dãy số phiếu thu.)*
- **SC-012**: Bộ lập lịch tính số ngày còn đủ tiền và tình trạng số dư cho 300 hồ sơ xong trong không quá 5 phút; chạy lại cùng ngày không gửi thông báo trùng (NFR-04).
- **SC-013** *(sửa 2026-10-22, Q-385)*: 100% cặp cấn trừ tiền cọc có hai giao dịch cùng số tiền, ngược dấu, cùng trạng thái.
- **SC-014** *(Q-378)*: 100% bảng chi phí Đã chốt trong đợt thử có tối đa một phiếu báo phí; tổng phiếu bằng tổng bảng và số dư trước, sau khớp sổ.
- **SC-015** *(Q-379, sửa theo Q-385)*: 100% lệnh Kết thúc lưu trú thành công có bảng quyết toán hiện hành dựng sau giao dịch cuối cùng của hồ sơ; số liệu trên bảng khớp 100% sổ số dư, sổ cọc và bảng kỳ cuối.
- **SC-016** *(Q-377)*: 100% chứng từ có dòng "Chứng từ này không thay thế hóa đơn"; 0 chứng từ có tên thuốc; tải lại sau khi dữ liệu nguồn đổi cho file giống hệt bản đã lưu.
- **SC-017**: Phát hành một phiếu báo phí 50 khoản (PDF và Excel) hoặc dựng một bảng quyết toán xong trong không quá 5 giây.

## Assumptions

- Số feature `017` là số kế tiếp sau spec 016; tài liệu nguồn (1.6, 24.2 Q-211) ghi "số dư và thu chi chưa có spec".
- Sổ được mở khi hợp đồng đầu tiên chuyển Chờ ký, để thu cọc lúc gia đình đến ký (Q-225, 15.9).
- Mã QR không kèm số tiền, vì gia đình nộp số tiền tùy ý; mã nộp tiền đủ để Kế toán nhận ra người nộp.
- Kế toán không cần duyệt cho nộp tiền và cấn trừ cọc (bảng 15.9 không nêu người duyệt). Kiểm soát tiền mặt, đối soát ngân hàng làm ở phần mềm kế toán (Q-385).
- Số ngày còn đủ tiền làm tròn xuống, để báo sớm hơn thay vì muộn hơn.
- Người nhận thông báo "sắp hết tiền" là mọi người đại diện có quan hệ Hiệu lực (BR-M11-13).
- Thời hạn lưu giữ giao dịch và chứng từ theo NFR-07, CFG-M15-03 (như chi phí đã chốt).
- Tham số dùng: CFG-M11-04, CFG-M11-05, CFG-M11-10, CFG-M11-11, CFG-M02-09, CFG-M14-01.
- "Excel", "PDF", "VietQR", "mã giao dịch ngân hàng" là định dạng và chuẩn nghiệp vụ đã có ở 15.9, 15.9a, 23, không phải lựa chọn công nghệ (constitution II).
- Còn nợ → Sắp hết tiền không báo ngay, vì tình trạng đã tốt lên và gia đình vừa được báo trước đó (FR-018).
- Hoàn tiền giữa kỳ bị giới hạn bởi tổng chi phí chưa chốt để không tạo nợ mới (FR-014, BR-M11-14).

## Điểm cần báo lại về tài liệu nguồn

Các điểm 1 → 14 của các lượt 2026-09-28 → 2026-10-10 đã xử lý (xem lịch sử git của file này). Riêng điểm 7 (chốt quỹ ngày, số phiếu thu) và điểm 8 (cặp điều chỉnh chuyển tiền) đã bị bỏ theo Q-385.

**(2026-10-22) Đồng bộ Q-377 → Q-379, Q-381, Q-385; viết lại spec.** *(Đã xử lý 2026-10-22: Clarifications "Cập nhật 2026-10-22", toàn bộ spec.)* Còn báo lại:

1. Plan, data-model, contracts (api-017), quickstart, tasks viết theo bản cũ (chốt quỹ ngày, đối soát sao kê, giao dịch đảo, cặp điều chỉnh, số phiếu, biên bản có vòng đời); cần /speckit-plan và /speckit-tasks lại theo spec này. Plan cần chọn thư viện dựng PDF, Excel và nơi lưu file (S3 như tệp đính kèm, 001 H-13).
2. Tên, mẫu chứng từ cần đối chiếu Nghị định 123/2020/NĐ-CP (24.4, Q-377) trước khi vận hành.

**(2026-10-22, sau plan) Ý "không còn giao dịch Chờ duyệt" với ngoại lệ.** *(Đã xử lý 2026-10-22: chủ dự án chọn theo BR-M11-22; FR-022 tách hai phần, ngoại lệ chỉ thay ba ý về tiền; FR-034 và User Story 7 kịch bản 4 sửa theo; plan 017 điểm báo lại 1.)*

**(2026-10-22) Q-387.** *(Đã xử lý 2026-10-22: Clarifications, FR-023, FR-034, User Story 10 kịch bản 5; plan 017 điểm báo lại 3.)*
