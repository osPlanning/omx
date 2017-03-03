package omx.hdf5;

import java.util.*;

/**
 * The {@code OmxModifiableGroup} ...
 *
 * @author crf
 *         Started 9/20/13 12:08 PM
 */
public class OmxModifiableGroup extends AbstractOmxGroup implements OmxMutableGroup {
    private String name = null;
    private Map<String,OmxDataset> datasets = null;
    private Map<String,OmxGroup> groups = null;
    private Map<String,Object> attributes = null;
    private boolean modified = false;
    private boolean attributesModified = false;
    private final OmxGroup parentGroup;

    public OmxModifiableGroup(OmxGroup parentGroup) {
        this.parentGroup = parentGroup;
    }

    @Override
    public void setName(String name) {
        this.name = name;
        modified = true;
    }

    @Override
    public void setDatasets(Collection<OmxDataset> datasets) {
        this.datasets = new HashMap<>();
        for (OmxDataset dataset : datasets)
            setDataset(dataset);
        modified = true;
    }

    @Override
    public void setGroups(Collection<OmxGroup> groups) {
        this.groups = new HashMap<>();
        for (OmxGroup group : groups)
            setGroup(group);
        modified = true;
    }

    @Override
    public void setDataset(OmxDataset dataset) {
        if (datasets == null)
            setDatasets(parentGroup.getDatasets());
        OmxMutableDataset mutableDataset = new OmxModifiableDataset(dataset);
        String groupName = mutableDataset.getName();
        String baseName = groupName.substring(groupName.lastIndexOf("/") + 1);
        mutableDataset.setName(getName() + "/" + baseName);
        datasets.put(baseName,dataset);
        modified = true;
    }

    @Override
    public void deleteDataset(String name) {
        if (datasets == null)
            setDatasets(parentGroup.getDatasets());
        if (!datasets.containsKey(name))
            throw new IllegalArgumentException("Dataset name not found: " + name);
        datasets.remove(name);
        modified = true;
    }

    @Override
    public void setGroup(OmxGroup group) {
        if (groups == null)
            setGroups(parentGroup.getGroups());
        OmxMutableGroup mutableGroup = new OmxModifiableGroup(group);
        String groupName = mutableGroup.getName();
        String baseName = groupName.substring(groupName.lastIndexOf("/") + 1);
        mutableGroup.setName(getName() + "/" + baseName);
        groups.put(baseName,group);
        modified = true;
    }

    @Override
    public void deleteGroup(String name) {
        if (groups == null)
            setGroups(parentGroup.getGroups());
        if (!groups.containsKey(name))
            throw new IllegalArgumentException("Group name not found: " + name);
        groups.remove(name);
        modified = true;
    }

    @Override
    public void setAttributes(Map<String,Object> attributes) {
        this.attributes = new HashMap<>(attributes);
        modified = true;
        attributesModified = true;
    }

    @Override
    public void setAttribute(String key, Object attribute) {
        if (attributes == null)
            setAttributes(parentGroup.getAttributes());
        attributes.put(key,attribute);
        modified = true;
        attributesModified = true;
    }

    @Override
    public void deleteAttribute(String key) {
        if (attributes == null)
            setAttributes(parentGroup.getAttributes());
        if (!attributes.containsKey(key))
            throw new IllegalArgumentException("Attribute key not found: " + key);
        attributes.remove(key);
        modified = true;
        attributesModified = true;
    }

    @Override
    public boolean isMutated() {
        return modified;
    }

    @Override
    public boolean areAttributesMutated() {
        return attributesModified;
    }

    @Override
    public String getName() {
        return name == null ? parentGroup.getName() : name;
    }

    @Override
    public Collection<OmxDataset> getDatasets() {
        return datasets == null ? parentGroup.getDatasets() : Collections.unmodifiableCollection(datasets.values());
    }

    @Override
    public Collection<OmxGroup> getGroups() {
        return groups == null ? parentGroup.getGroups() : Collections.unmodifiableCollection(groups.values());
    }

    @Override
    public Collection<String> getDatasetNames() {
        return datasets == null ? parentGroup.getDatasetNames() : Collections.unmodifiableCollection(datasets.keySet());
    }

    @Override
    public Collection<String> getGroupNames() {
        return groups == null ? parentGroup.getGroupNames() : Collections.unmodifiableCollection(groups.keySet());
    }

    @Override
    public OmxDataset getDataset(String name) {
        return datasets == null ? parentGroup.getDataset(name) : getDataset(name,datasets);
    }

    @Override
    public OmxGroup getGroup(String name) {
        return groups == null ? parentGroup.getGroup(name) : getGroup(name,groups);
    }

    @Override
    public Map<String,Object> getAttributes() {
        return attributes == null ? parentGroup.getAttributes() : Collections.unmodifiableMap(attributes);
    }

    @Override
    public List<String> getNamedDatatypes() {
        return parentGroup.getNamedDatatypes();
    }

    @Override
    public List<String> getNtypes() {
        return parentGroup.getNtypes();
    }

    @Override
    public List<String> getUnknownTypes() {
        return parentGroup.getUnknownTypes();
    }

    @Override
    public OmxMutableGroup getMutableGroup() {
        return this;
    }

    @Override
    public boolean hasGroup(String name) {
        if (groups != null)
            return hasGroup(name,groups);
        return parentGroup.hasGroup(name);
    }

    @Override
    public boolean hasDataset(String name) {
        if (datasets != null)
            return hasDataset(name,datasets);
        return parentGroup.hasDataset(name);
    }
}
