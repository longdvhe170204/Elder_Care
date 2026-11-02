package com.eldercare.backend.administration.parameter.api.dto;

/**
 * Mã tham số cấu hình: đủ 122 mã của Phụ lục 25 (docs/business/nghiep-vu.md), sinh từ phụ lục (000 T030).
 * Không có lệnh tạo hay xóa mã (FR-011); thêm mã khi tài liệu nghiệp vụ bổ sung.
 */
public enum ParameterCode {
    /** Quá hạn trở lại sau tạm vắng (mặc định: 2 giờ) */
    CFG_M01_01,
    /** Chu kỳ đánh giá lại định kỳ; thời hạn hiệu lực của đánh giá khi tiếp nhận (mặc định: 90 ngày) */
    CFG_M01_02,
    /** Hạn đánh giá lại kể từ lúc bác sĩ được báo (mặc định: 48 giờ) */
    CFG_M01_03,
    /** Khóa tài khoản người thân sau kết thúc lưu trú (mặc định: 30 ngày) */
    CFG_M01_04,
    /** Chu kỳ nhắc khi đã tiếp nhận mà chưa có bản đồng ý Hiệu lực (mặc định: 1 ngày) */
    CFG_M01_06,
    /** Thời hạn giữ chỗ tạm cho hồ sơ chờ (mặc định: 48 giờ) */
    CFG_M02_02,
    /** Nhắc xác nhận hồ sơ chờ không cập nhật (mặc định: 30 ngày) */
    CFG_M02_03,
    /** Nhắc trước khi hợp đồng dài hạn hết hạn (mặc định: 30 ngày) */
    CFG_M02_04,
    /** Bảng chính sách phí khi vắng (mặc định: Theo bảng 6.7) */
    CFG_M02_05,
    /** Thời hạn báo vắng trước của bán trú (mặc định: 24 giờ) */
    CFG_M02_06,
    /** Coi là vắng không báo sau giờ đến dự kiến (mặc định: 2 giờ) */
    CFG_M02_07,
    /** Bảng điểm ưu tiên danh sách chờ (điểm theo ba mức chăm sóc ở mục 4) (mặc định: Mỗi 7 ngày chờ +1 điểm; tình huống đặc biệt theo cơ sở) */
    CFG_M02_08,
    /** Chu kỳ nhắc danh sách việc sau qua đời (mặc định: 1 ngày) */
    CFG_M02_09,
    /** Số ngày hợp đồng quá hạn trước khi báo Quản lý viện (mặc định: 7 ngày) */
    CFG_M02_10,
    /** Thời gian lưu trú dự kiến tối thiểu để đăng ký tạm trú; dưới ngưỡng thì thông báo lưu trú (mặc định: 30 ngày) */
    CFG_M02_11,
    /** Hạn khai báo tạm trú, lưu trú sau Hoàn tất tiếp nhận (mặc định: 1 ngày) */
    CFG_M02_12,
    /** Nhắc gia hạn tạm trú trước ngày hết hạn (mặc định: 30 ngày) */
    CFG_M02_13,
    /** Sức chứa khu nghỉ bán trú mỗi buổi (mặc định: Theo cơ sở) */
    CFG_M03_01,
    /** Chu kỳ nhắc chuyển giường khi phòng không còn phù hợp mức chăm sóc (mặc định: 1 ngày) */
    CFG_M03_02,
    /** Hạn xác nhận giường sẵn sàng (mặc định: 4 giờ) */
    CFG_M03_04,
    /** Khoảng lùi tối đa khi nhập bù phân bổ giường không cần duyệt (mặc định: 24 giờ) */
    CFG_M03_07,
    /** Độ trễ tối đa của việc tự động theo lịch (chuyển giường, bắt đầu/kết thúc phân bổ) trước khi báo Quản lý viện (mặc định: 30 phút) */
    CFG_M03_08,
    /** Nhắc trước hạn bảo trì, kiểm định, đăng kiểm, bảo hiểm tài sản (mặc định: 30 ngày) */
    CFG_M03_09,
    /** Mốc sớm nhất Người phụ trách ca lập công việc của ca (mặc định: 1 giờ trước giờ bắt đầu ca) */
    CFG_M04_01,
    /** Khung thời gian cho phép mặc định của công việc (mặc định: ±15 phút) */
    CFG_M04_02,
    /** Mốc kiểm tra lượng nước / ngưỡng tối thiểu (mặc định: 16:00 / 60% mục tiêu) */
    CFG_M04_03,
    /** Số bữa ăn kém liên tiếp để cảnh báo (mặc định: 3) */
    CFG_M04_04,
    /** Số ngày tâm trạng tiêu cực liên tiếp (mặc định: 3) */
    CFG_M04_05,
    /** Ngưỡng ghi nhận muộn (mặc định: 2 giờ) */
    CFG_M04_06,
    /** Quá giờ về của hoạt động ngoài viện (mặc định: 30 phút) */
    CFG_M04_07,
    /** Số ngày không tham gia hoạt động nhóm để cảnh báo cô lập (mặc định: 7 ngày) */
    CFG_M04_10,
    /** Tỷ lệ tối thiểu công việc người kiểm tra chọn để kiểm tra chất lượng (mặc định: 5% mỗi ca) */
    CFG_M04_11,
    /** Khoảng chờ từ lúc công việc Thường Quá hạn tới khi báo người phụ trách ca (mặc định: 30 phút) */
    CFG_M04_12,
    /** Thời gian sớm nhất được điểm danh rời viện trước giờ rời dự kiến của chuyến đi (mặc định: 1 giờ) */
    CFG_M04_13,
    /** Thời hạn tiếp nhận cảnh báo mức trung bình (mặc định: 15 phút) */
    CFG_M05_01,
    /** Số lần lặp cảnh báo trung bình để báo lặp lại (mặc định: 3 lần / 24 giờ) */
    CFG_M05_02,
    /** Ngưỡng sụt cân (mặc định: 5% trong 30 ngày) */
    CFG_M05_03,
    /** Số lần từ chối cùng một thuốc liên tiếp (mặc định: 2) */
    CFG_M05_04,
    /** Số đêm mất ngủ liên tiếp (mặc định: 3) */
    CFG_M05_05,
    /** Theo dõi sau ngã (mặc định: Mỗi 4 giờ trong 72 giờ) */
    CFG_M05_06,
    /** Khoảng thời gian truy vết tiếp xúc (người cùng phòng, hoạt động chung, nhân viên phân công, người thân thăm; spec 007) (mặc định: 5 ngày) */
    CFG_M05_07,
    /** Theo dõi người tiếp xúc (mặc định: Đo nhiệt độ 2 lần/ngày trong 7 ngày) */
    CFG_M05_08,
    /** Chu kỳ ứng dụng gửi lại vị trí khi cảnh báo từ nút SOS, nút hỗ trợ khẩn cấp chưa có người tới nơi (mặc định: 30 giây) */
    CFG_M05_09,
    /** Nhắc thiết lập ngưỡng cá nhân (mặc định: 7 ngày sau khi lưu trú) */
    CFG_M06_01,
    /** Cảnh báo trước khi giấy phép cơ sở/người hành nghề hết hạn (mọi giấy phép hành nghề) (mặc định: 60 ngày) */
    CFG_M06_02,
    /** Khoảng giá trị hợp lệ vật lý của từng chỉ số (mặc định: Theo danh mục chỉ số) */
    CFG_M06_03,
    /** Thời gian chờ đo lại giá trị ở mức nguy hiểm; hết hạn thì bản ghi được xác nhận với nhãn "chưa đo lại" (mặc định: 10 phút) */
    CFG_M06_04,
    /** Cửa sổ thời gian mặc định của liều (mặc định: ±30 phút) */
    CFG_M07_01,
    /** Thời gian từ Trễ chuyển sang Bỏ lỡ (mặc định: 30 phút) */
    CFG_M07_02,
    /** Nhắc trước khi đơn thuốc hết hạn (mặc định: 3 ngày) */
    CFG_M07_04,
    /** Hạn hoàn thành đối chiếu thuốc (mặc định: 4 giờ) */
    CFG_M07_05,
    /** Bỏ theo Q-393, giữ mã (Phụ lục 25): nhắc gia đình khi thuốc gửi sắp hết */
    CFG_M07_06,
    /** Bỏ theo Q-393, giữ mã (Phụ lục 25): mốc nhắc lần hai khi thuốc gia đình gửi sắp hết */
    CFG_M07_07,
    /** Mốc Nhân viên bếp được chốt suất ăn (mặc định: 2 giờ trước bữa) */
    CFG_M08_01,
    /** Số ngày lặp món liên tiếp để cảnh báo (mặc định: 3 ngày) */
    CFG_M08_03,
    /** Giờ bữa dự kiến / ngưỡng giao trễ (mặc định: Theo cơ sở / 30 phút) */
    CFG_M08_04,
    /** Nhắc xác nhận phát sinh trước bữa (mặc định: 30 phút) */
    CFG_M08_05,
    /** Thời gian giữ mẫu thức ăn (mặc định: 24 giờ) */
    CFG_M08_06,
    /** Nhắc công bố thực đơn tuần kế tiếp (mặc định: 2 ngày trước ngày đầu tuần) */
    CFG_M08_07,
    /** Bỏ theo Q-392, giữ mã (Phụ lục 25): nhắc lô nguyên liệu sắp hết hạn dùng */
    CFG_M08_08,
    /** Bỏ theo Q-392, giữ mã (Phụ lục 25): chu kỳ kiểm kê kho nguyên liệu */
    CFG_M08_09,
    /** Ngưỡng tỷ lệ phục vụ: trọng số tối đa trên mỗi nhân viên, theo mẫu ca (Q-79, Q-205; trọng số ở CFG-M09-12) (mặc định: Theo cơ sở) */
    CFG_M09_01,
    /** Số giờ làm liên tục tối đa trước khi cảnh báo (mặc định: 16 giờ) */
    CFG_M09_02,
    /** Cảnh báo trước khi chứng chỉ đào tạo nhân viên hết hạn (giấy phép hành nghề dùng CFG-M06-02) (mặc định: 60 ngày) */
    CFG_M09_03,
    /** Mốc lập bàn giao trước khi kết ca (mặc định: 30 phút) */
    CFG_M09_04,
    /** Nhắc khi ca sau chưa xác nhận bàn giao (mặc định: 30 phút từ đầu ca) */
    CFG_M09_05,
    /** Nhắc lập lịch ca tháng trước đầu tháng (mặc định: 15 ngày) */
    CFG_M09_06,
    /** Yêu cầu phủ ca tối thiểu theo vai trò (mẫu ca, phạm vi, vai trò, số người). Dòng mặc định áp mọi tầng/khu vực, mọi mẫu ca; ca toàn viện không có dòng mặc định; chỉ tính nhân viên có giấy phép, đào tạo còn hiệu lực (Q-188). Mẫu xoay ca là danh mục riêng (13.2), không thuộc tham số này (mặc định: ≥ 1 điều dưỡng / tầng / ca) */
    CFG_M09_07,
    /** Báo Quản lý viện khi ca dưới ngưỡng phục vụ sắp bắt đầu (mặc định: 24 giờ) */
    CFG_M09_08,
    /** Khoảng tối đa tìm ca sau để nhận bàn giao (mặc định: 24 giờ) */
    CFG_M09_09,
    /** Nhắc công bố lịch ca tháng trước ngày đầu tháng (mặc định: 3 ngày) */
    CFG_M09_10,
    /** Nhắc yêu cầu đổi ca, nghỉ đột xuất chưa quyết định trước giờ bắt đầu ca (mặc định: 24 giờ) */
    CFG_M09_11,
    /** Trọng số chăm sóc theo từng mức chăm sóc (Q-205; ba mức ở mục 4; nhu cầu chăm sóc không có trọng số) (mặc định: Theo cơ sở (ví dụ cơ bản = 1, đặc biệt = 2,5)) */
    CFG_M09_12,
    /** Số dòng tối đa của một file import danh sách nhân viên (mặc định: 500 dòng) */
    CFG_M09_13,
    /** Hạn xử lý phản hồi (mặc định: Cao 24 giờ / thường 72 giờ) */
    CFG_M10_01,
    /** Tự đóng phản hồi khi người gửi không phản hồi (mặc định: 7 ngày) */
    CFG_M10_02,
    /** Kỳ bản tin (mốc nhắc lập) / hạn duyệt bản tin (mặc định: Thứ 2 hằng tuần / 48 giờ) */
    CFG_M10_03,
    /** Khung giờ thăm và sức chứa mỗi khung (mặc định: 09:00–11:00 và 15:00–17:00; 20 người mỗi khung) */
    CFG_M10_04,
    /** Thời hạn đăng ký thăm trước (mặc định: Tối thiểu 2 giờ, tối đa 14 ngày) */
    CFG_M10_05,
    /** Số người tối đa mỗi lượt thăm (mặc định: 3) */
    CFG_M10_06,
    /** Nhắc ghi giờ ra sau khi hết khung thăm (mặc định: 30 phút) */
    CFG_M10_07,
    /** Hiệu lực của ngoại lệ đón; cũng là hiệu lực của xác nhận người nhận khác đồ gửi (spec 013) (mặc định: 4 giờ) */
    CFG_M10_08,
    /** Thời gian bản ghi đón chưa dùng còn làm căn cứ (mặc định: 2 giờ) */
    CFG_M10_09,
    /** Số lượt người thân ở lại tối đa cùng lúc cho một người cao tuổi (mặc định: 1) */
    CFG_M10_10,
    /** Nhắc / báo Quản lý viện khi yêu cầu BR-M10-07 Chờ xác nhận lâu (mặc định: 48 giờ / 96 giờ) */
    CFG_M10_11,
    /** Mốc Trưởng tầng được duyệt thay bản tin (mặc định: 96 giờ) */
    CFG_M10_12,
    /** Hạn chốt bảng chi phí của kỳ vừa kết thúc (không đổi ranh giới kỳ; Q-132) (mặc định: Ngày cuối của tháng liền sau kỳ) */
    CFG_M11_01,
    /** Hạn mức mua hộ cần đồng ý/duyệt (mặc định: 500.000 đồng) */
    CFG_M11_02,
    /** Biến động tổng chi phí kỳ để cảnh báo (mặc định: 30% so với kỳ trước) */
    CFG_M11_03,
    /** Ngưỡng số ngày còn đủ tiền để báo sắp hết tiền (mặc định: 15 ngày) */
    CFG_M11_04,
    /** Chu kỳ nhắc lại số dư thấp, còn nợ; số ngày còn nợ trước khi báo Quản lý viện (mặc định: 7 ngày) */
    CFG_M11_05,
    /** Mức vượt tối đa so với số tiền mua hộ đã đồng ý mà không cần đồng ý bổ sung (mặc định: 10% (đối chiếu khi triển khai, 24.4)) */
    CFG_M11_06,
    /** Mức vượt tối đa của số tiền mua thực tế so với số được duyệt của yêu cầu mua sắm (mặc định: 10%) */
    CFG_M11_07,
    /** Thời gian yêu cầu mua sắm Đã duyệt chưa ghi đã mua thì nhắc (mặc định: 30 ngày) */
    CFG_M11_08,
    /** Thời gian yêu cầu mua sắm Chờ đối chiếu thì nhắc Kế toán (mặc định: 7 ngày) */
    CFG_M11_09,
    /** Tài khoản thu của viện (ngân hàng, số tài khoản, tên chủ tài khoản) dùng cho sao kê và mã QR nộp tiền (Q-335) (mặc định: Viện nhập khi cài đặt) */
    CFG_M11_10,
    /** Thông tin viện in trên chứng từ, phiếu tóm tắt chuyển viện và hiện ở màn gọi 115 (tên, địa chỉ, mã số thuế, điện thoại, logo) (mặc định: Viện nhập khi cài đặt) */
    CFG_M11_11,
    /** Danh mục đồ có giá trị bắt buộc chụp ảnh (mặc định: Điện thoại, trang sức, tiền mặt) */
    CFG_M12_01,
    /** Chu kỳ nhắc đồ gửi chưa trả sau khi hồ sơ ở trạng thái cuối (mặc định: 7 ngày) */
    CFG_M12_02,
    /** Thời gian tối thiểu từ khi hồ sơ ở trạng thái cuối tới khi được lập đề nghị xử lý đồ không người nhận (mặc định: 90 ngày) */
    CFG_M12_03,
    /** Chu kỳ và hạn kiểm kê đồ có giá trị (mặc định: Hằng tháng, hạn hoàn thành 3 ngày) */
    CFG_M12_04,
    /** Thời gian chờ người đại diện phản hồi trước khi Quản lý viện được duyệt thay xác nhận người nhận khác (mặc định: 4 giờ) */
    CFG_M12_05,
    /** Bằng chứng liên hệ tối thiểu trước khi Quản lý viện duyệt thay (mặc định: Mỗi người đại diện 2 lần qua 2 kênh) */
    CFG_M12_06,
    /** Bằng chứng liên hệ tối thiểu trước khi lập đề nghị xử lý đồ không người nhận (mặc định: 2 lần, cách nhau không dưới CFG-M12-02) */
    CFG_M12_07,
    /** Thời hạn xác nhận thông báo khẩn cấp trước khi gọi điện (mặc định: 5 phút) */
    CFG_M13_01,
    /** Giờ yên tĩnh cho thông báo người thân (mặc định: 21:00–07:00) */
    CFG_M13_02,
    /** Khung gộp tin nhắn mức Trung bình cho cùng người nhận (mặc định: 5 phút) */
    CFG_M13_03,
    /** Hạn ghi kết quả gọi điện trước khi nhắc người kế tiếp (mặc định: 5 phút) */
    CFG_M13_04,
    /** Số lần tự gửi lại khi kênh ngoài ứng dụng lỗi (mặc định: 1 lần) */
    CFG_M13_05,
    /** Giờ hành chính cho vai trò không xếp ca (Quản lý viện, Hành chính, Kế toán; Q-332) (mặc định: 07:30–17:00, thứ 2 → thứ 7) */
    CFG_M13_06,
    /** Độ dài tối đa của khoảng thời gian một lần xem, xuất báo cáo (mặc định: 12 tháng) */
    CFG_M14_01,
    /** Khóa tạm tài khoản khi đăng nhập sai (mặc định: 5 lần / khóa 15 phút) */
    CFG_M15_01,
    /** Tài khoản người thân không hoạt động (mặc định: 180 ngày) */
    CFG_M15_02,
    /** Thời hạn lưu giữ hồ sơ người cao tuổi, sức khỏe, thuốc, sự cố (mặc định: 10 năm sau khi kết thúc lưu trú) */
    CFG_M15_03,
    /** Thời hạn lưu giữ nhật ký hệ thống (mặc định: 10 năm) */
    CFG_M15_04,
    /** Nhắc người duyệt khi yêu cầu ở Chờ duyệt quá lâu (mặc định: 48 giờ) */
    CFG_M15_05,
    /** Báo Quản lý viện khi yêu cầu vẫn chưa được duyệt (lớn hơn CFG-M15-05) (mặc định: 96 giờ) */
    CFG_M15_06,
    /** Khoảng phạm vi dữ liệu trước và sau giờ ca (mặc định: 2 giờ) */
    CFG_M15_07,
    /** Thời gian ngoại tuyến tối đa được tự động chấp nhận (mặc định: 24 giờ) */
    CFG_M15_08,
    /** Độ mạnh tối thiểu của mật khẩu (mặc định: Tối thiểu 8 ký tự, có cả chữ và số) */
    CFG_M15_09,
    /** Thời gian không thao tác thì tự đăng xuất, theo kênh (mặc định: Web 30 phút / ứng dụng nhân viên 12 giờ / ứng dụng người thân 30 ngày / ứng dụng người cao tuổi không tự đăng xuất (BR-M15-24)) */
    CFG_M15_10,
    /** Hiệu lực mã QR đăng nhập của người cao tuổi (mặc định: 5 phút) */
    CFG_M15_11;

    /** Mã dạng {@code CFG-M15-05}. */
    public String code() {
        return name().replace('_', '-');
    }

    public static ParameterCode of(String code) {
        return valueOf(code.replace('-', '_'));
    }
}
