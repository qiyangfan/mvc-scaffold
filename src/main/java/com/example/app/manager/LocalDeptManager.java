package com.example.app.manager;

import com.example.app.common.exception.BizException;
import com.example.app.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 本地部门 Manager：读取配置文件中的模拟数据，默认实现（dept.provider=local 或未配置时生效）。
 * <p>替换为真实调用时切换 dept.provider=remote，业务代码零改动——这正是防腐层的价值。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "dept", name = "provider", havingValue = "local", matchIfMissing = true)
@RequiredArgsConstructor
public class LocalDeptManager implements DeptManager {

    private final DeptLocalProperties properties;

    @Override
    public DeptInfo getDeptInfo(Long deptId) {
        DeptLocalProperties.LocalDept localDept = properties.getDepts().stream()
                .filter(dept -> Objects.equals(dept.getId(), deptId))
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.DEPT_NOT_FOUND));
        if (!Boolean.TRUE.equals(localDept.getEnabled())) {
            throw new BizException(ErrorCode.DEPT_DISABLED);
        }
        return new DeptInfo(localDept.getId(), localDept.getName(), true);
    }

    @Override
    public List<DeptInfo> listDeptInfos(Collection<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return List.of();
        }
        Map<Long, DeptInfo> deptInfoMap = properties.getDepts().stream()
                .collect(Collectors.toMap(DeptLocalProperties.LocalDept::getId, this::toDeptInfo));
        List<DeptInfo> result = new ArrayList<>(deptIds.size());
        for (Long deptId : deptIds) {
            DeptInfo deptInfo = deptInfoMap.get(deptId);
            if (deptInfo != null) {
                result.add(deptInfo);
            }
        }
        return result;
    }

    private DeptInfo toDeptInfo(DeptLocalProperties.LocalDept localDept) {
        return new DeptInfo(localDept.getId(), localDept.getName(), Boolean.TRUE.equals(localDept.getEnabled()));
    }
}
