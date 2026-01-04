package com.excalicode.platform.core.service.chatbi;

import com.excalicode.platform.core.exception.BusinessException;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.util.TablesNamesFinder;

/**
 * SQL 安全栅栏：只允许单条 SELECT，并限制表范围。后续考虑 jsqlparser 做更多静态检查。
 *
 * <p>注意：我们本来就不信任模型输出，因此 SQL 由后端构建；这里属于双保险。
 */
@Slf4j
public final class ChatBiSqlSafetyGuard {

  private ChatBiSqlSafetyGuard() {}

  public static void assertSafeSelect(String sql) {
    if (sql == null || sql.isBlank()) {
      throw new BusinessException("SQL 为空");
    }

    // JSqlParser 对 JDBC 占位符支持不好；解析时用哑元替换，先结构校验。
    String sqlForParse = sql.replace('?', '0');
    sqlForParse = normalizeWhitespace(sqlForParse);

    try {
      Statement statement = CCJSqlParserUtil.parse(sqlForParse);

      if (statement instanceof SetOperationList) {
        throw new BusinessException("不允许 UNION/INTERSECT/EXCEPT");
      }
      if (!(statement instanceof PlainSelect)) {
        throw new BusinessException("仅允许执行单条 SELECT 查询");
      }

      TablesNamesFinder tablesFinder = new TablesNamesFinder();
      List<String> tables = tablesFinder.getTableList(statement);
      for (String table : tables) {
        String normalized = table == null ? "" : table.trim().toLowerCase(Locale.ROOT);
        if (!TaskBiSchema.ALLOWED_TABLES.contains(normalized)) {
          throw new BusinessException("不允许访问的表: " + table);
        }
      }
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      log.warn("SQL 解析失败: sql={} ", sqlForParse, ex);
      throw new BusinessException("SQL 解析失败");
    }
  }

  private static String normalizeWhitespace(String sql) {
    if (sql == null || sql.isEmpty()) {
      return sql;
    }

    StringBuilder sb = new StringBuilder(sql.length());
    boolean lastWasSpace = false;
    for (int i = 0; i < sql.length(); i++) {
      char c = sql.charAt(i);
      boolean isSpace = Character.isWhitespace(c);
      if (isSpace) {
        if (!lastWasSpace) {
          sb.append(' ');
          lastWasSpace = true;
        }
        continue;
      }
      sb.append(c);
      lastWasSpace = false;
    }

    return sb.toString().trim();
  }
}
