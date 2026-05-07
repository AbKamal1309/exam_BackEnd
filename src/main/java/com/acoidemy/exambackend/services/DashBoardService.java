package com.acoidemy.exambackend.services;

import com.acoidemy.exambackend.dtos.*;
import com.acoidemy.exambackend.exceptions.AnswerNotFoundException;
import com.acoidemy.exambackend.exceptions.ExamNotFoundException;
import com.acoidemy.exambackend.exceptions.TestNotFoundException;
import com.acoidemy.exambackend.exceptions.UserNotFoundException;

import java.util.List;

public interface DashBoardService {


    ResponseTestScoreDTO getScoreTest(RequestTestScoreDTO requestTestScore) throws TestNotFoundException, AnswerNotFoundException;

    ResponseAllTestExam getAllTestExam(RequestAllTestExam requestAllTestExam) throws ExamNotFoundException;

    ResponseAllTestUser getAllTestUser(Long userId) throws UserNotFoundException;

    List<String> getMostPopularExam();

    BestScoreDTO getBestScoreForExam(String examId) throws ExamNotFoundException;

    String getBestUserScored(String examId) throws ExamNotFoundException;
}
