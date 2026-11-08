// Nhãn tiếng Việt cho enum của api-017 (017; tên trạng thái theo nghiep-vu.md mục 15, DESIGN.md 3.5) và định dạng số
// tiền (NFR-11). Hàm thuần, không JSX.

export const TYPE_LABELS = {
  CASH_PAYMENT: 'Nộp tiền mặt',
  BANK_TRANSFER_PAYMENT: 'Nộp chuyển khoản',
  STATEMENT_PAYMENT: 'Thanh toán bảng chi phí',
  REFUND: 'Hoàn tiền',
  DEPOSIT_OFFSET: 'Cấn trừ tiền cọc',
  ADJUSTMENT: 'Điều chỉnh',
  DEPOSIT_RECEIPT: 'Thu tiền cọc',
  DEPOSIT_REFUND: 'Hoàn tiền cọc',
}

export const LEDGER_LABELS = { BALANCE: 'Số dư', DEPOSIT: 'Tiền cọc' }

export const TRANSACTION_STATUS_LABELS = {
  PENDING_APPROVAL: 'Chờ duyệt',
  CONFIRMED: 'Đã xác nhận',
  REJECTED: 'Từ chối',
  CANCELLED: 'Đã hủy',
}

export const STATEMENT_TYPE_LABELS = {
  REGULAR: 'Bảng thường',
  SUPPLEMENTARY: 'Bảng bổ sung',
  FINAL_PERIOD: 'Bảng kỳ cuối',
}

export const BALANCE_STATUS_LABELS = { NORMAL: 'Bình thường', RUNNING_LOW: 'Sắp hết tiền', IN_DEBT: 'Còn nợ' }

export const DEPOSIT_STATUS_LABELS = {
  NOT_REQUIRED: 'Không yêu cầu đặt cọc',
  SATISFIED: 'Đã đáp ứng',
  NOT_SATISFIED: 'Chưa đáp ứng',
}

export const METHOD_LABELS = { CASH: 'Tiền mặt', BANK_TRANSFER: 'Chuyển khoản', INTERNAL: 'Nội bộ', OTHER: 'Khác' }

export const labelOf = (map, code) => map[code] ?? code

export function formatMoney(value) {
  return value === undefined || value === null ? '—' : `${Number(value).toLocaleString('vi-VN')} đ`
}
