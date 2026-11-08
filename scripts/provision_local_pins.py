#!/usr/bin/env python3
"""Set local development PINs after applying D1 migrations. Never targets remote D1."""

import base64
import getpass
import hashlib
import pathlib
import secrets
import subprocess
import tempfile


ROOT = pathlib.Path(__file__).resolve().parents[1]
BACKEND = ROOT / "backend"


def main() -> None:
    pin = getpass.getpass("Local development PIN (4–8 digits): ")
    if not (pin.isascii() and pin.isdigit() and 4 <= len(pin) <= 8):
        raise SystemExit("PIN must contain 4–8 ASCII digits")
    if getpass.getpass("Repeat PIN: ") != pin:
        raise SystemExit("PINs do not match")

    salt = secrets.token_bytes(16)
    digest = hashlib.pbkdf2_hmac("sha256", pin.encode(), salt, 100_000)
    encoded = "pbkdf2:100000:" + base64.b64encode(salt).decode() + ":" + base64.b64encode(digest).decode()
    sql = (
        "UPDATE users SET pin_hash = '" + encoded + "', updated_at = datetime('now') "
        "WHERE id IN ('user-shrut', 'user-mom', 'user-dad');\n"
    )
    with tempfile.TemporaryDirectory(prefix="accountant-local-pin-") as temporary:
        script = pathlib.Path(temporary) / "pins.sql"
        script.write_text(sql)
        script.chmod(0o600)
        subprocess.run(
            ["npx", "wrangler", "d1", "execute", "accountant-db", "--local", "--file", str(script)],
            cwd=BACKEND,
            check=True,
        )
    print("Local development PIN updated.")


if __name__ == "__main__":
    main()
