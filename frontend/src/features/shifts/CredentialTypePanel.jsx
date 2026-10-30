import { useState } from 'react'
import { postCredentialTypes } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { ROLES } from '@/common/layout/navigation'
import { Choice } from './ShiftForm'
export function CredentialTypePanel({ types, create, onDone }) {
  const [adding, setAdding] = useState(false),
    [name, setName] = useState(''),
    [group, setGroup] = useState('TRAINING'),
    [expiry, setExpiry] = useState(true),
    [scopes, setScopes] = useState(false),
    [roles, setRoles] = useState([]),
    [failure, setFailure] = useState(null),
    [busy, setBusy] = useState(false)
  async function save(e) {
    e.preventDefault()
    setBusy(true)
    try {
      await postCredentialTypes({
        body: { name, group, hasExpiry: expiry, hasPracticeScopes: scopes, requiredForRoles: roles, active: true },
        headers: { 'Idempotency-Key': crypto.randomUUID() },
      })
      setAdding(false)
      setName('')
      setRoles([])
      onDone()
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }
  return (
    <section className="flex flex-col gap-4 rounded-2xl border bg-card p-4 md:p-5">
      <div className="flex flex-wrap justify-between gap-3">
        <h2 className="type-section">Loại giấy phép và đào tạo</h2>
        {create && (
          <Button variant="outline" onClick={() => setAdding(true)}>
            Thêm loại chứng chỉ
          </Button>
        )}
      </div>
      <ProblemAlert problem={failure} />
      {types.map((t) => (
        <article key={t.id} className="rounded-xl border p-3">
          <p className="font-medium">{t.name}</p>
          <p className="text-muted-foreground">
            {t.group === 'TRAINING' ? 'Đào tạo' : 'Giấy phép hành nghề'} ·{' '}
            {t.hasExpiry ? 'Có thời hạn' : 'Không thời hạn'}
            {t.active === false ? ' · Ngừng sử dụng' : ''}
          </p>
          <p>
            Bắt buộc với:{' '}
            {(t.requiredForRoles ?? []).map((code) => ROLES.find((r) => r.code === code)?.label ?? code).join(', ') ||
              'Chưa có vai trò bắt buộc'}
          </p>
        </article>
      ))}
      {adding && (
        <form onSubmit={save} className="flex flex-col gap-4 rounded-xl border p-4">
          <Field>
            <FieldLabel htmlFor="credential-type-name">Tên loại chứng chỉ</FieldLabel>
            <Input
              id="credential-type-name"
              required
              maxLength={200}
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </Field>
          <Choice
            id="credential-type-group"
            label="Nhóm chứng chỉ"
            value={group}
            onChange={(e) => setGroup(e.target.value)}
          >
            <option value="TRAINING">Đào tạo</option>
            <option value="PROFESSIONAL_LICENSE">Giấy phép hành nghề</option>
          </Choice>
          <label className="flex min-h-11 items-center gap-2">
            <input type="checkbox" checked={expiry} onChange={(e) => setExpiry(e.target.checked)} />
            Có thời hạn
          </label>
          <label className="flex min-h-11 items-center gap-2">
            <input type="checkbox" checked={scopes} onChange={(e) => setScopes(e.target.checked)} />
            Có phạm vi hành nghề
          </label>
          <fieldset className="grid gap-2 sm:grid-cols-2">
            <legend>Bắt buộc với vai trò</legend>
            {ROLES.filter((r) => r.code !== 'FAMILY_MEMBER').map((r) => (
              <label key={r.code} className="flex min-h-11 items-center gap-2">
                <input
                  type="checkbox"
                  checked={roles.includes(r.code)}
                  onChange={(e) =>
                    setRoles(e.target.checked ? [...roles, r.code] : roles.filter((code) => code !== r.code))
                  }
                />
                {r.label}
              </label>
            ))}
          </fieldset>
          <div className="flex flex-wrap gap-2">
            <Button type="submit" disabled={busy}>
              Lưu loại chứng chỉ
            </Button>
            <Button type="button" variant="outline" onClick={() => setAdding(false)} disabled={busy}>
              Hủy
            </Button>
          </div>
        </form>
      )}
    </section>
  )
}
