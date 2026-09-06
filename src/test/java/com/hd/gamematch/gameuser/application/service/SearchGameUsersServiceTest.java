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

    @Test
    void 게임_조건이_없으면_null을_그대로_전달해_전체_게임에서_검색한다() {
        // given
        SearchGameUsersQuery query = SearchGameUsersQuery.of("player", null, 1, 10);
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
                        Game.of(4L, "Valorant", "FPS", "https://example.com/valorant"),
                        5L
                )
        );

        given(loadGameUsersPort.loadGameUsersByNicknamePrefix("player", null, 1, 10))
                .willReturn(profiles);
        given(loadGameUsersPort.countGameUsersByNicknamePrefix("player", null))
                .willReturn(2L);

        // when
        SearchGameUsersResult result = searchGameUsersService.searchGameUsers(query);

        // then
        assertThat(result.totalCount()).isEqualTo(2L);
        assertThat(result.gameUsers()).containsExactlyElementsOf(profiles);
        then(loadGameUsersPort).should()
                .loadGameUsersByNicknamePrefix("player", null, 1, 10);
        then(loadGameUsersPort).should()
                .countGameUsersByNicknamePrefix("player", null);
    }

    @Test
    void 검색_결과가_없으면_빈_목록과_0_개를_반환한다() {
        // given
        SearchGameUsersQuery query = SearchGameUsersQuery.of("unknown", null, 1, 10);

        given(loadGameUsersPort.loadGameUsersByNicknamePrefix("unknown", null, 1, 10))
                .willReturn(List.of());
        given(loadGameUsersPort.countGameUsersByNicknamePrefix("unknown", null))
                .willReturn(0L);

        // when
        SearchGameUsersResult result = searchGameUsersService.searchGameUsers(query);

        // then
        assertThat(result.totalCount()).isZero();
        assertThat(result.gameUsers()).isEmpty();
        then(loadGameUsersPort).should()
                .loadGameUsersByNicknamePrefix("unknown", null, 1, 10);
        then(loadGameUsersPort).should()
                .countGameUsersByNicknamePrefix("unknown", null);
    }
}
