import { useCallback, useEffect, useState } from 'react'
import { getMePermissions, getBeds } from '@/api'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Sheet, SheetContent } from '@/components/ui/sheet'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { StructureForm } from './StructureForm'
import { BedTransfersPanel } from './BedTransfersPanel'
import { BedAllocationsPanel } from './BedAllocationsPanel'
import { LocationHistoryPanel } from './LocationHistoryPanel'
import { DayCarePanel } from './DayCarePanel'
import { catalogs, bedStates } from './bedCatalog'

async function fetchCatalogs() {
  const { data: permissions } = await getMePermissions()
  if (
    !permissions.some(
      (permission) => permission.function === 'ROOM_BED_CONFIGURATION' && permission.operation === 'VIEW',
    )
  )
    return { data: {}, permissions }
  const entries = await Promise.all(
    Object.entries(catalogs)
      .filter(([kind]) => kind !== 'beds')
      .map(async ([kind, config]) => [kind, (await config.list()).data]),
  )
  const { data: board } = await getBeds()
  return { data: { ...Object.fromEntries(entries), beds: board.beds }, permissions }
}

// T018: cơ cấu bốn cấp và hai danh mục; quyền thao tác lấy từ backend, không suy từ vai trò.
export function BedsPage() {
  const [state, setState] = useState({ status: 'loading' })
  const [tab, setTab] = useState('structure')
  const [opened, setOpened] = useState(null)
  const [notice, setNotice] = useState(null)
  const load = useCallback(async () => {
    try {
      setState({ status: 'ready', ...(await fetchCatalogs()) })
    } catch (cause) {
      setState({ status: 'error', problem: problemOf(cause) })
    }
  }, [])
  useEffect(() => {
    let current = true
    fetchCatalogs()
      .then((result) => {
        if (current) setState({ status: 'ready', ...result })
      })
      .catch((cause) => {
        if (current) setState({ status: 'error', problem: problemOf(cause) })
      })
    return () => {
      current = false
    }
  }, [])
  if (state.status === 'loading') return <p aria-busy="true">Đang tải phòng, giường…</p>
  if (state.status === 'error')
    return state.problem.status === 403 ? (
      <PermissionDeniedState label="phòng, giường" />
    ) : (
      <ProblemAlert problem={state.problem}>
        <Button variant="outline" onClick={load}>
          Thử lại
        </Button>
      </ProblemAlert>
    )
  const { data, permissions } = state
  const allowed = (operation) =>
    permissions.some(
      (permission) => permission.function === 'ROOM_BED_CONFIGURATION' && permission.operation === operation,
    )
  const allocationAllowed = (operation) =>
    permissions.some((permission) => permission.function === 'BED_ALLOCATION' && permission.operation === operation)
  const historyAllowed =
    allocationAllowed('VIEW') ||
    permissions.some(
      (permission) => permission.function === 'INFECTION_CONTAINMENT' && permission.operation === 'CREATE',
    )
  if (!allowed('VIEW') && !allocationAllowed('VIEW') && !historyAllowed)
    return <PermissionDeniedState label="phòng, giường" />
  const activeTab = allowed('VIEW')
    ? tab
    : !allocationAllowed('VIEW') || tab === 'history'
      ? 'history'
      : tab === 'allocations'
        ? 'allocations'
        : 'transfers'
  const canCreate = allowed('CREATE'),
    canUpdate = allowed('UPDATE')
  function edit(kind, item, action = 'update') {
    setOpened({ kind, id: item.id, action })
  }
  function create(kind, defaults = {}) {
    setOpened({ kind, action: 'create', defaults })
  }
  function controls(kind, item) {
    const name = item.name ?? item.code
    return (
      canUpdate && (
        <div className="flex flex-wrap gap-2">
          <Button size="sm" variant="outline" aria-label={`Sửa ${name}`} onClick={() => edit(kind, item)}>
            Sửa
          </Button>
          {kind !== 'beds' && (
            <>
              {item.status === 'ACTIVE' ? (
                <Button
                  size="sm"
                  variant="outline"
                  disabled={item.fixed}
                  aria-label={`Ngừng hiệu lực ${name}`}
                  onClick={() => edit(kind, item, 'deactivate')}
                >
                  Ngừng hiệu lực
                </Button>
              ) : (
                <Button
                  size="sm"
                  variant="outline"
                  aria-label={`Kích hoạt lại ${name}`}
                  onClick={() => edit(kind, item, 'reactivate')}
                >
                  Kích hoạt lại
                </Button>
              )}
              <Button
                size="sm"
                variant="ghost"
                disabled={item.fixed}
                aria-label={`Xóa ${name}`}
                onClick={() => edit(kind, item, 'remove')}
              >
                Xóa
              </Button>
            </>
          )}
        </div>
      )
    )
  }
  function status(item) {
    return <Badge variant="secondary">{item.status === 'ACTIVE' ? 'Hiệu lực' : 'Ngừng hiệu lực'}</Badge>
  }
  async function saved(warnings) {
    setOpened(null)
    setNotice(warnings.length ? warnings.map((warning) => warning.description).join(' ') : 'Đã lưu thay đổi.')
    await load()
  }
  return (
    <div className="mx-auto flex w-full max-w-[1600px] flex-col gap-6">
      <PageHeader title="Phòng, giường" subtitle="Cơ cấu khu vực, tầng, phòng và giường của viện.">
        <div role="tablist" aria-label="Nội dung phòng, giường" className="flex flex-wrap gap-2">
          {[
            ...(allowed('VIEW')
              ? [
                  ['structure', 'Cơ cấu'],
                  ['room-types', 'Loại phòng'],
                  ['bed-transfer-reasons', 'Lý do chuyển'],
                  ['day-care', 'Bán trú'],
                ]
              : []),
            ...(allocationAllowed('VIEW')
              ? [
                  ['allocations', 'Phân bổ giường'],
                  ['transfers', 'Chuyển giường'],
                ]
              : []),
            ...(historyAllowed ? [['history', 'Lịch sử vị trí']] : []),
          ].map(([key, label]) => (
            <Button
              key={key}
              role="tab"
              aria-selected={activeTab === key}
              variant={activeTab === key ? 'secondary' : 'outline'}
              onClick={() => setTab(key)}
            >
              {label}
            </Button>
          ))}
        </div>
      </PageHeader>
      {notice && (
        <Alert>
          <AlertTitle>Kết quả thao tác</AlertTitle>
          <AlertDescription>{notice}</AlertDescription>
        </Alert>
      )}
      {activeTab === 'day-care' ? (
        <DayCarePanel catalogs={data} canCreate={canCreate} />
      ) : activeTab === 'history' ? (
        <LocationHistoryPanel catalogs={data} canCorrectAllocation={allocationAllowed('CREATE')} />
      ) : activeTab === 'allocations' ? (
        <BedAllocationsPanel canCreate={allocationAllowed('CREATE')} />
      ) : activeTab === 'transfers' ? (
        <BedTransfersPanel canTransfer={allocationAllowed('CREATE')} />
      ) : tab === 'structure' ? (
        <section className="flex flex-col gap-4">
          {canCreate && (
            <Button className="self-start" onClick={() => create('areas')}>
              Thêm khu vực
            </Button>
          )}
          {!data.areas.length && (
            <p className="rounded-xl border bg-card p-6 text-muted-foreground">
              Chưa có khu vực. Quản lý viện có thể thêm cơ cấu đầu tiên.
            </p>
          )}
          {data.areas.map((area) => (
            <section key={area.id} aria-label={area.name} className="rounded-2xl border bg-card p-4">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <h2 className="type-title">
                  {area.name} <span className="type-body-sm text-muted-foreground">({area.code})</span>
                </h2>
                {status(area)}
                {controls('areas', area)}
                {canCreate && (
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={area.status !== 'ACTIVE'}
                    onClick={() => create('floors', { areaId: area.id })}
                  >
                    Thêm tầng
                  </Button>
                )}
              </div>
              <div className="mt-4 flex flex-col gap-4 border-l pl-4">
                {data.floors
                  .filter((floor) => floor.areaId === area.id)
                  .map((floor) => (
                    <section key={floor.id}>
                      <div className="flex flex-wrap items-center gap-3">
                        <h3 className="type-title-sm">{floor.name}</h3>
                        {status(floor)}
                        {controls('floors', floor)}
                        {canCreate && (
                          <Button
                            variant="outline"
                            size="sm"
                            disabled={floor.status !== 'ACTIVE'}
                            onClick={() => create('rooms', { floorId: floor.id })}
                          >
                            Thêm phòng
                          </Button>
                        )}
                      </div>
                      <div className="mt-3 grid gap-3 lg:grid-cols-2">
                        {data.rooms
                          .filter((room) => room.floorId === floor.id)
                          .map((room) => (
                            <section key={room.id} className="rounded-xl border p-4">
                              <div className="flex flex-wrap items-center gap-3">
                                <h4 className="font-medium">{room.name}</h4>
                                {status(room)}
                                {controls('rooms', room)}
                              </div>
                              <p className="mt-2 text-muted-foreground">
                                {data['room-types'].find((type) => type.id === room.roomTypeId)?.name} ·{' '}
                                {{ MALE: 'Nam', FEMALE: 'Nữ', UNRESTRICTED: 'Không giới hạn' }[room.genderPolicy]}
                              </p>
                              <ul className="my-3 flex flex-col gap-2">
                                {data.beds
                                  .filter((bed) => bed.location.roomId === room.id)
                                  .map((bed) => (
                                    <li
                                      key={bed.id}
                                      className="flex flex-wrap items-center justify-between gap-2 rounded-lg bg-muted p-3"
                                    >
                                      <div>
                                        <p className="font-medium">{bed.location.label}</p>
                                        <p className="type-caption text-muted-foreground">
                                          {bedStates[bed.state] ?? bed.state}
                                        </p>
                                      </div>
                                      {controls('beds', bed)}
                                    </li>
                                  ))}
                              </ul>
                              {canCreate && (
                                <Button
                                  size="sm"
                                  variant="outline"
                                  disabled={room.status !== 'ACTIVE'}
                                  aria-label={`Thêm giường ${room.name}`}
                                  onClick={() => create('beds', { roomId: room.id })}
                                >
                                  Thêm giường
                                </Button>
                              )}
                            </section>
                          ))}
                      </div>
                    </section>
                  ))}
              </div>
            </section>
          ))}
        </section>
      ) : (
        <section className="flex flex-col gap-3 rounded-2xl border bg-card p-4">
          {canCreate && (
            <Button className="self-start" onClick={() => create(tab)}>
              Thêm {catalogs[tab].label}
            </Button>
          )}
          {!data[tab].length && <p className="text-muted-foreground">Chưa có dữ liệu.</p>}
          {data[tab].map((item) => (
            <div key={item.id} className="flex flex-wrap items-center justify-between gap-3 rounded-xl border p-4">
              <div>
                <h2 className="font-medium">{item.name}</h2>
                <p className="type-caption text-muted-foreground">
                  {tab === 'room-types'
                    ? `Số giường tiêu chuẩn: ${item.standardBeds}`
                    : `${item.code}${item.fixed ? ' · Cố định' : ''}`}
                </p>
              </div>
              {status(item)}
              {controls(tab, item)}
            </div>
          ))}
        </section>
      )}
      <Sheet open={Boolean(opened)} onOpenChange={(value) => !value && setOpened(null)}>
        <SheetContent className="flex flex-col gap-4">
          {opened && (
            <StructureForm
              key={`${opened.kind}:${opened.id ?? 'new'}:${opened.action}`}
              target={opened}
              data={data}
              onSaved={saved}
              onClose={() => setOpened(null)}
            />
          )}
        </SheetContent>
      </Sheet>
    </div>
  )
}
