package com.opencirc.api.passport.controller.test;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencirc.api.passport.controller.PassportLogController;
import com.opencirc.api.passport.dto.PassportLogDto;
import com.opencirc.api.passport.service.PassportLogService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
public class TestPassportLogController {

  private MockMvc mockMvc;

  @Mock private PassportLogService passportLogService;

  private ObjectMapper objectMapper = new ObjectMapper();

  @InjectMocks private PassportLogController passportLogController;

  @BeforeEach
  public void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(passportLogController).build();
  }

  @Test
  public void shouldGetLogsForPassport() throws Exception {
    String passportId = "test-passport-id";
    PassportLogDto log =
        new PassportLogDto(
            "log-id",
            passportId,
            objectMapper.createObjectNode().put("action", "CREATE"),
            "actor-id",
            null,
            null);

    when(passportLogService.getLogsByPassportId(passportId)).thenReturn(List.of(log));

    mockMvc
        .perform(get("/api/passport/{passportId}/logs", passportId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].passportId").value(passportId))
        .andExpect(jsonPath("$[0].data.action").value("CREATE"));
  }
}
