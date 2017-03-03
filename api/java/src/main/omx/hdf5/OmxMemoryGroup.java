package omx.hdf5;

import java.util.*;

/**
 * The {@code OmxMemoryGroup} ...
 *
 * @author crf
 *         Started 9/20/13 12:10 PM
 */
public class OmxMemoryGroup extends AbstractOmxGroup {
    private final String name;
    private final Map<String,OmxDataset> datasets;
    private final Map<String,OmxGroup> groups;
    private final List<String> namedDatatypes = new LinkedList<>();
    private final List<String> ntypes = new LinkedList<>();
    private final List<String> unknownTypes = new LinkedList<>();
    private final Map<String,Object> attributes;
    private final OmxMutableGroup mutableGroup;

    public OmxMemoryGroup(String name, Map<String,OmxDataset> datasets, Map<String,OmxGroup> groups, Map<String,Object> attributes) {
        this.name = name;
        this.datasets = datasets;
        this.groups = groups;
        this.attributes = attributes;
        mutableGroup = new OmxModifiableGroup(this);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Collection<OmxDataset> getDatasets() {
        return Collections.unmodifiableCollection(datasets.values());
    }

    @Override
    public Collection<OmxGroup> getGroups() {
        return Collections.unmodifiableCollection(groups.values());
    }

    @Override
    public Collection<String> getDatasetNames() {
        return Collections.unmodifiableCollection(datasets.keySet());
    }

    @Override
    public Collection<String> getGroupNames() {
        return Collections.unmodifiableCollection(groups.keySet());
    }

    @Override
    public OmxDataset getDataset(String name) {
        return getDataset(name,datasets);
    }

    @Override
    public OmxGroup getGroup(String name) {
        return getGroup(name,groups);
    }

    @Override
    public Map<String,Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    @Override
    public List<String> getNamedDatatypes() {
        return Collections.unmodifiableList(namedDatatypes);
    }

    @Override
    public List<String> getNtypes() {
        return Collections.unmodifiableList(ntypes);
    }

    @Override
    public List<String> getUnknownTypes() {
        return Collections.unmodifiableList(unknownTypes);
    }

    @Override
    public OmxMutableGroup getMutableGroup() {
        return mutableGroup;
    }

    @Override
    public boolean hasGroup(String name) {
        return hasGroup(name,groups);
    }

    @Override
    public boolean hasDataset(String name) {
        return hasDataset(name,datasets);
    }
}
