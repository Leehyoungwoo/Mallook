package io.ssafy.mallook.domain.style.dto.response;

import io.ssafy.mallook.domain.style.entity.Style;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "스타일 리스트 정보 요청시 응답 DTO")
public record StyleListRes(
        @Schema(name = "스타일 id")
        Long id,
        @Schema(name="스타일 제목")
        String name

) {
    public static StyleListRes toDto(Style style) {
        return StyleListRes.builder()
                .id(style.getId())
                .name(style.getName())
                .build();
    }
}
