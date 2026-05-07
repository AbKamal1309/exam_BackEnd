package com.acoidemy.exambackend.web;

import com.acoidemy.exambackend.dtos.TestExamDTO;
import com.acoidemy.exambackend.dtos.TestRequestDTO;
import com.acoidemy.exambackend.dtos.TestResultDTO;
import com.acoidemy.exambackend.dtos.TestSendDTO;
import com.acoidemy.exambackend.exceptions.*;
import com.acoidemy.exambackend.services.ExamService;
import com.acoidemy.exambackend.services.TestService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@Slf4j
public class TestRestController {

    private TestService testService;

    @GetMapping("/test")
    public TestExamDTO getTestExam(@RequestBody TestRequestDTO testRequestDTO)
            throws UserNotFoundException, ExamNotFoundException {
        return testService.getTestExam(testRequestDTO);
    }

    @PostMapping("/test")
    public TestResultDTO sendingTest(@RequestBody TestSendDTO testSendDTO)
            throws UserNotFoundException, QuestionNotFoundException, ExamNotFoundException, AnswerNotFoundException, TestNotFoundException {
        return testService.sendTest(testSendDTO);

    }


}
