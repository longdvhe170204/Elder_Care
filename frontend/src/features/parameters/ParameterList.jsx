import { useCallback, useEffect, useState } from 'react'
import { ApiError, getParameters, getParametersByCode } from '@/api'
import { Alert, AlertAction, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { formatDate } from '@/common/dateTime'
import { ParameterForm } from './ParameterForm'
import { ParameterHistory } from './ParameterHistory'
import { formatValue } from './parameterValue'

// Danh sách tham số vận hành (000 T081; US4 #1, #3, #6; FR-011, FR-013; hợp đồng GET /parameters).
// Mọi nhân viên xem được; chỉ Quản lý viện đổi giá trị (BR-M15-04): canChange chỉ để ẩn form, backend vẫn kiểm.

const MODULES = Array.from({ length: 15 }, (_, i) => `M${String(i + 1).padStart(2, '0')}`)

function matches(parameter, search) {
  const text = search.trim().toLowerCase()
  if (!text) return true
  return parameter.code.toLowerCase().includes(text) || parameter.description.toLowerCase().includes(text)
}

function ParameterDetail({ code, canChange, onChanged }) {
  const [state, setState] = useState({ status: 'loading' })
  const [tab, setTab] = useState(canChange ? 'change' : 'history')

  const load = useCallback(async () => {
    setState({ status: 'loading' })
    try {
      const { data, etag } = await getParametersByCode({ path: { code } })
      setState({ status: 'ready', parameter: data, etag })
      onChanged(data)
    } catch (error) {
      setState({ status: 'error', problem: problemOf(error) })
    }
  }, [code, onChanged])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải chi tiết (kèm ETag) khi mở khung
    load()
  }, [load])

  if (state.status === 'loading') {
    return (
      <div aria-busy="true" className="flex flex-col gap-2 px-4 pt-14">
        <span className="sr-only">Đang tải tham số</span>
        <Skeleton className="h-8" />
        <Skeleton className="h-24" />
      </div>
    )
  }
  if (state.status === 'error') {
    return (
      <div className="px-4 pt-14">
        <ProblemAlert problem={state.problem} />
      </div>
    )
  }

  const { parameter, etag } = state
  function saved(data, nextEtag) {
    setState({ status: 'ready', parameter: data, etag: nextEtag })
    onChanged(data)
  }

  return (
    <>
      <SheetHeader className="gap-1 pt-6">
        <SheetTitle className="type-title">{parameter.code}</SheetTitle>
        <SheetDescription>{parameter.description}</SheetDescription>
      </SheetHeader>
      <div className="flex flex-col gap-5 overflow-y-auto px-4 pb-6 type-body-sm">
        <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1">
          <dt className="text-muted-foreground">Hiện hành</dt>
          <dd className="font-medium">{formatValue(parameter.type, parameter.currentValue, parameter.unit)}</dd>
          <dt className="text-muted-foreground">Mặc định</dt>
          <dd>{formatValue(parameter.type, parameter.defaultValue, parameter.unit)}</dd>
        </dl>
        {parameter.usedIn && <p className="text-muted-foreground">Dùng tại: {parameter.usedIn}</p>}

        {canChange && (
          <ToggleGroup
            variant="outline"
            value={[tab]}
            onValueChange={(next) => next[0] && setTab(next[0])}
            className="grid w-full grid-cols-2"
          >
            <ToggleGroupItem value="change" className="h-10">
              Đổi giá trị
            </ToggleGroupItem>
            <ToggleGroupItem value="history" className="h-10">
              Lịch sử
            </ToggleGroupItem>
          </ToggleGroup>
        )}
        {canChange && tab === 'change' ? (
          <ParameterForm parameter={parameter} etag={etag} onSaved={saved} onStale={load} />
        ) : (
          <ParameterHistory key={etag} parameter={parameter} />
        )}
      </div>
    </>
  )
}

export function ParameterList({ canChange }) {
  const [module, setModule] = useState('')
  const [search, setSearch] = useState('')
  const [state, setState] = useState({ status: 'loading', items: [] })
  const [opened, setOpened] = useState(null)

  const load = useCallback(async (nextModule) => {
    setState({ status: 'loading', items: [] })
    try {
      const { data } = await getParameters({ query: { module: nextModule || undefined } })
      setState({ status: 'ready', items: data })
    } catch (error) {
      setState({ status: error instanceof ApiError && error.status === 403 ? 'denied' : 'error', items: [] })
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải danh sách khi mở màn
    load('')
  }, [load])

  const replaceRow = useCallback((data) => {
    setState((s) => ({ ...s, items: s.items.map((p) => (p.code === data.code ? data : p)) }))
  }, [])

  if (state.status === 'denied') return <PermissionDeniedState label="tham số vận hành" />

  const rows = state.items.filter((p) => matches(p, search))

  return (
    <section className="flex flex-col gap-3 rounded-2xl border bg-card p-4 md:p-5">
      <div className="grid gap-3 md:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
        <Field>
          <FieldLabel htmlFor="parameter-search">Tìm mã hoặc mô tả</FieldLabel>
          <Input id="parameter-search" value={search} onChange={(e) => setSearch(e.target.value)} className="h-10" />
        </Field>
        <Field>
          <FieldLabel htmlFor="parameter-module">Module</FieldLabel>
          <select
            id="parameter-module"
            value={module}
            onChange={(e) => {
              setModule(e.target.value)
              load(e.target.value)
            }}
            className="h-10 w-full rounded-md border border-input bg-transparent px-3"
          >
            <option value="">Tất cả</option>
            {MODULES.map((m) => (
              <option key={m} value={m}>
                {m}
              </option>
            ))}
          </select>
        </Field>
      </div>

      {state.status === 'loading' && (
        <div aria-busy="true" className="flex flex-col gap-2">
          <span className="sr-only">Đang tải tham số</span>
          <Skeleton className="h-10" />
          <Skeleton className="h-10" />
          <Skeleton className="h-10" />
        </div>
      )}
      {state.status === 'error' && (
        <Alert role="alert">
          <AlertTitle>Không tải được tham số</AlertTitle>
          <AlertAction>
            <Button variant="outline" size="sm" onClick={() => load(module)}>
              Thử lại
            </Button>
          </AlertAction>
        </Alert>
      )}
      {state.status === 'ready' && rows.length === 0 && (
        <Empty>
          <EmptyHeader>
            <EmptyTitle>Không có tham số khớp</EmptyTitle>
            <EmptyDescription>Đổi từ khóa hoặc chọn module khác.</EmptyDescription>
          </EmptyHeader>
        </Empty>
      )}
      {state.status === 'ready' && rows.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Mã</TableHead>
              <TableHead className="min-w-56">Mô tả</TableHead>
              <TableHead>Hiện hành</TableHead>
              <TableHead>Mặc định</TableHead>
              <TableHead>Đổi lần cuối</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((p) => (
              <TableRow key={p.code} className="align-top">
                <TableCell>
                  <Button variant="link" className="h-auto p-0 tabular-nums" onClick={() => setOpened(p.code)}>
                    {p.code}
                  </Button>
                </TableCell>
                <TableCell className="whitespace-normal">{p.description}</TableCell>
                <TableCell className="whitespace-normal font-medium">
                  {formatValue(p.type, p.currentValue, p.unit)}
                </TableCell>
                <TableCell className="whitespace-normal text-muted-foreground">
                  {formatValue(p.type, p.defaultValue, p.unit)}
                </TableCell>
                <TableCell className="tabular-nums">
                  {p.changedAt ? formatDate(p.changedAt) : <span className="text-muted-foreground">Chưa đổi</span>}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      <Sheet open={opened !== null} onOpenChange={(open) => !open && setOpened(null)}>
        <SheetContent className="sm:max-w-lg">
          {opened && <ParameterDetail key={opened} code={opened} canChange={canChange} onChanged={replaceRow} />}
        </SheetContent>
      </Sheet>
    </section>
  )
}
