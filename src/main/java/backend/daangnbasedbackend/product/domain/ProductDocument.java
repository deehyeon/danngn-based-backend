package backend.daangnbasedbackend.product.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.Instant;
import java.time.ZoneId;

@Document(indexName = "products")
@Setting(settingPath = "elasticsearch/product-setting.json")
@Mapping(mappingPath = "elasticsearch/product-mapping.json")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductDocument {
    @Id
    private Long id;

    @Field(type = FieldType.Long)
    private Long productId;

    @Field(type = FieldType.Text, analyzer = "nori_analyzer")
    private String title;

    @Field(type = FieldType.Text, analyzer = "nori_analyzer")
    private String description;

    @Field(type = FieldType.Keyword)
    private String location;

    @Field(type = FieldType.Keyword)
    private String state;

    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant refreshedAt;

    public static ProductDocument from(Product product) {
        ProductDocument doc = new ProductDocument();
        doc.id = product.getId();
        doc.productId = product.getId();
        doc.title = product.getTitle();
        doc.description = product.getDescription();
        doc.location = product.getLocation();
        doc.state = product.getState().name();
        doc.refreshedAt = product.getRefreshAt().atZone(ZoneId.systemDefault()).toInstant();
        return doc;
    }
}
