import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { PageHeader } from '@/common/PageHeader'
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
      <div role="tablist" aria-label="Loại yêu cầu" className="flex gap-2">
        <Button
          type="button"
          role="tab"
          aria-selected={tab === 'swap'}
          variant={tab === 'swap' ? 'default' : 'outline'}
          onClick={() => setTab('swap')}
        >
          Đổi ca
        </Button>
        <Button
          type="button"
          role="tab"
          aria-selected={tab === 'leave'}
          variant={tab === 'leave' ? 'default' : 'outline'}
          onClick={() => setTab('leave')}
        >
          Nghỉ đột xuất
        </Button>
      </div>
      {tab === 'swap' ? <SwapPanel /> : <LeavePanel />}
    </div>
  )
}
