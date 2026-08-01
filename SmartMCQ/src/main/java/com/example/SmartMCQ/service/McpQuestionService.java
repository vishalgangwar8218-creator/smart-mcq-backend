package com.example.SmartMCQ.service;

import com.example.SmartMCQ.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class McpQuestionService {

    @Autowired
    private QuestionRepository questionRepository;

    
}
