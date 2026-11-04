import { useEffect, useRef, useState } from 'react'
import { Dialog as DialogPrimitive } from '@base-ui/react/dialog'
import { getMe, onResponse } from '@/api'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogOverlay,
  DialogPortal,
  DialogTitle,
} from '@/components/ui/dialog'
import { homeOf } from '@/common/session'
import { LoginForm } from '@/features/auth/LoginPage'

// Hết phiên trên web (002 T047b; FR-013a; DESIGN.md 9.1 tiêu chí 2.2.5, 9.3 tiêu chí 2.2.1). Đếm giờ không thao tác như
// máy chủ: mỗi phản hồi của yêu cầu do người dùng gây ra đếm lại, yêu cầu tự làm mới (X-Background-Refresh) thì không.
// Còn 2 phút thì báo, "Tiếp tục làm việc" gửi GET /me để đếm lại. Hết giờ hoặc gặp 401 SESSION_EXPIRED thì đăng nhập lại
// ngay trên màn: màn bên dưới không bị tháo nên phần đang nhập còn nguyên, chỉ nằm trong bộ nhớ của trang, không gửi
// lên máy chủ. Đăng nhập lại tài khoản khác thì tải lại trang (bỏ phần đang nhập); Đăng xuất cũng rời trang.

export const WARN_BEFORE_MS = 120_000
const ENDED = ['SESSION_EXPIRED', 'UNAUTHORIZED']

const countdown = (ms) => {
  const seconds = Math.max(0, Math.ceil(ms / 1000))
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`
}

export function SessionGuard({ accountId, timeoutSeconds, onLogout, children }) {
  const [lastActive, setLastActive] = useState(() => Date.now())
  const [now, setNow] = useState(() => Date.now())
  const [ended, setEnded] = useState(false)
  const continueRef = useRef(null)

  useEffect(
    () =>
      onResponse(({ status, errorCode, background }) => {
        if (status === 401 && ENDED.includes(errorCode)) setEnded(true)
        else if (!background && status !== 401) setLastActive(Date.now())
      }),
    [],
  )

  useEffect(() => {
    if (timeoutSeconds == null) return
    const timer = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(timer)
  }, [timeoutSeconds])

  const remaining = timeoutSeconds == null ? Infinity : lastActive + timeoutSeconds * 1000 - now
  const expired = ended || remaining <= 0
  const warning = !expired && remaining <= WARN_BEFORE_MS

  function keepWorking() {
    getMe().catch(() => {})
  }

  function loggedIn(data) {
    if (data.user?.accountId !== accountId || data.mustChangePassword) {
      window.location.assign(homeOf({ ...data.user, mustChangePassword: data.mustChangePassword }))
      return
    }
    const at = Date.now()
    setLastActive(at)
    setNow(at)
    setEnded(false)
  }

  return (
    <>
      {children}

      <Dialog open={warning} onOpenChange={(open) => !open && keepWorking()}>
        {/* Focus đầu tiên vào nút giữ phiên, không vào Đăng xuất */}
        <DialogContent showCloseButton={false} initialFocus={continueRef} className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="type-title">Phiên đăng nhập sắp hết</DialogTitle>
            <DialogDescription className="type-body text-foreground">
              Bạn chưa thao tác một lúc. Phiên sẽ kết thúc sau{' '}
              <span className="tabular-nums">{countdown(remaining)}</span>; phần đang nhập vẫn còn nếu bạn đăng nhập
              lại.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="ghost" className="h-11" onClick={onLogout}>
              Đăng xuất
            </Button>
            <Button ref={continueRef} className="h-11" onClick={keepWorking}>
              Tiếp tục làm việc
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Nền che kín màn để người khác dùng máy không đọc được dữ liệu; Esc, bấm ra ngoài không đóng được */}
      <Dialog open={expired} disablePointerDismissal onOpenChange={() => {}}>
        <DialogPortal>
          <DialogOverlay className="bg-background" />
          <DialogPrimitive.Popup
            data-slot="dialog-content"
            className="fixed top-1/2 left-1/2 z-50 flex max-h-[calc(100%-2rem)] w-full max-w-[calc(100%-2rem)] -translate-x-1/2 -translate-y-1/2 flex-col gap-4 overflow-y-auto rounded-xl bg-popover p-6 text-popover-foreground ring-1 ring-foreground/10 outline-none sm:max-w-sm"
          >
            <DialogHeader>
              <DialogTitle className="type-title">Phiên đăng nhập đã hết</DialogTitle>
              <DialogDescription className="type-body text-foreground">
                Phiên kết thúc vì không có thao tác quá thời gian cho phép. Đăng nhập lại đúng tài khoản này để làm
                tiếp; phần đang nhập vẫn còn nguyên.
              </DialogDescription>
            </DialogHeader>
            <LoginForm submitLabel="Đăng nhập lại" onLoggedIn={loggedIn} />
            <Button variant="ghost" className="h-11" onClick={onLogout}>
              Đăng xuất
            </Button>
          </DialogPrimitive.Popup>
        </DialogPortal>
      </Dialog>
    </>
  )
}
