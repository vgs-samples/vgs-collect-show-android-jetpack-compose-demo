package com.verygoodsecurity.compose

import android.os.Bundle
import android.util.TypedValue
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.setPadding
import com.verygoodsecurity.compose.ui.AppTheme
import com.verygoodsecurity.vgscollect.core.HTTPMethod
import com.verygoodsecurity.vgscollect.core.VGSCollect
import com.verygoodsecurity.vgscollect.core.VgsCollectResponseListener
import com.verygoodsecurity.vgscollect.view.InputFieldView
import com.verygoodsecurity.vgscollect.widget.ExpirationDateEditText
import com.verygoodsecurity.vgscollect.widget.VGSCardNumberEditText
import com.verygoodsecurity.vgscollect.widget.VGSTextInputLayout
import com.verygoodsecurity.vgsshow.VGSShow
import com.verygoodsecurity.vgsshow.core.listener.VGSOnResponseListener
import com.verygoodsecurity.vgsshow.core.network.client.VGSHttpMethod
import com.verygoodsecurity.vgsshow.core.network.model.VGSRequest
import com.verygoodsecurity.vgsshow.widget.VGSTextView
import com.verygoodsecurity.vgsshow.widget.core.VGSView
import org.json.JSONException
import org.json.JSONObject

class CollectAndShowActivity : AppCompatActivity(), VgsCollectResponseListener, VGSOnResponseListener {

    private val collect: VGSCollect by lazy {
        VGSCollect.Builder(this, "<VAULT_ID>").create()
    }

    private val show: VGSShow by lazy {
        VGSShow.Builder(this, "<VAULT_ID>").build()
    }

    private var cardNumberToken: MutableState<String> = mutableStateOf("")
    private var expirationDateToken: MutableState<String> = mutableStateOf("")
    private var isCollectingData: MutableState<Boolean> = mutableStateOf(false)
    private var isRevealingData: MutableState<Boolean> = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Scaffold(topBar = {
                    TopAppBar(
                        title = { Text(text = "Compose with VGSCollect/Show SDK") },
                        elevation = 12.dp
                    )
                }) {
                    Row {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Collect {
                                collect.bindView(it)
                            }
                        }
                        Divider(
                            color = Color.Black,
                            modifier = Modifier
                                .width(Dp(1f))
                                .fillMaxHeight(),
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Show {
                                show.subscribe(it)
                            }
                        }
                    }
                }
            }
        }

        collect.addOnResponseListeners(this)
        show.addOnResponseListener(this)
    }

    override fun onResponse(response: CollectResponse?) {
        isCollectingData.value = false
        try {
            val json = when (response) {
                is CollectResponseSuccess -> JSONObject(response?.rawResponse)
                else -> null
            }

            parseNumberAlias(json)
            parseDateAlias(json)
        } catch (e: JSONException) {
        }
    }

    override fun onResponse(response: ShowResponse) {
        isRevealingData.value = false
    }

    private fun submitData() {
        isCollectingData.value = true
        collect.asyncSubmit("/post", HTTPMethod.POST)
    }

    private fun revealData() {
        isRevealingData.value = true
        show.requestAsync(
            VGSRequest.Builder("post", VGSHttpMethod.POST).body(
                mapOf(
                    "payment_card_number" to cardNumberToken.value,
                    "payment_card_expiration_date" to expirationDateToken.value
                )
            ).build()
        )
    }

    private fun parseDateAlias(json: JSONObject?) {
        json?.let {
            if (it.has("json") && it.getJSONObject("json").has("expDate")) {
                it.getJSONObject("json").getString("expDate").let { date ->
                    expirationDateToken.value = date
                }
            }
        }
    }

    private fun parseNumberAlias(json: JSONObject?) {
        json?.let {
            if (it.has("json") && it.getJSONObject("json").has("cardNumber")) {
                it.getJSONObject("json").getString("cardNumber").let { number ->
                    cardNumberToken.value = number
                }
            }
        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun Collect(onViewAdded: (InputFieldView) -> Unit) {
        AnimatedVisibility(
            visible = isCollectingData.value,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            LinearProgressIndicator()
        }
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                VGSTextInputLayout(context).apply {
                    setBoxCornerRadius(
                        8f,
                        8f,
                        8f,
                        8f
                    )
                    addView(VGSCardNumberEditText(context).apply {
                        setFieldName("cardNumber")
                        setHint("Card number")
                        setPadding(16)
                        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
                        setDivider('-')
                        setText("4111111111111111")
                        onViewAdded.invoke(this)
                    })
                }
            },
        )
        Text(text = cardNumberToken.value, fontSize = 12.sp)
        AndroidView(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
            factory = { context ->
                VGSTextInputLayout(context).apply {
                    setBoxCornerRadius(
                        8f,
                        8f,
                        8f,
                        8f
                    )
                    addView(ExpirationDateEditText(context).apply {
                        setFieldName("expDate")
                        setHint("Expiration date")
                        setPadding(16)
                        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
                        setText("06/2022")
                        onViewAdded.invoke(this)
                    })
                }
            },
        )
        Text(text = expirationDateToken.value, fontSize = 12.sp)
        Button(
            onClick = { submitData() },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(text = "Submit")
        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun Show(onViewAdded: (VGSView<*>) -> Unit) {
        AnimatedVisibility(
            visible = isRevealingData.value,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            LinearProgressIndicator()
        }
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                VGSTextView(context).apply {
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, 18f)
                    setHint("Card number")
                    setContentPath("json.payment_card_number")
                    onViewAdded.invoke(this)
                }
            },
        )
        AndroidView(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
            factory = { context ->
                VGSTextView(context).apply {
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, 18f)
                    setContentPath("json.payment_card_expiration_date")
                    setHint("Expiration date")
                    onViewAdded.invoke(this)
                }
            },
        )
        Button(
            onClick = { revealData() },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(text = "Reveal")
        }
    }
}

typealias CollectResponse = com.verygoodsecurity.vgscollect.core.model.network.VGSResponse
typealias CollectResponseSuccess = com.verygoodsecurity.vgscollect.core.model.network.VGSResponse.SuccessResponse
typealias ShowResponse = com.verygoodsecurity.vgsshow.core.network.model.VGSResponse