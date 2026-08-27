package com.cenziang.itsmserver.api;

import com.cenziang.itsmcommon.api.ApiResponse;
import com.cenziang.itsmpojo.dto.ColleagueMessageDtos;
import com.cenziang.itsmserver.service.ColleagueMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 同事消息接口。
 */
@Tag(name = "同事消息", description = "同事之间的一对一消息发送与读取")
@RestController
@RequestMapping("/api/v1/colleagues/messages")
public class ColleagueMessageController extends ControllerSupport {
    private final ColleagueMessageService colleagueMessageService;

    public ColleagueMessageController(ColleagueMessageService colleagueMessageService) {
        this.colleagueMessageService = colleagueMessageService;
    }

    @Operation(summary = "发送同事消息")
    @PostMapping
    public ApiResponse<ColleagueMessageDtos.MessageView> send(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                              @Valid @RequestBody ColleagueMessageDtos.SendRequest request,
                                                              HttpServletRequest httpServletRequest) {
        return ok(colleagueMessageService.send(context(httpServletRequest, tenantId), request), httpServletRequest);
    }

    @Operation(summary = "读取与某同事的消息（分页，最新在前）")
    @GetMapping
    public ApiResponse<ColleagueMessageDtos.MessagePage> list(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                               @Parameter(description = "对方用户主键", required = true) @RequestParam String peerUserId,
                                                               @Parameter(description = "游标：取该消息 ID 之前的历史") @RequestParam(required = false) Long beforeId,
                                                               @Parameter(description = "每页条数（默认 30，最大 50）") @RequestParam(required = false) Integer limit,
                                                               HttpServletRequest httpServletRequest) {
        return ok(colleagueMessageService.list(context(httpServletRequest, tenantId), peerUserId, beforeId, limit), httpServletRequest);
    }

    @Operation(summary = "读取当前用户的同事会话列表（含未读数与最后一条消息）")
    @GetMapping("/conversations")
    public ApiResponse<List<ColleagueMessageDtos.ConversationView>> conversations(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                                                  HttpServletRequest httpServletRequest) {
        return ok(colleagueMessageService.listConversations(context(httpServletRequest, tenantId)), httpServletRequest);
    }

    @Operation(summary = "标记与某同事的消息为已读")
    @PostMapping("/{peerUserId}/read")
    public ApiResponse<Void> markRead(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                      @Parameter(description = "对方用户主键", required = true) @PathVariable String peerUserId,
                                      HttpServletRequest httpServletRequest) {
        colleagueMessageService.markRead(context(httpServletRequest, tenantId), peerUserId);
        return ok(null, httpServletRequest);
    }
}
