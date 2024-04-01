package io.ssafy.mallook.domain.cart.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "장바구니 내 상품 정보 요청시 페이지 정보 포함 응답 DTO")
public record CartPageRes(
        @Schema(description = "장바구니 내 상품 정보 리스트")
        List<CartDetailRes> content,
        @Schema(description = "현재 페이지")
        int currentPage,
        @Schema(description = "전체 페이지")
        int totalPage
) {

}
