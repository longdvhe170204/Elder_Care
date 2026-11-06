import { useCallback, useEffect, useState } from 'react'
import { getRecipientConfirmations } from '@/api'
import { problemOf } from '@/common/problem'

// Xác nhận người nhận khác Hiệu lực và Chờ xác nhận của một người cao tuổi (013 FR-017; api-013 GET
// /recipient-confirmations). Chỉ tải khi form trả cần xác nhận; reload sau khi lập ngay.
export function useOpenConfirmations(residentId, enabled) {
  const [state, setState] = useState({ status: 'idle', items: [] })
  const load = useCallback(async () => {
    try {
      const { data } = await getRecipientConfirmations({
        query: { residentId, status: 'EFFECTIVE,PENDING_CONFIRMATION' },
      })
      setState({ status: 'ready', items: data })
    } catch (error) {
      setState({ status: 'error', items: [], problem: problemOf(error) })
    }
  }, [residentId])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải khi người nhận cần xác nhận người nhận khác
    if (enabled) load()
  }, [enabled, load])

  return { ...state, reload: load }
}
