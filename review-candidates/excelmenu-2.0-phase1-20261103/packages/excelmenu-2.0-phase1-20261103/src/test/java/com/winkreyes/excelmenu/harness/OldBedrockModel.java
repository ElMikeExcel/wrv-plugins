package com.winkreyes.excelmenu.harness;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The OLD Bedrock column, computed independently of the 2.0 code:
 * ExcelBedrockMenu 1.5.27's BUTTONS/COMMANDS arrays (machine-extracted
 * to fixtures/bedrock-arrays.json by tools/extract_bedrock_arrays.py —
 * no hand transcription), with the labels the class actually computes
 * (group colour prefix, state-bearing Silk/Aim labels, guild label
 * unchanged while locked — lockedLabel() returns the base string).
 * YAML is a superset of JSON, so SnakeYAML reads the fixture directly.
 */
final class OldBedrockModel {

    private final List<String> buttons;
    private final List<String> commands;

    @SuppressWarnings("unchecked")
    OldBedrockModel(Path arraysJson) throws Exception {
        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(arraysJson)) {
            root = new Yaml().load(in);
        }
        buttons = (List<String>) root.get("buttons");
        commands = (List<String>) root.get("commands");
        if (buttons == null || commands == null || buttons.size() != commands.size()) {
            throw new IllegalStateException("bedrock arrays fixture is incomplete");
        }
    }

    List<HarnessSupport.Row> visible(HarnessSupport.ViewerState s) {
        List<HarnessSupport.Row> rows = new ArrayList<>();
        for (int i = 0; i < buttons.size(); i++) {
            String command = commands.get(i);
            String label = HarnessSupport.bedrockGroupColor(command)
                    + HarnessSupport.bedrockStateLabel(buttons.get(i), command, s);
            rows.add(new HarnessSupport.Row(
                    HarnessSupport.COMMAND_TO_ID.get(command), label,
                    HarnessSupport.bedrockKey(command, s)));
        }
        return rows;
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
