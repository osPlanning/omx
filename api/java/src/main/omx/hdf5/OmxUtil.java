package omx.hdf5;

import java.util.Arrays;
import java.util.Map;

/**
 * The {@code OmxUtil} ...
 *
 * @author crf
 *         Started 9/18/13 3:49 PM
 */
public class OmxUtil {

    public static String deepToString(OmxDataset dataset) {
        StringBuilder sb = new StringBuilder();
        sb.append("dataset '").append(dataset.getName()).append("':").append("\n");
        sb.append("\tdims: ").append(Arrays.toString(dataset.getShape())).append("\n");
        sb.append("\tdatatype: ").append(dataset.getDatatype().getDatatypeName()).append(" (").append(dataset.getDatatype().getOmxJavaType()).append(")").append("\n");
//        if (dataset.getTags().size() > 0)
//            sb.append("\ttags: ").append(getTags().deepToString()).append("\n");
        Map<String,Object> attributes = dataset.getAttributes();
        if (attributes.size() > 0) {
            sb.append("\tattributes (").append(attributes.size()).append("):").append("\n");
            for (String attribute : attributes.keySet())
                sb.append("\t\t").append(attribute).append("  :  ").append(objectToString(attributes.get(attribute))).append("\n");
        }
        if (sb.charAt(sb.length()-1) == '\t')
            sb.deleteCharAt(sb.length()-1); //pop off last tab
        return sb.toString();
    }

    public static String deepToString(OmxGroup group) {
        StringBuilder sb = new StringBuilder();
        sb.append("group '").append(group.getName()).append("':").append("\n");
        Map<String,Object> attributes = group.getAttributes();
        if (attributes.size() > 0) {
            sb.append("\tattributes (").append(attributes.size()).append("):").append("\n");
            for (String attribute : attributes.keySet())
                sb.append("\t\t").append(attribute).append("  :  ").append(objectToString(attributes.get(attribute))).append("\n");
        }
        if (group.getNamedDatatypes().size() > 0) {
            sb.append("\tnamed datatypes:").append("\n");
            for (String nType : group.getNamedDatatypes())
                sb.append("\t\t").append(nType).append("\n");
        }
        if (group.getNtypes().size() > 0) {
            sb.append("\tntypes:").append("\n");
            for (String nType : group.getNtypes())
                sb.append("\t\t").append(nType).append("\n");
        }
        if (group.getUnknownTypes().size() > 0) {
            sb.append("\tunknown types:").append("\n");
            for (String unknownType : group.getUnknownTypes())
                sb.append("\t\t").append(unknownType).append("\n");
        }
        if (group.getDatasets().size() > 0) {
            sb.append("\t");
            for (OmxDataset omxDataset : group.getDatasets())
                sb.append(OmxUtil.deepToString(omxDataset).replace("\n","\n\t"));
        }
        if (group.getGroups().size() > 0) {
            sb.append("\t");
            for (OmxGroup omxGroup : group.getGroups())
                sb.append(deepToString(omxGroup).replace("\n","\n\t"));
        }
        if (sb.charAt(sb.length()-1) == '\t')
            sb.deleteCharAt(sb.length()-1); //pop off last tab
        return sb.toString();
    }

    public static String objectToString(Object value) {
        if (value == null)
            return "null";
        else if (value.getClass().isArray()) {
            Class<?> component = value.getClass().getComponentType();
            if (component == byte.class)
                return Arrays.toString((byte[]) value);
            if (component == short.class)
                return Arrays.toString((short[]) value);
            if (component == int.class)
                return Arrays.toString((int[]) value);
            if (component == long.class)
                return Arrays.toString((long[]) value);
            if (component == float.class)
                return Arrays.toString((float[]) value);
            if (component == double.class)
                return Arrays.toString((double[]) value);
            if (component == boolean.class)
                return Arrays.toString((boolean[]) value);
            if (component == char.class)
                return Arrays.toString((char[]) value);
            else
                return Arrays.toString((Object[]) value);
        }
        return value.toString();
    }

}
