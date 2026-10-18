import { ChevronRight, FileText, HeartPulse, LogOut, MessageSquare, Package, Receipt, UserRound } from 'lucide-react'
import { PageHeader } from '@/common/PageHeader'
import { PortalCard } from './PortalParts'
import { MORE_ITEMS } from './portalNav'

// Mục "Thêm": chỉ liệt kê mục người xem có quyền; mục không có quyền không hiện (012 FR-046, FR-046b).
const ICONS = { health: HeartPulse, costs: Receipt, feedback: MessageSquare, deposits: Package, contract: FileText, account: UserRound }

export function MorePage({ viewer, onNavigate, onExit }) {
  const items = MORE_ITEMS.filter((i) => i.allowed(viewer))
  return (
    <>
      <PageHeader title="Thêm" />
      <PortalCard>
        <ul className="flex flex-col">
          {items.map((item) => {
            const Icon = ICONS[item.key]
            return (
              <li key={item.key} className="border-t first:border-t-0">
                <button type="button" onClick={() => onNavigate(item.key)} className="flex min-h-12 w-full items-center gap-3 rounded-lg text-left">
                  <Icon className="size-5 text-muted-foreground" aria-hidden="true" />
                  <span className="flex-1 type-label">{item.label}</span>
                  <ChevronRight className="size-4 text-muted-foreground" aria-hidden="true" />
                </button>
              </li>
            )
          })}
          <li className="border-t">
            <a href="/login" className="flex min-h-12 w-full items-center gap-3 rounded-lg">
              <LogOut className="size-5 text-muted-foreground" aria-hidden="true" />
              <span className="flex-1 type-label">Đăng xuất</span>
            </a>
          </li>
          {/* Chỉ có khi mở cổng từ bản mẫu nhân viên (vai trò Người thân) */}
          {onExit && (
            <li className="border-t">
              <button type="button" onClick={onExit} className="flex min-h-12 w-full items-center gap-3 rounded-lg text-left type-caption text-muted-foreground">
                Bản mẫu: về bản mẫu nhân viên
              </button>
            </li>
          )}
        </ul>
      </PortalCard>
    </>
  )
}
