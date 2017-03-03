package omx;

import omx.hdf5.OmxMutableDataset;
import omx.hdf5.OmxMutableGroup;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The {@code AttributedElement} is an Open MatriX element which contains attributes. Attributes are essentially metadata
 * which are organized as (String) key - (Object) value pairs. Note that even though there are not technical restrictions
 * on the types of Objects the values may be, straying from basic types (numbers, Strings, lists) may cause errors when
 * saving to OMX files.
 *
 * @author crf
 *         Started 9/21/13 1:18 PM
 */
public class AttributedElement {
    private final Map<String,Object> attributes;

    /**
     * Constructor.
     */
    public AttributedElement() {
        attributes = new HashMap<>();
    }

    /**
     * Get the attribute value corresponding to a specific key.
     *
     * @param key
     *        The attribute key.
     *
     * @return the value corresponding to {@code key}.
     *
     * @throws IllegalArgumentException if this element does not have an attribute corresponding to {@code key}.
     */
    public Object getAttribute(String key) {
        if (!attributes.containsKey(key))
            throw new IllegalArgumentException("Attribute key not found: " + key);
        return attributes.get(key);
    }

    /**
     * Get the set of keys for this element's attributes.
     *
     * @return this element's attribut keys.
     */
    public Set<String> getAttributeKeys() {
        return attributes.keySet();
    }

    /**
     * Delete the attribute corresponding to a specific key.
     *
     * @param key
     *        The attribute key.
     *
     * @throws IllegalArgumentException if this element does not have an attribute corresponding to {@code key}.
     */
    public void deleteAttribute(String key) {
        if (!attributes.containsKey(key))
            throw new IllegalArgumentException("Attribute key not found: " + key);
        attributes.remove(key);
    }

    /**
     * Set an attribute value. If this element already has an attribute corresponding to the specific key, the current
     * attribute value will be overwritten with the new one specified in this method call.
     *
     * @param key
     *        The attribute key.
     *
     * @param value
     *        The attribute value.
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key,value);
    }

    /**
     * Get a mapping of all attribute key-value pairs. The returned map is unmodifiable and intended to be read-only.
     *
     * @return all attributes as a key-value map.
     */
    public Map<String,Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    /**
     * Determine if this element has an attribute corresponding to a specified key.
     *
     * @param key
     *        The attribute key.
     *
     * @return {@code true} if this element has an attribute corresponding to {@code key}, {@code false} otherwise.
     */
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    /**
     * Replace all of the attributes in an {@code OmxMutableDataset} with those in this element. Note that this means the
     * following:
     * <ol>
     *     <li>All attribute keys in this element but not in the dataset will be added to the dataset.</li>
     *     <li>All attribute keys in the dataset but not in this element will be deleted from the dataset.</li>
     *     <li>All attribute keys in both this element and the dataset will be retained in the dataset, with the attribute
     *         value in the dataset being set to that in this element.</li>
     * </ol>
     * <p>
     * Note that this method is intended to be used by implementations which need direct access to OMX HDF5 objects (most
     * likely to read/write HDF5 files), and should not be needed/used outside of this context.
     *
     * @param dataset
     *        The dataset to transfer this element's attributes to.
     */
    protected void transferAttributes(OmxMutableDataset dataset) {
        Set<String> attributes = getAttributeKeys();
        Map<String,Object> datasetAttributes = dataset.getAttributes();
        for (String key : datasetAttributes.keySet()) {
            if (attributes.contains(key)) {
                if (!datasetAttributes.get(key).equals(getAttribute(key)))
                    dataset.setAttribute(key,getAttribute(key));
            } else {
                dataset.deleteAttribute(key);
            }
        }
        for (String key : attributes) {
            if (!datasetAttributes.containsKey(key))
                dataset.setAttribute(key,getAttribute(key));
        }
    }


    /**
     * Replace all of the attributes in an {@code OmxMutableGroup} with those in this element. Note that this means the
     * following:
     * <ol>
     *     <li>All attribute keys in this element but not in the group will be added to the group.</li>
     *     <li>All attribute keys in the group but not in this element will be deleted from the group.</li>
     *     <li>All attribute keys in both this element and the group will be retained in the group, with the attribute
     *         value in the group being set to that in this element.</li>
     * </ol>
     * <p>
     * Note that this method is intended to be used by implementations which need direct access to OMX HDF5 objects (most
     * likely to read/write HDF5 files), and should not be needed/used outside of this context.
     *
     * @param group
     *        The group to transfer this element's attributes to.
     */
    protected void transferAttributes(OmxMutableGroup group) {
        Set<String> attributes = getAttributeKeys();
        Map<String,Object> groupAttributes = group.getAttributes();
        for (String key : groupAttributes.keySet()) {
            if (attributes.contains(key)) {
                if (groupAttributes.get(key).equals(getAttribute(key)))
                    group.setAttribute(key,getAttribute(key));
            } else {
                group.deleteAttribute(key);
            }
        }
        for (String key : attributes) {
            if (!groupAttributes.containsKey(key))
                group.setAttribute(key,getAttribute(key));
        }
    }
}
