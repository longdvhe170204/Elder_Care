import { useCallback, useEffect, useState } from 'react'
import { ApiError, getAuditLogs } from '@/api'
import { Alert, AlertAction, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { formatDateTime } from '@/common/dateTime'

// Tra cứu nhật ký toàn viện (000 T073; FR-049, NFR-10, NFR-11; hợp đồng GET /audit-logs). Chỉ Quản lý viện:
// backend trả 403 cho vai trò khác; nhân viên xem lịch sử của từng đối tượng ở màn của đối tượng đó.
// Trường người xem không được xem đã bị che ở backend ({"changed": true}); màn chỉ hiện "Đã thay đổi".

// Vai trò theo mã actor (nghiep-vu.md 2.3, Phụ lục 26.1)
const ROLE_LABELS = {
  'AC-01': 'Quản lý viện',
  'AC-02': 'Trưởng tầng',
  'AC-03': 'Bác sĩ',
  'AC-04': 'Điều dưỡng',
  'AC-05': 'Nhân viên chăm sóc',
  'AC-07': 'Nhân viên bếp',
  'AC-09': 'Hành chính',
  'AC-11': 'Bộ lập lịch hệ thống',
  'AC-14': 'Kế toán',
}

const EMPTY_FILTER = { entityType: '', entityId: '', actorId: '', from: '', to: '' }

// Khoảng ngày nhập theo ngày lịch Việt Nam, gồm cả hai đầu (FR-046)
function toQuery(filter) {
  const query = {}
  if (filter.entityType.trim()) query.entityType = filter.entityType.trim()
  if (filter.entityId.trim()) query.entityId = filter.entityId.trim()
  if (filter.actorId.trim()) query.actorId = filter.actorId.trim()
  if (filter.from) query.from = `${filter.from}T00:00:00+07:00`
  if (filter.to) query.to = `${filter.to}T23:59:59.999+07:00`
  return query
}

function actorLabel(actor) {
  if (actor.type === 'SCHEDULER') return 'Bộ lập lịch hệ thống'
  if (actor.type === 'BOOTSTRAP') return 'Khởi tạo hệ thống'
  return actor.fullName ?? `Tài khoản ${actor.id}`
}

function valueText(value) {
  if (value === null || value === undefined) return 'trống'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function isMasked(value) {
  return value !== null && typeof value === 'object' && value.changed === true
}

function Changes({ before, after }) {
  if (!before && !after) return <span className="text-muted-foreground">Không đổi dữ liệu</span>
  const fields = [...new Set([...Object.keys(before ?? {}), ...Object.keys(after ?? {})])]
  return (
    <ul className="flex flex-col gap-1">
      {fields.map((field) => {
        const from = before?.[field]
        const to = after?.[field]
        return (
          <li key={field}>
            {field}:{' '}
            {isMasked(from) || isMasked(to) ? (
              <span className="text-muted-foreground">Đã thay đổi</span>
            ) : (
              `${valueText(from)} → ${valueText(to)}`
            )}
          </li>
        )
      })}
    </ul>
  )
}

function EntryRow({ entry }) {
  return (
    <TableRow className="align-top">
      <TableCell className="whitespace-normal tabular-nums">
        <div>{formatDateTime(entry.occurredAt)}</div>
        {entry.deviceTime && (
          <div className="type-caption text-muted-foreground">Trên thiết bị: {formatDateTime(entry.deviceTime)}</div>
        )}
      </TableCell>
      <TableCell className="whitespace-normal">
        <div>{actorLabel(entry.actor)}</div>
        {entry.actor.type === 'USER' && (
          <div className="type-caption text-muted-foreground">{ROLE_LABELS[entry.actor.role] ?? entry.actor.role}</div>
        )}
      </TableCell>
      <TableCell>{entry.action}</TableCell>
      <TableCell className="whitespace-normal">
        {entry.entity.type} {entry.entity.id}
      </TableCell>
      <TableCell className="whitespace-normal">
        <Changes before={entry.dataBefore} after={entry.dataAfter} />
      </TableCell>
      <TableCell className="whitespace-normal">
        {entry.reasonHidden ? (
          <span className="text-muted-foreground">Đã ẩn vì có trường không được xem</span>
        ) : (
          (entry.reason ?? <span className="text-muted-foreground">Không ghi lý do</span>)
        )}
      </TableCell>
    </TableRow>
  )
}

function FilterField({ id, label, type = 'text', value, onChange, error }) {
  return (
    <Field data-invalid={!!error || undefined}>
      <FieldLabel htmlFor={id}>{label}</FieldLabel>
      <Input
        id={id}
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-invalid={!!error || undefined}
        className="h-10"
      />
      {error && <FieldError>{error}</FieldError>}
    </Field>
  )
}

export function AuditLogTable() {
  const [filter, setFilter] = useState(EMPTY_FILTER)
  const [actorError, setActorError] = useState('')
  const [query, setQuery] = useState({})
  const [state, setState] = useState({ status: 'loading', items: [], nextCursor: null })

  const load = useCallback(async (nextQuery, cursor) => {
    setState((s) => (cursor ? { ...s, loadingMore: true } : { status: 'loading', items: [], nextCursor: null }))
    try {
      const { data } = await getAuditLogs({ query: { ...nextQuery, cursor } })
      setState((s) => ({
        status: 'ready',
        items: cursor ? [...s.items, ...data.items] : data.items,
        nextCursor: data.nextCursor,
      }))
    } catch (error) {
      setState({ status: error instanceof ApiError && error.status === 403 ? 'denied' : 'error', items: [] })
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải trang đầu khi mở màn
    load({})
  }, [load])

  function search(event) {
    event.preventDefault()
    if (filter.actorId.trim() && !/^\d+$/.test(filter.actorId.trim())) {
      setActorError('Nhập mã số tài khoản')
      return
    }
    setActorError('')
    const next = toQuery(filter)
    setQuery(next)
    load(next)
  }

  if (state.status === 'denied') {
    return (
      <PermissionDeniedState
        label="nhật ký toàn viện"
        description="Tra cứu nhật ký toàn viện chỉ dành cho Quản lý viện. Lịch sử thay đổi của một hồ sơ, bản ghi xem ở màn của từng đối tượng."
      />
    )
  }

  const update = (key) => (value) => setFilter({ ...filter, [key]: value })

  return (
    <div className="mx-auto flex w-full max-w-[1600px] flex-col gap-6">
      <PageHeader
        title="Nhật ký thay đổi toàn viện"
        subtitle="Ai đã làm gì, lúc nào, trên đối tượng nào. Mới nhất ở trên."
      >
        <form
          onSubmit={search}
          noValidate
          className="grid gap-3 md:grid-cols-[repeat(5,minmax(0,1fr))_auto] md:items-end"
        >
          <FilterField
            id="audit-entity-type"
            label="Loại đối tượng"
            value={filter.entityType}
            onChange={update('entityType')}
          />
          <FilterField
            id="audit-entity-id"
            label="Mã đối tượng"
            value={filter.entityId}
            onChange={update('entityId')}
          />
          <FilterField
            id="audit-actor"
            label="Mã người thực hiện"
            value={filter.actorId}
            onChange={update('actorId')}
            error={actorError}
          />
          <FilterField id="audit-from" label="Từ ngày" type="date" value={filter.from} onChange={update('from')} />
          <FilterField id="audit-to" label="Đến ngày" type="date" value={filter.to} onChange={update('to')} />
          <Button type="submit" className="h-10">
            Tra cứu
          </Button>
        </form>
      </PageHeader>

      <section className="flex flex-col gap-3 rounded-2xl border bg-card p-4 md:p-5">
        {state.status === 'loading' && (
          <div aria-busy="true" className="flex flex-col gap-2">
            <span className="sr-only">Đang tải nhật ký</span>
            <Skeleton className="h-10" />
            <Skeleton className="h-10" />
            <Skeleton className="h-10" />
          </div>
        )}
        {state.status === 'error' && (
          <Alert role="alert">
            <AlertTitle>Không tải được nhật ký</AlertTitle>
            <AlertAction>
              <Button variant="outline" size="sm" onClick={() => load(query)}>
                Thử lại
              </Button>
            </AlertAction>
          </Alert>
        )}
        {state.status === 'ready' && state.items.length === 0 && (
          <Empty>
            <EmptyHeader>
              <EmptyTitle>Không có bản ghi nhật ký khớp bộ lọc</EmptyTitle>
              <EmptyDescription>Đổi bộ lọc hoặc mở rộng khoảng ngày rồi tra cứu lại.</EmptyDescription>
            </EmptyHeader>
          </Empty>
        )}
        {state.status === 'ready' && state.items.length > 0 && (
          <>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Thời điểm</TableHead>
                  <TableHead>Người thực hiện</TableHead>
                  <TableHead>Hành động</TableHead>
                  <TableHead>Đối tượng</TableHead>
                  <TableHead className="min-w-44">Thay đổi</TableHead>
                  <TableHead className="min-w-36">Lý do</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {state.items.map((entry) => (
                  <EntryRow key={entry.id} entry={entry} />
                ))}
              </TableBody>
            </Table>
            {state.nextCursor && (
              <Button
                variant="outline"
                className="self-start"
                disabled={state.loadingMore}
                onClick={() => load(query, state.nextCursor)}
              >
                Xem thêm
              </Button>
            )}
          </>
        )}
      </section>
    </div>
  )
}
