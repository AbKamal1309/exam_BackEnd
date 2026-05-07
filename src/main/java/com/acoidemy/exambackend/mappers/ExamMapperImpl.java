package com.acoidemy.exambackend.mappers;

import com.acoidemy.exambackend.dtos.AnswerDTO;
import com.acoidemy.exambackend.dtos.ExamDTO;
import com.acoidemy.exambackend.dtos.QuestionDTO;
import com.acoidemy.exambackend.dtos.UserDTO;
import com.acoidemy.exambackend.entities.Answer;
import com.acoidemy.exambackend.entities.AppUser;
import com.acoidemy.exambackend.entities.Exam;
import com.acoidemy.exambackend.entities.Question;
import com.acoidemy.exambackend.repositories.AnswerRepository;
import com.acoidemy.exambackend.services.ExamService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ExamMapperImpl {


    public UserDTO fromUser(AppUser appUser){
        UserDTO userDTO=new UserDTO();
        BeanUtils.copyProperties(appUser,userDTO);

        return userDTO;
    }

    public AppUser fromUserDTO(UserDTO userDTO){
        AppUser appUser =new AppUser();
       BeanUtils.copyProperties(userDTO, appUser);

        return appUser;
    }
    public ExamDTO fromExam(Exam exam){
        ExamDTO examDTO=new ExamDTO();
        BeanUtils.copyProperties(exam,examDTO);

        examDTO.setUserId(exam.getAppUser().getId());
        examDTO.setNumberOfQuestions(exam.getNumberOfQuestions());
       // List<QuestionDTO> questionDTOS=new ArrayList<>();
        List<Question> questions = exam.getQuestions();
        List<QuestionDTO> questionDTOS = questions.stream()
                .map(question -> this.fromQuestion(question))
                .collect(Collectors.toList());
        examDTO.setNumberOfQuestions(questionDTOS.size());

//        for (int i=0;i<exam.getNumberOfQuestions();i++){
//         //   questionDTOS.add(this.fromQuestion(exam.getQuestions().get(i)));
//            questionDTOS.add(this.fromQuestion(exam.getQuestions().get(i)));
//        }
            examDTO.setQuestionDTOList(questionDTOS);
        return examDTO;
    }
    public Exam fromExamDTO(ExamDTO examDTO){
        Exam exam=new Exam();
        BeanUtils.copyProperties(examDTO,exam);
        exam.setQuestions(fromQuestionDTOList(examDTO.getQuestionDTOList()));

        return exam;
    }
    public Exam fromExamAllQuestionsAndAnswersDTO(ExamDTO examDTO){
        Exam exam=new Exam();
        exam.setDescription(examDTO.getDescription());
        List<QuestionDTO> questionDTOList = examDTO.getQuestionDTOList();

        List<Question> questionList=new ArrayList<>();

        for (int i=0;i< questionDTOList.size();i++){
            questionList.add(this.fromQuestionDTO(questionDTOList.get(i)));
            List<Answer> answerList=new ArrayList<>();
            List<AnswerDTO> answers = questionDTOList.get(i).getAnswers();
            for (int j=0;j<4;j++){
                Answer answer=new Answer();
                answer.setAnswerContent(answers.get(j).getAnswerContent());
                answer.setAnswerStatus(answers.get(j).getAnswerStatus());
                answerList.add(answer);
            }

                questionList.get(i).setAnswers(answerList);

        }

        exam.setQuestions(questionList);

        return exam;
    }

    public QuestionDTO fromQuestion(Question question){
        QuestionDTO questionDTO=new QuestionDTO();
        //BeanUtils.copyProperties(question,questionDTO);
        questionDTO.setCodeQuestion(question.getCodeQuestion());
        questionDTO.setDescription(question.getDescription());
        questionDTO.setQuestionContent(question.getQuestionContent());
        questionDTO.setExamId(question.getExam().getCodeExam());
        List<Answer> answerList = question.getAnswers();
        List<AnswerDTO> answers=new ArrayList<>();
        for (int i=0;i<answerList.size();i++){
            AnswerDTO answerDTO=new AnswerDTO();
            answerDTO.setCodeAnswer(question.getAnswers().get(i).getCodeAnswer());
            answerDTO.setAnswerContent(question.getAnswers().get(i).getAnswerContent());
            answerDTO.setDescription(question.getAnswers().get(i).getDescription());
            answerDTO.setAnswerStatus(question.getAnswers().get(i).getAnswerStatus());
           // answerDTO.setQuestionId(question.getAnswers().get(i).getQuestion().getCodeQuestion());
            answerDTO.setQuestionId(question.getCodeQuestion());
            answers.add(answerDTO);
        }
        questionDTO.setAnswers(answers);
        return questionDTO;
    }
    public Question fromNewQuestionDTOWithoutAnswers(QuestionDTO questionDTO){
        Question question=new Question();
        question.setQuestionContent(questionDTO.getQuestionContent());
        question.setDescription(questionDTO.getDescription());

        return question;
    }
    public Question fromNewQuestionDTOAndAnswers(QuestionDTO questionDTO){
        Question question=new Question();
       question.setCodeQuestion(UUID.randomUUID().toString());
        question.setQuestionContent(questionDTO.getQuestionContent());
        question.setDescription(questionDTO.getDescription());

        //question.getExam().setCodeExam(questionDTO.getExamId());
       List<AnswerDTO> answers = questionDTO.getAnswers();
        List<Answer> answerList=new ArrayList<>();
        for (int i=0;i<answers.size();i++){
            Answer answer=new Answer();
            answer.setAnswerContent(answers.get(i).getAnswerContent());
        //    answer.setCodeAnswer(UUID.randomUUID().toString());
         //   answer.getQuestion().setCodeQuestion(question.getCodeQuestion());
            answer.setQuestion(question);
            answerList.add(answer);
        }
        question.setAnswers(answerList);
        return question;
    }
    public QuestionDTO fromNewQuestionAndAnswers(Question question){
        QuestionDTO questionDTO=new QuestionDTO();
        questionDTO.setCodeQuestion(question.getCodeQuestion());
        questionDTO.setQuestionContent(question.getQuestionContent());
        questionDTO.setDescription(question.getDescription());
        questionDTO.setExamId(question.getExam().getCodeExam());
        List<AnswerDTO> answers=new ArrayList<>();
        for (int i=0;i<question.getAnswers().size();i++){
            AnswerDTO answerDTO=new AnswerDTO();
            answerDTO.setCodeAnswer(question.getAnswers().get(i).getCodeAnswer());
            answerDTO.setAnswerContent(question.getAnswers().get(i).getAnswerContent());
            answerDTO.setDescription(question.getAnswers().get(i).getDescription());
            answerDTO.setAnswerStatus(question.getAnswers().get(i).getAnswerStatus());
            answerDTO.setQuestionId(question.getAnswers().get(i).getQuestion().getCodeQuestion());
            answers.add(answerDTO);
        }
        questionDTO.setAnswers(answers);

        return questionDTO;
    }
    public QuestionDTO fromNewQuestionWithoutAnswers(Question question){
        QuestionDTO questionDTO=new QuestionDTO();
        questionDTO.setCodeQuestion(question.getCodeQuestion());
        questionDTO.setDescription(question.getDescription());
        questionDTO.setQuestionContent(question.getQuestionContent());
        questionDTO.setExamId(question.getExam().getCodeExam());

        return questionDTO;
    }

    public Question fromQuestionDTO(QuestionDTO questionDTO){
        Question question=new Question();
        //BeanUtils.copyProperties(questionDTO,question);
        question.setCodeQuestion(questionDTO.getCodeQuestion());
        question.setQuestionContent(questionDTO.getQuestionContent());
        question.setDescription(questionDTO.getDescription());

        return question;
    }

    public List<QuestionDTO> fromQuestionList(List<Question> questionList){
        List<QuestionDTO> questionDTOList=new ArrayList<>();
        for (int i=0;i<questionList.size();i++){
            questionDTOList.add(this.fromQuestion(questionList.get(i)));
        }
        return questionDTOList;
    }
    public List<Question> fromQuestionDTOList(List<QuestionDTO> questionDTOList){
        List<Question> questionList=new ArrayList<>();
        for (int i=0;i<questionDTOList.size();i++){
            questionList.add(this.fromQuestionDTO(questionDTOList.get(i)));
        }
        return questionList;
    }


    public AnswerDTO fromAnswer(Answer answer){
        AnswerDTO answerDTO=new AnswerDTO();
        //BeanUtils.copyProperties(answer,answerDTO);
        answerDTO.setCodeAnswer(answer.getCodeAnswer());
        answerDTO.setAnswerContent(answer.getAnswerContent());
        answerDTO.setDescription(answer.getDescription());
        answerDTO.setAnswerStatus(answer.getAnswerStatus());
        answerDTO.setQuestionId(answer.getQuestion().getCodeQuestion());
        return answerDTO;
    }

    public Answer fromAnswerDTO(AnswerDTO answerDTO){
        Answer answer=new Answer();
       // BeanUtils.copyProperties(answerDTO,answer);
        answer.setCodeAnswer(answerDTO.getCodeAnswer());
        answer.setAnswerContent(answerDTO.getAnswerContent());
        answer.setDescription(answerDTO.getDescription());
        answer.setAnswerStatus(answerDTO.getAnswerStatus());


        return answer;
    }

    public ExamDTO fromExamAllQuestionsAndAnswers(Exam exam) {
        ExamDTO examDTO=new ExamDTO();
        List<QuestionDTO> questionDTOS=new ArrayList<>();
        examDTO.setCodeExam(exam.getCodeExam());
        examDTO.setDateCreation(exam.getDateCreation());
        examDTO.setUserId(exam.getAppUser().getId());
        examDTO.setDescription(exam.getDescription());
        examDTO.setNumberOfQuestions(exam.getQuestions().size());
        for (int i=0;i<exam.getQuestions().size();i++){
            QuestionDTO questionDTO=new QuestionDTO();
            List<Answer> answerList = exam.getQuestions().get(i).getAnswers();
            List<AnswerDTO> answers=new ArrayList<>();
            questionDTO.setQuestionContent(exam.getQuestions().get(i).getQuestionContent());
            for (int j=0;j<4;j++){
                answers.add(this.fromAnswer(answerList.get(j)));
            }
            questionDTO.setAnswers(answers);
            questionDTOS.add(questionDTO);
        }
        examDTO.setQuestionDTOList(questionDTOS);

        return examDTO;
    }
}
