import { useEffect, useState } from 'react'
import { Button } from '@/components/ui/button'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { SheetHeader, SheetTitle, SheetDescription } from '@/components/ui/sheet'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { catalogs, careLevels, actionNames } from './bedCatalog'

const fieldLabels = {
  code: 'Mã',
  name: 'Tên',
  buildingName: 'Tên tòa nhà',
  standardBeds: 'Số giường tiêu chuẩn',
  areaId: 'Khu vực',
  floorId: 'Tầng',
  roomTypeId: 'Loại phòng',
  roomId: 'Phòng',
}
const references = { areaId: 'areas', floorId: 'floors', roomTypeId: 'room-types', roomId: 'rooms' }

// Mỗi lệnh chỉ gửi trường hợp đồng, giữ ETag đã đọc và một Idempotency-Key cho mỗi lần gửi.
export function StructureForm({ target, data, onSaved, onClose }) {
  const { kind, action, id, defaults = {} } = target
  const config = catalogs[kind]
  const [draft, setDraft] = useState({
    code: '',
    name: '',
    servesLongTerm: true,
    servesShortTerm: true,
    medicalSafety: false,
    defaultCareLevels: [],
    ...defaults,
  })
  const [etag, setEtag] = useState(null)
  const [loading, setLoading] = useState(Boolean(id))
  const [busy, setBusy] = useState(false)
  const [reason, setReason] = useState('')
  const [error, setError] = useState(null)
  const command = ['deactivate', 'reactivate', 'remove'].includes(action)

  useEffect(() => {
    if (!id) return
    let current = true
    config
      .detail({ path: { id } })
      .then(({ data: item, etag: version }) => {
        if (!current) return
        setDraft({ ...item, roomId: item.location?.roomId ?? item.roomId })
        setEtag(version)
        setLoading(false)
      })
      .catch((cause) => {
        if (current) {
          setError(problemOf(cause))
          setLoading(false)
        }
      })
    return () => {
      current = false
    }
  }, [config, id])

  async function reload() {
    setLoading(true)
    setError(null)
    try {
      const { data: item, etag: version } = await config.detail({ path: { id } })
      setDraft({ ...item, roomId: item.location?.roomId ?? item.roomId })
      setEtag(version)
    } catch (cause) {
      setError(problemOf(cause))
    } finally {
      setLoading(false)
    }
  }
  function change(key, value) {
    setDraft((previous) => {
      const next = { ...previous, [key]: value }
      if (key === 'roomTypeId' && action === 'create')
        next.allowedCareLevels = data['room-types'].find((item) => item.id === value)?.defaultCareLevels ?? []
      return next
    })
  }
  async function submit(event) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    const headers = { 'Idempotency-Key': crypto.randomUUID(), ...(id && { 'If-Match': etag }) }
    let body
    if (command) body = action === 'remove' ? undefined : { reason }
    else {
      body = Object.fromEntries(config.fields.filter((key) => draft[key] !== undefined).map((key) => [key, draft[key]]))
      if (['areas', 'rooms'].includes(kind) && reason.trim()) body.reason = reason
    }
    try {
      const result = await config[action]({ path: id ? { id } : undefined, headers, body })
      await onSaved(result.data?.warnings ?? [])
    } catch (cause) {
      setError(problemOf(cause))
    } finally {
      setBusy(false)
    }
  }
  function label(key) {
    return ['code', 'name'].includes(key) ? `${fieldLabels[key]} ${config.label}` : fieldLabels[key]
  }
  function levels(key, title) {
    return (
      <fieldset className="flex flex-col gap-2 rounded-xl border p-3">
        <legend className="px-1 font-medium">{title}</legend>
        {careLevels.map((level) => (
          <label key={level.code} className="flex items-center gap-2">
            <input
              type="checkbox"
              checked={(draft[key] ?? []).includes(level.code)}
              onChange={(event) =>
                change(
                  key,
                  event.target.checked
                    ? [...(draft[key] ?? []), level.code]
                    : (draft[key] ?? []).filter((value) => value !== level.code),
                )
              }
            />
            {level.label}
          </label>
        ))}
      </fieldset>
    )
  }
  return (
    <>
      <SheetHeader>
        <SheetTitle>
          {actionNames[action]} {config.label}
        </SheetTitle>
        <SheetDescription>Thay đổi được ghi nhật ký. Dữ liệu có tham chiếu lịch sử được giữ lại.</SheetDescription>
      </SheetHeader>
      <form onSubmit={submit} className="flex flex-col gap-4 overflow-y-auto px-4 pb-6 type-body-sm">
        {loading ? (
          <p aria-busy="true">Đang tải dữ liệu…</p>
        ) : (
          <>
            {command ? (
              <p>
                {actionNames[action]} <strong>{draft.name ?? draft.code}</strong>?
              </p>
            ) : (
              config.fields
                .filter(
                  (key) =>
                    ![
                      'defaultCareLevels',
                      'allowedCareLevels',
                      'servesLongTerm',
                      'servesShortTerm',
                      'medicalSafety',
                      'genderPolicy',
                    ].includes(key),
                )
                .map((key) => (
                  <Field key={key}>
                    <FieldLabel htmlFor={key}>{label(key)}</FieldLabel>
                    {references[key] ? (
                      <select
                        id={key}
                        value={draft[key] ?? ''}
                        required
                        disabled={kind === 'beds' && Boolean(id)}
                        onChange={(event) => change(key, Number(event.target.value))}
                        className="h-10 w-full rounded-md border bg-background px-3"
                      >
                        <option value="">Chọn {label(key).toLowerCase()}</option>
                        {(data[references[key]] ?? [])
                          .filter((item) => item.status === 'ACTIVE' || item.id === draft[key])
                          .map((item) => (
                            <option key={item.id} value={item.id}>
                              {item.code ? item.code + ' — ' : ''}
                              {item.name}
                            </option>
                          ))}
                      </select>
                    ) : (
                      <Input
                        id={key}
                        type={key === 'standardBeds' ? 'number' : 'text'}
                        min={key === 'standardBeds' ? 1 : undefined}
                        max={key === 'standardBeds' ? 32767 : undefined}
                        maxLength={key === 'code' ? (kind === 'bed-transfer-reasons' ? 30 : 20) : 100}
                        required={key !== 'buildingName'}
                        value={draft[key] ?? ''}
                        onChange={(event) =>
                          change(key, key === 'standardBeds' ? Number(event.target.value) : event.target.value)
                        }
                      />
                    )}
                  </Field>
                ))
            )}
            {!command && kind === 'room-types' && (
              <>
                {levels('defaultCareLevels', 'Mức chăm sóc mặc định')}
                <label className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    checked={draft.servesLongTerm ?? true}
                    onChange={(event) => change('servesLongTerm', event.target.checked)}
                  />
                  Phục vụ nội trú dài hạn
                </label>
                <label className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    checked={draft.servesShortTerm ?? true}
                    onChange={(event) => change('servesShortTerm', event.target.checked)}
                  />
                  Phục vụ nội trú ngắn ngày
                </label>
              </>
            )}
            {!command && kind === 'rooms' && (
              <>
                <Field>
                  <FieldLabel htmlFor="genderPolicy">Chính sách giới tính</FieldLabel>
                  <select
                    id="genderPolicy"
                    value={draft.genderPolicy ?? ''}
                    required
                    onChange={(event) => change('genderPolicy', event.target.value)}
                    className="h-10 rounded-md border bg-background px-3"
                  >
                    <option value="">Chọn chính sách</option>
                    <option value="MALE">Nam</option>
                    <option value="FEMALE">Nữ</option>
                    <option value="UNRESTRICTED">Không giới hạn</option>
                  </select>
                </Field>
                {levels('allowedCareLevels', 'Mức chăm sóc được phép')}
              </>
            )}
            {!command && kind === 'bed-transfer-reasons' && (
              <label className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={draft.medicalSafety ?? false}
                  disabled={draft.fixed}
                  onChange={(event) => change('medicalSafety', event.target.checked)}
                />
                Y tế, an toàn
              </label>
            )}
            {((command && action !== 'remove') || (!command && ['areas', 'rooms'].includes(kind))) && (
              <Field>
                <FieldLabel htmlFor="reason">Lý do{!command && ' (không bắt buộc)'}</FieldLabel>
                <textarea
                  id="reason"
                  required={command}
                  maxLength={2000}
                  value={reason}
                  onChange={(event) => setReason(event.target.value)}
                  className="min-h-24 rounded-md border bg-background p-3"
                />
              </Field>
            )}
          </>
        )}
        <ProblemAlert problem={error}>
          {error?.status === 412 && (
            <>
              <p>
                Giá trị hiện hành: <strong>{error.currentValue?.name ?? error.currentValue?.code}</strong>
              </p>
              <Button type="button" variant="outline" onClick={reload}>
                Tải lại dữ liệu
              </Button>
            </>
          )}
        </ProblemAlert>
        <div className="flex justify-end gap-2 border-t pt-4">
          <Button type="button" variant="outline" onClick={onClose}>
            Đóng
          </Button>
          <Button type="submit" disabled={loading || busy || Boolean(id && !etag)}>
            {busy ? 'Đang lưu…' : command ? 'Xác nhận' : 'Lưu'}
          </Button>
        </div>
      </form>
    </>
  )
}
