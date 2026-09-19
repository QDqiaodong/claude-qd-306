package com.print.shop.repository;

import com.print.shop.entity.PrintJob;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 工单的筛选条件比较多（客户 / 状态 / 用纸 / 交期区间），
 * 所以这一个仓储额外实现了 Specification，把条件拼装交给调用方。
 */
public interface PrintJobRepository extends JpaRepository<PrintJob, Long>, JpaSpecificationExecutor<PrintJob> {

    Optional<PrintJob> findByJobNo(String jobNo);

    List<PrintJob> findAllByOrderByIdDesc();

    /** 落试印时先锁住这张单，同一张单的「通过」只能一条一条来。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from PrintJob j where j.id = :id")
    Optional<PrintJob> findByIdForUpdate(@Param("id") Long id);
}
