package com.print.shop.repository;

import com.print.shop.entity.PrintJob;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 工单的筛选条件比较多（客户 / 状态 / 用纸 / 交期区间），
 * 所以这一个仓储额外实现了 Specification，把条件拼装交给调用方。
 */
public interface PrintJobRepository extends JpaRepository<PrintJob, Long>, JpaSpecificationExecutor<PrintJob> {

    Optional<PrintJob> findByJobNo(String jobNo);

    List<PrintJob> findAllByOrderByIdDesc();
}
