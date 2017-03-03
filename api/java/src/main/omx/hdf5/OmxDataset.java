package omx.hdf5;

import java.util.Map;

/**
 * The {@code OmxDataset} ...
 *
 * @author crf
 *         Started 9/18/13 3:47 PM
 */
public interface OmxDataset {
    String getName();
    int[] getShape();
    OmxHdf5Datatype getDatatype();
    Map<String,Object> getAttributes();
    Object getData();
    OmxMutableDataset getMutableDataset();
}
