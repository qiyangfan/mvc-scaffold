#!/usr/bin/env node
// 依赖许可证门禁:读取 CycloneDX BOM,任一组件无白名单许可证即失败。
// 双许可组件按「任一许可证在白名单内即通过」处理。

import { readFileSync } from "node:fs";

const ALLOWED = new Set([
  "0BSD",
  "Apache-2.0",
  "BSD-2-Clause",
  "BSD-3-Clause",
  "BSD-4-Clause",
  "CC0-1.0",
  "EPL-1.0",
  "EPL-2.0",
  "GPL-2.0-with-classpath-exception",
  // MySQL Connector/J: GPL-2.0 + Universal FOSS Exception(Oracle)。
  // 种子保留 MySQL 运行时驱动;对外分发形态变化时须复核。
  // 负责人:Solvela;到期日:2027-06-30。
  "GPL-2.0-with-universal-foss-exception",
  "ISC",
  "LGPL-2.1-only",
  "LGPL-2.1-or-later",
  "MIT",
  "MPL-2.0",
  "Unlicense",
]);

const NORMALIZE = new Map([
  ["The Apache Software License, Version 2.0", "Apache-2.0"],
  ["Apache License, Version 2.0", "Apache-2.0"],
  ["Apache License 2.0", "Apache-2.0"],
  ["MIT License", "MIT"],
  ["The MIT License", "MIT"],
  ["Eclipse Public License - v 1.0", "EPL-1.0"],
  ["Eclipse Public License v1.0", "EPL-1.0"],
  ["Eclipse Public License 1.0", "EPL-1.0"],
  ["EPL 1.0", "EPL-1.0"],
  ["Eclipse Public License v2.0", "EPL-2.0"],
  ["Eclipse Public License 2.0", "EPL-2.0"],
  ["GPL2 w/ CPE", "GPL-2.0-with-classpath-exception"],
  [
    "The GNU General Public License, v2 with Universal FOSS Exception, v1.0",
    "GPL-2.0-with-universal-foss-exception",
  ],
  ["CC0 1.0 Universal", "CC0-1.0"],
]);

function normalize(id) {
  const trimmed = id.trim();
  return NORMALIZE.get(trimmed) ?? trimmed;
}

function readJson(path) {
  try {
    return JSON.parse(readFileSync(path, "utf8"));
  } catch (error) {
    console.error(`[license-guard] 无法读取 ${path}: ${error.message}`);
    process.exit(2);
  }
}

function componentLicenses(component) {
  return (component.licenses ?? [])
    .map((entry) => entry.expression ?? entry.license?.id ?? entry.license?.name)
    .filter(Boolean)
    .map(normalize);
}

const [bomPath] = process.argv.slice(2);
if (!bomPath) {
  console.error("用法: license-guard.mjs <target/bom.json>");
  process.exit(2);
}

// 空/残缺输入必须判红,否则生成步骤失败会伪装成「全部合规」。
const MIN_COMPONENTS = 50;

const bom = readJson(bomPath);
if (!Array.isArray(bom.components)) {
  console.error(`[license-guard] ${bomPath} 缺少 components 数组,无法判定`);
  process.exit(2);
}

const failures = [];
let count = 0;

for (const component of bom.components) {
  count += 1;
  const licenses = componentLicenses(component);
  const label = `${component.group ? `${component.group}:` : ""}${component.name}@${component.version}`;
  if (licenses.length === 0) {
    failures.push(`${label}: 未声明许可证`);
  } else if (!licenses.some((id) => ALLOWED.has(id))) {
    failures.push(`${label}: ${licenses.join(", ")}`);
  }
}

if (count < MIN_COMPONENTS) {
  failures.push(`组件清单疑似残缺: ${count} 个(下限 ${MIN_COMPONENTS})`);
}

if (failures.length > 0) {
  console.error(`[license-guard] 发现 ${failures.length} 个不在白名单的依赖许可证:`);
  failures.forEach((failure) => console.error(`  - ${failure}`));
  process.exit(1);
}

console.log(`[license-guard] 通过: ${count} 个组件,许可证均在白名单内`);
