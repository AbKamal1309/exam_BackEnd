package com.acoidemy.exambackend.services;

import com.acoidemy.exambackend.dtos.AnswerDTO;
import com.acoidemy.exambackend.dtos.GenerateQuestionsRequestDTO;
import com.acoidemy.exambackend.dtos.QuestionDTO;
import com.acoidemy.exambackend.enums.AnswerStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AiQuestionGenerationService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ⚠️ deepseek-chat retire le 24/07/2026 : on utilise directement le nouveau nom de modèle.
    @Value("${app.deepseek.api-key:}")
    private String apiKey;

    @Value("${app.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${app.deepseek.model:deepseek-v4-flash}")
    private String model;

    private static final int MAX_QUESTIONS = 20;
    private static final int MAX_REFERENCE_CHARS = 12000;
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10 Mo

    public List<QuestionDTO> generateQuestions(GenerateQuestionsRequestDTO request, MultipartFile file) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La génération IA n'est pas configurée (app.deepseek.api-key manquant côté serveur).");
        }
        if (request.getSubject() == null || request.getSubject().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le sujet est requis.");
        }

        int count = request.getNumberOfQuestions() == null
                ? 5
                : Math.min(Math.max(request.getNumberOfQuestions(), 1), MAX_QUESTIONS);

        String referenceText = extractTextFromFile(file);
        String prompt = buildPrompt(request, count, referenceText);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", 0.7);
        body.put("response_format", Map.of("type", "json_object"));
        body.put("messages", List.of(
                Map.of("role", "system", "content",
                        "Tu es un générateur de questions d'examen. Tu réponds UNIQUEMENT avec un objet JSON valide, sans texte ni balises markdown autour."),
                Map.of("role", "user", "content", prompt)
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        String content;
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/chat/completions", entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            content = root.path("choices").get(0).path("message").path("content").asText();
        } catch (HttpClientErrorException e) {
            log.error("Erreur appel DeepSeek (HTTP {}) : {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw switch (e.getStatusCode().value()) {
                case 402 -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Le service de génération IA est temporairement indisponible (quota/solde DeepSeek épuisé). Réessaie plus tard.");
                case 401, 403 -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Le service de génération IA est mal configuré côté serveur (clé API invalide).");
                case 429 -> new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Trop de demandes de génération IA en peu de temps. Réessaie dans quelques instants.");
                default -> new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Erreur lors de la génération IA (code " + e.getStatusCode().value() + ").");
            };
        } catch (Exception e) {
            log.error("Erreur appel DeepSeek", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Erreur lors de la génération IA : " + e.getMessage());
        }

        return parseQuestions(fixInvalidJsonEscapes(content));
    }

    private String extractTextFromFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Le fichier dépasse la taille maximale autorisée (10 Mo).");
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String text;

        try {
            if (filename.endsWith(".pdf")) {
                try (PDDocument document = PDDocument.load(file.getInputStream())) {
                    text = new PDFTextStripper().getText(document);
                }
            } else if (filename.endsWith(".docx")) {
                try (XWPFDocument document = new XWPFDocument(file.getInputStream());
                     XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                    text = extractor.getText();
                }
            } else if (filename.endsWith(".txt")) {
                text = new String(file.getBytes(), StandardCharsets.UTF_8);
            } else {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Format non supporté pour la génération IA (utilise .pdf, .docx ou .txt). " +
                                "Les images ne sont pas lisibles par l'IA.");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IOException e) {
            log.error("Erreur lecture du fichier de référence IA", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Impossible de lire le contenu du fichier fourni.");
        } catch (Exception e) {
            log.error("Erreur extraction texte du fichier de référence IA", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le fichier semble corrompu ou illisible.");
        }

        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Aucun texte exploitable n'a été trouvé dans le fichier.");
        }

        return text.length() > MAX_REFERENCE_CHARS ? text.substring(0, MAX_REFERENCE_CHARS) : text;
    }

    private String mapDifficulty(String difficulty) {
        if (difficulty == null) return "moyen";
        return switch (difficulty.toUpperCase()) {
            case "FACILE" -> "facile";
            case "AVANCE" -> "avancé";
            default -> "moyen";
        };
    }

    private String buildPrompt(GenerateQuestionsRequestDTO request, int count, String referenceText) {
        String lang = "ar".equalsIgnoreCase(request.getLanguage()) ? "arabe" : "français";
        boolean multi = request.getAllowMultipleCorrectAnswers() == null || request.getAllowMultipleCorrectAnswers();
        String difficulty = mapDifficulty(request.getDifficulty());

        String referenceBlock = (referenceText != null && !referenceText.isBlank())
                ? "Base-toi PRIORITAIREMENT sur le contenu suivant fourni par l'utilisateur pour générer les questions " +
                  "(reste dans le sujet qu'il couvre, ne l'ignore pas) :\n\"\"\"\n" + referenceText + "\n\"\"\"\n\n"
                : "";

        return """
                %sGénère %d questions à choix multiples sur le sujet "%s" pour un niveau "%s".
                Niveau de difficulté demandé : %s. Adapte la complexité des questions, le vocabulaire et
                les pièges éventuels des mauvaises réponses en fonction de ce niveau de difficulté.
                Langue de rédaction : %s.
                %s

                Pour les formules mathématiques (uniquement si le sujet s'y prête), utilise la notation LaTeX
                avec des antislashs, par exemple \\frac{a}{b}, \\sqrt{x}, x^{2}, x_{n}, \\int, \\sum, \\alpha.
                N'utilise aucune notation LaTeX si le sujet n'est pas mathématique.
                IMPORTANT : n'entoure JAMAIS les formules avec des délimiteurs comme \\( \\), \\[ \\], $ ou $$.
                Écris directement \\frac{2}{5} — jamais \\( \\frac{2}{5} \\) ni $\\frac{2}{5}$.
                Dans le JSON de ta réponse, chaque antislash DOIT être doublé pour rester valide
                (écris \\\\frac et non \\frac dans le JSON final).

                Réponds STRICTEMENT avec un objet JSON de cette forme, sans aucun texte ni markdown autour :
                {
                  "questions": [
                    {
                      "questionContent": "texte de la question",
                      "description": "indice optionnel ou explication courte (peut être une chaîne vide)",
                      "answers": [
                        { "answerContent": "texte de la réponse", "answerStatus": "CORRECT", "description": "" },
                        { "answerContent": "texte de la réponse", "answerStatus": "WRONG", "description": "" }
                      ]
                    }
                  ]
                }
                Chaque question doit avoir entre 3 et 5 réponses. "answerStatus" vaut uniquement "CORRECT" ou "WRONG".
                """.formatted(
                referenceBlock,
                count,
                request.getSubject(),
                request.getLevel() != null && !request.getLevel().isBlank() ? request.getLevel() : "non précisé",
                difficulty,
                lang,
                multi ? "Plusieurs bonnes réponses sont autorisées par question."
                        : "Une seule bonne réponse par question."
        );
    }

    private String cleanMathDelimiters(String text) {
        if (text == null) return null;
        return text
                .replace("\\(", "")
                .replace("\\)", "")
                .replace("\\[", "")
                .replace("\\]", "")
                .replaceAll("\\$\\$", "")
                .replaceAll("\\$", "")
                .trim();
    }

    /**
     * Les LLM oublient parfois de doubler les antislashs LaTeX (\frac au lieu de \\frac)
     * dans leur réponse JSON. Or "\f" est une séquence d'échappement JSON VALIDE (form feed) :
     * le parseur l'interprète silencieusement comme un caractère de contrôle invisible et
     * avale le "f", transformant "\frac{1}{3}" en "▯rac{1}{3}". On double ici tout antislash
     * qui n'est PAS déjà suivi d'une séquence d'échappement JSON valide, pour le préserver
     * comme antislash littéral une fois le JSON parsé.
     */
    private String fixInvalidJsonEscapes(String raw) {
        StringBuilder sb = new StringBuilder(raw.length() + 32);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\\') {
                char next = (i + 1 < raw.length()) ? raw.charAt(i + 1) : 0;
                boolean validEscape = next == '"' || next == '\\' || next == '/'
                        || next == 'b' || next == 'f' || next == 'n'
                        || next == 'r' || next == 't' || next == 'u';
                if (!validEscape) {
                    sb.append("\\\\");
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private List<QuestionDTO> parseQuestions(String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            JsonNode questionsNode = root.path("questions");
            List<QuestionDTO> result = new ArrayList<>();

            for (JsonNode qNode : questionsNode) {
                QuestionDTO q = new QuestionDTO();
                q.setQuestionContent(cleanMathDelimiters(qNode.path("questionContent").asText("")));
                q.setDescription(cleanMathDelimiters(qNode.path("description").asText("")));

                List<AnswerDTO> answers = new ArrayList<>();
                for (JsonNode aNode : qNode.path("answers")) {
                    AnswerDTO a = new AnswerDTO();
                    a.setAnswerContent(cleanMathDelimiters(aNode.path("answerContent").asText("")));
                    String status = aNode.path("answerStatus").asText("WRONG");
                    a.setAnswerStatus("CORRECT".equalsIgnoreCase(status) ? AnswerStatus.CORRECT : AnswerStatus.WRONG);
                    a.setDescription(cleanMathDelimiters(aNode.path("description").asText("")));
                    answers.add(a);
                }
                q.setAnswers(answers);
                result.add(q);
            }

            if (result.isEmpty()) {
                throw new IllegalStateException("Aucune question dans la réponse IA");
            }
            return result;
        } catch (Exception e) {
            log.error("Erreur parsing réponse IA : {}", content, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "La réponse de l'IA n'a pas pu être interprétée. Réessaie.");
        }
    }
}