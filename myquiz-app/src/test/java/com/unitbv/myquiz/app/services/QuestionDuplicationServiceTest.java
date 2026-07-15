package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.types.DuplicateComparisonStrategy;
import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.api.types.StudyYear;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuestionDuplicate;
import com.unitbv.myquiz.app.entities.QuestionError;
import com.unitbv.myquiz.app.repositories.QuestionDuplicateRepository;
import com.unitbv.myquiz.app.repositories.QuestionErrorRepository;
import com.unitbv.myquiz.app.repositories.QuestionRepository;
import com.unitbv.myquiz.app.testutil.ServiceTestData;
import com.unitbv.myquiz.app.testutil.TestEntityFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionDuplicationServiceTest {

    @Autowired
    private QuestionDuplicationService service;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionDuplicateRepository questionDuplicateRepository;

    @Autowired
    private QuestionErrorRepository questionErrorRepository;

    @Autowired
    private TestEntityFactory testEntityFactory;

    @Test
    void checkDuplicatesInCourse_trueFalseDuplicate_persistsLinkAndErrors() {
        String marker = "it-tf-" + UUID.randomUUID();
        Question existing = createQuestionForCourse(ServiceTestData.COURSE, QuestionType.TRUEFALSE, marker + "-title", marker + "-text", "TRUE");
        Question uploaded = createQuestionForCourse(ServiceTestData.COURSE, QuestionType.TRUEFALSE, marker + "-TITLE", marker + "-TEXT", "FALSE");

        List<QuestionError> errors = service.checkDuplicatesInCourse(List.of(existing), ServiceTestData.COURSE, List.of(uploaded));

        assertEquals(2, errors.size());
        assertTrue(errors.stream().anyMatch(e -> e.getDescription().startsWith(MyUtil.REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS)));
        assertTrue(errors.stream().anyMatch(e -> e.getDescription().startsWith(MyUtil.REFORMULATE_QUESTION_ANSWER_ALREADY_EXISTS)));

        long lower = Math.min(existing.getId(), uploaded.getId());
        long higher = Math.max(existing.getId(), uploaded.getId());
        assertTrue(questionDuplicateRepository.existsByQuestionIdAndDuplicateQuestionId(lower, higher));
    }

    @Test
    void checkDuplicatesInCourse_trueFalseMissingAnswer_createsMissingAnswerErrorOnly() {
        String marker = "it-missing-" + UUID.randomUUID();
        Question existing = createQuestionForCourse(ServiceTestData.COURSE, QuestionType.TRUEFALSE, marker + "-existing", marker + "-text", "TRUE");
        Question uploaded = createQuestionForCourse(ServiceTestData.COURSE, QuestionType.TRUEFALSE, marker + "-uploaded", marker + "-text2", null);

        List<QuestionError> errors = service.checkDuplicatesInCourse(List.of(existing), ServiceTestData.COURSE, List.of(uploaded));

        assertEquals(1, errors.size());
        assertTrue(errors.getFirst().getDescription().startsWith(MyUtil.MISSING_ANSWER));
        assertTrue(questionDuplicateRepository.findByQuestionIdOrDuplicateQuestionId(uploaded.getId(), uploaded.getId()).isEmpty());
    }

    @Test
    void removeDuplicateAssociations_lastLinksRemoved_cleansDuplicateErrorsForAllAffectedQuestions() {
        String marker = "it-remove-" + UUID.randomUUID();
        Question q1 = createQuestionForCourse(ServiceTestData.COURSE, QuestionType.MULTICHOICE, marker + "-q1", marker + "-t1", "A1");
        Question q2 = createQuestionForCourse(ServiceTestData.COURSE, QuestionType.MULTICHOICE, marker + "-q2", marker + "-t2", "B1");

        saveDuplicateLink(q1, q2);
        saveDuplicateError(q1, MyUtil.REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS);
        saveDuplicateError(q2, MyUtil.REFORMULATE_QUESTION_ANSWER_ALREADY_EXISTS);

        service.removeDuplicateAssociations(q1.getId());

        assertTrue(questionDuplicateRepository.findByQuestionIdOrDuplicateQuestionId(q1.getId(), q1.getId()).isEmpty());
        assertTrue(questionErrorRepository.findByQuestionIdInAndDescriptionStartingWith(List.of(q1.getId(), q2.getId()), MyUtil.REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS).isEmpty());
        assertTrue(questionErrorRepository.findByQuestionIdInAndDescriptionStartingWith(List.of(q1.getId(), q2.getId()), MyUtil.REFORMULATE_QUESTION_ANSWER_ALREADY_EXISTS).isEmpty());
    }

    @Test
    void getQuestionDuplicates_returnsLinkedDuplicateDtos() {
        String marker = "it-duplicates-" + UUID.randomUUID();
        Question q1 = createQuestionForCourse(ServiceTestData.COURSE, QuestionType.MULTICHOICE, marker + "-q1", marker + "-t1", "A1");
        Question q2 = createQuestionForCourse(ServiceTestData.COURSE, QuestionType.MULTICHOICE, marker + "-q2", marker + "-t2", "B1");

        saveDuplicateLink(q1, q2);

        var dto = service.getQuestionDuplicates(q1.getId());

        assertEquals(q1.getId(), dto.getId());
        assertEquals(1, dto.getDuplicates().size());
        assertEquals(q2.getId(), dto.getDuplicates().getFirst().getQuestionId());
    }

    @Test
    void recomputeDuplicatesForCourse_isolatedCourse_cleansLegacyLinksAndErrors() {
        String marker = "course-clean-" + UUID.randomUUID();
        String course = "BD-IT-" + UUID.randomUUID();
        Question q1 = createQuestionForCourse(course, QuestionType.MULTICHOICE, marker + "-1", marker + "-text-1", "R1");
        Question q2 = createQuestionForCourse(course, QuestionType.MULTICHOICE, marker + "-2", marker + "-text-2", "R2");
        Question q3 = createQuestionForCourse(course, QuestionType.MULTICHOICE, marker + "-3", marker + "-text-3", "R3");

        saveDuplicateLink(q1, q2);
        saveDuplicateLink(q2, q3);

        saveDuplicateError(q1, MyUtil.REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS);
        saveDuplicateError(q2, MyUtil.REFORMULATE_QUESTION_ANSWER_ALREADY_EXISTS);
        saveDuplicateError(q3, MyUtil.REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS);

        QuestionDuplicationService.DuplicateRecomputeSummary summary = service.recomputeDuplicatesForCourse(course);

        assertEquals(3, summary.totalQuestions());
        assertTrue(summary.duplicateLinksRemoved() >= 2);
        assertTrue(summary.duplicateErrorsRemoved() >= 3);
    }

    @Test
    void recomputeDuplicatesWithStringEqualityStrategy_isolatedCourse_findsOnlyExactMatches() {
        String marker = "course-strategy-" + UUID.randomUUID();
        String course = "BD-IT-" + UUID.randomUUID();
        Question q1 = createQuestionForCourse(course, QuestionType.MULTICHOICE, marker + "-title", marker + "-text", "R1");
        Question q2 = createQuestionForCourse(course, QuestionType.MULTICHOICE, marker + "-TITLE", marker + "-TEXT", "R1");
        Question q3 = createQuestionForCourse(course, QuestionType.MULTICHOICE, marker + "-title-modified", marker + "-text", "R1");

        QuestionDuplicationService.DuplicateRecomputeSummary summary = service.recomputeDuplicatesForCourse(course, DuplicateComparisonStrategy.STRING_EQUALITY.getAlgorithmName());

        assertEquals(3, summary.totalQuestions());
        // String equality should only find q1 and q2 (exact case-insensitive match)
        assertTrue(summary.duplicateErrorsCreated() >= 2);
    }

    private Question createQuestionForCourse(String course, QuestionType type, String title, String text, String response1) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        TestEntityFactory.QuestionBankAuthorFixture fixture = testEntityFactory.createQuestionBankAuthorFixture(
                TestEntityFactory.QuestionBankAuthorSpec.builder()
                        .authorName("Author-" + suffix)
                        .initials("A" + suffix.substring(0, 2))
                        .questionBankName("QB-" + suffix)
                        .course(course)
                        .studyYear(StudyYear.Y2026_2027)
                        .source("test-" + suffix + ".xlsx")
                        .build()
        );

        Question question = new Question();
        question.setQuestionBankAuthor(fixture.questionBankAuthor());
        question.setType(type);
        question.setCrtNo(1);
        question.setTitle(title);
        question.setText(text);

        if (type == QuestionType.MULTICHOICE) {
            question.setResponse1(response1);
            question.setResponse2(response1 + "-2");
            question.setResponse3(response1 + "-3");
            question.setResponse4(response1 + "-4");
        } else {
            question.setResponse1(response1);
        }

        return questionRepository.save(question);
    }

    private void saveDuplicateLink(Question left, Question right) {
        long lowerId = Math.min(left.getId(), right.getId());
        long higherId = Math.max(left.getId(), right.getId());

        Question lower = questionRepository.findById(lowerId).orElseThrow();
        Question higher = questionRepository.findById(higherId).orElseThrow();

        QuestionDuplicate link = new QuestionDuplicate();
        link.setQuestion(lower);
        link.setDuplicateQuestion(higher);
        questionDuplicateRepository.save(link);
    }

    private void saveDuplicateError(Question question, String prefix) {
        QuestionError error = new QuestionError();
        error.setQuestion(question);
        error.setRowNumber(question.getCrtNo());
        error.setDescription(prefix + " (" + question.getTitle() + ")");
        questionErrorRepository.save(error);
    }
}
