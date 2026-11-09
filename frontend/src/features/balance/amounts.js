// Tiền VND (NFR-11) và nhãn của phần số dư 017 (glossary mục 23.3).

const MONEY = new Intl.NumberFormat('vi-VN')

export function formatMoney(amount) {
  return `${MONEY.format(amount ?? 0)} đồng`
}

export const BALANCE_STATUS = { NORMAL: 'Bình thường', RUNNING_LOW: 'Sắp hết tiền', IN_DEBT: 'Còn nợ' }

export const DEPOSIT_STATUS = { NOT_REQUIRED: 'Không yêu cầu', SATISFIED: 'Đã đáp ứng', NOT_SATISFIED: 'Chưa đáp ứng' }

export const LEDGER = { BALANCE: 'Số dư', DEPOSIT: 'Tiền cọc' }

export const TRANSACTION_TYPE = {
  CASH_PAYMENT: 'Nộp tiền mặt',
  BANK_TRANSFER_PAYMENT: 'Nộp chuyển khoản',
  STATEMENT_PAYMENT: 'Thanh toán bảng chi phí',
  REFUND: 'Hoàn tiền',
  DEPOSIT_OFFSET: 'Cấn trừ tiền cọc',
  ADJUSTMENT: 'Điều chỉnh',
  DEPOSIT_RECEIPT: 'Thu cọc',
  DEPOSIT_REFUND: 'Hoàn cọc',
}

// Số dư âm hiện là "còn nợ" kèm số tiền (FR-002)
export function balanceText(balance) {
  return balance < 0 ? `Còn nợ ${formatMoney(-balance)}` : formatMoney(balance)
}
