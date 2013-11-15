package omx.hdf5;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The {@code OmxGroup} ...
 *
 * @author crf
 *         Started 9/20/13 11:24 AM
 */
public interface OmxGroup {
    String getName();
    Collection<OmxDataset> getDatasets();
    Collection<OmxGroup> getGroups();
    Collection<String> getDatasetNames();
    Collection<String> getGroupNames();
    OmxDataset getDataset(String name);
    OmxGroup getGroup(String name);
    Map<String,Object> getAttributes();
    List<String> getNamedDatatypes();
    List<String> getNtypes();
    List<String> getUnknownTypes();
    OmxMutableGroup getMutableGroup();
    boolean hasGroup(String name);
    boolean hasDataset(String name);
}
