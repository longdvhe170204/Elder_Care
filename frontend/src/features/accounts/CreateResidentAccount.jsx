import { useEffect, useState } from 'react'
import { ApiError, getResidents, postResidentAccounts } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { ResidentConsentFields } from './ResidentConsentFields'
import { EMPTY_CONSENT, consentBody, consentErrors } from './residentConsent'
import { TemporaryPasswordNotice } from './TemporaryPasswordNotice'

// Tạo tài khoản người cao tuổi (002 T095; US8 #1; FR-067; bố cục duyệt 2026-10-24; hợp đồng POST /resident-accounts).
// Danh sách là hồ sơ Đang lưu trú trong phạm vi (GET /residents của 001); đã có tài khoản thì backend trả 409.
// Idempotency-Key giữ suốt form.

export function CreateResidentAccount({ onCreated, onClose }) {
  const [residents, setResidents] = useState(null)
  const [residentId, setResidentId] = useState('')
  const [username, setUsername] = useState('')
  const [consent, setConsent] = useState(EMPTY_CONSENT)
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const [saving, setSaving] = useState(false)
  const [created, setCreated] = useState(null)
  const [key] = useState(() => crypto.randomUUID())

  useEffect(() => {
    let cancelled = false
    getResidents({ query: { status: ['IN_RESIDENCE'] } })
      .then(({ data }) => !cancelled && setResidents(data.items))
      .catch((error) => {
        if (!cancelled) {
          setResidents([])
          setFailure(problemOf(error))
        }
      })
    return () => {
      cancelled = true
    }
  }, [])

  async function submit(event) {
    event.preventDefault()
    setFailure(null)
    const next = consentErrors(consent)
    if (!residentId) next.resident = 'Chọn một người cao tuổi'
    if (!username.trim()) next.username = 'Nhập tên đăng nhập'
    setErrors(next)
    if (Object.keys(next).length > 0) return

    setSaving(true)
    try {
      const { data } = await postResidentAccounts({
        headers: { 'Idempotency-Key': key },
        body: { residentId: Number(residentId), username: username.trim(), consent: consentBody(consent) },
      })
      setCreated(data)
      onCreated(data)
    } catch (error) {
      const p = problemOf(error)
      if (error instanceof ApiError && error.status === 422) {
        setErrors({ username: p.detail || p.title })
      } else {
        setFailure(p)
      }
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <SheetHeader className="gap-1 pt-6">
        <SheetTitle className="type-title">Tạo tài khoản người cao tuổi</SheetTitle>
        <SheetDescription>Chỉ khi chính người cao tuổi yêu cầu hoặc đồng ý.</SheetDescription>
      </SheetHeader>
      <div className="flex flex-col gap-5 overflow-y-auto px-4 pb-6">
        {created ? (
          <div className="flex flex-col gap-3">
            <p className="type-body-sm">
              Đã tạo tài khoản {created.username}. Tạo mã QR đăng nhập trong khung chi tiết tài khoản.
            </p>
            <TemporaryPasswordNotice password={created.temporaryPassword} />
            <Button variant="outline" className="self-start" onClick={onClose}>
              Xong
            </Button>
          </div>
        ) : (
          <form onSubmit={submit} noValidate className="flex flex-col gap-4">
            <FieldGroup>
              <Field data-invalid={!!errors.resident || undefined}>
                <FieldLabel htmlFor="create-resident-account-resident">Người cao tuổi</FieldLabel>
                <select
                  id="create-resident-account-resident"
                  value={residentId}
                  onChange={(e) => setResidentId(e.target.value)}
                  aria-invalid={!!errors.resident || undefined}
                  className="h-10 w-full rounded-md border border-input bg-transparent px-3"
                >
                  <option value="">{residents === null ? 'Đang tải…' : 'Chọn người cao tuổi'}</option>
                  {(residents ?? []).map((r) => (
                    <option key={r.id} value={r.id}>
                      {`${r.personalInfo.fullName} · ${r.profileCode}`}
                    </option>
                  ))}
                </select>
                {errors.resident ? (
                  <FieldError>{errors.resident}</FieldError>
                ) : (
                  <FieldDescription>Hồ sơ Đang lưu trú; mỗi người tối đa một tài khoản.</FieldDescription>
                )}
              </Field>
              <Field data-invalid={!!errors.username || undefined}>
                <FieldLabel htmlFor="create-resident-account-username">Tên đăng nhập</FieldLabel>
                <Input
                  id="create-resident-account-username"
                  autoComplete="off"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  aria-invalid={!!errors.username || undefined}
                  className="h-10"
                />
                {errors.username ? (
                  <FieldError>{errors.username}</FieldError>
                ) : (
                  <FieldDescription>Dùng khi đăng nhập bằng mật khẩu dự phòng.</FieldDescription>
                )}
              </Field>
              <ResidentConsentFields
                idPrefix="create-resident-account"
                consent={consent}
                onChange={setConsent}
                errors={errors}
              />
            </FieldGroup>

            <ProblemAlert problem={failure} />

            <div className="flex flex-wrap gap-3">
              <Button type="submit" className="h-11" disabled={saving}>
                Tạo tài khoản
              </Button>
              <Button type="button" variant="outline" className="h-11" onClick={onClose}>
                Hủy
              </Button>
            </div>
          </form>
        )}
      </div>
    </>
  )
}
