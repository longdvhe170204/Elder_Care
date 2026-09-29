# Feature Specification: Số dư và thu chi của người cao tuổi

**Feature Branch**: `017-resident-balance-ledger`

**Created**: 2026-09-28

**Status**: Draft

**Input**: User description: "Quản lý số dư và thu chi của người cao tuổi theo docs/nghiep-vu.md mục 15.9 (Module 11, BR-M11-10 → BR-M11-15), 6.5 (đặt cọc do Kế toán thu), 6.8 (quyết toán số dư và tiền cọc khi kết thúc lưu trú, Q-222), mục 23 (ngân hàng, sao kê), luồng BF-16 ở docs/luong-nghiep-vu.md; quyết định Q-211, Q-212, Q-219, Q-222; UC-12, UC-64, UC-85 → UC-88; actor Kế toán (AC-14), Ngân hàng (AC-15); DBR-28 → DBR-30; CFG-M11-04, CFG-M11-05. Gồm: sổ số dư và sổ tiền cọc; mã nộp tiền, mã QR VietQR; ghi nộp tiền mặt; nhập sao kê và đối soát chuyển khoản; tự trừ số dư khi bảng chi phí Đã chốt (sự kiện từ feature 010); hoàn tiền, điều chỉnh, giao dịch đảo có Quản lý viện duyệt; báo sắp hết tiền/còn nợ cho người đại diện và Kế toán; quyết toán khi kết thúc lưu trú và qua đời; xuất sao kê, báo cáo thu chi, sổ tiền cọc ra Excel; người thân xem số dư trên cổng."

## Clarifications

### Session 2026-09-28

- Q: Khi tính "số ngày còn đủ tiền" cho người bán trú, "phí một ngày" được tính thế nào? → A: Hợp đồng giá tháng: giá tháng ÷ số ngày của tháng hiện tại; hợp đồng giá buổi: giá buổi × số buổi có lịch trong 7 ngày tới ÷ 7 (chốt theo Mặc định Q-223).
- Q: Số dư khác 0 xuất hiện sau khi hồ sơ đã ở trạng thái cuối (bảng bổ sung chốt muộn, chuyển khoản tới muộn, số dư dương chưa hoàn) được xử lý thế nào? → A: Vẫn ghi giao dịch bình thường; nhắc Kế toán mỗi CFG-M02-09 tới khi số dư về 0; hồ sơ không mở lại (chốt theo Mặc định Q-224).
- Q: Kế toán có được thu tiền cọc khi hợp đồng còn "Chờ ký" không? → A: Có. Sổ số dư, sổ tiền cọc và mã nộp tiền được mở khi hợp đồng đầu tiên chuyển Chờ ký; hợp đồng bị hủy trước khi Hiệu lực (hoặc Hủy tiếp nhận) thì Kế toán lập hoàn cọc, Quản lý viện duyệt.
- Q: Phiếu thu tiền mặt của Kế toán có cần bước kiểm soát thứ hai không? → A: Phiếu thu có hiệu lực ngay, có số phiếu liên tục; cuối ngày Kế toán lập chốt quỹ ngày (thu, chi tiền mặt, tiền thực có, chênh lệch) và Quản lý viện xác nhận; lệch thì ghi lý do, không đổi số dư của người cao tuổi.

## Phạm vi

**Trong phạm vi** (Module 11, mục 15.9; 6.5; 6.8 phần quyết toán; UC-12, UC-85 → UC-88; BF-16):

1. Sổ số dư và sổ tiền cọc của từng người cao tuổi; mã nộp tiền; thông tin nộp tiền (số tài khoản của viện, mã QR theo chuẩn VietQR) cung cấp cho cổng người thân (15.9, DBR-28, DBR-29).
2. Ghi thu tiền mặt (có số phiếu) vào số dư hoặc vào sổ tiền cọc; xác định trạng thái đặt cọc cho feature 004; chốt quỹ tiền mặt cuối ngày có Quản lý viện xác nhận (6.5, UC-12, UC-85; Clarification 2026-09-28).
3. Nhập sao kê ngân hàng, khớp tự động theo mã nộp tiền, gán thủ công, xác nhận thành giao dịch nộp chuyển khoản (BR-M11-12, UC-86).
4. Tự tạo giao dịch "thanh toán bảng chi phí" khi bảng chi phí Đã chốt (BR-M11-11, DBR-30).
5. Hoàn tiền, cấn trừ tiền cọc, điều chỉnh, giao dịch đảo; Quản lý viện duyệt. Hoàn cọc khi hợp đồng bị hủy trước khi Hiệu lực hoặc Hủy tiếp nhận (BR-M11-10, BR-M11-14; Clarification 2026-09-28).
6. Tính số ngày còn đủ tiền hằng ngày; báo "sắp hết tiền" và "còn nợ" (BR-M11-13, UC-87, CFG-M11-04, CFG-M11-05).
7. Điều kiện "số dư và tiền cọc đã quyết toán" khi kết thúc lưu trú, và mục tương ứng trong danh sách việc sau qua đời (6.8, Q-222).
8. Xuất sao kê số dư, báo cáo thu chi, sổ tiền cọc ra Excel (UC-88, 18.4).
9. Dấu "số dư không đủ" cho đề nghị mua hộ (BR-M11-15).

**Ngoài phạm vi** (spec này **nhận** sự kiện hoặc **cung cấp** dữ liệu cho feature sở hữu):

- Quy tắc dùng chung (nhóm dữ liệu, lý do bắt buộc, vòng đời yêu cầu phê duyệt, nhật ký, tham số, "hoặc toàn bộ, hoặc không", Bộ lập lịch chạy lại không tạo trùng): feature 000. Spec này kế thừa và không lặp lại.
- Hợp đồng, khoản cần đặt cọc, hồ sơ kết thúc lưu trú, danh sách việc sau qua đời, yêu cầu ngoại lệ kết thúc lưu trú: feature 004. Spec này cung cấp trạng thái đặt cọc và trạng thái điều kiện quyết toán.
- Khoản chi phí, bảng chi phí, chốt bảng, chi phí tạm tính (Q-136), đề nghị mua hộ, file kế toán (UC-64): feature 010. Người xuất file kế toán đổi từ Hành chính sang Kế toán theo Q-212; việc này thuộc spec 010.
- Hiển thị trên cổng người thân và quyền xem chi phí của từng người thân: feature 012. Gửi thông báo: feature 009. Báo cáo, dashboard chung: feature 016.
- Hạch toán, báo cáo tài chính của viện, hóa đơn, thanh toán trực tuyến, kết nối trực tiếp với ngân hàng (1.2, 15.1, 23). Xử lý khoản thiếu hoặc thừa quỹ tiền mặt phát hiện ở chốt quỹ ngày (bồi hoàn, hạch toán) cũng thuộc kế toán của viện; spec này chỉ ghi nhận chênh lệch và lý do.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Mở sổ số dư, cấp mã nộp tiền, người thân xem số dư và thông tin nộp tiền (Priority: P1)

Khi hợp đồng đầu tiên của một người cao tuổi chuyển Chờ ký, hệ thống mở sổ số dư và sổ tiền cọc, số dư ban đầu bằng 0, và cấp một mã nộp tiền không đổi. Người thân có quyền xem chi phí mở cổng thấy số dư, lịch sử giao dịch, số tài khoản của viện, mã nộp tiền và mã QR có sẵn nội dung chuyển khoản. Kế toán, Hành chính, Quản lý viện thấy số dư trên hồ sơ người cao tuổi. Nhân viên chăm sóc, Điều dưỡng, Bác sĩ không thấy số dư.

**Why this priority**: Không có sổ và mã nộp tiền thì không thể ghi thu, đối soát hay trừ tiền. Đây là yêu cầu trực tiếp của góp ý "trong hồ sơ nên có bao nhiêu tiền" (Q-211).

**Independent Test**: Cho hợp đồng của A chuyển Chờ ký. Kiểm tra sổ, mã nộp tiền, và hiển thị theo từng vai trò.

**Acceptance Scenarios**:

1. **Given** A chưa có hợp đồng nào từng ở Chờ ký, **When** hợp đồng đầu tiên của A chuyển Chờ ký (feature 004), **Then** A có đúng một sổ số dư (số dư 0) và một sổ tiền cọc (số dư cọc 0), và một mã nộp tiền duy nhất (DBR-28, DBR-29; Clarification 2026-09-28).
2. **Given** A đã có sổ, **When** hợp đồng Chờ ký bị Trả về nháp rồi gửi ký lại, hợp đồng chuyển Hiệu lực, hoặc A có phụ lục, hợp đồng mới (gia hạn), **Then** không có sổ mới; mã nộp tiền không đổi.
3. **Given** người thân R của A có quyền xem chi phí, **When** R mở cổng, **Then** R thấy số dư, lịch sử giao dịch, số tài khoản thu của viện, mã nộp tiền và mã QR có nội dung chuyển khoản chứa mã nộp tiền (15.9, 23).
4. **Given** người thân S của A không có quyền xem chi phí, **When** S mở cổng, **Then** S không thấy số dư hay thông tin nộp tiền.
5. **Given** Điều dưỡng D mở hồ sơ A, **When** hồ sơ hiển thị, **Then** không có số dư (19.3).
6. **Given** A từng kết thúc lưu trú và quay lại với hồ sơ mới (Q-12), **When** hợp đồng đầu tiên của hồ sơ mới chuyển Chờ ký, **Then** hồ sơ mới có sổ mới và mã nộp tiền mới; sổ cũ giữ nguyên chỉ đọc.

---

### User Story 2 - Kế toán ghi thu tiền mặt và thu tiền cọc (Priority: P1)

Gia đình nộp tiền mặt tại quầy. Kế toán lập phiếu thu vào số dư, hoặc phiếu thu tiền cọc, ghi số tiền, người nộp, chứng từ. Giao dịch có hiệu lực ngay. Khi tổng tiền cọc đã thu không nhỏ hơn khoản cần đặt cọc của hợp đồng, đặt cọc tự chuyển "Đã đáp ứng", làm điều kiện Hoàn tất tiếp nhận của feature 004.

**Why this priority**: Thu cọc là điều kiện tiếp nhận (5.6). Việc chuyển thu cọc từ Hành chính sang Kế toán là quyết định Q-212.

**Independent Test**: Hợp đồng của A cần cọc 20.000.000 đồng. Thu 2 lần 10.000.000 đồng. Kiểm tra trạng thái đặt cọc sau từng lần và việc Hành chính không lập được phiếu thu.

**Acceptance Scenarios**:

1. **Given** hợp đồng của A đang Chờ ký, cần cọc 20.000.000 đồng, **When** Kế toán ghi thu cọc tiền mặt 10.000.000 đồng, **Then** sổ cọc là 10.000.000 đồng, đặt cọc vẫn "Chưa đáp ứng" (6.5; Clarification 2026-09-28).
2. **Given** tiếp theo, **When** Kế toán ghi thu cọc chuyển khoản 10.000.000 đồng từ dòng sao kê đã đối soát (User Story 3), **Then** sổ cọc là 20.000.000 đồng, đặt cọc chuyển "Đã đáp ứng", feature 004 nhận trạng thái mới.
3. **Given** hợp đồng không yêu cầu đặt cọc, **When** hợp đồng chuyển Chờ ký, **Then** đặt cọc ở "Không yêu cầu", được coi là đạt, không cần giao dịch nào (feature 004 FR-031).
4. **Given** Kế toán ghi thu tiền mặt 5.000.000 đồng vào số dư của A, **When** lưu, **Then** giao dịch "Nộp tiền mặt" Đã xác nhận, số dư tăng 5.000.000 đồng; giao dịch không sửa, không xóa được (BR-M11-10).
5. **Given** Hành chính H, **When** H tìm cách ghi thu tiền, **Then** hệ thống chặn; H chỉ xem được số dư và trạng thái đặt cọc (Phụ lục 27 ²⁶).
6. **Given** phiếu thu số tiền 0 hoặc âm, **When** lưu, **Then** hệ thống chặn.
7. **Given** A đã nộp cọc 20.000.000 đồng khi hợp đồng Chờ ký, **When** hợp đồng chuyển Đã hủy (hoặc A Hủy tiếp nhận) trước khi Hiệu lực, **Then** Kế toán được nhắc lập hoàn cọc 20.000.000 đồng cho người đại diện; hoàn cọc chỉ có hiệu lực khi Quản lý viện duyệt; Kế toán được nhắc mỗi CFG-M02-09 mặc định \[1 ngày\] tới khi số dư cọc về 0 (FR-015a, Clarification 2026-09-28).
8. **Given** ngày 05/10 Kế toán lập phiếu thu tiền mặt số 101, 102, 103 tổng 25.000.000 đồng, **When** cuối ngày Kế toán lập chốt quỹ với tiền thực có 24.500.000 đồng mà không ghi lý do, **Then** hệ thống chặn. **When** Kế toán ghi lý do "thiếu 500.000 đồng, đang tìm", **Then** chốt quỹ ở Đã lập, Quản lý viện được báo; số dư của các cụ không đổi. **When** tới hết giờ hành chính CFG-M13-06 ngày 06/10 mà ngày 06/10 có phiếu thu nhưng chưa có chốt quỹ, **Then** Kế toán được nhắc (FR-005a, Clarification 2026-09-28).

---

### User Story 3 - Nhập sao kê và đối soát chuyển khoản (Priority: P1)

Kế toán tải sao kê tài khoản thu của viện từ ngân hàng và nhập vào hệ thống. Hệ thống bỏ qua dòng đã nhập trước đó, khớp từng dòng mới theo mã nộp tiền trong nội dung, đưa dòng không khớp hoặc khớp nhiều mã vào danh sách "chưa khớp". Kế toán xác nhận dòng đã khớp thành giao dịch nộp tiền (vào số dư hoặc vào sổ cọc), gán thủ công dòng chưa khớp có lý do, hoặc đánh dấu "không thuộc người cao tuổi".

**Why this priority**: Chuyển khoản là kênh nộp tiền chính; không đối soát thì số dư sai (Q-211). Hệ thống không kết nối trực tiếp với ngân hàng (23).

**Independent Test**: Sao kê 6 dòng: 3 dòng đúng mã, 1 dòng không có mã, 1 dòng chứa 2 mã, 1 dòng đã nhập ở lần trước. Kiểm tra kết quả khớp và việc nhập lại cùng file.

**Acceptance Scenarios**:

1. **Given** sao kê có dòng mã giao dịch ngân hàng NH001, 3.000.000 đồng, nội dung chứa đúng mã nộp tiền của A, **When** Kế toán nhập, **Then** dòng ở "Đã khớp" với A; số dư A chưa đổi (BR-M11-12).
2. **Given** dòng NH001 Đã khớp, **When** Kế toán xác nhận vào số dư, **Then** có giao dịch "Nộp chuyển khoản" Đã xác nhận 3.000.000 đồng, trỏ về dòng NH001; số dư A tăng.
3. **Given** dòng NH002 không chứa mã nộp tiền nào, **When** nhập, **Then** dòng ở "Chưa khớp". **When** Kế toán gán cho B kèm lý do "gia đình ghi tên cụ", **Then** dòng thành giao dịch nộp tiền của B.
4. **Given** dòng NH003 chứa mã của A và mã của B, **When** nhập, **Then** dòng ở "Chưa khớp" với lý do "khớp nhiều mã"; không tự tạo giao dịch.
5. **Given** dòng NH001 đã nhập ở lần trước, **When** Kế toán nhập lại file có NH001, **Then** NH001 bị bỏ qua và được báo "đã nhập trước đó"; không có giao dịch thứ hai (DBR-29).
6. **Given** dòng NH004 là tiền điện của viện, **When** Kế toán đánh dấu "không thuộc người cao tuổi" kèm lý do, **Then** dòng không tạo giao dịch và không còn trong danh sách chưa khớp.
7. **Given** dòng NH005 khớp mã của C nhưng C đã ở trạng thái cuối, **When** Kế toán xác nhận, **Then** giao dịch vẫn được ghi (tiền đã về viện) và Kế toán được nhắc lập hoàn tiền (FR-024).
8. **Given** dòng NH006 25.000.000 đồng có mã nộp tiền của A, gia đình báo gồm 20.000.000 đồng tiền cọc và 5.000.000 đồng nộp số dư, **When** Kế toán xác nhận toàn bộ dòng vào sổ cọc rồi lập cặp điều chỉnh chuyển tiền 5.000.000 đồng từ sổ cọc sang số dư, **Then** dòng sinh đúng một giao dịch; cặp điều chỉnh ở Chờ duyệt; khi Quản lý viện duyệt, sổ cọc 20.000.000 đồng, số dư +5.000.000 đồng (FR-009a).

---

### User Story 4 - Chốt bảng chi phí tự trừ số dư (Priority: P1)

Khi Quản lý viện chốt bảng chi phí của một người (feature 010), hệ thống tạo ngay một giao dịch "thanh toán bảng chi phí" bằng tổng bảng. Bảng bổ sung có tổng âm làm tăng số dư. Số dư được phép âm, và khi âm được hiển thị là "còn nợ".

**Why this priority**: Đây là nối kết giữa chi phí và tiền (Q-211); không có bước này số dư không phản ánh thực tế.

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

**Why this priority**: Trả lời trực tiếp câu hỏi "ai thông báo khi các cụ sắp hết tiền" (Q-211): hệ thống tự báo, Kế toán theo dõi.

**Independent Test**: Dùng đồng hồ giả lập, cho A có số dư giảm dần qua ngưỡng CFG-M11-04 rồi âm; kiểm tra ngày gửi, người nhận, mức, và việc dừng nhắc khi gia đình nộp tiền.

**Acceptance Scenarios**:

1. **Given** hợp đồng của A giá tháng 9.000.000 đồng, tháng hiện tại 30 ngày (phí ngày 300.000 đồng), số dư 6.000.000 đồng, tổng chi phí chưa chốt 2.400.000 đồng, CFG-M11-04 mặc định \[15 ngày\], **When** Bộ lập lịch chạy, **Then** số ngày còn đủ tiền là 12; tình trạng "Sắp hết tiền"; người đại diện của A nhận thông báo mức Trung bình loại "chi phí"; Kế toán nhận mức Nhẹ (BR-M11-13).
2. **Given** A đang "Sắp hết tiền" và đã được báo ngày 01/11, CFG-M11-05 mặc định \[7 ngày\], **When** Bộ lập lịch chạy các ngày 02/11 → 07/11 và A vẫn dưới ngưỡng, **Then** không có thông báo mới; ngày 08/11 có thông báo nhắc lại.
3. **Given** A "Sắp hết tiền", **When** gia đình nộp 10.000.000 đồng và lần tính kế tiếp cho số ngày còn đủ tiền là 45, **Then** tình trạng về "Bình thường", không nhắc nữa.
4. **Given** số dư A âm từ ngày 01/11, **When** Bộ lập lịch chạy ngày 08/11 (quá CFG-M11-05), **Then** Quản lý viện được báo thêm, cùng với nhắc lại cho người đại diện và Kế toán.
5. **Given** A "Còn nợ", **When** điều dưỡng phát thuốc, nhân viên ghi công việc, bếp chốt suất, **Then** không có thao tác nào bị chặn vì số dư (Q-219).
6. **Given** A có hai người đại diện P1 (có quyền xem chi phí) và P2 (không có), **When** A chuyển "Sắp hết tiền", **Then** cả hai nhận thông báo; P1 thấy số dư, tổng chi phí chưa chốt, thông tin nộp tiền; P2 chỉ thấy phần "chung" đề nghị liên hệ Kế toán; người thân không phải người đại diện không nhận (FR-018, Q-105).
7. **Given** A đã "Còn nợ" từ ngày 01/11, **When** ngày 05/11 gia đình nộp một phần, hết nợ nhưng số ngày còn đủ tiền là 4, **Then** tình trạng chuyển "Sắp hết tiền", không có thông báo ngay; lần nhắc tiếp theo vẫn là ngày 08/11 (bảng chuyển tình trạng số dư).

---

### User Story 6 - Hoàn tiền, điều chỉnh, giao dịch đảo có Quản lý viện duyệt (Priority: P2)

Kế toán lập hoàn tiền (trả số dư dương hoặc tiền cọc cho người đại diện), điều chỉnh (tăng hoặc giảm, có lý do), hoặc giao dịch đảo cho một giao dịch ghi sai. Các giao dịch này ở "Chờ duyệt" và chỉ tác động số dư khi Quản lý viện duyệt.

**Why this priority**: Cần cho sai sót và kết thúc lưu trú, nhưng không nằm trong luồng thu hằng ngày.

**Independent Test**: A có số dư 2.000.000 đồng. Lập hoàn 3.000.000 đồng (bị chặn), hoàn 2.000.000 đồng (duyệt), đảo một phiếu thu ghi nhầm người.

**Acceptance Scenarios**:

1. **Given** số dư A 2.000.000 đồng, **When** Kế toán lập hoàn 3.000.000 đồng, **Then** hệ thống chặn vì vượt số dư (BR-M11-14).
2. **Given** Kế toán lập hoàn 2.000.000 đồng cho người đại diện P, phương thức chuyển khoản, **When** lưu, **Then** giao dịch ở Chờ duyệt, số dư chưa đổi, Quản lý viện được báo. **When** Quản lý viện duyệt, **Then** giao dịch Đã xác nhận, số dư 0.
3. **Given** hoàn tiền Chờ duyệt 2.000.000 đồng, **When** trước lúc duyệt một bảng chi phí 500.000 đồng được chốt (số dư còn 1.500.000 đồng), **Then** lệnh duyệt bị chặn vì vượt số dư hiện có; Kế toán được báo để lập lại.
4. **Given** phiếu thu tiền mặt 5.000.000 đồng ghi nhầm cho A thay vì B, **When** Kế toán lập giao dịch đảo cho phiếu đó và Quản lý viện duyệt, **Then** số dư A giảm 5.000.000 đồng, giao dịch gốc giữ nguyên và được đánh dấu "đã đảo"; Kế toán lập phiếu thu mới cho B (BR-M11-10, DBR-28).
5. **Given** giao dịch đã có một giao dịch đảo Đã xác nhận, **When** Kế toán lập giao dịch đảo thứ hai cho cùng giao dịch, **Then** hệ thống chặn.
6. **Given** giao dịch "Thanh toán bảng chi phí", **When** Kế toán lập giao dịch đảo cho nó, **Then** hệ thống chặn; sai sót của bảng đã chốt đi qua khoản điều chỉnh của feature 010 (DBR-17, DBR-30).
7. **Given** Quản lý viện từ chối một điều chỉnh kèm lý do, **When** lưu, **Then** giao dịch chuyển Từ chối, số dư không đổi, Kế toán được báo.
8. **Given** A đang lưu trú, số dư 5.000.000 đồng, tổng chi phí chưa chốt 4.000.000 đồng, **When** gia đình xin rút 2.000.000 đồng và Kế toán lập hoàn tiền 2.000.000 đồng, **Then** hệ thống chặn vì vượt 1.000.000 đồng được phép; hoàn 1.000.000 đồng thì được lập (FR-014).
9. **Given** hoàn tiền Chờ duyệt ghi người nhận P, **When** trước lúc duyệt P thôi là người đại diện (feature 012), **Then** lệnh duyệt bị chặn; Kế toán được báo để lập lại với người nhận khác (FR-014).

---

### User Story 7 - Quyết toán số dư và tiền cọc khi kết thúc lưu trú hoặc qua đời (Priority: P2)

Khi hồ sơ kết thúc lưu trú được lập, điều kiện "số dư và tiền cọc đã quyết toán" xuất hiện ở trạng thái Chưa đạt. Sau khi bảng kỳ cuối đã chốt và trừ vào số dư, Kế toán cấn trừ tiền cọc vào số dư âm hoặc hoàn cọc, hoàn số dư dương, hoặc ghi thu phần còn nợ. Khi sổ cọc và số dư đều bằng 0 và bảng kỳ cuối đã chốt, điều kiện Đạt. Khi qua đời, việc này là một mục trong danh sách việc sau qua đời.

**Why this priority**: Là điều kiện kết thúc lưu trú đã chốt (Q-222), nhưng chỉ xảy ra ở cuối vòng đời.

**Independent Test**: A kết thúc lưu trú với số dư −1.500.000 đồng và cọc 20.000.000 đồng. Kiểm tra trình tự cấn trừ, hoàn, và trạng thái điều kiện.

**Acceptance Scenarios**:

1. **Given** hồ sơ kết thúc của A được lập, bảng kỳ cuối chưa chốt, **When** feature 004 xem danh sách điều kiện, **Then** điều kiện "số dư và tiền cọc đã quyết toán" là Chưa đạt, kèm lý do "bảng kỳ cuối chưa chốt" (6.8).
2. **Given** bảng kỳ cuối Đã chốt, số dư −1.500.000 đồng, cọc 20.000.000 đồng, **When** Kế toán cấn trừ 1.500.000 đồng từ cọc, **Then** có hai giao dịch liên kết: sổ cọc −1.500.000 đồng, số dư +1.500.000 đồng; số dư 0, cọc 18.500.000 đồng.
3. **Given** tiếp theo, **When** Kế toán lập hoàn cọc 18.500.000 đồng cho người đại diện và Quản lý viện duyệt, **Then** cọc 0; điều kiện chuyển Đạt; feature 004 nhận trạng thái mới.
4. **Given** A còn nợ 3.000.000 đồng và cọc 0, gia đình hẹn nộp sau, **When** Quản lý viện duyệt ngoại lệ ở feature 004, **Then** điều kiện là "Đạt (ngoại lệ)"; số dư vẫn âm và tiếp tục được báo "còn nợ" (FR-020).
5. **Given** A qua đời, **When** feature 004 mở danh sách việc sau qua đời, **Then** có mục "quyết toán số dư và tiền cọc" do Kế toán thực hiện, Chưa hoàn thành cho tới khi đạt như trên, hoặc Không áp dụng nếu sổ cọc và số dư đã bằng 0 và không còn bảng chưa chốt.
6. **Given** điều kiện đã Đạt và lệnh Kết thúc lưu trú đã thực hiện, **When** sau đó một bảng bổ sung của A được chốt, **Then** giao dịch vẫn được tạo, số dư khác 0, Kế toán được nhắc quyết toán phần mới; hồ sơ không mở lại (Q-224, Clarification 2026-09-28).

---

### User Story 8 - Xuất sao kê, báo cáo thu chi và sổ tiền cọc (Priority: P2)

Kế toán xuất ra Excel: sao kê số dư của một hoặc nhiều người trong một khoảng (số dư đầu kỳ, từng giao dịch, số dư cuối kỳ); báo cáo thu chi theo khoảng (tổng nộp theo phương thức, tổng thanh toán bảng chi phí, tổng hoàn, tổng điều chỉnh, tổng số dư âm); sổ tiền cọc. Người thân có quyền xem chi phí tải được sao kê của người cao tuổi trên cổng.

**Why this priority**: Yêu cầu trực tiếp của góp ý "xuất bản ghi excel sao kê, thu chi" (Q-212), nhưng không chặn nghiệp vụ thu hằng ngày.

**Independent Test**: Với dữ liệu tháng 10 của 3 người, xuất sao kê, báo cáo thu chi, sổ cọc; so số liệu với tính tay.

**Acceptance Scenarios**:

1. **Given** A có số dư 01/10 là 1.000.000 đồng, trong tháng nộp 10.000.000 đồng, thanh toán bảng −9.300.000 đồng, **When** Kế toán xuất sao kê A từ 01/10 tới 31/10, **Then** file có số dư đầu kỳ 1.000.000 đồng, hai giao dịch, số dư cuối kỳ 1.700.000 đồng (UC-88).
2. **Given** tháng 10 có nộp tiền mặt 15.000.000 đồng và chuyển khoản 40.000.000 đồng, **When** xuất báo cáo thu chi tháng 10, **Then** tổng nộp tiền mặt và chuyển khoản hiện riêng, khớp tổng giao dịch.
3. **Given** giao dịch Chờ duyệt hoặc Từ chối, **When** xuất, **Then** không có trong sao kê và báo cáo; chỉ giao dịch Đã xác nhận được tính.
4. **Given** Hành chính H, **When** H tìm cách xuất báo cáo thu chi, **Then** hệ thống chặn (Phụ lục 27, dòng "Sao kê, báo cáo thu chi").
5. **Given** mỗi lần xuất, **When** xong, **Then** lần xuất được ghi (người xuất, thời điểm, loại, khoảng, phạm vi, số dòng); file đã xuất không đổi khi dữ liệu sau đó thay đổi.
6. **Given** khoảng xuất dài hơn CFG-M14-01 mặc định \[12 tháng\], **When** xuất, **Then** hệ thống từ chối (18.7).

---

### Edge Cases

- Người nộp tiền chuyển khoản nhiều hơn một lần cùng ngày cùng số tiền: mỗi dòng có mã giao dịch ngân hàng khác nhau nên được ghi riêng (DBR-29).
- Dòng sao kê có số tiền âm (ngân hàng thu phí, hoàn trả): không khớp tự động; Kế toán đánh dấu "không thuộc người cao tuổi" hoặc gán thủ công như một điều chỉnh có duyệt.
- Mã nộp tiền viết sai một ký tự: không khớp; vào "chưa khớp" để gán thủ công.
- Người cao tuổi đang Tạm vắng hoặc Điều trị tại bệnh viện: vẫn tính số ngày còn đủ tiền (phí lưu trú vẫn sinh theo 6.7).
- Người chưa có hợp đồng nào từng ở Chờ ký nhưng gia đình chuyển khoản trước: dòng không khớp mã nào (chưa có sổ); nằm ở "chưa khớp" tới khi sổ được mở, rồi Kế toán gán.
- Hai Kế toán cùng xác nhận một dòng: chỉ lần đầu thành công, lần sau được báo dòng đã xác nhận (DBR-29).
- Một lần chuyển khoản gồm cả tiền cọc và tiền nộp số dư, hoặc nộp chung cho hai người cao tuổi: dòng được ghi toàn bộ vào một sổ của một hồ sơ; phần thuộc sổ hoặc hồ sơ khác được chuyển bằng cặp giao dịch điều chỉnh có duyệt (FR-009a).
- Gia đình xin rút bớt tiền khi người cao tuổi đang lưu trú: được, bằng hoàn tiền có duyệt, trong giới hạn FR-014.
- Phí lưu trú một ngày bằng 0 (hợp đồng không có phí lưu trú): số ngày còn đủ tiền không xác định; chỉ báo khi số dư âm.

## Requirements *(mandatory)*

### Functional Requirements

Mọi yêu cầu dưới đây kế thừa feature 000. Phân nhóm dữ liệu của feature này:

| Dữ liệu | Nhóm (1.5) | Thao tác được phép |
| --- | --- | --- |
| Sổ số dư, sổ tiền cọc | 2 – bản ghi dẫn xuất | Mở do hệ thống; số dư chỉ thay đổi qua giao dịch Đã xác nhận |
| Mã nộp tiền | 1 | Do hệ thống cấp, không đổi, không cấp lại cho người khác |
| Giao dịch số dư, giao dịch tiền cọc | 2 khi Chờ duyệt → 3 khi Đã xác nhận | Theo bảng mục C; giao dịch Đã xác nhận chỉ ghi thêm giao dịch đảo |
| Lần nhập sao kê, dòng sao kê | 3 (nội dung dòng); 2 (trạng thái khớp) | Nội dung dòng không sửa; trạng thái theo bảng mục B |
| Tình trạng số dư (Bình thường / Sắp hết tiền / Còn nợ) | 2 – bản ghi dẫn xuất | Theo bảng chuyển tình trạng số dư; chỉ Bộ lập lịch đổi, trừ "dừng nhắc" của Quản lý viện |
| Lịch sử tình trạng và lần báo | 3 | Chỉ ghi thêm |
| Lần xuất sao kê, báo cáo | 3 | Chỉ ghi thêm |
| Chốt quỹ ngày | 2 → 3 khi Đã xác nhận | Theo bảng trạng thái chốt quỹ ngày (FR-005a) |
| Tham số CFG-M11-04, CFG-M11-05 | 1 – Tham số | Cấu hình (Quản lý viện, feature 000) |

#### A. Sổ, mã nộp tiền, hiển thị

- **FR-001**: Khi hợp đồng đầu tiên của một hồ sơ người cao tuổi chuyển Chờ ký (Clarification 2026-09-28), hệ thống MUST mở đúng một sổ số dư và một sổ tiền cọc với số dư 0, và cấp một mã nộp tiền duy nhất, không đổi, không dùng lại cho hồ sơ khác. Hồ sơ mới của người quay lại (Q-12) MUST có sổ và mã mới. *(Nguồn: 15.9, DBR-28, DBR-29, UC-85)*
- **FR-002**: Số dư MUST luôn bằng tổng các giao dịch số dư Đã xác nhận; số dư cọc MUST luôn bằng tổng các giao dịch tiền cọc Đã xác nhận. Số dư được phép âm; số dư âm MUST hiển thị là "còn nợ" kèm số tiền. *(Nguồn: BR-M11-10, BR-M11-11, DBR-28)*
- **FR-003**: Hệ thống MUST cung cấp cho feature 012: số dư, số dư cọc, lịch sử giao dịch Đã xác nhận, số tài khoản thu của viện, mã nộp tiền, mã QR theo chuẩn VietQR có nội dung chuyển khoản chứa mã nộp tiền (không kèm số tiền), sao kê tải được. Mọi thông tin này thuộc loại thông tin **"chi phí"** trong bốn loại của cổng (feature 012 FR-046); chỉ người thân có quyền xem chi phí được thấy. *(Nguồn: 15.9, 23, 19.3, Phụ lục 27 dòng "Số dư, thu chi, đối soát")*
- **FR-004**: Số dư MUST hiển thị trên hồ sơ người cao tuổi cho Kế toán, Hành chính, Quản lý viện; MUST NOT hiển thị cho vai trò khác. Giao dịch loại thanh toán bảng chi phí chỉ hiện tổng bảng, không hiện tên thuốc (Q-133). *(Nguồn: 15.9, 19.3)*

#### B. Thu tiền và đối soát

- **FR-005**: Kế toán MUST ghi được phiếu thu tiền mặt vào số dư hoặc vào sổ tiền cọc: số tiền (lớn hơn 0), người nộp, thời điểm thu, chứng từ (không bắt buộc), ghi chú. Phiếu tạo giao dịch Đã xác nhận ngay và mang **số phiếu** do hệ thống cấp, tăng liên tục, không bỏ số, không dùng lại. Phiếu thu không phải thực thể riêng: số phiếu là thuộc tính của giao dịch "Nộp tiền mặt" hoặc "Thu cọc" tiền mặt, và hai loại dùng **chung một dãy số** của toàn viện. Hoàn tiền, hoàn cọc bằng tiền mặt mang số phiếu chi theo một dãy riêng, cấp khi giao dịch Đã xác nhận. *(Nguồn: 15.9, UC-85, 6.5; Clarification 2026-09-28)*
- **FR-005a**: Với mỗi ngày có giao dịch tiền mặt (phiếu thu, hoặc hoàn tiền, hoàn cọc Đã xác nhận có phương thức tiền mặt), Kế toán MUST lập một **chốt quỹ ngày** gồm: ngày; danh sách số phiếu; tổng thu tiền mặt; tổng chi tiền mặt; số tiền phải có = tổng thu − tổng chi; số tiền thực có do Kế toán đếm; chênh lệch; lý do (bắt buộc khi chênh lệch khác 0). Quản lý viện MUST xác nhận hoặc trả lại (bắt buộc lý do). Chênh lệch MUST NOT làm thay đổi số dư của người cao tuổi; sai phiếu cụ thể xử lý bằng giao dịch đảo (FR-011). Hết giờ hành chính của ngày đó mà chưa có chốt quỹ thì Kế toán được nhắc; hết giờ hành chính của ngày làm việc kế tiếp (CFG-M13-06 mặc định \[07:30–17:00, thứ 2 → thứ 7\]) mà chưa được xác nhận thì Quản lý viện được báo. Mỗi ngày có tối đa một chốt quỹ ở Đã lập hoặc Đã xác nhận. *(Nguồn: BR-M11-16, DBR-35, UC-93, Q-226; Clarification 2026-09-28)*

**Bảng trạng thái chốt quỹ ngày** *(Clarification 2026-09-28)*:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái kế tiếp | Ai thực hiện | Điều kiện (chặn nếu không đạt) | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Lập chốt quỹ | Đã lập | Kế toán | Ngày đã có giao dịch tiền mặt; ngày chưa có chốt quỹ Đã lập hoặc Đã xác nhận; có lý do khi chênh lệch khác 0 | Báo Quản lý viện |
| Đã lập | Xác nhận | Đã xác nhận | Quản lý viện | — | — |
| Đã lập | Trả lại | Bị trả lại | Quản lý viện | Có lý do | Báo Kế toán; Kế toán lập chốt quỹ mới cho ngày đó |
| Đã xác nhận | Có giao dịch tiền mặt mới được xác nhận cho ngày đó (ví dụ hoàn tiền được duyệt muộn) | Đã xác nhận (giữ) | Hệ thống | — | Ghi dấu "phát sinh sau chốt quỹ"; nhắc Kế toán lập chốt quỹ bổ sung cho ngày duyệt |

Đã xác nhận và Bị trả lại là trạng thái cuối; chốt quỹ không sửa được. Giao dịch tiền mặt được tính vào chốt quỹ của ngày nó chuyển Đã xác nhận.
- **FR-006**: Trạng thái đặt cọc là **dẫn xuất**, không có lệnh "xác nhận" riêng. Với hợp đồng đang Chờ ký hoặc Hiệu lực: "Không yêu cầu" khi hợp đồng ghi không yêu cầu đặt cọc (được coi là đạt); "Đã đáp ứng" khi số dư cọc của hồ sơ không nhỏ hơn khoản cần đặt cọc; ngược lại "Chưa đáp ứng". Trạng thái được tính lại mỗi khi sổ cọc hoặc khoản cần đặt cọc đổi (kể cả phụ lục đổi khoản cọc, giao dịch đảo một lần thu cọc). Mỗi lần trạng thái đổi, hệ thống MUST báo feature 004 kèm thời điểm, giao dịch làm thay đổi và Kế toán lập giao dịch đó (nguồn xác nhận). Feature 004 dùng trạng thái này cho điều kiện (c) của danh sách điều kiện tiếp nhận (feature 004 FR-004, FR-033). *(Nguồn: 6.5, 5.6, UC-12, Q-212)*
- **FR-007**: Kế toán MUST nhập được sao kê tài khoản thu của viện (Excel/CSV). Mỗi dòng MUST có tối thiểu: mã giao dịch ngân hàng, thời điểm, số tiền, nội dung. File không đọc được, hoặc thiếu hẳn một trong bốn cột trên, MUST bị từ chối toàn bộ. Với file hợp lệ: dòng thiếu giá trị bị từ chối và được liệt kê; dòng có mã giao dịch ngân hàng đã có MUST bị bỏ qua và được báo "đã nhập trước đó"; các dòng còn lại được nhận theo nguyên tắc "hoặc toàn bộ, hoặc không" của feature 000, tức là lỗi giữa chừng không để lại dòng nào của lần nhập đó. Mỗi lần nhập được ghi (người nhập, thời điểm, tên file, số dòng nhận, bỏ qua, từ chối). *(Nguồn: BR-M11-12, DBR-29, 23, UC-86, feature 000)*
- **FR-008**: Với mỗi dòng mới có số tiền dương, hệ thống MUST tìm mã nộp tiền có hiệu lực trong nội dung: đúng một mã thì dòng ở "Đã khớp" với hồ sơ đó; không có mã, nhiều mã, hoặc số tiền âm thì "Chưa khớp" kèm lý do. Khớp tự động MUST NOT tạo giao dịch. *(Nguồn: BR-M11-12)*
- **FR-009**: Kế toán MUST xác nhận được dòng Đã khớp thành giao dịch "Nộp chuyển khoản" vào số dư hoặc vào sổ tiền cọc; gán được dòng Chưa khớp cho một hồ sơ (bắt buộc lý do) rồi xác nhận; bỏ khớp dòng Đã khớp về Chưa khớp (bắt buộc lý do); đánh dấu dòng "Không thuộc người cao tuổi" (bắt buộc lý do). Mỗi dòng sinh tối đa một giao dịch. *(Nguồn: BR-M11-12, DBR-29)*
- **FR-009a**: Một dòng sao kê MUST NOT được tách thành nhiều giao dịch (DBR-29). Khi một lần chuyển khoản gồm tiền của hai sổ (cọc và số dư) hoặc của hai hồ sơ, Kế toán xác nhận toàn bộ dòng vào một sổ của một hồ sơ, rồi lập một **cặp điều chỉnh chuyển tiền**: giảm ở sổ hoặc hồ sơ đã ghi, tăng ở sổ hoặc hồ sơ nhận, cùng số tiền, liên kết với nhau và với dòng sao kê. Cặp này đi qua duyệt theo FR-013 và chỉ có hiệu lực khi cả hai được duyệt cùng lúc. *(Nguồn: BR-M11-12, DBR-28, DBR-29, BR-M11-10, Q-227)*

**Bảng trạng thái dòng sao kê** *(BR-M11-12)*:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái kế tiếp | Ai thực hiện | Điều kiện (chặn nếu không đạt) | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Nhập sao kê, đúng một mã nộp tiền, số tiền dương | Đã khớp | Hệ thống | Mã giao dịch ngân hàng chưa có (DBR-29) | — |
| (chưa có) | Nhập sao kê, không có mã, nhiều mã, hoặc số tiền âm | Chưa khớp | Hệ thống | Như trên | Vào danh sách chưa khớp; báo Kế toán (Nhẹ) |
| Đã khớp | Bỏ khớp | Chưa khớp | Kế toán | Có lý do | — |
| Chưa khớp | Gán cho hồ sơ | Đã khớp | Kế toán | Có lý do; hồ sơ đã có sổ | — |
| Đã khớp | Xác nhận vào số dư / vào sổ cọc | Đã ghi nhận | Kế toán | Dòng chưa sinh giao dịch | Tạo giao dịch nộp chuyển khoản Đã xác nhận; cập nhật đặt cọc (FR-006) |
| Chưa khớp | Đánh dấu không thuộc người cao tuổi | Không thuộc người cao tuổi | Kế toán | Có lý do | — |

Đã ghi nhận và Không thuộc người cao tuổi là trạng thái cuối. Sai sót sau Đã ghi nhận xử lý bằng giao dịch đảo (mục C).

#### C. Giao dịch và phê duyệt

- **FR-010**: Các loại giao dịch MUST theo bảng 15.9. Sổ số dư: Nộp tiền mặt, Nộp chuyển khoản, Thanh toán bảng chi phí, Hoàn tiền, Cấn trừ tiền cọc (tăng), Điều chỉnh, Giao dịch đảo. Sổ tiền cọc: Thu cọc (tiền mặt, chuyển khoản), Hoàn cọc, Cấn trừ tiền cọc (giảm), Điều chỉnh, Giao dịch đảo. "Cấn trừ tiền cọc" luôn là một cặp hai giao dịch cùng tên, liên kết với nhau (FR-015); "cặp điều chỉnh chuyển tiền" cũng vậy (FR-009a). Mỗi giao dịch MUST ghi: hồ sơ; sổ; loại; số tiền có dấu; thời điểm; phương thức; tham chiếu (phiếu thu, dòng sao kê, bảng chi phí, giao dịch gốc, giao dịch liên kết); người lập; người duyệt nếu có; lý do nếu có; trạng thái. *(Nguồn: 15.9)*
- **FR-011**: Giao dịch Đã xác nhận MUST NOT sửa, xóa hay hủy. Sai sót MUST xử lý bằng giao dịch đảo trỏ đúng một giao dịch gốc, có số tiền ngược dấu bằng giao dịch gốc; mỗi giao dịch gốc có tối đa một giao dịch đảo không ở Từ chối, Đã hủy. Giao dịch "Thanh toán bảng chi phí" MUST NOT bị đảo; sai sót của bảng đi qua khoản điều chỉnh của feature 010. *(Nguồn: BR-M11-10, DBR-17, DBR-28, DBR-30)*
- **FR-012**: Khi feature 010 báo một bảng chi phí (thường hoặc bổ sung) chuyển Đã chốt, hệ thống MUST tạo ngay đúng một giao dịch "Thanh toán bảng chi phí" Đã xác nhận, số tiền bằng tổng bảng với dấu ngược (tổng dương làm giảm số dư, tổng âm làm tăng, tổng 0 vẫn tạo giao dịch 0 đồng). Sự kiện nhận lặp (cùng mã bảng) MUST NOT tạo giao dịch thứ hai. Mọi hồ sơ có bảng chi phí đều đã có sổ, vì bảng chỉ có sau khi hợp đồng Hiệu lực, tức là đã qua Chờ ký (FR-001). *(Nguồn: BR-M11-11, DBR-30)*
- **FR-013**: Hoàn tiền, Hoàn cọc, Điều chỉnh, Giao dịch đảo do Kế toán lập MUST ở Chờ duyệt và chỉ tác động sổ khi Quản lý viện duyệt; dùng vòng đời yêu cầu phê duyệt chung (6.6) với các trạng thái Chờ duyệt → Đã xác nhận / Từ chối / Đã hủy. Kế toán không có quyền duyệt nên không tự duyệt (Q-10 không áp). *(Nguồn: BR-M11-10, BR-M11-14, Phụ lục 27 ³²)*
- **FR-014**: Hoàn tiền và hoàn cọc MUST ghi người nhận (một người đại diện có quan hệ Hiệu lực tại lúc lập; hồ sơ không còn người đại diện thì ghi người nhận khác và Quản lý viện duyệt kèm lý do), phương thức, chứng từ. Giới hạn số tiền: hoàn cọc không vượt số dư cọc; hoàn tiền không vượt số dư, và khi hồ sơ chưa ở trạng thái cuối thì không vượt (số dư − tổng chi phí chưa chốt, FR-016), để gia đình rút bớt tiền giữa kỳ mà không làm phát sinh nợ. Tại lúc duyệt, giới hạn số tiền và người nhận MUST được kiểm tra lại; người nhận không còn là người đại diện Hiệu lực thì lệnh duyệt bị chặn và Kế toán lập lại. *(Nguồn: BR-M11-14)*
- **FR-015**: Cấn trừ tiền cọc do Kế toán thực hiện, không cần duyệt, chỉ khi hồ sơ có hồ sơ kết thúc lưu trú đang chuẩn bị, hoặc danh sách việc sau qua đời đang mở, và bảng kỳ cuối đã chốt. Số tiền MUST NOT vượt số dư cọc và MUST NOT vượt phần còn nợ. Lệnh MUST tạo hai giao dịch liên kết trong cùng một lần: sổ cọc giảm, số dư tăng cùng số tiền. *(Nguồn: 15.9, 6.8, Q-219, Q-222)*
- **FR-015a**: Khi hợp đồng có tiền cọc đã thu chuyển Đã hủy trước khi Hiệu lực, hoặc hồ sơ chuyển Hủy tiếp nhận, mà số dư cọc lớn hơn 0, Kế toán MUST được nhắc lập hoàn cọc (FR-013, FR-014) mỗi CFG-M02-09 mặc định \[1 ngày\] tới khi số dư cọc về 0. Hủy tiếp nhận MUST NOT bị chặn vì tiền cọc chưa hoàn (5.6). Hợp đồng Chờ ký được Trả về nháp thì tiền cọc giữ nguyên. *(Nguồn: 6.3, 6.5, 5.6; Clarification 2026-09-28)*

**Bảng trạng thái giao dịch** *(BR-M11-10, BR-M11-11, BR-M11-14, 6.6)*:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái kế tiếp | Ai thực hiện | Điều kiện (chặn nếu không đạt) | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Ghi phiếu thu tiền mặt (Nộp tiền mặt, Thu cọc tiền mặt) | Đã xác nhận | Kế toán | FR-005 | Cấp số phiếu; cập nhật sổ; cập nhật đặt cọc (FR-006) |
| (chưa có) | Xác nhận dòng sao kê (Nộp chuyển khoản, Thu cọc chuyển khoản) | Đã xác nhận | Kế toán | Bảng trạng thái dòng sao kê | Cập nhật sổ; cập nhật đặt cọc |
| (chưa có) | Bảng chi phí Đã chốt (Thanh toán bảng chi phí) | Đã xác nhận | Hệ thống | FR-012 | Cập nhật số dư |
| (chưa có) | Cấn trừ tiền cọc (cặp) | Đã xác nhận | Kế toán | FR-015 | Sổ cọc giảm, số dư tăng |
| (chưa có) | Lập hoàn tiền / hoàn cọc / điều chỉnh / cặp điều chỉnh chuyển tiền / giao dịch đảo | Chờ duyệt | Kế toán | Có lý do; FR-009a, FR-011, FR-014 | Báo Quản lý viện |
| Chờ duyệt | Duyệt | Đã xác nhận | Quản lý viện | Kiểm tra lại FR-011, FR-014 tại lúc duyệt | Cập nhật số dư hoặc số dư cọc; giao dịch gốc được đánh dấu "đã đảo" (với giao dịch đảo) |
| Chờ duyệt | Từ chối | Từ chối | Quản lý viện | Có lý do | Báo Kế toán |
| Chờ duyệt | Hủy | Đã hủy | Kế toán | Có lý do | — |

Quá hạn duyệt được nhắc theo CFG-M15-05 mặc định \[48 giờ\], CFG-M15-06 mặc định \[96 giờ\] (6.6). Đã xác nhận, Từ chối, Đã hủy là trạng thái cuối. Giao dịch Đã xác nhận chỉ bị bù trừ bằng giao dịch đảo (FR-011), không đổi trạng thái.

#### D. Báo sắp hết tiền và còn nợ

- **FR-016**: Hằng ngày, sau khi Bộ lập lịch của feature 010 đã sinh phí của ngày vừa kết thúc, Bộ lập lịch MUST tính cho mỗi hồ sơ có hợp đồng Hiệu lực và chưa ở trạng thái cuối: **số ngày còn đủ tiền** = (số dư − **tổng chi phí chưa chốt**) ÷ phí lưu trú một ngày, làm tròn xuống. Tổng chi phí chưa chốt là tổng các khoản của mọi bảng chi phí chưa Đã chốt của hồ sơ (bảng thường của kỳ hiện tại và kỳ trước còn Đang mở hoặc Chờ chốt, bảng bổ sung), lấy theo thành phần của Q-136, do feature 010 cung cấp (feature 010 FR-038a). Với hợp đồng tính theo tháng, phí lưu trú một ngày là phí ngày cơ sở của tháng hiện tại (feature 010 FR-009); với hợp đồng tính theo ngày là đơn giá ngày; với hợp đồng bán trú giá tháng là giá tháng ÷ số ngày của tháng hiện tại, với hợp đồng bán trú giá buổi là giá buổi × số buổi có lịch trong 7 ngày tới ÷ 7 (Q-223, Clarification 2026-09-28). *(Nguồn: BR-M11-13, Q-136)*
- **FR-017**: Tình trạng số dư MUST là: "Còn nợ" khi số dư − tổng chi phí chưa chốt < 0; "Sắp hết tiền" khi không còn nợ và số ngày còn đủ tiền < CFG-M11-04 mặc định \[15 ngày\]; "Bình thường" trong trường hợp còn lại. Khi phí lưu trú một ngày bằng 0, chỉ xét "Còn nợ". *(Nguồn: BR-M11-13, CFG-M11-04)*
- **FR-018**: Khi tình trạng chuyển sang "Sắp hết tiền" hoặc "Còn nợ", và mỗi CFG-M11-05 mặc định \[7 ngày\] sau lần báo trước trong khi tình trạng vẫn không phải Bình thường, hệ thống MUST gửi qua feature 009: mọi người đại diện có quan hệ Hiệu lực, mức Trung bình; Kế toán, mức Nhẹ, kèm danh sách. Nội dung gửi người thân chia phần theo loại thông tin (Q-105): phần "chung" là "có thông báo về tài chính của [họ tên], vui lòng liên hệ Kế toán"; phần "chi phí" gồm tình trạng, số dư, tổng chi phí chưa chốt, thông tin nộp tiền. Người đại diện không có quyền xem chi phí chỉ nhận phần "chung" (BR-M13-04). Chiều xấu đi ("Sắp hết tiền" → "Còn nợ") là một lần chuyển mới: báo ngay và đặt lại chu kỳ CFG-M11-05. Chiều tốt lên ("Còn nợ" → "Sắp hết tiền") không báo ngay và giữ chu kỳ cũ. *(Nguồn: BR-M11-13, BR-M13-04, Q-105, Q-211)*
- **FR-019**: Khi hồ sơ ở "Còn nợ" liên tục từ CFG-M11-05 trở lên, Quản lý viện MUST được báo thêm ở mỗi lần nhắc lại. *(Nguồn: BR-M11-13)*
- **FR-020**: Tình trạng "Sắp hết tiền" hay "Còn nợ" MUST NOT chặn bất kỳ lệnh chăm sóc, thuốc, suất ăn, hoạt động nào. Hồ sơ đã kết thúc lưu trú mà số dư còn âm (ngoại lệ ở FR-022) vẫn được báo "còn nợ" cho người đại diện và Kế toán theo FR-018 cho tới khi số dư bằng 0 hoặc Quản lý viện ghi "dừng nhắc" kèm lý do. *(Nguồn: Q-219)*
- **FR-021**: Khi đề nghị mua hộ được gửi (feature 010) mà số dư − tổng chi phí chưa chốt nhỏ hơn số tiền dự kiến, hệ thống MUST cung cấp cho feature 010 dấu "số dư không đủ" cho đề nghị; dấu không chặn. Dấu MUST được tính lại mỗi khi số dư hoặc tổng chi phí chưa chốt của hồ sơ đổi, cho tới khi đề nghị được đồng ý, từ chối hoặc hủy, để người đại diện hay Quản lý viện thấy giá trị mới nhất khi quyết định. *(Nguồn: BR-M11-15, Q-219)*

**Bảng chuyển tình trạng số dư** *(BR-M11-13; chỉ Bộ lập lịch đổi tình trạng, trừ "dừng nhắc")*:

| Tình trạng hiện tại | Sự kiện (lần tính hằng ngày) | Tình trạng kế tiếp | Tác động |
| --- | --- | --- | --- |
| Bình thường | Số ngày còn đủ tiền < CFG-M11-04, không nợ | Sắp hết tiền | Báo ngay theo FR-018; bắt đầu chu kỳ CFG-M11-05 |
| Bình thường, Sắp hết tiền | Số dư − tổng chi phí chưa chốt < 0 | Còn nợ | Báo ngay; đặt lại chu kỳ; bắt đầu đếm cho FR-019 |
| Còn nợ | Hết nợ nhưng số ngày còn đủ tiền < CFG-M11-04 | Sắp hết tiền | Không báo ngay; giữ chu kỳ |
| Sắp hết tiền, Còn nợ | Số ngày còn đủ tiền ≥ CFG-M11-04 và không nợ | Bình thường | Dừng nhắc |
| Sắp hết tiền, Còn nợ | Chưa đổi và đã qua CFG-M11-05 kể từ lần báo trước | Giữ nguyên | Nhắc lại theo FR-018, FR-019 |
| Còn nợ (hồ sơ trạng thái cuối) | Quản lý viện ghi "dừng nhắc", có lý do | Giữ nguyên, mang dấu "dừng nhắc" | Không nhắc nữa (FR-020) |

Hồ sơ không có hợp đồng Hiệu lực và chưa ở trạng thái cuối không được tính.

#### E. Quyết toán

- **FR-022**: Hệ thống MUST cung cấp cho feature 004 điều kiện "số dư và tiền cọc đã quyết toán" của hồ sơ kết thúc lưu trú. Điều kiện Đạt khi: bảng kỳ cuối Đã chốt và đã có giao dịch thanh toán; số dư cọc bằng 0; số dư bằng 0; không còn giao dịch Chờ duyệt. Bảng bổ sung chưa chốt **không** làm điều kiện Chưa đạt, để nhất quán với Q-134 của feature 010; khi bảng đó chốt sau này, giao dịch mới được xử lý theo FR-024 (nếu hồ sơ đã ở trạng thái cuối) hoặc làm điều kiện về Chưa đạt (nếu lệnh Kết thúc lưu trú chưa thực hiện). Ngược lại Chưa đạt kèm lý do. Ngoại lệ do Quản lý viện duyệt ở feature 004 cho kết quả "Đạt (ngoại lệ)". Mỗi lần kết quả đổi, feature 004 MUST được báo. *(Nguồn: 6.8, 5.6, Q-222)*
- **FR-023**: Khi người cao tuổi qua đời, mục "quyết toán số dư và tiền cọc" của danh sách việc sau qua đời MUST có kết quả theo cùng điều kiện FR-022: Hoàn thành khi Đạt; Không áp dụng khi sổ chưa từng có giao dịch nào khác 0 đồng. Kế toán thực hiện mục này. *(Nguồn: 6.8, Q-222)*
- **FR-024**: Giao dịch làm số dư khác 0 sau khi hồ sơ đã ở trạng thái cuối (bảng bổ sung chốt muộn, chuyển khoản tới muộn) MUST được ghi bình thường; Kế toán MUST được nhắc mỗi CFG-M02-09 mặc định \[1 ngày\] tới khi số dư về 0; hồ sơ không mở lại (Q-224, Clarification 2026-09-28; ngoại lệ (3) của BR-M01-05). *(Nguồn: BR-M01-05, 6.8)*

#### F. Xuất dữ liệu

- **FR-025**: Kế toán MUST xuất được ra Excel: (a) sao kê số dư của một hoặc nhiều hồ sơ trong một khoảng, gồm số dư đầu kỳ, từng giao dịch Đã xác nhận (thời điểm, loại, số tiền, phương thức, tham chiếu), số dư cuối kỳ; (b) báo cáo thu chi theo khoảng: tổng nộp theo phương thức, tổng thanh toán bảng chi phí, tổng hoàn tiền, tổng hoàn cọc, tổng điều chỉnh, tổng số dư âm cuối khoảng, số hồ sơ còn nợ; (c) sổ tiền cọc: số dư cọc từng hồ sơ và giao dịch cọc trong khoảng. Quản lý viện xem được các nội dung này. Khoảng xuất MUST NOT dài hơn CFG-M14-01 mặc định \[12 tháng\]. *(Nguồn: 15.9, 18.4, 18.7, UC-88)*
- **FR-026**: Người thân có quyền xem chi phí MUST tải được sao kê (a) của người cao tuổi đó qua cổng (feature 012), trong khoảng không dài hơn CFG-M14-01. *(Nguồn: 15.9)*
- **FR-027**: Mỗi lần xuất MUST được ghi (người xuất, vai trò, thời điểm, loại, khoảng, phạm vi, số dòng). File đã xuất không đổi khi dữ liệu nguồn đổi. *(Nguồn: 19.4, 18.7)*

#### G. Quyền

- **FR-028**: Quyền MUST theo Phụ lục 27 và 19.3: Kế toán thực hiện mọi lệnh của mục B, C (lập), E, F; Quản lý viện duyệt, từ chối, xác nhận hoặc trả lại chốt quỹ ngày (FR-005a), ghi "dừng nhắc", xem toàn bộ; Hành chính chỉ xem số dư, số dư cọc và trạng thái đặt cọc; người thân có quyền xem chi phí xem theo FR-003, FR-026; vai trò khác không có quyền. Kế toán xem hồ sơ người cao tuổi ở mức định danh, hợp đồng, bảng chi phí, số dư, không xem sức khỏe, khoản thuốc chỉ thấy mã vật phẩm (Q-133). *(Nguồn: 19.3, Phụ lục 27 ²⁶, ²⁹, ³²)*
- **FR-029**: File sao kê đã nhập, lần nhập và mọi dòng sao kê (kể cả dòng "Không thuộc người cao tuổi") MUST chỉ Kế toán và Quản lý viện xem được; người thân chỉ thấy giao dịch đã ghi vào sổ của người cao tuổi mình có quyền. Giao dịch, dòng sao kê, lần nhập, chốt quỹ ngày MUST được lưu giữ theo thời hạn của chi phí đã chốt (NFR-07, CFG-M15-03 mặc định \[10 năm sau khi kết thúc lưu trú\]). Mọi lệnh của spec này được ghi nhật ký (19.4); lần người thân xem số dư không bắt buộc ghi nhật ký, vì NFR-08 chỉ đòi với hồ sơ sức khỏe. *(Nguồn: 19.3, 19.4, NFR-07, NFR-08)*

#### I. Thông báo

- **FR-030**: Feature này MUST yêu cầu feature 009 gửi đúng các thông báo ở bảng dưới, theo khuôn chung của feature 009. Thông báo tới người thân chia phần theo loại thông tin (Q-105). *(Nguồn: 17, BR-M13-01, BR-M13-04, Q-211)*

| Sự kiện | Người nhận | Mức | Căn cứ |
| --- | --- | --- | --- |
| Tình trạng chuyển Sắp hết tiền hoặc Còn nợ; nhắc lại mỗi CFG-M11-05 | Mọi người đại diện (phần "chung"; phần "chi phí" nếu có quyền) | Trung bình | FR-018 |
| Như trên | Kế toán (kèm danh sách) | Nhẹ | FR-018 |
| Còn nợ liên tục từ CFG-M11-05 | Quản lý viện | Nhẹ | FR-019 |
| Có dòng sao kê Chưa khớp sau lần nhập | Kế toán | Nhẹ | Bảng trạng thái dòng sao kê |
| Giao dịch cần duyệt được lập; chốt quỹ ngày được lập | Quản lý viện | Nhẹ | FR-013, FR-005a |
| Giao dịch bị từ chối; chốt quỹ bị trả lại; lệnh duyệt bị chặn vì người nhận hoặc giới hạn số tiền | Kế toán | Nhẹ | FR-013, FR-014, FR-005a |
| Hết giờ hành chính chưa lập chốt quỹ | Kế toán | Nhẹ | FR-005a |
| Chốt quỹ chưa được xác nhận sau ngày làm việc kế tiếp | Quản lý viện | Trung bình | FR-005a |
| Hợp đồng bị hủy hoặc Hủy tiếp nhận khi còn tiền cọc; nhắc mỗi CFG-M02-09 | Kế toán | Nhẹ | FR-015a |
| Số dư khác 0 sau khi hồ sơ ở trạng thái cuối; nhắc mỗi CFG-M02-09 | Kế toán | Nhẹ | FR-024 |
| Dòng sao kê khớp hồ sơ đã ở trạng thái cuối được xác nhận | Kế toán | Nhẹ | User Story 3 kịch bản 7 |

#### H. Giao tiếp với feature khác

| Hướng | Feature | Nội dung | Dữ liệu tối thiểu |
| --- | --- | --- | --- |
| Nhận | 004 | Hợp đồng chuyển Chờ ký, Trả về nháp, Hiệu lực, Đã hủy; Hủy tiếp nhận; khoản cần đặt cọc (hoặc "không yêu cầu") của hợp đồng Chờ ký hoặc hiệu lực, kể cả khi phụ lục đổi khoản cọc; hồ sơ kết thúc lưu trú lập, hủy; qua đời; danh sách việc sau qua đời; ngoại lệ quyết toán được duyệt | Hồ sơ, hợp đồng, số tiền cọc, trạng thái |
| Gửi | 004 | Trạng thái đặt cọc (FR-006); điều kiện quyết toán (FR-022); kết quả mục danh sách việc sau qua đời (FR-023) | Hồ sơ, trạng thái, lý do, giao dịch nguồn |
| Nhận | 010 | Bảng chi phí Đã chốt (feature 010 bảng giao tiếp "Gửi 017"); tổng chi phí chưa chốt của hồ sơ (feature 010 FR-038a); phí ngày cơ sở (feature 010 FR-009); đề nghị mua hộ được gửi, được quyết định, bị hủy | Mã bảng (duy nhất), loại bảng, kỳ, hồ sơ, tổng có dấu; tổng chưa chốt; đề nghị, số tiền dự kiến |
| Gửi | 010 | Dấu "số dư không đủ" cho đề nghị mua hộ, tính lại tới khi đề nghị được quyết định (FR-021) | Đề nghị, dấu |
| Gửi | 012 | Số dư, giao dịch, thông tin nộp tiền, sao kê (FR-003, FR-026) | Theo FR-003 |
| Gửi | 009 | Các thông báo ở bảng FR-030 | Nguồn, mức, nhóm người nhận, loại thông tin ("chung", "chi phí") |
| Gửi | 016 | Số hồ sơ Sắp hết tiền, Còn nợ cho dashboard (18.5) | Hồ sơ, tình trạng, số dư |

### Key Entities *(include if feature involves data)*

- **Sổ số dư (SO_DU)** – nhóm 2 dẫn xuất: hồ sơ người cao tuổi, mã nộp tiền, số dư, tình trạng số dư, ngày mở.
- **Sổ tiền cọc** – nhóm 2 dẫn xuất: hồ sơ, số dư cọc.
- **Giao dịch số dư / giao dịch tiền cọc (GIAO_DICH_SO_DU)** – nhóm 2 → 3: các trường ở FR-010; số phiếu thu hoặc số phiếu chi với giao dịch tiền mặt (FR-005); giao dịch liên kết với cặp cấn trừ tiền cọc và cặp điều chỉnh chuyển tiền (FR-009a, FR-015).
- **Lần nhập sao kê** – nhóm 3: người nhập, thời điểm, tên file, số dòng nhận, bỏ qua, từ chối.
- **Dòng sao kê (DONG_SAO_KE)** – nội dung nhóm 3, trạng thái nhóm 2: mã giao dịch ngân hàng, thời điểm, số tiền, nội dung, hồ sơ được khớp, lý do, giao dịch sinh ra.
- **Lịch sử tình trạng số dư** – nhóm 3: hồ sơ, ngày, số ngày còn đủ tiền, tình trạng, lần báo.
- **Lần xuất** – nhóm 3: theo FR-027.
- **Chốt quỹ ngày** – nhóm 2 → 3: các trường ở FR-005a, người lập, người xác nhận, trạng thái.
- Dùng từ feature khác: **Hợp đồng**, **Hồ sơ kết thúc lưu trú**, **Danh sách việc sau qua đời** (004); **Bảng chi phí (BANG_CHI_PHI)**, **Đề nghị mua hộ** (010); **Quan hệ người thân** (012).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Với bộ kiểm thử 3 tháng của 50 hồ sơ, 100% số dư và số dư cọc bằng tổng giao dịch Đã xác nhận tính tay; 0 giao dịch Đã xác nhận bị sửa, xóa.
- **SC-002**: 100% bảng chi phí Đã chốt có đúng một giao dịch thanh toán với số tiền bằng tổng bảng; 0 giao dịch trùng khi sự kiện chốt được gửi lặp.
- **SC-003**: Nhập cùng một file sao kê 3 lần tạo 0 giao dịch trùng. 100% dòng có đúng một mã nộp tiền hợp lệ được khớp tự động.
- **SC-004**: Với bộ kiểm thử 200 dòng sao kê, 100% dòng có nội dung chứa đúng một mã nộp tiền hợp lệ được khớp tự động, 0 dòng không có mã hoặc nhiều mã bị khớp tự động. Trong đợt thử, với file 200 dòng có ít nhất 180 dòng ghi đúng mã, Kế toán đối soát xong trong không quá 30 phút. Đây là mục tiêu đo trong đợt thử, không phải tham số.
- **SC-005**: 100% hồ sơ dưới ngưỡng CFG-M11-04 hoặc còn nợ được báo trong lần chạy đầu tiên của Bộ lập lịch sau khi chuyển tình trạng; 0 lần báo lặp sớm hơn CFG-M11-05.
- **SC-006**: 0 lệnh chăm sóc, thuốc, suất ăn bị chặn vì số dư.
- **SC-007**: 0 hoàn tiền, điều chỉnh, giao dịch đảo tác động số dư khi chưa được Quản lý viện duyệt; 0 hoàn tiền vượt số dư tại lúc duyệt.
- **SC-008**: 100% lệnh Kết thúc lưu trú thành công có điều kiện quyết toán Đạt hoặc Đạt (ngoại lệ).
- **SC-009**: File sao kê và báo cáo thu chi khớp 100% với giao dịch Đã xác nhận trong khoảng (số dư đầu kỳ + tổng giao dịch = số dư cuối kỳ).
- **SC-010**: 0 lần số dư hoặc thông tin nộp tiền hiển thị cho người thân không có quyền xem chi phí, hoặc cho Nhân viên chăm sóc, Điều dưỡng, Bác sĩ.
- **SC-011**: 100% ngày có giao dịch tiền mặt trong đợt thử có chốt quỹ Đã xác nhận trong vòng 2 ngày làm việc; 0 khoảng trống hay trùng lặp trong dãy số phiếu thu.
- **SC-012**: Bộ lập lịch tính số ngày còn đủ tiền và tình trạng số dư cho 300 hồ sơ xong trong không quá 5 phút; chạy lại cùng ngày không gửi thông báo trùng (NFR-04).
- **SC-013**: 0 dòng sao kê sinh nhiều hơn một giao dịch; 100% cặp điều chỉnh chuyển tiền và cặp cấn trừ tiền cọc có hai giao dịch cùng số tiền, ngược dấu, cùng trạng thái.

## Assumptions

- Số feature `017` là số kế tiếp sau spec 016; tài liệu nguồn (1.6, 24.2 Q-211) ghi "số dư và thu chi chưa có spec".
- Sổ được mở khi hợp đồng đầu tiên chuyển Chờ ký, để thu cọc lúc gia đình đến ký (Clarification 2026-09-28; Q-225, 15.9 đã sửa). Chuyển khoản tới trước khi có sổ nằm ở danh sách chưa khớp.
- Mã QR không kèm số tiền, vì gia đình nộp số tiền tùy ý; mã nộp tiền đủ để khớp.
- Kế toán không cần duyệt từng phiếu cho nộp tiền và cấn trừ cọc (bảng 15.9 không nêu người duyệt); tiền mặt được kiểm soát bằng chốt quỹ ngày có Quản lý viện xác nhận (FR-005a).
- Số ngày còn đủ tiền làm tròn xuống, để báo sớm hơn thay vì muộn hơn.
- Người nhận thông báo "sắp hết tiền" là mọi người đại diện có quan hệ Hiệu lực (BR-M11-13 ghi "người đại diện", không giới hạn một người).
- Thời hạn lưu giữ giao dịch và sao kê theo NFR-07, CFG-M15-03 (như chi phí đã chốt).
- Không đề xuất tham số mới ngoài CFG-M11-04, CFG-M11-05 đã có; nhắc quyết toán muộn dùng lại CFG-M02-09.
- "Excel/CSV", "VietQR", "mã giao dịch ngân hàng" là định dạng và chuẩn nghiệp vụ đã có ở mục 23 và 15.9, không phải lựa chọn công nghệ (constitution II).
- Còn nợ → Sắp hết tiền không báo ngay, vì tình trạng đã tốt lên và gia đình vừa được báo trước đó; chu kỳ nhắc giữ nguyên (FR-018).
- Hoàn tiền giữa kỳ bị giới hạn bởi tổng chi phí chưa chốt để không tạo nợ mới (FR-014). Nguồn không quy định; đây là cách hiểu thận trọng của "số tiền hoàn không vượt số dư hiện có" ở BR-M11-14.

## Điểm cần báo lại về tài liệu nguồn

**Trạng thái (2026-09-28):** theo yêu cầu của người dùng, các điểm 1 → 4 và 6 → 10 đã được đưa vào docs/nghiep-vu.md (2.4, 6.5, 6.8, BR-M11-12 → 14, BR-M11-16 mới, 15.9, 24.2 Q-223 → Q-230, UC-93, Phụ lục 27 dòng "Chốt quỹ ngày" và chú thích ³², DBR-28, DBR-35) và docs/luong-nghiep-vu.md (BF-01 bước 11, BF-16); hai file dẫn xuất đã cập nhật. Điểm 5 đã xử lý ở spec 016 (FR-010, FR-020, dòng "Nhận 017"). Điểm 11 đã xử lý ở spec 004, 010.

1. **Q-223** – phí lưu trú một ngày của hợp đồng bán trú khi tính số ngày còn đủ tiền (FR-016): người dùng chốt theo Mặc định ở Clarification 2026-09-28; cần chuyển dòng Q-223 từ 24.1 sang 24.2.
2. **Q-224** – số dư khác 0 sau khi hồ sơ đã ở trạng thái cuối (FR-024, User Story 7 kịch bản 6): người dùng chốt theo Mặc định ở Clarification 2026-09-28; cần chuyển dòng Q-224 từ 24.1 sang 24.2.
3. **Trạng thái "Đã hủy" của giao dịch cần duyệt (FR-013)** không có trong câu "trạng thái Chờ duyệt / Đã xác nhận / Từ chối" ở 15.9; spec lấy theo vòng đời yêu cầu phê duyệt chung 6.6. Đề nghị bổ sung vào 15.9.
4. **Điều kiện cấn trừ cọc (FR-015)** – 15.9 chỉ nêu cấn trừ ở quyết toán; spec giới hạn chỉ khi có hồ sơ kết thúc hoặc danh sách việc sau qua đời và bảng kỳ cuối đã chốt. Đề nghị ghi rõ ở 6.5 hoặc 15.9.
5. **Mục 18.5 dashboard** đã có chỉ tiêu "số người số dư thấp hoặc còn nợ"; feature 016 cần bổ sung nguồn dữ liệu từ spec này.
6. **Thời điểm mở sổ (Clarification 2026-09-28)** – 15.9 ghi sổ số dư "mở khi hợp đồng đầu tiên chuyển Hiệu lực"; người dùng chốt mở khi hợp đồng đầu tiên chuyển **Chờ ký** để thu cọc lúc ký, kèm hoàn cọc khi hợp đồng bị hủy trước khi Hiệu lực (FR-001, FR-015a). Cần sửa 15.9, 6.5 và BF-01 bước 11 (thu cọc có thể trước bước 10), BF-16 bước 1, và thêm mã Q mới ở 24.2.
7. **Chốt quỹ ngày và số phiếu thu (Clarification 2026-09-28)** – khái niệm mới, chưa có trong 15.9, 2.4, Phụ lục 26 → 28. Cần bổ sung: mô tả ở 15.9; thuật ngữ "Chốt quỹ ngày" ở 2.4; UC mới (Lập, xác nhận chốt quỹ ngày; Kế toán, Quản lý viện); dòng quyền ở Phụ lục 27 (KT T, QL D); DBR (mỗi ngày tối đa một chốt quỹ Đã lập hoặc Đã xác nhận; số phiếu thu liên tục, duy nhất); mã Q mới ở 24.2; BF-16 thêm bước chốt quỹ.
8. **Cặp điều chỉnh chuyển tiền (FR-009a, checklist CHK028, CHK029)** – để giữ DBR-29 ("mỗi dòng sao kê sinh tối đa một giao dịch"), chuyển khoản gộp nhiều khoản được xử lý bằng cặp điều chỉnh có duyệt, không tách dòng. Đề nghị ghi vào 15.9 (bảng loại giao dịch) và cân nhắc bổ sung vào DBR-28.
9. **Bảng chuyển tình trạng số dư và nội dung chia phần của thông báo (FR-018, checklist CHK006, CHK007, CHK021)** – BR-M11-13 không nêu chiều "Còn nợ → Sắp hết tiền", cũng không nêu người đại diện không có quyền xem chi phí nhận gì. Spec chọn: không báo ngay khi tình trạng tốt lên; người không có quyền xem chi phí chỉ nhận phần "chung" (Q-105). Đề nghị ghi rõ ở BR-M11-13.
10. **Giới hạn hoàn tiền giữa kỳ (FR-014, checklist CHK030)** – spec giới hạn bằng số dư − tổng chi phí chưa chốt khi hồ sơ chưa ở trạng thái cuối. Đề nghị ghi rõ ở BR-M11-14.
11. **Đồng bộ spec 004, 010 (checklist CHK009 → CHK019)** – ngày 2026-09-28 đã sửa spec 004 (đặt cọc dẫn xuất từ spec này, điều kiện quyết toán, sự kiện hợp đồng) và spec 010 (FR-038a, người xuất file là Kế toán, ranh giới FR-042, bảng giao tiếp "Gửi 017", dấu "số dư không đủ").
