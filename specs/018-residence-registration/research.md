# Research: Khai báo tạm trú, lưu trú (018)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

Mỗi mục gồm Quyết định, Lý do, Phương án đã cân nhắc. Quyết định nền R-01 → R-14 của 000 và các mẫu đã chốt ở 001 → 017 (H-02 bean trạng thái, T-01 → T-15 của 004, B-02 khuôn thông báo) được dùng lại, không quyết lại. Các quyết định dưới đây là kỹ thuật; chúng không đổi nghiệp vụ (constitution IX). Chỗ spec chưa rõ hoặc mâu thuẫn được ghi ở plan, mục "Điểm cần báo lại".

---

## Z-01. Vị trí trong backend

**Quyết định**: Feature 018 là phần 6.10 của module M02 (UC-80, UC-81; BR-M02-11 → 14), thêm gói `com.cambae.backend.m02.khaibao` cạnh các gói của 004. Gói dùng dịch vụ đọc hợp đồng của 004 trong cùng M02 qua lớp nội bộ (ArchUnit cho phép giữa các gói M02), không qua `m02.api`.

**Lý do**: Constitution X chia gói theo M01–M15; Phụ lục 26.2 đặt UC-80, UC-81 ở 018, cùng Module 02 với 004 (mẫu 005 ↔ 014, 008 ↔ 015).

**Phương án loại**: module riêng ngoài M01–M15.

---

## Z-02. Chiều phụ thuộc

**Quyết định**: 018 dùng: nội bộ M02 (hợp đồng, phụ lục, loại lưu trú, ngày bắt đầu/kết thúc; sự kiện nội bộ `TiepNhanHoanTat`, `PhuLucDaApDung`, `HopDongDaDoiTrangThai`, `HoSoKetThucDaHoanTat`, `QuaDoiDaGhiNhan`); 001 (`NguoiCaoTuoiApi.trangThai`, `TrangThaiNguoiCaoTuoiApi` lịch sử, địa chỉ thường trú trên hồ sơ để Hành chính đối chiếu); 000 (tham số, nhật ký, đính chính, tệp). Cài bean `ThamGiaChuyenTrangThai` (`m01.api`) cho tác động khi trạng thái cuối. Phát thông báo (009); 016 đọc `KhaiBaoThongKeApi` (`m02.api`). Không có SPI mới.

---

## Z-03. Nghĩa vụ khai báo và hạn (FR-001, FR-001a, FR-002, FR-006, FR-007; Q-215, Q-241, Q-289, Q-306)

**Quyết định**:
- "Cần khai báo" là số liệu tính. Để nhắc đúng ngày và chạy bù, plan giữ một bảng đệm kỹ thuật `nghia_vu_khai_bao` (dựng lại được từ dữ liệu nguồn): người cao tuổi, mốc phát sinh (`HOAN_TAT_TIEP_NHAN`, `NOI_TRU_HIEU_LUC`, `PHU_LUC_GIA_HAN`, `HET_HAN`), thời điểm mốc, hạn (= mốc + CFG-M02-12 đọc lúc ghi mốc), khai báo đáp ứng (nếu đã lập), đã nộp hay chưa. Listener nội bộ M02 ghi mốc khi sự kiện nguồn xảy ra; job ghi mốc `HET_HAN`. Đây không phải bản ghi nghiệp vụ và không có lệnh (Q-306). *(R-15: `nghia_vu_khai_bao` → `trang_thai_nct`)*
- Hàm thuần `TongThoiGianLuuTru(chuoiHopDong)`: tìm chuỗi hợp đồng nối tiếp không gián đoạn trong cùng hồ sơ (hợp đồng sau bắt đầu ngày ngay sau ngày kết thúc hợp đồng trước), từ ngày bắt đầu hợp đồng đầu chuỗi (hoặc ngày hiệu lực phần nội trú, FR-001a) tới ngày kết thúc dự kiến mới nhất gồm phụ lục gia hạn; không có ngày kết thúc → vô hạn.
- Hàm thuần `LoaiKhaiBao(tong, cfg11, thuongTruCungXa)` → `DANG_KY_TAM_TRU` / `THONG_BAO_LUU_TRU`.
- Lệnh "Lập khai báo" kiểm: người nội trú, có nghĩa vụ chưa đáp ứng (hoặc khai báo gia hạn khi có Đăng ký tạm trú Đã xác nhận), không có khai báo đang xử lý; hạn khai báo của khai báo = hạn của nghĩa vụ (lập muộn không kéo dài).

---

## Z-04. Khai báo và vòng đời (FR-003 → FR-005; DBR-33)

**Quyết định**:
- `khai_bao_tam_tru` (nhóm 2; nội dung nộp, kết quả, xóa là nhóm 3 qua cột đặt một lần và đính chính 000): trạng thái `CAN_KHAI_BAO | DA_NOP | DA_XAC_NHAN | HET_HAN | DA_XOA | DUOC_THAY_THE | DA_HUY`. *(R-15: `khai_bao_tam_tru` → `ho_so_chung`)*
- DBR-33: cột sinh `khoa_dang_xu_ly = IF(trang_thai IN ('CAN_KHAI_BAO','DA_NOP'), nct_id, NULL)` `UNIQUE`; `khoa_xac_nhan = IF(trang_thai = 'DA_XAC_NHAN', nct_id, NULL)` `UNIQUE`. Ghi chấp nhận của khai báo mới chuyển khai báo Đã xác nhận cũ sang `DUOC_THAY_THE` trước trong cùng transaction (khóa `khai_bao_nct`). *(R-15: `khai_bao_nct` → `trang_thai_nct`)*
- Ghi bị từ chối: về `CAN_KHAI_BAO`, hạn mới = thời điểm ghi + CFG-M02-12; lần nộp, lý do lưu ở `khai_bao_lan_nop` (nhóm 3).
- Xác nhận "thường trú cùng xã/phường" (FR-001): cột `thuong_tru_cung_xa` + ghi chú; chuyển loại; cờ ở hồ sơ khai báo (`khai_bao_nct.thuong_tru_cung_xa`) để khai báo sau dùng loại Thông báo lưu trú; gỡ bằng đính chính khi còn Cần khai báo.
- Đính chính kết quả ghi nhầm: `DinhChinhDuocProvider` `M02.KHAI_BAO` (không gắn tầng).

---

## Z-05. Trạng thái cuối, khai báo xóa (FR-008; BR-M02-13)

**Quyết định**: Bean `ThamGiaChuyenTrangThai` (`@Order` cùng nhóm 004): không điều kiện chặn (SC-004); tác động `KET_THUC_LUU_TRU`, `GHI_NHAN_QUA_DOI`, `HUY_TIEP_NHAN`: khai báo `CAN_KHAI_BAO`/`DA_NOP` → `DA_HUY`, xóa nghĩa vụ chưa đáp ứng. "Cần khai báo xóa" = người ở trạng thái cuối có khai báo `DA_XAC_NHAN` hoặc `HET_HAN` (tính lúc hỏi). Lệnh "Ghi đã khai báo xóa" cho phép khi hồ sơ ở trạng thái cuối (ngoại lệ (3) BR-M01-05); bằng chứng bắt buộc. Không phải mục danh sách việc sau qua đời.

---

## Z-06. Bộ lập lịch và thông báo (FR-002, FR-006, FR-008, FR-010)

**Quyết định**: Job hằng ngày 06:00 (`ViecHangNgayM02KhaiBao`):
- nghĩa vụ quá hạn chưa nộp → nhắc Hành chính mỗi ngày; Quản lý viện một lần (ngày quá hạn đầu tiên);
- Đăng ký tạm trú Đã xác nhận còn ≤ CFG-M02-13 ngày tới hết hạn, chưa có khai báo gia hạn đang xử lý → nhắc mỗi ngày;
- qua ngày hết hạn chưa có khai báo gia hạn Đã xác nhận → `HET_HAN` (Bộ lập lịch), báo Hành chính, Quản lý viện; ghi nghĩa vụ `HET_HAN` nếu chưa có khai báo đang xử lý;
- cần khai báo xóa → nhắc mỗi CFG-M02-09.
Khóa `(loại nhắc, nct_id, ngày)` chống trùng; chạy bù khi khởi động. Mọi thông báo mức Nhẹ (FR-010), cài `CoYeuCauThongBao`. Không tạo khai báo (Q-306).

---

## Z-07. Quyền, kiểm thử

**Quyết định**: `ChucNang` `KHAI_BAO_TAM_TRU`: `THUC_HIEN` (Hành chính); `XEM` (Quản lý viện). Vai trò khác, người thân không có quyền (FR-009). Kiểm thử: test bảng `TongThoiGianLuuTru` (chuỗi nối tiếp, khoảng trống, hồ sơ mới Q-12, phần nội trú FR-001a, không ngày kết thúc), `LoaiKhaiBao`; test tích hợp MySQL `UNIQUE` DBR-33; đồng hồ giả lập cho CFG-M02-09, 11 → 13; test bean trạng thái không chặn Kết thúc lưu trú (SC-004). Tên test theo BR (`BR_M02_11_KhaiBaoIT`, `DBR_33_MotKhaiBaoIT`).
