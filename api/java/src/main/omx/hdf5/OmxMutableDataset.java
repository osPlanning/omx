package omx.hdf5;

import java.util.Map;

/**
 * The {@code OmxMutableDataset} ...
 *
 * @author crf
 *         Started 9/18/13 4:24 PM
 */
public interface OmxMutableDataset extends OmxDataset {
    void setName(String name);
    void setShape(int[] shape);
    void setDatatype(OmxHdf5Datatype datatype);
    void setAttributes(Map<String,Object> attributes);
    void setAttribute(String key, Object attribute);
    void deleteAttribute(String key);
    void setData(Object data);
    boolean isMutated();
    boolean isDataMutated();
    boolean areAttributesMutated();
}
