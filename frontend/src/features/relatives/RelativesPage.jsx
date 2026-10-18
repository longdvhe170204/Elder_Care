import { useState } from 'react'
import { TriangleAlert } from 'lucide-react'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Textarea } from '@/components/ui/textarea'
import { PageHeader } from '@/common/PageHeader'
import { SourceTag } from '@/common/SourceTag'
import { StatusBadge } from '@/common/StatusBadge'
import { DEMO_RELATIVE_REQUEST, DEMO_RELATIVES, DEMO_RESIDENTS, DEMO_VISIT_WISHES } from '@/common/demoData'

// Bản mẫu người thân và mong muốn về khách thăm, người đón của một người cao tuổi (DESIGN.md 2.2, màn C;
// 14.1, Q-351, Q-364; feature 012). Hệ thống không tự đổi quyền theo mong muốn (Q-307): chỉ hiện mong muốn
// cạnh người thân và đưa yêu cầu trái mong muốn về Quản lý viện duyệt. Quyền thật do backend trả.

// Phụ lục 27, dòng "Mong muốn về khách thăm, người đón": Trưởng tầng, Điều dưỡng, Hành chính ghi; Quản lý viện xem
const WISH_RECORDERS = ['FLOOR_MANAGER', 'NURSING_STAFF', 'OFFICE_STAFF']
const APPROVER = 'NURSING_HOME_MANAGER'

function WishLine({ wish }) {
  return (
    <li className="flex flex-col gap-1 border-t py-2 first:border-t-0 first:pt-0">
      <span className="flex flex-wrap items-center gap-x-2 gap-y-1">
        <span className="font-medium">{wish.text}</span>
        <SourceTag source={wish.source} relative={wish.relative} />
      </span>
      <span className="type-caption text-muted-foreground tabular-nums">
        Ghi bởi {wish.recordedBy}, {wish.recordedAt}
      </span>
    </li>
  )
}

function WishesCard({ canRecord }) {
  const [askedNew, setAskedNew] = useState(false)
  return (
    <Card>
      <CardHeader className="flex flex-row flex-wrap items-center justify-between gap-2">
        <CardTitle className="type-card-title font-semibold">Mong muốn của người cao tuổi</CardTitle>
        {canRecord && (
          <Button variant="outline" onClick={() => setAskedNew(true)}>
            Ghi mong muốn mới
          </Button>
        )}
      </CardHeader>
      <CardContent className="flex flex-col gap-2 type-body-sm">
        <ul className="flex flex-col">
          {DEMO_VISIT_WISHES.map((w) => (
            <WishLine key={w.text} wish={w} />
          ))}
        </ul>
        <p role="status" className="type-caption text-muted-foreground">
          {askedNew && 'Form ghi mong muốn: chưa có bản mẫu.'}
        </p>
      </CardContent>
    </Card>
  )
}

function RelativesTable() {
  return (
    <Card className="py-0">
      <CardHeader className="pt-4">
        <CardTitle className="type-card-title font-semibold">Người thân</CardTitle>
      </CardHeader>
      <Table>
        <TableHeader>
          <TableRow className="hover:bg-transparent">
            <TableHead className="pl-4">Họ tên</TableHead>
            <TableHead>Quan hệ</TableHead>
            <TableHead>Được phép đón</TableHead>
            <TableHead className="pr-4">Mong muốn của người cao tuổi</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {DEMO_RELATIVES.map((r) => {
            const wish = DEMO_VISIT_WISHES.find((w) => w.about === r.name)
            return (
              <TableRow key={r.name}>
                <TableCell className="pl-4">
                  <span className="flex flex-col">
                    <span className="font-medium">{r.name}</span>
                    <span className="type-caption text-muted-foreground">
                      {[r.representative && 'Người đại diện', r.primaryContact && 'Người liên hệ chính']
                        .filter(Boolean)
                        .join(', ')}
                    </span>
                  </span>
                </TableCell>
                <TableCell>{r.relation}</TableCell>
                <TableCell>{r.canPickUp ? 'Có' : 'Không'}</TableCell>
                <TableCell className="pr-4 whitespace-normal">
                  {wish ? (
                    <span className="flex flex-col gap-1">
                      {wish.text}
                      <SourceTag source={wish.source} relative={wish.relative} />
                    </span>
                  ) : (
                    <span className="text-muted-foreground">Chưa ghi</span>
                  )}
                </TableCell>
              </TableRow>
            )
          })}
        </TableBody>
      </Table>
    </Card>
  )
}

// Yêu cầu BR-M10-07 trái mong muốn đang hiệu lực: xác nhận của người đại diện không đủ, chỉ có hiệu lực
// khi Quản lý viện duyệt, bắt buộc lý do và ghi đã trao đổi với người cao tuổi (Q-351).
function RequestCard({ canApprove }) {
  const request = DEMO_RELATIVE_REQUEST
  const conflict = DEMO_VISIT_WISHES.find((w) => w.about === request.about)
  const [reason, setReason] = useState('')
  const [talked, setTalked] = useState(false)
  const [submitted, setSubmitted] = useState(false)
  const [done, setDone] = useState(false)
  const reasonMissing = !reason.trim()

  function approve(event) {
    event.preventDefault()
    setSubmitted(true)
    if (!reasonMissing && talked) setDone(true)
  }

  return (
    <Card>
      <CardHeader className="flex flex-row flex-wrap items-center justify-between gap-2">
        <CardTitle className="type-card-title font-semibold">{request.change}</CardTitle>
        <StatusBadge status={request.status} />
      </CardHeader>
      <CardContent className="flex flex-col gap-4 type-body-sm">
        <p className="text-muted-foreground tabular-nums">
          Yêu cầu của {request.requestedBy}, {request.requestedAt}
        </p>
        {conflict && (
          <Alert variant="attention">
            <TriangleAlert aria-hidden="true" />
            <AlertTitle>Trái mong muốn của người cao tuổi</AlertTitle>
            <AlertDescription>
              {conflict.text}, ghi ngày {conflict.recordedAt}. Xác nhận của người đại diện không đủ; yêu cầu chỉ có hiệu lực khi
              Quản lý viện duyệt.
            </AlertDescription>
          </Alert>
        )}
        {canApprove ? (
          <form onSubmit={approve} className="flex flex-col gap-4" noValidate>
            <Field data-invalid={(submitted && reasonMissing) || undefined}>
              <FieldLabel htmlFor="approve-reason">Lý do duyệt</FieldLabel>
              <Textarea
                id="approve-reason"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                rows={3}
                aria-invalid={(submitted && reasonMissing) || undefined}
              />
              {submitted && reasonMissing && <FieldError>Nhập lý do duyệt</FieldError>}
            </Field>
            <Field orientation="horizontal" data-invalid={(submitted && !talked) || undefined}>
              <Checkbox
                id="talked"
                checked={talked}
                onCheckedChange={setTalked}
                aria-invalid={(submitted && !talked) || undefined}
              />
              <FieldLabel htmlFor="talked">Đã trao đổi với người cao tuổi</FieldLabel>
            </Field>
            {submitted && !talked && <FieldError>Xác nhận đã trao đổi với người cao tuổi trước khi duyệt</FieldError>}
            <div className="flex flex-wrap items-center gap-3">
              <Button type="submit" className="h-11">
                Duyệt yêu cầu
              </Button>
              <p role="status">{done && 'Bản mẫu: lệnh duyệt hợp lệ. Trạng thái thật do backend trả.'}</p>
            </div>
          </form>
        ) : (
          <p>Chờ Quản lý viện duyệt.</p>
        )}
      </CardContent>
    </Card>
  )
}

export function RelativesPage({ role }) {
  const resident = DEMO_RESIDENTS[0]
  return (
    <div className="mx-auto flex max-w-4xl flex-col gap-6">
      <PageHeader title={`Người thân của ${resident.name}`} subtitle={`${resident.code}, phòng ${resident.room}`} />
      <WishesCard canRecord={WISH_RECORDERS.includes(role)} />
      <RelativesTable />
      <RequestCard canApprove={role === APPROVER} />
    </div>
  )
}
