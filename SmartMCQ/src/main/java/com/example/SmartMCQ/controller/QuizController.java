package com.example.SmartMCQ.controller;

import com.example.SmartMCQ.model.QuizResult;
import com.example.SmartMCQ.repository.QuizResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    @Autowired
    private QuizResultRepository quizResultRepository;

    // 1. Quiz Khatam hone par result save karne ke liye endpoint
    @PostMapping("/save-result")
    public ResponseEntity<?> saveResult(@RequestBody QuizResult quizResult) {
        try {
            QuizResult savedResult = quizResultRepository.save(quizResult);
            return ResponseEntity.ok(savedResult);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error saving result: " + e.getMessage());
        }
    }
}
