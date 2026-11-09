import { useCallback, useEffect, useRef, useState } from 'react'
import * as api from '@/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Card } from '@/components/ui/card'
import { DetailLayout } from '@/common/DetailLayout'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { formatDateTime } from '@/common/dateTime'
import { uploadFile } from '@/common/files'
import { money, controlClass } from './chargeLabels'

const states = {
  DRAFT: 'Nháp',
  AWAITING_CONSENT: 'Chờ đồng ý',
  PURCHASE_ALLOWED: 'Được phép mua',
  REJECTED: 'Từ chối',
  CANCELLED: 'Hủy',
  PURCHASED: 'Đã mua',
}
const commands = {
  send: ['Gửi đề nghị', api.postPurchaseAssistanceRequestsByIdSend],
  agree: ['Đồng ý', api.postPurchaseAssistanceRequestsByIdAgree],
  reject: ['Từ chối', api.postPurchaseAssistanceRequestsByIdReject],
  additional: ['Xin đồng ý bổ sung', api.postPurchaseAssistanceRequestsByIdRequestAdditionalConsent],
  withdraw: ['Rút yêu cầu bổ sung', api.postPurchaseAssistanceRequestsByIdWithdrawAdditionalConsent],
  cancel: ['Hủy đề nghị', api.postPurchaseAssistanceRequestsByIdCancel],
  purchased: ['Ghi đã mua', api.postPurchaseAssistanceRequestsByIdRecordPurchased],
}

export function PurchaseRequests({ residentId, familyMode = false }) {
  const { can, status: permissionStatus } = usePermissions()
  const [rows, setRows] = useState(null)
  const [opened, setOpened] = useState(null)
  const [failure, setFailure] = useState(null)
  const [denied, setDenied] = useState(false)
  const [revision, setRevision] = useState(0)
  const [creating, setCreating] = useState(false)
  const [announcement, setAnnouncement] = useState('')
  const [filter, setFilter] = useState('')
  const canView = can('PURCHASE_ON_BEHALF', 'VIEW')
  const deny = useCallback(() => {
    setRows(null)
    setOpened(null)
    setCreating(false)
    setDenied(true)
  }, [])
  useEffect(() => {
    if (!canView || permissionStatus !== 'ready') return
    let active = true
    api
      .getPurchaseAssistanceRequests({
        query: { ...(residentId ? { residentId } : {}), ...(filter ? { status: filter } : {}) },
      })
      .then(({ data }) => {
        if (active) {
          setRows(data)
          setFailure(null)
        }
      })
      .catch((error) => {
        if (active) {
          if (error.status === 403) deny()
          else setFailure(problemOf(error))
        }
      })
    return () => {
      active = false
    }
  }, [canView, permissionStatus, residentId, filter, revision, deny])
  function changed(conflict = false) {
    setOpened(null)
    setCreating(false)
    setRevision((n) => n + 1)
    setAnnouncement(
      conflict
        ? 'Đề nghị đã thay đổi. Hãy xem lại danh sách trước khi thao tác tiếp.'
        : 'Đã ghi nhận. Danh sách được tải lại.',
    )
  }
  if (denied || permissionStatus === 'error' || (permissionStatus === 'ready' && !canView))
    return <PermissionDeniedState label="Đề nghị mua hộ" />
  const selected = rows?.find((row) => row.id === opened)
  return (
    <DetailLayout
      open={!!selected}
      detailKey={opened}
      onClose={() => setOpened(null)}
      label="Chi tiết đề nghị mua hộ"
      renderDetail={({ Title, Description }) => (
        <>
          <Title>Đề nghị #{selected.id}</Title>
          <Description>{states[selected.status]}</Description>
          <PurchaseDetail
            key={selected.id}
            row={selected}
            familyMode={familyMode}
            onChanged={changed}
            onDenied={deny}
          />
        </>
      )}
    >
      <div className="flex flex-wrap items-end gap-3">
        <label className="type-body-sm">
          Trạng thái
          <select
            className={controlClass}
            value={filter}
            onChange={(e) => {
              setFilter(e.target.value)
              setOpened(null)
            }}
          >
            <option value="">Tất cả</option>
            {Object.entries(states).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </label>
        {!familyMode && can('PURCHASE_ON_BEHALF', 'CREATE') && (
          <Button onClick={() => setCreating(!creating)}>Lập đề nghị mua hộ</Button>
        )}
        <Button variant="outline" onClick={() => setRevision((n) => n + 1)}>
          Tải lại
        </Button>
      </div>
      <ProblemAlert problem={failure} />
      <p role="status" className="type-body-sm">
        {announcement}
      </p>
      {creating && <PurchaseCreate residentId={residentId} onChanged={changed} onDenied={deny} />}
      {rows === null ? (
        <p className="type-body-sm">Đang tải đề nghị…</p>
      ) : rows.length === 0 ? (
        <p className="type-body-sm">Không có đề nghị trong phạm vi này.</p>
      ) : (
        <Card className="overflow-x-auto">
          <table className="w-full type-body-sm">
            <thead>
              <tr>
                <th className="p-3 text-left">Đề nghị</th>
                <th className="p-3 text-left">Người cao tuổi</th>
                <th className="p-3 text-left">Số tiền dự kiến</th>
                <th className="p-3 text-left">Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id} className="border-t">
                  <td className="p-3">
                    <Button
                      variant="ghost"
                      aria-label={`Xem đề nghị #${row.id}`}
                      aria-pressed={opened === row.id}
                      onClick={() => setOpened(row.id)}
                    >
                      #{row.id}
                    </Button>
                  </td>
                  <td className="p-3">#{row.residentId}</td>
                  <td className="p-3 tabular-nums">{money(row.estimatedAmount)}</td>
                  <td className="p-3">{states[row.status]}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
    </DetailLayout>
  )
}

function PurchaseDetail({ row, familyMode, onChanged, onDenied }) {
  const { can } = usePermissions()
  const [command, setCommand] = useState(null)
  const [reason, setReason] = useState('')
  const [amount, setAmount] = useState('')
  const [date, setDate] = useState('')
  const [receipt, setReceipt] = useState(null)
  const uploaded = useRef(new Map())
  const [pending, setPending] = useState(false)
  const [failure, setFailure] = useState(null)
  const busy = useRef(false)
  const nonce = useRef(null)
  const canWrite = !familyMode && can('PURCHASE_ON_BEHALF', 'UPDATE')
  const canDecide = can('PURCHASE_ON_BEHALF', familyMode ? 'UPDATE' : 'APPROVE')
  const needsReason = ['reject', 'additional', 'withdraw', 'cancel'].includes(command)
  function choose(value) {
    if (busy.current) return
    setCommand(value)
    setReason('')
    setAmount('')
    setDate('')
    setReceipt('')
    setFailure(null)
    nonce.current = null
  }
  async function submit(e) {
    e.preventDefault()
    if (busy.current || (needsReason && !reason.trim()) || (command === 'purchased' && !receipt)) return
    busy.current = true
    setPending(true)
    setFailure(null)
    nonce.current ??= crypto.randomUUID()
    try {
      if (command === 'purchased' && !uploaded.current.has(receipt))
        uploaded.current.set(receipt, await uploadFile(receipt))
      const body = {
        ...(needsReason ? { reason: reason.trim() } : {}),
        ...(command === 'additional' ? { newAmount: Number(amount) } : {}),
        ...(command === 'purchased'
          ? { actualAmount: Number(amount), purchasedOn: date, receiptFileId: uploaded.current.get(receipt) }
          : {}),
      }
      await commands[command][1]({
        path: { id: row.id },
        headers: { 'Idempotency-Key': nonce.current },
        ...(Object.keys(body).length ? { body } : {}),
      })
      onChanged()
    } catch (error) {
      if (error.status === 403) onDenied()
      else if (error.status === 409) onChanged(true)
      else setFailure(problemOf(error))
    } finally {
      busy.current = false
      setPending(false)
    }
  }
  return (
    <div className="flex flex-col gap-4 type-body-sm">
      <p>{row.description}</p>
      <p>Số lượng: {row.quantity}</p>
      <p>Dự kiến: {money(row.estimatedAmount)}</p>
      {row.allowedAmount != null && <p>Được phép: {money(row.allowedAmount)}</p>}
      {row.additionalConsentPending && (
        <p>
          Số tiền xin đồng ý mới:{' '}
          {money(row.decisions?.findLast((entry) => entry.type === 'REQUEST_ADDITIONAL')?.newAmount)}
        </p>
      )}
      {row.insufficientBalance && <p>Số dư không đủ. Dấu này không chặn quyết định.</p>}
      <p>Lý do: {row.reason}</p>
      <div className="flex flex-wrap gap-2">
        {canWrite && row.status === 'DRAFT' && <Button onClick={() => choose('send')}>Gửi đề nghị</Button>}
        {canDecide && row.status === 'AWAITING_CONSENT' && (
          <>
            <Button onClick={() => choose('agree')}>Đồng ý</Button>
            <Button variant="outline" onClick={() => choose('reject')}>
              Từ chối
            </Button>
          </>
        )}
        {canWrite && row.status === 'PURCHASE_ALLOWED' && (
          <>
            <Button onClick={() => choose('purchased')}>Ghi đã mua</Button>
            <Button variant="outline" onClick={() => choose('additional')}>
              Xin đồng ý bổ sung
            </Button>
          </>
        )}
        {canWrite && row.additionalConsentPending && (
          <Button variant="outline" onClick={() => choose('withdraw')}>
            Rút yêu cầu bổ sung
          </Button>
        )}
        {canWrite && ['DRAFT', 'AWAITING_CONSENT', 'PURCHASE_ALLOWED'].includes(row.status) && (
          <Button variant="outline" onClick={() => choose('cancel')}>
            Hủy đề nghị
          </Button>
        )}
      </div>
      {command && (
        <form onSubmit={submit} className="flex flex-col gap-3">
          <p>
            Xem lại đề nghị #{row.id} trước khi {commands[command][0].toLowerCase()}.
          </p>
          {['additional', 'purchased'].includes(command) && (
            <label>
              Số tiền (đồng, bắt buộc)
              <Input
                type="number"
                min="0"
                max="999999999999999"
                step="1"
                required
                disabled={pending}
                value={amount}
                onChange={(e) => {
                  setAmount(e.target.value)
                  nonce.current = null
                }}
              />
            </label>
          )}
          {command === 'purchased' && (
            <>
              <label>
                Ngày mua (bắt buộc)
                <Input
                  type="date"
                  required
                  disabled={pending}
                  value={date}
                  onChange={(e) => {
                    setDate(e.target.value)
                    nonce.current = null
                  }}
                />
              </label>
              <label>
                Chứng từ (bắt buộc)
                <Input
                  type="file"
                  required
                  disabled={pending}
                  onChange={(e) => {
                    setReceipt(e.target.files?.[0] ?? null)
                    nonce.current = null
                  }}
                />
              </label>
              {receipt && <p>Đã chọn: {receipt.name}</p>}
            </>
          )}
          {needsReason && (
            <label>
              Lý do (bắt buộc)
              <Textarea
                required
                maxLength={2000}
                disabled={pending}
                value={reason}
                onChange={(e) => {
                  setReason(e.target.value)
                  nonce.current = null
                }}
              />
            </label>
          )}
          <ProblemAlert problem={failure} />
          <Button
            type="submit"
            disabled={pending || (needsReason && !reason.trim()) || (command === 'purchased' && !receipt)}
          >
            Xác nhận {commands[command][0].toLowerCase()}
          </Button>
          <Button type="button" variant="outline" disabled={pending} onClick={() => choose(null)}>
            Quay lại
          </Button>
        </form>
      )}
      <h3 className="type-label">Lịch sử quyết định</h3>
      {(row.decisions ?? []).map((entry, index) => (
        <p key={index}>
          {entry.reason || 'Đã ghi quyết định'}, {formatDateTime(entry.occurredAt)}
          {entry.newAmount != null ? `, ${money(entry.newAmount)}` : ''}
        </p>
      ))}
    </div>
  )
}

function PurchaseCreate({ residentId, onChanged, onDenied }) {
  const [values, setValues] = useState({
    residentId: residentId ?? '',
    description: '',
    quantity: '',
    estimatedAmount: '',
    requesterId: '',
    requesterType: 'STAFF',
    reason: '',
  })
  const [pending, setPending] = useState(false)
  const [failure, setFailure] = useState(null)
  const busy = useRef(false)
  const nonce = useRef(null)
  function update(key, value) {
    setValues((v) => ({ ...v, [key]: value }))
    nonce.current = null
  }
  async function submit(event) {
    event.preventDefault()
    if (busy.current) return
    busy.current = true
    setPending(true)
    nonce.current ??= crypto.randomUUID()
    try {
      await api.postPurchaseAssistanceRequests({
        headers: { 'Idempotency-Key': nonce.current },
        body: {
          residentId: Number(values.residentId),
          description: values.description,
          quantity: Number(values.quantity),
          estimatedAmount: Number(values.estimatedAmount),
          reason: values.reason,
          requester: { type: values.requesterType, id: Number(values.requesterId) },
        },
      })
      onChanged()
    } catch (error) {
      if (error.status === 403) onDenied()
      else setFailure(problemOf(error))
    } finally {
      busy.current = false
      setPending(false)
    }
  }
  return (
    <form onSubmit={submit} className="flex max-w-xl flex-col gap-3 type-body-sm">
      <h2 className="type-title">Lập đề nghị mua hộ</h2>
      {[
        ['residentId', 'Mã người cao tuổi', 'number'],
        ['description', 'Mô tả mục cần mua', 'text'],
        ['quantity', 'Số lượng', 'number'],
        ['estimatedAmount', 'Số tiền dự kiến (đồng)', 'number'],
        ['requesterId', 'Mã hồ sơ người yêu cầu', 'number'],
      ].map(([key, label, type]) => (
        <label key={key}>
          {label} (bắt buộc)
          <Input
            type={type}
            required
            min={type === 'number' ? (key === 'estimatedAmount' ? 0 : key === 'quantity' ? 0.001 : 1) : undefined}
            step={key === 'quantity' ? '0.001' : undefined}
            value={values[key]}
            onChange={(e) => update(key, e.target.value)}
            disabled={pending}
          />
        </label>
      ))}
      <label>
        Người yêu cầu
        <select
          className={controlClass}
          value={values.requesterType}
          onChange={(e) => update('requesterType', e.target.value)}
        >
          <option value="STAFF">Nhân viên</option>
          <option value="FAMILY_MEMBER">Người thân</option>
        </select>
      </label>
      <label>
        Lý do (bắt buộc)
        <Textarea
          required
          maxLength={2000}
          value={values.reason}
          onChange={(e) => update('reason', e.target.value)}
          disabled={pending}
        />
      </label>
      <ProblemAlert problem={failure} />
      <Button type="submit" disabled={pending}>
        Lưu đề nghị nháp
      </Button>
    </form>
  )
}
