package omx.hdf5;

import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import ncsa.hdf.hdf5lib.exceptions.HDF5LibraryException;
import ncsa.hdf.hdf5lib.structs.H5O_info_t;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * The {@code Hdf5Util} ...
 *
 * @author crf
 *         Started 8/18/13 7:15 PM
 */
public class Hdf5Util {

    public static Map<String,Object> getAttributes(int id) {
        Map<String,Object> attributes = new HashMap<>();
        try {
            H5O_info_t info = H5.H5Oget_info(id);
            for (int i = 0; i < info.num_attrs; i++) {
                int attributeId = H5.H5Aopen_by_idx(id,".",HDF5Constants.H5_INDEX_CRT_ORDER,HDF5Constants.H5_ITER_INC,i,HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
                String[] attributeNameHolder = new String[1];
                H5.H5Aget_name(attributeId,256,attributeNameHolder); //256 is from digging through HDF5 code, this is the max size for an attribute name
                int size = (int) H5.H5Aget_storage_size(attributeId);
                byte[] data = new byte[size];
                int attributeType = H5.H5Aget_type(attributeId);
                H5.H5Aread(attributeId,attributeType,data);
                attributes.put(attributeNameHolder[0],readData(attributeType,data));
                H5.H5Aclose(attributeId);
            }
        } catch (HDF5LibraryException e) {
            throw new RuntimeException(e);
        }
        return attributes;
    }

    public static void deleteAttributes(int id) {
        try {
            H5O_info_t info = H5.H5Oget_info(id);
            for (int i = 0; i < info.num_attrs; i++) {
                int attributeId = H5.H5Aopen_by_idx(id,".",HDF5Constants.H5_INDEX_CRT_ORDER,HDF5Constants.H5_ITER_INC,0,HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
                String[] attributeNameHolder = new String[1];
                H5.H5Aget_name(attributeId,256,attributeNameHolder); //256 is from digging through HDF5 code, this is the max size for an attribute name
                H5.H5Aclose(attributeId);
                H5.H5Adelete(id,attributeNameHolder[0]);
            }
        } catch (HDF5LibraryException e) {
            throw new RuntimeException(e);
        }
    }

    public static int getDatatype(Object o) {
        Class<?> c = o.getClass();
        while (c.isArray())
            c = c.getComponentType();
        try {
            if (c == byte.class || c == Byte.class)
                return H5.H5Tcopy(OmxHdf5Datatype.OmxJavaType.BYTE.getHdf5NativeId());
            if (c == short.class || c == Short.class)
                return H5.H5Tcopy(OmxHdf5Datatype.OmxJavaType.SHORT.getHdf5NativeId());
            if (c == int.class || c == Integer.class)
                return H5.H5Tcopy(OmxHdf5Datatype.OmxJavaType.INT.getHdf5NativeId());
            if (c == float.class || c == Float.class)
                return H5.H5Tcopy(OmxHdf5Datatype.OmxJavaType.FLOAT.getHdf5NativeId());
            if (c == double.class || c == Double.class)
                return H5.H5Tcopy(OmxHdf5Datatype.OmxJavaType.DOUBLE.getHdf5NativeId());
            if (c == String .class)
                return H5.H5Tcopy(OmxHdf5Datatype.OmxJavaType.STRING.getHdf5NativeId());
        } catch (HDF5LibraryException e) {
            throw new RuntimeException(e);
        }
        throw new IllegalArgumentException("Object cannot be saved to HDF5 datatype (" + c + "): " + o);
    }

    public static long[] getDataspaceDimensions(Object o) {
        List<Integer> size = new LinkedList<>();
        Class<?> c = o.getClass();
        if (c.isArray()) {
            while (c.isArray()) {
                size.add(Array.getLength(o));
                o = Array.get(o,0); //assume non-empty and rectangular
                c = o.getClass();
            }
        } else {
            size.add(1);
        }
        long[] space = new long[size.size()];
        int counter = 0;
        for (int s : size)
            space[counter++] = s;
        return space;
    }

    public static int getDataspace(long[] space) {
        try {
            return H5.H5Screate_simple(space.length,space,null);
        } catch (HDF5Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeAttribute(int id, String attributeName, Object attributeValue) {
        try {
            long[] dataspaceDimension = getDataspaceDimensions(attributeValue);
            int datatype = getDatatype(attributeValue);
            byte[] data = Hdf5Util.getData(attributeValue,dataspaceDimension);
            if (attributeValue.getClass() == String.class)
                H5.H5Tset_size(datatype,data.length);
            int dataspace = getDataspace(dataspaceDimension);
            int attribute = H5.H5Acreate(id,attributeName,datatype,dataspace,HDF5Constants.H5P_DEFAULT,HDF5Constants.H5P_DEFAULT);
            H5.H5Awrite(attribute,datatype,data);
            H5.H5Tclose(datatype);
            H5.H5Sclose(dataspace);
            H5.H5Aclose(attribute);
        } catch (HDF5LibraryException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object readData(int h5AttributeType, byte[] data) {
        OmxHdf5Datatype.OmxJavaType javaType = OmxHdf5Datatype.OmxJavaType.getJavaTypeForHdf5Id(h5AttributeType);
        if (javaType == null) //don't know this
            return data;
        int dataLength = javaType.getDataLength();
        if (dataLength > 0) //a sanity check for now
            assert data.length % dataLength == 0;
        switch (javaType) {
            case INT : {
                ByteBuffer byteBuffer = ByteBuffer.wrap(data);
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                if (data.length > dataLength) {
                    int[] result = new int[data.length / dataLength];
                    for (int i = 0; i < result.length; i++)
                        result[i] = byteBuffer.getInt();
                    return result;
                } else {
                    return byteBuffer.getInt();
                }
            }
            case SHORT : {
                ByteBuffer byteBuffer = ByteBuffer.wrap(data);
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                if (data.length > dataLength) {
                    short[] result = new short[data.length / dataLength];
                    for (int i = 0; i < result.length; i++)
                        result[i] = byteBuffer.getShort();
                    return result;
                } else {
                    return byteBuffer.getShort();
                }
            }
            case FLOAT : {
                ByteBuffer byteBuffer = ByteBuffer.wrap(data);
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                if (data.length > dataLength) {
                    float[] result = new float[data.length / dataLength];
                    for (int i = 0; i < result.length; i++)
                        result[i] = byteBuffer.getFloat();
                    return result;
                } else {
                    return byteBuffer.getFloat();
                }
            }
            case DOUBLE : {
                ByteBuffer byteBuffer = ByteBuffer.wrap(data);
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                if (data.length > dataLength) {
                    double[] result = new double[data.length / dataLength];
                    for (int i = 0; i < result.length; i++)
                        result[i] = byteBuffer.getDouble();
                    return result;
                } else {
                    return byteBuffer.getDouble();
                }
            }
            case BYTE : {
                ByteBuffer byteBuffer = ByteBuffer.wrap(data);
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                if (data.length > dataLength) {
                    byte[] result = new byte[data.length / dataLength];
                    for (int i = 0; i < result.length; i++)
                        result[i] = byteBuffer.get();
                    return result;
                } else {
                    return byteBuffer.get();
                }
            }
            case STRING : {
                return new String(data);
            }
            default : return data; //shouldn't get here, though
        }
    }

    public static enum H5O_type {
        H5O_TYPE_UNKNOWN(-1), // Unknown object type
        H5O_TYPE_GROUP(0), // Object is a group
        H5O_TYPE_DATASET(1), // Object is a dataset
        H5O_TYPE_NAMED_DATATYPE(2), // Object is a named data type
        H5O_TYPE_NTYPES(3); // Number of different object types
        private static final Map<Integer,H5O_type> lookup = new HashMap<Integer, H5O_type>();

        static {
            for (H5O_type s : EnumSet.allOf(H5O_type.class))
                lookup.put(s.getCode(), s);
        }

        private int code;

        H5O_type(int layout_type) {
            this.code = layout_type;
        }

        public int getCode() {
            return this.code;
        }

        public static H5O_type get(int code) {
            return lookup.get(code);
        }
    }

    public static byte[] getData(Object o, long[] size) {
        Class<?> c = o.getClass();
        if (!c.isArray()) {
            if (c == Byte.class)
                return getData((byte) (Byte) o);
            if (c == Short.class)
                return getData((short) (Short) o);
            if (c == Integer.class)
                return getData((int) (Integer) o);
            if (c == Float.class)
                return getData((float) (Float) o);
            if (c == Double.class)
                return getData((double) (Double) o);
            if (c == String.class)
                return getData((String) o);
            throw new IllegalArgumentException("Object cannot be saved to HDF5 datatype (" + c + "): " + o);
        }
        if (size.length == 1) {
            c = c.getComponentType();
            if ( c == byte.class)
                return getData((byte[]) o);
            if (c == short.class)
                return getData((short[]) o);
            if (c == int.class)
                return getData((int[]) o);
            if (c == float.class)
                return getData((float[]) o);
            if (c == double.class)
                return getData((double[]) o);
            throw new IllegalArgumentException("Object array (" + c + "[]) cannot be saved to HDF5 datatype: " + o);
        } else {
            int sizeCounter = 0;
            long[] subSize = new long[size.length-1];
            System.arraycopy(size,1,subSize,0,size.length - 1);
            List<byte[]> data = new LinkedList<>();
            for (Object oo : ((Object[]) o)) {
                byte[] byteData = getData(oo,subSize);
                sizeCounter += byteData.length;
                data.add(byteData);
            }
            byte[] finalData = new byte[sizeCounter];
            sizeCounter = 0;
            for (byte[] d : data) {
                System.arraycopy(d,0,finalData,sizeCounter,d.length);
                sizeCounter += d.length;
            }
            return finalData;
        }
    }

    public static byte[] getData(byte data) {
        return getData(new byte[] {data});
    }

    public static byte[] getData(short data) {
        return getData(new short[] {data});
    }

    public static byte[] getData(int data) {
        return getData(new int[] {data});
    }

    public static byte[] getData(float data) {
        return getData(new float[] {data});
    }

    public static byte[] getData(double data) {
        return getData(new double[] {data});
    }

    public static byte[] getData(byte[] data) {
        return data;
    }

    public static byte[] getData(short[] data) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(OmxHdf5Datatype.OmxJavaType.SHORT.getDataLength()*data.length);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        for (short i : data)
            byteBuffer.putShort(i);
        return byteBuffer.array();
    }

    public static byte[] getData(int[] data) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(OmxHdf5Datatype.OmxJavaType.INT.getDataLength()*data.length);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        for (int i : data)
            byteBuffer.putInt(i);
        return byteBuffer.array();
    }

    public static byte[] getData(float[] data) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(OmxHdf5Datatype.OmxJavaType.FLOAT.getDataLength()*data.length);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        for (float f : data)
            byteBuffer.putFloat(f);
        return byteBuffer.array();
    }

    public static byte[] getData(double[] data) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(OmxHdf5Datatype.OmxJavaType.DOUBLE.getDataLength()*data.length);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        for (double d : data)
            byteBuffer.putDouble(d);
        return byteBuffer.array();
    }

    public static byte[] getData(String data) {
        return data.getBytes();
    }

    public static String getBaseName(String name) {
        int lastSeparatorLocation = name.lastIndexOf("/");
        return lastSeparatorLocation == -1 ? name : name.substring(lastSeparatorLocation+1);
    }
}
