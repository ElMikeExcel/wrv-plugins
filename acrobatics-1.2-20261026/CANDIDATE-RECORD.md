# ExcelAcrobatics 1.2 candidate record

DRAFT RECORD. DO NOT MERGE. Candidate prepared and backed up; installation and live mcMMO acceptance are pending confirmation. This record does not establish production status.

Fix: cap the fresh style/Feather-Falling-adjusted water award before repeat reduction. Config repeat-multipliers changes from [1,0.75,0.5,0.25,0.1] to [1,0.25]. First jump unchanged; repeated same-spot water landings pay25% of the capped custom award. Existing per-player/world radius5 blocks and rolling10-minute window preserved. No farm blacklist or removal of earned XP. Nearby different routes can count as same spot; landings more than5 blocks away can evade the minimal existing spatial detector. Downstream mcMMO modifiers can change final profile delta.

Base source: PR43 commit0ee0cd1b7ee5d5edf1e889f903b69ba8b1ab26da. JAR SHA25687e663dfdc4e251dbb67368a982193c0d6b28419873722a46be6fed15c655736. Current config must also receive repeat table; JAR saveDefaultConfig does not overwrite it.

Validation:65 existing assertions and131 packaged-class regression comparisons passed. Limited exactPaper26.3-214/Java25 boot enabled candidate and failed closed without mcMMO. Exact live mcMMO binary/API integration and actual player XP awards NOT tested. No claim that standalone startup establishes live integration. Keep rollback and record actual installation separately.
