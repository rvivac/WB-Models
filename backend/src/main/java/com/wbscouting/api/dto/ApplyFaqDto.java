package com.wbscouting.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyFaqDto {
    private UUID id;
    private String question;
    private String answer;
    private String questionEn;
    private String answerEn;
    private Integer order;
    private Integer displayOrder;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getQuestionEn() { return questionEn; }
    public void setQuestionEn(String questionEn) { this.questionEn = questionEn; }

    public String getAnswerEn() { return answerEn; }
    public void setAnswerEn(String answerEn) { this.answerEn = answerEn; }

    public Integer getOrder() { return order != null ? order : (displayOrder != null ? displayOrder + 1 : 1); }
    public void setOrder(Integer order) { this.order = order; }

    public Integer getDisplayOrder() { return displayOrder != null ? displayOrder : (order != null ? order - 1 : 0); }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
