package com.excalicode.platform.core.model.chatbi;

import java.util.List;

/** ChatBI 查询结果 表格 */
public record ChatBiQueryResult(List<String> columns, List<List<Object>> rows) {}
