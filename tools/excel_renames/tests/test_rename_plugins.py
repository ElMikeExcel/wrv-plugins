import copy
import io
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
import warnings
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import rename_plugins as r
import deployment_preflight as d


def archive(yml=None, extra=None, plugin='NoPhantoms', entries=None):
    yml = yml or f'name: {plugin}\nversion: {r.VERSIONS[plugin]}\nmain: example.Main\ncommands:\n  original:\n    permission: original.use\n'.encode()
    entries = entries if entries is not None else [('plugin.yml', yml), ('example/Main.class', b'\xca\xfe\xba\xbe-original'), ('config.yml', b'unrelated: untouched\n')]
    entries += extra or []
    b = io.BytesIO()
    with warnings.catch_warnings():
        warnings.simplefilter('ignore', UserWarning)
        with zipfile.ZipFile(b, 'w', zipfile.ZIP_DEFLATED) as z:
            z.comment = b'original archive comment'
            for name, body in entries:
                info = zipfile.ZipInfo(name, (2026, 1, 2, 3, 4, 6)) if isinstance(name, str) else name
                info.compress_type = zipfile.ZIP_DEFLATED
                info.comment = b'entry-comment'
                z.writestr(info, body)
    return b.getvalue()


def transform(data, plugin='NoPhantoms', hash_=None):
    return r.transform_bytes(data, hash_ or r.sha256(data), plugin)


class RenameTests(unittest.TestCase):
    def test_exact_allowed_change_and_payload_identity(self):
        data = archive(); candidate, audit = transform(data)
        self.assertEqual(audit['changed_entries'], ['plugin.yml'])
        self.assertEqual(audit['descriptor_after']['commands'], audit['descriptor_before']['commands'])
        self.assertEqual(audit['descriptor_after']['provides'], ['NoPhantoms'])
        for a, b in zip(audit['entries'][1:], audit['entries'][1:]):
            self.assertEqual(a['input_sha256'], b['output_sha256'])
        self.assertEqual(zipfile.ZipFile(io.BytesIO(candidate)).comment, b'original archive comment')

    def test_reproducible(self):
        data = archive(); self.assertEqual(transform(data), transform(data))

    def test_seven_metadata_only_plugins(self):
        for name in r.VERSIONS:
            if name == 'MenuJoin': continue
            with self.subTest(name=name):
                _, a = transform(archive(plugin=name), name)
                self.assertEqual(a['new_name'], 'Excel' + name)
                self.assertEqual(a['changed_entries'], ['plugin.yml'])

    def test_hash_mismatch(self):
        with self.assertRaisesRegex(r.Refusal, 'mismatch'): transform(archive(), hash_='0'*64)

    def test_hash_required(self):
        for value in ('', 'bad', 'A'*64, None):
            with self.subTest(value=value), self.assertRaises(r.Refusal): r.transform_bytes(archive(), value, 'NoPhantoms')

    def test_wrong_name(self):
        with self.assertRaisesRegex(r.Refusal, 'runtime name'): transform(archive(plugin='ShopVillager'))

    def test_wrong_version(self):
        with self.assertRaisesRegex(r.Refusal, 'version'): transform(archive(yml=b'name: NoPhantoms\nversion: 99\nmain: example.Main\n'))

    def test_unknown_plugin(self):
        with self.assertRaises(r.Refusal): transform(archive(), 'Unknown')

    def test_already_renamed(self):
        candidate, _ = transform(archive())
        with self.assertRaises(r.Refusal): transform(candidate)

    def test_menujoin_rejects_unpatched(self):
        with self.assertRaisesRegex(r.Refusal, 'namespace'): transform(archive(plugin='MenuJoin'), 'MenuJoin')

    def test_menujoin_stub_refuses(self):
        from menujoin_namespace import patch_menujoin
        data = archive(plugin='MenuJoin')
        with self.assertRaisesRegex(r.Refusal, 'blocked'):
            r.transform_bytes(data, r.sha256(data), 'MenuJoin', patch_menujoin)

    def test_duplicate_entries(self):
        with self.assertRaisesRegex(r.Refusal, 'Duplicate'): transform(archive(extra=[('plugin.yml', b'name: evil')]))

    def test_case_collision(self):
        with self.assertRaisesRegex(r.Refusal, 'colliding'): transform(archive(extra=[('PLUGIN.YML', b'x')]))

    def test_unsafe_paths(self):
        for path in ('../x', '/tmp/x', 'a/../x', 'a//x', './x', 'a\\x', 'C:/x'):
            with self.subTest(path=path), self.assertRaises(r.Refusal): transform(archive(extra=[(path, b'x')]))

    def test_nul_filename_rejected(self):
        data=archive(extra=[('xYz', b'value')]).replace(b'xYz', b'x\x00z')
        with self.assertRaisesRegex(r.Refusal, 'NUL'): transform(data)

    def test_special_file(self):
        i = zipfile.ZipInfo('link'); i.create_system = 3; i.external_attr = 0o120777 << 16
        with self.assertRaisesRegex(r.Refusal, 'Special'): transform(archive(extra=[(i, b'target')]))

    def test_signed_jars(self):
        for path in ('META-INF/X.SF', 'META-INF/x.rsa', 'META-INF/SIG-FOO'):
            with self.subTest(path=path), self.assertRaisesRegex(r.Refusal, 'Signed'): transform(archive(extra=[(path, b'x')]))

    def test_manifest_digests(self):
        with self.assertRaisesRegex(r.Refusal, 'digest'): transform(archive(extra=[('META-INF/MANIFEST.MF', b'Manifest-Version: 1.0\nSHA-256-Digest: abc\n')]))

    def test_unexpected_loader_metadata(self):
        for path in ('paper-plugin.yml', 'bungee.yml', 'velocity-plugin.json', 'META-INF/INDEX.LIST', 'META-INF/versions/9/X.class'):
            with self.subTest(path=path), self.assertRaises(r.Refusal): transform(archive(extra=[(path, b'x')]))

    def test_missing_descriptor(self):
        with self.assertRaisesRegex(r.Refusal, 'plugin.yml'): transform(archive(entries=[('x', b'x')]))

    def test_missing_main(self):
        with self.assertRaisesRegex(r.Refusal, 'main class'): transform(archive(entries=[('plugin.yml', b'name: NoPhantoms\nversion: 1.0.0\nmain: example.Main\n')]))

    def test_duplicate_yaml_keys(self):
        for yml in (b'name: NoPhantoms\nname: Evil\nversion: 1.0.0\nmain: example.Main\n', b'name: NoPhantoms\nversion: 1.0.0\nmain: example.Main\ncommands:\n  x: 1\n  x: 2\n'):
            with self.subTest(yml=yml), self.assertRaisesRegex(r.Refusal, 'Duplicate YAML'): transform(archive(yml=yml))

    def test_yaml_alias_tags_multi_document(self):
        base = b'name: NoPhantoms\nversion: 1.0.0\nmain: example.Main\n'
        for extra in (b'x: &foo hello\ny: *foo\n', b'x: !!str foo\n', b'---\nname: Evil\n'):
            with self.subTest(extra=extra), self.assertRaises(r.Refusal): transform(archive(yml=base + extra))

    def test_existing_provides_preserved(self):
        yml = b'name: NoPhantoms\nversion: 1.0.0\nmain: example.Main\nprovides:\n- AnAlias\n'
        _, a = transform(archive(yml=yml)); self.assertEqual(a['descriptor_after']['provides'], ['AnAlias', 'NoPhantoms'])

    def test_conflicting_provides(self):
        for aliases in ('NoPhantoms', 'ExcelNoPhantoms', 'Alias, alias'):
            yml = f'name: NoPhantoms\nversion: 1.0.0\nmain: example.Main\nprovides: [{aliases}]\n'.encode()
            with self.subTest(aliases=aliases), self.assertRaises(r.Refusal): transform(archive(yml=yml))

    def test_comments_crlf_and_unchanged_fields(self):
        yml = b'# keep\r\nname: "NoPhantoms" # label\r\nversion: \'1.0.0\'\r\nmain: example.Main\r\npermissions:\r\n  n.use:\r\n    default: true\r\n'
        candidate, _ = transform(archive(yml=yml))
        new = zipfile.ZipFile(io.BytesIO(candidate)).read('plugin.yml')
        self.assertIn(b'# keep\r\nname: ExcelNoPhantoms # label\r\n', new)
        self.assertIn(b'permissions:\r\n  n.use:\r\n    default: true\r\n', new)

    def test_source_preserved_and_overwrite_refused(self):
        with tempfile.TemporaryDirectory() as temp:
            p = Path(temp); source = p / 'original.jar'; data = archive(); source.write_bytes(data)
            out, a = r.transform_file(source, r.sha256(data), 'NoPhantoms', p)
            self.assertEqual(source.read_bytes(), data)
            self.assertEqual(r.sha256(out.read_bytes()), a['output_sha256'])
            with self.assertRaisesRegex(r.Refusal, 'exists'): r.transform_file(source, r.sha256(data), 'NoPhantoms', p)

    def test_source_symlink_refused(self):
        with tempfile.TemporaryDirectory() as temp:
            p=Path(temp); source=p/'a'; source.write_bytes(archive()); link=p/'b'; link.symlink_to(source)
            with self.assertRaises(r.Refusal): r.read_regular(link)


class PreflightTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(); self.addCleanup(self.temp.cleanup)
        self.root=Path(self.temp.name); self.plugins=self.root/'plugins'; self.plugins.mkdir(); self.candidates=self.root/'candidates'; self.candidates.mkdir()
        self.plan={'schema':1, 'plugins': []}
        self.namespace_fixture = lambda x: (x+b'-namespace-fixture', {'fixture':True})
        mock = patch('deployment_preflight.patch_menujoin', self.namespace_fixture)
        mock.start(); self.addCleanup(mock.stop)
        for old in r.VERSIONS:
            data=archive(plugin=old); (self.plugins/f'{old}-{r.VERSIONS[old]}.jar').write_bytes(data)
            # Preflight fixtures check deployment structure, not the separate class patch.
            patcher=self.namespace_fixture if old == 'MenuJoin' else None
            candidate,a=r.transform_bytes(data,r.sha256(data),old,patcher)
            path=self.candidates/a['output_filename']; path.write_bytes(candidate)
            self.plan['plugins'].append({'old_name':old,'baseline_sha256':r.sha256(data),'candidate_path':str(path),'candidate_sha256':r.sha256(candidate)})
        (self.plugins/'MenuJoin').mkdir(); (self.plugins/'MenuJoin'/'config.yml').write_text('saved: unchanged\n')

    def test_clean_preflight_with_explicit_test_namespace_mock_and_config_hash(self):
        receipt=d.preflight(self.plugins,self.plan)
        self.assertEqual(len(receipt['actions']),8)
        a=next(a for a in receipt['actions'] if a['old_name']=='MenuJoin')
        self.assertEqual(a['config_snapshot'][0]['sha256'],r.sha256(b'saved: unchanged\n'))
        self.assertTrue((self.plugins/'MenuJoin').is_dir())
        self.assertFalse((self.plugins/'ExcelMenuJoin').exists())

    def test_real_namespace_patch_blocks_all_eight_production_preflight(self):
        from menujoin_namespace import patch_menujoin
        with patch('deployment_preflight.patch_menujoin', patch_menujoin):
            with self.assertRaisesRegex(r.Refusal, 'blocked'):
                d.preflight(self.plugins,self.plan)

    def test_changed_candidate_payloads_rejected_even_with_matching_plan_hash(self):
        item=next(x for x in self.plan['plugins'] if x['old_name']=='NoPhantoms')
        path=Path(item['candidate_path']); original=path.read_bytes()
        for target in ('example/Main.class', 'config.yml', 'unexpected.txt'):
            _, _, payloads=r.safe_archive(original)
            payloads[target]=b'UNAUTHORIZED CHANGE'
            mutated=archive(entries=list(payloads.items()))
            path.write_bytes(mutated); item['candidate_sha256']=r.sha256(mutated)
            with self.subTest(target=target), self.assertRaisesRegex(r.Refusal, 'payload/archive'):
                d.preflight(self.plugins,self.plan)

    def test_pending_update_jar_rejected(self):
        update=self.plugins/'update'; update.mkdir()
        source=next(self.plugins.glob('NoPhantoms*.jar')); (update/source.name).write_bytes(source.read_bytes())
        with self.assertRaisesRegex(r.Refusal,'Pending update'):
            d.preflight(self.plugins,self.plan)

    def test_custom_update_directory_rejected(self):
        update=self.root/'custom-updates'; update.mkdir()
        source=next(self.plugins.glob('NoPhantoms*.jar')); (update/source.name).write_bytes(source.read_bytes())
        self.plan['additional_update_directories']=[str(update)]
        with self.assertRaisesRegex(r.Refusal,'Pending update'):
            d.preflight(self.plugins,self.plan)

    def test_additional_plugin_scan_duplicate_rejected(self):
        extra=self.root/'other-plugins'; extra.mkdir()
        source=next(self.plugins.glob('NoPhantoms*.jar')); (extra/'unknown.jar').write_bytes(source.read_bytes())
        self.plan['additional_plugin_directories']=[str(extra)]
        with self.assertRaisesRegex(r.Refusal,'duplicates'):
            d.preflight(self.plugins,self.plan)

    def test_baseline_only_in_additional_scan_directory_rejected(self):
        extra=self.root/'other-plugins'; extra.mkdir()
        source=next(self.plugins.glob('NoPhantoms*.jar')); source.rename(extra/source.name)
        self.plan['additional_plugin_directories']=[str(extra)]
        with self.assertRaisesRegex(r.Refusal,'outside primary'):
            d.preflight(self.plugins,self.plan)

    def test_case_collision_in_additional_scan_directory_rejected(self):
        extra=self.root/'other-plugins'; extra.mkdir()
        (extra/'Folder').mkdir(); (extra/'folder').mkdir()
        self.plan['additional_plugin_directories']=[str(extra)]
        with self.assertRaisesRegex(r.Refusal,'Case-colliding'):
            d.preflight(self.plugins,self.plan)

    def test_duplicate_legacy_jar(self):
        source=next(self.plugins.glob('NoPhantoms*.jar')); (self.plugins/'NoPhantoms-copy.jar').write_bytes(source.read_bytes())
        with self.assertRaisesRegex(r.Refusal,'duplicates'): d.preflight(self.plugins,self.plan)

    def test_old_and_new_jar_collision(self):
        item=next(x for x in self.plan['plugins'] if x['old_name']=='NoPhantoms'); p=Path(item['candidate_path']); (self.plugins/p.name).write_bytes(p.read_bytes())
        with self.assertRaisesRegex(r.Refusal,'duplicates'): d.preflight(self.plugins,self.plan)

    def test_renamed_file_still_detected(self):
        source=next(self.plugins.glob('NoPhantoms*.jar')); (self.plugins/'mystery.jar').write_bytes(source.read_bytes())
        with self.assertRaisesRegex(r.Refusal,'duplicates'): d.preflight(self.plugins,self.plan)

    def test_destination_conflict(self):
        (self.plugins/'ExcelMenuJoin').mkdir()
        with self.assertRaisesRegex(r.Refusal,'destination'): d.preflight(self.plugins,self.plan)

    def test_casefold_destination_conflict(self):
        (self.plugins/'excelmenujoin').mkdir()
        with self.assertRaisesRegex(r.Refusal,'destination'): d.preflight(self.plugins,self.plan)

    def test_baseline_changed(self):
        self.plan['plugins'][0]['baseline_sha256']='0'*64
        with self.assertRaisesRegex(r.Refusal,'baseline hash'): d.preflight(self.plugins,self.plan)

    def test_candidate_changed(self):
        self.plan['plugins'][0]['candidate_sha256']='0'*64
        with self.assertRaisesRegex(r.Refusal,'candidate hash'): d.preflight(self.plugins,self.plan)

    def test_incomplete_plan(self):
        self.plan['plugins'].pop()
        with self.assertRaisesRegex(r.Refusal,'each'): d.preflight(self.plugins,self.plan)

    def test_symlink_config(self):
        (self.plugins/'MenuJoin'/'secret').symlink_to(self.root)
        with self.assertRaisesRegex(r.Refusal,'Symlink'): d.preflight(self.plugins,self.plan)

    def test_unexpected_claim_from_other_plugin(self):
        yml=b'name: Other\nversion: 1.0.0\nmain: example.Main\nprovides: [MenuJoin]\n'
        (self.plugins/'Other.jar').write_bytes(archive(yml=yml))
        with self.assertRaisesRegex(r.Refusal,'duplicates'): d.preflight(self.plugins,self.plan)

    def test_unverified_original_bedrock_rejected(self):
        path=next(self.plugins.glob('BedrockMenu*.jar'))
        data=archive(yml=b'name: BedrockMenu\nversion: 1.4.2\nmain: example.Main\n')
        path.write_bytes(data)
        item=next(x for x in self.plan['plugins'] if x['old_name']=='BedrockMenu')
        item['baseline_sha256']=r.sha256(data)
        with self.assertRaisesRegex(r.Refusal,'provenance'): d.preflight(self.plugins,self.plan)

    def test_no_missing_baseline(self):
        next(self.plugins.glob('NoPhantoms*.jar')).unlink()
        with self.assertRaisesRegex(r.Refusal,'expected exactly one'): d.preflight(self.plugins,self.plan)


if __name__ == '__main__': unittest.main()
