package pl.sportdata.mojito.entities.groups;

import java.util.List;

import pl.sportdata.mojito.entities.items.Item;

public class Group {

    public final String name;
    public final int id;
    public final List<Item> items;

    public Group(int id, String name, List<Item> items) {
        this.id = id;
        this.name = name;
        this.items = items;
    }
}
