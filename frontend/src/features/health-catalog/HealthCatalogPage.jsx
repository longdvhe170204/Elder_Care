import { useEffect, useState } from 'react'
import { Activity, Edit2, Heart, PowerOff, Search, Settings } from 'lucide-react'
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
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { PageHeader } from '@/common/PageHeader'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { problemOf } from '@/common/problem'
import {
  getIncidentTypes,
  getProgressionTypes,
  getTrendRules,
  getVitalSignCatalog,
  postVitalSignCatalogByIdDeactivate,
  putIncidentTypesById,
  putTrendRulesByCode,
  putVitalSignCatalogById,
} from '@/api/api007'

const SUBTITLES = {
  'vital-signs': 'Danh mục chỉ số sức khỏe, cờ sinh hiệu và khoảng hợp lệ vật lý theo CFG-M06-03.',
  'trend-rules': 'Quy tắc phát hiện xu hướng bất thường, mức cảnh báo và tham số kiểm tra.',
  'incident-types': 'Danh mục loại sự cố, mức mặc định và phạm vi liên quan sức khỏe.',
  'progression-types': 'Các loại diễn biến trong xử lý sự cố và cờ biện pháp hồi sức cấp cứu.',
}

const INCIDENT_LEVEL_LABELS = {
  LOW: 'Nhẹ',
  MEDIUM: 'Trung bình',
  CRITICAL: 'Khẩn cấp',
}

const PROGRESSION_GROUP_LABELS = {
  EVALUATION: 'Đánh giá',
  ACTION: 'Hành động',
  NOTIFICATION: 'Thông báo',
  MONITORING: 'Theo dõi',
  SYSTEM: 'Hệ thống',
}

export function HealthCatalogPage({ role }) {
  const isManager = role === 'NURSING_HOME_MANAGER'
  const [activeTab, setActiveTab] = useState('vital-signs')
  const [problem, setProblem] = useState(null)
  const [search, setSearch] = useState('')

  // Vital Signs State
  const [vitalSigns, setVitalSigns] = useState([])
  const [vitalSignEtag, setVitalSignEtag] = useState(null)
  const [vitalSignFilter, setVitalSignFilter] = useState('all')
  const [editingVitalSign, setEditingVitalSign] = useState(null)
  const [deactivatingVitalSign, setDeactivatingVitalSign] = useState(null)
  const [deactivateReason, setDeactivateReason] = useState('')

  // Trend Rules State
  const [trendRules, setTrendRules] = useState([])
  const [trendRulesEtag, setTrendRulesEtag] = useState(null)
  const [editingTrendRule, setEditingTrendRule] = useState(null)
  const [trendRuleReason, setTrendRuleReason] = useState('')

  // Incident Types State
  const [incidentTypes, setIncidentTypes] = useState([])
  const [incidentTypesEtag, setIncidentTypesEtag] = useState(null)
  const [editingIncidentType, setEditingIncidentType] = useState(null)

  // Progression Types State
  const [progressionTypes, setProgressionTypes] = useState([])

  const loadVitalSigns = async () => {
    try {
      const res = await getVitalSignCatalog()
      setVitalSigns(res.data || [])
      setVitalSignEtag(res.etag)
    } catch (err) {
      setProblem(problemOf(err))
    }
  }

  const loadTrendRules = async () => {
    try {
      const res = await getTrendRules()
      setTrendRules(res.data || [])
      setTrendRulesEtag(res.etag)
    } catch (err) {
      setProblem(problemOf(err))
    }
  }

  const loadIncidentTypes = async () => {
    try {
      const res = await getIncidentTypes()
      setIncidentTypes(res.data || [])
      setIncidentTypesEtag(res.etag)
    } catch (err) {
      setProblem(problemOf(err))
    }
  }

  useEffect(() => {
    let cancelled = false
    if (activeTab === 'vital-signs') {
      getVitalSignCatalog()
        .then(({ data, etag }) => {
          if (cancelled) return
          setVitalSigns(data || [])
          setVitalSignEtag(etag)
        })
        .catch((err) => {
          if (!cancelled) setProblem(problemOf(err))
        })
    } else if (activeTab === 'trend-rules') {
      getTrendRules()
        .then(({ data, etag }) => {
          if (cancelled) return
          setTrendRules(data || [])
          setTrendRulesEtag(etag)
        })
        .catch((err) => {
          if (!cancelled) setProblem(problemOf(err))
        })
    } else if (activeTab === 'incident-types') {
      getIncidentTypes()
        .then(({ data, etag }) => {
          if (cancelled) return
          setIncidentTypes(data || [])
          setIncidentTypesEtag(etag)
        })
        .catch((err) => {
          if (!cancelled) setProblem(problemOf(err))
        })
    } else if (activeTab === 'progression-types') {
      getProgressionTypes()
        .then(({ data }) => {
          if (cancelled) return
          setProgressionTypes(data || [])
        })
        .catch((err) => {
          if (!cancelled) setProblem(problemOf(err))
        })
    }
    return () => {
      cancelled = true
    }
  }, [activeTab])

  const handleTabChange = (next) => {
    if (next[0]) {
      setProblem(null)
      setActiveTab(next[0])
    }
  }

  // Save Vital Sign
  const handleSaveVitalSign = async (e) => {
    e.preventDefault()
    if (!editingVitalSign) return
    try {
      await putVitalSignCatalogById({
        path: { id: editingVitalSign.id },
        headers: { 'If-Match': vitalSignEtag || '"1"' },
        body: {
          name: editingVitalSign.name,
          unit: editingVitalSign.unit,
          vitalSign: editingVitalSign.vitalSign,
        },
      })
      setEditingVitalSign(null)
      loadVitalSigns()
    } catch (err) {
      setProblem(problemOf(err))
    }
  }

  // Deactivate Vital Sign
  const handleDeactivateVitalSign = async (e) => {
    e.preventDefault()
    if (!deactivatingVitalSign || !deactivateReason.trim()) return
    try {
      await postVitalSignCatalogByIdDeactivate({
        path: { id: deactivatingVitalSign.id },
        body: { reason: deactivateReason.trim() },
      })
      setDeactivatingVitalSign(null)
      setDeactivateReason('')
      loadVitalSigns()
    } catch (err) {
      setProblem(problemOf(err))
    }
  }

  // Save Trend Rule
  const handleSaveTrendRule = async (e) => {
    e.preventDefault()
    if (!editingTrendRule || !trendRuleReason.trim()) return
    try {
      await putTrendRulesByCode({
        path: { code: editingTrendRule.code },
        headers: { 'If-Match': trendRulesEtag || '"1"' },
        body: {
          enabled: editingTrendRule.enabled,
          level: editingTrendRule.level,
          reason: trendRuleReason.trim(),
        },
      })
      setEditingTrendRule(null)
      setTrendRuleReason('')
      loadTrendRules()
    } catch (err) {
      setProblem(problemOf(err))
    }
  }

  // Save Incident Type
  const handleSaveIncidentType = async (e) => {
    e.preventDefault()
    if (!editingIncidentType) return
    try {
      await putIncidentTypesById({
        path: { id: editingIncidentType.id },
        headers: { 'If-Match': incidentTypesEtag || '"1"' },
        body: {
          name: editingIncidentType.name,
          defaultLevel: editingIncidentType.defaultLevel,
          nonHealth: editingIncidentType.nonHealth,
          active: editingIncidentType.active,
        },
      })
      setEditingIncidentType(null)
      loadIncidentTypes()
    } catch (err) {
      setProblem(problemOf(err))
    }
  }

  const filteredVitalSigns = vitalSigns.filter((item) => {
    const matchesFilter = vitalSignFilter === 'all' ? true : vitalSignFilter === 'active' ? item.active : !item.active
    const matchesSearch =
      !search ||
      item.name.toLowerCase().includes(search.toLowerCase()) ||
      item.code.toLowerCase().includes(search.toLowerCase())
    return matchesFilter && matchesSearch
  })

  return (
    <div className="mx-auto flex w-full max-w-[1600px] flex-col gap-6">
      <PageHeader title="Danh mục sức khỏe & sự cố" subtitle={SUBTITLES[activeTab]}>
        <ToggleGroup variant="outline" value={[activeTab]} onValueChange={handleTabChange} className="self-start">
          <ToggleGroupItem value="vital-signs" className="h-10 px-4">
            Chỉ số sức khỏe
          </ToggleGroupItem>
          <ToggleGroupItem value="trend-rules" className="h-10 px-4">
            Quy tắc xu hướng
          </ToggleGroupItem>
          <ToggleGroupItem value="incident-types" className="h-10 px-4">
            Loại sự cố
          </ToggleGroupItem>
          <ToggleGroupItem value="progression-types" className="h-10 px-4">
            Loại diễn biến
          </ToggleGroupItem>
        </ToggleGroup>
      </PageHeader>

      {problem && <ProblemAlert problem={problem} />}

      {/* TAB 1: CHỈ SỐ SỨC KHỎE */}
      {activeTab === 'vital-signs' && (
        <div className="flex flex-col gap-4">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div className="relative min-w-[280px] max-w-sm flex-1">
              <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder="Tìm kiếm chỉ số..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="pl-9"
              />
            </div>
            <ToggleGroup
              variant="outline"
              value={[vitalSignFilter]}
              onValueChange={(val) => val[0] && setVitalSignFilter(val[0])}
            >
              <ToggleGroupItem value="all" className="h-9 px-3 text-sm">
                Tất cả
              </ToggleGroupItem>
              <ToggleGroupItem value="active" className="h-9 px-3 text-sm">
                Hiệu lực
              </ToggleGroupItem>
              <ToggleGroupItem value="inactive" className="h-9 px-3 text-sm">
                Ngừng hiệu lực
              </ToggleGroupItem>
            </ToggleGroup>
          </div>

          <Card>
            <CardContent className="p-0">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="border-b bg-muted/40 font-medium text-muted-foreground">
                    <tr>
                      <th className="px-4 py-3">Mã chỉ số</th>
                      <th className="px-4 py-3">Tên chỉ số</th>
                      <th className="px-4 py-3">Đơn vị</th>
                      <th className="px-4 py-3">Phân loại</th>
                      <th className="px-4 py-3">Thành phần</th>
                      <th className="px-4 py-3">Khoảng hợp lệ vật lý</th>
                      <th className="px-4 py-3">Trạng thái</th>
                      {isManager && <th className="px-4 py-3 text-right">Thao tác</th>}
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    {filteredVitalSigns.length === 0 ? (
                      <tr>
                        <td colSpan={isManager ? 8 : 7} className="px-4 py-8 text-center text-muted-foreground">
                          Không có chỉ số nào phù hợp.
                        </td>
                      </tr>
                    ) : (
                      filteredVitalSigns.map((item) => (
                        <tr key={item.id} className="hover:bg-muted/30">
                          <td className="px-4 py-3 font-mono font-medium">{item.code}</td>
                          <td className="px-4 py-3 font-medium text-foreground">{item.name}</td>
                          <td className="px-4 py-3">{item.unit || '—'}</td>
                          <td className="px-4 py-3">
                            {item.vitalSign ? (
                              <span className="inline-flex items-center gap-1 text-primary">
                                <Heart className="size-3.5 fill-current" />
                                Sinh hiệu
                              </span>
                            ) : (
                              <span className="text-muted-foreground">Chỉ số thường</span>
                            )}
                          </td>
                          <td className="px-4 py-3">
                            {item.components && item.components.length > 0
                              ? item.components.map((c) => c.name).join(', ')
                              : '—'}
                          </td>
                          <td className="px-4 py-3">
                            {item.missingValidRange ? (
                              <span className="text-amber-600 dark:text-amber-400">Chưa có khoảng hợp lệ</span>
                            ) : item.validRange ? (
                              `${item.validRange.min} - ${item.validRange.max} ${item.unit}`
                            ) : (
                              '—'
                            )}
                          </td>
                          <td className="px-4 py-3">
                            <StatusBadge status={item.active ? 'Hiệu lực' : 'Đã đóng'} />
                          </td>
                          {isManager && (
                            <td className="px-4 py-3 text-right">
                              <div className="flex items-center justify-end gap-1">
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  title={`Sửa chỉ số ${item.name}`}
                                  onClick={() => setEditingVitalSign(item)}
                                >
                                  <Edit2 className="size-4" />
                                </Button>
                                {item.active && (
                                  <Button
                                    variant="ghost"
                                    size="sm"
                                    title={`Ngừng hiệu lực ${item.name}`}
                                    className="text-destructive hover:bg-destructive/10 hover:text-destructive"
                                    onClick={() => {
                                      setDeactivatingVitalSign(item)
                                      setDeactivateReason('')
                                    }}
                                  >
                                    <PowerOff className="size-4" />
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
        </div>
      )}

      {/* TAB 2: QUY TẮC XU HƯỚNG */}
      {activeTab === 'trend-rules' && (
        <Card>
          <CardContent className="p-0">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="border-b bg-muted/40 font-medium text-muted-foreground">
                  <tr>
                    <th className="px-4 py-3">Mã quy tắc</th>
                    <th className="px-4 py-3">Tên quy tắc</th>
                    <th className="px-4 py-3">Mức cảnh báo</th>
                    <th className="px-4 py-3">Trạng thái</th>
                    <th className="px-4 py-3">Mã tham số CFG</th>
                    {isManager && <th className="px-4 py-3 text-right">Thao tác</th>}
                  </tr>
                </thead>
                <tbody className="divide-y">
                  {trendRules.map((rule) => (
                    <tr key={rule.code} className="hover:bg-muted/30">
                      <td className="px-4 py-3 font-mono font-medium">{rule.code}</td>
                      <td className="px-4 py-3 font-medium text-foreground">{rule.name}</td>
                      <td className="px-4 py-3">
                        <StatusBadge status={INCIDENT_LEVEL_LABELS[rule.level] || rule.level} />
                      </td>
                      <td className="px-4 py-3">
                        <StatusBadge status={rule.enabled ? 'Hiệu lực' : 'Đã đóng'} />
                      </td>
                      <td className="px-4 py-3 font-mono text-muted-foreground">{rule.parameterCode || '—'}</td>
                      {isManager && (
                        <td className="px-4 py-3 text-right">
                          <Button
                            variant="ghost"
                            size="sm"
                            title={`Cấu hình quy tắc ${rule.name}`}
                            onClick={() => {
                              setEditingTrendRule({ ...rule })
                              setTrendRuleReason('')
                            }}
                          >
                            <Settings className="size-4" />
                          </Button>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </CardContent>
        </Card>
      )}

      {/* TAB 3: LOẠI SỰ CỐ */}
      {activeTab === 'incident-types' && (
        <Card>
          <CardContent className="p-0">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="border-b bg-muted/40 font-medium text-muted-foreground">
                  <tr>
                    <th className="px-4 py-3">Mã</th>
                    <th className="px-4 py-3">Tên sự cố</th>
                    <th className="px-4 py-3">Mức mặc định</th>
                    <th className="px-4 py-3">Phân loại sức khỏe</th>
                    <th className="px-4 py-3">Đối tượng</th>
                    <th className="px-4 py-3">Trạng thái</th>
                    {isManager && <th className="px-4 py-3 text-right">Thao tác</th>}
                  </tr>
                </thead>
                <tbody className="divide-y">
                  {incidentTypes.map((type) => (
                    <tr key={type.id} className="hover:bg-muted/30">
                      <td className="px-4 py-3 font-mono font-medium">{type.code}</td>
                      <td className="px-4 py-3 font-medium text-foreground">{type.name}</td>
                      <td className="px-4 py-3">
                        <StatusBadge status={INCIDENT_LEVEL_LABELS[type.defaultLevel] || type.defaultLevel} />
                      </td>
                      <td className="px-4 py-3">
                        {type.nonHealth ? (
                          <span className="text-amber-600 dark:text-amber-400">Không thuộc sức khỏe</span>
                        ) : (
                          <span className="text-primary">Liên quan sức khỏe</span>
                        )}
                      </td>
                      <td className="px-4 py-3">
                        {type.allowsNoResident ? 'Cho phép không gắn NCT' : 'Bắt buộc gắn NCT'}
                      </td>
                      <td className="px-4 py-3">
                        <StatusBadge status={type.active ? 'Hiệu lực' : 'Đã đóng'} />
                      </td>
                      {isManager && (
                        <td className="px-4 py-3 text-right">
                          <Button
                            variant="ghost"
                            size="sm"
                            title={`Sửa loại sự cố ${type.name}`}
                            onClick={() => setEditingIncidentType({ ...type })}
                          >
                            <Edit2 className="size-4" />
                          </Button>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </CardContent>
        </Card>
      )}

      {/* TAB 4: LOẠI DIỄN BIẾN */}
      {activeTab === 'progression-types' && (
        <Card>
          <CardContent className="p-0">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="border-b bg-muted/40 font-medium text-muted-foreground">
                  <tr>
                    <th className="px-4 py-3">Mã diễn biến</th>
                    <th className="px-4 py-3">Tên diễn biến</th>
                    <th className="px-4 py-3">Nhóm</th>
                    <th className="px-4 py-3">Đặc tính</th>
                  </tr>
                </thead>
                <tbody className="divide-y">
                  {progressionTypes.map((prog) => (
                    <tr key={prog.id} className="hover:bg-muted/30">
                      <td className="px-4 py-3 font-mono font-medium">{prog.code}</td>
                      <td className="px-4 py-3 font-medium text-foreground">{prog.name}</td>
                      <td className="px-4 py-3">{PROGRESSION_GROUP_LABELS[prog.group] || prog.group}</td>
                      <td className="px-4 py-3">
                        {prog.resuscitation ? (
                          <span className="inline-flex items-center gap-1 font-medium text-destructive">
                            <Activity className="size-3.5" />
                            Biện pháp hồi sức
                          </span>
                        ) : (
                          '—'
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </CardContent>
        </Card>
      )}

      {/* DIALOG: SỬA CHỈ SỐ */}
      <Dialog open={!!editingVitalSign} onOpenChange={(open) => !open && setEditingVitalSign(null)}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Sửa thông tin chỉ số</DialogTitle>
            <DialogDescription>
              Cập nhật tên, đơn vị và phân loại sinh hiệu cho chỉ số {editingVitalSign?.code}.
            </DialogDescription>
          </DialogHeader>
          {editingVitalSign && (
            <form onSubmit={handleSaveVitalSign} className="flex flex-col gap-4">
              <div className="flex flex-col gap-2">
                <Label htmlFor="vs-name">Tên chỉ số *</Label>
                <Input
                  id="vs-name"
                  value={editingVitalSign.name}
                  onChange={(e) => setEditingVitalSign({ ...editingVitalSign, name: e.target.value })}
                  required
                />
              </div>
              <div className="flex flex-col gap-2">
                <Label htmlFor="vs-unit">Đơn vị đo</Label>
                <Input
                  id="vs-unit"
                  value={editingVitalSign.unit || ''}
                  onChange={(e) => setEditingVitalSign({ ...editingVitalSign, unit: e.target.value })}
                />
              </div>
              <div className="flex items-center gap-2 pt-2">
                <input
                  type="checkbox"
                  id="vs-vitalSign"
                  checked={editingVitalSign.vitalSign}
                  onChange={(e) => setEditingVitalSign({ ...editingVitalSign, vitalSign: e.target.checked })}
                  className="size-4 rounded border-input"
                />
                <Label htmlFor="vs-vitalSign">Thuộc nhóm sinh hiệu trọng yếu</Label>
              </div>
              <DialogFooter className="pt-4">
                <Button type="button" variant="outline" onClick={() => setEditingVitalSign(null)}>
                  Hủy
                </Button>
                <Button type="submit">Lưu thay đổi</Button>
              </DialogFooter>
            </form>
          )}
        </DialogContent>
      </Dialog>

      {/* DIALOG: NGỪNG HIỆU LỰC CHỈ SỐ */}
      <Dialog open={!!deactivatingVitalSign} onOpenChange={(open) => !open && setDeactivatingVitalSign(null)}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Ngừng hiệu lực chỉ số</DialogTitle>
            <DialogDescription>
              Chỉ số {deactivatingVitalSign?.name} ({deactivatingVitalSign?.code}) sẽ không thể chọn khi lập lịch đo
              mới.
            </DialogDescription>
          </DialogHeader>
          <form onSubmit={handleDeactivateVitalSign} className="flex flex-col gap-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="deactivate-reason">Lý do ngừng hiệu lực *</Label>
              <Input
                id="deactivate-reason"
                placeholder="Nhập lý do nghiệp vụ..."
                value={deactivateReason}
                onChange={(e) => setDeactivateReason(e.target.value)}
                required
              />
            </div>
            <DialogFooter className="pt-4">
              <Button type="button" variant="outline" onClick={() => setDeactivatingVitalSign(null)}>
                Hủy
              </Button>
              <Button type="submit" variant="destructive" disabled={!deactivateReason.trim()}>
                Xác nhận ngừng
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* DIALOG: CẤU HÌNH QUY TẮC XU HƯỚNG */}
      <Dialog open={!!editingTrendRule} onOpenChange={(open) => !open && setEditingTrendRule(null)}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Cấu hình quy tắc xu hướng</DialogTitle>
            <DialogDescription>
              Điều chỉnh mức cảnh báo hoặc trạng thái hoạt động của quy tắc {editingTrendRule?.code}.
            </DialogDescription>
          </DialogHeader>
          {editingTrendRule && (
            <form onSubmit={handleSaveTrendRule} className="flex flex-col gap-4">
              <div className="flex flex-col gap-2">
                <Label htmlFor="tr-level">Mức cảnh báo *</Label>
                <select
                  id="tr-level"
                  value={editingTrendRule.level}
                  onChange={(e) => setEditingTrendRule({ ...editingTrendRule, level: e.target.value })}
                  className="h-10 rounded-lg border border-input bg-background px-3 py-2 text-sm"
                >
                  <option value="LOW">Nhẹ</option>
                  <option value="MEDIUM">Trung bình</option>
                  <option value="CRITICAL">Khẩn cấp</option>
                </select>
              </div>
              <div className="flex items-center gap-2 pt-1">
                <input
                  type="checkbox"
                  id="tr-enabled"
                  checked={editingTrendRule.enabled}
                  onChange={(e) => setEditingTrendRule({ ...editingTrendRule, enabled: e.target.checked })}
                  className="size-4 rounded border-input"
                />
                <Label htmlFor="tr-enabled">Kích hoạt quy tắc</Label>
              </div>
              <div className="flex flex-col gap-2">
                <Label htmlFor="tr-reason">Lý do điều chỉnh *</Label>
                <Input
                  id="tr-reason"
                  placeholder="Nhập lý do thay đổi..."
                  value={trendRuleReason}
                  onChange={(e) => setTrendRuleReason(e.target.value)}
                  required
                />
              </div>
              <DialogFooter className="pt-4">
                <Button type="button" variant="outline" onClick={() => setEditingTrendRule(null)}>
                  Hủy
                </Button>
                <Button type="submit" disabled={!trendRuleReason.trim()}>
                  Lưu cấu hình
                </Button>
              </DialogFooter>
            </form>
          )}
        </DialogContent>
      </Dialog>

      {/* DIALOG: SỬA LOẠI SỰ CỐ */}
      <Dialog open={!!editingIncidentType} onOpenChange={(open) => !open && setEditingIncidentType(null)}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Sửa loại sự cố</DialogTitle>
            <DialogDescription>
              Cập nhật tên, mức mặc định và phạm vi sức khỏe cho loại {editingIncidentType?.code}.
            </DialogDescription>
          </DialogHeader>
          {editingIncidentType && (
            <form onSubmit={handleSaveIncidentType} className="flex flex-col gap-4">
              <div className="flex flex-col gap-2">
                <Label htmlFor="it-name">Tên sự cố *</Label>
                <Input
                  id="it-name"
                  value={editingIncidentType.name}
                  onChange={(e) => setEditingIncidentType({ ...editingIncidentType, name: e.target.value })}
                  required
                />
              </div>
              <div className="flex flex-col gap-2">
                <Label htmlFor="it-level">Mức mặc định *</Label>
                <select
                  id="it-level"
                  value={editingIncidentType.defaultLevel}
                  onChange={(e) => setEditingIncidentType({ ...editingIncidentType, defaultLevel: e.target.value })}
                  className="h-10 rounded-lg border border-input bg-background px-3 py-2 text-sm"
                >
                  <option value="LOW">Nhẹ</option>
                  <option value="MEDIUM">Trung bình</option>
                  <option value="CRITICAL">Khẩn cấp</option>
                </select>
              </div>
              <div className="flex items-center gap-2 pt-1">
                <input
                  type="checkbox"
                  id="it-nonHealth"
                  checked={editingIncidentType.nonHealth}
                  onChange={(e) => setEditingIncidentType({ ...editingIncidentType, nonHealth: e.target.checked })}
                  className="size-4 rounded border-input"
                />
                <Label htmlFor="it-nonHealth">Không thuộc sức khỏe</Label>
              </div>
              <div className="flex items-center gap-2 pt-1">
                <input
                  type="checkbox"
                  id="it-active"
                  checked={editingIncidentType.active}
                  onChange={(e) => setEditingIncidentType({ ...editingIncidentType, active: e.target.checked })}
                  className="size-4 rounded border-input"
                />
                <Label htmlFor="it-active">Đang hiệu lực</Label>
              </div>
              <DialogFooter className="pt-4">
                <Button type="button" variant="outline" onClick={() => setEditingIncidentType(null)}>
                  Hủy
                </Button>
                <Button type="submit">Lưu thay đổi</Button>
              </DialogFooter>
            </form>
          )}
        </DialogContent>
      </Dialog>
    </div>
  )
}
