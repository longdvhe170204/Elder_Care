import { useEffect, useState } from 'react'
import { ApiError, getMe, postAuthLogout } from '@/api'
import { AppShell } from '@/common/layout/AppShell'
import { ACCOUNT_PAGE, NAV_GROUPS, canSee, navForRole } from '@/common/layout/navigation'
import { PermissionsProvider, usePermissions } from '@/common/permissions/permissions'
import { PermissionDeniedState } from '@/common/PageState'
import { SessionGuard } from '@/common/SessionGuard'
import { homeOf, primaryRole } from '@/common/session'
import { DashboardPage } from '@/features/dashboard/DashboardPage'
import { MyAccountPage } from '@/features/permissions/MyAccountPage'
import { ResidentListPage } from '@/features/residents/ResidentListPage'
import { CareChecklistPage } from '@/features/care-tasks/CareChecklistPage'
import { OverdueTasksPage } from '@/features/care-tasks/OverdueTasksPage'
import { TimetablePage } from '@/features/care-tasks/TimetablePage'
import { CarePlanPage } from '@/features/care-tasks/CarePlanPage'
import { ShiftTaskPlanningPage } from '@/features/care-tasks/ShiftTaskPlanningPage'
import { DayCareAttendancePage } from '@/features/care-tasks/DayCareAttendancePage'
import { CareCatalogPage } from '@/features/care-tasks/CareCatalogPage'
import { ActivitySessionsPage } from '@/features/activities/ActivitySessionsPage'
import { NutritionCatalogPage } from '@/features/nutrition/catalog/NutritionCatalogPage'
import { ResidentDietPage } from '@/features/nutrition/resident/ResidentDietPage'
import { MealsPage } from '@/features/nutrition/meals/MealsPage'
import { QualityCheckPage } from '@/features/quality-checks/QualityCheckPage'
import { AlertsPage } from '@/features/alerts/AlertsPage'
import { AdminPage } from '@/features/admin/AdminPage'
import { ApprovalInbox } from '@/features/approval-requests/ApprovalInbox'
import { NotificationsPage } from '@/features/notifications/NotificationsPage'
import { PendingReviewPage } from '@/features/pending-review/PendingReviewPage'
import { AuditLogTable } from '@/features/audit-log/AuditLogTable'
import { FamilyPortal } from '@/features/family-portal/FamilyPortal'
import { PortalBelongings } from '@/features/belongings/PortalBelongings'
import { HandoverPage } from '@/features/handover/HandoverPage'
import { BalancePage } from '@/features/balance/BalancePage'
import { ResidenceRegistrationPage } from '@/features/residence-registration/ResidenceRegistrationPage'
import { IncidentsPage } from '@/features/incidents/IncidentsPage'
import { MedicationPage } from '@/features/medication/MedicationPage'
import { MedicationCatalogPage } from '@/features/medication-catalog/MedicationCatalogPage'
import { HealthCatalogPage } from '@/features/health-catalog/HealthCatalogPage'
import { MonitoringSettingsPage } from '@/features/measurement-schedules/MonitoringSettingsPage'
import { InfectionControlPage } from '@/features/infection-control/InfectionControlPage'
import { FacilityLicensePage } from '@/features/facility-license/FacilityLicensePage'
import { RelativesPage } from '@/features/relatives/RelativesPage'
import { FamilyMembersPage } from '@/features/relatives/FamilyMembersPage'
import { PickupPage } from '@/features/relatives/pickups/PickupPage'
import { VisitsPage } from '@/features/relatives/visits/VisitsPage'
import { FeedbackPage } from '@/features/relatives/feedback/FeedbackPage'
import { FamilyStaysPage } from '@/features/relatives/stays/FamilyStaysPage'
import { ApplicationsPage } from '@/features/admissions/ApplicationsPage'
import { WaitingListPage } from '@/features/admissions/WaitingListPage'
import { ContractsPage } from '@/features/admissions/ContractsPage'
import { ChangeRequestsPage } from '@/features/admissions/ChangeRequestsPage'
import { AbsencesPage } from '@/features/admissions/AbsencesPage'
import { EndingsPage } from '@/features/admissions/EndingsPage'
import { DeathsPage } from '@/features/admissions/DeathsPage'
import { ServicesPage } from '@/features/admissions/ServicesPage'
import { ShiftsPage } from '@/features/shifts/ShiftsPage'
import { ShiftSwapsPage } from '@/features/shift-swaps/ShiftSwapsPage'
import { StaffPage } from '@/features/shifts/StaffPage'
import { BedsPage } from '@/features/beds/BedsPage'
import { ReportsPage } from '@/features/reports/ReportsPage'
import { ChargesPage } from '@/features/charges/ChargesPage'
import { BelongingsPage } from '@/features/belongings/BelongingsPage'
import { ProcurementRequestsPage } from '@/features/inventory/ProcurementRequestsPage'
import { AssetsPage } from '@/features/inventory/AssetsPage'
import { VehicleSchedulesPage } from '@/features/inventory/VehicleSchedulesPage'
import { SuppliedMedicationIntakePage } from '@/features/medication/SuppliedMedicationIntakePage'

// Bản mẫu giai đoạn thiết kế: chuyển màn bằng state, React Router thêm ở 000 T005.
const PAGES = {
  procurement: ProcurementRequestsPage,
  'medication-receipt': SuppliedMedicationIntakePage,
  assets: AssetsPage,
  vehicles: VehicleSchedulesPage,
  charges: ChargesPage,
  beds: BedsPage,
  dashboard: DashboardPage,
  reports: ReportsPage,
  residents: ResidentListPage,
  shifts: ShiftsPage,
  'shift-swaps': ShiftSwapsPage,
  staff: StaffPage,
  'care-tasks': CareChecklistPage,
  'overdue-care-tasks': OverdueTasksPage,
  'care-timetable': TimetablePage,
  'care-plans': CarePlanPage,
  'care-task-planning': ShiftTaskPlanningPage,
  'day-care-attendance': DayCareAttendancePage,
  'care-catalogs': CareCatalogPage,
  'activity-sessions': ActivitySessionsPage,
  nutrition: NutritionCatalogPage,
  'resident-diets': ResidentDietPage,
  meals: MealsPage,
  'quality-checks': QualityCheckPage,
  incidents: IncidentsPage,
  medication: MedicationPage,
  'medication-catalog': MedicationCatalogPage,
  'health-catalog': HealthCatalogPage,
  'measurement-schedules': MonitoringSettingsPage,
  'infection-control': InfectionControlPage,
  'facility-license': FacilityLicensePage,
  alerts: AlertsPage,
  handovers: HandoverPage,
  'residence-registrations': ResidenceRegistrationPage,
  balance: BalancePage,
  relatives: RelativesPage,
  'family-members': FamilyMembersPage,
  pickups: PickupPage,
  visits: VisitsPage,
  feedback: FeedbackPage,
  'family-stays': FamilyStaysPage,
  'admission-applications': ApplicationsPage,
  'waiting-list': WaitingListPage,
  contracts: ContractsPage,
  'residency-changes': ChangeRequestsPage,
  absences: AbsencesPage,
  'residency-endings': EndingsPage,
  deaths: DeathsPage,
  services: ServicesPage,
  'audit-log': AuditLogTable,
  admin: AdminPage,
  'approval-requests': ApprovalInbox,
  notifications: NotificationsPage,
  'pending-review-records': PendingReviewPage,
  belongings: BelongingsPage,
  [ACCOUNT_PAGE]: MyAccountPage,
}

// Mục menu chỉ hiện khi đã có màn
const BUILT = new Set(Object.keys(PAGES))

// Màn đang mở giữ ở hash của URL để tải lại trang (F5) không về Tổng quan (BUG-UI-002); hash lạ thì về màn mặc định.
// Tham số id đi sau dấu ? (`#incidents?incidentId=9`, 016 Q12): mở thẳng bản ghi ở màn của feature sở hữu.
function pageFromUrl() {
  const key = window.location.hash.slice(1).split('?')[0]
  return Object.hasOwn(PAGES, key) ? key : 'dashboard'
}

function paramsFromUrl() {
  const [key, query] = window.location.hash.slice(1).split('?')
  if (!query || !Object.hasOwn(PAGES, key)) return {}
  return Object.fromEntries(
    [...new URLSearchParams(query)].map(([name, value]) => [name, /^\d+$/.test(value) ? Number(value) : value]),
  )
}

function hashOf(page, params) {
  const query = new URLSearchParams(params).toString()
  return query ? `#${page}?${query}` : `#${page}`
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
  const misplaced = status !== 'loading' && page !== ACCOUNT_PAGE && (!item || !canSee(item, role, check, BUILT))
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
  const [params, setParams] = useState(paramsFromUrl)
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
    window.history.replaceState(null, '', hashOf(nextPage, nextParams))
  }

  function changeRole(nextRole) {
    setRole(nextRole)
    const allowed = navForRole(nextRole, null, BUILT).flatMap((g) => g.items.map((i) => i.key))
    if (page !== ACCOUNT_PAGE && !allowed.includes(page)) navigate(firstPage(nextRole))
  }

  if (checking) return null
  if (role === 'FAMILY_MEMBER')
    return <FamilyPortal onExit={() => changeRole('FLOOR_MANAGER')} belongings={PortalBelongings} />

  const Page = PAGES[page]
  const item = NAV_GROUPS.flatMap((g) => g.items).find((i) => i.key === page)

  const shell = (hasFunction) => {
    // Chặn hiển thị khi không có quyền (quyền thật do backend trả, backend vẫn kiểm)
    const allowed = page === ACCOUNT_PAGE || (item && canSee(item, role, hasFunction, BUILT))
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
          <Page
            key={`${page}:${JSON.stringify(params)}`}
            role={role}
            onNavigate={navigate}
            {...(page === ACCOUNT_PAGE ? { session, built: BUILT } : {})}
            {...params}
          />
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
