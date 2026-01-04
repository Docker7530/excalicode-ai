package com.excalicode.platform.core.model.chatbi;

import java.util.List;

/** 已构建并通过校验的 SQL 查询 */
public record ChatBiBuiltQuery(String sql, List<Object> params, List<String> columns) {}
