package com.example.clientragdemo.configuration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;

class EmbeddingDimensionCheckTest {

    private final EmbeddingModel model = mock(EmbeddingModel.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final EmbeddingDimensionCheck check = new EmbeddingDimensionCheck(model, jdbc);

    private void columnDimension(List<Integer> result) {
        when(jdbc.queryForList(anyString(), eq(Integer.class))).thenReturn(result);
    }

    @Test
    void passesWhenTheModelMatchesTheColumn() {
        columnDimension(List.of(768));
        when(model.embed(anyString())).thenReturn(new float[768]);
        assertThatCode(() -> check.run(null)).doesNotThrowAnyException();
    }

    @Test
    void failsStartupWithBothNumbersWhenTheyDiffer() {
        columnDimension(List.of(768));
        when(model.embed(anyString())).thenReturn(new float[1024]);
        assertThatThrownBy(() -> check.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("1024-dimension")
                .hasMessageContaining("vector(768)");
    }

    @Test
    void skipsWhenTheColumnIsNotThere() {
        columnDimension(List.of());
        assertThatCode(() -> check.run(null)).doesNotThrowAnyException();
    }
}
