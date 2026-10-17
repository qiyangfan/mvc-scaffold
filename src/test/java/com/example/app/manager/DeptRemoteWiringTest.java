package com.example.app.manager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 远程防腐层装配冒烟测试。
 *
 * <p>为什么需要它：{@link DeptRemoteConfig} 与 {@link RemoteDeptManager} 都挂在
 * {@code dept.provider=remote} 条件上，默认 profile 是 local，所以常规集成测试
 * <b>根本不会走到这条装配路径</b>——配置类写错（条件不匹配、bean 缺失、注入不了）
 * 只会在切到 remote 模式那天才炸。这里用一次真实上下文启动把这条路堵住。</p>
 *
 * <p>base-url 指向一个不会连接的地址：启动期只建 bean、不发请求，因此测试是确定性的。</p>
 */
@SpringBootTest(properties = {
        "dept.provider=remote",
        "dept.remote.base-url=http://dept.invalid"
})
class DeptRemoteWiringTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void remoteModeShouldWireRestClientAndRemoteManagerOnly() {
        // 客户端真的被装配出来了（带超时的那一个），且能被注入
        assertThat(context.getBeansOfType(RestClient.class)).containsOnlyKeys("deptRestClient");
        assertThat(context.getBean(DeptManager.class)).isInstanceOf(RemoteDeptManager.class);
        // 条件互斥：remote 模式下 local 实现必须退场，否则两个 DeptManager 会争抢注入
        assertThat(context.getBeansOfType(DeptManager.class)).hasSize(1);
    }
}
