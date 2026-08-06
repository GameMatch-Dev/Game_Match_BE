package com.hd.gamematch.game.adapter.in.web;


import com.hd.gamematch.game.application.port.in.FindGameQuery;
import com.hd.gamematch.game.application.port.in.FindGameUseCase;
import com.hd.gamematch.game.application.port.in.FindGamesQuery;
import com.hd.gamematch.game.application.port.in.FindGamesUseCase;
import com.hd.gamematch.game.domain.Game;
import com.hd.gamematch.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/games")
public class GameController {
    private final FindGamesUseCase findGamesUseCase;
    private final FindGameUseCase findGameUseCase;

    @GetMapping
    public CommonResponse<List<GameResponse>> findGames(
            @RequestParam MultiValueMap<String, String> parameters
    ) {
        String name = extractSingleValue(parameters, "name");
        String sort = extractSingleValue(parameters, "sort");

        return findGames(name, sort);
    }

    public CommonResponse<List<GameResponse>> findGames(String name, String sort) {
        List<GameResponse> response = findGamesUseCase.findGames(FindGamesQuery.of(name, sort))
                .stream()
                .map(GameResponse::from)
                .toList();

        return CommonResponse.success(response);
    }

    private String extractSingleValue(MultiValueMap<String, String> parameters, String parameterName) {
        List<String> values = parameters.get(parameterName);
        if (values == null || values.isEmpty()) {
            return null;
        }
        if (values.size() > 1) {
            throw new IllegalArgumentException(parameterName + " 파라미터는 한 번만 지정할 수 있습니다.");
        }
        return values.get(0);
    }


    @GetMapping("/{gameId}")
    public CommonResponse<GameResponse> findGame(@PathVariable Long gameId){
        Game game = findGameUseCase.findGame(FindGameQuery.of(gameId));

        return CommonResponse.success(GameResponse.from(game));
    }
}
