package omx.hdf5;

import java.util.HashMap;
import java.util.Map;

/**
 * The {@code OmxConstants} ...
 *
 * @author crf
 *         Started 9/18/13 2:37 PM
 */
public class OmxConstants {
    public static enum OmxNames {
        OMX_VERSION_KEY("OMX_VERSION"),
        OMX_SHAPE_KEY("SHAPE"),
        OMX_DATASET_DIM_KEY("dims"),
        OMX_DATASET_TITLE_KEY("title"),
        OMX_DATASET_NA_KEY("NA"),
        OMX_DATA_GROUP("data"),
        OMX_LOOKUP_GROUP("lookup");

        private final String key;

        private OmxNames(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    public static enum OmxVersion {
        VERSION_02("0.2");

        private final String versionString;

        private OmxVersion(String versionString) {
            this.versionString = versionString;
        }

        public String getVersionString() {
            return versionString;
        }

        private static final Map<String,OmxVersion> versionMap;

        static {
            versionMap = new HashMap<>();
            for (OmxVersion version : values())
                versionMap.put(version.getVersionString(),version);
        }

        public static OmxVersion getVersion(String versionString) {
            versionString = versionString.trim(); //remove whatever whitespace exists
            if (versionMap.containsKey(versionString))
                return versionMap.get(versionString);
            throw new IllegalArgumentException("Unknown/unsupported OMX version (valid set = " + versionMap.keySet() + "): '" + versionString + "'");
        }
    }
}
