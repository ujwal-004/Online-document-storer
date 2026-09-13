package com.docstorer.service;

import com.docstorer.dto.request.FolderRequest;
import com.docstorer.dto.response.FolderResponse;
import java.util.List;

public interface FolderService {
    FolderResponse createFolder(FolderRequest request, String userEmail);
    FolderResponse updateFolder(Long id, FolderRequest request, String userEmail);
    void deleteFolder(Long id, String userEmail);
    List<FolderResponse> getUserFolders(String userEmail);
    FolderResponse getFolderById(Long id, String userEmail);
}
