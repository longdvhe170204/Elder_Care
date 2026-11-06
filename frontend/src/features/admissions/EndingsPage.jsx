import { useState } from 'react'
import { getResidencyEndingsById } from '@/api'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { CreateEndingSheet, EndingView } from './EndingParts'

// Kết thúc lưu trú (004 T061, T107; US7, FR-062 → FR-067): hồ sơ kết thúc, điều kiện (a) → (h), bàn giao, ngoại lệ. Hợp đồng
// api-004 chưa có danh sách hồ sơ kết thúc nên mở theo mã hồ sơ (ghi ở team-plan mục 8); lập mới thì mở ngay hồ sơ vừa lập.

export function EndingsPage({ role }) {
  const { can } = usePermissions()
  const [code, setCode] = useState('')
  const [ending, setEnding] = useState(null)
  const [failure, setFailure] = useState(null)
  const [creating, setCreating] = useState(false)
  const [announcement, setAnnouncement] = useState('')

  function open(id) {
    setFailure(null)
    return getResidencyEndingsById({ path: { id } })
      .then(({ data }) => setEnding(data))
      .catch((e) => {
        setEnding(null)
        setFailure(problemOf(e))
      })
  }

  if (failure?.status === 403) return <PermissionDeniedState label="Kết thúc lưu trú" />

  return (
    <div className="mx-auto flex max-w-[1600px] flex-col gap-5">
      <PageHeader
        title="Kết thúc lưu trú"
        subtitle="Hồ sơ kết thúc lưu trú của người cao tuổi: điều kiện, bàn giao, ngoại lệ."
        aside={
          can('RESIDENCY_END_DEATH', 'CREATE') && (
            <Button className="h-11" onClick={() => setCreating(true)}>
              Lập hồ sơ kết thúc
            </Button>
          )
        }
      />
      {creating && (
        <CreateEndingSheet
          role={role}
          onClose={() => setCreating(false)}
          onDone={(created) => {
            setCreating(false)
            setAnnouncement('Đã lập hồ sơ kết thúc lưu trú')
            setEnding(created)
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
            <FieldLabel htmlFor="ending-code">Mã hồ sơ kết thúc</FieldLabel>
            <Input
              id="ending-code"
              type="number"
              min="1"
              className="h-11"
              value={code}
              onChange={(e) => setCode(e.target.value)}
            />
          </Field>
          <Button type="submit" variant="outline" className="h-11">
            Mở hồ sơ
          </Button>
        </form>
        <p className="type-caption text-muted-foreground">
          Mã hồ sơ hiện ở thông báo khi lập hồ sơ; danh sách hồ sơ chờ hợp đồng bổ sung.
        </p>
      </Card>
      <p role="status" className="empty:hidden type-body-sm">
        {announcement}
      </p>
      <ProblemAlert problem={failure} />
      {ending && (
        <EndingView
          key={ending.id}
          ending={ending}
          role={role}
          canUpdate={can('RESIDENCY_END_DEATH', 'UPDATE')}
          onChanged={(message) => {
            setAnnouncement(message)
            open(ending.id)
          }}
        />
      )}
    </div>
  )
}
