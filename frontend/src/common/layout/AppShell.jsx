import { useEffect, useState } from 'react'
import { ChevronDown, LogOut, Menu, Moon, Sun, UserRound, X } from 'lucide-react'
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible'
import { Sheet, SheetContent, SheetTitle } from '@/components/ui/sheet'
import { applyTheme, readTheme } from '@/common/theme'
import { cn } from '@/lib/utils'
import { ACCOUNT_PAGE, ROLES, groupOfPage, navForRole } from './navigation'
import { ICONS } from './navIcons'

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

const ICON_BUTTON =
  'flex size-10 shrink-0 items-center justify-center rounded-full text-sidebar-foreground hover:bg-(--sidebar-hover)'

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
      className={ICON_BUTTON}
    >
      {dark ? <Sun className="size-5" aria-hidden="true" /> : <Moon className="size-5" aria-hidden="true" />}
    </button>
  )
}

function RoleAvatar({ label, className }) {
  return (
    <span
      className={cn(
        'flex size-9 shrink-0 items-center justify-center rounded-full bg-(--avatar-1) type-caption text-(--avatar-1-foreground)',
        className,
      )}
      aria-hidden="true"
    >
      {initialsOf(label)}
    </span>
  )
}

// Khối người dùng cuối menu (DESIGN.md 5). Có phiên thật (002 T045) thì hiện họ tên, vai trò và nút Đăng xuất;
// không có thì ô chọn vai trò của bản mẫu. Hai hàng: tên đủ chiều rộng menu, không bị cắt (6.7.2); nút ở hàng dưới.
// Nút "Tài khoản của tôi" (bố cục duyệt 2026-11-09) cùng chỗ ở bản mẫu và phiên thật (DESIGN.md 9.3, WCAG 3.2.6).
function UserBlock({ role, onRoleChange, session, onLogout, accountOpen, onOpenAccount }) {
  const current = ROLES.find((r) => r.code === role)
  return (
    <div className="flex flex-col gap-1 border-t border-sidebar-border px-4 pt-3">
      <div className="flex items-start gap-3">
        <RoleAvatar label={current.label} />
        {session ? (
          <div className="flex min-w-0 flex-1 flex-col">
            <span className="type-label break-words text-sidebar-primary-foreground">
              {session.fullName ?? `Tài khoản ${session.accountId}`}
            </span>
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
      </div>
      <div className="flex justify-end gap-1">
        <button
          type="button"
          onClick={onOpenAccount}
          aria-label="Tài khoản của tôi"
          title="Tài khoản của tôi"
          aria-current={accountOpen ? 'page' : undefined}
          className={cn(
            ICON_BUTTON,
            accountOpen && 'bg-sidebar-accent text-sidebar-accent-foreground hover:bg-sidebar-accent',
          )}
        >
          <UserRound className="size-5" aria-hidden="true" />
        </button>
        <ThemeToggle />
        {session && (
          <button type="button" onClick={onLogout} aria-label="Đăng xuất" title="Đăng xuất" className={ICON_BUTTON}>
            <LogOut className="size-5" aria-hidden="true" />
          </button>
        )}
      </div>
    </div>
  )
}

// Nhóm đang mở của menu, nhớ theo trình duyệt; không đọc ghi được thì chỉ giữ trong phiên xem
const OPEN_GROUPS_KEY = 'elder-care-nav-groups'

function readOpenGroups() {
  try {
    const saved = JSON.parse(localStorage.getItem(OPEN_GROUPS_KEY))
    return Array.isArray(saved) ? saved : []
  } catch {
    return []
  }
}

function writeOpenGroups(ids) {
  try {
    localStorage.setItem(OPEN_GROUPS_KEY, JSON.stringify(ids))
  } catch {
    // bỏ qua: trình duyệt chặn lưu trữ
  }
}

function withGroup(ids, id) {
  return id && !ids.includes(id) ? [...ids, id] : ids
}

// Nhóm chứa trang đang mở thì mở sẵn (cả khi chuyển trang); người xem vẫn đóng được
function useOpenGroups(page) {
  const [state, setState] = useState(() => ({ page, ids: withGroup(readOpenGroups(), groupOfPage(page)) }))
  if (state.page !== page) setState({ page, ids: withGroup(state.ids, groupOfPage(page)) })

  function setOpen(id, open) {
    const ids = open ? withGroup(state.ids, id) : state.ids.filter((x) => x !== id)
    writeOpenGroups(ids)
    setState({ page, ids })
  }
  return [state.ids, setOpen]
}

function NavItem({ item, page, onSelect }) {
  const Icon = ICONS[item.icon]
  const active = item.key === page
  return (
    <button
      type="button"
      onClick={() => onSelect(item.key)}
      aria-current={active ? 'page' : undefined}
      className={cn(
        'flex min-h-10 shrink-0 items-center gap-2.5 rounded-menu px-3 text-left type-label text-sidebar-foreground hover:bg-(--sidebar-hover)',
        active &&
          'bg-sidebar-accent text-sidebar-accent-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground',
      )}
    >
      <Icon className="size-4 shrink-0" aria-hidden="true" />
      {item.label}
    </button>
  )
}

// Khối đầu không tên luôn hiện; nhóm nghiệp vụ là Collapsible (Base UI): nút có aria-expanded, aria-controls,
// Enter/Space bằng bàn phím; cao 44px (DESIGN.md 9.1)
function NavList({ groups, page, onSelect, openGroups, onGroupOpenChange }) {
  return (
    <nav aria-label="Menu chính" className="flex flex-1 flex-col gap-1 overflow-y-auto px-3">
      {groups.map((group) =>
        group.label ? (
          <Collapsible
            key={group.id}
            open={openGroups.includes(group.id)}
            onOpenChange={(open) => onGroupOpenChange(group.id, open)}
            className="flex flex-col gap-1"
          >
            <CollapsibleTrigger className="group mt-1 flex min-h-11 shrink-0 items-center gap-2 rounded-menu px-2.5 text-left type-label text-sidebar-muted-foreground hover:bg-(--sidebar-hover)">
              <span className="flex-1">{group.label}</span>
              <ChevronDown
                className="size-4 shrink-0 -rotate-90 transition-transform duration-150 group-data-[panel-open]:rotate-0 motion-reduce:transition-none"
                aria-hidden="true"
              />
            </CollapsibleTrigger>
            <CollapsibleContent className="flex flex-col gap-1">
              {group.items.map((item) => (
                <NavItem key={item.key} item={item} page={page} onSelect={onSelect} />
              ))}
            </CollapsibleContent>
          </Collapsible>
        ) : (
          <div key={group.id} className="flex flex-col gap-1">
            {group.items.map((item) => (
              <NavItem key={item.key} item={item} page={page} onSelect={onSelect} />
            ))}
          </div>
        ),
      )}
    </nav>
  )
}

const DESKTOP = '(min-width: 768px)'

export function AppShell({ role, hasFunction, built, onRoleChange, session, onLogout, page, onPageChange, children }) {
  const groups = navForRole(role, hasFunction, built)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [openGroups, setGroupOpen] = useOpenGroups(page)
  const pageLabel =
    page === ACCOUNT_PAGE
      ? 'Tài khoản của tôi'
      : (groups.flatMap((g) => g.items).find((item) => item.key === page)?.label ?? 'Elder_Care')
  const roleLabel = ROLES.find((r) => r.code === role).label

  // Rộng ra từ 768px thì menu trái hiện lại, ngăn kéo đóng
  useEffect(() => {
    const query = window.matchMedia?.(DESKTOP)
    if (!query) return undefined
    const close = () => query.matches && setDrawerOpen(false)
    query.addEventListener('change', close)
    return () => query.removeEventListener('change', close)
  }, [])

  const userBlock = (
    <UserBlock
      role={role}
      onRoleChange={onRoleChange}
      session={session}
      onLogout={onLogout}
      accountOpen={page === ACCOUNT_PAGE}
      onOpenAccount={() => {
        setDrawerOpen(false)
        onPageChange(ACCOUNT_PAGE)
      }}
    />
  )

  return (
    <div className="grid min-h-screen grid-cols-1 content-start md:grid-cols-[240px_minmax(0,1fr)]">
      {/* Bỏ qua menu (DESIGN.md 9.3, WCAG 2.4.1): ẩn, hiện khi focus bằng bàn phím */}
      <a
        href="#main-content"
        className="sr-only z-50 rounded-full bg-primary px-4 py-2 type-label text-primary-foreground focus:not-sr-only focus:fixed focus:top-3 focus:left-3"
      >
        Bỏ qua menu
      </a>

      {/* Thanh trên gọn dưới 768px (DESIGN.md 10): nút mở menu, tên màn, ảnh đại diện vai trò */}
      <header data-sidebar-surface className="sticky top-0 z-40 flex h-14 items-center gap-2 bg-sidebar px-2 text-sidebar-foreground md:hidden">
        <button
          type="button"
          onClick={() => setDrawerOpen(true)}
          aria-label="Mở menu"
          aria-expanded={drawerOpen}
          className={ICON_BUTTON}
        >
          <Menu className="size-5" aria-hidden="true" />
        </button>
        <span className="min-w-0 flex-1 truncate type-label text-sidebar-primary-foreground">{pageLabel}</span>
        <RoleAvatar label={roleLabel} className="mr-2 size-8" />
      </header>

      <Sheet open={drawerOpen} onOpenChange={setDrawerOpen}>
        <SheetContent
          side="left"
          showCloseButton={false}
          data-sidebar-surface
          className="w-[280px] max-w-[85vw] gap-2 border-sidebar-border bg-sidebar pb-3 text-sidebar-foreground sm:max-w-[280px]"
        >
          <div className="flex items-center gap-2.5 px-4 pt-3 pb-1">
            <LeafMark />
            <span className="flex-1 type-title text-sidebar-primary-foreground">Elder_Care</span>
            <SheetTitle className="sr-only">Menu</SheetTitle>
            <button type="button" onClick={() => setDrawerOpen(false)} aria-label="Đóng menu" className={ICON_BUTTON}>
              <X className="size-5" aria-hidden="true" />
            </button>
          </div>
          <NavList
            groups={groups}
            page={page}
            openGroups={openGroups}
            onGroupOpenChange={setGroupOpen}
            onSelect={(key) => {
              setDrawerOpen(false)
              onPageChange(key)
            }}
          />
          {userBlock}
        </SheetContent>
      </Sheet>

      <aside
        data-slot="sidebar"
        className="hidden flex-col gap-2 border-r border-sidebar-border bg-sidebar pb-3 text-sidebar-foreground md:sticky md:top-0 md:flex md:h-screen"
      >
        <div className="flex items-center gap-2.5 px-4 pt-4 pb-2">
          <LeafMark />
          <span className="type-title text-sidebar-primary-foreground">Elder_Care</span>
        </div>
        <NavList
          groups={groups}
          page={page}
          openGroups={openGroups}
          onGroupOpenChange={setGroupOpen}
          onSelect={onPageChange}
        />
        {userBlock}
      </aside>

      <main id="main-content" tabIndex={-1} className="min-w-0 px-4 py-6 outline-none md:px-8">
        {children}
      </main>
    </div>
  )
}
