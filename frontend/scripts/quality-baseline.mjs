// Legacy TypeScript/ESLint debt ratchet. Existing errors do not fail the check;
// any increase does. Lower the baselines when debt is paid down.
import { spawnSync } from "node:child_process";
import { appendFileSync } from "node:fs";

const BASELINE = { typescript: 616, eslint: 451 };

function run(command, args) {
  return spawnSync(command, args, { encoding: "utf8", maxBuffer: 64 * 1024 * 1024 });
}

const tsc = run("npx", ["tsc", "-p", "tsconfig.app.json", "--noEmit"]);
const typescriptErrors = (tsc.stdout.match(/error TS\d+/g) ?? []).length;

const eslint = run("npx", ["eslint", ".", "--format", "json"]);
const report = JSON.parse(eslint.stdout);
const eslintErrors = report.reduce((sum, file) => sum + file.errorCount, 0);
const eslintWarnings = report.reduce((sum, file) => sum + file.warningCount, 0);

const lines = [
  `TypeScript errors: ${typescriptErrors} (baseline ${BASELINE.typescript})`,
  `ESLint errors: ${eslintErrors} (baseline ${BASELINE.eslint}), warnings: ${eslintWarnings}`,
];
console.log(lines.join("\n"));
if (process.env.GITHUB_STEP_SUMMARY) {
  appendFileSync(process.env.GITHUB_STEP_SUMMARY, lines.join("\n\n") + "\n");
}

let failed = false;
for (const [name, count] of [["typescript", typescriptErrors], ["eslint", eslintErrors]]) {
  if (count > BASELINE[name]) {
    console.error(`${name} errors increased from ${BASELINE[name]} to ${count}`);
    failed = true;
  } else if (count < BASELINE[name]) {
    console.log(`${name} errors dropped to ${count}; lower the baseline in scripts/quality-baseline.mjs`);
  }
}
process.exit(failed ? 1 : 0);
