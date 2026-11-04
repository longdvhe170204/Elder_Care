import { Fragment, useEffect, useState } from 'react'
import { getResidents, getResidentsByResidentIdTimetable } from '@/api'
import { Skeleton } from '@/components/ui/skeleton'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { formatDate } from '@/common/dateTime'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { careTime } from './careChecklistView'
import { CareField } from './CareField'
import { timetableView } from './timetableView'

const todayIso = () => new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date())

// T058 (FR-048): Trưởng tầng, Điều dưỡng xem thời khóa biểu theo ngày; chỉ xem, thay đổi ở nguồn.
export function TimetablePage({ initialDate }) {
  const { status, can } = usePermissions()
  const permitted = can('CARE_PLAN', 'VIEW')
  const [residents, setResidents] = useState(null)
  const [residentId, setResidentId] = useState('')
  const [date, setDate] = useState(initialDate ?? todayIso())
  const [result, setResult] = useState(null)
  const [failure, setFailure] = useState(null)
  const key = `${residentId}:${date}`

  useEffect(() => {
    let active = true
    if (status !== 'ready' || !permitted) return
    getResidents({ query: { status: ['IN_RESIDENCE'] } })
      .then(({ data }) => active && setResidents(data.items))
      .catch((error) => {
        if (!active) return
        setResidents([])
        setFailure(problemOf(error))
      })
    return () => {
      active = false
    }
  }, [status, permitted])

  useEffect(() => {
    let active = true
    if (!residentId || !date) return
    getResidentsByResidentIdTimetable({ path: { residentId }, query: { date } })
      .then(({ data }) => active && setResult({ key, items: data, failure: null }))
      .catch((error) => active && setResult({ key, items: [], failure: problemOf(error) }))
    return () => {
      active = false
    }
  }, [residentId, date, key])

  if (status === 'loading') return <Skeleton className="h-24" />
  const current = result?.key === key ? result : null
  if (!permitted || current?.failure?.status === 403) return <PermissionDeniedState label="Thời khóa biểu" />
  const person = (residents ?? []).find((r) => String(r.id) === residentId)
  const today = date === todayIso()
  const view = timetableView(current?.items ?? [], today ? new Date() : null)
  const options = Object.fromEntries((residents ?? []).map((r) => [r.id, `${r.personalInfo.fullName} · ${r.profileCode}`]))

  return (
    <div className="flex flex-col gap-5">
      <PageHeader
        title={person ? `Thời khóa biểu của ${person.personalInfo.fullName}, ${formatDate(date)}` : 'Thời khóa biểu'}
        subtitle="Chỉ để xem. Thay đổi thực hiện ở nguồn của từng mục."
        aside={
          <div className="flex flex-wrap gap-4">
            <CareField label="Người cao tuổi" value={residentId} onChange={setResidentId} options={options} />
            <CareField label="Ngày" type="date" value={date} onChange={setDate} />
          </div>
        }
      />
      <ProblemAlert problem={failure ?? current?.failure} />
      {!residentId ? (
        <p className="type-body text-muted-foreground">Chọn người cao tuổi để xem thời khóa biểu.</p>
      ) : !current ? (
        <Skeleton className="h-24" />
      ) : view.rows.length === 0 ? (
        <p role="status" className="type-body text-muted-foreground">
          Không có mục nào trong ngày
        </p>
      ) : (
        <ol aria-label="Các mục trong ngày" className="flex flex-col rounded-xl border bg-card">
          {view.rows.map((row, index) => (
            <Fragment key={row.key}>
              {index === view.nowIndex && <NowLine />}
              <li className="grid grid-cols-[4rem_9rem_1fr_auto] items-baseline gap-3 border-b px-4 py-3 last:border-b-0 max-md:grid-cols-[4rem_1fr]">
                <span className="type-body font-medium tabular-nums">{row.time}</span>
                <span className="type-body-sm text-muted-foreground">{row.source}</span>
                <span className="type-body max-md:col-start-2">{row.description}</span>
                <span className="type-body-sm text-muted-foreground max-md:col-start-2">{row.status}</span>
              </li>
            </Fragment>
          ))}
          {view.nowIndex === view.rows.length && <NowLine />}
        </ol>
      )}
    </div>
  )
}

function NowLine() {
  return (
    <li aria-label="Bây giờ" className="flex items-center gap-3 px-4 py-1">
      <span className="type-caption font-medium text-primary">Bây giờ {careTime(new Date().toISOString())}</span>
      <span aria-hidden className="h-px flex-1 bg-primary" />
    </li>
  )
}
