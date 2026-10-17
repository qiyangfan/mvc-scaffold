package com.example.app.contract;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API 契约测试：OpenAPI schema 是前后端契约的<b>单一事实源</b>（见 ADR-0009）。
 * <p>两件事在一次测试里完成：</p>
 * <ol>
 *   <li>常态校验：文档端点必须产出合法 JSON 且包含主链路端点（防止注解被误删导致契约残缺）；</li>
 *   <li>按需导出：{@code -Dcontract.export=true} 时把最新 schema 写入 {@code contracts/openapi.json}，
 *       CI 紧接着执行 {@code git diff --exit-code contracts/}——"改了接口忘了导出"直接让流水线红。</li>
 * </ol>
 * <p>前端类型由 {@code contracts/openapi.json} 生成，手写 API 类型视为违规。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContractTest {

    private static final String API_DOCS_PATH = "/v3/api-docs";
    private static final Path CONTRACT_FILE = Paths.get("contracts", "openapi.json");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Value("${contract.export:false}")
    private boolean exportEnabled;

    @Test
    void apiDocsShouldBeValidAndExportable() throws Exception {
        String content = mockMvc.perform(get(API_DOCS_PATH))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        JsonNode document = jsonMapper.readTree(content);
        JsonNode paths = document.get("paths");
        assertThat(paths).isNotNull();
        assertThat(paths.isObject()).isTrue();
        // 主链路端点必须出现在契约里：缺一个都说明注解或 Controller 被破坏了
        assertThat(paths.has("/api/v1/employees")).isTrue();
        assertThat(paths.has("/api/v1/employees/{id}")).isTrue();
        // 组件模型必须存在：前端生成的类型依赖 schemas 段
        assertThat(document.get("components")).isNotNull();

        if (exportEnabled) {
            export(document);
        }
    }

    /** 导出统一格式化后的契约：入库文件必须稳定可 diff，禁止逐次格式抖动 */
    private void export(JsonNode document) throws Exception {
        Path parent = CONTRACT_FILE.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String pretty = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(document);
        Files.writeString(CONTRACT_FILE, pretty + System.lineSeparator(), StandardCharsets.UTF_8);
    }
}
