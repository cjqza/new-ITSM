(function () {
  'use strict';

  const DATA = window.ITSM_DEMO_DATA;
  const appRoot = document.getElementById('app');
  const toastRoot = document.getElementById('toast-root');
  const currentUserId = DATA.profiles.user.login.data.user.userId;

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
      reason: ''
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
    return DATA.profiles.user.login.data.user;
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
    return state.activeRole === 'SUPPORT';
  }

  function userSessions() {
    return DATA.sessions.filter((session) => session.userId === currentUserId).slice().sort((a, b) => new Date(b.lastMessageAt || b.createdAt) - new Date(a.lastMessageAt || a.createdAt));
  }

  function userTickets() {
    return DATA.tickets.filter((ticket) => ticket.requester.userId === currentUserId).slice().sort((a, b) => new Date(b.updatedAt || b.createdAt) - new Date(a.updatedAt || a.createdAt));
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
    return userSessions()[0] || DATA.sessions[0];
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
      WHALE: renderWhale
    };
    (views[state.view] || renderMessages)();
  }

  function renderLogin() {
    appRoot.innerHTML = `
      <div class="login-screen">
        <section class="login-card">
          <div class="login-brand"><span class="brand-mark">ITSM</span><div><strong>企业服务台</strong><small>用户统一登录入口</small></div></div>
          <div class="login-heading"><span class="eyebrow">企业身份登录</span><h1>进入 IT 服务工作台</h1><p>登录后可访问消息、历史工单、联系人和工作台。</p></div>
          <form id="login-form" class="login-form">
            <label for="login-role">演示身份</label>
            <select id="login-role" name="role" class="login-select"><option value="USER">普通用户</option><option value="SUPPORT">客服</option><option value="ADMIN">管理员</option></select>
            <label for="sso-code">统一身份凭证</label>
            <input id="sso-code" name="ssoCode" value="USER-001" autocomplete="username">
            <button type="submit" class="primary-button login-submit">登录</button>
          </form>
          <div class="login-note">普通用户可申请权限；客服与管理员可打开 ITSM 工单系统。</div>
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
    const profile = activeProfile().login.data.user;
    const historyCount = historyTickets().length;
    const unreadCount = (DATA.colleagueConversations || []).reduce((sum, item) => sum + (item.unreadCount || 0), 0);
    const viewLabels = {
      MESSAGES: '消息',
      HISTORY: '历史消息',
      CONTACTS: '联系人',
      WORKSPACE: '工作台',
      ITSM: 'ITSM 工单',
      WHALE: '数鲸看板'
    };
    const views = [
      { key: 'MESSAGES', label: '消息' },
      { key: 'HISTORY', label: '历史消息' },
      { key: 'CONTACTS', label: '联系人' },
      { key: 'WORKSPACE', label: '工作台' }
    ];
    views.push({ key: 'ITSM', label: 'ITSM 工单' }, { key: 'WHALE', label: '数鲸看板' });
    const nav = views.map((item) => `
      <button class="portal-nav-button ${item.key === activeView ? 'active' : ''}" data-action="nav" data-view="${item.key}">
        <span class="portal-nav-label">${item.label}</span>
        ${item.key === 'HISTORY' ? `<span class="portal-nav-count">${historyCount}</span>` : ''}
        ${item.key === 'MESSAGES' && unreadCount ? `<span class="portal-nav-count accent">${unreadCount}</span>` : ''}
      </button>
    `).join('');

    appRoot.innerHTML = `
      <div class="user-portal">
        <div class="portal-top"><span><strong>ITSM</strong> · 用户工作台</span><span>${escapeHtml(DATA.tenant.tenantName)}</span></div>
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
      </div>
    `;
  }

  function renderProfilePopover(profile) {
    return `
      <div class="popover-backdrop" data-action="toggle-profile"></div>
      <section class="profile-popover">
        <div class="profile-popover-head"><span class="avatar large">${escapeHtml(profile.displayName.slice(0, 1))}</span><div><h2>${escapeHtml(profile.displayName)}</h2><p>${escapeHtml(profile.departmentName)}</p></div></div>
        <div class="profile-grid"><div><span>用户编号</span><strong>${escapeHtml(profile.userId)}</strong></div><div><span>所属企业</span><strong>${escapeHtml(DATA.tenant.tenantName)}</strong></div><div><span>当前角色</span><strong>${escapeHtml(activeProfile().label)}</strong></div><div><span>邮箱</span><strong>${escapeHtml(profile.email || '企业内部邮箱')}</strong></div></div>
        <div class="popover-actions"><button class="primary-button" data-action="toggle-profile">返回工作台</button><button class="ghost-button" data-action="logout">退出登录</button></div>
      </section>
    `;
  }

  function renderMessages() {
    const sessions = userSessions();
    const colleagues = (DATA.colleagueConversations || []).slice().sort((a, b) => new Date(b.lastMessageAt) - new Date(a.lastMessageAt));
    const items = [
      { key: 'assistant', type: 'ASSISTANT', name: 'IT 助手', subtitle: '智能服务助手', preview: '操作系统、网络、邮箱等问题都可以问我', time: '现在', avatar: 'IT', tone: 'blue' },
      ...colleagues.map((item) => ({ key: item.conversationId, type: 'COLLEAGUE', name: item.displayName, subtitle: item.departmentName, preview: item.lastMessage, time: formatDate(item.lastMessageAt), avatar: item.displayName.slice(0, 1), tone: '' }))
    ];
    const filtered = state.portalSearch ? items.filter((item) => [item.name, item.subtitle, item.preview].join(' ').toLowerCase().includes(state.portalSearch.trim().toLowerCase())) : items;
    const sidebar = filtered.map((item) => `
      <button class="chat-list-item ${state.chat.type === item.type && state.chat.id === item.key ? 'active' : ''}" data-action="open-chat" data-type="${item.type}" data-id="${item.key}">
        <span class="chat-list-avatar ${item.tone}">${escapeHtml(item.avatar)}</span><span class="chat-list-copy"><span class="chat-list-top"><strong>${escapeHtml(item.name)}</strong><time>${escapeHtml(item.time)}</time></span><span class="chat-list-sub">${escapeHtml(item.subtitle)}</span><span class="chat-list-preview">${escapeHtml(item.preview)}</span></span>
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
    const successTicket = state.handoffSuccess ? DATA.tickets.find((ticket) => ticket.ticketId === state.handoffSuccess) : null;

    return `
      <div class="chat-body">
        <div class="chat-scroll">
          <div class="welcome"><div class="welcome-mark">IT</div><h2>你好，${escapeHtml(userProfile().displayName)}</h2><p>你可以描述问题，也可以从服务选项开始。</p></div>
          <div class="category-grid">${categoryCards}</div>
          <div class="message-thread">${messageHtml || '<div class="empty-state">还没有消息，先选择一个分类开始。</div>'}</div>
          ${successTicket ? renderHandoffSuccessCard(successTicket) : ''}
          ${state.handoffOpen ? renderHandoffForm() : ''}
        </div>
        <form class="composer" id="assistant-chat-form"><div class="composer-tools"><button type="button" data-action="tool" data-tool="attach" title="附件">＋</button><button type="button" data-action="tool" data-tool="file" title="文件">文</button><button type="button" data-action="tool" data-tool="screenshot" title="截图">截</button></div><textarea class="textarea-input" name="message" placeholder="请输入问题，例如：我的电脑无法连接网络">${escapeHtml(state.assistantDraft)}</textarea><div class="composer-footer"><button type="button" class="ghost-button" data-action="open-handoff">转人工</button><button class="primary-button" type="submit">发送</button></div></form>
      </div>
    `;
  }

  function renderHandoffForm() {
    return `<div class="handoff-card"><div class="handoff-head"><div><span>转人工服务</span><h3>把当前问题交给人工客服</h3></div><button class="close-button" data-action="close-handoff" title="关闭">×</button></div><div class="handoff-grid"><div><span>问题标题</span><strong>${escapeHtml(state.handoff.title || currentSession().summary || 'IT 服务咨询')}</strong></div><div><span>问题分类</span><strong>${escapeHtml(state.handoff.category)}</strong></div><div><span>处理队列</span><strong>IT 支持 · 普通队列</strong></div></div><p class="handoff-note">提交后系统会保留当前对话上下文，并生成可跟踪的工单。</p><div class="handoff-actions"><button class="ghost-button" data-action="close-handoff">暂不转人工</button><button class="primary-button" data-action="confirm-handoff">确认转人工</button></div></div>`;
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
    const contacts = (DATA.contacts || []).filter((contact) => !keyword || [contact.displayName, contact.departmentName, contact.positionName, contact.email].join(' ').toLowerCase().includes(keyword));
    const rows = contacts.map((contact) => `<button class="contact-row" data-action="open-contact" data-contact-id="${escapeHtml(contact.contactId)}"><span class="contact-avatar ${contact.status.toLowerCase()}">${escapeHtml(contact.displayName.slice(0, 1))}</span><span class="contact-copy"><strong>${escapeHtml(contact.displayName)}</strong><small>${escapeHtml(contact.departmentName)} · ${escapeHtml(contact.positionName)}</small></span><span class="contact-presence"><i></i>${contact.status === 'ONLINE' ? '在线' : contact.status === 'BUSY' ? '忙碌' : '离线'}</span><span class="contact-open">发消息</span></button>`).join('');
    renderShell('CONTACTS', `<section class="page-section"><div class="page-heading"><div><h1>联系人</h1><p>查找同事并开始聊天</p></div><div class="heading-actions"><input class="text-input contact-search" data-bind="contactKeyword" placeholder="搜索姓名、部门或职位" value="${escapeHtml(state.contactKeyword)}"><button class="ghost-button" data-action="refresh-contacts">刷新</button></div></div><div class="contacts-layout"><div class="contacts-list panel">${rows || '<div class="empty-state">没有匹配的联系人</div>'}</div><aside class="contact-side panel"><div class="contact-side-placeholder"><span>联</span><h3>选择一位同事</h3><p>查看资料或发起企业内聊天。</p></div></aside></div></section>`);
  }

  function renderWorkspace() {
    const apps = (DATA.workbenchApps || []).filter((app) => app.type !== 'TICKET');
    const accessApps = [
      {
        appId: 'wb_whale_board',
        name: '数鲸看板',
        description: '查询 IT 支持工作量与未解决工单分布',
        type: 'WHALE',
        icon: '鲸',
        tone: 'cyan'
      }
    ];
    const allApps = [...apps, ...accessApps];
    const cards = allApps.map((app) => `<button class="workspace-app ${app.tone}" data-action="open-app" data-app-id="${escapeHtml(app.appId)}"><span class="app-icon">${escapeHtml(app.icon)}</span><span class="app-copy"><strong>${escapeHtml(app.name)}</strong><small>${escapeHtml(app.description)}</small></span><span class="app-open">打开</span></button>`).join('');
    renderShell('WORKSPACE', `<section class="page-section"><div class="page-heading"><div><h1>工作台</h1><p>IT 助手、客服会话、同事与 ITSM 系统入口</p></div><span class="status-chip ok">当前用户工作台</span></div><div class="workspace-grid panel">${cards || '<div class="empty-state">暂无工作台应用</div>'}</div></section>`);
  }

  function renderItsm() {
    const hasAccess = isSupportAgent();
    const admin = DATA.profiles.admin.login.data.user;
    let body;
    if (hasAccess) {
      body = `<iframe class="itsm-system-frame" src="./index.html?embedded=1" title="ITSM 工单系统"></iframe>`;
    } else if (state.itsmApplication.status === 'SUBMITTED') {
      body = `
        <div class="itsm-permission-state submitted">
          <div class="permission-state-icon">已</div>
          <h2>权限申请已提交</h2>
          <p>管理员会在审批通过后为你添加 ITSM 权限。你可以通过首页联系管理员，提醒尽快处理。</p>
          <div class="permission-contact-card"><span>当前管理员</span><strong>${escapeHtml(admin.displayName)}</strong><small>${escapeHtml(admin.departmentName)} · ${escapeHtml(admin.userId)}</small></div>
          <div class="permission-state-actions"><button class="ghost-button" data-action="contact-admin">首页联系管理员</button><button class="primary-button" data-action="open-oa-form">重新填写申请</button></div>
        </div>
      `;
    } else if (state.itsmApplication.status === 'REQUESTING') {
      body = `
        <div class="itsm-permission-state requesting">
          <div class="permission-state-icon">权</div>
          <h2>申请 ITSM 权限</h2>
          <p>提交申请后，管理员将根据你的账号角色和工作需要完成权限配置。</p>
          <form id="itsm-permission-form" class="itsm-permission-form">
            <label>申请账号</label><input class="text-input" value="${escapeHtml(activeProfile().login.data.user.displayName)}" readonly>
            <label for="itsm-permission-reason">申请原因</label><textarea id="itsm-permission-reason" class="textarea-input" name="reason" placeholder="请说明需要使用 ITSM 工单系统的原因">${escapeHtml(state.itsmApplication.reason)}</textarea>
            <div class="permission-form-actions"><button type="button" class="ghost-button" data-action="cancel-itsm-permission">取消</button><button type="submit" class="primary-button">申请权限</button></div>
          </form>
        </div>
      `;
    } else {
      body = `
        <div class="itsm-permission-state denied">
          <div class="permission-state-icon">!</div>
          <h2>您好，您暂时没有权限</h2>
          <p>当前账号尚未开通 ITSM 工单系统的访问权限，请联系管理员或在线提交权限申请。</p>
          <div class="permission-state-actions"><button class="ghost-button" data-action="nav" data-view="MESSAGES">返回首页</button><button class="primary-button" data-action="open-itsm-permission">申请权限</button></div>
        </div>
      `;
    }
    renderShell('ITSM', `<section class="itsm-system-container"><div class="itsm-system-toolbar"><div><span class="status-chip ${hasAccess ? 'ok' : 'info'}">ITSM 工单系统</span><small>${hasAccess ? '客服工单处理工作台' : '工单权限申请与处理系统'}</small></div>${hasAccess ? '<span class="muted">在门户内完成工单查询与处理</span>' : ''}</div>${body}</section>`);
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
      render();
      return;
    }
    if (action === 'open-oa-form') {
      state.itsmApplication.status = 'REQUESTING';
      state.itsmApplication.reason = '';
      render();
      return;
    }
    if (action === 'contact-admin') {
      const admin = (DATA.contacts || []).find((contact) => contact.userId === 'usr_admin_01');
      if (!admin) return;
      let conversation = (DATA.colleagueConversations || []).find((item) => item.contactId === admin.contactId);
      if (!conversation) {
        conversation = {
          conversationId: `col_con_admin_${Date.now()}`,
          contactId: admin.contactId,
          displayName: admin.displayName,
          departmentName: admin.departmentName,
          status: admin.status,
          lastMessage: '请联系我处理 ITSM 权限申请',
          lastMessageAt: nowIso(),
          unreadCount: 0,
          messages: [
            {
              messageId: `col_msg_admin_${Date.now()}`,
              senderType: 'SYSTEM',
              senderName: '系统',
              content: `已为你打开与 ${admin.displayName} 的会话，可说明 ITSM 权限申请事项。`,
              createdAt: nowIso()
            }
          ]
        };
        DATA.colleagueConversations.unshift(conversation);
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
      render();
      return;
    }
    if (action === 'open-chat') {
      state.view = 'MESSAGES';
      state.chat = { type: target.dataset.type || 'ASSISTANT', id: target.dataset.id || 'assistant' };
      state.colleagueDraft = '';
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
      const contact = (DATA.contacts || []).find((item) => item.contactId === target.dataset.contactId);
      if (!contact) return;
      let conversation = (DATA.colleagueConversations || []).find((item) => item.contactId === contact.contactId);
      if (!conversation) {
        conversation = { conversationId: `col_con_${Date.now()}`, contactId: contact.contactId, displayName: contact.displayName, departmentName: contact.departmentName, status: contact.status, lastMessage: '你们已经成为会话伙伴', lastMessageAt: nowIso(), unreadCount: 0, messages: [{ messageId: `col_msg_${Date.now()}`, senderType: 'SYSTEM', senderName: '系统', content: `你和 ${contact.displayName} 已经可以开始聊天。`, createdAt: nowIso() }] };
        DATA.colleagueConversations.unshift(conversation);
      }
      state.view = 'MESSAGES';
      state.chat = { type: 'COLLEAGUE', id: conversation.conversationId };
      render();
      return;
    }
    if (action === 'category') {
      state.assistantCategory = target.dataset.key;
      state.handoff.category = target.dataset.label;
      const session = currentSession();
      if (session) {
        session.messages.push({ messageId: `msg_${Date.now()}`, senderType: 'USER', content: target.dataset.label, createdAt: nowIso() }, { messageId: `msg_${Date.now() + 1}`, senderType: 'ASSISTANT', content: `已选择${target.dataset.label}，你可以直接描述现象；需要人工处理时点击转人工即可。`, createdAt: nowIso() });
        session.lastMessageAt = nowIso();
      }
      render();
      return;
    }
    if (action === 'open-handoff') {
      state.handoffOpen = true;
      state.handoff.title = state.handoff.title || currentSession().summary || currentSession().subject || 'IT 服务咨询';
      state.handoff.description = state.assistantDraft || '用户请求从 IT 助手转接人工客服。';
      render();
      return;
    }
    if (action === 'close-handoff') {
      state.handoffOpen = false;
      render();
      return;
    }
    if (action === 'confirm-handoff') {
      const session = currentSession();
      if (session) {
        session.status = 'HANDOFF_PENDING';
        session.lastMessageAt = nowIso();
        session.messages.push({ messageId: `msg_${Date.now()}`, senderType: 'ASSISTANT', content: `已提交转人工服务，当前工单为“${state.handoff.title || '转人工服务'}”，正在安排客服接入。`, createdAt: nowIso() });
      }
      const ticketId = `tkt_${Date.now()}`;
      const ticket = { ticketId, ticketNo: String(3000000 + Math.floor(Math.random() * 900000)), tenantId: DATA.tenant.tenantId, requester: { userId: userProfile().userId, displayName: userProfile().displayName, departmentName: userProfile().departmentName }, title: state.handoff.title || '转人工服务', description: state.handoff.description, source: 'AGENT_HANDOFF', status: 'PENDING_ACCEPTANCE', priority: state.handoff.priority, businessLineCode: 'IT_SUPPORT', queueId: 'queue_it_support', classification: { managementUnitId: '', symptomId: '', reasonId: null, solutionMethodId: null, customReason: null, customSolution: null }, assignee: null, conversation: { sessionId: session ? session.sessionId : '', summary: state.handoff.title, messageCount: session ? session.messages.length : 1 }, statusHistory: [{ status: 'NEW', occurredAt: nowIso(), operator: '系统', note: 'IT 助手转人工后创建服务请求' }, { status: 'PENDING_ACCEPTANCE', occurredAt: nowIso(), operator: '系统', note: '进入客服队列' }], auditEvents: [{ action: 'TicketCreated', occurredAt: nowIso(), actor: 'assistant' }], resolution: null, rating: null, createdAt: nowIso(), updatedAt: nowIso() };
      DATA.tickets.unshift(ticket);
      if (session) session.ticketId = ticketId;
      state.handoffOpen = false;
      state.handoffSuccess = ticketId;
      showToast('转人工成功', `已生成工单 ${ticket.ticketNo}`, 'success');
      render();
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
      state.oaDraft.applicant = activeProfile().login.data.user.displayName;
      state.oaDraft.department = activeProfile().login.data.user.departmentName;
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
      showToast('联系人已刷新', '当前显示企业联系人', 'success');
      return;
    }
    if (action === 'tool') {
      showToast('附件入口', '当前原型暂未接入真实上传服务', 'info');
      return;
    }
    if (action === 'open-ticket') {
      showToast('工单详情', `工单 ${target.dataset.ticketId} 可在历史工单或原客服视图中查看。`, 'info');
      return;
    }
  }

  function handleSubmit(event) {
    const form = event.target;
    if (form.id === 'login-form') {
      event.preventDefault();
      const code = String(new FormData(form).get('ssoCode') || '').trim();
      const role = String(new FormData(form).get('role') || 'USER');
      const profile = DATA.profiles[role.toLowerCase()] || DATA.profiles.user;
      if (!code || code !== profile.ssoCode) {
        showToast('登录失败', 'TOKEN_INVALID', 'error');
        return;
      }
      state.activeRole = role.toUpperCase();
      state.actorUserId = profile.login.data.user.userId;
      state.actorDisplayName = profile.login.data.user.displayName;
      state.actorDepartment = profile.login.data.user.departmentName;
      state.loggedIn = true;
      showToast('登录成功', `已进入 ${profile.login.data.user.displayName} 的工作台`, 'success');
      render();
      return;
    }
    if (form.id === 'assistant-chat-form') {
      event.preventDefault();
      const text = String(new FormData(form).get('message') || '').trim();
      if (!text) return;
      const session = currentSession();
      if (session) {
        session.messages.push({ messageId: `msg_${Date.now()}`, senderType: 'USER', content: text, createdAt: nowIso() });
        session.lastMessageAt = nowIso();
      }
      state.assistantDraft = '';
      if (/转人工|人工客服|人工服务/.test(text)) {
        state.handoff.title = state.handoff.title || session.summary || '转人工服务';
        state.handoff.description = text;
        state.handoffOpen = true;
        if (session) session.messages.push({ messageId: `msg_${Date.now() + 1}`, senderType: 'ASSISTANT', content: '好的，我将为你转接人工客服。当前对话上下文会一并保留，客服接入后可继续沟通。', createdAt: nowIso() });
      } else {
        const map = [
          { match: /网络|网线|wifi|vpn|无法联网|断网/i, reply: '建议先检查网络连接，再尝试重启路由器或切换网络。如果仍异常，我可以为你转人工排查。' },
          { match: /邮箱|邮件|收不到|发不出去|outlook/i, reply: '可以尝试清理邮箱缓存并检查收件规则。如果收发仍异常，建议转人工查看邮箱服务状态。' },
          { match: /系统|开机|蓝屏|死机|windows|更新/i, reply: '可以先记录蓝屏代码，并检查最近安装的更新。若重启后问题仍存在，可以转人工进一步排查。' },
          { match: /账号|密码|权限|登录/i, reply: '可以尝试重新登录并确认账号权限。若仍提示异常，我可以直接帮你转人工核实账号状态。' },
          { match: /软件|安装|办公|office|版本/i, reply: '可以先确认软件版本和安装权限，再尝试修复安装。需要人工协助时可以直接点击转人工。' }
        ];
        const matched = map.find((item) => item.match.test(text));
        if (session) session.messages.push({ messageId: `msg_${Date.now() + 2}`, senderType: 'ASSISTANT', content: matched ? matched.reply : '收到，我会结合当前上下文继续帮你整理。需要转人工时可以直接点击转人工。', createdAt: nowIso() });
      }
      render();
      return;
    }

    if (form.id === 'colleague-chat-form') {
      event.preventDefault();
      const text = String(new FormData(form).get('message') || '').trim();
      const conversation = (DATA.colleagueConversations || []).find((item) => item.conversationId === state.chat.id);
      if (!conversation || !text) return;
      conversation.messages.push({ messageId: `col_msg_${Date.now()}`, senderType: 'USER', senderName: userProfile().displayName, content: text, createdAt: nowIso() });
      conversation.lastMessage = text;
      conversation.lastMessageAt = nowIso();
      state.colleagueDraft = '';
      render();
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
      state.itsmApplication.reason = reason;
      state.itsmApplication.status = 'SUBMITTED';
      showToast('权限申请已提交', '管理员将在审批后为你添加 ITSM 权限', 'success');
      render();
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
  render();
})();
