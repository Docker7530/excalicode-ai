package com.excalicode.platform.core.pojo;

/**
 * 重复项的位置信息和原始值。
 */
public record DuplicateItem(int processIndex, int stepIndex, String value) {}
