import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import { applyTheme, readTheme } from '@/common/theme'
import { ChangePasswordPage } from '@/features/auth/ChangePasswordPage'
import { LoginPage } from '@/features/auth/LoginPage'
import { FamilyPortal } from '@/features/family-portal/FamilyPortal'

applyTheme(readTheme())

// Đăng nhập chung ở /login, đổi mật khẩu ở /change-password (002 T045); cổng người thân ở /family (trang chủ là trang
// giới thiệu viện);
// React Router đã cài (000 T005), chuyển sang khi làm màn hình chức năng.
const path = window.location.pathname
let page = <App />
if (path.startsWith('/login')) page = <LoginPage />
else if (path.startsWith('/change-password')) page = <ChangePasswordPage />
else if (path.startsWith('/family')) page = <FamilyPortal />

createRoot(document.getElementById('root')).render(<StrictMode>{page}</StrictMode>)
