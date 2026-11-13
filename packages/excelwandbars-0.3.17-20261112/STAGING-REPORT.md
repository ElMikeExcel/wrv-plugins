# STAGING REPORT — ExcelWandBars 0.3.17 records package

Staged Nov 12, 2026 by the board supervisor run 269 records lane,
mirroring the 0.3.15 package shape. Package folder:
`~/workspace/records-catchup-20261103/packages/excelwandbars-0.3.17-20261112/`
Nothing was published by this staging lane (no GitHub, no Drive,
no MANIFEST edit, nothing outside this package folder).

## Status

ExcelWandBars 0.3.17 installed Nov 12, 2026 (parent hand window, 18:32 CST boot, PC lane) and booted clean — Done (21.245s), 83 plugins, SEVERE 0; see INSTALLED.txt and BUILD-RECORD.md section 4. Mike's live Bedrock proof PASSED Nov 12, 2026, 6:45 PM CST, releasing the D29 hold.

## Package contents

- Total files in package (including this report): 133
- `src/` tree: 96 files, extracted from `ExcelWandBars-0.3.17-src.zip`
- `tests/` tree: 23 files
- Jar: NOT in this package (per staging instruction) — record only: `ExcelWandBars-0.3.17.jar` — 553,827 B, SHA-256 `949e9cd48790cd10bb2861ce32f4cc3442410aee219e91bbc48f6cb526330c9b`, build `EXC-WANDBARS-0.3.17-WRV-20261112-01-9d0b77`
- `RIG-RESULTS.md`: real-Magic mirror rig record (evidence of record for the layout gates)

## Verification

- `sha256sum -c SHA256SUMS` result: **131 entries checked, 131 OK, 0 failed, exit 0**.
- SHA256SUMS covers every file in the package except SHA256SUMS
  itself and this STAGING-REPORT.md (both excluded to avoid
  self-reference; this report is listed in FILE-LIST.txt).
- FILE-LIST.txt lists all 133 files in the package.
- Build-folder artifacts verified before staging: build folder
  `sha256sum -c SHA256SUMS` OK for the jar and source zip, with
  sizes/hashes matching the install packet exactly.

## Files (path — size B — SHA-256)

- `./BUILD-RECORD.md` — 4829 — `a35960fa048d34d0ff1f39b9714ee433e466cf3db83f6da24dfc0088203ba9dd`
- `./ExcelWandBars-0.3.17-src.zip` — 283848 — `e6758a449fb0cf182912330f877787148af87647f91c1b073a1315cdba002499`
- `./FILE-LIST.txt` — 6316 — `471750586aecf69b8984aec929b8149dbaafa687e34ca5eb4c8bdf616cdc5dd6`
- `./INSTALLED.txt` — 375 — `60c6cf6dbe74addb0e622f27c7b90647a9e80d5acfcbf16f852b8adbe7d4830e`
- `./PUSH-NOTES.md` — 2222 — `158f2e64378143d2c6008ed8cd8a6e1345c52b9ad521ab2dbad3734080e169f3`
- `./README.md` — 1090 — `12fe1c93b5f28a849316ff407004a087c721108276630482d2514bb09964f8fd`
- `./RIG-RESULTS.md` — 7185 — `22910c05e28089a95adc05a00c86b80990aff5546948c0d6c7a2876290f0def7`
- `./SHA256SUMS` — 14929 — `43ad776578be075dfccebbc7f739f45fc11a8e5a3547a6744d1b4185adf827af`
- `./SOURCE.md` — 1766 — `18299beb9b394410a82e7f540b7d8314b2612e6e903d0cb440fa36f1c0ac508e`
- `./STAGING-REPORT.md` — this report (excluded from SHA256SUMS, see Verification)
- `./TEST-RESULTS.txt` — 2886 — `46bc6ece94669dbced45dc0c7e68cda840b21997af0e64831a8d3124778080b3`
- `./config.yml` — 27089 — `ec8766d182748f6c411ac16d44940b96657cb2fc8946764b82b14ed0ac3eb6dd`
- `./plugin.yml` — 8628 — `2cd5782d23a39b3cbe6f9af8c39f97ebca98628622b0f952ce67384751778379`
- `./repack.py` — 3024 — `8a42d67356829696e60dc24e3dcd9d5fafc218a8eb447cbe95a666e988591760`
- `./src/com/winkreyes/excelwandbars/AppliedMagicStage.java` — 1873 — `9bcbd641725234ac3de209a671237949bb2af9d5560e4a049531e9d911426e74`
- `./src/com/winkreyes/excelwandbars/ArrangeHook.java` — 45611 — `e16341033843908dd3eba547bd0711b1a48eebcfcc2d66850f3dd413c7c3d5f1`
- `./src/com/winkreyes/excelwandbars/BedrockForms.java` — 4549 — `c857fdba723d8232eac57fd4ecd78d4f368ddb96a20bf08ebef95dd764921369`
- `./src/com/winkreyes/excelwandbars/BuildInfo.java` — 966 — `441ae5d6146452a48761f10910b12412d6adc95ef87a273e94f365c68374cf22`
- `./src/com/winkreyes/excelwandbars/CompassGuard.java` — 6544 — `09f0e74ae8d1e2a4eb5a8a618561f5269b2aa96a6d404360dcdfb62ae88456e9`
- `./src/com/winkreyes/excelwandbars/CurrentPlayerPracticeProviders.java` — 9673 — `2231ff76cd004bb84d6eab627be4145685ba40a77b10d4d719bbdd94ce56e992`
- `./src/com/winkreyes/excelwandbars/ExcelWandBars.java` — 51663 — `acaad141631025ff1b2c92b06fced309ff03e4a06b445b6f391d1e08c078965e`
- `./src/com/winkreyes/excelwandbars/ExitSweep.java` — 5669 — `2a71f895d490eb46742ffc6146bf5ccb226d9f05fd4f4103535f20270fc08c12`
- `./src/com/winkreyes/excelwandbars/FirstObservationCoordinator.java` — 17971 — `3037a415112d843299f841649c8d414ec484de8341e83bf60603c0493240a753`
- `./src/com/winkreyes/excelwandbars/FlexibleInventoryGuard.java` — 2975 — `948c7c4565a1edc4a51f207212aa23e818b4a16430b7f8449533e24aa5c796d1`
- `./src/com/winkreyes/excelwandbars/GeyserHoldHook.java` — 6617 — `0973336da1fc891e0855dbc83bd50218ca5751c9dbe20355961c3e7ce7330a81`
- `./src/com/winkreyes/excelwandbars/GeyserTapHook.java` — 4149 — `3fc450110eb09a8c8d7b6bc194a6d0d6712f66e4f92e9b8b76147ea81bca985d`
- `./src/com/winkreyes/excelwandbars/LegacyWandBaselines.java` — 2814 — `ccfb20c258eb2ae4fe2c130b34041c632f6aa6e7fe3a6c8419436f46d3db093b`
- `./src/com/winkreyes/excelwandbars/LoadoutHook.java` — 33325 — `3b643ff20e2cb245c25b4231a1e9cdf1766a375e2844c3282dd8243fe9d1e8a2`
- `./src/com/winkreyes/excelwandbars/MagicHook.java` — 85205 — `d05508580d5be2c9934596c09e99c66e8a3efd1275358589f4a99aab15f69bf0`
- `./src/com/winkreyes/excelwandbars/ManagedMagicRuntime.java` — 10435 — `4dea159de6e6c96957041ab9835c3666e4cdb0eed39ed668dab8b0cc6708c309`
- `./src/com/winkreyes/excelwandbars/MigrationTouchGuard.java` — 1885 — `d7d0173b8e64ab2aebeaf798787c71faabeba2f1983b703e45a0f243ce3729bd`
- `./src/com/winkreyes/excelwandbars/NameTagHook.java` — 22255 — `ffca14195f139a9e413ff7d0bcaa158d27ffe2e61ded34b36e2002bd1a3e6259`
- `./src/com/winkreyes/excelwandbars/NativePracticeAdmission.java` — 5653 — `4a63e70d4cf6b1884178429b087f9df450abc72e29ded7abcec19012f8cbcb74`
- `./src/com/winkreyes/excelwandbars/OwnedPracticeLifecycle.java` — 4766 — `b8d5bd9cb68063e3ca802235aac25ceac707565728fc519e9637f72a04624fa1`
- `./src/com/winkreyes/excelwandbars/PagerHook.java` — 28087 — `74f2b8fba719d55cdde89074c8731412c136cc1f8a938fdde9a3acccb8bedd6d`
- `./src/com/winkreyes/excelwandbars/PracticeBootstrap.java` — 4037 — `9152f4f1827ff252ccd6dcdfc083bb1977c45ebdf3b20453417bc9377dd2c5e7`
- `./src/com/winkreyes/excelwandbars/PracticeBootstrapConfig.java` — 3337 — `ae2acd5b07676d149d5d9e41589aca365951263a79b556d010b112cdda9e436a`
- `./src/com/winkreyes/excelwandbars/PracticeDisplayBridge.java` — 2069 — `9b5a2ffb9e730e449d6eff57a02bd0f9ff6141e6941741ffb536fc7d5d58a4ba`
- `./src/com/winkreyes/excelwandbars/PracticeIdentityJournal.java` — 9682 — `930563ad2429163355b58f7b115ba730751b1f880e7c6648f61489093c2806fc`
- `./src/com/winkreyes/excelwandbars/PreparedPracticeFactory.java` — 11747 — `8e8ca4c8ee548d34add5a675b5bd49110f8cbd8003279b24f52b297b1814a5fc`
- `./src/com/winkreyes/excelwandbars/PresentedSweep.java` — 8541 — `46ca59164892fee13974dc4d36d59bd340084db40264d2697782d40ae1327c7d`
- `./src/com/winkreyes/excelwandbars/QuitCloseHook.java` — 1499 — `886dd3fc57ab6422ce720b5a1b90bde0c0748b0aa74029010848fa4eb3894313`
- `./src/com/winkreyes/excelwandbars/RankUpHook.java` — 49041 — `d8cfd4e099e4ce3083f1b44b3c087d08af4c76d9fa1b355cdfe8e0f298bcaed2`
- `./src/com/winkreyes/excelwandbars/RenderedExitGuard.java` — 724 — `f574c274ca573f262721ab5f87a8cbfa8caa1d92ad24309e6f1d0116fcf5b9f2`
- `./src/com/winkreyes/excelwandbars/SharedHistoryAttachment.java` — 2284 — `b6c129ca2da709da006cff47f57f06f3d28cb848b541d1313b1a4cf2e1c705ef`
- `./src/com/winkreyes/excelwandbars/SourceAwarePracticeBinding.java` — 1906 — `11f844010949645f1fe0edbd3159960b065327f532596fa0154ab4c71201c1dd`
- `./src/com/winkreyes/excelwandbars/SpellScreen.java` — 9026 — `0b38f79460007129ae1d64ee41b15f9d4d8bae7fe993f0c96d2698cac75b6620`
- `./src/com/winkreyes/excelwandbars/SpellTipHook.java` — 8792 — `a599b01b6d8228d82b253135b9b346a5f48c0512c1e3378f7801c6f7bacab3ef`
- `./src/com/winkreyes/excelwandbars/StoredNativeRanks.java` — 2684 — `7a13a68363a8f760ee2f52156fd8d2cfbc32c641ad6dea98106535f4963c1d77`
- `./src/com/winkreyes/excelwandbars/StyleHook.java` — 37378 — `be7e3ddbacfef65ef723ad4c8e70e80a5d75d4f31d1411d9a07cf26343e092c8`
- `./src/com/winkreyes/excelwandbars/TapBarHook.java` — 78002 — `1fc127adcf4bdd1da2448f87e598a4238736150ac0f593728fb1c5d219fd0b04`
- `./src/com/winkreyes/excelwandbars/TeamUsefulTargets.java` — 1330 — `74915868a5be79279bf0330087c70ef2ed3e9c2d66866fc12976d07cc4ed9522`
- `./src/com/winkreyes/excelwandbars/TipHook.java` — 9257 — `2960858157314844b267be48765de0230642eb266d18eff6253979ed2ceda849`
- `./src/com/winkreyes/excelwandbars/WandSlotHook.java` — 12596 — `87749b25df5eccc84dfde685a06e8a3d3cb89a25330c50f8990ea9f781fd0571`
- `./src/com/winkreyes/excelwandbars/core/ApplyCoalescePlan.java` — 3023 — `1ab0c30baf7831a4e37af8f6b043c95f3c7e46bc974a7844993c1e895ca5667f`
- `./src/com/winkreyes/excelwandbars/core/ApplyGuard.java` — 1242 — `90a07ba0cfca15837efe35faac6655683bffdaa395ed821a8e7bfc35a5d4c833`
- `./src/com/winkreyes/excelwandbars/core/ArrangePlan.java` — 7718 — `f9a77ffb0980ddfb40a073d5d232d163f16c3b12ecb7a4d0df80977b09a78a2c`
- `./src/com/winkreyes/excelwandbars/core/BarLoadout.java` — 14509 — `efd803b68cd6c0f1da3d23f7d3221549139f39201035b0f29451d77730209942`
- `./src/com/winkreyes/excelwandbars/core/BarPosition.java` — 2369 — `3ceeed14607b90a95304896e579768974449434a12ea98bc8a7a0854487c8d53`
- `./src/com/winkreyes/excelwandbars/core/BarText.java` — 1161 — `94e155cefd94c2b740e78027b4ec076dcd02db4c587fa81777675e957c513652`
- `./src/com/winkreyes/excelwandbars/core/BookHoldPlan.java` — 6196 — `5d8c413a7cd863dccf2d8dec85953e9d1dd6d44442b324ada9ec743432b25d30`
- `./src/com/winkreyes/excelwandbars/core/CompassDedupe.java` — 1061 — `16977859450af599c3b90db2ae07cc9c887844cdd6d5ae303da734e575e3a020`
- `./src/com/winkreyes/excelwandbars/core/ControlsText.java` — 8406 — `92f48bab7d0d53ba44004f94b7907200faa49ca49b06d0f2f1dcd8fc3f7ceb2c`
- `./src/com/winkreyes/excelwandbars/core/CooldownDisplayPlan.java` — 4655 — `5428a8c0164f41340d7d98e021f44cf3836e0323eb23b23833950eb16a6a41cc`
- `./src/com/winkreyes/excelwandbars/core/CooldownTagPlan.java` — 5044 — `bac6bea82d54dbf8248b72551a494c8b5f1e6d69e1ff2679a7711fefb29f0727`
- `./src/com/winkreyes/excelwandbars/core/CrouchHoldGate.java` — 2203 — `85b9b9aaac48660d34f4e40af1dfced9b7fda88772fce7d1afcc24d06e1d7328`
- `./src/com/winkreyes/excelwandbars/core/DragWritePlan.java` — 11473 — `c66ece26567b8ef7383a98b6f934916664b3bfcaff1e3bea803c0a32ce798e9a`
- `./src/com/winkreyes/excelwandbars/core/Durations.java` — 1043 — `bf40b06a5d1fa22509bd533ebad157bb9319f5c36e2d04694741f855978649a1`
- `./src/com/winkreyes/excelwandbars/core/EarnMath.java` — 3299 — `73a8e4ab495d56921d93648ea710485f6e3fdc2db164e2855efc375ae778859f`
- `./src/com/winkreyes/excelwandbars/core/EarnedBarGate.java` — 1950 — `33480d203b1fa9a1f9941355e97fa26fe21ba4666c9a201d5410467e71b5cb86`
- `./src/com/winkreyes/excelwandbars/core/ExitSlotPolicy.java` — 644 — `ecc66725a564f9bbde18097b8ca3f97074ed4b4e31e904050e318fc2d8ef93cc`
- `./src/com/winkreyes/excelwandbars/core/ExitSweepPlan.java` — 1861 — `0b289c1292b01ac576a2d422323302fe593aa0aff146768752a0ee9d516ed881`
- `./src/com/winkreyes/excelwandbars/core/FlexibleBarLayout.java` — 2791 — `4c93cdc9430e7a30c9d779f2a585a394c19f1c28de112c5a05df91b44585dade`
- `./src/com/winkreyes/excelwandbars/core/FlipGate.java` — 3243 — `4590227bb4daf40f06662a15a1b5fb250b9155d9e88d0bfc171323092a1fac41`
- `./src/com/winkreyes/excelwandbars/core/FormContrast.java` — 2685 — `448bf99e3d2f1c1294aa7f3a9631dc1677629da3345b40602543096ac5553a9a`
- `./src/com/winkreyes/excelwandbars/core/HoldReadPlan.java` — 2318 — `d1a05caecfe57bd6613ea796fd18758b4e84429d4ec5f5031777c11daeb83ce5`
- `./src/com/winkreyes/excelwandbars/core/JoinGuardPlan.java` — 2541 — `e1bbbd5b3a3c8e8926609170d9efb5682d71cbe61d4e4a29e47637d132114865`
- `./src/com/winkreyes/excelwandbars/core/JoinSweepPlan.java` — 2010 — `fd0a3f0e959246cd7f9fc90e9308376929c0d765bdb76bb5a65fee9d20f127ac`
- `./src/com/winkreyes/excelwandbars/core/LoadoutText.java` — 9507 — `0145881791a270490bedbfcb88dad23588399bfd60c07a73563c87a3968e1607`
- `./src/com/winkreyes/excelwandbars/core/PageLayout.java` — 4724 — `670465361d3ce493d3b9e8ce216cc61e6a3974f190e1bb3dd292642e5a469ee5`
- `./src/com/winkreyes/excelwandbars/core/PagerPlan.java` — 8526 — `1c30d865ff97e4cd78ce56eec067ee75fd3c6b607fd0b57eeb9438a78ba9009a`
- `./src/com/winkreyes/excelwandbars/core/PracticeDisplay.java` — 1531 — `b98fe7f2a4902c4155c81c4486acb307dac78b66e1c68536c67c82397953eca5`
- `./src/com/winkreyes/excelwandbars/core/ProgressGate.java` — 1903 — `2560b12831af223f0f0a6de3f94b8d5dc1e1a6fc86c994c9ea81984ad0266be0`
- `./src/com/winkreyes/excelwandbars/core/ProgressText.java` — 5980 — `8a51629e6aec44b12b6dd00d203c1fc8c44de7df13bd5269b7aeab5c8c498165`
- `./src/com/winkreyes/excelwandbars/core/QuitClosePlan.java` — 2529 — `908f4ec922f9128196afb56d7b559ba13b606935cae4a45f20dde3d453796a20`
- `./src/com/winkreyes/excelwandbars/core/RankLadder.java` — 2723 — `62cac1728e33d986db5cfb522c9dd858915ab9696f534e2cd47daabde49bf9cb`
- `./src/com/winkreyes/excelwandbars/core/RankText.java` — 2263 — `707d7a2771399329f19951b37d336a6b8c42cbc1ef63c6b0ec1ae883d804438e`
- `./src/com/winkreyes/excelwandbars/core/SharedSpAward.java` — 686 — `596926e05f62205589cc98b2ce94e723d66369a7dd9aa88d208f93b5b54f2136`
- `./src/com/winkreyes/excelwandbars/core/SharedSpShadow.java` — 4781 — `f27ae2d5b9f54ee9691cc03f914559836c4dd0e2e6089f0353ca383eca2e3c4a`
- `./src/com/winkreyes/excelwandbars/core/SlotGuardPlan.java` — 7480 — `5a11adb31cbabb63ad01da8c96a193b885a265227173bd2fe875c59451b16e17`
- `./src/com/winkreyes/excelwandbars/core/SpellEffects.java` — 30062 — `97bff8d3009526b6252e8e07f117882ebd00ce16e9e8fccbfc97ebaa463186ba`
- `./src/com/winkreyes/excelwandbars/core/SpellFacts.java` — 2840 — `5b9d632fa2d84bf087550d516e7d33bd11dbbcd103d2895402376fe7569ddcaa`
- `./src/com/winkreyes/excelwandbars/core/SpellInfoText.java` — 6824 — `2d96529f58bbe6c7bc65dbe07ae53b290cab4c15b624d43274c0b14cdc1c2838`
- `./src/com/winkreyes/excelwandbars/core/SpellTip.java` — 4412 — `a3ccf5f18dbe744c61cf5fc8361363960813ad44eca7d64d0bee55bc215b087a`
- `./src/com/winkreyes/excelwandbars/core/SpellbookGroups.java` — 6168 — `fd8aa3da1d01d3b29b8a6f13bece87c4d1b7bfc61eac9a5f34db1a409308192f`
- `./src/com/winkreyes/excelwandbars/core/SpellbookText.java` — 15282 — `5c7f5c3b12576bcb5d59914ae6bd793fc517e3f2a4b6e61057f74fe39fbb941e`
- `./src/com/winkreyes/excelwandbars/core/StylePrefs.java` — 2456 — `b6d84304089c97a303d1a4c701080a0284d765e36ff553351c2dc4f07f406d19`
- `./src/com/winkreyes/excelwandbars/core/StyleProfile.java` — 9031 — `94072a49550417c16f1ecccc3cde55c77c7fe8baa285a3b99d1c599ad027ca8d`
- `./src/com/winkreyes/excelwandbars/core/TipGate.java` — 7362 — `3a63e7d976a36308735ef8f36de34a9481d398409d669c382c5d338920f6f749`
- `./src/com/winkreyes/excelwandbars/core/UnarmedDragPlan.java` — 2401 — `32d46012ae564e9da9a3e29b1ac74def51356cfd8d01f77ef58e1248eae48901`
- `./src/com/winkreyes/excelwandbars/core/VerifyRetryPlan.java` — 5481 — `fc803ab63238224c08178313bae0e3be08688af819e9cdbf9b68e926c2ffe4f4`
- `./src/com/winkreyes/excelwandbars/core/WandSlotPlan.java` — 5696 — `a48215e51f68ac042da17c9bd3cba58fb722c9b9c57a45d783fe14ec034f5f3e`
- `./src/com/winkreyes/excelwandbars/core/WandStyle.java` — 5009 — `d303ee691a6e0c0b8061482aa8d9d4797f3eb426ce0e20254a4c3599af9acb19`
- `./src/wrv/magic/evidence/EvidenceFingerprint.java` — 2973 — `2215b7550e8e11b11caa4b96e27f194a3bcc7fcc8d3d1d8c70575e84203f1c12`
- `./src/wrv/magic/evidence/IssuedUsefulSinkV2.java` — 4209 — `d52c140e97d995343b356c755917208f595babb90ae22e644d626875e6d826aa`
- `./src/wrv/magic/evidence/PracticeConsumerAckV2.java` — 5492 — `e59227f62a95629dbd94aac17c9e3fe8f431cb11a825068f35f086873be6f421`
- `./src/wrv/magic/retention/AuthenticatedWindow.java` — 14260 — `065a38478e859e685fbcf9e93d08803214dcb55f2c1f650371a7f6b24b24b9cb`
- `./src/wrv/magic/retention/BoundedPractice.java` — 15965 — `f76ec7e235648fd39333c9b7a602b332a02ed08e1468003680e4645a9ae36ab5`
- `./src/wrv/sp/route/NativeCastRouteAdapter.java` — 6138 — `4db267733b4e09fcb3754550ac0ea983dab6994d66fcc2e0aa08aec59245d520`
- `./src/wrv/sp/route/SingleSpRoute.java` — 5903 — `411bd735948b1ab868efa451b1c538c63f0159e0e055cb00e01f3e8f3557ae86`
- `./tests/ApplyCoalescePlanTest.java` — 2225 — `78256df17a67a3afe6456fd8b6cb6c4fab51f6323dcc05fd2eb5992aaf4d65d0`
- `./tests/ArrangePlanTest.java` — 3983 — `5178b493a207fdf42966ec436d3ccde97457dcbb97d86fbee33899dfe0638eca`
- `./tests/BarLoadoutNewTest.java` — 3707 — `51f5187220b1a41072c09c907542b572ebf3d2a090ce3255bb92faf8738750b8`
- `./tests/BarPositionTest.java` — 4550 — `7cc84314df03dbc201dc5432355e4c4793812fb83f6328c49f3be3407349dc0d`
- `./tests/BookHoldPlanTest.java` — 4777 — `50a238f4cf525622ec3ad6080e9ea9921032da05076db3ff5d4d3c8c450e20d7`
- `./tests/CooldownDisplayPlanTest.java` — 4273 — `a21aefd6aa035b1eeeefe0e507bc1cd5d90da10ce3eac0fea035e1bbcd0fee0e`
- `./tests/CooldownTagPlanTest.java` — 4903 — `61b0aabb0a9f8dbd77e34045c26aaa8f782da59468d62388c8f28bd9b803bc01`
- `./tests/DragWritePlanTest.java` — 5769 — `d8e620f688f2fa1e07d93fad5cd92883fa81aa078615ff4481605ba31783db2d`
- `./tests/DragWriteTwoTapTest.java` — 3782 — `b012c2b1941fbbfbcd758a6117782cc33cd876b4bf5452c70c99558c1e866f20`
- `./tests/ExitSweepPlanTest.java` — 2798 — `ee8afd191313371c4514794e27bf6b1f0b3ab7aaab2d2bb15ece4b1b4c22213a`
- `./tests/FormContrastTest.java` — 3803 — `0b2f87d007b219c41a701af0d16c01878e98f604b3eba3aff5b6efff970e0123`
- `./tests/HoldReadPlanTest.java` — 1651 — `d7f4d31747167d92a7e42b465136381dedcbc14714be292f0eae706d0c9e970c`
- `./tests/JoinGuardPlanTest.java` — 2344 — `0a81d97d108ebc516233a0d54a56dfb14911286ad3d7d47c99e86a330b7477d5`
- `./tests/JoinSweepPlanTest.java` — 1531 — `fed2e18ba75e3af573307ee7c76344597503fbc33a938741b0549095423c99b4`
- `./tests/PagerDefault0312Test.java` — 3673 — `febcb15a7cf55b00749f94291322a11523d91e834a6d481823ded72b6c510f2a`
- `./tests/PagerPlanTest.java` — 2438 — `7000a8779daa5a12d1e455bfb987ad89a41e8538fd048148021a89048d0f8fdd`
- `./tests/QuitClosePlanTest.java` — 1546 — `89e2cc8a435482d1b2df3563250837361a32875d73000ab1a9742a1b103417c7`
- `./tests/ShapeAPageCapTest.java` — 5257 — `1e291b3bba316f355a25c7d1d65215ef3ee76f54eb879890528dc35f67bbfa8e`
- `./tests/SlotGuardPlanTest.java` — 5097 — `7b2740e5b0dd62c8143963e2b981fc78731d7bcae9e2016f0ddd7b00dbe11948`
- `./tests/SpellbookNewTest.java` — 2207 — `a5f7ecdff5f470bd6c2c79f56e33d8f2864eade775b8061171ba8740ea64e406`
- `./tests/SpellbookPager039Test.java` — 8256 — `4114b55fe722b0662ffbc16d7e58be9cfba60b22fd892ae71969e4bac7c70ba8`
- `./tests/UnarmedDragPlanTest.java` — 2205 — `9e7f8a8688f167d508f2d39180ffd935a1dd1819a100c11b044e0cfef6cd846b`
- `./tests/VerifyRetryPlanTest.java` — 4962 — `d2c6a9aed621dad5042a4c1bde05cb6899a14362c6dec87509a6f52d97155064`
