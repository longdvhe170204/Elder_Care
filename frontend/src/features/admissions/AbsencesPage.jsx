import { useCallback, useEffect, useState } from 'react'
import { getAbsences } from '@/api'
import { Card } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { DetailLayout } from '@/common/DetailLayout'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { ABSENCE_STATUS, absenceLabel } from './admissionLabels'
import { AbsenceDetail } from './AbsenceParts'
import { DayCareTab } from './DayCareTab'
import { ResidentSelect } from './FormParts'

// Tạm vắng, bán trú (004 T056; US6, FR-048 → FR-061): lượt vắng của một người cao tuổi kèm gia hạn, giải phóng giường,
// đính chính; báo vắng bán trú và ngày khu bán trú nghỉ. Cho tạm vắng và Ghi nhận trở về nằm ở tab Trạng thái của hồ sơ (001).

export function AbsencesPage({ role }) {
  const { can } = usePermissions()
  const [tab, setTab] = useState('absences')
  const [residentId, setResidentId] = useState('')
  const [absences, setAbsences] = useState(null)
  const [failure, setFailure] = useState(null)
  const [opened, setOpened] = useState(null)
  const [announcement, setAnnouncement] = useState('')

  const load = useCallback(() => {
    if (!residentId) return Promise.resolve()
    return getAbsences({ query: { residentId: Number(residentId) } })
      .then(({ data }) => {
        setAbsences(data)
        setFailure(null)
      })
      .catch((e) => {
        setAbsences([])
        setFailure(problemOf(e))
      })
  }, [residentId])

  useEffect(() => {
    load()
  }, [load])

  if (failure?.status === 403) return <PermissionDeniedState label="Tạm vắng, trở về" />

  const current = (absences ?? []).find((a) => a.id === opened)

  function changed(message) {
    setAnnouncement(message)
    load()
  }

  return (
    <DetailLayout
      open={tab === 'absences' && !!current}
      detailKey={opened}
      onClose={() => setOpened(null)}
      label="Chi tiết lượt vắng"
      renderDetail={(titles) => (
        <AbsenceDetail
          key={opened}
          absence={current}
          role={role}
          canUpdate={can('ABSENCE_RETURN', 'UPDATE')}
          onChanged={changed}
          {...titles}
        />
      )}
    >
      <PageHeader
        title="Tạm vắng, bán trú"
        subtitle="Lượt vắng của người cao tuổi nội trú, báo vắng và ngày nghỉ của khu bán trú."
      />
      <ToggleGroup
        variant="filter"
        size="lg"
        value={[tab]}
        onValueChange={(n) => n[0] && setTab(n[0])}
        aria-label="Chọn nhóm"
      >
        <ToggleGroupItem value="absences" className="h-11 px-4">
          Lượt vắng
        </ToggleGroupItem>
        <ToggleGroupItem value="day-care" className="h-11 px-4">
          Bán trú
        </ToggleGroupItem>
      </ToggleGroup>
      <p role="status" className="empty:hidden type-body-sm">
        {announcement}
      </p>
      {tab === 'day-care' ? (
        <DayCareTab canWrite={can('ABSENCE_RETURN', 'CREATE')} />
      ) : (
        <>
          <Card className="gap-3 p-4">
            <ResidentSelect
              id="absence-resident"
              value={residentId}
              onChange={(value) => {
                setResidentId(value)
                setAbsences(null)
                setOpened(null)
                setFailure(null)
              }}
              onFailure={(e) => setFailure(problemOf(e))}
            />
          </Card>
          <ProblemAlert problem={failure} />
          {!residentId ? (
            <p className="type-body-sm text-muted-foreground">Chọn người cao tuổi để xem lượt vắng.</p>
          ) : absences === null ? (
            <Skeleton className="h-24 w-full" />
          ) : absences.length === 0 ? (
            !failure && <p className="type-body-sm text-muted-foreground">Người cao tuổi này chưa có lượt vắng.</p>
          ) : (
            <Card className="gap-0 py-0">
              <ul aria-label="Lượt vắng">
                {absences.map((a) => (
                  <li
                    key={a.id}
                    className={`flex flex-wrap items-center justify-between gap-2 border-b p-4 last:border-b-0 ${a.id === opened ? 'bg-accent/40' : ''}`}
                  >
                    <button
                      type="button"
                      className="text-left underline-offset-4 hover:underline"
                      aria-pressed={a.id === opened}
                      onClick={() => setOpened(a.id)}
                    >
                      {absenceLabel(a)}
                    </button>
                    <span className="flex items-center gap-3 type-caption text-muted-foreground tabular-nums">
                      {`Rời ${formatDateTime(a.departedAt)}`}
                      <StatusBadge status={ABSENCE_STATUS[a.status] ?? a.status} />
                    </span>
                  </li>
                ))}
              </ul>
            </Card>
          )}
        </>
      )}
    </DetailLayout>
  )
}
