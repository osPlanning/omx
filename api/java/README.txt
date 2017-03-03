Open Matrix Java library
See javadoc for basic functionality (/javadoc/index.html)
Run main method in omx.OmxFile for a simple example. The binary libraries
 in /lib/ are required to run the model, and must be present on the java
 library path. For example, on a 64-bit window platform:

    java -classpath omx.jar -Djava.library.path=./lib/win64/ omx.OmxFile

Built with HD5 2.8 - http://www.hdfgroup.org/ftp/HDF5/releases/HDF-JAVA/HDF-JAVA-2.8/bin/

This is the code for the main method in omx.OmxFile:

  public static void main(String ... args) {
        
    	Random r = new Random();
        String f = "example.omx";
        try (OmxFile omxFile = new OmxFile(f)) {
            
        	int dim0 = 2000;
        	
            int dim1 = dim0;
            int[] shape = {dim0,dim1};

            double mat1NA = -1;
            double[][] mat1Data = new double[dim0][dim1];
            for (int i = 0; i < dim0; i++)
                for (int j = 0; j < dim1; j++)
                    mat1Data[i][j] = i+j;
            OmxMatrix.OmxDoubleMatrix mat1 = new OmxMatrix.OmxDoubleMatrix("mat1",mat1Data,mat1NA);
            mat1.setAttribute(OmxConstants.OmxNames.OMX_DATASET_TITLE_KEY.getKey(),"an int matrix");

            int mat2NA = -99999;
            int[][] mat2Data = new int[dim0][dim1];
            for (int i = 0; i < dim0; i++)
                for (int j = 0; j < dim1; j++)
                    mat2Data[i][j] = r.nextInt(100);
            OmxMatrix.OmxIntMatrix mat2 = new OmxMatrix.OmxIntMatrix("mat2",mat2Data,mat2NA);

            int lookup1NA = -1;
            int[] lookup1Data = new int[dim0];
            Set<Integer> lookup1Used = new HashSet<>();
            for (int i = 0; i < lookup1Data.length; i++) {
                int lookup = i+1;
                lookup1Data[i] = lookup1Used.add(lookup) ? lookup : lookup1NA;
            }
            OmxLookup.OmxIntLookup lookup1 = new OmxLookup.OmxIntLookup("lookup1",lookup1Data,lookup1NA);

            double lookup2NA = -99999;
            double[] lookup2Data = new double[dim1];
            Set<Double> lookup2Used = new HashSet<>();
            for (int i = 0; i < lookup2Data.length; i++) {
            	double lookup = i+1;
                lookup2Data[i] = lookup2Used.add(lookup) ? lookup : lookup2NA;
            }
            OmxLookup.OmxDoubleLookup lookup2 = new OmxLookup.OmxDoubleLookup("lookup2",lookup2Data,lookup2NA);

            omxFile.openNew(shape);
            omxFile.addMatrix(mat1);
            omxFile.addLookup(lookup1);
            omxFile.addMatrix(mat2);
            omxFile.addLookup(lookup2);
            omxFile.save();
            System.out.println(omxFile.summary());
            
        }

        try (OmxFile omxFile = new OmxFile(f)) {
            omxFile.openReadWrite();
            System.out.println(omxFile.summary());
            omxFile.deleteMatrix("mat1");
            omxFile.deleteLookup("lookup1");
            System.out.println(omxFile.summary());
        }
        
    }
