using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Threading.Tasks;
using HDF5DotNet;
using CSharpOMX;


namespace CSharpOMXTester
{
    class Tester
    {
        static string testCreateFile = ".\\testcsharp.omx";
        static string testRFile = ".\\test.omx";

        static void Main(string[] args)
        {
            string testMtx = "tempMat";
            CreateMatrixTest(testCreateFile);
            ReadTest(testCreateFile);
            //ReadTest(testRFile); //- FAILS - cannot read R file attributes
            ReadWriteTablesTest(testCreateFile, testMtx);
            ReadWriteRowTest(testCreateFile, testMtx);
            MappingTest(testCreateFile, testMtx);
            Console.ReadLine();
        }


        // test that we can create an empty file and add matrices
        public static void CreateMatrixTest(string file)
        {
            int zones = 3;
            double[,] testblock;
            string[] matrixNames = { "mat1", "mat2" };
            OmxWriteStream ws = OmxFile.Create(file, zones, true);

            // NOTE: cannot create data type until after file stream is created
            H5DataTypeId matrixDataTypes = H5T.copy(H5T.H5Type.NATIVE_DOUBLE);
            for (int i = 0; i < matrixNames.Length; i++)
                ws.AddMatrix(matrixNames[i], matrixDataTypes);

            ws.Close();

            OmxReadStream rs = OmxFile.OpenReadOnly(file);

            for (int i = 0; i < matrixNames.Length; i++)
                testblock = rs.GetMatrixBlock<double>(matrixNames[i], 0, 0, 1, 1);

            Console.WriteLine("mat shape is {0},{1}", rs.Shape[0], rs.Shape[1]);
            Console.WriteLine("mat names are {0},{1}", rs.MatrixNames[0], rs.MatrixNames[1]);
            Console.WriteLine("mat data type: {0},{1}",
                H5T.getClass(rs.GetMatrixDataType(matrixNames[0])),
                H5T.getClass(rs.GetMatrixDataType(matrixNames[1])));
            rs.Close();

        }

        // Test that we can read an OMX file with attributes and tables
        public static void ReadTest(string file)
        {
            OmxReadStream rs = OmxFile.OpenReadOnly(file);
            Console.WriteLine("OMX version is {0}", rs.OmxVersion);
            Console.WriteLine("mat shape is {0},{1}", rs.Shape[0], rs.Shape[1]);
            Console.WriteLine("first mat names is {0}", rs.MatrixNames[0]);
            Console.WriteLine("first mat data type is {0}",H5T.getClass(rs.GetMatrixDataType(rs.MatrixNames[0])));
            rs.Close();
        }

        // test that we can read/write with a created matrix tables
        public static void ReadWriteTablesTest(string testCSharp, string testMtx)
        {
            int zones = 200;

            //make a file and some data
            OmxWriteStream ws = OmxFile.Create(testCSharp, zones, true);

            double[,] MtxTest = new double[zones, zones];

            for (int i = 0; i < zones; i++)
            {
                for (int j = 0; j < zones; j++)
                {
                    MtxTest[i, j] = Convert.ToDouble(i + j);
                }
            }

            H5DataTypeId matrixTypeId = H5T.copy(H5T.H5Type.NATIVE_DOUBLE);
            ws.AddMatrix(testMtx, matrixTypeId);
            ws.SetMatrix<double>(testMtx, MtxTest);
            ws.Close();

            //now open the file again and read the data back in
            OmxReadStream rs = OmxFile.OpenReadOnly(testCSharp);
            double[,] omxTestRead = rs.GetMatrix<double>(testMtx);

            double testValue = MtxTest[4, 4];
            double testValue2 = omxTestRead[4, 4];

            if (testValue == testValue2)
            {
                Console.WriteLine(" Value match success");
            }
            else
            {
                Console.WriteLine(" Value match failure");
            }
            rs.Close();
        }

        // test read/write row function
        public static void ReadWriteRowTest(string file, string matName)
        {
            OmxWriteStream ws = OmxFile.OpenReadWrite(file);
            Console.WriteLine("mat shape is {0},{1}", ws.Shape[0], ws.Shape[1]);

            H5DataTypeId dt =  ws.GetMatrixDataType(matName);

            // blocks we are reading need to match the data type of the matrix
            var rowData = new double[ws.Shape[0]];
            var rowData2 = new double[ws.Shape[0]];

            rowData = ws.GetMatrixRow<double>(matName, 2);

            rowData[2] = rowData[2] + 1.0;
            ws.SetMatrixRow<double>(matName, 2, rowData);
            rowData2 = ws.GetMatrixRow<double>(matName, 2);

            if (rowData.Sum() == rowData2.Sum())
                Console.WriteLine("Row Read/Write successful");
            else
                Console.WriteLine("Read/Write mismatch");
            ws.Close();
        }

        // test that we can store and retrieve index maps
        public static void MappingTest(string testCSharp, string testMtx)
        {
            //test mapping the data to a new set of ids
            OmxWriteStream ws = OmxFile.OpenReadWrite(testCSharp);

            int zones = (int)ws.Shape[0];
            int[] testMap = new int[zones];
            for (int i = 0; i < zones; i++)
            {
                testMap[i] = i + 2;
            }

            string mapName = "mapping";
            H5DataTypeId mapData = H5T.copy(H5T.H5Type.NATIVE_INT);

            // create map
            ws.CreateMapping<int>(testMap, mapData, mapName);

            // get map data type
            H5DataTypeId returnedData = ws.GetMappingDataType(mapName);

            if(mapData.GetType().Equals(returnedData.GetType()))
                Console.WriteLine("Mapping data match");
            else
                Console.WriteLine("Mapping data MISMATCH");

            // read map and use it to read data from matrix
            int[] testReadMap = ws.GetMapping<int>(mapName);
            Console.WriteLine("value for map at 2 is {0}", testReadMap[2]);

            int offsetsize = Convert.ToInt32(zones) - 2;
            double[,] omxTestReadMap = ws.GetMatrixBlock<double>(testMtx, testReadMap[0], testReadMap[0], offsetsize, offsetsize);
                      
            Console.WriteLine("Value for matrix at 4,4 is {0}", omxTestReadMap[4,4]);

            ws.Close();
        }
    }
}

