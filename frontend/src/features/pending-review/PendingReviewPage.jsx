import { useCallback, useEffect, useState } from 'react'
import {
  ApiError,
  getPendingReviewRecords,
  postPendingReviewRecordsByIdAccept,
  postPendingReviewRecordsByIdReject,
} from '@/api'
import { Alert, AlertAction, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Textarea } from '@/components/ui/textarea'
import { PageHeader } from '@/common/PageHeader'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'

// Bản ghi chờ xem lại (002 T059; US4 #14; FR-041c; hợp đồng /pending-review-records). Backend chỉ trả bản ghi người
// xem là người quyết định (Trưởng tầng → Người phụ trách ca → Quản lý viện khi không gắn tầng). Chấp nhận thì bản ghi
// có hiệu lực từ lúc chấp nhận; từ chối bắt buộc lý do, bản ghi được lưu nhưng không có hiệu lực.

const TEXT_MAX = 2000

const REASON_LABELS = {
  LATER_THAN_SYNC: 'Muộn hơn lúc đồng bộ',
  EARLIER_THAN_LAST_CONNECTION: 'Sớm hơn lần kết nối cuối',
  EXCEEDS_CFG_M15_08: 'Quá CFG-M15-08 kể từ lần kết nối cuối',
}

// Loại bản ghi ghi được khi mất mạng (OfflineHandler của từng module); loại chưa có nhãn thì hiện mã loại
const TYPE_LABELS = {
  'M07.KET_QUA_LIEU': 'Kết quả liều thuốc',
}

const typeLabel = (code) => TYPE_LABELS[code] ?? code
const floorLabel = (r) => (r.floorAreaId == null ? 'Không gắn tầng' : (r.floorAreaName ?? `Tầng #${r.floorAreaId}`))
const recorderLabel = (r) => r.recorderName ?? `Tài khoản #${r.recorderId}`

function valueText(value) {
  if (value === null || value === undefined) return 'trống'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function RecordDetail({ record, onDecided, onGone }) {
  const [rejecting, setRejecting] = useState(false)
  const [reason, setReason] = useState('')
  const [error, setError] = useState(null)
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)

  async function run(command, body, message) {
    setFailure(null)
    setError(null)
    setBusy(true)
    try {
      await command({ path: { id: record.id }, headers: { 'If-Match': `"${record.version}"` }, body })
      onDecided(record.id, message)
    } catch (e) {
      const p = problemOf(e)
      if (e instanceof ApiError && e.status === 409 && p.errorCode === 'ALREADY_PROCESSED') {
        onGone(record.id, 'Bản ghi đã được người khác quyết định.')
      } else if (e instanceof ApiError && e.status === 422) {
        setError(p.detail || p.title)
      } else {
        setFailure(p)
      }
    } finally {
      setBusy(false)
    }
  }

  function reject() {
    if (!reason.trim()) {
      setError('Nhập lý do từ chối')
      return
    }
    run(
      postPendingReviewRecordsByIdReject,
      { reason },
      'Đã từ chối bản ghi. Bản ghi được lưu nhưng không có hiệu lực.',
    )
  }

  const content = Object.entries(record.content ?? {})

  return (
    <>
      <SheetHeader className="gap-1 pt-6">
        <SheetTitle className="type-title">{typeLabel(record.recordType)}</SheetTitle>
        <SheetDescription>
          {recorderLabel(record)}, {floorLabel(record)}
        </SheetDescription>
      </SheetHeader>
      <div className="flex flex-col gap-5 overflow-y-auto px-4 pb-6 type-body-sm">
        <StatusBadge status="Chờ xem lại" className="self-start" />

        <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1">
          <dt className="text-muted-foreground">Giờ trên thiết bị</dt>
          <dd className="tabular-nums">{formatDateTime(record.deviceTime)}</dd>
          <dt className="text-muted-foreground">Kết nối cuối</dt>
          <dd className="tabular-nums">{formatDateTime(record.lastConnectedAt)}</dd>
          <dt className="text-muted-foreground">Đồng bộ lúc</dt>
          <dd className="tabular-nums">{formatDateTime(record.syncedAt)}</dd>
          <dt className="text-muted-foreground">Lý do chờ</dt>
          <dd>{REASON_LABELS[record.pendingReason] ?? record.pendingReason}</dd>
        </dl>

        {content.length > 0 && (
          <section className="flex flex-col gap-2">
            <h3 className="type-caption text-muted-foreground">Nội dung gốc từ thiết bị</h3>
            <ul className="flex flex-col gap-1">
              {content.map(([field, value]) => (
                <li key={field} className="break-words">
                  <span className="text-muted-foreground">{field}:</span> <span>{valueText(value)}</span>
                </li>
              ))}
            </ul>
          </section>
        )}

        <ProblemAlert problem={failure} />

        <div className="flex flex-wrap gap-2">
          <Button
            className="h-11"
            disabled={busy}
            onClick={() =>
              run(
                postPendingReviewRecordsByIdAccept,
                undefined,
                'Đã chấp nhận bản ghi. Bản ghi có hiệu lực từ lúc chấp nhận.',
              )
            }
          >
            Chấp nhận bản ghi
          </Button>
          {!rejecting && (
            <Button variant="outline" className="h-11" disabled={busy} onClick={() => setRejecting(true)}>
              Từ chối
            </Button>
          )}
        </div>

        {rejecting && (
          <section className="flex flex-col gap-3 rounded-xl border p-3">
            <Field data-invalid={!!error || undefined}>
              <FieldLabel htmlFor="pending-review-reason">Lý do từ chối</FieldLabel>
              <Textarea
                id="pending-review-reason"
                rows={3}
                maxLength={TEXT_MAX}
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                aria-invalid={!!error || undefined}
              />
              {error && <FieldError>{error}</FieldError>}
            </Field>
            <div className="flex flex-wrap gap-2">
              <Button variant="outline" className="h-11" disabled={busy} onClick={reject}>
                Xác nhận từ chối
              </Button>
              <Button variant="ghost" className="h-11" onClick={() => setRejecting(false)}>
                Không từ chối
              </Button>
            </div>
          </section>
        )}
      </div>
    </>
  )
}

export function PendingReviewPage() {
  const [state, setState] = useState({ status: 'loading', items: [], nextCursor: null })
  const [opened, setOpened] = useState(null)
  const [notice, setNotice] = useState('')

  const load = useCallback(async (cursor) => {
    setState((s) => (cursor ? { ...s, loadingMore: true } : { status: 'loading', items: [], nextCursor: null }))
    try {
      const { data } = await getPendingReviewRecords({ query: { cursor } })
      setState((s) => ({
        status: 'ready',
        items: cursor ? [...s.items, ...data.items] : data.items,
        nextCursor: data.nextCursor,
      }))
    } catch {
      setState({ status: 'error', items: [], nextCursor: null })
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải danh sách khi mở màn
    load()
  }, [load])

  // Đã quyết định (hoặc người khác đã quyết định): bỏ khỏi bảng, đóng khung, báo qua vùng aria-live
  const remove = useCallback((id, message) => {
    setState((s) => ({ ...s, items: s.items.filter((r) => r.id !== id) }))
    setOpened(null)
    setNotice(message)
  }, [])

  const record = state.items.find((r) => r.id === opened)

  return (
    <div className="mx-auto flex w-full max-w-[1600px] flex-col gap-6">
      <PageHeader
        title="Bản ghi chờ xem lại"
        subtitle="Bản ghi ghi khi mất mạng có thời điểm trên thiết bị không hợp lý. Bạn chấp nhận hoặc từ chối có lý do."
      />

      <section className="flex flex-col gap-3 rounded-2xl border bg-card p-4 md:p-5">
        <p aria-live="polite" className="type-body-sm empty:hidden">
          {notice}
        </p>
        {state.status === 'loading' && (
          <div aria-busy="true" className="flex flex-col gap-2">
            <span className="sr-only">Đang tải bản ghi</span>
            <Skeleton className="h-10" />
            <Skeleton className="h-10" />
            <Skeleton className="h-10" />
          </div>
        )}
        {state.status === 'error' && (
          <Alert role="alert">
            <AlertTitle>Không tải được bản ghi chờ xem lại</AlertTitle>
            <AlertAction>
              <Button variant="outline" size="sm" onClick={() => load()}>
                Thử lại
              </Button>
            </AlertAction>
          </Alert>
        )}
        {state.status === 'ready' && state.items.length === 0 && (
          <Empty>
            <EmptyHeader>
              <EmptyTitle>Không có bản ghi nào chờ bạn xem lại</EmptyTitle>
              <EmptyDescription>
                Bản ghi đồng bộ có thời điểm không hợp lý sẽ hiện ở đây và bạn được thông báo.
              </EmptyDescription>
            </EmptyHeader>
          </Empty>
        )}
        {state.status === 'ready' && state.items.length > 0 && (
          <>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Loại bản ghi</TableHead>
                  <TableHead>Người ghi</TableHead>
                  <TableHead>Tầng</TableHead>
                  <TableHead>Giờ trên thiết bị</TableHead>
                  <TableHead>Đồng bộ lúc</TableHead>
                  <TableHead className="min-w-44">Lý do chờ</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {state.items.map((r) => (
                  <TableRow key={r.id} className="align-top">
                    <TableCell>
                      <Button variant="link" className="h-auto p-0" onClick={() => setOpened(r.id)}>
                        {typeLabel(r.recordType)}
                      </Button>
                    </TableCell>
                    <TableCell>{recorderLabel(r)}</TableCell>
                    <TableCell>{floorLabel(r)}</TableCell>
                    <TableCell className="tabular-nums">{formatDateTime(r.deviceTime)}</TableCell>
                    <TableCell className="tabular-nums">{formatDateTime(r.syncedAt)}</TableCell>
                    <TableCell className="whitespace-normal">
                      {REASON_LABELS[r.pendingReason] ?? r.pendingReason}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            {state.nextCursor && (
              <Button
                variant="outline"
                className="self-start"
                disabled={state.loadingMore}
                onClick={() => load(state.nextCursor)}
              >
                Xem thêm
              </Button>
            )}
          </>
        )}
      </section>

      <Sheet open={!!record} onOpenChange={(open) => !open && setOpened(null)}>
        <SheetContent className="sm:max-w-lg">
          {record && <RecordDetail key={record.id} record={record} onDecided={remove} onGone={remove} />}
        </SheetContent>
      </Sheet>
    </div>
  )
}
