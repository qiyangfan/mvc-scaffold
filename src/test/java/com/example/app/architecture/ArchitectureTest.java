package com.example.app.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * 架构守护测试：四层增强 MVC（Controller -> Service -> Manager -> Mapper）依赖方向强制执行。
 * <p>执法射程为生产代码：测试代码允许合法跨层装配与读取墙钟。</p>
 */
@AnalyzeClasses(packages = "com.example.app", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /**
     * 分层依赖方向：Controller -> Service -> Manager -> Mapper
     * <p>禁止跨层访问：Controller 严禁绕过 Service 直连 Manager 或 Mapper；
     * 任何层均不得反向调用上层。</p>
     */
    @ArchTest
    static final ArchRule layerDependencyRule = layeredArchitecture().consideringAllDependencies()
            .layer("Controller").definedBy("..app.controller..")
            .layer("Service").definedBy("..app.service..")
            .layer("Manager").definedBy("..app.manager..")
            .layer("Mapper").definedBy("..app.mapper..")
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Manager").mayOnlyBeAccessedByLayers("Service")
            .whereLayer("Mapper").mayOnlyBeAccessedByLayers("Service", "Manager");

    /**
     * 黄山版模型规约：DO（Data Object）映射表结构，严禁穿透 Service 层暴露给 Controller。
     */
    @ArchTest
    static final ArchRule doShouldNotLeakToController = noClasses()
            .that().resideInAPackage("..app.controller..")
            .should().dependOnClassesThat().resideInAPackage("..app.model.dataobject..");

    /**
     * 包间无环依赖守护
     */
    @ArchTest
    static final ArchRule packagesShouldBeFreeOfCycles =
            SlicesRuleDefinition.slices().matching("com.example.app.(*)..").should().beFreeOfCycles();

    /**
     * 业务时间必须取注入 Clock：Service 层（业务规则所在层）禁止直呼墙钟，
     * 保证时间语义可确定性测试。身份生成（common/support/Ids）不在射程内。
     */
    @ArchTest
    static final ArchRule businessTimeMustComeFromInjectedClock = noClasses()
            .that().resideInAPackage("..app.service..")
            .should().callMethod(LocalDateTime.class, "now")
            .orShould().callMethod(LocalDate.class, "now")
            .orShould().callMethod(Instant.class, "now")
            .orShould().callMethod(System.class, "currentTimeMillis")
            .because("业务时间必须取注入 Clock，禁止直呼墙钟");
}
