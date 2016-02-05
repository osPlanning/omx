using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Threading.Tasks;
using HDF5DotNet;

namespace CSharpOMX
{
    public class OmxBase
    {
        protected string filepath;
        protected H5FileId fileId;
        protected H5GroupId dataGroup;
        protected H5GroupId luGroup; // lookupGroup
        protected Dictionary<string, H5DataSetId> tables = new Dictionary<string, H5DataSetId>();
        protected Dictionary<string, H5DataSetId> indexMaps = new Dictionary<string, H5DataSetId>();

        // OMX Version 0.2 required fields
        protected string[] dllVersion = { "0.2" };
        protected string dataGroupName = "/data";
        protected string luGroupName = "/lookup";
        protected string omxVersionName = "OMX_VERSION";
        protected string omxShapeAttr = "SHAPE";

        // OMX file attributes
        public string OmxVersion { get; protected set; }
        public long[] Shape { get; protected set; }
        public string[] MatrixNames { get; protected set; }
        public int NumMatrix { get; protected set; }
        public string[] IndexMapNames { get; protected set; }
        public int NumIndexMap { get; protected set; }
        public bool IsValid { get; protected set; }
        public string ErrMessage { get; protected set; }

        public void Close()
        {
            closeTables();
            closeIndexMaps();
            if (fileId != null) H5F.close(fileId);
            this.IsValid = false;
            this.Shape = new long[] { 0, 0 };
        }

        private void closeTables()
        {
            foreach (KeyValuePair<string, H5DataSetId> matrix in tables)
            {
                H5D.close(matrix.Value);
            }
            tables.Clear();
            if (dataGroup != null) H5G.close(dataGroup);
        }

        private void closeIndexMaps()
        {
            foreach (KeyValuePair<string, H5DataSetId> im in indexMaps)
            {
                H5D.close(im.Value);
            }
            indexMaps.Clear();
            if (luGroup != null) H5G.close(luGroup);
        }


        protected T[] getAttribute<T>(H5AttributeId aid)
        {
            H5DataTypeId sv = H5A.getType(aid);
            int size = H5T.getSize(sv);
            var attValue = new T[size];
            H5A.read<T>(aid, sv, new H5Array<T>(attValue));

            return attValue;
        }

        protected T[] setAttribute<T>(H5AttributeId aid)
        {
            H5DataTypeId sv = H5A.getType(aid);
            int size = H5T.getSize(sv);
            var attValue = new T[size];
            H5A.read<T>(aid, sv, new H5Array<T>(attValue));

            return attValue;
        }


    }
}
