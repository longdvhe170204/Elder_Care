import { useEffect, useState } from 'react'
import { problemOf } from '@/common/problem'

// Tải một danh sách; `reload` gọi lại sau lệnh. Lỗi giữ ở `failure` (403 thì màn hiện PermissionDeniedState).
export function useLoad(load, deps) {
  const [data, setData] = useState(null)
  const [failure, setFailure] = useState(null)
  const [tick, setTick] = useState(0)
  useEffect(() => {
    let active = true
    load()
      .then((value) => {
        if (!active) return
        setData(value)
        setFailure(null)
      })
      .catch((e) => {
        if (!active) return
        setData(null)
        setFailure(problemOf(e))
      })
    return () => {
      active = false
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick])
  return { data, failure, reload: () => setTick((t) => t + 1) }
}

// Gửi một lệnh ghi: mỗi lần một Idempotency-Key mới; lỗi giữ ở `failure` cho ProblemAlert (DESIGN.md 7.4)
export function useCommand() {
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)
  async function run(call, params = {}) {
    setBusy(true)
    setFailure(null)
    try {
      const result = await call({ ...params, headers: { 'Idempotency-Key': crypto.randomUUID(), ...params.headers } })
      return result ?? {}
    } catch (e) {
      setFailure(problemOf(e))
      return null
    } finally {
      setBusy(false)
    }
  }
  return { busy, failure, setFailure, run }
}
