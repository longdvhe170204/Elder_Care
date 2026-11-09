import { useCallback, useEffect, useState } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { ProblemAlert } from '@/common/ProblemAlert'
import { ResidentAvatar } from '@/common/ResidentAvatar'
import { problemOf } from '@/common/problem'
import { getResidents } from '@/api/api001'

// Chọn người cao tuổi (tìm qua GET /residents của 001, trong phạm vi của người dùng) cho màn Thuốc khi cần, Đơn thuốc:
// api-006 liệt kê đơn theo từng người cao tuổi nên màn phải chọn người trước.
export function ResidentPicker({ onPick, selectedId }) {
  const [q, setQ] = useState('')
  const [state, setState] = useState({ status: 'loading', items: [] })

  const load = useCallback(async (text) => {
    setState({ status: 'loading', items: [] })
    try {
      const { data } = await getResidents({ query: { q: text || undefined } })
      setState({ status: 'ready', items: data.items })
    } catch (error) {
      setState({ status: 'error', items: [], problem: problemOf(error) })
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải danh sách khi mở màn
    load('')
  }, [load])

  function search(event) {
    event.preventDefault()
    load(q.trim())
  }

  return (
    <div className="flex flex-col gap-3">
      <form role="search" className="flex flex-wrap items-center gap-2" onSubmit={search}>
        <Input
          aria-label="Tìm người cao tuổi"
          placeholder="Họ tên hoặc mã hồ sơ"
          className="h-11 w-full sm:w-72"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
        <Button type="submit" variant="outline" className="h-11">
          Tìm
        </Button>
      </form>
      {state.status === 'error' && <ProblemAlert problem={state.problem} />}
      {state.status === 'ready' && state.items.length === 0 && (
        <p className="type-body-sm text-muted-foreground">Không có người cao tuổi khớp với từ tìm trong phạm vi của bạn.</p>
      )}
      {state.items.length > 0 && (
        <ul className="flex flex-wrap gap-2">
          {state.items.map((r) => (
            <li key={r.id}>
              <Button
                variant={r.id === selectedId ? 'default' : 'outline'}
                className="h-11 gap-2"
                aria-label={`${r.personalInfo.fullName}, ${r.profileCode}`}
                aria-pressed={r.id === selectedId}
                onClick={() => onPick(r)}
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
