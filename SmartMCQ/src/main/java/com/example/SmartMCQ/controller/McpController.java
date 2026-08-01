package com.example.SmartMCQ.controller;

import com.example.SmartMCQ.model.Question;
import com.example.SmartMCQ.model.SearchHistory;
import com.example.SmartMCQ.repository.QuestionRepository;
import com.example.SmartMCQ.repository.SearchHistoryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mcp")
public class McpController {
    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/tools/call")
    public Map<String, Object> callTool(@RequestBody Map<String, Object> request) {
        String toolName = (String) request.get("name");
        Map<String, Object> arguments = (Map<String, Object>) request.get("arguments");

        Map<String, Object> response = new HashMap<>();

        if ("get_questions".equals(toolName) && arguments != null) {
            String subject = ((String) arguments.get("subject")).trim();
            String topic = ((String) arguments.get("topic")).trim();

            // 🔥 Android se "count" uthao (Safe handling ke sath agar nahi bheja toh default 5)
            int count = 5;
            if (arguments.containsKey("count")) {
                count = Integer.parseInt(arguments.get("count").toString());
            }

            // 1. Pehle Supabase check karo ki kya pehle se questions hain
            List<Question> questions = questionRepository.findBySubjectIgnoreCaseAndTopicIgnoreCase(subject, topic);

            // 2. AGAR DATABASE ME NAHI HAIN, TOH AI SE GENERATE KARWAO!
            if (questions.isEmpty()) {
                System.out.println("Database empty! Requesting Gemini AI to generate" + count + " questions for: " + topic);
                questions = generateQuestionsWithAI(subject, topic, count);

                // Generated questions ko Supabase me save kar lo taaki agli baar speed fast rahe
                if (!questions.isEmpty()){
                    System.out.println("Saving AI generated questions into Supabase...");

                    for (Question q: questions) {
                        q.setSubject(subject);
                        q.setTopic(topic);
                    }
                    questionRepository.saveAll(questions);
                }
            } else {
                System.out.println("Data found in Supabase! Serving from database. Total: " + questions.size());
            }

            if (arguments.containsKey("userId") && !questions.isEmpty()) {
                try {
                    Long userId = Long.parseLong(arguments.get("userId").toString());

                    SearchHistory history = new SearchHistory();
                    history.setUserId(userId);
                    history.setSubject(subject);
                    history.setTopic(topic);

                    // Supabase ki search_history table me save ho jayega
                    searchHistoryRepository.save(history);
                    System.out.println("🕒 User History Saved successfully for userId: " + userId);
                } catch (Exception e) {
                    System.err.println("❌ History save karne me panga hua: " + e.getMessage());
                }
            }

            response.put("status", "success");
            response.put("content", questions);
        } else {
            response.put("status", "error");
            response.put("message", "Tool not found or invalid arguments");
        }

        return response;
    }

    // REAL SMART AI LOGIC: Gemini API Call to Generate MCQs
    private List<Question> generateQuestionsWithAI(String subject, String topic, int count) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;

        // Prompt Engineering: AI ko strict instructions dena ki hume sirf pure JSON chahiye
        String prompt = " Generate exactly"+ count + " high-quality multiple choice questions (MCQs) for the subject '" + subject + "' and topic '" + topic + "'. "
                + "Provide the output strictly as a JSON array where each object has these exact keys: "
                + "'subject', 'topic', 'questionText', 'optionA', 'optionB', 'optionC', 'optionD', 'correctAnswer', 'explanation'. "
                + "The 'correctAnswer' must be only a single character from 'A', 'B', 'C' or 'D'. Do not include markdown or backticks like ```json.";

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> jsonModeConfig = Map.of("responseMimeType", "application/json");
            // Gemini standard request structure
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", jsonModeConfig
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> apiResponse = restTemplate.postForEntity(url, entity, String.class);

            // Response se raw text nikalna
            Map<String, Object> responseMap = objectMapper.readValue(apiResponse.getBody(), new TypeReference<>() {});
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>)  content.get("parts");
            String rawJsonText = (String) parts.get(0).get("text");

            if (rawJsonText.contains("```")){
                rawJsonText = rawJsonText.replaceAll("```json","").replaceAll("```","").trim();
            }

            System.out.println("Cleaned AI JSON: " + rawJsonText);

            // Response se raw text nikalna
            return objectMapper.readValue(rawJsonText, new TypeReference<List<Question>>() {});
        } catch (Exception e) {
            System.err.println("Error generating questions with AI: " + e.getMessage());
            e.printStackTrace();
            return List.of(); // Error aane par empty list return karega
        }
    }

    @GetMapping("/tools")
    public Map<String, Object> listTools() {
        Map<String, Object> tools = new HashMap<>();
        tools.put("name", "get_questions");
        tools.put("description", "Fetch or dynamically generate high-quality MCQs using AI based on subject and topic.");

        return Map.of("tools", List.of(tools));
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<List<SearchHistory>> getUserHistory(@PathVariable("userId") Long userId) {
        try {
            List<SearchHistory> historyList = searchHistoryRepository.findByUserIdOrderBySearchedAtDesc(userId);
            return ResponseEntity.ok(historyList);
        }catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @DeleteMapping("/history/delete/{id}")
    public ResponseEntity<?> deleteHistoryItem(@PathVariable("id") Long id) {
        try {
            if (searchHistoryRepository.existsById(id)) {
                searchHistoryRepository.deleteById(id);
                return ResponseEntity.ok(Map.of("success", true, "message", "History deleted successfully!"));
            } else {
                return ResponseEntity.status(404).body(Map.of("success", false, "message", "Item not found"));
            }
        }catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Server Error" + e.getMessage()));
        }
    }
}
