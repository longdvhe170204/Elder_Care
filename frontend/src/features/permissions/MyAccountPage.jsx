import { useEffect, useState } from 'react'
import { getMePermissions, getMeShifts } from '@/api'
import { Button, buttonVariants } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatDate, formatDateTime } from '@/common/dateTime'
import { ResidentAvatar } from '@/common/ResidentAvatar'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { staffRoles } from '@/common/session'
import { ROLES, navForRole } from '@/common/layout/navigation'
import { MyContact } from './MyContact'
import { FUNCTION_ORDER, OPERATION_ORDER, OPERATION_LABELS, SCOPE_LABELS, functionLabel } from './permissionLabels'

// Tài khoản của tôi (002 FR-042; bố cục duyệt 2026-11-09, sửa cùng ngày): đầu trang ảnh chữ cái, họ tên, các vai trò;
// Thông tin tài khoản (GET /me); Bảo mật (đổi mật khẩu ở /change-password); Ca sắp tới (GET /me/shifts) khi có quyền
// Lịch ca; Quyền của tôi (GET /me/permissions, chỉ xem). Số điện thoại, email sửa ở MyContact (PUT /me/contact).

const SOURCE_LABELS = { ROLE: 'Vai trò', DUTY: 'Nhiệm vụ', ADDITIONAL_GRANT: 'Quyền gán thêm' }
const SCOPE_TEXT = { ...SCOPE_LABELS, SELF: 'Của chính mình' }

const roleLabel = (code) => ROLES.find((r) => r.code === code)?.label ?? code

// Gộp các thao tác cùng chức năng, phạm vi, nguồn thành một dòng; xếp theo thứ tự Phụ lục 27
function permissionRows(permissions) {
  const rows = new Map()
  for (const p of permissions) {
    const key = `${p.function}|${p.scope}|${p.source}`
    if (!rows.has(key)) rows.set(key, { key, function: p.function, scope: p.scope, source: p.source, operations: [] })
    rows.get(key).operations.push(p.operation)
  }
  const order = (list, code) => (list.indexOf(code) < 0 ? list.length : list.indexOf(code))
  return [...rows.values()]
    .map((row) => ({
      ...row,
      operations: row.operations.sort((a, b) => order(OPERATION_ORDER, a) - order(OPERATION_ORDER, b)),
    }))
    .sort((a, b) => order(FUNCTION_ORDER, a.function) - order(FUNCTION_ORDER, b.function))
}

function operationsText(operations) {
  const text = operations.map((op) => (OPERATION_LABELS[op] ?? op).toLowerCase()).join(', ')
  return text.charAt(0).toUpperCase() + text.slice(1)
}

function InfoRow({ label, children }) {
  return (
    <div className="flex flex-col gap-0.5">
      <dt className="type-caption text-muted-foreground">{label}</dt>
      <dd className="type-body">{children}</dd>
    </div>
  )
}

function PermissionTable({ rows }) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Chức năng</TableHead>
          <TableHead>Thao tác</TableHead>
          <TableHead>Phạm vi</TableHead>
          <TableHead>Nguồn</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {rows.map((row) => (
          <TableRow key={row.key}>
            <TableCell className="whitespace-normal">{functionLabel(row.function)}</TableCell>
            <TableCell className="whitespace-normal">{operationsText(row.operations)}</TableCell>
            <TableCell>{SCOPE_TEXT[row.scope] ?? row.scope}</TableCell>
            <TableCell>{SOURCE_LABELS[row.source] ?? row.source}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}

function useMyPermissions(live) {
  const [state, setState] = useState({ status: live ? 'loading' : 'demo' })
  useEffect(() => {
    if (!live) return undefined
    let cancelled = false
    getMePermissions()
      .then(({ data }) => !cancelled && setState({ status: 'ready', data }))
      .catch((error) => !cancelled && setState({ status: 'error', problem: problemOf(error) }))
    return () => {
      cancelled = true
    }
  }, [live])
  return state
}

// Ca của chính mình trong 7 ngày tới (008 FR-033, FR-054; GET /me/shifts). Hợp đồng chỉ có mã người cùng ca nên chưa
// hiện tên; nhãn trạng thái ca chép theo api-008 (không import chéo feature shifts).
const SHIFT_STATUS = {
  DRAFT: 'Nháp',
  PUBLISHED: 'Đã công bố',
  IN_PROGRESS: 'Đang diễn ra',
  AWAITING_HANDOVER: 'Chờ bàn giao',
  CLOSED: 'Đã đóng',
  CANCELLED: 'Đã hủy',
}
const SHIFT_DAYS = 7
const ymd = (date) => new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh' }).format(date)
const timeOf = (iso) => formatDateTime(iso).split(' ')[1]

function useMyShifts(enabled) {
  const [state, setState] = useState({ status: 'loading' })
  useEffect(() => {
    if (!enabled) return undefined
    let cancelled = false
    const now = Date.now()
    const query = { from: ymd(new Date(now)), to: ymd(new Date(now + SHIFT_DAYS * 86400000)) }
    getMeShifts({ query })
      .then(({ data }) => {
        if (cancelled) return
        const upcoming = data
          .filter((shift) => new Date(shift.endsAt).getTime() > now)
          .sort((a, b) => new Date(a.startsAt) - new Date(b.startsAt))
        setState({ status: 'ready', shifts: upcoming })
      })
      .catch((error) => !cancelled && setState({ status: 'error', problem: problemOf(error) }))
    return () => {
      cancelled = true
    }
  }, [enabled])
  return state
}

function UpcomingShifts({ state, onNavigate }) {
  return (
    <Block title="Ca sắp tới" id="my-shifts">
      {state.status === 'loading' && <Skeleton className="h-10" />}
      {state.status === 'error' && <ProblemAlert problem={state.problem} />}
      {state.status === 'ready' &&
        (state.shifts.length > 0 ? (
          <ul className="flex flex-col gap-2">
            {state.shifts.map((shift) => (
              <li key={shift.id} className="flex flex-wrap items-baseline justify-between gap-2">
                <span className="type-body tabular-nums">
                  {`${formatDate(shift.startsAt)}, ${timeOf(shift.startsAt)}–${timeOf(shift.endsAt)}`}
                </span>
                <span className="type-caption text-muted-foreground">{SHIFT_STATUS[shift.status] ?? shift.status}</span>
              </li>
            ))}
          </ul>
        ) : (
          <p className="type-body text-muted-foreground">Không có ca trong {SHIFT_DAYS} ngày tới.</p>
        ))}
      <div>
        <Button type="button" variant="outline" onClick={() => onNavigate('shifts')}>
          Xem lịch ca của tôi
        </Button>
      </div>
    </Block>
  )
}

// Một khối có tiêu đề cấp 2, là vùng có tên để trình đọc màn hình nhảy tới (WCAG 1.3.1)
function Block({ title, id, description, children }) {
  const headingId = `${id}-title`
  return (
    <section aria-labelledby={headingId}>
      <Card>
        <CardHeader>
          <CardTitle id={headingId} role="heading" aria-level={2} className="type-card-title">
            {title}
          </CardTitle>
          {description && <p className="type-body-sm text-muted-foreground">{description}</p>}
        </CardHeader>
        <CardContent className="flex flex-col gap-4">{children}</CardContent>
      </Card>
    </section>
  )
}

// built: tập màn đã có (như menu), để bản mẫu chỉ liệt kê mục có trong menu
export function MyAccountPage({ role, session = null, built = null, onNavigate }) {
  const live = Boolean(session)
  const permissions = useMyPermissions(live)
  const roles = (live ? staffRoles(session.roles) : [role]).map(roleLabel).join(', ')
  const name = live ? (session.fullName ?? `Tài khoản ${session.accountId}`) : null
  const demoItems = live ? [] : navForRole(role, null, built).flatMap((g) => g.items)
  const canSeeShifts =
    live && permissions.status === 'ready' && permissions.data.some((p) => p.function === 'SHIFT_SCHEDULE')
  const shifts = useMyShifts(canSeeShifts)

  return (
    <div className="flex max-w-[1600px] flex-col gap-5">
      <section className="flex items-center gap-4">
        {live && <ResidentAvatar name={name} code={String(session.accountId)} className="size-14 type-title" />}
        <div className="flex flex-col gap-1">
          <h1 className="type-page text-primary">{live ? name : 'Tài khoản của tôi'}</h1>
          <p className="text-muted-foreground">{live ? roles : 'Bản mẫu, dữ liệu minh họa'}</p>
        </div>
      </section>

      <Block title="Thông tin tài khoản" id="my-info">
        <dl className="grid gap-4 sm:grid-cols-2">
          {live && <InfoRow label="Họ tên">{name}</InfoRow>}
          <InfoRow label="Vai trò">{roles}</InfoRow>
        </dl>
        {live && <MyContact initial={session} InfoRow={InfoRow} />}
      </Block>

      {live && (
        <Block title="Bảo mật" id="my-security">
          <div>
            <a href="/change-password" className={buttonVariants({ variant: 'outline' })}>
              Đổi mật khẩu
            </a>
          </div>
        </Block>
      )}

      {canSeeShifts && <UpcomingShifts state={shifts} onNavigate={onNavigate} />}

      <Block
        title={live ? 'Quyền của tôi' : 'Mục được dùng'}
        id="my-permissions"
        description={
          live
            ? 'Quyền hiện hành theo vai trò, nhiệm vụ và quyền gán thêm. Cần thay đổi quyền thì liên hệ Quản lý viện.'
            : 'Bản mẫu chưa có quyền thật: liệt kê các mục menu vai trò này dùng theo ma trận quyền.'
        }
      >
        {permissions.status === 'loading' && (
          <div aria-busy="true" className="flex flex-col gap-2">
            <span className="sr-only">Đang tải quyền</span>
            <Skeleton className="h-10" />
            <Skeleton className="h-10" />
          </div>
        )}
        {permissions.status === 'error' && <ProblemAlert problem={permissions.problem} />}
        {permissions.status === 'ready' &&
          (permissions.data.length > 0 ? (
            <PermissionTable rows={permissionRows(permissions.data)} />
          ) : (
            <p className="type-body text-muted-foreground">Tài khoản chưa có quyền nào.</p>
          ))}
        {permissions.status === 'demo' && (
          <ul className="flex flex-col gap-1 type-body">
            {demoItems.map((item) => (
              <li key={item.key}>{item.label}</li>
            ))}
          </ul>
        )}
      </Block>
    </div>
  )
}
