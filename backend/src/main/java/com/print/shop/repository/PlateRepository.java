package com.print.shop.repository;

import com.print.shop.entity.Plate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlateRepository extends JpaRepository<Plate, Long> {

    Optional<Plate> findByPlateCode(String plateCode);

    List<Plate> findAllByOrderByIdAsc();

    long countByPressId(Long pressId);
}
