# Câu hỏi cho buổi rà với người vận hành viện

**Ngày soạn:** 2026-09-29
**Người tham dự đề xuất:** Quản lý viện; Điều dưỡng trưởng hoặc một Trưởng tầng; Hành chính (cho phần A1, B7, B12); Kế toán (cho B9, B10); Dinh dưỡng viên hoặc người phụ trách bếp (cho A4, B11) nếu có mặt.
**Thời lượng đề xuất:** khoảng 90 phút. Nếu thiếu thời gian, làm phần A và các câu có dấu ★ ở phần B trước. Các câu không có dấu ★ ở B7 → B15 có thể gửi trước dưới dạng phiếu cho từng người phụ trách điền.

> Tài liệu này không phải tài liệu nguồn. Nó chỉ dùng để hỏi. Kết quả buổi rà được ghi vào `docs/nghiep-vu.md` (mục 24) và các spec liên quan theo cách ở phần E.

## Cách dùng

Mỗi câu nêu một tình huống, cách hệ thống **đang định làm**, và câu hỏi. Với mỗi câu, người vận hành chỉ cần trả lời một trong ba:

- **Đồng ý**: đúng với cách viện đang làm hoặc muốn làm.
- **Không**: kèm cách viện thực sự làm.
- **Chưa rõ**: cần hỏi thêm người khác (ghi tên người đó).

Cột "Mã" dành cho nhóm phân tích tra lại. Người vận hành không cần đọc cột này.

> **Đề xuất trả lời (2026-09-30).** Dưới mỗi bảng có mục "Đề xuất trả lời" do nhóm phân tích điền sẵn, dựa trên tài liệu nguồn, văn bản pháp luật liên quan và thông lệ vận hành. Đây **chưa phải** xác nhận của người vận hành. Trong buổi rà, người vận hành chỉ cần đồng ý hoặc sửa từng dòng; tổng hợp các thay đổi ở phần F.

---

## Phần A — Các quyết định vừa chốt ngày 29/09, cần xác nhận với thực tế

Các quyết định dưới đây do nhóm phân tích chốt dựa trên lập luận, chưa được người vận hành xác nhận.

### A1. Khai báo tạm trú, lưu trú với công an

Lưu ý: phần này còn là câu hỏi pháp lý. Buổi rà chỉ xác nhận cách viện đang làm. Căn cứ pháp luật vẫn cần tư vấn pháp lý kiểm tra riêng.

| # | Tình huống và cách hệ thống đang định làm | Câu hỏi | Mã |
| --- | --- | --- | --- |
| A1.1 ★ | Người ở nội trú dự kiến từ 30 ngày trở lên thì viện đăng ký tạm trú; dưới 30 ngày thì thông báo lưu trú. Người bán trú không khai báo. | Viện đang khai báo theo cách này không? Mốc có đúng là 30 ngày không? | Q-215, CFG-M02-11 |
| A1.2 | Mốc 30 ngày tính cộng dồn: hợp đồng đầu tiên cộng các lần gia hạn, cộng các hợp đồng nối tiếp liền nhau. Nếu có khoảng nghỉ giữa hai hợp đồng thì tính lại từ đầu. | Cách tính này có khớp cách viện hiểu không? | Q-215, Q-241 |
| A1.3 ★ | Người ở ngắn ngày được gia hạn mà tổng vẫn dưới 30 ngày (ví dụ 14 ngày thêm 7 ngày): viện thông báo lưu trú **lại** cho ngày kết thúc mới. | Thực tế viện có thông báo lại không, hay để nguyên thông báo cũ? | Q-215 |
| A1.4 | Người có thường trú cùng phường/xã với viện: luôn chỉ thông báo lưu trú, không đăng ký tạm trú, dù ở bao lâu. Hành chính xác nhận việc này dựa trên địa chỉ thường trú ghi trên hồ sơ. | Viện có gặp trường hợp này không? Xử lý có đúng như vậy không? | Q-215, Q-244 |
| A1.5 | Hạn khai báo là 1 ngày sau khi người cao tuổi vào ở. Quá hạn thì Hành chính được nhắc mỗi ngày, Quản lý viện được báo một lần. Việc khai báo không chặn việc nhận người vào ở. | Hạn 1 ngày có thực tế không? Quản lý viện có muốn được báo nhiều hơn một lần không? | Q-215, CFG-M02-12, BR-M02-11 |

**Đề xuất trả lời:**

| Câu | Đề xuất trả lời | Kết quả |
| --- | --- | --- |
| A1.1 | Giữ cách làm và mốc 30 ngày (tham số CFG-M02-11). Cách chia này khớp Luật Cư trú 2020: ở từ 30 ngày trở lên tại nơi ngoài xã/phường thường trú thì đăng ký tạm trú (Điều 27); ở dưới 30 ngày thì thông báo lưu trú (Điều 30). Tư vấn pháp lý rà lại văn bản hiện hành trước khi vận hành, nhưng việc này không chặn spec. | Đồng ý |
| A1.2 | Tính cộng dồn theo chuỗi hợp đồng nối tiếp là đúng thực tế: người cao tuổi vẫn ở liên tục tại viện; có khoảng nghỉ thì tính lại. | Đồng ý |
| A1.3 | Thông báo lưu trú gắn với thời gian ở dự kiến; ngày kết thúc đổi thì phải thông báo lại, để nguyên thông báo cũ là sai thông tin với công an. | Đồng ý |
| A1.4 | Giữ cách làm. Luật Cư trú 2020 (Điều 27) chỉ cho đăng ký tạm trú ở nơi ngoài xã/phường đã đăng ký thường trú, nên người thường trú cùng xã/phường với viện không đăng ký tạm trú được; thông báo lưu trú là cách còn lại. Trường hợp này hiếm. | Đồng ý |
| A1.5 | Hạn 1 ngày là mức trần hợp lý (thông báo lưu trú theo Luật Cư trú làm ngay trong ngày). Hành chính được nhắc hằng ngày là đủ ép tiến độ; báo Quản lý viện một lần để biết, không cần lặp. Không chặn tiếp nhận. | Đồng ý |

### A2. Giường hỏng và tài sản của viện

| # | Tình huống và cách hệ thống đang định làm | Câu hỏi | Mã |
| --- | --- | --- | --- |
| A2.1 ★ | Giường đang có người nằm bị báo hỏng: hệ thống ghi nhận hư hỏng nhưng giường vẫn "đang sử dụng". Trưởng tầng được nhắc mỗi ngày để chuyển người sang giường khác. | Khi giường có người bị hỏng, thực tế viện làm gì: chuyển người ngay, sửa tại chỗ, hay để người nằm tiếp? | Q-232 |
| A2.2 ★ | Sau khi người đó được chuyển đi và giường đã dọn, giường **không** được xếp người mới cho tới khi Trưởng tầng báo hỏng lại (để đưa vào bảo trì) hoặc xác nhận "đã sửa tại chỗ". | Quy trình này có hợp lý không, hay quá chặt? | Q-235 |
| A2.3 | Nếu giường bị báo hỏng mà đã có người được xếp sẵn sẽ vào ở tuần sau, việc xếp đó vẫn giữ, nhưng người đó chưa vào được cho tới khi giường xử lý xong. Hành chính và Trưởng tầng được báo. | Thực tế viện có muốn hủy luôn việc xếp giường để chọn giường khác ngay không? | Q-235 |
| A2.4 | Chỉ có một cách để đưa giường vào bảo trì hoặc ngừng dùng: thao tác trên tài sản giường. Người làm được: Quản lý viện; Trưởng tầng cho giường trong tầng mình. | Ai trong viện được quyết định đưa giường vào bảo trì? | Q-234, Q-51 |
| A2.5 | Giường ngừng dùng vĩnh viễn chỉ bằng cách thanh lý, và không dùng lại được. Muốn tạm không dùng một giường (ví dụ đóng bớt phòng mùa thấp điểm) thì phải để giường ở trạng thái bảo trì. | Viện có nhu cầu tạm đóng giường hoặc phòng mà không phải vì hỏng không? | Q-237 |
| A2.6 | Giường đã từng có người nằm thì không chuyển sang phòng khác trong hệ thống. Nếu viện chuyển giường vật lý, hệ thống ghi thành giường mới ở phòng mới và thanh lý giường cũ. | Viện có hay chuyển giường giữa các phòng không? | Q-243 |
| A2.7 | Khi tạo giường mới trong hệ thống, hồ sơ tài sản của giường được tạo tự động; Quản lý viện bổ sung ngày mua, giá, lịch bảo trì sau. | Viện có theo dõi giường như tài sản (giá, bảo trì) không, hay chỉ cần theo dõi xe và thiết bị lớn? | Q-238, Q-242 |

**Đề xuất trả lời:**

| Câu | Đề xuất trả lời | Kết quả |
| --- | --- | --- |
| A2.1 | Không để một cách xử lý cho mọi mức hỏng. Khi báo hỏng giường đang có người, người báo chọn mức ảnh hưởng: (a) **mất an toàn** (gãy khung, hỏng thành giường, hỏng cơ cấu nâng hạ) thì hệ thống báo ngay mức Khẩn cấp cho Trưởng tầng và Người phụ trách ca, việc chuyển người dùng lệnh chuyển giường gấp (Q-44); (b) **không mất an toàn** thì giữ cách hiện tại (dấu "chờ chuyển người", nhắc hằng ngày), và Trưởng tầng được xác nhận "đã sửa tại chỗ" để gỡ dấu ngay khi người vẫn đang nằm. | Không — sửa Q-232 |
| A2.2 | Hợp lý: giường từng báo hỏng phải được xác nhận xử lý xong mới nhận người mới. | Đồng ý |
| A2.3 | Không tự hủy việc xếp giường vì giường có thể sửa kịp trước ngày vào. Hành chính đã được báo kèm danh sách người bị ảnh hưởng, và có thể chủ động đổi giường đặt trước khi thấy không kịp; lịch sử phân bổ cũ vẫn giữ. | Đồng ý |
| A2.4 | Quản lý viện trên toàn viện; Trưởng tầng trong tầng mình. | Đồng ý |
| A2.5 | Viện có nhu cầu tạm đóng giường, phòng không vì hỏng (mùa thấp điểm, thiếu nhân sự, sửa chữa khu vực). Dùng "Đang bảo trì" cho việc này làm sai số liệu bảo trì. Thêm trạng thái giường **Tạm ngừng sử dụng**: chỉ Quản lý viện đặt và gỡ, bắt buộc lý do; chỉ đặt được khi giường Trống và không có phân bổ tương lai; không tính vào giường khả dụng; gỡ thì về Trống. Thanh lý vẫn là cách ngừng dùng vĩnh viễn duy nhất. | Không — sửa Q-237, Q-234 |
| A2.6 | Viện hiếm khi chuyển giường giữa các phòng. Cách hiện tại giữ đúng lịch sử "ai nằm giường nào, ở phòng nào" mà không phải lưu vị trí theo thời gian. Khi thanh lý giường cũ vì lý do này, ghi lý do "chuyển vị trí" để phân biệt với thanh lý thật. | Đồng ý |
| A2.7 | Tạo tài sản tự động không tốn công của ai. Ngày mua, giá, lịch bảo trì là thông tin tùy chọn; viện chỉ cần theo dõi xe và thiết bị lớn thì để trống. | Đồng ý |

### A3. Xe đưa đón

| # | Tình huống và cách hệ thống đang định làm | Câu hỏi | Mã |
| --- | --- | --- | --- |
| A3.1 ★ | Chuyến đi ngoài viện dùng xe của viện phải có lịch xe trước; chưa có lịch xe thì không điểm danh rời viện được. | Viện có quy định đặt xe trước như vậy không? Ai đặt xe? | Q-220 |
| A3.2 | Lịch xe tự chuyển "đang dùng" khi người đầu tiên lên xe rời viện, và "hoàn thành" khi đoàn điểm danh về xong. Với việc khác (đưa đi khám, chuyển viện không cấp cứu), người đặt tự ghi giờ xuất phát và giờ trả xe. | Cách ghi này có khớp thực tế không? | Q-231 |
| A3.3 | Chuyến đang đi mà cần về muộn hơn: được gia hạn ngay, kể cả khi xe đã có lịch khác ngay sau. Quản lý viện và người đặt lịch sau được báo để xử lý. | Khi xe về trễ làm lỡ việc sau, thực tế viện xử lý thế nào? | Q-236 |
| A3.4 | Dời một chuyến chưa đi sang giờ mà xe đã có lịch khác: hệ thống chặn, phải chọn giờ khác hoặc đổi xe trước. | Hợp lý không? | Q-239 |

**Đề xuất trả lời:**

| Câu | Đề xuất trả lời | Kết quả |
| --- | --- | --- |
| A3.1 | Cần đặt xe trước để tránh trùng xe. Người đặt: Trưởng tầng, Hành chính hoặc Quản lý viện (7.7); với chuyến đi ngoài viện thường là người tổ chức chuyến. | Đồng ý |
| A3.2 | Tự chuyển trạng thái theo điểm danh đỡ ghi tay; việc khác ghi giờ thực tế. | Đồng ý |
| A3.3 | Giữ cách làm. Xe đang ở ngoài cùng đoàn người cao tuổi thì chặn gia hạn cũng không làm xe về sớm hơn, chỉ làm dữ liệu sai với thực tế. Việc cần là báo sớm: Quản lý viện và người đặt lịch sau được báo để đổi xe hoặc dời việc sau. | Đồng ý |
| A3.4 | Chuyến chưa đi thì còn chọn được giờ hoặc xe khác, nên chặn là đúng. | Đồng ý |

### A4. Kho nguyên liệu bếp

| # | Tình huống và cách hệ thống đang định làm | Câu hỏi | Mã |
| --- | --- | --- | --- |
| A4.1 ★ | Kiểm kê kho hằng tháng: Nhân viên bếp đếm và lập phiếu kiểm kê, Quản lý viện duyệt hoặc trả lại; người đếm không tự duyệt. | Thực tế ai đếm, ai duyệt chênh lệch kho? Viện có thủ kho riêng không? | Q-233, CFG-M08-09 |
| A4.2 | Nhân viên bếp lập đề nghị nhập khi thiếu nguyên liệu; Quản lý viện chuyển thành phiếu nhập hoặc từ chối. Nhân viên bếp không tự lập phiếu nhập. | Ai thực sự nhận hàng và ký phiếu nhập? | Q-220, 12.7 |

**Đề xuất trả lời:**

| Câu | Đề xuất trả lời | Kết quả |
| --- | --- | --- |
| A4.1 | Viện quy mô vừa thường không có thủ kho riêng. Nhân viên bếp đếm, Quản lý viện duyệt chênh lệch; tách người đếm và người duyệt. | Đồng ý |
| A4.2 | Nhân viên bếp nhận hàng thực tế và kiểm số lượng, chất lượng (ký biên bản giao nhận với nhà cung cấp ngoài hệ thống); Quản lý viện lập phiếu nhập theo số thực nhận. | Đồng ý |

---

## Phần B — Các cách làm nhóm tự suy ra, chưa có trong tài liệu gốc

Đây là những chi tiết tài liệu gốc không nói. Nhóm đã chọn một cách hợp lý để viết tiếp. Chỉ những điểm ảnh hưởng tới công việc hằng ngày được đưa vào đây.

### B1. Kế hoạch chăm sóc và công việc hằng ngày

| # | Cách hệ thống đang định làm | Câu hỏi | Mã |
| --- | --- | --- | --- |
| B1.1 | Mỗi người cao tuổi chỉ có một bản kế hoạch chăm sóc đang soạn hoặc đang chờ duyệt tại một thời điểm. | Có khi nào cần soạn song song hai bản không? | 005 FR-004 |
| B1.2 | Người duyệt kế hoạch không được sửa ngày hiệu lực; muốn đổi thì trả lại cho người soạn. | Bác sĩ duyệt có hay muốn sửa trực tiếp không? | 005 FR-004 |
| B1.3 | Với người có mục tiêu lượng nước trong kế hoạch: việc nhắc uống thêm nước chỉ tạo khi người đó có mặt tại viện lúc kiểm tra, và tối đa một lần mỗi ngày. | Một lần mỗi ngày có đủ không? | 005 FR-039 |
| B1.4 | Việc chăm sóc làm ngoài kế hoạch được ghi bằng "ghi nhận phát sinh". Có giới hạn về thời điểm và vai trò được ghi, và hệ thống gợi ý để tránh ghi trùng. | Nhân viên hay phải ghi việc ngoài kế hoạch không? Ai ghi? | 005 FR-025 |

**Đề xuất trả lời:**

| Câu | Đề xuất trả lời | Kết quả |
| --- | --- | --- |
| B1.1 | Hai bản soạn song song dễ ghi đè nhau; bản mới làm sau khi bản đang soạn được duyệt hoặc hủy. | Đồng ý |
| B1.2 | Người duyệt sửa trực tiếp làm mất vết ai quyết định; trả lại người soạn là đủ nhanh. | Đồng ý |
| B1.3 | Việc cho uống nước theo giờ đã nằm trong công việc của kế hoạch chăm sóc; đây chỉ là nhắc bù khi thấy thiếu so với mục tiêu tại mốc kiểm tra, một lần mỗi ngày là đủ và tránh nhắc dồn. | Đồng ý |
| B1.4 | Có phát sinh thường xuyên (thay đồ đột xuất, hỗ trợ vệ sinh). Người ghi: Nhân viên chăm sóc, Điều dưỡng có ca với người cao tuổi đó. | Đồng ý |

### B2. Thuốc

| # | Cách hệ thống đang định làm | Câu hỏi | Mã |
| --- | --- | --- | --- |
| B2.1 ★ | Khi đang có phiếu đối chiếu thuốc chưa xong (ví dụ sau khi người cao tuổi đi viện về), không nhập đơn mới, tiếp tục đơn hay đổi liều ngoài phiếu đó; ngừng một đơn thì vẫn được. | Có tình huống cần đổi thuốc gấp trong lúc đối chiếu không? | 006 FR-037 |
| B2.2 | "Từ chối uống thuốc liên tiếp" được đếm cả khi đơn đổi sang đơn mới cùng thuốc; bỏ qua các liều đang tạm dừng hoặc đã hủy. | Điều dưỡng hiểu "liên tiếp" có giống vậy không? | 006 FR-032 |
| B2.3 | Thuốc gia đình gửi: hệ thống tính số ngày còn dùng được và báo gia đình một lần mỗi khi xuống dưới ngưỡng. | Gia đình có cần được nhắc lại nếu chưa mang thuốc tới không? | 006 FR-046 |
| B2.4 | Khi phát thuốc, hệ thống gợi ý lô có hạn dùng sớm nhất. Có lệnh "kiểm kê điều chỉnh" khi số thuốc thực tế lệch sổ. | Viện có kiểm đếm thuốc định kỳ không? Ai làm? | 006 FR-044, FR-045 |

**Đề xuất trả lời:**

| Câu | Đề xuất trả lời | Kết quả |
| --- | --- | --- |
| B2.1 | Giữ cách làm. Sai sót thuốc hay xảy ra nhất ngay sau khi người cao tuổi đi viện về; cho đổi thuốc ngoài phiếu đúng lúc này tạo hai nguồn y lệnh. Việc gấp thì Bác sĩ ghi ngay trên phiếu đối chiếu và xác nhận phiếu; ngừng thuốc vẫn làm được ngoài phiếu. | Đồng ý |
| B2.2 | Người cao tuổi từ chối cùng một thuốc là tín hiệu lâm sàng, dù đơn vừa đổi liều hay kê lại. Liều tạm dừng, hủy không phải lần từ chối. | Đồng ý |
| B2.3 | Báo một lần có nguy cơ hết thuốc thật. Bổ sung **một lần nhắc thứ hai** khi số ngày còn lại xuống dưới \[2 ngày\] (tham số mới), gửi người thân như lần đầu và gửi thêm Điều dưỡng phụ trách để chủ động liên hệ gia đình hoặc báo Bác sĩ. | Không — sửa 006 FR-046 |
| B2.4 | Thuốc gia đình gửi được kiểm khi có lệch (lệnh Kiểm kê điều chỉnh do Điều dưỡng làm, FR-045). Chưa bắt buộc kiểm đếm định kỳ ở giai đoạn này; riêng thuốc kiểm soát đặc biệt xem D5. | Đồng ý |

### B3. Cảnh báo và sự cố

| # | Cách hệ thống đang định làm | Câu hỏi | Mã |
| --- | --- | --- | --- |
| B3.1 | Có loại sự cố "cơ sở vật chất" (hỏng điện nước, thiết bị) không gắn với người cao tuổi nào. | Viện có muốn ghi loại sự cố này trong hệ thống không? | 007 điểm 12 |
| B3.2 ★ | Cảnh báo đã leo thang tới cấp 2 mà vẫn chưa ai tiếp nhận thì không leo thang thêm; cảnh báo mang dấu "đã leo thang tối đa" và hiện nổi trên màn hình của Quản lý viện và Trưởng tầng. | Sau cấp 2, thực tế viện còn gọi ai nữa không? | 007 FR-034 |
| B3.3 | Không đóng được sự cố lây nhiễm khi khu vẫn đang khoanh vùng. | Hợp lý không? | 007 FR-066 |
| B3.4 | Người bị ngã lần mới thì lịch theo dõi sau ngã mới thay lịch cũ, không chạy song song. | Điều dưỡng có muốn giữ cả hai lịch không? | 007 FR-052 |

**Đề xuất trả lời:**

| Câu | Đề xuất trả lời | Kết quả |
| --- | --- | --- |
| B3.1 | Hỏng điện, nước, thang máy ảnh hưởng an toàn người cao tuổi, cần ghi và theo dõi xử lý. | Đồng ý |
| B3.2 | Cấp 2 đã tới Quản lý viện, trên đó không còn cấp nào trong viện nên không leo thang thêm. Nhưng chỉ hiện nổi trên màn hình là chưa đủ khi không ai mở máy. Bổ sung: cảnh báo "đã leo thang tối đa" được **nhắc lại** cho những người nhận ở cấp 2 theo chu kỳ bằng thời hạn tiếp nhận của mức cảnh báo (9.3) tới khi có người tiếp nhận. | Không — sửa 007 FR-034 |
| B3.3 | Đóng sự cố khi khu còn khoanh vùng là mâu thuẫn. | Đồng ý |
| B3.4 | Lịch theo dõi tính từ lần ngã mới luôn phủ muộn hơn lịch cũ; chạy song song chỉ sinh việc trùng. Kết quả đã ghi của lịch cũ vẫn giữ. | Đồng ý |

### B4. Ca trực và bàn giao

| # | Cách hệ thống đang định làm | Câu hỏi | Mã |
| --- | --- | --- | --- |
| B4.1 ★ | Sau khi lịch ca đã công bố, Trưởng tầng (tầng của ca) hoặc Quản lý viện được bổ sung người vào ca và đổi người phụ trách ca; riêng hủy một ca chưa bắt đầu thì chỉ Quản lý viện làm, kèm lý do. Các việc này **không** cần duyệt (chỉ đổi ca và nghỉ đột xuất mới cần duyệt). | Việc sửa lịch sau khi công bố có cần ai duyệt không? | 008 FR-018, FR-019, FR-021 |
| B4.2 | Lịch ca của tầng do Trưởng tầng được giao hoặc Quản lý viện công bố; lịch toàn viện (không gắn tầng) chỉ do Quản lý viện lập. | Cách chia này có đúng với viện không? | 008 FR-016 |
| B4.3 | Có thể có nhiều Bác sĩ trực cùng lúc. | Viện có trực nhiều bác sĩ cùng ca không? | 008 FR-023 |
| B4.4 | Tới giờ ca sau mà bàn giao chưa được xác nhận, các cảnh báo, sự cố đang mở của người đã hết ca được chuyển tạm cho Người phụ trách ca sau (không có thì Trưởng tầng), mang dấu "tạm nhận". | Thực tế ai chịu trách nhiệm trong khoảng chưa nhận bàn giao? | 008 FR-044a |
| B4.5 | Thông tin cá nhân của nhân viên chia hai nhóm. Nhóm công khai (họ tên, chức danh, chuyên môn) ai cũng xem được. Nhóm hạn chế (số điện thoại, số giấy phép, lý do nghỉ việc) chỉ Quản lý viện xem đủ; Trưởng tầng chỉ xem thêm số liên hệ của nhân viên có ca trong tầng mình. Lý do vắng ca chỉ người ghi, Trưởng tầng của tầng và Quản lý viện xem; thông báo về vắng ca không nêu lý do. | Mức riêng tư này có phù hợp với viện không? | 008 FR-052, FR-053 |

**Đề xuất trả lời:**

| Câu | Đề xuất trả lời | Kết quả |
| --- | --- | --- |
| B4.1 | Người sửa (Trưởng tầng, Quản lý viện) chính là người có quyền duyệt, nên thêm bước duyệt không có tác dụng; hủy ca chỉ Quản lý viện, có lý do và nhật ký. | Đồng ý |
| B4.2 | Đúng phân cấp của viện. | Đồng ý |
| B4.3 | Mô hình cho phép nhiều Bác sĩ nhưng không bắt buộc; viện một bác sĩ vẫn dùng được. | Đồng ý |
| B4.4 | Luôn có người chịu trách nhiệm trong khoảng chưa nhận bàn giao. | Đồng ý |
| B4.5 | Phù hợp nguyên tắc dữ liệu cá nhân chỉ dùng đúng mục đích công việc. | Đồng ý |

### B5. Thông báo và gọi điện

| # | Cách hệ thống đang định làm | Câu hỏi | Mã |
| --- | --- | --- | --- |
| B5.1 ★ | Thông báo khẩn cấp gửi thất bại qua mọi kênh (ứng dụng, tin nhắn) thì hệ thống tạo ngay yêu cầu gọi điện cho người có trách nhiệm. | Ai trong viện sẽ nhận và thực hiện cuộc gọi đó? | 009 FR-028 |
| B5.2 | Khi người cần nhận thông báo không có (nghỉ, chưa phân công), thông báo được chuyển theo thứ tự: Điều dưỡng phụ trách → Người phụ trách ca của tầng → Trưởng tầng → Quản lý viện. Tầng tạm chưa có Trưởng tầng thì Người phụ trách ca nhận thay. Thông báo khẩn cấp mà vẫn không có ai nhận thì mọi Quản lý viện và Trưởng tầng đều nhận. | Thứ tự thay thế này có đúng với cách viện báo tin không? | 009 FR-010, FR-011 |
| B5.3 | Thông báo mức Trung bình gửi người thân mà thất bại ở mọi kênh (không có tài khoản cổng, tin nhắn lỗi hoặc không có số) thì người thân đó vào danh sách "cần liên hệ trực tiếp" để Hành chính gọi và ghi kết quả. | Hành chính có làm được việc này không? | 009 FR-021 |
| B5.4 | Tin nhắn gửi nhân viên không ghi họ tên người cao tuổi hay thông tin sức khỏe. Tin nhắn gửi người thân được ghi họ tên người nhà của họ, nhưng không ghi chẩn đoán, chỉ số, thuốc; chi tiết phải xem trong ứng dụng. | Nhân viên và người thân có chấp nhận mức nội dung này không? | 009 FR-019 |
| B5.5 | Người vừa gây ra một sự kiện không nhận thông báo mức nhẹ về chính sự kiện đó. | Hợp lý không? | 009 FR-012 |
| B5.6 | Sau khi người cao tuổi qua đời, các thông báo phát sinh sau thời điểm đó không gửi cho người thân, trừ các thông báo về việc cần làm sau qua đời gửi người đại diện. Tin báo qua đời được gửi ở mức khẩn cấp. | Có thông báo nào khác gia đình vẫn cần nhận sau đó không? | 009 FR-015 |

**Đề xuất trả lời:**

| Câu | Đề xuất trả lời | Kết quả |
| --- | --- | --- |
| B5.1 | Người gọi đã chốt ở Q-95: Người phụ trách ca → Trưởng tầng → Quản lý viện. | Đồng ý |
| B5.2 | Đúng thứ bậc báo tin của viện. Việc gửi cho mọi Quản lý viện và Trưởng tầng chỉ xảy ra với thông báo khẩn cấp khi không còn ai nhận, nên không gây gửi trùng thường xuyên. | Đồng ý |
| B5.3 | Hành chính đang là đầu mối liên lạc với gia đình. | Đồng ý |
| B5.4 | Thông tin sức khỏe là dữ liệu cá nhân nhạy cảm (Nghị định 13/2023/NĐ-CP), không đưa vào tin nhắn. | Đồng ý |
| B5.5 | Tránh báo cho chính người vừa thao tác. | Đồng ý |
| B5.6 | Quyết toán, nhận đồ gửi đã nằm trong danh sách việc sau qua đời (Q-222), nên người đại diện vẫn nhận đủ thông báo cần thiết. | Đồng ý |

### B6. Người thân và cổng thông tin

| # | Cách hệ thống đang định làm | Câu hỏi | Mã |
| --- | --- | --- | --- |
| B6.1 ★ | Khi có nhiều người đại diện, xác nhận của **một** người là đủ cho các thay đổi về quyền và danh sách được phép đón. | Viện có yêu cầu mọi người đại diện cùng đồng ý không? | 012 FR-016 |
| B6.2 | Yêu cầu do người đại diện tự gửi qua cổng coi như đã được xác nhận. | Hợp lý không? | 012 FR-015 |
| B6.3 | Người thân tự bỏ tên mình khỏi danh sách được phép đón, hoặc tự tắt quyền của mình, mà không cần duyệt. | Hợp lý không? | 012 FR-014 |
| B6.4 | Người liên hệ chính luôn nhận thông báo khẩn, không tắt được. | Có gia đình nào muốn tắt không? | 012 FR-009 |
| B6.5 | Lượt thăm đã duyệt tự hủy khi người cao tuổi vắng (đi viện, về nhà). Tắt quyền đăng ký thăm của một người thân không hủy các lượt đã duyệt. | Viện muốn xử lý lượt thăm đã hẹn như thế nào? | 012 FR-035, FR-036 |
| B6.6 | Không gửi bản tin định kỳ nếu người cao tuổi vắng cả kỳ. Khi người cao tuổi kết thúc lưu trú, bản tin đang chờ duyệt vẫn được gửi cho kỳ trước đó; nếu người cao tuổi qua đời thì bản tin đó không gửi. | Hợp lý không? | 012 FR-051, FR-075 |
| B6.7 | Người thân hỏi về sức khỏe, thuốc mà không có quyền xem sức khỏe thì nhân viên được cảnh báo khi soạn, và người thân chỉ thấy phần trả lời chung. Phản hồi quá hạn được báo Quản lý viện đúng một lần. | Thực tế ai trả lời câu hỏi sức khỏe của gia đình? Báo một lần có đủ không? | 012 FR-058, FR-063 |

**Đề xuất trả lời:**

| Câu | Đề xuất trả lời | Kết quả |
| --- | --- | --- |
| B6.1 | Giữ "một người đại diện là đủ" để không kẹt việc khi người đại diện ở xa. Bổ sung: khi danh sách được phép đón hoặc quyền thay đổi, các người đại diện còn lại được **thông báo mức Nhẹ**, để phát hiện sớm tranh chấp trong gia đình. | Đồng ý, bổ sung — sửa 012 FR-016 |
| B6.2 | Người đại diện đăng nhập cổng đã được xác thực; bắt họ xác nhận lại yêu cầu của chính mình là thừa. | Đồng ý |
| B6.3 | Tự rút quyền của chính mình không làm tăng rủi ro. | Đồng ý |
| B6.4 | Viện cần luôn có một người nhà nhận tin khẩn; ai không muốn thì đổi người liên hệ chính. | Đồng ý |
| B6.5 | Người cao tuổi vắng thì không có ai để thăm; lượt đã duyệt của người bị tắt quyền vẫn giữ vì đã được xét lúc duyệt. | Đồng ý |
| B6.6 | Hợp lý. | Đồng ý |
| B6.7 | Người trả lời là người phụ trách phản hồi do Quản lý viện giao; câu hỏi sức khỏe giao Điều dưỡng phụ trách hoặc Bác sĩ. Báo quá hạn một lần là đủ vì phản hồi quá hạn nằm trong danh sách của Quản lý viện tới khi xử lý (FR-063). | Đồng ý |

### B7. Hồ sơ, tiếp nhận, hợp đồng và kết thúc lưu trú

| # | Cách hệ thống đang định làm | Câu hỏi | Mã |
| --- | --- | --- | --- |
| B7.1 ★ | Hợp đồng nội trú chỉ được ghi là đã ký khi người cao tuổi đã được xếp giường từ ngày bắt đầu hợp đồng. Phí tính từ ngày bắt đầu hợp đồng, và người cao tuổi không vào ở sớm hơn ngày đó. | Viện có khi nào ký hợp đồng trước khi có giường, hoặc nhận người vào trước ngày bắt đầu hợp đồng không? | Q-22, Q-28 |
| B7.2 | Hợp đồng qua ngày kết thúc mà chưa gia hạn hay kết thúc thì vẫn hiệu lực, tính phí theo điều khoản cũ; quá 7 ngày thì báo Quản lý viện. | 7 ngày có hợp lý không? | Q-20, CFG-M02-10 |
| B7.3 | Bảng giá mới không áp cho hợp đồng đang hiệu lực; muốn đổi giá thì lập phụ lục hợp đồng. Các khoản ngoài hợp đồng tính theo giá tại ngày phát sinh. | Khi tăng giá, viện áp cho người đang ở như thế nào? | Q-25 |
| B7.4 ★ | Kết thúc lưu trú làm hai bước: trước hết chốt chi phí kỳ cuối và quyết toán số dư, tiền cọc; sau đó mới thực hiện lệnh kết thúc lưu trú. | Thực tế viện làm thủ tục ra viện theo thứ tự nào? Có khi nào người cao tuổi rời viện trước khi quyết toán xong không? | Q-21, Q-222 |
| B7.5 | Hủy tiếp nhận sau ngày bắt đầu hợp đồng: vẫn tính phí tới hết ngày hủy; muốn miễn giảm thì lập khoản điều chỉnh có Quản lý viện duyệt. | Viện có thu phí trong trường hợp này không? | Q-27 |
| B7.6 | Người đang tạm vắng (ví dụ về nhà) mà chuyển sang nằm viện: ngày vắng đếm lại từ 1 theo dòng "Bệnh viện" của bảng phí vắng; giường vẫn được giữ liên tục. | Viện tính phí vắng trong trường hợp này thế nào? | Q-24 |
| B7.7 | Bác sĩ duyệt đánh giá được gắn thêm cờ nguy cơ (ngã, loét, đi lạc) kèm lý do, nhưng không được bỏ cờ mà thang điểm đã đề xuất. | Bác sĩ có khi nào cần bỏ cờ hệ thống đề xuất không? | Q-13 |

**Đề xuất trả lời:**

| Câu | Đề xuất trả lời | Kết quả |
| --- | --- | --- |
| B7.1 | Hợp đồng nội trú mà chưa có giường thì phí tính từ ngày bắt đầu nhưng không nhận người vào được. Người chưa có giường nằm ở danh sách chờ. Muốn vào sớm hơn thì lập hợp đồng với ngày bắt đầu sớm hơn. | Đồng ý |
| B7.2 | 7 ngày đã là tham số CFG-M02-10. | Đồng ý |
| B7.3 | Đơn phương đổi giá hợp đồng đang hiệu lực là trái thỏa thuận; tăng giá phải qua phụ lục có ký. | Đồng ý |
| B7.4 | Thứ tự hai bước giữ nguyên. Trường hợp gia đình đón về trước khi quyết toán xong đã có đường: Quản lý viện duyệt ngoại lệ (Q-222), phần tiền còn lại xử lý sau khi đóng hồ sơ (Q-224). | Đồng ý |
| B7.5 | Nhất quán với việc tính phí từ ngày bắt đầu hợp đồng (Q-22); chính sách hủy riêng của viện thể hiện bằng khoản điều chỉnh có duyệt. | Đồng ý |
| B7.6 | Theo bảng phí vắng (xem D3). | Đồng ý |
| B7.7 | Thang điểm sàng lọc (ví dụ nguy cơ ngã) có thể đề xuất sai; Bác sĩ phải được dùng phán đoán lâm sàng. Cho Bác sĩ **bỏ cờ đề xuất**, bắt buộc lý do; lịch sử đánh giá vẫn hiện cờ đã đề xuất và lý do bỏ. | Không — sửa Q-13 |

### B8. Giường, phòng và khu bán trú

| # | Cách hệ thống đang định làm | Câu hỏi | Mã |
| --- | --- | --- | --- |
| B8.1 ★ | Chuyển giường gấp vì lý do y tế hoặc an toàn sang phòng khác giá: chuyển ngay. Hệ thống tự tạo yêu cầu đổi giá chờ Quản lý viện duyệt; nếu không duyệt thì giữ giá cũ và nhắc chuyển về phòng cùng giá. | Khi chuyển gấp sang phòng đắt hơn, viện có thu thêm tiền không? | Q-44, Q-52 |
| B8.2 | Ghi bù việc xếp giường sau khi người cao tuổi đã vào ở: Hành chính, Trưởng tầng tự ghi nếu lùi không quá 24 giờ; lùi xa hơn thì Quản lý viện duyệt. | Viện có hay ghi bù không? 24 giờ có đủ không? | Q-43, CFG-M03-07 |
| B8.3 | Không nhận người vào ở (hoàn tất tiếp nhận) khi giường đã xếp chưa dọn xong, trừ khi đổi ngay sang một giường trống khác. | Thực tế có khi nào người mới đến mà giường chưa dọn không? Viện xử lý thế nào? | Q-50 |
| B8.4 | Người tạm vắng trở về đúng lúc phòng của mình đang cách ly: vẫn ghi nhận trở về, báo Bác sĩ và Trưởng tầng; chỉ được ở lại giường cũ khi Bác sĩ xác nhận. | Hợp lý không? | Q-46 |
| B8.5 | Đổi mức chăm sóc không bị chặn khi phòng hiện tại không phù hợp mức mới; hệ thống nhắc chuyển giường mỗi ngày tới khi chuyển. | Viện có bắt buộc chuyển phòng khi mức chăm sóc thay đổi không? | Q-19, CFG-M03-02 |
| B8.6 | Cả viện chỉ có một khu nghỉ ban ngày cho người bán trú, sức chứa mỗi buổi là một con số chung. | Viện có một hay nhiều khu cho người bán trú? | Q-45, CFG-M03-01 |

**Đề xuất trả lời:**

| Câu | Đề xuất trả lời | Kết quả |
| --- | --- | --- |
| B8.1 | Thu thêm hay không do Quản lý viện quyết từng trường hợp qua yêu cầu đổi giá; không duyệt thì giữ giá cũ. Cách này đã đủ linh hoạt. | Đồng ý |
| B8.2 | 24 giờ đã là tham số CFG-M03-07. | Đồng ý |
| B8.3 | Không nhận người vào giường chưa vệ sinh; đổi ngay sang giường trống khác là cách xử lý thực tế. | Đồng ý |
| B8.4 | Hợp lý về kiểm soát lây nhiễm. | Đồng ý |
| B8.5 | Không nên chặn thay đổi mức chăm sóc (việc chăm sóc cần áp dụng ngay); chuyển phòng làm sau theo nhắc. | Đồng ý |
| B8.6 | Viện quy mô vừa có một khu nghỉ ban ngày; tách nhiều khu để giai đoạn sau khi có nhu cầu. | Đồng ý |

### B9. Chi phí phát sinh và chốt chi phí

| # | Cách hệ thống đang định làm | Câu hỏi | Mã |
| --- | --- | --- | --- |
| B9.1 ★ | Mỗi tháng, mỗi người cao tuổi có một bảng chi phí. Hành chính kiểm tra rồi gửi chốt từng bảng; Quản lý viện chốt. Hạn chốt là ngày cuối của tháng liền sau. Kỳ của viện xong khi mọi bảng đã chốt. | Thực tế ai kiểm tra, ai chốt? Viện chốt từng người hay cả viện một lần? Hạn đó có ổn không? | Q-131, Q-132, CFG-M11-01 |
| B9.2 | Khoản phát sinh sau khi kỳ cuối đã chốt (ví dụ liều thuốc sáng ngày ra viện) vào một bảng bổ sung và không chặn thủ tục ra viện. | Viện có thu các khoản phát sinh muộn như vậy không? | Q-134 |
| B9.3 | Ngày người cao tuổi qua đời vẫn tính trọn ngày phí lưu trú; các khoản phát sinh sau thời điểm qua đời thì không tính. | Viện có tính phí ngày qua đời không? | Q-141 |
| B9.4 | Mua hộ vượt số tiền người đại diện đã đồng ý: vẫn ghi đã mua, khoản mang dấu; Quản lý viện phải ghi lý do khi duyệt; người đại diện được báo. | Viện có cho mua vượt không, hay phải hỏi lại gia đình trước? | Q-137 |
| B9.5 | Người bán trú đến thêm một buổi ngoài lịch: tính 100% giá một buổi, ngoài giá tháng. Buổi rơi vào ngày khu bán trú nghỉ thì không tính. | Viện tính phí buổi đến thêm như vậy không? | Q-138, Q-140 |
| B9.6 | Khoản chi phí chưa có đơn giá: Quản lý viện tạo đơn giá có hiệu lực lùi về trước; không ai nhập tay giá cho từng khoản. | Có khi nào viện cần nhập giá riêng cho một lần phát sinh không? | Q-135 |

**Đề xuất trả lời:**

| Câu | Đề xuất trả lời | Kết quả |
| --- | --- | --- |
| B9.1 | Theo quy trình nguồn (15.x: Hành chính kiểm tra → Quản lý viện duyệt, chốt; Q-212). Chốt từng người để bảng nào xong gửi gia đình trước. Hạn cuối tháng liền sau là mức trần, không phải ngày chốt thường lệ. | Đồng ý |
| B9.2 | Khoản phát sinh muộn vẫn thu, không giữ người cao tuổi lại vì khoản nhỏ. | Đồng ý |
| B9.3 | Tính trọn ngày theo hệ số của ngày đó; miễn giảm (nếu viện muốn) bằng khoản điều chỉnh có duyệt. | Đồng ý |
| B9.4 | Tiêu vượt tiền của gia đình mà không hỏi dễ gây khiếu nại. Vượt trong mức nhỏ \[10%\] (tham số mới) thì mua luôn và báo như hiện tại; vượt hơn thì **hỏi lại người đại diện trước khi mua**; trường hợp gấp (thuốc, vật tư y tế) được mua trước, Quản lý viện duyệt kèm lý do. | Không — sửa Q-137 |
| B9.5 | Theo Q-140. | Đồng ý |
| B9.6 | Khoản thuộc danh mục phải có đơn giá để tính đồng nhất. Khoản giá riêng một lần dùng khoản điều chỉnh có Quản lý viện duyệt (BR-M11-05), không cần nhập giá tay. | Đồng ý |

### B10. Số dư, thu chi và quỹ tiền mặt

| # | Cách hệ thống đang định làm | Câu hỏi | Mã |
| --- | --- | --- | --- |
| B10.1 ★ | Sổ số dư và sổ tiền cọc mở khi hợp đồng chuyển "chờ ký", để thu cọc lúc ký. Hợp đồng bị hủy trước khi hiệu lực thì hoàn cọc. | Viện thu cọc lúc nào: lúc ký, trước khi ký hay khi người cao tuổi vào ở? | Q-225 |
| B10.2 ★ | Phiếu thu tiền mặt có hiệu lực ngay, số phiếu liên tục. Cuối ngày Kế toán lập chốt quỹ, Quản lý viện xác nhận. Chênh lệch quỹ không làm đổi số dư của người cao tuổi. | Viện có chốt quỹ hằng ngày không? Ai xác nhận? | Q-226 |
| B10.3 | Gia đình chuyển một khoản gộp (ví dụ cả tiền cọc và tiền nộp, hoặc cho hai người): Kế toán ghi cả khoản vào một sổ rồi lập cặp điều chỉnh để chuyển phần còn lại; Quản lý viện duyệt. | Thực tế có hay gặp chuyển khoản gộp không? | Q-227 |
| B10.4 | Hoàn tiền khi người cao tuổi còn đang ở: không vượt số dư trừ chi phí chưa chốt; Quản lý viện duyệt. | Gia đình có hay xin rút bớt tiền khi còn ở không? | Q-229 |
| B10.5 | Báo "sắp hết tiền", "còn nợ" cho người đại diện và Kế toán. Khi số dư tốt lên thì không báo ngay. Người đại diện không có quyền xem chi phí chỉ nhận thông báo chung, không có số tiền. | Gia đình có muốn được báo khi đã nộp đủ không? | Q-228 |
| B10.6 | Hồ sơ đã đóng mà số dư vẫn khác 0: Kế toán được nhắc mỗi ngày tới khi số dư về 0; hồ sơ không mở lại. | Viện xử lý tiền thừa, tiền thiếu sau khi đóng hồ sơ thế nào? | Q-224, CFG-M02-09 |

**Đề xuất trả lời:**

| Câu | Đề xuất trả lời | Kết quả |
| --- | --- | --- |
| B10.1 | Thu cọc khi ký hợp đồng là thông lệ. | Đồng ý |
| B10.2 | Chốt quỹ hằng ngày là kiểm soát tiền mặt tối thiểu; Quản lý viện xác nhận. | Đồng ý |
| B10.3 | Có gặp (gia đình có hai người ở viện, nộp gộp cọc và tiền tháng). | Đồng ý |
| B10.4 | Ít gặp; có kiểm tra số dư khả dụng và duyệt là đủ. | Đồng ý |
| B10.5 | Gia đình đã bị báo "sắp hết tiền", "còn nợ" thì cần biết khoản nộp đã được ghi nhận, nếu không sẽ gọi hỏi. Bổ sung: khi **một khoản nộp** làm tình trạng số dư trở về bình thường, gửi người đại diện thông báo mức Nhẹ "đã ghi nhận khoản nộp"; không kèm số tiền với người không có quyền xem chi phí. Các biến động tốt lên khác vẫn không báo. | Không — sửa Q-228 |
| B10.6 | Hợp lý. | Đồng ý |

### B11. Dinh dưỡng và bữa ăn

| # | Cách hệ thống đang định làm | Câu hỏi | Mã |
| --- | --- | --- | --- |
| B11.1 ★ | Dinh dưỡng viên tự công bố thực đơn tuần sau khi hệ thống kiểm tra tự động (dị ứng, chế độ ăn); không cần ai duyệt. | Viện có cần Bác sĩ hay Quản lý viện duyệt thực đơn không? | Q-142 |
| B11.2 | Đổi chế độ ăn cần Bác sĩ duyệt khi: đổi, thêm hoặc bỏ chế độ ăn điều trị; chuyển sang thức ăn mềm, xay hơn; hoặc bỏ một hạn chế. Các thay đổi khác áp dụng ngay và báo Bác sĩ. | Ranh giới này có đúng với cách viện làm không? | Q-145 |
| B11.3 ★ | Ngoài giờ làm của dinh dưỡng viên, suất chưa có món thay thế tự dùng "món an toàn" của chế độ ăn đó; nếu vẫn không hợp thì báo Điều dưỡng phụ trách. | Buổi tối, cuối tuần, ai quyết định đổi món cho người cao tuổi? | Q-147 |
| B11.4 | Mỗi bữa lưu mẫu mọi món đã nấu, mỗi món một mẫu; chưa ghi lưu mẫu thì phiếu bữa không chuyển "đã giao" được. Món không lưu được thì ghi lý do. | Bếp có lưu mẫu mọi món không? Có chấp nhận việc chặn giao khi chưa lưu mẫu không? | Q-39, Q-146 |
| B11.5 | Người đang vắng mà dự kiến về trước giờ ăn vẫn được tính suất; về trễ thì suất được giữ tại tầng. | Hợp lý không, hay chỉ tính suất khi người đã về? | Q-144 |
| B11.6 | Người nhận phiếu bữa tại tầng: Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc có ca ở tầng đó. | Thực tế ai nhận cơm từ bếp và kiểm đếm? | Q-143 |

**Đề xuất trả lời:**

| Câu | Đề xuất trả lời | Kết quả |
| --- | --- | --- |
| B11.1 | Dinh dưỡng viên là người có chuyên môn; kiểm tra tự động đã chặn dị ứng và chế độ ăn. Bắt Bác sĩ duyệt thực đơn hằng tuần tốn công mà ít tác dụng; chế độ ăn điều trị của từng người đã do Bác sĩ duyệt (B11.2). | Đồng ý |
| B11.2 | Đúng ranh giới chuyên môn. | Đồng ý |
| B11.3 | Món an toàn là món do Dinh dưỡng viên khai báo trước cho từng chế độ ăn, tức là danh sách đã được người có chuyên môn chọn. Ngoài đó, Điều dưỡng phụ trách quyết định. | Đồng ý |
| B11.4 | Lưu mẫu mọi món là yêu cầu với bếp ăn tập thể (Quyết định 1246/QĐ-BYT); ghi lưu mẫu trước khi giao chỉ mất một thao tác. | Đồng ý |
| B11.5 | Người về trễ vẫn cần ăn; suất giữ tại tầng tránh người cao tuổi bị đói. Nếu cuối cùng không về thì ghi "−1" để đối chiếu. | Đồng ý |
| B11.6 | Đúng. | Đồng ý |

### B12. Đồ gửi của người cao tuổi

| # | Cách hệ thống đang định làm | Câu hỏi | Mã |
| --- | --- | --- | --- |
| B12.1 ★ | Điện thoại được giao cho người cao tuổi tự giữ. Tiền mặt, trang sức chỉ được tự giữ khi người đại diện đồng ý cho đúng món đó và người cao tuổi không có cờ nguy cơ đi lạc. | Viện có cho người cao tuổi tự giữ tiền, trang sức không? | Q-155 |
| B12.2 | Hành chính trả được mọi đồ; Điều dưỡng chỉ trả đồ không có giá trị cho người có quyền nhận. Người cao tuổi tự nhận lại đồ cũng cần người đại diện xác nhận; không liên lạc được người đại diện sau 4 giờ thì Quản lý viện duyệt thay. | Ai trong viện được trả đồ? Có cần gia đình xác nhận khi trả đồ cho chính người cao tuổi không? | Q-149, Q-150, Q-153, CFG-M12-05 |
| B12.3 | Hằng tháng Hành chính kiểm kê đồ có giá trị đang giữ, hạn 3 ngày; không tìm thấy thì báo thất lạc. | Viện có kiểm kê đồ gửi định kỳ không? Ai làm? | Q-154, CFG-M12-04 |
| B12.4 | Đồ không ai nhận sau 90 ngày kể từ khi hồ sơ đóng: Hành chính lập đề nghị xử lý kèm biên bản, Quản lý viện duyệt. Tiền mặt chỉ được chuyển cơ quan có thẩm quyền. | Viện đang xử lý đồ bỏ lại như thế nào? | Q-152, Q-159, CFG-M12-03 |
| B12.5 | Đồ bị hư hỏng chặn thủ tục ra viện tới khi trả; đồ thất lạc không chặn (sự cố đi kèm vẫn chặn tới khi đóng). Bồi thường cho đồ mất, hỏng xử lý ngoài hệ thống. | Viện có bồi thường không? Có cần ghi bồi thường vào chi phí không? | Q-148 |

**Đề xuất trả lời:**

| Câu | Đề xuất trả lời | Kết quả |
| --- | --- | --- |
| B12.1 | Người có nguy cơ đi lạc tự giữ tiền, trang sức dễ mất; giữ điều kiện hiện tại. | Đồng ý |
| B12.2 | Hành chính trả mọi đồ, Điều dưỡng trả đồ không có giá trị: đồng ý. Nhưng người cao tuổi còn minh mẫn nhận lại đồ của chính mình mà phải chờ gia đình xác nhận là hạn chế quyền tự quyết tài sản. Sửa: người cao tuổi **tự nhận lại không cần người đại diện xác nhận**, trừ khi có cờ nguy cơ đi lạc; tiền mặt, trang sức vẫn theo điều kiện của B12.1 (BR-M12-06). | Không — sửa Q-149 |
| B12.3 | Hành chính kiểm kê hằng tháng. | Đồng ý |
| B12.4 | 90 ngày đã là tham số CFG-M12-03. | Đồng ý |
| B12.5 | Đồ hư hỏng vẫn phải trả tận tay gia đình kèm biên bản, nên chờ trả xong mới kết thúc là hợp lý. Bồi thường xử lý ngoài hệ thống ở giai đoạn đầu. | Đồng ý |

### B13. Hoạt động, chuyến đi và kiểm tra chất lượng

| # | Cách hệ thống đang định làm | Câu hỏi | Mã |
| --- | --- | --- | --- |
| B13.1 ★ | Trưởng đoàn chuyến đi là Trưởng tầng, Nhân viên chăm sóc hoặc Điều dưỡng; người đi cùng là nhân viên bất kỳ có ca trong giờ chuyến. Trưởng tầng đánh giá ai được đi. | Thực tế ai dẫn đoàn? Ai quyết định người cao tuổi đi được hay không? | Q-161, Q-214 |
| B13.2 | Bác sĩ gắn chỉ định hạn chế hoạt động (mọi hoạt động, hoạt động nhóm, ngoài viện, theo loại). Điều dưỡng được gắn tạm tối đa 24 giờ, Bác sĩ xác nhận hoặc gỡ. | Điều dưỡng có cần quyền gắn tạm như vậy không? | Q-162, CFG-M04-14 |
| B13.3 | Người thân không đón thẳng người cao tuổi từ điểm đến của chuyến đi; người cao tuổi phải về viện điểm danh trước rồi mới được đón. | Có gia đình nào muốn đón ở điểm đến không? | Q-166 |
| B13.4 | Chuyến về quá giờ 30 phút thì báo Trưởng đoàn và Trưởng tầng; quá 60 phút thì báo thêm Quản lý viện. | Mốc báo này có hợp lý không? | Q-172, CFG-M04-07 |
| B13.5 | Người bỏ về giữa buổi hoạt động vẫn tính là có mặt, tính phí và tính là đã tham gia. | Hợp lý không? | Q-173 |
| B13.6 ★ | Kiểm tra chất lượng: hệ thống chọn ngẫu nhiên 5% công việc ngay khi hoàn thành, Trưởng tầng kiểm tra trước khi hết ca. Công việc do chính Trưởng tầng làm không bị kiểm tra. Ca đêm không có Trưởng tầng sẽ có nhiều mục quá hạn kiểm tra. | Ai kiểm tra ca đêm? Có nên cho Người phụ trách ca kiểm tra không? | Q-163, Q-168, CFG-M04-11 |
| B13.7 | Người bán trú tham gia hoạt động kết thúc sau giờ về: được, khi Hành chính ghi nhận người đại diện đồng ý cho đúng buổi đó. | Gia đình có chấp nhận cách xin đồng ý này không? | Q-167 |

**Đề xuất trả lời:**

| Câu | Đề xuất trả lời | Kết quả |
| --- | --- | --- |
| B13.1 | Theo Q-161, Q-214. Bác sĩ tham gia qua chỉ định hạn chế hoạt động (B13.2), nên Trưởng tầng không quyết một mình về mặt y tế. | Đồng ý |
| B13.2 | Cần cho tình huống ban đêm, cuối tuần khi Bác sĩ không có mặt. | Đồng ý |
| B13.3 | Bàn giao ở điểm đến khó kiểm soát danh tính người đón và trách nhiệm; về viện điểm danh trước rồi cho đón. | Đồng ý |
| B13.4 | Đã là tham số CFG-M04-07. | Đồng ý |
| B13.5 | Hợp lý; dấu "bỏ giữa chừng" vẫn được lưu để xem lại. | Đồng ý |
| B13.6 | Ca đêm không có Trưởng tầng thì mẫu kiểm tra luôn quá hạn, số liệu vô nghĩa. Sửa: trong ca không có Trưởng tầng, **Người phụ trách ca** kiểm tra; công việc do chính người đó làm bị loại khỏi mẫu (như Q-168). | Không — sửa Q-163, Q-168 |
| B13.7 | Ghi nhận đồng ý cho đúng buổi là đủ. | Đồng ý |

### B14. Lịch xoay ca, đổi ca và nghỉ đột xuất

| # | Cách hệ thống đang định làm | Câu hỏi | Mã |
| --- | --- | --- | --- |
| B14.1 ★ | Lịch còn ca thiếu người tối thiểu vẫn công bố được, người công bố ghi lý do; mỗi ca thiếu mở một cảnh báo cho Trưởng tầng. | Viện có công bố lịch khi còn thiếu người không? | Q-178, Q-189 |
| B14.2 | Đổi ca có hai dạng: đổi qua lại, hoặc nhận thay một chiều; chỉ trong cùng tầng hoặc cùng nhóm toàn viện. Điều người sang tầng khác dùng lệnh bổ sung nhân viên vào ca. | Nhân viên có hay đổi ca với người tầng khác không? | Q-179 |
| B14.3 | Lời mời nhận ca thay gửi nhiều người; người nhận đầu tiên được xếp vào ca ngay, không cần Trưởng tầng xác nhận. Lời mời chỉ gửi trong ứng dụng, không gửi tin nhắn. | Trưởng tầng có muốn xác nhận người nhận thay không? Có cần gửi tin nhắn không? | Q-183, Q-186 |
| B14.4 | Nghỉ nhiều ca được duyệt một phần. Nghỉ biết trước cho tháng chưa có lịch thì xin theo ngày, hệ thống không xếp người đó vào các ngày ấy. | Hợp lý không? | Q-182, Q-184 |
| B14.5 | Người phụ trách ca xin đổi ca mà không có ai đủ điều kiện thay thì không duyệt được; xin nghỉ đột xuất thì vẫn duyệt, ca mang dấu "thiếu người phụ trách". | Khi người phụ trách ca nghỉ gấp, thực tế ai thay? | Q-187 |
| B14.6 | Số người tối thiểu của ca chỉ tính người có giấy phép, chứng chỉ còn hạn trong toàn ca. Điều dưỡng không được tính vào chỉ tiêu Nhân viên chăm sóc. | Hợp lý không? | Q-188, Q-221 |

**Đề xuất trả lời:**

| Câu | Đề xuất trả lời | Kết quả |
| --- | --- | --- |
| B14.1 | Thiếu người là chuyện thường; chặn công bố thì nhân viên không có lịch để đi làm. | Đồng ý |
| B14.2 | Nhân viên cần quen người cao tuổi của tầng; điều người sang tầng khác để Trưởng tầng quyết qua lệnh bổ sung. | Đồng ý |
| B14.3 | Hệ thống đã kiểm tra lại điều kiện người nhận (Q-183) và Trưởng tầng vẫn sửa được sau đó; thêm bước xác nhận làm chậm lấp ca. Chưa có nhà cung cấp tin nhắn (Q-06) nên chỉ gửi trong ứng dụng. | Đồng ý |
| B14.4 | Hợp lý. | Đồng ý |
| B14.5 | Khi ca thiếu người phụ trách, chuỗi thay thế đã có: Trưởng tầng nhận thông báo, cuộc gọi thay (B5.2, Q-95). | Đồng ý |
| B14.6 | Người có giấy phép, chứng chỉ hết hạn không được làm việc chuyên môn thì không được tính phủ. | Đồng ý |

### B15. Báo cáo

| # | Cách hệ thống đang định làm | Câu hỏi | Mã |
| --- | --- | --- | --- |
| B15.1 | Ai xem được báo cáo thì xuất được ra file, trong phạm vi của mình; mỗi lần xuất được ghi lại. Xuất danh sách có tên người cao tuổi thì Quản lý viện phải ghi mục đích. Số giờ làm theo từng nhân viên chỉ Quản lý viện xuất. | Viện có cần xuất báo cáo ra file không? Dùng cho việc gì? | Q-193, Q-197, Q-198 |
| B15.2 | Giai đoạn đầu có báo cáo người cao tuổi, chăm sóc, sức khỏe, chi phí, nhân sự và ca trực, cùng dashboard. Báo cáo thăm, phản hồi, đồ gửi, thông báo để giai đoạn sau. | Viện có cần báo cáo nào trong nhóm để sau ngay từ đầu không? | Q-194 |
| B15.3 | Trưởng tầng xem báo cáo của tầng đang được giao, kể cả số liệu trước khi mình được giao. | Hợp lý không? | Q-192 |

**Đề xuất trả lời:**

| Câu | Đề xuất trả lời | Kết quả |
| --- | --- | --- |
| B15.1 | Tách quyền theo loại dữ liệu. Số liệu tổng hợp: ai xem được thì xuất được, trong phạm vi của mình. **File có định danh người cao tuổi chỉ Quản lý viện xuất**, bắt buộc mục đích (Q-198), vì dữ liệu sức khỏe là dữ liệu nhạy cảm (Nghị định 13/2023/NĐ-CP). | Đồng ý (khớp Q-193 hiện hành) |
| B15.2 | Số suất theo bữa hằng ngày đã có trên phiếu bữa ăn theo tầng; các báo cáo để sau không cần cho vận hành ngày đầu. | Đồng ý |
| B15.3 | Trưởng tầng mới cần xem lịch sử tầng để nắm tình hình. | Đồng ý |

---

## Phần C — Nhóm tự chốt, không hỏi người vận hành

Các mặc định sau là chi tiết kỹ thuật hoặc cách viết tài liệu, không ảnh hưởng tới cách viện làm việc. Nhóm phân tích tự xác nhận:

- 005: sửa loại công việc chỉ áp cho công việc tạo sau (FR-038); cập nhật bản nháp bàn giao khi người cao tuổi chuyển trạng thái cuối (FR-010); bảng trạng thái yêu cầu xem xét kế hoạch (FR-011).
- 006: phiếu đối chiếu bị hủy khi người cao tuổi chuyển viện lại (FR-035).
- 007: cách nhắc thiết lập ngưỡng cá nhân (FR-025); phạm vi cơ sở suy từ giấy phép (FR-071); danh mục quy tắc xu hướng có mức cấu hình (FR-039).
- 008: lệnh ghi nhận thu hồi giấy phép, nhận lại làm việc (FR-003, FR-006); quy tắc nhân viên chính (FR-028); phần "phát sinh sau khi lập" của bàn giao (FR-041); sao chép phân công, phân công nháp (FR-025, FR-032); trạng thái ca Chờ bàn giao (FR-022); ca qua hai tháng (FR-012); ngoại lệ nhật ký cho lịch nháp (FR-055).
- 009: khóa sự kiện, một yêu cầu cho mỗi người nhận, dữ liệu trả lại module nguồn, điều kiện chung cho spec nguồn (FR-005, FR-007, FR-035, FR-043a); danh sách lỗi gửi (FR-038); nội dung mặc định khi thiếu (FR-003); định nghĩa "chuỗi gọi" (FR-029a, FR-030); đánh số CFG-M13-03 → 06; định nghĩa THONG_BAO.
- 012: tối đa một yêu cầu chờ cho mỗi quan hệ và quyền (FR-018); giá trị hiện hành theo yêu cầu có hiệu lực muộn nhất (FR-018); tham số nhắc riêng CFG-M10-11 (FR-019); nhật ký lượt xem sức khỏe qua bản tin (FR-048); quy ước thuật ngữ.
- 000: vòng đời yêu cầu phê duyệt chung, tự duyệt kèm lý do (Q-10), trạng thái "Áp dụng không thành" (Q-11); lý do bắt buộc với lệnh nhóm 2, đính chính, từ chối, hủy; quy tắc đính chính và "Hủy ghi nhận".
- 001: hồ sơ mới liên kết hồ sơ cũ khi người từng lưu trú quay lại, CCCD duy nhất trong các hồ sơ chưa ở trạng thái cuối (Q-12); trạng thái "Được thay thế" của yêu cầu phê duyệt.
- 002: phạm vi dữ liệu theo ca ± CFG-M15-07 (Q-14); hợp phạm vi khi một người có nhiều vai trò; từ chối mặc định khi thiếu dữ liệu nguồn; nhật ký ghi cả đăng nhập.
- 003: phân bổ đặt trước bắt đầu khi Hoàn tất tiếp nhận (Q-42); buổi bán trú có khung giờ, lịch đến chiếm mọi buổi giao nhau (Q-47); ghi giờ thực tế khi việc tự động chạy trễ, CFG-M03-08 (Q-48); mức quan trọng cố định của công việc vệ sinh (Q-49); người yêu cầu "Hệ thống" khi chuyển giường gấp (Q-52); nâng vệ sinh trả giường lên khử khuẩn khi danh sách nghi nhiễm cập nhật muộn (Q-53).
- 004: vòng đời hợp đồng (Trả về nháp, Đã hủy, Kết thúc khác Chấm dứt); hồ sơ chờ Từ chối hoặc Hủy chờ kéo theo Hủy tiếp nhận (Q-29); hệ số phí khi giữ giường, giải phóng giường, chờ quyết định (BR-M02-07).
- 010: kỳ chi phí luôn là tháng dương lịch (Q-132); vòng đời bảng chi phí, bảng bổ sung và kỳ của viện; khoản điều chỉnh bắt đầu ở Nháp (BR-M11-05); che tên thuốc trên khoản chi phí với người không có quyền xem thuốc (Q-133); thành phần chi phí tạm tính (Q-136); thuốc mua hộ không tính thêm theo liều (Q-139).
- 011: vòng đời bản gán chế độ ăn và thực đơn (Chờ hiệu lực, Rút lại, Đã hủy); mỗi bữa, mỗi tầng/khu có ít nhất một suất thì có đúng một phiếu (DBR-27); dừng phát sinh tự động ở giờ bữa, sau đó dùng yêu cầu suất bổ sung (FR-040); trạng thái đích của phiếu có sai lệch.
- 013: thang 4 mức tình trạng đồ gửi (Q-156); bằng chứng liên hệ tối thiểu trước khi Quản lý viện duyệt thay (Q-157); giới hạn đính chính bản ghi bàn giao (Q-160); tách đồ khi bàn giao một phần (FR-012); người ghi nhận thay người nhận khi Thất lạc (BR-M12-01).
- 014: buổi hết ngày chưa điểm danh đủ nhận "Không ghi nhận" (Q-169); đính chính điểm danh rời/về (Q-170); phạm vi kiểm tra chất lượng và công việc ngoại tuyến (Q-174); địa điểm, người phụ trách hoạt động toàn viện (Q-175); feature 005 sở hữu giờ về theo ngày của bán trú (Q-176).
- 015: cách tính số giờ đã làm khi sắp người thay (Q-180); nhân viên nhiều vai trò tính cho một dòng phủ (Q-181); không sửa mẫu xoay ca của nhóm đã có thành viên (Q-190); điều kiện vai trò khi đổi ca (Q-191); gỡ trưởng đoàn, người đi cùng khi đổi ca, nghỉ (Q-185).
- 016: phạm vi của người phụ trách ca trên dashboard (Q-195); không mở hồ sơ của người ngoài phạm vi từ danh sách chi tiết (Q-196); đếm leo thang sự cố (Q-199); tỷ lệ phục vụ của ca đã qua (Q-200); đếm bản ghi ngoại tuyến "chờ xem lại" (Q-201); độ dài tối đa một lần xem, xuất (CFG-M14-01).
- 017: quy đổi phí một ngày của hợp đồng bán trú khi tính số ngày còn đủ tiền (Q-223); trạng thái Đã hủy của giao dịch cần duyệt (Q-229); bảng bổ sung chưa chốt không chặn quyết toán (Q-230).
- 018: người tạm vắng, nằm viện vẫn giữ khai báo; tạo việc khai báo xóa cho cả khai báo Hết hạn khi kết thúc lưu trú (FR-008).
- 019: phiếu nhập, xuất có hiệu lực ngay, không có Nháp; đơn giá trên phiếu nhập chỉ để tham khảo; nhà cung cấp ghi văn bản tự do; nhắc chuyển người khỏi giường hỏng dùng lại CFG-M03-02.

---

## Phần D — Nếu còn thời gian: các quyết định còn mở từ trước

Các quyết định dưới đây đang chạy theo giá trị mặc định (mục 24.1). Người vận hành trả lời được phần lớn.

| # | Câu hỏi | Mặc định đang dùng | Mã |
| --- | --- | --- | --- |
| D1 | Khi mất mạng, nhân viên có ghi tạm trên máy rồi đồng bộ sau không? | Có; riêng việc khẩn cấp bắt buộc phải có mạng | Q-01 |
| D2 | Ai duyệt kế hoạch chăm sóc? | Bác sĩ; viện có thể cho điều dưỡng duyệt | Q-07 |
| D3 | Bảng tính phí khi người cao tuổi vắng (về nhà, đi viện) viện đang áp dụng là gì? | Theo bảng ví dụ ở 6.7 | Q-08 |
| D4 | Viện xoay ca thế nào? | 2 ca ngày/đêm | Q-09 |
| D5 | Thuốc gây nghiện, hướng thần có cần người chứng kiến khi phát, hoặc đếm số lượng còn lại không? | Không bổ sung | Q-63 |

Q-02 (ngưỡng thang điểm đánh giá), Q-03 (bản đồng ý chia sẻ dữ liệu), Q-05 (định dạng file kế toán), Q-06 (nhà cung cấp tin nhắn) cần bác sĩ, tư vấn pháp lý, kế toán hoặc Quản lý viện trả lời riêng.

**Đề xuất trả lời:**

| Câu | Đề xuất trả lời | Kết quả |
| --- | --- | --- |
| D1 | Giữ mặc định: ghi tạm khi mất mạng, đồng bộ sau; việc khẩn cấp bắt buộc trực tuyến. | Đồng ý |
| D2 | Giữ mặc định: Bác sĩ duyệt; viện gán thêm cho Điều dưỡng khi cần (Q-15). | Đồng ý |
| D3 | Bảng phí vắng là chính sách giá riêng của viện, nhóm không tự quyết được. Giữ bảng ví dụ 6.7 cho tới khi có bảng thật. | Chưa rõ — Kế toán, Quản lý viện |
| D4 | Giữ mặc định 2 ca ngày/đêm; mẫu xoay ca cấu hình được nên đổi sang 3 ca không ảnh hưởng spec. | Đồng ý |
| D5 | Thuốc gây nghiện, hướng thần có quy định riêng về sổ theo dõi và kiểm kê (Thông tư 20/2017/TT-BYT). Nếu viện có dùng loại thuốc này, đề xuất bổ sung đếm số lượng còn sau mỗi lần phát và kiểm kê định kỳ; chưa bắt buộc người chứng kiến. Cần dược sĩ hoặc tư vấn pháp lý đối chiếu. | Chưa rõ — dược sĩ, tư vấn pháp lý |
| Q-02 | Ngưỡng thang điểm đánh giá: giữ bảng 5.3. | Chưa rõ — Bác sĩ |
| Q-03 | Mẫu bản đồng ý chia sẻ dữ liệu: theo Nghị định 13/2023/NĐ-CP. | Chưa rõ — tư vấn pháp lý |
| Q-05 | Định dạng file kế toán: Excel/CSV theo mục 23. | Chưa rõ — Kế toán |
| Q-06 | Nhà cung cấp tin nhắn: giai đoạn đầu chỉ thông báo trong ứng dụng. | Chưa rõ — Quản lý viện |

---

## Phần E — Ghi kết quả sau buổi rà

- **Đồng ý:** nhóm ghi "đã xác nhận với người vận hành ngày …" vào mục báo lại của spec liên quan. Với quyết định phần A, ghi thêm vào dòng Q tương ứng ở mục 24.2.
- **Không:** mở một mã Q mới ở mục 24, ghi cách viện thực sự làm, rồi sửa thân tài liệu, các phụ lục, luồng nghiệp vụ và spec theo quy trình chuyển Q ở đầu 24.2.
- **Đồng ý, bổ sung:** xử lý như "Không" (có thay đổi so với tài liệu).
- **Chưa rõ:** ghi tên người cần hỏi và giữ nguyên mặc định cho tới khi có câu trả lời.

---

## Phần F — Tổng hợp đề xuất thay đổi (nhóm phân tích, 2026-09-30)

Trong 100 câu ở phần A, B (A: 18, B: 82): 90 câu đề xuất giữ nguyên, 10 câu đề xuất thay đổi (dòng 11 của bảng dưới được rút sau khi đối chiếu, xem Trạng thái). Phần D: 3 câu giữ mặc định, 6 mục chờ người chuyên môn. Mỗi thay đổi dưới đây sẽ mở một mã Q mới ở mục 24 `docs/nghiep-vu.md` sau khi người vận hành xác nhận (theo phần E).

**Trạng thái (2026-09-30):** người dùng đã duyệt phần F. Mười thay đổi đã được đưa vào `docs/nghiep-vu.md` thành Q-245 → Q-254 (mục 24.2), cùng thân tài liệu, Phụ lục 25, 27, `docs/luong-nghiep-vu.md` và các spec 001, 003, 006, 007, 009, 010, 012, 013, 014, 016, 017, 019. Thay đổi 11 (B15.1) không mở Q: quy tắc hiện hành (Q-193, 18.7) đã chỉ cho Quản lý viện xuất thông tin định danh người cao tuổi, vai trò khác chỉ xuất số liệu tổng hợp; câu hỏi B15.1 tóm tắt chưa đủ. Hai điều chỉnh khi đưa vào: B9.4 không cần đồng ý bổ sung khi số thực tế vẫn không quá CFG-M11-02; B12.2 bỏ điều kiện "có người giám hộ" vì hệ thống chưa có khái niệm này, và tiền mặt, trang sức vẫn theo BR-M12-06. Các thay đổi vẫn chờ người vận hành xác nhận.

**Bổ sung (2026-10-01):** khi rà chéo các spec sau lượt sửa trên (checklist `specs/000-shared-foundation/checklists/cross-feature.md`, CHK059 → CHK101), nhóm phân tích chốt thêm Q-255 → Q-270 để làm rõ chi tiết của mười thay đổi này (cờ nguy cơ đang gắn, tầng hết giường trống, nộp tiền chưa đủ, Điều dưỡng trả đồ, vệ sinh khi dùng lại giường, định nghĩa ca không có Trưởng tầng, căn cứ mua gấp…). Buổi rà với người vận hành cần xác nhận cả Q-245 → Q-270, cùng hai giá trị mặc định 2 ngày (CFG-M07-07) và 10% (CFG-M11-06).

**Chốt giai đoạn Phân tích (2026-10-01):** buổi rà với người vận hành chưa diễn ra. Chủ dự án quyết định chốt Q-245 → Q-270 và chín quyết định còn mở (D1 → D5 tức Q-01, Q-07, Q-08, Q-09, Q-63; cùng Q-02, Q-03, Q-05, Q-06) theo đúng giá trị mặc định, để spec không còn phụ thuộc quyết định mở. Các câu trả lời trong phiếu này vì thế vẫn là đề xuất của nhóm phân tích, **không phải** xác nhận của người vận hành. Danh sách điểm cần đối chiếu trước khi triển khai, kèm người đối chiếu, nằm ở mục 24.4 của `docs/nghiep-vu.md`; phiếu này được giữ lại để dùng cho buổi đối chiếu đó.

| # | Câu | Thay đổi đề xuất | Sửa quyết định, yêu cầu | Spec bị ảnh hưởng | Mã mới |
| --- | --- | --- | --- | --- | --- |
| 1 | A2.1 | Báo hỏng giường đang có người có hai mức: mất an toàn thì báo Khẩn cấp và chuyển giường gấp; không mất an toàn thì giữ cách cũ, thêm "đã sửa tại chỗ" khi người vẫn nằm | Q-232 | 003, 019 | Q-245 |
| 2 | A2.5 | Thêm trạng thái giường "Tạm ngừng sử dụng" (Quản lý viện đặt, gỡ; chỉ khi giường Trống, không có phân bổ tương lai) | Q-237, Q-234; 7.2 | 003, 019, 016 | Q-246 |
| 3 | B2.3 | Thuốc gia đình gửi: thêm lần nhắc thứ hai dưới \[2 ngày\] (tham số mới), gửi thêm Điều dưỡng phụ trách | 006 FR-046 | 006, 009 | Q-247 |
| 4 | B3.2 | Cảnh báo "đã leo thang tối đa" nhắc lại cho người nhận cấp 2 theo chu kỳ thời hạn tiếp nhận tới khi có người tiếp nhận | 007 FR-034; BR-M05-01 | 007, 009 | Q-248 |
| 5 | B6.1 | Thay đổi danh sách đón, quyền: báo mức Nhẹ cho các người đại diện còn lại | 012 FR-016 | 012, 009 | Q-249 |
| 6 | B7.7 | Bác sĩ được bỏ cờ nguy cơ đề xuất, bắt buộc lý do, lịch sử vẫn hiện | Q-13 | 001 | Q-250 |
| 7 | B9.4 | Mua hộ vượt trong \[10%\] (tham số mới) thì mua luôn; vượt hơn phải hỏi người đại diện trước, trừ việc gấp do Quản lý viện duyệt | Q-137 | 010 | Q-251 |
| 8 | B10.5 | Khoản nộp làm số dư trở về bình thường thì báo người đại diện mức Nhẹ | Q-228 | 017, 009 | Q-252 |
| 9 | B12.2 | Người cao tuổi tự nhận lại đồ không cần người đại diện xác nhận, trừ khi có cờ đi lạc; tiền mặt, trang sức vẫn theo BR-M12-06 | Q-149 | 013 | Q-253 |
| 10 | B13.6 | Ca không có Trưởng tầng: Người phụ trách ca kiểm tra chất lượng, loại công việc của chính họ | Q-163, Q-168 | 014 | Q-254 |
| 11 | B15.1 | File có định danh người cao tuổi chỉ Quản lý viện xuất; số liệu tổng hợp giữ quy tắc "xem được thì xuất được" | Q-193 | 016 | Không mở (đã có ở Q-193, 18.7) |

Câu cần người chuyên môn trả lời riêng: D3 (Kế toán, Quản lý viện), D5 (dược sĩ, tư vấn pháp lý), Q-02 (Bác sĩ), Q-03 (tư vấn pháp lý), Q-05 (Kế toán), Q-06 (Quản lý viện). A1.1 và A1.4 dựa trên Luật Cư trú 2020, nên tư vấn pháp lý rà lại cùng lúc với Q-03.
