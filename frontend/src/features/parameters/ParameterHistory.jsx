import { useCallback, useEffect, useState } from 'react'
import { getParametersByCodeHistory } from '@/api'
import { Field, FieldDescription, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { formatDateTime } from '@/common/dateTime'
import { formatValue } from './parameterValue'

// Lịch sử giá trị một tham số (000 T081; FR-019; hợp đồng GET /parameters/{code}/history). Chọn ngày thì hiện
// giá trị đang hiệu lực cuối ngày đó (ngày lịch Việt Nam).

function actorLabel(actor) {
  if (actor.type === 'SCHEDULER') return 'Nạp ban đầu'
  return actor.fullName ?? `Tài khoản ${actor.id}`
}

export function ParameterHistory({ parameter }) {
  const [asOf, setAsOf] = useState('')
  const [state, setState] = useState({ status: 'loading', items: [] })

  const load = useCallback(
    async (date) => {
      setState({ status: 'loading', items: [] })
      try {
        const query = date ? { asOf: `${date}T23:59:59.999+07:00` } : undefined
        const { data } = await getParametersByCodeHistory({ path: { code: parameter.code }, query })
        setState({ status: 'ready', items: data })
      } catch (error) {
        setState({ status: 'error', items: [], problem: problemOf(error) })
      }
    },
    [parameter.code],
  )

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải lịch sử khi mở tab
    load('')
  }, [load])

  function changeDate(value) {
    setAsOf(value)
    load(value)
  }

  return (
    <div className="flex flex-col gap-4">
      <Field>
        <FieldLabel htmlFor="parameter-as-of">Giá trị tại ngày</FieldLabel>
        <Input
          id="parameter-as-of"
          type="date"
          value={asOf}
          onChange={(e) => changeDate(e.target.value)}
          className="h-10 max-w-48"
        />
        <FieldDescription>Bỏ trống để xem toàn bộ lịch sử.</FieldDescription>
      </Field>

      {state.status === 'loading' && (
        <div aria-busy="true" className="flex flex-col gap-2">
          <span className="sr-only">Đang tải lịch sử</span>
          <Skeleton className="h-12" />
          <Skeleton className="h-12" />
        </div>
      )}
      {state.status === 'error' && <ProblemAlert problem={state.problem} />}
      {state.status === 'ready' && state.items.length === 0 && <p>Không có giá trị nào hiệu lực tại ngày này.</p>}
      {state.status === 'ready' && state.items.length > 0 && (
        <ul aria-label="Lịch sử giá trị" className="flex flex-col gap-3 border-l-2 pl-3">
          {state.items.map((item) => (
            <li key={item.effectiveFrom} className="flex flex-col gap-1">
              <span className="font-medium">
                {formatValue(parameter.type, item.value, parameter.unit)}
                {item.isDefaultRestore && <span className="text-muted-foreground"> (khôi phục mặc định)</span>}
              </span>
              <span className="type-caption text-muted-foreground tabular-nums">
                Từ {formatDateTime(item.effectiveFrom)}
                {item.effectiveTo ? ` đến ${formatDateTime(item.effectiveTo)}` : ', đang hiệu lực'};{' '}
                {actorLabel(item.actor)}
              </span>
              {item.reason && <span className="type-body-sm">{item.reason}</span>}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
