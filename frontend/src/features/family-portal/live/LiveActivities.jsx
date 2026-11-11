import { useCallback, useEffect, useState } from 'react'
import {
  getFamilyPortalResidentsByResidentIdActivities,
  postFamilyPortalLateReturnConsents,
  postFamilyPortalLateReturnConsentsByIdWithdraw,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDate, formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { InfoList, PortalCard } from '../PortalParts'

// Hoạt động trên cổng người thân (014 FR-068, loại thông tin chung; api-014 FamilyActivities, chủ dự án duyệt
// 2026-11-10): buổi đã đăng ký, chuyến đi, số buổi đã tham gia. Người đại diện ghi, rút đồng ý về muộn cho buổi đủ điều
// kiện (FR-016a). Không có mức tham gia, giao tiếp, cảnh báo hay kết quả kiểm tra.

const TRIP_STATUS = { SCHEDULED: 'Đã lên lịch', IN_PROGRESS: 'Đang đi', RETURNED: 'Đã về', CANCELLED: 'Đã hủy' }
const REGISTRATION_STATUS = { REGISTERED: 'Đã đăng ký', ATTENDED: 'Đã tham gia' }

export function LiveActivities({ residentId, representative }) {
  const [data, setData] = useState(null)
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')

  const load = useCallback(
    () =>
      getFamilyPortalResidentsByResidentIdActivities({ path: { residentId } })
        .then(({ data }) => setData(data))
        .catch((e) => setFailure(problemOf(e))),
    [residentId],
  )
  useEffect(() => {
    load()
  }, [load])

  async function run(call, done) {
    setBusy(true)
    setFailure(null)
    try {
      await call({ 'Idempotency-Key': crypto.randomUUID() })
      setMessage(done)
      await load()
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  if (failure?.status === 403) return null
  return (
    <section aria-label="Hoạt động">
      <PortalCard title="Hoạt động">
        <ProblemAlert problem={failure} />
        <p role="status" className="empty:hidden">
          {message}
        </p>
        {!data ? (
          <Skeleton className="h-16 w-full" />
        ) : (
          <>
            {data.registeredSessions.length === 0 ? (
              <p className="text-muted-foreground">Chưa đăng ký buổi nào trong khoảng này.</p>
            ) : (
              <ul className="flex flex-col divide-y">
                {data.registeredSessions.map((s) => (
                  <li key={s.sessionId} className="flex flex-wrap items-center justify-between gap-2 py-2">
                    <span className="flex flex-col">
                      <span className="type-label">{s.activityName}</span>
                      <span className="tabular-nums text-muted-foreground">{formatDateTime(s.startsAt)}</span>
                      {s.activeLateReturnConsentId && <span>Đã đồng ý về muộn</span>}
                    </span>
                    <span className="flex flex-wrap items-center gap-2">
                      <StatusBadge status={REGISTRATION_STATUS[s.registrationStatus] ?? s.registrationStatus} />
                      {representative && s.lateReturnConsentEligible && !s.activeLateReturnConsentId && (
                        <Button
                          variant="outline"
                          className="h-11"
                          disabled={busy}
                          aria-label={`Đồng ý về muộn cho buổi ${s.activityName}`}
                          onClick={() =>
                            run(
                              (headers) =>
                                postFamilyPortalLateReturnConsents({
                                  headers,
                                  body: { residentId, sessionId: s.sessionId },
                                }),
                              `Đã đồng ý về muộn cho buổi ${s.activityName}`,
                            )
                          }
                        >
                          Đồng ý về muộn
                        </Button>
                      )}
                      {representative && s.activeLateReturnConsentId && (
                        <Button
                          variant="ghost"
                          className="h-11"
                          disabled={busy}
                          aria-label={`Rút đồng ý về muộn cho buổi ${s.activityName}`}
                          onClick={() =>
                            run(
                              (headers) =>
                                postFamilyPortalLateReturnConsentsByIdWithdraw({
                                  path: { id: s.activeLateReturnConsentId },
                                  headers,
                                }),
                              `Đã rút đồng ý về muộn cho buổi ${s.activityName}`,
                            )
                          }
                        >
                          Rút đồng ý
                        </Button>
                      )}
                    </span>
                  </li>
                ))}
              </ul>
            )}
            {data.trips.length > 0 && (
              <InfoList
                rows={data.trips.map((t) => [
                  <span className="flex flex-col">
                    <span className="type-label">{t.destination}</span>
                    <span className="tabular-nums text-muted-foreground">
                      {`${formatDateTime(t.departureAt)} → ${formatDateTime(t.expectedReturnAt)}`}
                    </span>
                  </span>,
                  <StatusBadge status={TRIP_STATUS[t.status] ?? t.status} />,
                ])}
              />
            )}
            <p className="text-muted-foreground">
              {`Đã tham gia ${data.attendedSessionCount} buổi từ ${formatDate(data.from)} đến ${formatDate(data.to)}`}
            </p>
          </>
        )}
      </PortalCard>
    </section>
  )
}
