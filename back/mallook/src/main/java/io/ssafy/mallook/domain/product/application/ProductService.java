package io.ssafy.mallook.domain.product.application;

import io.ssafy.mallook.domain.product.dto.request.ProductHotKeywordDto;
import io.ssafy.mallook.domain.product.dto.response.*;
import io.ssafy.mallook.domain.product.entity.MainCategory;
import io.ssafy.mallook.domain.product.entity.Products;
import io.ssafy.mallook.domain.product.entity.ReviewObject;
import io.ssafy.mallook.domain.product.entity.SubCategory;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface ProductService {
    Slice<ProductListDto> getProductList(Long cursor, Pageable pageable, MainCategory mainCategory, SubCategory subCategory);

    ProductsPageRes getProductDetail(String name, String cursor, Pageable pageable);

    ProductsPageRes getProductDetail(ProductHotKeywordDto hotKeywordDto, String cursor, Pageable pageable);

    ProductsPageRes getMongoProductsList(ObjectId cursor, Pageable pageable, String mainCategory, String subCategory);

    ProductsDetailDto getMongoProductsDetail(String id);

    ReviewPageRes getReviewList(String productsId, Pageable pageable);

    ProductPageRes getProductsWithManyReviews(Pageable pageable);

    Long getLastProductId();

    Slice<Products> findByName(Pageable pageable);

    String getLastMongoProductsId();
}
