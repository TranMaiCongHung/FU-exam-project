package com.se196693.mvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.se196693.mvc.dto.request.AnswerOptionRequest;
import com.se196693.mvc.dto.request.CreateQuestionRequest;
import com.se196693.mvc.dto.request.QuestionRequest;
import com.se196693.mvc.dto.request.UpdateQuestionRequest;
import com.se196693.mvc.dto.response.QuestionResponse;
import com.se196693.mvc.entity.Exam;
import com.se196693.mvc.entity.Question;
import com.se196693.mvc.enums.QuestionType;
import com.se196693.mvc.exception.DuplicateResourceException;
import com.se196693.mvc.exception.ResourceNotFoundException;
import com.se196693.mvc.repository.ExamRepository;
import com.se196693.mvc.repository.QuestionRepository;
import com.se196693.mvc.service.FileStorageService;

@ExtendWith(MockitoExtension.class)
class QuestionServiceImplTest {

    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private ExamRepository examRepository;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private QuestionServiceImpl service;

    private Exam exam;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "publicUrl", "https://cdn.test");
        exam = new Exam();
        exam.setId(1L);
    }

    private static AnswerOptionRequest opt(String label, Boolean correct) {
        return new AnswerOptionRequest(label, "content " + label, correct);
    }

    private static CreateQuestionRequest req(QuestionType type, Integer number, String content,
            AnswerOptionRequest... options) {
        CreateQuestionRequest r = new CreateQuestionRequest();
        r.setQuestionType(type);
        r.setQuestionNumber(number);
        r.setContent(content);
        r.setAnswerOption(List.of(options));
        return r;
    }

    private static MockMultipartFile png() {
        return new MockMultipartFile("image", "q.png", "image/png", new byte[]{1, 2, 3});
    }

    private void stubExam() {
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
    }

    private void stubSave() {
        when(questionRepository.saveAndFlush(any(Question.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void addQuestion_withImage_uploadsAndSaves() {
        stubExam();
        stubSave();
        when(questionRepository.existsByExamIdAndQuestionNumber(1L, 1)).thenReturn(false);

        QuestionResponse res = service.addQuestion(1L,
                req(QuestionType.SINGLE_CHOICE, 1, " hello ", opt("A", true), opt("B", false)), png());

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(fileStorageService).upload(any(), key.capture());
        ArgumentCaptor<Question> q = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).saveAndFlush(q.capture());
        assertEquals(key.getValue(), q.getValue().getObjectKey());
        assertTrue(key.getValue().startsWith("exams/1/questions/1-"));
        assertEquals("hello", q.getValue().getContent());
        assertTrue(q.getValue().getAnswerOptions().get(0).isCorrect());
        assertEquals(false, q.getValue().getAnswerOptions().get(1).isCorrect());
        assertNotNull(res.getImageUrl());
    }

    @Test
    void addQuestion_withoutImage_noUpload() {
        stubExam();
        stubSave();
        QuestionResponse res = service.addQuestion(1L,
                req(QuestionType.SINGLE_CHOICE, 1, "text", opt("A", true), opt("B", false)), null);
        verify(fileStorageService, never()).upload(any(), anyString());
        assertNull(res.getImageUrl());
    }

    @Test
    void addQuestion_noContentNoImage_throws() {
        stubExam();
        assertThrows(IllegalArgumentException.class, () -> service.addQuestion(1L,
                req(QuestionType.SINGLE_CHOICE, 1, "  ", opt("A", true), opt("B", false)), null));
    }

    @Test
    void addQuestion_singleChoiceZeroCorrect_throws() {
        stubExam();
        assertThrows(IllegalArgumentException.class, () -> service.addQuestion(1L,
                req(QuestionType.SINGLE_CHOICE, 1, "t", opt("A", false), opt("B", false)), null));
    }

    @Test
    void addQuestion_singleChoiceTwoCorrect_throws() {
        stubExam();
        assertThrows(IllegalArgumentException.class, () -> service.addQuestion(1L,
                req(QuestionType.SINGLE_CHOICE, 1, "t", opt("A", true), opt("B", true)), null));
    }

    @Test
    void addQuestion_multipleChoiceZeroCorrect_throws() {
        stubExam();
        assertThrows(IllegalArgumentException.class, () -> service.addQuestion(1L,
                req(QuestionType.MULTIPLE_CHOICE, 1, "t", opt("A", false), opt("B", null)), null));
    }

    @Test
    void addQuestion_fewerThanTwoOptions_throws() {
        stubExam();
        assertThrows(IllegalArgumentException.class, () -> service.addQuestion(1L,
                req(QuestionType.SINGLE_CHOICE, 1, "t", opt("A", true)), null));
    }

    @Test
    void addQuestion_duplicateLabels_throws() {
        stubExam();
        assertThrows(IllegalArgumentException.class, () -> service.addQuestion(1L,
                req(QuestionType.SINGLE_CHOICE, 1, "t", opt("A", true), opt("a", false)), null));
    }

    @Test
    void addQuestion_nullCorrectTreatedAsFalse() {
        stubExam();
        stubSave();
        service.addQuestion(1L,
                req(QuestionType.SINGLE_CHOICE, 1, "t", opt("A", true), opt("B", null)), null);
        ArgumentCaptor<Question> q = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).saveAndFlush(q.capture());
        assertEquals(false, q.getValue().getAnswerOptions().get(1).isCorrect());
    }

    @Test
    void addQuestion_invalidImageType_throwsNoUpload() {
        stubExam();
        MockMultipartFile gif = new MockMultipartFile("image", "q.gif", "image/gif", new byte[]{1});
        assertThrows(IllegalArgumentException.class, () -> service.addQuestion(1L,
                req(QuestionType.SINGLE_CHOICE, 1, "t", opt("A", true), opt("B", false)), gif));
        verify(fileStorageService, never()).upload(any(), anyString());
    }

    @Test
    void addQuestion_duplicateNumber_throwsNoUpload() {
        stubExam();
        when(questionRepository.existsByExamIdAndQuestionNumber(1L, 2)).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> service.addQuestion(1L,
                req(QuestionType.SINGLE_CHOICE, 2, "t", opt("A", true), opt("B", false)), png()));
        verify(fileStorageService, never()).upload(any(), anyString());
    }

    @Test
    void addQuestion_nullNumber_assignsMaxPlusOne() {
        stubExam();
        stubSave();
        when(questionRepository.findMaxQuestionNumberByExamId(1L)).thenReturn(7);
        QuestionResponse res = service.addQuestion(1L,
                req(QuestionType.SINGLE_CHOICE, null, "t", opt("A", true), opt("B", false)), null);
        assertEquals(8, res.getQuestionNumber());
    }

    @Test
    void addQuestion_saveFails_deletesUploadedImageAndRethrows() {
        stubExam();
        when(questionRepository.existsByExamIdAndQuestionNumber(1L, 1)).thenReturn(false);
        when(questionRepository.saveAndFlush(any(Question.class))).thenThrow(new RuntimeException("db"));

        assertThrows(RuntimeException.class, () -> service.addQuestion(1L,
                req(QuestionType.SINGLE_CHOICE, 1, "t", opt("A", true), opt("B", false)), png()));

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(fileStorageService).upload(any(), key.capture());
        verify(fileStorageService).delete(key.getValue());
    }

    @Test
    void addQuestion_examNotFound_throws() {
        when(examRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.addQuestion(1L,
                req(QuestionType.SINGLE_CHOICE, 1, "t", opt("A", true), opt("B", false)), null));
    }

    private static QuestionRequest ocr(Integer number) {
        QuestionRequest r = new QuestionRequest();
        r.setQuestionNumber(number);
        r.setContent("c" + number);
        r.setQuestionType(QuestionType.SINGLE_CHOICE);
        r.setAnswerOption(List.of(opt("A", true), opt("B", false)));
        return r;
    }

    @Test
    void saveOcrQuestions_appendsWithoutDeleting() {
        stubExam();
        when(questionRepository.findExistingQuestionNumbers(any(), any())).thenReturn(List.of());

        service.saveOcrQuestions(1L, List.of(ocr(1), ocr(2)));

        verify(questionRepository, never()).deleteAll(any());
        verify(questionRepository, never()).deleteAll();
        verify(questionRepository, org.mockito.Mockito.times(2)).save(any(Question.class));
    }

    private List<Integer> savedNumbers(int times) {
        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository, org.mockito.Mockito.times(times)).save(captor.capture());
        return captor.getAllValues().stream().map(Question::getQuestionNumber).toList();
    }

    @Test
    void saveOcrQuestions_numberAlreadyInDb_renumbersAfterMax() {
        stubExam();
        when(questionRepository.findExistingQuestionNumbers(any(), any())).thenReturn(List.of(3));
        when(questionRepository.findMaxQuestionNumberByExamId(1L)).thenReturn(10);

        service.saveOcrQuestions(1L, List.of(ocr(3), ocr(4)));

        assertEquals(List.of(11, 4), savedNumbers(2));
    }

    @Test
    void saveOcrQuestions_duplicatesInRequest_firstKeptRestRenumbered() {
        stubExam();
        when(questionRepository.findExistingQuestionNumbers(any(), any())).thenReturn(List.of());
        when(questionRepository.findMaxQuestionNumberByExamId(1L)).thenReturn(0);

        // Trường hợp OCR từng ảnh đều trả về "1"
        service.saveOcrQuestions(1L, List.of(ocr(1), ocr(1), ocr(1)));

        assertEquals(List.of(1, 2, 3), savedNumbers(3));
    }

    @Test
    void saveOcrQuestions_nullOrInvalidNumbers_assignedAfterKeptOnes() {
        stubExam();
        when(questionRepository.findExistingQuestionNumbers(any(), any())).thenReturn(List.of());
        when(questionRepository.findMaxQuestionNumberByExamId(1L)).thenReturn(2);

        service.saveOcrQuestions(1L, List.of(ocr(null), ocr(7), ocr(0)));

        assertEquals(List.of(8, 7, 9), savedNumbers(3));
    }

    @Test
    void saveOcrQuestions_emptyList_throws() {
        stubExam();
        assertThrows(IllegalArgumentException.class, () -> service.saveOcrQuestions(1L, List.of()));
    }

    @Test
    void updateQuestion_invalidOptions_throws() {
        when(examRepository.existsById(1L)).thenReturn(true);
        Question existing = Question.builder().id(9L).questionNumber(1).exam(exam)
                .questionType(QuestionType.SINGLE_CHOICE).build();
        when(questionRepository.findById(9L)).thenReturn(Optional.of(existing));

        UpdateQuestionRequest r = new UpdateQuestionRequest();
        r.setQuestionNumber(1);
        r.setContent("c");
        r.setQuestionType(QuestionType.SINGLE_CHOICE);
        r.setAnswerOption(List.of(opt("A", false), opt("B", false)));

        assertThrows(IllegalArgumentException.class, () -> service.updateQuestion(1L, 9L, r));
        verify(questionRepository, never()).save(any(Question.class));
    }
}
