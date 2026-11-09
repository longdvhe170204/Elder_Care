import { getResidents, getResidentsById } from '@/api/api001'
import {
  getIncidentTypes,
  getIncidents,
  getIncidentsById,
  getProgressionTypes,
  postIncidents,
  postIncidentsByIdChangeLevel,
  postIncidentsByIdClose,
  postIncidentsByIdJoinHandling,
  postIncidentsByIdMoveToMonitoring,
  postIncidentsByIdProgressions,
  postIncidentsByIdReceive,
  postIncidentsByIdReopen,
} from '@/api/api007'
import { residentName } from './incidentView'

// Đọc và ghi sự cố qua client sinh từ hợp đồng (api-007 incidents, incident-types, progression-types; api-001 Profile,
// danh sách người cao tuổi). Danh mục hoặc hồ sơ không đọc được thì màn ghi tên chung, không chặn danh sách.

export async function loadIncidentBoard() {
  const [incidents, types] = await Promise.all([getIncidents({}), getIncidentTypes({}).catch(() => ({ data: [] }))])
  const ids = [...new Set(incidents.data.map((i) => i.content?.residentId).filter((id) => id != null))]
  const profiles = await Promise.allSettled(ids.map((id) => getResidentsById({ path: { id } })))
  const names = {}
  ids.forEach((id, i) => {
    if (profiles[i].status === 'fulfilled') names[id] = residentName(profiles[i].value.data)
  })
  return { incidents: incidents.data, types: types.data ?? [], names }
}

export async function loadIncident(id) {
  const [incident, progressionTypes] = await Promise.all([
    getIncidentsById({ path: { id } }),
    getProgressionTypes({}).catch(() => ({ data: [] })),
  ])
  return { incident: incident.data, progressionTypes: progressionTypes.data ?? [] }
}

export async function loadResidentChoices() {
  const { data } = await getResidents({})
  return (data.items ?? []).map((r) => ({ id: r.id, code: r.profileCode, name: residentName(r) }))
}

const key = () => ({ 'Idempotency-Key': crypto.randomUUID() })

const COMMANDS = {
  receive: postIncidentsByIdReceive,
  'join-handling': postIncidentsByIdJoinHandling,
  progressions: postIncidentsByIdProgressions,
  'change-level': postIncidentsByIdChangeLevel,
  'move-to-monitoring': postIncidentsByIdMoveToMonitoring,
  reopen: postIncidentsByIdReopen,
  close: postIncidentsByIdClose,
}

export function runIncidentCommand(id, command, body) {
  return COMMANDS[command]({ path: { id }, headers: key(), ...(body ? { body } : {}) })
}

export async function recordIncident(body) {
  const { data } = await postIncidents({ headers: key(), body })
  return data
}
