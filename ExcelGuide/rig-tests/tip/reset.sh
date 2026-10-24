#!/usr/bin/env bash
# Reset the rig ExcelGuide data folder to the live-like post-clear state.
D=/workspace/guide-rig/plugins/ExcelGuide
rm -f $D/peer-review.yml $D/peer-tip.yml $D/peer.log
printf '[]\n' > $D/peer-answers.yml; printf '{}\n' > $D/learned.yml; : > $D/unanswered.log; : > $D/player-toggles.txt
