import { useState } from 'react'
import { postRelationshipsByIdByCommand } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { uploadFile } from '@/common/files'
import { problemOf } from '@/common/problem'
import { CONFLICT_HINTS, RELATIONSHIP_LABELS, rightLines, rolesOf } from './relationshipLabels'

// Khung chi tiết một quan hệ (012 T013; FR-002 → FR-012): hồ sơ người thân, vai trò, sáu quyền đang bật và tác dụng;
// lệnh của Hành chính mở form ngay dưới, lý do bắt buộc (DESIGN.md 7.4). Quyền chỉ đổi qua yêu cầu (US3), không có nút
// sửa quyền ở đây. Kết thúc quan hệ có bước xác nhận vì không kích hoạt lại được (FR-006; WCAG 3.3.4).

const COMMANDS = {
  'change-primary-contact': {
    label: 'Đổi người liên hệ chính',
    confirm: 'Xác nhận đổi người liên hệ chính',
    done: 'Đã đổi người liên hệ chính',
  },
  'set-first-representative': {
    label: 'Đặt người đại diện đầu tiên',
    confirm: 'Xác nhận đặt người đại diện',
    done: 'Đã đặt người đại diện',
    evidence: true,
  },
  'change-contact-order': {
    label: 'Đổi thứ tự liên hệ',
    confirm: 'Lưu thứ tự liên hệ',
    done: 'Đã đổi thứ tự liên hệ',
    order: true,
  },
  end: {
    label: 'Kết thúc quan hệ',
    confirm: 'Xác nhận kết thúc quan hệ',
    done: 'Đã kết thúc quan hệ',
    note: 'Quan hệ đã kết thúc không kích hoạt lại được. Người thân bị bỏ khỏi danh sách được phép đón.',
  },
}

function CommandForm({ relationship, command, onDone, onCancel }) {
  const spec = COMMANDS[command]
  const [key] = useState(() => crypto.randomUUID())
  const [reason, setReason] = useState('')
  const [order, setOrder] = useState(String(relationship.contactOrder))
  const [file, setFile] = useState(null)
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)

  const reasonMissing = !reason.trim()
  const orderInvalid = spec.order && !/^\d{1,4}$/.test(order)
  const fileMissing = spec.evidence && !file

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (reasonMissing || orderInvalid || fileMissing) return
    setBusy(true)
    try {
      const body = { reason: reason.trim() }
      if (spec.order) body.contactOrder = Number(order)
      if (spec.evidence) body.evidenceFileId = await uploadFile(file)
      await postRelationshipsByIdByCommand({
        path: { id: relationship.id, command },
        headers: { 'Idempotency-Key': key },
        body,
      })
      onDone(spec.done)
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-4 rounded-lg border p-4">
      <h3 className="type-card-title">{spec.label}</h3>
      {spec.note && <p className="type-body-sm text-muted-foreground">{spec.note}</p>}
      {spec.order && (
        <Field data-invalid={(submitted && orderInvalid) || undefined}>
          <FieldLabel htmlFor="contact-order">Thứ tự liên hệ mới</FieldLabel>
          <Input
            id="contact-order"
            inputMode="numeric"
            className="h-11"
            value={order}
            onChange={(e) => setOrder(e.target.value)}
            aria-invalid={(submitted && orderInvalid) || undefined}
          />
          {submitted && orderInvalid && <FieldError>Nhập số thứ tự, ví dụ 1</FieldError>}
        </Field>
      )}
      {spec.evidence && (
        <Field data-invalid={(submitted && fileMissing) || undefined}>
          <FieldLabel htmlFor="representative-evidence">
            Bằng chứng (văn bản ủy quyền, giấy tờ quan hệ, hợp đồng)
          </FieldLabel>
          <input
            id="representative-evidence"
            type="file"
            accept="application/pdf,image/jpeg,image/png"
            className="type-body-sm h-11 rounded-lg border px-3 py-2"
            aria-invalid={(submitted && fileMissing) || undefined}
            onChange={(e) => setFile(e.target.files?.[0] ?? null)}
          />
          {submitted && fileMissing && <FieldError>Chọn tệp bằng chứng</FieldError>}
        </Field>
      )}
      <Field data-invalid={(submitted && reasonMissing) || undefined}>
        <FieldLabel htmlFor="command-reason">Lý do</FieldLabel>
        <Textarea
          id="command-reason"
          rows={3}
          maxLength={2000}
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          aria-invalid={(submitted && reasonMissing) || undefined}
        />
        {submitted && reasonMissing && <FieldError>Nhập lý do</FieldError>}
      </Field>
      <ProblemAlert problem={failure}>
        {failure && CONFLICT_HINTS[failure.errorCode] && <p>{CONFLICT_HINTS[failure.errorCode]}</p>}
      </ProblemAlert>
      <div className="flex flex-wrap gap-3">
        <Button type="submit" className="h-11" variant={command === 'end' ? 'destructive' : 'default'} disabled={busy}>
          {spec.confirm}
        </Button>
        <Button type="button" variant="outline" className="h-11" onClick={onCancel}>
          Hủy
        </Button>
      </div>
    </form>
  )
}

export function RelationshipDetail({ relationship, hasRepresentative, canUpdate, onChanged, Title, Description }) {
  const [command, setCommand] = useState(null)
  const member = relationship.familyMember
  const active = relationship.status === 'ACTIVE'
  const roles = rolesOf(relationship)

  const commands =
    !active || !canUpdate
      ? []
      : [
          !relationship.primaryContact && 'change-primary-contact',
          !hasRepresentative && 'set-first-representative',
          'change-contact-order',
          'end',
        ].filter(Boolean)

  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-col gap-1">
        <Title className="type-title">{member.fullName}</Title>
        <Description className="type-body-sm text-muted-foreground">
          {[RELATIONSHIP_LABELS[relationship.relationship] ?? relationship.relationship, roles]
            .filter(Boolean)
            .join(', ')}
        </Description>
        <StatusBadge status={active ? 'Hiệu lực' : 'Đã kết thúc'} />
      </div>

      <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 type-body-sm">
        <dt className="text-muted-foreground">Số điện thoại</dt>
        <dd className="tabular-nums">{member.phoneNumber}</dd>
        <dt className="text-muted-foreground">Giấy tờ tùy thân</dt>
        <dd>
          {member.idDocumentNumber ? `${member.idDocumentType ?? ''} ${member.idDocumentNumber}`.trim() : 'Chưa ghi'}
        </dd>
        <dt className="text-muted-foreground">Thứ tự liên hệ</dt>
        <dd className="tabular-nums">{relationship.contactOrder}</dd>
      </dl>

      <section aria-labelledby={`rights-${relationship.id}`} className="flex flex-col gap-2">
        <h3 id={`rights-${relationship.id}`} className="type-card-title">
          Quyền
        </h3>
        <dl className="grid grid-cols-[1fr_auto] gap-x-4 gap-y-1 type-body-sm">
          {rightLines(relationship).map((line) => (
            <div key={line.right} className="contents">
              <dt>{line.label}</dt>
              <dd className={line.value === 'Có, không có tác dụng' ? 'text-warning-foreground' : undefined}>
                {line.value}
              </dd>
            </div>
          ))}
        </dl>
        <p className="type-caption text-muted-foreground">
          Quyền chỉ đổi qua yêu cầu thay đổi quyền hoặc phiếu đăng ký.
        </p>
      </section>

      {command ? (
        <CommandForm
          key={command}
          relationship={relationship}
          command={command}
          onCancel={() => setCommand(null)}
          onDone={(message) => {
            setCommand(null)
            onChanged(message)
          }}
        />
      ) : (
        commands.length > 0 && (
          <div className="flex flex-wrap gap-3">
            {commands.map((c) => (
              <Button
                key={c}
                variant={c === 'end' ? 'destructive' : 'outline'}
                className="h-11"
                onClick={() => setCommand(c)}
              >
                {COMMANDS[c].label}
              </Button>
            ))}
          </div>
        )
      )}
    </div>
  )
}
