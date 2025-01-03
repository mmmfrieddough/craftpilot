package mmmfrieddough.craftpilot.util;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;

public class BlockMatrixUtils {
    public static final Function<Map.Entry<Property<?>, Comparable<?>>, String> PROPERTY_MAP_PRINTER = new Function<Map.Entry<Property<?>, Comparable<?>>, String>() {
        @Override
        public String apply(Map.Entry<Property<?>, Comparable<?>> entry) {
            Property<?> property = entry.getKey();
            Comparable<?> value = entry.getValue();
            return property.getName() + "=" + value.toString();
        }
    };

    public static String getBlockStateString(BlockState state) {
        StringBuilder sb = new StringBuilder();
        sb.append(Registries.BLOCK.getId(state.getBlock()));

        if (!state.getProperties().isEmpty()) {
            sb.append("[");
            sb.append((String) state.getEntries().entrySet().stream().map(PROPERTY_MAP_PRINTER)
                    .collect(Collectors.joining(",")));
            sb.append("]");
        }

        return sb.toString();
    }
}