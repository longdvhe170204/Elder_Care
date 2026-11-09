import { useState } from 'react'
import { getResidents } from '@/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { ProblemAlert } from '@/common/ProblemAlert'
import { ResidentAvatar } from '@/common/ResidentAvatar'
import { problemOf } from '@/common/problem'

// Ô tìm người cao tuổi theo họ tên hoặc mã hồ sơ (GET /residents của api-001, trong phạm vi quyền của người dùng);
// chọn một người thì gọi onPick({ residentId, fullName }).
export function ResidentPicker({ onPick }) {
  const [q, setQ] = useState('')
  const [state, setState] = useState({ status: 'idle', items: [] })

  async function search(event) {
    event.preventDefault()
    setState({ status: 'loading', items: [] })
    try {
      const { data } = await getResidents({ query: { q: q.trim() || undefined } })
      setState({ status: 'ready', items: data.items })
    } catch (error) {
      setState({ status: 'error', items: [], problem: problemOf(error) })
    }
  }

  return (
    <div className="flex flex-col gap-3">
      <form role="search" className="flex flex-wrap items-center gap-2" onSubmit={search}>
        <Input
          aria-label="Tìm người cao tuổi"
          placeholder="Họ tên hoặc mã hồ sơ"
          className="h-11 w-72"
          value={q}
          onChange={(event) => setQ(event.target.value)}
        />
        <Button type="submit" variant="outline" className="h-11">
          Tìm
        </Button>
      </form>
      {state.status === 'error' && <ProblemAlert problem={state.problem} />}
      {state.status === 'ready' && state.items.length === 0 && (
        <p className="type-body-sm text-muted-foreground">
          Không có người cao tuổi khớp với từ tìm trong phạm vi của bạn.
        </p>
      )}
      {state.items.length > 0 && (
        <ul className="flex flex-wrap gap-2">
          {state.items.map((r) => (
            <li key={r.id}>
              <Button
                variant="outline"
                className="h-11 gap-2"
                aria-label={`${r.personalInfo.fullName}, ${r.profileCode}`}
                onClick={() => {
                  onPick({ residentId: r.id, fullName: r.personalInfo.fullName })
                  setState({ status: 'idle', items: [] })
                  setQ('')
                }}
              >
                <ResidentAvatar name={r.personalInfo.fullName} code={String(r.id)} />
                <span className="type-label">{r.personalInfo.fullName}</span>
                <span className="type-caption text-muted-foreground">{r.profileCode}</span>
              </Button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
