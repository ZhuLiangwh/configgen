package configgen.genlua;

import configgen.gen.LangSwitch;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class AContext {
    private static final AContext instance = new AContext();

    static AContext getInstance() {
        return instance;
    }

    private String pkgPrefixStr;
    private LangSwitch langSwitch;
    private boolean sharedEmptyTable;
    private boolean shared;
    private boolean pack;
    private boolean tryColumnMode;
    private boolean noStr;

    private String emptyTableStr;
    private final Set<String> forbidLocalNames = new HashSet<>(Arrays.asList("Beans", "this", "mk",
            "A", //表示共享Table
            "E", //表示emptyTable
            "R"  //表示为共享Table的一个包装方法
    ));

    private AStat statistics;

    void init(String pkg, LangSwitch ls, boolean shareEmptyTable, boolean share, boolean col, boolean pak, boolean no_str) {
        langSwitch = ls;
        sharedEmptyTable = shareEmptyTable;
        shared = share;
        tryColumnMode = col;
        pack = pak;
        noStr = no_str;

        if (sharedEmptyTable) {
            emptyTableStr = "E";
        } else {
            emptyTableStr = "{}";
        }

        if (pkg.length() == 0) {
            pkgPrefixStr = "";
        } else {
            pkgPrefixStr = pkg + ".";
            forbidLocalNames.add(pkg);
        }

        statistics = new AStat();
    }


    boolean isForbidName(String name) {
        return forbidLocalNames.contains(name);
    }

    LangSwitch getLangSwitch() {
        return langSwitch;
    }

    boolean isShared() {
        return shared;
    }

    boolean isPack() {
        return pack;
    }

    boolean isTryColumnMode() {
        return tryColumnMode;
    }

    boolean isNoStr() {
        return noStr;
    }


    String getEmptyTableStr() {
        return emptyTableStr;
    }

    String getPkgPrefixStr() {
        return pkgPrefixStr;
    }

    AStat getStatistics() {
        return statistics;
    }
}
