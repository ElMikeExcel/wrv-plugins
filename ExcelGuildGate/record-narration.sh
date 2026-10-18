#!/usr/bin/env bash
# Re-record ALL FIVE GuildGate narrator lines and rebuild the resource pack.
#
#   ./record-narration.sh              full run: TTS -> ogg -> pack zip + sha1
#   ./record-narration.sh --pack-only  skip TTS: rebuild the zip from the
#                                      oggs already under pack-src/
#   VOICE=avocado_v2:someone ./record-narration.sh
#                                      same lines, a different voice
#                                      (Mike's voice approval is the only
#                                      gate on the shipped take)
#
# The five scripts below are the canonical text; NARRATION-LINES.md quotes
# them for reading. Edit a line here, re-run, and the pack (and its SHA-1)
# is regenerated in one go. After a re-record, update server.properties'
# resource-pack-sha1 to the new value printed at the end (INSTALL-PACKET.md).
set -euo pipefail
cd "$(dirname "$0")"

VOICE="${VOICE:-avocado_v2:magnus}"
TTS="${TTS:-/opt/hatch/bin/tts}"

LINES=(
"The first seal has fallen. Long-sealed magic seeps back into the valley, and the Healer's path stands open. The Adventurers Guild stirs in its sleep."
"The second seal has fallen. Old roads shine again in the dark, and the Pathfinder walks them once more. The Guild stirs in its sleep."
"The third seal has fallen. Thunder remembers its voice, and the Wonder wakes in storm and spark. The Guild stirs in its sleep."
"The fourth seal has fallen. Stone and timber answer the Masons' hands, and walls rise where the will directs. The Guild stirs in its sleep."
"The fifth seal has fallen. The Adventurers Guild opens its doors at last, and every school of magic returns to the valley."
)

SOUND_DIR="pack-src/assets/wrv/sounds/guildgate"

if [[ "${1:-}" != "--pack-only" ]]; then
  mkdir -p narration/mp3 "$SOUND_DIR"
  for i in 1 2 3 4 5; do
    echo "== seal $i: recording with voice $VOICE"
    "$TTS" speak --voice "$VOICE" --text "${LINES[$((i - 1))]}" \
      --output "narration/mp3/seal$i.mp3" >/dev/null
    ffmpeg -v error -y -i "narration/mp3/seal$i.mp3" \
      -ac 1 -ar 44100 -codec:a libvorbis -q:a 4 "$SOUND_DIR/seal$i.ogg"
  done
else
  echo "== --pack-only: keeping existing oggs under $SOUND_DIR"
fi

# Deterministic zip: sorted entries, pinned timestamps (Moon's playbook —
# the same discipline as the jar build), so a rebuild reproduces the SHA-1
# whenever the audio is unchanged.
PACK="wrv-guildgate-narrator-pack.zip"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
mkdir -p "$WORK/stage"
cp -r pack-src/* "$WORK/stage/"
find "$WORK/stage" -exec touch -t 202610170000.00 {} +
(cd "$WORK/stage" && find . -type f | LC_ALL=C sort | sed 's|^\./||' \
  | zip -q -X -D "$WORK/$PACK" -@)
mv "$WORK/$PACK" "$PACK"
SHA1="$(sha1sum "$PACK" | awk '{print $1}')"
echo "$SHA1" > PACK-SHA1.txt
echo "built $PACK ($(stat -c%s "$PACK") bytes)"
echo "resource-pack-sha1: $SHA1"
