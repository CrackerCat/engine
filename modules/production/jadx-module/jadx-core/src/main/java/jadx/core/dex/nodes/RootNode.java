package jadx.core.dex.nodes;

import jadx.api.IJadxArgs;
import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.api.ResourcesLoader;
import jadx.core.clsp.ClspGraph;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.ConstStorage;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.StringUtils;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.files.DexFile;
import jadx.core.utils.files.InputFile;
import jadx.core.xmlgen.ResContainer;
import jadx.core.xmlgen.ResTableParser;
import jadx.core.xmlgen.ResourceStorage;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class RootNode {
    private static final Logger LOG = LoggerFactory.getLogger(RootNode.class);

    private final ErrorsCounter errorsCounter = new ErrorsCounter();
    private final IJadxArgs args;
    private final StringUtils stringUtils;
    private final ConstStorage constValues;

    private List<DexNode> dexNodes;
    @Nullable
    private String appPackage;
    private ClassNode appResClass;
    private ClspGraph clsp;

    public RootNode(IJadxArgs args) {
        this.args = args;
        this.stringUtils = new StringUtils(args);
        this.constValues = new ConstStorage(args);
    }

    public void load(List<InputFile> inputFiles) throws DecodeException {
        dexNodes = new ArrayList<DexNode>();
        for (InputFile input : inputFiles) {
            for (DexFile dexFile : input.getDexFiles()) {
                try {
                    LOG.debug("Load: {}", dexFile);
                    DexNode dexNode = new DexNode(this, dexFile);
                    dexNodes.add(dexNode);
                } catch (Exception e) {
                    throw new DecodeException("Error decode file: " + dexFile, e);
                }
            }
        }
        for (DexNode dexNode : dexNodes) {
            dexNode.loadClasses();
        }
        initInnerClasses();
    }

    public void loadResources(List<ResourceFile> resources) {
        ResourceFile arsc = null;
        for (ResourceFile rf : resources) {
            if (rf.getType() == ResourceType.ARSC) {
                arsc = rf;
                break;
            }
        }
        if (arsc == null) {
            LOG.debug("'.arsc' file not found");
            return;
        }
        final ResTableParser parser = new ResTableParser();
        try {
            ResourcesLoader.decodeStream(arsc, new ResourcesLoader.ResourceDecoder() {
                @Override
                public ResContainer decode(long size, InputStream is) throws IOException {
                    parser.decode(is);
                    return null;
                }
            });
        } catch (JadxException e) {
            LOG.error("Failed to parse '.arsc' file", e);
            return;
        }

        ResourceStorage resStorage = parser.getResStorage();
        constValues.setResourcesNames(resStorage.getResourcesNames());
        appPackage = resStorage.getAppPackage();
    }

    public void initAppResClass() {
        ClassNode resCls;
        if (appPackage == null) {
            appResClass = makeClass("R");
            return;
        }
        String fullName = appPackage + ".R";
        resCls = searchClassByName(fullName);
        if (resCls != null) {
            appResClass = resCls;
        } else {
            appResClass = makeClass(fullName);
        }
    }

    private ClassNode makeClass(String clsName) {
        DexNode firstDex = dexNodes.get(0);
        ClassInfo r = ClassInfo.fromName(firstDex, clsName);
        return new ClassNode(firstDex, r);
    }

    public void initClassPath() throws DecodeException {
        try {
            if (this.clsp == null) {
                ClspGraph clsp = new ClspGraph();
                clsp.load();

                List<ClassNode> classes = new ArrayList<ClassNode>();
                for (DexNode dexNode : dexNodes) {
                    classes.addAll(dexNode.getClasses());
                }
                clsp.addApp(classes);

                this.clsp = clsp;
            }
        } catch (IOException e) {
            throw new DecodeException("Error loading classpath", e);
        }
    }

    private void initInnerClasses() {
        for (DexNode dexNode : dexNodes) {
            dexNode.initInnerClasses();
        }
    }

    public List<ClassNode> getClasses(boolean includeInner) {
        List<ClassNode> classes = new ArrayList<ClassNode>();
        for (DexNode dex : dexNodes) {
            if (includeInner) {
                classes.addAll(dex.getClasses());
            } else {
                for (ClassNode cls : dex.getClasses()) {
                    if (!cls.getClassInfo().isInner()) {
                        classes.add(cls);
                    }
                }
            }
        }
        return classes;
    }

    public ClassNode searchClassByName(String fullName) {
        for (DexNode dexNode : dexNodes) {
            ClassInfo clsInfo = ClassInfo.fromName(dexNode, fullName);
            ClassNode cls = dexNode.resolveClass(clsInfo);
            if (cls != null) {
                return cls;
            }
        }
        return null;
    }

    public List<DexNode> getDexNodes() {
        return dexNodes;
    }

    public ClspGraph getClsp() {
        return clsp;
    }

    public ErrorsCounter getErrorsCounter() {
        return errorsCounter;
    }

    @Nullable
    public String getAppPackage() {
        return appPackage;
    }

    public ClassNode getAppResClass() {
        return appResClass;
    }

    public IJadxArgs getArgs() {
        return args;
    }

    public StringUtils getStringUtils() {
        return stringUtils;
    }

    public ConstStorage getConstValues() {
        return constValues;
    }
}