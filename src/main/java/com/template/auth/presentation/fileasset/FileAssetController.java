package com.template.auth.presentation.fileasset;

import com.template.auth.application.fileasset.FileAssetDto;
import com.template.auth.application.fileasset.command.DeleteFileCommand;
import com.template.auth.application.fileasset.command.DeleteFileHandler;
import com.template.auth.application.fileasset.command.UploadFileCommand;
import com.template.auth.application.fileasset.command.UploadFileHandler;
import com.template.auth.application.fileasset.query.DownloadFileHandler;
import com.template.auth.application.fileasset.query.DownloadFileHandler.DownloadedFile;
import com.template.auth.application.fileasset.query.DownloadFileQuery;
import com.template.auth.application.fileasset.query.ListFileAssetsHandler;
import com.template.auth.application.fileasset.query.ListFileAssetsQuery;
import com.template.auth.application.shared.PaginatedResult;
import com.template.auth.application.shared.PaginationParams;
import com.template.auth.application.shared.security.AuthenticatedUser;
import com.template.auth.presentation.shared.ApiPaths;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** All routes are scoped to the caller's own files. Max upload size: spring.servlet.multipart.max-file-size. */
@RestController
@RequestMapping(ApiPaths.V1 + "/files")
@Tag(name = "Files")
public class FileAssetController {

    private final UploadFileHandler uploadFile;
    private final ListFileAssetsHandler listFileAssets;
    private final DownloadFileHandler downloadFile;
    private final DeleteFileHandler deleteFile;

    public FileAssetController(UploadFileHandler uploadFile, ListFileAssetsHandler listFileAssets,
            DownloadFileHandler downloadFile, DeleteFileHandler deleteFile) {
        this.uploadFile = uploadFile;
        this.listFileAssets = listFileAssets;
        this.downloadFile = downloadFile;
        this.deleteFile = deleteFile;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileAssetDto upload(@RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser user) throws IOException {
        String fileName = Objects.requireNonNullElse(file.getOriginalFilename(), "file");
        String mimeType = Objects.requireNonNullElse(file.getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
        return uploadFile.execute(new UploadFileCommand(fileName, mimeType, file.getSize(), file.getBytes(),
                user.id()));
    }

    @GetMapping
    public PaginatedResult<FileAssetDto> list(@RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return listFileAssets.execute(new ListFileAssetsQuery(user.id(), PaginationParams.of(page, limit)));
    }

    /** Streamed straight from storage - the file is never fully buffered in memory. */
    @GetMapping("/{fileAssetId}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID fileAssetId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        DownloadedFile file = downloadFile.execute(new DownloadFileQuery(fileAssetId, user.id()));

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mimeType()))
                .contentLength(file.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new InputStreamResource(file.stream()));
    }

    @DeleteMapping("/{fileAssetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID fileAssetId, @AuthenticationPrincipal AuthenticatedUser user) {
        deleteFile.execute(new DeleteFileCommand(fileAssetId, user.id()));
    }
}
