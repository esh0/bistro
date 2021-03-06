package pl.sportdata.mojito.entities.items;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.List;

public class ItemUtils {

    public static final String SEPARATOR_NAME = "----------";
    public static final String DESCRIPTOR_NAME = "<?>";

    public static boolean isItemGroupSeparator(@Nullable Item item) {
        return item != null && SEPARATOR_NAME.equals(item.name) && "N".equalsIgnoreCase(item.type);
    }

    public static boolean isItemDescriptor(@Nullable Item item) {
        return item != null && DESCRIPTOR_NAME.equals(item.name) && "T".equalsIgnoreCase(item.type);
    }

    @Nullable
    public static Item findSeparatorItem(@Nullable List<Item> items) {
        Item item = findItemWithName(items, SEPARATOR_NAME);
        if (isItemGroupSeparator(item)) {
            return item;
        }

        return null;
    }

    @Nullable
    public static Item findDescriptorItem(@Nullable List<Item> items) {
        Item item = findItemWithName(items, DESCRIPTOR_NAME);
        if (isItemDescriptor(item)) {
            return item;
        }

        return null;
    }

    @Nullable
    public static Item findItemWithName(@Nullable List<Item> items, @Nullable String name) {
        if (items != null && !TextUtils.isEmpty(name)) {
            for (Item iteratedItem : items) {
                if (iteratedItem != null && iteratedItem.name.equalsIgnoreCase(name)) {
                    return iteratedItem;
                }
            }
        }

        return null;
    }
}
