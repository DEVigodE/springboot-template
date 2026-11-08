package com.template.auth.application.fileasset.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.template.auth.application.fileasset.FileAssetDto;
import com.template.auth.application.fileasset.FileAssetRepository;
import com.template.auth.application.fileasset.FileStoragePort;
import com.template.auth.application.shared.FileTypeDetectorPort;
import com.template.auth.application.shared.NotificationQueuePort;
import com.template.auth.domain.fileasset.FileAssetEntity;
import com.template.auth.domain.shared.exception.InvalidFileTypeException;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UploadFileHandlerTest {

    @Mock
    private FileStoragePort fileStorage;
    @Mock
    private FileAssetRepository fileAssetRepository;
    @Mock
    private NotificationQueuePort notificationQueue;
    @Mock
    private FileTypeDetectorPort fileTypeDetector;

    @InjectMocks
    private UploadFileHandler handler;

    @Test
    void rejectsContentWhoseRealTypeIsNotAllowed() throws IOException {
        byte[] content = "#!/bin/sh".getBytes();
        when(fileTypeDetector.detect(content)).thenReturn(Optional.of("text/x-shellscript"));

        assertThatThrownBy(() -> handler.execute(new UploadFileCommand("evil.png", "image/png", content.length,
                content, UUID.randomUUID())))
                .isInstanceOf(InvalidFileTypeException.class)
                .hasMessageContaining("text/x-shellscript");

        verify(fileStorage, never()).save(anyString(), any());
    }

    @Test
    void rejectsUndetectableContentEvenWithAnAllowedContentTypeHeader() throws IOException {
        byte[] content = {1, 2, 3, 4};
        when(fileTypeDetector.detect(content)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.execute(new UploadFileCommand("x.pdf", "application/pdf", 4, content,
                UUID.randomUUID())))
                .isInstanceOf(InvalidFileTypeException.class)
                .hasMessage("File type could not be determined");

        verify(fileStorage, never()).save(anyString(), any());
    }

    @Test
    void storesTheDetectedTypeNotTheClientHeaderAndSanitizesTheStorageKey() throws IOException {
        byte[] content = {(byte) 0x89, 'P', 'N', 'G'};
        UUID uploader = UUID.randomUUID();
        when(fileTypeDetector.detect(content)).thenReturn(Optional.of("image/png"));
        when(fileAssetRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FileAssetDto dto = handler.execute(new UploadFileCommand("../../etc/cron.d/evil.png", "text/plain",
                content.length, content, uploader));

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(fileStorage).save(key.capture(), any());
        assertThat(key.getValue()).doesNotContain("..").doesNotContain("/").endsWith("-evil.png");

        ArgumentCaptor<FileAssetEntity> saved = ArgumentCaptor.forClass(FileAssetEntity.class);
        verify(fileAssetRepository).create(saved.capture());
        assertThat(saved.getValue().getMimeType()).isEqualTo("image/png");
        assertThat(dto.mimeType()).isEqualTo("image/png");
        verify(notificationQueue).enqueue(any());
    }

    @Test
    void safeBaseNameStripsDirectoriesAndOddCharacters() {
        assertThat(UploadFileHandler.safeBaseName("..\\..\\windows\\evil.pdf")).isEqualTo("evil.pdf");
        assertThat(UploadFileHandler.safeBaseName("relatório final (v2).pdf")).isEqualTo("relat_rio_final__v2_.pdf");
        assertThat(UploadFileHandler.safeBaseName("")).isEqualTo("file");
    }
}
