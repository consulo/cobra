package org.cobraparser.css;

import org.cobraparser.util.gui.ColorFactory;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * @author VISTALL
 * @since 2024-11-18
 */
public abstract class StandardColorProvider {
    public static final StandardColorProvider INSTANCE = init();

    private static StandardColorProvider init() {
        ServiceLoader<StandardColorProvider> loader = ServiceLoader.load(StandardColorProvider.class, StandardColorProvider.class.getClassLoader());

        Optional<StandardColorProvider> first = loader.findFirst();
        if (first.isPresent()) {
            return first.get();
        }

        return new DefaultStandardColorProvider();
    }

    private final Map<String, Color> colorMap = new HashMap<>();

    public StandardColorProvider() {
        fill(colorMap);
    }

    public void fill(Map<String, Color> map) {
        map.put("transparent", ColorFactory.TRANSPARENT);

        // http://www.w3schools.com/css/css_colornames.asp
        map.put("aliceblue", new Color(0xf0f8ff));
        map.put("antiquewhite", new Color(0xfaebd7));
        map.put("aqua", new Color(0x00ffff));
        map.put("aquamarine", new Color(0x7fffd4));
        map.put("azure", new Color(0xf0ffff));
        map.put("beige", new Color(0xf5f5dc));
        map.put("bisque", new Color(0xffe4c4));
        map.put("black", new Color(0x000000));
        map.put("blanchedalmond", new Color(0xffebcd));
        map.put("blue", new Color(0x0000ff));
        map.put("blueviolet", new Color(0x8a2be2));
        map.put("brown", new Color(0xa52a2a));
        map.put("burlywood", new Color(0xdeb887));
        map.put("cadetblue", new Color(0x5f9ea0));
        map.put("chartreuse", new Color(0x7fff00));
        map.put("chocolate", new Color(0xd2691e));
        map.put("coral", new Color(0xff7f50));
        map.put("cornflowerblue", new Color(0x6495ed));
        map.put("cornsilk", new Color(0xfff8dc));
        map.put("crimson", new Color(0xdc143c));
        map.put("cyan", new Color(0x00ffff));
        map.put("darkblue", new Color(0x00008b));
        map.put("darkcyan", new Color(0x008b8b));
        map.put("darkgoldenrod", new Color(0xb8860b));
        map.put("darkgray", new Color(0xa9a9a9));
        map.put("darkgrey", new Color(0xa9a9a9));
        map.put("darkgreen", new Color(0x006400));
        map.put("darkkhaki", new Color(0xbdb76b));
        map.put("darkmagenta", new Color(0x8b008b));
        map.put("darkolivegreen", new Color(0x556b2f));
        map.put("darkorange", new Color(0xff8c00));
        map.put("darkorchid", new Color(0x9932cc));
        map.put("darkred", new Color(0x8b0000));
        map.put("darksalmon", new Color(0xe9967a));
        map.put("darkseagreen", new Color(0x8fbc8f));
        map.put("darkslateblue", new Color(0x483d8b));
        map.put("darkslategray", new Color(0x2f4f4f));
        map.put("darkslategrey", new Color(0x2f4f4f));
        map.put("darkturquoise", new Color(0x00ced1));
        map.put("darkviolet", new Color(0x9400d3));
        map.put("deeppink", new Color(0xff1493));
        map.put("deepskyblue", new Color(0x00bfff));
        map.put("dimgray", new Color(0x696969));
        map.put("dimgrey", new Color(0x696969));
        map.put("dodgerblue", new Color(0x1e90ff));
        map.put("firebrick", new Color(0xb22222));
        map.put("floralwhite", new Color(0xfffaf0));
        map.put("forestgreen", new Color(0x228b22));
        map.put("fuchsia", new Color(0xff00ff));
        map.put("gainsboro", new Color(0xdcdcdc));
        map.put("ghostwhite", new Color(0xf8f8ff));
        map.put("gold", new Color(0xffd700));
        map.put("goldenrod", new Color(0xdaa520));
        map.put("gray", new Color(0x808080));
        map.put("grey", new Color(0x808080));
        map.put("green", new Color(0x008000));
        map.put("greenyellow", new Color(0xadff2f));
        map.put("honeydew", new Color(0xf0fff0));
        map.put("hotpink", new Color(0xff69b4));
        map.put("indianred", new Color(0xcd5c5c));
        map.put("indigo", new Color(0x4b0082));
        map.put("ivory", new Color(0xfffff0));
        map.put("khaki", new Color(0xf0e68c));
        map.put("lavender", new Color(0xe6e6fa));
        map.put("lavenderblush", new Color(0xfff0f5));
        map.put("lawngreen", new Color(0x7cfc00));
        map.put("lemonchiffon", new Color(0xfffacd));
        map.put("lightblue", new Color(0xadd8e6));
        map.put("lightcoral", new Color(0xf08080));
        map.put("lightcyan", new Color(0xe0ffff));
        map.put("lightgoldenrodyellow", new Color(0xfafad2));
        map.put("lightgray", new Color(0xd3d3d3));
        map.put("lightgrey", new Color(0xd3d3d3));
        map.put("lightgreen", new Color(0x90ee90));
        map.put("lightpink", new Color(0xffb6c1));
        map.put("lightsalmon", new Color(0xffa07a));
        map.put("lightseagreen", new Color(0x20b2aa));
        map.put("lightskyblue", new Color(0x87cefa));
        map.put("lightslategray", new Color(0x778899));
        map.put("lightslategrey", new Color(0x778899));
        map.put("lightsteelblue", new Color(0xb0c4de));
        map.put("lightyellow", new Color(0xffffe0));
        map.put("lime", new Color(0x00ff00));
        map.put("limegreen", new Color(0x32cd32));
        map.put("linen", new Color(0xfaf0e6));
        map.put("magenta", new Color(0xff00ff));
        map.put("maroon", new Color(0x800000));
        map.put("mediumaquamarine", new Color(0x66cdaa));
        map.put("mediumblue", new Color(0x0000cd));
        map.put("mediumorchid", new Color(0xba55d3));
        map.put("mediumpurple", new Color(0x9370d8));
        map.put("mediumseagreen", new Color(0x3cb371));
        map.put("mediumslateblue", new Color(0x7b68ee));
        map.put("mediumspringgreen", new Color(0x00fa9a));
        map.put("mediumturquoise", new Color(0x48d1cc));
        map.put("mediumvioletred", new Color(0xc71585));
        map.put("midnightblue", new Color(0x191970));
        map.put("mintcream", new Color(0xf5fffa));
        map.put("mistyrose", new Color(0xffe4e1));
        map.put("moccasin", new Color(0xffe4b5));
        map.put("navajowhite", new Color(0xffdead));
        map.put("navy", new Color(0x000080));
        map.put("oldlace", new Color(0xfdf5e6));
        map.put("olive", new Color(0x808000));
        map.put("olivedrab", new Color(0x6b8e23));
        map.put("orange", new Color(0xffa500));
        map.put("orangered", new Color(0xff4500));
        map.put("orchid", new Color(0xda70d6));
        map.put("palegoldenrod", new Color(0xeee8aa));
        map.put("palegreen", new Color(0x98fb98));
        map.put("paleturquoise", new Color(0xafeeee));
        map.put("palevioletred", new Color(0xd87093));
        map.put("papayawhip", new Color(0xffefd5));
        map.put("peachpuff", new Color(0xffdab9));
        map.put("peru", new Color(0xcd853f));
        map.put("pink", new Color(0xffc0cb));
        map.put("plum", new Color(0xdda0dd));
        map.put("powderblue", new Color(0xb0e0e6));
        map.put("purple", new Color(0x800080));
        map.put("red", new Color(0xff0000));
        map.put("rosybrown", new Color(0xbc8f8f));
        map.put("royalblue", new Color(0x4169e1));
        map.put("saddlebrown", new Color(0x8b4513));
        map.put("salmon", new Color(0xfa8072));
        map.put("sandybrown", new Color(0xf4a460));
        map.put("seagreen", new Color(0x2e8b57));
        map.put("seashell", new Color(0xfff5ee));
        map.put("sienna", new Color(0xa0522d));
        map.put("silver", new Color(0xc0c0c0));
        map.put("skyblue", new Color(0x87ceeb));
        map.put("slateblue", new Color(0x6a5acd));
        map.put("slategray", new Color(0x708090));
        map.put("slategrey", new Color(0x708090));
        map.put("snow", new Color(0xfffafa));
        map.put("springgreen", new Color(0x00ff7f));
        map.put("steelblue", new Color(0x4682b4));
        map.put("tan", new Color(0xd2b48c));
        map.put("teal", new Color(0x008080));
        map.put("thistle", new Color(0xd8bfd8));
        map.put("tomato", new Color(0xff6347));
        map.put("turquoise", new Color(0x40e0d0));
        map.put("violet", new Color(0xee82ee));
        map.put("wheat", new Color(0xf5deb3));
        map.put("white", new Color(0xffffff));
        map.put("whitesmoke", new Color(0xf5f5f5));
        map.put("yellow", new Color(0xffff00));
        map.put("yellowgreen", new Color(0x9acd32));
    }

    public Color getColor(String color) {
        return colorMap.get(color);
    }
}
