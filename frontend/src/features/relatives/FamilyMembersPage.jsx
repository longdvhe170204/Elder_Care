import { useCallback, useEffect, useState } from 'react'
import { getResidents, getResidentsByResidentIdRelationships } from '@/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { DetailLayout } from '@/common/DetailLayout'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { ResidentAvatar } from '@/common/ResidentAvatar'
import { StatusBadge } from '@/common/StatusBadge'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { AddRelationshipSheet } from './AddRelationshipSheet'
import { RegistrationFormSheet } from './RegistrationFormSheet'
import { RelationshipDetail } from './RelationshipDetail'
import { RELATIONSHIP_LABELS, enabledRightsText, rolesOf } from './relationshipLabels'

// Hồ sơ người thân và quan hệ của một người cao tuổi (012 T013; US1, FR-001 → FR-012; Phụ lục 27 dòng "Hồ sơ, quan hệ,
// vai trò người thân": Hành chính lập, sửa; Quản lý viện xem). Chọn người cao tuổi, bảng quan hệ theo thứ tự liên hệ,
// bấm dòng mở khung chi tiết (DESIGN.md 5, 7.2). Nút theo /me/permissions; backend kiểm lại ở lệnh.

const FUNCTION = 'FAMILY_MEMBER_PERMISSION'

function ResidentPicker({ onPick }) {
  const [q, setQ] = useState('')
  const [items, setItems] = useState(null)
  const [failure, setFailure] = useState(null)

  const load = useCallback(async (query) => {
    try {
      const { data } = await getResidents({ query: { q: query || undefined } })
      setItems(data.items)
      setFailure(null)
    } catch (error) {
      setItems([])
      setFailure(problemOf(error))
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- nạp danh sách người cao tuổi khi vào màn
    load('')
  }, [load])

  return (
    <Card>
      <CardContent className="flex flex-col gap-3">
        <form
          role="search"
          className="flex gap-2"
          onSubmit={(e) => {
            e.preventDefault()
            load(q.trim())
          }}
        >
          <label htmlFor="resident-search" className="sr-only">
            Tìm người cao tuổi theo họ tên hoặc mã hồ sơ
          </label>
          <Input
            id="resident-search"
            placeholder="Tìm người cao tuổi theo họ tên hoặc mã hồ sơ"
            className="h-11"
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
          <Button type="submit" variant="outline" className="h-11">
            Tìm
          </Button>
        </form>
        <ProblemAlert problem={failure} />
        {items === null ? (
          <Skeleton className="h-11 w-full" />
        ) : (
          <ul className="flex flex-col" aria-label="Người cao tuổi">
            {items.length === 0 && (
              <li className="type-body-sm text-muted-foreground">Không có người cao tuổi khớp.</li>
            )}
            {items.map((p) => (
              <li key={p.id} className="border-t first:border-t-0">
                <button
                  type="button"
                  className="flex min-h-11 w-full items-center gap-3 rounded-md px-2 py-2 text-left hover:bg-muted"
                  onClick={() => onPick({ id: p.id, name: p.personalInfo.fullName, code: p.profileCode })}
                >
                  <ResidentAvatar name={p.personalInfo.fullName} code={String(p.id)} />
                  <span className="flex flex-col">
                    <span className="type-label">{p.personalInfo.fullName}</span>
                    <span className="type-caption text-muted-foreground tabular-nums">{p.profileCode}</span>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}

function RelationshipTable({ relationships, selectedId, onSelect }) {
  return (
    <Card className="py-0">
      <Table>
        <TableHeader>
          <TableRow className="hover:bg-transparent">
            <TableHead className="pl-4">Người thân</TableHead>
            <TableHead>Quan hệ</TableHead>
            <TableHead>Thứ tự liên hệ</TableHead>
            <TableHead>Quyền đang bật</TableHead>
            <TableHead className="pr-4">Trạng thái</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {relationships.map((r) => (
            <TableRow key={r.id} className={selectedId === r.id ? 'bg-accent' : undefined}>
              <TableCell className="pl-4">
                <button
                  type="button"
                  className="flex min-h-11 flex-col items-start text-left"
                  aria-pressed={selectedId === r.id}
                  onClick={() => onSelect(r.id)}
                >
                  <span className="type-label">{r.familyMember.fullName}</span>
                  <span className="type-caption text-muted-foreground">{rolesOf(r)}</span>
                </button>
              </TableCell>
              <TableCell>{RELATIONSHIP_LABELS[r.relationship] ?? r.relationship}</TableCell>
              <TableCell className="tabular-nums">{r.contactOrder}</TableCell>
              <TableCell className="whitespace-normal">{enabledRightsText(r)}</TableCell>
              <TableCell className="pr-4">
                <StatusBadge status={r.status === 'ACTIVE' ? 'Hiệu lực' : 'Đã kết thúc'} />
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Card>
  )
}

export function FamilyMembersPage() {
  const { can } = usePermissions()
  const [resident, setResident] = useState(null)
  const [relationships, setRelationships] = useState(null)
  const [failure, setFailure] = useState(null)
  const [selectedId, setSelectedId] = useState(null)
  const [sheet, setSheet] = useState(null)
  const [announcement, setAnnouncement] = useState('')

  const load = useCallback(async (residentId) => {
    try {
      const { data } = await getResidentsByResidentIdRelationships({ path: { residentId } })
      setRelationships(data)
      setFailure(null)
    } catch (error) {
      setRelationships([])
      setFailure(problemOf(error))
    }
  }, [])

  function pick(next) {
    setResident(next)
    setRelationships(null)
    setSelectedId(null)
    load(next.id)
  }

  function changed(message) {
    setSheet(null)
    setAnnouncement(message)
    load(resident.id)
  }

  const selected = relationships?.find((r) => r.id === selectedId) ?? null
  const active = (relationships ?? []).filter((r) => r.status === 'ACTIVE')
  const hasRepresentative = active.some((r) => r.representative)
  const hasPrimaryContact = active.some((r) => r.primaryContact)

  if (failure?.status === 403) {
    return <PermissionDeniedState label="Người thân và quyền" />
  }

  return (
    <DetailLayout
      open={!!selected}
      detailKey={selectedId}
      onClose={() => setSelectedId(null)}
      label={selected ? `Chi tiết người thân ${selected.familyMember.fullName}` : 'Chi tiết người thân'}
      renderDetail={({ Title, Description }) =>
        selected && (
          <RelationshipDetail
            relationship={selected}
            hasRepresentative={hasRepresentative}
            canUpdate={can(FUNCTION, 'UPDATE')}
            onChanged={changed}
            Title={Title}
            Description={Description}
          />
        )
      }
    >
      <PageHeader
        title={resident ? `Người thân của ${resident.name}` : 'Người thân và quyền'}
        subtitle={
          resident
            ? `${resident.code}. Vai trò, thứ tự liên hệ và quyền theo từng người thân.`
            : 'Chọn người cao tuổi để xem hồ sơ người thân, vai trò và quyền.'
        }
      />
      <p role="status" className="sr-only">
        {announcement}
      </p>

      {!resident ? (
        <ResidentPicker onPick={pick} />
      ) : (
        <>
          <div className="flex flex-wrap gap-3">
            {can(FUNCTION, 'CREATE') && (
              <Button className="h-11" onClick={() => setSheet('add')}>
                Thêm người thân
              </Button>
            )}
            {can(FUNCTION, 'CREATE') && hasRepresentative && (
              <Button variant="outline" className="h-11" onClick={() => setSheet('form')}>
                Ghi phiếu đăng ký
              </Button>
            )}
            <Button variant="ghost" className="h-11" onClick={() => setResident(null)}>
              Chọn người cao tuổi khác
            </Button>
          </div>
          <ProblemAlert problem={failure} />
          {relationships === null ? (
            <Skeleton className="h-40 w-full" />
          ) : relationships.length === 0 ? (
            <Card>
              <CardContent>
                <Empty>
                  <EmptyHeader>
                    <EmptyTitle>Chưa có người thân</EmptyTitle>
                    <EmptyDescription>
                      Thêm người thân, đặt người liên hệ chính và người đại diện trước khi Hoàn tất tiếp nhận.
                    </EmptyDescription>
                  </EmptyHeader>
                </Empty>
              </CardContent>
            </Card>
          ) : (
            <RelationshipTable relationships={relationships} selectedId={selectedId} onSelect={setSelectedId} />
          )}
          {sheet === 'add' && (
            <AddRelationshipSheet
              open
              residentId={resident.id}
              residentName={resident.name}
              hasPrimaryContact={hasPrimaryContact}
              hasRepresentative={hasRepresentative}
              onClose={() => setSheet(null)}
              onDone={changed}
            />
          )}
          {sheet === 'form' && (
            <RegistrationFormSheet
              open
              residentId={resident.id}
              residentName={resident.name}
              relationships={active}
              onClose={() => setSheet(null)}
              onDone={changed}
            />
          )}
        </>
      )}
    </DetailLayout>
  )
}
