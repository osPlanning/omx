package omx.hdf5;

import java.util.Map;

/**
 * The {@code AbstractOmxGroup} ...
 *
 * @author crf
 *         Started 9/20/13 12:24 PM
 */
public abstract class AbstractOmxGroup implements OmxGroup {

    protected OmxDataset getDataset(String name, Map<String,OmxDataset> datasets) {
        if (name.indexOf("/") == 0)
            name = name.substring(1);
        String[] groupsPlusName = name.split("/");
        if ((groupsPlusName.length == 1) && (datasets.containsKey(groupsPlusName[0]))) {
            return datasets.get(groupsPlusName[0]);
        } else if (groupsPlusName.length > 1) {
            return getGroup(groupsPlusName[0]).getDataset(name.replaceFirst(groupsPlusName[0],"").replace("//","/"));
        }
        throw new IllegalArgumentException("Dataset not found: " + name);
    }

    protected OmxGroup getGroup(String name, Map<String,OmxGroup> groups) {
        String[] groupsPlusName = name.split("/");
        if (groupsPlusName[0].length() == 0)
            return this;
        if (!groups.containsKey(groupsPlusName[0]))
            throw new IllegalArgumentException("group not found: " + groupsPlusName[0] + " (in " + getName() + ")");
        return groups.get(groupsPlusName[0]).getGroup(name.replaceFirst(groupsPlusName[0],"").replace("//","/"));
    }

    public boolean hasGroup(String name, Map<String,OmxGroup> groups) {
        if (name.contains(getName()))
            return groups.containsKey(name.replaceFirst(getName(),"").replace("/",""));
        return false;
    }

    public boolean hasDataset(String name, Map<String,OmxDataset> datasets) {
        if (name.contains(getName()))
            return datasets.containsKey(name.replaceFirst(getName(),"").replace("/",""));
        return false;
    }
}
