package com.print.shop.repository;

import com.print.shop.entity.TestPrint;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestPrintRepository extends JpaRepository<TestPrint, Long> {

    List<TestPrint> findAllByOrderByIdDesc();

    List<TestPrint> findByJobIdOrderByIdDesc(Long jobId);

    List<TestPrint> findByResult(String result);

    /** 一张单最新的一条「通过」——眼下只认这一条。 */
    Optional<TestPrint> findFirstByJobIdAndResultOrderByIdDesc(Long jobId, String result);
}
