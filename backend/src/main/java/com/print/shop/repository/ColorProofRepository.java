package com.print.shop.repository;

import com.print.shop.entity.ColorProof;
import com.print.shop.entity.PrintJob;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface ColorProofRepository extends JpaRepository<ColorProof, Long> {

    List<ColorProof> findAllByOrderByProofTimeDescIdDesc();

    List<ColorProof> findByJobIdOrderByProofTimeDescIdDesc(Long jobId);

    Optional<ColorProof> findFirstByJobIdAndResultOrderByProofTimeDescIdDesc(Long jobId, String result);

    /**
     * 落「通过」前给工单行加写锁：同一个待印单上两个人同时落通过，
     * 后一个会在这把锁上等，拿到锁后看到已有的通过记录，当场失败。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select j from PrintJob j where j.id = :jobId")
    Optional<PrintJob> lockJob(@Param("jobId") Long jobId);
}
