package com.acoidemy.exambackend.web;

import com.acoidemy.exambackend.dtos.*;
import com.acoidemy.exambackend.exceptions.AnswerNotFoundException;
import com.acoidemy.exambackend.exceptions.ExamNotFoundException;
import com.acoidemy.exambackend.exceptions.TestNotFoundException;
import com.acoidemy.exambackend.exceptions.UserNotFoundException;
import com.acoidemy.exambackend.services.DashBoardService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@Slf4j
public class DashBoardController {

    private DashBoardService dashBoardService;
    @GetMapping("/dashboard/score")
    public ResponseTestScoreDTO getScoreTest(@RequestParam String testId)
            throws AnswerNotFoundException, TestNotFoundException {
        RequestTestScoreDTO request = new RequestTestScoreDTO();
        request.setTestId(testId);
        return dashBoardService.getScoreTest(request);
    }
    @GetMapping("/dashboard/allTestForExam")
    public ResponseEntity<ResponseAllTestExam> getAllTest(@RequestParam String examId) throws ExamNotFoundException {
        RequestAllTestExam request = new RequestAllTestExam();
        request.setExamId(examId);
        ResponseAllTestExam response = dashBoardService.getAllTestExam(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard/allTestForExam/{userId}")
    public ResponseAllTestUser getAllTestUser(@PathVariable Long userId) throws UserNotFoundException {
        return dashBoardService.getAllTestUser(userId);
    }

    @GetMapping("/dashboard/popularExams")
    public List<String> getPopularExams(){
        return dashBoardService.getMostPopularExam();
    }

    @GetMapping("/dashboard/bestScore/{examId}")
    public BestScoreDTO getBestScore(@PathVariable String examId) throws ExamNotFoundException {
        return dashBoardService.getBestScoreForExam(examId);
    }

    @GetMapping("/dashboard/bestScoreName/{examId}")
    public String getBestScoreName(@PathVariable String examId) throws ExamNotFoundException {
        return dashBoardService.getBestUserScored(examId);
    }



}
