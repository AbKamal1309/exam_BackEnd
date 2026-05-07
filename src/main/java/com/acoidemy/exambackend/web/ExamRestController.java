package com.acoidemy.exambackend.web;

import com.acoidemy.exambackend.dtos.AnswerDTO;
import com.acoidemy.exambackend.dtos.ExamDTO;
import com.acoidemy.exambackend.dtos.QuestionDTO;
import com.acoidemy.exambackend.exceptions.AnswerNotFoundException;
import com.acoidemy.exambackend.exceptions.ExamNotFoundException;
import com.acoidemy.exambackend.exceptions.QuestionNotFoundException;
import com.acoidemy.exambackend.exceptions.UserNotFoundException;
import com.acoidemy.exambackend.services.ExamService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@Slf4j
@CrossOrigin("*")
public class ExamRestController {

    private ExamService examService;

    @GetMapping("/exams")
    public List<ExamDTO> exams(){
        return examService.listExams();
    }

    @GetMapping("/exams/{id}")
    public ExamDTO getExam(@PathVariable(name = "id") String codeExam) throws ExamNotFoundException{
        return examService.getExam(codeExam);
    }
    @GetMapping("/examsOfUser/{userId}")
    public List<ExamDTO> getExamsForUser(@PathVariable Long userId) throws UserNotFoundException {
        return examService.listExamsByUser(userId);
    }

    @PostMapping("/exams")
    public ExamDTO saveExam(@RequestBody ExamDTO examDTO) throws UserNotFoundException, ExamNotFoundException {
        return examService.saveExam(examDTO);
    }

    @PostMapping("/examAllQuestionsAndAnswers/{userId}")
    public ExamDTO saveExamAllQuestionsAndAnswers(@PathVariable Long userId
            ,@RequestBody ExamDTO examDTO)
            throws UserNotFoundException {
        examDTO.setUserId(userId);
        return examService.saveExamAllQuestionsAndAnswers(examDTO);
    }

    @PutMapping("/exams/{codeExam}")
    public ExamDTO updateExam(@PathVariable String codeExam,@RequestBody ExamDTO examDTO)
            throws UserNotFoundException, ExamNotFoundException {
        examDTO.setCodeExam(codeExam);
        return examService.updateExam(examDTO);
    }

    @GetMapping("/questions")
    public List<QuestionDTO> getAllQuestions(){

        return examService.listAllQuestions();
    }

    @GetMapping("/exams/{codeExam}/questions")
    public List<QuestionDTO> getQuestions(@PathVariable String codeExam) throws ExamNotFoundException {
        return examService.listQuestions(codeExam);
    }
    @GetMapping("/questions/{codeQuestion}")
    public QuestionDTO getQuestion(@PathVariable String codeQuestion) throws QuestionNotFoundException {
        return examService.getQuestion(codeQuestion);
    }
    @PostMapping("/question")
    public QuestionDTO saveQuestion(@RequestBody QuestionDTO questionDTO) throws ExamNotFoundException {
        return examService.saveQuestionWithAnswers(questionDTO);
    }
    @PostMapping("/questionAndAnswers")
    public QuestionDTO saveQuestionAndAnswers(@RequestBody QuestionDTO questionDTO) throws ExamNotFoundException {

        return examService.saveQuestionWithAnswers(questionDTO);
    }

    @GetMapping("/answers")
    public List<AnswerDTO> getAllAnswers(){
        return examService.listAllAnswers();
    }

    @GetMapping("/answers/{codeQuestion}")
    public List<AnswerDTO> getAnswers(@PathVariable String codeQuestion) throws QuestionNotFoundException {
        return examService.listAnswers(codeQuestion);
    }

    @GetMapping("/{codeAnswer}")
    public AnswerDTO getAnswer(@PathVariable String codeAnswer) throws AnswerNotFoundException {
        return examService.getAnswer(codeAnswer);
    }

    @PostMapping("/answer")
    public AnswerDTO saveAnswer(@RequestBody AnswerDTO answerDTO) throws QuestionNotFoundException {
        return examService.saveAnswer(answerDTO);
    }
    @PutMapping("/question/{codeQuestion}")
    public QuestionDTO updateQuestion(@PathVariable String codeQuestion,@RequestBody QuestionDTO questionDTO)
            throws QuestionNotFoundException {
        questionDTO.setCodeQuestion(codeQuestion);
        return examService.updateQuestion(questionDTO);
    }
    @PutMapping("/questionAndAnswers/{codeQuestion}")
    public QuestionDTO updateQuestionWithAnswers(@PathVariable String codeQuestion,@RequestBody QuestionDTO questionDTO)
            throws QuestionNotFoundException {
        questionDTO.setCodeQuestion(codeQuestion);
        return examService.updateQuestionWithAnswers(questionDTO);
    }
    @PutMapping("/answer/{codeAnswer}")
    public AnswerDTO updateAnswer(@PathVariable String codeAnswer,@RequestBody AnswerDTO answerDTO)
            throws AnswerNotFoundException, QuestionNotFoundException {

        answerDTO.setCodeAnswer(codeAnswer);
        return examService.updateAnswer(answerDTO);
    }


}
