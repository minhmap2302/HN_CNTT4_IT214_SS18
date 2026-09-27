package vn.rikkei.exam.trainingroom.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.rikkei.exam.trainingroom.model.ResourceType;
public interface ResourceTypeRepository extends JpaRepository<ResourceType, String> { }
