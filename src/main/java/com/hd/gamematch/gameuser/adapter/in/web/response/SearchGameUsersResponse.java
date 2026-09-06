package com.hd.gamematch.gameuser.adapter.in.web.response;

import com.hd.gamematch.gameuser.application.port.in.search.SearchGameUsersResult;

import java.util.List;

public record SearchGameUsersResponse(
        long totalCount,
        List<FindGameUserResponse> gameUsers
) {

    public static SearchGameUsersResponse from(SearchGameUsersResult result) {
        return new SearchGameUsersResponse(
                result.totalCount(),
                result.gameUsers().stream()
                        .map(FindGameUserResponse::from)
                        .toList()
        );
    }
}
