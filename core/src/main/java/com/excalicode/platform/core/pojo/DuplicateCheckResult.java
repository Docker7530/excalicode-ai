package com.excalicode.platform.core.pojo;

import java.util.ArrayList;
import java.util.List;

/**
 * 记录重复项的检测结果。
 */
public class DuplicateCheckResult {

    private final List<DuplicateItem> duplicateSubProcessDescs = new ArrayList<>();
    private final List<DuplicateItem> duplicateDataGroups = new ArrayList<>();
    private final List<DuplicateItem> duplicateDataAttributes = new ArrayList<>();

    public void addDuplicateSubProcessDesc(int processIndex, int stepIndex, String value) {
        duplicateSubProcessDescs.add(new DuplicateItem(processIndex, stepIndex, value));
    }

    public void addDuplicateDataGroup(int processIndex, int stepIndex, String value) {
        duplicateDataGroups.add(new DuplicateItem(processIndex, stepIndex, value));
    }

    public void addDuplicateDataAttribute(int processIndex, int stepIndex, String value) {
        duplicateDataAttributes.add(new DuplicateItem(processIndex, stepIndex, value));
    }

    public boolean hasDuplicates() {
        return !duplicateSubProcessDescs.isEmpty()
               || !duplicateDataGroups.isEmpty()
               || !duplicateDataAttributes.isEmpty();
    }

    public List<DuplicateItem> getDuplicateSubProcessDescs() {
        return duplicateSubProcessDescs;
    }

    public List<DuplicateItem> getDuplicateDataGroups() {
        return duplicateDataGroups;
    }

    public List<DuplicateItem> getDuplicateDataAttributes() {
        return duplicateDataAttributes;
    }
}
