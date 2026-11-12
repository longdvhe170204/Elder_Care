// Dựng lại schema demo elder_care_qs3 từ đầu (chỉ schema này): xóa, tạo lại. Chạy khi backend đã dừng; sau đó khởi động
// backend (Flyway tạo bảng, dữ liệu demo), rồi `node seed.cjs`, `node setup.cjs`.
const L = require('./lib.cjs');
if (L.SCHEMA !== 'elder_care_qs3') throw new Error('reset.cjs chỉ chạy trên elder_care_qs3');
L.sql(`DROP DATABASE IF EXISTS ${L.SCHEMA}; CREATE DATABASE ${L.SCHEMA} CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci`, { write: true, db: '' });
console.log('Đã dựng lại', L.SCHEMA);
