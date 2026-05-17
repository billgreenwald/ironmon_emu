For every feature that is built, your most common mistake is not looking at the original ironmon tracker lua scripts.  This is ESPECIALLY TRUE for memory addresses.  For every feature, before you make any edits or start to develop, you MUST review the code in the lua script and make sure you are keeping parity and copying their constants.  if you think that you should stray, ask me before you go ahead and do.

Also this code is generalizable across 5 games; do not hard code the pokemon emerald codes in to any of the scripts you write

The maxData/*.json files (max-fr-gen4.json, max-fr.json, max-fr-gen5-fr.json, max-em.json) have GameInfo fields like "GameName": "Pokemon Emerald (U)" that do NOT reflect the actual base ROM. These names are meaningless metadata — the JSON address values are the source of truth. Do not second-guess or "fix" the GameInfo names.
