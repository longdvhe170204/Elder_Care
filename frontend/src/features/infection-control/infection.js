import { getResidentsById } from '@/api/api001'
import { getAreas, getFloors } from '@/api/api003'
import {
  getIncidentTypes,
  getIncidents,
  getIncidentsByIdContactList,
  getIncidentsByIdContactLookup,
  getIsolations,
  postContactListsByIdConfirm,
  postContactListsByIdContacts,
  postContactListsByIdContactsByContactIdExclude,
  postContactListsByIdContactsByContactIdRemove,
  postIncidentsByIdContactList,
  postIsolations,
  postIsolationsByIdLift,
} from '@/api/api007'

// Vùng khoanh, danh sách tiếp xúc qua client sinh từ hợp đồng (api-007; tên tầng, khu vực từ api-003; họ tên từ api-001).
// Sự cố lây nhiễm là sự cố đang mở có loại hiện hành mã SUSPECTED_INFECTION (danh mục api-007).

const OPEN = ['NEW', 'IN_PROGRESS', 'MONITORING']
const key = () => ({ 'Idempotency-Key': crypto.randomUUID() })

export async function loadInfectionBoard() {
  const zones = await getIsolations({})
  const [floors, areas, incidents, types] = await Promise.all([
    getFloors({}).catch(() => ({ data: [] })),
    getAreas({}).catch(() => ({ data: [] })),
    getIncidents({}).catch(() => ({ data: [] })),
    getIncidentTypes({}).catch(() => ({ data: [] })),
  ])
  const infectionTypeIds = (types.data ?? []).filter((t) => t.code === 'SUSPECTED_INFECTION').map((t) => t.id)
  const infections = (incidents.data ?? []).filter(
    (i) => OPEN.includes(i.status) && infectionTypeIds.includes(i.currentTypeId),
  )
  const ids = [...new Set(infections.map((i) => i.content?.residentId).filter((id) => id != null))]
  const profiles = await Promise.allSettled(ids.map((id) => getResidentsById({ path: { id } })))
  const names = {}
  ids.forEach((id, i) => {
    if (profiles[i].status === 'fulfilled')
      names[id] = profiles[i].value.data?.personalInfo?.fullName ?? 'Người cao tuổi'
  })
  return { zones: zones.data, floors: floors.data ?? [], areas: areas.data ?? [], infections, names }
}

// Chưa lập danh sách thì backend trả 404: màn hiện nút "Lập danh sách tiếp xúc"
export async function loadContactList(incidentId) {
  try {
    const { data } = await getIncidentsByIdContactList({ path: { id: incidentId } })
    return data
  } catch (error) {
    if (error?.status === 404) return null
    throw error
  }
}

export const createContactList = (incidentId) =>
  postIncidentsByIdContactList({ path: { id: incidentId }, headers: key() })
export const lookupContacts = async (incidentId) =>
  (await getIncidentsByIdContactLookup({ path: { id: incidentId } })).data
export const addContact = (listId, body) => postContactListsByIdContacts({ path: { id: listId }, headers: key(), body })
export const removeContact = (listId, contactId) =>
  postContactListsByIdContactsByContactIdRemove({ path: { id: listId, contactId }, headers: key() })
export const excludeContact = (listId, contactId, reason) =>
  postContactListsByIdContactsByContactIdExclude({ path: { id: listId, contactId }, headers: key(), body: { reason } })
export const confirmContactList = (listId, body) =>
  postContactListsByIdConfirm({ path: { id: listId }, headers: key(), ...(body ? { body } : {}) })
export const createIsolation = (body) => postIsolations({ headers: key(), body })
export const liftIsolation = (id, reason) => postIsolationsByIdLift({ path: { id }, headers: key(), body: { reason } })
