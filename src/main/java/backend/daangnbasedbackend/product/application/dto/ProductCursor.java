package backend.daangnbasedbackend.product.application.dto;

import java.time.LocalDateTime;
import java.util.Base64;

public record ProductCursor(LocalDateTime refreshedAt, Long id) {

    public static ProductCursor decode(String cursor) {
        try {
            String decoded = new String(Base64.getDecoder().decode(cursor));
            int sep = decoded.lastIndexOf('_');
            return new ProductCursor(
                LocalDateTime.parse(decoded.substring(0, sep)),
                Long.parseLong(decoded.substring(sep + 1))
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("올바르지 않은 커서입니다.");
        }
    }

    public String encode() {
        return Base64.getEncoder().encodeToString(
            (refreshedAt.toString() + "_" + id).getBytes()
        );
    }
}
