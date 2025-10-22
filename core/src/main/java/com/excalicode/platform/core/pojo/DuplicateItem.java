package com.excalicode.platform.core.pojo;

/**
 * 重复项的位置信息和原始值。
 */
public class DuplicateItem {

    private final int index;
    private final String value;

    public DuplicateItem(int index, String value) {
        this.index = index;
        this.value = value;
    }

    public int getIndex() {
        return index;
    }

    public String getValue() {
        return value;
    }
}
