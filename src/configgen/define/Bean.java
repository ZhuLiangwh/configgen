package configgen.define;

import configgen.Node;
import configgen.util.DomUtils;
import org.w3c.dom.Element;

import java.util.*;

public class Bean extends Node {
    public enum BeanType {
        NormalBean,         //Bean
        Table,              //表
        BaseDynamicBean,    //多态Bean的基类
        ChildDynamicBean,   //多态Bean的具体子类
    }

    public final BeanType type;
    public final String own;

    //对应Column.CompressType.UseSeparator,之后建议column配置用AsOne，这里就不需要了。
    public final boolean compress;
    public final char compressSeparator;

    public final Map<String, Column> columns = new LinkedHashMap<>();  //列
    public final Map<String, ForeignKey> foreignKeys = new LinkedHashMap<>(); //外键
    public final Map<String, KeyRange> ranges = new LinkedHashMap<>(); //额外约束

    //多态Bean基类包含这些子类定义
    public final String childDynamicBeanEnumRef;
    public final Map<String, Bean> childDynamicBeans = new LinkedHashMap<>();


    Bean(AllDefine _parent, Element self) {
        super(_parent, self.getAttribute("name"));
        own = self.getAttribute("own");

        compress = self.hasAttribute("compress");
        if (compress) {
            String sep = self.getAttribute("compress");
            require(sep.length() == 1, "分隔符compress长度必须为1");
            compressSeparator = sep.toCharArray()[0];
        } else {
            compressSeparator = ';';
        }
        childDynamicBeanEnumRef = self.getAttribute("enumRef");
        if (self.hasAttribute("enumRef")) {
            type = BeanType.BaseDynamicBean;
            DomUtils.permitAttributes(self, "name", "own", "enumRef");
            DomUtils.permitElements(self, "bean");
            for (Element e : DomUtils.elements(self, "bean")) {
                Bean b = new Bean(this, e);
                require(null == childDynamicBeans.put(b.name, b), "Bean名字重复", b.name);
            }
        } else {
            type = BeanType.NormalBean;
            DomUtils.permitAttributes(self, "name", "own", "compress");
            DomUtils.permitElements(self, "column", "foreignKey", "keyRange");
            init(self);
        }
    }

    Bean(Table _parent, Element self) {
        super(_parent, self.getAttribute("name"));
        own = self.getAttribute("own");
        type = BeanType.Table;
        compress = false;
        compressSeparator = ';';
        childDynamicBeanEnumRef = "";
        init(self);
    }

    private Bean(Bean _parent, Element self) {
        super(_parent, self.getAttribute("name"));
        own = self.getAttribute("own");
        require(_parent.type == BeanType.BaseDynamicBean, "不允许ChildDynamicBean又有ChildDynamicBean");
        type = BeanType.ChildDynamicBean;
        compress = false;
        childDynamicBeanEnumRef = "";
        compressSeparator = ';';
        DomUtils.permitAttributes(self, "name", "own");
        DomUtils.permitElements(self, "column", "foreignKey", "keyRange");
        init(self);
    }

    private void init(Element self) {
        for (Element ele : DomUtils.elements(self, "column")) {
            Column c = new Column(this, ele);
            require(null == columns.put(c.name, c), "列名字定义重复", c.name);
        }

        for (Element ele : DomUtils.elements(self, "foreignKey")) {
            ForeignKey fk = new ForeignKey(this, ele);
            require(null == foreignKeys.put(fk.name, fk), "外键名字定义重复", fk.name);
        }

        for (Element ef : DomUtils.elements(self, "keyRange")) {
            KeyRange r = new KeyRange(this, ef);
            require(null == ranges.put(r.key, r), "keyRange定义重复", r.key);
        }
    }

    Bean(Table table, String name) {
        super(table, name);
        type = BeanType.Table;
        childDynamicBeanEnumRef = "";
        own = "";
        compress = false;
        compressSeparator = ';';
    }



    @Override
    public String fullName() {
        if (parent instanceof Table) {
            return parent.fullName();
        } else {
            return parent.fullName() + "." + name;
        }
    }


    //////////////////////////////// extract
    private Bean(Node _parent, Bean original) {
        super(_parent, original.name);
        type = original.type;
        childDynamicBeanEnumRef = original.childDynamicBeanEnumRef;
        own = original.own;
        compress = original.compress;
        compressSeparator = original.compressSeparator;
    }

    Bean extract(Node _parent, String _own) {
        Bean part = new Bean(_parent, this);

        if (type == BeanType.BaseDynamicBean) {
            if (!own.contains(_own))
                return null;
            // 对于多态Bean,只用在基类上配置own，不需要在每个子Bean上都配置own
            childDynamicBeans.forEach((name, actionBean) -> {
                Bean bn = actionBean.extract(part, _own);
                part.childDynamicBeans.put(name, bn);
            });
        } else {
            // 标记了own的列 提取出来
            columns.forEach((name, c) -> {
                Column pc = c.extract(part, _own);
                if (pc != null) {
                    part.columns.put(name, pc);
                }
            });

            // ChildDynamicBean一旦需要，就算没有列，其实也隐含了枚举字符串，所以要包含上
            if (part.columns.isEmpty() && type != BeanType.ChildDynamicBean) {
                return null;
            }

            ranges.forEach((n, r) -> {
                if (part.columns.containsKey(n))
                    part.ranges.put(n, new KeyRange(part, r));
            });

            foreignKeys.forEach((n, fk) -> {
                if (part.columns.keySet().containsAll(Arrays.asList(fk.keys)))
                    part.foreignKeys.put(n, new ForeignKey(part, fk));
            });
        }
        return part;
    }

    void resolveExtract(AllDefine top) {
        for (Column col : columns.values()) {
            col.resolveExtract(top);
        }

        List<String> dels = new ArrayList<>();
        foreignKeys.forEach((n, fk) -> {
            if (fk.invalid(top)) {
                dels.add(n);
            }
        });
        for (String del : dels) {
            foreignKeys.remove(del);
        }
    }


    //////////////////////////////// save
    void save(Element parent) {
        update(DomUtils.newChild(parent, "bean"));
    }

    void update(Element self) {
        self.setAttribute("name", name);
        if (!own.isEmpty())
            self.setAttribute("own", own);
        if (compress)
            self.setAttribute("compress", String.valueOf(compressSeparator));
        if (!childDynamicBeanEnumRef.isEmpty())
            self.setAttribute("enumRef", childDynamicBeanEnumRef);

        columns.values().forEach(c -> c.save(self));
        foreignKeys.values().forEach(c -> c.save(self));
        ranges.values().forEach(c -> c.save(self));
        childDynamicBeans.values().forEach(c -> c.save(self));
    }
}