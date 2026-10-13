"""Fail-closed placeholder for the exact live MenuJoin class patch.

Do not replace this with recompilation of recovered/canonical source. See
MENUJOIN-NAMESPACE-PLAN.md. The parent verified the live JAR SHA-256, but the
corresponding class bytes have not yet been retrieved and inspected.
"""
from rename_plugins import Refusal

LIVE_JAR_SHA256 = 'abc335c402295f458b5beae4ad0972bb8c3c981ed74dca28efe0571d0d1d8178'
PERSISTED_KEYS = ('menujoin:autojoin_disabled', 'menujoin:menu_compass')


def patch_menujoin(class_bytes):
    raise Refusal('MenuJoin namespace patch is blocked: exact live class bytes have not been retrieved, inspected and pinned. No MenuJoin candidate may be emitted.')
