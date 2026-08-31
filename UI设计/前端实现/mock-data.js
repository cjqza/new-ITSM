window.ITSM_DEMO_DATA = {
  appName: "ITSM 前端实现",
  appVersion: "v0.1",
  tenant: {
    tenantId: "tenant_001",
    tenantName: "示例企业"
  },
  profiles: {
    user: {
      key: "user",
      ssoCode: "USER-001",
      label: "用户",
      defaultRoute: "HOME",
      login: {
        code: "SUCCESS",
        message: "success",
        data: {
          accessToken: "demo-user-token",
          refreshToken: "demo-user-refresh",
          tokenType: "Bearer",
          expiresIn: 7200,
          user: {
            userId: "usr_10001",
            displayName: "张三",
            departmentName: "技术支持部"
          },
          tenant: {
            tenantId: "tenant_001",
            tenantName: "示例企业"
          },
          roles: ["USER"],
          permissionsVersion: "perm_20260825_01"
        },
        traceId: "trc_login_user_001",
        details: null
      },
      me: {
        code: "SUCCESS",
        message: "success",
        data: {
          userId: "usr_10001",
          displayName: "张三",
          departmentName: "技术支持部",
          tenantId: "tenant_001",
          roles: ["USER"],
          permissionsVersion: "perm_20260825_01"
        },
        traceId: "trc_me_user_001",
        details: null
      },
      permissions: {
        code: "SUCCESS",
        message: "success",
        data: {
          userId: "usr_10001",
          tenantId: "tenant_001",
          roles: [
            {
              roleId: "role_user",
              roleCode: "USER",
              roleName: "用户"
            }
          ],
          permissions: [
            "conversation:create",
            "conversation:read",
            "conversation:message",
            "ticket:read",
            "ticket:create",
            "ticket:confirm",
            "ticket:reopen",
            "ticket:rating"
          ],
          menus: ["HOME", "TICKET"],
          dataScope: {
            scopeType: "SELF",
            userIds: ["usr_10001"]
          },
          permissionsVersion: "perm_20260825_01"
        },
        traceId: "trc_perm_user_001",
        details: null
      }
    },
    support: {
      key: "support",
      ssoCode: "AGENT-001",
      label: "客服",
      defaultRoute: "HOME",
      login: {
        code: "SUCCESS",
        message: "success",
        data: {
          accessToken: "demo-support-token",
          refreshToken: "demo-support-refresh",
          tokenType: "Bearer",
          expiresIn: 7200,
          user: {
            userId: "usr_support_01",
            displayName: "客服一",
            departmentName: "一线支持组"
          },
          tenant: {
            tenantId: "tenant_001",
            tenantName: "示例企业"
          },
          roles: ["SUPPORT_AGENT"],
          permissionsVersion: "perm_20260825_01"
        },
        traceId: "trc_login_support_001",
        details: null
      },
      me: {
        code: "SUCCESS",
        message: "success",
        data: {
          userId: "usr_support_01",
          displayName: "客服一",
          departmentName: "一线支持组",
          tenantId: "tenant_001",
          roles: ["SUPPORT_AGENT"],
          permissionsVersion: "perm_20260825_01"
        },
        traceId: "trc_me_support_001",
        details: null
      },
      permissions: {
        code: "SUCCESS",
        message: "success",
        data: {
          userId: "usr_support_01",
          tenantId: "tenant_001",
          roles: [
            {
              roleId: "role_support_agent",
              roleCode: "SUPPORT_AGENT",
              roleName: "普通客服"
            }
          ],
          permissions: [
            "ticket:read",
            "ticket:accept",
            "ticket:classify",
            "ticket:resolve",
            "ticket:close",
            "conversation:read"
          ],
          menus: ["HOME", "TICKET"],
          dataScope: {
            scopeType: "BUSINESS_LINE",
            businessLineCodes: ["IT_SUPPORT"]
          },
          permissionsVersion: "perm_20260825_01"
        },
        traceId: "trc_perm_support_001",
        details: null
      }
    },
    admin: {
      key: "admin",
      ssoCode: "ADMIN-001",
      label: "管理员",
      defaultRoute: "CONFIG",
      login: {
        code: "SUCCESS",
        message: "success",
        data: {
          accessToken: "demo-admin-token",
          refreshToken: "demo-admin-refresh",
          tokenType: "Bearer",
          expiresIn: 7200,
          user: {
            userId: "usr_admin_01",
            displayName: "管理员",
            departmentName: "客服管理部"
          },
          tenant: {
            tenantId: "tenant_001",
            tenantName: "示例企业"
          },
          roles: ["SUPPORT_ADMIN"],
          permissionsVersion: "perm_20260825_02"
        },
        traceId: "trc_login_admin_001",
        details: null
      },
      me: {
        code: "SUCCESS",
        message: "success",
        data: {
          userId: "usr_admin_01",
          displayName: "管理员",
          departmentName: "客服管理部",
          tenantId: "tenant_001",
          roles: ["SUPPORT_ADMIN"],
          permissionsVersion: "perm_20260825_02"
        },
        traceId: "trc_me_admin_001",
        details: null
      },
      permissions: {
        code: "SUCCESS",
        message: "success",
        data: {
          userId: "usr_admin_01",
          tenantId: "tenant_001",
          roles: [
            {
              roleId: "role_support_admin",
              roleCode: "SUPPORT_ADMIN",
              roleName: "管理员客服"
            }
          ],
          permissions: [
            "ticket:read",
            "ticket:accept",
            "ticket:classify",
            "ticket:resolve",
            "ticket:close",
            "conversation:read",
            "dict:read",
            "dict:create",
            "dict:update",
            "dict:disable",
            "role:read",
            "role:permission:read"
          ],
          menus: ["HOME", "TICKET", "CONFIG"],
          dataScope: {
            scopeType: "TENANT"
          },
          permissionsVersion: "perm_20260825_02"
        },
        traceId: "trc_perm_admin_001",
        details: null
      }
    },
    supervisor: {
      key: "supervisor",
      ssoCode: "SUPERVISOR-001",
      label: "主管",
      defaultRoute: "HOME",
      login: {
        code: "SUCCESS",
        message: "success",
        data: {
          accessToken: "demo-supervisor-token",
          refreshToken: "demo-supervisor-refresh",
          tokenType: "Bearer",
          expiresIn: 7200,
          user: {
            userId: "usr_supervisor_01",
            displayName: "主管",
            departmentName: "质检中心"
          },
          tenant: {
            tenantId: "tenant_001",
            tenantName: "示例企业"
          },
          roles: ["SUPERVISOR"],
          permissionsVersion: "perm_20260825_01"
        },
        traceId: "trc_login_supervisor_001",
        details: null
      },
      me: {
        code: "SUCCESS",
        message: "success",
        data: {
          userId: "usr_supervisor_01",
          displayName: "主管",
          departmentName: "质检中心",
          tenantId: "tenant_001",
          roles: ["SUPERVISOR"],
          permissionsVersion: "perm_20260825_01"
        },
        traceId: "trc_me_supervisor_001",
        details: null
      },
      permissions: {
        code: "SUCCESS",
        message: "success",
        data: {
          userId: "usr_supervisor_01",
          tenantId: "tenant_001",
          roles: [
            {
              roleId: "role_supervisor",
              roleCode: "SUPERVISOR",
              roleName: "主管/质检"
            }
          ],
          permissions: [
            "ticket:read",
            "ticket:read:audit"
          ],
          menus: ["HOME", "TICKET"],
          dataScope: {
            scopeType: "TENANT_READONLY"
          },
          permissionsVersion: "perm_20260825_01"
        },
        traceId: "trc_perm_supervisor_001",
        details: null
      }
    }
  },
  quickQuestions: [
    "账号权限不足怎么排查",
    "工单现在到哪一步了",
    "如何转人工处理",
    "我可以重新打开吗",
    "帮我生成处理摘要"
  ],
  contacts: [
    {
      contactId: "usr_contact_01",
      userId: "usr_20002",
      displayName: "李四",
      departmentName: "人力资源部",
      positionName: "人事专员",
      status: "ONLINE",
      email: "lisi@example.com",
      phone: "***********"
    },
    {
      contactId: "usr_contact_02",
      userId: "usr_20003",
      displayName: "王五",
      departmentName: "财务部",
      positionName: "财务专员",
      status: "ONLINE",
      email: "wangwu@example.com",
      phone: "***********"
    },
    {
      contactId: "usr_contact_03",
      userId: "usr_20004",
      displayName: "赵六",
      departmentName: "行政部",
      positionName: "行政主管",
      status: "BUSY",
      email: "zhaoliu@example.com",
      phone: "***********"
    },
    {
      contactId: "usr_contact_04",
      userId: "usr_20005",
      displayName: "陈七",
      departmentName: "市场部",
      positionName: "市场经理",
      status: "OFFLINE",
      email: "chenqi@example.com",
      phone: "***********"
    },
    {
      contactId: "usr_contact_05",
      userId: "usr_20006",
      displayName: "客服一",
      departmentName: "一线支持组",
      positionName: "IT 客服",
      status: "ONLINE",
      email: "service01@example.com",
      phone: "***********"
    },
    {
      contactId: "usr_contact_admin",
      userId: "usr_admin_01",
      displayName: "管理员",
      departmentName: "客服管理部",
      positionName: "系统管理员",
      status: "ONLINE",
      email: "admin@example.com",
      phone: "***********"
    }
  ],
  colleagueConversations: [
    {
      conversationId: "col_con_01",
      contactId: "usr_contact_01",
      displayName: "李四",
      departmentName: "人力资源部",
      status: "ONLINE",
      lastMessage: "稍后我把入职流程的链接发你。",
      lastMessageAt: "2026-08-26T09:18:00Z",
      unreadCount: 1,
      messages: [
        {
          messageId: "col_msg_001",
          senderType: "CONTACT",
          senderName: "李四",
          content: "新同事的账号今天能开通吗？",
          createdAt: "2026-08-26T09:10:00Z"
        },
        {
          messageId: "col_msg_002",
          senderType: "USER",
          senderName: "张三",
          content: "已经提了，我正在跟进。",
          createdAt: "2026-08-26T09:12:00Z"
        },
        {
          messageId: "col_msg_003",
          senderType: "CONTACT",
          senderName: "李四",
          content: "收到，稍后我把入职流程的链接发你。",
          createdAt: "2026-08-26T09:18:00Z"
        }
      ]
    },
    {
      conversationId: "col_con_02",
      contactId: "usr_contact_02",
      displayName: "王五",
      departmentName: "财务部",
      status: "ONLINE",
      lastMessage: "报销单已经批完了。",
      lastMessageAt: "2026-08-25T16:42:00Z",
      unreadCount: 0,
      messages: [
        {
          messageId: "col_msg_011",
          senderType: "USER",
          senderName: "张三",
          content: "差旅报销的单据帮我看看。",
          createdAt: "2026-08-25T16:32:00Z"
        },
        {
          messageId: "col_msg_012",
          senderType: "CONTACT",
          senderName: "王五",
          content: "报销单已经批完了。",
          createdAt: "2026-08-25T16:42:00Z"
        }
      ]
    },
    {
      conversationId: "col_con_03",
      contactId: "usr_contact_03",
      displayName: "赵六",
      departmentName: "行政部",
      status: "BUSY",
      lastMessage: "工位申请我下午统一处理。",
      lastMessageAt: "2026-08-25T11:08:00Z",
      unreadCount: 0,
      messages: [
        {
          messageId: "col_msg_021",
          senderType: "USER",
          senderName: "张三",
          content: "我想申请一台外接显示器。",
          createdAt: "2026-08-25T11:02:00Z"
        },
        {
          messageId: "col_msg_022",
          senderType: "CONTACT",
          senderName: "赵六",
          content: "工位申请我下午统一处理。",
          createdAt: "2026-08-25T11:08:00Z"
        }
      ]
    }
  ],
  workbenchApps: [
    {
      appId: "wb_it_assistant",
      name: "IT 助手",
      description: "操作系统、网络、邮箱等问题自助咨询",
      type: "ASSISTANT",
      icon: "IT",
      tone: "blue"
    },
    {
      appId: "wb_service_chat",
      name: "客服会话",
      description: "与 IT 客服沟通并跟踪服务请求",
      type: "SERVICE",
      icon: "服",
      tone: "green"
    },
    {
      appId: "wb_contact_chat",
      name: "同事沟通",
      description: "查找同事并发起即时聊天",
      type: "CONTACT",
      icon: "联",
      tone: "violet"
    },
    {
      appId: "wb_agent_system",
      name: "客服工单 ITSM 系统",
      description: "客服处理、分类和关闭服务工单",
      type: "SYSTEM",
      icon: "工",
      tone: "orange"
    },
    {
      appId: "wb_ticket_history",
      name: "历史工单",
      description: "查看历史服务请求、处理记录和评价",
      type: "TICKET",
      icon: "史",
      tone: "slate"
    }
  ],
  whaleWorkload: [
    {
      agentId: "usr_support_01",
      agentName: "客服一",
      departmentName: "一线支持组",
      draftPerDay: 8,
      solvedPerDay: 6,
      unresolved: 7,
      over48Hours: 2
    },
    {
      agentId: "usr_support_02",
      agentName: "客服二",
      departmentName: "一线支持组",
      draftPerDay: 6,
      solvedPerDay: 5,
      unresolved: 5,
      over48Hours: 1
    },
    {
      agentId: "usr_support_03",
      agentName: "客服三",
      departmentName: "一线支持组",
      draftPerDay: 5,
      solvedPerDay: 4,
      unresolved: 4,
      over48Hours: 1
    },
    {
      agentId: "usr_support_04",
      agentName: "客服四",
      departmentName: "一线支持组",
      draftPerDay: 7,
      solvedPerDay: 5,
      unresolved: 6,
      over48Hours: 2
    },
    {
      agentId: "usr_retail_01",
      agentName: "零售客服一",
      departmentName: "零售支持组",
      draftPerDay: 3,
      solvedPerDay: 2,
      unresolved: 2,
      over48Hours: 0
    },
    {
      agentId: "usr_aftersales_01",
      agentName: "售后客服一",
      departmentName: "售后支持组",
      draftPerDay: 2,
      solvedPerDay: 2,
      unresolved: 1,
      over48Hours: 0
    }
  ],
  whaleDistribution: [
    { label: "系统与软件", count: 12 },
    { label: "网络问题", count: 8 },
    { label: "邮箱问题", count: 6 },
    { label: "账号权限", count: 5 },
    { label: "硬件与设备", count: 4 },
    { label: "其他", count: 3 }
  ],
  sessions: [
    {
      sessionId: "ses_01J8V9X6Q7",
      tenantId: "tenant_001",
      userId: "usr_10001",
      channel: "WORKBENCH",
      subject: "办公系统无法登录",
      status: "ACTIVE",
      summary: "用户正在描述办公系统登录失败，疑似账号权限问题。",
      ticketId: null,
      createdAt: "2026-08-25T08:10:00Z",
      lastMessageAt: "2026-08-25T08:16:38Z",
      messages: [
        {
          messageId: "msg_001",
          senderType: "USER",
          content: "我在办公系统登录时提示账号无权限。",
          createdAt: "2026-08-25T08:10:00Z"
        },
        {
          messageId: "msg_002",
          senderType: "ASSISTANT",
          content: "我先帮你确认一下现象。你是否刚刚修改过密码，或切换过浏览器？",
          createdAt: "2026-08-25T08:10:08Z"
        },
        {
          messageId: "msg_003",
          senderType: "USER",
          content: "没有，只是早上突然登录不上。",
          createdAt: "2026-08-25T08:11:02Z"
        },
        {
          messageId: "msg_004",
          senderType: "ASSISTANT",
          content: "根据描述，账号权限或租户映射都可能存在问题。你也可以直接转人工，我会保留上下文。",
          createdAt: "2026-08-25T08:11:12Z"
        }
      ]
    },
    {
      sessionId: "ses_01J8V9X6Q8",
      tenantId: "tenant_001",
      userId: "usr_10001",
      channel: "WORKBENCH",
      subject: "邮件提醒未收到",
      status: "TICKET_CREATED",
      summary: "已创建工单，等待客服受理。",
      ticketId: "tkt_20260825_003",
      createdAt: "2026-08-25T07:24:00Z",
      lastMessageAt: "2026-08-25T07:37:12Z",
      messages: [
        {
          messageId: "msg_011",
          senderType: "USER",
          content: "系统通知邮件经常收不到。",
          createdAt: "2026-08-25T07:24:00Z"
        },
        {
          messageId: "msg_012",
          senderType: "ASSISTANT",
          content: "我已经帮你创建了服务请求，当前正在等待客服受理。",
          createdAt: "2026-08-25T07:24:10Z"
        }
      ]
    },
    {
      sessionId: "ses_01J8V9X6Q9",
      tenantId: "tenant_001",
      userId: "usr_10001",
      channel: "WORKBENCH",
      subject: "打印机驱动异常",
      status: "ENDED",
      summary: "该会话对应工单已关闭，可查看历史记录与评价。",
      ticketId: "tkt_20260825_005",
      createdAt: "2026-08-24T14:20:00Z",
      lastMessageAt: "2026-08-24T15:05:00Z",
      messages: [
        {
          messageId: "msg_021",
          senderType: "USER",
          content: "打印机驱动更新后无法打印。",
          createdAt: "2026-08-24T14:20:00Z"
        },
        {
          messageId: "msg_022",
          senderType: "ASSISTANT",
          content: "问题已处理完成，服务请求已关闭，感谢反馈。",
          createdAt: "2026-08-24T15:05:00Z"
        }
      ]
    }
  ],
  tickets: [
    {
      ticketId: "tkt_20260825_001",
      ticketNo: "3053001",
      tenantId: "tenant_001",
      requester: {
        userId: "usr_10001",
        displayName: "张三",
        departmentName: "技术支持部"
      },
      title: "办公系统无法登录",
      description: "进入办公系统后提示账号无权限，使用浏览器无痕模式后仍然失败。",
      source: "AGENT_HANDOFF",
      status: "PENDING_ACCEPTANCE",
      priority: "HIGH",
      businessLineCode: "IT_SUPPORT",
      queueId: "queue_it_support",
      classification: {
        managementUnitId: "dict_unit_account",
        symptomId: "dict_symptom_login_failed",
        reasonId: null,
        solutionMethodId: null,
        customReason: null,
        customSolution: null
      },
      assignee: null,
      conversation: {
        sessionId: "ses_01J8V9X6Q7",
        summary: "用户反馈办公系统登录报无权限，已保留聊天上下文。",
        messageCount: 4
      },
      statusHistory: [
        {
          status: "NEW",
          occurredAt: "2026-08-25T08:13:10Z",
          operator: "系统",
          note: "会话转人工后创建工单"
        },
        {
          status: "PENDING_ACCEPTANCE",
          occurredAt: "2026-08-25T08:13:12Z",
          operator: "系统",
          note: "进入客服队列"
        }
      ],
      auditEvents: [
        {
          action: "TicketCreated",
          occurredAt: "2026-08-25T08:13:10Z",
          actor: "system"
        }
      ],
      resolution: null,
      rating: null,
      createdAt: "2026-08-25T08:13:10Z",
      updatedAt: "2026-08-25T08:13:12Z"
    },
    {
      ticketId: "tkt_20260825_002",
      ticketNo: "3053002",
      tenantId: "tenant_001",
      requester: {
        userId: "usr_10001",
        displayName: "张三",
        departmentName: "技术支持部"
      },
      title: "VPN 客户端无法连接",
      description: "VPN 客户端反复提示证书校验失败，切换网络后仍然无法连接。",
      source: "MANUAL",
      status: "IN_PROGRESS",
      priority: "MEDIUM",
      businessLineCode: "IT_SUPPORT",
      queueId: "queue_it_support",
      classification: {
        managementUnitId: "dict_unit_network",
        symptomId: "dict_symptom_vpn_failed",
        reasonId: "dict_reason_cert",
        solutionMethodId: "dict_solution_reinstall",
        customReason: null,
        customSolution: null
      },
      assignee: {
        userId: "usr_support_01",
        displayName: "客服一"
      },
      conversation: {
        sessionId: "ses_01J8V9X60A",
        summary: "用户手动建单，描述 VPN 客户端无法连接。",
        messageCount: 2
      },
      statusHistory: [
        {
          status: "PENDING_ACCEPTANCE",
          occurredAt: "2026-08-25T06:20:00Z",
          operator: "系统",
          note: "进入客服队列"
        },
        {
          status: "IN_PROGRESS",
          occurredAt: "2026-08-25T06:44:00Z",
          operator: "客服一",
          note: "已受理并开始排查"
        }
      ],
      auditEvents: [
        {
          action: "TicketAccepted",
          occurredAt: "2026-08-25T06:44:00Z",
          actor: "usr_support_01"
        }
      ],
      resolution: null,
      rating: null,
      createdAt: "2026-08-25T06:20:00Z",
      updatedAt: "2026-08-25T06:44:00Z"
    },
    {
      ticketId: "tkt_20260825_003",
      ticketNo: "3053003",
      tenantId: "tenant_001",
      requester: {
        userId: "usr_10001",
        displayName: "张三",
        departmentName: "技术支持部"
      },
      title: "邮件提醒未收到",
      description: "审批邮件和日常提醒有时收不到，怀疑与邮箱规则相关。",
      source: "AGENT_HANDOFF",
      status: "PENDING_USER_CONFIRM",
      priority: "MEDIUM",
      businessLineCode: "IT_SUPPORT",
      queueId: "queue_it_support",
      classification: {
        managementUnitId: "dict_unit_mail",
        symptomId: "dict_symptom_mail_rule",
        reasonId: "dict_reason_filter",
        solutionMethodId: "dict_solution_rebuild_rule",
        customReason: null,
        customSolution: "已调整邮箱规则并同步提醒策略"
      },
      assignee: {
        userId: "usr_support_01",
        displayName: "客服一"
      },
      conversation: {
        sessionId: "ses_01J8V9X6Q8",
        summary: "邮件提醒类问题已提交解决结果，等待用户确认。",
        messageCount: 2
      },
      statusHistory: [
        {
          status: "NEW",
          occurredAt: "2026-08-25T07:24:00Z",
          operator: "系统",
          note: "由会话生成工单"
        },
        {
          status: "IN_PROGRESS",
          occurredAt: "2026-08-25T07:30:10Z",
          operator: "客服一",
          note: "已受理"
        },
        {
          status: "PENDING_USER_CONFIRM",
          occurredAt: "2026-08-25T07:37:12Z",
          operator: "客服一",
          note: "已提交解决结果，等待用户确认"
        }
      ],
      auditEvents: [
        {
          action: "TicketResolutionSubmitted",
          occurredAt: "2026-08-25T07:37:12Z",
          actor: "usr_support_01"
        }
      ],
      resolution: {
        summary: "已调整邮箱规则并验证提醒恢复。",
        method: "重新配置规则"
      },
      rating: null,
      createdAt: "2026-08-25T07:24:00Z",
      updatedAt: "2026-08-25T07:37:12Z"
    },
    {
      ticketId: "tkt_20260825_004",
      ticketNo: "3053004",
      tenantId: "tenant_001",
      requester: {
        userId: "usr_10001",
        displayName: "张三",
        departmentName: "技术支持部"
      },
      title: "密码重置后仍无法登录",
      description: "执行密码重置后，旧设备仍然提示登录失败。",
      source: "MANUAL",
      status: "RESOLVED",
      priority: "LOW",
      businessLineCode: "IT_SUPPORT",
      queueId: "queue_it_support",
      classification: {
        managementUnitId: "dict_unit_account",
        symptomId: "dict_symptom_login_failed",
        reasonId: "dict_reason_sync",
        solutionMethodId: "dict_solution_reset_permission",
        customReason: null,
        customSolution: null
      },
      assignee: {
        userId: "usr_support_01",
        displayName: "客服一"
      },
      conversation: {
        sessionId: "ses_01J8V9X60B",
        summary: "账号同步延迟导致登录失败，已解决待确认。",
        messageCount: 3
      },
      statusHistory: [
        {
          status: "IN_PROGRESS",
          occurredAt: "2026-08-24T11:20:00Z",
          operator: "客服一",
          note: "开始处理"
        },
        {
          status: "PENDING_USER_CONFIRM",
          occurredAt: "2026-08-24T11:40:00Z",
          operator: "客服一",
          note: "提交解决结果"
        },
        {
          status: "RESOLVED",
          occurredAt: "2026-08-24T12:00:00Z",
          operator: "系统",
          note: "用户确认超时后自动置为已解决"
        }
      ],
      auditEvents: [
        {
          action: "TicketUserConfirmed",
          occurredAt: "2026-08-24T12:00:00Z",
          actor: "system"
        }
      ],
      resolution: {
        summary: "修复账号同步延迟问题，更新后重新登录成功。",
        method: "重置账号权限并刷新会话"
      },
      rating: null,
      createdAt: "2026-08-24T11:20:00Z",
      updatedAt: "2026-08-24T12:00:00Z"
    },
    {
      ticketId: "tkt_20260825_005",
      ticketNo: "3053005",
      tenantId: "tenant_001",
      requester: {
        userId: "usr_10001",
        displayName: "张三",
        departmentName: "技术支持部"
      },
      title: "打印机驱动异常",
      description: "打印队列堵塞，升级驱动后已恢复。",
      source: "MANUAL",
      status: "CLOSED",
      priority: "LOW",
      businessLineCode: "IT_SUPPORT",
      queueId: "queue_it_support",
      classification: {
        managementUnitId: "dict_unit_device",
        symptomId: "dict_symptom_printer",
        reasonId: "dict_reason_driver",
        solutionMethodId: "dict_solution_reinstall",
        customReason: null,
        customSolution: null
      },
      assignee: {
        userId: "usr_support_01",
        displayName: "客服一"
      },
      conversation: {
        sessionId: "ses_01J8V9X6Q9",
        summary: "打印机驱动更新后恢复打印，工单已关闭。",
        messageCount: 2
      },
      statusHistory: [
        {
          status: "RESOLVED",
          occurredAt: "2026-08-24T15:00:00Z",
          operator: "客服一",
          note: "解决方案提交成功"
        },
        {
          status: "CLOSED",
          occurredAt: "2026-08-24T15:10:00Z",
          operator: "客服一",
          note: "确认关闭"
        }
      ],
      auditEvents: [
        {
          action: "TicketClosed",
          occurredAt: "2026-08-24T15:10:00Z",
          actor: "usr_support_01"
        }
      ],
      resolution: {
        summary: "驱动重新安装后恢复正常打印。",
        method: "重装驱动"
      },
      rating: {
        score: 5,
        comment: "处理及时，反馈清楚。"
      },
      createdAt: "2026-08-24T14:20:00Z",
      updatedAt: "2026-08-24T15:10:00Z"
    },
    {
      ticketId: "tkt_20260825_006",
      ticketNo: "3053006",
      tenantId: "tenant_001",
      requester: {
        userId: "usr_20002",
        displayName: "李四",
        departmentName: "人力资源部"
      },
      title: "门户页面空白",
      description: "登录门户后页面空白，只能看到顶部导航。",
      source: "MANUAL",
      status: "REOPENED",
      priority: "HIGH",
      businessLineCode: "HR_SYSTEM",
      queueId: "queue_hr_system",
      classification: {
        managementUnitId: "dict_unit_browser",
        symptomId: "dict_symptom_page_blank",
        reasonId: "dict_reason_cache",
        solutionMethodId: "dict_solution_refresh",
        customReason: null,
        customSolution: "已清理缓存并重新发布前端资源"
      },
      assignee: {
        userId: "usr_support_02",
        displayName: "客服二"
      },
      conversation: {
        sessionId: "ses_01J8V9X6QX",
        summary: "用户认为问题未彻底解决，已重新进入队列。",
        messageCount: 5
      },
      statusHistory: [
        {
          status: "RESOLVED",
          occurredAt: "2026-08-23T09:00:00Z",
          operator: "客服二",
          note: "曾提交解决"
        },
        {
          status: "REOPENED",
          occurredAt: "2026-08-23T10:15:00Z",
          operator: "用户",
          note: "用户认为未完全解决"
        }
      ],
      auditEvents: [
        {
          action: "TicketReopened",
          occurredAt: "2026-08-23T10:15:00Z",
          actor: "usr_20002"
        }
      ],
      resolution: {
        summary: "门户前端资源缓存问题已处理。",
        method: "刷新静态资源缓存"
      },
      rating: null,
      createdAt: "2026-08-23T08:40:00Z",
      updatedAt: "2026-08-23T10:15:00Z"
    },
    {
      ticketId: "tkt_20260825_007",
      ticketNo: "3053007",
      tenantId: "tenant_001",
      requester: {
        userId: "usr_30003",
        displayName: "王五",
        departmentName: "财务部"
      },
      title: "审批流提交异常",
      description: "财务审批流提交后一直处于排队状态。",
      source: "MANUAL",
      status: "IN_PROGRESS",
      priority: "MEDIUM",
      businessLineCode: "ERP",
      queueId: "queue_erp",
      classification: {
        managementUnitId: "dict_unit_process",
        symptomId: "dict_symptom_submit_pending",
        reasonId: "dict_reason_queue",
        solutionMethodId: "dict_solution_review",
        customReason: null,
        customSolution: null
      },
      assignee: {
        userId: "usr_support_03",
        displayName: "客服三"
      },
      conversation: {
        sessionId: "ses_01J8V9X6QY",
        summary: "审批流排队超时，等待进一步排查。",
        messageCount: 3
      },
      statusHistory: [
        {
          status: "PENDING_ACCEPTANCE",
          occurredAt: "2026-08-25T07:00:00Z",
          operator: "系统",
          note: "进入队列"
        },
        {
          status: "IN_PROGRESS",
          occurredAt: "2026-08-25T07:25:00Z",
          operator: "客服三",
          note: "已受理"
        }
      ],
      auditEvents: [],
      resolution: null,
      rating: null,
      createdAt: "2026-08-25T07:00:00Z",
      updatedAt: "2026-08-25T07:25:00Z"
    }
  ],
  dictionaries: {
    BUSINESS_SYSTEM: {
      dictType: "BUSINESS_SYSTEM",
      title: "业务系统",
      description: "用于工单归属和页面入口分类",
      items: [
        {
          itemId: "dict_bs_office",
          code: "OFFICE",
          name: "办公系统",
          parentId: null,
          enabled: true,
          sort: 10,
          version: 4,
          description: "OA、协作与办公入口",
          updatedAt: "2026-08-25T08:00:00Z",
          updatedBy: "admin"
        },
        {
          itemId: "dict_bs_vpn",
          code: "VPN",
          name: "远程接入",
          parentId: null,
          enabled: true,
          sort: 20,
          version: 2,
          description: "VPN、证书与安全接入",
          updatedAt: "2026-08-24T14:20:00Z",
          updatedBy: "admin"
        },
        {
          itemId: "dict_bs_mail",
          code: "MAIL",
          name: "邮件系统",
          parentId: null,
          enabled: true,
          sort: 30,
          version: 3,
          description: "邮箱、提醒与通知",
          updatedAt: "2026-08-24T11:12:00Z",
          updatedBy: "admin"
        },
        {
          itemId: "dict_bs_hr",
          code: "HR",
          name: "人力资源系统",
          parentId: null,
          enabled: true,
          sort: 40,
          version: 1,
          description: "HR 门户与审批流",
          updatedAt: "2026-08-23T09:15:00Z",
          updatedBy: "admin"
        }
      ]
    },
    MANAGEMENT_UNIT: {
      dictType: "MANAGEMENT_UNIT",
      title: "管理单元",
      description: "工单一级归类，用于决定处理场景",
      items: [
        {
          itemId: "dict_unit_account",
          code: "ACCOUNT",
          name: "账号",
          parentId: null,
          enabled: true,
          sort: 10,
          version: 4,
          description: "账号与身份相关问题",
          updatedAt: "2026-08-25T08:01:00Z",
          updatedBy: "admin"
        },
        {
          itemId: "dict_unit_network",
          code: "NETWORK",
          name: "网络",
          parentId: null,
          enabled: true,
          sort: 20,
          version: 2,
          description: "网络、专线、VPN 与证书",
          updatedAt: "2026-08-24T14:05:00Z",
          updatedBy: "admin"
        },
        {
          itemId: "dict_unit_mail",
          code: "MAIL",
          name: "邮件",
          parentId: null,
          enabled: true,
          sort: 30,
          version: 2,
          description: "邮箱、提醒、通知与规则",
          updatedAt: "2026-08-24T12:12:00Z",
          updatedBy: "admin"
        },
        {
          itemId: "dict_unit_device",
          code: "DEVICE",
          name: "终端设备",
          parentId: null,
          enabled: true,
          sort: 40,
          version: 1,
          description: "打印机、驱动与外设",
          updatedAt: "2026-08-24T15:12:00Z",
          updatedBy: "admin"
        },
        {
          itemId: "dict_unit_browser",
          code: "BROWSER",
          name: "浏览器",
          parentId: null,
          enabled: true,
          sort: 50,
          version: 1,
          description: "门户、页面渲染与缓存",
          updatedAt: "2026-08-23T09:00:00Z",
          updatedBy: "admin"
        },
        {
          itemId: "dict_unit_process",
          code: "PROCESS",
          name: "流程",
          parentId: null,
          enabled: true,
          sort: 60,
          version: 1,
          description: "审批流与业务流程",
          updatedAt: "2026-08-23T10:12:00Z",
          updatedBy: "admin"
        }
      ]
    },
    SYMPTOM: {
      dictType: "SYMPTOM",
      title: "症状",
      description: "问题表现，用于引导诊断和筛选",
      items: [
        {
          itemId: "dict_symptom_login_failed",
          code: "LOGIN_FAILED",
          name: "登录失败",
          parentId: "dict_unit_account",
          enabled: true,
          sort: 10,
          version: 3,
          description: "账号无法登录或提示无权限",
          updatedAt: "2026-08-25T08:02:00Z",
          updatedBy: "admin"
        },
        {
          itemId: "dict_symptom_vpn_failed",
          code: "VPN_FAILED",
          name: "VPN 连接失败",
          parentId: "dict_unit_network",
          enabled: true,
          sort: 20,
          version: 2,
          description: "VPN 证书或网络连接异常",
          updatedAt: "2026-08-24T14:04:00Z",
          updatedBy: "admin"
        },
        {
          itemId: "dict_symptom_mail_rule",
          code: "MAIL_RULE",
          name: "邮件提醒未收到",
          parentId: "dict_unit_mail",
          enabled: true,
          sort: 30,
          version: 2,
          description: "通知邮件或提醒未到达",
          updatedAt: "2026-08-24T12:00:00Z",
          updatedBy: "admin"
        },
        {
          itemId: "dict_symptom_printer",
          code: "PRINTER",
          name: "打印异常",
          parentId: "dict_unit_device",
          enabled: true,
          sort: 40,
          version: 1,
          description: "打印队列、驱动和外设异常",
          updatedAt: "2026-08-24T15:00:00Z",
          updatedBy: "admin"
        },
        {
          itemId: "dict_symptom_page_blank",
          code: "PAGE_BLANK",
          name: "页面空白",
          parentId: "dict_unit_browser",
          enabled: true,
          sort: 50,
          version: 1,
          description: "页面渲染为空或只显示框架",
          updatedAt: "2026-08-23T09:10:00Z",
          updatedBy: "admin"
        },
        {
          itemId: "dict_symptom_submit_pending",
          code: "SUBMIT_PENDING",
          name: "提交后排队",
          parentId: "dict_unit_process",
          enabled: true,
          sort: 60,
          version: 1,
          description: "业务流程卡在提交或排队阶段",
          updatedAt: "2026-08-23T10:00:00Z",
          updatedBy: "admin"
        }
      ]
    },
    REASON: {
      dictType: "REASON",
      title: "原因",
      description: "问题原因，可标准化或自定义",
      items: [
        {
          itemId: "dict_reason_permission",
          code: "PERMISSION_MISSING",
          name: "权限缺失",
          parentId: "dict_symptom_login_failed",
          enabled: true,
          sort: 10,
          version: 3,
          description: "账号未分配业务权限",
          updatedAt: "2026-08-25T08:03:00Z",
          updatedBy: "admin"
        },
        {
          itemId: "dict_reason_cert",
          code: "CERT_FAILED",
          name: "证书失效",
          parentId: "dict_symptom_vpn_failed",
          enabled: true,
          sort: 20,
          version: 2,
          description: "VPN 证书或凭证失效",
          updatedAt: "2026-08-24T14:02:00Z",
          updatedBy: "admin"
        },
        {
          itemId: "dict_reason_filter",
          code: "MAIL_FILTER",
          name: "过滤规则",
          parentId: "dict_symptom_mail_rule",
          enabled: true,
          sort: 30,
          version: 2,
          description: "邮件被规则过滤或拦截",
          updatedAt: "2026-08-24T12:05:00Z",
          updatedBy: "admin"
        },
        {
          itemId: "dict_reason_driver",
          code: "DRIVER_ERROR",
          name: "驱动异常",
          parentId: "dict_symptom_printer",
          enabled: true,
          sort: 40,
          version: 1,
          description: "终端设备驱动损坏或过旧",
          updatedAt: "2026-08-24T15:01:00Z",
          updatedBy: "admin"
        },
        {
          itemId: "dict_reason_cache",
          code: "CACHE_DIRTY",
          name: "缓存异常",
          parentId: "dict_symptom_page_blank",
          enabled: true,
          sort: 50,
          version: 1,
          description: "前端静态资源缓存过旧",
          updatedAt: "2026-08-23T09:12:00Z",
          updatedBy: "admin"
        },
        {
          itemId: "dict_reason_queue",
          code: "QUEUE_DELAY",
          name: "队列延迟",
          parentId: "dict_symptom_submit_pending",
          enabled: true,
          sort: 60,
          version: 1,
          description: "流程队列排队超时",
          updatedAt: "2026-08-23T10:05:00Z",
          updatedBy: "admin"
        },
        {
          itemId: "dict_reason_sync",
          code: "SYNC_DELAY",
          name: "同步延迟",
          parentId: "dict_symptom_login_failed",
          enabled: true,
          sort: 70,
          version: 1,
          description: "权限或账号同步尚未完成",
          updatedAt: "2026-08-24T08:30:00Z",
          updatedBy: "admin"
        }
      ]
    },
    SOLUTION_METHOD: {
      dictType: "SOLUTION_METHOD",
      title: "解决方法",
      description: "标准解决手段，可与自定义说明配合使用",
      items: [
        {
          itemId: "dict_solution_reset_permission",
          code: "RESET_PERMISSION",
          name: "补充账号权限",
          parentId: "dict_reason_permission",
          enabled: true,
          sort: 10,
          version: 1,
          description: "刷新账号权限和租户映射",
          updatedAt: "2026-08-25T08:04:00Z",
          updatedBy: "admin"
        },
        {
          itemId: "dict_solution_reinstall",
          code: "REINSTALL",
          name: "重新安装客户端",
          parentId: "dict_reason_driver",
          enabled: true,
          sort: 20,
          version: 1,
          description: "重装驱动或客户端组件",
          updatedAt: "2026-08-24T15:02:00Z",
          updatedBy: "admin"
        },
        {
          itemId: "dict_solution_refresh",
          code: "REFRESH_CACHE",
          name: "刷新缓存",
          parentId: "dict_reason_cache",
          enabled: true,
          sort: 30,
          version: 1,
          description: "清理浏览器缓存并重新加载资源",
          updatedAt: "2026-08-23T09:14:00Z",
          updatedBy: "admin"
        },
        {
          itemId: "dict_solution_review",
          code: "QUEUE_REVIEW",
          name: "人工复核队列",
          parentId: "dict_reason_queue",
          enabled: true,
          sort: 40,
          version: 1,
          description: "由人工确认流程节点和队列配置",
          updatedAt: "2026-08-23T10:08:00Z",
          updatedBy: "admin"
        },
        {
          itemId: "dict_solution_rebuild_rule",
          code: "REBUILD_RULE",
          name: "重建提醒规则",
          parentId: "dict_reason_filter",
          enabled: true,
          sort: 50,
          version: 1,
          description: "修订邮箱提醒规则和触发条件",
          updatedAt: "2026-08-24T12:07:00Z",
          updatedBy: "admin"
        }
      ]
    }
  },
  roles: [
    {
      roleId: "role_user",
      roleCode: "USER",
      roleName: "用户",
      enabled: true,
      description: "本租户内的普通业务用户",
      permissionCount: 8
    },
    {
      roleId: "role_support_agent",
      roleCode: "SUPPORT_AGENT",
      roleName: "普通客服",
      enabled: true,
      description: "一线客服处理与关闭工单",
      permissionCount: 6
    },
    {
      roleId: "role_support_admin",
      roleCode: "SUPPORT_ADMIN",
      roleName: "管理员客服",
      enabled: true,
      description: "可维护字典和查看角色权限",
      permissionCount: 12
    },
    {
      roleId: "role_supervisor",
      roleCode: "SUPERVISOR",
      roleName: "主管/质检",
      enabled: true,
      description: "只读查看工单与审计",
      permissionCount: 2
    }
  ],
  rolePermissions: {
    role_user: {
      roleId: "role_user",
      roleCode: "USER",
      permissions: [
        {
          permissionCode: "conversation:create",
          permissionName: "新建会话",
          permissionType: "BUTTON"
        },
        {
          permissionCode: "conversation:message",
          permissionName: "发送消息",
          permissionType: "BUTTON"
        },
        {
          permissionCode: "ticket:create",
          permissionName: "创建工单",
          permissionType: "BUTTON"
        },
        {
          permissionCode: "ticket:confirm",
          permissionName: "确认解决",
          permissionType: "BUTTON"
        },
        {
          permissionCode: "ticket:reopen",
          permissionName: "重开工单",
          permissionType: "BUTTON"
        },
        {
          permissionCode: "ticket:rating",
          permissionName: "工单评价",
          permissionType: "BUTTON"
        }
      ],
      menus: ["HOME", "TICKET"],
      dataScope: {
        scopeType: "SELF"
      }
    },
    role_support_agent: {
      roleId: "role_support_agent",
      roleCode: "SUPPORT_AGENT",
      permissions: [
        {
          permissionCode: "ticket:read",
          permissionName: "查看工单",
          permissionType: "MENU"
        },
        {
          permissionCode: "ticket:accept",
          permissionName: "受理工单",
          permissionType: "BUTTON"
        },
        {
          permissionCode: "ticket:classify",
          permissionName: "更新分类",
          permissionType: "BUTTON"
        },
        {
          permissionCode: "ticket:resolve",
          permissionName: "提交解决",
          permissionType: "BUTTON"
        },
        {
          permissionCode: "ticket:close",
          permissionName: "关闭工单",
          permissionType: "BUTTON"
        }
      ],
      menus: ["HOME", "TICKET"],
      dataScope: {
        scopeType: "BUSINESS_LINE",
        businessLineCodes: ["IT_SUPPORT"]
      }
    },
    role_support_admin: {
      roleId: "role_support_admin",
      roleCode: "SUPPORT_ADMIN",
      permissions: [
        {
          permissionCode: "ticket:read",
          permissionName: "查看工单",
          permissionType: "MENU"
        },
        {
          permissionCode: "ticket:accept",
          permissionName: "受理工单",
          permissionType: "BUTTON"
        },
        {
          permissionCode: "ticket:classify",
          permissionName: "更新分类",
          permissionType: "BUTTON"
        },
        {
          permissionCode: "ticket:resolve",
          permissionName: "提交解决",
          permissionType: "BUTTON"
        },
        {
          permissionCode: "ticket:close",
          permissionName: "关闭工单",
          permissionType: "BUTTON"
        },
        {
          permissionCode: "dict:read",
          permissionName: "读取字典",
          permissionType: "MENU"
        },
        {
          permissionCode: "dict:create",
          permissionName: "新增字典项",
          permissionType: "BUTTON"
        },
        {
          permissionCode: "dict:update",
          permissionName: "更新字典项",
          permissionType: "BUTTON"
        },
        {
          permissionCode: "dict:disable",
          permissionName: "停用字典项",
          permissionType: "BUTTON"
        },
        {
          permissionCode: "role:read",
          permissionName: "查询角色",
          permissionType: "MENU"
        },
        {
          permissionCode: "role:permission:read",
          permissionName: "查询角色权限",
          permissionType: "MENU"
        }
      ],
      menus: ["HOME", "TICKET", "CONFIG"],
      dataScope: {
        scopeType: "TENANT"
      }
    },
    role_supervisor: {
      roleId: "role_supervisor",
      roleCode: "SUPERVISOR",
      permissions: [
        {
          permissionCode: "ticket:read",
          permissionName: "查看工单",
          permissionType: "MENU"
        },
        {
          permissionCode: "ticket:read:audit",
          permissionName: "查看审计",
          permissionType: "MENU"
        }
      ],
      menus: ["HOME", "TICKET"],
      dataScope: {
        scopeType: "TENANT_READONLY"
      }
    }
  },
  stats: {
    workspaceName: "ITSM 桌面工单处理系统"
  }
};
