package com.template.auth.application.fileasset.query;

import com.template.auth.application.fileasset.FileAssetDto;
import com.template.auth.application.fileasset.FileAssetRepository;
import com.template.auth.application.shared.PaginatedResult;
import org.springframework.stereotype.Service;

@Service
public class ListFileAssetsHandler {

    private final FileAssetRepository fileAssetRepository;

    public ListFileAssetsHandler(FileAssetRepository fileAssetRepository) {
        this.fileAssetRepository = fileAssetRepository;
    }

    public PaginatedResult<FileAssetDto> execute(ListFileAssetsQuery query) {
        return fileAssetRepository.findAllForUser(query.userId(), query.pagination()).map(FileAssetDto::from);
    }
}
