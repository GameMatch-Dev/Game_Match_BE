package com.hd.gamematch.friendlymatch.application.service;

import com.hd.gamematch.friendlymatch.application.port.in.create.CreateFriendlyMatchCommand;
import com.hd.gamematch.friendlymatch.application.port.out.SaveFriendlyMatchPort;
import com.hd.gamematch.friendlymatch.application.exception.FriendlyMatchAccessDeniedException;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatch;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchSort;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchState;
import com.hd.gamematch.game.domain.Game;
import com.hd.gamematch.gameuser.application.port.in.find.FindGameUserQuery;
import com.hd.gamematch.gameuser.application.port.in.find.FindGameUserUseCase;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CreateFriendlyMatchServiceTest {

    @Mock
    private FindGameUserUseCase findGameUserUseCase;

    @Mock
    private SaveFriendlyMatchPort saveFriendlyMatchPort;

    @InjectMocks
    private CreateFriendlyMatchService createFriendlyMatchService;

    // 인증된 사용자의 게임 프로필을 호스트로 친선 경기 모집을 생성한다.
    @Test
    void 인증된_사용자가_자신의_게임_프로필로_친선_경기_모집을_생성한다() {
        // 생성 요청의 호스트가 될 기존 게임과 게임 프로필을 준비한다.
        Game game = Game.of(1L, "League of Legends", "MOBA", "https://example.com/lol");
        GameUserProfile host = new GameUserProfile(10L, "hostA", game, 3L);

        // JWT 사용자 3번이 10번 게임 프로필을 호스트로 지정한 생성 입력이다.
        CreateFriendlyMatchCommand command = CreateFriendlyMatchCommand.of(
                3L,
                10L,
                1L,
                LocalDate.of(2027, 6, 10),
                LocalTime.of(20, 0),
                FriendlyMatchSort.TEAM,
                5,
                "저녁에 같이 하실 분"
        );

        // 저장 Adapter가 DB에서 ID를 발급한 뒤 돌려줄 생성 완료 결과다.
        FriendlyMatch savedFriendlyMatch = new FriendlyMatch(
                100L,
                host,
                game,
                command.date(),
                command.time(),
                command.sort(),
                FriendlyMatchState.RECRUITING,
                command.capacity(),
                command.comment()
        );

        // Service가 호스트를 조회하고, 새 매치를 저장하면 위 결과를 돌려주도록 Mock을 설정한다.
        given(findGameUserUseCase.findGameUser(FindGameUserQuery.of(10L))).willReturn(host);
        given(saveFriendlyMatchPort.save(any(FriendlyMatch.class))).willReturn(savedFriendlyMatch);

        // 친선 경기 모집 생성 기능을 호출한다.
        FriendlyMatch result = createFriendlyMatchService.create(command);

        // 호출자에게는 저장 완료된 매치 정보를 반환해야 한다.
        assertThat(result).isEqualTo(savedFriendlyMatch);

        // 저장 Port에 전달한 생성 직전 매치를 꺼내, Service가 만든 상태를 검증한다.
        ArgumentCaptor<FriendlyMatch> friendlyMatchCaptor = ArgumentCaptor.forClass(FriendlyMatch.class);
        then(saveFriendlyMatchPort).should().save(friendlyMatchCaptor.capture());

        FriendlyMatch creatingFriendlyMatch = friendlyMatchCaptor.getValue();

        // DB 저장 전에는 ID가 없고, 모집 상태는 RECRUITING으로 시작해야 한다.
        assertThat(creatingFriendlyMatch.id()).isNull();
        assertThat(creatingFriendlyMatch.host()).isEqualTo(host);
        assertThat(creatingFriendlyMatch.game()).isEqualTo(game);
        assertThat(creatingFriendlyMatch.date()).isEqualTo(command.date());
        assertThat(creatingFriendlyMatch.time()).isEqualTo(command.time());
        assertThat(creatingFriendlyMatch.sort()).isEqualTo(FriendlyMatchSort.TEAM);
        assertThat(creatingFriendlyMatch.state()).isEqualTo(FriendlyMatchState.RECRUITING);
        assertThat(creatingFriendlyMatch.capacity()).isEqualTo(5);
        assertThat(creatingFriendlyMatch.comment()).isEqualTo("저녁에 같이 하실 분");
    }

    // 인증 사용자와 호스트 프로필 소유자가 다르면 타인의 프로필로 매치를 만들 수 없다.
    @Test
    void 타인의_게임_프로필로는_친선_경기_모집을_생성할_수_없다() {
        // JWT 사용자 3번이 사용자 4번 소유의 10번 게임 프로필을 호스트로 지정했다.
        Game game = Game.of(1L, "League of Legends", "MOBA", "https://example.com/lol");
        GameUserProfile anotherUsersHost = new GameUserProfile(10L, "hostA", game, 4L);
        CreateFriendlyMatchCommand command = CreateFriendlyMatchCommand.of(
                3L,
                10L,
                1L,
                LocalDate.of(2027, 6, 10),
                LocalTime.of(20, 0),
                FriendlyMatchSort.TEAM,
                5,
                "저녁에 같이 하실 분"
        );
        given(findGameUserUseCase.findGameUser(FindGameUserQuery.of(10L)))
                .willReturn(anotherUsersHost);

        // 소유권이 다르면 저장 전에 예외가 발생해야 한다.
        assertThatThrownBy(() -> createFriendlyMatchService.create(command))
                .isInstanceOf(FriendlyMatchAccessDeniedException.class)
                .hasMessage("친선 경기 모집을 만들 권한이 없습니다.");

        // 실패한 요청은 매치 저장으로 이어지면 안 된다.
        then(saveFriendlyMatchPort).shouldHaveNoInteractions();
    }

    // 호스트 프로필의 게임과 요청 게임이 다르면 잘못된 게임으로 매치를 만들 수 없다.
    @Test
    void 호스트_게임_프로필과_다른_게임으로는_친선_경기_모집을_생성할_수_없다() {
        // 10번 호스트 프로필은 1번 게임용인데 요청은 2번 게임을 지정했다.
        Game hostGame = Game.of(1L, "League of Legends", "MOBA", "https://example.com/lol");
        GameUserProfile host = new GameUserProfile(10L, "hostA", hostGame, 3L);
        CreateFriendlyMatchCommand command = CreateFriendlyMatchCommand.of(
                3L,
                10L,
                2L,
                LocalDate.of(2027, 6, 10),
                LocalTime.of(20, 0),
                FriendlyMatchSort.TEAM,
                5,
                "저녁에 같이 하실 분"
        );
        given(findGameUserUseCase.findGameUser(FindGameUserQuery.of(10L))).willReturn(host);

        // 게임이 다르면 저장 전에 예외가 발생해야 한다.
        assertThatThrownBy(() -> createFriendlyMatchService.create(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("호스트 게임 프로필의 게임과 요청 게임이 일치하지 않습니다.");

        // 실패한 요청은 매치 저장으로 이어지면 안 된다.
        then(saveFriendlyMatchPort).shouldHaveNoInteractions();
    }
}
