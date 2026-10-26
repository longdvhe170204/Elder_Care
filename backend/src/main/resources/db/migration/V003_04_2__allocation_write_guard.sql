-- T025/T043: khóa kỹ thuật thực sự xung đột, tránh gap-lock deadlock của trigger khi tham gia transaction RR.
CREATE TABLE allocation_write_guard (
 id TINYINT NOT NULL PRIMARY KEY,
 CONSTRAINT chk_allocation_write_guard CHECK (id=1)
);
INSERT INTO allocation_write_guard(id) VALUES(1);
GRANT SELECT,UPDATE ON allocation_write_guard TO 'eldercare_app'@'%';
