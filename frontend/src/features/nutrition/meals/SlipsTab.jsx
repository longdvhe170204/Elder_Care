import { useEffect, useState } from 'react'
import {
  getMealSlips,
  postMealSlipsByIdByCommand,
  postSpecialPortionsByIdAssignDish,
  postSpecialPortionsByIdConfirmServing,
  postSpecialPortionsByIdDiscardHeldPortion,
  postSpecialPortionsByIdMarkPrepared,
  postSpecialPortionsByIdServeHeldPortion,
  postSupplementaryPortionsByIdByCommand,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { AllergyNote } from '@/common/CareNotes'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { Info, ReasonField, SELECT } from '../NutritionParts'
import { useCommand, useLoad } from '../useNutrition'
import {
  CLOSING_FLAG,
  PORTION_FLAG,
  RESOLUTION,
  SLIP_COMMAND,
  SLIP_STATUS,
  TEXTURE,
  label,
  slipCommands,
} from '../nutritionView'

// Phiếu bữa ăn (011 T031, T033; FR-044 → FR-050, BR-M08-09 → 12, 14): bếp nhận phiếu không có dị ứng, bệnh lý (SC-008),
// đánh dấu chuẩn bị, giao; người nhận tại tầng nhận, báo sai lệch, xác nhận phục vụ suất đặc biệt (chặn khi sai người, dị
// ứng, hạn chế; đã ăn món dị ứng thì ghi sự cố 007 trong cùng lệnh). Nút theo trạng thái và quyền; backend kiểm lại.

function ServingForm({ portion, residents, onClose, onDone }) {
  const [residentId, setResidentId] = useState('')
  const [eaten, setEaten] = useState(null)
  const [incident, setIncident] = useState({ severity: '', symptoms: '', treatment: '' })
  const [submitted, setSubmitted] = useState(false)
  const { busy, failure, run } = useCommand()
  const allergen = failure?.errorCode === 'PORTION_HAS_ALLERGEN' || eaten !== null
  const set = (patch) => setIncident((i) => ({ ...i, ...patch }))

  async function submit() {
    setSubmitted(true)
    if (!residentId) return
    const body = { residentId: Number(residentId), deviceTime: new Date().toISOString() }
    if (allergen) {
      if (eaten === null) return
      body.hasEaten = eaten
      if (eaten) {
        if (!incident.severity.trim() || !incident.symptoms.trim()) return
        body.incident = {
          severity: incident.severity.trim(),
          symptoms: incident.symptoms.trim(),
          ...(incident.treatment.trim() && { treatment: incident.treatment.trim() }),
        }
      }
    }
    const ok = await run(postSpecialPortionsByIdConfirmServing, { path: { id: portion.id }, body })
    if (!ok) return
    if (!allergen) onDone('Đã xác nhận phục vụ')
    else onDone(eaten ? 'Đã ghi sự cố ăn món có thành phần dị ứng' : 'Đã ghi suất bị chặn, người cao tuổi chưa ăn')
  }

  return (
    <div className="flex flex-col gap-3 rounded-md border p-3">
      <h4 className="type-label">{`Xác nhận phục vụ suất của ${portion.fullName}`}</h4>
      <FieldGroup>
        <Field data-invalid={(submitted && !residentId) || undefined}>
          <FieldLabel htmlFor={`serve-resident-${portion.id}`}>Người đang được phục vụ</FieldLabel>
          <select
            id={`serve-resident-${portion.id}`}
            className={SELECT}
            value={residentId}
            onChange={(e) => setResidentId(e.target.value)}
          >
            <option value="">Chọn người cao tuổi</option>
            {residents.map((r) => (
              <option key={r.id} value={r.id}>
                {r.name}
              </option>
            ))}
          </select>
          {submitted && !residentId && <FieldError>Chọn đúng người đang được phục vụ</FieldError>}
        </Field>
        {allergen && (
          <>
            <Field data-invalid={(submitted && eaten === null) || undefined}>
              <FieldLabel>Người cao tuổi đã ăn món này chưa?</FieldLabel>
              <ToggleGroup
                variant="outline"
                value={eaten === null ? [] : [eaten ? 'yes' : 'no']}
                onValueChange={(n) => n[0] && setEaten(n[0] === 'yes')}
                aria-label="Người cao tuổi đã ăn món này chưa"
              >
                <ToggleGroupItem value="no" className="h-11 px-4">
                  Chưa ăn
                </ToggleGroupItem>
                <ToggleGroupItem value="yes" className="h-11 px-4">
                  Đã ăn
                </ToggleGroupItem>
              </ToggleGroup>
              {submitted && eaten === null && <FieldError>Chọn đã ăn hoặc chưa ăn</FieldError>}
            </Field>
            {eaten && (
              <>
                <Field data-invalid={(submitted && !incident.severity.trim()) || undefined}>
                  <FieldLabel htmlFor={`serve-severity-${portion.id}`}>Mức độ</FieldLabel>
                  <Input
                    id={`serve-severity-${portion.id}`}
                    className="h-11"
                    value={incident.severity}
                    onChange={(e) => set({ severity: e.target.value })}
                  />
                  {submitted && !incident.severity.trim() && <FieldError>Nhập mức độ</FieldError>}
                </Field>
                <ReasonField
                  id={`serve-symptoms-${portion.id}`}
                  label="Triệu chứng"
                  value={incident.symptoms}
                  onChange={(symptoms) => set({ symptoms })}
                  error={submitted && !incident.symptoms.trim() ? 'Nhập triệu chứng' : null}
                />
                <ReasonField
                  id={`serve-treatment-${portion.id}`}
                  label="Xử trí"
                  value={incident.treatment}
                  onChange={(treatment) => set({ treatment })}
                />
              </>
            )}
          </>
        )}
      </FieldGroup>
      <ProblemAlert problem={failure} />
      <div className="flex justify-end gap-2">
        <Button variant="ghost" className="h-11" onClick={onClose}>
          Thôi
        </Button>
        <Button className="h-11" disabled={busy} onClick={submit}>
          {allergen ? 'Ghi kết quả' : 'Xác nhận phục vụ'}
        </Button>
      </div>
    </div>
  )
}

function LabelRow({ slip, portion, dishes, residents, can, onChanged }) {
  const [mode, setMode] = useState(null)
  const [dishId, setDishId] = useState('')
  const [note, setNote] = useState('')
  const { busy, failure, run } = useCommand()
  const held = portion.flags?.includes('HELD_PORTION') || portion.flags?.includes('EXPECTED_RETURN')

  async function simple(call, message) {
    if (await run(call, { path: { id: portion.id } })) onChanged(message)
  }

  return (
    <li className="flex flex-col gap-2 rounded-md border p-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span className="type-label">{`${portion.fullName}, phòng ${portion.room ?? '—'}`}</span>
        {portion.prepared && <StatusBadge status="Đã chuẩn bị" />}
      </div>
      <span className="type-body-sm">{`${portion.diet}, kết cấu ${label(TEXTURE, portion.texture).toLowerCase()}: ${portion.dishes ?? '—'}`}</span>
      {portion.flags?.length > 0 && (
        <span className="type-caption text-muted-foreground">
          {portion.flags.map((f) => label(PORTION_FLAG, f)).join(', ')}
        </span>
      )}
      <ProblemAlert problem={failure} />
      {mode === 'serve' && (
        <ServingForm portion={portion} residents={residents} onClose={() => setMode(null)} onDone={onChanged} />
      )}
      {mode === 'assign' && (
        <div className="flex flex-col gap-2">
          <Field>
            <FieldLabel htmlFor={`assign-dish-${portion.id}`}>Món chỉ định</FieldLabel>
            <select
              id={`assign-dish-${portion.id}`}
              className={SELECT}
              value={dishId}
              onChange={(e) => setDishId(e.target.value)}
            >
              <option value="">Chọn món</option>
              {dishes
                .filter((d) => d.status === 'ACTIVE')
                .map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name}
                  </option>
                ))}
            </select>
          </Field>
          <Field>
            <FieldLabel htmlFor={`assign-note-${portion.id}`}>Ghi chú</FieldLabel>
            <Input
              id={`assign-note-${portion.id}`}
              className="h-11"
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
          </Field>
          <div className="flex justify-end gap-2">
            <Button variant="ghost" className="h-11" onClick={() => setMode(null)}>
              Thôi
            </Button>
            <Button
              className="h-11"
              disabled={busy || !dishId}
              onClick={async () => {
                const body = { dishId: Number(dishId), ...(note.trim() && { note: note.trim() }) }
                if (await run(postSpecialPortionsByIdAssignDish, { path: { id: portion.id }, body }))
                  onChanged('Đã chỉ định món')
              }}
            >
              Chỉ định món
            </Button>
          </div>
        </div>
      )}
      {mode === null && (
        <div className="flex flex-wrap justify-end gap-2">
          {can.kitchen && slip.status === 'CLOSED' && !portion.prepared && (
            <Button
              variant="outline"
              size="sm"
              disabled={busy}
              onClick={() => simple(postSpecialPortionsByIdMarkPrepared, `Đã chuẩn bị suất của ${portion.fullName}`)}
            >
              Đã chuẩn bị
            </Button>
          )}
          {can.doctor && (slip.status === 'CLOSED' || slip.status === 'PREPARED') && (
            <Button variant="outline" size="sm" onClick={() => setMode('assign')}>
              Chỉ định món…
            </Button>
          )}
          {can.receiver && held && (
            <>
              <Button
                variant="outline"
                size="sm"
                disabled={busy}
                onClick={() =>
                  simple(postSpecialPortionsByIdServeHeldPortion, `Đã phục vụ suất giữ của ${portion.fullName}`)
                }
              >
                Phục vụ suất giữ
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={busy}
                onClick={() =>
                  simple(postSpecialPortionsByIdDiscardHeldPortion, `Đã hủy suất giữ của ${portion.fullName}`)
                }
              >
                Hủy suất giữ
              </Button>
            </>
          )}
          {can.server && slip.status === 'RECEIVED' && (
            <Button size="sm" onClick={() => setMode('serve')}>
              Xác nhận phục vụ…
            </Button>
          )}
        </div>
      )}
    </li>
  )
}

function SlipDetail({ slip, dishes, residents, allergenName, residentName, can, onChanged, Title, Description }) {
  const [mode, setMode] = useState(null)
  const [content, setContent] = useState('')
  const [resolution, setResolution] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const { busy, failure, setFailure, run } = useCommand()
  const commands = slipCommands(slip, can)

  async function command(cmd) {
    setSubmitted(true)
    let body = {}
    if (cmd === 'report-discrepancy') {
      if (!content.trim()) return
      body = { content: content.trim() }
    }
    if (cmd === 'resolve-discrepancy') {
      if (!resolution) return
      body = { resolution, ...(content.trim() && { description: content.trim() }) }
    }
    if (await run(postMealSlipsByIdByCommand, { path: { id: slip.id, command: cmd }, body })) {
      setMode(null)
      setContent('')
      onChanged(`${SLIP_COMMAND[cmd].replace('…', '')}: xong`)
    }
  }

  function start(cmd) {
    setFailure(null)
    setSubmitted(false)
    if (cmd === 'report-discrepancy' || cmd === 'resolve-discrepancy') setMode(cmd)
    else command(cmd)
  }

  const allergyLines = (slip.allergies ?? []).map(
    (a) => `${residentName(a.residentId)}: ${a.allergenId ? allergenName(a.allergenId) : a.text}`,
  )

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <StatusBadge status={label(SLIP_STATUS, slip.status)} />
        <Title className="type-title">{`Phiếu ${slip.meal}, ${slip.floorArea}`}</Title>
        <Description className="type-body-sm text-muted-foreground">
          {(slip.flags ?? []).map((f) => label(CLOSING_FLAG, f)).join(', ') || 'Phiếu bữa ăn của tầng/khu'}
        </Description>
      </div>
      <AllergyNote allergies={allergyLines} />
      {(slip.needsCrossCheck ?? []).length > 0 && (
        <Info label="Cần đối chiếu dị ứng loại khác">{slip.needsCrossCheck.map(residentName).join(', ')}</Info>
      )}
      <section className="flex flex-col gap-2" aria-labelledby={`slip-diets-${slip.id}`}>
        <h3 id={`slip-diets-${slip.id}`} className="type-card-title">
          Số suất theo chế độ ăn
        </h3>
        <ul className="flex flex-col gap-1 type-body-sm">
          {(slip.portionsByDiet ?? []).map((p) => (
            <li key={p.diet} className="flex justify-between gap-2">
              <span>{p.diet}</span>
              <span className="tabular-nums">{p.portionCount}</span>
            </li>
          ))}
        </ul>
      </section>
      {(slip.labels ?? []).length > 0 && (
        <section className="flex flex-col gap-2" aria-labelledby={`slip-labels-${slip.id}`}>
          <h3 id={`slip-labels-${slip.id}`} className="type-card-title">
            Suất đặc biệt
          </h3>
          <ul className="flex flex-col gap-2">
            {slip.labels.map((p) => (
              <LabelRow
                key={p.id}
                slip={slip}
                portion={p}
                dishes={dishes}
                residents={residents}
                can={can}
                onChanged={onChanged}
              />
            ))}
          </ul>
        </section>
      )}
      {(slip.supplementaryPortions ?? []).length > 0 && (
        <section className="flex flex-col gap-2" aria-labelledby={`slip-extra-${slip.id}`}>
          <h3 id={`slip-extra-${slip.id}`} className="type-card-title">
            Phần bổ sung
          </h3>
          <ul className="flex flex-col gap-2">
            {slip.supplementaryPortions.map((s) => (
              <Supplement key={s.id} supplement={s} can={can} onChanged={onChanged} />
            ))}
          </ul>
        </section>
      )}
      <ProblemAlert problem={failure} />
      {mode && (
        <div className="flex flex-col gap-3 border-t pt-4">
          {mode === 'resolve-discrepancy' && (
            <Field data-invalid={(submitted && !resolution) || undefined}>
              <FieldLabel>Cách xử lý</FieldLabel>
              <ToggleGroup
                variant="outline"
                value={resolution ? [resolution] : []}
                onValueChange={(n) => n[0] && setResolution(n[0])}
                aria-label="Cách xử lý sai lệch"
              >
                {Object.entries(RESOLUTION).map(([code, text]) => (
                  <ToggleGroupItem key={code} value={code} className="h-11 px-4">
                    {text}
                  </ToggleGroupItem>
                ))}
              </ToggleGroup>
              {submitted && !resolution && <FieldError>Chọn cách xử lý</FieldError>}
            </Field>
          )}
          <ReasonField
            id={`slip-content-${slip.id}`}
            label={mode === 'report-discrepancy' ? 'Nội dung sai lệch' : 'Mô tả'}
            value={content}
            onChange={setContent}
            error={mode === 'report-discrepancy' && submitted && !content.trim() ? 'Nhập nội dung sai lệch' : null}
          />
          <div className="flex justify-end gap-2">
            <Button variant="ghost" className="h-11" onClick={() => setMode(null)}>
              Thôi
            </Button>
            <Button className="h-11" disabled={busy} onClick={() => command(mode)}>
              {SLIP_COMMAND[mode].replace('…', '')}
            </Button>
          </div>
        </div>
      )}
      {!mode && commands.length > 0 && (
        <div className="flex flex-wrap justify-end gap-2 border-t pt-4">
          {commands.map((cmd) => (
            <Button
              key={cmd}
              variant={cmd === 'deliver' || cmd === 'receive' ? 'default' : 'outline'}
              className="h-11"
              disabled={busy}
              onClick={() => start(cmd)}
            >
              {SLIP_COMMAND[cmd]}
            </Button>
          ))}
        </div>
      )}
    </div>
  )
}

function Supplement({ supplement, can, onChanged }) {
  const [reporting, setReporting] = useState(false)
  const [content, setContent] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const { busy, failure, run } = useCommand()
  async function send(command, body, message) {
    if (await run(postSupplementaryPortionsByIdByCommand, { path: { id: supplement.id, command }, body }))
      onChanged(message)
  }
  return (
    <li className="flex flex-col gap-2 rounded-md border p-3">
      <span className="type-body-sm">
        {`${supplement.portionCount} suất`}
        {supplement.deliveredAt ? `, giao ${formatDateTime(supplement.deliveredAt)}` : ', chưa giao'}
        {supplement.receivedAt ? `, nhận ${formatDateTime(supplement.receivedAt)}` : ''}
      </span>
      <ProblemAlert problem={failure} />
      {reporting && (
        <ReasonField
          id={`supplement-content-${supplement.id}`}
          label="Nội dung sai lệch"
          value={content}
          onChange={setContent}
          error={submitted && !content.trim() ? 'Nhập nội dung sai lệch' : null}
        />
      )}
      <div className="flex flex-wrap justify-end gap-2">
        {can.kitchen && !supplement.deliveredAt && (
          <Button size="sm" disabled={busy} onClick={() => send('deliver', {}, 'Đã giao phần bổ sung')}>
            Giao phần bổ sung
          </Button>
        )}
        {can.receiver && supplement.deliveredAt && !supplement.receivedAt && !reporting && (
          <>
            <Button size="sm" disabled={busy} onClick={() => send('receive', {}, 'Đã nhận phần bổ sung')}>
              Nhận phần bổ sung
            </Button>
            <Button variant="outline" size="sm" onClick={() => setReporting(true)}>
              Báo sai lệch…
            </Button>
          </>
        )}
        {reporting && (
          <>
            <Button variant="ghost" size="sm" onClick={() => setReporting(false)}>
              Thôi
            </Button>
            <Button
              size="sm"
              disabled={busy}
              onClick={() => {
                setSubmitted(true)
                if (content.trim())
                  send('report-discrepancy', { content: content.trim() }, 'Đã báo sai lệch phần bổ sung')
              }}
            >
              Báo sai lệch
            </Button>
          </>
        )}
      </div>
    </li>
  )
}

export function SlipsTab({ meals, dishes, residents, allergens, can, openedKey, open, announce }) {
  const closed = meals.filter((m) => m.closed)
  const [dailyMealId, setDailyMealId] = useState(closed[0]?.dailyMealId ?? null)
  const selected = closed.some((m) => m.dailyMealId === dailyMealId) ? dailyMealId : (closed[0]?.dailyMealId ?? null)
  const slips = useLoad(
    () => (selected ? getMealSlips({ query: { dailyMealId: selected } }).then((r) => r.data) : Promise.resolve([])),
    [selected],
  )
  const residentName = (id) => residents.find((r) => r.id === id)?.name ?? `Người cao tuổi mã ${id}`
  const allergenName = (id) => allergens.find((a) => a.id === id)?.name ?? `dị nguyên mã ${id}`

  function changed(message) {
    announce(message)
    slips.reload()
  }

  function openSlip(s) {
    open(`slip-${s.id}`, 'Chi tiết phiếu', (titles) => (
      <SlipDetail
        key={s.id}
        slip={s}
        dishes={dishes}
        residents={residents}
        allergenName={allergenName}
        residentName={residentName}
        can={can}
        onChanged={changed}
        {...titles}
      />
    ))
  }

  useEffect(() => {
    const s = slips.data?.find((x) => openedKey === `slip-${x.id}`)
    if (s) openSlip(s)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [slips.data])

  if (closed.length === 0)
    return (
      <p className="type-body-sm text-muted-foreground">
        Chưa có bữa nào được chốt trong ngày; phiếu được lập khi bếp chốt suất.
      </p>
    )
  return (
    <div className="flex flex-col gap-4">
      <Field className="max-w-sm">
        <FieldLabel htmlFor="slip-meal">Bữa</FieldLabel>
        <select
          id="slip-meal"
          className={SELECT}
          value={selected ?? ''}
          onChange={(e) => setDailyMealId(Number(e.target.value))}
        >
          {closed.map((m) => (
            <option key={m.dailyMealId} value={m.dailyMealId}>
              {m.mealName}
            </option>
          ))}
        </select>
      </Field>
      <ProblemAlert problem={slips.failure} />
      {slips.data === null ? (
        !slips.failure && <Skeleton className="h-24 w-full" />
      ) : slips.data.length === 0 ? (
        <p className="type-body-sm text-muted-foreground">Không có phiếu nào của bữa này trong phạm vi của bạn.</p>
      ) : (
        <Card className="py-0">
          <Table aria-label="Phiếu bữa ăn">
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="pl-4">Tầng/khu</TableHead>
                <TableHead>Tổng suất</TableHead>
                <TableHead>Suất đặc biệt</TableHead>
                <TableHead className="pr-4">Trạng thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {slips.data.map((s) => (
                <TableRow key={s.id} className={openedKey === `slip-${s.id}` ? 'bg-accent/40' : undefined}>
                  <TableCell className="pl-4">
                    <button
                      type="button"
                      className="text-left underline-offset-4 hover:underline"
                      aria-pressed={openedKey === `slip-${s.id}`}
                      onClick={() => openSlip(s)}
                    >
                      {s.floorArea}
                    </button>
                  </TableCell>
                  <TableCell className="tabular-nums">
                    {(s.portionsByDiet ?? []).reduce((n, p) => n + p.portionCount, 0)}
                  </TableCell>
                  <TableCell className="tabular-nums">{(s.labels ?? []).length}</TableCell>
                  <TableCell className="pr-4">
                    <StatusBadge status={label(SLIP_STATUS, s.status)} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Card>
      )}
    </div>
  )
}
