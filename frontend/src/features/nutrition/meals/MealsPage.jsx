import { useState } from 'react'
import { getCatalogsByType, getDailyMeals, getFloors, getNutritionDiets, getNutritionDishes, getResidents } from '@/api'
import { Input } from '@/components/ui/input'
import { Field, FieldLabel } from '@/components/ui/field'
import { Skeleton } from '@/components/ui/skeleton'
import { DetailLayout } from '@/common/DetailLayout'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { usePermissions } from '@/common/permissions/permissions'
import { Tabs } from '../NutritionParts'
import { useLoad } from '../useNutrition'
import { todayInVietnam } from '../nutritionDates'
import { DayTab } from './DayTab'
import { SamplesTab } from './SamplesTab'
import { SlipsTab } from './SlipsTab'

// Mục menu "Suất ăn" (011 T027, T031, T033, T039; bố cục duyệt 2026-11-08): bữa trong ngày (tính sẵn, chốt suất, phát
// sinh), phiếu bữa ăn (bếp và người nhận tại tầng, xác nhận phục vụ), lưu mẫu. Tab theo quyền các dòng "Suất ăn đã chốt,
// phiếu bữa ăn", "Chuẩn bị, giao phiếu, lưu mẫu", "Nhận phiếu, báo sai lệch", "Xác nhận phục vụ suất đặc biệt".

const safe = (promise, fallback = []) => promise.then((r) => r.data).catch(() => fallback)

export function MealsPage({ initialDate = todayInVietnam() }) {
  const { status, can } = usePermissions()
  const [date, setDate] = useState(initialDate)
  const [tab, setTab] = useState(null)
  const [detail, setDetail] = useState(null)
  const [announcement, setAnnouncement] = useState('')
  const meals = useLoad(() => getDailyMeals({ query: { date } }).then((r) => r.data), [date])
  const shared = useLoad(
    () =>
      Promise.all([
        safe(getFloors()),
        safe(getNutritionDiets()),
        safe(getNutritionDishes({ query: {} })),
        safe(getResidents({ query: { status: ['IN_RESIDENCE'] } }), { items: [] }).then((p) =>
          (p.items ?? []).map((r) => ({ id: r.id, name: r.personalInfo?.fullName ?? `Người cao tuổi mã ${r.id}` })),
        ),
        safe(getCatalogsByType({ path: { type: 'allergens' }, query: { group: 'FOOD' } })),
      ]).then(([floors, diets, dishes, residents, allergens]) => ({ floors, diets, dishes, residents, allergens })),
    [],
  )

  if (status === 'loading') return <Skeleton className="h-24" />
  const can_ = {
    count: can('MEAL_COUNT_TICKET', 'VIEW'),
    kitchen: can('MEAL_PREPARATION', 'UPDATE'),
    closer: can('MEAL_PREPARATION', 'CREATE'),
    confirmer: can('MEAL_PREPARATION', 'CONFIRM'),
    receiver: can('MEAL_RECEIPT', 'CREATE'),
    server: can('SPECIAL_MEAL_SERVICE', 'CREATE'),
    doctor: can('DIET', 'UPDATE'),
    samples: can('MEAL_PREPARATION', 'VIEW'),
  }
  const tabs = [
    can_.count && ['day', 'Bữa trong ngày'],
    (can_.count || can('MEAL_RECEIPT', 'VIEW') || can('SPECIAL_MEAL_SERVICE', 'VIEW')) && ['slips', 'Phiếu bữa ăn'],
    can_.samples && ['samples', 'Lưu mẫu'],
  ].filter(Boolean)
  if (tabs.length === 0 || meals.failure?.status === 403) return <PermissionDeniedState label="Suất ăn" />

  const current = tabs.some(([k]) => k === tab) ? tab : tabs[0][0]
  const data = shared.data ?? { floors: [], diets: [], dishes: [], residents: [], allergens: [] }
  const props = {
    openedKey: detail?.key,
    open: (key, label, render) => setDetail({ key, label, render }),
    announce: (message) => {
      setAnnouncement(message)
      meals.reload()
    },
  }

  return (
    <DetailLayout
      open={detail !== null}
      detailKey={detail?.key}
      onClose={() => setDetail(null)}
      label={detail?.label}
      renderDetail={(titles) => detail.render(titles)}
    >
      <PageHeader
        title="Suất ăn"
        subtitle="Bữa trong ngày, phiếu bữa ăn của các tầng/khu và lưu mẫu thức ăn."
        aside={
          <Field>
            <FieldLabel htmlFor="meals-date">Ngày</FieldLabel>
            <Input
              id="meals-date"
              type="date"
              className="h-11"
              value={date}
              onChange={(e) => {
                if (!e.target.value) return
                setDate(e.target.value)
                setDetail(null)
              }}
            />
          </Field>
        }
      />
      <Tabs
        label="Phần suất ăn"
        tabs={tabs}
        value={current}
        onChange={(next) => {
          setTab(next)
          setDetail(null)
        }}
      />
      <p role="status" className="empty:hidden type-body-sm">
        {announcement}
      </p>
      <ProblemAlert problem={meals.failure} />
      {meals.data === null ? (
        !meals.failure && <Skeleton className="h-24 w-full" />
      ) : (
        <>
          {current === 'day' && (
            <DayTab
              meals={meals.data}
              floors={data.floors}
              diets={data.diets}
              residents={data.residents}
              kitchen={can_.closer || can_.confirmer}
              receiver={can_.receiver}
              reload={meals.reload}
              {...props}
            />
          )}
          {current === 'slips' && (
            <SlipsTab
              meals={meals.data}
              dishes={data.dishes}
              residents={data.residents}
              allergens={data.allergens}
              can={{ kitchen: can_.kitchen, receiver: can_.receiver, server: can_.server, doctor: can_.doctor }}
              {...props}
            />
          )}
          {current === 'samples' && (
            <SamplesTab meals={meals.data} kitchen={can('MEAL_PREPARATION', 'CREATE')} announce={props.announce} />
          )}
        </>
      )}
    </DetailLayout>
  )
}
