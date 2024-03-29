package io.ssafy.mallook.domain.script.api;

import com.theokanning.openai.service.OpenAiService;
import io.ssafy.mallook.domain.script.application.ScriptService;
import io.ssafy.mallook.domain.script.dto.request.ScriptCreatDto;
import io.ssafy.mallook.domain.script.dto.request.ScriptDeleteListDto;
import io.ssafy.mallook.domain.script.dto.response.ScriptDetailDto;
import io.ssafy.mallook.domain.script.dto.response.ScriptListDto;
import io.ssafy.mallook.global.common.BaseResponse;
import io.ssafy.mallook.global.common.code.SuccessCode;
import io.ssafy.mallook.global.security.user.UserSecurityDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/scripts")
@Log4j2
public class ScriptController {

    private final ScriptService scriptService;

    @GetMapping
    public ResponseEntity<BaseResponse<Slice<ScriptListDto>>> getScriptList(
            @AuthenticationPrincipal UserSecurityDTO principal,
            @PageableDefault(size = 20,
                    sort = "id",
                    direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) Long cursor) {

        cursor = !Objects.isNull(cursor) ? cursor : scriptService.getMaxScriptId() + 1;

        return BaseResponse.success(
                SuccessCode.SELECT_SUCCESS,
                scriptService.getScriptList(cursor, principal.getId(), pageable)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authService.authorizeToReadScriptDetail(#principal.getId(), #id)")
    public ResponseEntity<BaseResponse<ScriptDetailDto>> getScriptDetail(@AuthenticationPrincipal UserSecurityDTO principal,
                                                                         @PathVariable Long id) {
        return BaseResponse.success(
                SuccessCode.SELECT_SUCCESS,
                scriptService.getScriptDetail(id)
        );
    }

    @PostMapping
    public ResponseEntity<BaseResponse<String>> createScript(@AuthenticationPrincipal UserSecurityDTO principal,
                                                             @RequestBody @Valid ScriptCreatDto scriptCreateDto) {
        UUID id = principal.getId();
        scriptService.createScript(scriptCreateDto, id);

        return BaseResponse.success(
                SuccessCode.INSERT_SUCCESS,
                "스크립트가 생성되었습니다."
        );
    }

    @DeleteMapping
    @PreAuthorize("@authService.authorizeToDeleteScript(#principal.getId(), #scriptDeleteListDto)")
    public ResponseEntity<BaseResponse<String>> deleteScript(@AuthenticationPrincipal UserSecurityDTO principal,
                                                             @RequestBody @Valid ScriptDeleteListDto scriptDeleteListDto) {
        log.info("삭제 시작");
        scriptService.deleteScript(scriptDeleteListDto);
        return BaseResponse.success(
                SuccessCode.DELETE_SUCCESS,
                "스크립트 삭제 성공"
        );
    }
}
