import { useState } from 'react'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { PageHeader } from '@/common/PageHeader'
import { NotDesignedState } from '@/common/PageState'
import { ParameterList } from '@/features/parameters/ParameterList'

// Màn "Tài khoản, tham số" (Phụ lục 27, dòng "Tài khoản, phân quyền, tham số"). Tab Tài khoản làm ở 002.
// Chỉ Quản lý viện đổi tham số (BR-M15-04); quyền thật do backend trả và kiểm.

export function AdminPage({ role }) {
  const [tab, setTab] = useState('parameters')
  return (
    <div className="mx-auto flex w-full max-w-[1600px] flex-col gap-6">
      <PageHeader
        title="Tài khoản, tham số"
        subtitle="Tham số vận hành có hiệu lực ngay khi lưu; mỗi lần đổi ghi lý do."
      >
        <ToggleGroup
          variant="outline"
          value={[tab]}
          onValueChange={(next) => next[0] && setTab(next[0])}
          className="self-start"
        >
          <ToggleGroupItem value="accounts" className="h-10 px-4">
            Tài khoản
          </ToggleGroupItem>
          <ToggleGroupItem value="parameters" className="h-10 px-4">
            Tham số
          </ToggleGroupItem>
        </ToggleGroup>
      </PageHeader>
      {tab === 'parameters' ? (
        <ParameterList canChange={role === 'NURSING_HOME_MANAGER'} />
      ) : (
        <NotDesignedState label="Tài khoản" />
      )}
    </div>
  )
}
