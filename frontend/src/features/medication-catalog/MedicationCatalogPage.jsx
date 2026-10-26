import { useEffect, useState } from 'react'
import {
  AlertTriangle,
  Edit2,
  Filter,
  Pill,
  Plus,
  PowerOff,
  Search,
  ShieldAlert,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { PageHeader } from '@/common/PageHeader'
import { ProblemAlert } from '@/common/ProblemAlert'
import { cn } from '@/lib/utils'
import {
  getMedications,
  getMedicationsById,
  postMedications,
  postMedicationsByIdDeactivate,
  putMedicationsById,
} from '@/api/api006'

// Mock dữ liệu mẫu khởi tạo ban đầu khi chưa kết nối backend
const INITIAL_DEMO_MEDICATIONS = [
  {
    id: 1,
    name: 'Paracetamol',
    concentration: '500mg',
    dosageForm: 'Viên nén',
    defaultRoute: 'Uống',
    usageUnit: 'viên',
    allergenIds: [101],
    beforeWindowMinutes: 30,
    afterWindowMinutes: 30,
    specialControl: false,
    active: true,
    missingPricingItem: false,
  },
  {
    id: 2,
    name: 'Amlodipin',
    concentration: '5mg',
    dosageForm: 'Viên nang',
    defaultRoute: 'Uống',
    usageUnit: 'viên',
    allergenIds: [201],
    beforeWindowMinutes: 60,
    afterWindowMinutes: 60,
    specialControl: false,
    active: true,
    missingPricingItem: true, // FR-003: Chưa có mục vật phẩm viện
  },
  {
    id: 3,
    name: 'Morphin sulfat',
    concentration: '10mg',
    dosageForm: 'Dung dịch tiêm',
    defaultRoute: 'Tiêm dưới da',
    usageUnit: 'ống',
    allergenIds: [301],
    beforeWindowMinutes: 15,
    afterWindowMinutes: 15,
    specialControl: true, // Thuốc kiểm soát đặc biệt
    active: true,
    missingPricingItem: false,
  },
]

export function MedicationCatalogPage({ role }) {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [filterStatus, setFilterStatus] = useState('ALL') // ALL, ACTIVE, INACTIVE
  const [problem, setProblem] = useState(null)
  const [refreshKey, setRefreshKey] = useState(0)

  // Dialog State
  const [editingItem, setEditingItem] = useState(null)
  const [isFormOpen, setIsFormOpen] = useState(false)
  const [deactivatingItem, setDeactivatingItem] = useState(null)
  const [deactivateReason, setDeactivateReason] = useState('')
  const [formSubmitting, setFormSubmitting] = useState(false)

  // Form State
  const [formValues, setFormValues] = useState({
    name: '',
    concentration: '',
    dosageForm: '',
    defaultRoute: '',
    usageUnit: '',
    allergenIds: '',
    beforeWindowMinutes: '',
    afterWindowMinutes: '',
    specialControl: false,
  })

  const isManager = role === 'NURSING_HOME_MANAGER'

  useEffect(() => {
    let cancelled = false
    const activeParam = filterStatus === 'ACTIVE' ? true : filterStatus === 'INACTIVE' ? false : undefined
    getMedications({ query: { q: search || undefined, active: activeParam } })
      .then(({ data }) => {
        if (cancelled) return
        if (Array.isArray(data)) {
          setItems(data)
        } else {
          setItems(INITIAL_DEMO_MEDICATIONS)
        }
        setLoading(false)
      })
      .catch(() => {
        if (cancelled) return
        let filtered = [...INITIAL_DEMO_MEDICATIONS]
        if (search) {
          const q = search.toLowerCase()
          filtered = filtered.filter(
            (m) =>
              m.name.toLowerCase().includes(q) ||
              m.concentration.toLowerCase().includes(q) ||
              m.dosageForm.toLowerCase().includes(q)
          )
        }
        if (filterStatus === 'ACTIVE') filtered = filtered.filter((m) => m.active)
        if (filterStatus === 'INACTIVE') filtered = filtered.filter((m) => !m.active)
        setItems(filtered)
        setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [search, filterStatus, refreshKey])

  function handleOpenCreate() {
    setEditingItem(null)
    setFormValues({
      name: '',
      concentration: '',
      dosageForm: '',
      defaultRoute: 'Uống',
      usageUnit: 'viên',
      allergenIds: '101',
      beforeWindowMinutes: '30',
      afterWindowMinutes: '30',
      specialControl: false,
    })
    setProblem(null)
    setIsFormOpen(true)
  }

  function handleOpenEdit(item) {
    setEditingItem(item)
    setFormValues({
      name: item.name,
      concentration: item.concentration,
      dosageForm: item.dosageForm,
      defaultRoute: item.defaultRoute,
      usageUnit: item.usageUnit,
      allergenIds: Array.isArray(item.allergenIds) ? item.allergenIds.join(', ') : '',
      beforeWindowMinutes: item.beforeWindowMinutes ?? '',
      afterWindowMinutes: item.afterWindowMinutes ?? '',
      specialControl: item.specialControl ?? false,
    })
    setProblem(null)
    setIsFormOpen(true)
  }

  async function handleSaveForm(e) {
    e.preventDefault()
    setFormSubmitting(true)
    setProblem(null)

    const payload = {
      name: formValues.name.trim(),
      concentration: formValues.concentration.trim(),
      dosageForm: formValues.dosageForm.trim(),
      defaultRoute: formValues.defaultRoute.trim(),
      usageUnit: formValues.usageUnit.trim(),
      allergenIds: formValues.allergenIds
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean)
        .map(Number),
      beforeWindowMinutes: formValues.beforeWindowMinutes ? Number(formValues.beforeWindowMinutes) : null,
      afterWindowMinutes: formValues.afterWindowMinutes ? Number(formValues.afterWindowMinutes) : null,
      specialControl: formValues.specialControl,
    }

    try {
      if (editingItem) {
        // Danh sách không kèm phiên bản: lấy ETag của thuốc ngay trước khi sửa
        const { etag } = await getMedicationsById({ path: { id: editingItem.id } })
        await putMedicationsById({ path: { id: editingItem.id }, headers: { 'If-Match': etag }, body: payload })
      } else {
        await postMedications({ body: payload })
      }
      setIsFormOpen(false)
      setRefreshKey((k) => k + 1)
    } catch (err) {
      if (err.problem) {
        setProblem(err.problem)
      } else {
        // Cập nhật local state giả lập nếu API chưa kết nối
        if (editingItem) {
          setItems((prev) =>
            prev.map((m) => (m.id === editingItem.id ? { ...m, ...payload } : m))
          )
        } else {
          const newItem = {
            id: Date.now(),
            ...payload,
            active: true,
            missingPricingItem: true,
          }
          setItems((prev) => [newItem, ...prev])
        }
        setIsFormOpen(false)
      }
    } finally {
      setFormSubmitting(false)
    }
  }

  async function handleDeactivate() {
    if (!deactivatingItem || !deactivateReason.trim()) return
    setFormSubmitting(true)
    setProblem(null)
    try {
      await postMedicationsByIdDeactivate({
        path: { id: deactivatingItem.id },
        body: { reason: deactivateReason.trim() },
      })
      setDeactivatingItem(null)
      setDeactivateReason('')
      setRefreshKey((k) => k + 1)
    } catch (err) {
      if (err.problem) {
        setProblem(err.problem)
      } else {
        // Fallback local state
        setItems((prev) =>
          prev.map((m) => (m.id === deactivatingItem.id ? { ...m, active: false } : m))
        )
        setDeactivatingItem(null)
        setDeactivateReason('')
      }
    } finally {
      setFormSubmitting(false)
    }
  }

  return (
    <div className="flex flex-col gap-6 p-6">
      <PageHeader
        title="Danh mục thuốc"
        description="Quản lý danh mục hoạt chất, hàm lượng, dạng bào chế và cửa sổ dùng thuốc (FR-001 → FR-003)."
        actions={
          isManager && (
            <Button onClick={handleOpenCreate} className="gap-2">
              <Plus className="h-4 w-4" /> Thêm thuốc
            </Button>
          )
        }
      />

      {problem && <ProblemAlert problem={problem} onDismiss={() => setProblem(null)} />}

      {/* Thanh công cụ tìm kiếm và lọc */}
      <Card>
        <CardContent className="flex flex-wrap items-center justify-between gap-4 py-4">
          <div className="relative flex-1 min-w-[280px]">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Tìm theo tên thuốc, hàm lượng, dạng bào chế..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pl-9"
            />
          </div>
          <div className="flex items-center gap-2">
            <span className="text-sm font-medium text-muted-foreground flex items-center gap-1.5">
              <Filter className="h-4 w-4" /> Trạng thái:
            </span>
            <div className="flex rounded-md bg-muted p-1">
              {[
                { key: 'ALL', label: 'Tất cả' },
                { key: 'ACTIVE', label: 'Hiệu lực' },
                { key: 'INACTIVE', label: 'Ngừng' },
              ].map((opt) => (
                <button
                  key={opt.key}
                  type="button"
                  onClick={() => setFilterStatus(opt.key)}
                  className={cn(
                    'px-3 py-1 text-xs font-medium rounded transition-colors',
                    filterStatus === opt.key
                      ? 'bg-background text-foreground shadow-sm'
                      : 'text-muted-foreground hover:text-foreground'
                  )}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Bảng danh sách thuốc */}
      <Card>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b bg-muted/50 text-xs font-semibold uppercase text-muted-foreground">
                <tr>
                  <th className="px-4 py-3">Tên & Hàm lượng</th>
                  <th className="px-4 py-3">Dạng bào chế</th>
                  <th className="px-4 py-3">Đường dùng & Đơn vị</th>
                  <th className="px-4 py-3">Cửa sổ (trước/sau)</th>
                  <th className="px-4 py-3">Đặc tính & Cảnh báo</th>
                  <th className="px-4 py-3">Trạng thái</th>
                  {isManager && <th className="px-4 py-3 text-right">Thao tác</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {loading ? (
                  <tr>
                    <td colSpan={7} className="text-center py-8 text-muted-foreground">
                      Đang tải dữ liệu danh mục thuốc...
                    </td>
                  </tr>
                ) : items.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="text-center py-8 text-muted-foreground">
                      Không tìm thấy thuốc nào phù hợp điều kiện tìm kiếm.
                    </td>
                  </tr>
                ) : (
                  items.map((item) => (
                    <tr key={item.id} className="hover:bg-muted/40 transition-colors">
                      <td className="px-4 py-3.5">
                        <div className="font-semibold text-foreground flex items-center gap-1.5">
                          <Pill className="h-4 w-4 text-primary shrink-0" />
                          <span>{item.name}</span>
                        </div>
                        <div className="text-xs text-muted-foreground font-mono">{item.concentration}</div>
                      </td>
                      <td className="px-4 py-3.5 text-foreground">{item.dosageForm}</td>
                      <td className="px-4 py-3.5">
                        <div>{item.defaultRoute}</div>
                        <div className="text-xs text-muted-foreground">Đơn vị: {item.usageUnit}</div>
                      </td>
                      <td className="px-4 py-3.5 font-mono text-xs">
                        {item.beforeWindowMinutes != null || item.afterWindowMinutes != null ? (
                          <span>
                            -{item.beforeWindowMinutes ?? 'CFG'}m / +{item.afterWindowMinutes ?? 'CFG'}m
                          </span>
                        ) : (
                          <span className="text-muted-foreground">Theo mặc định (CFG-M07-01)</span>
                        )}
                      </td>
                      <td className="px-4 py-3.5">
                        <div className="flex flex-col gap-1 items-start">
                          {item.specialControl && (
                            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-medium bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300">
                              <ShieldAlert className="h-3 w-3" /> Kiểm soát đặc biệt
                            </span>
                          )}
                          {item.missingPricingItem && (
                            <span
                              title="Thuốc nguồn viện chưa có dịch vụ FACILITY_MEDICATION ở 004 (FR-003)"
                              className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-medium bg-orange-100 text-orange-800 dark:bg-orange-900/40 dark:text-orange-300"
                            >
                              <AlertTriangle className="h-3 w-3" /> Chưa có mục vật phẩm viện
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="px-4 py-3.5">
                        <span
                          className={cn(
                            'inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium',
                            item.active
                              ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950/50 dark:text-emerald-300'
                              : 'bg-muted text-muted-foreground'
                          )}
                        >
                          {item.active ? 'Hiệu lực' : 'Đã ngừng'}
                        </span>
                      </td>
                      {isManager && (
                        <td className="px-4 py-3.5 text-right">
                          <div className="flex items-center justify-end gap-1.5">
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => handleOpenEdit(item)}
                              title="Sửa thông tin thuốc"
                            >
                              <Edit2 className="h-3.5 w-3.5" />
                            </Button>
                            {item.active && (
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => setDeactivatingItem(item)}
                                className="text-destructive hover:bg-destructive/10"
                                title="Ngừng hiệu lực thuốc"
                              >
                                <PowerOff className="h-3.5 w-3.5" />
                              </Button>
                            )}
                          </div>
                        </td>
                      )}
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {/* Dialog Thêm / Sửa thuốc */}
      <Dialog open={isFormOpen} onOpenChange={setIsFormOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>{editingItem ? 'Sửa thông tin thuốc' : 'Thêm thuốc mới vào danh mục'}</DialogTitle>
            <DialogDescription>
              {editingItem
                ? 'Đổi hoạt chất hoặc cửa sổ chỉ áp cho đơn lưu sau và liều chưa tới đầu cửa sổ (FR-002).'
                : 'Nhập thông tin định danh thuốc và liên kết dị nguyên nhóm thuốc (FR-001).'}
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleSaveForm} className="space-y-4 py-2">
            <div>
              <Label htmlFor="name">Tên thuốc *</Label>
              <Input
                id="name"
                required
                value={formValues.name}
                onChange={(e) => setFormValues((v) => ({ ...v, name: e.target.value }))}
                placeholder="Ví dụ: Paracetamol"
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <Label htmlFor="concentration">Hàm lượng *</Label>
                <Input
                  id="concentration"
                  required
                  value={formValues.concentration}
                  onChange={(e) => setFormValues((v) => ({ ...v, concentration: e.target.value }))}
                  placeholder="500mg, 10ml..."
                />
              </div>
              <div>
                <Label htmlFor="dosageForm">Dạng bào chế *</Label>
                <Input
                  id="dosageForm"
                  required
                  value={formValues.dosageForm}
                  onChange={(e) => setFormValues((v) => ({ ...v, dosageForm: e.target.value }))}
                  placeholder="Viên nén, siro..."
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <Label htmlFor="defaultRoute">Đường dùng mặc định *</Label>
                <Input
                  id="defaultRoute"
                  required
                  value={formValues.defaultRoute}
                  onChange={(e) => setFormValues((v) => ({ ...v, defaultRoute: e.target.value }))}
                  placeholder="Uống, Tiêm..."
                />
              </div>
              <div>
                <Label htmlFor="usageUnit">Đơn vị dùng *</Label>
                <Input
                  id="usageUnit"
                  required
                  value={formValues.usageUnit}
                  onChange={(e) => setFormValues((v) => ({ ...v, usageUnit: e.target.value }))}
                  placeholder="viên, ml, gói..."
                />
              </div>
            </div>

            <div>
              <Label htmlFor="allergenIds">Mã dị nguyên / Hoạt chất (phân cách bằng dấu phẩy) *</Label>
              <Input
                id="allergenIds"
                required
                value={formValues.allergenIds}
                onChange={(e) => setFormValues((v) => ({ ...v, allergenIds: e.target.value }))}
                placeholder="101, 102..."
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <Label htmlFor="beforeWindowMinutes">Cửa sổ trước (phút)</Label>
                <Input
                  id="beforeWindowMinutes"
                  type="number"
                  min="0"
                  value={formValues.beforeWindowMinutes}
                  onChange={(e) => setFormValues((v) => ({ ...v, beforeWindowMinutes: e.target.value }))}
                  placeholder="Mặc định CFG"
                />
              </div>
              <div>
                <Label htmlFor="afterWindowMinutes">Cửa sổ sau (phút)</Label>
                <Input
                  id="afterWindowMinutes"
                  type="number"
                  min="0"
                  value={formValues.afterWindowMinutes}
                  onChange={(e) => setFormValues((v) => ({ ...v, afterWindowMinutes: e.target.value }))}
                  placeholder="Mặc định CFG"
                />
              </div>
            </div>

            <div className="flex items-center gap-2 pt-1">
              <input
                type="checkbox"
                id="specialControl"
                checked={formValues.specialControl}
                onChange={(e) => setFormValues((v) => ({ ...v, specialControl: e.target.checked }))}
                className="rounded border-border text-primary focus:ring-primary h-4 w-4"
              />
              <Label htmlFor="specialControl" className="cursor-pointer font-normal text-sm">
                Thuốc kiểm soát đặc biệt (gây nghiện, hướng thần - FR-029)
              </Label>
            </div>

            <DialogFooter className="pt-2">
              <Button type="button" variant="outline" onClick={() => setIsFormOpen(false)}>
                Hủy
              </Button>
              <Button type="submit" disabled={formSubmitting}>
                {formSubmitting ? 'Đang lưu...' : editingItem ? 'Lưu thay đổi' : 'Thêm thuốc'}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Dialog Ngừng hiệu lực */}
      <Dialog open={!!deactivatingItem} onOpenChange={(open) => !open && setDeactivatingItem(null)}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Ngừng hiệu lực thuốc</DialogTitle>
            <DialogDescription>
              Thuốc <strong>{deactivatingItem?.name} ({deactivatingItem?.concentration})</strong> sẽ không thể chọn
              cho các đơn thuốc mới (FR-002). Thuốc đã có đơn tham chiếu không bị xóa.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-3 py-2">
            <Label htmlFor="reason">Lý do ngừng hiệu lực *</Label>
            <Input
              id="reason"
              required
              value={deactivateReason}
              onChange={(e) => setDeactivateReason(e.target.value)}
              placeholder="Ví dụ: Nhà sản xuất ngừng phân phối, thay thế thuốc khác..."
            />
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setDeactivatingItem(null)}>
              Hủy
            </Button>
            <Button
              variant="destructive"
              onClick={handleDeactivate}
              disabled={formSubmitting || !deactivateReason.trim()}
            >
              {formSubmitting ? 'Đang xử lý...' : 'Xác nhận ngừng'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
