package com.acoidemy.exambackend.web;

import com.acoidemy.exambackend.dtos.GenerateQuestionsRequestDTO;
import com.acoidemy.exambackend.dtos.QuestionDTO;
import com.acoidemy.exambackend.services.AiQuestionGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiQuestionGenerationService aiQuestionGenerationService;

    // Génère des questions SANS les enregistrer : le frontend les ajoute au
    // formulaire d'examen pour relecture/édition avant l'enregistrement définitif.
    // "file" est optionnel : un document de référence (PDF/DOCX/TXT) dont le texte
    // est extrait côté serveur et injecté dans le prompt IA.
    @PostMapping(value = "/generate-questions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public List<QuestionDTO> generateQuestions(
            @RequestPart("request") GenerateQuestionsRequestDTO request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return aiQuestionGenerationService.generateQuestions(request, file);
    }
}
