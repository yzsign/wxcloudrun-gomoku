package com.gomoku.sync.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gomoku.sync.api.dto.CheckinResponse;
import com.gomoku.sync.domain.User;
import com.gomoku.sync.domain.UserCheckinState;
import com.gomoku.sync.mapper.UserCheckinMapper;
import com.gomoku.sync.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

@Service
public class CheckinService {

    private static final ZoneId CHECKIN_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int DAILY_REWARD_POINTS = 10;
    private static final int MAX_HISTORY = 500;

    private final UserMapper userMapper;
    private final UserCheckinMapper userCheckinMapper;
    private final ObjectMapper objectMapper;

    public CheckinService(
            UserMapper userMapper,
            UserCheckinMapper userCheckinMapper,
            ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.userCheckinMapper = userCheckinMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CheckinResponse checkin(long userId) {
        User u = userMapper.selectByIdForUpdate(userId);
        if (u == null) {
            return new CheckinResponse(false, false, 0, 0, 0, false, false, null, new ArrayList<>());
        }
        UserCheckinState st = userCheckinMapper.selectByUserIdForUpdate(userId);
        if (st == null) {
            userCheckinMapper.insertInitial(userId);
            st = userCheckinMapper.selectByUserIdForUpdate(userId);
        }
        if (st == null) {
            return new CheckinResponse(false, false, 0, 0, u.getActivityPoints(), false, false, null, new ArrayList<>());
        }

        LocalDate today = LocalDate.now(CHECKIN_ZONE);
        String todayStr = today.toString();
        String last = st.getLastCheckinYmd();
        if (last != null && last.equals(todayStr)) {
            List<String> hist = readHistory(st.getHistoryJson());
            return new CheckinResponse(
                    false,
                    true,
                    st.getStreak(),
                    0,
                    u.getActivityPoints(),
                    st.isPieceSkinTuanMoeUnlocked(),
                    false,
                    todayStr,
                    hist);
        }

        int newStreak = 1;
        if (last != null && !last.isEmpty()) {
            LocalDate lastD = LocalDate.parse(last);
            LocalDate yest = today.minusDays(1);
            if (lastD.equals(yest)) {
                newStreak = st.getStreak() + 1;
            } else if (!lastD.equals(today)) {
                newStreak = 1;
            }
        }

        List<String> histList = new ArrayList<>(readHistory(st.getHistoryJson()));
        TreeSet<String> sorted = new TreeSet<>(histList);
        sorted.add(todayStr);
        List<String> capped = new ArrayList<>(sorted);
        if (capped.size() > MAX_HISTORY) {
            capped = new ArrayList<>(capped.subList(capped.size() - MAX_HISTORY, capped.size()));
        }

        String historyJson;
        try {
            historyJson = objectMapper.writeValueAsString(capped);
        } catch (Exception e) {
            historyJson = "[]";
        }

        int newPoints = u.getActivityPoints() + DAILY_REWARD_POINTS;
        boolean wasUnlocked = st.isPieceSkinTuanMoeUnlocked();
        boolean nowUnlocked = wasUnlocked || newStreak >= 7;

        st.setLastCheckinYmd(todayStr);
        st.setStreak(newStreak);
        st.setHistoryJson(historyJson);
        st.setPieceSkinTuanMoeUnlocked(nowUnlocked);

        u.setActivityPoints(newPoints);
        userMapper.updateActivityPoints(u);
        userCheckinMapper.updateState(st);

        return new CheckinResponse(
                true,
                false,
                newStreak,
                DAILY_REWARD_POINTS,
                newPoints,
                nowUnlocked,
                !wasUnlocked && nowUnlocked,
                todayStr,
                capped);
    }

    private List<String> readHistory(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return list != null ? new ArrayList<>(list) : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
