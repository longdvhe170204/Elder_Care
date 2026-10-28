import { useEffect, useRef } from 'react'
import { X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { cn } from '@/lib/utils'
import { useWide } from './useWide'

// Danh sách và khung chi tiết (DESIGN.md mục 5, 7.2 detail-panel). Từ 1280px: khung cố định bên phải 420px, chỉ hiện
// khi đã chọn một mục; mở thì focus vào khung, có nút Đóng và Esc, đóng thì focus trở về mục đã mở (WCAG 2.1.2).
// Hẹp hơn: Sheet mở đè (Sheet tự trả focus). renderDetail nhận { Title, Description } để khung và Sheet có đúng
// tiêu đề cho trình đọc màn hình; detailKey đổi khi chọn mục khác.
export function DetailLayout({ open, detailKey, onClose, label, renderDetail, children }) {
  const wide = useWide()
  const panelOpen = wide && open
  const panelRef = useRef(null)
  const triggerRef = useRef(null)
  const wasOpen = useRef(false)

  useEffect(() => {
    if (panelOpen) {
      triggerRef.current = document.activeElement
      panelRef.current?.focus()
    } else if (wasOpen.current) {
      const trigger = triggerRef.current
      requestAnimationFrame(() => trigger?.focus?.())
    }
    wasOpen.current = panelOpen
  }, [panelOpen, detailKey])

  return (
    <div
      className={cn(
        'mx-auto flex max-w-[1600px] flex-col gap-5',
        panelOpen && 'grid grid-cols-[minmax(0,1fr)_420px] items-start gap-0',
      )}
    >
      <div className={cn('flex min-w-0 flex-col gap-5', panelOpen && 'pr-8')}>{children}</div>

      {panelOpen && (
        <aside
          ref={panelRef}
          tabIndex={-1}
          aria-label={label}
          onKeyDown={(event) => event.key === 'Escape' && onClose()}
          className="sticky top-0 -my-6 -mr-8 flex h-screen flex-col gap-2 overflow-y-auto border-l bg-card p-6 outline-none"
        >
          <Button variant="ghost" size="icon" className="self-end" aria-label="Đóng chi tiết" onClick={onClose}>
            <X aria-hidden="true" />
          </Button>
          {renderDetail({ Title: 'h2', Description: 'p' })}
        </aside>
      )}

      <Sheet open={!wide && open} onOpenChange={(next) => !next && onClose()}>
        <SheetContent className="overflow-y-auto p-6 sm:max-w-lg">
          {!wide && open && renderDetail({ Title: SheetTitle, Description: SheetDescription })}
        </SheetContent>
      </Sheet>
    </div>
  )
}
