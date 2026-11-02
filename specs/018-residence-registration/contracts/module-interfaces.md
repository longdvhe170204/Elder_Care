# Giao diện giữa module: Feature 018

**Ngày**: 2026-10-10 | **Plan**: [../plan.md](../plan.md)

Feature 018 thêm gói `admission.khaibao` vào module M02 (cùng 004). Giao diện dưới đây gồm phần dùng nội bộ M02, bản cài SPI và giao diện công khai (research Z-01, Z-02).

## 1. Nội bộ M02 (004 → 018)

| Nguồn | Dùng cho |
| --- | --- |
| Dịch vụ đọc hợp đồng của 004: loại lưu trú, ngày bắt đầu, ngày kết thúc dự kiến (kể cả phụ lục gia hạn), chuỗi hợp đồng của hồ sơ | `TongThoiGianLuuTru` (FR-001, FR-007, Q-241) |
| Sự kiện `TiepNhanHoanTat(nctId, hopDongId, loaiLuuTru, ngayBatDau, ngayKetThucDuKien)` | Mốc `HOAN_TAT_TIEP_NHAN` (người nội trú) |
| `PhuLucDaApDung(phuLucId, hopDongId, nctId, loaiThayDoi, ngayHieuLuc)` | Mốc `PHU_LUC_GIA_HAN` (khi chỉ có Thông báo lưu trú); mốc `NOI_TRU_HIEU_LUC` (đổi loại lưu trú sang nội trú, FR-001a) |
| `HopDongDaDoiTrangThai` (hợp đồng nội trú mới của người đang bán trú Hiệu lực) | Mốc `NOI_TRU_HIEU_LUC` (FR-001a) |

## 2. 018 cung cấp

| SPI / API | Bản cài hoặc chữ ký | Dùng bởi |
| --- | --- | --- |
| `ThamGiaChuyenTrangThai` (`resident.api`) | Không điều kiện chặn (SC-004). Tác động `KET_THUC_LUU_TRU`, `GHI_NHAN_QUA_DOI`, `HUY_TIEP_NHAN`: khai báo đang xử lý → Đã hủy; nghĩa vụ chưa đáp ứng → hủy | 001 H-02 |
| `DinhChinhDuocProvider` (000) | `M02.KHAI_BAO` (lần nộp, kết quả, xác nhận thường trú cùng xã/phường; không gắn tầng) | 000 FR-026 |
| `PhanLoaiTruongProvider`, `LuuTruProvider` (000) | Lưu theo hồ sơ (CFG-M15-03) | 000 |
| `QuyenTepProvider` (`common.tep`) | `M02.BANG_CHUNG_KHAI_BAO`: Hành chính, Quản lý viện | NFR-08 |
| `admission.api.KhaiBaoThongKeApi` (016) | `tongHop()`: số người cần khai báo (chưa lập hoặc Cần khai báo), số quá hạn, số Hết hạn, số cần khai báo xóa | FR-011 |

Sự kiện 018 phát: thông báo (cài `CoYeuCauThongBao`) tới 009 theo FR-010; `KhaiBaoDaDoiTrangThai(khaiBaoId, nctId, tu, den)` cho 016.

## 3. 018 cần từ module khác

| Giao diện | Module | Dùng cho |
| --- | --- | --- |
| `NguoiCaoTuoiApi.trangThai`, `laTrangThaiCuoi`; địa chỉ thường trú trên hồ sơ (hiển thị cho Hành chính đối chiếu) | 001 | FR-001, FR-008 |
| `NhatKyApi`, `ThamSoApi` (CFG-M02-09, 11 → 13), `DinhChinhApi`, kho tệp | 000 | Chung |
