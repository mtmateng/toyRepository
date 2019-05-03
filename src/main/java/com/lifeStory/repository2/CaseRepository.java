package com.lifeStory.repository2;

import com.lifeStory.baseRepo.MyBaseRepo;
import com.lifeStory.model2.CaseEntity;

import java.util.Optional;

public interface CaseRepository extends MyBaseRepo<CaseEntity, String> {

    Optional<CaseEntity> findByCaseSubjectId(String subjectId);

}
