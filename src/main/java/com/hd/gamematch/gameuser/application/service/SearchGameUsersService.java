package com.hd.gamematch.gameuser.application.service;

import com.hd.gamematch.gameuser.application.port.in.search.SearchGameUsersQuery;
import com.hd.gamematch.gameuser.application.port.in.search.SearchGameUsersResult;
import com.hd.gamematch.gameuser.application.port.in.search.SearchGameUsersUseCase;
import com.hd.gamematch.gameuser.application.port.out.LoadGameUsersPort;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchGameUsersService implements SearchGameUsersUseCase {

    private final LoadGameUsersPort loadGameUsersPort;

    @Override
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
