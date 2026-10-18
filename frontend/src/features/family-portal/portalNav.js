// Mục ở "Thêm" của cổng người thân; chỉ hiện khi người xem có quyền (012 FR-046, FR-046b, FR-049).
// Bản mẫu đọc quyền từ dữ liệu minh họa; quyền thật do backend trả.
export const MORE_ITEMS = [
  { key: 'health', label: 'Sức khỏe', allowed: (v) => v.perms.health },
  { key: 'costs', label: 'Chi phí, số dư', allowed: (v) => v.perms.costs },
  { key: 'feedback', label: 'Phản hồi', allowed: () => true },
  { key: 'deposits', label: 'Đồ gửi', allowed: (v) => v.representative || v.perms.pickup },
  { key: 'contract', label: 'Hợp đồng, dịch vụ', allowed: (v) => v.representative },
  { key: 'account', label: 'Tài khoản, quyền của tôi', allowed: () => true },
]

export function allowedPage(page, viewer) {
  const item = MORE_ITEMS.find((i) => i.key === page)
  return !item || item.allowed(viewer)
}
