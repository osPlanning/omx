package omx.hdf5;

import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import ncsa.hdf.hdf5lib.exceptions.HDF5LibraryException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import omx.OmxLookup.OmxDoubleLookup;
import omx.OmxMatrix.OmxDoubleMatrix;
import omx.hdf5.OmxHdf5Datatype.OmxJavaType;

/**
 * The {@code OmxHdf5File} ...
 *
 * @author crf
 *         Started 8/18/13 9:03 AM
 */
public class OmxHdf5File implements AutoCloseable {
    private final Path filePath;
    private final AtomicBoolean opened;
    private final AtomicBoolean writable;
    private volatile OmxGroup baseGroup;
    private volatile int fileId;

    public OmxHdf5File(Path filePath) {
        this.filePath = filePath;
        opened = new AtomicBoolean(false);
        writable = new AtomicBoolean(false);
    }

    public OmxHdf5File(String filePath) {
        this(Paths.get(filePath));
    }

    public Path getFilePath() {
        return filePath;
    }

    public OmxGroup getBaseGroup() {
        return baseGroup;
    }

    public void openReadOnly() {
        open(FileMode.READ,null);
    }

    public void openReadWrite() {
        open(FileMode.WRITE,null);
    }

    public void openNew(int[] shape) {
        if (!isValidShape(shape))
            throw new IllegalStateException("Invalid shape: " + Arrays.toString(shape));
        open(FileMode.NEW,shape);
    }

    private void open(FileMode mode, int[] shape) { //shape only used if a new file
        if (opened.get())
            throw new IllegalStateException("File already opened: " + filePath);
        synchronized (opened) {
            int accessMode = -1;
            switch (mode) {
                case READ : {
                    if (!Files.exists(filePath))
                        throw new IllegalArgumentException("File path not found to read: " + filePath);
                    accessMode = HDF5Constants.H5F_ACC_RDONLY;
                    writable.set(false);
                } break;
                case WRITE : {
                    if (!Files.exists(filePath))
                        throw new IllegalArgumentException("File path not found to read: " + filePath);
                    accessMode = HDF5Constants.H5F_ACC_RDWR;
                    writable.set(true);
                } break;
                case NEW : {
                    accessMode = HDF5Constants.H5F_ACC_RDWR;
                    writable.set(true);
                } break;
            }

            //if new, we need to initialize the file first
            if (mode == FileMode.NEW) {
                try {
                    fileId = H5.H5Fcreate(filePath.toString(),HDF5Constants.H5F_ACC_TRUNC,HDF5Constants.H5P_DEFAULT,HDF5Constants.H5P_DEFAULT);
                    setupNewFile(shape);
                    H5.H5Fclose(fileId);
                } catch (HDF5LibraryException e) {
                   throw new RuntimeException(e);
               }
            }

            try {
                fileId = H5.H5Fopen(filePath.toString(),accessMode,HDF5Constants.H5P_DEFAULT);
            } catch (HDF5LibraryException e) {
                throw new RuntimeException(e);
            }

            baseGroup = new OmxHdf5Group(fileId,"","",true); //load if it is an existing file

            checkForConsistency();
            opened.set(true);
        }
    }

    private void checkForConsistency() {
        //checks:
        //X 1) must have OMX_VERSION in root
        //X 1a) version must be valid version
        //X 2) must have SHAPE in root
        //X 2a) SHAPE must be array of two positive integers
        //X 3) must have data group
        // 4) must have lookup group
        //X 5) all datasets in data group must have same shape
        //X 6) <not true> all datasets in data group must have same data type
        // 7) each lookup must have one dimension
        // 8) each lookup must have length == one of the dimensions defined in SHAPE
        OmxConstants.OmxVersion version = getOmxVersion();
        getShape(version);
        checkDatasetsForConsistency(version);
        checkLookupsForConsistency(version);
    }

    public OmxConstants.OmxVersion getOmxVersion() {
        Map<String,Object> attributes = getBaseGroup().getAttributes();
        if (!attributes.containsKey(OmxConstants.OmxNames.OMX_VERSION_KEY.getKey()))
            throw new IllegalStateException("OMX file missing version attribute: " + OmxConstants.OmxNames.OMX_VERSION_KEY.getKey());
        return OmxConstants.OmxVersion.getVersion((String) attributes.get(OmxConstants.OmxNames.OMX_VERSION_KEY.getKey()));
    }

    public int[] getShape() {
        return getShape(getOmxVersion());
    }

    private int[] getShape(OmxConstants.OmxVersion version) {
        Map<String,Object> attributes = getBaseGroup().getAttributes();
        if (!attributes.containsKey(OmxConstants.OmxNames.OMX_SHAPE_KEY.getKey()))
            throw new IllegalStateException("OMX file missing shape attribute: " + OmxConstants.OmxNames.OMX_SHAPE_KEY.getKey());
        int[] shape = (int[]) attributes.get(OmxConstants.OmxNames.OMX_SHAPE_KEY.getKey());
        if (!isValidShape(shape))
            throw new IllegalStateException("OMX file has invalid shape attribute: " + Arrays.toString(shape));
        return shape;
    }

    private void checkDatasetsForConsistency(OmxConstants.OmxVersion version) {
        if (!getBaseGroup().getGroupNames().contains(OmxConstants.OmxNames.OMX_DATA_GROUP.getKey()))
            throw new IllegalStateException("OMX file missing 'data' group.");
        int[] shape = getShape(version);
//        OmxHdf5Datatype datatype = null;
//        String baseName = null;
        for (OmxDataset dataset : getBaseGroup().getGroup(OmxConstants.OmxNames.OMX_DATA_GROUP.getKey()).getDatasets()) {
            checkDatasetShapeForConsistency(shape,dataset);
//            OmxHdf5Datatype datasetDatatype = dataset.getDatatype();
//            if (datatype == null) {
//                datatype = datasetDatatype;
//                baseName = dataset.getName();
//            }
//            else if (!datatype.equals(datasetDatatype))
//                throw new IllegalStateException("Inconsistent datatype for datasets: '" +
//                        datatype.getDatatypeName() + "' (" + baseName + ") vs. '" +
//                        datasetDatatype.getDatatypeName() + "' (" + dataset.getName() + ")");
        }
    }

    private void checkDatasetShapeForConsistency(int[] shape, OmxDataset dataset) {
        if (!Arrays.equals(shape,dataset.getShape()))
            throw new IllegalStateException("The shape of dataset '" + dataset.getName() + "' does not match OMX file's specified shape (" + Arrays.toString(shape) + "): " + Arrays.toString(dataset.getShape()));
    }

    private void checkLookupsForConsistency(OmxConstants.OmxVersion version) {
        if (!getBaseGroup().getGroupNames().contains(OmxConstants.OmxNames.OMX_LOOKUP_GROUP.getKey()))
            throw new IllegalStateException("OMX file missing 'lookup' group.");
        for (OmxDataset dataset : getBaseGroup().getGroup(OmxConstants.OmxNames.OMX_LOOKUP_GROUP.getKey()).getDatasets()) {
            int[] lookupShape = dataset.getShape();
            if (lookupShape.length != 1)
                throw new IllegalStateException("OMX lookup '" + dataset.getName() + "' shape is not that of a vector: " + Arrays.toString(lookupShape));
            int[] shape = getShape(version);
            int lookupLength = lookupShape[0];
            if ((lookupLength != shape[0]) && (lookupLength != shape[1]))
                throw new IllegalStateException("OMX lookup '" + dataset.getName() + "' length (" + lookupLength + ") " +
                                                "does not match to either matrix dimension (" + Arrays.toString(shape) + ")");
        }
    }

    private boolean isValidShape(int[] shape) {
        return (shape.length == 2) || (shape[0] > 0) || (shape[1] > 0);
    }

    @Override
    public void close() {
        close(true);
    }

    private void close(boolean withSave) {
        synchronized (opened) {
            if (opened.compareAndSet(true,false)) {
                if (writable.get() && withSave)
                    save(false);
                try {
                    H5.H5Fclose(fileId);
                } catch (HDF5LibraryException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public boolean isOpened() {
        return opened.get();
    }






    private void setupNewFile(int[] shape) {
        try {
            int rootGroup = H5.H5Gopen(fileId,"/",HDF5Constants.H5P_DEFAULT);

            //write omx version
            byte[] version = Hdf5Util.getData(OmxConstants.OmxVersion.VERSION_02.getVersionString());
            int datatype = H5.H5Tcopy(OmxHdf5Datatype.OmxJavaType.STRING.getHdf5NativeId());
            H5.H5Tset_size(datatype,version.length);
            int dataspace = H5.H5Screate_simple(1,new long[] {1},null);
            int attribute = H5.H5Acreate(rootGroup,OmxConstants.OmxNames.OMX_VERSION_KEY.getKey(),datatype,dataspace,HDF5Constants.H5P_DEFAULT,HDF5Constants.H5P_DEFAULT);
            H5.H5Awrite(attribute,datatype,version);
            H5.H5Tclose(datatype);
            H5.H5Sclose(dataspace);
            H5.H5Aclose(attribute);

            //write shape
            datatype = H5.H5Tcopy(OmxHdf5Datatype.OmxJavaType.INT.getHdf5NativeId());
            dataspace = H5.H5Screate_simple(1,new long[]{2},null);
            attribute = H5.H5Acreate(rootGroup,OmxConstants.OmxNames.OMX_SHAPE_KEY.getKey(),datatype,dataspace,HDF5Constants.H5P_DEFAULT,HDF5Constants.H5P_DEFAULT);
            H5.H5Awrite(attribute,datatype,Hdf5Util.getData(shape));
            H5.H5Tclose(datatype);
            H5.H5Sclose(dataspace);
            H5.H5Aclose(attribute);

            //add data group
            int group = H5.H5Gcreate(fileId,"/" + OmxConstants.OmxNames.OMX_DATA_GROUP.getKey(),HDF5Constants.H5P_DEFAULT,HDF5Constants.H5P_DEFAULT,HDF5Constants.H5P_DEFAULT);
            H5.H5Gclose(group);

            //add lookup group
            group = H5.H5Gcreate(fileId,"/" + OmxConstants.OmxNames.OMX_LOOKUP_GROUP.getKey(),HDF5Constants.H5P_DEFAULT,HDF5Constants.H5P_DEFAULT,HDF5Constants.H5P_DEFAULT);
            H5.H5Gclose(group);

            H5.H5Gclose(rootGroup);

        } catch (HDF5Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writeDataset(OmxGroup baseGroup, OmxMutableDataset dataset) {
        boolean newDataset = !baseGroup.hasDataset(dataset.getName());
        if (newDataset || dataset.isMutated()) {
            //first add dataset if it doesn't exist
            int datasetId = -1;
            try {
                if (newDataset) {
                    int datatype = H5.H5Tcopy(dataset.getDatatype().getNativeDatatypeId());
                    int[] shape = dataset.getShape();
                    long[] space = new long[shape.length];
                    for (int i = 0; i < shape.length; i++)
                        space[i] = shape[i];
                    int dataspace = H5.H5Screate_simple(space.length,space,null);
                    
                    //Use a row-chunked, zip-compressed data format
                    int plist = H5.H5Pcreate(HDF5Constants.H5P_DATASET_CREATE);
                    long[] chunksize = new long[2];
                    chunksize[0] = 1;
                    chunksize[1] = shape[0];
                    H5.H5Pset_chunk(plist, space.length, chunksize);
                    H5.H5Pset_deflate(plist, 7);
                    
                    datasetId = H5.H5Dcreate(fileId,dataset.getName(),datatype,dataspace,HDF5Constants.H5P_DEFAULT,plist,HDF5Constants.H5P_DEFAULT);
                    
                    H5.H5Tclose(datatype);
                    H5.H5Sclose(dataspace);
                } else {
                    datasetId = H5.H5Dopen(fileId,dataset.getName(),HDF5Constants.H5P_DEFAULT);
                }
                //next add data
                if (newDataset || dataset.isDataMutated()) {
                	
                	//if matrix 
                	int[] shape = dataset.getShape();
                	if(shape.length>1) {
                	
                		//setup hyperslab i/o
                    	long[] count = new long[2];
                    	long[] offset= new long[2];
                    	count[0] = 1;
                    	count[1] = shape[1];
                    	
                        int memspace = H5.H5Screate_simple(2,count,null);
                    	int dataspace = H5.H5Dget_space(datasetId);
                    	
                    	//write rows
                        for (int i = 0; i < shape[0]; i++) {
                        	
                        	offset[0] = i;
                        	offset[1] = 0;
                        	
                        	//get data container
                        	Object rowdata = null;
                        	OmxJavaType ojt = dataset.getDatatype().getOmxJavaType();
                        	if (ojt.equals(OmxHdf5Datatype.OmxJavaType.INT)) {
                        		rowdata = ((int[][]) dataset.getData())[i];
                        	} else if(ojt.equals(OmxHdf5Datatype.OmxJavaType.SHORT)) {
                        		rowdata = ((short[][]) dataset.getData())[i];
                        	} else if(ojt.equals(OmxHdf5Datatype.OmxJavaType.FLOAT)) {
                        		rowdata = ((float[][]) dataset.getData())[i];
                        	} else if(ojt.equals(OmxHdf5Datatype.OmxJavaType.DOUBLE)) {
                        		rowdata = ((double[][]) dataset.getData())[i];
                        	} else if(ojt.equals(OmxHdf5Datatype.OmxJavaType.BYTE)) {
                        		rowdata = ((byte[][]) dataset.getData())[i];
                        	} else if(ojt.equals(OmxHdf5Datatype.OmxJavaType.STRING)) {
                        		rowdata = ((String[][]) dataset.getData())[i];
                        	}
                        	H5.H5Sselect_hyperslab (dataspace, HDF5Constants.H5S_SELECT_SET, offset, null, count, null);
                        	H5.H5Dwrite(datasetId,dataset.getDatatype().getNativeDatatypeId(),memspace,dataspace,HDF5Constants.H5P_DEFAULT,rowdata);
                        }
                		
                	} else {
                		
                		H5.H5Dwrite(datasetId,dataset.getDatatype().getNativeDatatypeId(),HDF5Constants.H5S_ALL,HDF5Constants.H5S_ALL,HDF5Constants.H5P_DEFAULT,dataset.getData());
                	}
                    
                }
                //next add attributes
                if (newDataset || dataset.areAttributesMutated()) {
                    Hdf5Util.deleteAttributes(datasetId);
                    Map<String,Object> attributes = dataset.getAttributes();
                    for (String attributeName : attributes.keySet())
                        Hdf5Util.writeAttribute(datasetId,attributeName,attributes.get(attributeName));
                }
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
    }

    private void writeNestedGroups(OmxGroup baseGroup, OmxMutableGroup group, OmxGroup originalGroup) {
        String name = group.getName();
        boolean newGroup = !name.equals("/") && !baseGroup.hasGroup(name);
        if (newGroup || group.isMutated()) {
            int groupId = -1;
            try {
                //write group if it exists
                if (newGroup)
                    groupId = H5.H5Gcreate(fileId,name,HDF5Constants.H5P_DEFAULT,HDF5Constants.H5P_DEFAULT,HDF5Constants.H5P_DEFAULT);
                else
                    groupId = H5.H5Gopen(fileId,name,HDF5Constants.H5P_DEFAULT);
                //write nested groups
                for (OmxGroup subGroup : group.getGroups())
                    writeNestedGroups(baseGroup,subGroup.getMutableGroup(),subGroup);
                //write attributes
                if (newGroup || group.areAttributesMutated()) {
                    Hdf5Util.deleteAttributes(groupId);
                    Map<String,Object> attributes = group.getAttributes();
                    for (String attributeName : attributes.keySet())
                        Hdf5Util.writeAttribute(groupId,attributeName,attributes.get(attributeName));
                }
                //write datasets
                for (OmxDataset dataset : group.getDatasets())
                    writeDataset(originalGroup,dataset.getMutableDataset());
                //remove deleted datasets and groups
                if (!newGroup) {
                    for (OmxGroup oldGroup : originalGroup.getGroups()) {
                        if (!group.hasGroup(oldGroup.getName()))
                            H5.H5Ldelete(fileId,oldGroup.getName(),HDF5Constants.H5P_DEFAULT);
                    }
                    for (OmxDataset oldDataset : originalGroup.getDatasets()) {
                        if (!group.hasDataset(oldDataset.getName()))
                            H5.H5Ldelete(fileId,oldDataset.getName(),HDF5Constants.H5P_DEFAULT);
                    }
                }
            } catch (HDF5LibraryException e) {
                throw new RuntimeException(e);
            } finally {
                if (groupId > -1) {
                    try {
                        H5.H5Gclose(groupId);
                    } catch (HDF5LibraryException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    public void save() {
        save(true);
    }

    private void save(boolean checkOpened) {
        if (checkOpened && !opened.get())
            throw new IllegalStateException("Cannot save unopened file: " + filePath);
        if (!writable.get())
            throw new IllegalStateException("Cannot save read-only file: " + filePath);

        OmxGroup rootGroup = getBaseGroup();
        writeNestedGroups(rootGroup,rootGroup.getMutableGroup(),rootGroup);
        reload(checkOpened,false);
    }

    private void reload(boolean checkOpened, boolean withSave) {
        if (checkOpened && !opened.get())
            throw new IllegalStateException("Cannot reload unopened file: " + filePath);
        close(withSave);
        if (checkOpened) {
            if (writable.get())
                openReadWrite();
            else
                openReadOnly();
        }
    }

    public void reload() {
        reload(true,true);
    }
}
