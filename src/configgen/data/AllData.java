package configgen.data;

import configgen.Logger;
import configgen.Node;
import configgen.define.Db;
import configgen.define.Table;
import configgen.type.TDb;
import configgen.type.TTable;
import configgen.util.CSVParser;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DDb extends Node {
    private final Path dataDir;
    private final Map<String, DTable> dTables = new HashMap<>();

    public DDb(Path _dataDir, String dataEncoding) {
        super(null, "ddb");
        dataDir = _dataDir;


//        System.gc();
//        Logger.mm("--");
//        List<List<String>> allLines = CSV.readFromFile(dataDir.resolve("filter\\fullwordfilter.csv"), dataEncoding);
//        System.gc();
//        Logger.mm("==");
//        System.exit(1);

        try {
            Files.walkFileTree(dataDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes a) {
                    String path = dataDir.relativize(file).toString();
                    if (path.endsWith(".csv")) {
                        String p = path.substring(0, path.length() - 4);
                        String configName = String.join(".", p.split("[\\\\/]")).toLowerCase();
                        List<List<String>> allLines = CSVParser.readFromFile(file, dataEncoding);
//                        Logger.mm(file.toString());
                        dTables.put(configName, new DTable(DDb.this, configName, allLines));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public DTable getDTable(String tableName){
        return dTables.get(tableName);
    }

    public void autoCompleteDefine(Db defineDb, TDb typeDb) {
        Map<String, TTable> old = new HashMap<>();
        for (TTable tTable : typeDb.getTTables()) {
            old.put(tTable.name, tTable);
        }

        defineDb.tables.clear();
        for (DTable dTable : dTables.values()) {
            TTable ttable = old.remove(dTable.name);
            Table tableDefine;
            if (ttable != null) {
                tableDefine = ttable.getTableDefine();
                defineDb.tables.put(dTable.name, tableDefine);
            } else {
                tableDefine = defineDb.newTable(dTable.name);
                Logger.verbose("new table " + tableDefine.fullName());
            }

            try {
                dTable.parse(ttable);
                dTable.autoCompleteDefine(tableDefine);
            } catch (Throwable e) {
                throw new AssertionError(dTable.name + ", 根据这个表里的数据来猜测表结构和类型出错，看是不是手动在xml里声明一下", e);
            }
        }

        old.forEach((k, cfg) -> Logger.verbose("delete table " + cfg.fullName()));
    }
}
