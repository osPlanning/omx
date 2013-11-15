package omx.hdf5;

import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import ncsa.hdf.hdf5lib.exceptions.HDF5LibraryException;

import java.lang.reflect.Array;
import java.util.*;

/**
 * The {@code OmxHdf5Dataset} ...
 *
 * @author crf
 *         Started 8/18/13 9:34 AM
 */
public class OmxHdf5Dataset implements OmxDataset {
    private final int fileId;
    private final String name;
    private final int[] shape;
    private final OmxHdf5Datatype datatype;
    private final Map<String,Object> attributes;
    private final OmxMutableDataset mutableDataset;

    OmxHdf5Dataset(int fileId, String datasetName, String parentName) {
        this.fileId = fileId;
        name = parentName + (parentName.endsWith("/") ? "" : "/") + datasetName;
        int datasetId = -1;
        try {
            datasetId = H5.H5Dopen(fileId,name,HDF5Constants.H5P_DEFAULT);

            //get datatype
            datatype = new OmxHdf5Datatype(H5.H5Dget_type(datasetId));

            //get shape
            int dataspaceId = H5.H5Dget_space(datasetId);
            long nDims = H5.H5Sget_simple_extent_ndims(dataspaceId);
            long[] dimSize = new long[(int) nDims];
            long[] maxDimSize =  new long[(int) nDims];
            H5.H5Sget_simple_extent_dims(dataspaceId,dimSize,maxDimSize);
            shape = new int[dimSize.length];
            for (int i = 0; i < dimSize.length; i++)
                shape[i] = (int) dimSize[i];

            //get attributes
            attributes = Hdf5Util.getAttributes(datasetId);

        } catch (HDF5Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (datasetId > -1) {
                try {
                    H5.H5Dclose(datasetId);
                } catch (HDF5LibraryException e) {
                    throw new RuntimeException(e); //should this be logged & swallowed instead? todo
                }
            }
        }
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
    public Map<String,Object> getAttributes() { //note: values will not be immutable, so be careful... todo
        return Collections.unmodifiableMap(attributes);
    }

    private void copyData(Object arrayForData) {
        int datasetId = -1;
        try {
            datasetId = H5.H5Dopen(fileId,name,HDF5Constants.H5P_DEFAULT);
            H5.H5Dread(datasetId,getDatatype().getOmxJavaType().getHdf5NativeId(),HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL,HDF5Constants.H5P_DEFAULT,arrayForData);
        } catch (HDF5Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (datasetId > -1) {
                try {
                    H5.H5Dclose(datasetId);
                } catch (HDF5LibraryException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public Object getData() {
        Object arrayForData = Array.newInstance(getDatatype().getOmxJavaType().getJavaClass(),getShape());
        copyData(arrayForData);
        return arrayForData;
    }

    @Override
    public OmxMutableDataset getMutableDataset() {
        return mutableDataset;
    }
}
