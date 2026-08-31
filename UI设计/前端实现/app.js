(function () {
  'use strict';

  const DATA = window.ITSM_DEMO_DATA;
  const appRoot = document.getElementById('app');
  const overlayRoot = document.getElementById('overlay-root');
  const toastRoot = document.getElementById('toast-root');

  const navItems = [
    { key: 'HOME', label: '首页', icon: 'H' },
    { key: 'TICKET', label: '工单', icon: 'W' },
    { key: 'CONFIG', label: '配置', icon: 'C' }
  ];

  const STATUS_LABELS = {
    NEW: '新建',
    PENDING_ACCEPTANCE: '待受理',
    IN_PROGRESS: '处理中',
    PENDING_USER_CONFIRM: '待确认',
    RESOLVED: '已解决',
    CLOSED: '已关闭',
    REOPENED: '已重开'
  };

  const PRIORITY_LABELS = {
    HIGH: '高',
    MEDIUM: '中',
    LOW: '低'
  };

  const SUPPORT_AGENTS = [
    { userId: 'usr_support_01', displayName: '客服一', businessLineCodes: ['IT_SUPPORT', 'HR_SYSTEM'] },
    { userId: 'usr_support_02', displayName: '客服二', businessLineCodes: ['IT_SUPPORT', 'ERP'] },
    { userId: 'usr_support_03', displayName: '客服三', businessLineCodes: ['IT_SUPPORT', 'ERP'] },
    { userId: 'usr_support_04', displayName: '客服四', businessLineCodes: ['IT_SUPPORT', 'HR_SYSTEM'] },
    { userId: 'usr_retail_01', displayName: '零售客服一', businessLineCodes: ['RETAIL'] },
    { userId: 'usr_aftersales_01', displayName: '售后客服一', businessLineCodes: ['AFTER_SALES'] },
    { userId: 'usr_logistics_01', displayName: '物流客服一', businessLineCodes: ['LOGISTICS'] }
  ];

  const BUSINESS_LINES = [
    { code: 'IT_SUPPORT', name: '桌面 IT', description: '桌面、网络与办公系统支持' },
    { code: 'RETAIL', name: '零售', description: '门店、零售业务问题' },
    { code: 'AFTER_SALES', name: '售后', description: '售后服务与退换处理' },
    { code: 'LOGISTICS', name: '物流', description: '配送、仓储与物流查询' },
    { code: 'HR_SYSTEM', name: '人力资源系统', description: 'HR 门户与审批流程' },
    { code: 'ERP', name: 'ERP', description: '财务、供应链与业务系统' }
  ];

  const dictTabs = [
    { key: 'BUSINESS_SYSTEM', label: '业务系统' },
    { key: 'MANAGEMENT_UNIT', label: '管理单元' },
    { key: 'SYMPTOM', label: '症状' },
    { key: 'REASON', label: '原因' },
    { key: 'SOLUTION_METHOD', label: '解决方法' }
  ];

  const state = {
    loggedIn: true,
    activeProfileKey: 'support',
    route: 'HOME',
    activeSessionId: DATA.sessions[0] ? DATA.sessions[0].sessionId : null,
    activeTicketId: DATA.tickets[0] ? DATA.tickets[0].ticketId : null,
    activeDictTab: 'MANAGEMENT_UNIT',
    activeRoleId: 'role_support_agent',
    overlay: null,
    filters: {
      sessionKeyword: '',
      ticketKeyword: '',
      supportTicketNo: '',
      supportRequester: '',
      supportTicketView: 'all',
      supportPage: 1,
      serviceDeskTab: 'pending',
      dictKeyword: '',
      roleKeyword: '',
      ticketView: 'all',
      ticketStatus: 'all',
      ticketPriority: 'all',
      supervisorPeriod: 'month'
    },
    draft: {
      message: '',
      ratingScore: 0,
      ratingComment: ''
    },
    loading: {
      login: false,
      permissions: false
    }
  };

  function escapeHtml(value) {
    return String(value == null ? '' : value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function clone(value) {
    if (typeof structuredClone === 'function') {
      return structuredClone(value);
    }
    return JSON.parse(JSON.stringify(value));
  }

  function delay(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  function nowIso() {
    return new Date().toISOString();
  }

  function isoFromMinutesAgo(minutes) {
    return new Date(Date.now() - minutes * 60 * 1000).toISOString();
  }

  function normalizeSupportDemoData() {
    const requesterTemplates = [
      { userId: 'usr_10001', displayName: '张三', departmentName: '技术支持部' },
      { userId: 'usr_20002', displayName: '李四', departmentName: '人力资源部' },
      { userId: 'usr_30003', displayName: '王五', departmentName: '财务部' },
      { userId: 'usr_40004', displayName: '赵六', departmentName: '市场部' },
      { userId: 'usr_50005', displayName: '孙七', departmentName: '运营部' },
      { userId: 'usr_60006', displayName: '周八', departmentName: '门店一部' },
      { userId: 'usr_70007', displayName: '吴九', departmentName: '供应链部' }
    ];
    const rows = [
      ['3053008', 'Outlook 无法收取新邮件', 'MEDIUM', 'IT_SUPPORT', 'PENDING_ACCEPTANCE', 12, null],
      ['3053009', '共享盘权限无法访问', 'HIGH', 'IT_SUPPORT', 'PENDING_ACCEPTANCE', 17, null],
      ['3053010', '新电脑无法加入域', 'HIGH', 'IT_SUPPORT', 'PENDING_ACCEPTANCE', 23, null],
      ['3053011', '浏览器主页被篡改', 'LOW', 'IT_SUPPORT', 'PENDING_ACCEPTANCE', 31, null],
      ['3053012', '网络时断时续', 'MEDIUM', 'IT_SUPPORT', 'IN_PROGRESS', 45, 'usr_support_01'],
      ['3053013', '操作系统更新失败', 'HIGH', 'IT_SUPPORT', 'IN_PROGRESS', 58, 'usr_support_01'],
      ['3053014', '远程桌面黑屏', 'MEDIUM', 'IT_SUPPORT', 'IN_PROGRESS', 72, 'usr_support_01'],
      ['3053015', '邮箱自动回复规则异常', 'LOW', 'IT_SUPPORT', 'IN_PROGRESS', 86, 'usr_support_02'],
      ['3053016', '内部系统登录后闪退', 'HIGH', 'IT_SUPPORT', 'IN_PROGRESS', 101, 'usr_support_01'],
      ['3053017', '门店收银机无法开机', 'HIGH', 'RETAIL', 'IN_PROGRESS', 118, 'usr_retail_01'],
      ['3053018', '退货单状态未同步', 'MEDIUM', 'AFTER_SALES', 'PENDING_ACCEPTANCE', 132, null],
      ['3053019', '物流单号查询超时', 'MEDIUM', 'LOGISTICS', 'PENDING_ACCEPTANCE', 141, null],
      ['3053020', '审批中心页面无数据', 'HIGH', 'HR_SYSTEM', 'IN_PROGRESS', 153, 'usr_support_02'],
      ['3053021', 'SAP 凭证无法打印', 'MEDIUM', 'ERP', 'PENDING_ACCEPTANCE', 166, null],
      ['3053022', '桌面文件被加密', 'HIGH', 'IT_SUPPORT', 'REOPENED', 179, 'usr_support_01'],
      ['3053023', '打印机端口丢失', 'LOW', 'IT_SUPPORT', 'PENDING_USER_CONFIRM', 188, 'usr_support_01'],
      ['3053024', '软件许可证即将到期', 'MEDIUM', 'IT_SUPPORT', 'RESOLVED', 198, 'usr_support_01'],
      ['3053025', '无线网络无法自动连接', 'MEDIUM', 'IT_SUPPORT', 'CLOSED', 216, 'usr_support_02']
    ];

    rows.forEach(([ticketNo, title, priority, businessLineCode, status, minutesAgo, assigneeId]) => {
      if (DATA.tickets.some((ticket) => ticket.ticketNo === ticketNo)) return;
      const requester = requesterTemplates[(Number(ticketNo) - 3053008) % requesterTemplates.length];
      const assignee = SUPPORT_AGENTS.find((agent) => agent.userId === assigneeId) || null;
      const createdAt = isoFromMinutesAgo(minutesAgo + 8);
      const acceptedAt = ['IN_PROGRESS', 'PENDING_USER_CONFIRM', 'RESOLVED', 'CLOSED'].includes(status) ? isoFromMinutesAgo(minutesAgo) : null;
      const managementUnitId = title.includes('网络') || title.includes('无线') || title.includes('远程') ? 'dict_unit_network' : title.includes('操作系统') || title.includes('系统更新') ? 'dict_unit_os' : null;
      const history = [
        { status: 'NEW', occurredAt: createdAt, operator: '系统', note: '用户提交服务请求' },
        { status: 'PENDING_ACCEPTANCE', occurredAt: createdAt, operator: '系统', note: '进入客服队列' }
      ];
      if (acceptedAt) history.push({ status: 'IN_PROGRESS', occurredAt: acceptedAt, operator: assignee ? assignee.displayName : '客服', note: '受理工单并开始处理' });
      if (status === 'PENDING_USER_CONFIRM') history.push({ status, occurredAt: isoFromMinutesAgo(Math.max(0, minutesAgo - 20)), operator: assignee ? assignee.displayName : '客服', note: '已提交解决结果' });
      if (status === 'RESOLVED') history.push({ status: 'PENDING_USER_CONFIRM', occurredAt: isoFromMinutesAgo(Math.max(0, minutesAgo - 20)), operator: assignee ? assignee.displayName : '客服', note: '已提交解决结果' }, { status, occurredAt: isoFromMinutesAgo(Math.max(0, minutesAgo - 15)), operator: requester.displayName, note: '用户确认解决' });
      if (status === 'CLOSED') history.push({ status: 'PENDING_USER_CONFIRM', occurredAt: isoFromMinutesAgo(Math.max(0, minutesAgo - 35)), operator: assignee ? assignee.displayName : '客服', note: '已提交解决结果' }, { status: 'RESOLVED', occurredAt: isoFromMinutesAgo(Math.max(0, minutesAgo - 30)), operator: requester.displayName, note: '用户确认解决' }, { status, occurredAt: isoFromMinutesAgo(Math.max(0, minutesAgo - 25)), operator: assignee ? assignee.displayName : '客服', note: '工单已关闭' });
      DATA.tickets.push({
        ticketId: `tkt_${ticketNo}`,
        ticketNo,
        tenantId: 'tenant_001',
        requester,
        title,
        description: `${requester.displayName}提交的问题：${title}。`,
        source: 'MANUAL',
        status,
        priority,
        businessLineCode,
        queueId: `queue_${businessLineCode.toLowerCase()}`,
        classification: { managementUnitId, symptomId: null, reasonId: null, solutionMethodId: null, customReason: null, customSolution: ['PENDING_USER_CONFIRM', 'RESOLVED', 'CLOSED'].includes(status) ? '已按标准流程处理并验证。' : null },
        assignee,
        conversation: { sessionId: `ses_${ticketNo}`, summary: `${title}，已保留用户描述与处理上下文。`, messageCount: 2 + (Number(ticketNo) % 4) },
        statusHistory: history,
        auditEvents: acceptedAt ? [{ action: 'TicketAccepted', occurredAt: acceptedAt, actor: assignee ? assignee.userId : 'system' }] : [],
        ownershipHistory: acceptedAt ? [{ from: '客服队列', to: assignee ? assignee.displayName : '待分配', operator: assignee ? assignee.displayName : '系统', occurredAt: acceptedAt, stage: '受理', note: '进入处理中并成为责任人' }] : [],
        resolution: ['PENDING_USER_CONFIRM', 'RESOLVED', 'CLOSED'].includes(status) ? { summary: '问题已处理并完成验证。', method: '标准解决方法' } : null,
        rating: status === 'CLOSED' ? { score: 5, comment: '处理及时' } : null,
        isSuspended: false,
        suspendedAt: null,
        suspendedReason: '',
        slaPausedAt: null,
        slaAccumulatedMs: 0,
        slaHours: managementUnitId === 'dict_unit_network' ? 4 : managementUnitId === 'dict_unit_os' ? 3 : 8,
        slaStartedAt: acceptedAt || createdAt,
        createdAt,
        updatedAt: history[history.length - 1].occurredAt
      });
    });

    const unitDict = DATA.dictionaries.MANAGEMENT_UNIT;
    if (unitDict && !unitDict.items.some((item) => item.itemId === 'dict_unit_os')) {
      unitDict.items.push({ itemId: 'dict_unit_os', code: 'OS', name: '操作系统', parentId: null, enabled: true, sort: 25, version: 1, description: '操作系统安装、更新与故障', updatedAt: nowIso(), updatedBy: 'admin' });
    }
    DATA.supportAgents = SUPPORT_AGENTS;
    DATA.businessLines = BUSINESS_LINES;
    DATA.tickets.forEach((ticket) => {
      const classification = ticket.classification || {};
      if (!ticket.slaHours) ticket.slaHours = classification.managementUnitId === 'dict_unit_network' ? 4 : classification.managementUnitId === 'dict_unit_os' ? 3 : 8;
      if (!ticket.slaStartedAt) {
        const acceptedEvent = (ticket.statusHistory || []).slice().reverse().find((item) => item.status === 'IN_PROGRESS');
        ticket.slaStartedAt = acceptedEvent ? acceptedEvent.occurredAt : ticket.createdAt;
      }
      ticket.isSuspended = Boolean(ticket.isSuspended);
      ticket.suspendedAt = ticket.suspendedAt || null;
      ticket.suspendedReason = ticket.suspendedReason || '';
      ticket.slaPausedAt = ticket.slaPausedAt || null;
      ticket.slaAccumulatedMs = Number(ticket.slaAccumulatedMs || 0);
      ticket.ownershipHistory = ticket.ownershipHistory || [];
      if (!ticket.ownershipHistory.length && ticket.assignee) {
        const acceptedEvent = (ticket.statusHistory || []).slice().reverse().find((item) => item.status === 'IN_PROGRESS');
        ticket.ownershipHistory.push({ from: '客服队列', to: ticket.assignee.displayName, operator: ticket.assignee.displayName, occurredAt: acceptedEvent ? acceptedEvent.occurredAt : ticket.createdAt, stage: '受理', note: acceptedEvent ? acceptedEvent.note : '成为工单责任人' });
      }
    });
  }

  function updateSlaIndicators() {
    document.querySelectorAll('[data-sla-ticket-id]').forEach((element) => {
      const ticket = DATA.tickets.find((item) => item.ticketId === element.dataset.slaTicketId);
      if (!ticket) return;
      const info = slaInfo(ticket);
      const fill = element.querySelector('.sla-progress-fill');
      if (fill) {
        fill.style.width = `${info.percent}%`;
        fill.classList.toggle('timed-out', info.timedOut);
        fill.classList.toggle('suspended', Boolean(ticket.isSuspended));
      }
      const elapsed = element.querySelector('[data-sla-part="elapsed"]');
      const remaining = element.querySelector('[data-sla-part="remaining"]');
      const label = element.querySelector('[data-sla-part="label"]');
      if (elapsed) elapsed.textContent = formatDuration(info.elapsedMs);
      if (remaining) remaining.textContent = info.timedOut ? formatDuration(info.overMs) : formatDuration(info.remainingMs);
      if (label) label.textContent = info.label;
    });
  }

  function startSlaClock() {
    if (window.__itsmSlaClock) return;
    window.__itsmSlaClock = window.setInterval(updateSlaIndicators, 1000);
  }

  function businessLineView(code) {
    return (DATA.businessLines || BUSINESS_LINES).find((item) => item.code === code) || { code, name: code };
  }

  function businessLineName(code) {
    return businessLineView(code).name;
  }

  function supportScopeCodes() {
    const dataScope = currentProfile().permissions.data.dataScope || {};
    return dataScope.businessLineCodes || [];
  }

  function supportTickets() {
    const currentUserId = currentUser().userId;
    const scopeCodes = supportScopeCodes();
    return tenantTickets().filter((ticket) => {
      if (isSupervisorRole()) return true;
      const isAssigned = ticket.assignee && ticket.assignee.userId === currentUserId;
      return isAssigned || !scopeCodes.length || scopeCodes.includes(ticket.businessLineCode);
    }).sort((a, b) => new Date(b.updatedAt || b.createdAt) - new Date(a.updatedAt || a.createdAt));
  }

  function supportDraftTickets() {
    return supportTickets().filter((ticket) => ['NEW', 'PENDING_ACCEPTANCE'].includes(ticket.status));
  }

  function supportMineTickets() {
    const currentUserId = currentUser().userId;
    return supportTickets().filter((ticket) => ticket.assignee && ticket.assignee.userId === currentUserId && !['RESOLVED', 'CLOSED'].includes(ticket.status));
  }

  function supportPostTickets() {
    const currentUserId = currentUser().userId;
    return supportTickets().filter((ticket) => ['PENDING_ACCEPTANCE', 'REOPENED'].includes(ticket.status) && (!ticket.assignee || ticket.assignee.userId !== currentUserId));
  }

  function formatDuration(ms) {
    if (!Number.isFinite(ms) || ms <= 0) return '0 分钟';
    const totalMinutes = Math.floor(ms / 60000);
    const days = Math.floor(totalMinutes / 1440);
    const hours = Math.floor((totalMinutes % 1440) / 60);
    const minutes = totalMinutes % 60;
    if (days > 0) return `${days} 天 ${hours} 小时`;
    if (hours > 0) return `${hours} 小时 ${minutes} 分钟`;
    return `${minutes} 分钟`;
  }

  function slaInfo(ticket) {
    const terminal = ['PENDING_USER_CONFIRM', 'RESOLVED', 'CLOSED'].includes(ticket.status);
    const start = new Date(ticket.slaStartedAt || ticket.createdAt);
    if (!Number.isFinite(start.getTime())) {
      return { totalMs: 0, elapsedMs: 0, percent: 0, remainingMs: 0, timedOut: false, overMs: 0, label: '未开始计时' };
    }
    const totalMs = Number(ticket.slaHours || 8) * 60 * 60 * 1000;
    let elapsedMs = Number(ticket.slaAccumulatedMs || 0);
    if (!terminal && !ticket.isSuspended) elapsedMs += Math.max(0, Date.now() - start.getTime());
    const percent = Math.min(100, Math.round((elapsedMs / totalMs) * 100));
    const timedOut = elapsedMs >= totalMs;
    const overMs = Math.max(0, elapsedMs - totalMs);
    return {
      totalMs,
      elapsedMs,
      percent,
      timedOut,
      overMs,
      remainingMs: Math.max(0, totalMs - elapsedMs),
      label: terminal ? '处理已结束' : timedOut ? `已超时 ${formatDuration(overMs)}` : ticket.isSuspended ? '已挂起，计时暂停' : '处理中'
    };
  }

  function slaRisk(ticket) {
    const info = slaInfo(ticket);
    return ['IN_PROGRESS', 'REOPENED'].includes(ticket.status) && !ticket.isSuspended && info.percent >= 70;
  }

  function currentSupportAgent() {
    const currentUserId = currentUser().userId;
    return (DATA.supportAgents || SUPPORT_AGENTS).find((agent) => agent.userId === currentUserId) || { userId: currentUserId, displayName: currentUser().displayName, businessLineCodes: supportScopeCodes() };
  }

  function sameLineAgents(ticket) {
    const currentUserId = currentUser().userId;
    return (DATA.supportAgents || SUPPORT_AGENTS).filter((agent) => agent.userId !== currentUserId && agent.businessLineCodes.includes(ticket.businessLineCode));
  }

  function appendTicketHistory(ticket, status, note) {
    const now = nowIso();
    ticket.statusHistory = ticket.statusHistory || [];
    ticket.statusHistory.unshift({ status, occurredAt: now, operator: currentUser().displayName, note });
    ticket.updatedAt = now;
  }

  function appendOwnershipHistory(ticket, fromName, toName, stage, note) {
    ticket.ownershipHistory = ticket.ownershipHistory || [];
    ticket.ownershipHistory.unshift({ from: fromName, to: toName, operator: currentUser().displayName, occurredAt: nowIso(), stage, note });
  }

  function formatDateTime(iso) {
    if (!iso) return '-';
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) return String(iso);
    return date.toLocaleString('zh-CN', {
      hour12: false,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    }).replace(/\//g, '-');
  }

  function formatDate(iso) {
    if (!iso) return '-';
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) return String(iso);
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    }).replace(/\//g, '-');
  }

  function showToast(title, message, type = 'info', timeout = 2600) {
    const node = document.createElement('div');
    node.className = `toast ${type}`;
    node.innerHTML = `<strong>${escapeHtml(title)}</strong><p>${escapeHtml(message)}</p>`;
    toastRoot.appendChild(node);
    window.setTimeout(() => {
      node.style.opacity = '0';
      node.style.transform = 'translateY(4px)';
      node.style.transition = 'opacity 0.2s ease, transform 0.2s ease';
    }, timeout);
    window.setTimeout(() => node.remove(), timeout + 260);
  }

  function currentProfile() {
    return DATA.profiles[state.activeProfileKey];
  }

  function currentUser() {
    return currentProfile().me.data;
  }

  function currentTenant() {
    return currentProfile().login.data.tenant;
  }

  function currentRoleCodes() {
    const permissions = currentProfile().permissions.data;
    return (permissions.roles || []).map((role) => role.roleCode);
  }

  function currentRoleLabel() {
    const permissions = currentProfile().permissions.data;
    return (permissions.roles || []).map((role) => role.roleName).join(' / ') || currentProfile().label;
  }

  function isUserRole() {
    return currentRoleCodes().includes('USER');
  }

  function isSupportRole() {
    const roles = currentRoleCodes();
    return roles.includes('SUPPORT_AGENT') || roles.includes('SUPPORT_ADMIN') || roles.includes('SUPERVISOR');
  }

  function isSupervisorRole() {
    return currentRoleCodes().includes('SUPERVISOR');
  }

  function hasPermission(code) {
    return currentProfile().permissions.data.permissions.includes(code);
  }

  function hasMenu(menu) {
    return currentProfile().permissions.data.menus.includes(menu);
  }

  function getCurrentSession() {
    return DATA.sessions.find((item) => item.sessionId === state.activeSessionId) || DATA.sessions[0];
  }

  function getCurrentTicket() {
    return DATA.tickets.find((item) => item.ticketId === state.activeTicketId) || DATA.tickets[0];
  }

  function getTicketById(ticketId) {
    return DATA.tickets.find((item) => item.ticketId === ticketId) || getCurrentTicket();
  }

  function statusLabel(status) {
    return STATUS_LABELS[status] || status || '-';
  }

  function priorityLabel(priority) {
    return PRIORITY_LABELS[priority] || priority || '-';
  }

  function dictionaryLabel(dictType, itemId) {
    if (!itemId || !DATA.dictionaries[dictType]) return itemId || '-';
    const item = DATA.dictionaries[dictType].items.find((entry) => entry.itemId === itemId);
    return item ? item.name : itemId;
  }

  function tenantTickets() {
    return DATA.tickets.filter((ticket) => ticket.tenantId === currentTenant().tenantId);
  }

  function getIsoWeekInfo(date) {
    const utcDate = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
    const day = utcDate.getUTCDay() || 7;
    utcDate.setUTCDate(utcDate.getUTCDate() + 4 - day);
    const weekYear = utcDate.getUTCFullYear();
    const yearStart = new Date(Date.UTC(weekYear, 0, 1));
    const week = Math.ceil((((utcDate - yearStart) / 86400000) + 1) / 7);
    return { weekYear, week };
  }

  function ticketPeriodKey(ticket, period) {
    const date = new Date(ticket.createdAt);
    if (Number.isNaN(date.getTime())) return 'unknown';
    if (period === 'year') return String(date.getFullYear());
    if (period === 'week') {
      const info = getIsoWeekInfo(date);
      return `${info.weekYear}-W${String(info.week).padStart(2, '0')}`;
    }
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
  }

  function ticketPeriodLabel(key, period) {
    if (key === 'unknown') return '未知时间';
    if (period === 'year') return `${key} 年`;
    if (period === 'week') {
      const parts = key.split('-W');
      return `${parts[0]} 年第 ${Number(parts[1])} 周`;
    }
    const parts = key.split('-');
    return `${parts[0]} 年 ${Number(parts[1])} 月`;
  }

  function summarizeTickets(tickets, period, key) {
    const closedCount = tickets.filter((ticket) => ['RESOLVED', 'CLOSED'].includes(ticket.status)).length;
    return {
      key,
      period,
      label: ticketPeriodLabel(key, period),
      total: tickets.length,
      pending: tickets.filter((ticket) => ['PENDING_ACCEPTANCE', 'REOPENED'].includes(ticket.status)).length,
      progress: tickets.filter((ticket) => ticket.status === 'IN_PROGRESS').length,
      confirm: tickets.filter((ticket) => ticket.status === 'PENDING_USER_CONFIRM').length,
      resolved: tickets.filter((ticket) => ticket.status === 'RESOLVED').length,
      closed: tickets.filter((ticket) => ticket.status === 'CLOSED').length,
      closedCount,
      highPriority: tickets.filter((ticket) => ticket.priority === 'HIGH').length,
      closureRate: tickets.length ? Math.round((closedCount / tickets.length) * 100) : 0
    };
  }

  function supervisorSummary(period) {
    const grouped = new Map();
    tenantTickets().forEach((ticket) => {
      const key = ticketPeriodKey(ticket, period);
      if (!grouped.has(key)) grouped.set(key, []);
      grouped.get(key).push(ticket);
    });
    return Array.from(grouped.entries())
      .map(([key, tickets]) => summarizeTickets(tickets, period, key))
      .sort((a, b) => b.key.localeCompare(a.key));
  }

  function visibleSupervisorTickets() {
    const keyword = state.filters.ticketKeyword.trim().toLowerCase();
    const status = state.filters.ticketStatus;
    const priority = state.filters.ticketPriority;
    return tenantTickets()
      .filter((ticket) => {
        const byStatus = status === 'all' || ticket.status === status;
        const byPriority = priority === 'all' || ticket.priority === priority;
        const byKeyword = !keyword || [
          ticket.ticketNo,
          ticket.title,
          ticket.description,
          ticket.requester.displayName,
          ticket.requester.departmentName,
          ticket.assignee ? ticket.assignee.displayName : '',
          ticket.businessLineCode,
          ticket.status
        ].join(' ').toLowerCase().includes(keyword);
        return byStatus && byPriority && byKeyword;
      })
      .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  }

  function visibleTickets() {
    const keyword = state.filters.ticketKeyword.trim().toLowerCase();
    const view = state.filters.ticketView;
    const userId = currentUser().userId;

    return DATA.tickets.filter((ticket) => {
      const isMine = ticket.requester.userId === userId;
      const byRole = isUserRole() ? isMine : true;
      const byView = view === 'all'
        || (view === 'pending' && ['PENDING_ACCEPTANCE', 'REOPENED'].includes(ticket.status))
        || (view === 'progress' && ticket.status === 'IN_PROGRESS')
        || (view === 'confirm' && ticket.status === 'PENDING_USER_CONFIRM')
        || (view === 'closed' && ['RESOLVED', 'CLOSED'].includes(ticket.status));
      const byKeyword = !keyword || [ticket.ticketNo, ticket.title, ticket.description, ticket.requester.displayName, ticket.status, statusLabel(ticket.status), ticket.businessLineCode].join(' ').toLowerCase().includes(keyword);
      return byRole && byView && byKeyword;
    });
  }

  function visibleSessions() {
    const keyword = state.filters.sessionKeyword.trim().toLowerCase();
    const userId = currentUser().userId;
    return DATA.sessions.filter((session) => {
      const byRole = isUserRole() ? session.userId === userId : true;
      const byKeyword = !keyword || [session.subject, session.summary, session.status].join(' ').toLowerCase().includes(keyword);
      return byRole && byKeyword;
    }).slice().sort((a, b) => new Date(b.lastMessageAt || b.createdAt) - new Date(a.lastMessageAt || a.createdAt));
  }

  function visibleDictItems(dictType) {
    const dict = DATA.dictionaries[dictType];
    const keyword = state.filters.dictKeyword.trim().toLowerCase();
    return dict.items.filter((item) => {
      return !keyword || [item.code, item.name, item.description || '', item.parentId || ''].join(' ').toLowerCase().includes(keyword);
    });
  }

  function statusClass(status) {
    return `status-${status}`;
  }

  function priorityClass(priority) {
    return `priority-${priority}`;
  }

  function actionButton(label, action, extra = '') {
    return `<button class="${extra || 'small-button'}" data-action="${escapeHtml(action)}">${escapeHtml(label)}</button>`;
  }

  function metricCard(label, value, desc) {
    return `
      <div class="panel kpi">
        <div class="label">${escapeHtml(label)}</div>
        <div class="value">${escapeHtml(value)}</div>
        <div class="desc">${escapeHtml(desc)}</div>
      </div>
    `;
  }

  function routeTitle(route) {
    if (route === 'HOME') return isSupervisorRole() ? '主管总览' : '首页';
    if (route === 'TICKET') return isSupervisorRole() ? '全部工单' : '工单';
    if (route === 'CONFIG') return '配置';
    return '首页';
  }

  function shell(pageHtml) {
    const menus = navItems.filter((item) => item.key === 'HOME' || hasMenu(item.key));
    const activeRoute = state.route === 'LOGIN' ? 'HOME' : state.route;
    appRoot.innerHTML = `
      <div class="app">
        <div class="top-strip">
          <span><strong>${escapeHtml(DATA.appName)}</strong> · ${escapeHtml(DATA.stats.workspaceName)}</span>
          <span>${escapeHtml(currentTenant().tenantName)} · ${escapeHtml(currentUser().displayName)}</span>
        </div>
        <div class="app-shell">
          <aside class="sidebar">
            <div class="brand">ITSM</div>
            <div class="nav-list">
              ${menus.map((item) => `
                <button class="nav-button ${item.key === activeRoute ? 'active' : ''}" data-action="nav" data-route="${item.key}">
                  <span class="nav-icon">${escapeHtml(item.icon)}</span>
                  <span class="nav-label">${escapeHtml(item.label)}</span>
                </button>
              `).join('')}
            </div>
          </aside>
          <main class="content">
            <div class="header">
              <div class="breadcrumbs">
                <span>ITSM</span>
                <span class="sep">/</span>
                <strong>${escapeHtml(routeTitle(activeRoute))}</strong>
                <span class="sep">/</span>
                <span>${escapeHtml(currentTenant().tenantName)}</span>
              </div>
              <div class="header-actions">
                <span class="status-chip ok">${escapeHtml(currentRoleLabel())}</span>
                <button class="toolbar-button" data-action="reload-permissions" ${state.loading.permissions ? 'disabled' : ''}>${state.loading.permissions ? '刷新中...' : '刷新权限'}</button>
                <button class="toolbar-button" data-action="close-itsm">返回门户</button>
              </div>
            </div>
            <div class="page-tabs">
              ${menus.map((item) => `
                <button class="tab-button ${item.key === activeRoute ? 'active' : ''}" data-action="nav" data-route="${item.key}">${escapeHtml(item.label)}</button>
              `).join('')}
            </div>
            ${pageHtml}
          </main>
        </div>
      </div>
    `;
    if (state.overlay) {
      renderOverlay();
    } else {
      overlayRoot.innerHTML = '';
    }
  }

  function renderUserHomePage() {
    const session = getCurrentSession();
    const ticket = session.ticketId ? DATA.tickets.find((item) => item.ticketId === session.ticketId) : null;
    const sessions = visibleSessions();
    const quickQuestions = DATA.quickQuestions.slice(0, 6);
    const currentDay = formatDate(nowIso());
    let lastDivider = '';

    const chatMessages = session.messages.map((message) => {
      const messageDay = formatDate(message.createdAt);
      const divider = messageDay !== lastDivider
        ? `<div class="chat-divider"><span>${escapeHtml(messageDay === currentDay ? '今天' : messageDay)}</span></div>`
        : '';
      lastDivider = messageDay;

      const senderClass = message.senderType === 'USER' ? 'user' : message.senderType === 'SYSTEM' ? 'system' : 'assistant';
      const senderLabel = message.senderType === 'USER' ? currentUser().displayName : message.senderType === 'SYSTEM' ? '系统' : '智能助手';
      const avatar = message.senderType === 'USER' ? '我' : message.senderType === 'SYSTEM' ? '系' : '助';

      return `
        ${divider}
        <div class="message ${senderClass}">
          <div class="avatar">${escapeHtml(avatar)}</div>
          <div class="message-body">
            <div class="message-meta-top">
              <span>${escapeHtml(senderLabel)}</span>
              <span>${escapeHtml(formatDateTime(message.createdAt))}</span>
            </div>
            <div class="bubble">${escapeHtml(message.content)}</div>
          </div>
        </div>
      `;
    }).join('');

    const sessionCards = sessions.map((item) => {
      const avatar = item.subject ? item.subject.trim().charAt(0) : '会';
      return `
        <button class="conversation-item ${item.sessionId === session.sessionId ? 'active' : ''}" data-action="select-session" data-session-id="${item.sessionId}">
          <div class="conversation-avatar">${escapeHtml(avatar)}</div>
          <div class="conversation-meta">
            <div class="conversation-title">
              <span>${escapeHtml(item.subject)}</span>
              <span class="conversation-time">${escapeHtml(formatDate(item.lastMessageAt || item.createdAt))}</span>
            </div>
            <div class="conversation-preview">${escapeHtml(item.summary)}</div>
            <div class="conversation-footer">
              <span class="status-tag ${statusClass(item.status)}">${escapeHtml(item.status)}</span>
              ${item.ticketId ? `<span class="mini-badge">${escapeHtml(item.ticketId)}</span>` : ''}
            </div>
          </div>
        </button>
      `;
    }).join('');

    const quickToolbar = quickQuestions.map((question) => `
      <button class="quick-chip" data-action="suggestion" data-text="${escapeHtml(question)}">${escapeHtml(question)}</button>
    `).join('');

    const rightTicket = ticket ? `
      <div class="info-card">
        <div class="info-label">关联工单</div>
        <div class="info-title">${escapeHtml(ticket.ticketNo)} · ${escapeHtml(ticket.title)}</div>
        <div class="info-meta">${escapeHtml(ticket.status)} · ${escapeHtml(ticket.priority)} · ${escapeHtml(ticket.businessLineCode)}</div>
        <div class="info-lines">
          <div><span>处理人</span><strong>${escapeHtml(ticket.assignee ? ticket.assignee.displayName : '待分配')}</strong></div>
          <div><span>更新时间</span><strong>${escapeHtml(formatDateTime(ticket.updatedAt))}</strong></div>
          <div><span>解决方式</span><strong>${escapeHtml(ticket.resolution ? ticket.resolution.method : '暂无')}</strong></div>
        </div>
      </div>
    ` : `
      <div class="info-card">
        <div class="empty-state" style="min-height:160px;">
          <div class="empty-icon">单</div>
          <strong>暂未创建工单</strong>
          <p>当前会话尚未进入客服流程</p>
        </div>
      </div>
    `;

    const rightRating = ticket && ['RESOLVED', 'CLOSED'].includes(ticket.status) ? `
      <div class="info-card">
        <div class="info-label">工单评价</div>
        <div class="rating-stars" style="margin:8px 0 10px;">
          ${[1, 2, 3, 4, 5].map((score) => `
            <button class="rating-star ${state.draft.ratingScore === score ? 'selected' : ''}" data-action="set-rating" data-score="${score}">${score}</button>
          `).join('')}
        </div>
        <textarea class="textarea-input" id="ratingComment" placeholder="补充评价内容">${escapeHtml(state.draft.ratingComment)}</textarea>
        <div class="card-actions" style="margin-top:10px;">
          <button class="primary-button" data-action="submit-rating" data-ticket-id="${ticket.ticketId}">提交评价</button>
          <button class="ghost-button" data-action="reopen-ticket" data-ticket-id="${ticket.ticketId}">重开工单</button>
        </div>
      </div>
    ` : '';

    const page = `
      <div class="page user-chat-page">
        <div class="user-chat-toolbar panel">
          <div class="user-chat-toolbar-left">
            <div class="workspace-icon">F</div>
            <div>
              <div class="workspace-title">我的助手</div>
              <div class="workspace-subtitle">普通聊天界面 · 飞书式桌面布局</div>
            </div>
          </div>
          <div class="user-chat-toolbar-center">
            <input class="text-input" data-bind="sessionKeyword" placeholder="搜索会话、工单或消息" value="${escapeHtml(state.filters.sessionKeyword)}">
          </div>
          <div class="user-chat-toolbar-right">
            <button class="icon-button primary" type="button" data-action="new-session" title="新建会话">＋</button>
            <button class="icon-button" type="button" data-action="handoff" title="转人工">↗</button>
            ${hasPermission('ticket:create') ? '<button class="icon-button" type="button" data-action="create-ticket" title="创建工单">工</button>' : ''}
          </div>
        </div>

        <section class="feishu-layout">
          <aside class="panel feishu-sidebar">
            <div class="panel-header">
              <h3>会话</h3>
              <small>${sessions.length} 条</small>
            </div>
            <div class="session-search">
              <input class="text-input" data-bind="sessionKeyword" placeholder="搜索会话或状态" value="${escapeHtml(state.filters.sessionKeyword)}">
            </div>
            <div class="session-items">
              ${sessionCards}
            </div>
          </aside>

          <section class="panel feishu-chat-pane">
            <div class="feishu-chat-header">
              <div class="chat-title">
                <div class="assistant-mark">助</div>
                <div>
                  <strong>${escapeHtml(session.subject)}</strong>
                  <small>${escapeHtml(session.sessionId)} · ${escapeHtml(session.status)}</small>
                </div>
              </div>
              <div class="chat-actions">
                <span class="status-tag ${statusClass(session.status)}">${escapeHtml(session.status)}</span>
                ${ticket ? `<button class="ghost-button" data-action="open-ticket" data-ticket-id="${ticket.ticketId}">打开工单</button>` : ''}
                <button class="ghost-button" data-action="handoff">转人工</button>
                ${hasPermission('ticket:create') ? '<button class="primary-button" data-action="create-ticket">创建工单</button>' : ''}
              </div>
            </div>
            <div class="chat-subheader">
              <span class="chat-subject">${escapeHtml(session.summary)}</span>
              <span class="chat-hint">消息按照时间顺序展示，输入框固定在底部。</span>
            </div>
            <div class="message-list feishu-thread">
              ${chatMessages}
              ${!chatMessages ? '<div class="empty-state"><div class="empty-icon">聊</div><strong>开始聊天</strong><p>先输入问题，系统会按照对话上下文继续回复</p></div>' : ''}
            </div>
            <div class="quick-actions feishu-quick-actions">
              ${quickToolbar}
            </div>
            <form class="feishu-composer" id="chat-form">
              <div class="composer-toolbar">
                <button type="button" class="icon-button" data-action="composer-tool" data-tool="attach" title="附件">＋</button>
                <button type="button" class="icon-button" data-action="composer-tool" data-tool="file" title="文件">📎</button>
                <button type="button" class="icon-button" data-action="composer-tool" data-tool="emoji" title="表情">☺</button>
                <button type="button" class="icon-button" data-action="composer-tool" data-tool="screenshot" title="截图">▢</button>
                <span class="composer-hint">Enter 发送 · Shift+Enter 换行</span>
              </div>
              <textarea class="textarea-input" name="message" placeholder="输入消息，支持继续追问、补充截图或描述工单上下文">${escapeHtml(state.draft.message)}</textarea>
              <div class="composer-footer">
                <div class="composer-status">当前会话：${escapeHtml(session.sessionId)} · ${escapeHtml(session.status)}</div>
                <div class="composer-actions">
                  <button class="ghost-button" type="button" data-action="handoff">转人工</button>
                  <button class="primary-button" type="submit">发送</button>
                </div>
              </div>
            </form>
          </section>

          <aside class="panel feishu-info-pane">
            <div class="panel-header">
              <h3>会话信息</h3>
              <small>${ticket ? ticket.ticketNo : '未建单'}</small>
            </div>
            <div class="info-stack">
              <div class="info-card">
                <div class="info-label">当前会话</div>
                <div class="info-title">${escapeHtml(session.subject)}</div>
                <div class="info-meta">${escapeHtml(session.status)} · ${escapeHtml(session.ticketId || '尚未生成工单')}</div>
                <div class="info-lines">
                  <div><span>会话编号</span><strong>${escapeHtml(session.sessionId)}</strong></div>
                  <div><span>最后消息</span><strong>${escapeHtml(formatDateTime(session.lastMessageAt || session.createdAt))}</strong></div>
                  <div><span>消息数量</span><strong>${escapeHtml(String(session.messages.length))}</strong></div>
                </div>
              </div>

              ${rightTicket}

              <div class="info-card">
                <div class="info-label">常用操作</div>
                <div class="summary-actions">
                  <button class="ghost-button" data-action="new-session">新建会话</button>
                  <button class="ghost-button" data-action="handoff">转人工</button>
                  ${hasPermission('ticket:create') ? '<button class="ghost-button" data-action="create-ticket">创建工单</button>' : ''}
                </div>
                <div class="inline-tip">这是一个普通聊天界面，左边看会话，中间聊事情，右边看关联信息。</div>
              </div>

              ${rightRating}
            </div>
          </aside>
        </section>
      </div>
    `;
    shell(page);
  }

  function supportSlaMarkup(ticket) {
    const info = slaInfo(ticket);
    const terminal = ['PENDING_USER_CONFIRM', 'RESOLVED', 'CLOSED'].includes(ticket.status);
    const risk = !terminal && slaRisk(ticket);
    const stateText = terminal ? '处理已结束' : ticket.isSuspended ? '计时暂停' : info.timedOut ? '已超时' : risk ? '即将超时' : '处理中';
    return `
      <div class="sla-indicator ${risk || info.timedOut ? 'risk' : ''} ${ticket.isSuspended ? 'suspended' : ''}" data-sla-ticket-id="${escapeHtml(ticket.ticketId)}">
        <div class="sla-topline">
          <span>${escapeHtml(stateText)}</span>
          <strong>${escapeHtml(String(ticket.slaHours))} 小时</strong>
        </div>
        <div class="sla-progress-track"><span class="sla-progress-fill ${info.timedOut ? 'timed-out' : ''} ${ticket.isSuspended ? 'suspended' : ''} ${terminal ? 'complete' : ''}" style="width:${terminal ? 100 : info.percent}%;"></span></div>
        <div class="sla-bottomline">
          <span>已耗时 <b data-sla-part="elapsed">${escapeHtml(formatDuration(info.elapsedMs))}</b></span>
          <span>${terminal ? '累计用时' : info.timedOut ? '超时' : '剩余'} <b data-sla-part="remaining">${escapeHtml(terminal ? formatDuration(info.elapsedMs) : info.timedOut ? formatDuration(info.overMs) : formatDuration(info.remainingMs))}</b></span>
        </div>
      </div>
    `;
  }

  function supportBoardRow(ticket, compact = false) {
    const info = slaInfo(ticket);
    const risk = slaRisk(ticket) || info.timedOut;
    return `
      <button class="support-board-row" data-action="open-ticket" data-ticket-id="${escapeHtml(ticket.ticketId)}">
        <span class="support-board-row-top">
          <strong>${escapeHtml(ticket.ticketNo)}</strong>
          <time>${escapeHtml(formatDateTime(ticket.updatedAt || ticket.createdAt))}</time>
        </span>
        <span class="support-board-row-title">${escapeHtml(ticket.title)}</span>
        <span class="support-board-row-meta">${escapeHtml(ticket.requester.displayName)}${compact ? '' : ` · ${escapeHtml(businessLineName(ticket.businessLineCode))}`}</span>
        <span class="support-board-row-tags">
          <span class="status-tag ${statusClass(ticket.status)}">${escapeHtml(statusLabel(ticket.status))}</span>
          ${risk ? '<span class="risk-tag">时效风险</span>' : ''}
          ${ticket.isSuspended ? '<span class="risk-tag suspended">已挂起</span>' : ''}
        </span>
        ${!compact && ['IN_PROGRESS', 'REOPENED'].includes(ticket.status) ? supportSlaMarkup(ticket) : ''}
      </button>
    `;
  }

  function supportBoardEmpty(text) {
    return `<div class="support-board-empty"><span>空</span><strong>暂无数据</strong><p>${escapeHtml(text)}</p></div>`;
  }

  function serviceDeskTicketRow(ticket) {
    return `
      <button class="service-desk-ticket" data-action="open-ticket" data-ticket-id="${escapeHtml(ticket.ticketId)}">
        <span class="service-desk-ticket-no">${escapeHtml(ticket.ticketNo)}</span>
        <span class="service-desk-ticket-main">
          <strong>${escapeHtml(ticket.title)}</strong>
          <small>${escapeHtml(ticket.requester.displayName)} · ${escapeHtml(ticket.requester.departmentName)}</small>
        </span>
        <span class="status-tag ${statusClass(ticket.status)}">${escapeHtml(statusLabel(ticket.status))}</span>
        <time>${escapeHtml(formatDateTime(ticket.updatedAt || ticket.createdAt))}</time>
      </button>
    `;
  }

  function serviceDeskEmpty(text) {
    return `<div class="service-desk-empty"><span>空</span><p>${escapeHtml(text)}</p></div>`;
  }

  function renderSupportWorkbenchHomePage() {
    const drafts = supportDraftTickets();
    const mine = supportMineTickets();
    const acceptedTickets = mine.filter((ticket) => ['IN_PROGRESS', 'REOPENED'].includes(ticket.status));
    const ratingTickets = mine.filter((ticket) => ticket.status === 'PENDING_USER_CONFIRM');

    const page = `
      <div class="page support-workbench-page service-desk-page">
        <div class="support-board-nav">
          <div class="support-board-nav-left">
            <span class="board-nav-mark">工</span>
            <strong>工单服务台</strong>
            <span class="muted">${escapeHtml(String(supportTickets().length))} 条可查记录</span>
          </div>
          <div class="support-board-nav-right">
            <span class="status-chip ok">${escapeHtml(String(supportDraftTickets().length))} 条草稿</span>
            <span class="status-chip info">${escapeHtml(String(supportMineTickets().length))} 条待我处理</span>
          </div>
        </div>

        <section class="service-desk-split">
          <article class="panel service-desk-panel">
            <header class="service-desk-panel-header">
              <div><h2>草稿箱</h2><p>用户提交的未受理新工单</p></div>
              <span class="service-desk-count">${escapeHtml(String(drafts.length))}</span>
            </header>
            <div class="service-desk-scroll">
              ${drafts.map(serviceDeskTicketRow).join('') || serviceDeskEmpty('当前没有待受理草稿')}
            </div>
          </article>

          <article class="panel service-desk-panel">
            <header class="service-desk-panel-header">
              <div><h2>等待我处理的服务请求</h2><p>全部已受理未解决工单</p></div>
              <div class="service-desk-panel-header-right">
                <button class="service-desk-tab ${state.filters.serviceDeskTab === 'pending' ? 'active' : ''}" data-action="service-desk-tab" data-tab="pending">已受理 <span>${escapeHtml(String(acceptedTickets.length))}</span></button>
                <button class="service-desk-tab ${state.filters.serviceDeskTab === 'rating' ? 'active' : ''}" data-action="service-desk-tab" data-tab="rating">待评价 <span>${escapeHtml(String(ratingTickets.length))}</span></button>
              </div>
            </header>
            <div class="service-desk-right-body service-desk-right-tabs">
              <div class="service-desk-tab-panel ${state.filters.serviceDeskTab === 'rating' ? 'hidden' : ''}">
                <div class="service-desk-module">
                  <div class="service-desk-module-header">
                    <div><h3>已受理未解决</h3><small>当前处理中或已重开</small></div>
                    <span>${escapeHtml(String(acceptedTickets.length))}</span>
                  </div>
                  <div class="service-desk-scroll">
                    ${acceptedTickets.map(serviceDeskTicketRow).join('') || serviceDeskEmpty('暂无处理中工单')}
                  </div>
                </div>
              </div>

              <div class="service-desk-tab-panel ${state.filters.serviceDeskTab === 'rating' ? '' : 'hidden'}">
                <div class="service-desk-module service-desk-module-sub">
                  <div class="service-desk-module-header">
                    <div><h3>待评价</h3><small>等待用户确认或评价</small></div>
                    <span>${escapeHtml(String(ratingTickets.length))}</span>
                  </div>
                  <div class="service-desk-scroll">
                    ${ratingTickets.map(serviceDeskTicketRow).join('') || serviceDeskEmpty('暂无待评价工单')}
                  </div>
                </div>
              </div>
            </div>
          </article>
        </section>
      </div>
    `;
    shell(page);
  }

  function renderSupportHomePage() {
    if (isSupportRole()) {
      renderSupportWorkbenchHomePage();
      return;
    }
    const tickets = visibleTickets();
    const pendingCount = DATA.tickets.filter((ticket) => ['PENDING_ACCEPTANCE', 'REOPENED'].includes(ticket.status)).length;
    const progressCount = DATA.tickets.filter((ticket) => ticket.status === 'IN_PROGRESS').length;
    const confirmCount = DATA.tickets.filter((ticket) => ticket.status === 'PENDING_USER_CONFIRM').length;
    const closedCount = DATA.tickets.filter((ticket) => ['RESOLVED', 'CLOSED'].includes(ticket.status)).length;
    const selected = DATA.tickets.find((ticket) => ticket.ticketId === state.activeTicketId) || tickets[0] || DATA.tickets[0];

    const rows = tickets.slice(0, 6).map((ticket) => `
      <tr>
        <td class="compact">${escapeHtml(ticket.ticketNo)}</td>
        <td>
          <button class="link-button" data-action="support-select-ticket" data-ticket-id="${ticket.ticketId}">${escapeHtml(ticket.title)}</button>
          <div class="session-preview">${escapeHtml(ticket.description)}</div>
        </td>
        <td class="compact">${escapeHtml(ticket.requester.displayName)}</td>
        <td class="compact"><span class="status-tag ${statusClass(ticket.status)}">${escapeHtml(ticket.status)}</span></td>
        <td class="compact">${escapeHtml(ticket.updatedAt ? formatDateTime(ticket.updatedAt) : '-')}</td>
      </tr>
    `).join('');

    const queue = DATA.tickets.filter((ticket) => ['PENDING_ACCEPTANCE', 'REOPENED'].includes(ticket.status)).slice(0, 4).map((ticket) => `
      <div class="queue-item">
        <div>
          <div class="queue-title"><span>${escapeHtml(ticket.title)}</span></div>
          <div class="queue-meta">${escapeHtml(ticket.ticketNo)} · ${escapeHtml(ticket.requester.displayName)} · ${escapeHtml(ticket.businessLineCode)}</div>
        </div>
        <div class="queue-actions">
          <span class="status-tag ${statusClass(ticket.status)}">${escapeHtml(ticket.status)}</span>
          <time>${escapeHtml(formatDateTime(ticket.updatedAt || ticket.createdAt))}</time>
        </div>
      </div>
    `).join('');

    const summaryButtons = [];
    if (selected) {
      summaryButtons.push(`<button class="primary-button" data-action="open-ticket" data-ticket-id="${selected.ticketId}">打开详情</button>`);
      if (selected.status === 'PENDING_ACCEPTANCE' || selected.status === 'REOPENED') {
        summaryButtons.push(`<button class="ghost-button" data-action="accept-ticket" data-ticket-id="${selected.ticketId}" ${hasPermission('ticket:accept') ? '' : 'disabled'}>受理</button>`);
      }
      if (selected.status === 'IN_PROGRESS') {
        summaryButtons.push(`<button class="ghost-button" data-action="resolve-ticket" data-ticket-id="${selected.ticketId}" ${hasPermission('ticket:resolve') ? '' : 'disabled'}>提交解决</button>`);
      }
      if (selected.status === 'RESOLVED') {
        summaryButtons.push(`<button class="danger-button" data-action="close-ticket" data-ticket-id="${selected.ticketId}" ${hasPermission('ticket:close') ? '' : 'disabled'}>关闭工单</button>`);
      }
    }

    const page = `
      <div class="page">
        <div class="page-heading">
          <div>
            <h1>客服工作台</h1>
            <p>首页用于查看队列概况、最近工单和当前选中工单的概要处理入口。</p>
          </div>
          <div class="heading-actions">
            <span class="contract-tag">运营总览</span>
            <button class="toolbar-button" data-action="nav" data-route="TICKET">进入完整工作台</button>
          </div>
        </div>

        <section class="grid-4">
          ${metricCard('待受理', String(pendingCount), '等待客服接入的工单')}
          ${metricCard('处理中', String(progressCount), '已经受理并处理中的工单')}
          ${metricCard('待确认', String(confirmCount), '等待用户确认的工单')}
          ${metricCard('已闭环', String(closedCount), '已解决或已关闭工单')}
        </section>

        <section class="grid-3" style="margin-top:14px;">
          <aside class="panel queue-panel">
            <div class="panel-header">
              <h3>待受理队列</h3>
              <small>${pendingCount} 条</small>
            </div>
            <div class="queue-list">
              ${queue || '<div class="empty-state"><div class="empty-icon">队</div><strong>暂无待受理工单</strong><p>当前没有等待受理的请求</p></div>'}
            </div>
          </aside>

          <section class="panel">
            <div class="panel-header">
              <h3>最近工单</h3>
              <small>共 ${tickets.length} 条</small>
            </div>
            <div class="support-toolbar">
              <div class="filter-left">
                <div class="segmented">
                  <button class="segmented-button ${state.filters.ticketView === 'all' ? 'active' : ''}" data-action="ticket-view" data-view="all">全部</button>
                  <button class="segmented-button ${state.filters.ticketView === 'pending' ? 'active' : ''}" data-action="ticket-view" data-view="pending">待受理</button>
                  <button class="segmented-button ${state.filters.ticketView === 'progress' ? 'active' : ''}" data-action="ticket-view" data-view="progress">处理中</button>
                  <button class="segmented-button ${state.filters.ticketView === 'confirm' ? 'active' : ''}" data-action="ticket-view" data-view="confirm">待确认</button>
                  <button class="segmented-button ${state.filters.ticketView === 'closed' ? 'active' : ''}" data-action="ticket-view" data-view="closed">历史</button>
                </div>
              </div>
              <div class="filter-right">
                <input class="text-input" data-bind="ticketKeyword" placeholder="搜索工单号、标题、申请人" value="${escapeHtml(state.filters.ticketKeyword)}" style="width:250px;">
                <button class="ghost-button" data-action="apply-ticket-filter">搜索</button>
              </div>
            </div>
            <div class="table-wrap">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>工单号</th>
                    <th>标题 / 摘要</th>
                    <th>申请人</th>
                    <th>状态</th>
                    <th>更新时间</th>
                  </tr>
                </thead>
                <tbody>
                  ${rows || '<tr><td colspan="5"><div class="empty-state"><div class="empty-icon">空</div><strong>暂无工单</strong><p>请调整筛选条件或切换视图</p></div></td></tr>'}
                </tbody>
              </table>
            </div>
          </section>

          <aside class="panel summary-panel">
            <div class="panel-header">
              <h3>当前选中</h3>
              <small>${selected ? selected.ticketNo : '-'}</small>
            </div>
            <div class="summary-body">
              ${selected ? `
                <div class="summary-block">
                  <div class="summary-label">标题</div>
                  <h3 class="summary-title">${escapeHtml(selected.title)}</h3>
                  <div class="muted" style="margin-top:6px;font-size:12px;line-height:1.5;">${escapeHtml(selected.description)}</div>
                </div>
                <div class="summary-block">
                  <div class="summary-label">状态</div>
                  <span class="status-tag ${statusClass(selected.status)}">${escapeHtml(selected.status)}</span>
                  <div class="muted" style="margin-top:6px;font-size:12px;line-height:1.5;">${escapeHtml(selected.requester.displayName)} · ${escapeHtml(selected.businessLineCode)}</div>
                </div>
                <div class="summary-block">
                  <div class="summary-label">分类</div>
                  <div class="muted" style="font-size:12px;line-height:1.6;">
                    ${escapeHtml(selected.classification.managementUnitId || '-')}<br>
                    ${escapeHtml(selected.classification.symptomId || '-')}<br>
                    ${escapeHtml(selected.classification.reasonId || '-')}<br>
                    ${escapeHtml(selected.classification.solutionMethodId || '-')}
                  </div>
                </div>
                <div class="summary-actions">${summaryButtons.join('')}</div>
              ` : '<div class="empty-state"><div class="empty-icon">单</div><strong>暂无选中工单</strong><p>从列表中选择一条工单查看详情</p></div>'}
            </div>
          </aside>
        </section>
      </div>
    `;
    shell(page);
  }

  function renderSupervisorHomePage() {
    const tickets = tenantTickets();
    const summaries = supervisorSummary(state.filters.supervisorPeriod);
    const currentSummary = summarizeTickets(tickets, state.filters.supervisorPeriod, 'all');
    const latestSummary = summaries[0];
    const topPeriods = summaries.slice(0, 6);
    const maxTotal = Math.max(1, ...topPeriods.map((item) => item.total));
    const statusCounts = [
      { label: '待受理 / 重开', value: currentSummary.pending, className: 'status-PENDING_ACCEPTANCE' },
      { label: '处理中', value: currentSummary.progress, className: 'status-IN_PROGRESS' },
      { label: '待确认', value: currentSummary.confirm, className: 'status-PENDING_USER_CONFIRM' },
      { label: '已解决 / 已关闭', value: currentSummary.closedCount, className: 'status-RESOLVED' }
    ];

    const periodRows = topPeriods.map((item) => `
      <tr>
        <td><strong>${escapeHtml(item.label)}</strong></td>
        <td class="compact"><strong>${escapeHtml(String(item.total))}</strong></td>
        <td class="compact">${escapeHtml(String(item.pending))}</td>
        <td class="compact">${escapeHtml(String(item.progress))}</td>
        <td class="compact">${escapeHtml(String(item.confirm))}</td>
        <td class="compact">${escapeHtml(String(item.closedCount))}</td>
        <td class="compact"><span class="status-chip ${item.closureRate >= 70 ? 'ok' : item.closureRate >= 40 ? 'warn' : 'bad'}">${escapeHtml(String(item.closureRate))}%</span></td>
      </tr>
    `).join('');

    const statusRows = statusCounts.map((item) => `
      <div class="supervisor-status-row">
        <div class="supervisor-status-label"><span class="status-dot ${item.className}"></span><span>${escapeHtml(item.label)}</span></div>
        <strong>${escapeHtml(String(item.value))}</strong>
      </div>
    `).join('');

    const trendBars = topPeriods.slice().reverse().map((item) => `
      <div class="trend-row">
        <div class="trend-label" title="${escapeHtml(item.label)}">${escapeHtml(item.label)}</div>
        <div class="trend-track"><span style="width:${Math.max(8, Math.round((item.total / maxTotal) * 100))}%;"></span></div>
        <strong>${escapeHtml(String(item.total))}</strong>
      </div>
    `).join('');

    const periodLabel = state.filters.supervisorPeriod === 'year' ? '年度' : state.filters.supervisorPeriod === 'week' ? '周度' : '月度';
    const page = `
      <div class="page supervisor-page">
        <div class="page-heading">
          <div>
            <h1>主管工单总览</h1>
            <p>查看本租户全部工单的处理分布、趋势和闭环情况，点击工单号可打开只读详情。</p>
          </div>
          <div class="heading-actions">
            <span class="contract-tag">只读审计视图</span>
            <button class="primary-button" data-action="nav" data-route="TICKET">查看全部工单</button>
          </div>
        </div>

        <div class="supervisor-period-tabs">
          ${['year', 'month', 'week'].map((period) => `
            <button class="segmented-button ${state.filters.supervisorPeriod === period ? 'active' : ''}" data-action="supervisor-period" data-period="${period}">${period === 'year' ? '按年' : period === 'month' ? '按月' : '按周'}</button>
          `).join('')}
          <span class="muted">按工单创建时间统计 · 当前 ${escapeHtml(periodLabel)}视图</span>
        </div>

        <section class="grid-4">
          ${metricCard('全部工单', String(currentSummary.total), '当前租户可查看的工单总量')}
          ${metricCard('处理中', String(currentSummary.progress), '当前仍在处理中的工单')}
          ${metricCard('高优先级', String(currentSummary.highPriority), '需要重点关注的高优先级工单')}
          ${metricCard('闭环率', `${currentSummary.closureRate}%`, '已解决或已关闭 / 全部工单')}
        </section>

        <section class="supervisor-dashboard-grid">
          <div class="panel supervisor-trend-panel">
            <div class="panel-header">
              <div><h3>${escapeHtml(periodLabel)}工单量</h3><small>最近 ${topPeriods.length || 0} 个统计周期</small></div>
              ${latestSummary ? `<span class="status-chip info">最新 ${escapeHtml(latestSummary.label)} · ${escapeHtml(String(latestSummary.total))} 条</span>` : ''}
            </div>
            <div class="panel-body supervisor-trend-body">
              ${trendBars || '<div class="empty-state"><div class="empty-icon">统</div><strong>暂无汇总数据</strong><p>当前租户还没有可统计的工单</p></div>'}
            </div>
          </div>

          <div class="panel">
            <div class="panel-header"><h3>当前状态分布</h3><small>全部工单</small></div>
            <div class="panel-body supervisor-status-list">${statusRows}</div>
          </div>
        </section>

        <section class="panel" style="margin-top:14px;">
          <div class="panel-header">
            <div><h3>${escapeHtml(periodLabel)}汇总</h3><small>汇总数据随筛选粒度切换，明细请进入全部工单查看</small></div>
            <span class="muted">共 ${escapeHtml(String(summaries.length))} 个周期</span>
          </div>
          <div class="table-wrap">
            <table class="data-table supervisor-summary-table">
              <thead><tr><th>统计周期</th><th>工单总量</th><th>待受理 / 重开</th><th>处理中</th><th>待确认</th><th>已闭环</th><th>闭环率</th></tr></thead>
              <tbody>${periodRows || '<tr><td colspan="7"><div class="empty-state"><div class="empty-icon">空</div><strong>暂无汇总数据</strong><p>当前租户还没有可统计的工单</p></div></td></tr>'}</tbody>
            </table>
          </div>
        </section>
      </div>
    `;
    shell(page);
  }

  function renderSupervisorTicketsPage() {
    const tickets = visibleSupervisorTickets();
    const allTickets = tenantTickets();
    const selected = tickets.find((item) => item.ticketId === state.activeTicketId) || tickets[0] || allTickets[0];
    if (selected) state.activeTicketId = selected.ticketId;

    const statusOptions = ['all', 'PENDING_ACCEPTANCE', 'IN_PROGRESS', 'PENDING_USER_CONFIRM', 'RESOLVED', 'CLOSED', 'REOPENED'];
    const rows = tickets.map((ticket) => `
      <tr>
        <td class="compact"><button class="link-button" data-action="open-ticket" data-ticket-id="${ticket.ticketId}">${escapeHtml(ticket.ticketNo)}</button></td>
        <td><button class="link-button supervisor-title-link" data-action="open-ticket" data-ticket-id="${ticket.ticketId}">${escapeHtml(ticket.title)}</button><div class="session-preview">${escapeHtml(ticket.description)}</div></td>
        <td class="compact">${escapeHtml(ticket.requester.displayName)}<div class="session-preview">${escapeHtml(ticket.requester.departmentName)}</div></td>
        <td class="compact"><span class="status-tag ${statusClass(ticket.status)}">${escapeHtml(statusLabel(ticket.status))}</span></td>
        <td class="compact"><span class="priority-tag ${priorityClass(ticket.priority)}">${escapeHtml(priorityLabel(ticket.priority))}</span></td>
        <td class="compact">${escapeHtml(ticket.assignee ? ticket.assignee.displayName : '待分配')}</td>
        <td class="compact">${escapeHtml(formatDateTime(ticket.createdAt))}</td>
        <td class="compact">${escapeHtml(formatDateTime(ticket.updatedAt))}</td>
        <td class="compact"><button class="small-button" data-action="open-ticket" data-ticket-id="${ticket.ticketId}">查看详情</button></td>
      </tr>
    `).join('');

    const selectedSummary = selected ? `
      <div class="summary-block"><div class="summary-label">工单</div><h3 class="summary-title">${escapeHtml(selected.ticketNo)} · ${escapeHtml(selected.title)}</h3></div>
      <div class="summary-block"><div class="summary-label">当前状态</div><span class="status-tag ${statusClass(selected.status)}">${escapeHtml(statusLabel(selected.status))}</span><span class="priority-tag ${priorityClass(selected.priority)}" style="margin-left:6px;">${escapeHtml(priorityLabel(selected.priority))}优先级</span></div>
      <div class="summary-block"><div class="summary-label">请求人</div><div class="muted" style="font-size:12px;line-height:1.6;">${escapeHtml(selected.requester.displayName)} · ${escapeHtml(selected.requester.departmentName)}<br>${escapeHtml(selected.businessLineCode)} · ${escapeHtml(selected.assignee ? selected.assignee.displayName : '待分配')}</div></div>
      <div class="summary-block"><div class="summary-label">解决结果</div><div class="muted" style="font-size:12px;line-height:1.6;">${escapeHtml(selected.resolution ? selected.resolution.summary : '暂无解决结果')}</div></div>
      <div class="summary-actions"><button class="primary-button" data-action="open-ticket" data-ticket-id="${selected.ticketId}">打开只读详情</button></div>
    ` : '<div class="empty-state"><div class="empty-icon">单</div><strong>暂无选中工单</strong><p>请选择一条工单查看详细信息</p></div>';

    const page = `
      <div class="page supervisor-page">
        <div class="page-heading">
          <div><h1>全部工单</h1><p>主管可查看本租户全部工单、处理进度和审计信息，当前页面不提供业务写操作。</p></div>
          <div class="heading-actions"><span class="contract-tag">${escapeHtml(String(tickets.length))} / ${escapeHtml(String(allTickets.length))} 条</span><button class="toolbar-button" data-action="nav" data-route="HOME">返回主管总览</button></div>
        </div>

        <section class="grid-4">
          ${metricCard('筛选结果', String(tickets.length), '当前条件下的工单数量')}
          ${metricCard('待受理 / 重开', String(allTickets.filter((ticket) => ['PENDING_ACCEPTANCE', 'REOPENED'].includes(ticket.status)).length), '需要队列关注')}
          ${metricCard('待确认', String(allTickets.filter((ticket) => ticket.status === 'PENDING_USER_CONFIRM').length), '等待请求人确认')}
          ${metricCard('已闭环', String(allTickets.filter((ticket) => ['RESOLVED', 'CLOSED'].includes(ticket.status)).length), '已解决或已关闭')}
        </section>

        <section class="supervisor-ticket-layout" style="margin-top:14px;">
          <div class="panel supervisor-ticket-table-panel">
            <div class="support-toolbar supervisor-filter-toolbar">
              <div class="filter-left">
                <select class="select-input" data-bind="ticketStatus" style="width:150px;">
                  ${statusOptions.map((value) => `<option value="${value}" ${state.filters.ticketStatus === value ? 'selected' : ''}>${value === 'all' ? '全部状态' : escapeHtml(statusLabel(value))}</option>`).join('')}
                </select>
                <select class="select-input" data-bind="ticketPriority" style="width:120px;">
                  ${['all', 'HIGH', 'MEDIUM', 'LOW'].map((value) => `<option value="${value}" ${state.filters.ticketPriority === value ? 'selected' : ''}>${value === 'all' ? '全部优先级' : escapeHtml(priorityLabel(value))}</option>`).join('')}
                </select>
                <input class="text-input" data-bind="ticketKeyword" placeholder="搜索工单号、标题、申请人、处理人" value="${escapeHtml(state.filters.ticketKeyword)}" style="min-width:250px;width:320px;">
                <button class="ghost-button" data-action="apply-ticket-filter">筛选</button>
              </div>
              <div class="filter-right"><span class="muted">最近更新优先</span></div>
            </div>
            <div class="table-wrap">
              <table class="data-table supervisor-ticket-table">
                <thead><tr><th>工单号</th><th>标题 / 描述</th><th>请求人</th><th>状态</th><th>优先级</th><th>处理人</th><th>创建时间</th><th>更新时间</th><th>操作</th></tr></thead>
                <tbody>${rows || '<tr><td colspan="9"><div class="empty-state"><div class="empty-icon">空</div><strong>没有匹配工单</strong><p>请调整状态、优先级或关键词筛选</p></div></td></tr>'}</tbody>
              </table>
            </div>
          </div>

          <aside class="panel summary-panel supervisor-selected-panel">
            <div class="panel-header"><h3>当前工单</h3><small>${selected ? escapeHtml(selected.ticketNo) : '-'}</small></div>
            <div class="summary-body">${selectedSummary}<div class="inline-tip">主管详情为只读视图，审计事件和状态流转以服务端返回为准。</div></div>
          </aside>
        </section>
      </div>
    `;
    shell(page);
  }

  function renderSupervisorHomePage() {
    const tickets = tenantTickets();
    const summaries = supervisorSummary(state.filters.supervisorPeriod);
    const currentSummary = summarizeTickets(tickets, state.filters.supervisorPeriod, 'all');
    const latestSummary = summaries[0];
    const topPeriods = summaries.slice(0, 6);
    const maxTotal = Math.max(1, ...topPeriods.map((item) => item.total));
    const statusCounts = [
      { label: '待受理 / 重开', value: currentSummary.pending, className: 'status-PENDING_ACCEPTANCE' },
      { label: '处理中', value: currentSummary.progress, className: 'status-IN_PROGRESS' },
      { label: '待确认', value: currentSummary.confirm, className: 'status-PENDING_USER_CONFIRM' },
      { label: '已解决 / 已关闭', value: currentSummary.closedCount, className: 'status-RESOLVED' }
    ];

    const periodRows = topPeriods.map((item) => `
      <tr>
        <td><strong>${escapeHtml(item.label)}</strong></td>
        <td class="compact"><strong>${escapeHtml(String(item.total))}</strong></td>
        <td class="compact">${escapeHtml(String(item.pending))}</td>
        <td class="compact">${escapeHtml(String(item.progress))}</td>
        <td class="compact">${escapeHtml(String(item.confirm))}</td>
        <td class="compact">${escapeHtml(String(item.closedCount))}</td>
        <td class="compact"><span class="status-chip ${item.closureRate >= 70 ? 'ok' : item.closureRate >= 40 ? 'warn' : 'bad'}">${escapeHtml(String(item.closureRate))}%</span></td>
      </tr>
    `).join('');

    const statusRows = statusCounts.map((item) => `
      <div class="supervisor-status-row">
        <div class="supervisor-status-label"><span class="status-dot ${item.className}"></span><span>${escapeHtml(item.label)}</span></div>
        <strong>${escapeHtml(String(item.value))}</strong>
      </div>
    `).join('');

    const trendBars = topPeriods.slice().reverse().map((item) => `
      <div class="trend-row">
        <div class="trend-label" title="${escapeHtml(item.label)}">${escapeHtml(item.label)}</div>
        <div class="trend-track"><span style="width:${Math.max(8, Math.round((item.total / maxTotal) * 100))}%;"></span></div>
        <strong>${escapeHtml(String(item.total))}</strong>
      </div>
    `).join('');

    const periodLabel = state.filters.supervisorPeriod === 'year' ? '年度' : state.filters.supervisorPeriod === 'week' ? '周度' : '月度';
    const page = `
      <div class="page supervisor-page">
        <div class="page-heading">
          <div>
            <h1>主管工单总览</h1>
            <p>查看本租户全部工单的处理分布、趋势和闭环情况，点击工单号可打开只读详情。</p>
          </div>
          <div class="heading-actions">
            <span class="contract-tag">只读审计视图</span>
            <button class="primary-button" data-action="nav" data-route="TICKET">查看全部工单</button>
          </div>
        </div>

        <div class="supervisor-period-tabs">
          ${['year', 'month', 'week'].map((period) => `
            <button class="segmented-button ${state.filters.supervisorPeriod === period ? 'active' : ''}" data-action="supervisor-period" data-period="${period}">${period === 'year' ? '按年' : period === 'month' ? '按月' : '按周'}</button>
          `).join('')}
          <span class="muted">按工单创建时间统计 · 当前 ${escapeHtml(periodLabel)}视图</span>
        </div>

        <section class="grid-4">
          ${metricCard('全部工单', String(currentSummary.total), '当前租户可查看的工单总量')}
          ${metricCard('处理中', String(currentSummary.progress), '当前仍在处理中的工单')}
          ${metricCard('高优先级', String(currentSummary.highPriority), '需要重点关注的高优先级工单')}
          ${metricCard('闭环率', `${currentSummary.closureRate}%`, '已解决或已关闭 / 全部工单')}
        </section>

        <section class="supervisor-dashboard-grid">
          <div class="panel supervisor-trend-panel">
            <div class="panel-header">
              <div><h3>${escapeHtml(periodLabel)}工单量</h3><small>最近 ${topPeriods.length || 0} 个统计周期</small></div>
              ${latestSummary ? `<span class="status-chip info">最新 ${escapeHtml(latestSummary.label)} · ${escapeHtml(String(latestSummary.total))} 条</span>` : ''}
            </div>
            <div class="panel-body supervisor-trend-body">
              ${trendBars || '<div class="empty-state"><div class="empty-icon">统</div><strong>暂无汇总数据</strong><p>当前租户还没有可统计的工单</p></div>'}
            </div>
          </div>

          <div class="panel">
            <div class="panel-header"><h3>当前状态分布</h3><small>全部工单</small></div>
            <div class="panel-body supervisor-status-list">${statusRows}</div>
          </div>
        </section>

        <section class="panel" style="margin-top:14px;">
          <div class="panel-header">
            <div><h3>${escapeHtml(periodLabel)}汇总</h3><small>汇总数据随筛选粒度切换，明细请进入全部工单查看</small></div>
            <span class="muted">共 ${escapeHtml(String(summaries.length))} 个周期</span>
          </div>
          <div class="table-wrap">
            <table class="data-table supervisor-summary-table">
              <thead><tr><th>统计周期</th><th>工单总量</th><th>待受理 / 重开</th><th>处理中</th><th>待确认</th><th>已闭环</th><th>闭环率</th></tr></thead>
              <tbody>${periodRows || '<tr><td colspan="7"><div class="empty-state"><div class="empty-icon">空</div><strong>暂无汇总数据</strong><p>当前租户还没有可统计的工单</p></div></td></tr>'}</tbody>
            </table>
          </div>
        </section>
      </div>
    `;
    shell(page);
  }

  function renderSupervisorTicketsPage() {
    const tickets = visibleSupervisorTickets();
    const allTickets = tenantTickets();
    const selected = tickets.find((item) => item.ticketId === state.activeTicketId) || tickets[0] || allTickets[0];
    if (selected) state.activeTicketId = selected.ticketId;

    const statusOptions = ['all', 'PENDING_ACCEPTANCE', 'IN_PROGRESS', 'PENDING_USER_CONFIRM', 'RESOLVED', 'CLOSED', 'REOPENED'];
    const rows = tickets.map((ticket) => `
      <tr>
        <td class="compact"><button class="link-button" data-action="open-ticket" data-ticket-id="${ticket.ticketId}">${escapeHtml(ticket.ticketNo)}</button></td>
        <td><button class="link-button supervisor-title-link" data-action="open-ticket" data-ticket-id="${ticket.ticketId}">${escapeHtml(ticket.title)}</button><div class="session-preview">${escapeHtml(ticket.description)}</div></td>
        <td class="compact">${escapeHtml(ticket.requester.displayName)}<div class="session-preview">${escapeHtml(ticket.requester.departmentName)}</div></td>
        <td class="compact"><span class="status-tag ${statusClass(ticket.status)}">${escapeHtml(statusLabel(ticket.status))}</span></td>
        <td class="compact"><span class="priority-tag ${priorityClass(ticket.priority)}">${escapeHtml(priorityLabel(ticket.priority))}</span></td>
        <td class="compact">${escapeHtml(ticket.assignee ? ticket.assignee.displayName : '待分配')}</td>
        <td class="compact">${escapeHtml(formatDateTime(ticket.createdAt))}</td>
        <td class="compact">${escapeHtml(formatDateTime(ticket.updatedAt))}</td>
        <td class="compact"><button class="small-button" data-action="open-ticket" data-ticket-id="${ticket.ticketId}">查看详情</button></td>
      </tr>
    `).join('');

    const selectedSummary = selected ? `
      <div class="summary-block"><div class="summary-label">工单</div><h3 class="summary-title">${escapeHtml(selected.ticketNo)} · ${escapeHtml(selected.title)}</h3></div>
      <div class="summary-block"><div class="summary-label">当前状态</div><span class="status-tag ${statusClass(selected.status)}">${escapeHtml(statusLabel(selected.status))}</span><span class="priority-tag ${priorityClass(selected.priority)}" style="margin-left:6px;">${escapeHtml(priorityLabel(selected.priority))}优先级</span></div>
      <div class="summary-block"><div class="summary-label">请求人</div><div class="muted" style="font-size:12px;line-height:1.6;">${escapeHtml(selected.requester.displayName)} · ${escapeHtml(selected.requester.departmentName)}<br>${escapeHtml(selected.businessLineCode)} · ${escapeHtml(selected.assignee ? selected.assignee.displayName : '待分配')}</div></div>
      <div class="summary-block"><div class="summary-label">解决结果</div><div class="muted" style="font-size:12px;line-height:1.6;">${escapeHtml(selected.resolution ? selected.resolution.summary : '暂无解决结果')}</div></div>
      <div class="summary-actions"><button class="primary-button" data-action="open-ticket" data-ticket-id="${selected.ticketId}">打开只读详情</button></div>
    ` : '<div class="empty-state"><div class="empty-icon">单</div><strong>暂无选中工单</strong><p>请选择一条工单查看详细信息</p></div>';

    const page = `
      <div class="page supervisor-page">
        <div class="page-heading">
          <div><h1>全部工单</h1><p>主管可查看本租户全部工单、处理进度和审计信息，当前页面不提供业务写操作。</p></div>
          <div class="heading-actions"><span class="contract-tag">${escapeHtml(String(tickets.length))} / ${escapeHtml(String(allTickets.length))} 条</span><button class="toolbar-button" data-action="nav" data-route="HOME">返回主管总览</button></div>
        </div>

        <section class="grid-4">
          ${metricCard('筛选结果', String(tickets.length), '当前条件下的工单数量')}
          ${metricCard('待受理 / 重开', String(allTickets.filter((ticket) => ['PENDING_ACCEPTANCE', 'REOPENED'].includes(ticket.status)).length), '需要队列关注')}
          ${metricCard('待确认', String(allTickets.filter((ticket) => ticket.status === 'PENDING_USER_CONFIRM').length), '等待请求人确认')}
          ${metricCard('已闭环', String(allTickets.filter((ticket) => ['RESOLVED', 'CLOSED'].includes(ticket.status)).length), '已解决或已关闭')}
        </section>

        <section class="supervisor-ticket-layout" style="margin-top:14px;">
          <div class="panel supervisor-ticket-table-panel">
            <div class="support-toolbar supervisor-filter-toolbar">
              <div class="filter-left">
                <select class="select-input" data-bind="ticketStatus" style="width:150px;">
                  ${statusOptions.map((value) => `<option value="${value}" ${state.filters.ticketStatus === value ? 'selected' : ''}>${value === 'all' ? '全部状态' : escapeHtml(statusLabel(value))}</option>`).join('')}
                </select>
                <select class="select-input" data-bind="ticketPriority" style="width:120px;">
                  ${['all', 'HIGH', 'MEDIUM', 'LOW'].map((value) => `<option value="${value}" ${state.filters.ticketPriority === value ? 'selected' : ''}>${value === 'all' ? '全部优先级' : escapeHtml(priorityLabel(value))}</option>`).join('')}
                </select>
                <input class="text-input" data-bind="ticketKeyword" placeholder="搜索工单号、标题、申请人、处理人" value="${escapeHtml(state.filters.ticketKeyword)}" style="min-width:250px;width:320px;">
                <button class="ghost-button" data-action="apply-ticket-filter">筛选</button>
              </div>
              <div class="filter-right"><span class="muted">最近更新优先</span></div>
            </div>
            <div class="table-wrap">
              <table class="data-table supervisor-ticket-table">
                <thead><tr><th>工单号</th><th>标题 / 描述</th><th>请求人</th><th>状态</th><th>优先级</th><th>处理人</th><th>创建时间</th><th>更新时间</th><th>操作</th></tr></thead>
                <tbody>${rows || '<tr><td colspan="9"><div class="empty-state"><div class="empty-icon">空</div><strong>没有匹配工单</strong><p>请调整状态、优先级或关键词筛选</p></div></td></tr>'}</tbody>
              </table>
            </div>
          </div>

          <aside class="panel summary-panel supervisor-selected-panel">
            <div class="panel-header"><h3>当前工单</h3><small>${selected ? escapeHtml(selected.ticketNo) : '-'}</small></div>
            <div class="summary-body">${selectedSummary}<div class="inline-tip">主管详情为只读视图，审计事件和状态流转以服务端返回为准。</div></div>
          </aside>
        </section>
      </div>
    `;
    shell(page);
  }

  function filterDictOptions(dictType, selectedId, filterFn) {
    const items = DATA.dictionaries[dictType].items.filter(filterFn || (() => true));
    return items.map((item) => {
      const selected = item.itemId === selectedId ? 'selected' : '';
      const disabled = item.enabled ? '' : 'disabled';
      return `<option value="${escapeHtml(item.itemId)}" ${selected} ${disabled}>${escapeHtml(item.name)}${item.enabled ? '' : '（停用）'}</option>`;
    }).join('');
  }

  function renderUserTicketsPage() {
    const tickets = visibleTickets();
    const selected = tickets.find((item) => item.ticketId === state.activeTicketId) || tickets[0] || DATA.tickets[0];
    if (selected) {
      state.activeTicketId = selected.ticketId;
    }

    const total = tickets.length;
    const pendingConfirm = tickets.filter((ticket) => ticket.status === 'PENDING_USER_CONFIRM').length;
    const openCount = tickets.filter((ticket) => ['PENDING_ACCEPTANCE', 'IN_PROGRESS', 'REOPENED'].includes(ticket.status)).length;
    const closedCount = tickets.filter((ticket) => ['RESOLVED', 'CLOSED'].includes(ticket.status)).length;

    const rows = tickets.map((ticket) => {
      const actions = [];
      actions.push(`<button class="small-button" data-action="open-ticket" data-ticket-id="${ticket.ticketId}">详情</button>`);
      if (ticket.status === 'PENDING_USER_CONFIRM') {
        actions.push(`<button class="small-button" data-action="confirm-ticket" data-ticket-id="${ticket.ticketId}" ${hasPermission('ticket:confirm') ? '' : 'disabled'}>确认</button>`);
        actions.push(`<button class="small-button" data-action="reopen-ticket" data-ticket-id="${ticket.ticketId}" ${hasPermission('ticket:reopen') ? '' : 'disabled'}>重开</button>`);
      }
      if (['RESOLVED', 'CLOSED'].includes(ticket.status)) {
        actions.push(`<button class="small-button" data-action="submit-rating" data-ticket-id="${ticket.ticketId}" ${hasPermission('ticket:rating') ? '' : 'disabled'}>评价</button>`);
      }
      return `
        <tr>
          <td class="compact">${escapeHtml(ticket.ticketNo)}</td>
          <td>
            <button class="link-button" data-action="open-ticket" data-ticket-id="${ticket.ticketId}">${escapeHtml(ticket.title)}</button>
            <div class="session-preview">${escapeHtml(ticket.description)}</div>
          </td>
          <td class="compact">${escapeHtml(ticket.status)}</td>
          <td class="compact">${escapeHtml(ticket.assignee ? ticket.assignee.displayName : '待分配')}</td>
          <td class="compact">${escapeHtml(formatDateTime(ticket.updatedAt))}</td>
          <td class="compact">${actions.join(' ')}</td>
        </tr>
      `;
    }).join('');

    const page = `
      <div class="page">
        <div class="page-heading">
          <div>
            <h1>我的工单</h1>
            <p>用户可以在这里查看本人工单、确认解决、重开工单和评价结果。</p>
          </div>
          <div class="heading-actions">
            <span class="contract-tag">${escapeHtml(total)} 条</span>
            <button class="toolbar-button" data-action="nav" data-route="HOME">返回助手</button>
          </div>
        </div>

        <section class="grid-4">
          ${metricCard('全部', String(total), '本人名下的工单')}
          ${metricCard('处理中', String(openCount), '待受理或处理中')}
          ${metricCard('待确认', String(pendingConfirm), '等待用户确认')}
          ${metricCard('已完成', String(closedCount), '已解决或已关闭')}
        </section>

        <section class="grid-2" style="margin-top:14px;">
          <div class="panel">
            <div class="panel-header">
              <h3>工单列表</h3>
              <small>仅显示本人工单</small>
            </div>
            <div class="support-toolbar">
              <div class="filter-left">
                <div class="segmented">
                  <button class="segmented-button ${state.filters.ticketView === 'all' ? 'active' : ''}" data-action="ticket-view" data-view="all">全部</button>
                  <button class="segmented-button ${state.filters.ticketView === 'pending' ? 'active' : ''}" data-action="ticket-view" data-view="pending">待受理</button>
                  <button class="segmented-button ${state.filters.ticketView === 'progress' ? 'active' : ''}" data-action="ticket-view" data-view="progress">处理中</button>
                  <button class="segmented-button ${state.filters.ticketView === 'confirm' ? 'active' : ''}" data-action="ticket-view" data-view="confirm">待确认</button>
                  <button class="segmented-button ${state.filters.ticketView === 'closed' ? 'active' : ''}" data-action="ticket-view" data-view="closed">历史</button>
                </div>
              </div>
              <div class="filter-right">
                <input class="text-input" data-bind="ticketKeyword" placeholder="搜索工单号、标题、状态" value="${escapeHtml(state.filters.ticketKeyword)}" style="width:240px;">
                <button class="ghost-button" data-action="apply-ticket-filter">搜索</button>
              </div>
            </div>
            <div class="table-wrap">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>工单号</th>
                    <th>标题 / 摘要</th>
                    <th>状态</th>
                    <th>处理人</th>
                    <th>更新时间</th>
                    <th>动作</th>
                  </tr>
                </thead>
                <tbody>
                  ${rows || '<tr><td colspan="6"><div class="empty-state"><div class="empty-icon">空</div><strong>暂无工单</strong><p>当前筛选条件下没有匹配的记录</p></div></td></tr>'}
                </tbody>
              </table>
            </div>
          </div>

          <aside class="panel summary-panel">
            <div class="panel-header">
              <h3>当前工单</h3>
              <small>${selected ? selected.ticketNo : '-'}</small>
            </div>
            <div class="summary-body">
              ${selected ? `
                <div class="summary-block">
                  <div class="summary-label">标题</div>
                  <h3 class="summary-title">${escapeHtml(selected.title)}</h3>
                  <div class="muted" style="margin-top:6px;font-size:12px;line-height:1.5;">${escapeHtml(selected.description)}</div>
                </div>
                <div class="summary-block">
                  <div class="summary-label">状态</div>
                  <span class="status-tag ${statusClass(selected.status)}">${escapeHtml(selected.status)}</span>
                </div>
                <div class="summary-block">
                  <div class="summary-label">结果</div>
                  <div class="muted" style="font-size:12px;line-height:1.6;">${escapeHtml(selected.resolution ? selected.resolution.summary : '暂无解决结果')}</div>
                </div>
                <div class="summary-actions">
                  <button class="primary-button" data-action="open-ticket" data-ticket-id="${selected.ticketId}">打开详情</button>
                </div>
                <div class="inline-tip">用户动作会根据工单状态和接口返回的可用操作动态展示。</div>
              ` : '<div class="empty-state"><div class="empty-icon">单</div><strong>暂无选中工单</strong><p>请选择一条工单查看详细信息</p></div>'}
            </div>
          </aside>
        </section>
      </div>
    `;
    shell(page);
  }

  function filteredSupportTickets() {
    const view = state.filters.supportTicketView || 'all';
    const ticketNo = String(state.filters.supportTicketNo || '').trim().toLowerCase();
    const requester = String(state.filters.supportRequester || '').trim().toLowerCase();
    let tickets = supportTickets();
    if (view === 'draft') tickets = supportDraftTickets();
    if (view === 'mine') tickets = supportMineTickets();
    if (view === 'post') tickets = supportPostTickets();
    if (view === 'risk') tickets = supportMineTickets().filter((ticket) => slaRisk(ticket) || slaInfo(ticket).timedOut);
    if (ticketNo) tickets = tickets.filter((ticket) => String(ticket.ticketNo || '').toLowerCase().includes(ticketNo));
    if (requester) tickets = tickets.filter((ticket) => String(ticket.requester && ticket.requester.displayName || '').toLowerCase().includes(requester));
    return tickets;
  }

  function paginateTickets(tickets, page, pageSize = 10) {
    const total = tickets.length;
    const totalPages = Math.max(1, Math.ceil(total / pageSize));
    const safePage = Math.min(totalPages, Math.max(1, Number(page) || 1));
    const start = (safePage - 1) * pageSize;
    return { total, totalPages, page: safePage, items: tickets.slice(start, start + pageSize) };
  }

  function renderSupportPager(paging) {
    const pages = [];
    const start = Math.max(1, paging.page - 2);
    const end = Math.min(paging.totalPages, paging.page + 2);
    for (let page = start; page <= end; page += 1) pages.push(page);
    return `
      <div class="pagination">
        <span class="pagination-meta">共 ${escapeHtml(String(paging.total))} 条 · 每页 10 条</span>
        <div class="pagination-controls">
          <button class="page-button" data-action="support-page" data-page="${Math.max(1, paging.page - 1)}" ${paging.page === 1 ? 'disabled' : ''}>‹</button>
          ${pages.map((page) => `<button class="page-button ${page === paging.page ? 'active' : ''}" data-action="support-page" data-page="${page}">${page}</button>`).join('')}
          <button class="page-button" data-action="support-page" data-page="${Math.min(paging.totalPages, paging.page + 1)}" ${paging.page === paging.totalPages ? 'disabled' : ''}>›</button>
        </div>
      </div>
    `;
  }

  function renderSupportTicketsWorkbenchPage() {
    const tickets = filteredSupportTickets();
    const paging = paginateTickets(tickets, state.filters.supportPage);
    state.filters.supportPage = paging.page;
    const views = [
      ['all', '全部'],
      ['draft', '草稿箱'],
      ['mine', '等待我处理'],
      ['post', '待本岗位'],
      ['risk', '即将超时']
    ];
    const rows = paging.items.map((ticket) => {
      const info = slaInfo(ticket);
      const risk = slaRisk(ticket) || info.timedOut;
      return `
        <tr>
          <td class="compact"><button class="link-button" data-action="open-ticket" data-ticket-id="${escapeHtml(ticket.ticketId)}">${escapeHtml(ticket.ticketNo)}</button></td>
          <td>
            <button class="link-button ticket-title-link" data-action="open-ticket" data-ticket-id="${escapeHtml(ticket.ticketId)}">${escapeHtml(ticket.title)}</button>
            <div class="session-preview">${escapeHtml(ticket.description)}</div>
          </td>
          <td class="compact">${escapeHtml(ticket.requester.displayName)}<div class="session-preview">${escapeHtml(ticket.requester.departmentName)}</div></td>
          <td class="compact"><span class="status-tag ${statusClass(ticket.status)}">${escapeHtml(statusLabel(ticket.status))}</span>${ticket.isSuspended ? '<div class="risk-tag suspended" style="margin-top:5px;">已挂起</div>' : ''}</td>
          <td class="compact"><span class="priority-tag ${priorityClass(ticket.priority)}">${escapeHtml(priorityLabel(ticket.priority))}</span></td>
          <td class="compact">${escapeHtml(businessLineName(ticket.businessLineCode))}</td>
          <td class="compact">${escapeHtml(ticket.assignee ? ticket.assignee.displayName : '待分配')}</td>
          <td class="compact">${escapeHtml(formatDateTime(ticket.createdAt))}</td>
          <td class="compact">${escapeHtml(formatDateTime(ticket.updatedAt))}</td>
          <td class="compact">${risk ? '<span class="risk-tag">时效风险</span>' : '<span class="muted">正常</span>'}</td>
          <td class="compact"><button class="small-button" data-action="open-ticket" data-ticket-id="${escapeHtml(ticket.ticketId)}">查看详情</button></td>
        </tr>
      `;
    }).join('');

    const page = `
      <div class="page support-tickets-page">
        <div class="page-heading">
          <div>
            <h1>全部工单</h1>
            <p>共 ${escapeHtml(String(tickets.length))} 条结果，当前第 ${escapeHtml(String(paging.page))} 页</p>
          </div>
          <div class="heading-actions"><span class="contract-tag">分页查询</span><button class="toolbar-button" data-action="nav" data-route="HOME">返回首页</button></div>
        </div>

        <section class="panel support-ticket-list-panel">
          <div class="support-toolbar support-ticket-toolbar">
            <div class="filter-left">
              <div class="segmented">
                ${views.map(([value, label]) => `<button class="segmented-button ${state.filters.supportTicketView === value ? 'active' : ''}" data-action="support-view" data-view="${value}">${label}</button>`).join('')}
              </div>
            </div>
            <div class="filter-right">
              <input class="text-input" data-bind="supportTicketNo" placeholder="工单号" value="${escapeHtml(state.filters.supportTicketNo)}" style="width:130px;">
              <input class="text-input" data-bind="supportRequester" placeholder="提单人" value="${escapeHtml(state.filters.supportRequester)}" style="width:130px;">
              <button class="ghost-button" data-action="apply-support-filter">查询</button>
              <button class="ghost-button" data-action="reset-support-filter">重置</button>
            </div>
          </div>
          <div class="table-wrap">
            <table class="data-table support-ticket-table">
              <thead>
                <tr><th>工单号</th><th>标题 / 描述</th><th>提单人</th><th>状态</th><th>优先级</th><th>业务线</th><th>责任人</th><th>创建时间</th><th>更新时间</th><th>时效</th><th>操作</th></tr>
              </thead>
              <tbody>${rows || '<tr><td colspan="11"><div class="empty-state"><div class="empty-icon">空</div><strong>没有匹配工单</strong><p>请调整查询条件或切换视图</p></div></td></tr>'}</tbody>
            </table>
          </div>
          ${renderSupportPager(paging)}
        </section>
      </div>
    `;
    shell(page);
  }

  function renderSupportTicketsPage() {
    if (isSupportRole()) {
      renderSupportTicketsWorkbenchPage();
      return;
    }
    const tickets = visibleTickets();
    const selected = tickets.find((item) => item.ticketId === state.activeTicketId) || tickets[0] || DATA.tickets[0];
    if (selected) {
      state.activeTicketId = selected.ticketId;
    }

    const pendingCount = DATA.tickets.filter((ticket) => ['PENDING_ACCEPTANCE', 'REOPENED'].includes(ticket.status)).length;
    const progressCount = DATA.tickets.filter((ticket) => ticket.status === 'IN_PROGRESS').length;
    const confirmCount = DATA.tickets.filter((ticket) => ticket.status === 'PENDING_USER_CONFIRM').length;
    const closedCount = DATA.tickets.filter((ticket) => ['RESOLVED', 'CLOSED'].includes(ticket.status)).length;

    const rows = tickets.slice(0, 8).map((ticket) => `
      <tr>
        <td class="compact">${escapeHtml(ticket.ticketNo)}</td>
        <td>
          <button class="link-button" data-action="open-ticket" data-ticket-id="${ticket.ticketId}">${escapeHtml(ticket.title)}</button>
          <div class="session-preview">${escapeHtml(ticket.description)}</div>
        </td>
        <td class="compact">${escapeHtml(ticket.requester.displayName)}</td>
        <td class="compact"><span class="status-tag ${statusClass(ticket.status)}">${escapeHtml(ticket.status)}</span></td>
        <td class="compact">${escapeHtml(formatDateTime(ticket.updatedAt))}</td>
        <td class="compact">
          ${ticket.status === 'PENDING_ACCEPTANCE' || ticket.status === 'REOPENED'
            ? `<button class="small-button" data-action="accept-ticket" data-ticket-id="${ticket.ticketId}" ${hasPermission('ticket:accept') ? '' : 'disabled'}>受理</button>`
            : `<button class="small-button" data-action="open-ticket" data-ticket-id="${ticket.ticketId}">详情</button>`}
        </td>
      </tr>
    `).join('');

    const queue = DATA.tickets.filter((ticket) => ['PENDING_ACCEPTANCE', 'REOPENED'].includes(ticket.status)).slice(0, 4).map((ticket) => `
      <div class="queue-item">
        <div>
          <div class="queue-title"><span>${escapeHtml(ticket.title)}</span></div>
          <div class="queue-meta">${escapeHtml(ticket.ticketNo)} · ${escapeHtml(ticket.requester.displayName)} · ${escapeHtml(ticket.businessLineCode)}</div>
        </div>
        <div class="queue-actions">
          <span class="status-tag ${statusClass(ticket.status)}">${escapeHtml(ticket.status)}</span>
          <time>${escapeHtml(formatDateTime(ticket.updatedAt || ticket.createdAt))}</time>
        </div>
      </div>
    `).join('');

    const page = `
      <div class="page">
        <div class="page-heading">
          <div>
            <h1>客服工单台</h1>
            <p>客服可以在这里处理待受理、处理中和待确认工单，详情会根据当前状态展示可用动作。</p>
          </div>
          <div class="heading-actions">
            <span class="contract-tag">` + escapeHtml(String(tickets.length)) + ` 条结果</span>
            <button class="toolbar-button" data-action="nav" data-route="HOME">返回首页</button>
          </div>
        </div>

        <section class="grid-4">
          ${metricCard('待受理', String(pendingCount), '进入客服队列的工单')}
          ${metricCard('处理中', String(progressCount), '正在处理中的工单')}
          ${metricCard('待确认', String(confirmCount), '等待用户确认结果')}
          ${metricCard('闭环', String(closedCount), '已解决或已关闭')}
        </section>

        <section class="grid-3" style="margin-top:14px;">
          <aside class="panel queue-panel">
            <div class="panel-header">
              <h3>待受理队列</h3>
              <small>${pendingCount} 条</small>
            </div>
            <div class="queue-list">
              ${queue || '<div class="empty-state"><div class="empty-icon">队</div><strong>暂无待受理工单</strong><p>当前没有等待受理的请求</p></div>'}
            </div>
          </aside>

          <section class="panel">
            <div class="panel-header">
              <h3>工单列表</h3>
              <small>共 ${tickets.length} 条</small>
            </div>
            <div class="support-toolbar">
              <div class="filter-left">
                <div class="segmented">
                  <button class="segmented-button ${state.filters.ticketView === 'all' ? 'active' : ''}" data-action="ticket-view" data-view="all">全部</button>
                  <button class="segmented-button ${state.filters.ticketView === 'pending' ? 'active' : ''}" data-action="ticket-view" data-view="pending">待受理</button>
                  <button class="segmented-button ${state.filters.ticketView === 'progress' ? 'active' : ''}" data-action="ticket-view" data-view="progress">处理中</button>
                  <button class="segmented-button ${state.filters.ticketView === 'confirm' ? 'active' : ''}" data-action="ticket-view" data-view="confirm">待确认</button>
                  <button class="segmented-button ${state.filters.ticketView === 'closed' ? 'active' : ''}" data-action="ticket-view" data-view="closed">历史</button>
                </div>
              </div>
              <div class="filter-right">
                <input class="text-input" data-bind="ticketKeyword" placeholder="搜索工单号、标题、申请人" value="${escapeHtml(state.filters.ticketKeyword)}" style="width:250px;">
                <button class="ghost-button" data-action="apply-ticket-filter">搜索</button>
              </div>
            </div>
            <div class="table-wrap">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>工单号</th>
                    <th>标题 / 摘要</th>
                    <th>申请人</th>
                    <th>状态</th>
                    <th>更新时间</th>
                    <th>动作</th>
                  </tr>
                </thead>
                <tbody>
                  ${rows || '<tr><td colspan="6"><div class="empty-state"><div class="empty-icon">空</div><strong>暂无工单</strong><p>请调整筛选条件或切换视图</p></div></td></tr>'}
                </tbody>
              </table>
            </div>
          </section>

          <aside class="panel summary-panel">
            <div class="panel-header">
              <h3>当前选中</h3>
              <small>${selected ? selected.ticketNo : '-'}</small>
            </div>
            <div class="summary-body">
              ${selected ? `
                <div class="summary-block">
                  <div class="summary-label">标题</div>
                  <h3 class="summary-title">${escapeHtml(selected.title)}</h3>
                  <div class="muted" style="margin-top:6px;font-size:12px;line-height:1.5;">${escapeHtml(selected.description)}</div>
                </div>
                <div class="summary-block">
                  <div class="summary-label">状态</div>
                  <span class="status-tag ${statusClass(selected.status)}">${escapeHtml(selected.status)}</span>
                  <div class="muted" style="margin-top:6px;font-size:12px;line-height:1.5;">${escapeHtml(selected.requester.displayName)} · ${escapeHtml(selected.businessLineCode)}</div>
                </div>
                <div class="summary-block">
                  <div class="summary-label">结果</div>
                  <div class="muted" style="font-size:12px;line-height:1.6;">${escapeHtml(selected.resolution ? selected.resolution.summary : '暂无解决结果')}</div>
                </div>
                <div class="summary-actions">
                  <button class="primary-button" data-action="open-ticket" data-ticket-id="${selected.ticketId}">打开详情</button>
                </div>
                <div class="inline-tip">客服动作会根据工单状态和接口返回的可用操作动态展示。</div>
              ` : '<div class="empty-state"><div class="empty-icon">单</div><strong>暂无选中工单</strong><p>请选择一条工单查看详细信息</p></div>'}
            </div>
          </aside>
        </section>
      </div>
    `;
    shell(page);
  }

  function renderConfigPage() {
    if (!hasMenu('CONFIG')) {
      shell(`
        <div class="page">
          <div class="panel">
            <div class="empty-state">
              <div class="empty-icon">锁</div>
              <strong>无权访问</strong>
              <p>当前角色没有配置管理入口，请切换为管理员示例身份。</p>
              <button class="primary-button" data-action="nav" data-route="HOME">返回首页</button>
            </div>
          </div>
        </div>
      `);
      return;
    }

    const dict = DATA.dictionaries[state.activeDictTab];
    const keywordItems = visibleDictItems(state.activeDictTab);
    const roles = DATA.roles.filter((role) => !state.filters.roleKeyword || [role.roleCode, role.roleName, role.description].join(' ').toLowerCase().includes(state.filters.roleKeyword.toLowerCase()));
    const activeRole = DATA.rolePermissions[state.activeRoleId] || DATA.rolePermissions.role_support_agent;

    const dictSidebar = dictTabs.map((tab) => `
      <button class="config-item ${state.activeDictTab === tab.key ? 'active' : ''}" data-action="dict-tab" data-tab="${tab.key}">
        <div class="config-title">
          <span>${escapeHtml(tab.label)}</span>
          <span class="session-time">${escapeHtml(String(DATA.dictionaries[tab.key].items.length))}</span>
        </div>
        <div class="config-meta">${escapeHtml(DATA.dictionaries[tab.key].description)}</div>
      </button>
    `).join('');

    const dictRows = keywordItems.map((item) => `
      <tr>
        <td class="compact"><input type="checkbox" aria-label="${escapeHtml(item.name)}"></td>
        <td class="compact"><code>${escapeHtml(item.code)}</code></td>
        <td><strong>${escapeHtml(item.name)}</strong></td>
        <td class="compact">${escapeHtml(item.parentId || '-')}</td>
        <td class="compact"><span class="status-chip ${item.enabled ? 'ok' : 'bad'}">${item.enabled ? '启用' : '停用'}</span></td>
        <td class="compact">${escapeHtml(String(item.version))}</td>
        <td class="compact">${escapeHtml(formatDateTime(item.updatedAt))}</td>
        <td class="compact">
          <button class="small-button" data-action="open-edit-dict" data-item-id="${item.itemId}">编辑</button>
          ${item.enabled ? `<button class="small-button" data-action="disable-dict" data-item-id="${item.itemId}">停用</button>` : '<span class="muted">已停用</span>'}
        </td>
      </tr>
    `).join('');

    const roleRows = roles.map((role) => `
      <button class="role-item ${role.roleId === state.activeRoleId ? 'active' : ''}" data-action="role-select" data-role-id="${role.roleId}">
        <div class="role-title">
          <span>${escapeHtml(role.roleName)}</span>
          <span class="session-time">${role.enabled ? '启用' : '停用'}</span>
        </div>
        <div class="role-meta">${escapeHtml(role.roleCode)} · ${escapeHtml(role.description)}</div>
      </button>
    `).join('');

    const permissionRows = (activeRole.permissions || []).map((permission) => `
      <tr>
        <td class="compact">${escapeHtml(permission.permissionCode)}</td>
        <td>${escapeHtml(permission.permissionName)}</td>
        <td class="compact">${escapeHtml(permission.permissionType)}</td>
      </tr>
    `).join('');

    const page = `
      <div class="page">
        <div class="page-heading">
          <div>
            <h1>配置管理</h1>
            <p>字典、角色和权限查询按接口文档展示，写操作保留二次确认入口。</p>
          </div>
          <div class="heading-actions">
            <button class="toolbar-button" data-action="refresh-config">刷新</button>
            <button class="primary-button" data-action="create-dict">新增字典项</button>
          </div>
        </div>

        <section class="grid-4">
          ${metricCard('字典类型', String(dictTabs.length), '本期提供的配置类型')}
          ${metricCard('当前字典', dict.title, `共 ${keywordItems.length} 条`)}
          ${metricCard('角色数', String(DATA.roles.length), '本租户可用角色')}
          ${metricCard('权限版本', currentProfile().login.data.permissionsVersion, currentRoleLabel())}
        </section>

        <section class="config-layout" style="margin-top:14px;">
          <aside class="config-sidebar panel">
            <div class="panel-header">
              <h3>字典目录</h3>
              <small>${escapeHtml(String(dict.items.length))} 项</small>
            </div>
            <div class="config-search">
              <input class="text-input" data-bind="dictKeyword" placeholder="搜索字典编码或名称" value="${escapeHtml(state.filters.dictKeyword)}">
            </div>
            <div class="config-tab-list">${dictSidebar}</div>
          </aside>

          <div class="stack">
            <div class="panel">
              <div class="config-tools">
                <div class="config-tools-left">
                  <strong>${escapeHtml(dict.title)}</strong>
                  <span class="muted">共 ${keywordItems.length} 条</span>
                  <span class="contract-tag">${escapeHtml(dict.dictType)}</span>
                </div>
                <div class="config-tools-right">
                  <input class="text-input" data-bind="dictKeyword" placeholder="按编码、名称或说明筛选" value="${escapeHtml(state.filters.dictKeyword)}" style="width:240px;">
                  <button class="ghost-button" data-action="apply-dict-filter">搜索</button>
                </div>
              </div>
              <div class="table-wrap">
                <table class="data-table">
                  <thead>
                    <tr>
                      <th style="width:46px">选</th>
                      <th>编码</th>
                      <th>名称</th>
                      <th>父级</th>
                      <th>状态</th>
                      <th>版本</th>
                      <th>更新时间</th>
                      <th>操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    ${dictRows || '<tr><td colspan="8"><div class="empty-state"><div class="empty-icon">空</div><strong>暂无数据</strong><p>当前字典类型没有匹配的条目</p></div></td></tr>'}
                  </tbody>
                </table>
              </div>
            </div>

            <div class="grid-2">
              <div class="panel">
                <div class="panel-header">
                  <h3>角色列表</h3>
                  <small>当前租户可用角色</small>
                </div>
                <div class="config-search">
                  <input class="text-input" data-bind="roleKeyword" placeholder="搜索角色编码或名称" value="${escapeHtml(state.filters.roleKeyword)}">
                </div>
                <div class="role-list">${roleRows}</div>
              </div>
              <div class="panel">
                <div class="panel-header">
                  <h3>${escapeHtml(activeRole.roleName)} 权限</h3>
                  <small>${escapeHtml(String(activeRole.permissions.length))} 项</small>
                </div>
                <div class="panel-body">
                  <div class="meta-tags">
                    ${(activeRole.permissions || []).map((permission) => `<span class="perm-tag">${escapeHtml(permission.permissionName)}</span>`).join('')}
                  </div>
                  <div class="divider"></div>
                  <div class="table-wrap">
                    <table class="data-table">
                      <thead>
                        <tr>
                          <th>权限编码</th>
                          <th>权限名称</th>
                          <th>类型</th>
                        </tr>
                      </thead>
                      <tbody>
                        ${permissionRows || '<tr><td colspan="3"><div class="empty-state"><div class="empty-icon">权</div><strong>暂无权限</strong><p>该角色当前没有配置权限</p></div></td></tr>'}
                      </tbody>
                    </table>
                  </div>
                  <div class="inline-tip" style="margin-top:12px;">按钮可见性只做体验控制，真正的授权仍以后端返回和服务端校验为准。</div>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>
    `;
    shell(page);
  }

  function renderSupervisorTicketDrawer(ticket) {
    const classification = ticket.classification || {};
    const history = (ticket.statusHistory || []).map((item) => `
      <div class="timeline-item">
        <div class="timeline-marker"></div>
        <div class="timeline-content">
          <div class="timeline-topline"><strong>${escapeHtml(statusLabel(item.status))}</strong><time>${escapeHtml(formatDateTime(item.occurredAt))}</time></div>
          <div class="muted">${escapeHtml(item.operator || '-')} · ${escapeHtml(item.note || '-')}</div>
        </div>
      </div>
    `).join('');
    const audit = (ticket.auditEvents || []).map((item) => `
      <div class="message-mini"><strong>${escapeHtml(item.action || '-')}</strong><br>${escapeHtml(formatDateTime(item.occurredAt))} · ${escapeHtml(item.actor || '-')}</div>
    `).join('');

    overlayRoot.innerHTML = `
      <div class="drawer-backdrop" data-action="close-overlay"></div>
      <aside class="drawer supervisor-drawer">
        <div class="drawer-header">
          <div>
            <div class="drawer-eyebrow">主管只读详情</div>
            <h2>${escapeHtml(ticket.title)}</h2>
            <p>${escapeHtml(ticket.ticketNo)} · ${escapeHtml(ticket.businessLineCode)} · ${escapeHtml(ticket.source)}</p>
            <div class="meta-tags" style="margin-top:8px;">
              <span class="status-tag ${statusClass(ticket.status)}">${escapeHtml(statusLabel(ticket.status))}</span>
              <span class="priority-tag ${priorityClass(ticket.priority)}">${escapeHtml(priorityLabel(ticket.priority))}优先级</span>
              <span class="contract-tag">${escapeHtml(ticket.assignee ? ticket.assignee.displayName : '待分配')}</span>
            </div>
          </div>
          <button class="icon-button" data-action="close-overlay" aria-label="关闭详情">×</button>
        </div>
        <div class="drawer-body supervisor-drawer-body">
          <div class="detail-grid">
            <div class="detail-card">
              <h3>工单信息</h3>
              <div class="detail-row"><label>工单号</label><div>${escapeHtml(ticket.ticketNo)}</div></div>
              <div class="detail-row"><label>请求人</label><div>${escapeHtml(ticket.requester.displayName)} · ${escapeHtml(ticket.requester.departmentName)}</div></div>
              <div class="detail-row"><label>业务线</label><div>${escapeHtml(ticket.businessLineCode)}</div></div>
              <div class="detail-row"><label>队列</label><div>${escapeHtml(ticket.queueId || '-')}</div></div>
              <div class="detail-row"><label>创建时间</label><div>${escapeHtml(formatDateTime(ticket.createdAt))}</div></div>
              <div class="detail-row"><label>更新时间</label><div>${escapeHtml(formatDateTime(ticket.updatedAt))}</div></div>
            </div>
            <div class="detail-card">
              <h3>当前状态</h3>
              <div class="supervisor-current-status"><span class="status-tag ${statusClass(ticket.status)}">${escapeHtml(statusLabel(ticket.status))}</span><strong>${escapeHtml(ticket.assignee ? ticket.assignee.displayName : '待分配')}</strong></div>
              <div class="detail-row"><label>来源</label><div>${escapeHtml(ticket.source)}</div></div>
              <div class="detail-row"><label>消息数</label><div>${escapeHtml(String(ticket.conversation ? ticket.conversation.messageCount : 0))}</div></div>
              <div class="detail-row"><label>审计事件</label><div>${escapeHtml(String((ticket.auditEvents || []).length))} 条</div></div>
            </div>
          </div>

          <div class="detail-card" style="margin-top:12px;">
            <h3>问题描述与会话摘要</h3>
            <div class="supervisor-description">${escapeHtml(ticket.description || '暂无描述')}</div>
            <div class="message-mini" style="margin-top:10px;">会话摘要：${escapeHtml(ticket.conversation ? ticket.conversation.summary : '暂无会话摘要')}</div>
          </div>

          <div class="detail-card" style="margin-top:12px;">
            <h3>分类信息</h3>
            <div class="detail-grid supervisor-classification-grid">
              <div class="detail-row"><label>管理单元</label><div>${escapeHtml(dictionaryLabel('MANAGEMENT_UNIT', classification.managementUnitId))}</div></div>
              <div class="detail-row"><label>症状</label><div>${escapeHtml(dictionaryLabel('SYMPTOM', classification.symptomId))}</div></div>
              <div class="detail-row"><label>原因</label><div>${escapeHtml(dictionaryLabel('REASON', classification.reasonId))}</div></div>
              <div class="detail-row"><label>解决方法</label><div>${escapeHtml(dictionaryLabel('SOLUTION_METHOD', classification.solutionMethodId))}</div></div>
            </div>
            ${classification.customReason ? `<div class="message-mini" style="margin-top:10px;">补充原因：${escapeHtml(classification.customReason)}</div>` : ''}
            ${classification.customSolution ? `<div class="message-mini" style="margin-top:10px;">补充方案：${escapeHtml(classification.customSolution)}</div>` : ''}
          </div>

          <div class="detail-grid" style="margin-top:12px;">
            <div class="detail-card">
              <h3>状态流转</h3>
              <div class="timeline">${history || '<div class="message-mini">暂无状态历史</div>'}</div>
            </div>
            <div class="detail-card">
              <h3>审计记录</h3>
              ${audit || '<div class="message-mini">暂无审计事件</div>'}
            </div>
          </div>

          <div class="detail-card" style="margin-top:12px;">
            <h3>处理结果与评价</h3>
            <div class="detail-row"><label>结果摘要</label><div>${escapeHtml(ticket.resolution ? ticket.resolution.summary : '暂无解决结果')}</div></div>
            <div class="detail-row"><label>解决方式</label><div>${escapeHtml(ticket.resolution ? ticket.resolution.method : '暂无解决方式')}</div></div>
            <div class="detail-row"><label>用户评价</label><div>${ticket.rating ? `${escapeHtml(String(ticket.rating.score))} 分 · ${escapeHtml(ticket.rating.comment || '无补充评价')}` : '尚未评价'}</div></div>
          </div>
        </div>
        <div class="drawer-footer">
          <span class="muted">主管仅可查看工单详情和审计信息。</span>
          <div class="drawer-actions"><button class="primary-button" data-action="close-overlay">完成查看</button></div>
        </div>
      </aside>
    `;
  }

  function renderSupportTicketDrawer(ticket) {
    const currentUserId = currentUser().userId;
    const isMine = ticket.assignee && ticket.assignee.userId === currentUserId;
    const canAccept = ['PENDING_ACCEPTANCE', 'REOPENED'].includes(ticket.status) && !ticket.assignee;
    const canEdit = ['IN_PROGRESS', 'REOPENED'].includes(ticket.status) && isMine;
    const canResolve = ticket.status === 'IN_PROGRESS' && isMine;
    const canClose = ticket.status === 'RESOLVED' && isMine;
    const canSuspend = ['IN_PROGRESS', 'REOPENED'].includes(ticket.status) && isMine;
    const classification = ticket.classification || {};
    const info = slaInfo(ticket);
    const history = (ticket.statusHistory || []).map((item) => `
      <div class="timeline-item">
        <div class="timeline-marker"></div>
        <div class="timeline-content">
          <div class="timeline-topline"><strong>${escapeHtml(statusLabel(item.status))}</strong><time>${escapeHtml(formatDateTime(item.occurredAt))}</time></div>
          <div class="muted">${escapeHtml(item.operator || '-')} · ${escapeHtml(item.note || '-')}</div>
        </div>
      </div>
    `).join('');
    const ownership = (ticket.ownershipHistory || []).map((item) => `
      <div class="timeline-item">
        <div class="timeline-marker ownership"></div>
        <div class="timeline-content">
          <div class="timeline-topline"><strong>${escapeHtml(item.stage || '转单')}</strong><time>${escapeHtml(formatDateTime(item.occurredAt))}</time></div>
          <div class="muted">${escapeHtml(item.from || '-')} → ${escapeHtml(item.to || '-')}</div>
          <div class="muted-2">${escapeHtml(item.operator || '-')} · ${escapeHtml(item.note || '')}</div>
        </div>
      </div>
    `).join('');
    const sameLineOptions = sameLineAgents(ticket).map((agent) => `<option value="${escapeHtml(agent.userId)}">${escapeHtml(agent.displayName)}</option>`).join('');
    const otherLineOptions = BUSINESS_LINES.filter((line) => line.code !== ticket.businessLineCode).map((line) => `<option value="${escapeHtml(line.code)}">${escapeHtml(line.name)}</option>`).join('');
    const statusTransitions = {
      PENDING_ACCEPTANCE: ['IN_PROGRESS'],
      IN_PROGRESS: ['PENDING_USER_CONFIRM', 'REOPENED'],
      PENDING_USER_CONFIRM: ['RESOLVED', 'REOPENED'],
      RESOLVED: ['CLOSED', 'REOPENED'],
      CLOSED: ['REOPENED'],
      REOPENED: ['IN_PROGRESS', 'PENDING_USER_CONFIRM']
    };
    const visibleStatuses = [...new Set([ticket.status, ...(statusTransitions[ticket.status] || [])])];
    const statusOptions = visibleStatuses.map((status) => `<option value="${status}" ${ticket.status === status ? 'selected' : ''}>${escapeHtml(statusLabel(status))}</option>`).join('');
    const editable = canEdit ? '' : 'disabled';

    const topActions = [];
    if (canSuspend) {
      topActions.push(`<button class="ghost-button" data-action="toggle-suspend-ticket" data-ticket-id="${escapeHtml(ticket.ticketId)}">${ticket.isSuspended ? '恢复计时' : '工单挂起'}</button>`);
    }
    if (isMine && ['IN_PROGRESS', 'REOPENED'].includes(ticket.status)) {
      topActions.push(`<button class="ghost-button" data-action="scroll-to-transfer" data-target="same-line-transfer">同线转单</button>`);
      topActions.push(`<button class="ghost-button" data-action="scroll-to-transfer" data-target="cross-line-transfer">跨业务线转单</button>`);
    }

    const footerButtons = [];
    if (canAccept && hasPermission('ticket:accept')) footerButtons.push(`<button class="primary-button" data-action="accept-ticket" data-ticket-id="${escapeHtml(ticket.ticketId)}">受理工单</button>`);
    if (canEdit && hasPermission('ticket:classify')) footerButtons.push(`<button class="primary-button" data-action="save-classification" data-ticket-id="${escapeHtml(ticket.ticketId)}">保存分类</button>`);
    if (canResolve && hasPermission('ticket:resolve')) footerButtons.push(`<button class="primary-button" data-action="resolve-ticket" data-ticket-id="${escapeHtml(ticket.ticketId)}">提交解决</button>`);
    if (canClose && hasPermission('ticket:close')) footerButtons.push(`<button class="danger-button" data-action="close-ticket" data-ticket-id="${escapeHtml(ticket.ticketId)}">关闭工单</button>`);

    overlayRoot.innerHTML = `
      <div class="drawer-backdrop" data-action="close-overlay"></div>
      <aside class="drawer support-ticket-drawer">
        <div class="drawer-header support-ticket-drawer-header">
          <div class="drawer-header-main">
            <div class="drawer-eyebrow">工单号 ${escapeHtml(ticket.ticketNo)} · ${escapeHtml(businessLineName(ticket.businessLineCode))}</div>
            <h2>${escapeHtml(ticket.title)}</h2>
            <p>${escapeHtml(ticket.requester.displayName)} · ${escapeHtml(ticket.requester.departmentName)}</p>
            <div class="meta-tags">
              <span class="status-tag ${statusClass(ticket.status)}">${escapeHtml(statusLabel(ticket.status))}</span>
              <span class="priority-tag ${priorityClass(ticket.priority)}">${escapeHtml(priorityLabel(ticket.priority))}优先级</span>
              <span class="contract-tag">${escapeHtml(ticket.source)}</span>
            </div>
          </div>
          <div class="drawer-header-actions">
            <button class="icon-button" data-action="close-overlay" aria-label="关闭详情">×</button>
            <div class="drawer-top-actions">${topActions.join('')}</div>
          </div>
        </div>

        <div class="drawer-body support-ticket-drawer-body">
          <div class="support-ticket-layout">
            <div class="support-ticket-main">
              <div class="detail-card">
                <h3>工单信息与状态修改</h3>
                <div class="detail-grid">
                  <div class="detail-row"><label>工单号</label><div>${escapeHtml(ticket.ticketNo)}</div></div>
                  <div class="detail-row"><label>提单人</label><div>${escapeHtml(ticket.requester.displayName)}</div></div>
                  <div class="detail-row"><label>主题</label><div>${escapeHtml(ticket.title)}</div></div>
                  <div class="detail-row"><label>业务线</label><div>${escapeHtml(businessLineName(ticket.businessLineCode))}</div></div>
                </div>
                <div class="support-status-editor">
                  <div class="field">
                    <label>工单状态</label>
                    <select class="select-input" name="supportTicketStatus" ${canEdit ? '' : 'disabled'}>${statusOptions}</select>
                  </div>
                  ${canEdit ? `<button class="primary-button" data-action="apply-ticket-status" data-ticket-id="${escapeHtml(ticket.ticketId)}">修改状态</button>` : ''}
                </div>
                <div class="divider"></div>
                <div class="detail-row"><label>当前责任人</label><div><strong>${escapeHtml(ticket.assignee ? ticket.assignee.displayName : '待分配')}</strong></div></div>
                <div class="ownership-timeline">
                  <div class="section-subtitle">责任人变更记录</div>
                  <div class="timeline">${ownership || '<div class="message-mini">暂无责任人变更记录</div>'}</div>
                </div>
              </div>

              <div class="detail-card">
                <h3>问题描述</h3>
                <div class="support-description">${escapeHtml(ticket.description)}</div>
                <div class="message-mini" style="margin-top:10px;">会话摘要：${escapeHtml(ticket.conversation ? ticket.conversation.summary : '暂无会话摘要')}</div>
              </div>

              <div class="detail-card sla-card">
                <div class="detail-card-heading"><h3>处理时限</h3><span class="muted">开始于 ${escapeHtml(formatDateTime(ticket.slaStartedAt || ticket.createdAt))}</span></div>
                ${supportSlaMarkup(ticket)}
                <div class="sla-rule-note">网络问题按 4 小时，操作系统问题按 3 小时，其他工单默认按 8 小时。</div>
              </div>

              <div class="detail-card">
                <h3>分类、症状、原因与解决方法</h3>
                <form id="classification-form" class="classification-form">
                  <div class="field"><label>管理单元</label><select class="select-input" name="managementUnitId" ${editable}>${filterDictOptions('MANAGEMENT_UNIT', classification.managementUnitId, (item) => !item.parentId)}</select></div>
                  <div class="field"><label>症状</label><select class="select-input" name="symptomId" ${editable}>${filterDictOptions('SYMPTOM', classification.symptomId)}</select></div>
                  <div class="field"><label>原因</label><select class="select-input" name="reasonId" ${editable}>${filterDictOptions('REASON', classification.reasonId)}</select></div>
                  <div class="field"><label>解决方法</label><select class="select-input" name="solutionMethodId" ${editable}>${filterDictOptions('SOLUTION_METHOD', classification.solutionMethodId)}</select></div>
                  <div class="field full"><label>自定义原因</label><textarea class="textarea-input" name="customReason" ${editable}>${escapeHtml(classification.customReason || '')}</textarea></div>
                  <div class="field full"><label>自定义解决方法</label><textarea class="textarea-input" name="customSolution" ${editable}>${escapeHtml(classification.customSolution || '')}</textarea></div>
                </form>
              </div>
            </div>

            <aside class="support-ticket-side">
              <div class="detail-card">
                <h3>处理操作</h3>
                <div class="detail-lines">
                  <div><span>当前状态</span><strong>${escapeHtml(statusLabel(ticket.status))}</strong></div>
                  <div><span>处理人</span><strong>${escapeHtml(ticket.assignee ? ticket.assignee.displayName : '待分配')}</strong></div>
                  <div><span>开始时间</span><strong>${escapeHtml(formatDateTime(ticket.slaStartedAt || ticket.createdAt))}</strong></div>
                  <div><span>已耗时</span><strong>${escapeHtml(formatDuration(info.elapsedMs))}</strong></div>
                </div>
                ${ticket.isSuspended ? `<div class="suspension-note">挂起原因：${escapeHtml(ticket.suspendedReason || '用户长时间未回复，工单超时挂起')}<br>挂起时间：${escapeHtml(formatDateTime(ticket.suspendedAt || ticket.updatedAt))}</div>` : ''}
              </div>

              <div class="detail-card" id="same-line-transfer">
                <h3>转给同业务线同事</h3>
                <div class="field">
                  <label>选择同事</label>
                  <select class="select-input" name="sameLineAgent" ${canEdit ? '' : 'disabled'}>${sameLineOptions || '<option value="">暂无同线同事</option>'}</select>
                </div>
                <button class="primary-button" data-action="apply-same-line-transfer" data-ticket-id="${escapeHtml(ticket.ticketId)}" ${canEdit ? '' : 'disabled'}>确认转交</button>
              </div>

              <div class="detail-card" id="cross-line-transfer">
                <h3>转给其他业务线</h3>
                <div class="field">
                  <label>目标业务线</label>
                  <select class="select-input" name="targetBusinessLine" ${canEdit ? '' : 'disabled'}>${otherLineOptions}</select>
                </div>
                <button class="ghost-button" data-action="apply-business-line-transfer" data-ticket-id="${escapeHtml(ticket.ticketId)}" ${canEdit ? '' : 'disabled'}>提交转单</button>
                <div class="field-help">零售、售后、物流等业务线承接能力暂未接入。</div>
              </div>

              <div class="detail-card">
                <h3>状态流转</h3>
                <div class="timeline">${history || '<div class="message-mini">暂无状态历史</div>'}</div>
              </div>
            </aside>
          </div>
        </div>

        <div class="drawer-footer">
          <span class="muted">SLA 计时随状态与挂起状态实时更新。</span>
          <div class="drawer-actions">${footerButtons.join('')}</div>
        </div>
      </aside>
    `;
    startSlaClock();
  }

  function renderTicketDrawer(ticket) {
    if (isSupervisorRole()) {
      renderSupervisorTicketDrawer(ticket);
      return;
    }
    if (isSupportRole()) {
      renderSupportTicketDrawer(ticket);
      return;
    }
    const isUser = isUserRole();
    const canAccept = !isUser && (ticket.status === 'PENDING_ACCEPTANCE' || ticket.status === 'REOPENED');
    const canClassify = !isUser && ['IN_PROGRESS', 'REOPENED'].includes(ticket.status);
    const canResolve = !isUser && ticket.status === 'IN_PROGRESS';
    const canConfirm = isUser && ticket.status === 'PENDING_USER_CONFIRM';
    const canClose = !isUser && ticket.status === 'RESOLVED';
    const canRating = isUser && ['RESOLVED', 'CLOSED'].includes(ticket.status);
    const history = ticket.statusHistory.map((item) => `
      <div class="message-mini">${escapeHtml(formatDateTime(item.occurredAt))} · ${escapeHtml(item.status)} · ${escapeHtml(item.operator)} · ${escapeHtml(item.note)}</div>
    `).join('');
    const audit = ticket.auditEvents.map((item) => `
      <div class="message-mini">${escapeHtml(formatDateTime(item.occurredAt))} · ${escapeHtml(item.action)} · ${escapeHtml(item.actor)}</div>
    `).join('');

    const classificationForm = `
      <form id="classification-form" class="classification-form">
        <div class="field">
          <label>managementUnitId</label>
          <select class="select-input" name="managementUnitId" ${canClassify ? '' : 'disabled'}>
            ${filterDictOptions('MANAGEMENT_UNIT', ticket.classification.managementUnitId, (item) => !item.parentId)}
          </select>
        </div>
        <div class="field">
          <label>symptomId</label>
          <select class="select-input" name="symptomId" ${canClassify ? '' : 'disabled'}>
            ${filterDictOptions('SYMPTOM', ticket.classification.symptomId)}
          </select>
        </div>
        <div class="field">
          <label>reasonId</label>
          <select class="select-input" name="reasonId" ${canClassify ? '' : 'disabled'}>
            ${filterDictOptions('REASON', ticket.classification.reasonId)}
          </select>
        </div>
        <div class="field">
          <label>solutionMethodId</label>
          <select class="select-input" name="solutionMethodId" ${canClassify ? '' : 'disabled'}>
            ${filterDictOptions('SOLUTION_METHOD', ticket.classification.solutionMethodId)}
          </select>
        </div>
        <div class="field">
          <label>customReason</label>
          <textarea class="textarea-input" name="customReason" ${canClassify ? '' : 'disabled'}>${escapeHtml(ticket.classification.customReason || '')}</textarea>
        </div>
        <div class="field">
          <label>customSolution</label>
          <textarea class="textarea-input" name="customSolution" ${canClassify ? '' : 'disabled'}>${escapeHtml(ticket.classification.customSolution || '')}</textarea>
        </div>
      </form>
    `;

    const footerButtons = [];
    if (canAccept) {
      footerButtons.push(`<button class="primary-button" data-action="accept-ticket" data-ticket-id="${ticket.ticketId}" ${hasPermission('ticket:accept') ? '' : 'disabled'}>受理</button>`);
    }
    if (canClassify) {
      footerButtons.push(`<button class="ghost-button" data-action="save-classification" data-ticket-id="${ticket.ticketId}" ${hasPermission('ticket:classify') ? '' : 'disabled'}>保存分类</button>`);
    }
    if (canResolve) {
      footerButtons.push(`<button class="primary-button" data-action="resolve-ticket" data-ticket-id="${ticket.ticketId}" ${hasPermission('ticket:resolve') ? '' : 'disabled'}>提交解决</button>`);
    }
    if (canConfirm) {
      footerButtons.push(`<button class="primary-button" data-action="confirm-ticket" data-ticket-id="${ticket.ticketId}" ${hasPermission('ticket:confirm') ? '' : 'disabled'}>确认解决</button>`);
    }
    if (canClose) {
      footerButtons.push(`<button class="danger-button" data-action="close-ticket" data-ticket-id="${ticket.ticketId}" ${hasPermission('ticket:close') ? '' : 'disabled'}>关闭工单</button>`);
    }
    if (isUser && ['RESOLVED', 'CLOSED'].includes(ticket.status)) {
      footerButtons.push(`<button class="ghost-button" data-action="reopen-ticket" data-ticket-id="${ticket.ticketId}" ${hasPermission('ticket:reopen') ? '' : 'disabled'}>重开工单</button>`);
    }
    if (canRating) {
      footerButtons.push(`<button class="ghost-button" data-action="open-rating" data-ticket-id="${ticket.ticketId}" ${hasPermission('ticket:rating') ? '' : 'disabled'}>评价工单</button>`);
    }

    overlayRoot.innerHTML = `
      <div class="drawer-backdrop" data-action="close-overlay"></div>
      <aside class="drawer">
        <div class="drawer-header">
          <div>
            <h2>${escapeHtml(ticket.title)}</h2>
            <p>${escapeHtml(ticket.ticketNo)} · ${escapeHtml(ticket.businessLineCode)} · ${escapeHtml(ticket.source)}</p>
            <div class="meta-tags" style="margin-top:8px;">
              <span class="status-tag ${statusClass(ticket.status)}">${escapeHtml(ticket.status)}</span>
              <span class="priority-tag ${priorityClass(ticket.priority)}">${escapeHtml(ticket.priority)}</span>
            </div>
          </div>
          <button class="icon-button" data-action="close-overlay">×</button>
        </div>
        <div class="drawer-body">
          <div class="detail-grid">
            <div class="detail-card">
              <h3>请求摘要</h3>
              <div class="detail-row"><label>请求人</label><div>${escapeHtml(ticket.requester.displayName)} · ${escapeHtml(ticket.requester.departmentName)}</div></div>
              <div class="detail-row"><label>描述</label><div>${escapeHtml(ticket.description)}</div></div>
              <div class="detail-row"><label>会话摘要</label><div>${escapeHtml(ticket.conversation.summary)}</div></div>
              <div class="detail-row"><label>更新时间</label><div>${escapeHtml(formatDateTime(ticket.updatedAt))}</div></div>
            </div>
            <div class="detail-card">
              <h3>状态历史</h3>
              ${history || '<div class="message-mini">暂无历史</div>'}
            </div>
          </div>

          ${isUser ? `
            <div class="detail-card" style="margin-top:12px;">
              <h3>处理结果</h3>
              <div class="message-mini">结果摘要：${escapeHtml(ticket.resolution ? ticket.resolution.summary : '暂无结果')}</div>
              <div class="message-mini">解决方式：${escapeHtml(ticket.resolution ? ticket.resolution.method : '暂无结果')}</div>
              <div class="message-mini">工单评价：${ticket.rating ? `${escapeHtml(String(ticket.rating.score))} 分 · ${escapeHtml(ticket.rating.comment || '')}` : '尚未评价'}</div>
              ${canRating ? `
                <div class="rating-box" style="margin-top:12px;">
                  <div class="summary-label">提交评价</div>
                  <div class="rating-stars">
                    ${[1, 2, 3, 4, 5].map((score) => `
                      <button class="rating-star ${state.draft.ratingScore === score ? 'selected' : ''}" data-action="set-rating" data-score="${score}">${score}</button>
                    `).join('')}
                  </div>
                  <textarea class="textarea-input" id="ratingComment" placeholder="补充评价内容">${escapeHtml(state.draft.ratingComment)}</textarea>
                  <div class="inline-tip">评价按钮仅在 <code>RESOLVED</code> 或 <code>CLOSED</code> 时可用。</div>
                </div>
              ` : ''}
            </div>
          ` : `
            <div class="detail-card" style="margin-top:12px;">
              <h3>分类与处理</h3>
              ${classificationForm}
              <div class="inline-tip">分类字段按 <code>ITSM-SUPPORT-003</code> 的契约展示，保存动作由客服权限和工单状态共同决定。</div>
            </div>
            <div class="detail-card" style="margin-top:12px;">
              <h3>审计与结果</h3>
              ${audit || '<div class="message-mini">暂无审计事件</div>'}
              <div class="divider"></div>
              <div class="message-mini">解决结果：${escapeHtml(ticket.resolution ? ticket.resolution.summary : '未提交')}</div>
              <div class="message-mini">解决方式：${escapeHtml(ticket.resolution ? ticket.resolution.method : '未填写')}</div>
            </div>
          `}
        </div>
        <div class="drawer-footer">
          <span class="muted">${isUser ? '用户动作由工单状态决定。' : '客服动作由工单状态和权限共同决定。'}</span>
          <div class="drawer-actions">
            ${footerButtons.join('')}
          </div>
        </div>
      </aside>
    `;
  }

  function renderDictModal(mode, itemId) {
    const dict = DATA.dictionaries[state.activeDictTab];
    const item = itemId ? dict.items.find((entry) => entry.itemId === itemId) : null;
    const title = mode === 'edit' ? '编辑字典项' : '新增字典项';
    overlayRoot.innerHTML = `
      <div class="modal-backdrop">
        <form class="modal" id="dict-form">
          <div class="modal-header">
            <h3>${escapeHtml(title)}</h3>
            <button type="button" class="icon-button" data-action="close-overlay">×</button>
          </div>
          <div class="modal-body">
            <div class="form-stack">
              <div class="field">
                <label>code</label>
                <input class="text-input" name="code" value="${escapeHtml(item ? item.code : '')}" placeholder="如 LOGIN_FAILED">
              </div>
              <div class="field">
                <label>name</label>
                <input class="text-input" name="name" value="${escapeHtml(item ? item.name : '')}" placeholder="如 登录失败">
              </div>
              <div class="field-row">
                <div class="field">
                  <label>parentId</label>
                  <input class="text-input" name="parentId" value="${escapeHtml(item ? (item.parentId || '') : '')}" placeholder="上级字典项 ID">
                </div>
                <div class="field">
                  <label>sort</label>
                  <input class="text-input" name="sort" type="number" value="${escapeHtml(item ? item.sort : 10)}">
                </div>
              </div>
              <div class="field">
                <label>description</label>
                <textarea class="textarea-input" name="description">${escapeHtml(item ? (item.description || '') : '')}</textarea>
              </div>
              <div class="inline-tip">对应 <code>ITSM-DICT-002</code> / <code>ITSM-DICT-003</code> 的字段展示。真实保存时会依赖 <code>Idempotency-Key</code> 与字典启用状态校验。</div>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="ghost-button" data-action="close-overlay">取消</button>
            <button class="primary-button" type="submit">${escapeHtml(mode === 'edit' ? '保存修改' : '保存')}</button>
          </div>
        </form>
      </div>
    `;
  }

  function renderDisableModal(itemId) {
    const dict = DATA.dictionaries[state.activeDictTab];
    const item = dict.items.find((entry) => entry.itemId === itemId);
    overlayRoot.innerHTML = `
      <div class="modal-backdrop">
        <form class="modal" id="disable-form">
          <div class="modal-header">
            <h3>停用字典项</h3>
            <button type="button" class="icon-button" data-action="close-overlay">×</button>
          </div>
          <div class="modal-body">
            <p style="margin-top:0;line-height:1.7;">确认停用 <strong>${escapeHtml(item ? item.name : itemId)}</strong>？停用后新的工单和下拉选项将不再可选，但历史工单仍保留名称展示。</p>
            <div class="field">
              <label>reason</label>
              <textarea class="textarea-input" name="reason" placeholder="请输入停用原因"></textarea>
            </div>
            <div class="inline-tip">这与 <code>ITSM-DICT-004</code> 的二次确认行为保持一致。</div>
          </div>
          <div class="modal-footer">
            <button type="button" class="ghost-button" data-action="close-overlay">取消</button>
            <button class="danger-button" type="submit" data-item-id="${escapeHtml(itemId)}">确认停用</button>
          </div>
        </form>
      </div>
    `;
  }

  function renderOverlay() {
    if (!state.overlay) {
      overlayRoot.innerHTML = '';
      return;
    }

    if (state.overlay.type === 'ticket') {
      const ticket = DATA.tickets.find((item) => item.ticketId === state.overlay.ticketId) || getCurrentTicket();
      if (ticket) {
        renderTicketDrawer(ticket);
      }
      return;
    }

    if (state.overlay.type === 'dict-form') {
      renderDictModal(state.overlay.mode, state.overlay.itemId);
      return;
    }

    if (state.overlay.type === 'dict-disable') {
      renderDisableModal(state.overlay.itemId);
    }
  }

  function createTicketFromCurrentSession() {
    const session = getCurrentSession();
    const tenantId = currentTenant().tenantId;
    const user = currentUser();
    const now = nowIso();
    const ticketNo = `3053${String(DATA.tickets.length + 1).padStart(3, '0')}`;
    const ticket = {
      ticketId: `tkt_${Date.now()}`,
      ticketNo,
      tenantId,
      requester: {
        userId: user.userId,
        displayName: user.displayName,
        departmentName: user.departmentName
      },
      title: session.subject || '手动创建工单',
      description: session.summary || session.subject || '由会话创建的工单',
      source: 'CHAT_TO_TICKET',
      status: 'PENDING_ACCEPTANCE',
      priority: 'MEDIUM',
      businessLineCode: 'IT_SUPPORT',
      queueId: 'queue_it_support',
      classification: {
        managementUnitId: null,
        symptomId: null,
        reasonId: null,
        solutionMethodId: null,
        customReason: null,
        customSolution: null
      },
      assignee: null,
      conversation: {
        sessionId: session.sessionId,
        summary: session.summary || session.subject || '',
        messageCount: session.messages.length
      },
      statusHistory: [
        {
          status: 'NEW',
          occurredAt: now,
          operator: '系统',
          note: '由会话创建工单'
        },
        {
          status: 'PENDING_ACCEPTANCE',
          occurredAt: now,
          operator: '系统',
          note: '进入客服队列'
        }
      ],
      auditEvents: [
        {
          action: 'TicketCreated',
          occurredAt: now,
          actor: 'system'
        }
      ],
      resolution: null,
      rating: null,
      createdAt: now,
      updatedAt: now
    };
    DATA.tickets.unshift(ticket);
    session.ticketId = ticket.ticketId;
    session.status = 'TICKET_CREATED';
    session.summary = '工单已创建，等待客服受理。';
    session.lastMessageAt = now;
    state.activeTicketId = ticket.ticketId;
    showToast('工单已创建', ticket.ticketNo, 'success');
    return ticket;
  }

  function updateTicketStatus(ticketId, nextStatus, note, actionName) {
    const ticket = DATA.tickets.find((item) => item.ticketId === ticketId);
    if (!ticket) {
      showToast('操作失败', 'RESOURCE_NOT_FOUND', 'error');
      return false;
    }

    const allowed = {
      PENDING_ACCEPTANCE: ['IN_PROGRESS'],
      REOPENED: ['IN_PROGRESS'],
      IN_PROGRESS: ['PENDING_USER_CONFIRM'],
      PENDING_USER_CONFIRM: ['RESOLVED'],
      RESOLVED: ['CLOSED']
    };

    if (!allowed[ticket.status] || !allowed[ticket.status].includes(nextStatus)) {
      showToast('操作失败', 'ILLEGAL_STATE_TRANSITION · 当前状态不允许执行该操作', 'error');
      return false;
    }

    ticket.status = nextStatus;
    ticket.updatedAt = nowIso();
    ticket.statusHistory.unshift({
      status: nextStatus,
      occurredAt: ticket.updatedAt,
      operator: currentUser().displayName,
      note
    });
    ticket.auditEvents.unshift({
      action: actionName,
      occurredAt: ticket.updatedAt,
      actor: currentUser().userId
    });
    state.activeTicketId = ticket.ticketId;
    return true;
  }

  function applyTicketStatusFromDrawer(ticketId) {
    const ticket = DATA.tickets.find((item) => item.ticketId === ticketId);
    const select = document.querySelector('[name="supportTicketStatus"]');
    if (!ticket || !select) return false;
    const nextStatus = String(select.value || '');
    if (nextStatus === ticket.status) {
      showToast('状态未变更', '当前工单状态已经是最新值', 'info');
      return true;
    }
    const allowed = {
      PENDING_ACCEPTANCE: ['IN_PROGRESS', 'REOPENED'],
      IN_PROGRESS: ['PENDING_USER_CONFIRM', 'REOPENED'],
      PENDING_USER_CONFIRM: ['RESOLVED', 'REOPENED'],
      RESOLVED: ['CLOSED', 'REOPENED'],
      CLOSED: ['REOPENED'],
      REOPENED: ['IN_PROGRESS', 'PENDING_USER_CONFIRM']
    };
    if (!(allowed[ticket.status] || []).includes(nextStatus)) {
      showToast('状态修改失败', 'ILLEGAL_STATE_TRANSITION · 当前状态不允许该变更', 'error');
      return false;
    }
    const now = nowIso();
    ticket.status = nextStatus;
    ticket.updatedAt = now;
    (ticket.statusHistory || (ticket.statusHistory = [])).unshift({ status: nextStatus, occurredAt: now, operator: currentUser().displayName, note: '客服修改工单状态' });
    if (nextStatus === 'REOPENED') ticket.isSuspended = false;
    if (['PENDING_USER_CONFIRM', 'RESOLVED', 'CLOSED'].includes(nextStatus)) {
      ticket.slaAccumulatedMs = (Number(ticket.slaAccumulatedMs || 0) + Math.max(0, Date.now() - new Date(ticket.slaStartedAt || ticket.createdAt).getTime()));
      ticket.isSuspended = false;
      ticket.slaPausedAt = null;
    }
    if (nextStatus === 'IN_PROGRESS' && ticket.isSuspended && ticket.slaPausedAt) {
      ticket.isSuspended = false;
      ticket.suspendedReason = '';
      ticket.slaStartedAt = now;
      ticket.slaPausedAt = null;
    }
    showToast('状态已更新', `工单状态变更为 ${statusLabel(nextStatus)}`, 'success');
    return true;
  }

  function toggleTicketSuspend(ticketId) {
    const ticket = DATA.tickets.find((item) => item.ticketId === ticketId);
    if (!ticket) return false;
    const now = nowIso();
    if (!ticket.isSuspended) {
      const start = new Date(ticket.slaStartedAt || ticket.createdAt);
      if (Number.isFinite(start.getTime())) {
        ticket.slaAccumulatedMs = (Number(ticket.slaAccumulatedMs || 0) + Math.max(0, Date.now() - start.getTime()));
      }
      ticket.isSuspended = true;
      ticket.suspendedAt = now;
      ticket.suspendedReason = '用户长时间未回复，工单超时挂起';
      ticket.slaPausedAt = now;
      appendTicketHistory(ticket, ticket.status, `挂起工单：${ticket.suspendedReason}`);
    } else {
      ticket.isSuspended = false;
      ticket.suspendedReason = '';
      ticket.suspendedAt = null;
      ticket.slaPausedAt = null;
      ticket.slaStartedAt = now;
      appendTicketHistory(ticket, ticket.status, '恢复工单计时');
    }
    showToast(ticket.isSuspended ? '工单已挂起' : '计时已恢复', ticket.isSuspended ? '等待用户回复，SLA 计时已暂停' : '工单继续处理', 'success');
    return true;
  }

  function applySameLineTransfer(ticketId) {
    const ticket = DATA.tickets.find((item) => item.ticketId === ticketId);
    const select = document.querySelector('[name="sameLineAgent"]');
    if (!ticket || !select) return false;
    const target = (DATA.supportAgents || SUPPORT_AGENTS).find((agent) => agent.userId === select.value);
    if (!target) {
      showToast('转交失败', '请选择同业务线同事', 'error');
      return false;
    }
    const oldName = ticket.assignee ? ticket.assignee.displayName : '待分配';
    ticket.assignee = { userId: target.userId, displayName: target.displayName };
    appendOwnershipHistory(ticket, oldName, target.displayName, '同业务线转单', `${currentUser().displayName} 将工单转交给 ${target.displayName}`);
    appendTicketHistory(ticket, ticket.status, `责任人转交给 ${target.displayName}`);
    showToast('转交成功', `工单已转交给 ${target.displayName}`, 'success');
    return true;
  }

  function applyBusinessLineTransfer(ticketId) {
    const ticket = DATA.tickets.find((item) => item.ticketId === ticketId);
    const select = document.querySelector('[name="targetBusinessLine"]');
    if (!ticket || !select) return false;
    const lineCode = String(select.value || '');
    const line = BUSINESS_LINES.find((item) => item.code === lineCode);
    if (!line) {
      showToast('转单失败', '请选择目标业务线', 'error');
      return false;
    }
    const oldLine = businessLineName(ticket.businessLineCode);
    ticket.businessLineCode = line.code;
    ticket.queueId = `queue_${line.code.toLowerCase()}`;
    const target = (DATA.supportAgents || SUPPORT_AGENTS).find((agent) => agent.businessLineCodes.includes(line.code));
    const oldName = ticket.assignee ? ticket.assignee.displayName : '待分配';
    ticket.assignee = target ? { userId: target.userId, displayName: target.displayName } : null;
    appendOwnershipHistory(ticket, oldName, target ? target.displayName : '对应业务线队列', '跨业务线转单', `${currentUser().displayName} 将 ${oldLine} 工单转给 ${line.name}`);
    appendTicketHistory(ticket, 'PENDING_ACCEPTANCE', `转至 ${line.name}，等待对应业务线受理`);
    ticket.status = 'PENDING_ACCEPTANCE';
    ticket.isSuspended = false;
    showToast('转单已提交', `${line.name} 承接能力暂未接入，当前保留为待受理演示`, 'info');
    return true;
  }

  function scrollToTransfer(targetId) {
    const target = document.getElementById(targetId);
    if (target) target.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }

  function saveClassificationFromDrawer(ticket) {
    const form = document.getElementById('classification-form');
    if (!form) return false;
    const values = new FormData(form);
    const managementUnitId = String(values.get('managementUnitId') || '').trim() || null;
    const symptomId = String(values.get('symptomId') || '').trim() || null;
    const reasonId = String(values.get('reasonId') || '').trim() || null;
    const solutionMethodId = String(values.get('solutionMethodId') || '').trim() || null;
    const customReason = String(values.get('customReason') || '').trim() || null;
    const customSolution = String(values.get('customSolution') || '').trim() || null;

    ticket.classification = {
      managementUnitId,
      symptomId,
      reasonId,
      solutionMethodId,
      customReason,
      customSolution
    };
    ticket.updatedAt = nowIso();
    ticket.statusHistory.unshift({
      status: ticket.status,
      occurredAt: ticket.updatedAt,
      operator: currentUser().displayName,
      note: '更新分类信息'
    });
    ticket.auditEvents.unshift({
      action: 'TicketClassificationUpdated',
      occurredAt: ticket.updatedAt,
      actor: currentUser().userId
    });
    showToast('保存成功', '分类信息已更新', 'success');
    return true;
  }

  function submitResolution(ticket) {
    const now = nowIso();
    const detail = ticket.classification;
    if (!detail.managementUnitId || !detail.symptomId || (!detail.solutionMethodId && !detail.customSolution)) {
      showToast('保存失败', 'VALIDATION_ERROR · 解决前请先完成分类并补充解决方法', 'error');
      return false;
    }
    ticket.updatedAt = now;
    ticket.status = 'PENDING_USER_CONFIRM';
    ticket.statusHistory.unshift({
      status: 'PENDING_USER_CONFIRM',
      occurredAt: now,
      operator: currentUser().displayName,
      note: '客服提交解决结果'
    });
    ticket.auditEvents.unshift({
      action: 'TicketResolutionSubmitted',
      occurredAt: now,
      actor: currentUser().userId
    });
    ticket.resolution = {
      summary: detail.customSolution || '已提交标准解决方案',
      method: detail.solutionMethodId || '标准解决方法'
    };
    showToast('提交成功', '解决结果已提交，等待用户确认', 'success');
    return true;
  }

  function reopenTicket(ticket) {
    const now = nowIso();
    if (!['PENDING_USER_CONFIRM', 'RESOLVED', 'CLOSED'].includes(ticket.status)) {
      showToast('无法重开', 'ILLEGAL_STATE_TRANSITION', 'error');
      return false;
    }
    ticket.status = 'REOPENED';
    ticket.updatedAt = now;
    ticket.statusHistory.unshift({
      status: 'REOPENED',
      occurredAt: now,
      operator: currentUser().displayName,
      note: '用户重开工单'
    });
    ticket.auditEvents.unshift({
      action: 'TicketReopened',
      occurredAt: now,
      actor: currentUser().userId
    });
    showToast('工单已重开', 'REOPENED · code=SUCCESS', 'warning');
    return true;
  }

  function confirmTicket(ticket) {
    const now = nowIso();
    if (ticket.status !== 'PENDING_USER_CONFIRM') {
      showToast('无法确认', 'ILLEGAL_STATE_TRANSITION', 'error');
      return false;
    }
    ticket.status = 'RESOLVED';
    ticket.updatedAt = now;
    ticket.statusHistory.unshift({
      status: 'RESOLVED',
      occurredAt: now,
      operator: currentUser().displayName,
      note: '用户确认解决'
    });
    ticket.auditEvents.unshift({
      action: 'TicketUserConfirmed',
      occurredAt: now,
      actor: currentUser().userId
    });
    showToast('已确认', '工单已标记为 RESOLVED', 'success');
    return true;
  }

  function closeTicket(ticket) {
    const now = nowIso();
    if (ticket.status !== 'RESOLVED') {
      showToast('无法关闭', 'ILLEGAL_STATE_TRANSITION', 'error');
      return false;
    }
    ticket.status = 'CLOSED';
    ticket.updatedAt = now;
    ticket.statusHistory.unshift({
      status: 'CLOSED',
      occurredAt: now,
      operator: currentUser().displayName,
      note: '客服关闭工单'
    });
    ticket.auditEvents.unshift({
      action: 'TicketClosed',
      occurredAt: now,
      actor: currentUser().userId
    });
    showToast('已关闭', '工单状态更新为 CLOSED', 'success');
    return true;
  }

  function saveRating(ticket) {
    const score = Number(state.draft.ratingScore || 0);
    const commentField = document.getElementById('ratingComment');
    const comment = commentField ? String(commentField.value || '').trim() : state.draft.ratingComment.trim();
    if (!Number.isInteger(score) || score < 1 || score > 5) {
      showToast('评价失败', 'VALIDATION_ERROR · score 必须为 1-5', 'error');
      return false;
    }
    ticket.rating = {
      score,
      comment
    };
    ticket.auditEvents.unshift({
      action: 'TicketRated',
      occurredAt: nowIso(),
      actor: currentUser().userId
    });
    showToast('评价成功', '工单评价已提交', 'success');
    return true;
  }

  function saveDictItem(mode, itemId) {
    const form = document.getElementById('dict-form');
    if (!form) return false;
    const values = new FormData(form);
    const code = String(values.get('code') || '').trim();
    const name = String(values.get('name') || '').trim();
    const parentId = String(values.get('parentId') || '').trim() || null;
    const sort = Number(values.get('sort') || 10);
    const description = String(values.get('description') || '').trim();

    if (!code || !name) {
      showToast('保存失败', 'VALIDATION_ERROR · code/name 为必填项', 'error');
      return false;
    }

    const dict = DATA.dictionaries[state.activeDictTab];
    if (mode === 'edit') {
      const item = dict.items.find((entry) => entry.itemId === itemId);
      if (!item) {
        showToast('保存失败', 'RESOURCE_NOT_FOUND', 'error');
        return false;
      }
      item.code = code;
      item.name = name;
      item.parentId = parentId;
      item.sort = sort;
      item.description = description;
      item.updatedAt = nowIso();
      item.updatedBy = currentUser().userId;
      item.version += 1;
    } else {
      dict.items.unshift({
        itemId: `dict_${Date.now()}`,
        code,
        name,
        parentId,
        enabled: true,
        sort,
        version: 1,
        description,
        updatedAt: nowIso(),
        updatedBy: currentUser().userId
      });
    }

    showToast('保存成功', mode === 'edit' ? '字典项已更新' : '字典项已新增', 'success');
    return true;
  }

  function disableDictItem(itemId) {
    const form = document.getElementById('disable-form');
    if (!form) return false;
    const values = new FormData(form);
    const reason = String(values.get('reason') || '').trim();
    if (!reason) {
      showToast('保存失败', 'VALIDATION_ERROR · reason 不能为空', 'error');
      return false;
    }
    const dict = DATA.dictionaries[state.activeDictTab];
    const item = dict.items.find((entry) => entry.itemId === itemId);
    if (!item) {
      showToast('保存失败', 'RESOURCE_NOT_FOUND', 'error');
      return false;
    }
    item.enabled = false;
    item.updatedAt = nowIso();
    item.updatedBy = currentUser().userId;
    showToast('已停用', `${item.name} 已停用`, 'warning');
    return true;
  }

  function renderHomeRoute() {
    if (isUserRole()) {
      renderUserHomePage();
      return;
    }
    if (isSupervisorRole()) {
      renderSupervisorHomePage();
      return;
    }
    renderSupportHomePage();
  }

  function renderTicketRoute() {
    if (isUserRole()) {
      renderUserTicketsPage();
      return;
    }
    if (isSupervisorRole()) {
      renderSupervisorTicketsPage();
      return;
    }
    renderSupportTicketsPage();
  }

  function render() {
    if (state.route === 'HOME') {
      renderHomeRoute();
      return;
    }

    if (state.route === 'TICKET') {
      renderTicketRoute();
      return;
    }

    if (state.route === 'CONFIG') {
      renderConfigPage();
      return;
    }

    state.route = currentProfile().defaultRoute || 'HOME';
    render();
  }

  function openTicket(ticketId) {
    state.activeTicketId = ticketId;
    state.overlay = { type: 'ticket', ticketId };
    render();
  }

  function openDictForm(mode, itemId) {
    state.overlay = { type: 'dict-form', mode, itemId };
    render();
  }

  function openDisableDialog(itemId) {
    state.overlay = { type: 'dict-disable', itemId };
    render();
  }

  function closeOverlay() {
    state.overlay = null;
    overlayRoot.innerHTML = '';
  }

  function handleClick(event) {
    const target = event.target.closest('[data-action]');
    if (!target) return;

    const action = target.dataset.action;

    if (action === 'nav') {
      state.route = target.dataset.route;
      state.overlay = null;
      render();
      return;
    }

    if (action === 'close-itsm') {
      if (window.parent && window.parent !== window && window.parent.postMessage) {
        window.parent.postMessage({ type: 'ITSM_CLOSE' }, '*');
        return;
      }
      showToast('工单系统', '当前已处于独立演示视图', 'info');
      return;
    }

    if (action === 'reload-permissions') {
      state.loading.permissions = true;
      render();
      delay(180).then(() => {
        state.loading.permissions = false;
        showToast('刷新完成', `permissionsVersion = ${currentProfile().permissions.data.permissionsVersion}`, 'success');
        render();
      });
      return;
    }

    if (action === 'new-session') {
      const now = nowIso();
      const session = {
        sessionId: `ses_${Date.now()}`,
        tenantId: currentTenant().tenantId,
        userId: currentUser().userId,
        channel: 'WORKBENCH',
        subject: '新会话',
        status: 'ACTIVE',
        summary: '你可以直接输入问题。',
        ticketId: null,
        createdAt: now,
        lastMessageAt: now,
        messages: [
          {
            messageId: `msg_${Date.now()}`,
            senderType: 'ASSISTANT',
            content: '你好，我可以帮你继续处理问题。请直接告诉我需要咨询的内容。',
            createdAt: now
          }
        ]
      };
      DATA.sessions.unshift(session);
      state.activeSessionId = session.sessionId;
      state.activeTicketId = null;
      state.draft.message = '';
      showToast('新会话已创建', '可以直接开始聊天', 'success');
      render();
      return;
    }

    if (action === 'handoff') {
      const session = getCurrentSession();
      session.status = 'HANDOFF_PENDING';
      session.summary = '已提交转人工请求，等待工单生成或队列分流。';
      session.lastMessageAt = nowIso();
      showToast('已转人工', '当前会话进入 HANDOFF_PENDING', 'info');
      render();
      return;
    }

    if (action === 'create-ticket') {
      createTicketFromCurrentSession();
      render();
      return;
    }

    if (action === 'select-session') {
      state.activeSessionId = target.dataset.sessionId;
      const session = getCurrentSession();
      state.activeTicketId = session.ticketId || state.activeTicketId;
      render();
      return;
    }

    if (action === 'support-select-ticket') {
      state.activeTicketId = target.dataset.ticketId;
      render();
      return;
    }

    if (action === 'ticket-view') {
      state.filters.ticketView = target.dataset.view;
      render();
      return;
    }

    if (action === 'supervisor-period') {
      state.filters.supervisorPeriod = target.dataset.period || 'month';
      render();
      return;
    }

    if (action === 'apply-ticket-filter') {
      render();
      return;
    }

    if (action === 'service-desk-tab') {
      state.filters.serviceDeskTab = target.dataset.tab || 'pending';
      render();
      return;
    }

    if (action === 'support-view') {
      state.filters.supportTicketView = target.dataset.view || 'all';
      state.filters.supportPage = 1;
      render();
      return;
    }

    if (action === 'support-view-all') {
      state.filters.supportTicketView = target.dataset.view || 'all';
      state.filters.supportPage = 1;
      state.route = 'TICKET';
      state.overlay = null;
      render();
      return;
    }

    if (action === 'support-page') {
      state.filters.supportPage = Number(target.dataset.page || 1);
      render();
      return;
    }

    if (action === 'apply-support-filter') {
      state.filters.supportPage = 1;
      render();
      return;
    }

    if (action === 'reset-support-filter') {
      state.filters.supportTicketNo = '';
      state.filters.supportRequester = '';
      state.filters.supportTicketView = 'all';
      state.filters.supportPage = 1;
      render();
      return;
    }

    if (action === 'suggestion') {
      const composer = document.querySelector('[name="message"]');
      if (composer) {
        composer.value = target.dataset.text || '';
        state.draft.message = composer.value;
        composer.focus();
      }
      return;
    }

    if (action === 'composer-tool') {
      const tool = target.dataset.tool || 'tool';
      const hints = {
        attach: '附件入口暂未接入真实上传服务',
        file: '文件入口暂未接入真实上传服务',
        emoji: '表情入口暂未接入',
        screenshot: '截图入口暂未接入'
      };
      showToast('占位入口', hints[tool] || '该功能暂未接入', 'info');
      return;
    }

    if (action === 'open-ticket') {
      openTicket(target.dataset.ticketId);
      return;
    }

    if (action === 'close-overlay') {
      closeOverlay();
      return;
    }

    if (action === 'accept-ticket') {
      const ticketId = target.dataset.ticketId;
      const ticket = getTicketById(ticketId);
      if (!hasPermission('ticket:accept')) {
        showToast('无权操作', 'ROLE_FORBIDDEN', 'error');
        return;
      }
      if (updateTicketStatus(ticketId, 'IN_PROGRESS', '客服受理', 'TicketAccepted')) {
        const accepted = getTicketById(ticketId);
        if (accepted) {
          accepted.assignee = { userId: currentUser().userId, displayName: currentUser().displayName };
          accepted.slaStartedAt = nowIso();
          accepted.slaAccumulatedMs = 0;
          accepted.isSuspended = false;
          accepted.slaPausedAt = null;
          appendOwnershipHistory(accepted, ticket && ticket.assignee ? ticket.assignee.displayName : '客服队列', currentUser().displayName, '受理', '成为当前工单责任人');
        }
        state.overlay = { type: 'ticket', ticketId };
        render();
      }
      return;
    }

    if (action === 'apply-ticket-status') {
      const ticketId = target.dataset.ticketId;
      if (!hasPermission('ticket:classify')) {
        showToast('无权操作', 'ROLE_FORBIDDEN', 'error');
        return;
      }
      if (applyTicketStatusFromDrawer(ticketId)) {
        state.overlay = { type: 'ticket', ticketId };
        render();
      }
      return;
    }

    if (action === 'toggle-suspend-ticket') {
      const ticketId = target.dataset.ticketId;
      if (toggleTicketSuspend(ticketId)) {
        state.overlay = { type: 'ticket', ticketId };
        render();
      }
      return;
    }

    if (action === 'scroll-to-transfer') {
      scrollToTransfer(target.dataset.target);
      return;
    }

    if (action === 'apply-same-line-transfer') {
      const ticketId = target.dataset.ticketId;
      if (applySameLineTransfer(ticketId)) {
        state.overlay = { type: 'ticket', ticketId };
        render();
      }
      return;
    }

    if (action === 'apply-business-line-transfer') {
      const ticketId = target.dataset.ticketId;
      if (applyBusinessLineTransfer(ticketId)) {
        state.overlay = { type: 'ticket', ticketId };
        render();
      }
      return;
    }

    if (action === 'save-classification') {
      const ticket = getCurrentTicket();
      if (!ticket || !hasPermission('ticket:classify')) {
        showToast('无权操作', 'ROLE_FORBIDDEN', 'error');
        return;
      }
      if (saveClassificationFromDrawer(ticket)) {
        ticket.updatedAt = nowIso();
        ticket.auditEvents.unshift({
          action: 'TicketClassificationUpdated',
          occurredAt: ticket.updatedAt,
          actor: currentUser().userId
        });
        render();
      }
      return;
    }

    if (action === 'resolve-ticket') {
      const ticket = getCurrentTicket();
      if (!ticket || !hasPermission('ticket:resolve')) {
        showToast('无权操作', 'ROLE_FORBIDDEN', 'error');
        return;
      }
      if (submitResolution(ticket)) {
        render();
      }
      return;
    }

    if (action === 'confirm-ticket') {
      const ticket = getCurrentTicket();
      if (!ticket) return;
      if (confirmTicket(ticket)) {
        render();
      }
      return;
    }

    if (action === 'close-ticket') {
      const ticket = getCurrentTicket();
      if (!ticket || !hasPermission('ticket:close')) {
        showToast('无权操作', 'ROLE_FORBIDDEN', 'error');
        return;
      }
      if (closeTicket(ticket)) {
        render();
      }
      return;
    }

    if (action === 'reopen-ticket') {
      const ticket = getCurrentTicket();
      if (!ticket || !hasPermission('ticket:reopen')) {
        showToast('无权操作', 'ROLE_FORBIDDEN', 'error');
        return;
      }
      if (reopenTicket(ticket)) {
        render();
      }
      return;
    }

    if (action === 'open-rating') {
      const ticket = getCurrentTicket();
      if (!ticket) return;
      if (['RESOLVED', 'CLOSED'].includes(ticket.status)) {
        state.draft.ratingScore = ticket.rating ? ticket.rating.score : 0;
        state.draft.ratingComment = ticket.rating ? ticket.rating.comment || '' : '';
        render();
      }
      return;
    }

    if (action === 'set-rating') {
      state.draft.ratingScore = Number(target.dataset.score || 0);
      render();
      return;
    }

    if (action === 'submit-rating') {
      const ticket = getCurrentTicket();
      if (!ticket || !hasPermission('ticket:rating')) {
        showToast('无权操作', 'ROLE_FORBIDDEN', 'error');
        return;
      }
      if (saveRating(ticket)) {
        render();
      }
      return;
    }

    if (action === 'dict-tab') {
      state.activeDictTab = target.dataset.tab;
      render();
      return;
    }

    if (action === 'create-dict') {
      openDictForm('create', null);
      return;
    }

    if (action === 'open-edit-dict') {
      openDictForm('edit', target.dataset.itemId);
      return;
    }

    if (action === 'disable-dict') {
      openDisableDialog(target.dataset.itemId);
      return;
    }

    if (action === 'role-select') {
      state.activeRoleId = target.dataset.roleId;
      render();
      return;
    }

    if (action === 'refresh-config') {
      showToast('刷新完成', '配置数据已重新渲染', 'success');
      render();
      return;
    }
  }

  function handleSubmit(event) {
    const form = event.target;

    if (form.id === 'chat-form') {
      event.preventDefault();
      const formData = new FormData(form);
      const text = String(formData.get('message') || '').trim();
      if (!text) {
        showToast('发送失败', 'VALIDATION_ERROR · message 不能为空', 'error');
        return;
      }
      const session = getCurrentSession();
      const now = nowIso();
      session.messages.push(
        {
          messageId: `msg_${Date.now()}`,
          senderType: 'USER',
          content: text,
          createdAt: now
        },
        {
          messageId: `msg_${Date.now() + 1}`,
          senderType: 'ASSISTANT',
          content: '收到，我会结合当前上下文继续帮你整理。需要转人工时可以直接点击转人工。',
          createdAt: now
        }
      );
      session.summary = text.slice(0, 42);
      session.lastMessageAt = now;
      state.draft.message = '';
      form.reset();
      showToast('已发送', '消息已写入当前会话', 'success');
      render();
      return;
    }

    if (form.id === 'dict-form') {
      event.preventDefault();
      const overlay = state.overlay;
      if (!overlay || overlay.type !== 'dict-form') return;
      if (saveDictItem(overlay.mode, overlay.itemId)) {
        state.overlay = null;
        render();
      }
      return;
    }

    if (form.id === 'disable-form') {
      event.preventDefault();
      const overlay = state.overlay;
      if (!overlay || overlay.type !== 'dict-disable') return;
      if (disableDictItem(overlay.itemId)) {
        state.overlay = null;
        render();
      }
      return;
    }
  }

  function handleInput(event) {
    const target = event.target;
    if (target.name === 'message') {
      state.draft.message = target.value;
      return;
    }
    if (target.id === 'ratingComment') {
      state.draft.ratingComment = target.value;
      return;
    }
    if (target.dataset && target.dataset.bind) {
      state.filters[target.dataset.bind] = target.value;
      if (['sessionKeyword', 'ticketKeyword', 'supportTicketNo', 'supportRequester', 'ticketStatus', 'ticketPriority', 'dictKeyword', 'roleKeyword'].includes(target.dataset.bind)) {
        if (['supportTicketNo', 'supportRequester'].includes(target.dataset.bind)) state.filters.supportPage = 1;
        render();
      }
    }
  }

  document.addEventListener('click', handleClick);
  document.addEventListener('submit', handleSubmit);
  document.addEventListener('input', handleInput);
  document.addEventListener('change', handleInput);
  normalizeSupportDemoData();

  function renderTicketDrawerData() {
    return getCurrentTicket();
  }

  function renderOverlayWrapper() {
    if (!state.overlay) {
      overlayRoot.innerHTML = '';
      return;
    }
    if (state.overlay.type === 'ticket') {
      const ticket = DATA.tickets.find((item) => item.ticketId === state.overlay.ticketId) || renderTicketDrawerData();
      if (ticket) renderTicketDrawer(ticket);
      return;
    }
    if (state.overlay.type === 'dict-form') {
      renderDictModal(state.overlay.mode, state.overlay.itemId);
      return;
    }
    if (state.overlay.type === 'dict-disable') {
      renderDisableModal(state.overlay.itemId);
    }
  }

  function renderWithOverlay(pageRenderer) {
    pageRenderer();
    renderOverlayWrapper();
  }

  const originalShell = shell;
  shell = function (pageHtml) {
    originalShell(pageHtml);
    renderOverlayWrapper();
  };

  function renderStart() {
    if (!state.loggedIn) {
      state.loggedIn = true;
    }
    if (state.route === 'HOME') {
      if (isUserRole()) {
        renderUserHomePage();
      } else if (isSupervisorRole()) {
        renderSupervisorHomePage();
      } else {
        renderSupportHomePage();
      }
      return;
    }
    if (state.route === 'TICKET') {
      if (isUserRole()) {
        renderUserTicketsPage();
      } else if (isSupervisorRole()) {
        renderSupervisorTicketsPage();
      } else {
        renderSupportTicketsPage();
      }
      return;
    }
    if (state.route === 'CONFIG') {
      renderConfigPage();
      return;
    }
    state.route = currentProfile().defaultRoute || 'HOME';
    renderStart();
  }

  render = renderStart;

  function refreshCurrentView() {
    renderStart();
    renderOverlayWrapper();
  }

  render = refreshCurrentView;

  function renderInitial() {
    startSlaClock();
    renderStart();
    renderOverlayWrapper();
  }

  render = renderInitial;

  render();
})();
