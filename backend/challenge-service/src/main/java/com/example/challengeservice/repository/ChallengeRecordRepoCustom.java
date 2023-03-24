package com.example.challengeservice.repository;

import com.example.challengeservice.dto.response.PhotoRecordResponseDto;
import com.example.challengeservice.entity.ChallengeRecord;
import com.example.challengeservice.entity.UserChallenge;

import java.util.List;

public interface ChallengeRecordRepoCustom {

    List<PhotoRecordResponseDto> getSelfPhotoRecord(UserChallenge userChallenge   , String viewType);
    List<PhotoRecordResponseDto> getTeamPhotoRecord(Long challengeRoomId, String viewType);
}
