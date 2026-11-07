import { useState } from 'react'
import {
  getAlertsByIdCall115Info,
  getIncidentsByIdCall115Info,
  postAlertsByIdCall115,
  postCall115AttemptsByIdResult,
  postIncidentsByIdCall115,
} from '@/api'
import { Button } from '@/components/ui/button'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'

// Nút "Gọi 115" trên cảnh báo, sự cố Khẩn cấp (007 T102; FR-047k, FR-048b, BR-M05-31). Màn chỉ hiện khối này khi
// danh sách lệnh được phép của cảnh báo có "call-115" (sự cố: màn sự cố quyết định). Bấm gọi thì ghi lượt gọi trước rồi
// mở trình quay số `tel:115`; sau cuộc gọi người bấm chọn kết quả một lần. Dị ứng, bệnh nền chỉ hiện khi backend trả
// (người có quyền xem sức khỏe); phần bị ẩn ghi rõ.

const SOURCES = {
  alerts: { info: getAlertsByIdCall115Info, press: postAlertsByIdCall115 },
  incidents: { info: getIncidentsByIdCall115Info, press: postIncidentsByIdCall115 },
}
const HIDDEN = { allergies: 'dị ứng', chronicConditions: 'bệnh nền' }
const GENDER = { MALE: 'nam', FEMALE: 'nữ' }

function Line({ label, value }) {
  return (
    <div className="flex flex-col">
      <dt className="type-caption text-muted-foreground">{label}</dt>
      <dd className="type-body-sm">{value || 'Chưa có'}</dd>
    </div>
  )
}

function OperatorInfo({ info }) {
  const resident = info.resident
  const person = resident
    ? [resident.fullName, resident.age != null && `${resident.age} tuổi`, GENDER[resident.gender]]
        .filter(Boolean)
        .join(', ')
    : null
  const place = [info.floor, info.roomOrPlace].filter(Boolean).join(', ')
  const hidden = (info.hiddenFields ?? []).map((f) => HIDDEN[f] ?? f)
  return (
    <dl className="grid gap-3 sm:grid-cols-2">
      <Line label="Viện" value={[info.facility?.name, info.facility?.address].filter(Boolean).join(', ')} />
      <Line label="Điện thoại viện" value={info.facility?.phone} />
      <Line label="Nơi" value={place} />
      <Line label="Người cao tuổi" value={person} />
      {info.allergies && <Line label="Dị ứng" value={info.allergies.join(', ') || 'Chưa ghi nhận'} />}
      {info.chronicConditions && (
        <Line label="Bệnh nền" value={info.chronicConditions.join(', ') || 'Chưa ghi nhận'} />
      )}
      {hidden.length > 0 && (
        <p className="type-body-sm text-muted-foreground sm:col-span-2">
          Bạn không được xem {hidden.join(', ')}; người có quyền xem sức khỏe đọc phần này cho tổng đài.
        </p>
      )}
    </dl>
  )
}

export function Call115Panel({ subject = 'alerts', id, dial = (href) => window.location.assign(href) }) {
  const api = SOURCES[subject]
  const [pressKey] = useState(() => crypto.randomUUID())
  const [info, setInfo] = useState(null)
  const [attempt, setAttempt] = useState(null)
  const [result, setResult] = useState(null)
  const [busy, setBusy] = useState(false)
  const [problem, setProblem] = useState(null)

  async function run(action) {
    setBusy(true)
    setProblem(null)
    try {
      await action()
    } catch (error) {
      setProblem(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  const loadInfo = () => run(async () => setInfo((await api.info({ path: { id } })).data))

  const call = () =>
    run(async () => {
      const { data } = await api.press({ path: { id }, headers: { 'Idempotency-Key': pressKey } })
      setAttempt(data)
      dial('tel:115')
    })

  const record = (value) =>
    run(async () => {
      await postCall115AttemptsByIdResult({ path: { id: attempt.id }, body: { result: value } })
      setResult(value)
    })

  return (
    <section aria-label="Gọi 115" className="flex flex-col gap-3">
      <div className="flex flex-wrap gap-2">
        <Button variant="destructive" className="h-11" onClick={call} disabled={busy || attempt != null}>
          Gọi 115
        </Button>
        {!info && (
          <Button variant="outline" className="h-11" onClick={loadInfo} disabled={busy}>
            Xem thông tin đọc cho tổng đài
          </Button>
        )}
      </div>
      {info && <OperatorInfo info={info} />}
      {attempt && !result && (
        <div className="flex flex-col gap-2">
          <p className="type-body-sm">Sau cuộc gọi, chọn kết quả:</p>
          <div className="flex flex-wrap gap-2">
            <Button className="h-11" onClick={() => record('CONNECTED')} disabled={busy}>
              Đã gọi được
            </Button>
            <Button variant="outline" className="h-11" onClick={() => record('NOT_CONNECTED')} disabled={busy}>
              Không gọi được
            </Button>
          </div>
        </div>
      )}
      {result && (
        <p role="status" className="type-body-sm">
          {result === 'CONNECTED' ? 'Đã ghi: đã gọi được cấp cứu 115.' : 'Đã ghi: không gọi được 115.'}
        </p>
      )}
      <ProblemAlert problem={problem} />
    </section>
  )
}
