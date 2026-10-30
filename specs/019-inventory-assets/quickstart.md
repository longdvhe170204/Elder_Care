# Quickstart: Kiểm chứng feature 019

> **Sửa 2026-10-29 (Q-392):** hệ thống không quản lý kho nguyên liệu. Các mục về kho (nguyên liệu, lô, phiếu nhập, xuất, đảo, kiểm kê, cảnh báo hạn dùng, tồn tối thiểu, `nutrition.kho`) trong tài liệu này không còn hiệu lực; xem spec 019 mục Cập nhật 2026-10-29.

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md)

Hướng dẫn dùng ở giai đoạn triển khai; giai đoạn thiết kế chưa có mã nên các lệnh chưa chạy được. Hợp đồng xem [contracts/api-019.yaml](contracts/api-019.yaml) và [contracts/module-interfaces.md](contracts/module-interfaces.md); bảng xem [data-model.md](data-model.md).

## 1. Điều kiện cần

- Như quickstart của 000 → 018; profile `demo` có đồng hồ giả lập.
- Dữ liệu mẫu:
  - bộ phận Bếp (Phụ trách PB = Nhân viên bếp B1), Chăm sóc (Phụ trách PC = Trưởng tầng TT2), Hành chính – Kế toán (Phụ trách HC); bộ phận "Kỹ thuật" mới chưa có Phụ trách; Nhân viên bếp B2; Kế toán KT; Quản lý viện QL; Bác sĩ BS;
  - nguyên liệu "Gạo" (tồn tối thiểu 50 kg), "Tôm" (dị nguyên tôm); lô Gạo L1 hạn 12/10, L2 hạn 30/10;
  - xe X1 (bộ phận Hành chính – Kế toán, hạn đăng kiểm 20/10); giường G201 (003) có tài sản liên kết;
  - CFG theo mặc định Phụ lục 25: CFG-M03-02 1 ngày, CFG-M03-09 30 ngày, CFG-M08-08 2 ngày, CFG-M08-09 hằng tháng, CFG-M11-07 10%, CFG-M11-08, CFG-M11-09 theo Phụ lục 25;
  - bản giả `NhiemVuApi` (bộ phận, Phụ trách bộ phận); 014 gọi `LichXeLenhApi` qua bộ phát thử.

## 2. Chạy

```text
backend/  ./mvnw verify
backend/  ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
frontend/ npm ci && npm test && npm run dev
```

Kết quả mong đợi: `verify` xanh, gồm Modulith `verify()` (M03 không phụ thuộc M04, M08, M09, M11; `YeuCauMuaSamPort` khai ở `roombedallocation.api`), ArchUnit port nội bộ M03, M08, M11, test cơ sở dữ liệu (data-model mục 6).

## 3. Kịch bản kiểm chứng (profile demo)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| Q1 | PB lập yêu cầu mua sắm nguyên liệu 2.000.000; gửi duyệt; QL duyệt giảm còn 1.800.000 có lý do | Đã duyệt, số được duyệt 1.800.000; PB được báo | US7, FR-008 |
| Q2 | KT lập yêu cầu mua sắm; QL lập cho bộ phận Bếp | 403 | US7, Q-293, Q-299 |
| Q3 | B2 lập phiếu nhập hàng mua không gắn yêu cầu; gắn yêu cầu Q1 | 409 `THIEU_YEU_CAU_MUA_SAM`; lưu được, dòng Không đạt không vào tồn; yêu cầu có nguồn gắn | US1, FR-002, FR-008c, SC-007 |
| Q4 | PB ghi đã mua 2.100.000 (vượt 16,7%) | 409 `VUOT_SO_DUOC_DUYET` kèm hướng dẫn yêu cầu bổ sung | FR-008a |
| Q5 | PB ghi đã mua 1.900.000 kèm chứng từ; KT xác nhận khớp khi tổng phiếu nhập 1.900.000 | Chờ đối chiếu → Đã đối chiếu | US7, FR-008b |
| Q6 | Người ghi đã mua tự đối chiếu | 409 `NGUOI_GHI_MUA_KHONG_DOI_CHIEU` | FR-008b |
| Q7 | KT ghi lệch; QL chấp nhận lệch | Lệch → Đã đối chiếu; PB, KT được báo | FR-008 |
| Q8 | PB hủy yêu cầu Đã duyệt đã có phiếu nhập gắn | 409 `DA_CO_NGUON_GAN` | Q-290 |
| Q9 | Bộ phận Kỹ thuật (chưa có Phụ trách) lập yêu cầu | Chặn `BO_PHAN_CHUA_CO_PHU_TRACH`; QL được nhắc chỉ định | Q-292 |
| Q10 | B2 xuất Gạo cho bữa trưa: danh sách lô hiện L1 trước L2; chọn L2 không lý do | L1 đứng đầu, không lô nào được chọn sẵn; 409 `THIEU_LY_DO_CHON_LO` | US2, FR-003, SC-002 |
| Q11 | Xuất cho bữa từ lô quá hạn | 409 `LO_QUA_HAN`; mục đích Hủy được | FR-005 |
| Q12 | Hai phiếu xuất đồng thời lấy hết lô L1 | Một thành công, một 409 `TON_AM` | FR-004, SC-001 |
| Q13 | Đảo phiếu nhập sai một dòng | Phiếu đảo trỏ gốc; tồn giảm đúng | FR-006 |
| Q14 | 05:00: L1 còn 2 ngày tới hạn; Gạo dưới tồn tối thiểu | PB, B2 nhận Nhẹ; nhắc PB lập yêu cầu mua sắm; không có yêu cầu tự tạo | US3, FR-009, SC-003 |
| Q15 | B2 lập phiếu kiểm kê có lệch không lý do; có lý do; B2 tự duyệt | 409; Chờ duyệt; 403 `TU_DUYET`; PB duyệt → điều chỉnh tồn | US3, FR-010 |
| Q16 | PB lập phiếu kiểm kê | Người duyệt là QL | FR-010 |
| ~~Q17~~ | *(Bỏ theo Q-392)* | — | — |
| Q18 | HC ghi tăng máy lọc nước gắn yêu cầu tài sản thiếu nguyên giá | 409 `THIEU_NGUYEN_GIA`; có nguyên giá → Sẵn sàng, kho tài sản | US4, FR-011, Q-294 |
| Q19 | 003 tạo giường G305 | Tài sản giường tạo cùng lệnh, Sẵn sàng, bộ phận Chăm sóc | US5, Q-238 |
| Q20 | PC đưa vào bảo trì G201 khi giường Trống; ghi kết quả Đạt | Giường Đang bảo trì → Trống (qua 003) | US5, FR-015, SC-004 |
| Q21 | TT2 báo hỏng G201 khi có người, mức "mất an toàn", tầng còn giường Trống | Hư hỏng ghi với dấu chờ chuyển người, giường không đổi; Khẩn cấp cho TT2, Người phụ trách ca | FR-015, Q-245 |
| Q22 | Như Q21 khi tầng hết giường Trống phù hợp | Thêm QL, Hành chính, kèm danh sách tầng khác | Q-256 |
| Q23 | TT2 gỡ dấu "đã sửa tại chỗ" mức mất an toàn không ghi nội dung đã sửa | 409; có nội dung, người sửa → gỡ được | Q-265 |
| Q24 | Chuyển vị trí G201 (đã từng có phân bổ) | 409 `GIUONG_DA_PHAN_BO` | FR-012, Q-243 |
| Q25 | PC lập yêu cầu thanh lý G201; QL duyệt | Đã thanh lý; giường Không sử dụng | FR-013, FR-015 |
| Q26 | X1 tới mốc 30 ngày trước hạn đăng kiểm; quá hạn | HC (Phụ trách) nhắc; dấu "quá hạn đăng kiểm", nhắc mỗi ngày | FR-014 |
| Q27 | TT2 đặt lịch X1 chồng giờ lịch khác; đặt khi X1 quá hạn đăng kiểm | 409 `CHONG_LICH_XE`; 409 `XE_QUA_HAN_DANG_KIEM_BAO_HIEM` | US6, FR-017, SC-005 |
| Q28 | 014 điểm danh rời viện chuyến dùng X1 không có lịch | 014 chặn "chưa có lịch xe" (`LichXeApi.coLichDaDat`) | FR-019 |
| Q29 | Có lịch; rời viện người đầu tiên; gia hạn giờ về chồng lịch kế tiếp; kết thúc về | Đang dùng; dời giờ về, không chặn, QL và người đặt lịch kế tiếp được báo; Đã hoàn thành | FR-018, Q-236 |
| Q30 | Hủy ghi nhận bản ghi rời viện cuối cùng | Lịch về Đã đặt | Q-240 |
| Q31 | Hủy lịch có người lái không có tài khoản | Báo người đặt, Phụ trách bộ phận quản lý xe; không báo người lái | Q-298 |
| Q32 | 014 dời chuyến sang giờ xe đã có lịch khác | `kiemTraDoi` trả "xe đã có lịch khác"; 014 chặn | Q-239 |
| Q33 | QL thực hiện phiếu kho; Bác sĩ xem tồn | 403; xem được | FR-020 |
| Q34 | *(Bổ sung 2026-10-11, Q-337)* Điều dưỡng D lập yêu cầu `THUOC` cho A (đơn Amlodipin nguồn viện), mức Gấp, gửi duyệt; thử gắn đơn gia đình gửi; thử lập loại nguyên liệu; QL duyệt; Phụ trách bộ phận Y tế ghi đã mua; gọi `YeuCauThuocCungUngPort` | Chờ duyệt, QL được báo; 409; 403; Chờ đối chiếu; port trả dòng kèm `boPhanId` của yêu cầu | US7 #8, FR-008d |
| Q35 | *(Bổ sung 2026-10-15, quyết định của chủ dự án)* Phụ trách bộ phận Bếp (không xem được `SUC_KHOE`) lập yêu cầu `THUOC` gắn đơn của A; Phụ trách bộ phận Y tế lập cùng yêu cầu; gửi dòng thiếu `donThuocId`; Điều dưỡng D lập cho người cao tuổi ngoài phạm vi phân công | 403, không được cấp thêm quyền xem; 201; 422; 403 | US7 #8, FR-008d, 19.3, BR-M15-01 |

## 4. Nhóm test tự động

| Nhóm | Test | Kịch bản |
| --- | --- | --- |
| Hàm thuần | `ThuTuLoTheoHanTest`, `KiemTraVuotTest`, `KhopChungTuTest`, `AnhXaTrangThaiGiuongTest`, `KiemTraDatLichTest` | Q4, Q5, Q10, Q20, Q27 |
| Kho | `BR_M08_17_TonKhoIT`, `BR_M08_18_HanDungIT`, `BR_M08_19_KiemKeIT`, `BR_M08_20_PhieuDaoIT`, `DBR_31_TonKhongAmIT` (SC-001: 200 phiếu) | Q10 → Q17 |
| Mua sắm | `BR_M11_17_MuaSamIT`, `BR_M11_18_VuotIT`, `BR_M11_19_DoiChieuIT`, `BR_M11_20_NhacIT`, `DBR_36_GanNguonIT`, `FR_008d_YeuCauThuocIT` | Q1 → Q9, Q34, Q35 |
| Tài sản | `BR_M03_15_GiuongTaiSanIT` (SC-004), `BR_M03_17_HanBaoTriIT`, `BR_M03_18_HuHongIT`, `DBR_32_TaiSanGiuongIT` | Q18 → Q26 |
| Lịch xe | `BR_M03_16_LichXeIT` (SC-005), `FR_018_TheoChuyenIT` | Q27 → Q32 |
| Đồng thời | Hai phiếu xuất chỗ cuối; hai lịch xe cùng giờ; báo hỏng cùng lúc với phân bổ | Q12, Q27 |
| Kiến trúc | `ApplicationModules.verify()`; ArchUnit: chỉ `roombedallocation.taisan` dùng port giường; job không tạo phiếu, yêu cầu, lịch (Q-306) | |
| Web (Vitest) | Màn xuất kho hiện lô theo thứ tự hạn dùng, không chọn sẵn; màn báo hỏng giường hiện tiêu chí "mất an toàn" | Q10, Q21 |

## 5. Tiêu chí hoàn tất

| SC | Cách đo |
| --- | --- |
| SC-001 | Bộ 200 phiếu so tính tay |
| SC-002 | Q10, Q11 |
| SC-003 | Q14, Q26 |
| SC-004 | Bất biến giường ↔ tài sản sau mọi kịch bản |
| SC-005 | Q27 → Q32 |
| SC-006 | Đo phiếu xuất 10 dòng ≤ 3 phút |
| SC-007 | Q1 → Q9, Q18 |
