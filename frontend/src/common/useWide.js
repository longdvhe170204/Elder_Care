import { useEffect, useState } from 'react'

const WIDE = '(min-width: 1280px)'

// Màn rộng từ 1280px: khung chi tiết cố định bên phải (DESIGN.md mục 5, 10)
export function useWide() {
  const [wide, setWide] = useState(() => typeof window !== 'undefined' && !!window.matchMedia?.(WIDE).matches)
  useEffect(() => {
    const query = window.matchMedia?.(WIDE)
    if (!query) return undefined
    const update = () => setWide(query.matches)
    query.addEventListener('change', update)
    return () => query.removeEventListener('change', update)
  }, [])
  return wide
}
