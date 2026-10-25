# Rollback 0.1.6 -> 0.1.5
1. Move ExcelTrainingWheels-0.1.6.jar out of plugins/, put the kept 0.1.5 jar back
   (sha256 fbe4b1e826ff87ac0294aabc51074720471aeee7c7f9c80a137e8d01a4e89afa). Restart.
2. Config: 0.1.5 ignores shield.compensation.source and bedrock-grace.* (unknown keys are
   harmless). 0.1.5 uses the dealt-multipliers table as the divisor for ALL elites.
3. Quick disable without a jar swap: bedrock-grace.enabled: false and/or
   shield.compensation.source: table, then "tw reload".
