import { LockKeyhole, Users } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { PageHeader } from '@/common/PageHeader'
import { ResidentAvatar } from '@/common/ResidentAvatar'
import { DEMO_INCIDENT } from '@/common/demoData'

// Bản mẫu chi tiết một sự cố, phần thông tin cho người thân (DESIGN.md 2.2, màn B; 14.5, Q-352, Q-354,
// Q-358; 007 FR-082). Thông báo cho người thân do hệ thống gửi (009), nhân viên không soạn; màn này
// chỉ cho thấy ai nhận phần nào và bản ghi gửi. Danh sách sự cố, lệnh xử lý thuộc 007, chưa có bản mẫu.

function Facts({ rows }) {
  return (
    <dl className="grid gap-x-4 gap-y-2 type-body-sm sm:grid-cols-[10rem_minmax(0,1fr)]">
      {rows.map(([label, value]) => (
        <div key={label} className="contents">
          <dt className="text-muted-foreground">{label}</dt>
          <dd>{value}</dd>
        </div>
      ))}
    </dl>
  )
}

function GeneralPart({ incident }) {
  const g = incident.general
  return (
    <Facts
      rows={[
        ['Loại sự cố', g.type],
        ['Mức', g.level],
        ['Thời điểm', <span className="tabular-nums">{g.at}</span>],
        ['Viện đã xử lý', g.handled],
      ]}
    />
  )
}

function Recipients({ names }) {
  return (
    <p className="flex items-start gap-2 rounded-lg bg-muted px-3 py-2 type-body-sm">
      <Users className="mt-0.5 size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
      <span>
        <span className="font-medium">Người thân nhận: </span>
        {names.length > 0 ? names.join(', ') : 'Chưa có người thân trong phạm vi'}
      </span>
    </p>
  )
}

// Vai trò nhân viên nào thấy phần sức khỏe theo quyền backend trả (Phụ lục 27, chú thích 10); bản mẫu hiện đủ.
function StaffView({ incident }) {
  const h = incident.health
  const healthRecipients = incident.deliveries.filter((d) => d.parts.includes('sức khỏe')).map((d) => d.relative)
  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle className="type-card-title font-semibold">Phần chung</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <GeneralPart incident={incident} />
          <Recipients names={incident.deliveries.map((d) => d.relative)} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 type-card-title font-semibold">
            <LockKeyhole className="size-4 text-muted-foreground" aria-hidden="true" />
            Phần sức khỏe
          </CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <Facts
            rows={[
              ['Mô tả', h.description],
              ['Xử lý ban đầu', h.firstAid],
              ['Diễn biến', h.progress],
              ['Tổn thương', h.injury],
            ]}
          />
          <Recipients names={healthRecipients} />
          <p className="type-caption text-muted-foreground">Chỉ người thân có trong bản đồng ý chia sẻ sức khỏe (BR-M01-08).</p>
        </CardContent>
      </Card>

      <Card className="py-0">
        <CardHeader className="pt-4">
          <CardTitle className="type-card-title font-semibold">Bản ghi gửi</CardTitle>
        </CardHeader>
        <Table>
          <TableHeader>
            <TableRow className="hover:bg-transparent">
              <TableHead className="pl-4">Giờ</TableHead>
              <TableHead>Người thân</TableHead>
              <TableHead>Phần đã gửi</TableHead>
              <TableHead className="pr-4">Căn cứ</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {incident.deliveries.map((d) => (
              <TableRow key={d.relative}>
                <TableCell className="pl-4 tabular-nums">{d.at}</TableCell>
                <TableCell>
                  {d.relative}, {d.relation.toLowerCase()}
                </TableCell>
                <TableCell>{d.parts}</TableCell>
                <TableCell className="pr-4 whitespace-normal text-muted-foreground">{d.basis}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Card>
    </>
  )
}

export function IncidentDetailPage() {
  const incident = DEMO_INCIDENT
  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6">
      <PageHeader
        title={`Sự cố ${incident.code}`}
        subtitle="Thông tin cho người thân, hệ thống gửi theo phạm vi đồng ý"
        aside={
          <div className="flex items-center gap-2.5">
            <ResidentAvatar name={incident.resident} code={incident.residentCode} initials={incident.initials} />
            <div className="flex flex-col">
              <span className="font-medium">{incident.resident}</span>
              <span className="type-caption text-muted-foreground tabular-nums">{incident.room}</span>
            </div>
          </div>
        }
      />
      <StaffView incident={incident} />
    </div>
  )
}
