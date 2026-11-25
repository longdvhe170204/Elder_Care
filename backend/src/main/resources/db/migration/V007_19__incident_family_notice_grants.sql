-- BF-06: tài khoản ứng dụng cần quyền UPDATE trên các cột mới của V007_18 (cấp theo cột, như V007_14).
GRANT UPDATE (recorded_later, family_notice_state, family_notified_by, family_notified_at, family_notice_method,
              family_notice_note) ON incident TO 'eldercare_app'@'%';
