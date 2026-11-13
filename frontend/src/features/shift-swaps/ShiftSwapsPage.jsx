import { useState } from 'react'
import { PageHeader } from '@/common/PageHeader'
import { SectionTabs } from '@/common/SectionTabs'
import { PermissionDeniedState } from '@/common/PageState'
import { PermissionsProvider, usePermissions } from '@/common/permissions/permissions'
import { LeavePanel } from './LeavePanel'
import { SwapPanel } from './SwapPanel'

// 015: đổi ca và nghỉ đột xuất (UC-54). Hai tab; mọi lệnh chỉ trực tuyến (FR-043).
export function ShiftSwapsPage() {
  return (
    <PermissionsProvider>
      <Workspace />
    </PermissionsProvider>
  )
}

function Workspace() {
  const { status, hasFunction } = usePermissions()
  const [tab, setTab] = useState('swap')
  if (status === 'loading') return <p role="status">Đang tải…</p>
  if (status === 'error' || !hasFunction('SHIFT_SWAP_REQUEST')) return <PermissionDeniedState />
  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Đổi ca, nghỉ đột xuất"
        subtitle="Yêu cầu đổi ca và nghỉ đột xuất; chỉ thực hiện khi có mạng."
      />
      <SectionTabs
        label="Loại yêu cầu"
        value={tab}
        onChange={setTab}
        tabs={[
          ['swap', 'Đổi ca'],
          ['leave', 'Nghỉ đột xuất'],
        ]}
      />
      {tab === 'swap' ? <SwapPanel /> : <LeavePanel />}
    </div>
  )
}
