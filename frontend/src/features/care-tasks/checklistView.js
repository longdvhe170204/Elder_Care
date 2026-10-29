// Chuyển dữ liệu hợp đồng api-005 (Checklist, CongViec, CongViecChiTiet, LoaiCongViec) sang dữ liệu cho màn.
// Chỉ đọc trường có trong hợp đồng; khi có client sinh từ hợp đồng đã dịch (000 T010) chỉ sửa file này.

const STATUS = {
  CHUA_DEN_HAN: 'Chưa đến hạn',
  DEN_HAN: 'Đến hạn',
  QUA_HAN: 'Quá hạn',
  HOAN_THANH: 'Hoàn thành',
  HOAN_THANH_TRE: 'Hoàn thành trễ',
  KHONG_THUC_HIEN: 'Không thực hiện',
  HUY: 'Hủy',
}
const IMPORTANCE = { THUONG: 'Thường', QUAN_TRONG: 'Quan trọng', BAT_BUOC: 'Bắt buộc' }
// Nguồn (canCu.loai), tên theo data-model 005 loai_can_cu
const SOURCE = {
  MUC_KE_HOACH: 'Kế hoạch chăm sóc',
  LAN_DO: 'Lịch đo',
  BUA: 'Bữa ăn',
  BUOI_HOAT_DONG: 'Buổi hoạt động',
  THEO_DOI_SAU_NGA: 'Theo dõi sau ngã',
  THEO_DOI_TIEP_XUC: 'Theo dõi tiếp xúc',
  CANH_BAO_UONG_NUOC: 'Cảnh báo uống nước',
  KIEM_TRA_DAU_CA: 'Kiểm tra đầu ca',
  GIAM_SAT: 'Giám sát',
  PHAT_SINH: 'Phát sinh',
  GHI_NHAN_PHAT_SINH: 'Ghi nhận phát sinh',
  LAM_LAI: 'Làm lại',
}
const CARRY = { TON_TU_CA_TRUOC: 'tồn từ ca trước', TON_CHO_XAC_NHAN: 'tồn, chờ xác nhận' }
// Kiểu ô nhập của form ghi nhận theo loaiKetQua của loại công việc
const RESULT_KIND = { ML: 'water', MUC_AN: 'meal', CHI_SO: 'measure', SO_LUONG: 'quantity' }

const hhmm = (iso) => iso.slice(11, 16)
const ddmmyyyy = (date) => date.split('-').reverse().join('/')

export function toTaskView(congViec, { choicesByNct, typesByName }) {
  const type = typesByName.get(congViec.loaiCongViec)
  return {
    id: congViec.id,
    nctId: congViec.nctId,
    resident: congViec.theNguoiCaoTuoi?.hoTen,
    photo: congViec.theNguoiCaoTuoi?.anh,
    bed: congViec.theNguoiCaoTuoi?.phongGiuong,
    careNote: congViec.theNguoiCaoTuoi?.luuYChamSoc,
    location: congViec.viTri,
    floorTask: congViec.congViecChung,
    title: congViec.loaiCongViec,
    content: congViec.noiDung,
    source: SOURCE[congViec.canCu?.loai],
    carry: CARRY[congViec.dauTon],
    plannedAt: hhmm(congViec.thoiDiemDuKien),
    from: hhmm(congViec.dauKhung),
    to: hhmm(congViec.cuoiKhung),
    importance: IMPORTANCE[congViec.mucQuanTrong],
    assignee: congViec.nguoiThucHien,
    status: STATUS[congViec.trangThai],
    awaitingRemeasure: congViec.choXacNhanDoLai,
    commands: congViec.lenhDuocPhep ?? [],
    kind: RESULT_KIND[type?.loaiKetQua] ?? 'simple',
    unit: type?.loaiChiSo ?? (type?.loaiKetQua === 'SO_LUONG' ? 'cái' : undefined),
    riskChoices: (choicesByNct.get(congViec.nctId) ?? []).map((t) => ({ choice: t.luaChon, reviewOn: ddmmyyyy(t.ngayXemLai) })),
  }
}

export function checklistView(checklist, taskTypes) {
  const choicesByNct = new Map(checklist.thoaThuan.map((x) => [x.nctId, x.thoaThuan]))
  const typesByName = new Map(taskTypes.map((t) => [t.ten, t]))
  return {
    now: hhmm(checklist.taiLuc),
    tasks: checklist.congViec.map((cv) => toTaskView(cv, { choicesByNct, typesByName })),
    reasons: checklist.loaiLyDoKhongThucHien.filter((r) => r.trangThai === 'HIEU_LUC'),
  }
}

// Kết quả ngắn của việc đã đóng, từ CongViecChiTiet
export function resultText(detail) {
  if (!detail) return null
  if (detail.lyDo) return detail.lyDo
  const { ketQua } = detail
  if (!ketQua) return null
  const value = Object.values(ketQua.giaTriHienHanh ?? {}).join(', ')
  return [value, `lúc ${hhmm(ketQua.thoiDiemThucHien)}`, ketQua.nguoiThucHien].filter(Boolean).join(', ')
}
