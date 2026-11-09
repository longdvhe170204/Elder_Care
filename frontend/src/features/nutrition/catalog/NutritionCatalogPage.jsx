import { useState } from 'react'
import { getCatalogsByType, getNutritionDiets, getNutritionDishes, getNutritionMeals } from '@/api'
import { DetailLayout } from '@/common/DetailLayout'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { Skeleton } from '@/components/ui/skeleton'
import { usePermissions } from '@/common/permissions/permissions'
import { Tabs } from '../NutritionParts'
import { useLoad } from '../useNutrition'
import { todayInVietnam } from '../nutritionDates'
import { DietsTab, ForbiddenTypesTab, MealsTab } from './CatalogTabs'
import { DishesTab } from './DishesTab'
import { MenusTab } from './MenusTab'

// Mục menu "Dinh dưỡng" (011 T011, T020; bố cục duyệt 2026-11-08): thực đơn tuần, món ăn, chế độ ăn, bữa ăn, loại đồ ăn
// cấm. Tab theo quyền (Phụ lục 27 dòng "Món ăn, thực đơn", "Chế độ ăn", "Danh mục bữa ăn, loại đồ ăn cấm"); khung chi
// tiết ở cấp màn (DESIGN.md 6.7.3), đổi tab thì đóng khung.

export function NutritionCatalogPage({ today = todayInVietnam() }) {
  const { status, can } = usePermissions()
  const [tab, setTab] = useState(null)
  const [detail, setDetail] = useState(null)
  const [announcement, setAnnouncement] = useState('')
  const shared = useLoad(
    () =>
      Promise.all([
        getNutritionDiets()
          .then((r) => r.data)
          .catch(() => []),
        getNutritionDishes({ query: {} })
          .then((r) => r.data)
          .catch(() => []),
        getNutritionMeals()
          .then((r) => r.data)
          .catch(() => []),
        getCatalogsByType({ path: { type: 'allergens' }, query: { group: 'FOOD', status: 'ACTIVE' } })
          .then((r) => r.data)
          .catch(() => []),
      ]).then(([diets, dishes, meals, allergens]) => ({ diets, dishes, meals, allergens })),
    [],
  )

  if (status === 'loading') return <Skeleton className="h-24" />
  const menuView = can('DISH_MENU', 'VIEW')
  const tabs = [
    menuView && ['menus', 'Thực đơn tuần'],
    menuView && ['dishes', 'Món ăn'],
    can('DIET', 'VIEW') && ['diets', 'Chế độ ăn'],
    can('NUTRITION_CATALOG', 'VIEW') && ['meals', 'Bữa ăn'],
    can('NUTRITION_CATALOG', 'VIEW') && ['forbidden', 'Loại đồ ăn cấm'],
  ].filter(Boolean)
  if (tabs.length === 0) return <PermissionDeniedState label="Dinh dưỡng" />

  const current = tabs.some(([k]) => k === tab) ? tab : tabs[0][0]
  const kitchen = can('DISH_MENU', 'CREATE')
  const doctor = can('DISH_MENU', 'APPROVE')
  const data = shared.data ?? { diets: [], dishes: [], meals: [], allergens: [] }
  const activeMeals = data.meals.filter((m) => m.status === 'ACTIVE')
  const props = {
    openedKey: detail?.key,
    open: (key, label, render) => setDetail({ key, label, render }),
    announce: (message) => {
      setAnnouncement(message)
      shared.reload()
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
      <PageHeader title="Dinh dưỡng" subtitle="Thực đơn tuần, món ăn, chế độ ăn và danh mục bữa ăn của viện." />
      <Tabs
        label="Phần dinh dưỡng"
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
      {current === 'menus' && (
        <MenusTab
          today={today}
          meals={activeMeals}
          diets={data.diets}
          dishes={data.dishes}
          kitchen={kitchen}
          doctor={doctor}
          {...props}
        />
      )}
      {current === 'dishes' && (
        <DishesTab allergens={data.allergens} diets={data.diets} kitchen={kitchen} doctor={doctor} {...props} />
      )}
      {current === 'diets' && (
        <DietsTab
          diets={data.diets}
          allergens={data.allergens}
          dishes={data.dishes}
          doctor={can('DIET', 'CREATE')}
          reloadShared={shared.reload}
          {...props}
        />
      )}
      {current === 'meals' && <MealsTab manager={can('NUTRITION_CATALOG', 'CREATE')} {...props} />}
      {current === 'forbidden' && (
        <ForbiddenTypesTab manager={can('NUTRITION_CATALOG', 'CREATE')} announce={props.announce} />
      )}
    </DetailLayout>
  )
}
