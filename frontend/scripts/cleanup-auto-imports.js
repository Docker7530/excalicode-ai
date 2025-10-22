import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.resolve(__dirname, '..');
const autoImportsPath = path.join(projectRoot, 'auto-imports.d.ts');
const srcDir = path.join(projectRoot, 'src');

const supportedExtensions = new Set(['.js', '.jsx', '.ts', '.tsx', '.vue']);

function parseAutoImports(filePath) {
  const content = fs.readFileSync(filePath, 'utf-8');
  const moduleMap = new Map();
  const regex = /const\s+(\w+):\s+typeof import\('([^']+)'\)\['[^']+'\]/g;

  let match;
  while ((match = regex.exec(content)) !== null) {
    const [, name, moduleName] = match;
    if (!moduleMap.has(moduleName)) {
      moduleMap.set(moduleName, new Set());
    }
    moduleMap.get(moduleName).add(name);
  }

  return moduleMap;
}

function shouldProcess(filePath) {
  const ext = path.extname(filePath);
  return supportedExtensions.has(ext);
}

function cleanupImports(content, moduleMap) {
  const importRegex = /import\s+([\s\S]*?)\s+from\s+(['"])([^'";]+)\2;?/g;

  const updated = content.replace(
    importRegex,
    (full, specifiers, quote, source) => {
      const moduleSet = moduleMap.get(source);
      if (!moduleSet || moduleSet.size === 0) {
        return full;
      }

      const specTrimmed = specifiers.trim();

      if (/^type\s+/i.test(specTrimmed)) {
        return full;
      }

      if (!specTrimmed.startsWith('{') || !specTrimmed.endsWith('}')) {
        return full;
      }

      const inner = specTrimmed.slice(1, -1);
      const rawParts = inner.split(',');

      const keptParts = [];
      let removedCount = 0;

      for (const rawPart of rawParts) {
        const part = rawPart.trim();
        if (!part) continue;

        const aliasMatch = part.split(/\s+as\s+/i);
        if (aliasMatch.length > 1) {
          keptParts.push(part);
          continue;
        }

        const name = aliasMatch[0].trim();
        if (!name) {
          keptParts.push(part);
          continue;
        }

        if (moduleSet.has(name)) {
          removedCount += 1;
        } else {
          keptParts.push(part);
        }
      }

      if (removedCount === 0) {
        return full;
      }

      if (keptParts.length === 0) {
        return '';
      }

      const specJoined = keptParts.join(', ');
      return `import { ${specJoined} } from ${quote}${source}${quote};`;
    },
  );

  return updated;
}

function processFile(filePath, moduleMap) {
  const original = fs.readFileSync(filePath, 'utf-8');
  const cleaned = squeezeExtraBlankLines(
    removeLeadingBlankLines(cleanupImports(original, moduleMap)),
  );
  if (cleaned !== original) {
    fs.writeFileSync(filePath, cleaned, 'utf-8');
    return true;
  }
  return false;
}

function squeezeExtraBlankLines(content) {
  return content.replace(/(\r?\n){3,}/g, (match) => {
    const newline = match.includes('\r\n') ? '\r\n' : '\n';
    return newline.repeat(2);
  });
}

function removeLeadingBlankLines(content) {
  return content.replace(/^(?:\s*\r?\n)+/, '');
}

function walkDirectory(dirPath, moduleMap, changedFiles) {
  const entries = fs.readdirSync(dirPath, { withFileTypes: true });
  for (const entry of entries) {
    const fullPath = path.join(dirPath, entry.name);
    if (entry.isDirectory()) {
      walkDirectory(fullPath, moduleMap, changedFiles);
    } else if (entry.isFile() && shouldProcess(fullPath)) {
      if (processFile(fullPath, moduleMap)) {
        changedFiles.push(fullPath);
      }
    }
  }
}

function main() {
  if (!fs.existsSync(autoImportsPath)) {
    console.error(`未找到自动导入定义文件: ${autoImportsPath}`);
    process.exit(1);
  }

  const moduleMap = parseAutoImports(autoImportsPath);
  const changedFiles = [];
  walkDirectory(srcDir, moduleMap, changedFiles);

  console.log(`已处理 ${changedFiles.length} 个文件。`);
  if (changedFiles.length) {
    for (const file of changedFiles) {
      console.log(`  - ${path.relative(projectRoot, file)}`);
    }
  }
}

main();
