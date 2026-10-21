import { useState } from 'react'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { PageHeader } from '@/common/PageHeader'
import { AccountList } from '@/features/accounts/AccountList'
import { ParameterList } from '@/features/parameters/ParameterList'
import { RolePermissions } from '@/features/permissions/RolePermissions'

// Màn "Tài khoản, tham số" (Phụ lục 27, dòng "Tài khoản, phân quyền, tham số"). Tab Tài khoản: 002 T035; tab Phân
// quyền: 002 T077 (chỉ Quản lý viện, backend trả 403 cho vai trò khác).
// Chỉ Quản lý viện đổi tham số (BR-M15-04), tạo tài khoản nhân viên (FR-002); quyền thật do backend trả và kiểm.

const SUBTITLES = {
  accounts: 'Tài khoản không xóa được; khóa, mở khóa, đổi vai trò đều ghi lý do.',
  permissions: 'Chỉ thu hẹp được trong ma trận quyền; mỗi thay đổi ghi lý do và có hiệu lực ngay.',
  parameters: 'Tham số vận hành có hiệu lực ngay khi lưu; mỗi lần đổi ghi lý do.',
}

export function AdminPage({ role }) {
  const [tab, setTab] = useState('parameters')
  return (
    <div className="mx-auto flex w-full max-w-[1600px] flex-col gap-6">
      <PageHeader title="Tài khoản, tham số" subtitle={SUBTITLES[tab]}>
        <ToggleGroup
          variant="outline"
          value={[tab]}
          onValueChange={(next) => next[0] && setTab(next[0])}
          className="self-start"
        >
          <ToggleGroupItem value="accounts" className="h-10 px-4">
            Tài khoản
          </ToggleGroupItem>
          <ToggleGroupItem value="permissions" className="h-10 px-4">
            Phân quyền
          </ToggleGroupItem>
          <ToggleGroupItem value="parameters" className="h-10 px-4">
            Tham số
          </ToggleGroupItem>
        </ToggleGroup>
      </PageHeader>
      {tab === 'parameters' && <ParameterList canChange={role === 'NURSING_HOME_MANAGER'} />}
      {tab === 'accounts' && <AccountList canCreate={role === 'NURSING_HOME_MANAGER'} />}
      {tab === 'permissions' && <RolePermissions />}
    </div>
  )
}
