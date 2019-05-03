package com.toySpring.repository.custom;

import com.toySpring.repository.baseRepository.BaseRepository;
import lombok.Data;

@Data
public class CustomRepoSetting {

    private String entityPackage = null;
    private String repositoryPackage = null;
    private String dataSourceName = "default";
    private Class<? extends BaseRepository> baseRepositoryClass = null;

}
