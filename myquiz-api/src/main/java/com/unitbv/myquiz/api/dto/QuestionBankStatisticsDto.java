package com.unitbv.myquiz.api.dto;

import java.util.ArrayList;
import java.util.List;

public class QuestionBankStatisticsDto {
    private QuestionBankSummaryDto questionBank;
    private List<AuthorStatsDto> authorStats = new ArrayList<>();

    public QuestionBankSummaryDto getQuestionBank() {
        return questionBank;
    }

    public void setQuestionBank(QuestionBankSummaryDto questionBank) {
        this.questionBank = questionBank;
    }

    public List<AuthorStatsDto> getAuthorStats() {
        return authorStats;
    }

    public void setAuthorStats(List<AuthorStatsDto> authorStats) {
        this.authorStats = authorStats;
    }

    public static class AuthorStatsDto {
        private Long authorId;
        private String authorName;
        private int mcCount;
        private int tfCount;
        private int errorCount;

        public Long getAuthorId() {
            return authorId;
        }

        public void setAuthorId(Long authorId) {
            this.authorId = authorId;
        }

        public String getAuthorName() {
            return authorName;
        }

        public void setAuthorName(String authorName) {
            this.authorName = authorName;
        }

        public int getMcCount() {
            return mcCount;
        }

        public void setMcCount(int mcCount) {
            this.mcCount = mcCount;
        }

        public int getTfCount() {
            return tfCount;
        }

        public void setTfCount(int tfCount) {
            this.tfCount = tfCount;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public void setErrorCount(int errorCount) {
            this.errorCount = errorCount;
        }
    }
}

