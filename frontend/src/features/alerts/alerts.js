import { getResidentsById } from '@/api/api001'
import {
  getAlerts,
  getAlertsById,
  postAlertsByIdChangeLevel,
  postAlertsByIdClose,
  postAlertsByIdReceive,
  postAlertsByIdStartHandling,
} from '@/api/api007'
import { residentName } from './alertView'

// Đọc và ghi cảnh báo qua client sinh từ hợp đồng (api-007 alerts; api-001 Profile cho họ tên). Hồ sơ không đọc được
// thì ghi "Người cao tuổi", không chặn danh sách; danh sách lỗi thì ném lỗi cho màn.

export async function loadAlertBoard() {
  const { data } = await getAlerts({})
  const ids = [...new Set(data.map((a) => a.residentId).filter((id) => id != null))]
  const profiles = await Promise.allSettled(ids.map((id) => getResidentsById({ path: { id } })))
  const names = {}
  ids.forEach((id, i) => {
    if (profiles[i].status === 'fulfilled') names[id] = residentName(profiles[i].value.data)
  })
  return { alerts: data, names }
}

export async function loadAlert(id) {
  const { data } = await getAlertsById({ path: { id } })
  return data
}

const COMMANDS = {
  receive: postAlertsByIdReceive,
  'start-handling': postAlertsByIdStartHandling,
  close: postAlertsByIdClose,
  'change-level': postAlertsByIdChangeLevel,
}

export function runAlertCommand(id, command, body) {
  return COMMANDS[command]({
    path: { id },
    headers: { 'Idempotency-Key': crypto.randomUUID() },
    ...(body ? { body } : {}),
  })
}
