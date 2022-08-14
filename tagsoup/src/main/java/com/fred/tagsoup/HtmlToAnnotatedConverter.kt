package com.fred.tagsoup

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.sp
import org.ccil.cowan.tagsoup.HTMLSchema
import org.ccil.cowan.tagsoup.Parser
import org.xml.sax.*
import java.io.IOException
import java.io.StringReader
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.collections.ArrayList


fun htmlToAnnotatedString(html: String): AnnotatedString {
    val parser = Parser()
    try {
        parser.setProperty(Parser.schemaProperty, HTMLSchema())
        val converter = HtmlToAnnotatedConverter()
        parser.contentHandler = converter
        parser.parse(InputSource(StringReader(html)))
        return converter.convert()
    } catch (e: SAXNotRecognizedException) {
        // Should not happen.
//        throw RuntimeException(e)
    } catch (e: SAXNotSupportedException) {
        // Should not happen.
//        throw RuntimeException(e)
    }catch (e: SAXException) {
        // TagSoup doesn't throw parse exceptions.
//        throw java.lang.RuntimeException(e)
    }catch (e: IOException) {
        // We are reading from a string. There should not be IO problems.
//        throw java.lang.RuntimeException(e)
    }
    return buildAnnotatedString{}
}

val h1:SpanStyle  by lazy {
    SpanStyle(fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    )
}
val h2:SpanStyle  by lazy {
    SpanStyle(fontWeight = FontWeight.Bold,
        fontSize = 26.sp
    )
}
val h3:SpanStyle  by lazy {
    SpanStyle(fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    )
}
val h4:SpanStyle  by lazy {
    SpanStyle(fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    )
}
val h5:SpanStyle  by lazy {
    SpanStyle(fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    )
}
val h6:SpanStyle  by lazy {
    SpanStyle(fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp
    )
}

private class HtmlToAnnotatedConverter: ContentHandler {

    private val OL = "ol"
    private val UL = "ul"
    private val builder:AnnotatedString.Builder by lazy {
        AnnotatedString.Builder()
    }
    private var spanStyle: SpanStyle? = null
    //ParagraphStyle 不能出现嵌套
    private var paragraphStyle:ParagraphStyle? = null
    private var canAddParagraphStyle = true
    private var newLine = false
    private val styleIndexStack by lazy {
        Stack<ElementInfo>()
    }
    private val htmlList:ArrayList<HtmlList> by lazy {
        arrayListOf()
    }

    private val textAlignPattern by lazy {
        Pattern.compile("(?:\\s+|\\A)text-align\\s*:\\s*(\\S*)\\b")
    }
    private val textColorPattern by lazy {
        Pattern.compile("(?:\\s+|\\A)color\\s*:\\s*(\\S*)\\b")
    }
    private val listStyleTypePattern by lazy {
        Pattern.compile("(?:\\s+|\\A)list-style-type\\s*:\\s*(\\S*)\\b")
    }
    private val textBackgroundPattern by lazy {
        Pattern.compile("(?:\\s+|\\A)background(?:-color)?\\s*:\\s*(\\S*)\\b")
    }
    private val textDecorationPattern by lazy {
        Pattern.compile("(?:\\s+|\\A)text-decoration\\s*:\\s*(\\S*)\\b")
    }
    private val textShadowPattern by lazy {
        Pattern.compile("(?:\\s+|\\A)text-shadow\\s*:\\s*((\\S*\\s*){3,})\\b")
    }



    fun convert():AnnotatedString{
        try {
            return builder.toAnnotatedString()
        }catch (e:Exception){
            e.printStackTrace()
        }
        return buildAnnotatedString {  }
    }

    override fun setDocumentLocator(locator: Locator?) {}

    override fun startDocument() {}

    override fun endDocument() {}

    override fun startPrefixMapping(prefix: String?, uri: String?) {}

    override fun endPrefixMapping(prefix: String?) {}

    override fun startElement(uri: String?, localName: String?, qName: String?, atts: Attributes?) {
        if (localName != null && atts != null) {
            handleStartTag(localName, atts)
        }
    }

    override fun endElement(uri: String?, localName: String?, qName: String?) {
        if (localName != null) {
            handleEndTag(localName)
        }
    }

    override fun characters(ch: CharArray?, start: Int, length: Int) {
        if (ch != null) {
            val sb = StringBuilder()
            /*
             * 忽略空格后的其他空格，忽略换行符
             */
            for (i in 0 until length) {
                val c = ch[i + start]
                if (c == '\n') {

                } else if (c == ' ') {
                    val len = sb.length
                    val pred = if (len == 0) {
                        'a'
                    } else {
                        sb[len - 1]
                    }
                    if (pred != ' ') {
                        newLine = false
                        sb.append(' ')
                    }
                } else {
                    newLine = false
                    sb.append(c)
                }
            }
            builder.append(sb.toString())
        }
    }

    override fun ignorableWhitespace(ch: CharArray?, start: Int, length: Int) {
    }

    override fun processingInstruction(target: String?, data: String?) {
    }

    override fun skippedEntity(name: String?) {
    }

    /**
     * 连着的换行标签只保留一个
     * text-indent 首行缩进
     */
    private fun handleStartTag(tag: String,atts: Attributes){
        if (tag.equals("br", ignoreCase = true)) {
            // We don't need to handle this. TagSoup will ensure that there's a </br> for each <br>
            // so we can safely emit the linebreaks when we handle the close tag.
        } else if (tag.equals("p", ignoreCase = true)) {
            startBlockElement(atts,1)
        } else if (tag.equals("ol",true)) {
            startListElement(OL,atts)
        } else if (tag.equals("ul", ignoreCase = true)) {
            startListElement(UL,atts)
        } else if (tag.equals("li", ignoreCase = true)) {
            startLi(atts)
        } else if (tag.equals("div", ignoreCase = true)) {
            startBlockElement(atts,2)
        } else if (tag.equals("span", ignoreCase = true)) {
            startBlockElement(atts,0)
        } else if (tag.equals("strong", ignoreCase = true)) {
            //把文本定义为语气更强的强调的内容。通常是用加粗的字体
            spanStyle = SpanStyle(fontWeight = FontWeight.Bold)
            startBlockElement(atts,0)
        } else if (tag.equals("b", ignoreCase = true)) {
            //标签规定粗体文本
            spanStyle = SpanStyle(fontWeight = FontWeight.Bold)
            startBlockElement(atts,0)
        } else if (tag.equals("em", ignoreCase = true)) {
            //把文本定义为强调的内容。 一般用斜体字来
            spanStyle = SpanStyle(fontStyle = FontStyle.Italic)
            handleStyle()
        } else if (tag.equals("cite", ignoreCase = true)) {
            //按照惯例，引用的文本将以斜体显示
            spanStyle = SpanStyle(fontStyle = FontStyle.Italic)
            handleStyle()
        } else if (tag.equals("dfn", ignoreCase = true)) {
            //通常用斜体来显示
            spanStyle = SpanStyle(fontStyle = FontStyle.Italic)
            handleStyle()
        } else if (tag.equals("i", ignoreCase = true)) {
            spanStyle = SpanStyle(fontStyle = FontStyle.Italic)
            handleStyle()
        } else if (tag.equals("big", ignoreCase = true)) {
//            spanStyle?.apply {
//                this.fontSize.times(1.2f)
//            }
        } else if (tag.equals("small", ignoreCase = true)) {
        } else if (tag.equals("font", ignoreCase = true)) {
//            startFont(mSpannableStringBuilder, attributes)
        } else if (tag.equals("blockquote", ignoreCase = true)) {
            //<blockquote> 与 </blockquote> 之间的所有文本都会从常规文本中分离出来，经常会在左、右两边进行缩进（增加外边距），而且有时会使用斜体
            //左边缩进 使用斜体
            spanStyle = SpanStyle(fontStyle = FontStyle.Italic)
            if (canAddParagraphStyle) {
                paragraphStyle = ParagraphStyle(textIndent = TextIndent(16.sp, 16.sp))
            }
            startBlockElement(atts,0)
        } else if (tag.equals("tt", ignoreCase = true)) {
            //打印机字体
        } else if (tag.equals("a", ignoreCase = true)) {
            startA(atts)
        } else if (tag.equals("u", ignoreCase = true)) {
            spanStyle = SpanStyle(textDecoration = TextDecoration.Underline)
            startBlockElement(atts,0)
        } else if (tag.equals("del", ignoreCase = true)) {
            spanStyle = SpanStyle(textDecoration = TextDecoration.LineThrough)
            startBlockElement(atts,0)
        } else if (tag.equals("ins",true)) {
            spanStyle = SpanStyle(textDecoration = TextDecoration.Underline)
            startBlockElement(atts,0)
        } else if (tag.equals("s", ignoreCase = true)) {
            spanStyle = SpanStyle(textDecoration = TextDecoration.LineThrough)
            startBlockElement(atts,0)
        } else if (tag.equals("strike", ignoreCase = true)) {
            spanStyle = SpanStyle(textDecoration = TextDecoration.LineThrough)
            startBlockElement(atts,0)
        } else if (tag.equals("sup", ignoreCase = true)) {
            spanStyle = SpanStyle(fontSize = 10.sp, baselineShift = BaselineShift(0.4f))
            startBlockElement(atts,0)
        } else if (tag.equals("sub", ignoreCase = true)) {
            spanStyle = SpanStyle(fontSize = 10.sp, baselineShift = BaselineShift(-0.2f))
            startBlockElement(atts,0)
        } else if (tag.length == 2 && tag[0].lowercaseChar() == 'h'
            && tag[1] >= '1' && tag[1] <= '6') {
            startHeading(atts, tag[1] - '0')
        } else if (tag.equals("img", ignoreCase = true)) {
            handleImg(atts)
        } /*else if (mTagHandler != null) {
            mTagHandler.handleTag(true, tag, mSpannableStringBuilder, mReader)
        }*/
    }

    private fun handleEndTag(tag: String) {
        if (tag.equals("br", ignoreCase = true)) {
            builder.append("\n")
//            android.text.HtmlToSpannedConverter.handleBr(mSpannableStringBuilder)
        } else if (tag.equals("p", ignoreCase = true)) {
//            android.text.HtmlToSpannedConverter.endCssStyle(mSpannableStringBuilder)
            endBlockElement()
        } else if (tag.equals("ol", ignoreCase = true)) {
            htmlList.removeLastOrNull()
            endBlockElement()
        } else if (tag.equals("ul", ignoreCase = true)) {
            htmlList.removeLastOrNull()
            endBlockElement()
        } else if (tag.equals("li", ignoreCase = true)) {
            endBlockElement()
        } else if (tag.equals("div", ignoreCase = true)) {
            endBlockElement()
        } else if (tag.equals("span", ignoreCase = true)) {
           endBlockElement()
        } else if (tag.equals("strong", ignoreCase = true)) {
            endBlockElement()
        } else if (tag.equals("b", ignoreCase = true)) {
            endBlockElement()
        } else if (tag.equals("em", ignoreCase = true)) {
            endBlockElement()
        } else if (tag.equals("cite", ignoreCase = true)) {
            endBlockElement()
        } else if (tag.equals("dfn", ignoreCase = true)) {
            endBlockElement()
        } else if (tag.equals("i", ignoreCase = true)) {
            endBlockElement()
        } else if (tag.equals("big", ignoreCase = true)) {
        } else if (tag.equals("small", ignoreCase = true)) {
        } else if (tag.equals("font", ignoreCase = true)) {
//            android.text.HtmlToSpannedConverter.endFont(mSpannableStringBuilder)
        } else if (tag.equals("blockquote", ignoreCase = true)) {
//            android.text.HtmlToSpannedConverter.endBlockquote(mSpannableStringBuilder)
        } else if (tag.equals("tt", ignoreCase = true)) {
        } else if (tag.equals("a", ignoreCase = true)) {
            endBlockElement()
        } else if (tag.equals("u", ignoreCase = true)) {
            endBlockElement()
        } else if (tag.equals("del", ignoreCase = true)) {
            endBlockElement()
        } else if (tag.equals("ins",true)) {
            endBlockElement()
        } else if (tag.equals("s", ignoreCase = true)) {
            endBlockElement()
        } else if (tag.equals("strike", ignoreCase = true)) {
            endBlockElement()
        } else if (tag.equals("sup", ignoreCase = true)) {
            endBlockElement()
        } else if (tag.equals("sub", ignoreCase = true)) {
            endBlockElement()
        } else if (tag.length == 2 && tag[0].lowercaseChar() == 'h'
            && tag[1] >= '1' && tag[1] <= '6') {
            endBlockElement()
            newLine = true
            builder.append('\n')
            builder.append('\n')
        } /*else if (mTagHandler != null) {
            mTagHandler.handleTag(false, tag, mSpannableStringBuilder, mReader)
        }*/
    }

    private fun startBlockElement(attributes: Attributes, margin: Int) {
        handleAttributes(attributes)
        if (canAddParagraphStyle) {
            paragraphStyle?.apply {
                newLine = true
            }
        }
        if (!newLine && margin > 0) {
            builder.append('\n')
            newLine = true
        }
        handleStyle()
    }

    /**
     * ol/ul
     */
    private fun startListElement(name:String,attributes: Attributes){
        //1 A a I i
        var type = if (name.equals(OL,true)) "1" else "circle"
        attributes.getValue("", "type")?.apply {
            type = this
        }
        val style = attributes.getValue("", "style")
        if (style != null) {
            val m: Matcher = listStyleTypePattern.matcher(style)
            if (m.find()) {
                m.group(1)?.apply {
                    type = this
                }
            }
        }
        var start = 0
        attributes.getValue("", "start")?.apply {
            try {
                start = this.toInt() - 1
            } catch (e:Exception){}

        }
        htmlList.add(HtmlList(name,type,start))
        if (canAddParagraphStyle) {
            paragraphStyle = ParagraphStyle(textIndent = TextIndent(20.sp, 32.sp))
        }
        startBlockElement(attributes,1)
    }

    private fun startLi(attributes: Attributes) {
        startBlockElement(attributes,1)
        htmlList.lastOrNull()?.apply {
            val symbol = getLiSymbol(this)
            builder.append(symbol)
            number+=1
        }
    }

    private fun handleImg(attributes: Attributes) {
        attributes.getValue("", "src")?.apply {
            spanStyle = SpanStyle(fontSize = 30.sp)
            startBlockElement(attributes,1)
            builder.append("￼")
            endBlockElement()
        }

    }

    private fun handleStyle() {
        val styleIndex = arrayListOf<Int>()
        var hasParagraphStyle = false
        if (canAddParagraphStyle) {
            paragraphStyle?.apply {
                newLine = true
                val index = builder.pushStyle(this)
                styleIndex.add(index)
                hasParagraphStyle = true
                canAddParagraphStyle = false
            }
        }
        spanStyle?.apply {
            val index = builder.pushStyle(this)
            styleIndex.add(index)
        }
        styleIndexStack.push(ElementInfo(styleIndex,hasParagraphStyle))
        spanStyle = null
    }

    private fun endBlockElement() {
        try {
            if (!styleIndexStack.isEmpty()) {
                val elementInfo = styleIndexStack.pop()
                try {
                    elementInfo.styleIndex?.reversed()?.forEach { index ->
                        builder.pop(index)
                    }
                }catch (e:Exception){
                    e.printStackTrace()
                }
                if (elementInfo.hasParagraphStyle){
                    paragraphStyle = null
                    canAddParagraphStyle = true
                }
            }
        } catch (e:Exception){
            e.printStackTrace()
        }
    }

    private fun startHeading(attributes: Attributes, margin: Int) {
        if (!newLine && builder.length > 0) {
            builder.append('\n')
            builder.append('\n')
            newLine = true
        } else {
            builder.append('\n')
        }
        when (margin) {
            1 -> spanStyle = h1
            2 -> spanStyle = h2
            3 -> spanStyle = h3
            4 -> spanStyle = h4
            5 -> spanStyle = h5
            6 -> spanStyle = h6
        }
        startBlockElement(attributes, 0)
    }

    private fun startA(attributes: Attributes) {
        attributes.getValue("", "href")?.apply {
            spanStyle = SpanStyle(color = Color.Blue,textDecoration = TextDecoration.Underline)
        }
        startBlockElement(attributes,0)
    }

    private fun handleAttributes(attributes: Attributes) {
        val align = attributes.getValue("", "align")
        getTextAlign(align)?.apply {
            paragraphStyle = paragraphStyle?.copy(textAlign = this)?:ParagraphStyle(textAlign = this)
        }
        val styles = attributes.getValue("", "style")
        styles?.split(";")?.forEach { style ->
            handleAlign(style)
            handleColor(style)
            handleBackgroundColor(style)
            handleDecoration(style)
            handleShadow(style)
        }
    }

    private fun handleShadow(style: String) {
        val s = textShadowPattern.matcher(style)
        if (s.find()) {
            s.group(1)?.apply {
                var x = 0f
                var y = 0f
                var radius = 0.01f
                var sColor = Color.Unspecified
                this.split(",").forEach { shadow ->
                    shadow.split(" ").forEachIndexed{index,value ->
                        println("index:$index value:$value")
                        try {
                            if (value.endsWith("px",true)){
                                val v = value.lowercase()
                                    .replace("px","").toFloat()
                                when (index) {
                                    0 -> x = v
                                    1 -> y = v
                                    2 -> radius = v
                                }
                            } else {
                                sColor = getColor(value)
                            }
                        }catch (e:Exception){
                            e.printStackTrace()
                        }
                    }
                }
                val shadow =  Shadow(color = sColor, offset = Offset(x,y),blurRadius = radius)
                spanStyle = spanStyle?.copy(shadow = shadow)?:SpanStyle(shadow = shadow)
            }
        }
    }

    private fun handleDecoration(style: String) {
        val d = textDecorationPattern.matcher(style)
        if (d.find()) {
            d.group(1)?.apply {
                //none	underline overline line-through	 blink inherit
                spanStyle = if (this.equals("underline",true)) {
                    spanStyle?.copy(textDecoration = TextDecoration.Underline)
                        ?: SpanStyle(textDecoration = TextDecoration.Underline)
                } else if (this.equals("line-through",true)) {
                    spanStyle?.copy(textDecoration = TextDecoration.LineThrough)
                        ?: SpanStyle(textDecoration = TextDecoration.LineThrough)
                } else {
                    spanStyle?.copy(textDecoration = TextDecoration.None)
                        ?: SpanStyle(textDecoration = TextDecoration.None)
                }
            }
        }
    }

    private fun handleBackgroundColor(style: String) {
        val b = textBackgroundPattern.matcher(style)
        if (b.find()) {
            val bc = b.group(1)
            val backgroundColor = getColor(bc)
            spanStyle = spanStyle?.copy(background = backgroundColor)
                ?: SpanStyle(background = backgroundColor)
        }
    }

    private fun handleColor(style: String) {
        val c = textColorPattern.matcher(style)
        if (c.find()) {
            val color = c.group(1)
            val fontColor = getColor(color)
            spanStyle = spanStyle?.copy(color = fontColor) ?: SpanStyle(color = fontColor)
        }
    }

    private fun handleAlign(style: String) {
        val a: Matcher = textAlignPattern.matcher(style)
        if (a.find()) {
            val alignment = a.group(1)
            getTextAlign(alignment)?.apply {
                paragraphStyle =
                    paragraphStyle?.copy(textAlign = this) ?: ParagraphStyle(textAlign = this)
            }
        }
    }

    private fun getTextAlign(alignment:String?):TextAlign? {
        if (alignment != null) {
            if (alignment.equals("left",true) ||
                alignment.equals("start",true)){
                return TextAlign.Start
            } else if (alignment.equals("right",true) ||
                alignment.equals("end",true)){
                return TextAlign.End
            } else if (alignment.equals("center",true)) {
                return TextAlign.Center
            } else if (alignment.equals("justify",true)) {
                return TextAlign.Justify
            }
        }
        return null
    }

    private fun getLiSymbol(htmlList: HtmlList):String {
        when(htmlList.tag) {
            OL -> {
                //1 A a
                return if (htmlList.type.equals("lower-alpha",true) ||
                    htmlList.type == "a") {
                    val builder = StringBuilder()
                    alphaSymbol(builder,97,97+26,htmlList.start,htmlList.number)
                    builder.append(". ")
                    builder.toString()
                } else if (htmlList.type.equals("upper-alpha",true) ||
                    htmlList.type == "A") {
                    val builder = StringBuilder()
                    alphaSymbol(builder,65,65+26,htmlList.start,htmlList.number)
                    builder.append(". ")
                    builder.toString()
                } else {
                    "${htmlList.number + htmlList.start + 1}. "
                }
            }
            UL -> {
                return if (htmlList.type.equals("square",true)) {
                    "▪ "
                } else {
                    "• "
                }
            }
        }
        return ""
    }

    private fun alphaSymbol(builder: StringBuilder, firstSymbol: Int, maxSymbol: Int,
        start: Int, number: Int) {
        if (maxSymbol > 256 || firstSymbol > 256) return
        if (number + firstSymbol + start < maxSymbol) {
            builder.insert(0, (firstSymbol + number + start).toChar())
        } else {
            val a = (number + start) % 26
            builder.insert(0, (firstSymbol + a ).toChar())
            val b = (number + start)  / 26 - 1
            alphaSymbol(builder, firstSymbol, maxSymbol, 0, b)
        }
    }

    private fun getColor(cssColor:String?):Color{
        if (cssColor != null) {
            return if (cssColor.startsWith("#")){
                getColorHex(cssColor)
            } else if (cssColor.startsWith("rgb",true) ||
                cssColor.startsWith("rgba",true)){
                getColorRgb(cssColor)
            } else if (cssColor.startsWith("hsl",true) ||
                cssColor.startsWith("hsla",true)){
                getColorHsl(cssColor)
            } else {
                getColorByName(cssColor)
            }
        }
        return Color.Unspecified
    }

    private fun getColorByName(cssColor:String):Color{
        if (cssColor.equals("black",true)){
            return Color.Black
        } else if (cssColor.equals("blue",true)){
            return Color.Blue
        } else if (cssColor.equals("aqua",true)){
            return Color.Cyan
        } else if (cssColor.equals("fuchsia",true)){
            return Color.Magenta
        } else if (cssColor.equals("gray",true)){
            return Color(0xff808080)
        } else if (cssColor.equals("green",true)){
            return Color(0xff008000)
        } else if (cssColor.equals("lime",true)){
            return Color.Green
        } else if (cssColor.equals("cyan",true)) {
            return Color.Cyan
        } else if (cssColor.equals("maroon",true)){
            return Color(0xff800000)
        } else if (cssColor.equals("navy",true)){
            return Color(0xff000080)
        } else if (cssColor.equals("olive",true)){
            return Color(0xff808000)
        } else if (cssColor.equals("orange",true)){
            return Color(0xffffa500)
        } else if (cssColor.equals("purple",true)){
            return Color(0xff800080)
        } else if (cssColor.equals("red",true)){
            return Color.Red
        } else if (cssColor.equals("white",true)){
            return Color.White
        } else if (cssColor.equals("silver",true)){
            return Color(0xffc0c0c0)
        } else if (cssColor.equals("yellow",true)){
            return Color.Yellow
        } else if (cssColor.equals("AliceBlue",true)){
            return Color(0xfff0f8ff)
        } else if (cssColor.equals("AntiqueWhite",true)){
            return Color(0xfffaebd7)
        } else if (cssColor.equals("Aquamarine",true)){
            return Color(0xff7fffd4)
        } else if (cssColor.equals("Azure",true)){
            return Color(0xfff0ffff)
        } else if (cssColor.equals("Beige",true)){
            return Color(0xfff5f5dc)
        } else if (cssColor.equals("Bisque",true)){
            return Color(0xffffe4c4)
        } else if (cssColor.equals("BlanchedAlmond",true)){
            return Color(0xffffebcd)
        } else if (cssColor.equals("BlueViolet",true)){
            return Color(0xff8a2be2)
        } else if (cssColor.equals("Brown",true)){
            return Color(0xffa52a2a)
        } else if (cssColor.equals("BurlyWood",true)){
            return Color(0xffdeb887)
        } else if (cssColor.equals("CadetBlue",true)){
            return Color(0xff5f9ea0)
        } else if (cssColor.equals("Chartreuse",true)){
            return Color(0xff7fff00)
        } else if (cssColor.equals("Chocolate",true)){
            return Color(0xffd2691e)
        } else if (cssColor.equals("Coral",true)){
            return Color(0xffff7f50)
        } else if (cssColor.equals("CornflowerBlue",true)){
            return Color(0xff6495ed)
        } else if (cssColor.equals("Cornsilk",true)){
            return Color(0xfffff8dc)
        } else if (cssColor.equals("Crimson",true)){
            return Color(0xffdc143c)
        } else if (cssColor.equals("DarkBlue",true)){
            return Color(0xff00008b)
        } else if (cssColor.equals("DarkCyan",true)){
            return Color(0xff008b8b)
        } else if (cssColor.equals("DarkGoldenRod",true)){
            return Color(0xffb8860b)
        } else if (cssColor.equals("DarkGray",true)){
            return Color(0xffa9a9a9)
        } else if (cssColor.equals("DarkGreen",true)){
            return Color(0xff006400)
        } else if (cssColor.equals("DarkKhaki",true)){
            return Color(0xffbdb76b)
        } else if (cssColor.equals("DarkMagenta",true)){
            return Color(0xff8b008b)
        } else if (cssColor.equals("DarkOliveGreen",true)){
            return Color(0xff556b2f)
        } else if (cssColor.equals("DarkOrange",true)){
            return Color(0xffff8c00)
        } else if (cssColor.equals("DarkOrchid",true)){
            return Color(0xff9932cc)
        } else if (cssColor.equals("DarkRed",true)){
            return Color(0xff8b0000)
        } else if (cssColor.equals("DarkSalmon",true)){
            return Color(0xffe9967a)
        } else if (cssColor.equals("DarkSeaGreen",true)){
            return Color(0xff8fbc8f)
        } else if (cssColor.equals("DarkSlateBlue",true)){
            return Color(0xff483d8b)
        } else if (cssColor.equals("DarkSlateGray",true)){
            return Color(0xff2f4f4f)
        } else if (cssColor.equals("DarkTurquoise",true)){
            return Color(0xff00ced1)
        } else if (cssColor.equals("DarkViolet",true)){
            return Color(0xff9400d3)
        } else if (cssColor.equals("DeepPink",true)){
            return Color(0xffff1493)
        } else if (cssColor.equals("DeepSkyBlue",true)){
            return Color(0xff00bfff)
        }else if (cssColor.equals("DimGray",true)){
            return Color(0xff696969)
        }else if (cssColor.equals("DodgerBlue",true)){
            return Color(0xff1e90ff)
        }else if (cssColor.equals("FireBrick",true)){
            return Color(0xffb22222)
        }else if (cssColor.equals("FloralWhite",true)){
            return Color(0xfffffaf0)
        } else if (cssColor.equals("ForestGreen",true)){
            return Color(0xff228b22)
        }else if (cssColor.equals("Gainsboro",true)){
            return Color(0xffdcdcdc)
        }else if (cssColor.equals("GhostWhite",true)){
            return Color(0xfff8f8ff)
        }else if (cssColor.equals("Gold",true)){
            return Color(0xffffd700)
        }else if (cssColor.equals("GoldenRod",true)){
            return Color(0xffdaa520)
        }else if (cssColor.equals("GreenYellow",true)){
            return Color(0xffadff2f)
        }else if (cssColor.equals("HoneyDew",true)){
            return Color(0xfff0fff0)
        }else if (cssColor.equals("HotPink",true)){
            return Color(0xffff69b4)
        }else if (cssColor.equals("IndianRed",true)){
            return Color(0xffcd5c5c)
        }else if (cssColor.equals("Indigo",true)){
            return Color(0xff4b0082)
        }else if (cssColor.equals("Ivory",true)){
            return Color(0xfffffff0)
        }else if (cssColor.equals("Khaki",true)){
            return Color(0xfff0e68c)
        }else if (cssColor.equals("Lavender",true)){
            return Color(0xffe6e6fa)
        }else if (cssColor.equals("LavenderBlush",true)){
            return Color(0xfffff0f5)
        }else if (cssColor.equals("LawnGreen",true)){
            return Color(0xff7cfc00)
        }else if (cssColor.equals("LemonChiffon",true)){
            return Color(0xfffffacd)
        }else if (cssColor.equals("LightBlue",true)){
            return Color(0xffadd8e6)
        }else if (cssColor.equals("LightCoral",true)){
            return Color(0xfff08080)
        }else if (cssColor.equals("LightCyan",true)){
            return Color(0xffe0ffff)
        }else if (cssColor.equals("LightGoldenRodYellow",true)){
            return Color(0xfffafad2)
        }else if (cssColor.equals("LightGray",true)){
            return Color(0xffd3d3d3)
        }else if (cssColor.equals("LightGreen",true)){
            return Color(0xff90ee90)
        }else if (cssColor.equals("LightPink",true)){
            return Color(0xffffb6c1)
        }else if (cssColor.equals("LightSalmon",true)){
            return Color(0xffffa07a)
        }else if (cssColor.equals("LightSeaGreen",true)){
            return Color(0xff20b2aa)
        }else if (cssColor.equals("LightSkyBlue",true)){
            return Color(0xff87cefa)
        }else if (cssColor.equals("LightSlateGray",true)){
            return Color(0xff778899)
        }else if (cssColor.equals("LightSteelBlue",true)){
            return Color(0xffB0C4DE)
        }else if (cssColor.equals("LightYellow",true)){
            return Color(0xffFFFFE0)
        }else if (cssColor.equals("LimeGreen",true)){
            return Color(0xff32CD32)
        }else if (cssColor.equals("Linen",true)){
            return Color(0xffFAF0E6)
        }else if (cssColor.equals("Magenta",true)){
            return Color(0xffFF00FF)
        }else if (cssColor.equals("MediumAquaMarine",true)){
            return Color(0xff66CDAA)
        }else if (cssColor.equals("MediumBlue",true)){
            return Color(0xff0000CD)
        }else if (cssColor.equals("MediumOrchid",true)){
            return Color(0xffBA55D3)
        }else if (cssColor.equals("MediumPurple",true)){
            return Color(0xff9370DB)
        }else if (cssColor.equals("MediumSeaGreen",true)){
            return Color(0xff3CB371)
        }else if (cssColor.equals("MediumSlateBlue",true)){
            return Color(0xff7B68EE)
        }else if (cssColor.equals("MediumSpringGreen",true)){
            return Color(0xff00FA9A)
        }else if (cssColor.equals("MediumTurquoise",true)){
            return Color(0xff48D1CC)
        }else if (cssColor.equals("MediumVioletRed",true)){
            return Color(0xffC71585)
        }else if (cssColor.equals("MidnightBlue",true)){
            return Color(0xff191970)
        }else if (cssColor.equals("MintCream",true)){
            return Color(0xffF5FFFA)
        }else if (cssColor.equals("MistyRose",true)){
            return Color(0xffFFE4E1)
        }else if (cssColor.equals("Moccasin",true)){
            return Color(0xffFFE4B5)
        }else if (cssColor.equals("NavajoWhite",true)){
            return Color(0xffFFDEAD)
        }else if (cssColor.equals("OldLace",true)){
            return Color(0xffFDF5E6)
        }else if (cssColor.equals("OliveDrab",true)){
            return Color(0xff6B8E23)
        }else if (cssColor.equals("OrangeRed",true)){
            return Color(0xffFF4500)
        }else if (cssColor.equals("Orchid",true)){
            return Color(0xffDA70D6)
        }else if (cssColor.equals("PaleGoldenRod",true)){
            return Color(0xffEEE8AA)
        }else if (cssColor.equals("PaleGreen",true)){
            return Color(0xff98FB98)
        }else if (cssColor.equals("PaleTurquoise",true)){
            return Color(0xffAFEEEE)
        }else if (cssColor.equals("PaleVioletRed",true)){
            return Color(0xffDB7093)
        }else if (cssColor.equals("PapayaWhip",true)){
            return Color(0xffFFEFD5)
        }else if (cssColor.equals("PeachPuff",true)){
            return Color(0xffFFDAB9)
        }else if (cssColor.equals("Peru",true)){
            return Color(0xffCD853F)
        }else if (cssColor.equals("Pink",true)){
            return Color(0xffFFC0CB)
        }else if (cssColor.equals("Plum",true)){
            return Color(0xffDDA0DD)
        }else if (cssColor.equals("PowderBlue",true)){
            return Color(0xffB0E0E6)
        }else if (cssColor.equals("RosyBrown",true)){
            return Color(0xffBC8F8F)
        }else if (cssColor.equals("RoyalBlue",true)){
            return Color(0xff4169E1)
        }else if (cssColor.equals("SaddleBrown",true)){
            return Color(0xff8B4513)
        }else if (cssColor.equals("Salmon",true)){
            return Color(0xffFA8072)
        }else if (cssColor.equals("SandyBrown",true)){
            return Color(0xffF4A460)
        }else if (cssColor.equals("SeaGreen",true)){
            return Color(0xff2E8B57)
        }else if (cssColor.equals("SeaShell",true)){
            return Color(0xffFFF5EE)
        }else if (cssColor.equals("Sienna",true)){
            return Color(0xffA0522D)
        }else if (cssColor.equals("SkyBlue",true)){
            return Color(0xff87CEEB)
        }else if (cssColor.equals("SlateBlue",true)){
            return Color(0xff6A5ACD)
        }else if (cssColor.equals("SlateGray",true)){
            return Color(0xff708090)
        }else if (cssColor.equals("Snow",true)){
            return Color(0xffFFFAFA)
        }else if (cssColor.equals("SpringGreen",true)){
            return Color(0xff00FF7F)
        }else if (cssColor.equals("SteelBlue",true)){
            return Color(0xff4682B4)
        }else if (cssColor.equals("Tan",true)){
            return Color(0xffD2B48C)
        }else if (cssColor.equals("Teal",true)){
            return Color(0xff008080)
        }else if (cssColor.equals("Thistle",true)){
            return Color(0xffD8BFD8)
        }else if (cssColor.equals("Tomato",true)){
            return Color(0xffFF6347)
        }else if (cssColor.equals("Turquoise",true)){
            return Color(0xff40E0D0)
        }else if (cssColor.equals("Violet",true)){
            return Color(0xffEE82EE)
        }else if (cssColor.equals("Wheat",true)){
            return Color(0xffF5DEB3)
        }else if (cssColor.equals("WhiteSmoke",true)){
            return Color(0xffF5F5F5)
        }else if (cssColor.equals("YellowGreen",true)){
            return Color(0xff9ACD32)
        }else {
            return Color.Unspecified
        }
    }

    private fun getColorHex(hexColor:String):Color{
        val hex = hexColor.replace("#","").trim()
        when (hex.length) {
            3 -> {
                val r = "ff${hex[0]}${hex[0]}${hex[1]}${hex[1]}${hex[2]}${hex[2]}"
                return Color(r.toLong(16))
            }
            4 -> {
                val r = "${hex[3]}${hex[3]}${hex[0]}${hex[0]}${hex[1]}${hex[1]}${hex[2]}${hex[2]}"
                return Color(r.toLong(16))
            }
            6 -> {
                val r = "ff${hex[0]}${hex[1]}${hex[2]}${hex[3]}${hex[4]}${hex[5]}"
                return Color(r.toLong(16))
            }
            8 -> {
                val r = "${hex[6]}${hex[7]}${hex[0]}${hex[1]}${hex[2]}${hex[3]}${hex[4]}${hex[5]}"
                return Color(r.toLong(16))
            }
            else -> return Color.Unspecified
        }
    }

    private fun getColorRgb(rgbColor:String):Color {
        val rgb = rgbColor.lowercase()
            .replace("rgba","")
            .replace("rgb","")
            .trim()
            .replace("(","")
            .replace(")","")
        val rgbValue = rgb.split(",")
        when(rgbValue.size){
            3 -> {
                try {
                    val r = rgbValue[0].trim().toInt()
                    val g = rgbValue[1].trim().toInt()
                    val b = rgbValue[2].trim().toInt()
                    return Color(r,g,b)
                } catch (e:Exception){}
            }
            4 -> {
                try {
                    val r = rgbValue[0].trim().toInt()
                    val g = rgbValue[1].trim().toInt()
                    val b = rgbValue[2].trim().toInt()
                    val a = (rgbValue[3].trim().toFloat()*255).toInt()
                    return Color(r,g,b,a)
                } catch (e:Exception){
                    e.printStackTrace()
                }
            }
        }
        return Color.Unspecified
    }

    private fun getColorHsl(hslColor:String):Color {
        val hsl = hslColor.lowercase()
            .replace("hsla","")
            .replace("hsl","").trim()
            .replace("(","")
            .replace("%","")
            .replace(")","").trim()
        val hslValue = hsl.split(",")
        when(hslValue.size) {
            3 -> {
                try {
                    val hue = hslValue[0].trim().toFloat()
                    val saturation = hslValue[1].trim().toFloat()/100f
                    val lightness = hslValue[2].trim().toFloat()/100f
                    return Color.hsl(hue, saturation, lightness)
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }
            4 -> {
                try {
                    val hue = hslValue[0].trim().toFloat()
                    val saturation = hslValue[1].trim().toFloat()/100f
                    val lightness = hslValue[2].trim().toFloat()/100f
                    val alpha = hslValue[3].trim().toFloat()
                    return Color.hsl(hue, saturation, lightness,alpha)
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }
        }
        return Color.Unspecified
    }

}

/**
 * 列表标签
 * @property tag String  ol/ul
 * @property number Int li的个数
 */
data class HtmlList(
    val tag: String,
    val type:String,
    val start: Int = 0,
    var number:Int = 0
)

data class ElementInfo(
    val styleIndex:ArrayList<Int>?,
    val hasParagraphStyle:Boolean = false
)


fun main(){
    val str = "text-shadow:5px 5px 5px #FF0000,-5px -5px 5px #00ff00;color:red"
    val pattern =  Pattern.compile("(?:\\s+|\\A)text-shadow\\s*:\\s*((\\S*\\s*){3,})\\b")
    val s = pattern.matcher(str)
    if (s.find()) {
        println("size:${s.groupCount()}")
        s.group(0)?.apply {
            println("shadow:${this}")
        }
        s.group(1)?.apply {
            println("shadow:${this}")
        }
    }
}