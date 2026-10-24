import { useState } from 'react'
import {
  ArrowLeftRight,
  BedDouble,
  Bell,
  CalendarCheck,
  CalendarDays,
  ChartColumn,
  ClipboardCheck,
  FileSignature,
  HeartHandshake,
  History,
  Inbox,
  LayoutDashboard,
  ListChecks,
  LogOut,
  Moon,
  Pill,
  Receipt,
  Settings,
  Sun,
  TriangleAlert,
  Users,
  Utensils,
} from 'lucide-react'
import { applyTheme, readTheme } from '@/common/theme'
import { cn } from '@/lib/utils'
import { ROLES, navForRole } from './navigation'

const ICONS = {
  ArrowLeftRight,
  BedDouble,
  Bell,
  CalendarCheck,
  CalendarDays,
  ChartColumn,
  ClipboardCheck,
  FileSignature,
  HeartHandshake,
  History,
  Inbox,
  LayoutDashboard,
  ListChecks,
  Pill,
  Receipt,
  Settings,
  TriangleAlert,
  Users,
  Utensils,
}

// Dấu lá giống favicon
export function LeafMark() {
  return (
    <svg viewBox="0 0 32 32" className="size-7 shrink-0" aria-hidden="true">
      <rect width="32" height="32" rx="8" className="fill-sidebar-primary" />
      <path d="M23 8C14 8 9 13 9 22v2h2c9 0 14-5 14-14V8h-2Z" className="fill-sidebar-primary-foreground" />
      <path d="M10.5 22.5 20 13" strokeWidth="1.6" strokeLinecap="round" className="stroke-sidebar-primary" />
    </svg>
  )
}

function initialsOf(label) {
  return label
    .split(' ')
    .map((w) => w.charAt(0))
    .join('')
    .slice(0, 2)
    .toUpperCase()
}

// Nút đổi sáng/tối (DESIGN.md mục Màu): mặc định sáng, nhớ theo trình duyệt
function ThemeToggle() {
  const [theme, setTheme] = useState(readTheme)
  const dark = theme === 'dark'

  function toggle() {
    const next = dark ? 'light' : 'dark'
    applyTheme(next)
    setTheme(next)
  }

  return (
    <button
      type="button"
      onClick={toggle}
      aria-pressed={dark}
      aria-label={dark ? 'Chuyển sang chế độ sáng' : 'Chuyển sang chế độ tối'}
      title={dark ? 'Chế độ sáng' : 'Chế độ tối'}
      className="flex size-10 shrink-0 items-center justify-center rounded-full text-sidebar-foreground hover:bg-(--sidebar-hover)"
    >
      {dark ? <Sun className="size-5" aria-hidden="true" /> : <Moon className="size-5" aria-hidden="true" />}
    </button>
  )
}

// Khối người dùng cuối menu (DESIGN.md 2.1.0). Có phiên thật (002 T045) thì hiện họ tên, vai trò và nút Đăng xuất;
// không có thì ô chọn vai trò của bản mẫu.
function UserBlock({ role, onRoleChange, session, onLogout }) {
  const current = ROLES.find((r) => r.code === role)
  return (
    <div className="flex items-center gap-3 border-t border-sidebar-border px-4 pt-3 md:mt-auto">
      <span
        className="flex size-9 shrink-0 items-center justify-center rounded-full bg-(--avatar-1) type-caption text-(--avatar-1-foreground)"
        aria-hidden="true"
      >
        {initialsOf(current.label)}
      </span>
      {session ? (
        <div className="flex min-w-0 flex-1 flex-col">
          <span className="truncate type-label">{session.fullName ?? `Tài khoản ${session.accountId}`}</span>
          <span className="type-caption text-sidebar-muted-foreground">{current.label}</span>
        </div>
      ) : (
      <div className="flex min-w-0 flex-1 flex-col">
        <label htmlFor="demo-role" className="sr-only">
          Xem với vai trò
        </label>
        <select
          id="demo-role"
          value={role}
          onChange={(event) => onRoleChange(event.target.value)}
          className="h-10 w-full rounded-md border border-transparent bg-transparent type-label text-sidebar-foreground hover:border-sidebar-border [&>option]:text-foreground"
        >
          {ROLES.map((r) => (
            <option key={r.code} value={r.code}>
              {r.label}
            </option>
          ))}
        </select>
        <span className="type-caption text-sidebar-muted-foreground">Bản mẫu, dữ liệu minh họa</span>
      </div>
      )}
      <ThemeToggle />
      {session && (
        <button
          type="button"
          onClick={onLogout}
          aria-label="Đăng xuất"
          className="flex size-10 shrink-0 items-center justify-center rounded-full text-sidebar-foreground hover:bg-(--sidebar-hover)"
        >
          <LogOut className="size-5" aria-hidden="true" />
        </button>
      )}
    </div>
  )
}

export function AppShell({ role, onRoleChange, session, onLogout, page, onPageChange, children }) {
  const groups = navForRole(role)

  return (
    <div className="grid min-h-screen grid-cols-1 md:grid-cols-[240px_minmax(0,1fr)]">
      <aside
        data-slot="sidebar"
        className="flex flex-col gap-2 border-b border-sidebar-border bg-sidebar bg-linear-to-b/srgb from-(--sidebar-from) via-(--sidebar-mid) via-58% to-(--sidebar-to) pb-3 text-sidebar-foreground md:sticky md:top-0 md:h-screen md:border-r md:border-b-0"
      >
        <div className="flex items-center gap-2.5 px-4 pt-4 pb-2">
          <LeafMark />
          <span className="type-title">Elder_Care</span>
        </div>
        <nav className="flex gap-1 overflow-x-auto px-3 md:flex-col md:overflow-y-auto">
          {groups.map((group) => (
            <div key={group.label ?? 'main'} className="flex gap-1 md:flex-col">
              {group.label && (
                <div className="hidden px-2.5 pt-4 pb-1 type-caption text-sidebar-muted-foreground md:block">{group.label}</div>
              )}
              {group.items.map((item) => {
                const Icon = ICONS[item.icon]
                const active = item.key === page
                return (
                  <button
                    key={item.key}
                    type="button"
                    onClick={() => onPageChange(item.key)}
                    aria-current={active ? 'page' : undefined}
                    className={cn(
                      'flex min-h-10 shrink-0 items-center gap-2.5 rounded-full px-3 text-left type-label whitespace-nowrap text-sidebar-foreground hover:bg-(--sidebar-hover)',
                      active &&
                        'bg-sidebar-accent text-sidebar-accent-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground',
                    )}
                  >
                    <Icon className="size-4" aria-hidden="true" />
                    {item.label}
                  </button>
                )
              })}
            </div>
          ))}
        </nav>
        <UserBlock role={role} onRoleChange={onRoleChange} session={session} onLogout={onLogout} />
      </aside>

      <main className="min-w-0 px-4 py-6 md:px-8">{children}</main>
    </div>
  )
}
