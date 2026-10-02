#!/usr/bin/env python3
"""Capture Accountant screens into a private temporary folder without changing expenses.

Usage: python3 scripts/capture_android_screens.py [--serial SERIAL] [--output /tmp/path]
Requires an unlocked, signed-in phone with USB debugging. Does not enter credentials,
confirm reviews, save expenses, change settings, or capture the notification shade.
"""
import argparse
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import time
import xml.etree.ElementTree as ET

PACKAGE = "com.shrutsureja.accountant"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    adb = shutil.which("adb") or str(Path.home() / "Android/Sdk/platform-tools/adb")
    base = [adb] + (["-s", args.serial] if args.serial else [])

    def run(*command):
        return subprocess.run(base + list(command), check=True, capture_output=True, timeout=30).stdout

    devices = [line.split()[0] for line in run("devices").decode().splitlines()[1:] if "\tdevice" in line]
    if (not args.serial and len(devices) != 1) or (args.serial and args.serial not in devices):
        raise RuntimeError("Connect one authorized phone or choose it using --serial.")
    output = args.output or Path(tempfile.mkdtemp(prefix="accountant-screens-"))
    output.mkdir(parents=True, exist_ok=True, mode=0o700)
    os.chmod(output, 0o700)
    results = []

    def tree():
        focus = run("shell", "dumpsys", "window", "windows").decode()
        if not any(PACKAGE in line for line in focus.splitlines() if "mCurrentFocus=" in line):
            raise RuntimeError("Leave Accountant foreground, unlocked and signed in; capture stopped.")
        remote = "/sdcard/accountant-capture-window.xml"
        try:
            run("shell", "uiautomator", "dump", remote)
            root = ET.fromstring(run("shell", "cat", remote))
        finally:
            run("shell", "rm", "-f", remote)
        nodes = list(root.iter("node"))
        if any(n.get("password") == "true" or n.get("text") in ("Sign in", "Username", "PIN") for n in nodes):
            raise RuntimeError("Sign in manually before capturing; no authentication screenshot saved.")
        return nodes

    def tap(label, optional=False, bottom=False):
        nodes = [n for n in tree() if n.get("package") == PACKAGE and (
            label in (n.get("text"), n.get("content-desc")) or
            (label == "Needs review" and re.fullmatch(r"\d+ expenses? need review", n.get("text", "")))
        )]
        if not nodes:
            if optional:
                return False
            raise RuntimeError(f"Expected control {label!r} is not visible; stopped without guessing coordinates.")
        node = max(nodes, key=lambda n: int(re.findall(r"\d+", n.get("bounds", ""))[1])) if bottom else nodes[0]
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds", "")))
        run("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
        time.sleep(0.7)
        return True

    def capture(name):
        tree()  # Guard every screenshot against lock/login/external screens.
        path = output / f"{name}.png"
        path.write_bytes(run("exec-out", "screencap", "-p"))
        results.append(path.name)

    try:
        tap("Home", bottom=True)
        capture("01-home")
        if tap("Needs review", optional=True):
            capture("02-review")
            tap("Back")
        for label, name in [("Transactions", "03-transactions"), ("Add", "04-add"), ("Reports", "05-reports")]:
            tap(label, bottom=True)
            capture(name)
            if label == "Add" and tap("More", optional=True):
                capture("04-add-more-categories")
                run("shell", "input", "keyevent", "4")
                time.sleep(0.7)
        for label in ("Categories", "People"):
            tap(label)
            capture("06-reports-" + label.lower())
        tap("Profile", bottom=True)
        capture("07-profile")
        if tap("Categories", optional=True):
            capture("08-categories")
            tap("Back")
        tap("Home", bottom=True)
    finally:
        (output / "manifest.json").write_text(json.dumps({"screenshots": results}, indent=2))
        print(f"Saved {len(results)} screenshots in {output}")


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, subprocess.SubprocessError, OSError, ET.ParseError) as error:
        raise SystemExit(str(error))
