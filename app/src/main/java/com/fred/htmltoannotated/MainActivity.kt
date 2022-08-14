package com.fred.htmltoannotated

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import com.fred.tagsoup.htmlToAnnotatedString

class MainActivity : ComponentActivity() {

    private val html = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="generator" content="Hexo 4.2.0">
        </head>
        <h1 align="center">Privacy Policy</h1>
        <p>H<sub>2</sub>O</p>
        <p>x<sup>2</sup>+2y+y<sup>2</sup> = 15</p>
        <p>&quot;Z阅读&quot;尊重并保护所<del style="color:#ffaaff;background-color:red">有使用服务</del>用<ins style="color:AntiqueWhite">户的个人隐私权</ins>。为了给您提供更准确、更有个性化的服务，&quot;Z阅读&quot;会按照本隐私权政策的规定使用和披露您的个人信息。除本隐私权政策另有规定外，在未征得您事先许可的情况下，&quot;Z阅读&quot;不会将这些信息对外披露或向第三方提供。您在同意&quot;Z阅读&quot;服务使用协议之时，即视为您已经同意本隐私权政策全部内容。</p>
        <h5 style="color:rgB(230,12,222);background:#f23">1.适用范围</h5>
        <ul style="list-style-type:disc" >
            <li><p>
                在您使用本应<s style="color:Rgba(230,12,222,0.3)">用网络服务，</s>或访<u style="color:#30ff2030">问本应用平台网</u>页时，<a>本应用</a>自动接收并记录的您的浏览器和计算机上的信息，包括但不限于您的IP地址、浏览器的类型、使用的语言、访问日期和时间、软硬件特征信息及您需求的网页记录等数据；</p>
            </li>
            <li><p><a href="https://123456">本应用</a>通过合法途径从商业伙伴处取得的用户个人数据。 </p></li>
            <li><p style="color:hsl(240,100%,50%)">您了解并同意，以下信息不适用本隐私权政策：</p>
                <ol>
                    <li style="color:#f32;text-shadow:5.5px 5px 5px #FF0000,-5px -5px 5px #00ff00">您在使用本应用平台提供的搜索服务时输入的关键字信息；</li>
                    <li style="color:#aa28;text-shadow:5px 5px #ff0012">违反法律规定或违反本应用规则行为及本应用已对您采取的措施。</li>
                </ol>
            </li>
        </ul>
        <img src="https://8562cbd3"/>
        <h5>2.信息使用</h5>
        <ol type="1">
            <li>本应用不会向任何无关第三方提供、出售、出租、分享或交易您的个人信息，除非事先得到您的许可，或该第三方和本应用（含本应用关联公司）单独或共同为您提供服务，且在该服务结束后，其将被禁止访问包括其以前能够访问的所有这些资料。</li>
            <li>本应用亦不允许任何第三方以任何手段收集、编辑、出售或者无偿传播您的个人信息。任何本应用平台用户如从事上述活动，一经发现，本应用有权立即终止与该用户的服务协议。</li>
            <li>为服务用户的目的，本应用可能通过使用您的个人信息，向您提供您感兴趣的信息，包括但不限于向您发出产品和服务信息，或者与本应用合作伙伴共享信息以便他们向您发送有关其产品和服务的信息（后者需要您的事先同意）。</li>
        </ol>
        <h5>3.信息披露</h5>
        <p>在如下情况下，本应用将依据您的个人意愿或法律的规定全部或部分的披露您的个人信息： </p>
        <ol style="list-style-type:lower-alpha">
            <li>经您事先同意，向第三方披露；</li>
            <li>为提供您所要求的产品和服务，而必须和第三方分享您的个人信息；</li>
            <li>根据法律的有关规定，或者行政或司法机构的要求，向第三方或者行政、司法机构披露；</li>
            <li>如您出现违反中国有关法律、法规或者本应用服务协议或相关规则的情况，需要向4.第三方披露；</li>
            <li>如您是适格的知识产权投诉人并已提起投诉，应被投诉人要求，向被投诉人披露，以便双方处理可能的权利纠纷；</li>
            <li>在本应用平台上创建的某一交易中，如交易任何一方履行或部分履行了交易义务并提出信息披露请求的，本应用有权决定向该用户提供其交易对方的联络方式等必要信息，以促成交易的完成或纠纷的解决。</li>
            <li>其它本应用根据法律、法规或者网站政策认为合适的披露。</li>
        </ol>
        <h5>4.本隐私政策的更改</h5>
        <ol>
            <li>如果决定更改隐私政策，我们会在本政策中、以及我们认为适当的位置发布这些更改，以便您了解我们如何收集、使用您的个人信息，哪些人可以访问这些信息，以及在什么情况下我们会透露这些信息。
            </li>
            <li>本人保留随时修改本政策的权利，因此请经常查看。</li>
        </ol>
        </html>
    """.trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent{
            Text(text = htmlToAnnotatedString(html),
                modifier = Modifier.verticalScroll(rememberScrollState()))
        }
    }

    @Preview
    @Composable
    private fun testHtml(){
        Text(text = htmlToAnnotatedString(html))
    }



}