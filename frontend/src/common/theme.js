// Chế độ sáng/tối (DESIGN.md mục Màu): mặc định sáng; người dùng đổi bằng nút trong khối
// người dùng, lựa chọn được nhớ theo trình duyệt. Token tối khai báo dưới lớp .dark.
const STORAGE_KEY = 'elder-care-theme'

export function readTheme() {
  try {
    return localStorage.getItem(STORAGE_KEY) === 'dark' ? 'dark' : 'light'
  } catch {
    return 'light'
  }
}

export function applyTheme(theme) {
  document.documentElement.classList.toggle('dark', theme === 'dark')
  try {
    localStorage.setItem(STORAGE_KEY, theme)
  } catch {
    // Trình duyệt chặn lưu trữ: vẫn đổi được trong phiên này
  }
}
