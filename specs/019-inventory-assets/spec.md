# Feature Specification: Kho nguyên liệu và tài sản của viện

**Feature Branch**: `019-inventory-assets`

**Created**: 2026-09-28

**Status**: Draft

**Input**: User description: "Kho nguyên liệu nấu ăn và tài sản của viện theo docs/business/nghiep-vu.md mục 12.7 (Module 08, BR-M08-17 → BR-M08-20) và 7.7 (Module 03, BR-M03-15 → BR-M03-18); quyết định Q-209, Q-210, Q-220; UC-89 → UC-92; DBR-31, DBR-32; CFG-M03-09, CFG-M08-08, CFG-M08-09. Quản lý viện quản lý kho và tài sản. Kho: danh mục nguyên liệu, phiếu nhập có kiểm tra đầu vào, phiếu xuất theo lô, đề nghị nhập của bếp, kiểm kê có duyệt, cảnh báo hạn dùng và tồn tối thiểu; không tự trừ kho theo thực đơn. Tài sản: xe đưa đón, giường, thiết bị lớn; trạng thái, vị trí, bảo trì, kiểm định, đăng kiểm; lịch dùng xe không chồng giờ; trạng thái tài sản của giường đi cùng trạng thái giường."

## Clarifications

### Session 2026-09-29

- Q: Lịch dùng xe chuyển sang "Đang dùng" và "Đã hoàn thành" bằng cách nào? → A: Với chuyến đi ngoài viện, hệ thống tự chuyển khi người đầu tiên được điểm danh rời viện và khi kết thúc điểm danh về (feature 014); với việc khác, người đặt hoặc Quản lý viện ghi "xuất phát" và "trả xe".
- Q: Khi giường đang có người nằm mà Trưởng tầng báo hỏng, hệ thống xử lý thế nào? → A: Vẫn ghi hư hỏng vào lịch sử tài sản với dấu "chờ chuyển người" và nhắc Trưởng tầng chuyển người; trạng thái tài sản và giường không đổi; sau khi chuyển người, Trưởng tầng báo hỏng lại để giường chuyển "Đang bảo trì". *(Bổ sung ở Session 2026-09-30: mức ảnh hưởng và gỡ dấu "đã sửa tại chỗ", Q-245.)*
- Q: Ai lập phiếu kiểm kê kho và ai duyệt điều chỉnh tồn? → A: Nhân viên bếp đếm và lập phiếu kiểm kê; Quản lý viện duyệt hoặc trả lại; người lập không tự duyệt.
- Q: Giường có ba cách "ngừng dùng" (Ngừng hiệu lực ở 003, Không sử dụng, Đã thanh lý); quan hệ thế nào? → A: Thanh lý tài sản là cách duy nhất ngừng dùng giường vĩnh viễn (giường Không sử dụng); feature 003 bỏ lệnh Ngừng hiệu lực giường; phòng chỉ Ngừng hiệu lực khi mọi giường đã Không sử dụng (Q-237).
- Q: Tạo giường (003) và ghi tăng tài sản giường, bên nào trước? → A: Lệnh tạo giường tự ghi tăng tài sản loại giường ở Sẵn sàng trong cùng một lần; Quản lý viện bổ sung thông tin tài sản sau (Q-238).
- Q: Chuyến đi dời sang giờ làm lịch xe chồng lịch khác của cùng xe thì sao? → A: Chặn lệnh dời chuyến, lý do "xe đã có lịch khác" (Q-239).
- Q: Mọi bản ghi rời viện của chuyến bị Hủy ghi nhận thì lịch xe về đâu? → A: Chuyến quay về Đã lên lịch, lịch xe quay về Đã đặt, bỏ thời điểm xuất phát thực tế (Q-240).
- Q: Lệnh "Đưa vào sử dụng tại vị trí" áp cho tài sản loại giường thế nào? → A: Không áp; tài sản giường chỉ có Sẵn sàng, Hỏng, Đang bảo trì, Đã thanh lý; việc giường có người lấy theo phân bổ của feature 003 (Q-242).
- Q: Giường đã từng có người nằm có được chuyển sang phòng khác không? → A: Không; vị trí tài sản giường luôn là phòng của giường và chỉ đổi khi giường chưa từng có phân bổ; có lịch sử thì tạo giường mới ở phòng mới và Thanh lý giường cũ (Q-243).

### Session 2026-09-30 (rà soát vận hành)

- Q: Báo hỏng giường đang có người có phân mức không? → A: Có. Người báo chọn mức ảnh hưởng; "mất an toàn" thì báo Khẩn cấp cho Trưởng tầng và Người phụ trách ca để chuyển giường gấp; "không mất an toàn" giữ như Q-232. Với cả hai mức, dấu "chờ chuyển người" gỡ được với lý do "đã sửa tại chỗ" khi giường còn người (Q-245).
- Q: Tạm đóng giường không vì hỏng thì làm thế nào? → A: Dùng trạng thái giường Tạm ngừng sử dụng của feature 003 (lệnh trên giường, chỉ Quản lý viện); trạng thái tài sản giữ nguyên; bảo trì trong lúc tạm ngừng xong thì giường quay về Tạm ngừng sử dụng (Q-246).

### Session 2026-10-01 (/speckit-clarify, checklist cross-feature)

- Q: Giường đang có người bị báo hỏng mức "mất an toàn" mà tầng không còn giường Trống phù hợp thì sao? → A: Thông báo Khẩn cấp gửi thêm cho Quản lý viện và Hành chính, kèm danh sách giường Trống phù hợp ở tầng khác (Q-256).
- Q: Tiêu chí chọn mức "mất an toàn" là gì, và gỡ dấu "đã sửa tại chỗ" ở mức này cần gì? → A: "Mất an toàn" khi hư hỏng có thể làm người nằm ngã, kẹt, bị thương, hoặc mất chức năng mà việc chăm sóc đang cần; không chắc thì chọn "mất an toàn". Gỡ dấu ở mức này bắt buộc ghi nội dung đã sửa và người, đơn vị sửa; ảnh không bắt buộc. Thông báo Khẩn cấp theo quy tắc chung của feature 009, gồm yêu cầu gọi điện khi chưa xác nhận (Q-265).

### Cập nhật 2026-10-06 (đồng bộ Q-280 → Q-288, quyết định của chủ dự án)

- **Q-284**: kho và tài sản không còn do Quản lý viện quản lý. Mỗi tài sản thuộc một **bộ phận quản lý** (13.1a); Phụ trách bộ phận thực hiện các lệnh trên tài sản; Thanh lý do Phụ trách bộ phận lập, Quản lý viện duyệt; Quản lý viện chuyển tài sản giữa bộ phận. Kho nguyên liệu thuộc bộ phận Bếp: Nhân viên bếp lập phiếu nhập, xuất, kiểm kê; Phụ trách bộ phận Bếp quản lý danh mục nguyên liệu và duyệt kiểm kê (Quản lý viện duyệt khi người lập là Phụ trách bộ phận Bếp). Đề nghị nhập bỏ, thay bằng **yêu cầu mua sắm** của bộ phận (15.10, UC-94, UC-95, BR-M11-17 → 20, CFG-M11-07 → 09, DBR-36, BF-18): Quản lý viện chỉ duyệt chi phí, bộ phận tự mua, Kế toán đối chiếu chứng từ. Thêm User Story 7, FR-008 (viết lại), FR-008a → FR-008c, SC-007; sửa User Story 1 → 5, FR-001 → FR-003, FR-009 → FR-014, FR-016, FR-020, FR-021, bảng giao tiếp, Key Entities, Assumptions.
- **Q-283**: Bác sĩ xem tồn kho thay Dinh dưỡng viên.
- **Q-282**: hư hỏng "phát hiện khi vệ sinh" đổi thành "phát hiện khi dọn giường"; hư hỏng do nhân viên báo theo BR-M03-18.
- Các câu trả lời cũ ở Session 2026-09-29 về người duyệt kiểm kê (Quản lý viện) và người bổ sung thông tin tài sản giường (Quản lý viện) chỉ còn là lịch sử.

### Cập nhật 2026-10-06, lượt 2 (đồng bộ Q-292 → Q-299, quyết định của chủ dự án sau rà soát tài liệu nguồn)

- **Q-292**: bỏ quyền Quản lý viện làm thay Phụ trách bộ phận. Bộ phận chưa có Phụ trách bộ phận thì lệnh kho, tài sản, yêu cầu mua sắm của bộ phận bị chặn và Quản lý viện được nhắc chỉ định. Sửa User Story 7 kịch bản 7, FR-008, ghi chú sau bảng FR-013, FR-020, FR-021, Edge Cases, SC-007.
- **Q-293**: Kế toán không được giao nhiệm vụ Phụ trách bộ phận; Kế toán không lập yêu cầu mua sắm trong mọi trường hợp. Sửa User Story 7 kịch bản 5, FR-008b, FR-020.
- **Q-294**: ghi tăng tài sản gắn yêu cầu mua sắm bắt buộc nguyên giá; Kế toán đối chiếu chứng từ với tổng nguyên giá. Sửa FR-011, FR-008b, bảng FR-013; thêm User Story 4 kịch bản 8.
- **Q-295**: lịch xe dùng được cho việc viện đưa người bán trú về (mục đích "khác", feature 012). Sửa FR-017.
- **Q-298**: hủy, dời lịch xe thì báo người đặt lịch và Phụ trách bộ phận quản lý xe; người lái chỉ được báo khi có tài khoản. Sửa FR-017, bảng FR-018, FR-021; thêm User Story 6 kịch bản 7.
- **Q-299**: Quản lý viện không thuộc bộ phận, không được giao Phụ trách bộ phận (feature 008 FR-001a). Sửa FR-020.

### Cập nhật 2026-10-10 (đồng bộ Q-307 cho phiếu xuất, sau /speckit-analyze)

- **Q-307, BR-M08-17**: hệ thống không đề xuất lô. Người xuất tự chọn lô; hệ thống hiển thị các lô còn tồn theo thứ tự hạn dùng (số liệu hiển thị, Q-307 (e)) và chặn dòng xuất từ lô hạn muộn hơn khi còn lô hạn sớm hơn, trừ khi có lý do. Sửa User Story 2 (mô tả, kịch bản 1), FR-003.

### Cập nhật 2026-10-10, lượt 2 (đồng bộ Q-313, Q-314)

- **Q-314**: thanh lý tài sản đi qua yêu cầu phê duyệt chung. Sửa bảng FR-013.
- **Q-313**: Trưởng tầng chỉ báo hỏng tài sản có vị trí thuộc tầng mình. Sửa bảng FR-013.

### Cập nhật 2026-10-11 (đồng bộ Q-337 → Q-345 sau đối chiếu tài liệu gốc DOCX)

Chủ dự án chốt Q-337 → Q-345 (docs/business/nghiep-vu.md 24.2) sau khi đối chiếu với tài liệu gốc DOCX "nghiep-vu-quan-ly-vien-duong-lao.docx" (lưu ngoài kho mã từ 2026-10-15; bản cũ ở commit a2ff357); các Q đã chốt trước được giữ. Thay đổi ở spec này:

- **Q-337**: yêu cầu mua sắm có thêm loại "thuốc" cho thuốc do viện cung cấp của từng người cao tuổi; Điều dưỡng lập, gửi, rút lại, hủy (FR-008d, bảng FR-008, FR-020); dòng gắn đơn thuốc nguồn viện (feature 006); lần tiếp nhận do feature 006 ghi; không tồn kho thuốc, không sinh chi phí cho người cao tuổi. Phạm vi, User Story 7 kịch bản 8, bảng giao tiếp (006), Key Entities, SC-007 sửa theo.

## Phạm vi

**Trong phạm vi** (nhóm chức năng "Dinh dưỡng và kho bếp" và "Tài sản và đồ gửi", 1.6; UC-89 → UC-92, UC-94, UC-95):

1. Danh mục nguyên liệu; phiếu nhập theo lô, có kết quả kiểm tra đầu vào; phiếu xuất cho bữa, xuất hủy; tồn theo lô; phiếu đảo (12.7, UC-91, BR-M08-17, 18, 20, DBR-31).
2. **(Sửa, 2026-10-06, Q-284)** Yêu cầu mua sắm của bộ phận: lập, gửi duyệt, Quản lý viện duyệt chi phí, ghi đã mua kèm chứng từ, Kế toán đối chiếu, xử lý lệch; gắn phiếu nhập, ghi tăng tài sản, bảo trì thuê ngoài với yêu cầu (15.10, UC-94, UC-95, BR-M11-17 → 20, CFG-M11-07 → 09, DBR-36, BF-18). Thay đề nghị nhập của Nhân viên bếp. **(Bổ sung, 2026-10-11, Q-337)** Yêu cầu loại "thuốc" cho thuốc do viện cung cấp của từng người cao tuổi, do Điều dưỡng lập (11.7, FR-008d).
3. Kiểm kê kho theo chu kỳ, phiếu điều chỉnh có duyệt (UC-92, BR-M08-19, CFG-M08-09).
4. Cảnh báo lô sắp hết hạn và nguyên liệu dưới tồn tối thiểu (BR-M08-18, CFG-M08-08).
5. Danh mục tài sản; trạng thái, vị trí và lịch sử; bảo trì, kiểm định, đăng kiểm, bảo hiểm; báo hỏng (7.7, UC-89, BR-M03-17, 18, CFG-M03-09, DBR-32).
6. Đồng bộ trạng thái tài sản của giường với trạng thái giường (BR-M03-15, Q-220).
7. Lịch sử dụng xe đưa đón; điều kiện có lịch xe cho chuyến đi dùng xe của viện (UC-90, BR-M03-16, Q-220).
8. **(Bổ sung, 2026-10-06, Q-284)** Bộ phận quản lý của tài sản (13.1a) và việc chuyển tài sản giữa bộ phận; kho nguyên liệu thuộc bộ phận Bếp.

**Ngoài phạm vi**:

- Quy tắc dùng chung: feature 000.
- Trạng thái giường, phân bổ, bảo trì giường theo nghĩa phân bổ, xác nhận giường sẵn sàng và hư hỏng phát hiện khi dọn giường (BR-M03-05, BR-M03-13; sửa, 2026-10-06, Q-282): feature 003. Spec này **đồng bộ** trạng thái tài sản với trạng thái giường và **nhận** hư hỏng.
- Thực đơn, chốt suất, phiếu bữa ăn: feature 011. Spec này không tự trừ kho theo định lượng món × số suất (Q-220).
- Chuyến đi ngoài viện, điểm danh rời/về: feature 014. Spec này cung cấp lịch xe và điều kiện "có lịch xe".
- Kho thuốc, vật tư y tế, vật phẩm tiêu hao tính phí (bỉm, tã, sữa); công nợ nhà cung cấp, khấu hao, hạch toán tài sản: ngoài hệ thống (1.2). Lần tiếp nhận thuốc cung ứng theo yêu cầu loại "thuốc": feature 006 FR-054 (bổ sung 2026-10-11, Q-337). *(Sửa, 2026-10-06, Q-284: yêu cầu mua sắm của bộ phận nay thuộc spec này; việc mua thực tế vẫn diễn ra ngoài hệ thống.)*
- Đồ gửi của người cao tuổi: feature 013.
- **(Bổ sung, 2026-10-06, Q-284)** Danh mục bộ phận, bộ phận của nhân viên, chỉ định Phụ trách bộ phận: feature 008 (FR-001a). Quyền theo nhiệm vụ Phụ trách bộ phận: feature 002 (FR-019f). Đề nghị mua hộ cho người cao tuổi: feature 010 (FR-023); yêu cầu mua sắm không sinh chi phí cho người cao tuổi.
- Gửi thông báo: feature 009. Dashboard "tài sản quá hạn bảo trì", "nguyên liệu dưới tồn tối thiểu" (18.5): feature 016.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Nhân viên bếp nhập kho nguyên liệu theo lô, gắn yêu cầu mua sắm, có kiểm tra đầu vào (Priority: P1) *(sửa 2026-10-06, Q-284)*

Nhân viên bếp lập phiếu nhập gắn với một yêu cầu mua sắm Đã duyệt của bộ phận Bếp (hàng được tặng hoặc chuyển từ nơi khác thì ghi nguồn): nhà cung cấp, ngày nhập, chứng từ mua, từng dòng nguyên liệu với số lượng, đơn giá, số lô, hạn dùng, kết quả kiểm tra đầu vào. Dòng "không đạt" không vào tồn. Phiếu đã xác nhận không sửa; sai sót bằng phiếu đảo.

**Why this priority**: Không có nhập kho thì không có tồn; kiểm tra đầu vào là bước 1 của kiểm thực ba bước với bếp ăn tập thể (12.7).

**Independent Test**: Lập phiếu nhập 3 dòng gắn yêu cầu mua sắm Đã duyệt, một dòng "không đạt"; kiểm tra tồn theo lô; lập phiếu đảo cho một dòng nhập nhầm.

**Acceptance Scenarios**:

1. **Given** danh mục có "Thịt lợn" (kg) và "Gạo" (kg), **When** Nhân viên bếp lập phiếu nhập gắn yêu cầu mua sắm MS-01 Đã duyệt của bộ phận Bếp: 20 kg thịt lô L1 hạn 03/10, 50 kg gạo lô G1 hạn 01/03/2027, 10 kg thịt lô L2 kiểm tra "không đạt – có mùi", **Then** tồn thịt lô L1 là 20 kg, gạo G1 là 50 kg; lô L2 không vào tồn và được ghi lý do (12.7, DBR-31).
2. **Given** phiếu nhập đã xác nhận, **When** bất kỳ ai tìm cách sửa hay xóa, **Then** không có thao tác đó; **When** Nhân viên bếp lập phiếu đảo cho dòng gạo nhập nhầm 50 thành 40, **Then** phiếu đảo trừ 50 kg lô G1 và phiếu nhập mới ghi 40 kg (BR-M08-20).
3. **Given** phiếu đảo làm tồn của lô âm (gạo G1 đã xuất 45 kg), **When** lưu, **Then** hệ thống chặn (BR-M08-17).
4. **Given** Nhân viên bếp, **When** lập phiếu nhập hàng mua mà không gắn yêu cầu mua sắm Đã duyệt của bộ phận Bếp, **Then** hệ thống chặn; **When** phiếu ghi nguồn "được tặng" kèm người tặng, **Then** được lưu không cần yêu cầu (BR-M11-17, DBR-36). **When** Quản lý viện tìm cách lập phiếu nhập, **Then** hệ thống từ chối (12.7, Phụ lục 27 ³⁴). *(Sửa 2026-10-06, Q-284.)*

---

### User Story 2 - Nhân viên bếp xuất nguyên liệu cho bữa (Priority: P1) *(sửa 2026-10-06, Q-284: bỏ đề nghị nhập)*

Nhân viên bếp lập phiếu xuất cho một bữa hoặc xuất hủy. Lô có hạn dùng sớm nhất phải được xuất trước: hệ thống hiển thị các lô còn tồn theo thứ tự hạn dùng, người xuất tự chọn lô; chọn lô khác lô hạn sớm nhất phải có lý do. Lô quá hạn chỉ được xuất hủy. Khi thiếu nguyên liệu, Phụ trách bộ phận Bếp lập yêu cầu mua sắm (User Story 7).

**Why this priority**: Xuất kho hằng ngày là thao tác thường xuyên nhất của kho bếp.

**Independent Test**: Hai lô thịt L1 (hạn 03/10), L3 (hạn 06/10); xuất cho bữa trưa 02/10; thử xuất lô quá hạn cho bữa.

**Acceptance Scenarios**:

1. **Given** tồn thịt L1 20 kg hạn 03/10 và L3 10 kg hạn 06/10, **When** bếp mở phiếu xuất thịt cho bữa trưa 02/10, chọn lô L1 và xuất 8 kg, **Then** danh sách lô hiện L1 trước L3, phiếu được ghi không cần lý do; tồn L1 còn 12 kg (BR-M08-17).
2. **Given** như trên, **When** bếp chọn lô L3 thay cho L1 mà không nhập lý do, **Then** hệ thống chặn; có lý do thì được.
3. **Given** lô L1 đã quá hạn ngày 04/10, **When** bếp xuất L1 cho bữa sáng 04/10, **Then** hệ thống chặn; **When** bếp xuất hủy L1 kèm lý do "hết hạn", **Then** được (BR-M08-18).
4. **Given** bếp xuất 30 kg khi tồn chỉ 22 kg, **When** lưu, **Then** hệ thống chặn (BR-M08-17).
5. *(Bỏ, 2026-10-06, Q-284)* Đề nghị nhập không còn; xem User Story 7.
6. **Given** Bác sĩ (sửa, 2026-10-06, Q-283), **When** xem kho, **Then** thấy tồn theo nguyên liệu và lô; không lập được phiếu (Phụ lục 27).

---

### User Story 3 - Kiểm kê kho và cảnh báo hạn dùng, tồn tối thiểu (Priority: P2)

Mỗi tháng Nhân viên bếp đếm và lập phiếu kiểm kê: ghi tồn thực tế từng lô, hệ thống tính chênh lệch; chênh lệch cần lý do và chỉ điều chỉnh tồn khi Phụ trách bộ phận Bếp duyệt phiếu, hoặc Quản lý viện khi người lập là Phụ trách bộ phận Bếp (sửa, 2026-10-06, Q-284). Mỗi ngày hệ thống báo các lô sắp hết hạn và nguyên liệu dưới tồn tối thiểu.

**Why this priority**: Giữ tồn trên hệ thống khớp thực tế và tránh dùng nguyên liệu hết hạn.

**Independent Test**: Kiểm kê với 2 lô lệch; đồng hồ giả lập qua mốc hạn dùng và tồn tối thiểu.

**Acceptance Scenarios**:

1. **Given** CFG-M08-09 mặc định \[hằng tháng\], **When** tới kỳ kiểm kê mà chưa có phiếu kiểm kê, **Then** Nhân viên bếp và Phụ trách bộ phận Bếp được nhắc (BR-M08-19).
2. **Given** tồn gạo G1 trên hệ thống 30 kg, thực tế 28 kg, **When** Nhân viên bếp B lập phiếu kiểm kê ghi 28 kg mà không có lý do, **Then** hệ thống chặn; có lý do "hao hụt" thì phiếu ở "Chờ duyệt", Phụ trách bộ phận Bếp P được báo; **When** P duyệt, **Then** tồn G1 thành 28 kg qua một điều chỉnh −2 kg (BR-M08-19, DBR-31; Clarification 2026-09-29).
3. **Given** phiếu kiểm kê của B "Chờ duyệt", **When** P trả lại kèm lý do "đếm lại lô G1", **Then** phiếu chuyển "Bị trả lại", B được báo và lập phiếu mới; **When** B tìm cách duyệt phiếu của chính mình, **Then** hệ thống chặn. **Given** phiếu do chính P lập, **When** P tìm cách duyệt, **Then** hệ thống chặn; chỉ Quản lý viện duyệt được (12.7, Q-284).
4. **Given** lô L3 hạn dùng 06/10, CFG-M08-08 mặc định \[2 ngày\], **When** Bộ lập lịch chạy ngày 04/10, **Then** Phụ trách bộ phận Bếp và Nhân viên bếp được báo lô L3 sắp hết hạn (BR-M08-18).
5. **Given** "Gạo" có mức tồn tối thiểu 20 kg và tổng tồn còn 15 kg, **When** Bộ lập lịch chạy, **Then** Phụ trách bộ phận Bếp và Nhân viên bếp được báo, kèm nhắc lập yêu cầu mua sắm (BR-M08-18; sửa, Q-284).

---

### User Story 4 - Phụ trách bộ phận quản lý tài sản, bảo trì và báo hỏng (Priority: P2) *(sửa 2026-10-06, Q-284)*

Mỗi tài sản thuộc một bộ phận quản lý (13.1a). Phụ trách bộ phận quản lý tài sản ghi tăng tài sản (tài sản mua mới gắn yêu cầu mua sắm Đã duyệt của bộ phận), đưa vào sử dụng tại vị trí, chuyển vị trí, lập bảo trì, ghi kết quả và lập yêu cầu thanh lý; Quản lý viện duyệt thanh lý và chuyển tài sản giữa bộ phận. Trưởng tầng báo hỏng, đưa vào bảo trì tài sản trong tầng mình. Hệ thống nhắc Phụ trách bộ phận trước hạn bảo trì, kiểm định, đăng kiểm, bảo hiểm.

**Why this priority**: Góp ý nghiệp vụ Q-209: tài sản khác kho và khác đồ gửi, cần theo dõi trạng thái và bảo trì.

**Independent Test**: Ghi tăng một xe và một máy tạo oxy; báo hỏng máy tạo oxy; bảo trì đạt; qua mốc hạn đăng kiểm của xe; thanh lý một tài sản.

**Acceptance Scenarios**:

1. **Given** Phụ trách bộ phận Y tế Y ghi tăng máy tạo oxy mã TS-015 (bộ phận Y tế) gắn yêu cầu mua sắm MS-05 Đã duyệt, **When** đưa vào sử dụng tại phòng 203, **Then** tài sản "Đang sử dụng", vị trí phòng 203, lịch sử vị trí có một dòng (7.7, DBR-32).
2. **Given** TS-015 ở tầng 2, **When** Trưởng tầng tầng 2 báo hỏng kèm mô tả, **Then** TS-015 chuyển "Hỏng", Phụ trách bộ phận Y tế được báo (BR-M03-18); **When** Trưởng tầng tầng 1 tìm cách báo hỏng TS-015, **Then** hệ thống từ chối (ngoài phạm vi).
3. **Given** TS-015 Hỏng, **When** Y đưa vào bảo trì rồi ghi kết quả "đạt" kèm đơn vị thực hiện, **Then** TS-015 chuyển "Sẵn sàng"; ghi kết quả "không đạt" thì về "Hỏng". Bảo trì thuê ngoài có chi phí thì việc ghi kết quả bị chặn khi chưa gắn yêu cầu mua sắm Đã duyệt của bộ phận Y tế (BR-M11-17).
4. **Given** xe XE-01 hạn đăng kiểm 30/11, CFG-M03-09 mặc định \[30 ngày\], **When** tới 31/10, **Then** Phụ trách bộ phận Hành chính – Kế toán (bộ phận quản lý xe) được nhắc; qua 30/11 mà chưa ghi lần đăng kiểm mới, **Then** XE-01 mang dấu "quá hạn đăng kiểm", nhắc mỗi ngày và không đặt lịch được (BR-M03-16, 17).
5. **Given** khi dọn giường G5 phát hiện "tay vịn giường gãy" (feature 003 BR-M03-13), **When** Trưởng tầng báo hư hỏng gắn với tài sản giường G5, **Then** hư hỏng được ghi vào lịch sử tài sản G5 (BR-M03-18). *(Sửa 2026-10-06, Q-282: trước đây là công việc vệ sinh ghi hư hỏng.)*
6. **Given** tài sản cũ không dùng nữa, **When** Phụ trách bộ phận lập yêu cầu thanh lý kèm lý do, **Then** trạng thái chưa đổi; **When** Quản lý viện duyệt, **Then** tài sản "Đã thanh lý", không đổi trạng thái được nữa (7.7, Q-284).
7. **(Bổ sung, 2026-10-06, Q-284)** **Given** máy đo thuộc bộ phận Y tế, **When** Quản lý viện chuyển sang bộ phận Chăm sóc mà không có lý do, **Then** hệ thống chặn; có lý do thì được, lịch sử ghi bộ phận cũ, mới; **When** Phụ trách bộ phận Y tế tìm cách chuyển tài sản sang bộ phận khác, **Then** hệ thống từ chối (7.7).
8. **(Bổ sung, 2026-10-06, Q-294)** **Given** yêu cầu mua sắm MS-05 loại tài sản Đã duyệt của bộ phận Y tế, **When** Y ghi tăng máy tạo oxy gắn MS-05 mà không nhập nguyên giá, **Then** hệ thống chặn; **When** Y ghi tăng xe lăn được tặng không có nguyên giá, ghi nguồn "được tặng", **Then** được lưu (7.7, BR-M11-17).

---

### User Story 5 - Giường là tài sản: trạng thái đi cùng trạng thái giường (Priority: P2)

Mỗi giường của feature 003 có đúng một bản ghi tài sản. Đưa tài sản giường vào bảo trì hoặc báo hỏng làm giường chuyển "Đang bảo trì"; thanh lý làm giường "Không sử dụng". Khi giường đang có người, việc đổi trạng thái bị chặn như BR-M03-05, nhưng hư hỏng vẫn được ghi và Trưởng tầng được nhắc chuyển người.

**Why this priority**: Tránh hai nguồn trạng thái mâu thuẫn cho cùng một giường (Q-220).

**Independent Test**: Giường G5 trống và G6 có người; báo hỏng cả hai; ghi kết quả bảo trì đạt cho G5.

**Acceptance Scenarios**:

1. **Given** giường G5 Trống, **When** Trưởng tầng báo hỏng tài sản G5, **Then** tài sản "Hỏng" và giường G5 "Đang bảo trì" trong cùng lần (BR-M03-15).
2. **Given** giường G6 Đang sử dụng, **When** Trưởng tầng báo hỏng tài sản G6, **Then** trạng thái tài sản và giường không đổi; hư hỏng được ghi với dấu "chờ chuyển người"; Trưởng tầng được nhắc chuyển người sang giường khác (BR-M03-05, BR-M03-15). **When** người đã được chuyển và G6 trống, **Then** trạng thái không tự đổi; **When** Trưởng tầng báo hỏng lại G6, **Then** tài sản "Hỏng", giường "Đang bảo trì", dấu "chờ chuyển người" được gỡ (Clarification 2026-09-29).
3. **Given** G5 Đang bảo trì do hỏng, **When** Phụ trách bộ phận Chăm sóc ghi kết quả bảo trì đạt, **Then** tài sản "Sẵn sàng" và giường về Trống; feature 003 phát sự kiện "giường chuyển Trống" (BR-M02-01).
4. **Given** giường G7 có phân bổ tương lai, **When** Quản lý viện duyệt yêu cầu thanh lý tài sản G7 do Phụ trách bộ phận Chăm sóc lập, **Then** giường "Không sử dụng" và phân bổ tương lai xử lý như Q-51 của feature 003 (hủy, báo hành chính và trưởng tầng).
5. **Given** giường G5, **When** Phụ trách bộ phận Chăm sóc chuyển vị trí tài sản G5 khi G5 không có phân bổ hiện tại hay tương lai, **Then** được; có phân bổ thì hệ thống chặn (7.7).
6. **(Bổ sung, 2026-09-30)** **Given** giường G8 Đang sử dụng, **When** Trưởng tầng báo hỏng tài sản G8 với mức "mất an toàn", **Then** hư hỏng được ghi với dấu "chờ chuyển người", Trưởng tầng và Người phụ trách ca nhận thông báo Khẩn cấp (Q-245). **When** Trưởng tầng ghi "đã sửa tại chỗ" khi người vẫn nằm, **Then** dấu được gỡ, trạng thái tài sản và giường không đổi.
7. **(Bổ sung, 2026-09-30)** **Given** giường G9 Tạm ngừng sử dụng, **When** Phụ trách bộ phận Chăm sóc đưa tài sản G9 vào bảo trì rồi ghi kết quả đạt, **Then** giường về Tạm ngừng sử dụng, không kích hoạt BR-M02-01 (Q-246).

---

### User Story 6 - Đặt lịch dùng xe đưa đón (Priority: P2)

Trưởng tầng, Hành chính hoặc Quản lý viện đặt lịch dùng xe cho chuyến đi ngoài viện, đưa đi khám, chuyển viện không cấp cứu hoặc việc khác. Lịch không chồng giờ trên cùng xe; xe hỏng, bảo trì hoặc quá hạn đăng kiểm, bảo hiểm không đặt được. Chuyến đi ngoài viện dùng xe của viện phải có lịch xe trước khi điểm danh rời viện.

**Why this priority**: Xe là tài sản dùng chung nhiều nhất; trùng lịch xe làm hỏng chuyến đi và việc đưa đi khám.

**Independent Test**: Đặt hai lịch chồng giờ trên XE-01; đặt lịch cho xe đang bảo trì; điểm danh rời viện một chuyến chưa có lịch xe.

**Acceptance Scenarios**:

1. **Given** XE-01 có lịch 07:00–13:00 ngày 06/10 cho chuyến đi C1, **When** Hành chính đặt XE-01 10:00–11:00 cùng ngày đưa A đi khám, **Then** hệ thống chặn vì chồng giờ (BR-M03-16, DBR-32).
2. **Given** XE-02 đang bảo trì, **When** đặt lịch XE-02, **Then** hệ thống chặn.
3. **Given** chuyến đi C2 khai báo phương tiện "xe của viện" và chưa có lịch xe, **When** trưởng đoàn điểm danh rời viện (feature 014), **Then** hệ thống chặn và nêu lý do "chưa có lịch xe" (Q-220).
4. **Given** lịch xe của chuyến C1, **When** người đầu tiên của C1 được điểm danh rời viện, **Then** lịch chuyển "Đang dùng"; **When** "Kết thúc điểm danh về" của C1, **Then** lịch chuyển "Đã hoàn thành".
5. **Given** lịch xe đưa A đi khám (không gắn chuyến đi), **When** người đặt ghi "xuất phát" rồi "trả xe", **Then** lịch chuyển "Đang dùng" rồi "Đã hoàn thành" với thời điểm thực tế.
6. **Given** chuyến C1 bị hủy, **When** feature 014 báo hủy chuyến, **Then** lịch xe của C1 chuyển "Đã hủy".
7. **(Bổ sung, 2026-10-06, Q-298)** **Given** lịch xe XE-01 do Trưởng tầng T đặt, người lái là nhân viên L không có tài khoản, **When** Quản lý viện dời giờ lịch, **Then** T và Phụ trách bộ phận Hành chính – Kế toán (bộ phận quản lý xe) được báo, lịch hiện người lái L để viện báo ngoài hệ thống; **Given** người lái có tài khoản, **When** lịch bị hủy, **Then** người lái cũng được báo (7.7).

---

### User Story 7 - Bộ phận lập yêu cầu mua sắm; Quản lý viện duyệt chi phí; Kế toán đối chiếu chứng từ (Priority: P1) *(bổ sung 2026-10-06, Q-284)*

Khi bộ phận cần chi tiền (mua nguyên liệu, mua tài sản, thuê sửa chữa, bảo trì), Phụ trách bộ phận lập yêu cầu mua sắm và gửi duyệt. Quản lý viện chỉ duyệt về chi phí, được giảm số tiền. Bộ phận tự mua ngoài hệ thống, nhập hàng vào hệ thống gắn với yêu cầu, rồi ghi đã mua kèm chứng từ. Kế toán đối chiếu chứng từ với số đã ghi và hàng đã nhập. Yêu cầu mua sắm không sinh chi phí cho người cao tuổi.

**Why this priority**: Không có yêu cầu mua sắm Đã duyệt thì không lập được phiếu nhập kho hay ghi tăng tài sản mua mới (BR-M11-17), nên User Story 1, 4 phụ thuộc story này.

**Independent Test**: Bộ phận Bếp lập yêu cầu 2.000.000 đồng; Quản lý viện duyệt giảm còn 1.800.000 đồng; nhập kho gắn yêu cầu; ghi đã mua 1.950.000 đồng (dưới 10%) rồi thử 2.100.000 đồng; Kế toán đối chiếu; đồng hồ giả lập qua CFG-M11-08, CFG-M11-09.

**Acceptance Scenarios**:

1. **Given** Phụ trách bộ phận Bếp P, **When** P lập yêu cầu loại "nguyên liệu" gồm 50 kg gạo, 20 kg thịt, tổng dự kiến 2.000.000 đồng, lý do "gạo dưới tồn tối thiểu", ngày cần 10/10, rồi gửi duyệt, **Then** yêu cầu chuyển Chờ duyệt, Quản lý viện được báo; **When** P tìm cách sửa dòng, **Then** không có thao tác sửa; P Rút lại về Nháp để sửa (15.10).
2. **Given** yêu cầu Chờ duyệt, **When** Quản lý viện duyệt và giảm số được duyệt xuống 1.800.000 đồng mà không có lý do, **Then** hệ thống chặn; có lý do thì yêu cầu Đã duyệt với số được duyệt 1.800.000 đồng, P được báo; **When** Quản lý viện từ chối kèm lý do, **Then** yêu cầu Từ chối (trạng thái cuối).
3. **Given** yêu cầu Đã duyệt 1.800.000 đồng, CFG-M11-07 mặc định \[10%\], **When** P ghi đã mua 1.950.000 đồng kèm ảnh hóa đơn, nhà cung cấp, ngày mua, **Then** yêu cầu chuyển Chờ đối chiếu; **When** P ghi 2.100.000 đồng, **Then** hệ thống chặn và hướng P lập yêu cầu bổ sung cho phần vượt, tham chiếu yêu cầu gốc; **When** P ghi đã mua không có chứng từ, **Then** hệ thống chặn (BR-M11-18).
4. **Given** yêu cầu Chờ đối chiếu với số thực tế 1.950.000 đồng, chứng từ 1.950.000 đồng và các phiếu nhập gắn yêu cầu tổng 1.950.000 đồng, **When** Kế toán xác nhận khớp, **Then** yêu cầu Đã đối chiếu; **When** chứng từ ghi 1.900.000 đồng, **Then** Kế toán ghi lệch kèm lý do và số lệch, yêu cầu chuyển Lệch, Quản lý viện được báo; **When** Quản lý viện chấp nhận lệch hoặc yêu cầu bộ phận hoàn tiền kèm lý do, **Then** yêu cầu Đã đối chiếu (BR-M11-19, 15.10).
5. **Given** chứng từ bị thiếu, **When** Kế toán trả lại kèm lý do, **Then** yêu cầu về Đã duyệt để P ghi đã mua lại. ~~**Given** Kế toán K đang là Phụ trách bộ phận Hành chính – Kế toán và chính K ghi đã mua, **When** K tìm cách đối chiếu yêu cầu đó, **Then** hệ thống chặn (Phụ lục 27 ³⁸).~~ **(Sửa, 2026-10-06, Q-293)** **Given** Kế toán K, **When** K tìm cách lập yêu cầu mua sắm, **Then** hệ thống từ chối (Kế toán không được giao nhiệm vụ Phụ trách bộ phận, BR-M11-19, Phụ lục 27 ³⁸).
6. **Given** yêu cầu Đã duyệt từ 01/10, CFG-M11-08 mặc định \[30 ngày\], **When** tới 31/10 chưa ghi đã mua, **Then** P được nhắc; **Given** yêu cầu Chờ đối chiếu từ 01/10, CFG-M11-09 mặc định \[7 ngày\], **When** tới 08/10 chưa đối chiếu, **Then** Kế toán được nhắc (BR-M11-20).
7. **Given** Nhân viên chăm sóc không giữ nhiệm vụ Phụ trách bộ phận, **When** tìm cách lập yêu cầu mua sắm, **Then** hệ thống từ chối; ~~**Given** bộ phận Y tế chưa có Phụ trách bộ phận, **When** Quản lý viện lập yêu cầu thay, **Then** được~~ **(Sửa, 2026-10-06, Q-292)** **Given** bộ phận mới tạo chưa có Phụ trách bộ phận, **When** Quản lý viện tìm cách lập yêu cầu mua sắm, ghi tăng tài sản hay duyệt phiếu kiểm kê thay bộ phận đó, **Then** hệ thống từ chối và nhắc Quản lý viện chỉ định Phụ trách bộ phận (13.1a, Phụ lục 27 ³⁷).
8. **(Bổ sung, 2026-10-11, Q-337)** **Given** điều dưỡng D (không là Phụ trách bộ phận) thấy thuốc Amlodipin nguồn viện của A sắp hết, **When** D lập yêu cầu loại "thuốc" gồm A, đơn Amlodipin Hiệu lực, Amlodipin 5 mg viên nén, 30 viên, đơn giá dự kiến, mức ưu tiên Gấp, cần trước 12/10, rồi gửi duyệt, **Then** yêu cầu chuyển Chờ duyệt và Quản lý viện được báo; **When** D gắn dòng với đơn nguồn gia đình gửi hoặc đơn Đã ngừng, **Then** hệ thống chặn; **When** D tìm cách lập yêu cầu loại "nguyên liệu", **Then** hệ thống từ chối (chỉ Phụ trách bộ phận); **When** Quản lý viện duyệt và Phụ trách bộ phận Y tế ghi đã mua kèm chứng từ, **Then** yêu cầu Chờ đối chiếu và D ghi được lần tiếp nhận ở feature 006 (FR-054); yêu cầu không sinh chi phí cho A.

---

### Edge Cases

- Nguyên liệu Ngừng hiệu lực khi còn tồn: vẫn xuất được tới hết tồn, không nhập thêm.
- Phiếu xuất cho bữa đã qua: vẫn lập được; phiếu ghi cả thời điểm lập và bữa được chọn, để đối chiếu khi kiểm kê.
- Hai người cùng xuất một lô gần hết: chỉ phiếu lưu trước thành công nếu tồn không đủ cho cả hai (DBR-31).
- Tài sản chưa có vị trí (mới mua, để kho): trạng thái "Sẵn sàng", vị trí "kho tài sản".
- Xe gia hạn đăng kiểm trong ngày đã có lịch: dấu "quá hạn" gỡ khi ghi lần đăng kiểm mới; lịch đã đặt trước đó không bị hủy tự động nếu hạn mới bao trùm.
- Lịch xe quá giờ về dự kiến mà chưa trả xe: nhắc người đặt; không chặn lịch kế tiếp đã đặt nhưng báo xung đột cho Quản lý viện.
- **(Bổ sung, 2026-10-06, Q-284)** Hàng được tặng hoặc chuyển từ nơi khác: nhập kho, ghi tăng tài sản không cần yêu cầu mua sắm nhưng phải ghi nguồn; không có chứng từ mua (BR-M11-17).
- **(Bổ sung, 2026-10-06, Q-284)** Một yêu cầu mua sắm được thực hiện bằng nhiều phiếu nhập: tổng tiền các phiếu nhập gắn yêu cầu là số đã mua của yêu cầu; Kế toán đối chiếu với tổng này (BR-M11-19).
- **(Bổ sung, 2026-10-06, Q-292)** Bộ phận chưa có Phụ trách bộ phận (mới tạo): mọi lệnh dành cho Phụ trách bộ phận của bộ phận đó bị chặn, không ai làm thay; Nhân viên bếp vẫn lập phiếu xuất, kiểm kê (phiếu kiểm kê chờ tới khi có người duyệt). Phụ trách bộ phận nghỉ việc, bị khóa tài khoản hoặc chuyển bộ phận thì feature 008 bắt buộc chỉ định người thay trong cùng lệnh, nên bộ phận đang hoạt động không rơi vào trường hợp này.
- **(Bổ sung, 2026-10-06, Q-284)** Yêu cầu Đã duyệt nhưng bộ phận không mua nữa: Phụ trách bộ phận Hủy kèm lý do; phiếu nhập đã gắn yêu cầu thì không hủy được yêu cầu, phải ghi đã mua cho phần đã nhập. *(Q-290)*

## Requirements *(mandatory)*

### Functional Requirements

Mọi yêu cầu dưới đây kế thừa feature 000. Phân nhóm dữ liệu:

| Dữ liệu | Nhóm (1.5) | Thao tác được phép |
| --- | --- | --- |
| Danh mục nguyên liệu, danh mục loại tài sản | 1 – Danh mục | Tạo, sửa, Ngừng hiệu lực (danh mục nguyên liệu: Phụ trách bộ phận Bếp; danh mục loại tài sản: Quản lý viện; sửa, Q-284) |
| Phiếu nhập, phiếu xuất, phiếu đảo | 3 | Chỉ ghi thêm; sai sót bằng phiếu đảo (BR-M08-20) |
| Lô nguyên liệu, tồn | 2 – dẫn xuất | Chỉ thay đổi qua phiếu nhập, xuất, đảo, điều chỉnh kiểm kê đã duyệt (DBR-31) |
| Yêu cầu mua sắm (thay đề nghị nhập, Q-284) | 2 → 3 khi ở trạng thái cuối | Theo bảng FR-008 |
| Phiếu kiểm kê | 2 → 3 khi Đã duyệt | Theo bảng FR-010 |
| Tài sản | 1 (thông tin mô tả) và 2 (trạng thái, vị trí) | Thông tin mô tả sửa được; trạng thái, vị trí chỉ qua lệnh ở bảng FR-013 (7.7) |
| Lịch sử vị trí, lần bảo trì, kiểm định, đăng kiểm, hư hỏng | 3 | Chỉ ghi thêm; sai sót bằng đính chính |
| Lịch sử dụng xe | 2 | Theo bảng FR-018 |
| Tham số CFG-M03-09, CFG-M08-08, CFG-M08-09, CFG-M11-07 → CFG-M11-09 | 1 – Tham số | Cấu hình (Quản lý viện) |

#### A. Kho nguyên liệu

- **FR-001**: Phụ trách bộ phận Bếp (sửa, 2026-10-06, Q-284) MUST quản lý danh mục nguyên liệu: tên, nhóm, đơn vị tính, thành phần gây dị ứng (chọn từ danh mục dị nguyên của feature 001), mức tồn tối thiểu, trạng thái Đang dùng / Ngừng hiệu lực. Nguyên liệu Ngừng hiệu lực MUST NOT được nhập thêm nhưng vẫn xuất được tới hết tồn. *(Nguồn: 12.7)*
- **FR-002**: Nhân viên bếp (sửa, 2026-10-06, Q-284) MUST lập được phiếu nhập gồm: ngày nhập, nhà cung cấp (văn bản), yêu cầu mua sắm Đã duyệt của bộ phận Bếp mà phiếu thực hiện hoặc nguồn "được tặng" / "chuyển từ nơi khác" (ghi nguồn), chứng từ mua (hóa đơn, phiếu bán hàng, ảnh chụp; với hàng mua), các dòng (nguyên liệu, số lượng > 0, đơn giá, số lô, hạn dùng, kết quả kiểm tra đầu vào Đạt / Không đạt, ghi chú). Dòng Không đạt MUST có lý do và MUST NOT vào tồn. Phiếu có hiệu lực khi lưu (nhóm 3). Phiếu nhập hàng mua không gắn yêu cầu mua sắm Đã duyệt của bộ phận Bếp MUST NOT lưu được (FR-008c). *(Nguồn: 12.7; BR-M11-17, DBR-36)*
- **FR-003**: Nhân viên bếp MUST lập được phiếu xuất (sửa, 2026-10-06, Q-284: Quản lý viện không thực hiện lệnh kho) gồm: ngày, mục đích (cho bữa: ngày, bữa; hủy do hết hạn hoặc hỏng; khác, bắt buộc ghi chú), các dòng (nguyên liệu, lô, số lượng), người xuất. **(Sửa, 2026-10-10, Q-307)** Người xuất tự chọn lô; hệ thống MUST hiển thị các lô còn tồn theo thứ tự hạn dùng (số liệu hiển thị) và MUST chặn dòng xuất từ lô có hạn muộn hơn khi còn lô hạn sớm hơn, trừ khi người xuất ghi lý do. Hệ thống MUST NOT tự chọn hay đề xuất lô. *(Nguồn: 12.7, BR-M08-17, Q-220, Q-307)*
- **FR-004**: Tồn của mỗi lô MUST bằng nhập − xuất ± điều chỉnh đã duyệt, và MUST NOT âm; phiếu hoặc phiếu đảo làm một lô âm MUST bị chặn toàn bộ (feature 000 "hoặc toàn bộ, hoặc không"). Hai phiếu đồng thời trên cùng lô MUST cho kết quả như khi lưu lần lượt. *(Nguồn: BR-M08-17, DBR-31)*
- **FR-005**: Lô đã quá hạn dùng MUST NOT được xuất cho bữa; chỉ được xuất với mục đích "hủy". *(Nguồn: BR-M08-18)*
- **FR-006**: Phiếu nhập, phiếu xuất MUST NOT bị sửa hay xóa. Sai sót MUST xử lý bằng phiếu đảo trỏ đúng một phiếu gốc (toàn bộ hoặc một số dòng), có lý do, do người có quyền lập loại phiếu gốc lập. *(Nguồn: BR-M08-20, 1.5)*
- **FR-007**: Nhập, xuất kho MUST NOT sinh chi phí cho người cao tuổi. Hệ thống MUST NOT tự trừ kho theo thực đơn, định lượng món hay số suất đã chốt. *(Nguồn: BR-M08-20, 12.7, Q-220)*
- **FR-008**: **(Viết lại, 2026-10-06, Q-284)** Phụ trách bộ phận MUST lập được **yêu cầu mua sắm** của bộ phận mình gồm: bộ phận; người lập; loại (nguyên liệu / tài sản / dịch vụ sửa chữa, bảo trì / thuốc (bổ sung 2026-10-11, Q-337, FR-008d) / khác); các dòng (tên hàng hoặc dịch vụ, số lượng, đơn giá dự kiến, thành tiền); tổng số tiền dự kiến; lý do; ngày cần; tham chiếu (tài sản cần sửa, nguyên liệu dưới mức tồn tối thiểu) nếu có. Yêu cầu đã gửi MUST NOT sửa nội dung; muốn đổi thì Rút lại về Nháp. Yêu cầu mua sắm MUST NOT sinh khoản chi phí cho người cao tuổi (mua hộ theo feature 010). Đề nghị nhập của Nhân viên bếp bỏ. ~~Bộ phận chưa có Phụ trách bộ phận thì Quản lý viện thực hiện thay các lệnh của Phụ trách bộ phận (13.1a).~~ **(Sửa, 2026-10-06, Q-292)** Bộ phận chưa có Phụ trách bộ phận thì các lệnh của Phụ trách bộ phận MUST bị chặn, không ai làm thay, và Quản lý viện MUST được nhắc chỉ định (13.1a). Vòng đời: *(Nguồn: 15.10, UC-94, UC-95, BR-M11-17 → 19, DBR-36, BF-18)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái kế tiếp | Ai thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Lập yêu cầu | Nháp | Phụ trách bộ phận; Điều dưỡng (chỉ loại "thuốc", FR-008d) | — | — |
| Nháp | Gửi duyệt | Chờ duyệt | Người lập | Có ít nhất một dòng; có lý do | Báo Quản lý viện |
| Nháp | Hủy | Đã hủy | Người lập | — | — |
| Chờ duyệt | Rút lại | Nháp | Người lập | — | — |
| Chờ duyệt | Duyệt, có thể giảm số tiền được duyệt | Đã duyệt | Quản lý viện | Bắt buộc lý do khi giảm | Báo người lập |
| Chờ duyệt | Từ chối | Từ chối | Quản lý viện | Có lý do | Báo người lập |
| Đã duyệt | Ghi đã mua: số tiền thực tế, ngày mua, nhà cung cấp, chứng từ | Chờ đối chiếu | Phụ trách bộ phận | FR-008a | Báo Kế toán |
| Đã duyệt | Hủy (không mua nữa) | Đã hủy | Phụ trách bộ phận | Có lý do; chưa có phiếu nhập, tài sản hoặc kết quả bảo trì gắn với yêu cầu (Q-290) | — |
| Chờ đối chiếu | Xác nhận khớp | Đã đối chiếu | Kế toán | FR-008b | — |
| Chờ đối chiếu | Trả lại (thiếu hoặc sai chứng từ) | Đã duyệt | Kế toán | Có lý do | Báo người lập |
| Chờ đối chiếu | Ghi lệch | Lệch | Kế toán | Có lý do và số tiền lệch | Báo Quản lý viện |
| Lệch | Chấp nhận lệch hoặc yêu cầu bộ phận hoàn tiền | Đã đối chiếu | Quản lý viện | Có lý do | Báo người lập, Kế toán |

Đã đối chiếu, Từ chối, Đã hủy là trạng thái cuối.

- **FR-008a** *(bổ sung, 2026-10-06, Q-284)*: "Ghi đã mua" MUST bắt buộc chứng từ (ảnh hóa đơn hoặc phiếu bán hàng). Số tiền thực tế vượt số được duyệt không quá CFG-M11-07 (mặc định \[10%\]) thì được ghi; vượt hơn MUST bị chặn, và bộ phận lập yêu cầu bổ sung cho phần vượt, tham chiếu yêu cầu gốc, Quản lý viện duyệt như yêu cầu mới. *(Nguồn: BR-M11-18)*
- **FR-008b** *(bổ sung, 2026-10-06, Q-284)*: Kế toán MUST xác nhận khớp chỉ khi số tiền trên chứng từ bằng số tiền thực tế đã ghi, và (với loại nguyên liệu, tài sản) bằng tổng các phiếu nhập hoặc tổng nguyên giá các tài sản gắn với yêu cầu (**sửa, 2026-10-06, Q-294**). Kế toán MUST NOT lập hay duyệt yêu cầu mua sắm, trong mọi trường hợp (**sửa, Q-293:** Kế toán không được giao nhiệm vụ Phụ trách bộ phận); người ghi đã mua MUST NOT tự đối chiếu yêu cầu đó (Phụ lục 27 ³⁸). *(Nguồn: BR-M11-19, Q-293, Q-294)*
- **FR-008d** *(bổ sung, 2026-10-11, Q-337)*: Yêu cầu loại "thuốc" dùng cho thuốc do viện cung cấp của người cao tuổi; Điều dưỡng (kể cả khi không là Phụ trách bộ phận) hoặc Phụ trách bộ phận MUST lập được, bộ phận của yêu cầu là bộ phận của người lập. Mỗi dòng MUST gồm người cao tuổi, đơn thuốc nguồn "viện cung cấp" Hiệu lực, Tạm dừng hoặc Chờ hiệu lực của người đó (feature 006), tên thuốc, hoạt chất, hàm lượng, dạng, số lượng, đơn vị, đơn giá dự kiến; yêu cầu MUST có mức ưu tiên (Thường / Gấp) và thời điểm cần thuốc, MAY có tài liệu liên quan. Người lập là Điều dưỡng được gửi, rút lại, hủy yêu cầu của mình; "Ghi đã mua" vẫn chỉ do Phụ trách bộ phận (FR-008a). Hệ thống MUST NOT tự lập yêu cầu loại này (Q-307). Yêu cầu không sinh chi phí cho người cao tuổi; chi phí thuốc vẫn tính theo liều ở feature 006, 010. Lần tiếp nhận thuốc do feature 006 ghi (FR-054); không có phiếu nhập hay tồn kho thuốc. Trạng thái "Đang cung ứng", "Đã giao", "Đã nhận" của tài liệu gốc ứng với Đã duyệt, đã ghi đã mua và có lần tiếp nhận Đạt cho mọi dòng (11.7). *(Nguồn: 11.7, 15.10, BR-M07-24, BR-M11-17, Q-337)*
- **FR-008c** *(bổ sung, 2026-10-06, Q-284)*: Phiếu nhập kho nguyên liệu, lệnh ghi tăng tài sản mua mới và lệnh ghi kết quả bảo trì thuê ngoài có chi phí MUST gắn đúng một yêu cầu mua sắm đang ở Đã duyệt của cùng bộ phận; thiếu thì lệnh bị chặn. Hàng được tặng hoặc chuyển từ nơi khác được miễn, nhưng MUST ghi nguồn. Mỗi nhắc của BR-M11-20 theo FR-021. *(Nguồn: BR-M11-17, DBR-36)*

- **FR-009**: Mỗi ngày, Bộ lập lịch MUST báo Phụ trách bộ phận Bếp và Nhân viên bếp (mức Nhẹ; sửa, 2026-10-06, Q-284): các lô còn tồn có hạn dùng trong CFG-M08-08 (mặc định \[2 ngày\]) hoặc đã quá hạn; các nguyên liệu Đang dùng có tổng tồn dưới mức tồn tối thiểu, kèm nhắc Phụ trách bộ phận Bếp lập yêu cầu mua sắm (FR-008). *(Nguồn: BR-M08-18)*
- **FR-010**: Nhân viên bếp MUST đếm và lập phiếu kiểm kê theo chu kỳ CFG-M08-09 (mặc định \[hằng tháng\]); tới kỳ mà chưa có phiếu thì Nhân viên bếp và Phụ trách bộ phận Bếp được nhắc. Phiếu ghi tồn thực tế từng lô, hệ thống tính chênh lệch; dòng có chênh lệch khác 0 MUST có lý do. Phụ trách bộ phận Bếp duyệt; khi người lập là Phụ trách bộ phận Bếp thì Quản lý viện duyệt; người lập MUST NOT duyệt phiếu của chính mình (sửa, 2026-10-06, Q-284). Vòng đời: *(Nguồn: 12.7, BR-M08-19, Q-233)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái kế tiếp | Ai thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Lập phiếu kiểm kê | Chờ duyệt | Nhân viên bếp | Có lý do cho mọi dòng lệch; chưa có phiếu Chờ duyệt cùng kỳ | Báo người duyệt |
| Chờ duyệt | Duyệt | Đã duyệt | Phụ trách bộ phận Bếp; Quản lý viện khi người lập là Phụ trách bộ phận Bếp | Người duyệt khác người lập; tồn sau điều chỉnh không âm (có thể đã đổi do phiếu xuất sau khi lập) | Tạo điều chỉnh cho mọi dòng lệch; báo người lập |
| Chờ duyệt | Trả lại | Bị trả lại | Như dòng trên | Có lý do | Báo người lập; lập phiếu mới để đếm lại |
| Chờ duyệt | Hủy | Đã hủy | Người lập | Có lý do | — |

Đã duyệt, Bị trả lại và Đã hủy là trạng thái cuối; phiếu đã lập không sửa được.

#### B. Tài sản của viện

- **FR-011**: Quản lý viện MUST quản lý danh mục loại tài sản (xe, giường, thiết bị, nội thất, khác). Phụ trách bộ phận quản lý tài sản (**sửa, 2026-10-06, Q-284**) MUST quản lý tài sản của bộ phận gồm: mã tài sản (duy nhất), loại, tên, bộ phận quản lý (13.1a), yêu cầu mua sắm đã dùng để mua (nếu có), ngày đưa vào sử dụng, nguyên giá (không bắt buộc; **sửa, 2026-10-06, Q-294:** MUST bắt buộc khi tài sản gắn yêu cầu mua sắm), lịch bảo trì hoặc kiểm định (chu kỳ, hạn kế tiếp); với xe thêm biển số, số chỗ, hạn đăng kiểm, hạn bảo hiểm. Mỗi giường của feature 003 MUST có đúng một tài sản loại "giường" liên kết; tài sản này MUST được tạo tự động ở Sẵn sàng khi feature 003 tạo giường, trong cùng một lần, thuộc bộ phận Chăm sóc, và Phụ trách bộ phận Chăm sóc bổ sung các trường còn lại sau (Q-238, Q-284). Tài sản loại giường MUST NOT được ghi tăng riêng ở feature này. Quản lý viện MUST chuyển được tài sản sang bộ phận khác, bắt buộc lý do; lịch sử ghi bộ phận cũ, mới, thời điểm, người thực hiện. *(Nguồn: 7.7, DBR-32; Q-284)*
- **FR-012**: Mỗi tài sản MUST có đúng một trạng thái và một vị trí hiện hành (khu vực, tầng, phòng, "bãi xe" hoặc "kho tài sản"); lịch sử vị trí MUST không chồng khoảng. Chuyển vị trí ghi vị trí cũ, mới, thời điểm, người thực hiện, lý do. Vị trí của tài sản loại giường MUST luôn là phòng của giường (feature 003); chuyển vị trí tài sản giường là đổi phòng của giường và MUST bị chặn khi giường đã từng có phân bổ, kể cả phân bổ đã đóng hoặc đã hủy (feature 003 FR-007). Giường đã có lịch sử thì tạo giường mới ở phòng mới và Thanh lý giường cũ (Q-243). *(Nguồn: 7.7, DBR-32)*
- **FR-013**: Vòng đời tài sản MUST theo bảng dưới. *(Nguồn: 7.7, BR-M03-15, 18)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái kế tiếp | Ai thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Ghi tăng tài sản | Sẵn sàng | Phụ trách bộ phận | Không phải loại giường; tài sản mua mới gắn yêu cầu mua sắm Đã duyệt của cùng bộ phận và có nguyên giá (Q-294), hàng tặng hoặc chuyển thì ghi nguồn (FR-008c) | Vị trí mặc định "kho tài sản" |
| (chưa có) | Feature 003 tạo giường (Q-238) | Sẵn sàng | Hệ thống | — | Loại giường; vị trí là phòng của giường |
| Sẵn sàng | Đưa vào sử dụng tại vị trí | Đang sử dụng | Phụ trách bộ phận | Vị trí hợp lệ; không phải loại giường (Q-242) | Ghi lịch sử vị trí |
| Sẵn sàng, Đang sử dụng | Báo hỏng | Hỏng | Phụ trách bộ phận; Trưởng tầng (tài sản có vị trí thuộc tầng mình; tài sản ở kho tài sản, bãi xe chỉ Phụ trách bộ phận báo, Q-313) | Có mô tả; FR-015 với giường | Ghi hư hỏng; báo Phụ trách bộ phận |
| Sẵn sàng, Đang sử dụng, Hỏng | Đưa vào bảo trì | Đang bảo trì | Phụ trách bộ phận; Trưởng tầng (tầng mình) | FR-015 với giường | Mở lần bảo trì |
| Đang bảo trì | Ghi kết quả bảo trì: Đạt | Sẵn sàng | Phụ trách bộ phận | Có ngày, nội dung, đơn vị thực hiện; bảo trì thuê ngoài có chi phí: gắn yêu cầu mua sắm Đã duyệt (FR-008c) | Đóng lần bảo trì; FR-015 với giường |
| Đang bảo trì | Ghi kết quả bảo trì: Không đạt | Hỏng | Phụ trách bộ phận | Như trên | Đóng lần bảo trì |
| Mọi trạng thái trừ Đã thanh lý | Thanh lý | Đã thanh lý | Phụ trách bộ phận lập yêu cầu, Quản lý viện duyệt | Có lý do; FR-015 với giường; yêu cầu thanh lý theo vòng đời yêu cầu phê duyệt chung (feature 000, 6.6; Q-314) | Tài sản chuyển Đã thanh lý khi yêu cầu được duyệt |
| Mọi trạng thái trừ Đã thanh lý | Chuyển bộ phận quản lý (bổ sung, Q-284) | Giữ nguyên | Quản lý viện | Có lý do | Ghi lịch sử bộ phận |

Đã thanh lý là trạng thái cuối. **(Sửa, 2026-10-06, Q-284)** "Phụ trách bộ phận" ở bảng trên là Phụ trách của bộ phận quản lý tài sản; ~~bộ phận chưa có người phụ trách thì Quản lý viện thực hiện thay (13.1a)~~ (**sửa, 2026-10-06, Q-292:** bộ phận chưa có người phụ trách thì lệnh bị chặn, FR-008).

- **FR-014**: Hệ thống MUST nhắc Phụ trách bộ phận quản lý tài sản (sửa, 2026-10-06, Q-284) trước hạn bảo trì, kiểm định, đăng kiểm, bảo hiểm CFG-M03-09 (mặc định \[30 ngày\]). Quá hạn mà chưa ghi lần mới thì tài sản MUST mang dấu "quá hạn bảo trì" (hoặc "quá hạn đăng kiểm", "quá hạn bảo hiểm") và được nhắc mỗi ngày; ghi lần mới thì dấu được gỡ và hạn kế tiếp tính lại. *(Nguồn: BR-M03-17)*
- **FR-015**: Với tài sản loại giường, trạng thái tài sản và trạng thái giường (feature 003) MUST đi cùng nhau trong cùng một lần: Hỏng, Đang bảo trì → giường Đang bảo trì; Sẵn sàng → giường về trạng thái theo phân bổ (tài sản loại giường không dùng Đang sử dụng, Q-242) (Trống nếu không có phân bổ, kích hoạt BR-M02-01 qua feature 003); Đã thanh lý → giường Không sử dụng. Khi giường đang có người, lệnh đổi trạng thái tài sản MUST bị chặn như BR-M03-05; riêng "Báo hỏng" khi giường có người MUST ghi được hư hỏng với dấu "chờ chuyển người", không đổi trạng thái, và nhắc Trưởng tầng chuyển người mỗi CFG-M03-02 (mặc định \[1 ngày\]) tới khi giường không còn người. Khi giường đã trống, trạng thái MUST NOT tự đổi; Trưởng tầng hoặc Quản lý viện báo hỏng lại (hoặc đưa vào bảo trì) để tài sản chuyển Hỏng và giường chuyển Đang bảo trì, và dấu "chờ chuyển người" được gỡ (Q-232). Giường mang dấu MUST NOT nhận phân bổ mới và MUST NOT kích hoạt BR-M02-01 (feature 003 FR-013b); khi giường mang dấu về Trống, Trưởng tầng MUST được nhắc báo hỏng lại mỗi CFG-M03-02. **(Sửa, 2026-09-30, Q-245)** Lệnh "Báo hỏng" khi giường có người MUST bắt buộc chọn mức ảnh hưởng "mất an toàn" hoặc "không mất an toàn"; mức "mất an toàn" MUST yêu cầu feature 009 gửi mức Khẩn cấp cho Trưởng tầng và Người phụ trách ca của tầng, việc chuyển người dùng lệnh chuyển giường lý do "y tế/an toàn" của feature 003 (Q-44). **(Bổ sung, 2026-10-01, Q-256)** Nếu tại thời điểm báo hỏng, tầng của giường không còn giường Trống nào đạt điều kiện phân bổ của feature 003 FR-018 cho người đang nằm, thông báo Khẩn cấp MUST gửi thêm cho Quản lý viện và Hành chính và MUST kèm danh sách giường Trống đạt điều kiện ở tầng khác (rỗng thì ghi "không còn giường phù hợp trong viện"). Với cả hai mức, Trưởng tầng (tầng mình) hoặc Quản lý viện MUST gỡ được dấu với lý do "đã sửa tại chỗ" cả khi giường còn người; lần sửa được ghi vào lịch sử tài sản. **(Làm rõ, 2026-10-01, Q-265)** Mức "mất an toàn" áp khi hư hỏng có thể làm người nằm ngã, bị kẹt hoặc bị thương, hoặc làm mất một chức năng mà việc chăm sóc người đó đang cần (thành giường, phanh bánh xe, khung, sàn giường, cơ cấu nâng hạ); màn hình báo hỏng MUST hiện tiêu chí này và gợi ý chọn "mất an toàn" khi không chắc. Với mức "mất an toàn", lần gỡ dấu "đã sửa tại chỗ" MUST có nội dung đã sửa và người hoặc đơn vị sửa. Thông báo Khẩn cấp của mức này theo quy tắc xác nhận và yêu cầu gọi điện của feature 009 (FR-027, FR-028); spec này không đặt quy tắc riêng. **(Bổ sung, 2026-09-30, Q-246)** Giường Tạm ngừng sử dụng (feature 003) có tài sản Sẵn sàng; Báo hỏng, Đưa vào bảo trì làm giường Đang bảo trì; ghi kết quả bảo trì đạt MUST đưa giường về Tạm ngừng sử dụng thay vì Trống; Thanh lý đưa giường Không sử dụng. Trưởng tầng (tầng mình) hoặc Quản lý viện MUST gỡ được dấu kèm lý do (ví dụ đã sửa tại chỗ) mà không đổi trạng thái. Phân bổ tương lai đã có khi gắn dấu được giữ và báo Hành chính, Trưởng tầng; xử lý chi tiết ở feature 003 FR-013b (Q-235). Thanh lý giường có phân bổ tương lai MUST áp Q-51 của feature 003. *(Nguồn: BR-M03-15, BR-M03-05, Q-220, Q-232, Q-235, Q-245, Q-246)*
- **FR-016**: Hư hỏng do nhân viên báo, kể cả phát hiện khi dọn giường (feature 003 BR-M03-13; **sửa, 2026-10-06, Q-282**), MUST gắn được với một tài sản khi Trưởng tầng (tài sản trong tầng mình) hoặc Phụ trách bộ phận quản lý tài sản (**sửa, Q-284**) xác nhận; hư hỏng khi đó được ghi vào lịch sử tài sản và MAY dẫn tới Báo hỏng. *(Nguồn: BR-M03-18)*

#### C. Lịch sử dụng xe

- **FR-017**: Trưởng tầng, Hành chính và Quản lý viện MUST đặt được lịch dùng xe gồm: xe, giờ đi và giờ về dự kiến, mục đích (chuyến đi ngoài viện / đưa đi khám / chuyển viện không cấp cứu / khác; **bổ sung, 2026-10-06, Q-295:** "khác" gồm việc viện đưa người bán trú về, feature 012), tham chiếu (chuyến đi, người cao tuổi), người lái (hồ sơ nhân viên, có thể không có tài khoản; **bổ sung, Q-298**). Lịch MUST bị chặn khi chồng giờ với một lịch chưa hủy của cùng xe (trừ chồng giờ do gia hạn giờ về của lịch Đang dùng, FR-018, Q-236), hoặc khi xe ở Hỏng, Đang bảo trì, Đã thanh lý, hoặc mang dấu quá hạn đăng kiểm, bảo hiểm tại thời điểm đi. *(Nguồn: 7.7, BR-M03-16, DBR-32)*
- **FR-018**: Vòng đời lịch xe: *(Nguồn: 7.7, BR-M03-16, Q-220, Q-231, Q-236, Q-239, Q-240)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái kế tiếp | Ai thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Đặt lịch | Đã đặt | Trưởng tầng, Hành chính, Quản lý viện | FR-017 | — |
| Đã đặt | Ghi xuất phát; hoặc người đầu tiên của chuyến được điểm danh rời viện (feature 014) | Đang dùng | Người đặt, Quản lý viện; Hệ thống | — | Ghi thời điểm thực tế |
| Đang dùng | Chuyến được gia hạn giờ về (feature 014) | Đang dùng | Hệ thống | Không chặn khi chồng lịch kế tiếp cùng xe | Giờ về dự kiến dời theo; nếu chồng lịch kế tiếp thì báo Quản lý viện và người đặt lịch kế tiếp (Q-236) |
| Đang dùng | Mọi bản ghi rời viện của chuyến bị Hủy ghi nhận (feature 014 FR-031a) | Đã đặt | Hệ thống | — | Bỏ thời điểm xuất phát thực tế (Q-240) |
| Đang dùng | Ghi trả xe; hoặc "Kết thúc điểm danh về" của chuyến (feature 014) | Đã hoàn thành | Người đặt, Quản lý viện; Hệ thống | — | Ghi thời điểm thực tế |
| Đã đặt | Hủy; hoặc chuyến bị hủy (feature 014) | Đã hủy | Người đặt, Quản lý viện; Hệ thống | Có lý do | Báo người đặt lịch, Phụ trách bộ phận quản lý xe; người lái nếu có tài khoản (Q-298) |
| Đã đặt | Dời giờ | Đã đặt | Người đặt, Quản lý viện; Hệ thống khi chuyến đổi giờ | FR-017 với giờ mới; với chuyến đi, không thỏa thì feature 014 chặn lệnh dời chuyến (Q-239) | Như dòng trên (Q-298) |

Đã hoàn thành và Đã hủy là trạng thái cuối. Lịch Đang dùng quá giờ về dự kiến thì người đặt được nhắc; nếu chồng với lịch kế tiếp của cùng xe thì Quản lý viện được báo.

- **FR-019**: Chuyến đi ngoài viện (feature 014) khai báo phương tiện là xe của viện MUST có lịch xe Đã đặt bao trùm giờ rời dự kiến trước khi điểm danh rời viện; nếu không, feature 014 MUST chặn điểm danh rời viện với lý do "chưa có lịch xe". *(Nguồn: 7.7, Q-220)*

#### D. Quyền, thông báo, giao tiếp

- **FR-020**: **(Viết lại, 2026-10-06, Q-283, Q-284)** Quyền MUST theo Phụ lục 27 dòng "Kho nguyên liệu", "Tài sản, lịch xe", "Yêu cầu mua sắm" và 19.3: Nhân viên bếp lập phiếu nhập (gắn yêu cầu mua sắm), phiếu xuất cho bữa, xuất hủy, phiếu đảo và phiếu kiểm kê (³⁴); Phụ trách bộ phận Bếp quản lý danh mục nguyên liệu, duyệt và trả lại phiếu kiểm kê; Phụ trách bộ phận (nhiệm vụ, với bất kỳ vai trò nào, chỉ với bộ phận mình, ³⁷) ghi tăng, đưa vào sử dụng, bảo trì, chuyển vị trí tài sản, lập yêu cầu thanh lý; lập, gửi, rút lại, hủy yêu cầu mua sắm, ghi đã mua; **(bổ sung, 2026-10-11, Q-337)** Điều dưỡng lập, gửi, rút lại, hủy yêu cầu loại "thuốc" (⁴⁴); Kế toán đối chiếu chứng từ yêu cầu mua sắm (³⁸); Quản lý viện xem tồn kho và tài sản, duyệt yêu cầu mua sắm, duyệt thanh lý, chuyển tài sản giữa bộ phận, xử lý yêu cầu Lệch, duyệt phiếu kiểm kê khi người lập là Phụ trách bộ phận Bếp, cấu hình danh mục loại tài sản và tham số, đặt lịch xe; Quản lý viện MUST NOT thực hiện lệnh kho (12.7) và MUST NOT làm thay bất kỳ lệnh nào của Phụ trách bộ phận (**sửa, 2026-10-06, Q-292, Q-299**); Kế toán và Quản lý viện MUST NOT giữ nhiệm vụ Phụ trách bộ phận (Q-293, Q-299); Bác sĩ xem tồn (Q-283); Trưởng tầng báo hỏng, đưa vào bảo trì tài sản trong tầng mình và đặt lịch xe (³³); Hành chính đặt lịch xe và xem tài sản; vai trò khác và người thân không có quyền. *(Nguồn: Phụ lục 27 ³³, ³⁴, ³⁷, ³⁸; 19.3, 7.7, 12.7, 15.10; Q-233, Q-283, Q-284)*
- **FR-021**: Feature này MUST yêu cầu feature 009 gửi (mức Nhẹ, trừ báo hỏng giường đang có người ở mức "mất an toàn" là Khẩn cấp cho Trưởng tầng và Người phụ trách ca của tầng, thêm Quản lý viện và Hành chính khi tầng hết giường Trống phù hợp, FR-015, Q-245, Q-256): lô sắp hết hạn, dưới tồn tối thiểu (FR-009); tới kỳ kiểm kê (Nhân viên bếp, Phụ trách bộ phận Bếp); phiếu kiểm kê chờ duyệt (người duyệt theo FR-010); phiếu kiểm kê được duyệt hoặc bị trả lại (người lập); **(sửa, 2026-10-06, Q-284)** yêu cầu mua sắm gửi duyệt (Quản lý viện), được duyệt, bị từ chối, bị trả lại (người lập), chờ đối chiếu (Kế toán), ghi lệch (Quản lý viện), Đã duyệt quá CFG-M11-08 chưa ghi đã mua (Phụ trách bộ phận), Chờ đối chiếu quá CFG-M11-09 (Kế toán) (BR-M11-20); tài sản bị báo hỏng (Phụ trách bộ phận quản lý tài sản); nhắc hạn và quá hạn bảo trì, đăng kiểm, bảo hiểm (FR-014); hư hỏng giường "chờ chuyển người"; lịch xe bị hủy, dời (**sửa, 2026-10-06, Q-298:** người đặt lịch, Phụ trách bộ phận quản lý xe, người lái nếu có tài khoản), quá giờ trả xe, xung đột lịch; **(bổ sung, Q-292)** lệnh bị chặn vì bộ phận chưa có Phụ trách bộ phận (Quản lý viện). *(Nguồn: 17)*

| Hướng | Feature | Nội dung | Dữ liệu tối thiểu |
| --- | --- | --- | --- |
| Nhận, Gửi | 003 | Nhận danh sách giường, trạng thái giường, phân bổ hiện tại và tương lai, hư hỏng phát hiện khi dọn giường (sửa, Q-282); gửi yêu cầu đổi trạng thái giường theo FR-015 (nơi ra lệnh duy nhất cho Đang bảo trì, Không sử dụng, Q-234), dấu "chờ chuyển người" và việc gỡ dấu (Q-235) | Giường, trạng thái, phân bổ, hư hỏng |
| Nhận | 001 | Danh mục dị nguyên (FR-001) | Dị nguyên |
| Nhận, Gửi | 006 | *(bổ sung, 2026-10-11, Q-337)* Nhận đơn thuốc nguồn viện để gắn dòng yêu cầu loại "thuốc" và kết quả tiếp nhận từng dòng (hiển thị "Đã nhận"); gửi yêu cầu loại "thuốc" đã ghi đã mua để Điều dưỡng ghi lần tiếp nhận (feature 006 FR-054) | Yêu cầu, dòng, người cao tuổi, đơn thuốc, thuốc, số lượng, trạng thái; kết quả tiếp nhận |
| Nhận | 011 | Danh sách bữa (ngày, bữa) để chọn mục đích xuất | Ngày, bữa, giờ bữa |
| Nhận | 008 | *(Bổ sung, Q-284)* Bộ phận của nhân viên, Phụ trách bộ phận hiện hành (feature 008 FR-001a) | Nhân viên, bộ phận, thời điểm |
| Nhận, Gửi | 014 | Nhận chuyến đi (phương tiện, giờ rời, giờ về, dời, hủy, điểm danh rời đầu tiên, kết thúc điểm danh về); gửi điều kiện "có lịch xe" (FR-019) và trạng thái lịch xe | Chuyến, giờ, lịch xe, trạng thái |
| Gửi | 009 | Thông báo ở FR-021 | Nguồn, mức, người nhận |
| Gửi | 016 | Tài sản quá hạn bảo trì, đăng kiểm, bảo hiểm; nguyên liệu dưới tồn tối thiểu; lô sắp hết hạn (18.5) | Tài sản, dấu; nguyên liệu, tồn, mức tối thiểu |
| Gửi | 016 | *(Bổ sung, Q-284)* Yêu cầu mua sắm theo bộ phận, trạng thái, số tiền được duyệt và thực tế (BR-M11-20, feature 016 FR-055a) | Yêu cầu, bộ phận, trạng thái, số tiền |

### Key Entities *(include if feature involves data)*

- **Nguyên liệu** – nhóm 1: các trường ở FR-001.
- **Lô nguyên liệu (LO_NGUYEN_LIEU)** – nhóm 2 dẫn xuất: nguyên liệu, số lô, hạn dùng, tồn.
- **Phiếu kho (PHIEU_KHO)** – nhóm 3: loại (nhập / xuất / đảo / điều chỉnh kiểm kê), ngày, mục đích, nhà cung cấp, yêu cầu mua sắm hoặc nguồn (tặng, chuyển), chứng từ mua, dòng (nguyên liệu, lô, số lượng, đơn giá, kết quả kiểm tra, lý do), người lập, phiếu gốc (với phiếu đảo).
- **Yêu cầu mua sắm (YEU_CAU_MUA_SAM)** *(thay Đề nghị nhập, 2026-10-06, Q-284)* – nhóm 2, thành nhóm 3 khi ở trạng thái cuối: bộ phận, người lập, loại, các dòng (với loại "thuốc": người cao tuổi, đơn thuốc, hoạt chất, hàm lượng, dạng, đơn vị; mức ưu tiên, thời điểm cần; bổ sung 2026-10-11, Q-337), tổng dự kiến, số được duyệt và lý do giảm, lý do, ngày cần, tham chiếu, yêu cầu gốc (với yêu cầu bổ sung), số tiền thực tế, ngày mua, nhà cung cấp, chứng từ, kết quả đối chiếu, số lệch và cách xử lý, trạng thái, người duyệt, người đối chiếu.
- **Phiếu kiểm kê** – nhóm 2 → 3: ngày, dòng (lô, tồn hệ thống, tồn thực tế, chênh lệch, lý do), trạng thái, người lập, người duyệt (Phụ trách bộ phận Bếp hoặc Quản lý viện).
- **Tài sản (TAI_SAN)** – nhóm 1 và 2: các trường ở FR-011 (gồm bộ phận quản lý, yêu cầu mua sắm đã dùng để mua), trạng thái, vị trí hiện hành, dấu quá hạn, giường liên kết.
- **Lịch sử vị trí, lần bảo trì, hư hỏng** – nhóm 3 (gồm cả lịch sử bộ phận quản lý, Q-284; lần bảo trì thuê ngoài có chi phí trỏ tới yêu cầu mua sắm): tài sản, thời điểm, nội dung, người thực hiện, đơn vị thực hiện, kết quả; với hư hỏng báo trên giường đang có người: mức ảnh hưởng (mất an toàn / không mất an toàn), dấu "chờ chuyển người", lần gỡ dấu kèm lý do, nội dung đã sửa và người sửa (Q-245, Q-265).
- **Lịch xe (LICH_XE)** – nhóm 2: các trường ở FR-017, trạng thái, thời điểm xuất phát và trả thực tế.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Với bộ kiểm thử 200 phiếu nhập, xuất, đảo, điều chỉnh, 100% tồn từng lô khớp tính tay; 0 lô có tồn âm; 0 phiếu đã lưu bị sửa, xóa.
- **SC-002**: 0 lần xuất cho bữa từ lô đã quá hạn dùng; 100% lần chọn lô không phải lô hạn sớm nhất có lý do.
- **SC-003**: Với đồng hồ giả lập, 100% lô tới mốc CFG-M08-08 và 100% nguyên liệu dưới tồn tối thiểu được báo trong lần chạy đầu tiên; 100% tài sản tới mốc CFG-M03-09 được nhắc đúng ngày.
- **SC-004**: 0 giường có trạng thái giường và trạng thái tài sản mâu thuẫn theo FR-015 sau bất kỳ lệnh nào.
- **SC-005**: 0 cặp lịch xe chưa hủy chồng giờ trên cùng xe, trừ chồng giờ do gia hạn giờ về của chuyến đang đi (Q-236); 0 chuyến đi dùng xe của viện được điểm danh rời viện khi chưa có lịch xe.
- **SC-006**: Nhân viên bếp lập xong một phiếu xuất 10 dòng cho một bữa trong không quá 3 phút.
- **SC-007** *(bổ sung, 2026-10-06, Q-284)*: 0 phiếu nhập hàng mua, ghi tăng tài sản mua mới, kết quả bảo trì thuê ngoài có chi phí không gắn yêu cầu mua sắm Đã duyệt của cùng bộ phận; 0 lần ghi đã mua vượt số được duyệt quá CFG-M11-07; 0 yêu cầu được đối chiếu bởi chính người ghi đã mua; **(bổ sung, Q-292 → Q-294)** 0 lệnh của Phụ trách bộ phận do Quản lý viện hoặc Kế toán thực hiện; 0 tài sản gắn yêu cầu mua sắm thiếu nguyên giá; với đồng hồ giả lập, 100% yêu cầu quá CFG-M11-08 hoặc CFG-M11-09 được nhắc đúng người trong lần chạy đầu tiên. **(Bổ sung, 2026-10-11, Q-337)** 100% dòng của yêu cầu loại "thuốc" gắn một đơn nguồn viện chưa kết thúc của đúng người cao tuổi; 0 yêu cầu loại "thuốc" do hệ thống tự lập; 0 khoản chi phí người cao tuổi sinh từ yêu cầu mua sắm.

## Assumptions

- Số feature `019`; người dùng chọn gộp kho nguyên liệu và tài sản vào một spec, vì cùng do Quản lý viện quản lý (Q-209, Q-210, 2026-09-28). **(Sửa, 2026-10-06, Q-284)** Nay kho và tài sản do bộ phận tự quản lý; yêu cầu mua sắm cũng đặt ở spec này vì gắn trực tiếp với phiếu nhập, ghi tăng tài sản và bảo trì thuê ngoài.
- Phiếu nhập, xuất có hiệu lực ngay khi lưu, không có trạng thái Nháp: 12.7 và BR-M08-20 chỉ nêu phiếu "đã xác nhận" và phiếu đảo.
- Phiếu kiểm kê do Nhân viên bếp đếm và lập, Phụ trách bộ phận Bếp duyệt (Clarification 2026-09-29, Q-233; sửa, 2026-10-06, Q-284: Quản lý viện chỉ duyệt khi người lập là Phụ trách bộ phận Bếp), để tách người đếm khỏi người duyệt chênh lệch.
- Đơn giá trên phiếu nhập chỉ để tham khảo; hệ thống không tính giá vốn hay giá trị tồn kho (1.2).
- Nhà cung cấp là văn bản tự do, không có danh mục nhà cung cấp ở giai đoạn này.
- Cách chuyển trạng thái lịch xe và cách báo hỏng giường đang có người đã chốt ở Clarification 2026-09-29 (Q-231, Q-232). Chu kỳ nhắc chuyển người dùng lại CFG-M03-02 (vốn là chu kỳ nhắc chuyển giường khi phòng không còn phù hợp mức chăm sóc), không thêm tham số mới.

## Điểm cần báo lại về tài liệu nguồn

1. **Vòng đời lịch xe (FR-018)** – 7.7 nêu "Đã đặt → Đang dùng → Đã hoàn thành / Đã hủy" nhưng không nêu ai hoặc sự kiện nào chuyển trạng thái. Người dùng chốt ở Clarification 2026-09-29: chuyến đi ngoài viện chuyển theo điểm danh rời viện đầu tiên và "Kết thúc điểm danh về" của feature 014; việc khác do người đặt hoặc Quản lý viện ghi xuất phát, trả xe; chuyến hủy thì lịch hủy. *(Đã xử lý 2026-09-29: bảng trạng thái lịch xe ở 7.7, Q-231 ở 24.2, BF-15.)*
2. **Báo hỏng giường đang có người (FR-015)** – BR-M03-15 chặn đổi trạng thái như BR-M03-05. Người dùng chốt ở Clarification 2026-09-29: vẫn ghi hư hỏng với dấu "chờ chuyển người", nhắc Trưởng tầng chuyển người (CFG-M03-02), giường trống thì báo hỏng lại mới đổi trạng thái. *(Đã xử lý 2026-09-29: BR-M03-15, Q-232 ở 24.2, Phụ lục 25 CFG-M03-02.)*
3. **Đề nghị nhập và phiếu kiểm kê có vòng đời (FR-008, FR-010)** – 12.7 nêu nội dung nhưng không có bảng trạng thái. *(Đã xử lý 2026-09-29: hai bảng trạng thái ở 12.7.)*
4. **Spec 014 FR-040** cần thêm điều kiện "có lịch xe" theo FR-019 của spec này (điểm báo lại 2 của spec 014). *(Đã xử lý 2026-09-28.)*
5. **Người lập phiếu kiểm kê (FR-010, FR-020; Clarification 2026-09-29)** – người dùng chốt Nhân viên bếp đếm và lập, Quản lý viện duyệt. Khác với Phụ lục 27 chú thích ³⁴ ("Nhân viên bếp ... không lập phiếu nhập, kiểm kê"), 19.3 và Q-220 ("Quản lý viện làm phần còn lại"). *(Đã xử lý 2026-09-29: ³⁴, 19.3, UC-92, 12.7, BR-M08-19, Q-233 ở 24.2; ô Bếp ở dòng "Kho nguyên liệu" giữ T³⁴.)*
6. **Rà chéo với spec 003, 014 (2026-09-29)** – người dùng chốt: giường chỉ đổi Đang bảo trì, Không sử dụng qua lệnh tài sản của feature này (Q-234); giường mang dấu "chờ chuyển người" không nhận phân bổ, không kích hoạt BR-M02-01, có lệnh gỡ dấu (Q-235); chuyến gia hạn giờ về thì lịch xe Đang dùng dời theo, không chặn khi chồng (Q-236). *(Đã xử lý 2026-09-29: 7.2, 7.7, BR-M03-01, 06, 13, 15, 16, DBR-32, BF-05, BF-15; FR-015, FR-017, FR-018, SC-005.)*
7. **Rà soát vận hành 2026-09-30 (A2.1, A2.5; bổ sung 2026-10-01: tiêu chí mức ảnh hưởng, điều kiện gỡ dấu, trường dữ liệu hư hỏng, Q-265, checklist CHK060, 061, 063, 064; bổ sung 2026-10-01: Q-256, người nhận thêm khi tầng hết giường Trống phù hợp, checklist CHK062)** – Báo hỏng giường đang có người thêm mức ảnh hưởng và gỡ dấu "đã sửa tại chỗ" khi còn người (Q-245, sửa Q-232); tạm ngừng giường không vì hỏng bằng trạng thái Tạm ngừng sử dụng của feature 003 (Q-246, sửa Q-234, Q-237). *(Đã xử lý 2026-09-30: 7.2, BR-M03-15, 17, 19.3, Phụ lục 27 ³⁵, Q-245, Q-246 ở 24.2; FR-015, User Story 5 kịch bản 6, 7.)*

**(2026-10-05) Thay đổi do chủ dự án chốt Q-280 → Q-286.** *(Đã xử lý 2026-10-06: spec đã viết lại theo các quyết định dưới đây, xem mục "Cập nhật 2026-10-06" ở Clarifications)*

- **Q-284** – kho, tài sản do bộ phận tự quản lý qua Phụ trách bộ phận (13.1a); Quản lý viện chỉ duyệt chi phí qua yêu cầu mua sắm và duyệt thanh lý; bộ phận tự mua, Kế toán đối chiếu chứng từ (15.10, BR-M11-17 → 20, CFG-M11-07 → 09, UC-94, UC-95, DBR-36, BF-18). Ảnh hưởng: User Story 1, User Story 2, FR-008, FR-010, FR-011, FR-020, FR-021 (Quản lý viện quản lý kho, tài sản; đề nghị nhập); thêm yêu cầu mua sắm (15.10, UC-94, UC-95, BF-18).
- **Q-283** – gộp Dinh dưỡng viên vào Bác sĩ; bản gán chế độ ăn do Bác sĩ lập nên không còn bước duyệt riêng (BR-M08-03); cột DDV của Phụ lục 27 gộp vào cột BS. Ảnh hưởng: FR-020 (Dinh dưỡng viên xem tồn kho).
- **Q-282** – bỏ vai trò Nhân viên vệ sinh và nghiệp vụ vệ sinh (7.5, BR-M03-08, 11, 12, 14, UC-71 → 73); giường kết thúc phân bổ chuyển "Chờ sẵn sàng", Trưởng tầng hoặc Người phụ trách ca "Xác nhận giường sẵn sàng" (nghi nhiễm thì bắt buộc xác nhận đã khử khuẩn) (BR-M03-09, 10, UC-74); nhân viên không dùng hệ thống chỉ có hồ sơ nhân viên. Ảnh hưởng: hư hỏng phát hiện khi vệ sinh đổi thành phát hiện khi dọn giường.

**(2026-10-06) Thay đổi do chủ dự án chốt Q-292 → Q-299 (rà soát tài liệu nguồn).** *(Đã xử lý 2026-10-06: spec đã viết lại, xem mục "Cập nhật 2026-10-06, lượt 2" ở Clarifications.)*

- **Q-292** – mỗi bộ phận luôn có đúng một Phụ trách bộ phận; bỏ quyền Quản lý viện làm thay. Đổi Phụ trách bộ phận, Cho nghỉ việc, khóa tài khoản hoặc chuyển bộ phận của người đang là Phụ trách bộ phận phải chỉ định người thay trong cùng lệnh; bộ phận mới tạo chưa có Phụ trách bộ phận thì lệnh kho, tài sản, mua sắm bị chặn và Quản lý viện được nhắc (13.1a, 19.3, Phụ lục 27 ³⁷, BF-18). Ảnh hưởng: FR-008, FR-011, FR-020, User Story 2 (bộ phận chưa có Phụ trách bộ phận).
- **Q-293** – Kế toán không được giao nhiệm vụ Phụ trách bộ phận; Phụ trách bộ phận Hành chính – Kế toán là Hành chính; giữ BR-M11-19, bỏ phần "Kế toán đang là Phụ trách bộ phận" ở chú thích ³⁸ (13.1a, Phụ lục 27 ³⁸, BF-18). Ảnh hưởng: User Story 2 kịch bản 5 (bỏ trường hợp Kế toán K là Phụ trách bộ phận), FR-020.
- **Q-294** – ghi tăng tài sản gắn yêu cầu mua sắm bắt buộc nguyên giá; Kế toán đối chiếu chứng từ với tổng nguyên giá (7.7, BR-M11-19). Ảnh hưởng: FR-011 (nguyên giá), đối chiếu yêu cầu loại tài sản.
- **Q-295** – thêm loại bản ghi đón "Viện đưa về" cho người bán trú: người đưa về (Hành chính, Nhân viên chăm sóc) làm quy trình đón tại nơi bàn giao; trạng thái có mặt chuyển Đã về khi ghi bàn giao; chưa bàn giao được thì vẫn Có mặt; dùng xe của viện thì có lịch xe (3.4, 14.3, UC-58, BF-02 bước 9, BF-12 bước 10). Ảnh hưởng: FR-017 (lịch xe cho việc đưa người bán trú về).
- **Q-298** – hủy, dời lịch xe thì báo người đặt lịch và Phụ trách bộ phận quản lý xe; người lái có tài khoản thì được báo thêm; người lái không có tài khoản do viện báo ngoài hệ thống (7.7). Ảnh hưởng: FR-017, bảng trạng thái lịch xe (cột "Báo người lái").
- **Q-299** – Quản lý viện không thuộc bộ phận nào và không được giao Phụ trách bộ phận; tài khoản các vai trò khác thuộc đúng một bộ phận (13.1a). Ảnh hưởng: FR-020.

**(2026-10-10) FR-003 "hệ thống MUST đề xuất lô" trái Q-307 (a)** (plan 019, Điểm cần báo lại 1). *(Đã xử lý 2026-10-10: FR-003 viết theo BR-M08-17 — hiển thị lô theo hạn dùng, chặn khi chọn lô hạn muộn hơn mà không có lý do; không đổi nghiệp vụ, không cần Q mới.)*

**(2026-10-10) Plan 019, điểm báo lại 2, 3.** *(Đã xử lý 2026-10-10: Q-313, Q-314; sửa bảng FR-013.)*

**(2026-10-11) Đối chiếu tài liệu gốc DOCX và rà soát bổ sung (báo cáo audit 2026-10-11, không lưu trong kho mã).** *(Đã xử lý 2026-10-11: Q-337 → Q-345; sửa Phạm vi, User Story 7 kịch bản 8, FR-008, bảng FR-008, FR-008d, FR-020, bảng giao tiếp, Key Entities, SC-007; xem mục "Cập nhật 2026-10-11" ở Clarifications.)*
