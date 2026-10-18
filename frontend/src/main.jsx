import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import { applyTheme, readTheme } from '@/common/theme'
import { FamilyLoginPage } from '@/features/family-portal/FamilyLoginPage'
import { FamilyPortal } from '@/features/family-portal/FamilyPortal'

applyTheme(readTheme())

// Bản mẫu: đăng nhập người thân ở /dang-nhap, cổng người thân ở /cong-nguoi-than (trang chủ là trang giới thiệu viện);
// chưa có React Router (000 T005).
const path = window.location.pathname
let page = <App />
if (path.startsWith('/dang-nhap')) page = <FamilyLoginPage />
else if (path.startsWith('/cong-nguoi-than')) page = <FamilyPortal />

createRoot(document.getElementById('root')).render(<StrictMode>{page}</StrictMode>)
