import { useEffect, useState } from 'react'
import { getResidentsByResidentIdWellbeingProfile, getResultValues } from '@/api'
import { Button } from '@/components/ui/button'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { ProblemAlert } from '@/common/ProblemAlert'
import { SourceTag } from '@/common/SourceTag'
import { formatDate, formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { ActivityField } from './ActivityField'
import { addDays, todayInVietnam, ATTENDANCE_RESULT, PARTICIPATION_LEVEL, COMMUNICATION_LEVEL } from './sessionView'

function PreferenceList({ items, historical = false }) {
  if (!items.length) return <p className="type-body-sm text-muted-foreground">Chưa ghi nhận.</p>
  return (
    <ul className="flex flex-col gap-3">
      {items.map((item) => (
        <li key={item.id} className="flex flex-col gap-1 border-b pb-3">
          <SourceTag
            source={{ RESIDENT: 'resident', FAMILY_MEMBER: 'family', OBSERVING_STAFF: 'staff' }[item.source.type]}
            relative={`người thân mã ${item.source.familyMemberId}`}
          />
          <p className="type-body-sm">
            {item.level === 'LIKE' ? 'Thích' : 'Không thích'}, nhóm {item.interestGroupId}
          </p>
          {item.description && <p className="type-body">{item.description}</p>}
          <p className="type-body-sm text-muted-foreground">
            Ghi bởi tài khoản {item.recordedBy}, {formatDateTime(item.recordedAt)}
          </p>
          {historical && (
            <p className="type-body-sm text-muted-foreground">
              {item.status === 'REPLACED' ? 'Đã thay' : 'Đã bỏ'}, {formatDateTime(item.endedAt)}
              {item.endReason && `, ${item.endReason}`}
            </p>
          )}
        </li>
      ))}
    </ul>
  )
}

// FR-053, bố cục duyệt 08/11/2026: thông tin chỉ đọc, nguồn ghi nằm ở điểm danh và công việc chăm sóc.
export function WellbeingSheet({ residentId, residentName, onClose }) {
  const [form, setForm] = useState(() => ({ from: addDays(todayInVietnam(), -29), to: todayInVietnam() }))
  const [query, setQuery] = useState(form)
  const [result, setResult] = useState(null)
  const [resultValues, setResultValues] = useState([])
  const [catalogFailure, setCatalogFailure] = useState(null)
  useEffect(() => {
    let active = true
    getResultValues()
      .then(({ data }) => active && setResultValues(data))
      .catch((error) => active && setCatalogFailure(problemOf(error)))
    return () => {
      active = false
    }
  }, [])
  function observationText(record) {
    const values = record.values
    const behavior = values.jsonValue
    if (record.resultType === 'BEHAVIOR' && behavior) {
      if (!behavior.occurred) return 'Không có hành vi bất thường'
      const name = resultValues.find((v) => v.type === 'BEHAVIOR' && v.code === behavior.behaviorCode)?.name
      return [name ?? 'Chưa đọc được tên loại hành vi', behavior.description].filter(Boolean).join(', ')
    }
    return (
      resultValues.find((v) => v.type === record.resultType && v.code === values.codeValue)?.name ??
      'Chưa đọc được tên tâm trạng'
    )
  }
  const requestKey = `${residentId}:${query.from}:${query.to}`
  const current = result?.key === requestKey ? result : null
  useEffect(() => {
    let active = true
    getResidentsByResidentIdWellbeingProfile({ path: { residentId }, query })
      .then(({ data }) => active && setResult({ key: requestKey, data }))
      .catch((error) => active && setResult({ key: requestKey, failure: problemOf(error) }))
    return () => {
      active = false
    }
  }, [residentId, query, requestKey])
  const profile = current?.data
  return (
    <Sheet
      open
      onOpenChange={(open) => {
        if (!open) onClose()
      }}
    >
      <SheetContent className="flex w-full flex-col gap-5 overflow-y-auto p-6 sm:max-w-3xl">
        <SheetTitle className="type-title pr-8">Hồ sơ tinh thần của {residentName}</SheetTitle>
        <SheetDescription className="type-body-sm">
          Dữ liệu được ghi từ điểm danh, công việc chăm sóc và sở thích.
        </SheetDescription>
        <form
          className="grid grid-cols-1 items-end gap-3 sm:grid-cols-3"
          onSubmit={(event) => {
            event.preventDefault()
            setQuery({ ...form })
            setResult(null)
          }}
        >
          <ActivityField
            label="Từ ngày"
            type="date"
            value={form.from}
            required
            onChange={(value) => setForm((old) => ({ ...old, from: value }))}
          />
          <ActivityField
            label="Đến ngày"
            type="date"
            value={form.to}
            required
            onChange={(value) => setForm((old) => ({ ...old, to: value }))}
          />
          <Button type="submit" variant="outline" className="h-11">
            Xem theo khoảng
          </Button>
        </form>
        {!current && <Skeleton className="h-32" />}
        {current?.failure && <ProblemAlert problem={current.failure} />}
        {profile && (
          <>
            <section className="flex flex-col gap-3" aria-label="Tham gia hoạt động nhóm">
              <h3 className="type-card-title">Tham gia hoạt động nhóm theo tuần</h3>
              <ul className="flex flex-col gap-2">
                {profile.weeklyParticipation.map((week) => (
                  <li key={week.weekStart} className="type-body-sm">
                    Tuần từ {formatDate(week.weekStart)}, {week.presentCount} buổi có mặt
                  </li>
                ))}
              </ul>
              {!profile.weeklyParticipation.length && (
                <p className="type-body-sm">Chưa có lượt tham gia nhóm trong khoảng.</p>
              )}
              <h3 className="type-card-title">Tham gia và giao tiếp theo buổi</h3>
              <ul className="flex flex-col gap-3">
                {profile.sessions.map((session) => (
                  <li key={session.sessionId} className="flex flex-col gap-1 border-b pb-3">
                    <p className="type-label">
                      Buổi {session.sessionId}, {formatDateTime(session.startsAt)}
                    </p>
                    <p className="type-body-sm">{ATTENDANCE_RESULT[session.result] ?? 'Không ghi nhận'}</p>
                    {session.participationLevel && (
                      <p className="type-body-sm">
                        {PARTICIPATION_LEVEL[session.participationLevel] ?? session.participationLevel}
                      </p>
                    )}
                    {session.communicationLevel && (
                      <p className="type-body-sm">
                        {COMMUNICATION_LEVEL[session.communicationLevel] ?? session.communicationLevel}
                      </p>
                    )}
                  </li>
                ))}
              </ul>
            </section>
            <section className="flex flex-col gap-3" aria-label="Tâm trạng và hành vi">
              <h3 className="type-card-title">Tâm trạng và hành vi theo ngày</h3>
              <ProblemAlert problem={catalogFailure} />
              {!profile.dailyObservations.length && <p className="type-body-sm">Chưa ghi nhận trong khoảng.</p>}
              {profile.dailyObservations.map((day) => (
                <div key={day.date} className="flex flex-col gap-2">
                  <h4 className="type-label">{formatDate(day.date)}</h4>
                  {day.records.map((record) => (
                    <p key={record.id} className="type-body-sm">
                      {record.resultType === 'MOOD' ? 'Tâm trạng' : 'Hành vi'}, {observationText(record)}
                      {record.note && `, ${record.note}`}
                    </p>
                  ))}
                </div>
              ))}
            </section>
            <section className="flex flex-col gap-3" aria-label="Sở thích">
              <h3 className="type-card-title">Sở thích hiện hành</h3>
              <PreferenceList items={profile.preferences.current} />
              <details>
                <summary className="type-label cursor-pointer py-3">
                  Lịch sử sở thích ({profile.preferences.history.length})
                </summary>
                <PreferenceList items={profile.preferences.history} historical />
              </details>
            </section>
            <section className="flex flex-col gap-2" aria-label="Cảnh báo">
              <h3 className="type-card-title">Cảnh báo cô lập và tâm trạng</h3>
              {profile.alerts === null ? (
                <p className="type-body-sm text-muted-foreground">Chưa có nguồn lịch sử cảnh báo.</p>
              ) : (
                <p className="type-body-sm">Chưa có cảnh báo trong khoảng.</p>
              )}
            </section>
          </>
        )}
      </SheetContent>
    </Sheet>
  )
}
