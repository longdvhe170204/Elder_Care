# Data Model: Báo cáo và dashboard (016)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Feature 016 không sở hữu dữ liệu nghiệp vụ nhóm 1, 2, 3 (spec, Key Entities). Tài liệu này mô tả các mô hình đọc (không lưu), bản ghi duy nhất được ghi và cách bảo đảm các ràng buộc. Không phải script SQL.

---

## 1. Không có bảng nghiệp vụ

M14 không có bảng riêng trong cơ sở dữ liệu. Thứ được ghi:

| Bản ghi | Nơi lưu | Nguồn |
| --- | --- | --- |
| Nhật ký hành động "Xuất báo cáo" | Bảng nhật ký của 000 (nhóm 3, `NhatKyApi`) | FR-062 |
| Tệp đã xuất | Kho tệp `common.tep` của 000, loại `M14.BAO_CAO_XUAT`; quyền tải: người xuất, Quản lý viện | FR-061 |

Bộ đệm dashboard 20 giây (research O-06) là bộ nhớ trong tiến trình, không phải dữ liệu.

---

## 2. Mô hình đọc (không lưu)

| Mô hình | Trường | Ghi chú |
| --- | --- | --- |
| `ChiTieu` | mã, nhóm, tên, định nghĩa hiển thị, đối tượng đếm, điều kiện, mốc xếp khoảng, tử số, mẫu số, nguồn | Định nghĩa cố định trong mã (FR-003); danh sách ở contracts/chi-tieu.md |
| `GiaTriChiTieu` | mã, giá trị, tử số, mẫu số, nhãn (`KHONG_CO_DU_LIEU`, `CHUA_CAU_HINH`, `NGAY_CHUA_KET_THUC`, `CHO_XEM_LAI:n`, `LOAI_BO_LUU_TRU:loai`), tính đến | FR-007, FR-008 |
| `PhamViBaoCao` | vai trò, danh sách (tầng/khu hoặc đối tượng, khoảng thời gian), `PhamViLoc` thô | FR-012 (research O-03) |
| `DongChiTiet` | khóa bản ghi nguồn, feature nguồn, loại, trạng thái, mốc, tầng/khu, người cao tuổi (đầy đủ hoặc rút gọn Q-196), người thực hiện, giá trị theo giới hạn trường, cờ chờ xem lại | FR-009, FR-012 |
| `YeuCauXuat` | báo cáo, khoảng, bộ lọc, chế độ, có định danh, mục đích | FR-061, FR-062 |

---

## 3. Ràng buộc và cách bảo đảm

| Ràng buộc | Cách bảo đảm |
| --- | --- |
| Chỉ đọc (FR-001, SC-008) | Transaction `readOnly`; ArchUnit cấm gọi API lệnh; test đếm bản ghi |
| Số trên màn hình = số dòng chi tiết (FR-009, SC-006) | Hàm tính và hàm chi tiết dùng cùng tập bản ghi đã lọc |
| Không lộ ngoài phạm vi (FR-013, SC-004) | Lọc hai bước (O-03); bộ lọc chỉ giá trị trong phạm vi |
| Giới hạn trường (FR-014) | `@NhomTruong` trên DTO và cột file |
| Tệp chỉ giao khi có nhật ký (FR-062) | Ghi nhật ký trong transaction trước khi trả liên kết |
| Khoảng tối đa (FR-060) | Kiểm CFG-M14-01 ở mọi báo cáo và xuất |
| DBR-25 | Mốc, khoảng theo Asia/Ho_Chi_Minh |
