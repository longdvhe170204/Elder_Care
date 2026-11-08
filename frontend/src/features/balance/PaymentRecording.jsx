import { useState } from 'react'
import {
  ApiError,
  getBalancesByPaymentCode,
  getResidentsById,
  postResidentsByResidentIdBankTransferPayments,
  postResidentsByResidentIdCashPayments,
} from '@/api'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { ProblemAlert } from '@/common/ProblemAlert'
import { formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { DEPOSIT_STATUS, LEDGER, TRANSACTION_TYPE, balanceText, formatMoney } from './amounts'

// Kế toán ghi nhận tiền nộp (017 T015; US2, US3; FR-005 → FR-007; BR-M11-12, 16; DBR-29): tìm hồ sơ theo mã nộp tiền
// đọc từ nội dung chuyển khoản, rồi ghi chuyển khoản (một hoặc hai dòng: sổ cọc, số dư, cùng mã giao dịch ngân hàng)
// hoặc tiền mặt. Giao dịch Đã xác nhận ngay, không cấp số phiếu. Mã ngân hàng đã dùng: backend trả 409 kèm giao dịch
// trước, Kế toán xem rồi xác nhận lại (không chặn). Màn gắn vào trang Số dư, thu chi (B) như một phần.

function today() {
  return new Date().toLocaleDateString('en-CA', { timeZone: 'Asia/Ho_Chi_Minh' })
}

function nowLocal() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`
}

function amountOf(text) {
  const digits = text.replace(/[^\d]/g, '')
  return digits === '' ? null : Number(digits)
}

function Found({ found }) {
  const b = found.balance
  return (
    <dl className="grid gap-x-6 gap-y-2 sm:grid-cols-2 lg:grid-cols-4">
      <div className="flex flex-col gap-0.5">
        <dt className="type-caption text-muted-foreground">Người cao tuổi</dt>
        <dd className="type-label">{found.fullName ?? `Mã hồ sơ ${b.residentId}`}</dd>
      </div>
      <div className="flex flex-col gap-0.5">
        <dt className="type-caption text-muted-foreground">Mã nộp tiền</dt>
        <dd className="tabular-nums">{b.paymentCode}</dd>
      </div>
      <div className="flex flex-col gap-0.5">
        <dt className="type-caption text-muted-foreground">Số dư, tiền cọc</dt>
        <dd className="tabular-nums">
          {balanceText(b.balance)}, cọc {formatMoney(b.depositBalance)}
        </dd>
      </div>
      <div className="flex flex-col gap-0.5">
        <dt className="type-caption text-muted-foreground">Đặt cọc</dt>
        <dd>{DEPOSIT_STATUS[b.depositStatus] ?? 'Chưa có hợp đồng Chờ ký, Hiệu lực'}</dd>
      </div>
    </dl>
  )
}

export function PaymentRecording() {
  const [code, setCode] = useState('')
  const [found, setFound] = useState(null)
  const [searchError, setSearchError] = useState('')
  const [method, setMethod] = useState('transfer')
  const [form, setForm] = useState({
    deposit: '',
    balance: '',
    receivedOn: today(),
    bankCode: '',
    cashLedger: 'BALANCE',
    cashAmount: '',
    receivedAt: nowLocal(),
    payer: '',
    note: '',
  })
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const [duplicates, setDuplicates] = useState(null)
  const [saving, setSaving] = useState(false)
  const [notice, setNotice] = useState('')

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }))

  async function search(event) {
    event.preventDefault()
    setSearchError('')
    setFound(null)
    setNotice('')
    if (!code.trim()) {
      setSearchError('Nhập mã nộp tiền trong nội dung chuyển khoản')
      return
    }
    try {
      const { data } = await getBalancesByPaymentCode({ query: { code: code.trim() } })
      let fullName = null
      try {
        fullName = (await getResidentsById({ path: { id: data.residentId } })).data?.fullName ?? null
      } catch {
        // Không đọc được hồ sơ thì chỉ hiện mã hồ sơ
      }
      setFound({ balance: data, fullName })
    } catch (error) {
      setSearchError(
        error instanceof ApiError && error.status === 404
          ? 'Không tìm thấy hồ sơ với mã này. Kiểm tra lại mã trong nội dung chuyển khoản.'
          : problemOf(error).title,
      )
    }
  }

  async function send(confirmDuplicateCode) {
    setSaving(true)
    setFailure(null)
    const residentId = found.balance.residentId
    const headers = { 'Idempotency-Key': crypto.randomUUID() }
    try {
      const { data } =
        method === 'transfer'
          ? await postResidentsByResidentIdBankTransferPayments({
              path: { residentId },
              headers,
              body: {
                lines: [
                  ...(amountOf(form.deposit) ? [{ ledger: 'DEPOSIT', amount: amountOf(form.deposit) }] : []),
                  ...(amountOf(form.balance) ? [{ ledger: 'BALANCE', amount: amountOf(form.balance) }] : []),
                ],
                receivedOn: form.receivedOn,
                bankTransactionCode: form.bankCode.trim(),
                payerName: form.payer.trim() || undefined,
                note: form.note.trim() || undefined,
                confirmDuplicateCode,
              },
            })
          : await postResidentsByResidentIdCashPayments({
              path: { residentId },
              headers,
              body: {
                ledger: form.cashLedger,
                amount: amountOf(form.cashAmount),
                payerName: form.payer.trim(),
                receivedAt: new Date(form.receivedAt).toISOString(),
                note: form.note.trim() || undefined,
              },
            })
      setDuplicates(null)
      setFound((f) => ({ ...f, balance: data.balance }))
      setForm((f) => ({ ...f, deposit: '', balance: '', bankCode: '', cashAmount: '', note: '' }))
      setNotice(
        `Đã ghi nhận. Số dư mới: ${balanceText(data.balance.balance)}, cọc ${formatMoney(data.balance.depositBalance)}.`,
      )
    } catch (error) {
      const p = problemOf(error)
      if (p.errorCode === 'BANK_TRANSACTION_CODE_USED') {
        setDuplicates(error.problem?.duplicateTransactions ?? [])
      } else if (error instanceof ApiError && error.status === 422) {
        setErrors({ form: p.detail || p.title })
      } else {
        setDuplicates(null)
        setFailure(p)
      }
    } finally {
      setSaving(false)
    }
  }

  function submit(event) {
    event.preventDefault()
    setNotice('')
    const next = {}
    if (method === 'transfer') {
      if (!amountOf(form.deposit) && !amountOf(form.balance)) next.amounts = 'Nhập số tiền vào sổ cọc hoặc vào số dư'
      if (!form.bankCode.trim()) next.bankCode = 'Nhập mã giao dịch ngân hàng'
      if (!form.receivedOn) next.receivedOn = 'Chọn ngày nhận'
    } else {
      if (!amountOf(form.cashAmount)) next.cashAmount = 'Nhập số tiền lớn hơn 0'
      if (!form.payer.trim()) next.payer = 'Nhập người nộp'
      if (!form.receivedAt) next.receivedAt = 'Chọn thời điểm thu'
    }
    setErrors(next)
    if (Object.keys(next).length === 0) send(false)
  }

  return (
    <div className="flex flex-col gap-5">
      <Card>
        <CardHeader>
          <CardTitle>Tìm theo mã nộp tiền</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <form onSubmit={search} noValidate className="flex flex-col gap-3 sm:flex-row sm:items-end">
            <Field data-invalid={!!searchError || undefined} className="sm:max-w-sm">
              <FieldLabel htmlFor="payment-code">Mã nộp tiền</FieldLabel>
              <Input
                id="payment-code"
                value={code}
                onChange={(e) => setCode(e.target.value)}
                aria-invalid={!!searchError || undefined}
                autoComplete="off"
              />
              <FieldDescription>Không phân biệt chữ hoa, chữ thường; bỏ khoảng trắng, gạch nối.</FieldDescription>
              {searchError && <FieldError>{searchError}</FieldError>}
            </Field>
            <Button type="submit" variant="outline">
              Tìm hồ sơ
            </Button>
          </form>
          {found && <Found found={found} />}
        </CardContent>
      </Card>

      {found && (
        <Card>
          <CardHeader>
            <CardTitle>Ghi nhận tiền nộp</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={submit} noValidate className="flex flex-col gap-4">
              <ToggleGroup
                variant="outline"
                value={[method]}
                onValueChange={(next) => {
                  if (next[0]) setMethod(next[0])
                  setErrors({})
                }}
                aria-label="Cách nộp"
              >
                <ToggleGroupItem value="transfer">Chuyển khoản</ToggleGroupItem>
                <ToggleGroupItem value="cash">Tiền mặt</ToggleGroupItem>
              </ToggleGroup>
              <FieldGroup>
                {method === 'transfer' ? (
                  <>
                    <div className="grid gap-4 sm:grid-cols-2">
                      <Field data-invalid={!!errors.amounts || undefined}>
                        <FieldLabel htmlFor="deposit-amount">Số tiền vào sổ cọc</FieldLabel>
                        <Input id="deposit-amount" inputMode="numeric" value={form.deposit} onChange={set('deposit')} />
                      </Field>
                      <Field data-invalid={!!errors.amounts || undefined}>
                        <FieldLabel htmlFor="balance-amount">Số tiền vào số dư</FieldLabel>
                        <Input id="balance-amount" inputMode="numeric" value={form.balance} onChange={set('balance')} />
                        {errors.amounts && <FieldError>{errors.amounts}</FieldError>}
                      </Field>
                    </div>
                    <div className="grid gap-4 sm:grid-cols-2">
                      <Field data-invalid={!!errors.bankCode || undefined}>
                        <FieldLabel htmlFor="bank-code">Mã giao dịch ngân hàng</FieldLabel>
                        <Input
                          id="bank-code"
                          value={form.bankCode}
                          onChange={set('bankCode')}
                          aria-invalid={!!errors.bankCode || undefined}
                        />
                        {errors.bankCode && <FieldError>{errors.bankCode}</FieldError>}
                      </Field>
                      <Field data-invalid={!!errors.receivedOn || undefined}>
                        <FieldLabel htmlFor="received-on">Ngày nhận</FieldLabel>
                        <Input id="received-on" type="date" value={form.receivedOn} onChange={set('receivedOn')} />
                        {errors.receivedOn && <FieldError>{errors.receivedOn}</FieldError>}
                      </Field>
                    </div>
                  </>
                ) : (
                  <>
                    <ToggleGroup
                      variant="outline"
                      value={[form.cashLedger]}
                      onValueChange={(next) => next[0] && setForm((f) => ({ ...f, cashLedger: next[0] }))}
                      aria-label="Sổ"
                    >
                      <ToggleGroupItem value="BALANCE">{LEDGER.BALANCE}</ToggleGroupItem>
                      <ToggleGroupItem value="DEPOSIT">{LEDGER.DEPOSIT}</ToggleGroupItem>
                    </ToggleGroup>
                    <div className="grid gap-4 sm:grid-cols-2">
                      <Field data-invalid={!!errors.cashAmount || undefined}>
                        <FieldLabel htmlFor="cash-amount">Số tiền</FieldLabel>
                        <Input
                          id="cash-amount"
                          inputMode="numeric"
                          value={form.cashAmount}
                          onChange={set('cashAmount')}
                          aria-invalid={!!errors.cashAmount || undefined}
                        />
                        {errors.cashAmount && <FieldError>{errors.cashAmount}</FieldError>}
                      </Field>
                      <Field data-invalid={!!errors.receivedAt || undefined}>
                        <FieldLabel htmlFor="received-at">Thời điểm thu</FieldLabel>
                        <Input
                          id="received-at"
                          type="datetime-local"
                          value={form.receivedAt}
                          onChange={set('receivedAt')}
                        />
                        {errors.receivedAt && <FieldError>{errors.receivedAt}</FieldError>}
                      </Field>
                    </div>
                  </>
                )}
                <Field data-invalid={!!errors.payer || undefined}>
                  <FieldLabel htmlFor="payer">Người nộp</FieldLabel>
                  <Input
                    id="payer"
                    value={form.payer}
                    onChange={set('payer')}
                    aria-invalid={!!errors.payer || undefined}
                  />
                  {errors.payer && <FieldError>{errors.payer}</FieldError>}
                </Field>
                <Field data-invalid={!!errors.form || undefined}>
                  <FieldLabel htmlFor="payment-note">Ghi chú</FieldLabel>
                  <Textarea id="payment-note" value={form.note} onChange={set('note')} rows={2} />
                  <FieldDescription>
                    Ghi được số chứng từ của phần mềm kế toán; hệ thống không cấp số phiếu.
                  </FieldDescription>
                  {errors.form && <FieldError>{errors.form}</FieldError>}
                </Field>
              </FieldGroup>
              <ProblemAlert problem={failure} />
              {notice && (
                <Alert>
                  <AlertDescription>{notice}</AlertDescription>
                </Alert>
              )}
              <Button type="submit" size="lg" className="self-start" disabled={saving}>
                {method === 'transfer' ? 'Ghi nhận chuyển khoản' : 'Ghi nhận tiền mặt'}
              </Button>
            </form>
          </CardContent>
        </Card>
      )}

      <Dialog open={duplicates !== null} onOpenChange={(open) => !open && setDuplicates(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Mã giao dịch ngân hàng đã dùng</DialogTitle>
            <DialogDescription>
              Mã {form.bankCode.trim()} đã có ở giao dịch dưới đây. Một lần chuyển khoản gộp nhiều khoản được ghi cùng
              mã; kiểm tra trước khi ghi nhận lại.
            </DialogDescription>
          </DialogHeader>
          <ul className="flex flex-col gap-2">
            {(duplicates ?? []).map((t) => (
              <li key={t.id} className="flex flex-col gap-0.5 border-t pt-2 first:border-t-0 first:pt-0">
                <span className="type-label">
                  {TRANSACTION_TYPE[t.type] ?? t.type}, {LEDGER[t.ledger] ?? t.ledger}
                </span>
                <span className="tabular-nums">
                  {formatMoney(t.amount)}
                  {t.at && `, ${formatDateTime(t.at)}`}
                </span>
              </li>
            ))}
          </ul>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDuplicates(null)}>
              Quay lại sửa
            </Button>
            <Button onClick={() => send(true)} disabled={saving}>
              Vẫn ghi nhận
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
