package com.xinsec.devicesimulator.service.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xinsec.devicesimulator.service.entity.RequestResponseRuleEntity;
import com.xinsec.devicesimulator.service.mapper.RequestResponseRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
@Slf4j
public class RequestResponseRuleController {

    private final RequestResponseRuleMapper ruleMapper;

    @GetMapping
    public List<RequestResponseRuleEntity> getAllRules() {
        return ruleMapper.selectList(null);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RequestResponseRuleEntity> getRuleById(@PathVariable Long id) {
        RequestResponseRuleEntity rule = ruleMapper.selectById(id);
        return rule != null ? ResponseEntity.ok(rule) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<RequestResponseRuleEntity> createRule(@RequestBody RequestResponseRuleEntity rule) {
        // 检查同一规则分组下是否存在具有相同请求键的规则
        QueryWrapper<RequestResponseRuleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(RequestResponseRuleEntity::getRuleGroup, rule.getRuleGroup())
                .eq(RequestResponseRuleEntity::getRequestKey, rule.getRequestKey());
        if (ruleMapper.selectCount(queryWrapper) > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409 冲突
        }
        rule.setCreateTime(LocalDateTime.now());
        rule.setUpdateTime(LocalDateTime.now());
        ruleMapper.insert(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(rule);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RequestResponseRuleEntity> updateRule(@PathVariable Long id, @RequestBody RequestResponseRuleEntity rule) {
        RequestResponseRuleEntity existingRule = ruleMapper.selectById(id);

        if (existingRule == null) {
            return ResponseEntity.notFound().build();
        }

        // 如果规则分组或请求键被更改，则检查是否存在冲突
        if (!existingRule.getRuleGroup().equals(rule.getRuleGroup()) || !existingRule.getRequestKey().equals(rule.getRequestKey())) {
            QueryWrapper<RequestResponseRuleEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda()
                    .eq(RequestResponseRuleEntity::getRuleGroup, rule.getRuleGroup())
                    .eq(RequestResponseRuleEntity::getRequestKey, rule.getRequestKey())
                    .ne(RequestResponseRuleEntity::getId, id); // 从冲突检查中排除当前规则
            if (ruleMapper.selectCount(queryWrapper) > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409 冲突
            }
        }

        rule.setId(id);
        rule.setUpdateTime(LocalDateTime.now());
        ruleMapper.updateById(rule);
        return ResponseEntity.ok(rule);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        int deletedRows = ruleMapper.deleteById(id);
        return deletedRows > 0 ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
