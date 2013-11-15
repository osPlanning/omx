Open Matrix Java library
See javadoc for basic functionality (/javadoc/index.html)
Run main method in omx.OmxFile for a simple example. The binary libraries
 in /lib/ are required to run the model, and must be present on the java
 library path. For example, on a 64-bit window platform:

    java -classpath omx.jar -Djava.library.path=./lib/win64/ omx.OmxFile

This is the code for the main method in omx.OmxFile:

    public static void main(String ... args) {
        Random r = new Random();
        String f = "example.omx";
        try (OmxFile omxFile = new OmxFile(f)) {
            int dim0 = 43;
            int dim1 = 67;
            int[] shape = {dim0,dim1};

            int mat1NA = -1;
            int[][] mat1Data = new int[dim0][dim1];
            for (int i = 0; i < dim0; i++)
                for (int j = 0; j < dim1; j++)
                    mat1Data[i][j] = r.nextInt(100) < 2 ? mat1NA : r.nextInt(1000);
            OmxMatrix.OmxIntMatrix mat1 = new OmxMatrix.OmxIntMatrix("mat1",mat1Data,mat1NA);
            mat1.setAttribute(OmxConstants.OmxNames.OMX_DATASET_TITLE_KEY.getKey(),"an int matrix");

            double mat2NA = Double.NaN;
            double[][] mat2Data = new double[dim0][dim1];
            for (int i = 0; i < dim0; i++)
                for (int j = 0; j < dim1; j++)
                    mat2Data[i][j] = r.nextInt(100) < 2 ? mat2NA : r.nextDouble()*1000;
            OmxMatrix.OmxDoubleMatrix mat2 = new OmxMatrix.OmxDoubleMatrix("mat2",mat2Data,mat2NA);

            int lookup1NA = -1;
            int[] lookup1Data = new int[dim0];
            Set<Integer> lookup1Used = new HashSet<>();
            for (int i = 0; i < lookup1Data.length; i++) {
                int lookup = r.nextInt(100);
                lookup1Data[i] = lookup1Used.add(lookup) ? lookup : lookup1NA;
            }
            OmxLookup.OmxIntLookup lookup1 = new OmxLookup.OmxIntLookup("lookup1",lookup1Data,lookup1NA);

            float lookup2NA = -1;
            float[] lookup2Data = new float[dim1];
            Set<Float> lookup2Used = new HashSet<>();
            for (int i = 0; i < lookup2Data.length; i++) {
                float lookup = r.nextInt(100);
                lookup2Data[i] = lookup2Used.add(lookup) ? lookup : lookup2NA;
            }
            OmxLookup.OmxFloatLookup lookup2 = new OmxLookup.OmxFloatLookup("lookup2",lookup2Data,lookup2NA);


            omxFile.openNew(shape);
            omxFile.addMatrix(mat1);
            omxFile.addMatrix(mat2);
            omxFile.addLookup(lookup1);
            omxFile.save();
            omxFile.addLookup(lookup2);
            omxFile.save();
            System.out.println(omxFile.summary());
            System.out.println(mat1.getName());
            omxFile.deleteMatrix(mat1.getName());
            omxFile.deleteLookup(lookup1.getName());
            omxFile.deleteLookup(lookup2.getName());
        }

        System.out.println("repack");
        OmxFile.repack(f);

        try (OmxFile omxFile = new OmxFile(f)) {
            omxFile.openReadOnly();
            System.out.println(omxFile.summary());
        }
    }
