"""Structural/artifact audit, supplementing the JVM and real Paper tests."""
from pathlib import Path
import hashlib
import importlib.util
import json
import tempfile
import unittest
import zipfile

BASE = Path(__file__).resolve().parents[1]
SRC = BASE / 'src/com/Acrobot/ChestShop'


class GuardAudit(unittest.TestCase):
    def test_default_deny_is_part_of_both_event_types(self):
        self.assertIn('!com.Acrobot.ChestShop.Confirmation.ConfirmationGate.isAdmitted(this)', (SRC / 'Events/TransactionEvent.java').read_text())
        self.assertIn('!com.Acrobot.ChestShop.Confirmation.ConfirmationGate.isPrepared(this)', (SRC / 'Events/PreTransactionEvent.java').read_text())

    def test_sign_interaction_never_executes_a_transaction(self):
        code = (SRC / 'Listeners/Player/PlayerInteract.java').read_text()
        interaction = code.split('public static void onInteract(', 1)[1].split('public static boolean canPrepareTrade', 1)[0]
        self.assertIn('ConfirmationGate.offer(player, sign)', interaction)
        self.assertNotIn('new TransactionEvent', interaction)
        self.assertNotIn('new PreTransactionEvent', interaction)
        self.assertNotIn('setSneaking(', code)
        self.assertNotIn('addAttachment(', code)
        self.assertIn('public static PreTransactionEvent prepareTrade(', code)

    def test_direct_mutation_entrypoints_have_guards(self):
        requirements = {
            'Listeners/PostTransaction/EconomicModule.java': ['ConfirmationGate.beginMoney(event)', 'ConfirmationGate.bindTransfer(event, currencyTransferEvent)', 'ConfirmationGate.finishMoney'],
            'Listeners/PostTransaction/ItemManager.java': ['ConfirmationGate.beginItems(event)', 'ConfirmationGate.finishItems(event, leftovers)'],
            'Listeners/Economy/TaxModule.java': ['ConfirmationGate.beginTax(event)'],
            'Listeners/Economy/EconomyAdapter.java': ['ConfirmationGate.beginTransfer(event)'],
            'Listeners/Modules/StockCounterModule.java': ['event.isCancelled()', 'ConfirmationGate.itemsCompleted(event)'],
        }
        for file, guards in requirements.items():
            code = (SRC / file).read_text()
            for guard in guards:
                with self.subTest(file=file, guard=guard):
                    self.assertIn(guard, code)

    def test_success_notifications_require_receipt_only_capability(self):
        code = (SRC / 'Listeners/PostTransaction/TransactionMessageSender.java').read_text()
        monitor = code.split('public static void onCurrencyTransfer(', 1)[1].split('public static void sendAcknowledged(', 1)[0]
        self.assertNotIn('sendBuyMessage(', monitor)
        self.assertNotIn('sendSellMessage(', monitor)
        self.assertIn('ConfirmationGate.isAcknowledging(event)', code)
        gate = (SRC / 'Confirmation/ConfirmationGate.java').read_text()
        self.assertIn('public boolean acknowledgeSuccess(PreparedTrade trade)', gate)
        self.assertLess(gate.index('trade.acknowledged = true;'), gate.index('TransactionMessageSender.sendAcknowledged(trade.receipt)'))

    def test_provider_and_tax_are_explicitly_fail_closed(self):
        code = (SRC / 'Confirmation/ConfirmationGate.java').read_text()
        self.assertIn('Properties.TAX_AMOUNT != 0.0 || Properties.SERVER_TAX_AMOUNT != 0.0', code)
        self.assertIn('"2.22.0".equals(essentials.getDescription().getVersion())', code)
        self.assertIn('com.earth2me.essentials.economy.vault.VaultEconomyProvider', code)
        vault = (SRC / 'Listeners/Economy/Plugins/VaultListener.java').read_text()
        method = vault.split('public void onCurrencyHoldCheck(', 1)[1]
        self.assertLess(method.index('if (transactionCanFail())'), method.index('provider.depositPlayer('))

    def test_pinned_artifact_preserves_original_resources_and_main(self):
        original = BASE / '.cache/ChestShop-3.13-pre-1-build474.jar'
        guarded = BASE / 'target/ChestShop-3.13-pre-1-build474-confirmation.1.jar'
        self.assertTrue(original.is_file(), 'Run bootstrap.sh first')
        self.assertTrue(guarded.is_file(), 'Run the Maven build and package.py first')
        self.assertEqual('b31a40e9561d399d8b04fada4e863ae28c523618976ff3a3b1a6f16fe3aa8ab2', hashlib.sha256(original.read_bytes()).hexdigest())
        source_files = list((BASE / 'src').rglob('*.java'))
        patched_classes = {p.relative_to(BASE / 'src').with_suffix('.class').as_posix() for p in source_files}
        with zipfile.ZipFile(original) as upstream, zipfile.ZipFile(guarded) as patched:
            for entry in upstream.infolist():
                family = entry.filename.split('$', 1)[0] + '.class' if '$' in entry.filename else entry.filename
                if entry.is_dir() or family in patched_classes or entry.filename == 'plugin.yml':
                    continue
                with self.subTest(entry=entry.filename):
                    self.assertEqual(upstream.read(entry.filename), patched.read(entry.filename))
            self.assertIn("version: '3.13-pre-1 (build 474)+confirmation.1'", patched.read('plugin.yml').decode())
            manifest = json.loads(patched.read('META-INF/confirmation-guard/build.json'))
            self.assertEqual('chestshop474-confirmation.1', manifest['build_id'])
            for source, expected in manifest['overlay_sources'].items():
                self.assertEqual(expected, hashlib.sha256((BASE / source).read_bytes()).hexdigest())

    def test_complete_source_archive_is_independently_buildable(self):
        source = BASE / 'target/ChestShop-3.13-pre-1-build474-confirmation.1-sources.zip'
        with zipfile.ZipFile(source) as archive:
            for path in ['pom.xml', 'mvnw', 'BUILDING-GUARD.txt', 'ChestShop-upstream/LICENSE',
                         'ChestShop-upstream/plugin/src/main/java/com/Acrobot/ChestShop/ChestShop.java',
                         'ChestShop-ConfirmationGuard/pom.xml', 'ChestShop-ConfirmationGuard/bootstrap.sh',
                         'ChestShop-ConfirmationGuard/COPYING.LESSER', 'ChestShop-ConfirmationGuard/NOTICE']:
                self.assertIn(path, archive.namelist())
            root = archive.read('pom.xml').decode()
            self.assertIn('<modules><module>ChestShop-ConfirmationGuard</module></modules>', root)
            self.assertNotIn('.cache/settings.xml', '\n'.join(archive.namelist()))


if __name__ == '__main__':
    unittest.main()
