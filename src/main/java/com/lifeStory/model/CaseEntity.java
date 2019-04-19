package com.lifeStory.model;

import lombok.Data;

import java.time.LocalDate;

/**
 * 其中笔录不能抽出来的数据就忽略了
 */
@Data
public class CaseEntity {

    private String caseSubjectId;
    private String caseId;              //案件编号
    private String caseGuid;
    private String caseName;            //案件名称
    private String caseType;            //案件类型，行政、刑事、调解等
    private String summary;             //简要案情

    private LocalDate updateTime;       //最后一份笔录发生变动的时间
    private String category;            //涉嫌罪名类型，诈骗、斗殴等

}
