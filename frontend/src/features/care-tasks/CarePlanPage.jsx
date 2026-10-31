import { useEffect, useState } from 'react'
import {
  getResidents,
  getResidentsByResidentIdCarePlans,
  postResidentsByResidentIdCarePlans,
  getCarePlansCompare,
} from '@/api'
import { Button } from '@/components/ui/button'
import { PageHeader } from '@/common/PageHeader'
import { DetailLayout } from '@/common/DetailLayout'
import { StatusBadge } from '@/common/StatusBadge'
import { ProblemAlert } from '@/common/ProblemAlert'
import { PermissionDeniedState } from '@/common/PageState'
import { usePermissions } from '@/common/permissions/permissions'
import { formatDate } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { CareField, CareCheck } from './CareField'
import { CarePlanDetail } from './CarePlanDetail'
import { PLAN_STATUS } from './carePlanLabels'

function ResidentChoice({ value, onChange }) {
  const [text, setText] = useState('')
  const [query, setQuery] = useState('')
  const [residents, setResidents] = useState([])
  const [cursor, setCursor] = useState(null)
  const [failure, setFailure] = useState(null)
  useEffect(() => {
    let cancelled = false
    getResidents({ query: { q: query || undefined } })
      .then(({ data }) => {
        if (!cancelled) {
          setResidents(data.items)
          setCursor(data.nextCursor)
          setFailure(null)
        }
      })
      .catch((error) => {
        if (!cancelled) setFailure(problemOf(error))
      })
    return () => {
      cancelled = true
    }
  }, [query])
  async function more() {
    try {
      const { data } = await getResidents({ query: { q: query || undefined, cursor } })
      setResidents((old) => [...old, ...data.items])
      setCursor(data.nextCursor)
    } catch (error) {
      setFailure(problemOf(error))
    }
  }
  const options = Object.fromEntries(residents.map((resident) => [resident.id, resident.fullName]))
  if (value && !options[value]) options[value] = 'Người cao tuổi đã chọn'
  return (
    <section className="flex flex-col gap-4 rounded-xl border bg-card p-5">
      <form
        className="flex flex-wrap items-end gap-3"
        onSubmit={(event) => {
          event.preventDefault()
          setQuery(text.trim())
        }}
      >
        <div className="min-w-0 flex-1">
          <CareField label="Tìm người cao tuổi" value={text} onChange={setText} />
        </div>
        <Button type="submit" variant="outline">
          Tìm hồ sơ
        </Button>
      </form>
      <ProblemAlert problem={failure} />
      <CareField label="Người cao tuổi" value={value || ''} onChange={(id) => onChange(Number(id))} options={options} />
      {cursor && (
        <Button variant="outline" onClick={more}>
          Xem thêm hồ sơ
        </Button>
      )}
    </section>
  )
}

function Comparison({ value }) {
  const labels = {
    taskTypeId: 'Loại công việc',
    content: 'Nội dung',
    frequencyType: 'Tần suất',
    intervalHours: 'Khoảng giờ',
    timesPerDay: 'Số lần trong ngày',
    startsAt: 'Giờ bắt đầu',
    times: 'Các mốc giờ',
    weekdays: 'Ngày trong tuần',
    windowBeforeMinutes: 'Khung trước',
    windowAfterMinutes: 'Khung sau',
    performerRole: 'Vai trò thực hiện',
    resultType: 'Kết quả',
    importance: 'Mức quan trọng',
    billable: 'Tính phí',
    waterTargetMl: 'Mục tiêu nước',
  }
  return (
    <section className="flex flex-col gap-4 rounded-xl border bg-card p-5" aria-label="Kết quả so sánh">
      <h2 className="type-title">Kết quả so sánh</h2>
      <h3 className="type-label">Mục thêm ({value.added.length})</h3>
      <ul>
        {value.added.map((item) => (
          <li key={item.id}>{item.content}</li>
        ))}
      </ul>
      <h3 className="type-label">Mục bớt ({value.removed.length})</h3>
      <ul>
        {value.removed.map((item) => (
          <li key={item.id}>{item.content}</li>
        ))}
      </ul>
      <h3 className="type-label">Mục đổi ({value.changed.length})</h3>
      {value.changed.map((item) => (
        <article key={item.after.id} className="rounded-lg border p-3">
          <p>Trước: {item.before.content}</p>
          <p>Sau: {item.after.content}</p>
          <p className="type-body-sm text-muted-foreground">
            Trường thay đổi: {item.changedFields.map((field) => labels[field] ?? field).join(', ')}
          </p>
        </article>
      ))}
    </section>
  )
}

export function CarePlanPage({ residentId: initialResidentId = 0 }) {
  const { can } = usePermissions()
  const [residentId, setResidentId] = useState(initialResidentId)
  const [plans, setPlans] = useState(null)
  const [failure, setFailure] = useState(null)
  const [selected, setSelected] = useState(null)
  const [reload, setReload] = useState(0)
  const [copyCurrent, setCopyCurrent] = useState(true)
  const [busy, setBusy] = useState(false)
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [comparison, setComparison] = useState(null)
  useEffect(() => {
    if (!residentId) return undefined
    let cancelled = false
    getResidentsByResidentIdCarePlans({ path: { residentId } })
      .then(({ data }) => {
        if (!cancelled) {
          setPlans(data)
          setFailure(null)
        }
      })
      .catch((error) => {
        if (!cancelled) setFailure(problemOf(error))
      })
    return () => {
      cancelled = true
    }
  }, [residentId, reload])
  async function create() {
    setBusy(true)
    setFailure(null)
    try {
      const { data } = await postResidentsByResidentIdCarePlans({
        path: { residentId },
        body: { copyCurrent },
        headers: { 'Idempotency-Key': crypto.randomUUID() },
      })
      setSelected(data.id)
      setReload((value) => value + 1)
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }
  async function compare(event) {
    event.preventDefault()
    setBusy(true)
    setFailure(null)
    try {
      const { data } = await getCarePlansCompare({ query: { from: Number(from), to: Number(to) } })
      setComparison(data)
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }
  function choose(id) {
    setResidentId(id)
    setPlans(null)
    setSelected(null)
    setFailure(null)
    setComparison(null)
    setFrom('')
    setTo('')
  }
  const options = Object.fromEntries((plans ?? []).map((plan) => [plan.id, `Phiên bản ${plan.revision}`]))
  const open = (plans ?? []).some((plan) => ['DRAFT', 'PENDING_APPROVAL', 'PENDING_EFFECT'].includes(plan.status))
  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Kế hoạch chăm sóc" subtitle="Lập, duyệt và tra cứu lịch sử phiên bản" />
      <DetailLayout
        open={selected !== null}
        detailKey={selected}
        onClose={() => setSelected(null)}
        label="Chi tiết kế hoạch chăm sóc"
        renderDetail={({ Title, Description }) => (
          <CarePlanDetail
            key={selected}
            planId={selected}
            Title={Title}
            Description={Description}
            onChanged={() => setReload((value) => value + 1)}
          />
        )}
      >
        <ResidentChoice value={residentId} onChange={choose} />
        {failure?.status === 403 ? (
          <PermissionDeniedState label="kế hoạch chăm sóc" />
        ) : (
          <>
            <ProblemAlert problem={failure} />
            {residentId ? (
              <section className="flex flex-col gap-4 rounded-xl border bg-card p-5" aria-busy={busy || plans === null}>
                <h2 className="type-title">Lịch sử phiên bản</h2>
                {plans === null ? (
                  <p role="status">Đang tải phiên bản…</p>
                ) : plans.length === 0 ? (
                  <p>Chưa có kế hoạch chăm sóc.</p>
                ) : (
                  <ul className="divide-y">
                    {plans.map((plan) => (
                      <li key={plan.id} className="flex flex-wrap items-center justify-between gap-3 py-4">
                        <div>
                          <Button variant="link" onClick={() => setSelected(plan.id)}>
                            Phiên bản {plan.revision}
                          </Button>
                          <StatusBadge status={PLAN_STATUS[plan.status]} />
                          {plan.effectiveFrom && (
                            <p className="type-body-sm text-muted-foreground">
                              Từ {formatDate(plan.effectiveFrom)}
                              {plan.effectiveTo ? ` đến ${formatDate(plan.effectiveTo)}` : ''}
                            </p>
                          )}
                        </div>
                        <Button
                          variant="outline"
                          aria-label={`Xem phiên bản ${plan.revision}`}
                          onClick={() => setSelected(plan.id)}
                        >
                          Xem
                        </Button>
                      </li>
                    ))}
                  </ul>
                )}
                {can('CARE_PLAN', 'CREATE') && (
                  <div className="flex flex-col items-start gap-3 border-t pt-4">
                    <CareCheck
                      label="Sao chép phiên bản hiệu lực"
                      checked={copyCurrent}
                      onChange={setCopyCurrent}
                      disabled={busy || open}
                    />
                    <Button disabled={busy || open || plans === null} onClick={create}>
                      Lập phiên bản nháp
                    </Button>
                    {open && (
                      <p className="type-body-sm text-muted-foreground">
                        Đã có phiên bản đang mở. Hoàn tất phiên bản đó trước khi lập bản mới.
                      </p>
                    )}
                  </div>
                )}
              </section>
            ) : (
              <p>Chọn người cao tuổi để xem kế hoạch.</p>
            )}
            {plans?.length >= 2 && (
              <form className="flex flex-col gap-4 rounded-xl border bg-card p-5" onSubmit={compare}>
                <h2 className="type-title">So sánh phiên bản</h2>
                <div className="grid gap-4 sm:grid-cols-2">
                  <CareField label="So sánh từ phiên bản" value={from} onChange={setFrom} options={options} required />
                  <CareField label="Đến phiên bản" value={to} onChange={setTo} options={options} required />
                </div>
                <Button type="submit" disabled={busy || !from || !to}>
                  So sánh phiên bản
                </Button>
              </form>
            )}
            {comparison && <Comparison value={comparison} />}
          </>
        )}
      </DetailLayout>
    </div>
  )
}
