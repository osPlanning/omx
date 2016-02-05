using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Threading.Tasks;
using HDF5DotNet;

namespace CSharpOMX
{
    public class OmxWriteStream : OmxReadStream
    {
        public OmxWriteStream(string file)
            : base(file)
        {
            this.filepath = file;
            this.IsValid = false;
            this.Shape = new long[] { 0, 0 };
        }

        public H5FileId OpenReadWrite()
        {
            return open(H5F.OpenMode.ACC_RDWR);
        }
        
        private bool setOMXFileContents(H5DataTypeId[] matDataTypes)
        {
            bool valid = false;
            valid = setOMXFileAttributes();
            valid &= setOMXFileMatrixTables();
            valid &= setOMXFileIndexMaps();

            return valid;
        }

        private bool setOMXFileAttributes()
        {
            // write OMX attributes
            H5DataSpaceId dspace;
            H5DataTypeId dtype;
            H5AttributeId attr;

            // OMX version
            dspace = H5S.create(H5S.H5SClass.SCALAR);
            dtype = H5T.copy(H5T.H5Type.C_S1); // string datatype
            H5T.setSize(dtype, dllVersion[0].Length);
            attr = H5A.create(fileId, omxVersionName, dtype, dspace);
            ASCIIEncoding ascii = new ASCIIEncoding();
            H5A.write(attr, dtype, new H5Array<System.Byte>(ascii.GetBytes(dllVersion[0])));
            H5A.close(attr);

            // OMX shape - only 2D tables
            dspace = H5S.create_simple(1, new long[] { 2 });
            dtype = H5T.copy(H5T.H5Type.NATIVE_INT);
            attr = H5A.create(fileId, omxShapeAttr, dtype, dspace);
            int[] shape = new int[2];
            shape[0] = (int)Shape[0];
            shape[1] = (int)Shape[1];
            H5A.write<int>(attr, dtype, new H5Array<int>(shape));
            H5S.close(dspace);
            H5A.close(attr);

            return (true);
        }
        
        private bool setOMXFileMatrixTables()
        {
            this.dataGroup = H5G.create(fileId, dataGroupName);
            this.NumMatrix = 0;
            return (true);
        }

        private bool setOMXFileIndexMaps()
        {
            this.luGroup = H5G.create(fileId, luGroupName);
            this.NumIndexMap = 0;

            return (true);
        }

        private void overwriteCheck(bool forceOverwrite)
        {

            if (File.Exists(filepath))
            {
                if (forceOverwrite)
                {
                    File.Delete(filepath);
                }
                else
                {
                    Console.WriteLine("Tried to create file {0} that already exists.  Do you wish to overwrite y/n?");
                    string overwrite = Console.ReadLine();
                    if (overwrite.Equals("y"))
                    {
                        File.Delete(filepath);
                        Console.WriteLine("Deleted file {0}, now recreating.", filepath);
                    }
                    else
                    {
                        Console.WriteLine("You choose not to overwrite the file");
                        return;
                    }
                }
            }
        }

        // TODO: add create with both dimensions defined in shape.        
        /// <summary>
        /// Create OMX file with no matrix tables
        /// </summary>
        /// <param name="zones">Number of zones - matrix tables will all be square</param>
        /// <param name="overwrite">true to automatically overwrite file on disc, false will prompt user to overwrite</param>
        public void CreateFileOMX(int zones, bool overwrite)
        {
            overwriteCheck(overwrite);

            // create and open are two seperate hanldes, need to close create after finished
            H5FileId createFile = H5F.create(filepath, H5F.CreateMode.ACC_EXCL);
            H5F.close(createFile);

            fileId = H5F.open(filepath, H5F.OpenMode.ACC_RDWR);

            this.Shape = new long[] { zones, zones };
            H5DataTypeId[] matDataTypes = null;

            this.IsValid = setOMXFileContents(matDataTypes);
        }

        #region IndexMaps
        /// <summary>
        /// Create mapping index - assumes that matrices are square and uses first dimension as the map size
        /// </summary>
        /// <typeparam name="T"></typeparam>
        /// <param name="tazEquiv">array the size of the matrix table dimension</param>
        /// <param name="mapDataType">Data type of array</param>
        /// <param name="mapName">Name of index map</param>
        public void CreateMapping<T>(T[] tazEquiv, H5DataTypeId mapDataType, string mapName)
        {
            long[] oneDShape = { Shape[0] };
            H5DataSpaceId mapSpaceId = H5S.create_simple(1, oneDShape);

            H5DataSetId newMappingID = H5D.create(luGroup, mapName, mapDataType, mapSpaceId);
            H5D.write(newMappingID, mapDataType, new H5Array<T>(tazEquiv));

            this.indexMaps.Add(mapName, newMappingID);
        }

        #endregion

        #region matrixTables
        // Matrix Specific Methods
        // TODO: 
        // 1. add handling for matrix title
        // 2. add specification of NA values
        // 3. other attributes: pa-format flag, year int, source string

        /// <summary>
        /// Add a matrix table to an opened OMX file
        /// </summary>
        /// <param name="matName"></param>
        /// <param name="matDataType"></param>
        /// <returns></returns>
        public int AddMatrix(string matName, H5DataTypeId matDataType)
        {
            int status = -1;

            // check that matrix doesn't already exist
            if (!tables.ContainsKey(matName))
            {
                H5DataSpaceId matSpace = H5S.create_simple(2, Shape);
                H5DataSetId matId = H5D.create(dataGroup, matName, matDataType, matSpace);
                tables.Add(matName, matId);
                H5S.close(matSpace);
                status = 0;
            }
            else
            {
                this.ErrMessage = string.Format("matrix {0} already exists", matName);
            }
            return status;
        }

        /// <summary>
        /// Writes a row of the matrix
        /// </summary>
        /// <typeparam name="T"></typeparam>
        /// <param name="matName"></param>
        /// <param name="rowIndex"></param>
        /// <returns></returns>
        public void SetMatrixRow<T>(string matName, int rowIndex, T[] rowData)
        {
            // check that matrix exists
            if (tables.ContainsKey(matName))
            {
                H5DataSetId matId;
                tables.TryGetValue(matName, out matId);

                H5DataTypeId matDataId = H5D.getType(matId);
                H5DataSpaceId spaceId = H5S.create_simple(2, Shape);

                long[] start = { rowIndex, 0 };
                long[] count = { 1, Shape[1] };
                var h5matrix = new H5Array<T>(rowData);

                H5S.selectHyperslab(spaceId, H5S.SelectOperator.SET, start, count);
                H5DataSpaceId readSpaceId = H5S.create_simple(2, count);

                H5D.write(matId, matDataId, readSpaceId, spaceId, H5P.create(H5P.PropertyListClass.DATASET_XFER), h5matrix);
                H5S.close(spaceId);
                H5S.close(readSpaceId);
            }
            else
            {
                Console.WriteLine("table {0} not found in matrix file", matName);
            }
            return;
        }

        /// <summary>
        /// Method to write an individual cell or block of data from a matrix
        /// rowIndex and colIndex are the zero-based offsets into the matrix
        /// rowLength and colLength are the size of the arrays (min 1)
        /// </summary>
        /// <typeparam name="T"></typeparam>
        /// <param name="matName"></param>
        /// <param name="rowIndex"></param>
        /// <param name="colIndex"></param>
        /// <param name="rowLength"></param>
        /// <param name="colLength"></param>
        /// <param name="blockData"></param>
        public void SetMatrixBlock<T>(string matName, int rowIndex, int colIndex, int rowLength, int colLength, T[,] blockData)
        {
            // check that block parameters are legit
            if ((rowLength < 1 | colLength < 1) | (rowIndex + rowLength > Shape[0] | colIndex + colLength > Shape[1]))
            {
                Console.WriteLine("invalid block size and/or index, must be non-zero in both dimensions and within matrix size");
                return;
            }

            // check that matrix exists
            if (tables.ContainsKey(matName))
            {
                H5DataSetId matId;
                tables.TryGetValue(matName, out matId);

                H5DataTypeId matDataId = H5D.getType(matId);
                H5DataSpaceId spaceId = H5S.create_simple(2, Shape);

                long[] start = { rowIndex, colIndex };
                long[] count = { rowLength, colLength };
                var h5matrix = new H5Array<T>(blockData);

                H5S.selectHyperslab(spaceId, H5S.SelectOperator.SET, start, count);
                H5DataSpaceId readSpaceId = H5S.create_simple(2, count);

                H5D.write(matId, matDataId, readSpaceId, spaceId, H5P.create(H5P.PropertyListClass.DATASET_XFER), h5matrix);
                H5S.close(spaceId);
                H5S.close(readSpaceId);
            }
            else
            {
                Console.WriteLine("table {0} not found in matrix file", matName);
            }

            return;
        }

        /// <summary>
        /// Writes the entire matrix table
        /// </summary>
        /// <typeparam name="T"></typeparam>
        /// <param name="matName"></param>
        /// <param name="rowIndex"></param>
        /// <returns></returns>
        public void SetMatrix<T>(string matName, T[,] data)
        {
            // check that matrix exists
            if (tables.ContainsKey(matName))
            {
                H5DataSetId matId;
                tables.TryGetValue(matName, out matId);

                H5DataTypeId matDataId = H5D.getType(matId);
                H5DataSpaceId spaceId = H5S.create_simple(2, Shape);

                long[] start = { 0, 0 };
                long[] count = { Shape[0], Shape[1] };
                var h5matrix = new H5Array<T>(data);

                H5S.selectHyperslab(spaceId, H5S.SelectOperator.SET, start, count);
                H5DataSpaceId readSpaceId = H5S.create_simple(2, count);

                H5D.write(matId, matDataId, readSpaceId, spaceId, H5P.create(H5P.PropertyListClass.DATASET_XFER), h5matrix);
                H5S.close(spaceId);
                H5S.close(readSpaceId);
            }
            else
            {
                Console.WriteLine("table {0} not found in matrix file", matName);
            }
            return;
        }
        
        #endregion

    }
}
