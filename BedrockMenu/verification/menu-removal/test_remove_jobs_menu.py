import copy
import json
import hashlib
import os
import importlib.util
import tempfile
import unittest
import zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('patch',ROOT/'tools/remove_jobs_menu.py')
m=importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
BASELINE=Path(os.environ['BEDROCKMENU_BASELINE']).resolve()

class PatchTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.out=Path(self.tmp.name)/'candidate.jar'
        self.entries,self.infos,self.comment=m.read_jar(BASELINE)
        self.hash=m.sha256(BASELINE.read_bytes())
    def write_modified(self, name, data):
        p=Path(self.tmp.name)/'modified.jar'
        with zipfile.ZipFile(p,'w') as z:
            for info in self.infos:
                z.writestr(copy.copy(info), data if info.filename==name else self.entries[info.filename])
        return p
    def test_baseline_payload_matches_verified_live_inventory(self):
        expected=json.loads((Path(__file__).parent/'baseline-payload-sha256.json').read_text())
        actual={name: {'bytes': len(data), 'sha256': hashlib.sha256(data).hexdigest()}
                for name,data in self.entries.items() if not name.endswith('/')}
        self.assertEqual(actual,expected)
    def test_exact_mapping_and_payload(self):
        report=m.build(BASELINE,self.out,self.hash)
        self.assertEqual(report['button_command_pairs'],list(map(list,zip(m.NEW_BUTTONS,m.NEW_COMMANDS))))
        self.assertEqual(len(report['unchanged_methods']),8)
        self.assertTrue(report['constant_pool_and_all_other_class_bytes_identical'])
        self.assertEqual(report['changed_entries'],[m.MAIN,'plugin.yml'])
    def test_build_is_deterministic(self):
        m.build(BASELINE,self.out,self.hash)
        second=self.out.with_name('second.jar')
        m.build(BASELINE,second,self.hash)
        self.assertEqual(self.out.read_bytes(),second.read_bytes())
    def test_wrong_hash_rejected(self):
        with self.assertRaisesRegex(ValueError,'SHA-256'):
            m.build(BASELINE,self.out,'0'*64)
        self.assertFalse(self.out.exists())
    def test_no_output_overwrite(self):
        self.out.write_bytes(b'sentinel')
        with self.assertRaisesRegex(ValueError,'overwrite'):
            m.build(BASELINE,self.out,self.hash)
        self.assertEqual(self.out.read_bytes(),b'sentinel')
    def test_double_patch_rejected(self):
        once=m.patch_class(self.entries[m.MAIN])
        with self.assertRaisesRegex(ValueError,'exact historical'):
            m.patch_class(once)
    def test_wrong_version_rejected(self):
        data=self.entries['plugin.yml'].replace(b'version: 1.4.2',b'version: 1.4.3')
        with self.assertRaisesRegex(ValueError,'version'):
            m.patch_descriptor(data)
    def test_duplicate_version_rejected(self):
        with self.assertRaisesRegex(ValueError,'version'):
            m.patch_descriptor(self.entries['plugin.yml']+b'\nversion: 1.4.2\n')
    def test_java25_class_rejected(self):
        data=self.entries[m.MAIN]; data=data[:6]+m.p2(69)+data[8:]
        with self.assertRaisesRegex(ValueError,'version 65'):
            m.patch_class(data)
    def test_changed_route_rejected(self):
        data=self.entries[m.MAIN].replace(b'jobs browse',b'jobs browse'[:-1]+b'x')
        with self.assertRaisesRegex(ValueError,'exact historical'):
            m.patch_class(data)
    def test_unordered_indices_rejected(self):
        data=self.entries[m.MAIN]; cls=m.ClassFile(data); a=cls.clinit_code()
        p=a['payload_start']+8+7
        changed=data[:p]+b'\x01'+data[p+1:]
        with self.assertRaisesRegex(ValueError,'ordered array index'):
            m.patch_class(changed)
    def test_extra_initializer_code_rejected(self):
        data=self.entries[m.MAIN]; cls=m.ClassFile(data); a=cls.clinit_code()
        p=a['payload_start']+8+m.u4(data,a['payload_start']+4)-1
        with self.assertRaisesRegex(ValueError,'outside the two arrays'):
            m.patch_class(data[:p]+b'\x00'+data[p+1:])
    def test_unrelated_file_change_detected(self):
        m.build(BASELINE,self.out,self.hash)
        with zipfile.ZipFile(self.out) as z: data={n:z.read(n) for n in z.namelist()}
        data['META-INF/MANIFEST.MF']+=b'\n'
        bad=self.out.with_name('bad.jar')
        with zipfile.ZipFile(bad,'w') as z:
            for info in self.infos: z.writestr(copy.copy(info),data[info.filename])
        with self.assertRaisesRegex(ValueError,'Unexpected changed entries'):
            m.verify(BASELINE,bad)
    def test_other_member_change_detected(self):
        m.build(BASELINE,self.out,self.hash)
        with zipfile.ZipFile(self.out) as z: data={n:z.read(n) for n in z.namelist()}
        data[m.MAIN]=data[m.MAIN].replace(b'Only players can use /menu.',b'Only players can use /xxxx.')
        bad=self.out.with_name('bad.jar')
        with zipfile.ZipFile(bad,'w') as z:
            for info in self.infos: z.writestr(copy.copy(info),data[info.filename])
        with self.assertRaisesRegex(ValueError,'exact narrow patch'):
            m.verify(BASELINE,bad)
    def test_extra_classes_rejected(self):
        p=Path(self.tmp.name)/'extra.jar'
        with zipfile.ZipFile(p,'w') as z:
            for info in self.infos: z.writestr(copy.copy(info),self.entries[info.filename])
            z.writestr('com/winkreyes/bedrockmenu/SpawnLocations.class',self.entries[m.MAIN])
        with self.assertRaisesRegex(ValueError,'class set'):
            m.read_jar(p)
    def test_signed_jar_rejected(self):
        p=Path(self.tmp.name)/'signed.jar'
        with zipfile.ZipFile(p,'w') as z:
            for info in self.infos: z.writestr(copy.copy(info),self.entries[info.filename])
            z.writestr('META-INF/TEST.SF',b'signature')
        with self.assertRaisesRegex(ValueError,'Signed'):
            m.read_jar(p)
    def test_duplicate_zip_entry_rejected(self):
        p=Path(self.tmp.name)/'duplicate.jar'
        import warnings
        with warnings.catch_warnings():
            warnings.simplefilter('ignore')
            with zipfile.ZipFile(p,'w') as z:
                for info in self.infos: z.writestr(copy.copy(info),self.entries[info.filename])
                z.writestr('plugin.yml',self.entries['plugin.yml'])
        with self.assertRaisesRegex(ValueError,'Duplicate'):
            m.read_jar(p)

if __name__=='__main__': unittest.main(verbosity=2)
