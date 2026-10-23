// Nhãn tiếng Việt cho phần sức khỏe của hồ sơ (001 US2; FR-017, FR-018). Tên trường theo hợp đồng api-001
// (HealthRecordContent, HealthItem).

export const HEALTH_TYPE_LABELS = {
  ALLERGY: 'Dị ứng',
  CHRONIC_CONDITION: 'Bệnh nền',
  MEDICAL_HISTORY: 'Tiền sử bệnh',
}

// Thứ tự nhóm trong danh sách: dị ứng trước để không chìm giữa các mục khác (DESIGN.md 7.2, care-notes).
export const HEALTH_TYPE_ORDER = ['ALLERGY', 'CHRONIC_CONDITION', 'MEDICAL_HISTORY']

// Các ô của hồ sơ sức khỏe ban đầu (FR-017); cũng là ô nội dung của form đính chính (000).
export const HEALTH_RECORD_FIELDS = [
  { name: 'bloodType', label: 'Nhóm máu', short: true },
  { name: 'mobility', label: 'Khả năng vận động' },
  { name: 'selfCareAbility', label: 'Khả năng tự chăm sóc' },
  { name: 'cognition', label: 'Khả năng nhận thức' },
  { name: 'initialNeedsNote', label: 'Ghi chú nhu cầu chăm sóc ban đầu' },
  { name: 'nutritionNeeds', label: 'Nhu cầu dinh dưỡng' },
  { name: 'currentMedications', label: 'Thuốc đang sử dụng khi tiếp nhận' },
  { name: 'rehabilitationNeeds', label: 'Nhu cầu phục hồi chức năng' },
]

export const HEALTH_RECORD_TYPE = 'M01.INITIAL_HEALTH_RECORD'

// Danh mục cần cho form ghi nhận mục (GET /catalogs/{type}).
export const CATALOG_TYPES = {
  allergens: 'allergens',
  severities: 'allergy-severities',
  sources: 'information-sources',
}
