import { useState } from 'react'
import { AppShell } from '@/common/layout/AppShell'
import { NAV_GROUPS, navForRole } from '@/common/layout/navigation'
import { NotDesignedState, PermissionDeniedState } from '@/common/PageState'
import { DashboardPage } from '@/features/dashboard/DashboardPage'
import { ResidentListPage } from '@/features/residents/ResidentListPage'
import { ShiftChecklistPage } from '@/features/care-tasks/ShiftChecklistPage'
import { AlertsPage } from '@/features/alerts/AlertsPage'
import { AdminPage } from '@/features/admin/AdminPage'
import { ApprovalInbox } from '@/features/approval-requests/ApprovalInbox'
import { AuditLogTable } from '@/features/audit-log/AuditLogTable'
import { FamilyPortal } from '@/features/family-portal/FamilyPortal'
import { HandoverPage } from '@/features/handover/HandoverPage'
import { IncidentDetailPage } from '@/features/incidents/IncidentDetailPage'
import { MedicationPage } from '@/features/medication/MedicationPage'
import { RelativesPage } from '@/features/relatives/RelativesPage'

// Bản mẫu giai đoạn thiết kế: chuyển màn bằng state, React Router thêm ở 000 T005.
const PAGES = {
  dashboard: DashboardPage,
  residents: ResidentListPage,
  'care-tasks': ShiftChecklistPage,
  incidents: IncidentDetailPage,
  medication: MedicationPage,
  alerts: AlertsPage,
  handovers: HandoverPage,
  relatives: RelativesPage,
  'audit-log': AuditLogTable,
  admin: AdminPage,
  'approval-requests': ApprovalInbox,
}

function firstPage(role) {
  return navForRole(role)[0].items[0].key
}

function App() {
  const [role, setRole] = useState('FLOOR_MANAGER')
  const [page, setPage] = useState('dashboard')
  const [params, setParams] = useState({})

  function navigate(nextPage, nextParams = {}) {
    setPage(nextPage)
    setParams(nextParams)
  }

  function changeRole(nextRole) {
    setRole(nextRole)
    const allowed = navForRole(nextRole).flatMap((g) => g.items.map((i) => i.key))
    if (!allowed.includes(page)) navigate(firstPage(nextRole))
  }

  if (role === 'FAMILY_MEMBER') return <FamilyPortal onExit={() => changeRole('FLOOR_MANAGER')} />

  const Page = PAGES[page]
  const item = NAV_GROUPS.flatMap((g) => g.items).find((i) => i.key === page)
  // Chặn hiển thị khi vai trò không có quyền (quyền thật do backend trả, backend vẫn kiểm)
  const allowed = item?.roles.includes(role)

  return (
    <AppShell role={role} onRoleChange={changeRole} page={page} onPageChange={(key) => navigate(key)}>
      {!allowed ? (
        <PermissionDeniedState label={item?.label ?? 'màn này'} />
      ) : Page ? (
        <Page key={`${page}:${JSON.stringify(params)}`} role={role} onNavigate={navigate} {...params} />
      ) : (
        <NotDesignedState label={item.label} />
      )}
    </AppShell>
  )
}

export default App
