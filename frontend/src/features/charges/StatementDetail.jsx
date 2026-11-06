import { useEffect, useRef, useState } from 'react'
import * as api from '@/api'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Skeleton } from '@/components/ui/skeleton'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { ChargeConfirmation } from './ChargeConfirmation'
import { CostItemSheet } from './CostItemSheet'
import { controlClass, COST_TYPE, ORIGIN, FLAGS, ITEM_STATUS, STATEMENT_STATUS, excess, money } from './chargeLabels'

export function StatementDetail({ id, Title, Description, onChanged, onDenied }) {
  const { can } = usePermissions()
  const update = can('COST_ADJUSTMENT', 'UPDATE')
  const approve = can('COST_ADJUSTMENT', 'APPROVE')
  const [statement, setStatement] = useState(null)
  const [etag, setEtag] = useState(null)
  const [revision, setRevision] = useState(0)
  const [selected, setSelected] = useState([])
  const [filter, setFilter] = useState({})
  const [itemId, setItemId] = useState(null)
  const [intent, setIntent] = useState(null)
  const [pending, setPending] = useState(false)
  const [stale, setStale] = useState(false)
  const [problem, setProblem] = useState(null)
  const [invalidReason, setInvalidReason] = useState(false)
  const [result, setResult] = useState(null)
  const [warning, setWarning] = useState(null)
  const busy = useRef(false)

  useEffect(() => {
    let active = true
    api
      .getCostsStatementsById({ path: { id } })
      .then((response) => {
        if (!active) return
        setStatement(response.data)
        setEtag(response.etag)
        setStale(false)
        setProblem(null)
        setIntent((current) => current && { ...current, nonce: crypto.randomUUID() })
      })
      .catch((error) => {
        if (active) {
          if (error.status === 403) onDenied()
          else setProblem(problemOf(error))
        }
      })
    return () => {
      active = false
    }
  }, [id, revision, onDenied])

  function open(operation, ids = [], item = null, byFilter = false) {
    setItemId(null)
    setProblem(null)
    setInvalidReason(false)
    const one = item ?? statement.items?.find((row) => ids.length === 1 && row.id === ids[0])
    setIntent({
      operation,
      ids,
      filter: byFilter ? { statementId: id, ...filter } : undefined,
      reason: '',
      nonce: crypto.randomUUID(),
      excess: operation === 'APPROVE' && ids.length === 1 && excess(one ?? {}),
    })
  }

  async function confirm(required) {
    if (busy.current || stale) return
    if (required && !intent.reason.trim()) {
      setInvalidReason(true)
      return
    }
    busy.current = true
    setPending(true)
    setProblem(null)
    const headers = { 'Idempotency-Key': intent.nonce }
    const path = { id }
    const selection = intent.filter ? { filter: intent.filter } : { itemIds: intent.ids }
    let command
    let params = { path, headers }
    switch (intent.operation) {
      case 'CHECK':
        command = api.postCostsItemsCheck
        params.body = selection
        break
      case 'APPROVE':
        command = api.postCostsItemsApprove
        params.body = { ...selection, ...(intent.reason.trim() && { excessApprovalReason: intent.reason.trim() }) }
        break
      case 'SUBMIT':
        command = api.postCostsStatementsByIdSubmitForFinalization
        break
      case 'FINALIZE':
        command = api.postCostsStatementsByIdFinalize
        headers['If-Match'] = etag
        break
      case 'RETURN_STATEMENT':
        command = api.postCostsStatementsByIdReturn
        params.body = { reason: intent.reason.trim(), itemIds: intent.ids }
        break
      default:
        command = {
          RETURN: api.postCostsItemsByIdReturn,
          UNCHECK: api.postCostsItemsByIdUncheck,
          CANCEL: api.postCostsItemsByIdCancel,
        }[intent.operation]
        params = { path: { id: intent.ids[0] }, headers, body: { reason: intent.reason.trim() } }
    }
    try {
      const { data } = await command(params)
      setResult(data?.succeededIds ? data : null)
      setWarning(data?.changeWarning ?? null)
      setSelected([])
      setIntent(null)
      setRevision((value) => value + 1)
      onChanged('Đã thực hiện lệnh và tải lại bảng chi phí.')
    } catch (error) {
      if (error.status === 403) onDenied()
      else {
        setProblem(problemOf(error))
        if (error.status === 412) setStale(true)
      }
    } finally {
      busy.current = false
      setPending(false)
    }
  }

  if (!statement)
    return (
      <>
        <Title>Bảng #{id}</Title>
        <Description>Đang tải bảng chi phí.</Description>
        <ProblemAlert problem={problem} />
        {problem ? (
          <Button onClick={() => setRevision((n) => n + 1)}>Thử lại</Button>
        ) : (
          <Skeleton className="h-32 w-full" />
        )}
      </>
    )
  const editable = statement.status !== 'FINALIZED'
  const items = (statement.items ?? []).filter(
    (item) =>
      (!filter.costType || item.costType === filter.costType) &&
      (!filter.origin || item.origin === filter.origin) &&
      (!filter.flags || item.flags?.includes(filter.flags)),
  )
  return (
    <div className="flex flex-col gap-4">
      <Title className="type-title">Bảng #{id}</Title>
      <Description className="type-body-sm text-muted-foreground">
        Kỳ {statement.period}, người cao tuổi mã {statement.residentId}.
      </Description>
      <StatusBadge status={STATEMENT_STATUS[statement.status]} />
      <p className="type-card-title tabular-nums">Tổng: {money(statement.total)}</p>
      {(statement.totalsByType ?? []).map((group) => (
        <p key={group.type} className="type-body-sm">
          {COST_TYPE[group.type]}: {money(group.total)}
        </p>
      ))}
      <ProblemAlert problem={!intent ? problem : null} />
      {result && (
        <div role="status" className="rounded-md border p-3 type-body-sm">
          <p>Đã xử lý {result.succeededIds.length} khoản.</p>
          {result.skipped?.map((skip) => (
            <p key={skip.itemId}>
              Khoản #{skip.itemId}: {skip.reason}
            </p>
          ))}
        </div>
      )}
      {warning && (
        <div role="status" className="rounded-md border p-3 type-body-sm">
          <p>
            Chi phí tăng so với kỳ trước
            {warning.increaseRatio == null ? ' từ tổng bằng 0 đồng' : `: ${Number(warning.increaseRatio)}%`}.
          </p>
          {warning.topThreeIncreases?.map((type) => (
            <p key={type}>{COST_TYPE[type]}</p>
          ))}
        </div>
      )}
      <div className="flex flex-col gap-2">
        {Object.entries({
          costType: ['Loại khoản', COST_TYPE],
          origin: ['Nguồn khoản', ORIGIN],
          flags: ['Cờ cảnh báo', FLAGS],
        }).map(([key, [label, values]]) => (
          <label key={key} className="flex flex-col gap-1 type-body-sm">
            {label}
            <select
              className={controlClass}
              value={filter[key] ?? ''}
              onChange={(event) => {
                setSelected([])
                setFilter((current) => ({
                  ...current,
                  [key]: event.target.value || undefined,
                }))
              }}
            >
              <option value="">Tất cả</option>
              {Object.entries(values).map(([value, text]) => (
                <option key={value} value={value}>
                  {text}
                </option>
              ))}
            </select>
          </label>
        ))}
      </div>
      <ul className="flex flex-col divide-y">
        {items.map((item) => (
          <li key={item.id} className="flex gap-3 py-3">
            {editable && (update || approve) && (
              <Checkbox
                aria-label={`Chọn khoản #${item.id}`}
                checked={selected.includes(item.id)}
                disabled={pending || stale}
                onCheckedChange={(checked) =>
                  setSelected((current) =>
                    checked ? [...current, item.id] : current.filter((value) => value !== item.id),
                  )
                }
              />
            )}
            <div className="min-w-0 flex-1">
              <Button variant="link" aria-label={`Xem khoản #${item.id}`} onClick={() => setItemId(item.id)}>
                Khoản #{item.id}
              </Button>
              <p className="type-body-sm">{item.description}</p>
              <p className="type-body-sm tabular-nums">
                {item.quantity} {item.unit} × {item.unitPrice == null ? 'Chưa có đơn giá' : money(item.unitPrice)} ·{' '}
                {money(item.amount)}
              </p>
              <StatusBadge status={ITEM_STATUS[item.status]} />
              {item.flags?.map((flag) => (
                <p key={flag} className="type-body-sm text-muted-foreground">
                  {FLAGS[flag]}
                </p>
              ))}
            </div>
          </li>
        ))}
      </ul>
      {items.length === 0 && <p className="type-body-sm">Không có khoản khớp bộ lọc.</p>}
      {editable && (
        <div className="flex flex-wrap gap-2">
          {update && (
            <>
              <Button disabled={!selected.length || pending || stale} onClick={() => open('CHECK', selected)}>
                Kiểm tra các khoản đã chọn
              </Button>
              <Button variant="outline" disabled={pending || stale} onClick={() => open('CHECK', [], null, true)}>
                Kiểm tra theo bộ lọc
              </Button>
            </>
          )}
          {approve && (
            <>
              <Button disabled={!selected.length || pending || stale} onClick={() => open('APPROVE', selected)}>
                Duyệt các khoản đã chọn
              </Button>
              <Button variant="outline" disabled={pending || stale} onClick={() => open('APPROVE', [], null, true)}>
                Duyệt theo bộ lọc
              </Button>
            </>
          )}
          {statement.status === 'OPEN' && can('PERIOD_CLOSING', 'UPDATE') && (
            <Button variant="outline" disabled={pending || stale} onClick={() => open('SUBMIT')}>
              Gửi chốt
            </Button>
          )}
          {statement.status === 'AWAITING_FINALIZATION' && can('PERIOD_CLOSING', 'FINALIZE') && (
            <Button disabled={pending || stale || !etag} onClick={() => open('FINALIZE')}>
              Chốt bảng
            </Button>
          )}
          {statement.status === 'AWAITING_FINALIZATION' && can('PERIOD_CLOSING', 'APPROVE') && (
            <Button variant="outline" disabled={pending || stale} onClick={() => open('RETURN_STATEMENT', selected)}>
              Trả lại bảng
            </Button>
          )}
        </div>
      )}
      {itemId !== null && (
        <CostItemSheet
          key={itemId}
          id={itemId}
          onClose={() => setItemId(null)}
          onAction={(operation, item) => open(operation, [item.id], item)}
          canUpdate={update}
          canApprove={approve}
          statementStatus={statement.status}
          onDenied={onDenied}
        />
      )}
      {intent && (
        <ChargeConfirmation
          intent={intent}
          statement={statement}
          pending={pending}
          stale={stale}
          invalidReason={invalidReason}
          problem={problem}
          onReason={(reason) => {
            setIntent((current) => ({ ...current, reason, nonce: crypto.randomUUID() }))
            setInvalidReason(false)
          }}
          onConfirm={confirm}
          onReload={() => setRevision((n) => n + 1)}
          onClose={() => setIntent(null)}
        />
      )}
    </div>
  )
}
