import { useRef, useState } from 'react'
import { postCostsItemsManual } from '@/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { uploadFile } from '@/common/files'

// Ngày theo giờ Việt Nam (NFR-11), dạng yyyy-MM-dd cho ô ngày.
const todayIso = () => new Date().toLocaleDateString('sv-SE', { timeZone: 'Asia/Ho_Chi_Minh' })

const empty = {
  residentId: '',
  description: '',
  incurredOn: '',
  quantity: '',
  unit: '',
  unitPrice: '',
  reason: '',
}

const fields = [
  ['residentId', 'Mã người cao tuổi', { type: 'number', min: 1, step: 1 }],
  ['description', 'Mô tả', { type: 'text', maxLength: 2000 }],
  ['incurredOn', 'Ngày phát sinh', { type: 'date' }],
  ['quantity', 'Số lượng', { type: 'number', min: 0.001, step: 0.001 }],
  ['unit', 'Đơn vị', { type: 'text', maxLength: 40 }],
  ['unitPrice', 'Đơn giá (đồng)', { type: 'number', min: 0, step: 1, max: 999999999999999 }],
]

/** FR-021, BR-M11-04: khoản nhập tay chỉ cho mục ngoài danh mục; lý do bắt buộc; Quản lý viện duyệt từng khoản. */
export function ManualCostForm({ today = todayIso(), onCreated = () => {} }) {
  const [values, setValues] = useState(empty)
  const [evidence, setEvidence] = useState(null)
  const uploaded = useRef(new Map())
  const [failure, setFailure] = useState(null)
  const [pending, setPending] = useState(false)
  const [done, setDone] = useState(false)
  const busy = useRef(false)
  const nonce = useRef(null)
  function update(key, value) {
    setValues((v) => ({ ...v, [key]: value }))
    nonce.current = null
  }
  async function submit(event) {
    event.preventDefault()
    if (busy.current || !values.reason.trim() || !values.description.trim()) return
    busy.current = true
    setPending(true)
    setFailure(null)
    nonce.current ??= crypto.randomUUID()
    try {
      if (evidence && !uploaded.current.has(evidence)) uploaded.current.set(evidence, await uploadFile(evidence))
      await postCostsItemsManual({
        headers: { 'Idempotency-Key': nonce.current },
        body: {
          residentId: Number(values.residentId),
          description: values.description.trim(),
          incurredOn: values.incurredOn,
          quantity: Number(values.quantity),
          unit: values.unit.trim(),
          unitPrice: Number(values.unitPrice),
          reason: values.reason.trim(),
          ...(evidence ? { fileIds: [uploaded.current.get(evidence)] } : {}),
        },
      })
      setDone(true)
      onCreated()
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      busy.current = false
      setPending(false)
    }
  }
  if (done)
    return (
      <p role="status" className="type-body-sm">
        Đã lập khoản nhập tay nháp. Khoản cần được kiểm tra và Quản lý viện duyệt từng khoản.
      </p>
    )
  return (
    <form onSubmit={submit} className="flex max-w-xl flex-col gap-3 type-body-sm">
      <p>
        Chỉ lập cho mục không có trong danh mục dịch vụ, vật phẩm của viện, kể cả mục đã ngừng hiệu lực. Mục có trong
        danh mục thì lập theo bản ghi nguồn hoặc đề nghị mua hộ.
      </p>
      {fields.map(([key, label, attributes]) => (
        <label key={key}>
          {label}
          <Input
            {...attributes}
            {...(key === 'incurredOn' ? { max: today } : {})}
            required
            disabled={pending}
            value={values[key]}
            onChange={(e) => update(key, e.target.value)}
          />
        </label>
      ))}
      <label>
        Lý do
        <Textarea
          required
          maxLength={2000}
          disabled={pending}
          value={values.reason}
          onChange={(e) => update('reason', e.target.value)}
        />
      </label>
      <label>
        Bằng chứng (nếu có)
        <Input
          type="file"
          disabled={pending}
          onChange={(e) => {
            setEvidence(e.target.files?.[0] ?? null)
            nonce.current = null
          }}
        />
      </label>
      {evidence && <p>Đã chọn: {evidence.name}</p>}
      <ProblemAlert problem={failure} />
      <Button type="submit" disabled={pending || !values.reason.trim() || !values.description.trim()}>
        Lập khoản nhập tay
      </Button>
    </form>
  )
}
