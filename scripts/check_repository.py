#!/usr/bin/env python3
"""Check repository hygiene without pretending to compile Scala.

Run from any directory: python3 path/to/scripts/check_repository.py
Python 3.10+; no third-party packages or network access required.
"""
from __future__ import annotations

import json
import re
import subprocess
import sys
from pathlib import Path
from urllib.parse import unquote, urlparse

ROOT = Path(__file__).resolve().parents[1]
REQUIRED = (
    "build.sbt", "project/build.properties", "project/plugins.sbt",
    "project/ProjectInfo.scala", "README.md", "LICENSE", "VERIFICATION.md",
    "RELEASING.md", "CONTRIBUTING.md", "SECURITY.md", "CHANGELOG.md",
    "docs/design.md", "docs/dependencies.md", "docs/reference.md", "docs/conditionals.md",
    "docs/api-inventory.json", "docs/src/main/mdoc/index.md",
    "flake.nix", "flake.lock",
    ".github/workflows/ci.yml",
)


def main() -> int:
    problems: list[str] = []
    for relative in REQUIRED:
        if not (ROOT / relative).is_file():
            problems.append(f"Missing required file: {relative}")

    inventory_result = subprocess.run(
        [sys.executable, str(ROOT / "scripts/api_inventory.py"), "--check"],
        cwd=ROOT, check=False,
    )
    if inventory_result.returncode != 0:
        problems.append("API inventory is missing or stale")

    # This pattern covers this repository's ordinary inline Markdown links.
    # It deliberately does not claim to validate remote links or anchors.
    link_pattern = re.compile(r"(?<!!)\[[^\]\n]+\]\(([^\s)]+)(?:\s+[^)]*)?\)")
    checked_links = 0
    for document in sorted(ROOT.rglob("*.md")):
        if any(part in {"target", ".git", ".direnv", ".bloop", ".metals"}
               for part in document.relative_to(ROOT).parts):
            continue
        content = document.read_text(encoding="utf-8")
        # Ignore code: Scala's f[F](value) is not a Markdown link.
        content = re.sub(r"(?ms)^```[^\n]*\n.*?^```[ \t]*(?:\n|$)", "", content)
        content = re.sub(r"`[^`\n]*`", "", content)
        for raw in link_pattern.findall(content):
            parsed = urlparse(raw)
            if parsed.scheme or parsed.netloc or not parsed.path:
                continue
            target = (document.parent / unquote(parsed.path)).resolve()
            # These pages are staged by docs/siteReference before Laika renders.
            if document.is_relative_to(ROOT / "docs/src/main/mdoc") and not target.exists():
                staged = {"reference.md": ROOT / "docs/reference.md",
                          "design.md": ROOT / "docs/design.md"}
                target = staged.get(parsed.path, target)
            checked_links += 1
            if not target.exists():
                problems.append(f"Broken local link in {document.relative_to(ROOT)}: {raw}")

    runtime = [p for m in ("core", "effect", "fs2", "all")
               for dialect in ("scala", "scala-2", "scala-3")
               for p in (ROOT / "modules" / m / "src/main" / dialect).rglob("*.scala")]
    forbidden = ("???", "asInstanceOf", "unsafeRun", "Await.result")
    for source in runtime:
        text = source.read_text(encoding="utf-8")
        for marker in forbidden:
            if marker in text:
                problems.append(f"Forbidden runtime marker {marker!r}: {source.relative_to(ROOT)}")
        if not text.endswith("\n"):
            problems.append(f"Missing final newline: {source.relative_to(ROOT)}")
        if "Copyright 2026 pspsps contributors" not in text:
            problems.append(f"Missing copyright header: {source.relative_to(ROOT)}")

    test_files = sorted((ROOT / "tests/src/test").rglob("*Suite.scala"))
    test_source = "\n".join(p.read_text(encoding="utf-8") for p in test_files)
    explicit_tests = len(re.findall(r'^\s*(?:test|property)\("', test_source, re.MULTILINE))
    rulesets = len(re.findall(r'^\s*checkAll\(', test_source, re.MULTILINE))
    inventory_path = ROOT / "docs/api-inventory.json"
    if inventory_path.exists():
        try:
            data = json.loads(inventory_path.read_text(encoding="utf-8"))
            names = {entry["name"] for entry in data["extensions"]}
            used = set(re.findall(r"\b(as\w+|nullable)\b", test_source))
            missing = sorted(names - used)
            if missing:
                problems.append("Method names absent from test source: " + ", ".join(missing))
            print(f"Inventory: {data['declaration_count']} declarations, {len(names)} distinct names.")
        except (ValueError, KeyError, TypeError) as error:
            problems.append(f"Malformed API inventory: {error}")
    print(f"Checked {len(runtime)} runtime Scala files and {checked_links} local Markdown links.")
    print(f"Authored: {explicit_tests} explicit tests/properties in {len(test_files)} suites, "
          f"plus {rulesets} Discipline ruleset registrations.")
    print("Textual method-name presence is not test coverage or a passing test result.")
    if problems:
        for problem in problems:
            print("ERROR: " + problem, file=sys.stderr)
        return 1
    print("Repository hygiene checks passed. Scala compilation and tests are separate: sbt verify")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
