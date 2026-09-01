"""
ITSM AI Customer Service Agent (LangGraph)

LangGraph workflow:
1. classify  - classify the user problem
2. diagnose  - generate structured diagnosis
3. decide    - decide self-resolve or handoff to human

Uses DashScope-compatible OpenAI endpoint (Alibaba Cloud).
"""

import os
import json
import re
from typing import TypedDict, Annotated, Literal
from pydantic import BaseModel, Field

from langgraph.graph import StateGraph, END
from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, HumanMessage

# ---------- Configuration ----------
DASHSCOPE_API_KEY = os.getenv("DASHSCOPE_API_KEY", "")
DASHSCOPE_BASE_URL = os.getenv("DASHSCOPE_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1")
DASHSCOPE_MODEL = os.getenv("DASHSCOPE_MODEL", "qwen-plus")

# ---------- System Prompt ----------
SYSTEM_PROMPT = """你是企业内部资深桌面运维工程师，负责处理员工终端桌面、办公软件、账号权限、网络接入、外设设备等相关问题。你具备标准化 IT 运维排查思路，输出方案必须符合企业 IT 安全规范，操作步骤可落地、无风险，语言简洁易懂，面向非技术员工。

## 核心任务

基于用户提交的工单标题、问题描述、环境信息、错误截图，完成问题诊断、排查指引、方案输出，自动评估解决难度，给出是否需要转人工现场处理的建议，输出结构化结果供工单系统解析。

## 输入信息说明

系统将传入以下工单信息：
- 工单标题：问题核心概述
- 问题描述：故障现象、出现时机、复现步骤、已尝试操作
- 环境信息：操作系统版本、软件版本、网络环境、设备型号
- 附件信息：错误截图、日志文件（如有）

## 标准处理流程

1. **信息校验** 关键信息缺失时（无错误提示、无系统版本、未说明具体场景），主动列出需要补充的信息，此时置信度低于 0.5，标记为待补充。 信息充足则进入正式排查。
2. **问题分级与分类**
   - 优先级：
     - 高：完全无法办公（无法开机、系统崩溃、账号锁定、VPN 完全断连）
     - 中：功能受限但可临时替代（软件报错、打印机异常、部分功能不可用）
     - 低：咨询类、优化类、不影响正常办公
   - 分类枚举：SYSTEM系统 / SOFTWARE软件 / ACCOUNT账号 / NETWORK网络 / PERIPHERAL外设 / OTHER其他
3. **方案输出原则**
   - 遵循「从简到繁、从软到硬」，先给出无需重启、无风险的快速排查，再给出深度解决方案。
   - 每一步操作明确、通俗，告知用户预期结果。
   - 涉及数据风险的操作（重置配置、重装软件），必须先提示备份重要数据。
   - 标准 FAQ 问题直接输出知识库标准答案。
4. **转人工评估** 符合转人工条件的，明确给出转人工建议与原因；不符合的说明可自行尝试解决。

---

## 输出格式规范

严格按照以下结构输出，便于系统自动提取字段，对接工单状态机：

【问题摘要】 用 1 句话概括问题核心与最可能的原因

【问题分类】 从枚举中选择一个：SYSTEM / SOFTWARE / ACCOUNT / NETWORK / PERIPHERAL / OTHER

【优先级判定】 高 / 中 / 低

【排查步骤】 按顺序列出 3-5 步可执行的快速排查操作，每步一行，标注序号

【解决方案】 分点给出详细解决操作，关键步骤标注注意事项；有多套方案时按优先级排序

【是否建议转人工】 是 / 否 （选 "是" 必须补充原因）

【置信度评分】 0.0-1.0 之间的数值

【预防建议】 1-2 条避免同类问题再次发生的建议

---

## 强制转人工触发条件

出现以下任意一种情况，必须标记「建议转人工」：

1. 涉及硬件物理损坏、硬件更换、拆机操作
2. 需要管理员权限才能执行的操作（域账号解锁、权限变更、安装受管控软件）
3. 连续给出 3 套解决方案仍无法解决的问题
4. 系统崩溃、无法开机、反复蓝屏等严重系统故障
5. 涉及公司安全策略、存在违规操作风险的问题
6. 用户明确要求上门处理

## 禁忌规则

1. 严禁给出绕过企业 IT 安全策略的操作
2. 严禁回答超出桌面运维范围的问题
3. 严禁猜测不确定的原因，不确定时明确说明并建议转人工
4. 严禁给出可能导致数据丢失的操作而不提示备份
5. 避免使用专业技术术语，用员工易懂的通俗表达
"""

# ---------- State ----------
class AgentState(TypedDict):
    user_message: str
    chat_history: list  # previous messages [{role, content}]
    classification: str  # SYSTEM/SOFTWARE/ACCOUNT/NETWORK/PERIPHERAL/OTHER
    priority: str  # high/medium/low
    response: str  # full structured response
    confidence: float
    should_handoff: bool
    handoff_reason: str
    error: str

# ---------- LLM Helper ----------
def get_llm():
    return ChatOpenAI(
        model=DASHSCOPE_MODEL,
        openai_api_key=DASHSCOPE_API_KEY or "sk-placeholder",
        openai_api_base=DASHSCOPE_BASE_URL,
        temperature=0.3,
        max_tokens=2000,
    )

# ---------- Nodes ----------
def classify_node(state: AgentState) -> dict:
    """Classify the user problem into category and priority."""
    llm = get_llm()
    msgs = [
        SystemMessage(content="You are an IT support classifier. Given a user message, classify it into one of: SYSTEM, SOFTWARE, ACCOUNT, NETWORK, PERIPHERAL, OTHER. Also rate priority: high, medium, low. Respond ONLY with JSON: {\"category\": \"...\", \"priority\": \"...\"}"),
        HumanMessage(content=state["user_message"]),
    ]
    try:
        result = llm.invoke(msgs)
        text = result.content.strip()
        # Extract JSON from response
        json_match = re.search(r'\{[^}]+\}', text)
        if json_match:
            data = json.loads(json_match.group())
            return {
                "classification": data.get("category", "OTHER"),
                "priority": data.get("priority", "medium"),
            }
    except Exception as e:
        pass
    return {"classification": "OTHER", "priority": "medium"}


def diagnose_node(state: AgentState) -> dict:
    """Generate the full structured diagnosis response."""
    llm = get_llm()
    
    # Build conversation context
    history_text = ""
    for msg in state.get("chat_history", []):
        role = "用户" if msg.get("role") == "user" else "IT助手"
        history_text += f"{role}: {msg.get('content', '')}\n"
    
    context = f"""问题分类: {state['classification']}
优先级: {state['priority']}

历史对话:
{history_text}

当前用户消息: {state['user_message']}"""

    msgs = [
        SystemMessage(content=SYSTEM_PROMPT),
        HumanMessage(content=context),
    ]
    
    try:
        result = llm.invoke(msgs)
        response_text = result.content.strip()
        
        # Parse confidence
        confidence = 0.7
        conf_match = re.search(r'【置信度评分】\s*([\d.]+)', response_text)
        if conf_match:
            confidence = float(conf_match.group(1))
        
        # Parse handoff
        should_handoff = False
        handoff_reason = ""
        handoff_match = re.search(r'【是否建议转人工】\s*(是|否)', response_text)
        if handoff_match:
            should_handoff = handoff_match.group(1) == "是"
        if should_handoff:
            reason_match = re.search(r'【是否建议转人工】\s*是[：:]\s*(.+?)(?:\n|【)', response_text, re.DOTALL)
            if reason_match:
                handoff_reason = reason_match.group(1).strip()
            elif not handoff_reason:
                handoff_reason = "问题复杂，建议人工处理"
        
        # Update classification/priority from response if available
        cat_match = re.search(r'【问题分类】\s*(SYSTEM|SOFTWARE|ACCOUNT|NETWORK|PERIPHERAL|OTHER)', response_text)
        pri_match = re.search(r'【优先级判定】\s*(高|中|低)', response_text)
        
        classification = cat_match.group(1) if cat_match else state["classification"]
        priority_map = {"高": "high", "中": "medium", "低": "low"}
        priority = priority_map.get(pri_match.group(1), state["priority"]) if pri_match else state["priority"]
        
        return {
            "response": response_text,
            "confidence": confidence,
            "should_handoff": should_handoff,
            "handoff_reason": handoff_reason,
            "classification": classification,
            "priority": priority,
        }
    except Exception as e:
        return {
            "response": f"抱歉，AI 服务暂时不可用（{str(e)[:100]}），请稍后重试或直接转人工处理。",
            "confidence": 0.0,
            "should_handoff": True,
            "handoff_reason": f"AI service error: {str(e)[:200]}",
            "error": str(e),
        }


def decide_node(state: AgentState) -> dict:
    """Final decision: validate the response and set final state."""
    # Force handoff if confidence is too low
    if state["confidence"] < 0.5 and not state["should_handoff"]:
        return {
            "should_handoff": True,
            "handoff_reason": state.get("handoff_reason", "") or "置信度过低，建议人工进一步排查",
        }
    return {}


# ---------- Graph ----------
def build_graph():
    graph = StateGraph(AgentState)
    
    graph.add_node("classify", classify_node)
    graph.add_node("diagnose", diagnose_node)
    graph.add_node("decide", decide_node)
    
    graph.set_entry_point("classify")
    graph.add_edge("classify", "diagnose")
    graph.add_edge("diagnose", "decide")
    graph.add_edge("decide", END)
    
    return graph.compile()


# ---------- Public API ----------
_agent = None

def get_agent():
    global _agent
    if _agent is None:
        _agent = build_graph()
    return _agent


def invoke_agent(user_message: str, chat_history: list = None) -> dict:
    """Run the AI agent and return structured result."""
    agent = get_agent()
    result = agent.invoke({
        "user_message": user_message,
        "chat_history": chat_history or [],
        "classification": "OTHER",
        "priority": "medium",
        "response": "",
        "confidence": 0.0,
        "should_handoff": False,
        "handoff_reason": "",
        "error": "",
    })
    return {
        "response": result["response"],
        "classification": result["classification"],
        "priority": result["priority"],
        "confidence": result["confidence"],
        "shouldHandoff": result["should_handoff"],
        "handoffReason": result["handoff_reason"],
    }
