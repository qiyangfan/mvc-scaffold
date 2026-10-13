package com.example.app.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller 层集成测试：H2（MySQL 模式）+ 本地防腐层实现，完整走通 HTTP -&gt; Service -&gt; Manager -&gt; Mapper。
 */
@SpringBootTest
@AutoConfigureMockMvc
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTable() {
        jdbcTemplate.execute("DELETE FROM t_employee");
    }

    @Test
    void fullLifecycleShouldWork() throws Exception {
        // 1. 创建
        MvcResult createResult = mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Zhang San","deptId":1,"salary":20000.00,"status":0,"hireDate":"2026-09-01"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andReturn();
        String id = JsonPath.read(createResult.getResponse().getContentAsString(), "$.data");

        // 2. 详情：部门名来自防腐层，状态描述来自枚举
        mockMvc.perform(get("/api/v1/employees/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.name").value("Zhang San"))
                .andExpect(jsonPath("$.data.deptName").value("平台研发部"))
                .andExpect(jsonPath("$.data.statusDesc").value("试用期"))
                .andExpect(jsonPath("$.data.empNo").value("EMP000001"));

        // 3. 更新
        mockMvc.perform(put("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + id + ",\"salary\":25000.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        // 4. 分页
        mockMvc.perform(get("/api/v1/employees").param("pageNum", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].deptName").value("平台研发部"));

        // 5. 逻辑删除后再查，应返回员工不存在
        mockMvc.perform(delete("/api/v1/employees/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
        mockMvc.perform(get("/api/v1/employees/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("B0101"));
    }

    @Test
    void createShouldRejectDisabledDept() throws Exception {
        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Li Si","deptId":3,"salary":10000.00,"status":0,"hireDate":"2026-09-01"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("B0103"))
                .andExpect(jsonPath("$.message").value("department disabled"));
    }

    @Test
    void createShouldRejectInvalidParam() throws Exception {
        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","deptId":1,"salary":-1,"status":0,"hireDate":"2026-09-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
    }

    @Test
    void getShouldRejectNonPositiveId() throws Exception {
        mockMvc.perform(get("/api/v1/employees/{id}", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
    }

    @Test
    void listShouldReturnEmptyPageWhenNoData() throws Exception {
        // Long 统一序列化为字符串，total 亦为字符串 "0"
        mockMvc.perform(get("/api/v1/employees")
                        .param("pageNum", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.total").value("0"))
                .andExpect(jsonPath("$.data.list").isEmpty());
    }
}
