package omx;

import omx.hdf5.*;

import java.lang.reflect.Array;
import java.util.*;

/**
 * The {@code OmxMatrix} class provides access to OMX matrix data. OMX matrix data is two-dimensional, rectangular data
 * of a basic type (numeric or text) with an optionally specified missing value identifier. In addition to the matrix data
 * itself, this container provides information on the shape of the matrix, its name, and other metadata.
 * <p>
 * This class is parameterized by the type of its data container ({@code <D>}) and the type of its values's (Java) equivalent
 * object ({@code <C>}). Only a limited set of {@code <D,C>} pairs are allowed, and are as follows:
 * <p>
 * <table border="1" cellpadding="3" style="border-collapse: collapse; width: 0">
 *     <tr>
 *         <th>Data container (<code>D</code>)</th>
 *         <th>Data object equivalent (<code>C</code>)</th>
 *     </tr>
 *     <tr>
 *         <td><code>byte[][]</code></td>
 *         <td><code>Byte</code></td>
 *     </tr>
 *     <tr>
 *         <td><code>short[][]</code></td>
 *         <td><code>Short</code></td>
 *     </tr>
 *     <tr>
 *         <td><code>int[][]</code></td>
 *         <td><code>Integer</code></td>
 *     </tr>
 *     <tr>
 *         <td><code>float[][]</code></td>
 *         <td><code>Float</code></td>
 *     </tr>
 *     <tr>
 *         <td><code>double[][]</code></td>
 *         <td><code>Double</code></td>
 *     </tr>
 *     <tr>
 *         <td><code>String[][]</code></td>
 *         <td><code>String</code></td>
 *     </tr>
 * </table>
 * </p>
 * <p>
 * Because of this restriction, instances of this classes will generally not be (directly) needed. Instead, one of the
 * subclasses created from the static {@code Omx[DATATYPE]Matrix} classes provided in this class should be used.
 * <p>
 * Note that instances of this class intentionally do not provide full immutability guarantees. First, the missing value
 * identifier (which indicates the matrix value that should represent a "missing" value) may be changed/set after construction.
 * Additionally, while the data container may not be changed, its values are allowed to be modified; the OmxMatrix instance
 * will detect any changes and write them to the OMX file if a save is requested. For large matrices, this avoids the
 * need to copy large (internal) data arrays when modifying data in-place.
 *
 * @param <D>
 *        The type of the data container the matrix uses. This will always be a two-dimensional array of some sort.
 *
 * @param <C>
 *        The object-equivalent type of the component (base) type of {@code <D>}.
 *
 * @author crf
 *         Started 9/20/13 7:58 PM
 */
public class OmxMatrix<D,C> extends AttributedElement {
    private final String name;
    private final D data;
    private final int[] shape;
    private final Class<?> componentClass;
    private final Class<C> objectEquivalentClass;
    private volatile C missingValue;
    private final int initialHashcode;

    /**
     * Constructor specifying the name, data, and missing value for the matrix.
     *
     * @param name
     *        The matrix name.
     *
     * @param data
     *        The matrix data. This should be a rectangular (not jagged) array.
     *
     * @param missingValue
     *        The value to use for missing values in the matrix.
     *
     * @throws IllegalArgumentException if {@code data} is not a two-dimensional array or is empty; or if the component class
     *                                  of {@code data} is not one of the allowed types or does not match the object-equivalent
     *                                  class of {@code missingValue}.
     */
    public OmxMatrix(String name, D data, C missingValue) {
        this.name = name;
        if (!data.getClass().isArray())
            throw new IllegalArgumentException("Data must be a two dimensional array");
        int dim0 = Array.getLength(data);
        if (dim0 == 0)
            throw new IllegalArgumentException("Data array cannot be empty");
        //assume rectangular; if not, buyer beware!
        Object subD = Array.get(data,0);
        if (!subD.getClass().isArray())
            throw new IllegalArgumentException("Data must be a two dimensional array");
        int dim1 = Array.getLength(subD);
        if (dim1 == 0)
            throw new IllegalArgumentException("Data array cannot be empty");
        componentClass = subD.getClass().getComponentType();
        if (!ALLOWED_CLASS_TO_OBJECT_MAP.containsKey(componentClass))
            throw new IllegalArgumentException("Component class invalid: " + componentClass);
        @SuppressWarnings("unchecked") //correct, if rules are followed
        Class<C> objectEquivalentClass = (Class<C>) ALLOWED_CLASS_TO_OBJECT_MAP.get(componentClass);
        this.objectEquivalentClass = objectEquivalentClass;
        this.data = data;
        shape = new int[] {dim0,dim1};
        this.missingValue = missingValue;
        if ((missingValue != null) && (missingValue.getClass() != objectEquivalentClass))
            throw new IllegalStateException("Object equivalent component class (" + objectEquivalentClass + ") does not match missing value class: " + missingValue.getClass());
        if ((missingValue != null) && (!getAttributeKeys().contains(OmxConstants.OmxNames.OMX_DATASET_NA_KEY.getKey())))
            setAttribute(OmxConstants.OmxNames.OMX_DATASET_NA_KEY.getKey(),missingValue);
        initialHashcode = dataHashcode();
    }

    private int dataHashcode() {
        return Arrays.deepHashCode((Object[]) data);
    }

    /**
     * Determine if the data held by this matrix has been modified since its initial construction.
     *
     * @return {@code true} if the data has been changed, {@code false} otherwise.
     */
    public boolean isDataModified() {
        return initialHashcode != dataHashcode();
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
     * Get this matrix's data.
     *
     * @return the data held by this matrix.
     */
    public D getData() {
        return data;
    }

    /**
     * Get this matrix's shape. The shape is a two-element matrix containing, in order, the length of its first dimension
     * (rows) and the length of its second dimension (columns).
     *
     * @return the shape of this matrix.
     */
    public int[] getShape() {
        return Arrays.copyOf(shape,shape.length);
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
     * Get the value used by this matrix to indicate missing values. Note that this class only maintains this value; it is
     * up to users of this class to combine it with the actual matrix data to create  missing value functionality.
     *
     * @return this matrix's missing value value.
     */
    public C getMissingValue() {
        return missingValue;
    }

    /**
     * Set the value used by this matrix to indicate missing values.
     *
     * @param missingValue
     *        The value to use for missing values.
     */
    public void setMissingValue(C missingValue) {
        this.missingValue = missingValue;
        setAttribute(OmxConstants.OmxNames.OMX_DATASET_NA_KEY.getKey(),missingValue);
    }

    /**
     * Get the OMX Java type that corresponds to this matrix instance.
     *
     * @return this matrix's equivalent OMX Java type.
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
     * Transfer the data held in this matrix to an OMX dataset. This functionality is mainly intended for users interacting
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
        dataset.setData(getData());
    }

    private static final Map<Class<?>,Class<?>> ALLOWED_CLASS_TO_OBJECT_MAP = new HashMap<>();
    static {
        ALLOWED_CLASS_TO_OBJECT_MAP.put(byte.class,Byte.class);
        ALLOWED_CLASS_TO_OBJECT_MAP.put(short.class,Short.class);
        ALLOWED_CLASS_TO_OBJECT_MAP.put(int.class,Integer.class);
        ALLOWED_CLASS_TO_OBJECT_MAP.put(float.class,Float.class);
        ALLOWED_CLASS_TO_OBJECT_MAP.put(double.class,Double.class);
        ALLOWED_CLASS_TO_OBJECT_MAP.put(String .class,String .class);
    }

    /**
     * The {@code OmxByteMatrix} is an {@code OmxMatrix} which holds bytes.
     */
    public static class OmxByteMatrix extends OmxMatrix<byte[][],Byte> {

        /**
         * Constructor specifying the name, data, and missing value for the matrix.
         *
         * @param name
         *        The matrix name.
         *
         * @param data
         *        The matrix data. This should be a rectangular (not jagged) array.
         *
         * @param missingValue
         *        The value to use for missing values in the matrix.
         *
         * @throws IllegalArgumentException if {@code data} is not a two-dimensional array or is empty.
         */
        public OmxByteMatrix(String name, byte[][] data, Byte missingValue) {
            super(name,data,missingValue);
        }
    }

    /**
     * The {@code OmxShortMatrix} is an {@code OmxMatrix} which holds shorts.
     */
    public static class OmxShortMatrix extends OmxMatrix<short[][],Short> {
        /**
         * Constructor specifying the name, data, and missing value for the matrix.
         *
         * @param name
         *        The matrix name.
         *
         * @param data
         *        The matrix data. This should be a rectangular (not jagged) array.
         *
         * @param missingValue
         *        The value to use for missing values in the matrix.
         *
         * @throws IllegalArgumentException if {@code data} is not a two-dimensional array or is empty.
         */
        public OmxShortMatrix(String name, short[][] data, Short missingValue) {
            super(name,data,missingValue);
        }
    }

    /**
     * The {@code OmxIntMatrix} is an {@code OmxMatrix} which holds ints.
     */
    public static class OmxIntMatrix extends OmxMatrix<int[][],Integer> {

        /**
         * Constructor specifying the name, data, and missing value for the matrix.
         *
         * @param name
         *        The matrix name.
         *
         * @param data
         *        The matrix data. This should be a rectangular (not jagged) array.
         *
         * @param missingValue
         *        The value to use for missing values in the matrix.
         *
         * @throws IllegalArgumentException if {@code data} is not a two-dimensional array or is empty.
         */
        public OmxIntMatrix(String name, int[][] data, Integer missingValue) {
            super(name,data,missingValue);
        }
    }

    /**
     * The {@code OmxFloatMatrix} is an {@code OmxMatrix} which holds floats.
     */
    public static class OmxFloatMatrix extends OmxMatrix<float[][],Float> {

        /**
         * Constructor specifying the name, data, and missing value for the matrix.
         *
         * @param name
         *        The matrix name.
         *
         * @param data
         *        The matrix data. This should be a rectangular (not jagged) array.
         *
         * @param missingValue
         *        The value to use for missing values in the matrix.
         *
         * @throws IllegalArgumentException if {@code data} is not a two-dimensional array or is empty.
         */
        public OmxFloatMatrix(String name, float[][] data, Float missingValue) {
            super(name,data,missingValue);
        }
    }

    /**
     * The {@code OmxDoubleMatrix} is an {@code OmxMatrix} which holds doubles.
     */
    public static class OmxDoubleMatrix extends OmxMatrix<double[][],Double> {

        /**
         * Constructor specifying the name, data, and missing value for the matrix.
         *
         * @param name
         *        The matrix name.
         *
         * @param data
         *        The matrix data. This should be a rectangular (not jagged) array.
         *
         * @param missingValue
         *        The value to use for missing values in the matrix.
         *
         * @throws IllegalArgumentException if {@code data} is not a two-dimensional array or is empty.
         */
        public OmxDoubleMatrix(String name, double[][] data, Double missingValue) {
            super(name,data,missingValue);
        }
    }

    /**
     * The {@code OmxStringMatrix} is an {@code OmxMatrix} which holds strings (text).
     */
    public static class OmxStringMatrix extends OmxMatrix<String[][],String> {

        /**
         * Constructor specifying the name, data, and missing value for the matrix.
         *
         * @param name
         *        The matrix name.
         *
         * @param data
         *        The matrix data. This should be a rectangular (not jagged) array.
         *
         * @param missingValue
         *        The value to use for missing values in the matrix.
         *
         * @throws IllegalArgumentException if {@code data} is not a two-dimensional array or is empty.
         */
        public OmxStringMatrix(String name, String[][] data, String missingValue) {
            super(name,data,missingValue);
        }
    }

    /**
     * Get an {@code OmxMatrix} corresponding to an OMX dataset. The returned matrix will have an implicit connection to
     * the source dataset, which can be useful when modifying and/or saving data. Note that this method will fail if the
     * dataset holds an unallowed type or is not two-dimensional.
     *
     * @param dataset
     *        The dataset that the matrix will be built from.
     *
     * @return a matrix representing {@code dataset}.
     */
    public static OmxMatrix getMatrix(OmxDataset dataset) {
        OmxHdf5Datatype datatype = dataset.getDatatype();
        Map<String,Object> attributes = dataset.getAttributes();
        Object missingValue = null;
        if (attributes.containsKey(OmxConstants.OmxNames.OMX_DATASET_NA_KEY.getKey()))
            missingValue = attributes.get(OmxConstants.OmxNames.OMX_DATASET_NA_KEY.getKey());
        String name = Hdf5Util.getBaseName(dataset.getName());
        OmxMatrix matrix = null;
        switch (datatype.getOmxJavaType()) {
            case BYTE : {
                byte[][] data = (byte[][]) dataset.getData();
                matrix = new OmxByteMatrix(name,data,(Byte) missingValue);
            } break;
            case SHORT : {
                short[][] data = (short[][]) dataset.getData();
                matrix = new OmxShortMatrix(name,data,(Short) missingValue);
            } break;
            case INT : {
                int[][] data = (int[][]) dataset.getData();
                matrix = new OmxIntMatrix(name,data,(Integer) missingValue);
            } break;
            case FLOAT : {
                float[][] data = (float[][]) dataset.getData();
                matrix = new OmxFloatMatrix(name,data,(Float) missingValue);
            } break;
            case DOUBLE : {
                double[][] data = (double[][]) dataset.getData();
                matrix = new OmxDoubleMatrix(name,data,(Double) missingValue);
            } break;
            case STRING : {
                String[][] data = (String[][]) dataset.getData();
                matrix = new OmxStringMatrix(name,data,(String) missingValue);
            } break;
        }
        for (String key : attributes.keySet())
            matrix.setAttribute(key,attributes.get(key));
        return matrix;
    }
}
