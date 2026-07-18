package com.acoidemy.exambackend;

import com.acoidemy.exambackend.dtos.UserDTO;
import com.acoidemy.exambackend.entities.Answer;
import com.acoidemy.exambackend.entities.Exam;
import com.acoidemy.exambackend.entities.Question;
import com.acoidemy.exambackend.enums.AnswerStatus;
import com.acoidemy.exambackend.enums.ExamStatus;
import com.acoidemy.exambackend.enums.ExamVisibility;
import com.acoidemy.exambackend.exceptions.UserNotFoundException;
import com.acoidemy.exambackend.repositories.AnswerRepository;
import com.acoidemy.exambackend.repositories.ExamRepository;
import com.acoidemy.exambackend.repositories.QuestionRepository;
import com.acoidemy.exambackend.repositories.AppUserRepository;
import com.acoidemy.exambackend.services.AppUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.acoidemy.exambackend")
@EntityScan(basePackages = "com.acoidemy.exambackend.entities")
@Slf4j
public class EXamBackendApplication {

    public static String getRandomStr(int n) {
        String str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvxyz";
        StringBuilder s = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            int index = (int) (str.length() * Math.random());
            s.append(str.charAt(index));
        }
        return s.toString();
    }

    public static void main(String[] args) {
        SpringApplication.run(EXamBackendApplication.class, args);
    }

    /**
     * Données de démo (utilisateurs + examens factices).
     *
     * ⚠️ NE S'EXÉCUTE QU'EN PROFIL "dev" (spring.profiles.active=dev) — jamais en production.
     * Idempotent : ne recrée pas un utilisateur qui existe déjà, donc ne plante plus le
     * démarrage même si la base n'est pas réinitialisée.
     * @Order(1) : s'exécute AVANT AdminBootstrap, pour que "Kamal" existe déjà quand
     * AdminBootstrap tente de le promouvoir ADMIN.
     */
    @Bean
    @Profile("dev")
    @Order(1)
    CommandLineRunner start(AppUserService userService,
                             AppUserRepository appUserRepository,
                             ExamRepository examRepository,
                             QuestionRepository questionRepository,
                             AnswerRepository answerRepository) {
        Random nb = new Random();
        return args -> {
            Stream.of("Kamal", "Hind", "Jihad", "Nouh").forEach(name -> {
                String email = name + "@gmail.com";
                if (appUserRepository.findByEmail(email) != null) {
                    log.info("[SEED] Utilisateur {} existe déjà, on ne le recrée pas.", email);
                    return;
                }
                UserDTO userDTO = new UserDTO();
                userDTO.setName(name);
                userDTO.setEmail(email);
                userDTO.setPassword(name + "123");
                try {
                    userService.saveUser(userDTO);
                    log.info("[SEED] Utilisateur {} créé.", email);
                } catch (UserNotFoundException e) {
                    log.warn("[SEED] Impossible de créer {} : {}", email, e.getMessage());
                }
            });

            // N'ajoute des examens factices que s'il n'y en a pas déjà (évite d'en re-générer
            // des dizaines à chaque redémarrage si la base persiste).
            if (examRepository.count() > 0) {
                return;
            }

            appUserRepository.findAll().forEach(user -> {
                for (int i = 0; i < 5; i++) {
                    Exam exam = new Exam();
                    exam.setCodeExam(UUID.randomUUID().toString().substring(0, 8));
                    exam.setDateCreation(new Date());
                    exam.setStatus(ExamStatus.CREATED);
                    exam.setNumberOfQuestions(nb.nextInt(5, 10));
                    exam.setVisibility(ExamVisibility.PRIVATE);
                    exam.setAppUser(user);
                    examRepository.save(exam);
                }
            });

            examRepository.findAll().forEach(exam -> {
                for (int i = 0; i < exam.getNumberOfQuestions(); i++) {
                    Question question = new Question();
                    question.setCodeQuestion(UUID.randomUUID().toString().substring(0, 8));
                    question.setQuestionContent(getRandomStr(30));
                    question.setAppreciatedPoint(2);
                    question.setExam(exam);
                    questionRepository.save(question);
                }
            });

            questionRepository.findAll().forEach(question -> {
                for (int i = 0; i < 4; i++) {
                    Answer answer = new Answer();
                    answer.setCodeAnswer(UUID.randomUUID().toString().substring(0, 8));
                    answer.setAnswerContent(getRandomStr(30));
                    answer.setAnswerStatus(Math.random() > 0.5 ? AnswerStatus.WRONG : AnswerStatus.CORRECT);
                    answer.setQuestion(question);
                    answerRepository.save(answer);
                }
            });
        };
    }
}
