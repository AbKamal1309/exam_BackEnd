package com.acoidemy.exambackend;

import com.acoidemy.exambackend.dtos.UserDTO;
import com.acoidemy.exambackend.entities.Answer;
import com.acoidemy.exambackend.entities.Exam;
import com.acoidemy.exambackend.entities.Question;
import com.acoidemy.exambackend.enums.AnswerStatus;
import com.acoidemy.exambackend.enums.ExamStatus;
import com.acoidemy.exambackend.exceptions.UserNotFoundException;
import com.acoidemy.exambackend.repositories.AnswerRepository;
import com.acoidemy.exambackend.repositories.ExamRepository;
import com.acoidemy.exambackend.repositories.QuestionRepository;
import com.acoidemy.exambackend.repositories.AppUserRepository;
import com.acoidemy.exambackend.services.AppUserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;



@SpringBootApplication
public class EXamBackendApplication {

    public static String getRandomStr(int n)
    {
        //choisissez un caractére au hasard à partir de cette chaîne
        String str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "abcdefghijklmnopqrstuvxyz";

        StringBuilder s = new StringBuilder(n);

        for (int i = 0; i < n; i++) {
            int index = (int)(str.length() * Math.random());
            s.append(str.charAt(index));
        }
        return s.toString();
    }

    public static void main(String[] args)
    {
        SpringApplication.run(EXamBackendApplication.class, args);

    }


    @Bean
    CommandLineRunner start(AppUserService userService
            , AppUserRepository appUserRepository
            , ExamRepository examRepository
            , QuestionRepository questionRepository
            , AnswerRepository answerRepository){
        Random nb =new Random();
        return args -> {
            Stream.of("Kamal","Hind","Jihad","Nouh")
                    .forEach(name->{
                        UserDTO userDTO=new UserDTO();
                        userDTO.setName(name);
                        userDTO.setEmail(name+"@gmail.com");
                        userDTO.setPassword(name+"123");
                        try {
                            userService.saveUser(userDTO);
                        } catch (UserNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    });
            appUserRepository.findAll().forEach(user -> {
                for (int i=0;i<5;i++){
                    Exam exam=new Exam();
                    exam.setCodeExam(UUID.randomUUID().toString());
                    exam.setDateCreation(new Date());
                    exam.setStatus(ExamStatus.CREATED);
                    exam.setNumberOfQuestions(nb.nextInt(5,10));
                    exam.setAppUser(user);
                    examRepository.save(exam);

                }
            });
            examRepository.findAll().forEach(exam -> {
                for (int i=0;i< exam.getNumberOfQuestions();i++){
                    Question question=new Question();
                    question.setCodeQuestion(UUID.randomUUID().toString());
                    question.setQuestionContent(getRandomStr(30));
                    question.setAppreciatedPoint(2);
                    question.setExam(exam);
                    questionRepository.save(question);
                }
            });
            questionRepository.findAll().forEach(question -> {
                for (int i=0;i<4;i++){
                    Answer answer=new Answer();
                    answer.setCodeAnswer(UUID.randomUUID().toString());
                    answer.setAnswerContent(getRandomStr(30));
                    //accountOperation.setType(Math.random()>0.5? OperationType.DEBIT: OperationType.CREDIT);
                    answer.setAnswerStatus(Math.random()>0.5? AnswerStatus.WRONG: AnswerStatus.CORRECT);
                    answer.setQuestion(question);
                    answerRepository.save(answer);
                }
            });


        };
    }

    /*@Bean
    BCryptPasswordEncoder  passwordEncoder(){
        return new BCryptPasswordEncoder();
    }*/

}
