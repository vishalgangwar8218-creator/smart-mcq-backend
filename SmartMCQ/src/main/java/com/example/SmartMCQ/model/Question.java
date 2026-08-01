package com.example.SmartMCQ.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "questions")
@Data
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;
    private String subject; // e.g., "Java", "Android"
    private String topic;   // e.g., "Polymorphism", "RecyclerView"

    @Column(name = "question_text", columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "optiona")
    private String optionA;
    @Column(name = "optionb")
    private String optionB;
    @Column(name = "optionc")
    private String optionC;
    @Column(name = "optiond")
    private String optionD;
    @Column(name = "correct_answer")
    private String correctAnswer;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation; // AI explanation ke liye
}
