import {
  ArrowLeftRight,
  BedDouble,
  Bell,
  Building2,
  CalendarCheck,
  CalendarDays,
  FileSignature,
  History,
  LayoutDashboard,
  ListChecks,
  Pill,
  Receipt,
  Settings,
  TriangleAlert,
  Users,
  Utensils,
} from 'lucide-react'
import { cn } from '@/lib/utils'
import { ROLES, navForRole } from './navigation'

const ICONS = {
  ArrowLeftRight,
  BedDouble,
  Bell,
  CalendarCheck,
  CalendarDays,
  FileSignature,
  History,
  LayoutDashboard,
  ListChecks,
  Pill,
  Receipt,
  Settings,
  TriangleAlert,
  Users,
  Utensils,
}

export function AppShell({ role, onRoleChange, page, onPageChange, children }) {
  const groups = navForRole(role)

  return (
    <div className="grid min-h-screen grid-cols-1 md:grid-cols-[232px_minmax(0,1fr)]">
      <aside className="border-b border-sidebar-border bg-sidebar text-sidebar-foreground md:border-r md:border-b-0">
        <div className="flex items-center gap-2 px-4 py-4 font-medium">
          <Building2 className="size-5 text-primary" aria-hidden="true" />
          Elder_Care
        </div>
        <nav className="flex gap-1 overflow-x-auto px-2 pb-3 md:flex-col md:overflow-visible">
          {groups.map((group) => (
            <div key={group.label ?? 'main'} className="flex gap-1 md:flex-col">
              {group.label && (
                <div className="hidden px-2 pt-4 pb-1 text-xs text-muted-foreground md:block">{group.label}</div>
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
                      'flex shrink-0 items-center gap-2 rounded-md px-2 py-1.5 text-left text-sm whitespace-nowrap text-muted-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground',
                      active && 'bg-sidebar-accent font-medium text-sidebar-accent-foreground',
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
      </aside>

      <div className="flex min-w-0 flex-col">
        <header className="flex items-center justify-between gap-4 border-b bg-card px-4 py-2 md:px-6">
          <span className="hidden text-xs text-muted-foreground sm:inline">Bản mẫu thiết kế · dữ liệu minh họa</span>
          <label className="ml-auto flex items-center gap-2 text-sm whitespace-nowrap text-muted-foreground">
            Xem với vai trò
            <select
              value={role}
              onChange={(event) => onRoleChange(event.target.value)}
              className="h-8 rounded-md border border-input bg-background px-2 text-sm text-foreground"
            >
              {ROLES.map((r) => (
                <option key={r.code} value={r.code}>
                  {r.label}
                </option>
              ))}
            </select>
          </label>
        </header>
        <main className="min-w-0 flex-1 px-4 py-5 md:px-6">{children}</main>
      </div>
    </div>
  )
}
