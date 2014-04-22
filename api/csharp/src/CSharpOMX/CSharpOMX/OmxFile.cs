using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Threading.Tasks;
using HDF5DotNet;

namespace CSharpOMX
{
    public class OmxFile
    {
        private string filepath;
        private H5FileId fileID;
        public string[] omxVersion = { "0.2" };
        private H5GroupId rootGroup;
        private long[] shape;
        private string rootGroupName = "/data";
        public string omxVersionName = "OMX_Version";

        public OmxFile(string filePath, long[] shape)
        {
            this.filepath = filePath;
            this.shape = shape;

        }

        public long[] Shape
        {
            get
            {
                return shape;
            }

        }

        public H5FileId OpenReadOnly()
        {
            this.fileID = H5F.open(filepath, H5F.OpenMode.ACC_RDONLY);
            return fileID;
        }

        public H5FileId OpenReadWrite()
        {
            this.fileID = H5F.open(filepath, H5F.OpenMode.ACC_RDWR);
            return fileID;
        }

        public H5FileId OpenDebug()
        {
            this.fileID = H5F.open(filepath, H5F.OpenMode.ACC_DEBUG);
            return fileID;
        }

        public void Close()
        {
            this.fileID = fileID;
            H5F.close(fileID);
 
        }

        public H5FileId FileID
        {
            get
            {

                return fileID;
            }

        }

        public string RootGroupName
        {
            get
            {
                return rootGroupName;
            }


        }


        public string[] OmxVersion
        {
            get
            {
                return omxVersion;
            }

        }

        public void CreateFileOMX()
        {
            OverwriteCheck();
            H5F.create(filepath, H5F.CreateMode.ACC_EXCL);
            fileID = H5F.open(filepath, H5F.OpenMode.ACC_RDWR);
            rootGroup = H5G.create(fileID, rootGroupName);
            long[] dims = { 1 };
            long[] max_dims = { 1 };
            var omxVtype = H5T.copy(H5T.H5Type.C_S1);
            var omxVspace = H5S.create_simple(1, dims, max_dims);
            var h5OmxVersion = new H5Array<string>(omxVersion);

            H5DataSetId omxVersionAttribute = H5D.create(fileID, omxVersionName, omxVtype, omxVspace);
            H5D.write(omxVersionAttribute, omxVtype, h5OmxVersion);

        }


        public void OverwriteCheck()
        {

            if (File.Exists(filepath))
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

        public void CreateMapping(int[] tazEquiv, string mapName)
        {
            H5GroupId groupId = H5G.open(fileID, rootGroupName);
            long[] oneDShape = { shape[0] };
            H5DataSpaceId mapSpaceId = H5S.create_simple(1, oneDShape);
            H5DataTypeId mapTypeId = new H5DataTypeId(H5T.H5Type.NATIVE_INT);

            H5DataSetId newMappingID = H5D.create(groupId, mapName, mapTypeId, mapSpaceId);
            var wrapArray = new H5Array<Int32>(tazEquiv);
            H5D.write(newMappingID, mapTypeId, wrapArray);
        }

        public int[] GetMapping(string mapName)
        {
            H5GroupId groupId = H5G.open(fileID, rootGroupName);
            H5DataSetId mapId = H5D.open(groupId, mapName);
            H5DataSpaceId mapSpaceId = H5D.getSpace(mapId);
            shape = H5S.getSimpleExtentDims(mapSpaceId);
            H5DataTypeId mapTypeId = H5D.getType(mapId);
            var map = new int[shape[0]];
            var h5map = new H5Array<int>(map);
            H5D.read(mapId, mapTypeId, h5map);

            return map;
        }

    }
}
