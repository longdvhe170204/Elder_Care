import { useCallback, useEffect, useState } from 'react'
import { getAccountsById } from '@/api'
import { Button } from '@/components/ui/button'
import { SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { problemOf } from '@/common/problem'
import { formatDateTime } from '@/common/dateTime'
import { AccountCommandForm } from './AccountCommandForm'
import { COMMANDS, LOCK_CAUSE_LABELS, STATUS_LABELS, TYPE_LABELS, profileLabel, roleNames } from './accountLabels'

// Khung chi tiết tài khoản (002 T035; detail-panel DESIGN.md 7.2; FR-042): thông tin, rồi nút lệnh lấy đúng từ
// allowedCommands backend trả; mỗi lệnh mở form ngay trong khung.

export function AccountDetail({ id, onChanged }) {
  const [state, setState] = useState({ status: 'loading' })
  const [command, setCommand] = useState(null)

  const load = useCallback(async () => {
    setState({ status: 'loading' })
    try {
      const { data, etag } = await getAccountsById({ path: { id } })
      setState({ status: 'ready', account: data, etag })
      onChanged(data)
    } catch (error) {
      setState({ status: 'error', problem: problemOf(error) })
    }
  }, [id, onChanged])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải chi tiết (kèm ETag) khi mở khung
    load()
  }, [load])

  if (state.status === 'loading') {
    return (
      <div aria-busy="true" className="flex flex-col gap-2 px-4 pt-14">
        <span className="sr-only">Đang tải tài khoản</span>
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

  const { account, etag } = state
  const allowed = (account.allowedCommands ?? []).filter((c) => COMMANDS[c])

  // Lệnh xong thì tải lại (ETag, allowedCommands mới); cấp lại mật khẩu giữ form để hiện mật khẩu tạm
  function done() {
    if (command === 'reset-password') return
    setCommand(null)
    load()
  }

  return (
    <>
      <SheetHeader className="gap-1 pt-6">
        <SheetTitle className="type-title">{profileLabel(account)}</SheetTitle>
        <SheetDescription>Tên đăng nhập: {account.username}</SheetDescription>
      </SheetHeader>
      <div className="flex flex-col gap-5 overflow-y-auto px-4 pb-6 type-body-sm">
        <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2">
          <dt className="text-muted-foreground">Trạng thái</dt>
          <dd className="flex flex-wrap items-center gap-2">
            <StatusBadge status={STATUS_LABELS[account.status] ?? account.status} />
            {account.lockCause && <span>{LOCK_CAUSE_LABELS[account.lockCause] ?? account.lockCause}</span>}
            {account.lockedUntil && <span className="tabular-nums">tới {formatDateTime(account.lockedUntil)}</span>}
          </dd>
          <dt className="text-muted-foreground">Loại</dt>
          <dd>{TYPE_LABELS[account.type] ?? account.type}</dd>
          <dt className="text-muted-foreground">Vai trò</dt>
          <dd>{account.roles?.length ? roleNames(account.roles) : 'Chưa có vai trò'}</dd>
          <dt className="text-muted-foreground">Đăng nhập gần nhất</dt>
          <dd className="tabular-nums">
            {account.lastLoginAt ? formatDateTime(account.lastLoginAt) : 'Chưa đăng nhập'}
          </dd>
        </dl>

        {command ? (
          <AccountCommandForm
            key={command}
            command={command}
            account={account}
            etag={etag}
            onDone={done}
            onCancel={() => setCommand(null)}
            onStale={() => {
              setCommand(null)
              load()
            }}
          />
        ) : (
          allowed.length > 0 && (
            <div className="flex flex-wrap gap-2">
              {allowed.map((c) => (
                <Button key={c} variant="outline" onClick={() => setCommand(c)}>
                  {COMMANDS[c].label}
                </Button>
              ))}
            </div>
          )
        )}
      </div>
    </>
  )
}
