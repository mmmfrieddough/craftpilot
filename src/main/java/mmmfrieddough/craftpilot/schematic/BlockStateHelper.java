package mmmfrieddough.craftpilot.schematic;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

public class BlockStateHelper {
    public static BlockState parseBlockState(String blockStateString) {
        // Parse block state string like "minecraft:stone[facing=north]"
        String[] parts = blockStateString.split("\\[", 2);
        String blockId = parts[0];

        Block block = Registries.BLOCK.get(new Identifier(blockId));
        BlockState state = block.getDefaultState();

        if (parts.length > 1) {
            String propertiesString = parts[1].substring(0, parts[1].length() - 1);
            state = applyProperties(state, parseProperties(propertiesString));
        }

        return state;
    }

    private static Map<String, String> parseProperties(String propertiesString) {
        Map<String, String> properties = new HashMap<>();
        String[] propertyPairs = propertiesString.split(",");

        for (String pair : propertyPairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                properties.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }

        return properties;
    }

    private static BlockState applyProperties(BlockState state, Map<String, String> properties) {
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            state = applyProperty(state, entry.getKey(), entry.getValue());
        }
        return state;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static BlockState applyProperty(BlockState state, String propertyName, String value) {
        for (Property property : state.getProperties()) {
            if (property.getName().equals(propertyName)) {
                Optional<?> parsedValue = property.parse(value);
                if (parsedValue.isPresent()) {
                    return state.with(property, (Comparable) parsedValue.get());
                }
            }
        }
        return state;
    }

    private static Object parsePropertyValue(Property<?> property, String value) {
        return property.parse(value).orElse(null);
    }
}