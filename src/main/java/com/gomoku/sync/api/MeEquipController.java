package com.gomoku.sync.api;

import com.gomoku.sync.api.dto.ApiError;
import com.gomoku.sync.api.dto.EquipRequest;
import com.gomoku.sync.service.CosmeticEquipService;
import com.gomoku.sync.service.CosmeticEquipService.EquipResult;
import com.gomoku.sync.service.SessionJwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * 杂货铺装备：按 {@link com.gomoku.sync.domain.CosmeticCategory} 写入唯一槽位。
 */
@RestController
@RequestMapping("/api/me")
public class MeEquipController {

    private final CosmeticEquipService cosmeticEquipService;
    private final SessionJwtService sessionJwtService;

    public MeEquipController(CosmeticEquipService cosmeticEquipService, SessionJwtService sessionJwtService) {
        this.cosmeticEquipService = cosmeticEquipService;
        this.sessionJwtService = sessionJwtService;
    }

    @PostMapping("/equip")
    public ResponseEntity<?> equip(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) EquipRequest body) {
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (!uid.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "请先登录"));
        }
        if (body == null || body.getCategory() == null || body.getItemId() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiError("BAD_REQUEST", "缺少 category 或 itemId"));
        }
        EquipResult result = cosmeticEquipService.equip(uid.get(), body.getCategory(), body.getItemId());
        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getBody());
        }
        switch (result.getError()) {
            case BAD_REQUEST:
            case PIECE_SKIN_BAD_REQUEST:
                return ResponseEntity.badRequest()
                        .body(new ApiError("BAD_REQUEST", "请求无效"));
            case UNKNOWN_CATEGORY:
                return ResponseEntity.badRequest()
                        .body(new ApiError("UNKNOWN_CATEGORY", "未知装备种类"));
            case INVALID_ITEM:
            case PIECE_SKIN_INVALID:
                return ResponseEntity.badRequest()
                        .body(new ApiError("INVALID_ITEM", "不可装备该项"));
            case NOT_UNLOCKED:
            case PIECE_SKIN_NOT_UNLOCKED:
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiError("NOT_UNLOCKED", "尚未解锁"));
            case NOT_FOUND:
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError("NOT_FOUND", "用户不存在"));
            default:
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiError("SERVER_ERROR", "保存失败"));
        }
    }
}
