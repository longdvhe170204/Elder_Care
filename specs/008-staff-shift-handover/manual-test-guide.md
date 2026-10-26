# Hướng dẫn tự kiểm chức năng 008 trên web

Ngày 2026-10-26. Dùng cho nhánh đang làm của E sau khi đồng bộ develop và sửa lỗi biểu tượng BookOpen. Đây là hướng dẫn thao tác và kết quả mong đợi, **chưa phải biên bản đã chạy đạt**. Không tự đánh dấu nghiệm thu từ danh sách này.

Nguồn: [quickstart](quickstart.md), [tasks](tasks.md), [validation](validation.md), [hợp đồng HTTP](contracts/api-008.yaml), [giao tiếp module](contracts/module-interfaces.md), [nghiệp vụ chung](../../docs/business/nghiep-vu.md). Hợp đồng hiện có 49 đường dẫn / 62 thao tác; kiểm giao diện không thay thế việc kiểm hợp đồng, SPI, transaction và trigger.

## 1. Trước khi bắt đầu

1. Giữ backend profile demo ở cổng 8080 và frontend ở cổng 5173 đang chạy.
2. Mở http://localhost:5173/login; tài khoản quản lý mẫu là `demo.quanly`, mật khẩu `Demo2026!`.
3. Dùng database `eldercare_demo`. Không chạy bộ IT trong lúc thử web: lớp cơ sở IT tạo lại tài khoản MySQL eldercare_app/eldercare_retention, có thể làm phiên demo mất kết nối hoặc mất quyền database.
4. Với mỗi trường hợp, ghi: ngày/giờ, tài khoản, ID hồ sơ/ca/bàn giao, thao tác, kết quả thực tế, và một trong bốn trạng thái **Đạt / Lỗi / Chờ dữ liệu hoặc module / Chưa chạy**. Chỉ ghi Đạt sau khi đã thao tác và đối chiếu.
5. Nếu có lỗi: ghi nguyên thông báo, giữ ID và bước tái hiện. F12 → Network → Fetch/XHR → chọn request lỗi, lấy Status và Response. Không gửi mật khẩu, Cookie, token hoặc toàn bộ request đăng nhập.
6. Sau thao tác lưu thành công, đóng chi tiết rồi mở lại hoặc F5 để kiểm dữ liệu đã lưu thật.

Thử trường hợp thành công trước, trường hợp cố tình sai sau. Để các lệnh nghỉ việc, thu hồi, kết thúc và hủy ở cuối lượt dùng dữ liệu đó; hoặc dùng hồ sơ/ca riêng.

## 2. Chuẩn bị dữ liệu theo thứ tự

Các tên dưới đây là nhãn kiểm thử nhân viên. Không gán mặc định ID 1/9001; ghi lại ID thực tế sau khi tạo.

| Nhãn                    | Chức danh nhập khi tạo hồ sơ | Mục đích                                                       |
| ----------------------- | ---------------------------- | -------------------------------------------------------------- |
| Nhân viên thử TT        | Trưởng tầng                  | Giao tầng; kiểm phạm vi của Trưởng tầng                        |
| Nhân viên thử DD1       | Điều dưỡng                   | Có giấy phép phù hợp còn hiệu lực; phụ trách ca đầu            |
| Nhân viên thử DD2       | Điều dưỡng                   | Có giấy phép phù hợp còn hiệu lực; phụ trách ca sau / thay thế |
| Nhân viên thử CS1       | Nhân viên chăm sóc           | Xếp ca, phân công chính, bổ sung/vắng                          |
| Nhân viên thử CS2       | Nhân viên chăm sóc           | Phân công hỗ trợ, thử chồng và sao chép                        |
| Nhân viên thử nghỉ việc | Nhân viên chăm sóc           | Thử nghỉ/nhận lại trước khi gắn tài khoản hoặc xếp ca          |

- Tạo hồ sơ ở **Nhân sự → Nhân viên → Thêm nhân viên**. Tạo hồ sơ không tự tạo tài khoản.
- Thêm giấy phép cần thiết ở chi tiết hồ sơ → **Giấy phép và đào tạo**. Ngày hiệu lực phải phủ toàn bộ ca kiểm thử; không chỉ còn hạn tại thời điểm bấm lưu.
- Khi kiểm Bác sĩ trực, chuẩn bị thêm hai hồ sơ Bác sĩ, tài khoản và giấy phép phù hợp; không dùng chức danh Điều dưỡng thay cho Bác sĩ.
- Vào **Tài khoản, tham số** để tạo tài khoản gắn đúng ID hồ sơ và vai trò tương ứng. Chức danh tự do trong hồ sơ không tự cấp quyền đăng nhập. Với tài khoản được buộc đổi mật khẩu, hoàn thành đổi mật khẩu lần đầu trước khi thử chức năng.
- Với số ID hồ sơ cần nhập khi tạo tài khoản, có thể xem Response của `GET /api/v1/staff` trong Network. Hoặc chạy câu SELECT chỉ đọc dưới đây trong Workbench.

```sql
SELECT id, full_name, position, account_id, status
FROM eldercare_demo.staff_profile
ORDER BY id;
```

Liên kết tài khoản trên web được đọc qua AccountApi.byStaff của 002; cột account_id cục bộ có thể trống dù tài khoản đã được tạo. Câu SQL trên dùng để lấy ID hồ sơ, không dùng cột account_id để kết luận chưa có tài khoản và không UPDATE cột đó bằng tay.

- Ở **Phòng, giường → Cơ cấu** (phần C), cần khu vực và ít nhất hai tầng, cùng phòng/giường nếu thử phân công theo phòng hoặc bàn giao. Ghi lại ID số; mã hiển thị như T1 không thay cho ID số.
- Muốn thử theo người cao tuổi, tỷ lệ chăm sóc và đầy đủ nguồn bàn giao: cần dữ liệu thật từ các module của B/C/D, gồm hồ sơ, tình trạng lưu trú, phân bổ giường, mức chăm sóc, công việc/liều/cảnh báo. Thiếu dữ liệu này thì ghi Chờ, không tự coi danh sách rỗng là đã kiểm thành công.
- Chín tài khoản `demo.*` có sẵn không chứng minh chín hồ sơ nhân viên tương ứng đã được nạp. Dùng hồ sơ/tài khoản thực tế vừa kiểm ở trên khi xếp ca và đổi vai trò.
- Khi kiểm quyền, đăng xuất rồi đăng nhập bằng tài khoản cần thử. Có thể dùng Edge thường cho quản lý và một cửa sổ InPrivate cho nhân viên; các tab trong cùng phiên trình duyệt dùng chung đăng nhập.

## 3. Hồ sơ nhân viên và bộ phận

Vào **Nhân sự → Nhân viên**, đăng nhập Quản lý viện.

| Mã   | Thao tác                                                                                                  | Kết quả mong đợi                                                                                                                                               |
| ---- | --------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| NV01 | Thêm hồ sơ với họ tên, chức danh và bộ phận hợp lệ; mở lại sau F5                                         | Có một hồ sơ Đang làm việc, thông tin được lưu; chưa tự có tài khoản                                                                                           |
| NV02 | Để trống họ tên hoặc chức danh rồi lưu                                                                    | Form hoặc backend báo bắt buộc; không sinh hồ sơ mới                                                                                                           |
| NV03 | Tìm theo họ tên/chức danh; đổi bộ lọc trạng thái                                                          | Danh sách hiển thị đúng điều kiện, mở đúng hồ sơ                                                                                                               |
| NV04 | Sửa họ tên/chuyên môn/liên hệ, lưu và F5                                                                  | Thông tin mới giữ lại; lịch sử nhật ký phản ánh lệnh sửa                                                                                                       |
| NV05 | Chọn Đổi bộ phận; thử thiếu lý do, sau đó nhập lý do                                                      | Thiếu lý do không thành công; lần hợp lệ chuyển đúng bộ phận qua lệnh, không sửa lén trong form hồ sơ                                                          |
| NV06 | Với hồ sơ thử nghỉ việc chưa có tài khoản/ca: Cho nghỉ việc, thời điểm không sớm hơn hiện tại và có lý do | Tại thời điểm có hiệu lực chuyển Nghỉ việc, vẫn còn hồ sơ/lịch sử                                                                                              |
| NV07 | Nhận lại hồ sơ đã nghỉ; thử thiếu rồi đủ lý do                                                            | Thiếu lý do bị chặn; đủ lý do chuyển Đang làm việc. Nếu trước đó có tài khoản bị khóa, không tự mở lại tài khoản                                               |
| BP01 | Vào tab Bộ phận, kiểm danh mục nạp ban đầu                                                                | Có Y tế, Chăm sóc, Bếp, Hành chính – Kế toán theo migration                                                                                                    |
| BP02 | Thêm một bộ phận tên mới; thử tên đang dùng                                                               | Tên mới được lưu; tên trùng đang hoạt động bị từ chối                                                                                                          |
| BP03 | Chỉ định phụ trách: chọn nhân viên đang làm, thuộc bộ phận và đã có tài khoản; nhập lý do                 | Lưu đúng người; không cho chọn người không đáp ứng điều kiện                                                                                                   |
| BP04 | Với người đang phụ trách, thử đổi bộ phận/nghỉ việc mà thiếu người thay rồi thử có người thay hợp lệ      | Thiếu người thay bị chặn và dữ liệu cũ giữ nguyên; lần hợp lệ thay trong cùng lệnh. Nếu thiếu nguồn công việc cho người có tài khoản/ca thì ghi Chờ tích hợp C |

Luồng NV06/NV07 đầy đủ cho người đã có tài khoản/ca còn phải đối chiếu khóa tài khoản, rút ca, kết thúc phân công và chuyển công việc của 005. Không suy kết quả đó từ một hồ sơ trống.

Cách bấm cụ thể khi chỉ định/thay phụ trách: tab Bộ phận → Chỉ định phụ trách [tên bộ phận] → kéo xuống form Người phụ trách → chọn nhân viên → nhập Lý do → Lưu bộ phận. Muốn thay người dùng lại cùng nút và chọn người mới. Chuẩn bị hai nhân viên cùng bộ phận, Đang làm việc và tài khoản Hoạt động để thử hai lượt; bộ phận Hành chính – Kế toán yêu cầu người phụ trách có vai trò Hành chính. Bộ kiểm backend giữ các điều kiện vai trò, không chọn Quản lý viện hoặc Kế toán làm phụ trách.

## 4. Loại chứng chỉ, giấy phép và đào tạo

Quản lý viện mở tab **Loại chứng chỉ** hoặc mở một hồ sơ nhân viên → **Giấy phép và đào tạo**.

| Mã   | Thao tác                                                                       | Kết quả mong đợi                                                                                                       |
| ---- | ------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------- |
| CC01 | Xem danh mục ban đầu                                                           | Có loại giấy phép bác sĩ, giấy phép điều dưỡng và đào tạo sơ cứu theo cấu hình đã duyệt                                |
| CC02 | Thêm loại đào tạo kiểm thử; kiểm cờ Có thời hạn/Có phạm vi và vai trò bắt buộc | Lưu đúng cấu hình. Lượt đầu không gán bắt buộc hàng loạt để tránh thay điều kiện của những ca khác                     |
| CC03 | Thêm chứng chỉ có ngày cấp/hết hạn hợp lệ; mở lại                              | Hiện đúng loại, số, đơn vị, phạm vi, ngày và tình trạng hiệu lực                                                       |
| CC04 | Thử thiếu ngày bắt buộc hoặc ngày hết hạn không hợp lệ                         | Bị chặn; không sinh chứng chỉ nửa chừng                                                                                |
| CC05 | Gia hạn chứng chỉ với thời hạn mới                                             | Có bản mới liên kết bản cũ; bản cũ Đã thay thế, vẫn xem được lịch sử                                                   |
| CC06 | Thu hồi chứng chỉ với ngày hiệu lực và lý do; thử bỏ lý do                     | Thiếu lý do bị chặn; tới mốc bản đó Đã thu hồi. Tác động đến phân công cần kiểm thêm PC04 và nguồn công việc           |
| CC07 | Sửa sai số/đơn vị/ngày và nhập lý do                                           | Dữ liệu được sửa theo lệnh có nhật ký trước/sau; thiếu lý do không thành công                                          |
| CC08 | Đính kèm tệp scan, lưu rồi Xem bản scan                                        | Tệp thật mở được với người có quyền; nhân viên khác không đọc được tệp chỉ vì biết ID. Cần dịch vụ tệp của B chạy thật |

Nếu danh mục phạm vi hành nghề rỗng, chưa thể kết luận giấy phép cần phạm vi đã kiểm đủ. 008 hiện chỉ đọc danh mục này qua hợp đồng; cần chủ danh mục cung cấp dữ liệu.

## 5. Import Excel

Vào **Nhân sự → Import Excel**, dùng Quản lý viện. Bấm **Tải file mẫu** và giữ nguyên cấu trúc file `.xlsx`. Bốn cột đầu bắt buộc: Họ tên, Chức danh, Số điện thoại, Ngày bắt đầu làm việc; Chuyên môn và Email là hai cột sau.

| Mã   | Thao tác                                                                    | Kết quả mong đợi                                                                   |
| ---- | --------------------------------------------------------------------------- | ---------------------------------------------------------------------------------- |
| IM01 | Điền hai dòng hợp lệ với số điện thoại khác nhau, bấm Xem trước             | Hiện hai dòng, không lỗi, cho phép xác nhận; chưa có hai hồ sơ mới trong danh sách |
| IM02 | Xác nhận cùng file; sau đó xem danh sách nhân viên                          | Tạo đủ hai hồ sơ, nguồn IMPORT; không tự tạo tài khoản hoặc gán bộ phận            |
| IM03 | Một file có một dòng đúng và một dòng thiếu chức danh/sai điện thoại        | Báo đúng dòng/cột; không xác nhận được, không nhập riêng dòng đúng                 |
| IM04 | Thử hai dòng trùng số hoặc trùng số với hồ sơ đã có, kể cả hồ sơ Nghỉ việc  | Báo trùng tương ứng; không nhân đôi hồ sơ                                          |
| IM05 | Dùng file sai loại, thiếu/đổi cột hoặc vượt giới hạn dòng CFG-M09-13        | Báo sai mẫu/loại hoặc quá giới hạn; giữ nguyên dữ liệu đã có                       |
| IM06 | Xem trước hợp lệ, tạo hồ sơ khác dùng số của một dòng, rồi xác nhận file cũ | Kiểm lại tại lúc xác nhận; báo trùng và không nhập một phần                        |

Kiểm 500 dòng/10 giây trong quickstart chỉ áp khi giới hạn CFG hiện hành cho phép; ghi thời gian đo thực tế, không lấy thời gian test đơn vị làm số đo.

## 6. Giao Trưởng tầng và chuẩn bị lịch

Vào **Lịch ca → Giao Trưởng tầng**, dùng Quản lý viện. Cần tài khoản Trưởng tầng đang hoạt động, gắn hồ sơ thực tế và tầng thật của C.

| Mã   | Thao tác                                                                       | Kết quả mong đợi                                                              |
| ---- | ------------------------------------------------------------------------------ | ----------------------------------------------------------------------------- |
| TT01 | Giao người TT cho tầng thứ nhất trong khoảng ngày kiểm thử                     | Lưu được; tài khoản TT thấy phạm vi tầng được giao                            |
| TT02 | Giao thêm người khác cùng tầng với khoảng chồng                                | Bị chặn, không sinh hai người giữ nhiệm vụ tại cùng mốc                       |
| TT03 | Kết thúc/rút ngắn lần giao, có lý do; giao người thay trong khoảng không chồng | Lịch sử còn nguyên, phạm vi đổi đúng mốc; người TT không tự thấy tầng thứ hai |

## 7. Mẫu ca, lịch Nháp và công bố

Vào **Lịch ca**, dùng Quản lý viện trước rồi lặp lại phần thuộc phạm vi với Trưởng tầng.

- Tạo ít nhất hai ca nối tiếp cùng tầng: ca A do DD1 phụ trách, ca B do DD2 phụ trách. Để thử sửa/hủy/chồng mà không phá chuỗi bàn giao, tạo ca/lịch kiểm riêng.
- Lần đầu chọn ngày tương lai để chuẩn bị Nháp. Khi kiểm theo giờ, có thể dùng giờ chỉnh riêng của ca gần thời điểm hiện tại hoặc đồng hồ demo ở mục 13.
- Ghi lại ID lịch, ca A, ca B. Ghi rõ giờ Việt Nam; giờ kết thúc phải sau giờ bắt đầu.

| Mã   | Thao tác                                                                                                    | Kết quả mong đợi                                                                                                                                 |
| ---- | ----------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| LC01 | Mẫu ca → Thêm mẫu ca: tạo mẫu ban ngày rồi mẫu 18:00–07:00                                                  | Lưu được; mẫu đêm kết thúc hôm sau                                                                                                               |
| LC02 | Tạo mẫu có giờ đầu bằng giờ cuối; sửa một mẫu đã được dùng                                                  | Giờ đầu bằng giờ cuối biểu diễn ca 24 giờ. Sửa mẫu không viết lại ca đã tạo; ca có thời điểm đầu/cuối bằng nhau thật sự hoặc vượt 24 giờ bị chặn |
| LC03 | Lập lịch tháng cho tầng, thử lập lại cùng tháng/phạm vi                                                     | Lịch đầu Nháp; không nhân đôi cùng tháng/phạm vi                                                                                                 |
| LC04 | Thêm ca từ mẫu, chọn thành viên và phụ trách; sửa giờ/thành viên, rồi xóa một ca Nháp riêng                 | Lưu/sửa/xóa Nháp đúng; F5 vẫn khớp dữ liệu                                                                                                       |
| LC05 | Xếp cùng một nhân viên vào hai ca chồng giờ                                                                 | Báo chồng ca, không lưu ca/membership bị lỗi                                                                                                     |
| LC06 | Xếp chuỗi ca liên tục vượt ngưỡng hiện hành                                                                 | Hiện cảnh báo/điều kiện đúng CFG; trường hợp được xác nhận cần đánh dấu đã xem và nhập lý do                                                     |
| LC07 | Xem trước công bố khi thiếu người phụ trách hoặc người không đủ điều kiện                                   | Hiện đủ điều kiện chặn; chưa chuyển lịch sang Đã công bố                                                                                         |
| LC08 | Sửa các điều kiện chặn, kiểm cảnh báo rồi Công bố                                                           | Lịch Đã công bố, phân công chuyển phù hợp thời điểm; nhân viên nhận thông báo. Tỷ lệ thấp là cảnh báo, không tự biến thành điều kiện chặn        |
| LC09 | Sau công bố thử sửa/xóa ca bằng thao tác Nháp                                                               | Không cho sửa/xóa theo luồng Nháp; dùng các lệnh điều chỉnh ở mục 9                                                                              |
| LC10 | Đăng nhập DD1/CS1 → Lịch ca; đối chiếu ca A/B                                                               | Thấy ca/phân công của chính mình theo quyền; ca Nháp không cấp phạm vi dữ liệu thật                                                              |
| LC11 | Tạo mẫu/lịch toàn viện theo quyền Quản lý, xếp Bác sĩ và đánh dấu Bác sĩ trực; thử ca 24 giờ không bàn giao | Vai trò/phạm vi đúng luật; lịch toàn viện do Quản lý quản lý, không ép một ca không yêu cầu bàn giao phải lập bàn giao                           |

Nếu xem trước báo chưa có dữ liệu tỷ lệ hoặc nguồn mở rộng, ghi đúng thông báo và nguồn còn thiếu. Không thêm fake vào ứng dụng để làm bước công bố có vẻ đủ dữ liệu.

## 8. Phân công trong ca

Từ **Lịch ca**, chọn một ca → **Phân công**. Trước khi lưu bấm **Kiểm tra phân công**. Muốn kiểm đầy đủ theo người cao tuổi cần vị trí/lưu trú từ B/C; kiểm điều kiện loại công việc cần nguồn 005 và danh mục chuyên môn 002.

| Mã   | Thao tác                                                                                                 | Kết quả mong đợi                                                                                                                                   |
| ---- | -------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- |
| PC01 | Phân CS1 chính theo phòng/tầng hoặc người cao tuổi thật, trong khoảng ca                                 | Xem trước rồi lưu được; danh sách hiện đúng đối tượng, vai trò, thời gian                                                                          |
| PC02 | Phân thêm CS2 chính cho cùng đối tượng và khoảng chồng; sau đó thử dạng Hỗ trợ                           | Hai phân công chính cùng đối tượng chồng bị chặn; trường hợp hỗ trợ hợp lệ được xét theo luật riêng                                                |
| PC03 | Phân chính theo phòng và riêng một người trong phòng cho hai nhân viên                                   | Kết quả nhân viên chính của người được phân riêng ưu tiên đúng; những người còn lại theo phòng                                                     |
| PC04 | Phân Điều dưỡng phụ trách thiếu giấy phép, hết hạn hoặc không phủ hết ca; sau đó dùng người đủ giấy phép | Trường hợp thiếu điều kiện bị chặn và nêu lý do; trường hợp đủ điều kiện lưu được                                                                  |
| PC05 | Thử loại công việc cần chứng chỉ mà người được phân không đủ                                             | Cảnh báo hiện ở bước kiểm trước khi lưu; xử lý xác nhận theo hợp đồng. Thiếu API yêu cầu công việc thì ghi Chờ C                                   |
| PC06 | Xóa phân công Nháp; hủy phân công Tương lai; kết thúc phân công Hiệu lực có lý do                        | Đúng lệnh theo trạng thái. Kết thúc cắt phạm vi đúng mốc; việc/liều được xử lý qua nguồn thật tương ứng, thiếu nguồn thì chưa nghiệm thu luồng này |
| PC07 | Mở ca đích cùng phạm vi → Sao chép phân công, nhập ID ca nguồn                                           | Chép các dòng còn hợp lệ; dòng không hợp lệ trả rõ lý do, không chép âm thầm                                                                       |
| PC08 | Đăng nhập nhân viên sau khi phân công hoạt động rồi sau khi kết thúc                                     | Chỉ đọc được phạm vi đang có hiệu lực; lịch sử trước mốc kết thúc vẫn đúng khi kiểm bằng API/test lịch sử                                          |

## 9. Điều chỉnh ca đã công bố

Mở **Chi tiết ca → Điều chỉnh ca**. Dùng đúng tài khoản có quyền; nút hiện theo allowedCommands của backend.

| Mã   | Thao tác                                                                              | Kết quả mong đợi                                                                                                                  |
| ---- | ------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| DC01 | Bổ sung nhân viên vào ca đã công bố, nhập lý do                                       | Có thành viên mới, kiểm lại chồng/điều kiện, thông báo thay đổi đúng người                                                        |
| DC02 | Chuyển người phụ trách sang DD2 trong ca, nhập lý do                                  | Nhiệm vụ và phạm vi đổi đúng thời điểm; nếu có bàn giao đang dở thì chuyển người bàn giao, giữ tác giả ghi chú cũ                 |
| DC03 | Trưởng tầng hoặc phụ trách đúng ca ghi nhận một thành viên vắng trong cửa sổ cho phép | Có dấu vắng, cắt phân công đúng mốc, tính lại tỷ lệ và chuyển công việc nguyên tử. Cần nguồn onAbsence của 005 để kiểm trọn luồng |
| DC04 | Thử ghi vắng ca ngoài cửa sổ; thử hủy ca tương lai và hủy ca đã bắt đầu               | Ngoài cửa sổ bị chặn; chỉ hủy ca đã công bố chưa bắt đầu với đúng quyền/lý do                                                     |
| DC05 | Quản lý mở Lịch sử căn cứ nhiệm vụ sau các lệnh dùng quyền theo nhiệm vụ              | Đọc được căn cứ/tầng/mốc đúng; không đòi mọi lệnh bằng vai trò quản lý phải có một dòng nhiệm vụ                                  |

## 10. Tỷ lệ phục vụ và nhắc theo giờ

Mở ca → **Tỷ lệ phục vụ**. Cần đủ hồ sơ/lưu trú, mức chăm sóc, vị trí và lịch đến bán trú nếu có.

| Mã   | Thao tác                                                                                    | Kết quả mong đợi                                                                                                           |
| ---- | ------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| TL01 | Ghi tổng trọng số và số nhân viên được tính từ bộ dữ liệu thật; so với màn                  | Tỷ lệ = tổng trọng số / số nhân viên được tính. Trọng số/ngưỡng theo CFG hiện hành; không tự coi dữ liệu thiếu là 0 hợp lệ |
| TL02 | Dùng ca ít người để dưới ngưỡng, xem trước công bố                                          | Cảnh báo dưới ngưỡng, vẫn công bố nếu không có điều kiện chặn khác                                                         |
| TL03 | Bổ sung/vắng/nghỉ hoặc B/C thay mức chăm sóc, vị trí, tình trạng vắng; mở lại lịch sử tỷ lệ | Có lần tính do đúng sự kiện; báo/nhắc đúng người theo điều kiện, không phát lặp vô hạn. Nguồn chưa có thì ghi Chờ          |
| NH01 | Chứng chỉ đào tạo tới mốc sắp hết hạn rồi gia hạn                                           | Có cảnh báo/nhắc theo CFG và đóng nguồn cảnh báo bản cũ sau gia hạn; cần phần thông báo 009 thật                           |
| NH02 | Tới đầu ca thiếu Trưởng tầng/phụ trách hoặc tới mốc bàn giao chưa lập/chưa xác nhận         | Đúng nhóm nhận nhắc; hệ thống không tự lập ca, phân công, bàn giao hoặc mục bàn giao                                       |

## 11. Bàn giao ca

Ca A và ca B cùng tầng đã công bố, DD1/DD2 có tài khoản thật và nhiệm vụ đúng mốc. Tới cửa sổ lập bàn giao trước kết ca A theo CFG-M09-04. Chuẩn bị nguồn từ chủ module trước khi thử bỏ mục, mức nghiêm trọng, chuyển công việc/cảnh báo.

| Mã   | Thao tác                                                                                                 | Kết quả mong đợi                                                                                                                                  |
| ---- | -------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| BG01 | DD1 mở Bàn giao ca, chọn A; thử lập sớm, sau đó tới đúng cửa sổ bấm Lập bàn giao                         | Lập sớm bị chặn; đúng mốc tạo một Bản nháp, không tự chép mục nguồn. Cùng ca không có bàn giao thứ hai                                            |
| BG02 | Xem nguồn công việc/liều/chỉ số/cảnh báo/biến động/giường/đồ gửi/chuyến đi theo dữ liệu thật             | Đúng nhóm và phạm vi; nguồn lỗi hiện chưa đọc được, không được hiểu là không có việc                                                              |
| BG03 | Tự chọn nguồn thêm mục; sửa ghi chú/đánh dấu đã xử lý; thêm Vấn đề cần theo dõi gắn người/phòng trong ca | Chỉ ghi các mục người dùng chọn; công việc/liều/cảnh báo/sự cố phải chọn nguồn, ghi tự do dùng Vấn đề cần theo dõi                                |
| BG04 | Thử bỏ mục được phép và mục chỉ số vượt ngưỡng/biến động người cao tuổi                                  | Mục được phép bỏ thì bỏ được; nhóm bắt buộc không bỏ được theo Q-88                                                                               |
| BG05 | Nhập nhận định chung và riêng; hoàn tất khi mục nghiêm trọng thiếu ghi chú, sau đó bổ sung               | Thiếu ghi chú bị chặn; đủ điều kiện chuyển Đã lập. Nhận định chung có nhắc không ghi sức khỏe riêng từng người                                    |
| BG06 | Sau khi Đã lập, chủ nguồn phát sinh bản ghi mới hoặc đóng nguồn cũ; mở lại                               | Có phần phát sinh sau khi lập và trạng thái hiện tại; nội dung lúc chốt giữ nguyên, không tự thêm mục vào bản đã chốt                             |
| BG07 | DD2 hoặc Trưởng tầng đủ quyền gửi ý kiến; DD1/Trưởng tầng phản hồi từng ý kiến và gửi lại                | Chuyển Có ý kiến → Đã lập đúng luồng; người ngoài quyền/phạm vi không làm được                                                                    |
| BG08 | Người phụ trách ca sau trong phạm vi hoặc Trưởng tầng xác nhận                                           | Đã xác nhận; chuyển trách nhiệm công việc/cảnh báo cùng transaction. Thiếu nguồn C/D thì ghi Chờ, không kết luận đạt từ riêng trạng thái bàn giao |
| BG09 | Mở bàn giao đã xác nhận; thử sửa trực tiếp rồi dùng Xem và đính chính, có lý do                          | Không sửa nội dung gốc qua luồng Nháp; đính chính giữ bản gốc, có lịch sử/bản hiệu lực                                                            |
| BG10 | Ca A kết thúc khi bàn giao chưa lập/đang Nháp, rồi hoàn tất/giải quyết theo luồng                        | Trạng thái ca và nhắc đúng BR-M09-07; không tự sinh bàn giao để đóng ca                                                                           |

Các thử nghiệm nguồn biến mất vì đổi CFG, rollback khi nguồn lỗi, ca sau không tồn tại hoặc tranh chấp xác nhận cần bộ IT và kiểm tích hợp với chủ module; không giả lập bằng xóa dữ liệu nguồn trong Workbench.

## 12. Quyền, riêng tư, nhật ký và độ bền

| Mã   | Thao tác                                                                                      | Kết quả mong đợi                                                                                                                             |
| ---- | --------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| QH01 | Lặp xem hồ sơ/chứng chỉ bằng Quản lý, Trưởng tầng và chính/khác nhân viên                     | Liên hệ, số giấy phép/scan và lý do bị che đúng FR-052/053; không chỉ kiểm bằng cách nhìn có/không có nút                                    |
| QH02 | Trưởng tầng được giao tầng thứ nhất thử dữ liệu tầng thứ hai; nhân viên thử ca của người khác | Backend chặn theo phạm vi; người trong ca chỉ đọc được đối tượng được phép                                                                   |
| QH03 | Người ghi vắng, Trưởng tầng đúng tầng và người ngoài phạm vi xem lý do                        | Người đủ quyền xem; người khác không đọc được lý do qua API, thông báo hoặc nhật ký                                                          |
| QH04 | Mở bàn giao bằng Nhân viên chăm sóc có phân công                                              | Chỉ có mục người trong phạm vi và nhận định chung theo quyền; không lộ nhận định riêng ngoài phạm vi                                         |
| KT01 | Mở cùng hồ sơ hoặc ca Nháp ở hai tab: tab 1 sửa, tab 2 lưu bản cũ                             | Báo dữ liệu đã đổi/412 và cho tải lại; không ghi đè âm thầm                                                                                  |
| KT02 | F5, đăng xuất/đăng nhập lại sau các lệnh thành công; quản lý xem Nhật ký                      | Dữ liệu vẫn còn; lệnh có nhật ký/người/lý do thích hợp                                                                                       |
| KT03 | Trên web, F12 → Network → Offline rồi thử lệnh bàn giao                                       | Không báo thành công giả. Đây chỉ là kiểm lỗi mạng trên web; không thay nghiệm thu mobile ngoại tuyến FR-060                                 |
| KT04 | Hai lần lưu/gửi đồng thời hoặc rollback nguồn lỗi                                             | Dùng test tích hợp hiện có để kiểm Idempotency-Key, khóa và hoàn tác. Hai lần bấm UI dùng hai khóa khác nhau không đủ chứng minh idempotency |

Nhật ký/phạm vi/SPI cho module khác, thống kê StaffStatisticsApi, bảo vệ trigger/lưu giữ và chạy bù job còn phải kiểm bằng test tự động và lượt tích hợp A/B/C/D. Không sửa/xóa bảng lịch sử trực tiếp để kiểm giao diện.

Một truy vấn HTTP chưa có màn tra cứu riêng là Bác sĩ trực:

| Mã   | Thao tác                                                                                        | Kết quả mong đợi                                                                                                                      |
| ---- | ----------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| BS01 | Trong ca có hai Bác sĩ trực, đọc GET /api/v1/on-duty-doctors tại mốc trong ca, rồi mốc ngoài ca | Trả đúng các Bác sĩ còn nhiệm vụ tại từng mốc, có thể nhiều người; không có nhiệm vụ thì danh sách rỗng. Cần có dữ liệu thật của LC11 |

Có thể đọc bằng Console cùng phiên đăng nhập; mốc dưới đây lấy từ đồng hồ backend, không giả định giờ máy trùng đồng hồ demo:

```js
const clock008 = await fetch("/api/v1/testing/clock").then((r) => r.json());
const doctors008 = await fetch(
  "/api/v1/on-duty-doctors?at=" + encodeURIComponent(clock008.now),
);
console.log(doctors008.status, await doctors008.json());
```

## 13. Thử thời gian mà không đợi đến ngày mai

Chỉ dùng khi đang chạy profile demo và đã ghi lại các ID/ngày giờ cần kiểm. Đồng hồ này ảnh hưởng chung ứng dụng demo; chọn mốc phù hợp với cả chứng chỉ, nhiệm vụ và ca.

Đăng nhập, F12 → Console. Đọc giờ backend:

```js
fetch("/api/v1/testing/clock")
  .then(async (r) => ({ status: r.status, body: await r.json() }))
  .then(console.log);
```

Đặt mốc kiểm (thay thời điểm minh họa bằng mốc ca bạn vừa tạo, giữ +07:00):

```js
fetch("/api/v1/testing/clock", {
  method: "POST",
  headers: { "Content-Type": "application/json", "X-Requested-From": "web" },
  body: JSON.stringify({ setTo: "2026-10-26T14:35:00+07:00" }),
})
  .then(async (r) => ({ status: r.status, body: await r.json() }))
  .then(console.log);
```

Chọn lần lượt: trước đầu ca, trong ca, trong cửa sổ CFG-M09-04 trước kết ca, sau kết ca, sau mốc nhắc xác nhận. Với cửa sổ 30 phút và ca kết thúc 15:00, 14:35 là ví dụ nằm trong cửa sổ; phải đối chiếu CFG thực tế. Job M09 chạy theo lịch cấu hình, không phải đổi giờ là mọi trạng thái/nhắc lập tức đổi; chờ lượt job, thường tới phút kế tiếp, rồi tải lại. API runJobs hiện chưa liệt kê M09; không gửi tên job M09 tự đặt.

Nếu nhảy giờ làm hết hạn phiên, đăng nhập lại. Muốn đưa đồng hồ về gần giờ máy, gửi cùng request POST với `setTo: new Date().toISOString()`. Không đổi đồng hồ Windows để thử.

## 14. Những phần chưa thể đánh đạt toàn spec

- Chưa đủ nguồn/dữ liệu thật 005/007/013/014/015 và phần liên quan: ghi Chờ ở từng ca kiểm cần nguồn. Một số nguồn 001/003/006 đã có bản thật sau merge; vẫn phải tạo dữ liệu thật và kiểm đúng luồng, không coi cả nhóm nguồn đều chưa có hoặc đều hoàn thành.
- T043b: chủ module chạy lại nghiệp vụ 005–007 với SPI ca/phân công thật của 008; A review hồi quy 002.
- T049b: D kiểm H1–H9 US13 007 với AlertStaffPort thật.
- T044b: demo đầu–cuối; SC-002/SC-010 ít nhất mười lượt thao tác người dùng thật; SC-003 cần số liệu vận hành một tháng. Ghi số đo thật theo mẫu trong validation.md.
- Mobile nhân viên: T016b/T022b/T030/T041b chưa mở; không đánh xong từ việc chạy web hoặc test Flutter phần người cao tuổi.
- Clean verify toàn kho sau merge còn lỗi M07/hợp đồng thuốc đã ghi trong [merge-verification.md](merge-verification.md). 321 test web xanh và mở được màn Nhân sự không thay kết quả đó.

## 15. Lượt thử đầu tiên nên làm ngay

1. Chạy NV01 → NV04 bằng một hồ sơ Nhân viên chăm sóc, F5 để xác nhận lưu thật.
2. Chạy CC03 → CC05 bằng chứng chỉ đào tạo trên chính hồ sơ đó.
3. Chạy IM01 → IM03 bằng file hai dòng.
4. Chuẩn bị tài khoản/vị trí, rồi TT01 và LC01 → LC08.
5. Sau đó đi tiếp PC, DC, TL/NH, BG và QH/KT theo thứ tự trong tài liệu; để nghỉ việc/thu hồi/hủy dữ liệu đang dùng ở cuối lượt.

Mẫu ghi kết quả cho mỗi ca: `Mã ca | Ngày/giờ | Tài khoản | ID dữ liệu | Kết quả thực tế | Đạt/Lỗi/Chờ/Chưa chạy | Thông báo hoặc bằng chứng`. Mọi trường hợp trong tài liệu này ban đầu đều là Chưa chạy.

## 16. Danh mục HTTP để đối chiếu

Danh mục dưới đây lấy từ api-008.yaml hiện tại; tiền tố là /api/v1. Số mục chỉ nhóm hướng dẫn, không phải kết quả đã chạy kiểm. Cần đối chiếu request/response thực tế và bộ ApiContractIT khi nghiệm thu hợp đồng.

| HTTP   | Đường dẫn                                   | Mục hướng dẫn |
| ------ | ------------------------------------------- | ------------- |
| GET    | `/staff`                                    | 3 / 5         |
| POST   | `/staff`                                    | 3 / 5         |
| GET    | `/staff/import/template`                    | 5             |
| POST   | `/staff/import/preview`                     | 5             |
| POST   | `/staff/import/confirm`                     | 5             |
| GET    | `/staff/{id}`                               | 3 / 5         |
| PUT    | `/staff/{id}`                               | 3 / 5         |
| POST   | `/staff/{id}/terminate-employment`          | 3 / 5         |
| POST   | `/staff/{id}/rehire`                        | 3 / 5         |
| POST   | `/staff/{id}/change-department`             | 3 / 5         |
| GET    | `/departments`                              | 3 / 5         |
| POST   | `/departments`                              | 3 / 5         |
| POST   | `/departments/{id}/assign-lead`             | 3 / 5         |
| GET    | `/credential-types`                         | 4             |
| POST   | `/credential-types`                         | 4             |
| GET    | `/practice-scopes`                          | 4             |
| GET    | `/staff/{id}/credentials`                   | 4             |
| POST   | `/staff/{id}/credentials`                   | 4             |
| POST   | `/credentials/{id}/renew`                   | 4             |
| POST   | `/credentials/{id}/revoke`                  | 4             |
| POST   | `/credentials/{id}/correct`                 | 4             |
| GET    | `/shift-templates`                          | 7 / 9 / 13    |
| POST   | `/shift-templates`                          | 7 / 9 / 13    |
| PUT    | `/shift-templates/{id}`                     | 7 / 9 / 13    |
| GET    | `/shift-schedules`                          | 7 / 9 / 13    |
| POST   | `/shift-schedules`                          | 7 / 9 / 13    |
| POST   | `/shift-schedules/{id}/shifts`              | 7 / 9 / 13    |
| GET    | `/shifts/{id}`                              | 7 / 9 / 13    |
| PUT    | `/shifts/{id}`                              | 7 / 9 / 13    |
| DELETE | `/shifts/{id}`                              | 7 / 9 / 13    |
| GET    | `/shift-schedules/{id}/publication-preview` | 7 / 9 / 13    |
| POST   | `/shift-schedules/{id}/publish`             | 7 / 9 / 13    |
| POST   | `/shifts/{id}/add-staff`                    | 7 / 9 / 13    |
| POST   | `/shifts/{id}/change-lead`                  | 7 / 9 / 13    |
| POST   | `/shifts/{id}/record-absence`               | 7 / 9 / 13    |
| POST   | `/shifts/{id}/cancel`                       | 7 / 9 / 13    |
| GET    | `/me/shifts`                                | 7 / 9 / 13    |
| GET    | `/on-duty-doctors`                          | 7 / 12 (BS01) |
| GET    | `/floor-manager-assignments`                | 6 / 8         |
| POST   | `/floor-manager-assignments`                | 6 / 8         |
| POST   | `/floor-manager-assignments/{id}/end`       | 6 / 8         |
| GET    | `/shifts/{id}/assignments`                  | 6 / 8         |
| POST   | `/shifts/{id}/assignments`                  | 6 / 8         |
| POST   | `/shifts/{id}/assignments/preview`          | 6 / 8         |
| DELETE | `/assignments/{id}`                         | 6 / 8         |
| POST   | `/assignments/{id}/cancel`                  | 6 / 8         |
| POST   | `/assignments/{id}/end`                     | 6 / 8         |
| POST   | `/shifts/{id}/copy-assignments`             | 6 / 8         |
| GET    | `/shifts/{id}/staffing-ratios`              | 10            |
| GET    | `/shifts/{id}/handovers`                    | 11            |
| POST   | `/shifts/{id}/handovers`                    | 11            |
| GET    | `/handovers/{id}`                           | 11            |
| GET    | `/handovers/{id}/source-records`            | 11            |
| POST   | `/handovers/{id}/items`                     | 11            |
| PUT    | `/handovers/{id}/items/{itemId}`            | 11            |
| DELETE | `/handovers/{id}/items/{itemId}`            | 11            |
| PUT    | `/handovers/{id}/assessments`               | 11            |
| POST   | `/handovers/{id}/complete`                  | 11            |
| POST   | `/handovers/{id}/confirm`                   | 11            |
| POST   | `/handovers/{id}/comments`                  | 11            |
| POST   | `/handovers/{id}/supplement-and-resubmit`   | 11            |
| GET    | `/duty-basis-audit-logs`                    | 9 (DC05)      |
