package com.gomoku.sync.service;

import com.gomoku.sync.api.dto.RecordPveGameRequest;
import com.gomoku.sync.domain.GameRecord;
import com.gomoku.sync.domain.User;
import com.gomoku.sync.mapper.GameMapper;
import com.gomoku.sync.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PveGameService {

    private final UserMapper userMapper;
    private final GameMapper gameMapper;

    public PveGameService(UserMapper userMapper, GameMapper gameMapper) {
        this.userMapper = userMapper;
        this.gameMapper = gameMapper;
    }

    /**
     * 人机对局写入 games：对手为随机人机账号；Elo 字段记录当前值，delta 全 0。
     */
    public long recordPveGame(long humanUserId, RecordPveGameRequest req) {
        if (req.getTotalSteps() < 1 || req.getTotalSteps() > 15 * 15) {
            throw new IllegalArgumentException("totalSteps 非法");
        }
        String mr = req.getMyResult() != null ? req.getMyResult().trim().toUpperCase() : "";
        if (!"WIN".equals(mr) && !"LOSS".equals(mr) && !"DRAW".equals(mr)) {
            throw new IllegalArgumentException("myResult 须为 WIN、LOSS 或 DRAW");
        }

        Long botId = userMapper.selectRandomBotId();
        if (botId == null) {
            throw new IllegalStateException("暂无人机账号，无法归档");
        }
        User human = userMapper.selectById(humanUserId);
        User bot = userMapper.selectById(botId);
        if (human == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (bot == null || !bot.isBot()) {
            throw new IllegalStateException("人机账号异常");
        }

        boolean playBlack = req.isPlayBlack();
        long blackId = playBlack ? humanUserId : botId;
        long whiteId = playBlack ? botId : humanUserId;

        String outcome = resolveOutcome(playBlack, mr);

        int hElo = human.getEloScore();
        int bElo = bot.getEloScore();
        int blackElo = blackId == humanUserId ? hElo : bElo;
        int whiteElo = whiteId == humanUserId ? hElo : bElo;

        String roomId = "PVE" + UUID.randomUUID().toString().replace("-", "").substring(0, 29);

        GameRecord g = new GameRecord();
        g.setRoomId(roomId);
        g.setMatchRound(1);
        g.setBlackUserId(blackId);
        g.setWhiteUserId(whiteId);
        g.setTotalSteps(req.getTotalSteps());
        g.setOutcome(outcome);
        g.setRunawayUserId(null);
        g.setBlackEloBefore(blackElo);
        g.setWhiteEloBefore(whiteElo);
        g.setBlackEloAfter(blackElo);
        g.setWhiteEloAfter(whiteElo);
        g.setBlackEloDelta(0);
        g.setWhiteEloDelta(0);
        String mj = req.getMovesJson();
        if (mj != null && mj.trim().isEmpty()) {
            mj = null;
        }
        g.setMovesJson(mj);

        gameMapper.insert(g);
        return g.getId() != null ? g.getId() : 0L;
    }

    static String resolveOutcome(boolean humanPlaysBlack, String myResultUpper) {
        if ("DRAW".equals(myResultUpper)) {
            return GameHistoryService.OUTCOME_DRAW;
        }
        boolean win = "WIN".equals(myResultUpper);
        if (humanPlaysBlack) {
            return win ? GameHistoryService.OUTCOME_BLACK_WIN : GameHistoryService.OUTCOME_WHITE_WIN;
        }
        return win ? GameHistoryService.OUTCOME_WHITE_WIN : GameHistoryService.OUTCOME_BLACK_WIN;
    }
}
