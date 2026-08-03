package com.hd.gamematch.game.application.service;

import com.hd.gamematch.game.application.port.in.FindGameQuery;
import com.hd.gamematch.game.application.port.in.FindGameUseCase;
import com.hd.gamematch.game.application.port.out.LoadGamePort;
import com.hd.gamematch.game.application.exception.GameNotFoundException;
import com.hd.gamematch.game.domain.Game;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// Spring이 이 클래스를 "게임 한 건 조회" 업무를 처리하는 서비스로 등록한다.
// @RequiredArgsConstructor는 final 필드를 받는 생성자를 만들어 의존성을 주입할 수 있게 한다.
@Service
@RequiredArgsConstructor
public class FindGameService implements FindGameUseCase {

    // 서비스는 DB 구현체를 직접 알지 않고, 조회 역할만 약속한 포트(LoadGamePort)에 요청한다.
    // 덕분에 DB 대신 다른 저장소를 쓰거나 테스트용 가짜 객체를 넣어도 서비스 규칙은 그대로 유지된다.
    private final LoadGamePort loadGamePort;

    @Override
    public Game findGame(FindGameQuery query){
        // 조회 결과가 없음을 API 계층에서 구분할 수 있도록 전용 예외로 전달한다.
        return loadGamePort.loadGameById(query.gameId())
                // GameNotFoundException::new은 '예외를 새로 만드는 함수'를 짧게 쓴 메서드 참조다.
                // () -> new GameNotFoundException()과 같은 뜻이며, 전역 예외 처리기가 이 타입을 404로 바꾼다.
                .orElseThrow(GameNotFoundException::new);
    }
}
