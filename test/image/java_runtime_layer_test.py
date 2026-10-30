"""Check the installable Java runtime layer's contract."""

from pathlib import PurePosixPath
import sys
import tarfile
import unittest


ARCHIVE = sys.argv.pop(1)


class JavaRuntimeLayerTest(unittest.TestCase):
    def test_runtime_layout_and_permissions(self):
        with tarfile.open(ARCHIVE) as archive:
            files = {}
            for member in archive:
                path = PurePosixPath(member.name)
                self.assertFalse(path.is_absolute(), member.name)
                self.assertNotIn("..", path.parts)
                if member.isfile():
                    self.assertTrue(str(path).startswith("opt/java/"), member.name)
                    self.assertNotIn(str(path), files)
                    self.assertEqual(0, member.uid)
                    self.assertEqual(0, member.gid)
                    files[str(path)] = member
            for relative in [
                "bin/java",
                "lib/modules",
                "lib/libjli.so",
                "conf/security/java.security",
                "lib/security/cacerts",
                "legal/java.base/LICENSE",
                "legal/java.base/ASSEMBLY_EXCEPTION",
                "release"
            ]:
                member = files["opt/java/" + relative]
                self.assertGreater(member.size, 0)
                self.assertTrue(member.mode & 0o444)
            self.assertTrue(files["opt/java/bin/java"].mode & 0o111)
            self.assertFalse(any(member.mode & 0o6000 for member in files.values()))


if __name__ == "__main__":
    unittest.main()
