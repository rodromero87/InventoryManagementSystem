package com.inventario.loja.infrastructure.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class LocalInventoryRepositoryAdapterTest {

    @Test
    @DisplayName("findForUpdate delega para jpa.findBySkuForUpdate e propaga Optional presente")
    void findForUpdate_present() {
        // given
        SpringDataLocalInventoryRepository jpa = mock(SpringDataLocalInventoryRepository.class);
        LocalInventoryRepositoryAdapter adapter = new LocalInventoryRepositoryAdapter(jpa);

        var entity = new JpaLocalInventoryEntity();
        entity.setSku("ABC-123");

        given(jpa.findBySkuForUpdate("ABC-123")).willReturn(Optional.of(entity));

        // when
        Optional<JpaLocalInventoryEntity> result = adapter.findForUpdate("ABC-123");

        // then
        assertThat(result).containsSame(entity);
        then(jpa).should().findBySkuForUpdate("ABC-123");
        then(jpa).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("findForUpdate delega para jpa.findBySkuForUpdate e propaga Optional.empty")
    void findForUpdate_empty() {
        SpringDataLocalInventoryRepository jpa = mock(SpringDataLocalInventoryRepository.class);
        LocalInventoryRepositoryAdapter adapter = new LocalInventoryRepositoryAdapter(jpa);

        given(jpa.findBySkuForUpdate("NONE")).willReturn(Optional.empty());

        Optional<JpaLocalInventoryEntity> result = adapter.findForUpdate("NONE");

        assertThat(result).isEmpty();
        then(jpa).should().findBySkuForUpdate("NONE");
        then(jpa).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("save delega para jpa.saveAndFlush e retorna o salvo")
    void save_delegatesToSaveAndFlush() {
        // given
        SpringDataLocalInventoryRepository jpa = mock(SpringDataLocalInventoryRepository.class);
        LocalInventoryRepositoryAdapter adapter = new LocalInventoryRepositoryAdapter(jpa);

        var toSave = new JpaLocalInventoryEntity();
        toSave.setSku("SKU-1");

        var saved = new JpaLocalInventoryEntity();
        saved.setSku("SKU-1");
        saved.setOnHand(10);

        given(jpa.saveAndFlush(toSave)).willReturn(saved);

        // when
        JpaLocalInventoryEntity result = adapter.save(toSave);

        // then
        assertThat(result).isSameAs(saved);

        ArgumentCaptor<JpaLocalInventoryEntity> cap = ArgumentCaptor.forClass(JpaLocalInventoryEntity.class);
        then(jpa).should().saveAndFlush(cap.capture());
        assertThat(cap.getValue()).isSameAs(toSave);

        then(jpa).shouldHaveNoMoreInteractions();
    }
}
