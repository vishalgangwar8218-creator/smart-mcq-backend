package com.example.SmartMCQ.repository;

import com.example.SmartMCQ.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    // Subject aur Topic ke basis par database se MCQs dhoondne ke liye query
    List<Question> findBySubjectIgnoreCaseAndTopicIgnoreCase(String subject, String topic);
}
