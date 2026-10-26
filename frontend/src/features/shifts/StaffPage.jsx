import { useEffect, useState } from 'react'
import {
  getStaff,
  getStaffById,
  getStaffByIdCredentials,
  getDepartments,
  getCredentialTypes,
  getPracticeScopes,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription } from '@/components/ui/sheet'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { formatDate } from '@/common/dateTime'
import { PermissionsProvider, usePermissions } from '@/common/permissions/permissions'
import { Choice } from './ShiftForm'
import { StaffProfileForm, StaffCommandForm } from './StaffForms'
import { StaffImportPanel } from './StaffImportPanel'
import { DepartmentPanel } from './DepartmentPanel'
import { CredentialPanel } from './CredentialPanel'
import { CredentialTypePanel } from './CredentialTypePanel'
export function StaffPage() {
  return (
    <PermissionsProvider>
      <StaffWorkspace />
    </PermissionsProvider>
  )
}
function StaffWorkspace() {
  const { status, can } = usePermissions(),
    view = can('STAFF_PROFILE', 'VIEW'),
    create = can('STAFF_PROFILE', 'CREATE'),
    update = can('STAFF_PROFILE', 'UPDATE')
  const [rows, setRows] = useState([]),
    [departments, setDepartments] = useState([]),
    [types, setTypes] = useState([]),
    [scopes, setScopes] = useState([]),
    [selected, setSelected] = useState(null),
    [credentials, setCredentials] = useState([]),
    [etag, setEtag] = useState(null),
    [mode, setMode] = useState(null),
    [tab, setTab] = useState('staff'),
    [failure, setFailure] = useState(null),
    [loading, setLoading] = useState(true),
    [query, setQuery] = useState(''),
    [filter, setFilter] = useState(''),
    [revision, setRevision] = useState(0),
    [detailBusy, setDetailBusy] = useState(false)
  const refresh = () => setRevision((v) => v + 1)
  useEffect(() => {
    if (status !== 'ready' || !view) return
    let cancelled = false
    async function load() {
      setLoading(true)
      try {
        const result = await Promise.all([getStaff(), getDepartments(), getCredentialTypes(), getPracticeScopes()])
        if (cancelled) return
        setRows(result[0].data)
        setDepartments(result[1].data)
        setTypes(result[2].data)
        setScopes(result[3].data)
        setFailure(null)
      } catch (e) {
        if (!cancelled) setFailure(problemOf(e))
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [status, view, revision])
  async function open(id) {
    setDetailBusy(true)
    setFailure(null)
    try {
      const [person, credentials] = await Promise.all([
        getStaffById({ path: { id } }),
        getStaffByIdCredentials({ path: { id } }),
      ])
      setSelected(person.data)
      setEtag(person.etag)
      setCredentials(credentials.data)
      setMode(null)
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setDetailBusy(false)
    }
  }
  function done() {
    setSelected(null)
    setMode(null)
    refresh()
  }
  if (status === 'error' || (status === 'ready' && !view)) return <PermissionDeniedState />
  return (
    <div className="flex min-w-0 flex-col gap-5 [&_button]:min-h-11 [&_input:not([type=checkbox])]:min-h-11 [&_select]:min-h-11">
      <PageHeader
        title="Nhân sự"
        subtitle="Hồ sơ, bộ phận, giấy phép và đào tạo"
        aside={
          create && (
            <Button
              onClick={() => {
                setSelected(null)
                setMode('new')
              }}
            >
              Thêm nhân viên
            </Button>
          )
        }
      >
        <nav aria-label="Nội dung nhân sự" className="flex flex-wrap gap-2">
          {[
            ['staff', 'Nhân viên'],
            ['departments', 'Bộ phận'],
            ['types', 'Loại chứng chỉ'],
            ...(create ? [['import', 'Import Excel']] : []),
          ].map(([key, label]) => (
            <Button
              key={key}
              variant={tab === key ? 'default' : 'outline'}
              aria-pressed={tab === key}
              onClick={() => setTab(key)}
            >
              {label}
            </Button>
          ))}
        </nav>
      </PageHeader>
      <ProblemAlert problem={failure} />
      {loading || status === 'loading' ? (
        <p role="status">Đang tải nhân sự…</p>
      ) : tab === 'import' ? (
        <StaffImportPanel onDone={refresh} />
      ) : tab === 'types' ? (
        <CredentialTypePanel types={types} create={create} onDone={refresh} />
      ) : tab === 'departments' ? (
        <DepartmentPanel departments={departments} staff={rows} create={create} update={update} onDone={refresh} />
      ) : (
        <section className="flex min-w-0 flex-col gap-4 rounded-2xl border bg-card p-4 md:p-5">
          <div className="grid gap-4 sm:grid-cols-2">
            <Field>
              <FieldLabel htmlFor="staff-search">Tìm nhân viên</FieldLabel>
              <Input
                id="staff-search"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Họ tên hoặc chức danh"
              />
            </Field>
            <Choice
              id="staff-status-filter"
              label="Trạng thái làm việc"
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
            >
              <option value="">Tất cả</option>
              <option value="WORKING">Đang làm việc</option>
              <option value="RESIGNED">Nghỉ việc</option>
            </Choice>
          </div>
          {!rows.length ? (
            <p>Chưa có hồ sơ nhân viên trong phạm vi được xem.</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Nhân viên</TableHead>
                  <TableHead>Bộ phận</TableHead>
                  <TableHead>Trạng thái</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {rows
                  .filter(
                    (s) =>
                      (!filter || s.status === filter) &&
                      `${s.fullName} ${s.position}`.toLocaleLowerCase('vi').includes(query.toLocaleLowerCase('vi')),
                  )
                  .map((s) => (
                    <TableRow key={s.id}>
                      <TableCell>
                        <Button
                          variant="link"
                          className="h-auto max-w-full whitespace-normal p-0 text-left"
                          disabled={detailBusy}
                          onClick={() => open(s.id)}
                        >
                          {s.fullName}
                        </Button>
                        <p className="text-muted-foreground">{s.position}</p>
                      </TableCell>
                      <TableCell>
                        {departments.find((d) => d.id === s.departmentId)?.name ?? 'Chưa có bộ phận'}
                      </TableCell>
                      <TableCell>{s.status === 'WORKING' ? 'Đang làm việc' : 'Nghỉ việc'}</TableCell>
                    </TableRow>
                  ))}
              </TableBody>
            </Table>
          )}
        </section>
      )}
      <Sheet
        open={Boolean(selected) || mode === 'new'}
        onOpenChange={(open) => {
          if (!open) {
            setSelected(null)
            setMode(null)
          }
        }}
      >
        <SheetContent className="overflow-y-auto data-[side=right]:w-full data-[side=right]:sm:max-w-xl">
          <SheetHeader>
            <SheetTitle>{selected?.fullName ?? 'Thêm nhân viên'}</SheetTitle>
            <SheetDescription>
              {selected ? 'Thông tin được hiển thị theo quyền hiện hành.' : 'Tạo hồ sơ nhân viên; chưa tạo tài khoản.'}
            </SheetDescription>
          </SheetHeader>
          <div className="flex min-w-0 flex-col gap-5 p-4">
            {mode === 'new' || mode === 'edit' ? (
              <StaffProfileForm
                key={`${selected?.id ?? 'new'}-${mode}`}
                person={selected}
                etag={etag}
                departments={departments}
                onDone={done}
              />
            ) : (
              selected && (
                <>
                  <dl className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                    {[
                      ['Chức danh', selected.position],
                      ['Chuyên môn', selected.specialty],
                      ['Ngày bắt đầu', formatDate(selected.startDate)],
                      ['Số điện thoại', selected.phoneNumber],
                      ['Email', selected.email],
                      ['Bộ phận', departments.find((d) => d.id === selected.departmentId)?.name],
                      ['Trạng thái', selected.status === 'WORKING' ? 'Đang làm việc' : 'Nghỉ việc'],
                    ]
                      .filter(([, value]) => value)
                      .map(([label, value]) => (
                        <div key={label}>
                          <dt className="text-muted-foreground">{label}</dt>
                          <dd className="break-words font-medium">{value}</dd>
                        </div>
                      ))}
                  </dl>
                  {update && (
                    <div className="flex flex-wrap gap-2">
                      <Button variant="outline" onClick={() => setMode('edit')}>
                        Sửa hồ sơ
                      </Button>
                      <Button variant="outline" onClick={() => setMode('department')}>
                        Đổi bộ phận
                      </Button>
                      {selected.status === 'WORKING' ? (
                        <Button variant="outline" onClick={() => setMode('terminate')}>
                          Cho nghỉ việc
                        </Button>
                      ) : (
                        <Button variant="outline" onClick={() => setMode('rehire')}>
                          Nhận lại làm việc
                        </Button>
                      )}
                    </div>
                  )}
                  {['terminate', 'rehire', 'department'].includes(mode) ? (
                    <StaffCommandForm
                      key={mode}
                      kind={mode}
                      person={selected}
                      staff={rows}
                      departments={departments}
                      onDone={done}
                    />
                  ) : (
                    <CredentialPanel
                      person={selected}
                      rows={credentials}
                      types={types}
                      scopes={scopes}
                      create={create}
                      update={update}
                      onDone={() => open(selected.id)}
                    />
                  )}
                </>
              )
            )}
          </div>
        </SheetContent>
      </Sheet>
    </div>
  )
}
