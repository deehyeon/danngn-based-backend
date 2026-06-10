package backend.daangnbasedbackend.global.adapter.storage;

import backend.daangnbasedbackend.global.application.dto.PresignedUrlRes;
import backend.daangnbasedbackend.global.application.provided.StoragePort;
import backend.daangnbasedbackend.global.application.security.AuthDetails;
import backend.daangnbasedbackend.global.webapi.response.ApiResponse;
import backend.daangnbasedbackend.product.application.dto.PresignedUrlReq;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class StorageController implements StorageApi {
    private final StoragePort storagePort;

    @Override
    public ApiResponse<PresignedUrlRes> getPresignedUrl(AuthDetails authDetails, PresignedUrlReq req) {
        return ApiResponse.success(
                storagePort.generatePresignedUrl("products", authDetails.getMemberId(), req.filename(), req.contentType())
        );
    }
}
