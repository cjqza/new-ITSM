(function () {
  'use strict';

  const DATA = window.ITSM_DEMO_DATA;
  const appRoot = document.getElementById('app');
  const toastRoot = document.getElementById('toast-root');
  const session = {
    accessToken: null,
    refreshToken: null,
    user: null,
    tenant: null,
    roles: []
  };

  const remoteData = {
    sessions: [],
    tickets: []
  };

  const STATUS_LABELS = {
    NEW: '新建',
    PENDING_ACCEPTANCE: '待受理',
    IN_PROGRESS: '处理中',
    PENDING_USER_CONFIRM: '待确认',
    RESOLVED: '已解决',
    CLOSED: '已关闭',
    REOPENED: '已重开'
  };

  const state = {
    loggedIn: false,
    activeRole: 'USER',
    actorUserId: DATA.profiles.user.login.data.user.userId,
    actorDisplayName: DATA.profiles.user.login.data.user.displayName,
    actorDepartment: DATA.profiles.user.login.data.user.departmentName,
    view: 'MESSAGES',
    chat: { type: 'ASSISTANT', id: 'assistant' },
    assistantDraft: '',
    colleagueDraft: '',
    contactKeyword: '',
    portalSearch: '',
    assistantCategory: '',
    profileOpen: false,
    handoffOpen: false,
    handoffSuccess: null,
    handoff: {
      title: '',
      description: '',
      category: '操作系统问题',
      priority: 'MEDIUM'
    },
    oaDraft: {
      applicant: '',
      department: '',
      approvalType: 'OA-ITSM-PERM',
      reason: ''
    },
    oaRecords: [],
    whaleFilter: {
      range: '30',
      customStart: '',
      customEnd: '',
      appliedRange: '30'
    },
    itsmApplication: {
      status: 'NONE',
      reason: '',
      requestType: 'ITSM_ACCESS'
    },
    myRequests: [],
    approvals: [],
    contacts: [],
    contactsPage: 1,
    contactsPageSize: 10,
    colleagueConversations: [],
    employees: [],
    employeeEditingId: null,
    employeeForm: { departmentName: '', phone: '', email: '' },
    ticketDetail: null,
    ticketAction: 'none',
    reopenReason: '',
    ratingScore: 0,
    ratingComment: ''
  };

  function escapeHtml(value) {
    return String(value == null ? '' : value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function nowIso() {
    return new Date().toISOString();
  }

  function formatDate(iso) {
    if (!iso) return '-';
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) return String(iso);
    return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }).replace(/\//g, '-');
  }

  function formatDateTime(iso) {
    if (!iso) return '-';
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) return String(iso);
    return date.toLocaleString('zh-CN', { hour12: false, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).replace(/\//g, '-');
  }

  function showToast(title, message, type = 'info') {
    const node = document.createElement('div');
    node.className = `toast ${type}`;
    node.innerHTML = `<strong>${escapeHtml(title)}</strong><p>${escapeHtml(message)}</p>`;
    toastRoot.appendChild(node);
    window.setTimeout(() => {
      node.style.opacity = '0';
      node.style.transform = 'translateY(4px)';
      node.style.transition = 'opacity 0.2s ease, transform 0.2s ease';
    }, 2400);
    window.setTimeout(() => node.remove(), 2680);
  }

  function userProfile() {
    return session.user || DATA.profiles.user.login.data.user;
  }

  function currentUserId() {
    return userProfile().userId;
  }

  function currentTenant() {
    return session.tenant || DATA.tenant;
  }

  function roleFromLogin(roles) {
    const roleList = roles || [];
    if (roleList.includes('SUPPORT_ADMIN')) return 'ADMIN';
    if (roleList.includes('SUPPORT_AGENT')) return 'SUPPORT';
    return 'USER';
  }

  function roleLabel() {
    const roleList = session.roles || [];
    const labels = {
      USER: '普通用户',
      SUPPORT_AGENT: '普通客服',
      SUPPORT_ADMIN: '管理员客服',
      SUPERVISOR: '主管/质检'
    };
    if (roleList.length) return roleList.map((role) => labels[role] || role).join(' / ');
    return activeProfile().label;
  }

  function authHeaders() {
    return {
      'Content-Type': 'application/json',
      'X-Tenant-Id': currentTenant().tenantId,
      'Authorization': 'Bearer ' + (session.accessToken || '')
    };
  }

  async function apiGet(path) {
    const response = await fetch(path, { headers: authHeaders() });
    const body = await response.json().catch(() => ({}));
    if (!response.ok || body.code !== 'SUCCESS') {
      throw new Error(body.message || `HTTP ${response.status}`);
    }
    return body.data;
  }

  function mapTicket(item) {
    return {
      ticketId: item.ticketId,
      ticketNo: item.ticketNo,
      tenantId: currentTenant().tenantId,
      requester: { userId: currentUserId(), displayName: userProfile().displayName, departmentName: userProfile().departmentName },
      title: item.title,
      description: '',
      source: 'MANUAL',
      status: item.status,
      priority: item.priority,
      businessLineCode: item.businessLineCode,
      classification: null,
      assignee: item.assigneeId ? { userId: item.assigneeId, displayName: item.assigneeId } : null,
      conversation: null,
      statusHistory: [],
      auditEvents: [],
      resolution: null,
      rating: null,
      createdAt: item.updatedAt,
      updatedAt: item.updatedAt
    };
  }

  function mapSession(item) {
    return {
      sessionId: item.sessionId,
      tenantId: currentTenant().tenantId,
      userId: item.userId,
      channel: item.channel,
      subject: item.subject || '未命名会话',
      status: item.status,
      summary: item.summary || '',
      ticketId: item.ticketId || null,
      lastMessageAt: item.lastMessageAt || item.createdAt,
      messages: []
    };
  }

  async function loadSessionMessages(sessionItem) {
    try {
      const detail = await apiGet('/api/v1/conversations/sessions/' + encodeURIComponent(sessionItem.sessionId));
      sessionItem.messages = (detail.messages && detail.messages.items ? detail.messages.items : []).map((message) => ({
        messageId: message.messageId,
        senderType: message.senderType,
        content: message.content,
        createdAt: message.createdAt
      }));
      sessionItem.summary = detail.summary || sessionItem.summary;
      sessionItem.ticketId = detail.ticketId || sessionItem.ticketId;
      sessionItem.status = detail.status || sessionItem.status;
    } catch (error) {
      sessionItem.messages = [];
    }
  }

  async function loadRemoteData() {
    try {
      const [ticketPage, sessionPage] = await Promise.all([
        apiGet('/api/v1/tickets?page=1&pageSize=50'),
        apiGet('/api/v1/conversations/sessions?page=1&pageSize=50')
      ]);
      remoteData.tickets = (ticketPage.items || []).map(mapTicket);
      remoteData.sessions = (sessionPage.items || []).map(mapSession);
      if (remoteData.sessions.length) {
        await loadSessionMessages(remoteData.sessions[0]);
      }
      await Promise.all([loadMyRequests(), loadApprovals(), loadContacts(), loadEmployees()]);
    } catch (error) {
      remoteData.tickets = [];
      remoteData.sessions = [];
    }
  }

  async function loadMyRequests() {
    if (!session.accessToken) return;
    try {
      state.myRequests = await apiGet('/api/v1/permissions/requests/my') || [];
    } catch (error) {
      state.myRequests = [];
    }
  }

  async function loadApprovals() {
    if (!hasAdminRole()) return;
    try {
      state.approvals = await apiGet('/api/v1/admin/permissions/requests') || [];
    } catch (error) {
      state.approvals = [];
    }
  }

  async function loadContacts() {
    if (!session.accessToken) return;
    try {
      state.contacts = await apiGet('/api/v1/contacts') || [];
    } catch (error) {
      state.contacts = [];
    }
  }

  async function loadEmployees() {
    if (!hasAdminRole()) return;
    try {
      state.employees = await apiGet('/api/v1/admin/employees') || [];
    } catch (error) {
      state.employees = [];
    }
  }

  function startEditEmployee(userId) {
    const emp = (state.employees || []).find((e) => e.userId === userId);
    state.employeeEditingId = userId;
    state.employeeForm = {
      departmentName: emp ? (emp.departmentName || '') : '',
      phone: emp ? (emp.phone || '') : '',
      email: emp ? (emp.email || '') : ''
    };
    render();
  }

  function cancelEditEmployee() {
    state.employeeEditingId = null;
    state.employeeForm = { departmentName: '', phone: '', email: '' };
    render();
  }

  async function saveEmployee(userId) {
    try {
      await apiPatch('/api/v1/admin/employees/' + encodeURIComponent(userId), {
        departmentName: state.employeeForm.departmentName || null,
        phone: state.employeeForm.phone || null,
        email: state.employeeForm.email || null
      });
      state.employeeEditingId = null;
      state.employeeForm = { departmentName: '', phone: '', email: '' };
      await loadEmployees();
      render();
      showToast('已保存', '员工账号已更新', 'success');
    } catch (error) {
      showToast('保存失败', error.message, 'error');
    }
  }

  async function submitPermissionRequest(requestType, reason) {
    try {
      await apiPost('/api/v1/permissions/requests', { requestType, reason: reason || null });
      state.itsmApplication.status = 'NONE';
      state.itsmApplication.reason = '';
      await loadMyRequests();
      render();
      showToast('已提交', '权限申请已提交，等待管理员审批', 'success');
    } catch (error) {
      showToast('提交失败', error.message, 'error');
    }
  }

  async function approveRequest(requestId) {
    try {
      await apiPost('/api/v1/admin/permissions/requests/' + encodeURIComponent(requestId) + '/approve', {});
      await loadApprovals();
      render();
      showToast('已批准', '已授予对应权限', 'success');
    } catch (error) {
      showToast('操作失败', error.message, 'error');
    }
  }

  async function rejectRequest(requestId) {
    try {
      await apiPost('/api/v1/admin/permissions/requests/' + encodeURIComponent(requestId) + '/reject', {});
      await loadApprovals();
      render();
      showToast('已驳回', '申请已驳回', 'warning');
    } catch (error) {
      showToast('操作失败', error.message, 'error');
    }
  }

  async function apiPost(path, payload) {
    const response = await fetch(path, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify(payload)
    });
    const body = await response.json().catch(() => ({}));
    if (!response.ok || body.code !== 'SUCCESS') {
      throw new Error(body.message || `HTTP ${response.status}`);
    }
    return body.data;
  }

  async function apiPatch(path, payload) {
    const response = await fetch(path, {
      method: 'PATCH',
      headers: authHeaders(),
      body: JSON.stringify(payload)
    });
    const body = await response.json().catch(() => ({}));
    if (!response.ok || body.code !== 'SUCCESS') {
      throw new Error(body.message || `HTTP ${response.status}`);
    }
    return body.data;
  }

  function newClientMessageId() {
    return 'cli_' + Date.now() + '_' + Math.random().toString(36).slice(2, 10);
  }

  async function sendAssistantMessage(text) {
    try {
      let sessionId = currentSession() ? currentSession().sessionId : null;
      if (!sessionId) {
        const created = await apiPost('/api/v1/conversations/sessions', {
          channel: 'WORKBENCH',
          subject: text.slice(0, 40)
        });
        sessionId = created.sessionId;
      }
      await apiPost('/api/v1/conversations/sessions/' + encodeURIComponent(sessionId) + '/messages', {
        clientMessageId: newClientMessageId(),
        content: text
      });
      if (/转人工|人工客服|人工服务/.test(text)) {
        state.handoff.title = state.handoff.title || text.slice(0, 40);
        state.handoff.description = text;
        state.handoffOpen = true;
      }
      await loadRemoteData();
      render();
    } catch (error) {
      showToast('发送失败', error.message, 'error');
    }
  }

  async function endCurrentSession() {
    const session = currentSession();
    if (!session) {
      showToast('操作失败', '当前没有会话', 'error');
      return;
    }
    try {
      await apiPost('/api/v1/conversations/sessions/' + encodeURIComponent(session.sessionId) + '/end', {});
      await loadRemoteData();
      render();
      showToast('已结束', '会话已归档并清理缓存', 'success');
    } catch (error) {
      showToast('操作失败', error.message, 'error');
    }
  }

  async function loadColleagueMessages(conversation) {
    try {
      const list = await apiGet('/api/v1/colleagues/messages?peerUserId=' + encodeURIComponent(conversation.userId));
      conversation.messages = (list || []).map((m) => ({
        messageId: String(m.id),
        senderType: m.fromUserId === currentUserId() ? 'USER' : 'CONTACT',
        senderName: m.fromUserId === currentUserId() ? userProfile().displayName : conversation.displayName,
        content: m.content,
        createdAt: m.createdAt
      }));
      const isOpen = state.view === 'MESSAGES' && state.chat.type === 'COLLEAGUE' && state.chat.id === conversation.conversationId;
      if (isOpen) {
        conversation.unreadCount = 0;
        conversation.lastReadMessageId = conversation.messages.length ? Number(conversation.messages[conversation.messages.length - 1].messageId) : 0;
      } else {
        const lastRead = conversation.lastReadMessageId || 0;
        const unread = conversation.messages.filter((m) => Number(m.messageId) > lastRead && m.senderType === 'CONTACT');
        conversation.unreadCount = unread.length;
      }
      if (conversation.messages.length) {
        const last = conversation.messages[conversation.messages.length - 1];
        conversation.lastMessage = last.content;
        conversation.lastMessageAt = last.createdAt;
      }
    } catch (error) {
      conversation.messages = [];
    }
  }

  function openColleagueConversation(conversation) {
    conversation.lastReadMessageId = conversation.messages.length ? Number(conversation.messages[conversation.messages.length - 1].messageId) : 0;
    conversation.unreadCount = 0;
    state.view = 'MESSAGES';
    state.chat = { type: 'COLLEAGUE', id: conversation.conversationId };
  }

  async function sendColleagueMessage(conversation, content) {
    try {
      await apiPost('/api/v1/colleagues/messages', { toUserId: conversation.userId, content });
      state.colleagueDraft = '';
      await loadColleagueMessages(conversation);
      render();
    } catch (error) {
      showToast('发送失败', error.message, 'error');
    }
  }

  async function createHandoffTicket() {
    const session = currentSession();
    const title = state.handoff.title || (session && session.subject) || '转人工服务';
    const description = state.handoff.description || '用户请求转人工处理';
    try {
      const created = await apiPost('/api/v1/tickets', {
        source: 'AGENT_HANDOFF',
        sessionId: session ? session.sessionId : null,
        title: title,
        description: description,
        businessLineCode: 'IT_SUPPORT',
        priority: state.handoff.priority || 'MEDIUM'
      });
      state.handoffOpen = false;
      state.handoffSuccess = created.ticketId;
      await loadRemoteData();
      render();
      showToast('转人工成功', `已生成工单 ${created.ticketNo}`, 'success');
    } catch (error) {
      showToast('转人工失败', error.message, 'error');
    }
  }

  function mapTicketDetail(detail) {
    return {
      ticketId: detail.ticketId,
      ticketNo: detail.ticketNo,
      title: detail.title,
      description: detail.description,
      status: detail.status,
      priority: detail.priority,
      businessLineCode: detail.businessLineCode,
      assigneeName: detail.assignee ? detail.assignee.displayName : '待分配',
      statusHistory: detail.statusHistory || [],
      resolution: detail.resolution,
      rating: detail.rating,
      createdAt: detail.createdAt,
      updatedAt: detail.updatedAt
    };
  }

  function resetTicketAction() {
    state.ticketAction = 'none';
    state.reopenReason = '';
    state.ratingScore = 0;
    state.ratingComment = '';
  }

  async function openTicketDetail(ticketId) {
    try {
      const detail = await apiGet('/api/v1/tickets/' + encodeURIComponent(ticketId));
      state.ticketDetail = mapTicketDetail(detail);
      resetTicketAction();
      render();
    } catch (error) {
      showToast('加载失败', error.message, 'error');
    }
  }

  function closeTicketDetail() {
    state.ticketDetail = null;
    resetTicketAction();
    render();
  }

  async function confirmTicket(ticketId) {
    try {
      await apiPost('/api/v1/tickets/' + encodeURIComponent(ticketId) + '/confirm', {});
      await loadRemoteData();
      closeTicketDetail();
      showToast('已确认解决', '工单已解决', 'success');
    } catch (error) {
      showToast('操作失败', error.message, 'error');
    }
  }

  async function reopenTicket(ticketId) {
    const reason = state.reopenReason.trim();
    if (!reason) {
      showToast('请输入重开原因', '', 'error');
      return;
    }
    try {
      await apiPost('/api/v1/tickets/' + encodeURIComponent(ticketId) + '/reopen', { reason });
      await loadRemoteData();
      closeTicketDetail();
      showToast('已重开', '工单已重新打开', 'success');
    } catch (error) {
      showToast('操作失败', error.message, 'error');
    }
  }

  async function submitRating(ticketId) {
    if (state.ratingScore < 1 || state.ratingScore > 5) {
      showToast('请选择评分', '评分范围为 1-5 分', 'error');
      return;
    }
    try {
      await apiPost('/api/v1/tickets/' + encodeURIComponent(ticketId) + '/rating', {
        score: state.ratingScore,
        comment: state.ratingComment || null,
        tags: []
      });
      await loadRemoteData();
      closeTicketDetail();
      showToast('评价已提交', '感谢反馈', 'success');
    } catch (error) {
      showToast('操作失败', error.message, 'error');
    }
  }

  function renderTicketDetailOverlay() {
    const d = state.ticketDetail;
    if (!d) return '';
    const historyRows = (d.statusHistory || []).map((h) => `
      <div class="td-history-row">
        <span class="status-tag ${statusClass(h.status)}">${escapeHtml(statusLabel(h.status))}</span>
        <span class="td-history-note">${escapeHtml(h.note || '')}</span>
        <span class="muted">${escapeHtml(h.operator || '')} · ${escapeHtml(formatDateTime(h.occurredAt))}</span>
      </div>
    `).join('') || '<div class="empty-state">暂无状态流转</div>';

    let actionArea = '';
    if (d.status === 'PENDING_USER_CONFIRM') {
      if (state.ticketAction === 'reopen') {
        actionArea = `
          <div class="td-action-form">
            <input class="text-input" data-bind="reopenReason" placeholder="重开原因" value="${escapeHtml(state.reopenReason)}">
            <button class="primary-button" data-action="submit-reopen" data-ticket-id="${escapeHtml(d.ticketId)}">提交重开</button>
            <button class="ghost-button" data-action="cancel-ticket-action">取消</button>
          </div>`;
      } else {
        actionArea = `
          <div class="td-actions">
            <button class="primary-button" data-action="confirm-ticket" data-ticket-id="${escapeHtml(d.ticketId)}">确认解决</button>
            <button class="ghost-button" data-action="reopen-ticket" data-ticket-id="${escapeHtml(d.ticketId)}">重开</button>
          </div>`;
      }
    } else if (['RESOLVED', 'CLOSED'].includes(d.status) && !d.rating) {
      if (state.ticketAction === 'rating') {
        const stars = [1, 2, 3, 4, 5].map((score) => `<button class="rating-star ${state.ratingScore === score ? 'selected' : ''}" data-action="set-rating" data-score="${score}">${score}</button>`).join('');
        actionArea = `
          <div class="td-action-form">
            <div class="rating-stars">${stars}</div>
            <input class="text-input" data-bind="ratingComment" placeholder="评价内容（可选）" value="${escapeHtml(state.ratingComment)}">
            <button class="primary-button" data-action="submit-rating" data-ticket-id="${escapeHtml(d.ticketId)}">提交评价</button>
            <button class="ghost-button" data-action="cancel-ticket-action">取消</button>
          </div>`;
      } else {
        actionArea = `<div class="td-actions"><button class="primary-button" data-action="open-rating" data-ticket-id="${escapeHtml(d.ticketId)}">评价</button></div>`;
      }
    } else if (d.rating) {
      actionArea = `<div class="td-actions"><span class="muted">已评价 ${escapeHtml(String(d.rating.score))} 分</span></div>`;
    }

    return `
      <div class="popover-backdrop" data-action="close-ticket-detail"></div>
      <section class="ticket-detail-popover">
        <div class="popover-head"><div><h2>${escapeHtml(d.ticketNo)} · ${escapeHtml(d.title)}</h2><p>${escapeHtml(statusLabel(d.status))} · ${escapeHtml(d.priority || '-')} · ${escapeHtml(d.businessLineCode || '-')}</p></div><button class="close-button" data-action="close-ticket-detail">×</button></div>
        <div class="td-body">
          <p class="td-desc">${escapeHtml(d.description || '无描述')}</p>
          <div class="td-meta"><span>处理人</span><strong>${escapeHtml(d.assigneeName)}</strong></div>
          ${d.resolution ? `<div class="td-meta"><span>解决方案</span><strong>${escapeHtml(d.resolution.summary || '')}</strong></div>` : ''}
          <div class="td-history"><h3>状态流转</h3>${historyRows}</div>
        </div>
        ${actionArea ? `<div class="td-footer">${actionArea}</div>` : ''}
      </section>`;
  }

  function activeProfile() {
    if (state.activeRole === 'SUPPORT') return DATA.profiles.support;
    if (state.activeRole === 'ADMIN') return DATA.profiles.admin;
    return DATA.profiles.user;
  }

  function hasRolePermission(code) {
    return (activeProfile().permissions.data.permissions || []).includes(code);
  }

  function isSupportAgent() {
    return state.activeRole === 'SUPPORT' || state.activeRole === 'ADMIN';
  }

  function hasItsmAccess() {
    const roles = session.roles || [];
    return roles.includes('SUPPORT_AGENT') || roles.includes('SUPPORT_ADMIN') || roles.includes('SUPERVISOR');
  }

  function hasAdminRole() {
    const roles = session.roles || [];
    return roles.includes('SUPPORT_ADMIN');
  }

  function userSessions() {
    return remoteData.sessions;
  }

  function userTickets() {
    return remoteData.tickets;
  }

  function historyTickets() {
    const now = new Date();
    const cutoff = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
    return userTickets().filter((ticket) => {
      const date = new Date(ticket.createdAt || ticket.updatedAt);
      return !Number.isNaN(date.getTime()) && date >= cutoff;
    });
  }

  function allWorkloadTickets() {
    return DATA.tickets || [];
  }

  function currentSession() {
    return userSessions()[0] || null;
  }

  function statusLabel(status) {
    return STATUS_LABELS[status] || status || '-';
  }

  function statusClass(status) {
    return `status-${status}`;
  }

  function render() {
    if (!state.loggedIn) {
      renderLogin();
      return;
    }
    const views = {
      MESSAGES: renderMessages,
      HISTORY: renderHistory,
      CONTACTS: renderContacts,
      WORKSPACE: renderWorkspace,
      ITSM: renderItsm,
      WHALE: renderWhale,
      APPROVALS: renderApprovals,
      EMPLOYEES: renderEmployees
    };
    (views[state.view] || renderMessages)();
  }

  function renderLogin() {
    appRoot.innerHTML = `
      <div class="login-screen">
        <section class="login-card">
          <div class="login-brand"><span class="brand-mark">ITSM</span><div><strong>企业服务台</strong><small>用户统一登录入口</small></div></div>
          <div class="login-heading"><span class="eyebrow">企业身份登录</span><h1>进入 IT 服务工作台</h1><p>登录后可访问消息、历史工单、联系人和工作台。</p></div>
          <form id="login-form" class="login-form" autocomplete="off">
            <label for="tenant-id">租户 ID</label>
            <input id="tenant-id" name="tenantId" value="cza" required>
            <label for="login-account">邮箱或工号</label>
            <input id="login-account" name="account" placeholder="邮箱或工号" autocomplete="username" required>
            <label for="login-password">密码</label>
            <input id="login-password" name="password" type="password" placeholder="密码" autocomplete="current-password" required>
            <button type="submit" class="primary-button login-submit">登录</button>
          </form>
          <div class="login-note">种子账号：工号 000003（张三）/ P@ssw0rd123 · <a href="./register.html">注册账号</a></div>
        </section>
        <div class="login-visual">
          <div class="login-visual-panel">
            <div class="visual-top"><span>ITSM</span><span class="online-dot">在线</span></div>
            <h2>自助咨询 · 同事沟通 · 人工接转</h2>
            <p>在一个工作台里完成常见 IT 问题处理和客服协同。</p>
            <div class="visual-grid">
              <div><span>IT</span><strong>智能助手</strong><small>操作系统、网络、邮箱</small></div>
              <div><span>联</span><strong>企业联系人</strong><small>快速查找同事</small></div>
              <div><span>工</span><strong>历史工单</strong><small>处理进度与评价</small></div>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  function renderShell(activeView, content) {
    const profile = userProfile();
    const historyCount = historyTickets().length;
    const unreadCount = (state.colleagueConversations || []).reduce((sum, item) => sum + (item.unreadCount || 0), 0);
    const viewLabels = {
      MESSAGES: '消息',
      HISTORY: '历史消息',
      CONTACTS: '联系人',
      WORKSPACE: '工作台',
      ITSM: 'ITSM 工单',
      WHALE: '数鲸看板',
      APPROVALS: '审批',
      EMPLOYEES: '员工账号'
    };
    const views = [
      { key: 'MESSAGES', label: '消息' },
      { key: 'HISTORY', label: '历史消息' },
      { key: 'CONTACTS', label: '联系人' },
      { key: 'WORKSPACE', label: '工作台' }
    ];
    const nav = views.map((item) => `
      <button class="portal-nav-button ${item.key === activeView ? 'active' : ''}" data-action="nav" data-view="${item.key}">
        <span class="portal-nav-label">${item.label}</span>
        ${item.key === 'HISTORY' ? `<span class="portal-nav-count">${historyCount}</span>` : ''}
        ${item.key === 'MESSAGES' && unreadCount ? `<span class="portal-nav-count accent">${unreadCount}</span>` : ''}
      </button>
    `).join('');

    appRoot.innerHTML = `
      <div class="user-portal">
        <div class="portal-top"><span><strong>ITSM系统</strong></span><span>${escapeHtml(currentTenant().tenantName)}</span></div>
        <div class="portal-shell">
          <aside class="portal-sidebar">
            <button class="portal-profile-head" data-action="toggle-profile"><span class="avatar">${escapeHtml(profile.displayName.slice(0, 1))}</span><span class="portal-profile-title"><strong>${escapeHtml(profile.displayName)}</strong><small>${escapeHtml(profile.departmentName)}</small></span><span class="chevron">›</span></button>
            <nav class="portal-nav">${nav}</nav>
            
          </aside>
          <main class="portal-main">
            <header class="portal-header"><div class="breadcrumbs"><span>ITSM</span><span>/</span><strong>${escapeHtml(viewLabels[activeView] || '工作台')}</strong></div><div class="header-actions">${activeView === 'ITSM' ? '<button class="ghost-button" data-action="close-itsm-view">返回工作台</button>' : ''}<span class="presence"><i></i>${isSupportAgent() ? '客服在线' : '在线'}</span><button class="ghost-button" data-action="logout">退出登录</button></div></header>
            ${content}
          </main>
        </div>
        ${state.profileOpen ? renderProfilePopover(profile) : ''}
        ${state.ticketDetail ? renderTicketDetailOverlay() : ''}
      </div>
    `;
  }

  function renderProfilePopover(profile) {
    return `
      <div class="popover-backdrop" data-action="toggle-profile"></div>
      <section class="profile-popover">
        <div class="profile-popover-head"><span class="avatar large">${escapeHtml(profile.displayName.slice(0, 1))}</span><div><h2>${escapeHtml(profile.displayName)}</h2><p>${escapeHtml(profile.departmentName)}</p></div></div>
        <div class="profile-grid"><div><span>用户编号</span><strong>${escapeHtml(profile.userId)}</strong></div><div><span>所属企业</span><strong>${escapeHtml(currentTenant().tenantName)}</strong></div><div><span>当前角色</span><strong>${escapeHtml(roleLabel())}</strong></div><div><span>邮箱</span><strong>${escapeHtml(profile.email || '企业内部邮箱')}</strong></div></div>
        <div class="popover-actions"><button class="primary-button" data-action="toggle-profile">返回工作台</button><button class="ghost-button" data-action="logout">退出登录</button></div>
      </section>
    `;
  }

  function renderMessages() {
    const sessions = userSessions();
    const colleagues = (state.colleagueConversations || []).slice().sort((a, b) => new Date(b.lastMessageAt) - new Date(a.lastMessageAt));
    const items = [
      { key: 'assistant', type: 'ASSISTANT', name: 'IT 助手', subtitle: '智能服务助手', preview: '操作系统、网络、邮箱等问题都可以问我', time: '现在', avatar: 'IT', tone: 'blue' },
      ...colleagues.map((item) => ({ key: item.conversationId, type: 'COLLEAGUE', name: item.displayName, subtitle: item.departmentName, preview: item.lastMessage, time: formatDate(item.lastMessageAt), avatar: item.displayName.slice(0, 1), tone: '', unread: item.unreadCount || 0 }))
    ];
    const filtered = state.portalSearch ? items.filter((item) => [item.name, item.subtitle, item.preview].join(' ').toLowerCase().includes(state.portalSearch.trim().toLowerCase())) : items;
    const sidebar = filtered.map((item) => `
      <button class="chat-list-item ${state.chat.type === item.type && state.chat.id === item.key ? 'active' : ''}" data-action="open-chat" data-type="${item.type}" data-id="${item.key}">
        <span class="chat-list-avatar ${item.tone}">${escapeHtml(item.avatar)}</span><span class="chat-list-copy"><span class="chat-list-top"><strong>${escapeHtml(item.name)}</strong>${item.unread ? `<span class="chat-list-unread">${item.unread}</span>` : ''}<time>${escapeHtml(item.time)}</time></span><span class="chat-list-sub">${escapeHtml(item.subtitle)}</span><span class="chat-list-preview">${escapeHtml(item.preview)}</span></span>
      </button>
    `).join('');
    const colleague = colleagues.find((item) => item.conversationId === state.chat.id);
    const chatContent = state.chat.type === 'COLLEAGUE' ? renderColleagueChat(colleague) : renderAssistantChat(sessions);

    renderShell('MESSAGES', `
      <section class="message-workspace">
        <aside class="panel chat-list-panel"><div class="panel-head"><div><h1>消息</h1><p>IT 助手与同事会话</p></div></div><div class="search-box"><input class="text-input" data-bind="portalSearch" placeholder="搜索消息或联系人" value="${escapeHtml(state.portalSearch)}"></div><div class="chat-list">${sidebar || '<div class="empty-state">没有匹配的会话</div>'}</div></aside>
        <section class="panel chat-panel"><header class="chat-header"><button class="message-slide-back" data-action="show-chat-list" title="返回会话列表">‹</button><div class="chat-person"><span class="chat-avatar ${state.chat.type === 'ASSISTANT' ? 'blue' : 'green'}">${state.chat.type === 'ASSISTANT' ? 'IT' : escapeHtml(colleague ? colleague.displayName.slice(0, 1) : '同')}</span><div><strong>${state.chat.type === 'ASSISTANT' ? 'IT 助手' : escapeHtml(colleague ? colleague.displayName : '同事')}</strong><small>${state.chat.type === 'ASSISTANT' ? '在线 · 可随时提问' : '同事对话 · 企业内部沟通'}</small></div></div><button class="icon-button" data-action="nav" data-view="WORKSPACE" title="工作台">台</button></header>${chatContent}</section>
        <aside class="panel context-panel"><div class="panel-head"><h2>当前上下文</h2><p>聊天、工单与快捷服务</p></div>${renderContextCards(state.chat.type === 'ASSISTANT' ? 'IT 助手' : (colleague ? colleague.displayName : '同事'))}</aside>
      </section>
    `);
  }

  function renderAssistantChat(sessions) {
    const session = sessions[0] || currentSession();
    const messages = session ? session.messages : [];
    const categories = [
      { key: 'OS', label: '操作系统问题', desc: '开机异常、系统更新、蓝屏死机' },
      { key: 'NETWORK', label: '网络问题', desc: '无法联网、VPN、无线网络' },
      { key: 'MAIL', label: '邮箱问题', desc: '收发失败、登录异常、提醒设置' },
      { key: 'SOFTWARE', label: '软件问题', desc: '办公软件安装、权限与版本' },
      { key: 'ACCOUNT', label: '账号权限', desc: '密码重置、权限申请、登录失败' }
    ];
    const categoryCards = categories.map((item) => `<button class="assistant-option ${state.assistantCategory === item.key ? 'selected' : ''}" data-action="category" data-key="${item.key}" data-label="${escapeHtml(item.label)}"><strong>${escapeHtml(item.label)}</strong><small>${escapeHtml(item.desc)}</small><span>→</span></button>`).join('');
    const messageHtml = messages.map((message) => {
      const isUser = message.senderType === 'USER';
      return `<div class="chat-message ${isUser ? 'user' : 'assistant'}"><div class="message-avatar">${isUser ? escapeHtml(userProfile().displayName.slice(0, 1)) : 'IT'}</div><div class="message-body"><div class="message-meta"><span>${isUser ? escapeHtml(userProfile().displayName) : 'IT 助手'}</span><time>${escapeHtml(formatDateTime(message.createdAt))}</time></div><div class="message-bubble">${escapeHtml(message.content)}</div></div></div>`;
    }).join('');
    const successTicket = state.handoffSuccess ? userTickets().find((ticket) => ticket.ticketId === state.handoffSuccess) : null;

    return `
      <div class="chat-body">
        <div class="chat-scroll">
          <div class="welcome"><div class="welcome-mark">IT</div><h2>你好，${escapeHtml(userProfile().displayName)}</h2><p>你可以描述问题，也可以从服务选项开始。</p></div>
          <div class="category-grid">${categoryCards}</div>
          <div class="message-thread">${messageHtml || '<div class="empty-state">还没有消息，先选择一个分类开始。</div>'}</div>
          ${successTicket ? renderHandoffSuccessCard(successTicket) : ''}
          ${state.handoffOpen ? renderHandoffForm() : ''}
        </div>
        <form class="composer" id="assistant-chat-form"><div class="composer-tools"><button type="button" data-action="tool" data-tool="attach" title="附件">＋</button><button type="button" data-action="tool" data-tool="file" title="文件">文</button><button type="button" data-action="tool" data-tool="screenshot" title="截图">截</button></div><textarea class="textarea-input" name="message" placeholder="请输入问题，例如：我的电脑无法连接网络">${escapeHtml(state.assistantDraft)}</textarea><div class="composer-footer"><button type="button" class="ghost-button" data-action="end-session">结束会话</button><button type="button" class="ghost-button" data-action="open-handoff">转人工</button><button class="primary-button" type="submit">发送</button></div></form>
      </div>
    `;
  }

  function renderHandoffForm() {
    return `<div class="handoff-card"><div class="handoff-head"><div><span>转人工服务</span><h3>把当前问题交给人工客服</h3></div><button class="close-button" data-action="close-handoff" title="关闭">×</button></div><div class="handoff-grid"><div><span>问题标题</span><strong>${escapeHtml(state.handoff.title || (currentSession() && currentSession().summary) || 'IT 服务咨询')}</strong></div><div><span>问题分类</span><strong>${escapeHtml(state.handoff.category)}</strong></div><div><span>处理队列</span><strong>IT 支持 · 普通队列</strong></div></div><p class="handoff-note">提交后系统会保留当前对话上下文，并生成可跟踪的工单。</p><div class="handoff-actions"><button class="ghost-button" data-action="close-handoff">暂不转人工</button><button class="primary-button" data-action="confirm-handoff">确认转人工</button></div></div>`;
  }

  function renderHandoffSuccessCard(ticket) {
    return `<div class="handoff-success-card"><div class="success-head"><div><span>已提交转人工</span><h3>${escapeHtml(ticket.ticketNo)} · ${escapeHtml(ticket.title)}</h3></div><span class="status-tag ${statusClass(ticket.status)}">${escapeHtml(statusLabel(ticket.status))}</span></div><div class="success-steps"><span class="done">已创建服务请求</span><span class="active">等待客服受理</span><span>客服沟通</span><span>处理完成</span></div><div class="success-actions"><button class="primary-button" data-action="open-ticket" data-ticket-id="${escapeHtml(ticket.ticketId)}">查看工单</button><button class="ghost-button" data-action="dismiss-handoff-success">继续咨询</button></div></div>`;
  }

  function renderColleagueChat(conversation) {
    if (!conversation) return '<div class="chat-body"><div class="empty-state">暂无同事会话，请到联系人中发起聊天。</div></div>';
    const messageHtml = conversation.messages.map((message) => {
      const isUser = message.senderType === 'USER';
      return `<div class="chat-message ${isUser ? 'user' : 'contact'}"><div class="message-avatar">${isUser ? escapeHtml(userProfile().displayName.slice(0, 1)) : escapeHtml(message.senderName.slice(0, 1))}</div><div class="message-body"><div class="message-meta"><span>${escapeHtml(message.senderName)}</span><time>${escapeHtml(formatDateTime(message.createdAt))}</time></div><div class="message-bubble">${escapeHtml(message.content)}</div></div></div>`;
    }).join('');
    return `<div class="chat-body"><div class="chat-scroll"><div class="colleague-strip"><span class="${conversation.status.toLowerCase()}">${conversation.status === 'ONLINE' ? '在线' : conversation.status === 'BUSY' ? '忙碌' : '离线'}</span><small>${escapeHtml(conversation.departmentName)}</small></div><div class="message-thread">${messageHtml}</div></div><form class="composer" id="colleague-chat-form"><textarea class="textarea-input" name="message" placeholder="发送消息给 ${escapeHtml(conversation.displayName)}">${escapeHtml(state.colleagueDraft)}</textarea><div class="composer-footer"><span>${escapeHtml(conversation.displayName)} · ${escapeHtml(conversation.status)}</span><button class="primary-button" type="submit">发送</button></div></form></div>`;
  }

  function renderContextCards(name) {
    const openStatuses = ['PENDING_ACCEPTANCE', 'IN_PROGRESS', 'PENDING_USER_CONFIRM', 'REOPENED'];
    const ticket = userTickets().find((item) => openStatuses.includes(item.status)) || userTickets()[0];
    return `<div class="context-card accent"><span>当前对话</span><h3>${escapeHtml(name)}</h3><p>${name === 'IT 助手' ? '可先自助排查，也可以随时转人工。' : '企业内沟通不会自动创建 IT 工单。'}</p></div>${ticket ? `<div class="context-card"><span>最近工单</span><h3>${escapeHtml(ticket.title)}</h3><p><span class="status-tag ${statusClass(ticket.status)}">${escapeHtml(ticket.status)}</span> · ${escapeHtml(ticket.ticketNo)}</p><button class="ghost-button" data-action="open-ticket" data-ticket-id="${escapeHtml(ticket.ticketId)}">查看工单</button></div>` : ''}<div class="context-card"><span>快捷入口</span><div class="quick-links"><button data-action="nav" data-view="CONTACTS">查找同事</button><button data-action="nav" data-view="HISTORY">历史工单</button><button data-action="nav" data-view="WORKSPACE">工作台</button></div></div>`;
  }

  function renderContacts() {
    const keyword = state.contactKeyword.trim().toLowerCase();
    const filtered = (state.contacts || []).filter((contact) => !keyword || [contact.displayName, contact.departmentName, contact.email].join(' ').toLowerCase().includes(keyword));
    const total = filtered.length;
    const totalPages = Math.max(1, Math.ceil(total / state.contactsPageSize));
    const page = Math.min(totalPages, Math.max(1, state.contactsPage || 1));
    state.contactsPage = page;
    const start = (page - 1) * state.contactsPageSize;
    const items = filtered.slice(start, start + state.contactsPageSize);
    const rows = items.map((contact) => `<button class="contact-row" data-action="open-contact" data-contact-id="${escapeHtml(contact.contactId)}"><span class="contact-avatar offline">${escapeHtml((contact.displayName || '?').slice(0, 1))}</span><span class="contact-copy"><strong>${escapeHtml(contact.displayName)}</strong><small>${escapeHtml(contact.departmentName || '未分配部门')}</small></span><span class="contact-presence"><i></i>${contact.enabled ? '在职' : '停用'}</span><span class="contact-open">发消息</span></button>`).join('');
    const pager = `<div class="pagination"><span class="pagination-meta">共 ${total} 条 · 每页 ${state.contactsPageSize} 条</span><div class="pagination-controls"><button class="page-button" data-action="contacts-page" data-page="${page - 1}" ${page === 1 ? 'disabled' : ''}>‹</button><span class="page-button active">${page}</span><button class="page-button" data-action="contacts-page" data-page="${page + 1}" ${page === totalPages ? 'disabled' : ''}>›</button></div></div>`;
    renderShell('CONTACTS', `<section class="page-section"><div class="page-heading"><div><h1>联系人</h1><p>公司内注册用户自动成为联系人，同部门相邻展示</p></div><div class="heading-actions"><input class="text-input contact-search" data-bind="contactKeyword" placeholder="搜索姓名、部门" value="${escapeHtml(state.contactKeyword)}"><button class="ghost-button" data-action="refresh-contacts">刷新</button></div></div><div class="contacts-layout"><div class="contacts-list panel">${rows || '<div class="empty-state">没有匹配的联系人</div>'}</div><aside class="contact-side panel"><div class="contact-side-placeholder"><span>联</span><h3>选择一位同事</h3><p>查看资料或发起企业内聊天。</p></div></aside></div>${pager}</section>`);
  }

  function renderWorkspace() {
    const cards = [];
    if (hasItsmAccess()) {
      cards.push({ name: 'ITSM 工单', desc: '在新窗口打开 ITSM 工单处理系统', icon: '工', tone: 'orange', action: 'open-window', url: './index.html?direct=1' });
    }
    cards.push({ name: '数鲸看板', desc: '在新窗口打开数鲸看板', icon: '鲸', tone: 'cyan', action: 'open-window', url: './user-portal.html?view=WHALE' });
    if (hasAdminRole()) {
      cards.push({ name: '审批', desc: '审批 ITSM 权限与管理员权限申请', icon: '审', tone: 'violet', action: 'nav', target: 'APPROVALS' });
      cards.push({ name: '员工账号', desc: '管理员工手机号、邮箱与部门', icon: '员', tone: 'green', action: 'nav', target: 'EMPLOYEES' });
    }
    const cardsHtml = cards.map((card) => {
      const attr = card.action === 'open-window'
        ? `data-action="open-window" data-url="${escapeHtml(card.url)}"`
        : `data-action="nav" data-view="${card.target}"`;
      return `<button class="workspace-app ${card.tone}" ${attr}><span class="app-icon">${escapeHtml(card.icon)}</span><span class="app-copy"><strong>${escapeHtml(card.name)}</strong><small>${escapeHtml(card.desc)}</small></span><span class="app-open">打开</span></button>`;
    }).join('');
    renderShell('WORKSPACE', `<section class="page-section"><div class="page-heading"><div><h1>工作台</h1><p>从这里进入 ITSM 工单、数鲸看板与审批</p></div><span class="status-chip ok">当前用户工作台</span></div><div class="workspace-grid panel">${cardsHtml || '<div class="empty-state">暂无工作台应用</div>'}</div></section>`);
  }

  function renderItsm() {
    const hasAccess = hasItsmAccess();
    const pending = (state.myRequests || []).find((item) => item.status === 'PENDING');
    let body;
    if (hasAccess) {
      body = `<iframe class="itsm-system-frame" src="./index.html?embedded=1" title="ITSM 工单系统"></iframe>`;
    } else if (pending) {
      const typeLabel = pending.requestType === 'ADMIN' ? '管理员权限' : 'ITSM 权限';
      body = `
        <div class="itsm-permission-state submitted">
          <div class="permission-state-icon">已</div>
          <h2>${escapeHtml(typeLabel)}申请已提交</h2>
          <p>管理员审批通过后会为你开通对应权限，请稍后重新登录查看。</p>
          <div class="permission-state-actions"><button class="ghost-button" data-action="nav" data-view="MESSAGES">返回首页</button></div>
        </div>
      `;
    } else if (state.itsmApplication.status === 'REQUESTING') {
      const typeLabel = state.itsmApplication.requestType === 'ADMIN' ? '管理员权限' : 'ITSM 权限';
      body = `
        <div class="itsm-permission-state requesting">
          <div class="permission-state-icon">权</div>
          <h2>申请 ${escapeHtml(typeLabel)}</h2>
          <p>提交后由管理员审批，审批通过后重新登录生效。</p>
          <form id="itsm-permission-form" class="itsm-permission-form">
            <label>申请账号</label><input class="text-input" value="${escapeHtml(userProfile().displayName)}" readonly>
            <label for="itsm-permission-reason">申请原因</label><textarea id="itsm-permission-reason" class="textarea-input" name="reason" placeholder="请说明申请原因">${escapeHtml(state.itsmApplication.reason)}</textarea>
            <div class="permission-form-actions"><button type="button" class="ghost-button" data-action="cancel-itsm-permission">取消</button><button type="submit" class="primary-button">提交申请</button></div>
          </form>
        </div>
      `;
    } else {
      body = `
        <div class="itsm-permission-state denied">
          <div class="permission-state-icon">!</div>
          <h2>您好，您暂时没有 ITSM 权限</h2>
          <p>普通用户需要提交审批，管理员批准后即可使用 ITSM 工单系统。</p>
          <div class="permission-state-actions">
            <button class="ghost-button" data-action="nav" data-view="MESSAGES">返回首页</button>
            <button class="primary-button" data-action="open-itsm-permission">申请 ITSM 权限</button>
            <button class="ghost-button" data-action="open-admin-permission">申请管理员权限</button>
          </div>
        </div>
      `;
    }
    renderShell('ITSM', `<section class="itsm-system-container"><div class="itsm-system-toolbar"><div><span class="status-chip ${hasAccess ? 'ok' : 'info'}">ITSM 工单系统</span><small>${hasAccess ? '客服工单处理工作台' : '工单权限申请与处理系统'}</small></div>${hasAccess ? '<span class="muted">在门户内完成工单查询与处理</span>' : ''}</div>${body}</section>`);
  }

  function renderApprovals() {
    const rows = (state.approvals || []).map((item) => `
      <div class="approval-row">
        <div class="approval-info">
          <strong>${escapeHtml(item.requesterName || item.requesterId)}</strong>
          <span class="muted">${escapeHtml(item.requesterId)} · ${escapeHtml(item.requestType === 'ADMIN' ? '管理员权限' : 'ITSM 权限')}</span>
          <p>${escapeHtml(item.reason || '无申请原因')}</p>
        </div>
        <div class="approval-actions">
          <button class="primary-button" data-action="approve-request" data-request-id="${escapeHtml(item.requestId)}">批准</button>
          <button class="ghost-button" data-action="reject-request" data-request-id="${escapeHtml(item.requestId)}">驳回</button>
        </div>
      </div>
    `).join('');
    renderShell('APPROVALS', `<section class="page-section"><div class="page-heading"><div><h1>审批</h1><p>审批普通用户的 ITSM 权限或管理员权限申请</p></div><span class="status-chip info">${state.approvals.length} 条待审批</span></div><div class="panel approval-list">${rows || '<div class="empty-state">暂无待审批申请</div>'}</div></section>`);
  }

  function renderEmployees() {
    const rows = (state.employees || []).map((emp) => {
      if (state.employeeEditingId === emp.userId) {
        return `
          <div class="employee-row editing">
            <div class="employee-name"><strong>${escapeHtml(emp.displayName)}</strong><span class="muted">${escapeHtml(emp.userId)}</span></div>
            <div class="employee-fields">
              <input class="text-input" data-bind="employeeDepartment" placeholder="部门" value="${escapeHtml(state.employeeForm.departmentName)}">
              <input class="text-input" data-bind="employeePhone" placeholder="手机号" value="${escapeHtml(state.employeeForm.phone)}">
              <input class="text-input" data-bind="employeeEmail" placeholder="邮箱" value="${escapeHtml(state.employeeForm.email)}">
            </div>
            <div class="employee-actions">
              <button class="primary-button" data-action="save-employee" data-user-id="${escapeHtml(emp.userId)}">保存</button>
              <button class="ghost-button" data-action="cancel-employee-edit">取消</button>
            </div>
          </div>
        `;
      }
      return `
        <div class="employee-row">
          <div class="employee-name"><strong>${escapeHtml(emp.displayName)}</strong><span class="muted">${escapeHtml(emp.userId)}</span></div>
          <div class="employee-fields">
            <span>${escapeHtml(emp.departmentName || '未分配部门')}</span>
            <span>${escapeHtml(emp.phone || '无手机号')}</span>
            <span>${escapeHtml(emp.email || '无邮箱')}</span>
          </div>
          <div class="employee-actions"><button class="ghost-button" data-action="edit-employee" data-user-id="${escapeHtml(emp.userId)}">编辑</button></div>
        </div>
      `;
    }).join('');
    renderShell('EMPLOYEES', `<section class="page-section"><div class="page-heading"><div><h1>员工账号</h1><p>管理员工的手机号、邮箱与部门（仅管理员）</p></div><button class="ghost-button" data-action="refresh-employees">刷新</button></div><div class="panel employee-list">${rows || '<div class="empty-state">暂无员工</div>'}</div></section>`);
  }

  function renderHistory() {
    const tickets = historyTickets();
    const rows = tickets.map((ticket) => `<button class="history-row" data-action="open-ticket" data-ticket-id="${escapeHtml(ticket.ticketId)}"><span class="status-tag ${statusClass(ticket.status)}">${escapeHtml(statusLabel(ticket.status))}</span><span class="history-main"><strong>${escapeHtml(ticket.title)}</strong><small>${escapeHtml(ticket.description)}</small></span><span class="history-meta"><b>${escapeHtml(ticket.ticketNo)}</b><time>${escapeHtml(formatDateTime(ticket.updatedAt || ticket.createdAt))}</time></span><span class="history-arrow">›</span></button>`).join('');
    renderShell('HISTORY', `<section class="page-section"><div class="page-heading"><div><h1>历史消息</h1><p>仅展示最近 30 天的用户工单信息</p></div><span class="status-chip info">${tickets.length} 条 · 最近30天</span></div><div class="history-list panel">${rows || '<div class="empty-state">最近 30 天暂无工单</div>'}</div></section>`);
  }

  function renderWhale() {
    const workload = DATA.whaleWorkload || [];
    const distribution = DATA.whaleDistribution || [];
    const createdTotal = workload.reduce((sum, item) => sum + item.draftPerDay, 0);
    const solvedTotal = workload.reduce((sum, item) => sum + item.solvedPerDay, 0);
    const unresolvedTotal = workload.reduce((sum, item) => sum + item.unresolved, 0);
    const over48Total = workload.reduce((sum, item) => sum + item.over48Hours, 0);
    const distributionTotal = distribution.reduce((sum, item) => sum + item.count, 0) || 1;
    const maxCreated = Math.max(...workload.map((item) => item.draftPerDay), 1);
    const maxSolved = Math.max(...workload.map((item) => item.solvedPerDay), 1);
    const maxUnresolved = Math.max(...distribution.map((item) => item.count), 1);
    const createdBars = workload.map((item) => { const width = Math.max(10, Math.round(item.draftPerDay / maxCreated * 100)); return `<div class="whale-bar-row"><span class="whale-axis-label">${escapeHtml(item.agentName)}</span><div class="whale-bar-track"><i style="width:${width}%"></i><b>${item.draftPerDay}</b></div></div>`; }).join('');
    const solvedBars = workload.map((item) => { const width = Math.max(10, Math.round(item.solvedPerDay / maxSolved * 100)); return `<div class="whale-bar-row"><span class="whale-axis-label">${escapeHtml(item.agentName)}</span><div class="whale-bar-track solved"><i style="width:${width}%"></i><b>${item.solvedPerDay}</b></div></div>`; }).join('');
    const distributionBars = distribution.map((item) => { const width = Math.max(8, Math.round(item.count / maxUnresolved * 100)); return `<div class="whale-bar-row distribution"><span class="whale-axis-label">${escapeHtml(item.label)}</span><div class="whale-bar-track unresolved"><i style="width:${width}%"></i><b>${item.count}</b></div></div>`; }).join('');
    const rangeLabel = state.whaleFilter.appliedRange === 'custom' ? `${state.whaleFilter.customStart || '开始日期'} 至 ${state.whaleFilter.customEnd || '结束日期'}` : state.whaleFilter.appliedRange === '1' ? '昨天' : state.whaleFilter.appliedRange === '7' ? '今天' : '最近 30 天';
    renderShell('WHALE', `
      <section class="page-section whale-page">
        <div class="page-heading"><div><h1>数鲸看板</h1><p>ITSM 工作量、解决情况与未解决工单分布</p></div><span class="status-chip info">统计范围：${escapeHtml(rangeLabel)}</span></div>
        <section class="panel whale-filter-panel"><div class="whale-filter-head"><strong>时间范围</strong><small>选择要统计的工单数据范围</small></div><div class="whale-filter-options"><button class="whale-range-button ${state.whaleFilter.range === '1' ? 'active' : ''}" data-action="whale-range" data-range="1">昨天</button><button class="whale-range-button ${state.whaleFilter.range === '7' ? 'active' : ''}" data-action="whale-range" data-range="7">今天</button><button class="whale-range-button ${state.whaleFilter.range === '30' ? 'active' : ''}" data-action="whale-range" data-range="30">最近 30 天</button><label class="whale-custom-range">自定义<input class="text-input" type="date" data-bind="whaleCustomStart" value="${escapeHtml(state.whaleFilter.customStart)}"><span>至</span><input class="text-input" type="date" data-bind="whaleCustomEnd" value="${escapeHtml(state.whaleFilter.customEnd)}"></label></div><div class="whale-filter-actions"><button class="ghost-button" data-action="whale-reset">重置</button><button class="primary-button" data-action="whale-query">查询</button></div></section>
        <div class="whale-metric-grid">${whaleMetric('草稿池创建量', createdTotal, '当前筛选范围', 'blue')}${whaleMetric('工单解决量', solvedTotal, '当前筛选范围', 'green')}${whaleMetric('所有未解决数量', unresolvedTotal, '尚未关闭的工单', 'amber')}${whaleMetric('48 小时未解决数量', over48Total, '超过处理时限', 'red')}</div>
        <div class="whale-chart-grid"><section class="panel whale-chart-panel"><div class="panel-head"><h2>工单创建统计图</h2><p>横轴：客服名字 · 纵轴：工单创建量</p></div><div class="whale-chart-body">${createdBars || '<div class="empty-state">暂无数据</div>'}</div></section><section class="panel whale-chart-panel"><div class="panel-head"><h2>工单解决统计图</h2><p>横轴：客服名字 · 纵轴：工单解决量</p></div><div class="whale-chart-body">${solvedBars || '<div class="empty-state">暂无数据</div>'}</div></section></div>
        <section class="panel whale-chart-panel unresolved-panel"><div class="panel-head"><h2>未解决工单分布图</h2><p>按问题类型统计当前未解决工单数量</p></div><div class="whale-chart-body distribution-body">${distributionBars || '<div class="empty-state">暂无未解决工单</div>'}</div></section>
      </section>
    `);
  }

  function whaleMetric(label, value, desc, tone) {
    return `<div class="whale-metric ${tone}"><span>${escapeHtml(label)}</span><strong>${value}</strong><small>${escapeHtml(desc)}</small></div>`;
  }

  function handleClick(event) {
    const target = event.target.closest('[data-action]');
    if (!target) return;
    const action = target.dataset.action;

    if (action === 'nav') {
      state.view = target.dataset.view || 'MESSAGES';
      state.chat = { type: 'ASSISTANT', id: 'assistant' };
      state.portalSearch = '';
      render();
      return;
    }
    if (action === 'toggle-profile') {
      state.profileOpen = !state.profileOpen;
      render();
      return;
    }
    if (action === 'logout') {
      session.accessToken = null;
      session.refreshToken = null;
      session.user = null;
      session.tenant = null;
      session.roles = [];
      localStorage.removeItem('itsm.session');
      state.loggedIn = false;
      state.profileOpen = false;
      render();
      return;
    }
    if (action === 'close-itsm-view') {
      state.view = 'WORKSPACE';
      state.profileOpen = false;
      render();
      return;
    }
    if (action === 'open-itsm-permission') {
      state.itsmApplication.status = 'REQUESTING';
      state.itsmApplication.reason = '';
      state.itsmApplication.requestType = 'ITSM_ACCESS';
      render();
      return;
    }
    if (action === 'open-admin-permission') {
      state.itsmApplication.status = 'REQUESTING';
      state.itsmApplication.reason = '';
      state.itsmApplication.requestType = 'ADMIN';
      render();
      return;
    }
    if (action === 'open-oa-form') {
      state.itsmApplication.status = 'REQUESTING';
      state.itsmApplication.reason = '';
      state.itsmApplication.requestType = 'ITSM_ACCESS';
      render();
      return;
    }
    if (action === 'contact-admin') {
      const admin = (state.contacts || []).find((contact) => contact.userId === '000001');
      if (!admin) return;
      let conversation = state.colleagueConversations.find((item) => item.contactId === admin.contactId);
      if (!conversation) {
        conversation = {
          conversationId: 'col_con_admin_' + Date.now(),
          contactId: admin.contactId,
          userId: admin.userId,
          displayName: admin.displayName,
          departmentName: admin.departmentName || '客服管理部',
          status: 'ONLINE',
          lastMessage: '请联系我处理 ITSM 权限申请',
          lastMessageAt: nowIso(),
          unreadCount: 0,
          messages: [
            {
              messageId: 'col_msg_admin_' + Date.now(),
              senderType: 'SYSTEM',
              senderName: '系统',
              content: '已为你打开与 ' + admin.displayName + ' 的会话，可说明 ITSM 权限申请事项。',
              createdAt: nowIso()
            }
          ]
        };
        state.colleagueConversations.unshift(conversation);
      }
      state.view = 'MESSAGES';
      state.chat = { type: 'COLLEAGUE', id: conversation.conversationId };
      state.colleagueDraft = '你好，管理员。我提交了 ITSM 权限申请，麻烦帮忙添加权限。';
      render();
      return;
    }
    if (action === 'cancel-itsm-permission') {
      state.itsmApplication.status = 'NONE';
      state.itsmApplication.reason = '';
      state.itsmApplication.requestType = 'ITSM_ACCESS';
      render();
      return;
    }
    if (action === 'approve-request') {
      approveRequest(target.dataset.requestId);
      return;
    }
    if (action === 'reject-request') {
      rejectRequest(target.dataset.requestId);
      return;
    }
    if (action === 'edit-employee') {
      startEditEmployee(target.dataset.userId);
      return;
    }
    if (action === 'save-employee') {
      saveEmployee(target.dataset.userId);
      return;
    }
    if (action === 'cancel-employee-edit') {
      cancelEditEmployee();
      return;
    }
    if (action === 'refresh-employees') {
      loadEmployees().finally(() => render());
      return;
    }
    if (action === 'open-chat') {
      state.view = 'MESSAGES';
      state.chat = { type: target.dataset.type || 'ASSISTANT', id: target.dataset.id || 'assistant' };
      state.colleagueDraft = '';
      if (state.chat.type === 'COLLEAGUE') {
        const conv = state.colleagueConversations.find((item) => item.conversationId === state.chat.id);
        if (conv) {
          conv.lastReadMessageId = conv.messages.length ? Number(conv.messages[conv.messages.length - 1].messageId) : 0;
          conv.unreadCount = 0;
        }
      }
      render();
      return;
    }
    if (action === 'toggle-search') {
      state.portalSearch = state.portalSearch ? '' : ' ';
      render();
      if (state.portalSearch) window.setTimeout(() => { const input = document.querySelector('.search-box input'); if (input) input.focus(); }, 0);
      return;
    }
    if (action === 'show-chat-list') {
      const workspace = document.querySelector('.message-workspace');
      if (workspace) workspace.classList.add('show-chat-list');
      return;
    }
    if (action === 'open-contact') {
      const contact = (state.contacts || []).find((item) => item.contactId === target.dataset.contactId);
      if (!contact) return;
      let conversation = state.colleagueConversations.find((item) => item.contactId === contact.contactId);
      if (!conversation) {
        conversation = {
          conversationId: 'col_con_' + contact.userId,
          contactId: contact.contactId,
          userId: contact.userId,
          displayName: contact.displayName,
          departmentName: contact.departmentName || '未分配部门',
          status: 'ONLINE',
          lastMessage: '开始聊天',
          lastMessageAt: nowIso(),
          unreadCount: 0,
          messages: []
        };
        state.colleagueConversations.unshift(conversation);
      }
      openColleagueConversation(conversation);
      render();
      loadColleagueMessages(conversation).then(() => render());
      return;
    }
    if (action === 'category') {
      state.assistantCategory = target.dataset.key;
      state.handoff.category = target.dataset.label;
      render();
      return;
    }
    if (action === 'open-handoff') {
      const session = currentSession();
      state.handoffOpen = true;
      state.handoff.title = state.handoff.title || (session && (session.summary || session.subject)) || 'IT 服务咨询';
      state.handoff.description = state.assistantDraft || '用户请求从 IT 助手转接人工客服。';
      render();
      return;
    }
    if (action === 'end-session') {
      endCurrentSession();
      return;
    }
    if (action === 'close-handoff') {
      state.handoffOpen = false;
      render();
      return;
    }
    if (action === 'confirm-handoff') {
      createHandoffTicket();
      return;
    }
    if (action === 'dismiss-handoff-success') {
      state.handoffSuccess = null;
      render();
      return;
    }
    if (action === 'open-app') {
      const extraApps = [
        { appId: 'wb_whale_board', name: '数鲸看板', type: 'WHALE' }
      ];
      const app = [...(DATA.workbenchApps || []), ...extraApps].find((item) => item.appId === target.dataset.appId);
      if (!app) return;
      if (app.type === 'ASSISTANT') {
        state.view = 'MESSAGES';
        state.chat = { type: 'ASSISTANT', id: 'assistant' };
      } else if (app.type === 'CONTACT') {
        state.view = 'CONTACTS';
      } else if (app.type === 'TICKET') {
        state.view = 'HISTORY';
      } else if (app.type === 'WHALE') {
        state.view = 'WHALE';
      } else if (app.type === 'SYSTEM') {
        state.itsmApplication.status = state.itsmApplication.status || 'NONE';
        state.view = 'ITSM';
        state.profileOpen = false;
        render();
        return;
      } else {
        state.view = 'MESSAGES';
        state.chat = { type: 'ASSISTANT', id: 'assistant' };
      }
      render();
      return;
    }
    if (action === 'select-oa-type') {
      state.oaDraft.approvalType = target.dataset.type;
      render();
      return;
    }
    if (action === 'reset-oa') {
      state.oaDraft.applicant = userProfile().displayName;
      state.oaDraft.department = userProfile().departmentName;
      state.oaDraft.approvalType = 'OA-ITSM-PERM';
      state.oaDraft.reason = '';
      render();
      return;
    }
    if (action === 'whale-range') {
      state.whaleFilter.range = target.dataset.range || '30';
      if (state.whaleFilter.range !== 'custom') state.whaleFilter.appliedRange = state.whaleFilter.range;
      render();
      return;
    }
    if (action === 'whale-reset') {
      state.whaleFilter = { range: '30', customStart: '', customEnd: '', appliedRange: '30' };
      render();
      return;
    }
    if (action === 'whale-query') {
      if (state.whaleFilter.customStart && state.whaleFilter.customEnd) {
        if (state.whaleFilter.customStart > state.whaleFilter.customEnd) {
          showToast('日期范围无效', '开始日期不能晚于结束日期', 'error');
          return;
        }
        state.whaleFilter.range = 'custom';
        state.whaleFilter.appliedRange = 'custom';
      } else {
        state.whaleFilter.appliedRange = state.whaleFilter.range || '30';
      }
      showToast('查询完成', '已按当前时间范围刷新看板数据', 'success');
      render();
      return;
    }    if (action === 'refresh-contacts') {
      loadContacts().finally(() => render());
      return;
    }
    if (action === 'contacts-page') {
      state.contactsPage = Number(target.dataset.page || 1);
      render();
      return;
    }
    if (action === 'open-window') {
      const url = target.dataset.url;
      if (url) window.open(url, '_blank');
      return;
    }
    if (action === 'tool') {
      showToast('附件入口', '当前原型暂未接入真实上传服务', 'info');
      return;
    }
    if (action === 'open-ticket') {
      openTicketDetail(target.dataset.ticketId);
      return;
    }
    if (action === 'close-ticket-detail') {
      closeTicketDetail();
      return;
    }
    if (action === 'confirm-ticket') {
      confirmTicket(target.dataset.ticketId);
      return;
    }
    if (action === 'reopen-ticket') {
      state.ticketAction = 'reopen';
      render();
      return;
    }
    if (action === 'submit-reopen') {
      reopenTicket(target.dataset.ticketId);
      return;
    }
    if (action === 'open-rating') {
      state.ticketAction = 'rating';
      render();
      return;
    }
    if (action === 'set-rating') {
      state.ratingScore = Number(target.dataset.score) || 0;
      render();
      return;
    }
    if (action === 'submit-rating') {
      submitRating(target.dataset.ticketId);
      return;
    }
    if (action === 'cancel-ticket-action') {
      resetTicketAction();
      render();
      return;
    }
  }

  function handleSubmit(event) {
    const form = event.target;
    if (form.id === 'login-form') {
      event.preventDefault();
      const values = new FormData(form);
      const tenantId = String(values.get('tenantId') || 'cza').trim();
      const account = String(values.get('account') || '').trim();
      const password = String(values.get('password') || '');
      if (!account || !password) {
        showToast('登录失败', '请输入账号和密码', 'error');
        return;
      }
      fetch('/api/v1/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Tenant-Id': tenantId },
        body: JSON.stringify({ grantType: 'PASSWORD', account, password })
      })
        .then((response) => response.json().then((body) => ({ ok: response.ok, status: response.status, body })))
        .then(({ ok, status, body }) => {
          if (!ok || body.code !== 'SUCCESS') {
            throw new Error(body.message || `HTTP ${status}`);
          }
          const data = body.data;
          session.accessToken = data.accessToken;
          session.refreshToken = data.refreshToken;
          session.user = data.user;
          session.tenant = data.tenant;
          session.roles = data.roles || [];
          session.permissionsVersion = data.permissionsVersion;
          state.activeRole = roleFromLogin(session.roles);
          state.actorUserId = data.user.userId;
          state.actorDisplayName = data.user.displayName;
          state.actorDepartment = data.user.departmentName;
          localStorage.setItem('itsm.session', JSON.stringify({ accessToken: data.accessToken, tenantId, user: data.user, tenant: data.tenant, roles: data.roles || [] }));
          state.loggedIn = true;
          render();
          showToast('登录成功', `欢迎 ${data.user.displayName}`, 'success');
          loadRemoteData().finally(() => render());
        })
        .catch((error) => showToast('登录失败', error.message, 'error'));
      return;
    }
    if (form.id === 'assistant-chat-form') {
      event.preventDefault();
      const text = String(new FormData(form).get('message') || '').trim();
      if (!text) return;
      state.assistantDraft = '';
      sendAssistantMessage(text);
      return;
    }

    if (form.id === 'colleague-chat-form') {
      event.preventDefault();
      const text = String(new FormData(form).get('message') || '').trim();
      const conversation = (state.colleagueConversations || []).find((item) => item.conversationId === state.chat.id);
      if (!conversation || !text) return;
      sendColleagueMessage(conversation, text);
      return;
    }
    if (form.id === 'oa-approval-form') {
      event.preventDefault();
      const values = new FormData(form);
      const reason = String(values.get('reason') || '').trim();
      if (!reason) {
        showToast('提交失败', '申请原因不能为空', 'error');
        return;
      }
      const typeNames = {
        'OA-ITSM-PERM': 'ITSM 权限申请',
        'OA-SMALLWHALE-PERM': '数小鲸看板权限申请'
      };
      state.oaRecords.push({
        id: `oa_${Date.now()}`,
        type: state.oaDraft.approvalType,
        typeName: typeNames[state.oaDraft.approvalType] || state.oaDraft.approvalType,
        applicant: String(values.get('applicant') || '').trim(),
        department: String(values.get('department') || '').trim(),
        reason,
        submittedAt: nowIso(),
        status: 'PENDING'
      });
      state.oaDraft.reason = '';
      showToast('审批已提交', '申请已进入管理员审批队列', 'success');
      render();
      return;
    }
    if (form.id === 'itsm-permission-form') {
      event.preventDefault();
      const values = new FormData(form);
      const reason = String(values.get('reason') || '').trim();
      if (!reason) {
        showToast('提交失败', '申请原因不能为空', 'error');
        return;
      }
      submitPermissionRequest(state.itsmApplication.requestType, reason);
      return;
    }
  }

  function handleInput(event) {
    const target = event.target;
    if (target.closest('#assistant-chat-form')) {
      state.assistantDraft = target.value;
      return;
    }
    if (target.closest('#colleague-chat-form')) {
      state.colleagueDraft = target.value;
      return;
    }
    if (target.name === 'applicant') {
      state.oaDraft.applicant = target.value;
      return;
    }
    if (target.name === 'department') {
      state.oaDraft.department = target.value;
      return;
    }
    if (target.name === 'reason') {
      state.oaDraft.reason = target.value;
      return;
    }
    if (target.closest && target.closest('#itsm-permission-form') && target.name === 'reason') {
      state.itsmApplication.reason = target.value;
      return;
    }
    if (target.dataset && target.dataset.bind === 'whaleCustomStart') {
      state.whaleFilter.customStart = target.value;
      state.whaleFilter.range = 'custom';
      return;
    }
    if (target.dataset && target.dataset.bind === 'whaleCustomEnd') {
      state.whaleFilter.customEnd = target.value;
      state.whaleFilter.range = 'custom';
      return;
    }
    if (target.dataset && target.dataset.bind === 'reopenReason') {
      state.reopenReason = target.value;
      return;
    }
    if (target.dataset && target.dataset.bind === 'ratingComment') {
      state.ratingComment = target.value;
      return;
    }
    if (target.dataset && target.dataset.bind === 'employeeDepartment') {
      state.employeeForm.departmentName = target.value;
      return;
    }
    if (target.dataset && target.dataset.bind === 'employeePhone') {
      state.employeeForm.phone = target.value;
      return;
    }
    if (target.dataset && target.dataset.bind === 'employeeEmail') {
      state.employeeForm.email = target.value;
      return;
    }
    if (target.dataset && (target.dataset.bind === 'portalSearch' || target.dataset.bind === 'contactKeyword')) {
      state[target.dataset.bind] = target.value;
      render();
      if (target.dataset.bind === 'portalSearch') window.setTimeout(() => { const input = document.querySelector('.search-box input'); if (input) { input.focus(); input.setSelectionRange(input.value.length, input.value.length); } }, 0);
    }
  }

  document.addEventListener('click', handleClick);
  document.addEventListener('submit', handleSubmit);
  document.addEventListener('input', handleInput);
  window.addEventListener('message', (event) => {
    if (event.data && event.data.type === 'ITSM_CLOSE') {
      state.view = 'WORKSPACE';
      state.profileOpen = false;
      render();
    }
  });
  let touchStart = null;
  document.addEventListener('pointerdown', (event) => {
    const workspace = event.target.closest ? event.target.closest('.message-workspace') : null;
    if (workspace && window.innerWidth <= 880 && event.pointerType === 'touch') {
      touchStart = { x: event.clientX, y: event.clientY };
    }
  }, { passive: true });
  document.addEventListener('pointerup', (event) => {
    if (!touchStart) return;
    const deltaX = event.clientX - touchStart.x;
    const deltaY = event.clientY - touchStart.y;
    touchStart = null;
    if (Math.abs(deltaX) < 60 || Math.abs(deltaX) <= Math.abs(deltaY) * 1.35) return;
    const workspace = document.querySelector('.message-workspace');
    if (!workspace) return;
    if (deltaX > 0) workspace.classList.add('show-chat-list');
    else workspace.classList.remove('show-chat-list');
  }, { passive: true });
  (function restoreSession() {
    try {
      const stored = JSON.parse(localStorage.getItem('itsm.session') || 'null');
      if (stored && stored.accessToken) {
        session.accessToken = stored.accessToken;
        session.refreshToken = stored.refreshToken || null;
        session.user = stored.user || null;
        session.tenant = stored.tenant || null;
        session.roles = stored.roles || [];
        state.activeRole = roleFromLogin(session.roles);
        if (stored.user) {
          state.actorUserId = stored.user.userId;
          state.actorDisplayName = stored.user.displayName;
          state.actorDepartment = stored.user.departmentName;
        }
        state.loggedIn = true;
      }
    } catch (error) {}
  })();

  const urlView = new URLSearchParams(window.location.search).get('view');
  if (urlView && ['MESSAGES', 'HISTORY', 'CONTACTS', 'WORKSPACE', 'ITSM', 'WHALE', 'APPROVALS'].includes(urlView)) {
    state.view = urlView;
  }

  let lastSidebarSignature = '';
  window.setInterval(() => {
    if (!state.loggedIn) return;
    Promise.all(state.colleagueConversations.map((conv) => loadColleagueMessages(conv))).then(() => {
      const signature = state.colleagueConversations.map((c) => c.userId + ':' + (c.messages.length) + ':' + (c.unreadCount || 0)).join('|');
      if (signature !== lastSidebarSignature) {
        lastSidebarSignature = signature;
        if (state.view === 'MESSAGES') render();
      }
    });
  }, 5000);

  if (state.loggedIn) {
    render();
    loadRemoteData().finally(() => render());
  } else {
    render();
  }
})();
