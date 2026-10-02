#!/usr/bin/env python3
"""Build a versioned, locally signed Accountant APK. Nothing is uploaded.
Example: python3 scripts/build_apk.py --version 1.1.0 --code 2 --api-url https://api.accountant.shrutsureja.com/
Use --debug for local HTTP testing; debug APKs cannot replace release APKs.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import subprocess
from urllib.parse import urlparse


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--version', required=True, help='Display version, e.g. 1.1.0')
    parser.add_argument('--code', required=True, type=int, help='Increasing Android build number, e.g. 2')
    parser.add_argument('--api-url', required=True, help='Reachable backend base URL')
    parser.add_argument('--debug', action='store_true', help='Local test APK with debug signing and HTTP support')
    args = parser.parse_args()
    if not re.fullmatch(r'\d+\.\d+\.\d+(?:-[a-zA-Z0-9.-]+)?', args.version) or not 1 <= args.code <= 2100000000:
        parser.error('Use a version such as 1.1.0 and a positive build code <= 2100000000.')
    url = args.api_url.rstrip('/') + '/'
    parsed = urlparse(url)
    if not re.fullmatch(r'[A-Za-z0-9:/._~-]+', url) or not parsed.hostname or parsed.username or parsed.password or parsed.query or parsed.fragment or parsed.scheme not in ('https', 'http'):
        parser.error('Use an HTTP(S) base URL without credentials, query strings, or fragments.')
    if not args.debug and parsed.scheme != 'https':
        parser.error('Family release builds require HTTPS; use --debug for local HTTP testing.')
    root = Path(__file__).resolve().parents[1]
    state_dir = Path(os.environ.get('ACCOUNTANT_BUILD_HOME', str(Path.home() / '.config/accountant-build'))).expanduser()
    state_dir.mkdir(mode=0o700, parents=True, exist_ok=True)
    os.chmod(state_dir, 0o700)
    variant = 'debug' if args.debug else 'release'
    record = state_dir / f'last-{variant}-build.json'
    previous = json.loads(record.read_text()) if record.exists() else {}
    if args.code < previous.get('code', 0) or (args.code == previous.get('code') and args.version != previous.get('version')):
        parser.error('Increase --code for a new version. Older builds cannot update installed APKs.')
    env = os.environ.copy()
    if not args.debug:
        key_file = state_dir / 'accountant-release.p12'
        password_file = state_dir / 'signing-password'
        if key_file.exists() != password_file.exists():
            parser.error('Signing key/password pair is incomplete. Restore your backup before building.')
        if not key_file.exists():
            keytool = shutil.which('keytool')
            if not keytool:
                parser.error('Install a JDK and make keytool available first.')
            password = secrets.token_urlsafe(36)
            env['ACCOUNTANT_STORE_PASSWORD'] = password
            try:
                subprocess.run([keytool, '-genkeypair', '-keystore', str(key_file), '-storetype', 'PKCS12', '-storepass:env', 'ACCOUNTANT_STORE_PASSWORD', '-keypass:env', 'ACCOUNTANT_STORE_PASSWORD', '-alias', 'accountant', '-keyalg', 'RSA', '-keysize', '3072', '-validity', '10000', '-dname', 'CN=Accountant Family', '-noprompt'], env=env, check=True)
                with password_file.open('x') as handle:
                    os.chmod(password_file, 0o600)
                    handle.write(password)
                os.chmod(key_file, 0o600)
            except BaseException:
                # Never silently replace a successfully created signing key.
                raise
            print(f'Created release signing files in {state_dir}. Back up this folder privately.', flush=True)
        env['ACCOUNTANT_KEYSTORE'] = str(key_file.resolve())
        env['ACCOUNTANT_STORE_PASSWORD'] = password_file.read_text().strip()
    task = 'assembleDebug' if args.debug else 'assembleRelease'
    subprocess.run([str(root / 'android/gradlew'), ':app:testDebugUnitTest', f':app:{task}', f'-PAPP_VERSION_NAME={args.version}', f'-PAPP_VERSION_CODE={args.code}', f'-PAPI_BASE_URL={url}', '-PLOCAL_AUTH_BYPASS=false'], cwd=root / 'android', env=env, check=True)
    source = root / f'android/app/build/outputs/apk/{variant}/app-{variant}.apk'
    if not source.exists():
        raise RuntimeError('Expected signed APK was not produced.')
    target_dir = root / 'dist'
    target_dir.mkdir(exist_ok=True)
    target = target_dir / f'Accountant-{args.version}-build-{args.code}-{variant}.apk'
    shutil.copy2(source, target)
    digest = hashlib.sha256(target.read_bytes()).hexdigest()
    target.with_suffix('.apk.sha256').write_text(f'{digest}  {target.name}\n')
    record.write_text(json.dumps({'version': args.version, 'code': args.code, 'apiUrl': url}, indent=2))
    print(f'\nAPK: {target}\nSHA-256: {digest}\nBackend: {url}\nNo files were uploaded.')


if __name__ == '__main__':
    try:
        main()
    except (OSError, ValueError, RuntimeError, subprocess.CalledProcessError) as error:
        raise SystemExit(str(error))
