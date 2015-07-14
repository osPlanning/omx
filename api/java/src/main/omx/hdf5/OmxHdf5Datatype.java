package omx.hdf5;

import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.hdf5lib.exceptions.HDF5LibraryException;
import ncsa.hdf.object.h5.H5Datatype;

import java.util.Objects;

/**
 * The {@code OmxHdf5Datatype} ...
 *
 * @author crf
 *         Started 8/18/13 10:43 AM
 */
public class OmxHdf5Datatype {
    private final int datatypeId;
    private final String datatypeName;
    private final OmxJavaType omxJavaType;

    public OmxHdf5Datatype(int datatypeId) {
        this.datatypeId = datatypeId;
        datatypeName = H5Datatype.getDatatypeDescription(datatypeId);
        omxJavaType = OmxJavaType.getJavaTypeForHdf5Id(datatypeId);
    }

    public boolean equals(Object other) {
        if ((other == null) || !(other instanceof OmxHdf5Datatype))
            return false;
        OmxHdf5Datatype omxHdf5Datatype = (OmxHdf5Datatype) other;
        return omxJavaType == omxHdf5Datatype.getOmxJavaType();
    }

    public int hashCode() {
        return Objects.hashCode(omxJavaType);
    }

    public int getNativeDatatypeId() {
        try {
            return H5.H5Tget_native_type(datatypeId);
        } catch (HDF5LibraryException e) {
            throw new RuntimeException(e);
        }
    }

    public int getDatatypeId() {
        return datatypeId;
    }

    public String getDatatypeName() {
        return datatypeName;
    }

    public OmxJavaType getOmxJavaType() {
        return omxJavaType;
    }

    public static enum OmxJavaType {
        INT(HDF5Constants.H5T_NATIVE_INT,int.class,4),
        SHORT(HDF5Constants.H5T_NATIVE_SHORT,short.class,2),
        FLOAT(HDF5Constants.H5T_NATIVE_FLOAT,float.class,4),
        DOUBLE(HDF5Constants.H5T_NATIVE_DOUBLE,double.class,8),
        BYTE(HDF5Constants.H5T_NATIVE_CHAR,byte.class,1),
        STRING(HDF5Constants.H5T_C_S1,String.class,-1);

        private final int hdf5NativeId;
        private final Class<?> javaClass;
        private final int dataLength;

        private OmxJavaType(int hdf5NativeId, Class<?> javaClass, int dataLength) {
            this.hdf5NativeId = hdf5NativeId;
            this.javaClass = javaClass;
            this.dataLength = dataLength;
        }

        public static OmxJavaType getJavaTypeForHdf5Id(int hdf5Id) {
            try {
                int hdf5Native = H5.H5Tget_native_type(hdf5Id);
                
                //System.out.println(H5Datatype.getDatatypeDescription(hdf5Native).toLowerCase());
                
                if (H5Datatype.getDatatypeDescription(hdf5Native).toLowerCase().contains("32-bit integer"))
                    return INT;
                if (H5Datatype.getDatatypeDescription(hdf5Native).toLowerCase().contains("32-bit unsigned integer"))
                    return INT;
                
                if (H5Datatype.getDatatypeDescription(hdf5Native).toLowerCase().contains("16-bit integer"))
                    return SHORT;
                if (H5Datatype.getDatatypeDescription(hdf5Native).toLowerCase().contains("16-bit unsigned integer"))
                    return SHORT;
                
                if (H5Datatype.getDatatypeDescription(hdf5Native).toLowerCase().contains("32-bit float"))
                    return FLOAT;
                if (H5Datatype.getDatatypeDescription(hdf5Native).toLowerCase().contains("64-bit float"))
                    return DOUBLE;
                
                if (H5Datatype.getDatatypeDescription(hdf5Native).toLowerCase().contains("8-bit integer"))
                    return BYTE;
                if (H5Datatype.getDatatypeDescription(hdf5Native).toLowerCase().contains("8-bit unsigned integer"))
                    return BYTE;
                
                if (H5Datatype.getDatatypeDescription(hdf5Native).toLowerCase().contains("string"))
                    return STRING;

                return null;

            } catch (HDF5LibraryException e) {
                throw new RuntimeException(e);
            }
        }

        public Class<?> getJavaClass() {
            return javaClass;
        }

        public int getHdf5NativeId() {
            return hdf5NativeId;
        }

        public int getDataLength() {
            return dataLength;
        }

        public OmxHdf5Datatype getOmxHdf5Datatype() {
            return new OmxHdf5Datatype(hdf5NativeId);
        }
    }

    public static enum H5Type {
        H5T_ALPHA_B16(HDF5Constants.H5T_ALPHA_B16),
        H5T_ALPHA_B32(HDF5Constants.H5T_ALPHA_B32),
        H5T_ALPHA_B64(HDF5Constants.H5T_ALPHA_B64),
        H5T_ALPHA_B8(HDF5Constants.H5T_ALPHA_B8),
        H5T_ALPHA_F32(HDF5Constants.H5T_ALPHA_F32),
        H5T_ALPHA_F64(HDF5Constants.H5T_ALPHA_F64),
        H5T_ALPHA_I16(HDF5Constants.H5T_ALPHA_I16),
        H5T_ALPHA_I32(HDF5Constants.H5T_ALPHA_I32),
        H5T_ALPHA_I64(HDF5Constants.H5T_ALPHA_I64),
        H5T_ALPHA_I8(HDF5Constants.H5T_ALPHA_I8),
        H5T_ALPHA_U16(HDF5Constants.H5T_ALPHA_U16),
        H5T_ALPHA_U32(HDF5Constants.H5T_ALPHA_U32),
        H5T_ALPHA_U64(HDF5Constants.H5T_ALPHA_U64),
        H5T_ALPHA_U8(HDF5Constants.H5T_ALPHA_U8),
        H5T_ARRAY(HDF5Constants.H5T_ARRAY),
        H5T_BITFIELD(HDF5Constants.H5T_BITFIELD),
        H5T_BKG_NO(HDF5Constants.H5T_BKG_NO),
        H5T_BKG_YES(HDF5Constants.H5T_BKG_YES),
        H5T_C_S1(HDF5Constants.H5T_C_S1),
        H5T_COMPOUND(HDF5Constants.H5T_COMPOUND),
        H5T_CONV_CONV(HDF5Constants.H5T_CONV_CONV),
        H5T_CONV_FREE(HDF5Constants.H5T_CONV_FREE),
        H5T_CONV_INIT(HDF5Constants.H5T_CONV_INIT),
        H5T_CSET_ASCII(HDF5Constants.H5T_CSET_ASCII),
        H5T_CSET_ERROR(HDF5Constants.H5T_CSET_ERROR),
        H5T_CSET_RESERVED_10(HDF5Constants.H5T_CSET_RESERVED_10),
        H5T_CSET_RESERVED_11(HDF5Constants.H5T_CSET_RESERVED_11),
        H5T_CSET_RESERVED_12(HDF5Constants.H5T_CSET_RESERVED_12),
        H5T_CSET_RESERVED_13(HDF5Constants.H5T_CSET_RESERVED_13),
        H5T_CSET_RESERVED_14(HDF5Constants.H5T_CSET_RESERVED_14),
        H5T_CSET_RESERVED_15(HDF5Constants.H5T_CSET_RESERVED_15),
        H5T_CSET_RESERVED_2(HDF5Constants.H5T_CSET_RESERVED_2),
        H5T_CSET_RESERVED_3(HDF5Constants.H5T_CSET_RESERVED_3),
        H5T_CSET_RESERVED_4(HDF5Constants.H5T_CSET_RESERVED_4),
        H5T_CSET_RESERVED_5(HDF5Constants.H5T_CSET_RESERVED_5),
        H5T_CSET_RESERVED_6(HDF5Constants.H5T_CSET_RESERVED_6),
        H5T_CSET_RESERVED_7(HDF5Constants.H5T_CSET_RESERVED_7),
        H5T_CSET_RESERVED_8(HDF5Constants.H5T_CSET_RESERVED_8),
        H5T_CSET_RESERVED_9(HDF5Constants.H5T_CSET_RESERVED_9),
        H5T_CSET_UTF8(HDF5Constants.H5T_CSET_UTF8),
        H5T_DIR_ASCEND(HDF5Constants.H5T_DIR_ASCEND),
        H5T_DIR_DEFAULT(HDF5Constants.H5T_DIR_DEFAULT),
        H5T_DIR_DESCEND(HDF5Constants.H5T_DIR_DESCEND),
        H5T_ENUM(HDF5Constants.H5T_ENUM),
        H5T_FLOAT(HDF5Constants.H5T_FLOAT),
        H5T_FORTRAN_S1(HDF5Constants.H5T_FORTRAN_S1),
        H5T_IEEE_F32BE(HDF5Constants.H5T_IEEE_F32BE),
        H5T_IEEE_F32LE(HDF5Constants.H5T_IEEE_F32LE),
        H5T_IEEE_F64BE(HDF5Constants.H5T_IEEE_F64BE),
        H5T_IEEE_F64LE(HDF5Constants.H5T_IEEE_F64LE),
        H5T_INTEGER(HDF5Constants.H5T_INTEGER),
        H5T_INTEL_B16(HDF5Constants.H5T_INTEL_B16),
        H5T_INTEL_B32(HDF5Constants.H5T_INTEL_B32),
        H5T_INTEL_B64(HDF5Constants.H5T_INTEL_B64),
        H5T_INTEL_B8(HDF5Constants.H5T_INTEL_B8),
        H5T_INTEL_F32(HDF5Constants.H5T_INTEL_F32),
        H5T_INTEL_F64(HDF5Constants.H5T_INTEL_F64),
        H5T_INTEL_I16(HDF5Constants.H5T_INTEL_I16),
        H5T_INTEL_I32(HDF5Constants.H5T_INTEL_I32),
        H5T_INTEL_I64(HDF5Constants.H5T_INTEL_I64),
        H5T_INTEL_I8(HDF5Constants.H5T_INTEL_I8),
        H5T_INTEL_U16(HDF5Constants.H5T_INTEL_U16),
        H5T_INTEL_U32(HDF5Constants.H5T_INTEL_U32),
        H5T_INTEL_U64(HDF5Constants.H5T_INTEL_U64),
        H5T_INTEL_U8(HDF5Constants.H5T_INTEL_U8),
        H5T_MIPS_B16(HDF5Constants.H5T_MIPS_B16),
        H5T_MIPS_B32(HDF5Constants.H5T_MIPS_B32),
        H5T_MIPS_B64(HDF5Constants.H5T_MIPS_B64),
        H5T_MIPS_B8(HDF5Constants.H5T_MIPS_B8),
        H5T_MIPS_F32(HDF5Constants.H5T_MIPS_F32),
        H5T_MIPS_F64(HDF5Constants.H5T_MIPS_F64),
        H5T_MIPS_I16(HDF5Constants.H5T_MIPS_I16),
        H5T_MIPS_I32(HDF5Constants.H5T_MIPS_I32),
        H5T_MIPS_I64(HDF5Constants.H5T_MIPS_I64),
        H5T_MIPS_I8(HDF5Constants.H5T_MIPS_I8),
        H5T_MIPS_U16(HDF5Constants.H5T_MIPS_U16),
        H5T_MIPS_U32(HDF5Constants.H5T_MIPS_U32),
        H5T_MIPS_U64(HDF5Constants.H5T_MIPS_U64),
        H5T_MIPS_U8(HDF5Constants.H5T_MIPS_U8),
        H5T_NATIVE_B16(HDF5Constants.H5T_NATIVE_B16),
        H5T_NATIVE_B32(HDF5Constants.H5T_NATIVE_B32),
        H5T_NATIVE_B64(HDF5Constants.H5T_NATIVE_B64),
        H5T_NATIVE_B8(HDF5Constants.H5T_NATIVE_B8),
        H5T_NATIVE_CHAR(HDF5Constants.H5T_NATIVE_CHAR),
        H5T_NATIVE_DOUBLE(HDF5Constants.H5T_NATIVE_DOUBLE),
        H5T_NATIVE_FLOAT(HDF5Constants.H5T_NATIVE_FLOAT),
        H5T_NATIVE_HADDR(HDF5Constants.H5T_NATIVE_HADDR),
        H5T_NATIVE_HBOOL(HDF5Constants.H5T_NATIVE_HBOOL),
        H5T_NATIVE_HERR(HDF5Constants.H5T_NATIVE_HERR),
        H5T_NATIVE_HSIZE(HDF5Constants.H5T_NATIVE_HSIZE),
        H5T_NATIVE_HSSIZE(HDF5Constants.H5T_NATIVE_HSSIZE),
        H5T_NATIVE_INT(HDF5Constants.H5T_NATIVE_INT),
        H5T_NATIVE_INT_FAST16(HDF5Constants.H5T_NATIVE_INT_FAST16),
        H5T_NATIVE_INT_FAST32(HDF5Constants.H5T_NATIVE_INT_FAST32),
        H5T_NATIVE_INT_FAST64(HDF5Constants.H5T_NATIVE_INT_FAST64),
        H5T_NATIVE_INT_FAST8(HDF5Constants.H5T_NATIVE_INT_FAST8),
        H5T_NATIVE_INT_LEAST16(HDF5Constants.H5T_NATIVE_INT_LEAST16),
        H5T_NATIVE_INT_LEAST32(HDF5Constants.H5T_NATIVE_INT_LEAST32),
        H5T_NATIVE_INT_LEAST64(HDF5Constants.H5T_NATIVE_INT_LEAST64),
        H5T_NATIVE_INT_LEAST8(HDF5Constants.H5T_NATIVE_INT_LEAST8),
        H5T_NATIVE_INT16(HDF5Constants.H5T_NATIVE_INT16),
        H5T_NATIVE_INT32(HDF5Constants.H5T_NATIVE_INT32),
        H5T_NATIVE_INT64(HDF5Constants.H5T_NATIVE_INT64),
        H5T_NATIVE_INT8(HDF5Constants.H5T_NATIVE_INT8),
        H5T_NATIVE_LDOUBLE(HDF5Constants.H5T_NATIVE_LDOUBLE),
        H5T_NATIVE_LLONG(HDF5Constants.H5T_NATIVE_LLONG),
        H5T_NATIVE_LONG(HDF5Constants.H5T_NATIVE_LONG),
        H5T_NATIVE_OPAQUE(HDF5Constants.H5T_NATIVE_OPAQUE),
        H5T_NATIVE_SCHAR(HDF5Constants.H5T_NATIVE_SCHAR),
        H5T_NATIVE_SHORT(HDF5Constants.H5T_NATIVE_SHORT),
        H5T_NATIVE_UCHAR(HDF5Constants.H5T_NATIVE_UCHAR),
        H5T_NATIVE_UINT(HDF5Constants.H5T_NATIVE_UINT),
        H5T_NATIVE_UINT_FAST16(HDF5Constants.H5T_NATIVE_UINT_FAST16),
        H5T_NATIVE_UINT_FAST32(HDF5Constants.H5T_NATIVE_UINT_FAST32),
        H5T_NATIVE_UINT_FAST64(HDF5Constants.H5T_NATIVE_UINT_FAST64),
        H5T_NATIVE_UINT_FAST8(HDF5Constants.H5T_NATIVE_UINT_FAST8),
        H5T_NATIVE_UINT_LEAST16(HDF5Constants.H5T_NATIVE_UINT_LEAST16),
        H5T_NATIVE_UINT_LEAST32(HDF5Constants.H5T_NATIVE_UINT_LEAST32),
        H5T_NATIVE_UINT_LEAST64(HDF5Constants.H5T_NATIVE_UINT_LEAST64),
        H5T_NATIVE_UINT_LEAST8(HDF5Constants.H5T_NATIVE_UINT_LEAST8),
        H5T_NATIVE_UINT16(HDF5Constants.H5T_NATIVE_UINT16),
        H5T_NATIVE_UINT32(HDF5Constants.H5T_NATIVE_UINT32),
        H5T_NATIVE_UINT64(HDF5Constants.H5T_NATIVE_UINT64),
        H5T_NATIVE_UINT8(HDF5Constants.H5T_NATIVE_UINT8),
        H5T_NATIVE_ULLONG(HDF5Constants.H5T_NATIVE_ULLONG),
        H5T_NATIVE_ULONG(HDF5Constants.H5T_NATIVE_ULONG),
        H5T_NATIVE_USHORT(HDF5Constants.H5T_NATIVE_USHORT),
        H5T_NCLASSES(HDF5Constants.H5T_NCLASSES),
        H5T_NO_CLASS(HDF5Constants.H5T_NO_CLASS),
        H5T_NORM_ERROR(HDF5Constants.H5T_NORM_ERROR),
        H5T_NORM_IMPLIED(HDF5Constants.H5T_NORM_IMPLIED),
        H5T_NORM_MSBSET(HDF5Constants.H5T_NORM_MSBSET),
        H5T_NORM_NONE(HDF5Constants.H5T_NORM_NONE),
        H5T_NPAD(HDF5Constants.H5T_NPAD),
        H5T_NSGN(HDF5Constants.H5T_NSGN),
        H5T_OPAQUE(HDF5Constants.H5T_OPAQUE),
        H5T_OPAQUE_TAG_MAX(HDF5Constants.H5T_OPAQUE_TAG_MAX),
        H5T_ORDER_BE(HDF5Constants.H5T_ORDER_BE),
        H5T_ORDER_ERROR(HDF5Constants.H5T_ORDER_ERROR),
        H5T_ORDER_LE(HDF5Constants.H5T_ORDER_LE),
        H5T_ORDER_NONE(HDF5Constants.H5T_ORDER_NONE),
        H5T_ORDER_VAX(HDF5Constants.H5T_ORDER_VAX),
        H5T_PAD_BACKGROUND(HDF5Constants.H5T_PAD_BACKGROUND),
        H5T_PAD_ERROR(HDF5Constants.H5T_PAD_ERROR),
        H5T_PAD_ONE(HDF5Constants.H5T_PAD_ONE),
        H5T_PAD_ZERO(HDF5Constants.H5T_PAD_ZERO),
        H5T_PERS_DONTCARE(HDF5Constants.H5T_PERS_DONTCARE),
        H5T_PERS_HARD(HDF5Constants.H5T_PERS_HARD),
        H5T_PERS_SOFT(HDF5Constants.H5T_PERS_SOFT),
        H5T_REFERENCE(HDF5Constants.H5T_REFERENCE),
        H5T_SGN_2(HDF5Constants.H5T_SGN_2),
        H5T_SGN_ERROR(HDF5Constants.H5T_SGN_ERROR),
        H5T_SGN_NONE(HDF5Constants.H5T_SGN_NONE),
        H5T_STD_B16BE(HDF5Constants.H5T_STD_B16BE),
        H5T_STD_B16LE(HDF5Constants.H5T_STD_B16LE),
        H5T_STD_B32BE(HDF5Constants.H5T_STD_B32BE),
        H5T_STD_B32LE(HDF5Constants.H5T_STD_B32LE),
        H5T_STD_B64BE(HDF5Constants.H5T_STD_B64BE),
        H5T_STD_B64LE(HDF5Constants.H5T_STD_B64LE),
        H5T_STD_B8BE(HDF5Constants.H5T_STD_B8BE),
        H5T_STD_B8LE(HDF5Constants.H5T_STD_B8LE),
        H5T_STD_I16BE(HDF5Constants.H5T_STD_I16BE),
        H5T_STD_I16LE(HDF5Constants.H5T_STD_I16LE),
        H5T_STD_I32BE(HDF5Constants.H5T_STD_I32BE),
        H5T_STD_I32LE(HDF5Constants.H5T_STD_I32LE),
        H5T_STD_I64BE(HDF5Constants.H5T_STD_I64BE),
        H5T_STD_I64LE(HDF5Constants.H5T_STD_I64LE),
        H5T_STD_I8BE(HDF5Constants.H5T_STD_I8BE),
        H5T_STD_I8LE(HDF5Constants.H5T_STD_I8LE),
        H5T_STD_REF_DSETREG(HDF5Constants.H5T_STD_REF_DSETREG),
        H5T_STD_REF_OBJ(HDF5Constants.H5T_STD_REF_OBJ),
        H5T_STD_U16BE(HDF5Constants.H5T_STD_U16BE),
        H5T_STD_U16LE(HDF5Constants.H5T_STD_U16LE),
        H5T_STD_U32BE(HDF5Constants.H5T_STD_U32BE),
        H5T_STD_U32LE(HDF5Constants.H5T_STD_U32LE),
        H5T_STD_U64BE(HDF5Constants.H5T_STD_U64BE),
        H5T_STD_U64LE(HDF5Constants.H5T_STD_U64LE),
        H5T_STD_U8BE(HDF5Constants.H5T_STD_U8BE),
        H5T_STD_U8LE(HDF5Constants.H5T_STD_U8LE),
        H5T_STR_ERROR(HDF5Constants.H5T_STR_ERROR),
        H5T_STR_NULLPAD(HDF5Constants.H5T_STR_NULLPAD),
        H5T_STR_NULLTERM(HDF5Constants.H5T_STR_NULLTERM),
        H5T_STR_RESERVED_10(HDF5Constants.H5T_STR_RESERVED_10),
        H5T_STR_RESERVED_11(HDF5Constants.H5T_STR_RESERVED_11),
        H5T_STR_RESERVED_12(HDF5Constants.H5T_STR_RESERVED_12),
        H5T_STR_RESERVED_13(HDF5Constants.H5T_STR_RESERVED_13),
        H5T_STR_RESERVED_14(HDF5Constants.H5T_STR_RESERVED_14),
        H5T_STR_RESERVED_15(HDF5Constants.H5T_STR_RESERVED_15),
        H5T_STR_RESERVED_3(HDF5Constants.H5T_STR_RESERVED_3),
        H5T_STR_RESERVED_4(HDF5Constants.H5T_STR_RESERVED_4),
        H5T_STR_RESERVED_5(HDF5Constants.H5T_STR_RESERVED_5),
        H5T_STR_RESERVED_6(HDF5Constants.H5T_STR_RESERVED_6),
        H5T_STR_RESERVED_7(HDF5Constants.H5T_STR_RESERVED_7),
        H5T_STR_RESERVED_8(HDF5Constants.H5T_STR_RESERVED_8),
        H5T_STR_RESERVED_9(HDF5Constants.H5T_STR_RESERVED_9),
        H5T_STR_SPACEPAD(HDF5Constants.H5T_STR_SPACEPAD),
        H5T_STRING(HDF5Constants.H5T_STRING),
        H5T_TIME(HDF5Constants.H5T_TIME),
        H5T_UNIX_D32BE(HDF5Constants.H5T_UNIX_D32BE),
        H5T_UNIX_D32LE(HDF5Constants.H5T_UNIX_D32LE),
        H5T_UNIX_D64BE(HDF5Constants.H5T_UNIX_D64BE),
        H5T_UNIX_D64LE(HDF5Constants.H5T_UNIX_D64LE),
        H5T_VARIABLE(HDF5Constants.H5T_VARIABLE),
        H5T_VLEN(HDF5Constants.H5T_VLEN);

        private final int hdf5TypeId;

        private H5Type(int hdf5TypeId) {
            this.hdf5TypeId = hdf5TypeId;
        }

        public static H5Type getTypeForId(int hdf5Id) {
            for (H5Type type : values()) {
                try {
                    if (H5.H5Tequal(type.hdf5TypeId,hdf5Id))
                        return type;
                } catch (HDF5LibraryException e) {
                    //swallow
                }
            }
            return null;
        }
    }
}
