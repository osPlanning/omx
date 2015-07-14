package omx;

/**
 * Test basic OMX Matrix Reader and writer
 * java -classpath "omx.jar" -Djava.library.path="jhdfdllFolder" omx.OMXTest
 * requires omx.jar in classpath and jhdf5.dll,jhdf.dll in the java.library.path
 * @author    Ben Stabler
 * @version   1.0, 02/11/15
 */
public class OMXTest {

	private OMXTest() {}
	
	public static void main(String[] args) {
		
		//write 
		OmxFile omxfile = new OmxFile("test.omx");
		int[] shape = new int[2];
		shape[0] = 5;
		shape[1] = 5;	
		omxfile.openNew(shape);
		
		//create matrix
		double[][] valuesDouble = new double[shape[0]][shape[1]];
		for (int i = 0 ; i < valuesDouble.length; i++) {
			for (int j = 0 ; j < valuesDouble[0].length; j++) {
				valuesDouble[i][j] = i*j;
			}
		}
		
		//add matrix
		OmxMatrix.OmxDoubleMatrix mat = new OmxMatrix.OmxDoubleMatrix("test", valuesDouble, 99999.0);
		omxfile.addMatrix(mat);
		
		//add zone names
		int[] zoneNames = {100,101,102,103,104};
		OmxLookup.OmxIntLookup omxZoneNums = new OmxLookup.OmxIntLookup("NO", zoneNames, 0);
		omxfile.addLookup(omxZoneNums);
		
		omxfile.save();
		
		//read matrix
		omxfile = new OmxFile("test.omx");
		omxfile.openReadOnly();
		OmxMatrix.OmxDoubleMatrix omxMat = (OmxMatrix.OmxDoubleMatrix)omxfile.getMatrix("test");

		double[][] values = omxMat.getData();
		
		for (int i = 0 ; i < values.length; i++) {
			for (int j = 0 ; j < values[0].length; j++) {
				System.out.println(values[i][j]);
			}
		}
		
		//read zone names
		OmxLookup.OmxIntLookup zoneLabels = (OmxLookup.OmxIntLookup)omxfile.getLookup("NO");
		for (int j = 0 ; j < zoneLabels.getLength(); j++) {
			System.out.println(zoneLabels.getLookup()[j]);
		}
		
	}

}
