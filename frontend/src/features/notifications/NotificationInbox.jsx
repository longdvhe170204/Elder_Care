import { useCallback, useEffect, useState } from 'react'
import { getMeNotifications } from '@/api'
import { Alert, AlertAction, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Field, FieldLabel } from '@/components/ui/field'
import { Sheet, SheetContent } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { NotificationDetail } from './NotificationDetail'
import { LEVEL_LABELS, label } from './notificationLabels'

// Hộp thông báo (009 T035; FR-033 → FR-036; DESIGN.md `notification-inbox`). Khẩn cấp chưa xác nhận lên đầu (backend
// sắp); thông báo mới qua SSE thì tải lại trang đầu, mất luồng thì hỏi lại mỗi 60 giây. Lọc theo người cao tuổi chờ danh
// sách người cao tuổi của 001; nút xác nhận Khẩn cấp chờ 009 US1.

const LEVELS = ['URGENT', 'MEDIUM', 'LOW']
const STREAM_URL = '/api/v1/notifications/stream'
const POLL_MS = 60_000
const SELECT_CLASS = 'h-10 w-full rounded-md border border-input bg-transparent px-3'

function toQuery({ level, unread, eventType }, cursor) {
  return { level: level || undefined, unread: unread || undefined, eventType: eventType || undefined, cursor }
}

const isOpenUrgent = (n) => n.level === 'URGENT' && !n.acknowledgedAt

export function NotificationInbox() {
  const [filter, setFilter] = useState({ level: '', unread: false, eventType: '' })
  const [state, setState] = useState({ status: 'loading', items: [], nextCursor: null })
  const [eventTypes, setEventTypes] = useState([])
  const [opened, setOpened] = useState(null)
  const [announcement, setAnnouncement] = useState('')

  const load = useCallback(async (nextFilter, cursor) => {
    setState((s) =>
      cursor ? { ...s, loadingMore: true } : { ...s, status: s.status === 'ready' ? 'ready' : 'loading' },
    )
    try {
      const { data } = await getMeNotifications({ query: toQuery(nextFilter, cursor) })
      setState((s) => ({
        status: 'ready',
        items: cursor ? [...s.items, ...data.items] : data.items,
        nextCursor: data.nextCursor,
        unreadCount: data.unreadCount,
        urgentCount: data.unacknowledgedUrgentCount,
      }))
      setEventTypes((known) => [...new Set([...known, ...data.items.map((n) => n.eventType).filter(Boolean)])].sort())
    } catch {
      setState({ status: 'error', items: [], nextCursor: null })
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải trang đầu khi mở màn
    load(filter)
    // Chỉ tải lại khi bộ lọc đổi; filter đọc trong hàm theo giá trị lúc gọi
  }, [load, filter])

  useEffect(() => {
    const refresh = () => {
      setAnnouncement('Có thông báo mới')
      load(filter)
    }
    if (typeof EventSource === 'undefined') {
      const timer = setInterval(() => load(filter), POLL_MS)
      return () => clearInterval(timer)
    }
    const source = new EventSource(STREAM_URL, { withCredentials: true })
    let timer = null
    source.addEventListener('notification', refresh)
    source.onerror = () => {
      if (!timer) timer = setInterval(() => load(filter), POLL_MS)
    }
    source.onopen = () => {
      clearInterval(timer)
      timer = null
    }
    return () => {
      source.close()
      clearInterval(timer)
    }
  }, [load, filter])

  const replace = useCallback((data) => {
    setState((s) => ({ ...s, items: s.items.map((n) => (n.id === data.id ? data : n)) }))
  }, [])

  const hide = useCallback((id) => {
    setState((s) => ({ ...s, items: s.items.filter((n) => n.id !== id) }))
    setOpened(null)
    setAnnouncement('Đã ẩn thông báo khỏi danh sách')
  }, [])

  const urgent = state.items.filter(isOpenUrgent)
  const rest = state.items.filter((n) => !isOpenUrgent(n))

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-3 rounded-2xl border bg-card p-4 md:p-5">
        {state.status === 'ready' && (
          <p className="type-body">
            {`${state.unreadCount ?? 0} chưa xem, ${state.urgentCount ?? 0} khẩn cấp chưa xác nhận. Thông báo mới hiện ngay khi đến.`}
          </p>
        )}
        <div className="flex flex-wrap items-end gap-3">
          <ToggleGroup
            variant="outline"
            value={[filter.level || 'ALL']}
            onValueChange={(next) => next[0] && setFilter((f) => ({ ...f, level: next[0] === 'ALL' ? '' : next[0] }))}
            className="flex-wrap"
          >
            <ToggleGroupItem value="ALL" className="h-10 px-4">
              Tất cả mức
            </ToggleGroupItem>
            {LEVELS.map((level) => (
              <ToggleGroupItem key={level} value={level} className="h-10 px-4">
                {LEVEL_LABELS[level]}
              </ToggleGroupItem>
            ))}
          </ToggleGroup>
          <ToggleGroup
            variant="outline"
            value={filter.unread ? ['unread'] : []}
            onValueChange={(next) => setFilter((f) => ({ ...f, unread: next.includes('unread') }))}
          >
            <ToggleGroupItem value="unread" className="h-10 px-4">
              Chưa xem
            </ToggleGroupItem>
          </ToggleGroup>
          <Field className="w-60">
            <FieldLabel htmlFor="notification-event-type">Loại sự kiện</FieldLabel>
            <select
              id="notification-event-type"
              value={filter.eventType}
              onChange={(e) => setFilter((f) => ({ ...f, eventType: e.target.value }))}
              className={SELECT_CLASS}
            >
              <option value="">Tất cả</option>
              {eventTypes.map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
              ))}
            </select>
          </Field>
        </div>
      </div>

      <p aria-live="polite" className="sr-only">
        {announcement}
      </p>

      {state.status === 'loading' && (
        <div aria-busy="true" className="flex flex-col gap-2 rounded-2xl border bg-card p-4">
          <span className="sr-only">Đang tải thông báo</span>
          <Skeleton className="h-12" />
          <Skeleton className="h-12" />
          <Skeleton className="h-12" />
        </div>
      )}
      {state.status === 'error' && (
        <div className="rounded-2xl border bg-card p-4">
          <Alert role="alert">
            <AlertTitle>Không tải được thông báo</AlertTitle>
            <AlertAction>
              <Button variant="outline" size="sm" onClick={() => load(filter)}>
                Thử lại
              </Button>
            </AlertAction>
          </Alert>
        </div>
      )}

      {state.status === 'ready' && urgent.length > 0 && (
        <section
          aria-label="Khẩn cấp chưa xác nhận"
          className="flex flex-col gap-3 rounded-2xl border bg-card p-4 md:p-5"
        >
          <h2 className="type-title">Khẩn cấp chưa xác nhận</h2>
          <ul className="flex flex-col gap-2">
            {urgent.map((n) => (
              <li key={n.id} className="rounded-xl border-[1.5px] border-destructive p-3">
                <NotificationRow notification={n} onOpen={() => setOpened(n.id)} status="Chưa xác nhận" />
              </li>
            ))}
          </ul>
        </section>
      )}

      {state.status === 'ready' && (
        <section className="flex flex-col gap-3 rounded-2xl border bg-card p-4 md:p-5">
          {rest.length === 0 && urgent.length === 0 ? (
            <Empty>
              <EmptyHeader>
                <EmptyTitle>Không có thông báo nào</EmptyTitle>
                <EmptyDescription>Thông báo mới sẽ hiện ở đây. Đổi bộ lọc để xem thông báo khác.</EmptyDescription>
              </EmptyHeader>
            </Empty>
          ) : (
            <ul aria-label="Thông báo" className="flex flex-col">
              {rest.map((n) => (
                <li key={n.id} className="border-b py-3 last:border-b-0">
                  <NotificationRow
                    notification={n}
                    onOpen={() => setOpened(n.id)}
                    status={n.viewedAt ? 'Đã xem' : 'Chưa xem'}
                  />
                </li>
              ))}
            </ul>
          )}
          {state.nextCursor && (
            <Button
              variant="outline"
              className="self-start"
              disabled={state.loadingMore}
              onClick={() => load(filter, state.nextCursor)}
            >
              Xem thêm
            </Button>
          )}
        </section>
      )}

      <Sheet open={opened !== null} onOpenChange={(open) => !open && setOpened(null)}>
        <SheetContent className="sm:max-w-lg">
          {opened !== null && <NotificationDetail key={opened} id={opened} onOpened={replace} onHidden={hide} />}
        </SheetContent>
      </Sheet>
    </div>
  )
}

function NotificationRow({ notification: n, onOpen, status }) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-x-4 gap-y-1">
      <div className="flex min-w-0 flex-col gap-1">
        <div className="flex flex-wrap items-center gap-2">
          <StatusBadge status={label(LEVEL_LABELS, n.level)} />
          <Button variant="link" className="h-auto p-0 type-label" onClick={onOpen}>
            {n.title}
          </Button>
        </div>
        <span className="type-caption text-muted-foreground">{n.eventType}</span>
      </div>
      <div className="flex items-center gap-3">
        <span className="type-body-sm tabular-nums">{n.sentAt ? formatDateTime(n.sentAt) : ''}</span>
        <StatusBadge status={status} />
      </div>
    </div>
  )
}
