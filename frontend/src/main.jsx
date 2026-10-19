import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import { applyTheme, readTheme } from '@/common/theme'
import { FamilyLoginPage } from '@/features/family-portal/FamilyLoginPage'
import { FamilyPortal } from '@/features/family-portal/FamilyPortal'

applyTheme(readTheme())

// Bản mẫu: đăng nhập người thân ở /login, cổng người thân ở /family (trang chủ là trang giới thiệu viện);
// React Router đã cài (000 T005), chuyển sang khi làm màn hình chức năng.
const path = window.location.pathname
let page = <App />
if (path.startsWith('/login')) page = <FamilyLoginPage />
else if (path.startsWith('/family')) page = <FamilyPortal />

createRoot(document.getElementById('root')).render(<StrictMode>{page}</StrictMode>)
