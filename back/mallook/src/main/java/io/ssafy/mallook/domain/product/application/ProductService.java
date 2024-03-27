package io.ssafy.mallook.domain.product.application;

import io.ssafy.mallook.domain.product.dto.response.ProductListDto;
import io.ssafy.mallook.domain.product.dto.response.ProductsListDto;
import io.ssafy.mallook.domain.product.entity.MainCategory;
import io.ssafy.mallook.domain.product.entity.Products;
import io.ssafy.mallook.domain.product.entity.SubCategory;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;
public interface ProductService {
    Slice<ProductListDto> getProductList(Long cursor, Pageable pageable, MainCategory mainCategory, SubCategory subCategory);

    Long getLastProductId();

    Slice<Products> findByName(Pageable pageable);
    String getLastMongoProductsId();
    Slice<ProductsListDto> getMongoProductsList(ObjectId cursor, Pageable pageable, String mainCategory, String subCategory);
}
