import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import { applyTheme, readTheme } from '@/common/theme'
import { FamilyLandingPage } from '@/features/family-landing/FamilyLandingPage'
import { FamilyLoginPlaceholder } from '@/features/family-landing/FamilyLoginPlaceholder'

applyTheme(readTheme())

// Bản mẫu: trang giới thiệu cổng người thân ở /nguoi-than, chỗ đăng nhập ở /dang-nhap;
// chưa có React Router (000 T005).
const path = window.location.pathname
let page = <App />
if (path.startsWith('/nguoi-than')) page = <FamilyLandingPage />
else if (path.startsWith('/dang-nhap')) page = <FamilyLoginPlaceholder />

createRoot(document.getElementById('root')).render(<StrictMode>{page}</StrictMode>)
