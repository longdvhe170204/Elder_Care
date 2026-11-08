// File sinh bởi frontend/scripts/gen-api.mjs từ hợp đồng; không sửa tay.

// Nguồn: specs/011-nutrition-management/contracts (Elder Care – Feature 011 (dinh dưỡng và suất ăn))
import { request } from './client.js'

/** Danh mục chế độ ăn (hạn chế, món an toàn, mặc định) (FR-001) */
export const getNutritionDiets = (params) => request('GET', '/nutrition/diets', params)

/** Tạo chế độ ăn (Bác sĩ); bắt buộc món an toàn hợp lệ theo FR-005 (FR-001, FR-005, FR-037a) */
export const postNutritionDiets = (params) => request('POST', '/nutrition/diets', params)

/** Sửa chế độ ăn; đổi hạn chế chạy lại FR-005 với thực đơn chưa hết tuần, báo Bác sĩ và Bếp (FR-001, FR-005) */
export const putNutritionDietsById = (params) => request('PUT', '/nutrition/diets/{id}', params)

/** Ngừng chế độ ăn; chặn STILL_ASSIGNED, STILL_IN_MENU (FR-002) */
export const postNutritionDietsByIdDeactivate = (params) => request('POST', '/nutrition/diets/{id}/deactivate', params)

/** Bác sĩ thêm thành phần thực phẩm vào danh mục dị nguyên (qua 001 DiNguyenApi.themDiNguyen) (FR-003) */
export const postNutritionFoodIngredients = (params) => request('POST', '/nutrition/food-ingredients', params)

/** Danh mục món (lọc trạng thái, chế độ ăn phù hợp); kèm bản sửa chờ nếu có (FR-004, FR-004a) */
export const getNutritionDishes = (params) => request('GET', '/nutrition/dishes', params)

/** Tạo món Nháp (Nhân viên bếp) (FR-004, BR-M08-02) */
export const postNutritionDishes = (params) => request('POST', '/nutrition/dishes', params)

/** Sửa món Nháp; với món Hiệu lực chỉ sửa tên, ghi chú (nội dung đổi qua bản sửa) (FR-004, FR-004a) */
export const putNutritionDishesById = (params) => request('PUT', '/nutrition/dishes/{id}', params)

/** Lệnh trên món theo bảng trạng thái (FR-004, FR-004a) */
export const postNutritionDishesByIdByCommand = (params) => request('POST', '/nutrition/dishes/{id}/{command}', params)

/** Danh mục bữa ăn (giờ dự kiến, ngưỡng giao trễ từ CFG-M08-04) (FR-006) */
export const getNutritionMeals = (params) => request('GET', '/nutrition/meals', params)

/** Tạo bữa (Quản lý viện) (FR-006) */
export const postNutritionMeals = (params) => request('POST', '/nutrition/meals', params)

/** Sửa, ngừng bữa; bữa đã có lần chốt không xóa được (FR-006) */
export const putNutritionMealsById = (params) => request('PUT', '/nutrition/meals/{id}', params)

/** Danh mục loại đồ ăn bị cấm (FR-007) */
export const getNutritionForbiddenFoodTypes = (params) => request('GET', '/nutrition/forbidden-food-types', params)

/** Thêm, ngừng loại đồ ăn bị cấm (Quản lý viện) (FR-007) */
export const postNutritionForbiddenFoodTypes = (params) => request('POST', '/nutrition/forbidden-food-types', params)

/** Bản gán hiện hành và lịch sử (Bác sĩ, Điều dưỡng, Quản lý viện) (FR-010, FR-014) */
export const getResidentsByResidentIdDietAssignments = (params) => request('GET', '/residents/{residentId}/diet-assignments', params)

/** Lập đề xuất bản gán (Bác sĩ); trả kết quả đối chiếu FR-016 và có cần lý do chuyên môn (FR-010, FR-012, FR-013, FR-016, BR-M08-03) */
export const postResidentsByResidentIdDietAssignments = (params) => request('POST', '/residents/{residentId}/diet-assignments', params)

/** Áp dụng bản gán; Hiệu lực ngay hoặc Chờ hiệu lực (FR-017); bản cũ Ngừng cùng lệnh (FR-011, FR-013, FR-016, FR-017, FR-039, BR-M08-03) */
export const postDietAssignmentsByIdApply = (params) => request('POST', '/diet-assignments/{id}/apply', params)

/** Hủy bản gán Đề xuất hoặc Chờ hiệu lực (lý do) (FR-012) */
export const postDietAssignmentsByIdCancel = (params) => request('POST', '/diet-assignments/{id}/cancel', params)

/** Kết quả đối chiếu xung đột của một người với thực đơn chưa hết tuần (Bác sĩ) (FR-020, FR-022, BR-M08-02, BR-M01-07) */
export const getResidentsByResidentIdDishConflictCheck = (params) => request('GET', '/residents/{residentId}/dish-conflict-check', params)

/** Thực đơn theo tuần (FR-023) */
export const getMenus = (params) => request('GET', '/menus', params)

/** Tạo thực đơn Nháp, tùy chọn sao chép từ thực đơn trước (Nhân viên bếp) (FR-023, FR-024) */
export const postMenus = (params) => request('POST', '/menus', params)

/** Chi tiết thực đơn (nội dung, kết quả kiểm tra, lịch sử đổi món) (FR-024, FR-031) */
export const getMenusById = (params) => request('GET', '/menus/{id}', params)

/** Sửa nội dung thực đơn Nháp (FR-024) */
export const putMenusById = (params) => request('PUT', '/menus/{id}', params)

/** Thông tin trước khi gửi: số người theo chế độ ăn (không tên); tồn kho bỏ theo Q-392 (FR-025a) */
export const getMenusByIdPreSubmitInfo = (params) => request('GET', '/menus/{id}/pre-submit-info', params)

/** Chạy kiểm tra độ phủ, lặp món, dị ứng trong một lần (không đổi trạng thái) (FR-025, FR-026, FR-027, BR-M08-06, BR-M08-07, BR-M08-08) */
export const postMenusByIdCheck = (params) => request('POST', '/menus/{id}/check', params)

/** Lệnh trên thực đơn theo bảng trạng thái (FR-025, FR-025a, FR-028, FR-029) */
export const postMenusByIdByCommand = (params) => request('POST', '/menus/{id}/{command}', params)

/** Đổi món (thay, thêm, bớt món; đổi món thay thế) trên thực đơn Công bố hoặc Đã áp dụng (Nhân viên bếp; lý do); sau chốt tạo phát sinh (FR-031, FR-032, FR-039, BR-M08-08, BR-M08-13) */
export const postMenusByIdDishSwaps = (params) => request('POST', '/menus/{id}/dish-swaps', params)

/** Các bữa trong ngày, đã chốt chưa, mốc mở chốt (giờ bữa − CFG-M08-01) (FR-033) */
export const getDailyMeals = (params) => request('GET', '/daily-meals', params)

/** Tính sẵn người được tính, số suất theo tầng và chế độ ăn, suất đặc biệt (chỉ xem; không lưu) (FR-034, FR-035, FR-036, FR-036a, FR-038, BR-M08-01) */
export const getDailyMealsByIdPrecomputed = (params) => request('GET', '/daily-meals/{id}/precomputed', params)

/** Chốt suất (Nhân viên bếp); tính lại tại lúc lệnh, lập phiếu mỗi tầng/khu và suất ăn người thân trong cùng lệnh (FR-033, FR-035, FR-037a, FR-038, FR-043, FR-044, BR-M08-01, BR-M08-09) */
export const postDailyMealsByIdClosePortions = (params) => request('POST', '/daily-meals/{id}/close-portions', params)

/** Danh sách phát sinh của bữa (bếp thấy dạng không có thông tin sức khỏe) (FR-039, FR-041) */
export const getDailyMealsByIdAdjustments = (params) => request('GET', '/daily-meals/{id}/adjustments', params)

/** Bếp xác nhận phát sinh; phát sinh "+" của tầng/khu chưa có phiếu lập phiếu cùng lệnh (FR-039, FR-041, FR-044, BR-M08-13) */
export const postAdjustmentsByIdConfirm = (params) => request('POST', '/adjustments/{id}/confirm', params)

/** Người nhận tại tầng yêu cầu suất bổ sung sau giờ bữa, trong ngưỡng giao trễ (FR-040) */
export const postDailyMealsByIdExtraPortions = (params) => request('POST', '/daily-meals/{id}/extra-portions', params)

/** Phục vụ suất giữ cho người trở về trong ngưỡng (tạo phát sinh "+1" không cần giao) (FR-040a) */
export const postSpecialPortionsByIdServeHeldPortion = (params) => request('POST', '/special-portions/{id}/serve-held-portion', params)

/** Ghi hủy bỏ suất giữ khi hết ngưỡng (FR-040a) */
export const postSpecialPortionsByIdDiscardHeldPortion = (params) => request('POST', '/special-portions/{id}/discard-held-portion', params)

/** Bác sĩ chỉ định món thay thế riêng cho bữa chưa qua giờ; sau chốt tạo phát sinh đổi suất (FR-037, FR-039) */
export const postSpecialPortionsByIdAssignDish = (params) => request('POST', '/special-portions/{id}/assign-dish', params)

/** Phiếu theo bữa ngày, tầng/khu (bếp nhận KitchenMealSlip; tầng nhận FloorMealSlip trong phạm vi) (FR-044, FR-045, FR-046, BR-M08-09, BR-M08-10) */
export const getMealSlips = (params) => request('GET', '/meal-slips', params)

/** Bếp đánh dấu suất đặc biệt đã chuẩn bị (FR-045) */
export const postSpecialPortionsByIdMarkPrepared = (params) => request('POST', '/special-portions/{id}/mark-prepared', params)

/** Lệnh trên phiếu theo bảng trạng thái (FR-047, FR-048, BR-M08-10, BR-M08-11, BR-M08-15) */
export const postMealSlipsByIdByCommand = (params) => request('POST', '/meal-slips/{id}/{command}', params)

/** Giao (bếp), nhận hoặc báo sai lệch (người nhận tại tầng) phần bổ sung của phiếu (FR-048b) */
export const postSupplementaryPortionsByIdByCommand = (params) => request('POST', '/supplementary-portions/{id}/{command}', params)

/** Xác nhận phục vụ suất đặc biệt (Nhân viên chăm sóc, Điều dưỡng); ghi ngoại tuyến được (FR-049, FR-050, FR-051, BR-M08-14) */
export const postSpecialPortionsByIdConfirmServing = (params) => request('POST', '/special-portions/{id}/confirm-serving', params)

/** Đồ ăn gia đình của một người (FR-052) */
export const getResidentsByResidentIdFamilyFoods = (params) => request('GET', '/residents/{residentId}/family-foods', params)

/** Ghi nhận đồ ăn gia đình; hệ thống gán trạng thái theo đối chiếu (FR-053) (FR-052, FR-053, FR-054, FR-057, BR-M08-04) */
export const postResidentsByResidentIdFamilyFoods = (params) => request('POST', '/residents/{residentId}/family-foods', params)

/** Lệnh: approve-use (ghi chú), reject-use (lý do), record-disposal, end (FR-055, FR-057) */
export const postFamilyFoodsByIdByCommand = (params) => request('POST', '/family-foods/{id}/{command}', params)

/** Thông báo xem lại chế độ ăn đã gửi Bác sĩ (để chọn căn cứ khi lập bản gán) (FR-058, FR-059, BR-M08-05) */
export const getNutritionReviewNotices = (params) => request('GET', '/nutrition/review-notices', params)

/** Món cần lưu mẫu và bản ghi lưu mẫu (FR-060, FR-061) */
export const getDailyMealsByIdRetainedSamples = (params) => request('GET', '/daily-meals/{id}/retained-samples', params)

/** Ghi lưu mẫu (Nhân viên bếp); gọi lại để thêm món mới sau phát sinh (FR-060, FR-061, BR-M08-15) */
export const postDailyMealsByIdRetainedSamples = (params) => request('POST', '/daily-meals/{id}/retained-samples', params)

/** Ghi thời điểm hủy mẫu (FR-062) */
export const postRetainedSamplesByIdDiscard = (params) => request('POST', '/retained-samples/{id}/discard', params)
