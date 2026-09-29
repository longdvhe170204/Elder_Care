# Luồng nghiệp vụ chính (BF-01 → BF-17)

Tài liệu mô tả các luồng nghiệp vụ chính theo từng bước: ai thực hiện, làm gì, trạng thái thay đổi thế nào và căn cứ. Căn cứ là docs/nghiep-vu.md (ghi mã mục, BR, CFG, Q) và spec đã clarify (ghi "spec 0xx FR-yy"). Khi hai nguồn khác nhau, docs/nghiep-vu.md mục 19.3 là căn cứ về quyền.

Quy ước:

- Cột **Vai trò** dùng vai trò hệ thống (2.3) hoặc nhiệm vụ (2.4). "Hệ thống" là quy tắc tự động khi có sự kiện; "Bộ lập lịch" là tác vụ chạy theo thời gian (AC-11).
- Giá trị trong ngoặc vuông là tham số cấu hình (Phụ lục 25).
- Mỗi luồng có mục **Điểm đã làm rõ**, trả lời các câu hỏi mở trước đây về vai trò và trạng thái.
- Luồng có nhánh ngoại lệ quan trọng thì có thêm bảng **Nhánh ngoại lệ**.
- Từ 2026-09-28, sau khi bỏ docs/phan-tich-yeu-cau.md, tài liệu này là nguồn duy nhất của mã BF. Mã BF-03 cũ ("Người thân tự phục vụ") không còn dùng; BF-03 hiện là luồng thực đơn. Actor, use case, ma trận quyền và DBR nằm ở Phụ lục 26 → 28 của docs/nghiep-vu.md.

**Phạm vi (Q-206).** Tài liệu gồm 11 luồng lõi (BF-01 → BF-11) và bốn luồng BF-12 → BF-15 cho người thân, ca trực, đồ gửi, hoạt động. **(Bổ sung, 2026-09-28)** Thêm BF-16 (số dư, thu chi) và BF-17 (nguy kịch) theo góp ý nghiệp vụ Q-207 → Q-222. Thông báo và báo cáo là dịch vụ phục vụ các luồng khác, không có BF riêng, và được mô tả trong spec sở hữu.

| Luồng | Mã BF | Spec sở hữu | Mục nghiệp vụ |
| ----- | ----- | ----------- | ------------- |
| Người thân tự phục vụ: đăng ký thăm, đón, phản hồi, bản tin, người thân ở lại | BF-12 | 012 | 14.1 → 14.7 |
| Lập lịch ca, xoay ca, đổi ca, nghỉ đột xuất, bàn giao ca | BF-13 | 008, 015 | 13.2 → 13.5 |
| Đồ gửi: tiếp nhận, bàn giao, trả, kiểm kê | BF-14 | 013 | 16 |
| Hoạt động, chuyến đi ngoài viện, kiểm tra chất lượng | BF-15 | 014 | 8.8 → 8.10, BR-M04-23 |
| **(Bổ sung, 2026-09-28)** Số dư, thu chi, đối soát chuyển khoản, chốt quỹ ngày, báo sắp hết tiền | BF-16 | 017 | 6.5, 15.9 |
| **(Bổ sung, 2026-09-28)** Nguy kịch và thực hiện nguyện vọng cuối đời | BF-17 | 007, 004 (Q-213) | 5.2, 9.5 |
| **(Bổ sung, 2026-09-28)** Khai báo tạm trú, tài sản của viện, kho nguyên liệu | Không có BF riêng; mô tả bằng bảng trạng thái ở mục nghiệp vụ | 018 (tạm trú), 019 (tài sản, kho) | 6.10, 7.7, 12.7 |
| Thông báo và yêu cầu gọi điện | Không có BF (dịch vụ) | 009 | 17 |
| Báo cáo và dashboard | Không có BF (dịch vụ) | 016 | 18 |

---

## BF-01 – Tiếp nhận người cao tuổi

Khởi phát: gia đình liên hệ trực tiếp với viện. Kết thúc: người cao tuổi ở trạng thái Đang lưu trú, có lịch thuốc và kế hoạch chăm sóc.

| Bước | Vai trò | Hoạt động | Trạng thái / kết quả | Căn cứ |
| ---- | ------- | --------- | -------------------- | ------ |
| 1 | Hành chính | Tạo hồ sơ người cao tuổi; lập lượt đăng ký tiếp nhận (ngày đăng ký, người liên hệ, nhu cầu, loại lưu trú mong muốn, mức chăm sóc dự kiến, ngày mong muốn vào) | Hồ sơ: Đang tiếp nhận. Lượt đăng ký: Đang xử lý | UC-01, UC-09, 6.1, spec 004 FR-002 |
| 2 | Hành chính | Ghi nhận bản đồng ý chia sẻ dữ liệu (không chặn tiếp nhận) | Bản đồng ý: Hiệu lực | UC-03, 5.1 |
| 3 | Bác sĩ, Điều dưỡng | Ghi hồ sơ sức khỏe ban đầu, gồm dị ứng, bệnh nền, tiền sử (từng mục) và thuốc đang dùng khi tiếp nhận | Hồ sơ sức khỏe: Nháp → Bác sĩ xác nhận | UC-04, 5.2, spec 001 FR-017 |
| 3a | Bác sĩ hoặc Điều dưỡng (Q-217) | **(Bổ sung, 2026-09-28)** Ghi phiếu khảo sát nguyện vọng cuối đời: chuyển bệnh viện điều trị tích cực / đưa về nhà / ở lại viện chăm sóc giảm nhẹ; người trả lời; bản ký scan (người cao tuổi ký nếu còn đủ năng lực, không thì người đại diện). Không chặn tiếp nhận | Nguyện vọng: Hiệu lực | UC-82, 5.2, Q-213, Q-216 |
| 4 | Bác sĩ (Điều dưỡng hỗ trợ) | Đánh giá đầu vào bằng thang điểm | — | UC-05, 5.3 |
| 5 | Hệ thống | Quy đổi điểm; đề xuất mức chăm sóc, cờ nguy cơ, hoạt động mẫu | — | 5.3 |
| 6 | Bác sĩ | Chấp nhận hoặc điều chỉnh đề xuất (bắt buộc lý do khi khác đề xuất; không bỏ cờ đề xuất) | Hoạt động mẫu chép vào bản nháp kế hoạch chăm sóc | BR-M01-09, Q-13 |
| 7a | Hành chính | Có giường phù hợp: sang bước 8 | — | 6.1 |
| 7b | Hành chính, Hệ thống | Không có giường: đưa vào danh sách chờ. Hệ thống tính điểm ưu tiên hằng ngày; khi giường về Trống thì đề xuất [3] hồ sơ; hành chính liên hệ, giữ chỗ [48 giờ] | Hồ sơ chờ: Đang chờ → Đã liên hệ → Đã tiếp nhận (khi được phân bổ giường) | 6.2, BR-M02-01, 02, 10, Q-29 |
| 8 | Hành chính (hoặc Trưởng tầng trong phạm vi tầng) | Phân bổ giường, có thể đặt trước cho ngày vào | Phân bổ tương lai; giường bị giữ | UC-20, BR-M03-01, Q-42 |
| 9 | Hành chính; Quản lý viện | Lập hợp đồng. Hợp đồng có điều khoản khác chuẩn thì Quản lý viện duyệt trước khi gửi ký | Hợp đồng: Nháp → Chờ ký | UC-11, 6.3, Q-23 |
| 10 | Người đại diện, Hành chính | Người đại diện ký; hành chính ghi nhận đã ký (ngày ký, bản scan). Với nội trú, phải có phân bổ giường trước | Hợp đồng: Hiệu lực | 6.3, Q-28 |
| 11 | Kế toán (**đã chỉnh sửa, Q-212**; trước đây Hành chính) | Ghi thu tiền cọc (tiền mặt, hoặc chuyển khoản đã đối soát, BF-16). **(Q-225)** Làm được từ khi hợp đồng chuyển Chờ ký, nên thường làm cùng lúc với bước 10. Tổng tiền cọc đã thu đủ khoản cần đặt cọc thì đặt cọc tự chuyển Đã đáp ứng (trạng thái dẫn xuất, không có lệnh xác nhận) | Đặt cọc: Đã đáp ứng | UC-12, 6.5, spec 004 FR-031, spec 017 FR-006 |
| 12 | Bác sĩ hoặc Điều dưỡng; một Bác sĩ hoặc Điều dưỡng khác | Lập phiếu đối chiếu thuốc "tiếp nhận"; người khác xác nhận (quy tắc hai người). Phiếu có dòng đơn nội bộ chỉ Bác sĩ có quyền kê đơn xác nhận | Phiếu: đã xác nhận; đơn tạo ra ở trạng thái chờ | 11.5, Q-55, Q-57 |
| 13 | Hành chính | Thực hiện lệnh Hoàn tất tiếp nhận | Hệ thống kiểm tra điều kiện ở bước 14 | UC-08, BR-M01-06 |
| 14 | Hệ thống | Kiểm tra điều kiện: đánh giá đầu vào chưa quá [90 ngày]; hợp đồng Hiệu lực và thời điểm tiếp nhận không sớm hơn ngày bắt đầu hợp đồng; đặt cọc đạt; nội trú có giường, và giường đó đã về Trống. Thiếu điều kiện nào thì từ chối và liệt kê mọi điều kiện chưa đạt | Hồ sơ: Đang lưu trú. Lượt đăng ký: Đã tiếp nhận. Phân bổ bắt đầu thực tế | 5.6, Q-22, Q-50, spec 004 FR-004 |
| 15 | Hệ thống | Đơn thuốc từ phiếu đối chiếu có hiệu lực; sinh liều, lịch cá nhân, suất ăn | — | 5.6, Q-57, BR-M07-01 |
| 15a | Hệ thống; Hành chính | **(Bổ sung, 2026-09-28)** Với nội trú: tạo khai báo tạm trú "Cần khai báo" (đăng ký tạm trú hoặc thông báo lưu trú theo CFG-M02-11, tính cộng dồn cả gia hạn; người thường trú cùng xã/phường với viện luôn thông báo lưu trú, Q-215; cộng dồn theo chuỗi hợp đồng nối tiếp trong cùng hồ sơ, Q-241). Hành chính khai báo với công an trong CFG-M02-12, rồi ghi đã nộp và kết quả | Khai báo: Cần khai báo → Đã nộp → Đã xác nhận | UC-80, 6.10, BR-M02-11, Q-215 |
| 16 | Điều dưỡng; người có quyền Duyệt kế hoạch chăm sóc (mặc định Bác sĩ) | Điều dưỡng hoàn thiện phiên bản kế hoạch chăm sóc; người có quyền duyệt | Kế hoạch: Nháp → Chờ duyệt → Hiệu lực, sớm nhất từ ngày hôm sau ngày duyệt | UC-22, UC-23, BR-M04-19, Q-34 |
| 17 | — | Chuyển sang vòng ca hằng ngày (BF-02) | — | — |

**Nhánh ngoại lệ**

| Tình huống | Vai trò | Xử lý | Căn cứ |
| ---------- | ------- | ----- | ------ |
| Gia đình từ chối giường được đề xuất nhưng vẫn muốn chờ | Hành chính | Hồ sơ chờ về Đang chờ, giữ ngày đăng ký và điểm; giường được đề xuất cho người tiếp theo | 6.2, Q-29 |
| Hết hạn giữ chỗ [48 giờ] mà gia đình không phản hồi | Hệ thống | Giường về Trống, hồ sơ chờ về Đang chờ, hệ thống đề xuất người tiếp theo | BR-M02-02 |
| Gia đình không tiếp tục chờ | Hành chính | Hồ sơ chờ chuyển Từ chối hoặc Hủy chờ; trong cùng lần, lượt đăng ký chuyển Đã hủy và hồ sơ người cao tuổi chuyển Hủy tiếp nhận với cùng lý do | 6.2, Q-29, spec 004 FR-002 |
| Hủy tiếp nhận trực tiếp | Hành chính | Bắt buộc lý do. Hủy giữ chỗ giường, đóng hồ sơ chờ. Hợp đồng Nháp hoặc Chờ ký chuyển Đã hủy; hợp đồng đã Hiệu lực chuyển Chấm dứt, phí tới hết ngày hủy được giữ | 5.6, 6.3, Q-27 |
| Muốn vào ở sớm hơn ngày bắt đầu hợp đồng | Hành chính; Quản lý viện | Hoàn tất tiếp nhận bị chặn; phải có phụ lục đổi ngày bắt đầu qua yêu cầu thay đổi lưu trú (BF-08) | 6.3, Q-22 |
| Giường đặt trước chưa về Trống lúc Hoàn tất tiếp nhận | Hành chính | Lệnh bị chặn, trừ khi chuyển phân bổ sang một giường Trống khác ngay trong lệnh | 7.3, Q-50 |
| Chưa có bản đồng ý chia sẻ dữ liệu | Hệ thống | Không chặn; cảnh báo khi Hoàn tất tiếp nhận và nhắc Hành chính mỗi [1 ngày] (CFG-M01-06) | 5.1 |

**Điểm đã làm rõ**

- Kênh đăng ký là liên hệ trực tiếp; Hành chính nhập vào hệ thống. Cổng người thân không có chức năng đăng ký tiếp nhận.
- Nếu tới lúc Hoàn tất tiếp nhận mà phiếu đối chiếu chưa được xác nhận: người cao tuổi chưa có liều nào, và hạn đối chiếu [4 giờ] tính từ lúc Hoàn tất tiếp nhận (Q-57, BR-M07-09).
- Nếu tới lúc Hoàn tất tiếp nhận mà kế hoạch chăm sóc vẫn ở Nháp hoặc Chờ duyệt: lệnh **không bị chặn**. Người cao tuổi mang dấu "chưa có kế hoạch chăm sóc hiệu lực", Trưởng tầng và Điều dưỡng phụ trách thấy dấu này. Công việc vẫn được sinh từ lịch đo, lịch bữa và hoạt động. Việc gấp dùng công việc phát sinh (spec 005 FR-009, Q-34).
- Chưa có bản gán chế độ ăn cũng không chặn tiếp nhận: người cao tuổi dùng chế độ ăn mặc định và dinh dưỡng viên được nhắc (12.1).

---

## BF-02 – Vòng ca chăm sóc hằng ngày

| Bước | Vai trò | Hoạt động | Trạng thái / kết quả | Căn cứ |
| ---- | ------- | --------- | -------------------- | ------ |
| 1 | Bộ lập lịch | Vào CFG-M04-01, sinh công việc cho ngày hoặc ca tới từ: phiên bản kế hoạch Hiệu lực, lịch đo (Bác sĩ đặt), lịch bữa (feature 011), buổi hoạt động đã đăng ký, lịch vệ sinh, nguồn tự động (theo dõi sau ngã…). Liều thuốc do Module 07 sinh riêng, chỉ hiển thị chung trong checklist | Công việc: Chưa đến hạn | BR-M04-01, spec 005 FR-017, spec 011 FR-042 |
| 2 | Bộ lập lịch | Với bán trú: tạo trạng thái có mặt theo lịch đến | Có mặt: Chưa đến | 3.4, spec 005 FR-012 |
| 3 | Hành chính hoặc Nhân viên chăm sóc | Điểm danh bán trú đến; hệ thống sinh công việc từ giờ đến tới giờ về dự kiến | Có mặt: Chưa đến → Có mặt | UC-28, BR-M04-02, spec 005 FR-013 |
| 4 | Nhân viên chăm sóc, Điều dưỡng, Nhân viên vệ sinh | Nhận checklist đầu ca; ghi nhận thực hiện theo loại công việc | Công việc: Đến hạn → Hoàn thành / Không thực hiện (bắt buộc lý do) | UC-25, UC-26, 8.5, 8.6 |
| 5 | Hệ thống | Hết khung thời gian: chuyển Quá hạn, nhắc người thực hiện. Việc Thường: báo Người phụ trách ca sau CFG-M04-12. Việc Quan trọng: gửi thông báo cho Trưởng tầng (không tạo bản ghi cảnh báo). Việc Bắt buộc: tạo cảnh báo mức Trung bình (BF-06) | Công việc: Quá hạn → Hoàn thành trễ / Không thực hiện | BR-M04-05, 06, Q-31 |
| 6 | Trưởng tầng; Người phụ trách ca | Xử lý việc quá hạn, phân lại việc chung. Người phụ trách ca chỉ ghi thay việc đã Quá hạn | — | UC-27, Q-35 |
| 7 | Hệ thống | Xét chọn ngẫu nhiên [5%] (CFG-M04-11) ngay khi mỗi công việc Hoàn thành hoặc Hoàn thành trễ; tại mốc CFG-M09-04 trước khi kết ca, chọn bổ sung nếu chưa đủ tỷ lệ (làm tròn lên, tối thiểu 1) | Danh sách kiểm tra chất lượng | BR-M04-23, Q-163 |
| 8 | Trưởng tầng được giao của tầng | Kiểm tra lại, ghi Đạt / Không đạt trước hết ca. Không đạt thì tạo công việc làm lại trong chính ca đó. Hết ca chưa kiểm tra thì mục thành "Quá hạn kiểm tra" và Quản lý viện được báo | — | BR-M04-23 |
| 9 | Hành chính hoặc Nhân viên chăm sóc | Điểm danh bán trú về, qua quy trình đón; công việc sau giờ về chuyển Hủy | Có mặt: Có mặt → Đã về | 14.3, spec 005 FR-019 |
| 10 | Hệ thống; Người phụ trách ca (Trưởng tầng hoặc Điều dưỡng) | Công việc, cảnh báo còn mở vào bản nháp bàn giao; Người phụ trách ca lập bàn giao | — | BR-M04-07, BR-M09-06 |
| 11 | Người phụ trách ca sau (Trưởng tầng hoặc Điều dưỡng); không có thì Trưởng tầng | Xác nhận bàn giao. Việc Thường vẫn Quá hạn lúc đó tự chuyển Không thực hiện | — | UC-53, Q-32, Q-37 |

**Điểm đã làm rõ**

- "Lịch ăn" là **lịch bữa** do feature 011 cung cấp cho từng người (bữa, giờ dự kiến, có suất đặc biệt hay không). Giờ bữa là CFG-M08-04, do Quản lý viện cấu hình.
- "Lịch sinh hoạt" không phải nguồn sinh công việc. Thời khóa biểu cá nhân (8.2) chỉ để xem, tổng hợp từ công việc, liều thuốc, buổi hoạt động và lịch thăm (spec 005 US9).
- Người bán trú quên điểm danh về: cuối ngày Người phụ trách ca được báo mức Trung bình, Trưởng tầng mức Nhẹ; trạng thái có mặt không tự chuyển (spec 005 FR-019).

---

## BF-03 – Lập và công bố thực đơn tuần

| Bước | Vai trò | Hoạt động | Trạng thái / kết quả | Căn cứ |
| ---- | ------- | --------- | -------------------- | ------ |
| 1 | Dinh dưỡng viên | Quản lý danh mục chế độ ăn (có món an toàn bắt buộc) và danh mục món ăn: thành phần gây dị ứng (từ danh mục dị nguyên), chế độ ăn phù hợp, kết cấu chế biến được | Danh mục (nhóm 1) | 12.1, spec 011 US2, FR-004 |
| 2 | Dinh dưỡng viên | Lập thực đơn tuần (thứ 2 → chủ nhật): mỗi ngày, bữa, chế độ ăn gồm các món và món thay thế theo thứ tự ưu tiên | Thực đơn: Nháp | 12.2, Q-142 |
| 3 | Dinh dưỡng viên; Hệ thống | Gửi duyệt. Hệ thống kiểm tra độ phủ: thiếu món thì chặn. Hệ thống cảnh báo lặp món [3 ngày] và món có thành phần dị ứng chưa có món thay thế | Qua kiểm tra: Chờ duyệt, nghĩa là "đã qua kiểm tra, chờ công bố" | BR-M08-06, 07 |
| 4 | Dinh dưỡng viên | Rút lại nếu cần sửa | Chờ duyệt → Nháp | 12.2 |
| 5 | Dinh dưỡng viên | Tự công bố; kiểm tra độ phủ chạy lại với dữ liệu tại thời điểm công bố | Chờ duyệt → Công bố. Nếu tuần đã bắt đầu: chuyển thẳng Đã áp dụng | 12.2, Q-142 |
| 6 | Hệ thống | Tới ngày đầu tuần | Công bố → Đã áp dụng | 12.2 |
| 7 | Dinh dưỡng viên | Đổi món bằng lệnh có lý do, hiệu lực ngay. Bếp được báo; đổi sau thời điểm chốt suất thì thành phát sinh | Thực đơn không bị hủy | BR-M08-08 |
| 8 | Bộ lập lịch | Tới [2 ngày] trước ngày đầu tuần (CFG-M08-07) mà chưa có thực đơn Công bố: nhắc Dinh dưỡng viên, báo Quản lý viện | — | BR-M08-16 |

**Điểm đã làm rõ**

- Thực đơn **không có người duyệt thủ công** và **không có bước trả lại**. Chỉ có Rút lại (Chờ duyệt → Nháp) và Hủy (Nháp → Đã hủy).
- Quyền duyệt của Bác sĩ chỉ áp cho gán chế độ ăn (UC-45). Bác sĩ và Quản lý viện chỉ xem thực đơn và các cảnh báo đã được xác nhận (Q-142, 19.3).

---

## BF-04 – Chuẩn bị và phân phối suất ăn

| Bước | Vai trò | Hoạt động | Trạng thái phiếu / kết quả | Căn cứ |
| ---- | ------- | --------- | -------------------------- | ------ |
| 1 | Hệ thống | Trước bữa [2 giờ], chốt số suất theo chế độ ăn. Chọn món thay thế theo thứ tự ưu tiên; không có món phù hợp thì dùng món an toàn hoặc báo Dinh dưỡng viên. Sinh phiếu bữa ăn theo tầng/khu | Đã chốt | BR-M08-01, 02, 09 |
| 2 | Nhân viên bếp | Chuẩn bị suất; dán nhãn suất đặc biệt (họ tên, phòng, chế độ ăn); đánh dấu từng suất đặc biệt đã chuẩn bị | Đã chốt → Đã chuẩn bị (có phát sinh làm thêm hoặc đổi suất đặc biệt thì quay về Đã chốt) | 12.5, BR-M08-10 |
| 3 | Nhân viên bếp | Xác nhận đã nhận các phát sinh sau thời điểm chốt. Chưa xác nhận trước giờ bữa [30 phút] thì nhắc Bếp và Dinh dưỡng viên | — | BR-M08-13 |
| 4 | Nhân viên bếp | Ghi bản ghi lưu mẫu của bữa: mọi món được nấu, mỗi món một mẫu; món không lưu được thì ghi lý do | Bắt buộc có bản ghi trước bước 5 | BR-M08-15, Q-39, Q-146 |
| 5 | Nhân viên bếp | Giao phiếu và suất tới tầng/khu | Đã chuẩn bị → Đã giao | BR-M08-10 |
| 6 | Người nhận tại tầng | Kiểm đếm, rồi xác nhận nhận hoặc báo sai lệch kèm nội dung | Đã giao → Đã nhận (trạng thái cuối) / Có sai lệch | BR-M08-11 |
| 7 | Nhân viên bếp; Người nhận tại tầng | Bếp ghi cách xử lý (bổ sung, đổi suất, khác); tầng xác nhận lại | Có sai lệch → Đã giao → Đã nhận. Lịch sử sai lệch giữ nguyên | 12.5 |
| 8 | Nhân viên bếp; Người nhận tại tầng | Phát sinh tới sau khi phiếu Đã giao được giao như phần bổ sung, nhận hoặc báo sai lệch riêng | Phiếu không quay lại trạng thái trước | 12.5 |
| 9 | Nhân viên chăm sóc | Phục vụ. Với suất đặc biệt và người thuộc danh sách cần đối chiếu khi phục vụ: xác nhận đúng người, đúng suất trước khi ghi kết quả ăn uống. Suất có thành phần dị ứng với người nhận: hệ thống chặn và tạo sự cố mức Trung bình | — | BR-M08-14, spec 011 FR-036a, FR-050 |
| 10 | Hệ thống | Quá giờ bữa [30 phút] (CFG-M08-04) mà phiếu chưa Đã giao: cảnh báo nhẹ cho Trưởng tầng và Bếp | — | BR-M08-12 |

**Điểm đã làm rõ**

- **Người nhận tại tầng** là Trưởng tầng, Điều dưỡng hoặc Nhân viên chăm sóc (kể cả Người phụ trách ca) có phân công tại tầng/khu của phiếu trong ca đang diễn ra. Với phiếu khu bán trú: người thuộc các vai trò đó được phân công tại tầng/khu vực mà khu nghỉ bán trú gắn vào (Q-143, 2.4).
- **Kết cấu thức ăn** lưu trên bản gán chế độ ăn, do Dinh dưỡng viên nhập. Muốn đổi kết cấu phải tạo bản gán mới; đổi sang kết cấu cứng hơn khi đang dùng chế độ ăn liên quan điều trị thì cần Bác sĩ duyệt (12.1, BR-M08-03).
- **Suất thường và dị ứng**: theo định nghĩa, suất có món trùng dị ứng đang hiệu lực đã là suất đặc biệt, nên suất thường không có xung đột dị ứng đã biết. Người có dị ứng loại "khác" (không kiểm tra tự động) vẫn nhận suất thường nhưng nằm trong danh sách cần đối chiếu khi phục vụ, và phải được xác nhận như suất đặc biệt. Danh sách này bếp không thấy. Người có suất thường và không thuộc danh sách thì không cần xác nhận.

---

## BF-05 – Vệ sinh trả giường và khử khuẩn khoanh vùng

| Bước | Vai trò | Hoạt động | Trạng thái / kết quả | Căn cứ |
| ---- | ------- | --------- | -------------------- | ------ |
| 1 | Hệ thống | Một phân bổ giường kết thúc (chuyển giường, kết thúc lưu trú, qua đời, giải phóng sau giữ chỗ): sinh công việc vệ sinh trả giường, mức Quan trọng, hạn [4 giờ] (CFG-M03-04). Người rời giường thuộc danh sách nghi nhiễm hoặc tiếp xúc: thay bằng khử khuẩn (Bắt buộc, xác nhận đồ bảo hộ) | Giường: Chờ vệ sinh | BR-M03-09, 10, Q-49, Q-53 |
| 2 | Hành chính (hoặc Trưởng tầng) | Có thể tạo phân bổ tương lai cho giường Chờ vệ sinh, bắt đầu không sớm hơn hạn vệ sinh | — | 7.2, Q-40 |
| 3 | Nhân viên vệ sinh | Ghi kết quả từng hạng mục Đạt / Không đạt, hư hỏng, ghi chú | Công việc: Hoàn thành | 7.5, spec 003 FR-048 |
| 4 | Hệ thống | Vệ sinh trả giường Hoàn thành | Giường: Chờ vệ sinh → Trống; kích hoạt BR-M02-01 | BR-M03-09, BR-M03-06 |
| 5 | Hệ thống; Trưởng tầng hoặc Quản lý viện | Có hạng mục Không đạt hoặc hư hỏng: báo Trưởng tầng. Hư hỏng liên quan giường: Trưởng tầng hoặc Quản lý viện gắn hư hỏng với tài sản của giường rồi Báo hỏng hoặc Đưa vào bảo trì (7.7, Q-234); giường đang có người thì hư hỏng mang dấu "chờ chuyển người" (Q-232, Q-235) | Tài sản: Hỏng / Đang bảo trì; giường: Đang bảo trì (nếu không có người; phân bổ tương lai chuyển Đã hủy) | BR-M03-13, BR-M03-05, BR-M03-15, BR-M03-18, Q-51, Q-234 |
| 6 | Hệ thống | Vệ sinh trả giường bị hủy hoặc Không thực hiện: sinh ngay công việc thay thế, hạn tính lại, báo Trưởng tầng | — | Q-41 |
| 7 | Bác sĩ hoặc Quản lý viện | Đặt khoanh vùng lây nhiễm (một hoặc nhiều tầng, hoặc cả khu vực) | Vùng: Đang khoanh vùng | UC-37, 9.6, Q-69, spec 007 bảng trạng thái vùng |
| 8 | Hệ thống | Chặn thăm mới, hoạt động chung, phân bổ giường mới trong khu; sinh khử khuẩn [2 lần/ngày] (CFG-M03-05) cho phòng và khu vực chung trong vùng | — | BR-M05-11, BR-M03-11 |
| 9 | Bác sĩ hoặc Quản lý viện | Gỡ khoanh vùng, có lý do; hệ thống bỏ các chặn, sinh một lần khử khuẩn kết thúc | Vùng: Đã gỡ | BR-M05-12, BR-M03-11 |

**Điểm đã làm rõ**

- **Hạng mục Không đạt không liên quan giường**: hệ thống chỉ báo Trưởng tầng. Kết quả Không đạt không tự đổi trạng thái công việc và không tự sinh việc làm lại. Việc làm lại chỉ sinh khi kiểm tra chất lượng cho kết quả Không đạt (spec 003 FR-048, FR-049a). Trưởng tầng có thể tự tạo yêu cầu vệ sinh đột xuất (BR-M03-12).
- **Trả giường có hạng mục Không đạt**: công việc vẫn Hoàn thành và giường về Trống. Chỉ khi có hư hỏng liên quan giường và Trưởng tầng quyết định (lệnh trên tài sản, Q-234) thì giường mới vào bảo trì. Giường mang dấu "chờ chuyển người" về Trống thì không được phân bổ, không kích hoạt BR-M02-01 cho tới khi báo hỏng lại hoặc gỡ dấu (Q-235).
- Người đặt khoanh vùng và người gỡ khoanh vùng là cùng hai vai trò: Bác sĩ, Quản lý viện.
- Q-40 đã chốt (24.2).

---

## BF-06 – Cảnh báo, leo thang và sự cố

| Bước | Vai trò | Hoạt động | Trạng thái / kết quả | Căn cứ |
| ---- | ------- | --------- | -------------------- | ------ |
| 1 | Hệ thống; nhân viên | Tạo cảnh báo từ: chỉ số vượt ngưỡng, công việc Bắt buộc quá hạn, liều bỏ lỡ, từ chối thuốc, ăn kém, xu hướng. Cùng người, cùng loại đang mở thì gộp | Cảnh báo: Mới | 9.1, BR-M05-02, 04, BR-M06-03 |
| 2 | Hệ thống | Xác định hạn tiếp nhận: Nhẹ là hết ca hiện tại của tầng; Trung bình là [15 phút] (CFG-M05-01); Khẩn cấp là ngay lập tức | — | 9.3, spec 007 FR-031 |
| 3 | Điều dưỡng phụ trách (cấp 0) | Tiếp nhận. Nhân viên thực hiện công việc nguồn chỉ được thông báo | Mới → Đã tiếp nhận | BR-M05-01, spec 007 FR-033 |
| 4 | Bộ lập lịch | Quá hạn chưa tiếp nhận: lên **cấp 1**, gồm Trưởng tầng của tầng người cao tuổi **và** Bác sĩ trực, được báo đồng thời, một trong hai tiếp nhận là đủ. Tiếp tục quá hạn: lên **cấp 2**, Quản lý viện. Cấp không có người trong ca thì bỏ qua | Mới → Leo thang → Đã tiếp nhận | BR-M05-01, UC-38, Q-71 |
| 5 | Quản lý viện | Ở cấp 2: tiếp nhận rồi giao người phụ trách; không xử lý, không đóng | — | Q-71 |
| 6 | Điều dưỡng, Bác sĩ, Trưởng tầng | Xử lý; đóng kèm kết quả, hoặc chuyển thành sự cố | Đã tiếp nhận → Đang xử lý → Đã đóng / Chuyển sự cố | 9.4 |
| 7 | Mọi nhân viên | Ghi nhận sự cố; kích hoạt quy trình khẩn cấp (miễn kiểm tra phạm vi dữ liệu); hệ thống hiển thị thẻ thông tin khẩn cấp | Sự cố: Mới | UC-35, 36, BR-M05-13, BR-M15-01 |
| 8 | Hệ thống | Sự cố Khẩn cấp: báo đồng thời Bác sĩ trực, Trưởng tầng, Quản lý viện, người liên hệ chính. Không có Bác sĩ trực: báo mọi Bác sĩ đang hoạt động | — | BR-M05-06, Q-74 |
| 9 | Hệ thống | Sự cố ngã: tạo yêu cầu đánh giá lại, sinh công việc theo dõi sau ngã, tạo yêu cầu xem xét kế hoạch chăm sóc | — | BR-M05-07, BR-M04-20 |
| 10 | Điều dưỡng, Bác sĩ | Cần chuyển viện: hệ thống tự thực hiện lệnh Chuyển viện, tạo bản tóm tắt | Người cao tuổi: Điều trị tại bệnh viện | BR-M05-14 |
| 11 | Điều dưỡng hoặc Bác sĩ | Đóng sự cố kèm kết quả. Sự cố mức Trung bình trở lên cần xác nhận của Điều dưỡng hoặc Bác sĩ | Sự cố: Đã đóng | BR-M05-09 |

**Nhánh ngoại lệ**

| Tình huống | Vai trò | Xử lý | Căn cứ |
| ---------- | ------- | ----- | ------ |
| Nguồn của cảnh báo đang mở đã được xử lý (công việc hoàn thành đúng khung, liều được đính chính Đã dùng, phiếu đối chiếu được xác nhận) | Hệ thống | Cảnh báo tự chuyển Đã đóng với kết quả "nguồn đã được xử lý" | 9.4, spec 007 FR-032 |
| Nguồn của cảnh báo đã Chuyển sự cố được xử lý | Hệ thống | Cảnh báo giữ Chuyển sự cố; hệ thống thêm diễn biến "nguồn đã được xử lý" vào sự cố và báo người xử lý; sự cố không tự đóng | Q-76 |
| Cảnh báo mức Khẩn cấp | Hệ thống; người tiếp nhận | Không tự tạo sự cố; báo đồng thời Điều dưỡng phụ trách, Người phụ trách ca, Bác sĩ trực, Trưởng tầng. Người tiếp nhận chọn "Kích hoạt khẩn cấp từ cảnh báo" khi cần | 9.4, Q-67 |
| Cảnh báo Trung bình lặp lại [3] lần trong 24 giờ | Hệ thống | Đề xuất nâng lên mức Khẩn cấp | BR-M05-03 |
| Sự cố bị "Hủy ghi nhận" | Người được đính chính | Sự cố chuyển Đã hủy. Tác động tự động đã tạo không tự thu hồi; Điều dưỡng phụ trách và Bác sĩ nhận danh sách để đóng bằng lệnh riêng | 9.4, Q-73 |
| Đổi loại sự cố sang Ngã (hoặc lây nhiễm) / khỏi Ngã | Người được đính chính | Sang Ngã: tạo ngay tác động của loại mới. Khỏi Ngã: xử lý như hủy | Q-73 |
| Người cao tuổi chuyển trạng thái cuối khi cảnh báo còn mở | Hệ thống | Dừng leo thang và nhắc; cảnh báo được liệt kê để đóng | spec 007 FR-038 |
| **(Bổ sung, 2026-09-28)** Bác sĩ nhận định người cao tuổi nguy kịch | Bác sĩ; Hệ thống | Ghi dấu nguy kịch; hệ thống tạo cảnh báo Khẩn cấp "nguy kịch – thực hiện nguyện vọng cuối đời", không gộp. Xử lý theo BF-17 | BR-M05-15, Q-213 |

**Điểm đã làm rõ**

- Bước leo thang "trưởng tầng hoặc bác sĩ" không cần quy tắc chọn người: cả hai được báo cùng lúc, ai tiếp nhận trước là đủ (spec 007 FR-033).
- Cảnh báo mức Nhẹ chưa tiếp nhận khi hết ca: vào bản nháp bàn giao (BR-M05-05) và lên một cấp.
- Cảnh báo mức Khẩn cấp không leo thang theo cấp: mọi người nhận được báo đồng thời (Q-67).

---

## BF-07 – Thuốc: đơn, liều và đối chiếu

| Bước | Vai trò | Hoạt động | Trạng thái / kết quả | Căn cứ |
| ---- | ------- | --------- | -------------------- | ------ |
| 1 | Bác sĩ (kê đơn nội bộ khi có giấy phép); Bác sĩ hoặc Điều dưỡng (nhập đơn kê bên ngoài) | Nhập đơn. Hệ thống kiểm tra trùng hoạt chất và dị ứng; có vấn đề thì cảnh báo, chỉ tiếp tục khi có lý do | Đơn: Hiệu lực | UC-39, BR-M07-05, 06, BR-M06-05 |
| 2 | Điều dưỡng | Tiếp nhận thuốc gia đình gửi | Thuốc gia đình gửi: Chờ đối chiếu → Được sử dụng / Chỉ giữ hộ | UC-44, 11.4 |
| 3 | Bộ lập lịch | Sinh liều cho [2 ngày] tới | Liều: Chưa đến giờ | BR-M07-01 |
| 4 | Điều dưỡng (liều chung của tầng: mọi Điều dưỡng có ca tại tầng) | Xác nhận liều | Đến giờ → Đã dùng / Từ chối / Không thực hiện | UC-41, Q-61 |
| 5 | Hệ thống | Quá cửa sổ: chuyển Trễ, nhắc. Quá thêm [30 phút]: chuyển Bỏ lỡ, tạo cảnh báo mức Trung bình (BF-06) | Trễ → Bỏ lỡ | BR-M07-02 |
| 6 | Hệ thống | Người cao tuổi trở về từ bệnh viện: mọi đơn cũ Tạm dừng; tạo phiếu đối chiếu và yêu cầu đánh giá lại | Liều: Tạm dừng | 5.6, BR-M07-09 |
| 7 | Bác sĩ hoặc Điều dưỡng | Lập phiếu đối chiếu: mỗi thuốc chọn Tiếp tục / Ngừng / Thay đổi liều / Thêm mới | Phiếu: chờ xác nhận | 11.5, UC-43 |
| 8 | Một Bác sĩ hoặc Điều dưỡng khác người lập | Xác nhận (quy tắc hai người). Phiếu có dòng đơn nội bộ chỉ Bác sĩ có quyền kê đơn xác nhận | Phiếu: đã xác nhận; lịch thuốc chạy lại | Q-55 |
| 9 | Hệ thống | Quá [4 giờ] chưa đối chiếu: tạo cảnh báo mức Trung bình | — | BR-M07-09 |

**Điểm đã làm rõ**

- Người thực hiện và người xác nhận phiếu đối chiếu đã có quy định ở 11.5 (Q-55), áp như nhau khi tiếp nhận (BF-01 bước 12) và khi trở về từ bệnh viện. Sơ đồ có thể vẽ đủ các bước lập, xác nhận và chạy lại lịch thuốc.

---

## BF-08 – Thay đổi lưu trú

| Bước | Vai trò | Hoạt động | Trạng thái / kết quả | Căn cứ |
| ---- | ------- | --------- | -------------------- | ------ |
| 1 | Hành chính, Bác sĩ; Hệ thống; Người đại diện | Lập yêu cầu thay đổi: loại lưu trú, mức chăm sóc, dịch vụ, thời gian, phòng/giường. Hệ thống tự lập khi đánh giá lại ra mức khác. Người đại diện gửi qua cổng thì yêu cầu ở Nháp để Hành chính hoàn thiện | Yêu cầu: Nháp → Chờ duyệt | UC-13, 6.6, BR-M01-03 |
| 2 | Bộ lập lịch | Quá [48 giờ] chưa duyệt: nhắc người duyệt. Quá [96 giờ]: báo Quản lý viện. Không tự hủy | — | 6.6, CFG-M15-05, 06 |
| 3 | Quản lý viện | Duyệt hoặc từ chối | Đã duyệt (chờ hiệu lực), tạo phụ lục Chờ hiệu lực / Từ chối | UC-14, spec 004 FR-042 |
| 4 | Bộ lập lịch | Tới ngày hiệu lực: áp dụng, gồm đổi mức chăm sóc hiện hành, đổi đơn giá, chuyển giường, đổi ngày kết thúc, đổi dịch vụ; **kích hoạt xem xét kế hoạch chăm sóc**. Điều kiện không còn thỏa thì chuyển Áp dụng không thành | Yêu cầu: Đã áp dụng / Áp dụng không thành. Phụ lục: Đã áp dụng | BR-M02-04, Q-11, spec 004 FR-042 |
| 5 | Hệ thống | Tạo yêu cầu xem xét kế hoạch chăm sóc cho Điều dưỡng phụ trách, hạn [48 giờ] | — | BR-M04-20, spec 005 FR-011 |
| 6 | Điều dưỡng; người có quyền Duyệt kế hoạch chăm sóc | Điều dưỡng lập phiên bản mới; người có quyền duyệt | Kế hoạch: Nháp → Chờ duyệt → Hiệu lực, từ ngày hôm sau ngày duyệt | BR-M04-19, Q-34 |

**Điểm đã làm rõ**

- "Đổi kế hoạch chăm sóc" ở BR-M02-04 **không** tạo phiên bản kế hoạch tự động. Khi áp dụng thay đổi, hệ thống chỉ đổi mức chăm sóc hiện hành và tạo yêu cầu xem xét cho Điều dưỡng phụ trách. Phiên bản mới vẫn do Điều dưỡng lập và người có quyền duyệt (BR-M04-19, 20). Cho tới lúc đó, kế hoạch cũ vẫn áp dụng (BR-M01-03).

---

## BF-09 – Chốt chi phí kỳ

| Bước | Vai trò | Hoạt động | Trạng thái / kết quả | Căn cứ |
| ---- | ------- | --------- | -------------------- | ------ |
| 1 | Hệ thống | Tự tạo chi phí nháp từ sự kiện nguồn, có tham chiếu bản ghi nguồn | Khoản: Nháp | BR-M11-01, 15.2 |
| 2 | Hành chính | Lập khoản nhập tay, mua hộ, điều chỉnh (bắt buộc lý do) | Khoản: Nháp | BR-M11-04, UC-63 |
| 3 | Hệ thống; Hành chính | Hủy khoản Nháp: hệ thống hủy khi bản ghi nguồn bị hủy hoặc khoản nằm ngoài thời gian tính phí; Hành chính chỉ hủy khoản nhập tay, mua hộ, điều chỉnh do chính mình lập | Nháp → Đã hủy | spec 010 bảng trạng thái khoản, BR-M11-05 |
| 4 | Hành chính | Kiểm tra (được Bỏ kiểm tra khi bảng còn Đang mở) | Nháp → Đã kiểm tra | 15.6, UC-61 |
| 5 | Quản lý viện | Duyệt từng khoản hoặc hàng loạt. Khoản nhập tay, mua hộ, điều chỉnh do Hành chính lập phải duyệt từng khoản | Đã kiểm tra → Đã duyệt | 15.6 |
| 6 | Hành chính | Gửi chốt bảng chi phí của từng người cao tuổi | Bảng: Đang mở → Chờ chốt | Q-131 |
| 7 | Quản lý viện | Chốt hoặc trả lại. Bảng còn khoản Nháp hoặc chưa duyệt thì không chốt được | Bảng: Chờ chốt → Đã chốt. Khoản: Đã chốt | BR-M11-06, UC-62 |
| 8 | Hệ thống | Mọi bảng của kỳ đã chốt | Kỳ của viện: Đã chốt. Hạn chốt là CFG-M11-01 | 15.6, Q-132 |
| 9 | Kế toán (**đã chỉnh sửa, Q-212**; trước đây Hành chính) | Tải file Excel/CSV cho kế toán: toàn bộ bảng đã chốt của kỳ, hoặc chỉ bảng chưa từng xuất | Lần xuất được ghi | UC-64, 23, spec 010 FR-039 |
| 10 | Hệ thống | Sai sót sau chốt: tạo khoản điều chỉnh ở bảng chưa chốt hoặc bảng bổ sung, đi lại vòng đời từ bước 4 | — | BR-M11-05 |
| 11 | Hệ thống | **(Bổ sung, 2026-09-28)** Ngay khi một bảng (thường hoặc bổ sung) Đã chốt: tạo giao dịch "thanh toán bảng chi phí" trừ vào số dư (BF-16) | Số dư giảm (tăng nếu tổng bảng âm) | BR-M11-11, Q-211 |

**Điểm đã làm rõ**

- Người "Chốt" là **Quản lý viện** (Q-131, chú thích ¹⁷ của 4.4).
- **(Đã chỉnh sửa, Q-212)** Chỉ **Kế toán** tải file cho kế toán; Hành chính vẫn kiểm tra và gửi chốt.

---

## BF-10 – Kết thúc lưu trú

| Bước | Vai trò | Hoạt động | Trạng thái / kết quả | Căn cứ |
| ---- | ------- | --------- | -------------------- | ------ |
| 1 | Hành chính (hoặc Bác sĩ, với xuất viện, chuyển cơ sở vì lý do y tế) | Lập hồ sơ kết thúc lưu trú: trường hợp, ngày kết thúc dự kiến, lý do, nơi chuyển đến | Hệ thống dừng sinh chi phí sau ngày kết thúc dự kiến; tạo bảng kỳ cuối nháp | 6.8 bước 1, spec 004 FR-062, BR-M11-08 |
| 2 | Hệ thống | Hiển thị danh sách điều kiện: chi phí kỳ cuối đã chốt; không còn đồ gửi Đang giữ, Đang được sử dụng, Hư hỏng; không còn thuốc gia đình gửi hay lần giao thuốc mang theo chưa nhận lại; không còn cảnh báo, sự cố mở; đã bàn giao người cao tuổi | Từng điều kiện: Đạt / Chưa đạt | 6.8 bước 2, 5.6, Q-148, Q-64 |
| 3 | Hành chính; Quản lý viện | Hành chính kiểm tra, gửi chốt kỳ cuối; Quản lý viện duyệt và chốt (BF-09) | Điều kiện chi phí: Đạt | BR-M11-08, Q-21 |
| 4 | Hành chính, Điều dưỡng | Trả đồ gửi (đồ không có giá trị: Điều dưỡng được trả); hoàn trả thuốc gia đình gửi; Điều dưỡng ghi Nhận lại lần giao thuốc | Điều kiện đồ, thuốc: Đạt | 16.4, 19.3, Q-64 |
| 5 | Quản lý viện | Duyệt ngoại lệ cho bốn điều kiện đầu nếu cần. Bàn giao người cao tuổi không có ngoại lệ | — | 6.8 bước 2 |
| 6 | Hành chính | Ghi nhận bàn giao người cao tuổi (người nhận, thời điểm, nhân viên bàn giao) | Điều kiện bàn giao: Đạt | 6.8 bước 2 |
| 6a | Kế toán; Quản lý viện | **(Bổ sung, 2026-09-28)** Sau khi kỳ cuối đã chốt và trừ số dư: hoàn hoặc cấn trừ tiền cọc; hoàn số dư dương hoặc thu đủ số dư âm. Hoàn tiền có hiệu lực khi Quản lý viện duyệt. Quản lý viện được duyệt ngoại lệ | Điều kiện "số dư và tiền cọc đã quyết toán": Đạt | 6.8, BR-M11-14, Q-222 |
| 6b | Hệ thống; Hành chính | **(Bổ sung, 2026-09-28)** Sau lệnh Kết thúc lưu trú ở bước 7: tạo việc "khai báo xóa tạm trú" cho Hành chính; không phải điều kiện kết thúc | Khai báo: Đã xác nhận → Đã xóa | BR-M02-13 |
| 7 | Hành chính | Thực hiện lệnh Kết thúc lưu trú trong ngày kết thúc dự kiến | Người cao tuổi: Kết thúc lưu trú. Hợp đồng: Kết thúc / Chấm dứt. Giường: Chờ vệ sinh (BF-05). Lịch tương lai bị hủy. Tài khoản người thân khóa sau [30 ngày] | UC-17, 6.8 bước 3, 5.6, BR-M03-09 |
| 8 | Hệ thống | Quá ngày kết thúc dự kiến mà lệnh chưa thực hiện được: hồ sơ mang dấu "quá ngày dự kiến"; sinh bù chi phí; điều kiện chi phí về Chưa đạt; Hành chính được nhắc hằng ngày đặt ngày mới | — | 6.8 bước 4, Q-26 |

**Nhánh ngoại lệ**

| Tình huống | Vai trò | Xử lý | Căn cứ |
| ---------- | ------- | ----- | ------ |
| Một trong bốn điều kiện đầu chưa đạt (chi phí, đồ gửi, thuốc, cảnh báo và sự cố) | Hành chính; Quản lý viện | Hành chính lập yêu cầu ngoại lệ kèm lý do; điều kiện chuyển "Đạt (ngoại lệ)" khi Quản lý viện duyệt. Bàn giao người cao tuổi không có ngoại lệ | 6.8, spec 004 FR-064 |
| Hủy hồ sơ kết thúc | Hành chính | Bắt buộc lý do; hồ sơ chuyển Đã hủy; sinh chi phí tự động tiếp tục theo spec 010 | 6.8, spec 004 FR-067, spec 010 FR-034 |
| Người cao tuổi qua đời khi hồ sơ kết thúc đang chuẩn bị | Hệ thống | Hồ sơ tự chuyển Đã hủy; các điều kiện chưa đạt chuyển sang danh sách việc sau qua đời (BF-11) | 6.8, spec 004 FR-067 |
| Kết thúc khi người cao tuổi đang Tạm vắng hoặc Điều trị tại bệnh viện | Hành chính | Lượt vắng đóng tại thời điểm kết thúc; người đã đón được ghi là người nhận trong bàn giao | spec 004 mục Edge Cases |
| Đổi ngày kết thúc dự kiến sau khi kỳ cuối đã chốt | Hành chính; Hệ thống | Chênh lệch thành khoản điều chỉnh mang dấu "ảnh hưởng kết thúc lưu trú"; điều kiện chi phí về Chưa đạt tới khi khoản đó được chốt | BR-M11-08, Q-134 |

**Điểm đã làm rõ**

- Lệnh Kết thúc lưu trú do **Hành chính thực hiện**, không phải hệ thống tự chạy. "Trong ngày kết thúc dự kiến" là ràng buộc thời điểm, không phải lịch tự động (UC-17, spec 004 US kịch bản 4).

---

## BF-11 – Ghi nhận qua đời

| Bước | Vai trò | Hoạt động | Trạng thái / kết quả | Căn cứ |
| ---- | ------- | --------- | -------------------- | ------ |
| 1 | Bác sĩ (mất tại viện); Bác sĩ hoặc Hành chính kèm giấy tờ bằng chứng (mất ngoài viện) | Thực hiện lệnh Ghi nhận qua đời: thời điểm, địa điểm, người phát hiện, người xác nhận, nguyên nhân nếu đã xác định. Lệnh không bị chặn bởi đồ gửi, chi phí hay sự cố | Người cao tuổi: Qua đời. Hồ sơ chỉ đọc | UC-18, 5.6, 6.8 |
| 2 | Hệ thống | Chấm dứt hợp đồng; giải phóng giường; hủy lịch tương lai; dừng sinh chi phí từ sau thời điểm qua đời (ngày qua đời vẫn tính phí trọn ngày) | Hợp đồng: Chấm dứt. Giường: Chờ vệ sinh | 6.8, Q-141, BR-M03-09 |
| 3 | Hệ thống | Thông báo người liên hệ chính, mức Khẩn cấp. Công việc Đến hạn, Quá hạn chuyển Không thực hiện với lý do trạng thái cuối | — | 5.6, Q-38, Q-97 |
| 4 | Hệ thống | Mở danh sách việc sau qua đời: xử lý đồ gửi, hoàn trả thuốc gia đình gửi và lần giao thuốc chưa nhận lại, chốt chi phí kỳ cuối, xử lý cảnh báo, sự cố mở; **(bổ sung, 2026-09-28)** quyết toán số dư và tiền cọc (Kế toán, Q-222). Nhắc Hành chính mỗi [1 ngày] (CFG-M02-09). Tạo việc khai báo xóa tạm trú (BR-M02-13) | — | 6.8, Q-64 |
| 5 | Theo từng mục: đồ gửi – Hành chính, Điều dưỡng; thuốc gia đình gửi và lần giao thuốc – Điều dưỡng; chi phí kỳ cuối – Hành chính gửi chốt, Quản lý viện chốt; cảnh báo, sự cố – Điều dưỡng, Trưởng tầng, Bác sĩ | Hoàn thành từng mục (các thao tác này được phép trên hồ sơ trạng thái cuối theo BR-M01-05). Hệ thống tự cập nhật trạng thái mục: Chưa hoàn thành / Hoàn thành / Không áp dụng | — | 6.8, BR-M01-05, spec 004 FR-071 |
| 6 | Hệ thống | Mọi mục Hoàn thành hoặc Không áp dụng | Hồ sơ lưu trú: Đã đóng, không mở lại | 6.8, spec 004 FR-072 |
| 7 | Người có quyền đính chính; Quản lý viện | Bổ sung nguyên nhân tử vong về sau bằng đính chính, có hiệu lực khi Quản lý viện duyệt | — | 6.8, BR-M01-05 |

**Điểm đã làm rõ**

- Không có ai "xác định" tại viện hay ngoài viện. Điều này **suy ra từ trạng thái hiện tại** của hồ sơ lúc ghi nhận:
  - Đang lưu trú: mất tại viện, chỉ Bác sĩ được ghi nhận.
  - Tạm vắng, Hoạt động bên ngoài hoặc Điều trị tại bệnh viện: mất ngoài viện. Bác sĩ hoặc Hành chính ghi nhận, bắt buộc kèm giấy tờ bằng chứng.
  - Hành chính ghi nhận khi hồ sơ đang ở Đang lưu trú thì hệ thống từ chối (spec 001 clarify, US kịch bản 11).
- Người chuyển viện hoặc qua đời trong chuyến đi ngoài viện được đánh dấu "rời đoàn", không bị tính là thiếu người (8.9).

---

## BF-12 – Người thân: quyền, thăm, đón, ở lại, phản hồi và bản tin

Khởi phát: Hành chính lập quan hệ người thân cho một người cao tuổi. Luồng gồm sáu phần nối tiếp theo nhu cầu của gia đình: A. quyền (bước 1–5); B. thăm (6–9); C. đón (10); D. ở lại (11–14); E. phản hồi (15–17); F. bản tin (18–19).

| Bước | Vai trò | Hoạt động | Trạng thái / kết quả | Căn cứ |
| ---- | ------- | --------- | -------------------- | ------ |
| 1 | Hành chính | Lập hồ sơ người thân và quan hệ; đặt người đại diện đầu tiên kèm bằng chứng (không cần duyệt); đặt người liên hệ chính | Quan hệ: Hiệu lực; mọi quyền mặc định tắt | 14.1, Q-127, DBR-02 |
| 2 | Hành chính | Ghi nhận phiếu đăng ký người thân có chữ ký người đại diện, kèm bản scan. Mỗi quyền ghi trên phiếu có hiệu lực ngay. Quyền xem sức khỏe chỉ bật khi bản đồng ý chia sẻ dữ liệu bao gồm người thân đó | Quyền: bật theo phiếu | 14.1, Q-126, BR-M01-08 |
| 3 | Hành chính (hoặc Quản lý viện) | Tạo tài khoản cổng sau khi xác minh danh tính trực tiếp hoặc gọi lại số đã đăng ký | Tài khoản người thân: hoạt động | 19.1, Q-16 |
| 4 | Người đại diện (qua cổng); Hành chính | Lập yêu cầu: thêm/bỏ người được phép đón, đổi quyền của một người thân, bật/tắt dấu "được tự về", thêm/thôi người đại diện | Yêu cầu: Chờ xác nhận; quyền cũ vẫn áp dụng, riêng yêu cầu **bỏ** người được phép đón có hiệu lực ngay | BR-M10-07, Q-118 |
| 5 | Người đại diện khác người bị tác động (qua cổng hoặc bản ký); hoặc Quản lý viện | Xác nhận hoặc duyệt. Yêu cầu chờ lâu được nhắc theo CFG-M10-11 | Chờ xác nhận → Hiệu lực / Từ chối / Hủy | BR-M10-07, Q-127 |
| 6 | Người thân có quyền "đăng ký thăm" (qua cổng); Hành chính (tại quầy) | Đăng ký thăm: ngày, khung giờ, người đi cùng (tối đa CFG-M10-06). Hệ thống kiểm tra khung giờ và sức chứa (CFG-M10-04), thời hạn đăng ký trước (CFG-M10-05), khoanh vùng, trạng thái người cao tuổi | Đạt: lượt thăm **Đã duyệt** ngay, giữ chỗ. Không đạt: từ chối kèm lý do, không tạo lượt | 14.2, BR-M10-02, Q-119 |
| 7 | Hành chính | Ghi Vào: kiểm tra lại khoanh vùng và trạng thái người cao tuổi; ghi danh sách người thực tế vào (không vượt số đã đăng ký) | Đã duyệt → Đã vào | 14.2, spec 012 FR-033 |
| 8 | Hành chính | Ghi Ra; quá giờ kết thúc khung cộng CFG-M10-07 mà chưa ghi thì được nhắc. Giờ ra thực tế không muộn hơn thời điểm ghi | Đã vào → Đã ra | 14.2, spec 012 FR-034 |
| 9 | Bộ lập lịch | Hết khung mà lượt chưa ghi Vào | Đã duyệt → Không đến | spec 012 FR-034 |
| 10 | Người có quyền lệnh nguồn: Hành chính, Trưởng tầng (Cho tạm vắng, đi chơi); Hành chính, Nhân viên chăm sóc (Điểm danh về bán trú); Hành chính (Kết thúc lưu trú) | Quy trình đón, trực tuyến: chọn người đón → kiểm tra thuộc danh sách được phép đón hoặc có ngoại lệ Hiệu lực → xác minh danh tính → ghi thời điểm, nhân viên bàn giao | Bản ghi đón (nhóm 3), làm căn cứ cho lệnh nguồn trong CFG-M10-09 | 14.3, BR-M10-03, Q-121, spec 012 FR-021 → FR-026 |
| 11 | Hành chính | Đăng ký lượt ở lại: người thân, vị trí, thời gian dự kiến, lý do, có đăng ký ăn hay không. Bị chặn khi phòng đang khoanh vùng hoặc cách ly, hoặc người cao tuổi không Đang lưu trú | Lượt ở lại: Chờ xác nhận; báo Trưởng tầng được giao | 14.4, spec 012 FR-040, FR-041 |
| 12 | Trưởng tầng được giao của tầng (không có thì Quản lý viện) | Xác nhận hoặc từ chối. Phòng có từ hai giường đang sử dụng: ghi đã hỏi ý kiến người cùng phòng và kết quả. Kiểm tra giới hạn CFG-M10-10 lượt chồng thời gian | Chờ xác nhận → Đã xác nhận / Từ chối | 14.4, Q-123, Q-128 |
| 13 | Hành chính, Trưởng tầng | Bắt đầu ở lại (kiểm tra lại điều kiện bước 11); Hành chính gia hạn được, có lý do | Đã xác nhận → Đang ở lại. Suất ăn tính vào số suất (BR-M08-01); mỗi đêm qua mốc 00:00 tạo một khoản chi phí | BR-M10-04, spec 012 FR-042, FR-043 |
| 14 | Hành chính, Trưởng tầng | Kết thúc ở lại | Đang ở lại → Đã kết thúc; dừng suất ăn và chi phí sau ngày kết thúc | spec 012 FR-045 |
| 15 | Người thân | Gửi phản hồi, khiếu nại. Hệ thống giao theo nhóm nội dung: chăm sóc, sinh hoạt, ăn uống, sức khỏe, thuốc cho Trưởng tầng được giao (không có thì Quản lý viện); chi phí, hợp đồng, khác cho Hành chính. Khiếu nại mặc định mức Cao, còn lại Thường | Phản hồi: Mới | 14.7, Q-122 |
| 16 | Người phụ trách | Nhận xử lý rồi trả lời kèm hướng xử lý. Hạn: mức Cao [24 giờ], Thường [72 giờ]; quá hạn thì leo thang lên Quản lý viện | Mới → Đang xử lý → Đã phản hồi | BR-M10-05 |
| 17 | Người gửi; Hệ thống | Người gửi đồng ý đóng, hoặc hệ thống tự đóng sau [7 ngày] (CFG-M10-02); người gửi mở lại được, có lý do, hạn tính mới | Đã phản hồi → Đóng / Mở lại | 14.7, spec 012 FR-061 |
| 18 | Bộ lập lịch | Theo lịch [thứ 2 hằng tuần], sinh bản nháp bản tin từ dữ liệu đã ghi nhận, lọc theo quyền của từng người thân | Bản tin: Chờ duyệt | 14.5, BR-M10-08 |
| 19 | Điều dưỡng phụ trách | Thêm nhận xét; có sự cố mức Trung bình trở lên trong kỳ thì bắt buộc phần giải thích; duyệt trong [48 giờ] | Chờ duyệt → Đã gửi; bản tin đã gửi không sửa | BR-M10-08, BR-M10-09, Q-129 |

**Nhánh ngoại lệ**

| Tình huống | Vai trò | Xử lý | Căn cứ |
| ---------- | ------- | ----- | ------ |
| Người đón không thuộc danh sách được phép đón, hoặc xác minh danh tính không đạt | Người thực hiện quy trình đón; Người đại diện; Quản lý viện | Lượt đón bị chặn, ghi "lần chặn đón", báo Trưởng tầng mức Nhẹ. Người thực hiện lập yêu cầu ngoại lệ đón; người đại diện xác nhận qua cổng hoặc Quản lý viện duyệt kèm lý do. Ngoại lệ dùng cho đúng một lượt đón trong CFG-M10-08 | BR-M10-03, spec 012 FR-023, FR-024 |
| Người bán trú có dấu "được tự về" đang bật | Hành chính, Nhân viên chăm sóc | Điểm danh về không cần người đón; vẫn ghi bản ghi đón loại "Tự về" với nhân viên tiễn. Dấu không bật được khi có cờ nguy cơ đi lạc | 14.1, 14.3, Q-125 |
| Bản ghi đón không được lệnh nguồn dùng trong CFG-M10-09 | Hệ thống | Bản ghi chuyển "Không dùng", không còn là căn cứ | spec 012 FR-027 |
| Người cao tuổi Tạm vắng, Điều trị tại bệnh viện, trạng thái cuối, hoặc bán trú báo vắng | Hệ thống | Lượt thăm Đã duyệt trùng thời gian vắng tự Hủy, báo người đăng ký | spec 012 FR-036 |
| Khu bị khoanh vùng | Hệ thống | Lượt thăm Đã duyệt của người cao tuổi trong vùng tự Hủy, trả chỗ, báo người đăng ký mức Trung bình; gỡ vùng không khôi phục | 14.2, Q-120, BR-M05-11 |
| Đổi khung thăm hoặc không đến nữa | Người đăng ký, Hành chính | Hủy lượt trước giờ bắt đầu khung (Hành chính: có lý do); đổi khung bằng hủy rồi đăng ký lại | spec 012 FR-035 |
| Người cao tuổi chuyển Tạm vắng, Điều trị tại bệnh viện hoặc trạng thái cuối khi có lượt Đang ở lại | Hệ thống; Hành chính, Trưởng tầng | Hành chính và Trưởng tầng được nhắc kết thúc lượt; lượt không tự kết thúc | spec 012 FR-044 |
| Lượt ở lại chưa bắt đầu không còn cần | Hành chính | Hủy lượt Chờ xác nhận hoặc Đã xác nhận, có lý do | spec 012 FR-045 |
| Bản tin quá CFG-M10-12 chưa duyệt | Trưởng tầng được giao; Hệ thống | Trưởng tầng được giao duyệt thay (vẫn bắt buộc phần giải thích). Tới khi bản nháp kỳ sau được sinh mà vẫn chưa duyệt: bản cũ chuyển Không gửi, báo Quản lý viện | BR-M10-08, Q-124 |

**Điểm đã làm rõ**

- Mọi quyền của người thân mặc định tắt khi lập quan hệ; quyền chỉ bật qua phiếu đăng ký có chữ ký người đại diện, hoặc qua yêu cầu BR-M10-07 (Q-126).
- Lượt thăm không có bước duyệt tay; trạng thái "Đăng ký" và "Từ chối" không còn dùng cho lượt thăm (Q-119).
- Người thực hiện quy trình đón là người có quyền thực hiện lệnh nguồn mà lượt đón phục vụ, không phải một vai trò riêng (Q-121).
- Phí người thân ở lại tính theo **đêm**, không theo ngày (Q-123, BR-M10-04).
- Trường hợp "viện đưa về" người bán trú **chưa có quy tắc** (14.3) và chưa có mã quyết định ở 24.1 `[NEEDS CLARIFICATION]`.

---

## BF-13 – Lịch ca, đổi ca, nghỉ đột xuất và bàn giao ca

Khởi phát: tới hạn sinh lịch ca tháng kế tiếp. Kết thúc: ca đóng sau khi bàn giao được xác nhận. Ranh giới feature: spec 015 sở hữu sinh lịch từ mẫu xoay ca, đổi ca, nghỉ đột xuất và phủ tối thiểu; spec 008 sở hữu lịch ca, các lệnh trên ca và bàn giao (Q-77).

| Bước | Vai trò | Hoạt động | Trạng thái / kết quả | Căn cứ |
| ---- | ------- | --------- | -------------------- | ------ |
| 1 | Quản lý viện; Trưởng tầng (tầng được giao) hoặc Quản lý viện | Quản lý viện cấu hình mẫu xoay ca và yêu cầu phủ tối thiểu; Trưởng tầng hoặc Quản lý viện lập nhóm xoay ca (nhóm toàn viện chỉ Quản lý viện) | Mẫu, nhóm xoay ca | 13.2, CFG-M09-07, Q-190 |
| 2 | Bộ lập lịch | Trước [15 ngày], sinh bản nháp lịch ca tháng từ mẫu xoay ca; không tạo ca rỗng; báo "thiếu ca"; áp nghỉ đột xuất dạng "theo ngày" đã duyệt | Lịch: Nháp; ca: Nháp | BR-M09-09, Q-189, Q-184 |
| 3 | Trưởng tầng (tầng được giao); Quản lý viện (lịch toàn viện) | Chỉnh lịch Nháp, soạn phân công; hệ thống tính tỷ lệ phục vụ theo ngưỡng CFG-M09-01 và trọng số CFG-M09-12, chỉ cảnh báo | Lịch: Nháp | BR-M09-02, Q-79, Q-205 |
| 4 | Người lập lịch | Công bố. Bị chặn khi còn ca thiếu người phụ trách, tầng chưa có Trưởng tầng được giao cho cả tháng, hoặc còn phân công không hợp lệ. Còn ca thiếu phủ hoặc thiếu ca vẫn công bố được, kèm lý do; mỗi ca thiếu phủ mở cảnh báo cho Trưởng tầng. Nhắc công bố theo CFG-M09-10 | Lịch, ca: Đã công bố; phân công soạn sẵn có hiệu lực và được kiểm tra lại | 13.2, 13.4, Q-86, Q-178, Q-82 |
| 5 | Người nhường ca; người nhận ca | Người nhường lập yêu cầu đổi ca (hai chiều hoặc nhận thay một chiều, cùng phạm vi); người nhận đồng ý | Yêu cầu đổi ca: Chờ người nhận đồng ý → Chờ duyệt | BR-M09-10, Q-179, Q-191 |
| 6 | Trưởng tầng được giao của phạm vi; Quản lý viện (ca toàn viện) | Duyệt: kiểm tra giấy phép, đào tạo (BR-M09-01), chồng giờ và giờ làm liên tục (BR-M09-03). Phân công của người nhường chuyển cho người nhận nếu đạt BR-M09-01, không đạt thì thành việc chung | Chờ duyệt → Đã áp dụng / Từ chối | BR-M09-10, Q-177, Q-187 |
| 7 | Nhân viên; Trưởng tầng lập thay cho nhân viên tầng mình; Quản lý viện lập thay cho ca toàn viện | Lập yêu cầu nghỉ đột xuất: "theo ca" cho ca đã công bố chưa bắt đầu, hoặc "theo ngày" cho tháng chưa công bố | Yêu cầu nghỉ: Chờ duyệt | 13.2, Q-184 |
| 8 | Người duyệt như bước 6 | Duyệt tất cả, duyệt một phần theo từng ca hoặc ngày, hoặc từ chối | Chờ duyệt → Đã áp dụng / Đã áp dụng một phần / Từ chối; người nghỉ ở "Nghỉ có duyệt", không tính phủ | 13.2, Q-182 |
| 9 | Hệ thống; Trưởng tầng | Ca không đạt phủ tối thiểu (do nghỉ, đổi ca, vắng ca, nghỉ việc, giấy phép hết hiệu lực): cảnh báo Trưởng tầng, gợi ý người đang không có ca, sắp theo số giờ đã làm trong tháng; Trưởng tầng gửi lời mời nhận ca thay | Người nhận lời mời đầu tiên được bổ sung ngay vào ca | BR-M09-11, Q-180, Q-183, Q-188 |
| 10 | Hệ thống | Tới giờ bắt đầu ca | Ca: Đã công bố → Đang diễn ra; phạm vi dữ liệu theo ca có hiệu lực từ [2 giờ] trước giờ ca (CFG-M15-07) | 13.2, BR-M15-02 |
| 11 | Trưởng tầng hoặc Người phụ trách ca của ca đó | Ghi nhận vắng ca, từ giờ bắt đầu ca trừ [2 giờ] tới giờ kết thúc ca; công việc của người vắng thành việc chung | Chỉ ảnh hưởng ca đó | 13.2, Q-80, Q-81, Q-91, BR-M04-13 |
| 12 | Hệ thống | Trước khi kết thúc ca [30 phút], tự lập bản nháp bàn giao: công việc chưa hoàn thành; liều Trễ, Bỏ lỡ, Từ chối, Mang theo chờ ghi nhận; chỉ số vượt ngưỡng; cảnh báo, sự cố đang mở; biến động người cao tuổi; vệ sinh Gấp chưa xong; đồ có giá trị do nhân viên hết ca đang giữ | Bàn giao: Bản nháp; mục tự rời danh sách khi nguồn đã kết thúc | BR-M09-06, Q-88, Q-158 |
| 13 | Người phụ trách ca (Trưởng tầng hoặc Điều dưỡng) | Hoàn tất bàn giao: nhận định chung và ghi chú cho từng mục nghiêm trọng (cảnh báo, sự cố mức Khẩn cấp hoặc Trung bình; liều Bỏ lỡ, Từ chối; công việc Bắt buộc đang Quá hạn). Tới giờ kết thúc mà chưa xong thì ca chuyển Chờ bàn giao | Bàn giao: Bản nháp → Đã lập; mục phát sinh sau đó vào phần "Phát sinh sau khi lập" | 13.5, Q-83, BR-M09-07 |
| 14 | Người phụ trách ca sau (không có thì Trưởng tầng của phạm vi) | Xác nhận bàn giao, hoặc gửi ý kiến về thiếu sót. Chưa xác nhận sau [30 phút] từ đầu ca thì nhắc Trưởng tầng | Đã lập → Đã xác nhận / Có ý kiến | 13.5, BR-M09-07, CFG-M09-09 |
| 15 | Hệ thống | Khi xác nhận: công việc tồn sang checklist ca mới; cảnh báo đang mở chuyển cho Điều dưỡng phụ trách người cao tuổi ở ca mới (không có thì Người phụ trách ca mới); việc Thường quá hạn còn là việc chung chưa ai nhận thì tự đóng | Ca trước: Đã đóng; bàn giao đã xác nhận không sửa được | BR-M09-08, BR-M05-05, Q-32, Q-93 |

**Nhánh ngoại lệ**

| Tình huống | Vai trò | Xử lý | Căn cứ |
| ---------- | ------- | ----- | ------ |
| Tới giờ bắt đầu ca sau mà bàn giao chưa được xác nhận | Người phụ trách ca sau (không có thì Trưởng tầng); Hệ thống | Cảnh báo, sự cố đang mở của người ca trước đã hết ca được tạm nhận; người tạm nhận là người nhận thông báo và bậc đầu của chuỗi leo thang. Công việc chưa đóng và liều chờ ghi nhận của họ tạm thành việc chung, liều chung của tầng | BR-M09-08, Q-78, Q-85, Q-94 |
| Bàn giao ở Có ý kiến | Người phụ trách ca | Coi như chưa lập xong: ca ở Chờ bàn giao; người phụ trách ca và Trưởng tầng được nhắc; người bàn giao bổ sung rồi gửi lại | 13.5, BR-M09-07, Q-87 |
| Không có ca sau cùng phạm vi trong [24 giờ] | Trưởng tầng của phạm vi | Trưởng tầng xác nhận bàn giao | 13.5, CFG-M09-09 |
| Tầng tạm chưa có Trưởng tầng được giao | Quản lý viện; Người phụ trách ca | Quản lý viện được nhắc ngay để giao Trưởng tầng tạm; trong lúc chờ, Người phụ trách ca đang diễn ra thực hiện các lệnh bàn giao và tạm nhận của Trưởng tầng. Quản lý viện không làm thay | 13.3, Q-90 |
| Yêu cầu đổi ca chưa xong khi tới giờ ca, hoặc ca bị hủy, hoặc một bên không còn đủ điều kiện | Hệ thống | Yêu cầu hết hiệu lực; nhắc trước theo CFG-M09-11 | BR-M09-10 |
| Người nhận rút đồng ý hoặc từ chối; người nhường hủy | Người nhận; người nhường | Yêu cầu chuyển Người nhận từ chối / Đã hủy | spec 015 bảng trạng thái yêu cầu đổi ca |
| Người nhường là người phụ trách ca mà không ai đủ điều kiện thay | Người duyệt | Không duyệt được đổi ca. Nghỉ đột xuất vẫn duyệt được, kèm xử lý người phụ trách ca | BR-M09-10, BR-M09-11, Q-187 |
| Hủy ca đã công bố chưa bắt đầu | Quản lý viện | Chỉ Quản lý viện, có lý do | 13.2 |
| Nhân viên chuyển Nghỉ việc | Hệ thống | Khóa tài khoản ngay; công việc tương lai chuyển về việc chung; ca thiếu phủ được cảnh báo | BR-M09-04, BR-M09-11 |

**Điểm đã làm rõ**

- Người lập bàn giao là **Người phụ trách ca** (Trưởng tầng hoặc Điều dưỡng có tên trong ca), không phải mọi Điều dưỡng (2.4, 13.5).
- Người duyệt đổi ca và nghỉ đột xuất là Trưởng tầng được giao của phạm vi; ca toàn viện do Quản lý viện duyệt (Q-177).
- Lịch còn ca thiếu phủ vẫn công bố được, nhưng phải có lý do và mở cảnh báo cho từng ca (Q-178).
- Tỷ lệ phục vụ dùng hai tham số riêng: ngưỡng CFG-M09-01 và trọng số CFG-M09-12 (Q-205).

---

## BF-14 – Đồ gửi: tiếp nhận, giao sử dụng, trả và kiểm kê

Khởi phát: gia đình giao đồ cho viện. Kết thúc: mọi đồ gửi ở Đã trả hoặc Đã xử lý. Thuốc gia đình gửi không đi qua luồng này (11.4, BF-07).

| Bước | Vai trò | Hoạt động | Trạng thái đồ gửi / kết quả | Căn cứ |
| ---- | ------- | --------- | --------------------------- | ------ |
| 1 | Hành chính; Điều dưỡng (trong phạm vi) | Tiếp nhận: vật phẩm, số lượng, tình trạng theo danh mục mức tình trạng, người giao, vị trí lưu giữ, ảnh (bắt buộc với đồ có giá trị). Người cao tuổi phải ở Đang tiếp nhận, Đang lưu trú, Tạm vắng, Hoạt động bên ngoài hoặc Điều trị tại bệnh viện; trực tuyến | Đang giữ; bản ghi bàn giao loại Tiếp nhận | 16.2, BR-M12-05, Q-156 |
| 2 | Người giữ (Hành chính, Điều dưỡng) | Giao sử dụng cho người cao tuổi. Tiền mặt, trang sức cần người đại diện đồng ý cho tự giữ đúng đồ đó và người cao tuổi không có cờ nguy cơ đi lạc; điện thoại không cần đồng ý | Đang giữ → Đang được người cao tuổi sử dụng | 16.3, BR-M12-06, Q-155 |
| 3 | Người giữ; Hành chính | Thu lại; Chuyển giữ (đổi người giữ hoặc vị trí). Bàn giao một phần thì tách phần được bàn giao thành đồ gửi mới | Đang được sử dụng → Đang giữ; Đang giữ → Đang giữ | 16.3 |
| 4 | Người giữ; Hành chính | Ghi hư hỏng (bắt buộc ảnh); hệ thống tự tạo sự cố mức Trung bình và báo người liên hệ chính | → Hư hỏng | BR-M12-02, BR-M12-05 |
| 5 | Người phát hiện; Hành chính | Báo thất lạc; hệ thống tự tạo sự cố mức Trung bình và báo người liên hệ chính | → Thất lạc | BR-M12-01, BR-M12-02 |
| 6 | Hành chính | Ghi Tìm thấy (bắt buộc ảnh với đồ có giá trị) | Thất lạc → Đang giữ | 16.4, BR-M12-05, Q-148 |
| 7 | Bộ lập lịch; Hành chính | Theo CFG-M12-04, sinh phiếu kiểm kê cho mỗi vị trí có đồ có giá trị ở Đang giữ; Hành chính kiểm kê từng đồ: Đúng / Lệch tình trạng / Không tìm thấy | Phiếu: Chờ kiểm kê → Hoàn thành; đồ không tìm thấy → Thất lạc | BR-M12-07, 16.6, Q-154 |
| 8 | Hành chính (mọi đồ); Điều dưỡng (chỉ đồ không có giá trị) | Trả cho người có quyền nhận: người đại diện hoặc người thân có quyền "được phép đón" đang hiệu lực, xác minh danh tính như quy trình đón; người nhận ký, được ghi ý kiến không đồng ý về tình trạng. Ảnh bắt buộc với đồ có giá trị | Đang giữ, Đang được sử dụng, Hư hỏng → Đã trả | 16.4, BR-M12-04, Q-153 |
| 9 | Hệ thống | Khi kết thúc lưu trú: kiểm tra còn đồ ở Đang giữ, Đang được sử dụng hoặc Hư hỏng | Điều kiện đồ gửi của BF-10: Đạt / Chưa đạt | BR-M12-03, Q-148 |

**Nhánh ngoại lệ**

| Tình huống | Vai trò | Xử lý | Căn cứ |
| ---------- | ------- | ----- | ------ |
| Người nhận không có quyền nhận, kể cả chính người cao tuổi | Hành chính; Người đại diện; Quản lý viện | Hành chính lập yêu cầu xác nhận người nhận khác cho đúng người và đúng đồ. Người đại diện xác nhận qua cổng hoặc bản ký. Không liên hệ được sau CFG-M12-05, hoặc không còn người đại diện Hiệu lực: Quản lý viện duyệt thay kèm lý do và bằng chứng liên hệ; không duyệt thay khi người đại diện đã từ chối. Xác nhận dùng một lần trong CFG-M10-08 | 16.4, BR-M12-04, Q-149, Q-150, Q-157 |
| Đồng ý cho tự giữ bị rút, hoặc cờ đi lạc được gắn khi đồ đang được sử dụng | Hệ thống; Hành chính, Điều dưỡng phụ trách | Báo Hành chính và điều dưỡng phụ trách để thu lại | BR-M12-06 |
| Đếm thiếu khi bàn giao | Người ghi | Phần thiếu được tách và ghi Báo thất lạc trong cùng lệnh | 16.3 |
| Người giữ vắng mặt | Hành chính | Hành chính bàn giao thay theo điều kiện Q-158 | 16.3, Q-158 |
| Nhân viên giữ đồ có giá trị hết ca | Hệ thống | Đồ vào bản nháp bàn giao ca (BF-13); nhân viên được nhắc chuyển giữ | 16.6, Q-158 |
| Còn đồ khi kết thúc lưu trú | Quản lý viện | Duyệt ngoại lệ có lý do (BF-10); đồ Thất lạc không chặn điều kiện đồ gửi nhưng sự cố của nó chặn qua điều kiện "không còn sự cố mở" | BR-M12-03 |
| Người cao tuổi đã ở trạng thái cuối mà còn đồ | Hành chính | Vẫn thu lại, chuyển giữ, trả, báo thất lạc, ghi hư hỏng; Hành chính được nhắc theo CFG-M12-02 | 16.4, BR-M01-05 |
| Đồ không người nhận sau CFG-M12-03 kể từ trạng thái cuối | Hành chính; Quản lý viện | Hành chính lập đề nghị "Xử lý đồ không người nhận" kèm biên bản và bằng chứng liên hệ; Quản lý viện duyệt (không tự duyệt đề nghị do mình lập). Tiền mặt chỉ chuyển cơ quan có thẩm quyền | BR-M12-08, 16.6, Q-152, Q-159 |
| Kiểm kê phát hiện đồ thừa hoặc đồ không rõ chủ | Hành chính | Đồ Thất lạc thấy lại: ghi Tìm thấy. Số lượng nhiều hơn: đính chính lần tiếp nhận. Đồ không rõ chủ: ghi ở phiếu kèm ảnh, báo Quản lý viện | 16.6 |
| Bản ghi bàn giao gần nhất ghi sai | Người được đính chính | "Hủy ghi nhận" chỉ áp cho bản ghi gần nhất; trạng thái và sự cố liên quan được tính lại | 16.3, Q-160 |

**Điểm đã làm rõ**

- Hư hỏng **chặn** kết thúc lưu trú; Thất lạc không chặn điều kiện đồ gửi, nhưng sự cố của nó chặn qua điều kiện sự cố mở (Q-148).
- Điều dưỡng chỉ trả đồ **không có giá trị** cho người có quyền nhận; mọi trường hợp khác do Hành chính (Q-153).
- Trên cổng, đồ gửi chỉ hiển thị cho người đại diện và người có quyền "được phép đón" (BR-M12-09, Q-151).

---

## BF-15 – Hoạt động, chuyến đi ngoài viện và kiểm tra chất lượng

Khởi phát: Trưởng tầng khai báo hoạt động. Luồng gồm ba phần: A. hoạt động trong viện (bước 1–5); B. chuyến đi ngoài viện (6–12); C. kiểm tra chất lượng và cảnh báo cô lập (13–15).

| Bước | Vai trò | Hoạt động | Trạng thái / kết quả | Căn cứ |
| ---- | ------- | --------- | -------------------- | ------ |
| 1 | Trưởng tầng | Khai báo hoạt động (loại, dấu "hoạt động nhóm", có thu phí hay không), mẫu lặp hoặc buổi lẻ; địa điểm là phòng hoặc khu vực gắn tầng | Hoạt động: hiệu lực | 8.8, Q-175 |
| 2 | Bộ lập lịch; Trưởng tầng | Sinh buổi từ mẫu trước [7 ngày] (CFG-M04-09), hoặc Trưởng tầng tạo buổi lẻ; người tham gia thường xuyên đủ điều kiện được đăng ký sẵn | Buổi: Đã lên lịch | BR-M04-21, spec 014 bảng trạng thái buổi |
| 3 | Trưởng tầng, Nhân viên chăm sóc | Đăng ký người tham gia. Hệ thống chặn khi đủ số lượng, có chỉ định hạn chế hoạt động bao trùm, khu đang khoanh vùng, hoặc người mang dấu "nghi nhiễm" với hoạt động nhóm; gợi ý theo sở thích | Đăng ký: Đã đăng ký | BR-M04-15, 8.10 |
| 4 | Trưởng tầng, Nhân viên chăm sóc | Điểm danh từ giờ bắt đầu: Có mặt (kèm mức độ tham gia, mức giao tiếp, tình trạng sau hoạt động) hoặc Vắng (kèm lý do); được thêm người chưa đăng ký nếu đủ điều kiện | Đăng ký: Đã điểm danh. Lượt Có mặt ở hoạt động có thu phí tạo chi phí nháp | 8.8, BR-M04-18 |
| 5 | Trưởng tầng, Nhân viên chăm sóc | Hoàn tất điểm danh | Buổi: Đã lên lịch → Đã điểm danh | spec 014 bảng trạng thái buổi |
| 6 | Trưởng tầng | Tạo chuyến đi (buổi của hoạt động ngoài viện): điểm đến, giờ rời, giờ về dự kiến; phân công trưởng đoàn (Trưởng tầng, Nhân viên chăm sóc hoặc Điều dưỡng) và người đi cùng có ca chồng thời gian chuyến | Chuyến: Đã lên lịch | 8.9, Q-161, Q-172, Q-214 |
| 7 | Trưởng tầng của tầng người cao tuổi | Đánh giá khả năng tham gia (Đạt / Không đạt kèm lý do), kể cả với chuyến toàn viện. Người có cờ nguy cơ đi lạc được Đạt phải có một người đi cùng kèm riêng | Đăng ký: Đã đăng ký → Được đi / Không đi | 8.9, Q-161 |
| 8 | Điều dưỡng (phụ trách hoặc đi cùng) | Với liều trong khoảng đi: ghi "Giao thuốc mang theo", hoặc quyết định "không mang thuốc" có lý do | Liều: Mang theo, hoặc Tạm dừng | 8.9, BR-M07-03, Q-58, Q-171 |
| 9 | Trưởng đoàn (Trưởng tầng làm thay được, có lý do) | Điểm danh rời viện, trực tuyến; sớm nhất trước giờ rời dự kiến CFG-M04-13; chỉ người Đang lưu trú (bán trú: đã điểm danh đến). Rời viện bị chặn khi còn liều chưa xử lý ở bước 8 | Chuyến: Đang đi; người cao tuổi: Đang lưu trú → Hoạt động bên ngoài; công việc trong khoảng đi chuyển Hủy | 5.6, BR-M01-06, BR-M04-16, Q-171 |
| 10 | Điều dưỡng đi cùng; không có thì Điều dưỡng phụ trách theo báo lại | Ghi nhận liều Mang theo | Liều: Mang theo → Đã dùng / Không thực hiện | 11.3, BR-M04-16, Q-54 |
| 11 | Trưởng đoàn, Trưởng tầng | Gia hạn giờ về (có lý do); điểm danh về từng người | Người cao tuổi: Hoạt động bên ngoài → Đang lưu trú; công việc được sinh lại | 8.9, BR-M04-17 |
| 12 | Trưởng đoàn, Trưởng tầng | Kết thúc điểm danh về | Chuyến: Đang đi → Đã về | spec 014 bảng trạng thái chuyến đi |
| 13 | Hệ thống | Mỗi ca, xét chọn ngẫu nhiên [5%] (CFG-M04-11) công việc Hoàn thành hoặc Hoàn thành trễ, kể cả công việc vệ sinh gắn tầng; bổ sung tại mốc CFG-M09-04 nếu chưa đủ | Danh sách kiểm tra chất lượng | BR-M04-23, Q-163 |
| 14 | Trưởng tầng được giao của tầng | Ghi Đạt / Không đạt kèm ghi chú trước hết ca. Không đạt tạo công việc làm lại trong chính ca; không tự đổi trạng thái giường | Tỷ lệ Đạt vào báo cáo chất lượng theo nhân viên (18.2) | BR-M04-23, Q-168, Q-174 |
| 15 | Hệ thống | 00:00 mỗi ngày, xét chuỗi [7] ngày tính không có lượt Có mặt ở hoạt động nhóm | Cảnh báo nhẹ "nguy cơ cô lập" cho Trưởng tầng, kèm hoạt động gợi ý | BR-M04-22, Q-173 |

**Nhánh ngoại lệ**

| Tình huống | Vai trò | Xử lý | Căn cứ |
| ---------- | ------- | ----- | ------ |
| Quá giờ về dự kiến cộng CFG-M04-07 mà chưa kết thúc điểm danh về | Hệ thống | Chuyến mang dấu "quá giờ về"; báo trưởng đoàn và Trưởng tầng mức Trung bình; quá thêm một lần CFG-M04-07 mà chưa gia hạn thì báo Quản lý viện | BR-M04-17, Q-172 |
| Thiếu người trong chuyến | Trưởng đoàn, người đi cùng (kể cả Điều dưỡng), Trưởng tầng | "Báo thiếu người" ngay trong chuyến; khi kết thúc điểm danh về, người chưa về và không rời đoàn chuyển "thiếu khi về". Mỗi trường hợp tạo sự cố khẩn cấp loại "đi lạc hoặc không trở về" (BF-06). Hai lệnh này bắt buộc trực tuyến; mất kết nối thì gọi Trưởng tầng làm thay | 8.9, BR-M04-17 |
| Người tham gia chuyển viện hoặc qua đời trong chuyến | Hệ thống | Đánh dấu "rời đoàn", không bị tính là thiếu người | 8.9, spec 014 FR-045 |
| Người thân muốn đón thẳng từ điểm đến | Trưởng đoàn; Hành chính, Trưởng tầng | Người cao tuổi phải được điểm danh về viện trước, rồi mới Cho tạm vắng theo quy trình đón (BF-12) | 8.9, Q-166 |
| Chuyến bị hủy, hoặc người chuyển "Không đi" sau khi đã giao thuốc | Hệ thống; Điều dưỡng phụ trách | Điều dưỡng phụ trách được nhắc ghi Nhận lại thuốc | 8.9, BR-M07-03, Q-58 |
| Chuyến dời sang ngày khác | Trưởng tầng | Mọi đánh giá khả năng tham gia phải làm lại | 8.9 |
| Trưởng đoàn cần chuyển giữa chuyến | Trưởng đoàn, Trưởng tầng | Chuyển cho người đi cùng là Trưởng tầng, Nhân viên chăm sóc hoặc (**bổ sung, Q-214**) Điều dưỡng, có lý do; không còn ai đủ điều kiện thì Trưởng tầng tạm chịu trách nhiệm và Quản lý viện được báo | 8.9, Q-161, Q-172 |
| Người bán trú tham gia buổi kết thúc sau giờ về theo lịch | Hành chính; Người đại diện | Chỉ đăng ký được khi Hành chính đã ghi nhận đồng ý về muộn của người đại diện cho đúng buổi; giờ về dự kiến dời theo buổi. Rút đồng ý trước giờ bắt đầu thì đăng ký bị hủy | 3.4, Q-167, Q-176 |
| Hết ngày của buổi mà đăng ký chưa có kết quả | Hệ thống | Đăng ký nhận "Không ghi nhận"; buổi mang dấu "điểm danh không đủ", hoặc "Không điểm danh" nếu chưa có lượt nào; báo người phụ trách và Trưởng tầng | 8.8, Q-169 |
| Khu bị khoanh vùng; người được gắn dấu "nghi nhiễm"; chỉ định hạn chế được gắn | Hệ thống | Buổi trong viện chưa điểm danh trong khu và đăng ký bị bao trùm tự hủy; gỡ không khôi phục. Chuyến đang diễn ra không bị ảnh hưởng | BR-M04-15, BR-M05-11, Q-164, Q-165 |
| Điểm danh rời hoặc về ghi sai | Trưởng tầng | Đính chính có lý do: sửa thời điểm thực tế, hoặc "Hủy ghi nhận" khi chuyến chưa về (người bị ghi đi nhầm về lại Đang lưu trú) | 8.9, Q-170 |
| Mục kiểm tra chất lượng chưa được kiểm tra khi hết ca | Hệ thống | Mục thành "Quá hạn kiểm tra", báo Quản lý viện | BR-M04-23 |

**Điểm đã làm rõ**

- Trưởng đoàn và người đi cùng giữ nhiệm vụ tới khi chuyến về, kể cả khi hết ca (Q-161).
- **(Đã chỉnh sửa, 2026-09-28, Q-214)** Điều dưỡng được làm Trưởng đoàn, người phụ trách buổi, đăng ký và điểm danh như Nhân viên chăm sóc. Chỉ người đi cùng không có vai trò Trưởng tầng, Nhân viên chăm sóc hoặc Điều dưỡng mới bị giới hạn ở lệnh "Báo thiếu người". Liều Mang theo vẫn chỉ do Điều dưỡng ghi (Q-54).
- Lượt "bỏ giữa chừng" vẫn là Có mặt: tính phí và tính tham gia (Q-173).
- Chỉ Trưởng tầng được giao của tầng ghi kết quả kiểm tra chất lượng; Người phụ trách ca không có quyền này (BR-M04-23).
- **(Bổ sung, 2026-09-28, Q-220)** Chuyến đi dùng xe của viện phải có lịch xe (UC-90, 7.7) trước khi điểm danh rời viện.
- **(Bổ sung, 2026-09-29, Q-231)** Lịch xe của chuyến đi theo chuyến: người đầu tiên được điểm danh rời viện thì lịch chuyển Đang dùng; "Kết thúc điểm danh về" thì Đã hoàn thành; chuyến hủy thì lịch Đã hủy; chuyến đổi giờ thì lịch dời theo (7.7). **(Bổ sung, 2026-09-29, Q-236)** Chuyến đang đi được gia hạn giờ về thì giờ về của lịch xe dời theo; chồng lịch kế tiếp của cùng xe không chặn gia hạn, Quản lý viện và người đặt lịch kế tiếp được báo. **(Bổ sung, 2026-09-29, Q-239, Q-240)** Dời chuyến bị chặn nếu lịch xe ở giờ mới chồng lịch khác của cùng xe. Mọi bản ghi rời viện bị Hủy ghi nhận thì chuyến quay về Đã lên lịch và lịch xe về Đã đặt.

---

## BF-16 – Số dư, thu chi và báo sắp hết tiền (bổ sung, 2026-09-28)

Khởi phát: hợp đồng đầu tiên của người cao tuổi chuyển Hiệu lực, lúc đó sổ số dư được mở. Kết thúc: số dư và tiền cọc được quyết toán khi kết thúc lưu trú (BF-10 bước 6a).

| Bước | Vai trò | Hoạt động | Trạng thái / kết quả | Căn cứ |
| ---- | ------- | --------- | -------------------- | ------ |
| 1 | Hệ thống | Khi hợp đồng đầu tiên chuyển Chờ ký (Q-225): mở sổ số dư và sổ tiền cọc; cấp mã nộp tiền không đổi | Số dư: 0 | 15.9, DBR-28, 29 |
| 2 | Người thân (qua cổng) | Xem số tài khoản của viện, mã nộp tiền, mã QR (VietQR) có sẵn nội dung; chuyển khoản | — | 15.9, 23 |
| 3 | Kế toán | Nộp tiền mặt hoặc thu cọc tiền mặt: lập phiếu thu (số phiếu liên tục), ghi số tiền, người nộp, chứng từ | Giao dịch: Đã xác nhận; số dư hoặc sổ cọc tăng | UC-85, 15.9, BR-M11-16 |
| 3a | Kế toán; Quản lý viện | Cuối ngày có giao dịch tiền mặt: Kế toán lập chốt quỹ ngày (thu, chi, tiền thực có, chênh lệch và lý do); Quản lý viện xác nhận hoặc trả lại | Chốt quỹ: Đã lập → Đã xác nhận / Bị trả lại | UC-93, BR-M11-16, Q-226 |
| 4 | Kế toán | Tải sao kê ngân hàng (Excel/CSV) và nhập vào hệ thống. Dòng trùng mã giao dịch ngân hàng bị bỏ qua | Dòng sao kê: đã nhập | UC-86, BR-M11-12, DBR-29 |
| 5 | Hệ thống | Khớp từng dòng theo mã nộp tiền trong nội dung. Dòng không khớp hoặc khớp nhiều mã vào danh sách "chưa khớp" | Dòng: Đã khớp / Chưa khớp | BR-M11-12 |
| 6 | Kế toán | Xác nhận dòng đã khớp vào số dư hoặc sổ cọc; gán thủ công dòng chưa khớp (có lý do) hoặc đánh dấu "không thuộc người cao tuổi". Chuyển khoản gộp nhiều khoản: ghi toàn bộ vào một sổ rồi lập cặp điều chỉnh chuyển tiền (Q-227) | Giao dịch nộp tiền: Đã xác nhận; số dư hoặc sổ cọc tăng | BR-M11-12 |
| 7 | Hệ thống | Bảng chi phí Đã chốt (BF-09 bước 7): tạo giao dịch thanh toán bằng tổng bảng | Số dư giảm; số dư âm hiện "còn nợ" | BR-M11-11, DBR-30 |
| 8 | Bộ lập lịch | Hằng ngày tính số ngày còn đủ tiền = (số dư − chi phí chưa chốt của mọi bảng chưa chốt) ÷ phí lưu trú ngày (Q-228, Q-223). Dưới [15 ngày] (CFG-M11-04) hoặc số dư âm: báo người đại diện mức Trung bình (người không có quyền xem chi phí chỉ nhận phần "chung") và Kế toán mức Nhẹ; nhắc lại mỗi [7 ngày] (CFG-M11-05); chiều tốt lên không báo ngay; còn nợ quá CFG-M11-05 thì báo thêm Quản lý viện | Thông báo "sắp hết tiền" / "còn nợ" | UC-87, BR-M11-13 |
| 9 | Kế toán | Theo dõi danh sách số dư thấp và liên hệ gia đình | — | 15.9 |
| 10 | Kế toán; Quản lý viện | Lập hoàn tiền (giữa kỳ không vượt số dư − chi phí chưa chốt), điều chỉnh, cặp điều chỉnh chuyển tiền, giao dịch đảo (bắt buộc lý do); Quản lý viện duyệt, lúc duyệt kiểm tra lại số tiền và người nhận | Giao dịch: Chờ duyệt → Đã xác nhận / Từ chối / Đã hủy | BR-M11-10, 14, Q-229 |
| 11 | Kế toán | Xuất sao kê số dư, báo cáo thu chi, sổ tiền cọc ra Excel | — | UC-88, 15.9 |

**Điểm đã làm rõ**

- Người báo "sắp hết tiền" là **hệ thống** (Bộ lập lịch). Kế toán là người theo dõi và liên hệ gia đình (Q-211).
- Số dư thấp hoặc âm không chặn chăm sóc, thuốc, suất ăn (Q-219).
- Tiền cọc theo dõi riêng, không vào số dư (Q-219). Hợp đồng bị hủy trước khi Hiệu lực hoặc Hủy tiếp nhận mà còn cọc: Kế toán được nhắc lập hoàn cọc có duyệt; Hủy tiếp nhận không bị chặn (Q-225).
- Bảng bổ sung chưa chốt không chặn điều kiện "số dư và tiền cọc đã quyết toán" (Q-230, nhất quán Q-134).
- Hệ thống không kết nối trực tiếp với ngân hàng; chuyển khoản chỉ được ghi nhận qua đối soát sao kê.

---

## BF-17 – Nguy kịch và thực hiện nguyện vọng cuối đời (bổ sung, 2026-09-28)

Khởi phát: Bác sĩ nhận định người cao tuổi nguy kịch. Kết thúc: lựa chọn theo nguyện vọng đã được thực hiện và cảnh báo "nguy kịch" được đóng.

| Bước | Vai trò | Hoạt động | Trạng thái / kết quả | Căn cứ |
| ---- | ------- | --------- | -------------------- | ------ |
| 1 | Bác sĩ; Điều dưỡng khi không có Bác sĩ trực (dấu tạm, Q-218) | Ghi dấu nguy kịch, bắt buộc nhận định | Dấu nguy kịch: đang mở | UC-83, 9.5 |
| 2 | Hệ thống | Tạo cảnh báo Khẩn cấp "nguy kịch – thực hiện nguyện vọng cuối đời" (không gộp); báo đồng thời Bác sĩ trực, Điều dưỡng phụ trách, Trưởng tầng, Quản lý viện; hiển thị nguyện vọng Hiệu lực hoặc "chưa có nguyện vọng" | Cảnh báo: Mới | BR-M05-15 |
| 3 | Hệ thống | Báo người liên hệ chính và người đại diện mức Khẩn cấp (nội dung tối thiểu theo Q-72 nếu không thuộc bản đồng ý); tạo yêu cầu xác nhận lại nguyện vọng theo thứ tự gọi, bắt đầu từ người đại diện | Yêu cầu gọi điện | BR-M05-16, BR-M13-02 |
| 4 | Điều dưỡng hoặc Bác sĩ | Gọi, ghi kết quả: giữ nguyện vọng / phiên bản nguyện vọng mới / không liên lạc được | Kết quả xác nhận (nhóm 3) | UC-84, BR-M05-16 |
| 5a | Điều dưỡng, Bác sĩ | Lựa chọn "chuyển bệnh viện điều trị tích cực": thực hiện Chuyển viện | Người cao tuổi: Điều trị tại bệnh viện; dấu nguy kịch tự gỡ | UC-16, BR-M05-14 |
| 5b | Hành chính hoặc Trưởng tầng | Lựa chọn "đưa về nhà": Cho tạm vắng lý do "về nhà theo nguyện vọng cuối đời", qua quy trình đón (BF-12); Hành chính lập hồ sơ kết thúc lưu trú khi gia đình quyết định (Q-218) | Người cao tuổi: Tạm vắng | 6.7, 6.8 |
| 5c | Bác sĩ | Lựa chọn "ở lại viện chăm sóc giảm nhẹ": ghi quyết định; hệ thống tạo yêu cầu xem xét kế hoạch chăm sóc | Kế hoạch mới theo BF-08 bước 5–6 | BR-M04-20 |
| 6 | Bác sĩ, Điều dưỡng | Đóng cảnh báo kèm lựa chọn đã thực hiện | Cảnh báo: Đã đóng | BR-M05-15 |
| 7 | Bác sĩ | Gỡ dấu nguy kịch khi tình trạng ổn định, có lý do | Dấu nguy kịch: đã gỡ | 9.5 |

**Điểm đã làm rõ**

- Không liên lạc được ai: người xử lý làm theo nguyện vọng Hiệu lực. Chưa có nguyện vọng: Bác sĩ quyết định theo chuyên môn và ghi lý do (BR-M05-16).
- Nếu có sự cố khẩn cấp cùng lúc (ngừng thở, ngã…), BF-06 và thẻ thông tin khẩn cấp vẫn áp dụng, gồm cả xác nhận "đã đối chiếu nguyện vọng" (BR-M05-13).
- Phiếu nguyện vọng do người cao tuổi ký khi còn đủ năng lực, không còn thì người đại diện ký; ý kiến khác nhau thì theo ý người cao tuổi (Q-216).
