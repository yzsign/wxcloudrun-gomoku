package com.gomoku.sync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gomoku.sync.domain.GameRoom;
import com.gomoku.sync.domain.GameRoomStateSnapshot;
import com.gomoku.sync.domain.RoomGameStateRow;
import com.gomoku.sync.domain.Stone;
import com.gomoku.sync.mapper.RoomGameStateMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对局状态 MySQL 持久化与多实例同步（乐观锁 state_version + 轮询拉取）。
 */
@Service
public class RoomGameStateService {

    private final RoomGameStateMapper roomGameStateMapper;
    private final ObjectMapper objectMapper;
    private final int boardSize;
    /** 本实例最近一次与 DB 一致的版本号（按 roomId） */
    private final Map<String, Long> lastKnownVersion = new ConcurrentHashMap<>();

    public RoomGameStateService(
            RoomGameStateMapper roomGameStateMapper,
            ObjectMapper objectMapper,
            @Value("${gomoku.board-size:15}") int boardSize) {
        this.roomGameStateMapper = roomGameStateMapper;
        this.objectMapper = objectMapper;
        this.boardSize = boardSize;
    }

    public void insertInitial(String roomId) {
        try {
            GameRoomStateSnapshot empty = GameRoomStateSnapshot.empty(boardSize);
            String json = objectMapper.writeValueAsString(empty);
            roomGameStateMapper.insertInitial(roomId, json);
            lastKnownVersion.put(roomId, 0L);
        } catch (Exception e) {
            throw new IllegalStateException("写入 room_game_state 失败", e);
        }
    }

    /**
     * 残局好友房：写入给定盘面与行棋方（房主观战，黑座可无 WS）。
     */
    public void insertPuzzleInitial(String roomId, int[][] board, int sideToMove) {
        DailyPuzzleAdminService.validateBoardCells(board, boardSize);
        if (sideToMove != Stone.BLACK && sideToMove != Stone.WHITE) {
            throw new IllegalArgumentException("sideToMove 须为 1（黑）或 2（白）");
        }
        try {
            GameRoomStateSnapshot snap = new GameRoomStateSnapshot();
            snap.setBoardSize(boardSize);
            int[][] b = new int[boardSize][boardSize];
            for (int i = 0; i < boardSize; i++) {
                System.arraycopy(board[i], 0, b[i], 0, boardSize);
            }
            snap.setBoard(b);
            snap.setCurrent(sideToMove);
            snap.setGameOver(false);
            snap.setWinner(null);
            snap.setMatchRound(1);
            snap.setMoves(new ArrayList<>());
            snap.setPendingUndoRequesterColor(null);
            snap.setPendingUndoPops(0);
            snap.setPendingRematchRequesterColor(null);
            snap.setPendingDrawRequesterColor(null);
            snap.setClusterBlackConnected(false);
            snap.setClusterWhiteConnected(false);
            long now = System.currentTimeMillis();
            snap.setClockMoveDeadlineWallMs(now + GameRoom.CLOCK_MOVE_MS);
            int stones = countStones(b);
            if (stones > 0) {
                snap.setClockGameDeadlineWallMs(now + GameRoom.CLOCK_GAME_MS);
            } else {
                snap.setClockGameDeadlineWallMs(0L);
            }
            snap.setClockPauseStartedWallMs(0L);
            snap.setFriendBothHumanSeatsLiveOnce(false);
            snap.setGameEndReason(null);
            String json = objectMapper.writeValueAsString(snap);
            roomGameStateMapper.insertInitial(roomId, json);
            lastKnownVersion.put(roomId, 0L);
        } catch (Exception e) {
            throw new IllegalStateException("写入残局初始状态失败", e);
        }
    }

    private static int countStones(int[][] b) {
        int n = 0;
        for (int[] row : b) {
            for (int v : row) {
                if (v != Stone.EMPTY) {
                    n++;
                }
            }
        }
        return n;
    }

    public void deleteByRoomId(String roomId) {
        roomGameStateMapper.deleteByRoomId(roomId);
        lastKnownVersion.remove(roomId);
    }

    /** 从 DB 加载完整快照到 GameRoom（新建房间或跨实例首次加载） */
    public void hydrateRoom(GameRoom room) {
        String roomId = room.getRoomId();
        RoomGameStateRow row = roomGameStateMapper.selectByRoomId(roomId);
        if (row == null) {
            insertInitial(roomId);
            row = roomGameStateMapper.selectByRoomId(roomId);
        }
        if (row == null) {
            return;
        }
        room.getLock().lock();
        try {
            applySnapshotJson(room, row.getStateJson());
            lastKnownVersion.put(roomId, row.getStateVersion());
        } finally {
            room.getLock().unlock();
        }
    }

    /** 若 DB 版本更新于本实例缓存，则覆盖内存棋盘（对局消息处理前调用） */
    public void syncRoomFromDbIfBehind(GameRoom room) {
        String roomId = room.getRoomId();
        RoomGameStateRow row = roomGameStateMapper.selectByRoomId(roomId);
        if (row == null) {
            insertInitial(roomId);
            row = roomGameStateMapper.selectByRoomId(roomId);
        }
        if (row == null) {
            return;
        }
        long dbVer = row.getStateVersion();
        Long local = lastKnownVersion.get(roomId);
        if (local != null && dbVer <= local) {
            return;
        }
        room.getLock().lock();
        try {
            row = roomGameStateMapper.selectByRoomId(roomId);
            if (row == null) {
                return;
            }
            dbVer = row.getStateVersion();
            local = lastKnownVersion.get(roomId);
            if (local != null && dbVer <= local) {
                return;
            }
            applySnapshotJson(room, row.getStateJson());
            lastKnownVersion.put(roomId, dbVer);
        } finally {
            room.getLock().unlock();
        }
    }

    public boolean tryPersist(GameRoom room) {
        String roomId = room.getRoomId();
        room.getLock().lock();
        try {
            GameRoomStateSnapshot snap = room.toStateSnapshot();
            String json = objectMapper.writeValueAsString(snap);
            Long expected = lastKnownVersion.get(roomId);
            if (expected == null) {
                RoomGameStateRow row = roomGameStateMapper.selectByRoomId(roomId);
                if (row == null) {
                    insertInitial(roomId);
                    row = roomGameStateMapper.selectByRoomId(roomId);
                }
                if (row == null) {
                    return false;
                }
                expected = row.getStateVersion();
                lastKnownVersion.put(roomId, expected);
            }
            int n = roomGameStateMapper.updateState(roomId, json, expected);
            if (n == 0) {
                return false;
            }
            lastKnownVersion.put(roomId, expected + 1);
            return true;
        } catch (Exception e) {
            throw new IllegalStateException("持久化对局状态失败", e);
        } finally {
            room.getLock().unlock();
        }
    }

    public void forceReloadFromDb(GameRoom room) {
        RoomGameStateRow row = roomGameStateMapper.selectByRoomId(room.getRoomId());
        if (row == null) {
            return;
        }
        room.getLock().lock();
        try {
            applySnapshotJson(room, row.getStateJson());
            lastKnownVersion.put(room.getRoomId(), row.getStateVersion());
        } finally {
            room.getLock().unlock();
        }
    }

    /**
     * 轮询：若 DB 版本新于本实例已知版本，则应用并返回 true（由调用方 broadcast）。
     */
    public boolean pollApplyIfNewer(String roomId, GameRoom room) {
        Long dbVerObj = roomGameStateMapper.selectStateVersionByRoomId(roomId);
        if (dbVerObj == null) {
            return false;
        }
        long dbVer = dbVerObj;
        Long local = lastKnownVersion.get(roomId);
        if (local != null && dbVer <= local) {
            return false;
        }
        room.getLock().lock();
        try {
            RoomGameStateRow row = roomGameStateMapper.selectByRoomId(roomId);
            if (row == null) {
                return false;
            }
            dbVer = row.getStateVersion();
            local = lastKnownVersion.get(roomId);
            if (local != null && dbVer <= local) {
                return false;
            }
            applySnapshotJson(room, row.getStateJson());
            lastKnownVersion.put(roomId, dbVer);
            return true;
        } finally {
            room.getLock().unlock();
        }
    }

    private void applySnapshotJson(GameRoom room, String json) {
        try {
            GameRoomStateSnapshot snap = objectMapper.readValue(json, GameRoomStateSnapshot.class);
            room.replaceGameStateFromSnapshot(snap);
        } catch (Exception e) {
            throw new IllegalStateException("解析对局快照失败", e);
        }
    }
}
