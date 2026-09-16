package com.unitbv.myquiz.app.util;

import com.unitbv.myquiz.api.util.PaginationParams;
import com.unitbv.myquiz.app.entities.Question;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpringDataPaginationAdapterTest {

    @Test
    void questionOrderingUsesZeroLastThenCrtNoAndId() {
        Pageable pageable = SpringDataPaginationAdapter.toPageable(
                new PaginationParams(1, 2),
                "crtNo",
                "asc",
                "id"
        );

        List<Sort.Order> orders = pageable.getSort().toList();

        assertEquals(3, orders.size());
        assertEquals("CASE WHEN crtNo = 0 THEN 1 ELSE 0 END", orders.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(0).getDirection());
        assertEquals("crtNo", orders.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(1).getDirection());
        assertEquals("id", orders.get(2).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(2).getDirection());
    }

    @Test
    void questionOrderingIsIdenticalForEveryPage() {
        Pageable firstPage = SpringDataPaginationAdapter.toPageable(
                new PaginationParams(1, 2),
                "crtNo",
                "asc",
                "id"
        );
        Pageable secondPage = SpringDataPaginationAdapter.toPageable(
                new PaginationParams(2, 2),
                "crtNo",
                "asc",
                "id"
        );

        assertEquals(0, firstPage.getOffset());
        assertEquals(2, secondPage.getOffset());
        assertEquals(firstPage.getSort(), secondPage.getSort());
    }

    @Test
    void genericOrderingAppendsAscendingIdTieBreaker() {
        Pageable pageable = SpringDataPaginationAdapter.toPageable(
                new PaginationParams(1, 10),
                "name",
                "desc",
                "id"
        );

        List<Sort.Order> orders = pageable.getSort().toList();

        assertEquals(2, orders.size());
        assertEquals("name", orders.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, orders.get(0).getDirection());
        assertEquals("id", orders.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(1).getDirection());
    }

    @Test
    void inMemoryQuestionOrderingKeepsZeroRowsLastAndUsesIdTieBreaker() {
        Question first = question(10, 20L);
        Question second = question(10, 30L);
        Question unnumbered = question(0, 40L);

        List<Question> ordered = List.of(unnumbered, second, first).stream()
                .sorted(QuestionOrdering.byCrtNoThenId())
                .toList();

        assertEquals(List.of(20L, 30L, 40L), ordered.stream().map(Question::getId).toList());
    }

    private Question question(int crtNo, Long id) {
        Question question = new Question();
        question.setCrtNo(crtNo);
        question.setId(id);
        return question;
    }
}
