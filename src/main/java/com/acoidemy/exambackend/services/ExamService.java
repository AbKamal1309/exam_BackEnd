package com.acoidemy.exambackend.services;

import com.acoidemy.exambackend.dtos.*;
import com.acoidemy.exambackend.exceptions.AnswerNotFoundException;
import com.acoidemy.exambackend.exceptions.ExamNotFoundException;
import com.acoidemy.exambackend.exceptions.QuestionNotFoundException;
import com.acoidemy.exambackend.exceptions.UserNotFoundException;

import java.util.List;

public interface ExamService {

    ExamDTO saveExam(ExamDTO examDTO) throws UserNotFoundException, ExamNotFoundException;

    List<ExamDTO> listExams();

    ExamDTO getExam(String codeExam) throws ExamNotFoundException;

    ExamDTO updateExam(ExamDTO examDTO) throws UserNotFoundException, ExamNotFoundException;

    void deleteExam(String codeExam);

    QuestionDTO saveQuestion(QuestionDTO questionDTO) throws ExamNotFoundException;

    QuestionDTO saveQuestionWithAnswers(QuestionDTO questionDTO) throws ExamNotFoundException;

    List<QuestionDTO> listAllQuestions();//list all questions in the DB
    List<QuestionDTO> listQuestions(String codeExam) throws ExamNotFoundException;//list questions for exam
    QuestionDTO getQuestion(String codeQuestion) throws QuestionNotFoundException;
    QuestionDTO updateQuestion(QuestionDTO questionDTO) throws QuestionNotFoundException;
    void deleteQuestion(String codeQuestion);

    AnswerDTO saveAnswer(AnswerDTO answerDTO) throws QuestionNotFoundException;
    List<AnswerDTO> listAllAnswers();//list all answers in the DB
    List<AnswerDTO> listAnswers(String codeQuestion) throws QuestionNotFoundException;
    AnswerDTO getAnswer(String codeAnswer) throws AnswerNotFoundException;
    AnswerDTO updateAnswer(AnswerDTO answerDTO) throws QuestionNotFoundException, AnswerNotFoundException;
    void deleteAnswer(String codeAnswer);


    QuestionDTO updateQuestionWithAnswers(QuestionDTO questionDTO) throws QuestionNotFoundException;


    List<ExamDTO> listExamsByUser(Long userId) throws UserNotFoundException;

    ExamDTO saveExamAllQuestionsAndAnswers(ExamDTO examDTO) throws UserNotFoundException;



    //AccountHistoryDTO getAccountHistory(String accountId, int page, int size) throws BankAccountNotFoundException;

}
