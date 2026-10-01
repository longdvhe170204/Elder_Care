# Feature Specification: Kho nguyên liệu và tài sản của viện

**Feature Branch**: `019-inventory-assets`

**Created**: 2026-09-28

**Status**: Draft

**Input**: User description: "Kho nguyên liệu nấu ăn và tài sản của viện theo docs/nghiep-vu.md mục 12.7 (Module 08, BR-M08-17 → BR-M08-20) và 7.7 (Module 03, BR-M03-15 → BR-M03-18); quyết định Q-209, Q-210, Q-220; UC-89 → UC-92; DBR-31, DBR-32; CFG-M03-09, CFG-M08-08, CFG-M08-09. Quản lý viện quản lý kho và tài sản. Kho: danh mục nguyên liệu, phiếu nhập có kiểm tra đầu vào, phiếu xuất theo lô, đề nghị nhập của bếp, kiểm kê có duyệt, cảnh báo hạn dùng và tồn tối thiểu; không tự trừ kho theo thực đơn. Tài sản: xe đưa đón, giường, thiết bị lớn; trạng thái, vị trí, bảo trì, kiểm định, đăng kiểm; lịch dùng xe không chồng giờ; trạng thái tài sản của giường đi cùng trạng thái giường."

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

## Phạm vi

**Trong phạm vi** (nhóm chức năng "Dinh dưỡng và kho bếp" và "Tài sản và đồ gửi", 1.6):

1. Danh mục nguyên liệu; phiếu nhập theo lô, có kết quả kiểm tra đầu vào; phiếu xuất cho bữa, xuất hủy; tồn theo lô; phiếu đảo (12.7, UC-91, BR-M08-17, 18, 20, DBR-31).
2. Đề nghị nhập của Nhân viên bếp (12.7, Q-220).
3. Kiểm kê kho theo chu kỳ, phiếu điều chỉnh có duyệt (UC-92, BR-M08-19, CFG-M08-09).
4. Cảnh báo lô sắp hết hạn và nguyên liệu dưới tồn tối thiểu (BR-M08-18, CFG-M08-08).
5. Danh mục tài sản; trạng thái, vị trí và lịch sử; bảo trì, kiểm định, đăng kiểm, bảo hiểm; báo hỏng (7.7, UC-89, BR-M03-17, 18, CFG-M03-09, DBR-32).
6. Đồng bộ trạng thái tài sản của giường với trạng thái giường (BR-M03-15, Q-220).
7. Lịch sử dụng xe đưa đón; điều kiện có lịch xe cho chuyến đi dùng xe của viện (UC-90, BR-M03-16, Q-220).

**Ngoài phạm vi**:

- Quy tắc dùng chung: feature 000.
- Trạng thái giường, phân bổ, bảo trì giường theo nghĩa phân bổ, vệ sinh và hư hỏng phát hiện khi vệ sinh (BR-M03-05, BR-M03-13): feature 003. Spec này **đồng bộ** trạng thái tài sản với trạng thái giường và **nhận** hư hỏng.
- Thực đơn, chốt suất, phiếu bữa ăn: feature 011. Spec này không tự trừ kho theo định lượng món × số suất (Q-220).
- Chuyến đi ngoài viện, điểm danh rời/về: feature 014. Spec này cung cấp lịch xe và điều kiện "có lịch xe".
- Kho thuốc, vật tư y tế, vật phẩm tiêu hao tính phí (bỉm, tã, sữa); mua sắm, công nợ nhà cung cấp, khấu hao, hạch toán tài sản: ngoài hệ thống (1.2).
- Đồ gửi của người cao tuổi: feature 013.
- Gửi thông báo: feature 009. Dashboard "tài sản quá hạn bảo trì", "nguyên liệu dưới tồn tối thiểu" (18.5): feature 016.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Quản lý viện nhập kho nguyên liệu theo lô, có kiểm tra đầu vào (Priority: P1)

Quản lý viện lập phiếu nhập: nhà cung cấp, ngày nhập, từng dòng nguyên liệu với số lượng, đơn giá, số lô, hạn dùng, kết quả kiểm tra đầu vào. Dòng "không đạt" không vào tồn. Phiếu đã xác nhận không sửa; sai sót bằng phiếu đảo.

**Why this priority**: Không có nhập kho thì không có tồn; kiểm tra đầu vào là bước 1 của kiểm thực ba bước với bếp ăn tập thể (12.7).

**Independent Test**: Lập phiếu nhập 3 dòng, một dòng "không đạt"; kiểm tra tồn theo lô; lập phiếu đảo cho một dòng nhập nhầm.

**Acceptance Scenarios**:

1. **Given** danh mục có "Thịt lợn" (kg) và "Gạo" (kg), **When** Quản lý viện lập phiếu nhập: 20 kg thịt lô L1 hạn 03/10, 50 kg gạo lô G1 hạn 01/03/2027, 10 kg thịt lô L2 kiểm tra "không đạt – có mùi", **Then** tồn thịt lô L1 là 20 kg, gạo G1 là 50 kg; lô L2 không vào tồn và được ghi lý do (12.7, DBR-31).
2. **Given** phiếu nhập đã xác nhận, **When** bất kỳ ai tìm cách sửa hay xóa, **Then** không có thao tác đó; **When** Quản lý viện lập phiếu đảo cho dòng gạo nhập nhầm 50 thành 40, **Then** phiếu đảo trừ 50 kg lô G1 và phiếu nhập mới ghi 40 kg (BR-M08-20).
3. **Given** phiếu đảo làm tồn của lô âm (gạo G1 đã xuất 45 kg), **When** lưu, **Then** hệ thống chặn (BR-M08-17).
4. **Given** Nhân viên bếp, **When** tìm cách lập phiếu nhập, **Then** hệ thống từ chối (Phụ lục 27 ³⁴).

---

### User Story 2 - Nhân viên bếp xuất nguyên liệu cho bữa và đề nghị nhập (Priority: P1)

Nhân viên bếp lập phiếu xuất cho một bữa hoặc xuất hủy. Hệ thống đề xuất lô có hạn dùng sớm nhất trước; chọn lô khác phải có lý do. Lô quá hạn chỉ được xuất hủy. Khi thiếu nguyên liệu, bếp lập đề nghị nhập để Quản lý viện chuyển thành phiếu nhập hoặc từ chối.

**Why this priority**: Xuất kho hằng ngày là thao tác thường xuyên nhất của kho bếp.

**Independent Test**: Hai lô thịt L1 (hạn 03/10), L3 (hạn 06/10); xuất cho bữa trưa 02/10; thử xuất lô quá hạn cho bữa; lập đề nghị nhập.

**Acceptance Scenarios**:

1. **Given** tồn thịt L1 20 kg hạn 03/10 và L3 10 kg hạn 06/10, **When** bếp lập phiếu xuất 8 kg thịt cho bữa trưa 02/10, **Then** hệ thống đề xuất lô L1; tồn L1 còn 12 kg (BR-M08-17).
2. **Given** như trên, **When** bếp chọn lô L3 thay cho L1 mà không nhập lý do, **Then** hệ thống chặn; có lý do thì được.
3. **Given** lô L1 đã quá hạn ngày 04/10, **When** bếp xuất L1 cho bữa sáng 04/10, **Then** hệ thống chặn; **When** bếp xuất hủy L1 kèm lý do "hết hạn", **Then** được (BR-M08-18).
4. **Given** bếp xuất 30 kg khi tồn chỉ 22 kg, **When** lưu, **Then** hệ thống chặn (BR-M08-17).
5. **Given** thiếu rau, **When** bếp lập đề nghị nhập 15 kg rau cải, **Then** Quản lý viện được báo; **When** Quản lý viện chuyển đề nghị thành phiếu nhập, **Then** đề nghị chuyển "Đã nhập", liên kết phiếu nhập; **When** từ chối kèm lý do, **Then** bếp được báo.
6. **Given** Dinh dưỡng viên, **When** xem kho, **Then** thấy tồn theo nguyên liệu và lô; không lập được phiếu (Phụ lục 27).

---

### User Story 3 - Kiểm kê kho và cảnh báo hạn dùng, tồn tối thiểu (Priority: P2)

Mỗi tháng Nhân viên bếp đếm và lập phiếu kiểm kê: ghi tồn thực tế từng lô, hệ thống tính chênh lệch; chênh lệch cần lý do và chỉ điều chỉnh tồn khi Quản lý viện duyệt phiếu (Clarification 2026-09-29). Mỗi ngày hệ thống báo các lô sắp hết hạn và nguyên liệu dưới tồn tối thiểu.

**Why this priority**: Giữ tồn trên hệ thống khớp thực tế và tránh dùng nguyên liệu hết hạn.

**Independent Test**: Kiểm kê với 2 lô lệch; đồng hồ giả lập qua mốc hạn dùng và tồn tối thiểu.

**Acceptance Scenarios**:

1. **Given** CFG-M08-09 mặc định \[hằng tháng\], **When** tới kỳ kiểm kê mà chưa có phiếu kiểm kê, **Then** Nhân viên bếp và Quản lý viện được nhắc (BR-M08-19).
2. **Given** tồn gạo G1 trên hệ thống 30 kg, thực tế 28 kg, **When** Nhân viên bếp B lập phiếu kiểm kê ghi 28 kg mà không có lý do, **Then** hệ thống chặn; có lý do "hao hụt" thì phiếu ở "Chờ duyệt", Quản lý viện được báo; **When** Quản lý viện duyệt, **Then** tồn G1 thành 28 kg qua một điều chỉnh −2 kg (BR-M08-19, DBR-31; Clarification 2026-09-29).
3. **Given** phiếu kiểm kê của B "Chờ duyệt", **When** Quản lý viện trả lại kèm lý do "đếm lại lô G1", **Then** phiếu chuyển "Bị trả lại", B được báo và lập phiếu mới; **When** B tìm cách duyệt phiếu của chính mình, **Then** hệ thống chặn.
4. **Given** lô L3 hạn dùng 06/10, CFG-M08-08 mặc định \[2 ngày\], **When** Bộ lập lịch chạy ngày 04/10, **Then** Quản lý viện và Nhân viên bếp được báo lô L3 sắp hết hạn (BR-M08-18).
5. **Given** "Gạo" có mức tồn tối thiểu 20 kg và tổng tồn còn 15 kg, **When** Bộ lập lịch chạy, **Then** Quản lý viện và Nhân viên bếp được báo (BR-M08-18).

---

### User Story 4 - Quản lý tài sản, bảo trì và báo hỏng (Priority: P2)

Quản lý viện ghi tăng tài sản (xe, giường, thiết bị lớn), đưa vào sử dụng tại vị trí, chuyển vị trí, lập bảo trì, ghi kết quả và thanh lý. Trưởng tầng báo hỏng, đưa vào bảo trì tài sản trong tầng mình. Hệ thống nhắc trước hạn bảo trì, kiểm định, đăng kiểm, bảo hiểm.

**Why this priority**: Góp ý nghiệp vụ Q-209: tài sản khác kho và khác đồ gửi, cần theo dõi trạng thái và bảo trì.

**Independent Test**: Ghi tăng một xe và một máy tạo oxy; báo hỏng máy tạo oxy; bảo trì đạt; qua mốc hạn đăng kiểm của xe; thanh lý một tài sản.

**Acceptance Scenarios**:

1. **Given** Quản lý viện ghi tăng máy tạo oxy mã TS-015, **When** đưa vào sử dụng tại phòng 203, **Then** tài sản "Đang sử dụng", vị trí phòng 203, lịch sử vị trí có một dòng (7.7, DBR-32).
2. **Given** TS-015 ở tầng 2, **When** Trưởng tầng tầng 2 báo hỏng kèm mô tả, **Then** TS-015 chuyển "Hỏng", Quản lý viện được báo (BR-M03-18); **When** Trưởng tầng tầng 1 tìm cách báo hỏng TS-015, **Then** hệ thống từ chối (ngoài phạm vi).
3. **Given** TS-015 Hỏng, **When** Quản lý viện đưa vào bảo trì rồi ghi kết quả "đạt" kèm đơn vị thực hiện, **Then** TS-015 chuyển "Sẵn sàng"; ghi kết quả "không đạt" thì về "Hỏng".
4. **Given** xe XE-01 hạn đăng kiểm 30/11, CFG-M03-09 mặc định \[30 ngày\], **When** tới 31/10, **Then** Quản lý viện được nhắc; qua 30/11 mà chưa ghi lần đăng kiểm mới, **Then** XE-01 mang dấu "quá hạn đăng kiểm", nhắc mỗi ngày và không đặt lịch được (BR-M03-16, 17).
5. **Given** công việc vệ sinh ghi hư hỏng "tay vịn giường gãy" gắn với giường G5 (feature 003 BR-M03-13), **When** Trưởng tầng xác nhận gắn với tài sản giường G5, **Then** hư hỏng được ghi vào lịch sử tài sản G5 (BR-M03-18).
6. **Given** tài sản cũ không dùng nữa, **When** Quản lý viện thanh lý kèm lý do, **Then** tài sản "Đã thanh lý", không đổi trạng thái được nữa.

---

### User Story 5 - Giường là tài sản: trạng thái đi cùng trạng thái giường (Priority: P2)

Mỗi giường của feature 003 có đúng một bản ghi tài sản. Đưa tài sản giường vào bảo trì hoặc báo hỏng làm giường chuyển "Đang bảo trì"; thanh lý làm giường "Không sử dụng". Khi giường đang có người, việc đổi trạng thái bị chặn như BR-M03-05, nhưng hư hỏng vẫn được ghi và Trưởng tầng được nhắc chuyển người.

**Why this priority**: Tránh hai nguồn trạng thái mâu thuẫn cho cùng một giường (Q-220).

**Independent Test**: Giường G5 trống và G6 có người; báo hỏng cả hai; ghi kết quả bảo trì đạt cho G5.

**Acceptance Scenarios**:

1. **Given** giường G5 Trống, **When** Trưởng tầng báo hỏng tài sản G5, **Then** tài sản "Hỏng" và giường G5 "Đang bảo trì" trong cùng lần (BR-M03-15).
2. **Given** giường G6 Đang sử dụng, **When** Trưởng tầng báo hỏng tài sản G6, **Then** trạng thái tài sản và giường không đổi; hư hỏng được ghi với dấu "chờ chuyển người"; Trưởng tầng được nhắc chuyển người sang giường khác (BR-M03-05, BR-M03-15). **When** người đã được chuyển và G6 trống, **Then** trạng thái không tự đổi; **When** Trưởng tầng báo hỏng lại G6, **Then** tài sản "Hỏng", giường "Đang bảo trì", dấu "chờ chuyển người" được gỡ (Clarification 2026-09-29).
3. **Given** G5 Đang bảo trì do hỏng, **When** Quản lý viện ghi kết quả bảo trì đạt, **Then** tài sản "Sẵn sàng" và giường về Trống; feature 003 phát sự kiện "giường chuyển Trống" (BR-M02-01).
4. **Given** giường G7 có phân bổ tương lai, **When** Quản lý viện thanh lý tài sản G7, **Then** giường "Không sử dụng" và phân bổ tương lai xử lý như Q-51 của feature 003 (hủy, báo hành chính và trưởng tầng).
5. **Given** giường G5, **When** Quản lý viện chuyển vị trí tài sản G5 khi G5 không có phân bổ hiện tại hay tương lai, **Then** được; có phân bổ thì hệ thống chặn (7.7).
6. **(Bổ sung, 2026-09-30)** **Given** giường G8 Đang sử dụng, **When** Trưởng tầng báo hỏng tài sản G8 với mức "mất an toàn", **Then** hư hỏng được ghi với dấu "chờ chuyển người", Trưởng tầng và Người phụ trách ca nhận thông báo Khẩn cấp (Q-245). **When** Trưởng tầng ghi "đã sửa tại chỗ" khi người vẫn nằm, **Then** dấu được gỡ, trạng thái tài sản và giường không đổi.
7. **(Bổ sung, 2026-09-30)** **Given** giường G9 Tạm ngừng sử dụng, **When** Quản lý viện đưa tài sản G9 vào bảo trì rồi ghi kết quả đạt, **Then** giường về Tạm ngừng sử dụng, không kích hoạt BR-M02-01 (Q-246).

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

---

### Edge Cases

- Nguyên liệu Ngừng hiệu lực khi còn tồn: vẫn xuất được tới hết tồn, không nhập thêm.
- Phiếu xuất cho bữa đã qua: vẫn lập được; phiếu ghi cả thời điểm lập và bữa được chọn, để đối chiếu khi kiểm kê.
- Hai người cùng xuất một lô gần hết: chỉ phiếu lưu trước thành công nếu tồn không đủ cho cả hai (DBR-31).
- Tài sản chưa có vị trí (mới mua, để kho): trạng thái "Sẵn sàng", vị trí "kho tài sản".
- Xe gia hạn đăng kiểm trong ngày đã có lịch: dấu "quá hạn" gỡ khi ghi lần đăng kiểm mới; lịch đã đặt trước đó không bị hủy tự động nếu hạn mới bao trùm.
- Lịch xe quá giờ về dự kiến mà chưa trả xe: nhắc người đặt; không chặn lịch kế tiếp đã đặt nhưng báo xung đột cho Quản lý viện.

## Requirements *(mandatory)*

### Functional Requirements

Mọi yêu cầu dưới đây kế thừa feature 000. Phân nhóm dữ liệu:

| Dữ liệu | Nhóm (1.5) | Thao tác được phép |
| --- | --- | --- |
| Danh mục nguyên liệu, danh mục loại tài sản | 1 – Danh mục | Tạo, sửa, Ngừng hiệu lực (Quản lý viện) |
| Phiếu nhập, phiếu xuất, phiếu đảo | 3 | Chỉ ghi thêm; sai sót bằng phiếu đảo (BR-M08-20) |
| Lô nguyên liệu, tồn | 2 – dẫn xuất | Chỉ thay đổi qua phiếu nhập, xuất, đảo, điều chỉnh kiểm kê đã duyệt (DBR-31) |
| Đề nghị nhập | 2 | Theo bảng FR-008 |
| Phiếu kiểm kê | 2 → 3 khi Đã duyệt | Theo bảng FR-010 |
| Tài sản | 1 (thông tin mô tả) và 2 (trạng thái, vị trí) | Thông tin mô tả sửa được; trạng thái, vị trí chỉ qua lệnh ở bảng FR-013 (7.7) |
| Lịch sử vị trí, lần bảo trì, kiểm định, đăng kiểm, hư hỏng | 3 | Chỉ ghi thêm; sai sót bằng đính chính |
| Lịch sử dụng xe | 2 | Theo bảng FR-018 |
| Tham số CFG-M03-09, CFG-M08-08, CFG-M08-09 | 1 – Tham số | Cấu hình (Quản lý viện) |

#### A. Kho nguyên liệu

- **FR-001**: Quản lý viện MUST quản lý danh mục nguyên liệu: tên, nhóm, đơn vị tính, thành phần gây dị ứng (chọn từ danh mục dị nguyên của feature 001), mức tồn tối thiểu, trạng thái Đang dùng / Ngừng hiệu lực. Nguyên liệu Ngừng hiệu lực MUST NOT được nhập thêm nhưng vẫn xuất được tới hết tồn. *(Nguồn: 12.7)*
- **FR-002**: Quản lý viện MUST lập được phiếu nhập gồm: ngày nhập, nhà cung cấp (văn bản), các dòng (nguyên liệu, số lượng > 0, đơn giá, số lô, hạn dùng, kết quả kiểm tra đầu vào Đạt / Không đạt, ghi chú). Dòng Không đạt MUST có lý do và MUST NOT vào tồn. Phiếu có hiệu lực khi lưu (nhóm 3). *(Nguồn: 12.7)*
- **FR-003**: Nhân viên bếp và Quản lý viện MUST lập được phiếu xuất gồm: ngày, mục đích (cho bữa: ngày, bữa; hủy do hết hạn hoặc hỏng; khác, bắt buộc ghi chú), các dòng (nguyên liệu, lô, số lượng), người xuất. Hệ thống MUST đề xuất lô có hạn dùng sớm nhất còn tồn; chọn lô khác MUST có lý do. *(Nguồn: 12.7, BR-M08-17, Q-220)*
- **FR-004**: Tồn của mỗi lô MUST bằng nhập − xuất ± điều chỉnh đã duyệt, và MUST NOT âm; phiếu hoặc phiếu đảo làm một lô âm MUST bị chặn toàn bộ (feature 000 "hoặc toàn bộ, hoặc không"). Hai phiếu đồng thời trên cùng lô MUST cho kết quả như khi lưu lần lượt. *(Nguồn: BR-M08-17, DBR-31)*
- **FR-005**: Lô đã quá hạn dùng MUST NOT được xuất cho bữa; chỉ được xuất với mục đích "hủy". *(Nguồn: BR-M08-18)*
- **FR-006**: Phiếu nhập, phiếu xuất MUST NOT bị sửa hay xóa. Sai sót MUST xử lý bằng phiếu đảo trỏ đúng một phiếu gốc (toàn bộ hoặc một số dòng), có lý do, do người có quyền lập loại phiếu gốc lập. *(Nguồn: BR-M08-20, 1.5)*
- **FR-007**: Nhập, xuất kho MUST NOT sinh chi phí cho người cao tuổi. Hệ thống MUST NOT tự trừ kho theo thực đơn, định lượng món hay số suất đã chốt. *(Nguồn: BR-M08-20, 12.7, Q-220)*
- **FR-008**: Nhân viên bếp MUST lập được đề nghị nhập (nguyên liệu, số lượng, lý do, ngày cần). Vòng đời: *(Nguồn: 12.7, Q-220)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái kế tiếp | Ai thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Lập đề nghị | Chờ xử lý | Nhân viên bếp | Nguyên liệu Đang dùng | Báo Quản lý viện |
| Chờ xử lý | Chuyển thành phiếu nhập | Đã nhập | Quản lý viện | Phiếu nhập được lưu trong cùng lần | Liên kết phiếu nhập; báo người lập |
| Chờ xử lý | Từ chối | Từ chối | Quản lý viện | Có lý do | Báo người lập |
| Chờ xử lý | Hủy | Đã hủy | Người lập | Có lý do | — |

Đã nhập, Từ chối, Đã hủy là trạng thái cuối.

- **FR-009**: Mỗi ngày, Bộ lập lịch MUST báo Quản lý viện và Nhân viên bếp (mức Nhẹ): các lô còn tồn có hạn dùng trong CFG-M08-08 (mặc định \[2 ngày\]) hoặc đã quá hạn; các nguyên liệu Đang dùng có tổng tồn dưới mức tồn tối thiểu. *(Nguồn: BR-M08-18)*
- **FR-010**: Nhân viên bếp MUST đếm và lập phiếu kiểm kê theo chu kỳ CFG-M08-09 (mặc định \[hằng tháng\]); tới kỳ mà chưa có phiếu thì Nhân viên bếp và Quản lý viện được nhắc. Phiếu ghi tồn thực tế từng lô, hệ thống tính chênh lệch; dòng có chênh lệch khác 0 MUST có lý do. Chỉ Quản lý viện duyệt; người lập MUST NOT duyệt phiếu của chính mình. Vòng đời: *(Nguồn: 12.7, BR-M08-19, Q-233)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái kế tiếp | Ai thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Lập phiếu kiểm kê | Chờ duyệt | Nhân viên bếp | Có lý do cho mọi dòng lệch; chưa có phiếu Chờ duyệt cùng kỳ | Báo Quản lý viện |
| Chờ duyệt | Duyệt | Đã duyệt | Quản lý viện | Người duyệt khác người lập; tồn sau điều chỉnh không âm (có thể đã đổi do phiếu xuất sau khi lập) | Tạo điều chỉnh cho mọi dòng lệch; báo người lập |
| Chờ duyệt | Trả lại | Bị trả lại | Quản lý viện | Có lý do | Báo người lập; lập phiếu mới để đếm lại |
| Chờ duyệt | Hủy | Đã hủy | Người lập | Có lý do | — |

Đã duyệt, Bị trả lại và Đã hủy là trạng thái cuối; phiếu đã lập không sửa được.

#### B. Tài sản của viện

- **FR-011**: Quản lý viện MUST quản lý danh mục loại tài sản (xe, giường, thiết bị, nội thất, khác) và tài sản gồm: mã tài sản (duy nhất), loại, tên, ngày đưa vào sử dụng, nguyên giá (không bắt buộc), lịch bảo trì hoặc kiểm định (chu kỳ, hạn kế tiếp); với xe thêm biển số, số chỗ, hạn đăng kiểm, hạn bảo hiểm. Mỗi giường của feature 003 MUST có đúng một tài sản loại "giường" liên kết; tài sản này MUST được tạo tự động ở Sẵn sàng khi feature 003 tạo giường, trong cùng một lần, và Quản lý viện bổ sung các trường còn lại sau (Q-238). Tài sản loại giường MUST NOT được ghi tăng riêng ở feature này. *(Nguồn: 7.7, DBR-32)*
- **FR-012**: Mỗi tài sản MUST có đúng một trạng thái và một vị trí hiện hành (khu vực, tầng, phòng, "bãi xe" hoặc "kho tài sản"); lịch sử vị trí MUST không chồng khoảng. Chuyển vị trí ghi vị trí cũ, mới, thời điểm, người thực hiện, lý do. Vị trí của tài sản loại giường MUST luôn là phòng của giường (feature 003); chuyển vị trí tài sản giường là đổi phòng của giường và MUST bị chặn khi giường đã từng có phân bổ, kể cả phân bổ đã đóng hoặc đã hủy (feature 003 FR-007). Giường đã có lịch sử thì tạo giường mới ở phòng mới và Thanh lý giường cũ (Q-243). *(Nguồn: 7.7, DBR-32)*
- **FR-013**: Vòng đời tài sản MUST theo bảng dưới. *(Nguồn: 7.7, BR-M03-15, 18)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái kế tiếp | Ai thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Ghi tăng tài sản | Sẵn sàng | Quản lý viện | Không phải loại giường | Vị trí mặc định "kho tài sản" |
| (chưa có) | Feature 003 tạo giường (Q-238) | Sẵn sàng | Hệ thống | — | Loại giường; vị trí là phòng của giường |
| Sẵn sàng | Đưa vào sử dụng tại vị trí | Đang sử dụng | Quản lý viện | Vị trí hợp lệ; không phải loại giường (Q-242) | Ghi lịch sử vị trí |
| Sẵn sàng, Đang sử dụng | Báo hỏng | Hỏng | Quản lý viện; Trưởng tầng (tài sản trong tầng mình) | Có mô tả; FR-015 với giường | Ghi hư hỏng; báo Quản lý viện |
| Sẵn sàng, Đang sử dụng, Hỏng | Đưa vào bảo trì | Đang bảo trì | Quản lý viện; Trưởng tầng (tầng mình) | FR-015 với giường | Mở lần bảo trì |
| Đang bảo trì | Ghi kết quả bảo trì: Đạt | Sẵn sàng | Quản lý viện | Có ngày, nội dung, đơn vị thực hiện | Đóng lần bảo trì; FR-015 với giường |
| Đang bảo trì | Ghi kết quả bảo trì: Không đạt | Hỏng | Quản lý viện | Như trên | Đóng lần bảo trì |
| Mọi trạng thái trừ Đã thanh lý | Thanh lý | Đã thanh lý | Quản lý viện | Có lý do; FR-015 với giường | — |

Đã thanh lý là trạng thái cuối.

- **FR-014**: Hệ thống MUST nhắc Quản lý viện trước hạn bảo trì, kiểm định, đăng kiểm, bảo hiểm CFG-M03-09 (mặc định \[30 ngày\]). Quá hạn mà chưa ghi lần mới thì tài sản MUST mang dấu "quá hạn bảo trì" (hoặc "quá hạn đăng kiểm", "quá hạn bảo hiểm") và được nhắc mỗi ngày; ghi lần mới thì dấu được gỡ và hạn kế tiếp tính lại. *(Nguồn: BR-M03-17)*
- **FR-015**: Với tài sản loại giường, trạng thái tài sản và trạng thái giường (feature 003) MUST đi cùng nhau trong cùng một lần: Hỏng, Đang bảo trì → giường Đang bảo trì; Sẵn sàng → giường về trạng thái theo phân bổ (tài sản loại giường không dùng Đang sử dụng, Q-242) (Trống nếu không có phân bổ, kích hoạt BR-M02-01 qua feature 003); Đã thanh lý → giường Không sử dụng. Khi giường đang có người, lệnh đổi trạng thái tài sản MUST bị chặn như BR-M03-05; riêng "Báo hỏng" khi giường có người MUST ghi được hư hỏng với dấu "chờ chuyển người", không đổi trạng thái, và nhắc Trưởng tầng chuyển người mỗi CFG-M03-02 (mặc định \[1 ngày\]) tới khi giường không còn người. Khi giường đã trống, trạng thái MUST NOT tự đổi; Trưởng tầng hoặc Quản lý viện báo hỏng lại (hoặc đưa vào bảo trì) để tài sản chuyển Hỏng và giường chuyển Đang bảo trì, và dấu "chờ chuyển người" được gỡ (Q-232). Giường mang dấu MUST NOT nhận phân bổ mới và MUST NOT kích hoạt BR-M02-01 (feature 003 FR-013b); khi giường mang dấu về Trống, Trưởng tầng MUST được nhắc báo hỏng lại mỗi CFG-M03-02. **(Sửa, 2026-09-30, Q-245)** Lệnh "Báo hỏng" khi giường có người MUST bắt buộc chọn mức ảnh hưởng "mất an toàn" hoặc "không mất an toàn"; mức "mất an toàn" MUST yêu cầu feature 009 gửi mức Khẩn cấp cho Trưởng tầng và Người phụ trách ca của tầng, việc chuyển người dùng lệnh chuyển giường lý do "y tế/an toàn" của feature 003 (Q-44). **(Bổ sung, 2026-10-01, Q-256)** Nếu tại thời điểm báo hỏng, tầng của giường không còn giường Trống nào đạt điều kiện phân bổ của feature 003 FR-018 cho người đang nằm, thông báo Khẩn cấp MUST gửi thêm cho Quản lý viện và Hành chính và MUST kèm danh sách giường Trống đạt điều kiện ở tầng khác (rỗng thì ghi "không còn giường phù hợp trong viện"). Với cả hai mức, Trưởng tầng (tầng mình) hoặc Quản lý viện MUST gỡ được dấu với lý do "đã sửa tại chỗ" cả khi giường còn người; lần sửa được ghi vào lịch sử tài sản. **(Bổ sung, 2026-09-30, Q-246)** Giường Tạm ngừng sử dụng (feature 003) có tài sản Sẵn sàng; Báo hỏng, Đưa vào bảo trì làm giường Đang bảo trì; ghi kết quả bảo trì đạt MUST đưa giường về Tạm ngừng sử dụng thay vì Trống; Thanh lý đưa giường Không sử dụng. Trưởng tầng (tầng mình) hoặc Quản lý viện MUST gỡ được dấu kèm lý do (ví dụ đã sửa tại chỗ) mà không đổi trạng thái. Phân bổ tương lai đã có khi gắn dấu được giữ và báo Hành chính, Trưởng tầng; xử lý chi tiết ở feature 003 FR-013b (Q-235). Thanh lý giường có phân bổ tương lai MUST áp Q-51 của feature 003. *(Nguồn: BR-M03-15, BR-M03-05, Q-220, Q-232, Q-235, Q-245, Q-246)*
- **FR-016**: Hư hỏng phát hiện khi vệ sinh (feature 003 BR-M03-13) MUST gắn được với một tài sản khi Trưởng tầng hoặc Quản lý viện xác nhận; hư hỏng khi đó được ghi vào lịch sử tài sản và MAY dẫn tới Báo hỏng. *(Nguồn: BR-M03-18)*

#### C. Lịch sử dụng xe

- **FR-017**: Trưởng tầng, Hành chính và Quản lý viện MUST đặt được lịch dùng xe gồm: xe, giờ đi và giờ về dự kiến, mục đích (chuyến đi ngoài viện / đưa đi khám / chuyển viện không cấp cứu / khác), tham chiếu (chuyến đi, người cao tuổi), người lái. Lịch MUST bị chặn khi chồng giờ với một lịch chưa hủy của cùng xe (trừ chồng giờ do gia hạn giờ về của lịch Đang dùng, FR-018, Q-236), hoặc khi xe ở Hỏng, Đang bảo trì, Đã thanh lý, hoặc mang dấu quá hạn đăng kiểm, bảo hiểm tại thời điểm đi. *(Nguồn: 7.7, BR-M03-16, DBR-32)*
- **FR-018**: Vòng đời lịch xe: *(Nguồn: 7.7, BR-M03-16, Q-220, Q-231, Q-236, Q-239, Q-240)*

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái kế tiếp | Ai thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Đặt lịch | Đã đặt | Trưởng tầng, Hành chính, Quản lý viện | FR-017 | — |
| Đã đặt | Ghi xuất phát; hoặc người đầu tiên của chuyến được điểm danh rời viện (feature 014) | Đang dùng | Người đặt, Quản lý viện; Hệ thống | — | Ghi thời điểm thực tế |
| Đang dùng | Chuyến được gia hạn giờ về (feature 014) | Đang dùng | Hệ thống | Không chặn khi chồng lịch kế tiếp cùng xe | Giờ về dự kiến dời theo; nếu chồng lịch kế tiếp thì báo Quản lý viện và người đặt lịch kế tiếp (Q-236) |
| Đang dùng | Mọi bản ghi rời viện của chuyến bị Hủy ghi nhận (feature 014 FR-031a) | Đã đặt | Hệ thống | — | Bỏ thời điểm xuất phát thực tế (Q-240) |
| Đang dùng | Ghi trả xe; hoặc "Kết thúc điểm danh về" của chuyến (feature 014) | Đã hoàn thành | Người đặt, Quản lý viện; Hệ thống | — | Ghi thời điểm thực tế |
| Đã đặt | Hủy; hoặc chuyến bị hủy (feature 014) | Đã hủy | Người đặt, Quản lý viện; Hệ thống | Có lý do | Báo người lái |
| Đã đặt | Dời giờ | Đã đặt | Người đặt, Quản lý viện; Hệ thống khi chuyến đổi giờ | FR-017 với giờ mới; với chuyến đi, không thỏa thì feature 014 chặn lệnh dời chuyến (Q-239) | Báo người lái |

Đã hoàn thành và Đã hủy là trạng thái cuối. Lịch Đang dùng quá giờ về dự kiến thì người đặt được nhắc; nếu chồng với lịch kế tiếp của cùng xe thì Quản lý viện được báo.

- **FR-019**: Chuyến đi ngoài viện (feature 014) khai báo phương tiện là xe của viện MUST có lịch xe Đã đặt bao trùm giờ rời dự kiến trước khi điểm danh rời viện; nếu không, feature 014 MUST chặn điểm danh rời viện với lý do "chưa có lịch xe". *(Nguồn: 7.7, Q-220)*

#### D. Quyền, thông báo, giao tiếp

- **FR-020**: Quyền MUST theo Phụ lục 27 dòng "Kho nguyên liệu" và "Tài sản, lịch xe" và 19.3: Quản lý viện cấu hình và thực hiện mọi lệnh; Nhân viên bếp lập phiếu xuất cho bữa, xuất hủy, đề nghị nhập và phiếu kiểm kê (không lập phiếu nhập, không duyệt kiểm kê; Q-233); Quản lý viện duyệt, trả lại phiếu kiểm kê; Dinh dưỡng viên xem tồn; Trưởng tầng báo hỏng, đưa vào bảo trì tài sản trong tầng mình và đặt lịch xe; Hành chính đặt lịch xe và xem tài sản; vai trò khác và người thân không có quyền. *(Nguồn: Phụ lục 27 ³³, ³⁴; 19.3; Q-220, Q-233)*
- **FR-021**: Feature này MUST yêu cầu feature 009 gửi (mức Nhẹ, trừ báo hỏng giường đang có người ở mức "mất an toàn" là Khẩn cấp cho Trưởng tầng và Người phụ trách ca của tầng, thêm Quản lý viện và Hành chính khi tầng hết giường Trống phù hợp, FR-015, Q-245, Q-256): lô sắp hết hạn, dưới tồn tối thiểu (FR-009); tới kỳ kiểm kê (Nhân viên bếp, Quản lý viện); phiếu kiểm kê chờ duyệt (Quản lý viện); phiếu kiểm kê được duyệt hoặc bị trả lại (người lập); đề nghị nhập mới, được xử lý; tài sản bị báo hỏng; nhắc hạn và quá hạn bảo trì, đăng kiểm, bảo hiểm (FR-014); hư hỏng giường "chờ chuyển người"; lịch xe bị hủy, dời, quá giờ trả xe, xung đột lịch. *(Nguồn: 17)*

| Hướng | Feature | Nội dung | Dữ liệu tối thiểu |
| --- | --- | --- | --- |
| Nhận, Gửi | 003 | Nhận danh sách giường, trạng thái giường, phân bổ hiện tại và tương lai, hư hỏng phát hiện khi vệ sinh; gửi yêu cầu đổi trạng thái giường theo FR-015 (nơi ra lệnh duy nhất cho Đang bảo trì, Không sử dụng, Q-234), dấu "chờ chuyển người" và việc gỡ dấu (Q-235) | Giường, trạng thái, phân bổ, hư hỏng |
| Nhận | 001 | Danh mục dị nguyên (FR-001) | Dị nguyên |
| Nhận | 011 | Danh sách bữa (ngày, bữa) để chọn mục đích xuất | Ngày, bữa, giờ bữa |
| Nhận, Gửi | 014 | Nhận chuyến đi (phương tiện, giờ rời, giờ về, dời, hủy, điểm danh rời đầu tiên, kết thúc điểm danh về); gửi điều kiện "có lịch xe" (FR-019) và trạng thái lịch xe | Chuyến, giờ, lịch xe, trạng thái |
| Gửi | 009 | Thông báo ở FR-021 | Nguồn, mức, người nhận |
| Gửi | 016 | Tài sản quá hạn bảo trì, đăng kiểm, bảo hiểm; nguyên liệu dưới tồn tối thiểu; lô sắp hết hạn (18.5) | Tài sản, dấu; nguyên liệu, tồn, mức tối thiểu |

### Key Entities *(include if feature involves data)*

- **Nguyên liệu** – nhóm 1: các trường ở FR-001.
- **Lô nguyên liệu (LO_NGUYEN_LIEU)** – nhóm 2 dẫn xuất: nguyên liệu, số lô, hạn dùng, tồn.
- **Phiếu kho (PHIEU_KHO)** – nhóm 3: loại (nhập / xuất / đảo / điều chỉnh kiểm kê), ngày, mục đích, nhà cung cấp, dòng (nguyên liệu, lô, số lượng, đơn giá, kết quả kiểm tra, lý do), người lập, phiếu gốc (với phiếu đảo).
- **Đề nghị nhập** – nhóm 2: nguyên liệu, số lượng, lý do, ngày cần, người lập, trạng thái, phiếu nhập liên kết.
- **Phiếu kiểm kê** – nhóm 2 → 3: ngày, dòng (lô, tồn hệ thống, tồn thực tế, chênh lệch, lý do), trạng thái, người lập, người duyệt.
- **Tài sản (TAI_SAN)** – nhóm 1 và 2: các trường ở FR-011, trạng thái, vị trí hiện hành, dấu quá hạn, giường liên kết.
- **Lịch sử vị trí, lần bảo trì, hư hỏng** – nhóm 3: tài sản, thời điểm, nội dung, người thực hiện, đơn vị thực hiện, kết quả.
- **Lịch xe (LICH_XE)** – nhóm 2: các trường ở FR-017, trạng thái, thời điểm xuất phát và trả thực tế.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Với bộ kiểm thử 200 phiếu nhập, xuất, đảo, điều chỉnh, 100% tồn từng lô khớp tính tay; 0 lô có tồn âm; 0 phiếu đã lưu bị sửa, xóa.
- **SC-002**: 0 lần xuất cho bữa từ lô đã quá hạn dùng; 100% lần chọn lô không phải lô hạn sớm nhất có lý do.
- **SC-003**: Với đồng hồ giả lập, 100% lô tới mốc CFG-M08-08 và 100% nguyên liệu dưới tồn tối thiểu được báo trong lần chạy đầu tiên; 100% tài sản tới mốc CFG-M03-09 được nhắc đúng ngày.
- **SC-004**: 0 giường có trạng thái giường và trạng thái tài sản mâu thuẫn theo FR-015 sau bất kỳ lệnh nào.
- **SC-005**: 0 cặp lịch xe chưa hủy chồng giờ trên cùng xe, trừ chồng giờ do gia hạn giờ về của chuyến đang đi (Q-236); 0 chuyến đi dùng xe của viện được điểm danh rời viện khi chưa có lịch xe.
- **SC-006**: Nhân viên bếp lập xong một phiếu xuất 10 dòng cho một bữa trong không quá 3 phút.

## Assumptions

- Số feature `019`; người dùng chọn gộp kho nguyên liệu và tài sản vào một spec, vì cùng do Quản lý viện quản lý (Q-209, Q-210, 2026-09-28).
- Phiếu nhập, xuất có hiệu lực ngay khi lưu, không có trạng thái Nháp: 12.7 và BR-M08-20 chỉ nêu phiếu "đã xác nhận" và phiếu đảo.
- Phiếu kiểm kê do Nhân viên bếp đếm và lập, Quản lý viện duyệt (Clarification 2026-09-29, Q-233), để tách người đếm khỏi người duyệt chênh lệch.
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
7. **Rà soát vận hành 2026-09-30 (A2.1, A2.5; bổ sung 2026-10-01: Q-256, người nhận thêm khi tầng hết giường Trống phù hợp, checklist CHK062)** – Báo hỏng giường đang có người thêm mức ảnh hưởng và gỡ dấu "đã sửa tại chỗ" khi còn người (Q-245, sửa Q-232); tạm ngừng giường không vì hỏng bằng trạng thái Tạm ngừng sử dụng của feature 003 (Q-246, sửa Q-234, Q-237). *(Đã xử lý 2026-09-30: 7.2, BR-M03-15, 17, 19.3, Phụ lục 27 ³⁵, Q-245, Q-246 ở 24.2; FR-015, User Story 5 kịch bản 6, 7.)*
