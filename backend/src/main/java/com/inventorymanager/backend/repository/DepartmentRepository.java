import com.inventorymanager.backend.domain.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long>, JpaSpecificationExecutor<Department> {
    @Override
    @EntityGraph(attributePaths = {"branch"})
    Page<Department> findAll(Specification<Department> spec, Pageable pageable);

    java.util.Optional<Department> findByNameAndBranch_Id(String name, Long branchId);
}
