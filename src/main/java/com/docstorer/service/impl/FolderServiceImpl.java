package com.docstorer.service.impl;

import com.docstorer.dto.request.FolderRequest;
import com.docstorer.dto.response.FolderResponse;
import com.docstorer.entity.*;
import com.docstorer.exception.ResourceNotFoundException;
import com.docstorer.exception.UnauthorizedException;
import com.docstorer.repository.*;
import com.docstorer.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {

    private final FolderRepository folderRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public FolderResponse createFolder(FolderRequest req, String userEmail) {
        User user = getUser(userEmail);
        Folder parent = null;
        if (req.getParentId() != null) {
            parent = folderRepository.findById(req.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Folder","id",req.getParentId()));
            if (!parent.getOwner().getId().equals(user.getId()))
                throw new UnauthorizedException("Access denied to parent folder");
        }
        Folder folder = Folder.builder()
                .name(req.getName()).description(req.getDescription())
                .color(req.getColor() != null ? req.getColor() : "#6366f1")
                .owner(user).parent(parent).build();
        return toResponse(folderRepository.save(folder));
    }

    @Override
    @Transactional
    public FolderResponse updateFolder(Long id, FolderRequest req, String userEmail) {
        User user = getUser(userEmail);
        Folder folder = getFolder(id);
        if (!folder.getOwner().getId().equals(user.getId()))
            throw new UnauthorizedException("Access denied");
        if (req.getName() != null) folder.setName(req.getName());
        if (req.getDescription() != null) folder.setDescription(req.getDescription());
        if (req.getColor() != null) folder.setColor(req.getColor());
        return toResponse(folderRepository.save(folder));
    }

    @Override
    @Transactional
    public void deleteFolder(Long id, String userEmail) {
        User user = getUser(userEmail);
        Folder folder = getFolder(id);
        if (!folder.getOwner().getId().equals(user.getId()))
            throw new UnauthorizedException("Access denied");
        folderRepository.delete(folder);
    }

    @Override
    public List<FolderResponse> getUserFolders(String userEmail) {
        User user = getUser(userEmail);
        return folderRepository.findByOwnerAndParentIsNull(user).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public FolderResponse getFolderById(Long id, String userEmail) {
        User user = getUser(userEmail);
        Folder folder = getFolder(id);
        if (!folder.getOwner().getId().equals(user.getId()))
            throw new UnauthorizedException("Access denied");
        return toResponse(folder);
    }

    private FolderResponse toResponse(Folder f) {
        return FolderResponse.builder()
                .id(f.getId()).name(f.getName()).description(f.getDescription())
                .color(f.getColor())
                .parentId(f.getParent() != null ? f.getParent().getId() : null)
                .parentName(f.getParent() != null ? f.getParent().getName() : null)
                .documentCount(f.getDocuments().size())
                .createdAt(f.getCreatedAt()).build();
    }

    private User getUser(String email) {
        return userRepository.findByEmailOrUsername(email)
                .or(() -> userRepository.findByEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("User","email",email));
    }

    private Folder getFolder(Long id) {
        return folderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Folder","id",id));
    }
}
