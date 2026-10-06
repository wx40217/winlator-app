#!/usr/bin/env python3
"""Prepare a tested sync candidate, or publish it using only this repo's token."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import urllib.request

REPOS = {'app': 'wx40217/winlator-app', 'main': 'wx40217/winlator'}
UPSTREAM = {'app': 'https://github.com/brunodev85/winlator-app.git',
            'main': 'https://github.com/brunodev85/winlator.git'}
APP_URL = 'https://github.com/wx40217/winlator-app.git'


def git(*args, cwd=None):
    return subprocess.check_output(['git', *args], cwd=cwd, text=True).strip()


def ancestor(old, new, cwd=None):
    result = subprocess.run(['git', 'merge-base', '--is-ancestor', old, new], cwd=cwd)
    if result.returncode not in (0, 1):
        raise RuntimeError('Cannot determine commit ancestry')
    return result.returncode == 0


def prepare(kind, output, upstream=None, app_url=APP_URL, app_upstream=None):
    output = Path(output).resolve()
    output.mkdir(parents=True, exist_ok=True)
    base = git('rev-parse', 'HEAD')
    git('config', 'user.name', 'github-actions[bot]')
    git('config', 'user.email', '41898282+github-actions[bot]@users.noreply.github.com')
    git('fetch', '--no-tags', upstream or UPSTREAM[kind], 'main')
    incoming = git('rev-parse', 'FETCH_HEAD')
    # Stable commit dates make reruns reuse the same branch/PR after a transient failure.
    stamp = max(int(git('show', '-s', '--format=%ct', base)),
                int(git('show', '-s', '--format=%ct', incoming))) + 1
    os.environ['GIT_AUTHOR_DATE'] = os.environ['GIT_COMMITTER_DATE'] = f'@{stamp} +0000'
    app_sha = None
    if kind == 'main':
        # Public cross-repo reads do not require a cross-repo credential.
        app_repo = output / 'app-probe'
        git('init', str(app_repo))
        git('fetch', '--no-tags', app_url, 'main', cwd=app_repo)
        app_sha = git('rev-parse', 'FETCH_HEAD', cwd=app_repo)
        git('fetch', '--no-tags', app_upstream or UPSTREAM['app'], 'main', cwd=app_repo)
        latest = git('rev-parse', 'FETCH_HEAD', cwd=app_repo)
        if not ancestor(latest, app_sha, cwd=app_repo):
            raise RuntimeError('App fork has not incorporated latest upstream; handle app sync first')
    gitlink = git('rev-parse', 'HEAD:app') if kind == 'main' else None
    changed = not ancestor(incoming, base) or (kind == 'main' and gitlink != app_sha)
    if not changed:
        plan = {'changed': False, 'base': base, 'upstream': incoming, 'kind': kind}
    else:
        # A merge conflict exits nonzero; no strategy silently discards fork work.
        git('merge', '--no-edit', incoming)
        if kind == 'main':
            git('update-index', '--cacheinfo', f'160000,{app_sha},app')
            if subprocess.run(['git', 'diff', '--cached', '--quiet']).returncode:
                git('commit', '-m', f'Sync tested app fork {app_sha}')
            if git('config', '-f', '.gitmodules', 'submodule.app.url') != APP_URL:
                raise RuntimeError('Fork app URL changed: human review required')
        # Upstream cannot automatically replace the trusted automation itself.
        protected = git('diff', '--name-only', base, 'HEAD', '--', '.github/workflows',
                        'scripts/upstream_sync.py', 'scripts/verify_sync.py',
                        'scripts/test_upstream_sync.py', 'docs/upstream-sync.md')
        if protected:
            raise RuntimeError('Upstream changes automation; human review required: ' + protected)
        candidate = git('rev-parse', 'HEAD')
        git('update-ref', 'refs/heads/sync-candidate', candidate)
        git('bundle', 'create', str(output / 'candidate.bundle'),
            f'{base}..refs/heads/sync-candidate')
        plan = {'changed': True, 'base': base, 'candidate': candidate,
                'upstream': incoming, 'app': app_sha, 'kind': kind}
    (output / 'plan.json').write_text(json.dumps(plan, indent=2) + '\n')
    if os.environ.get('GITHUB_OUTPUT'):
        with open(os.environ['GITHUB_OUTPUT'], 'a') as f:
            f.write('changed=' + str(changed).lower() + '\n')
    print(json.dumps(plan))
    return plan


def api(path, method='GET', body=None):
    request = urllib.request.Request('https://api.github.com/' + path, method=method,
        data=json.dumps(body).encode() if body is not None else None,
        headers={'Authorization': 'Bearer ' + os.environ['GH_TOKEN'],
                 'Accept': 'application/vnd.github+json',
                 'X-GitHub-Api-Version': '2022-11-28', 'Content-Type': 'application/json'})
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.load(response)


def publish(directory):
    plan = json.loads((Path(directory) / 'plan.json').read_text())
    if not plan['changed']:
        return
    repo = REPOS[plan['kind']]
    if os.environ['GITHUB_REPOSITORY'] != repo:
        raise RuntimeError('Wrong repository')
    base, candidate = plan['base'], plan['candidate']
    git('fetch', '--no-tags', 'origin', 'main')
    git('fetch', str(Path(directory).resolve() / 'candidate.bundle'),
        'refs/heads/sync-candidate:refs/heads/sync-candidate')
    if git('rev-parse', 'sync-candidate') != candidate or not ancestor(base, candidate):
        raise RuntimeError('Candidate identity or ancestry mismatch')
    if ancestor(candidate, 'origin/main'):
        print('Candidate already merged; nothing to publish')
        return
    if git('rev-parse', 'origin/main') != base:
        raise RuntimeError('Main moved after validation; rerun from current main')
    branch = 'sync/upstream-' + candidate[:12]
    remote = git('ls-remote', 'origin', 'refs/heads/' + branch)
    if remote and remote.split()[0] != candidate:
        raise RuntimeError('Existing sync branch differs; refusing to overwrite')
    if not remote:
        git('push', 'origin', candidate + ':refs/heads/' + branch)
    pulls = api(f'repos/{repo}/pulls?state=open&head=wx40217:{branch}&base=main')
    body = ('Upstream sync validated in this workflow, including debug APK and 10 unit tests.\n\n'
            f'Base: `{base}`\nCandidate: `{candidate}`\nUpstream: `{plan["upstream"]}`\n'
            f'App gitlink: `{plan.get("app")}`\n\n'
            f'Run: https://github.com/{repo}/actions/runs/{os.environ["GITHUB_RUN_ID"]}\n'
            'No release or APK is published. Validation does not rely on PR-triggered checks.')
    pr = pulls[0] if pulls else api(f'repos/{repo}/pulls', 'POST', {
        'title': 'Sync upstream after debug build and unit tests',
        'head': branch, 'base': 'main', 'body': body})
    git('fetch', '--no-tags', 'origin', 'main')
    if git('rev-parse', 'origin/main') != base:
        raise RuntimeError('Main moved before merge; leaving PR for takeover')
    result = api(f'repos/{repo}/pulls/{pr["number"]}/merge', 'PUT',
                 {'sha': candidate, 'merge_method': 'merge'})
    if not result.get('merged'):
        raise RuntimeError('PR did not merge: ' + result.get('message', 'unknown'))
    git('fetch', '--no-tags', 'origin', 'main')
    if not ancestor(result['sha'], 'origin/main') or not ancestor(candidate, 'origin/main'):
        raise RuntimeError('Remote main did not contain the reported merge and tested candidate')
    if git('rev-parse', result['sha'] + '^{tree}') != git('rev-parse', candidate + '^{tree}'):
        raise RuntimeError('Merged tree differs from tested candidate; concurrent main change needs takeover')
    print(f'Merged {pr["html_url"]} as {result["sha"]}; remote verified')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('operation', choices=['prepare', 'publish'])
    parser.add_argument('--kind', choices=REPOS)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()
    if args.operation == 'prepare':
        if not args.kind:
            parser.error('--kind is required for prepare')
        prepare(args.kind, args.output)
    else:
        publish(args.output)
