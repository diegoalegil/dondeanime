package com.dondeanime.backend.curated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class CuratorProfileServiceTest {

    private final CuratorProfileRepository repository = mock(CuratorProfileRepository.class);
    private final CuratorProfileService service = new CuratorProfileService(repository);

    @Test
    void approveCuratorNormalizesEmailAndSavesProfile() {
        when(repository.findByEmailIgnoreCase("diego@example.com")).thenReturn(Optional.empty());
        when(repository.save(any(CuratorProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CuratorProfileDto dto = service.approveCurator(
                new CuratorProfileSaveRequest("Diego@Example.com", "Diego"));

        assertThat(dto.email()).isEqualTo("diego@example.com");
        assertThat(dto.displayName()).isEqualTo("Diego");
        assertThat(dto.approved()).isTrue();
        verify(repository).save(any(CuratorProfile.class));
    }

    @Test
    void revokeCuratorDisablesExistingProfile() {
        CuratorProfile profile = new CuratorProfile();
        profile.approve("diego@example.com", "Diego");
        when(repository.findByEmailIgnoreCase("diego@example.com")).thenReturn(Optional.of(profile));
        when(repository.save(profile)).thenReturn(profile);

        Optional<CuratorProfileDto> dto = service.revokeCurator("Diego@Example.com");

        assertThat(dto).isPresent();
        assertThat(dto.get().approved()).isFalse();
        assertThat(dto.get().revokedAt()).isNotNull();
    }

    @Test
    void listCuratorsMapsDtos() {
        CuratorProfile profile = new CuratorProfile();
        profile.approve("diego@example.com", "Diego");
        when(repository.findAll()).thenReturn(List.of(profile));

        assertThat(service.listCurators())
                .singleElement()
                .extracting(CuratorProfileDto::email)
                .isEqualTo("diego@example.com");
    }
}
