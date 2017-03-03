package omx.hdf5;

import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import ncsa.hdf.hdf5lib.exceptions.HDF5LibraryException;

import java.lang.reflect.Array;
import java.util.*;

import omx.hdf5.OmxHdf5Datatype.OmxJavaType;

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

    private Object copyData() {
        int datasetId = -1;
        
        //data container
        Object arrayForData = Array.newInstance(getDatatype().getOmxJavaType().getJavaClass(),getShape());
        
        try {
            datasetId = H5.H5Dopen(fileId,name,HDF5Constants.H5P_DEFAULT);
            
            //if matrix 
        	if(shape.length>1) {

        		//temp data container
                Object[] matrixData = new Object[shape[0]];
        		
        		//setup hyperslab i/o
            	long[] count = new long[2];
            	long[] offset= new long[2];
            	count[0] = 1;
            	count[1] = shape[1];
            	
                int memspace = H5.H5Screate_simple(2,count,null);
            	int dataspace = H5.H5Dget_space(datasetId);
            	
            	//read rows
                for (int i = 0; i < shape[0]; i++) {
                	
                	offset[0] = i;
                	offset[1] = 0;
                	
                	H5.H5Sselect_hyperslab (dataspace, HDF5Constants.H5S_SELECT_SET, offset, null, count, null);
                	Object rowdata = Array.newInstance(getDatatype().getOmxJavaType().getJavaClass(),shape[0]);
                	H5.H5Dread(datasetId,getDatatype().getOmxJavaType().getHdf5NativeId(),memspace, dataspace,HDF5Constants.H5P_DEFAULT,rowdata);
                	matrixData[i] = rowdata;
                }
                
                //typecast - this can probably be done in a better fashion
                OmxJavaType ojt = getDatatype().getOmxJavaType();
            	if (ojt.equals(OmxHdf5Datatype.OmxJavaType.INT)) {
            		int[][] tempData = new int[shape[0]][shape[1]];
            		for (int i = 0; i < shape[0]; i++) {
            			tempData[i] = (int[]) matrixData[i];
                    }
            		arrayForData = tempData;
            	} else if(ojt.equals(OmxHdf5Datatype.OmxJavaType.SHORT)) {
            		short[][] tempData = new short[shape[0]][shape[1]];
            		for (int i = 0; i < shape[0]; i++) {
            			tempData[i] = (short[]) matrixData[i];
                    }
            		arrayForData = tempData;
            	} else if(ojt.equals(OmxHdf5Datatype.OmxJavaType.FLOAT)) {
            		float[][] tempData = new float[shape[0]][shape[1]];
            		for (int i = 0; i < shape[0]; i++) {
            			tempData[i] = (float[]) matrixData[i];
                    }
            		arrayForData = tempData;
            	} else if(ojt.equals(OmxHdf5Datatype.OmxJavaType.DOUBLE)) {
            		double[][] tempData = new double[shape[0]][shape[1]];
            		for (int i = 0; i < shape[0]; i++) {
            			tempData[i] = (double[]) matrixData[i];
                    }
            		arrayForData = tempData;
            	} else if(ojt.equals(OmxHdf5Datatype.OmxJavaType.BYTE)) {
            		byte[][] tempData = new byte[shape[0]][shape[1]];
            		for (int i = 0; i < shape[0]; i++) {
            			tempData[i] = (byte[]) matrixData[i];
                    }
            		arrayForData = tempData;
            	} else if(ojt.equals(OmxHdf5Datatype.OmxJavaType.STRING)) {
            		String[][] tempData = new String[shape[0]][shape[1]];
            		for (int i = 0; i < shape[0]; i++) {
            			tempData[i] = (String[]) matrixData[i];
                    }
            		arrayForData = tempData;
            	}            	
                
        	} else {
        		H5.H5Dread(datasetId,getDatatype().getOmxJavaType().getHdf5NativeId(),HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL,HDF5Constants.H5P_DEFAULT,arrayForData);
        	}
            
        	return(arrayForData);
        	
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
        Object arrayForData = copyData();
        return arrayForData;
    }

    @Override
    public OmxMutableDataset getMutableDataset() {
        return mutableDataset;
    }
}
