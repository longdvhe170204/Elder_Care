import { useState } from 'react'
import { Bell, CalendarCheck, House, LogOut, Menu, Newspaper, UserRound } from 'lucide-react'
import { LeafMark } from '@/common/layout/AppShell'
import { DEMO_PORTAL_CONFIRMATIONS, DEMO_PORTAL_NOTIFICATIONS, DEMO_PORTAL_VIEWER } from '@/common/demoData'
import { cn } from '@/lib/utils'
import { AccountPage } from './AccountPage'
import { BulletinPage } from './BulletinPage'
import { ContractPage } from './ContractPage'
import { CostsPage } from './CostsPage'
import { DepositsPage } from './DepositsPage'
import { FeedbackPage } from './FeedbackPage'
import { HealthPage } from './HealthPage'
import { HomePage } from './HomePage'
import { MorePage } from './MorePage'
import { NotificationsPage } from './NotificationsPage'
import { TodayPage } from './TodayPage'
import { VisitsPage } from './VisitsPage'

// Cổng người thân, bản web (DESIGN.md 2.3): đăng nhập xong vào Trang chủ, là trang giới thiệu viện; các màn khác
// đi từ đầu trang (máy tính) hoặc thanh dưới (điện thoại). Đầu trang cùng kiểu với trang giới thiệu: dải nền trời,
// logo, các mục, người đăng nhập. App mobile làm sau theo DESIGN.md 15.

const NAV = [
  { key: 'home', label: 'Trang chủ', icon: House },
  { key: 'today', label: 'Hôm nay', icon: UserRound },
  { key: 'bulletins', label: 'Bản tin', icon: Newspaper },
  { key: 'visits', label: 'Thăm', icon: CalendarCheck },
  { key: 'notifications', label: 'Thông báo', icon: Bell },
  { key: 'more', label: 'Thêm', icon: Menu },
]
// Thanh dưới trên điện thoại tối đa 5 mục (DESIGN.md 15.4); Thông báo nằm ở đầu trang
const BOTTOM_NAV = NAV.filter((n) => n.key !== 'notifications')

const PAGES = {
  home: HomePage,
  today: TodayPage,
  bulletins: BulletinPage,
  visits: VisitsPage,
  notifications: NotificationsPage,
  more: MorePage,
  health: HealthPage,
  costs: CostsPage,
  feedback: FeedbackPage,
  deposits: DepositsPage,
  contract: ContractPage,
  account: AccountPage,
}

// Số việc chờ người xem: thông báo cần xác nhận đã nhận, việc chờ người đại diện quyết định
function pendingCount(viewer) {
  const acks = DEMO_PORTAL_NOTIFICATIONS.filter((n) => n.needsAck).length
  return acks + (viewer.representative ? DEMO_PORTAL_CONFIRMATIONS.length : 0)
}

function Badge({ count }) {
  if (!count) return null
  return (
    <span
      aria-hidden="true"
      className="absolute -top-1 -right-1 flex min-w-4 items-center justify-center rounded-full bg-primary px-1 type-caption leading-4 text-primary-foreground tabular-nums"
    >
      {count}
    </span>
  )
}

function Header({ viewer, page, badge, onNavigate }) {
  return (
    <header className="sticky top-0 z-10 border-b border-foreground/10 bg-landing-sky px-4 sm:px-8">
      <div className="mx-auto flex h-16 max-w-[1120px] items-center gap-4">
        <button type="button" onClick={() => onNavigate('home')} className="flex items-center gap-2.5 rounded-lg">
          <LeafMark />
          <span className="type-title">Elder_Care</span>
        </button>
        <nav aria-label="Cổng người thân" className="ml-auto hidden lg:block">
          <ul className="flex gap-1">
            {NAV.map((item) => (
              <li key={item.key}>
                <button
                  type="button"
                  onClick={() => onNavigate(item.key)}
                  aria-current={page === item.key ? 'page' : undefined}
                  className={cn(
                    'relative inline-flex h-10 items-center rounded-full px-3.5 type-label hover:bg-card/40',
                    page === item.key && 'bg-primary text-primary-foreground hover:bg-primary',
                  )}
                >
                  {item.label}
                  {item.key === 'notifications' && <Badge count={badge} />}
                  {item.key === 'notifications' && badge > 0 && <span className="sr-only">, {badge} việc chờ bạn</span>}
                </button>
              </li>
            ))}
          </ul>
        </nav>
        <div className="ml-auto flex items-center gap-1 lg:ml-2">
          <button
            type="button"
            onClick={() => onNavigate('notifications')}
            aria-label={`Thông báo${badge ? `, ${badge} việc chờ bạn` : ''}`}
            aria-current={page === 'notifications' ? 'page' : undefined}
            className="relative flex size-11 items-center justify-center rounded-full hover:bg-card/40 lg:hidden"
          >
            <Bell className="size-5" aria-hidden="true" />
            <Badge count={badge} />
          </button>
          <span className="hidden type-caption sm:inline">{viewer.name}</span>
          <a href="/dang-nhap" aria-label="Đăng xuất" className="flex size-11 items-center justify-center rounded-full hover:bg-card/40">
            <LogOut className="size-5" aria-hidden="true" />
          </a>
        </div>
      </div>
    </header>
  )
}

export function FamilyPortal({ onExit }) {
  const [page, setPage] = useState('home')
  const viewer = DEMO_PORTAL_VIEWER
  const Page = PAGES[page]
  const badge = pendingCount(viewer)
  const activeBottom = BOTTOM_NAV.some((n) => n.key === page) ? page : page === 'notifications' ? null : 'more'

  function navigate(next) {
    setPage(next)
    window.scrollTo(0, 0)
  }

  return (
    <div className="min-h-screen pb-20 lg:pb-0">
      <Header viewer={viewer} page={page} badge={badge} onNavigate={navigate} />

      {page === 'home' ? (
        <HomePage onNavigate={navigate} />
      ) : (
        <main className="mx-auto flex max-w-[960px] flex-col gap-4 px-4 py-6 sm:px-8 md:py-8">
          <Page viewer={viewer} onNavigate={navigate} onExit={onExit} />
        </main>
      )}

      <nav aria-label="Cổng người thân" className="fixed inset-x-0 bottom-0 flex border-t bg-card pb-[env(safe-area-inset-bottom)] lg:hidden">
        {BOTTOM_NAV.map((item) => {
          const Icon = item.icon
          const active = activeBottom === item.key
          return (
            <button
              key={item.key}
              type="button"
              onClick={() => navigate(item.key)}
              aria-current={active ? 'page' : undefined}
              className={cn('flex min-h-14 flex-1 flex-col items-center justify-center gap-0.5 type-caption text-muted-foreground', active && 'text-primary')}
            >
              <span className={cn('flex h-7 w-12 items-center justify-center rounded-full', active && 'bg-accent')}>
                <Icon className="size-5" aria-hidden="true" />
              </span>
              {item.label}
            </button>
          )
        })}
      </nav>
    </div>
  )
}
