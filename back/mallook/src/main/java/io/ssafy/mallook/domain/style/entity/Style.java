package io.ssafy.mallook.domain.style.entity;

import io.ssafy.mallook.domain.BaseEntity;
import io.ssafy.mallook.domain.member.entity.Member;
import io.ssafy.mallook.domain.style_product.entity.StyleProduct;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "style")
@SQLRestriction("status=TRUE")
public class Style extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @NotBlank
    private String name;

    @NotNull
    private Long heartCount;

    @NotNull
    @Column(name = "total_like")
    private Integer totalLike;

    @OneToMany(mappedBy = "style", cascade = CascadeType.ALL)
    @Builder.Default
    private List<StyleProduct> styleProductList = new ArrayList<>();

    public void like() {
        this.heartCount++;
        this.totalLike++;
    }

    public void unlike() {
        this.heartCount--;
        this.totalLike--;
    }
}
