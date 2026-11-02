import { KeyRound } from 'lucide-react'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'

// Mật khẩu tạm chỉ trả một lần khi tạo tài khoản hoặc cấp lại mật khẩu (FR-006); người dùng phải đổi ở lần đăng nhập
// kế tiếp. Màn không lưu lại: đóng khung là mất. Tài khoản người cao tuổi ghi thêm hệ quả của đăng nhập QR (FR-006).
const DEFAULT_HINT = 'Chỉ hiện một lần. Giao trực tiếp cho người dùng; người dùng phải đổi mật khẩu khi đăng nhập.'

export function TemporaryPasswordNotice({ password, hint = DEFAULT_HINT }) {
  return (
    <Alert variant="attention" role="status">
      <KeyRound aria-hidden="true" />
      <AlertTitle>Mật khẩu tạm</AlertTitle>
      <AlertDescription>
        <p className="type-title tabular-nums select-all" data-testid="temporary-password">
          {password}
        </p>
        <p>{hint}</p>
      </AlertDescription>
    </Alert>
  )
}
