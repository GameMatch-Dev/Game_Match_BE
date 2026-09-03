package com.hd.gamematch.gameuser.application.service;

import com.hd.gamematch.game.domain.Game;
import com.hd.gamematch.gameuser.application.port.in.search.SearchGameUsersQuery;
import com.hd.gamematch.gameuser.application.port.in.search.SearchGameUsersResult;
import com.hd.gamematch.gameuser.application.port.out.LoadGameUsersPort;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SearchGameUsersServiceTest {

    @Mock
    private LoadGameUsersPort  loadGameUsersPort;

    @InjectMocks
    private SearchGameUsersService searchGameUsersService;

    @Test
    void 닉네임_접두사와_게임_조건으로_페이지를_조회하고_전체_개수를_반환한다() {
        // given
        SearchGameUsersQuery query = SearchGameUsersQuery.of("player", 2L, 2, 10);
        List<GameUserProfile> profiles = List.of(
                new GameUserProfile(
                        11L,
                        "playerA",
                        Game.of(2L, "League of Legends", "MOBA", "https://example.com/lol"),
                        3L
                ),
                new GameUserProfile(
                        12L,
                        "playerB",
                        Game.of(2L, "League of Legends", "MOBA", "https://example.com/lol"),
                        4L
                )
        );

        given(loadGameUsersPort.loadGameUsersByNicknamePrefix("player", 2L, 2, 10))
                .willReturn(profiles);
        given(loadGameUsersPort.countGameUsersByNicknamePrefix("player", 2L))
                .willReturn(12L);

        // when
        SearchGameUsersResult result = searchGameUsersService.searchGameUsers(query);

        // then
        assertThat(result.totalCount()).isEqualTo(12L);
        assertThat(result.gameUsers()).containsExactlyElementsOf(profiles);
        then(loadGameUsersPort).should()
                .loadGameUsersByNicknamePrefix("player", 2L, 2, 10);
        then(loadGameUsersPort).should()
                .countGameUsersByNicknamePrefix("player", 2L);
    }
}
