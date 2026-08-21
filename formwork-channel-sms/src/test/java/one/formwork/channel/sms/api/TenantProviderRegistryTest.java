package one.formwork.channel.sms.api;

import one.formwork.channel.sms.tenant.TenantProviderAssignment;
import one.formwork.channel.sms.tenant.TenantProviderAssignmentRepository;
import one.formwork.channel.sms.tenant.TenantRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantProviderRegistryTest {
    private final TenantRepository tenants = mock(TenantRepository.class);
    private final TenantProviderAssignmentRepository assignments = mock(TenantProviderAssignmentRepository.class);
    private final TenantProviderRegistry registry = new TenantProviderRegistry(tenants, assignments);
    private final UUID tenantId = UUID.randomUUID();

    @Test
    void resolvesPersistedProviderAndFallsBackWhenMissing() {
        when(assignments.findById(tenantId)).thenReturn(Optional.empty());
        assertEquals("TWILIO", registry.resolveProvider(tenantId, "TWILIO"));
        verify(assignments).findById(tenantId);
    }

    @Test
    void rejectsUnsupportedProvider() {
        assertThrows(IllegalArgumentException.class, () -> registry.assignProvider(tenantId, "UNKNOWN"));
        verifyNoInteractions(tenants, assignments);
    }

    @Test
    void clearingAssignmentDeletesPersistedMapping() {
        registry.assignProvider(tenantId, null);
        verify(assignments).deleteById(tenantId);
        verifyNoInteractions(tenants);
    }

    @Test
    void resolvesAssignmentFromRepository() {
        TenantProviderAssignment assignment = mock(TenantProviderAssignment.class);
        when(assignments.findById(tenantId)).thenReturn(Optional.of(assignment));
        when(assignment.getProvider()).thenReturn("VONAGE");
        assertEquals("VONAGE", registry.resolveProvider(tenantId, "TWILIO"));
    }
}
