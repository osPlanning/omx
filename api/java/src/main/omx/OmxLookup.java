package omx;

import omx.hdf5.*;

import java.lang.reflect.Array;
import java.util.*;

/**
 * The {@code OmxLookup} class provides access to OMX lookup data. Lookups are lists of basic data types (numbers or text)
 * which allow a matrix (or other fixed-width data structure) to be re-indexed. A given lookup matches to a specific dimension,
 * and has an element for each position (index) in that dimension. If the element is missing, then that index is not included
 * in the lookup, otherwise the element provides the index value to use when accessing the data through the lookup.
 * <p>
 * For example, if a 3x4 matrix has the following row and column lookups with missing values specified as -1:
 * <p>
 * <ul>
 *     <li><bold>row:</bold> [1,0,2]</li>
 *     <li><bold>column:</bold> [0,1,2,-1]</li>
 * </ul>
 * <p>
 * Then a matrix using this lookup would be 3x3, and have (compared to the source matrix) the final column removed and the
 * first and second rows swapped. Note that this is using the lookup index values to order the subsequent matrix, but this
 * is not required (and may not make sense, <i>e.g.</i> with text lookups).
 * <p>
 * This class is parameterized by the type of its lookup data container ({@code <D>}) and the type of its lookup values's
 * (Java) equivalent  object ({@code <C>}). Only a limited set of {@code <D,C>} pairs are allowed, and are as follows:
 * <p>
 * <table border="1" cellpadding="3" style="border-collapse: collapse; width: 0">
 *     <tr>
 *         <th>Lookup data container (<code>D</code>)</th>
 *         <th>Lookup object equivalent (<code>C</code>)</th>
 *     </tr>
 *     <tr>
 *         <td><code>byte[]</code></td>
 *         <td><code>Byte</code></td>
 *     </tr>
 *     <tr>
 *         <td><code>short[]</code></td>
 *         <td><code>Short</code></td>
 *     </tr>
 *     <tr>
 *         <td><code>int[]</code></td>
 *         <td><code>Integer</code></td>
 *     </tr>
 *     <tr>
 *         <td><code>float[]</code></td>
 *         <td><code>Float</code></td>
 *     </tr>
 *     <tr>
 *         <td><code>double[]</code></td>
 *         <td><code>Double</code></td>
 *     </tr>
 *     <tr>
 *         <td><code>String[]</code></td>
 *         <td><code>String</code></td>
 *     </tr>
 * </table>
 * </p>
 * <p>
 * Because of this restriction, instances of this classes will generally not be (directly) needed. Instead, one of the
 * subclasses created from the static {@code Omx[DATATYPE]Lookup} classes provided in this class should be used.
 * <p>
 * Note that instances of this class intentionally do not provide full immutability guarantees. First, the missing value
 * identifier (which indicates the lookup value that should represent a "missing" (or ignored) value) may be changed/set
 * after construction.  Additionally, while the data container may not be changed, its values are allowed to be modified;
 * the OmxLookup instance will detect any changes and write them to the OMX file if a save is requested. This avoids the
 * need to copy (internal) data arrays when modifying data in-place.
 *
 * @param <D>
 *        The type of the data container the lookup uses. This will always be a one-dimensional array of some sort.
 *
 * @param <C>
 *        The object-equivalent type of the component (base) type of {@code <D>}.
 *
 * @author crf
 *         Started 9/20/13 7:58 PM
 */
public class OmxLookup<D,C> extends AttributedElement {
    private final String name;
    private final D lookup;
    private final int length;
    private final Class<?> componentClass;
    private final Class<C> objectEquivalentClass;
    private volatile C missingLookupValue;
    private final int initialHashcode;

    /**
     * Constructor specifying the name, data, and missing value for the lookup.
     *
     * @param name
     *        The lookup name.
     *
     * @param lookup
     *        The lookup data.
     *
     * @param missingLookupValue
     *        The value to use for missing values in the lookup.
     *
     * @throws IllegalArgumentException if {@code lookup} is not a one-dimensional array or is empty; or if the component class
     *                                  of {@code lookup} is not one of the allowed types or does not match the object-equivalent
     *                                  class of {@code missingLookupValue}.
     */
    public OmxLookup(String name, D lookup, C missingLookupValue) {
        this.name = name;
        if (!lookup.getClass().isArray())
            throw new IllegalArgumentException("Lookup must be a one dimensional array");
        length = Array.getLength(lookup);
        if (length == 0)
            throw new IllegalArgumentException("Lookup array cannot be empty");
        componentClass = lookup.getClass().getComponentType();
        if (!ALLOWED_CLASS_TO_OBJECT_MAP.containsKey(componentClass))
            throw new IllegalArgumentException("Component class invalid: " + componentClass);
        @SuppressWarnings("unchecked") //correct, if rules are followed
        Class<C> objectEquivalentClass = (Class<C>) ALLOWED_CLASS_TO_OBJECT_MAP.get(componentClass);
        this.objectEquivalentClass = objectEquivalentClass;
        this.missingLookupValue = missingLookupValue;
        if ((missingLookupValue != null) && (missingLookupValue.getClass() != objectEquivalentClass))
            throw new IllegalStateException("Object equivalent component class (" + objectEquivalentClass + ") does not match missing lookup value class: " + missingLookupValue.getClass());
        if ((missingLookupValue != null) && (!getAttributeKeys().contains(OmxConstants.OmxNames.OMX_DATASET_NA_KEY.getKey())))
            setAttribute(OmxConstants.OmxNames.OMX_DATASET_NA_KEY.getKey(),missingLookupValue);
        checkLookupValidity(lookup,missingLookupValue);
        this.lookup = lookup;
        initialHashcode = lookupHashcode();
    }

    private void checkLookupValidity(D lookup, C missingLookupValue) {
        boolean nullMissing = missingLookupValue == null;
        Set<Object> lookups = new HashSet<>();
        for (int i = 0; i < getLength(); i++) {
            Object value = Array.get(lookup,i);
            if (nullMissing || !missingLookupValue.equals(value))
                if (!lookups.add(value))
                    throw new IllegalArgumentException("Lookup is invalid because it has a repeated value: " + value);
        }
    }

    private int lookupHashcode() {
        if (componentClass == byte.class)
            return Arrays.hashCode((byte[]) lookup);
        else if (componentClass == short.class)
            return Arrays.hashCode((short[]) lookup);
        else if (componentClass == int.class)
            return Arrays.hashCode((int[]) lookup);
        else if (componentClass == float.class)
            return Arrays.hashCode((float[]) lookup);
        else if (componentClass == double.class)
            return Arrays.hashCode((double[]) lookup);
        else if (componentClass == String .class)
            return Arrays.hashCode((String[]) lookup);
        throw new IllegalStateException("Should not be here...");
    }

    /**
     * Determine if the data held by this matrix has been modified since its initial construction.
     *
     * @return {@code true} if the data has been changed, {@code false} otherwise.
     */
    public boolean isDataModified() {
        return initialHashcode != lookupHashcode();
    }

    /**
     * Get this matrix's name.
     *
     * @return the name of this matrix.
     */
    public String getName() {
        return name;
    }

    /**
     * Get this lookup's data.
     *
     * @return the index data held by this lookuyp.
     */
    public D getLookup() {
        return lookup;
    }

    /**
     * Get this lookup's length. Note that this is the same regardless of how many "missing" values are in the actual lookup.
     * That is, this is the length of the <i>source</i> dimension this lookup applies to, not the resultant dimension size.
     *
     * @return the the length of this lookup.
     */
    public int getLength() {
        return length;
    }

    /**
     * Get the value used by this lookup to indicate missing values. Note that this class only maintains this value; it is
     * up to users of this class to combine it with the actual lookup to create  missing value functionality.
     *
     * @return this lookup's missing value value.
     */
    public C getMissingValue() {
        return missingLookupValue;
    }

    /**
     * Set the value used by this lookup to indicate missing values.
     *
     * @param missingLookupValue
     *        The value to use for missing values.
     */
    public void setMissingValue(C missingLookupValue) {
        this.missingLookupValue = missingLookupValue;
        setAttribute(OmxConstants.OmxNames.OMX_DATASET_NA_KEY.getKey(),missingLookupValue);
    }

    /**
     * Get the type of data this matrix holds. In general, this will be a primitive class (<i>e.g.</i> <code>int.class</code>),
     * though in some cases (such as <code>String.class</code>) with no primitive equivalent, the standard Java object
     * class will be returned.
     *
     * @return this matrix's data type class.
     */
    public Class<?> getComponentClass() {
        return componentClass;
    }

    /**
     * Get the OMX Java type that corresponds to this lookup instance.
     *
     * @return this lookup's equivalent OMX Java type.
     */
    public OmxHdf5Datatype.OmxJavaType getOmxJavaType() {
        if (componentClass == byte.class)
            return OmxHdf5Datatype.OmxJavaType.BYTE;
        else if (componentClass == short.class)
            return OmxHdf5Datatype.OmxJavaType.SHORT;
        else if (componentClass == int.class)
            return OmxHdf5Datatype.OmxJavaType.INT;
        else if (componentClass == float.class)
            return OmxHdf5Datatype.OmxJavaType.FLOAT;
        else if (componentClass == double.class)
            return OmxHdf5Datatype.OmxJavaType.DOUBLE;
        else if (componentClass == String.class)
            return OmxHdf5Datatype.OmxJavaType.STRING;
        throw new IllegalStateException("Should not be here!");
    }

    /**
     * Transfer the data held in this lookup to an OMX dataset. This functionality is mainly intended for users interacting
     * with the HDF5 OMX data structures directly, and should not be needed for general use. Note that this copies not only
     * the data, but also metadata (attributes and the value used for missing values).
     *
     * @param dataset
     *        The dataset to transfer the data to.
     */
    public void transfer(OmxMutableDataset dataset) {
        transferAttributes(dataset);
        transferData(dataset);
    }

    private void transferData(OmxMutableDataset dataset) {
        if ((dataset instanceof OmxHdf5Dataset) && !isDataModified()) //if not from hdf5, then even if data isn't modified, it is new
            return;
        dataset.setData(getLookup());
    }

    private static final Map<Class<?>,Class<?>> ALLOWED_CLASS_TO_OBJECT_MAP = new HashMap<>();
    static {
        ALLOWED_CLASS_TO_OBJECT_MAP.put(byte.class,Byte.class);
        ALLOWED_CLASS_TO_OBJECT_MAP.put(short.class,Short.class);
        ALLOWED_CLASS_TO_OBJECT_MAP.put(int.class,Integer.class);
        ALLOWED_CLASS_TO_OBJECT_MAP.put(float.class,Float.class);
        ALLOWED_CLASS_TO_OBJECT_MAP.put(double.class,Double.class);
        ALLOWED_CLASS_TO_OBJECT_MAP.put(String.class,String.class);
    }

    /**
     * The {@code OmxByteLookup} is an {@code OmxLookup} which holds bytes.
     */
    public static class OmxByteLookup extends OmxLookup<byte[],Byte> {

        /**
         * Constructor specifying the name, data, and missing value for the lookup.
         *
         * @param name
         *        The lookup name.
         *
         * @param lookup
         *        The lookup data.
         *
         * @param missingLookupValue
         *        The value to use for missing values in the lookup.
         *
         * @throws IllegalArgumentException if {@code lookup} is not a one-dimensional array or is empty.
         */
        public OmxByteLookup(String name, byte[] lookup, Byte missingLookupValue) {
            super(name,lookup,missingLookupValue);
        }
    }

    /**
     * The {@code OmxShortLookup} is an {@code OmxLookup} which holds shorts.
     */
    public static class OmxShortLookup extends OmxLookup<short[],Short> {

        /**
         * Constructor specifying the name, data, and missing value for the lookup.
         *
         * @param name
         *        The lookup name.
         *
         * @param lookup
         *        The lookup data.
         *
         * @param missingLookupValue
         *        The value to use for missing values in the lookup.
         *
         * @throws IllegalArgumentException if {@code lookup} is not a one-dimensional array or is empty.
         */
        public OmxShortLookup(String name, short[] lookup, Short missingLookupValue) {
            super(name,lookup,missingLookupValue);
        }
    }

    /**
     * The {@code OmxIntLookup} is an {@code OmxLookup} which holds ints.
     */
    public static class OmxIntLookup extends OmxLookup<int[],Integer> {

        /**
         * Constructor specifying the name, data, and missing value for the lookup.
         *
         * @param name
         *        The lookup name.
         *
         * @param lookup
         *        The lookup data.
         *
         * @param missingLookupValue
         *        The value to use for missing values in the lookup.
         *
         * @throws IllegalArgumentException if {@code lookup} is not a one-dimensional array or is empty.
         */
        public OmxIntLookup(String name, int[] lookup, Integer missingLookupValue) {
            super(name,lookup,missingLookupValue);
        }
    }

    /**
     * The {@code OmxFloatLookup} is an {@code OmxLookup} which holds floats.
     */
    public static class OmxFloatLookup extends OmxLookup<float[],Float> {

        /**
         * Constructor specifying the name, data, and missing value for the lookup.
         *
         * @param name
         *        The lookup name.
         *
         * @param lookup
         *        The lookup data.
         *
         * @param missingLookupValue
         *        The value to use for missing values in the lookup.
         *
         * @throws IllegalArgumentException if {@code lookup} is not a one-dimensional array or is empty.
         */
        public OmxFloatLookup(String name, float[] lookup, Float missingLookupValue) {
            super(name,lookup,missingLookupValue);
        }
    }

    /**
     * The {@code OmxDoubleLookup} is an {@code OmxLookup} which holds doubles.
     */
    public static class OmxDoubleLookup extends OmxLookup<double[],Double> {

        /**
         * Constructor specifying the name, data, and missing value for the lookup.
         *
         * @param name
         *        The lookup name.
         *
         * @param lookup
         *        The lookup data.
         *
         * @param missingLookupValue
         *        The value to use for missing values in the lookup.
         *
         * @throws IllegalArgumentException if {@code lookup} is not a one-dimensional array or is empty.
         */
        public OmxDoubleLookup(String name, double[] lookup, Double missingLookupValue) {
            super(name,lookup,missingLookupValue);
        }
    }

    /**
     * The {@code OmxStringLookup} is an {@code OmxLookup} which holds Strings (text).
     */
    public static class OmxStringLookup extends OmxLookup<String[],String> {
        public OmxStringLookup(String name, String[] data, String missingLookupValue) {
            super(name,data,missingLookupValue);
        }
    }

    /**
     * Get an {@code OmxLookup} corresponding to an OMX dataset. The returned lookup will have an implicit connection to
     * the source dataset, which can be useful when modifying and/or saving data. Note that this method will fail if the
     * dataset holds an unallowed type or is not one-dimensional.
     *
     * @param dataset
     *        The dataset that the lookup will be built from.
     *
     * @return a lookup representing {@code dataset}.
     */
    public static OmxLookup getLookup(OmxDataset dataset) {
        OmxHdf5Datatype datatype = dataset.getDatatype();
        Map<String,Object> attributes = dataset.getAttributes();
        Object missingValue = null;
        if (attributes.containsKey(OmxConstants.OmxNames.OMX_DATASET_NA_KEY.getKey()))
            missingValue = attributes.get(OmxConstants.OmxNames.OMX_DATASET_NA_KEY.getKey());
        String name = Hdf5Util.getBaseName(dataset.getName());
        OmxLookup lookup = null;
        switch (datatype.getOmxJavaType()) {
            case BYTE : {
                byte[] data = (byte[]) dataset.getData();
                lookup = new OmxByteLookup(name,data,(Byte) missingValue);
            } break;
            case SHORT : {
                short[] data = (short[]) dataset.getData();
                lookup = new OmxShortLookup(name,data,(Short) missingValue);
            } break;
            case INT : {
                int[] data = (int[]) dataset.getData();
                lookup = new OmxIntLookup(name,data,(Integer) missingValue);
            } break;
            case FLOAT : {
                float[] data = (float[]) dataset.getData();
                lookup = new OmxFloatLookup(name,data,(Float) missingValue);
            } break;
            case DOUBLE : {
                double[] data = (double[]) dataset.getData();
                lookup = new OmxDoubleLookup(name,data,(Double) missingValue);
            } break;
            case STRING : {
                String[] data = (String[]) dataset.getData();
                lookup = new OmxStringLookup(name,data,(String) missingValue);
            } break;
        }

        for (String key : attributes.keySet())
            lookup.setAttribute(key,attributes.get(key));
        return lookup;
    }
}
