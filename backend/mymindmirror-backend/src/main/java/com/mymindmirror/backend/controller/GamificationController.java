package com.mymindmirror.backend.controller;

import com.mymindmirror.backend.annotation.CurrentUser;
import com.mymindmirror.backend.api.GamificationApi;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.response.UserStatsResponse;
import com.mymindmirror.backend.service.GamificationService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gamification")
@RequiredArgsConstructor
public class GamificationController implements GamificationApi {

    private final GamificationService gamificationService;

    @Override
    @GetMapping("/stats")
    public ResponseEntity<UserStatsResponse> getUserStats(@CurrentUser User currentUser) {
        UserStatsResponse stats = gamificationService.getUserStats(currentUser);
        return ResponseEntity.ok(stats);
    }
}