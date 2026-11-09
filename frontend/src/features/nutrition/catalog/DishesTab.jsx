import { useEffect, useState } from 'react'
import {
  getNutritionDishes,
  postNutritionDishes,
  postNutritionDishesByIdByCommand,
  putNutritionDishesById,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { CheckList, Info, ReasonField } from '../NutritionParts'
import { useCommand, useLoad } from '../useNutrition'
import {
  DISH_COMMAND,
  DISH_REASON_COMMANDS,
  DISH_STATUS,
  TEXTURE,
  dishCommands,
  dishContent,
  label,
} from '../nutritionView'

// Món ăn (011 FR-004, FR-004a; Q-283, Q-308): Nhân viên bếp lập món, gửi xác nhận, gửi bản sửa; Bác sĩ xác nhận hoặc
// trả lại. Nút theo trạng thái và quyền "Món ăn, thực đơn" (bếp CREATE, Bác sĩ APPROVE); backend kiểm lại.

const EMPTY = { name: '', note: '', ingredients: [], noCatalogIngredient: false, suitableDietIds: [], texture: '' }

export function DishForm({
  title,
  description,
  initial,
  allergens,
  diets,
  submitLabel,
  onSubmit,
  onClose,
  busy,
  failure,
}) {
  const [form, setForm] = useState(initial ?? EMPTY)
  const [submitted, setSubmitted] = useState(false)
  const set = (patch) => setForm((f) => ({ ...f, ...patch }))
  const errors = {
    name: !form.name.trim() ? 'Nhập tên món' : null,
    diets: form.suitableDietIds.length === 0 ? 'Chọn ít nhất một chế độ ăn phù hợp' : null,
    texture: !form.texture ? 'Chọn kết cấu' : null,
    ingredients:
      form.ingredients.length === 0 && !form.noCatalogIngredient
        ? 'Chọn thành phần, hoặc đánh dấu món không có thành phần trong danh mục'
        : null,
  }
  function submit() {
    setSubmitted(true)
    if (Object.values(errors).some(Boolean)) return
    onSubmit({ ...form, name: form.name.trim(), note: form.note.trim() })
  }
  const show = (key) => (submitted ? errors[key] : null)
  return (
    <Sheet open onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-2xl">
        <SheetTitle className="type-title">{title}</SheetTitle>
        <SheetDescription className="type-body-sm text-muted-foreground">{description}</SheetDescription>
        <FieldGroup className="mt-4">
          <Field data-invalid={!!show('name') || undefined}>
            <FieldLabel htmlFor="dish-name">Tên món</FieldLabel>
            <Input
              id="dish-name"
              className="h-11"
              value={form.name}
              onChange={(e) => set({ name: e.target.value })}
              aria-invalid={!!show('name') || undefined}
            />
            {show('name') && <FieldError>{show('name')}</FieldError>}
          </Field>
          <Field>
            <FieldLabel htmlFor="dish-note">Ghi chú</FieldLabel>
            <Input id="dish-note" className="h-11" value={form.note} onChange={(e) => set({ note: e.target.value })} />
          </Field>
          <CheckList
            idPrefix="dish-ingredient"
            legend="Thành phần"
            options={allergens}
            value={form.ingredients}
            onChange={(ingredients) => set({ ingredients })}
            error={show('ingredients')}
          />
          <Field orientation="horizontal">
            <Checkbox
              id="dish-no-catalog"
              checked={form.noCatalogIngredient}
              onCheckedChange={(c) => set({ noCatalogIngredient: c === true })}
            />
            <FieldLabel htmlFor="dish-no-catalog">Món không có thành phần trong danh mục</FieldLabel>
          </Field>
          <CheckList
            idPrefix="dish-diet"
            legend="Chế độ ăn phù hợp"
            options={diets}
            value={form.suitableDietIds}
            onChange={(suitableDietIds) => set({ suitableDietIds })}
            error={show('diets')}
          />
          <Field data-invalid={!!show('texture') || undefined}>
            <FieldLabel>Kết cấu</FieldLabel>
            <ToggleGroup
              variant="outline"
              value={form.texture ? [form.texture] : []}
              onValueChange={(n) => n[0] && set({ texture: n[0] })}
              aria-label="Kết cấu"
            >
              {Object.entries(TEXTURE).map(([code, text]) => (
                <ToggleGroupItem key={code} value={code} className="h-11 px-4">
                  {text}
                </ToggleGroupItem>
              ))}
            </ToggleGroup>
            {show('texture') && <FieldError>{show('texture')}</FieldError>}
          </Field>
        </FieldGroup>
        <div className="mt-4 flex flex-col gap-3">
          <ProblemAlert problem={failure} />
          <div className="flex justify-end gap-2">
            <Button variant="ghost" className="h-11" onClick={onClose}>
              Đóng
            </Button>
            <Button className="h-11" disabled={busy} onClick={submit}>
              {submitLabel}
            </Button>
          </div>
        </div>
      </SheetContent>
    </Sheet>
  )
}

function DishDetail({ dish, allergens, diets, kitchen, doctor, onChanged, Title, Description }) {
  const [mode, setMode] = useState(null)
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const { busy, failure, setFailure, run } = useCommand()
  const content = dishContent(dish)
  const names = (ids, list) => ids?.map((id) => list.find((x) => x.id === id)?.name ?? `mã ${id}`).join(', ')

  async function command(cmd, body, message) {
    const ok = await run(postNutritionDishesByIdByCommand, { path: { id: dish.id, command: cmd }, body })
    if (ok) {
      setMode(null)
      setReason('')
      onChanged(message)
    }
  }

  function start(cmd) {
    setFailure(null)
    setSubmitted(false)
    if (DISH_REASON_COMMANDS.has(cmd)) setMode(cmd)
    else command(cmd, {}, `${DISH_COMMAND[cmd]}: xong`)
  }

  const commands = dishCommands(dish, { kitchen, doctor })
  const reasonError = submitted && !reason.trim() ? 'Nhập lý do' : null

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <StatusBadge status={label(DISH_STATUS, dish.status)} />
        <Title className="type-title">{dish.name}</Title>
        <Description className="type-body-sm text-muted-foreground">
          {dish.pendingRevision ? 'Có bản sửa chờ Bác sĩ xác nhận' : 'Nội dung đang áp dụng'}
        </Description>
      </div>
      {content && (
        <div className="grid gap-3">
          <Info label="Thành phần">
            {content.ingredients?.length ? names(content.ingredients, allergens) : 'Không có thành phần trong danh mục'}
          </Info>
          <Info label="Chế độ ăn phù hợp">{names(content.suitableDietIds, diets)}</Info>
          <Info label="Kết cấu">{label(TEXTURE, content.texture)}</Info>
          {content.note && <Info label="Ghi chú">{content.note}</Info>}
        </div>
      )}
      <ProblemAlert problem={failure} />
      {mode && (
        <div className="flex flex-col gap-3 border-t pt-4">
          <ReasonField id="dish-reason" value={reason} onChange={setReason} error={reasonError} />
          <div className="flex justify-end gap-2">
            <Button variant="ghost" className="h-11" onClick={() => setMode(null)}>
              Thôi
            </Button>
            <Button
              variant={mode === 'cancel' || mode === 'deactivate' ? 'destructive' : 'default'}
              className="h-11"
              disabled={busy}
              onClick={() => {
                setSubmitted(true)
                if (reason.trim())
                  command(mode, { reason: reason.trim() }, `${DISH_COMMAND[mode].replace('…', '')}: xong`)
              }}
            >
              {DISH_COMMAND[mode].replace('…', '')}
            </Button>
          </div>
        </div>
      )}
      {!mode && (commands.length > 0 || kitchen) && (
        <div className="flex flex-wrap justify-end gap-2 border-t pt-4">
          {kitchen && dish.status === 'DRAFT' && (
            <Button variant="outline" className="h-11" onClick={() => setMode('edit')}>
              Sửa món
            </Button>
          )}
          {kitchen && dish.status === 'ACTIVE' && !dish.pendingRevision && (
            <Button variant="outline" className="h-11" onClick={() => setMode('revision')}>
              Gửi bản sửa…
            </Button>
          )}
          {commands.map((cmd) => (
            <Button
              key={cmd}
              variant={cmd === 'confirm' || cmd === 'submit' ? 'default' : 'outline'}
              className="h-11"
              disabled={busy}
              onClick={() => start(cmd)}
            >
              {DISH_COMMAND[cmd]}
            </Button>
          ))}
        </div>
      )}
      {(mode === 'edit' || mode === 'revision') && (
        <DishForm
          title={mode === 'edit' ? `Sửa món ${dish.name}` : `Bản sửa của ${dish.name}`}
          description={
            mode === 'edit'
              ? 'Sửa nội dung món Nháp.'
              : 'Bản sửa chờ Bác sĩ xác nhận; món vẫn dùng nội dung cũ tới khi được xác nhận.'
          }
          initial={{ ...EMPTY, ...content, name: dish.name, note: content?.note ?? '' }}
          allergens={allergens}
          diets={diets}
          submitLabel={mode === 'edit' ? 'Lưu món' : 'Gửi bản sửa'}
          busy={busy}
          failure={failure}
          onClose={() => setMode(null)}
          onSubmit={async (body) => {
            const ok =
              mode === 'edit'
                ? await run(putNutritionDishesById, {
                    path: { id: dish.id },
                    headers: { 'If-Match': `"${dish.version}"` },
                    body,
                  })
                : await run(postNutritionDishesByIdByCommand, {
                    path: { id: dish.id, command: 'submit-revision' },
                    body: { content: body },
                  })
            if (ok) {
              setMode(null)
              onChanged(mode === 'edit' ? 'Đã lưu món' : 'Đã gửi bản sửa')
            }
          }}
        />
      )}
    </div>
  )
}

export function DishesTab({ allergens, diets, kitchen, doctor, openedKey, open, announce }) {
  const [creating, setCreating] = useState(false)
  const dishes = useLoad(() => getNutritionDishes({ query: {} }).then((r) => r.data), [])
  const create = useCommand()

  function changed(message) {
    announce(message)
    dishes.reload()
  }

  function openDish(dish) {
    open(`dish-${dish.id}`, 'Chi tiết món', (titles) => (
      <DishDetail
        key={dish.id}
        dish={dish}
        allergens={allergens}
        diets={diets}
        kitchen={kitchen}
        doctor={doctor}
        onChanged={changed}
        {...titles}
      />
    ))
  }

  // Sau lệnh, danh sách tải lại: khung đang mở nhận dữ liệu mới
  useEffect(() => {
    const d = dishes.data?.find((x) => openedKey === `dish-${x.id}`)
    if (d) openDish(d)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dishes.data])

  return (
    <div className="flex flex-col gap-4">
      {kitchen && (
        <div>
          <Button className="h-11" onClick={() => setCreating(true)}>
            Thêm món
          </Button>
        </div>
      )}
      {creating && (
        <DishForm
          title="Thêm món"
          description="Món mới ở Nháp; gửi xác nhận để Bác sĩ xác nhận trước khi dùng trong thực đơn."
          allergens={allergens}
          diets={diets}
          submitLabel="Lưu món"
          busy={create.busy}
          failure={create.failure}
          onClose={() => setCreating(false)}
          onSubmit={async (body) => {
            if (await create.run(postNutritionDishes, { body })) {
              setCreating(false)
              changed('Đã thêm món')
            }
          }}
        />
      )}
      <ProblemAlert problem={dishes.failure} />
      {dishes.data === null ? (
        !dishes.failure && <Skeleton className="h-24 w-full" />
      ) : dishes.data.length === 0 ? (
        <p className="type-body-sm text-muted-foreground">Chưa có món nào. Nhân viên bếp thêm món để lập thực đơn.</p>
      ) : (
        <Card className="py-0">
          <Table aria-label="Món ăn">
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="pl-4">Món</TableHead>
                <TableHead>Kết cấu</TableHead>
                <TableHead className="pr-4">Trạng thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {dishes.data.map((d) => (
                <TableRow key={d.id} className={openedKey === `dish-${d.id}` ? 'bg-accent/40' : undefined}>
                  <TableCell className="whitespace-normal pl-4">
                    <button
                      type="button"
                      className="text-left underline-offset-4 hover:underline"
                      aria-pressed={openedKey === `dish-${d.id}`}
                      onClick={() => openDish(d)}
                    >
                      {d.name}
                    </button>
                    {d.pendingRevision && (
                      <span className="block type-caption text-muted-foreground">có bản sửa chờ xác nhận</span>
                    )}
                  </TableCell>
                  <TableCell>{label(TEXTURE, dishContent(d)?.texture)}</TableCell>
                  <TableCell className="pr-4">
                    <StatusBadge status={label(DISH_STATUS, d.status)} />
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
