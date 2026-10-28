// Nhãn danh mục M01 (001 T072; FR-018, FR-018a, BR-M01-11). Mức chăm sóc cố định, chỉ đọc, không đưa vào màn này.

export const CATALOG_TYPES = [
  { value: 'allergens', label: 'Dị nguyên' },
  { value: 'allergy-severities', label: 'Mức độ dị ứng' },
  { value: 'information-sources', label: 'Nguồn thông tin' },
  { value: 'care-needs', label: 'Nhu cầu chăm sóc' },
]

export const ALLERGEN_GROUPS = {
  ACTIVE_INGREDIENT: 'Hoạt chất',
  FOOD: 'Thực phẩm',
  OTHER: 'Khác',
}

export const STATUS_LABELS = {
  ACTIVE: 'Hiệu lực',
  INACTIVE: 'Ngừng hiệu lực',
}
