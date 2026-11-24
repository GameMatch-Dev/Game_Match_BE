package com.hd.gamematch.gameuser.application.service;

import com.hd.gamematch.gameuser.application.exception.GameUserNotFoundException;
import com.hd.gamematch.gameuser.application.port.in.find.FindGameUserQuery;
import com.hd.gamematch.gameuser.application.port.in.find.FindGameUserUseCase;
import com.hd.gamematch.gameuser.application.port.out.LoadGameUserPort;
import com.hd.gamematch.gameuser.application.port.out.LoadGameUsersPort;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindGameUserService implements FindGameUserUseCase {

    private final LoadGameUserPort loadGameUserPort;
    private final LoadGameUsersPort loadGameUsersPort;

    @Override
    public GameUserProfile findGameUser(FindGameUserQuery query) {
        return loadGameUserPort.loadGameUserById(query.gameUserId())
                .orElseThrow(GameUserNotFoundException::new);
    }

    // 모든 입력을 검증한 뒤 일괄 조회하고 요청 순서로 복원한다.
    @Override
    @Transactional(readOnly = true)
    public List<GameUserProfile> findGameUsers(List<Long> gameUserIds) {
        if (gameUserIds.isEmpty()) {
            return List.of();
        }
        List<Long> validatedIds = gameUserIds.stream()
                .map(FindGameUserQuery::of)
                .map(FindGameUserQuery::gameUserId)
                .toList();
        Map<Long, GameUserProfile> profilesById = loadGameUsersPort.loadGameUsersByIds(validatedIds).stream()
                .collect(Collectors.toMap(GameUserProfile::id, profile -> profile));
        return validatedIds.stream()
                .map(id -> {
                    GameUserProfile profile = profilesById.get(id);
                    if (profile == null) {
                        throw new GameUserNotFoundException();
                    }
                    return profile;
                })
                .toList();
    }
}
