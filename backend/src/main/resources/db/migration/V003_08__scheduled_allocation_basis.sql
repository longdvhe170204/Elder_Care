-- T033: tra căn cứ yêu cầu không quét/khóa toàn bộ phân bổ trong transaction nguồn.
CREATE INDEX idx_allocation_basis ON bed_allocation(basis_type,basis_id);
