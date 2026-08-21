package one.formwork.channel.sms.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TenantProviderAssignmentRepository extends JpaRepository<TenantProviderAssignment, UUID> {}
