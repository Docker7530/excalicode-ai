/**
 * API 模块统一导出
 * 提供所有 API 服务的统一入口
 */

// 导入基础请求工具
import api, { request } from './request.js';

// 导入具体业务服务
import * as authService from './auth.js';
import cosmicService from './cosmic.js';
import * as requirementKnowledgeService from './requirementKnowledge.js';
import * as userService from './user.js';
import * as taskService from './task.js';

/**
 * 统一的 API 服务对象
 * 包含所有业务模块的API服务
 */
const apiServices = Object.freeze({
  // 基础HTTP请求工具
  http: api,

  // COSMIC分析服务
  cosmic: cosmicService,

  // 系统认证服务
  auth: authService,

  // 系统用户管理服务
  user: userService,

  // 任务管理服务
  task: taskService,

  // 需求知识库服务
  requirementKnowledge: requirementKnowledgeService,
});

// 默认导出统一API服务
export default apiServices;

// 分别导出各个服务，便于按需导入
export {
  api,
  apiServices,
  authService,
  cosmicService,
  requirementKnowledgeService,
  request,
  taskService,
  userService,
};
