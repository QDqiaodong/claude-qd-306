package com.print.shop.repository;

import com.print.shop.entity.Paper;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperRepository extends JpaRepository<Paper, Long> {

    Optional<Paper> findByPaperCode(String code);

    List<Paper> findAllByOrderByIdAsc();
}
