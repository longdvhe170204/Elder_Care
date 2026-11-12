import { useEffect, useState } from 'react'
import {
  getFamilyPortalResidentsByResidentIdContract,
  getFamilyPortalResidentsByResidentIdGeneral,
  getFamilyPortalResidentsByResidentIdHealthBySection,
} from '@/api'
import { Skeleton } from '@/components/ui/skeleton'
import { PageHeader } from '@/common/PageHeader'
import { ProblemAlert } from '@/common/ProblemAlert'
import { ResidentAvatar } from '@/common/ResidentAvatar'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDate, formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { HealthLock, InfoList, PortalCard } from '../PortalParts'
import { LiveActivities } from './LiveActivities'

// Màn cổng dùng dữ liệu thật (012 T028; US5, FR-046 → FR-048): Hôm nay (thông tin chung), Sức khỏe (từng phần qua 012,
// mỗi lần mở được ghi nhật ký), Hợp đồng (chỉ người đại diện). Chỉ trường của api-012; mục nguồn chưa có ghi rõ
// "chưa có dữ liệu" (DESIGN.md 2, không giả lập).

const STATUS_LABELS = {
  ADMISSION_IN_PROGRESS: 'Đang tiếp nhận',
  IN_RESIDENCE: 'Đang lưu trú',
  TEMPORARILY_ABSENT: 'Tạm vắng',
  OFF_SITE_ACTIVITY: 'Hoạt động bên ngoài',
  HOSPITALIZED: 'Điều trị tại bệnh viện',
  RESIDENCY_ENDED: 'Kết thúc lưu trú',
  DECEASED: 'Qua đời',
  ADMISSION_CANCELLED: 'Hủy tiếp nhận',
}

const VISIT_LABELS = {
  APPROVED: 'Đã duyệt',
  CHECKED_IN: 'Đã vào',
  CHECKED_OUT: 'Đã ra',
  NO_SHOW: 'Không đến',
  CANCELLED: 'Hủy',
}

const CONTRACT_LABELS = { DRAFT: 'Bản nháp', ACTIVE: 'Hiệu lực', ENDED: 'Đã kết thúc' }

const SECTIONS = [
  ['allergies-conditions', 'Dị ứng, bệnh nền'],
  ['vital-signs', 'Chỉ số'],
  ['weight', 'Cân nặng'],
  ['medications', 'Thuốc đang dùng'],
  ['incidents', 'Sự cố, phần sức khỏe'],
  ['care-results', 'Kết quả chăm sóc'],
  ['eating', 'Ăn uống'],
  ['diet', 'Chế độ ăn riêng'],
  ['medical-appointments', 'Lịch khám bên ngoài'],
]

const ITEM_TYPE = { ALLERGY: 'Dị ứng', CHRONIC_CONDITION: 'Bệnh nền' }

function useLoad(load, deps) {
  const [state, setState] = useState({ data: null, failure: null })
  useEffect(() => {
    let cancelled = false
    load()
      .then(({ data }) => !cancelled && setState({ data, failure: null }))
      .catch((error) => !cancelled && setState({ data: null, failure: problemOf(error) }))
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- tải lại khi đổi người cao tuổi
  }, deps)
  return state
}

export function LiveTodayPage({ viewer }) {
  const { data: g, failure } = useLoad(
    () => getFamilyPortalResidentsByResidentIdGeneral({ path: { residentId: viewer.residentId } }),
    [viewer.residentId],
  )
  if (failure) return <ProblemAlert problem={failure} />
  if (!g) return <Skeleton className="h-40 w-full" />
  return (
    <>
      <PortalCard>
        <div className="flex items-center gap-3">
          <ResidentAvatar name={g.fullName} code={String(g.residentId)} className="size-14 type-title" />
          <div className="flex min-w-0 flex-1 flex-col gap-0.5">
            <span className="type-card-title">{g.fullName}</span>
            {g.location && <span className="text-muted-foreground">{g.location}</span>}
          </div>
          <StatusBadge status={STATUS_LABELS[g.status] ?? g.status} />
        </div>
      </PortalCard>
      <PortalCard title="Lượt thăm của bạn">
        {g.visits.length === 0 ? (
          <p className="text-muted-foreground">Chưa có lượt thăm sắp tới.</p>
        ) : (
          <InfoList
            rows={g.visits.map((v) => [
              <span className="tabular-nums">{`${formatDate(v.date)}, ${v.startTime}–${v.endTime}`}</span>,
              <StatusBadge status={VISIT_LABELS[v.status] ?? v.status} />,
            ])}
          />
        )}
      </PortalCard>
      {g.dayCareSchedule.length > 0 && (
        <PortalCard title="Lịch đến, về bán trú 7 ngày tới">
          <InfoList
            rows={g.dayCareSchedule.map((s) => [
              <span className="tabular-nums">{formatDate(s.date)}</span>,
              <span className="tabular-nums">{`${s.arrivalTime}–${s.departureTime}`}</span>,
            ])}
          />
        </PortalCard>
      )}
      {g.absences.length > 0 && (
        <PortalCard title="Lượt vắng 30 ngày qua">
          <InfoList
            rows={g.absences.map((a) => [
              <span className="tabular-nums">{formatDateTime(a.from)}</span>,
              a.to ? <span className="tabular-nums">{formatDateTime(a.to)}</span> : 'Đang vắng',
            ])}
          />
        </PortalCard>
      )}
      <PortalCard title="Sự cố, phần chung">
        {g.unavailable.includes('incidents') ? (
          <p className="text-muted-foreground">Chưa có dữ liệu, tính năng sự cố chưa hoạt động.</p>
        ) : g.incidents.length === 0 ? (
          <p className="text-muted-foreground">Không có sự cố trong 30 ngày qua.</p>
        ) : (
          <InfoList
            rows={g.incidents.map((i) => [
              `${i.type}, mức ${i.level}`,
              <span className="tabular-nums">{formatDateTime(i.occurredAt)}</span>,
            ])}
          />
        )}
        {!viewer.perms.health && <HealthLock />}
      </PortalCard>
      <LiveActivities residentId={viewer.residentId} representative={!!viewer.representative} />
    </>
  )
}

function HealthSection({ residentId, section, title }) {
  const { data, failure } = useLoad(
    () => getFamilyPortalResidentsByResidentIdHealthBySection({ path: { residentId, section } }),
    [residentId, section],
  )
  if (failure?.status === 403) return null
  return (
    <PortalCard title={title}>
      {failure ? (
        <ProblemAlert problem={failure} />
      ) : !data ? (
        <Skeleton className="h-8 w-full" />
      ) : !data.available ? (
        <p className="text-muted-foreground">{data.note}</p>
      ) : data.items.length === 0 ? (
        <p className="text-muted-foreground">Chưa ghi nhận.</p>
      ) : section === 'allergies-conditions' ? (
        <InfoList rows={data.items.map((i) => [ITEM_TYPE[i.type] ?? i.type, i.text])} />
      ) : section === 'medications' ? (
        <InfoList rows={data.items.map((m) => [m.medicationName, m.dosageInstruction])} />
      ) : section === 'eating' ? (
        // Chủ dự án chốt 2026-11-12: trung bình theo ngày có ghi nước, kèm số ngày có dữ liệu
        <InfoList
          rows={data.items.map((e) => [
            'Lượng nước trung bình',
            <span className="tabular-nums">{`${Number(e.averageWaterMl).toLocaleString('vi-VN')} ml/ngày, tính trên ${e.waterDays} ngày có ghi`}</span>,
          ])}
        />
      ) : (
        <p>{data.items.length} mục</p>
      )}
    </PortalCard>
  )
}

export function LiveHealthPage({ viewer }) {
  const [denied, setDenied] = useState(false)
  useEffect(() => {
    let cancelled = false
    // Kiểm một phần trước: bị từ chối thì chỉ hiện dòng khóa, không lộ tiêu đề từng phần (DESIGN.md 2.2)
    getFamilyPortalResidentsByResidentIdHealthBySection({
      path: { residentId: viewer.residentId, section: 'allergies-conditions' },
    }).catch((error) => !cancelled && problemOf(error).status === 403 && setDenied(true))
    return () => {
      cancelled = true
    }
  }, [viewer.residentId])
  return (
    <>
      <PageHeader title="Sức khỏe" subtitle="Giá trị hiện hành. Mỗi lần xem mục này được viện ghi lại." />
      {denied || !viewer.perms.health ? (
        <HealthLock />
      ) : (
        SECTIONS.map(([section, title]) => (
          <HealthSection key={section} residentId={viewer.residentId} section={section} title={title} />
        ))
      )}
    </>
  )
}

// P9 Đồ gửi với phiên thật (012 FR-046b; 013 FR-030): phần đồ gửi của 013 theo người cao tuổi đang xem; backend kiểm
// người đại diện, người được phép đón đang hiệu lực
export function LiveDepositsPage({ viewer, belongings: Belongings }) {
  return (
    <>
      <PageHeader title="Đồ gửi" subtitle="Đồ dùng gia đình gửi viện giữ hộ và lần bàn giao gần nhất." />
      <PortalCard>
        {Belongings ? (
          <Belongings residentId={viewer.residentId} />
        ) : (
          <p className="text-muted-foreground">Chưa có dữ liệu đồ gửi.</p>
        )}
      </PortalCard>
    </>
  )
}

export function LiveContractPage({ viewer }) {
  const { data, failure } = useLoad(
    () => getFamilyPortalResidentsByResidentIdContract({ path: { residentId: viewer.residentId } }),
    [viewer.residentId],
  )
  return (
    <>
      <PageHeader title="Hợp đồng, dịch vụ" subtitle="Hợp đồng chỉ xem. Thay đổi đi qua yêu cầu và thành phụ lục." />
      <ProblemAlert problem={failure} />
      {!data && !failure && <Skeleton className="h-24 w-full" />}
      {data?.contracts.length === 0 && (
        <PortalCard>
          <p className="text-muted-foreground">Chưa có hợp đồng.</p>
        </PortalCard>
      )}
      {data?.contracts.map((c) => (
        <PortalCard
          key={c.id}
          title={`Hợp đồng ${c.contractNumber}`}
          action={<StatusBadge status={CONTRACT_LABELS[c.status] ?? c.status} />}
        >
          <InfoList
            rows={[
              [
                'Thời hạn',
                <span className="tabular-nums">{`${formatDate(c.startDate)} → ${formatDate(c.currentEndDate ?? c.endDate)}`}</span>,
              ],
            ]}
          />
        </PortalCard>
      ))}
    </>
  )
}
