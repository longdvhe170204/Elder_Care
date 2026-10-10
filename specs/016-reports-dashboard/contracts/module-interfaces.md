# Giao diện giữa module: Feature 016

**Ngày**: 2026-10-10 | **Plan**: [../plan.md](../plan.md)

Module M14 chỉ đọc qua giao diện thống kê và API đọc của module khác (research O-02); không cung cấp giao diện cho module nào. Danh mục chỉ tiêu ↔ giao diện: [chi-tieu.md](chi-tieu.md).

## 1. 016 dùng

| Giao diện | Module | Ghi chú |
| --- | --- | --- |
| `PhamViApi.boLoc`, `boLocLichSu`; `QuyenApi`; `QuyenXemTruongApi`; giới hạn trường (A-08) | 002 | Phạm vi, thu hẹp quyền (FR-010a), tên thuốc (FR-054) |
| `NhiemVuApi.truongTangDuocGiao`, `nguoiPhuTrachCa`; `CaTrucApi.caCuaNhanVien`, `caDangDienRa` | 002/008 | Phạm vi Trưởng tầng, Điều dưỡng, Người phụ trách ca (FR-012) |
| `NhatKyApi.ghi` (hành động "Xuất báo cáo"); tóm tắt loại bỏ (000 FR-051); `ThamSoApi` (CFG-M14-01, CFG-M15-07 và CFG hiển thị); kho tệp | 000 | FR-007, FR-060, FR-062 |
| `NguoiCaoTuoiThongKeApi` (bổ sung theo plan này), `TrangThaiNguoiCaoTuoiApi.khoangTrangThai` | 001 | Mục O-10 |
| `LuuTruThongKeApi` (bổ sung theo plan này), `HopDongDocApi`, `LuotVangApi` | 004 | Mục O-10 |
| `TinhTrangGiuongApi.demTheoTrangThai`, `GiuongThongKeApi` (bổ sung theo plan này) | 003 | Mục O-10 |
| `CongViecThongKeApi`, `CoMatBanTruApi` | 005 | |
| `ThuocThongKeApi` | 006 | |
| `ChiSoThongKeApi`, `CanhBaoThongKeApi`, `SuCoApi`, `KhoanhVungApi`, `GiayPhepCoSoApi`, danh mục loại sự cố (dấu "không thuộc sức khỏe") | 007 | |
| `NhanSuThongKeApi`, `CaTrucApi` | 008 | |
| `ChiPhiThongKeApi` | 010 | |
| `DinhDuongThongKeApi` | 011 | |
| `HoatDongThongKeApi` | 014 | |
| `XoayCaThongKeApi` | 015 | |
| `SoDuThongKeApi` | 017 | |
| `KhaiBaoThongKeApi` | 018 | |
| `TaiSanThongKeApi`, `KhoThongKeApi`, `MuaSamThongKeApi` | 019 | |

Không dùng ở giai đoạn này (Q-194): `NguoiThanThongKeApi` (012), `DoGuiThongKeApi` (013), thống kê thông báo (009).

## 2. Yêu cầu chung với mọi giao diện thống kê

1. Nhận `PhamViLoc` (002) và khoảng; trả bản ghi có: khóa nguồn, trạng thái do module nguồn xác định, mốc xếp khoảng (thời điểm thiết bị với bản ghi ngoại tuyến), tầng/khu theo 016 FR-011, cờ "chờ xem lại", không gồm bản ghi Hủy ghi nhận (hoặc gắn cờ để 016 đếm dòng tham khảo).
2. Chỉ đọc, không khóa; chạy được song song.
3. Có hàm đếm theo nhóm cho dashboard (không tải bản ghi) bên cạnh hàm trả luồng bản ghi cho báo cáo, chi tiết.

Plan của từng module giữ đúng ba yêu cầu này; điểm còn thiếu được ghi ở plan 016 mục "Đồng bộ".

## 3. Sự kiện

016 không nghe sự kiện và không phát sự kiện (ngoài bản ghi nhật ký của lần xuất).
