package io.ssafy.mallook.domain.style.application;

import io.ssafy.mallook.domain.member.dao.MemberRepository;
import io.ssafy.mallook.domain.member.entity.Member;
import io.ssafy.mallook.domain.product.dao.mongo.ProductsRepository;
import io.ssafy.mallook.domain.product.dto.response.ProductsListDto;
import io.ssafy.mallook.domain.product.entity.Products;
import io.ssafy.mallook.domain.style.dao.StyleRepository;
import io.ssafy.mallook.domain.style.dto.request.StyleInsertReq;
import io.ssafy.mallook.domain.style.dto.response.StyleDetailRes;
import io.ssafy.mallook.domain.style.dto.response.StyleProductRes;
import io.ssafy.mallook.domain.style.dto.response.StyleRes;
import io.ssafy.mallook.domain.style.dto.response.StyledWorldCupDto;
import io.ssafy.mallook.domain.style.entity.Style;
import io.ssafy.mallook.domain.style_product.dao.StyleProductRepository;
import io.ssafy.mallook.domain.style_product.entity.StyleProduct;
import io.ssafy.mallook.global.batch.dao.Top50RedisDao;
import io.ssafy.mallook.global.common.code.ErrorCode;
import io.ssafy.mallook.global.exception.BaseExceptionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static io.ssafy.mallook.global.common.code.ErrorCode.NOT_FOUND_PRODUCT;
import static java.util.stream.Collectors.*;
import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StyleServiceImpl implements StyleService {

    private final MemberRepository memberRepository;
    private final StyleRepository styleRepository;
    private final StyleProductRepository styleProductRepository;
    private final ProductsRepository productsRepository;
    private final Top50RedisDao top50RedisDao;

    @Override
    public Slice<StyleRes> findStyleListFirst(Pageable pageable) {
        Long maxId = styleRepository.findMaxId();
        // Style 객체의 Slice를 조회
        Slice<Style> styleSlice = styleRepository.findStylesByIdLessThan(pageable, maxId + 1);

        // Style 객체를 StyleRes 객체로 변환
        List<StyleRes> convertedList = styleSlice.getContent().stream()
                .map(this::toResDto) // Style -> StyleRes 변환
                .collect(toList());

        // 변환된 List<StyleRes>를 사용하여 새로운 Slice<StyleRes> 생성
        return new SliceImpl<>(convertedList, pageable, styleSlice.hasNext());
    }

    @Override
    public Slice<StyleRes> findStyleList(Pageable pageable, Long cursor) {
        // Style 객체의 Slice를 조회
        Slice<Style> styleSlice = styleRepository.findStylesByIdLessThan(pageable, cursor + 1);

        // Style 객체를 StyleRes 객체로 변환
        List<StyleRes> convertedList = styleSlice.getContent().stream()
                .map(this::toResDto) // Style -> StyleRes 변환
                .collect(toList());

        // 변환된 List<StyleRes>를 사용하여 새로운 Slice<StyleRes> 생성
        return new SliceImpl<>(convertedList, pageable, styleSlice.hasNext());
    }

    @Override
    public List<StyledWorldCupDto> getWorldCupList() {
        List<Long> topStylePkList = top50RedisDao.getStylesDto().styleIdList();
        List<Style> top50StyleList = styleRepository.findAllById(topStylePkList);
        Collections.shuffle(top50StyleList);

        return top50StyleList.stream()
                .map(this::toDto)
                .limit(8)
                .toList();
    }

    @Override
    public StyleDetailRes findStyleDetail(Long id) {
        var style = styleRepository.findById(id)
                .orElseThrow(() -> new BaseExceptionHandler(ErrorCode.NOT_FOUND_ERROR));
        List<StyleProductRes> productList = style.getStyleProductList().stream()
                .map(ele -> productsRepository.findById(ele.getProduct())
                        .map(product -> new StyleProductRes(
                                product.getName(),
                                product.getPrice(),
                                product.getBrandName(),
                                product.getImage()))
                        .orElseThrow(() -> new BaseExceptionHandler(NOT_FOUND_PRODUCT)))
                .collect(toList());

        return new StyleDetailRes(
                style.getMember().getNickname(),
                style.getName(),
                style.getHeartCount(),
                productList
        );
    }

    @Override
    @Transactional
    public void saveStyle(UUID memberId, StyleInsertReq styleInsertRes) {
        Member proxyMember = memberRepository.getReferenceById(memberId);
        Style style = Style.builder()
                .name(styleInsertRes.name())
                .heartCount(0L)
                .totalLike(0)
                .member(proxyMember)
                .build();
        var st = styleRepository.save(style);
        styleInsertRes.productIdList().forEach(ele ->
                styleProductRepository.save(
                        StyleProduct.builder()
                                .style(st)
                                .product(ele)
                                .build()));
    }

    @Override
    @Transactional
    public void DeleteStyle(UUID memberId, List<Long> styleIdList) {
        styleRepository.deleteMyStyle(memberId, styleIdList);
    }

    public StyledWorldCupDto toDto(Style style) {
        return StyledWorldCupDto.builder()
                .id(style.getId())
                .name(style.getName())
                .heartCount(style.getHeartCount())
                .memberNickname(style.getMember().getNickname())
                .urlList(style.getStyleProductList()
                        .stream()
                        .map(ele -> productsRepository.findById(ele.getProduct())
                                .map(Products::getImage)
                                .orElseThrow(() -> new BaseExceptionHandler(NOT_FOUND_PRODUCT)))
                        .collect(toList()))
                .keywordList(style.getStyleProductList()
                        .stream()
                        .flatMap(ele -> productsRepository.findById(ele.getProduct())
                                .map(product -> product.getKeywords().stream())
                                .orElseThrow(() -> new BaseExceptionHandler(NOT_FOUND_PRODUCT)))
                        .collect(toCollection(LinkedHashSet::new))
                        .stream()
                        .limit(5)
                        .collect(toList()))
                .build();
    }

    public StyleRes toResDto(Style style) {
        List<ProductsListDto> productsListDtoList = new ArrayList<>();

        for (StyleProduct styleProduct : style.getStyleProductList()) {
            // StyleProduct의 product 필드로 MongoDB에서 Products 객체를 조회
            productsRepository.findById(styleProduct.getProduct()).ifPresent(products -> {
                // 조회된 Products 객체를 ProductsListDto로 변환하여 목록에 추가
                productsListDtoList.add(ProductsListDto.toDto(products));
            });
        }

        return StyleRes.builder()
                .id(style.getId())
                .name(style.getName())
                .heartCount(style.getHeartCount())
                .memberNickname(style.getMember().getNickname())
                .productsListDtoList(productsListDtoList)
                .build();
    }
}
