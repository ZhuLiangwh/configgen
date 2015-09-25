package configgen.define;

import configgen.Node;
import configgen.Utils;
import org.w3c.dom.Element;

public class Ref extends Node {
    public final String name;
    public final String[] keys;
    public final String ref;
    public final String nullableRef;
    public final String keyRef;

    public Ref(Bean parent, Element self) {
        super(parent, "");
        String[] attrs = Utils.attributes(self, "name", "keys", "ref", "nullableref", "keyref");
        name = attrs[0];
        link = name;
        keys = attrs[1].split(",");
        ref = attrs[2];
        nullableRef = attrs[3];
        keyRef = attrs[4];
    }

    public void save(Element parent) {
        Element self = Utils.newChild(parent, "ref");
        self.setAttribute("name", name);
        self.setAttribute("keys", String.join(",", keys));
        if (!ref.isEmpty())
            self.setAttribute("ref", ref);
        else if (!nullableRef.isEmpty())
            self.setAttribute("nullableref", nullableRef);

        if (!keyRef.isEmpty())
            self.setAttribute("keyref", keyRef);
    }
}
