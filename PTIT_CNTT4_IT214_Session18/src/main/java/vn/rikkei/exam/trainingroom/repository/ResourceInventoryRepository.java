package vn.rikkei.exam.trainingroom.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.rikkei.exam.trainingroom.model.ResourceInventory;

import java.time.LocalDate;
import java.util.Optional;

public interface ResourceInventoryRepository extends JpaRepository<ResourceInventory, Long> {
    Optional<ResourceInventory> findByResourceType_ResourceCodeAndAvailableDate(String resourceCode, LocalDate availableDate);
}
