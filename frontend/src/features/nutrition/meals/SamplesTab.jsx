import { useState } from 'react'
import {
  getDailyMealsByIdRetainedSamples,
  postDailyMealsByIdRetainedSamples,
  postRetainedSamplesByIdDiscard,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { Info, SELECT } from '../NutritionParts'
import { useCommand, useLoad } from '../useNutrition'

// Lưu mẫu thức ăn (011 T039; FR-060 → FR-062, BR-M08-15): bếp ghi mẫu của mỗi bữa đã chốt, món nào không lưu thì ghi lý
// do; món phát sinh sau ghi thêm lần sau. Hủy mẫu là lệnh của bếp, hệ thống chỉ nhắc khi quá hạn lưu (CFG-M08-06).

const SAMPLE_STATUS = { RECORDED: 'Đang lưu', DISCARDED: 'Đã hủy mẫu' }

function SampleForm({ dailyMealId, view, kitchen, onChanged }) {
  const pending = view.dishes.filter((d) => !d.recorded)
  const [entries, setEntries] = useState(() =>
    Object.fromEntries(pending.map((d) => [d.dishId, { retained: true, reason: '' }])),
  )
  const [location, setLocation] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const { busy, failure, run } = useCommand()
  const missingReason = (id) => !entries[id]?.retained && !entries[id]?.reason.trim()
  const set = (id, patch) => setEntries((e) => ({ ...e, [id]: { ...e[id], ...patch } }))

  if (!kitchen || pending.length === 0 || view.sample?.status === 'DISCARDED') return null
  return (
    <div className="flex flex-col gap-3 border-t pt-4">
      <h3 className="type-card-title">{view.sample ? 'Ghi thêm món mới' : 'Ghi lưu mẫu'}</h3>
      {!view.sample && (
        <Field>
          <FieldLabel htmlFor="sample-location">Nơi lưu</FieldLabel>
          <Input id="sample-location" className="h-11" value={location} onChange={(e) => setLocation(e.target.value)} />
        </Field>
      )}
      <ul className="flex flex-col gap-2">
        {pending.map((d) => (
          <li key={d.dishId} className="flex flex-col gap-2 rounded-md border p-3">
            <Field orientation="horizontal">
              <Checkbox
                id={`sample-dish-${d.dishId}`}
                checked={entries[d.dishId]?.retained}
                onCheckedChange={(c) => set(d.dishId, { retained: c === true })}
              />
              <FieldLabel htmlFor={`sample-dish-${d.dishId}`}>{`Đã lưu mẫu ${d.dishName}`}</FieldLabel>
            </Field>
            {!entries[d.dishId]?.retained && (
              <Field data-invalid={(submitted && missingReason(d.dishId)) || undefined}>
                <FieldLabel htmlFor={`sample-reason-${d.dishId}`}>Lý do không lưu</FieldLabel>
                <Input
                  id={`sample-reason-${d.dishId}`}
                  className="h-11"
                  value={entries[d.dishId]?.reason}
                  onChange={(e) => set(d.dishId, { reason: e.target.value })}
                />
                {submitted && missingReason(d.dishId) && <FieldError>Nhập lý do không lưu mẫu</FieldError>}
              </Field>
            )}
          </li>
        ))}
      </ul>
      <ProblemAlert problem={failure} />
      <div className="flex justify-end">
        <Button
          className="h-11"
          disabled={busy}
          onClick={async () => {
            setSubmitted(true)
            if (pending.some((d) => missingReason(d.dishId))) return
            const body = {
              ...(location.trim() && { storageLocation: location.trim() }),
              dishes: pending.map((d) => ({
                dishId: d.dishId,
                retained: entries[d.dishId].retained,
                ...(!entries[d.dishId].retained && { notRetainedReason: entries[d.dishId].reason.trim() }),
              })),
            }
            if (await run(postDailyMealsByIdRetainedSamples, { path: { id: dailyMealId }, body }))
              onChanged('Đã ghi lưu mẫu')
          }}
        >
          Ghi lưu mẫu
        </Button>
      </div>
    </div>
  )
}

export function SamplesTab({ meals, kitchen, announce }) {
  const closed = meals.filter((m) => m.closed)
  const [dailyMealId, setDailyMealId] = useState(null)
  const selected = closed.some((m) => m.dailyMealId === dailyMealId) ? dailyMealId : (closed[0]?.dailyMealId ?? null)
  const view = useLoad(
    () =>
      selected
        ? getDailyMealsByIdRetainedSamples({ path: { id: selected } }).then((r) => r.data)
        : Promise.resolve(null),
    [selected],
  )
  const discard = useCommand()
  const [confirming, setConfirming] = useState(false)

  function changed(message) {
    announce(message)
    view.reload()
  }

  if (closed.length === 0)
    return <p className="type-body-sm text-muted-foreground">Chưa có bữa nào được chốt trong ngày.</p>
  const v = view.data
  return (
    <div className="flex flex-col gap-4">
      <Field className="max-w-sm">
        <FieldLabel htmlFor="sample-meal">Bữa</FieldLabel>
        <select
          id="sample-meal"
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
      <ProblemAlert problem={view.failure} />
      {v === null ? (
        !view.failure && <Skeleton className="h-24 w-full" />
      ) : (
        <Card className="gap-4 p-4">
          {v.sample ? (
            <div className="flex flex-col gap-3">
              <StatusBadge status={SAMPLE_STATUS[v.sample.status] ?? v.sample.status} />
              <div className="grid gap-3 sm:grid-cols-3">
                <Info label="Lưu lúc">{formatDateTime(v.sample.retainedAt)}</Info>
                <Info label="Hạn lưu">{v.sample.expiresAt ? formatDateTime(v.sample.expiresAt) : null}</Info>
                <Info label="Nơi lưu">{v.sample.storageLocation}</Info>
                {v.sample.discardedAt && <Info label="Hủy mẫu lúc">{formatDateTime(v.sample.discardedAt)}</Info>}
              </div>
            </div>
          ) : (
            <p className="type-body-sm text-muted-foreground">Bữa này chưa ghi lưu mẫu.</p>
          )}
          <ul className="flex flex-col gap-1 type-body-sm">
            {v.dishes.map((d) => (
              <li key={d.dishId}>
                {d.dishName}
                {d.recorded ? (d.retained ? ', đã lưu mẫu' : `, không lưu: ${d.notRetainedReason}`) : ', chưa ghi'}
                {d.addedLater ? ' (ghi thêm sau phát sinh)' : ''}
              </li>
            ))}
          </ul>
          <SampleForm
            key={`${selected}-${v.dishes.filter((d) => d.recorded).length}`}
            dailyMealId={selected}
            view={v}
            kitchen={kitchen}
            onChanged={changed}
          />
          <ProblemAlert problem={discard.failure} />
          {kitchen && v.sample?.status === 'RECORDED' && (
            <div className="flex flex-wrap items-center justify-end gap-2 border-t pt-4">
              {confirming ? (
                <>
                  <span className="type-body-sm">Thời điểm hủy mẫu chỉ ghi một lần.</span>
                  <Button variant="ghost" className="h-11" onClick={() => setConfirming(false)}>
                    Thôi
                  </Button>
                  <Button
                    variant="destructive"
                    className="h-11"
                    disabled={discard.busy}
                    onClick={async () => {
                      if (await discard.run(postRetainedSamplesByIdDiscard, { path: { id: v.sample.id } })) {
                        setConfirming(false)
                        changed('Đã ghi hủy mẫu')
                      }
                    }}
                  >
                    Ghi hủy mẫu
                  </Button>
                </>
              ) : (
                <Button variant="outline" className="h-11" onClick={() => setConfirming(true)}>
                  Ghi hủy mẫu…
                </Button>
              )}
            </div>
          )}
        </Card>
      )}
    </div>
  )
}
