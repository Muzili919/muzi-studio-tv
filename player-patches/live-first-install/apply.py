"""Apply the legacy 422 -> 423 repair to an apktool decoded working copy.

Usage: python3 apply.py DECODED_APK HELPER_SMALI ORIGINAL_APK
The original APK is read only, and this script never reads device user data.
"""
from pathlib import Path
import shutil
import sys
import zipfile

target, helper, original = map(Path, sys.argv[1:])
assert (helper / 'studio/muzi/fongmi/DefaultConfig.smali').is_file(), 'Compile helper first'
assert original.is_file(), 'Original APK must be available'
app = target / 'smali/com/fongmi/android/tv/App.smali'
text = app.read_text()
head, body = text.split('.method public final onCreate()V', 1)
assert 'DefaultConfig;->ensure' not in body, 'Patch already applied'
body = body.replace('    return-void', '    invoke-static {}, Lstudio/muzi/fongmi/DefaultConfig;->ensure()V\n\n    return-void', 1)
app.write_text(head + '.method public final onCreate()V' + body)
destination = target / 'smali_classes4/studio/muzi/fongmi/DefaultConfig.smali'
shutil.copyfile(helper / 'studio/muzi/fongmi/DefaultConfig.smali', destination)

surface = target / 'smali_classes4/studio/muzi/fongmi/HomeSurface.smali'
text = surface.read_text()
start = text.index('.method public static entry(')
end = text.index('.end method', start)
entry = text[start:end]
old = '    iput-boolean v1, v0, Lstudio/muzi/fongmi/HomeSurface;->livePending:Z'
assert entry.count(old) == 2
entry = entry.replace(old, old + '\n\n    invoke-virtual {v0, v1}, Lstudio/muzi/fongmi/HomeSurface;->setLiveMode(Z)V')
text = text[:start] + entry + text[end:]
fields = [('nav', 'LinearLayout'), ('sort', 'LinearLayout'), ('regions', 'LinearLayout'), ('grid', 'GridView')]
method = '\n.method setLiveMode(Z)V\n    .locals 2\n    const/4 v1, 0x0\n    if-eqz p1, :ready\n    const/16 v1, 0x8\n    :ready\n'
for field, kind in fields:
    method += f'    iget-object v0, p0, Lstudio/muzi/fongmi/HomeSurface;->{field}:Landroid/widget/{kind};\n'
    method += '    invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V\n'
method += '    return-void\n.end method\n'
surface.write_text(text + method)

yml = target / 'apktool.yml'
text = yml.read_text()
assert 'versionCode: 422' in text and 'versionName: 4.0.7-muzi.10' in text
yml.write_text(text.replace('versionCode: 422', 'versionCode: 423').replace('versionName: 4.0.7-muzi.10', 'versionName: 4.0.7-muzi.11'))

# --no-assets decoding omits these; restore the original bytes for Python/catalog/native support.
with zipfile.ZipFile(original) as archive:
    for member in archive.infolist():
        if not member.filename.startswith('assets/') or member.is_dir():
            continue
        path = target / member.filename
        assert target.resolve() in path.resolve().parents
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(archive.read(member.filename))
print('Candidate repair applied; version 423. Real-device fresh-install acceptance still required.')
