import { useState } from 'react'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { DetailLayout } from '@/common/DetailLayout'
import { PageHeader } from '@/common/PageHeader'
import { PermissionsProvider, usePermissions } from '@/common/permissions/permissions'
import { AccountList } from '@/features/accounts/AccountList'
import { CatalogList } from '@/features/catalogs-m01/CatalogList'
import { ParameterList } from '@/features/parameters/ParameterList'
import { RolePermissions } from '@/features/permissions/RolePermissions'

// Màn "Tài khoản, tham số" (Phụ lục 27, dòng "Tài khoản, phân quyền, tham số"). Tab Tài khoản: 002 T035; tab Phân
// quyền: 002 T077 (chỉ Quản lý viện, backend trả 403 cho vai trò khác). Tab Danh mục: 001 T072.
// Chỉ Quản lý viện đổi tham số (BR-M15-04), tạo tài khoản nhân viên (FR-002); quyền thật do backend trả và kiểm.
// Tab Danh mục theo quyền hiện hành của người dùng (GET /me/permissions, HEALTH_CATALOGS), không theo vai trò: không có
// quyền xem thì ẩn tab; tạo, sửa là hai quyền riêng. Chưa tải xong hoặc tải lỗi thì ẩn (usePermissions).
// Chi tiết của tab mở trong DetailLayout ở cấp màn để khung cao hết màn (DESIGN.md 5); đổi tab thì đóng khung.

const SUBTITLES = {
  accounts: 'Tài khoản không xóa được; khóa, mở khóa, đổi vai trò đều ghi lý do.',
  permissions: 'Chỉ thu hẹp được trong ma trận quyền; mỗi thay đổi ghi lý do và có hiệu lực ngay.',
  parameters: 'Tham số vận hành có hiệu lực ngay khi lưu; mỗi lần đổi ghi lý do.',
  catalogs: 'Danh mục không xóa được; mục ngừng hiệu lực vẫn đọc được ở dữ liệu cũ, ngừng hoặc kích hoạt lại ghi lý do.',
}

export function AdminPage(props) {
  return (
    <PermissionsProvider>
      <AdminContent {...props} />
    </PermissionsProvider>
  )
}

function AdminContent({ role }) {
  const [tab, setTab] = useState('parameters')
  const [detail, setDetail] = useState(null)
  const { can } = usePermissions()
  const canSeeCatalogs = can('HEALTH_CATALOGS', 'VIEW')
  return (
    <DetailLayout
      open={detail !== null}
      detailKey={detail?.key}
      onClose={() => setDetail(null)}
      label={detail?.label}
      renderDetail={(titles) => detail.render(titles)}
    >
      <PageHeader title="Tài khoản, tham số" subtitle={SUBTITLES[tab]}>
        <ToggleGroup
          variant="outline"
          value={[tab]}
          onValueChange={(next) => {
            if (!next[0]) return
            setTab(next[0])
            setDetail(null)
          }}
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
          {canSeeCatalogs && (
            <ToggleGroupItem value="catalogs" className="h-10 px-4">
              Danh mục
            </ToggleGroupItem>
          )}
        </ToggleGroup>
      </PageHeader>
      {tab === 'parameters' && (
        <ParameterList canChange={role === 'NURSING_HOME_MANAGER'} openedKey={detail?.key} onOpen={setDetail} />
      )}
      {tab === 'accounts' && (
        <AccountList
          canCreate={role === 'NURSING_HOME_MANAGER'}
          canCreateResident={role === 'NURSING_HOME_MANAGER' || role === 'OFFICE_STAFF'}
          openedKey={detail?.key}
          onOpen={setDetail}
        />
      )}
      {tab === 'permissions' && <RolePermissions />}
      {tab === 'catalogs' && canSeeCatalogs && (
        <CatalogList canCreate={can('HEALTH_CATALOGS', 'CREATE')} canUpdate={can('HEALTH_CATALOGS', 'UPDATE')} />
      )}
    </DetailLayout>
  )
}
