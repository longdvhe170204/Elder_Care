import { useEffect, useState } from 'react'
import {
  getTaskTypes,
  postTaskTypes,
  putTaskTypesById,
  postTaskTypesByIdDeactivate,
  getResultValues,
  postResultValues,
  putResultValuesById,
  postResultValuesByIdDeactivate,
  getNonPerformanceReasonTypes,
  postNonPerformanceReasonTypes,
  putNonPerformanceReasonTypesById,
  postNonPerformanceReasonTypesByIdDeactivate,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog'
import { PageHeader } from '@/common/PageHeader'
import { StatusBadge } from '@/common/StatusBadge'
import { ProblemAlert } from '@/common/ProblemAlert'
import { PermissionDeniedState } from '@/common/PageState'
import { problemOf } from '@/common/problem'
import { usePermissions } from '@/common/permissions/permissions'
import { CareField, CareCheck } from './CareField'
import { CARE_ROLES, IMPORTANCE, RESULT_TYPES, TASK_GROUPS } from './carePlanLabels'

const CATALOGS = {
  tasks: {
    title: 'Loại công việc',
    get: getTaskTypes,
    create: postTaskTypes,
    update: putTaskTypesById,
    deactivate: postTaskTypesByIdDeactivate,
  },
  results: {
    title: 'Giá trị kết quả',
    get: getResultValues,
    create: postResultValues,
    update: putResultValuesById,
    deactivate: postResultValuesByIdDeactivate,
  },
  reasons: {
    title: 'Lý do không thực hiện',
    get: getNonPerformanceReasonTypes,
    create: postNonPerformanceReasonTypes,
    update: putNonPerformanceReasonTypesById,
    deactivate: postNonPerformanceReasonTypesByIdDeactivate,
  },
}
const NUMBERS = ['windowBeforeMinutes', 'windowAfterMinutes', 'serviceId', 'upperLimit']

function CatalogForm({ kind, row, deactivate, onSaved, onReload }) {
  const [values, setValues] = useState(() => ({
    name: '',
    group: '',
    professional: false,
    resultType: '',
    defaultRole: '',
    defaultImportance: '',
    billable: false,
    type: '',
    code: '',
    ruleGroup: '',
    ...row,
    certificates: (row?.certificates ?? []).join(', '),
  }))
  const [reason, setReason] = useState('')
  const [error, setError] = useState(null)
  const [reasonError, setReasonError] = useState(false)
  const [busy, setBusy] = useState(false)
  const set = (name, value) => setValues((old) => ({ ...old, [name]: value }))
  const field = (name, label, options, type = 'text', required = false) => (
    <CareField
      key={name}
      label={label}
      value={values[name]}
      onChange={(value) => set(name, value)}
      options={options}
      type={type}
      required={required}
    />
  )
  async function submit(event) {
    event.preventDefault()
    if (deactivate && !reason.trim()) {
      setReasonError(true)
      return
    }
    let body =
      kind === 'reasons'
        ? { name: values.name }
        : kind === 'results'
          ? { type: values.type, code: values.code, name: values.name, ruleGroup: values.ruleGroup || null }
          : {
              name: values.name,
              group: values.group,
              professional: values.professional,
              resultType: values.resultType,
              defaultRole: values.defaultRole,
              defaultImportance: values.defaultImportance,
              billable: values.billable,
              certificates: values.certificates
                .split(',')
                .map((v) => v.trim())
                .filter(Boolean),
              vitalSignType: values.vitalSignType || null,
              ...Object.fromEntries(
                NUMBERS.map((key) => [key, values[key] === '' || values[key] == null ? null : Number(values[key])]),
              ),
            }
    if (deactivate) body = { reason: reason.trim() }
    setBusy(true)
    setError(null)
    try {
      const operation = deactivate ? CATALOGS[kind].deactivate : row ? CATALOGS[kind].update : CATALOGS[kind].create
      await operation({
        path: row ? { id: row.id } : undefined,
        body,
        headers: {
          'Idempotency-Key': crypto.randomUUID(),
          ...(row && !deactivate && { 'If-Match': `"${row.version}"` }),
        },
      })
      onSaved()
    } catch (failure) {
      setError(problemOf(failure))
    } finally {
      setBusy(false)
    }
  }
  return (
    <form onSubmit={submit} className="flex flex-col gap-4">
      <ProblemAlert problem={error}>
        {error?.status === 412 && (
          <>
            <p>{error.currentValue?.name}</p>
            <Button type="button" variant="outline" onClick={onReload}>
              Tải lại danh mục
            </Button>
          </>
        )}
      </ProblemAlert>
      {deactivate ? (
        <>
          <CareField label="Lý do" value={reason} onChange={setReason} type="textarea" maxLength={2000} />
          <p className="type-body-sm text-muted-foreground">Ngừng hiệu lực: {row.name}</p>
          {reasonError && (
            <p role="alert" className="text-destructive">
              Nhập lý do trước khi thực hiện.
            </p>
          )}
        </>
      ) : (
        <>
          <CareField
            label="Tên"
            value={values.name}
            onChange={(value) => set('name', value)}
            required
            maxLength={200}
          />
          {kind === 'tasks' && (
            <div className="grid gap-4 sm:grid-cols-2">
              {field('group', 'Nhóm công việc', TASK_GROUPS, 'text', true)}
              {field('resultType', 'Kết quả cần ghi', RESULT_TYPES, 'text', true)}
              {field('defaultRole', 'Vai trò mặc định', CARE_ROLES, 'text', true)}
              {field('defaultImportance', 'Mức quan trọng mặc định', IMPORTANCE, 'text', true)}
              <CareCheck
                label="Công việc chuyên môn"
                checked={values.professional}
                onChange={(value) => set('professional', value)}
              />
              <CareCheck label="Có tính phí" checked={values.billable} onChange={(value) => set('billable', value)} />
              {field('windowBeforeMinutes', 'Khung trước (phút)', null, 'number')}
              {field('windowAfterMinutes', 'Khung sau (phút)', null, 'number')}
              {field('serviceId', 'Mã dịch vụ', null, 'number')}
              {field('upperLimit', 'Giới hạn trên', null, 'number')}
              {field('vitalSignType', 'Loại chỉ số')}
              {field('certificates', 'Mã chứng chỉ, cách nhau bằng dấu phẩy')}
            </div>
          )}
          {kind === 'results' && (
            <>
              {field(
                'type',
                'Loại kết quả',
                { SLEEP_QUALITY: 'Chất lượng ngủ', MOOD: 'Tâm trạng', BEHAVIOR: 'Hành vi' },
                'text',
                true,
              )}
              {field('code', 'Mã giá trị', null, 'text', true)}
              {field('ruleGroup', 'Nhóm quy tắc', { INSOMNIA: 'Mất ngủ', NEGATIVE: 'Tiêu cực' })}
            </>
          )}
        </>
      )}
      <Button disabled={busy || error?.status === 412} type="submit">
        {busy ? 'Đang lưu…' : deactivate ? 'Xác nhận ngừng' : 'Lưu danh mục'}
      </Button>
    </form>
  )
}

export function CareCatalogPage() {
  const { can } = usePermissions()
  const [kind, setKind] = useState('tasks')
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [failure, setFailure] = useState(null)
  const [reload, setReload] = useState(0)
  const [editing, setEditing] = useState(null)
  useEffect(() => {
    let cancelled = false
    CATALOGS[kind]
      .get()
      .then(({ data }) => {
        if (!cancelled) {
          setItems(data)
          setFailure(null)
          setLoading(false)
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setFailure(problemOf(error))
          setLoading(false)
        }
      })
    return () => {
      cancelled = true
    }
  }, [kind, reload])
  function refresh() {
    setEditing(null)
    setLoading(true)
    setReload((value) => value + 1)
  }
  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Danh mục chăm sóc" subtitle="Loại công việc, giá trị kết quả và lý do không thực hiện" />
      <div className="flex flex-wrap gap-2">
        {Object.entries(CATALOGS).map(([key, value]) => (
          <Button
            key={key}
            variant={key === kind ? 'default' : 'outline'}
            aria-pressed={kind === key}
            onClick={() => {
              setLoading(true)
              setKind(key)
            }}
          >
            {value.title}
          </Button>
        ))}
      </div>
      {failure?.status === 403 ? (
        <PermissionDeniedState label="danh mục chăm sóc" />
      ) : (
        <section className="flex flex-col gap-4 rounded-xl border bg-card p-5" aria-busy={loading}>
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 className="type-title">{CATALOGS[kind].title}</h2>
            {can('CARE_TASK_CATALOG', 'CREATE') && (
              <Button onClick={() => setEditing({ row: null })}>Thêm danh mục</Button>
            )}
          </div>
          <ProblemAlert problem={failure} />
          {loading ? (
            <p role="status">Đang tải danh mục…</p>
          ) : items.length === 0 ? (
            <p>Chưa có mục danh mục.</p>
          ) : (
            <ul className="divide-y">
              {items.map((row) => (
                <li key={row.id} className="flex flex-wrap items-center justify-between gap-4 py-4">
                  <div>
                    <p className="type-label">{row.name}</p>
                    <StatusBadge status={row.status === 'ACTIVE' ? 'Hiệu lực' : 'Ngừng hiệu lực'} />
                    {row.fixed && <p className="type-body-sm text-muted-foreground">Lý do cố định</p>}
                  </div>
                  {can('CARE_TASK_CATALOG', 'UPDATE') && !row.fixed && (
                    <div className="flex gap-2">
                      <Button variant="outline" aria-label={`Sửa ${row.name}`} onClick={() => setEditing({ row })}>
                        Sửa
                      </Button>
                      {row.status === 'ACTIVE' && (
                        <Button
                          variant="outline"
                          aria-label={`Ngừng ${row.name}`}
                          onClick={() => setEditing({ row, deactivate: true })}
                        >
                          Ngừng hiệu lực
                        </Button>
                      )}
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>
      )}
      <Dialog open={editing !== null} onOpenChange={(open) => !open && setEditing(null)}>
        <DialogContent className="max-h-[90dvh] overflow-y-auto sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>
              {editing?.deactivate ? 'Ngừng hiệu lực' : editing?.row ? 'Sửa danh mục' : 'Thêm danh mục'}
            </DialogTitle>
            <DialogDescription>{CATALOGS[kind].title}</DialogDescription>
          </DialogHeader>
          {editing && (
            <CatalogForm
              key={`${kind}-${editing.row?.id ?? 'new'}-${editing.deactivate ?? false}`}
              kind={kind}
              {...editing}
              onSaved={refresh}
              onReload={refresh}
            />
          )}
        </DialogContent>
      </Dialog>
    </div>
  )
}
