# Tóm tắt nghiệp vụ Hệ thống Quản lý Viện Dưỡng Lão

Tóm tắt từ docs/nghiep-vu.md và docs/luong-nghiep-vu.md (ngày 2026-09-28). Tài liệu chỉ mô tả **nghiệp vụ và luồng chính** ở mức tổng quan. Quy tắc chi tiết (BR, DBR), tham số, yêu cầu phi chức năng, tích hợp, ma trận quyền và quyết định còn mở nằm ở docs/quy-tac-nghiep-vu.md. Khi có khác biệt, docs/nghiep-vu.md là căn cứ.

## 1. Mục tiêu và phạm vi

Hệ thống giúp viện dưỡng lão (quy mô khoảng 100–300 người cao tuổi, khoảng 40–100 nhân viên, 2 ca ngày/đêm) quản lý tập trung toàn bộ quá trình:

Tiếp nhận → Đánh giá → Lưu trú → Kế hoạch chăm sóc → Phân công → Chăm sóc hằng ngày → Theo dõi sức khỏe → Thuốc → Dinh dưỡng → Hoạt động → Xử lý sự cố → Chi phí và thu chi → Tương tác với người thân → Kết thúc lưu trú.

**Ranh giới.** Hệ thống **không** thay thế: phần mềm kế toán (không hạch toán, không báo cáo tài chính của viện, không thanh toán trực tuyến); bệnh án điện tử; kho thuốc và vật tư y tế; camera; nhân sự/tiền lương chuyên sâu. Hệ thống **có** quản lý: số dư và thu chi của từng người cao tuổi, kho nguyên liệu nấu ăn, tài sản của viện.

**Nguyên tắc chung.**
- Mọi thông tin quan trọng gắn với người cao tuổi, thời điểm và người thực hiện; mọi thay đổi có lịch sử.
- Hệ thống **tự sinh** công việc, liều thuốc, suất ăn, cảnh báo, chi phí từ dữ liệu đã có; nhân viên chủ yếu xác nhận kết quả.
- Không "sửa trạng thái" trực tiếp: trạng thái chỉ đổi qua **lệnh nghiệp vụ** có điều kiện (ví dụ Cho tạm vắng, Chuyển giường). Bản ghi đã xác nhận không sửa, không xóa; sai sót xử lý bằng đính chính hoặc khoản điều chỉnh.
- Ngưỡng, thời hạn, tỷ lệ là **tham số** do viện cấu hình, không cố định.
- Thông tin y tế chỉ hiện theo phạm vi chuyên môn, phân công và bản đồng ý chia sẻ dữ liệu.

## 2. Vai trò

| Vai trò | Việc chính |
| --- | --- |
| Quản lý viện | Cấu hình, phê duyệt (thay đổi lưu trú, chi phí, ngoại lệ), chốt chi phí, xem báo cáo; quản lý kho nguyên liệu (duyệt phiếu kiểm kê) và tài sản |
| Trưởng tầng | Điều phối tầng: phân công, xử lý việc quá hạn, kiểm tra chất lượng, lập lịch ca, duyệt đổi ca |
| Bác sĩ | Đánh giá, thiết lập ngưỡng, kê đơn (trong phạm vi giấy phép), duyệt kế hoạch chăm sóc, ghi dấu nguy kịch |
| Điều dưỡng | Thuốc, cảnh báo, kế hoạch chăm sóc, bàn giao ca; làm được mọi việc của nhân viên chăm sóc |
| Nhân viên chăm sóc | Thực hiện và ghi nhận công việc chăm sóc, đo chỉ số, hoạt động |
| Dinh dưỡng viên | Chế độ ăn, món ăn, thực đơn |
| Nhân viên bếp | Chuẩn bị, giao suất ăn; lưu mẫu; phiếu xuất nguyên liệu, đề nghị nhập, lập phiếu kiểm kê kho |
| Nhân viên vệ sinh | Vệ sinh phòng, khu vực, trả giường, khử khuẩn |
| Hành chính | Tiếp nhận, hợp đồng, tạm vắng, người thân, đồ gửi, kiểm tra chi phí, khai báo tạm trú |
| Kế toán | Thu cọc và tiền nộp, chốt quỹ tiền mặt cuối ngày, đối soát chuyển khoản, hoàn tiền, quyết toán, xuất sao kê và file kế toán |
| Người thân | Qua cổng: xem thông tin được phép, đăng ký thăm, xem chi phí và số dư, phản hồi; người đại diện ký, đồng ý, yêu cầu thay đổi |
| Bộ lập lịch (hệ thống) | Tự sinh công việc, liều, suất, chi phí; nhắc hạn; leo thang cảnh báo |

**Nhiệm vụ** (gán theo ca, không phải vai trò): Người phụ trách ca, Bác sĩ trực, Điều dưỡng phụ trách, Trưởng đoàn, Người nhận tại tầng. **Quan hệ người thân:** Người đại diện, Người liên hệ chính (đúng một người), Người được phép đón.

## 3. Khái niệm cốt lõi

**Loại hình lưu trú** (độc lập với mức chăm sóc):
- **Nội trú dài hạn:** hợp đồng dài hạn, có giường, giữ giường khi vắng theo chính sách.
- **Nội trú ngắn ngày:** có ngày bắt đầu, kết thúc, không tự gia hạn.
- **Bán trú:** đến và về trong ngày theo lịch, điểm danh đến/về, có trạng thái có mặt theo ngày (Chưa đến, Có mặt, Đã về, Vắng có báo, Vắng không báo).

**Mức chăm sóc:** cơ bản, thường xuyên, đặc biệt, hỗ trợ vận động, phục hồi…; xác định từ đánh giá (thang Barthel, MMSE, Morse, Braden), mỗi mức có trọng số dùng để tính tỷ lệ phục vụ khi lập ca.

**Trạng thái người cao tuổi:**

| Trạng thái | Chuyển được sang |
| --- | --- |
| Đang tiếp nhận | Đang lưu trú, Hủy tiếp nhận |
| Đang lưu trú | Tạm vắng, Hoạt động bên ngoài, Điều trị tại bệnh viện, Kết thúc lưu trú, Qua đời |
| Tạm vắng | Đang lưu trú, Điều trị tại bệnh viện, Kết thúc lưu trú, Qua đời |
| Hoạt động bên ngoài | Đang lưu trú, Điều trị tại bệnh viện, Qua đời |
| Điều trị tại bệnh viện | Đang lưu trú, Kết thúc lưu trú, Qua đời |
| Kết thúc lưu trú, Qua đời, Hủy tiếp nhận | Trạng thái cuối (hồ sơ chỉ đọc) |

**Phân loại dữ liệu:** (1) danh mục – được tạo, sửa; (2) nghiệp vụ có trạng thái – chỉ đổi qua lệnh hoặc phiên bản mới; (3) ghi nhận đã xác nhận – chỉ ghi thêm.

## 4. Các nhóm chức năng

| Nhóm | Nội dung chính |
| --- | --- |
| **Hồ sơ và đánh giá** (M01) | Hồ sơ cá nhân, bản đồng ý chia sẻ dữ liệu, hồ sơ sức khỏe ban đầu (dị ứng, bệnh nền ghi từng mục, không xóa), phiếu nguyện vọng cuối đời, đánh giá đầu vào và đánh giá lại, cờ nguy cơ (ngã, loét, đi lạc; bác sĩ được thêm hoặc bỏ cờ đề xuất, bắt buộc lý do) |
| **Lưu trú và tạm trú** (M02, M03) | Đăng ký tiếp nhận, danh sách chờ có điểm ưu tiên, hợp đồng (Nháp → Chờ ký → Hiệu lực → Kết thúc/Chấm dứt; sửa bằng phụ lục), đặt cọc, thay đổi lưu trú có duyệt, tạm vắng với bảng chính sách phí vắng, kết thúc lưu trú, khai báo tạm trú với công an (từ 30 ngày cộng dồn theo chuỗi hợp đồng thì đăng ký tạm trú, dưới đó hoặc thường trú cùng phường thì thông báo lưu trú; gia hạn thì khai báo lại); cấu trúc khu – tầng – phòng – giường, phân bổ và chuyển giường, vệ sinh phòng và trả giường |
| **Chăm sóc** (M04, M06 phần chỉ số, M10) | Kế hoạch chăm sóc theo phiên bản có duyệt; công việc tự sinh theo ca (gồm đo chỉ số), checklist, ghi nhận kết quả, việc quá hạn; hoạt động trong và ngoài viện; theo dõi tinh thần; người thân (quyền, thăm, đón, ở lại, phản hồi, bản tin) |
| **Thuốc** (M07) | Đơn thuốc (định kỳ, khi cần), lịch liều tự sinh, xác nhận liều, thuốc gia đình gửi, đối chiếu thuốc khi tiếp nhận và khi trở về từ bệnh viện |
| **Sự cố và cảnh báo** (M05, M06) | Ngưỡng chỉ số theo từng người, cảnh báo tự động và leo thang, sự cố và quy trình khẩn cấp (thẻ thông tin khẩn cấp), khoanh vùng lây nhiễm, dấu nguy kịch và thực hiện nguyện vọng cuối đời |
| **Dinh dưỡng và kho bếp** (M08) | Chế độ ăn gán cho từng người, thực đơn tuần, chốt suất, phiếu bữa ăn theo tầng, suất đặc biệt, đồ ăn gia đình mang vào, lưu mẫu; kho nguyên liệu (nhập, xuất theo lô, kiểm kê) |
| **Nhân sự và ca trực** (M09) | Hồ sơ nhân viên và giấy phép, lịch ca xoay vòng, phủ tối thiểu, phân công, đổi ca, nghỉ đột xuất, bàn giao ca tự lập |
| **Tài chính** (M11) | Chi phí tự sinh từ sự kiện nguồn, kiểm tra – duyệt – chốt bảng chi phí theo tháng, khoản điều chỉnh, mua hộ; số dư và thu chi của từng người (sổ mở khi hợp đồng Chờ ký), chốt quỹ ngày, đối soát chuyển khoản, báo sắp hết tiền, quyết toán |
| **Tài sản và đồ gửi** (M03 phần tài sản, M12) | Tài sản của viện (xe, giường, thiết bị lớn; bảo trì; lịch xe tự chuyển theo chuyến đi). Giường tạo kèm tài sản; trạng thái bảo trì, ngừng dùng của giường chỉ đổi qua lệnh tài sản; ngừng dùng vĩnh viễn bằng thanh lý; tạm đóng giường không vì hỏng bằng trạng thái Tạm ngừng sử dụng (Quản lý viện); đồ gửi của người cao tuổi (tiếp nhận, bàn giao, trả, kiểm kê) |
| **Dịch vụ chung** (M13, M14, M15) | Thông báo 3 mức (Nhẹ, Trung bình, Khẩn cấp) qua ứng dụng, tin nhắn, gọi điện; báo cáo và dashboard chỉ đọc; tài khoản, phân quyền 3 lớp (vai trò ∩ phạm vi phân công ∩ điều kiện pháp lý), nhật ký, tham số |

## 5. Luồng nghiệp vụ chính

Mỗi luồng ghi các bước chính. Bước chi tiết, nhánh ngoại lệ và căn cứ ở docs/luong-nghiep-vu.md.

### BF-01 – Tiếp nhận người cao tuổi
1. Hành chính tạo hồ sơ, lập lượt đăng ký, ghi bản đồng ý chia sẻ dữ liệu.
2. Bác sĩ, Điều dưỡng ghi hồ sơ sức khỏe ban đầu và phiếu nguyện vọng cuối đời.
3. Bác sĩ đánh giá đầu vào; hệ thống đề xuất mức chăm sóc, cờ nguy cơ, hoạt động mẫu; Bác sĩ chấp nhận hoặc điều chỉnh.
4. Có giường thì phân bổ; không có thì vào danh sách chờ (hệ thống đề xuất khi có giường trống).
5. Hành chính lập hợp đồng và gửi ký; từ lúc hợp đồng Chờ ký, Kế toán thu cọc (trạng thái đặt cọc tự tính từ số tiền đã thu); người đại diện ký.
6. Đối chiếu thuốc khi tiếp nhận (hai người xác nhận).
7. Hành chính Hoàn tất tiếp nhận; hệ thống kiểm tra điều kiện (đánh giá còn hạn, hợp đồng Hiệu lực, cọc đạt, có giường) → **Đang lưu trú**; sinh liều, lịch, suất ăn; tạo khai báo tạm trú.
8. Điều dưỡng lập kế hoạch chăm sóc, người có quyền duyệt.

### BF-02 – Vòng ca chăm sóc hằng ngày
1. Bộ lập lịch sinh công việc cho ca từ kế hoạch, lịch đo, lịch bữa, hoạt động, lịch vệ sinh.
2. Bán trú: điểm danh đến, sinh công việc trong giờ có mặt.
3. Nhân viên nhận checklist, ghi nhận Hoàn thành hoặc Không thực hiện (có lý do).
4. Quá hạn: nhắc; việc Quan trọng báo Trưởng tầng; việc Bắt buộc tạo cảnh báo.
5. Kiểm tra chất lượng ngẫu nhiên; cuối ca tự lập bản nháp bàn giao; ca sau xác nhận.

### BF-03 – Lập và công bố thực đơn tuần
Dinh dưỡng viên lập thực đơn (món và món thay thế theo chế độ ăn) → hệ thống kiểm tra độ phủ, lặp món, dị ứng → dinh dưỡng viên công bố (không có người duyệt thủ công) → đổi món bằng lệnh có lý do.

### BF-04 – Chuẩn bị và phân phối suất ăn
Trước bữa, hệ thống chốt số suất và sinh phiếu bữa ăn theo tầng → bếp chuẩn bị, dán nhãn suất đặc biệt, ghi lưu mẫu → giao → người nhận tại tầng kiểm đếm, xác nhận hoặc báo sai lệch → nhân viên chăm sóc xác nhận đúng người, đúng suất khi phục vụ.

### BF-05 – Vệ sinh trả giường và khử khuẩn khoanh vùng
Phân bổ giường kết thúc → giường Chờ vệ sinh, sinh công việc trả giường (hoặc khử khuẩn nếu nghi nhiễm) → hoàn thành thì giường Trống. Hư hỏng liên quan giường → báo hỏng tài sản của giường; giường trống thì vào bảo trì, giường đang có người thì mang dấu "chờ chuyển người" và không được xếp người mới tới khi báo hỏng lại hoặc gỡ dấu; hỏng mức "mất an toàn" thì báo khẩn để chuyển giường gấp, hỏng nhẹ sửa tại chỗ thì gỡ dấu ngay. Quản lý viện tạm đóng giường không vì hỏng bằng trạng thái Tạm ngừng sử dụng. Khoanh vùng lây nhiễm → chặn thăm, hoạt động chung, phân bổ mới; khử khuẩn định kỳ tới khi gỡ.

### BF-06 – Cảnh báo, leo thang và sự cố
Cảnh báo tạo từ chỉ số vượt ngưỡng, việc bắt buộc quá hạn, liều bỏ lỡ… → Điều dưỡng phụ trách tiếp nhận → quá hạn leo thang lên Trưởng tầng và Bác sĩ trực, rồi Quản lý viện; sau đó không leo thang thêm nhưng nhắc lại theo chu kỳ tới khi có người tiếp nhận → xử lý, đóng hoặc chuyển sự cố. Sự cố khẩn cấp báo đồng thời mọi bên, hiển thị thẻ thông tin khẩn cấp, có thể chuyển viện.

### BF-07 – Thuốc: đơn, liều và đối chiếu
Nhập đơn (kiểm tra trùng hoạt chất, dị ứng) → sinh liều → Điều dưỡng xác nhận liều → quá giờ thì Trễ, Bỏ lỡ và cảnh báo. Trở về từ bệnh viện: tạm dừng đơn cũ, đối chiếu thuốc hai người xác nhận.

### BF-08 – Thay đổi lưu trú
Lập yêu cầu (Hành chính, Bác sĩ, hệ thống, người đại diện) → Quản lý viện duyệt → tới ngày hiệu lực áp dụng (mức chăm sóc, đơn giá, giường, thời hạn) → Điều dưỡng xem xét lại kế hoạch chăm sóc.

### BF-09 – Chốt chi phí kỳ
Hệ thống tự sinh khoản nháp từ sự kiện nguồn → Hành chính kiểm tra, gửi chốt → Quản lý viện duyệt, chốt bảng từng người → trừ vào số dư → Kế toán xuất file kế toán. Sai sót sau chốt bằng khoản điều chỉnh.

### BF-10 – Kết thúc lưu trú
Lập hồ sơ kết thúc với ngày dự kiến → hệ thống kiểm tra điều kiện: chi phí kỳ cuối đã chốt, đồ gửi và thuốc gửi đã trả, không còn cảnh báo/sự cố mở, số dư và tiền cọc đã quyết toán, đã bàn giao người cao tuổi (Quản lý viện được duyệt ngoại lệ, trừ bàn giao) → Hành chính thực hiện Kết thúc lưu trú → giải phóng giường, hủy lịch tương lai, khai báo xóa tạm trú.

### BF-11 – Ghi nhận qua đời
Bác sĩ (hoặc Hành chính kèm giấy tờ khi mất ngoài viện) ghi nhận → hệ thống chấm dứt hợp đồng, giải phóng giường, dừng sinh chi phí, báo người liên hệ chính → mở danh sách việc sau qua đời (đồ gửi, thuốc, chi phí, quyết toán, cảnh báo/sự cố) → hoàn thành thì đóng hồ sơ.

### BF-12 – Người thân: quyền, thăm, đón, ở lại, phản hồi, bản tin
Hành chính lập quan hệ và quyền → tạo tài khoản cổng → người thân đăng ký thăm (kiểm tra khung giờ, sức chứa, khoanh vùng) → quy trình đón kiểm tra người được phép đón → người thân ở lại có xác nhận của Trưởng tầng → phản hồi có hạn xử lý → bản tin định kỳ do Điều dưỡng duyệt.

### BF-13 – Lịch ca, đổi ca, nghỉ đột xuất, bàn giao ca
Sinh lịch tháng từ mẫu xoay ca → kiểm tra phủ tối thiểu, tỷ lệ phục vụ → công bố → đổi ca, nghỉ đột xuất qua yêu cầu có Trưởng tầng duyệt, gợi ý người thay → bàn giao cuối ca, ca sau xác nhận.

### BF-14 – Đồ gửi
Tiếp nhận (đồ có giá trị bắt buộc ảnh) → bàn giao giữa người giữ → trả cho người có quyền nhận (người cao tuổi không có cờ đi lạc tự nhận lại đồ của mình, trừ tiền mặt, trang sức, không cần người đại diện xác nhận) → kiểm kê định kỳ; đồ không người nhận xử lý qua đề nghị có duyệt.

### BF-15 – Hoạt động, chuyến đi ngoài viện, kiểm tra chất lượng
Khai báo hoạt động và mẫu lặp → đăng ký, điểm danh → chuyến đi: phân công trưởng đoàn, xử lý thuốc mang theo, đặt xe (lịch xe tự chuyển Đang dùng, Đã hoàn thành, Đã hủy theo chuyến; dời chuyến bị chặn nếu xe đã có lịch khác), điểm danh rời/về, báo thiếu người → kiểm tra chất lượng ngẫu nhiên (Trưởng tầng; ca không có Trưởng tầng thì Người phụ trách ca) → cảnh báo nguy cơ cô lập khi không tham gia hoạt động nhóm.

### BF-16 – Số dư, thu chi và báo sắp hết tiền
Hợp đồng Chờ ký thì mở sổ số dư, sổ tiền cọc và cấp mã nộp tiền → gia đình nộp tiền mặt (phiếu thu có số liên tục) hoặc chuyển khoản (mã QR) → cuối ngày Kế toán lập chốt quỹ, Quản lý viện xác nhận → Kế toán đối soát sao kê, xác nhận (chuyển khoản gộp nhiều khoản thì dùng cặp điều chỉnh chuyển tiền) → chốt bảng chi phí tự trừ số dư → hằng ngày hệ thống tính số ngày còn đủ tiền, báo "sắp hết tiền" hoặc "còn nợ" cho người đại diện và Kế toán; gia đình nộp đủ thì báo "đã ghi nhận khoản nộp" → hoàn tiền, điều chỉnh có Quản lý viện duyệt → quyết toán khi kết thúc lưu trú → xuất sao kê, báo cáo thu chi.

### BF-17 – Nguy kịch và nguyện vọng cuối đời
Bác sĩ ghi dấu nguy kịch → cảnh báo khẩn cấp hiển thị nguyện vọng, báo gia đình → gọi xác nhận lại nguyện vọng → thực hiện lựa chọn: chuyển viện điều trị tích cực, đưa về nhà (tạm vắng), hoặc ở lại viện chăm sóc giảm nhẹ → đóng cảnh báo.

## 6. Việc còn mở

Không còn quyết định mở (2026-10-01): Q-01 → Q-03, Q-05 → Q-09, Q-63 đã được chủ dự án chốt theo giá trị mặc định, cùng Q-245 → Q-270 sau rà soát vận hành và Q-271 → Q-279 (các quy tắc bổ sung từ lượt đánh giá checklist của spec 000 → 008, gồm 63 quy tắc mang mã BR mới ở các module 01, 02, 03, 04, 05, 07, 09, 15; danh sách đầy đủ ở docs/business-rule.md). Các quyết định này chưa có xác nhận của người vận hành viện hay người có chuyên môn; danh sách cần đối chiếu trước khi triển khai nằm ở mục 24.4 của docs/nghiep-vu.md (tóm tắt ở mục H của docs/quy-tac-nghiep-vu.md). Các chức năng mới thêm ngày 2026-09-28 có spec: số dư và thu chi (017), khai báo tạm trú (018), kho nguyên liệu và tài sản (019).
