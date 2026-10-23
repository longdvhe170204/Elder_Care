import { useCallback, useEffect, useState } from 'react'
import { ApiError, getCatalogsByType } from '@/api'
import { Alert, AlertAction, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Field, FieldLabel } from '@/components/ui/field'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { PermissionDeniedState } from '@/common/PageState'
import { StatusBadge } from '@/common/StatusBadge'
import { ALLERGEN_GROUPS, CATALOG_TYPES, STATUS_LABELS } from './catalogLabels'
import { CatalogItemForm } from './CatalogItemForm'
import { CatalogReasonForm } from './CatalogReasonForm'

// Danh mục của M01 (001 T072; FR-018, FR-018a, BR-M01-11, BR-M15-06; hợp đồng /catalogs/{type}).
// Mọi vai trò có quyền xem thấy danh sách; chỉ Quản lý viện đổi (ma trận HEALTH_CATALOGS): canChange chỉ để ẩn nút,
// backend vẫn kiểm. Quyền cấu hình danh mục chưa có dòng riêng ở nghiep-vu.md (UC-69 chỉ nêu tham số), xem plan 001.

const SELECT = 'h-10 w-full rounded-md border border-input bg-transparent px-3'

export function CatalogList({ canChange }) {
  const [type, setType] = useState('allergens')
  const [status, setStatus] = useState('')
  const [group, setGroup] = useState('')
  const [state, setState] = useState({ status: 'loading', items: [] })
  const [panel, setPanel] = useState(null)

  const load = useCallback(async () => {
    setState({ status: 'loading', items: [] })
    try {
      const { data } = await getCatalogsByType({
        path: { type },
        query: { status: status || undefined, group: type === 'allergens' ? group || undefined : undefined },
      })
      setState({ status: 'ready', items: data })
    } catch (error) {
      setState({ status: error instanceof ApiError && error.status === 403 ? 'denied' : 'error', items: [] })
    }
  }, [type, status, group])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải danh sách khi đổi loại hoặc bộ lọc
    load()
  }, [load])

  function changed() {
    setPanel(null)
    load()
  }

  if (state.status === 'denied') return <PermissionDeniedState label="danh mục" />

  const isAllergen = type === 'allergens'
  const isSeverity = type === 'allergy-severities'

  return (
    <section className="flex flex-col gap-3 rounded-2xl border bg-card p-4 md:p-5">
      <ToggleGroup
        variant="outline"
        value={[type]}
        onValueChange={(next) => {
          if (!next[0]) return
          setType(next[0])
          setGroup('')
          setPanel(null)
        }}
        className="flex-wrap self-start"
      >
        {CATALOG_TYPES.map((t) => (
          <ToggleGroupItem key={t.value} value={t.value} className="h-10 px-4">
            {t.label}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>

      <div className="flex flex-wrap items-end gap-3">
        <Field className="w-48">
          <FieldLabel htmlFor="catalog-status">Trạng thái</FieldLabel>
          <select id="catalog-status" value={status} onChange={(e) => setStatus(e.target.value)} className={SELECT}>
            <option value="">Tất cả</option>
            {Object.entries(STATUS_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </Field>
        {isAllergen && (
          <Field className="w-48">
            <FieldLabel htmlFor="catalog-group-filter">Nhóm dị nguyên</FieldLabel>
            <select id="catalog-group-filter" value={group} onChange={(e) => setGroup(e.target.value)} className={SELECT}>
              <option value="">Tất cả</option>
              {Object.entries(ALLERGEN_GROUPS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </Field>
        )}
        {canChange && (
          <Button className="ml-auto h-10" onClick={() => setPanel({ kind: 'create' })}>
            Thêm mục
          </Button>
        )}
      </div>

      {state.status === 'loading' && (
        <div aria-busy="true" className="flex flex-col gap-2">
          <span className="sr-only">Đang tải danh mục</span>
          <Skeleton className="h-10" />
          <Skeleton className="h-10" />
          <Skeleton className="h-10" />
        </div>
      )}
      {state.status === 'error' && (
        <Alert role="alert">
          <AlertTitle>Không tải được danh mục</AlertTitle>
          <AlertAction>
            <Button variant="outline" size="sm" onClick={load}>
              Thử lại
            </Button>
          </AlertAction>
        </Alert>
      )}
      {state.status === 'ready' && state.items.length === 0 && (
        <Empty>
          <EmptyHeader>
            <EmptyTitle>Chưa có mục nào</EmptyTitle>
            <EmptyDescription>Đổi bộ lọc hoặc thêm mục mới.</EmptyDescription>
          </EmptyHeader>
        </Empty>
      )}
      {state.status === 'ready' && state.items.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Tên</TableHead>
              {isAllergen && <TableHead>Nhóm</TableHead>}
              {isSeverity && <TableHead>Thứ tự</TableHead>}
              <TableHead>Trạng thái</TableHead>
              {canChange && <TableHead>Thao tác</TableHead>}
            </TableRow>
          </TableHeader>
          <TableBody>
            {state.items.map((item) => (
              <TableRow key={item.id}>
                <TableCell className="font-medium">{item.name}</TableCell>
                {isAllergen && <TableCell>{ALLERGEN_GROUPS[item.group] ?? item.group}</TableCell>}
                {isSeverity && <TableCell className="tabular-nums">{item.order}</TableCell>}
                <TableCell>
                  <StatusBadge status={STATUS_LABELS[item.status] ?? item.status} />
                </TableCell>
                {canChange && (
                  <TableCell>
                    <div className="flex flex-wrap gap-2">
                      <Button variant="outline" size="sm" onClick={() => setPanel({ kind: 'edit', item })}>
                        Sửa
                      </Button>
                      <Button variant="outline" size="sm" onClick={() => setPanel({ kind: 'reason', item })}>
                        {item.status === 'ACTIVE' ? 'Ngừng hiệu lực' : 'Kích hoạt lại'}
                      </Button>
                    </div>
                  </TableCell>
                )}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      {canChange && panel?.kind === 'create' && (
        <CatalogItemForm key="create" type={type} onSaved={changed} onCancel={() => setPanel(null)} onStale={changed} />
      )}
      {canChange && panel?.kind === 'edit' && (
        <CatalogItemForm
          key={`edit-${panel.item.id}`}
          type={type}
          item={panel.item}
          onSaved={changed}
          onCancel={() => setPanel(null)}
          onStale={changed}
        />
      )}
      {canChange && panel?.kind === 'reason' && (
        <CatalogReasonForm
          key={`reason-${panel.item.id}`}
          type={type}
          item={panel.item}
          onDone={changed}
          onCancel={() => setPanel(null)}
        />
      )}
    </section>
  )
}
