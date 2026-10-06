#!/usr/bin/env python3
import contextlib
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import upstream_sync as sync


@contextlib.contextmanager
def at(path):
    old = Path.cwd()
    os.chdir(path)
    try:
        yield
    finally:
        os.chdir(old)


class SyncTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.env = patch.dict(os.environ, {}, clear=False)
        self.env.start()
        self.upstream = self.root / 'upstream'
        sync.git('init', '-b', 'main', str(self.upstream))
        for key, value in [('user.name','Test'), ('user.email','test@example.invalid')]:
            sync.git('config', key, value, cwd=self.upstream)
        self.commit(self.upstream, 'base.txt', 'base')
        self.fork = self.root / 'fork'
        sync.git('clone', str(self.upstream), str(self.fork))
        self.output = self.root / 'output'

    def tearDown(self):
        self.env.stop()
        self.tmp.cleanup()

    def commit(self, repo, path, content):
        p = repo / path
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(content)
        sync.git('add', path, cwd=repo)
        sync.git('-c', 'user.name=Test', '-c', 'user.email=test@example.invalid',
                 'commit', '-m', path, cwd=repo)
        return sync.git('rev-parse', 'HEAD', cwd=repo)

    def prepare(self, kind='app', **kwargs):
        with at(self.fork):
            return sync.prepare(kind, self.output, upstream=str(self.upstream), **kwargs)

    def test_no_update_produces_no_bundle(self):
        self.assertFalse(self.prepare()['changed'])
        self.assertFalse((self.output / 'candidate.bundle').exists())

    def test_merge_keeps_fork_and_bundle_exact(self):
        base = self.commit(self.fork, 'custom.txt', 'fork')
        incoming = self.commit(self.upstream, 'new.txt', 'upstream')
        plan = self.prepare()
        self.assertTrue(plan['changed'])
        self.assertTrue(sync.ancestor(incoming, plan['candidate'], cwd=self.fork))
        self.assertEqual((self.fork / 'custom.txt').read_text(), 'fork')
        receiver = self.root / 'receiver'
        sync.git('clone', str(self.fork), str(receiver))
        sync.git('bundle', 'verify', str(self.output / 'candidate.bundle'), cwd=receiver)
        self.assertEqual(plan['base'], base)

    def test_conflict_stops_without_candidate(self):
        self.commit(self.fork, 'base.txt', 'fork')
        self.commit(self.upstream, 'base.txt', 'upstream')
        with self.assertRaises(subprocess.CalledProcessError):
            self.prepare()
        self.assertFalse((self.output / 'candidate.bundle').exists())

    def test_upstream_cannot_replace_automation(self):
        self.commit(self.upstream, '.github/workflows/unsafe.yml', 'unsafe')
        with self.assertRaisesRegex(RuntimeError, 'automation'):
            self.prepare()

    def test_candidate_is_deterministic_on_retry(self):
        base = self.commit(self.fork, 'custom.txt', 'fork')
        self.commit(self.upstream, 'new.txt', 'upstream')
        first = self.prepare()['candidate']
        sync.git('reset', '--hard', base, cwd=self.fork)
        second = self.prepare()['candidate']
        self.assertEqual(first, second)

    def main_fixture(self):
        app = self.root / 'app'
        sync.git('init', '-b', 'main', str(app))
        app_base = self.commit(app, 'app.txt', 'app')
        self.commit(self.upstream, '.gitmodules',
                    '[submodule "app"]\n path = app\n url = ' + sync.APP_URL + '\n')
        sync.git('update-index', '--add', '--cacheinfo', f'160000,{app_base},app', cwd=self.upstream)
        sync.git('commit', '-m', 'app gitlink', cwd=self.upstream)
        sync.git('pull', '--ff-only', cwd=self.fork)
        return app

    def test_main_updates_gitlink_to_app_main(self):
        app = self.main_fixture()
        latest = self.commit(app, 'new.txt', 'new app')
        plan = self.prepare('main', app_url=str(app), app_upstream=str(app))
        self.assertEqual(plan['app'], latest)
        self.assertEqual(sync.git('rev-parse', 'HEAD:app', cwd=self.fork), latest)
        self.assertFalse(self.prepare('main', app_url=str(app), app_upstream=str(app))['changed'])

    def test_main_stops_until_app_contains_upstream(self):
        app = self.main_fixture()
        newer = self.root / 'newer-app'
        sync.git('clone', str(app), str(newer))
        self.commit(newer, 'new.txt', 'newer app upstream')
        with self.assertRaisesRegex(RuntimeError, 'handle app sync first'):
            self.prepare('main', app_url=str(app), app_upstream=str(newer))

    def test_publish_refuses_moved_main_before_push(self):
        self.commit(self.fork, 'custom.txt', 'fork')
        self.commit(self.upstream, 'new.txt', 'upstream')
        self.prepare()
        self.commit(self.upstream, 'concurrent.txt', 'someone else')
        with at(self.fork), patch.dict(os.environ, {'GITHUB_REPOSITORY':sync.REPOS['app']}):
            with self.assertRaisesRegex(RuntimeError, 'Main moved'):
                sync.publish(self.output)
        self.assertFalse(sync.git('ls-remote', 'origin', 'refs/heads/sync/*', cwd=self.fork))


if __name__ == '__main__':
    unittest.main()
