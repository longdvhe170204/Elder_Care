import { useEffect, useState } from 'react'
import {
  getNutritionForbiddenFoodTypes,
  getNutritionMeals,
  postNutritionDiets,
  postNutritionDietsByIdDeactivate,
  postNutritionFoodIngredients,
  postNutritionForbiddenFoodTypes,
  postNutritionMeals,
  putNutritionMealsById,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { CheckList, Info, ReasonField, SELECT } from '../NutritionParts'
import { useCommand, useLoad } from '../useNutrition'
import { CATALOG_STATUS, label } from '../nutritionView'

// Chế độ ăn (FR-001, FR-002, FR-005: Bác sĩ), bữa ăn (FR-006) và loại đồ ăn cấm (FR-007) (Q-401: Quản lý viện). Hợp đồng
// Diet không trả version nên web chưa sửa chế độ ăn (PUT cần If-Match); chỉ tạo và Ngừng hiệu lực.

function Bool({ id, label: text, checked, onChange }) {
  return (
    <Field orientation="horizontal">
      <Checkbox id={id} checked={checked} onCheckedChange={(c) => onChange(c === true)} />
      <FieldLabel htmlFor={id}>{text}</FieldLabel>
    </Field>
  )
}

function SheetFrame({ title, description, onClose, failure, busy, submitLabel, onSubmit, children }) {
  return (
    <Sheet open onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-2xl">
        <SheetTitle className="type-title">{title}</SheetTitle>
        <SheetDescription className="type-body-sm text-muted-foreground">{description}</SheetDescription>
        <FieldGroup className="mt-4">{children}</FieldGroup>
        <div className="mt-4 flex flex-col gap-3">
          <ProblemAlert problem={failure} />
          <div className="flex justify-end gap-2">
            <Button variant="ghost" className="h-11" onClick={onClose}>
              Đóng
            </Button>
            <Button className="h-11" disabled={busy} onClick={onSubmit}>
              {submitLabel}
            </Button>
          </div>
        </div>
      </SheetContent>
    </Sheet>
  )
}

function DietForm({ allergens, activeDishes, onClose, onDone }) {
  const [form, setForm] = useState({
    name: '',
    description: '',
    treatmentRelated: false,
    isDefault: false,
    restrictions: [],
    safeDishId: '',
  })
  const [submitted, setSubmitted] = useState(false)
  const { busy, failure, run } = useCommand()
  const set = (patch) => setForm((f) => ({ ...f, ...patch }))
  const nameError = submitted && !form.name.trim() ? 'Nhập tên chế độ ăn' : null
  async function submit() {
    setSubmitted(true)
    if (!form.name.trim()) return
    const body = { ...form, name: form.name.trim(), safeDishId: form.safeDishId ? Number(form.safeDishId) : null }
    if (await run(postNutritionDiets, { body })) onDone('Đã thêm chế độ ăn')
  }
  return (
    <SheetFrame
      title="Thêm chế độ ăn"
      description="Món an toàn là món dùng khi không còn món phù hợp cho người dùng chế độ này (FR-005, FR-037a)."
      onClose={onClose}
      failure={failure}
      busy={busy}
      submitLabel="Lưu chế độ ăn"
      onSubmit={submit}
    >
      <Field data-invalid={!!nameError || undefined}>
        <FieldLabel htmlFor="diet-name">Tên chế độ ăn</FieldLabel>
        <Input
          id="diet-name"
          className="h-11"
          value={form.name}
          onChange={(e) => set({ name: e.target.value })}
          aria-invalid={!!nameError || undefined}
        />
        {nameError && <FieldError>{nameError}</FieldError>}
      </Field>
      <Field>
        <FieldLabel htmlFor="diet-description">Mô tả</FieldLabel>
        <Input
          id="diet-description"
          className="h-11"
          value={form.description}
          onChange={(e) => set({ description: e.target.value })}
        />
      </Field>
      <Bool
        id="diet-treatment"
        label="Liên quan điều trị"
        checked={form.treatmentRelated}
        onChange={(v) => set({ treatmentRelated: v })}
      />
      <Bool
        id="diet-default"
        label="Chế độ ăn mặc định của viện"
        checked={form.isDefault}
        onChange={(v) => set({ isDefault: v })}
      />
      <CheckList
        idPrefix="diet-restriction"
        legend="Thành phần hạn chế"
        options={allergens}
        value={form.restrictions}
        onChange={(restrictions) => set({ restrictions })}
      />
      <Field>
        <FieldLabel htmlFor="diet-safe-dish">Món an toàn</FieldLabel>
        <select
          id="diet-safe-dish"
          className={SELECT}
          value={form.safeDishId}
          onChange={(e) => set({ safeDishId: e.target.value })}
        >
          <option value="">Chưa chọn</option>
          {activeDishes.map((d) => (
            <option key={d.id} value={d.id}>
              {d.name}
            </option>
          ))}
        </select>
      </Field>
    </SheetFrame>
  )
}

function DietDetail({ diet, allergens, dishes, doctor, onChanged, Title, Description }) {
  const [mode, setMode] = useState(null)
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const { busy, failure, run } = useCommand()
  const names = (ids) =>
    ids?.length ? ids.map((id) => allergens.find((a) => a.id === id)?.name ?? `mã ${id}`).join(', ') : 'Không có'
  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <StatusBadge status={label(CATALOG_STATUS, diet.status)} />
        <Title className="type-title">{diet.name}</Title>
        <Description className="type-body-sm text-muted-foreground">
          {[diet.isDefault && 'Chế độ ăn mặc định', diet.treatmentRelated && 'liên quan điều trị']
            .filter(Boolean)
            .join(', ') || 'Chế độ ăn'}
        </Description>
      </div>
      <div className="grid gap-3">
        {diet.description && <Info label="Mô tả">{diet.description}</Info>}
        <Info label="Thành phần hạn chế">{names(diet.restrictions)}</Info>
        <Info label="Món an toàn">
          {dishes.find((d) => d.id === diet.safeDishId)?.name ??
            (diet.safeDishId ? `Món mã ${diet.safeDishId}` : 'Chưa chọn')}
        </Info>
      </div>
      <ProblemAlert problem={failure} />
      {doctor && diet.status === 'ACTIVE' && !diet.isDefault && (
        <div className="flex flex-col gap-3 border-t pt-4">
          {mode === 'deactivate' && (
            <ReasonField
              id="diet-reason"
              value={reason}
              onChange={setReason}
              error={submitted && !reason.trim() ? 'Nhập lý do' : null}
            />
          )}
          <div className="flex justify-end gap-2">
            {mode === 'deactivate' ? (
              <>
                <Button variant="ghost" className="h-11" onClick={() => setMode(null)}>
                  Thôi
                </Button>
                <Button
                  variant="destructive"
                  className="h-11"
                  disabled={busy}
                  onClick={async () => {
                    setSubmitted(true)
                    if (!reason.trim()) return
                    if (
                      await run(postNutritionDietsByIdDeactivate, {
                        path: { id: diet.id },
                        body: { reason: reason.trim() },
                      })
                    )
                      onChanged('Đã ngừng hiệu lực chế độ ăn')
                  }}
                >
                  Ngừng hiệu lực
                </Button>
              </>
            ) : (
              <Button variant="outline" className="h-11" onClick={() => setMode('deactivate')}>
                Ngừng hiệu lực…
              </Button>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

function IngredientForm({ onClose, onDone }) {
  const [name, setName] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const { busy, failure, run } = useCommand()
  const error = submitted && !name.trim() ? 'Nhập tên thành phần' : null
  return (
    <SheetFrame
      title="Thêm thành phần thực phẩm"
      description="Thành phần thêm vào danh mục dị nguyên nhóm thực phẩm, dùng cho món và hạn chế (FR-003)."
      onClose={onClose}
      failure={failure}
      busy={busy}
      submitLabel="Thêm thành phần"
      onSubmit={async () => {
        setSubmitted(true)
        if (name.trim() && (await run(postNutritionFoodIngredients, { body: { name: name.trim() } })))
          onDone('Đã thêm thành phần')
      }}
    >
      <Field data-invalid={!!error || undefined}>
        <FieldLabel htmlFor="ingredient-name">Tên thành phần</FieldLabel>
        <Input
          id="ingredient-name"
          className="h-11"
          value={name}
          onChange={(e) => setName(e.target.value)}
          aria-invalid={!!error || undefined}
        />
        {error && <FieldError>{error}</FieldError>}
      </Field>
    </SheetFrame>
  )
}

export function DietsTab({ diets, allergens, dishes, doctor, openedKey, open, announce, reloadShared }) {
  const [sheet, setSheet] = useState(null)

  function changed(message) {
    setSheet(null)
    announce(message)
    reloadShared()
  }

  function openDiet(d) {
    open(`diet-${d.id}`, 'Chi tiết chế độ ăn', (titles) => (
      <DietDetail
        key={d.id}
        diet={d}
        allergens={allergens}
        dishes={dishes}
        doctor={doctor}
        onChanged={changed}
        {...titles}
      />
    ))
  }

  useEffect(() => {
    const d = diets.find((x) => openedKey === `diet-${x.id}`)
    if (d) openDiet(d)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [diets])

  return (
    <div className="flex flex-col gap-4">
      {doctor && (
        <div className="flex flex-wrap gap-2">
          <Button className="h-11" onClick={() => setSheet('diet')}>
            Thêm chế độ ăn
          </Button>
          <Button variant="outline" className="h-11" onClick={() => setSheet('ingredient')}>
            Thêm thành phần thực phẩm
          </Button>
        </div>
      )}
      {sheet === 'diet' && (
        <DietForm
          allergens={allergens}
          activeDishes={dishes.filter((d) => d.status === 'ACTIVE')}
          onClose={() => setSheet(null)}
          onDone={changed}
        />
      )}
      {sheet === 'ingredient' && <IngredientForm onClose={() => setSheet(null)} onDone={changed} />}
      {diets.length === 0 ? (
        <p className="type-body-sm text-muted-foreground">
          Chưa có chế độ ăn nào. Bác sĩ thêm chế độ ăn trước khi gán cho người cao tuổi.
        </p>
      ) : (
        <Card className="py-0">
          <Table aria-label="Chế độ ăn">
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="pl-4">Chế độ ăn</TableHead>
                <TableHead>Số thành phần hạn chế</TableHead>
                <TableHead className="pr-4">Trạng thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {diets.map((d) => (
                <TableRow key={d.id} className={openedKey === `diet-${d.id}` ? 'bg-accent/40' : undefined}>
                  <TableCell className="whitespace-normal pl-4">
                    <button
                      type="button"
                      className="text-left underline-offset-4 hover:underline"
                      aria-pressed={openedKey === `diet-${d.id}`}
                      onClick={() => openDiet(d)}
                    >
                      {d.name}
                    </button>
                    {d.isDefault && <span className="block type-caption text-muted-foreground">mặc định</span>}
                  </TableCell>
                  <TableCell className="tabular-nums">{d.restrictions?.length ?? 0}</TableCell>
                  <TableCell className="pr-4">
                    <StatusBadge status={label(CATALOG_STATUS, d.status)} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Card>
      )}
    </div>
  )
}

function MealForm({ meal, onClose, onDone }) {
  const [form, setForm] = useState(
    meal
      ? {
          code: meal.code,
          name: meal.name,
          servesResidential: !!meal.servesResidential,
          servesDayCare: !!meal.servesDayCare,
          sortOrder: meal.sortOrder ?? 0,
          status: meal.status,
        }
      : { code: '', name: '', servesResidential: true, servesDayCare: false, sortOrder: 0, status: 'ACTIVE' },
  )
  const [submitted, setSubmitted] = useState(false)
  const { busy, failure, run } = useCommand()
  const set = (patch) => setForm((f) => ({ ...f, ...patch }))
  const errors = { code: !form.code.trim() ? 'Nhập mã bữa' : null, name: !form.name.trim() ? 'Nhập tên bữa' : null }
  async function submit() {
    setSubmitted(true)
    if (errors.code || errors.name) return
    const body = { ...form, code: form.code.trim(), name: form.name.trim(), sortOrder: Number(form.sortOrder) || 0 }
    const ok = meal
      ? await run(putNutritionMealsById, { path: { id: meal.id }, body })
      : await run(postNutritionMeals, { body })
    if (ok) onDone(meal ? 'Đã lưu bữa' : 'Đã thêm bữa')
  }
  const show = (k) => (submitted ? errors[k] : null)
  return (
    <SheetFrame
      title={meal ? `Sửa bữa ${meal.name}` : 'Thêm bữa'}
      description="Giờ bữa dự kiến và ngưỡng giao trễ đặt ở tham số CFG-M08-04. Bữa đã có lần chốt chỉ Ngừng hiệu lực, không xóa."
      onClose={onClose}
      failure={failure}
      busy={busy}
      submitLabel="Lưu bữa"
      onSubmit={submit}
    >
      <Field data-invalid={!!show('code') || undefined}>
        <FieldLabel htmlFor="meal-code">Mã bữa</FieldLabel>
        <Input
          id="meal-code"
          className="h-11"
          value={form.code}
          disabled={!!meal}
          onChange={(e) => set({ code: e.target.value })}
          aria-invalid={!!show('code') || undefined}
        />
        {show('code') && <FieldError>{show('code')}</FieldError>}
      </Field>
      <Field data-invalid={!!show('name') || undefined}>
        <FieldLabel htmlFor="meal-name">Tên bữa</FieldLabel>
        <Input
          id="meal-name"
          className="h-11"
          value={form.name}
          onChange={(e) => set({ name: e.target.value })}
          aria-invalid={!!show('name') || undefined}
        />
        {show('name') && <FieldError>{show('name')}</FieldError>}
      </Field>
      <Bool
        id="meal-residential"
        label="Phục vụ nội trú"
        checked={form.servesResidential}
        onChange={(v) => set({ servesResidential: v })}
      />
      <Bool
        id="meal-daycare"
        label="Phục vụ bán trú"
        checked={form.servesDayCare}
        onChange={(v) => set({ servesDayCare: v })}
      />
      <Field>
        <FieldLabel htmlFor="meal-order">Thứ tự trong ngày</FieldLabel>
        <Input
          id="meal-order"
          type="number"
          className="h-11"
          value={form.sortOrder}
          onChange={(e) => set({ sortOrder: e.target.value })}
        />
      </Field>
      {meal && (
        <Bool
          id="meal-active"
          label="Đang hiệu lực"
          checked={form.status === 'ACTIVE'}
          onChange={(v) => set({ status: v ? 'ACTIVE' : 'INACTIVE' })}
        />
      )}
    </SheetFrame>
  )
}

export function MealsTab({ manager, openedKey, open, announce }) {
  const meals = useLoad(() => getNutritionMeals().then((r) => r.data), [])
  const [editing, setEditing] = useState(null)

  function changed(message) {
    setEditing(null)
    announce(message)
    meals.reload()
  }

  function openMeal(m) {
    open(`meal-${m.id}`, 'Chi tiết bữa', ({ Title, Description }) => (
      <div className="flex flex-col gap-4">
        <div className="flex flex-col gap-1">
          <StatusBadge status={label(CATALOG_STATUS, m.status)} />
          <Title className="type-title">{m.name}</Title>
          <Description className="type-body-sm text-muted-foreground">Mã {m.code}</Description>
        </div>
        <div className="grid gap-3">
          <Info label="Phục vụ">
            {[m.servesResidential && 'nội trú', m.servesDayCare && 'bán trú'].filter(Boolean).join(', ') ||
              'Chưa chọn loại lưu trú'}
          </Info>
          <Info label="Giờ dự kiến (CFG-M08-04)">{m.expectedTime ?? 'Viện chưa đặt'}</Info>
          <Info label="Ngưỡng giao trễ">
            {m.lateDeliveryThresholdMinutes != null ? `${m.lateDeliveryThresholdMinutes} phút` : null}
          </Info>
        </div>
        {manager && (
          <div className="flex justify-end border-t pt-4">
            <Button className="h-11" onClick={() => setEditing(m)}>
              Sửa bữa
            </Button>
          </div>
        )}
      </div>
    ))
  }

  useEffect(() => {
    const m = meals.data?.find((x) => openedKey === `meal-${x.id}`)
    if (m) openMeal(m)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [meals.data])

  return (
    <div className="flex flex-col gap-4">
      {manager && (
        <div>
          <Button className="h-11" onClick={() => setEditing('new')}>
            Thêm bữa
          </Button>
        </div>
      )}
      {editing && (
        <MealForm meal={editing === 'new' ? null : editing} onClose={() => setEditing(null)} onDone={changed} />
      )}
      <ProblemAlert problem={meals.failure} />
      {meals.data === null ? (
        !meals.failure && <Skeleton className="h-24 w-full" />
      ) : (
        <Card className="py-0">
          <Table aria-label="Bữa ăn">
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="pl-4">Bữa</TableHead>
                <TableHead>Giờ dự kiến</TableHead>
                <TableHead>Phục vụ</TableHead>
                <TableHead className="pr-4">Trạng thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {meals.data.map((m) => (
                <TableRow key={m.id} className={openedKey === `meal-${m.id}` ? 'bg-accent/40' : undefined}>
                  <TableCell className="pl-4">
                    <button
                      type="button"
                      className="text-left underline-offset-4 hover:underline"
                      aria-pressed={openedKey === `meal-${m.id}`}
                      onClick={() => openMeal(m)}
                    >
                      {m.name}
                    </button>
                  </TableCell>
                  <TableCell className="tabular-nums">{m.expectedTime ?? 'Chưa đặt'}</TableCell>
                  <TableCell>
                    {[m.servesResidential && 'Nội trú', m.servesDayCare && 'Bán trú'].filter(Boolean).join(', ') || '—'}
                  </TableCell>
                  <TableCell className="pr-4">
                    <StatusBadge status={label(CATALOG_STATUS, m.status)} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Card>
      )}
    </div>
  )
}

export function ForbiddenTypesTab({ manager, announce }) {
  const types = useLoad(() => getNutritionForbiddenFoodTypes().then((r) => r.data), [])
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const { busy, failure, run } = useCommand()

  async function save(body, message) {
    if (await run(postNutritionForbiddenFoodTypes, { body })) {
      setName('')
      setDescription('')
      setSubmitted(false)
      announce(message)
      types.reload()
    }
  }

  const nameError = submitted && !name.trim() ? 'Nhập tên loại đồ ăn' : null
  return (
    <div className="flex flex-col gap-4">
      {manager && (
        <Card className="gap-3 p-4">
          <h3 className="type-card-title">Thêm loại đồ ăn bị cấm</h3>
          <div className="grid gap-3 sm:grid-cols-2">
            <Field data-invalid={!!nameError || undefined}>
              <FieldLabel htmlFor="forbidden-name">Tên loại</FieldLabel>
              <Input
                id="forbidden-name"
                className="h-11"
                value={name}
                onChange={(e) => setName(e.target.value)}
                aria-invalid={!!nameError || undefined}
              />
              {nameError && <FieldError>{nameError}</FieldError>}
            </Field>
            <Field>
              <FieldLabel htmlFor="forbidden-description">Mô tả</FieldLabel>
              <Input
                id="forbidden-description"
                className="h-11"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </Field>
          </div>
          <ProblemAlert problem={failure} />
          <div className="flex justify-end">
            <Button
              className="h-11"
              disabled={busy}
              onClick={() => {
                setSubmitted(true)
                if (name.trim())
                  save(
                    { name: name.trim(), description: description.trim(), status: 'ACTIVE' },
                    'Đã thêm loại đồ ăn bị cấm',
                  )
              }}
            >
              Thêm loại
            </Button>
          </div>
        </Card>
      )}
      <ProblemAlert problem={types.failure} />
      {types.data === null ? (
        !types.failure && <Skeleton className="h-24 w-full" />
      ) : types.data.length === 0 ? (
        <p className="type-body-sm text-muted-foreground">
          Chưa có loại đồ ăn bị cấm nào; đồ ăn gia đình chỉ đối chiếu dị ứng, hạn chế, hạn dùng.
        </p>
      ) : (
        <Card className="py-0">
          <Table aria-label="Loại đồ ăn bị cấm">
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="pl-4">Loại</TableHead>
                <TableHead>Mô tả</TableHead>
                <TableHead>Trạng thái</TableHead>
                {manager && <TableHead className="pr-4">Lệnh</TableHead>}
              </TableRow>
            </TableHeader>
            <TableBody>
              {types.data.map((t) => (
                <TableRow key={t.id}>
                  <TableCell className="pl-4">{t.name}</TableCell>
                  <TableCell className="whitespace-normal">{t.description || '—'}</TableCell>
                  <TableCell>
                    <StatusBadge status={label(CATALOG_STATUS, t.status)} />
                  </TableCell>
                  {manager && (
                    <TableCell className="pr-4">
                      {t.status === 'ACTIVE' && (
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={busy}
                          aria-label={`Ngừng loại ${t.name}`}
                          onClick={() => save({ name: t.name, status: 'INACTIVE' }, 'Đã ngừng loại đồ ăn bị cấm')}
                        >
                          Ngừng
                        </Button>
                      )}
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Card>
      )}
    </div>
  )
}
