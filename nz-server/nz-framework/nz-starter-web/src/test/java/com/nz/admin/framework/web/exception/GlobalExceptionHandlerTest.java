package com.nz.admin.framework.web.exception;

import com.nz.admin.common.core.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private ApiExceptionLogRecorder apiExceptionLogRecorder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ReflectionTestUtils.setField(handler, "apiExceptionLogRecorder", apiExceptionLogRecorder);
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(handler)
                .build();
    }

    @Test
    void shouldReturnBusinessErrorMessage() throws Exception {
        mockMvc.perform(get("/test/business").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.msg").value("业务失败"));
    }

    @Test
    void shouldReturnBadRequestForIllegalArgument() throws Exception {
        mockMvc.perform(get("/test/illegal").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("bad request"));
    }

    @Test
    void shouldReturnBadRequestWhenRequiredParamMissing() throws Exception {
        mockMvc.perform(get("/test/required").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("id 不能为空"));
    }

    @Test
    void shouldHideUnknownExceptionMessageAndRecordIt() throws Exception {
        mockMvc.perform(get("/test/unknown").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("系统繁忙，请稍后再试"));

        verify(apiExceptionLogRecorder, times(1)).record(any(RuntimeException.class), any());
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/business")
        public void business() {
            throw new BusinessException(409, "业务失败");
        }

        @GetMapping("/illegal")
        public void illegal() {
            throw new IllegalArgumentException("bad request");
        }

        @GetMapping("/unknown")
        public void unknown() {
            throw new RuntimeException("db down");
        }

        @GetMapping("/required")
        public void required(@RequestParam String id) {
        }
    }
}
