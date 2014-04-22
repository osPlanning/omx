using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using HDF5DotNet;

namespace CSharpOMX
{
    public class OmxMatrix
    {
        private long[] shape;
        private H5FileId fileId;
        private H5DataSetId matrixId;
        private H5DataTypeId matrixTypeId;
        private H5DataSpaceId matrixSpaceId;
        private string groupName;
        private H5GroupId groupId;
        private string matrixName;

        public OmxMatrix(OmxFile omxfile, string matrixName, H5DataTypeId matrixTypeId)
        {
            this.fileId = omxfile.FileID;
            this.shape = omxfile.Shape;
            this.groupName = omxfile.RootGroupName;
            this.matrixName = matrixName;
            this.matrixTypeId = matrixTypeId;
            groupId = H5G.open(fileId, groupName);
            matrixSpaceId = H5S.create_simple(2, shape);
            matrixId = CreateMatrixIfNoneExists(groupId, matrixName, shape, matrixTypeId, matrixSpaceId);
        }

        public OmxMatrix(OmxFile omxfile, string matrixName)
        {
            this.fileId = omxfile.FileID;
            this.shape = omxfile.Shape;
            this.groupName = omxfile.RootGroupName;
            this.matrixName = matrixName;
            groupId = H5G.open(fileId, groupName);
            matrixId = H5D.open(groupId, matrixName);
            this.matrixTypeId = H5D.getType(matrixId);
            this.matrixSpaceId = H5D.getSpace(matrixId);
        }

        public H5DataTypeId MatrixTypeId
        {
            get
            {
                return matrixTypeId;
            }

        }

        public UInt16[,] GetInt16Matrix()
        {
            matrixId = H5D.open(groupId, matrixName);
            matrixSpaceId = H5D.getSpace(matrixId);
            shape = H5S.getSimpleExtentDims(matrixSpaceId);
            matrixTypeId = H5D.getType(matrixId);
            var theMatrix = new UInt16[shape[0], shape[1]];
            var h5Matrix = new H5Array<UInt16>(theMatrix);
            H5D.read(matrixId, matrixTypeId, h5Matrix);

            return theMatrix;
        }

        public void FillInt16Matrix(UInt16[,] memMatrix)
        {

            var wrapArray = new H5Array<UInt16>(memMatrix);
            H5D.write(matrixId, matrixTypeId, wrapArray);
        }

        public double[,] GetDoubleMatrix()
        {
            matrixId = H5D.open(groupId, matrixName);
            matrixSpaceId = H5D.getSpace(matrixId);
            shape = H5S.getSimpleExtentDims(matrixSpaceId);
            matrixTypeId = H5D.getType(matrixId);
            var theDMatrix = new double[shape[0], shape[1]];
            var h5matrix = new H5Array<double>(theDMatrix);
            H5D.read(matrixId, matrixTypeId,  h5matrix);

            return theDMatrix;
        }

        public UInt16[,] GetInt16MatrixWithMap(int[] mapping, int offsetsize)
        {

            matrixId = H5D.open(groupId, matrixName);
            matrixSpaceId = H5D.getSpace(matrixId);
            shape = H5S.getSimpleExtentDims(matrixSpaceId);
            matrixTypeId = H5D.getType(matrixId);
            var matrix = new UInt16[shape[0], shape[1]];
            var h5Matrix = new H5Array<UInt16>(matrix);
            H5D.read(matrixId, matrixTypeId, h5Matrix);
            var off_set_matrix = new UInt16[offsetsize, offsetsize];

            for (var i = 0; i < shape[0]; i++)
            {
                for (var j = 0; j < shape[1]; j++)
                {
                    off_set_matrix[mapping[i], mapping[j]] = matrix[i, j];
                }
            }
            return off_set_matrix;
        }

        public void FillFloatMatrix(float[,] memMatrix)
        {
            var wrapArray = new H5Array<float>(memMatrix);
            H5D.write(matrixId, matrixTypeId, wrapArray);
        }

        public static H5DataSetId CreateMatrixIfNoneExists(H5GroupId groupId, string matrixName, long[] shape, H5DataTypeId matrixTypeId, H5DataSpaceId matrixSpaceId)
        {
            H5DataSetId newMatrixId;

            if (H5L.Exists(groupId, matrixName))
            {
                newMatrixId = H5D.open(groupId, matrixName);
            }
            else
            {
                H5PropertyListId linkp = H5P.create(H5P.PropertyListClass.LINK_CREATE);
                H5PropertyListId accessp = H5P.create(H5P.PropertyListClass.DATASET_ACCESS);
                H5PropertyListId createp = H5P.create(H5P.PropertyListClass.DATASET_CREATE);
                H5P.setChunk(createp, shape);
                H5P.setDeflate(createp, 1);

                newMatrixId = H5D.create(groupId, matrixName, matrixTypeId, matrixSpaceId, linkp, createp, accessp);
            }
            return newMatrixId;
        }

    }
}