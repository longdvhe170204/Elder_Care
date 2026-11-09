import { useEffect, useState } from 'react'
import {
  getDailyMealsByIdAdjustments,
  getDailyMealsByIdPrecomputed,
  postAdjustmentsByIdConfirm,
  postDailyMealsByIdClosePortions,
  postDailyMealsByIdExtraPortions,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { Info, ReasonField, SELECT } from '../NutritionParts'
import { useCommand, useLoad } from '../useNutrition'
import { ADJUSTMENT_CHANGE, CLOSING_FLAG, MISSING_DATA, floorName, label } from '../nutritionView'

// Bữa trong ngày (011 T027; FR-033 → FR-043, BR-M08-01, BR-M08-13): tính sẵn chỉ để xem, nút "Chốt suất" tách riêng và
// hỏi lại trước khi chốt (mỗi bữa chỉ một lần, hệ thống không tự chốt; Q-306). Phát sinh sau chốt chờ bếp xác nhận;
// người nhận tại tầng yêu cầu suất bổ sung sau giờ bữa.

function timeOf(iso) {
  return iso ? formatDateTime(iso).slice(11) : null
}

function ClosingTable({ result, floors, dietName }) {
  const rows = result?.portionsByFloorArea ?? []
  if (rows.length === 0)
    return <p className="type-body-sm text-muted-foreground">Chưa có người được tính suất ở bữa này.</p>
  return (
    <Table aria-label="Số suất theo tầng/khu và chế độ ăn">
      <TableHeader>
        <TableRow className="hover:bg-transparent">
          <TableHead>Tầng/khu</TableHead>
          <TableHead>Chế độ ăn</TableHead>
          <TableHead className="text-right">Số suất</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {rows.map((p) => (
          <TableRow key={`${p.floorAreaId}-${p.dietId}`}>
            <TableCell>{floorName(floors, p.floorAreaId)}</TableCell>
            <TableCell>{dietName(p.dietId)}</TableCell>
            <TableCell className="text-right tabular-nums">{p.portionCount}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}

function ExtraPortionForm({ meal, floors, residents, onClose, onDone }) {
  const [form, setForm] = useState({ residentId: '', floorAreaId: '', reason: '' })
  const [submitted, setSubmitted] = useState(false)
  const { busy, failure, run } = useCommand()
  const set = (patch) => setForm((f) => ({ ...f, ...patch }))
  const errors = {
    residentId: !form.residentId ? 'Chọn người cao tuổi' : null,
    floorAreaId: !form.floorAreaId ? 'Chọn tầng/khu' : null,
    reason: !form.reason.trim() ? 'Nhập lý do' : null,
  }
  const show = (k) => (submitted ? errors[k] : null)
  return (
    <div className="flex flex-col gap-3 border-t pt-4">
      <h3 className="type-card-title">Yêu cầu suất bổ sung</h3>
      <FieldGroup>
        <Field data-invalid={!!show('residentId') || undefined}>
          <FieldLabel htmlFor="extra-resident">Người cao tuổi</FieldLabel>
          <select
            id="extra-resident"
            className={SELECT}
            value={form.residentId}
            onChange={(e) => set({ residentId: e.target.value })}
          >
            <option value="">Chọn người cao tuổi</option>
            {residents.map((r) => (
              <option key={r.id} value={r.id}>
                {r.name}
              </option>
            ))}
          </select>
          {show('residentId') && <FieldError>{show('residentId')}</FieldError>}
        </Field>
        <Field data-invalid={!!show('floorAreaId') || undefined}>
          <FieldLabel htmlFor="extra-floor">Tầng/khu nhận</FieldLabel>
          <select
            id="extra-floor"
            className={SELECT}
            value={form.floorAreaId}
            onChange={(e) => set({ floorAreaId: e.target.value })}
          >
            <option value="">Chọn tầng/khu</option>
            {floors.map((f) => (
              <option key={f.id} value={f.id}>
                {f.name}
              </option>
            ))}
          </select>
          {show('floorAreaId') && <FieldError>{show('floorAreaId')}</FieldError>}
        </Field>
        <ReasonField
          id="extra-reason"
          value={form.reason}
          onChange={(reason) => set({ reason })}
          error={show('reason')}
        />
      </FieldGroup>
      <ProblemAlert problem={failure} />
      <div className="flex justify-end gap-2">
        <Button variant="ghost" className="h-11" onClick={onClose}>
          Thôi
        </Button>
        <Button
          className="h-11"
          disabled={busy}
          onClick={async () => {
            setSubmitted(true)
            if (Object.values(errors).some(Boolean)) return
            const body = {
              residentId: Number(form.residentId),
              floorAreaId: Number(form.floorAreaId),
              reason: form.reason.trim(),
            }
            if (await run(postDailyMealsByIdExtraPortions, { path: { id: meal.dailyMealId }, body }))
              onDone('Đã gửi yêu cầu suất bổ sung tới bếp')
          }}
        >
          Gửi yêu cầu suất bổ sung
        </Button>
      </div>
    </div>
  )
}

function DailyMealDetail({ meal, floors, diets, residents, kitchen, receiver, onChanged, Title, Description }) {
  const result = useLoad(
    () => getDailyMealsByIdPrecomputed({ path: { id: meal.dailyMealId } }).then((r) => r.data),
    [meal.dailyMealId, meal.closed],
  )
  const adjustments = useLoad(
    () =>
      meal.closed
        ? getDailyMealsByIdAdjustments({ path: { id: meal.dailyMealId } }).then((r) => r.data)
        : Promise.resolve([]),
    [meal.dailyMealId, meal.closed],
  )
  const [mode, setMode] = useState(null)
  const { busy, failure, run } = useCommand()
  const dietName = (id) => diets.find((d) => d.id === id)?.name ?? `Chế độ ăn mã ${id}`
  const r = result.data

  async function close() {
    if (await run(postDailyMealsByIdClosePortions, { path: { id: meal.dailyMealId } })) {
      setMode(null)
      onChanged(`Đã chốt suất ${meal.mealName}`)
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <StatusBadge status={meal.closed ? 'Đã chốt' : 'Chưa chốt'} />
        <Title className="type-title">{meal.mealName}</Title>
        <Description className="type-body-sm text-muted-foreground">
          {meal.closed
            ? 'Số suất đã chốt'
            : 'Tính sẵn theo dữ liệu lúc này, chỉ để xem; lệnh chốt tính lại tại lúc chốt.'}
        </Description>
      </div>
      <div className="grid gap-3">
        <Info label="Giờ dự kiến">{timeOf(meal.expectedTime) ?? 'Viện chưa đặt giờ (CFG-M08-04)'}</Info>
        {!meal.closed && <Info label="Mở chốt từ">{timeOf(meal.closingOpensAt)}</Info>}
      </div>
      <ProblemAlert problem={result.failure} />
      {r === null ? (
        !result.failure && <Skeleton className="h-24 w-full" />
      ) : (
        <>
          {(r.flags?.length > 0 || r.missingData?.length > 0) && (
            <Alert variant="attention">
              <AlertTitle>Cần chú ý</AlertTitle>
              <AlertDescription>
                <ul className="ml-4 list-disc">
                  {(r.flags ?? []).map((f) => (
                    <li key={f}>{label(CLOSING_FLAG, f)}</li>
                  ))}
                  {(r.missingData ?? []).map((m) => (
                    <li key={m}>{label(MISSING_DATA, m)}</li>
                  ))}
                </ul>
              </AlertDescription>
            </Alert>
          )}
          <ClosingTable result={r} floors={floors} dietName={dietName} />
          <div className="grid gap-3 sm:grid-cols-2">
            <Info label="Suất đặc biệt">{r.specialPortions}</Info>
            <Info label="Suất người thân ở lại">{r.familyMealPortions}</Info>
          </div>
        </>
      )}
      {meal.closed && (adjustments.data ?? []).length > 0 && (
        <section className="flex flex-col gap-2 border-t pt-4" aria-labelledby="meal-adjustments">
          <h3 id="meal-adjustments" className="type-card-title">
            Phát sinh sau chốt
          </h3>
          <ul className="flex flex-col gap-2">
            {adjustments.data.map((a) => (
              <li key={a.id} className="flex flex-wrap items-center justify-between gap-2 rounded-md border p-3">
                <span className="type-body-sm">
                  {`${label(ADJUSTMENT_CHANGE, a.changeType)} ${a.portionCount > 0 ? '+' : ''}${a.portionCount}, ${floorName(floors, a.floorAreaId)}, ${dietName(a.dietId)}`}
                  {a.needsDelivery ? ', cần giao' : ''}
                </span>
                {a.kitchenConfirmedAt ? (
                  <StatusBadge status="Đã xác nhận" />
                ) : kitchen ? (
                  <Button
                    size="sm"
                    disabled={busy}
                    aria-label={`Xác nhận phát sinh ${floorName(floors, a.floorAreaId)}`}
                    onClick={async () => {
                      if (await run(postAdjustmentsByIdConfirm, { path: { id: a.id } })) {
                        adjustments.reload()
                        onChanged('Đã xác nhận phát sinh')
                      }
                    }}
                  >
                    Xác nhận
                  </Button>
                ) : (
                  <StatusBadge status="Chờ xác nhận" />
                )}
              </li>
            ))}
          </ul>
        </section>
      )}
      <ProblemAlert problem={failure} />
      {mode === 'extra' && (
        <ExtraPortionForm
          meal={meal}
          floors={floors}
          residents={residents}
          onClose={() => setMode(null)}
          onDone={(message) => {
            setMode(null)
            adjustments.reload()
            onChanged(message)
          }}
        />
      )}
      {mode === 'close' && (
        <div className="flex flex-col gap-3 border-t pt-4">
          <p className="type-body-sm">{`Chốt suất ${meal.mealName}? Số suất được tính lại tại lúc chốt và lập phiếu cho mỗi tầng/khu; mỗi bữa chỉ chốt một lần.`}</p>
          <div className="flex justify-end gap-2">
            <Button variant="ghost" className="h-11" onClick={() => setMode(null)}>
              Thôi
            </Button>
            <Button className="h-11" disabled={busy} onClick={close}>
              Chốt suất
            </Button>
          </div>
        </div>
      )}
      {mode === null && (
        <div className="flex flex-wrap justify-end gap-2 border-t pt-4">
          {receiver && meal.closed && (
            <Button variant="outline" className="h-11" onClick={() => setMode('extra')}>
              Yêu cầu suất bổ sung…
            </Button>
          )}
          {kitchen && !meal.closed && (
            <Button className="h-11" onClick={() => setMode('close')}>
              Chốt suất…
            </Button>
          )}
        </div>
      )}
    </div>
  )
}

export function DayTab({ meals, floors, diets, residents, kitchen, receiver, openedKey, open, announce, reload }) {
  function changed(message) {
    announce(message)
    reload()
  }

  function openMeal(m) {
    open(`day-${m.dailyMealId}`, 'Chi tiết bữa', (titles) => (
      <DailyMealDetail
        key={m.dailyMealId}
        meal={m}
        floors={floors}
        diets={diets}
        residents={residents}
        kitchen={kitchen}
        receiver={receiver}
        onChanged={changed}
        {...titles}
      />
    ))
  }

  useEffect(() => {
    const m = meals.find((x) => openedKey === `day-${x.dailyMealId}`)
    if (m) openMeal(m)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [meals])

  if (meals.length === 0)
    return <p className="type-body-sm text-muted-foreground">Ngày này không có bữa nào trong danh mục.</p>
  return (
    <Card className="py-0">
      <Table aria-label="Bữa trong ngày">
        <TableHeader>
          <TableRow className="hover:bg-transparent">
            <TableHead className="pl-4">Bữa</TableHead>
            <TableHead>Giờ dự kiến</TableHead>
            <TableHead>Mở chốt từ</TableHead>
            <TableHead className="pr-4">Trạng thái</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {meals.map((m) => (
            <TableRow key={m.dailyMealId} className={openedKey === `day-${m.dailyMealId}` ? 'bg-accent/40' : undefined}>
              <TableCell className="pl-4">
                <button
                  type="button"
                  className="text-left underline-offset-4 hover:underline"
                  aria-pressed={openedKey === `day-${m.dailyMealId}`}
                  onClick={() => openMeal(m)}
                >
                  {m.mealName}
                </button>
              </TableCell>
              <TableCell className="tabular-nums">{timeOf(m.expectedTime) ?? 'Chưa đặt'}</TableCell>
              <TableCell className="tabular-nums">{m.closed ? '—' : (timeOf(m.closingOpensAt) ?? '—')}</TableCell>
              <TableCell className="pr-4">
                <StatusBadge status={m.closed ? 'Đã chốt' : 'Chưa chốt'} />
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Card>
  )
}
