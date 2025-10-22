/** @type {import("prettier").Config} */
module.exports = {
  // 每行最大长度，建议 80 更利于代码审查
  printWidth: 80,

  // 缩进 2 空格
  tabWidth: 2,

  // 不使用 tab，保持空格缩进
  useTabs: false,

  // 语句结尾加分号（更标准，避免 ASI 问题）
  semi: true,

  // 使用单引号
  singleQuote: true,

  // 对象和数组末尾加逗号（更利于 git diff）
  trailingComma: 'all',

  // 大括号内部是否加空格 { a: 1 }
  bracketSpacing: true,

  // JSX 标签的闭合 > 是否换行
  bracketSameLine: false,

  // 箭头函数参数总是加括号 (x) => {}
  arrowParens: 'always',

  // 保持换行符与当前环境一致（避免 Windows 上被强制改成 LF）
  endOfLine: 'auto',

  // Vue 文件中 <script> 和 <style> 是否缩进
  vueIndentScriptAndStyle: false,

  // 嵌套对象或数组过长时尽量自动格式化成多行
  proseWrap: 'preserve',
};
