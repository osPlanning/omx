using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using HDF5DotNet;
using CSharpOMX;


namespace CSharpOMXTester
{
    class Tester
    {
        static string testFile = "C:\\omx\\api\\csharp\\test\\testcsharp.omx";
        static string testPythonFile = "C:\\omx\\api\\csharp\\test\\myfile.omx";
        static string testRFile = "C:\\omx\\api\\r\\test.omx";
        static string testRMtx = "PA1";
        static string testMtx = "MtxTest";
        static string testPythonMtx = "m1";

        static void Main(string[] args)
        {
            TestFiles(testFile, testMtx);
           // TestPythonFiles(testPythonFile, testPythonMtx);
           // TestRFile(testRFile, testRMtx);
        }

        public static void TestFiles(string testCSharp, string testMtx)
        {
            long[] shape = {200, 200};

            //make a file and some data
            OmxFile testOmxOutFile = new OmxFile(testCSharp, shape);
            testOmxOutFile.CreateFileOMX();

            UInt16[,] MtxTest = new UInt16[shape[0],shape[1]];

            for (UInt16 i = 0; i < shape[0]; i++)
            {
                for (UInt16 j = 0; j < shape[1]; j++)
                {
                    MtxTest[i,j] = Convert.ToUInt16(i + j);
                }
            }

            H5DataTypeId matrixTypeId = new H5DataTypeId(H5T.H5Type.NATIVE_USHORT);
            OmxMatrix testOmxOutMtx = new OmxMatrix(testOmxOutFile, testMtx, matrixTypeId);
            testOmxOutMtx.FillInt16Matrix(MtxTest);
            testOmxOutFile.Close();

            //now open the file again and read the data back in
            OmxFile testOmxFile = new OmxFile(testCSharp, shape);
            testOmxFile.OpenReadWrite();
       
       
            OmxMatrix testInOmxMtx = new OmxMatrix(testOmxFile, testMtx, matrixTypeId);
            UInt16[,] omxTestRead = testInOmxMtx.GetInt16Matrix();

            int testValue = omxTestRead[4, 4];
            Console.WriteLine("Test Value is {0} ", testValue.ToString());
            Console.WriteLine("The file is OMX version {0}", testOmxFile.OmxVersion);

            int testValue2 = omxTestRead[4, 4];

            if (testValue == testValue2){
              Console.WriteLine(" Value match success");
            }
            else{
              Console.WriteLine(" Value match failure");
            }

            //test mapping the data to a new set of ids

            OmxFile testMapFile = new OmxFile(testCSharp, shape);
            testMapFile.OpenReadWrite();

            int[] testMap = new int[shape[0]];
            for (int i = 0; i < shape[0]; i++)
            {
                testMap[i] = i + 2;
            }

            string mapName = "mapping";
            testMapFile.CreateMapping(testMap, mapName);
            int[] testReadMap = testOmxFile.GetMapping(mapName);
            Console.WriteLine("value for map at 2 is {0}", testReadMap[2]);

            int offsetsize = Convert.ToInt32(shape[0]) +2;
            OmxMatrix testWithMap = new OmxMatrix(testMapFile, testMtx, matrixTypeId);
            UInt16[,] omxTestReadMap =testInOmxMtx.GetInt16MatrixWithMap(testReadMap, offsetsize);

            Console.WriteLine("Value for matrix at 4,4 is {0}", omxTestReadMap[4,4]);

            testMapFile.Close();
        }

        public static void TestPythonFiles(string testPythonFile, string testPythonMtx)
        {
            long[] shape = { 100, 100 };

            OmxFile testOmxFile = new OmxFile(testPythonFile, shape);
            testOmxFile.OpenReadWrite();


            OmxMatrix testOmxMtx = new OmxMatrix(testOmxFile, testPythonMtx);
            double[,] omxTestRead = testOmxMtx.GetDoubleMatrix();

            double testValue = omxTestRead[4, 4];
            Console.WriteLine("Test Value is {0} ", testValue.ToString());
            Console.WriteLine("The file is OMX version {0}", testOmxFile.OmxVersion);
            testOmxFile.Close();
        }

        public static void TestRFile(string testRFile, string testRMtx)
        {
            long[] shape = { 2000, 2000};

            OmxFile testOmxFile = new OmxFile(testRFile, shape);
            testOmxFile.OpenReadWrite();

            // H5DataTypeId matrixTypeId = new H5DataTypeId(H5T.H5Type.NATIVE_FLOAT);

            OmxMatrix testOmxMtx = new OmxMatrix(testOmxFile, testRMtx);
            double[,] omxTestRead = testOmxMtx.GetDoubleMatrix();

            double testValue = omxTestRead[4, 4];
            Console.WriteLine("Test Value is {0} ", testValue.ToString());
            Console.WriteLine("The file is OMX version {0}", testOmxFile.OmxVersion);
            testOmxFile.Close();
        }

    }
}

