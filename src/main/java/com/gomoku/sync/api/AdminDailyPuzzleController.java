package com.gomoku.sync.api;

import com.gomoku.sync.api.dto.ApiError;
import com.gomoku.sync.api.dto.admin.AdminDailyPuzzleCreateResponse;
import com.gomoku.sync.api.dto.admin.AdminDailyPuzzleDetailResponse;
import com.gomoku.sync.api.dto.admin.AdminDailyPuzzleScheduleRequest;
import com.gomoku.sync.api.dto.admin.AdminDailyPuzzleUpsertRequest;
import com.gomoku.sync.domain.DailyPuzzle;
import com.gomoku.sync.service.AdminTokenService;
import com.gomoku.sync.service.DailyPuzzleAdminService;
import com.gomoku.sync.service.SessionJwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * 管理端：残局题库与排期。请求头 {@value AdminTokenService#HEADER}，配置项 gomoku.admin.token（环境变量
 * GOMOKU_ADMIN_TOKEN）；或配置 {@code gomoku.admin.openids} 后使用玩家 {@code Authorization: Bearer}。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminDailyPuzzleController {

    private final DailyPuzzleAdminService dailyPuzzleAdminService;
    private final AdminTokenService adminTokenService;
    private final SessionJwtService sessionJwtService;

    public AdminDailyPuzzleController(
            DailyPuzzleAdminService dailyPuzzleAdminService,
            AdminTokenService adminTokenService,
            SessionJwtService sessionJwtService) {
        this.dailyPuzzleAdminService = dailyPuzzleAdminService;
        this.adminTokenService = adminTokenService;
        this.sessionJwtService = sessionJwtService;
    }

    @GetMapping("/daily-puzzles")
    public ResponseEntity<?> list(
            @RequestHeader(value = AdminTokenService.HEADER, required = false) String adminToken,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        ResponseEntity<?> deny = gate(adminToken, authorization);
        if (deny != null) {
            return deny;
        }
        List<DailyPuzzle> list = dailyPuzzleAdminService.listPuzzles();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/daily-puzzles/{id}")
    public ResponseEntity<?> get(
            @RequestHeader(value = AdminTokenService.HEADER, required = false) String adminToken,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("id") long id) {
        ResponseEntity<?> deny = gate(adminToken, authorization);
        if (deny != null) {
            return deny;
        }
        try {
            AdminDailyPuzzleDetailResponse body = dailyPuzzleAdminService.getPuzzle(id);
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("NOT_FOUND", e.getMessage()));
        }
    }

    @PostMapping("/daily-puzzles")
    public ResponseEntity<?> create(
            @RequestHeader(value = AdminTokenService.HEADER, required = false) String adminToken,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody AdminDailyPuzzleUpsertRequest body) {
        ResponseEntity<?> deny = gate(adminToken, authorization);
        if (deny != null) {
            return deny;
        }
        try {
            AdminDailyPuzzleCreateResponse res = dailyPuzzleAdminService.create(body);
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiError("BAD_REQUEST", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError("ERROR", e.getMessage()));
        }
    }

    @PutMapping("/daily-puzzles/{id}")
    public ResponseEntity<?> update(
            @RequestHeader(value = AdminTokenService.HEADER, required = false) String adminToken,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("id") long id,
            @RequestBody AdminDailyPuzzleUpsertRequest body) {
        ResponseEntity<?> deny = gate(adminToken, authorization);
        if (deny != null) {
            return deny;
        }
        try {
            dailyPuzzleAdminService.update(id, body);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            if ("题目不存在".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("NOT_FOUND", e.getMessage()));
            }
            return ResponseEntity.badRequest().body(new ApiError("BAD_REQUEST", e.getMessage()));
        }
    }

    @PutMapping("/daily-puzzle-schedule")
    public ResponseEntity<?> schedule(
            @RequestHeader(value = AdminTokenService.HEADER, required = false) String adminToken,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody AdminDailyPuzzleScheduleRequest body) {
        ResponseEntity<?> deny = gate(adminToken, authorization);
        if (deny != null) {
            return deny;
        }
        if (body == null || body.getPuzzleDate() == null || body.getPuzzleDate().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiError("BAD_REQUEST", "puzzleDate 不能为空"));
        }
        try {
            dailyPuzzleAdminService.setSchedule(body.getPuzzleDate(), body.getPuzzleId());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiError("BAD_REQUEST", e.getMessage()));
        }
    }

    private ResponseEntity<?> gate(String adminToken, String authorization) {
        if (!adminTokenService.isApiEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiError(
                            "ADMIN_DISABLED",
                            "未配置 gomoku.admin.token 或 gomoku.admin.openids（GOMOKU_ADMIN_OPENIDS）"));
        }
        if (adminTokenService.matchesToken(adminToken)) {
            return null;
        }
        Optional<Long> uid = sessionJwtService.parseAuthorizationBearer(authorization);
        if (uid.isPresent() && adminTokenService.isUserAdmin(uid.get())) {
            return null;
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError("UNAUTHORIZED", "需要 " + AdminTokenService.HEADER + " 或管理员微信账号"));
    }
}
