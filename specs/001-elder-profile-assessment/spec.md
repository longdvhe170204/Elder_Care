# Feature Specification: Hồ sơ và đánh giá người cao tuổi

**Feature Branch**: `001-elder-profile-assessment`

**Created**: 2026-09-25

**Status**: Draft

**Input**: User description: "Quản lý hồ sơ người cao tuổi của viện dưỡng lão theo docs/business/nghiep-vu.md Module 01 (mục 5): hồ sơ cá nhân; bản đồng ý chia sẻ dữ liệu; hồ sơ sức khỏe ban đầu với dị ứng, bệnh nền lưu thành từng mục chỉ loại trừ chứ không xóa; đánh giá đầu vào và đánh giá lại bằng thang điểm, tự quy đổi ra mức chăm sóc đề xuất và cờ nguy cơ *(sửa 2026-10-06, Q-280, Q-281: bác sĩ đánh giá bằng nhận xét và chọn mức chăm sóc; không thang điểm, không quy đổi, không cờ nguy cơ)*; trạng thái người cao tuổi chỉ thay đổi qua các lệnh nghiệp vụ theo bảng 5.5 với điều kiện ở 5.6. Loại hình lưu trú và mức chăm sóc là hai thuộc tính độc lập."

## Clarifications

### Session 2026-09-25

- Q: Khi một người đã Kết thúc lưu trú hoặc Hủy tiếp nhận quay lại đăng ký lần nữa, hệ thống tạo hồ sơ mới hay dùng lại hồ sơ cũ? → A: Tạo hồ sơ mới liên kết với hồ sơ cũ; CCCD chỉ phải duy nhất trong các hồ sơ chưa ở trạng thái cuối (đề xuất Q-12).
- Q: Có bắt buộc phải có bản đồng ý chia sẻ dữ liệu đang Hiệu lực thì mới được Hoàn tất tiếp nhận không? → A: Không chặn; hệ thống cảnh báo khi Hoàn tất tiếp nhận và nhắc hành chính định kỳ (CFG-M01-06 đề xuất, mặc định 1 ngày) cho tới khi có bản đồng ý Hiệu lực (Q-03).
- Q: Khi chấp nhận một lần đánh giá, bác sĩ có được bỏ cờ nguy cơ hệ thống đề xuất, hoặc gắn thêm cờ không có trong đề xuất, không? → A: Được gắn thêm cờ (bắt buộc lý do); không được bỏ cờ đề xuất (đề xuất Q-13). *(Sửa ở Session 2026-09-30: được bỏ cờ đề xuất kèm lý do, Q-250.)*
- Q: Sau khi hồ sơ đã Kết thúc lưu trú hoặc Qua đời, người đồng ý (hoặc người đại diện hợp pháp) có được yêu cầu rút lại bản đồng ý chia sẻ dữ liệu không? → A: Có; "Rút lại đồng ý" là ngoại lệ duy nhất cho phép trên hồ sơ trạng thái cuối, do hành chính thực hiện, bắt buộc lý do và bằng chứng (Q-03).
- Q: Quản lý viện có phải duyệt trước khi hành chính hoặc trưởng tầng thực hiện "Cho tạm vắng" hay "Ghi nhận trở về" không? → A: Không; hai lệnh thực hiện trực tiếp. Quyền D của Quản lý viện ở dòng "Tạm vắng, trở về" (4.4) chỉ áp dụng cho quyết định giữ hay giải phóng giường khi vắng quá ngưỡng (BR-M02-07, feature 004).
- Q: Khi hồ sơ đã ở trạng thái cuối mà phát hiện một bản ghi đã xác nhận bị sai, có được tạo bản đính chính không? → A: Được, nhưng mỗi bản đính chính phải qua yêu cầu phê duyệt loại "Đính chính" và chỉ có hiệu lực khi Quản lý viện duyệt.
- Q: Khi đang có một yêu cầu đổi mức chăm sóc chưa áp dụng mà bác sĩ xác nhận một lần đánh giá lại mới, yêu cầu cũ được xử lý thế nào? → A: Hệ thống chuyển yêu cầu cũ sang trạng thái kết thúc mới "Được thay thế", trỏ tới lần đánh giá và yêu cầu mới; đề xuất bổ sung trạng thái này vào vòng đời chung của feature 000.
- Q: Ai được thực hiện lệnh "Ghi nhận qua đời": chỉ Bác sĩ, hay cả Hành chính như Permission Matrix đang ghi? → A: Bác sĩ khi mất tại viện (Đang lưu trú); khi mất ngoài viện (Tạm vắng, Hoạt động bên ngoài, Điều trị tại bệnh viện), Hành chính cũng được ghi nhận, bắt buộc kèm giấy tờ bằng chứng.
- Q: Dị ứng nên được ghi bằng cách chọn từ danh mục hay gõ văn bản tự do? → A: Chọn từ danh mục dị nguyên là chính; được ghi loại "khác" bằng văn bản, nhưng mục đó mang dấu "không kiểm tra tự động" và người ghi được cảnh báo.
- Q: Nếu hồ sơ nằm danh sách chờ lâu, đánh giá đầu vào đã xác nhận có còn dùng được để Hoàn tất tiếp nhận không? → A: Không, nếu lần đánh giá Đã xác nhận gần nhất đã quá CFG-M01-02 (mặc định 90 ngày) thì chặn Hoàn tất tiếp nhận; phải có một lần đánh giá lại trước.

### Cập nhật 2026-09-26 (đồng bộ với spec 009)

Spec 009 đã chốt khi clarify: chỉ thông báo mức Khẩn cấp được gửi người thân trong giờ yên tĩnh CFG-M13-02 (Q-97), và mọi thông báo phải được module nguồn nêu mức (Q-108, spec 009 FR-043a). Thông báo cho người liên hệ chính khi Chuyển viện và khi Ghi nhận qua đời vì vậy được nêu rõ là mức Khẩn cấp (bảng trạng thái, User Story 4 kịch bản 11), kéo theo xác nhận đã nhận và yêu cầu gọi điện nếu chưa xác nhận (spec 009 FR-026 → FR-030a). Các thông báo khác của spec này (FR-036a) chưa được nêu mức; spec 009 tạm gửi ở mức Nhẹ kèm dấu "thiếu mức" (spec 009 Điểm báo lại 14).

### Cập nhật 2026-09-26 (đồng bộ với spec 012)

Spec 012 đã chốt khi clarify: điều kiện "người đón" của lệnh Cho tạm vắng là người thuộc danh sách được phép đón tại thời điểm đón hoặc có ngoại lệ đón Hiệu lực (BR-M10-03, feature 012 FR-021, FR-024); dòng "Đang lưu trú → Cho tạm vắng" của bảng trạng thái được sửa cho khớp. *(Không còn áp dụng, Q-281: không còn cờ nguy cơ.)* Cờ nguy cơ đi lạc do spec này quản lý làm dấu "được tự về" của người bán trú mất tác dụng ngay khi được gắn (feature 012 FR-021, Q-125); spec này không đổi hành vi. Dòng "Đang tiếp nhận → Hoàn tất tiếp nhận" thêm điều kiện đủ DBR-02 do feature 012 cung cấp (012 FR-003).

### Cập nhật 2026-09-27 (đồng bộ với spec 011)

Spec 011 cần thêm hai sự kiện từ spec này: mục dị ứng chuyển Đã loại trừ (để tính lại suất đặc biệt, đối chiếu lại đồ ăn gia đình, đóng nguồn cảnh báo xung đột), và mục dị ứng loại "khác" mới (để đưa người cao tuổi vào danh sách cần đối chiếu khi phục vụ). FR-020 được bổ sung tương ứng.

### Cập nhật 2026-09-27 (đồng bộ với spec 013)

Spec 013 và tài liệu nguồn (BR-M12-03, BR-M12-06, Q-148, Q-155) đã chốt:
- *(Không còn áp dụng phần cờ đi lạc, 2026-10-06, Q-281.)* Tiền mặt, trang sức chỉ được giao cho người cao tuổi tự giữ khi không có cờ nguy cơ đi lạc; khi cờ đi lạc được gắn trong lúc người cao tuổi đang tự giữ đồ đó, feature 013 báo thu lại. Spec này cung cấp cho feature 013 cờ đi lạc hiện hành và sự kiện gắn, gỡ cờ (FR-038 được bổ sung).
- Điều kiện đồ gửi của lệnh Kết thúc lưu trú (bảng trạng thái) là "không còn đồ gửi ở Đang giữ, Đang được sử dụng hoặc Hư hỏng"; đồ Thất lạc không tính ở điều kiện này nhưng sự cố của nó thuộc điều kiện sự cố mở.
- Thao tác trên đồ gửi sau khi hồ sơ ở trạng thái cuối (thu lại, trả, báo thất lạc, xử lý đồ không người nhận) thuộc ngoại lệ (3) của BR-M01-05, không sửa hồ sơ người cao tuổi.

### Cập nhật 2026-09-27 (đồng bộ với spec 014)

Spec 014 và tài liệu nguồn (5.6, 8.9, BR-M04-04, Q-166, Q-170, Q-171) đã chốt:
- Chuyển Đang lưu trú → Hoạt động bên ngoài do trưởng đoàn, hoặc trưởng tầng thay, điểm danh rời viện; chỉ từ Đang lưu trú. Công việc Chưa đến hạn trong khoảng đi bị **Hủy** theo BR-M04-04 (không "tạm dừng"), sinh lại khi trở về (bảng trạng thái, hai dòng chuyến đi được sửa).
- Chuyển Hoạt động bên ngoài → Đang lưu trú còn xảy ra khi trưởng tầng Hủy ghi nhận bản ghi rời viện của người bị ghi đi nhầm (feature 014 FR-031a); căn cứ ghi "đính chính điểm danh".
- Hoạt động bên ngoài không chuyển sang Tạm vắng; người thân không đón thẳng từ điểm đến (Q-166). Người thiếu khi về giữ Hoạt động bên ngoài tới lệnh phù hợp (không đổi).

### Cập nhật 2026-09-28 (đồng bộ với góp ý nghiệp vụ Q-213, Q-216, Q-217)

Tài liệu nguồn (5.2, 19.3, UC-82, Phụ lục 27 dòng "Nguyện vọng cuối đời", DBR-34, BF-01 bước 3a) đã chốt: "thông tin chăm sóc cuối đời" trở thành **phiếu nguyện vọng cuối đời** có phiên bản, khảo sát khi tiếp nhận. Spec này thêm mục Phạm vi 7, User Story 7, mục C1 (FR-023a → FR-023f), dòng phân nhóm dữ liệu, Key Entities, SC-011; FR-017 và FR-023 được sửa. Việc dùng nguyện vọng khi nguy kịch thuộc feature 007 (BR-M05-15, 16).

### Cập nhật 2026-09-28 (đồng bộ với spec 003, rà chéo)

Spec 003 và tài liệu nguồn (7.3, Q-50) đã chốt: "Hoàn tất tiếp nhận" bị chặn khi giường đặt trước chưa về Trống (ví dụ còn Chờ sẵn sàng, trước đây "Chờ vệ sinh", Q-282), trừ khi người thực hiện chuyển phân bổ sang một giường Trống khác ngay trong lệnh. Điều kiện được thêm vào dòng "Đang tiếp nhận → Hoàn tất tiếp nhận" của bảng trạng thái người cao tuổi.

### Cập nhật 2026-09-27 (đồng bộ với spec 016)

Spec 016 (báo cáo và dashboard) lấy tình trạng "quá hạn" của yêu cầu đánh giá lại từ feature này, không tự tính (spec 016 FR-002, FR-044). FR-041 được bổ sung dấu "quá hạn" trên yêu cầu và việc cung cấp cho feature 016.

### Session 2026-09-30 (rà soát vận hành)

- Q: Bác sĩ có được bỏ cờ nguy cơ mà thang điểm đề xuất không? → A: Được, bắt buộc lý do; kết quả đánh giá lưu cả cờ đề xuất, cờ được chấp nhận và lý do; lịch sử đánh giá hiện cờ đã bị bỏ. Thay quyết định trước đó (Q-13) (Q-250).

### Session 2026-10-01 (/speckit-clarify, checklist cross-feature)

- Q: Người cao tuổi đang có cờ nguy cơ, lần đánh giá lại vẫn đề xuất cờ đó nhưng bác sĩ bỏ cờ đề xuất thì cờ đang gắn được giữ hay gỡ? → A: Gỡ. Bác sĩ bỏ cờ đề xuất trong một lần đánh giá lại Đã xác nhận thì cờ đang gắn cùng loại được gỡ, bắt buộc lý do; cờ lưu lần đánh giá gỡ và lý do (Q-255).

### Session 2026-10-01 (đánh giá checklist business-rules)

Các điểm dưới đây do nhóm phân tích quyết định khi đánh giá checklist business-rules; không đổi quy tắc ở tài liệu nguồn, trừ các đề xuất nêu ở mục "Điểm cần báo lại".

- Q: Lần đánh giá Nháp bị bỏ dở xử lý thế nào? → A: Mỗi người cao tuổi có tối đa một lần đánh giá Nháp; người tạo hoặc bác sĩ "Hủy nháp" kèm lý do, bản nháp chuyển Đã hủy và vẫn lưu (CHK004).
- Q: Hồ sơ sức khỏe ban đầu chưa xác nhận có chặn Hoàn tất tiếp nhận không? → A: Không; 5.6 không nêu điều kiện này (CHK003).
- Q: Ai xem được giấy tờ định danh và ảnh? → A: Mọi vai trò được xem hồ sơ theo FR-007 thấy đủ thông tin cá nhân; tài liệu nguồn không có quyền riêng cho hai trường này (CHK007).
- Q: Kiểm tra dị ứng không chạy được thì mục dị ứng có được lưu không? → A: Có; mục vẫn lưu và mang dấu "chờ kiểm tra" tới khi kiểm tra chạy xong (CHK036).
- Q: Bảng quy đổi đổi khi đang có lần đánh giá Nháp thì dùng bảng nào? → A: Bảng hiệu lực tại thời điểm bác sĩ chấp nhận (CHK039).

### Cập nhật 2026-10-06 (đồng bộ Q-280 → Q-283, quyết định của chủ dự án)

Tài liệu nguồn (5.3, BR-M01-09, BR-M01-10, DBR-05, Phụ lục 25 → 27) đã chốt:

- **Q-280**: không còn thang điểm (Barthel, MMSE, Morse, Braden), bảng quy đổi CFG-M01-05, mức đề xuất hay hoạt động mẫu. Bác sĩ xem hồ sơ sức khỏe ban đầu và chỉ số đo, ghi **nhận xét đánh giá** (kèm phần **lưu ý chăm sóc**) và chọn mức chăm sóc. Kế hoạch chăm sóc do điều dưỡng lập tay (feature 005). Q-02 không còn áp dụng.
- **Q-281**: không còn cờ nguy cơ; phần lưu ý chăm sóc của lần đánh giá Đã xác nhận gần nhất thay cờ trên checklist và thẻ thông tin khẩn cấp. Các quy tắc dựa vào cờ đi lạc ở feature 012, 013, 014 bỏ điều kiện này, không có cơ chế thay thế. Q-13, Q-250, Q-255 không còn áp dụng.
- **Q-282, Q-283**: bỏ vai trò Nhân viên vệ sinh và Dinh dưỡng viên; Bác sĩ xem đầy đủ hồ sơ sức khỏe.
- **Q-291** *(bổ sung 2026-10-06, góp ý rà soát)*: mức chăm sóc chỉ còn ba mức theo cường độ (Chăm sóc cơ bản, Chăm sóc thường xuyên, Chăm sóc đặc biệt). Hỗ trợ vận động, Theo dõi sức khỏe thường xuyên, Phục hồi sau tai biến, Phục hồi chức năng khác chuyển thành **nhu cầu chăm sóc**, chọn nhiều giá trị khi đánh giá. User Story 3 kịch bản 5, FR-025, FR-029, Key Entities và Assumptions đã sửa.

Các Clarification, cập nhật và mục "Điểm cần báo lại" trước đây nói về thang điểm, quy đổi hay cờ nguy cơ chỉ còn là lịch sử. User Story 3, 6, mục D, bảng trạng thái, Key Entities, SC-004, SC-005, SC-012 và Assumptions đã viết lại theo quyết định mới.

### Cập nhật 2026-10-06, lượt 2 (đồng bộ Q-292 → Q-299, quyết định của chủ dự án sau rà soát tài liệu nguồn)

- **Q-296**: khi đánh giá lại được xác nhận, feature 005 chỉ được kích hoạt tạo yêu cầu xem xét kế hoạch chăm sóc khi mức chăm sóc hoặc tập nhu cầu chăm sóc khác lần đánh giá Đã xác nhận trước đó (kể cả chỉ đổi nhu cầu). Yêu cầu đánh giá lại đang mở vẫn được đóng như cũ. Sửa FR-029, FR-043.

### Cập nhật 2026-10-06, lượt 3 (đồng bộ Q-300, quyết định của chủ dự án)

- **Q-300**: thực tế gia đình chọn mức chăm sóc cùng loại lưu trú và dịch vụ khi lập hợp đồng (feature 004). Bác sĩ vẫn đánh giá đầu vào; mức hiện hành vẫn lấy từ lần đánh giá Đã xác nhận (FR-033 không đổi cách đặt mức). Hoàn tất tiếp nhận thêm điều kiện mức của hợp đồng Hiệu lực trùng mức hiện hành (bảng trạng thái, dòng "Đang tiếp nhận → Hoàn tất tiếp nhận"); FR-033 thêm sự kiện gửi feature 004 khi xác nhận đánh giá đầu vào.

### Cập nhật 2026-10-06, lượt 4 (đồng bộ Q-306, Q-307: hệ thống không tự sinh, tự tạo, tự gợi ý)

- **Q-307**: bỏ thực thể "Yêu cầu đánh giá lại". Hệ thống chỉ báo bác sĩ cần đánh giá lại (đến hạn định kỳ, sau sự cố, trở về từ bệnh viện, đính chính khác mức); "cần đánh giá lại" và "quá hạn đánh giá lại" là trạng thái tính từ thời điểm báo và lần đánh giá Đã xác nhận, không phải bản ghi. Bác sĩ, điều dưỡng muốn đánh giá lại thì tạo lần đánh giá trực tiếp. Viết lại User Story 6, mục E (FR-039 → FR-043), FR-032, bảng trạng thái, Key Entities, SC-007.
- **Q-306**: đánh giá lại ra mức khác không còn tự tạo yêu cầu thay đổi lưu trú; Hành chính được báo và lập yêu cầu, chưa lập thì được nhắc theo CFG-M03-02 (FR-036, FR-036a). Xem xét kế hoạch chăm sóc chỉ là thông báo cho điều dưỡng phụ trách (FR-029, FR-031, FR-043). Tác động khi chuyển trạng thái không còn "sinh" công việc: Người phụ trách ca được báo để lập (bảng trạng thái).

### Cập nhật 2026-10-10, lượt 2 (đồng bộ Q-311, Q-318 → Q-320 và câu chữ sau rà soát điểm báo lại của plan)

- **Q-311**: thêm FR-054 (mức thông báo, người nhận "bác sĩ").
- **Q-318**: Từ chối yêu cầu đổi mức không dừng nhắc Hành chính. Sửa FR-036.
- **Q-319**: chỉ liên kết hồ sơ cũ khi trùng CCCD. Sửa FR-005.
- **Q-320**: Tạo hồ sơ không bắt lý do. Sửa FR-046.
- Bảng trạng thái: thêm điều kiện "không sớm hơn ngày bắt đầu hợp đồng" cho Hoàn tất tiếp nhận; "đã bàn giao người cao tuổi", "số dư và tiền cọc đã quyết toán" cho Kết thúc lưu trú (5.6).
- Bỏ dòng "Yêu cầu đánh giá lại" ở bảng nhóm dữ liệu và câu Assumptions tương ứng (Q-307).

### Cập nhật 2026-10-11 (đồng bộ Q-337 → Q-345 sau đối chiếu tài liệu gốc DOCX)

Chủ dự án chốt Q-337 → Q-345 (docs/business/nghiep-vu.md 24.2) sau khi đối chiếu với tài liệu gốc DOCX "nghiep-vu-quan-ly-vien-duong-lao.docx" (lưu ngoài kho mã từ 2026-10-15; bản cũ ở commit a2ff357); các Q đã chốt trước được giữ. Thay đổi ở spec này:

- **Q-342**: lệnh Ghi nhận qua đời ngoài viện do Hành chính thực hiện kèm giấy tờ do cơ sở y tế hoặc cơ quan có thẩm quyền cấp; giấy tờ này là xác nhận chuyên môn về tử vong theo tài liệu gốc 30.2 (FR-047b).
- **Q-343**: điều kiện DBR-02 của Hoàn tất tiếp nhận nay có trong 5.6 và 004 FR-004 (f); bảng trạng thái dẫn chiếu thêm 004 FR-004 (f).

## Phạm vi

**Trong phạm vi** (Module 01, mục 5; UC-01 → UC-08):

1. Hồ sơ cá nhân người cao tuổi: tạo, cập nhật qua lệnh, tra cứu theo quyền (5.1, UC-01, UC-02, DBR-01).
2. Bản đồng ý xử lý và chia sẻ dữ liệu: ghi nhận, rút lại (5.1, UC-03, BR-M01-08, DBR-03).
3. Hồ sơ sức khỏe ban đầu; dị ứng, bệnh nền, tiền sử bệnh lưu thành từng mục, chỉ loại trừ, không xóa (5.2, UC-04, BR-M01-07, DBR-04).
4. Đánh giá đầu vào và đánh giá lại: bác sĩ ghi nhận xét đánh giá, lưu ý chăm sóc và chọn mức chăm sóc; nhắc bác sĩ đánh giá lại (5.3, 5.4, UC-05 → UC-07, BR-M01-02, 03, 09, 10, DBR-05; **sửa 2026-10-06, Q-280, Q-281, Q-307**).
5. Vòng đời trạng thái người cao tuổi: tập trạng thái, chuyển hợp lệ, lệnh nghiệp vụ, điều kiện chặn, lịch sử (5.5, 5.6, BR-M01-01, 04, 05, 06).
6. Tính độc lập giữa loại hình lưu trú và mức chăm sóc (1.3, mục 4).
7. Phiếu nguyện vọng cuối đời: khảo sát khi tiếp nhận, phiên bản, người ký, nhắc khi chưa có (5.2, UC-82, DBR-34, Q-213, Q-216, Q-217).

**Ngoài phạm vi** (spec này chỉ **dùng** kết quả hoặc **kích hoạt** feature sở hữu):

- Quy tắc dùng chung (phân loại dữ liệu, đính chính, yêu cầu phê duyệt, nhật ký, tham số): feature 000 — spec này kế thừa, không lặp lại.
- Tài khoản, phân quyền, phạm vi dữ liệu (feature 002, BR-M15-01).
- Giường, phân bổ, giữ chỗ (feature 003); hợp đồng, đặt cọc, danh sách chờ, yêu cầu thay đổi lưu trú, chính sách phí vắng, nội dung chi tiết của tạm vắng/kết thúc lưu trú/qua đời (feature 004).
- Kế hoạch chăm sóc, công việc, lịch cá nhân (feature 005); đơn thuốc, liều, đối chiếu thuốc (feature 006); sự cố, cảnh báo, thẻ thông tin khẩn cấp (feature 007); thông báo (feature 009); chi phí (feature 010); chế độ ăn, suất ăn (feature 011); người thân, quyền của người thân, cổng người thân, đón người cao tuổi (feature 012); đồ gửi (feature 013); hoạt động ngoài viện (feature 014).
- Tác động khi chuyển trạng thái (hủy công việc, tạm dừng liều, hủy suất ăn, giữ giường, thông báo để actor lập công việc…) do feature sở hữu thực hiện; hệ thống không tự sinh công việc hay dữ liệu (Q-306); spec này chỉ quy định **khi nào** tác động được kích hoạt.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Hành chính tạo hồ sơ người cao tuổi và nhân viên tra cứu theo quyền (Priority: P1)

Khi một gia đình đăng ký, nhân viên hành chính tạo hồ sơ cá nhân cho người cao tuổi thay cho sổ giấy. Hồ sơ mới ở trạng thái Đang tiếp nhận. Nhân viên các vai trò khác tra cứu hồ sơ trong phạm vi được phân công và chỉ thấy phần thông tin vai trò mình được xem.

**Why this priority**: Mọi nghiệp vụ khác (đánh giá, hợp đồng, chăm sóc, thuốc, chi phí) đều gắn với người cao tuổi (mục 1.3). Không có hồ sơ thì không feature nào chạy được.

**Independent Test**: Tạo một hồ sơ với đầy đủ thông tin, tạo hồ sơ thứ hai trùng CCCD, rồi đăng nhập lần lượt bằng từng vai trò trong Permission Matrix để tra cứu và kiểm tra phần hiển thị.

**Acceptance Scenarios**:

1. **Given** nhân viên hành chính có quyền tạo hồ sơ, **When** nhập họ tên, ngày sinh, giới tính, giấy tờ định danh, ảnh, địa chỉ thường trú, địa chỉ liên hệ, thông tin liên hệ, thông tin đặc biệt và lưu, **Then** hệ thống cấp một mã hồ sơ duy nhất, hồ sơ ở trạng thái Đang tiếp nhận, lịch sử trạng thái có bản ghi đầu tiên kèm người tạo và thời điểm.
2. **Given** đã có hồ sơ chưa ở trạng thái cuối mang CCCD X, **When** hành chính tạo hồ sơ mới với cùng CCCD X, **Then** hệ thống từ chối và chỉ ra hồ sơ đang mang CCCD đó (DBR-01).
3. **Given** hồ sơ H1 mang CCCD X đang ở Kết thúc lưu trú (hoặc Hủy tiếp nhận), **When** hành chính tạo hồ sơ mới với cùng CCCD X, **Then** hệ thống tạo hồ sơ H2 ở Đang tiếp nhận với mã hồ sơ mới, liên kết H2 với H1; H1 giữ nguyên chỉ đọc; người được xem H2 xem được H1 qua liên kết theo quyền của họ trên H1 (FR-004).
4. **Given** hồ sơ mới không có CCCD và đã có hồ sơ trùng họ tên và ngày sinh, **When** lưu, **Then** hệ thống cảnh báo khả năng trùng và yêu cầu hành chính xác nhận trước khi tạo.
5. **Given** hồ sơ đang ở trạng thái không phải trạng thái cuối, **When** hành chính thực hiện lệnh "Cập nhật thông tin cá nhân" kèm lý do, **Then** thông tin mới được áp dụng, giá trị trước/sau được ghi nhật ký; **When** thiếu lý do, **Then** hệ thống từ chối.
6. **Given** nhân viên chăm sóc được phân công cho người cao tuổi A trong ca hiện tại, **When** tra cứu hồ sơ A, **Then** thấy hồ sơ; **When** tra cứu hồ sơ B ngoài phạm vi, **Then** hệ thống từ chối (BR-M15-01).
7. **Given** nhân viên bếp, **When** tra cứu hồ sơ người cao tuổi, **Then** hệ thống từ chối (Permission Matrix dòng "Hồ sơ người cao tuổi"). *(Sửa 2026-10-06, Q-282: không còn vai trò Nhân viên vệ sinh.)*
8. *(Bỏ 2026-10-06, Q-283: không còn vai trò Dinh dưỡng viên; Bác sĩ xem đầy đủ hồ sơ sức khỏe.)*
9. **Given** người thân của người cao tuổi A, **When** xem hồ sơ A, **Then** thấy thông tin cá nhân; phần sức khỏe chỉ hiện khi có bản đồng ý đang hiệu lực bao gồm người thân đó (User Story 5); **When** xem hồ sơ người cao tuổi khác, **Then** hệ thống từ chối.
10. **(Bổ sung, CHK033)** **Given** hồ sơ H3 mang CCCD Y ở trạng thái Qua đời, **When** hành chính tạo hồ sơ mới với CCCD Y, **Then** hệ thống tạo hồ sơ mới nhưng không liên kết với H3 và cảnh báo "CCCD trùng hồ sơ đã qua đời, cần kiểm tra lại giấy tờ" (FR-004). **Given** người X có hai hồ sơ cũ H1 (Kết thúc lưu trú 2025) và H2 (Kết thúc lưu trú 2026), **When** tạo hồ sơ H4 cho X, **Then** H4 liên kết với H2, và từ H4 xem được cả H2 lẫn H1 qua chuỗi liên kết.

---

### User Story 2 - Bác sĩ, điều dưỡng ghi hồ sơ sức khỏe ban đầu; dị ứng và bệnh nền là từng mục chỉ loại trừ (Priority: P1)

Khi tiếp nhận, bác sĩ hoặc điều dưỡng ghi hồ sơ sức khỏe ban đầu. Dị ứng, bệnh nền, tiền sử bệnh được ghi thành từng mục riêng có mức độ, nguồn, người ghi, ngày ghi. Khi một mục không còn đúng, người có quyền loại trừ mục đó kèm lý do; mục không bao giờ bị xóa hay sửa nội dung.

**Why this priority**: Dị ứng là thông tin an toàn: nó được dùng để kiểm tra đơn thuốc (BR-M07-06), chế độ ăn (BR-M08-02) và hiển thị trên thẻ thông tin khẩn cấp (9.5). Mất hoặc sửa âm thầm một mục dị ứng có thể gây hại trực tiếp cho người cao tuổi.

**Independent Test**: Ghi 2 mục dị ứng và 1 bệnh nền, loại trừ 1 mục dị ứng, rồi thử xóa và sửa nội dung; kiểm tra danh sách hiện hành, danh sách đã loại trừ và nhật ký.

**Acceptance Scenarios**:

1. **Given** hồ sơ đang Đang tiếp nhận hoặc Đang lưu trú, **When** bác sĩ hoặc điều dưỡng ghi một mục dị ứng với nội dung, mức độ, nguồn thông tin, **Then** hệ thống lưu mục ở trạng thái Hiệu lực kèm người ghi và ngày ghi.
2. **Given** một mục dị ứng mới vừa được ghi, **When** lưu thành công, **Then** hệ thống kích hoạt kiểm tra lại mọi đơn thuốc đang hiệu lực (BR-M07-06) và chế độ ăn, thực đơn đang phân bổ (BR-M08-02) của người cao tuổi đó; mỗi xung đột tạo một cảnh báo.
3. **Given** một mục đang Hiệu lực, **When** bác sĩ hoặc điều dưỡng chọn "Loại trừ" kèm lý do, **Then** mục chuyển Đã loại trừ, vẫn hiển thị trong lịch sử kèm lý do, người loại trừ, thời điểm; **When** thiếu lý do, **Then** hệ thống từ chối.
4. **Given** bất kỳ mục nào, **When** bất kỳ ai (kể cả quản lý viện) cố xóa hoặc sửa nội dung mục, **Then** hệ thống không có thao tác đó (DBR-04).
5. **Given** mục dị ứng ghi sai nội dung, **When** cần sửa, **Then** người dùng loại trừ mục sai với lý do "ghi nhầm" và ghi một mục mới đúng; cả hai mục cùng hiện trong lịch sử.
6. **Given** nhân viên chăm sóc hoặc trưởng tầng, **When** cố ghi hoặc loại trừ một mục, **Then** hệ thống từ chối (Permission Matrix dòng "Dị ứng, bệnh nền": chỉ BS, ĐD là T).
7. **Given** người cao tuổi đã có một mục dị ứng Hiệu lực với cùng dị nguyên trong danh mục, **When** ghi thêm mục trùng, **Then** hệ thống cảnh báo trùng và yêu cầu xác nhận.
8. **Given** dị nguyên "Penicillin" có trong danh mục và người cao tuổi đang có đơn thuốc chứa amoxicillin, **When** điều dưỡng chọn "Penicillin" từ danh mục và lưu, **Then** việc kiểm tra theo FR-020 phát hiện xung đột và tạo cảnh báo; **When** thay vào đó điều dưỡng ghi dị ứng loại "khác" với văn bản "phấn hoa cây X" (chưa có trong danh mục), **Then** mục được lưu, hệ thống cảnh báo ngay rằng mục này không được kiểm tra tự động, và mục mang dấu "không kiểm tra tự động" trên hồ sơ và thẻ thông tin khẩn cấp (FR-018a).

---

### User Story 3 - Bác sĩ đánh giá đầu vào bằng nhận xét và chọn mức chăm sóc (Priority: P1) *(viết lại 2026-10-06, Q-280, Q-281)*

Bác sĩ (có điều dưỡng hỗ trợ lập bản nháp khi cần) xem hồ sơ sức khỏe ban đầu và các chỉ số đo khi tiếp nhận. Bác sĩ ghi nhận xét đánh giá, phần lưu ý chăm sóc (ví dụ "nguy cơ ngã cao, hỗ trợ khi di chuyển") và chọn mức chăm sóc. Hệ thống không tính điểm, không đề xuất mức, không gắn cờ. Lần đánh giá Đã xác nhận là căn cứ để điều dưỡng lập kế hoạch chăm sóc bằng tay.

**Why this priority**: 5.6 chặn Hoàn tất tiếp nhận khi chưa có đánh giá đầu vào hoàn tất. Mức chăm sóc là đầu vào của hợp đồng, phân bổ giường (BR-M03-01), tỷ lệ phục vụ (BR-M09-02) và kế hoạch chăm sóc.

**Independent Test**: Lập một lần đánh giá đầu vào; thử xác nhận khi thiếu nhận xét, khi thiếu mức chăm sóc, khi điều dưỡng xác nhận; xác nhận hợp lệ rồi kiểm tra mức chăm sóc hiện hành, lưu ý chăm sóc trên thẻ người cao tuổi và việc không có bản nháp kế hoạch chăm sóc nào được tạo tự động.

**Acceptance Scenarios**:

1. **Given** hồ sơ ở Đang tiếp nhận, **When** bác sĩ hoặc điều dưỡng tạo lần đánh giá đầu vào, **Then** lần đánh giá ở Nháp và màn hình đánh giá hiển thị kèm hồ sơ sức khỏe ban đầu, các mục sức khỏe Hiệu lực và chỉ số đo gần nhất (feature 007) để tham khảo (FR-024).
2. **Given** lần đánh giá Nháp chưa có nhận xét hoặc chưa chọn mức chăm sóc, **When** bác sĩ xác nhận, **Then** hệ thống từ chối và chỉ ra trường còn thiếu (FR-028, DBR-05).
3. **Given** lần đánh giá Nháp có nhận xét và mức "Chăm sóc thường xuyên", **When** bác sĩ xác nhận, **Then** lần đánh giá chuyển Đã xác nhận, mức chăm sóc hiện hành của người cao tuổi là "Chăm sóc thường xuyên"; hệ thống không tạo bản nháp kế hoạch chăm sóc nào (FR-031, feature 005).
4. **Given** nhận xét có phần lưu ý chăm sóc "nguy cơ ngã cao, hỗ trợ khi di chuyển", **When** bác sĩ xác nhận, **Then** phần lưu ý hiện trên thẻ người cao tuổi trong checklist ca (feature 005) và trên thẻ thông tin khẩn cấp (feature 007); không có cờ nào được gắn (FR-038, BR-M01-10).
5. **Given** người cao tuổi sau tai biến cần tập phục hồi, **When** bác sĩ chọn mức "Chăm sóc đặc biệt" cùng nhu cầu "Phục hồi sau tai biến" và "Hỗ trợ vận động" rồi xác nhận, **Then** hệ thống chấp nhận mà không đòi lý do (FR-029); mức chăm sóc hiện hành là "Chăm sóc đặc biệt", hai nhu cầu được lưu cùng lần đánh giá và hiện trên hồ sơ. *(Sửa 2026-10-06, Q-291)*
6. **Given** điều dưỡng đã lập đủ nhận xét và mức chăm sóc, **When** điều dưỡng cố xác nhận, **Then** hệ thống từ chối; chỉ bác sĩ được xác nhận (Permission Matrix dòng "Đánh giá, mức chăm sóc": BS T, D; ĐD T).
7. **Given** lần đánh giá Đã xác nhận, **When** bất kỳ ai cố sửa nhận xét hoặc mức chăm sóc, **Then** hệ thống không có thao tác sửa; sai sót xử lý bằng đính chính theo feature 000 (FR-032, Edge Cases).
8. **Given** người cao tuổi có loại hình lưu trú "Bán trú" theo hợp đồng, **When** bác sĩ chọn mức "Chăm sóc đặc biệt", **Then** hệ thống chấp nhận tổ hợp này; thay đổi mức chăm sóc không làm thay đổi loại hình lưu trú và ngược lại (FR-051).

---

### User Story 4 - Trạng thái người cao tuổi chỉ thay đổi qua lệnh nghiệp vụ có điều kiện (Priority: P1)

Không ai "sửa trạng thái". Mỗi chuyển trạng thái là một lệnh nghiệp vụ riêng (Hoàn tất tiếp nhận, Hủy tiếp nhận, Cho tạm vắng, Ghi nhận trở về, Chuyển viện, Kết thúc lưu trú, Ghi nhận qua đời, và hai chuyển do điểm danh chuyến đi ngoài viện). Mỗi lệnh chỉ chạy khi trạng thái hiện tại cho phép theo bảng 5.5 và điều kiện ở 5.6 được thỏa; nếu không, hệ thống từ chối và nêu điều kiện chưa thỏa. Mọi lần chuyển được ghi lịch sử.

**Why this priority**: Trạng thái người cao tuổi quyết định công việc nào được sinh, liều thuốc nào được phát, suất ăn nào được nấu, giường nào được giữ. Chuyển sai trạng thái kéo theo sai ở hầu hết module khác.

**Independent Test**: Đi hết các chuyển hợp lệ trong bảng trạng thái (mục C), thử mọi cặp trạng thái không có trong bảng, và thử từng lệnh khi thiếu từng điều kiện chặn. Điều kiện do feature khác cung cấp (hợp đồng, đặt cọc, giường, đồ gửi, thuốc gửi, chi phí, cảnh báo, sự cố) được giả lập bằng kết quả Đạt / Chưa đạt, nên kiểm thử không cần feature đó đã chạy (CHK035).

**Acceptance Scenarios**:

1. **Given** hồ sơ Đang tiếp nhận có đánh giá đầu vào Đã xác nhận, hợp đồng Hiệu lực, đặt cọc đạt, loại hình nội trú và đã có giường, **When** hành chính thực hiện "Hoàn tất tiếp nhận", **Then** trạng thái chuyển Đang lưu trú, lịch sử ghi trạng thái từ/đến, lệnh, người, thời điểm, lý do, lịch thuốc và suất ăn được tính từ ngày hiệu lực (006, 011), và Người phụ trách ca được báo để lập công việc (005; sửa 2026-10-06, Q-306).
2. **Given** hồ sơ Đang tiếp nhận chưa có đánh giá đầu vào Đã xác nhận, **When** thực hiện "Hoàn tất tiếp nhận", **Then** hệ thống từ chối, nêu rõ "chưa hoàn tất đánh giá đầu vào", trạng thái giữ nguyên; tương tự khi thiếu hợp đồng Hiệu lực, đặt cọc chưa đạt, hoặc nội trú chưa có giường — hệ thống liệt kê tất cả điều kiện chưa thỏa trong một lần. **Given** hồ sơ Đang tiếp nhận có đánh giá đầu vào được xác nhận cách đây 120 ngày (CFG-M01-02 mặc định \[90 ngày\]) và đủ các điều kiện khác, **When** thực hiện "Hoàn tất tiếp nhận", **Then** hệ thống từ chối với "đánh giá đã quá hạn, cần đánh giá lại"; **When** bác sĩ xác nhận một lần đánh giá lại rồi thực hiện lại lệnh, **Then** lệnh thành công và mức chăm sóc hiện hành là mức của lần đánh giá lại (FR-034a, FR-035).
3. **Given** hồ sơ Bán trú đủ các điều kiện khác nhưng không có giường, **When** thực hiện "Hoàn tất tiếp nhận", **Then** hệ thống chấp nhận (điều kiện giường chỉ áp dụng cho nội trú, 3.3).
4. **Given** hồ sơ Đang tiếp nhận, **When** hành chính thực hiện "Hủy tiếp nhận" kèm lý do, **Then** trạng thái chuyển Hủy tiếp nhận, giữ chỗ giường nếu có bị hủy (feature 003), hồ sơ chờ liên quan bị đóng (feature 004).
5. **Given** hồ sơ Hoạt động bên ngoài, **When** bất kỳ ai thực hiện "Kết thúc lưu trú", **Then** hệ thống từ chối vì chuyển này không có trong bảng 5.5.
6. **Given** hồ sơ Đang lưu trú, **When** trưởng đoàn điểm danh rời viện cho chuyến đi ngoài viện có người cao tuổi đó (feature 014), **Then** hệ thống tự chuyển trạng thái sang Hoạt động bên ngoài, người thực hiện ghi là người điểm danh, căn cứ là chuyến đi (BR-M04-16).
7. **Given** hồ sơ Điều trị tại bệnh viện, **When** thực hiện "Ghi nhận trở về", **Then** trạng thái chuyển Đang lưu trú; toàn bộ lịch thuốc cũ bị tạm dừng và điều dưỡng phụ trách được báo cần lập phiếu đối chiếu thuốc (BR-M07-09, feature 006); bác sĩ được báo cần đánh giá lại (User Story 6; sửa 2026-10-06, Q-307).
8. **Given** hồ sơ Đang lưu trú và người đón thuộc danh sách được phép đón, **When** trưởng tầng thực hiện "Cho tạm vắng" kèm lý do, **Then** trạng thái chuyển Tạm vắng ngay, không tạo yêu cầu phê duyệt nào (FR-047a).
9. **Given** hồ sơ Tạm vắng có thời điểm dự kiến trở lại T, **When** đến T + CFG-M01-01 (mặc định \[2 giờ\]) mà chưa có "Ghi nhận trở về", **Then** hệ thống cảnh báo trưởng tầng và hành chính (BR-M01-01), trạng thái giữ nguyên Tạm vắng.
10. **Given** hồ sơ Đang lưu trú còn đồ gửi đang giữ, **When** thực hiện "Kết thúc lưu trú", **Then** hệ thống từ chối và nêu điều kiện chưa thỏa; **When** quản lý viện đã duyệt ngoại lệ cho lần kết thúc này, **Then** lệnh được thực hiện, lịch sử ghi tham chiếu yêu cầu ngoại lệ.
11. **Given** hồ sơ Tạm vắng (người cao tuổi mất tại nhà), **When** hành chính thực hiện "Ghi nhận qua đời" kèm bản scan giấy báo tử, **Then** trạng thái chuyển Qua đời, người liên hệ chính được thông báo ở mức Khẩn cấp (feature 009, Q-97), hồ sơ chuyển chỉ đọc; **When** thiếu giấy tờ bằng chứng, **Then** hệ thống từ chối; **Given** hồ sơ Đang lưu trú, **When** hành chính thực hiện "Ghi nhận qua đời", **Then** hệ thống từ chối vì người cao tuổi mất tại viện phải do bác sĩ xác nhận (FR-047b).
12. **Given** hồ sơ ở Kết thúc lưu trú, Qua đời hoặc Hủy tiếp nhận, **When** bất kỳ ai cố cập nhật thông tin cá nhân, ghi mục sức khỏe, tạo đánh giá hay thực hiện lệnh chuyển trạng thái, **Then** hệ thống chặn; hồ sơ vẫn tra cứu được theo quyền (BR-M01-05); **When** hành chính thực hiện "Rút lại đồng ý" kèm lý do và bằng chứng trên hồ sơ Kết thúc lưu trú có bản đồng ý Hiệu lực, **Then** lệnh được thực hiện và người thân trong phạm vi mất quyền xem sức khỏe ngay (FR-014a); **When** bác sĩ lập bản đính chính nhận xét của lần đánh giá lại cuối cùng trên hồ sơ Qua đời, **Then** hệ thống tạo yêu cầu phê duyệt loại "Đính chính" ở Chờ duyệt, bản gốc chưa bị đánh dấu; bản đính chính chỉ được lưu khi Quản lý viện duyệt, và không báo đánh giá lại (FR-049a).
13. **Given** hai người thực hiện hai lệnh khác nhau trên cùng một hồ sơ gần như đồng thời (ví dụ "Cho tạm vắng" và "Chuyển viện"), **When** cả hai gửi, **Then** chỉ lệnh đến trước được áp dụng; lệnh sau được kiểm tra lại trên trạng thái mới và bị từ chối nếu không còn hợp lệ.

---

### User Story 5 - Hành chính ghi nhận và thu hồi bản đồng ý chia sẻ dữ liệu (Priority: P2)

Khi tiếp nhận, hành chính ghi nhận bản đồng ý xử lý và chia sẻ dữ liệu của người cao tuổi (hoặc của người đại diện hợp pháp khi người cao tuổi không đủ khả năng): phạm vi chia sẻ thông tin sức khỏe cho những người thân nào, sử dụng hình ảnh, nhận thông báo; kèm bản ký được scan. Khi đồng ý bị rút lại, quyền xem sức khỏe tương ứng của người thân tắt ngay.

**Why this priority**: Đây là căn cứ pháp lý để cổng người thân hiển thị thông tin sức khỏe (BR-M01-08, DBR-03). Chăm sóc tại viện vẫn chạy được khi chưa có cổng người thân, nên xếp sau nhóm P1.

**Independent Test**: Ghi nhận một bản đồng ý bao gồm người thân X nhưng không gồm Y, kiểm tra X xem được sức khỏe còn Y không; rút lại bản đồng ý và kiểm tra X mất quyền ngay.

**Acceptance Scenarios**:

1. **Given** hồ sơ chưa ở trạng thái cuối, **When** hành chính ghi nhận bản đồng ý với người đồng ý, phạm vi (danh sách người thân được xem sức khỏe, sử dụng hình ảnh, nhận thông báo), thời điểm, bản scan có chữ ký, **Then** bản đồng ý ở trạng thái Hiệu lực.
2. **Given** thiếu bản scan bằng chứng, **When** lưu, **Then** hệ thống từ chối.
3. **Given** người đồng ý là người đại diện, **When** lưu, **Then** hệ thống yêu cầu người đó đang là người đại diện của người cao tuổi (feature 012) và yêu cầu ghi lý do người cao tuổi không tự đồng ý.
4. **Given** bản đồng ý Hiệu lực bao gồm người thân X, **When** X mở phần sức khỏe trên cổng người thân, **Then** thấy thông tin; **Given** người thân Y không có trong phạm vi, **When** Y mở, **Then** không thấy (BR-M01-08).
5. **Given** bản đồng ý Hiệu lực, **When** hành chính ghi nhận "Rút lại đồng ý" kèm lý do và bằng chứng yêu cầu rút lại, **Then** bản đồng ý chuyển Đã rút lại, quyền xem sức khỏe của mọi người thân thuộc phạm vi đó bị tắt ngay ở lần truy cập tiếp theo (liên kết 14.1).
6. **Given** bản đồng ý Hiệu lực, **When** cần đổi phạm vi (thêm/bớt người thân), **Then** hành chính ghi nhận bản đồng ý mới; bản cũ chuyển Đã rút lại với lý do "được thay thế" và tham chiếu bản mới; người thân bị bớt mất quyền ngay.
7. **Given** hồ sơ Đang tiếp nhận đủ mọi điều kiện ở 5.6 nhưng chưa có bản đồng ý Hiệu lực, **When** hành chính thực hiện "Hoàn tất tiếp nhận", **Then** lệnh được thực hiện, hệ thống hiển thị cảnh báo "chưa có bản đồng ý chia sẻ dữ liệu", và từ đó nhắc hành chính mỗi CFG-M01-06 (mặc định \[1 ngày\]); **When** bản đồng ý được ghi nhận, **Then** việc nhắc dừng.
8. **Given** người cao tuổi Đang lưu trú chưa có bản đồng ý Hiệu lực, **When** bất kỳ người thân nào mở phần sức khỏe trên cổng người thân, **Then** không thấy thông tin sức khỏe.
9. **Given** trưởng tầng, bác sĩ hoặc điều dưỡng, **When** cố ghi nhận hoặc rút lại bản đồng ý, **Then** hệ thống từ chối (Permission Matrix: chỉ HC là T).

---

### User Story 6 - Đánh giá lại theo định kỳ, sau sự cố, sau nằm viện; mức chăm sóc thay đổi thì Hành chính lập yêu cầu thay đổi lưu trú (Priority: P2) *(viết lại 2026-10-06, Q-306, Q-307)*

Hệ thống báo bác sĩ cần đánh giá lại khi đến hạn định kỳ, sau sự cố ngã hoặc sự cố mức trung bình trở lên, và khi người cao tuổi trở về từ bệnh viện; hệ thống không tạo yêu cầu đánh giá lại. Bác sĩ, điều dưỡng tự tạo lần đánh giá lại bất kỳ lúc nào khi tình trạng thay đổi đáng kể hoặc có yêu cầu chuyên môn. Đánh giá lại làm cùng cách như đánh giá đầu vào: nhận xét, lưu ý chăm sóc, mức chăm sóc. Nếu mức chăm sóc được xác nhận khác mức hiện tại, Hành chính được báo để lập yêu cầu thay đổi lưu trú. *(Sửa 2026-10-06, Q-280, Q-281: bỏ thang điểm, quy đổi, cờ nguy cơ; Q-306, Q-307: bỏ yêu cầu do hệ thống tạo.)*

**Why this priority**: Giữ mức chăm sóc và lưu ý chăm sóc khớp với tình trạng thực tế. Cần đánh giá đầu vào (User Story 3) có trước.

**Independent Test**: Dùng đồng hồ giả lập (NFR-13) đưa thời gian tới hạn CFG-M01-02, kiểm tra bác sĩ được báo; hoàn thành đánh giá lại với mức khác mức hiện tại và kiểm tra Hành chính được báo; để quá CFG-M01-03 kể từ lúc báo và kiểm tra cảnh báo.

**Acceptance Scenarios**:

1. **Given** lần đánh giá Đã xác nhận gần nhất của người cao tuổi Đang lưu trú cách hiện tại CFG-M01-02 (mặc định \[90 ngày\]), **When** Bộ lập lịch hệ thống chạy, **Then** bác sĩ được báo cần đánh giá lại, căn cứ "định kỳ"; người cao tuổi hiện "cần đánh giá lại" với hạn CFG-M01-03 (mặc định \[48 giờ\]) tính từ lúc báo; không có bản ghi yêu cầu nào được tạo (BR-M01-02, UC-07).
2. **Given** feature 007 ghi nhận một sự cố ngã hoặc sự cố mức trung bình trở lên cho người cao tuổi, **When** sự cố được ghi, **Then** bác sĩ được báo cần đánh giá lại, căn cứ "sau sự cố", tham chiếu sự cố.
3. **Given** người cao tuổi đang "cần đánh giá lại" vì căn cứ định kỳ, **When** phát sinh thêm một căn cứ (ví dụ sự cố ngã), **Then** bác sĩ được báo thêm căn cứ mới; hạn vẫn tính từ lần báo sớm nhất chưa được đáp ứng.
4. **Given** đã quá CFG-M01-03 kể từ lúc báo mà chưa có lần đánh giá lại Đã xác nhận, **When** Bộ lập lịch kiểm tra, **Then** hệ thống cảnh báo bác sĩ; người cao tuổi mang trạng thái "quá hạn đánh giá lại" tới khi có lần đánh giá lại Đã xác nhận.
5. **Given** mức chăm sóc hiện hành là "Chăm sóc cơ bản", **When** bác sĩ xác nhận đánh giá lại với mức "Chăm sóc đặc biệt", **Then** Hành chính được báo để lập yêu cầu thay đổi lưu trú loại "đổi mức chăm sóc" (BR-M01-03, feature 004); chưa có yêu cầu thì Hành chính được nhắc mỗi CFG-M03-02 (mặc định \[1 ngày\]); mức chăm sóc hiện hành và kế hoạch chăm sóc cũ giữ nguyên tới ngày hiệu lực của thay đổi.
6. **Given** đã có yêu cầu đổi mức sang "Chăm sóc đặc biệt" đang Chờ duyệt, **When** bác sĩ xác nhận đánh giá lại mới với mức "Chăm sóc thường xuyên", **Then** yêu cầu cũ chuyển "Được thay thế" (không tạo tác động, trỏ tới lần đánh giá mới), và Hành chính được báo để lập yêu cầu mới sang "Chăm sóc thường xuyên"; **When** thay vào đó mức mới bằng mức hiện hành, **Then** yêu cầu cũ chuyển "Được thay thế" và Hành chính không được báo lập yêu cầu mới (FR-036a).
7. *(Sửa 2026-10-06, Q-281)* **Given** lần đánh giá trước của A có lưu ý chăm sóc "nguy cơ ngã cao", **When** bác sĩ xác nhận đánh giá lại với lưu ý "đi lại vững, không cần hỗ trợ", **Then** thẻ người cao tuổi và thẻ thông tin khẩn cấp hiện lưu ý của lần mới thay lần cũ; lưu ý cũ vẫn xem được trong lịch sử đánh giá (FR-038).
8. *(Sửa 2026-10-06, Q-281)* **Given** lần đánh giá lại Đã xác nhận gần nhất của A để trống phần lưu ý chăm sóc, **When** xem thẻ người cao tuổi, **Then** thẻ không hiện lưu ý nào; hệ thống không lấy lưu ý từ lần đánh giá cũ hơn (FR-038).
9. **Given** bác sĩ xác nhận đánh giá lại có mức hoặc nhu cầu chăm sóc khác lần trước, **When** lưu, **Then** người cao tuổi hết "cần đánh giá lại", và điều dưỡng phụ trách được báo để xem xét kế hoạch chăm sóc (BR-M04-20, feature 005); không có yêu cầu xem xét nào được tạo.
10. **(Bổ sung, CHK034)** **Given** người cao tuổi đang "cần đánh giá lại", **When** hồ sơ chuyển Kết thúc lưu trú, Qua đời hoặc Hủy tiếp nhận, **Then** không còn nhắc và không cảnh báo quá hạn nữa (FR-043).
11. **(Bổ sung, CHK034)** **Given** mức chăm sóc của A là "Chăm sóc cơ bản" từ 01/03 và đổi sang "Chăm sóc đặc biệt" từ 15/06 theo một yêu cầu thay đổi lưu trú, **When** tra cứu mức chăm sóc của A tại ngày 01/06 và ngày 20/06, **Then** kết quả là "Chăm sóc cơ bản" và "Chăm sóc đặc biệt", mỗi giá trị kèm căn cứ (lần đánh giá, yêu cầu thay đổi lưu trú) (FR-053).
12. **(Bổ sung, CHK037)** **Given** A đang Tạm vắng, mức hiện hành "Chăm sóc cơ bản", **When** bác sĩ xác nhận đánh giá lại với mức "Chăm sóc thường xuyên", **Then** Hành chính được báo để lập yêu cầu thay đổi lưu trú như kịch bản 5; trạng thái Tạm vắng không đổi (FR-035, FR-036).

---

### User Story 7 - Bác sĩ, điều dưỡng ghi phiếu nguyện vọng cuối đời khi tiếp nhận (Priority: P2) *(bổ sung 2026-09-28)*

Khi tiếp nhận, bác sĩ hoặc điều dưỡng khảo sát và ghi nguyện vọng của người cao tuổi hoặc gia đình về nơi chăm sóc khi nguy kịch: chuyển bệnh viện điều trị tích cực, đưa về nhà, hoặc ở lại viện chăm sóc giảm nhẹ. Phiếu có người ký, bản scan, và được thay bằng phiên bản mới khi nguyện vọng đổi. Phiếu không chặn tiếp nhận nhưng được nhắc tới khi có.

**Why this priority**: Góp ý nghiệp vụ Q-213: khi người cao tuổi nguy kịch, nhân viên phải biết ngay gia đình muốn gì; đây là thông tin có ý nghĩa pháp lý (Q-216).

**Independent Test**: Hoàn tất tiếp nhận một người chưa có phiếu, kiểm tra cảnh báo và nhắc; ghi phiếu, ghi phiên bản mới; thử xem phiếu bằng tài khoản Hành chính.

**Acceptance Scenarios**:

1. **Given** A Đang tiếp nhận, Bác sĩ nhận định A còn đủ năng lực, **When** điều dưỡng D ghi phiếu với lựa chọn "chuyển bệnh viện điều trị tích cực", người ký là A, kèm bản scan, **Then** phiếu Hiệu lực với người ghi D, thời điểm, người ký A (FR-023a, FR-023c).
2. **Given** B không còn đủ năng lực theo nhận định của Bác sĩ, **When** D ghi phiếu với người ký là chính B, **Then** hệ thống chặn; người ký phải là một người đại diện Hiệu lực (FR-023c, Q-216).
3. **Given** A còn đủ năng lực chọn "ở lại viện chăm sóc giảm nhẹ", con của A muốn "chuyển bệnh viện", **When** D ghi phiếu, **Then** lựa chọn ghi theo ý A; ý kiến của con được ghi vào ghi chú (FR-023c).
4. **Given** C đủ mọi điều kiện tiếp nhận nhưng chưa có phiếu, **When** hành chính Hoàn tất tiếp nhận, **Then** lệnh thành công, hiển thị cảnh báo "chưa có nguyện vọng cuối đời", Bác sĩ và Điều dưỡng phụ trách được nhắc mỗi CFG-M01-06 (mặc định \[1 ngày\]) tới khi có phiếu (FR-023d).
5. **Given** A có phiếu Hiệu lực, **When** gia đình A yêu cầu đổi sang "đưa về nhà" và D ghi phiên bản mới, **Then** phiên bản cũ chuyển Được thay thế, phiên bản mới Hiệu lực; không có thao tác sửa hay xóa phiên bản cũ (FR-023b, DBR-34).
6. **Given** hành chính H mở hồ sơ A, **When** xem mục nguyện vọng, **Then** H chỉ thấy "đã có nguyện vọng", không thấy lựa chọn hay ghi chú (FR-023e).

---

### Edge Cases

- **Đánh giá đã xác nhận bị ghi sai** *(sửa 2026-10-06, Q-280)*: sửa bằng đính chính theo feature 000. Đính chính không tự đổi mức chăm sóc đã áp dụng; nếu bản đính chính ghi mức chăm sóc khác mức của lần đã xác nhận, hệ thống báo bác sĩ cần đánh giá lại với căn cứ "đính chính đánh giá". Đính chính chỉ sửa nhận xét hoặc lưu ý chăm sóc thì không báo; thẻ người cao tuổi hiện lưu ý theo bản đính chính.
- **Đính chính một lần đánh giá không phải lần gần nhất** (CHK038): bản đính chính được lưu để lịch sử đúng, nhưng không báo đánh giá lại; chỉ đính chính lần đánh giá Đã xác nhận gần nhất mới xét báo theo FR-032.
- **Người cao tuổi Đang tiếp nhận qua đời hoặc phải nhập viện trước khi Hoàn tất tiếp nhận** (CHK008): bảng 5.5 không có chuyển nào khác từ Đang tiếp nhận, nên hành chính dùng "Hủy tiếp nhận" kèm lý do tương ứng; muốn tiếp nhận lại thì theo FR-004.
- **Người cao tuổi xuất viện nhưng về thẳng nhà, không quay lại viện** (CHK009): dùng lệnh "Kết thúc lưu trú" từ Điều trị tại bệnh viện với đủ điều kiện của lệnh; không cần "Ghi nhận trở về" trước.
- **Kiểm tra dị ứng không chạy được** (feature 006 hoặc 011 lỗi, CHK036): mục dị ứng vẫn được lưu và mang dấu "chờ kiểm tra"; hệ thống thử lại tới khi cả hai kiểm tra chạy xong rồi gỡ dấu; trong lúc đó mục vẫn hiện trên hồ sơ và thẻ thông tin khẩn cấp. Đây là ngoại lệ có chủ ý của nguyên tắc "hoặc toàn bộ, hoặc không": mất mục dị ứng nguy hiểm hơn chậm kiểm tra.
- **Người đại diện đã ký bản đồng ý rồi thôi là người đại diện** (CHK040): bản đồng ý vẫn Hiệu lực vì nó có hiệu lực theo thời điểm ký; chỉ lệnh "Rút lại đồng ý" hoặc bản thay thế mới làm nó hết hiệu lực.
- **Người cao tuổi đang Điều trị tại bệnh viện khi đến hạn đánh giá định kỳ**: bác sĩ vẫn được báo căn cứ định kỳ, nhưng hạn CFG-M01-03 chỉ bắt đầu tính từ khi Ghi nhận trở về, cùng với căn cứ "trở về từ bệnh viện".
- **Tạm vắng dài vượt dự kiến**: BR-M01-01 cảnh báo đúng một lần mỗi mốc dự kiến trở lại; nếu thời điểm dự kiến được gia hạn (feature 004), mốc cảnh báo tính lại theo thời điểm mới.
- **Hoạt động bên ngoài không trở về đúng kế hoạch**: trạng thái giữ nguyên Hoạt động bên ngoài; feature 014 tạo cảnh báo Khẩn cấp và trưởng đoàn hoặc trưởng tầng ghi nhận sự cố (BR-M04-17, Q-307); trạng thái sau đó chuyển bằng lệnh phù hợp (Ghi nhận trở về, Chuyển viện, Ghi nhận qua đời).
- **Mức chăm sóc đang có yêu cầu thay đổi chờ duyệt thì có đánh giá lại mới cho mức khác nữa**: yêu cầu cũ chưa áp dụng được hệ thống chuyển sang "Được thay thế" và Hành chính được báo để lập yêu cầu mới; nếu mức mới trùng mức hiện hành thì yêu cầu cũ vẫn chuyển "Được thay thế" và không báo lập yêu cầu mới (FR-036a).
- **Loại trừ mục dị ứng**: không kích hoạt tự động bỏ cảnh báo đã tạo; cảnh báo đã tạo xử lý theo feature 007.
- **Rút lại đồng ý khi người thân đang mở trang sức khỏe**: dữ liệu sức khỏe không được trả thêm từ lần truy cập kế tiếp; không có khoảng trễ chờ đăng nhập lại.
- **Hồ sơ ở trạng thái cuối cần rút lại đồng ý** (người đại diện yêu cầu sau khi kết thúc lưu trú hoặc sau khi người cao tuổi qua đời): hành chính thực hiện "Rút lại đồng ý" như ngoại lệ của BR-M01-05 (FR-014a); ngoài đính chính có Quản lý viện duyệt (FR-049a), mọi thao tác ghi khác vẫn bị chặn. Không được ghi nhận bản đồng ý mới trên hồ sơ trạng thái cuối.
- **Người cao tuổi từng Hủy tiếp nhận hoặc Kết thúc lưu trú rồi đăng ký lại**: tạo hồ sơ mới liên kết hồ sơ cũ (FR-004); mục dị ứng cũ chỉ là tham khảo, bác sĩ/điều dưỡng phải ghi lại thành mục của hồ sơ mới để có hiệu lực.
- **Người từng lưu trú có nhiều hồ sơ cũ** (quay lại nhiều lần): hồ sơ mới liên kết với hồ sơ gần nhất; chuỗi liên kết cho xem được toàn bộ các lần trước.

## Requirements *(mandatory)*

### Functional Requirements

Mọi yêu cầu dưới đây kế thừa feature 000: nhóm dữ liệu (mục 1.5), lý do bắt buộc cho lệnh nhóm 2, đính chính cho nhóm 3, nhật ký cho mọi thay đổi, tham số theo mã CFG. Phân nhóm dữ liệu của feature này:

| Dữ liệu | Nhóm (1.5) | Thao tác được phép |
| --- | --- | --- |
| Danh mục dị nguyên | 1 – Danh mục | Tạo, sửa, Ngừng hiệu lực (người có quyền cấu hình danh mục thuốc/món ăn, theo feature 006, 011) |
| Người cao tuổi (thông tin cá nhân, trạng thái, loại hình lưu trú, mức chăm sóc) | 2 | Lệnh nghiệp vụ |
| Bản đồng ý chia sẻ dữ liệu | 2 | Ghi nhận, Rút lại |
| Mục sức khỏe (dị ứng, bệnh nền, tiền sử bệnh) | 2 | Ghi nhận, Loại trừ |
| Hồ sơ sức khỏe ban đầu | 3 (sau xác nhận) | Nháp sửa được; sau xác nhận chỉ đính chính |
| Phiếu nguyện vọng cuối đời | 2 – phiên bản | Ghi phiên bản mới; phiên bản đã lưu không sửa, không xóa (FR-023b) |
| Lần đánh giá (nhận xét, lưu ý chăm sóc, mức chăm sóc) | 3 (sau xác nhận) | Nháp sửa được; sau xác nhận chỉ đính chính |
| Lịch sử trạng thái | 3 | Chỉ ghi thêm |

*(Sửa 2026-10-06, Q-280, Q-281: bỏ hai dòng "Thang điểm và mục chấm", "Cờ nguy cơ". Sửa 2026-10-10, Q-307: bỏ dòng "Yêu cầu đánh giá lại".)*

#### A. Hồ sơ cá nhân

- **FR-001**: Hệ thống MUST cho phép nhân viên hành chính tạo hồ sơ người cao tuổi gồm: họ tên, ngày sinh, giới tính, giấy tờ định danh (CCCD hoặc giấy tờ khác), ảnh, địa chỉ thường trú (theo giấy tờ định danh), địa chỉ liên hệ, thông tin liên hệ, thông tin đặc biệt. Họ tên, ngày sinh, giới tính là bắt buộc. Thông tin người thân không thuộc hồ sơ này (Module 10, feature 012). *(Nguồn: 5.1, UC-01, Permission Matrix "Hồ sơ người cao tuổi": HC T; hai trường địa chỉ: Q-244)*
- **FR-002**: Hồ sơ mới MUST ở trạng thái Đang tiếp nhận và MUST có bản ghi lịch sử trạng thái đầu tiên. *(Nguồn: 5.5, BR-M01-04)*
- **FR-003**: Mã hồ sơ MUST duy nhất và do hệ thống cấp; CCCD, nếu có, MUST duy nhất trong các hồ sơ chưa ở trạng thái cuối (Kết thúc lưu trú, Qua đời, Hủy tiếp nhận); mỗi hồ sơ MUST có đúng một trạng thái hiện tại. *(Nguồn: DBR-01; Clarification 2026-09-25)*
- **FR-004**: Khi một người đã có hồ sơ ở trạng thái Kết thúc lưu trú hoặc Hủy tiếp nhận đăng ký tiếp nhận lại, hệ thống MUST tạo hồ sơ mới (mã hồ sơ mới, trạng thái Đang tiếp nhận) và liên kết với hồ sơ cũ; hồ sơ cũ MUST giữ nguyên chỉ đọc (BR-M01-05). Người được xem hồ sơ mới MUST xem được hồ sơ cũ qua liên kết, trong giới hạn quyền của họ trên hồ sơ cũ. Hồ sơ mới MUST NOT kế thừa tự động trạng thái, bản đồng ý hay đánh giá của hồ sơ cũ; mục sức khỏe Hiệu lực của hồ sơ cũ MAY được hiển thị làm tham khảo khi ghi mục sức khỏe cho hồ sơ mới. Hồ sơ Qua đời MUST NOT được dùng làm căn cứ tạo hồ sơ mới. *(Nguồn: DBR-01, BR-M01-05, 5.5, 6.2; Clarification 2026-09-25, đề xuất Q-12)*
- **FR-005**: Khi hồ sơ mới không có CCCD và trùng họ tên và ngày sinh với một hồ sơ đã có, hệ thống MUST cảnh báo và yêu cầu hành chính xác nhận trước khi tạo. Hồ sơ mới chỉ được liên kết với hồ sơ cũ (FR-004) khi trùng CCCD; không có CCCD thì không liên kết (Q-319). So khớp họ tên không phân biệt chữ hoa, chữ thường và khoảng trắng thừa, có phân biệt dấu; ngày sinh so khớp chính xác (CHK012). *(Suy ra từ DBR-01: tránh hai hồ sơ cho một người khi thiếu CCCD)*
- **FR-006**: Thông tin cá nhân MUST chỉ thay đổi qua lệnh "Cập nhật thông tin cá nhân" do hành chính thực hiện, kèm lý do; nhật ký ghi giá trị trước/sau. Lệnh MUST bị chặn khi hồ sơ ở trạng thái cuối. *(Nguồn: 1.5 nhóm 2, BR-M01-05)*
- **FR-007**: Tra cứu hồ sơ MUST tuân theo Permission Matrix dòng "Hồ sơ người cao tuổi": Quản lý viện, bác sĩ xem (X); trưởng tầng, điều dưỡng, nhân viên chăm sóc xem trong phạm vi được phân công (P); hành chính thực hiện (T); nhân viên bếp không có quyền (**sửa 2026-10-06, Q-282, Q-283:** bỏ Dinh dưỡng viên, Nhân viên vệ sinh); người thân chỉ xem hồ sơ của người cao tuổi mình có quan hệ. Quyền hiệu lực MUST là giao của vai trò, phạm vi dữ liệu và điều kiện pháp lý. Mọi vai trò được xem hồ sơ theo dòng này MUST thấy đủ thông tin cá nhân của FR-001, gồm giấy tờ định danh và ảnh; tài liệu nguồn không đặt quyền riêng cho hai trường này (CHK007). *(Nguồn: UC-02, 4.4, BR-M15-01)*
- **FR-008**: **(Sửa 2026-10-06, Q-280, Q-281, Q-283)** Trong hồ sơ, phần sức khỏe MUST lọc thêm theo vai trò: nhân viên chăm sóc xem dị ứng, bệnh nền trong phạm vi (P) nhưng không xem nhận xét đánh giá; người thân chỉ thấy dị ứng, bệnh nền khi thỏa FR-022. "Kết quả đánh giá" ở đây là nhận xét đánh giá. Phần lưu ý chăm sóc là thông tin an toàn, hiển thị theo FR-009 và FR-038 cho mọi nhân viên được xem hồ sơ (CHK026). *(Nguồn: 19.3, 4.4 dòng "Dị ứng, bệnh nền" và "Đánh giá, mức chăm sóc")*
- **FR-009**: **(Sửa 2026-10-06, Q-281)** Hồ sơ MUST hiển thị trạng thái hiện tại, loại hình lưu trú hiện hành, mức chăm sóc hiện hành và phần lưu ý chăm sóc của lần đánh giá Đã xác nhận gần nhất cho mọi người dùng được xem hồ sơ (trừ người thân với phần lưu ý chăm sóc, vốn là kết quả đánh giá).

#### B. Bản đồng ý chia sẻ dữ liệu

- **FR-010**: Hệ thống MUST cho phép hành chính ghi nhận bản đồng ý gồm: người đồng ý (người cao tuổi, hoặc người đại diện hợp pháp khi người cao tuổi không đủ khả năng); phạm vi (danh sách người thân được xem thông tin sức khỏe; đồng ý sử dụng hình ảnh; đồng ý nhận thông báo); thời điểm; bằng chứng là bản ký được scan (bắt buộc); trạng thái Hiệu lực / Đã rút lại. Hai mục phạm vi "sử dụng hình ảnh" và "nhận thông báo" được spec này lưu và cung cấp giá trị hiện hành cho feature 012 (bản tin, cổng người thân) và feature 009 (thông báo cho người thân); cách dùng và tác động khi rút lại do feature đó quy định, và có hiệu lực từ lần dùng kế tiếp như FR-015 (CHK006). *(Nguồn: 5.1, UC-03, 4.4 dòng "Bản đồng ý chia sẻ dữ liệu": HC T, QL X, NT X)*
- **FR-011**: Khi người đồng ý là người đại diện, người đó MUST đang là người đại diện của người cao tuổi (14.1, feature 012) và bản đồng ý MUST ghi lý do người cao tuổi không tự đồng ý.
- **FR-012**: Bản đồng ý MUST NOT là điều kiện chặn của "Hoàn tất tiếp nhận". Khi Hoàn tất tiếp nhận mà người cao tuổi chưa có bản đồng ý Hiệu lực, hệ thống MUST cảnh báo người thực hiện (lệnh vẫn được thực hiện) và MUST nhắc hành chính theo chu kỳ CFG-M01-06 (đề xuất, mặc định \[1 ngày\]) cho tới khi có bản đồng ý Hiệu lực hoặc hồ sơ chuyển trạng thái cuối. Trong thời gian chưa có bản đồng ý Hiệu lực, không người thân nào xem được thông tin sức khỏe (FR-022). Mẫu bản đồng ý và các mục phạm vi bắt buộc theo căn cứ pháp lý được chốt ở Q-03 (Luật Bảo vệ dữ liệu cá nhân số 91/2025/QH15 và Nghị định 356/2025/NĐ-CP, thay Nghị định 13/2023/NĐ-CP từ 01/01/2026, cần đối chiếu văn bản mới nhất). *(Nguồn: 5.1, 5.6, Q-03; Clarification 2026-09-25)*
- **FR-013**: Mỗi người cao tuổi MUST có tối đa một bản đồng ý Hiệu lực tại một thời điểm. Ghi nhận bản mới khi đã có bản Hiệu lực MUST chuyển bản cũ sang Đã rút lại với lý do "được thay thế" và tham chiếu bản mới, trong cùng một lần thực hiện.
- **FR-014**: Lệnh "Rút lại đồng ý" MUST do hành chính thực hiện, kèm lý do và bằng chứng yêu cầu rút lại của người đồng ý (hoặc người đại diện hợp pháp). Bản đồng ý Đã rút lại MUST NOT được kích hoạt lại; muốn đồng ý lại thì ghi nhận bản mới.
- **FR-014a**: Lệnh "Rút lại đồng ý" MUST được thực hiện được cả khi hồ sơ ở trạng thái cuối (Kết thúc lưu trú, Qua đời, Hủy tiếp nhận), với cùng điều kiện ở FR-014; đây là một trong hai ngoại lệ của BR-M01-05 cho thao tác của người dùng (cùng đính chính có duyệt, FR-049a). Ghi nhận bản đồng ý mới MUST bị chặn trên hồ sơ trạng thái cuối. *(Nguồn: 5.1 căn cứ bảo vệ dữ liệu cá nhân, BR-M01-05; Clarification 2026-09-25, Q-03)*
- **FR-015**: Ngay khi bản đồng ý chuyển Đã rút lại (kể cả do được thay thế), mọi người thân không còn thuộc phạm vi của một bản đồng ý Hiệu lực MUST mất quyền xem thông tin sức khỏe từ lần truy cập kế tiếp. *(Nguồn: BR-M01-08, 14.1)*
- **FR-016**: Bản đồng ý MUST NOT bị sửa nội dung hay xóa. *(Nguồn: 1.5 nhóm 2)*

#### C. Hồ sơ sức khỏe ban đầu và mục sức khỏe

- **FR-017**: Hệ thống MUST cho phép bác sĩ hoặc điều dưỡng ghi hồ sơ sức khỏe ban đầu gồm: nhóm máu nếu có; khả năng vận động; khả năng tự chăm sóc; khả năng nhận thức; ghi chú nhu cầu chăm sóc ban đầu (văn bản tham khảo; nhu cầu chăm sóc chính thức chọn từ danh mục khi đánh giá, FR-025, Q-291); nhu cầu dinh dưỡng; thuốc đang sử dụng khi tiếp nhận; nhu cầu phục hồi chức năng. *(Đã chỉnh sửa 2026-09-28: thông tin chăm sóc cuối đời chuyển thành phiếu nguyện vọng cuối đời ở mục C1.)* Hồ sơ sức khỏe ban đầu sửa được khi còn Nháp; sau khi bác sĩ xác nhận, chỉ sửa bằng đính chính (feature 000). Thuốc dùng trong thời gian lưu trú thuộc feature 006. Việc hồ sơ sức khỏe ban đầu đã được xác nhận hay chưa MUST NOT là điều kiện chặn của "Hoàn tất tiếp nhận" (5.6 không nêu; CHK003). *(Nguồn: 5.2)*
- **FR-018**: Dị ứng, bệnh nền và tiền sử bệnh MUST được lưu thành từng mục riêng, mỗi mục gồm: loại (dị ứng / bệnh nền / tiền sử bệnh); nội dung; mức độ (bắt buộc với dị ứng); nguồn thông tin; người ghi nhận; ngày ghi nhận; trạng thái Hiệu lực / Đã loại trừ; lý do loại trừ. Mức độ và nguồn thông tin MUST chọn từ danh mục cấu hình được (nhóm 1); giá trị khởi tạo đề xuất: mức độ Nhẹ / Trung bình / Nặng; nguồn thông tin Người cao tuổi / Người thân / Hồ sơ y tế kèm theo / Bác sĩ khám. "Thông tin đặc biệt" của FR-001 là văn bản tự do (CHK010). *(Nguồn: 5.2)*
- **FR-018a**: Nội dung của mục dị ứng MUST được chọn từ danh mục dị nguyên (hoạt chất hoặc nhóm thuốc, thành phần thực phẩm, dị nguyên khác), dùng chung với danh mục thuốc (feature 006) và thành phần món ăn (feature 011). Khi dị nguyên chưa có trong danh mục, người ghi MAY ghi loại "khác" bằng văn bản; mục đó MUST được đánh dấu "không kiểm tra tự động", hệ thống MUST cảnh báo người ghi ngay khi lưu rằng xung đột với thuốc và chế độ ăn sẽ không được phát hiện tự động, và dấu hiệu này MUST hiển thị cùng mục ở mọi nơi mục được xem (hồ sơ, thẻ thông tin khẩn cấp). *(Nguồn: BR-M01-07, BR-M07-06, BR-M08-02; Clarification 2026-09-25)*
- **FR-019**: Chỉ bác sĩ và điều dưỡng MUST được ghi nhận hoặc loại trừ mục sức khỏe; lệnh "Loại trừ" MUST có lý do; mục MUST NOT bị xóa hay sửa nội dung bởi bất kỳ ai. Sai nội dung được xử lý bằng loại trừ mục sai và ghi mục mới. *(Nguồn: BR-M01-07, DBR-04, UC-04, 4.4: BS T, ĐD T)*
- **FR-020**: Khi một mục dị ứng mới được ghi nhận, hệ thống MUST kích hoạt kiểm tra lại toàn bộ đơn thuốc đang hiệu lực (BR-M07-06, feature 006) và chế độ ăn, thực đơn đang phân bổ (BR-M08-02, feature 011) của người cao tuổi đó; mỗi xung đột tạo cảnh báo (feature 007). Việc kiểm tra MUST so khớp theo mục danh mục dị nguyên; mục loại "khác" không tham gia kiểm tra tự động (FR-018a). Khi một mục dị ứng chuyển Đã loại trừ, hệ thống MUST báo feature 011 để tính lại suất đặc biệt, đối chiếu lại đồ ăn gia đình và báo feature 007 nguồn của cảnh báo xung đột tương ứng đã được xử lý (feature 011 FR-021). Mục dị ứng loại "khác" mới được ghi MUST được báo cho feature 011 để đưa người đó vào danh sách cần đối chiếu khi phục vụ (feature 011 FR-036a). *(Nguồn: BR-M01-07; đồng bộ spec 011)*
- **FR-021**: Khi ghi một mục dị ứng cùng mục danh mục dị nguyên, hoặc một mục (bệnh nền, tiền sử, dị ứng "khác") có loại và nội dung trùng một mục đang Hiệu lực, hệ thống MUST cảnh báo trùng và yêu cầu xác nhận. *(Suy ra từ BR-M01-07: mỗi dị ứng là một mục riêng)*
- **FR-022**: Người thân MUST chỉ xem được dị ứng, bệnh nền và thông tin sức khỏe khác khi có bản đồng ý Hiệu lực có người thân đó trong phạm vi; khi xem, chỉ thấy mục đang Hiệu lực. *(Nguồn: BR-M01-08, DBR-03, 4.4 chú thích ¹)*
- **FR-023**: Các mục dị ứng Hiệu lực, bệnh nền Hiệu lực và phiên bản nguyện vọng cuối đời Hiệu lực (FR-023a) MUST sẵn có cho thẻ thông tin khẩn cấp (9.5) và cảnh báo nguy kịch (BR-M05-15) của feature 007.

#### C1. Nguyện vọng cuối đời *(bổ sung 2026-09-28)*

- **FR-023a**: Bác sĩ hoặc Điều dưỡng MUST ghi được phiếu nguyện vọng cuối đời cho người cao tuổi Đang tiếp nhận hoặc chưa ở trạng thái cuối, gồm: lựa chọn khi nguy kịch — (a) chuyển bệnh viện điều trị tích cực, (b) đưa về nhà, (c) ở lại viện chăm sóc giảm nhẹ; người cần liên hệ trước khi thực hiện; ghi chú; người trả lời (người cao tuổi hoặc người đại diện) và quan hệ; nhận định của Bác sĩ về việc người cao tuổi còn đủ năng lực hay không; người ký; thời điểm; bản ký được scan (bắt buộc); người ghi nhận. *(Nguồn: 5.2, UC-82, Q-213, Q-217)*
- **FR-023b**: Phiếu là dữ liệu có phiên bản. Mỗi người cao tuổi MUST có tối đa một phiên bản Hiệu lực (DBR-34). Ghi phiên bản mới MUST chuyển phiên bản cũ sang "Được thay thế" trong cùng lần; phiên bản đã lưu MUST NOT bị sửa hay xóa. Phiên bản mới được ghi theo yêu cầu của người cao tuổi hoặc người đại diện, hoặc khi feature 007 ghi kết quả xác nhận lại nguyện vọng lúc nguy kịch (BR-M05-16). *(Nguồn: 5.2, DBR-34)*
- **FR-023c**: Người ký MUST là người cao tuổi khi Bác sĩ nhận định người cao tuổi còn đủ năng lực; ngược lại MUST là một người đại diện có quan hệ Hiệu lực (feature 012). Khi ý kiến của người cao tuổi còn đủ năng lực và của gia đình khác nhau, lựa chọn được ghi theo ý người cao tuổi, ý kiến của gia đình ghi vào ghi chú. Mẫu phiếu do viện ban hành. *(Nguồn: 5.2, Q-216)*
- **FR-023d**: Chưa có phiếu Hiệu lực MUST NOT chặn "Hoàn tất tiếp nhận". Khi Hoàn tất tiếp nhận mà chưa có phiếu, hệ thống MUST hiển thị cảnh báo "chưa có nguyện vọng cuối đời" và từ đó nhắc Bác sĩ, Điều dưỡng phụ trách mỗi CFG-M01-06 (mặc định \[1 ngày\]) tới khi có phiếu, như với bản đồng ý (FR-012). *(Nguồn: 5.2, Q-217)*
- **FR-023e**: Nội dung phiếu là thông tin sức khỏe: Bác sĩ, Điều dưỡng, Trưởng tầng, Nhân viên chăm sóc xem trong phạm vi; người thân xem khi có bản đồng ý Hiệu lực bao gồm mình (FR-022); Hành chính và Kế toán chỉ thấy "đã có / chưa có nguyện vọng", không thấy nội dung. *(Nguồn: 19.3, Phụ lục 27 dòng "Nguyện vọng cuối đời", chú thích ³⁰)*
- **FR-023f**: Khi hồ sơ chuyển trạng thái cuối, phiên bản Hiệu lực giữ nguyên để tra cứu; việc nhắc ở FR-023d dừng. *(Suy ra từ BR-M01-05)*

#### D. Đánh giá đầu vào và đánh giá lại *(viết lại 2026-10-06, Q-280, Q-281)*

Mã FR giữ nguyên để các spec khác vẫn trích dẫn được; FR không còn áp dụng được ghi "Bỏ".

- **FR-024**: Hệ thống MUST NOT dùng thang điểm chuẩn, bảng quy đổi hay bất kỳ cơ chế tự đề xuất mức chăm sóc nào. Khi bác sĩ hoặc điều dưỡng mở một lần đánh giá, hệ thống MUST hiển thị kèm để tham khảo: hồ sơ sức khỏe ban đầu (FR-017), các mục sức khỏe Hiệu lực (FR-018) và các chỉ số đo gần nhất của người cao tuổi (feature 007). *(Nguồn: 5.3, Q-280)*
- **FR-025**: Mỗi lần đánh giá MUST ghi: loại (đầu vào / đánh giá lại); lý do hoặc căn cứ; thời điểm; người lập; **nhận xét đánh giá** (văn bản, bắt buộc): tình trạng chung, vận động, tự chăm sóc, nhận thức, các nguy cơ cần lưu ý; **lưu ý chăm sóc** (văn bản ngắn, không bắt buộc), dùng để hiện trên thẻ người cao tuổi; **mức chăm sóc** (chọn một trong ba mức ở mục 4, bắt buộc); **nhu cầu chăm sóc** (chọn không, một hoặc nhiều giá trị từ danh mục nhu cầu chăm sóc, mục 4; bổ sung 2026-10-06, Q-291); với đánh giá đầu vào: loại hình lưu trú đề xuất (không bắt buộc); người xác nhận và thời điểm xác nhận. *(Nguồn: 5.3, BR-M01-09, DBR-05)*
- **FR-026**: Bác sĩ và điều dưỡng MUST được tạo lần đánh giá và lập nội dung (Nháp); chỉ bác sĩ MUST được xác nhận. Mỗi người cao tuổi MUST có tối đa một lần đánh giá Nháp; người tạo bản nháp hoặc bác sĩ MUST "Hủy nháp" được kèm lý do, khi đó bản nháp chuyển Đã hủy, vẫn lưu và không sửa được (CHK004, BR-M01-13). *(Nguồn: 5.3, UC-05, UC-06, Phụ lục 27: BS T, D; ĐD T)*
- **FR-027**: *(Bỏ, Q-280)* Không tính điểm, không áp bảng quy đổi CFG-M01-05.
- **FR-028**: Bác sĩ MUST NOT xác nhận lần đánh giá khi thiếu nhận xét đánh giá hoặc mức chăm sóc. *(Nguồn: DBR-05)*
- **FR-029**: **(Sửa, 2026-10-06, Q-291)** Bác sĩ MUST chọn được đúng một trong ba mức chăm sóc (Chăm sóc cơ bản, Chăm sóc thường xuyên, Chăm sóc đặc biệt) và không, một hoặc nhiều nhu cầu chăm sóc. Vì không có mức đề xuất, hệ thống MUST NOT đòi lý do khi chọn. Nhu cầu chăm sóc không có trọng số, không tham gia điều kiện phân bổ phòng (feature 003) hay tỷ lệ phục vụ (feature 008); lần đánh giá lại chỉ đổi nhu cầu mà giữ nguyên mức MUST NOT dẫn tới báo lập yêu cầu thay đổi lưu trú (FR-036), nhưng điều dưỡng phụ trách vẫn được báo để xem xét kế hoạch chăm sóc (FR-043; **bổ sung, 2026-10-06, Q-296; sửa Q-306**). *(Nguồn: mục 4, BR-M01-09; Q-280, Q-291, Q-296, Q-306)*
- **FR-030**: *(Bỏ, Q-281)* Không có cờ nguy cơ. Feature khác (005, 007, 012, 013, 014) MUST NOT dựa vào cờ nguy cơ; nơi cần thông tin nguy cơ thì dùng phần lưu ý chăm sóc theo FR-038.
- **FR-031**: Khi bác sĩ xác nhận, hệ thống MUST NOT tạo bản nháp kế hoạch chăm sóc hay chép hoạt động nào vào kế hoạch; điều dưỡng lập kế hoạch bằng tay từ nhận xét đánh giá (8.1, feature 005). Với đánh giá lại, điều dưỡng phụ trách được báo xem xét kế hoạch theo FR-043. *(Nguồn: 8.1, BR-M01-09, Q-280, Q-306)*
- **FR-032**: Lần đánh giá sửa được khi còn Nháp; sau khi bác sĩ xác nhận, lần đánh giá là ghi nhận đã xác nhận, chỉ sửa bằng đính chính (feature 000). Đính chính MUST NOT tự thay đổi mức chăm sóc đã áp dụng; nếu bản đính chính ghi mức chăm sóc khác mức của lần đã xác nhận, hệ thống MUST báo bác sĩ cần đánh giá lại với căn cứ "đính chính đánh giá" (FR-040; **sửa, 2026-10-06, Q-307**). Đính chính nhận xét hoặc lưu ý chăm sóc MUST NOT báo đánh giá lại; phần lưu ý hiển thị theo bản đính chính. *(Nguồn: 1.5 nhóm 3, Q-307)*
- **FR-033**: Với đánh giá đầu vào, khi bác sĩ xác nhận, mức chăm sóc của lần đánh giá MUST trở thành mức chăm sóc hiện hành của người cao tuổi. Loại hình lưu trú đề xuất chỉ để tham khảo; loại hình lưu trú chính thức chỉ được xác định bởi hợp đồng (6.3, feature 004). **(Bổ sung, 2026-10-06, Q-300)** Mức chăm sóc của hợp đồng do gia đình chọn khi lập hợp đồng (feature 004 FR-025), có thể trước khi có đánh giá đầu vào; mức hiện hành vẫn lấy từ lần đánh giá này. Khi xác nhận đánh giá đầu vào, spec này MUST gửi feature 004 sự kiện kèm mức đã chọn để feature 004 báo Hành chính nếu mức khác mức của hợp đồng (feature 004 FR-025b). *(Nguồn: 5.3, BR-M01-09, BR-M02-22)*
- **FR-034**: Người cao tuổi MUST có tối đa một lần đánh giá đầu vào Đã xác nhận. *(Suy ra từ 5.3; đánh giá tiếp theo là đánh giá lại)*
- **FR-034a**: Kết quả đánh giá còn hiệu lực cho việc Hoàn tất tiếp nhận MUST là lần đánh giá Đã xác nhận gần nhất (đánh giá đầu vào hoặc đánh giá lại) có thời điểm xác nhận cách thời điểm thực hiện lệnh không quá CFG-M01-02 (mặc định \[90 ngày\]). Nếu quá hạn, lệnh "Hoàn tất tiếp nhận" MUST bị từ chối với điều kiện chưa thỏa "đánh giá đã quá hạn, cần đánh giá lại". *(Nguồn: 5.6 "đánh giá đầu vào hoàn tất", BR-M01-02, CFG-M01-02; Clarification 2026-09-25)*
- **FR-035**: Đánh giá lại MUST có cùng nội dung và cùng quy tắc xác nhận như đánh giá đầu vào (FR-024 → FR-032), và chỉ thực hiện được khi người cao tuổi ở trạng thái Đang lưu trú, Tạm vắng, Hoạt động bên ngoài, hoặc Đang tiếp nhận khi đã có đánh giá đầu vào Đã xác nhận. Với đánh giá lại ở trạng thái Đang tiếp nhận, mức chăm sóc được xác nhận MUST thay mức chăm sóc hiện hành ngay như FR-033 (chưa có lưu trú nên không cần yêu cầu thay đổi lưu trú); nếu đã có hợp đồng mang mức chăm sóc khác, việc điều chỉnh hợp đồng thuộc feature 004. *(Nguồn: 5.4, UC-06; Clarification 2026-09-25)*
- **FR-036**: **(Sửa, 2026-10-06, Q-306)** Khi đánh giá lại được xác nhận với mức chăm sóc khác mức hiện hành, hệ thống MUST báo Hành chính kèm mức mới và tham chiếu lần đánh giá để Hành chính lập yêu cầu thay đổi lưu trú loại "đổi mức chăm sóc" (vòng đời feature 000, loại yêu cầu thuộc feature 004); hệ thống MUST NOT tự tạo yêu cầu. Chưa có yêu cầu đổi mức chưa kết thúc cho người đó thì hệ thống MUST nhắc Hành chính mỗi CFG-M03-02 (mặc định \[1 ngày\]), kể cả sau khi yêu cầu trước bị Từ chối, tới khi có yêu cầu mới hoặc lần đánh giá lại mới (Q-318). Mức chăm sóc hiện hành và kế hoạch chăm sóc cũ MUST giữ nguyên tới ngày hiệu lực của thay đổi. Người duyệt là Quản lý viện theo Phụ lục 27 dòng "Yêu cầu thay đổi lưu trú" (feature 004) (CHK005). *(Nguồn: BR-M01-03, 5.4, Q-306)*
- **FR-036a**: Khi bác sĩ xác nhận một lần đánh giá lại mà người cao tuổi đang có yêu cầu đổi mức chăm sóc ở Nháp, Chờ duyệt hoặc Đã duyệt (chờ hiệu lực), hệ thống MUST chuyển yêu cầu cũ sang trạng thái kết thúc "Được thay thế", trong cùng lần thực hiện với việc xác nhận, ghi tham chiếu tới lần đánh giá mới, người thực hiện là hệ thống (tác động kèm lệnh xác nhận của bác sĩ); yêu cầu Được thay thế MUST NOT tạo tác động nào. Sau đó: nếu mức mới khác mức hiện hành thì báo Hành chính theo FR-036; nếu mức mới trùng mức hiện hành thì không báo. Tại mọi thời điểm, mỗi người cao tuổi MUST có tối đa một yêu cầu đổi mức chăm sóc chưa kết thúc. Người yêu cầu và người duyệt của yêu cầu cũ MUST được báo (feature 009). *(Nguồn: BR-M01-03; Clarification 2026-09-25)*
- **FR-037**: *(Bỏ, Q-281)* Không có cờ nguy cơ nên không có quy tắc gỡ cờ.
- **FR-038**: Phần lưu ý chăm sóc của lần đánh giá Đã xác nhận gần nhất (kể cả theo bản đính chính) MUST sẵn có cho thẻ người cao tuổi trong checklist ca (feature 005) và thẻ thông tin khẩn cấp (feature 007). Lần đánh giá mới hơn để trống phần này thì thẻ không hiện lưu ý; hệ thống MUST NOT lấy lưu ý từ lần cũ hơn. Spec này MUST NOT cung cấp cờ hay sự kiện gắn, gỡ cờ cho feature 012, 013, 014. *(Nguồn: BR-M01-10, Q-281)*

#### E. Nhắc đánh giá lại *(viết lại 2026-10-06, Q-307)*

Hệ thống không tạo bản ghi "yêu cầu đánh giá lại". "Cần đánh giá lại" là trạng thái tính được: người cao tuổi có ít nhất một căn cứ đã được báo mà sau thời điểm báo chưa có lần đánh giá lại Đã xác nhận.


- **FR-039**: Bộ lập lịch hệ thống MUST báo bác sĩ cần đánh giá lại (căn cứ "định kỳ") cho người cao tuổi Đang lưu trú, Tạm vắng, Hoạt động bên ngoài hoặc Điều trị tại bệnh viện khi lần đánh giá Đã xác nhận gần nhất đã qua CFG-M01-02 (mặc định \[90 ngày\]). Mốc tính là thời điểm bác sĩ xác nhận lần đánh giá đó; khoảng được tính theo ngày lịch, múi giờ theo feature 000 (CHK014). *(Nguồn: BR-M01-02, UC-07)*
- **FR-040**: Hệ thống MUST báo bác sĩ cần đánh giá lại khi: feature 007 ghi nhận sự cố ngã hoặc sự cố mức trung bình trở lên; lệnh "Ghi nhận trở về" từ Điều trị tại bệnh viện được thực hiện; FR-032 phát hiện kết quả sau đính chính khác. Thông báo ghi căn cứ và tham chiếu bản ghi nguồn. Khi tình trạng thay đổi đáng kể hoặc có yêu cầu chuyên môn, bác sĩ, điều dưỡng tự tạo lần đánh giá lại (FR-026); không có lệnh tạo yêu cầu. *(Nguồn: BR-M01-02, 5.4, 5.6, Q-307)*
- **FR-041**: Hạn đánh giá lại là CFG-M01-03 (mặc định \[48 giờ\]) tính từ lần báo sớm nhất chưa được đáp ứng; riêng người cao tuổi đang Điều trị tại bệnh viện, hạn tính từ lúc Ghi nhận trở về. Quá hạn mà chưa có đánh giá lại Đã xác nhận, hệ thống MUST cảnh báo bác sĩ. Từ lúc quá hạn tới khi có lần đánh giá lại Đã xác nhận, người cao tuổi MUST mang trạng thái tính được "quá hạn đánh giá lại" kèm thời điểm bắt đầu quá hạn, và dấu này MUST được cung cấp cho báo cáo sức khỏe (feature 016 FR-044). *(Nguồn: BR-M01-02; 18.3)*
- **FR-042**: Căn cứ mới phát sinh khi người cao tuổi đang "cần đánh giá lại" MUST được báo thêm cho bác sĩ, không đặt lại hạn; hạn vẫn theo lần báo sớm nhất chưa được đáp ứng (FR-041). *(Suy ra từ BR-M01-02; sửa 2026-10-06, Q-307)*
- **FR-043**: Khi đánh giá lại được bác sĩ xác nhận, mọi căn cứ đã báo trước thời điểm xác nhận MUST được coi là đã đáp ứng (người cao tuổi hết "cần đánh giá lại"). Khi mức chăm sóc hoặc tập nhu cầu chăm sóc của lần này khác lần đánh giá Đã xác nhận trước đó (**sửa, 2026-10-06, Q-296:** kể cả chỉ đổi nhu cầu), spec này MUST gửi feature 005 sự kiện để điều dưỡng phụ trách được báo xem xét kế hoạch chăm sóc (BR-M04-20); hệ thống MUST NOT tạo yêu cầu xem xét (**sửa, 2026-10-06, Q-306**). Khi người cao tuổi chuyển sang trạng thái cuối, hệ thống MUST dừng nhắc và cảnh báo quá hạn đánh giá lại. *(Nguồn: BR-M01-02, BR-M04-20, Q-306, Q-307)*

#### F. Trạng thái người cao tuổi và lệnh nghiệp vụ

- **FR-044**: Hệ thống MUST chỉ cho phép các chuyển trạng thái trong bảng dưới đây; MUST NOT có thao tác "sửa trạng thái". *(Nguồn: 5.5, BR-M01-04, BR-M01-06)*
- **FR-045**: Mỗi lệnh MUST kiểm tra toàn bộ điều kiện chặn trước khi chuyển; nếu có điều kiện chưa thỏa, hệ thống MUST từ chối, liệt kê mọi điều kiện chưa thỏa trong một lần, và giữ nguyên trạng thái. *(Nguồn: 5.6, BR-M01-06)*
- **FR-046**: Mỗi lần chuyển trạng thái MUST ghi lịch sử: trạng thái từ, trạng thái đến, lệnh, thời điểm, người thực hiện (hoặc Bộ lập lịch hệ thống), lý do, căn cứ (ví dụ sự cố, chuyến đi, yêu cầu ngoại lệ). Lý do MUST bắt buộc với mọi lệnh chuyển trạng thái do người dùng thực hiện; lệnh Tạo hồ sơ không có trạng thái "từ" nên không bắt lý do (Q-320). *(Nguồn: BR-M01-04, 3.2 LICH_SU_TRANG_THAI)*
- **FR-047**: Khi chuyển trạng thái thành công, hệ thống MUST kích hoạt các tác động ở cột "Tác động" cho feature sở hữu, trong cùng một lần thực hiện với việc chuyển trạng thái: hoặc chuyển trạng thái và kích hoạt tác động cùng thành công, hoặc không có gì thay đổi. Với tác động có độ trễ theo tham số (khóa tài khoản người thân sau CFG-M01-04), phần thuộc "cùng một lần thực hiện" là việc lên lịch tác động đó; việc thực hiện khi tới hạn do Bộ lập lịch hệ thống làm (FR-049) (CHK022). *(Nguồn: 5.6; nguyên tắc "hoặc toàn bộ, hoặc không" của feature 000)*
- **FR-047a**: "Cho tạm vắng" và "Ghi nhận trở về" MUST được hành chính hoặc trưởng tầng thực hiện trực tiếp, không qua yêu cầu phê duyệt. Quyền duyệt (D) của Quản lý viện ở dòng "Tạm vắng, trở về" của Permission Matrix MUST chỉ áp dụng cho quyết định giữ tiếp hay giải phóng giường khi thời gian vắng vượt ngưỡng giữ giường (BR-M02-07, feature 004). *(Nguồn: 5.6, 4.4; Clarification 2026-09-25)*
- **FR-047b**: Lệnh "Ghi nhận qua đời" MUST do bác sĩ thực hiện khi người cao tuổi đang ở trạng thái Đang lưu trú (mất tại viện). Khi trạng thái hiện tại là Tạm vắng, Hoạt động bên ngoài hoặc Điều trị tại bệnh viện (mất ngoài viện), bác sĩ hoặc hành chính MAY thực hiện; nếu hành chính thực hiện, lệnh MUST kèm bản scan giấy tờ bằng chứng do cơ sở y tế hoặc cơ quan có thẩm quyền cấp (giấy báo tử, giấy chứng tử), thiếu bằng chứng thì bị từ chối. Giấy tờ này là xác nhận chuyên môn về tử vong theo tài liệu gốc (bổ sung câu, 2026-10-11, Q-342). *(Nguồn: 5.6 "có người xác nhận", 6.8, UC-18, 4.4 dòng "Kết thúc lưu trú, qua đời": BS T, HC T; Clarification 2026-09-25)*
- **FR-048**: Khi hai lệnh được gửi gần như đồng thời trên cùng một người cao tuổi, chỉ lệnh đầu tiên MUST được áp dụng; lệnh sau MUST được kiểm tra lại trên trạng thái mới. *(Suy ra từ BR-M01-04, BR-M01-06; quy tắc "lệnh được ghi nhận trước thắng" của feature 000)*
- **FR-049**: Hồ sơ ở trạng thái cuối (Kết thúc lưu trú, Qua đời, Hủy tiếp nhận) MUST chỉ được xem theo quyền; mọi lệnh và mọi thao tác ghi trên hồ sơ, bản đồng ý, mục sức khỏe, đánh giá của người đó MUST bị chặn, trừ hai ngoại lệ do người dùng thực hiện — lệnh "Rút lại đồng ý" (FR-014a) và đính chính theo FR-049a — và các thao tác của Bộ lập lịch hệ thống được quy định ở feature 000 (lưu giữ) và 5.6 (khóa tài khoản người thân). *(Nguồn: BR-M01-05, 5.5)*
- **FR-049a**: Bản ghi đã xác nhận thuộc hồ sơ ở trạng thái cuối (lần đánh giá, hồ sơ sức khỏe ban đầu, lịch sử trạng thái) MUST chỉ được đính chính dưới dạng yêu cầu phê duyệt loại "Đính chính" (vòng đời feature 000), do người được đính chính theo feature 000 FR-026 lập; bản đính chính MUST chỉ được lưu và có hiệu lực khi Quản lý viện duyệt, kể cả với bản ghi gắn tầng mà khi hồ sơ còn hoạt động được đính chính trực tiếp. Đính chính trên hồ sơ trạng thái cuối MUST NOT kích hoạt báo đánh giá lại (FR-032) hay bất kỳ tác động nào lên trạng thái, mức chăm sóc. *(Nguồn: BR-M01-05, 1.3, feature 000 FR-023 → FR-026; Clarification 2026-09-25)*
- **FR-050**: Khi người cao tuổi Tạm vắng quá thời điểm dự kiến trở lại cộng CFG-M01-01 (mặc định \[2 giờ\]) mà chưa có "Ghi nhận trở về", hệ thống MUST cảnh báo trưởng tầng và hành chính một lần cho mỗi mốc dự kiến trở lại. *(Nguồn: BR-M01-01)*

**Bảng trạng thái người cao tuổi** *(5.5, 5.6, BR-M01-06; quyền theo 4.4)*:

| Trạng thái hiện tại | Lệnh | Trạng thái kế tiếp | Ai thực hiện | Điều kiện (chặn nếu không đạt) | Tác động (feature thực hiện) |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Tạo hồ sơ | Đang tiếp nhận | Hành chính | FR-001, FR-003 | — |
| Đang tiếp nhận | Hoàn tất tiếp nhận | Đang lưu trú | Hành chính | Đánh giá đầu vào Đã xác nhận và lần đánh giá gần nhất chưa quá CFG-M01-02 (001, FR-034a); hợp đồng Hiệu lực và đặt cọc đạt (004); mức chăm sóc của hợp đồng Hiệu lực trùng mức chăm sóc hiện hành (004 FR-004 (e), BR-M02-22, Q-300); thời điểm hiệu lực của lệnh không sớm hơn ngày bắt đầu hợp đồng Hiệu lực (004 FR-075, Q-22); nếu nội trú: đã có phân bổ giường và giường đặt trước đã về Trống, hoặc người thực hiện chuyển phân bổ sang một giường Trống khác ngay trong lệnh (003 FR-023, Q-50); đủ DBR-02 — đúng một người liên hệ chính và ít nhất một người đại diện (012 FR-003; 004 FR-004 (f), Q-343). Bản đồng ý không chặn (FR-012) | Lịch cá nhân, lịch thuốc, suất ăn được tính từ thời điểm thực hiện lệnh, là thời điểm bắt đầu lưu trú thực tế (005, 006, 011; CHK013); Người phụ trách ca được báo để lập công việc (005, Q-306); nếu chưa có bản đồng ý Hiệu lực: cảnh báo và bắt đầu nhắc theo CFG-M01-06 (001) |
| Đang tiếp nhận | Hủy tiếp nhận | Hủy tiếp nhận | Hành chính | Có lý do | Hủy giữ chỗ giường nếu có (003); đóng hồ sơ chờ liên quan (004) |
| Đang lưu trú | Cho tạm vắng | Tạm vắng | Hành chính, Trưởng tầng | Có bản ghi đón hợp lệ: người đón thuộc danh sách được phép đón hoặc có ngoại lệ đón Hiệu lực (14.3, BR-M10-03, 012 FR-026) | Hủy công việc, suất ăn trong thời gian vắng (005, 011); liều chuyển "Mang theo" hoặc "Tạm dừng" (006); giường Giữ chỗ hoặc Trống theo chính sách (003); áp chính sách phí vắng BR-M02-06 (004, 010) |
| Đang lưu trú | Điểm danh rời viện (chuyến đi) | Hoạt động bên ngoài | Hệ thống, khi trưởng đoàn (hoặc trưởng tầng thay) điểm danh rời viện (014 FR-040) | Người cao tuổi có trong danh sách chuyến đi, được đánh giá Đạt, đang Đang lưu trú (8.9) | Hủy công việc Chưa đến hạn trong khoảng đi (005 FR-020, BR-M04-04); liều chuyển "Mang theo" (006) (BR-M04-16) |
| Đang lưu trú, Tạm vắng, Hoạt động bên ngoài | Chuyển viện | Điều trị tại bệnh viện | Điều dưỡng, Bác sĩ (UC-16, BR-M01-06; CHK024) | Có sự cố ghi ở feature 007, hoặc chỉ định chuyển viện của Bác sĩ ghi trong lý do của lệnh; khi người cao tuổi đang Tạm vắng, căn cứ là thông tin gia đình hoặc cơ sở y tế báo, ghi trong lý do (CHK016) | Như Cho tạm vắng; thông báo người liên hệ chính mức Khẩn cấp (009, Q-97) |
| Tạm vắng | Ghi nhận trở về | Đang lưu trú | Hành chính, Trưởng tầng | — | Lịch thuốc, suất ăn được tính lại từ thời điểm trở về (006, 011); Người phụ trách ca được báo để lập công việc (005, Q-306) |
| Hoạt động bên ngoài | Điểm danh về (chuyến đi), hoặc Hủy ghi nhận điểm danh rời viện (014 FR-031a) | Đang lưu trú | Hệ thống, khi trưởng đoàn hoặc trưởng tầng điểm danh về, hoặc trưởng tầng đính chính (014) | Người cao tuổi được điểm danh về, hoặc bản ghi rời viện bị hủy ghi nhận khi chuyến chưa về | Người phụ trách ca được báo để lập lại công việc từ lúc trở về (005 FR-020, Q-306); tính lại liều Mang theo (006) |
| Điều trị tại bệnh viện | Ghi nhận trở về | Đang lưu trú | Hành chính, Trưởng tầng | — | Tạm dừng toàn bộ lịch thuốc cũ, báo cần lập phiếu đối chiếu thuốc BR-M07-09 (006); báo bác sĩ cần đánh giá lại (001, FR-040) |
| Đang lưu trú, Tạm vắng, Điều trị tại bệnh viện | Kết thúc lưu trú | Kết thúc lưu trú | Hành chính, Bác sĩ; Quản lý viện duyệt ngoại lệ | Không còn đồ gửi ở Đang giữ, Đang được sử dụng hoặc Hư hỏng (013, Q-148); không còn thuốc gửi đang giữ (006); chi phí đã chốt (010); không còn cảnh báo/sự cố mở (007); đã bàn giao người cao tuổi theo quy trình đón (004, 012); số dư và tiền cọc đã quyết toán (017, 15.9) — trừ khi có yêu cầu ngoại lệ đã được Quản lý viện duyệt (vòng đời 000) | Hủy mọi lịch tương lai (005, 006, 011); giải phóng giường (003); khóa tài khoản người thân sau CFG-M01-04, mặc định \[30 ngày\] (012) |
| Đang lưu trú, Tạm vắng, Hoạt động bên ngoài, Điều trị tại bệnh viện | Ghi nhận qua đời | Qua đời | Bác sĩ; Hành chính chỉ khi trạng thái hiện tại là Tạm vắng, Hoạt động bên ngoài hoặc Điều trị tại bệnh viện (FR-047b) | Có người xác nhận: bác sĩ thực hiện lệnh, hoặc giấy tờ bằng chứng (giấy báo tử, giấy tờ của cơ sở y tế) khi hành chính thực hiện | Như Kết thúc lưu trú; thông báo người liên hệ chính mức Khẩn cấp (009, Q-97); hồ sơ chỉ đọc |

Kết thúc lưu trú, Qua đời, Hủy tiếp nhận là trạng thái cuối; không có lệnh nào đưa hồ sơ ra khỏi các trạng thái này. Nội dung chi tiết của từng lệnh (ví dụ thời gian dự kiến trở lại, người bàn giao, thông tin qua đời ở 6.8) thuộc feature 004; spec này quy định tập trạng thái, chuyển hợp lệ, điều kiện chặn và lịch sử.

**Bảng trạng thái các thực thể khác của feature** *(constitution IV; bổ sung 2026-10-01, CHK001)*:

| Thực thể | Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái kế tiếp | Ai thực hiện | Điều kiện (chặn nếu không đạt) | Tác động |
| --- | --- | --- | --- | --- | --- | --- |
| Bản đồng ý | (chưa có) | Ghi nhận | Hiệu lực | Hành chính | Hồ sơ chưa ở trạng thái cuối; có bản scan; FR-011 nếu người đại diện ký | Bản Hiệu lực cũ chuyển Đã rút lại (FR-013) |
| Bản đồng ý | Hiệu lực | Rút lại đồng ý; hoặc được thay thế | Đã rút lại (trạng thái cuối) | Hành chính; hệ thống khi có bản mới | Lý do và bằng chứng (FR-014) | Người thân ngoài phạm vi mất quyền xem sức khỏe (FR-015) |
| Mục sức khỏe | (chưa có) | Ghi nhận | Hiệu lực | Bác sĩ, Điều dưỡng | Hồ sơ chưa ở trạng thái cuối | Với dị ứng: kiểm tra đơn thuốc, chế độ ăn (FR-020) |
| Mục sức khỏe | Hiệu lực | Loại trừ | Đã loại trừ (trạng thái cuối) | Bác sĩ, Điều dưỡng | Có lý do | Báo feature 011, 007 (FR-020) |
| Hồ sơ sức khỏe ban đầu | Nháp | Xác nhận | Đã xác nhận | Bác sĩ | — | Thành nhóm 3, chỉ đính chính (FR-017) |
| Lần đánh giá | (chưa có) | Tạo | Nháp | Bác sĩ, Điều dưỡng | Không có lần Nháp khác của cùng người; trạng thái người cao tuổi theo FR-035 | — |
| Lần đánh giá | Nháp | Xác nhận | Đã xác nhận (trạng thái cuối) | Bác sĩ | Có nhận xét đánh giá và mức chăm sóc (FR-028, DBR-05) | Mức chăm sóc hiện hành, hoặc báo Hành chính lập yêu cầu thay đổi lưu trú (FR-033, FR-036); lưu ý chăm sóc lên thẻ người cao tuổi (FR-038); hết "cần đánh giá lại" (FR-043). **(Sửa 2026-10-06, Q-280, Q-281)** Không gắn cờ, không chép hoạt động mẫu |
| Lần đánh giá | Nháp | Hủy nháp | Đã hủy (trạng thái cuối) | Người tạo, Bác sĩ | Có lý do | — |
| Phiếu nguyện vọng cuối đời | (chưa có), Hiệu lực | Ghi phiên bản mới | Hiệu lực; phiên bản trước chuyển Được thay thế (trạng thái cuối) | Bác sĩ, Điều dưỡng | FR-023a, FR-023c | Dừng nhắc FR-023d |

#### G. Loại hình lưu trú và mức chăm sóc

- **FR-051**: Loại hình lưu trú và mức chăm sóc MUST là hai thuộc tính độc lập của người cao tuổi: mọi tổ hợp giữa ba loại hình lưu trú (3.1–3.3) và các mức chăm sóc trong danh mục (mục 4) MUST được chấp nhận; thay đổi thuộc tính này MUST NOT tự động thay đổi thuộc tính kia. Tính độc lập này chỉ ở cấp thuộc tính của người cao tuổi; ràng buộc phòng phải cho phép mức chăm sóc (BR-M03-01) và trọng số chăm sóc thuộc feature 003, 008 (CHK027). *(Nguồn: 1.3, mục 4)*
- **FR-052**: Loại hình lưu trú hiện hành MUST lấy từ hợp đồng Hiệu lực (kể cả phụ lục đã áp dụng, BR-M02-09); mức chăm sóc hiện hành MUST lấy từ đánh giá đầu vào đã xác nhận (FR-033), sau đó chỉ thay đổi khi một yêu cầu thay đổi lưu trú đổi mức chăm sóc được áp dụng (FR-036, feature 004). Không có thao tác sửa trực tiếp hai thuộc tính này. Khi chưa có hợp đồng Hiệu lực (Đang tiếp nhận), loại hình lưu trú hiện hành để trống; điều kiện "nếu nội trú" của Hoàn tất tiếp nhận lấy loại hình từ hợp đồng Hiệu lực mà lệnh này đòi phải có (CHK019). *(Nguồn: 5.3, 6.3, 6.6, BR-M01-03)*
- **FR-053**: Lịch sử của từng thuộc tính MUST cho biết giá trị hiện hành tại một ngày bất kỳ và căn cứ thay đổi (lần đánh giá, hợp đồng, yêu cầu thay đổi lưu trú).
- **FR-054**: *(Bổ sung, 2026-10-10, Q-311)* Mức của các thông báo spec này chưa nêu mức (feature 009 FR-043a, FR-043b): **Nhẹ** — báo bác sĩ cần đánh giá lại (FR-039, FR-040), nhắc thiếu bản đồng ý (CFG-M01-06), nhắc thiếu nguyện vọng cuối đời, nhắc Hành chính lập yêu cầu đổi mức (FR-036); **Trung bình** — quá hạn đánh giá lại (FR-041), tạm vắng quá hạn trở lại (CFG-M01-01), báo Hành chính lần đầu khi mức chăm sóc đổi. Người nhận "bác sĩ" là Bác sĩ trực theo phân công ca (feature 008); không có Bác sĩ trực thì mọi Bác sĩ. *(Nguồn: Q-311, BR-M01-02, 17.1)*

**Truy vết use case** *(constitution I; bổ sung 2026-10-01, CHK048)*: mục A (FR-001 → FR-009) — UC-01, UC-02; mục B (FR-010 → FR-016) — UC-03; mục C (FR-017 → FR-023) — UC-04; mục C1 (FR-023a → FR-023f) — UC-82; mục D (FR-024 → FR-038) — UC-05, UC-06; mục E (FR-039 → FR-043) — UC-07 (Nhắc đánh giá lại); mục F (FR-044 → FR-050) — UC-08, cùng UC-15 → UC-18 của feature 004 cho nội dung chi tiết của từng lệnh; mục G (FR-051 → FR-053) — không có UC riêng, là ràng buộc của 1.3 và mục 4.

### Key Entities *(include if feature involves data)*

- **Người cao tuổi (NGUOI_CAO_TUOI)** – nhóm 2: mã hồ sơ, hồ sơ trước (liên kết tới hồ sơ ở trạng thái cuối của cùng người, nếu có), họ tên, ngày sinh, giới tính, giấy tờ định danh, ảnh, địa chỉ thường trú, địa chỉ liên hệ, liên hệ, thông tin đặc biệt, trạng thái hiện tại, loại hình lưu trú hiện hành, mức chăm sóc hiện hành.
- **Lịch sử trạng thái (LICH_SU_TRANG_THAI)** – nhóm 3: trạng thái từ, đến, lệnh, thời điểm, người thực hiện, lý do, căn cứ.
- **Bản đồng ý (BAN_DONG_Y)** – nhóm 2: người đồng ý, là người đại diện hay không, lý do đại diện, phạm vi (người thân được xem sức khỏe, hình ảnh, thông báo), thời điểm, bằng chứng, trạng thái, bản thay thế.
- **Hồ sơ sức khỏe ban đầu** – nhóm 3 sau xác nhận: các thông tin ở FR-017, người ghi, người xác nhận.
- **Phiếu nguyện vọng cuối đời (NGUYEN_VONG_CUOI_DOI)** – nhóm 2, có phiên bản: các trường ở FR-023a, trạng thái (Hiệu lực / Được thay thế), phiên bản thay thế, nguồn (tiếp nhận, yêu cầu của gia đình, xác nhận lại lúc nguy kịch).
- **Danh mục dị nguyên** – nhóm 1: tên, nhóm (thuốc/hoạt chất, thực phẩm, khác), liên kết tới hoạt chất (feature 006) hoặc thành phần món ăn (feature 011).
- **Mục sức khỏe (MUC_SUC_KHOE)** – nhóm 2: loại, nội dung (với dị ứng: mục danh mục dị nguyên, hoặc văn bản kèm dấu "không kiểm tra tự động"), mức độ, nguồn, người ghi, ngày ghi, trạng thái, lý do và người loại trừ.
- **Lần đánh giá (DANH_GIA)** – nhóm 3 sau xác nhận: loại, căn cứ, thời điểm, người lập, nhận xét đánh giá, lưu ý chăm sóc, mức chăm sóc, nhu cầu chăm sóc (nhiều giá trị, Q-291), loại hình lưu trú đề xuất, người xác nhận, thời điểm xác nhận, trạng thái (Nháp / Đã xác nhận / Đã hủy). *(Sửa 2026-10-06, Q-280, Q-281: bỏ thực thể Thang điểm, Kết quả thang điểm, Cờ nguy cơ.)*
- **Nhu cầu chăm sóc** *(bổ sung 2026-10-06, Q-291)* – danh mục nhóm 1: tên, trạng thái; khởi tạo gồm Hỗ trợ vận động, Theo dõi sức khỏe thường xuyên, Phục hồi sau tai biến, Phục hồi chức năng khác. Nhu cầu hiện hành của người cao tuổi lấy từ lần đánh giá Đã xác nhận gần nhất.
- **Yêu cầu đánh giá lại** – *(Bỏ, 2026-10-06, Q-307)*. Căn cứ đánh giá lại nằm trong thông báo gửi bác sĩ (feature 009); "cần đánh giá lại", "quá hạn đánh giá lại" được tính từ thời điểm báo và lần đánh giá Đã xác nhận (FR-041, FR-043).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% lần chuyển trạng thái người cao tuổi trong đợt kiểm thử thuộc bảng 5.5 và có bản ghi lịch sử đủ trạng thái từ/đến, lệnh, người, thời điểm, lý do; 0 chuyển trạng thái ngoài bảng được chấp nhận.
- **SC-002**: Với mỗi lệnh có điều kiện chặn, 100% lần thực hiện khi thiếu một hoặc nhiều điều kiện bị từ chối và người dùng thấy đầy đủ danh sách điều kiện chưa thỏa ngay trong lần từ chối đầu tiên.
- **SC-003**: 0 mục dị ứng, bệnh nền, tiền sử bệnh bị mất hoặc đổi nội dung sau khi ghi, trong mọi kịch bản kiểm thử kể cả với tài khoản quản lý viện.
- **SC-004** *(sửa 2026-10-06, Q-280, Q-281)*: 100% lần đánh giá Đã xác nhận có nhận xét đánh giá và mức chăm sóc; 0 lần hệ thống tự gán mức chăm sóc, cờ, hay tự tạo hoạt động trong kế hoạch chăm sóc.
- **SC-005**: Bác sĩ hoàn tất một lần đánh giá đầu vào (từ lúc mở đến lúc xác nhận, không kể thời gian khám; **sửa 2026-10-06, Q-280:** không còn 4 thang) trong không quá 10 phút; đo trên ít nhất 10 lần của bác sĩ đã được hướng dẫn, lấy trung vị (CHK032).
- **SC-006**: Hành chính tạo xong một hồ sơ người cao tuổi đầy đủ thông tin trong không quá 5 phút, không cần sổ giấy; đo trên ít nhất 10 lần của hành chính đã được hướng dẫn, lấy trung vị, có tính thời gian tải ảnh và bản scan (CHK032).
- **SC-007** *(sửa 2026-10-06, Q-307)*: 100% căn cứ ở BR-M01-02 được báo cho bác sĩ đúng lúc trong đợt kiểm thử với đồng hồ giả lập; 100% trường hợp quá CFG-M01-03 kể từ lúc báo mà chưa có đánh giá lại có cảnh báo bác sĩ; 0 bản ghi yêu cầu đánh giá lại hay yêu cầu thay đổi lưu trú do hệ thống tạo.
- **SC-008**: Sau khi bản đồng ý bị rút lại, 0 lần người thân ngoài phạm vi còn xem được thông tin sức khỏe ở lần truy cập kế tiếp.
- **SC-009**: 100% lần thêm dị ứng mới chọn từ danh mục kích hoạt kiểm tra đơn thuốc và chế độ ăn; mọi xung đột có sẵn trong dữ liệu kiểm thử đều sinh cảnh báo; 100% mục dị ứng loại "khác" mang dấu "không kiểm tra tự động" ở mọi nơi hiển thị.
- **SC-010**: Mọi tổ hợp loại hình lưu trú × mức chăm sóc trong danh mục đều ghi nhận được cho một người cao tuổi; đổi một thuộc tính không làm thay đổi thuộc tính còn lại trong 100% ca kiểm thử.
- **SC-011**: 0 người cao tuổi có hai phiên bản nguyện vọng Hiệu lực cùng lúc; 100% người Hoàn tất tiếp nhận mà chưa có phiếu được nhắc theo CFG-M01-06 tới khi có phiếu; 0 lần Hành chính, Kế toán thấy nội dung phiếu.
- **SC-012** *(bổ sung, CHK030)*: Với mỗi vai trò ở Phụ lục 27 dòng "Hồ sơ người cao tuổi", "Dị ứng, bệnh nền" và "Đánh giá, mức chăm sóc", 100% ca kiểm thử cho kết quả đúng ma trận: 0 lần nhân viên bếp mở được hồ sơ; 0 lần nhân viên chăm sóc thấy nhận xét đánh giá (vẫn thấy lưu ý chăm sóc). *(Sửa 2026-10-06, Q-282, Q-283.)*
- **SC-013** *(bổ sung, CHK031)*: Với đồng hồ giả lập, 100% trường hợp Tạm vắng quá mốc dự kiến cộng CFG-M01-01 có đúng một cảnh báo cho mỗi mốc (FR-050); 100% hồ sơ Đang lưu trú chưa có bản đồng ý Hiệu lực được nhắc theo CFG-M01-06 và việc nhắc dừng khi có bản đồng ý (FR-012).

## Assumptions

- Số feature `001` theo bản đồ feature ở mục 4.2 `docs/phan-tich-yeu-cau.md` (UC-01 → UC-08) và trùng số tuần tự kế tiếp.
- Quy tắc dùng chung (nhóm dữ liệu, đính chính, yêu cầu phê duyệt, nhật ký, tham số) theo feature 000 và không lặp lại ở đây.
- Họ tên, ngày sinh, giới tính là trường bắt buộc tối thiểu của hồ sơ cá nhân; giới tính cần cho ràng buộc giới tính của phòng (BR-M02-01, BR-M03-01).
- *(Sửa 2026-10-06, Q-280)* Nhận xét đánh giá là văn bản tự do; hệ thống không bắt cấu trúc theo mục. Mẫu nhận xét (nếu có) do viện ban hành ngoài hệ thống; nội dung bắt buộc của nhận xét thuộc điểm đối chiếu với bác sĩ của cơ sở ở mục 24.4.
- *(Sửa 2026-10-06, Q-280, Q-291)* Ba mức chăm sóc và mọi nhu cầu chăm sóc đều được bác sĩ chọn trực tiếp, không cần lý do; không còn "mức chuyên biệt".
- Mỗi người cao tuổi có tối đa một bản đồng ý Hiệu lực; đổi phạm vi bằng bản mới thay thế (mục 5.1 chỉ có hai trạng thái Hiệu lực / Đã rút lại).
- *(Bỏ, 2026-10-10, Q-307)* Không còn yêu cầu đánh giá lại; hệ thống chỉ báo bác sĩ, căn cứ mới được báo thêm (FR-039 → FR-043).
- Hồ sơ sức khỏe ban đầu được bác sĩ xác nhận; sau xác nhận là nhóm 3. Tình trạng thay đổi sau đó được phản ánh qua đánh giá lại và mục sức khỏe, không sửa hồ sơ ban đầu.
- Nội dung chi tiết của lệnh Ghi nhận qua đời (thời điểm, địa điểm, người phát hiện, nguyên nhân) thuộc feature 004.
- Ghi nhận trở về từ Tạm vắng không có điều kiện chặn (5.6 không nêu); việc khôi phục công việc, liều, suất ăn do feature sở hữu quy định.
- Hai chuyển do điểm danh chuyến đi (vào/ra Hoạt động bên ngoài) do hệ thống thực hiện khi trưởng đoàn điểm danh ở feature 014, người thực hiện ghi là người điểm danh.
- Việc ghi nhật ký lượt người thân xem hồ sơ sức khỏe (NFR-08) thuộc feature 012. Lượt nhân viên xem dữ liệu sức khỏe không được ghi nhật ký ở giai đoạn này (Q-03 đã chốt theo mặc định; cần tư vấn pháp lý đối chiếu trước khi triển khai, mục 24.4) (CHK041).
- Mọi hạn theo CFG-M01-01, CFG-M01-02, CFG-M01-03, CFG-M01-06 tính theo múi giờ và đồng hồ máy chủ của feature 000 (DBR-25, NFR-09) và kiểm thử được bằng đồng hồ giả lập (NFR-13) (CHK043).
- *(Sửa 2026-10-06)* Hai giả định trước đây về thang bắt buộc và mức chuyên biệt (CHK044, Q-02) không còn áp dụng vì Q-280 bỏ thang điểm; điểm đối chiếu với bác sĩ của cơ sở nay là Q-280, Q-281 ở mục 24.4.
- Các điều kiện chặn và tác động ở bảng trạng thái phụ thuộc feature 003, 004, 005, 006, 007, 009, 010, 011, 012, 013, 014 qua giao diện "điều kiện Đạt / Chưa đạt" và "sự kiện chuyển trạng thái"; spec này viết được song song với các feature đó, nhưng nghiệm thu đầu–cuối của từng lệnh cần feature tương ứng (CHK045).
- Hai chuyển do điểm danh chuyến đi là lệnh do hệ thống thực hiện, nên không thuộc quy tắc "lý do bắt buộc với lệnh do người dùng thực hiện" của FR-046; căn cứ ghi là chuyến đi, người thực hiện ghi kèm là người điểm danh (CHK046).

## Điểm cần báo lại về tài liệu nguồn

Ngày 2026-09-25, các điểm đã chốt đã được đưa vào `docs/business/nghiep-vu.md` và `docs/phan-tich-yeu-cau.md` theo yêu cầu. Danh sách dưới đây ghi nơi đã phản ánh và các điểm còn mở.

**Đã phản ánh vào tài liệu nguồn**

1. Hai chuyển trạng thái vào/ra Hoạt động bên ngoài do điểm danh chuyến đi kích hoạt; Cho tạm vắng / Ghi nhận trở về không qua duyệt: BR-M01-06.
2. Người từng lưu trú quay lại có hồ sơ mới liên kết hồ sơ cũ, CCCD duy nhất trong các hồ sơ chưa ở trạng thái cuối (Q-12): 5.1, DBR-01, mục 24.2.
3. Bản đồng ý không chặn tiếp nhận, nhắc theo CFG-M01-06: 5.1, dòng "Đang tiếp nhận → Đang lưu trú" của 5.6, Phụ lục 25.
4. Ngoại lệ của BR-M01-05 (rút lại đồng ý; đính chính có Quản lý viện duyệt): BR-M01-05.
5. *(Không còn áp dụng, Q-281.)* Cờ nguy cơ khi chấp nhận đánh giá — được gắn thêm, không được bỏ (Q-13): BR-M01-09, mục 24.2. **(Sửa, 2026-09-30)** Được bỏ cờ đề xuất, bắt buộc lý do (Q-250).
6. Quyền D của Quản lý viện ở dòng "Tạm vắng, trở về" chỉ cho quyết định giữ/giải phóng giường: chú thích ⁸ của Permission Matrix 4.4.
7. Trạng thái kết thúc "Được thay thế" của yêu cầu phê duyệt: 6.6 và BR-M01-03.
8. Thời hạn hiệu lực của đánh giá khi Hoàn tất tiếp nhận theo CFG-M01-02: dòng "Đang tiếp nhận → Đang lưu trú" của 5.6; cột "Dùng tại" của CFG-M01-02.
9. Người xác nhận qua đời (bác sĩ khi mất tại viện; bác sĩ hoặc hành chính kèm bằng chứng khi mất ngoài viện): dòng "→ Qua đời" của 5.6.

**Còn mở**

1. *(Đã đóng 2026-10-06: Q-280 bỏ thang điểm, Q-02 không còn áp dụng.)* **Bảng 5.3, dòng Barthel 21–60** ghi "mức chăm sóc thường xuyên trở lên" — không phải một mức cụ thể; spec giả định đề xuất "Chăm sóc thường xuyên" (đã ghi tạm vào cột Mặc định của Q-02, mục 24.1). Chờ bác sĩ của cơ sở chốt Q-02.
2. **Q-03** (căn cứ pháp lý và mẫu bản đồng ý) vẫn mở. **(Cập nhật, 2026-10-01)** Q-03 đã chốt theo mặc định (Nghị định 13/2023/NĐ-CP; mẫu do viện ban hành) và chuyển sang 24.2; cần tư vấn pháp lý đối chiếu trước khi triển khai (24.4). Spec không đổi.

**(2026-09-28, góp ý nghiệp vụ Q-207 → Q-222) Tài liệu nguồn đã thay đổi, spec cần rà lại.** *(2026-09-29: mọi điểm dưới đây đã xử lý, xem nhãn "Đã xử lý" từng điểm.)* Các điểm dưới đây đã có trong `docs/business/nghiep-vu.md` và `docs/business/luong-nghiep-vu.md`; spec **chưa** được sửa theo, và cần chạy `/speckit-clarify` hoặc cập nhật FR tương ứng.

1. **[Đã xử lý 2026-09-28: Phạm vi 7, User Story 7, FR-017, FR-023, FR-023a → FR-023f, bảng phân nhóm dữ liệu, Key Entities, SC-011]** **Nguyện vọng cuối đời (Q-213, Q-216, Q-217; 5.2, UC-82, DBR-34).** "Thông tin chăm sóc cuối đời" ở FR-017, FR-023 thành phiếu khảo sát nguyện vọng có phiên bản: (a) chuyển bệnh viện điều trị tích cực, (b) đưa về nhà, (c) ở lại viện chăm sóc giảm nhẹ. Phiếu ghi người trả lời, bản ký scan. Người cao tuổi còn đủ năng lực tự ký, không còn thì người đại diện ký; ý kiến khác nhau thì theo ý người cao tuổi. Bác sĩ hoặc Điều dưỡng ghi phiếu. Chưa có phiếu không chặn Hoàn tất tiếp nhận nhưng có cảnh báo và nhắc theo CFG-M01-06. Hành chính chỉ thấy "đã có / chưa có".

**(2026-09-30)** 10. **Rà soát vận hành (B7.7)** – bác sĩ được bỏ cờ nguy cơ đề xuất, bắt buộc lý do (Q-250, sửa Q-13). *(Đã xử lý 2026-09-30: BR-M01-09, Q-250 ở 24.2, BF-01 bước 6; FR-030, kịch bản bỏ cờ.)*

**(2026-10-01)** 11. **Checklist cross-feature CHK097** – bác sĩ bỏ cờ đề xuất trong lần đánh giá lại thì cờ đang gắn cùng loại được gỡ (Q-255). *(Đã xử lý 2026-10-01: BR-M01-10, Q-255 ở 24.2; FR-030, FR-037.)*

**(2026-10-01)** 12. **Checklist cross-feature CHK098** – nêu rõ feature khác chỉ đọc cờ đang gắn, không đọc cờ đề xuất. *(Không đổi tài liệu nguồn; FR-030.)*

**(2026-10-01)** 13. **Đánh giá checklist business-rules** – 28 mục được bổ sung vào spec (bảng trạng thái các thực thể khác, kịch bản, tiêu chí thành công, giả định, nguồn của FR). Ba điểm là đề xuất mới chưa có trong tài liệu nguồn, cần ghi vào 5.2, 5.3 khi rà lại nguồn: danh mục mức độ dị ứng và nguồn thông tin (FR-018); tối đa một lần đánh giá Nháp và lệnh "Hủy nháp" (FR-026); mục dị ứng mang dấu "chờ kiểm tra" khi kiểm tra chưa chạy được (Edge Cases). Các điểm còn lại chỉ làm rõ điều tài liệu nguồn đã có. *(Đã xử lý 2026-10-01: Q-272 ở 24.2, các quy tắc mang mã BR-M01-11 → BR-M01-13; cuối các mục 5.2, 5.3.)*

**(2026-10-05) Thay đổi do chủ dự án chốt Q-280 → Q-286.** *(Đã xử lý 2026-10-06: spec đã viết lại theo các quyết định dưới đây, xem mục "Cập nhật 2026-10-06" ở Clarifications. Nguồn sửa thêm DBR-05.)*

- **Q-280** – bỏ thang điểm (Barthel, MMSE, Morse, Braden), bảng quy đổi CFG-M01-05 và hoạt động mẫu; Bác sĩ đánh giá bằng nhận xét dựa trên hồ sơ sức khỏe ban đầu và chỉ số đo, rồi chọn mức chăm sóc (5.3, BR-M01-09). Ảnh hưởng: User Story 3, FR-024 → FR-036, FR-049a, SC-004, SC-005, thực thể Thang điểm, Lần đánh giá, Kết quả thang điểm.
- **Q-281** – bỏ cờ nguy cơ (ngã, loét, đi lạc); phần "lưu ý chăm sóc" của nhận xét đánh giá gần nhất hiện trên checklist và thẻ thông tin khẩn cấp; bỏ mọi điều kiện dựa vào cờ đi lạc (BR-M01-10). Ảnh hưởng: FR-004, FR-008, FR-009, FR-030, FR-037, FR-038, FR-049, thực thể Cờ nguy cơ, điểm báo lại 11, 12.
- **Q-283** – gộp Dinh dưỡng viên vào Bác sĩ; bản gán chế độ ăn do Bác sĩ lập nên không còn bước duyệt riêng (BR-M08-03); cột DDV của Phụ lục 27 gộp vào cột BS. Ảnh hưởng: FR-007, FR-008, SC-012 (quyền xem của Dinh dưỡng viên).
- **Q-282** – bỏ vai trò Nhân viên vệ sinh và nghiệp vụ vệ sinh (7.5, BR-M03-08, 11, 12, 14, UC-71 → 73); giường kết thúc phân bổ chuyển "Chờ sẵn sàng", Trưởng tầng hoặc Người phụ trách ca "Xác nhận giường sẵn sàng" (nghi nhiễm thì bắt buộc xác nhận đã khử khuẩn) (BR-M03-09, 10, UC-74); nhân viên không dùng hệ thống chỉ có hồ sơ nhân viên. Ảnh hưởng: FR-007, SC-012 (Nhân viên vệ sinh).

**(2026-10-06) Thay đổi do chủ dự án chốt Q-292 → Q-299 (rà soát tài liệu nguồn).** *(Đã xử lý 2026-10-06: spec đã viết lại, xem mục "Cập nhật 2026-10-06, lượt 2" ở Clarifications.)*

- **Q-296** – yêu cầu xem xét kế hoạch chăm sóc tạo khi lần đánh giá lại được xác nhận có mức chăm sóc hoặc tập nhu cầu chăm sóc khác lần trước (kể cả chỉ đổi nhu cầu); chỉ đổi mức mới tạo yêu cầu thay đổi lưu trú (5.4, BR-M04-20, BF-08). Ảnh hưởng: FR-036, FR-043 (nguồn của yêu cầu xem xét kế hoạch chăm sóc chuyển cho feature 005 khi đổi mức hoặc nhu cầu).

**(2026-10-06) Thay đổi do chủ dự án chốt Q-300 (quy trình tiếp nhận thực tế).** *(Đã xử lý 2026-10-06: xem mục "Cập nhật 2026-10-06, lượt 3" ở Clarifications.)*

- **Q-300** – gia đình chọn mức chăm sóc trong hợp đồng; Bác sĩ vẫn đánh giá đầu vào, mức hiện hành vẫn lấy từ đánh giá; Hoàn tất tiếp nhận bị chặn khi mức của hợp đồng Hiệu lực khác mức hiện hành; Bác sĩ xác nhận đánh giá đầu vào có mức khác hợp đồng thì Hành chính được báo (4, 5.3, 5.6, BR-M01-09, BR-M02-22, BF-01 bước 6, 14). Ảnh hưởng: FR-033, bảng trạng thái dòng "Đang tiếp nhận → Hoàn tất tiếp nhận". Mức của thông báo lệch mức chưa được tài liệu nguồn nêu (spec 009 Q-108); feature 004 FR-025b chưa ghi mức. *(Đã xử lý 2026-10-06: Q-302, Trung bình cho Hành chính.)*

**(2026-10-06) Cập nhật căn cứ pháp lý (rà bảng 24.4).** Chỉ đổi trích dẫn, không đổi quy tắc; nội dung cần tư vấn pháp lý xác nhận ở 24.4.

- **Q-03** – Nghị định 13/2023/NĐ-CP hết hiệu lực từ 01/01/2026, thay bằng Luật Bảo vệ dữ liệu cá nhân số 91/2025/QH15 và Nghị định 356/2025/NĐ-CP. Tài liệu nguồn (5.1, Q-03, 24.4) đã đổi trích dẫn. Cần tư vấn pháp lý xác nhận theo luật mới: mẫu bản đồng ý, và có bắt buộc ghi nhật ký mỗi lần nhân viên xem dữ liệu sức khỏe không (Q-03 hiện chọn không). Spec này đổi trích dẫn ở FR-012.

**(2026-10-06) Thay đổi do chủ dự án chốt Q-306 (hệ thống không tự sinh dữ liệu, công việc).** *(Đã xử lý 2026-10-06: spec đã viết lại, xem mục "Cập nhật 2026-10-06, lượt 4" ở Clarifications.)*

- **Q-306** – Đánh giá lại ra mức khác không còn tự tạo yêu cầu thay đổi lưu trú: Hành chính được báo và lập yêu cầu, chưa lập thì được nhắc theo CFG-M03-02 (BR-M01-03). Yêu cầu xem xét kế hoạch chăm sóc bị bỏ, thay bằng thông báo cho điều dưỡng phụ trách (BR-M04-20, Q-296 sửa). Yêu cầu đánh giá lại (BR-M01-02) giữ nguyên. Nguyên tắc chung: mọi bản ghi do actor lập bằng lệnh; hệ thống chỉ gợi ý từ dữ liệu đã có, tự chuyển trạng thái theo thời gian, cảnh báo, leo thang, nhắc, kiểm tra và thực hiện tác động kèm theo lệnh (1.3, 24.2). FR cần rà (dò theo từ khóa, chưa đối chiếu từng FR): FR-029, FR-031, FR-043.

**(2026-10-06) Thay đổi do chủ dự án chốt Q-307 (bỏ gợi ý, đề xuất, yêu cầu và sự cố do hệ thống tự tạo).** *(Đã xử lý 2026-10-06: spec đã viết lại, xem mục "Cập nhật 2026-10-06, lượt 4" ở Clarifications.)*

- **Q-307** – Bỏ yêu cầu đánh giá lại: hệ thống chỉ báo bác sĩ cần đánh giá lại (đến hạn định kỳ, sau sự cố ngã hoặc mức Trung bình trở lên, trở về từ bệnh viện); quá CFG-M01-03 kể từ lúc báo mà chưa có lần đánh giá lại được xác nhận thì cảnh báo bác sĩ (BR-M01-02, UC-07 đổi thành "Nhắc đánh giá lại"). Giữ: cảnh báo do hệ thống tạo và leo thang, nhắc, thông báo; bản ghi kèm lệnh của actor; số liệu tính để hiển thị (24.2).

**(2026-10-10) Plan 001, điểm báo lại 1 → 8.** *(Đã xử lý 2026-10-10: Q-311, Q-313 (dòng "Chuyển viện" ở Phụ lục 27), Q-317 (danh mục do Quản lý viện cấu hình), Q-318 → Q-320; sửa FR-005, FR-036, FR-046, bảng trạng thái, bảng nhóm dữ liệu, Assumptions; thêm FR-054. Câu dẫn `docs/phan-tich-yeu-cau.md` giữ làm lịch sử.)*

**(2026-10-11) Đối chiếu tài liệu gốc DOCX và rà soát bổ sung (báo cáo audit 2026-10-11, không lưu trong kho mã).** *(Đã xử lý 2026-10-11: Q-337 → Q-345; sửa FR-047b, bảng trạng thái người cao tuổi (dòng Hoàn tất tiếp nhận); xem mục "Cập nhật 2026-10-11" ở Clarifications.)*
