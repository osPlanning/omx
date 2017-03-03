package omx.hdf5;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

/**
 * The {@code OmxMemoryDataset} ...
 *
 * @author crf
 *         Started 9/18/13 3:39 PM
 */
public class OmxMemoryDataset implements OmxDataset {
    private final String name;
    private final int[] shape;
    private final OmxHdf5Datatype datatype;
    private final Map<String,Object> attributes;
    private final Object data;
    private final OmxMutableDataset mutableDataset;

    public OmxMemoryDataset(String name, int[] shape, OmxHdf5Datatype datatype, Map<String,Object> attributes, Object data) {
        this.name = name;
        this.shape = shape;
        this.datatype = datatype;
        this.attributes = attributes;
        this.data = data;
        mutableDataset = new OmxModifiableDataset(this);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int[] getShape() {
        return Arrays.copyOf(shape,shape.length);
    }

    @Override
    public OmxHdf5Datatype getDatatype() {
        return datatype;
    }

    @Override
    public Map<String,Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    @Override
    public Object getData() {
        return data;
    }

    @Override
    public OmxMutableDataset getMutableDataset() {
        return mutableDataset;
    }
}
