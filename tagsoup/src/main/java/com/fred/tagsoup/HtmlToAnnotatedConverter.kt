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

    private val ol = "ol"
    private val ul = "ul"
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
                    //忽略换行
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
            startListElement(ol,atts)
        } else if (tag.equals("ul", ignoreCase = true)) {
            startListElement(ul,atts)
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
            //
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
            //
        } else if (tag.equals("small", ignoreCase = true)) {
            //
        } else if (tag.equals("font", ignoreCase = true)) {
//            android.text.HtmlToSpannedConverter.endFont(mSpannableStringBuilder)
        } else if (tag.equals("blockquote", ignoreCase = true)) {
//            android.text.HtmlToSpannedConverter.endBlockquote(mSpannableStringBuilder)
        } else if (tag.equals("tt", ignoreCase = true)) {
            //
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
        var type = if (name.equals(ol,true)) "1" else "circle"
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
            ol -> {
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
            ul -> {
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

    private fun getColorByName(cssColor: String): Color {
        when (cssColor.lowercase()) {
            "black" -> return Color.Black
            "blue" -> return Color.Blue
            "aqua" -> return Color.Cyan
            "fuchsia" -> return Color.Magenta
            "gray" -> return Color(0xff808080)
            "green" -> return Color(0xff008000)
            "lime" -> return Color.Green
            "cyan" -> return Color.Cyan
            "maroon" -> return Color(0xff800000)
            "navy" -> return Color(0xff000080)
            "olive" -> return Color(0xff808000)
            "orange" -> return Color(0xffffa500)
            "purple" -> return Color(0xff800080)
            "red" -> return Color.Red
            "white" -> return Color.White
            "silver" -> return Color(0xffc0c0c0)
            "yellow" -> return Color.Yellow
            "AliceBlue".lowercase() -> return Color(0xfff0f8ff)
            "AntiqueWhite".lowercase() -> return Color(0xfffaebd7)
            "aquamarine" -> return Color(0xff7fffd4)
            "azure" -> return Color(0xfff0ffff)
            "beige" -> return Color(0xfff5f5dc)
            "bisque" -> return Color(0xffffe4c4)
            "BlanchedAlmond".lowercase() -> return Color(0xffffebcd)
            "BlueViolet".lowercase() -> return Color(0xff8a2be2)
            "brown" -> return Color(0xffa52a2a)
            "BurlyWood".lowercase() -> return Color(0xffdeb887)
            "CadetBlue".lowercase() -> return Color(0xff5f9ea0)
            "chartreuse" -> return Color(0xff7fff00)
            "chocolate" -> return Color(0xffd2691e)
            "coral" -> return Color(0xffff7f50)
            "CornflowerBlue".lowercase() -> return Color(0xff6495ed)
            "CornSilk" -> return Color(0xfffff8dc)
            "crimson" -> return Color(0xffdc143c)
            "DarkBlue".lowercase() -> return Color(0xff00008b)
            "DarkCyan".lowercase() -> return Color(0xff008b8b)
            "DarkGoldenRod".lowercase() -> return Color(0xffb8860b)
            "DarkGray".lowercase() -> return Color(0xffa9a9a9)
            "DarkGreen".lowercase() -> return Color(0xff006400)
            "DarkKhaki".lowercase() -> return Color(0xffbdb76b)
            "DarkMagenta.lowercase()" -> return Color(0xff8b008b)
            "DarkOliveGreen".lowercase() -> return Color(0xff556b2f)
            "DarkOrange".lowercase() -> return Color(0xffff8c00)
            "DarkOrchid".lowercase() -> return Color(0xff9932cc)
            "DarkRed".lowercase() -> return Color(0xff8b0000)
            "DarkSalmon".lowercase() -> return Color(0xffe9967a)
            "DarkSeaGreen".lowercase() -> return Color(0xff8fbc8f)
            "DarkSlateBlue".lowercase() -> return Color(0xff483d8b)
            "DarkSlateGray".lowercase() -> return Color(0xff2f4f4f)
            "DarkTurquoise".lowercase() -> return Color(0xff00ced1)
            "DarkViolet".lowercase() -> return Color(0xff9400d3)
            "DeepPink".lowercase() -> return Color(0xffff1493)
            "DeepSkyBlue".lowercase() -> return Color(0xff00bfff)
            "DimGray".lowercase() -> return Color(0xff696969)
            "DodgerBlue".lowercase() -> return Color(0xff1e90ff)
            "FireBrick".lowercase() -> return Color(0xffb22222)
            "FloralWhite".lowercase() -> return Color(0xfffffaf0)
            "ForestGreen".lowercase() -> return Color(0xff228b22)
            "gainsboro" -> return Color(0xffdcdcdc)
            "GhostWhite".lowercase() -> return Color(0xfff8f8ff)
            "gold" -> return Color(0xffffd700)
            "GoldenRod".lowercase() -> return Color(0xffdaa520)
            "GreenYellow".lowercase() -> return Color(0xffadff2f)
            "HoneyDew".lowercase() -> return Color(0xfff0fff0)
            "HotPink".lowercase() -> return Color(0xffff69b4)
            "IndianRed".lowercase() -> return Color(0xffcd5c5c)
            "indigo" -> return Color(0xff4b0082)
            "ivory" -> return Color(0xfffffff0)
            "khaki" -> return Color(0xfff0e68c)
            "lavender" -> return Color(0xffe6e6fa)
            "LavenderBlush".lowercase() -> return Color(0xfffff0f5)
            "LawnGreen".lowercase() -> return Color(0xff7cfc00)
            "LemonChiffon".lowercase() -> return Color(0xfffffacd)
            "LightBlue".lowercase() -> return Color(0xffadd8e6)
            "LightCoral".lowercase() -> return Color(0xfff08080)
            "LightCyan".lowercase() -> return Color(0xffe0ffff)
            "LightGoldenRodYellow".lowercase() -> return Color(0xfffafad2)
            "LightGray".lowercase() -> return Color(0xffd3d3d3)
            "LightGreen".lowercase() -> return Color(0xff90ee90)
            "LightPink".lowercase() -> return Color(0xffffb6c1)
            "LightSalmon".lowercase() -> return Color(0xffffa07a)
            "LightSeaGreen".lowercase() -> return Color(0xff20b2aa)
            "LightSkyBlue".lowercase() -> return Color(0xff87cefa)
            "LightSlateGray".lowercase() -> return Color(0xff778899)
            "LightSteelBlue".lowercase() -> return Color(0xffB0C4DE)
            "LightYellow".lowercase() -> return Color(0xffFFFFE0)
            "LimeGreen".lowercase() -> return Color(0xff32CD32)
            "linen" -> return Color(0xffFAF0E6)
            "magenta" -> return Color(0xffFF00FF)
            "MediumAquaMarine".lowercase() -> return Color(0xff66CDAA)
            "MediumBlue".lowercase() -> return Color(0xff0000CD)
            "MediumOrchid".lowercase() -> return Color(0xffBA55D3)
            "MediumPurple".lowercase() -> return Color(0xff9370DB)
            "MediumSeaGreen".lowercase() -> return Color(0xff3CB371)
            "MediumSlateBlue".lowercase() -> return Color(0xff7B68EE)
            "MediumSpringGreen".lowercase() -> return Color(0xff00FA9A)
            "MediumTurquoise".lowercase() -> return Color(0xff48D1CC)
            "MediumVioletRed".lowercase() -> return Color(0xffC71585)
            "MidnightBlue".lowercase() -> return Color(0xff191970)
            "MintCream".lowercase() -> return Color(0xffF5FFFA)
            "MistyRose".lowercase() -> return Color(0xffFFE4E1)
            "moccasin" -> return Color(0xffFFE4B5)
            "NavajoWhite".lowercase() -> return Color(0xffFFDEAD)
            "OldLace".lowercase() -> return Color(0xffFDF5E6)
            "OliveDrab".lowercase() -> return Color(0xff6B8E23)
            "OrangeRed".lowercase() -> return Color(0xffFF4500)
            "orchid" -> return Color(0xffDA70D6)
            "PaleGoldenRod".lowercase() -> return Color(0xffEEE8AA)
            "PaleGreen".lowercase() -> return Color(0xff98FB98)
            "PaleTurquoise".lowercase() -> return Color(0xffAFEEEE)
            "PaleVioletRed".lowercase() -> return Color(0xffDB7093)
            "PapayaWhip".lowercase() -> return Color(0xffFFEFD5)
            "PeachPuff".lowercase() -> return Color(0xffFFDAB9)
            "peru" -> return Color(0xffCD853F)
            "pink" -> return Color(0xffFFC0CB)
            "plum" -> return Color(0xffDDA0DD)
            "PowderBlue".lowercase() -> return Color(0xffB0E0E6)
            "RosyBrown".lowercase() -> return Color(0xffBC8F8F)
            "RoyalBlue".lowercase() -> return Color(0xff4169E1)
            "SaddleBrown".lowercase() -> return Color(0xff8B4513)
            "salmon" -> return Color(0xffFA8072)
            "SandyBrown".lowercase() -> return Color(0xffF4A460)
            "SeaGreen".lowercase() -> return Color(0xff2E8B57)
            "SeaShell".lowercase() -> return Color(0xffFFF5EE)
            "sienna" -> return Color(0xffA0522D)
            "SkyBlue".lowercase() -> return Color(0xff87CEEB)
            "SlateBlue".lowercase() -> return Color(0xff6A5ACD)
            "SlateGray".lowercase() -> return Color(0xff708090)
            "snow" -> return Color(0xffFFFAFA)
            "SpringGreen".lowercase() -> return Color(0xff00FF7F)
            "SteelBlue".lowercase() -> return Color(0xff4682B4)
            "tan" -> return Color(0xffD2B48C)
            "teal" -> return Color(0xff008080)
            "thistle" -> return Color(0xffD8BFD8)
            "tomato" -> return Color(0xffFF6347)
            "turquoise" -> return Color(0xff40E0D0)
            "violet" -> return Color(0xffEE82EE)
            "wheat" -> return Color(0xffF5DEB3)
            "WhiteSmoke".lowercase() -> return Color(0xffF5F5F5)
            "YellowGreen".lowercase() -> return Color(0xff9ACD32)
            else -> return Color.Unspecified
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
