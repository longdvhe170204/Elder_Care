import { useCallback, useEffect, useRef, useState } from 'react'
import { LockIcon } from 'lucide-react'
import { getMeNotificationsById, postMeNotificationsByIdHide } from '@/api'
import { Button } from '@/components/ui/button'
import { SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { problemOf } from '@/common/problem'
import { formatDateTime } from '@/common/dateTime'
import { LEVEL_LABELS, PART_LABELS, STATUS_LABELS, label } from './notificationLabels'

// Khung chi tiết thông báo (009 T035; FR-034, FR-036; DESIGN.md 2.2, `notification-inbox`). Mở là ghi xem lần đầu
// (backend); nội dung theo quyền hiện hành: phần đã hết quyền thay bằng dòng khóa, không lộ nội dung. Bản ghi nguồn chỉ
// là chữ tới khi module nguồn có khóa quyền xem (`canOpen`).

const PART_ORDER = ['GENERAL', 'HEALTH', 'COSTS', 'VISITS', 'CONTRACT']

export function NotificationDetail({ id, onOpened, onHidden }) {
  const [state, setState] = useState({ status: 'loading' })
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const hideKey = useRef(crypto.randomUUID())

  const load = useCallback(async () => {
    try {
      const { data } = await getMeNotificationsById({ path: { id } })
      setState({ status: 'ready', notification: data })
      onOpened(data)
    } catch (error) {
      setState({ status: 'error', problem: problemOf(error) })
    }
  }, [id, onOpened])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải khi mở khung
    load()
  }, [load])

  if (state.status === 'loading') {
    return (
      <div aria-busy="true" className="flex flex-col gap-3 p-6">
        <span className="sr-only">Đang tải thông báo</span>
        <Skeleton className="h-6 w-2/3" />
        <Skeleton className="h-20" />
      </div>
    )
  }
  if (state.status === 'error') {
    return (
      <div className="p-6">
        <ProblemAlert problem={state.problem} title="Không mở được thông báo" />
      </div>
    )
  }

  const n = state.notification
  const parts = PART_ORDER.filter(
    (type) => n.content.some((p) => p.informationType === type) || n.hiddenParts.includes(type),
  )

  async function hide() {
    setFailure(null)
    setBusy(true)
    try {
      await postMeNotificationsByIdHide({ path: { id }, headers: { 'Idempotency-Key': hideKey.current } })
      onHidden(id)
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <SheetHeader className="gap-1 pt-6">
        <SheetTitle className="type-title">{n.title}</SheetTitle>
        <SheetDescription>{n.eventType}</SheetDescription>
      </SheetHeader>
      <div className="flex flex-col gap-5 overflow-y-auto px-4 pb-6 type-body-sm">
        <div className="flex flex-wrap gap-2">
          <StatusBadge status={label(LEVEL_LABELS, n.level)} />
          <StatusBadge status={label(STATUS_LABELS, n.status)} />
        </div>
        <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1">
          <dt className="text-muted-foreground">Gửi lúc</dt>
          <dd className="tabular-nums">{n.sentAt ? formatDateTime(n.sentAt) : 'Chưa gửi'}</dd>
          <dt className="text-muted-foreground">Xem lần đầu</dt>
          <dd className="tabular-nums">{n.viewedAt ? formatDateTime(n.viewedAt) : 'Chưa xem'}</dd>
          {n.acknowledgedAt && (
            <>
              <dt className="text-muted-foreground">Xác nhận lúc</dt>
              <dd className="tabular-nums">{formatDateTime(n.acknowledgedAt)}</dd>
            </>
          )}
          <dt className="text-muted-foreground">Bản ghi nguồn</dt>
          <dd>{n.sourceRecord ? `${n.sourceRecord.type} ${n.sourceRecord.id}` : 'Không có'}</dd>
        </dl>

        {n.identityOnly && (
          <p className="rounded-xl bg-muted p-3">
            Bạn nhận thay vì không có người nhận; chỉ xem được thông tin nhận dạng của người cao tuổi.
          </p>
        )}

        {parts.map((type) => {
          const hidden = n.hiddenParts.includes(type)
          return (
            <section key={type} className="flex flex-col gap-2 rounded-xl border p-3">
              <h3 className="flex items-center gap-2 type-card-title">
                {type === 'HEALTH' && <LockIcon aria-hidden="true" className="size-4" />}
                {PART_LABELS[type]}
              </h3>
              {hidden ? (
                <p className="text-muted-foreground">Bạn không còn quyền xem phần này.</p>
              ) : (
                n.content
                  .filter((p) => p.informationType === type)
                  .map((p, i) => (
                    <p key={i} className="whitespace-pre-line">
                      {p.content}
                    </p>
                  ))
              )}
            </section>
          )
        })}

        <ProblemAlert problem={failure} />
        {n.viewedAt && (
          <Button variant="outline" className="self-start" disabled={busy} onClick={hide}>
            Ẩn khỏi danh sách
          </Button>
        )}
      </div>
    </>
  )
}
