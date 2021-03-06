package configgen.value;

import configgen.define.Column;
import configgen.type.TList;

import java.util.ArrayList;
import java.util.List;

public class VList extends VComposite {
    private ArrayList<Value> list;

    VList(TList type, AData<TList> adata) {
        super(type, adata.cells);

        List<Cell> parsed;
        boolean compressAsOne = adata.compressAsOne || type.compressType == Column.CompressType.AsOne;
        if (compressAsOne) {
            require(adata.cells.size() == 1);
            Cell dat = adata.cells.get(0);
            parsed = Cells.parseNestList(dat);

        } else if (type.compressType == Column.CompressType.UseSeparator) { //为了兼容之前的设计
            require(adata.cells.size() == 1);
            Cell dat = adata.cells.get(0);
            parsed = Cells.parseList(dat, type.compressSeparator);

        } else {
            require(adata.cells.size() == adata.fullType.columnSpan());
            parsed = adata.cells;
        }

        list = new ArrayList<>();
        int vc = compressAsOne ? 1 :
                adata.fullType.value.columnSpan();  // 注意这里compressAsOne的自上而下一直传递的特性

        for (int s = 0; s < parsed.size(); s += vc) {
            if (!parsed.get(s).data.isEmpty()) { //第一个单元作为是否还有item的标记
                list.add(Values.create(type.value, parsed.subList(s, s + vc),
                        adata.fullType.value, compressAsOne));
            } else {
                for (Cell dc : parsed.subList(s, s + vc)) {
                    require(dc.data.isEmpty(), "list的item第一个为空格后，之后必须也都是空格", dc);
                }
            }
        }

        list.trimToSize();
    }

    VList(ArrayList<Value> vs) { // for primaryKey and keysRef
        super(null, toRaw(vs));
        list = vs;
    }


    private static List<Cell> toRaw(List<Value> vs) {
        List<Cell> res = new ArrayList<>(vs.size());
        for (Value v : vs) {
            v.collectCells(res);
        }
        return res;
    }

    public List<Value> getList() {
        return list;
    }

    @Override
    public void accept(ValueVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void verifyConstraint() {
        for (Value value : list) {
            value.verifyConstraint();
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof VList && list.equals(((VList) o).list);
    }

    @Override
    public int hashCode() {
        return list.hashCode();
    }


}
