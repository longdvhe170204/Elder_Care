import { useState } from 'react'
import { postActivitiesByIdByCommand, postActivitySessions } from '@/api'
import { Button } from '@/components/ui/button'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { ActivityField } from './ActivityField'
import { toOffsetIso } from './sessionView'

// 014 T009: hai form lệnh tạo buổi (FR-006, FR-011) mở bằng Sheet; giá trị đã gõ giữ khi backend báo lỗi (9.2 điểm 4).
export function CommandSheet({ open, onOpenChange, title, description, submitLabel, ready, onSubmit, children }) {
  const [problem, setProblem] = useState(null)
  const [busy, setBusy] = useState(false)
  const [key, setKey] = useState(() => crypto.randomUUID())
  return (
    <Sheet
      open={open}
      onOpenChange={(next) => {
        if (!next) setProblem(null)
        onOpenChange(next)
      }}
    >
      <SheetContent className="overflow-y-auto p-6 sm:max-w-lg [&_[data-slot=sheet-title]]:pr-8">
        <SheetTitle className="type-title">{title}</SheetTitle>
        <SheetDescription className="type-body-sm">{description}</SheetDescription>
        <form
          className="mt-4 flex flex-col gap-3"
          onSubmit={async (e) => {
            e.preventDefault()
            setBusy(true)
            setProblem(null)
            try {
              await onSubmit({ 'Idempotency-Key': key })
              setKey(crypto.randomUUID())
            } catch (error) {
              setProblem(problemOf(error))
            } finally {
              setBusy(false)
            }
          }}
        >
          {children}
          <ProblemAlert problem={problem} />
          <Button type="submit" size="lg" disabled={busy || !ready}>
            {submitLabel}
          </Button>
        </form>
      </SheetContent>
    </Sheet>
  )
}

export function CreateFromTemplateSheet({ open, onOpenChange, activities, onDone }) {
  const [form, setForm] = useState({ activityId: '', fromDate: '', toDate: '' })
  const set = (field) => (value) => setForm((f) => ({ ...f, [field]: value }))
  return (
    <CommandSheet
      open={open}
      onOpenChange={onOpenChange}
      title="Tạo buổi từ mẫu"
      description="Chỉ tạo các buổi còn thiếu trong khoảng ngày theo mẫu lặp; người tham gia thường xuyên đạt điều kiện được đăng ký sẵn."
      submitLabel="Tạo các buổi còn thiếu"
      ready={form.activityId && form.fromDate && form.toDate}
      onSubmit={async (headers) => {
        await postActivitiesByIdByCommand({
          path: { id: Number(form.activityId), command: 'create-sessions-from-template' },
          body: { fromDate: form.fromDate, toDate: form.toDate },
          headers,
        })
        setForm({ activityId: '', fromDate: '', toDate: '' })
        onDone('Đã tạo buổi từ mẫu')
      }}
    >
      <ActivityField
        label="Hoạt động"
        options={activities}
        value={form.activityId}
        onChange={set('activityId')}
        required
      />
      <ActivityField label="Từ ngày" type="date" value={form.fromDate} onChange={set('fromDate')} required />
      <ActivityField label="Đến ngày" type="date" value={form.toDate} onChange={set('toDate')} required />
    </CommandSheet>
  )
}

export function CreateSingleSheet({ open, onOpenChange, activities, onDone }) {
  const empty = {
    activityId: '',
    startsAt: '',
    endsAt: '',
    isTrip: false,
    destination: '',
    plannedDepartureAt: '',
    plannedReturnAt: '',
    transport: '',
    facilityVehicle: false,
  }
  const [form, setForm] = useState(empty)
  const set = (field) => (value) => setForm((f) => ({ ...f, [field]: value }))
  return (
    <CommandSheet
      open={open}
      onOpenChange={onOpenChange}
      title="Tạo buổi lẻ"
      description="Buổi không theo mẫu lặp, cho hoạt động đang hiệu lực, ở thời điểm sau bây giờ."
      submitLabel="Tạo buổi lẻ"
      ready={
        form.activityId &&
        form.startsAt &&
        form.endsAt &&
        (!form.isTrip || (form.destination && form.plannedDepartureAt && form.plannedReturnAt && form.transport))
      }
      onSubmit={async (headers) => {
        await postActivitySessions({
          body: {
            activityId: Number(form.activityId),
            startsAt: toOffsetIso(form.startsAt),
            endsAt: toOffsetIso(form.endsAt),
            ...(form.isTrip
              ? {
                  trip: {
                    destination: form.destination,
                    plannedDepartureAt: toOffsetIso(form.plannedDepartureAt),
                    plannedReturnAt: toOffsetIso(form.plannedReturnAt),
                    transport: form.transport,
                    facilityVehicle: form.facilityVehicle,
                  },
                }
              : {}),
          },
          headers,
        })
        setForm(empty)
        onDone('Đã tạo buổi lẻ')
      }}
    >
      <ActivityField
        label="Hoạt động"
        options={activities}
        value={form.activityId}
        onChange={set('activityId')}
        required
      />
      <ActivityField label="Bắt đầu" type="datetime-local" value={form.startsAt} onChange={set('startsAt')} required />
      <ActivityField label="Kết thúc" type="datetime-local" value={form.endsAt} onChange={set('endsAt')} required />
      <label className="flex min-h-11 items-center gap-3 type-label">
        <input type="checkbox" checked={form.isTrip} onChange={(e) => set('isTrip')(e.target.checked)} />
        Chuyến đi ngoài viện
      </label>
      {form.isTrip && (
        <>
          <ActivityField label="Điểm đến" value={form.destination} onChange={set('destination')} required />
          <ActivityField
            label="Giờ rời dự kiến"
            type="datetime-local"
            value={form.plannedDepartureAt}
            onChange={set('plannedDepartureAt')}
            required
          />
          <ActivityField
            label="Giờ về dự kiến"
            type="datetime-local"
            value={form.plannedReturnAt}
            onChange={set('plannedReturnAt')}
            required
          />
          <ActivityField label="Phương tiện" value={form.transport} onChange={set('transport')} required />
          <label className="flex min-h-11 items-center gap-3 type-label">
            <input
              type="checkbox"
              checked={form.facilityVehicle}
              onChange={(e) => set('facilityVehicle')(e.target.checked)}
            />
            Xe của viện
          </label>
        </>
      )}
    </CommandSheet>
  )
}
