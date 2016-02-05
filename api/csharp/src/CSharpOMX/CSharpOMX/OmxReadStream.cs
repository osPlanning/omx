using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Threading.Tasks;
using HDF5DotNet;

namespace CSharpOMX
{
    public class OmxReadStream : OmxBase
    {
        public OmxReadStream(string file)
        {
            this.filepath = file;
            this.IsValid = false;
            this.Shape = new long[] { 0, 0 };
        }

        public H5FileId OpenReadOnly()
        {
            return open(H5F.OpenMode.ACC_RDONLY);
        }

        protected H5FileId open(H5F.OpenMode mode)
        {
            this.fileId = H5F.open(filepath, mode);
            this.IsValid = this.getOMXFileAttributes();
            this.IsValid &= this.getOMXMatrixTables();
            this.IsValid &= this.getOMXIndexMaps();

            return fileId;
        }

        private bool getOMXFileAttributes()
        {
            string version = "unknown";

            // OMX Version
            H5AttributeId verId = H5A.open(fileId, omxVersionName);
            H5T.H5TClass verCl = H5T.getClass(H5A.getType(verId));

            if (verCl.Equals(H5T.H5TClass.STRING))
            {
                Byte[] buff = getAttribute<byte>(verId);
                ASCIIEncoding enc = new ASCIIEncoding();
                version = enc.GetString(buff);
            }
            else if (verCl.Equals(H5T.H5TClass.FLOAT))
            {
                float[] buff = getAttribute<float>(verId);
                version = buff.ToString();
                // produces garbage - bail out from here...
                throw new NotImplementedException();
            }
            else
            {
                throw new NotImplementedException();
            }
            //Console.WriteLine("omx version: {0}", version);
            this.OmxVersion = version;
            H5A.close(verId);

            // matrix shape
            H5AttributeId shId = H5A.open(fileId, omxShapeAttr);
            H5T.H5TClass shCl = H5T.getClass(H5A.getType(shId));

            if (shCl.Equals(H5T.H5TClass.INTEGER))
            {
                int[] shape = getAttribute<int>(shId);

                this.Shape[0] = (long)shape[0];
                this.Shape[1] = (long)shape[1];
            }
            else if (shCl.Equals(H5T.H5TClass.FLOAT))
            {
                float[] shape = getAttribute<float>(shId);

                this.Shape[0] = (long)shape[0];
                this.Shape[1] = (long)shape[1];

                // returns garbage, bail out from here
                throw new NotImplementedException();
            }
            else throw new NotImplementedException();

            H5A.close(shId);
            return (true);
        }

        private bool getOMXMatrixTables()
        {
            // get info about the tables
            this.dataGroup = H5G.open(fileId, dataGroupName);
            this.NumMatrix = (int)H5G.getNumObjects(this.dataGroup);
            this.MatrixNames = new List<string>();

            for (int i = 0; i < NumMatrix; i++)
            {
                string matName = H5G.getObjectNameByIndex(this.dataGroup, (ulong)i);
                MatrixNames.Add(matName);
                H5DataSetId matId = H5D.open(dataGroup, matName);
                tables.Add(matName, matId);
            }
            return (true);
        }

        private bool getOMXIndexMaps()
        {
            this.luGroup = H5G.open(fileId, luGroupName);
            this.NumIndexMap = (int)H5G.getNumObjects(this.luGroup);
            this.IndexMapNames = new List<string>();

            for (int i = 0; i < NumIndexMap; i++)
            {
                string imName = H5G.getObjectNameByIndex(this.luGroup, (ulong)i);
                IndexMapNames.Add(imName);
                H5DataSetId imId = H5D.open(luGroup, imName);
                indexMaps.Add(imName, imId);
            }

            return (true);
        }


        #region IndexMaps
        /// <summary>
        /// Returns mapping index - assumes that matrices are square and uses first dimension as the map size
        /// </summary>
        /// <typeparam name="T"></typeparam>
        /// <param name="mapName">Name of index map</param>
        public T[] GetMapping<T>(string mapName)
        {
            var mapData = new T[Shape[0]];

            // check that index map exists
            if (indexMaps.ContainsKey(mapName))
            {
                H5DataSetId mapId;
                indexMaps.TryGetValue(mapName, out mapId);
                H5D.read(mapId, H5D.getType(mapId), new H5Array<T>(mapData));
            }
            else
            {
                Console.WriteLine("index map {0} not found in file", mapName);
            }
            return mapData;
        }

        /// <summary>
        /// Returns the data types of the index map
        /// </summary>
        /// <param name="mapName"></param>
        /// <returns></returns>
        public H5DataTypeId GetMappingDataType(string mapName)
        {
            H5DataTypeId mapDataId = null;

            // check that index map exists
            if (indexMaps.ContainsKey(mapName))
            {
                H5DataSetId mapId;
                indexMaps.TryGetValue(mapName, out mapId);
                mapDataId = H5D.getType(mapId);
            }
            else
            {
                Console.WriteLine("index map {0} not found in file", mapName);
            }
            return mapDataId;
        }


        #endregion

        #region matrixTables
        // Matrix Specific Methods
        // TODO: 
        // 1. add handling for matrix title
        // 2. add specification of NA values
        // 3. other attributes: pa-format flag, year int, source string

        /// <summary>
        /// Returns a row of the matrix
        /// </summary>
        /// <typeparam name="T"></typeparam>
        /// <param name="matName"></param>
        /// <param name="rowIndex"></param>
        /// <returns></returns>
        public T[] GetMatrixRow<T>(string matName, int rowIndex)
        {
            var rowData = new T[Shape[1]];

            // check that matrix exists
            if (tables.ContainsKey(matName))
            {
                H5DataSetId matId;
                tables.TryGetValue(matName, out matId);

                H5DataTypeId matDataId = H5D.getType(matId);
                H5DataSpaceId spaceId = H5S.create_simple(2, Shape);

                var h5matrix = new H5Array<T>(rowData);

                long[] start = { rowIndex, 0 };
                long[] count = { 1, Shape[1] };
                H5S.selectHyperslab(spaceId, H5S.SelectOperator.SET, start, count);
                H5DataSpaceId readSpaceId = H5S.create_simple(2, count);

                H5D.read(matId, matDataId, readSpaceId, spaceId, H5P.create(H5P.PropertyListClass.DATASET_XFER), h5matrix);
                H5S.close(spaceId);
                H5S.close(readSpaceId);
            }
            else
            {
                Console.WriteLine("table {0} not found in matrix file", matName);
            }
            return rowData;
        }

        /// <summary>
        /// Method to read an individual cell or block of data from a matrix
        /// rowIndex and colIndex are the zero-based offsets into the matrix
        /// rowLength and colLength are the size of the arrays (min 1)
        /// </summary>
        /// <typeparam name="T"></typeparam>
        /// <param name="matName"></param>
        /// <param name="rowIndex"></param>
        /// <param name="colIndex"></param>
        /// <param name="rowLength"></param>
        /// <param name="colLength"></param>
        /// <returns></returns>
        public T[,] GetMatrixBlock<T>(string matName, int rowIndex, int colIndex, int rowLength, int colLength)
        {
            // check that block parameters are legit
            if ((rowLength < 1 | colLength < 1) | (rowIndex + rowLength > Shape[0] | colIndex + colLength > Shape[1]))
            {
                Console.WriteLine("invalid block size and/or index, must be non-zero in both dimensions and within matrix size");
                return null;
            }
            var blockData = new T[rowLength, colLength];

            // check that matrix exists
            if (tables.ContainsKey(matName))
            {
                H5DataSetId matId;
                tables.TryGetValue(matName, out matId);

                H5DataTypeId matDataId = H5D.getType(matId);
                H5DataSpaceId spaceId = H5S.create_simple(2, Shape);

                var h5matrix = new H5Array<T>(blockData);

                long[] start = { rowIndex, colIndex };
                long[] count = { rowLength, colLength };
                H5S.selectHyperslab(spaceId, H5S.SelectOperator.SET, start, count);
                H5DataSpaceId readSpaceId = H5S.create_simple(2, count);

                H5D.read(matId, matDataId, readSpaceId, spaceId, H5P.create(H5P.PropertyListClass.DATASET_XFER), h5matrix);
                H5S.close(spaceId);
                H5S.close(readSpaceId);
            }
            else
            {
                Console.WriteLine("table {0} not found in matrix file", matName);
            }

            return blockData;
        }

        /// <summary>
        /// Returns the entire matrix table
        /// </summary>
        /// <typeparam name="T"></typeparam>
        /// <param name="matName"></param>
        /// <param name="rowIndex"></param>
        /// <returns></returns>
        public T[,] GetMatrix<T>(string matName)
        {
            var data = new T[Shape[0], Shape[1]];

            // check that matrix exists
            if (tables.ContainsKey(matName))
            {
                H5DataSetId matId;
                tables.TryGetValue(matName, out matId);

                H5DataTypeId matDataId = H5D.getType(matId);
                H5DataSpaceId spaceId = H5S.create_simple(2, Shape);

                var h5matrix = new H5Array<T>(data);

                long[] start = { 0, 0 };
                long[] count = { Shape[0], Shape[1] };
                H5S.selectHyperslab(spaceId, H5S.SelectOperator.SET, start, count);
                H5DataSpaceId readSpaceId = H5S.create_simple(2, count);

                H5D.read(matId, matDataId, readSpaceId, spaceId, H5P.create(H5P.PropertyListClass.DATASET_XFER), h5matrix);
                H5S.close(spaceId);
                H5S.close(readSpaceId);
            }
            else
            {
                Console.WriteLine("table {0} not found in matrix file", matName);
            }
            return data;
        }

        /// <summary>
        /// Returns the data types of the matrix
        /// </summary>
        /// <param name="mapName"></param>
        /// <returns></returns>
        public H5DataTypeId GetMatrixDataType(string matName)
        {
            H5DataTypeId matDataId = null;

            // check that index map exists
            if (tables.ContainsKey(matName))
            {
                H5DataSetId matId;
                tables.TryGetValue(matName, out matId);
                matDataId = H5D.getType(matId);
            }
            else
            {
                Console.WriteLine("table {0} not found in file", matName);
            }
            return matDataId;
        }


        #endregion
    }
}
