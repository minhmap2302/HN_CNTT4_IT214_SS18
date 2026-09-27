package vn.rikkei.exam.trainingroom.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.rikkei.exam.trainingroom.model.AppUser;
public interface AppUserRepository extends JpaRepository<AppUser, String> { }
