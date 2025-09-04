package com.inventario.loja.infrastructure.persistence;

import com.inventario.loja.domain.port.out.AppliedEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AppliedEventRepositoryAdapterTest {

    @Test
    @DisplayName("seen: delega para repo.existsById(eventId)")
    void seen_delegatesToExistsById() {
        // given
        SpringDataAppliedEventRepository repo = mock(SpringDataAppliedEventRepository.class);
        AppliedEventRepository adapter = new AppliedEventRepositoryAdapter(repo);

        given(repo.existsById("e-1")).willReturn(true);
        given(repo.existsById("e-2")).willReturn(false);

        // when/then
        assertThat(adapter.seen("e-1")).isTrue();
        assertThat(adapter.seen("e-2")).isFalse();

        then(repo).should().existsById("e-1");
        then(repo).should().existsById("e-2");
        then(repo).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("mark: salva entidade com eventId correto e timestamp ≈ agora")
    void mark_savesEntityWithIdAndCurrentTimestamp() {
        // given
        SpringDataAppliedEventRepository repo = mock(SpringDataAppliedEventRepository.class);
        AppliedEventRepositoryAdapter adapter = new AppliedEventRepositoryAdapter(repo);

        String eventId = "evt-123";
        Instant before = Instant.now();

        // when
        adapter.mark(eventId);

        Instant after = Instant.now();

        // then
        ArgumentCaptor<JpaAppliedEventEntity> cap = ArgumentCaptor.forClass(JpaAppliedEventEntity.class);
        then(repo).should().saveAndFlush(cap.capture());

        JpaAppliedEventEntity saved = cap.getValue();
        assertThat(saved).isNotNull();

        // Verifica o id via getter comum (getId ou getEventId); se não existir, tenta por campo
        String savedId = tryInvokeStringGetter(saved, "getEventId", "getId");
        if (savedId == null) {
            savedId = tryReadStringField(saved, "eventId", "id");
        }
        assertThat(savedId).isEqualTo(eventId);

        // Verifica o Instant via getter comum (getCreatedAt/appliedAt/timestamp) ou por campo Instant
        Instant ts = tryInvokeInstantGetter(saved, "getCreatedAt", "getAppliedAt", "getTimestamp", "getTs");
        if (ts == null) {
            ts = tryReadInstantField(saved, "createdAt", "appliedAt", "timestamp", "ts");
            if (ts == null) {
                for (Field f : saved.getClass().getDeclaredFields()) {
                    if (f.getType().equals(Instant.class)) {
                        f.setAccessible(true);
                        try { ts = (Instant) f.get(saved); } catch (Exception ignored) {}
                        break;
                    }
                }
            }
        }
        assertThat(ts).isNotNull();
        // Janela de tolerância para "agora"
        assertThat(ts).isAfterOrEqualTo(before.minusSeconds(1));
        assertThat(ts).isBeforeOrEqualTo(after.plusSeconds(1));
    }


    private static String tryInvokeStringGetter(Object target, String... methodNames) {
        for (String m : methodNames) {
            try {
                Method method = target.getClass().getMethod(m);
                if (method.getReturnType().equals(String.class)) {
                    Object val = method.invoke(target);
                    return (String) val;
                }
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private static String tryReadStringField(Object target, String... fieldNames) {
        for (String name : fieldNames) {
            try {
                Field f = target.getClass().getDeclaredField(name);
                if (f.getType().equals(String.class)) {
                    f.setAccessible(true);
                    return (String) f.get(target);
                }
            } catch (NoSuchFieldException ignored) {
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private static Instant tryInvokeInstantGetter(Object target, String... methodNames) {
        for (String m : methodNames) {
            try {
                Method method = target.getClass().getMethod(m);
                if (method.getReturnType().equals(Instant.class)) {
                    return (Instant) method.invoke(target);
                }
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private static Instant tryReadInstantField(Object target, String... fieldNames) {
        for (String name : fieldNames) {
            try {
                Field f = target.getClass().getDeclaredField(name);
                if (f.getType().equals(Instant.class)) {
                    f.setAccessible(true);
                    return (Instant) f.get(target);
                }
            } catch (NoSuchFieldException ignored) {
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
