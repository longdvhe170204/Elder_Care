// Nhãn tiếng Việt cho màn bản đồng ý (001 T059; hợp đồng api-001, schema Consent).

export const CONSENTER_LABELS = {
  RESIDENT: 'Người cao tuổi',
  LEGAL_REPRESENTATIVE: 'Người đại diện theo pháp luật',
}

export const STATUS_LABELS = {
  ACTIVE: 'Hiệu lực',
  WITHDRAWN: 'Đã rút lại',
}

// Hồ sơ trạng thái cuối: không ghi nhận bản mới được, rút lại vẫn làm được (FR-014a)
export const FINAL_STATUSES = ['RESIDENCY_ENDED', 'DECEASED', 'ADMISSION_CANCELLED']
