import { useEffect, useState } from 'react'
import {
  getMenus,
  getMenusById,
  postMenus,
  postMenusByIdByCommand,
  postMenusByIdCheck,
  postMenusByIdDishSwaps,
  putMenusById,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDate, formatDateTime } from '@/common/dateTime'
import { Info, ReasonField, SELECT } from '../NutritionParts'
import { useCommand, useLoad } from '../useNutrition'
import {
  MENU_COMMAND,
  MENU_STATUS,
  addDays,
  cellDishes,
  label,
  menuCommands,
  mondayOf,
  warningCodes,
  weekDays,
  weekdayLabel,
} from '../nutritionView'

// Thực đơn tuần (011 FR-024 → FR-032, BR-M08-06 → 08; Q-308): bếp lập nội dung ở Nháp, kiểm tra (độ phủ chặn; lặp món,
// dị ứng cần "đã xem"), gửi công bố; Bác sĩ công bố hoặc trả lại. Sau công bố bếp đổi món có lý do. Kết quả kiểm tra hiện
// một lần, đủ mọi mục (FR-025).

function CheckResult({ result, dishName, mealName, dietName }) {
  if (!result) return null
  const gaps = result.coverageGaps ?? []
  const repeats = result.dishRepetitions ?? []
  const allergies = result.allergies ?? []
  if (gaps.length === 0 && repeats.length === 0 && allergies.length === 0)
    return <p className="type-body-sm">Kiểm tra xong: đủ món cho mọi chế độ ăn, không có cảnh báo.</p>
  return (
    <div className="flex flex-col gap-3">
      {gaps.length > 0 && (
        <Alert variant="attention">
          <AlertTitle>{`Thiếu món (${gaps.length}), chưa gửi công bố được`}</AlertTitle>
          <AlertDescription>
            <ul className="ml-4 list-disc">
              {gaps.map((g) => (
                <li
                  key={`${g.dietId}-${g.date}-${g.mealId}`}
                >{`${dietName(g.dietId)}, ${weekdayLabel(g.date)}, ${mealName(g.mealId)}`}</li>
              ))}
            </ul>
          </AlertDescription>
        </Alert>
      )}
      {repeats.length > 0 && (
        <div className="flex flex-col gap-1">
          <h4 className="type-label">{`Lặp món (${repeats.length})`}</h4>
          <ul className="ml-4 list-disc type-body-sm">
            {repeats.map((r, i) => (
              <li
                key={i}
              >{`${dishName(r.dishId)}, ${dietName(r.dietId)}, ${formatDate(r.from)} → ${formatDate(r.to)}`}</li>
            ))}
          </ul>
        </div>
      )}
      {allergies.length > 0 && (
        <div className="flex flex-col gap-1">
          <h4 className="type-label">{`Cảnh báo dị ứng (${allergies.length})`}</h4>
          <ul className="ml-4 list-disc type-body-sm">
            {allergies.map((a, i) => (
              <li key={i}>
                {`${dishName(a.dishId)}, ${dietName(a.dietId)}, ${weekdayLabel(a.date)}, ${mealName(a.mealId)}`}
                {a.residentCount != null && `, ${a.residentCount} người`}
                {a.residents?.length ? `: ${a.residents.map((r) => r.name).join(', ')}` : ''}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}

function MenuContentSheet({ menu, etag, meals, diets, dishes, editable, onClose, onSaved }) {
  const [rows, setRows] = useState(menu.content?.dishes ?? [])
  const [dietId, setDietId] = useState(diets[0]?.id ?? null)
  const { busy, failure, run } = useCommand()
  const days = weekDays(menu.weekStart)
  const suitable = dishes.filter((d) => d.status === 'ACTIVE' && (d.effective?.suitableDietIds ?? []).includes(dietId))
  const nameOf = (id) => dishes.find((d) => d.id === id)?.name ?? `Món mã ${id}`

  function add(date, mealId, dishId) {
    if (!dishId) return
    setRows((r) => [...r, { date, mealId, dietId, dishId: Number(dishId), substituteDishIds: [] }])
  }
  function remove(row) {
    setRows((r) => r.filter((x) => x !== row))
  }
  function addSubstitute(row, dishId) {
    if (!dishId) return
    setRows((r) =>
      r.map((x) => (x === row ? { ...x, substituteDishIds: [...(x.substituteDishIds ?? []), Number(dishId)] } : x)),
    )
  }

  return (
    <Sheet open onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-3xl">
        <SheetTitle className="type-title">{`Nội dung thực đơn tuần ${formatDate(menu.weekStart)}`}</SheetTitle>
        <SheetDescription className="type-body-sm text-muted-foreground">
          {editable
            ? 'Mỗi ô là món của một bữa trong ngày cho chế độ ăn đang chọn; món thay thế theo thứ tự ưu tiên.'
            : 'Chỉ xem.'}
        </SheetDescription>
        <Field className="mt-4 max-w-sm">
          <FieldLabel htmlFor="menu-diet">Chế độ ăn</FieldLabel>
          <select
            id="menu-diet"
            className={SELECT}
            value={dietId ?? ''}
            onChange={(e) => setDietId(Number(e.target.value))}
          >
            {diets.map((d) => (
              <option key={d.id} value={d.id}>
                {d.name}
              </option>
            ))}
          </select>
        </Field>
        <div className="mt-4 overflow-x-auto">
          <Table aria-label="Món theo ngày và bữa">
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead>Ngày</TableHead>
                {meals.map((m) => (
                  <TableHead key={m.id}>{m.name}</TableHead>
                ))}
              </TableRow>
            </TableHeader>
            <TableBody>
              {days.map((date) => (
                <TableRow key={date}>
                  <TableHead scope="row" className="whitespace-nowrap">
                    {weekdayLabel(date)}
                  </TableHead>
                  {meals.map((m) => (
                    <TableCell key={m.id} className="min-w-44 align-top whitespace-normal">
                      <ul className="flex flex-col gap-2">
                        {cellDishes({ dishes: rows }, date, m.id, dietId).map((row, i) => (
                          <li key={i} className="flex flex-col gap-1">
                            <span className="type-body-sm">{nameOf(row.dishId)}</span>
                            {row.substituteDishIds?.length > 0 && (
                              <span className="type-caption text-muted-foreground">{`Thay thế: ${row.substituteDishIds.map(nameOf).join(', ')}`}</span>
                            )}
                            {editable && (
                              <div className="flex flex-wrap gap-1">
                                <select
                                  aria-label={`Thêm món thay thế cho ${nameOf(row.dishId)}`}
                                  className="h-8 rounded-md border border-input bg-transparent px-2 type-caption"
                                  value=""
                                  onChange={(e) => addSubstitute(row, e.target.value)}
                                >
                                  <option value="">+ món thay thế</option>
                                  {suitable
                                    .filter((d) => d.id !== row.dishId)
                                    .map((d) => (
                                      <option key={d.id} value={d.id}>
                                        {d.name}
                                      </option>
                                    ))}
                                </select>
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  aria-label={`Bỏ ${nameOf(row.dishId)}, ${weekdayLabel(date)}, ${m.name}`}
                                  onClick={() => remove(row)}
                                >
                                  Bỏ
                                </Button>
                              </div>
                            )}
                          </li>
                        ))}
                      </ul>
                      {editable && (
                        <select
                          aria-label={`Thêm món ${weekdayLabel(date)}, ${m.name}`}
                          className="mt-2 h-8 w-full rounded-md border border-input bg-transparent px-2 type-caption"
                          value=""
                          onChange={(e) => add(date, m.id, e.target.value)}
                        >
                          <option value="">+ thêm món</option>
                          {suitable.map((d) => (
                            <option key={d.id} value={d.id}>
                              {d.name}
                            </option>
                          ))}
                        </select>
                      )}
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
        <div className="mt-4 flex flex-col gap-3">
          <ProblemAlert problem={failure} />
          <div className="flex justify-end gap-2">
            <Button variant="ghost" className="h-11" onClick={onClose}>
              Đóng
            </Button>
            {editable && (
              <Button
                className="h-11"
                disabled={busy}
                onClick={async () => {
                  if (
                    await run(putMenusById, {
                      path: { id: menu.id },
                      headers: { 'If-Match': etag },
                      body: { dishes: rows },
                    })
                  )
                    onSaved()
                }}
              >
                Lưu nội dung
              </Button>
            )}
          </div>
        </div>
      </SheetContent>
    </Sheet>
  )
}

function DishSwapForm({ menu, meals, diets, dishes, onClose, onDone }) {
  const [form, setForm] = useState({
    date: menu.weekStart,
    mealId: meals[0]?.id ?? '',
    dietId: diets[0]?.id ?? '',
    oldDishId: '',
    newDishId: '',
    reason: '',
  })
  const [submitted, setSubmitted] = useState(false)
  const [warnings, setWarnings] = useState(null)
  const { busy, failure, run } = useCommand()
  const set = (patch) => setForm((f) => ({ ...f, ...patch }))
  const current = cellDishes(menu.content, form.date, Number(form.mealId), Number(form.dietId))
  const nameOf = (id) => dishes.find((d) => d.id === id)?.name ?? `Món mã ${id}`
  const reasonError = submitted && !form.reason.trim() ? 'Nhập lý do đổi món' : null
  const pickError = submitted && !form.oldDishId && !form.newDishId ? 'Chọn món bỏ hoặc món mới' : null

  async function submit() {
    setSubmitted(true)
    if (reasonError || pickError || !form.reason.trim() || (!form.oldDishId && !form.newDishId)) return
    const body = {
      date: form.date,
      mealId: Number(form.mealId),
      dietId: Number(form.dietId),
      reason: form.reason.trim(),
      ...(form.oldDishId && { oldDishId: Number(form.oldDishId) }),
      ...(form.newDishId && { newDishId: Number(form.newDishId) }),
      confirmedWarnings: warnings ?? [],
    }
    const result = await run(postMenusByIdDishSwaps, { path: { id: menu.id }, body })
    if (result) onDone('Đã đổi món')
  }

  return (
    <Sheet open onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-2xl">
        <SheetTitle className="type-title">Đổi món</SheetTitle>
        <SheetDescription className="type-body-sm text-muted-foreground">
          Thay, thêm hoặc bớt món của một bữa; bữa đã chốt suất thì tạo phát sinh tới bếp (FR-031, FR-032).
        </SheetDescription>
        <FieldGroup className="mt-4">
          <div className="grid gap-3 sm:grid-cols-3">
            <Field>
              <FieldLabel htmlFor="swap-date">Ngày</FieldLabel>
              <select
                id="swap-date"
                className={SELECT}
                value={form.date}
                onChange={(e) => set({ date: e.target.value, oldDishId: '' })}
              >
                {weekDays(menu.weekStart).map((d) => (
                  <option key={d} value={d}>
                    {weekdayLabel(d)}
                  </option>
                ))}
              </select>
            </Field>
            <Field>
              <FieldLabel htmlFor="swap-meal">Bữa</FieldLabel>
              <select
                id="swap-meal"
                className={SELECT}
                value={form.mealId}
                onChange={(e) => set({ mealId: e.target.value, oldDishId: '' })}
              >
                {meals.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field>
              <FieldLabel htmlFor="swap-diet">Chế độ ăn</FieldLabel>
              <select
                id="swap-diet"
                className={SELECT}
                value={form.dietId}
                onChange={(e) => set({ dietId: e.target.value, oldDishId: '' })}
              >
                {diets.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name}
                  </option>
                ))}
              </select>
            </Field>
          </div>
          <Field data-invalid={!!pickError || undefined}>
            <FieldLabel htmlFor="swap-old">Món bỏ</FieldLabel>
            <select
              id="swap-old"
              className={SELECT}
              value={form.oldDishId}
              onChange={(e) => set({ oldDishId: e.target.value })}
            >
              <option value="">Không bỏ món nào</option>
              {current.map((r) => (
                <option key={r.dishId} value={r.dishId}>
                  {nameOf(r.dishId)}
                </option>
              ))}
            </select>
          </Field>
          <Field data-invalid={!!pickError || undefined}>
            <FieldLabel htmlFor="swap-new">Món mới</FieldLabel>
            <select
              id="swap-new"
              className={SELECT}
              value={form.newDishId}
              onChange={(e) => set({ newDishId: e.target.value })}
            >
              <option value="">Không thêm món</option>
              {dishes
                .filter(
                  (d) => d.status === 'ACTIVE' && (d.effective?.suitableDietIds ?? []).includes(Number(form.dietId)),
                )
                .map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name}
                  </option>
                ))}
            </select>
            {pickError && <FieldError>{pickError}</FieldError>}
          </Field>
          <ReasonField
            id="swap-reason"
            value={form.reason}
            onChange={(reason) => set({ reason })}
            error={reasonError}
          />
        </FieldGroup>
        <div className="mt-4 flex flex-col gap-3">
          <ProblemAlert problem={failure} />
          {failure?.errorCode === 'WARNINGS_NOT_CONFIRMED' && (
            <Field orientation="horizontal">
              <Checkbox
                id="swap-warnings"
                checked={warnings !== null}
                onCheckedChange={(c) =>
                  setWarnings(c === true ? (failure.unmetConditions ?? []).map((u) => u.code) : null)
                }
              />
              <FieldLabel htmlFor="swap-warnings">Đã xem các cảnh báo trên</FieldLabel>
            </Field>
          )}
          <div className="flex justify-end gap-2">
            <Button variant="ghost" className="h-11" onClick={onClose}>
              Đóng
            </Button>
            <Button className="h-11" disabled={busy} onClick={submit}>
              Đổi món
            </Button>
          </div>
        </div>
      </SheetContent>
    </Sheet>
  )
}

function MenuDetail({ summary, meals, diets, dishes, kitchen, doctor, onChanged, Title, Description }) {
  const detail = useLoad(() => getMenusById({ path: { id: summary.id } }), [summary.id, summary.version])
  const [sheet, setSheet] = useState(null)
  const [check, setCheck] = useState(null)
  const [seen, setSeen] = useState(false)
  const [processed, setProcessed] = useState(false)
  const [note, setNote] = useState('')
  const [mode, setMode] = useState(null)
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const { busy, failure, setFailure, run } = useCommand()

  const menu = detail.data?.data
  const status = menu?.status ?? summary.status
  const commands = menuCommands({ status }, { kitchen, doctor })
  const nameOf = (list) => (id) => list.find((x) => x.id === id)?.name ?? `mã ${id}`
  const codes = warningCodes(check)
  const needsSeen = codes.length > 0

  async function runCheck() {
    const result = await run(postMenusByIdCheck, { path: { id: summary.id } })
    if (result) {
      setCheck(result.data)
      setSeen(false)
    }
  }

  async function command(cmd) {
    setSubmitted(true)
    const body = {}
    if (cmd === 'cancel' || cmd === 'return') {
      if (!reason.trim()) return
      body.reason = reason.trim()
    }
    if (cmd === 'submit') {
      if (!processed) return
      body.processingCheckConfirmed = true
      if (note.trim()) body.processingNote = note.trim()
    }
    if (cmd === 'submit' || cmd === 'publish') {
      if (needsSeen && !seen) return
      body.confirmedWarnings = seen ? codes : []
    }
    const result = await run(postMenusByIdByCommand, { path: { id: summary.id, command: cmd }, body })
    if (result) {
      setMode(null)
      setReason('')
      setCheck(null)
      onChanged(`${MENU_COMMAND[cmd].replace('…', '')}: xong`)
    } else if (cmd === 'submit' || cmd === 'publish') {
      runCheck()
    }
  }

  function start(cmd) {
    setFailure(null)
    setSubmitted(false)
    setMode(cmd)
    if ((cmd === 'submit' || cmd === 'publish') && !check) runCheck()
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <StatusBadge status={label(MENU_STATUS, status)} />
        <Title className="type-title">{`Thực đơn tuần ${formatDate(summary.weekStart)}`}</Title>
        <Description className="type-body-sm text-muted-foreground">{`Từ ${formatDate(summary.weekStart)} tới ${formatDate(addDays(summary.weekStart, 6))}`}</Description>
      </div>
      <ProblemAlert problem={detail.failure} />
      {!menu ? (
        !detail.failure && <Skeleton className="h-20 w-full" />
      ) : (
        <div className="grid gap-3">
          <Info label="Số món đã xếp">{menu.content?.dishes?.length ?? 0}</Info>
          {menu.dishesToReplace?.length > 0 && (
            <Info label="Món cần thay (không còn Hiệu lực)">{menu.dishesToReplace.map(nameOf(dishes)).join(', ')}</Info>
          )}
          {menu.submittedAt && <Info label="Gửi công bố">{formatDateTime(menu.submittedAt)}</Info>}
          {menu.publishedAt && <Info label="Công bố">{formatDateTime(menu.publishedAt)}</Info>}
          {menu.returnReason && <Info label="Lý do trả lại">{menu.returnReason}</Info>}
          {menu.cancelReason && <Info label="Lý do hủy">{menu.cancelReason}</Info>}
          {menu.confirmedWarnings?.length > 0 && (
            <Info label="Cảnh báo bếp đã xem">{`${menu.confirmedWarnings.length} cảnh báo`}</Info>
          )}
          {menu.dishSwaps?.length > 0 && <Info label="Đã đổi món">{`${menu.dishSwaps.length} lần`}</Info>}
        </div>
      )}
      <div className="flex flex-wrap gap-2">
        <Button variant="outline" className="h-11" disabled={!menu} onClick={() => setSheet('content')}>
          {kitchen && status === 'DRAFT' ? 'Sửa nội dung' : 'Xem nội dung'}
        </Button>
        {(kitchen || doctor) && status !== 'CANCELLED' && (
          <Button variant="outline" className="h-11" disabled={busy} onClick={runCheck}>
            Kiểm tra thực đơn
          </Button>
        )}
        {kitchen && (status === 'PUBLISHED' || status === 'APPLIED') && (
          <Button variant="outline" className="h-11" onClick={() => setSheet('swap')}>
            Đổi món…
          </Button>
        )}
      </div>
      {check && (
        <div className="flex flex-col gap-3 border-t pt-4">
          <h3 className="type-card-title">Kết quả kiểm tra</h3>
          <CheckResult result={check} dishName={nameOf(dishes)} mealName={nameOf(meals)} dietName={nameOf(diets)} />
        </div>
      )}
      <ProblemAlert problem={failure} />
      {mode && (
        <div className="flex flex-col gap-3 border-t pt-4">
          {(mode === 'cancel' || mode === 'return') && (
            <ReasonField
              id="menu-reason"
              value={reason}
              onChange={setReason}
              error={submitted && !reason.trim() ? 'Nhập lý do' : null}
            />
          )}
          {mode === 'submit' && (
            <>
              <Field orientation="horizontal" data-invalid={(submitted && !processed) || undefined}>
                <Checkbox id="menu-processed" checked={processed} onCheckedChange={(c) => setProcessed(c === true)} />
                <FieldLabel htmlFor="menu-processed">Đã kiểm tra khả năng chế biến của bếp</FieldLabel>
              </Field>
              {submitted && !processed && <FieldError>Đánh dấu đã kiểm tra khả năng chế biến trước khi gửi</FieldError>}
              <Field>
                <FieldLabel htmlFor="menu-note">Ghi chú chế biến</FieldLabel>
                <Input id="menu-note" className="h-11" value={note} onChange={(e) => setNote(e.target.value)} />
              </Field>
            </>
          )}
          {(mode === 'submit' || mode === 'publish') && needsSeen && (
            <>
              <Field orientation="horizontal" data-invalid={(submitted && !seen) || undefined}>
                <Checkbox id="menu-seen" checked={seen} onCheckedChange={(c) => setSeen(c === true)} />
                <FieldLabel htmlFor="menu-seen">{`Đã xem ${codes.length} cảnh báo lặp món, dị ứng`}</FieldLabel>
              </Field>
              {submitted && !seen && <FieldError>Xác nhận đã xem cảnh báo trước khi tiếp tục</FieldError>}
            </>
          )}
          <div className="flex justify-end gap-2">
            <Button variant="ghost" className="h-11" onClick={() => setMode(null)}>
              Thôi
            </Button>
            <Button
              variant={mode === 'cancel' ? 'destructive' : 'default'}
              className="h-11"
              disabled={busy}
              onClick={() => command(mode)}
            >
              {MENU_COMMAND[mode].replace('…', '')}
            </Button>
          </div>
        </div>
      )}
      {!mode && commands.length > 0 && (
        <div className="flex flex-wrap justify-end gap-2 border-t pt-4">
          {commands.map((cmd) => (
            <Button
              key={cmd}
              variant={cmd === 'submit' || cmd === 'publish' ? 'default' : 'outline'}
              className="h-11"
              onClick={() => start(cmd)}
            >
              {MENU_COMMAND[cmd]}
            </Button>
          ))}
        </div>
      )}
      {sheet === 'content' && menu && (
        <MenuContentSheet
          menu={menu}
          etag={detail.data.etag}
          meals={meals}
          diets={diets.filter((d) => d.status === 'ACTIVE')}
          dishes={dishes}
          editable={kitchen && status === 'DRAFT'}
          onClose={() => setSheet(null)}
          onSaved={() => {
            setSheet(null)
            detail.reload()
            onChanged('Đã lưu nội dung thực đơn')
          }}
        />
      )}
      {sheet === 'swap' && menu && (
        <DishSwapForm
          menu={menu}
          meals={meals}
          diets={diets.filter((d) => d.status === 'ACTIVE')}
          dishes={dishes}
          onClose={() => setSheet(null)}
          onDone={(message) => {
            setSheet(null)
            detail.reload()
            onChanged(message)
          }}
        />
      )}
    </div>
  )
}

function CreateMenuSheet({ weekStart, onClose, onDone }) {
  const [week, setWeek] = useState(weekStart)
  const [copyFrom, setCopyFrom] = useState('')
  const previous = useLoad(() => getMenus({ query: { week: addDays(week, -7) } }).then((r) => r.data), [week])
  const { busy, failure, run } = useCommand()
  return (
    <Sheet open onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-lg">
        <SheetTitle className="type-title">Tạo thực đơn tuần</SheetTitle>
        <SheetDescription className="type-body-sm text-muted-foreground">
          Thực đơn mới ở Nháp. Sao chép từ thực đơn tuần trước thì món không còn Hiệu lực được đánh dấu để thay.
        </SheetDescription>
        <FieldGroup className="mt-4">
          <Field>
            <FieldLabel htmlFor="menu-week">Tuần bắt đầu (thứ Hai)</FieldLabel>
            <Input
              id="menu-week"
              type="date"
              className="h-11"
              value={week}
              onChange={(e) => e.target.value && setWeek(mondayOf(e.target.value))}
            />
          </Field>
          <Field>
            <FieldLabel htmlFor="menu-copy">Sao chép từ</FieldLabel>
            <select id="menu-copy" className={SELECT} value={copyFrom} onChange={(e) => setCopyFrom(e.target.value)}>
              <option value="">Không sao chép</option>
              {(previous.data ?? [])
                .filter((m) => m.status !== 'CANCELLED')
                .map((m) => (
                  <option key={m.id} value={m.id}>
                    {`Tuần ${formatDate(m.weekStart)}, ${label(MENU_STATUS, m.status)}`}
                  </option>
                ))}
            </select>
          </Field>
        </FieldGroup>
        <div className="mt-4 flex flex-col gap-3">
          <ProblemAlert problem={failure} />
          <div className="flex justify-end gap-2">
            <Button variant="ghost" className="h-11" onClick={onClose}>
              Đóng
            </Button>
            <Button
              className="h-11"
              disabled={busy}
              onClick={async () => {
                const body = { weekStart: week, ...(copyFrom && { copyFromId: Number(copyFrom) }) }
                if (await run(postMenus, { body })) onDone(week)
              }}
            >
              Tạo thực đơn
            </Button>
          </div>
        </div>
      </SheetContent>
    </Sheet>
  )
}

export function MenusTab({ today, meals, diets, dishes, kitchen, doctor, openedKey, open, announce }) {
  const [week, setWeek] = useState(mondayOf(today))
  const [creating, setCreating] = useState(false)
  const menus = useLoad(() => getMenus({ query: { week } }).then((r) => r.data), [week])

  function changed(message) {
    announce(message)
    menus.reload()
  }

  function openMenu(m) {
    open(`menu-${m.id}`, 'Chi tiết thực đơn', (titles) => (
      <MenuDetail
        key={m.id}
        summary={m}
        meals={meals}
        diets={diets}
        dishes={dishes}
        kitchen={kitchen}
        doctor={doctor}
        onChanged={changed}
        {...titles}
      />
    ))
  }

  useEffect(() => {
    const m = menus.data?.find((x) => openedKey === `menu-${x.id}`)
    if (m) openMenu(m)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [menus.data])

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center gap-2">
        <Button variant="outline" className="h-11" onClick={() => setWeek(addDays(week, -7))}>
          Tuần trước
        </Button>
        <span className="type-label tabular-nums">{`Tuần ${formatDate(week)} → ${formatDate(addDays(week, 6))}`}</span>
        <Button variant="outline" className="h-11" onClick={() => setWeek(addDays(week, 7))}>
          Tuần sau
        </Button>
        {kitchen && (
          <Button className="h-11 sm:ml-auto" onClick={() => setCreating(true)}>
            Tạo thực đơn tuần
          </Button>
        )}
      </div>
      {creating && (
        <CreateMenuSheet
          weekStart={week}
          onClose={() => setCreating(false)}
          onDone={(w) => {
            setCreating(false)
            setWeek(w)
            changed('Đã tạo thực đơn Nháp')
          }}
        />
      )}
      <ProblemAlert problem={menus.failure} />
      {menus.data === null ? (
        !menus.failure && <Skeleton className="h-24 w-full" />
      ) : menus.data.length === 0 ? (
        <p className="type-body-sm text-muted-foreground">
          Tuần này chưa có thực đơn. Nhân viên bếp tạo thực đơn tuần để Bác sĩ công bố.
        </p>
      ) : (
        <Card className="py-0">
          <Table aria-label="Thực đơn tuần">
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="pl-4">Tuần</TableHead>
                <TableHead className="pr-4">Trạng thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {menus.data.map((m) => (
                <TableRow key={m.id} className={openedKey === `menu-${m.id}` ? 'bg-accent/40' : undefined}>
                  <TableCell className="pl-4">
                    <button
                      type="button"
                      className="text-left underline-offset-4 hover:underline"
                      aria-pressed={openedKey === `menu-${m.id}`}
                      onClick={() => openMenu(m)}
                    >
                      {`Thực đơn tuần ${formatDate(m.weekStart)}`}
                    </button>
                  </TableCell>
                  <TableCell className="pr-4">
                    <StatusBadge status={label(MENU_STATUS, m.status)} />
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
