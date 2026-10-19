"""Exercise release retries/promotion without contacting GitHub."""

import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "scripts" / "publish-dev-release.sh"
SHA = "a" * 40


class PublishDevReleaseTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.apk = self.root / "app-debug.apk"
        self.apk.write_bytes(b"test APK")
        self.calls_file = self.root / "calls.jsonl"
        gh = self.root / "gh"
        gh.write_text(
            "#!/usr/bin/env python3\n"
            "import json, os, sys\n"
            "args = sys.argv[1:]\n"
            "with open(os.environ['GH_CALLS'], 'a') as log:\n"
            "    log.write(json.dumps(args) + '\\n')\n"
            "if args[:2] == ['release', 'view']:\n"
            "    release = os.environ['GH_VIEW_RESULT']\n"
            "    if release == 'null':\n"
            "        sys.exit(1)\n"
            "    print(release)\n"
            "elif args[1] == os.environ.get('GH_FAIL_COMMAND'):\n"
            "    sys.exit(1)\n"
        )
        gh.chmod(0o755)

    def run_publisher(self, release=None, fail_command=""):
        env = os.environ.copy()
        env.update(
            PATH=f"{self.root}{os.pathsep}{env['PATH']}",
            GITHUB_RUN_NUMBER="143",
            GITHUB_SHA=SHA,
            GH_CALLS=str(self.calls_file),
            GH_VIEW_RESULT=json.dumps(release),
            GH_FAIL_COMMAND=fail_command,
        )
        result = subprocess.run(
            ["bash", str(SCRIPT), str(self.apk)],
            env=env,
            capture_output=True,
            text=True,
        )
        calls = (
            [json.loads(line) for line in self.calls_file.read_text().splitlines()]
            if self.calls_file.exists()
            else []
        )
        return result, calls

    def release(self, *, draft=False, prerelease=True, sha=SHA, assets=True):
        return {
            "isDraft": draft,
            "isPrerelease": prerelease,
            "targetCommitish": sha,
            "assets": [{"name": "app-debug.apk", "size": 123}] if assets else [],
        }

    def test_new_release_is_uploaded_as_draft_before_publishing(self):
        result, calls = self.run_publisher()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual([call[1] for call in calls], ["view", "create", "upload", "edit"])
        self.assertTrue(all(call[2] == "dev-143" for call in calls))
        self.assertIn("--draft", calls[1])
        self.assertIn("--prerelease", calls[1])
        self.assertEqual(calls[1][calls[1].index("--target") + 1], SHA)
        self.assertEqual(calls[2][3], str(self.apk))
        self.assertIn("--draft=false", calls[3])
        self.assertIn("--prerelease", calls[3])
        self.assertIn("--latest=false", calls[3])

    def test_retry_resumes_unpublished_draft(self):
        result, calls = self.run_publisher(self.release(draft=True, assets=False))
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual([call[1] for call in calls], ["view", "upload", "edit"])
        self.assertIn("--clobber", calls[1])

    def test_published_prerelease_and_promoted_release_are_unchanged(self):
        for prerelease in (True, False):
            with self.subTest(prerelease=prerelease):
                self.calls_file.unlink(missing_ok=True)
                result, calls = self.run_publisher(self.release(prerelease=prerelease))
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertEqual([call[1] for call in calls], ["view"])

    def test_commit_collision_is_rejected(self):
        for draft in (True, False):
            with self.subTest(draft=draft):
                self.calls_file.unlink(missing_ok=True)
                result, calls = self.run_publisher(self.release(draft=draft, sha="b" * 40))
                self.assertNotEqual(result.returncode, 0)
                self.assertIn("different commit", result.stdout)
                self.assertEqual([call[1] for call in calls], ["view"])

    def test_incomplete_published_release_is_not_modified(self):
        for assets in ([], [{"name": "app-debug.apk", "size": 0}]):
            with self.subTest(assets=assets):
                self.calls_file.unlink(missing_ok=True)
                release = self.release()
                release["assets"] = assets
                result, calls = self.run_publisher(release)
                self.assertNotEqual(result.returncode, 0)
                self.assertIn("missing its APK", result.stdout)
                self.assertEqual([call[1] for call in calls], ["view"])

    def test_failed_creation_does_not_upload(self):
        result, calls = self.run_publisher(fail_command="create")
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual([call[1] for call in calls], ["view", "create"])

    def test_failed_upload_does_not_publish(self):
        result, calls = self.run_publisher(fail_command="upload")
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual([call[1] for call in calls], ["view", "create", "upload"])

    def test_empty_apk_is_rejected_without_contacting_github(self):
        self.apk.write_bytes(b"")
        result, calls = self.run_publisher()
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(calls, [])


if __name__ == "__main__":
    unittest.main()
