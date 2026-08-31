package com.cenziang.itsmserver.api;

import com.cenziang.itsmcommon.api.ApiResponse;
import com.cenziang.itsmpojo.dto.ContactDtos;
import com.cenziang.itsmserver.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 企业联系人接口。
 */
@Tag(name = "联系人", description = "查询本租户企业联系人")
@RestController
@RequestMapping("/api/v1/contacts")
public class ContactController extends ControllerSupport {
    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @Operation(summary = "查询联系人", description = "返回本租户除自己外的用户列表，按部门排序")
    @GetMapping
    public ApiResponse<List<ContactDtos.ContactView>> list(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                           @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
                                                           HttpServletRequest httpServletRequest) {
        return ok(contactService.listContacts(context(httpServletRequest, tenantId), keyword), httpServletRequest);
    }
}
