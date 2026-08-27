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

    @Operation(summary = "读取与某同事的消息")
    @GetMapping
    public ApiResponse<List<ColleagueMessageDtos.MessageView>> list(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                                    @Parameter(description = "对方用户主键", required = true) @RequestParam String peerUserId,
                                                                    HttpServletRequest httpServletRequest) {
        return ok(colleagueMessageService.list(context(httpServletRequest, tenantId), peerUserId), httpServletRequest);
    }
}
