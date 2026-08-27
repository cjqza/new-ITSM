(function () {
  'use strict';

  const API = {
    sendCode: '/api/v1/auth/register/send-code',
    register: '/api/v1/auth/register',
    login: '/api/v1/auth/login',
    me: '/api/v1/auth/me'
  };

  const tabs = document.querySelectorAll('.auth-tab');
  const forms = {
    login: document.getElementById('login-form'),
    register: document.getElementById('register-form')
  };
  const result = document.getElementById('result');
  const sendCodeButton = document.getElementById('send-code-button');
  const codeHint = document.getElementById('code-hint');
  let countdown = null;

  function showResult(type, title, detail) {
    result.classList.remove('hidden', 'ok', 'err');
    result.classList.add(type);
    result.innerHTML = `<strong>${escapeHtml(title)}</strong><div>${escapeHtml(detail || '')}</div>`;
  }

  function escapeHtml(value) {
    return String(value == null ? '' : value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function switchTab(tab) {
    tabs.forEach((item) => item.classList.toggle('active', item === tab));
    Object.keys(forms).forEach((key) => forms[key].classList.toggle('active', key === tab.dataset.tab));
    result.classList.add('hidden');
  }

  async function request(url, options) {
    const response = await fetch(url, options);
    const body = await response.json().catch(() => ({}));
    if (!response.ok || body.code !== 'SUCCESS') {
      const message = body.message || `HTTP ${response.status}`;
      throw new Error(message + (body.details ? `：${JSON.stringify(body.details)}` : ''));
    }
    return body;
  }

  function tenantHeader(form) {
    return String(new FormData(form).get('tenantId') || 'tenant_001').trim();
  }

  function startCountdown(seconds) {
    if (countdown) window.clearInterval(countdown);
    let left = seconds;
    const update = () => {
      sendCodeButton.disabled = left > 0;
      sendCodeButton.textContent = left > 0 ? `重新获取（${left}s）` : '获取验证码';
      if (left <= 0) window.clearInterval(countdown);
      left -= 1;
    };
    update();
    countdown = window.setInterval(update, 1000);
  }

  async function handleSendCode(event) {
    event.preventDefault();
    const form = forms.register;
    const tenantId = tenantHeader(form);
    const phone = String(new FormData(form).get('phone') || '').trim();
    if (!phone) {
      showResult('err', '请输入手机号');
      return;
    }
    try {
      const body = await request(API.sendCode, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Tenant-Id': tenantId },
        body: JSON.stringify({ phone })
      });
      const data = body.data || {};
      if (data.code) {
        codeHint.textContent = `开发模式验证码：${data.code}（有效期 ${data.expiresIn} 秒）`;
        codeHint.classList.remove('hidden');
        form.querySelector('input[name="code"]').value = data.code;
      }
      startCountdown(Number(data.expiresIn) || 60);
      showResult('ok', '验证码已发送', `已发送到 ${phone}`);
    } catch (error) {
      showResult('err', '发送失败', error.message);
    }
  }

  async function handleRegister(event) {
    event.preventDefault();
    const form = forms.register;
    const tenantId = tenantHeader(form);
    const values = Object.fromEntries(new FormData(form).entries());
    try {
      const body = await request(API.register, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Tenant-Id': tenantId },
        body: JSON.stringify({
          username: values.username,
          phone: values.phone,
          password: values.password,
          confirmPassword: values.confirmPassword,
          code: values.code
        })
      });
      const data = body.data || {};
      showResult('ok', '注册成功', `userId：${data.userId}，登录账号：${data.loginName}。请切换到“登录”标签登录。`);
      form.reset();
    } catch (error) {
      showResult('err', '注册失败', error.message);
    }
  }

  async function handleLogin(event) {
    event.preventDefault();
    const form = forms.login;
    const tenantId = tenantHeader(form);
    const values = Object.fromEntries(new FormData(form).entries());
    try {
      const body = await request(API.login, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Tenant-Id': tenantId },
        body: JSON.stringify({ grantType: 'PASSWORD', account: values.account, password: values.password })
      });
      const data = body.data || {};
      localStorage.setItem('itsm.session', JSON.stringify({
        accessToken: data.accessToken,
        refreshToken: data.refreshToken || null,
        tenantId,
        user: data.user,
        tenant: data.tenant,
        roles: data.roles || []
      }));
      showResult('ok', '登录成功', '正在跳转到用户工作台…');
      window.location.replace('user-portal.html');
    } catch (error) {
      showResult('err', '登录失败', error.message);
    }
  }

  tabs.forEach((tab) => tab.addEventListener('click', () => switchTab(tab)));
  sendCodeButton.addEventListener('click', handleSendCode);
  forms.register.addEventListener('submit', handleRegister);
  forms.login.addEventListener('submit', handleLogin);
})();
