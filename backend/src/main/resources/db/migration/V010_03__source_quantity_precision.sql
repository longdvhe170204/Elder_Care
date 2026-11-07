-- FR-005/FR-013: giữ nguyên lượng nguồn 006 có ba chữ số lẻ; số tiền vẫn DECIMAL(15,0).
ALTER TABLE cost_source_index
    MODIFY COLUMN current_quantity DECIMAL(12,3) NOT NULL;

ALTER TABLE cost_item
    MODIFY COLUMN quantity DECIMAL(12,3) NOT NULL;
