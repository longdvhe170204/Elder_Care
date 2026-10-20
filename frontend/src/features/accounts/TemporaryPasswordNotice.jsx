import { KeyRound } from 'lucide-react'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'

// Mật khẩu tạm chỉ trả một lần khi tạo tài khoản hoặc cấp lại mật khẩu (FR-006); người dùng phải đổi ở lần đăng nhập
// kế tiếp. Màn không lưu lại: đóng khung là mất.
export function TemporaryPasswordNotice({ password }) {
  return (
    <Alert variant="attention" role="status">
      <KeyRound aria-hidden="true" />
      <AlertTitle>Mật khẩu tạm</AlertTitle>
      <AlertDescription>
        <p className="type-title tabular-nums select-all" data-testid="temporary-password">
          {password}
        </p>
        <p>Chỉ hiện một lần. Giao trực tiếp cho người dùng; người dùng phải đổi mật khẩu khi đăng nhập.</p>
      </AlertDescription>
    </Alert>
  )
}
