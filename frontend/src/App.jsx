import { useState } from 'react'
import { AppShell } from '@/common/layout/AppShell'
import { navForRole } from '@/common/layout/navigation'
import { DashboardPage } from '@/features/dashboard/DashboardPage'
import { ResidentListPage } from '@/features/residents/ResidentListPage'

// Bản mẫu giai đoạn thiết kế: chuyển màn bằng state, React Router thêm ở 000 T005.
const PAGES = {
  dashboard: DashboardPage,
  residents: ResidentListPage,
}

function firstPage(role) {
  return navForRole(role)[0].items[0].key
}

function App() {
  const [role, setRole] = useState('FLOOR_MANAGER')
  const [page, setPage] = useState('dashboard')

  function changeRole(nextRole) {
    setRole(nextRole)
    const allowed = navForRole(nextRole).flatMap((g) => g.items.map((i) => i.key))
    if (!allowed.includes(page)) setPage(firstPage(nextRole))
  }

  const Page = PAGES[page]

  return (
    <AppShell role={role} onRoleChange={changeRole} page={page} onPageChange={setPage}>
      {Page ? (
        <Page />
      ) : (
        <p className="text-sm text-muted-foreground">Màn này chưa có bản mẫu trong giai đoạn thiết kế.</p>
      )}
    </AppShell>
  )
}

export default App
