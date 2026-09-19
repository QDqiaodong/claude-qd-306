package com.print.shop.repository;

import com.print.shop.entity.Press;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PressRepository extends JpaRepository<Press, Long> {

    Optional<Press> findByPressCode(String code);

    List<Press> findAllByOrderByIdAsc();
}
