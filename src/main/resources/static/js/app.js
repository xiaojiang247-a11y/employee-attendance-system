/* ============================================================
   企业员工考勤管理系统 · 交互与自绘 SVG 图表
   零外部依赖：不加载 ECharts / jQuery / Bootstrap
   ============================================================ */
(function () {
  'use strict';

  /* ---------- 状态码字典：所有页面统一由 JS 翻译成中文徽章 ---------- */
  var STATUS = {
    NORMAL: ['正常', 'green'], LATE: ['迟到', 'amber'], EARLY_LEAVE: ['早退', 'amber'],
    LATE_AND_EARLY: ['迟到且早退', 'red'], ABSENT: ['旷工', 'red'],
    MISSING_IN: ['缺上班卡', 'red'], MISSING_OUT: ['缺下班卡', 'amber'],
    LEAVE: ['请假', 'blue'], REST: ['休息', 'gray'],
    PENDING: ['待审批', 'amber'], APPROVED: ['已通过', 'green'],
    REJECTED: ['已驳回', 'red'], CANCELLED: ['已撤销', 'gray'],
    ACTIVE: ['在职', 'green'], PROBATION: ['试用期', 'amber'],
    REGULAR: ['正式', 'blue'], INTERN: ['实习', 'purple'],
    FIXED: ['固定班制', 'blue'], SCHEDULE: ['排班制', 'purple'], FLEX: ['自由工时', 'amber'],
    WORKDAY: ['调休上班', 'blue'], HOLIDAY: ['法定节假日', 'red'],
    WORKDAY_OT: ['工作日加班', 'blue'], WEEKEND: ['周末加班', 'purple'],
    COMP: ['转调休', 'green'], FEE: ['加班费', 'amber'],
    IN: ['上班卡', 'blue'], OUT: ['下班卡', 'purple'],
    LEAVE_BIZ: ['请假', 'blue'], OVERTIME: ['加班', 'purple'], TRIP: ['出差', 'amber'],
    MAKEUP: ['补卡', 'blue'], APPEAL: ['异常申诉', 'red'],
    ADMIN: ['系统管理员', 'purple'], HR: ['人事考勤员', 'blue'],
    MANAGER: ['部门负责人', 'amber'], EMPLOYEE: ['普通员工', 'green'],
    ALL: ['全公司', 'purple'], DEPT: ['本部门', 'blue'], SELF: ['仅本人', 'gray'],
    NOTICE: ['通知', 'blue'], POLICY: ['制度', 'purple'], URGENT: ['紧急', 'red'],
    SUCCESS: ['成功', 'green'], ERROR: ['失败', 'red'],
    '1': ['是', 'green'], '0': ['否', 'gray'],
    DIR: ['目录', 'gray'], MENU: ['菜单', 'blue']
  };
  var PIE_COLORS = ['#4f6bed', '#12b886', '#f59f00', '#fa5252', '#845ef7', '#4c6ef5', '#20c997', '#e8590c'];

  function esc(s) {
    return String(s).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }

  /* ---------- 徽章 ---------- */
  function paintBadges() {
    document.querySelectorAll('[data-status]').forEach(function (el) {
      var raw = (el.getAttribute('data-status') || '').trim();
      if (!raw) return;
      var item = STATUS[raw];
      if (item) {
        el.textContent = item[0];
        el.className = 'badge ' + item[1];
      } else {
        el.textContent = raw;
        el.className = 'badge gray';
      }
    });
  }

  /* ---------- 时间格式化 ---------- */
  function fmt(raw, withTime, dateOnly) {
    if (!raw) return '-';
    var s = String(raw).trim();
    var m = s.match(/(\d{4})-(\d{2})-(\d{2})(?:[T\s](\d{2}):(\d{2}))?/);
    if (!m) {
      var t = s.match(/^(\d{2}):(\d{2})/);
      return t ? t[1] + ':' + t[2] : s;
    }
    if (dateOnly || !m[4]) return m[1] + '-' + m[2] + '-' + m[3];
    return withTime ? m[2] + '-' + m[3] + ' ' + m[4] + ':' + m[5] : m[1] + '-' + m[2] + '-' + m[3];
  }
  function paintDates() {
    document.querySelectorAll('[data-dt]').forEach(function (el) {
      el.textContent = fmt(el.getAttribute('data-dt'), true, false);
    });
    document.querySelectorAll('[data-date]').forEach(function (el) {
      el.textContent = fmt(el.getAttribute('data-date'), false, true);
    });
    document.querySelectorAll('[data-time]').forEach(function (el) {
      var s = (el.getAttribute('data-time') || '').trim();
      el.textContent = s ? s.substring(0, 5) : '-';
    });
  }

  /* ---------- 分页 ---------- */
  function paintPagers() {
    document.querySelectorAll('.pager[data-pages]').forEach(function (p) {
      var cur = parseInt(p.getAttribute('data-page') || '1', 10);
      var pages = parseInt(p.getAttribute('data-pages') || '1', 10);
      var total = p.getAttribute('data-total') || '0';
      if (!(pages > 1)) { p.style.display = 'none'; return; }
      if (!(cur >= 1 && cur <= pages)) cur = 1;   // 页码越界时回落到第一页，避免高亮丢失
      var u = new URL(window.location.href);
      function link(i, cls, text) {
        u.searchParams.set('page', i);
        return '<a href="' + esc(u.pathname + u.search) + '" class="' + cls + '">' + text + '</a>';
      }
      var html = '<span class="info">共 ' + esc(total) + ' 条 · 第 ' + cur + ' / ' + pages + ' 页</span>';
      if (cur > 1) html += link(cur - 1, 'step', '上一页');
      // 只渲染首尾 + 当前页附近的窗口，避免上千页时生成上千个 <a>
      var items = [];
      if (pages <= 9) {
        for (var i = 1; i <= pages; i++) items.push(i);
      } else {
        items.push(1);
        var s = Math.max(2, cur - 2), e = Math.min(pages - 1, cur + 2);
        if (s > 2) items.push('...');
        for (var j = s; j <= e; j++) items.push(j);
        if (e < pages - 1) items.push('...');
        items.push(pages);
      }
      items.forEach(function (it) {
        html += (it === '...') ? '<span class="gap">…</span>' : link(it, it === cur ? 'cur' : '', it);
      });
      if (cur < pages) html += link(cur + 1, 'step', '下一页');
      p.innerHTML = html;
    });
  }

  /* ---------- 侧边栏 ---------- */
  function initSidebar() {
    var sb = document.getElementById('sidebar');
    if (!sb) return;
    if (localStorage.getItem('att_sidebar') === 'collapsed') sb.classList.add('collapsed');
    document.querySelectorAll('[data-sidebar-toggle]').forEach(function (b) {
      b.addEventListener('click', function () {
        if (window.innerWidth <= 1024) { sb.classList.toggle('open'); return; }
        sb.classList.toggle('collapsed');
        localStorage.setItem('att_sidebar', sb.classList.contains('collapsed') ? 'collapsed' : 'expanded');
      });
    });
    document.addEventListener('click', function (e) {
      if (window.innerWidth <= 1024 && sb.classList.contains('open') &&
          !sb.contains(e.target) && !e.target.closest('[data-sidebar-toggle]')) {
        sb.classList.remove('open');
      }
    });
  }

  /* ---------- 模态框 / 确认 / Toast / 时钟 ---------- */
  function initMisc() {
    document.querySelectorAll('[data-modal]').forEach(function (b) {
      b.addEventListener('click', function () {
        var m = document.getElementById(b.getAttribute('data-modal'));
        if (m) m.classList.add('show');
      });
    });
    document.querySelectorAll('[data-modal-close]').forEach(function (b) {
      b.addEventListener('click', function () {
        var m = b.closest('.modal-mask'); if (m) m.classList.remove('show');
      });
    });
    document.querySelectorAll('.modal-mask').forEach(function (m) {
      m.addEventListener('click', function (e) { if (e.target === m) m.classList.remove('show'); });
    });
    document.querySelectorAll('form[data-confirm]').forEach(function (f) {
      f.addEventListener('submit', function (e) {
        if (!window.confirm(f.getAttribute('data-confirm'))) e.preventDefault();
      });
    });
    document.querySelectorAll('.toast').forEach(function (t) {
      setTimeout(function () { t.style.opacity = '0'; t.style.transition = 'opacity .4s'; setTimeout(function () { t.remove(); }, 400); }, 3600);
    });
    var clocks = document.querySelectorAll('[data-clock]');
    if (clocks.length) {
      // 页面标注的是「北京时间」，不能用 new Date().getHours() —— 那取的是浏览器本机时区，
      // 用户时区不是 UTC+8（出国 / 系统时区设错 / 远程桌面）时会和后端 AppClock 打架。
      var fmt = null;
      try {
        fmt = new Intl.DateTimeFormat('en-GB', {
          timeZone: 'Asia/Shanghai', hourCycle: 'h23',
          hour: '2-digit', minute: '2-digit', second: '2-digit'
        });
      } catch (e) { fmt = null; }
      var pad = function (n) { return n < 10 ? '0' + n : '' + n; };
      var tick = function () {
        var now = new Date(), s;
        if (fmt) {
          s = fmt.format(now);
        } else {
          // 兜底：按 UTC+8 手工换算，避免旧浏览器无 Intl timeZone 支持
          var t = new Date(now.getTime() + (now.getTimezoneOffset() + 480) * 60000);
          s = pad(t.getHours()) + ':' + pad(t.getMinutes()) + ':' + pad(t.getSeconds());
        }
        clocks.forEach(function (c) { c.textContent = s; });
      };
      tick(); setInterval(tick, 1000);
    }
  }

  /* ---------- SVG 工具 ---------- */
  var NS = 'http://www.w3.org/2000/svg';
  function E(tag, attrs) {
    var e = document.createElementNS(NS, tag);
    for (var k in attrs) if (attrs.hasOwnProperty(k)) e.setAttribute(k, attrs[k]);
    return e;
  }
  function T(x, y, s, attrs) {
    var e = E('text', Object.assign({ x: x, y: y, fill: '#8590a3', 'font-size': '10.5',
      'font-family': 'system-ui,sans-serif' }, attrs || {}));
    e.textContent = s; return e;
  }
  function num(v) { var n = Number(v); return isNaN(n) ? 0 : n; }
  function rowsOf(box) {
    var key = box.getAttribute('data-source') || '';
    var rows = (window.DASH && Array.isArray(window.DASH[key])) ? window.DASH[key] : [];
    return rows;
  }
  function emptyChart(box) {
    box.innerHTML = '<div class="chart-empty">暂无数据 · 录入员工与考勤记录后自动生成</div>';
  }
  function legend(key, names, colors) {
    var box = document.getElementById('legend-' + key);
    if (!box) return;
    box.innerHTML = names.map(function (n, i) {
      return '<span><i style="background:' + colors[i % colors.length] + '"></i>' + esc(n) + '</span>';
    }).join('');
  }

  /* ---------- 折线 / 面积图 ---------- */
  function lineChart(box, rows) {
    var W = box.clientWidth || 620, H = box.clientHeight || 240;
    var pad = { l: 38, r: 12, t: 14, b: 26 };
    var iw = W - pad.l - pad.r, ih = H - pad.t - pad.b;
    var xk = box.getAttribute('data-x') || 'label';
    var yks = (box.getAttribute('data-y') || 'cnt').split(',');
    var names = (box.getAttribute('data-names') || yks.join(',')).split(',');
    var colors = (box.getAttribute('data-colors') || '#4f6bed,#f59f00,#fa5252,#12b886').split(',');
    var max = 1;
    rows.forEach(function (r) { yks.forEach(function (k) { max = Math.max(max, num(r[k])); }); });
    var step = Math.max(1, Math.ceil(max / 4)); max = step * 4;
    var svg = E('svg', { viewBox: '0 0 ' + W + ' ' + H });

    for (var i = 0; i <= 4; i++) {
      var y = pad.t + ih - ih * i / 4;
      svg.appendChild(E('line', { x1: pad.l, y1: y, x2: W - pad.r, y2: y,
        stroke: '#eef1f7', 'stroke-width': 1 }));
      svg.appendChild(T(pad.l - 7, y + 3, String(step * i), { 'text-anchor': 'end' }));
    }
    var every = Math.max(1, Math.ceil(rows.length / 8));
    rows.forEach(function (r, i) {
      if (i % every !== 0 && i !== rows.length - 1) return;
      var x = pad.l + (rows.length === 1 ? iw / 2 : iw * i / (rows.length - 1));
      svg.appendChild(T(x, H - 7, String(r[xk] == null ? '' : r[xk]), { 'text-anchor': 'middle' }));
    });
    yks.forEach(function (k, ci) {
      var color = colors[ci % colors.length].trim();
      var pts = rows.map(function (r, i) {
        return [pad.l + (rows.length === 1 ? iw / 2 : iw * i / (rows.length - 1)),
                pad.t + ih - ih * num(r[k]) / max];
      });
      if (!pts.length) return;
      var d = pts.map(function (p, i) { return (i ? 'L' : 'M') + p[0].toFixed(1) + ' ' + p[1].toFixed(1); }).join(' ');
      var area = d + ' L' + pts[pts.length - 1][0].toFixed(1) + ' ' + (pad.t + ih) +
                 ' L' + pts[0][0].toFixed(1) + ' ' + (pad.t + ih) + ' Z';
      var gradId = 'grad-' + Math.random().toString(36).slice(2, 8);
      var defs = E('defs', {});
      var lg = E('linearGradient', { id: gradId, x1: '0', y1: '0', x2: '0', y2: '1' });
      lg.appendChild(E('stop', { offset: '0%', 'stop-color': color, 'stop-opacity': '.22' }));
      lg.appendChild(E('stop', { offset: '100%', 'stop-color': color, 'stop-opacity': '0' }));
      defs.appendChild(lg); svg.appendChild(defs);
      if (ci === 0) svg.appendChild(E('path', { d: area, fill: 'url(#' + gradId + ')' }));
      svg.appendChild(E('path', { d: d, fill: 'none', stroke: color,
        'stroke-width': 2.2, 'stroke-linejoin': 'round', 'stroke-linecap': 'round' }));
      pts.forEach(function (p) {
        svg.appendChild(E('circle', { cx: p[0], cy: p[1], r: 2.6, fill: '#fff',
          stroke: color, 'stroke-width': 1.8 }));
      });
    });
    box.appendChild(svg);
    legend(box.getAttribute('data-source'), names, colors);
  }

  /* ---------- 环形图 ---------- */
  function donutChart(box, rows) {
    var W = box.clientWidth || 260, H = box.clientHeight || 240;
    var cx = W / 2, cy = H / 2, r = Math.min(W, H) / 2 - 10, rin = r * 0.62;
    var labelK = box.getAttribute('data-x') || 'label';
    var total = rows.reduce(function (s, x) { return s + num(x.cnt); }, 0);
    if (total <= 0) { emptyChart(box); return; }
    var svg = E('svg', { viewBox: '0 0 ' + W + ' ' + H });
    var angle = -Math.PI / 2;
    rows.forEach(function (row, i) {
      var frac = num(row.cnt) / total, a2 = angle + frac * Math.PI * 2;
      var color = PIE_COLORS[i % PIE_COLORS.length];
      var x1 = cx + r * Math.cos(angle), y1 = cy + r * Math.sin(angle);
      var x2 = cx + r * Math.cos(a2), y2 = cy + r * Math.sin(a2);
      var x3 = cx + rin * Math.cos(a2), y3 = cy + rin * Math.sin(a2);
      var x4 = cx + rin * Math.cos(angle), y4 = cy + rin * Math.sin(angle);
      var large = frac > 0.5 ? 1 : 0;
      var d = 'M' + x1 + ' ' + y1 + ' A' + r + ' ' + r + ' 0 ' + large + ' 1 ' + x2 + ' ' + y2 +
              ' L' + x3 + ' ' + y3 + ' A' + rin + ' ' + rin + ' 0 ' + large + ' 0 ' + x4 + ' ' + y4 + ' Z';
      svg.appendChild(E('path', { d: d, fill: color, stroke: '#fff', 'stroke-width': 2 }));
      angle = a2;
    });
    svg.appendChild(T(cx, cy - 2, String(total), { 'text-anchor': 'middle', 'font-size': '22',
      'font-weight': '700', fill: '#1b2437' }));
    svg.appendChild(T(cx, cy + 15, '总人次', { 'text-anchor': 'middle' }));
    box.appendChild(svg);
    var lbox = document.getElementById('legend-' + box.getAttribute('data-source'));
    if (lbox) {
      lbox.innerHTML = rows.map(function (row, i) {
        var name = STATUS[row[labelK]] ? STATUS[row[labelK]][0] : row[labelK];
        return '<div><i style="background:' + PIE_COLORS[i % PIE_COLORS.length] + '"></i>' +
               '<span>' + esc(name) + '</span><b>' + num(row.cnt) + '</b></div>';
      }).join('');
    }
  }

  /* ---------- 柱状图 ---------- */
  function barChart(box, rows) {
    var W = box.clientWidth || 600, H = box.clientHeight || 240;
    var pad = { l: 36, r: 12, t: 16, b: 34 };
    var iw = W - pad.l - pad.r, ih = H - pad.t - pad.b;
    var xk = box.getAttribute('data-x') || 'label';
    var yk = box.getAttribute('data-y') || 'rate';
    var color = box.getAttribute('data-color') || '#4f6bed';
    var max = Math.max.apply(null, rows.map(function (r) { return num(r[yk]); }).concat([1]));
    var step = Math.max(1, Math.ceil(max / 4)); max = step * 4;
    var svg = E('svg', { viewBox: '0 0 ' + W + ' ' + H });
    for (var i = 0; i <= 4; i++) {
      var y = pad.t + ih - ih * i / 4;
      svg.appendChild(E('line', { x1: pad.l, y1: y, x2: W - pad.r, y2: y, stroke: '#eef1f7' }));
      svg.appendChild(T(pad.l - 7, y + 3, String(step * i), { 'text-anchor': 'end' }));
    }
    var bw = Math.min(38, iw / rows.length * 0.55);
    rows.forEach(function (r, i) {
      var x = pad.l + iw * (i + 0.5) / rows.length - bw / 2;
      var h = ih * num(r[yk]) / max;
      svg.appendChild(E('rect', { x: x, y: pad.t + ih - h, width: bw, height: Math.max(1, h),
        rx: 4, fill: color, opacity: .88 }));
      svg.appendChild(T(x + bw / 2, pad.t + ih - h - 5, String(num(r[yk])), { 'text-anchor': 'middle', fill: '#4b5568' }));
      var label = String(r[xk] == null ? '' : r[xk]);
      svg.appendChild(T(x + bw / 2, H - 12, label.length > 6 ? label.substring(0, 6) : label,
        { 'text-anchor': 'middle' }));
    });
    box.appendChild(svg);
  }

  /* ---------- 水平条形排行 ---------- */
  function hbarChart(box, rows) {
    var yk = box.getAttribute('data-y') || 'cnt';
    var xk = box.getAttribute('data-x') || 'label';
    var max = Math.max.apply(null, rows.map(function (r) { return num(r[yk]); }).concat([1]));
    var html = '<div style="display:flex;flex-direction:column;gap:11px;padding:4px 2px">';
    rows.forEach(function (r, i) {
      var pct = Math.round(num(r[yk]) / max * 100);
      var label = String(r[xk] == null ? '' : r[xk]);
      html += '<div style="display:flex;align-items:center;gap:10px">' +
        '<span class="rank-num ' + (i === 0 ? 'top' : '') + '">' + (i + 1) + '</span>' +
        '<span style="width:96px;flex:0 0 96px;font-size:12px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">' + esc(label) + '</span>' +
        '<div class="progress" style="flex:1"><i style="width:' + pct + '%"></i></div>' +
        '<b style="width:52px;text-align:right;font-size:12px">' + num(r[yk]) + '</b></div>';
    });
    box.innerHTML = html + '</div>';
  }

  function renderCharts() {
    document.querySelectorAll('[data-chart]').forEach(function (box) {
      var rows = rowsOf(box);
      if (!rows.length) { emptyChart(box); return; }
      // 统一在这里清空：避免 resize 回调先清空、260ms 后才重绘，中间出现一块空白
      box.innerHTML = '';
      var kind = box.getAttribute('data-chart');
      try {
        if (kind === 'line') lineChart(box, rows);
        else if (kind === 'donut') donutChart(box, rows);
        else if (kind === 'bar') barChart(box, rows);
        else if (kind === 'hbar') hbarChart(box, rows);
      } catch (err) {
        box.innerHTML = '<div class="chart-empty">图表渲染失败：' + esc(err.message) + '</div>';
      }
    });
  }

  /* ---------- 启动 ---------- */
  document.addEventListener('DOMContentLoaded', function () {
    initSidebar();
    paintBadges();
    paintDates();
    paintPagers();
    initMisc();
    renderCharts();
  });
  window.addEventListener('resize', function () {
    clearTimeout(window.__att_rz);
    window.__att_rz = setTimeout(function () {
      // 不再先 b.innerHTML=''：交给 renderCharts 内部同步清空+重绘，减少缩放时的空白闪烁
      renderCharts();
    }, 200);
  });
})();
