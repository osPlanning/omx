package omx.hdf5;

import java.util.*;

/**
 * The {@code OmxModifiableDataset} ...
 *
 * @author crf
 *         Started 9/18/13 4:32 PM
 */
public class OmxModifiableDataset implements OmxMutableDataset {
    private String name = null;
    private int[] shape = null;
    private OmxHdf5Datatype datatype = null;
    private Map<String,Object> attributes = null;
    private Object data = null;
    private boolean modified = false;
    private boolean dataModified = false;
    private boolean attributesModified = false;
    private final OmxDataset parentDataset;

    public OmxModifiableDataset(OmxDataset parentDataset) {
        this.parentDataset = parentDataset;
    }

    @Override
    public void setName(String name) {
        this.name = name;
        modified = true;
    }

    @Override
    public void setShape(int[] shape) {
        this.shape = Arrays.copyOf(shape,shape.length);
        modified = true;
    }

    @Override
    public void setDatatype(OmxHdf5Datatype datatype) {
        this.datatype = datatype;
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
            setAttributes(parentDataset.getAttributes());
        attributes.put(key,attribute);
        modified = true;
        attributesModified = true;
    }

    @Override
    public void deleteAttribute(String key) {
        if (attributes == null)
            setAttributes(parentDataset.getAttributes());
        if (!attributes.containsKey(key))
            throw new IllegalArgumentException("Attribute key not found: " + key);
        attributes.remove(key);
        modified = true;
        attributesModified = true;
    }

    @Override
    public void setData(Object data) {
        this.data = data;
        modified = true;
        dataModified = true;
    }

    @Override
    public boolean isMutated() {
        return modified;
    }

    @Override
    public boolean isDataMutated() {
        return dataModified;
    }

    @Override
    public boolean areAttributesMutated() {
        return attributesModified;
    }

    @Override
    public String getName() {
        return name == null ? parentDataset.getName() : name;
    }

    @Override
    public int[] getShape() {
        return shape == null ? parentDataset.getShape() : Arrays.copyOf(shape,shape.length);
    }

    @Override
    public OmxHdf5Datatype getDatatype() {
        return datatype == null ? parentDataset.getDatatype() : datatype;
    }

    @Override
    public Map<String,Object> getAttributes() {
        return attributes == null ? parentDataset.getAttributes() : Collections.unmodifiableMap(attributes);
    }

    @Override
    public Object getData() {
        return data == null ? parentDataset.getData() : data;
    }

    @Override
    public OmxMutableDataset getMutableDataset() {
        return this;
    }
}
