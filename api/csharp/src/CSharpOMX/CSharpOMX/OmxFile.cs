using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace CSharpOMX
{
    public static class OmxFile
    {
        public static OmxReadStream OpenReadOnly(string filePath)
        {
            OmxReadStream rs = new OmxReadStream(filePath);

            rs.OpenReadOnly();
            return rs;
        }

        public static OmxWriteStream OpenReadWrite(string filePath)
        {
            OmxWriteStream ws = new OmxWriteStream(filePath);
            ws.OpenReadWrite();
            return ws;
        }

        public static OmxWriteStream Create(string filePath, int zones, bool overwrite)
        {
            OmxWriteStream ws = new OmxWriteStream(filePath);
            ws.CreateFileOMX(zones, overwrite);
            return ws;
        }
    }
}
