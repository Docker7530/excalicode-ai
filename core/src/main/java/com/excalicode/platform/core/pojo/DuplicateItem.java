package com.excalicode.platform.core.pojo;

import lombok.Getter;

/**
 * 重复项的位置信息和原始值。
 */
@Getter
public class DuplicateItem {

    private final int processIndex;
    private final int stepIndex;
    private final String value;

    public DuplicateItem(int processIndex, int stepIndex, String value) {
        this.processIndex = processIndex;
        this.stepIndex = stepIndex;
        this.value = value;
    }

}
