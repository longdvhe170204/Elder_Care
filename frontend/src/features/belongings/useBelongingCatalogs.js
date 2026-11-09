import { useEffect, useState } from 'react'
import { getBelongingsConditionLevels, getBelongingsStorageLocations, getBelongingsTypes } from '@/api'

// Danh mục đồ gửi (013 FR-001, FR-001a, FR-002) để hiện tên loại, mức, vị trí và làm ô chọn của các form.
export function useBelongingCatalogs() {
  const [catalogs, setCatalogs] = useState({ status: 'loading', types: [], levels: [], locations: [] })

  useEffect(() => {
    let cancelled = false
    Promise.all([getBelongingsTypes(), getBelongingsConditionLevels(), getBelongingsStorageLocations()])
      .then(([types, levels, locations]) => {
        if (!cancelled)
          setCatalogs({ status: 'ready', types: types.data, levels: levels.data, locations: locations.data })
      })
      .catch(() => {
        if (!cancelled) setCatalogs({ status: 'error', types: [], levels: [], locations: [] })
      })
    return () => {
      cancelled = true
    }
  }, [])

  const nameOf = (list, id) => list.find((i) => i.id === id)?.name
  return {
    ...catalogs,
    typeOf: (id) => catalogs.types.find((t) => t.id === id),
    typeName: (id) => nameOf(catalogs.types, id) ?? `Loại #${id}`,
    levelName: (id) => nameOf(catalogs.levels, id),
    locationName: (id) => (id == null ? 'Không ghi vị trí' : (nameOf(catalogs.locations, id) ?? `Vị trí #${id}`)),
  }
}
