package io.ssafy.mallook.domain.cart_product.entity;

import io.ssafy.mallook.domain.BaseEntity;
import io.ssafy.mallook.domain.cart.entity.Cart;
import io.ssafy.mallook.domain.shoppingmall.entity.ShoppingMall;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@SQLRestriction("status=true")
@Table(name = "cart_product")
public class CartProduct extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @NotNull
    @Column(name = "product_id")
    private String product;

    @NotNull
    @Column(name = "product_count")
    private Integer productCount;

    @NotNull
    @Column(name = "product_price")
    private Integer productPrice;

    @NotBlank
    @Column(name = "product_name")
    private String productName;

    @Column(name = "product_image")
    private String productImage;

    @NotBlank
    @Column(name = "product_size")
    private String productSize;

    @NotBlank
    @Column(name = "product_color")
    private String productColor;

    @NotNull
    @Column(name = "product_fee")
    private Integer productFee;

    //쇼핑몰 id
    @ManyToOne
    @JoinColumn(name = "shoppingmall_id")
    private ShoppingMall shopMallId;
}
