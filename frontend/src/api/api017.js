// File sinh bởi frontend/scripts/gen-api.mjs từ hợp đồng; không sửa tay.

// Nguồn: specs/017-resident-balance-ledger/contracts (Elder Care – Feature 017 (số dư và thu chi))
import { request } from './client.js'

/** Số dư, số dư cọc, tình trạng, trạng thái đặt cọc, mã nộp tiền, thông tin nộp tiền (Kế toán, Hành chính, Quản lý viện) (FR-002, FR-003, FR-004, FR-006) */
export const getResidentsByResidentIdBalance = (params) => request('GET', '/residents/{residentId}/balance', params)

/** Giao dịch của hồ sơ theo sổ, khoảng, trạng thái (FR-010) */
export const getResidentsByResidentIdBalanceTransactions = (params) => request('GET', '/residents/{residentId}/balance-transactions', params)

/** Danh mục ngân hàng (mã BIN NAPAS) còn hiệu lực, cho ô chọn ngân hàng của CFG-M11-10 (Quản lý viện; bổ sung 2026-10-22) (FR-003) */
export const getBanks = (params) => request('GET', '/banks', params)

/** Tìm hồ sơ theo mã nộp tiền đọc từ nội dung chuyển khoản (Kế toán) (FR-007) */
export const getBalancesByPaymentCode = (params) => request('GET', '/balances/by-payment-code', params)

/** Kế toán ghi nhận khoản nộp tiền mặt vào số dư hoặc sổ cọc; Đã xác nhận ngay; không cấp số phiếu (FR-005, FR-006, FR-018, BR-M11-16) */
export const postResidentsByResidentIdCashPayments = (params) => request('POST', '/residents/{residentId}/cash-payments', params)

/** Kế toán ghi nhận khoản chuyển khoản đã về tài khoản của viện; nhiều dòng (khác sổ) cùng mã giao dịch ngân hàng (FR-007, FR-006, FR-018, BR-M11-12) */
export const postResidentsByResidentIdBankTransferPayments = (params) => request('POST', '/residents/{residentId}/bank-transfer-payments', params)

/** Kế toán lập hoàn tiền, hoàn cọc hoặc điều chỉnh (Chờ duyệt); tạo yêu cầu M11.BALANCE_TRANSACTION của 000 (FR-010, FR-011, FR-013, FR-014, BR-M11-10, BR-M11-14) */
export const postBalanceTransactions = (params) => request('POST', '/balance-transactions', params)

/** Giao dịch Chờ duyệt (Quản lý viện, Kế toán) kèm kết quả kiểm tra hiện tại (FR-013) */
export const getBalanceTransactionsPendingApproval = (params) => request('GET', '/balance-transactions/pending-approval', params)

/** Kế toán cấn trừ tiền cọc vào số dư âm (cặp hai giao dịch liên kết, không duyệt) (FR-015) */
export const postResidentsByResidentIdDepositOffsets = (params) => request('POST', '/residents/{residentId}/deposit-offsets', params)

/** Danh sách hồ sơ Sắp hết tiền, Còn nợ (Kế toán, Quản lý viện) (FR-016, FR-017, FR-018, BR-M11-13) */
export const getBalanceStatuses = (params) => request('GET', '/balance-statuses', params)

/** Quản lý viện ghi "dừng nhắc" cho hồ sơ trạng thái cuối còn nợ (FR-020) */
export const postResidentsByResidentIdBalanceStopReminders = (params) => request('POST', '/residents/{residentId}/balance/stop-reminders', params)

/** Điều kiện quyết toán (hai mã) và các phiên bản bảng quyết toán (FR-022, FR-023, FR-034) */
export const getResidentsByResidentIdSettlement = (params) => request('GET', '/residents/{residentId}/settlement', params)

/** Kế toán Xác nhận quyết toán; dựng bảng quyết toán PDF phiên bản mới (FR-022, FR-024, FR-034, FR-037, BR-M11-22, BR-M11-23) */
export const postResidentsByResidentIdSettlementConfirm = (params) => request('POST', '/residents/{residentId}/settlement/confirm', params)

/** Kế toán đính kèm bản scan có chữ ký của người đại diện vào phiên bản hiện hành (một lần; không đổi điều kiện) (FR-034) */
export const postSettlementStatementsByIdSignedCopy = (params) => request('POST', '/settlement-statements/{id}/signed-copy', params)

/** Tải đúng file PDF đã lưu của phiên bản; ghi lần tải (FR-037, BR-M11-23) */
export const getSettlementStatementsByIdFile = (params) => request('GET', '/settlement-statements/{id}/file', params)

/** Bảng chi phí Đã chốt (đã có giao dịch thanh toán) chưa phát hành phiếu (Kế toán) (FR-031) */
export const getPaymentNoticesPendingIssue = (params) => request('GET', '/payment-notices/pending-issue', params)

/** Kế toán phát hành phiếu báo phí (PDF và Excel) cho một bảng Đã chốt; mỗi bảng tối đa một phiếu (FR-031, FR-032, FR-037, FR-030, BR-M11-21, BR-M11-23) */
export const postCostsStatementsByStatementIdPaymentNotice = (params) => request('POST', '/costs/statements/{statementId}/payment-notice', params)

/** Phiếu báo phí của hồ sơ (Kế toán, Quản lý viện) (FR-031) */
export const getResidentsByResidentIdPaymentNotices = (params) => request('GET', '/residents/{residentId}/payment-notices', params)

/** Tải đúng file đã lưu (PDF hoặc XLSX); ghi lần tải (FR-037, BR-M11-23) */
export const getPaymentNoticesByIdFile = (params) => request('GET', '/payment-notices/{id}/file', params)

/** Xuất sao kê số dư (gồm giao dịch tiền cọc) của một hoặc nhiều hồ sơ, Excel hoặc PDF (Kế toán; Quản lý viện xem được) (FR-025, FR-027) */
export const postBalanceExportsStatement = (params) => request('POST', '/balance-exports/statement', params)

/** Xuất báo cáo thu chi thu gọn theo khoảng, Excel hoặc PDF (Q-386) (FR-025, FR-027) */
export const postBalanceExportsIncomeExpenseReport = (params) => request('POST', '/balance-exports/income-expense-report', params)

/** Tải file của một lần xuất (đúng file đã dựng) (FR-027) */
export const getBalanceExportsByIdFile = (params) => request('GET', '/balance-exports/{id}/file', params)

/** Số dư, số dư cọc, giao dịch Đã xác nhận, thông tin nộp tiền (người thân có quyền xem chi phí) (FR-003) */
export const getFamilyPortalResidentsByResidentIdBalance = (params) => request('GET', '/family-portal/residents/{residentId}/balance', params)

/** Phiếu báo phí và phiên bản hiện hành của bảng quyết toán (người thân có quyền xem chi phí) (FR-038) */
export const getFamilyPortalResidentsByResidentIdDocuments = (params) => request('GET', '/family-portal/residents/{residentId}/documents', params)

/** Người thân tải chứng từ (đúng file đã lưu); ghi lần tải (FR-038, FR-037) */
export const getFamilyPortalDocumentsByTypeByIdFile = (params) => request('GET', '/family-portal/documents/{type}/{id}/file', params)

/** Người thân có quyền xem chi phí xuất sao kê của người cao tuổi (Excel hoặc PDF, khoảng ≤ CFG-M14-01) (FR-026, FR-027) */
export const postFamilyPortalResidentsByResidentIdBalanceStatement = (params) => request('POST', '/family-portal/residents/{residentId}/balance-statement', params)
