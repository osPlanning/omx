package omx.hdf5;

import java.util.Collection;
import java.util.Map;

/**
 * The {@code OmxMutableGroup} ...
 *
 * @author crf
 *         Started 9/20/13 12:02 PM
 */
public interface OmxMutableGroup extends OmxGroup {
    void setName(String name);
    void setDatasets(Collection<OmxDataset> datasets);
    void setGroups(Collection<OmxGroup> groups);
    void setDataset(OmxDataset dataset);
    void deleteDataset(String name);
    void setGroup(OmxGroup group);
    void deleteGroup(String name);
    void setAttributes(Map<String,Object> attributes);
    void setAttribute(String key, Object attribute);
    void deleteAttribute(String key);
    boolean isMutated();
    boolean areAttributesMutated();
}
