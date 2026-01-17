# Classic Pipes (Fabric 1.21.10) - Agent Instructions

## Goal
Fix Classic Pipes item list search so it works with CJK input (Chinese/Japanese) and still works for Latin languages.

## Project notes
- MultiLoader layout: common/ + fabric/ (+ forge/neoforge). Prefer implementing logic in common/ so all loaders benefit.
- Target: Minecraft 1.21.10 Fabric.

## Build commands
- Primary: `./gradlew --no-daemon :fabric:build -x test`
- If needed: `./gradlew --no-daemon build -x test`

## What to avoid
- Don’t change gameplay mechanics.
- Keep changes localized to search / UI filtering logic.
- Don’t add heavy dependencies.
