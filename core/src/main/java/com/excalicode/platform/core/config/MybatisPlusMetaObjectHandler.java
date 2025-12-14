package com.excalicode.platform.core.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/** MyBatis-Plus 字段自动填充处理器 自动填充 createdTime 和 updatedTime 字段 */
@Component
public class MybatisPlusMetaObjectHandler implements MetaObjectHandler {

  /** 插入时自动填充 */
  @Override
  public void insertFill(MetaObject metaObject) {
    LocalDateTime now = LocalDateTime.now();
    this.strictInsertFill(metaObject, "createdTime", LocalDateTime.class, now);
    this.strictInsertFill(metaObject, "updatedTime", LocalDateTime.class, now);
  }

  /** 更新时自动填充 */
  @Override
  public void updateFill(MetaObject metaObject) {
    this.strictUpdateFill(metaObject, "updatedTime", LocalDateTime.class, LocalDateTime.now());
  }
}
