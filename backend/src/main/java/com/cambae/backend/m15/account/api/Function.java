package com.cambae.backend.m15.account.api;

/**
 * Chức năng: một dòng của Phụ lục 27 (`docs/business/nghiep-vu.md`), theo đúng thứ tự (002 research A-04; glossary mục 7).
 * {@link #label()} là tên dòng không kèm chú thích, để nạp và kiểm ma trận quyền khớp tài liệu nguồn (002 T007).
 * Dòng mới của Phụ lục 27 thêm một giá trị ở đây.
 */
public enum Function {
    RESIDENT_PROFILE("Hồ sơ người cao tuổi"),
    DATA_SHARING_CONSENT("Bản đồng ý chia sẻ dữ liệu"),
    ALLERGY_CONDITION("Dị ứng, bệnh nền"),
    HEALTH_CATALOGS("Danh mục dị nguyên, mức độ dị ứng, nguồn thông tin, nhu cầu chăm sóc"),
    ASSESSMENT_CARE_LEVEL("Đánh giá, mức chăm sóc"),
    ADMISSION_CONTRACT_DEPOSIT("Tiếp nhận, hợp đồng, đặt cọc"),
    WAITING_LIST("Danh sách chờ, điểm ưu tiên"),
    RESIDENCY_CHANGE_REQUEST("Yêu cầu thay đổi lưu trú"),
    ABSENCE_RETURN("Tạm vắng, trở về"),
    RESIDENCY_END_DEATH("Kết thúc lưu trú, qua đời"),
    TRANSFER_OUT("Chuyển viện"),
    ROOM_BED_CONFIGURATION("Cấu hình phòng, giường"),
    ROOM_ISOLATION("Đặt, gỡ cách ly phòng"),
    SERVICE_PRICE("Dịch vụ, đơn giá"),
    BED_ALLOCATION("Phân bổ, chuyển giường"),
    BED_MAINTENANCE("Xử lý hư hỏng, đưa giường vào bảo trì"),
    BED_READINESS("Xác nhận giường sẵn sàng"),
    CARE_PLAN("Kế hoạch chăm sóc"),
    SHIFT_TASK_PLANNING("Lập công việc của ca"),
    TASK_CHECKLIST("Checklist, ghi nhận công việc"),
    DAY_CARE_ATTENDANCE("Điểm danh bán trú đến/về"),
    OVERDUE_TASK_HANDLING("Xử lý việc quá hạn"),
    ACTIVITY_OUTING("Hoạt động, ngoài viện"),
    QUALITY_CHECK("Kiểm tra chất lượng"),
    VITAL_SIGN("Chỉ số sức khỏe"),
    ALERT_THRESHOLD("Ngưỡng cảnh báo"),
    ALERT_HANDLING("Xử lý cảnh báo"),
    SOS_BUTTON("Nút SOS"),
    INCIDENT_EMERGENCY("Sự cố, khẩn cấp"),
    INFECTION_CONTAINMENT("Khoanh vùng lây nhiễm"),
    CONTACT_LIST("Danh sách tiếp xúc"),
    PRESCRIPTION("Đơn thuốc"),
    MEDICATION_CATALOG("Danh mục thuốc"),
    MEDICATION_ADMINISTRATION("Phát thuốc, thuốc khi cần"),
    MEDICATION_RECONCILIATION("Đối chiếu thuốc, thuốc gia đình gửi"),
    MEDICATION_RECEIPT("Tiếp nhận thuốc cung ứng"),
    DIET("Chế độ ăn"),
    DISH_MENU("Món ăn, thực đơn"),
    MEAL_COUNT_TICKET("Suất ăn đã chốt, phiếu bữa ăn"),
    MEAL_PREPARATION("Chuẩn bị, giao phiếu, lưu mẫu"),
    MEAL_RECEIPT("Nhận phiếu, báo sai lệch"),
    SPECIAL_MEAL_SERVICE("Xác nhận phục vụ suất đặc biệt"),
    FAMILY_FOOD_CHECK("Đối chiếu đồ ăn gia đình"),
    STAFF_PROFILE("Hồ sơ nhân viên, chứng chỉ"),
    SHIFT_SCHEDULE("Lịch ca, phân công"),
    SHIFT_HANDOVER("Bàn giao ca"),
    SHIFT_SWAP_REQUEST("Yêu cầu đổi ca"),
    FAMILY_MEMBER_PERMISSION("Người thân và quyền"),
    VISIT_REGISTRATION("Đăng ký thăm"),
    PICKUP("Quy trình đón, ngoại lệ đón"),
    VISITOR_PREFERENCE("Mong muốn về khách thăm, người đón"),
    FAMILY_STAY("Người thân ở lại"),
    FEEDBACK("Phản hồi, khiếu nại"),
    RESIDENT_CHANNEL("Kênh người cao tuổi"),
    PERIODIC_NEWSLETTER("Bản tin định kỳ"),
    COST_ADJUSTMENT("Chi phí, khoản điều chỉnh"),
    PERIOD_CLOSING("Chốt kỳ, xuất kế toán"),
    PURCHASE_ON_BEHALF("Đề nghị mua hộ"),
    BELONGINGS("Đồ gửi"),
    DASHBOARD_REPORT("Dashboard, báo cáo"),
    ACCOUNT_PERMISSION_PARAMETER("Tài khoản, phân quyền, tham số"),
    RESIDENT_ACCOUNT("Tài khoản người cao tuổi"),
    AUDIT_LOG("Nhật ký thay đổi"),
    UNASSIGNED_RECORD_CORRECTION("Đính chính bản ghi không gắn tầng"),
    RESIDENCE_REGISTRATION("Khai báo tạm trú, lưu trú"),
    END_OF_LIFE_WISHES("Nguyện vọng cuối đời"),
    CRITICAL_SIGN("Dấu nguy kịch, xác nhận lại nguyện vọng"),
    BALANCE_LEDGER("Số dư, thu chi, đối soát"),
    STATEMENT_REPORT("Sao kê, báo cáo thu chi"),
    DAILY_CASH_CLOSING("Chốt quỹ ngày"),
    ASSET_VEHICLE("Tài sản, lịch xe"),
    INGREDIENT_INVENTORY("Kho nguyên liệu"),
    PURCHASE_REQUEST("Yêu cầu mua sắm");

    private final String label;

    Function(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
