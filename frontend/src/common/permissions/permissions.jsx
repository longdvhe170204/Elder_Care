import { createContext, useContext, useEffect, useState } from 'react'
import { getMePermissions } from '@/api'

// 002 T026 (FR-042): ẩn nút, menu theo quyền hiện hành (GET /me/permissions) và theo allowedCommands của đối tượng.
// Chỉ để hiển thị; backend luôn kiểm lại ở lệnh (constitution XI). Chưa tải xong hoặc lỗi thì ẩn.

const PermissionsContext = createContext({ status: 'loading', keys: new Set() })

const keyOf = (fn, operation) => `${fn}:${operation}`

export function PermissionsProvider({ children }) {
  const [state, setState] = useState({ status: 'loading', keys: new Set() })

  useEffect(() => {
    let cancelled = false
    getMePermissions()
      .then(({ data }) => {
        if (!cancelled) setState({ status: 'ready', keys: new Set(data.map((p) => keyOf(p.function, p.operation))) })
      })
      .catch(() => {
        if (!cancelled) setState({ status: 'error', keys: new Set() })
      })
    return () => {
      cancelled = true
    }
  }, [])

  return <PermissionsContext.Provider value={state}>{children}</PermissionsContext.Provider>
}

// can('TASK_CHECKLIST', 'CREATE'): người dùng có khóa quyền này ở /me/permissions
// eslint-disable-next-line react-refresh/only-export-components
export function usePermissions() {
  const { status, keys } = useContext(PermissionsContext)
  return {
    status,
    can: (fn, operation) => keys.has(keyOf(fn, operation)),
    // Có ít nhất một thao tác của chức năng (để hiện mục menu)
    hasFunction: (fn) => [...keys].some((k) => k.startsWith(`${fn}:`)),
  }
}

// Lệnh có trong allowedCommands mà backend trả cùng đối tượng
// eslint-disable-next-line react-refresh/only-export-components
export function isAllowed(resource, command) {
  return (resource?.allowedCommands ?? []).includes(command)
}

// <IfAllowed function="X" operation="Y"> theo khóa quyền; <IfAllowed resource={obj} command="approve"> theo đối tượng
export function IfAllowed({ function: fn, operation, resource, command, children }) {
  const { can } = usePermissions()
  const shown = resource !== undefined ? isAllowed(resource, command) : can(fn, operation)
  return shown ? children : null
}
