import { useState } from 'react'
import { postActivitySessionsByIdAttendance } from '@/api'
import { Button } from '@/components/ui/button'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { ActivityField } from './ActivityField'
import {
  ABSENCE_REASON,
  ATTENDANCE_RESULT,
  COMMUNICATION_LEVEL,
  PARTICIPATION_LEVEL,
  attendanceBody,
} from './sessionView'

// 014 T014 (FR-027 → FR-029; bố cục duyệt 2026-11-05): điểm danh các đăng ký chưa có kết quả, thêm người lúc điểm danh.
// Kết quả đã ghi không sửa ở đây (nhóm 3, đính chính theo 000).
function Choice({ legend, options, value, onChange }) {
  return (
    <fieldset aria-label={legend} className="flex flex-wrap gap-2">
      {Object.entries(options).map(([code, label]) => (
        <label
          key={code}
          className="flex min-h-11 items-center gap-2 rounded-full border border-input px-3 has-checked:border-primary"
        >
          <input
            type="radio"
            className="size-4 accent-primary"
            name={legend}
            checked={value === code}
            onChange={() => onChange(code)}
          />
          <span>{label}</span>
        </label>
      ))}
    </fieldset>
  )
}

function Row({ name, entry, onChange }) {
  const set = (field) => (value) => onChange({ ...entry, [field]: value })
  return (
    <li className="flex flex-col gap-2 border-b py-3 last:border-b-0">
      <span className="type-label">{name}</span>
      <Choice
        legend={`Kết quả của ${name}`}
        options={ATTENDANCE_RESULT}
        value={entry.result}
        onChange={set('result')}
      />
      {entry.result === 'PRESENT' && (
        <>
          <Choice
            legend={`Mức tham gia của ${name}`}
            options={PARTICIPATION_LEVEL}
            value={entry.participationLevel}
            onChange={set('participationLevel')}
          />
          <Choice
            legend={`Mức giao tiếp của ${name}`}
            options={COMMUNICATION_LEVEL}
            value={entry.communicationLevel}
            onChange={set('communicationLevel')}
          />
          <ActivityField
            label={`Tình trạng sau của ${name}`}
            value={entry.conditionAfter}
            onChange={set('conditionAfter')}
          />
          <ActivityField
            label={`Ghi chú của ${name}`}
            type="textarea"
            maxLength={2000}
            value={entry.note}
            onChange={set('note')}
          />
        </>
      )}
      {entry.result === 'ABSENT' && (
        <Choice
          legend={`Lý do vắng của ${name}`}
          options={ABSENCE_REASON}
          value={entry.absenceReason}
          onChange={set('absenceReason')}
        />
      )}
    </li>
  )
}

export function SessionAttendanceForm({ sessionId, registrations, residents, nameOf, onDone }) {
  const pending = registrations.filter((r) => r.status === 'REGISTERED')
  const [entries, setEntries] = useState({})
  const [walkIns, setWalkIns] = useState([])
  const [candidate, setCandidate] = useState('')
  const [problem, setProblem] = useState(null)
  const [busy, setBusy] = useState(false)
  const [key, setKey] = useState(() => crypto.randomUUID())
  const taken = new Set([...registrations.map((r) => r.residentId), ...walkIns])
  const options = residents.filter((r) => !taken.has(r.id)).map((r) => [String(r.id), r.fullName])
  const rows = [
    ...pending.map((r) => ({ residentId: r.residentId, addedAtAttendance: false })),
    ...walkIns.map((id) => ({ residentId: id, addedAtAttendance: true })),
  ]
  const body = attendanceBody(rows, entries)

  async function save() {
    setBusy(true)
    setProblem(null)
    try {
      await postActivitySessionsByIdAttendance({ path: { id: sessionId }, body, headers: { 'Idempotency-Key': key } })
      setKey(crypto.randomUUID())
      setEntries({})
      setWalkIns([])
      onDone('Đã lưu điểm danh')
    } catch (error) {
      setProblem(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="flex flex-col gap-3 border-t pt-4" aria-labelledby="session-attendance">
      <h3 id="session-attendance" className="type-card-title">
        Điểm danh
      </h3>
      {rows.length === 0 ? (
        <p className="type-body-sm text-muted-foreground">Mọi đăng ký đã có kết quả điểm danh.</p>
      ) : (
        <ul className="flex flex-col">
          {rows.map((row) => (
            <Row
              key={row.residentId}
              name={nameOf(row.residentId)}
              entry={entries[row.residentId] ?? {}}
              onChange={(entry) => setEntries((all) => ({ ...all, [row.residentId]: entry }))}
            />
          ))}
        </ul>
      )}
      <div className="flex flex-wrap items-end gap-2">
        <ActivityField label="Người thêm lúc điểm danh" options={options} value={candidate} onChange={setCandidate} />
        <Button
          variant="outline"
          size="lg"
          disabled={!candidate}
          onClick={() => {
            setWalkIns((list) => [...list, Number(candidate)])
            setCandidate('')
          }}
        >
          Thêm vào điểm danh
        </Button>
      </div>
      <ProblemAlert problem={problem} />
      <Button size="lg" disabled={busy || !body} onClick={save}>
        Lưu điểm danh
      </Button>
    </section>
  )
}
