#!/usr/bin/env python3
"""Narrow, offline BedrockMenu 1.4.2 hotfix; standard library only.

Only replaces the exact BUTTONS/COMMANDS <clinit> Code attribute and the
plugin.yml version/description. Every other class-file byte is preserved.
Input SHA-256 is mandatory; use the hash of the verified live server download.
"""
import argparse
import copy
import hashlib
import json
import re
import struct
import zipfile
from pathlib import Path

MAIN = 'com/winkreyes/bedrockmenu/BedrockMenu.class'
CLASSES = {MAIN, 'com/winkreyes/bedrockmenu/GeyserForms.class',
           'com/winkreyes/bedrockmenu/TeleportMenu.class',
           'com/winkreyes/bedrockmenu/TeleportMenu$PickHolder.class'}
OLD_BUTTONS = ['Spawn', 'Teleport to a player', 'Spawn Shop', 'Jobs & Money',
               'mcMMO Stats', 'Leaderboard', 'Server Info', 'Auto-open on join']
OLD_COMMANDS = ['spawn', 'tpmenu', 'shop', 'jobs browse', 'mcstats',
                'leaderboard', 'motd', 'menujoin']
NEW_BUTTONS = [s for s in OLD_BUTTONS if s != 'Jobs & Money']
NEW_COMMANDS = [s for s in OLD_COMMANDS if s != 'jobs browse']
VERSION = '1.4.2-menu.1'
DESCRIPTION = '/menu that works on Bedrock via native Geyser forms. Removes the redundant Jobs & Money menu entry.'

def require(condition, message):
    if not condition:
        raise ValueError(message)

def sha256(data):
    return hashlib.sha256(data).hexdigest()

def u2(data, pos):
    return struct.unpack_from('>H', data, pos)[0]

def u4(data, pos):
    return struct.unpack_from('>I', data, pos)[0]

def p2(n):
    return struct.pack('>H', n)

def p4(n):
    return struct.pack('>I', n)

class ClassFile:
    def __init__(self, data):
        self.data = data
        require(data[:4] == b'\xca\xfe\xba\xbe', 'Not a Java class')
        self.major = u2(data, 6)
        self.cp = [None] * u2(data, 8)
        p = 10
        i = 1
        while i < len(self.cp):
            tag = data[p]
            p += 1
            if tag == 1:
                size = u2(data, p)
                value = data[p+2:p+2+size].decode('utf-8')
                p += 2 + size
            elif tag in (7, 8, 16, 19, 20):
                value = u2(data, p)
                p += 2
            elif tag in (9, 10, 11, 12, 17, 18):
                value = (u2(data, p), u2(data, p+2))
                p += 4
            elif tag in (3, 4):
                value = data[p:p+4]
                p += 4
            elif tag in (5, 6):
                value = data[p:p+8]
                p += 8
            elif tag == 15:
                value = (data[p], u2(data, p+1))
                p += 3
            else:
                raise ValueError(f'Unsupported constant-pool tag {tag}')
            self.cp[i] = (tag, value)
            i += 2 if tag in (5, 6) else 1
        self.cp_end = p
        self.this_class = u2(data, p+2)
        self.super_class = u2(data, p+4)
        self.header_start = p
        p += 6
        p += 2 + 2 * u2(data, p)
        self.fields_count_pos = p
        self.fields, p = self.members(p)
        self.methods_count_pos = p
        self.methods, p = self.members(p)
        self.class_attributes_pos = p
        self.class_attributes, p = self.attributes(p)
        require(p == len(data), 'Trailing or malformed class data')

    def utf8(self, index):
        tag, value = self.cp[index]
        require(tag == 1, 'Expected UTF8 constant')
        return value

    def attributes(self, pos):
        data = self.data
        count = u2(data, pos)
        p = pos + 2
        result = []
        for _ in range(count):
            size = u4(data, p+2)
            result.append({'name': self.utf8(u2(data, p)), 'start': p,
                           'payload_start': p+6, 'end': p+6+size})
            p += 6 + size
        return result, p

    def members(self, pos):
        data = self.data
        count = u2(data, pos)
        p = pos + 2
        result = []
        for _ in range(count):
            start = p
            attrs, end = self.attributes(p+6)
            result.append({'start': start, 'end': end,
                           'name': self.utf8(u2(data, p+2)),
                           'descriptor': self.utf8(u2(data, p+4)),
                           'attributes': attrs})
            p = end
        return result, p

    def clinit_code(self):
        m = [m for m in self.methods if m['name'] == '<clinit>']
        require(len(m) == 1 and m[0]['descriptor'] == '()V', 'Expected one ()V class initializer')
        attrs = [a for a in m[0]['attributes'] if a['name'] == 'Code']
        require(len(attrs) == 1, 'Expected initializer Code attribute')
        return attrs[0]

    def string(self, index):
        tag, value = self.cp[index]
        require(tag == 8, 'Expected String constant')
        return self.utf8(value)

    def class_name(self, index):
        tag, value = self.cp[index]
        require(tag == 7, 'Expected Class constant')
        return self.utf8(value)

    def field(self, index):
        tag, (owner, name_type) = self.cp[index]
        require(tag == 9, 'Expected Fieldref constant')
        tag, (name, descriptor) = self.cp[name_type]
        require(tag == 12, 'Expected NameAndType constant')
        return self.class_name(owner), self.utf8(name), self.utf8(descriptor)

    def initializer(self):
        """Parse only the exact straight-line ASM historical array initializer."""
        attr = self.clinit_code()
        payload = self.data[attr['payload_start']:attr['end']]
        size = u4(payload, 4)
        require(payload[:4] == p2(4)+p2(0), 'Unexpected initializer max_stack/locals')
        require(payload[8+size:] == b'\x00\x00\x00\x00',
                'Initializer has exceptions or nested metadata; needs manual review')
        code = payload[8:8+size]
        p = 0
        arrays = []
        for field_name in ('BUTTONS', 'COMMANDS'):
            start = p
            require(code[p] == 0x10, 'Expected BIPUSH array length')
            count = code[p+1]
            p += 2
            require(code[p] == 0xbd, 'Expected ANEWARRAY')
            string_class = u2(code, p+1)
            require(self.class_name(string_class) == 'java/lang/String', 'Wrong array element class')
            array_header = code[start:p+3]
            p += 3
            items = []
            values = []
            for i in range(count):
                item_start = p
                require(code[p:p+2] == b'\x59\x10' and code[p+2] == i,
                        'Expected DUP/BIPUSH ordered array index')
                p += 3
                if code[p] == 0x12:
                    index = code[p+1]
                    p += 2
                else:
                    require(code[p] == 0x13, 'Expected LDC or LDC_W String')
                    index = u2(code, p+1)
                    p += 3
                values.append(self.string(index))
                require(code[p] == 0x53, 'Expected AASTORE')
                p += 1
                items.append(code[item_start:p])
            require(code[p] == 0xb3, 'Expected PUTSTATIC')
            require(self.field(u2(code, p+1)) ==
                    ('com/winkreyes/bedrockmenu/BedrockMenu', field_name, '[Ljava/lang/String;'),
                    'Wrong field target')
            footer = code[p:p+3]
            p += 3
            arrays.append({'field': field_name, 'values': values,
                           'header': array_header, 'items': items, 'footer': footer})
        require(code[p:] == b'\xb1', 'Unexpected code outside the two arrays')
        return arrays, payload, attr

def patch_class(data):
    cls = ClassFile(data)
    require(cls.major == 65, 'Expected live Java 21 class (version 65)')
    arrays, payload, attr = cls.initializer()
    require([a['values'] for a in arrays] == [OLD_BUTTONS, OLD_COMMANDS],
            'Input does not match the exact historical 1.4.2 menu')
    new_code = bytearray()
    for arr in arrays:
        header = bytearray(arr['header'])
        header[1] -= 1
        new_code.extend(header)
        for old_index, raw in enumerate(arr['items']):
            if old_index == 3:
                continue
            item = bytearray(raw)
            item[2] = old_index if old_index < 3 else old_index - 1
            new_code.extend(item)
        new_code.extend(arr['footer'])
    new_code.append(0xb1)
    new_payload = payload[:4] + p4(len(new_code)) + new_code + b'\x00\x00\x00\x00'
    return data[:attr['start']+2] + p4(len(new_payload)) + new_payload + data[attr['end']:]

def patch_descriptor(data):
    text = data.decode('utf-8')
    require(len(re.findall(r'^version: 1\.4\.2\r?$', text, re.M)) == 1,
            'Expected exactly version: 1.4.2')
    require(len(re.findall(r'^description: .*$', text, re.M)) == 1,
            'Expected exactly one description')
    text = re.sub(r'^version: 1\.4\.2(?=\r?$)', 'version: '+VERSION, text, flags=re.M)
    text = re.sub(r'^description: [^\r\n]*', 'description: '+DESCRIPTION, text, flags=re.M)
    return text.encode('utf-8')

def read_jar(path):
    with zipfile.ZipFile(path) as z:
        names = z.namelist()
        require(len(names) == len(set(names)), 'Duplicate ZIP entries')
        require({n for n in names if n.endswith('.class')} == CLASSES,
                'Unexpected class set; not the four-class historical plugin')
        require(not any(n.upper().endswith(('.SF', '.RSA', '.DSA', '.EC'))
                        for n in names), 'Signed JAR cannot be patched')
        require(z.testzip() is None, 'Corrupt ZIP entry')
        entries = {n: z.read(n) for n in names}
        require('plugin.yml' in entries, 'Missing descriptor')
        for name in CLASSES:
            require(ClassFile(entries[name]).major == 65, f'Unexpected class version: {name}')
        return entries, z.infolist(), z.comment

def build(input_path, output_path, expected_sha256):
    original = Path(input_path).read_bytes()
    require(sha256(original) == expected_sha256.lower(), 'Input SHA-256 mismatch')
    entries, infos, comment = read_jar(input_path)
    entries[MAIN] = patch_class(entries[MAIN])
    entries['plugin.yml'] = patch_descriptor(entries['plugin.yml'])
    output_path = Path(output_path)
    require(not output_path.exists(), 'Refusing to overwrite output')
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output_path, 'w') as out:
        out.comment = comment
        for info in infos:
            # Preserve timestamps, comments, permissions, extras and compression type.
            out.writestr(copy.copy(info), entries[info.filename])
    return verify(input_path, output_path)

def verify(input_path, output_path):
    before, infos_before, comment_before = read_jar(input_path)
    after, infos_after, comment_after = read_jar(output_path)
    require(list(before) == list(after), 'Archive entries/order changed')
    require(comment_before == comment_after, 'Archive comment changed')
    changed = [n for n in before if before[n] != after[n]]
    require(changed == [n for n in before if n in (MAIN, 'plugin.yml')],
            f'Unexpected changed entries: {changed}')
    require(after[MAIN] == patch_class(before[MAIN]), 'Class differs from exact narrow patch')
    require(after['plugin.yml'] == patch_descriptor(before['plugin.yml']), 'Descriptor changed beyond hotfix version/description')
    c1, c2 = ClassFile(before[MAIN]), ClassFile(after[MAIN])
    a1, a2 = c1.clinit_code(), c2.clinit_code()
    require(before[MAIN][:a1['start']] == after[MAIN][:a2['start']] and
            before[MAIN][a1['end']:] == after[MAIN][a2['end']:],
            'Bytes outside initializer Code attribute changed')
    arrays, _, _ = c2.initializer()
    require([a['values'] for a in arrays] == [NEW_BUTTONS, NEW_COMMANDS], 'Incorrect menu mapping')
    attrs = ('filename', 'date_time', 'compress_type', 'comment', 'extra',
             'create_system', 'create_version', 'extract_version', 'internal_attr', 'external_attr')
    for i1, i2 in zip(infos_before, infos_after):
        require(all(getattr(i1, n) == getattr(i2, n) for n in attrs),
                'ZIP entry metadata changed: '+i1.filename)
    unchanged_methods = [m['name']+m['descriptor'] for m in c1.methods if m['name'] != '<clinit>']
    return {'input_sha256': sha256(Path(input_path).read_bytes()),
            'output_sha256': sha256(Path(output_path).read_bytes()),
            'output_bytes': Path(output_path).stat().st_size,
            'version': VERSION, 'changed_entries': changed,
            'unchanged_entries': [n for n in before if n not in changed],
            'only_changed_class_attribute': 'BedrockMenu.<clinit>()V Code',
            'constant_pool_and_all_other_class_bytes_identical': True,
            'unchanged_methods': unchanged_methods,
            'button_command_pairs': list(map(list, zip(NEW_BUTTONS, NEW_COMMANDS))),
            'scope': 'Offline bytecode/archive checks only; actual Geyser/Paper/player testing not run.'}

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('action', choices=['build', 'verify'])
    p.add_argument('input', type=Path)
    p.add_argument('output', type=Path)
    p.add_argument('--input-sha256', required=True)
    p.add_argument('--report', type=Path)
    a = p.parse_args()
    require(sha256(a.input.read_bytes()) == a.input_sha256.lower(), 'Input SHA-256 mismatch')
    report = build(a.input, a.output, a.input_sha256) if a.action == 'build' else verify(a.input, a.output)
    report_text = json.dumps(report, indent=2)+'\n'
    if a.report:
        a.report.write_text(report_text)
    print(report_text, end='')

if __name__ == '__main__':
    main()
