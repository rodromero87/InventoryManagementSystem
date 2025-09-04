package com.inventario.apicentral.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyStoreAdapterTest {

    @Mock
    SpringDataIdempotencyRepository repo;

    @InjectMocks
    IdempotencyStoreAdapter adapter;

    @Test
    void exists_deve_delegar_para_repo() {
        when(repo.existsById("K1")).thenReturn(true);
        when(repo.existsById("K2")).thenReturn(false);

        assertThat(adapter.exists("K1")).isTrue();
        assertThat(adapter.exists("K2")).isFalse();

        verify(repo).existsById("K1");
        verify(repo).existsById("K2");
        verifyNoMoreInteractions(repo);
    }

    @Test
    void save_deve_persistir_com_createdAt_preenchido() {
        ArgumentCaptor<JpaIdempotencyEntity> captor = ArgumentCaptor.forClass(JpaIdempotencyEntity.class);

        adapter.save("KEY-123");

        verify(repo, times(1)).saveAndFlush(captor.capture());
        var entity = captor.getValue();
        assertThat(entity.getId()).isEqualTo("KEY-123");
        assertThat(entity.getCreatedAt()).isNotNull();
        // sanity: createdAt ~ agora (janela de 5s)
        assertThat(entity.getCreatedAt()).isBetween(Instant.now().minusSeconds(5), Instant.now().plusSeconds(5));
    }

    @Test
    void save_deve_ignorar_duplicata() {
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(repo).saveAndFlush(any(JpaIdempotencyEntity.class));

        assertDoesNotThrow(() -> adapter.save("DUP-1"));
        verify(repo, times(1)).saveAndFlush(any(JpaIdempotencyEntity.class));
    }
}