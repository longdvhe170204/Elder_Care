import { useState } from 'react'
import { getResidencyChangeRequestsById } from '@/api'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { ChangeRequestSheet, ChangeRequestView } from './ChangeParts'

// Yêu cầu thay đổi lưu trú (004 T047; US5, FR-037 → FR-047): lập, xem, sửa ở Nháp. Hợp đồng api-004 chưa có danh sách yêu
// cầu nên mở theo mã yêu cầu (ghi ở team-plan mục 8); gửi duyệt, duyệt dùng màn Yêu cầu phê duyệt (api-000).

export function ChangeRequestsPage() {
  const { can } = usePermissions()
  const [code, setCode] = useState('')
  const [shown, setShown] = useState(null)
  const [failure, setFailure] = useState(null)
  const [sheet, setSheet] = useState(null)
  const [announcement, setAnnouncement] = useState('')

  function open(id) {
    setFailure(null)
    return getResidencyChangeRequestsById({ path: { id } })
      .then(({ data, etag }) => setShown({ request: data, etag }))
      .catch((e) => {
        setShown(null)
        setFailure(problemOf(e))
      })
  }

  if (failure?.status === 403) return <PermissionDeniedState label="Yêu cầu thay đổi lưu trú" />

  return (
    <div className="mx-auto flex max-w-[1600px] flex-col gap-5">
      <PageHeader
        title="Yêu cầu thay đổi lưu trú"
        subtitle="Đổi loại lưu trú, mức chăm sóc, dịch vụ, thời hạn, giường hoặc lịch đến của hợp đồng đang hiệu lực."
        aside={
          can('RESIDENCY_CHANGE_REQUEST', 'CREATE') && (
            <Button className="h-11" onClick={() => setSheet({})}>
              Lập yêu cầu thay đổi
            </Button>
          )
        }
      />
      {sheet && (
        <ChangeRequestSheet
          request={sheet.request}
          etag={sheet.etag}
          onClose={() => setSheet(null)}
          onDone={(message, id) => {
            setSheet(null)
            setAnnouncement(message)
            open(id)
          }}
        />
      )}
      <Card className="gap-3 p-4">
        <form
          className="flex flex-wrap items-end gap-3"
          onSubmit={(e) => {
            e.preventDefault()
            if (code) open(Number(code))
          }}
        >
          <Field className="w-48">
            <FieldLabel htmlFor="change-code">Mã yêu cầu</FieldLabel>
            <Input
              id="change-code"
              type="number"
              min="1"
              className="h-11"
              value={code}
              onChange={(e) => setCode(e.target.value)}
            />
          </Field>
          <Button type="submit" variant="outline" className="h-11">
            Mở yêu cầu
          </Button>
        </form>
        <p className="type-caption text-muted-foreground">
          Danh sách yêu cầu chờ hợp đồng bổ sung; yêu cầu vừa lập mở ngay tại đây.
        </p>
      </Card>
      <p role="status" className="empty:hidden type-body-sm">
        {announcement}
      </p>
      <ProblemAlert problem={failure} />
      {shown && (
        <ChangeRequestView
          key={shown.request.id}
          request={shown.request}
          canUpdate={can('RESIDENCY_CHANGE_REQUEST', 'UPDATE')}
          onEdit={(request) => setSheet({ request, etag: shown.etag })}
          onReload={() => open(shown.request.id)}
        />
      )}
    </div>
  )
}
