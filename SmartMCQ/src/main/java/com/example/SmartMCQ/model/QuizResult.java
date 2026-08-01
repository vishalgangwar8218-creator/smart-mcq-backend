package com.example.SmartMCQ.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_results")
@Data
public class QuizResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String subject;
    private String topic;
    private int score;
    private int totalQuestions;
    private String timeTaken;
    private LocalDateTime createdAt = LocalDateTime.now();
}
