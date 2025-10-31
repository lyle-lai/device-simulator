package com.xinsec.devicesimulator.service.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xinsec.devicesimulator.service.entity.PayloadRepositoryEntity;
import com.xinsec.devicesimulator.service.mapper.PayloadRepositoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 报文库管理控制器，提供报文的CRUD操作。
 */
@RestController
@RequestMapping("/api/payloads")
@RequiredArgsConstructor
@Slf4j
public class PayloadController {

    private final PayloadRepositoryMapper payloadRepositoryMapper;

    /**
     * 获取所有报文。
     * @return 所有报文实体列表。
     */
    @GetMapping
    public List<PayloadRepositoryEntity> getAllPayloads() {
//        log.info("请求获取所有报文.");
        return payloadRepositoryMapper.selectList(null);
    }

    /**
     * 根据报文键获取单个报文。
     * @param payloadKey 报文的唯一键。
     * @return 对应的报文实体，如果不存在则返回404。
     */
    @GetMapping("/{payloadKey}")
    public ResponseEntity<PayloadRepositoryEntity> getPayloadByKey(@PathVariable String payloadKey) {
        log.info("请求获取报文, 报文键: {}.", payloadKey);
        QueryWrapper<PayloadRepositoryEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PayloadRepositoryEntity::getPayloadKey, payloadKey);
        PayloadRepositoryEntity payload = payloadRepositoryMapper.selectOne(queryWrapper);
        if (payload != null) {
            log.debug("报文 {} 获取成功.", payloadKey);
            return ResponseEntity.ok(payload);
        } else {
            log.warn("报文 {} 未找到.", payloadKey);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 创建新报文。
     * @param payload 待创建的报文实体。
     * @return 创建成功的报文实体，如果报文键已存在则返回409冲突。
     */
    @PostMapping
    public ResponseEntity<PayloadRepositoryEntity> createPayload(@RequestBody PayloadRepositoryEntity payload) {
        log.info("请求创建新报文, 报文键: {}.", payload.getPayloadKey());
        QueryWrapper<PayloadRepositoryEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PayloadRepositoryEntity::getPayloadKey, payload.getPayloadKey());
        if (payloadRepositoryMapper.selectCount(queryWrapper) > 0) {
            log.warn("报文键 {} 已存在, 创建失败.", payload.getPayloadKey());
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409 Conflict
        }
        payload.setCreateTime(LocalDateTime.now());
        payload.setUpdateTime(LocalDateTime.now());
        payloadRepositoryMapper.insert(payload);
        log.info("报文 {} 创建成功.", payload.getPayloadKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(payload);
    }

    /**
     * 更新现有报文。
     * @param payloadKey 路径中的报文键。
     * @param payload 待更新的报文实体。
     * @return 更新后的报文实体，如果报文不存在则返回404，如果新报文键冲突则返回409。
     */
    @PutMapping("/{payloadKey}")
    public ResponseEntity<PayloadRepositoryEntity> updatePayload(@PathVariable String payloadKey, @RequestBody PayloadRepositoryEntity payload) {
        log.info("请求更新报文, 路径报文键: {}.", payloadKey);
        QueryWrapper<PayloadRepositoryEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PayloadRepositoryEntity::getPayloadKey, payloadKey);
        PayloadRepositoryEntity existingPayload = payloadRepositoryMapper.selectOne(queryWrapper);

        if (existingPayload == null) {
            log.warn("待更新报文 {} 未找到.", payloadKey);
            return ResponseEntity.notFound().build();
        }

        // 如果报文键发生变化，检查新键是否冲突
        if (!payloadKey.equals(payload.getPayloadKey())) {
            log.info("报文键从 {} 变更为 {}. 检查新键是否冲突.", payloadKey, payload.getPayloadKey());
            QueryWrapper<PayloadRepositoryEntity> newKeyQueryWrapper = new QueryWrapper<>();
            newKeyQueryWrapper.lambda().eq(PayloadRepositoryEntity::getPayloadKey, payload.getPayloadKey());
            if (payloadRepositoryMapper.selectCount(newKeyQueryWrapper) > 0) {
                log.warn("新报文键 {} 已存在, 更新失败.", payload.getPayloadKey());
                return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409 Conflict with new key
            }
        }

        payload.setId(existingPayload.getId()); // 确保ID被保留用于更新
        payload.setUpdateTime(LocalDateTime.now());
        payloadRepositoryMapper.updateById(payload);
        log.info("报文 {} 更新成功.", payload.getPayloadKey());
        return ResponseEntity.ok(payload);
    }

    /**
     * 删除报文。
     * @param payloadKey 待删除报文的键。
     * @return 如果删除成功返回204，否则返回404。
     */
    @DeleteMapping("/{payloadKey}")
    public ResponseEntity<Void> deletePayload(@PathVariable String payloadKey) {
        log.info("请求删除报文, 报文键: {}.", payloadKey);
        QueryWrapper<PayloadRepositoryEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PayloadRepositoryEntity::getPayloadKey, payloadKey);
        int deletedRows = payloadRepositoryMapper.delete(queryWrapper);
        if (deletedRows > 0) {
            log.info("报文 {} 删除成功.", payloadKey);
            return ResponseEntity.noContent().build();
        } else {
            log.warn("报文 {} 未找到, 删除失败.", payloadKey);
            return ResponseEntity.notFound().build();
        }
    }
}
