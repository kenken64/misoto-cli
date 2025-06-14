# package.json

**File Path:** `G:\Projects\claude-code-source-code-deobfuscation\claude-code\package.json`  
**Language:** json  
**Generated:** 2025-06-14 14:32:09

---

```json
{
  "name": "claude-code",
  "version": "0.1.0",
  "description": "Claude Code CLI - Your AI coding assistant in the terminal",
  "main": "dist/src/cli.js",
  "type": "module",
  "bin": {
    "claude-code": "dist/src/cli.js"
  },
  "scripts": {
    "build": "tsc",
    "start": "node dist/src/cli.js",
    "dev": "ts-node --esm src/cli.ts",
    "test": "jest",
    "lint": "eslint src",
    "clean": "rm -rf dist"
  },
  "keywords": [
    "claude",
    "ai",
    "code",
    "cli",
    "assistant",
    "anthropic"
  ],
  "author": "Anthropic",
  "license": "MIT",
  "dependencies": {
    "node-fetch": "^3.3.1",
    "open": "^9.1.0"
  },
  "devDependencies": {
    "@types/node": "^20.4.7",
    "typescript": "^5.1.6",
    "ts-node": "^10.9.1",
    "eslint": "^8.46.0",
    "@typescript-eslint/eslint-plugin": "^6.2.1",
    "@typescript-eslint/parser": "^6.2.1"
  },
  "engines": {
    "node": ">=18.0.0"
  }
} 
```

---

*This markdown file was automatically generated from the source code file.*
