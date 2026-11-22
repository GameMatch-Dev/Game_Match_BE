package com.hd.gamematch.gameuser.application.service;

import com.hd.gamematch.gameuser.application.port.in.search.SearchGameUsersQuery;
import com.hd.gamematch.gameuser.application.port.in.search.SearchGameUsersResult;
import com.hd.gamematch.gameuser.application.port.in.search.SearchGameUsersUseCase;
import com.hd.gamematch.gameuser.application.port.out.LoadGameUsersPort;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchGameUsersService implements SearchGameUsersUseCase {

    private final LoadGameUsersPort loadGameUsersPort;

    @Override
    // 두 조회 사이에 다른 변경이 커밋되어도 목록과 전체 건수는 같은 스냅샷을 본다.
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public SearchGameUsersResult searchGameUsers(SearchGameUsersQuery query) {
        List<GameUserProfile> gameUsers = loadGameUsersPort.loadGameUsersByNicknamePrefix(
                query.nickname(),
                query.gameId(),
                query.page(),
                query.size()
        );
        long totalCount = loadGameUsersPort.countGameUsersByNicknamePrefix(
                query.nickname(),
                query.gameId()
        );

        return new SearchGameUsersResult(totalCount, gameUsers);
    }
}
