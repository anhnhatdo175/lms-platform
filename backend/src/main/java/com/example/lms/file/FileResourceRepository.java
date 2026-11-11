package com.example.lms.file;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FileResourceRepository extends JpaRepository<FileResource, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<FileResource> findByIdAndUploadedBy_Id(Long id, Long uploadedById);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select resource from FileResource resource where resource.id = :id")
    Optional<FileResource> findByIdForUpdate(@Param("id") Long id);
}
