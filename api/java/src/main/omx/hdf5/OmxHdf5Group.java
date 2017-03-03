package omx.hdf5;

import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.hdf5lib.exceptions.HDF5LibraryException;

import java.util.*;

/**
 * The {@code OmxGroup} ...
 *
 * @author crf
 *         Started 8/18/13 7:57 PM
 */
public class OmxHdf5Group extends AbstractOmxGroup {
    private final String name;
    private final Map<String,OmxDataset> datasets = new HashMap<>();
    private final Map<String,OmxGroup> groups = new HashMap<>();
    private final List<String> namedDatatypes = new LinkedList<>();
    private final List<String> ntypes = new LinkedList<>();
    private final List<String> unknownTypes = new LinkedList<>();
    private final Map<String,Object> attributes = new HashMap<>();
    private final OmxMutableGroup mutableGroup;

    protected OmxHdf5Group(int fileId, String groupName, String parentName, boolean loadInfo) {
        name = parentName + (parentName.endsWith("/") ? "" : "/") + groupName;
        if (loadInfo)
            loadGroupInformation(fileId);
        mutableGroup = new OmxModifiableGroup(this);
    }

    public void loadGroupInformation(int fileId) {
        int groupId = -1;
        //clear all existing junk
        datasets.clear();
        groups.clear();
        namedDatatypes.clear();
        ntypes.clear();
        unknownTypes.clear();
        attributes.clear();
        try {
            String groupName = getName();
            int count = H5.H5Gn_members(fileId,groupName);
            if (count > 0) {
                String[] oname = new String[count];
                int[] otype = new int[count];
                int[] ltype = new int[count];
                long[] orefs = new long[count];
                H5.H5Gget_obj_info_all(fileId,groupName,oname,otype,ltype,orefs,HDF5Constants.H5_INDEX_NAME);
                for (int i = 0; i < otype.length; i++) {
                    String s = oname[i];
                    switch (Hdf5Util.H5O_type.get(otype[i])) {
                        case H5O_TYPE_UNKNOWN : break;
                        case H5O_TYPE_GROUP : groups.put(s,new OmxHdf5Group(fileId,s,groupName,true)); break;
                        case H5O_TYPE_DATASET : datasets.put(s,new OmxHdf5Dataset(fileId,s,groupName)); break;
                        case H5O_TYPE_NAMED_DATATYPE : namedDatatypes.add(s); break;
                        case H5O_TYPE_NTYPES : ntypes.add(s); break;
                    }
                }
            }

            groupId = H5.H5Gopen(fileId,groupName,HDF5Constants.H5P_DEFAULT);

            //get attributes
            attributes.putAll(Hdf5Util.getAttributes(groupId));

        } catch (HDF5LibraryException e) {
            throw new RuntimeException(e);
        } finally {
            if (groupId > -1) {
                try {
                    H5.H5Gclose(groupId);
                } catch (HDF5LibraryException e) {
                    throw new RuntimeException(e);
                }
            }
        }
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
    public List<String> getNamedDatatypes() {
        return Collections.unmodifiableList(namedDatatypes);
    }

    @Override
    public List<String> getNtypes() {
        return Collections.unmodifiableList(ntypes);
    }

    @Override
    public List<String> getUnknownTypes() {
        return unknownTypes;
    }

    @Override
    public OmxMutableGroup getMutableGroup() {
        return mutableGroup;
    }

    @Override
    public Map<String,Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
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
