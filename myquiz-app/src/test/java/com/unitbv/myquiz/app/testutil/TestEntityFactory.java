package com.unitbv.myquiz.app.testutil;
import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.api.types.StudyYear;
import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuestionBank;
import com.unitbv.myquiz.app.entities.QuestionBankAuthor;
import com.unitbv.myquiz.app.repositories.AuthorRepository;
import com.unitbv.myquiz.app.repositories.QuestionRepository;
import com.unitbv.myquiz.app.repositories.QuestionBankAuthorRepository;
import com.unitbv.myquiz.app.repositories.QuestionBankRepository;
import com.unitbv.myquiz.app.services.CourseService;
import org.springframework.stereotype.Component;
@Component
public class TestEntityFactory {
    private final AuthorRepository authorRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionBankAuthorRepository questionBankAuthorRepository;
    private final QuestionRepository questionRepository;
    private final CourseService courseService;
    public TestEntityFactory(AuthorRepository authorRepository,
                             QuestionBankRepository questionBankRepository,
                             QuestionBankAuthorRepository questionBankAuthorRepository,
                             QuestionRepository questionRepository,
                             CourseService courseService) {
        this.authorRepository = authorRepository;
        this.questionBankRepository = questionBankRepository;
        this.questionBankAuthorRepository = questionBankAuthorRepository;
        this.questionRepository = questionRepository;
        this.courseService = courseService;
    }
    public Author createAuthor(String name, String initials) {
        return authorRepository.save(new Author(name, initials));
    }

    public QuestionBank createQuestionBank(String questionBankName, String course, StudyYear studyYear) {
        QuestionBank questionBank = new QuestionBank();
        questionBank.setName(questionBankName);
        questionBank.setCourse(courseService.getOrCreateCourseEntity(course));
        questionBank.setStudyYear(studyYear);
        return questionBankRepository.save(questionBank);
    }
    public QuestionBankAuthor createQuestionBankAuthor(Author author, QuestionBank questionBank, String source) {
        QuestionBankAuthor questionBankAuthor = new QuestionBankAuthor();
        questionBankAuthor.setAuthor(author);
        questionBankAuthor.setQuestionBank(questionBank);
        questionBankAuthor.setSource(source);
        return questionBankAuthorRepository.save(questionBankAuthor);
    }
    public Question createQuestion(QuestionBankAuthor questionBankAuthor, QuestionType type, String title, String text) {
        Question question = new Question();
        question.setQuestionBankAuthor(questionBankAuthor);
        question.setType(type);
        question.setTitle(title);
        question.setText(text);
        return questionRepository.save(question);
    }

    public QuestionBankAuthorFixture createQuestionBankAuthorFixture(QuestionBankAuthorSpec spec) {
        return createQuestionBankAuthorFixture(
                spec.authorName,
                spec.initials,
                spec.questionBankName,
                spec.course,
                spec.studyYear,
                spec.source
        );
    }

    public QuestionBankAuthorFixture createQuestionBankAuthorFixture(String authorName,
                                                      String initials,
                                                      String questionBankName,
                                                      String course,
                                                      StudyYear studyYear,
                                                      String source) {
        Author author = createAuthor(authorName, initials);
        QuestionBank questionBank = createQuestionBank(questionBankName, course, studyYear);
        QuestionBankAuthor questionBankAuthor = createQuestionBankAuthor(author, questionBank, source);
        return new QuestionBankAuthorFixture(author, questionBank, questionBankAuthor);
    }

    public QuestionFixture createQuestionFixture(QuestionSpec spec) {
        return createQuestionFixture(
                spec.authorName,
                spec.initials,
                spec.questionBankName,
                spec.course,
                spec.studyYear,
                spec.source,
                spec.type,
                spec.title,
                spec.text
        );
    }

    public QuestionFixture createQuestionFixture(String authorName,
                                                 String initials,
                                                 String questionBankName,
                                                 String course,
                                                 StudyYear studyYear,
                                                 String source,
                                                 QuestionType type,
                                                 String title,
                                                 String text) {
        QuestionBankAuthorFixture fixture = createQuestionBankAuthorFixture(authorName, initials, questionBankName, course, studyYear, source);
        Question question = createQuestion(fixture.questionBankAuthor(), type, title, text);
        return new QuestionFixture(fixture.author(), fixture.questionBank(), fixture.questionBankAuthor(), question);
    }
    public void cleanupQuestionFixture(QuestionFixture fixture) {
        if (fixture == null) {
            return;
        }
        if (fixture.question() != null && fixture.question().getId() != null) {
            questionRepository.findById(fixture.question().getId()).ifPresent(questionRepository::delete);
        }
        cleanupQuestionBankAuthorFixture(new QuestionBankAuthorFixture(fixture.author(), fixture.questionBank(), fixture.questionBankAuthor()));
    }
    public void cleanupQuestionBankAuthorFixture(QuestionBankAuthorFixture fixture) {
        if (fixture == null) {
            return;
        }
        if (fixture.questionBankAuthor() != null && fixture.questionBankAuthor().getId() != null) {
            questionBankAuthorRepository.findById(fixture.questionBankAuthor().getId()).ifPresent(questionBankAuthorRepository::delete);
        }
        if (fixture.questionBank() != null && fixture.questionBank().getId() != null) {
            questionBankRepository.findById(fixture.questionBank().getId()).ifPresent(questionBankRepository::delete);
        }
        if (fixture.author() != null && fixture.author().getId() != null) {
            authorRepository.findById(fixture.author().getId()).ifPresent(authorRepository::delete);
        }
    }
    public record QuestionBankAuthorFixture(Author author, QuestionBank questionBank, QuestionBankAuthor questionBankAuthor) {
    }
    public record QuestionFixture(Author author, QuestionBank questionBank, QuestionBankAuthor questionBankAuthor, Question question) {
    }

    public static final class QuestionBankAuthorSpec {
        private final String authorName;
        private final String initials;
        private final String questionBankName;
        private final String course;
        private final StudyYear studyYear;
        private final String source;

        private QuestionBankAuthorSpec(Builder builder) {
            this.authorName = builder.authorName;
            this.initials = builder.initials;
            this.questionBankName = builder.questionBankName;
            this.course = builder.course;
            this.studyYear = builder.studyYear;
            this.source = builder.source;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String authorName;
            private String initials;
            private String questionBankName;
            private String course;
            private StudyYear studyYear;
            private String source;

            public Builder authorName(String authorName) {
                this.authorName = authorName;
                return this;
            }

            public Builder initials(String initials) {
                this.initials = initials;
                return this;
            }

            public Builder questionBankName(String questionBankName) {
                this.questionBankName = questionBankName;
                return this;
            }

            public Builder course(String course) {
                this.course = course;
                return this;
            }

            public Builder studyYear(StudyYear studyYear) {
                this.studyYear = studyYear;
                return this;
            }

            public Builder source(String source) {
                this.source = source;
                return this;
            }

            public QuestionBankAuthorSpec build() {
                return new QuestionBankAuthorSpec(this);
            }
        }
    }

    public static final class QuestionSpec {
        private final String authorName;
        private final String initials;
        private final String questionBankName;
        private final String course;
        private final StudyYear studyYear;
        private final String source;
        private final QuestionType type;
        private final String title;
        private final String text;

        private QuestionSpec(Builder builder) {
            this.authorName = builder.authorName;
            this.initials = builder.initials;
            this.questionBankName = builder.questionBankName;
            this.course = builder.course;
            this.studyYear = builder.studyYear;
            this.source = builder.source;
            this.type = builder.type;
            this.title = builder.title;
            this.text = builder.text;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String authorName;
            private String initials;
            private String questionBankName;
            private String course;
            private StudyYear studyYear;
            private String source;
            private QuestionType type;
            private String title;
            private String text;

            public Builder authorName(String authorName) {
                this.authorName = authorName;
                return this;
            }

            public Builder initials(String initials) {
                this.initials = initials;
                return this;
            }

            public Builder questionBankName(String questionBankName) {
                this.questionBankName = questionBankName;
                return this;
            }

            public Builder course(String course) {
                this.course = course;
                return this;
            }

            public Builder studyYear(StudyYear studyYear) {
                this.studyYear = studyYear;
                return this;
            }

            public Builder source(String source) {
                this.source = source;
                return this;
            }

            public Builder type(QuestionType type) {
                this.type = type;
                return this;
            }

            public Builder title(String title) {
                this.title = title;
                return this;
            }

            public Builder text(String text) {
                this.text = text;
                return this;
            }

            public QuestionSpec build() {
                return new QuestionSpec(this);
            }
        }
    }
}
