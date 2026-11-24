package com.hd.gamematch.friendlymatch.application.service;

import com.hd.gamematch.friendlymatch.application.port.in.create.CreateFriendlyMatchCommand;
import com.hd.gamematch.friendlymatch.application.port.out.SaveFriendlyMatchPort;
import com.hd.gamematch.friendlymatch.application.port.out.SaveHostRosterPort;
import com.hd.gamematch.friendlymatch.application.exception.FriendlyMatchAccessDeniedException;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatch;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchSort;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchState;
import com.hd.gamematch.game.domain.Game;
import com.hd.gamematch.gameuser.application.exception.GameUserNotFoundException;
import com.hd.gamematch.gameuser.application.port.in.find.FindGameUserQuery;
import com.hd.gamematch.gameuser.application.port.in.find.FindGameUserUseCase;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CreateFriendlyMatchServiceTest {

    @Mock
    private Clock clock;

    @BeforeEach
    void fixCurrentTime() {
        // 현재 시간을 고정해 실행 날짜와 무관하게 경계값을 검증한다.
        org.mockito.Mockito.lenient().when(clock.instant()).thenReturn(Instant.parse("2027-06-10T19:00:00Z"));
        org.mockito.Mockito.lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Mock
    private FindGameUserUseCase findGameUserUseCase;

    @Mock
    private SaveFriendlyMatchPort saveFriendlyMatchPort;

    @Mock
    private SaveHostRosterPort saveHostRosterPort;

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

    // 선택한 호스트 명단은 매치 저장 후 발급된 ID로 저장하고 그 결과를 반환한다.
    @ParameterizedTest
    @MethodSource("confirmedHostRosters")
    void 호스트_명단이_있으면_매치_저장_후_명단을_저장한_결과를_반환한다(List<Long> hostTeamMemberIds) {
        // 인증 사용자 소유의 호스트와 미래 팀 경기 요청을 준비한다.
        Game game = Game.of(1L, "League of Legends", "MOBA", "https://example.com/lol");
        GameUserProfile host = new GameUserProfile(10L, "hostA", game, 3L);
        CreateFriendlyMatchCommand command = CreateFriendlyMatchCommand.of(
                3L, 10L, 1L, LocalDate.of(2027, 6, 10), LocalTime.of(20, 0),
                FriendlyMatchSort.TEAM, 5, "정상 소개", hostTeamMemberIds);

        // 매치 저장 결과와 명단 저장 결과는 필드가 같아도 서로 다른 인스턴스다.
        FriendlyMatch savedFriendlyMatch = new FriendlyMatch(
                100L, host, game, command.date(), command.time(), command.sort(),
                FriendlyMatchState.RECRUITING, command.capacity(), command.comment());
        FriendlyMatch rosterSavedFriendlyMatch = new FriendlyMatch(
                100L, host, game, command.date(), command.time(), command.sort(),
                FriendlyMatchState.RECRUITING, command.capacity(), command.comment());
        given(findGameUserUseCase.findGameUser(FindGameUserQuery.of(10L))).willReturn(host);
        given(saveFriendlyMatchPort.save(any(FriendlyMatch.class))).willReturn(savedFriendlyMatch);
        // 아직 명단 저장 호출이 없으면 미사용 설정 대신 호출 순서 검증에서 실패하게 한다.
        if (!hostTeamMemberIds.isEmpty()) {
            lenient().when(findGameUserUseCase.findGameUsers(hostTeamMemberIds))
                    .thenReturn(rosterProfiles(hostTeamMemberIds, game));
        }
        lenient().when(saveHostRosterPort.saveHostRoster(100L, hostTeamMemberIds))
                .thenReturn(rosterSavedFriendlyMatch);

        // 매치를 생성한 뒤 발급된 ID와 요청 명단 그대로 저장하는 순서를 검증한다.
        FriendlyMatch result = createFriendlyMatchService.create(command);
        InOrder order = inOrder(saveFriendlyMatchPort, saveHostRosterPort);
        order.verify(saveFriendlyMatchPort).save(any(FriendlyMatch.class));
        order.verify(saveHostRosterPort).saveHostRoster(100L, hostTeamMemberIds);

        // 최종 반환값은 명단 저장 Port가 돌려준 인스턴스여야 한다.
        assertThat(result).isSameAs(rosterSavedFriendlyMatch);
    }

    // 같은 프로필을 중복 지정하면 생성과 명단 저장을 막는다.
    @Test
    void 중복된_호스트_명단이면_저장하지_않는다() {
        CreateFriendlyMatchCommand command = rosterCommand(FriendlyMatchSort.TEAM, List.of(20L, 20L));
        prepareRosterRequest(command);

        assertThatThrownBy(() -> createFriendlyMatchService.create(command))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(saveFriendlyMatchPort, saveHostRosterPort);
    }

    // 참가자 조회의 미존재 예외를 전달하고 생성 전에 저장을 막는다.
    @Test
    void 명단의_게임_프로필이_없으면_저장하지_않는다() {
        CreateFriendlyMatchCommand command = rosterCommand(FriendlyMatchSort.TEAM, List.of(20L, 99L));
        prepareRosterRequest(command);
        lenient().when(findGameUserUseCase.findGameUsers(command.hostTeamMemberIds()))
                .thenThrow(new GameUserNotFoundException());

        assertThatThrownBy(() -> createFriendlyMatchService.create(command))
                .isInstanceOf(GameUserNotFoundException.class);
        verifyNoInteractions(saveFriendlyMatchPort, saveHostRosterPort);
    }

    // 호스트와 다른 게임의 참가자는 모집 생성 전에 거절한다.
    @Test
    void 명단에_다른_게임_프로필이_있으면_저장하지_않는다() {
        CreateFriendlyMatchCommand command = rosterCommand(FriendlyMatchSort.TEAM, List.of(20L, 21L));
        prepareRosterRequest(command);
        Game otherGame = Game.of(2L, "Other Game", "FPS", null);
        lenient().when(findGameUserUseCase.findGameUsers(command.hostTeamMemberIds()))
                .thenReturn(List.of(new GameUserProfile(20L, "member20", rosterGame(), 4L),
                        new GameUserProfile(21L, "member21", otherGame, 5L)));

        assertThatThrownBy(() -> createFriendlyMatchService.create(command))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(saveFriendlyMatchPort, saveHostRosterPort);
    }

    // 두 모집 방식에서 실제 참가자 수가 최대 인원을 초과하면 저장을 막는다.
    @ParameterizedTest
    @EnumSource(FriendlyMatchSort.class)
    void 명단이_최대_인원을_초과하면_저장하지_않는다(FriendlyMatchSort sort) {
        CreateFriendlyMatchCommand command = rosterCommand(sort, List.of(20L, 21L, 22L, 23L, 24L, 25L));
        prepareRosterRequest(command);
        lenient().when(findGameUserUseCase.findGameUsers(command.hostTeamMemberIds()))
                .thenReturn(rosterProfiles(command.hostTeamMemberIds(), rosterGame()));

        assertThatThrownBy(() -> createFriendlyMatchService.create(command))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(saveFriendlyMatchPort, saveHostRosterPort);
    }

    // 참가하지 않는 주최자를 더하지 않고 명단의 실제 참가자만 센다.
    @ParameterizedTest
    @EnumSource(FriendlyMatchSort.class)
    void 명단이_최대_인원과_같으면_저장한다(FriendlyMatchSort sort) {
        List<Long> ids = List.of(20L, 21L, 22L, 23L, 24L);
        CreateFriendlyMatchCommand command = rosterCommand(sort, ids);
        FriendlyMatch saved = prepareRosterRequest(command);
        lenient().when(findGameUserUseCase.findGameUsers(ids)).thenReturn(rosterProfiles(ids, rosterGame()));
        given(saveHostRosterPort.saveHostRoster(100L, ids)).willReturn(saved);

        assertThat(createFriendlyMatchService.create(command)).isSameAs(saved);
        then(saveHostRosterPort).should().saveHostRoster(100L, ids);
    }

    // 두 모집 방식에서 빈 명단도 확정 명단으로 저장한다.
    @ParameterizedTest
    @EnumSource(FriendlyMatchSort.class)
    void 빈_명단도_확정하여_저장한다(FriendlyMatchSort sort) {
        List<Long> ids = List.of();
        CreateFriendlyMatchCommand command = rosterCommand(sort, ids);
        FriendlyMatch saved = prepareRosterRequest(command);
        given(saveHostRosterPort.saveHostRoster(100L, ids)).willReturn(saved);

        assertThat(createFriendlyMatchService.create(command)).isSameAs(saved);
        then(saveHostRosterPort).should().saveHostRoster(100L, ids);
    }

    // 검증 누락이 저장 반환값의 null 오류로 가려지지 않게 정상 결과를 준비한다.
    private FriendlyMatch prepareRosterRequest(CreateFriendlyMatchCommand command) {
        Game game = rosterGame();
        GameUserProfile host = new GameUserProfile(10L, "hostA", game, 3L);
        given(findGameUserUseCase.findGameUser(FindGameUserQuery.of(10L))).willReturn(host);
        FriendlyMatch saved = new FriendlyMatch(
                100L, host, game, command.date(), command.time(), command.sort(),
                FriendlyMatchState.RECRUITING, command.capacity(), command.comment());
        lenient().when(saveFriendlyMatchPort.save(any(FriendlyMatch.class))).thenReturn(saved);
        return saved;
    }

    // 모집 방식과 참가자 명단을 지정한 생성 요청을 준비한다.
    private CreateFriendlyMatchCommand rosterCommand(FriendlyMatchSort sort, List<Long> ids) {
        return CreateFriendlyMatchCommand.of(
                3L, 10L, 1L, LocalDate.of(2027, 6, 10), LocalTime.of(20, 0),
                sort, 5, "정상 소개", ids);
    }

    // 호스트와 참가자가 함께 사용할 기준 게임을 준비한다.
    private Game rosterGame() {
        return Game.of(1L, "League of Legends", "MOBA", "https://example.com/lol");
    }

    // 명단의 각 식별자에 대응하는 같은 게임 프로필을 준비한다.
    private List<GameUserProfile> rosterProfiles(List<Long> ids, Game game) {
        return ids.stream().map(id -> new GameUserProfile(id, "member" + id, game,
                id.equals(10L) ? 3L : id)).toList();
    }

    // 실제 참가자 명단은 주최자를 포함하거나 참가자가 없는 확정 명단일 수 있다.
    static Stream<List<Long>> confirmedHostRosters() {
        return Stream.of(List.of(20L, 21L), List.of(10L, 20L), List.<Long>of());
    }

    // 호스트 명단을 생략한 기존 생성 호출은 매치 저장 결과를 그대로 반환한다.
    @Test
    void 호스트_명단을_생략하면_명단을_저장하지_않는다() {
        // 명단 인자가 없는 기존 팩토리로 미래 팀 경기 요청을 준비한다.
        Game game = Game.of(1L, "League of Legends", "MOBA", "https://example.com/lol");
        GameUserProfile host = new GameUserProfile(10L, "hostA", game, 3L);
        CreateFriendlyMatchCommand command = CreateFriendlyMatchCommand.of(
                3L, 10L, 1L, LocalDate.of(2027, 6, 10), LocalTime.of(20, 0),
                FriendlyMatchSort.TEAM, 5, "정상 소개");
        FriendlyMatch savedFriendlyMatch = new FriendlyMatch(
                100L, host, game, command.date(), command.time(), command.sort(),
                FriendlyMatchState.RECRUITING, command.capacity(), command.comment());
        given(findGameUserUseCase.findGameUser(FindGameUserQuery.of(10L))).willReturn(host);
        given(saveFriendlyMatchPort.save(any(FriendlyMatch.class))).willReturn(savedFriendlyMatch);

        // 명단 저장 없이 생성 완료된 매치 인스턴스를 그대로 반환해야 한다.
        FriendlyMatch result = createFriendlyMatchService.create(command);
        verifyNoInteractions(saveHostRosterPort);
        assertThat(result).isSameAs(savedFriendlyMatch);
    }

    // 호스트 명단을 명시적으로 null로 전달해도 매치 저장 결과를 그대로 반환한다.
    @Test
    void 호스트_명단이_null이면_명단을_저장하지_않는다() {
        // 명단 인자에 null을 지정한 미래 팀 경기 요청을 준비한다.
        Game game = Game.of(1L, "League of Legends", "MOBA", "https://example.com/lol");
        GameUserProfile host = new GameUserProfile(10L, "hostA", game, 3L);
        CreateFriendlyMatchCommand command = CreateFriendlyMatchCommand.of(
                3L, 10L, 1L, LocalDate.of(2027, 6, 10), LocalTime.of(20, 0),
                FriendlyMatchSort.TEAM, 5, "정상 소개", null);
        FriendlyMatch savedFriendlyMatch = new FriendlyMatch(
                100L, host, game, command.date(), command.time(), command.sort(),
                FriendlyMatchState.RECRUITING, command.capacity(), command.comment());
        given(findGameUserUseCase.findGameUser(FindGameUserQuery.of(10L))).willReturn(host);
        given(saveFriendlyMatchPort.save(any(FriendlyMatch.class))).willReturn(savedFriendlyMatch);

        // 명단 저장 없이 생성 완료된 매치 인스턴스를 그대로 반환해야 한다.
        FriendlyMatch result = createFriendlyMatchService.create(command);
        verifyNoInteractions(saveHostRosterPort);
        assertThat(result).isSameAs(savedFriendlyMatch);
    }

    // 같은 날의 과거 시간과 현재 시각은 모집 가능한 미래 일시가 아니다.
    @ParameterizedTest
    @ValueSource(strings = {"18:59:59", "19:00:00"})
    void 경기_일시가_현재_이하면_저장하지_않는다(String time) {
        Game game = Game.of(1L, "League of Legends", "MOBA", "https://example.com/lol");
        GameUserProfile host = new GameUserProfile(10L, "hostA", game, 3L);
        given(findGameUserUseCase.findGameUser(FindGameUserQuery.of(10L))).willReturn(host);
        CreateFriendlyMatchCommand command = CreateFriendlyMatchCommand.of(
                3L, 10L, 1L, LocalDate.of(2027, 6, 10), LocalTime.parse(time),
                FriendlyMatchSort.TEAM, 5, "정상 소개");

        assertThatThrownBy(() -> createFriendlyMatchService.create(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("경기 일시는 현재보다 미래여야 합니다.");
        then(saveFriendlyMatchPort).shouldHaveNoInteractions();
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
                "저녁에 같이 하실 분",
                List.of(20L, 21L)
        );
        given(findGameUserUseCase.findGameUser(FindGameUserQuery.of(10L)))
                .willReturn(anotherUsersHost);

        // 소유권이 다르면 저장 전에 예외가 발생해야 한다.
        assertThatThrownBy(() -> createFriendlyMatchService.create(command))
                .isInstanceOf(FriendlyMatchAccessDeniedException.class)
                .hasMessage("친선 경기 모집을 만들 권한이 없습니다.");
        then(findGameUserUseCase).should(never()).findGameUsers(any());

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
