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
        // Check for existing rule with same requestKey within the same ruleGroup
        QueryWrapper<RequestResponseRuleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("rule_group", rule.getRuleGroup());
        queryWrapper.eq("request_key", rule.getRequestKey());
        if (ruleMapper.selectCount(queryWrapper) > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409 Conflict
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

        // Check for conflict if ruleGroup or requestKey is changed
        if (!existingRule.getRuleGroup().equals(rule.getRuleGroup()) || !existingRule.getRequestKey().equals(rule.getRequestKey())) {
            QueryWrapper<RequestResponseRuleEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("rule_group", rule.getRuleGroup());
            queryWrapper.eq("request_key", rule.getRequestKey());
            queryWrapper.ne("id", id); // Exclude current rule from conflict check
            if (ruleMapper.selectCount(queryWrapper) > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409 Conflict
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
