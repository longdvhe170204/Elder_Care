import { useEffect, useRef, useState } from 'react'
import {
  getMeShifts,
  getQualityChecksShiftsByShiftId,
  postQualityCheckItemsByIdResult,
  postQualityChecksShiftsByShiftId,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { DetailLayout } from '@/common/DetailLayout'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { ActivityField } from '@/features/activities/ActivityField'
import { todayInVietnam, toOffsetIso } from '@/features/activities/sessionView'

const ITEM_STATUS = {
  PENDING: 'Chờ kiểm tra',
  PASS: 'Đạt',
  FAIL: 'Không đạt',
  NOT_CHECKABLE: 'Không kiểm tra được',
  OVERDUE: 'Quá hạn kiểm tra',
}
const RESULTS = [
  ['PASS', 'Đạt'],
  ['FAIL', 'Không đạt'],
  ['NOT_CHECKABLE', 'Không kiểm tra được'],
]
const code = (id) => `CV-${id}`

// 014 T025 (FR-059 → FR-066; bố cục, schema GET duyệt 2026-11-07): công việc đủ điều kiện theo thứ tự mã, không xếp
// hạng hay gợi ý (Q-306, Q-307); mục kiểm tra mở khung chi tiết để ghi kết quả. Hợp đồng chỉ có mã nên hiện mã.
export function QualityCheckPage({ initialDate }) {
  const { status, can } = usePermissions()
  const permitted = can('QUALITY_CHECK', 'VIEW')
  const [date] = useState(() => initialDate ?? todayInVietnam())
  const [shifts, setShifts] = useState([])
  const [shiftId, setShiftId] = useState('')
  const [view, setView] = useState(null)
  const [failure, setFailure] = useState(null)
  const [revision, setRevision] = useState(0)
  const [chosen, setChosen] = useState([])
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)
  const [openItem, setOpenItem] = useState(null)
  const keys = useRef(new Map())
  const shift = shifts.find((s) => String(s.id) === shiftId)

  useEffect(() => {
    if (status !== 'ready' || !permitted) return
    let active = true
    getMeShifts({ query: { from: date, to: date } })
      .then(({ data }) => active && setShifts(data.filter((s) => !['DRAFT', 'CANCELLED'].includes(s.status))))
      .catch((error) => active && setFailure(problemOf(error)))
    return () => {
      active = false
    }
  }, [status, permitted, date])

  useEffect(() => {
    if (!shift) return
    let active = true
    getQualityChecksShiftsByShiftId({ path: { shiftId: shift.id }, query: { floorAreaId: shift.floorAreaId } })
      .then(({ data }) => active && (setView(data), setFailure(null)))
      .catch((error) => active && (setView(null), setFailure(problemOf(error))))
    return () => {
      active = false
    }
  }, [shift, revision])

  async function send(signature, run, done) {
    setBusy(true)
    setFailure(null)
    if (!keys.current.has(signature)) keys.current.set(signature, crypto.randomUUID())
    try {
      await run({ 'Idempotency-Key': keys.current.get(signature) })
      keys.current.delete(signature)
      setMessage(done)
      setRevision((v) => v + 1)
      return true
    } catch (error) {
      setFailure(problemOf(error))
      return false
    } finally {
      setBusy(false)
    }
  }

  if (status === 'loading') return <Skeleton className="h-24" />
  if (!permitted) return <PermissionDeniedState label="Kiểm tra chất lượng" />
  const item = view?.items.find((i) => i.id === openItem)
  const select = async () => {
    const body = { floorAreaId: shift.floorAreaId, careTaskIds: [...chosen].sort((a, b) => a - b) }
    const ok = await send(
      `select:${JSON.stringify(body)}`,
      (headers) => postQualityChecksShiftsByShiftId({ path: { shiftId: shift.id }, body, headers }),
      view.checklist ? 'Đã thêm mục kiểm tra' : 'Đã lập danh sách kiểm tra',
    )
    if (ok) setChosen([])
  }

  return (
    <DetailLayout
      open={Boolean(item)}
      detailKey={openItem}
      onClose={() => setOpenItem(null)}
      label="Ghi kết quả kiểm tra"
      renderDetail={({ Title, Description }) =>
        item && (
          <ResultForm
            key={item.id}
            item={item}
            busy={busy}
            failure={failure}
            Title={Title}
            Description={Description}
            onSave={(body) =>
              send(
                `result:${item.id}:${JSON.stringify(body)}`,
                (headers) => postQualityCheckItemsByIdResult({ path: { id: item.id }, body, headers }),
                'Đã lưu kết quả kiểm tra',
              ).then((ok) => ok && setOpenItem(null))
            }
          />
        )
      }
    >
      <PageHeader title="Kiểm tra chất lượng" subtitle="Chọn công việc đã hoàn thành của ca để kiểm tra" />
      <p role="status" className="sr-only">
        {message}
      </p>
      <Card>
        <CardContent className="flex flex-col gap-4">
          <div className="max-w-md">
            <ActivityField
              label="Ca"
              options={shifts.map((s) => [String(s.id), `${formatDateTime(s.startsAt)} – ${formatDateTime(s.endsAt)}`])}
              value={shiftId}
              onChange={(value) => {
                setShiftId(value)
                setChosen([])
                setView(null)
              }}
            />
          </div>
          {!openItem && <ProblemAlert problem={failure} />}
          {view && (
            <>
              <section className="flex flex-col gap-3">
                <div className="flex flex-wrap items-baseline justify-between gap-2">
                  <h2 className="type-card-title">Công việc đủ điều kiện ({view.eligibleTasks.length})</h2>
                  <p className="type-body-sm tabular-nums">
                    Cần chọn ít nhất {view.minimumItems} trên {view.eligibleTaskCount} công việc đủ điều kiện
                  </p>
                </div>
                <Table aria-label="Công việc đủ điều kiện">
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-12">Chọn</TableHead>
                      <TableHead>Công việc</TableHead>
                      <TableHead>Loại</TableHead>
                      <TableHead>Người cao tuổi, vị trí</TableHead>
                      <TableHead>Người thực hiện</TableHead>
                      <TableHead>Hoàn thành</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {[...view.eligibleTasks]
                      .sort((a, b) => a.taskId - b.taskId)
                      .map((t) => (
                        <TableRow key={t.taskId}>
                          <TableCell>
                            <input
                              type="checkbox"
                              className="size-5"
                              aria-label={`Chọn công việc ${code(t.taskId)}`}
                              checked={chosen.includes(t.taskId)}
                              disabled={busy}
                              onChange={(e) =>
                                setChosen((ids) =>
                                  e.target.checked ? [...ids, t.taskId] : ids.filter((id) => id !== t.taskId),
                                )
                              }
                            />
                          </TableCell>
                          <TableCell>{code(t.taskId)}</TableCell>
                          <TableCell>Loại {t.taskTypeId}</TableCell>
                          <TableCell>{t.residentId ? `NCT ${t.residentId}` : `Vị trí ${t.locationId}`}</TableCell>
                          <TableCell>{t.performerStaffId ? `NV-${t.performerStaffId}` : 'Chưa có'}</TableCell>
                          <TableCell className="tabular-nums">{formatDateTime(t.performedAt)}</TableCell>
                        </TableRow>
                      ))}
                  </TableBody>
                </Table>
                {can('QUALITY_CHECK', 'CREATE') && (
                  <div className="flex justify-end">
                    <Button size="lg" disabled={busy || chosen.length === 0} onClick={select}>
                      {view.checklist ? 'Thêm mục' : 'Lập danh sách'} ({chosen.length})
                    </Button>
                  </div>
                )}
              </section>
              <section className="flex flex-col gap-3 border-t pt-4">
                <h2 className="type-card-title">Mục kiểm tra</h2>
                {view.items.length === 0 ? (
                  <p className="type-body-sm">Ca này chưa có danh sách kiểm tra.</p>
                ) : (
                  <ul className="flex flex-col gap-2">
                    {view.items.map((i) => (
                      <li
                        key={i.id}
                        className="flex min-h-11 flex-wrap items-center justify-between gap-3 rounded-lg border p-3"
                      >
                        <span className="type-label">{code(i.taskId)}</span>
                        <StatusBadge status={ITEM_STATUS[i.status] ?? i.status} />
                        {i.status === 'PENDING' && can('QUALITY_CHECK', 'CREATE') && (
                          <Button
                            variant="outline"
                            size="lg"
                            aria-label={`Ghi kết quả công việc ${code(i.taskId)}`}
                            aria-pressed={openItem === i.id}
                            onClick={() => setOpenItem(i.id)}
                          >
                            Ghi kết quả
                          </Button>
                        )}
                      </li>
                    ))}
                  </ul>
                )}
              </section>
            </>
          )}
        </CardContent>
      </Card>
    </DetailLayout>
  )
}

function ResultForm({ item, busy, failure, onSave, Title, Description }) {
  const [result, setResult] = useState('')
  const [note, setNote] = useState('')
  const [reworkAt, setReworkAt] = useState('')
  const needsNote = result && result !== 'PASS'
  const ready = result && (!needsNote || note.trim()) && (result !== 'FAIL' || reworkAt)
  return (
    <form
      className="flex h-full flex-col gap-4"
      onSubmit={(e) => {
        e.preventDefault()
        onSave({
          result,
          ...(note.trim() ? { note: note.trim() } : {}),
          ...(result === 'FAIL' ? { reworkTask: { plannedAt: toOffsetIso(reworkAt) } } : {}),
        })
      }}
    >
      <div>
        <Title className="type-title">Công việc {code(item.taskId)}</Title>
        <Description className="type-body-sm">
          Ghi một kết quả cho mục kiểm tra; kết quả đã lưu không sửa trên màn này.
        </Description>
      </div>
      <fieldset className="flex flex-col gap-2">
        <legend className="type-label">Kết quả (bắt buộc)</legend>
        <div className="flex flex-wrap gap-2">
          {RESULTS.map(([value, text]) => (
            <label key={value} className="flex min-h-11 items-center gap-2 rounded-full border px-4">
              <input
                type="radio"
                name="result"
                value={value}
                checked={result === value}
                onChange={() => setResult(value)}
              />
              {text}
            </label>
          ))}
        </div>
      </fieldset>
      <ActivityField
        label="Ghi chú"
        type="textarea"
        value={note}
        onChange={setNote}
        required={Boolean(needsNote)}
        maxLength={2000}
      />
      {result === 'FAIL' && (
        <ActivityField
          label="Giờ làm lại dự kiến"
          type="datetime-local"
          value={reworkAt}
          onChange={setReworkAt}
          required
        />
      )}
      <ProblemAlert problem={failure} />
      <div className="mt-auto flex justify-end border-t pt-4">
        <Button type="submit" size="lg" disabled={busy || !ready}>
          Lưu kết quả
        </Button>
      </div>
    </form>
  )
}
