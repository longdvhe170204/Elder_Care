import { useEffect, useState } from 'react'
import { ApiError, getMe, postAuthLogout } from '@/api'
import { AppShell } from '@/common/layout/AppShell'
import { NAV_GROUPS, canSee, navForRole } from '@/common/layout/navigation'
import { PermissionsProvider, usePermissions } from '@/common/permissions/permissions'
import { PermissionDeniedState } from '@/common/PageState'
import { SessionGuard } from '@/common/SessionGuard'
import { homeOf, primaryRole } from '@/common/session'
import { DashboardPage } from '@/features/dashboard/DashboardPage'
import { ResidentListPage } from '@/features/residents/ResidentListPage'
import { CareChecklistPage } from '@/features/care-tasks/CareChecklistPage'
import { OverdueTasksPage } from '@/features/care-tasks/OverdueTasksPage'
import { CarePlanPage } from '@/features/care-tasks/CarePlanPage'
import { ShiftTaskPlanningPage } from '@/features/care-tasks/ShiftTaskPlanningPage'
import { DayCareAttendancePage } from '@/features/care-tasks/DayCareAttendancePage'
import { CareCatalogPage } from '@/features/care-tasks/CareCatalogPage'
import { AlertsPage } from '@/features/alerts/AlertsPage'
import { AdminPage } from '@/features/admin/AdminPage'
import { ApprovalInbox } from '@/features/approval-requests/ApprovalInbox'
import { NotificationsPage } from '@/features/notifications/NotificationsPage'
import { PendingReviewPage } from '@/features/pending-review/PendingReviewPage'
import { AuditLogTable } from '@/features/audit-log/AuditLogTable'
import { FamilyPortal } from '@/features/family-portal/FamilyPortal'
import { HandoverPage } from '@/features/handover/HandoverPage'
import { IncidentDetailPage } from '@/features/incidents/IncidentDetailPage'
import { MedicationPage } from '@/features/medication/MedicationPage'
import { MedicationCatalogPage } from '@/features/medication-catalog/MedicationCatalogPage'
import { HealthCatalogPage } from '@/features/health-catalog/HealthCatalogPage'
import { RelativesPage } from '@/features/relatives/RelativesPage'
import { FamilyMembersPage } from '@/features/relatives/FamilyMembersPage'
import { ShiftsPage } from '@/features/shifts/ShiftsPage'
import { StaffPage } from '@/features/shifts/StaffPage'
import { BedsPage } from '@/features/beds/BedsPage'
import { ReportsPage } from '@/features/reports/ReportsPage'
import { BelongingsPage } from '@/features/belongings/BelongingsPage'

// Bản mẫu giai đoạn thiết kế: chuyển màn bằng state, React Router thêm ở 000 T005.
const PAGES = {
  beds: BedsPage,
  dashboard: DashboardPage,
  reports: ReportsPage,
  residents: ResidentListPage,
  shifts: ShiftsPage,
  staff: StaffPage,
  'care-tasks': CareChecklistPage,
  'overdue-care-tasks': OverdueTasksPage,
  'care-plans': CarePlanPage,
  'care-task-planning': ShiftTaskPlanningPage,
  'day-care-attendance': DayCareAttendancePage,
  'care-catalogs': CareCatalogPage,
  incidents: IncidentDetailPage,
  medication: MedicationPage,
  'medication-catalog': MedicationCatalogPage,
  'health-catalog': HealthCatalogPage,
  alerts: AlertsPage,
  handovers: HandoverPage,
  relatives: RelativesPage,
  'family-members': FamilyMembersPage,
  'audit-log': AuditLogTable,
  admin: AdminPage,
  'approval-requests': ApprovalInbox,
  notifications: NotificationsPage,
  'pending-review-records': PendingReviewPage,
  belongings: BelongingsPage,
}

// Mục menu chỉ hiện khi đã có màn
const BUILT = new Set(Object.keys(PAGES))

// Màn đang mở giữ ở hash của URL để tải lại trang (F5) không về Tổng quan (BUG-UI-002); hash lạ thì về màn mặc định.
function pageFromUrl() {
  const key = window.location.hash.slice(1)
  return Object.hasOwn(PAGES, key) ? key : 'dashboard'
}

function firstPage(role, hasFunction = null) {
  return navForRole(role, hasFunction, BUILT)[0]?.items[0].key
}

// Có phiên thật: menu và quyền mở màn theo GET /me/permissions; chưa tải xong thì chưa vẽ để menu không nháy, lỗi thì
// lùi về lọc theo vai trò. Màn đang mở không được phép (mặc định Tổng quan) thì sang mục đầu tiên được phép.
function LivePermissions({ role, page, onNavigate, render }) {
  const { status, hasFunction } = usePermissions()
  const check = status === 'ready' ? hasFunction : null
  const item = NAV_GROUPS.flatMap((g) => g.items).find((i) => i.key === page)
  const misplaced = status !== 'loading' && (!item || !canSee(item, role, check, BUILT))
  const first = misplaced ? firstPage(role, check) : null

  useEffect(() => {
    if (first && first !== page) onNavigate(first)
  }, [first, page, onNavigate])

  if (status === 'loading') return null
  return render(check)
}

function App() {
  const [role, setRole] = useState('FLOOR_MANAGER')
  const [page, setPage] = useState(pageFromUrl)
  const [params, setParams] = useState({})
  // Phiên thật (002 T045): có thì vai trò theo tài khoản; chưa đăng nhập thì sang /login; không có backend thì bản
  // mẫu đổi vai trò
  const [session, setSession] = useState(null)
  // Chưa biết có phiên hay không thì chưa vẽ khung, tránh nháy bản mẫu trước khi sang /login
  const [checking, setChecking] = useState(true)

  useEffect(() => {
    let cancelled = false
    getMe()
      .then(({ data }) => {
        if (cancelled) return
        const home = homeOf(data)
        if (home !== '/') {
          window.location.assign(home)
          return
        }
        setSession(data)
        const real = primaryRole(data.roles)
        if (real) setRole(real)
        setChecking(false)
      })
      .catch((error) => {
        if (cancelled) return
        // Backend trả 401 (chưa đăng nhập) thì vào màn đăng nhập; không gọi được backend thì giữ bản mẫu
        if (error instanceof ApiError && error.status === 401) window.location.assign('/login')
        else setChecking(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  async function logout() {
    try {
      await postAuthLogout()
    } finally {
      window.location.assign('/login')
    }
  }

  function navigate(nextPage, nextParams = {}) {
    setPage(nextPage)
    setParams(nextParams)
    window.history.replaceState(null, '', `#${nextPage}`)
  }

  function changeRole(nextRole) {
    setRole(nextRole)
    const allowed = navForRole(nextRole, null, BUILT).flatMap((g) => g.items.map((i) => i.key))
    if (!allowed.includes(page)) navigate(firstPage(nextRole))
  }

  if (checking) return null
  if (role === 'FAMILY_MEMBER') return <FamilyPortal onExit={() => changeRole('FLOOR_MANAGER')} />

  const Page = PAGES[page]
  const item = NAV_GROUPS.flatMap((g) => g.items).find((i) => i.key === page)

  const shell = (hasFunction) => {
    // Chặn hiển thị khi không có quyền (quyền thật do backend trả, backend vẫn kiểm)
    const allowed = item && canSee(item, role, hasFunction, BUILT)
    return (
      <AppShell
        role={role}
        hasFunction={hasFunction}
        built={BUILT}
        onRoleChange={changeRole}
        session={session}
        onLogout={logout}
        page={page}
        onPageChange={(key) => navigate(key)}
      >
        {allowed ? (
          <Page key={`${page}:${JSON.stringify(params)}`} role={role} onNavigate={navigate} {...params} />
        ) : (
          <PermissionDeniedState label={item?.label ?? 'màn này'} />
        )}
      </AppShell>
    )
  }

  if (!session) return shell(null)
  // Báo trước khi hết phiên, đăng nhập lại tại chỗ giữ phần đang nhập (002 T047b)
  return (
    <SessionGuard accountId={session.accountId} timeoutSeconds={session.sessionIdleTimeoutSeconds} onLogout={logout}>
      <PermissionsProvider>
        <LivePermissions role={role} page={page} onNavigate={navigate} render={shell} />
      </PermissionsProvider>
    </SessionGuard>
  )
}

export default App
