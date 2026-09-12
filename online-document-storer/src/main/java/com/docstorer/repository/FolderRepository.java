package com.docstorer.repository;

import com.docstorer.entity.Folder;
import com.docstorer.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
    List<Folder> findByOwnerAndParentIsNull(User owner);
    List<Folder> findByOwnerAndParent(User owner, Folder parent);
    boolean existsByNameAndOwnerAndParent(String name, User owner, Folder parent);
}
