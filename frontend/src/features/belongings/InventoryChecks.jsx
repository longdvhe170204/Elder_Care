import { useCallback, useEffect, useRef, useState } from 'react'
import {
  getBelongingInventoryChecks,
  getBelongingInventoryChecksById,
  postBelongingInventoryChecks,
  postBelongingInventoryChecksByIdByCommand,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDate } from '@/common/dateTime'
import { uploadFile } from '@/common/files'
import { problemOf } from '@/common/problem'
import { FilesField, SelectField } from './BelongingFields'
import { activeOptions } from './belongingLabels'

// Kiểm kê đồ có giá trị (013 T026, US5; FR-028a → FR-028c, BR-M12-07; api-013 /belonging-inventory-checks): kỳ hiện tại
// và hạn, các vị trí có đồ có giá trị Đang giữ kèm phiếu của kỳ (không xếp ưu tiên, không gợi ý vị trí nào lập trước,
// Q-306, Q-307), Hành chính lập phiếu cho một vị trí, hoàn thành (kết quả từng đồ, đồ thất lạc tìm thấy tại vị trí, đồ
// không rõ chủ) hoặc hủy có lý do. Backend sinh bản ghi bàn giao cho đồ không thấy, hư hỏng, tìm thấy.

const CHECK_STATUS = { WAITING_CHECK: 'Chờ kiểm kê', COMPLETED: 'Hoàn thành', CANCELLED: 'Đã hủy' }
const RESULTS = [
  { value: 'MATCHED', label: 'Đúng' },
  { value: 'CONDITION_MISMATCH', label: 'Lệch tình trạng' },
  { value: 'NOT_FOUND', label: 'Không thấy' },
]
const RESULT_LABELS = { ...Object.fromEntries(RESULTS.map((r) => [r.value, r.label])), DROPPED: 'Đã rời vị trí' }

function CheckDetail({ checkId, catalogs, onChanged }) {
  const [check, setCheck] = useState(null)
  const [failure, setFailure] = useState(null)
  const [lines, setLines] = useState({})
  const [found, setFound] = useState([])
  const [unknown, setUnknown] = useState([])
  const [reason, setReason] = useState('')
  const [errors, setErrors] = useState({})
  const [busy, setBusy] = useState(false)
  const keys = useRef({ complete: crypto.randomUUID(), cancel: crypto.randomUUID() })

  const load = useCallback(async () => {
    try {
      const { data } = await getBelongingInventoryChecksById({ path: { id: checkId } })
      setCheck(data)
    } catch (error) {
      setFailure(problemOf(error))
    }
  }, [checkId])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải phiếu khi mở
    load()
  }, [load])

  if (!check) return failure ? <ProblemAlert problem={failure} /> : <Skeleton className="h-24 w-full" />
  const waiting = check.status === 'WAITING_CHECK'
  const setLine = (id, patch) => setLines((l) => ({ ...l, [id]: { ...l[id], ...patch } }))

  async function command(name, body) {
    setFailure(null)
    setBusy(true)
    try {
      await postBelongingInventoryChecksByIdByCommand({
        path: { id: check.id, command: name },
        headers: { 'Idempotency-Key': keys.current[name] },
        body,
      })
      onChanged(name === 'complete' ? 'Đã hoàn thành phiếu kiểm kê.' : 'Đã hủy phiếu kiểm kê.')
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  async function complete() {
    const e = {}
    for (const l of check.content.lines) if (!lines[l.belongingId]?.result) e[l.belongingId] = 'Chọn kết quả.'
    setErrors(e)
    if (Object.keys(e).length > 0) return
    const upload = async (files = []) => {
      const ids = []
      for (const f of files) ids.push(await uploadFile(f))
      return ids
    }
    const body = { lines: [], found: [], unknownOwnerItems: [] }
    for (const l of check.content.lines) {
      const x = lines[l.belongingId]
      const photoFileIds = await upload(x.photos)
      body.lines.push({
        belongingId: l.belongingId,
        result: x.result,
        ...(x.result === 'CONDITION_MISMATCH' && x.levelId && { newCondition: { levelId: Number(x.levelId) } }),
        ...(x.result === 'CONDITION_MISMATCH' && x.damage && { recordDamage: true }),
        ...(photoFileIds.length > 0 && { photoFileIds }),
      })
    }
    for (const f of found.filter((f) => f.belongingId)) {
      const photoFileIds = await upload(f.photos)
      body.found.push({ belongingId: Number(f.belongingId), ...(photoFileIds.length > 0 && { photoFileIds }) })
    }
    for (const u of unknown.filter((u) => u.description.trim())) {
      const photoFileIds = await upload(u.photos)
      body.unknownOwnerItems.push({
        description: u.description.trim(),
        ...(photoFileIds.length > 0 && { photoFileIds }),
      })
    }
    command('complete', body)
  }

  return (
    <section className="flex flex-col gap-4 rounded-xl border p-3" aria-label="Phiếu kiểm kê">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h3 className="type-card-title">
          Phiếu {catalogs.locationName(check.content.storageLocationId)}, kỳ {check.content.periodCode}
        </h3>
        <StatusBadge status={CHECK_STATUS[check.status] ?? check.status} />
      </div>
      <ul className="flex flex-col gap-3">
        {check.content.lines.map((l) => (
          <li key={l.belongingId} className="flex flex-col gap-2 rounded-lg border p-3">
            <span className="type-label">
              Đồ #{l.belongingId}, người cao tuổi #{l.residentId}, số lượng lúc lập {l.amountAtCreation}
            </span>
            {waiting ? (
              <>
                <SelectField
                  id={`line-result-${l.belongingId}`}
                  label={`Kết quả đồ #${l.belongingId}`}
                  value={lines[l.belongingId]?.result ?? ''}
                  onChange={(result) => setLine(l.belongingId, { result })}
                  options={RESULTS}
                  error={errors[l.belongingId]}
                />
                {lines[l.belongingId]?.result === 'CONDITION_MISMATCH' && (
                  <>
                    <SelectField
                      id={`line-level-${l.belongingId}`}
                      label={`Mức mới của đồ #${l.belongingId}`}
                      value={lines[l.belongingId]?.levelId ?? ''}
                      onChange={(levelId) => setLine(l.belongingId, { levelId })}
                      options={activeOptions(catalogs.levels)}
                    />
                    <label className="flex min-h-10 cursor-pointer items-center gap-2">
                      <Checkbox
                        checked={!!lines[l.belongingId]?.damage}
                        onCheckedChange={(damage) => setLine(l.belongingId, { damage })}
                        aria-label={`Ghi hư hỏng đồ #${l.belongingId}`}
                      />
                      <span className="type-body-sm">Ghi hư hỏng</span>
                    </label>
                  </>
                )}
                <FilesField
                  id={`line-photos-${l.belongingId}`}
                  label={`Ảnh đồ #${l.belongingId}`}
                  files={lines[l.belongingId]?.photos ?? []}
                  onChange={(photos) => setLine(l.belongingId, { photos })}
                  multiple
                />
              </>
            ) : (
              <span className="type-body-sm text-muted-foreground">
                Kết quả: {RESULT_LABELS[l.result] ?? 'Chưa có'}
              </span>
            )}
          </li>
        ))}
      </ul>
      {waiting && (
        <>
          <fieldset className="flex flex-col gap-2">
            <legend className="type-label">Đồ thất lạc tìm thấy tại vị trí</legend>
            {found.map((f, i) => (
              <div key={i} className="grid gap-3 sm:grid-cols-2">
                <Field>
                  <FieldLabel htmlFor={`found-id-${i}`}>Mã đồ tìm thấy {i + 1}</FieldLabel>
                  <Input
                    id={`found-id-${i}`}
                    inputMode="numeric"
                    className="h-11"
                    value={f.belongingId}
                    onChange={(e) =>
                      setFound((l) => l.map((x, j) => (j === i ? { ...x, belongingId: e.target.value } : x)))
                    }
                  />
                </Field>
                <FilesField
                  id={`found-photos-${i}`}
                  label={`Ảnh đồ tìm thấy ${i + 1}`}
                  files={f.photos}
                  onChange={(photos) => setFound((l) => l.map((x, j) => (j === i ? { ...x, photos } : x)))}
                  multiple
                  hint="Bắt buộc với đồ có giá trị."
                />
              </div>
            ))}
            <Button
              type="button"
              variant="outline"
              className="h-11 self-start"
              onClick={() => setFound((l) => [...l, { belongingId: '', photos: [] }])}
            >
              Thêm đồ tìm thấy
            </Button>
          </fieldset>
          <fieldset className="flex flex-col gap-2">
            <legend className="type-label">Đồ không rõ chủ</legend>
            {unknown.map((u, i) => (
              <div key={i} className="grid gap-3 sm:grid-cols-2">
                <Field>
                  <FieldLabel htmlFor={`unknown-${i}`}>Mô tả đồ không rõ chủ {i + 1}</FieldLabel>
                  <Input
                    id={`unknown-${i}`}
                    className="h-11"
                    value={u.description}
                    onChange={(e) =>
                      setUnknown((l) => l.map((x, j) => (j === i ? { ...x, description: e.target.value } : x)))
                    }
                  />
                </Field>
                <FilesField
                  id={`unknown-photos-${i}`}
                  label={`Ảnh đồ không rõ chủ ${i + 1}`}
                  files={u.photos}
                  onChange={(photos) => setUnknown((l) => l.map((x, j) => (j === i ? { ...x, photos } : x)))}
                  multiple
                />
              </div>
            ))}
            <Button
              type="button"
              variant="outline"
              className="h-11 self-start"
              onClick={() => setUnknown((l) => [...l, { description: '', photos: [] }])}
            >
              Thêm đồ không rõ chủ
            </Button>
          </fieldset>
          <ProblemAlert problem={failure} />
          <Button className="h-11 self-start" disabled={busy} onClick={complete}>
            Hoàn thành kiểm kê
          </Button>
          <Field>
            <FieldLabel htmlFor={`cancel-${check.id}`}>Lý do hủy phiếu</FieldLabel>
            <Textarea
              id={`cancel-${check.id}`}
              rows={2}
              maxLength={2000}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
            {errors.reason && <FieldError>{errors.reason}</FieldError>}
          </Field>
          <Button
            variant="destructive"
            className="h-11 self-start"
            disabled={busy}
            onClick={() => {
              if (!reason.trim()) return setErrors({ reason: 'Nhập lý do hủy.' })
              command('cancel', { reason: reason.trim() })
            }}
          >
            Hủy phiếu
          </Button>
        </>
      )}
    </section>
  )
}

export function InventoryChecks({ catalogs }) {
  const [state, setState] = useState({ status: 'loading' })
  const [opened, setOpened] = useState(null)
  const [notice, setNotice] = useState('')
  const [failure, setFailure] = useState(null)

  const load = useCallback(async () => {
    try {
      const { data } = await getBelongingInventoryChecks()
      setState({ status: 'ready', ...data })
    } catch (error) {
      setState({ status: 'error', problem: problemOf(error) })
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải kỳ kiểm kê khi mở tab
    load()
  }, [load])

  async function create(storageLocationId) {
    setFailure(null)
    try {
      const { data } = await postBelongingInventoryChecks({
        headers: { 'Idempotency-Key': crypto.randomUUID() },
        body: { storageLocationId },
      })
      setNotice('Đã lập phiếu kiểm kê.')
      setOpened(data.id)
      load()
    } catch (error) {
      setFailure(problemOf(error))
    }
  }

  if (state.status === 'loading') return <Skeleton className="h-24 w-full" />
  if (state.status === 'error') return <ProblemAlert problem={state.problem} />

  const checkOf = (id) => state.checks.find((c) => c.id === id)
  return (
    <Card>
      <CardContent className="flex flex-col gap-4">
        <h2 className="type-title">
          Kỳ {state.periodCode}, hạn hoàn thành {formatDate(state.dueDate)}
        </h2>
        <div role="status" className="type-body-sm text-success-foreground">
          {notice}
        </div>
        <ProblemAlert problem={failure} />
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Vị trí</TableHead>
              <TableHead>Đồ có giá trị đang giữ</TableHead>
              <TableHead>Phiếu của kỳ</TableHead>
              <TableHead>Thao tác</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {state.locations.map((l) => {
              const check = l.checkId != null ? checkOf(l.checkId) : null
              const name = catalogs.locationName(l.storageLocationId)
              return (
                <TableRow key={l.storageLocationId}>
                  <TableCell>{name}</TableCell>
                  <TableCell className="tabular-nums">{l.valuableCount}</TableCell>
                  <TableCell>
                    {check ? (
                      <span className="flex flex-wrap gap-2">
                        <StatusBadge status={CHECK_STATUS[check.status] ?? check.status} />
                        {check.late && <StatusBadge status="Trễ hạn" />}
                      </span>
                    ) : (
                      'Chưa lập'
                    )}
                  </TableCell>
                  <TableCell>
                    {l.checkId != null ? (
                      <Button
                        variant="outline"
                        size="sm"
                        className="h-10"
                        aria-label={`Mở phiếu ${name}`}
                        onClick={() => setOpened(l.checkId)}
                      >
                        Mở phiếu
                      </Button>
                    ) : (
                      <Button
                        variant="outline"
                        size="sm"
                        className="h-10"
                        aria-label={`Lập phiếu cho ${name}`}
                        onClick={() => create(l.storageLocationId)}
                      >
                        Lập phiếu
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              )
            })}
          </TableBody>
        </Table>
        {opened != null && (
          <CheckDetail
            key={opened}
            checkId={opened}
            catalogs={catalogs}
            onChanged={(message) => {
              setNotice(message)
              setOpened(null)
              load()
            }}
          />
        )}
      </CardContent>
    </Card>
  )
}
