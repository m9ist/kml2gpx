package ru.m9ist;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.PrintStream;

public class XMLParser {
    private final File tmpDir;
    private final File outDir;
    private final String achiever;
    final static private DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    public XMLParser(final File tmpDir, final File outDir, final String achiever) {
        this.tmpDir = tmpDir;
        this.outDir = outDir;
        this.achiever = achiever;
    }

    private static class DocInfo {
        String name = null;
    }

    private static class NodeInfo {
        String name = null;
        String descr = null;

        // следующие поля задействуются только в точке
        String time = null;
        String lat = null;
        String lon = null;
        String ele = null;

        // для путя
        final StringBuilder track = new StringBuilder();
        String type = null;

        @Override
        public String toString() {
            return "NodeInfo{" +
                    "name='" + name + '\'' +
                    ", descr='" + descr + '\'' +
                    ", time='" + time + '\'' +
                    ", lat='" + lat + '\'' +
                    ", ele='" + ele + '\'' +
                    ", lon='" + lon + '\'' +
                    ", type='" + type + '\'' +
                    ", trackSize='" + track.length() + '\'' +
                    '}';
        }
    }

    public String parseXML(final File fileSourse) {
        try {
            //Using factory get an instance of document builder
            final DocumentBuilder db = dbf.newDocumentBuilder();
            //parse using builder to get DOM representation of the XML file
            final Document dom = db.parse(fileSourse);
            final Element dElement = dom.getDocumentElement();
            Node document = null;
            NodeList childNodes = dElement.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                final Node node = childNodes.item(i);
                if ("Document".equals(node.getNodeName()))
                    document = node;
            }
            if (document == null) {
                System.out.println("Not found Document node!!!");
                return null;
            }

            final NodeList docInfoNodes = document.getChildNodes();
            final DocInfo docInfo = new DocInfo();
            boolean isPoint;
            final StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<gpx\n" +
                    "  xmlns=\"http://www.topografix.com/GPX/1/0\"\n" +
                    "  version=\"1.0\" creator=\"kazmin.oleg@gmail.com\"\n" +
                    "  xmlns:wissenbach=\"http://www.cableone.net/cdwissenbach\"\n" +
                    "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                    "  xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\n" +
                    "                      http://www.cableone.net/cdwissenbach http://www.cableone.net/cdwissenbach/wissenbach.xsd\">\n");
            for (int i = 0; i < docInfoNodes.getLength(); i++) {
                final Node item = docInfoNodes.item(i);
                isPoint = true;
                if (isEmptyNode(item)) {
                    continue;
                }
                final String name = item.getNodeName();
                if ("open".equals(name)) {
                    //<open>1</open>
                    //todo разобраться что это
                    continue;
                } else if ("visibility".equals(name)) {
                    //<visibility>1</visibility>
                    //тоже мало понятно что это, расшаренность треков возможно? в любом случае сами будем это поле заполнять если что
                    continue;
                } else if ("name".equals(name)) {
                    //<name><![CDATA[ул. Багратиона, 29]]></name>
                    docInfo.name = getNotEmptyNode(item).getNodeValue();
                    continue;
                } else if ("atom:author".equals(name)) {
                    //<atom:author><atom:name><![CDATA[Создано в приложении Мои треки на Android]]></atom:name></atom:author>
                    // игнорируем эту фигню, она не нужна нам, в крайнем случае укажем, что мы сами сконвертировали эту шнягу
                    continue;
                } else if ("Style".equals(name)) {
                    // судя по всему это финтифлюшки с картинками, на которые нам пофиг
                    continue;
                } else if ("Schema".equals(name)) {
                    // судя по всему тут доп информация от датчиков, которых у меня пока нет
                    continue;
                } else if ("Placemark".equals(name)) {
                    // есть тэги без названий, которые регистрируют, как я понимаю промежуточные точки
                    // и есть тэг с id = tour который и является нашим путем, нам надо вытащить что мы заполняем тут
                    // и дальше уже распарсить его вне if'ов
                    final NamedNodeMap attributes = item.getAttributes();
                    if (attributes.getLength() > 0) {
                        // у нас именно трек? на всякий случай убедимся на случай смены формата ^^
                        if (attributes.getLength() == 1) {
                            final Node att = attributes.item(0);
                            if ("id".equals(att.getNodeName()) && "tour".equals(att.getNodeValue())) {
                                isPoint = false;
                            }
                        }
                        if (isPoint) {
                            throw new RuntimeException("Unsupported format, need to understand why");
                        }
                    }
                } else {
                    throw new RuntimeException("Undefined node name: " + name);
                }

                // проходимся и вытаскиваем все тэги
                final NodeList placemark = item.getChildNodes();
                final NodeInfo nodeInfo = new NodeInfo();
                for (int j = 0; j < placemark.getLength(); j++) {
                    final Node node = placemark.item(j);
                    if (isEmptyNode(node)) {
                        continue;
                    }
                    final String nodeName = node.getNodeName();
                    if ("name".equals(nodeName)) {
                        nodeInfo.name = getNotEmptyNode(node).getNodeValue();
                    } else if ("description".equals(nodeName)) {
                        nodeInfo.descr = getNotEmptyNode(node).getNodeValue();
                    } else //noinspection StatementWithEmptyBody
                        if ("styleUrl".equals(nodeName)) {
                            // игнорируем этот тэг
                        } else {
                            if (isPoint) {
                                if ("TimeStamp".equals(nodeName)) {
                                    nodeInfo.time = getNotEmptyNode(getNotEmptyNode(node)).getNodeValue();
                                } else if ("Point".equals(nodeName)) {
                                    final String[] coords = getNotEmptyNode(getNotEmptyNode(node)).getNodeValue().split(",");
                                    nodeInfo.lat = coords[1];
                                    nodeInfo.lon = coords[0];
                                    nodeInfo.ele = coords[2];
                                } else {
                                    throw new RuntimeException("Unsupported tag name in point tag: " + nodeName);
                                }
                            } else {
                                if ("ExtendedData".equals(nodeName)) {
                                    nodeInfo.type = getNotEmptyNode(getNotEmptyNode(getNotEmptyNode(node))).getNodeValue();
                                } else if ("gx:MultiTrack".equals(nodeName)) {
                                    parseTrack(getTrackNode(node), nodeInfo.track);
                                } else {
                                    throw new RuntimeException("Unsupported tag name in track tag: " + nodeName);
                                }
                            }
                        }
                }

                if (isPoint) {
                    if (nodeInfo.lat == null
                            || nodeInfo.lon == null
                            || nodeInfo.name == null
                            || nodeInfo.descr == null
                            || nodeInfo.ele == null
                            || nodeInfo.time == null
                            ) {
                        throw new RuntimeException("Undefined situation: " + nodeInfo.toString());
                    }
                    sb.append("\t<wpt lat=\"").append(nodeInfo.lat);
                    sb.append("\" lon=\"").append(nodeInfo.lon);
                    sb.append("\">\n\t\t<ele>").append(nodeInfo.ele);
                    sb.append("</ele>\n\t\t<time>").append(nodeInfo.time);
                    sb.append("</time>\n\t\t<name><![CDATA[").append(nodeInfo.name);
                    sb.append("]]></name>\n\t\t<desc><![CDATA[").append(nodeInfo.descr); // can add <cmt>
                    sb.append("]]></desc>\n\t\t<type><![CDATA[");//todo add sym and type??
                    sb.append("]]></type>\n\t</wpt>\n");
                } else {
                    //todo
                    try {
                        if (nodeInfo.name == null
                                || nodeInfo.descr == null
                                || nodeInfo.type == null
                                || nodeInfo.track.length() == 0
                                ) {
                            throw new RuntimeException("Undefined situation: " + nodeInfo.toString());
                        }
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                    sb.append("\t<trk>\n\t\t<name>").append(nodeInfo.name);
                    sb.append("</name>\n\t\t<desc><![CDATA[").append(nodeInfo.descr); // can add <cmt>
                    sb.append("]]></desc>\n\t\t<type><![CDATA[").append(nodeInfo.type);//todo add sym??
                    sb.append("]]></type>\n\t\t<trkseg>\n").append(nodeInfo.track.toString());
                    sb.append("\n\t\t</trkseg>\n\t</trk>\n");
                }
            }
            sb.append("</gpx>");
            return sb.toString();
        } catch (final Exception ignored) {
            ignored.printStackTrace();
        }
        return null;
    }

    private static Node getTrackNode(Node node) {
        final NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            node = childNodes.item(i);
            if (isEmptyNode(node)) {
                continue;
            }
            if ("gx:Track".equals(node.getNodeName())) {
                return node;
            }
            if (node.getChildNodes().getLength() > 0) {
                node = getTrackNode(node);
                if (node != null) {
                    return node;
                }
            }
        }
        return null;
    }

    private static void parseTrack(Node node, final StringBuilder sb) {
        final NodeList childNodes = node.getChildNodes();
        String time = null;
        for (int i = 0; i < childNodes.getLength(); i++) {
            node = childNodes.item(i);
            if (isEmptyNode(node))
                continue;
            final String nodeName = node.getNodeName();
            if ("when".equals(nodeName)) {
                time = getNotEmptyNode(node).getNodeValue();
            } else if ("gx:coord".equals(nodeName)) {
                final String[] coords = getNotEmptyNode(node).getNodeValue().split(" ");
                if (time == null)
                    throw new RuntimeException("Time before coordinates!!!");
                if (sb.length() > 0)
                    sb.append("\t\n");
                sb.append("\t\t\t<trkpt lat=\"").append(coords[1]);
                sb.append("\" lon=\"").append(coords[0]);
                sb.append("\"><ele>").append(coords[2]);
                sb.append("</ele><time>").append(time);
                sb.append("</time></trkpt>");//<sym>Waypoint</sym>
                time = null;
            } else //noinspection StatementWithEmptyBody
                if ("ExtendedData".equals(nodeName)) {
                    //speed bearing accuracy
                } else {
                    throw new RuntimeException("Unsupported tag name in track tag: " + nodeName);
                }
        }
    }

    private static Node getNotEmptyNode(Node node) {
        final NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            node = childNodes.item(i);
            if (isEmptyNode(node))
                continue;
            return node;
        }
        return null;
    }

    private static boolean isEmptyNode(final Node node) {
        return "#text".equals(node.getNodeName()) && "\n".equals(node.getNodeValue());
    }

    private void processDir(final File dir) {
        System.out.println("  Process directory " + dir.getAbsolutePath());
        for (File source : dir.listFiles()) {
            if (source.isDirectory()) {
                processDir(source);
            } else if (source.isFile()) {
                processFile(source);
            }
        }
    }

    private void processFile(final File file) {
        System.out.println("    Process file " + file.getAbsolutePath());
        if (!file.getName().endsWith(".kmz")) {
            System.out.println("    --- Not kmz file, skipping.");
            return;
        }
        final String command = achiever + " e \"" + file.getAbsolutePath() + "\" -o\"" + tmpDir.getAbsolutePath() + "\"";
        try {
            final Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
        } catch (final Exception ignored) {
            ignored.printStackTrace();
        }
        final File kmlFile = findKMLFile(tmpDir);
        if (kmlFile == null) {
            System.out.println("    --- Not found kml file in archive.");
        }
        final String out = parseXML(kmlFile);
        if (out == null || out.length() == 0) {
            System.out.println("    --- Empty kml file.");
        } else {
            try {
                final PrintStream outputFie = new PrintStream(getOutputFile(file));
                try {
                    outputFie.print(out);
                } finally {
                    outputFie.close();
                }
            } catch (final Exception ignored) {
                ignored.printStackTrace();
            }
        }
        System.out.println("    --- Processed, clear tmp directory.");
        clearDir(tmpDir);
    }

    private File getOutputFile(final File inputFile) {
        final String name = inputFile.getName().substring(0, inputFile.getName().length() - 4);
        for (int i = 0; i < 50; i++) {
            final File res = new File(outDir, name + getRandom() + ".gpx");
            if (!res.exists())
                return res;
        }
        return null;
    }

    final private static char[] chars = new char[26 + 10];

    static {
        for (int i = 0; i < 10; i++) {
            chars[i] = (char) ('0' + i);
        }
        for (int i = 0; i < 26; i++) {
            chars[i + 10] = (char) ('A' + i);
        }
    }

    private static String getRandom() {
        final StringBuilder sb = new StringBuilder("_");
        for (int i = 0; i < 5; i++) {
            final int j = (int) (Math.random() * chars.length);
            sb.append(chars[Math.min(j, chars.length - 1)]);
        }
        return sb.toString();
    }

    private File findKMLFile(final File source) {
        if (source == null)
            return null;
        for (final File file : source.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".kml")) {
                return file;
            }
            if (file.isDirectory()) {
                final File kmlFile = findKMLFile(file);
                if (kmlFile != null)
                    return kmlFile;
            }
        }
        return null;
    }

    private void clearDir(final File source) {
        if (source == null)
            return;
        if (source.isFile()) {
            source.delete();
        } else {
            for (final File file : source.listFiles()) {
                clearDir(file);
            }
            if (source != tmpDir) {
                source.delete();
            }
        }
    }

    public static void main(final String[] args) {
        final File tmpDir = new File("C:\\Users\\Oleg\\Desktop\\tmp");
        final String achiever = "C:\\Program Files\\7-Zip\\7z.exe";
        final File input = new File("C:\\Users\\Oleg\\Desktop\\tracks");
        final File outDir = new File("C:\\Users\\Oleg\\Desktop\\out");
        if (!input.exists()) {
            System.out.println("No input!");
            return;
        }
        tmpDir.mkdirs();
        outDir.mkdirs();
        if (!outDir.exists() || !outDir.isDirectory()) {
            System.out.println("Error creating output dir! " + outDir.getAbsolutePath());
            return;
        }
        if (!tmpDir.isDirectory()) {
            System.out.println("TMP directory cannot be created or is a file!");
            return;
        }
        final XMLParser xmlParser = new XMLParser(tmpDir, outDir, achiever);
        xmlParser.clearDir(tmpDir);
        System.out.println("Start processing " + input.getAbsolutePath());
        if (input.isFile()) {
            xmlParser.processFile(input);
        } else if (input.isDirectory()) {
            xmlParser.processDir(input);
        }
        xmlParser.clearDir(tmpDir);
        tmpDir.delete();
        System.out.println("Stop program working...");
    }
}
