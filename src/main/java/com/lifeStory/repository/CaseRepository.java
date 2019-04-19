package com.lifeStory.repository;

import com.lifeStory.baseRepository.Repository;
import com.lifeStory.model.CaseEntity;

import java.util.Optional;

public interface CaseRepository extends Repository<CaseEntity, String> {

    Optional<CaseEntity> findByCaseSubjectId(String subjectId);

}
