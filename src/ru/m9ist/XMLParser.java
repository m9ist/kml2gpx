package ru.m9ist;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class XMLParser {
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

    public void parseXML(final String fileName) {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            //Using factory get an instance of document builder
            final DocumentBuilder db = dbf.newDocumentBuilder();
            //parse using builder to get DOM representation of the XML file
            final Document dom = db.parse(fileName);
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
                return;
            }

            final NodeList docInfoNodes = document.getChildNodes();
            final DocInfo docInfo = new DocInfo();
            boolean isPoint;
            final StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><gpx\n" +
                    "  xmlns=\"http://www.topografix.com/GPX/1/0\"\n" +
                    "  version=\"1.0\" creator=\"kazmin.oleg@gmail.com\"\n" +
                    "  xmlns:wissenbach=\"http://www.cableone.net/cdwissenbach\"\n" +
                    "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                    "  xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\n" +
                    "                       http://www.cableone.net/cdwissenbach http://www.cableone.net/cdwissenbach/wissenbach.xsd\">");
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
                                    nodeInfo.lat = coords[0];
                                    nodeInfo.lon = coords[1];
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
                    sb.append("<wpt lat=\"").append(nodeInfo.lat);
                    sb.append("\" lon=\"").append(nodeInfo.lon);
                    sb.append("\"><ele>").append(nodeInfo.ele);
                    sb.append("</ele><time>").append(nodeInfo.time);
                    sb.append("</time><name><![CDATA[").append(nodeInfo.name);
                    sb.append("]]></name><desc><![CDATA[").append(nodeInfo.descr); // can add <cmt>
                    sb.append("]]></desc><type><![CDATA[");//todo add sym and type??
                    sb.append("]]></type></wpt>");
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
                    sb.append("<trk><name>").append(nodeInfo.name);
                    sb.append("</name><desc><![CDATA[").append(nodeInfo.descr); // can add <cmt>
                    sb.append("]]></desc><type><![CDATA[").append(nodeInfo.type);//todo add sym??
                    sb.append("]]></type><trkseg>").append(nodeInfo.track.toString());
                    sb.append("</trkseg></trk>");
                }
            }
            sb.append("</gpx>");
            System.out.println(sb.toString());
            System.out.println();
            System.out.println();
            System.out.println();
        } catch (final Exception ignored) {
            ignored.printStackTrace();
        }
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
                sb.append("<trkpt lat=\"").append(coords[0]);
                sb.append("\" lon=\"").append(coords[1]);
                sb.append("\"><ele>").append(coords[2]);
                sb.append("</ele><time>").append(time);
                sb.append("</time><sym>Waypoint</sym></trkpt>");
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

    public static void main(String[] args) {
        final String fileName = "D:\\Projects\\kml2gpx\\doc.kml";

        final XMLParser xmlParser = new XMLParser();
        xmlParser.parseXML(fileName);
    }
}
