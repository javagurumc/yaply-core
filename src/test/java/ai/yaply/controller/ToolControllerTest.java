package ai.yaply.controller;

import ai.yaply.dto.ToolExecuteResponse;
import ai.yaply.dto.ToolName;
import ai.yaply.service.ToolExecutionService;
import ai.yaply.testsupport.ClarityWebMvcTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ClarityWebMvcTest(ToolController.class)
class ToolControllerTest {

    //Test

    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private ToolExecutionService toolExecutionService;

    @Test
    @WithMockUser
    void givenValidToolExecuteRequest_whenExecute_thenReturnsToolExecuteResponse() {
        var conversationId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        when(toolExecutionService.execute(any())).thenReturn(new ToolExecuteResponse(
                ToolName.KB_SEARCH,
                Map.of("ok", true)
        ));

        restTestClient.post()
                .uri("/api/tools/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "conversationId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                          "toolName": "kb_search",
                          "arguments": {
                            "query": "hello"
                          }
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.toolName").isEqualTo("kb_search")
                .jsonPath("$.result.ok").isEqualTo(true);

        verify(toolExecutionService).execute(argThat(req ->
                conversationId.equals(req.conversationId())
                        && req.toolName() == ToolName.KB_SEARCH
                        && "hello".equals(req.arguments().get("query"))
        ));
    }
}

