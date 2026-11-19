package com.example.ezanvakti.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.example.ezanvakti.AppConfig
import com.example.ezanvakti.BuildConfig
import com.example.ezanvakti.i18n.Dil
import com.example.ezanvakti.i18n.t
import com.example.ezanvakti.model.SelectedLocation

/** Geri bildirim metninin üst sınırı (karakter). */
const val GERI_BILDIRIM_SINIR = 100

/**
 * Geri bildirim penceresi: kullanıcı en fazla [GERI_BILDIRIM_SINIR] karakter yazar, "Gönder" telefonun
 * e-posta uygulamasını [AppConfig.GERI_BILDIRIM_EPOSTA] adresine hazır mesajla açar. Uygulama hiçbir şeyi
 * kendisi göndermez ve sunucuya bağlanmaz; sürüm, dil ve ülke bilgisi mesajın altına eklenir (kullanıcı görür).
 */
@Composable
fun GeriBildirimDialog(konum: SelectedLocation?, onKapat: () -> Unit, onSonuc: (String) -> Unit) {
    val context = LocalContext.current
    var metin by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onKapat,
        title = { Text(t("Geri bildirim")) },
        text = {
            Column {
                Text(
                    t("Öneri, hata ya da isteğinizi yazın. Mesaj e-posta uygulamanızda hazır açılır; göndermeden önce görebilirsiniz."),
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = metin,
                    onValueChange = { metin = it.take(GERI_BILDIRIM_SINIR) },
                    minLines = 3,
                    maxLines = 4,
                    placeholder = { Text(t("Mesajınız")) },
                    supportingText = {
                        Text(
                            "${metin.length}/$GERI_BILDIRIM_SINIR",
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = metin.isNotBlank(),
                onClick = {
                    if (gonder(context, metin.trim(), konum)) {
                        onKapat()
                    } else {
                        onSonuc(t("Telefonda e-posta uygulaması bulunamadı."))
                    }
                },
            ) { Text(t("Gönder")) }
        },
        dismissButton = { TextButton(onClick = onKapat) { Text(t("Vazgeç")) } },
    )
}

private fun gonder(context: Context, metin: String, konum: SelectedLocation?): Boolean {
    val ek = buildString {
        append("\n\n— Ezan Vakti ").append(BuildConfig.VERSION_NAME)
        append(" · ").append(Dil.kod)
        append(" · ").append(konum?.let { if (it.turkiyede) "TR" else it.country?.nameEn ?: it.country?.name } ?: "-")
        append(" · Android ").append(android.os.Build.VERSION.RELEASE)
    }
    val konu = "Ezan Vakti — geri bildirim (${BuildConfig.VERSION_NAME})"
    val govde = metin + ek
    // Bazı e-posta uygulamaları (Outlook, Samsung E-posta…) ACTION_SENDTO ile gelen ekstraları yok sayar;
    // alıcı, konu ve metin bu yüzden mailto adresinin içine de yazılır.
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse(
            "mailto:" + AppConfig.GERI_BILDIRIM_EPOSTA +
                "?subject=" + Uri.encode(konu) + "&body=" + Uri.encode(govde)
        )
        putExtra(Intent.EXTRA_EMAIL, arrayOf(AppConfig.GERI_BILDIRIM_EPOSTA))
        putExtra(Intent.EXTRA_SUBJECT, konu)
        putExtra(Intent.EXTRA_TEXT, govde)
    }
    return try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
