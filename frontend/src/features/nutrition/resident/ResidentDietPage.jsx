import { useState } from 'react'
import {
  getCatalogsByType,
  getNutritionDiets,
  getNutritionReviewNotices,
  getResidents,
  getResidentsByResidentIdDietAssignments,
  getResidentsByResidentIdFamilyFoods,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { DetailLayout } from '@/common/DetailLayout'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { formatDateTime } from '@/common/dateTime'
import { usePermissions } from '@/common/permissions/permissions'
import { useLoad } from '../useNutrition'
import { REVIEW_SOURCE, assignmentState, label } from '../nutritionView'
import {
  AssignmentSummary,
  FamilyFoodItem,
  FamilyFoodSheet,
  PendingAssignment,
  ProposeSheet,
} from './ResidentDietParts'

// Mục menu "Chế độ ăn từng người" (011 T015, T035; bố cục duyệt 2026-11-08): bảng người cao tuổi Đang lưu trú trong
// phạm vi; khung chi tiết có chế độ ăn đang áp dụng, bản chờ, thông báo xem lại (Bác sĩ) và đồ ăn gia đình. Chỉ hiện dữ
// liệu đã ghi, không gợi ý chế độ ăn (Q-306, Q-307).

function ResidentDietDetail({ resident, diets, allergens, onAnnounce, Title, Description }) {
  const { can } = usePermissions()
  const doctor = can('DIET', 'CREATE')
  const foodView = can('FAMILY_FOOD_CHECK', 'VIEW')
  const [sheet, setSheet] = useState(null)
  const assignments = useLoad(
    () => getResidentsByResidentIdDietAssignments({ path: { residentId: resident.id } }).then((r) => r.data),
    [resident.id],
  )
  const notices = useLoad(
    () =>
      doctor
        ? getNutritionReviewNotices({ query: { residentId: resident.id } }).then((r) => r.data ?? [])
        : Promise.resolve([]),
    [resident.id, doctor],
  )
  const foods = useLoad(
    () =>
      foodView
        ? getResidentsByResidentIdFamilyFoods({ path: { residentId: resident.id } }).then((r) => r.data)
        : Promise.resolve([]),
    [resident.id, foodView],
  )
  const dietName = (id) => diets.find((d) => d.id === id)?.name ?? `Chế độ ăn mã ${id}`
  const state = assignmentState(assignments.data)
  const activeDiets = diets.filter((d) => d.status === 'ACTIVE')

  function changed(message) {
    setSheet(null)
    onAnnounce(message)
    assignments.reload()
    foods.reload()
    notices.reload()
  }

  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-col gap-1">
        <Title className="type-title">{resident.name}</Title>
        <Description className="type-body-sm text-muted-foreground">{`Mã hồ sơ ${resident.code}`}</Description>
      </div>

      <section className="flex flex-col gap-3" aria-labelledby="diet-current">
        <h3 id="diet-current" className="type-card-title">
          Chế độ ăn đang áp dụng
        </h3>
        <ProblemAlert problem={assignments.failure} />
        {assignments.data === null ? (
          !assignments.failure && <Skeleton className="h-16 w-full" />
        ) : (
          <AssignmentSummary assignment={state.current} dietName={dietName} />
        )}
        {state.pending.length > 0 && (
          <>
            <h4 className="type-label">Bản chờ</h4>
            <ul className="flex flex-col gap-2">
              {state.pending.map((a) => (
                <PendingAssignment key={a.id} assignment={a} dietName={dietName} doctor={doctor} onChanged={changed} />
              ))}
            </ul>
          </>
        )}
        {doctor && state.pending.length === 0 && (
          <div>
            <Button className="h-11" onClick={() => setSheet('propose')}>
              Lập bản gán chế độ ăn
            </Button>
          </div>
        )}
      </section>

      {doctor && (notices.data ?? []).length > 0 && (
        <section className="flex flex-col gap-2 border-t pt-4" aria-labelledby="diet-notices">
          <h3 id="diet-notices" className="type-card-title">
            Thông báo xem lại chế độ ăn
          </h3>
          <ul className="flex flex-col gap-1 type-body-sm">
            {notices.data.map((n) => (
              <li key={n.id}>{`${label(REVIEW_SOURCE, n.source)}, báo lúc ${formatDateTime(n.noticedAt)}`}</li>
            ))}
          </ul>
        </section>
      )}

      {foodView && (
        <section className="flex flex-col gap-3 border-t pt-4" aria-labelledby="diet-family-food">
          <h3 id="diet-family-food" className="type-card-title">
            Đồ ăn gia đình mang vào
          </h3>
          <ProblemAlert problem={foods.failure} />
          {foods.data === null ? (
            !foods.failure && <Skeleton className="h-16 w-full" />
          ) : foods.data.length === 0 ? (
            <p className="type-body-sm text-muted-foreground">Chưa ghi nhận đồ ăn gia đình nào.</p>
          ) : (
            <ul className="flex flex-col gap-2">
              {foods.data.map((f) => (
                <FamilyFoodItem
                  key={f.id}
                  food={f}
                  canConfirm={can('FAMILY_FOOD_CHECK', 'CONFIRM')}
                  canUpdate={can('FAMILY_FOOD_CHECK', 'UPDATE')}
                  onChanged={changed}
                />
              ))}
            </ul>
          )}
          {can('FAMILY_FOOD_CHECK', 'CREATE') && (
            <div>
              <Button variant="outline" className="h-11" onClick={() => setSheet('food')}>
                Ghi nhận đồ ăn gia đình
              </Button>
            </div>
          )}
        </section>
      )}

      {sheet === 'propose' && (
        <ProposeSheet
          resident={resident}
          diets={activeDiets}
          allergens={allergens}
          notices={notices.data ?? []}
          onClose={() => setSheet(null)}
          onDone={changed}
        />
      )}
      {sheet === 'food' && (
        <FamilyFoodSheet resident={resident} allergens={allergens} onClose={() => setSheet(null)} onDone={changed} />
      )}
    </div>
  )
}

export function ResidentDietPage() {
  const { status, can } = usePermissions()
  const [q, setQ] = useState('')
  const [query, setQuery] = useState('')
  const [opened, setOpened] = useState(null)
  const [announcement, setAnnouncement] = useState('')
  const permitted = can('DIET', 'VIEW') || can('FAMILY_FOOD_CHECK', 'VIEW')
  const residents = useLoad(
    () =>
      permitted
        ? getResidents({ query: { status: ['IN_RESIDENCE'], q: query || undefined } }).then((r) => r.data.items ?? [])
        : Promise.resolve([]),
    [query, permitted],
  )
  const shared = useLoad(
    () =>
      Promise.all([
        getNutritionDiets()
          .then((r) => r.data)
          .catch(() => []),
        getCatalogsByType({ path: { type: 'allergens' }, query: { group: 'FOOD', status: 'ACTIVE' } })
          .then((r) => r.data)
          .catch(() => []),
      ]),
    [],
  )

  if (status === 'loading') return <Skeleton className="h-24" />
  if (!permitted || residents.failure?.status === 403) return <PermissionDeniedState label="Chế độ ăn từng người" />

  const rows = (residents.data ?? []).map((r) => ({
    id: r.id,
    code: r.profileCode,
    name: r.personalInfo?.fullName ?? `Người cao tuổi mã ${r.id}`,
  }))
  const current = rows.find((r) => r.id === opened)
  const [diets, allergens] = shared.data ?? [[], []]

  return (
    <DetailLayout
      open={!!current}
      detailKey={opened}
      onClose={() => setOpened(null)}
      label="Chế độ ăn của người cao tuổi"
      renderDetail={(titles) => (
        <ResidentDietDetail
          key={opened}
          resident={current}
          diets={diets}
          allergens={allergens}
          onAnnounce={setAnnouncement}
          {...titles}
        />
      )}
    >
      <PageHeader
        title="Chế độ ăn từng người"
        subtitle="Người cao tuổi Đang lưu trú trong phạm vi của bạn: chế độ ăn được gán, đồ ăn gia đình mang vào."
        aside={
          <form
            className="flex gap-2"
            role="search"
            onSubmit={(e) => {
              e.preventDefault()
              setQuery(q.trim())
            }}
          >
            <Input
              aria-label="Tìm người cao tuổi theo họ tên hoặc mã"
              className="h-11 w-64"
              value={q}
              onChange={(e) => setQ(e.target.value)}
              placeholder="Họ tên hoặc mã hồ sơ"
            />
            <Button type="submit" variant="outline" className="h-11">
              Tìm
            </Button>
          </form>
        }
      />
      <p role="status" className="empty:hidden type-body-sm">
        {announcement}
      </p>
      <ProblemAlert problem={residents.failure} />
      {residents.data === null ? (
        !residents.failure && <Skeleton className="h-24 w-full" />
      ) : rows.length === 0 ? (
        <p className="type-body-sm text-muted-foreground">
          Không có người cao tuổi Đang lưu trú nào khớp trong phạm vi của bạn.
        </p>
      ) : (
        <Card className="py-0">
          <Table aria-label="Người cao tuổi">
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="pl-4">Người cao tuổi</TableHead>
                <TableHead className="pr-4">Mã hồ sơ</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((r) => (
                <TableRow key={r.id} className={r.id === opened ? 'bg-accent/40' : undefined}>
                  <TableCell className="pl-4">
                    <button
                      type="button"
                      className="text-left underline-offset-4 hover:underline"
                      aria-pressed={r.id === opened}
                      onClick={() => setOpened(r.id)}
                    >
                      {r.name}
                    </button>
                  </TableCell>
                  <TableCell className="pr-4 tabular-nums">{r.code}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Card>
      )}
    </DetailLayout>
  )
}
