// Nghiệm thu quickstart 004 mục 3, 6 bằng API thật của backend demo (QuickstartDemoApplication, cổng QS_PORT).
// Chỉ chạy các bước báo cáo đối chiếu ghi MỘT PHẦN / KHÔNG THẤY; bỏ bước lỗi thời theo Q-394, Q-395. README cùng thư mục.
const F = require('./fixture.cjs');
const { summary, results } = require('./api.cjs');
const part1 = require('./part1.cjs');
const part2 = require('./part2.cjs');
const part3 = require('./part3.cjs');

(async () => {
  await F.loginAll();
  let nowIso = await F.now();
  const c = (await F.call(F.U.QL, 'GET', '/testing/clock')).body;
  if (!c.simulated) nowIso = await F.clock({ setTo: new Date().toISOString() });
  const today = F.day(nowIso);
  const start = F.day(nowIso, 3);
  console.log(`Đồng hồ demo ${nowIso}; ngày bắt đầu hợp đồng ${start}; hậu tố ${F.tag}`);
  const s = await F.structure();
  const svc = await F.services(s);
  const ctx = { s, svc, today, nowIso, start };

  const only = process.argv[2]; // "1", "2", "3": chỉ chạy một phần (phần 2, 3 tự dựng dữ liệu riêng)
  if (!only || only === '1') {
    await part1(ctx);
    // tua tới 08:30 ngày bắt đầu hợp đồng
    ctx.nowIso = await F.clock({ setTo: F.at(start, '08:30') });
    await part1.afterStart(ctx);
  }
  if (!only || only === '2') await part2(ctx);
  if (!only || only === '3') await part3(ctx);

  const by = summary();
  const fails = results.filter((r) => r.verdict !== 'ĐẠT' && r.verdict !== 'GHI CHÚ');
  if (fails.length) {
    console.log('\nCác bước không đạt hoặc cần xem tay:');
    for (const r of fails) console.log(`- ${r.verdict} ${r.step} ${r.label}`);
  }
  process.exitCode = by['KHÔNG ĐẠT'] ? 1 : 0;
})().catch((e) => {
  console.error('KHÔNG CHẠY ĐƯỢC:', e.stack || e.message);
  summary();
  process.exitCode = 2;
});
